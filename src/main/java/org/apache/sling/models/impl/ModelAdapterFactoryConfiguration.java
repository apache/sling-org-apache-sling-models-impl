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

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Apache Sling Model Adapter Factory")
public @interface ModelAdapterFactoryConfiguration {
    @AttributeDefinition(name = "Maximum Recursion Depth", description = "Maximum depth adaptation will be attempted.")
    int max_recursion_depth() default 20;

    @AttributeDefinition(
            name = "Cleanup Job Period",
            description = "Period in seconds at which OSGi service references from ThreadLocals will be cleaned up.")
    long cleanup_job_period() default 30l;

    @AttributeDefinition(
            name = "Cache Implementation Lookups",
            description = "Cache the resolution of a model implementation class per adaptable type and resource type "
                    + "within a ResourceResolver. This avoids redundant repository lookups when the same model is "
                    + "resolved repeatedly (e.g. once for canCreateFromAdaptable and once for createModel, or across "
                    + "many components sharing a resource type). Disable this if a custom ImplementationPicker selects "
                    + "an implementation based on more than the resource type (e.g. selectors or request attributes).")
    boolean cache_implementation_lookups() default true;

    @AttributeDefinition(
            name = "Implementation Lookup Cache Size",
            description = "Maximum number of entries in the per-ResourceResolver implementation lookup cache (see "
                    + "'Cache Implementation Lookups'). Once exceeded, the least recently used entries are evicted, so "
                    + "that the cache cannot grow unbounded for long-lived resolvers or large numbers of distinct "
                    + "resource types. Must be greater than 0.")
    int implementation_lookup_cache_size() default 100;
}
