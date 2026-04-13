## Why

When a KSML test contains multiple `assert` blocks, the test runner stops at the first failing block and never executes the rest. Users running a test suite from the command line (or from a Docker image) get a whack-a-mole experience: fix one assertion, rerun, discover the next broken block, repeat. There is no way to see every failure in a single run. This also blocks any future structured output (JUnit XML, GitHub annotations, third-party CI reporters), because the current `TestResult` data model carries only one status + one message per test, with no per-block granularity.

## What Changes

- **AssertionRunner** stops returning early on the first failing assert block. All blocks within a test execute, each producing its own result, and a later block is allowed to run even if an earlier one failed or errored.
- **BREAKING (internal API)**: `TestResult` record gains `Path sourceFile`, `List<BlockResult> blocks`, `Duration duration`, and a `String setupError` field. The `message` field is replaced by `setupError` (non-null only when the test failed before assertions could run). Existing callers of `TestResult.pass/fail/error(testName, ...)` factory methods are updated.
- **New record `BlockResult`** carries `int index`, `String topic` (nullable), `List<String> stores` (nullable), `Status status`, `String message` (nullable), and `Duration duration`.
- **Test-level status** is derived from block results: any `ERROR` → `ERROR`, else any `FAIL` → `FAIL`, else `PASS`. A non-null `setupError` short-circuits to `ERROR` with no blocks recorded.
- **CLI reporter** (`KSMLTestRunner.reportResults`) is rewritten in pytest style: the per-test line shows an aggregate count (`FAIL  My test  (2/3 blocks failed)`) and each failing block is printed beneath it with its index, target topic/stores, and message. The final summary adds a second line reporting total assert blocks executed vs failed.
- **New CLI flag `--fail-fast`** preserves the old bail-on-first-failure behavior for users who want it (large suites, quick iteration loops). Default is the new collect-all behavior.
- **Shared Python context across blocks within a test is documented as intentional.** Blocks may rely on globals defined by earlier blocks in the same test. Users wanting isolation split into separate tests.
- Test runner exit code semantics are unchanged: `0` if every test passed, `1` otherwise.

## Capabilities

### New Capabilities
- `ksml-test-runner`: Standalone test runner that parses YAML test definitions, executes them against `TopologyTestDriver`, runs Python assertions, and reports results. This change introduces the capability spec (no prior spec exists) with requirements covering result aggregation, reporter output, and the `--fail-fast` flag.

### Modified Capabilities

## Impact

- **Code**:
  - `ksml-test-runner/src/main/java/io/axual/ksml/testrunner/TestResult.java` — record shape change
  - New file `ksml-test-runner/src/main/java/io/axual/ksml/testrunner/BlockResult.java`
  - `ksml-test-runner/src/main/java/io/axual/ksml/testrunner/AssertionRunner.java` — collect-all refactor, timing instrumentation
  - `ksml-test-runner/src/main/java/io/axual/ksml/testrunner/KSMLTestRunner.java` — reporter rewrite, `--fail-fast` wiring, timing instrumentation, pass `sourceFile` through to results
- **Tests**: Any unit test that constructs or asserts on `TestResult` directly will need updating. The existing sample tests (`sample-filter-test.yaml` etc.) continue to work unchanged.
- **Public CLI**: New flag `--fail-fast`. No other user-visible CLI changes. Docker invocation unchanged.
- **Docs**: `ksml-test-runner/README.md` gains a note about shared Python context across blocks and documents `--fail-fast`.
- **Dependencies**: None added or removed.
- **Downstream**: Unblocks the follow-on `add-junit-xml-test-output` change, which will serialize the new data model to JUnit XML.
