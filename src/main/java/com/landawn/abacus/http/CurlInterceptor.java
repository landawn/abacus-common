/*
 * Copyright (C) 2022 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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

/**
 * An OkHttp interceptor that logs HTTP requests as cURL commands.
 * This interceptor captures the details of outgoing HTTP requests and formats them as 
 * executable cURL commands, which can be useful for debugging and reproducing requests.
 * 
 * <p>The interceptor processes the request headers, method, URL, and body content to generate
 * a complete cURL command that can be executed from the command line.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * OkHttpClient client = new OkHttpClient.Builder()
 *     .addInterceptor(new CurlInterceptor(curl -> System.out.println(curl)))
 *     .build();
 * }</pre>
 * 
 */
class CurlInterceptor implements Interceptor {
    static final char DEFAULT_QUOTE_CHAR = '\'';
    private final Consumer<? super String> logHandler;
    private final char quoteChar;

    /**
     * Creates a new CurlInterceptor with the default single quote character for shell escaping.
     * 
     * @param logHandler A consumer function that handles the generated cURL command string.
     *                   This is typically used to log or store the command.
     */
    public CurlInterceptor(final Consumer<? super String> logHandler) {
        this(DEFAULT_QUOTE_CHAR, logHandler);
    }

    /**
     * Creates a new CurlInterceptor with a specified quote character for shell escaping.
     * 
     * @param quoteChar The character to use for quoting values in the cURL command.
     *                  Can be either single quote (') or double quote (") depending on shell requirements.
     * @param logHandler A consumer function that handles the generated cURL command string.
     *                   This is typically used to log or store the command.
     */
    public CurlInterceptor(final char quoteChar, final Consumer<? super String> logHandler) {
        this.logHandler = logHandler;
        this.quoteChar = quoteChar;
    }

    /**
     * Intercepts the HTTP request and generates a cURL command representation.
     * This method is called automatically by OkHttp for each request when this interceptor is registered.
     * 
     * @param chain The interceptor chain containing the request to be processed
     * @return The response from the next interceptor in the chain
     * @throws IOException If an I/O error occurs during request processing
     */
    @Override
    public Response intercept(final Chain chain) throws IOException {
        final Request request = chain.request();
        buildCurl(request);

        return chain.proceed(request);
    }

    /**
     * Builds a cURL command string from the given HTTP request.
     * This method extracts all relevant information from the request including:
     * - HTTP method (GET, POST, PUT, etc.)
     * - URL
     * - Headers
     * - Request body (if present)
     * 
     * The generated cURL command can be directly executed in a terminal to reproduce the request.
     * 
     * @param request The HTTP request to convert to a cURL command
     * @throws IOException If an error occurs while reading the request body
     */
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
            //noinspection DataFlowIssue
            bodyType = requestBody.contentType() == null ? null : requestBody.contentType().toString();

            if (Strings.isNotEmpty(bodyType) && bodyType.contains("charset")) {
                charset = HttpUtil.getCharset(bodyType);
            } else if (Strings.isNotEmpty(contentType) && contentType.contains("charset")) {
                charset = HttpUtil.getCharset(contentType);
            }

            final Buffer buffer = new Buffer();
            try {
                requestBody.writeTo(buffer);
                buffer.flush();
                bodyString = buffer.readString(charset);
            } finally {
                IOUtil.close(buffer);
            }
        }

        final String curl = WebUtil.buildCurl(httpMethod, url, httpHeaders, bodyString, bodyType, quoteChar);

        logHandler.accept(curl);
    }

}
