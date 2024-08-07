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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.impl.injectors.BindingsInjector;
import org.apache.sling.models.impl.injectors.ChildResourceInjector;
import org.apache.sling.models.impl.injectors.OSGiServiceInjector;
import org.apache.sling.models.impl.injectors.RequestAttributeInjector;
import org.apache.sling.models.impl.injectors.ValueMapInjector;
import org.apache.sling.models.impl.via.BeanPropertyViaProvider;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessorFactory;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessorFactory2;
import org.apache.sling.models.testmodels.classes.InjectorSpecificAnnotationModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("deprecation")
public class InjectorSpecificAnnotationTest {

    @Mock
    private BundleContext bundleContext;

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private Logger log;

    private ModelAdapterFactory factory;

    private OSGiServiceInjector osgiInjector;

    @Before
    public void setup() {
        factory = AdapterFactoryTest.createModelAdapterFactory();

        osgiInjector = new OSGiServiceInjector();
        osgiInjector.activate(bundleContext);

        BindingsInjector bindingsInjector = new BindingsInjector();
        ValueMapInjector valueMapInjector = new ValueMapInjector();
        ChildResourceInjector childResourceInjector = new ChildResourceInjector();
        RequestAttributeInjector requestAttributeInjector = new RequestAttributeInjector();

        factory.injectors = Arrays.asList(
                bindingsInjector, valueMapInjector, childResourceInjector, requestAttributeInjector, osgiInjector);

        factory.bindStaticInjectAnnotationProcessorFactory(bindingsInjector, new ServicePropertiesMap(1L, 0));
        factory.injectAnnotationProcessorFactories =
                Collections.<InjectAnnotationProcessorFactory>singletonList(valueMapInjector);
        factory.injectAnnotationProcessorFactories2 =
                Collections.<InjectAnnotationProcessorFactory2>singletonList(childResourceInjector);
        factory.bindStaticInjectAnnotationProcessorFactory(requestAttributeInjector, new ServicePropertiesMap(4L, 0));
        factory.bindStaticInjectAnnotationProcessorFactory(osgiInjector, new ServicePropertiesMap(5L, 0));
        factory.bindViaProvider(new BeanPropertyViaProvider(), null);

        SlingBindings bindings = new SlingBindings();
        bindings.setLog(log);
        Mockito.when(request.getAttribute(SlingBindings.class.getName())).thenReturn(bindings);

        factory.adapterImplementations.addClassesAsAdapterAndImplementation(
                InjectorSpecificAnnotationModel.class,
                org.apache.sling.models.testmodels.classes.constructorinjection.InjectorSpecificAnnotationModel.class);
    }

    @Test
    public void testSimpleValueModelField() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("first", "first-value");
        map.put("second", "second-value");
        ValueMap vm = new ValueMapDecorator(map);

        Resource res = mock(Resource.class);
        when(res.adaptTo(ValueMap.class)).thenReturn(vm);
        when(request.getResource()).thenReturn(res);

        InjectorSpecificAnnotationModel model = factory.getAdapter(request, InjectorSpecificAnnotationModel.class);
        assertNotNull("Could not instanciate model", model);
        assertEquals("first-value", model.getFirst());
        assertEquals("second-value", model.getSecond());
    }

    @Test
    public void testOrderForValueAnnotationField() {
        // make sure that that the correct injection is used
        // make sure that log is adapted from value map
        // and not coming from request attribute
        Logger logFromValueMap = LoggerFactory.getLogger(this.getClass());

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("first", "first-value");
        map.put("log", logFromValueMap);
        ValueMap vm = new ValueMapDecorator(map);

        Resource res = mock(Resource.class);
        when(res.adaptTo(ValueMap.class)).thenReturn(vm);
        when(request.getResource()).thenReturn(res);

        InjectorSpecificAnnotationModel model = factory.getAdapter(request, InjectorSpecificAnnotationModel.class);
        assertNotNull("Could not instanciate model", model);
        assertEquals("first-value", model.getFirst());
        assertEquals(logFromValueMap, model.getLog());
    }

    @Test
    @SuppressWarnings({"unchecked", "null"})
    public void testOSGiServiceField() throws InvalidSyntaxException {
        ServiceReference ref = mock(ServiceReference.class);
        Logger log = mock(Logger.class);
        when(bundleContext.getServiceReferences(Logger.class.getName(), null)).thenReturn(new ServiceReference[] {ref});
        when(bundleContext.getService(ref)).thenReturn(log);

        InjectorSpecificAnnotationModel model = factory.getAdapter(request, InjectorSpecificAnnotationModel.class);
        assertNotNull("Could not instanciate model", model);
        assertEquals(log, model.getService());
    }

    @Test
    public void testScriptVariableField() throws InvalidSyntaxException {
        SlingBindings bindings = new SlingBindings();
        SlingScriptHelper helper = mock(SlingScriptHelper.class);
        bindings.setSling(helper);
        when(request.getAttribute(SlingBindings.class.getName())).thenReturn(bindings);

        InjectorSpecificAnnotationModel model = factory.getAdapter(request, InjectorSpecificAnnotationModel.class);
        assertNotNull("Could not instanciate model", model);
        assertEquals(helper, model.getHelper());
    }

    @Test
    public void testRequestAttributeField() throws InvalidSyntaxException {
        Object attribute = new Object();
        when(request.getAttribute("attribute")).thenReturn(attribute);

        InjectorSpecificAnnotationModel model = factory.getAdapter(request, InjectorSpecificAnnotationModel.class);
        assertNotNull("Could not instanciate model", model);
        assertEquals(attribute, model.getRequestAttribute());
    }

    @Test
    public void testChildResourceField() {
        Resource res = mock(Resource.class);
        Resource child = mock(Resource.class);
        when(res.getChild("child1")).thenReturn(child);
        when(request.getResource()).thenReturn(res);

        InjectorSpecificAnnotationModel model = factory.getAdapter(request, InjectorSpecificAnnotationModel.class);
        assertNotNull("Could not instanciate model", model);
        assertEquals(child, model.getChildResource());
    }

    @Test
    public void testSimpleValueModelConstructor() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("first", "first-value");
        map.put("second", "second-value");
        ValueMap vm = new ValueMapDecorator(map);

        Resource res = mock(Resource.class);
        when(res.adaptTo(ValueMap.class)).thenReturn(vm);
        when(request.getResource()).thenReturn(res);

        org.apache.sling.models.testmodels.classes.constructorinjection.InjectorSpecificAnnotationModel model =
                factory.getAdapter(
                        request,
                        org.apache.sling.models.testmodels.classes.constructorinjection.InjectorSpecificAnnotationModel
                                .class);
        assertNotNull("Could not instanciate model", model);
        assertEquals("first-value", model.getFirst());
        assertEquals("second-value", model.getSecond());
    }

    @Test
    public void testOrderForValueAnnotationConstructor() {
        // make sure that that the correct injection is used
        // make sure that log is adapted from value map
        // and not coming from request attribute
        Logger logFromValueMap = LoggerFactory.getLogger(this.getClass());

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("first", "first-value");
        map.put("log", logFromValueMap);
        ValueMap vm = new ValueMapDecorator(map);

        Resource res = mock(Resource.class);
        when(res.adaptTo(ValueMap.class)).thenReturn(vm);
        when(request.getResource()).thenReturn(res);

        org.apache.sling.models.testmodels.classes.constructorinjection.InjectorSpecificAnnotationModel model =
                factory.getAdapter(
                        request,
                        org.apache.sling.models.testmodels.classes.constructorinjection.InjectorSpecificAnnotationModel
                                .class);
        assertNotNull("Could not instanciate model", model);
        assertEquals("first-value", model.getFirst());
        assertEquals(logFromValueMap, model.getLog());
    }

    @Test
    @SuppressWarnings({"unchecked", "null"})
    public void testOSGiServiceConstructor() throws InvalidSyntaxException {
        ServiceReference ref = mock(ServiceReference.class);
        Logger log = mock(Logger.class);
        when(bundleContext.getServiceReferences(Logger.class.getName(), null)).thenReturn(new ServiceReference[] {ref});
        when(bundleContext.getService(ref)).thenReturn(log);

        org.apache.sling.models.testmodels.classes.constructorinjection.InjectorSpecificAnnotationModel model =
                factory.getAdapter(
                        request,
                        org.apache.sling.models.testmodels.classes.constructorinjection.InjectorSpecificAnnotationModel
                                .class);
        assertNotNull("Could not instanciate model", model);
        assertEquals(log, model.getService());
    }

    @Test
    public void testScriptVariableConstructor() throws InvalidSyntaxException {
        SlingBindings bindings = new SlingBindings();
        SlingScriptHelper helper = mock(SlingScriptHelper.class);
        bindings.setSling(helper);
        when(request.getAttribute(SlingBindings.class.getName())).thenReturn(bindings);

        org.apache.sling.models.testmodels.classes.constructorinjection.InjectorSpecificAnnotationModel model =
                factory.getAdapter(
                        request,
                        org.apache.sling.models.testmodels.classes.constructorinjection.InjectorSpecificAnnotationModel
                                .class);
        assertNotNull("Could not instanciate model", model);
        assertEquals(helper, model.getHelper());
    }

    @Test
    public void testRequestAttributeConstructor() throws InvalidSyntaxException {
        Object attribute = new Object();
        when(request.getAttribute("attribute")).thenReturn(attribute);

        org.apache.sling.models.testmodels.classes.constructorinjection.InjectorSpecificAnnotationModel model =
                factory.getAdapter(
                        request,
                        org.apache.sling.models.testmodels.classes.constructorinjection.InjectorSpecificAnnotationModel
                                .class);
        assertNotNull("Could not instanciate model", model);
        assertEquals(attribute, model.getRequestAttribute());
    }

    @Test
    public void testChildResourceConstructor() {
        Resource res = mock(Resource.class);
        Resource child = mock(Resource.class);
        when(res.getChild("child1")).thenReturn(child);
        when(request.getResource()).thenReturn(res);

        org.apache.sling.models.testmodels.classes.constructorinjection.InjectorSpecificAnnotationModel model =
                factory.getAdapter(
                        request,
                        org.apache.sling.models.testmodels.classes.constructorinjection.InjectorSpecificAnnotationModel
                                .class);
        assertNotNull("Could not instanciate model", model);
        assertEquals(child, model.getChildResource());
    }
}
