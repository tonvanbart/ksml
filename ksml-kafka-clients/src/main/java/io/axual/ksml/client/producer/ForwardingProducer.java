package io.axual.ksml.client.producer;

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

import org.apache.kafka.clients.consumer.ConsumerGroupMetadata;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.ProducerFencedException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

public class ForwardingProducer<K, V> implements Producer<K, V> {
    private Producer<K, V> delegate;

    public void initializeProducer(Producer<K, V> delegate) {
        if (delegate == null) {
            throw new UnsupportedOperationException("Delegate producer can not be null");
        }
        if (this.delegate != null) {
            throw new UnsupportedOperationException("ForwardingProducer already initialized");
        }
        this.delegate = delegate;
    }

    @Override
    public void initTransactions() {
        delegate.initTransactions();
    }

    @Override
    public void beginTransaction() throws ProducerFencedException {
        delegate.beginTransaction();
    }

    @Override
    @Deprecated
    public void sendOffsetsToTransaction(Map<TopicPartition, OffsetAndMetadata> offsets, String consumerGroupId) throws ProducerFencedException {
        delegate.sendOffsetsToTransaction(offsets, consumerGroupId);
    }

    @Override
    public void sendOffsetsToTransaction(Map<TopicPartition, OffsetAndMetadata> offsets, ConsumerGroupMetadata consumerGroupMetadata) throws ProducerFencedException {
        delegate.sendOffsetsToTransaction(offsets, consumerGroupMetadata);
    }

    @Override
    public void commitTransaction() throws ProducerFencedException {
        delegate.commitTransaction();
    }

    @Override
    public void abortTransaction() throws ProducerFencedException {
        delegate.abortTransaction();
    }

    @Override
    public Future<RecordMetadata> send(ProducerRecord<K, V> record) {
        return delegate.send(record);
    }

    @Override
    public Future<RecordMetadata> send(ProducerRecord<K, V> record, Callback callback) {
        return delegate.send(record, callback);
    }

    @Override
    public void flush() {
        delegate.flush();
    }

    @Override
    public List<PartitionInfo> partitionsFor(String topic) {
        return delegate.partitionsFor(topic);
    }

    @Override
    public Map<MetricName, ? extends Metric> metrics() {
        return delegate.metrics();
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public void close(Duration duration) {
        delegate.close(duration);
    }
}
