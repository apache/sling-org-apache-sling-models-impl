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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.impl.injectors.RequestAttributeInjector;
import org.apache.sling.models.testmodels.classes.CachedModel;
import org.apache.sling.models.testmodels.classes.UncachedModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CachingTest {

    @Mock
    private SlingHttpServletRequest request;

    private ModelAdapterFactory factory;

    @Before
    public void setup() {
        factory = AdapterFactoryTest.createModelAdapterFactory();
        factory.bindInjector(new RequestAttributeInjector(), new ServicePropertiesMap(0, 0));
        factory.adapterImplementations.addClassesAsAdapterAndImplementation(CachedModel.class, UncachedModel.class,
                org.apache.sling.models.testmodels.interfaces.CachedModel.class, org.apache.sling.models.testmodels.interfaces.UncachedModel.class);

        when(request.getAttribute("testValue")).thenReturn("test");
    }

    @Test
    public void testCachedClass() {
        CachedModel cached1 = factory.getAdapter(request, CachedModel.class);
        CachedModel cached2 = factory.getAdapter(request, CachedModel.class);

        assertTrue(cached1 == cached2);
        assertEquals("test", cached1.getTestValue());
        assertEquals("test", cached2.getTestValue());

        verify(request, times(1)).getAttribute("testValue");
    }

    @Test
    public void testNoCachedClass() {
        UncachedModel uncached1 = factory.getAdapter(request, UncachedModel.class);
        UncachedModel uncached2 = factory.getAdapter(request, UncachedModel.class);

        assertTrue(uncached1 != uncached2);
        assertEquals("test", uncached1.getTestValue());
        assertEquals("test", uncached2.getTestValue());

        verify(request, times(2)).getAttribute("testValue");
    }

    @Test
    public void testCachedInterface() {
        org.apache.sling.models.testmodels.interfaces.CachedModel cached1 = factory.getAdapter(request, org.apache.sling.models.testmodels.interfaces.CachedModel.class);
        org.apache.sling.models.testmodels.interfaces.CachedModel cached2 = factory.getAdapter(request, org.apache.sling.models.testmodels.interfaces.CachedModel.class);

        assertTrue(cached1 == cached2);
        assertEquals("test", cached1.getTestValue());
        assertEquals("test", cached2.getTestValue());

        verify(request, times(1)).getAttribute("testValue");
    }

    @Test
    public void testNoCachedInterface() {
        org.apache.sling.models.testmodels.interfaces.UncachedModel uncached1 = factory.getAdapter(request, org.apache.sling.models.testmodels.interfaces.UncachedModel.class);
        org.apache.sling.models.testmodels.interfaces.UncachedModel uncached2 = factory.getAdapter(request, org.apache.sling.models.testmodels.interfaces.UncachedModel.class);

        assertTrue(uncached1 != uncached2);
        assertEquals("test", uncached1.getTestValue());
        assertEquals("test", uncached2.getTestValue());

        verify(request, times(2)).getAttribute("testValue");
    }
}
