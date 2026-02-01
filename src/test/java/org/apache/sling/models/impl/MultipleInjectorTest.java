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

import java.util.Arrays;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Source;
import org.apache.sling.models.factory.ModelClassException;
import org.apache.sling.models.impl.injectors.BindingsInjector;
import org.apache.sling.models.impl.injectors.RequestAttributeInjector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class MultipleInjectorTest {

    @Spy
    private BindingsInjector bindingsInjector;

    @Spy
    private RequestAttributeInjector attributesInjector;

    @Mock
    private SlingJakartaHttpServletRequest request;

    private ModelAdapterFactory factory;

    private SlingBindings bindings;

    @BeforeEach
    void setup() {
        bindings = new SlingBindings();

        factory = AdapterFactoryTest.createModelAdapterFactory();
        // binding injector should be asked first as it has a lower service ranking!
        factory.injectors = Arrays.asList(bindingsInjector, attributesInjector);
        factory.bindStaticInjectAnnotationProcessorFactory(bindingsInjector, new ServicePropertiesMap(1, 1));

        lenient().when(request.getAttribute(SlingBindings.class.getName())).thenReturn(bindings);
        factory.adapterImplementations.addClassesAsAdapterAndImplementation(
                ForTwoInjectorsWithSource.class, ForTwoInjectors.class, ForTwoInjectorsWithInvalidSource.class);
    }

    @Test
    void testInjectorOrder() {
        String bindingsValue = "bindings value";
        bindings.put("firstAttribute", bindingsValue);

        String attributeValue = "attribute value";
        lenient().when(request.getAttribute("firstAttribute")).thenReturn(attributeValue);

        ForTwoInjectors obj = factory.getAdapter(request, ForTwoInjectors.class);

        assertNotNull(obj);
        assertEquals(obj.firstAttribute, bindingsValue);
    }

    @Test
    void testInjectorOrderWithSource() {
        String bindingsValue = "bindings value";
        bindings.put("firstAttribute", bindingsValue);

        String attributeValue = "attribute value";
        lenient().when(request.getAttribute("firstAttribute")).thenReturn(attributeValue);

        ForTwoInjectorsWithSource obj = factory.getAdapter(request, ForTwoInjectorsWithSource.class);

        assertNotNull(obj);
        assertEquals(obj.firstAttribute, attributeValue);
    }

    @Test
    void testInjectorWithInvalidSource() {
        ForTwoInjectorsWithInvalidSource obj = factory.getAdapter(request, ForTwoInjectorsWithInvalidSource.class);
        assertNull(obj);
    }

    @Test
    void testInjectorWithInvalidSourceWithException() {
        assertThrows(
                ModelClassException.class, () -> factory.createModel(request, ForTwoInjectorsWithInvalidSource.class));
    }

    @Model(adaptables = SlingJakartaHttpServletRequest.class)
    public static class ForTwoInjectors {

        @Inject
        private String firstAttribute;
    }

    @Model(adaptables = SlingJakartaHttpServletRequest.class)
    public static class ForTwoInjectorsWithSource {

        @Inject
        @Source("request-attributes")
        private String firstAttribute;
    }

    @Model(adaptables = SlingJakartaHttpServletRequest.class)
    public static class ForTwoInjectorsWithInvalidSource {

        @Inject
        @Source("this-is-an-invalid-source")
        private String firstAttribute;
    }
}
