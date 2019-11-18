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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.impl.injectors.ValueMapInjector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OptionalObjectsTest {

    private ModelAdapterFactory factory;

    @Before
    public void setup() {
        factory = AdapterFactoryTest.createModelAdapterFactory();
        factory.bindInjector(new ValueMapInjector(), new ServicePropertiesMap(2, 2));
        factory.adapterImplementations.addClassesAsAdapterAndImplementation(
                org.apache.sling.models.testmodels.classes.OptionalObjectsModel.class);
    }

    @Test
    public void testFieldInjectionClass() {
        Map<String, Object> map = new HashMap<>();
        map.put("optionalString", "foo bar baz");
        map.put("optionalByte", Byte.valueOf("1"));
        map.put("optionalInteger", Integer.valueOf("1"));
        map.put("optionalShort", Short.valueOf("1"));
        map.put("optionalLong", Long.valueOf("1"));
        map.put("optionalShort", Short.valueOf("1"));
        map.put("optionalDouble", Double.valueOf("1"));
        map.put("optionalFloat", Float.valueOf("1"));
        map.put("optionalChar", '1');
        map.put("optionalBoolean", Boolean.valueOf("true"));
        map.put("optionalList", Arrays.asList("foo", "bar", "baz"));
        map.put("optional2List", Arrays.asList("foo1", "bar1", "baz1"));
        map.put("optionalArray", new String[]{"qux", "quux"});

        Resource res = mock(Resource.class);
        when(res.adaptTo(ValueMap.class)).thenReturn(new ValueMapDecorator(map));


        org.apache.sling.models.testmodels.classes.OptionalObjectsModel model = factory.getAdapter(res,
                org.apache.sling.models.testmodels.classes.OptionalObjectsModel.class);
        assertNotNull(model);

        /*
        assertEquals(Optional.of(Arrays.asList("foo", "bar", "baz")), model.getOptionalList());
        assertEquals(Arrays.asList("foo", "bar", "baz"), model.getOptional2List());
        */

    }
}
