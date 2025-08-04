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
import org.apache.sling.models.annotations.via.ForcedResourceType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ForcedResourceTypeViaProviderTest {

    private ForcedResourceTypeViaProvider provider = new ForcedResourceTypeViaProvider();

    @Mock
    private Resource resource;

    /**
     * Test method for {@link org.apache.sling.models.impl.via.ForcedResourceTypeViaProvider#handle(java.lang.String)}.
     */
    @Test
    public void testHandle() {
        assertTrue(provider.handle("foo"));
        assertFalse(provider.handle(""));
    }

    /**
     * Test method for {@link org.apache.sling.models.impl.via.ForcedResourceTypeViaProvider#getResourceType(org.apache.sling.api.resource.Resource, java.lang.String)}.
     */
    @Test
    public void testGetResourceType() {
        assertEquals("foo", provider.getResourceType(resource, "foo"));
    }

    /**
     * Test method for {@link org.apache.sling.models.impl.via.ForcedResourceTypeViaProvider#getType()}.
     */
    @Test
    public void testGetType() {
        assertEquals(ForcedResourceType.class, provider.getType());
    }
}
