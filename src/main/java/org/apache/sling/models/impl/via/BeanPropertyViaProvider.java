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

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.models.annotations.ViaProviderType;
import org.apache.sling.models.annotations.via.BeanProperty;
import org.apache.sling.models.spi.ViaProvider;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service=ViaProvider.class)
public class BeanPropertyViaProvider implements ViaProvider {

    private static final Logger log = LoggerFactory.getLogger(BeanPropertyViaProvider.class);

    @Override
    public Class<? extends ViaProviderType> getType() {
        return BeanProperty.class;
    }

    @Override
    public Object getAdaptable(Object original, String value) {
        if (StringUtils.isBlank(value)) {
            return ORIGINAL;
        }

        // support nested values, e.g. requestPathInfo.suffixResource.path
        if (StringUtils.contains(value, '.')) {
            String[] parts = StringUtils.split(value, ".", 2);
            Object adaptable = getAdaptable(original, parts[0]);
            return getAdaptable(adaptable, parts[1]);
        }

        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(original.getClass());
            for (PropertyDescriptor desc : beanInfo.getPropertyDescriptors()) {
                if (desc.getName().equals(value)) {
                    return desc.getReadMethod().invoke(original);
                }
            }
        } catch (Exception e) {
            log.error("Unable to execution projection " + value, e);
        }
        return null;
    }
}
