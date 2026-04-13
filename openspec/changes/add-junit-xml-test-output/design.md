## Context

After the `aggregate-test-results` change lands, `TestResult` carries per-block results, a source file path, and measured durations. All the information needed for a JUnit XML report is in that data model — this change is a pure serialization task plus CLI plumbing. No runner behavior changes, no new concepts, no assertion-engine work.

JUnit XML is not a formally standardized format but there is a widely supported "Ant / Surefire" flavor that every CI reporter understands: `<testsuites>` root → `<testsuite>` → `<testcase>` with `<failure>` or `<error>` children. The schema is small enough to emit by hand with `javax.xml.stream.XMLStreamWriter` — no external library needed, no runtime dependency added.

The main design question is how to represent per-block failures within the one-failure-per-testcase JUnit shape. That was decided earlier in exploration: **concatenate** every failing block's message into a single `<failure>` body. This keeps JUnit consumers simple (they read one failure per test, which is what they expect) and preserves per-block detail in the text for humans who click through.

## Goals / Non-Goals

**Goals:**
- Emit a JUnit XML report that passes into `dorny/test-reporter`, IntelliJ's "Import JUnit XML", Jenkins JUnit plugin, and GitLab's test reporter without further massaging.
- Preserve per-block failure detail in the `<failure>` / `<error>` body text even though JUnit's structure only supports one per testcase.
- Populate timing attributes (`time="..."`) from real measured durations, not synthesized values.
- Write atomically so an interrupted run never leaves a malformed XML file on disk.
- Keep the change small and self-contained — one new class, one new CLI flag, one new unit test class.

**Non-Goals:**
- Per-block `<testcase>` emission. We chose concatenation for a reason; revisit only if real users complain.
- Custom XML dialects (xUnit.net, TestNG). Standard Surefire-flavored JUnit XML is what CI tooling expects.
- SARIF output, HTML reports, or any other format. One format at a time.
- GitHub annotations (`::error file=...::`). That belongs in a future GitHub Action change, not in the runner.
- Line-level source mapping back to the YAML `code:` block. Interesting, but the synthesized Python wrapper in `AssertionRunner` makes this non-trivial and nothing in v1 depends on it.
- Attaching captured stdout/stderr via `<system-out>` / `<system-err>`. Can be added later if users ask; adds noise for now.

## Decisions

**1. Emit XML by hand via `XMLStreamWriter`, no external library.**

Alternative considered: pull in a Surefire or reporter JAR. Rejected because the schema is ~six element types and the JDK's `XMLOutputFactory` produces well-formed output with correct escaping. Adding a dependency for a sub-100-line serializer is not worth the build-graph cost. The alternative of hand-building strings with `String.format` is also rejected because getting XML escaping right (`<`, `>`, `&`, control characters in assertion messages) is exactly what `XMLStreamWriter` handles for free.

**2. `<testsuite>` per source YAML file, `<testcase>` per test definition.**

Alternative A considered: one giant `<testsuite>` containing every test, with `classname` carrying the file path. Rejected because reporters that group by suite would show everything lumped into one bucket, losing the natural file-level grouping KSML users already think in.

Alternative B considered: `<testcase>` per assert block, with synthesized names like `"My test / block 2 (topic=foo)"`. Rejected because it inflates the testcase count (a user with 20 tests each having 3 blocks would see 60 testcases, not 20), breaks the user's mental model of what a "test" is, and creates awkward names in reporters. The per-block detail lives in the `<failure>` body instead.

**3. Multi-block failure shape: concatenate, one `<failure>` per testcase.**

When a test has failing blocks, the `<testcase>` carries exactly one `<failure>` element. Its `message` attribute is the message from the first failing block (because many reporters display that attribute prominently and only the first one is a reasonable default). Its body text is the per-block breakdown:

```
block 1 (topic=foo): Expected 2, got 1
block 3 (topic=foo): First record should be sensor-1, got sensor-3
```

One line per failing block, in `index` order, with the target in parentheses (topic or comma-separated stores). Passing blocks are omitted from the body. The trailing newline on each line is deliberate — most reporters render the body as preformatted text and the newlines make the detail readable.

Alternative considered: put the concatenation only in the body and leave `message` empty. Rejected because some reporters (notably GitLab's) only show the `message` attribute in their summary view, and an empty message gives users a blank PR check. Using the first block's message as the summary is not perfect but it is the most useful heuristic.

**4. Failure vs error precedence.**

JUnit distinguishes `<failure>` (assertion failed) from `<error>` (infrastructure broke). A test can have both. The rule: **ERROR takes precedence**. If the test has a non-null `setupError`, or any block with status `ERROR`, the `<testcase>` gets one `<error>` element and no `<failure>` element, even if other blocks also failed. This matches how the test-level status is derived (`aggregate-test-results` design decision #3) and avoids emitting both elements on one testcase — which the XSD technically allows but many reporters mis-handle.

The `<error>` body uses the same line-per-block format as `<failure>`, covering the erroring blocks only (not the failing ones). This trades completeness for simplicity: if a user hits a test with both FAILs and ERRORs, they see the ERRORs in the XML and still see both in the CLI output.

**5. Durations are serialized as decimal seconds with millisecond precision.**

JUnit XML's `time` attribute is specified as a decimal number in seconds. The `Duration` fields are nanosecond-precise but serializing nanoseconds is overkill; milliseconds (`%.3f`) is what every reporter expects. The format is `Locale.ROOT` `String.format("%.3f", durationMs / 1000.0)` to guarantee a dot decimal separator regardless of JVM locale.

Alternative considered: emit nanoseconds truthfully (`%.9f`). Rejected because nanosecond precision is both unhelpful (wall-clock variance dominates) and triggers some reporter parsers that expect shorter numbers.

**6. Atomic write via temp file + rename.**

Write the XML content to `<target>.tmp` in the same directory as `<target>`, then `Files.move(tmp, target, ATOMIC_MOVE, REPLACE_EXISTING)`. If the move fails because the filesystem does not support atomic moves, fall back to non-atomic replace and log a warning. This is a single-digit line count using `java.nio.file.Files` — no external library.

Alternative considered: stream directly to the target path. Rejected because an interrupted run (Ctrl-C, OOM kill, container stopped) would leave a half-written XML file that CI reporters parse as "zero tests ran" — silently passing a broken build. Atomic rename prevents this.

**7. CLI flag is opt-in and orthogonal.**

`--junit-xml FILE` is a picocli `@Option`. When unset, the XML writer is never constructed and no file is touched. When set, the writer runs after the existing text reporter, using the already-collected `List<TestResult>`. No change to exit codes, no change to stdout/stderr, no interaction with `--fail-fast`. The two flags are fully orthogonal: `--fail-fast --junit-xml out.xml` produces an XML file reflecting exactly what was executed (which may be fewer tests/blocks than a non-fail-fast run).

## Risks / Trade-offs

- **[Risk] JUnit XML flavor ambiguity.** There is no single canonical schema. We target the Apache Ant / Surefire flavor because that is what `dorny/test-reporter`, Jenkins, GitLab, IntelliJ, and VS Code's Test Explorer all understand. Mitigation: validate the emitted XML against a real reporter during implementation (run a sample through `dorny/test-reporter` locally or in a scratch workflow).

- **[Risk] Assertion messages with control characters.** A Python `AssertionError` could in principle contain characters that are illegal in XML text (e.g. `\u0000`). Mitigation: `XMLStreamWriter` handles escaping for legal characters but will throw on truly illegal control characters. Wrap the writer to strip or replace illegal characters with the Unicode replacement character before writing, and cover this with a unit test.

- **[Trade-off] First-block message as `<failure message="...">`.** Arbitrary choice among N failing blocks. Users whose most interesting failure is block 5 will see block 1's message in the summary. Acceptable because the full breakdown is always in the body, and "message" attributes are heuristics across the whole JUnit ecosystem.

- **[Trade-off] No `<system-out>` / `<system-err>`.** Reporters that show captured log output for failed tests will show nothing. Can be added later if users ask; capturing Python stdout from GraalPy is non-trivial and out of scope.

- **[Trade-off] Atomic write requires parent directory to exist.** If the user passes `--junit-xml nested/path/results.xml` and `nested/path/` does not exist, the runner creates the directories (or fails with a clear message if creation is not possible). This costs a few lines of `Files.createDirectories` but avoids surprising the user.

- **[Trade-off] Flag order in the picocli command means `--junit-xml` positional handling needs attention.** The existing `Arguments` class uses `@Parameters(arity = "1..*")` for test paths — picocli needs the option to be separated from positional parameters, which is standard picocli but worth confirming with a test invocation.
