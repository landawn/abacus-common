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
import com.landawn.abacus.util.Beans;
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
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Simple GET request
 * String response = HttpRequest.url("https://api.example.com/users")
 *     .get(String.class);
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
 *     .asyncGet(String.class);
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
     * @param httpClient The HttpClient to use for the request. Must not be {@code null}.
     * @return A new HttpRequest instance
     */
    public static HttpRequest create(final HttpClient httpClient) {
        return new HttpRequest(httpClient);
    }

    /**
     * Creates an HttpRequest for the specified URL with default connection and read timeouts.
     * A new HttpClient will be created internally and closed after the request execution.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String response = HttpRequest.url("https://api.example.com/data").get(String.class);
     * }</pre>
     *
     * @param url The target URL for the request
     * @return A new HttpRequest instance
     * @throws IllegalArgumentException if the scheme of {@code url} is not {@code http} or {@code https}.
     */
    public static HttpRequest url(final String url) {
        return url(url, HttpClient.DEFAULT_CONNECTION_TIMEOUT, HttpClient.DEFAULT_READ_TIMEOUT);
    }

    /**
     * Creates an HttpRequest for the specified URL with custom timeouts.
     * A new HttpClient will be created internally and closed after the request execution.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String response = HttpRequest.url("https://api.example.com/data", 5000, 10000).get(String.class);
     * }</pre>
     *
     * @param url The target URL for the request
     * @param connectionTimeoutInMillis Connection timeout in milliseconds
     * @param readTimeoutInMillis Read timeout in milliseconds
     * @return A new HttpRequest instance
     * @throws IllegalArgumentException if the scheme of {@code url} is not {@code http} or {@code https}.
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
     * Merges the provided HTTP settings with existing settings on this request.
     * This method allows you to apply pre-configured settings to a request. If the same setting
     * is defined in both existing and provided settings, the provided settings take precedence.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpSettings settings = new HttpSettings()
     *     .header("User-Agent", "MyApp/1.0")
     *     .setConnectionTimeout(5000);
     * HttpRequest.url("https://api.example.com")
     *     .settings(settings)
     *     .get();
     * }</pre>
     *
     * @param httpSettings The HTTP settings to merge. If {@code null}, this method has no effect.
     * @return This HttpRequest instance for method chaining
     * @see HttpSettings
     */
    @Beta
    public HttpRequest settings(final HttpSettings httpSettings) {
        checkSettings();

        if (httpSettings != null) {
            Beans.merge(settings, httpSettings);
        }

        return this;
    }

    /**
     * Sets HTTP Basic Authentication header using the specified username and password.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest.url("https://api.example.com/secure")
     *     .basicAuth("username", "password")
     *     .get();
     * }</pre>
     *
     * @param user The username for authentication
     * @param password The password for authentication
     * @return This HttpRequest instance for method chaining
     * @see HttpHeaders
     * @see HttpHeaders.Names
     * @see HttpHeaders.Values
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest.url("https://api.example.com")
     *     .header("Authorization", "Bearer token123")
     *     .header("Accept", "application/json")
     *     .get();
     * }</pre>
     *
     * @param name The header name
     * @param value The header value
     * @return This HttpRequest instance for method chaining
     * @see HttpHeaders
     * @see HttpHeaders.Names
     * @see HttpHeaders.Values
     */
    public HttpRequest header(final String name, final Object value) {
        checkSettings();

        settings.header(name, value);

        return this;
    }

    /**
     * Sets two HTTP headers with the specified names and values.
     * If this request already has any headers with those names, they are all replaced.
     * This is a convenience method for setting multiple headers at once.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest.url("https://api.example.com")
     *     .headers("Authorization", "Bearer token123",
     *              "Accept", "application/json")
     *     .get();
     * }</pre>
     *
     * @param name1 The first header name
     * @param value1 The first header value
     * @param name2 The second header name
     * @param value2 The second header value
     * @return This HttpRequest instance for method chaining
     * @see HttpHeaders
     * @see HttpHeaders.Names
     * @see HttpHeaders.Values
     */
    public HttpRequest headers(final String name1, final Object value1, final String name2, final Object value2) {
        checkSettings();

        settings.headers(name1, value1, name2, value2);

        return this;
    }

    /**
     * Sets three HTTP headers with the specified names and values.
     * If this request already has any headers with those names, they are all replaced.
     * This is a convenience method for setting multiple headers at once.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest.url("https://api.example.com")
     *     .headers("Authorization", "Bearer token123",
     *              "Accept", "application/json",
     *              "User-Agent", "MyApp/1.0")
     *     .get();
     * }</pre>
     *
     * @param name1 The first header name
     * @param value1 The first header value
     * @param name2 The second header name
     * @param value2 The second header value
     * @param name3 The third header name
     * @param value3 The third header value
     * @return This HttpRequest instance for method chaining
     * @see HttpHeaders
     * @see HttpHeaders.Names
     * @see HttpHeaders.Values
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> headers = Map.of(
     *     "Authorization", "Bearer token123",
     *     "Content-Type", "application/json"
     * );
     * HttpRequest.url("https://api.example.com")
     *     .headers(headers)
     *     .get();
     * }</pre>
     *
     * @param headers A map containing header names and values
     * @return This HttpRequest instance for method chaining
     * @see HttpHeaders
     * @see HttpHeaders.Names
     * @see HttpHeaders.Values
     */
    public HttpRequest headers(final Map<String, ?> headers) {
        checkSettings();

        settings.headers(headers);

        return this;
    }

    /**
     * Removes all headers on this request and adds the specified headers.
     * This method replaces all existing headers with the provided HttpHeaders instance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpHeaders headers = HttpHeaders.create()
     *     .setAuthorization("Bearer token123")
     *     .setAccept("application/json");
     * HttpRequest.url("https://api.example.com")
     *     .headers(headers)
     *     .get();
     * }</pre>
     *
     * @param headers The HttpHeaders instance to set
     * @return This HttpRequest instance for method chaining
     * @see HttpHeaders
     * @see HttpHeaders.Names
     * @see HttpHeaders.Values
     */
    public HttpRequest headers(final HttpHeaders headers) {
        checkSettings();

        settings.headers(headers);

        return this;
    }

    /**
     * Sets the connection timeout in milliseconds for this HTTP request.
     * The connection timeout defines how long to wait when establishing a connection
     * to the remote server before timing out.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest.url("https://api.example.com")
     *     .connectionTimeout(5000) // 5 seconds
     *     .get();
     * }</pre>
     *
     * @param connectionTimeout The connection timeout in milliseconds. Must be non-negative.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest.url("https://api.example.com")
     *     .connectionTimeout(Duration.ofSeconds(5))
     *     .get();
     * }</pre>
     *
     * @param connectionTimeout The connection timeout as a Duration
     * @return This HttpRequest instance for method chaining
     */
    public HttpRequest connectionTimeout(final Duration connectionTimeout) {
        checkSettings();

        settings.setConnectionTimeout(connectionTimeout.toMillis());

        return this;
    }

    /**
     * Sets the read timeout in milliseconds for this HTTP request.
     * The read timeout defines how long to wait for data to be received from
     * the server after the connection has been established.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest.url("https://api.example.com")
     *     .readTimeout(10000) // 10 seconds
     *     .get();
     * }</pre>
     *
     * @param readTimeout The read timeout in milliseconds. Must be non-negative.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest.url("https://api.example.com")
     *     .readTimeout(Duration.ofSeconds(10))
     *     .get();
     * }</pre>
     *
     * @param readTimeout The read timeout as a Duration
     * @return This HttpRequest instance for method chaining
     */
    public HttpRequest readTimeout(final Duration readTimeout) {
        checkSettings();

        settings.setReadTimeout(readTimeout.toMillis());

        return this;
    }

    /**
     * Sets whether to use caches for this HTTP request.
     * When enabled, the request may use cached responses according to HTTP caching rules.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest.url("https://api.example.com/static-data")
     *     .useCaches(true)
     *     .get();
     * }</pre>
     *
     * @param useCaches {@code true} to enable caching, {@code false} to disable
     * @return This HttpRequest instance for method chaining
     */
    public HttpRequest useCaches(final boolean useCaches) {
        checkSettings();

        settings.setUseCaches(useCaches);

        return this;
    }

    /**
     * Sets the SSL socket factory for HTTPS connections.
     * This allows customization of SSL/TLS behavior, such as trusting specific certificates
     * or using custom trust managers.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SSLSocketFactory factory = createCustomSSLFactory();
     * HttpRequest.url("https://api.example.com")
     *     .sslSocketFactory(factory)
     *     .get();
     * }</pre>
     *
     * @param sslSocketFactory The SSL socket factory to use. Must not be {@code null}.
     * @return This HttpRequest instance for method chaining
     */
    public HttpRequest sslSocketFactory(final SSLSocketFactory sslSocketFactory) {
        checkSettings();

        settings.setSSLSocketFactory(sslSocketFactory);

        return this;
    }

    /**
     * Sets the proxy for this HTTP request.
     * This allows the request to be routed through a proxy server.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxy.example.com", 8080));
     * HttpRequest.url("https://api.example.com")
     *     .proxy(proxy)
     *     .get();
     * }</pre>
     *
     * @param proxy The proxy to use. Must not be {@code null}.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest.url("https://api.example.com/search")
     *     .query("q=test&page=1")
     *     .get();
     * }</pre>
     *
     * @param query The query string
     * @return This HttpRequest instance for method chaining
     */
    public HttpRequest query(final String query) {
        request = query;

        return this;
    }

    /**
     * Sets query parameters for GET or DELETE requests from a map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> params = Map.of("q", "test", "page", 1);
     * HttpRequest.url("https://api.example.com/search")
     *     .query(params)
     *     .get();
     * }</pre>
     *
     * @param queryParams A map containing query parameter names and values
     * @return This HttpRequest instance for method chaining
     */
    public HttpRequest query(final Map<String, ?> queryParams) {
        request = queryParams;

        return this;
    }

    /**
     * Sets the request body as JSON string and sets the Content-Type header to application/json.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest.url("https://api.example.com/users")
     *     .jsonBody("{\"name\":\"John\",\"age\":30}")
     *     .post();
     * }</pre>
     *
     * @param json The JSON string
     * @return This HttpRequest instance for method chaining
     */
    public HttpRequest jsonBody(final String json) {
        setContentType(HttpHeaders.Values.APPLICATION_JSON);

        request = json;

        return this;
    }

    /**
     * Sets the request body as JSON by serializing the specified object and sets the Content-Type header to application/json.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", "Doe");
     * HttpRequest.url("https://api.example.com/users")
     *     .jsonBody(user)
     *     .post();
     * }</pre>
     *
     * @param obj The object to serialize as JSON
     * @return This HttpRequest instance for method chaining
     */
    public HttpRequest jsonBody(final Object obj) {
        setContentType(HttpHeaders.Values.APPLICATION_JSON);

        request = N.toJson(obj);

        return this;
    }

    /**
     * Sets the request body as an XML string and sets the Content-Type header to application/xml.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String xml = "<user><name>John</name><age>30</age></user>";
     * HttpRequest.url("https://api.example.com/users")
     *     .xmlBody(xml)
     *     .post();
     * }</pre>
     *
     * @param xml The XML string to send as the request body
     * @return This HttpRequest instance for method chaining
     */
    public HttpRequest xmlBody(final String xml) {
        setContentType(HttpHeaders.Values.APPLICATION_XML);

        request = xml;

        return this;
    }

    /**
     * Sets the request body as XML by serializing the specified object and sets the Content-Type header to application/xml.
     * The object is serialized using the internal XML serialization mechanism.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", 30);
     * HttpRequest.url("https://api.example.com/users")
     *     .xmlBody(user)
     *     .post();
     * }</pre>
     *
     * @param obj The object to serialize as XML. Must not be {@code null}.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> formData = Map.of("username", "john", "password", "secret");
     * HttpRequest.url("https://api.example.com/login")
     *     .formBody(formData)
     *     .post();
     * }</pre>
     *
     * @param formBodyByMap A map containing form field names and values
     * @return This HttpRequest instance for method chaining
     */
    public HttpRequest formBody(final Map<?, ?> formBodyByMap) {
        setContentType(HttpHeaders.Values.APPLICATION_URL_ENCODED);

        request = formBodyByMap;

        return this;
    }

    /**
     * Sets the request body as form URL-encoded data from a bean object and sets the Content-Type header to application/x-www-form-urlencoded.
     * The bean's properties are extracted and encoded as form fields.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LoginForm form = new LoginForm("john", "secret");
     * HttpRequest.url("https://api.example.com/login")
     *     .formBody(form)
     *     .post();
     * }</pre>
     *
     * @param formBodyByBean A bean object whose properties will be used as form fields. Must not be {@code null}.
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
     * Sets the request body to the specified object.
     * The body can be a String, byte[], File, InputStream, Reader, or any object that will be serialized.
     * The Content-Type header should be set separately when using this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] data = {1, 2, 3, 4};
     * HttpRequest.url("https://api.example.com/upload")
     *     .header("Content-Type", "application/octet-stream")
     *     .body(data)
     *     .post();
     * }</pre>
     *
     * @param requestBody The request body. Accepted types include String, byte[], File, InputStream, Reader, or any serializable object.
     * @return This HttpRequest instance for method chaining
     */
    public HttpRequest body(final Object requestBody) {
        request = requestBody;

        return this;
    }

    /**
     * Executes a GET request and returns the response as an HttpResponse.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpResponse response = HttpRequest.url("https://api.example.com/data").get();
     * System.out.println("Status: " + response.statusCode());
     * System.out.println("Body: " + response.body(String.class));
     * }</pre>
     *
     * @return The HttpResponse object containing status, headers, and body
     * @throws UncheckedIOException if an I/O error occurs
     */
    public HttpResponse get() throws UncheckedIOException {
        return get(HttpResponse.class);
    }

    /**
     * Executes a GET request and deserializes the response to the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = HttpRequest.url("https://api.example.com/users/123")
     *     .get(User.class);
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object
     * @return The deserialized response object
     * @throws UncheckedIOException if an I/O error occurs
     */
    public <T> T get(final Class<T> resultClass) throws UncheckedIOException {
        return execute(HttpMethod.GET, resultClass);
    }

    /**
     * Executes a POST request and returns the response as an HttpResponse.
     * POST requests typically send data to the server to create or update a resource.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpResponse response = HttpRequest.url("https://api.example.com/users")
     *     .jsonBody(new User("John", "Doe"))
     *     .post();
     * }</pre>
     *
     * @return The HttpResponse object containing status code, headers, and response body
     * @throws UncheckedIOException if an I/O error occurs during the request
     */
    public HttpResponse post() throws UncheckedIOException {
        return post(HttpResponse.class);
    }

    /**
     * Executes a POST request and deserializes the response to the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User newUser = new User("John", "Doe");
     * User createdUser = HttpRequest.url("https://api.example.com/users")
     *     .jsonBody(newUser)
     *     .post(User.class);
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object
     * @return The deserialized response object
     * @throws UncheckedIOException if an I/O error occurs
     */
    public <T> T post(final Class<T> resultClass) throws UncheckedIOException {
        return execute(HttpMethod.POST, resultClass);
    }

    /**
     * Executes a PUT request and returns the response as an HttpResponse.
     * PUT requests typically replace an entire resource on the server.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpResponse response = HttpRequest.url("https://api.example.com/users/123")
     *     .jsonBody(updatedUser)
     *     .put();
     * }</pre>
     *
     * @return The HttpResponse object containing status code, headers, and response body
     * @throws UncheckedIOException if an I/O error occurs during the request
     */
    public HttpResponse put() throws UncheckedIOException {
        return put(HttpResponse.class);
    }

    /**
     * Executes a PUT request and deserializes the response to the specified type.
     * PUT requests typically replace an entire resource on the server.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User updatedUser = HttpRequest.url("https://api.example.com/users/123")
     *     .jsonBody(user)
     *     .put(User.class);
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object. Must not be {@code null}.
     * @return The deserialized response object
     * @throws UncheckedIOException if an I/O error occurs during the request
     */
    public <T> T put(final Class<T> resultClass) throws UncheckedIOException {
        return execute(HttpMethod.PUT, resultClass);
    }

    /**
     * Executes a DELETE request and returns the response as an HttpResponse.
     * DELETE requests remove a specified resource from the server.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpResponse response = HttpRequest.url("https://api.example.com/users/123")
     *     .delete();
     * }</pre>
     *
     * @return The HttpResponse object containing status code, headers, and response body
     * @throws UncheckedIOException if an I/O error occurs during the request
     */
    public HttpResponse delete() throws UncheckedIOException {
        return delete(HttpResponse.class);
    }

    /**
     * Executes a DELETE request and deserializes the response to the specified type.
     * DELETE requests remove a specified resource from the server.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DeleteResponse response = HttpRequest.url("https://api.example.com/users/123")
     *     .delete(DeleteResponse.class);
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object. Must not be {@code null}.
     * @return The deserialized response object
     * @throws UncheckedIOException if an I/O error occurs during the request
     */
    public <T> T delete(final Class<T> resultClass) throws UncheckedIOException {
        return execute(HttpMethod.DELETE, resultClass);
    }

    /**
     * Executes a HEAD request and returns the response as an HttpResponse.
     * HEAD requests retrieve only the headers (metadata) without the response body,
     * useful for checking resource existence or getting metadata without downloading the full content.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpResponse response = HttpRequest.url("https://api.example.com/users/123")
     *     .head();
     * System.out.println("Exists: " + (response.statusCode() == 200));
     * }</pre>
     *
     * @return The HttpResponse object containing status code and headers (body will be empty)
     * @throws UncheckedIOException if an I/O error occurs during the request
     */
    public HttpResponse head() throws UncheckedIOException {
        return head(HttpResponse.class);
    }

    /**
     * Executes a HEAD request with the specified result class.
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object
     * @return The deserialized response object (typically {@code null} for HEAD requests)
     * @throws UncheckedIOException if an I/O error occurs
     */
    <T> T head(final Class<T> resultClass) throws UncheckedIOException {
        return execute(HttpMethod.HEAD, resultClass);
    }

    /**
     * Executes an HTTP request with the specified method and returns the response as an HttpResponse.
     * This is a generic method that allows executing any HTTP method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpResponse response = HttpRequest.url("https://api.example.com/data")
     *     .execute(HttpMethod.GET);
     * }</pre>
     *
     * @param httpMethod The HTTP method to use (GET, POST, PUT, DELETE, HEAD, etc.). Must not be {@code null}.
     * @return The HttpResponse object containing status code, headers, and response body
     * @throws UncheckedIOException if an I/O error occurs during the request
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
     * Executes an HTTP request with the specified method and writes the response body to a file.
     * This is useful for downloading files or saving large responses directly to disk.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("response.json");
     * HttpRequest.url("https://api.example.com/large-data")
     *     .execute(HttpMethod.GET, file);
     * }</pre>
     *
     * @param httpMethod The HTTP method to use (GET, POST, PUT, DELETE, HEAD, etc.). Must not be {@code null}.
     * @param output The file to write the response body to. Must not be {@code null}.
     * @throws IllegalArgumentException if httpMethod is null
     * @throws UncheckedIOException if an I/O error occurs during the request or file writing
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
     * Executes an HTTP request with the specified method and writes the response body to an output stream.
     * The output stream is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteArrayOutputStream baos = new ByteArrayOutputStream();
     * HttpRequest.url("https://api.example.com/data")
     *     .execute(HttpMethod.GET, baos);
     * byte[] data = baos.toByteArray();
     * }</pre>
     *
     * @param httpMethod The HTTP method to use (GET, POST, PUT, DELETE, HEAD, etc.). Must not be {@code null}.
     * @param output The output stream to write the response body to. Must not be {@code null}.
     * @throws IllegalArgumentException if httpMethod is null
     * @throws UncheckedIOException if an I/O error occurs during the request or stream writing
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
     * Executes an HTTP request with the specified method and writes the response body to a writer.
     * The writer is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * StringWriter writer = new StringWriter();
     * HttpRequest.url("https://api.example.com/text")
     *     .execute(HttpMethod.GET, writer);
     * String text = writer.toString();
     * }</pre>
     *
     * @param httpMethod The HTTP method to use (GET, POST, PUT, DELETE, HEAD, etc.). Must not be {@code null}.
     * @param output The writer to write the response body to. Must not be {@code null}.
     * @throws IllegalArgumentException if httpMethod is null
     * @throws UncheckedIOException if an I/O error occurs during the request or writing
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest.url("https://api.example.com/data")
     *     .asyncGet()
     *     .getThenAccept(response -> System.out.println("Status: " + response.statusCode()));
     * }</pre>
     *
     * @return A ContinuableFuture that will complete with the HttpResponse
     */
    public ContinuableFuture<HttpResponse> asyncGet() {
        return asyncGet(HttpResponse.class);
    }

    /**
     * Executes an asynchronous GET request with a custom executor and returns a ContinuableFuture with the HttpResponse.
     * The request is executed on the provided executor's thread pool.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ExecutorService executor = Executors.newFixedThreadPool(4);
     * HttpRequest.url("https://api.example.com/data")
     *     .asyncGet(executor)
     *     .getThenAccept(response -> System.out.println("Got: " + response.statusCode()));
     * }</pre>
     *
     * @param executor The executor to use for the asynchronous operation. Must not be {@code null}.
     * @return A ContinuableFuture that will complete with the HttpResponse
     */
    public ContinuableFuture<HttpResponse> asyncGet(final Executor executor) {
        return asyncGet(HttpResponse.class, executor);
    }

    /**
     * Executes an asynchronous GET request and deserializes the response to the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest.url("https://api.example.com/users/123")
     *     .asyncGet(User.class)
     *     .getThenAccept(user -> System.out.println("User: " + user.getName()));
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object
     * @return A ContinuableFuture that will complete with the deserialized response
     */
    public <T> ContinuableFuture<T> asyncGet(final Class<T> resultClass) {
        return asyncExecute(HttpMethod.GET, resultClass);
    }

    /**
     * Executes an asynchronous GET request with a custom executor and deserializes the response to the specified type.
     * The request is executed on the provided executor's thread pool.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ExecutorService executor = Executors.newFixedThreadPool(4);
     * HttpRequest.url("https://api.example.com/users/123")
     *     .asyncGet(User.class, executor)
     *     .getThenAccept(user -> System.out.println("User: " + user.getName()));
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object. Must not be {@code null}.
     * @param executor The executor to use for the asynchronous operation. Must not be {@code null}.
     * @return A ContinuableFuture that will complete with the deserialized response
     */
    public <T> ContinuableFuture<T> asyncGet(final Class<T> resultClass, final Executor executor) {
        return asyncExecute(HttpMethod.GET, resultClass, executor);
    }

    /**
     * Executes an asynchronous POST request and returns a ContinuableFuture with the HttpResponse.
     * POST requests typically send data to the server to create or update a resource.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest.url("https://api.example.com/users")
     *     .jsonBody(new User("John", "Doe"))
     *     .asyncPost()
     *     .getThenAccept(response -> System.out.println("Created: " + response.statusCode()));
     * }</pre>
     *
     * @return A ContinuableFuture that will complete with the HttpResponse
     */
    public ContinuableFuture<HttpResponse> asyncPost() {
        return asyncPost(HttpResponse.class);
    }

    /**
     * Executes an asynchronous POST request with a custom executor and returns a ContinuableFuture with the HttpResponse.
     * The request is executed on the provided executor's thread pool.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ExecutorService executor = Executors.newFixedThreadPool(4);
     * HttpRequest.url("https://api.example.com/users")
     *     .jsonBody(newUser)
     *     .asyncPost(executor)
     *     .getThenAccept(response -> System.out.println("Status: " + response.statusCode()));
     * }</pre>
     *
     * @param executor The executor to use for the asynchronous operation. Must not be {@code null}.
     * @return A ContinuableFuture that will complete with the HttpResponse
     */
    public ContinuableFuture<HttpResponse> asyncPost(final Executor executor) {
        return asyncPost(HttpResponse.class, executor);
    }

    /**
     * Executes an asynchronous POST request and deserializes the response to the specified type.
     * POST requests typically send data to the server to create or update a resource.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest.url("https://api.example.com/users")
     *     .jsonBody(new User("John", "Doe"))
     *     .asyncPost(User.class)
     *     .getThenAccept(user -> System.out.println("Created user: " + user.getId()));
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object. Must not be {@code null}.
     * @return A ContinuableFuture that will complete with the deserialized response
     */
    public <T> ContinuableFuture<T> asyncPost(final Class<T> resultClass) {
        return asyncExecute(HttpMethod.POST, resultClass);
    }

    /**
     * Executes an asynchronous POST request with a custom executor and deserializes the response to the specified type.
     * The request is executed on the provided executor's thread pool.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ExecutorService executor = Executors.newFixedThreadPool(4);
     * HttpRequest.url("https://api.example.com/users")
     *     .jsonBody(newUser)
     *     .asyncPost(User.class, executor)
     *     .getThenAccept(user -> System.out.println("Created: " + user.getId()));
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object. Must not be {@code null}.
     * @param executor The executor to use for the asynchronous operation. Must not be {@code null}.
     * @return A ContinuableFuture that will complete with the deserialized response
     */
    public <T> ContinuableFuture<T> asyncPost(final Class<T> resultClass, final Executor executor) {
        return asyncExecute(HttpMethod.POST, resultClass, executor);
    }

    /**
     * Executes an asynchronous PUT request and returns a ContinuableFuture with the HttpResponse.
     * PUT requests typically replace an entire resource on the server.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest.url("https://api.example.com/users/123")
     *     .jsonBody(updatedUser)
     *     .asyncPut()
     *     .getThenAccept(response -> System.out.println("Updated: " + response.statusCode()));
     * }</pre>
     *
     * @return A ContinuableFuture that will complete with the HttpResponse
     */
    public ContinuableFuture<HttpResponse> asyncPut() {
        return asyncPut(HttpResponse.class);
    }

    /**
     * Executes an asynchronous PUT request with a custom executor and returns a ContinuableFuture with the HttpResponse.
     * The request is executed on the provided executor's thread pool.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ExecutorService executor = Executors.newFixedThreadPool(4);
     * HttpRequest.url("https://api.example.com/users/123")
     *     .jsonBody(updatedUser)
     *     .asyncPut(executor)
     *     .getThenAccept(response -> System.out.println("Status: " + response.statusCode()));
     * }</pre>
     *
     * @param executor The executor to use for the asynchronous operation. Must not be {@code null}.
     * @return A ContinuableFuture that will complete with the HttpResponse
     */
    public ContinuableFuture<HttpResponse> asyncPut(final Executor executor) {
        return asyncPut(HttpResponse.class, executor);
    }

    /**
     * Executes an asynchronous PUT request and deserializes the response to the specified type.
     * PUT requests typically replace an entire resource on the server.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest.url("https://api.example.com/users/123")
     *     .jsonBody(updatedUser)
     *     .asyncPut(User.class)
     *     .getThenAccept(user -> System.out.println("Updated user: " + user.getId()));
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object. Must not be {@code null}.
     * @return A ContinuableFuture that will complete with the deserialized response
     */
    public <T> ContinuableFuture<T> asyncPut(final Class<T> resultClass) {
        return asyncExecute(HttpMethod.PUT, resultClass);
    }

    /**
     * Executes an asynchronous PUT request with a custom executor and deserializes the response to the specified type.
     * The request is executed on the provided executor's thread pool.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ExecutorService executor = Executors.newFixedThreadPool(4);
     * HttpRequest.url("https://api.example.com/users/123")
     *     .jsonBody(updatedUser)
     *     .asyncPut(User.class, executor)
     *     .getThenAccept(user -> System.out.println("Updated: " + user.getId()));
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object. Must not be {@code null}.
     * @param executor The executor to use for the asynchronous operation. Must not be {@code null}.
     * @return A ContinuableFuture that will complete with the deserialized response
     */
    public <T> ContinuableFuture<T> asyncPut(final Class<T> resultClass, final Executor executor) {
        return asyncExecute(HttpMethod.PUT, resultClass, executor);
    }

    /**
     * Executes an asynchronous DELETE request and returns a ContinuableFuture with the HttpResponse.
     * DELETE requests remove a specified resource from the server.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest.url("https://api.example.com/users/123")
     *     .asyncDelete()
     *     .getThenAccept(response -> System.out.println("Deleted: " + response.statusCode()));
     * }</pre>
     *
     * @return A ContinuableFuture that will complete with the HttpResponse
     */
    public ContinuableFuture<HttpResponse> asyncDelete() {
        return asyncDelete(HttpResponse.class);
    }

    /**
     * Executes an asynchronous DELETE request with a custom executor and returns a ContinuableFuture with the HttpResponse.
     * The request is executed on the provided executor's thread pool.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ExecutorService executor = Executors.newFixedThreadPool(4);
     * HttpRequest.url("https://api.example.com/users/123")
     *     .asyncDelete(executor)
     *     .getThenAccept(response -> System.out.println("Status: " + response.statusCode()));
     * }</pre>
     *
     * @param executor The executor to use for the asynchronous operation. Must not be {@code null}.
     * @return A ContinuableFuture that will complete with the HttpResponse
     */
    public ContinuableFuture<HttpResponse> asyncDelete(final Executor executor) {
        return asyncDelete(HttpResponse.class, executor);
    }

    /**
     * Executes an asynchronous DELETE request and deserializes the response to the specified type.
     * DELETE requests remove a specified resource from the server.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest.url("https://api.example.com/users/123")
     *     .asyncDelete(DeleteResponse.class)
     *     .getThenAccept(response -> System.out.println("Deleted: " + response.isSuccess()));
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object. Must not be {@code null}.
     * @return A ContinuableFuture that will complete with the deserialized response
     */
    public <T> ContinuableFuture<T> asyncDelete(final Class<T> resultClass) {
        return asyncExecute(HttpMethod.DELETE, resultClass);
    }

    /**
     * Executes an asynchronous DELETE request with a custom executor and deserializes the response to the specified type.
     * The request is executed on the provided executor's thread pool.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ExecutorService executor = Executors.newFixedThreadPool(4);
     * HttpRequest.url("https://api.example.com/users/123")
     *     .asyncDelete(DeleteResponse.class, executor)
     *     .getThenAccept(response -> System.out.println("Deleted: " + response.isSuccess()));
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object. Must not be {@code null}.
     * @param executor The executor to use for the asynchronous operation. Must not be {@code null}.
     * @return A ContinuableFuture that will complete with the deserialized response
     */
    public <T> ContinuableFuture<T> asyncDelete(final Class<T> resultClass, final Executor executor) {
        return asyncExecute(HttpMethod.DELETE, resultClass, executor);
    }

    /**
     * Executes an asynchronous HEAD request and returns a ContinuableFuture with the HttpResponse.
     * HEAD requests retrieve only the headers (metadata) without the response body.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest.url("https://api.example.com/users/123")
     *     .asyncHead()
     *     .getThenAccept(response -> System.out.println("Exists: " + (response.statusCode() == 200)));
     * }</pre>
     *
     * @return A ContinuableFuture that will complete with the HttpResponse
     */
    public ContinuableFuture<HttpResponse> asyncHead() {
        return asyncHead(HttpResponse.class);
    }

    /**
     * Executes an asynchronous HEAD request with a custom executor and returns a ContinuableFuture with the HttpResponse.
     * The request is executed on the provided executor's thread pool. HEAD requests retrieve only headers without the body.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ExecutorService executor = Executors.newFixedThreadPool(4);
     * HttpRequest.url("https://api.example.com/file")
     *     .asyncHead(executor)
     *     .getThenAccept(response -> System.out.println("Size: " + response.getContentLength()));
     * }</pre>
     *
     * @param executor The executor to use for the asynchronous operation. Must not be {@code null}.
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
     * <p><b>Note:</b> This method has unusual parameter ordering (executor, resultClass) compared to
     * other async methods which use (resultClass, executor). This is maintained for backward compatibility.</p>
     *
     * @param <T> The type of the response object
     * @param executor The executor to use for the asynchronous operation. Must not be {@code null}.
     * @param resultClass The class of the expected response object. Must not be {@code null}.
     * @return A ContinuableFuture that will complete with the response
     */
    public <T> ContinuableFuture<T> asyncHead(final Executor executor, final Class<T> resultClass) {
        return asyncExecute(HttpMethod.HEAD, resultClass, executor);
    }

    /**
     * Executes an asynchronous HTTP request with the specified method and returns a ContinuableFuture with the HttpResponse.
     * This is a generic method that allows executing any HTTP method asynchronously.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest.url("https://api.example.com/data")
     *     .asyncExecute(HttpMethod.GET)
     *     .getThenAccept(response -> System.out.println("Status: " + response.statusCode()));
     * }</pre>
     *
     * @param httpMethod The HTTP method to use (GET, POST, PUT, DELETE, HEAD, etc.). Must not be {@code null}.
     * @return A ContinuableFuture that will complete with the HttpResponse
     */
    @Beta
    public ContinuableFuture<HttpResponse> asyncExecute(final HttpMethod httpMethod) {
        return asyncExecute(httpMethod, HttpResponse.class);
    }

    /**
     * Executes an asynchronous HTTP request with the specified method and a custom executor.
     * The request is executed on the provided executor's thread pool.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ExecutorService executor = Executors.newFixedThreadPool(4);
     * HttpRequest.url("https://api.example.com/data")
     *     .asyncExecute(HttpMethod.GET, executor)
     *     .getThenAccept(response -> System.out.println("Status: " + response.statusCode()));
     * }</pre>
     *
     * @param httpMethod The HTTP method to use (GET, POST, PUT, DELETE, HEAD, etc.). Must not be {@code null}.
     * @param executor The executor to use for the asynchronous operation. Must not be {@code null}.
     * @return A ContinuableFuture that will complete with the HttpResponse
     */
    @Beta
    public ContinuableFuture<HttpResponse> asyncExecute(final HttpMethod httpMethod, final Executor executor) {
        return asyncExecute(httpMethod, HttpResponse.class, executor);
    }

    /**
     * Executes an asynchronous HTTP request with the specified method and deserializes the response to the specified type.
     * This is a generic method that allows executing any HTTP method asynchronously with automatic deserialization.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest.url("https://api.example.com/users/123")
     *     .asyncExecute(HttpMethod.GET, User.class)
     *     .getThenAccept(user -> System.out.println("User: " + user.getName()));
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param httpMethod The HTTP method to use (GET, POST, PUT, DELETE, HEAD, etc.). Must not be {@code null}.
     * @param resultClass The class of the expected response object. Must not be {@code null}.
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
     * Executes an asynchronous HTTP request with the specified method, custom executor, and deserializes the response to the specified type.
     * The request is executed on the provided executor's thread pool.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ExecutorService executor = Executors.newFixedThreadPool(4);
     * HttpRequest.url("https://api.example.com/users/123")
     *     .asyncExecute(HttpMethod.GET, User.class, executor)
     *     .getThenAccept(user -> System.out.println("User: " + user.getName()));
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param httpMethod The HTTP method to use (GET, POST, PUT, DELETE, HEAD, etc.). Must not be {@code null}.
     * @param resultClass The class of the expected response object. Must not be {@code null}.
     * @param executor The executor to use for the asynchronous operation. Must not be {@code null}.
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
     * Executes an asynchronous HTTP request and writes the response body to a file.
     * This is useful for downloading files asynchronously without blocking.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("data.json");
     * HttpRequest.url("https://api.example.com/large-data")
     *     .asyncExecute(HttpMethod.GET, file)
     *     .thenRun(() -> System.out.println("Download complete"));
     * }</pre>
     *
     * @param httpMethod The HTTP method to use (GET, POST, PUT, DELETE, HEAD, etc.). Must not be {@code null}.
     * @param output The file to write the response body to. Must not be {@code null}.
     * @return A ContinuableFuture that will complete when the file is written successfully
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
     * Executes an asynchronous HTTP request with a custom executor and writes the response body to a file.
     * The request is executed on the provided executor's thread pool.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ExecutorService executor = Executors.newFixedThreadPool(4);
     * File file = new File("data.json");
     * HttpRequest.url("https://api.example.com/large-data")
     *     .asyncExecute(HttpMethod.GET, file, executor)
     *     .thenRun(() -> System.out.println("Download complete"));
     * }</pre>
     *
     * @param httpMethod The HTTP method to use (GET, POST, PUT, DELETE, HEAD, etc.). Must not be {@code null}.
     * @param output The file to write the response body to. Must not be {@code null}.
     * @param executor The executor to use for the asynchronous operation. Must not be {@code null}.
     * @return A ContinuableFuture that will complete when the file is written successfully
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
     * Executes an asynchronous HTTP request and writes the response body to an output stream.
     * The output stream is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteArrayOutputStream baos = new ByteArrayOutputStream();
     * HttpRequest.url("https://api.example.com/data")
     *     .asyncExecute(HttpMethod.GET, baos)
     *     .thenRun(() -> System.out.println("Downloaded " + baos.size() + " bytes"));
     * }</pre>
     *
     * @param httpMethod The HTTP method to use (GET, POST, PUT, DELETE, HEAD, etc.). Must not be {@code null}.
     * @param output The output stream to write the response body to. Must not be {@code null}.
     * @return A ContinuableFuture that will complete when the stream is written successfully
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
     * Executes an asynchronous HTTP request with a custom executor and writes the response body to an output stream.
     * The request is executed on the provided executor's thread pool. The output stream is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ExecutorService executor = Executors.newFixedThreadPool(4);
     * ByteArrayOutputStream baos = new ByteArrayOutputStream();
     * HttpRequest.url("https://api.example.com/data")
     *     .asyncExecute(HttpMethod.GET, baos, executor)
     *     .thenRun(() -> System.out.println("Downloaded " + baos.size() + " bytes"));
     * }</pre>
     *
     * @param httpMethod The HTTP method to use (GET, POST, PUT, DELETE, HEAD, etc.). Must not be {@code null}.
     * @param output The output stream to write the response body to. Must not be {@code null}.
     * @param executor The executor to use for the asynchronous operation. Must not be {@code null}.
     * @return A ContinuableFuture that will complete when the stream is written successfully
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
     * Executes an asynchronous HTTP request and writes the response body to a writer.
     * The writer is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * StringWriter writer = new StringWriter();
     * HttpRequest.url("https://api.example.com/text")
     *     .asyncExecute(HttpMethod.GET, writer)
     *     .thenRun(() -> System.out.println("Content: " + writer.toString()));
     * }</pre>
     *
     * @param httpMethod The HTTP method to use (GET, POST, PUT, DELETE, HEAD, etc.). Must not be {@code null}.
     * @param output The writer to write the response body to. Must not be {@code null}.
     * @return A ContinuableFuture that will complete when the writer is written successfully
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
     * Executes an asynchronous HTTP request with a custom executor and writes the response body to a writer.
     * The request is executed on the provided executor's thread pool. The writer is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ExecutorService executor = Executors.newFixedThreadPool(4);
     * StringWriter writer = new StringWriter();
     * HttpRequest.url("https://api.example.com/text")
     *     .asyncExecute(HttpMethod.GET, writer, executor)
     *     .thenRun(() -> System.out.println("Content: " + writer.toString()));
     * }</pre>
     *
     * @param httpMethod The HTTP method to use (GET, POST, PUT, DELETE, HEAD, etc.). Must not be {@code null}.
     * @param output The writer to write the response body to. Must not be {@code null}.
     * @param executor The executor to use for the asynchronous operation. Must not be {@code null}.
     * @return A ContinuableFuture that will complete when the writer is written successfully
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
