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
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.List;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.models.spi.DisposalCallback;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.jetbrains.annotations.NotNull;

public class DisposalCallbackRegistryImpl implements DisposalCallbackRegistry {

    private static final String RESOURCE_RESOLVER_DISPOSABLE =
            ModelAdapterFactory.class.getName().concat(".Disposable");

    private static final ReferenceQueue<Object> QUEUE = new ReferenceQueue<>();

    private static final List<DisposablePhantomReference> REFERENCES = new ArrayList<>();

    private final List<DisposalCallback> callbacks = new ArrayList<>();

    private boolean sealed = false;

    @Override
    public void addDisposalCallback(@NotNull DisposalCallback callback) {
        if (sealed) {
            throw new IllegalStateException("DisposalCallbackRegistry is sealed");
        }
        callbacks.add(callback);
    }

    @SuppressWarnings("resource")
    public void registerIfNotEmpty(final ResourceResolverFactory factory, final Object referencedObject) {
        if (!this.callbacks.isEmpty()) {
            this.sealed = true;

            final ResourceResolver resolver = factory.getThreadResourceResolver();
            if (resolver != null) {
                @SuppressWarnings("unchecked")
                final List<DisposalCallbackRegistryImpl> list =
                        (List<DisposalCallbackRegistryImpl>) resolver.getPropertyMap()
                                .computeIfAbsent(RESOURCE_RESOLVER_DISPOSABLE, key -> new CloseableList());
                list.add(this);
            } else {
                new DisposablePhantomReference(this, referencedObject, QUEUE);
            }
        }
    }

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

    public static class DisposablePhantomReference extends PhantomReference<Object> implements Closeable {

        private final DisposalCallbackRegistryImpl registry;

        public DisposablePhantomReference(
                final DisposalCallbackRegistryImpl registry,
                final Object referent,
                final ReferenceQueue<Object> queue) {
            super(referent, queue);
            this.registry = registry;
            synchronized (REFERENCES) {
                REFERENCES.add(this);
            }
        }

        @Override
        public void close() {
            registry.close();
        }
    }

    public static void cleanupDisposables() {
        DisposablePhantomReference ref = (DisposablePhantomReference) QUEUE.poll();
        while (ref != null) {
            synchronized (REFERENCES) {
                REFERENCES.remove(ref);
            }
            ref.close();
            ref = (DisposablePhantomReference) QUEUE.poll();
        }
    }
}
