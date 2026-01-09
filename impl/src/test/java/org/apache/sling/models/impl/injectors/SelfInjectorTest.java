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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.wrappers.JakartaToJavaxRequestWrapper;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.impl.model.ConstructorParameter;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.injectorspecific.StaticInjectAnnotationProcessorFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SelfInjectorTest {

    private SelfInjector injector = new SelfInjector();

    @Mock
    private SlingJakartaHttpServletRequest request;

    @Mock
    private AnnotatedElement annotatedElement;

    @Mock
    private Model modelAnnotation;

    @Mock
    private DisposalCallbackRegistry registry;

    private ConstructorParameter firstConstructorParameter;
    private ConstructorParameter secondConstructorParameter;

    @Before
    public void setup() {
        lenient().when(modelAnnotation.defaultInjectionStrategy()).thenReturn(DefaultInjectionStrategy.REQUIRED);
        firstConstructorParameter = new ConstructorParameter(
                new Annotation[0],
                Object.class,
                Object.class,
                true,
                0,
                null,
                new StaticInjectAnnotationProcessorFactory[0],
                null);
        secondConstructorParameter = new ConstructorParameter(
                new Annotation[0],
                Object.class,
                Object.class,
                true,
                1,
                null,
                new StaticInjectAnnotationProcessorFactory[0],
                null);
    }

    @Test
    public void testJakartaMatchingClass() {
        assertSame(
                request,
                injector.getValue(
                        request,
                        "notRelevant",
                        SlingJakartaHttpServletRequest.class,
                        firstConstructorParameter.getAnnotatedElement(),
                        registry));
        assertNull(injector.getValue(
                request,
                "notRelevant",
                SlingJakartaHttpServletRequest.class,
                secondConstructorParameter.getAnnotatedElement(),
                registry));
        assertNull(injector.getValue(
                request, "notRelevant", SlingJakartaHttpServletRequest.class, annotatedElement, registry));
    }

    /**
     * @deprecated use {@link #testJakartaMatchingClass()} instead
     */
    @Deprecated
    @Test
    public void testJavaxMatchingClass() {
        org.apache.sling.api.SlingHttpServletRequest javaxRequest =
                JakartaToJavaxRequestWrapper.toJavaxRequest(request);
        assertSame(
                javaxRequest,
                injector.getValue(
                        javaxRequest,
                        "notRelevant",
                        org.apache.sling.api.SlingHttpServletRequest.class,
                        firstConstructorParameter.getAnnotatedElement(),
                        registry));
        assertNull(injector.getValue(
                javaxRequest,
                "notRelevant",
                org.apache.sling.api.SlingHttpServletRequest.class,
                secondConstructorParameter.getAnnotatedElement(),
                registry));
        assertNull(injector.getValue(
                javaxRequest,
                "notRelevant",
                org.apache.sling.api.SlingHttpServletRequest.class,
                annotatedElement,
                registry));
    }

    @Test
    public void testJakartaMatchingSubClass() {
        assertSame(
                request,
                injector.getValue(
                        request,
                        "notRelevant",
                        HttpServletRequest.class,
                        firstConstructorParameter.getAnnotatedElement(),
                        registry));
        assertNull(injector.getValue(
                request,
                "notRelevant",
                HttpServletRequest.class,
                secondConstructorParameter.getAnnotatedElement(),
                registry));
        assertNull(injector.getValue(request, "notRelevant", HttpServletRequest.class, annotatedElement, registry));
    }

    /**
     * @deprecated use {@link #testJakartaMatchingSubClass()} instead
     */
    @Deprecated
    @Test
    public void testJavaxMatchingSubClass() {
        org.apache.sling.api.SlingHttpServletRequest javaxRequest =
                JakartaToJavaxRequestWrapper.toJavaxRequest(request);
        assertSame(
                javaxRequest,
                injector.getValue(
                        javaxRequest,
                        "notRelevant",
                        javax.servlet.http.HttpServletRequest.class,
                        firstConstructorParameter.getAnnotatedElement(),
                        registry));
        assertNull(injector.getValue(
                javaxRequest,
                "notRelevant",
                javax.servlet.http.HttpServletRequest.class,
                secondConstructorParameter.getAnnotatedElement(),
                registry));
        assertNull(injector.getValue(
                javaxRequest, "notRelevant", javax.servlet.http.HttpServletRequest.class, annotatedElement, registry));
    }

    @Test
    public void testNotMatchingClass() {
        assertNull(injector.getValue(
                request,
                "notRelevant",
                ResourceResolver.class,
                firstConstructorParameter.getAnnotatedElement(),
                registry));
        assertNull(injector.getValue(
                request,
                "notRelevant",
                ResourceResolver.class,
                secondConstructorParameter.getAnnotatedElement(),
                registry));
        assertNull(injector.getValue(request, "notRelevant", ResourceResolver.class, annotatedElement, registry));
    }

    @Test
    public void testJakartaWithNullName() {
        assertSame(
                request,
                injector.getValue(
                        request,
                        null,
                        SlingJakartaHttpServletRequest.class,
                        firstConstructorParameter.getAnnotatedElement(),
                        registry));
        assertNull(injector.getValue(
                request,
                null,
                SlingJakartaHttpServletRequest.class,
                secondConstructorParameter.getAnnotatedElement(),
                registry));
        assertNull(injector.getValue(request, null, SlingJakartaHttpServletRequest.class, annotatedElement, registry));
    }

    /**
     * @deprecated use {@link #testJakartaWithNullName()} instead
     */
    @Deprecated
    @Test
    public void testJavaxWithNullName() {
        org.apache.sling.api.SlingHttpServletRequest javaxRequest =
                JakartaToJavaxRequestWrapper.toJavaxRequest(request);
        assertSame(
                javaxRequest,
                injector.getValue(
                        javaxRequest,
                        null,
                        org.apache.sling.api.SlingHttpServletRequest.class,
                        firstConstructorParameter.getAnnotatedElement(),
                        registry));
        assertNull(injector.getValue(
                javaxRequest,
                null,
                org.apache.sling.api.SlingHttpServletRequest.class,
                secondConstructorParameter.getAnnotatedElement(),
                registry));
        assertNull(injector.getValue(
                javaxRequest, null, org.apache.sling.api.SlingHttpServletRequest.class, annotatedElement, registry));
    }

    @Test
    public void testJakartaMatchingClassWithSelfAnnotation() {
        when(annotatedElement.isAnnotationPresent(Self.class)).thenReturn(true);
        Object result = injector.getValue(
                request, "notRelevant", SlingJakartaHttpServletRequest.class, annotatedElement, registry);
        assertSame(request, result);
    }

    /**
     * @deprecated use {@link #testJakartaMatchingClassWithSelfAnnotation()} instead
     */
    @Deprecated
    @Test
    public void testJavaxMatchingClassWithSelfAnnotation() {
        org.apache.sling.api.SlingHttpServletRequest javaxRequest =
                JakartaToJavaxRequestWrapper.toJavaxRequest(request);
        when(annotatedElement.isAnnotationPresent(Self.class)).thenReturn(true);
        Object result = injector.getValue(
                javaxRequest,
                "notRelevant",
                org.apache.sling.api.SlingHttpServletRequest.class,
                annotatedElement,
                registry);
        assertSame(javaxRequest, result);
    }

    @Test
    public void testNotMatchingClassWithSelfAnnotation() {
        when(annotatedElement.isAnnotationPresent(Self.class)).thenReturn(true);
        Object result = injector.getValue(request, "notRelevant", ResourceResolver.class, annotatedElement, registry);
        assertSame(request, result);
    }
}
