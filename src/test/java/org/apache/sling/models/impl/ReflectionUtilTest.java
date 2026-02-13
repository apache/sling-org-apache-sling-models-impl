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

import java.lang.reflect.Constructor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings({
    "PackageVisibleInnerClass",
    "FieldCanBeLocal",
    "unused",
    "java:S1172",
    "InstanceVariableMayNotBeInitialized",
    "PublicConstructorInNonPublicClass"
})
class ReflectionUtilTest {

    static class TestClassOne {
        private final int field1;
        private final String field2;

        public TestClassOne(int field1, String field2) {
            this.field1 = field1;
            this.field2 = field2;
        }
    }

    static class TestClassTwo {
        private final int field1;
        private final String field2;
        private boolean field3;

        public TestClassTwo(int field1, String field2) {
            this.field1 = field1;
            this.field2 = field2;
        }
    }

    static class TestClassThree {
        private final int field1;

        public TestClassThree(int field1, String field2) {
            this.field1 = field1;
        }
    }

    static class TestClassFour {

        public TestClassFour(int field1, String field2, boolean field3) {}
    }

    static class TestClassFive {
        private final int field1;

        public TestClassFive(int field1) {
            this.field1 = field1;
        }
    }

    static class TestClassSix {
        private final int field1;

        public TestClassSix() {
            this.field1 = 0;
        }
    }

    static class TestClassSeven {
        private final String field1;
        private final String field2;

        public TestClassSeven(String field1, String field2) {
            this.field1 = field1;
            this.field2 = field2;
        }
    }

    static class TestClassEight {
        private final String field1;

        public TestClassEight(String field1, String field2) {
            this.field1 = field1;
        }
    }

    @Test
    void testBalancedConstructor() throws NoSuchMethodException {
        Constructor<?> constructor = TestClassOne.class.getConstructor(int.class, String.class);
        assertTrue(ReflectionUtil.areBalancedCtorParamsAndFields(constructor));
    }

    @Test
    void testMoreFieldsThanParams() throws NoSuchMethodException {
        Constructor<?> constructor = TestClassTwo.class.getConstructor(int.class, String.class);
        assertFalse(ReflectionUtil.areBalancedCtorParamsAndFields(constructor));
    }

    @Test
    void testMoreParamsThanFields() throws NoSuchMethodException {
        Constructor<?> constructor = TestClassThree.class.getConstructor(int.class, String.class);
        assertFalse(ReflectionUtil.areBalancedCtorParamsAndFields(constructor));
    }

    @Test
    void testNoFields() throws NoSuchMethodException {
        Constructor<?> constructor = TestClassFour.class.getConstructor(int.class, String.class, boolean.class);
        assertFalse(ReflectionUtil.areBalancedCtorParamsAndFields(constructor));
    }

    @Test
    void testOneFieldOneParam() throws NoSuchMethodException {
        Constructor<?> constructor = TestClassFive.class.getConstructor(int.class);
        assertTrue(ReflectionUtil.areBalancedCtorParamsAndFields(constructor));
    }

    @Test
    void testNoParams() throws NoSuchMethodException {
        Constructor<?> constructor = TestClassSix.class.getConstructor();
        assertFalse(ReflectionUtil.areBalancedCtorParamsAndFields(constructor));
    }

    @Test
    void testBalancedRepeatableTypes() throws NoSuchMethodException {
        Constructor<?> constructor = TestClassSeven.class.getConstructor(String.class, String.class);
        assertTrue(ReflectionUtil.areBalancedCtorParamsAndFields(constructor));
    }

    @Test
    void testUnbalancedRepeatableTypes() throws NoSuchMethodException {
        Constructor<?> constructor = TestClassEight.class.getConstructor(String.class, String.class);
        assertFalse(ReflectionUtil.areBalancedCtorParamsAndFields(constructor));
    }
}
