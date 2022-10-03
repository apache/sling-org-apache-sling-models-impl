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

import static org.apache.sling.api.scripting.SlingBindings.LOG;
import static org.apache.sling.api.scripting.SlingBindings.OUT;
import static org.apache.sling.api.scripting.SlingBindings.READER;
import static org.apache.sling.api.scripting.SlingBindings.REQUEST;
import static org.apache.sling.api.scripting.SlingBindings.RESOURCE;
import static org.apache.sling.api.scripting.SlingBindings.RESPONSE;
import static org.apache.sling.api.scripting.SlingBindings.SLING;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.script.Bindings;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.scripting.LazyBindings;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.models.factory.ExportException;
import org.apache.sling.models.factory.MissingExporterException;
import org.apache.sling.models.factory.ModelFactory;
import org.apache.sling.scripting.api.BindingsValuesProvidersByContext;
import org.apache.sling.scripting.core.ScriptHelper;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
class ExportServlet extends SlingSafeMethodsServlet {

    private static final String APPLICATION_JSON = "application/json";
    private static final String UTF_8 = "UTF-8";

    private final Logger logger;

    private final String exporterName;
    private final String registeredSelector;
    private final BundleContext bundleContext;
    private final ModelFactory modelFactory;
    private final BindingsValuesProvidersByContext bindingsValuesProvidersByContext;
    private final SlingModelsScriptEngineFactory scriptEngineFactory;
    private final ExportedObjectAccessor accessor;
    private final Map<String, String> baseOptions;

    public ExportServlet(BundleContext bundleContext, ModelFactory modelFactory,
                         BindingsValuesProvidersByContext bindingsValuesProvidersByContext, SlingModelsScriptEngineFactory scriptFactory,
                         Class<?> annotatedClass, String registeredSelector, String exporterName, ExportedObjectAccessor accessor,
                         Map<String, String> baseOptions) {
        this.bundleContext = bundleContext;
        this.modelFactory = modelFactory;
        this.bindingsValuesProvidersByContext = bindingsValuesProvidersByContext;
        this.scriptEngineFactory = scriptFactory;
        this.registeredSelector = registeredSelector;
        this.exporterName = exporterName;
        this.accessor = accessor;
        this.baseOptions = baseOptions;

        String loggerName = ExportServlet.class.getName() + "." + annotatedClass.getName();
        this.logger = LoggerFactory.getLogger(loggerName);
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        String contentType = request.getResponseContentType();
        response.setContentType(contentType);

        // workaround for SLING-7344
        if (APPLICATION_JSON.equals(contentType)) {
            response.setCharacterEncoding(UTF_8);
        }

        Map<String, String> options = createOptionMap(request);

        ScriptHelper scriptHelper = new ScriptHelper(bundleContext, null, request, response);

        try {
            addScriptBindings(scriptHelper, request, response);
            String exported;
            try {
                exported = accessor.getExportedString(request, options, modelFactory, exporterName);
            } catch (ExportException e) {
                logger.error("Could not perform export with " + exporterName + " requested by model.", e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            } catch (MissingExporterException e) {
                logger.error("Could not get exporter " + exporterName + " requested by model.", e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }
            if (exported == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            response.getWriter().write(exported);
        } finally {
            scriptHelper.cleanup();
        }
    }

    private void addScriptBindings(SlingScriptHelper scriptHelper, SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws IOException {
        Bindings bindings = new LazyBindings();
        bindings.put(SLING, scriptHelper);
        bindings.put(RESOURCE, request.getResource());
        bindings.put(SlingModelsScriptEngineFactory.RESOLVER, request.getResource().getResourceResolver());
        bindings.put(REQUEST, request);
        bindings.put(RESPONSE, response);
        try {
            bindings.put(READER, request.getReader());
        } catch (Exception e) {
            bindings.put(READER, new BufferedReader(new StringReader("")));
        }
        bindings.put(OUT, response.getWriter());
        bindings.put(LOG, logger);

        scriptEngineFactory.invokeBindingsValuesProviders(bindingsValuesProvidersByContext, bindings);

        SlingBindings slingBindings = new SlingBindings();
        slingBindings.putAll(bindings);

        request.setAttribute(SlingBindings.class.getName(), slingBindings);

    }

    private Map<String, String> createOptionMap(SlingHttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        String[] selectors = request.getRequestPathInfo().getSelectors();
        Map<String, String> result = new HashMap<String, String>(baseOptions.size() + parameterMap.size() + selectors.length - 1);
        result.putAll(baseOptions);
        for (String selector : selectors) {
            if (!selector.equals(registeredSelector)) {
                result.put(selector, "true");
            }
        }

        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            if (entry.getValue().length == 0) {
                result.put(entry.getKey(), Boolean.TRUE.toString());
            } else {
                result.put(entry.getKey(), entry.getValue()[0]);
            }
        }
        return result;
    }

    public interface ExportedObjectAccessor {
        String getExportedString(SlingHttpServletRequest request, Map<String, String> options, ModelFactory modelFactory, String exporterName) throws ExportException, MissingExporterException;
    }

    public static final class ResourceAccessor implements ExportedObjectAccessor {

        private final Class<?> adapterClass;

        public ResourceAccessor(Class<?> adapterClass) {
            this.adapterClass = adapterClass;
        }

        @Override
        public String getExportedString(SlingHttpServletRequest request, Map<String, String> options, ModelFactory modelFactory, String exporterName) throws ExportException, MissingExporterException {
            Object adapter = modelFactory.createModel(request.getResource(), adapterClass);
            return modelFactory.exportModel(adapter, exporterName, String.class, options);
        }
    }

    public static final class RequestAccessor implements ExportedObjectAccessor {

        private final Class<?> adapterClass;

        public RequestAccessor(Class<?> adapterClass) {
            this.adapterClass = adapterClass;
        }

        @Override
        public String getExportedString(SlingHttpServletRequest request, Map<String, String> options, ModelFactory modelFactory, String exporterName) throws ExportException, MissingExporterException {
            Object adapter = modelFactory.createModel(request, adapterClass);
            return modelFactory.exportModel(adapter, exporterName, String.class, options);
        }
    };

}
