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

/**
 * Defines a single step in a pipeline's "via" section.
 * Different step types use different properties.
 */
@Getter
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonClassDescription("""
        Defines a single transformation step in a pipeline.
        The 'type' property determines which other properties are applicable.
        """)
public class PipelineStepDefinition {
    @JsonProperty(value = "name", required = false)
    @JsonPropertyDescription("Optional name for this step. Useful for debugging and documentation.")
    private final String name;

    @JsonProperty(value = "type", required = true)
    @JsonPropertyDescription("""
            The type of operation. Values: 'filter', 'filterNot', 'map', 'mapKey', 'mapValue', 'mapValues',
            'peek', 'groupBy', 'groupByKey', 'aggregate', 'reduce', 'count', 'join', 'leftJoin', 'outerJoin',
            'windowByTime', 'toStream', 'toTable', 'transformKey', 'transformValue', 'transformKeyValue',
            'transformMetadata', 'transformKeyValueToKeyValueList', 'transformKeyValueToValueList',
            'flatMap', 'flatMapValues', 'selectKey', 'repartition', 'merge', 'suppress'.
            """)
    private final String type;

    // ---- Filter properties ----
    @JsonProperty(value = "if", required = false)
    @JsonPropertyDescription("Predicate function reference or inline definition for filter operations. Records matching the predicate pass through.")
    private final Object predicate;

    // ---- Peek/ForEach properties ----
    @JsonProperty(value = "forEach", required = false)
    @JsonPropertyDescription("ForEach function reference or inline definition for peek/forEach operations. Executes for each record without modifying the stream.")
    private final Object forEach;

    // ---- Mapper properties (for map, transformKeyValue, etc.) ----
    @JsonProperty(value = "mapper", required = false)
    @JsonPropertyDescription("Mapper function reference or inline definition for transformation operations.")
    private final Object mapper;

    // ---- Window properties ----
    @JsonProperty(value = "windowType", required = false)
    @JsonPropertyDescription("Window type for windowByTime operations. Values: 'tumbling', 'hopping', 'sliding', 'session'.")
    private final WindowType windowType;

    @JsonProperty(value = "duration", required = false)
    @JsonPropertyDescription("Window duration for time-based windows. Format: duration string (e.g., '20s', '5m', '1h').")
    private final String duration;

    @JsonProperty(value = "advanceBy", required = false)
    @JsonPropertyDescription("Advance duration for hopping windows. Format: duration string (e.g., '5s', '1m').")
    private final String advanceBy;

    @JsonProperty(value = "grace", required = false)
    @JsonPropertyDescription("Grace period duration for late-arriving records. Format: duration string (e.g., '1s', '30s').")
    private final String grace;

    // ---- Aggregate properties ----
    @JsonProperty(value = "store", required = false)
    @JsonPropertyDescription("Store reference (string) or inline store definition for aggregate operations.")
    private final Object store;

    @JsonProperty(value = "initializer", required = false)
    @JsonPropertyDescription("Initializer function for aggregate operations. Returns the initial aggregation value.")
    private final InlineFunctionDefinition initializer;

    @JsonProperty(value = "aggregator", required = false)
    @JsonPropertyDescription("Aggregator function for aggregate operations. Combines the current value with the aggregated value.")
    private final InlineFunctionDefinition aggregator;

    // ---- Join properties ----
    @JsonProperty(value = "table", required = false)
    @JsonPropertyDescription("Table reference for stream-table join operations.")
    private final String table;

    @JsonProperty(value = "stream", required = false)
    @JsonPropertyDescription("Stream reference for stream-stream join operations.")
    private final String stream;

    @JsonProperty(value = "valueJoiner", required = false)
    @JsonPropertyDescription("Value joiner function reference or inline definition for join operations.")
    private final Object valueJoiner;

    @JsonProperty(value = "timeDifference", required = false)
    @JsonPropertyDescription("Time difference window for stream-stream joins. Format: duration string (e.g., '1s', '5m').")
    private final String timeDifference;

    @JsonProperty(value = "thisStore", required = false)
    @JsonPropertyDescription("Store reference for this side of a stream-stream join.")
    private final String thisStore;

    @JsonProperty(value = "otherStore", required = false)
    @JsonPropertyDescription("Store reference for the other side of a stream-stream join.")
    private final String otherStore;

    // ---- Reduce properties ----
    @JsonProperty(value = "reducer", required = false)
    @JsonPropertyDescription("Reducer function for reduce operations.")
    private final Object reducer;

    @JsonProperty(value = "adder", required = false)
    @JsonPropertyDescription("Adder function for reduce operations (adds a new record to the aggregate).")
    private final Object adder;

    @JsonProperty(value = "subtractor", required = false)
    @JsonPropertyDescription("Subtractor function for reduce operations (removes a record from the aggregate).")
    private final Object subtractor;
}
