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
package org.apache.sling.models.impl.via;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.ViaProviderType;
import org.apache.sling.models.annotations.via.OriginalResourceType;
import org.apache.sling.models.spi.ViaProvider;
import org.osgi.service.component.annotations.Component;

/**
 * This {@link ViaProvider} implements the counterpart of the {@link ForcedResourceTypeViaProvider} and the
 * {@link ResourceSuperTypeViaProvider}. It is in particular helpful in models that want to inject another model using the original
 * {@link Resource}'s, {@link SlingJakartaHttpServletRequest}'s or {@link org.apache.sling.api.SlingHttpServletRequest}'s resource type instead of the one forced by either of the above-mentioned
 * {@link ViaProvider}s
 * <p>
 * The implementation simply unwraps the {@link org.apache.sling.api.resource.ResourceWrapper},
 * {@link org.apache.sling.api.wrappers.SlingJakartaHttpServletRequestWrapper} or
 * {@link org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper} used by the {@link ForcedResourceTypeViaProvider} and
 * {@link ResourceSuperTypeViaProvider}.
 */
@Component
public class OriginalResourceTypeViaProvider implements ViaProvider {

    @Override
    public Class<? extends ViaProviderType> getType() {
        return OriginalResourceType.class;
    }

    @Override
    public Object getAdaptable(Object original, String value) {
        if (original instanceof SlingJakartaHttpServletRequest originalRequest) {
            while (originalRequest instanceof ResourceTypeForcingJakartaRequestWrapper jakartaRequest) {
                originalRequest = jakartaRequest.getSlingRequest();
            }
            return originalRequest;
        } else if (original instanceof org.apache.sling.api.SlingHttpServletRequest originalRequest) {
            while (originalRequest instanceof ResourceTypeForcingRequestWrapper javaxRequest) {
                originalRequest = javaxRequest.getSlingRequest();
            }
            return originalRequest;
        } else if (original instanceof Resource originalResource) {
            while (originalResource instanceof ResourceTypeForcingResourceWrapper wrapper) {
                originalResource = wrapper.getResource();
            }
            return originalResource;
        } else {
            return null;
        }
    }
}
