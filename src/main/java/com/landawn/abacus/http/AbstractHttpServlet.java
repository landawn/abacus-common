/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.http;

import static com.landawn.abacus.http.HTTP.URL_ENCODED;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.landawn.abacus.util.N;

// TODO: Auto-generated Javadoc
/**
 * The Class AbstractHttpServlet.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public abstract class AbstractHttpServlet extends HttpServlet {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = -6025259563199941531L;

    /**
     * Inits the.
     *
     * @throws ServletException the servlet exception
     */
    @Override
    public void init() throws ServletException {
        super.init();
    }

    /**
     * Gets the inits the parameter.
     *
     * @param config the config
     * @param parameterName the parameter name
     * @return the inits the parameter
     */
    protected String getInitParameter(ServletConfig config, String parameterName) {
        String parameterValue = config.getInitParameter(parameterName);

        return N.isNullOrEmpty(parameterValue) ? parameterValue : parameterValue.trim();
    }

    /**
     * Checks if is url encoded.
     *
     * @param request the request
     * @return true, if is url encoded
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected boolean isUrlEncoded(HttpServletRequest request) throws IOException {
        String contentType = request.getHeader(HttpHeaders.Names.CONTENT_TYPE);

        return N.notNullOrEmpty(contentType) && contentType.indexOf(URL_ENCODED) >= 0;
    }

    /**
     * Gets the content format.
     *
     * @param request the request
     * @return the content format
     */
    protected ContentFormat getContentFormat(HttpServletRequest request) {
        String contentType = request.getHeader(HttpHeaders.Names.CONTENT_TYPE);
        String contentEncoding = request.getHeader(HttpHeaders.Names.CONTENT_ENCODING);

        ContentFormat contentFormat = HTTP.getContentFormat(contentType, contentEncoding);

        return contentFormat == null || contentFormat == ContentFormat.NONE ? ContentFormat.JSON : contentFormat;
    }

    /**
     * Gets the input stream.
     *
     * @param request the request
     * @param contentFormat the content format
     * @return the input stream
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected InputStream getInputStream(HttpServletRequest request, final ContentFormat contentFormat) throws IOException {
        final InputStream is = new UncloseableInputStream(request.getInputStream());

        return N.defaultIfNull(HTTP.wrapInputStream(is, contentFormat), N.emptyInputStream());
    }

    /**
     * Gets the output stream.
     *
     * @param response the response
     * @param contentFormat the content format
     * @return the output stream
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected OutputStream getOutputStream(HttpServletResponse response, ContentFormat contentFormat) throws IOException {
        final String contentType = HTTP.getContentType(contentFormat);

        if (N.notNullOrEmpty(contentType)) {
            response.setContentType(contentType);
        }

        final String contentEncoding = HTTP.getContentEncoding(contentFormat);

        if (N.notNullOrEmpty(contentEncoding)) {
            response.setHeader(HttpHeaders.Names.CONTENT_ENCODING, contentEncoding);
        }

        final OutputStream os = new UncloseableOutputStream(response.getOutputStream());

        return HTTP.wrapOutputStream(os, contentFormat);
    }

    /**
     * Flush.
     *
     * @param os the os
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected void flush(OutputStream os) throws IOException {
        HTTP.flush(os);
    }

    /**
     * Gets the parameter.
     *
     * @param parameterMap the parameter map
     * @param parameterName the parameter name
     * @return the parameter
     */
    protected String getParameter(Map<String, String[]> parameterMap, String parameterName) {
        final String[] values = parameterMap.get(parameterName);

        return N.isNullOrEmpty(values) ? null : values[0];
    }

    /**
     * Gets the parameter.
     *
     * @param parameterMap the parameter map
     * @param parameterName the parameter name
     * @param defaultValue the default value
     * @return the parameter
     */
    protected String getParameter(Map<String, String[]> parameterMap, String parameterName, String defaultValue) {
        final String[] values = parameterMap.get(parameterName);

        return N.isNullOrEmpty(values) ? defaultValue : values[0];
    }

    /**
     * Gets the parameter.
     *
     * @param <T> the generic type
     * @param cls the cls
     * @param parameterMap the parameter map
     * @param parameterName the parameter name
     * @return the parameter
     */
    protected <T> T getParameter(Class<T> cls, Map<String, String[]> parameterMap, String parameterName) {
        final String[] values = parameterMap.get(parameterName);

        return N.isNullOrEmpty(values) ? N.defaultValueOf(cls) : N.convert(values[0], cls);
    }

    /**
     * Gets the parameter.
     *
     * @param <T> the generic type
     * @param cls the cls
     * @param parameterMap the parameter map
     * @param parameterName the parameter name
     * @param defaultValue the default value
     * @return the parameter
     */
    protected <T> T getParameter(Class<T> cls, Map<String, String[]> parameterMap, String parameterName, T defaultValue) {
        final String[] values = parameterMap.get(parameterName);

        return N.isNullOrEmpty(values) ? defaultValue : N.convert(values[0], cls);
    }
}
