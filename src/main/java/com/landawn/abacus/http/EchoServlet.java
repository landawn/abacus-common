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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.Charsets;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public class EchoServlet extends AbstractHttpServlet {
    private static final long serialVersionUID = -8506987801604338536L;

    private static final Logger logger = LoggerFactory.getLogger(HttpClient.class);

    public static final String IS_GET_FIRST = "isGetFirst";
    private boolean isGetFirst;

    @Override
    public void init() throws ServletException {
        super.init();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        isGetFirst = Boolean.valueOf(getInitParameter(config, IS_GET_FIRST));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        execute(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        execute(req, resp);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) {
        execute(req, resp);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        execute(req, resp);
    }

    protected void execute(HttpServletRequest request, HttpServletResponse response) throws UncheckedIOException {
        final ContentFormat contentFormat = getContentFormat(request);
        Map<String, String[]> paramMap = null;
        Charset charset = Charsets.UTF_8;
        InputStream is = null;
        OutputStream os = null;

        try {
            final Enumeration<String> headerNames = request.getHeaderNames();
            if (headerNames != null) {
                final Map<String, Object> headers = new LinkedHashMap<>();
                String headerName = null;

                while (headerNames.hasMoreElements()) {
                    headerName = headerNames.nextElement();
                    response.setHeader(headerName, request.getHeader(headerName));
                    headers.put(headerName, request.getHeader(headerName));
                }

                charset = HTTP.getCharset(headers);

                logger.info("Request Headers: " + N.toJSON(headers));
            }

            byte[] bytes = N.EMPTY_BYTE_ARRAY;

            if (isGetFirst) {
                paramMap = request.getParameterMap();

                if (N.isNullOrEmpty(paramMap)) {
                    is = getInputStream(request, contentFormat);
                    os = getOutputStream(response, contentFormat);
                }
            } else {
                is = getInputStream(request, contentFormat);
                os = getOutputStream(response, contentFormat);

                paramMap = request.getParameterMap();
            }

            if (N.isNullOrEmpty(paramMap)) {
                bytes = IOUtil.readBytes(is);

                logger.info("Request body: " + new String(bytes, charset));

                IOUtil.write(os, bytes);
            } else {
                final StringBuilder sb = Objectory.createStringBuilder();

                try {
                    int i = 0;
                    String[] parameterValues = null;
                    for (String parameterName : paramMap.keySet()) {
                        parameterValues = paramMap.get(parameterName);

                        if (i++ > 0) {
                            sb.append('&');
                        }

                        sb.append(parameterName);
                        sb.append('=');
                        sb.append(N.isNullOrEmpty(parameterValues) ? N.EMPTY_STRING
                                : (parameterValues.length == 1 ? N.toString(parameterValues[0]) : N.toString(parameterValues)));
                    }

                    final String queryParts = sb.toString();

                    if (N.isNullOrEmpty(queryParts)) {
                        logger.info("Request query: " + N.toJSON(queryParts));
                    }

                    IOUtil.write(os, queryParts);
                } finally {
                    Objectory.recycle(sb);
                }
            }

            if (os != null) {
                flush(os);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(is);
            IOUtil.close(os);
        }
    }
}
