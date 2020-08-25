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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.ExternalizePath;
import org.apache.sling.models.annotations.ExternalizePathProvider;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.osgi.framework.Constants.SERVICE_ID;
import static org.osgi.framework.Constants.SERVICE_RANKING;

public class ExternalizedPathSerializerTest {

    private ExternalizePathSerializer externalizePathSerializer;
    private ExternalizePathProviderManagerService externalizePathProviderManagerService;
    private Resource resource;
    private ResourceResolver resourceResolver;
    private JsonGenerator jsonGenerator;
    private SerializerProvider serializerProvider;

    @Before
    public void setup() {
        externalizePathSerializer = spy(new ExternalizePathSerializer());
        externalizePathProviderManagerService = new ExternalizePathProviderManagerService();
        ExternalizePathProvider defaultProvider = new DefaultExternalizePathProvider();
        Map<String, Object> props = new HashMap<>();
        props.put(SERVICE_ID, 1L);
        props.put(SERVICE_RANKING, 1);
        externalizePathProviderManagerService.bindExternalizePathProvider(defaultProvider, props);

        resource = mock(Resource.class);
        jsonGenerator = mock(JsonGenerator.class);
        serializerProvider = mock(SerializerProvider.class);
        resourceResolver = mock(ResourceResolver.class);
        when(resource.getResourceResolver()).thenReturn(resourceResolver);
    }

    @Test
    public void testNoSerialization() throws Exception {
        final String imagePath = "/content/test/image/test-image.jpg";
        final String name = "imagePath";

        NoAnnotationModel model = new NoAnnotationModel(resource, imagePath);
        when(resourceResolver.map(imagePath)).thenReturn(imagePath);
        doAnswer(
            invocation -> {
                String fieldName = (String) invocation.getArguments()[1];
                Object value = invocation.getArguments()[2];
                if(fieldName.equals(name)) {
                    String stringValue = (String) value;
                    assertEquals("Image Path should not have changed", imagePath, stringValue);
                }
                return null;
            }
        ).when(externalizePathSerializer).createProperty(any(JsonGenerator.class), anyString(), any(Object.class), any(SerializerProvider.class));
        externalizePathSerializer.serialize(model, jsonGenerator, serializerProvider);
    }

    @Test
    public void testSimpleSerialization() throws Exception {
        final String imagePath = "/content/test/image/test-image.jpg";
        final String mappedImagePath = "/image/test-image.jpg";
        final String name = "imagePath";

        Whitebox.setInternalState(
            externalizePathSerializer, "externalizePathProviderManager", externalizePathProviderManagerService
        );
        MethodAnnotatedModel model = new MethodAnnotatedModel(resource, imagePath);
        when(resourceResolver.map(imagePath)).thenReturn(mappedImagePath);
        doAnswer(
            invocation -> {
                String fieldName = (String) invocation.getArguments()[1];
                Object value = invocation.getArguments()[2];
                if(fieldName.equals(name)) {
                    String stringValue = (String) value;
                    assertNotEquals("Image Path should have changed", imagePath, stringValue);
                    assertEquals("Image Path did not change as expected", mappedImagePath, stringValue);
                }
                return null;
            }
        ).when(externalizePathSerializer).createProperty(any(JsonGenerator.class), anyString(), any(Object.class), any(SerializerProvider.class));
        externalizePathSerializer.serialize(model, jsonGenerator, serializerProvider);
    }

    @Test
    public void testCustomProviderInjection() throws Exception {
        String imagePath = "/content/test/image/test-image.jpg";
        String from = "/content/test/image/";
        String to1 = "/image1/";
        String to2 = "/image2/";
        String to3 = "/image3/";
        String mappedImagePath = "/image/test-image.jpg";
        String mappedImagePath3 = "/image3/test-image.jpg";
        String name = "imagePath";

        when(resourceResolver.map(imagePath)).thenReturn(mappedImagePath);

        TestExternalizePathProvider provider1 = new TestExternalizePathProvider(from, to1);
        // ATTENTION: Properties Map need to be reset as they are stored into the RankedServices as is
        Map<String, Object> props = new HashMap<>();
        props.put(SERVICE_ID, 1234L);
        props.put(SERVICE_RANKING, 100);
        externalizePathProviderManagerService.bindExternalizePathProvider(provider1, props);
        TestExternalizePathProvider provider3 = new TestExternalizePathProvider(from, to3);
        props = new HashMap<>();
        props.put(SERVICE_ID, 1235L);
        props.put(SERVICE_RANKING, 400);
        externalizePathProviderManagerService.bindExternalizePathProvider(provider3, props);
        TestExternalizePathProvider provider2 = new TestExternalizePathProvider(from, to2);
        props = new HashMap<>();
        props.put(SERVICE_ID, 1236L);
        props.put(SERVICE_RANKING, 200);
        externalizePathProviderManagerService.bindExternalizePathProvider(provider2, props);

        Whitebox.setInternalState(
            externalizePathSerializer, "externalizePathProviderManager", externalizePathProviderManagerService
        );
        MethodAnnotatedModel model = new MethodAnnotatedModel(resource, imagePath);
        when(resourceResolver.map(imagePath)).thenReturn(mappedImagePath);
        doAnswer(
            invocation -> {
                String fieldName = (String) invocation.getArguments()[1];
                Object value = invocation.getArguments()[2];
                if(fieldName.equals(name)) {
                    String stringValue = (String) value;
                    assertNotEquals("Image Path should have changed", imagePath, stringValue);
                    assertEquals("Image Path did not change as expected", mappedImagePath3, stringValue);
                }
                return null;
            }
        ).when(externalizePathSerializer).createProperty(any(JsonGenerator.class), anyString(), any(Object.class), any(SerializerProvider.class));
        externalizePathSerializer.serialize(model, jsonGenerator, serializerProvider);
    }

    @Test
    public void testAnnotationInInheritanceSerialization() throws Exception {
        final String imagePath = "/content/test/image/test-image.jpg";
        final String testPath = "/content/testTest/image/test-image.jpg";
        final String mappedImagePath = "/image/test-image.jpg";
        final String mappedTestImagePath = "/image/test/test-image.jpg";
        final String name = "imagePath";
        final String testName = "testPath";

        Whitebox.setInternalState(
            externalizePathSerializer, "externalizePathProviderManager", externalizePathProviderManagerService
        );
        MethodAnnotatedInInheritanceModel model = new MethodAnnotatedInInheritanceModel(resource, imagePath, testPath);
        when(resourceResolver.map(eq(imagePath))).thenReturn(mappedImagePath);
        when(resourceResolver.map(eq(testPath))).thenReturn(mappedTestImagePath);
        doAnswer(
            invocation -> {
                String fieldName = (String) invocation.getArguments()[1];
                Object value = invocation.getArguments()[2];
                if(fieldName.equals(name)) {
                    String stringValue = (String) value;
                    assertNotEquals("Image Path should have changed", imagePath, stringValue);
                    assertEquals("Image Path did not change as expected", mappedImagePath, stringValue);
                } else if(fieldName.equals(testName)) {
                    String stringValue = (String) value;
                    assertNotEquals("Test Path should have changed", testPath, stringValue);
                    assertEquals("Test Path did not change as expected", mappedTestImagePath, stringValue);
                }
                return null;
            }
        ).when(externalizePathSerializer).createProperty(any(JsonGenerator.class), anyString(), any(Object.class), any(SerializerProvider.class));
        externalizePathSerializer.serialize(model, jsonGenerator, serializerProvider);
    }

    @Test
    public void testResourceMethod() throws Exception {
        final String imagePath = "/content/test/image/test-image.jpg";
        final String mappedImagePath = "/image/test-image.jpg";
        final String name = "imagePath";

        Whitebox.setInternalState(
            externalizePathSerializer, "externalizePathProviderManager", externalizePathProviderManagerService
        );
        AnnotationResourceMethodModel model = new AnnotationResourceMethodModel(resource, imagePath);
        when(resourceResolver.map(eq(imagePath))).thenReturn(mappedImagePath);
        doAnswer(
            invocation -> {
                String fieldName = (String) invocation.getArguments()[1];
                Object value = invocation.getArguments()[2];
                if(fieldName.equals(name)) {
                    String stringValue = (String) value;
                    assertNotEquals("Image Path should have changed", imagePath, stringValue);
                    assertEquals("Image Path did not change as expected", mappedImagePath, stringValue);
                }
                return null;
            }
        ).when(externalizePathSerializer).createProperty(any(JsonGenerator.class), anyString(), any(Object.class), any(SerializerProvider.class));
        externalizePathSerializer.serialize(model, jsonGenerator, serializerProvider);
    }

    @Test
    public void testResourceField() throws Exception {
        final String imagePath = "/content/test/image/test-image.jpg";
        final String mappedImagePath = "/image/test-image.jpg";
        final String name = "imagePath";

        Whitebox.setInternalState(
            externalizePathSerializer, "externalizePathProviderManager", externalizePathProviderManagerService
        );
        AnnotationResourceFieldModel model = new AnnotationResourceFieldModel(resource, imagePath);
        when(resourceResolver.map(eq(imagePath))).thenReturn(mappedImagePath);
        doAnswer(
            invocation -> {
                String fieldName = (String) invocation.getArguments()[1];
                Object value = invocation.getArguments()[2];
                if(fieldName.equals(name)) {
                    String stringValue = (String) value;
                    assertNotEquals("Image Path should have changed", imagePath, stringValue);
                    assertEquals("Image Path did not change as expected", mappedImagePath, stringValue);
                }
                return null;
            }
        ).when(externalizePathSerializer).createProperty(any(JsonGenerator.class), anyString(), any(Object.class), any(SerializerProvider.class));
        externalizePathSerializer.serialize(model, jsonGenerator, serializerProvider);
    }

    @Test
    public void testAnnotationOnField() throws Exception {
        final String imagePath = "/content/test/image/test-image.jpg";
        final String mappedImagePath = "/image/test-image.jpg";
        final String name = "imagePath";

        Whitebox.setInternalState(
            externalizePathSerializer, "externalizePathProviderManager", externalizePathProviderManagerService
        );
        FieldAnnotatedModel model = new FieldAnnotatedModel(resource, imagePath);
        when(resourceResolver.map(eq(imagePath))).thenReturn(mappedImagePath);
        doAnswer(
            invocation -> {
                String fieldName = (String) invocation.getArguments()[1];
                Object value = invocation.getArguments()[2];
                if(fieldName.equals(name)) {
                    String stringValue = (String) value;
                    assertNotEquals("Image Path should have changed", imagePath, stringValue);
                    assertEquals("Image Path did not change as expected", mappedImagePath, stringValue);
                }
                return null;
            }
        ).when(externalizePathSerializer).createProperty(any(JsonGenerator.class), anyString(), any(Object.class), any(SerializerProvider.class));
        externalizePathSerializer.serialize(model, jsonGenerator, serializerProvider);
    }

    @Test
    public void testAnnotationOnSubclassField() throws Exception {
        final String imagePath = "/content/test/image/test-image.jpg";
        final String mappedImagePath = "/image/test-image.jpg";
        final String name = "imagePath";

        Whitebox.setInternalState(
            externalizePathSerializer, "externalizePathProviderManager", externalizePathProviderManagerService
        );
        SubclassFieldAnnotatedModel model = new SubclassFieldAnnotatedModel(resource, imagePath);
        when(resourceResolver.map(eq(imagePath))).thenReturn(mappedImagePath);
        doAnswer(
            invocation -> {
                String fieldName = (String) invocation.getArguments()[1];
                Object value = invocation.getArguments()[2];
                if(fieldName.equals(name)) {
                    String stringValue = (String) value;
                    assertNotEquals("Image Path should have changed", imagePath, stringValue);
                    assertEquals("Image Path did not change as expected", mappedImagePath, stringValue);
                }
                return null;
            }
        ).when(externalizePathSerializer).createProperty(any(JsonGenerator.class), anyString(), any(Object.class), any(SerializerProvider.class));
        externalizePathSerializer.serialize(model, jsonGenerator, serializerProvider);
    }

    private abstract static class AbstractModel {
        private Resource resource;
        private String path;

        public AbstractModel(Resource resource, String path) {
            this.resource = resource;
            this.path = path;
        }

        public Resource getResource() { return resource; }

        public String getImagePath() {
            return path;
        }
    }

    private static class NoAnnotationModel extends AbstractModel {
        public NoAnnotationModel(Resource resource, String path) {
            super(resource, path);
        }

        public String getImagePath() {
            return super.getImagePath();
        }
    }

    private static class MethodAnnotatedModel extends AbstractModel {
        public MethodAnnotatedModel(Resource resource, String path) {
            super(resource, path);
        }

        @ExternalizePath
        public String getImagePath() {
            return super.getImagePath();
        }
    }

    private static class MethodAnnotatedInInheritanceModel
        extends MethodAnnotatedModel
    {
        @ExternalizePath
        private String testPath;

        public MethodAnnotatedInInheritanceModel(Resource resource, String path, String testPath) {
            super(resource, path);
            this.testPath = testPath;
        }

        public String getTestPath() {
            return testPath;
        }
    }

    private static class AnnotationResourceMethodModel {
        private Resource myResource;
        private String path;
        public AnnotationResourceMethodModel(Resource resource, String path) {
            this.myResource = resource;
            this.path = path;
        }

        @ExternalizePath(resourceMethod = "getMyResource")
        public String getImagePath() {
            return path;
        }

        public Resource getMyResource() {
            return myResource;
        }
    }

    private static class AnnotationResourceFieldModel {
        private Resource anotherResource;
        private String path;
        public AnnotationResourceFieldModel(Resource resource, String path) {
            this.anotherResource = resource;
            this.path = path;
        }

        @ExternalizePath(resourceField = "anotherResource")
        public String getImagePath() {
            return path;
        }

        public Resource getMyResource() {
            return anotherResource;
        }
    }

    private static class FieldAnnotatedModel {
        private Resource resource;
        @ExternalizePath
        private String imagePath;

        public FieldAnnotatedModel(Resource resource, String path) {
            this.resource = resource;
            this.imagePath = path;
        }

        public String getImagePath() {
            return imagePath;
        }
    }

    private static class SubclassFieldAnnotatedModel extends FieldAnnotatedModel {
        public SubclassFieldAnnotatedModel(Resource resource, String path) {
            super(resource, path);
        }
    }

    private static class TestExternalizePathProvider
        implements ExternalizePathProvider
    {
        private String from;
        private String to;

        public TestExternalizePathProvider(String from, String to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public String externalize(@NotNull Object model, ExternalizePath annocation, String sourcePath) {
            String answer = sourcePath;
            if(sourcePath.startsWith(from)) {
                answer = to + sourcePath.substring(from.length());
            }
            return answer;
        }
    }
}
