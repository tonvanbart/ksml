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

import java.util.List;

/**
 * Defines a processing pipeline that consumes from a source and processes records.
 */
@Getter
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonClassDescription("""
        Defines a stream processing pipeline.
        A pipeline consumes from a source (stream, table, or another pipeline), applies transformations,
        and writes to a sink (stream, table, forEach, or branches to multiple destinations).
        """)
public class PipelineDefinition {
    @JsonProperty(value = "from", required = true)
    @JsonPropertyDescription("The source for this pipeline. Can be a stream/table name reference (string), an inline stream definition, or another pipeline's name.")
    private final Object from;

    @JsonProperty(value = "via", required = false)
    @JsonPropertyDescription("List of transformation steps to apply to records. Each step has a type and type-specific configuration.")
    private final List<PipelineStepDefinition> via;

    @JsonProperty(value = "to", required = false)
    @JsonPropertyDescription("Target stream or table reference (string) or inline definition for output. Mutually exclusive with forEach, branch, and toTopicNameExtractor.")
    private final Object to;

    @JsonProperty(value = "forEach", required = false)
    @JsonPropertyDescription("Terminal forEach operation. Can be a function reference (string) or inline function definition. Mutually exclusive with to, branch, and toTopicNameExtractor.")
    private final Object forEach;

    @JsonProperty(value = "branch", required = false)
    @JsonPropertyDescription("Branch definitions for conditional routing to multiple destinations. Mutually exclusive with to, forEach, and toTopicNameExtractor.")
    private final List<BranchDefinition> branch;

    @JsonProperty(value = "toTopicNameExtractor", required = false)
    @JsonPropertyDescription("Dynamic topic routing configuration. Uses a function to determine the target topic for each record. Mutually exclusive with to, forEach, and branch.")
    private final TopicNameExtractorDefinition toTopicNameExtractor;

    @JsonProperty(value = "as", required = false)
    @JsonPropertyDescription("Save the pipeline output under this name for linking to other pipelines. Other pipelines can use this name in their 'from' field.")
    private final String as;
}
