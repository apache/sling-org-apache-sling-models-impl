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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.impl.injectors.SelfInjector;
import org.apache.sling.models.testmodels.classes.DirectCyclicSelfDependencyModel;
import org.apache.sling.models.testmodels.classes.IndirectCyclicSelfDependencyModelA;
import org.apache.sling.models.testmodels.classes.IndirectCyclicSelfDependencyModelB;
import org.apache.sling.models.testmodels.classes.SelfDependencyModelA;
import org.apache.sling.models.testmodels.classes.SelfDependencyModelB;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@RunWith(MockitoJUnitRunner.class)
public class SelfDependencyTest {

    private ModelAdapterFactory factory;

    @Mock
    private SlingHttpServletRequest request;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() {
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
    public void testChainedSelfDependency() {
        SelfDependencyModelA objectA = factory.getAdapter(request, SelfDependencyModelA.class);
        assertNotNull(objectA);
        SelfDependencyModelB objectB = objectA.getDependencyB();
        assertNotNull(objectB);
        assertSame(request, objectB.getRequest());
    }

    @Test
    public void testDirectCyclicSelfDependency() {
        DirectCyclicSelfDependencyModel object = factory.getAdapter(request, DirectCyclicSelfDependencyModel.class);
        assertNull(object);
    }

    @Test
    public void testInddirectCyclicSelfDependency() {
        IndirectCyclicSelfDependencyModelA object =
                factory.getAdapter(request, IndirectCyclicSelfDependencyModelA.class);
        assertNull(object);
    }
}
