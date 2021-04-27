/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.models.impl.model;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.Optional;

import org.apache.sling.models.annotations.ViaProviderType;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessor;

public class OptionalTypedInjectableElement implements InjectableElement {

    private final InjectableElement element;
    private final Type type;

    public OptionalTypedInjectableElement(InjectableElement element, Type type) {
        this.element = element;
        this.type = type;
    }

    @Override
    public AnnotatedElement getAnnotatedElement() {
        return element.getAnnotatedElement();
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public boolean isPrimitive() {
        return element.isPrimitive();
    }

    @Override
    public String getName() {
        return element.getName();
    }

    @Override
    public String getSource() {
        return element.getSource();
    }

    @Override
    public String getVia() {
        return element.getVia();
    }

    @Override
    public Class<? extends ViaProviderType> getViaProviderType() {
        return element.getViaProviderType();
    }

    @Override
    public boolean hasDefaultValue() {
        return element.hasDefaultValue();
    }

    @Override
    public Object getDefaultValue() {
        // Default value injector will be evaluated last, make sure we return a value here so the injection is successful
        return element.getDefaultValue() == null ? Optional.empty() : element.getDefaultValue();
    }

    @Override
    public boolean isOptional(InjectAnnotationProcessor annotationProcessor) {
        return true;
    }
}
