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
import java.util.Map;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.factory.InvalidAdaptableException;
import org.apache.sling.models.factory.ModelClassException;
import org.apache.sling.models.impl.injectors.ChildResourceInjector;
import org.apache.sling.models.impl.injectors.ValueMapInjector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class InvalidAdaptationsTest {
    private ModelAdapterFactory factory;

    @Before
    public void setup() {
        factory = AdapterFactoryTest.createModelAdapterFactory();
        factory.injectors = Arrays.asList(new ValueMapInjector(), new ChildResourceInjector());
        factory.adapterImplementations.addClassesAsAdapterAndImplementation(NonModel.class, RequestModel.class);
    }

    @Test
    public void testNonModelClass() {
        Map<String, Object> emptyMap = Collections.<String, Object>emptyMap();

        Resource res = mock(Resource.class);
        lenient().when(res.adaptTo(ValueMap.class)).thenReturn(new ValueMapDecorator(emptyMap));

        assertNull(factory.getAdapter(res, NonModel.class));
    }

    @Test(expected = ModelClassException.class)
    public void testNonModelClassException() {
        Map<String, Object> emptyMap = Collections.<String, Object>emptyMap();

        Resource res = mock(Resource.class);
        lenient().when(res.adaptTo(ValueMap.class)).thenReturn(new ValueMapDecorator(emptyMap));

        assertNull(factory.createModel(res, NonModel.class));
    }

    @Test
    public void testWrongAdaptableClass() {
        Map<String, Object> emptyMap = Collections.<String, Object>emptyMap();

        Resource res = mock(Resource.class);
        lenient().when(res.adaptTo(ValueMap.class)).thenReturn(new ValueMapDecorator(emptyMap));

        assertNull(factory.getAdapter(res, RequestModel.class));
    }

    @Test(expected = InvalidAdaptableException.class)
    public void testWrongAdaptableClassException() {
        Map<String, Object> emptyMap = Collections.<String, Object>emptyMap();

        Resource res = mock(Resource.class);
        lenient().when(res.adaptTo(ValueMap.class)).thenReturn(new ValueMapDecorator(emptyMap));

        assertNull(factory.createModel(res, RequestModel.class));
    }

    private class NonModel {}

    @Model(adaptables = SlingHttpServletRequest.class)
    private class RequestModel {}
}
