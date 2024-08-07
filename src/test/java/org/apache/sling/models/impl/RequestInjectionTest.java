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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.models.impl.injectors.BindingsInjector;
import org.apache.sling.models.testmodels.classes.BindingsModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RequestInjectionTest {

    private ModelAdapterFactory factory;

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private SlingScriptHelper sling;

    @Before
    public void setup() {
        SlingBindings bindings = new SlingBindings();
        bindings.setSling(sling);
        bindings.setLog(LoggerFactory.getLogger("test"));
        when(request.getAttribute(SlingBindings.class.getName())).thenReturn(bindings);

        factory = AdapterFactoryTest.createModelAdapterFactory();
        factory.injectors = Arrays.asList(new BindingsInjector());
        factory.adapterImplementations.addClassesAsAdapterAndImplementation(
                BindingsModel.class,
                org.apache.sling.models.testmodels.classes.constructorinjection.BindingsModel.class);
    }

    @Test
    public void testNamedInjectionField() {
        BindingsModel model = factory.getAdapter(request, BindingsModel.class);
        assertNotNull(model.getSling());
        assertEquals(sling, model.getSling());
        assertEquals("test", model.getLog().getName());
        verify(request, times(1)).getAttribute(SlingBindings.class.getName());
    }

    @Test
    public void testNamedInjectionConstructor() {
        org.apache.sling.models.testmodels.classes.constructorinjection.BindingsModel model = factory.getAdapter(
                request, org.apache.sling.models.testmodels.classes.constructorinjection.BindingsModel.class);
        assertNotNull(model.getSling());
        assertEquals(sling, model.getSling());
        assertEquals("test", model.getLog().getName());

        verify(request, times(1)).getAttribute(SlingBindings.class.getName());
    }
}
