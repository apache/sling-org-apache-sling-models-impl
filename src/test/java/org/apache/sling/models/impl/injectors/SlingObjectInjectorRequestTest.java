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
package org.apache.sling.models.impl.injectors;

import java.lang.reflect.AnnotatedElement;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.felix.http.javaxwrappers.HttpServletRequestWrapper;
import org.apache.felix.http.javaxwrappers.HttpServletResponseWrapper;
import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.SlingJakartaHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.api.wrappers.JakartaToJavaxResponseWrapper;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SlingObjectInjectorRequestTest {

    private final SlingObjectInjector injector = new SlingObjectInjector();

    @Mock
    private AnnotatedElement annotatedElement;

    @Mock
    private SlingJakartaHttpServletRequest request;

    @Mock
    private SlingJakartaHttpServletResponse response;

    @Mock
    private SlingScriptHelper scriptHelper;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private Resource resource;

    @Mock
    private DisposalCallbackRegistry registry;

    @Before
    public void setUp() {
        SlingBindings bindings = new SlingBindings();
        bindings.put(SlingBindings.SLING, this.scriptHelper);
        when(this.request.getResourceResolver()).thenReturn(this.resourceResolver);
        when(this.request.getResource()).thenReturn(this.resource);
        when(this.request.getAttribute(SlingBindings.class.getName())).thenReturn(bindings);
        when(this.scriptHelper.getResponse()).thenReturn(JakartaToJavaxResponseWrapper.toJavaxResponse(this.response));
        when(this.scriptHelper.getJakartaResponse()).thenReturn(this.response);
    }

    @Test
    public void testResourceResolver() {
        Object result =
                this.injector.getValue(this.request, null, ResourceResolver.class, this.annotatedElement, registry);
        assertSame(this.resourceResolver, result);
    }

    @Test
    public void testResource() {
        Object result = this.injector.getValue(this.request, null, Resource.class, this.annotatedElement, registry);
        assertNull(result);

        when(annotatedElement.isAnnotationPresent(SlingObject.class)).thenReturn(true);
        result = this.injector.getValue(this.request, null, Resource.class, this.annotatedElement, registry);
        assertSame(resource, result);
    }

    @Test
    public void testJakartaRequest() {
        Object result = this.injector.getValue(
                this.request, null, SlingJakartaHttpServletRequest.class, this.annotatedElement, registry);
        assertSame(this.request, result);

        result = this.injector.getValue(this.request, null, HttpServletRequest.class, this.annotatedElement, registry);
        assertSame(this.request, result);
    }

    /**
     * @deprecated use {@link #testJakartaResponse()} instead
     */
    @Deprecated(since = "2.0.0")
    @Test
    public void testJavaxResponse() {
        Object result = this.injector.getValue(
                this.request,
                null,
                org.apache.sling.api.SlingHttpServletResponse.class,
                this.annotatedElement,
                registry);
        assertTrue(result instanceof HttpServletResponseWrapper);
        assertSame(this.response, ((HttpServletResponseWrapper) result).getResponse());

        result = this.injector.getValue(
                this.request, null, javax.servlet.http.HttpServletResponse.class, this.annotatedElement, registry);
        assertTrue(result instanceof HttpServletResponseWrapper);
        assertSame(this.response, ((HttpServletResponseWrapper) result).getResponse());
    }

    /**
     * @deprecated use {@link #testJakartaRequest()} instead
     */
    @Deprecated(since = "2.0.0")
    @Test
    public void testJavaxRequest() {
        Object result = this.injector.getValue(
                this.request,
                null,
                org.apache.sling.api.SlingHttpServletRequest.class,
                this.annotatedElement,
                registry);
        assertTrue(result instanceof HttpServletRequestWrapper);
        assertSame(this.request, ((HttpServletRequestWrapper) result).getRequest());

        result = this.injector.getValue(
                this.request, null, javax.servlet.http.HttpServletRequest.class, this.annotatedElement, registry);
        assertTrue(result instanceof HttpServletRequestWrapper);
        assertSame(this.request, ((HttpServletRequestWrapper) result).getRequest());
    }

    @Test
    public void testJakartaResponse() {
        Object result = this.injector.getValue(
                this.request, null, SlingJakartaHttpServletResponse.class, this.annotatedElement, registry);
        assertSame(this.response, result);

        result = this.injector.getValue(this.request, null, HttpServletResponse.class, this.annotatedElement, registry);
        assertSame(this.response, result);
    }

    @Test
    public void testScriptHelper() {
        Object result =
                this.injector.getValue(this.request, null, SlingScriptHelper.class, this.annotatedElement, registry);
        assertSame(this.scriptHelper, result);
    }

    @Test
    public void testInvalid() {
        Object result = this.injector.getValue(this, null, SlingScriptHelper.class, this.annotatedElement, registry);
        assertNull(result);
    }
}
