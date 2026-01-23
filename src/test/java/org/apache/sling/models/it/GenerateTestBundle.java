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
package org.apache.sling.models.it;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.apache.sling.models.annotations.Model;
import org.ops4j.pax.tinybundles.TinyBundle;
import org.ops4j.pax.tinybundles.TinyBundles;
import org.osgi.framework.Constants;

/**
 * This uses tinybundles to create a test bundle that is deployed to sling
 * starter. The test bundle contains all classes from the packages
 * org.apache.sling.models.it.testbundle.*
 */
public class GenerateTestBundle {

    private static final String DUMMY_TEXT = "Dummy file for Integration Test bundle.";

    public static void main(String[] args) throws Exception {
        Path outputFile = Paths.get(args[0]);
        try (InputStream bundleStream = createBundle().build(TinyBundles.bndBuilder())) {
            Files.copy(bundleStream, outputFile);
        }
        System.out.println("Test bundle created at " + outputFile.toAbsolutePath());
    }

    static TinyBundle createBundle() {
        TinyBundle bundle = TinyBundles.bundle()
                .setHeader(Constants.BUNDLE_NAME, "Apache Sling Models Implementation - IT Test Bundle")
                .setHeader(Constants.BUNDLE_VERSION, "1.0.0-SNAPSHOT")
                .setHeader(Constants.EXPORT_PACKAGE, "org.apache.sling.models.it.testbundle.*")
                // optional import for 1 test case
                .setHeader(Constants.IMPORT_PACKAGE, "org.apache.commons.beanutils;resolution:=optional,*")
                // add dummy files to please verify-legal-files check
                .addResource("META-INF/LICENSE", new ByteArrayInputStream(DUMMY_TEXT.getBytes(StandardCharsets.UTF_8)))
                .addResource("META-INF/NOTICE", new ByteArrayInputStream(DUMMY_TEXT.getBytes(StandardCharsets.UTF_8)));

        // add all testbundle classes
        Set<String> modelClassNames = new TreeSet<>();
        getAllClasses().forEach(clazz -> {
            bundle.addClass(clazz);
            if (clazz.isAnnotationPresent(Model.class)) {
                modelClassNames.add(clazz.getName());
            }
        });

        bundle.setHeader("Sling-Model-Classes", modelClassNames.stream().collect(Collectors.joining(",")));

        return bundle;
    }

    /**
     * Dynamically find all classes in classpath under package(s) org.apache.sling.models.it.testbundle.*
     */
    static List<Class<?>> getAllClasses() {
        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo()
                .acceptPackages("org.apache.sling.models.it.testbundle")
                .scan()) {
            return scanResult.getAllClasses().stream().map(ClassInfo::loadClass).collect(Collectors.toList());
        }
    }
}
