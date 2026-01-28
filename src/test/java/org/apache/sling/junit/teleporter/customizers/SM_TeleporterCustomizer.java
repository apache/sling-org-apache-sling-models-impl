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
package org.apache.sling.junit.teleporter.customizers;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeoutException;

import org.apache.sling.junit.rules.TeleporterRule;
import org.apache.sling.models.impl.ModelAdapterFactory;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.osgi.OsgiConsoleClient;
import org.apache.sling.testing.serversetup.instance.SlingTestBase;
import org.apache.sling.testing.teleporter.client.ClientSideTeleporter;
import org.apache.sling.testing.timeouts.TimeoutsProvider;

/** This is required by the TeleporterRule, to setup the client-side
 *  teleporter with (at least) the test server URL.
 */
public class SM_TeleporterCustomizer implements TeleporterRule.Customizer {

    private static final SlingTestBase S = new SlingTestBase();

    private static final Class[] EXPECTED_COMPONENTS = new Class[] {ModelAdapterFactory.class};

    @Override
    public void customize(TeleporterRule t, String options) {
        final ClientSideTeleporter cst = (ClientSideTeleporter) t;
        cst.setBaseUrl(S.getServerBaseUrl());
        cst.setServerCredentials(S.getServerUsername(), S.getServerPassword());
        cst.setTestReadyTimeoutSeconds(TimeoutsProvider.getInstance().getTimeout(5));
        cst.includeDependencyPrefix("org.apache.sling.models.it.testing");

        // additionally check for the registration of mandatory sling models components
        try (OsgiConsoleClient osgiClient =
                new OsgiConsoleClient(URI.create(S.getServerBaseUrl()), S.getServerUsername(), S.getServerPassword())) {
            for (Class clazz : EXPECTED_COMPONENTS) {
                osgiClient.waitComponentRegistered(clazz.getName(), 20000, 200);
            }
        } catch (ClientException | TimeoutException | InterruptedException | IOException ex) {
            throw new RuntimeException("Error waiting for expected components.", ex);
        }
    }
}
