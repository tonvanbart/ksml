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

import io.axual.ksml.definition.parser.MetadataTransformerDefinitionParser;
import io.axual.ksml.dsl.KSMLDSL;
import io.axual.ksml.generator.TopologyResources;
import io.axual.ksml.operation.TransformMetadataOperation;
import io.axual.ksml.parser.StructParser;

public class TransformMetadataOperationParser extends OperationParser<TransformMetadataOperation> {
    public TransformMetadataOperationParser(TopologyResources resources) {
        super(KSMLDSL.Operations.TRANSFORM_METADATA, resources);
    }

    @Override
    protected StructParser<TransformMetadataOperation> parser() {
        return structParser(
                TransformMetadataOperation.class,
                "",
                "Convert the metadata of every record in the stream",
                operationTypeField(),
                operationNameField(),
                functionField(KSMLDSL.Operations.Transform.MAPPER, "A function that converts the metadata (Kafka headers, timestamp) of every record in the stream", new MetadataTransformerDefinitionParser()),
                storeNamesField(),
                (type, name, mapper, storeNames, tags) -> new TransformMetadataOperation(operationConfig(name, tags, storeNames), mapper));
    }
}
