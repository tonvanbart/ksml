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

import io.axual.ksml.definition.TestGivenDefinition;
import io.axual.ksml.definition.TestMessageDefinition;
import io.axual.ksml.generator.TopologyResources;
import io.axual.ksml.parser.StructsParser;
import io.axual.ksml.parser.TopologyResourceAwareParser;

import java.util.List;

import static io.axual.ksml.dsl.KSMLDSL.Tests;

public class TestGivenDefinitionParser extends TopologyResourceAwareParser<TestGivenDefinition> {
    public TestGivenDefinitionParser(TopologyResources resources) {
        super(resources);
    }

    @Override
    public StructsParser<TestGivenDefinition> parser() {
        return structsParser(
                TestGivenDefinition.class,
                "",
                "Input data section of a test definition",
                optional(listField(Tests.Given.MESSAGES, "message", "message", "Inline test messages", new TestMessageDefinitionParser())),
                optional(functionField(Tests.Given.GENERATOR, "A generator function to produce test messages", new GeneratorDefinitionParser(false))),
                optional(longField(Tests.Given.COUNT, "Number of messages to generate when using a generator function")),
                (messages, generator, count, tags) -> new TestGivenDefinition(messages != null ? messages : List.of(), generator, count));
    }
}
