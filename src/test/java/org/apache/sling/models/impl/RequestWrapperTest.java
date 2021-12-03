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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import javax.script.Bindings;
import javax.script.ScriptEngineFactory;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.AdapterManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.scripting.api.BindingsValuesProvider;
import org.apache.sling.scripting.api.BindingsValuesProvidersByContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RequestWrapperTest {

    @Mock
    private AdapterManager adapterManager;

    @Mock
    private BindingsValuesProvidersByContext bindingsValuesProvidersByContext;

    @Mock
    private BindingsValuesProvider bindingsValuesProvider;

    @Mock
    private Resource resource;

    @Mock
    private SlingHttpServletRequest request;

    @InjectMocks
    private ModelAdapterFactory factory;

    @Before
    public void setup() {
        factory = AdapterFactoryTest.createModelAdapterFactory();
        factory.bindingsValuesProvidersByContext = bindingsValuesProvidersByContext;
        factory.adapterManager = adapterManager;
        when(bindingsValuesProvidersByContext.getBindingsValuesProviders(any(ScriptEngineFactory.class), eq(BindingsValuesProvider.DEFAULT_CONTEXT))).
                thenReturn(Collections.singleton(bindingsValuesProvider));
    }

    @Test
    public void testWrapper() {
        Target expected = new Target();
        when(adapterManager.getAdapter(any(SlingHttpServletRequest.class), eq(Target.class))).thenReturn(expected);

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

    private ArgumentMatcher<SlingHttpServletRequest> requestHasResource(final Resource resource) {
        return new ArgumentMatcher<SlingHttpServletRequest>() {
            @Override
            public boolean matches(SlingHttpServletRequest slingHttpServletRequest) {
                return slingHttpServletRequest.getResource().equals(resource);
            }
        };
    }

    class Target {

    }

}
