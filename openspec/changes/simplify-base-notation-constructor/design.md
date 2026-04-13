## Context

`BaseNotation` currently stores five fields initialized via a single constructor: `context`, `filenameExtension`, `defaultType`, `converter`, and `schemaParser`. Lombok `@Getter` generates the accessors that satisfy the `Notation` interface. Every subclass passes hardcoded values (or `null`) for four of these — they are type constants, not instance configuration. Intermediate abstract classes (`StringNotation`, `VendorNotation`) forward these parameters through their own constructors, adding boilerplate at every level.

The class hierarchy (production classes only):

```
BaseNotation(context, filenameExtension, defaultType, converter, schemaParser)
├── BinaryNotation       — passes (context, null, UNKNOWN, null, null)
├── JsonNotation         — passes (context, ".json", DEFAULT_TYPE, new Converter(), new Parser())
├── StringNotation(context, ext, type, converter, parser, stringMapper)
│   ├── CsvNotation      — passes (".csv", DEFAULT_TYPE, new Converter(), new Parser())
│   ├── XmlNotation      — passes (".xsd", DEFAULT_TYPE, new Converter(), new Parser(ctx))
│   └── SoapNotation     — passes (null, DEFAULT_TYPE, new Converter(), null)
└── VendorNotation(VendorNotationContext, ext, type, converter, parser)
    ├── AvroNotation      — passes (".avsc", DEFAULT_TYPE, null, PARSER)
    ├── JsonSchemaNotation— passes (".json", DEFAULT_TYPE, new Converter(), new Parser())
    └── ProtobufNotation  — passes (".proto", DEFAULT_TYPE, null, parserFromCtorArg)
```

Call sites that consume these values (`SchemaLibrary`, `ConvertUtil`) already null-check `converter()` and `schemaParser()` before use, so returning `null` from "not applicable" notations is safe.

## Goals / Non-Goals

**Goals:**
- Reduce `BaseNotation` constructor to a single parameter (`NotationContext`).
- Reduce `StringNotation` constructor to two parameters (`NotationContext` + `DataObjectMapper<String>`).
- Reduce `VendorNotation` constructor to one parameter (`VendorNotationContext`).
- Make each leaf class self-describing by implementing `filenameExtension()`, `defaultType()`, `converter()`, and `schemaParser()` directly.
- Prevent future constructor bloat — new type-specific attributes become abstract methods, not constructor args.

**Non-Goals:**
- Changing `NotationContext` or `VendorNotationContext` (those carry genuine instance configuration).
- Changing the `Notation` interface contract (it stays exactly as-is).
- Introducing `Optional` or sentinels to replace `null` returns — the existing null convention is well-established.
- Addressing object creation/caching for converter and schemaParser instances — subclasses may choose to create new instances per call or cache in a field as they see fit.

## Decisions

**1. Abstract methods on BaseNotation, not a builder or config object**

Alternative considered: a `NotationConfig` record holding the four values, passed as a single constructor arg. Rejected because it just moves the problem — the values are still type constants being threaded through construction. Abstract methods express the intent directly: "each notation type defines these."

**2. Keep `@Getter` on BaseNotation for `context` only**

The `context` field remains a constructor parameter and stored field — it is genuinely instance-specific. The four removed fields were satisfying the `Notation` interface via `@Getter`; after removal, the interface methods become abstract on `BaseNotation` and are implemented by each subclass.

**3. Leaf classes own their converter/schemaParser instantiation**

Whether a leaf creates a `new JsonDataObjectConverter()` in the method body, stores it in a `private final` field initialized in the constructor, or uses a static constant is up to that class. The abstract method just requires it to provide one (or `null`). This preserves current patterns — e.g., `AvroNotation` uses a static `AVRO_SCHEMA_PARSER`, `XmlNotation` needs context-dependent construction, `ProtobufNotation` receives its parser as a constructor arg.

**4. `name()` stays as a concrete method on BaseNotation**

`name()` delegates to `context.name()` and is the same for all subclasses. It remains a concrete method, not abstract.

## Risks / Trade-offs

- **[Risk] Large number of files touched** → The change spans ~10 modules. Mitigated by the fact that each file change is mechanical: remove constructor args, add method overrides returning the same values.
- **[Risk] Test breakage** → `BaseNotationTest.DummyNotation` and test doubles in `StringNotationTest`, `VendorNotationTest` need to implement the new abstract methods. Mitigated by the compiler catching all missing implementations.
- **[Risk] Merge conflict with open PR adding more constructor args** → This change should ideally land first or be coordinated. The open PR's new parameters would become additional abstract methods instead of constructor args, which is the whole point.
