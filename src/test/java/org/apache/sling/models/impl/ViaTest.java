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

import java.util.Collections;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.impl.injectors.ValueMapInjector;
import org.apache.sling.models.impl.via.BeanPropertyViaProvider;
import org.apache.sling.models.impl.via.ChildResourceViaProvider;
import org.apache.sling.models.testmodels.classes.ChildResourceViaModel;
import org.apache.sling.models.testmodels.classes.ViaModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ViaTest {

    @Mock
    private Resource resource;

    @Mock
    private Resource childResource;

    @Mock
    private SlingHttpServletRequest request;

    private ModelAdapterFactory factory;

    @Before
    public void setup() {
        when(request.getResource()).thenReturn(resource);
        when(resource.getChild("jcr:content")).thenReturn(childResource);
        factory = AdapterFactoryTest.createModelAdapterFactory();
        factory.injectors = Collections.singletonList(new ValueMapInjector());
        factory.bindViaProvider(new BeanPropertyViaProvider(), null);
        factory.bindViaProvider(new ChildResourceViaProvider(), null);
        factory.adapterImplementations.addClassesAsAdapterAndImplementation(ViaModel.class);
        factory.adapterImplementations.addClassesAsAdapterAndImplementation(ChildResourceViaModel.class);
    }

    @Test
    public void testProjectionToResource() {
        String value = RandomStringUtils.randomAlphanumeric(10);
        ValueMap map = new ValueMapDecorator(Collections.<String, Object>singletonMap("firstProperty", value));
        when(resource.adaptTo(ValueMap.class)).thenReturn(map);

        ViaModel model = factory.getAdapter(request, ViaModel.class);
        assertNotNull(model);
        assertEquals(value, model.getFirstProperty());
    }

    @Test
    public void testProjectionToChildResource() {
        String value = RandomStringUtils.randomAlphanumeric(10);
        ValueMap map = new ValueMapDecorator(Collections.<String, Object>singletonMap("firstProperty", value));
        when(childResource.adaptTo(ValueMap.class)).thenReturn(map);
        ChildResourceViaModel model = factory.getAdapter(resource, ChildResourceViaModel.class);
        assertNotNull(model);
        assertEquals(value, model.getFirstProperty());
    }
}
