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
package org.apache.sling.models.impl.model;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

import javax.inject.Inject;

import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.impl.AssignableFrom;
import org.apache.sling.models.impl.ReflectionUtil;
import org.apache.sling.models.spi.injectorspecific.StaticInjectAnnotationProcessorFactory;

public class ModelClassConstructor<M> {

    private final Constructor<M> constructor;
    private final boolean hasInjectAnnotation;
    private final ConstructorParameter[] constructorParametersArray;

    public ModelClassConstructor(Constructor<M> constructor, StaticInjectAnnotationProcessorFactory[] processorFactories, DefaultInjectionStrategy defaultInjectionStrategy) {
        this.constructor = constructor;
        this.hasInjectAnnotation = constructor.isAnnotationPresent(Inject.class);

        Type[] parameterTypes = constructor.getGenericParameterTypes();
        this.constructorParametersArray = new ConstructorParameter[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            Type genericType = ReflectionUtil.mapPrimitiveClasses(parameterTypes[i]);
            boolean isPrimitive = (parameterTypes[i] != genericType);
            this.constructorParametersArray[i] = new ConstructorParameter(
                    constructor.getParameterAnnotations()[i], constructor.getParameterTypes()[i], genericType, isPrimitive, i,
                    processorFactories, defaultInjectionStrategy);
        }
    }

    /**
     * Proxies the call to {@link Constructor#newInstance(Object...)}, checking (and
     * setting) accessibility first.
     * 
     * @param parameters
     *            the constructor parameters that are passed to
     *            {@link Constructor#newInstance(Object...)}
     * @return The constructed object
     * 
     * @throws InstantiationException when {@link Constructor#newInstance(Object...)} would throw
     * @throws IllegalAccessException when {@link Constructor#newInstance(Object...)} would throw
     * @throws IllegalArgumentException when {@link Constructor#newInstance(Object...)} would throw
     * @throws InvocationTargetException when {@link Constructor#newInstance(Object...)} would throw
     * 
     * @see Constructor#newInstance(Object...)
     */
    @SuppressWarnings({"java:S3011","java:S1874"})
    public M newInstance(Object... parameters) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        constructor.setAccessible(true);
        return constructor.newInstance(parameters);
    }

    public Constructor<M> getConstructor() {
        return constructor;
    }

    public boolean hasInjectAnnotation() {
        return hasInjectAnnotation;
    }

    public ConstructorParameter[] getConstructorParameters() {
        return constructorParametersArray;
    };

    public boolean isRecordConstructor() {
        Class<M> declaringClass = constructor.getDeclaringClass();
        AssignableFrom assignableFrom = new AssignableFrom() { };
        return assignableFrom.isAssignableFrom(declaringClass, "java.lang.Record");
    }
}
