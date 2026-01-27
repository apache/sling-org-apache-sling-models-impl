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
package org.apache.sling.models.impl;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LegacyAdaptablesExtender {

    private static final Logger LOGGER = LoggerFactory.getLogger(LegacyAdaptablesExtender.class);

    @SuppressWarnings("deprecation")
    public static Class<?>[] getAdaptables(Model modelAnnotation) {
        boolean hasJakartaServletRequest = false;
        boolean hasJavaxServletRequest = false;
        Class<?>[] adaptables = modelAnnotation.adaptables();
        for (Class<?> adaptable : adaptables) {
            if (adaptable == SlingJakartaHttpServletRequest.class) {
                hasJakartaServletRequest = true;
            } else if (adaptable == SlingHttpServletRequest.class) {
                hasJavaxServletRequest = true;
            }
        }

        if (hasJavaxServletRequest && !hasJakartaServletRequest) {

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        "Model {} adapts from {} but not from {}. Adjusting list to compensate.",
                        modelAnnotation,
                        SlingJakartaHttpServletRequest.class.getName(),
                        SlingHttpServletRequest.class.getName());
            }

            Class<?>[] newAdaptables = new Class<?>[adaptables.length + 1];
            System.arraycopy(adaptables, 0, newAdaptables, 0, adaptables.length);
            newAdaptables[adaptables.length] = SlingJakartaHttpServletRequest.class;
            return newAdaptables;
        }

        return adaptables;
    }

    private LegacyAdaptablesExtender() {}
}
