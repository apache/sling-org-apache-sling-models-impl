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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract base class for injectors to consolidate common functionality.
 */
abstract class AbstractInjector {

    @SuppressWarnings("deprecation")
    protected ResourceResolver getResourceResolver(Object adaptable) {
        ResourceResolver resolver = null;
        if (adaptable instanceof Resource r) {
            resolver = r.getResourceResolver();
        } else if (adaptable instanceof SlingJakartaHttpServletRequest jakartaRequest) {
            resolver = jakartaRequest.getResourceResolver();
        } else if (adaptable instanceof org.apache.sling.api.SlingHttpServletRequest javaxRequest) {
            resolver = javaxRequest.getResourceResolver();
        }
        return resolver;
    }

    /**
     * Retrieve the ValueMap from the given adaptable. This succeeds, if the adaptable is either
     * <ul>
     * <li>a {@link ValueMap},</li>
     * <li>a {@link SlingJakartaHttpServletRequest} or {@link org.apache.sling.api.SlingHttpServletRequest}, in which case the returned {@link ValueMap} is the one derived from the request's resource or</li>
     * <li>adaptable to a {@link ValueMap}.</li>
     * </ul>
     * Otherwise {@code null} is returned.
     * @param adaptable Adaptable
     * @return a ValueMap or {@code null}.
     */
    @SuppressWarnings("deprecation")
    protected @Nullable ValueMap getValueMap(Object adaptable) {
        ValueMap valueMap = null;
        if (adaptable instanceof ValueMap vm) {
            valueMap = vm;
        } else if (adaptable instanceof SlingJakartaHttpServletRequest jakartaRequest) {
            valueMap = toValueMap(jakartaRequest.getResource());
        } else if (adaptable instanceof org.apache.sling.api.SlingHttpServletRequest javaxRequest) {
            valueMap = toValueMap(javaxRequest.getResource());
        } else if (adaptable instanceof Adaptable a) {
            valueMap = a.adaptTo(ValueMap.class);
        }
        return valueMap;
    }

    protected @Nullable ValueMap toValueMap(final Resource resource) {
        ValueMap valueMap = null;
        // resource may be null for mocked adaptables, therefore do a check here
        if (resource != null) {
            valueMap = resource.adaptTo(ValueMap.class);
        }
        return valueMap;
    }

    protected boolean isDeclaredTypeCollection(Type declaredType) {
        boolean isCollection = false;
        if (declaredType instanceof ParameterizedType type) {
            Class<?> collectionType = (Class<?>) type.getRawType();
            isCollection = collectionType.equals(Collection.class) || collectionType.equals(List.class);
        }
        return isCollection;
    }
}
