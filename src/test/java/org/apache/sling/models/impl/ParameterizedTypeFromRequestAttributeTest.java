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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Iterator;

import javax.inject.Inject;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.impl.injectors.RequestAttributeInjector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ParameterizedTypeFromRequestAttributeTest {
    private ModelAdapterFactory factory;

    @Mock
    private SlingHttpServletRequest request;

    @Before
    public void setup() {
        factory = AdapterFactoryTest.createModelAdapterFactory();

        RequestAttributeInjector injector = new RequestAttributeInjector();
        factory.bindInjector(injector, new ServicePropertiesMap(1, 1));
        factory.adapterImplementations.addClassesAsAdapterAndImplementation(TestModel.class);
    }

    @Test
    public void test() {
        Iterator<Resource> it = Collections.<Resource> emptySet().iterator();

        when(request.getAttribute("someResources")).thenReturn(it);
        TestModel model = factory.getAdapter(request, TestModel.class);
        assertNotNull(model);
        assertEquals(it, model.getSomeResources());
    }

    @Model(adaptables = SlingHttpServletRequest.class)
    public static class TestModel {

        @Inject
        private Iterator<Resource> someResources;

        public Iterator<Resource> getSomeResources() {
            return someResources;
        }
    }
}
