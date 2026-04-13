## Context

`AssertionRunner.runAssertions` (ksml-test-runner/src/main/java/io/axual/ksml/testrunner/AssertionRunner.java:56) currently iterates its list of `AssertBlock` instances and returns the first non-`PASS` result, discarding the rest of the list. `TestResult` (ksml-test-runner/src/main/java/io/axual/ksml/testrunner/TestResult.java:30) is a three-field record (`testName`, `status`, `message`) — there is nowhere to store per-block results today. The top-level loop in `KSMLTestRunner.main` (line 102) already aggregates across test files correctly; only the within-test layer is broken.

Users running a test suite from the command line (locally, in Docker, or in CI) get a whack-a-mole experience: fix the reported block, rerun, learn about the next broken block, repeat. The fix unlocks downstream work — a follow-on change will serialize per-block results to JUnit XML, which in turn unblocks a GitHub Action.

Python assertions execute in a single `PythonContext` that is created at the top of `runAssertions` (AssertionRunner.java:57) and reused across blocks. Today this detail is invisible because later blocks never run. Once they do, the sharing becomes observable and needs to be a conscious decision rather than an accident of ordering.

## Goals / Non-Goals

**Goals:**
- Every assert block in a test runs, each producing its own result, regardless of whether earlier blocks failed.
- `TestResult` carries enough structure (per-block results, source file, durations) for both a hierarchical CLI reporter and future serializers (JUnit XML, GitHub annotations) to consume.
- CLI output shows all failures in a test at once, with enough detail to locate each failing block (index, topic/stores, message).
- An opt-in `--fail-fast` flag preserves the current bail-on-first-failure behavior for users who prefer it.
- Shared Python context across blocks is documented as an intentional design choice.

**Non-Goals:**
- JUnit XML output. Shipped in the follow-on `add-junit-xml-test-output` change.
- GitHub Actions annotations, or any line-level source mapping back to the YAML.
- Block isolation (a flag to run each block in a fresh `PythonContext`). Not needed for v1; can be added later if demand emerges.
- Parallel block execution. Blocks remain sequential — both because they share state by design, and because they read from shared `TopologyTestDriver` output topics where ordering matters.
- Changing the YAML test definition format. No new fields, no renamed fields.

## Decisions

**1. `TestResult` gains per-block structure, replacing the single `message` field.**

Alternative considered: keep `TestResult` as-is and introduce a side-car `Map<String, List<BlockResult>>`. Rejected because it splits the logical result across two places and invites drift. The cleaner move is one record that fully describes a test outcome.

The new shape:
```java
record TestResult(
    String testName,
    Path sourceFile,
    Status status,                 // derived
    String setupError,             // non-null iff test errored BEFORE assertions
    List<BlockResult> blocks,      // empty iff setupError != null
    Duration duration
)

record BlockResult(
    int index,                     // 1-based
    String topic,                  // nullable
    List<String> stores,           // nullable
    Status status,                 // PASS | FAIL | ERROR
    String message,                // null iff PASS
    Duration duration
)
```

The `Status` enum stays as `PASS | FAIL | ERROR`. FAIL = Python `AssertionError` raised. ERROR = everything else (store not found, GraalPy crash, produce failure, pipeline parse failure). This mirrors JUnit's `<failure>` vs `<error>` distinction and makes the downstream XML change trivial.

**2. Setup errors and block errors are represented distinctly.**

A test can fail at two levels:
- **Before assertions run**: pipeline parse, topology build, produce data. These are captured in `setupError` and the `blocks` list is empty. The test status is `ERROR`.
- **During an assertion block**: captured as a `BlockResult` with status `FAIL` (assertion tripped) or `ERROR` (infrastructure problem mid-block, e.g. state store not found).

This split matches the exception handler structure in `KSMLTestRunner.runSingleTest` (line 124–174), where the outer try/catch handles setup failures and delegates to `AssertionRunner` for block-level execution. Keeping the distinction explicit also makes the CLI output clearer: a test with `setupError` prints once with the setup message; a test with block-level failures prints a per-block breakdown.

**3. Test-level status is derived, not stored.**

Storing it would invite inconsistency (status says FAIL, blocks say PASS). A small derivation method on `TestResult` computes it lazily:
```java
public Status status() {
    if (setupError != null) return Status.ERROR;
    if (blocks.stream().anyMatch(b -> b.status() == Status.ERROR)) return Status.ERROR;
    if (blocks.stream().anyMatch(b -> b.status() == Status.FAIL)) return Status.FAIL;
    return Status.PASS;
}
```
Note: this requires making `TestResult` a regular class or adding a companion accessor, since Java records auto-generate accessors for components. A clean option is to store the raw state (`setupError`, `blocks`) and expose `status()` as an explicit method that shadows what a record component would produce — implemented by declaring `status` as a derived method rather than a record component.

**4. Python context is shared across blocks within a test, not across tests.**

`AssertionRunner.runAssertions` already creates `pythonContext` once per test (line 57). This change does not move the creation — it only removes the early return. Blocks can therefore see globals defined by earlier blocks in the same test. This is documented in `ksml-test-runner/README.md` so users can rely on it (set up a fixture in block 1, assert on it in block 2) or deliberately avoid it (put each concern in its own test).

Alternative considered: fresh context per block. Rejected for three reasons: (a) it changes implicit current behavior in a way nobody has asked for; (b) GraalPy context creation is not free and multi-block tests would regress in wall-clock time; (c) users who want isolation already have a natural unit for it — separate tests. If isolation is ever needed, a `--isolate-blocks` flag can be added later.

**5. CLI reporter output shape is pytest-flavored.**

```
=== KSML Test Results ===

  PASS  Filter pipeline passes blue sensors
  FAIL  Timestamp preservation check  (2/3 blocks failed)
          block 1 (topic=ksml_timestamped)
            AssertionError: Timestamp mismatch on record 0
          block 2 (topic=ksml_timestamped)
            AssertionError: Expected 5 records, got 4
  ERROR Broken pipeline test
          Invalid test definition: pipeline file not found

2 passed, 1 failed, 1 errored  (3 of 8 assert blocks failed)
```

Alternative considered: keep the flat one-line-per-test format and print a detail section at the bottom (pytest `=== FAILURES ===`). Rejected because nesting detail directly under the failing test line keeps visual locality — users do not need to scroll between a summary and a detail section to correlate them. The aggregate block count in the summary line is added because the test count alone hides the total work done.

**6. `--fail-fast` is the opt-in escape hatch, not the default.**

The new default is collect-all because it matches what CI users need ("show me everything broken"). Users who want tight feedback loops on large suites can opt in to `--fail-fast`. Keeping the current behavior as default was considered and rejected — leaving whack-a-mole as the default when the whole point of the change is to end it would be perverse.

`--fail-fast` applies within a test, not across tests. Cross-test iteration continues regardless. This matches pytest's `-x` which also stops mid-run at the first failing test, but in our model the within-test granularity is what users actually want to control. Cross-test stop-on-first is not requested and not in scope.

**7. Durations are measured, not faked.**

`Duration duration` on both records is measured by wrapping the existing work in `Instant.now()` timestamps. No performance concern at this scale — tests run sequentially and there are typically dozens to low hundreds of assert blocks total. Adding timing now means the downstream JUnit XML change has `<testcase time="...">` data available without further runner changes.

## Risks / Trade-offs

- **[Risk] Blocks can see each other's state in surprising ways.** A block that mutates a module-level Python global leaks that state into later blocks. Mitigated by documentation; users who want isolation use separate tests. Long-term mitigation is a `--isolate-blocks` flag, deferred.

- **[Risk] `TopologyTestDriver` output-topic draining is one-shot.** `collectOutputRecords` (AssertionRunner.java:142) drains records from the named output topic; a second block targeting the same topic sees an empty list. This is pre-existing behavior — it used to only surface when a user wrote multiple blocks against the same topic, which was rare because block 2 never ran anyway. With collect-all, it will surface more often. **Not fixed in this change** (scope is aggregation, not driver semantics), but will be called out in the README. A proper fix would buffer all records once at the start of the assertion phase, which is a bigger refactor best left for its own proposal.

- **[Risk] TestResult shape change breaks internal callers.** Only two call sites use `TestResult` today (`AssertionRunner` and `KSMLTestRunner`); both are updated in this change. No external consumers. The internal-only breakage is flagged in the proposal for transparency but does not need migration tooling.

- **[Trade-off] Derived `status()` vs stored.** Deriving keeps the data model self-consistent; the cost is a small amount of iteration on every read. Negligible at this scale.

- **[Trade-off] Printing block detail inline vs. in a tail section.** Inline is chosen for visual locality. The trade-off is that a very large failing test with many blocks will dominate its own area of the output. Acceptable — large tests are rare in practice and the user benefits from seeing all their breakage together.

- **[Trade-off] `--fail-fast` adds CLI surface area.** One extra flag to document and maintain. Small cost, clear benefit for the fast-iteration use case.
