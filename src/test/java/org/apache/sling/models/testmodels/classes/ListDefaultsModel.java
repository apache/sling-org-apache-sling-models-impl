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

import java.util.List;

import javax.inject.Inject;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;

@Model(adaptables = Resource.class)
public class ListDefaultsModel {

    @Inject
    @Default(values = {"v1","v2"})
    private List<String> stringList;

    @Inject
    @Default(intValues = {1,2,3})
    private List<Integer> intList;

    @Inject
    @Default(longValues = {1,2})
    private List<Long> longList;

    @Inject
    @Default(booleanValues = {true,false})
    private List<Boolean> booleanList;

    @Inject
    @Default(shortValues = {1})
    private List<Short> shortList;

    @Inject
    @Default(floatValues = {1.1f,1.2f})
    private List<Float> floatList;

    @Inject
    @Default(doubleValues = {1.1d,1.2d,1.3d})
    private List<Double> doubleList;

    public List<String> getStringList() {
        return stringList;
    }

    public List<Integer> getIntList() {
        return intList;
    }

    public List<Long> getLongList() {
        return longList;
    }

    public List<Boolean> getBooleanList() {
        return booleanList;
    }

    public List<Short> getShortList() {
        return shortList;
    }

    public List<Float> getFloatList() {
        return floatList;
    }

    public List<Double> getDoubleList() {
        return doubleList;
    }

}
