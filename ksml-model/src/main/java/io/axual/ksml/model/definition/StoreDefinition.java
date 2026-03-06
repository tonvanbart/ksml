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
 * Defines a state store for use in pipelines.
 */
@Getter
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonClassDescription("State store definition for storing local state during stream processing. Supports key-value and windowed stores.")
public class StoreDefinition {
    @JsonProperty(value = "type", required = true)
    @JsonPropertyDescription("The type of store: 'keyValue' for simple key-value storage, 'window' for windowed state.")
    private final StoreType type;

    @JsonProperty(value = "keyType", required = true)
    @JsonPropertyDescription("The type of the store key. Examples: 'string', 'long', 'json'.")
    private final String keyType;

    @JsonProperty(value = "valueType", required = true)
    @JsonPropertyDescription("The type of the store value. Examples: 'string', 'long', 'json'.")
    private final String valueType;

    @JsonProperty(value = "persistent", required = false)
    @JsonPropertyDescription("Whether the store is persistent (survives application restarts). Default depends on store type.")
    private final Boolean persistent;

    @JsonProperty(value = "historyRetention", required = false)
    @JsonPropertyDescription("History retention duration for versioned stores. Format: duration string (e.g., '1h', '30m', '7d').")
    private final String historyRetention;

    @JsonProperty(value = "caching", required = false)
    @JsonPropertyDescription("Whether caching is enabled for the store. Caching can improve performance but may delay downstream updates.")
    private final Boolean caching;

    @JsonProperty(value = "logging", required = false)
    @JsonPropertyDescription("Whether changelog logging is enabled. When true, changes are written to a changelog topic for fault tolerance.")
    private final Boolean logging;

    @JsonProperty(value = "windowSize", required = false)
    @JsonPropertyDescription("Window size duration for window stores. Format: duration string (e.g., '10m', '1h', '2s').")
    private final String windowSize;

    @JsonProperty(value = "retention", required = false)
    @JsonPropertyDescription("Retention duration for window stores. Old windows are removed after this period. Format: duration string (e.g., '1h', '3s').")
    private final String retention;

    @JsonProperty(value = "timestamped", required = false)
    @JsonPropertyDescription("Whether the window store records timestamps. Required for some join operations.")
    private final Boolean timestamped;

    @JsonProperty(value = "retainDuplicates", required = false)
    @JsonPropertyDescription("Whether to retain duplicate keys within a window. When true, all records are kept; when false, only the latest per key.")
    private final Boolean retainDuplicates;
}
