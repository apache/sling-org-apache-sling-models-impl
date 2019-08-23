package org.apache.sling.models.impl.injectors;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.ExternalizePath;
import org.apache.sling.models.annotations.injectorspecific.ExternalizedPathProvider;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExternalizedPathInjectorTest {

    @Test
    public void testNoResolveInjection() {
        String imagePath = "/content/test/image/test-image.jpg";

        ExternalizedPathInjector injector = new ExternalizedPathInjector();
        Resource adaptable = mock(Resource.class);
        ValueMap valueMap = mock(ValueMap.class);
        when(adaptable.adaptTo(eq(ValueMap.class))).thenReturn(valueMap);
        String name = "imagePath";
        when(valueMap.get(eq(name), eq(String.class))).thenReturn(imagePath);
        Type type = String.class;
        AnnotatedElement element = mock(AnnotatedElement.class);
        when(element.isAnnotationPresent(eq(ExternalizePath.class))).thenReturn(true);
        DisposalCallbackRegistry callbackRegistry = mock(DisposalCallbackRegistry.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        when(adaptable.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.map(imagePath)).thenReturn(imagePath);

        Object value = injector.getValue(adaptable, name, type, element, callbackRegistry);
        assertEquals("No Mapping was expected", imagePath, value);
    }

    @Test
    public void testResolveInjection() {
        String imagePath = "/content/test/image/test-image.jpg";
        String mappedImagePath = "/image/test-image.jpg";

        ExternalizedPathInjector injector = new ExternalizedPathInjector();
        Resource adaptable = mock(Resource.class);
        ValueMap valueMap = mock(ValueMap.class);
        when(adaptable.adaptTo(eq(ValueMap.class))).thenReturn(valueMap);
        String name = "imagePath";
        when(valueMap.get(eq(name), eq(String.class))).thenReturn(imagePath);
        Type type = String.class;
        AnnotatedElement element = mock(AnnotatedElement.class);
        when(element.isAnnotationPresent(eq(ExternalizePath.class))).thenReturn(true);
        DisposalCallbackRegistry callbackRegistry = mock(DisposalCallbackRegistry.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        when(adaptable.getResourceResolver()).thenReturn(resourceResolver);
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
        String mappedImagePath1 = "/image1/test-image.jpg";
        String mappedImagePath2 = "/image2/test-image.jpg";
        String mappedImagePath3 = "/image3/test-image.jpg";

        ExternalizedPathInjector injector = new ExternalizedPathInjector();
        Resource adaptable = mock(Resource.class);
        ValueMap valueMap = mock(ValueMap.class);
        when(adaptable.adaptTo(eq(ValueMap.class))).thenReturn(valueMap);
        String name = "imagePath";
        when(valueMap.get(eq(name), eq(String.class))).thenReturn(imagePath);
        Type type = String.class;
        AnnotatedElement element = mock(AnnotatedElement.class);
        when(element.isAnnotationPresent(eq(ExternalizePath.class))).thenReturn(true);
        DisposalCallbackRegistry callbackRegistry = mock(DisposalCallbackRegistry.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        when(adaptable.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.map(imagePath)).thenReturn(mappedImagePath);

        TestExternalizedPathProvider provider1 = new TestExternalizedPathProvider(100, from, to1);
        injector.bindExternalizedPathProvider(provider1);
        TestExternalizedPathProvider provider3 = new TestExternalizedPathProvider(300, from, to3);
        injector.bindExternalizedPathProvider(provider3);
        TestExternalizedPathProvider provider2 = new TestExternalizedPathProvider(200, from, to2);
        injector.bindExternalizedPathProvider(provider2);

        Object value = injector.getValue(adaptable, name, type, element, callbackRegistry);
        assertEquals("Wrong Provider was selected", mappedImagePath3, value);
    }

    private class TestExternalizedPathProvider
        implements ExternalizedPathProvider
    {
        private int priority;
        private String from = "/";
        private String to = "/";

        public TestExternalizedPathProvider(int priority, String from, String to) {
            this.priority = priority;
            this.from = from;
            this.to = to;
        }
        @Override
        public int getPriority() { return priority; }

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
