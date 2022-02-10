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
package org.apache.sling.models.testmodels.classes;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.testmodels.interfaces.AdapterType1;
import org.apache.sling.models.testmodels.interfaces.AdapterType2;
import org.apache.sling.models.testmodels.interfaces.AdapterType3;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = { AdapterType2.class, AdapterType3.class },
    cache = true
)
public class CachedModelWithAdapterTypes23 implements AdapterType2, AdapterType3 {


}
