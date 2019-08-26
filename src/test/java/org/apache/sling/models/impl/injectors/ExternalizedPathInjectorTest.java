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
package org.apache.sling.models.impl.injectors;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.ExternalizePath;
import org.apache.sling.models.annotations.injectorspecific.ExternalizePathProvider;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.osgi.framework.Constants.SERVICE_ID;
import static org.osgi.framework.Constants.SERVICE_RANKING;

public class ExternalizedPathInjectorTest {

    private ExternalizePathInjector injector;
    private Resource adaptable;
    private ValueMap valueMap;
    private Type type;
    private AnnotatedElement element;
    private DisposalCallbackRegistry callbackRegistry;
    private ResourceResolver resourceResolver;

    @Before
    public void setup() {
        injector = new ExternalizePathInjector();
        adaptable = mock(Resource.class);
        valueMap = mock(ValueMap.class);
        when(adaptable.adaptTo(eq(ValueMap.class))).thenReturn(valueMap);
        type = String.class;
        element = mock(AnnotatedElement.class);
        when(element.isAnnotationPresent(eq(ExternalizePath.class))).thenReturn(true);
        callbackRegistry = mock(DisposalCallbackRegistry.class);
        resourceResolver = mock(ResourceResolver.class);
        when(adaptable.getResourceResolver()).thenReturn(resourceResolver);
        ExternalizePathProvider defaultProvider = new DefaultExternalizePathProvider();
        Map<String, Object> props = new HashMap<>();
        props.put(SERVICE_ID, 1L);
        props.put(SERVICE_RANKING, 1);
        injector.bindExternalizePathProvider(defaultProvider, props);
    }

    @Test
    public void testNoResolveInjection() {
        String imagePath = "/content/test/image/test-image.jpg";
        String name = "imagePath";

        when(valueMap.get(eq(name), eq(String.class))).thenReturn(imagePath);
        when(resourceResolver.map(imagePath)).thenReturn(imagePath);

        Object value = injector.getValue(adaptable, name, type, element, callbackRegistry);
        assertEquals("No Mapping was expected", imagePath, value);
    }

    @Test
    public void testResolveInjection() {
        String imagePath = "/content/test/image/test-image.jpg";
        String mappedImagePath = "/image/test-image.jpg";
        String name = "imagePath";

        when(valueMap.get(eq(name), eq(String.class))).thenReturn(imagePath);
        when(resourceResolver.map(imagePath)).thenReturn(mappedImagePath);

        Object value = injector.getValue(adaptable, name, type, element, callbackRegistry);
        assertEquals("Mapping was expected", mappedImagePath, value);
    }

    @Test
    public void testCustomProviderInjection() {
        String imagePath = "/content/test/image/test-image.jpg";
        String from = "/content/test/image/";
        String to1 = "/image1/";
        String to2 = "/image2/";
        String to3 = "/image3/";
        String mappedImagePath = "/image/test-image.jpg";
        String mappedImagePath3 = "/image3/test-image.jpg";
        String name = "imagePath";

        when(valueMap.get(eq(name), eq(String.class))).thenReturn(imagePath);
        when(resourceResolver.map(imagePath)).thenReturn(mappedImagePath);

        TestExternalizePathProvider provider1 = new TestExternalizePathProvider(from, to1);
        // ATTENTION: Properties Map need to be reset as they are stored into the RankedServices as is
        Map<String, Object> props = new HashMap<>();
        props.put(SERVICE_ID, 1234L);
        props.put(SERVICE_RANKING, 100);
        injector.bindExternalizePathProvider(provider1, props);
        TestExternalizePathProvider provider3 = new TestExternalizePathProvider(from, to3);
        props = new HashMap<>();
        props.put(SERVICE_ID, 1235L);
        props.put(SERVICE_RANKING, 400);
        injector.bindExternalizePathProvider(provider3, props);
        TestExternalizePathProvider provider2 = new TestExternalizePathProvider(from, to2);
        props = new HashMap<>();
        props.put(SERVICE_ID, 1236L);
        props.put(SERVICE_RANKING, 200);
        injector.bindExternalizePathProvider(provider2, props);

        Object value = injector.getValue(adaptable, name, type, element, callbackRegistry);
        assertEquals("Wrong Provider was selected", mappedImagePath3, value);
    }

    private static class TestExternalizePathProvider
        implements ExternalizePathProvider
    {
        private String from = "/";
        private String to = "/";

        public TestExternalizePathProvider(String from, String to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public String externalize(@NotNull Object adaptable, String sourcePath) {
            String answer = sourcePath;
            if(sourcePath.startsWith(from)) {
                answer = to + sourcePath.substring(from.length());
            }
            return answer;
        }
    }
}
