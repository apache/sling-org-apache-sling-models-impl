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

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.adapter.AdapterManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.scripting.api.BindingsValuesProvidersByContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.Test.None;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ResourceOverridingJakartaRequestWrapperTest {

    private ResourceOverridingJakartaRequestWrapper wrapper = null;

    @Mock
    private SlingJakartaHttpServletRequest jakartaRequest;

    @Mock
    private Resource resource;

    @Before
    public void setup() {
        Mockito.when(jakartaRequest.getAttribute("attr1")).thenReturn("value1");

        SlingBindings mockSlingBindings = Mockito.mock(SlingBindings.class);
        Mockito.when(jakartaRequest.getAttribute(SlingBindings.class.getName())).thenReturn(mockSlingBindings);

        AdapterManager mockAdapterManager = Mockito.mock(AdapterManager.class);
        SlingModelsScriptEngineFactory mockScriptEngineFactory = Mockito.mock(SlingModelsScriptEngineFactory.class);
        BindingsValuesProvidersByContext mockProvidersByContext = Mockito.mock(BindingsValuesProvidersByContext.class);
        wrapper = new ResourceOverridingJakartaRequestWrapper(
                jakartaRequest, resource, mockAdapterManager, mockScriptEngineFactory, mockProvidersByContext);

        Mockito.when(mockAdapterManager.getAdapter(wrapper, String.class)).thenReturn("Adapted1");
    }

    @Test(expected = None.class)
    public void testCtorWithoutSlingBindings() {
        Mockito.when(jakartaRequest.getAttribute(SlingBindings.class.getName())).thenReturn(null);

        AdapterManager mockAdapterManager = Mockito.mock(AdapterManager.class);
        SlingModelsScriptEngineFactory mockScriptEngineFactory = Mockito.mock(SlingModelsScriptEngineFactory.class);
        BindingsValuesProvidersByContext mockProvidersByContext = Mockito.mock(BindingsValuesProvidersByContext.class);
        new ResourceOverridingJakartaRequestWrapper(
                jakartaRequest, resource, mockAdapterManager, mockScriptEngineFactory, mockProvidersByContext);
    }

    /**
     * Test method for {@link org.apache.sling.models.impl.ResourceOverridingJakartaRequestWrapper#getAttribute(java.lang.String)}.
     */
    @Test
    public void testGetAttributeString() {
        assertEquals("value1", wrapper.getAttribute("attr1"));
        assertTrue(wrapper.getAttribute(SlingBindings.class.getName()) instanceof SlingBindings);
    }

    /**
     * Test method for {@link org.apache.sling.models.impl.ResourceOverridingJakartaRequestWrapper#getResource()}.
     */
    @Test
    public void testGetResource() {
        assertEquals(resource, wrapper.getResource());
    }

    /**
     * Test method for {@link org.apache.sling.models.impl.ResourceOverridingJakartaRequestWrapper#adaptTo(java.lang.Class)}.
     */
    @Test
    public void testAdaptToClassOfAdapterType() {
        assertEquals("Adapted1", wrapper.adaptTo(String.class));
    }
}
