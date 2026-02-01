/*
 * Copyright (C) 2023 HaiYang Li
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
 * A fluent HTTP request builder and executor based on Java 11+ HttpClient.
 * This class provides a convenient API for building and executing HTTP requests with various features
 * such as headers, query parameters, request bodies, authentication, and timeouts.
 * 
 * <p>This implementation uses the modern Java HttpClient introduced in Java 11, providing better performance
 * and support for HTTP/2 compared to older HTTP clients.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Simple GET request
 * HttpResponse<String> response = HttpRequest.url("https://api.example.com/users")
 *     .header("Accept", "application/json")
 *     .get();
 *
 * // POST request with JSON body
 * User user = new User("John", "Doe");
 * User createdUser = HttpRequest.url("https://api.example.com/users")
 *     .jsonBody(user)
 *     .post(User.class);
 *
 * // Asynchronous request
 * CompletableFuture<String> future = HttpRequest.url("https://api.example.com/data")
 *     .asyncGet(String.class);
 * }</pre>
 *
 * @see URLEncodedUtil
 * @see HttpHeaders
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
        N.checkArgument(!(Strings.isEmpty(url) && uri == null), "'uri' or 'url' cannot be null or empty");

        this.url = url;
        this.uri = uri;
        this.httpClient = httpClient;
        this.clientBuilder = clientBuilder;
        this.requestBuilder = N.checkArgNotNull(requestBuilder);
    }

    /**
     * Creates a new HttpRequest instance with the specified URL and HTTP client.
     *
     * @param url the URL string for the request
     * @param httpClient the HttpClient to use for executing the request
     * @return a new HttpRequest instance
     * @throws IllegalArgumentException if the scheme of {@code url} is not {@code http} or {@code https}.
     */
    public static HttpRequest create(final String url, final HttpClient httpClient) {
        return new HttpRequest(url, null, httpClient, null, java.net.http.HttpRequest.newBuilder()).closeHttpClientAfterExecution(false);
    }

    /**
     * Creates a new HttpRequest instance with the specified URL and HTTP client.
     *
     * @param url the URL object for the request
     * @param httpClient the HttpClient to use for executing the request
     * @return a new HttpRequest instance
     * @throws IllegalArgumentException if the scheme of {@code url} is not {@code http} or {@code https}.
     */
    public static HttpRequest create(final URL url, final HttpClient httpClient) {
        return new HttpRequest(url.toString(), null, httpClient, null, java.net.http.HttpRequest.newBuilder()).closeHttpClientAfterExecution(false);
    }

    /**
     * Creates a new HttpRequest instance with the specified URI and HTTP client.
     *
     * @param uri the URI object for the request
     * @param httpClient the HttpClient to use for executing the request
     * @return a new HttpRequest instance
     * @throws IllegalArgumentException if the scheme of {@code url} is not {@code http} or {@code https}.
     */
    public static HttpRequest create(final URI uri, final HttpClient httpClient) {
        return new HttpRequest(null, uri, httpClient, null, java.net.http.HttpRequest.newBuilder()).closeHttpClientAfterExecution(false);
    }

    /**
     * Creates a new HttpRequest instance with the specified URL using the default HTTP client.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest request = HttpRequest.url("https://api.example.com/users");
     * }</pre>
     *
     * @param url the URL string for the request
     * @return a new HttpRequest instance
     * @throws IllegalArgumentException if the scheme of {@code url} is not {@code http} or {@code https}.
     */
    public static HttpRequest url(final String url) {
        return new HttpRequest(url, null, DEFAULT_HTTP_CLIENT, null, java.net.http.HttpRequest.newBuilder()).closeHttpClientAfterExecution(false);
    }

    /**
     * Creates a new HttpRequest instance with the specified URL and timeout settings.
     * A new HTTP client is created with the specified timeouts and will be closed after execution.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest request = HttpRequest.url("https://api.example.com/data", 5000, 30000);
     * }</pre>
     *
     * @param url the URL string for the request
     * @param connectTimeoutInMillis the connection timeout in milliseconds
     * @param readTimeoutInMillis the read timeout in milliseconds
     * @return a new HttpRequest instance
     */
    public static HttpRequest url(final String url, final long connectTimeoutInMillis, final long readTimeoutInMillis) {
        return new HttpRequest(url, null, null, HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connectTimeoutInMillis)),
                java.net.http.HttpRequest.newBuilder().timeout(Duration.ofMillis(readTimeoutInMillis))).closeHttpClientAfterExecution(true);
    }

    /**
     * Creates a new HttpRequest instance with the specified URL using the default HTTP client.
     *
     * @param url the URL object for the request
     * @return a new HttpRequest instance
     * @throws IllegalArgumentException if the scheme of {@code url} is not {@code http} or {@code https}.
     */
    public static HttpRequest url(final URL url) {
        return new HttpRequest(url.toString(), null, DEFAULT_HTTP_CLIENT, null, java.net.http.HttpRequest.newBuilder()).closeHttpClientAfterExecution(false);
    }

    /**
     * Creates a new HttpRequest instance with the specified URL and timeout settings.
     * A new HTTP client is created with the specified timeouts and will be closed after execution.
     *
     * @param url the URL object for the request
     * @param connectTimeoutInMillis the connection timeout in milliseconds
     * @param readTimeoutInMillis the read timeout in milliseconds
     * @return a new HttpRequest instance
     */
    public static HttpRequest url(final URL url, final long connectTimeoutInMillis, final long readTimeoutInMillis) {
        return new HttpRequest(url.toString(), null, null, HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connectTimeoutInMillis)),
                java.net.http.HttpRequest.newBuilder().timeout(Duration.ofMillis(readTimeoutInMillis))).closeHttpClientAfterExecution(true);
    }

    /**
     * Creates a new HttpRequest instance with the specified URI using the default HTTP client.
     *
     * @param uri the URI object for the request
     * @return a new HttpRequest instance
     * @throws IllegalArgumentException if the scheme of {@code url} is not {@code http} or {@code https}.
     */
    public static HttpRequest url(final URI uri) {
        return new HttpRequest(null, uri, DEFAULT_HTTP_CLIENT, null, java.net.http.HttpRequest.newBuilder()).closeHttpClientAfterExecution(false);
    }

    /**
     * Creates a new HttpRequest instance with the specified URI and timeout settings.
     * A new HTTP client is created with the specified timeouts and will be closed after execution.
     *
     * @param uri the URI object for the request
     * @param connectTimeoutInMillis the connection timeout in milliseconds
     * @param readTimeoutInMillis the read timeout in milliseconds
     * @return a new HttpRequest instance
     */
    public static HttpRequest url(final URI uri, final long connectTimeoutInMillis, final long readTimeoutInMillis) {
        return new HttpRequest(null, uri, null, HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connectTimeoutInMillis)),
                java.net.http.HttpRequest.newBuilder().timeout(Duration.ofMillis(readTimeoutInMillis))).closeHttpClientAfterExecution(true);
    }

    HttpRequest closeHttpClientAfterExecution(final boolean shouldClose) {
        closeHttpClientAfterExecution = shouldClose;

        return this;
    }

    /**
     * Sets the connection timeout for this request.
     * This creates a new HttpClient builder if one doesn't exist, or copies settings from the existing client.
     * The connection timeout is the maximum time to wait when establishing a connection to the server.
     * If the connection cannot be established within this timeout, the request will fail.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest.url("https://api.example.com/data")
     *     .connectTimeout(Duration.ofSeconds(10))
     *     .get();
     * }</pre>
     *
     * @param connectTimeout the connection timeout duration
     * @return this HttpRequest instance for method chaining
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

        if (httpClient != null && !requireNewClient) {
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
     * Sets the read timeout for this request.
     * The read timeout is the maximum time to wait for data to be read from the server.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest.url("https://api.example.com/slow-endpoint")
     *     .readTimeout(Duration.ofSeconds(60))
     *     .get();
     * }</pre>
     *
     * @param readTimeout the read timeout duration
     * @return this HttpRequest instance for method chaining
     */
    public HttpRequest readTimeout(final Duration readTimeout) {
        requestBuilder.timeout(readTimeout);

        return this;
    }

    /**
     * Sets the authenticator for this request.
     * The authenticator will be used to provide credentials when the server requests authentication
     * (e.g., HTTP Basic or Digest authentication). This is useful for scenarios requiring dynamic
     * credential retrieval or advanced authentication mechanisms.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Authenticator auth = new Authenticator() {
     *     @Override
     *     protected PasswordAuthentication getPasswordAuthentication() {
     *         return new PasswordAuthentication("username", "password".toCharArray());
     *     }
     * };
     *
     * HttpRequest.url("https://api.example.com/secure")
     *     .authenticator(auth)
     *     .get();
     * }</pre>
     *
     * @param authenticator the authenticator to use for providing credentials
     * @return this HttpRequest instance for method chaining
     * @see #basicAuth(String, Object)
     */
    public HttpRequest authenticator(final Authenticator authenticator) {
        initClientBuilder();

        clientBuilder.authenticator(authenticator);

        return this;
    }

    /**
     * Sets the Basic Authentication header for this request.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest.url("https://api.example.com/secure")
     *     .basicAuth("username", "password")
     *     .get();
     * }</pre>
     *
     * @param username the username for authentication
     * @param password the password for authentication
     * @return this HttpRequest instance for method chaining
     * @see HttpHeaders
     * @see HttpHeaders.Names
     * @see HttpHeaders.Values
     */
    public HttpRequest basicAuth(final String username, final Object password) {
        header(HttpHeaders.Names.AUTHORIZATION, "Basic " + Strings.base64Encode((username + ":" + password).getBytes(Charsets.UTF_8)));

        return this;
    }

    /**
     * Sets the HTTP header specified by {@code name/value}.
     * If this HttpRequest already has any headers with that name, they are all replaced.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest.url("https://api.example.com/data")
     *     .header("Accept", "application/json")
     *     .header("User-Agent", "MyApp/1.0")
     *     .get();
     * }</pre>
     *
     * @param name the header name
     * @param value the header value (will be converted to string)
     * @return this HttpRequest instance for method chaining
     * @see HttpHeaders
     * @see HttpHeaders.Names
     * @see HttpHeaders.Values
     */
    public HttpRequest header(final String name, final Object value) {
        requestBuilder.header(name, HttpHeaders.valueOf(value));

        return this;
    }

    /**
     * Sets HTTP headers specified by {@code name1/value1}, {@code name2/value2}.
     * If this HttpRequest already has any headers with those names, they are all replaced.
     * This is a convenience method for setting multiple headers in one call.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest.url("https://api.example.com/data")
     *     .headers("Accept", "application/json", "User-Agent", "MyApp/1.0")
     *     .get();
     * }</pre>
     *
     * @param name1 the first header name
     * @param value1 the first header value (will be converted to string)
     * @param name2 the second header name
     * @param value2 the second header value (will be converted to string)
     * @return this HttpRequest instance for method chaining
     * @see HttpHeaders
     * @see HttpHeaders.Names
     * @see HttpHeaders.Values
     */
    public HttpRequest headers(final String name1, final Object value1, final String name2, final Object value2) {
        return header(name1, value1).header(name2, value2);
    }

    /**
     * Sets HTTP headers specified by {@code name1/value1}, {@code name2/value2}, {@code name3/value3}.
     * If this HttpRequest already has any headers with those names, they are all replaced.
     * This is a convenience method for setting multiple headers in one call.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest.url("https://api.example.com/data")
     *     .headers("Accept", "application/json",
     *              "User-Agent", "MyApp/1.0",
     *              "X-Custom-Header", "custom-value")
     *     .get();
     * }</pre>
     *
     * @param name1 the first header name
     * @param value1 the first header value (will be converted to string)
     * @param name2 the second header name
     * @param value2 the second header value (will be converted to string)
     * @param name3 the third header name
     * @param value3 the third header value (will be converted to string)
     * @return this HttpRequest instance for method chaining
     * @see HttpHeaders
     * @see HttpHeaders.Names
     * @see HttpHeaders.Values
     */
    public HttpRequest headers(final String name1, final Object value1, final String name2, final Object value2, final String name3, final Object value3) {
        return header(name1, value1).header(name2, value2).header(name3, value3);
    }

    /**
     * Sets HTTP headers specified by the key/value entries from the provided Map.
     * If this HttpRequest already has any headers with those names, they are all replaced.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> headers = new HashMap<>();
     * headers.put("Accept", "application/json");
     * headers.put("Authorization", "Bearer token123");
     * 
     * HttpRequest.url("https://api.example.com/data")
     *     .headers(headers)
     *     .get();
     * }</pre>
     *
     * @param headers a map containing header names and values
     * @return this HttpRequest instance for method chaining
     * @see HttpHeaders
     * @see HttpHeaders.Names
     * @see HttpHeaders.Values
     */
    public HttpRequest headers(final Map<String, ?> headers) {
        if (N.notEmpty(headers)) {
            for (final Map.Entry<String, ?> entry : headers.entrySet()) {
                header(entry.getKey(), entry.getValue());
            }
        }

        return this;
    }

    //    /**
    //     * Sets HTTP headers from a provided HttpHeaders object.
    //     * If this HttpRequest already has any headers with those names, they are all replaced.
    //     * The existing headers in this HttpRequest but not in the provided HttpHeaders remain unchanged.
    //     * This is useful when you have a collection of headers to apply from another source.
    //     *
    //     * <p><b>Usage Examples:</b></p>
    //     * <pre>{@code
    //     * HttpHeaders headers = new HttpHeaders();
    //     * headers.set("Accept", "application/json");
    //     * headers.set("Authorization", "Bearer token123");
    //     *
    //     * HttpRequest.url("https://api.example.com/data")
    //     *     .headers(headers)
    //     *     .get();
    //     * }</pre>
    //     *
    //     * @param headers the HttpHeaders object containing all headers to set
    //     * @return this HttpRequest instance for method chaining
    //     * @see HttpHeaders
    //     * @see HttpHeaders.Names
    //     * @see HttpHeaders.Values
    //     * @see HttpHeaders#toMap()
    //     * @deprecated use {@link #headers(Map)} instead. Due to limitations of java.net.http.HttpRequest.Builder, the existing headers in this HttpRequest can't be removed. 
    //     *                  This is inconsistent with the similar methods {@link OkHttpRequest#headers(HttpHeaders)} and {@link com.landawn.abacus.http.HttpRequest#headers(HttpHeaders)}.
    //     */
    //    public HttpRequest headers(final HttpHeaders headers) {
    //        if (headers != null && !headers.isEmpty()) {
    //            headers.forEach(this::header);
    //        }
    //
    //        return this;
    //    }

    /**
     * Sets query parameters for {@code GET} or {@code DELETE} request.
     * The query string will be appended to the URL.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest.url("https://api.example.com/search")
     *     .query("q=java&limit=10")
     *     .get();
     * }</pre>
     *
     * @param query the query string
     * @return this HttpRequest instance for method chaining
     */
    public HttpRequest query(final String query) {
        this.query = query;

        return this;
    }

    /**
     * Sets query parameters for {@code GET} or {@code DELETE} request.
     * The parameters will be URL-encoded and appended to the URL.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> params = new HashMap<>();
     * params.put("q", "java programming");
     * params.put("limit", 10);
     * 
     * HttpRequest.url("https://api.example.com/search")
     *     .query(params)
     *     .get();
     * }</pre>
     *
     * @param queryParams a map containing query parameter names and values
     * @return this HttpRequest instance for method chaining
     */
    public HttpRequest query(final Map<String, ?> queryParams) {
        query = queryParams;

        return this;
    }

    /**
     * Sets the request body as JSON with Content-Type: application/json.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String json = "{\"name\":\"John\",\"age\":30}";
     * HttpRequest.url("https://api.example.com/users")
     *     .jsonBody(json)
     *     .post();
     * }</pre>
     *
     * @param json the JSON string to send as the request body
     * @return this HttpRequest instance for method chaining
     */
    public HttpRequest jsonBody(final String json) {
        setContentType(HttpHeaders.Values.APPLICATION_JSON);

        bodyPublisher = BodyPublishers.ofString(json);

        return this;
    }

    /**
     * Sets the request body as JSON with Content-Type: application/json.
     * The object will be serialized to JSON using the default JSON serializer.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", 30);
     * HttpRequest.url("https://api.example.com/users")
     *     .jsonBody(user)
     *     .post();
     * }</pre>
     *
     * @param obj the object to serialize to JSON and send as the request body
     * @return this HttpRequest instance for method chaining
     */
    public HttpRequest jsonBody(final Object obj) {
        setContentType(HttpHeaders.Values.APPLICATION_JSON);

        bodyPublisher = BodyPublishers.ofString(N.toJson(obj));

        return this;
    }

    /**
     * Sets the request body as XML with Content-Type: application/xml.
     * The provided XML string will be sent as-is in the request body.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String xml = "<user><name>John</name><age>30</age></user>";
     * HttpRequest.url("https://api.example.com/users")
     *     .xmlBody(xml)
     *     .post();
     * }</pre>
     *
     * @param xml the XML string to send as the request body
     * @return this HttpRequest instance for method chaining
     * @see #xmlBody(Object)
     */
    public HttpRequest xmlBody(final String xml) {
        setContentType(HttpHeaders.Values.APPLICATION_XML);

        bodyPublisher = BodyPublishers.ofString(xml);

        return this;
    }

    /**
     * Sets the request body as XML with Content-Type: application/xml.
     * The object will be serialized to XML using the default XML serializer.
     * This is useful when you have a POJO that you want to send as XML.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", 30);
     * HttpRequest.url("https://api.example.com/users")
     *     .xmlBody(user)
     *     .post();
     * }</pre>
     *
     * @param obj the object to serialize to XML and send as the request body
     * @return this HttpRequest instance for method chaining
     * @see #xmlBody(String)
     */
    public HttpRequest xmlBody(final Object obj) {
        setContentType(HttpHeaders.Values.APPLICATION_XML);

        bodyPublisher = BodyPublishers.ofString(N.toXml(obj));

        return this;
    }

    /**
     * Sets the request body as form data with Content-Type: application/x-www-form-urlencoded.
     * The map entries will be encoded as form fields.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> formData = new HashMap<>();
     * formData.put("username", "john_doe");
     * formData.put("password", "secret123");
     * 
     * HttpRequest.url("https://api.example.com/login")
     *     .formBody(formData)
     *     .post();
     * }</pre>
     *
     * @param formBodyByMap a map containing form field names and values
     * @return this HttpRequest instance for method chaining
     */
    public HttpRequest formBody(final Map<?, ?> formBodyByMap) {
        setContentType(HttpHeaders.Values.APPLICATION_URL_ENCODED);

        bodyPublisher = BodyPublishers.ofString(URLEncodedUtil.encode(formBodyByMap));

        return this;
    }

    /**
     * Sets the request body as form data with Content-Type: application/x-www-form-urlencoded.
     * The bean properties will be encoded as form fields using getter methods.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LoginRequest login = new LoginRequest();
     * login.setUsername("john_doe");
     * login.setPassword("secret123");
     * 
     * HttpRequest.url("https://api.example.com/login")
     *     .formBody(login)
     *     .post();
     * }</pre>
     *
     * @param formBodyByBean a bean object whose properties will be used as form fields
     * @return this HttpRequest instance for method chaining
     */
    public HttpRequest formBody(final Object formBodyByBean) {
        setContentType(HttpHeaders.Values.APPLICATION_URL_ENCODED);

        bodyPublisher = BodyPublishers.ofString(URLEncodedUtil.encode(formBodyByBean));

        return this;
    }

    private void setContentType(final String contentType) {
        header(HttpHeaders.Names.CONTENT_TYPE, contentType);
    }

    /**
     * Sets the request body with a custom BodyPublisher instance.
     * This allows full control over the request body content and is useful for advanced scenarios
     * such as streaming data, multipart form data, or custom content types.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BodyPublisher publisher = BodyPublishers.ofFile(Paths.get("data.bin"));
     * HttpRequest.url("https://api.example.com/upload")
     *     .header("Content-Type", "application/octet-stream")
     *     .body(publisher)
     *     .post();
     * }</pre>
     *
     * @param bodyPublisher the BodyPublisher to use for sending the request body
     * @return this HttpRequest instance for method chaining
     * @see java.net.http.HttpRequest.BodyPublishers
     */
    public HttpRequest body(final BodyPublisher bodyPublisher) {
        this.bodyPublisher = bodyPublisher;

        return this;
    }

    /**
     * Executes a GET request and returns the response with a String body.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpResponse<String> response = HttpRequest.url("https://api.example.com/users")
     *     .header("Accept", "application/json")
     *     .get();
     * 
     * if (response.statusCode() == 200) {
     *     String body = response.body();
     * }
     * }</pre>
     *
     * @return the HTTP response with String body
     * @throws UncheckedIOException if the request could not be executed
     */
    public HttpResponse<String> get() throws UncheckedIOException {
        return get(BodyHandlers.ofString());
    }

    /**
     * Executes a GET request with a custom response body handler.
     * This allows you to control how the response body is processed, such as saving to a file,
     * reading as bytes, or handling as an input stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpResponse<byte[]> response = HttpRequest.url("https://api.example.com/image.png")
     *     .get(BodyHandlers.ofByteArray());
     * byte[] imageData = response.body();
     * }</pre>
     *
     * @param <T> the response body type
     * @param responseBodyHandler the handler for processing the response body
     * @return the HTTP response with the processed body
     * @throws UncheckedIOException if the request could not be executed
     * @see java.net.http.HttpResponse.BodyHandlers
     */
    public <T> HttpResponse<T> get(final HttpResponse.BodyHandler<T> responseBodyHandler) throws UncheckedIOException {
        return execute(HttpMethod.GET, responseBodyHandler);
    }

    /**
     * Executes a GET request and returns the response body deserialized to the specified type.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<User> users = HttpRequest.url("https://api.example.com/users")
     *     .get(new TypeToken<List<User>>(){}.getType());
     * }</pre>
     *
     * @param <T> the type of the result
     * @param resultClass the class of the result type
     * @return the deserialized response body
     * @throws UncheckedIOException if the request could not be executed or the response indicates an error
     */
    public <T> T get(final Class<T> resultClass) throws UncheckedIOException {
        final BodyHandler<?> responseBodyHandler = createResponseBodyHandler(resultClass);

        return getBody(get(responseBodyHandler), resultClass);
    }

    /**
     * Executes a POST request and returns the response with a String body.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User newUser = new User("John", "Doe");
     * HttpResponse<String> response = HttpRequest.url("https://api.example.com/users")
     *     .jsonBody(newUser)
     *     .post();
     * }</pre>
     *
     * @return the HTTP response with String body
     * @throws UncheckedIOException if the request could not be executed
     */
    public HttpResponse<String> post() {
        return post(BodyHandlers.ofString());
    }

    /**
     * Executes a POST request with a custom response body handler.
     * This allows you to control how the response body is processed, such as saving to a file,
     * reading as bytes, or handling as an input stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpResponse<InputStream> response = HttpRequest.url("https://api.example.com/data")
     *     .jsonBody(requestData)
     *     .post(BodyHandlers.ofInputStream());
     * }</pre>
     *
     * @param <T> the response body type
     * @param responseBodyHandler the handler for processing the response body
     * @return the HTTP response with the processed body
     * @throws UncheckedIOException if the request could not be executed
     * @see java.net.http.HttpResponse.BodyHandlers
     */
    public <T> HttpResponse<T> post(final HttpResponse.BodyHandler<T> responseBodyHandler) throws UncheckedIOException {
        return execute(HttpMethod.POST, responseBodyHandler);
    }

    /**
     * Executes a POST request and returns the response body deserialized to the specified type.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User newUser = new User("John", "Doe");
     * User createdUser = HttpRequest.url("https://api.example.com/users")
     *     .jsonBody(newUser)
     *     .post(User.class);
     * }</pre>
     *
     * @param <T> the type of the result
     * @param resultClass the class of the result type
     * @return the deserialized response body
     * @throws UncheckedIOException if the request could not be executed or the response indicates an error
     */
    public <T> T post(final Class<T> resultClass) throws UncheckedIOException {
        final BodyHandler<?> responseBodyHandler = createResponseBodyHandler(resultClass);

        return getBody(post(responseBodyHandler), resultClass);
    }

    /**
     * Executes a PUT request and returns the response with a String body.
     * PUT requests are typically used to update or replace a resource on the server.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User updatedUser = new User("John", "Smith");
     * HttpResponse<String> response = HttpRequest.url("https://api.example.com/users/123")
     *     .jsonBody(updatedUser)
     *     .put();
     * }</pre>
     *
     * @return the HTTP response with String body
     * @throws UncheckedIOException if the request could not be executed
     */
    public HttpResponse<String> put() throws UncheckedIOException {
        return put(BodyHandlers.ofString());
    }

    /**
     * Executes a PUT request with a custom response body handler.
     * This allows you to control how the response body is processed, such as saving to a file,
     * reading as bytes, or handling as an input stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Path outputPath = Paths.get("response.json");
     * HttpResponse<Path> response = HttpRequest.url("https://api.example.com/users/123")
     *     .jsonBody(updatedUser)
     *     .put(BodyHandlers.ofFile(outputPath));
     * }</pre>
     *
     * @param <T> the response body type
     * @param responseBodyHandler the handler for processing the response body
     * @return the HTTP response with the processed body
     * @throws UncheckedIOException if the request could not be executed
     * @see java.net.http.HttpResponse.BodyHandlers
     */
    public <T> HttpResponse<T> put(final HttpResponse.BodyHandler<T> responseBodyHandler) throws UncheckedIOException {
        return execute(HttpMethod.PUT, responseBodyHandler);
    }

    /**
     * Executes a PUT request and returns the response body deserialized to the specified type.
     * This method automatically handles JSON/XML deserialization based on the response content type.
     * An exception is thrown if the response status code indicates an error (not 2xx).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User updatedUser = new User("John", "Smith");
     * User result = HttpRequest.url("https://api.example.com/users/123")
     *     .jsonBody(updatedUser)
     *     .put(User.class);
     * }</pre>
     *
     * @param <T> the type of the result
     * @param resultClass the class of the result type to deserialize the response body into
     * @return the deserialized response body
     * @throws UncheckedIOException if the request could not be executed or the response indicates an error
     */
    public <T> T put(final Class<T> resultClass) throws UncheckedIOException {
        final BodyHandler<?> responseBodyHandler = createResponseBodyHandler(resultClass);

        return getBody(put(responseBodyHandler), resultClass);
    }

    /**
     * Executes a PATCH request and returns the response with a String body.
     * PATCH requests are typically used to apply partial updates to a resource on the server.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> updates = new HashMap<>();
     * updates.put("email", "newemail@example.com");
     *
     * HttpResponse<String> response = HttpRequest.url("https://api.example.com/users/123")
     *     .jsonBody(updates)
     *     .patch();
     * }</pre>
     *
     * @return the HTTP response with String body
     * @throws UncheckedIOException if the request could not be executed
     */
    public HttpResponse<String> patch() throws UncheckedIOException {
        return patch(BodyHandlers.ofString());
    }

    /**
     * Executes a PATCH request with a custom response body handler.
     * This allows you to control how the response body is processed, such as saving to a file,
     * reading as bytes, or handling as an input stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> updates = new HashMap<>();
     * updates.put("status", "active");
     *
     * HttpResponse<String> response = HttpRequest.url("https://api.example.com/users/123")
     *     .jsonBody(updates)
     *     .patch(BodyHandlers.ofString());
     * }</pre>
     *
     * @param <T> the response body type
     * @param responseBodyHandler the handler for processing the response body
     * @return the HTTP response with the processed body
     * @throws UncheckedIOException if the request could not be executed
     * @see java.net.http.HttpResponse.BodyHandlers
     */
    public <T> HttpResponse<T> patch(final HttpResponse.BodyHandler<T> responseBodyHandler) throws UncheckedIOException {
        return execute(HttpMethod.PATCH, responseBodyHandler);
    }

    /**
     * Executes a PATCH request and returns the response body deserialized to the specified type.
     * This method automatically handles JSON/XML deserialization based on the response content type.
     * An exception is thrown if the response status code indicates an error (not 2xx).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> updates = new HashMap<>();
     * updates.put("email", "newemail@example.com");
     *
     * User result = HttpRequest.url("https://api.example.com/users/123")
     *     .jsonBody(updates)
     *     .patch(User.class);
     * }</pre>
     *
     * @param <T> the type of the result
     * @param resultClass the class of the result type to deserialize the response body into
     * @return the deserialized response body
     * @throws UncheckedIOException if the request could not be executed or the response indicates an error
     */
    public <T> T patch(final Class<T> resultClass) throws UncheckedIOException {
        final BodyHandler<?> responseBodyHandler = createResponseBodyHandler(resultClass);

        return getBody(patch(responseBodyHandler), resultClass);
    }

    /**
     * Executes a DELETE request and returns the response with a String body.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpResponse<String> response = HttpRequest.url("https://api.example.com/users/123")
     *     .delete();
     * }</pre>
     *
     * @return the HTTP response with String body
     * @throws UncheckedIOException if the request could not be executed
     */
    public HttpResponse<String> delete() throws UncheckedIOException {
        return delete(BodyHandlers.ofString());
    }

    /**
     * Executes a DELETE request with a custom response body handler.
     * This allows you to control how the response body is processed, such as saving to a file,
     * reading as bytes, or handling as an input stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpResponse<Void> response = HttpRequest.url("https://api.example.com/users/123")
     *     .delete(BodyHandlers.discarding());
     * System.out.println("Deleted with status: " + response.statusCode());
     * }</pre>
     *
     * @param <T> the response body type
     * @param responseBodyHandler the handler for processing the response body
     * @return the HTTP response with the processed body
     * @throws UncheckedIOException if the request could not be executed
     * @see java.net.http.HttpResponse.BodyHandlers
     */
    public <T> HttpResponse<T> delete(final HttpResponse.BodyHandler<T> responseBodyHandler) throws UncheckedIOException {
        return execute(HttpMethod.DELETE, responseBodyHandler);
    }

    /**
     * Executes a DELETE request and returns the response body deserialized to the specified type.
     * This method automatically handles JSON/XML deserialization based on the response content type.
     * An exception is thrown if the response status code indicates an error (not 2xx).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DeleteResponse result = HttpRequest.url("https://api.example.com/users/123")
     *     .delete(DeleteResponse.class);
     * }</pre>
     *
     * @param <T> the type of the result
     * @param resultClass the class of the result type to deserialize the response body into
     * @return the deserialized response body
     * @throws UncheckedIOException if the request could not be executed or the response indicates an error
     */
    public <T> T delete(final Class<T> resultClass) throws UncheckedIOException {
        final BodyHandler<?> responseBodyHandler = createResponseBodyHandler(resultClass);

        return getBody(delete(responseBodyHandler), resultClass);
    }

    /**
     * Executes a HEAD request and returns the response.
     * HEAD requests are used to retrieve headers without the response body, which is useful
     * for checking if a resource exists, getting metadata, or checking content length.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpResponse<Void> response = HttpRequest.url("https://api.example.com/large-file.zip")
     *     .head();
     * long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(0);
     * System.out.println("File size: " + contentLength + " bytes");
     * }</pre>
     *
     * @return the HTTP response (with no body, only headers)
     * @throws UncheckedIOException if the request could not be executed
     */
    public HttpResponse<Void> head() throws UncheckedIOException {
        return head(BodyHandlers.discarding());
    }

    /**
     * Executes a HEAD request with a custom response body handler.
     *
     * @param <T> the response body type
     * @param responseBodyHandler the handler for the response body
     * @return the HTTP response
     * @throws UncheckedIOException if the request could not be executed
     */
    private <T> HttpResponse<T> head(final HttpResponse.BodyHandler<T> responseBodyHandler) throws UncheckedIOException {
        return execute(HttpMethod.HEAD, responseBodyHandler);
    }

    /**
     * Executes an HTTP request with the specified method and returns the response with a String body.
     * This is a generic execution method that allows you to specify any HTTP method dynamically.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpResponse<String> response = HttpRequest.url("https://api.example.com/data")
     *     .execute(HttpMethod.GET);
     * }</pre>
     *
     * @param httpMethod the HTTP method to use (GET, POST, PUT, PATCH, DELETE, HEAD)
     * @return the HTTP response with String body
     * @throws IllegalArgumentException if httpMethod is null
     * @throws UncheckedIOException if the request could not be executed
     */
    @Beta
    public HttpResponse<String> execute(final HttpMethod httpMethod) throws UncheckedIOException {
        return execute(httpMethod, BodyHandlers.ofString());
    }

    /**
     * Executes an HTTP request with the specified method and custom response body handler.
     * This is a generic execution method that provides full control over both the HTTP method
     * and how the response is processed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpResponse<byte[]> response = HttpRequest.url("https://api.example.com/image")
     *     .execute(HttpMethod.GET, BodyHandlers.ofByteArray());
     * }</pre>
     *
     * @param <T> the response body type
     * @param httpMethod the HTTP method to use (GET, POST, PUT, PATCH, DELETE, HEAD)
     * @param responseBodyHandler the handler for processing the response body
     * @return the HTTP response with the processed body
     * @throws IllegalArgumentException if httpMethod is null
     * @throws UncheckedIOException if the request could not be executed
     * @see java.net.http.HttpResponse.BodyHandlers
     */
    @Beta
    public <T> HttpResponse<T> execute(final HttpMethod httpMethod, final HttpResponse.BodyHandler<T> responseBodyHandler)
            throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(httpMethod, HTTP_METHOD_STR);

        final HttpClient httpClientToUse = checkUrlAndHttpClient();

        try {
            return httpClientToUse.send(requestBuilder.method(httpMethod.name(), checkBodyPublisher()).build(), responseBodyHandler);
        } catch (IOException | InterruptedException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            doAfterExecution(httpClientToUse);
        }
    }

    /**
     * Executes an HTTP request with the specified method and returns the response body deserialized to the specified type.
     * This is a generic execution method that automatically handles JSON/XML deserialization.
     * An exception is thrown if the response status code indicates an error (not 2xx).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = HttpRequest.url("https://api.example.com/users/123")
     *     .execute(HttpMethod.GET, User.class);
     * }</pre>
     *
     * @param <T> the type of the result
     * @param httpMethod the HTTP method to use (GET, POST, PUT, PATCH, DELETE, HEAD)
     * @param resultClass the class of the result type to deserialize the response body into
     * @return the deserialized response body
     * @throws IllegalArgumentException if httpMethod is null
     * @throws UncheckedIOException if the request could not be executed or the response indicates an error
     */
    @Beta
    public <T> T execute(final HttpMethod httpMethod, final Class<T> resultClass) throws UncheckedIOException {
        final BodyHandler<?> responseBodyHandler = createResponseBodyHandler(resultClass);

        return getBody(execute(httpMethod, responseBodyHandler), resultClass);
    }

    private HttpClient checkUrlAndHttpClient() {
        final boolean hasNoQuery = query == null || (query instanceof String strQuery && Strings.isEmpty(strQuery))
                || (query instanceof Map<?, ?> mapQuery && mapQuery.isEmpty());

        if (hasNoQuery) {
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
     * Executes a GET request asynchronously and returns a CompletableFuture with a String body response.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CompletableFuture<HttpResponse<String>> future = HttpRequest.url("https://api.example.com/users")
     *     .asyncGet();
     * 
     * future.thenAccept(response -> {
     *     if (response.statusCode() == 200) {
     *         System.out.println(response.body());
     *     }
     * });
     * }</pre>
     *
     * @return a CompletableFuture that will complete with the HTTP response
     */
    public CompletableFuture<HttpResponse<String>> asyncGet() {
        return asyncGet(BodyHandlers.ofString());
    }

    /**
     * Executes a GET request asynchronously with a custom response body handler.
     * The request executes in the background, allowing the calling thread to continue processing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CompletableFuture<HttpResponse<byte[]>> future =
     *     HttpRequest.url("https://api.example.com/large-file")
     *         .asyncGet(BodyHandlers.ofByteArray());
     *
     * future.thenAccept(response -> {
     *     System.out.println("Downloaded " + response.body().length + " bytes");
     * });
     * }</pre>
     *
     * @param <T> the response body type
     * @param responseBodyHandler the handler for processing the response body
     * @return a CompletableFuture that will complete with the HTTP response
     * @see java.net.http.HttpResponse.BodyHandlers
     */
    public <T> CompletableFuture<HttpResponse<T>> asyncGet(final HttpResponse.BodyHandler<T> responseBodyHandler) {
        return asyncExecute(HttpMethod.GET, responseBodyHandler);
    }

    /**
     * Executes a GET request asynchronously and returns the response body deserialized to the specified type.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CompletableFuture<List<User>> future = HttpRequest.url("https://api.example.com/users")
     *     .asyncGet(new TypeToken<List<User>>(){}.getType());
     * 
     * future.thenAccept(users -> {
     *     users.forEach(System.out::println);
     * });
     * }</pre>
     *
     * @param <T> the type of the result
     * @param resultClass the class of the result type
     * @return a CompletableFuture that will complete with the deserialized response body
     */
    public <T> CompletableFuture<T> asyncGet(final Class<T> resultClass) {
        return asyncExecute(HttpMethod.GET, resultClass);
    }

    /**
     * Executes a GET request asynchronously with a custom response body handler and push promise handler.
     * The push promise handler is used for HTTP/2 server push, which allows the server to send
     * additional resources before the client requests them.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * PushPromiseHandler<String> pushHandler = (initiatingRequest, pushPromiseRequest, acceptor) -> {
     *     acceptor.apply(BodyHandlers.ofString());
     * };
     *
     * CompletableFuture<HttpResponse<String>> future =
     *     HttpRequest.url("https://http2.example.com/page")
     *         .asyncGet(BodyHandlers.ofString(), pushHandler);
     * }</pre>
     *
     * @param <T> the response body type
     * @param responseBodyHandler the handler for processing the response body
     * @param pushPromiseHandler the handler for processing HTTP/2 server push promises
     * @return a CompletableFuture that will complete with the HTTP response
     * @see java.net.http.HttpResponse.PushPromiseHandler
     */
    public <T> CompletableFuture<HttpResponse<T>> asyncGet(final HttpResponse.BodyHandler<T> responseBodyHandler,
            final PushPromiseHandler<T> pushPromiseHandler) {
        return asyncExecute(HttpMethod.GET, responseBodyHandler, pushPromiseHandler);
    }

    /**
     * Executes a POST request asynchronously and returns a CompletableFuture with a String body response.
     * The request executes in the background, allowing the calling thread to continue processing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User newUser = new User("John", "Doe");
     * CompletableFuture<HttpResponse<String>> future =
     *     HttpRequest.url("https://api.example.com/users")
     *         .jsonBody(newUser)
     *         .asyncPost();
     *
     * future.thenAccept(response -> {
     *     System.out.println("Created user: " + response.body());
     * });
     * }</pre>
     *
     * @return a CompletableFuture that will complete with the HTTP response
     */
    public CompletableFuture<HttpResponse<String>> asyncPost() {
        return asyncPost(BodyHandlers.ofString());
    }

    /**
     * Executes a POST request asynchronously with a custom response body handler.
     * This allows you to control how the response body is processed while the request executes
     * in the background.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CompletableFuture<HttpResponse<Path>> future =
     *     HttpRequest.url("https://api.example.com/report")
     *         .jsonBody(reportRequest)
     *         .asyncPost(BodyHandlers.ofFile(Paths.get("report.pdf")));
     * }</pre>
     *
     * @param <T> the response body type
     * @param responseBodyHandler the handler for processing the response body
     * @return a CompletableFuture that will complete with the HTTP response
     * @see java.net.http.HttpResponse.BodyHandlers
     */
    public <T> CompletableFuture<HttpResponse<T>> asyncPost(final HttpResponse.BodyHandler<T> responseBodyHandler) {
        return asyncExecute(HttpMethod.POST, responseBodyHandler);
    }

    /**
     * Executes a POST request asynchronously and returns the response body deserialized to the specified type.
     * This method automatically handles JSON/XML deserialization in the background.
     * An exception is thrown if the response status code indicates an error (not 2xx).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User newUser = new User("John", "Doe");
     * CompletableFuture<User> future =
     *     HttpRequest.url("https://api.example.com/users")
     *         .jsonBody(newUser)
     *         .asyncPost(User.class);
     *
     * future.thenAccept(createdUser -> {
     *     System.out.println("Created user with ID: " + createdUser.getId());
     * });
     * }</pre>
     *
     * @param <T> the type of the result
     * @param resultClass the class of the result type to deserialize the response body into
     * @return a CompletableFuture that will complete with the deserialized response body
     */
    public <T> CompletableFuture<T> asyncPost(final Class<T> resultClass) {
        return asyncExecute(HttpMethod.POST, resultClass);
    }

    /**
     * Executes a POST request asynchronously with a custom response body handler and push promise handler.
     * The push promise handler is used for HTTP/2 server push, which allows the server to send
     * additional resources before the client requests them.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * PushPromiseHandler<String> pushHandler = (initiatingRequest, pushPromiseRequest, acceptor) -> {
     *     acceptor.apply(BodyHandlers.ofString());
     * };
     *
     * CompletableFuture<HttpResponse<String>> future =
     *     HttpRequest.url("https://http2.example.com/submit")
     *         .jsonBody(data)
     *         .asyncPost(BodyHandlers.ofString(), pushHandler);
     * }</pre>
     *
     * @param <T> the response body type
     * @param responseBodyHandler the handler for processing the response body
     * @param pushPromiseHandler the handler for processing HTTP/2 server push promises
     * @return a CompletableFuture that will complete with the HTTP response
     * @see java.net.http.HttpResponse.PushPromiseHandler
     */
    public <T> CompletableFuture<HttpResponse<T>> asyncPost(final HttpResponse.BodyHandler<T> responseBodyHandler,
            final PushPromiseHandler<T> pushPromiseHandler) {
        return asyncExecute(HttpMethod.POST, responseBodyHandler, pushPromiseHandler);
    }

    /**
     * Executes a PUT request asynchronously and returns a CompletableFuture with a String body response.
     * The request executes in the background, allowing the calling thread to continue processing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User updatedUser = new User("John", "Smith");
     * CompletableFuture<HttpResponse<String>> future =
     *     HttpRequest.url("https://api.example.com/users/123")
     *         .jsonBody(updatedUser)
     *         .asyncPut();
     * }</pre>
     *
     * @return a CompletableFuture that will complete with the HTTP response
     */
    public CompletableFuture<HttpResponse<String>> asyncPut() {
        return asyncPut(BodyHandlers.ofString());
    }

    /**
     * Executes a PUT request asynchronously with a custom response body handler.
     * This allows you to control how the response body is processed while the request executes
     * in the background.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CompletableFuture<HttpResponse<String>> future =
     *     HttpRequest.url("https://api.example.com/users/123")
     *         .jsonBody(updatedUser)
     *         .asyncPut(BodyHandlers.ofString());
     *
     * future.thenAccept(response -> {
     *     System.out.println("Updated: " + response.body());
     * });
     * }</pre>
     *
     * @param <T> the response body type
     * @param responseBodyHandler the handler for processing the response body
     * @return a CompletableFuture that will complete with the HTTP response
     * @see java.net.http.HttpResponse.BodyHandlers
     */
    public <T> CompletableFuture<HttpResponse<T>> asyncPut(final HttpResponse.BodyHandler<T> responseBodyHandler) {
        return asyncExecute(HttpMethod.PUT, responseBodyHandler);
    }

    /**
     * Executes a PUT request asynchronously and returns the response body deserialized to the specified type.
     * This method automatically handles JSON/XML deserialization in the background.
     * An exception is thrown if the response status code indicates an error (not 2xx).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User updatedUser = new User("John", "Smith");
     * CompletableFuture<User> future =
     *     HttpRequest.url("https://api.example.com/users/123")
     *         .jsonBody(updatedUser)
     *         .asyncPut(User.class);
     *
     * future.thenAccept(user -> {
     *     System.out.println("Updated user: " + user.getName());
     * });
     * }</pre>
     *
     * @param <T> the type of the result
     * @param resultClass the class of the result type to deserialize the response body into
     * @return a CompletableFuture that will complete with the deserialized response body
     */
    public <T> CompletableFuture<T> asyncPut(final Class<T> resultClass) {
        return asyncExecute(HttpMethod.PUT, resultClass);
    }

    /**
     * Executes a PUT request asynchronously with a custom response body handler and push promise handler.
     * The push promise handler is used for HTTP/2 server push, which allows the server to send
     * additional resources before the client requests them.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * PushPromiseHandler<String> pushHandler = (initiatingRequest, pushPromiseRequest, acceptor) -> {
     *     acceptor.apply(BodyHandlers.ofString());
     * };
     *
     * CompletableFuture<HttpResponse<String>> future =
     *     HttpRequest.url("https://http2.example.com/users/123")
     *         .jsonBody(updatedUser)
     *         .asyncPut(BodyHandlers.ofString(), pushHandler);
     * }</pre>
     *
     * @param <T> the response body type
     * @param responseBodyHandler the handler for processing the response body
     * @param pushPromiseHandler the handler for processing HTTP/2 server push promises
     * @return a CompletableFuture that will complete with the HTTP response
     * @see java.net.http.HttpResponse.PushPromiseHandler
     */
    public <T> CompletableFuture<HttpResponse<T>> asyncPut(final HttpResponse.BodyHandler<T> responseBodyHandler,
            final PushPromiseHandler<T> pushPromiseHandler) {
        return asyncExecute(HttpMethod.PUT, responseBodyHandler, pushPromiseHandler);
    }

    /**
     * Executes a PATCH request asynchronously and returns a CompletableFuture with a String body response.
     * The request executes in the background, allowing the calling thread to continue processing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> updates = new HashMap<>();
     * updates.put("email", "newemail@example.com");
     *
     * CompletableFuture<HttpResponse<String>> future =
     *     HttpRequest.url("https://api.example.com/users/123")
     *         .jsonBody(updates)
     *         .asyncPatch();
     *
     * future.thenAccept(response -> {
     *     System.out.println("Patched: " + response.body());
     * });
     * }</pre>
     *
     * @return a CompletableFuture that will complete with the HTTP response
     */
    public CompletableFuture<HttpResponse<String>> asyncPatch() {
        return asyncPatch(BodyHandlers.ofString());
    }

    /**
     * Executes a PATCH request asynchronously with a custom response body handler.
     * This allows you to control how the response body is processed while the request executes
     * in the background.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> updates = new HashMap<>();
     * updates.put("status", "active");
     *
     * CompletableFuture<HttpResponse<String>> future =
     *     HttpRequest.url("https://api.example.com/users/123")
     *         .jsonBody(updates)
     *         .asyncPatch(BodyHandlers.ofString());
     *
     * future.thenAccept(response -> {
     *     System.out.println("Response: " + response.body());
     * });
     * }</pre>
     *
     * @param <T> the response body type
     * @param responseBodyHandler the handler for processing the response body
     * @return a CompletableFuture that will complete with the HTTP response
     * @see java.net.http.HttpResponse.BodyHandlers
     */
    public <T> CompletableFuture<HttpResponse<T>> asyncPatch(final HttpResponse.BodyHandler<T> responseBodyHandler) {
        return asyncExecute(HttpMethod.PATCH, responseBodyHandler);
    }

    /**
     * Executes a PATCH request asynchronously and returns the response body deserialized to the specified type.
     * This method automatically handles JSON/XML deserialization in the background.
     * An exception is thrown if the response status code indicates an error (not 2xx).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> updates = new HashMap<>();
     * updates.put("email", "newemail@example.com");
     *
     * CompletableFuture<User> future =
     *     HttpRequest.url("https://api.example.com/users/123")
     *         .jsonBody(updates)
     *         .asyncPatch(User.class);
     *
     * future.thenAccept(user -> {
     *     System.out.println("Updated user: " + user.getEmail());
     * });
     * }</pre>
     *
     * @param <T> the type of the result
     * @param resultClass the class of the result type to deserialize the response body into
     * @return a CompletableFuture that will complete with the deserialized response body
     */
    public <T> CompletableFuture<T> asyncPatch(final Class<T> resultClass) {
        return asyncExecute(HttpMethod.PATCH, resultClass);
    }

    /**
     * Executes a PATCH request asynchronously with a custom response body handler and push promise handler.
     * The push promise handler is used for HTTP/2 server push, which allows the server to send
     * additional resources before the client requests them.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * PushPromiseHandler<String> pushHandler = (initiatingRequest, pushPromiseRequest, acceptor) -> {
     *     acceptor.apply(BodyHandlers.ofString());
     * };
     *
     * CompletableFuture<HttpResponse<String>> future =
     *     HttpRequest.url("https://http2.example.com/users/123")
     *         .jsonBody(updates)
     *         .asyncPatch(BodyHandlers.ofString(), pushHandler);
     * }</pre>
     *
     * @param <T> the response body type
     * @param responseBodyHandler the handler for processing the response body
     * @param pushPromiseHandler the handler for processing HTTP/2 server push promises
     * @return a CompletableFuture that will complete with the HTTP response
     * @see java.net.http.HttpResponse.PushPromiseHandler
     */
    public <T> CompletableFuture<HttpResponse<T>> asyncPatch(final HttpResponse.BodyHandler<T> responseBodyHandler,
            final PushPromiseHandler<T> pushPromiseHandler) {
        return asyncExecute(HttpMethod.PATCH, responseBodyHandler, pushPromiseHandler);
    }

    /**
     * Executes a DELETE request asynchronously and returns a CompletableFuture with a String body response.
     * The request executes in the background, allowing the calling thread to continue processing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CompletableFuture<HttpResponse<String>> future =
     *     HttpRequest.url("https://api.example.com/users/123")
     *         .asyncDelete();
     *
     * future.thenAccept(response -> {
     *     System.out.println("Deleted with status: " + response.statusCode());
     * });
     * }</pre>
     *
     * @return a CompletableFuture that will complete with the HTTP response
     */
    public CompletableFuture<HttpResponse<String>> asyncDelete() {
        return asyncDelete(BodyHandlers.ofString());
    }

    /**
     * Executes a DELETE request asynchronously with a custom response body handler.
     * This allows you to control how the response body is processed while the request executes
     * in the background.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CompletableFuture<HttpResponse<Void>> future =
     *     HttpRequest.url("https://api.example.com/users/123")
     *         .asyncDelete(BodyHandlers.discarding());
     *
     * future.thenAccept(response -> {
     *     System.out.println("Deleted with status: " + response.statusCode());
     * });
     * }</pre>
     *
     * @param <T> the response body type
     * @param responseBodyHandler the handler for processing the response body
     * @return a CompletableFuture that will complete with the HTTP response
     * @see java.net.http.HttpResponse.BodyHandlers
     */
    public <T> CompletableFuture<HttpResponse<T>> asyncDelete(final HttpResponse.BodyHandler<T> responseBodyHandler) {
        return asyncExecute(HttpMethod.DELETE, responseBodyHandler);
    }

    /**
     * Executes a DELETE request asynchronously and returns the response body deserialized to the specified type.
     * This method automatically handles JSON/XML deserialization in the background.
     * An exception is thrown if the response status code indicates an error (not 2xx).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CompletableFuture<DeleteResponse> future =
     *     HttpRequest.url("https://api.example.com/users/123")
     *         .asyncDelete(DeleteResponse.class);
     *
     * future.thenAccept(result -> {
     *     System.out.println("Delete result: " + result.getMessage());
     * });
     * }</pre>
     *
     * @param <T> the type of the result
     * @param resultClass the class of the result type to deserialize the response body into
     * @return a CompletableFuture that will complete with the deserialized response body
     */
    public <T> CompletableFuture<T> asyncDelete(final Class<T> resultClass) {
        return asyncExecute(HttpMethod.DELETE, resultClass);
    }

    /**
     * Executes a DELETE request asynchronously with a custom response body handler and push promise handler.
     * The push promise handler is used for HTTP/2 server push, which allows the server to send
     * additional resources before the client requests them.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * PushPromiseHandler<String> pushHandler = (initiatingRequest, pushPromiseRequest, acceptor) -> {
     *     acceptor.apply(BodyHandlers.ofString());
     * };
     *
     * CompletableFuture<HttpResponse<String>> future =
     *     HttpRequest.url("https://http2.example.com/users/123")
     *         .asyncDelete(BodyHandlers.ofString(), pushHandler);
     * }</pre>
     *
     * @param <T> the response body type
     * @param responseBodyHandler the handler for processing the response body
     * @param pushPromiseHandler the handler for processing HTTP/2 server push promises
     * @return a CompletableFuture that will complete with the HTTP response
     * @see java.net.http.HttpResponse.PushPromiseHandler
     */
    public <T> CompletableFuture<HttpResponse<T>> asyncDelete(final HttpResponse.BodyHandler<T> responseBodyHandler,
            final PushPromiseHandler<T> pushPromiseHandler) {
        return asyncExecute(HttpMethod.DELETE, responseBodyHandler, pushPromiseHandler);
    }

    /**
     * Executes a HEAD request asynchronously and returns a CompletableFuture with no response body.
     * The request executes in the background, allowing the calling thread to continue processing.
     * HEAD requests are useful for checking if a resource exists or getting metadata without
     * downloading the full response body.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CompletableFuture<HttpResponse<Void>> future =
     *     HttpRequest.url("https://api.example.com/large-file.zip")
     *         .asyncHead();
     *
     * future.thenAccept(response -> {
     *     long size = response.headers().firstValueAsLong("Content-Length").orElse(0);
     *     System.out.println("File size: " + size + " bytes");
     * });
     * }</pre>
     *
     * @return a CompletableFuture that will complete with the HTTP response
     */
    public CompletableFuture<HttpResponse<Void>> asyncHead() {
        return asyncHead(BodyHandlers.discarding());
    }

    /**
     * Executes a HEAD request asynchronously with a custom response body handler.
     *
     * @param <T> the response body type
     * @param responseBodyHandler the handler for processing the response body
     * @return a CompletableFuture that will complete with the HTTP response
     */
    <T> CompletableFuture<HttpResponse<T>> asyncHead(final HttpResponse.BodyHandler<T> responseBodyHandler) {
        return asyncExecute(HttpMethod.HEAD, responseBodyHandler);
    }

    /**
     * Executes an HTTP request asynchronously with the specified method and returns a CompletableFuture with a String body response.
     * This is a generic async execution method that allows you to specify any HTTP method dynamically.
     * The request executes in the background, allowing the calling thread to continue processing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CompletableFuture<HttpResponse<String>> future =
     *     HttpRequest.url("https://api.example.com/data")
     *         .asyncExecute(HttpMethod.GET);
     *
     * future.thenAccept(response -> {
     *     System.out.println("Response: " + response.body());
     * });
     * }</pre>
     *
     * @param httpMethod the HTTP method to use (GET, POST, PUT, PATCH, DELETE, HEAD)
     * @return a CompletableFuture that will complete with the HTTP response
     */
    @Beta
    public CompletableFuture<HttpResponse<String>> asyncExecute(final HttpMethod httpMethod) {
        return asyncExecute(httpMethod, BodyHandlers.ofString());
    }

    /**
     * Executes an HTTP request asynchronously with the specified method and custom response body handler.
     * This is a generic async execution method that provides full control over both the HTTP method
     * and how the response is processed. The request executes in the background.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CompletableFuture<HttpResponse<byte[]>> future =
     *     HttpRequest.url("https://api.example.com/image")
     *         .asyncExecute(HttpMethod.GET, BodyHandlers.ofByteArray());
     *
     * future.thenAccept(response -> {
     *     System.out.println("Downloaded " + response.body().length + " bytes");
     * });
     * }</pre>
     *
     * @param <T> the response body type
     * @param httpMethod the HTTP method to use (GET, POST, PUT, PATCH, DELETE, HEAD)
     * @param responseBodyHandler the handler for processing the response body
     * @return a CompletableFuture that will complete with the HTTP response
     * @throws IllegalArgumentException if httpMethod is null
     * @see java.net.http.HttpResponse.BodyHandlers
     */
    @Beta
    public <T> CompletableFuture<HttpResponse<T>> asyncExecute(final HttpMethod httpMethod, final HttpResponse.BodyHandler<T> responseBodyHandler)
            throws IllegalArgumentException {
        N.checkArgNotNull(httpMethod, HTTP_METHOD_STR);

        final HttpClient httpClientToUse = checkUrlAndHttpClient();

        //    try {
        //        return httpClientToUse.sendAsync(requestBuilder.method(httpMethod.name(), checkBodyPublisher()).build(), responseBodyHandler);
        //    } finally {
        //        // This is asynchronous call
        //        // doAfterExecution(httpClientToUse);
        //    }

        return httpClientToUse.sendAsync(requestBuilder.method(httpMethod.name(), checkBodyPublisher()).build(), responseBodyHandler);
    }

    /**
     * Executes an HTTP request asynchronously with the specified method and returns the response body deserialized to the specified type.
     * This is a generic async execution method that automatically handles JSON/XML deserialization in the background.
     * An exception is thrown if the response status code indicates an error (not 2xx).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CompletableFuture<User> future =
     *     HttpRequest.url("https://api.example.com/users/123")
     *         .asyncExecute(HttpMethod.GET, User.class);
     *
     * future.thenAccept(user -> {
     *     System.out.println("User: " + user.getName());
     * });
     * }</pre>
     *
     * @param <T> the type of the result
     * @param httpMethod the HTTP method to use (GET, POST, PUT, PATCH, DELETE, HEAD)
     * @param resultClass the class of the result type to deserialize the response body into
     * @return a CompletableFuture that will complete with the deserialized response body
     * @throws IllegalArgumentException if httpMethod is null
     */
    @Beta
    public <T> CompletableFuture<T> asyncExecute(final HttpMethod httpMethod, final Class<T> resultClass) throws IllegalArgumentException {
        N.checkArgNotNull(httpMethod, HTTP_METHOD_STR);

        final HttpClient httpClientToUse = checkUrlAndHttpClient();
        final BodyHandler<?> responseBodyHandler = createResponseBodyHandler(resultClass);

        return httpClientToUse.sendAsync(requestBuilder.method(httpMethod.name(), checkBodyPublisher()).build(), responseBodyHandler)
                .thenApply(it -> getBody(it, resultClass));
    }

    /**
     * Executes an HTTP request asynchronously with the specified method, custom response body handler, and push promise handler.
     * The push promise handler is used for HTTP/2 server push, which allows the server to send
     * additional resources before the client requests them. This is a generic async execution method
     * that provides full control over all aspects of the request and response processing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * PushPromiseHandler<String> pushHandler = (initiatingRequest, pushPromiseRequest, acceptor) -> {
     *     System.out.println("Server push for: " + pushPromiseRequest.uri());
     *     acceptor.apply(BodyHandlers.ofString());
     * };
     *
     * CompletableFuture<HttpResponse<String>> future =
     *     HttpRequest.url("https://http2.example.com/page")
     *         .asyncExecute(HttpMethod.GET, BodyHandlers.ofString(), pushHandler);
     * }</pre>
     *
     * @param <T> the response body type
     * @param httpMethod the HTTP method to use (GET, POST, PUT, PATCH, DELETE, HEAD)
     * @param responseBodyHandler the handler for processing the response body
     * @param pushPromiseHandler the handler for processing HTTP/2 server push promises
     * @return a CompletableFuture that will complete with the HTTP response
     * @throws IllegalArgumentException if httpMethod is null
     * @see java.net.http.HttpResponse.PushPromiseHandler
     */
    @Beta
    public <T> CompletableFuture<HttpResponse<T>> asyncExecute(final HttpMethod httpMethod, final HttpResponse.BodyHandler<T> responseBodyHandler,
            final PushPromiseHandler<T> pushPromiseHandler) throws IllegalArgumentException {
        N.checkArgNotNull(httpMethod, HTTP_METHOD_STR);

        final HttpClient httpClientToUse = checkUrlAndHttpClient();

        //    try {
        //        return httpClientToUse.sendAsync(requestBuilder.method(httpMethod.name(), checkBodyPublisher()).build(), responseBodyHandler, pushPromiseHandler);
        //    } finally {
        //        // This is asynchronous call
        //        // doAfterExecution(httpClientToUse);
        //    }

        return httpClientToUse.sendAsync(requestBuilder.method(httpMethod.name(), checkBodyPublisher()).build(), responseBodyHandler, pushPromiseHandler);
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
