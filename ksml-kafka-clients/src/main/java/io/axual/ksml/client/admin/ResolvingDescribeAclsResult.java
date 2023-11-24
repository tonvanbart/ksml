package io.axual.ksml.client.admin;

/*-
 * ========================LICENSE_START=================================
 * axual-client-proxy
 * %%
 * Copyright (C) 2020 Axual B.V.
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

import io.axual.ksml.client.resolving.GroupResolver;
import io.axual.ksml.client.resolving.TopicResolver;
import org.apache.kafka.clients.admin.ExtendableDescribeAclsResult;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.internals.KafkaFutureImpl;

import java.util.Collection;

public class ResolvingDescribeAclsResult extends ExtendableDescribeAclsResult {
    public ResolvingDescribeAclsResult(KafkaFuture<Collection<AclBinding>> future, final TopicResolver topicResolver, final GroupResolver groupResolver) {
        super(convertFuture(future, topicResolver, groupResolver));
    }

    private static KafkaFuture<Collection<AclBinding>> convertFuture(final KafkaFuture<Collection<AclBinding>> future,
                                                                     final TopicResolver topicResolver,
                                                                     final GroupResolver groupResolver) {
        final KafkaFutureImpl<Collection<AclBinding>> wrappingFuture = new KafkaFutureImpl<>();
        future.whenComplete((aclBindings, throwable) -> {
            if (aclBindings != null) {
                wrappingFuture.complete(ResolverUtil.unresolveKeys(aclBindings, topicResolver, groupResolver));
            } else {
                wrappingFuture.completeExceptionally(throwable);
            }
        });
        return wrappingFuture;
    }
}
