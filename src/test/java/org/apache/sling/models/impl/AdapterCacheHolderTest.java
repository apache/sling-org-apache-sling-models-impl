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

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class AdapterCacheHolderTest {

    @Test
    public void testGetMapNonServletRequestAdaptable() {
        Map<Object, Map<Class<?>, SoftReference<Object>>> outerMap = spy(new HashMap<>());
        Object adaptable1 = new Object();
        Object adaptable2 = new Object();

        try (AdapterCacheHolder adapterCacheHolder = new AdapterCacheHolder(false, outerMap)) {
            Map<Class<?>, SoftReference<Object>> map1 = adapterCacheHolder.getCacheMapForAdaptable(adaptable1);
            Map<Class<?>, SoftReference<Object>> map2 = adapterCacheHolder.getCacheMapForAdaptable(adaptable1);
            Map<Class<?>, SoftReference<Object>> map3 = adapterCacheHolder.getCacheMapForAdaptable(adaptable2);

            assertSame(map1, map2);
            assertNotSame(map1, map3);
        }
    }

    @Test
    public void testGetMapServletRequestAdaptable() {
        Map<Object, Map<Class<?>, SoftReference<Object>>> outerMap = spy(new HashMap<>());
        SlingHttpServletRequest adaptable1 = new MockSlingHttpServletRequest(null);
        SlingHttpServletRequest adaptable2 = new SlingHttpServletRequestWrapper(adaptable1);
        SlingHttpServletRequest adaptable3 = new MockSlingHttpServletRequest(null);

        try (AdapterCacheHolder adapterCacheHolder = new AdapterCacheHolder(false, outerMap)) {
            Map<Class<?>, SoftReference<Object>> map1 = adapterCacheHolder.getCacheMapForAdaptable(adaptable1);
            Map<Class<?>, SoftReference<Object>> map2 = adapterCacheHolder.getCacheMapForAdaptable(adaptable2);
            Map<Class<?>, SoftReference<Object>> map3 = adapterCacheHolder.getCacheMapForAdaptable(adaptable3);

            assertSame(map1, map2);
            assertSame(map1, map3);
        }
    }

    @Test
    public void testClose() {
        Object adaptable = new Object();
        Object result = new Object();
        SoftReference<Object> ref = new SoftReference<>(result);

        Map<Class<?>, SoftReference<Object>> innerMap = spy(new HashMap<>());
        innerMap.put(Object.class, ref);

        Map<Object, Map<Class<?>, SoftReference<Object>>> outerMap = spy(new HashMap<>());
        outerMap.put(adaptable, innerMap);

        try (AdapterCacheHolder adapterCacheHolder = new AdapterCacheHolder(false, outerMap)) {
            // intentionally empty
        }

        verify(outerMap).values();
        verify(outerMap).clear();
        verify(innerMap).clear();
    }

    @Test
    public void testNewMapSyncMap() {
        // This is a very weak test, but it should be sufficient to at least confirm that we are not just using a
        //  WeakHashMap, in case someone changes it without knowing why.
        Map<Object, Object> nonSyncMap = AdapterCacheHolder.newMap(false);
        Map<Object, Object> syncMap = AdapterCacheHolder.newMap(true);

        try {
            WeakHashMap<Object, Object> weakMap = (WeakHashMap<Object, Object>) nonSyncMap;
        } catch (ClassCastException e) {
            fail("Expected a WeakHashMap. Actual: " + nonSyncMap.getClass().getName());
        }

        try {
            WeakHashMap<Object, Object> weakMap = (WeakHashMap<Object, Object>) syncMap;
            fail("Expected a map other than WeakHashMap.");
        } catch (ClassCastException e) {
            // Expected to throw
        }
    }
}
