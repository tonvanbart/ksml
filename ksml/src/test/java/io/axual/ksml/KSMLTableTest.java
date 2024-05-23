package io.axual.ksml;

import io.axual.ksml.testutil.KSMLTest;
import io.axual.ksml.testutil.KSMLTestExtension;
import io.axual.ksml.testutil.KSMLTopic;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;

@Slf4j
@ExtendWith(KSMLTestExtension.class)
public class KSMLTableTest {

    TestInputTopic testIn;

    TestOutputTopic testOut;

    @KSMLTest(topology = "pipelines/test-queryable-table.yaml", schemapath = "pipelines",
        inputTopics = {@KSMLTopic(variable = "testIn", topic = "ksml_sensordata_avro", valueSerde = KSMLTopic.SerdeType.AVRO)},
        outputTopics = {@KSMLTopic(variable = "testOut", topic = "ksml_sensordata_table", valueSerde = KSMLTopic.SerdeType.AVRO)})
    void testKTable() throws Exception {

        testIn.pipeInput("key1", SensorData.builder().color("blue").build().toRecord());
        testIn.pipeInput("key2", SensorData.builder().color("red").build().toRecord());
        testIn.pipeInput("key1", SensorData.builder().color("red").build().toRecord());
        testIn.pipeInput("key2", null); // tombstone record for key2

        assertFalse(testOut.isEmpty());

        Map kvMap = testOut.readKeyValuesToMap();
        System.out.println("kvMap.size() = " + kvMap.size());
        for (var kv: kvMap.entrySet()) {
            System.out.println("kv = " + kv);
        }

    }

}
