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
package org.apache.sling.models.impl.serializer;

import org.apache.sling.commons.osgi.Order;
import org.apache.sling.commons.osgi.RankedServices;
import org.apache.sling.models.annotations.ExternalizePathProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import java.util.Map;

/**
 * Simple Implementation of the Externalize Path Provider Manager service
 * which just binds them and then selects the highest one (first one as the
 * order is descending).
 */
@Component(
    service={
        ExternalizePathProviderManager.class
    }
)
public class ExternalizePathProviderManagerService
    implements ExternalizePathProviderManager
{
    private RankedServices<ExternalizePathProvider> providers = new RankedServices<>(Order.DESCENDING);

    @Reference(
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.DYNAMIC
    )
    protected void bindExternalizePathProvider(final ExternalizePathProvider provider, final Map<String, Object> props) {
        providers.bind(provider, props);
    }

    protected void unbindExternalizePathProvider(final ExternalizePathProvider provider, final Map<String, Object> props) {
        providers.unbind(provider, props);
    }

    @Override
    public ExternalizePathProvider getExternalizedPathProvider() {
        return providers.getList().get(0);
    }
}
