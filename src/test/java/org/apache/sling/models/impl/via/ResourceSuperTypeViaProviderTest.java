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

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.via.ResourceSuperType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ResourceSuperTypeViaProviderTest {

    private ResourceSuperTypeViaProvider provider = new ResourceSuperTypeViaProvider();

    @Mock
    private Resource resource;

    @Mock
    private ResourceResolver resourceResolver;

    /**
     * Test method for {@link org.apache.sling.models.impl.via.ResourceSuperTypeViaProvider#handle(java.lang.String)}.
     */
    @Test
    void testHandle() {
        assertTrue(provider.handle("foo"));
    }

    /**
     * Test method for {@link org.apache.sling.models.impl.via.ResourceSuperTypeViaProvider#getResourceType(org.apache.sling.api.resource.Resource, java.lang.String)}.
     */
    @Test
    void testGetResourceType() {
        Mockito.when(resourceResolver.getParentResourceType(resource)).thenReturn("test1");
        Mockito.when(resource.getResourceResolver()).thenReturn(resourceResolver);
        assertEquals("test1", provider.getResourceType(resource, "foo"));
    }

    /**
     * Test method for {@link org.apache.sling.models.impl.via.ResourceSuperTypeViaProvider#getType()}.
     */
    @Test
    void testGetType() {
        assertEquals(ResourceSuperType.class, provider.getType());
    }
}
