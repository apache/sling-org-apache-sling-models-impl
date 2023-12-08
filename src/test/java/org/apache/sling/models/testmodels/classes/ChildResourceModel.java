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
package org.apache.sling.models.testmodels.classes;

import javax.inject.Inject;
import javax.inject.Named;

import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;

@Model(adaptables = Resource.class)
public class ChildResourceModel {

    @Inject
    private Resource firstChild;

    @Inject
    @Named("secondChild")
    private List<Resource> grandChildren;

    @Inject
    @Named("emptyChild")
    private List<Resource> emptyGrandChildren;

    public Resource getFirstChild() {
        return firstChild;
    }

    public List<Resource> getGrandChildren() {
        return grandChildren;
    }

    public List<Resource> getEmptyGrandChildren() {
        return emptyGrandChildren;
    }
}
