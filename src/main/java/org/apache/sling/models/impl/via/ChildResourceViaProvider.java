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

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.apache.sling.models.annotations.ViaProviderType;
import org.apache.sling.models.annotations.via.ChildResource;
import org.apache.sling.models.spi.ViaProvider;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service=ViaProvider.class)
public class ChildResourceViaProvider implements ViaProvider {

    private static final Logger log = LoggerFactory.getLogger(ChildResourceViaProvider.class);

    @Override
    public Class<? extends ViaProviderType> getType() {
        return ChildResource.class;
    }

    @Override
    public Object getAdaptable(Object original, String value) {
        if (StringUtils.isBlank(value)) {
            return ORIGINAL;
        }
        if (original instanceof Resource) {
            return ((Resource) original).getChild(value);
        } else if (original instanceof SlingHttpServletRequest) {
            final SlingHttpServletRequest request = (SlingHttpServletRequest) original;
            final Resource resource = request.getResource();
            if (resource == null) {
                return null;
            }
            Resource child = resource.getChild(value);
            if (child == null) {
                log.debug("Could not obtain child {} of resource {}", value, resource.getPath());
                return null;
            }
            return new ChildResourceRequestWrapper(request, child);
        } else {
            log.warn("Received unexpected adaptable of type {}.", original.getClass().getName());
            return null;
        }
    }

    private class ChildResourceRequestWrapper extends SlingHttpServletRequestWrapper {

        private final Resource resource;

        private ChildResourceRequestWrapper(SlingHttpServletRequest request, Resource resource) {
            super(request);
            this.resource = resource;
        }

        @Override
        public Resource getResource() {
            return resource;
        }
    }
}
