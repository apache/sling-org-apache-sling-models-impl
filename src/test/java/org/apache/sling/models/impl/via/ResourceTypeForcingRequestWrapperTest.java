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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
/**
 * @deprecated use {@link ResourceTypeForcingJakartaRequestWrapperTest)} instead
 */
@Deprecated(since = "2.0.0")
@ExtendWith(MockitoExtension.class)
class ResourceTypeForcingRequestWrapperTest {

    @Mock
    private Resource resource;

    @Mock
    private SlingHttpServletRequest request;

    /**
     * Test method for {@link org.apache.sling.models.impl.via.ResourceTypeForcingRequestWrapper#getResource()}.
     */
    @Test
    void testGetResource() {
        // once
        ResourceTypeForcingRequestWrapper testCase = new ResourceTypeForcingRequestWrapper(request, resource, "foo");
        Resource wrappedResource = testCase.getResource();
        assertTrue(wrappedResource instanceof ResourceTypeForcingResourceWrapper);
        ResourceTypeForcingResourceWrapper resourceTypeForcingResourceWrapper =
                (ResourceTypeForcingResourceWrapper) wrappedResource;
        assertSame(resource, resourceTypeForcingResourceWrapper.getResource());
        assertEquals("foo", resourceTypeForcingResourceWrapper.getResourceType());
    }
}
