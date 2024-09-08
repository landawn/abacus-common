/*
 * Copyright (C) 2022 HaiYang Li
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
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.Strings;

import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

class CurlInterceptor implements Interceptor {
    static final char DEFAULT_QUOTE_CHAR = '\'';
    private final Consumer<? super String> logHandler;
    private final char quoteChar;

    /**
     *
     *
     * @param logHandler
     */
    public CurlInterceptor(final Consumer<? super String> logHandler) {
        this(DEFAULT_QUOTE_CHAR, logHandler);
    }

    /**
     *
     *
     * @param quoteChar
     * @param logHandler
     */
    public CurlInterceptor(final char quoteChar, final Consumer<? super String> logHandler) {
        this.logHandler = logHandler;
        this.quoteChar = quoteChar;
    }

    /**
     *
     *
     * @param chain
     * @return
     * @throws IOException
     */
    @Override
    public Response intercept(final Chain chain) throws IOException {
        final Request request = chain.request();
        buildCurl(request);

        return chain.proceed(request);
    }

    private void buildCurl(final Request request) throws IOException {
        final Headers headers = request.headers();
        final String httpMethod = request.method();
        final String url = request.url().toString();

        final Map<String, List<String>> httpHeaders = headers.toMultimap();

        String bodyString = null;
        String bodyType = null;

        final RequestBody requestBody = request.body();

        if (requestBody != null) {
            Charset charset = Charset.defaultCharset();
            final String contentType = headers.get(HttpHeaders.Names.CONTENT_TYPE);
            bodyType = requestBody.contentType() == null ? null : requestBody.contentType().toString();

            if (Strings.isNotEmpty(bodyType) && bodyType.indexOf("charset") >= 0) {
                charset = HttpUtil.getCharset(bodyType);
            } else if (Strings.isNotEmpty(contentType) && contentType.indexOf("charset") >= 0) {
                charset = HttpUtil.getCharset(contentType);
            }

            final Buffer buffer = new Buffer();
            requestBody.writeTo(buffer);
            buffer.flush();
            bodyString = buffer.readString(charset);

            IOUtil.close(buffer);
        }

        final String curl = WebUtil.buildCurl(httpMethod, url, httpHeaders, bodyString, bodyType, quoteChar);

        logHandler.accept(curl);
    }

}
