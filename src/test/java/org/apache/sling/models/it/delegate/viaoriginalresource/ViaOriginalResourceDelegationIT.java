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
package org.apache.sling.models.it.delegate.viaoriginalresource;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.junit.rules.TeleporterRule;
import org.apache.sling.models.factory.ModelFactory;
import org.apache.sling.models.it.rtbound.FakeRequest;
import org.apache.sling.models.it.testbundle.delegate.viaoriginalresource.A;
import org.apache.sling.models.it.testbundle.delegate.viaoriginalresource.models.A1Impl;
import org.apache.sling.models.it.testbundle.delegate.viaoriginalresource.models.AImpl;
import org.apache.sling.models.it.testbundle.delegate.viaoriginalresource.models.B1Impl;
import org.apache.sling.models.it.testbundle.delegate.viaoriginalresource.models.BImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ViaOriginalResourceDelegationIT {

    @Rule
    public final TeleporterRule teleporter = TeleporterRule.forClass(getClass(), "SM_Teleporter");

    private ResourceResolverFactory rrFactory;

    private ModelFactory modelFactory;

    private final String genericComponent = "/apps/delegate/nestedrtbound/generic";
    private final String specificComponent = "/apps/delegate/nestedrtbound/specific";
    private final String genericContent = "/content/delegate/nestedrtbound/generic";
    private final String specificContent = "/content/delegate/nestedrtbound/specific";

    @Before
    @SuppressWarnings("null")
    public void setup() throws LoginException, PersistenceException {
        rrFactory = teleporter.getService(ResourceResolverFactory.class);
        modelFactory = teleporter.getService(ModelFactory.class);
        try (ResourceResolver adminResolver = rrFactory.getServiceResourceResolver(null); ) {

            Map<String, Object> properties = new HashMap<String, Object>();
            ResourceUtil.getOrCreateResource(adminResolver, genericComponent, properties, null, false);
            properties.clear();

            properties.put("sling:resourceSuperType", "delegate/nestedrtbound/generic");
            ResourceUtil.getOrCreateResource(adminResolver, specificComponent, properties, null, false);
            properties.clear();

            properties.put("sling:resourceType", "delegate/nestedrtbound/generic");
            ResourceUtil.getOrCreateResource(adminResolver, genericContent, properties, null, false);
            properties.clear();

            properties.put("sling:resourceType", "delegate/nestedrtbound/specific");
            ResourceUtil.getOrCreateResource(adminResolver, specificContent, properties, null, false);
            properties.clear();

            adminResolver.commit();
        }
    }

    @Test
    public void testGenericModelFromRequest() throws LoginException {
        try (ResourceResolver resolver = rrFactory.getServiceResourceResolver(null); ) {
            final Resource content = resolver.getResource(genericContent);
            final FakeRequest baseRequest = new FakeRequest(content);

            final A model = modelFactory.createModel(baseRequest, A.class);
            assertTrue(model instanceof AImpl);
            assertTrue(((AImpl) model).other instanceof BImpl);
        }
    }

    @Test
    public void testSpecificModelFromRequest() throws LoginException {
        try (ResourceResolver resolver = rrFactory.getServiceResourceResolver(null); ) {
            final Resource content = resolver.getResource(specificContent);
            final FakeRequest baseRequest = new FakeRequest(content);

            final A model = modelFactory.createModel(baseRequest, A.class);
            assertTrue(model instanceof A1Impl);
            assertTrue(((A1Impl) model).other instanceof B1Impl);
            assertTrue(((A1Impl) model).delegate instanceof AImpl);
            assertTrue(((AImpl) ((A1Impl) model).delegate).other instanceof B1Impl);
            // Since SLING-11133 and cache = true
            assertSame(((A1Impl) model).other, ((AImpl) ((A1Impl) model).delegate).other);
        }
    }

    @Test
    public void testGenericModelFromResource() throws LoginException {
        try (ResourceResolver resolver = rrFactory.getServiceResourceResolver(null); ) {
            final Resource content = resolver.getResource(genericContent);

            final A model = modelFactory.createModel(content, A.class);
            assertTrue(model instanceof AImpl);
            assertTrue(((AImpl) model).other instanceof BImpl);
        }
    }

    @Test
    public void testSpecificModelFromResource() throws LoginException {
        try (ResourceResolver resolver = rrFactory.getServiceResourceResolver(null); ) {
            final Resource content = resolver.getResource(specificContent);

            final A model = modelFactory.createModel(content, A.class);
            assertTrue(model instanceof A1Impl);
            assertTrue(((A1Impl) model).other instanceof B1Impl);
            assertTrue(((A1Impl) model).delegate instanceof AImpl);
            assertTrue(((AImpl) ((A1Impl) model).delegate).other instanceof B1Impl);
            // Since SLING-11133 and cache = true
            assertSame(((A1Impl) model).other, ((AImpl) ((A1Impl) model).delegate).other);
        }
    }
}
