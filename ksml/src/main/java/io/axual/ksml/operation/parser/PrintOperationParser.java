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

import io.axual.ksml.definition.parser.KeyValuePrinterDefinitionParser;
import io.axual.ksml.dsl.KSMLDSL;
import io.axual.ksml.generator.TopologyResources;
import io.axual.ksml.operation.PrintOperation;
import io.axual.ksml.parser.StructParser;

public class PrintOperationParser extends OperationParser<PrintOperation> {
    public PrintOperationParser(TopologyResources resources) {
        super(KSMLDSL.Operations.PRINT, resources);
    }

    @Override
    public StructParser<PrintOperation> parser() {
        final var contentParser = structParser(
                PrintOperation.class,
                "",
                "Operation to print the contents of a pipeline on the screen or to write them to a file",
                operationNameField(),
                optional(stringField(KSMLDSL.Operations.Print.FILENAME, "The filename to output records to. If nothing is specified, then messages will be printed on stdout")),
                optional(stringField(KSMLDSL.Operations.Print.LABEL, "A label to attach to the output records")),
                optional(functionField(KSMLDSL.Operations.Print.MAPPER, "A function to convert record into a string for output", new KeyValuePrinterDefinitionParser())),
                (name, filename, label, mapper, tags) -> new PrintOperation(operationConfig(name, tags), filename, label, mapper));
        return structParser(
                PrintOperation.class,
                "",
                "Prints all messages resulting from a pipeline to a specified print target",
                optional(customField(KSMLDSL.Operations.PRINT, "The specification of where to print messages to", contentParser)),
                (p, tags) -> p);
    }
}
