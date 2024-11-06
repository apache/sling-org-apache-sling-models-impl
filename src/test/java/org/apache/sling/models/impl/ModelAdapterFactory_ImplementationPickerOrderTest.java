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

import java.util.function.IntSupplier;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.AdapterManager;
import org.apache.sling.models.annotations.Model;
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
import static org.osgi.framework.Constants.SERVICE_RANKING;

/**
 * Tests in which order the implementation pickers are handled depending on service ranking.
 * For historic/backwards compatibility reasons, higher ranking value means lower priority (inverse to DS behavior).
 */
@RunWith(MockitoJUnitRunner.class)
public class ModelAdapterFactory_ImplementationPickerOrderTest {

    @Rule
    public final OsgiContext context = new OsgiContext();

    @Mock
    private AdapterManager adapterManager;

    @Mock
    private BindingsValuesProvidersByContext bindingsValuesProvidersByContext;

    @Mock
    private SlingHttpServletRequest request;

    private ModelAdapterFactory factory;

    @Before
    public void setUp() {
        context.registerService(BindingsValuesProvidersByContext.class, bindingsValuesProvidersByContext);
        context.registerService(AdapterManager.class, adapterManager);
        factory = context.registerInjectActivateService(ModelAdapterFactory.class);

        ModelAdapterFactoryUtil.addModelsForPackage(context.bundleContext(), Model1.class, Model2.class);
    }

    @Test
    public void testFirstImplementationPicker() {
        context.registerInjectActivateService(FirstImplementationPicker.class);

        IntSupplier result = factory.createModel(request, IntSupplier.class);
        assertEquals(1, result.getAsInt());
    }

    @Test
    public void testMultipleImplementationPickers() {
        // LastImplementationPicker has higher priority
        context.registerInjectActivateService(FirstImplementationPicker.class); // ranking Integer.MAX_VALUE
        context.registerService(ImplementationPicker.class, new LastImplementationPicker(), SERVICE_RANKING, 100);

        IntSupplier result = factory.createModel(request, IntSupplier.class);
        assertEquals(2, result.getAsInt());
    }

    static final class LastImplementationPicker implements ImplementationPicker {
        @Override
        public Class<?> pick(
                @NotNull Class<?> adapterType, Class<?>[] implementationsTypes, @NotNull Object adaptable) {
            return implementationsTypes[implementationsTypes.length - 1];
        }
    }

    @Model(adaptables = SlingHttpServletRequest.class, adapters = IntSupplier.class)
    static final class Model1 implements IntSupplier {
        @Override
        public int getAsInt() {
            return 1;
        }
    }

    @Model(adaptables = SlingHttpServletRequest.class, adapters = IntSupplier.class)
    static final class Model2 implements IntSupplier {
        @Override
        public int getAsInt() {
            return 2;
        }
    }
}
