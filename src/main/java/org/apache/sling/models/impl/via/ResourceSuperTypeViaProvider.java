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

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.ViaProviderType;
import org.apache.sling.models.annotations.via.ResourceSuperType;
import org.apache.sling.models.spi.ViaProvider;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;

@Component(service = ViaProvider.class)
public class ResourceSuperTypeViaProvider extends AbstractResourceTypeViaProvider {

    @Override
    public Class<? extends ViaProviderType> getType() {
        return ResourceSuperType.class;
    }

    @Override
    protected String getResourceType(@NotNull Resource resource, @NotNull String value) {
        return resource.getResourceResolver().getParentResourceType(resource);
    }

    @Override
    protected boolean handle(@NotNull String value) {
        return true;
    }
}
