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
package org.apache.sling.models.impl.injectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.osgi.Order;
import org.apache.sling.commons.osgi.RankedServices;
import org.apache.sling.models.annotations.ExternalizePath;
import org.apache.sling.models.annotations.injectorspecific.ExternalizePathProvider;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.Injector;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Map;

/**
 * Injector for a Model Property with the Annotation 'Externalize Path'
 * which will change a Sling Path into its externalize form (shortening etc)
 *
 * The Externalize Path Provider is what will do the actual transformation and
 * is pluggable. The component with the highest service ranking is selected here
 * to provide the transformation.
 */
@Component(
    property=Constants.SERVICE_RANKING+":Integer=1000",
    service={
        Injector.class
    }
)
public class ExternalizePathInjector
    extends AbstractInjector
    implements Injector
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
    public @NotNull String getName() {
        return "externalize-path";
    }

    @Override
    public Object getValue(@NotNull Object adaptable, String name, @NotNull Type type, @NotNull AnnotatedElement element,
            @NotNull DisposalCallbackRegistry callbackRegistry) {
        if (adaptable == ObjectUtils.NULL) {
            return null;
        }
        if (element.isAnnotationPresent(ExternalizePath.class)) {
            ValueMap properties = getValueMap(adaptable);
            if (properties != ObjectUtils.NULL) {
                String imagePath = properties.get(name, String.class);
                if (imagePath != null) {
                    Iterator<ExternalizePathProvider> i = providers.iterator();
                    if (i.hasNext()) {
                        ExternalizePathProvider provider = i.next();
                        return provider.externalize(adaptable, imagePath);
                    }
                }
            }
        }
        return null;
    }

}
