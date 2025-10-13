# Format Compatibility Matrix for KSML Flags System

## Overview

This document provides compatibility matrices showing which flags should be set when comparing schemas/types between different serialization formats (Avro, Protobuf, JSON, XML, CSV).

**Background**: KSML's DataField is a superset container that stores metadata for all formats. When comparing schemas from different formats, certain flags must be set to ignore format-specific metadata that doesn't apply to both formats.

---

## Format Metadata Feature Matrix

First, let's understand what metadata each format supports:

| Feature | Avro | Protobuf | JSON | XML | CSV |
|---------|------|----------|------|-----|-----|
| **Field Names** | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes (headers) |
| **Field Tags** | ❌ No | ✅ Yes (field numbers) | ❌ No | ❌ No | ❌ No |
| **Field Order** | ✅ Yes (ASCENDING/DESCENDING/IGNORE) | ❌ No (defaults) | ❌ No | ❌ No | ❌ No |
| **Namespaces** | ✅ Yes | ✅ Yes (packages) | ✅ Yes ($id, $schema) | ✅ Yes (xmlns) | ❌ No |
| **Documentation** | ✅ Yes (doc field) | ✅ Yes (comments) | ✅ Yes (description) | ✅ Yes (annotations) | ❌ No |
| **Default Values** | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ❌ No |
| **Required Fields** | ✅ Yes (via union with null) | ✅ Yes (required/optional) | ✅ Yes (required array) | ✅ Yes (minOccurs) | ❌ No |
| **Schema Evolution** | ✅ Yes (name-based) | ✅ Yes (tag-based) | ⚠️  Limited | ⚠️  Limited | ❌ No |
| **Type System** | Rich (primitives, records, arrays, maps, unions, enums, fixed) | Rich (primitives, messages, repeated, maps, enums) | Medium (primitives, objects, arrays) | Medium (primitives, elements, attributes) | Simple (strings, primitives) |

---

## DataSchemaFlags Compatibility Matrix

This matrix shows which **DataSchemaFlags** should be set when comparing schemas between different formats. Setting these flags ensures format-agnostic structural comparison.

### Legend
- ✅ **No flags needed** - Formats are highly compatible
- 🔶 **Minimal flags** - Ignore 1-2 format-specific features
- 🔷 **Moderate flags** - Ignore 3-5 format-specific features
- 🔴 **Many flags** - Ignore 6+ format-specific features (significant differences)

### Comparison Matrix

|  | **From: Avro** | **From: Protobuf** | **From: JSON** | **From: XML** | **From: CSV** |
|---|---|---|---|---|---|
| **To: Avro** | ✅ None (same format) | 🔷 TAG + ORDER | 🔷 ORDER + NAMESPACE + DOC | 🔷 ORDER + NAMESPACE | 🔴 ORDER + NAMESPACE + DOC + DEFAULTS |
| **To: Protobuf** | 🔷 TAG + ORDER | ✅ None (same format) | 🔶 TAG + NAMESPACE + DOC | 🔶 TAG + NAMESPACE | 🔴 TAG + NAMESPACE + DOC + DEFAULTS |
| **To: JSON** | 🔷 ORDER + NAMESPACE + DOC | 🔶 TAG + NAMESPACE + DOC | ✅ None (same format) | 🔶 NAMESPACE | 🔷 NAMESPACE + DOC + DEFAULTS |
| **To: XML** | 🔷 ORDER + NAMESPACE | 🔶 TAG + NAMESPACE | 🔶 NAMESPACE | ✅ None (same format) | 🔷 NAMESPACE + DOC + DEFAULTS |
| **To: CSV** | 🔴 ORDER + NAMESPACE + DOC + DEFAULTS | 🔴 TAG + NAMESPACE + DOC + DEFAULTS | 🔷 NAMESPACE + DOC + DEFAULTS | 🔷 NAMESPACE + DOC + DEFAULTS | ✅ None (same format) |

---

## Detailed Flag Recommendations

### 1. Avro ↔ Protobuf Comparison

**Avro → Protobuf**:
```java
Flags flags = new Flags(
    DataSchemaFlags.IGNORE_DATA_FIELD_TAG,        // Avro doesn't have tags
    DataSchemaFlags.IGNORE_DATA_FIELD_ORDER       // Protobuf doesn't use Avro sort order
);
```

**Protobuf → Avro**:
```java
Flags flags = new Flags(
    DataSchemaFlags.IGNORE_DATA_FIELD_TAG,        // Protobuf tags don't exist in Avro
    DataSchemaFlags.IGNORE_DATA_FIELD_ORDER       // Avro order doesn't exist in Protobuf
);
```

**Reasoning**:
- Avro uses `order` (ASCENDING/DESCENDING/IGNORE) for record sorting
- Protobuf uses `tag` (field numbers) for wire format and evolution
- Both have rich type systems and support namespaces, docs, defaults

---

### 2. Avro ↔ JSON Comparison

**Avro → JSON or JSON → Avro**:
```java
Flags flags = new Flags(
    DataSchemaFlags.IGNORE_DATA_FIELD_ORDER,      // JSON doesn't have field order
    DataSchemaFlags.IGNORE_NAMED_SCHEMA_NAMESPACE, // JSON uses $id/$schema differently
    DataSchemaFlags.IGNORE_NAMED_SCHEMA_DOC       // JSON uses "description" vs Avro's "doc"
);
```

**Reasoning**:
- JSON schemas don't have Avro's sort order concept
- Namespace handling differs ($id vs Avro namespace)
- Documentation field names differ

---

### 3. Protobuf ↔ JSON Comparison

**Protobuf → JSON or JSON → Protobuf**:
```java
Flags flags = new Flags(
    DataSchemaFlags.IGNORE_DATA_FIELD_TAG,         // JSON doesn't have field tags
    DataSchemaFlags.IGNORE_NAMED_SCHEMA_NAMESPACE, // Different namespace concepts
    DataSchemaFlags.IGNORE_NAMED_SCHEMA_DOC        // Different doc formats
);
```

**Reasoning**:
- JSON doesn't use Protobuf field numbers
- Namespace/package handling differs
- Documentation formats differ

---

### 4. Any Format ↔ CSV Comparison

**Any Format → CSV**:
```java
Flags flags = new Flags(
    DataSchemaFlags.IGNORE_DATA_FIELD_TAG,              // CSV has no tags
    DataSchemaFlags.IGNORE_DATA_FIELD_ORDER,            // CSV has no order metadata
    DataSchemaFlags.IGNORE_NAMED_SCHEMA_NAMESPACE,      // CSV has no namespaces
    DataSchemaFlags.IGNORE_NAMED_SCHEMA_DOC,            // CSV has no documentation
    DataSchemaFlags.IGNORE_DATA_FIELD_DEFAULT_VALUE,    // CSV has no defaults
    DataSchemaFlags.IGNORE_DATA_FIELD_REQUIRED          // CSV doesn't track required
);
```

**Reasoning**:
- CSV is the simplest format with minimal metadata
- Only supports field names and basic types
- No schema evolution support

---

### 5. Avro ↔ XML Comparison

**Avro → XML or XML → Avro**:
```java
Flags flags = new Flags(
    DataSchemaFlags.IGNORE_DATA_FIELD_ORDER,       // XML doesn't have Avro sort order
    DataSchemaFlags.IGNORE_NAMED_SCHEMA_NAMESPACE  // Different namespace concepts (xmlns vs Avro)
);
```

**Reasoning**:
- XML and Avro both support rich schemas with docs and defaults
- Namespace implementations differ (xmlns vs Avro namespace)
- Avro's sort order doesn't apply to XML

---

### 6. Protobuf ↔ XML Comparison

**Protobuf → XML or XML → Protobuf**:
```java
Flags flags = new Flags(
    DataSchemaFlags.IGNORE_DATA_FIELD_TAG,         // XML doesn't have field numbers
    DataSchemaFlags.IGNORE_NAMED_SCHEMA_NAMESPACE  // Different namespace concepts
);
```

**Reasoning**:
- XML and Protobuf both support rich schemas
- Protobuf field numbers don't map to XML
- Namespace/package concepts differ

---

## DataTypeFlags Compatibility Matrix

**DataTypeFlags** are used for comparing **DataType** instances (runtime type information), not schemas. These flags are less dependent on serialization format and more about structural type comparison.

### When to Use DataTypeFlags

DataTypeFlags are typically used when:
1. Comparing types extracted from different sources (not format-specific)
2. Performing structural type matching without schema details
3. Checking type compatibility for function parameters/return values

### Common DataTypeFlags Scenarios

| Scenario | Recommended Flags |
|----------|------------------|
| **Structural-only comparison** (ignore Java types) | `IGNORE_DATA_TYPE_CONTAINER_CLASS` |
| **Type compatibility without schema** | `IGNORE_ENUM_TYPE_SCHEMA`, `IGNORE_STRUCT_TYPE_SCHEMA` |
| **Union type relaxed matching** | `IGNORE_UNION_TYPE_MEMBER_NAME`, `IGNORE_UNION_TYPE_MEMBER_TAG` |
| **Complete union structure ignore** | `IGNORE_UNION_TYPE_MEMBERS` |

### Example: Cross-Format Type Comparison

When comparing types derived from different formats:

```java
// Example: Comparing StructType from Avro schema vs Protobuf schema
Flags flags = new Flags(
    DataTypeFlags.IGNORE_DATA_TYPE_CONTAINER_CLASS,  // Different Java representations
    DataTypeFlags.IGNORE_STRUCT_TYPE_SCHEMA          // Ignore embedded schema differences
);

DataType avroType = avroSchemaMapper.toDataType(avroSchema);
DataType protobufType = protobufSchemaMapper.toDataType(protobufSchema);

Equal result = avroType.equals(protobufType, flags);
```

---

## General Recommendations

### 1. Format-Agnostic Structural Comparison

For comparing **structure only** across any formats:

```java
Flags flags = new Flags(
    // Ignore format-specific metadata
    DataSchemaFlags.IGNORE_DATA_FIELD_TAG,
    DataSchemaFlags.IGNORE_DATA_FIELD_ORDER,
    DataSchemaFlags.IGNORE_NAMED_SCHEMA_NAMESPACE,
    DataSchemaFlags.IGNORE_NAMED_SCHEMA_DOC,
    DataSchemaFlags.IGNORE_DATA_FIELD_DOC,

    // Focus on core structure
    // Keep: field names, field types, field required status
);
```

### 2. Schema Evolution Compatibility

For checking if schemas are compatible for evolution:

```java
Flags flags = new Flags(
    // Ignore metadata that doesn't affect compatibility
    DataSchemaFlags.IGNORE_NAMED_SCHEMA_DOC,
    DataSchemaFlags.IGNORE_DATA_FIELD_DOC,

    // Keep: field names, types, tags (Protobuf), defaults, required status
);
```

### 3. Test Data Validation

For validating test data structure matches expected schema:

```java
Flags flags = new Flags(
    // Ignore all metadata
    DataSchemaFlags.IGNORE_NAMED_SCHEMA_NAMESPACE,
    DataSchemaFlags.IGNORE_NAMED_SCHEMA_DOC,
    DataSchemaFlags.IGNORE_DATA_FIELD_DOC,
    DataSchemaFlags.IGNORE_DATA_FIELD_TAG,
    DataSchemaFlags.IGNORE_DATA_FIELD_ORDER,
    DataSchemaFlags.IGNORE_DATA_FIELD_DEFAULT_VALUE,

    // Focus only on field names and types
);
```

---

## Quick Reference: Flag Groups

### Format-Agnostic Flags (Use for any cross-format comparison)
```java
public static final String[] FORMAT_AGNOSTIC_FLAGS = {
    DataSchemaFlags.IGNORE_DATA_FIELD_TAG,
    DataSchemaFlags.IGNORE_DATA_FIELD_ORDER
};
```

### Metadata-Agnostic Flags (Structural comparison only)
```java
public static final String[] METADATA_AGNOSTIC_FLAGS = {
    DataSchemaFlags.IGNORE_NAMED_SCHEMA_NAMESPACE,
    DataSchemaFlags.IGNORE_NAMED_SCHEMA_DOC,
    DataSchemaFlags.IGNORE_DATA_FIELD_DOC,
    DataSchemaFlags.IGNORE_DATA_FIELD_TAG,
    DataSchemaFlags.IGNORE_DATA_FIELD_ORDER
};
```

### CSV Compatibility Flags (CSV → Rich Format)
```java
public static final String[] CSV_COMPATIBILITY_FLAGS = {
    DataSchemaFlags.IGNORE_DATA_FIELD_TAG,
    DataSchemaFlags.IGNORE_DATA_FIELD_ORDER,
    DataSchemaFlags.IGNORE_NAMED_SCHEMA_NAMESPACE,
    DataSchemaFlags.IGNORE_NAMED_SCHEMA_DOC,
    DataSchemaFlags.IGNORE_DATA_FIELD_DOC,
    DataSchemaFlags.IGNORE_DATA_FIELD_DEFAULT_VALUE,
    DataSchemaFlags.IGNORE_DATA_FIELD_REQUIRED
};
```

---

## Related Documentation

- **PR #375**: Introduces the Flags system for equality/assignability checks
- **conversation-summary-2025-01-12.md**: Details on DataSchema, DataField, and format-specific metadata
- **CLAUDE.md**: KSML architecture overview

---

## Notes

1. **Tags vs Order**: The most common incompatibility is between Protobuf tags (field numbers) and Avro order (sort order)
2. **Namespace Differences**: Each format has different namespace/package concepts
3. **CSV Limitations**: CSV is the least feature-rich format and requires the most flags when comparing
4. **Documentation Fields**: Different formats use different field names for documentation (doc, description, comments)
5. **Union Types**: Union/variant handling differs significantly across formats

When in doubt, use the **Format-Agnostic Flags** group for any cross-format comparison to ensure structural matching without format-specific metadata interference.
