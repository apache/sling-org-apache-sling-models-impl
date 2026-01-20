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
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.wrappers.JakartaToJavaxRequestWrapper;
import org.apache.sling.models.annotations.injectorspecific.ResourcePath;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ResourcePathInjectorTest {

    private ResourcePathInjector injector = new ResourcePathInjector();

    @Mock
    private AnnotatedElement element;

    @Mock
    private DisposalCallbackRegistry registry;

    @Mock
    private Resource resource;

    @Mock
    private SlingJakartaHttpServletRequest jakartaRequest;

    @BeforeEach
    void setUp() {
        ResourcePath mockResourcePath = Mockito.mock(ResourcePath.class);
        lenient().when(mockResourcePath.path()).thenReturn("/resource1");
        lenient().when(element.getAnnotation(ResourcePath.class)).thenReturn(mockResourcePath);

        ResourceResolver mockRR = mock(ResourceResolver.class);
        lenient().when(mockRR.getResource("/resource1")).thenReturn(resource);
        lenient().when(jakartaRequest.getResourceResolver()).thenReturn(mockRR);
    }

    @Test
    void testResourcePathFromJakartaRequest() {
        Object result = this.injector.getValue(this.jakartaRequest, null, Resource.class, element, registry);
        assertEquals(result, this.resource);
    }

    /**
     * @deprecated use {@link #testResourcePathFromJakartaRequest()} instead
     */
    @Deprecated(since = "2.0.0")
    @Test
    void testResourcePathFromJavaxRequest() {
        org.apache.sling.api.SlingHttpServletRequest javaxRequest =
                JakartaToJavaxRequestWrapper.toJavaxRequest(this.jakartaRequest);
        Object result = this.injector.getValue(javaxRequest, null, Resource.class, element, registry);
        assertEquals(result, this.resource);
    }
}
