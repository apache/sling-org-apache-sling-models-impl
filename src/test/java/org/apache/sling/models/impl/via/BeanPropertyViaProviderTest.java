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
package org.apache.sling.models.impl.via;

import org.apache.sling.models.annotations.via.BeanProperty;
import org.apache.sling.models.spi.ViaProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
class BeanPropertyViaProviderTest {

    private BeanPropertyViaProvider provider = new BeanPropertyViaProvider();

    /**
     * Test method for {@link org.apache.sling.models.impl.via.BeanPropertyViaProvider#getType()}.
     */
    @Test
    void testGetType() {
        assertEquals(BeanProperty.class, provider.getType());
    }

    /**
     * Test method for {@link org.apache.sling.models.impl.via.BeanPropertyViaProvider#getAdaptable(java.lang.Object, java.lang.String)}.
     */
    @Test
    void testGetAdaptable() {
        // not handled
        assertEquals(ViaProvider.ORIGINAL, provider.getAdaptable(new Object(), ""));

        // caught exception
        assertNull(provider.getAdaptable(null, "expectedException"));

        // bean property not found
        assertNull(provider.getAdaptable(new Object(), "notfound"));

        // bean property found
        TestBean testBean = new TestBean("value1", null);
        assertEquals("value1", provider.getAdaptable(testBean, "field1"));

        // bean nested property found
        testBean = new TestBean("value1", new TestNestedBean());
        assertEquals("nestedValue1", provider.getAdaptable(testBean, "nested1.nestedField1"));
    }

    private static class TestNestedBean {
        @SuppressWarnings("unused")
        public String getNestedField1() {
            return "nestedValue1";
        }
    }

    private static class TestBean {
        private String field1;
        private TestNestedBean nested1;

        private TestBean(String field1, TestNestedBean nested1) {
            super();
            this.field1 = field1;
            this.nested1 = nested1;
        }

        @SuppressWarnings("unused")
        public String getField1() {
            return field1;
        }

        @SuppressWarnings("unused")
        public TestNestedBean getNested1() {
            return nested1;
        }
    }
}
