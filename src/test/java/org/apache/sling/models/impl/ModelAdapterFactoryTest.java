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

import javax.inject.Inject;
import javax.servlet.ServletContext;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.spi.DisposalCallback;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.Injector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ModelAdapterFactoryTest {
    @Mock
    private Resource resource;

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private ServletContext servletContext;

    private ModelAdapterFactory factory;

    private Set<TestDisposalCallback> callbacks;

    @Before
    public void setup() {
        factory = AdapterFactoryTest.createModelAdapterFactory();
        factory.injectors = Arrays.asList(new DisposedInjector());
        factory.adapterImplementations.addClassesAsAdapterAndImplementation(TestModel.class);

        callbacks = new HashSet<>();
    }

    @Test
    public void testDisposableOnResourceResolverClose() {
        TestModel model = factory.getAdapter(request, TestModel.class);
        assertEquals("teststring", model.testString);

        assertNoneDisposed();

        factory.resourceResolverFactory.getThreadResourceResolver().close();

        assertAllDisposed();
    }

    @Test
    public void testDisposableWithPhantomResource() {
        DisposalCallbackRegistryImpl.cleanupDisposables();
        final ResourceResolver resourceResolver = factory.resourceResolverFactory.getThreadResourceResolver();
        when(factory.resourceResolverFactory.getThreadResourceResolver()).thenReturn(null);

        TestModel model = factory.getAdapter(request, TestModel.class);
        assertEquals("teststring", model.testString);
        model = null;
        System.gc();

        assertNoneDisposed();

        resourceResolver.close();
        assertNoneDisposed();

        DisposalCallbackRegistryImpl.cleanupDisposables();
        assertAllDisposed();
    }

    private void assertNoneDisposed() {
        for (TestDisposalCallback callback : callbacks) {
            assertFalse(callback.isDisposed());
        }
    }

    private void assertAllDisposed() {
        for (TestDisposalCallback callback : callbacks) {
            assertTrue(callback.isDisposed());
        }
    }

    @Model(adaptables = SlingHttpServletRequest.class)
    public static class TestModel {

        @Inject
        public String testString;
    }

    private class DisposedInjector implements Injector {
        @NotNull
        @Override
        public String getName() {
            return "disposed";
        }

        @Nullable
        @Override
        public Object getValue(
                @NotNull Object o,
                String s,
                @NotNull Type type,
                @NotNull AnnotatedElement annotatedElement,
                @NotNull DisposalCallbackRegistry disposalCallbackRegistry) {
            TestDisposalCallback callback = new TestDisposalCallback();
            callbacks.add(callback);
            disposalCallbackRegistry.addDisposalCallback(callback);
            return "teststring";
        }
    }

    private class TestDisposalCallback implements DisposalCallback {
        private boolean disposed = false;

        @Override
        public void onDisposed() {
            disposed = true;
        }

        public boolean isDisposed() {
            return disposed;
        }
    }
}
