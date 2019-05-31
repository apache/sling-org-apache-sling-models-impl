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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.Required;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.apache.sling.models.factory.MissingElementException;
import org.apache.sling.models.factory.MissingElementsException;
import org.apache.sling.models.impl.injectors.ValueMapInjector;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessorFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SuppressWarnings("deprecation")
@RunWith(MockitoJUnitRunner.class)
public class AnnotationConflictsTest {

    private ModelAdapterFactory factory;

    @Mock
    private Resource resource;

    @Before
    public void setup() {
        factory = AdapterFactoryTest.createModelAdapterFactory();
        ValueMapInjector injector = new ValueMapInjector();
        factory.bindInjector(injector, new ServicePropertiesMap(1, 1));
        factory.injectAnnotationProcessorFactories = Collections.<InjectAnnotationProcessorFactory>singletonList(new ValueMapInjector());

        for (Class<?> clazz : this.getClass().getDeclaredClasses()) {
            if (!clazz.isInterface()) {
                factory.adapterImplementations.addClassesAsAdapterAndImplementation(clazz);
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSucessfulAdaptations() {
        for (Class<?> clazz : this.getClass().getDeclaredClasses()) {
            if (!clazz.isInterface() && clazz.getSimpleName().startsWith("Successful")) {
                successful((Class<Methods>) clazz);
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFailingAdaptations() {
        for (Class<?> clazz : this.getClass().getDeclaredClasses()) {
            if (!clazz.isInterface() && clazz.getSimpleName().startsWith("Failing")) {
                failing((Class<Methods>) clazz);
            }
        }
    }

    // @Optional overrides default optional=false from annotation
    @Model(adaptables = Resource.class)
    public static class SuccessfulSingleOptionalBySeparateAnnotationFieldModel implements Methods {

        @ValueMapValue
        private String otherText;

        @Override
        public String getOtherText() {
            return otherText;
        }

        @ValueMapValue
        @Optional
        private String emptyText;

        @Override
        public String getEmptyText() {
            return emptyText;
        }
    }

    // optional=true attribute still works with no @Optional annotation
    @Model(adaptables = Resource.class)
    public static class SuccessfulSingleOptionalViaAttributeFieldModel implements Methods {

        @ValueMapValue
        private String otherText;

        @Override
        public String getOtherText() {
            return otherText;
        }

        @ValueMapValue(optional = true)
        private String emptyText;

        @Override
        public String getEmptyText() {
            return emptyText;
        }
    }

    // strategy still work with no @Optional attribute
    @Model(adaptables = Resource.class)
    public static class SuccessfulSingleOptionalByStrategyFieldModel implements Methods {

        @ValueMapValue
        private String otherText;

        @Override
        public String getOtherText() {
            return otherText;
        }

        @ValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
        private String emptyText;

        @Override
        public String getEmptyText() {
            return emptyText;
        }
    }

    // @Required overrides class-level strategy
    @Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
    public static class FailingSingleRequiredByAnnotationFieldModel implements Methods {

        @ValueMapValue
        private String otherText;

        @Override
        public String getOtherText() {
            return otherText;
        }

        @ValueMapValue
        @Required
        private String emptyText;

        @Override
        public String getEmptyText() {
            return emptyText;
        }
    }

    // REQUIRED strategy overrides class-level strategy
    @Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
    public static class FailingSingleRequiredByStrategyFieldModel implements Methods {

        @ValueMapValue
        private String otherText;

        @Override
        public String getOtherText() {
            return otherText;
        }

        @ValueMapValue(injectionStrategy = InjectionStrategy.REQUIRED)
        private String emptyText;

        @Override
        public String getEmptyText() {
            return emptyText;
        }
    }

    // optional=true overrides @Required annotation
    @Model(adaptables = Resource.class)
    public static class SuccessfulSingleRequiredBySeparateAnnotationOverridingAttributeFieldModel implements Methods {

        @ValueMapValue
        private String otherText;

        @Override
        public String getOtherText() {
            return otherText;
        }

        @ValueMapValue(optional = true)
        @Required
        private String emptyText;

        @Override
        public String getEmptyText() {
            return emptyText;
        }
    }

    // @Required overrides OPTIONAL strategy
    @Model(adaptables = Resource.class)
    public static class FailingSingleRequiredBySeparateAnnotationOverridingStrategyFieldModel implements Methods {

        @ValueMapValue
        private String otherText;

        @Override
        public String getOtherText() {
            return otherText;
        }

        @ValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
        @Required
        private String emptyText;

        @Override
        public String getEmptyText() {
            return emptyText;
        }
    }

    private interface Methods {
        String getOtherText();
        String getEmptyText();
    }

    private <T extends Methods> void successful(Class<T> modelClass) {
        ValueMap map = new ValueMapDecorator(Collections.<String, Object>singletonMap("otherText", "hello"));
        when(resource.adaptTo(ValueMap.class)).thenReturn(map);

        Methods model = factory.createModel(resource, modelClass);
        assertNotNull("Adaptation to " + modelClass.getSimpleName() + " was not null.", model);
        assertNull("Adaptation to " + modelClass.getSimpleName() + " had a non-null emptyText value.", model.getEmptyText());
        assertEquals("Adaptation to " + modelClass.getSimpleName() + " had an unexpected value in the otherText value.", "hello", model.getOtherText());
    }

    private <T extends Methods> void failing(Class<T> modelClass) {
        ValueMap map = new ValueMapDecorator(Collections.<String, Object>singletonMap("otherText", "hello"));
        when(resource.adaptTo(ValueMap.class)).thenReturn(map);

        boolean thrown = false;

        try {
            factory.createModel(resource, modelClass);
        } catch (MissingElementsException e) {
            assertEquals("Adaptation to " + modelClass.getSimpleName() + " failed, but with the wrong number of exceptions.",1, e.getMissingElements().size());
            MissingElementException me = e.getMissingElements().iterator().next();
            assertTrue("Adaptation to " + modelClass.getSimpleName() + " didn't fail due to emptyText.", me.getElement().toString().endsWith("emptyText"));
            thrown = true;
        }
        assertTrue("Adaptation to " + modelClass.getSimpleName() + " was successful.", thrown);
    }
}
