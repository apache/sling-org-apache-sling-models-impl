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

import java.util.Map;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.AdapterManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.impl.injectors.RequestAttributeInjector;
import org.apache.sling.models.impl.injectors.ValueMapInjector;
import org.apache.sling.models.spi.ImplementationPicker;
import org.apache.sling.models.testutil.ModelAdapterFactoryUtil;
import org.apache.sling.scripting.api.BindingsValuesProvidersByContext;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Tests in which order the injectors are handled depending on service ranking.
 * For historic/backwards compatibility reasons, higher ranking value means lower priority (inverse to DS behavior).
 */
@RunWith(MockitoJUnitRunner.class)
public class ModelAdapterFactory_InjectorOrderTest {

    @Rule
    public final OsgiContext context = new OsgiContext();

    @Mock
    private AdapterManager adapterManager;

    @Mock
    private BindingsValuesProvidersByContext bindingsValuesProvidersByContext;

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private Resource resource;

    private ModelAdapterFactory factory;

    @SuppressWarnings("null")
    @Before
    public void setUp() {
        context.registerService(BindingsValuesProvidersByContext.class, bindingsValuesProvidersByContext);
        context.registerService(AdapterManager.class, adapterManager);
        factory = context.registerInjectActivateService(ModelAdapterFactory.class);

        ModelAdapterFactoryUtil.addModelsForPackage(context.bundleContext(), TestModel.class);

        when(request.getResource()).thenReturn(resource);
        when(resource.adaptTo(ValueMap.class)).thenReturn(new ValueMapDecorator(Map.of("prop1", 1)));
        when(request.getAttribute("prop1")).thenReturn(2);
    }

    @Test
    public void testSingleInjector_ValueMap() {
        context.registerInjectActivateService(ValueMapInjector.class);

        TestModel model = factory.createModel(request, TestModel.class);
        assertEquals((Integer) 1, model.getProp1());
    }

    @Test
    public void testSingleInjector_RequestAttribute() {
        context.registerInjectActivateService(RequestAttributeInjector.class);

        TestModel model = factory.createModel(request, TestModel.class);
        assertEquals((Integer) 2, model.getProp1());
    }

    @Test
    public void testMultipleInjectors() {
        // ValueMapInjector has higher priority
        context.registerInjectActivateService(ValueMapInjector.class);
        context.registerInjectActivateService(RequestAttributeInjector.class);

        TestModel model = factory.createModel(request, TestModel.class);
        assertEquals((Integer) 1, model.getProp1());
    }

    static final class LastImplementationPicker implements ImplementationPicker {
        @Override
        public Class<?> pick(
                @NotNull Class<?> adapterType, Class<?>[] implementationsTypes, @NotNull Object adaptable) {
            return implementationsTypes[implementationsTypes.length - 1];
        }
    }

    @Model(adaptables = SlingHttpServletRequest.class)
    private interface TestModel {
        @Inject
        Integer getProp1();
    }
}
