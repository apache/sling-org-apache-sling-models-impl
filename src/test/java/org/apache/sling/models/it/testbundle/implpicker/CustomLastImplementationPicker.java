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
package org.apache.sling.models.it.testbundle.implpicker;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.spi.ImplementationPicker;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceRanking;

/**
 * This is a curious {@link ImplementationPicker} implementation for integration test
 * that picks the last implementation if the resource has the name "custom";
 */
@Component(service = ImplementationPicker.class)
@ServiceRanking(100)
public class CustomLastImplementationPicker implements ImplementationPicker {

    public static final String CUSTOM_NAME = "custom";

    public Class<?> pick(@NotNull Class<?> adapterType, Class<?>[] implementationsTypes, @NotNull Object adaptable) {
        if (adaptable instanceof Resource && StringUtils.equals(((Resource) adaptable).getName(), CUSTOM_NAME)) {
            return implementationsTypes[implementationsTypes.length - 1];
        }
        return null;
    }
}
