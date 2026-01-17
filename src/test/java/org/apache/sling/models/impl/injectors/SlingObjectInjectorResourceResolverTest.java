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

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.SlingJakartaHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

@ExtendWith(MockitoExtension.class)
class SlingObjectInjectorResourceResolverTest {

    private final SlingObjectInjector injector = new SlingObjectInjector();

    @Mock
    private AnnotatedElement annotatedElement;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private DisposalCallbackRegistry registry;

    @Test
    void testResourceResolver() {
        Object result = this.injector.getValue(
                this.resourceResolver, null, ResourceResolver.class, this.annotatedElement, registry);
        assertSame(this.resourceResolver, result);
    }

    @Test
    void testResource() {
        Object result =
                this.injector.getValue(this.resourceResolver, null, Resource.class, this.annotatedElement, registry);
        assertNull(result);
    }

    /**
     * @deprecated use {@link #testJakartaRequest()} instead
     */
    @Deprecated(since = "2.0.0")
    @Test
    void testJavaxRequest() {
        Object result = this.injector.getValue(
                this.resourceResolver,
                null,
                org.apache.sling.api.SlingHttpServletRequest.class,
                this.annotatedElement,
                registry);
        assertNull(result);
    }

    /**
     * @deprecated use {@link #testJakartaResponse()} instead
     */
    @Deprecated(since = "2.0.0")
    @Test
    void testJavaxResponse() {
        Object result = this.injector.getValue(
                this.resourceResolver,
                null,
                org.apache.sling.api.SlingHttpServletResponse.class,
                this.annotatedElement,
                registry);
        assertNull(result);
    }

    @Test
    void testJakartaRequest() {
        Object result = this.injector.getValue(
                this.resourceResolver, null, SlingJakartaHttpServletRequest.class, this.annotatedElement, registry);
        assertNull(result);
    }

    @Test
    void testJakartaResponse() {
        Object result = this.injector.getValue(
                this.resourceResolver, null, SlingJakartaHttpServletResponse.class, this.annotatedElement, registry);
        assertNull(result);
    }

    @Test
    void testScriptHelper() {
        Object result = this.injector.getValue(
                this.resourceResolver, null, SlingScriptHelper.class, this.annotatedElement, registry);
        assertNull(result);
    }

    @Test
    void testInvalid() {
        Object result = this.injector.getValue(this, null, SlingScriptHelper.class, this.annotatedElement, registry);
        assertNull(result);
    }
}
