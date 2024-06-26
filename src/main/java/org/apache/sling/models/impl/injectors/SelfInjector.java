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

import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.impl.model.ConstructorParameter;
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
 * Injects the adaptable object itself.
 */
@Component(
        property = Constants.SERVICE_RANKING + ":Integer=" + Integer.MAX_VALUE,
        service = {Injector.class, StaticInjectAnnotationProcessorFactory.class, AcceptsNullName.class})
public class SelfInjector implements Injector, StaticInjectAnnotationProcessorFactory, AcceptsNullName {

    @Override
    public @NotNull String getName() {
        return "self";
    }

    public Object getValue(
            @NotNull Object adaptable,
            String name,
            @NotNull Type type,
            @NotNull AnnotatedElement element,
            @NotNull DisposalCallbackRegistry callbackRegistry) {
        // if the @Self annotation is present return the adaptable to be inserted directly or to be adapted from
        if (element.isAnnotationPresent(Self.class)) {
            return adaptable;
        } else {
            // special handling for the first constructor parameter
            // apply class-based injection only if class matches or is a superclass
            if (element instanceof ConstructorParameter.FakeAnnotatedElement
                    && ((ConstructorParameter.FakeAnnotatedElement) element).getParameterIndex() == 0
                    && type instanceof Class<?>
                    && ((Class<?>) type).isAssignableFrom(adaptable.getClass())) {
                return adaptable;
            }
        }
        return null;
    }

    @Override
    @SuppressWarnings({"unused", "null"})
    public InjectAnnotationProcessor2 createAnnotationProcessor(AnnotatedElement element) {
        // check if the element has the expected annotation
        Self annotation = element.getAnnotation(Self.class);
        if (annotation != null) {
            return new SelfAnnotationProcessor(annotation);
        }
        return null;
    }

    private static class SelfAnnotationProcessor extends AbstractInjectAnnotationProcessor2 {

        private final Self annotation;

        public SelfAnnotationProcessor(Self annotation) {
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
