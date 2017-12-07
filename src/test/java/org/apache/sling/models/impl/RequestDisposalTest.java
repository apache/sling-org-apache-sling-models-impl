/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.models.impl;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.spi.DisposalCallback;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.Injector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequestEvent;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.Hashtable;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class RequestDisposalTest {
    @Mock
    private ComponentContext componentCtx;

    @Mock
    private BundleContext bundleContext;

    @Mock
    private Resource resource;

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private ServletContext servletContext;

    private ModelAdapterFactory factory;

    private TestDisposalCallback callback;

    @Before
    public void setup() {
        when(componentCtx.getBundleContext()).thenReturn(bundleContext);
        when(componentCtx.getProperties()).thenReturn(new Hashtable<String, Object>());

        factory = new ModelAdapterFactory();
        factory.activate(componentCtx);
        factory.bindInjector(new DisposedInjector(), new ServicePropertiesMap(0, 0));
        factory.adapterImplementations.addClassesAsAdapterAndImplementation(TestModel.class);

        callback = new TestDisposalCallback();

    }

    @Test
    public void test() {
        // destroy a wrapper of the root request
        SlingHttpServletRequest destroyedRequest = new SlingHttpServletRequestWrapper(request);

        // but adapt from a wrapper of a wrapper of that wrapper
        SlingHttpServletRequest adaptableRequest = new SlingHttpServletRequestWrapper(new SlingHttpServletRequestWrapper(destroyedRequest));

        TestModel model = factory.getAdapter(adaptableRequest, TestModel.class);
        assertEquals("teststring", model.testString);

        assertFalse(callback.isDisposed());

        factory.requestDestroyed(new ServletRequestEvent(servletContext, destroyedRequest));

        assertTrue(callback.isDisposed());
    }

    @Model(adaptables = SlingHttpServletRequest.class)
    public static class TestModel {

        @Inject
        public String testString;

    }

    private class DisposedInjector implements Injector {
        @Nonnull
        @Override
        public String getName() {
            return "disposed";
        }

        @CheckForNull
        @Override
        public Object getValue(@Nonnull Object o, String s, @Nonnull Type type, @Nonnull AnnotatedElement annotatedElement, @Nonnull DisposalCallbackRegistry disposalCallbackRegistry) {
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
