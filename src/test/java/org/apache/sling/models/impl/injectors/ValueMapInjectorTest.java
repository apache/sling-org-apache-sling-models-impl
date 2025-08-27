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
package org.apache.sling.models.impl.injectors;

import java.lang.reflect.AnnotatedElement;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.JakartaToJavaxRequestWrapper;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ValueMapInjectorTest {

    private ValueMapInjector injector = new ValueMapInjector();

    @Mock
    private ValueMap valueMap;

    @Mock
    private AnnotatedElement element;

    @Mock
    private DisposalCallbackRegistry registry;

    @Mock
    private Resource resource;

    @Mock
    private SlingJakartaHttpServletRequest jakartaRequest;

    private static final String STRING_PARAM = "param1";
    private static final String INTEGER_PARAM = "param2";
    private static final String CLASS_PARAM = "param3";
    private static final String STRING_VALUE = "myValue";
    private static final int INTEGER_VALUE = 42;
    private static final ResourceResolver CLASS_INSTANCE = mock(ResourceResolver.class);

    @Before
    public void setUp() {
        when(valueMap.get(STRING_PARAM, String.class)).thenReturn(STRING_VALUE);
        when(valueMap.get(INTEGER_PARAM, Integer.class)).thenReturn(INTEGER_VALUE);
        when(valueMap.get(CLASS_PARAM, ResourceResolver.class)).thenReturn(CLASS_INSTANCE);

        when(this.resource.adaptTo(ValueMap.class)).thenReturn(valueMap);
        when(this.jakartaRequest.getResource()).thenReturn(this.resource);
    }

    @Test
    public void testStringParam() {
        Object result = injector.getValue(valueMap, STRING_PARAM, String.class, element, registry);
        assertEquals(STRING_VALUE, result);
    }

    @Test
    public void testIntegerParam() {
        Object result = injector.getValue(valueMap, INTEGER_PARAM, Integer.class, element, registry);
        assertEquals(INTEGER_VALUE, result);
    }

    @Test
    public void testClassInstance() {
        Object result = injector.getValue(valueMap, CLASS_PARAM, ResourceResolver.class, element, registry);
        assertSame(CLASS_INSTANCE, result);
    }

    @Test
    public void testNonMatchingClassInstance() {
        Object result = injector.getValue(valueMap, CLASS_PARAM, Resource.class, element, registry);
        assertNull(result);
    }

    @Test
    public void testNonValueMapAdaptable() {
        Object result = injector.getValue(mock(ResourceResolver.class), STRING_PARAM, String.class, element, registry);
        assertNull(result);
    }

    @Test
    public void testStringParamFromJakartaRequest() {
        Object result = this.injector.getValue(this.jakartaRequest, STRING_PARAM, String.class, element, registry);
        assertEquals(STRING_VALUE, result);
    }

    /**
     * @deprecated use {@link #testJakartaRequest()} instead
     */
    @Deprecated(since = "2.0.0")
    @Test
    public void testStringParamFromJavaxRequest() {
        org.apache.sling.api.SlingHttpServletRequest javaxRequest =
                JakartaToJavaxRequestWrapper.toJavaxRequest(this.jakartaRequest);
        Object result = this.injector.getValue(javaxRequest, STRING_PARAM, String.class, element, registry);
        assertEquals(STRING_VALUE, result);
    }
}
