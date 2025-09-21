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
 * <p>Example usage:</p>
 * <pre>{@code
 * HttpSettings settings = HttpSettings.create()
 *     .setConnectionTimeout(5000)
 *     .setReadTimeout(10000)
 *     .header("Authorization", "Bearer token123")
 *     .header("Accept", "application/json")
 *     .setContentType("application/json")
 *     .setUseCaches(false);
 * 
 * // Use with HttpClient
 * HttpClient client = HttpClient.create("https://api.example.com", 
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

    private long connectionTimeout;

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
     * @return A new HttpSettings instance
     * 
     * <p>Example:</p>
     * <pre>{@code
     * HttpSettings settings = HttpSettings.create()
     *     .setConnectionTimeout(5000)
     *     .header("Accept", "application/json");
     * }</pre>
     */
    public static HttpSettings create() {
        return new HttpSettings();
    }

    /**
     * Gets the connection timeout in milliseconds.
     * The connection timeout is the time to wait for a connection to be established.
     *
     * @return The connection timeout in milliseconds, or 0 if not set
     */
    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * Sets the connection timeout in milliseconds.
     * The connection timeout is the time to wait for a connection to be established.
     * A timeout of 0 means infinite timeout.
     * 
     * Note: Only for {@code HttpClient}, not for {@code OKHttpClient}.
     *
     * @param connectionTimeout The connection timeout in milliseconds
     * @return This HttpSettings instance for method chaining
     * 
     * <p>Example:</p>
     * <pre>{@code
     * settings.setConnectionTimeout(5000); // 5 seconds
     * }</pre>
     */
    public HttpSettings setConnectionTimeout(final long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;

        return this;
    }

    /**
     * Gets the read timeout in milliseconds.
     * The read timeout is the time to wait for data to be available for reading.
     *
     * @return The read timeout in milliseconds, or 0 if not set
     */
    public long getReadTimeout() {
        return readTimeout;
    }

    /**
     * Sets the read timeout in milliseconds.
     * The read timeout is the time to wait for data to be available for reading.
     * A timeout of 0 means infinite timeout.
     * 
     * Note: Only for {@code HttpClient}, not for {@code OKHttpClient}.
     *
     * @param readTimeout The read timeout in milliseconds
     * @return This HttpSettings instance for method chaining
     * 
     * <p>Example:</p>
     * <pre>{@code
     * settings.setReadTimeout(10000); // 10 seconds
     * }</pre>
     */
    public HttpSettings setReadTimeout(final long readTimeout) {
        this.readTimeout = readTimeout;

        return this;
    }

    /**
     * Gets the SSL socket factory used for HTTPS connections.
     *
     * @return The SSL socket factory, or null if not set
     */
    public SSLSocketFactory getSSLSocketFactory() {
        return sslSocketFactory;
    }

    /**
     * Sets the SSL socket factory for HTTPS connections.
     * This allows customization of SSL/TLS behavior, such as certificate validation.
     *
     * @param sslSocketFactory The SSL socket factory to use
     * @return This HttpSettings instance for method chaining
     * 
     * <p>Example:</p>
     * <pre>{@code
     * SSLContext sslContext = SSLContext.getInstance("TLS");
     * // Configure SSL context...
     * settings.setSSLSocketFactory(sslContext.getSocketFactory());
     * }</pre>
     */
    public HttpSettings setSSLSocketFactory(final SSLSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;

        return this;
    }

    /**
     * Gets the proxy configuration.
     *
     * @return The proxy, or null if not set
     */
    public Proxy getProxy() {
        return proxy;
    }

    /**
     * Sets the proxy for HTTP connections.
     * This allows requests to be routed through a proxy server.
     *
     * @param proxy The proxy to use
     * @return This HttpSettings instance for method chaining
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Proxy proxy = new Proxy(Proxy.Type.HTTP, 
     *     new InetSocketAddress("proxy.example.com", 8080));
     * settings.setProxy(proxy);
     * }</pre>
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
     * Note: Only for {@code HttpClient}, not for {@code OKHttpClient}.
     *
     * @param useCaches true to use caches, {@code false} otherwise
     * @return This HttpSettings instance for method chaining
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
     * This should almost always be true (the default).
     * 
     * Note: Only for {@code HttpClient}, not for {@code OKHttpClient}.
     *
     * @param doInput true if the connection will be used for input
     * @return This HttpSettings instance for method chaining
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
     * This is automatically set to true for POST and PUT requests.
     * 
     * Note: Only for {@code HttpClient}, not for {@code OKHttpClient}.
     *
     * @param doOutput true if the connection will be used for output
     * @return This HttpSettings instance for method chaining
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
     * When true, the request will be sent but the response will not be read.
     * This can improve performance for requests where the response is not needed.
     *
     * @param isOneWayRequest true for one-way requests
     * @return This HttpSettings instance for method chaining
     */
    public HttpSettings isOneWayRequest(final boolean isOneWayRequest) {
        this.isOneWayRequest = isOneWayRequest;

        return this;
    }

    /**
     * Gets the content format for request/response serialization.
     * If not explicitly set, it will be determined from the Content-Type header.
     *
     * @return The content format, or null if not set
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
     * @param contentFormat The content format to use
     * @return This HttpSettings instance for method chaining
     * 
     * <p>Example:</p>
     * <pre>{@code
     * settings.setContentFormat(ContentFormat.JSON);
     * settings.setContentFormat(ContentFormat.XML);
     * settings.setContentFormat(ContentFormat.FormUrlEncoded);
     * }</pre>
     */
    public HttpSettings setContentFormat(final ContentFormat contentFormat) {
        this.contentFormat = contentFormat;

        //    if (contentFormat == null || contentFormat == ContentFormat.NONE) {
        //        if (headers != null) {
        //            headers.remove(HttpHeaders.Names.CONTENT_TYPE);
        //            headers.remove(HttpHeaders.Names.CONTENT_ENCODING);
        //        }
        //    } else {
        //        final String contentType = N.toString(headers().get(HttpHeaders.Names.CONTENT_TYPE));
        //        final String contentEncoding = N.toString(headers().get(HttpHeaders.Names.CONTENT_ENCODING));
        //
        //        if (!contentFormat.equals(HTTP.getContentFormat(contentType, contentEncoding))) {
        //            final String newContentType = HTTP.getContentType(contentFormat);
        //
        //            if (N.isEmpty(newContentType)) {
        //                headers().remove(HttpHeaders.Names.CONTENT_TYPE);
        //            } else {
        //                header(HttpHeaders.Names.CONTENT_TYPE, newContentType);
        //            }
        //
        //            final String newContentEncoding = HTTP.getContentEncoding(contentFormat);
        //
        //            if (N.isEmpty(newContentEncoding)) {
        //                headers().remove(HttpHeaders.Names.CONTENT_ENCODING);
        //            } else {
        //                header(HttpHeaders.Names.CONTENT_ENCODING, newContentEncoding);
        //            }
        //        }
        //    }

        return this;
    }

    /**
     * Gets the Content-Type header value.
     * If not explicitly set but a content format is configured, 
     * the content type will be derived from the content format.
     *
     * @return The Content-Type header value, or null if not set
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
     * @param contentType The Content-Type header value
     * @return This HttpSettings instance for method chaining
     * 
     * <p>Example:</p>
     * <pre>{@code
     * settings.setContentType("application/json; charset=UTF-8");
     * }</pre>
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
     * @return The Content-Encoding header value, or null if not set
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
     * @param contentEncoding The Content-Encoding header value
     * @return This HttpSettings instance for method chaining
     * 
     * <p>Example:</p>
     * <pre>{@code
     * settings.setContentEncoding("gzip");
     * }</pre>
     */
    public HttpSettings setContentEncoding(final String contentEncoding) {
        header(HttpHeaders.Names.CONTENT_ENCODING, contentEncoding);

        return this;
    }

    /**
     * Sets HTTP Basic Authentication header using the specified username and password.
     * This method automatically encodes the credentials and sets the Authorization header.
     *
     * @param user The username for authentication
     * @param password The password for authentication
     * @return This HttpSettings instance for method chaining
     * 
     * <p>Example:</p>
     * <pre>{@code
     * settings.basicAuth("username", "password");
     * // This sets the header: "Authorization: Basic dXNlcm5hbWU6cGFzc3dvcmQ="
     * }</pre>
     */
    @SuppressWarnings("UnusedReturnValue")
    public HttpSettings basicAuth(final String user, final Object password) {
        return header(HttpHeaders.Names.AUTHORIZATION, "Basic " + Strings.base64Encode((user + ":" + password).getBytes(Charsets.UTF_8)));
    }

    /**
     * Sets an HTTP header with the specified name and value.
     * If this settings object already has any headers with that name, they are all replaced.
     *
     * @param name The header name
     * @param value The header value
     * @return This HttpSettings instance for method chaining
     * @see HttpHeaders
     * 
     * <p>Example:</p>
     * <pre>{@code
     * settings.header("Authorization", "Bearer token123")
     *         .header("Accept", "application/json")
     *         .header("User-Agent", "MyApp/1.0");
     * }</pre>
     */
    public HttpSettings header(final String name, final Object value) {
        headers().set(name, value);

        return this;
    }

    /**
     * Sets two HTTP headers with the specified names and values.
     * If this settings object already has any headers with those names, they are all replaced.
     *
     * @param name1 The first header name
     * @param value1 The first header value
     * @param name2 The second header name
     * @param value2 The second header value
     * @return This HttpSettings instance for method chaining
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
     * @param name1 The first header name
     * @param value1 The first header value
     * @param name2 The second header name
     * @param value2 The second header value
     * @param name3 The third header name
     * @param value3 The third header value
     * @return This HttpSettings instance for method chaining
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
     * @param headers A map containing header names and values
     * @return This HttpSettings instance for method chaining
     * @see HttpHeaders
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Map<String, String> headers = Map.of(
     *     "Authorization", "Bearer token123",
     *     "Accept", "application/json",
     *     "User-Agent", "MyApp/1.0"
     * );
     * settings.headers(headers);
     * }</pre>
     */
    public HttpSettings headers(final Map<String, ?> headers) {
        headers().setAll(headers);

        return this;
    }

    /**
     * Removes all headers on this settings object and adds the specified headers.
     *
     * @param headers The HttpHeaders to set
     * @return This HttpSettings instance for method chaining
     * @see HttpHeaders
     */
    public HttpSettings headers(final HttpHeaders headers) {
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
     * @return The HttpHeaders object (never null)
     * @see HttpHeaders
     * 
     * <p>Example:</p>
     * <pre>{@code
     * HttpHeaders headers = settings.headers();
     * headers.set("X-Custom-Header", "value");
     * }</pre>
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
     * @return A new HttpSettings instance with the same configuration
     * 
     * <p>Example:</p>
     * <pre>{@code
     * HttpSettings original = HttpSettings.create()
     *     .setConnectionTimeout(5000)
     *     .header("Authorization", "Bearer token");
     * 
     * HttpSettings copy = original.copy();
     * // copy has the same settings as original
     * }</pre>
     */
    public HttpSettings copy() {
        final HttpSettings copy = new HttpSettings().setConnectionTimeout(connectionTimeout)
                .setReadTimeout(readTimeout)
                .setSSLSocketFactory(sslSocketFactory)
                .setProxy(proxy)
                .setUseCaches(useCaches)
                .doInput(doInput)
                .doOutput(doOutput)
                .isOneWayRequest(isOneWayRequest)
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
     * @return A string representation of this object
     */
    @Override
    public String toString() {
        return "{connectionTimeout=" + connectionTimeout + ", readTimeout=" + readTimeout + ", sslSocketFactory=" + sslSocketFactory + ", proxy=" + proxy
                + ", useCaches=" + useCaches + ", doInput=" + doInput + ", doOutput=" + doOutput + ", isOneWayRequest=" + isOneWayRequest + ", contentFormat="
                + contentFormat + ", headers=" + headers + "}";
    }

}