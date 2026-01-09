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

import org.apache.sling.api.SlingHttpServletRequest;
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
 * @deprecated use {@link ResourceOverridingJakartaRequestWrapperTest} instead
 */
@Deprecated
@RunWith(MockitoJUnitRunner.class)
public class ResourceOverridingRequestWrapperTest {

    private ResourceOverridingRequestWrapper wrapper = null;

    @Mock
    private SlingHttpServletRequest javaxRequest;

    @Mock
    private Resource resource;

    @Before
    public void setup() {
        Mockito.when(javaxRequest.getAttribute("attr1")).thenReturn("value1");

        SlingBindings mockSlingBindings = Mockito.mock(SlingBindings.class);
        Mockito.when(javaxRequest.getAttribute(SlingBindings.class.getName())).thenReturn(mockSlingBindings);

        AdapterManager mockAdapterManager = Mockito.mock(AdapterManager.class);
        SlingModelsScriptEngineFactory mockScriptEngineFactory = Mockito.mock(SlingModelsScriptEngineFactory.class);
        BindingsValuesProvidersByContext mockProvidersByContext = Mockito.mock(BindingsValuesProvidersByContext.class);
        wrapper = new ResourceOverridingRequestWrapper(
                javaxRequest, resource, mockAdapterManager, mockScriptEngineFactory, mockProvidersByContext);

        Mockito.when(mockAdapterManager.getAdapter(wrapper, String.class)).thenReturn("Adapted1");
    }

    @Test(expected = None.class)
    public void testCtorWithoutSlingBindings() {
        Mockito.when(javaxRequest.getAttribute(SlingBindings.class.getName())).thenReturn(null);

        AdapterManager mockAdapterManager = Mockito.mock(AdapterManager.class);
        SlingModelsScriptEngineFactory mockScriptEngineFactory = Mockito.mock(SlingModelsScriptEngineFactory.class);
        BindingsValuesProvidersByContext mockProvidersByContext = Mockito.mock(BindingsValuesProvidersByContext.class);
        new ResourceOverridingRequestWrapper(
                javaxRequest, resource, mockAdapterManager, mockScriptEngineFactory, mockProvidersByContext);
    }

    /**
     * Test method for {@link org.apache.sling.models.impl.ResourceOverridingRequestWrapper#getAttribute(java.lang.String)}.
     */
    @Test
    public void testGetAttributeString() {
        assertEquals("value1", wrapper.getAttribute("attr1"));
        assertTrue(wrapper.getAttribute(SlingBindings.class.getName()) instanceof SlingBindings);
    }

    /**
     * Test method for {@link org.apache.sling.models.impl.ResourceOverridingRequestWrapper#getResource()}.
     */
    @Test
    public void testGetResource() {
        assertEquals(resource, wrapper.getResource());
    }

    /**
     * Test method for {@link org.apache.sling.models.impl.ResourceOverridingRequestWrapper#adaptTo(java.lang.Class)}.
     */
    @Test
    public void testAdaptToClassOfAdapterType() {
        assertEquals("Adapted1", wrapper.adaptTo(String.class));
    }
}
