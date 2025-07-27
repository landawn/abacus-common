/*
 * Copyright (C) 2015 HaiYang Li
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.http.HttpHeaders.Names;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.AsyncExecutor;
import com.landawn.abacus.util.ContinuableFuture;
import com.landawn.abacus.util.ExceptionUtil;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.URLEncodedUtil;

/**
 * A thread-safe HTTP client implementation based on Java's HttpURLConnection.
 * This class provides a simple and efficient way to make HTTP requests with support for
 * various HTTP methods, content types, compression, and asynchronous operations.
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Support for GET, POST, PUT, DELETE, and HEAD methods</li>
 *   <li>Automatic content type detection and serialization/deserialization</li>
 *   <li>Connection pooling and timeout management</li>
 *   <li>SSL/TLS support with custom socket factories</li>
 *   <li>Proxy support</li>
 *   <li>Asynchronous request execution</li>
 *   <li>Support for various content formats (JSON, XML, Form URL-encoded, Kryo)</li>
 *   <li>Compression support (GZIP, LZ4, Snappy, Brotli)</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Create a simple HTTP client
 * HttpClient client = HttpClient.create("https://api.example.com");
 * 
 * // Make a GET request
 * String response = client.get();
 * 
 * // Make a POST request with JSON body
 * User user = new User("John", "Doe");
 * User createdUser = client.post(user, User.class);
 * 
 * // Make an async request
 * ContinuableFuture<String> future = client.asyncGet();
 * }</pre>
 * 
 * <p>Any header can be set into the parameter {@code settings}</p>
 * 
 * <p><b>HttpClient is thread safe.</b></p>
 * 
 * @author HaiYang Li
 * @see HttpSettings
 * @see HttpRequest
 * @see HttpResponse
 */
public final class HttpClient {

    static final Logger logger = LoggerFactory.getLogger(HttpClient.class);

    static {
        if (IOUtil.IS_PLATFORM_ANDROID) {
            // ignore
        } else {
            final int maxConnections = IOUtil.CPU_CORES * 16;

            System.setProperty("http.keepAlive", "true");
            System.setProperty("http.maxConnections", String.valueOf(maxConnections));
        }
    }

    // ...
    /** Default maximum number of concurrent connections per HttpClient instance. */
    public static final int DEFAULT_MAX_CONNECTION = 16;

    /** Default connection timeout in milliseconds (8 seconds). */
    public static final int DEFAULT_CONNECTION_TIMEOUT = 8000;

    /** Default read timeout in milliseconds (16 seconds). */
    public static final int DEFAULT_READ_TIMEOUT = 16000;

    // ...
    private final String _url; //NOSONAR

    private final int _maxConnection; //NOSONAR

    private final long _connectionTimeoutInMillis; //NOSONAR

    private final long _readTimeoutInMillis; //NOSONAR

    private final HttpSettings _settings; //NOSONAR

    final AsyncExecutor _asyncExecutor; //NOSONAR

    private final URL _netURL; //NOSONAR

    private final AtomicInteger _activeConnectionCounter; //NOSONAR

    private HttpClient(final String url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis,
            final HttpSettings settings, final AtomicInteger sharedActiveConnectionCounter, final Executor executor) {
        this(null, url, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, settings, sharedActiveConnectionCounter, executor);
    }

    private HttpClient(final URL netUrl, final String url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis,
            final HttpSettings settings, final AtomicInteger sharedActiveConnectionCounter, final Executor executor) {
        N.checkArgument(netUrl != null || Strings.isNotEmpty(url), "url cannot be null or empty");

        if ((maxConnection < 0) || (connectionTimeoutInMillis < 0) || (readTimeoutInMillis < 0)) {
            throw new IllegalArgumentException("maxConnection, connectionTimeoutInMillis or readTimeoutInMillis can't be less than 0: " + maxConnection + ", "
                    + connectionTimeoutInMillis + ", " + readTimeoutInMillis);
        }

        _netURL = netUrl == null ? createNetUrl(url) : netUrl;
        _url = Strings.isEmpty(url) ? _netURL.toString() : url;
        _maxConnection = (maxConnection == 0) ? DEFAULT_MAX_CONNECTION : maxConnection;
        _connectionTimeoutInMillis = (connectionTimeoutInMillis == 0) ? DEFAULT_CONNECTION_TIMEOUT : connectionTimeoutInMillis;
        _readTimeoutInMillis = (readTimeoutInMillis == 0) ? DEFAULT_READ_TIMEOUT : readTimeoutInMillis;
        _settings = settings == null ? HttpSettings.create() : settings;

        _asyncExecutor = executor == null ? HttpUtil.DEFAULT_ASYNC_EXECUTOR : new AsyncExecutor(executor);

        _activeConnectionCounter = sharedActiveConnectionCounter;
    }

    private static URL createNetUrl(final String url) {
        try {
            return URI.create(N.checkArgNotNull(url, "url")).toURL();
        } catch (final MalformedURLException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Gets the base URL configured for this HTTP client.
     * 
     * @return The base URL as a string
     */
    public String url() {
        return _url;
    }

    /**
     * Creates an HttpClient instance with the specified URL and default settings.
     * Uses default values for max connections, connection timeout, and read timeout.
     * 
     * @param url The base URL for the HTTP client
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if url is null or empty
     * 
     * <p>Example:</p>
     * <pre>{@code
     * HttpClient client = HttpClient.create("https://api.example.com");
     * }</pre>
     */
    public static HttpClient create(final String url) {
        return create(url, DEFAULT_MAX_CONNECTION);
    }

    /**
     * Creates an HttpClient instance with the specified URL and maximum connections.
     * Uses default values for connection timeout and read timeout.
     * 
     * @param url The base URL for the HTTP client
     * @param maxConnection Maximum number of concurrent connections
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if url is null or empty, or maxConnection is negative
     * 
     * <p>Example:</p>
     * <pre>{@code
     * HttpClient client = HttpClient.create("https://api.example.com", 32);
     * }</pre>
     */
    public static HttpClient create(final String url, final int maxConnection) {
        return create(url, maxConnection, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    /**
     * Creates an HttpClient instance with the specified URL and timeout settings.
     * Uses default value for max connections.
     * 
     * @param url The base URL for the HTTP client
     * @param connectionTimeoutInMillis Connection timeout in milliseconds
     * @param readTimeoutInMillis Read timeout in milliseconds
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if url is null or empty, or timeouts are negative
     * 
     * <p>Example:</p>
     * <pre>{@code
     * HttpClient client = HttpClient.create("https://api.example.com", 5000, 10000);
     * }</pre>
     */
    public static HttpClient create(final String url, final long connectionTimeoutInMillis, final long readTimeoutInMillis) {
        return create(url, DEFAULT_MAX_CONNECTION, connectionTimeoutInMillis, readTimeoutInMillis);
    }

    /**
     * Creates an HttpClient instance with the specified URL, max connections, and timeout settings.
     * 
     * @param url The base URL for the HTTP client
     * @param maxConnection Maximum number of concurrent connections
     * @param connectionTimeoutInMillis Connection timeout in milliseconds
     * @param readTimeoutInMillis Read timeout in milliseconds
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if url is null or empty, or any numeric parameter is negative
     * 
     * <p>Example:</p>
     * <pre>{@code
     * HttpClient client = HttpClient.create("https://api.example.com", 32, 5000, 10000);
     * }</pre>
     */
    public static HttpClient create(final String url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis) {
        return create(url, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, (HttpSettings) null);
    }

    /**
     * Creates an HttpClient instance with the specified URL, max connections, timeout settings, and HTTP settings.
     * 
     * @param url The base URL for the HTTP client
     * @param maxConnection Maximum number of concurrent connections
     * @param connectionTimeoutInMillis Connection timeout in milliseconds
     * @param readTimeoutInMillis Read timeout in milliseconds
     * @param settings Additional HTTP settings (headers, content type, etc.)
     * @return A new HttpClient instance
     * @throws UncheckedIOException if an I/O error occurs
     * @throws IllegalArgumentException if url is null or empty, or any numeric parameter is negative
     * 
     * <p>Example:</p>
     * <pre>{@code
     * HttpSettings settings = HttpSettings.create()
     *     .setContentType("application/json")
     *     .header("Authorization", "Bearer token123");
     * HttpClient client = HttpClient.create("https://api.example.com", 16, 5000, 10000, settings);
     * }</pre>
     */
    public static HttpClient create(final String url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis,
            final HttpSettings settings) throws UncheckedIOException {
        return create(url, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, settings, new AtomicInteger(0));
    }

    /**
     * Creates an HttpClient instance with a shared active connection counter.
     * This allows multiple HttpClient instances to share a connection limit.
     * 
     * @param url The base URL for the HTTP client
     * @param maxConnection Maximum number of concurrent connections
     * @param connectionTimeoutInMillis Connection timeout in milliseconds
     * @param readTimeoutInMillis Read timeout in milliseconds
     * @param settings Additional HTTP settings
     * @param sharedActiveConnectionCounter Shared counter for active connections
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if url is null or empty, or any numeric parameter is negative
     */
    public static HttpClient create(final String url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis,
            final HttpSettings settings, final AtomicInteger sharedActiveConnectionCounter) {
        return create(url, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, settings, sharedActiveConnectionCounter, null);
    }

    /**
     * Creates an HttpClient instance with a custom executor for async operations.
     * 
     * @param url The base URL for the HTTP client
     * @param maxConnection Maximum number of concurrent connections
     * @param connectionTimeoutInMillis Connection timeout in milliseconds
     * @param readTimeoutInMillis Read timeout in milliseconds
     * @param executor Custom executor for asynchronous operations
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if url is null or empty, or any numeric parameter is negative
     */
    public static HttpClient create(final String url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis,
            final Executor executor) {
        return create(url, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, null, executor);
    }

    /**
     * Creates an HttpClient instance with the specified URL, settings, and executor.
     * 
     * @param url The base URL for the HTTP client
     * @param maxConnection Maximum number of concurrent connections
     * @param connectionTimeoutInMillis Connection timeout in milliseconds
     * @param readTimeoutInMillis Read timeout in milliseconds
     * @param settings Additional HTTP settings
     * @param executor Custom executor for asynchronous operations
     * @return A new HttpClient instance
     * @throws UncheckedIOException if an I/O error occurs
     * @throws IllegalArgumentException if url is null or empty, or any numeric parameter is negative
     */
    public static HttpClient create(final String url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis,
            final HttpSettings settings, final Executor executor) throws UncheckedIOException {
        return create(url, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, settings, new AtomicInteger(0), executor);
    }

    /**
     * Creates an HttpClient instance with all configuration options.
     * 
     * @param url The base URL for the HTTP client
     * @param maxConnection Maximum number of concurrent connections
     * @param connectionTimeoutInMillis Connection timeout in milliseconds
     * @param readTimeoutInMillis Read timeout in milliseconds
     * @param settings Additional HTTP settings
     * @param sharedActiveConnectionCounter Shared counter for active connections
     * @param executor Custom executor for asynchronous operations
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if url is null or empty, or any numeric parameter is negative
     */
    public static HttpClient create(final String url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis,
            final HttpSettings settings, final AtomicInteger sharedActiveConnectionCounter, final Executor executor) {
        return new HttpClient(url, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, settings, sharedActiveConnectionCounter, executor);
    }

    /**
     * Creates an HttpClient instance with a URL object and default settings.
     * 
     * @param url The base URL for the HTTP client
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if url is null
     */
    public static HttpClient create(final URL url) {
        return create(url, DEFAULT_MAX_CONNECTION);
    }

    /**
     * Creates an HttpClient instance with a URL object and maximum connections.
     * 
     * @param url The base URL for the HTTP client
     * @param maxConnection Maximum number of concurrent connections
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if url is null or maxConnection is negative
     */
    public static HttpClient create(final URL url, final int maxConnection) {
        return create(url, maxConnection, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    /**
     * Creates an HttpClient instance with a URL object and timeout settings.
     * 
     * @param url The base URL for the HTTP client
     * @param connectionTimeoutInMillis Connection timeout in milliseconds
     * @param readTimeoutInMillis Read timeout in milliseconds
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if url is null or timeouts are negative
     */
    public static HttpClient create(final URL url, final long connectionTimeoutInMillis, final long readTimeoutInMillis) {
        return create(url, DEFAULT_MAX_CONNECTION, connectionTimeoutInMillis, readTimeoutInMillis);
    }

    /**
     * Creates an HttpClient instance with a URL object, max connections, and timeout settings.
     * 
     * @param url The base URL for the HTTP client
     * @param maxConnection Maximum number of concurrent connections
     * @param connectionTimeoutInMillis Connection timeout in milliseconds
     * @param readTimeoutInMillis Read timeout in milliseconds
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if url is null or any numeric parameter is negative
     */
    public static HttpClient create(final URL url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis) {
        return create(url, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, (HttpSettings) null);
    }

    /**
     * Creates an HttpClient instance with a URL object and all basic configuration options.
     * 
     * @param url The base URL for the HTTP client
     * @param maxConnection Maximum number of concurrent connections
     * @param connectionTimeoutInMillis Connection timeout in milliseconds
     * @param readTimeoutInMillis Read timeout in milliseconds
     * @param settings Additional HTTP settings
     * @return A new HttpClient instance
     * @throws UncheckedIOException if an I/O error occurs
     * @throws IllegalArgumentException if url is null or any numeric parameter is negative
     */
    public static HttpClient create(final URL url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis,
            final HttpSettings settings) throws UncheckedIOException {
        return create(url, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, settings, new AtomicInteger(0));
    }

    /**
     * Creates an HttpClient instance with a URL object and shared connection counter.
     * 
     * @param url The base URL for the HTTP client
     * @param maxConnection Maximum number of concurrent connections
     * @param connectionTimeoutInMillis Connection timeout in milliseconds
     * @param readTimeoutInMillis Read timeout in milliseconds
     * @param settings Additional HTTP settings
     * @param sharedActiveConnectionCounter Shared counter for active connections
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if url is null or any numeric parameter is negative
     */
    public static HttpClient create(final URL url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis,
            final HttpSettings settings, final AtomicInteger sharedActiveConnectionCounter) {
        return create(url, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, settings, sharedActiveConnectionCounter, null);
    }

    /**
     * Creates an HttpClient instance with a URL object and custom executor.
     * 
     * @param url The base URL for the HTTP client
     * @param maxConnection Maximum number of concurrent connections
     * @param connectionTimeoutInMillis Connection timeout in milliseconds
     * @param readTimeoutInMillis Read timeout in milliseconds
     * @param executor Custom executor for asynchronous operations
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if url is null or any numeric parameter is negative
     */
    public static HttpClient create(final URL url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis,
            final Executor executor) {
        return create(url, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, null, executor);
    }

    /**
     * Creates an HttpClient instance with a URL object, settings, and executor.
     * 
     * @param url The base URL for the HTTP client
     * @param maxConnection Maximum number of concurrent connections
     * @param connectionTimeoutInMillis Connection timeout in milliseconds
     * @param readTimeoutInMillis Read timeout in milliseconds
     * @param settings Additional HTTP settings
     * @param executor Custom executor for asynchronous operations
     * @return A new HttpClient instance
     * @throws UncheckedIOException if an I/O error occurs
     * @throws IllegalArgumentException if url is null or any numeric parameter is negative
     */
    public static HttpClient create(final URL url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis,
            final HttpSettings settings, final Executor executor) throws UncheckedIOException {
        return create(url, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, settings, new AtomicInteger(0), executor);
    }

    /**
     * Creates an HttpClient instance with a URL object and all configuration options.
     * 
     * @param url The base URL for the HTTP client
     * @param maxConnection Maximum number of concurrent connections
     * @param connectionTimeoutInMillis Connection timeout in milliseconds
     * @param readTimeoutInMillis Read timeout in milliseconds
     * @param settings Additional HTTP settings
     * @param sharedActiveConnectionCounter Shared counter for active connections
     * @param executor Custom executor for asynchronous operations
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if url is null or any numeric parameter is negative
     */
    public static HttpClient create(final URL url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis,
            final HttpSettings settings, final AtomicInteger sharedActiveConnectionCounter, final Executor executor) {
        return new HttpClient(url, null, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, settings, sharedActiveConnectionCounter, executor);
    }

    /**
     * Performs a GET request and returns the response as a String.
     * 
     * @return The response body as a String
     * @throws UncheckedIOException if an I/O error occurs
     * 
     * <p>Example:</p>
     * <pre>{@code
     * HttpClient client = HttpClient.create("https://api.example.com/users");
     * String response = client.get();
     * }</pre>
     */
    public String get() throws UncheckedIOException {
        return get(String.class);
    }

    /**
     * Performs a GET request with custom settings and returns the response as a String.
     * 
     * @param settings Additional HTTP settings for this request
     * @return The response body as a String
     * @throws UncheckedIOException if an I/O error occurs
     * 
     * <p>Example:</p>
     * <pre>{@code
     * HttpSettings settings = HttpSettings.create()
     *     .header("Accept", "application/json");
     * String response = client.get(settings);
     * }</pre>
     */
    public String get(final HttpSettings settings) throws UncheckedIOException {
        return get(settings, String.class);
    }

    /**
     * Performs a GET request with query parameters and returns the response as a String.
     * 
     * @param queryParameters Query parameters as a String, Map, or Bean object
     * @return The response body as a String
     * @throws UncheckedIOException if an I/O error occurs
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Map<String, Object> params = Map.of("page", 1, "size", 10);
     * String response = client.get(params);
     * }</pre>
     */
    public String get(final Object queryParameters) throws UncheckedIOException {
        return get(queryParameters, String.class);
    }

    /**
     * Performs a GET request with query parameters and custom settings.
     * 
     * @param queryParameters Query parameters as a String, Map, or Bean object
     * @param settings Additional HTTP settings for this request
     * @return The response body as a String
     * @throws UncheckedIOException if an I/O error occurs
     */
    public String get(final Object queryParameters, final HttpSettings settings) throws UncheckedIOException {
        return get(queryParameters, settings, String.class);
    }

    /**
     * Performs a GET request and deserializes the response to the specified type.
     * 
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object
     * @return The deserialized response object
     * @throws UncheckedIOException if an I/O error occurs
     * 
     * <p>Example:</p>
     * <pre>{@code
     * User user = client.get(User.class);
     * }</pre>
     */
    public <T> T get(final Class<T> resultClass) throws UncheckedIOException {
        return get(null, _settings, resultClass);
    }

    /**
     * Performs a GET request with custom settings and deserializes the response.
     * 
     * @param <T> The type of the response object
     * @param settings Additional HTTP settings for this request
     * @param resultClass The class of the expected response object
     * @return The deserialized response object
     * @throws UncheckedIOException if an I/O error occurs
     */
    public <T> T get(final HttpSettings settings, final Class<T> resultClass) throws UncheckedIOException {
        return get(null, settings, resultClass);
    }

    /**
     * Performs a GET request with query parameters and deserializes the response.
     * 
     * @param <T> The type of the response object
     * @param queryParameters Query parameters as a String, Map, or Bean object
     * @param resultClass The class of the expected response object
     * @return The deserialized response object
     * @throws UncheckedIOException if an I/O error occurs
     */
    public <T> T get(final Object queryParameters, final Class<T> resultClass) throws UncheckedIOException {
        return get(queryParameters, _settings, resultClass);
    }

    /**
     * Performs a GET request with all options and deserializes the response.
     * 
     * @param <T> The type of the response object
     * @param queryParameters Query parameters as a String, Map, or Bean object
     * @param settings Additional HTTP settings for this request
     * @param resultClass The class of the expected response object
     * @return The deserialized response object
     * @throws UncheckedIOException if an I/O error occurs
     */
    public <T> T get(final Object queryParameters, final HttpSettings settings, final Class<T> resultClass) throws UncheckedIOException {
        return execute(HttpMethod.GET, queryParameters, settings, resultClass);
    }

    /**
     * Performs a DELETE request and returns the response as a String.
     * 
     * @return The response body as a String
     * @throws UncheckedIOException if an I/O error occurs
     * 
     * <p>Example:</p>
     * <pre>{@code
     * String response = client.delete();
     * }</pre>
     */
    public String delete() throws UncheckedIOException {
        return delete(String.class);
    }

    /**
     * Performs a DELETE request with custom settings.
     * 
     * @param settings Additional HTTP settings for this request
     * @return The response body as a String
     * @throws UncheckedIOException if an I/O error occurs
     */
    public String delete(final HttpSettings settings) throws UncheckedIOException {
        return delete(settings, String.class);
    }

    /**
     * Performs a DELETE request with query parameters.
     * 
     * @param queryParameters Query parameters as a String, Map, or Bean object
     * @return The response body as a String
     * @throws UncheckedIOException if an I/O error occurs
     */
    public String delete(final Object queryParameters) throws UncheckedIOException {
        return delete(queryParameters, String.class);
    }

    /**
     * Performs a DELETE request with query parameters and custom settings.
     * 
     * @param queryParameters Query parameters as a String, Map, or Bean object
     * @param settings Additional HTTP settings for this request
     * @return The response body as a String
     * @throws UncheckedIOException if an I/O error occurs
     */
    public String delete(final Object queryParameters, final HttpSettings settings) throws UncheckedIOException {
        return delete(queryParameters, settings, String.class);
    }

    /**
     * Performs a DELETE request and deserializes the response.
     * 
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object
     * @return The deserialized response object
     * @throws UncheckedIOException if an I/O error occurs
     */
    public <T> T delete(final Class<T> resultClass) throws UncheckedIOException {
        return delete(null, _settings, resultClass);
    }

    /**
     * Performs a DELETE request with query parameters and deserializes the response.
     * 
     * @param <T> The type of the response object
     * @param queryParameters Query parameters as a String, Map, or Bean object
     * @param resultClass The class of the expected response object
     * @return The deserialized response object
     * @throws UncheckedIOException if an I/O error occurs
     */
    public <T> T delete(final Object queryParameters, final Class<T> resultClass) throws UncheckedIOException {
        return delete(queryParameters, _settings, resultClass);
    }

    /**
     * Performs a DELETE request with custom settings and deserializes the response.
     * 
     * @param <T> The type of the response object
     * @param settings Additional HTTP settings for this request
     * @param resultClass The class of the expected response object
     * @return The deserialized response object
     * @throws UncheckedIOException if an I/O error occurs
     */
    public <T> T delete(final HttpSettings settings, final Class<T> resultClass) throws UncheckedIOException {
        return delete(null, settings, resultClass);
    }

    /**
     * Performs a DELETE request with all options and deserializes the response.
     * 
     * @param <T> The type of the response object
     * @param queryParameters Query parameters as a String, Map, or Bean object
     * @param settings Additional HTTP settings for this request
     * @param resultClass The class of the expected response object
     * @return The deserialized response object
     * @throws UncheckedIOException if an I/O error occurs
     */
    public <T> T delete(final Object queryParameters, final HttpSettings settings, final Class<T> resultClass) throws UncheckedIOException {
        return execute(HttpMethod.DELETE, queryParameters, settings, resultClass);
    }

    /**
     * Performs a POST request with the specified request body and returns the response as a String.
     * 
     * @param request The request body (can be String, byte[], File, InputStream, Reader, or any object for JSON/XML serialization)
     * @return The response body as a String
     * @throws UncheckedIOException if an I/O error occurs
     * 
     * <p>Example:</p>
     * <pre>{@code
     * User user = new User("John", "Doe");
     * String response = client.post(user);
     * }</pre>
     */
    public String post(final Object request) throws UncheckedIOException {
        return post(request, String.class);
    }

    /**
     * Performs a POST request and deserializes the response.
     * 
     * @param <T> The type of the response object
     * @param request The request body
     * @param resultClass The class of the expected response object
     * @return The deserialized response object
     * @throws UncheckedIOException if an I/O error occurs
     * 
     * <p>Example:</p>
     * <pre>{@code
     * User newUser = new User("John", "Doe");
     * User createdUser = client.post(newUser, User.class);
     * }</pre>
     */
    public <T> T post(final Object request, final Class<T> resultClass) throws UncheckedIOException {
        return post(request, _settings, resultClass);
    }

    /**
     * Performs a POST request with custom settings.
     * 
     * @param request The request body
     * @param settings Additional HTTP settings for this request
     * @return The response body as a String
     * @throws UncheckedIOException if an I/O error occurs
     */
    public String post(final Object request, final HttpSettings settings) throws UncheckedIOException {
        return post(request, settings, String.class);
    }

    /**
     * Performs a POST request with custom settings and deserializes the response.
     * 
     * @param <T> The type of the response object
     * @param request The request body
     * @param settings Additional HTTP settings for this request
     * @param resultClass The class of the expected response object
     * @return The deserialized response object
     * @throws UncheckedIOException if an I/O error occurs
     */
    public <T> T post(final Object request, final HttpSettings settings, final Class<T> resultClass) throws UncheckedIOException {
        return execute(HttpMethod.POST, request, settings, resultClass);
    }

    /**
     * Performs a PUT request with the specified request body and returns the response as a String.
     * 
     * @param request The request body
     * @return The response body as a String
     * @throws UncheckedIOException if an I/O error occurs
     * 
     * <p>Example:</p>
     * <pre>{@code
     * User updatedUser = new User("John", "Smith");
     * String response = client.put(updatedUser);
     * }</pre>
     */
    public String put(final Object request) throws UncheckedIOException {
        return put(request, String.class);
    }

    /**
     * Performs a PUT request and deserializes the response.
     * 
     * @param <T> The type of the response object
     * @param request The request body
     * @param resultClass The class of the expected response object
     * @return The deserialized response object
     * @throws UncheckedIOException if an I/O error occurs
     */
    public <T> T put(final Object request, final Class<T> resultClass) throws UncheckedIOException {
        return put(request, _settings, resultClass);
    }

    /**
     * Performs a PUT request with custom settings.
     * 
     * @param request The request body
     * @param settings Additional HTTP settings for this request
     * @return The response body as a String
     * @throws UncheckedIOException if an I/O error occurs
     */
    public String put(final Object request, final HttpSettings settings) throws UncheckedIOException {
        return put(request, settings, String.class);
    }

    /**
     * Performs a PUT request with custom settings and deserializes the response.
     * 
     * @param <T> The type of the response object
     * @param request The request body
     * @param settings Additional HTTP settings for this request
     * @param resultClass The class of the expected response object
     * @return The deserialized response object
     * @throws UncheckedIOException if an I/O error occurs
     */
    public <T> T put(final Object request, final HttpSettings settings, final Class<T> resultClass) throws UncheckedIOException {
        return execute(HttpMethod.PUT, request, settings, resultClass);
    }

    //    //    TODO HTTP
    //    //    METHOD PATCH
    //    //    is not
    //    //    supported by HttpURLConnection.
    //
    //    /**
    //         *
    //         * @param request
    //         * @return
    //         * @throws UncheckedIOException the unchecked IO exception
    //         */
    //    public String patch(final Object request) throws UncheckedIOException {
    //        return patch(request, String.class);
    //    }
    //
    //    /**
    //     *
    //     * @param request
    //     * @param settings
    //     * @return
    //     * @throws UncheckedIOException the unchecked IO exception
    //     */
    //    public String patch(final Object request, final HttpSettings settings) throws UncheckedIOException {
    //        return patch(request, settings, String.class);
    //    }
    //
    //    /**
    //     *
    //     * @param request
    //     * @param resultClass
    //     * @param <T>
    //     * @return
    //     * @throws UncheckedIOException the unchecked IO exception
    //     */
    //    public <T> T patch(final Object request, final Class<T> resultClass) throws UncheckedIOException {
    //        return patch(request, _settings, resultClass);
    //    }
    //
    //    /**
    //     *
    //     * @param request
    //     * @param settings
    //     * @param resultClass
    //     * @param <T>
    //     * @return
    //     * @throws UncheckedIOException the unchecked IO exception
    //     */
    //    public <T> T patch(final Object request, final HttpSettings settings, final Class<T> resultClass) throws UncheckedIOException {
    //        return execute(HttpMethod.PATCH, request, settings, resultClass);
    //    }
    //
    //    /**
    //     *
    //     * @param request
    //     * @return
    //     */
    //    public ContinuableFuture<String> asyncPatch(final Object request) {
    //        return asyncPatch(request, String.class);
    //    }
    //
    //    /**
    //     *
    //     * @param request
    //     * @param settings
    //     * @return
    //     */
    //    public ContinuableFuture<String> asyncPatch(final Object request, final HttpSettings settings) {
    //        return asyncPatch(request, settings, String.class);
    //    }
    //
    //    /**
    //     *
    //     * @param request
    //     * @param resultClass
    //     * @param <T>
    //     * @return
    //     */
    //    public <T> ContinuableFuture<T> asyncPatch(final Object request, final Class<T> resultClass) {
    //        return asyncPatch(request, _settings, resultClass);
    //    }
    //
    //    /**
    //     *
    //     * @param request
    //     * @param settings
    //     * @param resultClass
    //     * @param <T>
    //     * @return
    //     */
    //    public <T> ContinuableFuture<T> asyncPatch(final Object request, final HttpSettings settings, final Class<T> resultClass) {
    //        return asyncExecute(HttpMethod.PATCH, request, settings, resultClass);
    //    }

    /**
     * Performs a HEAD request with default settings.
     * HEAD requests are used to retrieve headers without the response body.
     * 
     * @throws UncheckedIOException if an I/O error occurs
     * 
     * <p>Example:</p>
     * <pre>{@code
     * client.head(); // Check if resource exists
     * }</pre>
     */
    public void head() throws UncheckedIOException {
        head(_settings);
    }

    /**
     * Performs a HEAD request with custom settings.
     * 
     * @param settings Additional HTTP settings for this request
     * @throws UncheckedIOException if an I/O error occurs
     */
    public void head(final HttpSettings settings) throws UncheckedIOException {
        execute(HttpMethod.HEAD, null, settings, Void.class);
    }

    //    //    TODO HTTP
    //    //    METHOD PATCH
    //    //    is not
    //    //    supported by HttpURLConnection.
    //
    //    /**
    //         *
    //         * @param request
    //         * @return
    //         * @throws UncheckedIOException the unchecked IO exception
    //         */
    //    public String patch(final Object request) throws UncheckedIOException {
    //        return patch(request, String.class);
    //    }
    //
    //    /**
    //     *
    //     * @param request
    //     * @param settings
    //     * @return
    //     * @throws UncheckedIOException the unchecked IO exception
    //     */
    //    public String patch(final Object request, final HttpSettings settings) throws UncheckedIOException {
    //        return patch(request, settings, String.class);
    //    }
    //
    //    /**
    //     *
    //     * @param request
    //     * @param resultClass
    //     * @param <T>
    //     * @return
    //     * @throws UncheckedIOException the unchecked IO exception
    //     */
    //    public <T> T patch(final Object request, final Class<T> resultClass) throws UncheckedIOException {
    //        return patch(request, _settings, resultClass);
    //    }
    //
    //    /**
    //     *
    //     * @param request
    //     * @param settings
    //     * @param resultClass
    //     * @param <T>
    //     * @return
    //     * @throws UncheckedIOException the unchecked IO exception
    //     */
    //    public <T> T patch(final Object request, final HttpSettings settings, final Class<T> resultClass) throws UncheckedIOException {
    //        return execute(HttpMethod.PATCH, request, settings, resultClass);
    //    }
    //
    //    /**
    //     *
    //     * @param request
    //     * @return
    //     */
    //    public ContinuableFuture<String> asyncPatch(final Object request) {
    //        return asyncPatch(request, String.class);
    //    }
    //
    //    /**
    //     *
    //     * @param request
    //     * @param settings
    //     * @return
    //     */
    //    public ContinuableFuture<String> asyncPatch(final Object request, final HttpSettings settings) {
    //        return asyncPatch(request, settings, String.class);
    //    }
    //
    //    /**
    //     *
    //     * @param request
    //     * @param resultClass
    //     * @param <T>
    //     * @return
    //     */
    //    public <T> ContinuableFuture<T> asyncPatch(final Object request, final Class<T> resultClass) {
    //        return asyncPatch(request, _settings, resultClass);
    //    }
    //
    //    /**
    //     *
    //     * @param request
    //     * @param settings
    //     * @param resultClass
    //     * @param <T>
    //     * @return
    //     */
    //    public <T> ContinuableFuture<T> asyncPatch(final Object request, final HttpSettings settings, final Class<T> resultClass) {
    //        return asyncExecute(HttpMethod.PATCH, request, settings, resultClass);
    //    }

    /**
     * Executes an HTTP request with the specified method and request body.
     * 
     * @param httpMethod The HTTP method to use
     * @param request The request body (can be null for GET/DELETE)
     * @return The response body as a String
     * @throws UncheckedIOException if an I/O error occurs
     * 
     * <p>Example:</p>
     * <pre>{@code
     * String response = client.execute(HttpMethod.POST, requestBody);
     * }</pre>
     */
    public String execute(final HttpMethod httpMethod, final Object request) throws UncheckedIOException {
        return execute(httpMethod, request, String.class);
    }

    /**
     * Executes an HTTP request and deserializes the response.
     * 
     * @param <T> The type of the response object
     * @param httpMethod The HTTP method to use
     * @param request The request body (can be null for GET/DELETE)
     * @param resultClass The class of the expected response object
     * @return The deserialized response object
     * @throws UncheckedIOException if an I/O error occurs
     */
    public <T> T execute(final HttpMethod httpMethod, final Object request, final Class<T> resultClass) throws UncheckedIOException {
        return execute(httpMethod, request, _settings, resultClass);
    }

    /**
     * Executes an HTTP request with custom settings.
     * 
     * @param httpMethod The HTTP method to use
     * @param request The request body (can be null for GET/DELETE)
     * @param settings Additional HTTP settings for this request
     * @return The response body as a String
     * @throws UncheckedIOException if an I/O error occurs
     */
    public String execute(final HttpMethod httpMethod, final Object request, final HttpSettings settings) throws UncheckedIOException {
        return execute(httpMethod, request, settings, String.class);
    }

    /**
     * Executes an HTTP request with all options and deserializes the response.
     * This is the core method that all other request methods delegate to.
     * 
     * @param <T> The type of the response object
     * @param httpMethod The HTTP method to use
     * @param request The request body (can be null for GET/DELETE)
     * @param settings Additional HTTP settings for this request
     * @param resultClass The class of the expected response object
     * @return The deserialized response object, or null if resultClass is Void
     * @throws UncheckedIOException if an I/O error occurs
     */
    public <T> T execute(final HttpMethod httpMethod, final Object request, final HttpSettings settings, final Class<T> resultClass)
            throws UncheckedIOException {
        return execute(httpMethod, request, settings, resultClass, null, null);
    }

    /**
     * Executes an HTTP request and writes the response to a file.
     * 
     * @param httpMethod The HTTP method to use
     * @param request The request body (can be null for GET/DELETE)
     * @param settings Additional HTTP settings for this request
     * @param output The file to write the response to
     * @throws UncheckedIOException if an I/O error occurs
     * 
     * <p>Example:</p>
     * <pre>{@code
     * File outputFile = new File("response.json");
     * client.execute(HttpMethod.GET, null, settings, outputFile);
     * }</pre>
     */
    public void execute(final HttpMethod httpMethod, final Object request, final HttpSettings settings, final File output) throws UncheckedIOException {
        OutputStream os = null;

        try {
            os = IOUtil.newFileOutputStream(output);
            execute(httpMethod, request, settings, os);
        } finally {
            IOUtil.close(os);
        }
    }

    /**
     * Executes an HTTP request and writes the response to an output stream.
     * 
     * @param httpMethod The HTTP method to use
     * @param request The request body (can be null for GET/DELETE)
     * @param settings Additional HTTP settings for this request
     * @param output The output stream to write the response to
     * @throws UncheckedIOException if an I/O error occurs
     */
    public void execute(final HttpMethod httpMethod, final Object request, final HttpSettings settings, final OutputStream output) throws UncheckedIOException {
        execute(httpMethod, request, settings, null, output, null);
    }

    /**
     * Executes an HTTP request and writes the response to a writer.
     * 
     * @param httpMethod The HTTP method to use
     * @param request The request body (can be null for GET/DELETE)
     * @param settings Additional HTTP settings for this request
     * @param output The writer to write the response to
     * @throws UncheckedIOException if an I/O error occurs
     */
    public void execute(final HttpMethod httpMethod, final Object request, final HttpSettings settings, final Writer output) throws UncheckedIOException {
        execute(httpMethod, request, settings, null, null, output);
    }

    /**
     *
     * @param httpMethod
     * @param request
     * @param settings
     * @param resultClass
     * @param outputStream
     * @param outputWriter
     * @param <T>
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    private <T> T execute(final HttpMethod httpMethod, final Object request, final HttpSettings settings, final Class<T> resultClass,
            final OutputStream outputStream, final Writer outputWriter) throws UncheckedIOException {
        final Charset requestCharset = HttpUtil.getRequestCharset(settings == null || settings.headers().isEmpty() ? _settings.headers() : settings.headers());
        final ContentFormat requestContentFormat = getContentFormat(settings);
        final boolean doOutput = request != null && !(httpMethod == HttpMethod.GET || httpMethod == HttpMethod.DELETE);

        final HttpURLConnection connection = openConnection(httpMethod, request, settings, doOutput, resultClass);
        final long sentRequestAtMillis = System.currentTimeMillis();
        InputStream is = null;
        OutputStream os = null;

        try { //NOSONAR
            if (request != null && (requireBody(httpMethod))) {
                os = HttpUtil.getOutputStream(connection, requestContentFormat, getContentType(settings), getContentEncoding(settings));

                final Type<Object> type = N.typeOf(request.getClass());

                if (request instanceof File fileRequest) {
                    try (InputStream fileInputStream = IOUtil.newFileInputStream(fileRequest)) {
                        IOUtil.write(fileInputStream, os);
                    }
                } else if (type.isInputStream()) {
                    IOUtil.write((InputStream) request, os);
                } else if (type.isReader()) {
                    final BufferedWriter bw = Objectory.createBufferedWriter(IOUtil.newOutputStreamWriter(os, requestCharset));

                    try {
                        IOUtil.write((Reader) request, bw);

                        bw.flush();
                    } finally {
                        Objectory.recycle(bw);
                    }
                } else {
                    if (request instanceof String) {
                        IOUtil.write(((String) request).getBytes(requestCharset), os);
                    } else if (request.getClass().equals(byte[].class)) {
                        IOUtil.write((byte[]) request, os);
                    } else {
                        if (requestContentFormat == ContentFormat.KRYO && HttpUtil.kryoParser != null) {
                            HttpUtil.kryoParser.serialize(request, os);
                        } else if (requestContentFormat == ContentFormat.FormUrlEncoded) {
                            IOUtil.write(URLEncodedUtil.encode(request, requestCharset).getBytes(requestCharset), os);
                        } else {
                            final BufferedWriter bw = Objectory.createBufferedWriter(IOUtil.newOutputStreamWriter(os, requestCharset));

                            try {
                                HttpUtil.getParser(requestContentFormat).serialize(request, bw);

                                bw.flush();
                            } finally {
                                Objectory.recycle(bw);
                            }
                        }
                    }
                }

                HttpUtil.flush(os);
            }

            final int statusCode = connection.getResponseCode();
            final Map<String, List<String>> respHeaders = connection.getHeaderFields();
            final Charset respCharset = HttpUtil.getResponseCharset(respHeaders, requestCharset);
            final ContentFormat respContentFormat = HttpUtil.getResponseContentFormat(respHeaders, requestContentFormat);

            is = HttpUtil.getInputStream(connection, respContentFormat);

            if (!HttpUtil.isSuccessfulResponseCode(statusCode) && (resultClass == null || !resultClass.equals(HttpResponse.class))) {
                throw new UncheckedIOException(
                        new IOException(statusCode + ": " + connection.getResponseMessage() + ". " + IOUtil.readAllToString(is, respCharset)));
            }

            if (isOneWayRequest(settings, resultClass, outputStream, outputWriter)) {
                return null;
            } else {
                if (outputStream != null) {
                    IOUtil.write(is, outputStream, true);

                    return null;
                } else if (outputWriter != null) {
                    final BufferedReader br = Objectory.createBufferedReader(IOUtil.newInputStreamReader(is, respCharset));

                    try {
                        IOUtil.write(br, outputWriter, true);
                    } finally {
                        Objectory.recycle(br);
                    }

                    return null;
                } else {
                    if (resultClass.equals(HttpResponse.class)) {
                        return (T) new HttpResponse(_url, sentRequestAtMillis, System.currentTimeMillis(), statusCode, connection.getResponseMessage(),
                                respHeaders, IOUtil.readAllBytes(is), respContentFormat, respCharset);
                    } else {
                        if (resultClass.equals(String.class)) {
                            return (T) IOUtil.readAllToString(is, respCharset);
                        } else if (byte[].class.equals(resultClass)) {
                            return (T) IOUtil.readAllBytes(is);
                        } else {
                            if (respContentFormat == ContentFormat.KRYO && HttpUtil.kryoParser != null) {
                                return HttpUtil.kryoParser.deserialize(is, resultClass);
                            } else if (respContentFormat == ContentFormat.FormUrlEncoded) {
                                return URLEncodedUtil.decode(IOUtil.readAllToString(is, respCharset), resultClass);
                            } else {
                                final BufferedReader br = Objectory.createBufferedReader(IOUtil.newInputStreamReader(is, respCharset));

                                try {
                                    return HttpUtil.getParser(respContentFormat).deserialize(br, resultClass);
                                } finally {
                                    Objectory.recycle(br);
                                }
                            }
                        }
                    }
                }
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            close(os, is, connection);
        }
    }

    /**
     * Checks if is one way request.
     * @param settings
     * @param resultClass
     * @param outputStream
     * @param outputWriter
     *
     * @return {@code true}, if is one way request
     */
    boolean isOneWayRequest(final HttpSettings settings, final Class<?> resultClass, final OutputStream outputStream, final Writer outputWriter) {
        return (resultClass == null || Void.class.equals(resultClass) || (settings == null ? _settings.isOneWayRequest() : settings.isOneWayRequest()))
                && outputStream == null && outputWriter == null;
    }

    /**
     * Gets the content format.
     *
     * @param settings
     * @return
     */
    ContentFormat getContentFormat(final HttpSettings settings) {
        ContentFormat contentFormat = null;

        if (settings != null) {
            contentFormat = settings.getContentFormat();
        }

        if (contentFormat == null || contentFormat == ContentFormat.NONE) {
            contentFormat = _settings.getContentFormat();
        }

        return contentFormat;
    }

    /**
     * Gets the content type.
     *
     * @param settings
     * @return
     */
    String getContentType(final HttpSettings settings) {
        String contentType = null;

        if (settings != null) {
            contentType = settings.getContentType();
        }

        if (Strings.isEmpty(contentType)) {
            contentType = _settings.getContentType();
        }

        return contentType;
    }

    /**
     * Gets the content encoding.
     *
     * @param settings
     * @return
     */
    private String getContentEncoding(final HttpSettings settings) {
        String contentEncoding = null;

        if (settings != null) {
            contentEncoding = settings.getContentEncoding();
        }

        if (Strings.isEmpty(contentEncoding)) {
            contentEncoding = _settings.getContentEncoding();
        }

        return contentEncoding;
    }

    private boolean requireBody(final HttpMethod httpMethod) {
        return httpMethod == HttpMethod.POST || httpMethod == HttpMethod.PUT || httpMethod == HttpMethod.PATCH;
    }

    /**
     * Opens a new HTTP connection with the specified method and settings.
     * This method is primarily for advanced use cases where direct control over the connection is needed.
     * 
     * @param httpMethod The HTTP method to use
     * @param settings Additional HTTP settings for the connection
     * @param doOutput Whether the connection will send a request body
     * @param resultClass The expected result class (used for optimization)
     * @return A configured HttpURLConnection ready for use
     * @throws UncheckedIOException if an I/O error occurs
     */
    public HttpURLConnection openConnection(final HttpMethod httpMethod, final HttpSettings settings, final boolean doOutput, final Class<?> resultClass)
            throws UncheckedIOException {
        return openConnection(httpMethod, null, settings, doOutput, resultClass);
    }

    /**
     * Opens a new HTTP connection with query parameters and the specified settings.
     * This method is primarily for advanced use cases where direct control over the connection is needed.
     * 
     * @param httpMethod The HTTP method to use
     * @param queryParameters Query parameters for GET/DELETE requests
     * @param settings Additional HTTP settings for the connection
     * @param doOutput Whether the connection will send a request body
     * @param resultClass The expected result class (used for optimization)
     * @return A configured HttpURLConnection ready for use
     * @throws UncheckedIOException if an I/O error occurs or connection limit is exceeded
     */
    @SuppressWarnings("unused")
    public HttpURLConnection openConnection(final HttpMethod httpMethod, final Object queryParameters, final HttpSettings settings, final boolean doOutput,
            final Class<?> resultClass) throws UncheckedIOException {
        HttpURLConnection connection = null;

        if (_activeConnectionCounter.incrementAndGet() > _maxConnection) {
            _activeConnectionCounter.decrementAndGet();
            throw new RuntimeException("Can not get connection, exceeded max connection number: " + _maxConnection);
        }

        try {
            synchronized (_netURL) {
                URL netURL = _netURL;

                if (queryParameters != null && (httpMethod == HttpMethod.GET || httpMethod == HttpMethod.DELETE)) {
                    netURL = URI.create(URLEncodedUtil.encode(_url, queryParameters)).toURL();
                }

                final Proxy proxy = (settings == null ? _settings : settings).getProxy();

                if (proxy == null) {
                    connection = (HttpURLConnection) netURL.openConnection();
                } else {
                    connection = (HttpURLConnection) netURL.openConnection(proxy);
                }
            }

            if (connection instanceof HttpsURLConnection) {
                final SSLSocketFactory ssf = (settings == null ? _settings : settings).getSSLSocketFactory();

                if (ssf != null) {
                    ((HttpsURLConnection) connection).setSSLSocketFactory(ssf);
                }
            }

            int connectionTimeoutInMillis = _connectionTimeoutInMillis > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) _connectionTimeoutInMillis;

            if (settings != null && settings.getConnectionTimeout() > 0) {
                connectionTimeoutInMillis = Numbers.toIntExact(settings.getConnectionTimeout());
            }

            if (connectionTimeoutInMillis > 0) {
                connection.setConnectTimeout(connectionTimeoutInMillis);
            }

            int readTimeoutInMillis = _readTimeoutInMillis > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) _readTimeoutInMillis;

            if (settings != null && settings.getReadTimeout() > 0) {
                readTimeoutInMillis = Numbers.toIntExact(settings.getReadTimeout());
            }

            if (readTimeoutInMillis > 0) {
                connection.setReadTimeout(readTimeoutInMillis);
            }

            if (settings != null) {
                connection.setDoInput(settings.doInput());
                connection.setDoOutput(settings.doOutput());
            }

            connection.setUseCaches((settings != null && settings.getUseCaches()) || (_settings != null && _settings.getUseCaches()));

            //noinspection DataFlowIssue
            setHttpProperties(connection, settings == null || settings.headers().isEmpty() ? _settings : settings);

            // won't work for HttpURLConnection. 
            // com.landawn.abacus.exception.UncheckedIOException: java.net.ProtocolException: Cannot read from URLConnection if doInput=false (call setDoInput(true))
            //    if (isOneWayRequest(settings, resultClass)) {
            //        connection.setDoInput(false);
            //    } else {
            //        connection.setDoOutput(doOutput);
            //    }

            connection.setRequestMethod(httpMethod.name());

            return connection;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Sets the http properties.
     *
     * @param connection
     * @param settings
     * @throws UncheckedIOException the unchecked IO exception
     */
    void setHttpProperties(final HttpURLConnection connection, final HttpSettings settings) throws UncheckedIOException {
        final HttpHeaders headers = settings.headers();

        if (headers != null) {
            Object headerValue = null;

            for (final String headerName : headers.headerNameSet()) {
                // lazy set content-encoding
                // because if content-encoding(lz4/snappy/kryo...) is set but no parameter/result write to OutputStream,
                // error may happen when read the input stream in sever side.

                if (Names.CONTENT_ENCODING.equalsIgnoreCase(headerName)) {
                    continue;
                }

                headerValue = headers.get(headerName);

                connection.setRequestProperty(headerName, HttpHeaders.valueOf(headerValue));
            }
        }
    }

    /**
     *
     * @param os
     * @param is
     * @param connection
     */
    void close(final OutputStream os, final InputStream is, @SuppressWarnings("unused") final HttpURLConnection connection) { //NOSONAR
        try {
            IOUtil.closeQuietly(os);
            IOUtil.closeQuietly(is);
        } finally {
            _activeConnectionCounter.decrementAndGet();
        }

        // connection.disconnect();
    }

    /**
     * Performs an asynchronous GET request and returns the response as a String.
     * 
     * @return A ContinuableFuture that will complete with the response body
     * 
     * <p>Example:</p>
     * <pre>{@code
     * client.asyncGet()
     *     .thenAccept(response -> System.out.println("Response: " + response))
     *     .exceptionally(e -> { e.printStackTrace(); return null; });
     * }</pre>
     */
    public ContinuableFuture<String> asyncGet() {
        return asyncGet(String.class);
    }

    /**
     * Performs an asynchronous GET request with custom settings.
     * 
     * @param settings Additional HTTP settings for this request
     * @return A ContinuableFuture that will complete with the response body
     */
    public ContinuableFuture<String> asyncGet(final HttpSettings settings) {
        return asyncGet(settings, String.class);
    }

    /**
     * Performs an asynchronous GET request with query parameters.
     * 
     * @param queryParameters Query parameters as a String, Map, or Bean object
     * @return A ContinuableFuture that will complete with the response body
     */
    public ContinuableFuture<String> asyncGet(final Object queryParameters) {
        return asyncGet(queryParameters, String.class);
    }

    /**
     * Performs an asynchronous GET request with query parameters and custom settings.
     * 
     * @param queryParameters Query parameters as a String, Map, or Bean object
     * @param settings Additional HTTP settings for this request
     * @return A ContinuableFuture that will complete with the response body
     */
    public ContinuableFuture<String> asyncGet(final Object queryParameters, final HttpSettings settings) {
        return asyncGet(queryParameters, settings, String.class);
    }

    /**
     * Performs an asynchronous GET request and deserializes the response.
     * 
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object
     * @return A ContinuableFuture that will complete with the deserialized response
     * 
     * <p>Example:</p>
     * <pre>{@code
     * client.asyncGet(User.class)
     *     .thenAccept(user -> System.out.println("User: " + user.getName()));
     * }</pre>
     */
    public <T> ContinuableFuture<T> asyncGet(final Class<T> resultClass) {
        return asyncGet(null, _settings, resultClass);
    }

    /**
     * Performs an asynchronous GET request with query parameters and deserializes the response.
     * 
     * @param <T> The type of the response object
     * @param queryParameters Query parameters as a String, Map, or Bean object
     * @param resultClass The class of the expected response object
     * @return A ContinuableFuture that will complete with the deserialized response
     */
    public <T> ContinuableFuture<T> asyncGet(final Object queryParameters, final Class<T> resultClass) {
        return asyncGet(queryParameters, _settings, resultClass);
    }

    /**
     * Performs an asynchronous GET request with custom settings and deserializes the response.
     * 
     * @param <T> The type of the response object
     * @param settings Additional HTTP settings for this request
     * @param resultClass The class of the expected response object
     * @return A ContinuableFuture that will complete with the deserialized response
     */
    public <T> ContinuableFuture<T> asyncGet(final HttpSettings settings, final Class<T> resultClass) {
        return asyncGet(null, settings, resultClass);
    }

    /**
     * Performs an asynchronous GET request with all options and deserializes the response.
     * 
     * @param <T> The type of the response object
     * @param queryParameters Query parameters as a String, Map, or Bean object
     * @param settings Additional HTTP settings for this request
     * @param resultClass The class of the expected response object
     * @return A ContinuableFuture that will complete with the deserialized response
     */
    public <T> ContinuableFuture<T> asyncGet(final Object queryParameters, final HttpSettings settings, final Class<T> resultClass) {
        return asyncExecute(HttpMethod.GET, queryParameters, settings, resultClass);
    }

    /**
     * Performs an asynchronous DELETE request and returns the response as a String.
     * 
     * @return A ContinuableFuture that will complete with the response body
     */
    public ContinuableFuture<String> asyncDelete() {
        return asyncDelete(String.class);
    }

    /**
     * Performs an asynchronous DELETE request with query parameters.
     * 
     * @param queryParameters Query parameters as a String, Map, or Bean object
     * @return A ContinuableFuture that will complete with the response body
     */
    public ContinuableFuture<String> asyncDelete(final Object queryParameters) {
        return asyncDelete(queryParameters, String.class);
    }

    /**
     * Performs an asynchronous DELETE request with custom settings.
     * 
     * @param settings Additional HTTP settings for this request
     * @return A ContinuableFuture that will complete with the response body
     */
    public ContinuableFuture<String> asyncDelete(final HttpSettings settings) {
        return asyncDelete(settings, String.class);
    }

    /**
     * Performs an asynchronous DELETE request with query parameters and custom settings.
     * 
     * @param queryParameters Query parameters as a String, Map, or Bean object
     * @param settings Additional HTTP settings for this request
     * @return A ContinuableFuture that will complete with the response body
     */
    public ContinuableFuture<String> asyncDelete(final Object queryParameters, final HttpSettings settings) {
        return asyncDelete(queryParameters, settings, String.class);
    }

    /**
     * Performs an asynchronous DELETE request and deserializes the response.
     * 
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object
     * @return A ContinuableFuture that will complete with the deserialized response
     */
    public <T> ContinuableFuture<T> asyncDelete(final Class<T> resultClass) {
        return asyncDelete(null, _settings, resultClass);
    }

    /**
     * Performs an asynchronous DELETE request with query parameters and deserializes the response.
     * 
     * @param <T> The type of the response object
     * @param queryParameters Query parameters as a String, Map, or Bean object
     * @param resultClass The class of the expected response object
     * @return A ContinuableFuture that will complete with the deserialized response
     */
    public <T> ContinuableFuture<T> asyncDelete(final Object queryParameters, final Class<T> resultClass) {
        return asyncDelete(queryParameters, _settings, resultClass);
    }

    /**
     * Performs an asynchronous DELETE request with custom settings and deserializes the response.
     * 
     * @param <T> The type of the response object
     * @param settings Additional HTTP settings for this request
     * @param resultClass The class of the expected response object
     * @return A ContinuableFuture that will complete with the deserialized response
     */
    public <T> ContinuableFuture<T> asyncDelete(final HttpSettings settings, final Class<T> resultClass) {
        return asyncDelete(null, settings, resultClass);
    }

    /**
     * Performs an asynchronous DELETE request with all options and deserializes the response.
     * 
     * @param <T> The type of the response object
     * @param queryParameters Query parameters as a String, Map, or Bean object
     * @param settings Additional HTTP settings for this request
     * @param resultClass The class of the expected response object
     * @return A ContinuableFuture that will complete with the deserialized response
     */
    public <T> ContinuableFuture<T> asyncDelete(final Object queryParameters, final HttpSettings settings, final Class<T> resultClass) {
        return asyncExecute(HttpMethod.DELETE, queryParameters, settings, resultClass);
    }

    /**
     * Performs an asynchronous POST request with the specified request body.
     * 
     * @param request The request body
     * @return A ContinuableFuture that will complete with the response body
     * 
     * <p>Example:</p>
     * <pre>{@code
     * User newUser = new User("John", "Doe");
     * client.asyncPost(newUser)
     *     .thenAccept(response -> System.out.println("Created: " + response));
     * }</pre>
     */
    public ContinuableFuture<String> asyncPost(final Object request) {
        return asyncPost(request, String.class);
    }

    /**
     * Performs an asynchronous POST request and deserializes the response.
     * 
     * @param <T> The type of the response object
     * @param request The request body
     * @param resultClass The class of the expected response object
     * @return A ContinuableFuture that will complete with the deserialized response
     */
    public <T> ContinuableFuture<T> asyncPost(final Object request, final Class<T> resultClass) {
        return asyncPost(request, _settings, resultClass);
    }

    /**
     * Performs an asynchronous POST request with custom settings.
     * 
     * @param request The request body
     * @param settings Additional HTTP settings for this request
     * @return A ContinuableFuture that will complete with the response body
     */
    public ContinuableFuture<String> asyncPost(final Object request, final HttpSettings settings) {
        return asyncPost(request, settings, String.class);
    }

    /**
     * Performs an asynchronous POST request with custom settings and deserializes the response.
     * 
     * @param <T> The type of the response object
     * @param request The request body
     * @param settings Additional HTTP settings for this request
     * @param resultClass The class of the expected response object
     * @return A ContinuableFuture that will complete with the deserialized response
     */
    public <T> ContinuableFuture<T> asyncPost(final Object request, final HttpSettings settings, final Class<T> resultClass) {
        return asyncExecute(HttpMethod.POST, request, settings, resultClass);
    }

    /**
     * Performs an asynchronous PUT request with the specified request body.
     * 
     * @param request The request body
     * @return A ContinuableFuture that will complete with the response body
     */
    public ContinuableFuture<String> asyncPut(final Object request) {
        return asyncPut(request, String.class);
    }

    /**
     * Performs an asynchronous PUT request and deserializes the response.
     * 
     * @param <T> The type of the response object
     * @param request The request body
     * @param resultClass The class of the expected response object
     * @return A ContinuableFuture that will complete with the deserialized response
     */
    public <T> ContinuableFuture<T> asyncPut(final Object request, final Class<T> resultClass) {
        return asyncPut(request, _settings, resultClass);
    }

    /**
     * Performs an asynchronous PUT request with custom settings.
     * 
     * @param request The request body
     * @param settings Additional HTTP settings for this request
     * @return A ContinuableFuture that will complete with the response body
     */
    public ContinuableFuture<String> asyncPut(final Object request, final HttpSettings settings) {
        return asyncPut(request, settings, String.class);
    }

    /**
     * Performs an asynchronous PUT request with custom settings and deserializes the response.
     * 
     * @param <T> The type of the response object
     * @param request The request body
     * @param settings Additional HTTP settings for this request
     * @param resultClass The class of the expected response object
     * @return A ContinuableFuture that will complete with the deserialized response
     */
    public <T> ContinuableFuture<T> asyncPut(final Object request, final HttpSettings settings, final Class<T> resultClass) {
        return asyncExecute(HttpMethod.PUT, request, settings, resultClass);
    }

    // TODO HTTP METHOD PATCH is not supported by HttpURLConnection.
    //    /**
    //     *
    //     * @param request
    //     * @return
    //     * @throws UncheckedIOException the unchecked IO exception
    //     */
    //    public String patch(final Object request) throws UncheckedIOException {
    //        return patch(request, String.class);
    //    }
    //
    //    /**
    //     *
    //     * @param request
    //     * @param settings
    //     * @return
    //     * @throws UncheckedIOException the unchecked IO exception
    //     */
    //    public String patch(final Object request, final HttpSettings settings) throws UncheckedIOException {
    //        return patch(request, settings, String.class);
    //    }
    //
    //    /**
    //     *
    //     * @param <T>
    //     * @param request
    //     * @param resultClass
    //     * @return
    //     * @throws UncheckedIOException the unchecked IO exception
    //     */
    //    public <T> T patch(final Object request, final Class<T> resultClass) throws UncheckedIOException {
    //        return patch(request, _settings, resultClass);
    //    }
    //
    //    /**
    //     *
    //     * @param <T>
    //     * @param request
    //     * @param settings
    //     * @param resultClass
    //     * @return
    //     * @throws UncheckedIOException the unchecked IO exception
    //     */
    //    public <T> T patch(final Object request, final HttpSettings settings, final Class<T> resultClass) throws UncheckedIOException {
    //        return execute(HttpMethod.PATCH, request, settings, resultClass);
    //    }
    //
    //    /**
    //     *
    //     * @param request
    //     * @return
    //     */
    //    public ContinuableFuture<String> asyncPatch(final Object request) {
    //        return asyncPatch(request, String.class);
    //    }
    //
    //    /**
    //     *
    //     * @param request
    //     * @param settings
    //     * @return
    //     */
    //    public ContinuableFuture<String> asyncPatch(final Object request, final HttpSettings settings) {
    //        return asyncPatch(request, settings, String.class);
    //    }
    //
    //    /**
    //     *
    //     * @param <T>
    //     * @param request
    //     * @param resultClass
    //     * @return
    //     */
    //    public <T> ContinuableFuture<T> asyncPatch(final Object request, final Class<T> resultClass) {
    //        return asyncPatch(request, _settings, resultClass);
    //    }
    //
    //    /**
    //     *
    //     * @param <T>
    //     * @param request
    //     * @param settings
    //     * @param resultClass
    //     * @return
    //     */
    //    public <T> ContinuableFuture<T> asyncPatch(final Object request, final HttpSettings settings, final Class<T> resultClass) {
    //        return asyncExecute(HttpMethod.PATCH, request, settings, resultClass);
    //    }

    /**
     * Performs an asynchronous HEAD request with default settings.
     * 
     * @return A ContinuableFuture that will complete when the request finishes
     */
    public ContinuableFuture<Void> asyncHead() {
        return asyncHead(_settings);
    }

    /**
     * Performs an asynchronous HEAD request with custom settings.
     * 
     * @param settings Additional HTTP settings for this request
     * @return A ContinuableFuture that will complete when the request finishes
     */
    public ContinuableFuture<Void> asyncHead(final HttpSettings settings) {
        return asyncExecute(HttpMethod.HEAD, null, settings, Void.class);
    }

    /**
     * Executes an asynchronous HTTP request with the specified method and request body.
     * 
     * @param httpMethod The HTTP method to use
     * @param request The request body (can be null for GET/DELETE)
     * @return A ContinuableFuture that will complete with the response body
     */
    public ContinuableFuture<String> asyncExecute(final HttpMethod httpMethod, final Object request) {
        return asyncExecute(httpMethod, request, String.class);
    }

    /**
     * Executes an asynchronous HTTP request and deserializes the response.
     * 
     * @param <T> The type of the response object
     * @param httpMethod The HTTP method to use
     * @param request The request body (can be null for GET/DELETE)
     * @param resultClass The class of the expected response object
     * @return A ContinuableFuture that will complete with the deserialized response
     */
    public <T> ContinuableFuture<T> asyncExecute(final HttpMethod httpMethod, final Object request, final Class<T> resultClass) {
        return asyncExecute(httpMethod, request, _settings, resultClass);
    }

    /**
     * Executes an asynchronous HTTP request with custom settings.
     * 
     * @param httpMethod The HTTP method to use
     * @param request The request body (can be null for GET/DELETE)
     * @param settings Additional HTTP settings for this request
     * @return A ContinuableFuture that will complete with the response body
     */
    public ContinuableFuture<String> asyncExecute(final HttpMethod httpMethod, final Object request, final HttpSettings settings) {
        return asyncExecute(httpMethod, request, settings, String.class);
    }

    /**
     * Executes an asynchronous HTTP request with all options and deserializes the response.
     * This is the core async method that all other async request methods delegate to.
     * 
     * @param <T> The type of the response object
     * @param httpMethod The HTTP method to use
     * @param request The request body (can be null for GET/DELETE)
     * @param settings Additional HTTP settings for this request
     * @param resultClass The class of the expected response object
     * @return A ContinuableFuture that will complete with the deserialized response
     */
    public <T> ContinuableFuture<T> asyncExecute(final HttpMethod httpMethod, final Object request, final HttpSettings settings, final Class<T> resultClass) {
        final Callable<T> cmd = () -> execute(httpMethod, request, settings, resultClass);

        return _asyncExecutor.execute(cmd);
    }

    /**
     * Executes an asynchronous HTTP request and writes the response to a file.
     * 
     * @param httpMethod The HTTP method to use
     * @param request The request body (can be null for GET/DELETE)
     * @param settings Additional HTTP settings for this request
     * @param output The file to write the response to
     * @return A ContinuableFuture that will complete when the file is written
     */
    public ContinuableFuture<Void> asyncExecute(final HttpMethod httpMethod, final Object request, final HttpSettings settings, final File output) {
        final Callable<Void> cmd = () -> {
            execute(httpMethod, request, settings, output);

            return null;
        };

        return _asyncExecutor.execute(cmd);
    }

    /**
     * Executes an asynchronous HTTP request and writes the response to an output stream.
     * 
     * @param httpMethod The HTTP method to use
     * @param request The request body (can be null for GET/DELETE)
     * @param settings Additional HTTP settings for this request
     * @param output The output stream to write the response to
     * @return A ContinuableFuture that will complete when the stream is written
     */
    public ContinuableFuture<Void> asyncExecute(final HttpMethod httpMethod, final Object request, final HttpSettings settings, final OutputStream output) {
        final Callable<Void> cmd = () -> {
            execute(httpMethod, request, settings, output);

            return null;
        };

        return _asyncExecutor.execute(cmd);
    }

    /**
     * Executes an asynchronous HTTP request and writes the response to a writer.
     * 
     * @param httpMethod The HTTP method to use
     * @param request The request body (can be null for GET/DELETE)
     * @param settings Additional HTTP settings for this request
     * @param output The writer to write the response to
     * @return A ContinuableFuture that will complete when the writer is written
     */
    public ContinuableFuture<Void> asyncExecute(final HttpMethod httpMethod, final Object request, final HttpSettings settings, final Writer output) {
        final Callable<Void> cmd = () -> {
            execute(httpMethod, request, settings, output);

            return null;
        };

        return _asyncExecutor.execute(cmd);
    }

    /**
     * Closes this HTTP client and releases any resources.
     * Note: Currently this method does nothing as connections are managed per-request.
     * The method is provided for API consistency and future enhancements.
     */
    public synchronized void close() {
        // do nothing.
    }
}