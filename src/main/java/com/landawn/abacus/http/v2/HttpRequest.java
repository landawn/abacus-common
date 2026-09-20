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

import java.io.ByteArrayInputStream;
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
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.net.http.HttpResponse.PushPromiseHandler;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.BaseStream;

import javax.net.ssl.SSLSession;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.exception.HttpResponseException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.http.ContentFormat;
import com.landawn.abacus.http.HttpHeaders;
import com.landawn.abacus.http.HttpMethod;
import com.landawn.abacus.http.HttpUtil;
import com.landawn.abacus.util.Charsets;
import com.landawn.abacus.util.ExceptionUtil;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.URLEncodedUtil;
import com.landawn.abacus.util.cs;

/**
 * A fluent HTTP request builder and executor based on Java 11+ HttpClient.
 * This class provides a convenient API for building and executing HTTP requests with various features
 * such as headers, query parameters, request bodies, authentication, and timeouts.
 *
 * <p>This implementation uses the modern Java {@link java.net.http.HttpClient} introduced in Java 11,
 * providing support for HTTP/2.</p>
 *
 * <p><b>Thread-safety:</b> Instances of this class are mutable builders and are not thread-safe.
 * Each request should be configured and executed from a single thread; the underlying
 * {@code java.net.http.HttpClient} is itself thread-safe and is reused across calls when possible.</p>
 *
 * <p><b>HTTP errors:</b> overloads accepting a result {@code Class} throw {@link HttpResponseException}
 * for non-2xx responses, including when the result class is {@code null} or {@code Void.class}.
 * The exception retains the status, headers, response URI and at most
 * {@link HttpUtil#MAX_ERROR_BODY_SIZE} decoded body bytes. The reason phrase is {@code null},
 * because the JDK response API does not expose one. Missing or unreadable error bodies produce
 * an empty captured body; a multibyte character split at the byte limit is replaced when decoded.
 * The built-in String response overloads throw HttpResponseException if an error body cannot be decoded,
 * retaining its raw byte prefix and decoding failure through {@link HttpResponseException#rawResponseBody()}
 * and {@link HttpResponseException#responseBodyDecodingFailure()}. Custom body handlers retain their own behavior.</p>
 *
 * <p><b>Streaming responses:</b> close returned InputStream bodies to release their request-owned clients.
 * For Java stream and Flow.Publisher bodies (including {@code BodyHandlers.ofLines()} and
 * {@code BodyHandlers.ofPublisher()}), an owned client begins orderly, nonblocking shutdown before
 * delivery. Consume or close streams; consume publishers or subscribe and cancel their subscription
 * to release the exchange. Arbitrary custom containers of streaming resources retain their handler's
 * cleanup responsibilities. Caller-owned clients are left open. An {@code InputStream} obtained through a
 * result {@code Class} ({@code get(InputStream.class)}, {@code asyncGet(InputStream.class)}, or a supertype
 * such as {@code Object.class}) is delivered <i>decoded</i>: a {@code Content-Encoding} the library
 * understands (gzip, br, snappy, lz4) is removed lazily on the first read, so a corrupt encoding surfaces
 * from that read rather than from the call that returned the stream. Such streams must still be closed.</p>
 *
 * <p><b>Redirects:</b> the shared default client and every client this class creates itself (the
 * timeout-taking {@code url(..)} factories and fluent client-level configuration such as
 * {@link #connectTimeout(Duration)}) follow redirects with {@link HttpClient.Redirect#NORMAL}, like the
 * {@code com.landawn.abacus.http.HttpRequest} and {@code OkHttpRequest} builders. A caller-supplied client
 * ({@code create(.., HttpClient)}) keeps its own policy, including when fluent configuration copies it into
 * a replacement client; the JDK default for such a client is {@link HttpClient.Redirect#NEVER}, in which case
 * the overloads accepting a result {@code Class} throw {@link HttpResponseException} for a 3xx response.</p>
 *
 * <p><b>Asynchronous cancellation:</b> cancelling a future returned by an {@code async*} method does not
 * abort the in-flight exchange. The request runs to completion, after which its body and any owned client
 * are released; the cancelled future simply never observes the result.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Simple GET request
 * HttpResponse<String> response = HttpRequest.url("http://localhost:18080/users")
 *     .header("Accept", "application/json")
 *     .get();
 *
 * // POST request with JSON body
 * User user = new User("John", "Doe");
 * User createdUser = HttpRequest.url("http://localhost:18080/users")
 *     .jsonBody(user)
 *     .post(User.class);
 *
 * // Asynchronous request
 * CompletableFuture<String> future = HttpRequest.url("http://localhost:18080/data")
 *     .asyncGet(String.class);
 * }</pre>
 *
 * @see URLEncodedUtil
 * @see HttpHeaders
 * @see HttpMethod
 */
public final class HttpRequest {

    private static final HttpClient DEFAULT_HTTP_CLIENT = newClientBuilder().build();

    private final String url;
    private final URI uri;
    private final HttpClient httpClient;
    private final java.net.http.HttpRequest.Builder requestBuilder;

    private Object query;

    private HttpClient.Builder clientBuilder;
    private BodyPublisher bodyPublisher;

    private boolean requireNewClient = false;

    private boolean closeHttpClientAfterExecution = false;

    /**
     * Creates a request with the supplied target, client configuration and request builder.
     *
     * @param url the URL text; may be null or empty when {@code uri} is supplied
     * @param uri the target URI; may be null when {@code url} is nonempty
     * @param httpClient the HTTP client, or null to select a default or newly built client
     * @param clientBuilder the optional builder for a new HTTP client
     * @param requestBuilder the request builder; must not be null
     * @throws IllegalArgumentException if {@code url} is null or empty and {@code uri} is null, or if {@code requestBuilder} is null
     */
    HttpRequest(final String url, final URI uri, final HttpClient httpClient, final HttpClient.Builder clientBuilder,
            final java.net.http.HttpRequest.Builder requestBuilder) throws IllegalArgumentException {
        N.checkArgument(!(Strings.isEmpty(url) && uri == null), "'uri' or 'url' cannot be null or empty");
        N.checkArgNotNull(requestBuilder, cs.requestBuilder);

        this.url = url;
        this.uri = uri;
        this.httpClient = httpClient;
        this.clientBuilder = clientBuilder;
        this.requestBuilder = requestBuilder;
    }

    /**
     * Creates a new HttpRequest instance with the specified URL and HTTP client.
     *
     * <p>Note: only non-emptiness of {@code url} is checked here. Scheme/host validity is not
     * verified at construction; an {@link IllegalArgumentException} may be thrown later, when the
     * request is executed, if it is not a valid {@code http} or {@code https} URI.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpClient client = HttpClient.newHttpClient();
     * HttpRequest request = HttpRequest.create("http://localhost:18080/users", client);
     * // configure, then execute (network call happens only on get()/post()/etc.):
     * // String body = request.get(String.class);
     * }</pre>
     *
     * @param url the URL string for the request
     * @param httpClient the HttpClient to use for executing the request; {@code null} selects the shared
     *        default client (which follows redirects, see the class documentation)
     * @return a new HttpRequest instance
     * @throws IllegalArgumentException if {@code url} is {@code null} or empty.
     */
    public static HttpRequest create(final String url, final HttpClient httpClient) throws IllegalArgumentException {
        return new HttpRequest(url, null, httpClient, null, java.net.http.HttpRequest.newBuilder()).closeHttpClientAfterExecution(false);
    }

    /**
     * Creates a new HttpRequest instance with the specified URL and HTTP client.
     *
     * <p>Note: the URL is not validated here. An {@link IllegalArgumentException} may be
     * thrown later, when the request is executed, if it is not a valid {@code http} or
     * {@code https} URI.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpClient client = HttpClient.newHttpClient();
     * URL url = new URL("http://localhost:18080/users");
     * HttpRequest request = HttpRequest.create(url, client);
     * // String body = request.get(String.class);  // network call happens here (when executed)
     * }</pre>
     *
     * @param url the URL object for the request
     * @param httpClient the HttpClient to use for executing the request; {@code null} selects the shared
     *        default client (which follows redirects, see the class documentation)
     * @return a new HttpRequest instance
     * @throws IllegalArgumentException if {@code url} is {@code null}.
     */
    public static HttpRequest create(final URL url, final HttpClient httpClient) throws IllegalArgumentException {
        N.checkArgNotNull(url, cs.url);

        return new HttpRequest(url.toString(), null, httpClient, null, java.net.http.HttpRequest.newBuilder()).closeHttpClientAfterExecution(false);
    }

    /**
     * Creates a new HttpRequest instance with the specified URI and HTTP client.
     *
     * <p>Note: only non-nullity of {@code uri} is checked here. Scheme/host validity is not
     * verified at construction; an {@link IllegalArgumentException} may be thrown later, when the
     * request is executed, if its scheme is not {@code http} or {@code https}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpClient client = HttpClient.newHttpClient();
     * URI uri = URI.create("http://localhost:18080/users");
     * HttpRequest request = HttpRequest.create(uri, client);
     * // String body = request.get(String.class);  // network call happens here (when executed)
     * }</pre>
     *
     * @param uri the URI object for the request
     * @param httpClient the HttpClient to use for executing the request; {@code null} selects the shared
     *        default client (which follows redirects, see the class documentation)
     * @return a new HttpRequest instance
     * @throws IllegalArgumentException if {@code uri} is {@code null}.
     */
    public static HttpRequest create(final URI uri, final HttpClient httpClient) throws IllegalArgumentException {
        return new HttpRequest(null, uri, httpClient, null, java.net.http.HttpRequest.newBuilder()).closeHttpClientAfterExecution(false);
    }

    /**
     * Creates a new HttpRequest instance with the specified URL using the default HTTP client.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest request = HttpRequest.url("http://localhost:18080/users");
     * }</pre>
     *
     * <p>Note: only non-emptiness of {@code url} is checked here. Scheme/host validity is not
     * verified at construction; an {@link IllegalArgumentException} may be thrown later, when the
     * request is executed, if it is not a valid {@code http} or {@code https} URI.</p>
     *
     * @param url the URL string for the request
     * @return a new HttpRequest instance
     * @throws IllegalArgumentException if {@code url} is {@code null} or empty.
     */
    public static HttpRequest url(final String url) throws IllegalArgumentException {
        return new HttpRequest(url, null, DEFAULT_HTTP_CLIENT, null, java.net.http.HttpRequest.newBuilder()).closeHttpClientAfterExecution(false);
    }

    /**
     * Creates a new HttpRequest instance with the specified URL and timeout settings.
     * A new HTTP client is created with the specified timeouts and will be closed after execution.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest request = HttpRequest.url("http://localhost:18080/data", 5000, 30000);
     * }</pre>
     *
     * @param url the URL string for the request
     * @param connectTimeoutInMillis the connection timeout in milliseconds; {@code 0} means no connect timeout
     *        (the JDK default, unbounded)
     * @param readTimeoutInMillis the maximum duration allowed for the response, in milliseconds; {@code 0} means
     *        no response timeout (the JDK default, unbounded)
     * @return a new HttpRequest instance
     * @throws IllegalArgumentException if {@code url} is {@code null} or empty, or either timeout is negative.
     */
    public static HttpRequest url(final String url, final long connectTimeoutInMillis, final long readTimeoutInMillis) throws IllegalArgumentException {
        return new HttpRequest(url, null, null, withConnectTimeout(newClientBuilder(), connectTimeoutInMillis),
                withReadTimeout(java.net.http.HttpRequest.newBuilder(), readTimeoutInMillis)).closeHttpClientAfterExecution(true);
    }

    /**
     * Creates a new HttpRequest instance with the specified URL using the default HTTP client.
     *
     * <p>Note: the URL is not validated here. An {@link IllegalArgumentException} may be
     * thrown later, when the request is executed, if it is not a valid {@code http} or
     * {@code https} URI.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * URL url = new URL("http://localhost:18080/users");
     * HttpRequest request = HttpRequest.url(url);  // uses the default HttpClient
     * // String body = request.get(String.class);  // network call happens here (when executed)
     * }</pre>
     *
     * @param url the URL object for the request
     * @return a new HttpRequest instance
     * @throws IllegalArgumentException if {@code url} is {@code null}.
     */
    public static HttpRequest url(final URL url) throws IllegalArgumentException {
        N.checkArgNotNull(url, cs.url);

        return new HttpRequest(url.toString(), null, DEFAULT_HTTP_CLIENT, null, java.net.http.HttpRequest.newBuilder()).closeHttpClientAfterExecution(false);
    }

    /**
     * Creates a new HttpRequest instance with the specified URL and timeout settings.
     * A new HTTP client is created with the specified timeouts and will be closed after execution.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * URL url = new URL("http://localhost:18080/data");
     * HttpRequest request = HttpRequest.url(url, 5000, 30000);  // 5s connect, 30s response timeout
     * // String body = request.get(String.class);  // network call happens here (when executed)
     * }</pre>
     *
     * @param url the URL object for the request
     * @param connectTimeoutInMillis the connection timeout in milliseconds; {@code 0} means no connect timeout
     *        (the JDK default, unbounded)
     * @param readTimeoutInMillis the maximum duration allowed for the response, in milliseconds; {@code 0} means
     *        no response timeout (the JDK default, unbounded)
     * @return a new HttpRequest instance
     * @throws IllegalArgumentException if {@code url} is {@code null} or either timeout is negative.
     */
    public static HttpRequest url(final URL url, final long connectTimeoutInMillis, final long readTimeoutInMillis) throws IllegalArgumentException {
        N.checkArgNotNull(url, cs.url);

        return new HttpRequest(url.toString(), null, null, withConnectTimeout(newClientBuilder(), connectTimeoutInMillis),
                withReadTimeout(java.net.http.HttpRequest.newBuilder(), readTimeoutInMillis)).closeHttpClientAfterExecution(true);
    }

    /**
     * Creates a new HttpRequest instance with the specified URI using the default HTTP client.
     *
     * <p>Note: only non-nullity of {@code uri} is checked here. Scheme/host validity is not
     * verified at construction; an {@link IllegalArgumentException} may be thrown later, when the
     * request is executed, if its scheme is not {@code http} or {@code https}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * URI uri = URI.create("http://localhost:18080/users");
     * HttpRequest request = HttpRequest.url(uri);  // uses the default HttpClient
     * // String body = request.get(String.class);  // network call happens here (when executed)
     * }</pre>
     *
     * @param uri the URI object for the request
     * @return a new HttpRequest instance
     * @throws IllegalArgumentException if {@code uri} is {@code null}.
     */
    public static HttpRequest url(final URI uri) throws IllegalArgumentException {
        return new HttpRequest(null, uri, DEFAULT_HTTP_CLIENT, null, java.net.http.HttpRequest.newBuilder()).closeHttpClientAfterExecution(false);
    }

    /**
     * Creates a new HttpRequest instance with the specified URI and timeout settings.
     * A new HTTP client is created with the specified timeouts and will be closed after execution.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * URI uri = URI.create("http://localhost:18080/data");
     * HttpRequest request = HttpRequest.url(uri, 5000, 30000);  // 5s connect, 30s response timeout
     * // String body = request.get(String.class);  // network call happens here (when executed)
     * }</pre>
     *
     * @param uri the URI object for the request
     * @param connectTimeoutInMillis the connection timeout in milliseconds; {@code 0} means no connect timeout
     *        (the JDK default, unbounded)
     * @param readTimeoutInMillis the maximum duration allowed for the response, in milliseconds; {@code 0} means
     *        no response timeout (the JDK default, unbounded)
     * @return a new HttpRequest instance
     * @throws IllegalArgumentException if {@code uri} is {@code null}, or either timeout is negative.
     */
    public static HttpRequest url(final URI uri, final long connectTimeoutInMillis, final long readTimeoutInMillis) throws IllegalArgumentException {
        return new HttpRequest(null, uri, null, withConnectTimeout(newClientBuilder(), connectTimeoutInMillis),
                withReadTimeout(java.net.http.HttpRequest.newBuilder(), readTimeoutInMillis)).closeHttpClientAfterExecution(true);
    }

    /**
     * Records whether the {@link HttpClient} used to run this request is owned by the request and must
     * therefore be closed once execution finishes. Clients created by the timeout-taking factory
     * methods and by fluent client-level configuration are owned; a caller-supplied client is not.
     *
     * @param shouldClose {@code true} to close the client after this request completes
     * @return this HttpRequest instance for method chaining
     */
    HttpRequest closeHttpClientAfterExecution(final boolean shouldClose) {
        closeHttpClientAfterExecution = shouldClose;

        return this;
    }

    /**
     * Sets the connection timeout for this request.
     * This creates a new HttpClient builder if one doesn't exist, or copies settings from the existing client.
     * The connection timeout is the maximum time to wait when establishing a connection to the server.
     * If the connection cannot be established within this timeout, the request will fail.
     * A {@code null} or zero duration leaves the current setting unchanged; a negative duration is rejected.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest.url("http://localhost:18080/data")
     *     .connectTimeout(Duration.ofSeconds(10))
     *     .get();
     * }</pre>
     *
     * @param connectTimeout the connection timeout; {@code null} or zero leaves the current setting unchanged
     * @return this HttpRequest instance for method chaining
     * @throws IllegalArgumentException if {@code connectTimeout} is negative.
     */
    public HttpRequest connectTimeout(final Duration connectTimeout) throws IllegalArgumentException {
        if (connectTimeout != null && !connectTimeout.isZero()) {
            // Validate before initClientBuilder(): otherwise a rejected value would still leave this
            // request owning a replacement client that swaps out the caller's client on execution.
            N.checkArgument(!connectTimeout.isNegative(), "connectTimeout cannot be negative: %s", connectTimeout);

            initClientBuilder();
            clientBuilder.connectTimeout(connectTimeout);
        }

        return this;
    }

    /**
     * Sets the connection timeout for this request, in milliseconds.
     * This is a convenience overload of {@link #connectTimeout(Duration)} that provides parity with
     * the {@code com.landawn.abacus.http.HttpRequest} and {@code OkHttpRequest} builders.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest.url("http://localhost:18080/data")
     *     .connectTimeout(10_000L)
     *     .get();
     * }</pre>
     *
     * @param connectTimeoutInMillis the connection timeout in milliseconds ({@code 0} leaves the current
     *        setting unchanged, so a previously configured timeout survives)
     * @return this HttpRequest instance for method chaining
     * @throws IllegalArgumentException if {@code connectTimeoutInMillis} is negative.
     */
    public HttpRequest connectTimeout(final long connectTimeoutInMillis) throws IllegalArgumentException {
        // Rejected rather than mapped to null: a negative millis value is as meaningless as the negative
        // Duration this overload delegates to, and silently discarding it leaves the request with no
        // timeout at all - for example when the caller passes an already-expired deadline.
        N.checkArgNotNegative(connectTimeoutInMillis, cs.connectTimeout);

        return connectTimeout(connectTimeoutInMillis > 0 ? Duration.ofMillis(connectTimeoutInMillis) : null);
    }

    /**
     * Creates the builder for every client this class owns. The JDK default is {@link HttpClient.Redirect#NEVER};
     * the sibling builders follow redirects, and a typed overload would otherwise throw on a plain 302.
     *
     * @return a new builder that follows redirects with {@link HttpClient.Redirect#NORMAL}
     */
    private static HttpClient.Builder newClientBuilder() {
        return HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL);
    }

    private void initClientBuilder() {
        if (clientBuilder == null) {
            // A caller-supplied client's own policy is copied over this default below.
            clientBuilder = newClientBuilder();
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

        // Any client produced by this builder is owned by this request. In particular,
        // fluent client-level configuration replaces even a caller-supplied client with
        // a newly built one, so that replacement must be closed after execution.
        closeHttpClientAfterExecution = true;
    }

    /**
     * Sets the timeout for the request to complete. Despite the legacy method name, this delegates to
     * {@link java.net.http.HttpRequest.Builder#timeout(Duration)}; it is not a per-read inactivity timeout.
     * A {@code null} or zero duration leaves the current setting unchanged; a negative duration is rejected.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest.url("http://localhost:18080/slow-endpoint")
     *     .readTimeout(Duration.ofSeconds(60))
     *     .get();
     * }</pre>
     *
     * @param readTimeout the request timeout (maximum duration allowed for the response); {@code null} or zero leaves the current setting unchanged
     * @return this HttpRequest instance for method chaining
     * @throws IllegalArgumentException if {@code readTimeout} is negative.
     */
    public HttpRequest readTimeout(final Duration readTimeout) throws IllegalArgumentException {
        if (readTimeout != null && !readTimeout.isZero()) {
            requestBuilder.timeout(readTimeout);
        }

        return this;
    }

    /**
     * Sets the request-completion timeout, in milliseconds. Despite the legacy method name, this is not a per-read inactivity timeout.
     * This is a convenience overload of {@link #readTimeout(Duration)} that provides parity with
     * the {@code com.landawn.abacus.http.HttpRequest} and {@code OkHttpRequest} builders.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest.url("http://localhost:18080/slow-endpoint")
     *     .readTimeout(60_000L)
     *     .get();
     * }</pre>
     *
     * @param readTimeoutInMillis the maximum duration allowed for the response, in milliseconds ({@code 0}
     *        leaves the current setting unchanged, so a previously configured timeout survives)
     * @return this HttpRequest instance for method chaining
     * @throws IllegalArgumentException if {@code readTimeoutInMillis} is negative.
     */
    public HttpRequest readTimeout(final long readTimeoutInMillis) throws IllegalArgumentException {
        // See connectTimeout(long): a negative value is rejected instead of being silently discarded.
        N.checkArgNotNegative(readTimeoutInMillis, cs.readTimeout);

        return readTimeout(readTimeoutInMillis > 0 ? Duration.ofMillis(readTimeoutInMillis) : null);
    }

    /**
     * Applies the connect timeout of the {@code url(.., long, long)} factories. {@code 0} means "no connect
     * timeout" (the JDK default); a negative value is rejected rather than discarded, matching
     * {@link #connectTimeout(long)} and the sibling {@code com.landawn.abacus.http.HttpClient}, whose
     * constructor refuses a negative {@code connectTimeoutInMillis}.
     *
     * @param builder the client builder to configure
     * @param connectTimeoutInMillis the connection timeout in milliseconds; {@code 0} for none
     * @return {@code builder}
     * @throws IllegalArgumentException if {@code connectTimeoutInMillis} is negative.
     */
    private static HttpClient.Builder withConnectTimeout(final HttpClient.Builder builder, final long connectTimeoutInMillis) throws IllegalArgumentException {
        N.checkArgNotNegative(connectTimeoutInMillis, cs.connectTimeout);

        if (connectTimeoutInMillis > 0) {
            builder.connectTimeout(Duration.ofMillis(connectTimeoutInMillis));
        }

        return builder;
    }

    /**
     * Applies the response timeout of the {@code url(.., long, long)} factories. See
     * {@link #withConnectTimeout(HttpClient.Builder, long)} for the {@code 0}/negative contract.
     *
     * @param builder the request builder to configure
     * @param readTimeoutInMillis the maximum duration allowed for the response, in milliseconds; {@code 0} for none
     * @return {@code builder}
     * @throws IllegalArgumentException if {@code readTimeoutInMillis} is negative.
     */
    private static java.net.http.HttpRequest.Builder withReadTimeout(final java.net.http.HttpRequest.Builder builder, final long readTimeoutInMillis)
            throws IllegalArgumentException {
        N.checkArgNotNegative(readTimeoutInMillis, cs.readTimeout);

        if (readTimeoutInMillis > 0) {
            builder.timeout(Duration.ofMillis(readTimeoutInMillis));
        }

        return builder;
    }

    /**
     * Sets the authenticator for this request.
     * The authenticator will be used to provide credentials when the server requests authentication
     * (for example, HTTP Basic authentication). This is useful for scenarios requiring dynamic
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
     * HttpRequest.url("http://localhost:18080/secure")
     *     .authenticator(auth)
     *     .get();
     * }</pre>
     *
     * @param authenticator the authenticator to use for providing credentials
     * @return this HttpRequest instance for method chaining
     * @throws IllegalArgumentException if {@code authenticator} is {@code null}.
     * @see #basicAuth(String, Object)
     */
    public HttpRequest authenticator(final Authenticator authenticator) throws IllegalArgumentException {
        N.checkArgNotNull(authenticator, cs.authenticator);

        initClientBuilder();

        clientBuilder.authenticator(authenticator);

        return this;
    }

    /**
     * Sets the Basic Authentication header for this request.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest.url("http://localhost:18080/secure")
     *     .basicAuth("username", "password")
     *     .get();
     * }</pre>
     *
     * @param username the username for authentication
     * @param password the password for authentication; a {@code char[]} is converted with
     *                 {@code new String(char[])}, any other value with {@link String#valueOf(Object)}
     * @return this HttpRequest instance for method chaining
     * @throws IllegalArgumentException if the resulting header value contains characters the JDK client
     *         rejects; see {@link #header(String, Object)} for the header restrictions
     * @see #basicAuth(String, String)
     * @see HttpHeaders
     * @see HttpHeaders.Names
     * @see HttpHeaders.Values
     */
    public HttpRequest basicAuth(final String username, final Object password) throws IllegalArgumentException {
        final String pwd = password instanceof char[] cs ? new String(cs) : String.valueOf(password);
        header(HttpHeaders.Names.AUTHORIZATION, "Basic " + Strings.base64Encode((username + ":" + pwd).getBytes(Charsets.UTF_8)));

        return this;
    }

    /**
     * Sets the {@code Authorization} header using HTTP Basic authentication.
     * This is the {@link String}-typed overload that provides cross-builder signature parity with
     * the {@code com.landawn.abacus.http.HttpRequest} and {@code OkHttpRequest} builders (which both
     * expose {@code basicAuth(String, String)}); the {@link #basicAuth(String, Object)} overload is
     * retained for {@code char[]} passwords.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest.url("http://localhost:18080/secure")
     *     .basicAuth("username", "password")
     *     .get();
     * }</pre>
     *
     * @param username the username for authentication
     * @param password the password for authentication
     * @return this HttpRequest instance for method chaining
     * @see HttpHeaders
     */
    public HttpRequest basicAuth(final String username, final String password) {
        return basicAuth(username, (Object) password);
    }

    /**
     * Sets the HTTP header specified by {@code name/value}.
     * If this HttpRequest already has any headers with that name, they are all replaced.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest.url("http://localhost:18080/data")
     *     .header("Accept", "application/json")
     *     .header("User-Agent", "MyApp/1.0")
     *     .get();
     * }</pre>
     *
     * <p>The JDK client validates headers eagerly: names it manages itself ({@code Connection},
     * {@code Content-Length}, {@code Expect}, {@code Host}, {@code Upgrade}; the set can be relaxed with the
     * {@code jdk.httpclient.allowRestrictedHeaders} system property) and names or values containing illegal
     * characters are rejected here, not at execution. A {@code null} value is sent as an empty string.</p>
     *
     * @param name the header name
     * @param value the header value, converted with {@link HttpHeaders#valueOf(String, Object)} ({@code null}
     *        becomes {@code ""}; a {@code Collection} is joined with {@code ", "}, or with {@code "; "} on
     *        the {@code Cookie} field)
     * @return this HttpRequest instance for method chaining
     * @throws IllegalArgumentException if {@code name} is {@code null}, is a restricted header name or {@code name}/{@code value}
     *         contains characters that are illegal in an HTTP header.
     * @see HttpHeaders
     * @see HttpHeaders.Names
     * @see HttpHeaders.Values
     */
    public HttpRequest header(final String name, final Object value) throws IllegalArgumentException {
        N.checkArgNotNull(name, cs.name);

        // Use setHeader (replace) rather than header (append) so the documented
        // "any headers with that name are all replaced" contract holds. Otherwise
        // repeated header(...) / setContentType(...) calls produce duplicate headers
        // (e.g. two Content-Type values after header("Content-Type", ...) + jsonBody(...)).
        // The value is rendered by the field-aware HttpHeaders.valueOf(name, value), so a Collection on a
        // field with its own list grammar - Cookie, whose cookie-pairs RFC 6265 5.4 separates with "; " -
        // is not comma-joined into a malformed header line.
        requestBuilder.setHeader(name, HttpHeaders.valueOf(name, value));

        return this;
    }

    /**
     * Sets HTTP headers specified by {@code name1/value1}, {@code name2/value2}.
     * If this HttpRequest already has any headers with those names, they are all replaced.
     * This is a convenience method for setting multiple headers in one call.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest.url("http://localhost:18080/data")
     *     .headers("Accept", "application/json", "User-Agent", "MyApp/1.0")
     *     .get();
     * }</pre>
     *
     * @param name1 the first header name
     * @param value1 the first header value (will be converted to string)
     * @param name2 the second header name
     * @param value2 the second header value (will be converted to string)
     * @return this HttpRequest instance for method chaining
     * @throws IllegalArgumentException if a name is restricted or a name/value contains illegal characters;
     *         see {@link #header(String, Object)}
     * @see HttpHeaders
     * @see HttpHeaders.Names
     * @see HttpHeaders.Values
     */
    public HttpRequest headers(final String name1, final Object value1, final String name2, final Object value2) throws IllegalArgumentException {
        return header(name1, value1).header(name2, value2);
    }

    /**
     * Sets HTTP headers specified by {@code name1/value1}, {@code name2/value2}, {@code name3/value3}.
     * If this HttpRequest already has any headers with those names, they are all replaced.
     * This is a convenience method for setting multiple headers in one call.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest.url("http://localhost:18080/data")
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
     * @throws IllegalArgumentException if a name is restricted or a name/value contains illegal characters;
     *         see {@link #header(String, Object)}
     * @see HttpHeaders
     * @see HttpHeaders.Names
     * @see HttpHeaders.Values
     */
    public HttpRequest headers(final String name1, final Object value1, final String name2, final Object value2, final String name3, final Object value3)
            throws IllegalArgumentException {
        return header(name1, value1).header(name2, value2).header(name3, value3);
    }

    /**
     * Merges the given header entries into the headers already on this request.
     * For each entry in the map, a header with the same name is overwritten with the new value,
     * while any existing headers whose names are <i>not</i> present in the map are kept unchanged.
     * This is a merge, not a replace-all: headers not present in the map remain unchanged.
     * Create a new request when you need to discard all prior headers and install a fresh set.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> headers = new HashMap<>();
     * headers.put("Accept", "application/json");
     * headers.put("Authorization", "Bearer token123");
     *
     * HttpRequest.url("http://localhost:18080/data")
     *     .headers(headers)
     *     .get();
     * }</pre>
     *
     * @param headers a map containing header names and values
     * @return this HttpRequest instance for method chaining
     * @throws IllegalArgumentException if a name is restricted or a name/value contains illegal characters;
     *         see {@link #header(String, Object)}. Entries before the offending one have already been applied.
     * @see HttpHeaders
     * @see HttpHeaders.Names
     * @see HttpHeaders.Values
     */
    public HttpRequest headers(final Map<String, ?> headers) throws IllegalArgumentException {
        if (N.notEmpty(headers)) {
            for (final Map.Entry<String, ?> entry : headers.entrySet()) {
                header(entry.getKey(), entry.getValue());
            }
        }

        return this;
    }

    /**
     * Sets query parameters, typically used for {@code GET} or {@code DELETE} requests.
     * The query string is appended to the URL for any request method (unlike
     * {@link com.landawn.abacus.http.HttpRequest}, which rejects queries on other methods).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpRequest.url("http://localhost:18080/search")
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
     * Sets query parameters, typically used for {@code GET} or {@code DELETE} requests.
     * The parameters are URL-encoded and appended to the URL for any request method (unlike
     * {@link com.landawn.abacus.http.HttpRequest}, which rejects queries on other methods).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> params = new HashMap<>();
     * params.put("q", "java programming");
     * params.put("limit", 10);
     *
     * HttpRequest.url("http://localhost:18080/search")
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
     * HttpRequest.url("http://localhost:18080/users")
     *     .jsonBody(json)
     *     .post();
     * }</pre>
     *
     * @param json the JSON string to send as the request body
     * @return this HttpRequest instance for method chaining
     * @see #jsonBody(Object)
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
     * HttpRequest.url("http://localhost:18080/users")
     *     .jsonBody(user)
     *     .post();
     * }</pre>
     *
     * @param obj the object to serialize to JSON and send as the request body
     * @return this HttpRequest instance for method chaining
     * @see #jsonBody(String)
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
     * HttpRequest.url("http://localhost:18080/users")
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
     * HttpRequest.url("http://localhost:18080/users")
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
     * HttpRequest.url("http://localhost:18080/login")
     *     .formBody(formData)
     *     .post();
     * }</pre>
     *
     * @param formBodyByMap a map containing form field names and values
     * @return this HttpRequest instance for method chaining
     * @see #formBody(Object)
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
     * HttpRequest.url("http://localhost:18080/login")
     *     .formBody(login)
     *     .post();
     * }</pre>
     *
     * @param formBodyByBean a bean object whose properties will be used as form fields
     * @return this HttpRequest instance for method chaining
     * @see #formBody(Map)
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
     * HttpRequest.url("http://localhost:18080/upload")
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
     * HttpResponse<String> response = HttpRequest.url("http://localhost:18080/users")
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
        return get(createStringResponseBodyHandler(HttpMethod.GET));
    }

    /**
     * Executes a GET request with a custom response body handler.
     * This allows you to control how the response body is processed, such as saving to a file,
     * reading as bytes, or handling as an input stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpResponse<byte[]> response = HttpRequest.url("http://localhost:18080/image.png")
     *     .get(BodyHandlers.ofByteArray());
     * byte[] imageData = response.body();
     * }</pre>
     *
     * @param <T> the response body type
     * @param responseBodyHandler the handler for processing the response body
     * @return the HTTP response with the processed body
     * @throws IllegalArgumentException if {@code responseBodyHandler} is {@code null}.
     * @throws UncheckedIOException if the request could not be executed
     * @see java.net.http.HttpResponse.BodyHandlers
     */
    public <T> HttpResponse<T> get(final HttpResponse.BodyHandler<T> responseBodyHandler) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(responseBodyHandler, cs.responseBodyHandler);

        return execute(HttpMethod.GET, responseBodyHandler);
    }

    /**
     * Executes a GET request and returns the response body deserialized to the specified type.
     * This method automatically handles JSON/XML deserialization based on the response content type.
     * An exception is thrown if the response status code indicates an error (not 2xx).
     * For {@code InputStream.class} (or a supertype of it) the body is returned as a stream that is
     * decoded lazily on the first read, see the class documentation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = HttpRequest.url("http://localhost:18080/users/123")
     *     .get(User.class);
     * }</pre>
     *
     * @param <T> the type of the result
     * @param resultClass the class of the result type to deserialize the response body into
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
     * HttpResponse<String> response = HttpRequest.url("http://localhost:18080/users")
     *     .jsonBody(newUser)
     *     .post();
     * }</pre>
     *
     * @return the HTTP response with String body
     * @throws UncheckedIOException if the request could not be executed
     */
    public HttpResponse<String> post() throws UncheckedIOException {
        return post(createStringResponseBodyHandler(HttpMethod.POST));
    }

    /**
     * Executes a POST request with a custom response body handler.
     * This allows you to control how the response body is processed, such as saving to a file,
     * reading as bytes, or handling as an input stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User requestData = new User("John", "Doe");
     * HttpResponse<InputStream> response = HttpRequest.url("http://localhost:18080/data")
     *     .jsonBody(requestData)
     *     .post(BodyHandlers.ofInputStream());
     * try (InputStream body = response.body()) {
     *     // Consume the response body.
     * }
     * }</pre>
     *
     * @param <T> the response body type
     * @param responseBodyHandler the handler for processing the response body
     * @return the HTTP response with the processed body
     * @throws IllegalArgumentException if {@code responseBodyHandler} is {@code null}.
     * @throws UncheckedIOException if the request could not be executed
     * @see java.net.http.HttpResponse.BodyHandlers
     */
    public <T> HttpResponse<T> post(final HttpResponse.BodyHandler<T> responseBodyHandler) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(responseBodyHandler, cs.responseBodyHandler);

        return execute(HttpMethod.POST, responseBodyHandler);
    }

    /**
     * Executes a POST request and returns the response body deserialized to the specified type.
     * This method automatically handles JSON/XML deserialization based on the response content type.
     * An exception is thrown if the response status code indicates an error (not 2xx).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User newUser = new User("John", "Doe");
     * User createdUser = HttpRequest.url("http://localhost:18080/users")
     *     .jsonBody(newUser)
     *     .post(User.class);
     * }</pre>
     *
     * @param <T> the type of the result
     * @param resultClass the class of the result type to deserialize the response body into
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
     * HttpResponse<String> response = HttpRequest.url("http://localhost:18080/users/123")
     *     .jsonBody(updatedUser)
     *     .put();
     * }</pre>
     *
     * @return the HTTP response with String body
     * @throws UncheckedIOException if the request could not be executed
     */
    public HttpResponse<String> put() throws UncheckedIOException {
        return put(createStringResponseBodyHandler(HttpMethod.PUT));
    }

    /**
     * Executes a PUT request with a custom response body handler.
     * This allows you to control how the response body is processed, such as saving to a file,
     * reading as bytes, or handling as an input stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User updatedUser = new User("John", "Smith");
     * Path outputPath = Paths.get("response.json");
     * HttpResponse<Path> response = HttpRequest.url("http://localhost:18080/users/123")
     *     .jsonBody(updatedUser)
     *     .put(BodyHandlers.ofFile(outputPath));
     * }</pre>
     *
     * @param <T> the response body type
     * @param responseBodyHandler the handler for processing the response body
     * @return the HTTP response with the processed body
     * @throws IllegalArgumentException if {@code responseBodyHandler} is {@code null}.
     * @throws UncheckedIOException if the request could not be executed
     * @see java.net.http.HttpResponse.BodyHandlers
     */
    public <T> HttpResponse<T> put(final HttpResponse.BodyHandler<T> responseBodyHandler) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(responseBodyHandler, cs.responseBodyHandler);

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
     * User result = HttpRequest.url("http://localhost:18080/users/123")
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
     * HttpResponse<String> response = HttpRequest.url("http://localhost:18080/users/123")
     *     .jsonBody(updates)
     *     .patch();
     * }</pre>
     *
     * @return the HTTP response with String body
     * @throws UncheckedIOException if the request could not be executed
     */
    public HttpResponse<String> patch() throws UncheckedIOException {
        return patch(createStringResponseBodyHandler(HttpMethod.PATCH));
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
     * HttpResponse<String> response = HttpRequest.url("http://localhost:18080/users/123")
     *     .jsonBody(updates)
     *     .patch(BodyHandlers.ofString());
     * }</pre>
     *
     * @param <T> the response body type
     * @param responseBodyHandler the handler for processing the response body
     * @return the HTTP response with the processed body
     * @throws IllegalArgumentException if {@code responseBodyHandler} is {@code null}.
     * @throws UncheckedIOException if the request could not be executed
     * @see java.net.http.HttpResponse.BodyHandlers
     */
    public <T> HttpResponse<T> patch(final HttpResponse.BodyHandler<T> responseBodyHandler) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(responseBodyHandler, cs.responseBodyHandler);

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
     * User result = HttpRequest.url("http://localhost:18080/users/123")
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
     * HttpResponse<String> response = HttpRequest.url("http://localhost:18080/users/123")
     *     .delete();
     * }</pre>
     *
     * @return the HTTP response with String body
     * @throws UncheckedIOException if the request could not be executed
     */
    public HttpResponse<String> delete() throws UncheckedIOException {
        return delete(createStringResponseBodyHandler(HttpMethod.DELETE));
    }

    /**
     * Executes a DELETE request with a custom response body handler.
     * This allows you to control how the response body is processed, such as saving to a file,
     * reading as bytes, or handling as an input stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpResponse<Void> response = HttpRequest.url("http://localhost:18080/users/123")
     *     .delete(BodyHandlers.discarding());
     * System.out.println("Deleted with status: " + response.statusCode());
     * }</pre>
     *
     * @param <T> the response body type
     * @param responseBodyHandler the handler for processing the response body
     * @return the HTTP response with the processed body
     * @throws IllegalArgumentException if {@code responseBodyHandler} is {@code null}.
     * @throws UncheckedIOException if the request could not be executed
     * @see java.net.http.HttpResponse.BodyHandlers
     */
    public <T> HttpResponse<T> delete(final HttpResponse.BodyHandler<T> responseBodyHandler) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(responseBodyHandler, cs.responseBodyHandler);

        return execute(HttpMethod.DELETE, responseBodyHandler);
    }

    /**
     * Executes a DELETE request and returns the response body deserialized to the specified type.
     * This method automatically handles JSON/XML deserialization based on the response content type.
     * An exception is thrown if the response status code indicates an error (not 2xx).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DeleteResponse result = HttpRequest.url("http://localhost:18080/users/123")
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
     * HttpResponse<Void> response = HttpRequest.url("http://localhost:18080/large-file.zip")
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
     * HttpResponse<String> response = HttpRequest.url("http://localhost:18080/data")
     *     .execute(HttpMethod.GET);
     * }</pre>
     *
     * @param httpMethod the HTTP method to use (GET, POST, PUT, PATCH, DELETE, HEAD)
     * @return the HTTP response with String body
     * @throws IllegalArgumentException if {@code httpMethod} is {@code null}.
     * @throws UncheckedIOException if the request could not be executed
     */
    @Beta
    public HttpResponse<String> execute(final HttpMethod httpMethod) throws IllegalArgumentException, UncheckedIOException {
        return execute(httpMethod, createStringResponseBodyHandler(httpMethod));
    }

    /**
     * Executes an HTTP request with the specified method and custom response body handler.
     * This is a generic execution method that provides full control over both the HTTP method
     * and how the response is processed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpResponse<byte[]> response = HttpRequest.url("http://localhost:18080/image")
     *     .execute(HttpMethod.GET, BodyHandlers.ofByteArray());
     * }</pre>
     *
     * @param <T> the response body type
     * @param httpMethod the HTTP method to use (GET, POST, PUT, PATCH, DELETE, HEAD)
     * @param responseBodyHandler the handler for processing the response body
     * @return the HTTP response with the processed body
     * @throws IllegalArgumentException if {@code httpMethod} or {@code responseBodyHandler} is {@code null}.
     * @throws UncheckedIOException if the request could not be executed
     * @throws RuntimeException if the request is interrupted; the current thread's interrupt status is restored
     * @see java.net.http.HttpResponse.BodyHandlers
     */
    @Beta
    public <T> HttpResponse<T> execute(final HttpMethod httpMethod, final HttpResponse.BodyHandler<T> responseBodyHandler)
            throws IllegalArgumentException, UncheckedIOException, RuntimeException {
        N.checkArgNotNull(httpMethod, cs.httpMethod);
        N.checkArgNotNull(responseBodyHandler, cs.responseBodyHandler);

        final HttpClient httpClientToUse = checkUrlAndHttpClient();
        final HttpResponse<T> response;

        try {
            response = httpClientToUse.send(requestBuilder.method(httpMethod.name(), checkBodyPublisher()).build(), responseBodyHandler);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            final RuntimeException primary = ExceptionUtil.toRuntimeException(e, true);
            doAfterExecutionPreservingPrimary(httpClientToUse, primary);
            throw primary;
        } catch (final IOException e) {
            final RuntimeException primary = ExceptionUtil.toRuntimeException(e, true);
            doAfterExecutionPreservingPrimary(httpClientToUse, primary);
            throw primary;
        } catch (final RuntimeException | Error e) {
            doAfterExecutionPreservingPrimary(httpClientToUse, e);
            throw e;
        }

        // Successful send transfers cleanup to response preparation: immediate close for ordinary
        // bodies, close-on-stream-close for InputStream, orderly shutdown for Java streams/publishers.
        final T responseBody;

        try {
            checkStringBodyDecoding(response, responseBodyHandler);
            responseBody = response.body();
        } catch (final RuntimeException | Error e) {
            doAfterExecutionPreservingPrimary(httpClientToUse, e);
            throw e;
        }

        return prepareResponseForClientCleanup(response, responseBody, httpClientToUse);
    }

    /**
     * Executes an HTTP request with the specified method and returns the response body deserialized to the specified type.
     * This is a generic execution method that automatically handles JSON/XML deserialization.
     * An exception is thrown if the response status code indicates an error (not 2xx).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = HttpRequest.url("http://localhost:18080/users/123")
     *     .execute(HttpMethod.GET, User.class);
     * }</pre>
     *
     * @param <T> the type of the result
     * @param httpMethod the HTTP method to use (GET, POST, PUT, PATCH, DELETE, HEAD)
     * @param resultClass the class of the result type to deserialize the response body into
     * @return the deserialized response body
     * @throws IllegalArgumentException if {@code httpMethod} is {@code null}.
     * @throws UncheckedIOException if the request could not be executed or the response indicates an error
     */
    @Beta
    public <T> T execute(final HttpMethod httpMethod, final Class<T> resultClass) throws IllegalArgumentException, UncheckedIOException {
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
                // Explicit UTF-8: the 2-arg overload uses the PLATFORM default charset,
                // mis-encoding non-ASCII query values on non-UTF-8 JVMs.
                requestBuilder.uri(URI.create(URLEncodedUtil.encode(url, query, HttpUtil.DEFAULT_CHARSET)));
            } else {
                requestBuilder.uri(URI.create(URLEncodedUtil.encode(uri.toString(), query, HttpUtil.DEFAULT_CHARSET)));
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

    /**
     * Releases the {@link HttpClient} used for this request when that client is owned by the request.
     * The shared default client is never closed. Closing is best effort: any exception raised while
     * closing is swallowed. Calling this method more than once for the same client is harmless.
     *
     * @param httpClientUsed the client that executed the request
     */
    void doAfterExecution(final HttpClient httpClientUsed) {
        try {
            closeOwnedHttpClient(httpClientUsed);
        } catch (final Exception e) {
            // ignore — best effort cleanup
        }
    }

    private void doAfterExecutionPreservingPrimary(final HttpClient httpClientUsed, final Throwable primary) {
        try {
            closeOwnedHttpClient(httpClientUsed);
        } catch (final RuntimeException | Error cleanupFailure) {
            if (cleanupFailure != primary) {
                primary.addSuppressed(cleanupFailure);
            }
        } catch (final Exception e) {
            // Preserve the established best-effort treatment of checked close failures.
        }
    }

    private void doAfterExecutionPropagatingUnchecked(final HttpClient httpClientUsed) {
        try {
            closeOwnedHttpClient(httpClientUsed);
        } catch (final RuntimeException | Error e) {
            throw e;
        } catch (final Exception e) {
            // Preserve the established best-effort treatment of checked close failures.
        }
    }

    /**
     * @throws Exception if closing an owned HTTP client through {@link AutoCloseable#close()} fails
     */
    private void closeOwnedHttpClient(final HttpClient httpClientUsed) throws Exception {
        if (closeHttpClientAfterExecution && httpClientUsed != DEFAULT_HTTP_CLIENT && httpClientUsed instanceof AutoCloseable ac) {
            // Java 21+ HttpClient implements AutoCloseable; shut it down to release internal executor threads.
            ac.close();
        }
    }

    private void shutdownStreamingClient(final HttpClient httpClientUsed, final Object body) {
        try {
            if (closeHttpClientAfterExecution && httpClientUsed != DEFAULT_HTTP_CLIENT) {
                // close() waits for consumption, which cannot start until this response is delivered.
                // Orderly shutdown preserves the in-flight body while rejecting further requests.
                httpClientUsed.shutdown();
            }
        } catch (final RuntimeException | Error e) {
            closeOrphanedAsyncResult(body);
            throw e;
        }
    }

    /**
     * Executes a GET request asynchronously and returns a CompletableFuture with a String body response.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CompletableFuture<HttpResponse<String>> future = HttpRequest.url("http://localhost:18080/users")
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
        return asyncGet(createStringResponseBodyHandler(HttpMethod.GET));
    }

    /**
     * Executes a GET request asynchronously with a custom response body handler.
     * The request executes in the background, allowing the calling thread to continue processing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CompletableFuture<HttpResponse<byte[]>> future =
     *     HttpRequest.url("http://localhost:18080/large-file")
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
     * @throws IllegalArgumentException if {@code responseBodyHandler} is {@code null}.
     * @see java.net.http.HttpResponse.BodyHandlers
     */
    public <T> CompletableFuture<HttpResponse<T>> asyncGet(final HttpResponse.BodyHandler<T> responseBodyHandler) throws IllegalArgumentException {
        N.checkArgNotNull(responseBodyHandler, cs.responseBodyHandler);

        return asyncExecute(HttpMethod.GET, responseBodyHandler);
    }

    /**
     * Executes a GET request asynchronously and returns the response body deserialized to the specified type.
     * This method automatically handles JSON/XML deserialization in the background.
     * An exception is thrown if the response status code indicates an error (not 2xx).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CompletableFuture<User> future = HttpRequest.url("http://localhost:18080/users/123")
     *     .asyncGet(User.class);
     *
     * future.thenAccept(System.out::println);
     * }</pre>
     *
     * @param <T> the type of the result
     * @param resultClass the class of the result type to deserialize the response body into
     * @return a CompletableFuture that completes with the deserialized response body, or exceptionally with
     *         {@link HttpResponseException} (surfacing as {@code CompletionException}/{@code ExecutionException})
     *         for a non-2xx status
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
     *     HttpRequest.url("http://localhost:18080/page")
     *         .asyncGet(BodyHandlers.ofString(), pushHandler);
     * }</pre>
     *
     * @param <T> the response body type
     * @param responseBodyHandler the handler for processing the response body
     * @param pushPromiseHandler the handler for processing HTTP/2 server push promises
     * @return a CompletableFuture that will complete with the HTTP response
     * @throws IllegalArgumentException if {@code responseBodyHandler} is {@code null}, or {@code pushPromiseHandler}
     *         is {@code null}.
     * @see java.net.http.HttpResponse.PushPromiseHandler
     */
    public <T> CompletableFuture<HttpResponse<T>> asyncGet(final HttpResponse.BodyHandler<T> responseBodyHandler,
            final PushPromiseHandler<T> pushPromiseHandler) throws IllegalArgumentException {
        N.checkArgNotNull(responseBodyHandler, cs.responseBodyHandler);

        N.checkArgNotNull(pushPromiseHandler, cs.pushPromiseHandler);

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
     *     HttpRequest.url("http://localhost:18080/users")
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
        return asyncPost(createStringResponseBodyHandler(HttpMethod.POST));
    }

    /**
     * Executes a POST request asynchronously with a custom response body handler.
     * This allows you to control how the response body is processed while the request executes
     * in the background.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ReportRequest reportRequest = new ReportRequest("monthly");
     * CompletableFuture<HttpResponse<Path>> future =
     *     HttpRequest.url("http://localhost:18080/report")
     *         .jsonBody(reportRequest)
     *         .asyncPost(BodyHandlers.ofFile(Paths.get("report.pdf")));
     * }</pre>
     *
     * @param <T> the response body type
     * @param responseBodyHandler the handler for processing the response body
     * @return a CompletableFuture that will complete with the HTTP response
     * @throws IllegalArgumentException if {@code responseBodyHandler} is {@code null}.
     * @see java.net.http.HttpResponse.BodyHandlers
     */
    public <T> CompletableFuture<HttpResponse<T>> asyncPost(final HttpResponse.BodyHandler<T> responseBodyHandler) throws IllegalArgumentException {
        N.checkArgNotNull(responseBodyHandler, cs.responseBodyHandler);

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
     *     HttpRequest.url("http://localhost:18080/users")
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
     * @return a CompletableFuture that completes with the deserialized response body, or exceptionally with
     *         {@link HttpResponseException} (surfacing as {@code CompletionException}/{@code ExecutionException})
     *         for a non-2xx status
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
     * User data = new User("John", "Doe");
     * CompletableFuture<HttpResponse<String>> future =
     *     HttpRequest.url("http://localhost:18080/submit")
     *         .jsonBody(data)
     *         .asyncPost(BodyHandlers.ofString(), pushHandler);
     * }</pre>
     *
     * @param <T> the response body type
     * @param responseBodyHandler the handler for processing the response body
     * @param pushPromiseHandler the handler for processing HTTP/2 server push promises
     * @return a CompletableFuture that will complete with the HTTP response
     * @throws IllegalArgumentException if {@code responseBodyHandler} is {@code null}, or {@code pushPromiseHandler}
     *         is {@code null}.
     * @see java.net.http.HttpResponse.PushPromiseHandler
     */
    public <T> CompletableFuture<HttpResponse<T>> asyncPost(final HttpResponse.BodyHandler<T> responseBodyHandler,
            final PushPromiseHandler<T> pushPromiseHandler) throws IllegalArgumentException {
        N.checkArgNotNull(responseBodyHandler, cs.responseBodyHandler);

        N.checkArgNotNull(pushPromiseHandler, cs.pushPromiseHandler);

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
     *     HttpRequest.url("http://localhost:18080/users/123")
     *         .jsonBody(updatedUser)
     *         .asyncPut();
     * }</pre>
     *
     * @return a CompletableFuture that will complete with the HTTP response
     */
    public CompletableFuture<HttpResponse<String>> asyncPut() {
        return asyncPut(createStringResponseBodyHandler(HttpMethod.PUT));
    }

    /**
     * Executes a PUT request asynchronously with a custom response body handler.
     * This allows you to control how the response body is processed while the request executes
     * in the background.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User updatedUser = new User("John", "Smith");
     * CompletableFuture<HttpResponse<String>> future =
     *     HttpRequest.url("http://localhost:18080/users/123")
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
     * @throws IllegalArgumentException if {@code responseBodyHandler} is {@code null}.
     * @see java.net.http.HttpResponse.BodyHandlers
     */
    public <T> CompletableFuture<HttpResponse<T>> asyncPut(final HttpResponse.BodyHandler<T> responseBodyHandler) throws IllegalArgumentException {
        N.checkArgNotNull(responseBodyHandler, cs.responseBodyHandler);

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
     *     HttpRequest.url("http://localhost:18080/users/123")
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
     * @return a CompletableFuture that completes with the deserialized response body, or exceptionally with
     *         {@link HttpResponseException} (surfacing as {@code CompletionException}/{@code ExecutionException})
     *         for a non-2xx status
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
     * User updatedUser = new User("John", "Smith");
     * CompletableFuture<HttpResponse<String>> future =
     *     HttpRequest.url("http://localhost:18080/users/123")
     *         .jsonBody(updatedUser)
     *         .asyncPut(BodyHandlers.ofString(), pushHandler);
     * }</pre>
     *
     * @param <T> the response body type
     * @param responseBodyHandler the handler for processing the response body
     * @param pushPromiseHandler the handler for processing HTTP/2 server push promises
     * @return a CompletableFuture that will complete with the HTTP response
     * @throws IllegalArgumentException if {@code responseBodyHandler} is {@code null}, or {@code pushPromiseHandler}
     *         is {@code null}.
     * @see java.net.http.HttpResponse.PushPromiseHandler
     */
    public <T> CompletableFuture<HttpResponse<T>> asyncPut(final HttpResponse.BodyHandler<T> responseBodyHandler,
            final PushPromiseHandler<T> pushPromiseHandler) throws IllegalArgumentException {
        N.checkArgNotNull(responseBodyHandler, cs.responseBodyHandler);

        N.checkArgNotNull(pushPromiseHandler, cs.pushPromiseHandler);

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
     *     HttpRequest.url("http://localhost:18080/users/123")
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
        return asyncPatch(createStringResponseBodyHandler(HttpMethod.PATCH));
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
     *     HttpRequest.url("http://localhost:18080/users/123")
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
     * @throws IllegalArgumentException if {@code responseBodyHandler} is {@code null}.
     * @see java.net.http.HttpResponse.BodyHandlers
     */
    public <T> CompletableFuture<HttpResponse<T>> asyncPatch(final HttpResponse.BodyHandler<T> responseBodyHandler) throws IllegalArgumentException {
        N.checkArgNotNull(responseBodyHandler, cs.responseBodyHandler);

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
     *     HttpRequest.url("http://localhost:18080/users/123")
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
     * @return a CompletableFuture that completes with the deserialized response body, or exceptionally with
     *         {@link HttpResponseException} (surfacing as {@code CompletionException}/{@code ExecutionException})
     *         for a non-2xx status
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
     * Map<String, Object> updates = Map.of("status", "active");
     * CompletableFuture<HttpResponse<String>> future =
     *     HttpRequest.url("http://localhost:18080/users/123")
     *         .jsonBody(updates)
     *         .asyncPatch(BodyHandlers.ofString(), pushHandler);
     * }</pre>
     *
     * @param <T> the response body type
     * @param responseBodyHandler the handler for processing the response body
     * @param pushPromiseHandler the handler for processing HTTP/2 server push promises
     * @return a CompletableFuture that will complete with the HTTP response
     * @throws IllegalArgumentException if {@code responseBodyHandler} is {@code null}, or {@code pushPromiseHandler}
     *         is {@code null}.
     * @see java.net.http.HttpResponse.PushPromiseHandler
     */
    public <T> CompletableFuture<HttpResponse<T>> asyncPatch(final HttpResponse.BodyHandler<T> responseBodyHandler,
            final PushPromiseHandler<T> pushPromiseHandler) throws IllegalArgumentException {
        N.checkArgNotNull(responseBodyHandler, cs.responseBodyHandler);

        N.checkArgNotNull(pushPromiseHandler, cs.pushPromiseHandler);

        return asyncExecute(HttpMethod.PATCH, responseBodyHandler, pushPromiseHandler);
    }

    /**
     * Executes a DELETE request asynchronously and returns a CompletableFuture with a String body response.
     * The request executes in the background, allowing the calling thread to continue processing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CompletableFuture<HttpResponse<String>> future =
     *     HttpRequest.url("http://localhost:18080/users/123")
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
        return asyncDelete(createStringResponseBodyHandler(HttpMethod.DELETE));
    }

    /**
     * Executes a DELETE request asynchronously with a custom response body handler.
     * This allows you to control how the response body is processed while the request executes
     * in the background.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CompletableFuture<HttpResponse<Void>> future =
     *     HttpRequest.url("http://localhost:18080/users/123")
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
     * @throws IllegalArgumentException if {@code responseBodyHandler} is {@code null}.
     * @see java.net.http.HttpResponse.BodyHandlers
     */
    public <T> CompletableFuture<HttpResponse<T>> asyncDelete(final HttpResponse.BodyHandler<T> responseBodyHandler) throws IllegalArgumentException {
        N.checkArgNotNull(responseBodyHandler, cs.responseBodyHandler);

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
     *     HttpRequest.url("http://localhost:18080/users/123")
     *         .asyncDelete(DeleteResponse.class);
     *
     * future.thenAccept(result -> {
     *     System.out.println("Delete result: " + result.getMessage());
     * });
     * }</pre>
     *
     * @param <T> the type of the result
     * @param resultClass the class of the result type to deserialize the response body into
     * @return a CompletableFuture that completes with the deserialized response body, or exceptionally with
     *         {@link HttpResponseException} (surfacing as {@code CompletionException}/{@code ExecutionException})
     *         for a non-2xx status
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
     *     HttpRequest.url("http://localhost:18080/users/123")
     *         .asyncDelete(BodyHandlers.ofString(), pushHandler);
     * }</pre>
     *
     * @param <T> the response body type
     * @param responseBodyHandler the handler for processing the response body
     * @param pushPromiseHandler the handler for processing HTTP/2 server push promises
     * @return a CompletableFuture that will complete with the HTTP response
     * @throws IllegalArgumentException if {@code responseBodyHandler} is {@code null}, or {@code pushPromiseHandler}
     *         is {@code null}.
     * @see java.net.http.HttpResponse.PushPromiseHandler
     */
    public <T> CompletableFuture<HttpResponse<T>> asyncDelete(final HttpResponse.BodyHandler<T> responseBodyHandler,
            final PushPromiseHandler<T> pushPromiseHandler) throws IllegalArgumentException {
        N.checkArgNotNull(responseBodyHandler, cs.responseBodyHandler);

        N.checkArgNotNull(pushPromiseHandler, cs.pushPromiseHandler);

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
     *     HttpRequest.url("http://localhost:18080/large-file.zip")
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
     *     HttpRequest.url("http://localhost:18080/data")
     *         .asyncExecute(HttpMethod.GET);
     *
     * future.thenAccept(response -> {
     *     System.out.println("Response: " + response.body());
     * });
     * }</pre>
     *
     * @param httpMethod the HTTP method to use (GET, POST, PUT, PATCH, DELETE, HEAD)
     * @return a CompletableFuture that will complete with the HTTP response
     * @throws IllegalArgumentException if {@code httpMethod} is {@code null}.
     */
    @Beta
    public CompletableFuture<HttpResponse<String>> asyncExecute(final HttpMethod httpMethod) throws IllegalArgumentException {
        return asyncExecute(httpMethod, createStringResponseBodyHandler(httpMethod));
    }

    /**
     * Executes an HTTP request asynchronously with the specified method and custom response body handler.
     * This is a generic async execution method that provides full control over both the HTTP method
     * and how the response is processed. The request executes in the background.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CompletableFuture<HttpResponse<byte[]>> future =
     *     HttpRequest.url("http://localhost:18080/image")
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
     * @throws IllegalArgumentException if {@code httpMethod} or {@code responseBodyHandler} is {@code null}.
     * @see java.net.http.HttpResponse.BodyHandlers
     */
    @Beta
    public <T> CompletableFuture<HttpResponse<T>> asyncExecute(final HttpMethod httpMethod, final HttpResponse.BodyHandler<T> responseBodyHandler)
            throws IllegalArgumentException {
        N.checkArgNotNull(httpMethod, cs.httpMethod);
        N.checkArgNotNull(responseBodyHandler, cs.responseBodyHandler);

        final HttpClient httpClientToUse = checkUrlAndHttpClient();
        final ClientCleanup clientCleanup = new ClientCleanup(httpClientToUse);

        try {
            return observeAsyncExecution(httpClientToUse.sendAsync(requestBuilder.method(httpMethod.name(), checkBodyPublisher()).build(), responseBodyHandler),
                    response -> {
                        checkStringBodyDecoding(response, responseBodyHandler);
                        return prepareResponseForClientCleanup(response, clientCleanup);
                    }, clientCleanup);
        } catch (final RuntimeException | Error e) {
            clientCleanup.preservePrimary(e);
            throw e;
        }
    }

    /**
     * Executes an HTTP request asynchronously with the specified method and returns the response body deserialized to the specified type.
     * This is a generic async execution method that automatically handles JSON/XML deserialization in the background.
     * An exception is thrown if the response status code indicates an error (not 2xx).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CompletableFuture<User> future =
     *     HttpRequest.url("http://localhost:18080/users/123")
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
     * @return a CompletableFuture that completes with the deserialized response body, or exceptionally with
     *         {@link HttpResponseException} (surfacing as {@code CompletionException}/{@code ExecutionException})
     *         for a non-2xx status
     * @throws IllegalArgumentException if {@code httpMethod} is {@code null}.
     */
    @Beta
    public <T> CompletableFuture<T> asyncExecute(final HttpMethod httpMethod, final Class<T> resultClass) throws IllegalArgumentException {
        N.checkArgNotNull(httpMethod, cs.httpMethod);

        final BodyHandler<?> responseBodyHandler = createResponseBodyHandler(resultClass);
        final HttpClient httpClientToUse = checkUrlAndHttpClient();
        final ClientCleanup clientCleanup = new ClientCleanup(httpClientToUse);

        try {
            // Typed errors are streamed and read only to a bounded prefix. Move their conversion
            // off the client's I/O executor, which may have only one thread needed to deliver bytes.
            return observeAsyncExecution(httpClientToUse.sendAsync(requestBuilder.method(httpMethod.name(), checkBodyPublisher()).build(), responseBodyHandler),
                    response -> getBody(prepareResponseForClientCleanup(response, clientCleanup), resultClass), clientCleanup, true);
        } catch (final RuntimeException | Error e) {
            clientCleanup.preservePrimary(e);
            throw e;
        }
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
     *     HttpRequest.url("http://localhost:18080/page")
     *         .asyncExecute(HttpMethod.GET, BodyHandlers.ofString(), pushHandler);
     * }</pre>
     *
     * @param <T> the response body type
     * @param httpMethod the HTTP method to use (GET, POST, PUT, PATCH, DELETE, HEAD)
     * @param responseBodyHandler the handler for processing the response body
     * @param pushPromiseHandler the handler for processing HTTP/2 server push promises
     * @return a CompletableFuture that will complete with the HTTP response
     * @throws IllegalArgumentException if {@code httpMethod}, {@code responseBodyHandler}, or
     *         {@code pushPromiseHandler} is {@code null}.
     * @see java.net.http.HttpResponse.PushPromiseHandler
     */
    @Beta
    public <T> CompletableFuture<HttpResponse<T>> asyncExecute(final HttpMethod httpMethod, final HttpResponse.BodyHandler<T> responseBodyHandler,
            final PushPromiseHandler<T> pushPromiseHandler) throws IllegalArgumentException {
        N.checkArgNotNull(httpMethod, cs.httpMethod);
        N.checkArgNotNull(responseBodyHandler, cs.responseBodyHandler);
        N.checkArgNotNull(pushPromiseHandler, cs.pushPromiseHandler);

        final HttpClient httpClientToUse = checkUrlAndHttpClient();
        final ClientCleanup clientCleanup = new ClientCleanup(httpClientToUse);

        try {
            return observeAsyncExecution(
                    httpClientToUse.sendAsync(requestBuilder.method(httpMethod.name(), checkBodyPublisher()).build(), responseBodyHandler, pushPromiseHandler),
                    response -> prepareResponseForClientCleanup(response, clientCleanup), clientCleanup);
        } catch (final RuntimeException | Error e) {
            clientCleanup.preservePrimary(e);
            throw e;
        }
    }

    private <S, T> CompletableFuture<T> observeAsyncExecution(final CompletableFuture<S> upstream, final Function<? super S, ? extends T> resultMapper,
            final ClientCleanup clientCleanup) {
        return observeAsyncExecution(upstream, resultMapper, clientCleanup, false);
    }

    private <S, T> CompletableFuture<T> observeAsyncExecution(final CompletableFuture<S> upstream, final Function<? super S, ? extends T> resultMapper,
            final ClientCleanup clientCleanup, final boolean asyncMapping) {
        final CompletableFuture<T> result = new CompletableFuture<>();

        // This observer is deliberately not exposed to callers. Cancelling the public result
        // therefore cannot detach cleanup from the request that is still running upstream.
        final BiConsumer<S, Throwable> completion = (upstreamResult, upstreamFailure) -> {
            if (upstreamFailure != null) {
                clientCleanup.preservePrimary(upstreamFailure);
                result.completeExceptionally(upstreamFailure);
                return;
            }

            final T mappedResult;

            try {
                mappedResult = resultMapper.apply(upstreamResult);
            } catch (final Throwable e) {
                clientCleanup.preservePrimary(e);
                result.completeExceptionally(e);
                return;
            }

            if (!result.complete(mappedResult)) {
                closeOrphanedAsyncResult(mappedResult);
            }
        };

        if (asyncMapping) {
            // Dispatch the consuming observer itself: an intermediate async stage could finish
            // before registration and let a subsequent synchronous observer block the caller.
            upstream.whenCompleteAsync(completion);
        } else {
            upstream.whenComplete(completion);
        }

        return result;
    }

    @SuppressWarnings("unused")
    private static void closeOrphanedAsyncResult(final Object result) {
        try {
            final Object body = result instanceof HttpResponse<?> response ? response.body() : result;

            if (body instanceof InputStream inputStream) {
                inputStream.close();
            } else if (body instanceof BaseStream<?, ?> stream) {
                stream.close();
            } else if (body instanceof Flow.Publisher<?> publisher) {
                // No recipient can subscribe after failed delivery. Cancel without requesting bytes.
                publisher.subscribe(new Flow.Subscriber<Object>() {
                    @Override
                    public void onSubscribe(final Flow.Subscription subscription) {
                        subscription.cancel();
                    }

                    @Override
                    public void onNext(final Object item) {
                        // No demand is issued.
                    }

                    @Override
                    public void onError(final Throwable failure) {
                        // The public result already has its outcome.
                    }

                    @Override
                    public void onComplete() {
                        // Nothing remains to release.
                    }
                });
            }
        } catch (final Exception | Error e) {
            // Delivery failed or the public result already has an outcome; cleanup must not replace
            // that outcome. CleanupInputStream still releases its client in this path.
        }
    }

    private final class ClientCleanup {
        private final HttpClient httpClientUsed;
        private final AtomicBoolean claimed = new AtomicBoolean();

        private ClientCleanup(final HttpClient httpClientUsed) {
            this.httpClientUsed = httpClientUsed;
        }

        private void bestEffort() {
            if (claimed.compareAndSet(false, true)) {
                doAfterExecution(httpClientUsed);
            }
        }

        private void preservePrimary(final Throwable primary) {
            if (claimed.compareAndSet(false, true)) {
                doAfterExecutionPreservingPrimary(httpClientUsed, primary);
            }
        }

        private void propagateUnchecked() {
            if (claimed.compareAndSet(false, true)) {
                doAfterExecutionPropagatingUnchecked(httpClientUsed);
            }
        }

        private void shutdownStreaming(final Object body) {
            if (claimed.compareAndSet(false, true)) {
                shutdownStreamingClient(httpClientUsed, body);
            }
        }
    }

    private <T> HttpResponse<T> prepareResponseForClientCleanup(final HttpResponse<T> response, final T responseBody, final HttpClient httpClientUsed) {
        if (responseBody instanceof InputStream inputStream) {
            @SuppressWarnings("unchecked")
            final T body = (T) new CleanupInputStream(inputStream, () -> doAfterExecutionPropagatingUnchecked(httpClientUsed));

            return new DelegatingHttpResponse<>(response, body);
        }

        if (responseBody instanceof BaseStream<?, ?> || responseBody instanceof Flow.Publisher<?>) {
            shutdownStreamingClient(httpClientUsed, responseBody);
        } else {
            doAfterExecution(httpClientUsed);
        }
        return response;
    }

    private <T> HttpResponse<T> prepareResponseForClientCleanup(final HttpResponse<T> response, final ClientCleanup clientCleanup) {
        final T responseBody = response.body();

        if (responseBody instanceof InputStream inputStream) {
            @SuppressWarnings("unchecked")
            final T body = (T) new CleanupInputStream(inputStream, clientCleanup::propagateUnchecked);

            return new DelegatingHttpResponse<>(response, body);
        }

        if (responseBody instanceof BaseStream<?, ?> || responseBody instanceof Flow.Publisher<?>) {
            clientCleanup.shutdownStreaming(responseBody);
        } else {
            clientCleanup.bestEffort();
        }
        return response;
    }

    private static final class DelegatingHttpResponse<T> implements HttpResponse<T> {
        private final HttpResponse<T> response;
        private final T body;

        private DelegatingHttpResponse(final HttpResponse<T> response, final T body) {
            this.response = response;
            this.body = body;
        }

        @Override
        public int statusCode() {
            return response.statusCode();
        }

        @Override
        public java.net.http.HttpRequest request() {
            return response.request();
        }

        @Override
        public Optional<HttpResponse<T>> previousResponse() {
            return response.previousResponse();
        }

        @Override
        public java.net.http.HttpHeaders headers() {
            return response.headers();
        }

        @Override
        public T body() {
            return body;
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return response.sslSession();
        }

        @Override
        public URI uri() {
            return response.uri();
        }

        @Override
        public HttpClient.Version version() {
            return response.version();
        }
    }

    private static final class CleanupInputStream extends InputStream {
        private final InputStream inputStream;
        private final Runnable cleanup;
        private final AtomicBoolean closed = new AtomicBoolean();

        private CleanupInputStream(final InputStream inputStream, final Runnable cleanup) {
            this.inputStream = inputStream;
            this.cleanup = cleanup;
        }

        @Override
        public int read() throws IOException {
            return inputStream.read();
        }

        @Override
        public int read(final byte[] b) throws IOException {
            return inputStream.read(b);
        }

        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException {
            return inputStream.read(b, off, len);
        }

        @Override
        public long skip(final long n) throws IOException {
            return inputStream.skip(n);
        }

        @Override
        public int available() throws IOException {
            return inputStream.available();
        }

        @Override
        public void close() throws IOException {
            if (!closed.compareAndSet(false, true)) {
                return;
            }

            try {
                inputStream.close();
            } catch (final IOException | RuntimeException | Error e) {
                try {
                    cleanup.run();
                } catch (final RuntimeException | Error cleanupFailure) {
                    if (cleanupFailure != e) {
                        e.addSuppressed(cleanupFailure);
                    }
                }

                throw e;
            }

            cleanup.run();
        }

        @Override
        public synchronized void mark(final int readlimit) {
            inputStream.mark(readlimit);
        }

        @Override
        public synchronized void reset() throws IOException {
            inputStream.reset();
        }

        @Override
        public boolean markSupported() {
            return inputStream.markSupported();
        }
    }

    private BodyPublisher checkBodyPublisher() {
        return bodyPublisher == null ? BodyPublishers.noBody() : bodyPublisher;
    }

    private BodyHandler<Object> createResponseBodyHandler(final Class<?> resultClass) {
        return responseInfo -> {
            final BodySubscriber<?> subscriber;
            if (!HttpUtil.isSuccessfulResponseCode(responseInfo.statusCode())) {
                // Select this before the result type: even Void errors need bounded diagnostics,
                // and buffering compressed errors first would leave their memory use unbounded.
                subscriber = BodySubscribers.ofInputStream();
            } else if (resultClass == null || resultClass.equals(Void.class)) {
                subscriber = BodySubscribers.discarding();
            } else if (resultClass.isAssignableFrom(InputStream.class)) { // Intentional support for supertypes of InputStream.
                subscriber = BodySubscribers.ofInputStream();
            } else {
                subscriber = BodySubscribers.ofByteArray();
            }
            // Only adapt the subscriber's type here; blocking body reads must happen after delivery.
            return BodySubscribers.mapping(subscriber, body -> body);
        };
    }

    /**
     * Creates a String handler that decompresses the response before decoding with its declared charset.
     * The JDK's {@link BodyHandlers#ofString()} handles the charset but does not decompress the body.
     *
     * @param httpMethod the request method, used to identify responses that cannot carry content
     * @return a body handler that decompresses the response body and decodes it with the charset
     *         declared by the response {@code Content-Type}
     */
    private static BodyHandler<String> createStringResponseBodyHandler(final HttpMethod httpMethod) {
        // Decode diagnostics belong to one execution. Never cache this stateful handler across requests.
        return new StringResponseBodyHandler(httpMethod);
    }

    private static final class StringResponseBodyHandler implements BodyHandler<String> {
        private final HttpMethod method;
        // Publishing a failure also publishes its raw prefix and charset to synchronous/async response checks.
        private volatile RuntimeException decodingFailure;
        private byte[] rawBody;
        private Charset charset;

        private StringResponseBodyHandler(final HttpMethod method) {
            this.method = method;
        }

        @Override
        public HttpResponse.BodySubscriber<String> apply(final HttpResponse.ResponseInfo responseInfo) {
            final String contentType = responseInfo.headers().firstValue(HttpHeaders.Names.CONTENT_TYPE).orElse(null);
            final String encoding = responseInfo.headers().firstValue(HttpHeaders.Names.CONTENT_ENCODING).orElse(null);
            charset = HttpUtil.getCharset(contentType);
            final ContentFormat format = HttpUtil.hasResponseBody(method == null ? null : method.name(), responseInfo.statusCode())
                    ? HttpUtil.getContentFormat(contentType, encoding)
                    : ContentFormat.NONE;
            final boolean error = !HttpUtil.isSuccessfulResponseCode(responseInfo.statusCode());

            return BodySubscribers.mapping(BodySubscribers.ofByteArray(), bytes -> {
                try {
                    return new String(decompress(bytes, format), charset);
                } catch (final RuntimeException failure) {
                    if (!error) {
                        throw failure;
                    }
                    rawBody = Arrays.copyOf(bytes, Math.min(bytes.length, HttpUtil.MAX_ERROR_BODY_SIZE));
                    decodingFailure = failure;
                    // Complete transport first so the exception can retain the actual response URI and headers.
                    // This placeholder is never delivered: checkStringBodyDecoding throws before returning a response.
                    return Strings.EMPTY;
                }
            });
        }
    }

    private static void checkStringBodyDecoding(final HttpResponse<?> response, final BodyHandler<?> handler) {
        if (handler instanceof StringResponseBodyHandler stringHandler && stringHandler.decodingFailure != null) {
            throw new HttpResponseException(response.uri() == null ? null : response.uri().toString(), response.statusCode(), null, response.headers().map(),
                    new String(stringHandler.rawBody, stringHandler.charset), stringHandler.rawBody, stringHandler.decodingFailure);
        }
    }

    private <T> T getBody(final HttpResponse<?> httpResponse, final Class<T> resultClass) {
        final String contentType = httpResponse.headers().firstValue(HttpHeaders.Names.CONTENT_TYPE).orElse(null);
        final String contentEncoding = httpResponse.headers().firstValue(HttpHeaders.Names.CONTENT_ENCODING).orElse(null);
        final java.net.http.HttpRequest request = httpResponse.request();
        final ContentFormat responseContentFormat = HttpUtil.hasResponseBody(request == null ? null : request.method(), httpResponse.statusCode())
                ? HttpUtil.getContentFormat(contentType, contentEncoding)
                : ContentFormat.NONE;
        final Charset responseCharset = HttpUtil.getCharset(contentType);

        if (!HttpUtil.isSuccessfulResponseCode(httpResponse.statusCode())) {
            throw readHttpError(httpResponse, responseContentFormat, responseCharset);
        }

        if (resultClass == null || Void.class.equals(resultClass)) {
            return null; // refer to isOneWayRequest.
        }

        final Object body = httpResponse.body();

        if (body instanceof byte[] rawBytes) {
            final byte[] bytes = decompress(rawBytes, responseContentFormat);

            if (byte[].class.equals(resultClass)) {
                return (T) bytes;
            }

            if (String.class.equals(resultClass)) {
                return (T) new String(bytes, responseCharset);
            }

            if (responseContentFormat == ContentFormat.KRYO) {
                return HttpUtil.getParser(responseContentFormat).deserialize(new ByteArrayInputStream(bytes), resultClass);
            }

            final String bodyStr = new String(bytes, responseCharset);

            if (responseContentFormat == ContentFormat.FORM_URL_ENCODED) {
                return URLEncodedUtil.decode(bodyStr, responseCharset, resultClass);
            } else if (responseContentFormat != null && responseContentFormat != ContentFormat.NONE) {
                return HttpUtil.getParser(responseContentFormat).deserialize(bodyStr, resultClass);
            }

            return N.convert(bodyStr, resultClass);
        }

        if (body instanceof InputStream stream && Strings.isNotEmpty(HttpUtil.getContentEncoding(responseContentFormat))) {
            // Every other result type is delivered decoded; a stream must be too. Decoding is lazy so
            // the async path still completes at header delivery (GZIPInputStream reads its header
            // eagerly) and a stalled server cannot pin the completion thread.
            return N.convert(new LazyDecodingInputStream(stream, responseContentFormat), resultClass);
        }

        return N.convert(body, resultClass);
    }

    /**
     * Removes a response {@code Content-Encoding} from an {@code InputStream} result on first use. The raw
     * stream is the deferred-cleanup stream, so closing this one (directly, or through the decoder, which
     * closes what it wraps) releases an owned client exactly once. If wrapping fails - a corrupt or empty
     * compressed body - the raw stream is closed before the failure propagates.
     */
    private static final class LazyDecodingInputStream extends InputStream {
        private final InputStream rawStream;
        private final ContentFormat contentFormat;
        private final AtomicBoolean closed = new AtomicBoolean();
        private InputStream decoded;

        private LazyDecodingInputStream(final InputStream rawStream, final ContentFormat contentFormat) {
            this.rawStream = rawStream;
            this.contentFormat = contentFormat;
        }

        /**
         * @throws IOException if this stream was closed before its decoder was initialized
         * @throws UncheckedIOException if initializing the response decompressor fails, including an empty or malformed compressed header
         */
        private InputStream decoded() throws IOException, UncheckedIOException {
            if (decoded == null) {
                if (closed.get()) {
                    throw new IOException("Stream closed");
                }

                try {
                    decoded = HttpUtil.wrapInputStream(rawStream, contentFormat);
                } catch (final RuntimeException | Error e) {
                    // Nothing was handed out yet, so the raw stream (and its owned client) would leak.
                    if (closed.compareAndSet(false, true)) {
                        try {
                            rawStream.close();
                        } catch (final IOException | RuntimeException | Error closeFailure) {
                            if (closeFailure != e) {
                                e.addSuppressed(closeFailure);
                            }
                        }
                    }

                    throw e;
                }
            }

            return decoded;
        }

        /**
         * @throws IOException if this stream was closed before its decoder was initialized, or if reading decoded response bytes fails
         * @throws UncheckedIOException if initializing the response decompressor fails, including an empty or malformed compressed header
         */
        @Override
        public int read() throws IOException, UncheckedIOException {
            return decoded().read();
        }

        /**
         * @throws IOException if this stream was closed before its decoder was initialized, or if reading decoded response bytes fails
         * @throws UncheckedIOException if initializing the response decompressor fails, including an empty or malformed compressed header
         */
        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException, UncheckedIOException {
            return decoded().read(b, off, len);
        }

        /**
         * @throws IOException if this stream was closed before its decoder was initialized, or if skipping decoded response bytes fails
         * @throws UncheckedIOException if initializing the response decompressor fails, including an empty or malformed compressed header
         */
        @Override
        public long skip(final long n) throws IOException, UncheckedIOException {
            return decoded().skip(n);
        }

        /**
         * @throws IOException if this stream was closed before its decoder was initialized, or if querying the decoded response stream fails
         * @throws UncheckedIOException if initializing the response decompressor fails, including an empty or malformed compressed header
         */
        @Override
        public int available() throws IOException, UncheckedIOException {
            return decoded().available();
        }

        @Override
        public void close() throws IOException {
            if (!closed.compareAndSet(false, true)) {
                return;
            }

            // The decoder closes the raw stream itself; close whichever one is live, never both.
            if (decoded != null) {
                decoded.close();
            } else {
                rawStream.close();
            }
        }
    }

    @SuppressWarnings("resource")
    private static HttpResponseException readHttpError(final HttpResponse<?> response, final ContentFormat contentFormat, final Charset charset) {
        final Object body = response.body();
        InputStream input = body instanceof InputStream stream ? stream : body instanceof byte[] bytes ? new ByteArrayInputStream(bytes) : null;
        String captured = Strings.EMPTY;
        try {
            if (input != null) {
                // Assignment after construction preserves the raw stream for cleanup if wrapping fails.
                input = HttpUtil.wrapInputStream(input, contentFormat);
                captured = new String(input.readNBytes(HttpUtil.MAX_ERROR_BODY_SIZE), charset);
            }
        } catch (final IOException | RuntimeException e) {
            // A corrupt or unavailable error body must not erase the known status and headers.
        } catch (final Error primary) {
            // Fatal read failures still own the response stream; preserve them while releasing it.
            if (input != null) {
                try {
                    input.close();
                } catch (final IOException | RuntimeException | Error cleanupFailure) {
                    if (cleanupFailure != primary) {
                        primary.addSuppressed(cleanupFailure);
                    }
                }
            }
            throw primary;
        }

        final HttpResponseException failure = new HttpResponseException(response.uri() == null ? null : response.uri().toString(), response.statusCode(), null,
                response.headers().map(), captured);
        if (input != null) {
            try {
                input.close();
            } catch (final IOException | RuntimeException | Error cleanupFailure) {
                // CleanupInputStream closes an owned client too; keep that secondary to the HTTP error.
                failure.addSuppressed(cleanupFailure);
            }
        }
        return failure;
    }

    private static byte[] decompress(final byte[] body, final ContentFormat contentFormat) {
        // Nothing to remove: HttpUtil.wrapInputStream hands back the stream unchanged for a format that carries
        // no Content-Encoding (NONE, JSON, XML, ..), so the round trip through a stream would only copy the
        // whole body. Same predicate getBody applies before it decodes an InputStream result.
        if (Strings.isEmpty(HttpUtil.getContentEncoding(contentFormat))) {
            return body;
        }

        final InputStream input = HttpUtil.wrapInputStream(new ByteArrayInputStream(body), contentFormat);

        try {
            return IOUtil.readAllBytes(input);
        } finally {
            IOUtil.close(input);
        }
    }
}
