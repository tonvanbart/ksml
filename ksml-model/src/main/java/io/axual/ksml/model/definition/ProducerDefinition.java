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
 * Defines a data producer that generates records at regular intervals.
 */
@Getter
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonClassDescription("Defines a data producer that generates records at regular intervals using a generator function.")
public class ProducerDefinition {
    @JsonProperty(value = "generator", required = true)
    @JsonPropertyDescription("Reference to the generator function that produces records. The function should return a list of key-value tuples.")
    private final String generator;

    @JsonProperty(value = "interval", required = true)
    @JsonPropertyDescription("Interval between message generations. Format: duration string (e.g., '3s', '1m', '500ms').")
    private final String interval;

    @JsonProperty(value = "to", required = true)
    @JsonPropertyDescription("Target stream definition. Can be a stream name reference (string) or an inline stream definition with topic, keyType, and valueType.")
    private final Object to;
}
