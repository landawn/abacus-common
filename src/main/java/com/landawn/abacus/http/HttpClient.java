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
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import com.landawn.abacus.exception.HttpResponseException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.http.HttpHeaders.Names;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.AsyncExecutor;
import com.landawn.abacus.util.ContinuableFuture;
import com.landawn.abacus.util.ExceptionUtil;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.URLEncodedUtil;
import com.landawn.abacus.util.cs;

/**
 * A thread-safe HTTP client built on {@link HttpURLConnection}, with automatic serialization of
 * request bodies, automatic deserialization of responses, optional compression, SSL/TLS and proxy
 * configuration, and asynchronous execution through {@link ContinuableFuture}.
 *
 * <p><b>Thread safety:</b> instances are immutable after construction (URL, timeouts, default
 * {@link HttpSettings}) and every request opens its own connection, so a single client can be
 * shared across threads without external synchronization.</p>
 *
 * <p><b>Supported methods:</b> GET, POST, PUT, DELETE, HEAD, OPTIONS and TRACE.
 * {@link HttpURLConnection} cannot issue {@link HttpMethod#PATCH} or {@link HttpMethod#CONNECT} at all, so
 * every {@code execute}/{@code asyncExecute}/{@code openConnection} overload rejects those two with an
 * {@link UnsupportedOperationException} before a connection is opened, instead of letting them surface as a
 * {@code java.net.ProtocolException} wrapped in an {@link UncheckedIOException};
 * see {@link HttpMethod#PATCH} for workarounds.</p>
 *
 * <p><b>How the {@code request} argument is used</b> — this is decided by the HTTP method, not by
 * the argument type:</p>
 * <table border="1" style="border-collapse: collapse;">
 *   <caption><b>Request payload routing</b></caption>
 *   <tr><th>Method</th><th>Meaning of {@code request}</th></tr>
 *   <tr><td>POST, PUT, PATCH, OPTIONS</td><td>the request <b>body</b>, serialized as described below</td></tr>
 *   <tr><td>GET, DELETE, HEAD, TRACE, CONNECT</td><td><b>query parameters</b> appended to the URL
 *       (a raw pre-encoded query {@code String}, a {@code Map}, or a bean), UTF-8 percent-encoded</td></tr>
 * </table>
 *
 * <p>To send a body with a method that normally takes query parameters (for example a DELETE with a
 * payload), use {@link HttpRequest#body(Object)}, which tracks that distinction explicitly.</p>
 *
 * <p><b>Request body serialization and {@code Content-Type}:</b> the {@code Content-Type} placed on
 * the wire always describes the bytes actually written. When neither the client-level nor the
 * per-request {@link HttpSettings} declares one, it is derived from the payload:</p>
 * <table border="1" style="border-collapse: collapse;">
 *   <caption><b>Default request Content-Type</b></caption>
 *   <tr><th>Payload</th><th>Written as</th><th>Default {@code Content-Type}</th></tr>
 *   <tr><td>{@code String}, {@link Reader}</td><td>raw characters in the request charset</td><td>{@code text/plain; charset=...}</td></tr>
 *   <tr><td>{@code byte[]}, {@link File}, {@link InputStream}</td><td>raw bytes</td><td>{@code application/octet-stream}</td></tr>
 *   <tr><td>any other object</td><td>serialized with the parser for the active {@link ContentFormat}
 *       (JSON when none is configured)</td><td>the format's media type, e.g. {@code application/json}</td></tr>
 * </table>
 *
 * <p><b>Compression:</b> GZIP, LZ4 and Snappy are applied to request bodies and decoded from
 * responses; Brotli is decode-only. Compression is selected by the active {@link ContentFormat} (or
 * equivalently by a {@code Content-Encoding} header, from which the format is derived). Because the
 * client performs the compression, a body must <i>not</i> be pre-compressed by the caller; a
 * {@code Content-Encoding} that contradicts the active format is rejected with an
 * {@link IllegalArgumentException} rather than silently mislabelling the body.</p>
 *
 * <p><b>Error handling:</b> a non-2xx status raises an {@link HttpResponseException} (a subclass of
 * {@link UncheckedIOException}) that carries the status code, status message, response headers and a
 * bounded prefix of the error body — unless the requested result type is {@link HttpResponse}, in
 * which case the response is returned as-is for the caller to inspect (a request marked one-way via
 * {@link HttpSettings#setOneWayRequest(boolean)} still returns {@code null}, {@code HttpResponse}
 * and {@code head()} included, because its body is never read). Other I/O failures are
 * wrapped in {@link UncheckedIOException}. If an error body cannot be opened or decoded, the
 * status exception carries an empty body. The {@code async*} variants never throw these
 * synchronously; the same failures are delivered through the returned {@link ContinuableFuture}.</p>
 *
 * <p><b>Concurrency limit:</b> {@code maxConnection} caps the number of <i>concurrent in-flight
 * requests</i> for this client (or for the group of clients sharing an
 * {@link AtomicInteger} counter). It is not a connection-pool size, and it does not queue: a request
 * that would exceed the limit fails immediately with a {@link RejectedExecutionException}. Socket
 * reuse is handled by the JDK's own keep-alive cache.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Reuse one client per endpoint; it is thread-safe.
 * HttpClient client = HttpClient.create("http://localhost:18080/users", 16, 5000, 30000);
 *
 * // GET with query parameters, deserialized into a bean.
 * User user = client.get(Map.of("id", 123), User.class);
 *
 * // POST a bean; the body is serialized as JSON and labelled application/json.
 * User created = client.post(new CreateUserRequest("John", "Doe"), User.class);
 *
 * // Inspect the raw response instead of letting a non-2xx status throw.
 * HttpResponse response = client.get(HttpResponse.class);
 * if (!response.isSuccessful()) {
 *     logger.warn("failed: {} {}", response.statusCode(), response.message());
 * }
 *
 * // Asynchronous execution.
 * ContinuableFuture<User> future = client.asyncGet(User.class);
 * }</pre>
 *
 * <p><b>Per-request settings:</b> a {@link HttpSettings} passed to an individual call is layered on
 * top of the client's defaults rather than replacing them — headers are merged (same-named request
 * headers win), and timeouts, proxy, SSL factory, content metadata and the connection flags fall
 * back to the client-level value when the request settings do not specify one. For each timeout,
 * a positive request setting takes precedence over a positive client-level setting, followed by
 * the timeout passed to {@code create}; the resolved value is capped at {@link Integer#MAX_VALUE}.</p>
 *
 * <p><b>Relationship to {@link HttpRequest}:</b> {@code HttpRequest} is a fluent builder over this
 * class. Note that their no-argument accessors differ by design: {@code HttpClient.get()} returns the
 * response body as a {@code String}, while {@code HttpRequest.get()} returns the full
 * {@link HttpResponse}. Pass an explicit result class to either when the distinction matters.</p>
 *
 * @see HttpSettings
 * @see HttpRequest
 * @see HttpResponse
 * @see HttpResponseException
 * @see HttpURLConnection
 * @see ContinuableFuture
 * @see URLEncodedUtil
 * @see <a href="https://tools.ietf.org/html/rfc7231">RFC 7231: HTTP/1.1 Semantics and Content</a>
 */
public final class HttpClient implements AutoCloseable {

    // Loading this class deliberately does NOT set the JVM-global "http.keepAlive" /
    // "http.maxConnections" system properties. Overwriting them silently replaced whatever the
    // hosting application had configured, and "http.maxConnections" is read once by the JDK's
    // keep-alive cache anyway, so setting it at an arbitrary class-loading moment was unreliable.
    // Configure them with -D flags if the JDK defaults are not wanted.

    // Default client limits and timeouts.
    /**
     * Default cap on concurrent in-flight requests per {@code HttpClient} instance ({@value}).
     *
     * @see #create(String, int, long, long)
     */
    public static final int DEFAULT_MAX_CONNECTION = 16;

    /** Default connection timeout in milliseconds (8 seconds). */
    public static final int DEFAULT_CONNECTION_TIMEOUT = 8000;

    /** Default read timeout in milliseconds (16 seconds). */
    public static final int DEFAULT_READ_TIMEOUT = 16000;

    // Immutable request configuration and shared concurrency state.
    private final String _url; //NOSONAR

    private final int _maxConnection; //NOSONAR

    private final long _connectTimeoutInMillis; //NOSONAR

    private final long _readTimeoutInMillis; //NOSONAR

    private final HttpSettings _settings; //NOSONAR

    final AsyncExecutor _asyncExecutor; //NOSONAR

    private final URL _netURL; //NOSONAR

    private final AtomicInteger _activeConnectionCounter; //NOSONAR

    /**
     * Creates a new {@code HttpClient} with a string URL and delegates to the core constructor.
     *
     * @param url the request URL as a string
     * @param maxConnection the maximum number of concurrently tracked connections
     * @param connectTimeoutInMillis the connection timeout in milliseconds
     * @param readTimeoutInMillis the read timeout in milliseconds
     * @param settings the default HTTP settings for requests made by this client
     * @param sharedActiveConnectionCounter the shared counter used to enforce connection limits
     * @param executor the executor used for asynchronous operations, or {@code null} to use the default
     */
    private HttpClient(final String url, final int maxConnection, final long connectTimeoutInMillis, final long readTimeoutInMillis,
            final HttpSettings settings, final AtomicInteger sharedActiveConnectionCounter, final Executor executor) {
        this(null, url, maxConnection, connectTimeoutInMillis, readTimeoutInMillis, settings, sharedActiveConnectionCounter, executor);
    }

    /**
     * Creates a new {@code HttpClient} with normalized configuration and validated URL protocol.
     *
     * @param netUrl the pre-parsed URL, or {@code null} to derive from {@code url}
     * @param url the request URL as a string
     * @param maxConnection the maximum number of concurrent in-flight requests
     * @param connectTimeoutInMillis the connection timeout in milliseconds
     * @param readTimeoutInMillis the read timeout in milliseconds
     * @param settings the default HTTP settings for requests made by this client
     * @param sharedActiveConnectionCounter the shared counter used to enforce the in-flight limit
     * @param executor the executor used for asynchronous operations, or {@code null} to use the default
     * @throws IllegalArgumentException if the URL is invalid, is not valid URI syntax, uses an
     *         unsupported protocol, or a timeout/limit is negative.
     */
    private HttpClient(final URL netUrl, final String url, final int maxConnection, final long connectTimeoutInMillis, final long readTimeoutInMillis,
            final HttpSettings settings, final AtomicInteger sharedActiveConnectionCounter, final Executor executor) throws IllegalArgumentException {
        N.checkArgument(netUrl != null || Strings.isNotEmpty(url), "url cannot be null or empty");

        if ((maxConnection < 0) || (connectTimeoutInMillis < 0) || (readTimeoutInMillis < 0)) {
            throw new IllegalArgumentException("maxConnection, connectTimeoutInMillis or readTimeoutInMillis cannot be less than 0:" + maxConnection + ", "
                    + connectTimeoutInMillis + ", " + readTimeoutInMillis);
        }

        _netURL = netUrl == null ? createNetUrl(url) : netUrl;
        checkSupportedProtocol(_netURL);

        _url = Strings.isEmpty(url) ? _netURL.toString() : url;

        if (netUrl != null) {
            // A URL accepted by java.net.URL is not necessarily valid URI syntax (a space in the
            // path, for example). Appending query parameters re-parses the URL through URI, so
            // accepting such a URL used to defer the failure to the first request that carried query
            // parameters. Reject it up front instead; the URL is never silently re-encoded, which
            // could change what it addresses. The string form needs no check here: createNetUrl(..)
            // has already parsed exactly this string through URI.
            checkValidUriSyntax(_url);
        }

        _maxConnection = (maxConnection == 0) ? DEFAULT_MAX_CONNECTION : maxConnection;
        _connectTimeoutInMillis = (connectTimeoutInMillis == 0) ? DEFAULT_CONNECTION_TIMEOUT : connectTimeoutInMillis;
        _readTimeoutInMillis = (readTimeoutInMillis == 0) ? DEFAULT_READ_TIMEOUT : readTimeoutInMillis;
        _settings = settings == null ? HttpSettings.create() : settings.copy();

        _asyncExecutor = executor == null ? HttpUtil.DEFAULT_ASYNC_EXECUTOR : new AsyncExecutor(executor);

        _activeConnectionCounter = N.checkArgNotNull(sharedActiveConnectionCounter, cs.sharedActiveConnectionCounter);
    }

    /**
     * Converts a URL string to a {@link URL} and validates that the protocol is supported.
     *
     * @param url the URL string to parse
     * @return the parsed URL
     * @throws IllegalArgumentException if URL is {@code null}, is not valid URI syntax, or uses an
     *         unsupported protocol.
     */
    private static URL createNetUrl(final String url) throws IllegalArgumentException {
        N.checkArgNotNull(url, cs.url);

        final URI uri;

        try {
            uri = URI.create(url);
        } catch (final IllegalArgumentException e) {
            // Reported exactly like the URL-object path, which validates the same thing.
            throw new IllegalArgumentException("Invalid URI syntax in url: " + url + ". " + e.getMessage(), e);
        }

        try {
            final URL netUrl = uri.toURL();
            checkSupportedProtocol(netUrl);

            return netUrl;
        } catch (final MalformedURLException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Validates that the specified URL uses HTTP or HTTPS protocol.
     *
     * @param netUrl the URL to validate
     * @throws IllegalArgumentException if protocol is not HTTP/HTTPS.
     */
    private static void checkSupportedProtocol(final URL netUrl) throws IllegalArgumentException {
        final String protocol = netUrl == null ? null : netUrl.getProtocol();

        if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
            throw new IllegalArgumentException("Only HTTP/HTTPS protocol is supported, but got: " + protocol);
        }
    }

    /**
     * Validates that the URL is also valid URI syntax, which the query-parameter path requires.
     *
     * @param url the URL string to validate
     * @throws IllegalArgumentException if the URL cannot be parsed as a {@link URI}.
     */
    private static void checkValidUriSyntax(final String url) throws IllegalArgumentException {
        try {
            URI.create(url);
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid URI syntax in url: " + url + ". " + e.getMessage(), e);
        }
    }

    /**
     * Gets the base URL configured for this HTTP client.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpClient client = HttpClient.create("http://localhost:18080");
     * String baseUrl = client.url();
     * }</pre>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpClient client = HttpClient.create("http://localhost:18080");
     * }</pre>
     *
     * @param url The base URL for the HTTP client
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if url is {@code null}, empty, not valid URI syntax, or uses an
     *         unsupported protocol.
     */
    public static HttpClient create(final String url) throws IllegalArgumentException {
        return create(url, DEFAULT_MAX_CONNECTION);
    }

    /**
     * Creates an HttpClient instance with the specified URL and maximum connections.
     * Uses default values for connection timeout and read timeout.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpClient client = HttpClient.create("http://localhost:18080", 32);
     * }</pre>
     *
     * @param url The base URL for the HTTP client
     * @param maxConnection Maximum number of concurrent in-flight requests for this client;
     *        {@code 0} selects {@link #DEFAULT_MAX_CONNECTION}. This is not a connection-pool size:
     *        a request that would exceed it fails immediately with a {@link RejectedExecutionException}
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if url is {@code null}, empty, not valid URI syntax, uses an
     *         unsupported protocol, or maxConnection is negative.
     */
    public static HttpClient create(final String url, final int maxConnection) throws IllegalArgumentException {
        return create(url, maxConnection, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    /**
     * Creates an HttpClient instance with the specified URL and timeout settings.
     * Uses default value for max connections.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpClient client = HttpClient.create("http://localhost:18080", 5000, 10000);
     * }</pre>
     *
     * @param url The base URL for the HTTP client
     * @param connectTimeoutInMillis Connection timeout in milliseconds; {@code 0} selects
     *        {@link #DEFAULT_CONNECTION_TIMEOUT} (it does <i>not</i> mean "no timeout")
     * @param readTimeoutInMillis Read timeout in milliseconds; {@code 0} selects
     *        {@link #DEFAULT_READ_TIMEOUT} (it does <i>not</i> mean "no timeout")
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if url is {@code null}, empty, not valid URI syntax, uses an
     *         unsupported protocol, or a timeout is negative.
     */
    public static HttpClient create(final String url, final long connectTimeoutInMillis, final long readTimeoutInMillis) throws IllegalArgumentException {
        return create(url, DEFAULT_MAX_CONNECTION, connectTimeoutInMillis, readTimeoutInMillis);
    }

    /**
     * Creates an HttpClient instance with the specified URL, max connections, and timeout settings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpClient client = HttpClient.create("http://localhost:18080", 32, 5000, 10000);
     * }</pre>
     *
     * @param url The base URL for the HTTP client
     * @param maxConnection Maximum number of concurrent in-flight requests for this client;
     *        {@code 0} selects {@link #DEFAULT_MAX_CONNECTION}. This is not a connection-pool size:
     *        a request that would exceed it fails immediately with a {@link RejectedExecutionException}
     * @param connectTimeoutInMillis Connection timeout in milliseconds; {@code 0} selects
     *        {@link #DEFAULT_CONNECTION_TIMEOUT} (it does <i>not</i> mean "no timeout")
     * @param readTimeoutInMillis Read timeout in milliseconds; {@code 0} selects
     *        {@link #DEFAULT_READ_TIMEOUT} (it does <i>not</i> mean "no timeout")
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if url is {@code null}, empty, not valid URI syntax, uses an
     *         unsupported protocol, or any numeric parameter is negative.
     */
    public static HttpClient create(final String url, final int maxConnection, final long connectTimeoutInMillis, final long readTimeoutInMillis)
            throws IllegalArgumentException {
        return create(url, maxConnection, connectTimeoutInMillis, readTimeoutInMillis, (HttpSettings) null);
    }

    /**
     * Creates an HttpClient instance with the specified URL, max connections, timeout settings, and HTTP settings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpSettings settings = HttpSettings.create()
     *     .setContentType("application/json")
     *     .header("Authorization", "Bearer token123");
     * HttpClient client = HttpClient.create("http://localhost:18080", 16, 5000, 10000, settings);
     * }</pre>
     *
     * @param url The base URL for the HTTP client
     * @param maxConnection Maximum number of concurrent in-flight requests for this client;
     *        {@code 0} selects {@link #DEFAULT_MAX_CONNECTION}. This is not a connection-pool size:
     *        a request that would exceed it fails immediately with a {@link RejectedExecutionException}
     * @param connectTimeoutInMillis Connection timeout in milliseconds; {@code 0} selects
     *        {@link #DEFAULT_CONNECTION_TIMEOUT} (it does <i>not</i> mean "no timeout")
     * @param readTimeoutInMillis Read timeout in milliseconds; {@code 0} selects
     *        {@link #DEFAULT_READ_TIMEOUT} (it does <i>not</i> mean "no timeout")
     * @param settings Additional HTTP settings (headers, content type, etc.)
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if url is {@code null}, empty, not valid URI syntax, uses an
     *         unsupported protocol, or any numeric parameter is negative.
     */
    public static HttpClient create(final String url, final int maxConnection, final long connectTimeoutInMillis, final long readTimeoutInMillis,
            final HttpSettings settings) throws IllegalArgumentException {
        return create(url, maxConnection, connectTimeoutInMillis, readTimeoutInMillis, settings, new AtomicInteger(0));
    }

    /**
     * Creates an HttpClient instance with a shared active connection counter.
     * This allows multiple HttpClient instances to share a connection limit across all instances.
     * Useful when you need to enforce a global connection limit across multiple HTTP endpoints.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AtomicInteger sharedCounter = new AtomicInteger(0);
     * HttpClient client1 = HttpClient.create("http://localhost:18080/api1", 10, 5000, 10000, null, sharedCounter);
     * HttpClient client2 = HttpClient.create("http://localhost:18080/api2", 10, 5000, 10000, null, sharedCounter);
     * // Both clients share a maximum of 10 concurrent connections total
     * }</pre>
     *
     * @param url The base URL for the HTTP client
     * @param maxConnection Maximum number of concurrent in-flight requests for this client;
     *        {@code 0} selects {@link #DEFAULT_MAX_CONNECTION}. This is not a connection-pool size:
     *        a request that would exceed it fails immediately with a {@link RejectedExecutionException}
     * @param connectTimeoutInMillis Connection timeout in milliseconds; {@code 0} selects
     *        {@link #DEFAULT_CONNECTION_TIMEOUT} (it does <i>not</i> mean "no timeout")
     * @param readTimeoutInMillis Read timeout in milliseconds; {@code 0} selects
     *        {@link #DEFAULT_READ_TIMEOUT} (it does <i>not</i> mean "no timeout")
     * @param settings Additional HTTP settings
     * @param sharedActiveConnectionCounter Shared counter for active connections across multiple HttpClient instances; must not be {@code null}
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if url is {@code null}, empty, not valid URI syntax, uses an
     *         unsupported protocol, any numeric parameter is negative, or
     *         {@code sharedActiveConnectionCounter} is {@code null}.
     */
    public static HttpClient create(final String url, final int maxConnection, final long connectTimeoutInMillis, final long readTimeoutInMillis,
            final HttpSettings settings, final AtomicInteger sharedActiveConnectionCounter) throws IllegalArgumentException {
        return new HttpClient(url, maxConnection, connectTimeoutInMillis, readTimeoutInMillis, settings, sharedActiveConnectionCounter, null);
    }

    /**
     * Creates an HttpClient instance with a custom executor for async operations.
     * The executor will be used for all asynchronous HTTP requests initiated by this client.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Executor customExecutor = ForkJoinPool.commonPool();
     * HttpClient client = HttpClient.create("http://localhost:18080", 16, 5000, 10000, customExecutor);
     * client.asyncGet(User.class).thenRunAsync(user -> System.out.println(user));
     * }</pre>
     *
     * @param url The base URL for the HTTP client
     * @param maxConnection Maximum number of concurrent in-flight requests for this client;
     *        {@code 0} selects {@link #DEFAULT_MAX_CONNECTION}. This is not a connection-pool size:
     *        a request that would exceed it fails immediately with a {@link RejectedExecutionException}
     * @param connectTimeoutInMillis Connection timeout in milliseconds; {@code 0} selects
     *        {@link #DEFAULT_CONNECTION_TIMEOUT} (it does <i>not</i> mean "no timeout")
     * @param readTimeoutInMillis Read timeout in milliseconds; {@code 0} selects
     *        {@link #DEFAULT_READ_TIMEOUT} (it does <i>not</i> mean "no timeout")
     * @param executor Custom executor for asynchronous operations
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if {@code executor} is {@code null}, or url is {@code null}, empty,
     *         not valid URI syntax, uses an unsupported protocol, or any numeric parameter is negative.
     */
    public static HttpClient create(final String url, final int maxConnection, final long connectTimeoutInMillis, final long readTimeoutInMillis,
            final Executor executor) throws IllegalArgumentException {
        N.checkArgNotNull(executor, cs.executor);

        return create(url, maxConnection, connectTimeoutInMillis, readTimeoutInMillis, null, executor);
    }

    /**
     * Creates an HttpClient instance with the specified URL, settings, and custom executor.
     * Combines HTTP settings configuration with a custom async executor.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpSettings settings = HttpSettings.create()
     *     .setContentFormat(ContentFormat.JSON)
     *     .header("Authorization", "Bearer token");
     * Executor executor = ForkJoinPool.commonPool();
     * HttpClient client = HttpClient.create("http://localhost:18080", 20, 5000, 15000, settings, executor);
     * }</pre>
     *
     * @param url The base URL for the HTTP client
     * @param maxConnection Maximum number of concurrent in-flight requests for this client;
     *        {@code 0} selects {@link #DEFAULT_MAX_CONNECTION}. This is not a connection-pool size:
     *        a request that would exceed it fails immediately with a {@link RejectedExecutionException}
     * @param connectTimeoutInMillis Connection timeout in milliseconds; {@code 0} selects
     *        {@link #DEFAULT_CONNECTION_TIMEOUT} (it does <i>not</i> mean "no timeout")
     * @param readTimeoutInMillis Read timeout in milliseconds; {@code 0} selects
     *        {@link #DEFAULT_READ_TIMEOUT} (it does <i>not</i> mean "no timeout")
     * @param settings Additional HTTP settings (headers, content type, proxy, SSL, etc.)
     * @param executor Custom executor for asynchronous operations
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if {@code executor} is {@code null}, or url is {@code null}, empty,
     *         not valid URI syntax, uses an unsupported protocol, or any numeric parameter is negative.
     */
    public static HttpClient create(final String url, final int maxConnection, final long connectTimeoutInMillis, final long readTimeoutInMillis,
            final HttpSettings settings, final Executor executor) throws IllegalArgumentException {
        N.checkArgNotNull(executor, cs.executor);

        return create(url, maxConnection, connectTimeoutInMillis, readTimeoutInMillis, settings, new AtomicInteger(0), executor);
    }

    /**
     * Creates an HttpClient instance with all configuration options.
     * This is the most comprehensive factory method allowing full control over all settings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AtomicInteger sharedCounter = new AtomicInteger(0);
     * HttpSettings settings = HttpSettings.create().setContentFormat(ContentFormat.JSON);
     * Executor executor = ForkJoinPool.commonPool();
     * HttpClient client = HttpClient.create(
     *     "http://localhost:18080", 20, 5000, 10000, settings, sharedCounter, executor
     * );
     * }</pre>
     *
     * @param url The base URL for the HTTP client
     * @param maxConnection Maximum number of concurrent in-flight requests sharing
     *        {@code sharedActiveConnectionCounter}; {@code 0} selects {@link #DEFAULT_MAX_CONNECTION}
     * @param connectTimeoutInMillis Connection timeout in milliseconds; {@code 0} selects
     *        {@link #DEFAULT_CONNECTION_TIMEOUT} (it does <i>not</i> mean "no timeout")
     * @param readTimeoutInMillis Read timeout in milliseconds; {@code 0} selects
     *        {@link #DEFAULT_READ_TIMEOUT} (it does <i>not</i> mean "no timeout")
     * @param settings Additional HTTP settings (headers, content type, proxy, SSL, etc.)
     * @param sharedActiveConnectionCounter Shared counter for managing active connections across multiple clients; must not be {@code null}
     * @param executor Custom executor for asynchronous operations; must not be {@code null}
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if {@code executor} or {@code sharedActiveConnectionCounter} is {@code null},
     *         or url is {@code null}, empty, not valid URI syntax, uses an unsupported protocol, or any
     *         numeric parameter is negative.
     */
    public static HttpClient create(final String url, final int maxConnection, final long connectTimeoutInMillis, final long readTimeoutInMillis,
            final HttpSettings settings, final AtomicInteger sharedActiveConnectionCounter, final Executor executor) throws IllegalArgumentException {
        N.checkArgNotNull(executor, cs.executor);

        return new HttpClient(url, maxConnection, connectTimeoutInMillis, readTimeoutInMillis, settings, sharedActiveConnectionCounter, executor);
    }

    /**
     * Creates an HttpClient instance with a URL object and default settings.
     * Uses default values for max connections, connection timeout, and read timeout.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * URL apiUrl = new URL("http://localhost:18080");
     * HttpClient client = HttpClient.create(apiUrl);
     * }</pre>
     *
     * @param url The base URL for the HTTP client (as a java.net.URL object)
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if url is {@code null}, is not valid URI syntax, or uses an
     *         unsupported protocol.
     */
    public static HttpClient create(final URL url) throws IllegalArgumentException {
        return create(url, DEFAULT_MAX_CONNECTION);
    }

    /**
     * Creates an HttpClient instance with a URL object and maximum connections.
     * Uses default values for connection timeout and read timeout.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * URL apiUrl = new URL("http://localhost:18080");
     * HttpClient client = HttpClient.create(apiUrl, 32);
     * }</pre>
     *
     * @param url The base URL for the HTTP client (as a java.net.URL object)
     * @param maxConnection Maximum number of concurrent in-flight requests for this client;
     *        {@code 0} selects {@link #DEFAULT_MAX_CONNECTION}. This is not a connection-pool size:
     *        a request that would exceed it fails immediately with a {@link RejectedExecutionException}
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if url is {@code null}, is not valid URI syntax, uses an
     *         unsupported protocol, or maxConnection is negative.
     */
    public static HttpClient create(final URL url, final int maxConnection) throws IllegalArgumentException {
        return create(url, maxConnection, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    /**
     * Creates an HttpClient instance with a URL object and timeout settings.
     * Uses default value for max connections.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * URL apiUrl = new URL("http://localhost:18080");
     * HttpClient client = HttpClient.create(apiUrl, 5000, 10000);
     * }</pre>
     *
     * @param url The base URL for the HTTP client (as a java.net.URL object)
     * @param connectTimeoutInMillis Connection timeout in milliseconds; {@code 0} selects
     *        {@link #DEFAULT_CONNECTION_TIMEOUT} (it does <i>not</i> mean "no timeout")
     * @param readTimeoutInMillis Read timeout in milliseconds; {@code 0} selects
     *        {@link #DEFAULT_READ_TIMEOUT} (it does <i>not</i> mean "no timeout")
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if url is {@code null}, is not valid URI syntax, uses an
     *         unsupported protocol, or a timeout is negative.
     */
    public static HttpClient create(final URL url, final long connectTimeoutInMillis, final long readTimeoutInMillis) throws IllegalArgumentException {
        return create(url, DEFAULT_MAX_CONNECTION, connectTimeoutInMillis, readTimeoutInMillis);
    }

    /**
     * Creates an HttpClient instance with a URL object, max connections, and timeout settings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * URL apiUrl = new URL("http://localhost:18080");
     * HttpClient client = HttpClient.create(apiUrl, 32, 5000, 10000);
     * }</pre>
     *
     * @param url The base URL for the HTTP client (as a java.net.URL object)
     * @param maxConnection Maximum number of concurrent in-flight requests for this client;
     *        {@code 0} selects {@link #DEFAULT_MAX_CONNECTION}. This is not a connection-pool size:
     *        a request that would exceed it fails immediately with a {@link RejectedExecutionException}
     * @param connectTimeoutInMillis Connection timeout in milliseconds; {@code 0} selects
     *        {@link #DEFAULT_CONNECTION_TIMEOUT} (it does <i>not</i> mean "no timeout")
     * @param readTimeoutInMillis Read timeout in milliseconds; {@code 0} selects
     *        {@link #DEFAULT_READ_TIMEOUT} (it does <i>not</i> mean "no timeout")
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if url is {@code null}, is not valid URI syntax, uses an
     *         unsupported protocol, or any numeric parameter is negative.
     */
    public static HttpClient create(final URL url, final int maxConnection, final long connectTimeoutInMillis, final long readTimeoutInMillis)
            throws IllegalArgumentException {
        return create(url, maxConnection, connectTimeoutInMillis, readTimeoutInMillis, (HttpSettings) null);
    }

    /**
     * Creates an HttpClient instance with a URL object and all basic configuration options.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * URL apiUrl = new URL("http://localhost:18080");
     * HttpSettings settings = HttpSettings.create()
     *     .setContentType("application/json")
     *     .header("Authorization", "Bearer token123");
     * HttpClient client = HttpClient.create(apiUrl, 16, 5000, 10000, settings);
     * }</pre>
     *
     * @param url The base URL for the HTTP client (as a java.net.URL object)
     * @param maxConnection Maximum number of concurrent in-flight requests for this client;
     *        {@code 0} selects {@link #DEFAULT_MAX_CONNECTION}. This is not a connection-pool size:
     *        a request that would exceed it fails immediately with a {@link RejectedExecutionException}
     * @param connectTimeoutInMillis Connection timeout in milliseconds; {@code 0} selects
     *        {@link #DEFAULT_CONNECTION_TIMEOUT} (it does <i>not</i> mean "no timeout")
     * @param readTimeoutInMillis Read timeout in milliseconds; {@code 0} selects
     *        {@link #DEFAULT_READ_TIMEOUT} (it does <i>not</i> mean "no timeout")
     * @param settings Additional HTTP settings (headers, content type, proxy, SSL, etc.)
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if url is {@code null}, is not valid URI syntax, uses an
     *         unsupported protocol, or any numeric parameter is negative.
     */
    public static HttpClient create(final URL url, final int maxConnection, final long connectTimeoutInMillis, final long readTimeoutInMillis,
            final HttpSettings settings) throws IllegalArgumentException {
        return create(url, maxConnection, connectTimeoutInMillis, readTimeoutInMillis, settings, new AtomicInteger(0));
    }

    /**
     * Creates an HttpClient instance with a URL object and shared connection counter.
     * Allows multiple HttpClient instances to share a connection limit.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AtomicInteger sharedCounter = new AtomicInteger(0);
     * URL apiUrl1 = new URL("http://localhost:18080/api1");
     * URL apiUrl2 = new URL("http://localhost:18080/api2");
     * HttpClient client1 = HttpClient.create(apiUrl1, 10, 5000, 10000, null, sharedCounter);
     * HttpClient client2 = HttpClient.create(apiUrl2, 10, 5000, 10000, null, sharedCounter);
     * // Both clients share a maximum of 10 concurrent connections total
     * }</pre>
     *
     * @param url The base URL for the HTTP client (as a java.net.URL object)
     * @param maxConnection Maximum number of concurrent in-flight requests for this client;
     *        {@code 0} selects {@link #DEFAULT_MAX_CONNECTION}. This is not a connection-pool size:
     *        a request that would exceed it fails immediately with a {@link RejectedExecutionException}
     * @param connectTimeoutInMillis Connection timeout in milliseconds; {@code 0} selects
     *        {@link #DEFAULT_CONNECTION_TIMEOUT} (it does <i>not</i> mean "no timeout")
     * @param readTimeoutInMillis Read timeout in milliseconds; {@code 0} selects
     *        {@link #DEFAULT_READ_TIMEOUT} (it does <i>not</i> mean "no timeout")
     * @param settings Additional HTTP settings (headers, content type, proxy, SSL, etc.)
     * @param sharedActiveConnectionCounter Shared counter for active connections across multiple clients; must not be {@code null}
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if url is {@code null}, is not valid URI syntax, uses an
     *         unsupported protocol, any numeric parameter is negative, or
     *         {@code sharedActiveConnectionCounter} is {@code null}.
     */
    public static HttpClient create(final URL url, final int maxConnection, final long connectTimeoutInMillis, final long readTimeoutInMillis,
            final HttpSettings settings, final AtomicInteger sharedActiveConnectionCounter) throws IllegalArgumentException {
        return new HttpClient(url, null, maxConnection, connectTimeoutInMillis, readTimeoutInMillis, settings, sharedActiveConnectionCounter, null);
    }

    /**
     * Creates an HttpClient instance with a URL object and custom executor.
     * The executor will be used for all asynchronous HTTP requests.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * URL apiUrl = new URL("http://localhost:18080");
     * Executor customExecutor = ForkJoinPool.commonPool();
     * HttpClient client = HttpClient.create(apiUrl, 16, 5000, 10000, customExecutor);
     * client.asyncGet(User.class).thenRunAsync(user -> System.out.println(user));
     * }</pre>
     *
     * @param url The base URL for the HTTP client (as a java.net.URL object)
     * @param maxConnection Maximum number of concurrent in-flight requests for this client;
     *        {@code 0} selects {@link #DEFAULT_MAX_CONNECTION}. This is not a connection-pool size:
     *        a request that would exceed it fails immediately with a {@link RejectedExecutionException}
     * @param connectTimeoutInMillis Connection timeout in milliseconds; {@code 0} selects
     *        {@link #DEFAULT_CONNECTION_TIMEOUT} (it does <i>not</i> mean "no timeout")
     * @param readTimeoutInMillis Read timeout in milliseconds; {@code 0} selects
     *        {@link #DEFAULT_READ_TIMEOUT} (it does <i>not</i> mean "no timeout")
     * @param executor Custom executor for asynchronous operations
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if {@code executor} is {@code null}, url is {@code null}, is not
     *         valid URI syntax, uses an unsupported protocol, or any numeric parameter is negative.
     */
    public static HttpClient create(final URL url, final int maxConnection, final long connectTimeoutInMillis, final long readTimeoutInMillis,
            final Executor executor) throws IllegalArgumentException {
        N.checkArgNotNull(executor, cs.executor);

        return create(url, maxConnection, connectTimeoutInMillis, readTimeoutInMillis, null, executor);
    }

    /**
     * Creates an HttpClient instance with a URL object, settings, and custom executor.
     * Combines HTTP settings configuration with a custom async executor.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * URL apiUrl = new URL("http://localhost:18080");
     * HttpSettings settings = HttpSettings.create()
     *     .setContentFormat(ContentFormat.JSON)
     *     .header("Authorization", "Bearer token");
     * Executor executor = ForkJoinPool.commonPool();
     * HttpClient client = HttpClient.create(apiUrl, 20, 5000, 15000, settings, executor);
     * }</pre>
     *
     * @param url The base URL for the HTTP client (as a java.net.URL object)
     * @param maxConnection Maximum number of concurrent in-flight requests for this client;
     *        {@code 0} selects {@link #DEFAULT_MAX_CONNECTION}. This is not a connection-pool size:
     *        a request that would exceed it fails immediately with a {@link RejectedExecutionException}
     * @param connectTimeoutInMillis Connection timeout in milliseconds; {@code 0} selects
     *        {@link #DEFAULT_CONNECTION_TIMEOUT} (it does <i>not</i> mean "no timeout")
     * @param readTimeoutInMillis Read timeout in milliseconds; {@code 0} selects
     *        {@link #DEFAULT_READ_TIMEOUT} (it does <i>not</i> mean "no timeout")
     * @param settings Additional HTTP settings (headers, content type, proxy, SSL, etc.)
     * @param executor Custom executor for asynchronous operations
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if {@code executor} is {@code null}, url is {@code null}, is not
     *         valid URI syntax, uses an unsupported protocol, or any numeric parameter is negative.
     */
    public static HttpClient create(final URL url, final int maxConnection, final long connectTimeoutInMillis, final long readTimeoutInMillis,
            final HttpSettings settings, final Executor executor) throws IllegalArgumentException {
        N.checkArgNotNull(executor, cs.executor);

        return create(url, maxConnection, connectTimeoutInMillis, readTimeoutInMillis, settings, new AtomicInteger(0), executor);
    }

    /**
     * Creates an HttpClient instance with a URL object and all configuration options.
     * This is the most comprehensive factory method for URL-based clients.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * URL apiUrl = new URL("http://localhost:18080");
     * AtomicInteger sharedCounter = new AtomicInteger(0);
     * HttpSettings settings = HttpSettings.create().setContentFormat(ContentFormat.JSON);
     * Executor executor = ForkJoinPool.commonPool();
     * HttpClient client = HttpClient.create(
     *     apiUrl, 20, 5000, 10000, settings, sharedCounter, executor
     * );
     * }</pre>
     *
     * @param url The base URL for the HTTP client (as a java.net.URL object)
     * @param maxConnection Maximum number of concurrent in-flight requests for this client;
     *        {@code 0} selects {@link #DEFAULT_MAX_CONNECTION}. This is not a connection-pool size:
     *        a request that would exceed it fails immediately with a {@link RejectedExecutionException}
     * @param connectTimeoutInMillis Connection timeout in milliseconds; {@code 0} selects
     *        {@link #DEFAULT_CONNECTION_TIMEOUT} (it does <i>not</i> mean "no timeout")
     * @param readTimeoutInMillis Read timeout in milliseconds; {@code 0} selects
     *        {@link #DEFAULT_READ_TIMEOUT} (it does <i>not</i> mean "no timeout")
     * @param settings Additional HTTP settings (headers, content type, proxy, SSL, etc.)
     * @param sharedActiveConnectionCounter Shared counter for managing active connections across multiple clients; must not be {@code null}
     * @param executor Custom executor for asynchronous operations; must not be {@code null}
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if {@code executor} or {@code sharedActiveConnectionCounter} is {@code null},
     *         url is {@code null}, is not valid URI syntax, uses an unsupported protocol, or any numeric
     *         parameter is negative.
     */
    public static HttpClient create(final URL url, final int maxConnection, final long connectTimeoutInMillis, final long readTimeoutInMillis,
            final HttpSettings settings, final AtomicInteger sharedActiveConnectionCounter, final Executor executor) throws IllegalArgumentException {
        N.checkArgNotNull(executor, cs.executor);

        return new HttpClient(url, null, maxConnection, connectTimeoutInMillis, readTimeoutInMillis, settings, sharedActiveConnectionCounter, executor);
    }

    /**
     * Performs a GET request and returns the response as a String.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpClient client = HttpClient.create("http://localhost:18080/users");
     * String response = client.get();
     * }</pre>
     *
     * @return The response body as a String
     * @throws RejectedExecutionException if this client already has {@code maxConnection} requests in flight
     * @throws UncheckedIOException if opening the connection, transmitting the request, or reading or writing the response fails
     * @throws HttpResponseException if the response status code is not 2xx
     */
    public String get() throws RejectedExecutionException, UncheckedIOException, HttpResponseException {
        return get(String.class);
    }

    /**
     * Performs a GET request with custom settings and returns the response as a String.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpSettings settings = HttpSettings.create()
     *     .header("Accept", "application/json");
     * String response = client.get(settings);
     * }</pre>
     *
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @return The response body as a String
     * @throws RejectedExecutionException if this client already has {@code maxConnection} requests in flight
     * @throws UncheckedIOException if opening the connection, transmitting the request, or reading or writing the response fails
     * @throws HttpResponseException if the response status code is not 2xx
     */
    public String get(final HttpSettings settings) throws RejectedExecutionException, UncheckedIOException, HttpResponseException {
        return get(settings, String.class);
    }

    /**
     * Performs a GET request with query parameters and returns the response as a String.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> params = Map.of("page", 1, "size", 10);
     * String response = client.get(params);
     * }</pre>
     *
     * @param queryParameters Query parameters appended to the URL: a pre-encoded query
     *        {@code String}, a {@code Map}, or a bean. UTF-8 percent-encoded. May be {@code null}
     * @return The response body as a String
     * @throws RejectedExecutionException if this client already has {@code maxConnection} requests in flight
     * @throws UncheckedIOException if opening the connection, transmitting the request, or reading or writing the response fails
     * @throws HttpResponseException if the response status code is not 2xx
     */
    public String get(final Object queryParameters) throws RejectedExecutionException, UncheckedIOException, HttpResponseException {
        return get(queryParameters, String.class);
    }

    /**
     * Performs a GET request with query parameters and custom settings, returning the response as a String.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> params = Map.of("page", 1, "size", 10);
     * HttpSettings settings = HttpSettings.create().header("Accept", "application/json");
     * String response = client.get(params, settings);
     * }</pre>
     *
     * @param queryParameters Query parameters appended to the URL: a pre-encoded query
     *        {@code String}, a {@code Map}, or a bean. UTF-8 percent-encoded. May be {@code null}
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @return The response body as a String
     * @throws RejectedExecutionException if this client already has {@code maxConnection} requests in flight
     * @throws UncheckedIOException if opening the connection, transmitting the request, or reading or writing the response fails
     * @throws HttpResponseException if the response status code is not 2xx
     */
    public String get(final Object queryParameters, final HttpSettings settings)
            throws RejectedExecutionException, UncheckedIOException, HttpResponseException {
        return get(queryParameters, settings, String.class);
    }

    /**
     * Performs a GET request and deserializes the response to the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = client.get(User.class);
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object
     * @return The deserialized response object
     * @throws RejectedExecutionException if this client already has {@code maxConnection} requests in flight
     * @throws UncheckedIOException if opening the connection, transmitting the request, or reading or writing the response fails
     * @throws HttpResponseException if the response status code is not 2xx and {@code resultClass}
     *         is not {@link HttpResponse}
     */
    public <T> T get(final Class<T> resultClass) throws RejectedExecutionException, UncheckedIOException, HttpResponseException {
        return get(null, _settings, resultClass);
    }

    /**
     * Performs a GET request with custom settings and deserializes the response to the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpSettings settings = HttpSettings.create().header("Accept", "application/json");
     * String response = client.get(settings, String.class);
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @param resultClass The class of the expected response object (for deserialization)
     * @return The deserialized response object
     * @throws RejectedExecutionException if this client already has {@code maxConnection} requests in flight
     * @throws UncheckedIOException if opening the connection, transmitting the request, or reading or writing the response fails
     * @throws HttpResponseException if the response status code is not 2xx and {@code resultClass}
     *         is not {@link HttpResponse}
     */
    public <T> T get(final HttpSettings settings, final Class<T> resultClass) throws RejectedExecutionException, UncheckedIOException, HttpResponseException {
        return get(null, settings, resultClass);
    }

    /**
     * Performs a GET request with query parameters and deserializes the response to the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> params = Map.of("active", true);
     * String response = client.get(params, String.class);
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param queryParameters Query parameters appended to the URL: a pre-encoded query
     *        {@code String}, a {@code Map}, or a bean. UTF-8 percent-encoded. May be {@code null}
     * @param resultClass The class of the expected response object (for deserialization)
     * @return The deserialized response object
     * @throws RejectedExecutionException if this client already has {@code maxConnection} requests in flight
     * @throws UncheckedIOException if opening the connection, transmitting the request, or reading or writing the response fails
     * @throws HttpResponseException if the response status code is not 2xx and {@code resultClass}
     *         is not {@link HttpResponse}
     */
    public <T> T get(final Object queryParameters, final Class<T> resultClass) throws RejectedExecutionException, UncheckedIOException, HttpResponseException {
        return get(queryParameters, _settings, resultClass);
    }

    /**
     * Performs a GET request with all options and deserializes the response to the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> params = Map.of("active", true);
     * HttpSettings settings = HttpSettings.create().header("Accept", "application/json");
     * String response = client.get(params, settings, String.class);
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param queryParameters Query parameters appended to the URL: a pre-encoded query
     *        {@code String}, a {@code Map}, or a bean. UTF-8 percent-encoded. May be {@code null}
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @param resultClass The class of the expected response object (for deserialization)
     * @return The deserialized response object
     * @throws RejectedExecutionException if this client already has {@code maxConnection} requests in flight
     * @throws UncheckedIOException if opening the connection, transmitting the request, or reading or writing the response fails
     * @throws HttpResponseException if the response status code is not 2xx and {@code resultClass}
     *         is not {@link HttpResponse}
     */
    public <T> T get(final Object queryParameters, final HttpSettings settings, final Class<T> resultClass)
            throws RejectedExecutionException, UncheckedIOException, HttpResponseException {
        return execute(HttpMethod.GET, queryParameters, settings, resultClass);
    }

    /**
     * Performs a DELETE request and returns the response as a String.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String response = client.delete();
     * }</pre>
     *
     * @return The response body as a String
     * @throws RejectedExecutionException if this client already has {@code maxConnection} requests in flight
     * @throws UncheckedIOException if opening the connection, transmitting the request, or reading or writing the response fails
     * @throws HttpResponseException if the response status code is not 2xx
     */
    public String delete() throws RejectedExecutionException, UncheckedIOException, HttpResponseException {
        return delete(String.class);
    }

    /**
     * Performs a DELETE request with custom settings and returns the response as a String.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpSettings settings = HttpSettings.create().header("Accept", "application/json");
     * String response = client.delete(settings);
     * }</pre>
     *
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @return The response body as a String
     * @throws RejectedExecutionException if this client already has {@code maxConnection} requests in flight
     * @throws UncheckedIOException if opening the connection, transmitting the request, or reading or writing the response fails
     * @throws HttpResponseException if the response status code is not 2xx
     */
    public String delete(final HttpSettings settings) throws RejectedExecutionException, UncheckedIOException, HttpResponseException {
        return delete(settings, String.class);
    }

    /**
     * Performs a DELETE request with query parameters and returns the response as a String.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> params = Map.of("hardDelete", true);
     * String response = client.delete(params);
     * }</pre>
     *
     * @param queryParameters Query parameters appended to the URL: a pre-encoded query
     *        {@code String}, a {@code Map}, or a bean. UTF-8 percent-encoded. May be {@code null}
     * @return The response body as a String
     * @throws RejectedExecutionException if this client already has {@code maxConnection} requests in flight
     * @throws UncheckedIOException if opening the connection, transmitting the request, or reading or writing the response fails
     * @throws HttpResponseException if the response status code is not 2xx
     */
    public String delete(final Object queryParameters) throws RejectedExecutionException, UncheckedIOException, HttpResponseException {
        return delete(queryParameters, String.class);
    }

    /**
     * Performs a DELETE request with query parameters and custom settings, returning the response as a String.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> params = Map.of("hardDelete", true);
     * HttpSettings settings = HttpSettings.create().header("Accept", "application/json");
     * String response = client.delete(params, settings);
     * }</pre>
     *
     * @param queryParameters Query parameters appended to the URL: a pre-encoded query
     *        {@code String}, a {@code Map}, or a bean. UTF-8 percent-encoded. May be {@code null}
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @return The response body as a String
     * @throws RejectedExecutionException if this client already has {@code maxConnection} requests in flight
     * @throws UncheckedIOException if opening the connection, transmitting the request, or reading or writing the response fails
     * @throws HttpResponseException if the response status code is not 2xx
     */
    public String delete(final Object queryParameters, final HttpSettings settings)
            throws RejectedExecutionException, UncheckedIOException, HttpResponseException {
        return delete(queryParameters, settings, String.class);
    }

    /**
     * Performs a DELETE request and deserializes the response to the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DeleteResult result = client.delete(DeleteResult.class);
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object (for deserialization)
     * @return The deserialized response object
     * @throws RejectedExecutionException if this client already has {@code maxConnection} requests in flight
     * @throws UncheckedIOException if opening the connection, transmitting the request, or reading or writing the response fails
     * @throws HttpResponseException if the response status code is not 2xx and {@code resultClass}
     *         is not {@link HttpResponse}
     */
    public <T> T delete(final Class<T> resultClass) throws RejectedExecutionException, UncheckedIOException, HttpResponseException {
        return delete(null, _settings, resultClass);
    }

    /**
     * Performs a DELETE request with custom settings and deserializes the response to the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpSettings settings = HttpSettings.create().header("Accept", "application/json");
     * String response = client.delete(settings, String.class);
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @param resultClass The class of the expected response object (for deserialization)
     * @return The deserialized response object
     * @throws RejectedExecutionException if this client already has {@code maxConnection} requests in flight
     * @throws UncheckedIOException if opening the connection, transmitting the request, or reading or writing the response fails
     * @throws HttpResponseException if the response status code is not 2xx and {@code resultClass}
     *         is not {@link HttpResponse}
     */
    public <T> T delete(final HttpSettings settings, final Class<T> resultClass)
            throws RejectedExecutionException, UncheckedIOException, HttpResponseException {
        return delete(null, settings, resultClass);
    }

    /**
     * Performs a DELETE request with query parameters and deserializes the response to the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> params = Map.of("hardDelete", true);
     * String response = client.delete(params, String.class);
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param queryParameters Query parameters appended to the URL: a pre-encoded query
     *        {@code String}, a {@code Map}, or a bean. UTF-8 percent-encoded. May be {@code null}
     * @param resultClass The class of the expected response object (for deserialization)
     * @return The deserialized response object
     * @throws RejectedExecutionException if this client already has {@code maxConnection} requests in flight
     * @throws UncheckedIOException if opening the connection, transmitting the request, or reading or writing the response fails
     * @throws HttpResponseException if the response status code is not 2xx and {@code resultClass}
     *         is not {@link HttpResponse}
     */
    public <T> T delete(final Object queryParameters, final Class<T> resultClass)
            throws RejectedExecutionException, UncheckedIOException, HttpResponseException {
        return delete(queryParameters, _settings, resultClass);
    }

    /**
     * Performs a DELETE request with all options and deserializes the response to the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> params = Map.of("hardDelete", true);
     * HttpSettings settings = HttpSettings.create().header("Accept", "application/json");
     * String response = client.delete(params, settings, String.class);
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param queryParameters Query parameters appended to the URL: a pre-encoded query
     *        {@code String}, a {@code Map}, or a bean. UTF-8 percent-encoded. May be {@code null}
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @param resultClass The class of the expected response object (for deserialization)
     * @return The deserialized response object
     * @throws RejectedExecutionException if this client already has {@code maxConnection} requests in flight
     * @throws UncheckedIOException if opening the connection, transmitting the request, or reading or writing the response fails
     * @throws HttpResponseException if the response status code is not 2xx and {@code resultClass}
     *         is not {@link HttpResponse}
     */
    public <T> T delete(final Object queryParameters, final HttpSettings settings, final Class<T> resultClass)
            throws RejectedExecutionException, UncheckedIOException, HttpResponseException {
        return execute(HttpMethod.DELETE, queryParameters, settings, resultClass);
    }

    /**
     * Performs a POST request with the specified request body and returns the response as a String.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", "Doe");
     * String response = client.post(user);
     * }</pre>
     *
     * @param request The request body (can be String, byte[], File, InputStream, Reader, or any object for JSON/XML serialization)
     * @return The response body as a String
     * @throws RejectedExecutionException if this client already has {@code maxConnection} requests in flight
     * @throws UncheckedIOException if opening the connection, transmitting the request, or reading or writing the response fails
     * @throws HttpResponseException if the response status code is not 2xx
     */
    public String post(final Object request) throws RejectedExecutionException, UncheckedIOException, HttpResponseException {
        return post(request, String.class);
    }

    /**
     * Performs a POST request and deserializes the response.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User newUser = new User("John", "Doe");
     * User createdUser = client.post(newUser, User.class);
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param request The request body
     * @param resultClass The class of the expected response object
     * @return The deserialized response object
     * @throws RejectedExecutionException if this client already has {@code maxConnection} requests in flight
     * @throws UncheckedIOException if opening the connection, transmitting the request, or reading or writing the response fails
     * @throws HttpResponseException if the response status code is not 2xx and {@code resultClass}
     *         is not {@link HttpResponse}
     */
    public <T> T post(final Object request, final Class<T> resultClass) throws RejectedExecutionException, UncheckedIOException, HttpResponseException {
        return post(request, _settings, resultClass);
    }

    /**
     * Performs a POST request with custom settings and returns the response as a String.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpSettings settings = HttpSettings.create().header("Content-Type", "application/json");
     * String requestBody = "{\"name\":\"John\"}";
     * String response = client.post(requestBody, settings);
     * }</pre>
     *
     * @param request The request body (can be String, byte[], File, InputStream, Reader, or any object for serialization)
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @return The response body as a String
     * @throws RejectedExecutionException if this client already has {@code maxConnection} requests in flight
     * @throws UncheckedIOException if opening the connection, transmitting the request, or reading or writing the response fails
     * @throws HttpResponseException if the response status code is not 2xx
     */
    public String post(final Object request, final HttpSettings settings) throws RejectedExecutionException, UncheckedIOException, HttpResponseException {
        return post(request, settings, String.class);
    }

    /**
     * Performs a POST request with custom settings and deserializes the response to the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpSettings settings = HttpSettings.create().header("Content-Type", "application/json");
     * String requestBody = "{\"name\":\"John\"}";
     * String response = client.post(requestBody, settings, String.class);
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param request The request body (can be String, byte[], File, InputStream, Reader, or any object for serialization)
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @param resultClass The class of the expected response object (for deserialization)
     * @return The deserialized response object
     * @throws RejectedExecutionException if this client already has {@code maxConnection} requests in flight
     * @throws UncheckedIOException if opening the connection, transmitting the request, or reading or writing the response fails
     * @throws HttpResponseException if the response status code is not 2xx and {@code resultClass}
     *         is not {@link HttpResponse}
     */
    public <T> T post(final Object request, final HttpSettings settings, final Class<T> resultClass)
            throws RejectedExecutionException, UncheckedIOException, HttpResponseException {
        return execute(HttpMethod.POST, request, settings, resultClass);
    }

    /**
     * Performs a PUT request with the specified request body and returns the response as a String.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User updatedUser = new User("John", "Smith");
     * String response = client.put(updatedUser);
     * }</pre>
     *
     * @param request The request body
     * @return The response body as a String
     * @throws RejectedExecutionException if this client already has {@code maxConnection} requests in flight
     * @throws UncheckedIOException if opening the connection, transmitting the request, or reading or writing the response fails
     * @throws HttpResponseException if the response status code is not 2xx
     */
    public String put(final Object request) throws RejectedExecutionException, UncheckedIOException, HttpResponseException {
        return put(request, String.class);
    }

    /**
     * Performs a PUT request and deserializes the response to the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String requestBody = "{\"name\":\"John\"}";
     * String response = client.put(requestBody, String.class);
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param request The request body (can be String, byte[], File, InputStream, Reader, or any object for serialization)
     * @param resultClass The class of the expected response object (for deserialization)
     * @return The deserialized response object
     * @throws RejectedExecutionException if this client already has {@code maxConnection} requests in flight
     * @throws UncheckedIOException if opening the connection, transmitting the request, or reading or writing the response fails
     * @throws HttpResponseException if the response status code is not 2xx and {@code resultClass}
     *         is not {@link HttpResponse}
     */
    public <T> T put(final Object request, final Class<T> resultClass) throws RejectedExecutionException, UncheckedIOException, HttpResponseException {
        return put(request, _settings, resultClass);
    }

    /**
     * Performs a PUT request with custom settings and returns the response as a String.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpSettings settings = HttpSettings.create().header("Content-Type", "application/json");
     * String requestBody = "{\"name\":\"John\"}";
     * String response = client.put(requestBody, settings);
     * }</pre>
     *
     * @param request The request body (can be String, byte[], File, InputStream, Reader, or any object for serialization)
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @return The response body as a String
     * @throws RejectedExecutionException if this client already has {@code maxConnection} requests in flight
     * @throws UncheckedIOException if opening the connection, transmitting the request, or reading or writing the response fails
     * @throws HttpResponseException if the response status code is not 2xx
     */
    public String put(final Object request, final HttpSettings settings) throws RejectedExecutionException, UncheckedIOException, HttpResponseException {
        return put(request, settings, String.class);
    }

    /**
     * Performs a PUT request with custom settings and deserializes the response to the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpSettings settings = HttpSettings.create().header("Content-Type", "application/json");
     * String requestBody = "{\"name\":\"John\"}";
     * String response = client.put(requestBody, settings, String.class);
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param request The request body (can be String, byte[], File, InputStream, Reader, or any object for serialization)
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @param resultClass The class of the expected response object (for deserialization)
     * @return The deserialized response object
     * @throws RejectedExecutionException if this client already has {@code maxConnection} requests in flight
     * @throws UncheckedIOException if opening the connection, transmitting the request, or reading or writing the response fails
     * @throws HttpResponseException if the response status code is not 2xx and {@code resultClass}
     *         is not {@link HttpResponse}
     */
    public <T> T put(final Object request, final HttpSettings settings, final Class<T> resultClass)
            throws RejectedExecutionException, UncheckedIOException, HttpResponseException {
        return execute(HttpMethod.PUT, request, settings, resultClass);
    }

    /**
     * Performs a HEAD request with this client's default settings.
     * HEAD requests are used to retrieve headers (status, metadata) without a response body.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpResponse response = client.head();
     * // headers() is keyed exactly as the server sent them and also contains a null key for the
     * // status line, so look values up defensively rather than dereferencing get(name) directly.
     * List<String> contentLength = response.headers().getOrDefault("Content-Length", List.of());
     * }</pre>
     *
     * @return The {@link HttpResponse} containing the status code and headers (the body is empty for HEAD),
     *         or {@code null} when the client-level settings mark requests as one-way
     *         ({@link HttpSettings#setOneWayRequest(boolean)})
     * @throws RejectedExecutionException if this client already has {@code maxConnection} requests in flight
     * @throws UncheckedIOException if opening the connection, transmitting the request, or reading or writing the response fails
     */
    public HttpResponse head() throws RejectedExecutionException, UncheckedIOException {
        return head(_settings);
    }

    /**
     * Performs a HEAD request with custom settings.
     * HEAD requests retrieve only headers (status, metadata) without a response body.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpSettings settings = HttpSettings.create().header("Accept", "application/json");
     * HttpResponse response = client.head(settings);
     * }</pre>
     *
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @return The {@link HttpResponse} containing the status code and headers (the body is empty for HEAD),
     *         or {@code null} when the effective settings mark the request as one-way
     *         ({@link HttpSettings#setOneWayRequest(boolean)})
     * @throws RejectedExecutionException if this client already has {@code maxConnection} requests in flight
     * @throws UncheckedIOException if opening the connection, transmitting the request, or reading or writing the response fails
     */
    public HttpResponse head(final HttpSettings settings) throws RejectedExecutionException, UncheckedIOException {
        return execute(HttpMethod.HEAD, null, settings, HttpResponse.class);
    }

    /**
     * Executes an HTTP request with the specified method and request body.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String response = client.execute(HttpMethod.POST, requestBody);
     * }</pre>
     *
     * @param httpMethod The HTTP method to use
     * @param request The payload: the request body for POST/PUT/PATCH/OPTIONS, or query
     *        parameters appended to the URL (pre-encoded {@code String}, {@code Map} or bean) for
     *        every other method. May be {@code null} for no payload
     * @return The response body as a String
     * @throws UnsupportedOperationException if {@code httpMethod} is {@link HttpMethod#PATCH} or
     *         {@link HttpMethod#CONNECT}, which {@link HttpURLConnection} cannot issue
     * @throws RejectedExecutionException if this client already has {@code maxConnection} requests in flight
     * @throws UncheckedIOException if opening the connection, transmitting the request, or reading or writing the response fails
     * @throws HttpResponseException if the response status code is not 2xx
     */
    public String execute(final HttpMethod httpMethod, final Object request)
            throws UnsupportedOperationException, RejectedExecutionException, UncheckedIOException, HttpResponseException {
        return execute(httpMethod, request, String.class);
    }

    /**
     * Executes an HTTP request and deserializes the response to the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpClient client = HttpClient.create("https://example.com/api/users");
     * User user = client.execute(HttpMethod.GET, null, User.class);
     * // returns the response deserialized into a User instance (when executed)
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param httpMethod The HTTP method to use (GET, POST, PUT, DELETE, HEAD, etc.)
     * @param request The payload: the request body for POST/PUT/PATCH/OPTIONS, or query
     *        parameters appended to the URL (pre-encoded {@code String}, {@code Map} or bean) for
     *        every other method. May be {@code null} for no payload
     * @param resultClass The class of the expected response object (for deserialization)
     * @return The deserialized response object
     * @throws UnsupportedOperationException if {@code httpMethod} is {@link HttpMethod#PATCH} or
     *         {@link HttpMethod#CONNECT}, which {@link HttpURLConnection} cannot issue
     * @throws RejectedExecutionException if this client already has {@code maxConnection} requests in flight
     * @throws UncheckedIOException if opening the connection, transmitting the request, or reading or writing the response fails
     * @throws HttpResponseException if the response status code is not 2xx and {@code resultClass}
     *         is not {@link HttpResponse}
     */
    public <T> T execute(final HttpMethod httpMethod, final Object request, final Class<T> resultClass)
            throws UnsupportedOperationException, RejectedExecutionException, UncheckedIOException, HttpResponseException {
        return execute(httpMethod, request, _settings, resultClass);
    }

    /**
     * Executes an HTTP request with custom settings and returns the response as a String.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpClient client = HttpClient.create("https://example.com/api/data");
     * HttpSettings settings = HttpSettings.create().header("Accept", "application/json");
     * String response = client.execute(HttpMethod.GET, null, settings);
     * // returns the response body as a String (when executed)
     * }</pre>
     *
     * @param httpMethod The HTTP method to use (GET, POST, PUT, DELETE, HEAD, etc.)
     * @param request The payload: the request body for POST/PUT/PATCH/OPTIONS, or query
     *        parameters appended to the URL (pre-encoded {@code String}, {@code Map} or bean) for
     *        every other method. May be {@code null} for no payload
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @return The response body as a String
     * @throws UnsupportedOperationException if {@code httpMethod} is {@link HttpMethod#PATCH} or
     *         {@link HttpMethod#CONNECT}, which {@link HttpURLConnection} cannot issue
     * @throws RejectedExecutionException if this client already has {@code maxConnection} requests in flight
     * @throws UncheckedIOException if opening the connection, transmitting the request, or reading or writing the response fails
     * @throws HttpResponseException if the response status code is not 2xx
     */
    public String execute(final HttpMethod httpMethod, final Object request, final HttpSettings settings)
            throws UnsupportedOperationException, RejectedExecutionException, UncheckedIOException, HttpResponseException {
        return execute(httpMethod, request, settings, String.class);
    }

    /**
     * Executes an HTTP request with all options and deserializes the response to the specified type.
     * This is the core method that all other request methods delegate to.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpClient client = HttpClient.create("https://example.com/api/users");
     * HttpSettings settings = HttpSettings.create().header("Authorization", "Bearer token123");
     * User created = client.execute(HttpMethod.POST, new User("John"), settings, User.class);
     * // returns the created resource deserialized into a User (when executed)
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param httpMethod The HTTP method to use (GET, POST, PUT, DELETE, HEAD, etc.)
     * @param request The payload: the request body for POST/PUT/PATCH/OPTIONS, or query
     *        parameters appended to the URL (pre-encoded {@code String}, {@code Map} or bean) for
     *        every other method. May be {@code null} for no payload
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @param resultClass The class of the expected response object (for deserialization)
     * @return The deserialized response object, or {@code null} if {@code resultClass} is {@code Void}
     *         or the effective settings mark the request as one-way
     *         ({@link HttpSettings#setOneWayRequest(boolean)}) - this applies even when
     *         {@code resultClass} is {@link HttpResponse}
     * @throws IllegalArgumentException if {@code httpMethod} is {@code null}, or the payload is routed
     *         to the URL and cannot be encoded as a query (a pre-encoded {@code String} that is not
     *         valid URI syntax, or a {@code Map} with a {@code null} key); the in-flight slot is not consumed
     * @throws UnsupportedOperationException if {@code httpMethod} is {@link HttpMethod#PATCH} or
     *         {@link HttpMethod#CONNECT}, which {@link HttpURLConnection} cannot issue
     * @throws RejectedExecutionException if this client already has {@code maxConnection} requests in flight
     * @throws UncheckedIOException if opening the connection, transmitting the request, or reading or writing the response fails
     * @throws HttpResponseException if the response status code is not 2xx and {@code resultClass}
     *         is not {@link HttpResponse}
     */
    public <T> T execute(final HttpMethod httpMethod, final Object request, final HttpSettings settings, final Class<T> resultClass)
            throws IllegalArgumentException, UnsupportedOperationException, RejectedExecutionException, UncheckedIOException, HttpResponseException {
        return execute(httpMethod, request, settings, resultClass, null, null, null, false);
    }

    /**
     * Executes an HTTP request whose payload is explicitly a request body. This disambiguates a
     * DELETE body from the query parameters accepted by the public DELETE convenience methods.
     * Package-private for {@link HttpRequest}, which tracks that distinction explicitly.
     * @throws IllegalArgumentException if the HTTP method is null or the request content encoding conflicts with its content format
     * @throws UnsupportedOperationException if the method is PATCH or CONNECT, which HttpURLConnection does not support
     * @throws RejectedExecutionException if the maximum number of concurrent in-flight requests has been reached
     * @throws UncheckedIOException if opening the connection, transmitting the request, or reading or writing the response fails
     * @throws HttpResponseException if the response status is not 2xx and the requested result type is not HttpResponse
     */
    <T> T executeRequestBody(final HttpMethod httpMethod, final Object requestBody, final HttpSettings settings, final Class<T> resultClass)
            throws IllegalArgumentException, UnsupportedOperationException, RejectedExecutionException, UncheckedIOException, HttpResponseException {
        return execute(httpMethod, requestBody, settings, resultClass, null, null, null, true);
    }

    /**
     * Executes an HTTP request and writes the response to a file.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File outputFile = new File("response.json");
     * client.execute(HttpMethod.GET, null, settings, outputFile);
     * }</pre>
     *
     * @param httpMethod The HTTP method to use
     * @param request The payload: the request body for POST/PUT/PATCH/OPTIONS, or query
     *        parameters appended to the URL (pre-encoded {@code String}, {@code Map} or bean) for
     *        every other method. May be {@code null} for no payload
     * @param settings Additional HTTP settings for this request
     * @param output The file to write the response to. It is created (and an existing file
     *        truncated) only once a successful response has been received, so a failed request
     *        leaves an existing file untouched
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws UnsupportedOperationException if {@code httpMethod} is {@link HttpMethod#PATCH} or
     *         {@link HttpMethod#CONNECT}, which {@link HttpURLConnection} cannot issue
     * @throws RejectedExecutionException if this client already has {@code maxConnection} requests in flight
     * @throws UncheckedIOException if opening the connection, transmitting the request, or reading or writing the response fails
     * @throws HttpResponseException if the response status code is not 2xx
     */
    public void execute(final HttpMethod httpMethod, final Object request, final HttpSettings settings, final File output)
            throws IllegalArgumentException, UnsupportedOperationException, RejectedExecutionException, UncheckedIOException, HttpResponseException {
        N.checkArgNotNull(output, cs.output);

        execute(httpMethod, request, settings, null, output, null, null, false);
    }

    /**
     * @throws IllegalArgumentException if output or the HTTP method is null, or the request content encoding conflicts with its content format
     * @throws UnsupportedOperationException if the method is PATCH or CONNECT, which HttpURLConnection does not support
     * @throws RejectedExecutionException if the maximum number of concurrent in-flight requests has been reached
     * @throws UncheckedIOException if opening the connection, transmitting the request, or reading or writing the response fails
     * @throws HttpResponseException if the response status is not 2xx
     */
    void executeRequestBody(final HttpMethod httpMethod, final Object requestBody, final HttpSettings settings, final File output)
            throws IllegalArgumentException, UnsupportedOperationException, RejectedExecutionException, UncheckedIOException, HttpResponseException {
        N.checkArgNotNull(output, cs.output);

        execute(httpMethod, requestBody, settings, null, output, null, null, true);
    }

    /**
     * Executes an HTTP request and writes the response to an output stream.
     * The output stream is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpClient client = HttpClient.create("https://example.com/file.zip");
     * try (OutputStream out = new FileOutputStream("download.zip")) {
     *     client.execute(HttpMethod.GET, null, null, out);
     *     // streams the response body into 'out' (when executed)
     * }
     * }</pre>
     *
     * @param httpMethod The HTTP method to use (GET, POST, PUT, DELETE, HEAD, etc.)
     * @param request The payload: the request body for POST/PUT/PATCH/OPTIONS, or query
     *        parameters appended to the URL (pre-encoded {@code String}, {@code Map} or bean) for
     *        every other method. May be {@code null} for no payload
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @param output The output stream to write the response to
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws UnsupportedOperationException if {@code httpMethod} is {@link HttpMethod#PATCH} or
     *         {@link HttpMethod#CONNECT}, which {@link HttpURLConnection} cannot issue
     * @throws RejectedExecutionException if this client already has {@code maxConnection} requests in flight
     * @throws UncheckedIOException if opening the connection, transmitting the request, or reading or writing the response fails
     * @throws HttpResponseException if the response status code is not 2xx
     */
    public void execute(final HttpMethod httpMethod, final Object request, final HttpSettings settings, final OutputStream output)
            throws IllegalArgumentException, UnsupportedOperationException, RejectedExecutionException, UncheckedIOException, HttpResponseException {
        N.checkArgNotNull(output, cs.output);

        execute(httpMethod, request, settings, null, null, output, null, false);
    }

    /**
     * @throws IllegalArgumentException if output or the HTTP method is null, or the request content encoding conflicts with its content format
     * @throws UnsupportedOperationException if the method is PATCH or CONNECT, which HttpURLConnection does not support
     * @throws RejectedExecutionException if the maximum number of concurrent in-flight requests has been reached
     * @throws UncheckedIOException if opening the connection, transmitting the request, or reading or writing the response fails
     * @throws HttpResponseException if the response status is not 2xx
     */
    void executeRequestBody(final HttpMethod httpMethod, final Object requestBody, final HttpSettings settings, final OutputStream output)
            throws IllegalArgumentException, UnsupportedOperationException, RejectedExecutionException, UncheckedIOException, HttpResponseException {
        N.checkArgNotNull(output, cs.output);

        execute(httpMethod, requestBody, settings, null, null, output, null, true);
    }

    /**
     * Executes an HTTP request and writes the response to a writer.
     * The writer is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpClient client = HttpClient.create("https://example.com/api/data");
     * try (Writer writer = new StringWriter()) {
     *     client.execute(HttpMethod.GET, null, null, writer);
     *     // writes the response body into 'writer' (when executed)
     * }
     * }</pre>
     *
     * @param httpMethod The HTTP method to use (GET, POST, PUT, DELETE, HEAD, etc.)
     * @param request The payload: the request body for POST/PUT/PATCH/OPTIONS, or query
     *        parameters appended to the URL (pre-encoded {@code String}, {@code Map} or bean) for
     *        every other method. May be {@code null} for no payload
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @param output The writer to write the response to
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws UnsupportedOperationException if {@code httpMethod} is {@link HttpMethod#PATCH} or
     *         {@link HttpMethod#CONNECT}, which {@link HttpURLConnection} cannot issue
     * @throws RejectedExecutionException if this client already has {@code maxConnection} requests in flight
     * @throws UncheckedIOException if opening the connection, transmitting the request, or reading or writing the response fails
     * @throws HttpResponseException if the response status code is not 2xx
     */
    public void execute(final HttpMethod httpMethod, final Object request, final HttpSettings settings, final Writer output)
            throws IllegalArgumentException, UnsupportedOperationException, RejectedExecutionException, UncheckedIOException, HttpResponseException {
        N.checkArgNotNull(output, cs.output);

        execute(httpMethod, request, settings, null, null, null, output, false);
    }

    /**
     * @throws IllegalArgumentException if output or the HTTP method is null, or the request content encoding conflicts with its content format
     * @throws UnsupportedOperationException if the method is PATCH or CONNECT, which HttpURLConnection does not support
     * @throws RejectedExecutionException if the maximum number of concurrent in-flight requests has been reached
     * @throws UncheckedIOException if opening the connection, transmitting the request, or reading or writing the response fails
     * @throws HttpResponseException if the response status is not 2xx
     */
    void executeRequestBody(final HttpMethod httpMethod, final Object requestBody, final HttpSettings settings, final Writer output)
            throws IllegalArgumentException, UnsupportedOperationException, RejectedExecutionException, UncheckedIOException, HttpResponseException {
        N.checkArgNotNull(output, cs.output);

        execute(httpMethod, requestBody, settings, null, null, null, output, true);
    }

    /**
     * Internal core method to execute an HTTP request with the specified parameters.
     *
     * @param <T> the response type.
     * @param httpMethod the HTTP method to use.
     * @param request the query parameters or the request body, depending on {@code httpMethod}.
     * @param settings the HTTP settings.
     * @param resultClass the expected response type.
     * @param outputFile the file to write the response to, or {@code null}. It is opened only after a
     *        successful response, so a failed request never truncates it.
     * @param outputStream the output stream to write response to, or {@code null}.
     * @param outputWriter the writer to write response to, or {@code null}.
     * @param requestIsBody whether {@code request} is a request body rather than query parameters.
     * @return the response parsed as the specified result class, or {@code null} for one-way
     *         requests or when the response is written to an output target.
     * @throws IllegalArgumentException if the HTTP method is null, query parameters cannot be encoded in the URL, or request content encoding conflicts with its content format
     * @throws UnsupportedOperationException if the method is PATCH or CONNECT, which HttpURLConnection does not support
     * @throws RejectedExecutionException if the maximum number of concurrent in-flight requests has been reached
     * @throws UncheckedIOException if opening the connection, transmitting the request, or reading or writing the response fails
     * @throws HttpResponseException if the response status is not 2xx and the requested result type is not HttpResponse
     */
    private <T> T execute(final HttpMethod httpMethod, final Object request, final HttpSettings settings, final Class<T> resultClass, final File outputFile,
            final OutputStream outputStream, final Writer outputWriter, final boolean requestIsBody)
            throws IllegalArgumentException, UnsupportedOperationException, RejectedExecutionException, UncheckedIOException, HttpResponseException {
        N.checkArgNotNull(httpMethod, cs.httpMethod);

        // The payload is routed by HTTP method, not by argument type: body methods take it as a
        // request body, every other method appends it to the URL as query parameters. Previously
        // anything that was neither (HEAD/TRACE/CONNECT) was silently discarded.
        final boolean payloadIsBody = requestIsBody || isRequestBodyMethod(httpMethod);
        final Object queryParameters = payloadIsBody ? null : request;
        final boolean doOutput = request != null && payloadIsBody;

        final String settingsContentType = getContentType(settings);
        final Charset requestCharset = HttpUtil.getCharset(settingsContentType);
        final ContentFormat requestContentFormat = getContentFormat(settings);
        final String requestContentEncoding = doOutput ? getContentEncoding(settings) : null;

        if (doOutput) {
            // Validated before a connection is opened, so a contradictory configuration fails
            // without consuming an in-flight slot or touching the network.
            checkRequestContentEncoding(requestContentFormat, settingsContentType, requestContentEncoding);
        }

        final HttpURLConnection connection = openConnection(httpMethod, queryParameters, settings, doOutput, true);
        final long sentRequestAtMillis = System.currentTimeMillis();
        InputStream is = null;
        OutputStream os = null;

        try { //NOSONAR
            if (doOutput && connection.getDoOutput()) {
                final Type<Object> type = Type.of(request.getClass());
                final boolean isRawText = request instanceof String || type.isReader();
                final boolean isRawBinary = request instanceof File || request.getClass().equals(byte[].class) || type.isInputStream();

                // The Content-Type on the wire must describe the bytes that are actually written.
                // With nothing declaring one, HttpURLConnection supplies its legacy
                // "application/x-www-form-urlencoded" default while the body is serialized as JSON.
                final String requestContentType = Strings.isEmpty(settingsContentType)
                        ? defaultRequestContentType(requestContentFormat, isRawText, isRawBinary, requestCharset)
                        : settingsContentType;

                os = HttpUtil.getOutputStream(connection, requestContentFormat, requestContentType, requestContentEncoding);

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
                        } else if (requestContentFormat == ContentFormat.FORM_URL_ENCODED) {
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

            if (!HttpUtil.isSuccessfulResponseCode(statusCode) && (resultClass == null || !resultClass.equals(HttpResponse.class))) {
                String errorBody = Strings.EMPTY;
                try {
                    if (HttpUtil.hasResponseBody(httpMethod.name(), statusCode)) {
                        is = connection.getErrorStream();
                        if (is == null) {
                            is = connection.getInputStream();
                        }
                        // Keep the raw stream in 'is' until wrapping succeeds, so the outer finally
                        // still closes it if a malformed compression header prevents construction.
                        is = HttpUtil.wrapInputStream(is, respContentFormat);
                        errorBody = readBoundedErrorBody(is, respCharset);
                    }
                } catch (final IOException | RuntimeException e) {
                    // Once a non-success status is known, body acquisition/decoding failures must
                    // not replace its diagnostics. Successful and raw-response reads stay strict.
                }
                throw new HttpResponseException(connection.getURL().toString(), statusCode, connection.getResponseMessage(), respHeaders, errorBody);
            }

            // HEAD and these status codes have no response body, even when representation headers
            // describe compressed content. Opening a decompressor would fail on the empty stream.
            is = HttpUtil.hasResponseBody(httpMethod.name(), statusCode) ? HttpUtil.getInputStream(connection, respContentFormat) : N.emptyInputStream();

            if (isOneWayRequest(settings, resultClass, outputFile, outputStream, outputWriter)) {
                return null;
            } else if (outputFile != null) {
                // Created here rather than before the request: opening it up front truncated an
                // existing file even when the request then failed with a 4xx/5xx, silently
                // destroying the caller's data.
                try (OutputStream fileOutput = IOUtil.newFileOutputStream(outputFile)) {
                    IOUtil.write(is, fileOutput, true);
                }

                return null;
            } else if (outputStream != null) {
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
                    return (T) new HttpResponse(connection.getURL().toString(), sentRequestAtMillis, System.currentTimeMillis(), statusCode,
                            connection.getResponseMessage(), respHeaders, IOUtil.readAllBytes(is), respContentFormat, respCharset);
                } else {
                    if (resultClass.equals(String.class)) {
                        return (T) IOUtil.readAllToString(is, respCharset);
                    } else if (byte[].class.equals(resultClass)) {
                        return (T) IOUtil.readAllBytes(is);
                    } else {
                        if (respContentFormat == ContentFormat.KRYO && HttpUtil.kryoParser != null) {
                            return HttpUtil.kryoParser.deserialize(is, resultClass);
                        } else if (respContentFormat == ContentFormat.FORM_URL_ENCODED) {
                            return URLEncodedUtil.decode(IOUtil.readAllToString(is, respCharset), respCharset, resultClass);
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
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            close(os, is, connection);
        }
    }

    /**
     * Reads at most {@link HttpUtil#MAX_ERROR_BODY_SIZE} bytes of an error response body.
     *
     * @param is the (already decompressed) error body stream
     * @param charset the charset to decode the captured bytes with
     * @return the decoded prefix of the error body; never {@code null}, empty if it cannot be read.
     *         The cut is made on a byte boundary, so a multi-byte character straddling the limit is
     *         decoded as a replacement character
     */
    private static String readBoundedErrorBody(final InputStream is, final Charset charset) {
        try {
            return new String(is.readNBytes(HttpUtil.MAX_ERROR_BODY_SIZE), charset);
        } catch (final IOException | RuntimeException e) {
            // The status code is what matters here; a failure to read the error body must never
            // replace the HttpResponseException that is about to be thrown.
            return Strings.EMPTY;
        }
    }

    /**
     * Resolves the {@code Content-Type} to send when neither the client-level nor the per-request
     * settings declare one, so that it matches how the body is actually written.
     *
     * @param contentFormat the active content format, or {@code null} when none is configured
     * @param isRawText {@code true} for a {@code String}/{@link Reader} payload written verbatim
     * @param isRawBinary {@code true} for a {@code byte[]}/{@link File}/{@link InputStream} payload
     * @param charset the charset the request body is written with
     * @return a non-empty media type
     */
    private static String defaultRequestContentType(final ContentFormat contentFormat, final boolean isRawText, final boolean isRawBinary,
            final Charset charset) {
        final String derived = HttpUtil.getContentType(contentFormat);

        if (Strings.isNotEmpty(derived)) {
            return derived;
        }

        if (contentFormat == ContentFormat.KRYO) {
            // ContentFormat.KRYO carries its identity in Content-Encoding and declares no media type
            // of its own, but the payload is still application/kryo on the wire.
            return HttpHeaders.Values.APPLICATION_KRYO;
        }

        if (isRawText) {
            return HttpHeaders.Values.TEXT_PLAIN + "; charset=" + charset.name();
        }

        if (isRawBinary) {
            return HttpHeaders.Values.APPLICATION_OCTET_STREAM;
        }

        // Compression-only formats (and no format at all) serialize through the parser for
        // HttpUtil.DEFAULT_CONTENT_FORMAT, so the media type must be that format's.
        return HttpUtil.getContentType(HttpUtil.DEFAULT_CONTENT_FORMAT);
    }

    /**
     * Rejects a {@code Content-Encoding} that contradicts the compression the active
     * {@link ContentFormat} actually applies, instead of shipping a body whose declared encoding
     * does not match its bytes. An encoding this client does not implement (for example
     * {@code identity} or {@code deflate}) is passed through untouched and is never rejected.
     *
     * @param contentFormat the active content format, which decides the compression applied
     * @param contentType the resolved request content type, used to interpret the declared encoding
     * @param declaredEncoding the {@code Content-Encoding} resolved from the settings, may be empty
     * @throws IllegalArgumentException if the declared encoding names a supported compression that
     *         differs from the one the content format applies.
     */
    private static void checkRequestContentEncoding(final ContentFormat contentFormat, final String contentType, final String declaredEncoding)
            throws IllegalArgumentException {
        if (Strings.isEmpty(declaredEncoding)) {
            return;
        }

        final String appliedEncoding = HttpUtil.getContentEncoding(contentFormat);
        final String declaredCompression = HttpUtil.getContentEncoding(HttpUtil.getContentFormat(contentType, declaredEncoding));

        if (!declaredCompression.equalsIgnoreCase(appliedEncoding)) {
            throw new IllegalArgumentException("Content-Encoding: " + declaredEncoding + " conflicts with content format " + contentFormat + ", which applies "
                    + (Strings.isEmpty(appliedEncoding) ? "no compression" : appliedEncoding)
                    + ". Use a content format whose encoding matches, or remove the Content-Encoding header;"
                    + " the request body must not be pre-compressed by the caller.");
        }
    }

    /**
     * Determines whether the request should be treated as one-way and skip response body processing.
     *
     * @param settings the per-request settings, or {@code null} to use client defaults
     * @param resultClass the expected result type
     * @param outputFile the target file for response forwarding
     * @param outputStream the target output stream for response forwarding
     * @param outputWriter the target writer for response forwarding
     * @return {@code true} if no response body should be read into a return value
     */
    boolean isOneWayRequest(final HttpSettings settings, final Class<?> resultClass, final File outputFile, final OutputStream outputStream,
            final Writer outputWriter) {
        return (resultClass == null || Void.class.equals(resultClass) || resolveFlag(settings == null ? null : settings.isOneWayRequestOrNull(),
                _settings.isOneWayRequestOrNull(), HttpSettings.DEFAULT_ONE_WAY_REQUEST)) && outputFile == null && outputStream == null && outputWriter == null;
    }

    /**
     * Resolves the effective content format, preferring per-request settings over client defaults.
     *
     * @param settings the per-request settings, or {@code null} to use client defaults
     * @return the resolved content format
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
     * Resolves the effective content type, preferring per-request settings over client defaults.
     *
     * @param settings the per-request settings, or {@code null} to use client defaults
     * @return the resolved content type
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
     * Resolves the charset from the effective content type. An unrelated per-request header must not
     * hide a charset configured on the client defaults; {@link #setHttpProperties(HttpURLConnection,
     * HttpSettings)} applies both sets of headers, with the per-request content type overriding only
     * when it is actually present.
     *
     * <p>The request path resolves the content type once and derives the charset from it directly;
     * this accessor exists so the layering rule can be asserted on its own.</p>
     *
     * @param settings the per-request settings, or {@code null} to use client defaults
     * @return the charset declared by the effective content type, or UTF-8 when none is declared
     */
    Charset getRequestCharset(final HttpSettings settings) {
        return HttpUtil.getCharset(getContentType(settings));
    }

    /**
     * Resolves the effective content encoding, preferring per-request settings over client defaults.
     *
     * <p>When the per-request settings supply the content format (explicitly or derived from their
     * own {@code Content-Type}), that format is authoritative for the compression as well: a
     * non-compressing request format must be able to opt out of a compressing client-level default.
     * Only the client's <i>explicit</i> {@code Content-Encoding} header is still imported in that
     * case - {@link #setHttpProperties(HttpURLConnection, HttpSettings)} copies it onto the
     * connection regardless, so it has to be validated against the request format rather than
     * silently sent over an uncompressed body.</p>
     *
     * @param settings the per-request settings, or {@code null} to use client defaults
     * @return the resolved content encoding
     */
    private String getContentEncoding(final HttpSettings settings) {
        String contentEncoding = null;

        if (settings != null) {
            contentEncoding = settings.getContentEncoding();

            if (Strings.isEmpty(contentEncoding)) {
                final ContentFormat requestContentFormat = settings.getContentFormat();

                if (requestContentFormat != null && requestContentFormat != ContentFormat.NONE) {
                    // The request chose the format, and with it the compression. Do not pull in the
                    // client's format-derived encoding (e.g. gzip from JSON_GZIP); a literal client
                    // header still goes on the wire via setHttpProperties, so keep it for the check.
                    return HttpUtil.getContentEncoding(_settings.headersOrNull());
                }
            }
        }

        if (Strings.isEmpty(contentEncoding)) {
            contentEncoding = _settings.getContentEncoding();
        }

        return contentEncoding;
    }

    /**
     * Checks whether the generic execution API treats its request argument as a request body.
     * Every other method receives it as query parameters instead.
     *
     * @param httpMethod the HTTP method to evaluate
     * @return {@code true} for POST/PUT/PATCH/OPTIONS; otherwise {@code false}
     */
    private static boolean isRequestBodyMethod(final HttpMethod httpMethod) {
        return httpMethod == HttpMethod.POST || httpMethod == HttpMethod.PUT || httpMethod == HttpMethod.PATCH || httpMethod == HttpMethod.OPTIONS;
    }

    /**
     * Rejects the HTTP methods {@link HttpURLConnection} cannot issue at all. Without this guard they reach
     * {@link HttpURLConnection#setRequestMethod(String)}, whose {@code java.net.ProtocolException} is wrapped
     * into an {@link UncheckedIOException} and reads like an I/O failure rather than the documented
     * limitation it is. {@link HttpRequest} applies the same guard - with the same message - to PATCH at its
     * own entry points, so both routes report the same exception; CONNECT is caught only here, which is why
     * this check sits on the shared choke point every request funnels through rather than in the builders.
     *
     * @param httpMethod the HTTP method to evaluate
     * @throws UnsupportedOperationException if {@code httpMethod} is {@link HttpMethod#PATCH} or
     *         {@link HttpMethod#CONNECT}
     */
    private static void checkSupportedMethod(final HttpMethod httpMethod) throws UnsupportedOperationException {
        if (httpMethod == HttpMethod.PATCH || httpMethod == HttpMethod.CONNECT) {
            throw new UnsupportedOperationException(
                    "HttpMethod." + httpMethod.name() + " is not supported by the underlying java.net.HttpURLConnection; see HttpMethod#PATCH for workarounds");
        }
    }

    /**
     * Opens a new HTTP connection with the specified method and settings.
     * This method is primarily for advanced use cases where direct control over the connection is needed.
     * The returned connection is caller-managed and does not count against this client's in-flight
     * request limit.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpURLConnection conn = client.openConnection(HttpMethod.GET, settings, false);
     * // Manually configure and use the connection
     * conn.connect();
     * }</pre>
     *
     * @param httpMethod The HTTP method to use
     * @param settings Additional HTTP settings for the connection
     * @param doOutput Whether the connection will send a request body
     * @return A configured HttpURLConnection ready for use
     * @throws IllegalArgumentException if {@code httpMethod} is {@code null}.
     * @throws UnsupportedOperationException if {@code httpMethod} is {@link HttpMethod#PATCH} or
     *         {@link HttpMethod#CONNECT}, which {@link HttpURLConnection} cannot issue
     * @throws UncheckedIOException if creating the connection or configuring its request method fails
     */
    public HttpURLConnection openConnection(final HttpMethod httpMethod, final HttpSettings settings, final boolean doOutput)
            throws IllegalArgumentException, UnsupportedOperationException, UncheckedIOException {
        return openConnection(httpMethod, null, settings, doOutput, false);
    }

    /**
     * Opens a new HTTP connection with query parameters and the specified settings.
     * This method is primarily for advanced use cases where direct control over the connection is needed.
     * The returned connection is caller-managed and does not count against this client's in-flight
     * request limit.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> params = Map.of("id", 123);
     * HttpURLConnection conn = client.openConnection(HttpMethod.GET, params, settings, false);
     * }</pre>
     *
     * @param httpMethod The HTTP method to use
     * @param queryParameters Query parameters appended to this client's URL (a pre-encoded query
     *        {@code String}, a {@code Map} or a bean), or {@code null} for none. They are applied for
     *        every HTTP method
     * @param settings Additional HTTP settings for the connection
     * @param doOutput Whether the connection will send a request body
     * @return A configured HttpURLConnection ready for use
     * @throws IllegalArgumentException if {@code httpMethod} is {@code null}, or {@code queryParameters}
     *         cannot be encoded as a query (a pre-encoded {@code String} that is not valid URI
     *         syntax, or a {@code Map} with a {@code null} key). The same applies to every
     *         {@code get}/{@code delete}/{@code execute} overload that takes query parameters.
     * @throws UnsupportedOperationException if {@code httpMethod} is {@link HttpMethod#PATCH} or
     *         {@link HttpMethod#CONNECT}, which {@link HttpURLConnection} cannot issue
     * @throws UncheckedIOException if creating the connection or configuring its request method fails
     */
    public HttpURLConnection openConnection(final HttpMethod httpMethod, final Object queryParameters, final HttpSettings settings, final boolean doOutput)
            throws IllegalArgumentException, UnsupportedOperationException, UncheckedIOException {
        return openConnection(httpMethod, queryParameters, settings, doOutput, false);
    }

    /**
     * Internal connection factory with optional in-flight request tracking.
     *
     * @param httpMethod the HTTP method to use
     * @param queryParameters query parameters to append to the URL, or {@code null} for none
     * @param settings the per-request settings, or {@code null} to use client defaults
     * @param doOutput whether request-body output should be enabled
     * @param trackConnectionLimit whether to enforce/decrement the in-flight request counter
     * @return a configured HTTP connection
     * @throws IllegalArgumentException if {@code httpMethod} is {@code null}.
     * @throws UnsupportedOperationException if {@code httpMethod} is {@link HttpMethod#PATCH} or
     *         {@link HttpMethod#CONNECT}, which {@link HttpURLConnection} cannot issue
     * @throws RejectedExecutionException if {@code trackConnectionLimit} is {@code true} and the
     *         in-flight request limit has been reached
     * @throws UncheckedIOException if creating the connection or configuring its request method fails
     */
    private HttpURLConnection openConnection(final HttpMethod httpMethod, final Object queryParameters, final HttpSettings settings, final boolean doOutput,
            final boolean trackConnectionLimit)
            throws IllegalArgumentException, UnsupportedOperationException, RejectedExecutionException, UncheckedIOException {
        N.checkArgNotNull(httpMethod, cs.httpMethod);
        // Validated before the in-flight slot is taken, so a method this client can never issue costs
        // neither a slot nor a socket. Every execute/asyncExecute route funnels through here.
        checkSupportedMethod(httpMethod);

        if (trackConnectionLimit && _activeConnectionCounter.incrementAndGet() > _maxConnection) {
            _activeConnectionCounter.decrementAndGet();
            throw new RejectedExecutionException("Cannot execute request: " + _maxConnection + " concurrent in-flight requests already active for: " + _url);
        }

        HttpURLConnection connection = null;
        boolean success = false;

        try {
            URL netURL = _netURL;

            if (queryParameters != null) {
                // Explicit UTF-8 (like the form-body path): the 2-arg overload uses the
                // PLATFORM default charset, mis-encoding non-ASCII query values on non-UTF-8 JVMs.
                netURL = URI.create(URLEncodedUtil.encode(_url, queryParameters, HttpUtil.DEFAULT_CHARSET)).toURL();
            }

            // Per-request settings are layered on top of the client's defaults (the same
            // rule used for headers, timeouts and content metadata). A request settings
            // object created only to add a header therefore must not silently disable the
            // client-level proxy. Proxy.NO_PROXY remains available for an explicit bypass.
            Proxy proxy = settings == null ? null : settings.getProxy();

            if (proxy == null) {
                proxy = _settings.getProxy();
            }

            if (proxy == null) {
                connection = (HttpURLConnection) netURL.openConnection();
            } else {
                connection = (HttpURLConnection) netURL.openConnection(proxy);
            }

            if (connection instanceof HttpsURLConnection) {
                // A null request-level factory means "not specified", not "discard the client
                // default". This is particularly important for callers that pass an otherwise
                // unrelated per-request setting such as a tracing header.
                SSLSocketFactory ssf = settings == null ? null : settings.getSSLSocketFactory();

                if (ssf == null) {
                    ssf = _settings.getSSLSocketFactory();
                }

                if (ssf != null) {
                    ((HttpsURLConnection) connection).setSSLSocketFactory(ssf);
                }
            }

            connection.setConnectTimeout(
                    resolveTimeout(settings == null ? 0 : settings.getConnectTimeout(), _settings.getConnectTimeout(), _connectTimeoutInMillis));
            connection.setReadTimeout(resolveTimeout(settings == null ? 0 : settings.getReadTimeout(), _settings.getReadTimeout(), _readTimeoutInMillis));

            // The connection flags are layered exactly like the proxy/SSL/content settings above.
            // They used to be read only from whichever of the two settings objects was reachable on
            // the current branch, so a per-request HttpSettings created just to add a header reset
            // them to their defaults - and a null one ignored the client-level values entirely.
            connection.setDoInput(resolveFlag(settings == null ? null : settings.doInputOrNull(), _settings.doInputOrNull(), HttpSettings.DEFAULT_DO_INPUT));
            connection.setDoOutput(
                    doOutput && resolveFlag(settings == null ? null : settings.doOutputOrNull(), _settings.doOutputOrNull(), HttpSettings.DEFAULT_DO_OUTPUT));
            connection.setUseCaches(
                    resolveFlag(settings == null ? null : settings.useCachesOrNull(), _settings.useCachesOrNull(), HttpSettings.DEFAULT_USE_CACHES));

            setHttpProperties(connection, _settings);

            if (settings != null && settings != _settings) {
                setHttpProperties(connection, settings);
            }

            connection.setRequestMethod(httpMethod.name());

            success = true;
            return connection;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (trackConnectionLimit && !success) {
                _activeConnectionCounter.decrementAndGet();
            }
        }
    }

    /**
     * Resolves a tri-state connection flag: the per-request value wins, then the client-level value,
     * then the built-in default.
     *
     * @param requestValue the per-request value, or {@code null} if not set
     * @param clientValue the client-level value, or {@code null} if not set
     * @param defaultValue the value to use when neither is set
     * @return the effective flag value
     */
    private static boolean resolveFlag(final Boolean requestValue, final Boolean clientValue, final boolean defaultValue) {
        if (requestValue != null) {
            return requestValue;
        }

        return clientValue == null ? defaultValue : clientValue;
    }

    private static int resolveTimeout(final long requestValue, final long clientValue, final long constructorValue) {
        // Zero means unspecified at either settings layer. Resolve before narrowing so even
        // very large inherited values retain the same saturation rule as explicit overrides.
        final long value = requestValue > 0 ? requestValue : clientValue > 0 ? clientValue : constructorValue;
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    /**
     * Applies the HTTP header properties from the given settings to the specified connection.
     * Iterates over all header names in the settings and sets them as request properties on the
     * connection. Content-Encoding headers are skipped when the connection is not configured for output,
     * to avoid errors on the server side when no body is being sent.
     *
     * <p>Each value is rendered with {@link HttpHeaders#valueOf(String, Object)}, the field-aware form, so a
     * {@code Collection} on a field with its own list grammar - {@code Cookie}, whose cookie-pairs RFC 6265
     * &sect;5.4 separates with {@code "; "} - is not comma-joined into a malformed header line.</p>
     *
     * @param connection the HTTP URL connection to configure
     * @param settings the HTTP settings whose headers are to be applied
     * @throws IllegalStateException if a request header is applied after the connection has already connected
     */
    void setHttpProperties(final HttpURLConnection connection, final HttpSettings settings) throws IllegalStateException {
        final HttpHeaders headers = settings.headers();

        if (headers != null) {
            Object headerValue = null;

            for (final String headerName : headers.headerNames()) {
                // lazy set content-encoding
                // because if content-encoding(lz4/snappy/kryo...) is set but no parameter/result write to OutputStream,
                // error may happen when read the input stream in sever side.

                if (Names.CONTENT_ENCODING.equalsIgnoreCase(headerName) && !connection.getDoOutput()) {
                    continue;
                }

                headerValue = headers.get(headerName);

                connection.setRequestProperty(headerName, HttpHeaders.valueOf(headerName, headerValue));
            }
        }
    }

    /**
     * Closes request/response streams and releases this request's slot in the in-flight counter.
     *
     * @param os the request output stream to close
     * @param is the response input stream to close
     * @param connection the connection associated with the completed request
     */
    void close(final OutputStream os, final InputStream is, final HttpURLConnection connection) { //NOSONAR
        try {
            IOUtil.closeQuietly(os);
            IOUtil.closeQuietly(is);
        } finally {
            if (connection != null) {
                _activeConnectionCounter.decrementAndGet();
            }
        }

        // connection.disconnect();
    }

    /**
     * Performs an asynchronous GET request and returns the response as a String.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * client.asyncGet()
     *     .thenRunAsync((response, exception) -> {
     *         if (exception != null) {
     *             exception.printStackTrace();
     *         } else {
     *             System.out.println("Response: " + response);
     *         }
     *     });
     * }</pre>
     *
     * @return A ContinuableFuture that will complete with the response body
     */
    public ContinuableFuture<String> asyncGet() {
        return asyncGet(String.class);
    }

    /**
     * Performs an asynchronous GET request with custom settings.
     * Returns the response as a String asynchronously.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpSettings settings = HttpSettings.create()
     *     .header("Authorization", "Bearer token123");
     * client.asyncGet(settings)
     *     .thenRunAsync(response -> System.out.println("Response: " + response));
     * }</pre>
     *
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @return A ContinuableFuture that will complete with the response body as a String
     */
    public ContinuableFuture<String> asyncGet(final HttpSettings settings) {
        return asyncGet(settings, String.class);
    }

    /**
     * Performs an asynchronous GET request with query parameters.
     * Returns the response as a String asynchronously.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> params = Map.of("id", 123, "name", "John");
     * client.asyncGet(params)
     *     .thenRunAsync(response -> System.out.println("Response: " + response));
     * }</pre>
     *
     * @param queryParameters Query parameters appended to the URL: a pre-encoded query
     *        {@code String}, a {@code Map}, or a bean. UTF-8 percent-encoded. May be {@code null}
     * @return A ContinuableFuture that will complete with the response body as a String
     */
    public ContinuableFuture<String> asyncGet(final Object queryParameters) {
        return asyncGet(queryParameters, String.class);
    }

    /**
     * Performs an asynchronous GET request with query parameters and custom settings.
     * Returns the response as a String asynchronously.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> params = Map.of("page", 1, "size", 10);
     * HttpSettings settings = HttpSettings.create()
     *     .header("Authorization", "Bearer token123");
     * client.asyncGet(params, settings)
     *     .thenRunAsync(response -> System.out.println("Response: " + response));
     * }</pre>
     *
     * @param queryParameters Query parameters appended to the URL: a pre-encoded query
     *        {@code String}, a {@code Map}, or a bean. UTF-8 percent-encoded. May be {@code null}
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @return A ContinuableFuture that will complete with the response body as a String
     */
    public ContinuableFuture<String> asyncGet(final Object queryParameters, final HttpSettings settings) {
        return asyncGet(queryParameters, settings, String.class);
    }

    /**
     * Performs an asynchronous GET request and deserializes the response to the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * client.asyncGet(User.class)
     *     .thenRunAsync(user -> System.out.println("User: " + user.getName()));
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object (for deserialization)
     * @return A ContinuableFuture that will complete with the deserialized response object
     */
    public <T> ContinuableFuture<T> asyncGet(final Class<T> resultClass) {
        return asyncGet(null, _settings, resultClass);
    }

    /**
     * Performs an asynchronous GET request with custom settings and deserializes the response.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpSettings settings = HttpSettings.create()
     *     .setContentFormat(ContentFormat.JSON)
     *     .header("Authorization", "Bearer token123");
     * client.asyncGet(settings, User.class)
     *     .thenRunAsync(user -> System.out.println("User: " + user.getName()));
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @param resultClass The class of the expected response object (for deserialization)
     * @return A ContinuableFuture that will complete with the deserialized response object
     */
    public <T> ContinuableFuture<T> asyncGet(final HttpSettings settings, final Class<T> resultClass) {
        return asyncGet(null, settings, resultClass);
    }

    /**
     * Performs an asynchronous GET request with query parameters and deserializes the response.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> params = Map.of("userId", 123);
     * client.asyncGet(params, User.class)
     *     .thenRunAsync(user -> System.out.println("User: " + user.getName()));
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param queryParameters Query parameters appended to the URL: a pre-encoded query
     *        {@code String}, a {@code Map}, or a bean. UTF-8 percent-encoded. May be {@code null}
     * @param resultClass The class of the expected response object (for deserialization)
     * @return A ContinuableFuture that will complete with the deserialized response object
     */
    public <T> ContinuableFuture<T> asyncGet(final Object queryParameters, final Class<T> resultClass) {
        return asyncGet(queryParameters, _settings, resultClass);
    }

    /**
     * Performs an asynchronous GET request with all options and deserializes the response.
     * This is the most comprehensive GET method allowing full control over query parameters,
     * settings, and response deserialization.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> params = Map.of("page", 1, "size", 20);
     * HttpSettings settings = HttpSettings.create()
     *     .header("Authorization", "Bearer token123")
     *     .setConnectTimeout(10000);
     * client.asyncGet(params, settings, UserList.class)
     *     .thenRunAsync(users -> users.forEach(u -> System.out.println(u.getName())));
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param queryParameters Query parameters appended to the URL: a pre-encoded query
     *        {@code String}, a {@code Map}, or a bean. UTF-8 percent-encoded. May be {@code null}
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @param resultClass The class of the expected response object (for deserialization)
     * @return A ContinuableFuture that will complete with the deserialized response
     */
    public <T> ContinuableFuture<T> asyncGet(final Object queryParameters, final HttpSettings settings, final Class<T> resultClass) {
        return asyncExecute(HttpMethod.GET, queryParameters, settings, resultClass);
    }

    /**
     * Performs an asynchronous DELETE request and returns the response as a String.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * client.asyncDelete()
     *     .thenRunAsync(response -> System.out.println("Deleted: " + response));
     * }</pre>
     *
     * @return A ContinuableFuture that will complete with the response body as a String
     */
    public ContinuableFuture<String> asyncDelete() {
        return asyncDelete(String.class);
    }

    /**
     * Performs an asynchronous DELETE request with custom settings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpSettings settings = HttpSettings.create()
     *     .header("Authorization", "Bearer token123");
     * client.asyncDelete(settings)
     *     .thenRunAsync(response -> System.out.println("Deleted: " + response));
     * }</pre>
     *
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @return A ContinuableFuture that will complete with the response body as a String
     */
    public ContinuableFuture<String> asyncDelete(final HttpSettings settings) {
        return asyncDelete(settings, String.class);
    }

    /**
     * Performs an asynchronous DELETE request with query parameters.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> params = Map.of("id", 123);
     * client.asyncDelete(params)
     *     .thenRunAsync(response -> System.out.println("Deleted: " + response));
     * }</pre>
     *
     * @param queryParameters Query parameters appended to the URL: a pre-encoded query
     *        {@code String}, a {@code Map}, or a bean. UTF-8 percent-encoded. May be {@code null}
     * @return A ContinuableFuture that will complete with the response body as a String
     */
    public ContinuableFuture<String> asyncDelete(final Object queryParameters) {
        return asyncDelete(queryParameters, String.class);
    }

    /**
     * Performs an asynchronous DELETE request with query parameters and custom settings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpClient client = HttpClient.create("https://example.com/api/users");
     * Map<String, Object> params = Map.of("id", 123);
     * HttpSettings settings = HttpSettings.create().header("Authorization", "Bearer token123");
     * ContinuableFuture<String> future = client.asyncDelete(params, settings);
     * // future completes with the response body as a String (when executed)
     * }</pre>
     *
     * @param queryParameters Query parameters appended to the URL: a pre-encoded query
     *        {@code String}, a {@code Map}, or a bean. UTF-8 percent-encoded. May be {@code null}
     * @param settings Additional HTTP settings for this request
     * @return A ContinuableFuture that will complete with the response body as a String
     */
    public ContinuableFuture<String> asyncDelete(final Object queryParameters, final HttpSettings settings) {
        return asyncDelete(queryParameters, settings, String.class);
    }

    /**
     * Performs an asynchronous DELETE request and deserializes the response to the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpClient client = HttpClient.create("https://example.com/api/users/123");
     * ContinuableFuture<User> future = client.asyncDelete(User.class);
     * // future completes with the response deserialized into a User (when executed)
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object (for deserialization)
     * @return A ContinuableFuture that will complete with the deserialized response object
     */
    public <T> ContinuableFuture<T> asyncDelete(final Class<T> resultClass) {
        return asyncDelete(null, _settings, resultClass);
    }

    /**
     * Performs an asynchronous DELETE request with custom settings and deserializes the response to the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpClient client = HttpClient.create("https://example.com/api/users/123");
     * HttpSettings settings = HttpSettings.create().header("Authorization", "Bearer token123");
     * ContinuableFuture<User> future = client.asyncDelete(settings, User.class);
     * // future completes with the response deserialized into a User (when executed)
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @param resultClass The class of the expected response object (for deserialization)
     * @return A ContinuableFuture that will complete with the deserialized response object
     */
    public <T> ContinuableFuture<T> asyncDelete(final HttpSettings settings, final Class<T> resultClass) {
        return asyncDelete(null, settings, resultClass);
    }

    /**
     * Performs an asynchronous DELETE request with query parameters and deserializes the response to the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpClient client = HttpClient.create("https://example.com/api/users");
     * Map<String, Object> params = Map.of("id", 123);
     * ContinuableFuture<User> future = client.asyncDelete(params, User.class);
     * // future completes with the response deserialized into a User (when executed)
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param queryParameters Query parameters appended to the URL: a pre-encoded query
     *        {@code String}, a {@code Map}, or a bean. UTF-8 percent-encoded. May be {@code null}
     * @param resultClass The class of the expected response object (for deserialization)
     * @return A ContinuableFuture that will complete with the deserialized response object
     */
    public <T> ContinuableFuture<T> asyncDelete(final Object queryParameters, final Class<T> resultClass) {
        return asyncDelete(queryParameters, _settings, resultClass);
    }

    /**
     * Performs an asynchronous DELETE request with all options and deserializes the response to the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpClient client = HttpClient.create("https://example.com/api/users");
     * Map<String, Object> params = Map.of("id", 123);
     * HttpSettings settings = HttpSettings.create().header("Authorization", "Bearer token123");
     * ContinuableFuture<User> future = client.asyncDelete(params, settings, User.class);
     * // future completes with the response deserialized into a User (when executed)
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param queryParameters Query parameters appended to the URL: a pre-encoded query
     *        {@code String}, a {@code Map}, or a bean. UTF-8 percent-encoded. May be {@code null}
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @param resultClass The class of the expected response object (for deserialization)
     * @return A ContinuableFuture that will complete with the deserialized response object
     */
    public <T> ContinuableFuture<T> asyncDelete(final Object queryParameters, final HttpSettings settings, final Class<T> resultClass) {
        return asyncExecute(HttpMethod.DELETE, queryParameters, settings, resultClass);
    }

    /**
     * Performs an asynchronous POST request with the specified request body.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User newUser = new User("John", "Doe");
     * client.asyncPost(newUser)
     *     .thenRunAsync(response -> System.out.println("Created: " + response));
     * }</pre>
     *
     * @param request The request body (can be String, byte[], File, InputStream, Reader, or any object for serialization)
     * @return A ContinuableFuture that will complete with the response body as a String
     */
    public ContinuableFuture<String> asyncPost(final Object request) {
        return asyncPost(request, String.class);
    }

    /**
     * Performs an asynchronous POST request and deserializes the response to the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpClient client = HttpClient.create("https://example.com/api/users");
     * ContinuableFuture<User> future = client.asyncPost(new User("John"), User.class);
     * // future completes with the created resource deserialized into a User (when executed)
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param request The request body (can be String, byte[], File, InputStream, Reader, or any object for serialization)
     * @param resultClass The class of the expected response object (for deserialization)
     * @return A ContinuableFuture that will complete with the deserialized response object
     */
    public <T> ContinuableFuture<T> asyncPost(final Object request, final Class<T> resultClass) {
        return asyncPost(request, _settings, resultClass);
    }

    /**
     * Performs an asynchronous POST request with custom settings and returns the response as a String.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpClient client = HttpClient.create("https://example.com/api/users");
     * HttpSettings settings = HttpSettings.create().setContentType("application/json");
     * ContinuableFuture<String> future = client.asyncPost("{\"name\":\"John\"}", settings);
     * // future completes with the response body as a String (when executed)
     * }</pre>
     *
     * @param request The request body (can be String, byte[], File, InputStream, Reader, or any object for serialization)
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @return A ContinuableFuture that will complete with the response body as a String
     */
    public ContinuableFuture<String> asyncPost(final Object request, final HttpSettings settings) {
        return asyncPost(request, settings, String.class);
    }

    /**
     * Performs an asynchronous POST request with custom settings and deserializes the response to the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpClient client = HttpClient.create("https://example.com/api/users");
     * HttpSettings settings = HttpSettings.create().header("Authorization", "Bearer token123");
     * ContinuableFuture<User> future = client.asyncPost(new User("John"), settings, User.class);
     * // future completes with the created resource deserialized into a User (when executed)
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param request The request body (can be String, byte[], File, InputStream, Reader, or any object for serialization)
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @param resultClass The class of the expected response object (for deserialization)
     * @return A ContinuableFuture that will complete with the deserialized response object
     */
    public <T> ContinuableFuture<T> asyncPost(final Object request, final HttpSettings settings, final Class<T> resultClass) {
        return asyncExecute(HttpMethod.POST, request, settings, resultClass);
    }

    /**
     * Performs an asynchronous PUT request with the specified request body.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User updatedUser = new User("John", "Doe");
     * client.asyncPut(updatedUser)
     *     .thenRunAsync(response -> System.out.println("Updated: " + response));
     * }</pre>
     *
     * @param request The request body (can be String, byte[], File, InputStream, Reader, or any object for serialization)
     * @return A ContinuableFuture that will complete with the response body as a String
     */
    public ContinuableFuture<String> asyncPut(final Object request) {
        return asyncPut(request, String.class);
    }

    /**
     * Performs an asynchronous PUT request and deserializes the response to the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * UpdateUserRequest updateRequest = new UpdateUserRequest("Jane", "Doe");
     * client.asyncPut(updateRequest, User.class)
     *     .thenRunAsync(user -> System.out.println("Updated user: " + user.getName()));
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param request The request body (can be String, byte[], File, InputStream, Reader, or any object for serialization)
     * @param resultClass The class of the expected response object (for deserialization)
     * @return A ContinuableFuture that will complete with the deserialized response object
     */
    public <T> ContinuableFuture<T> asyncPut(final Object request, final Class<T> resultClass) {
        return asyncPut(request, _settings, resultClass);
    }

    /**
     * Performs an asynchronous PUT request with custom settings and returns the response as a String.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User updatedUser = new User("John", "Doe");
     * HttpSettings settings = HttpSettings.create()
     *     .header("If-Match", "\"abc123\"");
     * client.asyncPut(updatedUser, settings)
     *     .thenRunAsync(response -> System.out.println("Response: " + response));
     * }</pre>
     *
     * @param request The request body (can be String, byte[], File, InputStream, Reader, or any object for serialization)
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @return A ContinuableFuture that will complete with the response body as a String
     */
    public ContinuableFuture<String> asyncPut(final Object request, final HttpSettings settings) {
        return asyncPut(request, settings, String.class);
    }

    /**
     * Performs an asynchronous PUT request with custom settings and deserializes the response to the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * UpdateUserRequest updateRequest = new UpdateUserRequest("Jane", "Doe");
     * HttpSettings settings = HttpSettings.create()
     *     .header("If-Match", "\"abc123\"")
     *     .setReadTimeout(30000);
     * client.asyncPut(updateRequest, settings, User.class)
     *     .thenRunAsync((user, exception) -> {
     *         if (exception != null) {
     *             System.err.println("Update failed: " + exception.getMessage());
     *         } else {
     *             System.out.println("Updated: " + user.getName());
     *         }
     *     });
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param request The request body (can be String, byte[], File, InputStream, Reader, or any object for serialization)
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @param resultClass The class of the expected response object (for deserialization)
     * @return A ContinuableFuture that will complete with the deserialized response object
     */
    public <T> ContinuableFuture<T> asyncPut(final Object request, final HttpSettings settings, final Class<T> resultClass) {
        return asyncExecute(HttpMethod.PUT, request, settings, resultClass);
    }

    /**
     * Performs an asynchronous HEAD request with default settings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpClient client = HttpClient.create("https://example.com/api/status");
     * ContinuableFuture<HttpResponse> future = client.asyncHead();
     * int statusCode = future.get().statusCode();
     * }</pre>
     *
     * @return A ContinuableFuture that will complete with the {@link HttpResponse} containing the
     *         status code and headers (the body is empty for HEAD), or with {@code null} when the
     *         client-level settings mark requests as one-way ({@link HttpSettings#setOneWayRequest(boolean)})
     */
    public ContinuableFuture<HttpResponse> asyncHead() {
        return asyncHead(_settings);
    }

    /**
     * Performs an asynchronous HEAD request with custom settings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpClient client = HttpClient.create("https://example.com/api/status");
     * HttpSettings settings = HttpSettings.create().header("Authorization", "Bearer token123");
     * ContinuableFuture<HttpResponse> future = client.asyncHead(settings);
     * String contentLength = future.get().headers().getOrDefault("Content-Length", List.of()).stream().findFirst().orElse(null);
     * }</pre>
     *
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @return A ContinuableFuture that will complete with the {@link HttpResponse} containing the
     *         status code and headers (the body is empty for HEAD), or with {@code null} when the
     *         effective settings mark the request as one-way ({@link HttpSettings#setOneWayRequest(boolean)})
     */
    public ContinuableFuture<HttpResponse> asyncHead(final HttpSettings settings) {
        return asyncExecute(HttpMethod.HEAD, null, settings, HttpResponse.class);
    }

    /**
     * Executes an asynchronous HTTP request with the specified method and request body, returning the response as a String.
     * The request is submitted to the executor and returns immediately without blocking the calling thread.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpClient client = HttpClient.create("https://example.com/api/data");
     * ContinuableFuture<String> future = client.asyncExecute(HttpMethod.GET, null);
     * // future completes with the response body as a String (when executed)
     * }</pre>
     *
     * @param httpMethod The HTTP method to use (GET, POST, PUT, DELETE, HEAD, etc.)
     * @param request The payload: the request body for POST/PUT/PATCH/OPTIONS, or query
     *        parameters appended to the URL (pre-encoded {@code String}, {@code Map} or bean) for
     *        every other method. May be {@code null} for no payload
     * @return A ContinuableFuture that will complete with the response body as a String
     */
    public ContinuableFuture<String> asyncExecute(final HttpMethod httpMethod, final Object request) {
        return asyncExecute(httpMethod, request, String.class);
    }

    /**
     * Executes an asynchronous HTTP request and deserializes the response to the specified type.
     * The request is submitted to the executor and returns immediately without blocking the calling thread.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpClient client = HttpClient.create("https://example.com/api/users/1");
     * ContinuableFuture<User> future = client.asyncExecute(HttpMethod.GET, null, User.class);
     * // future completes with the response deserialized into a User (when executed)
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param httpMethod The HTTP method to use (GET, POST, PUT, DELETE, HEAD, etc.)
     * @param request The payload: the request body for POST/PUT/PATCH/OPTIONS, or query
     *        parameters appended to the URL (pre-encoded {@code String}, {@code Map} or bean) for
     *        every other method. May be {@code null} for no payload
     * @param resultClass The class of the expected response object (for deserialization)
     * @return A ContinuableFuture that will complete with the deserialized response
     */
    public <T> ContinuableFuture<T> asyncExecute(final HttpMethod httpMethod, final Object request, final Class<T> resultClass) {
        return asyncExecute(httpMethod, request, _settings, resultClass);
    }

    /**
     * Executes an asynchronous HTTP request with custom settings and returns the response as a String.
     * The request is submitted to the executor with the specified HTTP settings and returns immediately
     * without blocking the calling thread.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpClient client = HttpClient.create("https://example.com/api/data");
     * HttpSettings settings = HttpSettings.create().header("Accept", "application/json");
     * ContinuableFuture<String> future = client.asyncExecute(HttpMethod.GET, null, settings);
     * // future completes with the response body as a String (when executed)
     * }</pre>
     *
     * @param httpMethod The HTTP method to use (GET, POST, PUT, DELETE, HEAD, etc.)
     * @param request The payload: the request body for POST/PUT/PATCH/OPTIONS, or query
     *        parameters appended to the URL (pre-encoded {@code String}, {@code Map} or bean) for
     *        every other method. May be {@code null} for no payload
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @return A ContinuableFuture that will complete with the response body as a String
     */
    public ContinuableFuture<String> asyncExecute(final HttpMethod httpMethod, final Object request, final HttpSettings settings) {
        return asyncExecute(httpMethod, request, settings, String.class);
    }

    /**
     * Executes an asynchronous HTTP request with all options and deserializes the response to the specified type.
     * This is the core async method that all other async request methods delegate to. Provides full control
     * over the HTTP method, request body, settings, and response deserialization type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpClient client = HttpClient.create("https://example.com/api/users");
     * HttpSettings settings = HttpSettings.create().header("Authorization", "Bearer token123");
     * ContinuableFuture<User> future = client.asyncExecute(HttpMethod.POST, new User("John"), settings, User.class);
     * // future completes with the created resource deserialized into a User (when executed)
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param httpMethod The HTTP method to use (GET, POST, PUT, DELETE, HEAD, etc.)
     * @param request The payload: the request body for POST/PUT/PATCH/OPTIONS, or query
     *        parameters appended to the URL (pre-encoded {@code String}, {@code Map} or bean) for
     *        every other method. May be {@code null} for no payload
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @param resultClass The class of the expected response object (for deserialization)
     * @return A ContinuableFuture that will complete with the deserialized response
     */
    public <T> ContinuableFuture<T> asyncExecute(final HttpMethod httpMethod, final Object request, final HttpSettings settings, final Class<T> resultClass) {
        final Callable<T> cmd = () -> execute(httpMethod, request, settings, resultClass);

        return _asyncExecutor.execute(cmd);
    }

    /**
     * Executes an asynchronous HTTP request and writes the response body to the specified file.
     * The request is submitted to the executor and the response is streamed directly to the file
     * without returning the body as an object.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpClient client = HttpClient.create("https://example.com/file.zip");
     * File output = new File("download.zip");
     * ContinuableFuture<Void> future = client.asyncExecute(HttpMethod.GET, null, null, output);
     * // future completes (with a null result) after the body is written to the file (when executed)
     * }</pre>
     *
     * @param httpMethod The HTTP method to use (GET, POST, PUT, DELETE, HEAD, etc.)
     * @param request The payload: the request body for POST/PUT/PATCH/OPTIONS, or query
     *        parameters appended to the URL (pre-encoded {@code String}, {@code Map} or bean) for
     *        every other method. May be {@code null} for no payload
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @param output The file to write the response to
     * @return a ContinuableFuture that completes after the response has been written to the file
     */
    public ContinuableFuture<Void> asyncExecute(final HttpMethod httpMethod, final Object request, final HttpSettings settings, final File output) {
        final Callable<Void> cmd = () -> {
            execute(httpMethod, request, settings, output);

            return null;
        };

        return _asyncExecutor.execute(cmd);
    }

    /**
     * Executes an asynchronous HTTP request and writes the response body to the given output stream.
     * The stream is not closed by this method; the caller is responsible for closing it.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpClient client = HttpClient.create("https://example.com/file.zip");
     * try (OutputStream out = new FileOutputStream("download.zip")) {
     *     ContinuableFuture<Void> future = client.asyncExecute(HttpMethod.GET, null, null, out);
     *     future.get(); // wait until all response bytes have been written before closing out
     * }
     * }</pre>
     *
     * @param httpMethod The HTTP method to use (GET, POST, PUT, DELETE, HEAD, etc.)
     * @param request The payload: the request body for POST/PUT/PATCH/OPTIONS, or query
     *        parameters appended to the URL (pre-encoded {@code String}, {@code Map} or bean) for
     *        every other method. May be {@code null} for no payload
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @param output The output stream to write the response to
     * @return a ContinuableFuture that completes after the response has been written to the stream
     */
    public ContinuableFuture<Void> asyncExecute(final HttpMethod httpMethod, final Object request, final HttpSettings settings, final OutputStream output) {
        final Callable<Void> cmd = () -> {
            execute(httpMethod, request, settings, output);

            return null;
        };

        return _asyncExecutor.execute(cmd);
    }

    /**
     * Executes an asynchronous HTTP request and writes the response body to the given writer.
     * The writer is not closed by this method; the caller is responsible for closing it.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpClient client = HttpClient.create("https://example.com/api/data");
     * Writer writer = new StringWriter();
     * ContinuableFuture<Void> future = client.asyncExecute(HttpMethod.GET, null, null, writer);
     * // future completes (with a null result) after the body is written to the writer (when executed)
     * }</pre>
     *
     * @param httpMethod The HTTP method to use (GET, POST, PUT, DELETE, HEAD, etc.)
     * @param request The payload: the request body for POST/PUT/PATCH/OPTIONS, or query
     *        parameters appended to the URL (pre-encoded {@code String}, {@code Map} or bean) for
     *        every other method. May be {@code null} for no payload
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @param output The writer to write the response to
     * @return a ContinuableFuture that completes after the response has been written to the writer
     */
    public ContinuableFuture<Void> asyncExecute(final HttpMethod httpMethod, final Object request, final HttpSettings settings, final Writer output) {
        final Callable<Void> cmd = () -> {
            execute(httpMethod, request, settings, output);

            return null;
        };

        return _asyncExecutor.execute(cmd);
    }

    /**
     * Closes this HTTP client and releases any resources it owns.
     *
     * <p>Connections are managed per request, so there are no pooled connections to drain here.
     * A custom {@link Executor} supplied to a factory remains owned by the caller and is not shut
     * down by this method; it may be shared with other components or clients.</p>
     *
     * <p>This method is idempotent and never throws. {@code HttpClient} implements
     * {@link AutoCloseable}, so it can be used in a try-with-resources statement.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (HttpClient client = HttpClient.create("https://example.com/api")) {
     *     String responseBody = client.get();
     * }   // close() is called automatically
     *
     * HttpClient client = HttpClient.create("https://example.com/api");
     * client.close();   // releases owned resources; safe to call more than once
     * client.close();   // idempotent: a second call is a no-op and never throws
     * }</pre>
     *
     */
    @Override
    public void close() {
        // No owned resources. HttpURLConnection instances are closed per request and injected
        // executors remain caller-owned.
    }
}
