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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(MockitoJUnitRunner.class)
public class OriginalResourceTypeViaProviderTest {

    private OriginalResourceTypeViaProvider provider = new OriginalResourceTypeViaProvider();

    @Mock
    private Resource resource;

    @Mock
    private SlingJakartaHttpServletRequest request;

    @Test
    public void testReturnsCorrectMarkerInterface() {
        assertEquals(OriginalResourceType.class, provider.getType());
    }

    @Test
    public void testReturnsOriginalResourceIfNotWrapped() {
        Object projected = provider.getAdaptable(resource, null);
        assertEquals(resource, projected);
    }

    @Test
    public void testReturnsOriginalRequestIfNotWrapped() {
        Object projected = provider.getAdaptable(request, null);
        assertEquals(request, projected);
    }

    @Test
    public void testReturnsNullIfNeitherRequestOrResource() {
        Object projected = provider.getAdaptable(new Object(), null);
        assertNull(projected);
    }

    @Test
    public void testUnwrapsResource() {
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
    public void testUnwrapsJakartaRequest() {
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
    public void testUnwrapsJavaxRequest() {
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
    public void testDoesNotUnwrapOtherResourceWrappers() {
        Resource testCase = new ResourceWrapper(resource);
        Object projected = provider.getAdaptable(testCase, null);
        assertEquals(testCase, projected);
    }

    @Test
    public void testDoesNotUnwrapOtherJakartaRequestWrappers() {
        SlingJakartaHttpServletRequest testCase = new SlingJakartaHttpServletRequestWrapper(request);
        Object projected = provider.getAdaptable(testCase, null);
        assertEquals(testCase, projected);
    }

    /**
     * @deprecated use {@link #testDoesNotUnwrapOtherJakartaRequestWrappers()} instead
     */
    @Deprecated
    @Test
    public void testDoesNotUnwrapOtherJavaxRequestWrappers() {
        org.apache.sling.api.SlingHttpServletRequest javaxRequest =
                JakartaToJavaxRequestWrapper.toJavaxRequest(request);
        org.apache.sling.api.SlingHttpServletRequest testCase =
                new org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper(javaxRequest);
        Object projected = provider.getAdaptable(testCase, null);
        assertEquals(testCase, projected);
    }
}
