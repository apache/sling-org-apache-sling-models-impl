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
package org.apache.sling.models.impl.via;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.spi.ViaProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractResourceTypeViaProvider implements ViaProvider {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public Object getAdaptable(Object original, String value) {
        if (!handle(value)) {
            return ORIGINAL;
        }
         if (original instanceof Resource) {
            final Resource resource = (Resource) original;
            final String resourceType = getResourceType(resource, value);
            if (resourceType == null) {
                log.warn("Could not determine forced resource type for {} using via value {}.", resource, value);
                return null;
            }
            return new ResourceTypeForcingResourceWrapper(resource, resourceType);
         } else if (original instanceof SlingHttpServletRequest) {
            final SlingHttpServletRequest request = (SlingHttpServletRequest) original;
            final Resource resource = request.getResource();
            if (resource == null) {
                return null;
            }
            final String resourceType = getResourceType(resource, value);
            if (resourceType == null) {
                log.warn("Could not determine forced resource type for {} using via value {}.", resource, value);
                return null;
            }
            return new ResourceTypeForcingRequestWrapper(request, resource, resourceType);
         } else {
            log.warn("Received unexpected adaptable of type {}.", original.getClass().getName());
            return null;
         }
    }

    protected abstract boolean handle(@NotNull String value);

    protected abstract @Nullable String getResourceType(@NotNull Resource resource, @NotNull String value);

}
