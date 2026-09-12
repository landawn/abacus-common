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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.exception.HttpResponseException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.KryoParser;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.Charsets;
import com.landawn.abacus.util.ContinuableFuture;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.URLEncodedUtil;
import com.landawn.abacus.util.cs;

import okhttp3.CacheControl;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * A fluent HTTP request builder and executor based on OkHttp.
 * This class provides a convenient API for building and executing HTTP requests with various features
 * such as headers, query parameters, request bodies, and authentication.
 *
 * <p>Note: This class contains code and docs copied from
 * <a href="https://square.github.io/okhttp/">OkHttp</a> under Apache License v2, possibly with modifications.</p>
 *
 * <p><b>Thread-safety:</b> Instances of this class are mutable builders and are not thread-safe.
 * Each request should be configured and executed from a single thread; the underlying OkHttp
 * {@code OkHttpClient} is itself thread-safe and is meant to be shared.</p>
 *
 * <p><b>Connection reuse:</b> per-request options such as {@link #connectTimeout(long)} and
 * {@link #readTimeout(long)} build a derived {@code OkHttpClient}, which — as OkHttp intends —
 * shares the originating client's dispatcher and connection pool. Sockets and TLS sessions are
 * therefore reused across requests whether or not per-request options are set.</p>
 *
 * <p><b>Request bodies:</b> a body configured through {@code jsonBody}, {@code xmlBody}, {@code formBody} or
 * one of the {@code body(..)} overloads is sent with POST, PUT, PATCH and DELETE. OkHttp forbids a body on
 * GET and HEAD, so {@link #get()}, {@link #head()} and the typed/asynchronous variants throw
 * {@link IllegalArgumentException} when a body has been configured; through the {@code async*} methods that
 * exception surfaces as the failure of the returned future rather than being thrown at submission. Note that
 * {@code formBody(..)} with an empty or {@code null} map still installs an (empty) form body.</p>
 *
 * <p><b>Response ownership:</b> Methods that return a raw {@link Response} transfer ownership to the caller.
 * The response must be closed, preferably with try-with-resources. This also applies to a raw response obtained
 * from a completed asynchronous request. If cancellation wins before an asynchronous raw response
 * is published, the discarded response is closed automatically. Cancellation uses the underlying
 * task's interruption semantics; it does not guarantee that an in-progress HTTP call stops immediately.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Simple GET request
 * try (Response response = OkHttpRequest.url("http://localhost:18080/users")
 *         .header("Accept", "application/json")
 *         .get()) {
 *     // Consume the response.
 * }
 *
 * // POST request with JSON body
 * User user = new User("John", "Doe");
 * String result = OkHttpRequest.url("http://localhost:18080/users")
 *     .jsonBody(user)
 *     .post(String.class);
 *
 * // Asynchronous request
 * ContinuableFuture<Response> future = OkHttpRequest.url("http://localhost:18080/data")
 *     .asyncGet();
 * try (Response response = future.get()) {
 *     // Consume the response.
 * }
 * }</pre>
 *
 * @see URLEncodedUtil
 * @see HttpHeaders
 * @see HttpMethod
 */
public final class OkHttpRequest {

    private static final MediaType APPLICATION_JSON_MEDIA_TYPE = MediaType.get(HttpHeaders.Values.APPLICATION_JSON);
    private static final MediaType APPLICATION_XML_MEDIA_TYPE = MediaType.get(HttpHeaders.Values.APPLICATION_XML);

    private static final KryoParser KRYO_PARSER = ParserFactory.isKryoParserAvailable() ? ParserFactory.createKryoParser() : null;

    /**
     * The client used by the {@code url(..)} factories, and the base every timeout-configured client
     * is derived from so that they all share one dispatcher and connection pool.
     */
    static final OkHttpClient DEFAULT_CLIENT = new OkHttpClient();

    /**
     * Maximum number of bytes read from an error response body when building an
     * {@link HttpResponseException}; see {@link HttpUtil#MAX_ERROR_BODY_SIZE}.
     */
    private static final int MAX_ERROR_BODY_SIZE = HttpUtil.MAX_ERROR_BODY_SIZE;

    private final String url;
    private final HttpUrl httpUrl;

    private Object query;

    private final OkHttpClient httpClient;
    private OkHttpClient.Builder httpClientBuilder;
    private final Request.Builder requestBuilder;
    private RequestBody body;

    /**
     * Constructs an {@code OkHttpRequest} for the given target and client.
     * Exactly one of {@code url} and {@code httpUrl} identifies the target; at least one must be
     * supplied. This constructor is package-private, so use one of the {@code url(...)} or
     * {@code create(...)} factories.
     *
     * @param url the target URL as a string, or {@code null} when {@code httpUrl} is supplied
     * @param httpUrl the target URL as an OkHttp {@link HttpUrl}, or {@code null} when {@code url} is supplied
     * @param httpClient the OkHttp client used to execute this request; must not be {@code null}
     * @throws IllegalArgumentException if {@code url} is {@code null} or empty and {@code httpUrl} is
     *         {@code null}, or if {@code httpClient} is {@code null}.
     */
    OkHttpRequest(final String url, final HttpUrl httpUrl, final OkHttpClient httpClient) throws IllegalArgumentException {
        N.checkArgument(!(Strings.isEmpty(url) && httpUrl == null), "'url' cannot be null or empty");
        N.checkArgNotNull(httpClient, cs.httpClient);

        this.url = url;
        this.httpUrl = httpUrl;
        this.httpClient = httpClient;
        requestBuilder = new Request.Builder();
    }

    /**
     * Creates a new OkHttpRequest instance with the specified URL and HTTP client.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OkHttpClient client = new OkHttpClient();
     * OkHttpRequest req = OkHttpRequest.create("http://localhost:18080/users", client)
     *         .header("Accept", "application/json");
     * // req.get();   // returns the response when executed (network)
     * }</pre>
     *
     * @param url the URL string for the request
     * @param httpClient the OkHttpClient to use for executing the request
     * @return a new OkHttpRequest instance
     * @throws IllegalArgumentException if {@code url} is {@code null} or empty, or {@code httpClient} is {@code null}.
     */
    public static OkHttpRequest create(final String url, final OkHttpClient httpClient) throws IllegalArgumentException {
        return new OkHttpRequest(url, null, httpClient);
    }

    /**
     * Creates a new OkHttpRequest instance with the specified URL and HTTP client.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OkHttpClient client = new OkHttpClient();
     * URL url = new URL("http://localhost:18080/users");
     * OkHttpRequest req = OkHttpRequest.create(url, client)
     *         .header("Accept", "application/json");
     * // req.get();   // returns the response when executed (network)
     * }</pre>
     *
     * @param url the URL object for the request
     * @param httpClient the OkHttpClient to use for executing the request
     * @return a new OkHttpRequest instance
     * @throws IllegalArgumentException if the scheme of {@code url} is not {@code http} or {@code https}, or {@code httpClient} is {@code null}.
     */
    public static OkHttpRequest create(final URL url, final OkHttpClient httpClient) throws IllegalArgumentException {
        return new OkHttpRequest(null, HttpUrl.get(url), httpClient);
    }

    /**
     * Creates a new OkHttpRequest instance with the specified HttpUrl and HTTP client.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OkHttpClient client = new OkHttpClient();
     * HttpUrl url = HttpUrl.get("http://localhost:18080/users");
     * OkHttpRequest req = OkHttpRequest.create(url, client)
     *         .header("Accept", "application/json");
     * // req.get();   // returns the response when executed (network)
     * }</pre>
     *
     * @param url the HttpUrl object for the request
     * @param httpClient the OkHttpClient to use for executing the request
     * @return a new OkHttpRequest instance
     * @throws IllegalArgumentException if {@code url} is {@code null}, or {@code httpClient} is {@code null}.
     */
    public static OkHttpRequest create(final HttpUrl url, final OkHttpClient httpClient) throws IllegalArgumentException {
        return new OkHttpRequest(null, url, httpClient);
    }

    /**
     * Creates a new OkHttpRequest instance with the specified URL using the default HTTP client.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OkHttpRequest request = OkHttpRequest.url("http://localhost:18080/users");
     * }</pre>
     *
     * <p>Note: only non-emptiness of {@code url} is checked here. Scheme/host validity is not
     * verified at construction; an {@link IllegalArgumentException} may be thrown later, when the
     * request is executed, if it is not a valid HTTP or HTTPS URL. To validate up front, use
     * {@link HttpUrl#parse(String)}, which returns {@code null} for invalid URLs.</p>
     *
     * @param url the URL string for the request
     * @return a new OkHttpRequest instance
     * @throws IllegalArgumentException if {@code url} is {@code null} or empty.
     */
    public static OkHttpRequest url(final String url) throws IllegalArgumentException {
        return create(url, DEFAULT_CLIENT);
    }

    /**
     * Creates a new OkHttpRequest instance with the specified URL using the default HTTP client.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * URL url = new URL("http://localhost:18080/users");
     * OkHttpRequest req = OkHttpRequest.url(url)
     *         .header("Accept", "application/json");
     * // req.get();   // returns the response when executed (network)
     * }</pre>
     *
     * @param url the URL object for the request
     * @return a new OkHttpRequest instance
     * @throws IllegalArgumentException if the scheme of {@code url} is not {@code http} or {@code https}.
     */
    public static OkHttpRequest url(final URL url) throws IllegalArgumentException {
        return create(url, DEFAULT_CLIENT);
    }

    /**
     * Creates a new OkHttpRequest instance with the specified HttpUrl using the default HTTP client.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpUrl url = HttpUrl.get("http://localhost:18080/users");
     * OkHttpRequest req = OkHttpRequest.url(url)
     *         .header("Accept", "application/json");
     * // req.get();   // returns the response when executed (network)
     * }</pre>
     *
     * @param url the HttpUrl object for the request
     * @return a new OkHttpRequest instance
     * @throws IllegalArgumentException if {@code url} is {@code null}.
     */
    public static OkHttpRequest url(final HttpUrl url) throws IllegalArgumentException {
        return create(url, DEFAULT_CLIENT);
    }

    /**
     * Creates a new OkHttpRequest instance with the specified URL and timeout settings.
     * The request uses a client derived from the default client with the specified timeouts. The derived
     * client shares the default client's dispatcher and connection pool and is <i>not</i> closed after
     * execution, so sockets and TLS sessions stay reusable across requests. Following OkHttp's contract,
     * a timeout of {@code 0} disables that timeout.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Connect timeout of 3s, read timeout of 10s
     * OkHttpRequest req = OkHttpRequest.url("http://localhost:18080/users", 3000, 10000)
     *         .header("Accept", "application/json");
     * // req.get();   // returns the response when executed (network)
     * }</pre>
     *
     * @param url the URL string for the request
     * @param connectTimeoutInMillis the connection timeout in milliseconds; must be non-negative and fit in an {@code int}
     * @param readTimeoutInMillis the read timeout in milliseconds; must be non-negative and fit in an {@code int}
     * @return a new OkHttpRequest instance
     * @throws IllegalArgumentException if {@code url} is {@code null} or empty, or either timeout is negative
     *         or too large for an {@code int}.
     */
    public static OkHttpRequest url(final String url, final long connectTimeoutInMillis, final long readTimeoutInMillis) throws IllegalArgumentException {
        return create(url, newClient(connectTimeoutInMillis, readTimeoutInMillis));
    }

    /**
     * Builds a timeout-configured client that shares {@link #DEFAULT_CLIENT}'s dispatcher and
     * connection pool, so sockets stay reusable across requests.
     *
     * @param connectTimeoutInMillis the connection timeout in milliseconds
     * @param readTimeoutInMillis the read timeout in milliseconds
     * @return a derived client with the requested timeouts
     * @throws IllegalArgumentException if either timeout is negative or too large for an {@code int}
     */
    private static OkHttpClient newClient(final long connectTimeoutInMillis, final long readTimeoutInMillis) throws IllegalArgumentException {
        N.checkArgNotNegative(connectTimeoutInMillis, cs.connectTimeoutInMillis);
        N.checkArgNotNegative(readTimeoutInMillis, cs.readTimeoutInMillis);

        return DEFAULT_CLIENT.newBuilder()
                .connectTimeout(connectTimeoutInMillis, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeoutInMillis, TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * Creates a new OkHttpRequest instance with the specified URL and timeout settings.
     * The request uses a client derived from the default client with the specified timeouts. The derived
     * client shares the default client's dispatcher and connection pool and is <i>not</i> closed after
     * execution, so sockets and TLS sessions stay reusable across requests. Following OkHttp's contract,
     * a timeout of {@code 0} disables that timeout.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * URL url = new URL("http://localhost:18080/users");
     * OkHttpRequest req = OkHttpRequest.url(url, 3000, 10000)
     *         .header("Accept", "application/json");
     * // req.get();   // returns the response when executed (network)
     * }</pre>
     *
     * @param url the URL object for the request
     * @param connectTimeoutInMillis the connection timeout in milliseconds; must be non-negative and fit in an {@code int}
     * @param readTimeoutInMillis the read timeout in milliseconds; must be non-negative and fit in an {@code int}
     * @return a new OkHttpRequest instance
     * @throws IllegalArgumentException if the scheme of {@code url} is not {@code http} or {@code https}, or
     *         either timeout is negative or too large for an {@code int}.
     */
    public static OkHttpRequest url(final URL url, final long connectTimeoutInMillis, final long readTimeoutInMillis) throws IllegalArgumentException {
        return create(url, newClient(connectTimeoutInMillis, readTimeoutInMillis));
    }

    /**
     * Creates a new OkHttpRequest instance with the specified HttpUrl and timeout settings.
     * The request uses a client derived from the default client with the specified timeouts. The derived
     * client shares the default client's dispatcher and connection pool and is <i>not</i> closed after
     * execution, so sockets and TLS sessions stay reusable across requests. Following OkHttp's contract,
     * a timeout of {@code 0} disables that timeout.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpUrl url = HttpUrl.get("http://localhost:18080/users");
     * OkHttpRequest req = OkHttpRequest.url(url, 3000, 10000)
     *         .header("Accept", "application/json");
     * // req.get();   // returns the response when executed (network)
     * }</pre>
     *
     * @param url the HttpUrl object for the request
     * @param connectTimeoutInMillis the connection timeout in milliseconds; must be non-negative and fit in an {@code int}
     * @param readTimeoutInMillis the read timeout in milliseconds; must be non-negative and fit in an {@code int}
     * @return a new OkHttpRequest instance
     * @throws IllegalArgumentException if {@code url} is {@code null}, or either timeout is negative or too
     *         large for an {@code int}.
     */
    public static OkHttpRequest url(final HttpUrl url, final long connectTimeoutInMillis, final long readTimeoutInMillis) throws IllegalArgumentException {
        return create(url, newClient(connectTimeoutInMillis, readTimeoutInMillis));
    }

    /**
     * Returns the builder used to derive a per-request client, creating it on first use.
     *
     * <p>The derived client deliberately keeps the originating client's {@code Dispatcher} and
     * {@code ConnectionPool}, which is what {@link OkHttpClient#newBuilder()} does by default.
     * Replacing them per request gave every call a private, immediately discarded pool, so no
     * connection or TLS session was ever reused.</p>
     *
     * @return the per-request client builder
     */
    private OkHttpClient.Builder clientBuilder() {
        if (httpClientBuilder == null) {
            httpClientBuilder = httpClient.newBuilder();
        }

        return httpClientBuilder;
    }

    /**
     * Sets the connection timeout in milliseconds for this HTTP request.
     * The connection timeout defines how long to wait when establishing a connection
     * to the remote server before timing out.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OkHttpRequest.url("http://localhost:18080")
     *     .connectTimeout(5000) // 5 seconds
     *     .get();
     * }</pre>
     *
     * <p>Following OkHttp's own contract, {@code 0} means <i>no timeout</i> — not "use the default",
     * which is how {@link HttpRequest#connectTimeout(long)} interprets it.</p>
     *
     * @param connectTimeout The connection timeout in milliseconds. Must be non-negative and must fit
     *        in an {@code int}; {@code 0} disables the timeout.
     * @return This OkHttpRequest instance for method chaining
     * @throws IllegalArgumentException if {@code connectTimeout} is negative or too large for an {@code int}.
     */
    public OkHttpRequest connectTimeout(final long connectTimeout) throws IllegalArgumentException {
        N.checkArgNotNegative(connectTimeout, cs.connectTimeout);

        clientBuilder().connectTimeout(connectTimeout, TimeUnit.MILLISECONDS);

        return this;
    }

    /**
     * Sets the connection timeout using a Duration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OkHttpRequest.url("http://localhost:18080")
     *     .connectTimeout(Duration.ofSeconds(5))
     *     .get();
     * }</pre>
     *
     * <p>The timeout is applied with millisecond precision. A positive duration shorter than one
     * millisecond is rejected rather than silently truncated to {@code 0}, which OkHttp would read as
     * "no timeout".</p>
     *
     * @param connectTimeout The connection timeout as a Duration; must not be {@code null} and must not
     *        be negative. {@link Duration#ZERO} disables the timeout; any other value must be at least
     *        1 ms and fit in an {@code int} number of milliseconds.
     * @return This OkHttpRequest instance for method chaining
     * @throws IllegalArgumentException if {@code connectTimeout} is {@code null}, negative, positive but
     *         shorter than 1 ms, or too large for an {@code int} number of milliseconds.
     */
    public OkHttpRequest connectTimeout(final Duration connectTimeout) throws IllegalArgumentException {
        N.checkArgNotNull(connectTimeout, cs.connectTimeout);

        clientBuilder().connectTimeout(toTimeoutMillis(connectTimeout, cs.connectTimeout), TimeUnit.MILLISECONDS);

        return this;
    }

    /**
     * Converts a timeout {@code Duration} to whole milliseconds, enforcing this class's documented
     * {@link IllegalArgumentException} contract before the value reaches OkHttp (whose own checks
     * throw {@code IllegalStateException} for a negative value, and accept a sub-millisecond one as 0).
     *
     * @param timeout the non-null duration to convert
     * @param argName the argument name for the exception message
     * @return the duration in milliseconds; {@code 0} only for {@link Duration#ZERO}
     * @throws IllegalArgumentException if {@code timeout} is negative, positive but shorter than 1 ms,
     *         or does not fit in a {@code long} number of milliseconds
     */
    private static long toTimeoutMillis(final Duration timeout, final String argName) throws IllegalArgumentException {
        N.checkArgument(!timeout.isNegative(), "'%s' can not be negative: %s", argName, timeout);

        final long millis;

        try {
            millis = timeout.toMillis();
        } catch (final ArithmeticException e) {
            throw new IllegalArgumentException("'" + argName + "' is too large for a millisecond timeout: " + timeout, e);
        }

        // Duration.toMillis() truncates, so a positive sub-millisecond value would reach OkHttp as 0,
        // which OkHttp defines as "no timeout" - the opposite of what the caller asked for.
        N.checkArgument(millis > 0 || timeout.isZero(), "'%s' must be zero or at least 1 ms: %s", argName, timeout);

        return millis;
    }

    /**
     * Sets the read timeout in milliseconds for this HTTP request.
     * The read timeout defines how long to wait for data to be received from
     * the server after the connection has been established.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OkHttpRequest.url("http://localhost:18080")
     *     .readTimeout(10000) // 10 seconds
     *     .get();
     * }</pre>
     *
     * <p>Following OkHttp's own contract, {@code 0} means <i>no timeout</i> — not "use the default",
     * which is how {@link HttpRequest#readTimeout(long)} interprets it.</p>
     *
     * @param readTimeout The read timeout in milliseconds. Must be non-negative and must fit in an
     *        {@code int}; {@code 0} disables the timeout.
     * @return This OkHttpRequest instance for method chaining
     * @throws IllegalArgumentException if {@code readTimeout} is negative or too large for an {@code int}.
     */
    public OkHttpRequest readTimeout(final long readTimeout) throws IllegalArgumentException {
        N.checkArgNotNegative(readTimeout, cs.readTimeout);

        clientBuilder().readTimeout(readTimeout, TimeUnit.MILLISECONDS);

        return this;
    }

    /**
     * Sets the read timeout using a Duration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OkHttpRequest.url("http://localhost:18080")
     *     .readTimeout(Duration.ofSeconds(10))
     *     .get();
     * }</pre>
     *
     * <p>The timeout is applied with millisecond precision. A positive duration shorter than one
     * millisecond is rejected rather than silently truncated to {@code 0}, which OkHttp would read as
     * "no timeout".</p>
     *
     * @param readTimeout The read timeout as a Duration; must not be {@code null} and must not be
     *        negative. {@link Duration#ZERO} disables the timeout; any other value must be at least
     *        1 ms and fit in an {@code int} number of milliseconds.
     * @return This OkHttpRequest instance for method chaining
     * @throws IllegalArgumentException if {@code readTimeout} is {@code null}, negative, positive but
     *         shorter than 1 ms, or too large for an {@code int} number of milliseconds.
     */
    public OkHttpRequest readTimeout(final Duration readTimeout) throws IllegalArgumentException {
        N.checkArgNotNull(readTimeout, cs.readTimeout);

        clientBuilder().readTimeout(toTimeoutMillis(readTimeout, cs.readTimeout), TimeUnit.MILLISECONDS);

        return this;
    }

    /**
     * Sets this request's {@code Cache-Control} header, replacing any cache control headers already
     * present. If {@code cacheControl} doesn't define any directives, this clears this request's
     * cache-control headers.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CacheControl cacheControl = new CacheControl.Builder()
     *     .noCache()
     *     .build();
     *
     * OkHttpRequest.url("http://localhost:18080/data")
     *     .cacheControl(cacheControl)
     *     .get();
     * }</pre>
     *
     * @param cacheControl the cache control directives
     * @return this OkHttpRequest instance for method chaining
     * @throws IllegalArgumentException if {@code cacheControl} is {@code null}.
     */
    public OkHttpRequest cacheControl(final CacheControl cacheControl) throws IllegalArgumentException {
        N.checkArgNotNull(cacheControl, cs.cacheControl);

        requestBuilder.cacheControl(cacheControl);
        return this;
    }

    /**
     * Attaches {@code tag} to the request. It can be used later to cancel the request. If the tag
     * is unspecified or {@code null}, the request is canceled by using the request itself as the tag.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Object requestTag = "my-request-id";
     * OkHttpClient client = new OkHttpClient();
     * OkHttpRequest.create("http://localhost:18080/data", client)
     *     .tag(requestTag)
     *     .asyncGet();
     *
     * // Later, cancel the request using the tag
     * for (okhttp3.Call call : client.dispatcher().queuedCalls()) {
     *     if (requestTag.equals(call.request().tag())) {
     *         call.cancel();
     *     }
     * }
     * for (okhttp3.Call call : client.dispatcher().runningCalls()) {
     *     if (requestTag.equals(call.request().tag())) {
     *         call.cancel();
     *     }
     * }
     * client.dispatcher().executorService().shutdown();
     * client.connectionPool().evictAll();
     * }</pre>
     *
     * @param tag the tag to attach to the request
     * @return this OkHttpRequest instance for method chaining
     */
    public OkHttpRequest tag(final Object tag) {
        requestBuilder.tag(tag);
        return this;
    }

    /**
     * Attaches {@code tag} to the request using {@code type} as a key. Tags can be read from a
     * request using {@link Request#tag}. Use {@code null} to remove any existing tag assigned for {@code
     * type}.
     *
     * <p>Use this API to attach timing, debugging, or other application data to a request so that
     * you may read it in interceptors, event listeners, or callbacks.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * class RequestMetadata {
     *     long startTime;
     *     String requestId;
     * }
     *
     * RequestMetadata metadata = new RequestMetadata();
     * metadata.startTime = System.currentTimeMillis();
     * metadata.requestId = "req-123";
     *
     * OkHttpRequest.url("http://localhost:18080/data")
     *     .tag(RequestMetadata.class, metadata)
     *     .get();
     * }</pre>
     *
     * @param <T> the type of the tag
     * @param type the class type used as a key for the tag
     * @param tag the tag to attach, or {@code null} to remove existing tag
     * @return this OkHttpRequest instance for method chaining
     */
    public <T> OkHttpRequest tag(final Class<? super T> type, final T tag) {
        requestBuilder.tag(type, tag);
        return this;
    }

    /**
     * Sets the Basic Authentication header for this request.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OkHttpRequest.url("http://localhost:18080/secure")
     *     .basicAuth("username", "password")
     *     .get();
     * }</pre>
     *
     * <p>Neither argument is validated: a {@code null} username or password is stringified as the
     * literal {@code "null"} before encoding (so {@code basicAuth(null, "p")} sends the credential
     * {@code null:p}), consistent with {@link HttpHeaders#setBasicAuthentication(String, String)},
     * {@link HttpSettings#basicAuth(String, String)} and {@link HttpRequest#basicAuth(String, String)}.</p>
     *
     * @param username the username for authentication
     * @param password the password for authentication
     * @return this OkHttpRequest instance for method chaining
     */
    public OkHttpRequest basicAuth(final String username, final String password) {
        requestBuilder.header(HttpHeaders.Names.AUTHORIZATION, "Basic " + Strings.base64Encode((username + ":" + password).getBytes(Charsets.UTF_8)));
        return this;
    }

    /**
     * Sets the header named {@code name} to {@code value}.
     * If this request already has any headers with that name, they are all replaced.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OkHttpRequest.url("http://localhost:18080/data")
     *     .header("Accept", "application/json")
     *     .header("User-Agent", "MyApp/1.0")
     *     .get();
     * }</pre>
     *
     * @param name the header name
     * @param value the header value
     * @return this OkHttpRequest instance for method chaining
     * @throws IllegalArgumentException if {@code name} is {@code null}.
     * @see Request.Builder#header(String, String)
     * @see HttpHeaders
     */
    public OkHttpRequest header(final String name, final Object value) throws IllegalArgumentException {
        N.checkArgNotNull(name, cs.name);

        requestBuilder.header(name, HttpHeaders.valueOf(name, value));
        return this;
    }

    /**
     * Sets HTTP headers specified by {@code name1/value1}, {@code name2/value2}.
     * If this request already has any headers with that name, they are all replaced.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OkHttpRequest req = OkHttpRequest.url("http://localhost:18080/data")
     *         .headers("Accept", "application/json", "User-Agent", "MyApp/1.0");
     * // req.get();   // returns the response when executed (network)
     * }</pre>
     *
     * @param name1 the first header name
     * @param value1 the first header value
     * @param name2 the second header name
     * @param value2 the second header value
     * @return this OkHttpRequest instance for method chaining
     * @throws IllegalArgumentException if any header name is {@code null}.
     * @see Request.Builder#header(String, String)
     * @see HttpHeaders
     */
    public OkHttpRequest headers(final String name1, final Object value1, final String name2, final Object value2) throws IllegalArgumentException {
        header(name1, value1);
        header(name2, value2);

        return this;
    }

    /**
     * Sets HTTP headers specified by {@code name1/value1}, {@code name2/value2}, {@code name3/value3}.
     * If this request already has any headers with that name, they are all replaced.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OkHttpRequest req = OkHttpRequest.url("http://localhost:18080/data")
     *         .headers("Accept", "application/json",
     *                  "User-Agent", "MyApp/1.0",
     *                  "Authorization", "Bearer token123");
     * // req.get();   // returns the response when executed (network)
     * }</pre>
     *
     * @param name1 the first header name
     * @param value1 the first header value
     * @param name2 the second header name
     * @param value2 the second header value
     * @param name3 the third header name
     * @param value3 the third header value
     * @return this OkHttpRequest instance for method chaining
     * @throws IllegalArgumentException if any header name is {@code null}.
     * @see Request.Builder#header(String, String)
     * @see HttpHeaders
     */
    public OkHttpRequest headers(final String name1, final Object value1, final String name2, final Object value2, final String name3, final Object value3)
            throws IllegalArgumentException {
        header(name1, value1);
        header(name2, value2);
        header(name3, value3);

        return this;
    }

    /**
     * Merges the given header entries into the headers already on this request.
     * For each entry in the map, a header with the same name is overwritten with the new value,
     * while any existing headers whose names are <i>not</i> present in the map are kept unchanged.
     * This is a merge, not a replace-all: use {@link #setHeaders(HttpHeaders)} if you want to
     * discard all prior headers and install a fresh set.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> headers = new HashMap<>();
     * headers.put("Accept", "application/json");
     * headers.put("Authorization", "Bearer token123");
     *
     * OkHttpRequest.url("http://localhost:18080/data")
     *     .headers(headers)
     *     .get();
     * }</pre>
     *
     * @param headers A map containing header names and values
     * @return This OkHttpRequest instance for method chaining
     * @throws IllegalArgumentException if a header name is {@code null} or empty, or a name or formatted value contains a character rejected by OkHttp.
     * @see #setHeaders(Headers)
     * @see Request.Builder#header(String, String)
     * @see HttpHeaders
     */
    public OkHttpRequest headers(final Map<String, ?> headers) throws IllegalArgumentException {
        if (N.notEmpty(headers)) {
            for (final Map.Entry<String, ?> entry : headers.entrySet()) {
                header(entry.getKey(), entry.getValue());
            }
        }

        return this;
    }

    /**
     * Resets the headers by removing all existing headers first and then adds the specified headers (replace-all).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Headers headers = new Headers.Builder()
     *         .add("Accept", "application/json")
     *         .add("Authorization", "Bearer token123")
     *         .build();
     *
     * OkHttpRequest req = OkHttpRequest.url("http://localhost:18080/data")
     *         .setHeaders(headers);
     * // req.get();   // returns the response when executed (network)
     * }</pre>
     *
     * @param headers the Headers object containing all headers to set
     * @return this OkHttpRequest instance for method chaining
     * @throws IllegalArgumentException if {@code headers} is {@code null}.
     * @see #header(String, Object)
     * @see Request.Builder#headers(Headers)
     * @see HttpHeaders
     */
    public OkHttpRequest setHeaders(final Headers headers) throws IllegalArgumentException {
        N.checkArgNotNull(headers, cs.headers);

        requestBuilder.headers(headers);
        return this;
    }

    /**
     * Resets the headers by removing all existing headers first and then adds the specified headers (replace-all).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpHeaders headers = HttpHeaders.of("Accept", "application/json",
     *                                      "Authorization", "Bearer token123");
     *
     * OkHttpRequest req = OkHttpRequest.url("http://localhost:18080/data")
     *         .setHeaders(headers);
     * // req.get();   // returns the response when executed (network)
     * }</pre>
     *
     * @param headers the HttpHeaders object containing all headers to set
     * @return this OkHttpRequest instance for method chaining
     * @see Request.Builder#headers(Headers)
     * @see HttpHeaders
     */
    public OkHttpRequest setHeaders(final HttpHeaders headers) {
        final Headers.Builder builder = new Headers.Builder();

        if (headers != null && !headers.isEmpty()) {
            for (final String headerName : headers.headerNames()) {
                final Object headerValue = headers.get(headerName);

                // A collection-valued header is emitted as repeated header lines rather than being
                // collapsed into one comma-joined value, which reorders values when the bridge goes through
                // a HashMap. Cookie is the exception: RFC 6265 5.4 allows a request only one Cookie line, so
                // the field-aware HttpHeaders.valueOf(name, value) joins its cookie-pairs with "; " instead.
                if (headerValue instanceof Collection && !HttpHeaders.Names.COOKIE.equalsIgnoreCase(headerName)) {
                    for (final Object element : (Collection<?>) headerValue) {
                        builder.add(headerName, HttpHeaders.valueOf(element));
                    }
                } else {
                    builder.add(headerName, HttpHeaders.valueOf(headerName, headerValue));
                }
            }
        }

        return setHeaders(builder.build());
    }

    /**
     * Adds a header with {@code name} and {@code value}. Prefer this method for multiply-valued
     * headers like "Cookie".
     *
     * <p>Note that for some headers including {@code Content-Length} and {@code Content-Encoding},
     * OkHttp may replace {@code value} with a header derived from the request body.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OkHttpRequest req = OkHttpRequest.url("http://localhost:18080/data")
     *         .addHeader("Cookie", "session=abc")
     *         .addHeader("Cookie", "theme=dark");   // both values are kept
     * // req.get();   // returns the response when executed (network)
     * }</pre>
     *
     * @param name the header name
     * @param value the header value
     * @return this OkHttpRequest instance for method chaining
     * @throws IllegalArgumentException if {@code name} is {@code null}.
     * @deprecated This method is deprecated due to limited use cases in typical HTTP workflows.
     *             Most scenarios require replacing headers rather than adding duplicates.
     *             Use {@link #header(String, Object)} instead, which replaces any existing header
     *             with the same name. If you specifically need to add multiple headers with the
     *             same name (e.g., for multiply-valued headers like "Cookie"), consider using
     *             the underlying OkHttp RequestBuilder directly.
     */
    @Deprecated
    public OkHttpRequest addHeader(final String name, final Object value) throws IllegalArgumentException {
        N.checkArgNotNull(name, cs.name);

        requestBuilder.addHeader(name, HttpHeaders.valueOf(name, value));
        return this;
    }

    /**
     * Removes all headers with the specified name from this request.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OkHttpRequest req = OkHttpRequest.url("http://localhost:18080/data")
     *         .header("X-Debug", "true")
     *         .removeHeader("X-Debug");   // header no longer present
     * // req.get();   // returns the response when executed (network)
     * }</pre>
     *
     * @param name the name of the headers to remove
     * @return this OkHttpRequest instance for method chaining
     * @throws IllegalArgumentException if {@code name} is {@code null}.
     * @deprecated This method is deprecated due to limited use cases in typical HTTP workflows.
     *             In most scenarios, headers are set but rarely need to be explicitly removed.
     *             If you need to override a header, use {@link #header(String, Object)} which
     *             replaces any existing header. If you have a specific use case requiring header
     *             removal, consider restructuring your code or using the underlying OkHttp
     *             RequestBuilder directly.
     */
    @Deprecated
    public OkHttpRequest removeHeader(final String name) throws IllegalArgumentException {
        N.checkArgNotNull(name, cs.name);

        requestBuilder.removeHeader(name);
        return this;
    }

    /**
     * Sets query parameters, typically used for {@code GET} or {@code DELETE} requests.
     * The query string is appended to the URL for any request method (unlike
     * {@link HttpRequest}, which rejects queries on other methods).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OkHttpRequest.url("http://localhost:18080/search")
     *     .query("q=java&limit=10")
     *     .get();
     * }</pre>
     *
     * @param query the query string
     * @return this OkHttpRequest instance for method chaining
     */
    public OkHttpRequest query(final String query) {
        this.query = query;

        return this;
    }

    /**
     * Sets query parameters, typically used for {@code GET} or {@code DELETE} requests.
     * The parameters are URL-encoded and appended to the URL for any request method (unlike
     * {@link HttpRequest}, which rejects queries on other methods).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> params = new HashMap<>();
     * params.put("q", "java programming");
     * params.put("limit", 10);
     *
     * OkHttpRequest.url("http://localhost:18080/search")
     *     .query(params)
     *     .get();
     * }</pre>
     *
     * @param queryParams A map containing query parameter names and values
     * @return This OkHttpRequest instance for method chaining
     */
    public OkHttpRequest query(final Map<String, ?> queryParams) {
        query = queryParams;

        return this;
    }

    /**
     * Sets the request body as JSON with Content-Type: application/json.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String json = "{\"name\":\"John\",\"age\":30}";
     * OkHttpRequest.url("http://localhost:18080/users")
     *     .jsonBody(json)
     *     .post();
     * }</pre>
     *
     * @param json the JSON string to send as the request body
     * @return this OkHttpRequest instance for method chaining
     * @throws IllegalArgumentException if {@code json} is {@code null}.
     */
    public OkHttpRequest jsonBody(final String json) throws IllegalArgumentException {
        return body(json, APPLICATION_JSON_MEDIA_TYPE);
    }

    /**
     * Sets the request body as JSON with Content-Type: application/json.
     * The object will be serialized to JSON using the default JSON serializer.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", 30);
     * OkHttpRequest.url("http://localhost:18080/users")
     *     .jsonBody(user)
     *     .post();
     * }</pre>
     *
     * @param obj the object to serialize to JSON and send as the request body
     * @return this OkHttpRequest instance for method chaining
     */
    public OkHttpRequest jsonBody(final Object obj) {
        return body(N.toJson(obj), APPLICATION_JSON_MEDIA_TYPE);
    }

    /**
     * Sets the request body as XML with Content-Type: application/xml.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String xml = "<user><name>John</name><age>30</age></user>";
     * OkHttpRequest.url("http://localhost:18080/users")
     *     .xmlBody(xml)
     *     .post();
     * }</pre>
     *
     * @param xml the XML string to send as the request body
     * @return this OkHttpRequest instance for method chaining
     * @throws IllegalArgumentException if {@code xml} is {@code null}.
     */
    public OkHttpRequest xmlBody(final String xml) throws IllegalArgumentException {
        return body(xml, APPLICATION_XML_MEDIA_TYPE);
    }

    /**
     * Sets the request body as XML with Content-Type: application/xml.
     * The object will be serialized to XML using the default XML serializer.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", 30);
     * OkHttpRequest.url("http://localhost:18080/users")
     *     .xmlBody(user)
     *     .post();
     * }</pre>
     *
     * @param obj the object to serialize to XML and send as the request body
     * @return this OkHttpRequest instance for method chaining
     */
    public OkHttpRequest xmlBody(final Object obj) {
        return body(N.toXml(obj), APPLICATION_XML_MEDIA_TYPE);
    }

    /**
     * Sets the request body as form data with Content-Type: application/x-www-form-urlencoded.
     * The map entries will be encoded as form fields. Entries with {@code null} values are skipped,
     * consistently with {@link #formBody(Object)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> formData = new HashMap<>();
     * formData.put("username", "john_doe");
     * formData.put("password", "secret123");
     *
     * OkHttpRequest.url("http://localhost:18080/login")
     *     .formBody(formData)
     *     .post();
     * }</pre>
     *
     * @param formBodyByMap A map containing form field names and values
     * @return This OkHttpRequest instance for method chaining
     * @throws IllegalArgumentException if an entry with a non-{@code null} value has a {@code null} key.
     * @see FormBody.Builder
     */
    public OkHttpRequest formBody(final Map<?, ?> formBodyByMap) throws IllegalArgumentException {
        if (N.isEmpty(formBodyByMap)) {
            body = new FormBody.Builder().build();
            return this;
        }

        final FormBody.Builder formBodyBuilder = new FormBody.Builder();

        for (final Map.Entry<?, ?> entry : formBodyByMap.entrySet()) {
            if (entry.getValue() != null) {
                N.checkArgNotNull(entry.getKey(), cs.formFieldName);

                formBodyBuilder.add(N.stringOf(entry.getKey()), N.stringOf(entry.getValue()));
            }
        }

        body = formBodyBuilder.build();
        return this;
    }

    /**
     * Sets the request body as form data with Content-Type: application/x-www-form-urlencoded.
     * The bean properties will be encoded as form fields using getter methods. Properties with
     * {@code null} values are skipped, consistently with {@link #formBody(Map)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LoginRequest login = new LoginRequest();
     * login.setUsername("john_doe");
     * login.setPassword("secret123");
     *
     * OkHttpRequest.url("http://localhost:18080/login")
     *     .formBody(login)
     *     .post();
     * }</pre>
     *
     * @param formBodyByBean a bean object whose properties will be used as form fields
     * @return this OkHttpRequest instance for method chaining
     * @throws IllegalArgumentException if the provided object is not a bean class with getter/setter methods.
     * @see FormBody.Builder
     */
    public OkHttpRequest formBody(final Object formBodyByBean) throws IllegalArgumentException {
        if (formBodyByBean == null) {
            body = new FormBody.Builder().build();
            return this;
        }

        final Class<?> cls = formBodyByBean.getClass();
        N.checkArgument(Beans.isBeanClass(cls), "{} is not a bean class with getter/setter methods", cls);

        final BeanInfo beanInfo = ParserUtil.getBeanInfo(cls);
        final FormBody.Builder formBodyBuilder = new FormBody.Builder();
        Object propValue = null;

        for (final PropInfo propInfo : beanInfo.propInfoList) {
            propValue = propInfo.getPropValue(formBodyByBean);

            if (propValue != null) {
                formBodyBuilder.add(propInfo.name, N.stringOf(propValue));
            }
        }

        body = formBodyBuilder.build();
        return this;
    }

    /**
     * Sets the request body with a custom RequestBody instance.
     * This allows full control over the request body content and media type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * RequestBody body = RequestBody.create("{\"k\":1}", MediaType.get("application/json"));
     *
     * OkHttpRequest req = OkHttpRequest.url("http://localhost:18080/data")
     *         .body(body);
     * // req.post();   // returns the response when executed (network)
     * }</pre>
     *
     * @param body the RequestBody to use
     * @return this OkHttpRequest instance for method chaining
     * @see RequestBody
     */
    public OkHttpRequest body(final RequestBody body) {
        this.body = body;
        return this;
    }

    /**
     * Sets the request body with the specified content and media type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OkHttpRequest req = OkHttpRequest.url("http://localhost:18080/data")
     *         .body("{\"k\":1}", MediaType.get("application/json"));
     * // req.post();   // returns the response when executed (network)
     * }</pre>
     *
     * @param content the string content of the request body
     * @param contentType the media type of the content, or {@code null} to use default
     * @return this OkHttpRequest instance for method chaining
     * @throws IllegalArgumentException if {@code content} is {@code null}.
     * @see RequestBody#create(String, MediaType)
     */
    public OkHttpRequest body(final String content, final MediaType contentType) throws IllegalArgumentException {
        N.checkArgNotNull(content, cs.content);

        body = RequestBody.create(content, contentType);

        return this;
    }

    /**
     * Sets the request body with the specified byte array content and media type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] content = "{\"k\":1}".getBytes(StandardCharsets.UTF_8);
     *
     * OkHttpRequest req = OkHttpRequest.url("http://localhost:18080/data")
     *         .body(content, MediaType.get("application/json"));
     * // req.post();   // returns the response when executed (network)
     * }</pre>
     *
     * @param content the byte array content of the request body
     * @param contentType the media type of the content, or {@code null} to use default
     * @return this OkHttpRequest instance for method chaining
     * @throws IllegalArgumentException if {@code content} is {@code null}.
     * @see RequestBody#create(byte[], MediaType)
     */
    public OkHttpRequest body(final byte[] content, final MediaType contentType) throws IllegalArgumentException {
        N.checkArgNotNull(content, cs.content);

        body = RequestBody.create(content, contentType);

        return this;
    }

    /**
     * Sets the request body with the specified byte array content, offset, length, and media type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] content = "xx{\"k\":1}yy".getBytes(StandardCharsets.UTF_8);
     *
     * // Send only the 7 bytes starting at offset 2, i.e. {"k":1}
     * OkHttpRequest req = OkHttpRequest.url("http://localhost:18080/data")
     *         .body(content, 2, 7, MediaType.get("application/json"));
     * // req.post();   // returns the response when executed (network)
     * }</pre>
     *
     * @param content the byte array content of the request body
     * @param offset the offset in the byte array to start reading from
     * @param byteCount the number of bytes to read from the array
     * @param contentType the media type of the content, or {@code null} to use default
     * @return this OkHttpRequest instance for method chaining
     * @throws IllegalArgumentException if {@code content} is {@code null}.
     * @throws IndexOutOfBoundsException if {@code offset} or {@code byteCount} lies outside {@code content}
     * @see RequestBody#create(byte[], MediaType, int, int)
     */
    public OkHttpRequest body(final byte[] content, final int offset, final int byteCount, final MediaType contentType)
            throws IllegalArgumentException, IndexOutOfBoundsException {
        N.checkArgNotNull(content, cs.content);
        N.checkFromIndexSize(offset, byteCount, content.length);

        body = RequestBody.create(content, contentType, offset, byteCount);

        return this;
    }

    /**
     * Sets the request body with the content from the specified file and media type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("payload.json");
     *
     * OkHttpRequest req = OkHttpRequest.url("http://localhost:18080/upload")
     *         .body(file, MediaType.get("application/json"));
     * // req.post();   // returns the response when executed (network)
     * }</pre>
     *
     * @param content the file containing the request body content
     * @param contentType the media type of the content, or {@code null} to use default
     * @return this OkHttpRequest instance for method chaining
     * @throws IllegalArgumentException if {@code content} is {@code null}.
     * @see RequestBody#create(File, MediaType)
     */
    public OkHttpRequest body(final File content, final MediaType contentType) throws IllegalArgumentException {
        N.checkArgNotNull(content, cs.content);

        body = RequestBody.create(content, contentType);

        return this;
    }

    /**
     * Executes a GET request and returns the response.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Response response = OkHttpRequest.url("http://localhost:18080/users")
     *     .header("Accept", "application/json")
     *     .get()) {
     *     if (response.isSuccessful()) {
     *         String body = response.body().string();
     *     }
     * }
     * }</pre>
     *
     * @return the HTTP response; the caller must close it
     * @throws IllegalArgumentException if a request body has been configured on this request (OkHttp
     *         forbids a body on GET)
     * @throws UncheckedIOException if the request could not be executed
     */
    public Response get() throws IllegalArgumentException, UncheckedIOException {
        return execute(HttpMethod.GET);
    }

    /**
     * Executes a GET request and deserializes the response to the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String response = OkHttpRequest.url("http://localhost:18080/users")
     *     .get(String.class);
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object
     * @return The deserialized response body
     * @throws IllegalArgumentException if {@code resultClass} is {@code null} or {@link HttpResponse}, or a
     *         request body has been configured on this request (OkHttp forbids a body on GET)
     * @throws UncheckedIOException if opening the connection, sending the request, or reading the response body fails
     * @throws HttpResponseException if the status code is not 2xx and resultClass is not okhttp3.Response.class
     */
    public <T> T get(final Class<T> resultClass) throws IllegalArgumentException, UncheckedIOException, HttpResponseException {
        return execute(HttpMethod.GET, resultClass);
    }

    /**
     * Executes a POST request and returns the response.
     * POST requests typically send data to the server to create or update a resource.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User newUser = new User("John", "Doe");
     * try (Response response = OkHttpRequest.url("http://localhost:18080/users")
     *     .jsonBody(newUser)
     *     .post()) {
     *     // Consume the response.
     * }
     * }</pre>
     *
     * @return the HTTP response; the caller must close it
     * @throws UncheckedIOException if the request could not be executed
     */
    public Response post() throws UncheckedIOException {
        return execute(HttpMethod.POST);
    }

    /**
     * Executes a POST request and deserializes the response to the specified type.
     * POST requests typically send data to the server to create or update a resource.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User newUser = new User("John", "Doe");
     * User createdUser = OkHttpRequest.url("http://localhost:18080/users")
     *     .jsonBody(newUser)
     *     .post(User.class);
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object
     * @return The deserialized response body
     * @throws UncheckedIOException if opening the connection, sending the request, or reading the response body fails
     * @throws HttpResponseException if the status code is not 2xx and resultClass is not okhttp3.Response.class
     */
    public <T> T post(final Class<T> resultClass) throws UncheckedIOException, HttpResponseException {
        return execute(HttpMethod.POST, resultClass);
    }

    /**
     * Executes a PUT request and returns the response.
     * PUT requests typically send data to the server to create or fully replace a resource.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User updatedUser = new User("John", "Smith");
     * try (Response response = OkHttpRequest.url("http://localhost:18080/users/123")
     *     .jsonBody(updatedUser)
     *     .put()) {
     *     // Consume the response.
     * }
     * }</pre>
     *
     * @return the HTTP response; the caller must close it
     * @throws UncheckedIOException if the request could not be executed
     */
    public Response put() throws UncheckedIOException {
        return execute(HttpMethod.PUT);
    }

    /**
     * Executes a PUT request and deserializes the response to the specified type.
     * PUT requests typically send data to the server to create or fully replace a resource.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User updatedUser = new User("John", "Smith");
     * User result = OkHttpRequest.url("http://localhost:18080/users/123")
     *     .jsonBody(updatedUser)
     *     .put(User.class);
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object
     * @return The deserialized response body
     * @throws UncheckedIOException if opening the connection, sending the request, or reading the response body fails
     * @throws HttpResponseException if the status code is not 2xx and resultClass is not okhttp3.Response.class
     */
    public <T> T put(final Class<T> resultClass) throws UncheckedIOException, HttpResponseException {
        return execute(HttpMethod.PUT, resultClass);
    }

    /**
     * Executes a PATCH request and returns the response.
     * PATCH requests typically send data to the server to partially update a resource.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> updates = Map.of("status", "active");
     * try (Response response = OkHttpRequest.url("http://localhost:18080/users/123")
     *     .jsonBody(updates)
     *     .patch()) {
     *     // Consume the response.
     * }
     * }</pre>
     *
     * @return the HTTP response; the caller must close it
     * @throws UncheckedIOException if the request could not be executed
     */
    public Response patch() throws UncheckedIOException {
        return execute(HttpMethod.PATCH);
    }

    /**
     * Executes a PATCH request and deserializes the response to the specified type.
     * PATCH requests typically send data to the server to partially update a resource.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> updates = Map.of("status", "active");
     * User result = OkHttpRequest.url("http://localhost:18080/users/123")
     *     .jsonBody(updates)
     *     .patch(User.class);
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object
     * @return The deserialized response body
     * @throws UncheckedIOException if opening the connection, sending the request, or reading the response body fails
     * @throws HttpResponseException if the status code is not 2xx and resultClass is not okhttp3.Response.class
     */
    public <T> T patch(final Class<T> resultClass) throws UncheckedIOException, HttpResponseException {
        return execute(HttpMethod.PATCH, resultClass);
    }

    /**
     * Executes a DELETE request and returns the response.
     * DELETE requests typically remove a resource from the server.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Response response = OkHttpRequest.url("http://localhost:18080/users/123")
     *         .delete()) {
     *     // Consume the response.
     * }
     * }</pre>
     *
     * @return the HTTP response; the caller must close it
     * @throws UncheckedIOException if the request could not be executed
     */
    public Response delete() throws UncheckedIOException {
        return execute(HttpMethod.DELETE);
    }

    /**
     * Executes a DELETE request and deserializes the response to the specified type.
     * DELETE requests typically remove a resource from the server.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DeleteResponse result = OkHttpRequest.url("http://localhost:18080/users/123")
     *     .delete(DeleteResponse.class);
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object
     * @return The deserialized response body
     * @throws UncheckedIOException if opening the connection, sending the request, or reading the response body fails
     * @throws HttpResponseException if the status code is not 2xx and resultClass is not okhttp3.Response.class
     */
    public <T> T delete(final Class<T> resultClass) throws UncheckedIOException, HttpResponseException {
        return execute(HttpMethod.DELETE, resultClass);
    }

    /**
     * Executes a HEAD request and returns the response.
     * HEAD requests are used to retrieve headers without the response body.
     * This is useful for checking if a resource exists or getting metadata without downloading the full content.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Response response = OkHttpRequest.url("http://localhost:18080/large-file")
     *         .head()) {
     *     // Check headers without downloading the entire file
     *     String contentLength = response.header("Content-Length");
     * }
     * }</pre>
     *
     * @return the HTTP response (with no body); the caller must close it
     * @throws IllegalArgumentException if a request body has been configured on this request (OkHttp
     *         forbids a body on HEAD)
     * @throws UncheckedIOException if the request could not be executed
     */
    public Response head() throws IllegalArgumentException, UncheckedIOException {
        return execute(HttpMethod.HEAD);
    }

    /**
     * Executes an HTTP request with the specified method and returns the response.
     * This is a low-level method that allows executing any HTTP method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Response response = OkHttpRequest.url("http://localhost:18080/resource")
     *         .execute(HttpMethod.GET)) {
     *     // Consume the response.
     * }
     * }</pre>
     *
     * @param httpMethod The HTTP method to use (GET, POST, PUT, PATCH, DELETE, HEAD)
     * @return the HTTP response; the caller must close it
     * @throws IllegalArgumentException if {@code httpMethod} is {@code null}, or a request body has been
     *         configured and {@code httpMethod} is GET or HEAD (OkHttp forbids a body on those methods)
     * @throws UncheckedIOException if the request could not be executed
     */
    @Beta
    public Response execute(final HttpMethod httpMethod) throws IllegalArgumentException, UncheckedIOException {
        return execute(httpMethod, Response.class);
    }

    /**
     * Executes an HTTP request with the specified method and deserializes the response to the specified type.
     * This is a low-level method that allows executing any HTTP method with automatic response deserialization.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = OkHttpRequest.url("http://localhost:18080/users/123")
     *     .execute(HttpMethod.GET, User.class);
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param httpMethod The HTTP method to use (GET, POST, PUT, PATCH, DELETE, HEAD)
     * @param resultClass The class of the expected response object. Must not be {@code null}.
     *                    Use {@link Response Response.class} to receive the raw OkHttp response, or
     *                    {@code Void.class} to discard the response body.
     * @return the deserialized response body, or {@code null} if {@code resultClass} is {@code Void.class}
     *         or the response carries no body. If {@code resultClass} is {@code Response.class}, the
     *         caller must close the returned response.
     * @throws IllegalArgumentException if {@code httpMethod} or {@code resultClass} is {@code null}, or
     *         {@code resultClass} is the abacus {@link HttpResponse} type (use OkHttp's {@code Response} class
     *         directly instead), or a request body has been configured and {@code httpMethod} is GET or HEAD
     *         (OkHttp forbids a body on those methods).
     * @throws UncheckedIOException if opening the connection, sending the request, or reading the response body fails
     * @throws HttpResponseException if the status code is not 2xx and resultClass is not okhttp3.Response.class
     */
    @Beta
    public <T> T execute(final HttpMethod httpMethod, final Class<T> resultClass) throws IllegalArgumentException, UncheckedIOException, HttpResponseException {
        N.checkArgNotNull(httpMethod, cs.httpMethod);
        N.checkArgNotNull(resultClass, cs.resultClass);
        N.checkArgument(!HttpResponse.class.equals(resultClass),
                "Return type cannot be HttpResponse. Use okhttp3.Response, or a body type such as String.class");

        final boolean returningResponse = Response.class.equals(resultClass);
        Response resp = null;
        boolean responseOwnershipTransferred = false;

        try {
            // The request as it was built here, not resp.request(): after a redirect the latter
            // describes the followed request, while the content metadata below must describe what
            // this call actually serialized.
            final Request request = createRequest(httpMethod);

            resp = execute(request);

            if (returningResponse) {
                // Ownership passes to the caller, who must close it (see the class javadoc).
                responseOwnershipTransferred = true;
                return (T) resp;
            }

            String contentType = request.header(HttpHeaders.Names.CONTENT_TYPE);

            if (contentType == null && request.body() != null && request.body().contentType() != null) {
                // OkHttp materialises Content-Type from the body only inside BridgeInterceptor, on the
                // network request - never on the built Request. The media type every body setter of
                // this class attaches is therefore invisible to request.header(..), which would leave
                // the request-derived format/charset fallback below dead for all of them.
                contentType = request.body().contentType().toString();
            }

            final String contentEncoding = request.header(HttpHeaders.Names.CONTENT_ENCODING);
            final ContentFormat requestContentFormat = HttpUtil.getContentFormat(contentType, contentEncoding);
            final Charset requestCharset = HttpUtil.getCharset(contentType);
            final Map<String, List<String>> respHeaders = resp.headers().toMultimap();
            final Charset respCharset = HttpUtil.getResponseCharset(respHeaders, requestCharset);
            final ContentFormat respContentFormat = HttpUtil.getResponseContentFormat(respHeaders, requestContentFormat);
            final ResponseBody respBody = resp.body();

            if (!resp.isSuccessful()) {
                // Only a bounded prefix of the error body is captured, so that an arbitrarily large
                // error page cannot be materialized into an exception message.
                throw new HttpResponseException(request.url().toString(), resp.code(), resp.message(), respHeaders,
                        readBoundedErrorBody(respBody, respContentFormat, respCharset));
            }

            if (resultClass.equals(Void.class) || respBody == null) {
                return null;
            }

            final InputStream is = HttpUtil.hasResponseBody(httpMethod.name(), resp.code()) ? HttpUtil.wrapInputStream(respBody.byteStream(), respContentFormat)
                    : N.emptyInputStream();

            try {
                if (resultClass.equals(String.class)) {
                    return (T) IOUtil.readAllToString(is, respCharset);
                } else if (byte[].class.equals(resultClass)) {
                    return (T) IOUtil.readAllBytes(is);
                } else {
                    if (respContentFormat == ContentFormat.KRYO && KRYO_PARSER != null) {
                        return KRYO_PARSER.deserialize(is, resultClass);
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
            } finally {
                IOUtil.closeQuietly(is);
            }
        } catch (final IOException e) {
            // Parity with com.landawn.abacus.http.HttpRequest and http.v2.HttpRequest: surface
            // I/O failures as an unchecked UncheckedIOException so callers are not forced into
            // try/catch on the fluent API.
            throw new UncheckedIOException(e);
        } finally {
            if (!responseOwnershipTransferred) {
                IOUtil.closeQuietly(resp);
            }
        }
    }

    /**
     * Reads at most {@link #MAX_ERROR_BODY_SIZE} bytes of an error response body.
     *
     * @param respBody the error response body, or {@code null} when there is none
     * @param respContentFormat the response content format, used to decompress the body
     * @param respCharset the charset to decode the captured bytes with
     * @return the decoded prefix of the error body; never {@code null}, empty if it cannot be read
     */
    private static String readBoundedErrorBody(final ResponseBody respBody, final ContentFormat respContentFormat, final Charset respCharset) {
        if (respBody == null) {
            return Strings.EMPTY;
        }

        InputStream errorStream = null;

        try {
            errorStream = HttpUtil.wrapInputStream(respBody.byteStream(), respContentFormat);

            return new String(errorStream.readNBytes(MAX_ERROR_BODY_SIZE), respCharset);
        } catch (final IOException | RuntimeException e) {
            // The status code is what matters here; a failure to read the error body must never
            // replace the HttpResponseException that is about to be thrown.
            return Strings.EMPTY;
        } finally {
            IOUtil.closeQuietly(errorStream);
        }
    }

    /**
     * @throws IOException if executing the OkHttp call fails because of a connection, timeout, cancellation, or response-read failure
     */
    private Response execute(final Request request) throws IOException {
        // A derived client shares the originating client's dispatcher and connection pool, so it
        // owns no resources of its own and there is nothing to release once the call completes.
        return (httpClientBuilder == null ? httpClient : httpClientBuilder.build()).newCall(request).execute();
    }

    private Request createRequest(final HttpMethod httpMethod) {
        final RequestBody requestBody = body == null && requiresRequestBody(httpMethod) ? RequestBody.create(N.EMPTY_BYTE_ARRAY, null) : body;

        if (query == null || (query instanceof String && Strings.isEmpty((String) query))) {
            if (httpUrl == null) {
                requestBuilder.url(HttpUrl.get(url));
            } else {
                requestBuilder.url(httpUrl);
            }
        } else {
            if (httpUrl == null) {
                // Explicit UTF-8: the 2-arg overload uses the PLATFORM default charset,
                // mis-encoding non-ASCII query values on non-UTF-8 JVMs.
                requestBuilder.url(HttpUrl.get(URLEncodedUtil.encode(url, query, HttpUtil.DEFAULT_CHARSET)));
            } else {
                requestBuilder.url(HttpUrl.get(URLEncodedUtil.encode(httpUrl.toString(), query, HttpUtil.DEFAULT_CHARSET)));
            }
        }

        return requestBuilder.method(httpMethod.name(), requestBody).build();
    }

    private static boolean requiresRequestBody(final HttpMethod httpMethod) {
        return httpMethod == HttpMethod.POST || httpMethod == HttpMethod.PUT || httpMethod == HttpMethod.PATCH;
    }

    /**
     * Executes a GET request asynchronously using the default executor.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<Response> future = OkHttpRequest.url("http://localhost:18080/users")
     *     .asyncGet();
     *
     * future.getThenAccept(response -> {
     *     try (response) {
     *         if (response.isSuccessful()) {
     *             // Process response
     *         }
     *     }
     * });
     * }</pre>
     *
     * @return a ContinuableFuture that will complete with the HTTP response; the caller must close the completed response
     */
    public ContinuableFuture<Response> asyncGet() {
        return asyncGet(HttpUtil.DEFAULT_EXECUTOR);
    }

    private static <T> ContinuableFuture<T> submitAsync(final Callable<T> action, final Executor executor) {
        final FutureTask<T> task = new FutureTask<>(action) {
            @Override
            protected void set(final T value) {
                super.set(value);

                // FutureTask atomically arbitrates publication against cancellation. Once a value
                // is published cancellation cannot succeed; if cancellation won, no caller owns it.
                if (isCancelled() && value instanceof Response response) {
                    try {
                        response.close();
                    } catch (final Exception | Error e) {
                        // The future is already cancelled; orphan cleanup cannot replace that outcome.
                    }
                }
            }
        };

        executor.execute(task);
        return ContinuableFuture.wrap(task).thenUse(executor);
    }

    /**
     * Executes a GET request asynchronously using the specified executor.
     * The request is executed on the provided executor and returns immediately with a ContinuableFuture.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Executor executor = ForkJoinPool.commonPool();
     * ContinuableFuture<Response> future = OkHttpRequest.url("http://localhost:18080/users")
     *         .asyncGet(executor);
     * try (Response response = future.get()) {   // blocks for the result when executed (network)
     *     // Consume the response.
     * }
     * }</pre>
     *
     * @param executor the executor to use for the asynchronous operation
     * @return a ContinuableFuture that will complete with the HTTP response when the request finishes; the caller must close the completed response
     * @throws IllegalArgumentException if {@code executor} is {@code null}.
     */
    public ContinuableFuture<Response> asyncGet(final Executor executor) throws IllegalArgumentException {
        N.checkArgNotNull(executor, cs.executor);

        return submitAsync(this::get, executor);
    }

    /**
     * Executes a GET request asynchronously and deserializes the response to the specified type.
     * Uses the default executor.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<String> future = OkHttpRequest.url("http://localhost:18080/users")
     *     .asyncGet(String.class);
     *
     * future.getThenAccept(response -> {
     *     // Process response
     * });
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object
     * @return A ContinuableFuture that will complete with the deserialized response body
     */
    public <T> ContinuableFuture<T> asyncGet(final Class<T> resultClass) {
        return asyncGet(resultClass, HttpUtil.DEFAULT_EXECUTOR);
    }

    /**
     * Executes a GET request asynchronously using the specified executor and deserializes the response to the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Executor executor = ForkJoinPool.commonPool();
     * ContinuableFuture<String> future = OkHttpRequest.url("http://localhost:18080/users")
     *         .asyncGet(String.class, executor);
     * // String body = future.get();   // blocks for the result when executed (network)
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object
     * @param executor The executor to use for the asynchronous operation
     * @return A ContinuableFuture that will complete with the deserialized response body
     * @throws IllegalArgumentException if {@code executor} is {@code null}.
     */
    public <T> ContinuableFuture<T> asyncGet(final Class<T> resultClass, final Executor executor) throws IllegalArgumentException {
        N.checkArgNotNull(executor, cs.executor);

        return submitAsync(() -> get(resultClass), executor);
    }

    /**
     * Executes a POST request asynchronously using the default executor.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<Response> future = OkHttpRequest.url("http://localhost:18080/users")
     *         .jsonBody("{\"name\":\"John\"}")
     *         .asyncPost();
     * try (Response response = future.get()) {   // blocks for the result when executed (network)
     *     // Consume the response.
     * }
     * }</pre>
     *
     * @return a ContinuableFuture that will complete with the HTTP response; the caller must close the completed response
     */
    public ContinuableFuture<Response> asyncPost() {
        return asyncPost(HttpUtil.DEFAULT_EXECUTOR);
    }

    /**
     * Executes a POST request asynchronously using the specified executor.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Executor executor = ForkJoinPool.commonPool();
     * ContinuableFuture<Response> future = OkHttpRequest.url("http://localhost:18080/users")
     *         .jsonBody("{\"name\":\"John\"}")
     *         .asyncPost(executor);
     * try (Response response = future.get()) {   // blocks for the result when executed (network)
     *     // Consume the response.
     * }
     * }</pre>
     *
     * @param executor The executor to use for the asynchronous operation
     * @return a ContinuableFuture that will complete with the HTTP response; the caller must close the completed response
     * @throws IllegalArgumentException if {@code executor} is {@code null}.
     */
    public ContinuableFuture<Response> asyncPost(final Executor executor) throws IllegalArgumentException {
        N.checkArgNotNull(executor, cs.executor);

        return submitAsync(this::post, executor);
    }

    /**
     * Executes a POST request asynchronously and deserializes the response to the specified type.
     * Uses the default executor.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<User> future = OkHttpRequest.url("http://localhost:18080/users")
     *         .jsonBody("{\"name\":\"John\"}")
     *         .asyncPost(User.class);
     * // User user = future.get();   // blocks for the result when executed (network)
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object
     * @return A ContinuableFuture that will complete with the deserialized response body
     */
    public <T> ContinuableFuture<T> asyncPost(final Class<T> resultClass) {
        return asyncPost(resultClass, HttpUtil.DEFAULT_EXECUTOR);
    }

    /**
     * Executes a POST request asynchronously using the specified executor and deserializes the response to the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Executor executor = ForkJoinPool.commonPool();
     * ContinuableFuture<User> future = OkHttpRequest.url("http://localhost:18080/users")
     *         .jsonBody("{\"name\":\"John\"}")
     *         .asyncPost(User.class, executor);
     * // User user = future.get();   // blocks for the result when executed (network)
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object
     * @param executor The executor to use for the asynchronous operation
     * @return A ContinuableFuture that will complete with the deserialized response body
     * @throws IllegalArgumentException if {@code executor} is {@code null}.
     */
    public <T> ContinuableFuture<T> asyncPost(final Class<T> resultClass, final Executor executor) throws IllegalArgumentException {
        N.checkArgNotNull(executor, cs.executor);

        return submitAsync(() -> post(resultClass), executor);
    }

    /**
     * Executes a PUT request asynchronously using the default executor.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<Response> future = OkHttpRequest.url("http://localhost:18080/users/123")
     *         .jsonBody("{\"name\":\"John\"}")
     *         .asyncPut();
     * try (Response response = future.get()) {   // blocks for the result when executed (network)
     *     // Consume the response.
     * }
     * }</pre>
     *
     * @return a ContinuableFuture that will complete with the HTTP response; the caller must close the completed response
     */
    public ContinuableFuture<Response> asyncPut() {
        return asyncPut(HttpUtil.DEFAULT_EXECUTOR);
    }

    /**
     * Executes a PUT request asynchronously using the specified executor.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Executor executor = ForkJoinPool.commonPool();
     * ContinuableFuture<Response> future = OkHttpRequest.url("http://localhost:18080/users/123")
     *         .jsonBody("{\"name\":\"John\"}")
     *         .asyncPut(executor);
     * try (Response response = future.get()) {   // blocks for the result when executed (network)
     *     // Consume the response.
     * }
     * }</pre>
     *
     * @param executor The executor to use for the asynchronous operation
     * @return a ContinuableFuture that will complete with the HTTP response; the caller must close the completed response
     * @throws IllegalArgumentException if {@code executor} is {@code null}.
     */
    public ContinuableFuture<Response> asyncPut(final Executor executor) throws IllegalArgumentException {
        N.checkArgNotNull(executor, cs.executor);

        return submitAsync(this::put, executor);
    }

    /**
     * Executes a PUT request asynchronously and deserializes the response to the specified type.
     * Uses the default executor.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<User> future = OkHttpRequest.url("http://localhost:18080/users/123")
     *         .jsonBody("{\"name\":\"John\"}")
     *         .asyncPut(User.class);
     * // User user = future.get();   // blocks for the result when executed (network)
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object
     * @return A ContinuableFuture that will complete with the deserialized response body
     */
    public <T> ContinuableFuture<T> asyncPut(final Class<T> resultClass) {
        return asyncPut(resultClass, HttpUtil.DEFAULT_EXECUTOR);
    }

    /**
     * Executes a PUT request asynchronously using the specified executor and deserializes the response to the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Executor executor = ForkJoinPool.commonPool();
     * ContinuableFuture<User> future = OkHttpRequest.url("http://localhost:18080/users/123")
     *         .jsonBody("{\"name\":\"John\"}")
     *         .asyncPut(User.class, executor);
     * // User user = future.get();   // blocks for the result when executed (network)
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object
     * @param executor The executor to use for the asynchronous operation
     * @return A ContinuableFuture that will complete with the deserialized response body
     * @throws IllegalArgumentException if {@code executor} is {@code null}.
     */
    public <T> ContinuableFuture<T> asyncPut(final Class<T> resultClass, final Executor executor) throws IllegalArgumentException {
        N.checkArgNotNull(executor, cs.executor);

        return submitAsync(() -> put(resultClass), executor);
    }

    /**
     * Executes a PATCH request asynchronously using the default executor.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<Response> future = OkHttpRequest.url("http://localhost:18080/users/123")
     *         .jsonBody("{\"status\":\"active\"}")
     *         .asyncPatch();
     * try (Response response = future.get()) {   // blocks for the result when executed (network)
     *     // Consume the response.
     * }
     * }</pre>
     *
     * @return a ContinuableFuture that will complete with the HTTP response; the caller must close the completed response
     */
    public ContinuableFuture<Response> asyncPatch() {
        return asyncPatch(HttpUtil.DEFAULT_EXECUTOR);
    }

    /**
     * Executes a PATCH request asynchronously using the specified executor.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Executor executor = ForkJoinPool.commonPool();
     * ContinuableFuture<Response> future = OkHttpRequest.url("http://localhost:18080/users/123")
     *         .jsonBody("{\"status\":\"active\"}")
     *         .asyncPatch(executor);
     * try (Response response = future.get()) {   // blocks for the result when executed (network)
     *     // Consume the response.
     * }
     * }</pre>
     *
     * @param executor The executor to use for the asynchronous operation
     * @return a ContinuableFuture that will complete with the HTTP response; the caller must close the completed response
     * @throws IllegalArgumentException if {@code executor} is {@code null}.
     */
    public ContinuableFuture<Response> asyncPatch(final Executor executor) throws IllegalArgumentException {
        N.checkArgNotNull(executor, cs.executor);

        return submitAsync(this::patch, executor);
    }

    /**
     * Executes a PATCH request asynchronously and deserializes the response to the specified type.
     * Uses the default executor.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<User> future = OkHttpRequest.url("http://localhost:18080/users/123")
     *         .jsonBody("{\"status\":\"active\"}")
     *         .asyncPatch(User.class);
     * // User user = future.get();   // blocks for the result when executed (network)
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object
     * @return A ContinuableFuture that will complete with the deserialized response body
     */
    public <T> ContinuableFuture<T> asyncPatch(final Class<T> resultClass) {
        return asyncPatch(resultClass, HttpUtil.DEFAULT_EXECUTOR);
    }

    /**
     * Executes a PATCH request asynchronously using the specified executor and deserializes the response to the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Executor executor = ForkJoinPool.commonPool();
     * ContinuableFuture<User> future = OkHttpRequest.url("http://localhost:18080/users/123")
     *         .jsonBody("{\"status\":\"active\"}")
     *         .asyncPatch(User.class, executor);
     * // User user = future.get();   // blocks for the result when executed (network)
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object
     * @param executor The executor to use for the asynchronous operation
     * @return A ContinuableFuture that will complete with the deserialized response body
     * @throws IllegalArgumentException if {@code executor} is {@code null}.
     */
    public <T> ContinuableFuture<T> asyncPatch(final Class<T> resultClass, final Executor executor) throws IllegalArgumentException {
        N.checkArgNotNull(executor, cs.executor);

        return submitAsync(() -> patch(resultClass), executor);
    }

    /**
     * Executes a DELETE request asynchronously using the default executor.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<Response> future = OkHttpRequest.url("http://localhost:18080/users/123")
     *         .asyncDelete();
     * try (Response response = future.get()) {   // blocks for the result when executed (network)
     *     // Consume the response.
     * }
     * }</pre>
     *
     * @return a ContinuableFuture that will complete with the HTTP response; the caller must close the completed response
     */
    public ContinuableFuture<Response> asyncDelete() {
        return asyncDelete(HttpUtil.DEFAULT_EXECUTOR);
    }

    /**
     * Executes a DELETE request asynchronously using the specified executor.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Executor executor = ForkJoinPool.commonPool();
     * ContinuableFuture<Response> future = OkHttpRequest.url("http://localhost:18080/users/123")
     *         .asyncDelete(executor);
     * try (Response response = future.get()) {   // blocks for the result when executed (network)
     *     // Consume the response.
     * }
     * }</pre>
     *
     * @param executor The executor to use for the asynchronous operation
     * @return a ContinuableFuture that will complete with the HTTP response; the caller must close the completed response
     * @throws IllegalArgumentException if {@code executor} is {@code null}.
     */
    public ContinuableFuture<Response> asyncDelete(final Executor executor) throws IllegalArgumentException {
        N.checkArgNotNull(executor, cs.executor);

        return submitAsync(this::delete, executor);
    }

    /**
     * Executes a DELETE request asynchronously and deserializes the response to the specified type.
     * Uses the default executor.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<DeleteResponse> future = OkHttpRequest.url("http://localhost:18080/users/123")
     *         .asyncDelete(DeleteResponse.class);
     * // DeleteResponse result = future.get();   // blocks for the result when executed (network)
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object
     * @return A ContinuableFuture that will complete with the deserialized response body
     */
    public <T> ContinuableFuture<T> asyncDelete(final Class<T> resultClass) {
        return asyncDelete(resultClass, HttpUtil.DEFAULT_EXECUTOR);
    }

    /**
     * Executes a DELETE request asynchronously using the specified executor and deserializes the response to the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Executor executor = ForkJoinPool.commonPool();
     * ContinuableFuture<DeleteResponse> future = OkHttpRequest.url("http://localhost:18080/users/123")
     *         .asyncDelete(DeleteResponse.class, executor);
     * // DeleteResponse result = future.get();   // blocks for the result when executed (network)
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object
     * @param executor The executor to use for the asynchronous operation
     * @return A ContinuableFuture that will complete with the deserialized response body
     * @throws IllegalArgumentException if {@code executor} is {@code null}.
     */
    public <T> ContinuableFuture<T> asyncDelete(final Class<T> resultClass, final Executor executor) throws IllegalArgumentException {
        N.checkArgNotNull(executor, cs.executor);

        return submitAsync(() -> delete(resultClass), executor);
    }

    /**
     * Executes a HEAD request asynchronously using the default executor.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<Response> future = OkHttpRequest.url("http://localhost:18080/large-file")
     *         .asyncHead();
     * try (Response response = future.get()) {   // blocks for the headers when executed (network)
     *     // Inspect the response headers.
     * }
     * }</pre>
     *
     * @return a ContinuableFuture that will complete with the HTTP response; the caller must close the completed response
     */
    public ContinuableFuture<Response> asyncHead() {
        return asyncHead(HttpUtil.DEFAULT_EXECUTOR);
    }

    /**
     * Executes a HEAD request asynchronously using the specified executor.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Executor executor = ForkJoinPool.commonPool();
     * ContinuableFuture<Response> future = OkHttpRequest.url("http://localhost:18080/large-file")
     *         .asyncHead(executor);
     * try (Response response = future.get()) {   // blocks for the headers when executed (network)
     *     // Inspect the response headers.
     * }
     * }</pre>
     *
     * @param executor The executor to use for the asynchronous operation
     * @return a ContinuableFuture that will complete with the HTTP response; the caller must close the completed response
     * @throws IllegalArgumentException if {@code executor} is {@code null}.
     */
    public ContinuableFuture<Response> asyncHead(final Executor executor) throws IllegalArgumentException {
        N.checkArgNotNull(executor, cs.executor);

        return submitAsync(this::head, executor);
    }

    /**
     * Executes an HTTP request asynchronously with the specified method using the default executor.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<Response> future = OkHttpRequest.url("http://localhost:18080/resource")
     *         .asyncExecute(HttpMethod.GET);
     * try (Response response = future.get()) {   // blocks for the result when executed (network)
     *     // Consume the response.
     * }
     * }</pre>
     *
     * @param httpMethod The HTTP method to use (GET, POST, PUT, PATCH, DELETE, HEAD)
     * @return a ContinuableFuture that will complete with the HTTP response; the caller must close the completed response
     */
    @Beta
    public ContinuableFuture<Response> asyncExecute(final HttpMethod httpMethod) {
        return asyncExecute(httpMethod, HttpUtil.DEFAULT_EXECUTOR);
    }

    /**
     * Executes an HTTP request asynchronously with the specified method using the specified executor.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Executor executor = ForkJoinPool.commonPool();
     * ContinuableFuture<Response> future = OkHttpRequest.url("http://localhost:18080/resource")
     *         .asyncExecute(HttpMethod.GET, executor);
     * try (Response response = future.get()) {   // blocks for the result when executed (network)
     *     // Consume the response.
     * }
     * }</pre>
     *
     * @param httpMethod The HTTP method to use (GET, POST, PUT, PATCH, DELETE, HEAD)
     * @param executor The executor to use for the asynchronous operation
     * @return a ContinuableFuture that will complete with the HTTP response; the caller must close the completed response
     * @throws IllegalArgumentException if {@code executor} is {@code null}.
     */
    @Beta
    public ContinuableFuture<Response> asyncExecute(final HttpMethod httpMethod, final Executor executor) throws IllegalArgumentException {
        N.checkArgNotNull(executor, cs.executor);

        return submitAsync(() -> execute(httpMethod), executor);
    }

    /**
     * Executes an HTTP request asynchronously with the specified method and deserializes the response to the specified type.
     * Uses the default executor.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<User> future = OkHttpRequest.url("http://localhost:18080/users/123")
     *         .asyncExecute(HttpMethod.GET, User.class);
     * // User user = future.get();   // blocks for the result when executed (network)
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param httpMethod The HTTP method to use (GET, POST, PUT, PATCH, DELETE, HEAD)
     * @param resultClass The class of the expected response object
     * @return A ContinuableFuture that will complete with the deserialized response body
     */
    @Beta
    public <T> ContinuableFuture<T> asyncExecute(final HttpMethod httpMethod, final Class<T> resultClass) {
        return asyncExecute(httpMethod, resultClass, HttpUtil.DEFAULT_EXECUTOR);
    }

    /**
     * Executes an HTTP request asynchronously with the specified method using the specified executor and deserializes the response to the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Executor executor = ForkJoinPool.commonPool();
     * ContinuableFuture<User> future = OkHttpRequest.url("http://localhost:18080/users/123")
     *         .asyncExecute(HttpMethod.GET, User.class, executor);
     * // User user = future.get();   // blocks for the result when executed (network)
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param httpMethod The HTTP method to use (GET, POST, PUT, PATCH, DELETE, HEAD)
     * @param resultClass The class of the expected response object
     * @param executor The executor to use for the asynchronous operation
     * @return A ContinuableFuture that will complete with the deserialized response body
     * @throws IllegalArgumentException if {@code executor} is {@code null}.
     */
    @Beta
    public <T> ContinuableFuture<T> asyncExecute(final HttpMethod httpMethod, final Class<T> resultClass, final Executor executor)
            throws IllegalArgumentException {
        N.checkArgNotNull(executor, cs.executor);

        return submitAsync(() -> execute(httpMethod, resultClass), executor);
    }
}
