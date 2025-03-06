package io.axual.ksml.definition;

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


import io.axual.ksml.type.UserType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.kafka.streams.Topology;

@Getter
@EqualsAndHashCode
public class TableDefinition extends TopicDefinition {
    private final KeyValueStateStoreDefinition store;

    public TableDefinition(String topic, UserType keyType, UserType valueType, FunctionDefinition tsExtractor, Topology.AutoOffsetReset resetPolicy, KeyValueStateStoreDefinition store) {
        super(topic, keyType, valueType, tsExtractor, resetPolicy);
        this.store = store;
    }
}
