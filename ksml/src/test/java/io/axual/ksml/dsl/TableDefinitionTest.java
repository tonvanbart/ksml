package io.axual.ksml.dsl;

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

import io.axual.ksml.data.notation.Notation;
import io.axual.ksml.definition.KeyValueStateStoreDefinition;
import io.axual.ksml.definition.TableDefinition;
import io.axual.ksml.execution.ExecutionContext;
import io.axual.ksml.generator.TopologyBuildContext;
import io.axual.ksml.generator.TopologyResources;
import io.axual.ksml.parser.UserTypeParser;
import io.axual.ksml.stream.KTableWrapper;
import io.axual.ksml.type.UserType;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Materialized;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TableDefinitionTest {

    @Mock
    private StreamsBuilder builder;

    @Mock
    Notation mockNotation;

    @Test
    void testTableDefinition() {
        when(mockNotation.name()).thenReturn(UserType.DEFAULT_NOTATION);
        ExecutionContext.INSTANCE.notationLibrary().register(mockNotation);

        final var stringType = UserTypeParser.parse("string");

        // given a TableDefinition
        final var tableDefinition = new TableDefinition("topic", stringType, stringType, null, null, new KeyValueStateStoreDefinition("storename", stringType, stringType));
        final var resources = new TopologyResources("test");

        final var context = new TopologyBuildContext(builder, resources);
        // when it adds itself to Builder
        final var streamWrapper = context.getStreamWrapper(tableDefinition);

        // it adds a KTable to the StreamsBuilder with key and value dataType, and returns a KTableWrapper instance
        verify(mockNotation).serde(stringType.dataType(), true);
        verify(mockNotation).serde(stringType.dataType(), false);

        verify(builder).table(eq("topic"), isA(Consumed.class), isA(Materialized.class));
        assertThat(streamWrapper, instanceOf(KTableWrapper.class));
    }
}
