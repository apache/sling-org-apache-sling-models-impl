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
import java.lang.reflect.Type;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.SlingJakartaHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.api.wrappers.JakartaToJavaxRequestWrapper;
import org.apache.sling.api.wrappers.JavaxToJakartaRequestWrapper;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.spi.AcceptsNullName;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.Injector;
import org.apache.sling.models.spi.injectorspecific.AbstractInjectAnnotationProcessor2;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessor2;
import org.apache.sling.models.spi.injectorspecific.StaticInjectAnnotationProcessorFactory;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

/**
 * Injects common Sling objects that can be derived from either a SlingHttpServletRequest, a ResourceResolver or a
 * Resource.
 * Documentation see {@link SlingObject}.
 */
@Component(
        property = Constants.SERVICE_RANKING + ":Integer=" + Integer.MAX_VALUE,
        service = {Injector.class, StaticInjectAnnotationProcessorFactory.class, AcceptsNullName.class})
public final class SlingObjectInjector implements Injector, StaticInjectAnnotationProcessorFactory, AcceptsNullName {

    /**
     * Injector name
     */
    public static final @NotNull String NAME = "sling-object";

    @Override
    public @NotNull String getName() {
        return NAME;
    }

    @Override
    public Object getValue(
            final @NotNull Object adaptable,
            final String name,
            final @NotNull Type type,
            final @NotNull AnnotatedElement element,
            final @NotNull DisposalCallbackRegistry callbackRegistry) {

        // only class types are supported
        if (!(type instanceof Class<?>)) {
            return null;
        }
        Class<?> requestedClass = (Class<?>) type;

        SlingJakartaHttpServletRequest jakartaRequest = null;
        Supplier<org.apache.sling.api.SlingHttpServletRequest> javaxRequestSupplier = null;
        if (adaptable instanceof SlingJakartaHttpServletRequest jr) {
            jakartaRequest = jr;
            javaxRequestSupplier = () -> JakartaToJavaxRequestWrapper.toJavaxRequest(jr);
        } else if (adaptable instanceof org.apache.sling.api.SlingHttpServletRequest r) {
            jakartaRequest = JavaxToJakartaRequestWrapper.toJakartaRequest(r);
            javaxRequestSupplier = () -> r;
        }
        // validate input
        if (jakartaRequest != null) {
            if (requestedClass.equals(ResourceResolver.class)) {
                return jakartaRequest.getResourceResolver();
            }
            if (requestedClass.equals(Resource.class) && element.isAnnotationPresent(SlingObject.class)) {
                return jakartaRequest.getResource();
            }
            if (requestedClass.equals(SlingJakartaHttpServletRequest.class)
                    || requestedClass.equals(jakarta.servlet.http.HttpServletRequest.class)) {
                return jakartaRequest;
            }
            if (requestedClass.equals(SlingJakartaHttpServletResponse.class)
                    || requestedClass.equals(jakarta.servlet.http.HttpServletResponse.class)) {
                return getScriptHelperItem(jakartaRequest, SlingScriptHelper::getJakartaResponse);
            }
            if (requestedClass.equals(org.apache.sling.api.SlingHttpServletRequest.class)
                    || requestedClass.equals(javax.servlet.http.HttpServletRequest.class)) {
                return javaxRequestSupplier.get();
            }
            if (requestedClass.equals(org.apache.sling.api.SlingHttpServletResponse.class)
                    || requestedClass.equals(javax.servlet.http.HttpServletResponse.class)) {
                return getScriptHelperItem(jakartaRequest, SlingScriptHelper::getResponse);
            }
            if (requestedClass.equals(SlingScriptHelper.class)) {
                return getSlingJakartaScriptHelper(jakartaRequest);
            }
        } else if (adaptable instanceof ResourceResolver resourceResolver) {
            if (requestedClass.equals(ResourceResolver.class)) {
                return resourceResolver;
            }
        } else if (adaptable instanceof Resource resource) {
            if (requestedClass.equals(ResourceResolver.class)) {
                return resource.getResourceResolver();
            }
            if (requestedClass.equals(Resource.class) && element.isAnnotationPresent(SlingObject.class)) {
                return resource;
            }
        }

        return null;
    }

    private SlingScriptHelper getSlingJakartaScriptHelper(final SlingJakartaHttpServletRequest request) {
        SlingScriptHelper value = null;
        SlingBindings bindings = (SlingBindings) request.getAttribute(SlingBindings.class.getName());
        if (bindings != null) {
            value = bindings.getSling();
        }
        return value;
    }

    private <T> T getScriptHelperItem(final SlingJakartaHttpServletRequest request, Function<SlingScriptHelper, T> fn) {
        T value = null;
        SlingScriptHelper scriptHelper = getSlingJakartaScriptHelper(request);
        if (scriptHelper != null) {
            value = fn.apply(scriptHelper);
        }
        return value;
    }

    @Override
    @SuppressWarnings({"unused", "null"})
    public InjectAnnotationProcessor2 createAnnotationProcessor(final AnnotatedElement element) {
        // check if the element has the expected annotation
        SlingObject annotation = element.getAnnotation(SlingObject.class);
        if (annotation != null) {
            return new SlingObjectAnnotationProcessor(annotation);
        }
        return null;
    }

    private static class SlingObjectAnnotationProcessor extends AbstractInjectAnnotationProcessor2 {

        private final SlingObject annotation;

        public SlingObjectAnnotationProcessor(final SlingObject annotation) {
            this.annotation = annotation;
        }

        @Override
        public InjectionStrategy getInjectionStrategy() {
            return annotation.injectionStrategy();
        }

        @Override
        @SuppressWarnings("deprecation")
        public Boolean isOptional() {
            return annotation.optional();
        }
    }
}
