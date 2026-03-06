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

import com.fasterxml.jackson.databind.JsonNode;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;

import java.io.File;
import java.io.PrintWriter;

/**
 * Generates a JSON Schema from the KSML definition model classes.
 * <p>
 * This class is invoked during the Maven build to generate a JSON Schema file
 * that can be used to validate KSML definition YAML files.
 * </p>
 * <p>
 * Usage: java KsmlDefinitionSchemaGenerator [output-file]
 * </p>
 * <p>
 * If no output file is specified, the schema is printed to stdout.
 * </p>
 */
public class KsmlDefinitionSchemaGenerator {

    public static void main(String[] args) {
        String outputFile = args.length > 0 ? args[0] : null;
        generateSchema(outputFile);
    }

    /**
     * Generates the JSON Schema for the KsmlDefinition class.
     *
     * @param outputFile the file to write the schema to, or null to print to stdout
     */
    public static void generateSchema(String outputFile) {
        JacksonModule jacksonModule = new JacksonModule(
                JacksonOption.RESPECT_JSONPROPERTY_REQUIRED,
                JacksonOption.FLATTENED_ENUMS_FROM_JSONPROPERTY
        );

        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(
                SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON)
                .with(jacksonModule)
                .with(Option.MAP_VALUES_AS_ADDITIONAL_PROPERTIES,
                        Option.DEFINITIONS_FOR_ALL_OBJECTS,
                        Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT);

        SchemaGeneratorConfig config = configBuilder.build();
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode jsonSchema = generator.generateSchema(KsmlDefinition.class);
        String schema = jsonSchema.toPrettyString();

        if (outputFile != null && !outputFile.isBlank()) {
            try {
                File file = new File(outputFile);
                File parentDir = file.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                try (PrintWriter writer = new PrintWriter(file)) {
                    writer.println(schema);
                    System.out.println("KSML Model JSON schema written to file: " + outputFile);
                }
            } catch (Exception e) {
                System.err.println("Error writing KSML Model JSON schema to file: " + outputFile);
                System.err.println("Error: " + e.getMessage());
                System.exit(1);
            }
        } else {
            System.out.println(schema);
        }
    }
}
