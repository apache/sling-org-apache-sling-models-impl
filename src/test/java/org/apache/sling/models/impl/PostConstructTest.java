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

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.factory.PostConstructException;
import org.apache.sling.models.testmodels.classes.FailingPostConstructModel;
import org.apache.sling.models.testmodels.classes.FalsePostConstructModel;
import org.apache.sling.models.testmodels.classes.SubClass;
import org.apache.sling.models.testmodels.classes.SubClassOverriddenPostConstruct;
import org.apache.sling.models.testmodels.classes.TruePostConstructModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class PostConstructTest {

    @Mock
    private Resource resource;

    private ModelAdapterFactory factory;

    @BeforeEach
    void setup() {
        factory = AdapterFactoryTest.createModelAdapterFactory();
        // no injectors are necessary
        factory.adapterImplementations.addClassesAsAdapterAndImplementation(
                SubClass.class,
                SubClassOverriddenPostConstruct.class,
                FailingPostConstructModel.class,
                FalsePostConstructModel.class,
                TruePostConstructModel.class);
    }

    @Test
    void testClassOrder() {
        SubClass sc = factory.getAdapter(resource, SubClass.class);
        assertTrue(sc.getPostConstructCalledTimestampInSub() > sc.getPostConstructCalledTimestampInSuper());
        assertTrue(sc.getPostConstructCalledTimestampInSuper() > 0);
    }

    @Test
    void testOverriddenPostConstruct() {
        SubClassOverriddenPostConstruct sc = factory.getAdapter(resource, SubClassOverriddenPostConstruct.class);
        assertEquals(
                1, sc.getPostConstructorCalledCounter(), "Post construct not called exactly one time in sub class!");
        assertEquals(
                0,
                sc.getPostConstructCalledTimestampInSuper(),
                "Post construct was called on super class although overridden in sub class");
    }

    @Test
    void testPostConstructMethodWhichThrowsException() {
        FailingPostConstructModel model = factory.getAdapter(resource, FailingPostConstructModel.class);
        assertNull(model);
    }

    @Test
    void testPostConstructMethodWhichReturnsFalse() {
        FalsePostConstructModel model = factory.getAdapter(resource, FalsePostConstructModel.class);
        assertNull(model);
    }

    @Test
    void testPostConstructMethodWhichReturnsTrue() {
        TruePostConstructModel model = factory.getAdapter(resource, TruePostConstructModel.class);
        assertNotNull(model);
    }

    @Test
    void testPostConstructMethodWhichReturnsFalseCreateModel() {
        assertThrows(PostConstructException.class, () -> factory.createModel(resource, FalsePostConstructModel.class));
    }

    void testPostConstructMethodWhichReturnsFalseInternalCreateModel() {
        assertSame(
                Result.POST_CONSTRUCT_PREVENTED_MODEL_CONSTRUCTION,
                factory.internalCreateModel(resource, FalsePostConstructModel.class));
    }

    @Test
    void testPostConstructMethodWhichReturnsTrueCreateModel() {
        TruePostConstructModel model = factory.createModel(resource, TruePostConstructModel.class);
        assertNotNull(model);
    }

    @Test
    void testPostConstructMethodWhichThrowsExceptionThrowingException() {
        boolean thrown = false;
        try {
            factory.createModel(resource, FailingPostConstructModel.class);
        } catch (PostConstructException e) {
            assertTrue(e.getMessage().contains("Post-construct"));
            assertEquals("FAIL", e.getCause().getMessage());
            thrown = true;
        }
        assertTrue(thrown);
    }
}
