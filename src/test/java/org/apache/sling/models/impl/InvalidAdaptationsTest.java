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

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.factory.InvalidAdaptableException;
import org.apache.sling.models.factory.ModelClassException;
import org.apache.sling.models.impl.injectors.ChildResourceInjector;
import org.apache.sling.models.impl.injectors.ValueMapInjector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class InvalidAdaptationsTest {
    private ModelAdapterFactory factory;

    @BeforeEach
    void setup() {
        factory = AdapterFactoryTest.createModelAdapterFactory();
        factory.injectors = Arrays.asList(new ValueMapInjector(), new ChildResourceInjector());
        factory.adapterImplementations.addClassesAsAdapterAndImplementation(NonModel.class, RequestModel.class);
    }

    @Test
    void testNonModelClass() {
        Map<String, Object> emptyMap = Collections.<String, Object>emptyMap();

        Resource res = mock(Resource.class);
        lenient().when(res.adaptTo(ValueMap.class)).thenReturn(new ValueMapDecorator(emptyMap));

        assertNull(factory.getAdapter(res, NonModel.class));
    }

    @Test
    void testNonModelClassException() {
        Map<String, Object> emptyMap = Collections.<String, Object>emptyMap();

        Resource res = mock(Resource.class);
        lenient().when(res.adaptTo(ValueMap.class)).thenReturn(new ValueMapDecorator(emptyMap));

        assertThrows(ModelClassException.class, () -> factory.createModel(res, NonModel.class));
    }

    @Test
    void testWrongAdaptableClass() {
        Map<String, Object> emptyMap = Collections.<String, Object>emptyMap();

        Resource res = mock(Resource.class);
        lenient().when(res.adaptTo(ValueMap.class)).thenReturn(new ValueMapDecorator(emptyMap));

        assertNull(factory.getAdapter(res, RequestModel.class));
    }

    @Test
    void testWrongAdaptableClassException() {
        Map<String, Object> emptyMap = Collections.<String, Object>emptyMap();

        Resource res = mock(Resource.class);
        lenient().when(res.adaptTo(ValueMap.class)).thenReturn(new ValueMapDecorator(emptyMap));

        assertThrows(InvalidAdaptableException.class, () -> factory.createModel(res, RequestModel.class));
    }

    private class NonModel {}

    @Model(adaptables = SlingJakartaHttpServletRequest.class)
    private class RequestModel {}
}
