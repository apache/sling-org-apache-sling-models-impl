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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;

@Model(adaptables = Resource.class)
public class OptionalObjectsModel {

    @Inject
    private Optional<String> optionalString;

    @Inject
    private Optional<String> optionalNullString;

    @Inject
    private Optional<Byte> optionalByte;

    @Inject
    private Optional<Byte> optionalNullByte;

    @Inject
    private Optional<Short> optionalShort;

    @Inject
    private Optional<Short> optionalNullShort;

    @Inject
    private Optional<Integer> optionalInteger;

    @Inject
    private Optional<Integer> optionalNullInteger;

    @Inject
    private Optional<Long> optionalLong;

    @Inject
    private Optional<Long> optionalNullLong;

    @Inject
    private Optional<Float> optionalFloat;

    @Inject
    private Optional<Float> optionalNullFloat;

    @Inject
    private Optional<Double> optionalDouble;

    @Inject
    private Optional<Double> optionalNullDouble;

    @Inject
    private Optional<Character> optionalChar;

    @Inject
    private Optional<Character> optionalNullChar;

    @Inject
    private Optional<Boolean> optionalBoolean;

    @Inject
    private Optional<Boolean> optionalNullBoolean;

    @Inject
    private Optional<List> optionalList;

    @Inject
    private Optional<List> optionalNullList;

    @Inject
    private Optional<String[]> optionalArray;

    @Inject
    private Optional<String[]> optionalNullArray;

    @Inject
    private Optional<List<String>> stringList;

    @Inject
    private Optional<List<Integer>> intList;

    @Inject
    @Default(longValues = 1L)
    private Long optionalLongDefaultProperty;

    public Optional<String> getOptionalString() {
        return optionalString;
    }

    public Optional<String> getOptionalNullString() {
        return optionalNullString;
    }

    public Optional<Byte> getOptionalByte() {
        return optionalByte;
    }

    public Optional<Byte> getOptionalNullByte() {
        return optionalNullByte;
    }

    public Optional<Short> getOptionalShort() {
        return optionalShort;
    }

    public Optional<Short> getOptionalNullShort() {
        return optionalNullShort;
    }

    public Optional<Integer> getOptionalInteger() {
        return optionalInteger;
    }

    public Optional<Integer> getOptionalNullInteger() {
        return optionalNullInteger;
    }

    public Optional<Long> getOptionalLong() {
        return optionalLong;
    }

    public Optional<Long> getOptionalNullLong() {
        return optionalNullLong;
    }

    public Optional<Float> getOptionalFloat() {
        return optionalFloat;
    }

    public Optional<Float> getOptionalNullFloat() {
        return optionalNullFloat;
    }

    public Optional<Double> getOptionalDouble() {
        return optionalDouble;
    }

    public Optional<Double> getOptionalNullDouble() {
        return optionalNullDouble;
    }

    public Optional<Character> getOptionalChar() {
        return optionalChar;
    }

    public Optional<Character> getOptionalNullChar() {
        return optionalNullChar;
    }

    public Optional<Boolean> getOptionalBoolean() {
        return optionalBoolean;
    }

    public Optional<Boolean> getOptionalNullBoolean() {
        return optionalNullBoolean;
    }

    public Optional<? extends Collection> getOptionalList() {
        return optionalList;
    }

    public Optional<? extends Collection> getOptionalNullList() {
        return optionalNullList;
    }

    public Optional<String[]> getOptionalArray() {
        return optionalArray;
    }

    public Optional<String[]> getOptionalNullArray() {
        return optionalNullArray;
    }

    public Optional<List<String>> getStringList() {
        return stringList;
    }

    public Optional<List<Integer>> getIntList() {
        return intList;
    }

    public Long getOptionalLongDefaultProperty() {
        return optionalLongDefaultProperty;
    }
}
