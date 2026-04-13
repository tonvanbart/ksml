## 1. Implement JUnitXmlReporter

- [ ] 1.1 Create `ksml-test-runner/src/main/java/io/axual/ksml/testrunner/JUnitXmlReporter.java`. It takes a `List<TestResult>` and a target `Path` and writes a JUnit XML document.
- [ ] 1.2 Use `javax.xml.stream.XMLOutputFactory` and `XMLStreamWriter` for emission. No new Maven dependency.
- [ ] 1.3 Group `TestResult` entries by `sourceFile` to build one `<testsuite>` per file. Within each suite, emit one `<testcase>` per `TestResult`.
- [ ] 1.4 Compute and emit `tests`, `failures`, `errors`, `time` attributes on each `<testsuite>` and on the `<testsuites>` root.
- [ ] 1.5 Format `time` attributes using `Locale.ROOT` `String.format("%.3f", millis / 1000.0)` to guarantee a dot decimal separator.
- [ ] 1.6 For each `<testcase>`, emit `name` (test name), `classname` (source file name), and `time` (test duration).
- [ ] 1.7 For a test with only failing blocks (no errors, no setup error), emit one `<failure message="<first block message>">` whose body contains one line per failing block in the form `block <index> (topic=<topic>|stores=<...>): <message>`.
- [ ] 1.8 For a test with a setup error or any erroring blocks, emit one `<error message="<first error message>">` instead of a `<failure>`. Setup error body is empty. Block-error body lists erroring blocks in the same line format.
- [ ] 1.9 For a passing test, emit no child element inside `<testcase>`.
- [ ] 1.10 Handle messages containing XML-illegal control characters by replacing them with the Unicode replacement character (`\uFFFD`) before writing.

## 2. Atomic write

- [ ] 2.1 Write to a sibling temp file (e.g., `<target>.tmp` in the same directory) and then `Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)`. Fall back to a non-atomic replace with a `log.warn` if `ATOMIC_MOVE` is not supported.
- [ ] 2.2 If the parent directory of the target does not exist, create it via `Files.createDirectories`. Report a clear error and exit with non-zero code if creation fails.
- [ ] 2.3 If writing fails (disk full, permission denied), write the stack to `log.error`, emit a clear message to `System.err`, and return a non-zero exit from the runner.

## 3. Wire into KSMLTestRunner

- [ ] 3.1 Add `@CommandLine.Option(names = "--junit-xml", paramLabel = "FILE", description = "Write JUnit XML report to FILE")` on the picocli `Arguments` class.
- [ ] 3.2 In `main`, after `reportResults(results)` completes, if `--junit-xml` is set, instantiate and invoke `JUnitXmlReporter` with the result list and target path.
- [ ] 3.3 If the reporter throws, set the exit code to non-zero regardless of test outcome (failed write is a build failure).

## 4. Tests

- [ ] 4.1 Add `JUnitXmlReporterTest` in `ksml-test-runner/src/test/java/.../testrunner/`. Build synthetic `TestResult` lists and assert on the emitted XML via string matching and/or a DOM parse-and-check.
- [ ] 4.2 Test case: one passing test, no failures/errors, correct counts and timing.
- [ ] 4.3 Test case: one test with one failing block, one `<failure>` emitted with correct message attribute and body format.
- [ ] 4.4 Test case: one test with three blocks (blocks 1 and 3 fail, block 2 passes), one `<failure>` element with body listing blocks 1 and 3 in order, block 2 omitted.
- [ ] 4.5 Test case: test with a `setupError`, one `<error>` element with empty body and correct `message` attribute.
- [ ] 4.6 Test case: test with mixed FAIL and ERROR blocks, one `<error>` element (precedence), no `<failure>`.
- [ ] 4.7 Test case: multiple test files grouped into separate `<testsuite>` elements with correct per-suite counts and aggregate root counts.
- [ ] 4.8 Test case: assertion message containing an XML-illegal control character is replaced, produced XML still parses.
- [ ] 4.9 Test case: atomic write — write to a target, verify the temp file does not exist after the write, verify the target file content matches.
- [ ] 4.10 Test case: target directory does not exist, reporter creates it and writes successfully.
- [ ] 4.11 Integration test: run `KSMLTestRunner.main` with `--junit-xml out.xml` against an existing sample YAML (e.g., `sample-filter-test.yaml`) and parse the produced file to verify its structure.

## 5. Documentation

- [ ] 5.1 Update `ksml-test-runner/README.md` to add a "JUnit XML output" section describing the `--junit-xml FILE` flag, the XML shape (one suite per file, one case per test), and how multi-block failures are represented.
- [ ] 5.2 Include a GitHub Actions example snippet in the README showing how to pair `--junit-xml results.xml` with `dorny/test-reporter@v1` to get inline PR feedback. Note that the runner itself does not require the action — the snippet is illustrative of how the XML plugs in.
- [ ] 5.3 Note that the flag is fully orthogonal to `--fail-fast`: when both are set, the XML reflects whatever was executed before the fail-fast stop.

## 6. Verification

- [ ] 6.1 Run `mvn clean package -pl ksml-test-runner -am` and confirm all tests pass.
- [ ] 6.2 Manually invoke the runner with `--junit-xml /tmp/out.xml` against a sample suite that mixes passing, failing, and erroring tests. Open the XML in a browser and visually check structure.
- [ ] 6.3 Run the produced XML through at least one external reporter (e.g., paste into a scratch GitHub Actions workflow using `dorny/test-reporter@v1`, or open with IntelliJ's "Import Test Results") to confirm real-world compatibility.
