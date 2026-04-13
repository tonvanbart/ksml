## Why

The `BaseNotation` constructor accepts five parameters (`context`, `filenameExtension`, `defaultType`, `converter`, `schemaParser`), but four of them are type-specific constants — every subclass passes hardcoded values (or `null`). These parameters thread through intermediate abstract classes (`StringNotation`, `VendorNotation`), inflating their constructors too. An open PR is about to add more type-specific parameters, making this worse. Moving type-specific values out of the constructor into abstract methods stops the constructor from growing and makes the class hierarchy easier to extend.

## What Changes

- **BREAKING**: Remove `filenameExtension`, `defaultType`, `converter`, and `schemaParser` fields and constructor parameters from `BaseNotation`. The constructor will only take `NotationContext context`.
- Make `filenameExtension()`, `defaultType()`, `converter()`, and `schemaParser()` abstract methods on `BaseNotation` (they are already declared on the `Notation` interface; currently satisfied by Lombok `@Getter` on stored fields).
- Simplify `StringNotation` constructor from 6 parameters to 2 (`context` + `stringMapper`). Each `StringNotation` subclass implements the four methods directly.
- Simplify `VendorNotation` constructor from 5 parameters to 1 (`VendorNotationContext`). Each `VendorNotation` subclass implements the four methods directly.
- Each leaf class (`BinaryNotation`, `JsonNotation`, `CsvNotation`, `XmlNotation`, `SoapNotation`, `AvroNotation`, `JsonSchemaNotation`, `ProtobufNotation`) implements the four abstract methods, returning its current hardcoded values (including `null` where "not applicable").

## Capabilities

### New Capabilities
- `abstract-notation-methods`: Replace constructor-threaded type constants with abstract methods on BaseNotation, letting each subclass declare its own filenameExtension, defaultType, converter, and schemaParser.

### Modified Capabilities

## Impact

- **BaseNotation** and all its subclasses across multiple modules (`ksml-data`, `ksml-data-binary`, `ksml-data-json`, `ksml-data-csv`, `ksml-data-xml`, `ksml-data-soap`, `ksml-data-avro`, `ksml-data-jsonschema`, `ksml-data-protobuf`).
- Tests for all notation classes will need constructor call updates and may need adjustments where they assert on stored fields vs method returns.
- The `BaseNotationTest` `DummyNotation` inner class will need to implement the abstract methods.
- No external API impact — `Notation` interface is unchanged, callers already use the interface methods.
