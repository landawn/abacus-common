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
import java.nio.charset.Charset;
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
    private static final long serialVersionUID = 2996815402031635727L;

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
    protected static String getInitParameter(ServletConfig config, String parameterName) {
        String parameterValue = config.getInitParameter(parameterName);

        return N.isNullOrEmpty(parameterValue) ? parameterValue : parameterValue.trim();
    }

    protected static String getContentType(HttpServletRequest request) {
        String contentType = request.getHeader(HttpHeaders.Names.CONTENT_TYPE);

        if (N.isNullOrEmpty(contentType)) {
            contentType = request.getHeader(HttpHeaders.Names.L_CONTENT_TYPE);
        }

        return contentType;
    }

    protected static String getContentEncoding(HttpServletRequest request) {
        String contentEncoding = request.getHeader(HttpHeaders.Names.CONTENT_ENCODING);

        if (N.isNullOrEmpty(contentEncoding)) {
            contentEncoding = request.getHeader(HttpHeaders.Names.L_CONTENT_ENCODING);
        }

        return contentEncoding;
    }

    protected static String getAccept(HttpServletRequest request) {
        String accept = request.getHeader(HttpHeaders.Names.ACCEPT);

        if (N.isNullOrEmpty(accept)) {
            accept = request.getHeader(HttpHeaders.Names.L_ACCEPT);
        }

        return accept;
    }

    protected static String getAcceptEncoding(HttpServletRequest request) {
        String acceptEncoding = request.getHeader(HttpHeaders.Names.ACCEPT_ENCODING);

        if (N.isNullOrEmpty(acceptEncoding)) {
            acceptEncoding = request.getHeader(HttpHeaders.Names.L_ACCEPT_ENCODING);
        }

        return acceptEncoding;
    }

    protected static String getAcceptCharset(HttpServletRequest request) {
        String acceptCharset = request.getHeader(HttpHeaders.Names.ACCEPT_CHARSET);

        if (N.isNullOrEmpty(acceptCharset)) {
            acceptCharset = request.getHeader(HttpHeaders.Names.L_ACCEPT_CHARSET);
        }

        return acceptCharset;
    }

    /**
     * Checks if is url encoded.
     *
     * @param request
     * @return true, if is url encoded
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected static boolean isUrlEncoded(HttpServletRequest request) throws IOException {
        String contentType = getContentType(request);

        return N.notNullOrEmpty(contentType) && contentType.indexOf(URL_ENCODED) >= 0;
    }

    protected static ContentFormat getRequestContentFormat(HttpServletRequest request) {
        return getContentFormat(getContentType(request), getContentEncoding(request));
    }

    protected static ContentFormat getResponseContentFormat(final HttpServletRequest request, final ContentFormat requestContentFormat) {
        String accept = getAccept(request);

        if (N.isNullOrEmpty(accept)) {
            accept = requestContentFormat.contentType();
        }

        String acceptEncoding = getAcceptEncoding(request);

        // Content encoding should be specified explicitly
        //    if (N.isNullOrEmpty(acceptEncoding)) {
        //        acceptEncoding = request.getHeader(HttpHeaders.Names.CONTENT_ENCODING);
        //    }

        return getContentFormat(accept, acceptEncoding);
    }

    protected static ContentFormat getContentFormat(final String contentType, final String contentEncoding) {
        ContentFormat contentFormat = HttpUtil.getContentFormat(contentType, contentEncoding);

        return contentFormat == null || contentFormat == ContentFormat.NONE ? HttpUtil.DEFAULT_CONTENT_FORMAT : contentFormat;
    }

    /**
     * Gets the input stream.
     *
     * @param request
     * @param contentFormat
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected static InputStream getInputStream(HttpServletRequest request, final ContentFormat contentFormat) throws IOException {
        final InputStream is = new UncloseableInputStream(request.getInputStream());

        return N.defaultIfNull(HttpUtil.wrapInputStream(is, contentFormat), N.emptyInputStream());
    }

    /**
     * Gets the output stream.
     *
     * @param response
     * @param responseContentFormat
     * @param acceptCharset
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected static OutputStream getOutputStream(final HttpServletResponse response, final ContentFormat responseContentFormat, final Charset acceptCharset)
            throws IOException {
        final String responseContentType = HttpUtil.getContentType(responseContentFormat);

        if (N.notNullOrEmpty(responseContentType)) {
            response.setContentType(responseContentType);
        }

        final String contentEncoding = HttpUtil.getContentEncoding(responseContentFormat);

        if (N.notNullOrEmpty(contentEncoding)) {
            response.setHeader(HttpHeaders.Names.CONTENT_ENCODING, contentEncoding);
        }

        if (acceptCharset != null) {
            response.setCharacterEncoding(acceptCharset.name());
        }

        final OutputStream os = new UncloseableOutputStream(response.getOutputStream());

        return HttpUtil.wrapOutputStream(os, responseContentFormat);
    }

    /**
     *
     * @param os
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected static void flush(OutputStream os) throws IOException {
        HttpUtil.flush(os);
    }

    /**
     * Gets the parameter.
     *
     * @param parameterMap
     * @param parameterName
     * @return
     */
    protected static String getParameter(Map<String, String[]> parameterMap, String parameterName) {
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
    protected static String getParameter(Map<String, String[]> parameterMap, String parameterName, String defaultValue) {
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
    protected static <T> T getParameter(Class<T> cls, Map<String, String[]> parameterMap, String parameterName) {
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
    protected static <T> T getParameter(Class<T> cls, Map<String, String[]> parameterMap, String parameterName, T defaultValue) {
        final String[] values = parameterMap.get(parameterName);

        return N.isNullOrEmpty(values) ? defaultValue : N.convert(values[0], cls);
    }
}
