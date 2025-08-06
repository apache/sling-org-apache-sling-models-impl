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
import org.apache.sling.models.annotations.ViaProviderType;
import org.apache.sling.models.spi.ViaProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class AbstractResourceTypeViaProviderTest {
    private AbstractResourceTypeViaProvider provider = new AbstractResourceTypeViaProvider() {
        @Override
        public Class<? extends ViaProviderType> getType() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected boolean handle(@NotNull String value) {
            return "handled".equals(value) || "nullResourceType".equals(value);
        }

        @Override
        protected @Nullable String getResourceType(@NotNull Resource resource, @NotNull String value) {
            return "nullResourceType".equals(value) ? null : value;
        }
    };

    /**
     * Test method for {@link org.apache.sling.models.impl.via.AbstractResourceTypeViaProvider#getAdaptable(java.lang.Object, java.lang.String)}.
     */
    @Test
    public void testGetAdaptableWhenNotHandled() {
        assertEquals(ViaProvider.ORIGINAL, provider.getAdaptable("hello", "nothandled"));
    }

    @Test
    public void testGetAdaptableForResource() {
        Resource mockResource = Mockito.mock(Resource.class);
        Object adaptable = provider.getAdaptable(mockResource, "handled");
        assertTrue(adaptable instanceof ResourceTypeForcingResourceWrapper);

        assertNull(provider.getAdaptable(mockResource, "nullResourceType"));
    }

    @Test
    public void testGetAdaptableForJakartaRequest() {
        SlingJakartaHttpServletRequest mockJakartaRequest = Mockito.mock(SlingJakartaHttpServletRequest.class);
        Object adaptable = provider.getAdaptable(mockJakartaRequest, "handled");
        assertTrue(adaptable instanceof ResourceTypeForcingJakartaRequestWrapper);

        assertNull(provider.getAdaptable(mockJakartaRequest, "nullResourceType"));
    }

    /**
     * @deprecated use {@link #testGetAdaptableForJakartaRequest()} instead
     */
    @Deprecated(since = "2.0.0")
    @Test
    public void testGetAdaptableForJavaxRequest() {
        org.apache.sling.api.SlingHttpServletRequest mockJavaxRequest =
                Mockito.mock(org.apache.sling.api.SlingHttpServletRequest.class);
        Object adaptable = provider.getAdaptable(mockJavaxRequest, "handled");
        assertTrue(adaptable instanceof ResourceTypeForcingRequestWrapper);

        assertNull(provider.getAdaptable(mockJavaxRequest, "nullResourceType"));
    }

    @Test
    public void testGetAdaptableForOther() {
        assertNull(provider.getAdaptable(new Object(), "nullResourceType"));
    }
}
