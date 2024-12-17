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
import com.landawn.abacus.util.Charsets;
import com.landawn.abacus.util.ClassUtil;
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
 * Note: This class contains the codes and docs copied from : <a href="https://square.github.io/okhttp/">OkHttp</a> under Apache License v2.
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
     *
     * @param url
     * @param httpClient
     * @return
     */
    public static OkHttpRequest create(final String url, final OkHttpClient httpClient) {
        return new OkHttpRequest(url, null, httpClient);
    }

    /**
     *
     * @param url
     * @param httpClient
     * @return
     */
    public static OkHttpRequest create(final URL url, final OkHttpClient httpClient) {
        return new OkHttpRequest(null, HttpUrl.get(url), httpClient);
    }

    /**
     *
     * @param url
     * @param httpClient
     * @return
     */
    public static OkHttpRequest create(final HttpUrl url, final OkHttpClient httpClient) {
        return new OkHttpRequest(null, url, httpClient);
    }

    /**
     * Sets the URL target of this request.
     *
     * @param url
     * @return
     * @throws IllegalArgumentException if {@code url} is not a valid HTTP or HTTPS URL. Avoid this
     * exception by calling {@link HttpUrl#parse}; it returns {@code null} for invalid URLs.
     */
    public static OkHttpRequest url(final String url) {
        return create(url, defaultClient);
    }

    /**
     * Sets the URL target of this request.
     *
     * @param url
     * @return
     * @throws IllegalArgumentException if the scheme of {@code url} is not {@code http} or {@code https}.
     */
    public static OkHttpRequest url(final URL url) {
        return create(url, defaultClient);
    }

    /**
     *
     * @param url
     * @return
     */
    public static OkHttpRequest url(final HttpUrl url) {
        return create(url, defaultClient);
    }

    /**
     *
     * @param url
     * @param connectionTimeoutInMillis
     * @param readTimeoutInMillis
     * @return
     */
    public static OkHttpRequest url(final String url, final long connectionTimeoutInMillis, final long readTimeoutInMillis) {
        return create(url,
                new OkHttpClient.Builder().connectTimeout(connectionTimeoutInMillis, TimeUnit.MILLISECONDS)
                        .readTimeout(readTimeoutInMillis, TimeUnit.MILLISECONDS)
                        .build()).closeHttpClientAfterExecution(true);
    }

    /**
     *
     * @param url
     * @param connectionTimeoutInMillis
     * @param readTimeoutInMillis
     * @return
     */
    public static OkHttpRequest url(final URL url, final long connectionTimeoutInMillis, final long readTimeoutInMillis) {
        return create(url,
                new OkHttpClient.Builder().connectTimeout(connectionTimeoutInMillis, TimeUnit.MILLISECONDS)
                        .readTimeout(readTimeoutInMillis, TimeUnit.MILLISECONDS)
                        .build()).closeHttpClientAfterExecution(true);
    }

    /**
     *
     * @param url
     * @param connectionTimeoutInMillis
     * @param readTimeoutInMillis
     * @return
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
     * @param cacheControl
     * @return
     */
    public OkHttpRequest cacheControl(final CacheControl cacheControl) {
        requestBuilder.cacheControl(cacheControl);
        return this;
    }

    /**
     * Attaches {@code tag} to the request. It can be used later to cancel the request. If the tag
     * is unspecified or {@code null}, the request is canceled by using the request itself as the tag.
     *
     * @param tag
     * @return
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
     * you may read it in interceptors, event listeners, or callbacks.
     *
     * @param <T>
     * @param type
     * @param tag
     * @return
     */
    public <T> OkHttpRequest tag(final Class<? super T> type, @Nullable final T tag) {
        requestBuilder.tag(type, tag);
        return this;
    }

    /**
     *
     * @param user
     * @param password
     * @return
     */
    public OkHttpRequest basicAuth(final String user, final Object password) {
        requestBuilder.header(HttpHeaders.Names.AUTHORIZATION, "Basic " + Strings.base64Encode((user + ":" + password).getBytes(Charsets.UTF_8)));
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
     * @see HttpHeaders
     */
    public OkHttpRequest header(final String name, final String value) {
        requestBuilder.header(name, value);
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
     * @see HttpHeaders
     */
    public OkHttpRequest headers(final String name1, final String value1, final String name2, final String value2) {
        requestBuilder.header(name1, value1);
        requestBuilder.header(name2, value2);

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
     * @see HttpHeaders
     */
    public OkHttpRequest headers(final String name1, final String value1, final String name2, final String value2, final String name3, final String value3) {
        requestBuilder.header(name1, value1);
        requestBuilder.header(name2, value2);
        requestBuilder.header(name3, value3);

        return this;
    }

    /**
     * Set http headers specified by the key/value entities from {@code Map}.
     * If this request already has any headers with that name, they are all replaced.
     *
     * @param headers
     * @return
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
     * @param headers
     * @return
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
     * @param headers
     * @return
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
     * OkHttp may replace {@code value} with a header derived from the request body.
     *
     * @param name
     * @param value
     * @return
     * @deprecated no use case?
     */
    @Deprecated
    public OkHttpRequest addHeader(final String name, final String value) {
        requestBuilder.addHeader(name, value);
        return this;
    }

    /**
     *
     * @param name
     * @return
     * @deprecated no use case?
     */
    @Deprecated
    public OkHttpRequest removeHeader(final String name) {
        requestBuilder.removeHeader(name);
        return this;
    }

    /**
     * Set query parameters for {@code GET} or {@code DELETE} request.
     *
     * @param query
     * @return
     */
    public OkHttpRequest query(final String query) {
        this.query = query;

        return this;
    }

    /**
     * Set query parameters for {@code GET} or {@code DELETE} request.
     *
     * @param queryParams
     * @return
     */
    public OkHttpRequest query(final Map<String, ?> queryParams) {
        query = queryParams;

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
        return body(N.toJson(obj), APPLICATION_JSON_MEDIA_TYPE);
    }

    /**
     *
     * @param xml
     * @return
     */
    public OkHttpRequest xmlBody(final String xml) {
        return body(xml, APPLICATION_XML_MEDIA_TYPE);
    }

    /**
     *
     * @param obj
     * @return
     */
    public OkHttpRequest xmlBody(final Object obj) {
        return body(N.toXml(obj), APPLICATION_XML_MEDIA_TYPE);
    }

    /**
     *
     * @param formBodyByMap
     * @return
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
     *
     * @param formBodyByBean
     * @return
     * @throws IllegalArgumentException
     * @see {@code FormBody.Builder}
     */
    public OkHttpRequest formBody(final Object formBodyByBean) throws IllegalArgumentException {
        if (formBodyByBean == null) {
            body = Util.EMPTY_REQUEST;
            return this;
        }

        final Class<?> cls = formBodyByBean.getClass();
        N.checkArgument(ClassUtil.isBeanClass(cls), "{} is not a bean class with getter/setter methods", cls);

        final BeanInfo beanInfo = ParserUtil.getBeanInfo(cls);
        final FormBody.Builder formBodyBuilder = new FormBody.Builder();

        for (final PropInfo propInfo : beanInfo.propInfoList) {
            formBodyBuilder.add(propInfo.name, N.stringOf(propInfo.getPropValue(formBodyByBean)));
        }

        body = formBodyBuilder.build();
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
    public OkHttpRequest body(final RequestBody body) {
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
    public OkHttpRequest body(final String content, @Nullable final MediaType contentType) {
        body = RequestBody.create(contentType, content);

        return this;
    }

    /**
     *
     * @param content
     * @param contentType
     * @return
     * @see RequestBody#create(MediaType, byte[])
     */
    public OkHttpRequest body(final byte[] content, @Nullable final MediaType contentType) {
        body = RequestBody.create(contentType, content);

        return this;
    }

    /**
     *
     * @param content
     * @param offset
     * @param byteCount
     * @param contentType
     * @return
     * @see RequestBody#create(MediaType, byte[], int, int)
     */
    public OkHttpRequest body(final byte[] content, final int offset, final int byteCount, @Nullable final MediaType contentType) {
        body = RequestBody.create(contentType, content, offset, byteCount);

        return this;
    }

    /**
     *
     * @param content
     * @param contentType
     * @return
     */
    public OkHttpRequest body(final File content, @Nullable final MediaType contentType) {
        body = RequestBody.create(contentType, content);

        return this;
    }

    /**
     *
     * @return
     * @throws IOException
     */
    public Response get() throws IOException {
        return execute(HttpMethod.GET);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @return
     * @throws IOException
     */
    public <T> T get(final Class<T> resultClass) throws IOException {
        return execute(HttpMethod.GET, resultClass);
    }

    /**
     *
     * @return
     * @throws IOException
     */
    public Response post() throws IOException {
        return execute(HttpMethod.POST);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @return
     * @throws IOException
     */
    public <T> T post(final Class<T> resultClass) throws IOException {
        return execute(HttpMethod.POST, resultClass);
    }

    /**
     *
     * @return
     * @throws IOException
     */
    public Response put() throws IOException {
        return execute(HttpMethod.PUT);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @return
     * @throws IOException
     */
    public <T> T put(final Class<T> resultClass) throws IOException {
        return execute(HttpMethod.PUT, resultClass);
    }

    /**
     *
     * @return
     * @throws IOException
     */
    public Response patch() throws IOException {
        return execute(HttpMethod.PATCH);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @return
     * @throws IOException
     */
    public <T> T patch(final Class<T> resultClass) throws IOException {
        return execute(HttpMethod.PATCH, resultClass);
    }

    /**
     *
     * @return
     * @throws IOException
     */
    public Response delete() throws IOException {
        return execute(HttpMethod.DELETE);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @return
     * @throws IOException
     */
    public <T> T delete(final Class<T> resultClass) throws IOException {
        return execute(HttpMethod.DELETE, resultClass);
    }

    /**
     *
     * @return
     * @throws IOException
     */
    public Response head() throws IOException {
        return execute(HttpMethod.HEAD);
    }

    /**
     *
     * @param httpMethod
     * @return
     * @throws IOException
     */
    @Beta
    public Response execute(final HttpMethod httpMethod) throws IOException {
        // body = (body == null && HttpMethod.DELETE.equals(httpMethod)) ? Util.EMPTY_REQUEST : body;
        final Request request = createRequest(httpMethod);

        return execute(request);
    }

    /**
     *
     * @param <T>
     * @param httpMethod
     * @param resultClass
     * @return
     * @throws IllegalArgumentException
     * @throws IOException
     */
    @Beta
    public <T> T execute(final HttpMethod httpMethod, final Class<T> resultClass) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(resultClass, cs.resultClass);
        N.checkArgument(!HttpResponse.class.equals(resultClass), "Return type can't be HttpResponse");
        final Request request = createRequest(httpMethod);

        try (Response resp = execute(request)) {
            if (Response.class.equals(resultClass)) {
                return (T) resp;
            } else if (resultClass == null || resultClass.equals(Void.class)) {
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
            doAfterExecution();
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

    public ContinuableFuture<Response> asyncGet() {
        return asyncGet(HttpUtil.DEFAULT_EXECUTOR);
    }

    /**
     *
     * @param executor
     * @return
     */
    public ContinuableFuture<Response> asyncGet(final Executor executor) {
        return ContinuableFuture.call(this::get, executor);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @return
     */
    public <T> ContinuableFuture<T> asyncGet(final Class<T> resultClass) {
        return asyncGet(resultClass, HttpUtil.DEFAULT_EXECUTOR);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @param executor
     * @return
     */
    public <T> ContinuableFuture<T> asyncGet(final Class<T> resultClass, final Executor executor) {
        return ContinuableFuture.call(() -> get(resultClass), executor);
    }

    public ContinuableFuture<Response> asyncPost() {
        return asyncPost(HttpUtil.DEFAULT_EXECUTOR);
    }

    /**
     *
     * @param executor
     * @return
     */
    public ContinuableFuture<Response> asyncPost(final Executor executor) {
        return ContinuableFuture.call(this::post, executor);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @return
     */
    public <T> ContinuableFuture<T> asyncPost(final Class<T> resultClass) {
        return asyncPost(resultClass, HttpUtil.DEFAULT_EXECUTOR);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @param executor
     * @return
     */
    public <T> ContinuableFuture<T> asyncPost(final Class<T> resultClass, final Executor executor) {
        return ContinuableFuture.call(() -> post(resultClass), executor);
    }

    public ContinuableFuture<Response> asyncPut() {
        return asyncPut(HttpUtil.DEFAULT_EXECUTOR);
    }

    /**
     *
     * @param executor
     * @return
     */
    public ContinuableFuture<Response> asyncPut(final Executor executor) {
        return ContinuableFuture.call(this::put, executor);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @return
     */
    public <T> ContinuableFuture<T> asyncPut(final Class<T> resultClass) {
        return asyncPut(resultClass, HttpUtil.DEFAULT_EXECUTOR);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @param executor
     * @return
     */
    public <T> ContinuableFuture<T> asyncPut(final Class<T> resultClass, final Executor executor) {
        return ContinuableFuture.call(() -> put(resultClass), executor);
    }

    public ContinuableFuture<Response> asyncPatch() {
        return asyncPatch(HttpUtil.DEFAULT_EXECUTOR);
    }

    /**
     *
     * @param executor
     * @return
     */
    public ContinuableFuture<Response> asyncPatch(final Executor executor) {
        return ContinuableFuture.call(this::patch, executor);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @return
     */
    public <T> ContinuableFuture<T> asyncPatch(final Class<T> resultClass) {
        return asyncPatch(resultClass, HttpUtil.DEFAULT_EXECUTOR);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @param executor
     * @return
     */
    public <T> ContinuableFuture<T> asyncPatch(final Class<T> resultClass, final Executor executor) {
        return ContinuableFuture.call(() -> patch(resultClass), executor);
    }

    public ContinuableFuture<Response> asyncDelete() {
        return asyncDelete(HttpUtil.DEFAULT_EXECUTOR);
    }

    /**
     *
     * @param executor
     * @return
     */
    public ContinuableFuture<Response> asyncDelete(final Executor executor) {
        return ContinuableFuture.call(this::delete, executor);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @return
     */
    public <T> ContinuableFuture<T> asyncDelete(final Class<T> resultClass) {
        return asyncDelete(resultClass, HttpUtil.DEFAULT_EXECUTOR);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @param executor
     * @return
     */
    public <T> ContinuableFuture<T> asyncDelete(final Class<T> resultClass, final Executor executor) {
        return ContinuableFuture.call(() -> delete(resultClass), executor);
    }

    public ContinuableFuture<Response> asyncHead() {
        return asyncHead(HttpUtil.DEFAULT_EXECUTOR);
    }

    /**
     *
     * @param executor
     * @return
     */
    public ContinuableFuture<Response> asyncHead(final Executor executor) {
        return ContinuableFuture.call(this::head, executor);
    }

    /**
     *
     * @param httpMethod
     * @return
     */
    @Beta
    public ContinuableFuture<Response> asyncExecute(final HttpMethod httpMethod) {
        return asyncExecute(httpMethod, HttpUtil.DEFAULT_EXECUTOR);
    }

    /**
     *
     * @param httpMethod
     * @param executor
     * @return
     */
    @Beta
    public ContinuableFuture<Response> asyncExecute(final HttpMethod httpMethod, final Executor executor) {
        return ContinuableFuture.call(() -> execute(httpMethod), executor);
    }

    /**
     *
     * @param <T>
     * @param httpMethod
     * @param resultClass
     * @return
     */
    @Beta
    public <T> ContinuableFuture<T> asyncExecute(final HttpMethod httpMethod, final Class<T> resultClass) {
        return asyncExecute(httpMethod, resultClass, HttpUtil.DEFAULT_EXECUTOR);
    }

    /**
     *
     * @param <T>
     * @param httpMethod
     * @param resultClass
     * @param executor
     * @return
     */
    @Beta
    public <T> ContinuableFuture<T> asyncExecute(final HttpMethod httpMethod, final Class<T> resultClass, final Executor executor) {
        return ContinuableFuture.call(() -> execute(httpMethod, resultClass), executor);
    }
}
