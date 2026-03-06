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
 * Defines a branch in a pipeline's branch operation.
 */
@Getter
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonClassDescription("Defines a conditional branch in a pipeline. Records matching the condition are routed to the specified destination.")
public class BranchDefinition {
    @JsonProperty(value = "if", required = false)
    @JsonPropertyDescription("Predicate function reference or inline definition for the branch condition. If not specified, this is the default branch that catches all unmatched records.")
    private final Object predicate;

    @JsonProperty(value = "to", required = false)
    @JsonPropertyDescription("Target stream reference (string) or inline stream definition for this branch's output.")
    private final Object to;

    @JsonProperty(value = "forEach", required = false)
    @JsonPropertyDescription("ForEach function for terminal processing of this branch (alternative to 'to').")
    private final Object forEach;
}
