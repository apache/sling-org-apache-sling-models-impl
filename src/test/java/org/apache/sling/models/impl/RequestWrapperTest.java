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

import javax.script.Bindings;
import javax.script.ScriptEngineFactory;

import java.util.Collections;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.adapter.AdapterManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.scripting.api.BindingsValuesProvider;
import org.apache.sling.scripting.api.BindingsValuesProvidersByContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestWrapperTest {

    @Mock
    private AdapterManager adapterManager;

    @Mock
    private BindingsValuesProvidersByContext bindingsValuesProvidersByContext;

    @Mock
    private BindingsValuesProvider bindingsValuesProvider;

    @Mock
    private Resource resource;

    @Mock
    private SlingJakartaHttpServletRequest request;

    @InjectMocks
    private ModelAdapterFactory factory;

    @BeforeEach
    void setup() {
        factory = AdapterFactoryTest.createModelAdapterFactory();
        factory.bindingsValuesProvidersByContext = bindingsValuesProvidersByContext;
        factory.adapterManager = adapterManager;
        lenient()
                .when(bindingsValuesProvidersByContext.getBindingsValuesProviders(
                        any(ScriptEngineFactory.class), eq(BindingsValuesProvider.DEFAULT_CONTEXT)))
                .thenReturn(Collections.singleton(bindingsValuesProvider));
    }

    @Test
    void testWrapper() {
        Target expected = new Target();
        when(adapterManager.getAdapter(any(SlingJakartaHttpServletRequest.class), eq(Target.class)))
                .thenReturn(expected);

        Target actual = factory.getModelFromWrappedRequest(request, resource, Target.class);
        assertEquals(expected, actual);

        verify(adapterManager, times(1)).getAdapter(argThat(requestHasResource(resource)), eq(Target.class));
        verify(bindingsValuesProvider, times(1)).addBindings(argThat(bindingsHasResource(resource)));
    }

    private ArgumentMatcher<Bindings> bindingsHasResource(final Resource resource) {
        return new ArgumentMatcher<Bindings>() {
            @Override
            public boolean matches(Bindings bindings) {
                return bindings.get(SlingBindings.RESOURCE) == resource;
            }
        };
    }

    private ArgumentMatcher<SlingJakartaHttpServletRequest> requestHasResource(final Resource resource) {
        return new ArgumentMatcher<SlingJakartaHttpServletRequest>() {
            @Override
            public boolean matches(SlingJakartaHttpServletRequest slingHttpServletRequest) {
                return slingHttpServletRequest.getResource().equals(resource);
            }
        };
    }

    class Target {}
}
