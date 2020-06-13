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

package com.landawn.abacus.http.okhttp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import com.landawn.abacus.http.ContentFormat;
import com.landawn.abacus.http.HttpHeaders;
import com.landawn.abacus.http.HttpMethod;
import com.landawn.abacus.http.HttpResponse;
import com.landawn.abacus.http.HttpUtil;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.parser.KryoParser;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.EntityInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.parser.XMLParser;
import com.landawn.abacus.util.AndroidUtil;
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
public class OkHttpRequest {
    private static final Logger logger = LoggerFactory.getLogger(OkHttpRequest.class);

    private static final MediaType APPLICATION_JSON_MEDIA_TYPE = MediaType.get("application/json");

    private static final Executor DEFAULT_EXECUTOR;

    static {
        if (IOUtil.IS_PLATFORM_ANDROID) {
            DEFAULT_EXECUTOR = AndroidUtil.getThreadPoolExecutor();
        } else {
            DEFAULT_EXECUTOR = new ThreadPoolExecutor(Math.max(8, IOUtil.CPU_CORES), Math.max(64, IOUtil.CPU_CORES), 180L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<Runnable>());
        }
    }

    static final XMLParser xmlParser = ParserFactory.isXMLAvailable() ? ParserFactory.createXMLParser() : null;
    static final KryoParser kryoParser = ParserFactory.isKryoAvailable() ? ParserFactory.createKryoParser() : null;

    static final OkHttpClient defaultClient = new OkHttpClient();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (DEFAULT_EXECUTOR instanceof ExecutorService) {
                    final ExecutorService executorService = (ExecutorService) DEFAULT_EXECUTOR;

                    logger.warn("Starting to shutdown task in OkHttpRequest");

                    try {
                        executorService.shutdown();

                        executorService.awaitTermination(60, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        logger.warn("Not all the requests/tasks executed in OkHttpRequest are completed successfully before shutdown.");
                    } finally {
                        logger.warn("Completed to shutdown task in OkHttpRequest");
                    }
                }
            }
        });
    }

    final OkHttpClient httpClient;
    final Request.Builder builder = new Request.Builder();
    RequestBody body;
    Request request;

    OkHttpRequest(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public static OkHttpRequest create() {
        return new OkHttpRequest(defaultClient);
    }

    public static OkHttpRequest create(OkHttpClient httpClient) {
        return new OkHttpRequest(httpClient);
    }

    /**
     * Sets the URL target of this request.
     *
     * @throws IllegalArgumentException if {@code url} is not a valid HTTP or HTTPS URL. Avoid this
     * exception by calling {@link HttpUrl#parse}; it returns null for invalid URLs.
     */
    public OkHttpRequest url(String url) {
        builder.url(url);
        return this;
    }

    /**
     * Sets the URL target of this request.
     *
     * @throws IllegalArgumentException if the scheme of {@code url} is not {@code http} or {@code
     * https}.
     */
    public OkHttpRequest url(URL url) {
        builder.url(url);
        return this;
    }

    public OkHttpRequest url(HttpUrl url) {
        builder.url(url);

        return this;
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
        builder.tag(tag);
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
     * Sets the header named {@code name} to {@code value}. If this request already has any headers
     * with that name, they are all replaced.
     */
    public OkHttpRequest header(String name, String value) {
        builder.header(name, value);
        return this;
    }

    public OkHttpRequest headers(String name1, String value1, String name2, String value2) {
        builder.header(name1, value1);
        builder.header(name2, value2);

        return this;
    }

    public OkHttpRequest headers(String name1, String value1, String name2, String value2, String name3, String value3) {
        builder.header(name1, value1);
        builder.header(name2, value2);
        builder.header(name3, value3);

        return this;
    }

    public OkHttpRequest headers(final Map<String, ?> headers) {
        if (N.notNullOrEmpty(headers)) {
            for (Map.Entry<String, ?> entry : headers.entrySet()) {
                builder.header(entry.getKey(), HttpHeaders.valueOf(entry.getValue()));
            }
        }

        return this;
    }

    /**
     * Removes all headers on this builder and adds {@code headers}.
     */
    public OkHttpRequest headers(Headers headers) {
        builder.headers(headers);
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
     * @param formBodyByEntity
     * @return
     * @see {@code FormBody.Builder}
     */
    public OkHttpRequest formBody(final Object formBodyByEntity) {
        if (formBodyByEntity == null) {
            this.body = Util.EMPTY_REQUEST;
            return this;
        }

        final Class<?> cls = formBodyByEntity.getClass();
        N.checkArgument(ClassUtil.isEntity(cls), "{} is not an entity class with getter/setter methods", cls);

        final EntityInfo entityInfo = ParserUtil.getEntityInfo(cls);
        final FormBody.Builder builder = new FormBody.Builder();

        for (PropInfo propInfo : entityInfo.propInfoList) {
            builder.add(propInfo.name, N.stringOf(propInfo.getPropValue(formBodyByEntity)));
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
     * @param formBodyByEntity
     * @return
     * @see {@code FormBody.Builder}
     * @deprecated replaced by {@link #formBody(Object)}.
     */
    @Deprecated
    public OkHttpRequest body(final Object formBodyByEntity) {
        return formBody(formBodyByEntity);
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
            if (resp.isSuccessful()) {
                final String contentType = request.header(HttpHeaders.Names.CONTENT_TYPE);
                final String contentEncoding = request.header(HttpHeaders.Names.CONTENT_ENCODING);
                final ContentFormat requestContentFormat = HttpUtil.getContentFormat(contentType, contentEncoding);
                final Charset requestCharset = HttpUtil.getCharset(contentType);
                final Map<String, List<String>> respHeaders = resp.headers().toMultimap();
                final Charset respCharset = HttpUtil.getResponseCharset(respHeaders, requestCharset);
                final ContentFormat respContentFormat = HttpUtil.getResponseContentFormat(respHeaders, requestContentFormat);

                final InputStream is = HttpUtil.wrapInputStream(resp.body().byteStream(), respContentFormat);

                if (resultClass == null || resultClass.equals(String.class)) {
                    return (T) IOUtil.readString(is, respCharset);
                } else if (byte[].class.equals(resultClass)) {
                    return (T) IOUtil.readAllBytes(is);
                } else {
                    if (respContentFormat == ContentFormat.KRYO && kryoParser != null) {
                        return kryoParser.deserialize(resultClass, is);
                    } else if (respContentFormat == ContentFormat.FormUrlEncoded) {
                        return URLEncodedUtil.decode(resultClass, IOUtil.readString(is, respCharset));
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
        return asyncGet(DEFAULT_EXECUTOR);
    }

    public ContinuableFuture<Response> asyncGet(final Executor executor) {
        return ContinuableFuture.call(new Callable<Response>() {
            @Override
            public Response call() throws IOException {
                return get();
            }

        }, executor);
    }

    public <T> ContinuableFuture<T> asyncGet(final Class<T> resultClass) {
        return asyncGet(resultClass, DEFAULT_EXECUTOR);
    }

    public <T> ContinuableFuture<T> asyncGet(final Class<T> resultClass, final Executor executor) {
        return ContinuableFuture.call(new Callable<T>() {
            @Override
            public T call() throws IOException {
                return get(resultClass);
            }

        }, executor);
    }

    public ContinuableFuture<Response> asyncPost() {
        return asyncPost(DEFAULT_EXECUTOR);
    }

    public ContinuableFuture<Response> asyncPost(final Executor executor) {
        return ContinuableFuture.call(new Callable<Response>() {
            @Override
            public Response call() throws IOException {
                return post();
            }

        }, executor);
    }

    public <T> ContinuableFuture<T> asyncPost(final Class<T> resultClass) {
        return asyncPost(resultClass, DEFAULT_EXECUTOR);
    }

    public <T> ContinuableFuture<T> asyncPost(final Class<T> resultClass, final Executor executor) {
        return ContinuableFuture.call(new Callable<T>() {
            @Override
            public T call() throws IOException {
                return post(resultClass);
            }

        }, executor);
    }

    public ContinuableFuture<Response> asyncPut() {
        return asyncPut(DEFAULT_EXECUTOR);
    }

    public ContinuableFuture<Response> asyncPut(final Executor executor) {
        return ContinuableFuture.call(new Callable<Response>() {
            @Override
            public Response call() throws IOException {
                return put();
            }

        }, executor);
    }

    public <T> ContinuableFuture<T> asyncPut(final Class<T> resultClass) {
        return asyncPut(resultClass, DEFAULT_EXECUTOR);
    }

    public <T> ContinuableFuture<T> asyncPut(final Class<T> resultClass, final Executor executor) {
        return ContinuableFuture.call(new Callable<T>() {
            @Override
            public T call() throws IOException {
                return put(resultClass);
            }

        }, executor);
    }

    public ContinuableFuture<Response> asyncPatch() {
        return asyncPatch(DEFAULT_EXECUTOR);
    }

    public ContinuableFuture<Response> asyncPatch(final Executor executor) {
        return ContinuableFuture.call(new Callable<Response>() {
            @Override
            public Response call() throws IOException {
                return patch();
            }

        }, executor);
    }

    public <T> ContinuableFuture<T> asyncPatch(final Class<T> resultClass) {
        return asyncPatch(resultClass, DEFAULT_EXECUTOR);
    }

    public <T> ContinuableFuture<T> asyncPatch(final Class<T> resultClass, final Executor executor) {
        return ContinuableFuture.call(new Callable<T>() {
            @Override
            public T call() throws IOException {
                return patch(resultClass);
            }

        }, executor);
    }

    public ContinuableFuture<Response> asyncDelete() {
        return asyncDelete(DEFAULT_EXECUTOR);
    }

    public ContinuableFuture<Response> asyncDelete(final Executor executor) {
        return ContinuableFuture.call(new Callable<Response>() {
            @Override
            public Response call() throws IOException {
                return delete();
            }

        }, executor);
    }

    public <T> ContinuableFuture<T> asyncDelete(final Class<T> resultClass) {
        return asyncDelete(resultClass, DEFAULT_EXECUTOR);
    }

    public <T> ContinuableFuture<T> asyncDelete(final Class<T> resultClass, final Executor executor) {
        return ContinuableFuture.call(new Callable<T>() {
            @Override
            public T call() throws IOException {
                return delete(resultClass);
            }

        }, executor);
    }

    public ContinuableFuture<Response> asyncHead() {
        return asyncHead(DEFAULT_EXECUTOR);
    }

    public ContinuableFuture<Response> asyncHead(final Executor executor) {
        return ContinuableFuture.call(new Callable<Response>() {
            @Override
            public Response call() throws IOException {
                return head();
            }

        }, executor);
    }

    public ContinuableFuture<Response> asyncExecute(final HttpMethod httpMethod) {
        return asyncExecute(httpMethod, DEFAULT_EXECUTOR);
    }

    public ContinuableFuture<Response> asyncExecute(final HttpMethod httpMethod, final Executor executor) {
        return ContinuableFuture.call(new Callable<Response>() {
            @Override
            public Response call() throws IOException {
                return execute(httpMethod);
            }

        }, executor);
    }

    public <T> ContinuableFuture<T> asyncExecute(final Class<T> resultClass, final HttpMethod httpMethod) {
        return asyncExecute(resultClass, httpMethod, DEFAULT_EXECUTOR);
    }

    public <T> ContinuableFuture<T> asyncExecute(final Class<T> resultClass, final HttpMethod httpMethod, final Executor executor) {
        return ContinuableFuture.call(new Callable<T>() {
            @Override
            public T call() throws IOException {
                return execute(resultClass, httpMethod);
            }

        }, executor);
    }
}
