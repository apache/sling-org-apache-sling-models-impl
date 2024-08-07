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

import java.util.HashMap;

import org.osgi.framework.Constants;

@SuppressWarnings("serial")
public class ServicePropertiesMap extends HashMap<String, Object> implements Comparable<ServicePropertiesMap> {

    public ServicePropertiesMap(long serviceId, int serviceRanking) {
        super();
        put(Constants.SERVICE_ID, serviceId);
        put(Constants.SERVICE_RANKING, serviceRanking);
    }

    @Override
    public int compareTo(ServicePropertiesMap o) {
        int result = ((Integer) get(Constants.SERVICE_RANKING)).compareTo((Integer) o.get(Constants.SERVICE_RANKING));
        if (result == 0) {
            result = ((Long) get(Constants.SERVICE_ID)).compareTo((Long) o.get(Constants.SERVICE_ID));
        }
        return result;
    }
}
