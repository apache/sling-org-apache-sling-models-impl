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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.sling.models.annotations.ExternalizePath;
import org.apache.sling.models.annotations.ExternalizePathProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Json Serializer that will take 'Externalize Path' annotation and shortens them
 * with the current Externalize Path Provider. This Serializer is used as an Annotation
 * on the Model:
 *
 * @Exporter(name = "jackson", extensions = "json")
 * @JsonSerialize(using = ExternalizePathSerializer.class)
 *
 * ATTENTION: this class is no an OSGi class but it needs to obtain a service to the
 * {@link ExternalizePathProviderManager} and so this class can only be used in an OSGi
 * environment. There is also some restriction with respect to the Providers as they need
 * access to other services like the Resource Resolver.
 */
public class ExternalizePathSerializer
    extends JsonSerializer
{
    private Logger logger = LoggerFactory.getLogger(getClass());

    private ExternalizePathProviderManager externalizePathProviderManager =
        getService(ExternalizePathProviderManager.class, ExternalizePathProviderManager.class);

    @Override
    public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException
    {
        jgen.writeStartObject();
        try {
            if(value != null) {
                Class valueClass = value.getClass();
                // List all public methods (source of the Model)
                Method[] methods = value.getClass().getMethods();
                for(Method method: methods) {
                    // Ignore methods on Object class
                    if(method.getDeclaringClass() != Object.class) {
                        // Get Method Name, check if Method is Json Ignored and check that method is a Getter
                        String methodName = method.getName();
                        if(method.getAnnotation(JsonIgnore.class) != null) {
                            logger.debug("Ignore Method because of JsonIgnore Annotation: '{}'", methodName);
                        }
                        if ((methodName.startsWith("get") || methodName.startsWith("is")) && method.getParameterTypes().length == 0) {
                            Object property;
                            try {
                                // Obtain Value from method and get corresponding Field Name
                                property = method.invoke(value, null);
                                String fieldName = null;
                                if (methodName.startsWith("get")) {
                                    fieldName = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
                                } else if (methodName.startsWith("is")) {
                                    fieldName = methodName.substring(2, 3).toLowerCase() + methodName.substring(3);
                                }
                                if (property == null) {
                                    // If Property is null then write out a NULL
                                    jgen.writeNullField(fieldName);
                                } else {
                                    // Try to get the Annotation (Method or Field)
                                    ExternalizePath externalizePath = method.getAnnotation(ExternalizePath.class);
                                    if(externalizePath == null) {
                                        // If method does not have the Externalize Path Annotation then check its corresponding Field
                                        Field propertyField = FieldUtils.getField(valueClass, fieldName, true);
                                        if(propertyField != null) {
                                            // Check type
                                            Class fieldType = propertyField.getType();
                                            Class methodType = method.getReturnType();
                                            if (!fieldType.isAssignableFrom(methodType)) {
                                                logger.warn("Matching Field: '{}' is not assignable to method: '{}', ignore Annotation", fieldName, methodName);
                                            } else {
                                                externalizePath = propertyField.getAnnotation(ExternalizePath.class);
                                            }
                                        }
                                    }
                                    if(externalizePath != null) {
                                        // Enforce that this Annotation only works Strings and if so get the Externalization
                                        // Provider and if found externalize it
                                        if(!(property instanceof String)) {
                                            logger.warn(
                                                "Annotation 'Externalize Path' can only be applied to a String but was applied to: '{}'",
                                                method.getReturnType().getName()
                                            );
                                        } else {
                                            // If this method is Externalize Path then map the value first
                                            ExternalizePathProvider externalizePathProvider = getExternalizedPathProvider();
                                            if (externalizePathProvider != null) {
                                                property = externalizePathProvider.externalize(value, externalizePath, (String) property);
                                            }
                                        }
                                    }
                                    // Write Property out
                                    createProperty(jgen, fieldName, property, provider);
                                }
                            } catch (InvocationTargetException | RuntimeException e) {
                                logger.warn("Failed to Invoke Method: '{} -> ignored", e.getLocalizedMessage());
                            }
                        }
                    }
                }
            }
        } catch(JsonProcessingException | RuntimeException e) {
            logger.warn("Externalize Path Serialize failed", e);
        } catch (IllegalAccessException e) {
            logger.warn("Externalize Path Method Access failed", e);
        } finally {
            jgen.writeEndObject();
        }
    }

    void createProperty(final JsonGenerator jgen, final String name, final Object value,
                                final SerializerProvider provider)
        throws IOException {
        Object[] values = null;
        if (value.getClass().isArray()) {
            final int length = Array.getLength(value);
            // write out empty array
            if ( length == 0 ) {
                jgen.writeArrayFieldStart(name);
                jgen.writeEndArray();
                return;
            }
            values = new Object[Array.getLength(value)];
            for(int i=0; i<length; i++) {
                values[i] = Array.get(value, i);
            }
        }
        if (!value.getClass().isArray()) {
            jgen.writeFieldName(name);
            provider.defaultSerializeValue(value, jgen);
        } else {
            jgen.writeArrayFieldStart(name);
            for (Object v : values) {
                provider.defaultSerializeValue(v, jgen);
            }
            jgen.writeEndArray();
        }
    }

    /** @return Obtains the Externalize Path Provider Manager to obtain the highest Provider which is then returned **/
    private ExternalizePathProvider getExternalizedPathProvider() {
        ExternalizePathProvider answer = null;
        ExternalizePathProviderManager manager =
            this.externalizePathProviderManager != null ?
            this.externalizePathProviderManager :
            getService(ExternalizePathProviderManager.class, ExternalizePathProviderManager.class);
        if(manager != null) {
            answer = manager.getExternalizedPathProvider();
        } else {
            logger.warn("Externalized Path Provider Manager not found");
        }
        return answer;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> T getService(Class clazz, Class<T> type) {
        Bundle currentBundle = FrameworkUtil.getBundle(clazz);
        if (currentBundle == null) {
            return null;
        }
        BundleContext bundleContext = currentBundle.getBundleContext();
        if (bundleContext == null) {
            return null;
        }
        ServiceReference<T> serviceReference = bundleContext.getServiceReference(type);
        if (serviceReference == null) {
            return null;
        }
        T service = bundleContext.getService(serviceReference);
        if (service == null) {
            return null;
        }
        return service;
    }
}
