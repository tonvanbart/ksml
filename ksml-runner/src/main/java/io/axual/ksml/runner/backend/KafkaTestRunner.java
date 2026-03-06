package io.axual.ksml.runner.backend;

/*-
 * ========================LICENSE_START=================================
 * KSML Runner
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.axual.ksml.TopologyGenerator;
import io.axual.ksml.definition.PipelineDefinition;
import io.axual.ksml.definition.TestDefinition;
import io.axual.ksml.definition.TestGivenDefinition;
import io.axual.ksml.definition.TestMessageDefinition;
import io.axual.ksml.definition.TopicDefinition;
import io.axual.ksml.generator.TopologyDefinition;
import io.axual.ksml.operation.ToOperation;
import io.axual.ksml.python.PythonContext;
import io.axual.ksml.python.PythonContextConfig;
import io.axual.ksml.python.PythonTypeConverter;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TopologyTestDriver;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Runs KSML test definitions using Kafka Streams' TopologyTestDriver.
 * <p>
 * For each test definition:
 * 1. Builds the topology from the pipeline definitions
 * 2. Creates a TopologyTestDriver (in-memory, synchronous, no Kafka needed)
 * 3. Pipes input messages from the "given" section
 * 4. Reads output records and executes Python assertion code from the "then" section
 * <p>
 * Phase 1 limitations:
 * - String keys only (keyType: string)
 * - JSON values serialized as strings
 * - Single output topic only (pipeline must have a "to:" sink)
 */
@Slf4j
public class KafkaTestRunner implements Runner {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private final Config config;
    private State currentState;

    @Builder
    public record Config(
            Map<String, TopologyDefinition> definitions,
            PythonContextConfig pythonContextConfig) {
    }

    public KafkaTestRunner(Config config) {
        this.config = config;
        this.currentState = State.CREATED;
    }

    private synchronized void setState(State newState) {
        if (currentState.isValidNextState(newState)) {
            currentState = newState;
        }
    }

    @Override
    public void run() {
        log.info("Starting KSML test runner");
        setState(State.STARTING);

        int totalPassed = 0;
        int totalFailed = 0;

        try {
            setState(State.STARTED);

            for (var entry : config.definitions.entrySet()) {
                final var defName = entry.getKey();
                final var definition = entry.getValue();

                for (var testEntry : definition.tests().entrySet()) {
                    final var testName = testEntry.getKey();
                    final var testDef = testEntry.getValue();

                    try {
                        runSingleTest(defName, testName, testDef, definition);
                        log.info("TEST PASSED: {}", testName);
                        totalPassed++;
                    } catch (AssertionError e) {
                        log.error("TEST FAILED: {} - {}", testName, e.getMessage());
                        totalFailed++;
                    } catch (Exception e) {
                        log.error("TEST ERROR: {} - {}", testName, e.getMessage(), e);
                        totalFailed++;
                    }
                }
            }

            log.info("Test results: {} passed, {} failed, {} total",
                    totalPassed, totalFailed, totalPassed + totalFailed);

        } catch (Exception e) {
            setState(State.FAILED);
            log.error("Test runner failed", e);
            return;
        }

        setState(State.STOPPED);

        if (totalFailed > 0) {
            throw new RuntimeException(totalFailed + " test(s) failed");
        }
    }

    private void runSingleTest(String defName, String testName,
                               TestDefinition testDef,
                               TopologyDefinition definition) {
        log.info("Running test: {}", testName);

        // Resolve the pipeline being tested
        final var pipeline = definition.pipeline(testDef.pipeline());
        if (pipeline == null) {
            throw new IllegalArgumentException("Pipeline '" + testDef.pipeline() + "' not found in definition '" + defName + "'");
        }

        // Build the topology
        final var streamsBuilder = new StreamsBuilder();
        final var topologyGenerator = new TopologyGenerator(
                testName + ".test",
                null,
                config.pythonContextConfig());
        final var definitions = Map.of(defName, definition);
        final var topology = topologyGenerator.create(streamsBuilder, definitions);

        // Create TopologyTestDriver with minimal config
        final var props = new Properties();
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG,
                org.apache.kafka.common.serialization.Serdes.StringSerde.class.getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,
                org.apache.kafka.common.serialization.Serdes.StringSerde.class.getName());

        try (var testDriver = new TopologyTestDriver(topology, props)) {
            // Resolve input topic from pipeline source
            final var sourceTopicDef = pipeline.source().definition();
            final var inputTopicName = sourceTopicDef.topic();

            // Resolve output topic from pipeline sink
            final var outputTopicName = resolveOutputTopicName(pipeline);
            if (outputTopicName == null) {
                throw new IllegalArgumentException("Pipeline '" + testDef.pipeline()
                        + "' does not have a 'to:' sink. Only pipelines with a topic sink can be tested.");
            }

            // Create test input/output topics
            final var inputTopic = testDriver.createInputTopic(
                    inputTopicName,
                    new StringSerializer(),
                    new StringSerializer());
            final var outputTopic = testDriver.createOutputTopic(
                    outputTopicName,
                    new StringDeserializer(),
                    new StringDeserializer());

            // Execute "given" — pipe input messages
            pipeInputMessages(testDef.given(), inputTopic);

            // Read all output records
            final var outputRecords = outputTopic.readRecordsToList();
            final var results = new ArrayList<Map<String, Object>>();
            for (var outputRecord : outputRecords) {
                var resultMap = new HashMap<String, Object>();
                resultMap.put("key", outputRecord.key());
                // Try to parse value as JSON, fall back to raw string
                resultMap.put("value", parseJsonValue(outputRecord.value()));
                results.add(resultMap);
            }

            // Execute "then" — run Python assertion code
            executePythonAssertions(testName, testDef.then().code(), results);
        }
    }

    private void pipeInputMessages(TestGivenDefinition given,
                                   org.apache.kafka.streams.TestInputTopic<String, String> inputTopic) {
        if (given.messages() != null && !given.messages().isEmpty()) {
            for (TestMessageDefinition msg : given.messages()) {
                final var key = msg.key() != null ? msg.key().toString() : null;
                final var value = serializeValue(msg.value());
                inputTopic.pipeInput(key, value);
            }
        }
        // Generator support is deferred to a later phase
        if (given.generator() != null) {
            log.warn("Generator-based test input is not yet supported in Phase 1. Skipping generator.");
        }
    }

    private String serializeValue(Object value) {
        if (value == null) return null;
        if (value instanceof String s) return s;
        try {
            return JSON_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return value.toString();
        }
    }

    private Object parseJsonValue(String value) {
        if (value == null) return null;
        try {
            return JSON_MAPPER.readValue(value, Object.class);
        } catch (JsonProcessingException e) {
            // Not valid JSON, return as string
            return value;
        }
    }

    private String resolveOutputTopicName(PipelineDefinition pipeline) {
        if (pipeline.sink() instanceof ToOperation toOp) {
            return toOp.topic.topic();
        }
        return null;
    }

    private void executePythonAssertions(String testName, String code,
                                         List<Map<String, Object>> results) {
        final var pythonContext = new PythonContext(
                config.pythonContextConfig() != null
                        ? config.pythonContextConfig()
                        : PythonContextConfig.builder().build());

        // Convert results to Python-native types
        final var pythonResults = PythonTypeConverter.toPython(results);

        // Build Python assertion function
        final var pyCode = """
                import polyglot

                @polyglot.export_value
                def __ksml_test_assert(results):
                %s
                """.formatted(indentCode(code));

        final var assertFunc = pythonContext.registerFunction(pyCode, "__ksml_test_assert");
        if (assertFunc == null) {
            throw new RuntimeException("Failed to register test assertion code for test: " + testName);
        }

        try {
            assertFunc.execute(pythonResults);
        } catch (Exception e) {
            // Extract the Python AssertionError message
            if (isAssertionError(e)) {
                throw new AssertionError("Test '" + testName + "' assertion failed: "
                        + extractPythonMessage(e), e);
            }
            throw new RuntimeException("Test '" + testName + "' execution error: " + e.getMessage(), e);
        }
    }

    private String indentCode(String code) {
        final var lines = code.split("\\r?\\n");
        final var sb = new StringBuilder();
        for (var line : lines) {
            sb.append("    ").append(line).append("\n");
        }
        return sb.toString();
    }

    private boolean isAssertionError(Exception e) {
        var message = e.getMessage();
        if (message != null && message.contains("AssertionError")) return true;
        var cause = e.getCause();
        while (cause != null) {
            if (cause instanceof AssertionError) return true;
            if (cause.getMessage() != null && cause.getMessage().contains("AssertionError")) return true;
            cause = cause.getCause();
        }
        return false;
    }

    private String extractPythonMessage(Exception e) {
        var message = e.getMessage();
        if (message == null) return "Unknown assertion error";
        // Try to extract the message from "AssertionError: message"
        var idx = message.indexOf("AssertionError:");
        if (idx >= 0) {
            return message.substring(idx + "AssertionError:".length()).trim();
        }
        return message;
    }

    @Override
    public synchronized State getState() {
        return currentState;
    }

    @Override
    public void stop() {
        setState(State.STOPPING);
    }
}
