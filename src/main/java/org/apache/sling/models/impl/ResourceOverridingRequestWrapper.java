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

import javax.script.Bindings;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.AdapterManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.apache.sling.scripting.api.BindingsValuesProvidersByContext;

import static org.apache.sling.api.scripting.SlingBindings.LOG;
import static org.apache.sling.api.scripting.SlingBindings.OUT;
import static org.apache.sling.api.scripting.SlingBindings.READER;
import static org.apache.sling.api.scripting.SlingBindings.REQUEST;
import static org.apache.sling.api.scripting.SlingBindings.RESOURCE;
import static org.apache.sling.api.scripting.SlingBindings.RESPONSE;
import static org.apache.sling.api.scripting.SlingBindings.SLING;

/**
 * This request wrapper allows to adapt the given resource and request to a Sling Model
 */
class ResourceOverridingRequestWrapper extends SlingHttpServletRequestWrapper {

    private final Resource resource;
    private final AdapterManager adapterManager;
    private final Bindings bindings;

    ResourceOverridingRequestWrapper(
            SlingHttpServletRequest wrappedRequest,
            Resource resource,
            AdapterManager adapterManager,
            SlingModelsScriptEngineFactory scriptEngineFactory,
            BindingsValuesProvidersByContext bindingsValuesProvidersByContext) {
        super(wrappedRequest);
        this.resource = resource;
        this.adapterManager = adapterManager;

        SlingBindings existingBindings = (SlingBindings) wrappedRequest.getAttribute(SlingBindings.class.getName());

        bindings = new SlingBindings();
        if (existingBindings != null) {
            bindings.put(SLING, existingBindings.getSling());
            bindings.put(RESPONSE, existingBindings.getResponse());
            bindings.put(READER, existingBindings.getReader());
            bindings.put(OUT, existingBindings.getOut());
            bindings.put(LOG, existingBindings.getLog());
        }
        bindings.put(REQUEST, this);
        bindings.put(RESOURCE, resource);
        bindings.put(SlingModelsScriptEngineFactory.RESOLVER, resource.getResourceResolver());

        scriptEngineFactory.invokeBindingsValuesProviders(bindingsValuesProvidersByContext, bindings);
    }

    @Override
    public Object getAttribute(String name) {
        if (SlingBindings.class.getName().equals(name)) {
            return bindings;
        } else {
            return super.getAttribute(name);
        }
    }

    @Override
    public Resource getResource() {
        return resource;
    }

    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        return adapterManager.getAdapter(this, type);
    }
}
