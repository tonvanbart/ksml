package io.axual.ksml;

/*-
 * ========================LICENSE_START=================================
 * KSML
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
import io.axual.ksml.data.mapper.NativeDataObjectMapper;
import io.axual.ksml.data.notation.NotationLibrary;
import io.axual.ksml.data.notation.avro.AvroSchemaLoader;
import io.axual.ksml.data.notation.avro.MockAvroNotation;
import io.axual.ksml.data.notation.binary.BinaryNotation;
import io.axual.ksml.data.notation.json.JsonNotation;
import io.axual.ksml.data.notation.json.JsonSchemaLoader;
import io.axual.ksml.data.parser.ParseNode;
import io.axual.ksml.definition.parser.TopologyDefinitionParser;
import io.axual.ksml.generator.YAMLObjectMapper;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TopologyDescription;
import org.apache.kafka.streams.TopologyTestDriver;
import org.graalvm.home.Version;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Basic stream run test.
 * See {@link io.axual.ksml.testutil.KSMLTestExtension} for a solution that abstracts away the boilerplate code.
 */
@EnabledIf(value = "onGraalVM", disabledReason = "This test needs GraalVM to run")
public class BasicStreamRunTest {

    private final StreamsBuilder streamsBuilder = new StreamsBuilder();

    @Test
    void parseAndCheckOuput() throws Exception {
        final var mapper = new NativeDataObjectMapper();
        final var jsonNotation = new JsonNotation(mapper, new JsonSchemaLoader("."));
        NotationLibrary.register(BinaryNotation.NAME, new BinaryNotation(mapper, jsonNotation::serde));
        NotationLibrary.register(JsonNotation.NAME, jsonNotation);

        final var uri = ClassLoader.getSystemResource("pipelines/test-copying.yaml").toURI();
        final var path = Paths.get(uri);
        final var definition = YAMLObjectMapper.INSTANCE.readValue(Files.readString(path), JsonNode.class);
        final var definitions = ImmutableMap.of("definition",
                new TopologyDefinitionParser("test").parse(ParseNode.fromRoot(definition, "test")));
        var topologyGenerator = new TopologyGenerator("some.app.id");
        final var topology = topologyGenerator.create(streamsBuilder, definitions);
        final TopologyDescription description = topology.describe();
        System.out.println(description);

        try (TopologyTestDriver driver = new TopologyTestDriver(topology)) {
            TestInputTopic<String, String> inputTopic = driver.createInputTopic("ksml_sensordata_avro", new StringSerializer(), new StringSerializer());
            var outputTopic = driver.createOutputTopic("ksml_sensordata_copy", new StringDeserializer(), new StringDeserializer());
            inputTopic.pipeInput("key1", "value1");
            var keyValue = outputTopic.readKeyValue();
            assertEquals("key1", keyValue.key);
            assertEquals("value1", keyValue.value);
        }
    }

    @Test
    void testFilterAvroRecords() throws Exception {
        final var mapper = new NativeDataObjectMapper();
        final var jsonNotation = new JsonNotation(mapper, new JsonSchemaLoader("."));
        NotationLibrary.register(BinaryNotation.NAME, new BinaryNotation(mapper, jsonNotation::serde));
        NotationLibrary.register(JsonNotation.NAME, jsonNotation);

        URI testDirectory = ClassLoader.getSystemResource("pipelines").toURI();
        String schemaPath = testDirectory.getPath();
        System.out.println("schemaPath = " + schemaPath);
        final var avroNotation = new MockAvroNotation(new HashMap<>(), new AvroSchemaLoader(schemaPath));
        NotationLibrary.register(MockAvroNotation.NAME, avroNotation);

        final var uri = ClassLoader.getSystemResource("pipelines/test-filtering.yaml").toURI();
        final var path = Paths.get(uri);
        final var definition = YAMLObjectMapper.INSTANCE.readValue(Files.readString(path), JsonNode.class);
        final var definitions = ImmutableMap.of("definition",
                new TopologyDefinitionParser("test").parse(ParseNode.fromRoot(definition, "test")));
        var topologyGenerator = new TopologyGenerator("some.app.id");
        final var topology = topologyGenerator.create(streamsBuilder, definitions);
        final TopologyDescription description = topology.describe();
        System.out.println(description);

        try (TopologyTestDriver driver = new TopologyTestDriver(topology)) {
            final var kafkaAvroSerializer = new KafkaAvroSerializer(avroNotation.mockSchemaRegistryClient());
            kafkaAvroSerializer.configure(avroNotation.getSchemaRegistryConfigs(), false);

            final var kafkaAvroDeserializer = new KafkaAvroDeserializer(avroNotation.mockSchemaRegistryClient());
            kafkaAvroDeserializer.configure(avroNotation.getSchemaRegistryConfigs(), false);

            TestInputTopic<String, Object> avroInputTopic = driver.createInputTopic("ksml_sensordata_avro", new StringSerializer(), kafkaAvroSerializer);
            var outputTopic = driver.createOutputTopic("ksml_sensordata_filtered", new StringDeserializer(), kafkaAvroDeserializer);

            SensorData build = SensorData.builder().name("name").timestamp(System.currentTimeMillis()).value("AMS").type(SensorData.SensorType.AREA).unit("u").color("blue").build();

            avroInputTopic.pipeInput("key1", build.toRecord());
            if (!outputTopic.isEmpty()) {
                var keyValue = outputTopic.readKeyValue();
                assertEquals("key1", keyValue.key);
                System.out.printf("Output topic key=%s, value=%s\n", keyValue.key, keyValue.value);
            }
        }
    }

    static boolean onGraalVM() {
        return Version.getCurrent().isRelease();
    }
}
