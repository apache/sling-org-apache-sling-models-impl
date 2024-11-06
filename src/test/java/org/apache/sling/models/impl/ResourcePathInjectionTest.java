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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.impl.injectors.ResourcePathInjector;
import org.apache.sling.models.impl.injectors.SelfInjector;
import org.apache.sling.models.impl.injectors.ValueMapInjector;
import org.apache.sling.models.testmodels.classes.ResourcePathAllOptionalModel;
import org.apache.sling.models.testmodels.classes.ResourcePathModel;
import org.apache.sling.models.testmodels.classes.ResourcePathModelWrapping;
import org.apache.sling.models.testmodels.classes.ResourcePathPartialModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ResourcePathInjectionTest {
    private ModelAdapterFactory factory;

    @Mock
    private Resource adaptableResource;

    @Mock
    SlingHttpServletRequest adaptableRequest;

    @Mock
    private Resource byPathResource;

    @Mock
    private Resource byPathResource2;

    @Mock
    private Resource byPropertyValueResource;

    @Mock
    private Resource byPropertyValueResource2;

    @Mock
    private ResourceResolver resourceResolver;

    @Before
    public void setup() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("propertyContainingAPath", "/some/other/path");
        map.put("anotherPropertyContainingAPath", "/some/other/path2");
        String[] paths = new String[2];
        paths[0] = "/some/other/path";
        paths[1] = "/some/other/path2";

        String[] invalidPaths = new String[3];
        invalidPaths[0] = "/does/not/exist";
        invalidPaths[1] = "/does/not/exist2";
        invalidPaths[2] = "/some/other/path";
        map.put("propertyWithSeveralPaths", paths);
        map.put("propertyWithMissingPaths", invalidPaths);

        ValueMap properties = new ValueMapDecorator(map);

        when(adaptableResource.getResourceResolver()).thenReturn(resourceResolver);
        when(adaptableResource.adaptTo(ValueMap.class)).thenReturn(properties);

        when(resourceResolver.getResource("/some/path")).thenReturn(byPathResource);
        when(resourceResolver.getResource("/some/path2")).thenReturn(byPathResource2);
        when(resourceResolver.getResource("/some/other/path")).thenReturn(byPropertyValueResource);
        when(resourceResolver.getResource("/some/other/path2")).thenReturn(byPropertyValueResource2);

        when(adaptableRequest.getResource()).thenReturn(byPathResource);
        when(adaptableRequest.getResourceResolver()).thenReturn(resourceResolver);

        factory = AdapterFactoryTest.createModelAdapterFactory();
        factory.injectors = Arrays.asList(new SelfInjector(), new ValueMapInjector(), new ResourcePathInjector());
        factory.bindStaticInjectAnnotationProcessorFactory(
                new ResourcePathInjector(), new ServicePropertiesMap(3, 2500));
        factory.adapterImplementations.addClassesAsAdapterAndImplementation(
                ResourcePathModel.class,
                ResourcePathPartialModel.class,
                ResourcePathAllOptionalModel.class,
                ResourcePathModelWrapping.class);
    }

    @Test
    public void testPathInjectionFromResource() {
        ResourcePathModel model = factory.getAdapter(adaptableResource, ResourcePathModel.class);
        assertNotNull(model);
        assertEquals(byPathResource, model.getFromPath());
        assertEquals(byPropertyValueResource, model.getByDerefProperty());
        assertEquals(byPathResource2, model.getFromPath2());
        assertEquals(byPropertyValueResource2, model.getByDerefProperty2());
    }

    @Test
    public void testPathInjectionFromRequest() {
        // return the same properties through this request's resource, as through adaptableResource
        doReturn(adaptableResource.adaptTo(ValueMap.class)).when(byPathResource).adaptTo(ValueMap.class);
        ResourcePathModel model = factory.getAdapter(adaptableRequest, ResourcePathModel.class);
        assertNotNull(model);
        assertEquals(byPathResource, model.getFromPath());
        assertEquals(byPropertyValueResource, model.getByDerefProperty());
        assertEquals(byPathResource2, model.getFromPath2());
        assertEquals(byPropertyValueResource2, model.getByDerefProperty2());
    }

    @Test
    public void testOptionalPathInjectionWithNonResourceAdaptable() {
        ResourcePathAllOptionalModel model = factory.getAdapter(adaptableRequest, ResourcePathAllOptionalModel.class);
        // should not be null because resource paths fields are optional
        assertNotNull(model);
        // but the field itself are null
        assertNull(model.getFromPath());
        assertNull(model.getByDerefProperty());
        assertNull(model.getFromPath2());
        assertNull(model.getByDerefProperty2());
    }

    @Test
    public void testMultiplePathInjection() {
        ResourcePathModel model = factory.getAdapter(adaptableResource, ResourcePathModel.class);
        assertNotNull(model);
        List<Resource> resources = model.getMultipleResources();
        assertNotNull(resources);
        assertEquals(2, resources.size());
        assertEquals(byPropertyValueResource, resources.get(0));
        assertEquals(byPropertyValueResource2, resources.get(1));
        List<Resource> resourcesFromPathAnnotation = model.getManyFromPath();
        assertNotNull(resourcesFromPathAnnotation);
        assertEquals(byPathResource, resourcesFromPathAnnotation.get(0));
        assertEquals(byPathResource2, resourcesFromPathAnnotation.get(1));

        List<Resource> resourcesFromResourcePathAnnotation = model.getManyFromPath2();
        assertNotNull(resourcesFromResourcePathAnnotation);
        assertEquals(byPathResource2, resourcesFromResourcePathAnnotation.get(0));
        assertEquals(byPathResource, resourcesFromResourcePathAnnotation.get(1));

        assertNotNull(model.getPropertyWithSeveralPaths());
        assertEquals(
                byPropertyValueResource, model.getPropertyWithSeveralPaths().get(0));
        assertEquals(
                byPropertyValueResource2, model.getPropertyWithSeveralPaths().get(1));
    }

    @Test
    public void testPartialInjectionFailure1() {
        when(resourceResolver.getResource("/some/other/path")).thenReturn(null);

        ResourcePathPartialModel model = factory.getAdapter(adaptableResource, ResourcePathPartialModel.class);
        assertNull(model);
    }

    @Test
    public void testPartialInjectionFailure2() {
        lenient().when(resourceResolver.getResource("/some/other/path")).thenReturn(null);
        lenient().when(resourceResolver.getResource("/some/other/path2")).thenReturn(null);

        ResourcePathPartialModel model = factory.getAdapter(adaptableResource, ResourcePathPartialModel.class);
        assertNull(model);
    }

    @Test
    public void TestWithArrayWrapping() {
        ResourcePathModelWrapping model = factory.getAdapter(adaptableResource, ResourcePathModelWrapping.class);
        assertNotNull(model);
        assertTrue(model.getFromPath().length > 0);
        assertTrue(model.getMultipleResources().length > 0);
    }
}
