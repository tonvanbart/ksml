package io.axual.ksml.runner.backend;

/*-
 * ========================LICENSE_START=================================
 * KSML Runner
 * %%
 * Copyright (C) 2021 - 2024 Axual B.V.
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
import com.google.common.collect.ImmutableMap;
import io.axual.ksml.data.notation.NotationLibrary;
import io.axual.ksml.data.notation.binary.BinaryNotation;
import io.axual.ksml.data.notation.json.JsonDataObjectConverter;
import io.axual.ksml.data.notation.json.JsonNotation;
import io.axual.ksml.data.parser.ParseNode;
import io.axual.ksml.definition.parser.TopologyDefinitionParser;
import io.axual.ksml.generator.TopologyDefinition;
import io.axual.ksml.generator.YAMLObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.graalvm.home.Version;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@EnabledIf(value = "isRunningOnGraalVM", disabledReason = "This test needs GraalVM to work")
class KafkaProducerRunnerTest {

    MockProducer<byte[], byte[]> mockProducer = new MockProducer<>(true, new ByteArraySerializer(), new ByteArraySerializer());

    private KafkaProducerRunner producerRunner;

    @Test
    @DisplayName("when `interval` is omitted only 1 record is produced")
    void verifySingleShot() throws Exception {
        // given a topology with a single shot produce and a runner for it
        var topologyDefinitionMap = loadDefinitions("produce-test-single.yaml");
        var testConfig = new KafkaProducerRunner.Config(topologyDefinitionMap, new HashMap<>());
        producerRunner = runnerUnderTest(testConfig);

        // when the runner executes, only one record is produced.
        try (ScheduledExecutorService stopper = Executors.newSingleThreadScheduledExecutor()) {
            // schedule a stop for the runner loop, and result verification
            // note: allow some time for parsing and runner setup to happen!
            stopper.schedule(() -> {
                log.info("scheduled stop");
                producerRunner.stop();
                log.info("history size={}", mockProducer.history().size());
                assertEquals(1, mockProducer.history().size(), "only 1 record should be produced");
            }, 10, TimeUnit.SECONDS);

            // start the test
            producerRunner.run();
        }
    }

    /**
     * Load a topology definition from the given file in test/resources
     * @param filename ksml definition file
     * @return a Map containing the parsed definition
     * @throws IOException if loading fails
     * @throws URISyntaxException for invalid file name
     */
    private Map<String, TopologyDefinition> loadDefinitions(String filename) throws IOException, URISyntaxException {
        final var jsonNotation = new JsonNotation();
        NotationLibrary.register(BinaryNotation.NOTATION_NAME, new BinaryNotation(jsonNotation::serde), null);
        NotationLibrary.register(JsonNotation.NOTATION_NAME, jsonNotation, new JsonDataObjectConverter());

        final var uri = ClassLoader.getSystemResource(filename).toURI();
        final var path = Paths.get(uri);
        final var definition = YAMLObjectMapper.INSTANCE.readValue(Files.readString(path), JsonNode.class);
        return ImmutableMap.of("definition",
                new TopologyDefinitionParser("test").parse(ParseNode.fromRoot(definition, "test")));
    }

    /**
     * Create a KafkaProducerRunner from the given config, but with a mock Kafka producer.
     * @param config a {@link io.axual.ksml.runner.backend.KafkaProducerRunner.Config}.
     * @return a {@link KafkaProducerRunner} with a mocked producer.
     */
    private KafkaProducerRunner runnerUnderTest(KafkaProducerRunner.Config config) {
        return new KafkaProducerRunner(config) {
            @Override
            protected Producer<byte[], byte[]> createProducer(Map<String, Object> config) {
                return mockProducer;
            }
        };
    }

    static boolean isRunningOnGraalVM() {
        return Version.getCurrent().isRelease();
    }
}