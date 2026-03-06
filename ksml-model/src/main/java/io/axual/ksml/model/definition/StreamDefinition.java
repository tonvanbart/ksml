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
 * Defines a Kafka stream source or sink.
 */
@Getter
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonClassDescription("Defines a Kafka stream source or sink with topic, key/value types, and optional configuration.")
public class StreamDefinition {
    @JsonProperty(value = "topic", required = true)
    @JsonPropertyDescription("The Kafka topic name for this stream.")
    private final String topic;

    @JsonProperty(value = "keyType", required = true)
    @JsonPropertyDescription("The type of the message key. Examples: 'string', 'long', 'json', 'avro:SchemaName', 'protobuf:MessageName'.")
    private final String keyType;

    @JsonProperty(value = "valueType", required = true)
    @JsonPropertyDescription("The type of the message value. Examples: 'string', 'json', 'avro:SchemaName', 'protobuf:MessageName', 'csv:SchemaName', 'xml:SchemaName'.")
    private final String valueType;

    @JsonProperty(value = "offsetResetPolicy", required = false)
    @JsonPropertyDescription("The offset reset policy when no committed offset exists. Values: 'earliest' or 'latest'.")
    private final OffsetResetPolicy offsetResetPolicy;

    @JsonProperty(value = "timestampExtractor", required = false)
    @JsonPropertyDescription("Custom timestamp extractor for extracting event time from records.")
    private final TimestampExtractorDefinition timestampExtractor;

    @JsonProperty(value = "partitioner", required = false)
    @JsonPropertyDescription("Reference to a custom stream partitioner function for controlling partition assignment when producing.")
    private final String partitioner;
}
