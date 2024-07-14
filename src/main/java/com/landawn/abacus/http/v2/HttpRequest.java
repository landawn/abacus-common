/*
 * Copyright (C) 2023 HaiYang Li
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

package com.landawn.abacus.http.v2;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.PushPromiseHandler;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.http.HttpHeaders;
import com.landawn.abacus.http.HttpMethod;
import com.landawn.abacus.http.HttpUtil;
import com.landawn.abacus.util.Charsets;
import com.landawn.abacus.util.ExceptionUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.URLEncodedUtil;

/**
 * @see URLEncodedUtil
 * @see HttpHeaders
 * @author Haiyang Li
 */
public final class HttpRequest {

    private static final String HTTP_METHOD_STR = "httpMethod";

    private static final HttpClient DEFAULT_HTTP_CLIENT = HttpClient.newHttpClient();

    private final String url;
    private final URI uri;
    private final HttpClient httpClient;
    private final java.net.http.HttpRequest.Builder requestBuilder;

    private Object query;

    private HttpClient.Builder clientBuilder;
    private BodyPublisher bodyPublisher;

    private boolean requireNewClient = false;

    private boolean closeHttpClientAfterExecution = false;

    HttpRequest(final String url, final URI uri, final HttpClient httpClient, final HttpClient.Builder clientBuilder,
            final java.net.http.HttpRequest.Builder requestBuilder) {
        N.checkArgument(!(Strings.isEmpty(url) && uri == null), "'uri' or 'url' can't be null or empty");

        this.url = url;
        this.uri = uri;
        this.httpClient = httpClient;
        this.clientBuilder = clientBuilder;
        this.requestBuilder = N.checkArgNotNull(requestBuilder);
    }

    /**
     * Sets the URI target of this request.
     *
     * @param url
     * @param httpClient
     * @return
     * @throws IllegalArgumentException if the scheme of {@code url} is not {@code http} or {@code https}.
     */
    public static HttpRequest create(String url, final HttpClient httpClient) {
        return new HttpRequest(url, null, httpClient, null, java.net.http.HttpRequest.newBuilder()).closeHttpClientAfterExecution(false);
    }

    /**
     * Sets the URI target of this request.
     *
     * @param url
     * @param httpClient
     * @return
     * @throws IllegalArgumentException if the scheme of {@code url} is not {@code http} or {@code https}.
     */
    public static HttpRequest create(URL url, final HttpClient httpClient) {
        return new HttpRequest(url.toString(), null, httpClient, null, java.net.http.HttpRequest.newBuilder()).closeHttpClientAfterExecution(false);
    }

    /**
     * Sets the URI target of this request.
     *
     * @param uri
     * @param httpClient
     * @return
     * @throws IllegalArgumentException if the scheme of {@code url} is not {@code http} or {@code https}.
     */
    public static HttpRequest create(URI uri, final HttpClient httpClient) {
        return new HttpRequest(null, uri, httpClient, null, java.net.http.HttpRequest.newBuilder()).closeHttpClientAfterExecution(false);
    }

    /**
     *
     * @param url
     * @return
     */
    public static HttpRequest url(final String url) {
        return new HttpRequest(url, null, DEFAULT_HTTP_CLIENT, null, java.net.http.HttpRequest.newBuilder()).closeHttpClientAfterExecution(false);
    }

    /**
     *
     * @param url
     * @param connectionTimeoutInMillis
     * @param readTimeoutInMillis
     * @return
     */
    public static HttpRequest url(final String url, final long connectionTimeoutInMillis, final long readTimeoutInMillis) {
        return new HttpRequest(url, null, null, HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connectionTimeoutInMillis)),
                java.net.http.HttpRequest.newBuilder().timeout(Duration.ofMillis(readTimeoutInMillis))).closeHttpClientAfterExecution(true);
    }

    /**
     * Sets the URI target of this request.
     *
     * @param url
     * @return
     * @throws IllegalArgumentException if the scheme of {@code url} is not {@code http} or {@code https}.
     */
    public static HttpRequest url(URL url) {
        return new HttpRequest(url.toString(), null, DEFAULT_HTTP_CLIENT, null, java.net.http.HttpRequest.newBuilder()).closeHttpClientAfterExecution(false);
    }

    /**
     *
     *
     * @param url
     * @param connectionTimeoutInMillis
     * @param readTimeoutInMillis
     * @return
     */
    public static HttpRequest url(final URL url, final long connectionTimeoutInMillis, final long readTimeoutInMillis) {
        return new HttpRequest(url.toString(), null, null, HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connectionTimeoutInMillis)),
                java.net.http.HttpRequest.newBuilder().timeout(Duration.ofMillis(readTimeoutInMillis))).closeHttpClientAfterExecution(true);
    }

    /**
     * Sets the URI target of this request.
     *
     * @param uri
     * @return
     * @throws IllegalArgumentException if the scheme of {@code url} is not {@code http} or {@code https}.
     */
    public static HttpRequest url(URI uri) {
        return new HttpRequest(null, uri, DEFAULT_HTTP_CLIENT, null, java.net.http.HttpRequest.newBuilder()).closeHttpClientAfterExecution(false);
    }

    /**
     *
     *
     * @param uri
     * @param connectionTimeoutInMillis
     * @param readTimeoutInMillis
     * @return
     */
    public static HttpRequest url(final URI uri, final long connectionTimeoutInMillis, final long readTimeoutInMillis) {
        return new HttpRequest(null, uri, null, HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connectionTimeoutInMillis)),
                java.net.http.HttpRequest.newBuilder().timeout(Duration.ofMillis(readTimeoutInMillis))).closeHttpClientAfterExecution(true);
    }

    HttpRequest closeHttpClientAfterExecution(boolean b) {
        this.closeHttpClientAfterExecution = b;

        return this;
    }

    /**
     *
     * @param connectTimeout
     * @return
     */
    public HttpRequest connectTimeout(final Duration connectTimeout) {
        initClientBuilder();

        clientBuilder.connectTimeout(connectTimeout);

        return this;
    }

    private void initClientBuilder() {
        if (clientBuilder == null) {
            clientBuilder = HttpClient.newBuilder();
        }

        if (httpClient != null && requireNewClient == false) {
            httpClient.cookieHandler().ifPresent(it -> clientBuilder.cookieHandler(it));
            httpClient.connectTimeout().ifPresent(it -> clientBuilder.connectTimeout(it));
            httpClient.proxy().ifPresent(it -> clientBuilder.proxy(it));
            httpClient.authenticator().ifPresent(it -> clientBuilder.authenticator(it));
            httpClient.executor().ifPresent(it -> clientBuilder.executor(it));

            if (httpClient.followRedirects() != null) {
                clientBuilder.followRedirects(httpClient.followRedirects());
            }

            if (httpClient.sslContext() != null) {
                clientBuilder.sslContext(httpClient.sslContext());
            }

            if (httpClient.sslParameters() != null) {
                clientBuilder.sslParameters(httpClient.sslParameters());
            }

            if (httpClient.version() != null) {
                clientBuilder.version(httpClient.version());
            }

            requireNewClient = true;
        }
    }

    /**
     *
     * @param readTimeout
     * @return
     */
    public HttpRequest readTimeout(final Duration readTimeout) {
        requestBuilder.timeout(readTimeout);

        return this;
    }

    /**
     *
     * @param authenticator
     * @return
     */
    public HttpRequest authenticator(Authenticator authenticator) {
        initClientBuilder();

        clientBuilder.authenticator(authenticator);

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
        header(HttpHeaders.Names.AUTHORIZATION, "Basic " + Strings.base64Encode((user + ":" + password).getBytes(Charsets.UTF_8)));

        return this;
    }

    /**
     * Set http header specified by {@code name/value}.
     * If this {@code HttpSettings} already has any headers with that name, they are all replaced.
     *
     * @param name
     * @param value
     * @return
     * @see HttpHeaders
     */
    public HttpRequest header(String name, Object value) {
        requestBuilder.header(name, HttpHeaders.valueOf(value));

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
     * @see HttpHeaders
     */
    public HttpRequest headers(String name1, Object value1, String name2, Object value2) {
        return header(name1, value1).header(name2, value2);
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
     * @see HttpHeaders
     */
    public HttpRequest headers(String name1, Object value1, String name2, Object value2, String name3, Object value3) {
        return header(name1, value1).header(name2, value2).header(name3, value3);
    }

    /**
     * Set http headers specified by the key/value entities from {@code Map}.
     * If this {@code HttpSettings} already has any headers with that name, they are all replaced.
     *
     * @param headers
     * @return
     * @see HttpHeaders
     */
    public HttpRequest headers(Map<String, ?> headers) {
        if (N.notEmpty(headers)) {
            for (Map.Entry<String, ?> entry : headers.entrySet()) {
                header(entry.getKey(), entry.getValue());
            }
        }

        return this;
    }

    /**
     * Removes all headers on this {@code HttpSettings} and adds {@code headers}.
     *
     * @param headers
     * @return
     * @see HttpHeaders
     */
    public HttpRequest headers(HttpHeaders headers) {
        if (headers != null && !headers.isEmpty()) {
            headers.forEach(this::header);
        }

        return this;
    }

    /**
     * Set query parameters for {@code GET} or {@code DELETE} request.
     *
     * @param query
     * @return
     */
    public HttpRequest query(final String query) {
        this.query = query;

        return this;
    }

    /**
     * Set query parameters for {@code GET} or {@code DELETE} request.
     *
     * @param queryParams
     * @return
     */
    public HttpRequest query(final Map<String, ?> queryParams) {
        this.query = queryParams;

        return this;
    }

    /**
     *
     * @param json
     * @return
     */
    public HttpRequest jsonBody(final String json) {
        setContentType(HttpHeaders.Values.APPLICATION_JSON);

        this.bodyPublisher = BodyPublishers.ofString(json);

        return this;
    }

    /**
     *
     * @param obj
     * @return
     */
    public HttpRequest jsonBody(final Object obj) {
        setContentType(HttpHeaders.Values.APPLICATION_JSON);

        this.bodyPublisher = BodyPublishers.ofString(N.toJson(obj));

        return this;
    }

    /**
     *
     * @param xml
     * @return
     */
    public HttpRequest xmlBody(final String xml) {
        setContentType(HttpHeaders.Values.APPLICATION_XML);

        this.bodyPublisher = BodyPublishers.ofString(xml);

        return this;
    }

    /**
     *
     * @param obj
     * @return
     */
    public HttpRequest xmlBody(final Object obj) {
        setContentType(HttpHeaders.Values.APPLICATION_XML);

        this.bodyPublisher = BodyPublishers.ofString(N.toXml(obj));

        return this;
    }

    /**
     *
     * @param formBodyByMap
     * @return
     */
    public HttpRequest formBody(final Map<?, ?> formBodyByMap) {
        setContentType(HttpHeaders.Values.APPLICATION_URL_ENCODED);

        this.bodyPublisher = BodyPublishers.ofString(URLEncodedUtil.encode(formBodyByMap));

        return this;
    }

    /**
     *
     * @param formBodyByBean
     * @return
     */
    public HttpRequest formBody(final Object formBodyByBean) {
        setContentType(HttpHeaders.Values.APPLICATION_URL_ENCODED);

        this.bodyPublisher = BodyPublishers.ofString(URLEncodedUtil.encode(formBodyByBean));

        return this;
    }

    private void setContentType(String contentType) {
        header(HttpHeaders.Names.CONTENT_TYPE, contentType);
    }

    /**
     *
     * @param bodyPublisher
     * @return
     */
    public HttpRequest body(final BodyPublisher bodyPublisher) {
        this.bodyPublisher = bodyPublisher;

        return this;
    }

    /**
     *
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public HttpResponse<String> get() throws UncheckedIOException {
        return get(BodyHandlers.ofString());
    }

    /**
     *
     * @param <T>
     * @param responseBodyHandler
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> HttpResponse<T> get(final HttpResponse.BodyHandler<T> responseBodyHandler) throws UncheckedIOException {
        return execute(HttpMethod.GET, responseBodyHandler);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T get(final Class<T> resultClass) throws UncheckedIOException {
        final BodyHandler<?> responseBodyHandler = createResponseBodyHandler(resultClass);

        return getBody(get(responseBodyHandler), resultClass);
    }

    /**
     *
     * @return
     */
    public HttpResponse<String> post() {
        return post(BodyHandlers.ofString());
    }

    /**
     *
     * @param <T>
     * @param responseBodyHandler
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> HttpResponse<T> post(final HttpResponse.BodyHandler<T> responseBodyHandler) throws UncheckedIOException {
        return execute(HttpMethod.POST, responseBodyHandler);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T post(final Class<T> resultClass) throws UncheckedIOException {
        final BodyHandler<?> responseBodyHandler = createResponseBodyHandler(resultClass);

        return getBody(post(responseBodyHandler), resultClass);
    }

    /**
     *
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public HttpResponse<String> put() throws UncheckedIOException {
        return put(BodyHandlers.ofString());
    }

    /**
     *
     * @param <T>
     * @param responseBodyHandler
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> HttpResponse<T> put(final HttpResponse.BodyHandler<T> responseBodyHandler) throws UncheckedIOException {
        return execute(HttpMethod.PUT, responseBodyHandler);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T put(final Class<T> resultClass) throws UncheckedIOException {
        final BodyHandler<?> responseBodyHandler = createResponseBodyHandler(resultClass);

        return getBody(put(responseBodyHandler), resultClass);
    }

    /**
     *
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public HttpResponse<String> patch() throws UncheckedIOException {
        return patch(BodyHandlers.ofString());
    }

    /**
     *
     * @param <T>
     * @param responseBodyHandler
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> HttpResponse<T> patch(final HttpResponse.BodyHandler<T> responseBodyHandler) throws UncheckedIOException {
        return execute(HttpMethod.PATCH, responseBodyHandler);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T patch(final Class<T> resultClass) throws UncheckedIOException {
        final BodyHandler<?> responseBodyHandler = createResponseBodyHandler(resultClass);

        return getBody(patch(responseBodyHandler), resultClass);
    }

    /**
     *
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public HttpResponse<String> delete() throws UncheckedIOException {
        return delete(BodyHandlers.ofString());
    }

    /**
     *
     * @param <T>
     * @param responseBodyHandler
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> HttpResponse<T> delete(final HttpResponse.BodyHandler<T> responseBodyHandler) throws UncheckedIOException {
        return execute(HttpMethod.DELETE, responseBodyHandler);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T delete(final Class<T> resultClass) throws UncheckedIOException {
        final BodyHandler<?> responseBodyHandler = createResponseBodyHandler(resultClass);

        return getBody(delete(responseBodyHandler), resultClass);
    }

    /**
     *
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public HttpResponse<Void> head() throws UncheckedIOException {
        return head(BodyHandlers.discarding());
    }

    /**
     *
     * @param <T>
     * @param responseBodyHandler
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    private <T> HttpResponse<T> head(final HttpResponse.BodyHandler<T> responseBodyHandler) throws UncheckedIOException {
        return execute(HttpMethod.HEAD, responseBodyHandler);
    }

    /**
     *
     * @param httpMethod
     * @return
     * @throws UncheckedIOException
     */
    @Beta
    public HttpResponse<String> execute(final HttpMethod httpMethod) throws UncheckedIOException {
        return execute(httpMethod, BodyHandlers.ofString());
    }

    /**
     * 
     *
     * @param <T> 
     * @param httpMethod 
     * @param responseBodyHandler 
     * @return 
     * @throws IllegalArgumentException 
     * @throws UncheckedIOException the unchecked IO exception
     */
    @Beta
    public <T> HttpResponse<T> execute(final HttpMethod httpMethod, final HttpResponse.BodyHandler<T> responseBodyHandler)
            throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(httpMethod, HTTP_METHOD_STR);

        final HttpClient httpClientToUse = checkUrlAndHttpClient();

        try {
            return httpClientToUse.send(requestBuilder.method(httpMethod.name(), checkBodyPublisher()).build(), responseBodyHandler);
        } catch (IOException | InterruptedException e) {
            throw ExceptionUtil.toRuntimeException(e);
        } finally {
            doAfterExecution(httpClientToUse);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param httpMethod
     * @param resultClass
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    @Beta
    public <T> T execute(final HttpMethod httpMethod, final Class<T> resultClass) throws UncheckedIOException {
        final BodyHandler<?> responseBodyHandler = createResponseBodyHandler(resultClass);

        return getBody(execute(httpMethod, responseBodyHandler), resultClass);
    }

    private HttpClient checkUrlAndHttpClient() {
        if (query == null || (query instanceof String && Strings.isEmpty((String) query))) {
            if (uri == null) {
                requestBuilder.uri(URI.create(url));
            } else {
                requestBuilder.uri(uri);
            }
        } else {
            if (uri == null) {
                requestBuilder.uri(URI.create(URLEncodedUtil.encode(url, query)));
            } else {
                requestBuilder.uri(URI.create(URLEncodedUtil.encode(uri.toString(), query)));
            }
        }

        if (httpClient == null || requireNewClient) {
            if (clientBuilder == null) {
                return DEFAULT_HTTP_CLIENT;
            } else {
                return clientBuilder.build();
            }
        } else {
            return httpClient;
        }
    }

    void doAfterExecution(final HttpClient httpClientUsed) {
        if (closeHttpClientAfterExecution && httpClientUsed != DEFAULT_HTTP_CLIENT) {
            // Shutdown isn't necessary?
        }
    }

    /**
     *
     *
     * @return
     */
    public CompletableFuture<HttpResponse<String>> asyncGet() {
        return asyncGet(BodyHandlers.ofString());
    }

    /**
     *
     * @param <T>
     * @param responseBodyHandler
     * @return
     */
    public <T> CompletableFuture<HttpResponse<T>> asyncGet(final HttpResponse.BodyHandler<T> responseBodyHandler) {
        return asyncExecute(HttpMethod.GET, responseBodyHandler);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @return
     */
    public <T> CompletableFuture<T> asyncGet(final Class<T> resultClass) {
        return asyncExecute(HttpMethod.GET, resultClass);
    }

    /**
     *
     * @param <T>
     * @param responseBodyHandler
     * @param pushPromiseHandler
     * @return
     */
    public <T> CompletableFuture<HttpResponse<T>> asyncGet(final HttpResponse.BodyHandler<T> responseBodyHandler,
            final PushPromiseHandler<T> pushPromiseHandler) {
        return asyncExecute(HttpMethod.GET, responseBodyHandler, pushPromiseHandler);
    }

    /**
     *
     * @return
     */
    public CompletableFuture<HttpResponse<String>> asyncPost() {
        return asyncPost(BodyHandlers.ofString());
    }

    /**
     *
     * @param <T>
     * @param responseBodyHandler
     * @return
     */
    public <T> CompletableFuture<HttpResponse<T>> asyncPost(final HttpResponse.BodyHandler<T> responseBodyHandler) {
        return asyncExecute(HttpMethod.POST, responseBodyHandler);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @return
     */
    public <T> CompletableFuture<T> asyncPost(final Class<T> resultClass) {
        return asyncExecute(HttpMethod.POST, resultClass);
    }

    /**
     *
     * @param <T>
     * @param responseBodyHandlerr
     * @param pushPromiseHandler
     * @return
     */
    public <T> CompletableFuture<HttpResponse<T>> asyncPost(final HttpResponse.BodyHandler<T> responseBodyHandlerr,
            final PushPromiseHandler<T> pushPromiseHandler) {
        return asyncExecute(HttpMethod.POST, responseBodyHandlerr, pushPromiseHandler);
    }

    /**
     *
     * @return
     */
    public CompletableFuture<HttpResponse<String>> asyncPut() {
        return asyncPut(BodyHandlers.ofString());
    }

    /**
     *
     * @param <T>
     * @param responseBodyHandler
     * @return
     */
    public <T> CompletableFuture<HttpResponse<T>> asyncPut(final HttpResponse.BodyHandler<T> responseBodyHandler) {
        return asyncExecute(HttpMethod.PUT, responseBodyHandler);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @return
     */
    public <T> CompletableFuture<T> asyncPut(final Class<T> resultClass) {
        return asyncExecute(HttpMethod.PUT, resultClass);
    }

    /**
     *
     * @param <T>
     * @param responseBodyHandlerr
     * @param pushPromiseHandler
     * @return
     */
    public <T> CompletableFuture<HttpResponse<T>> asyncPut(final HttpResponse.BodyHandler<T> responseBodyHandlerr,
            final PushPromiseHandler<T> pushPromiseHandler) {
        return asyncExecute(HttpMethod.PUT, responseBodyHandlerr, pushPromiseHandler);
    }

    /**
     *
     * @return
     */
    public CompletableFuture<HttpResponse<String>> asyncPatch() {
        return asyncPatch(BodyHandlers.ofString());
    }

    /**
     *
     * @param <T>
     * @param responseBodyHandler
     * @return
     */
    public <T> CompletableFuture<HttpResponse<T>> asyncPatch(final HttpResponse.BodyHandler<T> responseBodyHandler) {
        return asyncExecute(HttpMethod.PATCH, responseBodyHandler);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @return
     */
    public <T> CompletableFuture<T> asyncPatch(final Class<T> resultClass) {
        return asyncExecute(HttpMethod.PATCH, resultClass);
    }

    /**
     *
     * @param <T>
     * @param responseBodyHandlerr
     * @param pushPromiseHandler
     * @return
     */
    public <T> CompletableFuture<HttpResponse<T>> asyncPatch(final HttpResponse.BodyHandler<T> responseBodyHandlerr,
            final PushPromiseHandler<T> pushPromiseHandler) {
        return asyncExecute(HttpMethod.PATCH, responseBodyHandlerr, pushPromiseHandler);
    }

    /**
     *
     *
     * @return
     */
    public CompletableFuture<HttpResponse<String>> asyncDelete() {
        return asyncDelete(BodyHandlers.ofString());
    }

    /**
     *
     * @param <T>
     * @param responseBodyHandler
     * @return
     */
    public <T> CompletableFuture<HttpResponse<T>> asyncDelete(final HttpResponse.BodyHandler<T> responseBodyHandler) {
        return asyncExecute(HttpMethod.DELETE, responseBodyHandler);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @return
     */
    public <T> CompletableFuture<T> asyncDelete(final Class<T> resultClass) {
        return asyncExecute(HttpMethod.DELETE, resultClass);
    }

    /**
     *
     * @param <T>
     * @param responseBodyHandlerr
     * @param pushPromiseHandler
     * @return
     */
    public <T> CompletableFuture<HttpResponse<T>> asyncDelete(final HttpResponse.BodyHandler<T> responseBodyHandlerr,
            final PushPromiseHandler<T> pushPromiseHandler) {
        return asyncExecute(HttpMethod.DELETE, responseBodyHandlerr, pushPromiseHandler);
    }

    /**
     *
     * @return
     */
    public CompletableFuture<HttpResponse<Void>> asyncHead() {
        return asyncHead(BodyHandlers.discarding());
    }

    /**
     *
     * @param <T>
     * @param responseBodyHandler
     * @return
     */
    <T> CompletableFuture<HttpResponse<T>> asyncHead(final HttpResponse.BodyHandler<T> responseBodyHandler) {
        return asyncExecute(HttpMethod.HEAD, responseBodyHandler);
    }

    /**
     *
     *
     * @param httpMethod
     * @return
     */
    @Beta
    public CompletableFuture<HttpResponse<String>> asyncExecute(final HttpMethod httpMethod) {
        return asyncExecute(httpMethod, BodyHandlers.ofString());
    }

    /**
     * 
     *
     * @param <T> 
     * @param httpMethod 
     * @param responseBodyHandler 
     * @return 
     * @throws IllegalArgumentException 
     */
    @Beta
    public <T> CompletableFuture<HttpResponse<T>> asyncExecute(final HttpMethod httpMethod, final HttpResponse.BodyHandler<T> responseBodyHandler)
            throws IllegalArgumentException {
        N.checkArgNotNull(httpMethod, HTTP_METHOD_STR);

        final HttpClient httpClientToUse = checkUrlAndHttpClient();

        try {
            return httpClientToUse.sendAsync(requestBuilder.method(httpMethod.name(), checkBodyPublisher()).build(), responseBodyHandler);
        } finally {
            // This is asynchronous call
            // doAfterExecution(httpClientToUse);
        }
    }

    /**
     * 
     *
     * @param <T> 
     * @param httpMethod 
     * @param resultClass 
     * @return 
     * @throws IllegalArgumentException 
     */
    @Beta
    public <T> CompletableFuture<T> asyncExecute(final HttpMethod httpMethod, final Class<T> resultClass) throws IllegalArgumentException {
        N.checkArgNotNull(httpMethod, HTTP_METHOD_STR);

        final HttpClient httpClientToUse = checkUrlAndHttpClient();
        final BodyHandler<?> responseBodyHandler = createResponseBodyHandler(resultClass);

        try {
            return httpClientToUse.sendAsync(requestBuilder.method(httpMethod.name(), checkBodyPublisher()).build(), responseBodyHandler)
                    .thenApply(it -> getBody(it, resultClass));
        } finally {
            // This is asynchronous call
            // doAfterExecution(httpClientToUse);
        }
    }

    /**
     * 
     *
     * @param <T> 
     * @param httpMethod 
     * @param responseBodyHandler 
     * @param pushPromiseHandler 
     * @return 
     * @throws IllegalArgumentException 
     */
    @Beta
    public <T> CompletableFuture<HttpResponse<T>> asyncExecute(final HttpMethod httpMethod, final HttpResponse.BodyHandler<T> responseBodyHandler,
            final PushPromiseHandler<T> pushPromiseHandler) throws IllegalArgumentException {
        N.checkArgNotNull(httpMethod, HTTP_METHOD_STR);

        final HttpClient httpClientToUse = checkUrlAndHttpClient();

        try {
            return httpClientToUse.sendAsync(requestBuilder.method(httpMethod.name(), checkBodyPublisher()).build(), responseBodyHandler, pushPromiseHandler);
        } finally {
            // This is asynchronous call
            // doAfterExecution(httpClientToUse);
        }
    }

    private BodyPublisher checkBodyPublisher() {
        return bodyPublisher == null ? BodyPublishers.noBody() : bodyPublisher;
    }

    private BodyHandler<?> createResponseBodyHandler(final Class<?> resultClass) {
        if (resultClass == null || resultClass.equals(Void.class)) {
            return BodyHandlers.discarding();
        } else if (resultClass.equals(String.class)) {
            return BodyHandlers.ofString();
        } else if (byte[].class.equals(resultClass)) {
            return BodyHandlers.ofByteArray();
        } else if (resultClass.isAssignableFrom(InputStream.class)) {
            return BodyHandlers.ofInputStream();
        } else {
            return BodyHandlers.ofString();
        }
    }

    private <T> T getBody(final HttpResponse<?> httpResponse, final Class<T> resultClass) {
        if (!HttpUtil.isSuccessfulResponseCode(httpResponse.statusCode())) {
            throw new UncheckedIOException(new IOException(httpResponse.statusCode() + ": " + httpResponse.body()));
        }

        if (resultClass == null || Void.class.equals(resultClass)) {
            return null; // refer to isOneWayRequest.
        }

        return N.convert(httpResponse.body(), resultClass);
    }
}
