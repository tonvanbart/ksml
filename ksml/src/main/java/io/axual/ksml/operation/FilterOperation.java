package io.axual.ksml.operation;

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


import io.axual.ksml.data.object.DataBoolean;
import io.axual.ksml.definition.FunctionDefinition;
import io.axual.ksml.generator.TopologyBuildContext;
import io.axual.ksml.operation.processor.FilterProcessor;
import io.axual.ksml.operation.processor.FixedKeyOperationProcessorSupplier;
import io.axual.ksml.stream.KStreamWrapper;
import io.axual.ksml.stream.KTableWrapper;
import io.axual.ksml.stream.StreamWrapper;
import io.axual.ksml.type.UserType;
import io.axual.ksml.user.UserPredicate;
import org.apache.kafka.streams.kstream.Named;

public class FilterOperation extends StoreOperation {
    private static final String PREDICATE_NAME = "Predicate";
    private final FunctionDefinition predicate;

    public FilterOperation(StoreOperationConfig config, FunctionDefinition predicate) {
        super(config);
        this.predicate = predicate;
    }

    @Override
    public StreamWrapper apply(KStreamWrapper input, TopologyBuildContext context) {
        /*    Kafka Streams method signature:
         *    KStream<K, V> filter(
         *          final Predicate<? super K, ? super V> predicate
         *          final Named named)
         */

        final var k = input.keyType();
        final var v = input.valueType();
        final var pred = userFunctionOf(context, PREDICATE_NAME, predicate, new UserType(DataBoolean.DATATYPE), superOf(k.flatten()), superOf(v.flatten()));
        final var userPred = new UserPredicate(pred, tags);
        final var storeNames = predicate.storeNames().toArray(String[]::new);
        final var supplier = new FixedKeyOperationProcessorSupplier<>(
                name,
                FilterProcessor::new,
                (stores, rec) -> userPred.test(stores, flattenValue(rec.key()), flattenValue(rec.value())),
                storeNames);
        final var output = name != null
                ? input.stream.processValues(supplier, Named.as(name), storeNames)
                : input.stream.processValues(supplier, storeNames);
        return new KStreamWrapper(output, k, v);
    }

    @Override
    public StreamWrapper apply(KTableWrapper input, TopologyBuildContext context) {
        /*    Kafka Streams method signature:
         *    KTable<K, V> filterNot(
         *          final Predicate<? super K, ? super V> predicate
         *          final Named named)
         */

        final var k = input.keyType();
        final var v = input.valueType();
        final var pred = userFunctionOf(context, PREDICATE_NAME, predicate, new UserType(DataBoolean.DATATYPE), superOf(k), superOf(v));
        final var userPred = new UserPredicate(pred, tags);
        final var kvStore = validateKeyValueStore(store(), k, v);
        final var mat = materializedOf(context, kvStore);
        final var named = namedOf();
        final var output = named != null
                ? mat != null
                ? input.table.filter(userPred, named, mat)
                : input.table.filter(userPred, named)
                : mat != null
                ? input.table.filter(userPred, mat)
                : input.table.filter(userPred);
        return new KTableWrapper(output, k, v);
    }
}
