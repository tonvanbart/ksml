# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

KSML (Kafka Streams Markup Language) is a wrapper around Apache Kafka Streams that allows developers to define stream processing topologies using YAML with embedded Python functions. It eliminates Java boilerplate by providing a declarative syntax for Kafka Streams operations while running on GraalVM for polyglot execution.

## Build Commands

### Full Build
```bash
mvn clean package
```

### Run Tests
```bash
mvn clean test
```

### Run Single Test
```bash
mvn test -Dtest=ClassName#methodName
```

### Run Tests for Specific Module
```bash
cd <module-name>
mvn test
```

### Build Docker Image
```bash
docker buildx create --name ksml
docker buildx --builder ksml build --load -t axual/ksml:local --target ksml -f Dockerfile .
docker buildx rm ksml
```

### Generate KSML JSON Schema
```bash
java io.axual.ksml.runner.KSMLRunner --schema [optional-output-file.json]
```

## Running KSML

### Local Development Environment

1. Start Kafka and dependencies:
```bash
docker compose up -d
```

2. Run KSML Runner (using example configuration):
```bash
java io.axual.ksml.runner.KSMLRunner workspace/local/ksml-runner-local.yaml
```

3. Run KSML Data Generator (produces test data):
```bash
java io.axual.ksml.runner.KSMLRunner workspace/local/ksml-data-generator-local.yaml
```

## Architecture

### Module Structure

The project is organized into independent modules that can be included separately:

- **ksml-data**: Core data type and schema logic, provides the foundation for all data operations
- **ksml-data-{format}**: Format-specific extensions (avro, binary, csv, json, protobuf, soap, xml) that implement serialization/deserialization for different data formats
- **ksml**: Core engine that parses KSML YAML definitions and converts them to Kafka Streams topologies via the Kafka Streams DSL
- **ksml-kafka-clients**: Custom Kafka clients supporting namespace resolution and topic patterns for multi-tenant Kafka installations
- **ksml-query**: REST API for querying Kafka Streams state stores from running KSML applications
- **ksml-runner**: Standalone application that loads KSML definitions and executes them

### Key Architecture Patterns

#### 1. YAML to Topology Pipeline

The core transformation flow: `YAML Definition → ParseNode → TopologyDefinition → Kafka Streams Topology`

- **TopologyDefinitionParser** (`io.axual.ksml.definition.parser.TopologyDefinitionParser`): Entry point that parses YAML into structured definitions
- **TopologyDefinition** (`io.axual.ksml.generator.TopologyDefinition`): Contains parsed pipelines and producers
- **PipelineDefinition**: Represents a single processing pipeline with source, operations, and sink
- **StreamWrapper** hierarchy: Wraps Kafka Streams objects (KStream, KTable, etc.) to provide uniform operation application

#### 2. Parser Framework

KSML uses a custom parser framework in `io.axual.ksml.parser`:

- **Parser**: Base interface for all parsers
- **DefinitionParser**: Parses structured definitions from YAML nodes
- **TopologyResourceAwareParser**: Parsers that need access to global topology resources (functions, stores, etc.)
- **ParseNode**: Represents a node in the parsed YAML tree with location tracking for error reporting

#### 3. Stream Operations

Operations are defined in `io.axual.ksml.operation` and follow a consistent pattern:

- Each operation implements `StreamOperation` interface
- Operations are applied to `StreamWrapper` instances which delegate to the underlying Kafka Streams objects
- StreamWrapper types: `KStreamWrapper`, `KTableWrapper`, `KGroupedStreamWrapper`, `GlobalKTableWrapper`, windowed variants

#### 4. Python Integration

Python functions are executed via GraalVM's polyglot API:

- **PythonContext** (`io.axual.ksml.python.PythonContext`): Manages GraalVM execution context and Python environment
- **PythonFunction**: Wraps Python code snippets for execution
- **Invoker**: Handles invocation of Python functions with data conversion
- **Bridge classes** (LoggerBridge, MetricsBridge, etc.): Expose Java functionality to Python code

Python functions in YAML can access:
- `value`: Current record value
- `key`: Current record key
- `log`: Logger instance
- `metrics`: Metrics bridge for custom metrics

#### 5. Data Type System

KSML has its own data type abstraction in `ksml-data`:

- **DataObject**: Universal container for all data types
- **DataSchema**: Schema representation independent of format (Avro, JSON Schema, etc.)
- **DataObjectMapper**: Bidirectional mapping between native formats and DataObject
- **Notation**: Abstraction for different serialization formats (registered in NotationLibrary)

#### 6. Runners

Two runner types in `ksml-runner`:

- **KafkaStreamsRunner**: Executes stream processing pipelines (continuous)
- **KafkaProducerRunner**: Executes producer definitions (batch or scheduled)

Both implement `Runner` interface with state management (CREATED → STARTING → STARTED → STOPPING → STOPPED/FAILED).

#### 7. State Store Management

- **StateStoreRegistry**: Global registry of state stores defined in KSML
- **StateStores**: Factory for creating Kafka Streams state stores
- State stores can be queried via the REST API when queryable is enabled

## Testing

### Test Structure

Tests use JUnit 5 with custom KSML testing infrastructure:

```java
@ExtendWith(KSMLTestExtension.class)
public class MyTest {
    @KSMLTest(topology = "pipelines/my-pipeline.yaml")
    @KSMLTopic(topic = "input-topic")
    @KSMLTopic(topic = "output-topic")
    void testMyPipeline(KSMLTopologyTest test) {
        // Test implementation using test.send() and test.receive()
    }
}
```

### Test Annotations

- `@ExtendWith(KSMLTestExtension.class)`: Enables KSML test framework
- `@KSMLTest(topology = "path")`: Specifies the KSML definition to test
- `@KSMLTopic(topic = "name")`: Declares topics needed for the test

### Test Coverage Requirements

- Maintain at least 70% code coverage
- Include unit or integration test that reproduces any bug being fixed
- Run `mvn test` to verify coverage with JaCoCo

## KSML Definition Structure

A typical KSML definition file contains:

```yaml
# Optional schema validation
$schema: https://raw.githubusercontent.com/Axual/ksml/refs/heads/release/1.0.x/docs/ksml-language-spec.json

# Pipeline definitions (for stream processing)
pipelines:
  pipeline_name:
    from:                          # Source
      topic: input-topic
      keyType: string
      valueType: avro:SchemaName
    via:                           # Operations (optional)
      - type: filter
        if:
          expression: value["field"] > 100
      - type: transform
        mapper:
          expression: value["newField"] = value["oldField"] * 2
    to:                           # Sink
      topic: output-topic

# Producer definitions (for data generation)
producers:
  producer_name:
    topic: target-topic
    keyType: string
    valueType: json
    key: "fixed-key"
    value:
      expression: {"timestamp": now(), "data": "test"}
```

## Common Development Tasks

### Adding a New Stream Operation

1. Create operation class in `ksml/src/main/java/io/axual/ksml/operation/`
2. Implement `StreamOperation` interface
3. Add operation logic to appropriate `StreamWrapper` subclass (e.g., `KStreamWrapper`)
4. Create parser in `ksml/src/main/java/io/axual/ksml/definition/parser/`
5. Register parser in the parser registry
6. Add tests in `ksml/src/test/java/io/axual/ksml/operation/`

### Adding Support for a New Data Format

1. Create new module: `ksml-data-<format>`
2. Implement `DataObjectMapper` for bidirectional conversion
3. Implement `DataSchemaMapper` for schema conversion
4. Create Serde implementations extending Kafka's Serializer/Deserializer
5. Register in NotationFactories (`ksml-runner`)

### Working with GraalVM Python Functions

- Python code runs in isolated GraalVM contexts
- Use `PythonContext` to manage execution environments
- Bridge classes expose Java APIs to Python (metrics, logging, state stores)
- Python expressions are evaluated in the context of record processing

## Important Configuration Files

- **pom.xml** (root): Parent POM with dependency management, requires Java 21 and GraalVM
- **ksml-runner.yaml**: Configuration for running KSML (Kafka connection, definitions, error handling)
- **docker-compose.yml**: Local development environment with Kafka and dependencies

## Code Style

Follow the Google Java Style Guide as specified in CONTRIBUTING.md.

## Metrics and Observability

- KSML exposes Prometheus metrics at `http://localhost:9999/metrics` (configurable)
- Custom KSML metrics are prefixed with `ksml_`
- Native Kafka metrics are prefixed with `kafka_`
- Python functions can emit custom metrics via the `metrics` bridge object

## REST Query API

When enabled, KSML exposes REST endpoints for querying state stores:
- Key-value stores: `/store/<store-name>/key/<key>`
- Windowed stores: `/store/<store-name>/window/<key>/<from>/<to>`
- State info: `/state`
