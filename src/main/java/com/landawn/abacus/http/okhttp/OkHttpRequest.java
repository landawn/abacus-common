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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.landawn.abacus.http.ContentFormat;
import com.landawn.abacus.http.HTTP;
import com.landawn.abacus.http.HttpHeaders;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.parser.KryoParser;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.parser.XMLParser;
import com.landawn.abacus.util.AndroidUtil;
import com.landawn.abacus.util.BufferedReader;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.ContinuableFuture;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Try;

import okhttp3.CacheControl;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.Util;

/**
 *
 * @since 1.3
 *
 * @author Haiyang Li
 */
public class OkHttpRequest {
    private static final Logger logger = LoggerFactory.getLogger(OkHttpRequest.class);

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
    public OkHttpRequest tag(Object tag) {
        builder.tag(tag);
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
     */
    public OkHttpRequest addHeader(String name, String value) {
        builder.addHeader(name, value);
        return this;
    }

    public OkHttpRequest removeHeader(String name) {
        builder.removeHeader(name);
        return this;
    }

    public OkHttpRequest body(final Map<?, ?> formBodyByMap) {
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

    public OkHttpRequest body(final Object formBodyByEntity) {
        if (formBodyByEntity == null) {
            this.body = Util.EMPTY_REQUEST;
            return this;
        }

        final Class<?> cls = formBodyByEntity.getClass();
        N.checkArgument(ClassUtil.isEntity(cls), "{} is not an entity class with getter/setter methods", cls);

        final FormBody.Builder builder = new FormBody.Builder();

        for (Map.Entry<String, Method> entry : ClassUtil.getPropGetMethodList(cls).entrySet()) {
            builder.add(entry.getKey(), N.stringOf(ClassUtil.getPropValue(formBodyByEntity, entry.getValue())));
        }

        this.body = builder.build();
        return this;
    }

    public OkHttpRequest body(RequestBody body) {
        this.body = body;
        return this;
    }

    public Response get() throws IOException {
        return execute("GET");
    }

    public <T> T get(Class<T> resultClass) throws IOException {
        return execute(resultClass, "GET");
    }

    public Response post() throws IOException {
        return execute("POST");
    }

    public <T> T post(Class<T> resultClass) throws IOException {
        return execute(resultClass, "POST");
    }

    public Response put() throws IOException {
        return execute("PUT");
    }

    public <T> T put(Class<T> resultClass) throws IOException {
        return execute(resultClass, "PUT");
    }

    public Response delete() throws IOException {
        return execute("DELETE");
    }

    public <T> T delete(Class<T> resultClass) throws IOException {
        return execute(resultClass, "DELETE");
    }

    public Response head() throws IOException {
        return execute("HEAD");
    }

    public Response patch() throws IOException {
        return execute("PATCH");
    }

    protected Response execute(String method) throws IOException {
        body = (body == null && "DELETE".equals(method)) ? Util.EMPTY_REQUEST : body;

        return httpClient.newCall(builder.method(method, body).build()).execute();
    }

    protected <T> T execute(Class<T> resultClass, String method) throws IOException {
        N.checkArgNotNull(resultClass, "resultClass");

        try (Response resp = execute(method)) {
            if (resp.isSuccessful()) {
                final ContentFormat respContentFormat = HTTP.getContentFormat(resp.header(HttpHeaders.Names.CONTENT_TYPE),
                        resp.header(HttpHeaders.Names.CONTENT_ENCODING));
                final Charset respCharset = HTTP.getCharset(resp.header(HttpHeaders.Names.CONTENT_TYPE));

                final InputStream is = HTTP.wrapInputStream(resp.body().byteStream(), respContentFormat);

                if (resultClass == null || resultClass.equals(String.class)) {
                    return (T) IOUtil.readString(is, respCharset);
                } else if (byte[].class.equals(resultClass)) {
                    return (T) IOUtil.readBytes(is);
                } else {
                    if (respContentFormat == ContentFormat.KRYO && kryoParser != null) {
                        return kryoParser.deserialize(resultClass, is);
                    } else {
                        final BufferedReader br = Objectory.createBufferedReader(new InputStreamReader(is, respCharset));

                        try {
                            return HTTP.getParser(respContentFormat).deserialize(resultClass, br);
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
        return ContinuableFuture.call(new Try.Callable<Response, IOException>() {
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
        return ContinuableFuture.call(new Try.Callable<T, IOException>() {
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
        return ContinuableFuture.call(new Try.Callable<Response, IOException>() {
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
        return ContinuableFuture.call(new Try.Callable<T, IOException>() {
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
        return ContinuableFuture.call(new Try.Callable<Response, IOException>() {
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
        return ContinuableFuture.call(new Try.Callable<T, IOException>() {
            @Override
            public T call() throws IOException {
                return put(resultClass);
            }

        }, executor);
    }

    public ContinuableFuture<Response> asyncDelete() {
        return asyncDelete(DEFAULT_EXECUTOR);
    }

    public ContinuableFuture<Response> asyncDelete(final Executor executor) {
        return ContinuableFuture.call(new Try.Callable<Response, IOException>() {
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
        return ContinuableFuture.call(new Try.Callable<T, IOException>() {
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
        return ContinuableFuture.call(new Try.Callable<Response, IOException>() {
            @Override
            public Response call() throws IOException {
                return head();
            }

        }, executor);
    }

    public ContinuableFuture<Response> asyncPatch() {
        return asyncPatch(DEFAULT_EXECUTOR);
    }

    public ContinuableFuture<Response> asyncPatch(final Executor executor) {
        return ContinuableFuture.call(new Try.Callable<Response, IOException>() {
            @Override
            public Response call() throws IOException {
                return patch();
            }

        }, executor);
    }
}
