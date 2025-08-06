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

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.SlingJakartaHttpServletRequestWrapper;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.impl.injectors.RequestAttributeInjector;
import org.apache.sling.models.impl.injectors.ValueMapInjector;
import org.apache.sling.models.testmodels.classes.CachedModel;
import org.apache.sling.models.testmodels.classes.CachedModelWithAdapterTypes12;
import org.apache.sling.models.testmodels.classes.CachedModelWithAdapterTypes23;
import org.apache.sling.models.testmodels.classes.UncachedModel;
import org.apache.sling.models.testmodels.interfaces.AdapterType1;
import org.apache.sling.models.testmodels.interfaces.AdapterType2;
import org.apache.sling.models.testmodels.interfaces.AdapterType3;
import org.apache.sling.servlethelpers.MockSlingJakartaHttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CachingTest {

    @Spy
    private MockSlingJakartaHttpServletRequest request = new MockSlingJakartaHttpServletRequest(null);

    private SlingJakartaHttpServletRequestWrapper requestWrapper;

    @Mock
    private Resource resource;

    private ModelAdapterFactory factory;

    @Before
    public void setup() {
        factory = AdapterFactoryTest.createModelAdapterFactory();
        factory.injectors = Arrays.asList(new RequestAttributeInjector(), new ValueMapInjector());
        factory.adapterImplementations.addClassesAsAdapterAndImplementation(
                CachedModel.class,
                UncachedModel.class,
                org.apache.sling.models.testmodels.interfaces.CachedModel.class,
                org.apache.sling.models.testmodels.interfaces.UncachedModel.class);
        factory.adapterImplementations.addAll(
                CachedModelWithAdapterTypes12.class,
                CachedModelWithAdapterTypes12.class,
                AdapterType1.class,
                AdapterType2.class);
        factory.adapterImplementations.addAll(
                CachedModelWithAdapterTypes23.class,
                CachedModelWithAdapterTypes23.class,
                AdapterType2.class,
                AdapterType3.class);

        when(request.getAttribute("testValue")).thenReturn("test");
        requestWrapper = new SlingJakartaHttpServletRequestWrapper(request);

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
        org.apache.sling.models.testmodels.interfaces.CachedModel cached1 =
                factory.getAdapter(request, org.apache.sling.models.testmodels.interfaces.CachedModel.class);
        org.apache.sling.models.testmodels.interfaces.CachedModel cached2 =
                factory.getAdapter(request, org.apache.sling.models.testmodels.interfaces.CachedModel.class);

        assertSame(cached1, cached2);
        assertEquals("test", cached1.getTestValue());
        assertEquals("test", cached2.getTestValue());

        verify(request, times(1)).getAttribute("testValue");
    }

    @Test
    public void testNoCachedInterface() {
        org.apache.sling.models.testmodels.interfaces.UncachedModel uncached1 =
                factory.getAdapter(request, org.apache.sling.models.testmodels.interfaces.UncachedModel.class);
        org.apache.sling.models.testmodels.interfaces.UncachedModel uncached2 =
                factory.getAdapter(request, org.apache.sling.models.testmodels.interfaces.UncachedModel.class);

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
        Enumeration<String> attributeNames = request.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            request.removeAttribute(attributeNames.nextElement());
        }
        CachedModel cached3 = factory.getAdapter(request, CachedModel.class);
        assertNotSame(cached1, cached3);
    }

    @Test
    public void testCachedInterfaceWithRequestWrapper() {
        org.apache.sling.models.testmodels.interfaces.CachedModel cached1 =
                factory.getAdapter(request, org.apache.sling.models.testmodels.interfaces.CachedModel.class);
        org.apache.sling.models.testmodels.interfaces.CachedModel cached2 =
                factory.getAdapter(requestWrapper, org.apache.sling.models.testmodels.interfaces.CachedModel.class);

        assertSame(cached1, cached2);
        assertEquals("test", cached1.getTestValue());
        assertEquals("test", cached2.getTestValue());

        verify(request, times(1)).getAttribute("testValue");
    }

    @Test
    public void testCachedModelWithAdapterTypes() {
        // test 2 model implementations that share a common adapter type, with an implementation picker that selects
        // exactly one of the
        // implementations for the common adapter type. verify that the models are cached accordingly
        factory.implementationPickers = Collections.singletonList((adapterType, impls, adaptable) -> {
            if (AdapterType1.class.equals(adapterType)) {
                return CachedModelWithAdapterTypes12.class;
            } else if (AdapterType2.class.equals(adapterType) || AdapterType3.class.equals(adapterType)) {
                return CachedModelWithAdapterTypes23.class;
            } else {
                return null;
            }
        });

        CachedModelWithAdapterTypes12 byImpl12 = factory.getAdapter(request, CachedModelWithAdapterTypes12.class);
        CachedModelWithAdapterTypes23 byImpl23 = factory.getAdapter(request, CachedModelWithAdapterTypes23.class);
        AdapterType1 byAdapterType1 = factory.getAdapter(request, AdapterType1.class);
        AdapterType2 byAdapterType2 = factory.getAdapter(request, AdapterType2.class);
        AdapterType3 byAdapterType3 = factory.getAdapter(request, AdapterType3.class);

        assertSame(byImpl12, byAdapterType1);
        assertSame(byImpl23, byAdapterType2);
        assertSame(byImpl23, byAdapterType3);
    }
}
