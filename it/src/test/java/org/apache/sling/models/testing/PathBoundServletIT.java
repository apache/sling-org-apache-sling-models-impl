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
package org.apache.sling.models.testing;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.sling.junit.rules.TeleporterRule;
import org.apache.sling.models.testing.helper.FakeRequest;
import org.apache.sling.models.testing.helper.FakeResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class PathBoundServletIT {

    @Rule
    public final TeleporterRule teleporter = TeleporterRule.forClass(getClass(), "SM_Teleporter");

    private ResourceResolverFactory rrFactory;
    private SlingRequestProcessor slingRequestProcessor;

    @Before
    @SuppressWarnings("null")
    public void setup() {
        rrFactory = teleporter.getService(ResourceResolverFactory.class);
        slingRequestProcessor = teleporter.getService(SlingRequestProcessor.class);
    }

    @Test
    public void testDoubledServlets() throws Exception {
        try (ResourceResolver resolver = rrFactory.getServiceResourceResolver(null); ) {
            FakeResponse response = new FakeResponse();
            slingRequestProcessor.processRequest(new FakeRequest("/apps/rtpickerrequest"), response, resolver);

            Assert.assertEquals(200, response.getStatus());
        }
    }
}
