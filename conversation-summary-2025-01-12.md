# Conversation Summary: KSML Architecture Deep Dive
**Date:** January 12, 2025
**Topics:** DataSchema, Field Ordering, Protobuf/Avro Integration, Schema Mapping

## Overview
This conversation explored the KSML codebase architecture, focusing on how KSML handles schema abstraction across different serialization formats (Avro, Protobuf, JSON, etc.).

---

## Key Questions and Answers

### 1. How Does `deserialize()` Method Inheritance Work in ProtobufNotation?

**Question:** Where does the `deserialize` method in `ProtobufNotation` come from?

**Answer:**
- The method is inherited from Kafka's `org.apache.kafka.common.serialization.Deserializer` interface
- Located in: `ksml-data-protobuf/src/main/java/io/axual/ksml/data/notation/protobuf/ProtobufNotation.java:131-169`
- The `WrappedSerde` record (line 131) implements both `Serializer<Object>` and `Deserializer<Object>`
- It wraps Apicurio's Protobuf deserializer and adds KSML's `DataObject` conversion layer via `MAPPER.toDataObject()`

**Architecture Pattern:**
```
Kafka Deserializer Interface
    ↓
WrappedSerde (implements Deserializer<Object>)
    ↓
Apicurio ProtobufKafkaDeserializer
    ↓
KSML DataObject conversion
```

---

### 2. What is DataSchema and How Does It Relate to Avro/Protobuf Schemas?

**Question:** Explain how `DataSchema` is used and its relation to format-specific schemas.

**Answer:**

**DataSchema** is KSML's **format-agnostic internal schema representation** - a "lingua franca" for schema definitions.

**Key Files:**
- Base class: `ksml-data/src/main/java/io/axual/ksml/data/schema/DataSchema.java:34`
- Common subclass: `ksml-data/src/main/java/io/axual/ksml/data/schema/StructSchema.java:35`

**Schema Hierarchy:**
```
DataSchema (base)
├── Primitive schemas (static instances)
│   ├── NULL_SCHEMA, BOOLEAN_SCHEMA
│   ├── BYTE/SHORT/INTEGER/LONG_SCHEMA
│   ├── FLOAT/DOUBLE_SCHEMA
│   ├── BYTES/STRING_SCHEMA
│   └── ANY_SCHEMA
└── Complex schemas (subclasses)
    ├── StructSchema - records with named fields
    ├── ListSchema - arrays
    ├── MapSchema - key-value maps
    ├── UnionSchema - union types
    ├── EnumSchema - enumerations
    └── FixedSchema - fixed-size binary
```

**Bidirectional Mapping via DataSchemaMapper:**

Each format has its own mapper:
- **AvroSchemaMapper** (`ksml-data-avro/src/main/java/io/axual/ksml/data/notation/avro/AvroSchemaMapper.java:36`)
- **ProtobufSchemaMapper** (`ksml-data-protobuf/src/main/java/io/axual/ksml/data/notation/protobuf/ProtobufSchemaMapper.java:30`)
- **JsonSchemaMapper** (`ksml-data-json/src/main/java/io/axual/ksml/data/notation/json/JsonSchemaMapper.java:39`)

**Conversion Flow:**
```
Kafka Topic (Avro)
  → Avro Deserializer
  → org.apache.avro.Schema
  → AvroSchemaMapper.toDataSchema()
  → DataSchema (KSML internal)
  → KSML operations (filter, transform, etc.)
  → ProtobufSchemaMapper.fromDataSchema()
  → ProtobufSchema
  → Protobuf Serializer
  → Kafka Topic (Protobuf)
```

---

### 3. How Are Field Ordering and Tags Handled Across Formats?

**Question:** Protobuf has field tags (= 1, = 2), how does this work in Avro/JSON?

**Answer:**

**DataField** maintains BOTH `tag` and `order` attributes simultaneously to preserve format-specific metadata:
- `tag` (int): For Protobuf field numbers - `ksml-data/src/main/java/io/axual/ksml/data/schema/DataField.java:82`
- `order` (enum): For Avro sort order - `ksml-data/src/main/java/io/axual/ksml/data/schema/DataField.java:92`

**Format Comparison:**

| Format | Uses `tag`? | Uses `order`? | Purpose |
|--------|------------|--------------|---------|
| **Protobuf** | ✅ Yes (field number) | ❌ No (defaults to ASCENDING) | Wire format serialization, schema evolution |
| **Avro** | ❌ No (NO_TAG = -1) | ✅ Yes (ASCENDING/DESCENDING/IGNORE) | Record comparison/sorting |
| **JSON** | ❌ No | ❌ No | Field identification by name only |

**Key Implementation:**

**Protobuf → DataSchema** (`ProtobufFileElementSchemaMapper.java:104`):
```java
return new DataField(name, type, field.getDocumentation(),
                    field.getTag(),  // ← Protobuf tag
                    required, false, defaultValue);
```

**Avro → DataSchema** (`AvroSchemaMapper.java:131`):
```java
result.add(new DataField(field.name(), schema, field.doc(),
                        NO_TAG,  // ← No tag in Avro
                        required, false, defaultValue,
                        convertAvroOrderToDataFieldOrder(field.order())));  // ← Avro order
```

**Why Both Exist:**
DataField is a **superset container** for all format-specific metadata. When converting:
- **Protobuf → Avro**: Tags are lost (Avro doesn't support them)
- **Avro → Protobuf**: KSML assigns tags during schema creation
- **JSON ↔ Any**: No tags or order preserved

---

### 4. Are Avro Records Equal If Field Order Differs?

**Question:** Two Avro structs with same fields but different order - are instances equal?

**Example:**
```
Schema 1: {name: String, email: String}
Instance 1: {"Ton", "ton@axual.com"}

Schema 2: {email: String, name: String}
Instance 2: {"ton@axual.com", "Ton"}
```

**Answer:** **YES** - field values are logically equal, but with caveats.

**In KSML:**
`StructSchema.isAssignableFrom()` checks fields **by name**, not position (`StructSchema.java:140-152`):

```java
for (var field : fields) {
    // Get the field with the same name from the other schema  ← BY NAME!
    final var otherField = otherStructSchema.field(field.name());
    if (otherField != null && !field.isAssignableFrom(otherField)) return false;
}
```

**In Avro:**
- ✅ **Field values are logically equal** (same name-value pairs)
- ❌ **Schemas are not equal** (different field order)
- ❌ **`GenericRecord.equals()` returns false** (schemas differ)
- ✅ **Avro schema resolution handles this** during serialization/deserialization

**Three Concepts:**
1. **Binary Serialization**: Field order matters (written in schema order)
2. **Sorting/Comparison**: Field order matters (deterministic sorting)
3. **Logical Equality**: Field order doesn't matter (compared by name)

**Relevant Links:**
- [Avro Specification - Schema Resolution](https://avro.apache.org/docs/1.11.1/specification/#schema-resolution)
- [Apache Avro Java GenericRecord](https://github.com/apache/avro/blob/master/lang/java/avro/src/main/java/org/apache/avro/generic/GenericData.java)

---

## Architecture Insights

### The Adapter Pattern for Schema Metadata

KSML uses the **adapter pattern** for schema handling:

1. **Universal Internal Representation**: `DataSchema` + `DataField`
2. **Format-Specific Mappers**: Convert to/from native schemas
3. **Superset of Metadata**: Stores all possible metadata (tags, order, defaults, etc.)
4. **Selective Usage**: Each format uses only what it supports

**Benefits:**
- Schema transformation between any formats
- Type validation without knowing serialization format
- Python function access to schema information
- Field compatibility checking

### Key Modules

From the earlier `/init` command, relevant modules:

**Data Modules:**
- `ksml-data`: Core data type and schema logic
- `ksml-data-avro`: Avro support with schema mapping
- `ksml-data-protobuf`: Protobuf support with schema mapping
- `ksml-data-json`: JSON support with schema mapping

**Core Module:**
- `ksml`: YAML parser, topology builder, stream wrappers

---

## Important File References

| File | Location | Purpose |
|------|----------|---------|
| **DataSchema** | `ksml-data/src/main/java/io/axual/ksml/data/schema/DataSchema.java:34` | Base schema abstraction |
| **DataField** | `ksml-data/src/main/java/io/axual/ksml/data/schema/DataField.java` | Field metadata with tag + order |
| **StructSchema** | `ksml-data/src/main/java/io/axual/ksml/data/schema/StructSchema.java:35` | Structured schema with fields |
| **AvroSchemaMapper** | `ksml-data-avro/src/main/java/io/axual/ksml/data/notation/avro/AvroSchemaMapper.java:36` | Avro ↔ DataSchema conversion |
| **ProtobufSchemaMapper** | `ksml-data-protobuf/src/main/java/io/axual/ksml/data/notation/protobuf/ProtobufSchemaMapper.java:30` | Protobuf ↔ DataSchema conversion |
| **ProtobufNotation** | `ksml-data-protobuf/src/main/java/io/axual/ksml/data/notation/protobuf/ProtobufNotation.java:48` | Protobuf notation with WrappedSerde |
| **DataSchemaMapper** | `ksml-data/src/main/java/io/axual/ksml/data/mapper/DataSchemaMapper.java:25` | Interface for schema mappers |

---

## Related Documentation

Also created during this session:
- **CLAUDE.md**: Comprehensive architecture guide for future Claude Code instances
  - Build commands and testing
  - Module structure
  - 7 key architecture patterns
  - Common development tasks

---

## Next Steps

To continue this conversation on another machine:
1. Commit these documentation files to git
2. Push to your repository
3. Pull on the other machine
4. Reference this summary when asking follow-up questions

```bash
git add CLAUDE.md docs/conversation-summary-2025-01-12.md
git commit -m "Add architecture documentation and conversation summary"
git push
```

---

## Key Takeaways

1. **DataSchema is format-agnostic**: It's the bridge between all serialization formats
2. **DataField is a superset**: Contains metadata for all formats (tags, order, defaults)
3. **Field order in Avro**: Doesn't affect logical equality, only binary layout and sorting
4. **Mappers are bidirectional**: Each format has `toDataSchema()` and `fromDataSchema()`
5. **Schema evolution**: Protobuf tags enable this, Avro uses field names with defaults
