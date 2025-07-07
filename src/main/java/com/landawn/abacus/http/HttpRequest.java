/*
 * Copyright (C) 2019 HaiYang Li
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
 * A fluent API for building and executing HTTP requests using HttpClient.
 * This class provides a builder-style interface for configuring HTTP requests with various options
 * such as headers, authentication, timeouts, and request bodies.
 * 
 * <p>HttpRequest is designed for single-use scenarios where you want to create an HttpClient,
 * execute a request, and then dispose of the client. For multiple requests to the same endpoint,
 * consider using HttpClient directly.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Simple GET request
 * String response = HttpRequest.url("https://api.example.com/users")
 *     .get();
 * 
 * // POST request with JSON body and headers
 * User createdUser = HttpRequest.url("https://api.example.com/users")
 *     .header("Authorization", "Bearer token123")
 *     .jsonBody(new User("John", "Doe"))
 *     .post(User.class);
 * 
 * // Async request with custom timeout
 * ContinuableFuture<String> future = HttpRequest.url("https://api.example.com/data")
 *     .connectionTimeout(5000)
 *     .readTimeout(10000)
 *     .asyncGet();
 * }</pre>
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
     * Creates an HttpRequest instance with the specified HttpClient.
     * This method is useful when you want to use an existing HttpClient with specific configuration.
     *
     * @param httpClient The HttpClient to use for the request
     * @return A new HttpRequest instance
     */
    public static HttpRequest create(final HttpClient httpClient) {
        return new HttpRequest(httpClient);
    }

    /**
     * Creates an HttpRequest for the specified URL with default connection and read timeouts.
     * A new HttpClient will be created internally and closed after the request execution.
     *
     * @param url The target URL for the request
     * @return A new HttpRequest instance
     * 
     * <p>Example:</p>
     * <pre>{@code
     * String response = HttpRequest.url("https://api.example.com/data").get();
     * }</pre>
     */
    public static HttpRequest url(final String url) {
        return url(url, HttpClient.DEFAULT_CONNECTION_TIMEOUT, HttpClient.DEFAULT_READ_TIMEOUT);
    }

    /**
     * Creates an HttpRequest for the specified URL with custom timeouts.
     * A new HttpClient will be created internally and closed after the request execution.
     *
     * @param url The target URL for the request
     * @param connectionTimeoutInMillis Connection timeout in milliseconds
     * @param readTimeoutInMillis Read timeout in milliseconds
     * @return A new HttpRequest instance
     * 
     * <p>Example:</p>
     * <pre>{@code
     * String response = HttpRequest.url("https://api.example.com/data", 5000, 10000).get();
     * }</pre>
     */
    public static HttpRequest url(final String url, final long connectionTimeoutInMillis, final long readTimeoutInMillis) {
        return new HttpRequest(HttpClient.create(url, 1, connectionTimeoutInMillis, readTimeoutInMillis)).closeHttpClientAfterExecution(true);
    }

    /**
     * Creates an HttpRequest for the specified URL with default connection and read timeouts.
     * A new HttpClient will be created internally and closed after the request execution.
     *
     * @param url The target URL for the request
     * @return A new HttpRequest instance
     * @throws IllegalArgumentException if the scheme of {@code url} is not {@code http} or {@code https}.
     */
    public static HttpRequest url(final URL url) {
        return url(url, HttpClient.DEFAULT_CONNECTION_TIMEOUT, HttpClient.DEFAULT_READ_TIMEOUT);
    }

    /**
     * Creates an HttpRequest for the specified URL with custom timeouts.
     * A new HttpClient will be created internally and closed after the request execution.
     *
     * @param url The target URL for the request
     * @param connectionTimeoutInMillis Connection timeout in milliseconds
     * @param readTimeoutInMillis Read timeout in milliseconds
     * @return A new HttpRequest instance
     */
    public static HttpRequest url(final URL url, final long connectionTimeoutInMillis, final long readTimeoutInMillis) {
        return new HttpRequest(HttpClient.create(url, 1, connectionTimeoutInMillis, readTimeoutInMillis)).closeHttpClientAfterExecution(true);
    }

    HttpRequest closeHttpClientAfterExecution(final boolean b) {
        closeHttpClientAfterExecution = b;

        return this;
    }

    /**
     * Overwrites the existing HTTP settings with the specified settings.
     * This method merges the provided settings with any existing settings.
     *
     * @param httpSettings The HTTP settings to apply
     * @return This HttpRequest instance for method chaining
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
     * Sets HTTP Basic Authentication header using the specified username and password.
     *
     * @param user The username for authentication
     * @param password The password for authentication
     * @return This HttpRequest instance for method chaining
     * @see HttpHeaders
     * 
     * <p>Example:</p>
     * <pre>{@code
     * HttpRequest.url("https://api.example.com/secure")
     *     .basicAuth("username", "password")
     *     .get();
     * }</pre>
     */
    public HttpRequest basicAuth(final String user, final Object password) {
        checkSettings();

        settings.basicAuth(user, password);

        return this;
    }

    /**
     * Sets an HTTP header with the specified name and value.
     * If this request already has any headers with that name, they are all replaced.
     *
     * @param name The header name
     * @param value The header value
     * @return This HttpRequest instance for method chaining
     * @see HttpSettings#header(String, Object)
     * @see HttpHeaders
     * 
     * <p>Example:</p>
     * <pre>{@code
     * HttpRequest.url("https://api.example.com")
     *     .header("Authorization", "Bearer token123")
     *     .header("Accept", "application/json")
     *     .get();
     * }</pre>
     */
    public HttpRequest header(final String name, final Object value) {
        checkSettings();

        settings.header(name, value);

        return this;
    }

    /**
     * Sets two HTTP headers with the specified names and values.
     * If this request already has any headers with those names, they are all replaced.
     *
     * @param name1 The first header name
     * @param value1 The first header value
     * @param name2 The second header name
     * @param value2 The second header value
     * @return This HttpRequest instance for method chaining
     * @see HttpSettings#headers(String, Object, String, Object)
     * @see HttpHeaders
     */
    public HttpRequest headers(final String name1, final Object value1, final String name2, final Object value2) {
        checkSettings();

        settings.headers(name1, value1, name2, value2);

        return this;
    }

    /**
     * Sets three HTTP headers with the specified names and values.
     * If this request already has any headers with those names, they are all replaced.
     *
     * @param name1 The first header name
     * @param value1 The first header value
     * @param name2 The second header name
     * @param value2 The second header value
     * @param name3 The third header name
     * @param value3 The third header value
     * @return This HttpRequest instance for method chaining
     * @see HttpSettings#headers(String, Object, String, Object, String, Object)
     * @see HttpHeaders
     */
    public HttpRequest headers(final String name1, final Object value1, final String name2, final Object value2, final String name3, final Object value3) {
        checkSettings();

        settings.headers(name1, value1, name2, value2, name3, value3);

        return this;
    }

    /**
     * Sets HTTP headers from the specified map.
     * If this request already has any headers with the same names, they are all replaced.
     *
     * @param headers A map containing header names and values
     * @return This HttpRequest instance for method chaining
     * @see HttpSettings#headers(Map)
     * @see HttpHeaders
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Map<String, String> headers = Map.of(
     *     "Authorization", "Bearer token123",
     *     "Content-Type", "application/json"
     * );
     * HttpRequest.url("https://api.example.com")
     *     .headers(headers)
     *     .get();
     * }</pre>
     */
    public HttpRequest headers(final Map<String, ?> headers) {
        checkSettings();

        settings.headers(headers);

        return this;
    }

    /**
     * Removes all headers on this request and adds the specified headers.
     *
     * @param headers The HttpHeaders to set
     * @return This HttpRequest instance for method chaining
     * @see HttpSettings#headers(HttpHeaders)
     * @see HttpHeaders
     */
    public HttpRequest headers(final HttpHeaders headers) {
        checkSettings();

        settings.headers(headers);

        return this;
    }

    /**
     * Sets the connection timeout in milliseconds.
     *
     * @param connectionTimeout The connection timeout in milliseconds
     * @return This HttpRequest instance for method chaining
     */
    public HttpRequest connectionTimeout(final long connectionTimeout) {
        checkSettings();

        settings.setConnectionTimeout(connectionTimeout);

        return this;
    }

    /**
     * Sets the connection timeout using a Duration.
     *
     * @param connectionTimeout The connection timeout as a Duration
     * @return This HttpRequest instance for method chaining
     * 
     * <p>Example:</p>
     * <pre>{@code
     * HttpRequest.url("https://api.example.com")
     *     .connectionTimeout(Duration.ofSeconds(5))
     *     .get();
     * }</pre>
     */
    public HttpRequest connectionTimeout(final Duration connectionTimeout) {
        checkSettings();

        settings.setConnectionTimeout(connectionTimeout.toMillis());

        return this;
    }

    /**
     * Sets the read timeout in milliseconds.
     *
     * @param readTimeout The read timeout in milliseconds
     * @return This HttpRequest instance for method chaining
     */
    public HttpRequest readTimeout(final long readTimeout) {
        checkSettings();

        settings.setReadTimeout(readTimeout);

        return this;
    }

    /**
     * Sets the read timeout using a Duration.
     *
     * @param readTimeout The read timeout as a Duration
     * @return This HttpRequest instance for method chaining
     * 
     * <p>Example:</p>
     * <pre>{@code
     * HttpRequest.url("https://api.example.com")
     *     .readTimeout(Duration.ofSeconds(10))
     *     .get();
     * }</pre>
     */
    public HttpRequest readTimeout(final Duration readTimeout) {
        checkSettings();

        settings.setReadTimeout(readTimeout.toMillis());

        return this;
    }

    /**
     * Sets whether to use caches for this request.
     *
     * @param useCaches true to use caches, false otherwise
     * @return This HttpRequest instance for method chaining
     */
    public HttpRequest useCaches(final boolean useCaches) {
        checkSettings();

        settings.setUseCaches(useCaches);

        return this;
    }

    /**
     * Sets the SSL socket factory for HTTPS connections.
     *
     * @param sslSocketFactory The SSL socket factory to use
     * @return This HttpRequest instance for method chaining
     */
    public HttpRequest sslSocketFactory(final SSLSocketFactory sslSocketFactory) {
        checkSettings();

        settings.setSSLSocketFactory(sslSocketFactory);

        return this;
    }

    /**
     * Sets the proxy for this request.
     *
     * @param proxy The proxy to use
     * @return This HttpRequest instance for method chaining
     */
    public HttpRequest proxy(final Proxy proxy) {
        checkSettings();

        settings.setProxy(proxy);

        return this;
    }

    /**
     * Sets query parameters for GET or DELETE requests as a query string.
     *
     * @param query The query string
     * @return This HttpRequest instance for method chaining
     * 
     * <p>Example:</p>
     * <pre>{@code
     * HttpRequest.url("https://api.example.com/search")
     *     .query("q=test&page=1")
     *     .get();
     * }</pre>
     */
    public HttpRequest query(final String query) {
        request = query;

        return this;
    }

    /**
     * Sets query parameters for GET or DELETE requests from a map.
     *
     * @param queryParams A map containing query parameter names and values
     * @return This HttpRequest instance for method chaining
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Map<String, Object> params = Map.of("q", "test", "page", 1);
     * HttpRequest.url("https://api.example.com/search")
     *     .query(params)
     *     .get();
     * }</pre>
     */
    public HttpRequest query(final Map<String, ?> queryParams) {
        request = queryParams;

        return this;
    }

    /**
     * Sets the request body as JSON string and sets the Content-Type header to application/json.
     *
     * @param json The JSON string
     * @return This HttpRequest instance for method chaining
     * 
     * <p>Example:</p>
     * <pre>{@code
     * HttpRequest.url("https://api.example.com/users")
     *     .jsonBody("{\"name\":\"John\",\"age\":30}")
     *     .post();
     * }</pre>
     */
    public HttpRequest jsonBody(final String json) {
        setContentType(HttpHeaders.Values.APPLICATION_JSON);

        request = json;

        return this;
    }

    /**
     * Sets the request body as JSON by serializing the specified object and sets the Content-Type header to application/json.
     *
     * @param obj The object to serialize as JSON
     * @return This HttpRequest instance for method chaining
     * 
     * <p>Example:</p>
     * <pre>{@code
     * User user = new User("John", "Doe");
     * HttpRequest.url("https://api.example.com/users")
     *     .jsonBody(user)
     *     .post();
     * }</pre>
     */
    public HttpRequest jsonBody(final Object obj) {
        setContentType(HttpHeaders.Values.APPLICATION_JSON);

        request = N.toJson(obj);

        return this;
    }

    /**
     * Sets the request body as XML string and sets the Content-Type header to application/xml.
     *
     * @param xml The XML string
     * @return This HttpRequest instance for method chaining
     */
    public HttpRequest xmlBody(final String xml) {
        setContentType(HttpHeaders.Values.APPLICATION_XML);

        request = xml;

        return this;
    }

    /**
     * Sets the request body as XML by serializing the specified object and sets the Content-Type header to application/xml.
     *
     * @param obj The object to serialize as XML
     * @return This HttpRequest instance for method chaining
     */
    public HttpRequest xmlBody(final Object obj) {
        setContentType(HttpHeaders.Values.APPLICATION_XML);

        request = N.toXml(obj);

        return this;
    }

    /**
     * Sets the request body as form URL-encoded data from a map and sets the Content-Type header to application/x-www-form-urlencoded.
     *
     * @param formBodyByMap A map containing form field names and values
     * @return This HttpRequest instance for method chaining
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Map<String, String> formData = Map.of("username", "john", "password", "secret");
     * HttpRequest.url("https://api.example.com/login")
     *     .formBody(formData)
     *     .post();
     * }</pre>
     */
    public HttpRequest formBody(final Map<?, ?> formBodyByMap) {
        setContentType(HttpHeaders.Values.APPLICATION_URL_ENCODED);

        request = formBodyByMap;

        return this;
    }

    /**
     * Sets the request body as form URL-encoded data from a bean and sets the Content-Type header to application/x-www-form-urlencoded.
     *
     * @param formBodyByBean A bean object whose properties will be used as form fields
     * @return This HttpRequest instance for method chaining
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
     * Sets the request body. Can be a String, byte[], File, InputStream, Reader, or any object for serialization.
     *
     * @param requestBody The request body
     * @return This HttpRequest instance for method chaining
     */
    public HttpRequest body(final Object requestBody) {
        request = requestBody;

        return this;
    }

    /**
     * Executes a GET request and returns the response as an HttpResponse.
     *
     * @return The HttpResponse object containing status, headers, and body
     * @throws UncheckedIOException if an I/O error occurs
     * 
     * <p>Example:</p>
     * <pre>{@code
     * HttpResponse response = HttpRequest.url("https://api.example.com/data").get();
     * System.out.println("Status: " + response.statusCode());
     * System.out.println("Body: " + response.body(String.class));
     * }</pre>
     */
    public HttpResponse get() throws UncheckedIOException {
        return get(HttpResponse.class);
    }

    /**
     * Executes a GET request and deserializes the response to the specified type.
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object
     * @return The deserialized response object
     * @throws UncheckedIOException if an I/O error occurs
     * 
     * <p>Example:</p>
     * <pre>{@code
     * User user = HttpRequest.url("https://api.example.com/users/123")
     *     .get(User.class);
     * }</pre>
     */
    public <T> T get(final Class<T> resultClass) throws UncheckedIOException {
        return execute(HttpMethod.GET, resultClass);
    }

    /**
     * Executes a POST request and returns the response as an HttpResponse.
     *
     * @return The HttpResponse object containing status, headers, and body
     * @throws UncheckedIOException if an I/O error occurs
     */
    public HttpResponse post() throws UncheckedIOException {
        return post(HttpResponse.class);
    }

    /**
     * Executes a POST request and deserializes the response to the specified type.
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object
     * @return The deserialized response object
     * @throws UncheckedIOException if an I/O error occurs
     * 
     * <p>Example:</p>
     * <pre>{@code
     * User newUser = new User("John", "Doe");
     * User createdUser = HttpRequest.url("https://api.example.com/users")
     *     .jsonBody(newUser)
     *     .post(User.class);
     * }</pre>
     */
    public <T> T post(final Class<T> resultClass) throws UncheckedIOException {
        return execute(HttpMethod.POST, resultClass);
    }

    /**
     * Executes a PUT request and returns the response as an HttpResponse.
     *
     * @return The HttpResponse object containing status, headers, and body
     * @throws UncheckedIOException if an I/O error occurs
     */
    public HttpResponse put() throws UncheckedIOException {
        return put(HttpResponse.class);
    }

    /**
     * Executes a PUT request and deserializes the response to the specified type.
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object
     * @return The deserialized response object
     * @throws UncheckedIOException if an I/O error occurs
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
     * Executes a DELETE request and returns the response as an HttpResponse.
     *
     * @return The HttpResponse object containing status, headers, and body
     * @throws UncheckedIOException if an I/O error occurs
     */
    public HttpResponse delete() throws UncheckedIOException {
        return delete(HttpResponse.class);
    }

    /**
     * Executes a DELETE request and deserializes the response to the specified type.
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object
     * @return The deserialized response object
     * @throws UncheckedIOException if an I/O error occurs
     */
    public <T> T delete(final Class<T> resultClass) throws UncheckedIOException {
        return execute(HttpMethod.DELETE, resultClass);
    }

    /**
     * Executes a HEAD request and returns the response as an HttpResponse.
     *
     * @return The HttpResponse object containing status and headers (no body)
     * @throws UncheckedIOException if an I/O error occurs
     */
    public HttpResponse head() throws UncheckedIOException {
        return head(HttpResponse.class);
    }

    /**
     * Executes a HEAD request with the specified result class.
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object
     * @return The deserialized response object (typically null for HEAD requests)
     * @throws UncheckedIOException if an I/O error occurs
     */
    <T> T head(final Class<T> resultClass) throws UncheckedIOException {
        return execute(HttpMethod.HEAD, resultClass);
    }

    /**
     * Executes an HTTP request with the specified method and returns the response as an HttpResponse.
     *
     * @param httpMethod The HTTP method to use
     * @return The HttpResponse object containing status, headers, and body
     * @throws UncheckedIOException if an I/O error occurs
     */
    @Beta
    public HttpResponse execute(final HttpMethod httpMethod) throws UncheckedIOException {
        return execute(httpMethod, HttpResponse.class);
    }

    /**
     * Executes an HTTP request with the specified method and deserializes the response to the specified type.
     *
     * @param <T> The type of the response object
     * @param httpMethod The HTTP method to use
     * @param resultClass The class of the expected response object
     * @return The deserialized response object
     * @throws IllegalArgumentException if httpMethod is null
     * @throws UncheckedIOException if an I/O error occurs
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
     * Executes an HTTP request with the specified method and writes the response to a file.
     *
     * @param httpMethod The HTTP method to use
     * @param output The file to write the response to
     * @throws IllegalArgumentException if httpMethod is null
     * @throws UncheckedIOException if an I/O error occurs
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
     * Executes an HTTP request with the specified method and writes the response to an output stream.
     *
     * @param httpMethod The HTTP method to use
     * @param output The output stream to write the response to
     * @throws IllegalArgumentException if httpMethod is null
     * @throws UncheckedIOException if an I/O error occurs
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
     * Executes an HTTP request with the specified method and writes the response to a writer.
     *
     * @param httpMethod The HTTP method to use
     * @param output The writer to write the response to
     * @throws IllegalArgumentException if httpMethod is null
     * @throws UncheckedIOException if an I/O error occurs
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

    /**
     * Executes an asynchronous GET request and returns a ContinuableFuture with the HttpResponse.
     *
     * @return A ContinuableFuture that will complete with the HttpResponse
     * 
     * <p>Example:</p>
     * <pre>{@code
     * HttpRequest.url("https://api.example.com/data")
     *     .asyncGet()
     *     .getThenAccept(response -> System.out.println("Status: " + response.statusCode()));
     * }</pre>
     */
    public ContinuableFuture<HttpResponse> asyncGet() {
        return asyncGet(HttpResponse.class);
    }

    /**
     * Executes an asynchronous GET request with a custom executor and returns a ContinuableFuture with the HttpResponse.
     *
     * @param executor The executor to use for the asynchronous operation
     * @return A ContinuableFuture that will complete with the HttpResponse
     */
    public ContinuableFuture<HttpResponse> asyncGet(final Executor executor) {
        return asyncGet(HttpResponse.class, executor);
    }

    /**
     * Executes an asynchronous GET request and deserializes the response to the specified type.
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object
     * @return A ContinuableFuture that will complete with the deserialized response
     * 
     * <p>Example:</p>
     * <pre>{@code
     * HttpRequest.url("https://api.example.com/users/123")
     *     .asyncGet(User.class)
     *     .getThenAccept(user -> System.out.println("User: " + user.getName()));
     * }</pre>
     */
    public <T> ContinuableFuture<T> asyncGet(final Class<T> resultClass) {
        return asyncExecute(HttpMethod.GET, resultClass);
    }

    /**
     * Executes an asynchronous GET request with a custom executor and deserializes the response to the specified type.
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object
     * @param executor The executor to use for the asynchronous operation
     * @return A ContinuableFuture that will complete with the deserialized response
     */
    public <T> ContinuableFuture<T> asyncGet(final Class<T> resultClass, final Executor executor) {
        return asyncExecute(HttpMethod.GET, resultClass, executor);
    }

    /**
     * Executes an asynchronous POST request and returns a ContinuableFuture with the HttpResponse.
     *
     * @return A ContinuableFuture that will complete with the HttpResponse
     */
    public ContinuableFuture<HttpResponse> asyncPost() {
        return asyncPost(HttpResponse.class);
    }

    /**
     * Executes an asynchronous POST request with a custom executor and returns a ContinuableFuture with the HttpResponse.
     *
     * @param executor The executor to use for the asynchronous operation
     * @return A ContinuableFuture that will complete with the HttpResponse
     */
    public ContinuableFuture<HttpResponse> asyncPost(final Executor executor) {
        return asyncPost(HttpResponse.class, executor);
    }

    /**
     * Executes an asynchronous POST request and deserializes the response to the specified type.
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object
     * @return A ContinuableFuture that will complete with the deserialized response
     */
    public <T> ContinuableFuture<T> asyncPost(final Class<T> resultClass) {
        return asyncExecute(HttpMethod.POST, resultClass);
    }

    /**
     * Executes an asynchronous POST request with a custom executor and deserializes the response to the specified type.
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object
     * @param executor The executor to use for the asynchronous operation
     * @return A ContinuableFuture that will complete with the deserialized response
     */
    public <T> ContinuableFuture<T> asyncPost(final Class<T> resultClass, final Executor executor) {
        return asyncExecute(HttpMethod.POST, resultClass, executor);
    }

    /**
     * Executes an asynchronous PUT request and returns a ContinuableFuture with the HttpResponse.
     *
     * @return A ContinuableFuture that will complete with the HttpResponse
     */
    public ContinuableFuture<HttpResponse> asyncPut() {
        return asyncPut(HttpResponse.class);
    }

    /**
     * Executes an asynchronous PUT request with a custom executor and returns a ContinuableFuture with the HttpResponse.
     *
     * @param executor The executor to use for the asynchronous operation
     * @return A ContinuableFuture that will complete with the HttpResponse
     */
    public ContinuableFuture<HttpResponse> asyncPut(final Executor executor) {
        return asyncPut(HttpResponse.class, executor);
    }

    /**
     * Executes an asynchronous PUT request and deserializes the response to the specified type.
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object
     * @return A ContinuableFuture that will complete with the deserialized response
     */
    public <T> ContinuableFuture<T> asyncPut(final Class<T> resultClass) {
        return asyncExecute(HttpMethod.PUT, resultClass);
    }

    /**
     * Executes an asynchronous PUT request with a custom executor and deserializes the response to the specified type.
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object
     * @param executor The executor to use for the asynchronous operation
     * @return A ContinuableFuture that will complete with the deserialized response
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

    /**
     * Executes an asynchronous DELETE request and returns a ContinuableFuture with the HttpResponse.
     *
     * @return A ContinuableFuture that will complete with the HttpResponse
     */
    public ContinuableFuture<HttpResponse> asyncDelete() {
        return asyncDelete(HttpResponse.class);
    }

    /**
     * Executes an asynchronous DELETE request with a custom executor and returns a ContinuableFuture with the HttpResponse.
     *
     * @param executor The executor to use for the asynchronous operation
     * @return A ContinuableFuture that will complete with the HttpResponse
     */
    public ContinuableFuture<HttpResponse> asyncDelete(final Executor executor) {
        return asyncDelete(HttpResponse.class, executor);
    }

    /**
     * Executes an asynchronous DELETE request and deserializes the response to the specified type.
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object
     * @return A ContinuableFuture that will complete with the deserialized response
     */
    public <T> ContinuableFuture<T> asyncDelete(final Class<T> resultClass) {
        return asyncExecute(HttpMethod.DELETE, resultClass);
    }

    /**
     * Executes an asynchronous DELETE request with a custom executor and deserializes the response to the specified type.
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object
     * @param executor The executor to use for the asynchronous operation
     * @return A ContinuableFuture that will complete with the deserialized response
     */
    public <T> ContinuableFuture<T> asyncDelete(final Class<T> resultClass, final Executor executor) {
        return asyncExecute(HttpMethod.DELETE, resultClass, executor);
    }

    /**
     * Executes an asynchronous HEAD request and returns a ContinuableFuture with the HttpResponse.
     *
     * @return A ContinuableFuture that will complete with the HttpResponse
     */
    public ContinuableFuture<HttpResponse> asyncHead() {
        return asyncHead(HttpResponse.class);
    }

    /**
     * Executes an asynchronous HEAD request with a custom executor and returns a ContinuableFuture with the HttpResponse.
     *
     * @param executor The executor to use for the asynchronous operation
     * @return A ContinuableFuture that will complete with the HttpResponse
     */
    public ContinuableFuture<HttpResponse> asyncHead(final Executor executor) {
        return asyncHead(executor, HttpResponse.class);
    }

    /**
     * Executes an asynchronous HEAD request with the specified result class.
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object
     * @return A ContinuableFuture that will complete with the response
     */
    <T> ContinuableFuture<T> asyncHead(final Class<T> resultClass) {
        return asyncExecute(HttpMethod.HEAD, resultClass);
    }

    /**
     * Executes an asynchronous HEAD request with a custom executor and the specified result class.
     *
     * @param <T> The type of the response object
     * @param executor The executor to use for the asynchronous operation
     * @param resultClass The class of the expected response object
     * @return A ContinuableFuture that will complete with the response
     */
    <T> ContinuableFuture<T> asyncHead(final Executor executor, final Class<T> resultClass) {
        return asyncExecute(HttpMethod.HEAD, resultClass, executor);
    }

    /**
     * Executes an asynchronous HTTP request with the specified method and returns a ContinuableFuture with the HttpResponse.
     *
     * @param httpMethod The HTTP method to use
     * @return A ContinuableFuture that will complete with the HttpResponse
     */
    @Beta
    public ContinuableFuture<HttpResponse> asyncExecute(final HttpMethod httpMethod) {
        return asyncExecute(httpMethod, HttpResponse.class);
    }

    /**
     * Executes an asynchronous HTTP request with the specified method and a custom executor.
     *
     * @param httpMethod The HTTP method to use
     * @param executor The executor to use for the asynchronous operation
     * @return A ContinuableFuture that will complete with the HttpResponse
     */
    @Beta
    public ContinuableFuture<HttpResponse> asyncExecute(final HttpMethod httpMethod, final Executor executor) {
        return asyncExecute(httpMethod, HttpResponse.class, executor);
    }

    /**
     * Executes an asynchronous HTTP request with the specified method and deserializes the response to the specified type.
     *
     * @param <T> The type of the response object
     * @param httpMethod The HTTP method to use
     * @param resultClass The class of the expected response object
     * @return A ContinuableFuture that will complete with the deserialized response
     * @throws IllegalArgumentException if httpMethod is null
     */
    @Beta
    public <T> ContinuableFuture<T> asyncExecute(final HttpMethod httpMethod, final Class<T> resultClass) throws IllegalArgumentException {
        N.checkArgNotNull(httpMethod, HTTP_METHOD_STR);

        final Callable<T> cmd = () -> execute(httpMethod, resultClass);
        return httpClient._asyncExecutor.execute(cmd);

    }

    /**
     * Executes an asynchronous HTTP request with the specified method, custom executor, and deserializes the response.
     *
     * @param <T> The type of the response object
     * @param httpMethod The HTTP method to use
     * @param resultClass The class of the expected response object
     * @param executor The executor to use for the asynchronous operation
     * @return A ContinuableFuture that will complete with the deserialized response
     * @throws IllegalArgumentException if httpMethod is null
     */
    @Beta
    public <T> ContinuableFuture<T> asyncExecute(final HttpMethod httpMethod, final Class<T> resultClass, final Executor executor)
            throws IllegalArgumentException {
        N.checkArgNotNull(httpMethod, HTTP_METHOD_STR);

        final Callable<T> cmd = () -> execute(httpMethod, resultClass);
        return execute(cmd, executor);
    }

    /**
     * Executes an asynchronous HTTP request and writes the response to a file.
     *
     * @param httpMethod The HTTP method to use
     * @param output The file to write the response to
     * @return A ContinuableFuture that will complete when the file is written
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
     * Executes an asynchronous HTTP request with a custom executor and writes the response to a file.
     *
     * @param httpMethod The HTTP method to use
     * @param output The file to write the response to
     * @param executor The executor to use for the asynchronous operation
     * @return A ContinuableFuture that will complete when the file is written
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
     * Executes an asynchronous HTTP request and writes the response to an output stream.
     *
     * @param httpMethod The HTTP method to use
     * @param output The output stream to write the response to
     * @return A ContinuableFuture that will complete when the stream is written
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
     * Executes an asynchronous HTTP request with a custom executor and writes the response to an output stream.
     *
     * @param httpMethod The HTTP method to use
     * @param output The output stream to write the response to
     * @param executor The executor to use for the asynchronous operation
     * @return A ContinuableFuture that will complete when the stream is written
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
     * Executes an asynchronous HTTP request and writes the response to a writer.
     *
     * @param httpMethod The HTTP method to use
     * @param output The writer to write the response to
     * @return A ContinuableFuture that will complete when the writer is written
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
     * Executes an asynchronous HTTP request with a custom executor and writes the response to a writer.
     *
     * @param httpMethod The HTTP method to use
     * @param output The writer to write the response to
     * @param executor The executor to use for the asynchronous operation
     * @return A ContinuableFuture that will complete when the writer is written
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
     * Executes a callable command using the specified executor.
     *
     * @param <R> The result type of the callable
     * @param cmd The callable command to execute
     * @param executor The executor to use
     * @return A ContinuableFuture that will complete with the result of the callable
     */
    <R> ContinuableFuture<R> execute(final Callable<R> cmd, final Executor executor) {
        N.checkArgNotNull(executor, cs.executor);

        return N.asyncExecute(cmd, executor);
    }

    HttpSettings checkSettings() {
        if (settings == null) {
            settings = new HttpSettings();
        }

        return settings;
    }
}