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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.lenient;

/**
 * @deprecated use {@link ResourceOverridingJakartaRequestWrapperTest} instead
 */
@Deprecated
@ExtendWith(MockitoExtension.class)
class ResourceOverridingRequestWrapperTest {

    private ResourceOverridingRequestWrapper wrapper = null;

    @Mock
    private SlingHttpServletRequest javaxRequest;

    @Mock
    private Resource resource;

    @BeforeEach
    void setup() {
        lenient().when(javaxRequest.getAttribute("attr1")).thenReturn("value1");

        SlingBindings mockSlingBindings = Mockito.mock(SlingBindings.class);
        lenient().when(javaxRequest.getAttribute(SlingBindings.class.getName())).thenReturn(mockSlingBindings);

        AdapterManager mockAdapterManager = Mockito.mock(AdapterManager.class);
        SlingModelsScriptEngineFactory mockScriptEngineFactory = Mockito.mock(SlingModelsScriptEngineFactory.class);
        BindingsValuesProvidersByContext mockProvidersByContext = Mockito.mock(BindingsValuesProvidersByContext.class);
        wrapper = new ResourceOverridingRequestWrapper(
                javaxRequest, resource, mockAdapterManager, mockScriptEngineFactory, mockProvidersByContext);

        lenient().when(mockAdapterManager.getAdapter(wrapper, String.class)).thenReturn("Adapted1");
    }

    @Test
    void testCtorWithoutSlingBindings() {
        lenient().when(javaxRequest.getAttribute(SlingBindings.class.getName())).thenReturn(null);

        AdapterManager mockAdapterManager = Mockito.mock(AdapterManager.class);
        SlingModelsScriptEngineFactory mockScriptEngineFactory = Mockito.mock(SlingModelsScriptEngineFactory.class);
        BindingsValuesProvidersByContext mockProvidersByContext = Mockito.mock(BindingsValuesProvidersByContext.class);
        wrapper = new ResourceOverridingRequestWrapper(
                javaxRequest, resource, mockAdapterManager, mockScriptEngineFactory, mockProvidersByContext);

        assertNotNull(wrapper);
        assertNotNull(wrapper.getAttribute(SlingBindings.class.getName()));
    }

    /**
     * Test method for {@link org.apache.sling.models.impl.ResourceOverridingRequestWrapper#getAttribute(java.lang.String)}.
     */
    @Test
    void testGetAttributeString() {
        assertEquals("value1", wrapper.getAttribute("attr1"));
        assertTrue(wrapper.getAttribute(SlingBindings.class.getName()) instanceof SlingBindings);
    }

    /**
     * Test method for {@link org.apache.sling.models.impl.ResourceOverridingRequestWrapper#getResource()}.
     */
    @Test
    void testGetResource() {
        assertEquals(resource, wrapper.getResource());
    }

    /**
     * Test method for {@link org.apache.sling.models.impl.ResourceOverridingRequestWrapper#adaptTo(java.lang.Class)}.
     */
    @Test
    void testAdaptToClassOfAdapterType() {
        assertEquals("Adapted1", wrapper.adaptTo(String.class));
    }
}
