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

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.models.impl.injectors.SelfInjector;
import org.apache.sling.models.testmodels.classes.DirectCyclicSelfDependencyModel;
import org.apache.sling.models.testmodels.classes.IndirectCyclicSelfDependencyModelA;
import org.apache.sling.models.testmodels.classes.IndirectCyclicSelfDependencyModelB;
import org.apache.sling.models.testmodels.classes.SelfDependencyModelA;
import org.apache.sling.models.testmodels.classes.SelfDependencyModelB;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class SelfDependencyTest {

    private ModelAdapterFactory factory;

    @Mock
    private SlingJakartaHttpServletRequest request;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setup() {
        lenient().when(request.adaptTo(any(Class.class))).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Class<?> clazz = (Class<?>) invocation.getArguments()[0];
                return factory.getAdapter(request, clazz);
            }
        });

        factory = AdapterFactoryTest.createModelAdapterFactory();
        factory.injectors = Collections.singletonList(new SelfInjector());
        factory.adapterImplementations.addClassesAsAdapterAndImplementation(
                SelfDependencyModelA.class,
                SelfDependencyModelB.class,
                DirectCyclicSelfDependencyModel.class,
                IndirectCyclicSelfDependencyModelA.class,
                IndirectCyclicSelfDependencyModelB.class);
    }

    @Test
    void testChainedSelfDependency() {
        SelfDependencyModelA objectA = factory.getAdapter(request, SelfDependencyModelA.class);
        assertNotNull(objectA);
        SelfDependencyModelB objectB = objectA.getDependencyB();
        assertNotNull(objectB);
        assertSame(request, objectB.getRequest());
    }

    @Test
    void testDirectCyclicSelfDependency() {
        DirectCyclicSelfDependencyModel object = factory.getAdapter(request, DirectCyclicSelfDependencyModel.class);
        assertNull(object);
    }

    @Test
    void testInddirectCyclicSelfDependency() {
        IndirectCyclicSelfDependencyModelA object =
                factory.getAdapter(request, IndirectCyclicSelfDependencyModelA.class);
        assertNull(object);
    }
}
