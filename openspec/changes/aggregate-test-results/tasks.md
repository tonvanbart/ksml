## 1. Introduce BlockResult and reshape TestResult

- [ ] 1.1 Create `ksml-test-runner/src/main/java/io/axual/ksml/testrunner/BlockResult.java` as a record with `index`, `topic`, `stores`, `status` (reusing `TestResult.Status`), `message`, `duration`.
- [ ] 1.2 Refactor `TestResult` to carry `testName`, `sourceFile`, `setupError`, `blocks`, `duration`. Replace the stored `status` / `message` components with a derived `status()` method (setupError non-null → ERROR; else any block ERROR → ERROR; else any block FAIL → FAIL; else PASS). Remove the old `pass/fail/error(testName, message)` factories and add new factories: `setupError(testName, sourceFile, message, duration)` and `fromBlocks(testName, sourceFile, blocks, duration)`.

## 2. Refactor AssertionRunner to collect all block results

- [ ] 2.1 Change `runAssertions` return type from `TestResult` to `List<BlockResult>`. Iterate every assert block, wrap each in its own try/catch, produce a `BlockResult` per block with measured `Duration`. Do not return early on failure or error.
- [ ] 2.2 Accept an optional `failFast` boolean parameter on `runAssertions`. When true, stop iterating after the first block whose status is not `PASS` (but still return the list of results collected so far).
- [ ] 2.3 Preserve the existing per-test `PythonContext` — create it once outside the block loop and reuse across blocks, matching the current behavior. Add a code comment noting this is intentional per the design decision.
- [ ] 2.4 Populate `BlockResult.topic` and `BlockResult.stores` from the corresponding `AssertBlock` fields for display in the reporter.

## 3. Wire the new data model into KSMLTestRunner

- [ ] 3.1 In `runSingleTest`, measure total duration, call the refactored `AssertionRunner`, and build a `TestResult` via the new factories. On setup failure (the existing outer try/catch branches), use `TestResult.setupError(...)`. On success path, use `TestResult.fromBlocks(...)`.
- [ ] 3.2 Add a `--fail-fast` option to the picocli `Arguments` class and thread it through to `AssertionRunner.runAssertions`.
- [ ] 3.3 Pass the test `sourceFile` (the YAML `Path`) into the result.

## 4. Rewrite reportResults for hierarchical output

- [ ] 4.1 For each `TestResult`, print the top-level status line. For FAIL include `"(<failedCount>/<totalCount> blocks failed)"`. For ERROR with `setupError`, print the setup error message indented on the next line.
- [ ] 4.2 For each non-PASS `BlockResult`, print an indented detail block: block index, target topic or stores in parentheses, and the message on a further-indented line.
- [ ] 4.3 At the end, print the existing `<N> passed, <N> failed, <N> errors` summary and add a second line `<total> assert blocks executed, <failed> failed` (skipping the second line if zero blocks ran, e.g. every test errored in setup).

## 5. Tests

- [ ] 5.1 Update `TestResult` unit tests (if any) to match the new shape. Search for `TestResult.pass(`, `TestResult.fail(`, `TestResult.error(` and update callers.
- [ ] 5.2 Add a new test YAML under `ksml-test-runner/src/test/resources/` with two assert blocks where the first fails and the second passes. Add a Java test that runs it via `KSMLTestRunner.runSingleTest` and asserts both `BlockResult` entries are present with correct statuses.
- [ ] 5.3 Add a Java test that exercises the shared-Python-context guarantee: block 1 defines a global, block 2 reads it and asserts on it.
- [ ] 5.4 Add a Java test for the `--fail-fast` behavior: two assert blocks where block 1 fails, verify only one `BlockResult` is in the list.
- [ ] 5.5 Add a Java test for the reporter: build a synthetic list of `TestResult` (mix of PASS / FAIL with failing blocks / ERROR with setupError) and assert on captured stdout to verify the expected hierarchical shape.

## 6. Documentation

- [ ] 6.1 Update `ksml-test-runner/README.md` to document the new collect-all behavior, the `--fail-fast` flag, and the shared-Python-context semantics across blocks within a test.
- [ ] 6.2 Add a short note to the README about the `TopologyTestDriver` output-topic draining gotcha: two assert blocks targeting the same topic will see the second block's `records` empty, because the first block drained it.

## 7. Verification

- [ ] 7.1 Run `mvn clean package -pl ksml-test-runner -am` and confirm all tests pass.
- [ ] 7.2 Manually run the new two-block sample YAML from the command line and verify the output matches the design's pytest-style example.
- [ ] 7.3 Manually run the same sample with `--fail-fast` and verify block 2 is not reported.
