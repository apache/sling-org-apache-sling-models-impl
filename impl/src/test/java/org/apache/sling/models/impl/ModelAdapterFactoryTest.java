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
package org.apache.sling.models.impl;

import java.util.Map;
import java.util.function.IntSupplier;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.adapter.AdapterManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.export.spi.ModelExporter;
import org.apache.sling.models.factory.ExportException;
import org.apache.sling.models.factory.MissingExporterException;
import org.apache.sling.models.factory.ModelClassException;
import org.apache.sling.models.testutil.ModelAdapterFactoryUtil;
import org.apache.sling.scripting.api.BindingsValuesProvidersByContext;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ModelAdapterFactoryTest {

    @Rule
    public final OsgiContext context = new OsgiContext();

    @Mock
    private AdapterManager adapterManager;

    @Mock
    private BindingsValuesProvidersByContext bindingsValuesProvidersByContext;

    @Mock
    private SlingJakartaHttpServletRequest jakartaRequest;

    /**
     * @deprecated use {@link #jakartaRequest} instead
     */
    @SuppressWarnings("deprecation")
    @Deprecated(since = "2.0.0")
    @Mock
    private org.apache.sling.api.SlingHttpServletRequest javaxRequest;

    @Mock
    private Resource resource;

    @Mock
    private ResourceResolver resourceResolver;

    private ModelAdapterFactory factory;

    @SuppressWarnings("deprecation")
    @Before
    public void setUp() {
        when(resourceResolver.getSearchPath()).thenReturn(new String[] {"/apps/", "/libs/"});

        context.registerService(BindingsValuesProvidersByContext.class, bindingsValuesProvidersByContext);
        context.registerService(AdapterManager.class, adapterManager);
        factory = context.registerInjectActivateService(ModelAdapterFactory.class);

        ModelAdapterFactoryUtil.addModelsForPackage(context.bundleContext(), JakartaModel1.class, JavaxModel2.class);

        Mockito.when(resource.getResourceType()).thenReturn("nt:unstructured");
        Mockito.when(resource.getResourceResolver()).thenReturn(resourceResolver);
        Mockito.when(jakartaRequest.getResource()).thenReturn(resource);
        Mockito.when(javaxRequest.getResource()).thenReturn(resource);
    }

    protected void registerModel(final Class<?> adaptableType, final Class<?> clazz) {
        factory.adapterImplementations.registerModelToResourceType(
                context.bundleContext().getBundle(), "nt:unstructured", adaptableType, clazz);
    }

    /**
     * Test method for {@link org.apache.sling.models.impl.ModelAdapterFactory#createModelFromWrappedRequest(org.apache.sling.api.SlingHttpServletRequest, org.apache.sling.api.resource.Resource, java.lang.Class)}.
     * @deprecated use {@link #testCreateModelFromWrappedRequestSlingJakartaHttpServletRequestResourceClassOfT()} instead
     */
    @Deprecated(since = "2.0.0")
    @Test
    public void testCreateModelFromWrappedRequestSlingHttpServletRequestResourceClassOfT() {
        assertNotNull(factory.createModelFromWrappedRequest(javaxRequest, resource, JavaxModel2.class));
    }

    /**
     * Test method for {@link org.apache.sling.models.impl.ModelAdapterFactory#createModelFromWrappedRequest(org.apache.sling.api.SlingJakartaHttpServletRequest, org.apache.sling.api.resource.Resource, java.lang.Class)}.
     */
    @Test
    public void testCreateModelFromWrappedRequestSlingJakartaHttpServletRequestResourceClassOfT() {
        assertNotNull(factory.createModelFromWrappedRequest(jakartaRequest, resource, JakartaModel1.class));
    }

    /**
     * Test method for {@link org.apache.sling.models.impl.ModelAdapterFactory#isModelClass(java.lang.Object, java.lang.Class)}.
     * @deprecated use {@link #testIsModelClassClassOfQ()} instead
     */
    @Deprecated(since = "2.0.0")
    @Test
    public void testIsModelClassObjectClassOfQ() {
        assertTrue(factory.isModelClass(jakartaRequest, JakartaModel1.class));
        assertTrue(factory.isModelClass(jakartaRequest, JavaxModel2.class));
        assertFalse(factory.isModelClass(jakartaRequest, Object.class));
    }

    /**
     * Test method for {@link org.apache.sling.models.impl.ModelAdapterFactory#isModelClass(java.lang.Class)}.
     */
    @SuppressWarnings("deprecation")
    @Test
    public void testIsModelClassClassOfQ() {
        assertTrue(factory.isModelClass(JakartaModel1.class));
        assertTrue(factory.isModelClass(JavaxModel2.class));
        assertFalse(factory.isModelClass(Object.class));
    }

    /**
     * Test method for {@link org.apache.sling.models.impl.ModelAdapterFactory#isModelAvailableForRequest(org.apache.sling.api.SlingHttpServletRequest)}.
     * @deprecated use {@link #testIsModelAvailableForRequestSlingJakartaHttpServletRequest()} instead
     */
    @Deprecated(since = "2.0.0")
    @Test
    public void testIsModelAvailableForRequestSlingHttpServletRequest() {
        assertFalse(factory.isModelAvailableForRequest(javaxRequest));

        registerModel(org.apache.sling.api.SlingHttpServletRequest.class, JavaxModel2.class);

        assertTrue(factory.isModelAvailableForRequest(javaxRequest));
    }

    /**
     * Test method for {@link org.apache.sling.models.impl.ModelAdapterFactory#isModelAvailableForRequest(org.apache.sling.api.SlingJakartaHttpServletRequest)}.
     */
    @Test
    public void testIsModelAvailableForRequestSlingJakartaHttpServletRequest() {
        assertFalse(factory.isModelAvailableForRequest(jakartaRequest));

        registerModel(SlingJakartaHttpServletRequest.class, JakartaModel1.class);

        assertTrue(factory.isModelAvailableForRequest(jakartaRequest));
    }

    /**
     * Test method for {@link org.apache.sling.models.impl.ModelAdapterFactory#getModelFromResource(org.apache.sling.api.resource.Resource)}.
     */
    @Test
    public void testGetModelFromResource() {
        assertThrows(ModelClassException.class, () -> factory.getModelFromResource(resource));

        registerModel(Resource.class, JakartaModel1.class);

        Object modelFromResource = factory.getModelFromResource(resource);
        assertTrue(modelFromResource instanceof JakartaModel1);
    }

    /**
     * Test method for {@link org.apache.sling.models.impl.ModelAdapterFactory#getModelFromRequest(org.apache.sling.api.SlingHttpServletRequest)}.
     * @deprecated use {@link #testGetModelFromRequestSlingJakartaHttpServletRequest()} instead
     */
    @SuppressWarnings("deprecation")
    @Deprecated(since = "2.0.0")
    @Test
    public void testGetModelFromRequestSlingHttpServletRequest() {
        assertThrows(ModelClassException.class, () -> factory.getModelFromRequest(javaxRequest));

        registerModel(org.apache.sling.api.SlingHttpServletRequest.class, JavaxModel2.class);

        Object modelFromRequest = factory.getModelFromRequest(javaxRequest);
        assertTrue(modelFromRequest instanceof JavaxModel2);
    }

    /**
     * Test method for {@link org.apache.sling.models.impl.ModelAdapterFactory#getModelFromRequest(org.apache.sling.api.SlingJakartaHttpServletRequest)}.
     */
    @Test
    public void testGetModelFromRequestSlingJakartaHttpServletRequest() {
        assertThrows(ModelClassException.class, () -> factory.getModelFromRequest(jakartaRequest));

        registerModel(SlingJakartaHttpServletRequest.class, JakartaModel1.class);

        Object modelFromRequest = factory.getModelFromRequest(jakartaRequest);
        assertTrue(modelFromRequest instanceof JakartaModel1);
    }

    /**
     * Test method for {@link org.apache.sling.models.impl.ModelAdapterFactory#exportModelForRequest(org.apache.sling.api.SlingHttpServletRequest, java.lang.String, java.lang.Class, java.util.Map)}.
     * @deprecated use {@link #testExportModelForRequestSlingJakartaHttpServletRequestStringClassOfTMapOfStringString()} instead
     */
    @Deprecated(since = "2.0.0")
    @SuppressWarnings({"unchecked", "deprecation"})
    @Test
    public void testExportModelForRequestSlingHttpServletRequestStringClassOfTMapOfStringString()
            throws ExportException, MissingExporterException {
        Map<String, String> options = Map.of();
        assertThrows(
                ModelClassException.class,
                () -> factory.exportModelForRequest(javaxRequest, "exporter1", JavaxModel2.class, options));

        ModelExporter mockExporter = context.registerService(ModelExporter.class, Mockito.mock(ModelExporter.class));
        Mockito.when(mockExporter.getName()).thenReturn("exporter1");
        Mockito.when(mockExporter.isSupported(JavaxModel2.class)).thenReturn(true);
        Mockito.when(mockExporter.export(any(), any(Class.class), anyMap())).thenAnswer(invocation -> {
            return invocation.getArgument(0, JavaxModel2.class);
        });

        registerModel(org.apache.sling.api.SlingHttpServletRequest.class, JavaxModel2.class);

        JavaxModel2 exportModelForRequest =
                factory.exportModelForRequest(javaxRequest, "exporter1", JavaxModel2.class, options);
        assertNotNull(exportModelForRequest);
    }

    /**
     * Test method for {@link org.apache.sling.models.impl.ModelAdapterFactory#exportModelForRequest(org.apache.sling.api.SlingJakartaHttpServletRequest, java.lang.String, java.lang.Class, java.util.Map)}.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testExportModelForRequestSlingJakartaHttpServletRequestStringClassOfTMapOfStringString()
            throws ExportException, MissingExporterException {
        Map<String, String> options = Map.of();
        assertThrows(
                ModelClassException.class,
                () -> factory.exportModelForRequest(jakartaRequest, "exporter1", JakartaModel1.class, options));

        ModelExporter mockExporter = context.registerService(ModelExporter.class, Mockito.mock(ModelExporter.class));
        Mockito.when(mockExporter.getName()).thenReturn("exporter1");
        Mockito.when(mockExporter.isSupported(JakartaModel1.class)).thenReturn(true);
        Mockito.when(mockExporter.export(any(), any(Class.class), anyMap())).thenAnswer(invocation -> {
            return invocation.getArgument(0, JakartaModel1.class);
        });

        registerModel(SlingJakartaHttpServletRequest.class, JakartaModel1.class);

        JakartaModel1 exportModelForRequest =
                factory.exportModelForRequest(jakartaRequest, "exporter1", JakartaModel1.class, options);
        assertNotNull(exportModelForRequest);
    }

    /**
     * Test method for {@link org.apache.sling.models.impl.ModelAdapterFactory#getModelFromWrappedRequest(org.apache.sling.api.SlingHttpServletRequest, org.apache.sling.api.resource.Resource, java.lang.Class)}.
     * @deprecated use {@link #testGetModelFromWrappedRequestSlingJakartaHttpServletRequestResourceClassOfT()} instead
     */
    @Deprecated(since = "2.0.0")
    @Test
    public void testGetModelFromWrappedRequestSlingHttpServletRequestResourceClassOfT() {
        JavaxModel2 modelFromWrappedRequest =
                factory.getModelFromWrappedRequest(javaxRequest, resource, JavaxModel2.class);
        assertNull(modelFromWrappedRequest);

        JavaxModel2 target = new JavaxModel2();
        when(adapterManager.getAdapter(any(org.apache.sling.api.SlingHttpServletRequest.class), eq(JavaxModel2.class)))
                .thenReturn(target);

        registerModel(org.apache.sling.api.SlingHttpServletRequest.class, JavaxModel2.class);

        modelFromWrappedRequest = factory.getModelFromWrappedRequest(javaxRequest, resource, JavaxModel2.class);
        assertNotNull(modelFromWrappedRequest);
    }

    /**
     * Test method for {@link org.apache.sling.models.impl.ModelAdapterFactory#getModelFromWrappedRequest(org.apache.sling.api.SlingJakartaHttpServletRequest, org.apache.sling.api.resource.Resource, java.lang.Class)}.
     */
    @Test
    public void testGetModelFromWrappedRequestSlingJakartaHttpServletRequestResourceClassOfT() {
        assertNotNull(factory.createModelFromWrappedRequest(jakartaRequest, resource, JakartaModel1.class));

        JakartaModel1 target = new JakartaModel1();
        when(adapterManager.getAdapter(any(SlingJakartaHttpServletRequest.class), eq(JakartaModel1.class)))
                .thenReturn(target);

        registerModel(SlingJakartaHttpServletRequest.class, JakartaModel1.class);

        JakartaModel1 modelFromWrappedRequest =
                factory.getModelFromWrappedRequest(jakartaRequest, resource, JakartaModel1.class);
        assertNotNull(modelFromWrappedRequest);
    }

    @Model(
            adaptables = {SlingJakartaHttpServletRequest.class, Resource.class},
            cache = true)
    static final class JakartaModel1 implements IntSupplier {
        @Override
        public int getAsInt() {
            return 1;
        }
    }

    /**
     * @deprecated use {@link JakartaModel1)} instead
     */
    @Deprecated(since = "2.0.0")
    @Model(adaptables = org.apache.sling.api.SlingHttpServletRequest.class, cache = true)
    @Exporter(name = "exporter1", extensions = "json")
    static final class JavaxModel2 implements IntSupplier {
        @Override
        public int getAsInt() {
            return 2;
        }
    }
}
