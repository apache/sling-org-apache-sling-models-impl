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

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRequestEvent;
import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.wrappers.SlingJakartaHttpServletRequestWrapper;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.spi.DisposalCallback;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.Injector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class RequestDisposalTest {
    @Mock
    private Resource resource;

    @Mock
    private SlingJakartaHttpServletRequest request;

    @Mock
    private ServletContext servletContext;

    private ModelAdapterFactory factory;

    private Set<TestDisposalCallback> callbacks;

    @BeforeEach
    void setup() {
        factory = AdapterFactoryTest.createModelAdapterFactory();
        factory.injectors = Arrays.asList(new DisposedInjector());
        factory.adapterImplementations.addClassesAsAdapterAndImplementation(TestModel.class);

        final Map<String, Object> attributes = new HashMap<>();

        lenient()
                .doAnswer(new Answer<Void>() {

                    @Override
                    public Void answer(InvocationOnMock invocation) {
                        attributes.put(
                                (String) invocation.getArguments()[0],
                                invocation.getArguments()[1]);
                        return null;
                    }
                })
                .when(request)
                .setAttribute(any(String.class), any());

        lenient().when(request.getAttribute(any(String.class))).then(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) {
                return attributes.get(invocation.getArguments()[0]);
            }
        });
        callbacks = new HashSet<>();
    }

    @Test
    void testWithInitializedRequest() {
        // destroy a wrapper of the root request
        SlingJakartaHttpServletRequest destroyedRequest = new SlingJakartaHttpServletRequestWrapper(request);
        factory.requestInitialized(new ServletRequestEvent(servletContext, destroyedRequest));

        // but adapt from a wrapper of a wrapper of that wrapper
        SlingJakartaHttpServletRequest adaptableRequest =
                new SlingJakartaHttpServletRequestWrapper(new SlingJakartaHttpServletRequestWrapper(destroyedRequest));

        TestModel model = factory.getAdapter(adaptableRequest, TestModel.class);
        assertEquals("teststring", model.testString);

        assertNoneDisposed();

        factory.requestDestroyed(new ServletRequestEvent(servletContext, destroyedRequest));

        assertAllDisposed();
    }

    @Test
    void testTwoInstancesWithInitializedRequest() {
        // destroy a wrapper of the root request
        SlingJakartaHttpServletRequest destroyedRequest = new SlingJakartaHttpServletRequestWrapper(request);
        factory.requestInitialized(new ServletRequestEvent(servletContext, destroyedRequest));

        // but adapt from a wrapper of a wrapper of that wrapper
        SlingJakartaHttpServletRequest adaptableRequest =
                new SlingJakartaHttpServletRequestWrapper(new SlingJakartaHttpServletRequestWrapper(destroyedRequest));

        TestModel model = factory.getAdapter(adaptableRequest, TestModel.class);
        assertEquals("teststring", model.testString);

        TestModel model2 = factory.getAdapter(adaptableRequest, TestModel.class);
        assertEquals("teststring", model2.testString);

        assertNoneDisposed();

        factory.requestDestroyed(new ServletRequestEvent(servletContext, destroyedRequest));

        assertAllDisposed();
    }

    @Test
    void testWithUnitializedRequest() {
        // destroy a wrapper of the root request
        SlingJakartaHttpServletRequest destroyedRequest = new SlingJakartaHttpServletRequestWrapper(request);

        // but adapt from a wrapper of a wrapper of that wrapper
        SlingJakartaHttpServletRequest adaptableRequest =
                new SlingJakartaHttpServletRequestWrapper(new SlingJakartaHttpServletRequestWrapper(destroyedRequest));

        TestModel model = factory.getAdapter(adaptableRequest, TestModel.class);
        assertEquals("teststring", model.testString);

        assertNoneDisposed();

        factory.requestDestroyed(new ServletRequestEvent(servletContext, destroyedRequest));

        assertNoneDisposed();
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

    @Model(adaptables = SlingJakartaHttpServletRequest.class)
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
