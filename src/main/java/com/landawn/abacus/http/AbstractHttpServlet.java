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

import static com.landawn.abacus.http.HttpUtil.URL_ENCODED;

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

/**
 * The Class AbstractHttpServlet.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public abstract class AbstractHttpServlet extends HttpServlet {
    private static final long serialVersionUID = -6025259563199941531L;

    private static final ContentFormat DEFAULT_CONTENT_FORMAT = ContentFormat.JSON;

    /**
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
     * @param config
     * @param parameterName
     * @return
     */
    protected String getInitParameter(ServletConfig config, String parameterName) {
        String parameterValue = config.getInitParameter(parameterName);

        return N.isNullOrEmpty(parameterValue) ? parameterValue : parameterValue.trim();
    }

    /**
     * Checks if is url encoded.
     *
     * @param request
     * @return true, if is url encoded
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected boolean isUrlEncoded(HttpServletRequest request) throws IOException {
        String contentType = request.getHeader(HttpHeaders.Names.CONTENT_TYPE);

        return N.notNullOrEmpty(contentType) && contentType.indexOf(URL_ENCODED) >= 0;
    }

    protected ContentFormat getRequestContentFormat(HttpServletRequest request) {
        String contentType = request.getHeader(HttpHeaders.Names.CONTENT_TYPE);
        String contentEncoding = request.getHeader(HttpHeaders.Names.CONTENT_ENCODING);

        return getContentFormat(contentType, contentEncoding);
    }

    protected ContentFormat getResponseContentFormat(final HttpServletRequest request) {
        String accept = request.getHeader(HttpHeaders.Names.ACCEPT);

        if (N.isNullOrEmpty(accept)) {
            accept = request.getHeader(HttpHeaders.Names.CONTENT_TYPE);
        }

        String acceptEncoding = request.getHeader(HttpHeaders.Names.ACCEPT_ENCODING);

        if (N.isNullOrEmpty(acceptEncoding)) {
            acceptEncoding = request.getHeader(HttpHeaders.Names.CONTENT_ENCODING);
        }

        return getContentFormat(accept, acceptEncoding);
    }

    protected ContentFormat getContentFormat(final String contentType, final String contentEncoding) {
        ContentFormat contentFormat = HttpUtil.getContentFormat(contentType, contentEncoding);

        return contentFormat == null || contentFormat == ContentFormat.NONE ? DEFAULT_CONTENT_FORMAT : contentFormat;
    }

    /**
     * Gets the input stream.
     *
     * @param request
     * @param contentFormat
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected InputStream getInputStream(HttpServletRequest request, final ContentFormat contentFormat) throws IOException {
        final InputStream is = new UncloseableInputStream(request.getInputStream());

        return N.defaultIfNull(HttpUtil.wrapInputStream(is, contentFormat), N.emptyInputStream());
    }

    /**
     * Gets the output stream.
     *
     * @param response
     * @param contentFormat
     * @param acceptCharset
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected OutputStream getOutputStream(final HttpServletResponse response, final ContentFormat contentFormat, final String acceptCharset)
            throws IOException {
        final String contentType = HttpUtil.getContentType(contentFormat);

        if (N.notNullOrEmpty(contentType)) {
            response.setContentType(contentType);
        }

        final String contentEncoding = HttpUtil.getContentEncoding(contentFormat);

        if (N.notNullOrEmpty(contentEncoding)) {
            response.setHeader(HttpHeaders.Names.CONTENT_ENCODING, contentEncoding);
        }

        if (N.notNullOrEmpty(acceptCharset)) {
            response.setCharacterEncoding(acceptCharset);
        }

        final OutputStream os = new UncloseableOutputStream(response.getOutputStream());

        return HttpUtil.wrapOutputStream(os, contentFormat);
    }

    /**
     *
     * @param os
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected void flush(OutputStream os) throws IOException {
        HttpUtil.flush(os);
    }

    /**
     * Gets the parameter.
     *
     * @param parameterMap
     * @param parameterName
     * @return
     */
    protected String getParameter(Map<String, String[]> parameterMap, String parameterName) {
        final String[] values = parameterMap.get(parameterName);

        return N.isNullOrEmpty(values) ? null : values[0];
    }

    /**
     * Gets the parameter.
     *
     * @param parameterMap
     * @param parameterName
     * @param defaultValue
     * @return
     */
    protected String getParameter(Map<String, String[]> parameterMap, String parameterName, String defaultValue) {
        final String[] values = parameterMap.get(parameterName);

        return N.isNullOrEmpty(values) ? defaultValue : values[0];
    }

    /**
     * Gets the parameter.
     *
     * @param <T>
     * @param cls
     * @param parameterMap
     * @param parameterName
     * @return
     */
    protected <T> T getParameter(Class<T> cls, Map<String, String[]> parameterMap, String parameterName) {
        final String[] values = parameterMap.get(parameterName);

        return N.isNullOrEmpty(values) ? N.defaultValueOf(cls) : N.convert(values[0], cls);
    }

    /**
     * Gets the parameter.
     *
     * @param <T>
     * @param cls
     * @param parameterMap
     * @param parameterName
     * @param defaultValue
     * @return
     */
    protected <T> T getParameter(Class<T> cls, Map<String, String[]> parameterMap, String parameterName, T defaultValue) {
        final String[] values = parameterMap.get(parameterName);

        return N.isNullOrEmpty(values) ? defaultValue : N.convert(values[0], cls);
    }
}
