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
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.SuppressFBWarnings;
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
import okhttp3.internal.Util;

/**
 * A fluent HTTP request builder and executor based on OkHttp.
 * This class provides a convenient API for building and executing HTTP requests with various features
 * such as headers, query parameters, request bodies, and authentication.
 * 
 * <p>Note: This class contains the codes and docs copied from: <a href="https://square.github.io/okhttp/">OkHttp</a> under Apache License v2 and may be modified.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Simple GET request
 * Response response = OkHttpRequest.url("https://api.example.com/users")
 *     .header("Accept", "application/json")
 *     .get();
 * 
 * // POST request with JSON body
 * User user = new User("John", "Doe");
 * String result = OkHttpRequest.url("https://api.example.com/users")
 *     .jsonBody(user)
 *     .post(String.class);
 * 
 * // Asynchronous request
 * ContinuableFuture<Response> future = OkHttpRequest.url("https://api.example.com/data")
 *     .asyncGet();
 * }</pre>
 *
 * @see URLEncodedUtil
 * @see HttpHeaders
 */
public final class OkHttpRequest {

    private static final MediaType APPLICATION_JSON_MEDIA_TYPE = MediaType.get(HttpHeaders.Values.APPLICATION_JSON);
    private static final MediaType APPLICATION_XML_MEDIA_TYPE = MediaType.get(HttpHeaders.Values.APPLICATION_XML);

    private static final KryoParser kryoParser = ParserFactory.isKryoAvailable() ? ParserFactory.createKryoParser() : null;

    private static final OkHttpClient defaultClient = new OkHttpClient();

    private final String url;
    private final HttpUrl httpUrl;

    private Object query;

    private final OkHttpClient httpClient;
    private final Request.Builder requestBuilder;
    private RequestBody body;

    @SuppressFBWarnings("URF_UNREAD_FIELD")
    private boolean closeHttpClientAfterExecution = false;

    OkHttpRequest(final String url, final HttpUrl httpUrl, final OkHttpClient httpClient) {
        N.checkArgument(!(Strings.isEmpty(url) && httpUrl == null), "'url' can't be null or empty");

        this.url = url;
        this.httpUrl = httpUrl;
        this.httpClient = httpClient;
        requestBuilder = new Request.Builder();
    }

    /**
     * Creates a new OkHttpRequest instance with the specified URL and HTTP client.
     *
     * @param url the URL string for the request
     * @param httpClient the OkHttpClient to use for executing the request
     * @return a new OkHttpRequest instance
     */
    public static OkHttpRequest create(final String url, final OkHttpClient httpClient) {
        return new OkHttpRequest(url, null, httpClient);
    }

    /**
     * Creates a new OkHttpRequest instance with the specified URL and HTTP client.
     *
     * @param url the URL object for the request
     * @param httpClient the OkHttpClient to use for executing the request
     * @return a new OkHttpRequest instance
     */
    public static OkHttpRequest create(final URL url, final OkHttpClient httpClient) {
        return new OkHttpRequest(null, HttpUrl.get(url), httpClient);
    }

    /**
     * Creates a new OkHttpRequest instance with the specified HttpUrl and HTTP client.
     *
     * @param url the HttpUrl object for the request
     * @param httpClient the OkHttpClient to use for executing the request
     * @return a new OkHttpRequest instance
     */
    public static OkHttpRequest create(final HttpUrl url, final OkHttpClient httpClient) {
        return new OkHttpRequest(null, url, httpClient);
    }

    /**
     * Creates a new OkHttpRequest instance with the specified URL using the default HTTP client.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * OkHttpRequest request = OkHttpRequest.url("https://api.example.com/users");
     * }</pre>
     *
     * @param url the URL string for the request
     * @return a new OkHttpRequest instance
     * @throws IllegalArgumentException if {@code url} is not a valid HTTP or HTTPS URL. Avoid this exception by calling {@link HttpUrl#parse}; it returns {@code null} for invalid URLs.
     */
    public static OkHttpRequest url(final String url) {
        return create(url, defaultClient);
    }

    /**
     * Creates a new OkHttpRequest instance with the specified URL using the default HTTP client.
     *
     * @param url the URL object for the request
     * @return a new OkHttpRequest instance
     * @throws IllegalArgumentException if the scheme of {@code url} is not {@code http} or {@code https}.
     */
    public static OkHttpRequest url(final URL url) {
        return create(url, defaultClient);
    }

    /**
     * Creates a new OkHttpRequest instance with the specified HttpUrl using the default HTTP client.
     *
     * @param url the HttpUrl object for the request
     * @return a new OkHttpRequest instance
     */
    public static OkHttpRequest url(final HttpUrl url) {
        return create(url, defaultClient);
    }

    /**
     * Creates a new OkHttpRequest instance with the specified URL and timeout settings.
     * A new HTTP client is created with the specified timeouts and will be closed after execution.
     *
     * @param url the URL string for the request
     * @param connectionTimeoutInMillis the connection timeout in milliseconds
     * @param readTimeoutInMillis the read timeout in milliseconds
     * @return a new OkHttpRequest instance
     */
    public static OkHttpRequest url(final String url, final long connectionTimeoutInMillis, final long readTimeoutInMillis) {
        return create(url,
                new OkHttpClient.Builder().connectTimeout(connectionTimeoutInMillis, TimeUnit.MILLISECONDS)
                        .readTimeout(readTimeoutInMillis, TimeUnit.MILLISECONDS)
                        .build()).closeHttpClientAfterExecution(true);
    }

    /**
     * Creates a new OkHttpRequest instance with the specified URL and timeout settings.
     * A new HTTP client is created with the specified timeouts and will be closed after execution.
     *
     * @param url the URL object for the request
     * @param connectionTimeoutInMillis the connection timeout in milliseconds
     * @param readTimeoutInMillis the read timeout in milliseconds
     * @return a new OkHttpRequest instance
     */
    public static OkHttpRequest url(final URL url, final long connectionTimeoutInMillis, final long readTimeoutInMillis) {
        return create(url,
                new OkHttpClient.Builder().connectTimeout(connectionTimeoutInMillis, TimeUnit.MILLISECONDS)
                        .readTimeout(readTimeoutInMillis, TimeUnit.MILLISECONDS)
                        .build()).closeHttpClientAfterExecution(true);
    }

    /**
     * Creates a new OkHttpRequest instance with the specified HttpUrl and timeout settings.
     * A new HTTP client is created with the specified timeouts and will be closed after execution.
     *
     * @param url the HttpUrl object for the request
     * @param connectionTimeoutInMillis the connection timeout in milliseconds
     * @param readTimeoutInMillis the read timeout in milliseconds
     * @return a new OkHttpRequest instance
     */
    public static OkHttpRequest url(final HttpUrl url, final long connectionTimeoutInMillis, final long readTimeoutInMillis) {
        return create(url,
                new OkHttpClient.Builder().connectTimeout(connectionTimeoutInMillis, TimeUnit.MILLISECONDS)
                        .readTimeout(readTimeoutInMillis, TimeUnit.MILLISECONDS)
                        .build()).closeHttpClientAfterExecution(true);
    }

    OkHttpRequest closeHttpClientAfterExecution(final boolean b) {
        closeHttpClientAfterExecution = b;

        return this;
    }

    /**
     * Sets this request's {@code Cache-Control} header, replacing any cache control headers already
     * present. If {@code cacheControl} doesn't define any directives, this clears this request's
     * cache-control headers.
     *
     * @param cacheControl the cache control directives
     * @return this OkHttpRequest instance for method chaining
     */
    public OkHttpRequest cacheControl(final CacheControl cacheControl) {
        requestBuilder.cacheControl(cacheControl);
        return this;
    }

    /**
     * Attaches {@code tag} to the request. It can be used later to cancel the request. If the tag
     * is unspecified or {@code null}, the request is canceled by using the request itself as the tag.
     *
     * @param tag the tag to attach to the request
     * @return this OkHttpRequest instance for method chaining
     */
    public OkHttpRequest tag(@Nullable final Object tag) {
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
     * @param <T> the type of the tag
     * @param type the class type used as a key for the tag
     * @param tag the tag to attach, or null to remove existing tag
     * @return this OkHttpRequest instance for method chaining
     */
    public <T> OkHttpRequest tag(final Class<? super T> type, @Nullable final T tag) {
        requestBuilder.tag(type, tag);
        return this;
    }

    /**
     * Sets the Basic Authentication header for this request.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * OkHttpRequest.url("https://api.example.com/secure")
     *     .basicAuth("username", "password")
     *     .get();
     * }</pre>
     *
     * @param user the username for authentication
     * @param password the password for authentication
     * @return this OkHttpRequest instance for method chaining
     */
    public OkHttpRequest basicAuth(final String user, final Object password) {
        requestBuilder.header(HttpHeaders.Names.AUTHORIZATION, "Basic " + Strings.base64Encode((user + ":" + password).getBytes(Charsets.UTF_8)));
        return this;
    }

    /**
     * Sets the header named {@code name} to {@code value}.
     * If this request already has any headers with that name, they are all replaced.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * OkHttpRequest.url("https://api.example.com/data")
     *     .header("Accept", "application/json")
     *     .header("User-Agent", "MyApp/1.0")
     *     .get();
     * }</pre>
     *
     * @param name the header name
     * @param value the header value
     * @return this OkHttpRequest instance for method chaining
     * @see Request.Builder#header(String, String)
     * @see HttpHeaders
     */
    public OkHttpRequest header(final String name, final String value) {
        requestBuilder.header(name, value);
        return this;
    }

    /**
     * Sets HTTP headers specified by {@code name1/value1}, {@code name2/value2}.
     * If this request already has any headers with that name, they are all replaced.
     *
     * @param name1 the first header name
     * @param value1 the first header value
     * @param name2 the second header name
     * @param value2 the second header value
     * @return this OkHttpRequest instance for method chaining
     * @see Request.Builder#header(String, String)
     * @see HttpHeaders
     */
    public OkHttpRequest headers(final String name1, final String value1, final String name2, final String value2) {
        requestBuilder.header(name1, value1);
        requestBuilder.header(name2, value2);

        return this;
    }

    /**
     * Sets HTTP headers specified by {@code name1/value1}, {@code name2/value2}, {@code name3/value3}.
     * If this request already has any headers with that name, they are all replaced.
     *
     * @param name1 the first header name
     * @param value1 the first header value
     * @param name2 the second header name
     * @param value2 the second header value
     * @param name3 the third header name
     * @param value3 the third header value
     * @return this OkHttpRequest instance for method chaining
     * @see Request.Builder#header(String, String)
     * @see HttpHeaders
     */
    public OkHttpRequest headers(final String name1, final String value1, final String name2, final String value2, final String name3, final String value3) {
        requestBuilder.header(name1, value1);
        requestBuilder.header(name2, value2);
        requestBuilder.header(name3, value3);

        return this;
    }

    /**
     * Sets HTTP headers specified by the key/value entries from the provided Map.
     * If this request already has any headers with that name, they are all replaced.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Map<String, String> headers = new HashMap<>();
     * headers.put("Accept", "application/json");
     * headers.put("Authorization", "Bearer token123");
     * 
     * OkHttpRequest.url("https://api.example.com/data")
     *     .headers(headers)
     *     .get();
     * }</pre>
     *
     * @param headers a map containing header names and values
     * @return this OkHttpRequest instance for method chaining
     * @see Request.Builder#header(String, String)
     * @see HttpHeaders
     */
    public OkHttpRequest headers(final Map<String, ?> headers) {
        if (N.notEmpty(headers)) {
            for (final Map.Entry<String, ?> entry : headers.entrySet()) {
                requestBuilder.header(entry.getKey(), HttpHeaders.valueOf(entry.getValue()));
            }
        }

        return this;
    }

    /**
     * Removes all headers on this builder and adds {@code headers}.
     *
     * @param headers the Headers object containing all headers to set
     * @return this OkHttpRequest instance for method chaining
     * @see Request.Builder#headers(Headers)
     * @see HttpHeaders
     */
    public OkHttpRequest headers(final Headers headers) {
        requestBuilder.headers(headers);
        return this;
    }

    /**
     * Removes all headers on this builder and adds {@code headers}.
     *
     * @param headers the HttpHeaders object containing all headers to set
     * @return this OkHttpRequest instance for method chaining
     * @see Request.Builder#headers(Headers)
     * @see HttpHeaders
     */
    public OkHttpRequest headers(final HttpHeaders headers) {
        if (headers != null && !headers.isEmpty()) {
            for (final String headerName : headers.headerNameSet()) {
                requestBuilder.header(headerName, HttpHeaders.valueOf(headers.get(headerName)));
            }
        }

        return this;
    }

    /**
     * Adds a header with {@code name} and {@code value}. Prefer this method for multiply-valued
     * headers like "Cookie".
     *
     * <p>Note that for some headers including {@code Content-Length} and {@code Content-Encoding},
     * OkHttp may replace {@code value} with a header derived from the request body.</p>
     *
     * @param name the header name
     * @param value the header value
     * @return this OkHttpRequest instance for method chaining
     * @deprecated no use case?
     */
    @Deprecated
    public OkHttpRequest addHeader(final String name, final String value) {
        requestBuilder.addHeader(name, value);
        return this;
    }

    /**
     * Removes all headers with the specified name from this request.
     *
     * @param name the name of the headers to remove
     * @return this OkHttpRequest instance for method chaining
     * @deprecated no use case?
     */
    @Deprecated
    public OkHttpRequest removeHeader(final String name) {
        requestBuilder.removeHeader(name);
        return this;
    }

    /**
     * Sets query parameters for {@code GET} or {@code DELETE} request.
     * The query string will be appended to the URL.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * OkHttpRequest.url("https://api.example.com/search")
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
     * Sets query parameters for {@code GET} or {@code DELETE} request.
     * The parameters will be URL-encoded and appended to the URL.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Map<String, Object> params = new HashMap<>();
     * params.put("q", "java programming");
     * params.put("limit", 10);
     * 
     * OkHttpRequest.url("https://api.example.com/search")
     *     .query(params)
     *     .get();
     * }</pre>
     *
     * @param queryParams a map containing query parameter names and values
     * @return this OkHttpRequest instance for method chaining
     */
    public OkHttpRequest query(final Map<String, ?> queryParams) {
        query = queryParams;

        return this;
    }

    /**
     * Sets the request body as JSON with Content-Type: application/json.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * String json = "{\"name\":\"John\",\"age\":30}";
     * OkHttpRequest.url("https://api.example.com/users")
     *     .jsonBody(json)
     *     .post();
     * }</pre>
     *
     * @param json the JSON string to send as the request body
     * @return this OkHttpRequest instance for method chaining
     */
    public OkHttpRequest jsonBody(final String json) {
        return body(json, APPLICATION_JSON_MEDIA_TYPE);
    }

    /**
     * Sets the request body as JSON with Content-Type: application/json.
     * The object will be serialized to JSON using the default JSON serializer.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * User user = new User("John", 30);
     * OkHttpRequest.url("https://api.example.com/users")
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
     * @param xml the XML string to send as the request body
     * @return this OkHttpRequest instance for method chaining
     */
    public OkHttpRequest xmlBody(final String xml) {
        return body(xml, APPLICATION_XML_MEDIA_TYPE);
    }

    /**
     * Sets the request body as XML with Content-Type: application/xml.
     * The object will be serialized to XML using the default XML serializer.
     *
     * @param obj the object to serialize to XML and send as the request body
     * @return this OkHttpRequest instance for method chaining
     */
    public OkHttpRequest xmlBody(final Object obj) {
        return body(N.toXml(obj), APPLICATION_XML_MEDIA_TYPE);
    }

    /**
     * Sets the request body as form data with Content-Type: application/x-www-form-urlencoded.
     * The map entries will be encoded as form fields.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Map<String, String> formData = new HashMap<>();
     * formData.put("username", "john_doe");
     * formData.put("password", "secret123");
     * 
     * OkHttpRequest.url("https://api.example.com/login")
     *     .formBody(formData)
     *     .post();
     * }</pre>
     *
     * @param formBodyByMap a map containing form field names and values
     * @return this OkHttpRequest instance for method chaining
     * @see {@code FormBody.Builder}
     */
    public OkHttpRequest formBody(final Map<?, ?> formBodyByMap) {
        if (N.isEmpty(formBodyByMap)) {
            body = Util.EMPTY_REQUEST;
            return this;
        }

        final FormBody.Builder formBodyBuilder = new FormBody.Builder();

        for (final Map.Entry<?, ?> entry : formBodyByMap.entrySet()) {
            formBodyBuilder.add(N.stringOf(entry.getKey()), N.stringOf(entry.getValue()));
        }

        body = formBodyBuilder.build();
        return this;
    }

    /**
     * Sets the request body as form data with Content-Type: application/x-www-form-urlencoded.
     * The bean properties will be encoded as form fields using getter methods.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * LoginRequest login = new LoginRequest();
     * login.setUsername("john_doe");
     * login.setPassword("secret123");
     * 
     * OkHttpRequest.url("https://api.example.com/login")
     *     .formBody(login)
     *     .post();
     * }</pre>
     *
     * @param formBodyByBean a bean object whose properties will be used as form fields
     * @return this OkHttpRequest instance for method chaining
     * @throws IllegalArgumentException if the provided object is not a bean class with getter/setter methods
     * @see {@code FormBody.Builder}
     */
    public OkHttpRequest formBody(final Object formBodyByBean) throws IllegalArgumentException {
        if (formBodyByBean == null) {
            body = Util.EMPTY_REQUEST;
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
     * Sets the request body as form data from a map.
     *
     * @param formBodyByMap a map containing form field names and values
     * @return this OkHttpRequest instance for method chaining
     * @see {@code FormBody.Builder}
     * @deprecated replaced by {@link #formBody(Map)}.
     */
    @Deprecated
    public OkHttpRequest body(final Map<?, ?> formBodyByMap) {
        return formBody(formBodyByMap);
    }

    /**
     * Sets the request body as form data from a bean object.
     *
     * @param formBodyByBean a bean object whose properties will be used as form fields
     * @return this OkHttpRequest instance for method chaining
     * @see {@code FormBody.Builder}
     * @deprecated replaced by {@link #formBody(Object)}.
     */
    @Deprecated
    public OkHttpRequest body(final Object formBodyByBean) {
        return formBody(formBodyByBean);
    }

    /**
     * Sets the request body with a custom RequestBody instance.
     * This allows full control over the request body content and media type.
     *
     * @param body the RequestBody to use
     * @return this OkHttpRequest instance for method chaining
     * @see {@code RequestBody}
     */
    public OkHttpRequest body(final RequestBody body) {
        this.body = body;
        return this;
    }

    /**
     * Sets the request body with the specified content and media type.
     *
     * @param content the string content of the request body
     * @param contentType the media type of the content, or null to use default
     * @return this OkHttpRequest instance for method chaining
     * @see RequestBody#create(MediaType, String)
     */
    public OkHttpRequest body(final String content, @Nullable final MediaType contentType) {
        body = RequestBody.create(content, contentType);

        return this;
    }

    /**
     * Sets the request body with the specified byte array content and media type.
     *
     * @param content the byte array content of the request body
     * @param contentType the media type of the content, or null to use default
     * @return this OkHttpRequest instance for method chaining
     * @see RequestBody#create(MediaType, byte[])
     */
    public OkHttpRequest body(final byte[] content, @Nullable final MediaType contentType) {
        body = RequestBody.create(content, contentType);

        return this;
    }

    /**
     * Sets the request body with the specified byte array content, offset, length, and media type.
     *
     * @param content the byte array content of the request body
     * @param offset the offset in the byte array to start reading from
     * @param byteCount the number of bytes to read from the array
     * @param contentType the media type of the content, or null to use default
     * @return this OkHttpRequest instance for method chaining
     * @see RequestBody#create(MediaType, byte[], int, int)
     */
    public OkHttpRequest body(final byte[] content, final int offset, final int byteCount, @Nullable final MediaType contentType) {
        body = RequestBody.create(content, contentType, offset, byteCount);

        return this;
    }

    /**
     * Sets the request body with the content from the specified file and media type.
     *
     * @param content the file containing the request body content
     * @param contentType the media type of the content, or null to use default
     * @return this OkHttpRequest instance for method chaining
     */
    public OkHttpRequest body(final File content, @Nullable final MediaType contentType) {
        body = RequestBody.create(content, contentType);

        return this;
    }

    /**
     * Executes a GET request and returns the response.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Response response = OkHttpRequest.url("https://api.example.com/users")
     *     .header("Accept", "application/json")
     *     .get();
     * 
     * if (response.isSuccessful()) {
     *     String body = response.body().string();
     * }
     * }</pre>
     *
     * @return the HTTP response
     * @throws IOException if the request could not be executed
     */
    public Response get() throws IOException {
        return execute(HttpMethod.GET);
    }

    /**
     * Executes a GET request and returns the response body deserialized to the specified type.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * List<User> users = OkHttpRequest.url("https://api.example.com/users")
     *     .get(new TypeToken<List<User>>(){}.getType());
     * }</pre>
     *
     * @param <T> the type of the result
     * @param resultClass the class of the result type
     * @return the deserialized response body
     * @throws IOException if the request could not be executed or the response indicates an error
     */
    public <T> T get(final Class<T> resultClass) throws IOException {
        return execute(HttpMethod.GET, resultClass);
    }

    /**
     * Executes a POST request and returns the response.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * User newUser = new User("John", "Doe");
     * Response response = OkHttpRequest.url("https://api.example.com/users")
     *     .jsonBody(newUser)
     *     .post();
     * }</pre>
     *
     * @return the HTTP response
     * @throws IOException if the request could not be executed
     */
    public Response post() throws IOException {
        return execute(HttpMethod.POST);
    }

    /**
     * Executes a POST request and returns the response body deserialized to the specified type.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * User newUser = new User("John", "Doe");
     * User createdUser = OkHttpRequest.url("https://api.example.com/users")
     *     .jsonBody(newUser)
     *     .post(User.class);
     * }</pre>
     *
     * @param <T> the type of the result
     * @param resultClass the class of the result type
     * @return the deserialized response body
     * @throws IOException if the request could not be executed or the response indicates an error
     */
    public <T> T post(final Class<T> resultClass) throws IOException {
        return execute(HttpMethod.POST, resultClass);
    }

    /**
     * Executes a PUT request and returns the response.
     *
     * @return the HTTP response
     * @throws IOException if the request could not be executed
     */
    public Response put() throws IOException {
        return execute(HttpMethod.PUT);
    }

    /**
     * Executes a PUT request and returns the response body deserialized to the specified type.
     *
     * @param <T> the type of the result
     * @param resultClass the class of the result type
     * @return the deserialized response body
     * @throws IOException if the request could not be executed or the response indicates an error
     */
    public <T> T put(final Class<T> resultClass) throws IOException {
        return execute(HttpMethod.PUT, resultClass);
    }

    /**
     * Executes a PATCH request and returns the response.
     *
     * @return the HTTP response
     * @throws IOException if the request could not be executed
     */
    public Response patch() throws IOException {
        return execute(HttpMethod.PATCH);
    }

    /**
     * Executes a PATCH request and returns the response body deserialized to the specified type.
     *
     * @param <T> the type of the result
     * @param resultClass the class of the result type
     * @return the deserialized response body
     * @throws IOException if the request could not be executed or the response indicates an error
     */
    public <T> T patch(final Class<T> resultClass) throws IOException {
        return execute(HttpMethod.PATCH, resultClass);
    }

    /**
     * Executes a DELETE request and returns the response.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Response response = OkHttpRequest.url("https://api.example.com/users/123")
     *     .delete();
     * }</pre>
     *
     * @return the HTTP response
     * @throws IOException if the request could not be executed
     */
    public Response delete() throws IOException {
        return execute(HttpMethod.DELETE);
    }

    /**
     * Executes a DELETE request and returns the response body deserialized to the specified type.
     *
     * @param <T> the type of the result
     * @param resultClass the class of the result type
     * @return the deserialized response body
     * @throws IOException if the request could not be executed or the response indicates an error
     */
    public <T> T delete(final Class<T> resultClass) throws IOException {
        return execute(HttpMethod.DELETE, resultClass);
    }

    /**
     * Executes a HEAD request and returns the response.
     * HEAD requests are used to retrieve headers without the response body.
     *
     * @return the HTTP response (with no body)
     * @throws IOException if the request could not be executed
     */
    public Response head() throws IOException {
        return execute(HttpMethod.HEAD);
    }

    /**
     * Executes an HTTP request with the specified method and returns the response.
     *
     * @param httpMethod the HTTP method to use (GET, POST, PUT, PATCH, DELETE, HEAD)
     * @return the HTTP response
     * @throws IOException if the request could not be executed
     */
    @Beta
    public Response execute(final HttpMethod httpMethod) throws IOException {
        // body = (body == null && HttpMethod.DELETE.equals(httpMethod)) ? Util.EMPTY_REQUEST : body;
        final Request request = createRequest(httpMethod);

        return execute(request);
    }

    /**
     * Executes an HTTP request with the specified method and returns the response body deserialized to the specified type.
     *
     * @param <T> the type of the result
     * @param httpMethod the HTTP method to use (GET, POST, PUT, PATCH, DELETE, HEAD)
     * @param resultClass the class of the result type
     * @return the deserialized response body
     * @throws IllegalArgumentException if resultClass is HttpResponse
     * @throws IOException if the request could not be executed or the response indicates an error
     */
    @Beta
    public <T> T execute(final HttpMethod httpMethod, final Class<T> resultClass) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(resultClass, cs.resultClass);
        N.checkArgument(!HttpResponse.class.equals(resultClass), "Return type can't be HttpResponse");
        final Request request = createRequest(httpMethod);

        final Response resp = execute(request);

        if (Response.class.equals(resultClass)) {
            return (T) resp;
        }

        try {
            if (resultClass == null || resultClass.equals(Void.class)) {
                return null;
            } else if (resp.isSuccessful()) {
                final String contentType = request.header(HttpHeaders.Names.CONTENT_TYPE);
                final String contentEncoding = request.header(HttpHeaders.Names.CONTENT_ENCODING);
                final ContentFormat requestContentFormat = HttpUtil.getContentFormat(contentType, contentEncoding);
                final Charset requestCharset = HttpUtil.getCharset(contentType);
                final Map<String, List<String>> respHeaders = resp.headers().toMultimap();
                final Charset respCharset = HttpUtil.getResponseCharset(respHeaders, requestCharset);
                final ContentFormat respContentFormat = HttpUtil.getResponseContentFormat(respHeaders, requestContentFormat);
                //noinspection DataFlowIssue
                final InputStream is = HttpUtil.wrapInputStream(resp.body().byteStream(), respContentFormat);

                if (resultClass.equals(String.class)) {
                    return (T) IOUtil.readAllToString(is, respCharset);
                } else if (byte[].class.equals(resultClass)) {
                    return (T) IOUtil.readAllBytes(is);
                } else {
                    if (respContentFormat == ContentFormat.KRYO && kryoParser != null) {
                        return kryoParser.deserialize(is, resultClass);
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
            } else {
                throw new IOException(resp.code() + ": " + resp.message());
            }
        } finally {
            try {
                IOUtil.close(resp);
            } finally {
                doAfterExecution();
            }
        }
    }

    private Response execute(final Request request) throws IOException {
        try {
            return httpClient.newCall(request).execute();
        } finally {
            doAfterExecution();
        }
    }

    void doAfterExecution() {
        if (closeHttpClientAfterExecution) {
            // Shutdown isn't necessary?
        }
    }

    private Request createRequest(final HttpMethod httpMethod) {
        if (query == null || (query instanceof String && Strings.isEmpty((String) query))) {
            if (httpUrl == null) {
                requestBuilder.url(HttpUrl.get(url));
            } else {
                requestBuilder.url(httpUrl);
            }
        } else {
            if (httpUrl == null) {
                requestBuilder.url(HttpUrl.get(URLEncodedUtil.encode(url, query)));
            } else {
                requestBuilder.url(HttpUrl.get(URLEncodedUtil.encode(httpUrl.toString(), query)));
            }
        }

        return requestBuilder.method(httpMethod.name(), body).build();
    }

    /**
     * Executes a GET request asynchronously using the default executor.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * ContinuableFuture<Response> future = OkHttpRequest.url("https://api.example.com/users")
     *     .asyncGet();
     * 
     * future.getThenAccept(response -> {
     *     if (response.isSuccessful()) {
     *         // Process response
     *     }
     * });
     * }</pre>
     *
     * @return a ContinuableFuture that will complete with the HTTP response
     */
    public ContinuableFuture<Response> asyncGet() {
        return asyncGet(HttpUtil.DEFAULT_EXECUTOR);
    }

    /**
     * Executes a GET request asynchronously using the specified executor.
     *
     * @param executor the executor to use for the asynchronous operation
     * @return a ContinuableFuture that will complete with the HTTP response
     */
    public ContinuableFuture<Response> asyncGet(final Executor executor) {
        return ContinuableFuture.call(this::get, executor);
    }

    /**
     * Executes a GET request asynchronously and returns the response body deserialized to the specified type.
     * Uses the default executor.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * ContinuableFuture<List<User>> future = OkHttpRequest.url("https://api.example.com/users")
     *     .asyncGet(new TypeToken<List<User>>(){}.getType());
     * 
     * future.getThenAccept(users -> {
     *     // Process users
     * });
     * }</pre>
     *
     * @param <T> the type of the result
     * @param resultClass the class of the result type
     * @return a ContinuableFuture that will complete with the deserialized response body
     */
    public <T> ContinuableFuture<T> asyncGet(final Class<T> resultClass) {
        return asyncGet(resultClass, HttpUtil.DEFAULT_EXECUTOR);
    }

    /**
     * Executes a GET request asynchronously using the specified executor and returns the response body deserialized to the specified type.
     *
     * @param <T> the type of the result
     * @param resultClass the class of the result type
     * @param executor the executor to use for the asynchronous operation
     * @return a ContinuableFuture that will complete with the deserialized response body
     */
    public <T> ContinuableFuture<T> asyncGet(final Class<T> resultClass, final Executor executor) {
        return ContinuableFuture.call(() -> get(resultClass), executor);
    }

    /**
     * Executes a POST request asynchronously using the default executor.
     *
     * @return a ContinuableFuture that will complete with the HTTP response
     */
    public ContinuableFuture<Response> asyncPost() {
        return asyncPost(HttpUtil.DEFAULT_EXECUTOR);
    }

    /**
     * Executes a POST request asynchronously using the specified executor.
     *
     * @param executor the executor to use for the asynchronous operation
     * @return a ContinuableFuture that will complete with the HTTP response
     */
    public ContinuableFuture<Response> asyncPost(final Executor executor) {
        return ContinuableFuture.call(this::post, executor);
    }

    /**
     * Executes a POST request asynchronously and returns the response body deserialized to the specified type.
     * Uses the default executor.
     *
     * @param <T> the type of the result
     * @param resultClass the class of the result type
     * @return a ContinuableFuture that will complete with the deserialized response body
     */
    public <T> ContinuableFuture<T> asyncPost(final Class<T> resultClass) {
        return asyncPost(resultClass, HttpUtil.DEFAULT_EXECUTOR);
    }

    /**
     * Executes a POST request asynchronously using the specified executor and returns the response body deserialized to the specified type.
     *
     * @param <T> the type of the result
     * @param resultClass the class of the result type
     * @param executor the executor to use for the asynchronous operation
     * @return a ContinuableFuture that will complete with the deserialized response body
     */
    public <T> ContinuableFuture<T> asyncPost(final Class<T> resultClass, final Executor executor) {
        return ContinuableFuture.call(() -> post(resultClass), executor);
    }

    /**
     * Executes a PUT request asynchronously using the default executor.
     *
     * @return a ContinuableFuture that will complete with the HTTP response
     */
    public ContinuableFuture<Response> asyncPut() {
        return asyncPut(HttpUtil.DEFAULT_EXECUTOR);
    }

    /**
     * Executes a PUT request asynchronously using the specified executor.
     *
     * @param executor the executor to use for the asynchronous operation
     * @return a ContinuableFuture that will complete with the HTTP response
     */
    public ContinuableFuture<Response> asyncPut(final Executor executor) {
        return ContinuableFuture.call(this::put, executor);
    }

    /**
     * Executes a PUT request asynchronously and returns the response body deserialized to the specified type.
     * Uses the default executor.
     *
     * @param <T> the type of the result
     * @param resultClass the class of the result type
     * @return a ContinuableFuture that will complete with the deserialized response body
     */
    public <T> ContinuableFuture<T> asyncPut(final Class<T> resultClass) {
        return asyncPut(resultClass, HttpUtil.DEFAULT_EXECUTOR);
    }

    /**
     * Executes a PUT request asynchronously using the specified executor and returns the response body deserialized to the specified type.
     *
     * @param <T> the type of the result
     * @param resultClass the class of the result type
     * @param executor the executor to use for the asynchronous operation
     * @return a ContinuableFuture that will complete with the deserialized response body
     */
    public <T> ContinuableFuture<T> asyncPut(final Class<T> resultClass, final Executor executor) {
        return ContinuableFuture.call(() -> put(resultClass), executor);
    }

    /**
     * Executes a PATCH request asynchronously using the default executor.
     *
     * @return a ContinuableFuture that will complete with the HTTP response
     */
    public ContinuableFuture<Response> asyncPatch() {
        return asyncPatch(HttpUtil.DEFAULT_EXECUTOR);
    }

    /**
     * Executes a PATCH request asynchronously using the specified executor.
     *
     * @param executor the executor to use for the asynchronous operation
     * @return a ContinuableFuture that will complete with the HTTP response
     */
    public ContinuableFuture<Response> asyncPatch(final Executor executor) {
        return ContinuableFuture.call(this::patch, executor);
    }

    /**
     * Executes a PATCH request asynchronously and returns the response body deserialized to the specified type.
     * Uses the default executor.
     *
     * @param <T> the type of the result
     * @param resultClass the class of the result type
     * @return a ContinuableFuture that will complete with the deserialized response body
     */
    public <T> ContinuableFuture<T> asyncPatch(final Class<T> resultClass) {
        return asyncPatch(resultClass, HttpUtil.DEFAULT_EXECUTOR);
    }

    /**
     * Executes a PATCH request asynchronously using the specified executor and returns the response body deserialized to the specified type.
     *
     * @param <T> the type of the result
     * @param resultClass the class of the result type
     * @param executor the executor to use for the asynchronous operation
     * @return a ContinuableFuture that will complete with the deserialized response body
     */
    public <T> ContinuableFuture<T> asyncPatch(final Class<T> resultClass, final Executor executor) {
        return ContinuableFuture.call(() -> patch(resultClass), executor);
    }

    /**
     * Executes a DELETE request asynchronously using the default executor.
     *
     * @return a ContinuableFuture that will complete with the HTTP response
     */
    public ContinuableFuture<Response> asyncDelete() {
        return asyncDelete(HttpUtil.DEFAULT_EXECUTOR);
    }

    /**
     * Executes a DELETE request asynchronously using the specified executor.
     *
     * @param executor the executor to use for the asynchronous operation
     * @return a ContinuableFuture that will complete with the HTTP response
     */
    public ContinuableFuture<Response> asyncDelete(final Executor executor) {
        return ContinuableFuture.call(this::delete, executor);
    }

    /**
     * Executes a DELETE request asynchronously and returns the response body deserialized to the specified type.
     * Uses the default executor.
     *
     * @param <T> the type of the result
     * @param resultClass the class of the result type
     * @return a ContinuableFuture that will complete with the deserialized response body
     */
    public <T> ContinuableFuture<T> asyncDelete(final Class<T> resultClass) {
        return asyncDelete(resultClass, HttpUtil.DEFAULT_EXECUTOR);
    }

    /**
     * Executes a DELETE request asynchronously using the specified executor and returns the response body deserialized to the specified type.
     *
     * @param <T> the type of the result
     * @param resultClass the class of the result type
     * @param executor the executor to use for the asynchronous operation
     * @return a ContinuableFuture that will complete with the deserialized response body
     */
    public <T> ContinuableFuture<T> asyncDelete(final Class<T> resultClass, final Executor executor) {
        return ContinuableFuture.call(() -> delete(resultClass), executor);
    }

    /**
     * Executes a HEAD request asynchronously using the default executor.
     *
     * @return a ContinuableFuture that will complete with the HTTP response
     */
    public ContinuableFuture<Response> asyncHead() {
        return asyncHead(HttpUtil.DEFAULT_EXECUTOR);
    }

    /**
     * Executes a HEAD request asynchronously using the specified executor.
     *
     * @param executor the executor to use for the asynchronous operation
     * @return a ContinuableFuture that will complete with the HTTP response
     */
    public ContinuableFuture<Response> asyncHead(final Executor executor) {
        return ContinuableFuture.call(this::head, executor);
    }

    /**
     * Executes an HTTP request asynchronously with the specified method using the default executor.
     *
     * @param httpMethod the HTTP method to use (GET, POST, PUT, PATCH, DELETE, HEAD)
     * @return a ContinuableFuture that will complete with the HTTP response
     */
    @Beta
    public ContinuableFuture<Response> asyncExecute(final HttpMethod httpMethod) {
        return asyncExecute(httpMethod, HttpUtil.DEFAULT_EXECUTOR);
    }

    /**
     * Executes an HTTP request asynchronously with the specified method using the specified executor.
     *
     * @param httpMethod the HTTP method to use (GET, POST, PUT, PATCH, DELETE, HEAD)
     * @param executor the executor to use for the asynchronous operation
     * @return a ContinuableFuture that will complete with the HTTP response
     */
    @Beta
    public ContinuableFuture<Response> asyncExecute(final HttpMethod httpMethod, final Executor executor) {
        return ContinuableFuture.call(() -> execute(httpMethod), executor);
    }

    /**
     * Executes an HTTP request asynchronously with the specified method and returns the response body deserialized to the specified type.
     * Uses the default executor.
     *
     * @param <T> the type of the result
     * @param httpMethod the HTTP method to use (GET, POST, PUT, PATCH, DELETE, HEAD)
     * @param resultClass the class of the result type
     * @return a ContinuableFuture that will complete with the deserialized response body
     */
    @Beta
    public <T> ContinuableFuture<T> asyncExecute(final HttpMethod httpMethod, final Class<T> resultClass) {
        return asyncExecute(httpMethod, resultClass, HttpUtil.DEFAULT_EXECUTOR);
    }

    /**
     * Executes an HTTP request asynchronously with the specified method using the specified executor and returns the response body deserialized to the specified type.
     *
     * @param <T> the type of the result
     * @param httpMethod the HTTP method to use (GET, POST, PUT, PATCH, DELETE, HEAD)
     * @param resultClass the class of the result type
     * @param executor the executor to use for the asynchronous operation
     * @return a ContinuableFuture that will complete with the deserialized response body
     */
    @Beta
    public <T> ContinuableFuture<T> asyncExecute(final HttpMethod httpMethod, final Class<T> resultClass, final Executor executor) {
        return ContinuableFuture.call(() -> execute(httpMethod, resultClass), executor);
    }
}