# KSML Parsing Overview

## Overview

This document provides a comprehensive overview of how KSML topology definitions are parsed, the classes involved in the parsing process, and the resulting data structures. It uses a minimal pipeline example to illustrate the complete parsing flow.

---

## Example KSML Definition

### Minimal Pipeline: Filter Odd Numbers

```yaml
streams:
  topicIn:
    topic: topicIn
    keyType: string
    valueType: int

  topicOut:
    topic: topicOut
    keyType: string
    valueType: int

pipelines:
  filter_odd_pipeline:
    from: topicIn
    via:
      - type: filter
        expression: value % 2 != 0
    to: topicOut
```

This simple pipeline:
1. Reads numbers from `topicIn` (string keys, int values)
2. Filters out even numbers (keeps only odd numbers)
3. Writes odd numbers to `topicOut`

---

## Parsing Flow

### Step 1: YAML → ParseNode

**Location**: `KSMLRunner.java:176-177`

The YAML file is loaded and converted to a `ParseNode` tree structure:

```java
final var parser = new TopologyDefinitionParser(name);
final var topologyDefinition = parser.parse(ParseNode.fromRoot(definition, name));
```

**Parser**: Jackson ObjectMapper converts YAML to JsonNode, then wrapped in ParseNode.

---

### Step 2: TopologyDefinitionParser Processing

**Location**: `TopologyDefinitionParser.java:59-74`

The parser processes the YAML in this order:

#### 2.1 Parse Topology Metadata (lines 60-62)

```java
final var name = optional(stringField(KSMLDSL.NAME, true, "<anonymous topology>",
                                     "The name of the topology")).parse(node);
final var version = optional(stringField(KSMLDSL.VERSION, true, "<no version>",
                                        "The version of the topology")).parse(node);
final var description = optional(stringField(KSMLDSL.DESCRIPTION, true, "",
                                            "The description of the topology")).parse(node);
```

Fields:
- `name`: (optional, defaults to `<anonymous topology>`)
- `version`: (optional, defaults to `<no version>`)
- `description`: (optional, defaults to `""`)

#### 2.2 Parse Resources (line 64)

```java
final var resources = resourcesParser.parse(node);
```

Delegates to `TopologyResourcesParser` which:
- Parses `streams` section → creates `StreamDefinition` objects
- Parses `tables` section → creates `TableDefinition` objects (if present)
- Parses `globalTables` section → creates `GlobalTableDefinition` objects (if present)
- Parses `functions` section → creates `FunctionDefinition` objects (if present)
- Parses `stateStores` section → creates state store definitions (if present)

#### 2.3 Create TopologyDefinition (line 65)

```java
final var result = new TopologyDefinition(resources.namespace(), name, version, description);
```

#### 2.4 Register Resources (lines 67-68)

```java
resources.topics().forEach(result::register);
resources.stateStores().forEach(result::register);
resources.functions().forEach(result::register);
```

#### 2.5 Parse Pipelines (line 71)

```java
new MapParser<>(PIPELINE, "pipeline definition", new PipelineDefinitionParser(resources))
    .parse(node.get(PIPELINES))
    .forEach(result::register);
```

---

### Step 3: Stream Definitions Parsing

**Location**: `TopologyResourcesParser.java:63-64`

For each entry in the `streams` section:

```java
final var streams = streamsParser.parse(node);
if (streams != null) streams.forEach(result::register);
```

Creates **two `TopicDefinition` objects**:

**topicIn**:
- `name`: "topicIn"
- `topic`: "topicIn"
- `keyType`: DataType for `string`
- `valueType`: DataType for `int`

**topicOut**:
- `name`: "topicOut"
- `topic`: "topicOut"
- `keyType`: DataType for `string`
- `valueType`: DataType for `int`

---

### Step 4: Pipeline Definition Parsing

**Location**: `PipelineDefinitionParser.java:80-94`

The `filter_odd_pipeline` is parsed into a `PipelineDefinition`:

```java
(name, from, via, as, branch, forEach, print, toTopic, toTne, tags) -> {
    final var shortName = validateName("Pipeline", name, defaultShortName, true);
    final var longName = validateName("Pipeline", name, defaultLongName, false);
    via = via != null ? via : new ArrayList<>();
    // ... determine which sink operation to use
    if (toTopic != null) return new PipelineDefinition(longName, from, via, toTopic);
    // ...
}
```

Components parsed:

#### 4.1 Pipeline Name
- **`name`**: "filter_odd_pipeline"

#### 4.2 Source
- **`from`**: Reference to `topicIn` TopicDefinition
  - Resolved via `resources.topic("topicIn")`

#### 4.3 Via Operations (Chain)
- **`via`**: List containing one `FilterOperation`
  - Created by `PipelineOperationParser` → `FilterOperationParser`
  - Contains a `FunctionDefinition` with the expression `value % 2 != 0`

#### 4.4 Sink
- **`to`**: Creates a `ToOperation` pointing to `topicOut`

---

## Resulting Data Structure

### TopologyDefinition

**Location**: `TopologyDefinition.java:32-43`

```java
public class TopologyDefinition extends TopologyResources {
    private final String name;           // "<anonymous topology>" or from YAML
    private final String version;        // "<no version>" or from YAML
    private final String description;    // "" or from YAML
    private final Map<String, PipelineDefinition> pipelines;  // LinkedHashMap
    private final Map<String, ProducerDefinition> producers;  // LinkedHashMap
}
```

**After parsing our example, contains**:

- `name`: (topology name)
- `version`: (topology version)
- `description`: (topology description)
- **Registered topics**:
  - `"topicIn"` → TopicDefinition(topic="topicIn", keyType=string, valueType=int)
  - `"topicOut"` → TopicDefinition(topic="topicOut", keyType=string, valueType=int)
- **Registered pipelines**:
  - `"filter_odd_pipeline"` → PipelineDefinition (see below)

---

### PipelineDefinition

**Location**: `PipelineDefinition.java:29-30`

```java
public record PipelineDefinition(
    String name,                              // "filter_odd_pipeline"
    TopologyResource<TopicDefinition> source, // Reference to topicIn
    List<StreamOperation> chain,              // [FilterOperation]
    StreamOperation sink                      // ToOperation(topicOut)
)
```

**After parsing our example**:

- **`name`**: `"filter_odd_pipeline"`

- **`source`**: `TopologyResource` wrapping `TopicDefinition` for `topicIn`
  - Points to: `TopicDefinition("topicIn", topic="topicIn", keyType=string, valueType=int)`

- **`chain`**: `List<StreamOperation>` with one element:
  - **`FilterOperation`**:
    - `config`: Contains operation name and tags
    - `predicate`: `FunctionDefinition` with expression `"value % 2 != 0"`
      - This will be compiled into a predicate function that returns `true` for odd numbers

- **`sink`**: **`ToOperation`**
  - Points to: `TopicDefinition("topicOut", topic="topicOut", keyType=string, valueType=int)`

---

## Visual Representation

```
TopologyDefinition
├── name: "<topology name>"
├── version: "<topology version>"
├── description: "<topology description>"
├── topics (Map)
│   ├── "topicIn" → TopicDefinition
│   │   ├── name: "topicIn"
│   │   ├── topic: "topicIn"
│   │   ├── keyType: DataType(string)
│   │   └── valueType: DataType(int)
│   └── "topicOut" → TopicDefinition
│       ├── name: "topicOut"
│       ├── topic: "topicOut"
│       ├── keyType: DataType(string)
│       └── valueType: DataType(int)
└── pipelines (Map)
    └── "filter_odd_pipeline" → PipelineDefinition
        ├── name: "filter_odd_pipeline"
        ├── source: TopologyResource<TopicDefinition("topicIn")>
        ├── chain: List<StreamOperation>
        │   └── [0] FilterOperation
        │       ├── config: OperationConfig
        │       │   ├── name: "filter_odd_pipeline_filter"
        │       │   └── tags: []
        │       └── predicate: FunctionDefinition
        │           ├── type: "expression"
        │           └── expression: "value % 2 != 0"
        └── sink: ToOperation
            └── target: TopologyResource<TopicDefinition("topicOut")>
```

---

## Key Classes Involved

### Parsing Classes

| Class | Location | Purpose |
|-------|----------|---------|
| **TopologyDefinitionParser** | `ksml/src/main/java/io/axual/ksml/definition/parser/TopologyDefinitionParser.java:37` | Top-level parser for KSML definitions |
| **TopologyResourcesParser** | `ksml/src/main/java/io/axual/ksml/definition/parser/TopologyResourcesParser.java:34` | Parses streams, tables, functions, state stores |
| **PipelineDefinitionParser** | `ksml/src/main/java/io/axual/ksml/definition/parser/PipelineDefinitionParser.java:37` | Parses pipeline definitions |
| **PipelineOperationParser** | `ksml/src/main/java/io/axual/ksml/operation/parser/PipelineOperationParser.java` | Parses individual operations (filter, map, etc.) |
| **FilterOperationParser** | `ksml/src/main/java/io/axual/ksml/operation/parser/FilterOperationParser.java` | Parses filter operations |
| **StreamDefinitionParser** | `ksml/src/main/java/io/axual/ksml/definition/parser/StreamDefinitionParser.java` | Parses stream (topic) definitions |
| **ParseNode** | `ksml/src/main/java/io/axual/ksml/parser/ParseNode.java` | Wraps YAML/JSON nodes for parsing |

### Definition Classes

| Class | Location | Purpose |
|-------|----------|---------|
| **TopologyDefinition** | `ksml/src/main/java/io/axual/ksml/generator/TopologyDefinition.java:32` | Root definition object containing all topology resources |
| **PipelineDefinition** | `ksml/src/main/java/io/axual/ksml/definition/PipelineDefinition.java:29` | Definition of a single pipeline (source → operations → sink) |
| **TopicDefinition** | `ksml/src/main/java/io/axual/ksml/definition/TopicDefinition.java` | Definition of a Kafka topic with key/value types |
| **FunctionDefinition** | `ksml/src/main/java/io/axual/ksml/definition/FunctionDefinition.java` | Definition of a user function (expression or Python) |

### Operation Classes

| Class | Location | Purpose |
|-------|----------|---------|
| **StreamOperation** | `ksml/src/main/java/io/axual/ksml/operation/StreamOperation.java` | Base interface for all stream operations |
| **FilterOperation** | `ksml/src/main/java/io/axual/ksml/operation/FilterOperation.java:35` | Filter operation implementation |
| **ToOperation** | `ksml/src/main/java/io/axual/ksml/operation/ToOperation.java` | Sink operation to write to a topic |
| **OperationConfig** | `ksml/src/main/java/io/axual/ksml/operation/OperationConfig.java` | Configuration for operations (name, tags) |

---

## Runtime Execution

When the topology is built (converted to Kafka Streams topology), operations are applied to create the actual streaming pipeline.

### FilterOperation.apply()

**Location**: `FilterOperation.java:45-66`

```java
public StreamWrapper apply(KStreamWrapper input, TopologyBuildContext context) {
    // 1. Get key and value types from input stream
    final var k = input.keyType();  // string
    final var v = input.valueType(); // int

    // 2. Create user predicate function from expression "value % 2 != 0"
    final var pred = userFunctionOf(context, PREDICATE_NAME, predicate,
                                   DataBoolean.DATATYPE, superOf(k.flatten()), superOf(v.flatten()));
    final var userPred = new UserPredicate(pred, tags);

    // 3. Create processor that evaluates the predicate
    final var supplier = new FixedKeyOperationProcessorSupplier<>(
        name,
        FilterProcessor::new,
        (stores, rec) -> userPred.test(stores, flattenValue(rec.key()), flattenValue(rec.value())),
        storeNames);

    // 4. Apply to Kafka Streams
    final var output = input.stream.processValues(supplier, Named.as(name), storeNames);

    // 5. Return wrapped stream with same types
    return new KStreamWrapper(output, k, v);
}
```

**Execution Flow**:

1. **Expression Compilation**: The expression `value % 2 != 0` is compiled into executable code
2. **Predicate Creation**: A `UserPredicate` wraps the compiled expression
3. **Processor Creation**: A Kafka Streams processor is created to evaluate the predicate
4. **Stream Application**: The processor is applied to the input KStream
5. **Result**: A new KStream containing only records where the predicate returns `true`

---

## Complete Pipeline Flow

### 1. Parse Time (Build Time)

```
YAML Definition
    ↓ (Jackson)
JsonNode
    ↓ (ParseNode.fromRoot)
ParseNode Tree
    ↓ (TopologyDefinitionParser)
TopologyDefinition
    ├── TopicDefinitions (topicIn, topicOut)
    └── PipelineDefinition
        ├── source: topicIn
        ├── chain: [FilterOperation]
        └── sink: ToOperation(topicOut)
```

### 2. Execution Time (Runtime)

```
Kafka Streams Topology Builder
    ↓ (TopologyGenerator)
For each PipelineDefinition:
    1. Create source processor (consume from topicIn)
    2. Apply chain operations:
       - FilterOperation.apply() → creates filter processor
    3. Apply sink operation:
       - ToOperation.apply() → creates sink processor (produce to topicOut)
    ↓
Kafka Streams Topology
    ↓ (KafkaStreams)
Running Stream Processing Application
```

### 3. Message Flow (Runtime)

```
Message arrives on topicIn: {key: "user123", value: 42}
    ↓
Source Processor reads message
    ↓
FilterProcessor evaluates: 42 % 2 != 0 → false (42 is even)
    ↓
Message filtered out (not forwarded)

---

Message arrives on topicIn: {key: "user456", value: 43}
    ↓
Source Processor reads message
    ↓
FilterProcessor evaluates: 43 % 2 != 0 → true (43 is odd)
    ↓
Message forwarded
    ↓
Sink Processor writes to topicOut: {key: "user456", value: 43}
```

---

## Parser Framework Architecture

KSML uses a custom parser framework with these key concepts:

### DefinitionParser<T>

Base class for all parsers. Provides:
- Field parsers: `stringField()`, `intField()`, `listField()`, `mapField()`
- Optional/required field handling
- Schema generation for validation

### StructsParser<T>

Parses structured objects with multiple fields. Used for:
- TopologyDefinition
- PipelineDefinition
- Operation definitions

### MapParser<T>

Parses maps of named objects. Used for:
- Map of pipelines (`pipelines: { pipeline1: {...}, pipeline2: {...} }`)
- Map of streams (`streams: { stream1: {...}, stream2: {...} }`)

### ParseNode

Wrapper around Jackson's JsonNode providing:
- Path tracking for error messages
- Type checking and conversion
- Child node access

---

## Type System Integration

KSML maintains type information throughout parsing and execution:

### DataType

Runtime type information for values:
- Primitive types: STRING, INTEGER, BOOLEAN, etc.
- Complex types: StructType, ListType, MapType
- Used for type checking and validation

### DataSchema

Schema information for structured data:
- Defines field names and types
- Used for serialization/deserialization
- Format-agnostic (works with Avro, Protobuf, JSON, etc.)

### Type Flow in Example

```
Parse Time:
  "valueType: int" → DataType.INTEGER
      ↓
  Stored in TopicDefinition
      ↓
  Retrieved during FilterOperation.apply()
      ↓
  Used to validate predicate signature

Runtime:
  Value deserialized as int (32-bit integer)
      ↓
  Predicate evaluates with correct type
      ↓
  Result serialized back as int
```

---

## Extension Points

The KSML parser framework is designed for extension:

### Adding New Operations

1. Create operation class implementing `StreamOperation`
2. Create parser class extending `OperationParser<YourOperation>`
3. Register parser in `PipelineOperationParser`

### Adding New Resource Types

1. Create definition class
2. Create parser class
3. Register in `TopologyResourcesParser`

### Adding New Notations (Serialization Formats)

1. Implement `Notation` interface
2. Implement `DataSchemaMapper` for the format
3. Register via SPI or configuration

---

## Related Documentation

- **CLAUDE.md**: General KSML architecture overview
- **conversation-summary-2025-01-12.md**: DataSchema and format-specific schema mapping
- **FORMAT_COMPATIBILITY_MATRIX.md**: Schema comparison flags across formats
- **FLAGS_USAGE_EXAMPLES.md**: Examples of using Flags for equality/assignability checks

---

## Summary

The KSML parsing process transforms YAML definitions into a rich object model that preserves:
- **Type information**: Keys and values maintain their types throughout
- **Topology structure**: Clear separation of sources, operations, and sinks
- **Operation semantics**: Each operation knows how to apply itself to streams
- **Error context**: Parse errors include line numbers and paths

This architecture enables:
- **Type safety**: Type checking at parse time
- **Extensibility**: Easy to add new operations and resource types
- **Format independence**: Works with any serialization format (Avro, Protobuf, JSON)
- **Validation**: JSON Schema generation for IDE support and validation
