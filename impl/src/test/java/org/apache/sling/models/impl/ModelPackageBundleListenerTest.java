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
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.models.testmodels.classes.ChildModel;
import org.apache.sling.models.testmodels.classes.SimpleModelWithInvalidSecondAnnotation;
import org.apache.sling.models.testmodels.classes.annotations.Hidden;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.springframework.core.OverridingClassLoader;

@RunWith(MockitoJUnitRunner.class)
public class ModelPackageBundleListenerTest {

    @Mock
    private BundleContext mockBundleContext;

    @Mock
    private ModelAdapterFactory mockModelAdapterFactory;

    @Mock
    private Bundle mockBundle;

    final AdapterImplementations adapterImplementations = new AdapterImplementations();

    /**
     * ClassLoader which doesn't delegate to the parent. In addition it blocks loading certain classes.
     */
    private static final class HideClassesClassLoader extends OverridingClassLoader {

        private final Collection<String> classNamesToHide;

        public HideClassesClassLoader(ClassLoader parent, String... classNamesToHide) {
            super(parent);
            this.classNamesToHide = Arrays.asList(classNamesToHide);
            // Exclude Hidden class since its loading via default class loader and throws ClassNotFoundException in
            // line:70
            this.excludeClass(Hidden.class.getName());
        }

        @Override
        protected Class<?> loadClassForOverriding(String name) throws ClassNotFoundException {
            if (classNamesToHide.contains(name)) {
                throw new ClassNotFoundException(
                        "Could not find class " + name + " as it is hidden by this class loader!");
            }
            return super.loadClassForOverriding(name);
        }
    }

    @Test
    public void testAddingBundleWithResolvableModelAnnotation() throws ClassNotFoundException {
        Assert.assertFalse(
                "Model should not yet have been registered but was",
                adapterImplementations.isModelClass(ChildModel.class));
        ModelPackageBundleListener listener = createListenerForBundleWithClass(ChildModel.class);
        listener.addingBundle(mockBundle, new BundleEvent(BundleEvent.STARTED, mockBundle));
        Assert.assertTrue(
                "Model should have been registered but was not", adapterImplementations.isModelClass(ChildModel.class));
    }

    @Test
    public void testAddingBundleWithNonResolvableNonModelAnnotation() throws ClassNotFoundException {
        ClassLoader classLoader = new HideClassesClassLoader(this.getClass().getClassLoader(), Hidden.class.getName());
        ModelPackageBundleListener listener =
                createListenerForBundleWithClass(classLoader, SimpleModelWithInvalidSecondAnnotation.class.getName());
        listener.addingBundle(mockBundle, new BundleEvent(BundleEvent.STARTED, mockBundle));
        Assert.assertFalse(
                "Model should not yet have been registered but was",
                adapterImplementations.isModelClass(SimpleModelWithInvalidSecondAnnotation.class));
    }

    private ModelPackageBundleListener createListenerForBundleWithClass(Class<?> modelClass)
            throws ClassNotFoundException {
        return createListenerForBundleWithClass(modelClass.getClassLoader(), modelClass.getName());
    }

    private ModelPackageBundleListener createListenerForBundleWithClass(ClassLoader classLoader, String className)
            throws ClassNotFoundException {
        Dictionary<String, String> headers = new Hashtable<>();
        headers.put(ModelPackageBundleListener.CLASSES_HEADER, className);
        Mockito.when(mockBundle.getHeaders()).thenReturn(headers);
        Mockito.when(mockBundle.loadClass(Mockito.anyString())).thenAnswer(new Answer<Class<?>>() {
            @Override
            public Class<?> answer(InvocationOnMock invocation) throws Throwable {
                Object argument = invocation.getArguments()[0];
                if (argument.equals(className)) {
                    return classLoader.loadClass(className);
                } else {
                    throw new ClassNotFoundException("Could not find class with name " + argument);
                }
            }
        });
        return new ModelPackageBundleListener(
                mockBundleContext, mockModelAdapterFactory, adapterImplementations, null, null);
    }
}
