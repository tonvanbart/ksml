## 1. Refactor BaseNotation

- [ ] 1.1 Remove `filenameExtension`, `defaultType`, `converter`, and `schemaParser` fields from `BaseNotation`. Remove them from the constructor. Declare them as abstract methods. Remove `@Getter` (replace with explicit `@Getter` on `context` only or a manual getter). Keep `name()` and `noSerdeFor()` as concrete methods.
- [ ] 1.2 Update `BaseNotationTest` — the `DummyNotation` inner class must implement the four abstract methods. Update constructor calls and assertions accordingly.

## 2. Refactor StringNotation and its subclasses

- [ ] 2.1 Simplify `StringNotation` constructor to accept only `NotationContext` and `DataObjectMapper<String>`. Remove forwarding of `filenameExtension`, `defaultType`, `converter`, `schemaParser` to `super()`.
- [ ] 2.2 Update `CsvNotation` — implement `filenameExtension()`, `defaultType()`, `converter()`, `schemaParser()` returning current hardcoded values. Simplify constructor.
- [ ] 2.3 Update `XmlNotation` — implement the four methods. `schemaParser()` will need to store the context-dependent `XmlSchemaParser` in a local field. Simplify constructor.
- [ ] 2.4 Update `SoapNotation` — implement the four methods (returning `null` for `filenameExtension` and `schemaParser`). Simplify constructor.
- [ ] 2.5 Update `StringNotationTest` — adjust `ConcreteStringNotation` to implement abstract methods and update constructor calls.

## 3. Refactor VendorNotation and its subclasses

- [ ] 3.1 Simplify `VendorNotation` constructor to accept only `VendorNotationContext`. Remove forwarding of `filenameExtension`, `defaultType`, `converter`, `schemaParser` to `super()`.
- [ ] 3.2 Update `AvroNotation` — implement the four methods. Simplify constructor.
- [ ] 3.3 Update `JsonSchemaNotation` — implement the four methods. Simplify constructor.
- [ ] 3.4 Update `ProtobufNotation` — implement the four methods. `schemaParser()` returns the parser received via its own constructor arg (stored in a local field). Simplify constructor.
- [ ] 3.5 Update `VendorNotationTest` — adjust `ConcreteVendorNotation` and constructor calls.

## 4. Refactor direct BaseNotation subclasses

- [ ] 4.1 Update `BinaryNotation` — implement the four methods (returning `null` for `filenameExtension`, `converter`, `schemaParser`; `DataType.UNKNOWN` for `defaultType`). Simplify constructor.
- [ ] 4.2 Update `JsonNotation` — implement the four methods. Remove the redundant `defaultType()` override that already exists. Simplify constructor.

## 5. Update remaining tests

- [ ] 5.1 Update `BinaryNotationTest`, `JsonNotationTest`, and any other test classes that construct notation instances or assert on the removed fields.
- [ ] 5.2 Verify `MockNotation` in `ksml/src/test/java/io/axual/ksml/notation/MockNotation.java` — it implements `Notation` directly (not via `BaseNotation`), so it should be unaffected, but verify.
- [ ] 5.3 Run full build to confirm no compilation errors or test failures across all modules.
