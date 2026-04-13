## ADDED Requirements

### Requirement: BaseNotation constructor accepts only NotationContext
The `BaseNotation` constructor SHALL accept a single parameter of type `NotationContext`. It SHALL NOT accept `filenameExtension`, `defaultType`, `converter`, or `schemaParser` as constructor parameters.

#### Scenario: Constructing a BaseNotation subclass
- **WHEN** a concrete subclass of `BaseNotation` is instantiated
- **THEN** only `NotationContext` (or a subclass like `VendorNotationContext`) is passed to `super()`

### Requirement: filenameExtension is an abstract method on BaseNotation
`BaseNotation` SHALL declare `filenameExtension()` as an abstract method. Each concrete subclass SHALL implement it, returning the type-specific file extension or `null` when not applicable.

#### Scenario: Notation with a schema file extension
- **WHEN** `filenameExtension()` is called on `JsonNotation`
- **THEN** it returns `".json"`

#### Scenario: Notation without a schema file extension
- **WHEN** `filenameExtension()` is called on `BinaryNotation`
- **THEN** it returns `null`

### Requirement: defaultType is an abstract method on BaseNotation
`BaseNotation` SHALL declare `defaultType()` as an abstract method. Each concrete subclass SHALL implement it, returning the type-specific default `DataType`.

#### Scenario: Default type for JSON notation
- **WHEN** `defaultType()` is called on `JsonNotation`
- **THEN** it returns the `UnionType(StructType, ListType)` constant

#### Scenario: Default type for Binary notation
- **WHEN** `defaultType()` is called on `BinaryNotation`
- **THEN** it returns `DataType.UNKNOWN`

### Requirement: converter is an abstract method on BaseNotation
`BaseNotation` SHALL declare `converter()` as an abstract method. Each concrete subclass SHALL implement it, returning the type-specific `Notation.Converter` or `null` when conversion is not supported.

#### Scenario: Notation with a converter
- **WHEN** `converter()` is called on `CsvNotation`
- **THEN** it returns an instance of `CsvDataObjectConverter`

#### Scenario: Notation without a converter
- **WHEN** `converter()` is called on `BinaryNotation`
- **THEN** it returns `null`

### Requirement: schemaParser is an abstract method on BaseNotation
`BaseNotation` SHALL declare `schemaParser()` as an abstract method. Each concrete subclass SHALL implement it, returning the type-specific `Notation.SchemaParser` or `null` when schema parsing is not supported.

#### Scenario: Notation with a schema parser
- **WHEN** `schemaParser()` is called on `AvroNotation`
- **THEN** it returns the Avro schema parser instance

#### Scenario: Notation without a schema parser
- **WHEN** `schemaParser()` is called on `BinaryNotation`
- **THEN** it returns `null`

### Requirement: StringNotation constructor is simplified
The `StringNotation` constructor SHALL accept only `NotationContext` and `DataObjectMapper<String>`. It SHALL NOT forward `filenameExtension`, `defaultType`, `converter`, or `schemaParser` to `BaseNotation`.

#### Scenario: Constructing a StringNotation subclass
- **WHEN** `CsvNotation` is instantiated with a `NotationContext`
- **THEN** it passes only the context to `StringNotation`'s constructor (along with its own `stringMapper`)

### Requirement: VendorNotation constructor is simplified
The `VendorNotation` constructor SHALL accept only `VendorNotationContext`. It SHALL NOT forward `filenameExtension`, `defaultType`, `converter`, or `schemaParser` to `BaseNotation`.

#### Scenario: Constructing a VendorNotation subclass
- **WHEN** `AvroNotation` is instantiated with a `VendorNotationContext`
- **THEN** it passes only the context to `VendorNotation`'s constructor
