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
 * Defines an inline function (used within pipeline steps, without a name reference).
 */
@Getter
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonClassDescription("Inline function definition used within pipeline steps. Contains Python code without requiring a separate named function.")
public class InlineFunctionDefinition {
    @JsonProperty(value = "globalCode", required = false)
    @JsonPropertyDescription("Python code executed once at initialization. Use for imports and global state initialization.")
    private final String globalCode;

    @JsonProperty(value = "code", required = false)
    @JsonPropertyDescription("Python code executed for each invocation.")
    private final String code;

    @JsonProperty(value = "expression", required = false)
    @JsonPropertyDescription("Python expression that returns the result value.")
    private final String expression;

    @JsonProperty(value = "resultType", required = false)
    @JsonPropertyDescription("The return type of the function. Examples: 'string', 'long', 'boolean', 'json'.")
    private final String resultType;
}
