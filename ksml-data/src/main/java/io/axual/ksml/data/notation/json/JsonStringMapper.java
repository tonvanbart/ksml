package io.axual.ksml.data.notation.json;

/*-
 * ========================LICENSE_START=================================
 * KSML
 * %%
 * Copyright (C) 2021 - 2023 Axual B.V.
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
import io.axual.ksml.data.exception.DataException;
import io.axual.ksml.data.notation.binary.JsonNodeNativeMapper;
import io.axual.ksml.data.notation.string.StringMapper;

import java.io.IOException;
import java.io.StringWriter;

public class JsonStringMapper implements StringMapper<Object> {
    protected final ObjectMapper mapper = new ObjectMapper();
    private static final JsonNodeNativeMapper NATIVE_MAPPER = new JsonNodeNativeMapper();

    @Override
    public Object fromString(String value) {
        if (value == null) return null; // Allow null strings as input, returning null as native output
        try {
            var tree = mapper.readTree(value);
            return NATIVE_MAPPER.toNative(tree);
        } catch (Exception mapException) {
            throw new DataException("Could not parse string to object: " + value);
        }
    }

    @Override
    public String toString(Object value) {
        if (value == null) return null; // Allow null as native input, return null string as output
        try {
            final var writer = new StringWriter();
            mapper.writeTree(mapper.createGenerator(writer), NATIVE_MAPPER.fromNative(value));
            return writer.toString();
        } catch (IOException e) {
            throw new DataException("Can not convert object to JSON string: " + value.toString(), e);
        }
    }
}
