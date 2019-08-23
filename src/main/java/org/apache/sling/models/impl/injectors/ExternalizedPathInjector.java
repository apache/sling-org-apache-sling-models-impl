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
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.ExternalizePath;
import org.apache.sling.models.annotations.injectorspecific.ExternalizedPathProvider;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Component(
    property=Constants.SERVICE_RANKING+":Integer=1000",
    service={
        Injector.class
    }
)
public class ExternalizedPathInjector
    extends AbstractInjector
    implements Injector
{
    List<ExternalizedPathProvider> providerList = new ArrayList<>();

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
    void bindExternalizedPathProvider(ExternalizedPathProvider provider) {
        providerList.add(provider);
        // The providers are sorted so that the one with the highest priority is the first entry
        Collections.sort(
            providerList,
            Comparator.comparingInt(ExternalizedPathProvider::getPriority).reversed()
        );
    }

    void unbindExternalizedPathProvider(ExternalizedPathProvider provider) {
        providerList.remove(provider);
    }

    public ExternalizedPathInjector() {
        bindExternalizedPathProvider(new DefaultExternalizedPathProvider());
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
            if(properties != ObjectUtils.NULL) {
                String imagePath = properties.get(name, String.class);
                if(imagePath != null) {
                    ExternalizedPathProvider provider = providerList.get(0);
                    return provider.externalize(adaptable, imagePath);
                }
            }
        }
        return null;
    }

    /** Fallback Implementation of the Externalized Path Provider that uses the Resource Resolver's map function **/
    private class DefaultExternalizedPathProvider
        implements ExternalizedPathProvider
    {
        @Override
        public int getPriority() {
            return FALLBACK_PRIORITY;
        }

        @Override
        public String externalize(@NotNull Object adaptable, String sourcePath) {
            String answer = sourcePath;
            ResourceResolver resourceResolver = getResourceResolver(adaptable);
            if(sourcePath != null && resourceResolver != null) {
                answer = resourceResolver.map(sourcePath);
            }
            return answer;
        }
    }
}
