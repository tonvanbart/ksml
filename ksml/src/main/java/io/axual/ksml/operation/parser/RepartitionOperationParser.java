package io.axual.ksml.operation.parser;

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


import io.axual.ksml.definition.parser.StreamPartitionerDefinitionParser;
import io.axual.ksml.dsl.KSMLDSL;
import io.axual.ksml.generator.TopologyResources;
import io.axual.ksml.operation.RepartitionOperation;
import io.axual.ksml.parser.StructParser;

public class RepartitionOperationParser extends OperationParser<RepartitionOperation> {
    public RepartitionOperationParser(TopologyResources resources) {
        super(KSMLDSL.Operations.REPARTITION, resources);
    }

    @Override
    public StructParser<RepartitionOperation> parser() {
        return structParser(
                RepartitionOperation.class,
                "",
                "Operation to (re)partition a stream",
                operationTypeField(),
                operationNameField(),
                functionField(KSMLDSL.Operations.Repartition.PARTITIONER, "A function that partitions stream records", new StreamPartitionerDefinitionParser()),
                (type, name, partitioner, tags) -> new RepartitionOperation(operationConfig(name, tags), partitioner));
    }
}
