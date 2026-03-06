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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Path;

/**
 * Utility class for loading KSML definition files.
 */
public class KsmlDefinitionLoader {
    private final ObjectMapper objectMapper;

    /**
     * Creates a new loader with default YAML configuration.
     */
    public KsmlDefinitionLoader() {
        this.objectMapper = new ObjectMapper(new YAMLFactory());
    }

    /**
     * Creates a new loader with a custom ObjectMapper.
     *
     * @param objectMapper the ObjectMapper to use (should be configured for YAML)
     */
    public KsmlDefinitionLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Loads a KSML definition from a file.
     *
     * @param file the file to load
     * @return the parsed KSML definition
     * @throws IOException if an I/O error occurs
     */
    public KsmlDefinition load(File file) throws IOException {
        return objectMapper.readValue(file, KsmlDefinition.class);
    }

    /**
     * Loads a KSML definition from a path.
     *
     * @param path the path to load
     * @return the parsed KSML definition
     * @throws IOException if an I/O error occurs
     */
    public KsmlDefinition load(Path path) throws IOException {
        return load(path.toFile());
    }

    /**
     * Loads a KSML definition from an input stream.
     *
     * @param inputStream the input stream to load
     * @return the parsed KSML definition
     * @throws IOException if an I/O error occurs
     */
    public KsmlDefinition load(InputStream inputStream) throws IOException {
        return objectMapper.readValue(inputStream, KsmlDefinition.class);
    }

    /**
     * Loads a KSML definition from a reader.
     *
     * @param reader the reader to load
     * @return the parsed KSML definition
     * @throws IOException if an I/O error occurs
     */
    public KsmlDefinition load(Reader reader) throws IOException {
        return objectMapper.readValue(reader, KsmlDefinition.class);
    }

    /**
     * Loads a KSML definition from a YAML string.
     *
     * @param yaml the YAML string to parse
     * @return the parsed KSML definition
     * @throws IOException if a parsing error occurs
     */
    public KsmlDefinition loadFromString(String yaml) throws IOException {
        return objectMapper.readValue(yaml, KsmlDefinition.class);
    }

    /**
     * Returns the ObjectMapper used by this loader.
     *
     * @return the ObjectMapper
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
