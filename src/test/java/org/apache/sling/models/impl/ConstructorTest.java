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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.factory.ModelClassException;
import org.apache.sling.models.impl.injectors.RequestAttributeInjector;
import org.apache.sling.models.impl.injectors.SelfInjector;
import org.apache.sling.models.impl.via.BeanPropertyViaProvider;
import org.apache.sling.models.testmodels.classes.InvalidConstructorModel;
import org.apache.sling.models.testmodels.classes.SuperclassConstructorModel;
import org.apache.sling.models.testmodels.classes.WithOneConstructorModel;
import org.apache.sling.models.testmodels.classes.WithThreeConstructorsModel;
import org.apache.sling.models.testmodels.classes.WithTwoConstructorsModel;
import org.apache.sling.models.testmodels.classes.constructorinjection.NoNameModel;
import org.apache.sling.models.testmodels.classes.constructorinjection.ViaRequestSuffixModel;
import org.apache.sling.models.testmodels.classes.constructorinjection.WithThreeConstructorsOneInjectModel;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConstructorTest {

    private ModelAdapterFactory factory;

    @Mock
    private SlingJakartaHttpServletRequest request;

    private static final int INT_VALUE = 42;

    private static final String STRING_VALUE = "myValue";

    @BeforeEach
    void setup() {

        lenient().when(request.getAttribute("attribute")).thenReturn(INT_VALUE);
        lenient().when(request.getAttribute("attribute2")).thenReturn(STRING_VALUE);

        factory = AdapterFactoryTest.createModelAdapterFactory();
        factory.injectors = Arrays.asList(new RequestAttributeInjector(), new SelfInjector());
        factory.bindViaProvider(new BeanPropertyViaProvider(), null);
        factory.adapterImplementations.addClassesAsAdapterAndImplementation(
                WithOneConstructorModel.class,
                WithThreeConstructorsModel.class,
                WithTwoConstructorsModel.class,
                SuperclassConstructorModel.class,
                InvalidConstructorModel.class,
                WithThreeConstructorsOneInjectModel.class,
                NoNameModel.class,
                ViaRequestSuffixModel.class);
    }

    @Test
    void testConstructorInjection() {
        WithOneConstructorModel model = factory.getAdapter(request, WithOneConstructorModel.class);
        assertNotNull(model);
        assertEquals(request, model.getRequest());
        assertEquals(INT_VALUE, model.getAttribute());
    }

    @Test
    void testThreeConstructorsInjection() {
        WithThreeConstructorsModel model = factory.getAdapter(request, WithThreeConstructorsModel.class);
        assertNotNull(model);
        assertEquals(request, model.getRequest());
        assertEquals(INT_VALUE, model.getAttribute());
    }

    @Test
    void testTwoConstructorsInjection() {
        WithTwoConstructorsModel model = factory.getAdapter(request, WithTwoConstructorsModel.class);
        assertNotNull(model);
        assertEquals(request, model.getRequest());
        assertEquals(INT_VALUE, model.getAttribute());
    }

    @Test
    void testSuperclassConstructorsInjection() {
        SuperclassConstructorModel model = factory.getAdapter(request, SuperclassConstructorModel.class);
        assertNotNull(model);
        assertEquals(request, model.getRequest());
        assertEquals(INT_VALUE, model.getAttribute());
    }

    @Test
    void testInvalidConstructorInjector() {
        InvalidConstructorModel model = factory.getAdapter(request, InvalidConstructorModel.class);
        assertNull(model);
    }

    @Test
    void testInvalidConstructorInjectorException() {
        assertThrows(ModelClassException.class, () -> factory.createModel(request, InvalidConstructorModel.class));
    }

    /**
     * Test model object with three constructors, and make sure that one with @Inject is picked for instantiation.
     * Test mixing of constructor injection and field injection as well.
     */
    @Test
    void testThreeConstructorsOneInjectInjection() {
        WithThreeConstructorsOneInjectModel model =
                factory.getAdapter(request, WithThreeConstructorsOneInjectModel.class);
        assertNotNull(model);
        assertNull(model.getRequest());
        assertEquals(INT_VALUE, model.getAttribute());
        assertEquals(STRING_VALUE, model.getAttribute2());
    }

    @Test
    void testMultiThreadedConstructorInjection() throws InterruptedException, ExecutionException {

        class ModelCreator implements Callable<String> {
            @Override
            @SuppressWarnings("unused")
            public String call() throws Exception {
                try {
                    WithOneConstructorModel model = factory.getAdapter(request, WithOneConstructorModel.class);
                    if (model == null) {
                        return "Expected model not null";
                    }
                    if (!request.equals(model.getRequest())) {
                        return "Expected same request";
                    }
                    if (INT_VALUE != model.getAttribute()) {
                        return "Expected same value for attribute";
                    }
                } catch (Throwable e) {
                    return "Exception not expected: " + e;
                }
                return null;
            }
        }

        int tries = 10000;
        int threads = 10;

        ExecutorService executorService = Executors.newFixedThreadPool(threads);
        List<ModelCreator> modelCreators = new ArrayList<>(tries);
        for (int i = 0; i < tries; i++) {
            modelCreators.add(new ModelCreator());
        }
        List<Future<String>> futures = executorService.invokeAll(modelCreators);
        executorService.shutdown();

        for (Future<String> f : futures) {
            String res = f.get();
            if (res != null) {
                fail(res);
            }
        }
    }

    @Test
    void testNoNameModel() {
        NoNameModel model = factory.getAdapter(request, NoNameModel.class);
        assertNull(model);
    }

    @Test
    @SuppressWarnings("null")
    void testViaInjectionModel() {
        Resource suffixResource = mock(Resource.class);
        when(suffixResource.getPath()).thenReturn("/the/suffix");

        RequestPathInfo requestPathInfo = mock(RequestPathInfo.class);
        when(requestPathInfo.getSuffixResource()).thenReturn(suffixResource);

        when(request.getRequestPathInfo()).thenReturn(requestPathInfo);

        ViaRequestSuffixModel model = factory.getAdapter(request, ViaRequestSuffixModel.class);
        assertThat(model, Matchers.notNullValue());
        assertThat(model.getSuffix(), Matchers.is("/the/suffix"));
    }
}
