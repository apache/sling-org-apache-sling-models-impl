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

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.impl.injectors.ValueMapInjector;
import org.apache.sling.models.testmodels.classes.DefaultPrimitivesModel;
import org.apache.sling.models.testmodels.classes.DefaultStringModel;
import org.apache.sling.models.testmodels.classes.DefaultWrappersModel;
import org.apache.sling.models.testmodels.interfaces.PropertyModelWithDefaults;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultTest {

    private ModelAdapterFactory factory;

    @BeforeEach
    void setup() {
        factory = AdapterFactoryTest.createModelAdapterFactory();
        factory.injectors = Arrays.asList(new ValueMapInjector());
        factory.implementationPickers = Collections.emptyList();
        factory.adapterImplementations.addClassesAsAdapterAndImplementation(
                DefaultStringModel.class,
                PropertyModelWithDefaults.class,
                DefaultPrimitivesModel.class,
                DefaultWrappersModel.class,
                org.apache.sling.models.testmodels.classes.constructorinjection.DefaultPrimitivesModel.class,
                org.apache.sling.models.testmodels.classes.constructorinjection.DefaultStringModel.class,
                org.apache.sling.models.testmodels.classes.constructorinjection.DefaultWrappersModel.class);
    }

    @Test
    void testDefaultStringValueField() {
        ValueMap vm = new ValueMapDecorator(Collections.<String, Object>emptyMap());

        Resource res = mock(Resource.class);
        when(res.adaptTo(ValueMap.class)).thenReturn(vm);

        DefaultStringModel model = factory.getAdapter(res, DefaultStringModel.class);
        assertNotNull(model);
        assertEquals("firstDefault", model.getFirstProperty());
        assertEquals(2, model.getSecondProperty().length);
    }

    @Test
    void testDefaultStringValueOnInterfaceField() {
        ValueMap vm = new ValueMapDecorator(Collections.<String, Object>singletonMap("first", "first value"));

        Resource res = mock(Resource.class);
        when(res.adaptTo(ValueMap.class)).thenReturn(vm);

        PropertyModelWithDefaults model = factory.getAdapter(res, PropertyModelWithDefaults.class);
        assertNotNull(model);
        assertEquals("first value", model.getFirst());
        assertEquals("second default", model.getSecond());
    }

    @Test
    void testDefaultPrimitivesField() {
        ValueMap vm = new ValueMapDecorator(Collections.<String, Object>emptyMap());

        Resource res = mock(Resource.class);
        when(res.adaptTo(ValueMap.class)).thenReturn(vm);

        DefaultPrimitivesModel model = factory.getAdapter(res, DefaultPrimitivesModel.class);
        assertNotNull(model);

        assertEquals(true, model.getBooleanProperty());
        assertArrayEquals(new boolean[] {true, true}, model.getBooleanArrayProperty());

        assertEquals(1L, model.getLongProperty());
        assertArrayEquals(new long[] {1L, 1L}, model.getLongArrayProperty());
    }

    @Test
    void testDefaultWrappersField() {
        ValueMap vm = new ValueMapDecorator(Collections.<String, Object>emptyMap());

        Resource res = mock(Resource.class);
        when(res.adaptTo(ValueMap.class)).thenReturn(vm);

        DefaultWrappersModel model = factory.getAdapter(res, DefaultWrappersModel.class);
        assertNotNull(model);

        assertEquals(Boolean.valueOf(true), model.getBooleanWrapperProperty());
        assertArrayEquals(new Boolean[] {Boolean.TRUE, Boolean.TRUE}, model.getBooleanWrapperArrayProperty());

        assertEquals(Long.valueOf(1L), model.getLongWrapperProperty());
        assertArrayEquals(new Long[] {Long.valueOf(1L), Long.valueOf(1L)}, model.getLongWrapperArrayProperty());
    }

    @Test
    void testDefaultStringValueConstructor() {
        ValueMap vm = new ValueMapDecorator(Collections.<String, Object>emptyMap());

        Resource res = mock(Resource.class);
        lenient().when(res.adaptTo(ValueMap.class)).thenReturn(vm);

        org.apache.sling.models.testmodels.classes.constructorinjection.DefaultStringModel model = factory.getAdapter(
                res, org.apache.sling.models.testmodels.classes.constructorinjection.DefaultStringModel.class);
        assertNotNull(model);
        assertEquals("firstDefault", model.getFirstProperty());
        assertEquals(2, model.getSecondProperty().length);
    }

    @Test
    void testDefaultPrimitivesConstructor() {
        ValueMap vm = new ValueMapDecorator(Collections.<String, Object>emptyMap());

        Resource res = mock(Resource.class);
        lenient().when(res.adaptTo(ValueMap.class)).thenReturn(vm);

        org.apache.sling.models.testmodels.classes.constructorinjection.DefaultPrimitivesModel model =
                factory.getAdapter(
                        res,
                        org.apache.sling.models.testmodels.classes.constructorinjection.DefaultPrimitivesModel.class);
        assertNotNull(model);

        assertEquals(true, model.getBooleanProperty());
        assertArrayEquals(new boolean[] {true, true}, model.getBooleanArrayProperty());

        assertEquals(1L, model.getLongProperty());
        assertArrayEquals(new long[] {1L, 1L}, model.getLongArrayProperty());
    }

    @Test
    void testDefaultWrappersConstructor() {
        ValueMap vm = new ValueMapDecorator(Collections.<String, Object>emptyMap());

        Resource res = mock(Resource.class);
        lenient().when(res.adaptTo(ValueMap.class)).thenReturn(vm);

        org.apache.sling.models.testmodels.classes.constructorinjection.DefaultWrappersModel model = factory.getAdapter(
                res, org.apache.sling.models.testmodels.classes.constructorinjection.DefaultWrappersModel.class);
        assertNotNull(model);

        assertEquals(Boolean.valueOf(true), model.getBooleanWrapperProperty());
        assertArrayEquals(new Boolean[] {Boolean.TRUE, Boolean.TRUE}, model.getBooleanWrapperArrayProperty());

        assertEquals(Long.valueOf(1L), model.getLongWrapperProperty());
        assertArrayEquals(new Long[] {Long.valueOf(1L), Long.valueOf(1L)}, model.getLongWrapperArrayProperty());
    }
}
