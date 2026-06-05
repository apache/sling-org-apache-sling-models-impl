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

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.models.impl.injectors.RequestAttributeInjector;
import org.apache.sling.models.impl.injectors.SelfInjector;
import org.apache.sling.models.testmodels.classes.constructorinjection.RecordModel;
import org.apache.sling.models.testmodels.classes.constructorinjection.RecordModelWithExtraConstructor;
import org.apache.sling.models.testmodels.classes.constructorinjection.RecordModelWithStaticFields;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class RecordModelTest {

    private ModelAdapterFactory factory;

    @Mock
    private SlingJakartaHttpServletRequest request;

    private static final int INT_VALUE = 42;

    private static final String STRING_VALUE = "myValue";

    @BeforeEach
    void setup() {
        lenient().when(request.getAttribute("attribute")).thenReturn(INT_VALUE);
        lenient().when(request.getAttribute("attribute2")).thenReturn(STRING_VALUE);

        factory = AdapterFactoryTest.createModelAdapterFactory();
        factory.injectors = Arrays.asList(new RequestAttributeInjector(), new SelfInjector());
        factory.adapterImplementations.addClassesAsAdapterAndImplementation(
                RecordModel.class, RecordModelWithStaticFields.class, RecordModelWithExtraConstructor.class);
    }

    @Test
    void testRecordModel() {
        RecordModel model = factory.getAdapter(request, RecordModel.class);
        assertNotNull(model);
        assertEquals(INT_VALUE, model.attribute());
        assertEquals(STRING_VALUE, model.attribute2());
    }

    @Test
    void testRecordModelWithStaticFields() {
        RecordModelWithStaticFields model = factory.getAdapter(request, RecordModelWithStaticFields.class);
        assertNotNull(model);
        assertEquals(INT_VALUE, model.attribute());
        assertEquals(STRING_VALUE, model.attribute2());
    }

    @Test
    void testRecordModelWithExtraConstructorUsesCanonicalConstructor() {
        RecordModelWithExtraConstructor model = factory.getAdapter(request, RecordModelWithExtraConstructor.class);
        assertNotNull(model);
        assertEquals(INT_VALUE, model.attribute());
        assertEquals(STRING_VALUE, model.attribute2());
    }
}
