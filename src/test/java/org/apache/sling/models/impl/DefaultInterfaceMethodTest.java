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
