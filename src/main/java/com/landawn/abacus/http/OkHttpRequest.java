/*
 * Copyright (C) 2019 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.http;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import com.landawn.abacus.parser.KryoParser;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.parser.XMLParser;
import com.landawn.abacus.util.BufferedReader;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.ContinuableFuture;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.URLEncodedUtil;

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
 * Note: This class contains the codes and docs copied from OkHttp: https://square.github.io/okhttp/ under Apache License v2.
 *
 * @since 1.3
 */
public final class OkHttpRequest {

    private static final MediaType APPLICATION_JSON_MEDIA_TYPE = MediaType.get("application/json");

    static final XMLParser xmlParser = ParserFactory.isXMLAvailable() ? ParserFactory.createXMLParser() : null;
    static final KryoParser kryoParser = ParserFactory.isKryoAvailable() ? ParserFactory.createKryoParser() : null;

    static final OkHttpClient defaultClient = new OkHttpClient();

    OkHttpClient httpClient;
    final Request.Builder builder = new Request.Builder();
    RequestBody body;
    Request request;

    OkHttpRequest(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public static OkHttpRequest create(final String url, final OkHttpClient httpClient) {
        final OkHttpRequest okHttpRequest = new OkHttpRequest(httpClient);
        okHttpRequest.builder.url(url);
        return okHttpRequest;
    }

    public static OkHttpRequest create(final URL url, final OkHttpClient httpClient) {
        final OkHttpRequest okHttpRequest = new OkHttpRequest(httpClient);
        okHttpRequest.builder.url(url);
        return okHttpRequest;
    }

    public static OkHttpRequest create(final HttpUrl url, final OkHttpClient httpClient) {
        final OkHttpRequest okHttpRequest = new OkHttpRequest(httpClient);
        okHttpRequest.builder.url(url);
        return okHttpRequest;
    }

    /**
     * Sets the URL target of this request.
     *
     * @throws IllegalArgumentException if {@code url} is not a valid HTTP or HTTPS URL. Avoid this
     * exception by calling {@link HttpUrl#parse}; it returns null for invalid URLs.
     */
    public static OkHttpRequest url(final String url) {
        return create(url, defaultClient);
    }

    /**
     * Sets the URL target of this request.
     *
     * @throws IllegalArgumentException if the scheme of {@code url} is not {@code http} or {@code
     * https}.
     */
    public static OkHttpRequest url(URL url) {
        return create(url, defaultClient);
    }

    public static OkHttpRequest url(HttpUrl url) {
        return create(url, defaultClient);
    }

    public static OkHttpRequest url(final String url, final long connectionTimeoutInMillis, final long readTimeoutInMillis) {
        return create(url,
                new OkHttpClient.Builder().connectTimeout(connectionTimeoutInMillis, TimeUnit.MILLISECONDS)
                        .readTimeout(readTimeoutInMillis, TimeUnit.MILLISECONDS)
                        .build());
    }

    public static OkHttpRequest url(final URL url, final long connectionTimeoutInMillis, final long readTimeoutInMillis) {
        return create(url,
                new OkHttpClient.Builder().connectTimeout(connectionTimeoutInMillis, TimeUnit.MILLISECONDS)
                        .readTimeout(readTimeoutInMillis, TimeUnit.MILLISECONDS)
                        .build());
    }

    public static OkHttpRequest url(final HttpUrl url, final long connectionTimeoutInMillis, final long readTimeoutInMillis) {
        return create(url,
                new OkHttpClient.Builder().connectTimeout(connectionTimeoutInMillis, TimeUnit.MILLISECONDS)
                        .readTimeout(readTimeoutInMillis, TimeUnit.MILLISECONDS)
                        .build());
    }

    /**
     * Sets this request's {@code Cache-Control} header, replacing any cache control headers already
     * present. If {@code cacheControl} doesn't define any directives, this clears this request's
     * cache-control headers.
     */
    public OkHttpRequest cacheControl(CacheControl cacheControl) {
        builder.cacheControl(cacheControl);
        return this;
    }

    /**
     * Attaches {@code tag} to the request. It can be used later to cancel the request. If the tag
     * is unspecified or null, the request is canceled by using the request itself as the tag.
     */
    public OkHttpRequest tag(@Nullable Object tag) {
        builder.tag(tag);
        return this;
    }

    /**
     * Attaches {@code tag} to the request using {@code type} as a key. Tags can be read from a
     * request using {@link Request#tag}. Use null to remove any existing tag assigned for {@code
     * type}.
     *
     * <p>Use this API to attach timing, debugging, or other application data to a request so that
     * you may read it in interceptors, event listeners, or callbacks.
     */
    public <T> OkHttpRequest tag(Class<? super T> type, @Nullable T tag) {
        builder.tag(type, tag);
        return this;
    }

    /**
     *
     * @param user
     * @param password
     * @return
     */
    public OkHttpRequest basicAuth(String user, Object password) {
        builder.header(HttpHeaders.Names.AUTHORIZATION, "Basic " + N.base64Encode((user + ":" + password).getBytes()));
        return this;
    }

    /**
     * Sets the header named {@code name} to {@code value}.
     * If this request already has any headers with that name, they are all replaced.
     *
     * @param name
     * @param value
     * @return
     * @see Request.Builder#header(String, String)
     */
    public OkHttpRequest header(String name, String value) {
        builder.header(name, value);
        return this;
    }

    /**
     * Set http headers specified by {@code name1/value1}, {@code name2/value2}.
     * If this request already has any headers with that name, they are all replaced.
     *
     * @param name1
     * @param value1
     * @param name2
     * @param value2
     * @return
     * @see Request.Builder#header(String, String)
     */
    public OkHttpRequest headers(String name1, String value1, String name2, String value2) {
        builder.header(name1, value1);
        builder.header(name2, value2);

        return this;
    }

    /**
     * Set http headers specified by {@code name1/value1}, {@code name2/value2}, {@code name3/value3}.
     * If this request already has any headers with that name, they are all replaced.
     *
     * @param name1
     * @param value1
     * @param name2
     * @param value2
     * @param name3
     * @param value3
     * @return
     * @see Request.Builder#header(String, String)
     */
    public OkHttpRequest headers(String name1, String value1, String name2, String value2, String name3, String value3) {
        builder.header(name1, value1);
        builder.header(name2, value2);
        builder.header(name3, value3);

        return this;
    }

    /**
     * Set http headers specified by the key/value entities from {@code Map}.
     * If this request already has any headers with that name, they are all replaced.
     *
     * @param headers
     * @return
     * @see Request.Builder#header(String, String)
     */
    public OkHttpRequest headers(final Map<String, ?> headers) {
        if (N.notNullOrEmpty(headers)) {
            for (Map.Entry<String, ?> entry : headers.entrySet()) {
                builder.header(entry.getKey(), HttpHeaders.valueOf(entry.getValue()));
            }
        }

        return this;
    }

    /**
     * Removes all headers on this builder and adds {@code headers}
     *
     * @param headers
     * @return
     * @see Request.Builder#headers(Headers)
     */
    public OkHttpRequest headers(Headers headers) {
        builder.headers(headers);
        return this;
    }

    /**
     * Removes all headers on this builder and adds {@code headers}
     *
     * @param headers
     * @return
     * @see Request.Builder#headers(Headers)
     */
    public OkHttpRequest headers(HttpHeaders headers) {
        if (headers != null && !headers.isEmpty()) {
            for (String headerName : headers.headerNameSet()) {
                builder.header(headerName, HttpHeaders.valueOf(headers.get(headerName)));
            }
        }

        return this;
    }

    /**
     * Adds a header with {@code name} and {@code value}. Prefer this method for multiply-valued
     * headers like "Cookie".
     *
     * <p>Note that for some headers including {@code Content-Length} and {@code Content-Encoding},
     * OkHttp may replace {@code value} with a header derived from the request body.
     * @deprecated no use case?
     */
    @Deprecated
    public OkHttpRequest addHeader(String name, String value) {
        builder.addHeader(name, value);
        return this;
    }

    /**
     *
     * @param name
     * @return
     * @deprecated no use case?
     */
    @Deprecated
    public OkHttpRequest removeHeader(String name) {
        builder.removeHeader(name);
        return this;
    }

    /**
     *
     * @param json
     * @return
     */
    public OkHttpRequest jsonBody(final String json) {
        return body(json, APPLICATION_JSON_MEDIA_TYPE);
    }

    /**
     *
     * @param obj
     * @return
     */
    public OkHttpRequest jsonBody(final Object obj) {
        return body(N.toJSON(obj), APPLICATION_JSON_MEDIA_TYPE);
    }

    /**
     *
     * @param formBodyByMap
     * @return
     * @see {@code FormBody.Builder}
     */
    public OkHttpRequest formBody(final Map<?, ?> formBodyByMap) {
        if (N.isNullOrEmpty(formBodyByMap)) {
            this.body = Util.EMPTY_REQUEST;
            return this;
        }

        final FormBody.Builder builder = new FormBody.Builder();

        for (Map.Entry<?, ?> entry : formBodyByMap.entrySet()) {
            builder.add(N.stringOf(entry.getKey()), N.stringOf(entry.getValue()));
        }

        this.body = builder.build();
        return this;
    }

    /**
     *
     * @param formBodyByBean
     * @return
     * @see {@code FormBody.Builder}
     */
    public OkHttpRequest formBody(final Object formBodyByBean) {
        if (formBodyByBean == null) {
            this.body = Util.EMPTY_REQUEST;
            return this;
        }

        final Class<?> cls = formBodyByBean.getClass();
        N.checkArgument(ClassUtil.isBeanClass(cls), "{} is not a bean class with getter/setter methods", cls);

        final BeanInfo beanInfo = ParserUtil.getBeanInfo(cls);
        final FormBody.Builder builder = new FormBody.Builder();

        for (PropInfo propInfo : beanInfo.propInfoList) {
            builder.add(propInfo.name, N.stringOf(propInfo.getPropValue(formBodyByBean)));
        }

        this.body = builder.build();
        return this;
    }

    /**
     *
     * @param formBodyByMap
     * @return
     * @see {@code FormBody.Builder}
     * @deprecated replaced by {@link #formBody(Map)}.
     */
    @Deprecated
    public OkHttpRequest body(final Map<?, ?> formBodyByMap) {
        return formBody(formBodyByMap);
    }

    /**
     *
     * @param formBodyByBean
     * @return
     * @see {@code FormBody.Builder}
     * @deprecated replaced by {@link #formBody(Object)}.
     */
    @Deprecated
    public OkHttpRequest body(final Object formBodyByBean) {
        return formBody(formBodyByBean);
    }

    /**
     *
     * @param body
     * @return
     * @see {@code RequestBody}
     */
    public OkHttpRequest body(RequestBody body) {
        this.body = body;
        return this;
    }

    /**
     *
     * @param content
     * @param contentType
     * @return
     * @see RequestBody#create(MediaType, String)
     */
    public OkHttpRequest body(String content, @Nullable MediaType contentType) {
        this.body = RequestBody.create(contentType, content);

        return this;
    }

    /**
     *
     * @param content
     * @param contentType
     * @return
     * @see RequestBody#create(MediaType, byte[])
     */
    public OkHttpRequest body(final byte[] content, @Nullable MediaType contentType) {
        this.body = RequestBody.create(contentType, content);

        return this;
    }

    /**
     *
     * @param content
     * @param offset
     * @param len
     * @param contentType
     * @return
     * @see RequestBody#create(MediaType, byte[], int, int)
     */
    public OkHttpRequest body(final byte[] content, final int offset, final int byteCount, @Nullable MediaType contentType) {
        this.body = RequestBody.create(contentType, content, offset, byteCount);

        return this;
    }

    /**
     *
     * @param content
     * @param contentType
     * @return
     */
    public OkHttpRequest body(final File content, @Nullable MediaType contentType) {
        this.body = RequestBody.create(contentType, content);

        return this;
    }

    public Response get() throws IOException {
        return execute(HttpMethod.GET);
    }

    public <T> T get(Class<T> resultClass) throws IOException {
        return execute(resultClass, HttpMethod.GET);
    }

    public Response post() throws IOException {
        return execute(HttpMethod.POST);
    }

    public <T> T post(Class<T> resultClass) throws IOException {
        return execute(resultClass, HttpMethod.POST);
    }

    public Response put() throws IOException {
        return execute(HttpMethod.PUT);
    }

    public <T> T put(Class<T> resultClass) throws IOException {
        return execute(resultClass, HttpMethod.PUT);
    }

    public Response patch() throws IOException {
        return execute(HttpMethod.PATCH);
    }

    public <T> T patch(Class<T> resultClass) throws IOException {
        return execute(resultClass, HttpMethod.PATCH);
    }

    public Response delete() throws IOException {
        return execute(HttpMethod.DELETE);
    }

    public <T> T delete(Class<T> resultClass) throws IOException {
        return execute(resultClass, HttpMethod.DELETE);
    }

    public Response head() throws IOException {
        return execute(HttpMethod.HEAD);
    }

    public Response execute(final HttpMethod httpMethod) throws IOException {
        // body = (body == null && HttpMethod.DELETE.equals(httpMethod)) ? Util.EMPTY_REQUEST : body;
        request = builder.method(httpMethod.name(), body).build();

        return httpClient.newCall(request).execute();
    }

    public <T> T execute(final Class<T> resultClass, final HttpMethod httpMethod) throws IOException {
        N.checkArgNotNull(resultClass, "resultClass");
        N.checkArgument(!HttpResponse.class.equals(resultClass), "Return type can't be HttpResponse");

        try (Response resp = execute(httpMethod)) {
            if (Response.class.equals(resultClass)) {
                return (T) resp;
            } else if (resp.isSuccessful()) {
                final String contentType = request.header(HttpHeaders.Names.CONTENT_TYPE);
                final String contentEncoding = request.header(HttpHeaders.Names.CONTENT_ENCODING);
                final ContentFormat requestContentFormat = HttpUtil.getContentFormat(contentType, contentEncoding);
                final Charset requestCharset = HttpUtil.getCharset(contentType);
                final Map<String, List<String>> respHeaders = resp.headers().toMultimap();
                final Charset respCharset = HttpUtil.getResponseCharset(respHeaders, requestCharset);
                final ContentFormat respContentFormat = HttpUtil.getResponseContentFormat(respHeaders, requestContentFormat);

                final InputStream is = HttpUtil.wrapInputStream(resp.body().byteStream(), respContentFormat);

                if (resultClass == null || resultClass.equals(String.class)) {
                    return (T) IOUtil.readAllToString(is, respCharset);
                } else if (byte[].class.equals(resultClass)) {
                    return (T) IOUtil.readAllBytes(is);
                } else {
                    if (respContentFormat == ContentFormat.KRYO && kryoParser != null) {
                        return kryoParser.deserialize(resultClass, is);
                    } else if (respContentFormat == ContentFormat.FormUrlEncoded) {
                        return URLEncodedUtil.decode(IOUtil.readAllToString(is, respCharset), resultClass);
                    } else {
                        final BufferedReader br = Objectory.createBufferedReader(new InputStreamReader(is, respCharset));

                        try {
                            return HttpUtil.getParser(respContentFormat).deserialize(resultClass, br);
                        } finally {
                            Objectory.recycle(br);
                        }
                    }
                }
            } else {
                throw new IOException(resp.code() + ": " + resp.message());
            }
        }
    }

    public ContinuableFuture<Response> asyncGet() {
        return asyncGet(HttpUtil.DEFAULT_EXECUTOR);
    }

    public ContinuableFuture<Response> asyncGet(final Executor executor) {
        return ContinuableFuture.call(this::get, executor);
    }

    public <T> ContinuableFuture<T> asyncGet(final Class<T> resultClass) {
        return asyncGet(resultClass, HttpUtil.DEFAULT_EXECUTOR);
    }

    public <T> ContinuableFuture<T> asyncGet(final Class<T> resultClass, final Executor executor) {
        return ContinuableFuture.call(() -> get(resultClass), executor);
    }

    public ContinuableFuture<Response> asyncPost() {
        return asyncPost(HttpUtil.DEFAULT_EXECUTOR);
    }

    public ContinuableFuture<Response> asyncPost(final Executor executor) {
        return ContinuableFuture.call(this::post, executor);
    }

    public <T> ContinuableFuture<T> asyncPost(final Class<T> resultClass) {
        return asyncPost(resultClass, HttpUtil.DEFAULT_EXECUTOR);
    }

    public <T> ContinuableFuture<T> asyncPost(final Class<T> resultClass, final Executor executor) {
        return ContinuableFuture.call(() -> post(resultClass), executor);
    }

    public ContinuableFuture<Response> asyncPut() {
        return asyncPut(HttpUtil.DEFAULT_EXECUTOR);
    }

    public ContinuableFuture<Response> asyncPut(final Executor executor) {
        return ContinuableFuture.call(this::put, executor);
    }

    public <T> ContinuableFuture<T> asyncPut(final Class<T> resultClass) {
        return asyncPut(resultClass, HttpUtil.DEFAULT_EXECUTOR);
    }

    public <T> ContinuableFuture<T> asyncPut(final Class<T> resultClass, final Executor executor) {
        return ContinuableFuture.call(() -> put(resultClass), executor);
    }

    public ContinuableFuture<Response> asyncPatch() {
        return asyncPatch(HttpUtil.DEFAULT_EXECUTOR);
    }

    public ContinuableFuture<Response> asyncPatch(final Executor executor) {
        return ContinuableFuture.call(this::patch, executor);
    }

    public <T> ContinuableFuture<T> asyncPatch(final Class<T> resultClass) {
        return asyncPatch(resultClass, HttpUtil.DEFAULT_EXECUTOR);
    }

    public <T> ContinuableFuture<T> asyncPatch(final Class<T> resultClass, final Executor executor) {
        return ContinuableFuture.call(() -> patch(resultClass), executor);
    }

    public ContinuableFuture<Response> asyncDelete() {
        return asyncDelete(HttpUtil.DEFAULT_EXECUTOR);
    }

    public ContinuableFuture<Response> asyncDelete(final Executor executor) {
        return ContinuableFuture.call(this::delete, executor);
    }

    public <T> ContinuableFuture<T> asyncDelete(final Class<T> resultClass) {
        return asyncDelete(resultClass, HttpUtil.DEFAULT_EXECUTOR);
    }

    public <T> ContinuableFuture<T> asyncDelete(final Class<T> resultClass, final Executor executor) {
        return ContinuableFuture.call(() -> delete(resultClass), executor);
    }

    public ContinuableFuture<Response> asyncHead() {
        return asyncHead(HttpUtil.DEFAULT_EXECUTOR);
    }

    public ContinuableFuture<Response> asyncHead(final Executor executor) {
        return ContinuableFuture.call(this::head, executor);
    }

    public ContinuableFuture<Response> asyncExecute(final HttpMethod httpMethod) {
        return asyncExecute(httpMethod, HttpUtil.DEFAULT_EXECUTOR);
    }

    public ContinuableFuture<Response> asyncExecute(final HttpMethod httpMethod, final Executor executor) {
        return ContinuableFuture.call(() -> execute(httpMethod), executor);
    }

    public <T> ContinuableFuture<T> asyncExecute(final Class<T> resultClass, final HttpMethod httpMethod) {
        return asyncExecute(resultClass, httpMethod, HttpUtil.DEFAULT_EXECUTOR);
    }

    public <T> ContinuableFuture<T> asyncExecute(final Class<T> resultClass, final HttpMethod httpMethod, final Executor executor) {
        return ContinuableFuture.call(() -> execute(resultClass, httpMethod), executor);
    }
}
