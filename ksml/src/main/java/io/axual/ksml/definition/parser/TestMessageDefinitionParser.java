package io.axual.ksml.definition.parser;

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

import io.axual.ksml.data.schema.DataSchema;
import io.axual.ksml.definition.TestMessageDefinition;
import io.axual.ksml.parser.DefinitionParser;
import io.axual.ksml.parser.ParseNode;
import io.axual.ksml.parser.ParserWithSchemas;
import io.axual.ksml.parser.StructsParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.axual.ksml.dsl.KSMLDSL.Tests;

public class TestMessageDefinitionParser extends DefinitionParser<TestMessageDefinition> {
    @Override
    public StructsParser<TestMessageDefinition> parser() {
        return structsParser(
                TestMessageDefinition.class,
                "",
                "A test message with key and value",
                customField(Tests.Message.KEY, "The message key", anyValueParser()),
                customField(Tests.Message.VALUE, "The message value", anyValueParser()),
                (key, value, tags) -> new TestMessageDefinition(key, value));
    }

    private static ParserWithSchemas<Object> anyValueParser() {
        return ParserWithSchemas.of(TestMessageDefinitionParser::parseAnyValue, DataSchema.STRING_SCHEMA);
    }

    /**
     * Converts a ParseNode to a native Java object (String, Boolean, Integer, Long, Double, Map, or List).
     * This allows YAML test messages to use inline objects like: value: {"temperature": 35}
     */
    static Object parseAnyValue(ParseNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isBoolean()) return node.asBoolean();
        if (node.isInt()) return node.asInt();
        if (node.isLong()) return node.asLong();
        if (node.isDouble()) return node.asDouble();
        if (node.isFloat()) return (double) node.asFloat();
        if (node.isString()) return node.asString();
        if (node.isArray()) {
            List<Object> list = new ArrayList<>();
            for (ParseNode child : node.children(null, "")) {
                list.add(parseAnyValue(child));
            }
            return list;
        }
        if (node.isObject()) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (ParseNode child : node.children(null, "")) {
                map.put(child.name(), parseAnyValue(child));
            }
            return map;
        }
        return node.asString();
    }
}
