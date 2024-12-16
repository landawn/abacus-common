/*
 * Copyright (C) 2019 HaiYang Li
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

import java.io.File;
import java.io.OutputStream;
import java.io.Writer;
import java.net.Proxy;
import java.net.URL;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLSocketFactory;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.util.ContinuableFuture;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.URLEncodedUtil;
import com.landawn.abacus.util.cs;

/**
 *
 * @see URLEncodedUtil
 * @see HttpHeaders
 * @see com.landawn.abacus.http.v2.HttpRequest
 */
public final class HttpRequest {

    private static final String HTTP_METHOD_STR = "httpMethod";

    private final HttpClient httpClient;

    private HttpSettings settings;

    private Object request;

    private boolean closeHttpClientAfterExecution = false;

    HttpRequest(final HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     *
     * @param httpClient
     * @return
     */
    public static HttpRequest create(final HttpClient httpClient) {
        return new HttpRequest(httpClient);
    }

    /**
     *
     * @param url
     * @return
     */
    public static HttpRequest url(final String url) {
        return url(url, HttpClient.DEFAULT_CONNECTION_TIMEOUT, HttpClient.DEFAULT_READ_TIMEOUT);
    }

    /**
     *
     * @param url
     * @param connectionTimeoutInMillis
     * @param readTimeoutInMillis
     * @return
     */
    public static HttpRequest url(final String url, final long connectionTimeoutInMillis, final long readTimeoutInMillis) {
        return new HttpRequest(HttpClient.create(url, 1, connectionTimeoutInMillis, readTimeoutInMillis)).closeHttpClientAfterExecution(true);
    }

    /**
     * Sets the URL target of this request.
     *
     * @param url
     * @return
     * @throws IllegalArgumentException if the scheme of {@code url} is not {@code http} or {@code https}.
     */
    public static HttpRequest url(final URL url) {
        return url(url, HttpClient.DEFAULT_CONNECTION_TIMEOUT, HttpClient.DEFAULT_READ_TIMEOUT);
    }

    /**
     *
     * @param url
     * @param connectionTimeoutInMillis
     * @param readTimeoutInMillis
     * @return
     */
    public static HttpRequest url(final URL url, final long connectionTimeoutInMillis, final long readTimeoutInMillis) {
        return new HttpRequest(HttpClient.create(url, 1, connectionTimeoutInMillis, readTimeoutInMillis)).closeHttpClientAfterExecution(true);
    }

    HttpRequest closeHttpClientAfterExecution(final boolean b) {
        closeHttpClientAfterExecution = b;

        return this;
    }

    /**
     * Overwrite the existing HTTP settings with specified {@code httpSettings}.
     *
     * @param httpSettings
     * @return
     * @see HttpSettings
     */
    @Beta
    public HttpRequest settings(final HttpSettings httpSettings) {
        checkSettings();

        if (httpSettings != null) {
            N.merge(settings, httpSettings);
        }

        return this;
    }

    /**
     *
     * @param user
     * @param password
     * @return
     * @see HttpHeaders
     */
    public HttpRequest basicAuth(final String user, final Object password) {
        checkSettings();

        settings.basicAuth(user, password);

        return this;
    }

    /**
     * Set http header specified by {@code name/value}.
     * If this {@code HttpSettings} already has any headers with that name, they are all replaced.
     *
     * @param name
     * @param value
     * @return
     * @see HttpSettings#header(String, Object)
     * @see HttpHeaders
     */
    public HttpRequest header(final String name, final Object value) {
        checkSettings();

        settings.header(name, value);

        return this;
    }

    /**
     * Set http headers specified by {@code name1/value1}, {@code name2/value2}.
     * If this {@code HttpSettings} already has any headers with that name, they are all replaced.
     *
     * @param name1
     * @param value1
     * @param name2
     * @param value2
     * @return
     * @see HttpSettings#headers(String, Object, String, Object)
     * @see HttpHeaders
     */
    public HttpRequest headers(final String name1, final Object value1, final String name2, final Object value2) {
        checkSettings();

        settings.headers(name1, value1, name2, value2);

        return this;
    }

    /**
     * Set http headers specified by {@code name1/value1}, {@code name2/value2}, {@code name3/value3}.
     * If this {@code HttpSettings} already has any headers with that name, they are all replaced.
     *
     * @param name1
     * @param value1
     * @param name2
     * @param value2
     * @param name3
     * @param value3
     * @return
     * @see HttpSettings#headers(String, Object, String, Object, String, Object)
     * @see HttpHeaders
     */
    public HttpRequest headers(final String name1, final Object value1, final String name2, final Object value2, final String name3, final Object value3) {
        checkSettings();

        settings.headers(name1, value1, name2, value2, name3, value3);

        return this;
    }

    /**
     * Set http headers specified by the key/value entities from {@code Map}.
     * If this {@code HttpSettings} already has any headers with that name, they are all replaced.
     *
     * @param headers
     * @return
     * @see HttpSettings#headers(Map)
     * @see HttpHeaders
     */
    public HttpRequest headers(final Map<String, ?> headers) {
        checkSettings();

        settings.headers(headers);

        return this;
    }

    /**
     * Removes all headers on this {@code HttpSettings} and adds {@code headers}.
     *
     * @param headers
     * @return
     * @see HttpSettings#headers(HttpHeaders)
     * @see HttpHeaders
     */
    public HttpRequest headers(final HttpHeaders headers) {
        checkSettings();

        settings.headers(headers);

        return this;
    }

    /**
     *
     * @param connectionTimeout
     * @return
     */
    public HttpRequest connectionTimeout(final long connectionTimeout) {
        checkSettings();

        settings.setConnectionTimeout(connectionTimeout);

        return this;
    }

    /**
     *
     * @param connectionTimeout
     * @return
     */
    public HttpRequest connectionTimeout(final Duration connectionTimeout) {
        checkSettings();

        settings.setConnectionTimeout(connectionTimeout.toMillis());

        return this;
    }

    /**
     *
     * @param readTimeout
     * @return
     */
    public HttpRequest readTimeout(final long readTimeout) {
        checkSettings();

        settings.setReadTimeout(readTimeout);

        return this;
    }

    /**
     *
     * @param readTimeout
     * @return
     */
    public HttpRequest readTimeout(final Duration readTimeout) {
        checkSettings();

        settings.setReadTimeout(readTimeout.toMillis());

        return this;
    }

    /**
     *
     * @param useCaches
     * @return
     */
    public HttpRequest useCaches(final boolean useCaches) {
        checkSettings();

        settings.setUseCaches(useCaches);

        return this;
    }

    /**
     *
     * @param sslSocketFactory
     * @return
     */
    public HttpRequest sslSocketFactory(final SSLSocketFactory sslSocketFactory) {
        checkSettings();

        settings.setSSLSocketFactory(sslSocketFactory);

        return this;
    }

    /**
     *
     * @param proxy
     * @return
     */
    public HttpRequest proxy(final Proxy proxy) {
        checkSettings();

        settings.setProxy(proxy);

        return this;
    }

    /**
     * Set query parameters for {@code GET} or {@code DELETE} request.
     *
     * @param query
     * @return
     */
    public HttpRequest query(final String query) {
        request = query;

        return this;
    }

    /**
     * Set query parameters for {@code GET} or {@code DELETE} request.
     *
     * @param queryParams
     * @return
     */
    public HttpRequest query(final Map<String, ?> queryParams) {
        request = queryParams;

        return this;
    }

    /**
     *
     * @param json
     * @return
     */
    public HttpRequest jsonBody(final String json) {
        setContentType(HttpHeaders.Values.APPLICATION_JSON);

        request = json;

        return this;
    }

    /**
     *
     * @param obj
     * @return
     */
    public HttpRequest jsonBody(final Object obj) {
        setContentType(HttpHeaders.Values.APPLICATION_JSON);

        request = N.toJson(obj);

        return this;
    }

    /**
     *
     * @param xml
     * @return
     */
    public HttpRequest xmlBody(final String xml) {
        setContentType(HttpHeaders.Values.APPLICATION_XML);

        request = xml;

        return this;
    }

    /**
     *
     * @param obj
     * @return
     */
    public HttpRequest xmlBody(final Object obj) {
        setContentType(HttpHeaders.Values.APPLICATION_XML);

        request = N.toXml(obj);

        return this;
    }

    /**
     *
     * @param formBodyByMap
     * @return
     */
    public HttpRequest formBody(final Map<?, ?> formBodyByMap) {
        setContentType(HttpHeaders.Values.APPLICATION_URL_ENCODED);

        request = formBodyByMap;

        return this;
    }

    /**
     *
     * @param formBodyByBean
     * @return
     */
    public HttpRequest formBody(final Object formBodyByBean) {
        setContentType(HttpHeaders.Values.APPLICATION_URL_ENCODED);

        request = formBodyByBean;

        return this;
    }

    private void setContentType(final String contentType) {
        checkSettings();

        if (Strings.isEmpty(settings.getContentType()) || !Strings.containsIgnoreCase(settings.getContentType(), contentType)) {
            settings.header(HttpHeaders.Names.CONTENT_TYPE, contentType);
        }
    }

    /**
     *
     * @param requestBody
     * @return
     */
    public HttpRequest body(final Object requestBody) {
        request = requestBody;

        return this;
    }

    /**
     *
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public HttpResponse get() throws UncheckedIOException {
        return get(HttpResponse.class);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T get(final Class<T> resultClass) throws UncheckedIOException {
        return execute(HttpMethod.GET, resultClass);
    }

    /**
     *
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public HttpResponse post() throws UncheckedIOException {
        return post(HttpResponse.class);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T post(final Class<T> resultClass) throws UncheckedIOException {
        return execute(HttpMethod.POST, resultClass);
    }

    /**
     *
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public HttpResponse put() throws UncheckedIOException {
        return put(HttpResponse.class);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T put(final Class<T> resultClass) throws UncheckedIOException {
        return execute(HttpMethod.PUT, resultClass);
    }

    // TODO HTTP METHOD PATCH is not supported by HttpURLConnection.
    //    /**
    //     *
    //     * @return
    //     * @throws UncheckedIOException the unchecked IO exception
    //     */
    //    public HttpResponse patch() throws UncheckedIOException {
    //        return patch(HttpResponse.class);
    //    }
    //
    //    /**
    //     *
    //     * @param <T>
    //     * @param resultClass
    //     * @return
    //     * @throws UncheckedIOException the unchecked IO exception
    //     */
    //    public <T> T patch(final Class<T> resultClass) throws UncheckedIOException {
    //        return execute(HttpMethod.PATCH, resultClass);
    //    }

    /**
     *
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public HttpResponse delete() throws UncheckedIOException {
        return delete(HttpResponse.class);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T delete(final Class<T> resultClass) throws UncheckedIOException {
        return execute(HttpMethod.DELETE, resultClass);
    }

    /**
     *
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public HttpResponse head() throws UncheckedIOException {
        return head(HttpResponse.class);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    <T> T head(final Class<T> resultClass) throws UncheckedIOException {
        return execute(HttpMethod.HEAD, resultClass);
    }

    /**
     *
     * @param httpMethod
     * @return
     * @throws UncheckedIOException
     */
    @Beta
    public HttpResponse execute(final HttpMethod httpMethod) throws UncheckedIOException {
        return execute(httpMethod, HttpResponse.class);
    }

    /**
     *
     * @param <T>
     * @param httpMethod
     * @param resultClass
     * @return
     * @throws IllegalArgumentException
     * @throws UncheckedIOException the unchecked IO exception
     */
    @Beta
    public <T> T execute(final HttpMethod httpMethod, final Class<T> resultClass) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(httpMethod, HTTP_METHOD_STR);

        try {
            return httpClient.execute(httpMethod, request, checkSettings(), resultClass);
        } finally {
            doAfterExecution();
        }
    }

    /**
     *
     * @param httpMethod
     * @param output
     * @throws IllegalArgumentException
     * @throws UncheckedIOException
     */
    @Beta
    public void execute(final HttpMethod httpMethod, final File output) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(httpMethod, HTTP_METHOD_STR);

        try {
            httpClient.execute(httpMethod, request, checkSettings(), output);
        } finally {
            doAfterExecution();
        }
    }

    /**
     *
     * @param httpMethod
     * @param output
     * @throws IllegalArgumentException
     * @throws UncheckedIOException
     */
    @Beta
    public void execute(final HttpMethod httpMethod, final OutputStream output) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(httpMethod, HTTP_METHOD_STR);

        try {
            httpClient.execute(httpMethod, request, checkSettings(), output);
        } finally {
            doAfterExecution();
        }
    }

    /**
     *
     * @param httpMethod
     * @param output
     * @throws IllegalArgumentException
     * @throws UncheckedIOException
     */
    @Beta
    public void execute(final HttpMethod httpMethod, final Writer output) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(httpMethod, HTTP_METHOD_STR);

        try {
            httpClient.execute(httpMethod, request, checkSettings(), output);
        } finally {
            doAfterExecution();
        }
    }

    void doAfterExecution() {
        if (closeHttpClientAfterExecution) {
            httpClient.close();
        }
    }

    public ContinuableFuture<HttpResponse> asyncGet() {
        return asyncGet(HttpResponse.class);
    }

    /**
     *
     * @param executor
     * @return
     */
    public ContinuableFuture<HttpResponse> asyncGet(final Executor executor) {
        return asyncGet(HttpResponse.class, executor);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @return
     */
    public <T> ContinuableFuture<T> asyncGet(final Class<T> resultClass) {
        return asyncExecute(HttpMethod.GET, resultClass);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @param executor
     * @return
     */
    public <T> ContinuableFuture<T> asyncGet(final Class<T> resultClass, final Executor executor) {
        return asyncExecute(HttpMethod.GET, resultClass, executor);
    }

    public ContinuableFuture<HttpResponse> asyncPost() {
        return asyncPost(HttpResponse.class);
    }

    /**
     *
     * @param executor
     * @return
     */
    public ContinuableFuture<HttpResponse> asyncPost(final Executor executor) {
        return asyncPost(HttpResponse.class, executor);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @return
     */
    public <T> ContinuableFuture<T> asyncPost(final Class<T> resultClass) {
        return asyncExecute(HttpMethod.POST, resultClass);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @param executor
     * @return
     */
    public <T> ContinuableFuture<T> asyncPost(final Class<T> resultClass, final Executor executor) {
        return asyncExecute(HttpMethod.POST, resultClass, executor);
    }

    public ContinuableFuture<HttpResponse> asyncPut() {
        return asyncPut(HttpResponse.class);
    }

    /**
     *
     * @param executor
     * @return
     */
    public ContinuableFuture<HttpResponse> asyncPut(final Executor executor) {
        return asyncPut(HttpResponse.class, executor);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @return
     */
    public <T> ContinuableFuture<T> asyncPut(final Class<T> resultClass) {
        return asyncExecute(HttpMethod.PUT, resultClass);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @param executor
     * @return
     */
    public <T> ContinuableFuture<T> asyncPut(final Class<T> resultClass, final Executor executor) {
        return asyncExecute(HttpMethod.PUT, resultClass, executor);
    }

    // TODO HTTP METHOD PATCH is not supported by HttpURLConnection.
    //    /**
    //     *
    //     * @return
    //     */
    //    public ContinuableFuture<HttpResponse> asyncPatch() {
    //        return asyncPatch(HttpResponse.class);
    //    }
    //
    //    /**
    //     *
    //     * @param executor
    //     * @return
    //     */
    //    public ContinuableFuture<HttpResponse> asyncPatch(final Executor executor) {
    //        return asyncPatch(HttpResponse.class, executor);
    //    }
    //
    //    /**
    //     *
    //     * @param <T>
    //     * @param resultClass
    //     * @return
    //     */
    //    public <T> ContinuableFuture<T> asyncPatch(final Class<T> resultClass) {
    //        return asyncExecute(HttpMethod.PATCH, resultClass);
    //    }
    //
    //    /**
    //     *
    //     * @param <T>
    //     * @param executor
    //     * @param resultClass
    //     * @return
    //     */
    //    public <T> ContinuableFuture<T> asyncPatch(final Executor executor, final Class<T> resultClass) {
    //        return asyncExecute(HttpMethod.PATCH, executor, resultClass);
    //    }

    public ContinuableFuture<HttpResponse> asyncDelete() {
        return asyncDelete(HttpResponse.class);
    }

    /**
     *
     * @param executor
     * @return
     */
    public ContinuableFuture<HttpResponse> asyncDelete(final Executor executor) {
        return asyncDelete(HttpResponse.class, executor);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @return
     */
    public <T> ContinuableFuture<T> asyncDelete(final Class<T> resultClass) {
        return asyncExecute(HttpMethod.DELETE, resultClass);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @param executor
     * @return
     */
    public <T> ContinuableFuture<T> asyncDelete(final Class<T> resultClass, final Executor executor) {
        return asyncExecute(HttpMethod.DELETE, resultClass, executor);
    }

    public ContinuableFuture<HttpResponse> asyncHead() {
        return asyncHead(HttpResponse.class);
    }

    /**
     *
     * @param executor
     * @return
     */
    public ContinuableFuture<HttpResponse> asyncHead(final Executor executor) {
        return asyncHead(executor, HttpResponse.class);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @return
     */
    <T> ContinuableFuture<T> asyncHead(final Class<T> resultClass) {
        return asyncExecute(HttpMethod.HEAD, resultClass);
    }

    /**
     *
     * @param executor
     * @param resultClass
     * @param <T>
     * @return
     */
    <T> ContinuableFuture<T> asyncHead(final Executor executor, final Class<T> resultClass) {
        return asyncExecute(HttpMethod.HEAD, resultClass, executor);
    }

    /**
     *
     * @param httpMethod
     * @return
     */
    @Beta
    public ContinuableFuture<HttpResponse> asyncExecute(final HttpMethod httpMethod) {
        return asyncExecute(httpMethod, HttpResponse.class);
    }

    /**
     *
     * @param httpMethod
     * @param executor
     * @return
     */
    @Beta
    public ContinuableFuture<HttpResponse> asyncExecute(final HttpMethod httpMethod, final Executor executor) {
        return asyncExecute(httpMethod, HttpResponse.class, executor);
    }

    /**
     *
     * @param <T>
     * @param httpMethod
     * @param resultClass
     * @return
     * @throws IllegalArgumentException
     */
    @Beta
    public <T> ContinuableFuture<T> asyncExecute(final HttpMethod httpMethod, final Class<T> resultClass) throws IllegalArgumentException {
        N.checkArgNotNull(httpMethod, HTTP_METHOD_STR);

        final Callable<T> cmd = () -> execute(httpMethod, resultClass);
        return httpClient._asyncExecutor.execute(cmd);

    }

    /**
     *
     * @param <T>
     * @param httpMethod
     * @param resultClass
     * @param executor
     * @return
     * @throws IllegalArgumentException
     */
    @Beta
    public <T> ContinuableFuture<T> asyncExecute(final HttpMethod httpMethod, final Class<T> resultClass, final Executor executor)
            throws IllegalArgumentException {
        N.checkArgNotNull(httpMethod, HTTP_METHOD_STR);

        final Callable<T> cmd = () -> execute(httpMethod, resultClass);
        return execute(cmd, executor);
    }

    /**
     *
     * @param httpMethod
     * @param output
     * @return
     */
    @Beta
    public ContinuableFuture<Void> asyncExecute(final HttpMethod httpMethod, final File output) {
        final Callable<Void> cmd = () -> {
            execute(httpMethod, output);

            return null;
        };

        return httpClient._asyncExecutor.execute(cmd);
    }

    /**
     *
     * @param httpMethod
     * @param output
     * @param executor
     * @return
     */
    @Beta
    public ContinuableFuture<Void> asyncExecute(final HttpMethod httpMethod, final File output, final Executor executor) {
        final Callable<Void> cmd = () -> {
            execute(httpMethod, output);

            return null;
        };

        return execute(cmd, executor);
    }

    /**
     *
     * @param httpMethod
     * @param output
     * @return
     */
    @Beta
    public ContinuableFuture<Void> asyncExecute(final HttpMethod httpMethod, final OutputStream output) {
        final Callable<Void> cmd = () -> {
            execute(httpMethod, output);

            return null;
        };

        return httpClient._asyncExecutor.execute(cmd);
    }

    /**
     *
     * @param httpMethod
     * @param output
     * @param executor
     * @return
     */
    @Beta
    public ContinuableFuture<Void> asyncExecute(final HttpMethod httpMethod, final OutputStream output, final Executor executor) {
        final Callable<Void> cmd = () -> {
            execute(httpMethod, output);

            return null;
        };

        return execute(cmd, executor);
    }

    /**
     *
     * @param httpMethod
     * @param output
     * @return
     */
    @Beta
    public ContinuableFuture<Void> asyncExecute(final HttpMethod httpMethod, final Writer output) {
        final Callable<Void> cmd = () -> {
            execute(httpMethod, output);

            return null;
        };

        return httpClient._asyncExecutor.execute(cmd);
    }

    /**
     *
     * @param httpMethod
     * @param output
     * @param executor
     * @return
     */
    @Beta
    public ContinuableFuture<Void> asyncExecute(final HttpMethod httpMethod, final Writer output, final Executor executor) {
        final Callable<Void> cmd = () -> {
            execute(httpMethod, output);

            return null;
        };

        return execute(cmd, executor);
    }

    /**
     *
     * @param <R>
     * @param cmd
     * @param executor
     * @return
     */
    <R> ContinuableFuture<R> execute(final Callable<R> cmd, final Executor executor) {
        N.checkArgNotNull(executor, cs.executor);

        return N.asyncExecute(cmd, executor);
    }

    /**
     * Check settings.
     */
    HttpSettings checkSettings() {
        if (settings == null) {
            settings = new HttpSettings();
        }

        return settings;
    }
}
