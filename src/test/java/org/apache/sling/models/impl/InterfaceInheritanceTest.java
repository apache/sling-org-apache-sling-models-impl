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
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.impl.injectors.ValueMapInjector;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessorFactory;
import org.apache.sling.models.testmodels.interfaces.SubClassModel;
import org.apache.sling.models.testmodels.interfaces.SuperClassModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("deprecation")
@RunWith(MockitoJUnitRunner.class)
public class InterfaceInheritanceTest {
    private ModelAdapterFactory factory;

    @Before
    public void setup() {

        factory = AdapterFactoryTest.createModelAdapterFactory();
        ValueMapInjector valueMapInjector = new ValueMapInjector();
        factory.injectors = Collections.singletonList(valueMapInjector);

        factory.injectAnnotationProcessorFactories =
                Collections.<InjectAnnotationProcessorFactory>singletonList(valueMapInjector);
        factory.adapterImplementations.addClassesAsAdapterAndImplementation(SuperClassModel.class, SubClassModel.class);
    }

    @Test
    public void testSimplePropertyModel() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("superClassString", "first-value");
        map.put("subClassString", "second-value");
        ValueMap vm = new ValueMapDecorator(map);

        Resource res = mock(Resource.class);
        when(res.adaptTo(ValueMap.class)).thenReturn(vm);

        SubClassModel model = factory.getAdapter(res, SubClassModel.class);
        assertNotNull(model);
        assertEquals("first-value", model.getSuperClassString());
        assertEquals("second-value", model.getSubClassString());
    }
}
