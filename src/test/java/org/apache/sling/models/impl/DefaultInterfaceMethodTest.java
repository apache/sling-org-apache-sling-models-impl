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

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.impl.injectors.ValueMapInjector;
import org.apache.sling.models.testmodels.interfaces.ModelWithDefaultMethods;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

public class DefaultInterfaceMethodTest {

    private ModelAdapterFactory factory;

    @Before
    public void setup() {
        factory = AdapterFactoryTest.createModelAdapterFactory();
        factory.bindInjector(new ValueMapInjector(), new ServicePropertiesMap(0, 0));
        factory.adapterImplementations.addClassesAsAdapterAndImplementation(ModelWithDefaultMethods.class);
    }

    @Test
    public void testDefaultInterfaceMethodsCanBeInjected() {
        ValueMap vm = new ValueMapDecorator(Collections.singletonMap("prop", "the prop"));
        Resource res = mock(Resource.class);
        lenient().when(res.adaptTo(ValueMap.class)).thenReturn(vm);

        ModelWithDefaultMethods model = factory.getAdapter(res,ModelWithDefaultMethods.class);

        assertEquals("the prop", model.getProp());
    }

    @Test
    public void testDefaultInterfaceMethodsDefaultImplementationsAreIgnored() {
        ValueMap vm = new ValueMapDecorator(Collections.<String, Object>emptyMap());
        Resource res = mock(Resource.class);
        lenient().when(res.adaptTo(ValueMap.class)).thenReturn(vm);

        ModelWithDefaultMethods model = factory.getAdapter(res,ModelWithDefaultMethods.class);

        assertNull(model.getProp());
    }
}
