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

import java.net.Proxy;
import java.util.Map;

import javax.net.ssl.SSLSocketFactory;

import com.landawn.abacus.util.Charsets;
import com.landawn.abacus.util.Strings;

/**
 * Configuration settings for HTTP requests.
 * This class provides a fluent interface for configuring various HTTP request parameters
 * including timeouts, headers, SSL settings, proxy configuration, and content format.
 *
 * <p>HttpSettings can be used with both HttpClient and HttpRequest to customize request behavior.
 * Settings can be applied globally to an HttpClient instance or per-request.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * HttpSettings settings = HttpSettings.create()
 *     .setConnectTimeout(5000)
 *     .setReadTimeout(10000)
 *     .header("Authorization", "Bearer token123")
 *     .header("Accept", "application/json")
 *     .setContentType("application/json")
 *     .setUseCaches(false);
 *
 * // Use with HttpClient
 * HttpClient client = HttpClient.create("http://localhost:18080",
 *     16, 5000, 10000, settings);
 *
 * // Or use with individual requests
 * String response = client.get(settings);
 * }</pre>
 *
 * @see HttpClient
 * @see HttpRequest
 * @see HttpHeaders
 */
public final class HttpSettings {

    private long connectTimeout;

    private long readTimeout;

    private boolean useCaches = false;

    private boolean doInput = true;

    private boolean doOutput = true;

    private boolean isOneWayRequest = false;

    private ContentFormat contentFormat;

    private HttpHeaders headers = null;

    private SSLSocketFactory sslSocketFactory;

    private Proxy proxy;

    /**
     * Creates a new HttpSettings instance with default values.
     */
    public HttpSettings() { //NOSONAR
    }

    /**
     * Creates a new HttpSettings instance with default values.
     * This is a convenience factory method equivalent to calling the constructor.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpSettings settings = HttpSettings.create()
     *     .setConnectTimeout(5000)
     *     .header("Accept", "application/json");
     * }</pre>
     *
     * @return a new HttpSettings instance
     */
    public static HttpSettings create() {
        return new HttpSettings();
    }

    /**
     * Gets the connection timeout in milliseconds.
     * The connection timeout is the time to wait for a connection to be established.
     *
     * @return the connection timeout in milliseconds, or 0 if not set
     * @see #setConnectTimeout(long)
     */
    public long getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Sets the connection timeout in milliseconds.
     *
     * <p>The connection timeout controls how long the client will wait while attempting
     * to establish a connection to the remote server. This timeout begins when the connection
     * attempt starts and expires if the connection cannot be established within the specified time.</p>
     *
     * <p>If the timeout expires before a connection is established, a {@link java.net.SocketTimeoutException}
     * will be thrown. A timeout of 0 means infinite timeout (wait indefinitely), which is not recommended
     * for production environments.</p>
     *
     * <p><b>Important:</b> This setting only applies to {@code HttpClient}, not {@code OKHttpClient}.</p>
     *
     * <p><b>Recommended Values:</b></p>
     * <ul>
     *   <li>Fast network environments: 3000-5000ms (3-5 seconds)</li>
     *   <li>Standard environments: 5000-10000ms (5-10 seconds)</li>
     *   <li>Slow or unreliable networks: 10000-30000ms (10-30 seconds)</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Set 5 second connection timeout
     * settings.setConnectTimeout(5000);
     *
     * // Set different timeouts for connection and read operations
     * settings.setConnectTimeout(5000)   // 5s to establish connection
     *         .setReadTimeout(10000);   // 10s to read response
     * }</pre>
     *
     * @param connectTimeout the connection timeout in milliseconds (0 = infinite)
     * @return this HttpSettings instance for method chaining
     */
    public HttpSettings setConnectTimeout(final long connectTimeout) {
        this.connectTimeout = connectTimeout;

        return this;
    }

    /**
     * Gets the read timeout in milliseconds.
     * The read timeout is the time to wait for data to be available for reading.
     *
     * @return the read timeout in milliseconds, or 0 if not set
     */
    public long getReadTimeout() {
        return readTimeout;
    }

    /**
     * Sets the read timeout in milliseconds.
     * The read timeout is the time to wait for data to be available for reading.
     * A timeout of 0 means infinite timeout.
     *
     * <p><b>Note:</b> Only for {@code HttpClient}, not for {@code OKHttpClient}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * settings.setReadTimeout(10000);   // 10 seconds
     * }</pre>
     *
     * @param readTimeout the read timeout in milliseconds
     * @return this HttpSettings instance for method chaining
     */
    public HttpSettings setReadTimeout(final long readTimeout) {
        this.readTimeout = readTimeout;

        return this;
    }

    /**
     * Gets the SSL socket factory used for HTTPS connections.
     *
     * @return the SSL socket factory, or {@code null} if not set
     */
    public SSLSocketFactory getSSLSocketFactory() {
        return sslSocketFactory;
    }

    /**
     * Sets the SSL socket factory for HTTPS connections.
     * This allows customization of SSL/TLS behavior, such as certificate validation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SSLContext sslContext = SSLContext.getInstance("TLS");
     * // Configure SSL context...
     * settings.setSSLSocketFactory(sslContext.getSocketFactory());
     * }</pre>
     *
     * @param sslSocketFactory the SSL socket factory to use
     * @return this HttpSettings instance for method chaining
     */
    public HttpSettings setSSLSocketFactory(final SSLSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;

        return this;
    }

    /**
     * Gets the proxy configuration.
     *
     * @return the proxy, or {@code null} if not set
     */
    public Proxy getProxy() {
        return proxy;
    }

    /**
     * Sets the proxy for HTTP connections.
     * This allows requests to be routed through a proxy server.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Proxy proxy = new Proxy(Proxy.Type.HTTP,
     *     new InetSocketAddress("proxy.example.com", 8080));
     * settings.setProxy(proxy);
     * }</pre>
     *
     * @param proxy the proxy to use
     * @return this HttpSettings instance for method chaining
     */
    public HttpSettings setProxy(final Proxy proxy) {
        this.proxy = proxy;

        return this;
    }

    /**
     * Gets whether to use caches.
     *
     * @return {@code true} if caches should be used, {@code false} otherwise
     */
    public boolean getUseCaches() { // NOSONAR
        return useCaches;
    }

    /**
     * Sets whether to use caches for HTTP connections.
     * When enabled, the HTTP implementation may cache responses.
     *
     * <p><b>Note:</b> Only for {@code HttpClient}, not for {@code OKHttpClient}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * settings.setUseCaches(false);   // Disable caching (default)
     * settings.setUseCaches(true);    // Enable caching
     * }</pre>
     *
     * @param useCaches {@code true} to use caches, {@code false} otherwise
     * @return this HttpSettings instance for method chaining
     */
    public HttpSettings setUseCaches(final boolean useCaches) {
        this.useCaches = useCaches;

        return this;
    }

    /**
     * Gets whether the connection will be used for input.
     * This corresponds to HttpURLConnection's doInput property.
     *
     * @return {@code true} if the connection will be used for input
     * @see java.net.HttpURLConnection#setDoInput(boolean)
     */
    public boolean doInput() {
        return doInput;
    }

    /**
     * Sets whether the connection will be used for input.
     * This should almost always be {@code true} (the default).
     *
     * <p><b>Note:</b> Only for {@code HttpClient}, not for {@code OKHttpClient}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * settings.doInput(true);    // Enable input (default)
     * settings.doInput(false);   // Disable input (rare)
     * }</pre>
     *
     * @param doInput {@code true} if the connection will be used for input
     * @return this HttpSettings instance for method chaining
     * @see java.net.HttpURLConnection#setDoInput(boolean)
     */
    public HttpSettings doInput(final boolean doInput) {
        this.doInput = doInput;

        return this;
    }

    /**
     * Gets whether the connection will be used for output.
     * This corresponds to HttpURLConnection's doOutput property.
     *
     * @return {@code true} if the connection will be used for output
     * @see java.net.HttpURLConnection#setDoOutput(boolean)
     */
    public boolean doOutput() {
        return doOutput;
    }

    /**
     * Sets whether the connection will be used for output.
     * This is automatically set to {@code true} for POST and PUT requests.
     *
     * <p><b>Note:</b> Only for {@code HttpClient}, not for {@code OKHttpClient}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * settings.doOutput(true);    // Enable output (default)
     * settings.doOutput(false);   // Disable output for GET requests
     * }</pre>
     *
     * @param doOutput {@code true} if the connection will be used for output
     * @return this HttpSettings instance for method chaining
     * @see java.net.HttpURLConnection#setDoOutput(boolean)
     */
    public HttpSettings doOutput(final boolean doOutput) {
        this.doOutput = doOutput;

        return this;
    }

    /**
     * Checks if this is a one-way request (fire-and-forget).
     * One-way requests don't wait for or process the response.
     *
     * @return {@code true} if this is a one-way request
     */
    public boolean isOneWayRequest() {
        return isOneWayRequest;
    }

    /**
     * Sets whether this is a one-way request (fire-and-forget).
     * When {@code true}, the request will be sent but the response will not be read.
     * This can improve performance for requests where the response is not needed.
     *
     * @param isOneWayRequest {@code true} for one-way requests
     * @return this HttpSettings instance for method chaining
     */
    public HttpSettings setOneWayRequest(final boolean isOneWayRequest) {
        this.isOneWayRequest = isOneWayRequest;

        return this;
    }

    /**
     * Gets the content format for request/response serialization.
     * If not explicitly set, it will be determined from the Content-Type header.
     *
     * @return The content format, or {@code null} if not set
     */
    public ContentFormat getContentFormat() {
        if ((contentFormat == null || contentFormat == ContentFormat.NONE) && headers != null) {
            contentFormat = HttpUtil.getContentFormat(HttpUtil.getContentType(headers), HttpUtil.getContentEncoding(headers));
        }

        return contentFormat;
    }

    /**
     * Sets the content format for request/response serialization.
     * This determines how request bodies are serialized and response bodies are deserialized.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * settings.setContentFormat(ContentFormat.JSON);
     * settings.setContentFormat(ContentFormat.XML);
     * settings.setContentFormat(ContentFormat.FormUrlEncoded);
     * }</pre>
     *
     * @param contentFormat the content format to use
     * @return this HttpSettings instance for method chaining
     */
    public HttpSettings setContentFormat(final ContentFormat contentFormat) {
        this.contentFormat = contentFormat;

        return this;
    }

    /**
     * Gets the Content-Type header value.
     * If not explicitly set but a content format is configured,
     * the content type will be derived from the content format.
     *
     * @return The Content-Type header value, or {@code null} if not set
     */
    public String getContentType() {
        String contentType = HttpUtil.getContentType(headers);

        if (Strings.isEmpty(contentType) && contentFormat != null) {
            contentType = HttpUtil.getContentType(contentFormat);

            if (Strings.isNotEmpty(contentType)) {
                header(HttpHeaders.Names.CONTENT_TYPE, contentType);
            }
        }

        return contentType;
    }

    /**
     * Sets the Content-Type header.
     * Common content types include:
     * <ul>
     *   <li>application/json</li>
     *   <li>application/xml</li>
     *   <li>application/x-www-form-urlencoded</li>
     *   <li>text/plain</li>
     *   <li>text/html</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * settings.setContentType("application/json; charset=UTF-8");
     * }</pre>
     *
     * @param contentType the Content-Type header value
     * @return this HttpSettings instance for method chaining
     */
    public HttpSettings setContentType(final String contentType) {
        header(HttpHeaders.Names.CONTENT_TYPE, contentType);

        return this;
    }

    /**
     * Gets the Content-Encoding header value.
     * If not explicitly set but a content format is configured,
     * the content encoding will be derived from the content format.
     *
     * @return The Content-Encoding header value, or {@code null} if not set
     */
    public String getContentEncoding() {
        String contentEncoding = HttpUtil.getContentEncoding(headers);

        if (Strings.isEmpty(contentEncoding) && contentFormat != null) {
            contentEncoding = HttpUtil.getContentEncoding(contentFormat);

            if (Strings.isNotEmpty(contentEncoding)) {
                header(HttpHeaders.Names.CONTENT_ENCODING, contentEncoding);
            }
        }

        return contentEncoding;
    }

    /**
     * Sets the Content-Encoding header.
     * Common content encodings include:
     * <ul>
     *   <li>gzip</li>
     *   <li>deflate</li>
     *   <li>br (Brotli)</li>
     *   <li>lz4</li>
     *   <li>snappy</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * settings.setContentEncoding("gzip");
     * }</pre>
     *
     * @param contentEncoding the Content-Encoding header value
     * @return this HttpSettings instance for method chaining
     */
    public HttpSettings setContentEncoding(final String contentEncoding) {
        header(HttpHeaders.Names.CONTENT_ENCODING, contentEncoding);

        return this;
    }

    /**
     * Sets HTTP Basic Authentication header using the specified username and password.
     * This method automatically encodes the credentials and sets the Authorization header.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * settings.basicAuth("username", "password");
     * // This sets the header: "Authorization: Basic dXNlcm5hbWU6cGFzc3dvcmQ="
     * }</pre>
     *
     * @param username the username for authentication
     * @param password the password for authentication
     * @return this HttpSettings instance for method chaining
     */
    @SuppressWarnings("UnusedReturnValue")
    public HttpSettings basicAuth(final String username, final Object password) {
        return header(HttpHeaders.Names.AUTHORIZATION, "Basic " + Strings.base64Encode((username + ":" + password).getBytes(Charsets.UTF_8)));
    }

    /**
     * Sets an HTTP header with the specified name and value.
     * If this settings object already has any headers with that name, they are all replaced.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * settings.header("Authorization", "Bearer token123")
     *         .header("Accept", "application/json")
     *         .header("User-Agent", "MyApp/1.0");
     * }</pre>
     *
     * @param name the header name (must not be {@code null})
     * @param value the header value
     * @return this HttpSettings instance for method chaining
     * @see HttpHeaders
     */
    public HttpSettings header(final String name, final Object value) {
        headers().set(name, value);

        return this;
    }

    /**
     * Sets two HTTP headers with the specified names and values.
     * If this settings object already has any headers with those names, they are all replaced.
     *
     * @param name1 the first header name
     * @param value1 the first header value
     * @param name2 the second header name
     * @param value2 the second header value
     * @return this HttpSettings instance for method chaining
     * @see HttpHeaders
     */
    public HttpSettings headers(final String name1, final Object value1, final String name2, final Object value2) {
        headers().set(name1, value1);
        headers().set(name2, value2);

        return this;
    }

    /**
     * Sets three HTTP headers with the specified names and values.
     * If this settings object already has any headers with those names, they are all replaced.
     *
     * @param name1 the first header name
     * @param value1 the first header value
     * @param name2 the second header name
     * @param value2 the second header value
     * @param name3 the third header name
     * @param value3 the third header value
     * @return this HttpSettings instance for method chaining
     * @see HttpHeaders
     */
    public HttpSettings headers(final String name1, final Object value1, final String name2, final Object value2, final String name3, final Object value3) {
        headers().set(name1, value1);
        headers().set(name2, value2);
        headers().set(name3, value3);

        return this;
    }

    /**
     * Sets HTTP headers from the specified map.
     * If this settings object already has any headers with the same names, they are all replaced.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> headers = Map.of(
     *     "Authorization", "Bearer token123",
     *     "Accept", "application/json",
     *     "User-Agent", "MyApp/1.0"
     * );
     * settings.headers(headers);
     * }</pre>
     *
     * @param headers a map containing header names and values
     * @return this HttpSettings instance for method chaining
     * @see HttpHeaders
     */
    public HttpSettings headers(final Map<String, ?> headers) {
        headers().setAll(headers);

        return this;
    }

    /**
     * Removes all headers on this settings object and adds the specified headers.
     *
     * @param headers the HttpHeaders to set
     * @return this HttpSettings instance for method chaining
     * @see HttpHeaders
     */
    public HttpSettings headers(final HttpHeaders headers) {
        if (this.headers == headers) {
            return this;
        }

        headers().clear();

        if (headers != null) {
            headers().setAll(headers.map);
        }

        return this;
    }

    /**
     * Gets the HTTP headers configured in this settings object.
     * If no headers have been set yet, this method creates an empty HttpHeaders object.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpHeaders headers = settings.headers();
     * headers.set("X-Custom-Header", "value");
     * }</pre>
     *
     * @return the HttpHeaders object (never {@code null})
     * @see HttpHeaders
     */
    public HttpHeaders headers() {
        if (headers == null) {
            headers = HttpHeaders.create();
        }

        return headers;
    }

    /**
     * Creates a copy of this HttpSettings object.
     * The copy includes all settings including headers, timeouts, SSL configuration, etc.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpSettings original = HttpSettings.create()
     *     .setConnectTimeout(5000)
     *     .header("Authorization", "Bearer token");
     *
     * HttpSettings copy = original.copy();
     * // copy has the same settings as original
     * }</pre>
     *
     * @return a new HttpSettings instance with the same configuration
     */
    public HttpSettings copy() {
        final HttpSettings copy = new HttpSettings().setConnectTimeout(connectTimeout)
                .setReadTimeout(readTimeout)
                .setSSLSocketFactory(sslSocketFactory)
                .setProxy(proxy)
                .setUseCaches(useCaches)
                .doInput(doInput)
                .doOutput(doOutput)
                .setOneWayRequest(isOneWayRequest)
                .setContentFormat(contentFormat);

        if (headers != null) {
            copy.headers(headers.copy());
        }

        return copy;
    }

    /**
     * Returns a string representation of this HttpSettings object.
     * The string includes all configured settings for debugging purposes.
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return "{connectTimeout=" + connectTimeout + ", readTimeout=" + readTimeout + ", sslSocketFactory=" + sslSocketFactory + ", proxy=" + proxy
                + ", useCaches=" + useCaches + ", doInput=" + doInput + ", doOutput=" + doOutput + ", isOneWayRequest=" + isOneWayRequest + ", contentFormat="
                + contentFormat + ", headers=" + headers + "}";
    }

}
