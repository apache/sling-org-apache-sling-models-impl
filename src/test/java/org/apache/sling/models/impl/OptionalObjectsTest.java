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

import java.util.*;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.impl.injectors.*;
import org.apache.sling.models.impl.via.BeanPropertyViaProvider;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessorFactory;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessorFactory2;
import org.apache.sling.models.testmodels.classes.OptionalObjectsModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OptionalObjectsTest {

    private ModelAdapterFactory factory;

    private OSGiServiceInjector osgiInjector;

    @Mock
    private BundleContext bundleContext;

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private Logger log;

    @Before
    public void setup() {
        factory = AdapterFactoryTest.createModelAdapterFactory();

        osgiInjector = new OSGiServiceInjector();
        osgiInjector.activate(bundleContext);

        BindingsInjector bindingsInjector = new BindingsInjector();
        ValueMapInjector valueMapInjector = new ValueMapInjector();
        ChildResourceInjector childResourceInjector = new ChildResourceInjector();
        RequestAttributeInjector requestAttributeInjector = new RequestAttributeInjector();

        factory.bindInjector(bindingsInjector,
                Collections.<String, Object>singletonMap(Constants.SERVICE_ID, 1L));
        factory.bindInjector(valueMapInjector,
                Collections.<String, Object>singletonMap(Constants.SERVICE_ID, 2L));
        factory.bindInjector(childResourceInjector,
                Collections.<String, Object>singletonMap(Constants.SERVICE_ID, 3L));
        factory.bindInjector(requestAttributeInjector,
                Collections.<String, Object>singletonMap(Constants.SERVICE_ID, 4L));
        factory.bindInjector(osgiInjector, Collections.<String, Object>singletonMap(Constants.SERVICE_ID, 5L));

        factory.bindStaticInjectAnnotationProcessorFactory(bindingsInjector,
                Collections.<String, Object>singletonMap(Constants.SERVICE_ID, 1L));
        factory.injectAnnotationProcessorFactories = Collections.<InjectAnnotationProcessorFactory>singletonList(valueMapInjector);
        factory.injectAnnotationProcessorFactories2 = Collections.<InjectAnnotationProcessorFactory2>singletonList(childResourceInjector);
        factory.bindStaticInjectAnnotationProcessorFactory(requestAttributeInjector,
                Collections.<String, Object>singletonMap(Constants.SERVICE_ID, 4L));
        factory.bindStaticInjectAnnotationProcessorFactory(osgiInjector,
                Collections.<String, Object>singletonMap(Constants.SERVICE_ID, 5L));
        factory.bindViaProvider(new BeanPropertyViaProvider(), null);

        SlingBindings bindings = new SlingBindings();
        bindings.setLog(log);
        Mockito.when(request.getAttribute(SlingBindings.class.getName())).thenReturn(bindings);

        factory.adapterImplementations.addClassesAsAdapterAndImplementation(OptionalObjectsModel.class);

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

        Resource res = mock(Resource.class);
        when(res.adaptTo(ValueMap.class)).thenReturn(new ValueMapDecorator(map));

        org.apache.sling.models.testmodels.classes.OptionalObjectsModel model = factory.getAdapter(res,
                OptionalObjectsModel.class);
        assertNotNull(model);

        assertEquals(Optional.of("foo bar baz"), model.getOptionalString());
        assertEquals(Optional.empty(), model.getOptionalNullString());

        assertEquals(Optional.of(1), model.getOptionalInteger());
        assertEquals(Optional.empty(), model.getOptionalNullInteger());

        assertEquals(Optional.of(Byte.valueOf("1")), model.getOptionalByte());
        assertEquals(Optional.empty(), model.getOptionalNullByte());

        assertEquals(Optional.of(Long.valueOf("1")), model.getOptionalLong());
        assertEquals(Optional.empty(), model.getOptionalNullLong());

        assertEquals(Optional.of(Short.valueOf("1")), model.getOptionalShort());
        assertEquals(Optional.empty(), model.getOptionalNullShort());

        assertEquals(Optional.of(Double.valueOf("1")), model.getOptionalDouble());
        assertEquals(Optional.empty(), model.getOptionalNullDouble());

        assertEquals(Optional.of(Float.valueOf("1")), model.getOptionalFloat());
        assertEquals(Optional.empty(), model.getOptionalNullFloat());

        assertEquals(Optional.of(1L), model.getOptionalLong());
        assertEquals(Optional.empty(), model.getOptionalNullLong());

        assertEquals(Optional.of('1'), model.getOptionalChar());
        assertEquals(Optional.empty(), model.getOptionalNullChar());

        assertEquals(Optional.of(Boolean.valueOf("true")), model.getOptionalBoolean());
        assertEquals(Optional.empty(), model.getOptionalNullBoolean());
    }

    @Test
    public void testFieldInjectionListsAndArrays() {
        Map<String, Object> map = new HashMap<>();

        map.put("intList", new Integer[]{1, 2, 9, 8});
        map.put("stringList", new String[]{"hello", "world"});
        map.put("optionalList", Arrays.asList("foo", "bar", "baz"));
        map.put("optionalArray", new String[]{"qux", "quux"});

        ValueMap vm = new ValueMapDecorator(map);
        Resource res = mock(Resource.class);
        when(res.adaptTo(ValueMap.class)).thenReturn(vm);

        OptionalObjectsModel model = factory.getAdapter(res, OptionalObjectsModel.class);
        assertNotNull(model);

        assertEquals(4, model.getIntList().get().size());
        assertEquals(new Integer(2), model.getIntList().get().get(1));

        assertEquals(2, model.getStringList().get().size());
        assertEquals("hello", model.getStringList().get().get(0));

        assertEquals(Optional.of(Arrays.asList("foo", "bar", "baz")), model.getOptionalList());
        assertEquals(Optional.empty(), model.getOptionalNullList());

        assertTrue(model.getOptionalArray().isPresent());
        assertTrue("qux".equalsIgnoreCase(model.getOptionalArray().get()[0]));
        assertTrue("quux".equalsIgnoreCase(model.getOptionalArray().get()[1]));
        assertEquals(Optional.empty(), model.getOptionalNullArray());
    }

    @Test
    public void testFieldInjectionsChildResource() throws Exception {
        Resource res = mock(Resource.class);
        Resource child = mock(Resource.class);
        when(child.getName()).thenReturn("child");
        when(res.getChild(eq("child"))).thenReturn(child);

        OptionalObjectsModel model = factory.getAdapter(res, OptionalObjectsModel.class);
        assertNotNull(model);

        assertEquals(child.getName(), model.getOptionalChild().get().getName());
        assertEquals(Optional.empty(), model.getOptionalNullChild());
    }

    @Test
    public void testFieldInjectionsScriptVariable() throws Exception {
        SlingBindings bindings = new SlingBindings();
        SlingScriptHelper helper = mock(SlingScriptHelper.class);
        bindings.setSling(helper);
        when(request.getAttribute(SlingBindings.class.getName())).thenReturn(bindings);

        OptionalObjectsModel model = factory.getAdapter(request, OptionalObjectsModel.class);
        assertNotNull(model);
        assertEquals(helper, model.getOptionalHelper().get());
        assertEquals(Optional.empty(), model.getOptionalNullHelper());
    }

    @Test
    public void testFieldInjectionsOSGiService() throws InvalidSyntaxException {
        ServiceReference ref = mock(ServiceReference.class);
        Logger log = mock(Logger.class);
        when(bundleContext.getServiceReference(Logger.class.getName())).thenReturn(ref);
        when(bundleContext.getService(ref)).thenReturn(log);

        OptionalObjectsModel model = factory.getAdapter(request, OptionalObjectsModel.class);
        assertNotNull(model);
        assertEquals(log, model.getLog().get());
    }

    @Test
    public void testFieldInjectionsRequestAttribute() throws InvalidSyntaxException {
        Object attribute = new Object();
        when(request.getAttribute("attribute")).thenReturn(attribute);

        OptionalObjectsModel model = factory.getAdapter(request, OptionalObjectsModel.class);
        assertNotNull(model);
        assertEquals(attribute, model.getOptionalRequestAttribute().get());
        assertEquals(Optional.empty(), model.getOptionalNullRequestAttribute());
    }
}
