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
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.wrappers.JakartaToJavaxRequestWrapper;
import org.apache.sling.api.wrappers.SlingJakartaHttpServletRequestWrapper;
import org.apache.sling.models.annotations.via.OriginalResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
class OriginalResourceTypeViaProviderTest {

    private OriginalResourceTypeViaProvider provider = new OriginalResourceTypeViaProvider();

    @Mock
    private Resource resource;

    @Mock
    private SlingJakartaHttpServletRequest request;

    @Test
    void testReturnsCorrectMarkerInterface() {
        assertEquals(OriginalResourceType.class, provider.getType());
    }

    @Test
    void testReturnsOriginalResourceIfNotWrapped() {
        Object projected = provider.getAdaptable(resource, null);
        assertEquals(resource, projected);
    }

    @Test
    void testReturnsOriginalRequestIfNotWrapped() {
        Object projected = provider.getAdaptable(request, null);
        assertEquals(request, projected);
    }

    @Test
    void testReturnsNullIfNeitherRequestOrResource() {
        Object projected = provider.getAdaptable(new Object(), null);
        assertNull(projected);
    }

    @Test
    void testUnwrapsResource() {
        // once
        Resource testCase = new ResourceTypeForcingResourceWrapper(resource, "foo");
        Object projected = provider.getAdaptable(testCase, null);
        assertEquals(resource, projected);

        // more than once
        testCase = new ResourceTypeForcingResourceWrapper(testCase, "bar");
        testCase = new ResourceTypeForcingResourceWrapper(testCase, "foobar");
        projected = provider.getAdaptable(testCase, null);
        assertEquals(resource, projected);
    }

    @Test
    void testUnwrapsJakartaRequest() {
        // once
        SlingJakartaHttpServletRequest testCase =
                new ResourceTypeForcingJakartaRequestWrapper(request, resource, "foo");
        Object projected = provider.getAdaptable(testCase, null);
        assertEquals(request, projected);

        // more than once
        testCase = new ResourceTypeForcingJakartaRequestWrapper(testCase, resource, "bar");
        testCase = new ResourceTypeForcingJakartaRequestWrapper(testCase, resource, "foobar");
        projected = provider.getAdaptable(testCase, null);
        assertEquals(request, projected);
    }

    /**
     * @deprecated use {@link #testUnwrapsJakartaRequest()} instead
     */
    @Deprecated
    @Test
    void testUnwrapsJavaxRequest() {
        org.apache.sling.api.SlingHttpServletRequest javaxRequest =
                JakartaToJavaxRequestWrapper.toJavaxRequest(request);
        // once
        org.apache.sling.api.SlingHttpServletRequest testCase =
                new ResourceTypeForcingRequestWrapper(javaxRequest, resource, "foo");
        Object projected = provider.getAdaptable(testCase, null);
        assertEquals(javaxRequest, projected);

        // more than once
        testCase = new ResourceTypeForcingRequestWrapper(testCase, resource, "bar");
        testCase = new ResourceTypeForcingRequestWrapper(testCase, resource, "foobar");
        projected = provider.getAdaptable(testCase, null);
        assertEquals(javaxRequest, projected);
    }

    @Test
    void testDoesNotUnwrapOtherResourceWrappers() {
        Resource testCase = new ResourceWrapper(resource);
        Object projected = provider.getAdaptable(testCase, null);
        assertEquals(testCase, projected);
    }

    @Test
    void testDoesNotUnwrapOtherJakartaRequestWrappers() {
        SlingJakartaHttpServletRequest testCase = new SlingJakartaHttpServletRequestWrapper(request);
        Object projected = provider.getAdaptable(testCase, null);
        assertEquals(testCase, projected);
    }

    /**
     * @deprecated use {@link #testDoesNotUnwrapOtherJakartaRequestWrappers()} instead
     */
    @Deprecated
    @Test
    void testDoesNotUnwrapOtherJavaxRequestWrappers() {
        org.apache.sling.api.SlingHttpServletRequest javaxRequest =
                JakartaToJavaxRequestWrapper.toJavaxRequest(request);
        org.apache.sling.api.SlingHttpServletRequest testCase =
                new org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper(javaxRequest);
        Object projected = provider.getAdaptable(testCase, null);
        assertEquals(testCase, projected);
    }
}
