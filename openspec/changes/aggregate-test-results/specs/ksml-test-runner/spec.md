## ADDED Requirements

### Requirement: All assertion blocks within a test are executed
The test runner SHALL execute every `assert` block declared in a test definition, even when an earlier block has failed or errored, unless the `--fail-fast` flag is set. Each block SHALL produce its own result.

#### Scenario: Later block runs after an earlier block fails
- **WHEN** a test definition contains three assert blocks and block 1 raises an `AssertionError`
- **THEN** blocks 2 and 3 are still executed, each producing a `BlockResult`

#### Scenario: Later block runs after an earlier block errors
- **WHEN** a test definition contains two assert blocks and block 1 references a non-existent state store (triggering an `ERROR` status)
- **THEN** block 2 is still executed and produces its own `BlockResult`

#### Scenario: Fail-fast flag stops on first failure
- **WHEN** the runner is invoked with `--fail-fast` and block 1 of a test fails
- **THEN** no further blocks in that test are executed and the test result contains only block 1's result

### Requirement: Test result data model carries per-block results
A `TestResult` SHALL carry the test name, the source YAML file path, a derived status, an optional setup error message, a list of `BlockResult` entries, and the total test duration. A `BlockResult` SHALL carry the block index (1-based), the target topic (nullable), the targeted state store names (nullable), a status, an optional message, and the block's duration.

#### Scenario: Test that passes all blocks
- **WHEN** a test with two passing assert blocks completes
- **THEN** the `TestResult` has status `PASS`, a null `setupError`, and a `blocks` list of size 2, each `BlockResult` with status `PASS`

#### Scenario: Test with one failing and one passing block
- **WHEN** a test with two assert blocks completes where block 1 fails and block 2 passes
- **THEN** the `TestResult` has status `FAIL`, a null `setupError`, and a `blocks` list where block 1 has status `FAIL` (with message) and block 2 has status `PASS`

#### Scenario: Test that errors before assertions run
- **WHEN** a test's pipeline fails to parse or a produce block references an unknown topic
- **THEN** the `TestResult` has status `ERROR`, a non-null `setupError`, and an empty `blocks` list

### Requirement: Test-level status is derived from block results
When a test has no setup error, the test-level status SHALL be computed as: `ERROR` if any block has status `ERROR`, otherwise `FAIL` if any block has status `FAIL`, otherwise `PASS`. When a test has a non-null `setupError`, the test-level status SHALL be `ERROR` regardless of blocks (which will be empty).

#### Scenario: All blocks pass
- **WHEN** every block in a test has status `PASS`
- **THEN** the test-level status is `PASS`

#### Scenario: Mix of pass and fail
- **WHEN** a test has one block with status `PASS` and one with status `FAIL`
- **THEN** the test-level status is `FAIL`

#### Scenario: Mix of fail and error
- **WHEN** a test has one block with status `FAIL` and one with status `ERROR`
- **THEN** the test-level status is `ERROR`

### Requirement: Python context is shared across blocks within a single test
The `PythonContext` used to run assertion code SHALL be created once per test and reused across all assertion blocks within that test. Globals, imports, and variable bindings defined by an earlier block SHALL be visible to later blocks.

#### Scenario: Block 2 reads a variable defined by block 1
- **WHEN** block 1 runs Python code `my_total = sum(r['value'] for r in records)` and block 2 runs `assert my_total > 0`
- **THEN** block 2's reference to `my_total` resolves to the value defined in block 1

#### Scenario: Block 2 runs in its own test has a fresh context
- **WHEN** two separate tests each reference a Python global `x`
- **THEN** the second test's `PythonContext` does not see `x` from the first test

### Requirement: CLI reporter shows per-block failure detail
The command-line reporter SHALL print one line per test with its status and, when failed, an aggregate count of failing blocks (`"(2/3 blocks failed)"`). For each failing or erroring block, the reporter SHALL print a nested detail line containing the block index, its target topic or stores, and its message. The final summary line SHALL report the total count of tests passed/failed/errored. A second summary line SHALL report the total number of assert blocks executed and how many of them failed.

#### Scenario: Test with one failing block
- **WHEN** a test with three assert blocks is reported and block 2 failed with message `"Expected 2, got 1"`
- **THEN** the report contains a line `FAIL  <testname>  (1/3 blocks failed)` followed by a nested detail line identifying block 2, its target, and the message `"Expected 2, got 1"`

#### Scenario: Summary line totals across tests
- **WHEN** three tests run, producing 2 passed + 1 failed at the test level and 7 total blocks of which 2 failed
- **THEN** the final summary reports `"2 passed, 1 failed, 0 errors"` and a second line reports `"7 assert blocks executed, 2 failed"`

### Requirement: --fail-fast flag preserves bail-on-first-failure behavior
The test runner SHALL accept a `--fail-fast` command-line flag. When set, within any single test, the runner SHALL stop executing further assert blocks after the first block whose status is not `PASS`. The flag SHALL NOT affect cross-test iteration — later test files always run regardless of earlier test outcomes.

#### Scenario: --fail-fast stops within a test
- **WHEN** `--fail-fast` is set and block 1 of a test fails
- **THEN** blocks 2 and later of that test are not executed

#### Scenario: --fail-fast does not stop across tests
- **WHEN** `--fail-fast` is set, test A fails, and test B is next in the sorted test file list
- **THEN** test B is still executed

### Requirement: Exit code reflects aggregate test outcome
The test runner process SHALL exit with code `0` if every test in the run has status `PASS`, and exit with code `1` otherwise.

#### Scenario: All tests pass
- **WHEN** every test in the run has status `PASS`
- **THEN** the process exits with code `0`

#### Scenario: At least one test fails or errors
- **WHEN** any test in the run has status `FAIL` or `ERROR`
- **THEN** the process exits with code `1`
