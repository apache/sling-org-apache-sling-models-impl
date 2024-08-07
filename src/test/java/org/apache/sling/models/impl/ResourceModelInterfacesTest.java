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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.impl.injectors.ChildResourceInjector;
import org.apache.sling.models.impl.injectors.ValueMapInjector;
import org.apache.sling.models.testmodels.classes.ResourceModelWithRequiredField;
import org.apache.sling.models.testmodels.interfaces.ChildModel;
import org.apache.sling.models.testmodels.interfaces.ChildResourceModel;
import org.apache.sling.models.testmodels.interfaces.ChildValueMapModel;
import org.apache.sling.models.testmodels.interfaces.ParentModel;
import org.apache.sling.models.testmodels.interfaces.SimplePropertyModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ResourceModelInterfacesTest {

    private ModelAdapterFactory factory;

    @Before
    public void setup() {
        factory = AdapterFactoryTest.createModelAdapterFactory();
        factory.injectors = Arrays.asList(new ChildResourceInjector(), new ValueMapInjector());
        factory.adapterImplementations.addClassesAsAdapterAndImplementation(
                SimplePropertyModel.class,
                ResourceModelWithRequiredField.class,
                ChildResourceModel.class,
                ChildValueMapModel.class,
                ParentModel.class,
                ChildModel.class);
    }

    @Test
    public void testSimplePropertyModel() {
        Map<String, Object> map = new HashMap<>();
        map.put("first", "first-value");
        map.put("third", "third-value");
        map.put("fourth", true);
        ValueMap vm = new ValueMapDecorator(map);

        Resource res = mock(Resource.class);
        when(res.adaptTo(ValueMap.class)).thenReturn(vm);

        SimplePropertyModel model = factory.getAdapter(res, SimplePropertyModel.class);
        assertNotNull(model);
        assertEquals("first-value", model.getFirst());
        assertNull(model.getSecond());
        assertEquals("third-value", model.getThirdProperty());
        assertTrue(model.isFourth());

        verify(res, times(1)).adaptTo(ValueMap.class);
    }

    @Test
    @SuppressWarnings({"unused", "null"})
    public void testRequiredPropertyModel() {
        Map<String, Object> map = new HashMap<>();
        map.put("first", "first-value");
        map.put("third", "third-value");
        ValueMap vm = spy(new ValueMapDecorator(map));

        Resource res = mock(Resource.class);
        when(res.adaptTo(ValueMap.class)).thenReturn(vm);

        ResourceModelWithRequiredField model = factory.getAdapter(res, ResourceModelWithRequiredField.class);
        assertNull(model);

        verify(vm).get("required", String.class);
    }

    @Test
    public void testChildResource() {
        Resource child = mock(Resource.class);

        Resource res = mock(Resource.class);
        when(res.getChild("firstChild")).thenReturn(child);

        ChildResourceModel model = factory.getAdapter(res, ChildResourceModel.class);
        assertNotNull(model);
        assertEquals(child, model.getFirstChild());
    }

    @Test
    public void testChildValueMap() {
        ValueMap map = ValueMapDecorator.EMPTY;

        Resource child = mock(Resource.class);
        when(child.adaptTo(ValueMap.class)).thenReturn(map);

        Resource res = mock(Resource.class);
        when(res.getChild("firstChild")).thenReturn(child);

        ChildValueMapModel model = factory.getAdapter(res, ChildValueMapModel.class);
        assertNotNull(model);
        assertEquals(map, model.getFirstChild());
    }

    @Test
    public void testChildModel() {
        Object value = RandomStringUtils.randomAlphabetic(10);
        Map<String, Object> props = Collections.singletonMap("property", value);
        ValueMap map = new ValueMapDecorator(props);

        final Resource firstChild = mock(Resource.class);
        lenient().when(firstChild.adaptTo(ValueMap.class)).thenReturn(map);
        lenient().when(firstChild.adaptTo(ChildModel.class)).thenAnswer(new AdaptToChildModel());

        Object firstGrandChildValue = RandomStringUtils.randomAlphabetic(10);
        ValueMap firstGrandChildMap = new ValueMapDecorator(Collections.singletonMap("property", firstGrandChildValue));
        Object secondGrandChildValue = RandomStringUtils.randomAlphabetic(10);
        ValueMap secondGrandChildMap =
                new ValueMapDecorator(Collections.singletonMap("property", secondGrandChildValue));

        final Resource firstGrandChild = mock(Resource.class);
        lenient().when(firstGrandChild.adaptTo(ValueMap.class)).thenReturn(firstGrandChildMap);
        lenient().when(firstGrandChild.adaptTo(ChildModel.class)).thenAnswer(new AdaptToChildModel());

        final Resource secondGrandChild = mock(Resource.class);
        lenient().when(secondGrandChild.adaptTo(ValueMap.class)).thenReturn(secondGrandChildMap);
        lenient().when(secondGrandChild.adaptTo(ChildModel.class)).thenAnswer(new AdaptToChildModel());

        Resource secondChild = mock(Resource.class);
        lenient()
                .when(secondChild.listChildren())
                .thenReturn(Arrays.asList(firstGrandChild, secondGrandChild).iterator());

        Resource emptyChild = mock(Resource.class);
        lenient()
                .when(emptyChild.listChildren())
                .thenReturn(Collections.<Resource>emptySet().iterator());

        Resource res = mock(Resource.class);
        lenient().when(res.getChild("firstChild")).thenReturn(firstChild);
        lenient().when(res.getChild("secondChild")).thenReturn(secondChild);
        lenient().when(res.getChild("emptyChild")).thenReturn(emptyChild);

        ParentModel model = factory.getAdapter(res, ParentModel.class);
        assertNotNull(model);

        ChildModel childModel = model.getFirstChild();
        assertNotNull(childModel);
        assertEquals(value, childModel.getProperty());
        assertEquals(2, model.getGrandChildren().size());
        assertEquals(firstGrandChildValue, model.getGrandChildren().get(0).getProperty());
        assertEquals(secondGrandChildValue, model.getGrandChildren().get(1).getProperty());
        assertEquals(0, model.getEmptyGrandChildren().size());
    }

    private class AdaptToChildModel implements Answer<ChildModel> {

        @Override
        public ChildModel answer(InvocationOnMock invocation) throws Throwable {
            return factory.getAdapter(invocation.getMock(), ChildModel.class);
        }
    }
}
