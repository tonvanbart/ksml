## Why

The KSML test runner today prints its results only as free-form text. Users running the runner in any CI system (GitHub Actions, GitLab, Jenkins, CircleCI, etc.) have no structured artifact to feed into test reporters, dashboards, or PR checks — they can only read job logs and scroll. JUnit XML is the de facto interchange format that every modern CI reporter speaks, regardless of the source language. Emitting JUnit XML from the runner unlocks PR-level test summaries, historical dashboards, and third-party reporters (`dorny/test-reporter` and friends) with zero additional tooling. It is also a prerequisite for any future GitHub Action wrapping the Docker image — without structured output, the action would be syntactic sugar over `docker run` rather than a real CI primitive.

This change depends on the `aggregate-test-results` change: it assumes `TestResult` carries per-block results, source file, and durations, and that every block has been executed rather than bailing on the first failure. That data model is what the XML writer serializes.

## What Changes

- **New CLI flag `--junit-xml FILE`** on `KSMLTestRunner`. When present, the runner writes a JUnit XML report to the given path at the end of the run. When absent, behavior is unchanged. The flag is opt-in — the runner never writes files the user did not request.
- **New class `JUnitXmlReporter`** in `ksml-test-runner` that consumes a `List<TestResult>` and serializes it to a JUnit XML document.
- **XML schema follows the widely supported Apache Ant / Surefire flavor**: a single `<testsuites>` root, one `<testsuite>` per source YAML file, one `<testcase>` per test defined in that file. Test-suite attributes (`tests`, `failures`, `errors`, `time`) are computed from the contained testcases.
- **Per-test failure representation (multi-block aggregation)**: when a test has one or more failing blocks, the runner emits **one `<failure>`** element on the `<testcase>` whose body concatenates every failing block's message (prefixed with the block index and its target topic or stores). A test with only erroring blocks or a `setupError` emits a single `<error>` element instead. A test that passes emits no `<failure>` or `<error>` child.
- **Durations**: `<testsuite time="...">` and `<testcase time="...">` are populated from the `Duration` fields already measured in the `aggregate-test-results` change, in seconds with millisecond precision.
- **Output file is written atomically at the end of the run**, after all tests finish. Write to a temp file then rename, so partial/interrupted runs do not leave a malformed XML document.
- **No change to text output or exit codes**: the existing CLI reporter and the `0/1` exit code semantics are unaffected. JUnit XML is strictly an additional side output.

## Capabilities

### New Capabilities

### Modified Capabilities
- `ksml-test-runner`: Adds requirements covering the `--junit-xml` flag, the XML schema mapping, multi-block failure aggregation into a single `<failure>` element, duration reporting, and atomic write semantics.

## Impact

- **Code**:
  - New file `ksml-test-runner/src/main/java/io/axual/ksml/testrunner/JUnitXmlReporter.java`
  - `ksml-test-runner/src/main/java/io/axual/ksml/testrunner/KSMLTestRunner.java` — add `--junit-xml` option to picocli `Arguments`, invoke reporter after test execution
- **Tests**: Unit tests for `JUnitXmlReporter` covering the suite/case mapping, multi-block concatenation, failure-vs-error distinction, setup-error tests, empty-blocks edge case, and atomic write. An integration test that runs a sample test YAML end-to-end with `--junit-xml` and validates the produced file against a reference shape.
- **Public CLI**: New flag `--junit-xml FILE`. No other user-visible changes.
- **Dependencies**: No new dependencies — JUnit XML can be emitted by hand (the schema is small) or via the JDK's built-in `javax.xml.stream.XMLStreamWriter`. No Surefire JAR on the runtime classpath.
- **Docs**: `ksml-test-runner/README.md` gains a section on the `--junit-xml` flag with an example GitHub Actions snippet that pairs it with `dorny/test-reporter@v1`.
- **Upstream dependency**: Requires `aggregate-test-results` to be merged first. That change provides the per-block data model this change serializes.
- **Downstream**: Unblocks a future `ksml-test-github-action` change that wraps the Docker image as a GitHub Action, exposing inline PR feedback via the emitted JUnit XML.
