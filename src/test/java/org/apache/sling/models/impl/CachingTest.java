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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.testing.sling.MockSlingHttpServletRequest;
import org.apache.sling.models.impl.injectors.RequestAttributeInjector;
import org.apache.sling.models.impl.injectors.ValueMapInjector;
import org.apache.sling.models.testmodels.classes.CachedModel;
import org.apache.sling.models.testmodels.classes.UncachedModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CachingTest {

    @Spy
    private MockRequest request;

    private SlingHttpServletRequestWrapper requestWrapper;

    @Mock
    private Resource resource;
    
    private ModelAdapterFactory factory;

    @Before
    public void setup() {
        factory = AdapterFactoryTest.createModelAdapterFactory();
        factory.bindInjector(new RequestAttributeInjector(), new ServicePropertiesMap(0, 0));
        factory.bindInjector(new ValueMapInjector(), new ServicePropertiesMap(1, 1));
        factory.adapterImplementations.addClassesAsAdapterAndImplementation(CachedModel.class, UncachedModel.class,
                org.apache.sling.models.testmodels.interfaces.CachedModel.class, org.apache.sling.models.testmodels.interfaces.UncachedModel.class);

        when(request.getAttribute("testValue")).thenReturn("test");
        requestWrapper = new SlingHttpServletRequestWrapper(request);
        
        ValueMap vm = new ValueMapDecorator(Collections.singletonMap("testValue", "test"));
        when(resource.adaptTo(ValueMap.class)).thenReturn(vm);
    }

    @Test
    public void testCachedClass() {
        CachedModel cached1 = factory.getAdapter(request, CachedModel.class);
        CachedModel cached2 = factory.getAdapter(request, CachedModel.class);

        assertSame(cached1, cached2);
        assertEquals("test", cached1.getTestValue());
        assertEquals("test", cached2.getTestValue());

        verify(request, times(1)).getAttribute("testValue");
    }
    
    @Test
    public void testCachedClassWithResource() {
        CachedModel cached1 = factory.getAdapter(resource, CachedModel.class);
        CachedModel cached2 = factory.getAdapter(resource, CachedModel.class);

        assertSame(cached1, cached2);
        assertEquals("test", cached1.getTestValue());
        assertEquals("test", cached2.getTestValue());

        verify(resource, times(1)).adaptTo(ValueMap.class);
    }

    @Test
    public void testNoCachedClass() {
        UncachedModel uncached1 = factory.getAdapter(request, UncachedModel.class);
        UncachedModel uncached2 = factory.getAdapter(request, UncachedModel.class);

        assertNotSame(uncached1, uncached2);
        assertEquals("test", uncached1.getTestValue());
        assertEquals("test", uncached2.getTestValue());

        verify(request, times(2)).getAttribute("testValue");
    }
    
    @Test
    public void testNoCachedClassWithResource() {
        UncachedModel uncached1 = factory.getAdapter(resource, UncachedModel.class);
        UncachedModel uncached2 = factory.getAdapter(resource, UncachedModel.class);

        assertNotSame(uncached1, uncached2);
        assertEquals("test", uncached1.getTestValue());
        assertEquals("test", uncached2.getTestValue());

        verify(resource, times(2)).adaptTo(ValueMap.class);
    }

    @Test
    public void testCachedInterface() {
        org.apache.sling.models.testmodels.interfaces.CachedModel cached1 = factory.getAdapter(request, org.apache.sling.models.testmodels.interfaces.CachedModel.class);
        org.apache.sling.models.testmodels.interfaces.CachedModel cached2 = factory.getAdapter(request, org.apache.sling.models.testmodels.interfaces.CachedModel.class);

        assertSame(cached1, cached2);
        assertEquals("test", cached1.getTestValue());
        assertEquals("test", cached2.getTestValue());

        verify(request, times(1)).getAttribute("testValue");
    }

    @Test
    public void testNoCachedInterface() {
        org.apache.sling.models.testmodels.interfaces.UncachedModel uncached1 = factory.getAdapter(request, org.apache.sling.models.testmodels.interfaces.UncachedModel.class);
        org.apache.sling.models.testmodels.interfaces.UncachedModel uncached2 = factory.getAdapter(request, org.apache.sling.models.testmodels.interfaces.UncachedModel.class);

        assertNotSame(uncached1, uncached2);
        assertEquals("test", uncached1.getTestValue());
        assertEquals("test", uncached2.getTestValue());

        verify(request, times(2)).getAttribute("testValue");
    }

    @Test
    public void testCachedClassWithRequestWrapper() {
        CachedModel cached1 = factory.getAdapter(request, CachedModel.class);
        CachedModel cached2 = factory.getAdapter(requestWrapper, CachedModel.class);

        assertSame(cached1, cached2);
        assertEquals("test", cached1.getTestValue());
        assertEquals("test", cached2.getTestValue());

        verify(request, times(1)).getAttribute("testValue");
        
        // If we clear the request attributes, the sling model is no longer cached
        request.clearAttributes();
        CachedModel cached3 = factory.getAdapter(request, CachedModel.class);
        assertNotSame(cached1, cached3);
    }

    @Test
    public void testCachedInterfaceWithRequestWrapper() {
        org.apache.sling.models.testmodels.interfaces.CachedModel cached1 = factory.getAdapter(request, org.apache.sling.models.testmodels.interfaces.CachedModel.class);
        org.apache.sling.models.testmodels.interfaces.CachedModel cached2 = factory.getAdapter(requestWrapper, org.apache.sling.models.testmodels.interfaces.CachedModel.class);

        assertSame(cached1, cached2);
        assertEquals("test", cached1.getTestValue());
        assertEquals("test", cached2.getTestValue());

        verify(request, times(1)).getAttribute("testValue");
    }
    
    // MockSlingHttpServletRequest doesn't implement set and get attributes
    private static class MockRequest extends MockSlingHttpServletRequest {

        private Map<String, Object> attributes = new HashMap<>();
        
        MockRequest() {
            super(null, null, null, null, null);
        }
        
        @Override
        public void setAttribute(String name, Object o) {
            attributes.put(name, o);
        }
        
        @Override
        public Object getAttribute(String name) {
            return attributes.get(name);
        }
        
        public void clearAttributes() {
            attributes.clear();
        }
    }
}

