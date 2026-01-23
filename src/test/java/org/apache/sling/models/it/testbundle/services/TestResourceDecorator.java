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
package org.apache.sling.models.it.testbundle.services;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.adapter.AdapterManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceDecorator;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.resource.ValueMap;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = ResourceDecorator.class)
public class TestResourceDecorator implements ResourceDecorator {

    @Reference
    private AdapterManager adapterManager;

    @Override
    @SuppressWarnings("null")
    public Resource decorate(@NotNull Resource resource) {
        ValueMap map = resource.adaptTo(ValueMap.class);
        if (map != null && map.containsKey("decorate")) {
            if (map.get("decorate", "default").equals("customAdaptTo")) {
                return new ResourceWrapper(resource) {
                    @Override
                    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
                        AdapterType adapter = adapterManager.getAdapter(this, type);
                        if (adapter != null) {
                            return adapter;
                        } else {
                            return super.adaptTo(type);
                        }
                    }
                };
            } else {
                return new ResourceWrapper(resource);
            }
        }
        return null;
    }

    @Override
    public Resource decorate(@NotNull Resource resource, @NotNull HttpServletRequest request) {
        return decorate(resource);
    }
}
