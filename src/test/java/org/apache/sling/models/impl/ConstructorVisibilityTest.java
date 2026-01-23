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

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.models.impl.injectors.RequestAttributeInjector;
import org.apache.sling.models.impl.injectors.SelfInjector;
import org.apache.sling.models.testmodels.classes.constructorvisibility.PackagePrivateConstructorModel;
import org.apache.sling.models.testmodels.classes.constructorvisibility.PrivateConstructorModel;
import org.apache.sling.models.testmodels.classes.constructorvisibility.ProtectedConstructorModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class ConstructorVisibilityTest {
    private ModelAdapterFactory factory;

    @Mock
    private SlingJakartaHttpServletRequest request;

    @BeforeEach
    void setup() {
        factory = AdapterFactoryTest.createModelAdapterFactory();
        factory.injectors = Arrays.asList(new RequestAttributeInjector(), new SelfInjector());
        factory.adapterImplementations.addClassesAsAdapterAndImplementation(
                ProtectedConstructorModel.class, PackagePrivateConstructorModel.class, PrivateConstructorModel.class);
    }

    @Test
    void testNonPublicConstructorProtectedModel() {
        ProtectedConstructorModel model = factory.createModel(request, ProtectedConstructorModel.class);
        assertNotNull(model);
    }

    @Test
    void testNonPublicConstructorPackagePrivateModel() {
        PackagePrivateConstructorModel model = factory.createModel(request, PackagePrivateConstructorModel.class);
        assertNotNull(model);
    }

    @Test
    void testNonPublicConstructorPrivateModel() {
        PrivateConstructorModel model = factory.createModel(request, PrivateConstructorModel.class);
        assertNotNull(model);
    }
}
