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

import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ChildResourceViaProviderTest {

    private ChildResourceViaProvider provider = new ChildResourceViaProvider();

    @Mock
    private Resource resource;

    @Mock
    private Resource childResource;

    @Mock
    private SlingHttpServletRequest request;

    @Before
    public void init() {
        when(resource.getChild("child")).thenReturn(childResource);
        when(request.getResource()).thenReturn(resource);
    }

    @Test
    public void testResource() {
        Object adaptable = provider.getAdaptable(resource, "child");
        Assert.assertEquals(adaptable, childResource);
    }

    @Test
    public void testRequest() {
        Object adaptable = provider.getAdaptable(request, "child");
        Resource adaptableResource = ((SlingHttpServletRequest) adaptable).getResource();
        Assert.assertEquals(adaptableResource, childResource);
    }

}
