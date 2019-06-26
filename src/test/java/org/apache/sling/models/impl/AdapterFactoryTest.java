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
package org.apache.sling.models.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.export.spi.ModelExporter;
import org.apache.sling.models.factory.ExportException;
import org.apache.sling.models.factory.InvalidAdaptableException;
import org.apache.sling.models.factory.MissingElementsException;
import org.apache.sling.models.factory.MissingExporterException;
import org.apache.sling.models.factory.ModelClassException;
import org.apache.sling.models.impl.injectors.SelfInjector;
import org.apache.sling.models.impl.injectors.ValueMapInjector;
import org.apache.sling.models.testmodels.classes.CachedModelWithSelfReference;
import org.apache.sling.models.testmodels.classes.ConstructorWithExceptionModel;
import org.apache.sling.models.testmodels.classes.DefaultStringModel;
import org.apache.sling.models.testmodels.classes.InvalidModelWithMissingAnnotation;
import org.apache.sling.models.testmodels.classes.ResourceModelWithRequiredField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.Converters;

@RunWith(MockitoJUnitRunner.class)
public class AdapterFactoryTest {

    @Mock
    private Resource resource;

    @Mock
    private SlingHttpServletRequest request;

    private ModelAdapterFactory factory;

    public static ModelAdapterFactory createModelAdapterFactory() {
        BundleContext bundleContext = Mockito.mock(BundleContext.class);
        return createModelAdapterFactory(bundleContext);
    }
    
    public static ModelAdapterFactory createModelAdapterFactory(BundleContext bundleContext) {
        ComponentContext componentCtx = Mockito.mock(ComponentContext.class);
        when(componentCtx.getBundleContext()).thenReturn(bundleContext);

        ModelAdapterFactory factory = new ModelAdapterFactory();
        Converter c = Converters.standardConverter();
        Map<String, String> map = new HashMap<>();
        ModelAdapterFactoryConfiguration config = c.convert(map).to(ModelAdapterFactoryConfiguration.class);
        factory.activate(componentCtx, config);
        factory.injectAnnotationProcessorFactories = Collections.emptyList();
        factory.injectAnnotationProcessorFactories2 = Collections.emptyList();
        return factory;
    }

    @Before
    public void setup() {
        factory = createModelAdapterFactory();
        factory.bindInjector(new ValueMapInjector(), new ServicePropertiesMap(0, 0));
        factory.bindInjector(new SelfInjector(), new ServicePropertiesMap(1, 1));
        factory.modelExporters = Arrays.<ModelExporter>asList(new FirstStringExporter(), new SecondStringExporter(), new FirstIntegerExporter());
        factory.adapterImplementations.addClassesAsAdapterAndImplementation(DefaultStringModel.class, ConstructorWithExceptionModel.class, NestedModel.class, NestedModelWithInvalidAdaptable.class, NestedModelWithInvalidAdaptable2.class, ResourceModelWithRequiredField.class, CachedModelWithSelfReference.class) ;
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testIsModelClass() {
        Assert.assertTrue(factory.isModelClass(resource, DefaultStringModel.class));
        Assert.assertFalse(factory.isModelClass(resource, InvalidModelWithMissingAnnotation.class));
    }

    @Test
    public void testCanCreateFromAdaptable() {
        Assert.assertTrue(factory.canCreateFromAdaptable(resource, DefaultStringModel.class));
        Assert.assertFalse(factory.canCreateFromAdaptable(request, DefaultStringModel.class));
    }

    @Test
    public void testCanCreateFromAdaptableWithInvalidModel() {
        Assert.assertFalse(factory.canCreateFromAdaptable(resource, InvalidModelWithMissingAnnotation.class));
    }

    @Test(expected = ModelClassException.class)
    public void testCreateFromNonModelClass() {
        factory.createModel(resource, InvalidModelWithMissingAnnotation.class);
    }

    @Test(expected = InvalidAdaptableException.class)
    public void testCreateFromInvalidAdaptable() {
        factory.createModel(request, DefaultStringModel.class);
    }

    @Test(expected = RuntimeException.class)
    public void testCreateWithConstructorException() {
        // Internally all exceptions are wrapped within RuntimeExceptions
        factory.createModel(resource, ConstructorWithExceptionModel.class);
    }

    @Model(adaptables = SlingHttpServletRequest.class)
    public static class NestedModelWithInvalidAdaptable {
        @Self
        DefaultStringModel nestedModel;
    }

    @Test(expected = MissingElementsException.class)
    public void testCreatedNestedModelWithInvalidAdaptable() {
        // nested model can only be adapted from another adaptable
        factory.createModel(request, NestedModelWithInvalidAdaptable.class);
    }

    @Model(adaptables = SlingHttpServletRequest.class)
    public static class NestedModelWithInvalidAdaptable2 {
        @Self
        InvalidModelWithMissingAnnotation nestedModel;
    }

    @Test(expected = MissingElementsException.class)
    public void testCreatedNestedModelWithInvalidAdaptable2() {
        // nested model is in fact no valid model
        factory.createModel(request, NestedModelWithInvalidAdaptable2.class);
    }

    @Model(adaptables = Resource.class)
    public static class NestedModel {
        @Self
        ResourceModelWithRequiredField nestedModel;

        public ResourceModelWithRequiredField getNestedModel() {
            return nestedModel;
        }
    }

    @Test
    public void testCreatedNestedModel() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("required", "required");
        ValueMap vm = new ValueMapDecorator(map);
        when(resource.adaptTo(ValueMap.class)).thenReturn(vm);

        NestedModel model = factory.createModel(resource, NestedModel.class);
        Assert.assertNotNull(model);
        Assert.assertEquals("required", model.getNestedModel().getRequired());
    }

    @Test(expected=MissingElementsException.class)
    public void testCreatedNestedModelWithMissingElements() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("invalid", "required");
        ValueMap vm = new ValueMapDecorator(map);
        when(resource.adaptTo(ValueMap.class)).thenReturn(vm);

        factory.createModel(resource, NestedModel.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSelectExporterByName() throws Exception {
        Result<Object> result = mock(Result.class);
        when(result.wasSuccessful()).thenReturn(true);
        when(result.getValue()).thenReturn(new Object());

        String exported = factory.handleAndExportResult(result, "second", String.class, Collections.<String, String>emptyMap());
        Assert.assertEquals("Export from second", exported);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSelectExporterByType() throws Exception {
        Result<Object> result = mock(Result.class);
        when(result.wasSuccessful()).thenReturn(true);
        when(result.getValue()).thenReturn(new Object());

        Integer exported = factory.handleAndExportResult(result, "first", Integer.class, Collections.<String, String>emptyMap());
        Assert.assertEquals(Integer.valueOf(42), exported);
    }

    @Test(expected = MissingExporterException.class)
    @SuppressWarnings("unchecked")
    public void testSelectExporterByNameAndWrongType() throws Exception {
        Result<Object> result = mock(Result.class);
        when(result.wasSuccessful()).thenReturn(true);
        when(result.getValue()).thenReturn(new Object());

        factory.handleAndExportResult(result, "second", Integer.class, Collections.<String, String>emptyMap());
    }

    private static class FirstStringExporter implements ModelExporter {
        @Override
        public boolean isSupported(@NotNull Class<?> aClass) {
            return aClass == String.class;
        }

        @Nullable
        @Override
        @SuppressWarnings("unchecked")
        public <T> T export(@NotNull Object o, @NotNull Class<T> aClass, @NotNull Map<String, String> map) throws ExportException {
            if (aClass == String.class) {
                return (T) "Export from first";
            } else {
                throw new ExportException(String.format("%s is not supported.", aClass));
            }
        }

        @NotNull
        @Override
        public String getName() {
            return "first";
        }
    }

    @SuppressWarnings("unchecked")
    private static class SecondStringExporter implements ModelExporter {
        @Override
        public boolean isSupported(@NotNull Class<?> aClass) {
            return aClass == String.class;
        }

        @Nullable
        @Override
        public <T> T export(@NotNull Object o, @NotNull Class<T> aClass, @NotNull Map<String, String> map) throws ExportException {
            if (aClass == String.class) {
                return (T) "Export from second";
            } else {
                throw new ExportException(String.format("%s is not supported.", aClass));
            }
        }

        @NotNull
        @Override
        public String getName() {
            return "second";
        }
    }

    @SuppressWarnings("unchecked")
    private static class FirstIntegerExporter implements ModelExporter {
        @Override
        public boolean isSupported(@NotNull Class<?> aClass) {
            return aClass == Integer.class;
        }

        @Nullable
        @Override
        public <T> T export(@NotNull Object o, @NotNull Class<T> aClass, @NotNull Map<String, String> map) throws ExportException {
            if (aClass == Integer.class) {
                return (T) Integer.valueOf(42);
            } else {
                throw new ExportException(String.format("%s is not supported.", aClass));
            }
        }

        @NotNull
        @Override
        public String getName() {
            return "first";
        }
    }

	@Test
    public void testCreateCachedModelWillNotCrashTheVMWithOOM() throws Exception {
        /*
         * LOAD_FACTOR is used to ensure the test will try create instances of the model to fill up
         * HEAP_SIZE * LOAD_FACTOR memory. This should be a number > 1.0, to ensure that memory would be
         * exhausted, should this test fail.
         */
        double LOAD_FACTOR = 2.0;
        long instanceSize = sizeOf(new CachedModelWithSelfReference());
        long maxHeapSize = Runtime.getRuntime().maxMemory();
        long maxInstances = (long) ((maxHeapSize / instanceSize) * LOAD_FACTOR);
        
        for (long i = 0; i < maxInstances; i++) {
            factory.createModel(mock(SlingHttpServletRequest.class), CachedModelWithSelfReference.class);
        }
    }
     
    @SuppressWarnings({ "restriction", "rawtypes" })
    public static long sizeOf(Object o) throws Exception {
        Class<sun.misc.Unsafe> unsafeClass = sun.misc.Unsafe.class;
        Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        sun.misc.Unsafe theUnsafe = (sun.misc.Unsafe) unsafeField.get(unsafeClass);

        long maxSize = 0;
        Class c = o.getClass();
        while (c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                if ((f.getModifiers() & Modifier.STATIC) == 0) {
                    long offset = theUnsafe.objectFieldOffset(f);
                    if (offset > maxSize) {
                        maxSize = offset;
                    }
                }
            }
            c = c.getSuperclass();
        }

        return ((maxSize/8) + 1) * 8;
    }
}
