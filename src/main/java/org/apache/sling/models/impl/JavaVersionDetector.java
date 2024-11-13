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

/**
 * This class helps to reliably detect the Java version that is used to run the current code, in order to help with
 * reflection and other compatibility issues. In addition it also provides some utility methods to check for specific
 * Java features.
 */
public final class JavaVersionDetector {

    private JavaVersionDetector() {}

    public static final int JAVA_VERSION = getJavaVersion();

    public static boolean supportsRecords() {
        return JAVA_VERSION >= 14;
    }

    /**
     * Detects the Java version that is used to run the current code.
     *
     * @return the major version number of the Java version, e.g. 8 for Java 8, 9 for Java 9, etc.
     */
    private static int getJavaVersion() {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            // Java versions 1.8 and earlier
            return Integer.parseInt(version.substring(2, 3));
        } else {
            // Java versions 9 and later
            int dotIndex = version.indexOf(".");
            int dashIndex = version.indexOf("-");
            try {
                if (dotIndex != -1) {
                    return Integer.parseInt(version.substring(0, dotIndex));
                } else if (dashIndex != -1) {
                    return Integer.parseInt(version.substring(0, dashIndex));
                } else {
                    return Integer.parseInt(version);
                }
            } catch (NumberFormatException e) {
                throw new IllegalStateException("Unexpected Java version format: " + version);
            }
        }
    }
}
