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
package org.apache.sling.models.impl.via;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.spi.ViaProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ChildResourceViaProviderTest {

    private ChildResourceViaProvider provider = new ChildResourceViaProvider();

    @Mock
    private Resource resource;

    @Mock
    private Resource childResource;

    @Mock
    private SlingJakartaHttpServletRequest jakartaRequest;

    /**
     * @deprecated use {@link #jakartaRequest} instead
     */
    @SuppressWarnings("deprecation")
    @Deprecated
    @Mock
    private org.apache.sling.api.SlingHttpServletRequest javaxRequest;

    @SuppressWarnings("deprecation")
    @BeforeEach
    void init() {
        lenient().when(resource.getChild("child")).thenReturn(childResource);
        lenient().when(jakartaRequest.getResource()).thenReturn(resource);
        lenient().when(javaxRequest.getResource()).thenReturn(resource);
    }

    @Test
    void testResource() {
        Object adaptable = provider.getAdaptable(resource, "child");
        assertEquals(adaptable, childResource);
    }

    @Test
    void testResourceWithBlank() {
        Object adaptable = provider.getAdaptable(resource, "");
        assertEquals(ViaProvider.ORIGINAL, adaptable);
    }

    @Test
    void testResourceWithOtherAdaptable() {
        assertNull(provider.getAdaptable(new Object(), "child"));
    }

    @Test
    void testJakartaRequest() {
        Object adaptable = provider.getAdaptable(jakartaRequest, "child");
        Resource adaptableResource = ((SlingJakartaHttpServletRequest) adaptable).getResource();
        assertEquals(adaptableResource, childResource);
    }

    @Test
    void testJakartaRequestWhenChildDoesNotExist() {
        assertNull(provider.getAdaptable(jakartaRequest, "notexisting"));
    }

    /**
     * @deprecated use {@link #testJakartaRequest()} instead
     */
    @Deprecated
    @Test
    void testJavaxRequest() {
        Object adaptable = provider.getAdaptable(javaxRequest, "child");
        Resource adaptableResource = ((org.apache.sling.api.SlingHttpServletRequest) adaptable).getResource();
        assertEquals(adaptableResource, childResource);
    }

    /**
     * @deprecated use {@link #testJakartaRequestWhenChildDoesNotExist()} instead
     */
    @Deprecated
    @Test
    void testJavaxRequestWhenChildDoesNotExist() {
        assertNull(provider.getAdaptable(javaxRequest, "notexisting"));
    }
}
