## ADDED Requirements

### Requirement: --junit-xml flag emits a JUnit XML report
The test runner SHALL accept a `--junit-xml FILE` command-line flag. When the flag is provided, the runner SHALL, after all tests have finished, write a JUnit XML document containing every test's result to the given file path. When the flag is not provided, the runner SHALL NOT create any XML file.

#### Scenario: Flag provided and all tests pass
- **WHEN** the runner is invoked with `--junit-xml results.xml` against a test suite where every test passes
- **THEN** a file `results.xml` is created containing a `<testsuites>` root with `failures="0"` and `errors="0"`

#### Scenario: Flag not provided
- **WHEN** the runner is invoked without `--junit-xml`
- **THEN** no XML file is created and the runner's behavior is otherwise identical to a run without the flag

#### Scenario: Flag with a non-existent parent directory
- **WHEN** the runner is invoked with `--junit-xml nested/path/results.xml` and the `nested/path/` directory does not exist
- **THEN** the runner creates the missing parent directories and writes the file, or exits with a clear error message indicating the path could not be written

### Requirement: XML structure groups testcases by source YAML file
The emitted XML SHALL use a single `<testsuites>` root element containing one `<testsuite>` child per source YAML file that was executed. Each `<testsuite>` SHALL contain one `<testcase>` child per test defined in that file. The `<testsuite>` `name` attribute SHALL be the source file's path relative to the current working directory when possible, or its absolute path otherwise. Each `<testcase>` `name` attribute SHALL be the test's `name` from the YAML definition. The `<testcase>` `classname` attribute SHALL be the source file's `name` attribute (the same value used for the parent suite).

#### Scenario: Two tests in one file
- **WHEN** a single YAML file defines two tests and the runner serializes its results
- **THEN** the XML contains one `<testsuite>` whose `name` matches the file path and two `<testcase>` children whose `name` values match the two test names

#### Scenario: Two files each with one test
- **WHEN** two YAML files each define one test
- **THEN** the XML contains two `<testsuite>` elements, each with exactly one `<testcase>` child

### Requirement: Suite and case attributes include counts and timing
Each `<testsuite>` element SHALL carry `tests`, `failures`, `errors`, and `time` attributes reflecting the sum of its contained testcases. Each `<testcase>` element SHALL carry a `time` attribute in seconds with millisecond precision, derived from the `TestResult.duration` field. The root `<testsuites>` element SHALL carry aggregate `tests`, `failures`, `errors`, and `time` attributes summing across all suites.

#### Scenario: Suite with one passing and one failing test
- **WHEN** a suite contains a passing test (duration 0.120s) and a failing test (duration 0.080s)
- **THEN** its `<testsuite>` attributes are `tests="2"`, `failures="1"`, `errors="0"`, `time="0.200"`

#### Scenario: Root totals aggregate across suites
- **WHEN** two suites each have `tests="2"`, one with `failures="1"` and one with `errors="1"`
- **THEN** the `<testsuites>` root has `tests="4"`, `failures="1"`, `errors="1"`

### Requirement: Multi-block failures concatenate into a single <failure> element
When a test has one or more `BlockResult` entries with status `FAIL` (and no blocks with status `ERROR` and no `setupError`), its `<testcase>` SHALL contain exactly one `<failure>` element. The `<failure>` `message` attribute SHALL be the message from the first failing block. The `<failure>` element body SHALL contain, for every failing block in index order, a line of the form `block <index> (topic=<topic>|stores=<store,…>): <message>` followed by a newline.

#### Scenario: Single failing block
- **WHEN** a test has one failing block with target `topic=foo` and message `"Expected 2, got 1"`
- **THEN** its `<testcase>` has one `<failure message="Expected 2, got 1">` element whose body is `"block 1 (topic=foo): Expected 2, got 1\n"`

#### Scenario: Multiple failing blocks
- **WHEN** a test has three blocks where blocks 1 and 3 fail and block 2 passes
- **THEN** its `<testcase>` has one `<failure>` element whose body contains two lines, one for block 1 and one for block 3, in that order

#### Scenario: Passing test has no failure or error child
- **WHEN** a test has all blocks with status `PASS`
- **THEN** its `<testcase>` contains no `<failure>` and no `<error>` child elements

### Requirement: Erroring tests emit a single <error> element
When a test has a non-null `setupError`, or when one or more of its `BlockResult` entries have status `ERROR`, its `<testcase>` SHALL contain exactly one `<error>` element (and no `<failure>` element). For `setupError`, the `message` attribute SHALL be the setup error message and the body SHALL be empty. For block-level errors, the `message` attribute SHALL be the message of the first erroring block and the body SHALL contain a line per erroring block in the same format used for failures.

#### Scenario: Setup error
- **WHEN** a test errors before assertions run with message `"Pipeline file not found: foo.yaml"`
- **THEN** its `<testcase>` contains one `<error message="Pipeline file not found: foo.yaml"/>` element and no `<failure>` element

#### Scenario: Mixed failure and error in the same test
- **WHEN** a test has one failing block and one erroring block (and no setup error)
- **THEN** its `<testcase>` contains one `<error>` element (covering the erroring block) and no `<failure>` element, because test-level ERROR takes precedence over FAIL

### Requirement: JUnit XML file is written atomically
The runner SHALL write the JUnit XML output to a temporary file in the target directory and rename it into place after the write completes successfully. If the runner is interrupted or crashes mid-write, the target path SHALL either contain the previous file's content (if any) or not exist — never a truncated or malformed XML document.

#### Scenario: Successful write
- **WHEN** the runner completes all tests and writes `results.xml`
- **THEN** at no point during the run does `results.xml` exist in a partially-written state; it appears fully formed only after the rename succeeds

#### Scenario: Write failure is reported
- **WHEN** writing the JUnit XML file fails (disk full, permission denied, etc.)
- **THEN** the runner reports the failure on stderr and exits with a non-zero code, regardless of whether the tests themselves passed
