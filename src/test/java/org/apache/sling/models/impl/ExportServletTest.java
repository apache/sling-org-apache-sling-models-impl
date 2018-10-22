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
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.factory.ModelFactory;
import org.apache.sling.models.testmodels.classes.DefaultStringModel;
import org.apache.sling.scripting.api.BindingsValuesProvidersByContext;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;

import javax.json.Json;
import javax.json.JsonReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.Collections;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class ExportServletTest {

    private static final String SELECTOR = "model";

    private ExportServlet exportServlet;

    @Mock
    private BundleContext bundleContext;

    @Mock
    private ModelFactory modelFactory;

    @Mock
    private BindingsValuesProvidersByContext bindingsValueProviderByContext;

    @Mock
    private SlingModelsScriptEngineFactory scriptFactory;

    @Mock
    private ExportServlet.ExportedObjectAccessor exportedObjectAccessor;

    @Mock
    private SlingHttpServletRequest slingHttpServletRequest;

    @Mock
    private SlingHttpServletResponse slingHttpServletResponse;

    @Mock
    private RequestPathInfo requestPathInfo;

    @Mock
    private Resource resource;

    @Mock
    private ResourceResolver resourceResolver;

    private Map<String, String> baseOption = Collections.emptyMap();
    private Class<?> annotatedClass = DefaultStringModel.class;
    private String registeredSelector = SELECTOR;
    private String exporterName = "exporterName";

    @Before
    public void setUp() {
        Mockito.when(resource.getResourceResolver()).thenReturn(resourceResolver);
        Mockito.when(requestPathInfo.getSelectors()).thenReturn(new String[]{SELECTOR});
        Mockito.when(slingHttpServletRequest.getResponseContentType()).thenReturn("application/json");
        Mockito.when(slingHttpServletRequest.getParameterMap()).thenReturn(Collections.emptyMap());
        Mockito.when(slingHttpServletRequest.getRequestPathInfo()).thenReturn(requestPathInfo);
        Mockito.when(slingHttpServletRequest.getResource()).thenReturn(resource);
        Mockito.when(slingHttpServletRequest.getProtocol()).thenReturn("HTTP/1.1");
        Mockito.when(slingHttpServletRequest.getMethod()).thenReturn("GET");

        exportServlet = new ExportServlet(bundleContext, modelFactory, bindingsValueProviderByContext, scriptFactory,
                annotatedClass, registeredSelector, exporterName, exportedObjectAccessor, baseOption);
    }

    @Test
    public void verifyPrintWriterFlushesOutputStream() throws Exception {
        // GIVEN
        String expectedJsonString = "{\"firstProperty\":\"firstDefault\",\"secondProperty\":\"firstDefault\"}";
        Mockito.when(exportedObjectAccessor
                .getExportedString(Matchers.any(SlingHttpServletRequest.class), Matchers.any(Map.class),
                        Matchers.any(ModelFactory.class), Matchers.anyString()))
                .thenReturn(expectedJsonString);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintWriter printWriter = new PrintWriter(byteArrayOutputStream);

        Mockito.when(slingHttpServletResponse.getWriter()).thenReturn(printWriter);

        // WHEN
        exportServlet.service(slingHttpServletRequest, slingHttpServletResponse);

        // THEN
        String actualJsonString = byteArrayOutputStream.toString();
        Assert.assertThat(actualJsonString, CoreMatchers.not(CoreMatchers.is("")));
        JsonReader expected = Json.createReader(new StringReader(expectedJsonString));
        JsonReader actual = Json.createReader(new StringReader(actualJsonString));
        Assert.assertThat(actual.read(), CoreMatchers.is(expected.read()));
    }
}
