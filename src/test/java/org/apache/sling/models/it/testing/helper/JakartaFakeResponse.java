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
package org.apache.sling.models.it.testing.helper;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Locale;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@SuppressWarnings("java:S1186") // ignore empty method warnings
public class JakartaFakeResponse implements HttpServletResponse {

    private StringWriter stringWriter = new StringWriter();
    private String contentType = null;
    private String characterEncoding = null;
    private int status = -1;

    @Override
    public void addCookie(Cookie cookie) {}

    @Override
    public boolean containsHeader(String name) {
        return false;
    }

    @Override
    public String encodeURL(String url) {
        return null;
    }

    @Override
    public String encodeRedirectURL(String url) {
        return null;
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {}

    @Override
    public void sendError(int sc) throws IOException {}

    @Override
    public void sendRedirect(String location) throws IOException {}

    @Override
    public void setDateHeader(String name, long date) {}

    @Override
    public void addDateHeader(String name, long date) {}

    @Override
    public void setHeader(String name, String value) {}

    @Override
    public void addHeader(String name, String value) {}

    @Override
    public void setIntHeader(String name, int value) {}

    @Override
    public void addIntHeader(String name, int value) {}

    @Override
    public void setStatus(int sc) {
        this.status = sc;
    }

    @Override
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return null;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return new PrintWriter(stringWriter);
    }

    @Override
    public void setCharacterEncoding(String charset) {
        this.characterEncoding = charset;
    }

    @Override
    public void setContentLength(int len) {}

    @Override
    public void setContentType(String type) {
        this.contentType = type;
    }

    @Override
    public void setBufferSize(int size) {}

    @Override
    public int getBufferSize() {
        return 0;
    }

    @Override
    public void flushBuffer() throws IOException {}

    @Override
    public void resetBuffer() {}

    @Override
    public boolean isCommitted() {
        return false;
    }

    @Override
    public void reset() {}

    @Override
    public void setLocale(Locale loc) {}

    @Override
    public Locale getLocale() {
        return null;
    }

    public StringWriter getStringWriter() {
        return stringWriter;
    }

    public int getStatus() {
        return status;
    }

    @Override
    public void setContentLengthLong(long len) {}

    @Override
    public String getHeader(String name) {
        return null;
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return null;
    }

    @Override
    public Collection<String> getHeaderNames() {
        return null;
    }

    @Override
    public void sendRedirect(String location, int sc, boolean clearBuffer) throws IOException {}
}
