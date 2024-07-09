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
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.sling.api.SlingHttpServletRequest;
import org.jetbrains.annotations.NotNull;

class AdapterCacheHolder implements AutoCloseable {
    public static final String ADAPTABLE_ADAPTER_CACHE_KEY =
            ModelAdapterFactory.class.getName() + ".AdapterCacheHolder";

    /*
     * This is to handle that all requests, including wrappers, utilize the same cache. If it is desired
     */
    private static final String SHARED_REQUEST_CACHE = "SHARED_REQUEST_CACHE";

    // Map<Adaptable, Map<AdapterType, SoftReference<AdaptationResult>>
    private final Map<Object, Map<Class<?>, SoftReference<Object>>> cacheMap;

    private final boolean sync;

    /**
     * Create a cache, setting the sync and the Map fields. This constructor is primarily used to make testing easier.
     * @param sync true if maps should be synchronized,
     * @param cacheMap the map
     */
    AdapterCacheHolder(boolean sync, Map<Object, Map<Class<?>, SoftReference<Object>>> cacheMap) {
        this.sync = sync;
        this.cacheMap = cacheMap;
    }

    /**
     * Create a cache, optionally using synchronized maps.
     * @param sync true if maps should be synchronized,
     */
    public AdapterCacheHolder(boolean sync) {
        this(sync, newMap(sync));
    }

    /**
     * Gets the cache for a given adaptable.
     * @param adaptable the adaptalbe
     * @return the cache object
     */
    public @NotNull Map<Class<?>, SoftReference<Object>> getCacheMapForAdaptable(@NotNull Object adaptable) {
        Object adaptableOrSharedKey = adaptable;

        // All request wrappers share the cache of the original request
        if (adaptable instanceof SlingHttpServletRequest) {
            adaptableOrSharedKey = SHARED_REQUEST_CACHE;
        }

        return cacheMap.computeIfAbsent(adaptableOrSharedKey, key -> newMap(sync));
    }

    /**
     * Clears the cache. Useful for caches that are stored in a {@link org.apache.sling.api.resource.ResourceResolver}
     * or a {@link SlingHttpServletRequest}.
     */
    public void close() {
        cacheMap.values().forEach(Map::clear);
        cacheMap.clear();
    }

    /**
     * Create a new map, either synchronized or not.
     * @param sync true if it should use synchronized maps
     * @return a new map
     * @param <K> the key type
     * @param <V> the value type
     */
    static <K, V> @NotNull Map<K, V> newMap(boolean sync) {
        Map<K, V> theMap = new WeakHashMap<>();

        if (sync) {
            theMap = Collections.synchronizedMap(theMap);
        }

        return theMap;
    }
}
