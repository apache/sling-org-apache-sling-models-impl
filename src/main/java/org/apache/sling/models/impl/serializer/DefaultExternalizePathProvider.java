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
package org.apache.sling.models.impl.serializer;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.ExternalizePath;
import org.apache.sling.models.annotations.ExternalizePathProvider;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Fallback Implementation of the Externalized Path Provider that uses the Resource Resolver's map function **/
@Component(
    property = Constants.SERVICE_RANKING + ":Integer=1",
    immediate = true,
    service = {
        ExternalizePathProvider.class
    }
)
public class DefaultExternalizePathProvider
    implements ExternalizePathProvider
{
    @Override
    public String externalize(@NotNull Object model, ExternalizePath annotation, String sourcePath) {
        String answer = sourcePath;
        ResourceResolver resourceResolver = getResourceResolver(model, annotation);
        if (sourcePath != null && !sourcePath.isEmpty() && resourceResolver != null) {
            answer = resourceResolver.map(sourcePath);
        }
        return answer;
    }

    /**
     * Obtains the Resource from the Model in order to Externalize
     * @param model
     * @param annotation
     * @return
     */
    private ResourceResolver getResourceResolver(Object model, ExternalizePath annotation) {
        Resource answer = null;
        // Get Resource from specified Resource method
        String resourceMethodName = annotation.resourceMethod();
        if(!resourceMethodName.isEmpty()) {
            try {
                Method getResourceMethod = model.getClass().getMethod(resourceMethodName);
                if(getResourceMethod.getReturnType().isAssignableFrom(Resource.class)) {
                    answer = (Resource) getResourceMethod.invoke(model, null);
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                // If not found then we cannot use Externalize Path but then we just send the original value
            }
        }
        if(answer == null) {
            // Get Resource from specified Resource Field
            String resourceFieldName = annotation.resourceField();
            if (!resourceFieldName.isEmpty()) {
                try {
                    Field resourceField = FieldUtils.getField(model.getClass(), resourceFieldName, true);
                    if(resourceField != null && resourceField.getType().isAssignableFrom(Resource.class)) {
                        answer = (Resource) resourceField.get(model);
                    }
                } catch (IllegalAccessException e) {
                    // If not found then we cannot use Externalize Path but then we just send the original value
                }
            }
        }
        if(answer == null) {
            // Get Resource from default location (getResource())
            try {
                Method getResourceMethod = model.getClass().getMethod("getResource");
                if(getResourceMethod.getReturnType().isAssignableFrom(Resource.class)) {
                    answer = (Resource) getResourceMethod.invoke(model, null);
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                // If not found then we cannot use Externalize Path but then we just send the original value
            }
        }
        if(answer == null) {
            // Get Resource from default Resource Field (resource)
            try {
                Field resourceField = FieldUtils.getField(model.getClass(), "resource", true);
                if(resourceField != null && resourceField.getType().isAssignableFrom(Resource.class)) {
                    answer = (Resource) resourceField.get(model);
                }
            } catch (IllegalAccessException e) {
                // If not found then we cannot use Externalize Path but then we just send the original value
            }
        }
        return answer == null ? null : answer.getResourceResolver();
    }
}
