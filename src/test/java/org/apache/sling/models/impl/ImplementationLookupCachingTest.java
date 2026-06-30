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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.spi.ImplementationPicker;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.converter.Converters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the per-{@link ResourceResolver} caching of model implementation class lookups (SLING-12217).
 *
 * <p>The lookup of the implementation class is the operation that - for resource-type-based adaptation - walks the
 * resource super type hierarchy and therefore causes repository access. By counting how often the
 * {@link ImplementationPicker} is consulted, these tests verify that the lookup is performed at most once per
 * {@code (adaptable type, requested type, resource type)} within a single resolver.
 */
@ExtendWith(MockitoExtension.class)
class ImplementationLookupCachingTest {

    /** The single resolver shared by all resources, providing the (resolver-scoped) cache via its property map. */
    private ResourceResolver resolver;

    private final Map<String, Object> propertyMap = new HashMap<>();

    /** Counts how often the implementation lookup actually consults the picker. */
    private final AtomicInteger pickCount = new AtomicInteger();

    private final ImplementationPicker countingPicker = new ImplementationPicker() {
        @Override
        public Class<?> pick(
                @NotNull Class<?> adapterType, Class<?> @NotNull [] implementationsTypes, @NotNull Object adaptable) {
            pickCount.incrementAndGet();
            if (adapterType == TestAdapter.class) {
                return TestModel.class;
            }
            // UnmatchedAdapter has registered implementations but the picker selects none -> negative result
            return null;
        }
    };

    @BeforeEach
    void setUp() {
        resolver = mock(ResourceResolver.class);
        lenient().when(resolver.getPropertyMap()).thenReturn(propertyMap);
    }

    private ModelAdapterFactory createFactory(boolean cacheLookups) {
        return createFactory(cacheLookups, 100);
    }

    private ModelAdapterFactory createFactory(boolean cacheLookups, int cacheSize) {
        ComponentContext componentCtx = mock(ComponentContext.class);
        BundleContext bundleContext = mock(BundleContext.class);
        when(componentCtx.getBundleContext()).thenReturn(bundleContext);

        ModelAdapterFactory factory = new ModelAdapterFactory();
        Map<String, Object> config = new HashMap<>();
        config.put("cache.implementation.lookups", cacheLookups);
        config.put("implementation.lookup.cache.size", cacheSize);
        factory.activate(
                componentCtx,
                Converters.standardConverter().convert(config).to(ModelAdapterFactoryConfiguration.class));
        factory.injectAnnotationProcessorFactories = Collections.emptyList();
        factory.injectAnnotationProcessorFactories2 = Collections.emptyList();
        factory.injectors = Collections.emptyList();
        factory.implementationPickers = Collections.singletonList(countingPicker);
        // register with an explicit adapter type so that the picker (and hence the lookup) is consulted
        factory.adapterImplementations.addAll(TestModel.class, TestAdapter.class);
        factory.adapterImplementations.addAll(UnmatchedModel.class, UnmatchedAdapter.class);
        return factory;
    }

    /** Size of the nested (bounded) lookup cache stored in the resolver property map, or 0 if none was created yet. */
    @SuppressWarnings("unchecked")
    private int nestedCacheSize() {
        if (propertyMap.isEmpty()) {
            return 0;
        }
        return ((Map<String, Object>) propertyMap.values().iterator().next()).size();
    }

    private Resource resourceOfType(String resourceType) {
        Resource resource = mock(Resource.class);
        lenient().when(resource.getResourceType()).thenReturn(resourceType);
        lenient().when(resource.getResourceResolver()).thenReturn(resolver);
        return resource;
    }

    @Test
    void lookupIsCachedAcrossComponentsWithSameResourceType() {
        ModelAdapterFactory factory = createFactory(true);

        TestAdapter first = factory.getAdapter(resourceOfType("my/type"), TestAdapter.class);
        TestAdapter second = factory.getAdapter(resourceOfType("my/type"), TestAdapter.class);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(1, pickCount.get(), "the implementation lookup must run only once for a given resource type");
    }

    @Test
    void canCreateAndCreateShareTheCachedLookup() {
        ModelAdapterFactory factory = createFactory(true);
        Resource resource = resourceOfType("my/type");

        // this is the JavaUseProvider pattern: guard with canCreateFromAdaptable, then createModel
        assertTrue(factory.canCreateFromAdaptable(resource, TestAdapter.class));
        TestAdapter model = factory.createModel(resource, TestAdapter.class);

        assertNotNull(model);
        assertEquals(1, pickCount.get(), "canCreateFromAdaptable and createModel must not look up the type twice");
    }

    @Test
    void differentResourceTypesAreNotSharedAndResolveCorrectly() {
        ModelAdapterFactory factory = createFactory(true);

        TestAdapter a = factory.getAdapter(resourceOfType("type/a"), TestAdapter.class);
        TestAdapter b = factory.getAdapter(resourceOfType("type/b"), TestAdapter.class);

        assertNotNull(a);
        assertNotNull(b);
        assertEquals(2, pickCount.get(), "different resource types must each trigger their own lookup");
    }

    @Test
    void negativeResultIsCached() {
        ModelAdapterFactory factory = createFactory(true);

        // UnmatchedAdapter has registered implementations but the picker returns none -> no model
        assertFalse(factory.canCreateFromAdaptable(resourceOfType("my/type"), UnmatchedAdapter.class));
        assertFalse(factory.canCreateFromAdaptable(resourceOfType("my/type"), UnmatchedAdapter.class));

        assertEquals(1, pickCount.get(), "a negative lookup result must be memoized as well");
    }

    @Test
    void freshResolverDoesNotShareTheCache() {
        ModelAdapterFactory factory = createFactory(true);

        factory.getAdapter(resourceOfType("my/type"), TestAdapter.class);

        // a second resolver has its own property map -> the cache does not carry over
        ResourceResolver otherResolver = mock(ResourceResolver.class);
        when(otherResolver.getPropertyMap()).thenReturn(new HashMap<>());
        Resource otherResource = mock(Resource.class);
        when(otherResource.getResourceType()).thenReturn("my/type");
        when(otherResource.getResourceResolver()).thenReturn(otherResolver);

        factory.getAdapter(otherResource, TestAdapter.class);

        assertEquals(2, pickCount.get(), "a different resolver must not reuse the cached lookup");
    }

    @Test
    void leastRecentlyUsedEntryIsEvictedBeyondTheCap() {
        ModelAdapterFactory factory = createFactory(true, 2);

        factory.getAdapter(resourceOfType("type/a"), TestAdapter.class); // miss -> cache [a]
        factory.getAdapter(resourceOfType("type/b"), TestAdapter.class); // miss -> cache [a, b]
        factory.getAdapter(resourceOfType("type/c"), TestAdapter.class); // miss -> evicts a -> cache [b, c]
        assertEquals(3, pickCount.get());

        // type/a was evicted, so it must be looked up again
        factory.getAdapter(resourceOfType("type/a"), TestAdapter.class); // miss -> evicts b -> cache [c, a]
        assertEquals(4, pickCount.get(), "evicted entries must be recomputed");

        // type/c is still cached and was made most-recently-used above, so no new lookup
        factory.getAdapter(resourceOfType("type/c"), TestAdapter.class);
        assertEquals(4, pickCount.get(), "a still-cached entry must not be looked up again");

        assertEquals(2, nestedCacheSize(), "the cache must never exceed its configured size");
    }

    @Test
    void longLivedResolverCacheStaysBounded() {
        ModelAdapterFactory factory = createFactory(true, 3);

        for (int i = 0; i < 50; i++) {
            factory.getAdapter(resourceOfType("type/" + i), TestAdapter.class);
        }

        assertEquals(50, pickCount.get(), "every distinct resource type triggers a lookup once");
        assertEquals(3, nestedCacheSize(), "the cache must stay bounded regardless of resolver lifetime");
    }

    @Test
    void cachingCanBeDisabled() {
        ModelAdapterFactory factory = createFactory(false);

        factory.getAdapter(resourceOfType("my/type"), TestAdapter.class);
        factory.getAdapter(resourceOfType("my/type"), TestAdapter.class);

        assertEquals(2, pickCount.get(), "with caching disabled the lookup runs on every adaptation");
        assertTrue(propertyMap.isEmpty(), "no cache entries must be written when caching is disabled");
    }

    public interface TestAdapter {}

    @Model(adaptables = Resource.class, adapters = TestAdapter.class)
    public static class TestModel implements TestAdapter {}

    public interface UnmatchedAdapter {}

    @Model(adaptables = Resource.class, adapters = UnmatchedAdapter.class)
    public static class UnmatchedModel implements UnmatchedAdapter {}
}
