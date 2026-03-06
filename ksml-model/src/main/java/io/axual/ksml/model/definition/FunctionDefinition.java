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
 * Defines a reusable function that can be used in pipelines.
 */
@Getter
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonClassDescription("""
        Defines a reusable function containing Python code for stream processing operations.
        Functions have a type that determines their implicit parameters and expected return value.
        """)
public class FunctionDefinition {
    @JsonProperty(value = "type", required = false)
    @JsonPropertyDescription("""
            The type of function. Determines implicit parameters and expected return type.
            Types: 'forEach', 'predicate', 'valueJoiner', 'generator', 'keyValueTransformer',
            'keyValueToKeyValueListTransformer', 'keyValueToValueListTransformer', 'streamPartitioner',
            'metadataTransformer'. Default is 'general' if not specified.
            """)
    private final String type;

    @JsonProperty(value = "parameters", required = false)
    @JsonPropertyDescription("Additional parameters for the function beyond the implicit parameters from the function type.")
    private final List<FunctionParameterDefinition> parameters;

    @JsonProperty(value = "globalCode", required = false)
    @JsonPropertyDescription("Python code executed once at initialization. Use for imports, global variables, and helper function definitions.")
    private final String globalCode;

    @JsonProperty(value = "code", required = false)
    @JsonPropertyDescription("Python code executed for each invocation. Has access to implicit parameters based on function type (e.g., 'key', 'value' for forEach).")
    private final String code;

    @JsonProperty(value = "expression", required = false)
    @JsonPropertyDescription("Python expression that returns the result value. Evaluated after 'code' executes.")
    private final String expression;

    @JsonProperty(value = "resultType", required = false)
    @JsonPropertyDescription("The return type of the function. Examples: 'string', 'json', 'boolean', 'tuple(string, json)', 'list(tuple(json, json))'.")
    private final String resultType;

    @JsonProperty(value = "stores", required = false)
    @JsonPropertyDescription("List of store names this function can access. The stores become available as variables in the Python code.")
    private final List<String> stores;
}
