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

import static org.junit.Assert.assertNotNull;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.impl.injectors.RequestAttributeInjector;
import org.apache.sling.models.impl.injectors.SelfInjector;
import org.apache.sling.models.testmodels.classes.constructorvisibility.PackagePrivateConstructorModel;
import org.apache.sling.models.testmodels.classes.constructorvisibility.PrivateConstructorModel;
import org.apache.sling.models.testmodels.classes.constructorvisibility.ProtectedConstructorModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ConstructorVisibilityTest {
    private ModelAdapterFactory factory;

    @Mock
    private SlingHttpServletRequest request;

    @Before
    public void setup() {
        factory = AdapterFactoryTest.createModelAdapterFactory();
        factory.bindInjector(new RequestAttributeInjector(), new ServicePropertiesMap(1, 1));
        factory.bindInjector(new SelfInjector(), new ServicePropertiesMap(2, 2));
        factory.adapterImplementations.addClassesAsAdapterAndImplementation(ProtectedConstructorModel.class, PackagePrivateConstructorModel.class, PrivateConstructorModel.class);
    }

    @Test
    public void testNonPublicConstructorProtectedModel() {
        ProtectedConstructorModel model = factory.createModel(request, ProtectedConstructorModel.class);
        assertNotNull(model);
    }
    
    @Test
    public void testNonPublicConstructorPackagePrivateModel() {
        PackagePrivateConstructorModel model = factory.createModel(request, PackagePrivateConstructorModel.class);
        assertNotNull(model);
    }
    
    @Test
    public void testNonPublicConstructorPrivateModel() {
        PrivateConstructorModel model = factory.createModel(request, PrivateConstructorModel.class);
        assertNotNull(model);
    }
}
