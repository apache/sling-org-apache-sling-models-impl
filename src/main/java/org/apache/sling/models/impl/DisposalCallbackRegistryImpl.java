/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.models.impl;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.models.spi.DisposalCallback;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.jetbrains.annotations.NotNull;

public class DisposalCallbackRegistryImpl implements DisposalCallbackRegistry, Closeable {

    private static final String RESOURCE_RESOLVER_DISPOSABLE =
            ModelAdapterFactory.class.getName().concat(".Disposable");

    private final List<DisposalCallback> callbacks = new ArrayList<>();

    private boolean sealed = false;

    @Override
    public void addDisposalCallback(@NotNull DisposalCallback callback) {
        if (sealed) {
            throw new IllegalStateException("DisposalCallbackRegistry is sealed");
        }
        callbacks.add(callback);
    }

    public void registerIfNotEmpty(final ResourceResolverFactory factory) {
        if (!this.callbacks.isEmpty()) {
            this.sealed = true;

            final ResourceResolver resolver = factory.getThreadResourceResolver();
            @SuppressWarnings("unchecked")
            final List<DisposalCallbackRegistryImpl> list = (List<DisposalCallbackRegistryImpl>)
                    resolver.getPropertyMap().computeIfAbsent(RESOURCE_RESOLVER_DISPOSABLE, key -> new CloseableList());
            list.add(this);
        }
    }

    @Override
    public void close() {
        for (DisposalCallback callback : callbacks) {
            callback.onDisposed();
        }
        callbacks.clear();
    }

    private static final class CloseableList extends ArrayList<DisposalCallbackRegistryImpl> implements Closeable {

        @Override
        public void close() {
            for (final DisposalCallbackRegistryImpl registry : this) {
                registry.close();
            }
            this.clear();
        }
    }
}
