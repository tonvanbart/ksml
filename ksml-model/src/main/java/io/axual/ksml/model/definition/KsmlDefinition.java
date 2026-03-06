package io.axual.ksml.model.definition;

/*-
 * ========================LICENSE_START=================================
 * KSML Model
 * %%
 * Copyright (C) 2021 - 2025 Axual B.V.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

/**
 * Root class for a KSML definition file.
 * A KSML definition describes Kafka Streams topologies using YAML.
 */
@Getter
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonClassDescription("""
        Root element for a KSML definition file.
        A KSML definition describes Kafka Streams topologies using YAML and Python.
        It contains sections for streams, tables, stores, functions, pipelines, and producers.
        """)
public class KsmlDefinition {
    @JsonProperty(value = "$schema", required = false)
    @JsonPropertyDescription("JSON Schema reference for validation (e.g., 'https://axual.github.io/ksml/latest/ksml-language-spec.json')")
    private final String schema;

    @JsonProperty(value = "streams", required = false)
    @JsonPropertyDescription("Named stream definitions. Each stream defines a Kafka topic with its key and value types.")
    private final Map<String, StreamDefinition> streams;

    @JsonProperty(value = "tables", required = false)
    @JsonPropertyDescription("Named table definitions for compacted topics. Tables provide key-value lookup semantics.")
    private final Map<String, TableDefinition> tables;

    @JsonProperty(value = "stores", required = false)
    @JsonPropertyDescription("Named state store definitions. Stores provide local state for stream processing operations.")
    private final Map<String, StoreDefinition> stores;

    @JsonProperty(value = "functions", required = false)
    @JsonPropertyDescription("Named function definitions. Functions contain reusable Python code for stream processing operations.")
    private final Map<String, FunctionDefinition> functions;

    @JsonProperty(value = "pipelines", required = false)
    @JsonPropertyDescription("Named pipeline definitions. Pipelines define the stream processing topology with sources, transformations, and sinks.")
    private final Map<String, PipelineDefinition> pipelines;

    @JsonProperty(value = "producers", required = false)
    @JsonPropertyDescription("Named producer definitions for data generation. Producers generate records at regular intervals using generator functions.")
    private final Map<String, ProducerDefinition> producers;
}
