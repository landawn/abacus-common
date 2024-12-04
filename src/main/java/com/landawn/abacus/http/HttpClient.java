/*
 * Copyright (C) 2015 HaiYang Li
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
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.http.HttpHeaders.Names;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.AsyncExecutor;
import com.landawn.abacus.util.ContinuableFuture;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.URLEncodedUtil;

/**
 * Any header can be set into the parameter {@code settings}
 *
 * <br>HttpClient is thread safe.</br>
 *
 */
public final class HttpClient {

    static final Logger logger = LoggerFactory.getLogger(HttpClient.class);

    static {
        if (IOUtil.IS_PLATFORM_ANDROID) {
            // ignore
        } else {
            final int maxConnections = IOUtil.CPU_CORES * 16;

            System.setProperty("http.keepAlive", "true");
            System.setProperty("http.maxConnections", String.valueOf(maxConnections));
        }
    }

    // ...
    public static final int DEFAULT_MAX_CONNECTION = 16;

    /** Unit is milliseconds. */
    public static final int DEFAULT_CONNECTION_TIMEOUT = 8000;

    public static final int DEFAULT_READ_TIMEOUT = 16000;

    // ...
    protected final String _url; //NOSONAR

    protected final int _maxConnection; //NOSONAR

    protected final long _connectionTimeoutInMillis; //NOSONAR

    protected final long _readTimeoutInMillis; //NOSONAR

    protected final HttpSettings _settings; //NOSONAR

    protected final AsyncExecutor _asyncExecutor; //NOSONAR

    protected final URL _netURL; //NOSONAR

    protected final AtomicInteger _activeConnectionCounter; //NOSONAR

    protected HttpClient(final String url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis,
            final HttpSettings settings, final AtomicInteger sharedActiveConnectionCounter, final Executor executor) {
        this(null, url, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, settings, sharedActiveConnectionCounter, executor);
    }

    protected HttpClient(final URL netUrl, final String url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis,
            final HttpSettings settings, final AtomicInteger sharedActiveConnectionCounter, final Executor executor) {
        N.checkArgument(netUrl != null || Strings.isNotEmpty(url), "url can not be null or empty");

        if ((maxConnection < 0) || (connectionTimeoutInMillis < 0) || (readTimeoutInMillis < 0)) {
            throw new IllegalArgumentException("maxConnection, connectionTimeoutInMillis or readTimeoutInMillis can't be less than 0: " + maxConnection + ", "
                    + connectionTimeoutInMillis + ", " + readTimeoutInMillis);
        }

        _netURL = netUrl == null ? createNetUrl(url) : netUrl;
        _url = Strings.isEmpty(url) ? netUrl.toString() : url;
        _maxConnection = (maxConnection == 0) ? DEFAULT_MAX_CONNECTION : maxConnection;
        _connectionTimeoutInMillis = (connectionTimeoutInMillis == 0) ? DEFAULT_CONNECTION_TIMEOUT : connectionTimeoutInMillis;
        _readTimeoutInMillis = (readTimeoutInMillis == 0) ? DEFAULT_READ_TIMEOUT : readTimeoutInMillis;
        _settings = settings == null ? HttpSettings.create() : settings;

        _asyncExecutor = executor == null ? HttpUtil.DEFAULT_ASYNC_EXECUTOR : new AsyncExecutor(executor);

        _activeConnectionCounter = sharedActiveConnectionCounter;
    }

    private static URL createNetUrl(final String url) {
        try {
            return URI.create(N.checkArgNotNull(url, "url")).toURL();
        } catch (final MalformedURLException e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     *
     * @param url
     * @return
     */
    public static HttpClient create(final String url) {
        return create(url, DEFAULT_MAX_CONNECTION);
    }

    /**
     *
     * @param url
     * @param maxConnection
     * @return
     */
    public static HttpClient create(final String url, final int maxConnection) {
        return create(url, maxConnection, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    /**
     *
     * @param url
     * @param connectionTimeoutInMillis
     * @param readTimeoutInMillis
     * @return
     */
    public static HttpClient create(final String url, final long connectionTimeoutInMillis, final long readTimeoutInMillis) {
        return create(url, DEFAULT_MAX_CONNECTION, connectionTimeoutInMillis, readTimeoutInMillis);
    }

    /**
     *
     * @param url
     * @param maxConnection
     * @param connectionTimeoutInMillis
     * @param readTimeoutInMillis
     * @return
     */
    public static HttpClient create(final String url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis) {
        return create(url, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, (HttpSettings) null);
    }

    /**
     *
     * @param url
     * @param maxConnection
     * @param connectionTimeoutInMillis
     * @param readTimeoutInMillis
     * @param settings
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static HttpClient create(final String url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis,
            final HttpSettings settings) throws UncheckedIOException {
        return create(url, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, settings, new AtomicInteger(0));
    }

    /**
     *
     * @param url
     * @param maxConnection
     * @param connectionTimeoutInMillis
     * @param readTimeoutInMillis
     * @param settings
     * @param sharedActiveConnectionCounter
     * @return
     */
    public static HttpClient create(final String url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis,
            final HttpSettings settings, final AtomicInteger sharedActiveConnectionCounter) {
        return create(url, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, settings, sharedActiveConnectionCounter, null);
    }

    /**
     *
     * @param url
     * @param maxConnection
     * @param connectionTimeoutInMillis
     * @param readTimeoutInMillis
     * @param executor
     * @return
     */
    public static HttpClient create(final String url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis,
            final Executor executor) {
        return create(url, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, null, executor);
    }

    /**
     *
     * @param url
     * @param maxConnection
     * @param connectionTimeoutInMillis
     * @param readTimeoutInMillis
     * @param settings
     * @param executor
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static HttpClient create(final String url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis,
            final HttpSettings settings, final Executor executor) throws UncheckedIOException {
        return create(url, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, settings, new AtomicInteger(0), executor);
    }

    /**
     *
     * @param url
     * @param maxConnection
     * @param connectionTimeoutInMillis
     * @param readTimeoutInMillis
     * @param settings
     * @param sharedActiveConnectionCounter
     * @param executor
     * @return
     */
    public static HttpClient create(final String url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis,
            final HttpSettings settings, final AtomicInteger sharedActiveConnectionCounter, final Executor executor) {
        return new HttpClient(url, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, settings, sharedActiveConnectionCounter, executor);
    }

    /**
     *
     * @param url
     * @return
     */
    public static HttpClient create(final URL url) {
        return create(url, DEFAULT_MAX_CONNECTION);
    }

    /**
     *
     * @param url
     * @param maxConnection
     * @return
     */
    public static HttpClient create(final URL url, final int maxConnection) {
        return create(url, maxConnection, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    /**
     *
     * @param url
     * @param connectionTimeoutInMillis
     * @param readTimeoutInMillis
     * @return
     */
    public static HttpClient create(final URL url, final long connectionTimeoutInMillis, final long readTimeoutInMillis) {
        return create(url, DEFAULT_MAX_CONNECTION, connectionTimeoutInMillis, readTimeoutInMillis);
    }

    /**
     *
     * @param url
     * @param maxConnection
     * @param connectionTimeoutInMillis
     * @param readTimeoutInMillis
     * @return
     */
    public static HttpClient create(final URL url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis) {
        return create(url, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, (HttpSettings) null);
    }

    /**
     *
     * @param url
     * @param maxConnection
     * @param connectionTimeoutInMillis
     * @param readTimeoutInMillis
     * @param settings
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static HttpClient create(final URL url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis,
            final HttpSettings settings) throws UncheckedIOException {
        return create(url, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, settings, new AtomicInteger(0));
    }

    /**
     *
     * @param url
     * @param maxConnection
     * @param connectionTimeoutInMillis
     * @param readTimeoutInMillis
     * @param settings
     * @param sharedActiveConnectionCounter
     * @return
     */
    public static HttpClient create(final URL url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis,
            final HttpSettings settings, final AtomicInteger sharedActiveConnectionCounter) {
        return create(url, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, settings, sharedActiveConnectionCounter, null);
    }

    /**
     *
     * @param url
     * @param maxConnection
     * @param connectionTimeoutInMillis
     * @param readTimeoutInMillis
     * @param executor
     * @return
     */
    public static HttpClient create(final URL url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis,
            final Executor executor) {
        return create(url, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, null, executor);
    }

    /**
     *
     * @param url
     * @param maxConnection
     * @param connectionTimeoutInMillis
     * @param readTimeoutInMillis
     * @param settings
     * @param executor
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static HttpClient create(final URL url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis,
            final HttpSettings settings, final Executor executor) throws UncheckedIOException {
        return create(url, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, settings, new AtomicInteger(0), executor);
    }

    /**
     *
     * @param url
     * @param maxConnection
     * @param connectionTimeoutInMillis
     * @param readTimeoutInMillis
     * @param settings
     * @param sharedActiveConnectionCounter
     * @param executor
     * @return
     */
    public static HttpClient create(final URL url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis,
            final HttpSettings settings, final AtomicInteger sharedActiveConnectionCounter, final Executor executor) {
        return new HttpClient(url, null, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, settings, sharedActiveConnectionCounter, executor);
    }

    public String url() {
        return _url;
    }

    /**
     *
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public String get() throws UncheckedIOException {
        return get(String.class);
    }

    /**
     *
     * @param settings
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public String get(final HttpSettings settings) throws UncheckedIOException {
        return get(settings, String.class);
    }

    /**
     *
     * @param queryParameters
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public String get(final Object queryParameters) throws UncheckedIOException {
        return get(queryParameters, String.class);
    }

    /**
     *
     * @param queryParameters
     * @param settings
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public String get(final Object queryParameters, final HttpSettings settings) throws UncheckedIOException {
        return get(queryParameters, settings, String.class);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T get(final Class<T> resultClass) throws UncheckedIOException {
        return get(null, _settings, resultClass);
    }

    /**
     *
     * @param <T>
     * @param settings
     * @param resultClass
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T get(final HttpSettings settings, final Class<T> resultClass) throws UncheckedIOException {
        return get(null, settings, resultClass);
    }

    /**
     *
     * @param <T>
     * @param queryParameters
     * @param resultClass
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T get(final Object queryParameters, final Class<T> resultClass) throws UncheckedIOException {
        return get(queryParameters, _settings, resultClass);
    }

    /**
     *
     * @param <T>
     * @param queryParameters
     * @param settings
     * @param resultClass
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T get(final Object queryParameters, final HttpSettings settings, final Class<T> resultClass) throws UncheckedIOException {
        return execute(HttpMethod.GET, queryParameters, settings, resultClass);
    }

    /**
     *
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public String delete() throws UncheckedIOException {
        return delete(String.class);
    }

    /**
     *
     * @param settings
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public String delete(final HttpSettings settings) throws UncheckedIOException {
        return delete(settings, String.class);
    }

    /**
     *
     * @param queryParameters
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public String delete(final Object queryParameters) throws UncheckedIOException {
        return delete(queryParameters, String.class);
    }

    /**
     *
     * @param queryParameters
     * @param settings
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public String delete(final Object queryParameters, final HttpSettings settings) throws UncheckedIOException {
        return delete(queryParameters, settings, String.class);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T delete(final Class<T> resultClass) throws UncheckedIOException {
        return delete(null, _settings, resultClass);
    }

    /**
     *
     * @param <T>
     * @param queryParameters
     * @param resultClass
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T delete(final Object queryParameters, final Class<T> resultClass) throws UncheckedIOException {
        return delete(queryParameters, _settings, resultClass);
    }

    /**
     *
     * @param <T>
     * @param settings
     * @param resultClass
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T delete(final HttpSettings settings, final Class<T> resultClass) throws UncheckedIOException {
        return delete(null, settings, resultClass);
    }

    /**
     *
     * @param <T>
     * @param queryParameters
     * @param settings
     * @param resultClass
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T delete(final Object queryParameters, final HttpSettings settings, final Class<T> resultClass) throws UncheckedIOException {
        return execute(HttpMethod.DELETE, queryParameters, settings, resultClass);
    }

    /**
     *
     * @param request
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public String post(final Object request) throws UncheckedIOException {
        return post(request, String.class);
    }

    /**
     *
     * @param <T>
     * @param request
     * @param resultClass
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T post(final Object request, final Class<T> resultClass) throws UncheckedIOException {
        return post(request, _settings, resultClass);
    }

    /**
     *
     * @param request
     * @param settings
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public String post(final Object request, final HttpSettings settings) throws UncheckedIOException {
        return post(request, settings, String.class);
    }

    /**
     *
     * @param <T>
     * @param request
     * @param settings
     * @param resultClass
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T post(final Object request, final HttpSettings settings, final Class<T> resultClass) throws UncheckedIOException {
        return execute(HttpMethod.POST, request, settings, resultClass);
    }

    /**
     *
     * @param request
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public String put(final Object request) throws UncheckedIOException {
        return put(request, String.class);
    }

    /**
     *
     * @param <T>
     * @param request
     * @param resultClass
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T put(final Object request, final Class<T> resultClass) throws UncheckedIOException {
        return put(request, _settings, resultClass);
    }

    /**
     *
     * @param request
     * @param settings
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public String put(final Object request, final HttpSettings settings) throws UncheckedIOException {
        return put(request, settings, String.class);
    }

    /**
     *
     * @param <T>
     * @param request
     * @param settings
     * @param resultClass
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T put(final Object request, final HttpSettings settings, final Class<T> resultClass) throws UncheckedIOException {
        return execute(HttpMethod.PUT, request, settings, resultClass);
    }

    //    //    TODO HTTP
    //    //    METHOD PATCH
    //    //    is not
    //    //    supported by HttpURLConnection.
    //
    //    /**
    //         *
    //         * @param request
    //         * @return
    //         * @throws UncheckedIOException the unchecked IO exception
    //         */
    //    public String patch(final Object request) throws UncheckedIOException {
    //        return patch(request, String.class);
    //    }
    //
    //    /**
    //     *
    //     * @param request
    //     * @param settings
    //     * @return
    //     * @throws UncheckedIOException the unchecked IO exception
    //     */
    //    public String patch(final Object request, final HttpSettings settings) throws UncheckedIOException {
    //        return patch(request, settings, String.class);
    //    }
    //
    //    /**
    //     *
    //     * @param request
    //     * @param resultClass
    //     * @param <T>
    //     * @return
    //     * @throws UncheckedIOException the unchecked IO exception
    //     */
    //    public <T> T patch(final Object request, final Class<T> resultClass) throws UncheckedIOException {
    //        return patch(request, _settings, resultClass);
    //    }
    //
    //    /**
    //     *
    //     * @param request
    //     * @param settings
    //     * @param resultClass
    //     * @param <T>
    //     * @return
    //     * @throws UncheckedIOException the unchecked IO exception
    //     */
    //    public <T> T patch(final Object request, final HttpSettings settings, final Class<T> resultClass) throws UncheckedIOException {
    //        return execute(HttpMethod.PATCH, request, settings, resultClass);
    //    }
    //
    //    /**
    //     *
    //     * @param request
    //     * @return
    //     */
    //    public ContinuableFuture<String> asyncPatch(final Object request) {
    //        return asyncPatch(request, String.class);
    //    }
    //
    //    /**
    //     *
    //     * @param request
    //     * @param settings
    //     * @return
    //     */
    //    public ContinuableFuture<String> asyncPatch(final Object request, final HttpSettings settings) {
    //        return asyncPatch(request, settings, String.class);
    //    }
    //
    //    /**
    //     *
    //     * @param request
    //     * @param resultClass
    //     * @param <T>
    //     * @return
    //     */
    //    public <T> ContinuableFuture<T> asyncPatch(final Object request, final Class<T> resultClass) {
    //        return asyncPatch(request, _settings, resultClass);
    //    }
    //
    //    /**
    //     *
    //     * @param request
    //     * @param settings
    //     * @param resultClass
    //     * @param <T>
    //     * @return
    //     */
    //    public <T> ContinuableFuture<T> asyncPatch(final Object request, final HttpSettings settings, final Class<T> resultClass) {
    //        return asyncExecute(HttpMethod.PATCH, request, settings, resultClass);
    //    }

    /**
     *
     * @throws UncheckedIOException the unchecked IO exception
     */
    public void head() throws UncheckedIOException {
        head(_settings);
    }

    /**
     *
     * @param settings
     * @throws UncheckedIOException the unchecked IO exception
     */
    public void head(final HttpSettings settings) throws UncheckedIOException {
        execute(HttpMethod.HEAD, null, settings, Void.class);
    }

    //    //    TODO HTTP
    //    //    METHOD PATCH
    //    //    is not
    //    //    supported by HttpURLConnection.
    //
    //    /**
    //         *
    //         * @param request
    //         * @return
    //         * @throws UncheckedIOException the unchecked IO exception
    //         */
    //    public String patch(final Object request) throws UncheckedIOException {
    //        return patch(request, String.class);
    //    }
    //
    //    /**
    //     *
    //     * @param request
    //     * @param settings
    //     * @return
    //     * @throws UncheckedIOException the unchecked IO exception
    //     */
    //    public String patch(final Object request, final HttpSettings settings) throws UncheckedIOException {
    //        return patch(request, settings, String.class);
    //    }
    //
    //    /**
    //     *
    //     * @param request
    //     * @param resultClass
    //     * @param <T>
    //     * @return
    //     * @throws UncheckedIOException the unchecked IO exception
    //     */
    //    public <T> T patch(final Object request, final Class<T> resultClass) throws UncheckedIOException {
    //        return patch(request, _settings, resultClass);
    //    }
    //
    //    /**
    //     *
    //     * @param request
    //     * @param settings
    //     * @param resultClass
    //     * @param <T>
    //     * @return
    //     * @throws UncheckedIOException the unchecked IO exception
    //     */
    //    public <T> T patch(final Object request, final HttpSettings settings, final Class<T> resultClass) throws UncheckedIOException {
    //        return execute(HttpMethod.PATCH, request, settings, resultClass);
    //    }
    //
    //    /**
    //     *
    //     * @param request
    //     * @return
    //     */
    //    public ContinuableFuture<String> asyncPatch(final Object request) {
    //        return asyncPatch(request, String.class);
    //    }
    //
    //    /**
    //     *
    //     * @param request
    //     * @param settings
    //     * @return
    //     */
    //    public ContinuableFuture<String> asyncPatch(final Object request, final HttpSettings settings) {
    //        return asyncPatch(request, settings, String.class);
    //    }
    //
    //    /**
    //     *
    //     * @param request
    //     * @param resultClass
    //     * @param <T>
    //     * @return
    //     */
    //    public <T> ContinuableFuture<T> asyncPatch(final Object request, final Class<T> resultClass) {
    //        return asyncPatch(request, _settings, resultClass);
    //    }
    //
    //    /**
    //     *
    //     * @param request
    //     * @param settings
    //     * @param resultClass
    //     * @param <T>
    //     * @return
    //     */
    //    public <T> ContinuableFuture<T> asyncPatch(final Object request, final HttpSettings settings, final Class<T> resultClass) {
    //        return asyncExecute(HttpMethod.PATCH, request, settings, resultClass);
    //    }

    /**
     *
     * @param httpMethod
     * @param request
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public String execute(final HttpMethod httpMethod, final Object request) throws UncheckedIOException {
        return execute(httpMethod, request, String.class);
    }

    /**
     *
     * @param <T>
     * @param httpMethod
     * @param request
     * @param resultClass
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T execute(final HttpMethod httpMethod, final Object request, final Class<T> resultClass) throws UncheckedIOException {
        return execute(httpMethod, request, _settings, resultClass);
    }

    /**
     *
     * @param httpMethod
     * @param request
     * @param settings
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public String execute(final HttpMethod httpMethod, final Object request, final HttpSettings settings) throws UncheckedIOException {
        return execute(httpMethod, request, settings, String.class);
    }

    /**
     *
     * @param <T>
     * @param httpMethod
     * @param request
     * @param settings
     * @param resultClass
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T execute(final HttpMethod httpMethod, final Object request, final HttpSettings settings, final Class<T> resultClass)
            throws UncheckedIOException {
        return execute(httpMethod, request, settings, resultClass, null, null);
    }

    /**
     *
     * @param httpMethod
     * @param request
     * @param settings
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    public void execute(final HttpMethod httpMethod, final Object request, final HttpSettings settings, final File output) throws UncheckedIOException {
        OutputStream os = null;

        try {
            os = IOUtil.newFileOutputStream(output);
            execute(httpMethod, request, settings, os);
        } finally {
            IOUtil.close(os);
        }
    }

    /**
     *
     * @param httpMethod
     * @param request
     * @param settings
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    public void execute(final HttpMethod httpMethod, final Object request, final HttpSettings settings, final OutputStream output) throws UncheckedIOException {
        execute(httpMethod, request, settings, null, output, null);
    }

    /**
     *
     * @param httpMethod
     * @param request
     * @param settings
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    public void execute(final HttpMethod httpMethod, final Object request, final HttpSettings settings, final Writer output) throws UncheckedIOException {
        execute(httpMethod, request, settings, null, null, output);
    }

    /**
     *
     * @param httpMethod
     * @param request
     * @param settings
     * @param resultClass
     * @param outputStream
     * @param outputWriter
     * @param <T>
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    private <T> T execute(final HttpMethod httpMethod, final Object request, final HttpSettings settings, final Class<T> resultClass,
            final OutputStream outputStream, final Writer outputWriter) throws UncheckedIOException {
        final Charset requestCharset = HttpUtil.getRequestCharset(settings == null || settings.headers().isEmpty() ? _settings.headers() : settings.headers());
        final ContentFormat requestContentFormat = getContentFormat(settings);
        final boolean doOutput = request != null && !(httpMethod == HttpMethod.GET || httpMethod == HttpMethod.DELETE);

        final HttpURLConnection connection = openConnection(httpMethod, request, settings, doOutput, resultClass);
        final long sentRequestAtMillis = System.currentTimeMillis();
        InputStream is = null;
        OutputStream os = null;

        try { //NOSONAR
            if (request != null && (requireBody(httpMethod))) {
                os = HttpUtil.getOutputStream(connection, requestContentFormat, getContentType(settings), getContentEncoding(settings));

                final Type<Object> type = N.typeOf(request.getClass());

                if (request instanceof File) {
                    try (InputStream fileInputStream = IOUtil.newFileInputStream((File) request)) {
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
                        } else if (requestContentFormat == ContentFormat.FormUrlEncoded) {
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

            is = HttpUtil.getInputStream(connection, respContentFormat);

            if (!HttpUtil.isSuccessfulResponseCode(statusCode) && (resultClass == null || !resultClass.equals(HttpResponse.class))) {
                throw new UncheckedIOException(
                        new IOException(statusCode + ": " + connection.getResponseMessage() + ". " + IOUtil.readAllToString(is, respCharset)));
            }

            if (isOneWayRequest(httpMethod, settings, resultClass)) {
                return null;
            } else {
                if (outputStream != null) {
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
                    if (resultClass == null) {
                        return null; // refer to isOneWayRequest.
                    } else if (resultClass.equals(HttpResponse.class)) {
                        return (T) new HttpResponse(_url, sentRequestAtMillis, System.currentTimeMillis(), statusCode, connection.getResponseMessage(),
                                respHeaders, IOUtil.readAllBytes(is), respContentFormat, respCharset);
                    } else {
                        if (resultClass.equals(String.class)) {
                            return (T) IOUtil.readAllToString(is, respCharset);
                        } else if (byte[].class.equals(resultClass)) {
                            return (T) IOUtil.readAllBytes(is);
                        } else {
                            if (respContentFormat == ContentFormat.KRYO && HttpUtil.kryoParser != null) {
                                return HttpUtil.kryoParser.deserialize(is, resultClass);
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
     * Checks if is one way request.
     *
     * @param httpMethod
     * @param settings
     * @param resultClass
     * @return {@code true}, if is one way request
     */
    protected boolean isOneWayRequest(final HttpMethod httpMethod, final HttpSettings settings, final Class<?> resultClass) {
        return HttpMethod.HEAD == httpMethod || resultClass == null || Void.class.equals(resultClass)
                || (settings == null ? _settings.isOneWayRequest() : settings.isOneWayRequest());
    }

    /**
     * Gets the content format.
     *
     * @param settings
     * @return
     */
    protected ContentFormat getContentFormat(final HttpSettings settings) {
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
     * Gets the content type.
     *
     * @param settings
     * @return
     */
    protected String getContentType(final HttpSettings settings) {
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
     * Gets the content encoding.
     *
     * @param settings
     * @return
     */
    protected String getContentEncoding(final HttpSettings settings) {
        String contentEncoding = null;

        if (settings != null) {
            contentEncoding = settings.getContentEncoding();
        }

        if (Strings.isEmpty(contentEncoding)) {
            contentEncoding = _settings.getContentEncoding();
        }

        return contentEncoding;
    }

    private boolean requireBody(final HttpMethod httpMethod) {
        return httpMethod == HttpMethod.POST || httpMethod == HttpMethod.PUT || httpMethod == HttpMethod.PATCH;
    }

    /**
     *
     * @param httpMethod
     * @param settings
     * @param doOutput
     * @param resultClass
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public HttpURLConnection openConnection(final HttpMethod httpMethod, final HttpSettings settings, final boolean doOutput, final Class<?> resultClass)
            throws UncheckedIOException {
        return openConnection(httpMethod, null, settings, doOutput, resultClass);
    }

    /**
     *
     * @param httpMethod
     * @param queryParameters
     * @param settings
     * @param doOutput
     * @param resultClass
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public HttpURLConnection openConnection(final HttpMethod httpMethod, final Object queryParameters, final HttpSettings settings, final boolean doOutput,
            final Class<?> resultClass) throws UncheckedIOException {
        HttpURLConnection connection = null;

        if (_activeConnectionCounter.incrementAndGet() > _maxConnection) {
            _activeConnectionCounter.decrementAndGet();
            throw new RuntimeException("Can not get connection, exceeded max connection number: " + _maxConnection);
        }

        try {
            synchronized (_netURL) {
                URL netURL = _netURL;

                if (queryParameters != null && (httpMethod == HttpMethod.GET || httpMethod == HttpMethod.DELETE)) {
                    netURL = URI.create(URLEncodedUtil.encode(_url, queryParameters)).toURL();
                }

                final Proxy proxy = (settings == null ? _settings : settings).getProxy();

                if (proxy == null) {
                    connection = (HttpURLConnection) netURL.openConnection();
                } else {
                    connection = (HttpURLConnection) netURL.openConnection(proxy);
                }
            }

            if (connection instanceof HttpsURLConnection) {
                final SSLSocketFactory ssf = (settings == null ? _settings : settings).getSSLSocketFactory();

                if (ssf != null) {
                    ((HttpsURLConnection) connection).setSSLSocketFactory(ssf);
                }
            }

            int connectionTimeoutInMillis = _connectionTimeoutInMillis > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) _connectionTimeoutInMillis;

            if (settings != null && settings.getConnectionTimeout() > 0) {
                connectionTimeoutInMillis = Numbers.toIntExact(settings.getConnectionTimeout());
            }

            if (connectionTimeoutInMillis > 0) {
                connection.setConnectTimeout(connectionTimeoutInMillis);
            }

            int readTimeoutInMillis = _readTimeoutInMillis > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) _readTimeoutInMillis;

            if (settings != null && settings.getReadTimeout() > 0) {
                readTimeoutInMillis = Numbers.toIntExact(settings.getReadTimeout());
            }

            if (readTimeoutInMillis > 0) {
                connection.setReadTimeout(readTimeoutInMillis);
            }

            if (settings != null) {
                connection.setDoInput(settings.doInput());
                connection.setDoOutput(settings.doOutput());
            }

            connection.setUseCaches((settings != null && settings.getUseCaches()) || (_settings != null && _settings.getUseCaches()));

            setHttpProperties(connection, settings == null || settings.headers().isEmpty() ? _settings : settings);

            if (isOneWayRequest(httpMethod, settings, resultClass)) {
                connection.setDoInput(false);
            } else {
                connection.setDoOutput(doOutput);
            }

            connection.setRequestMethod(httpMethod.name());

            return connection;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Sets the http properties.
     *
     * @param connection
     * @param settings
     * @throws UncheckedIOException the unchecked IO exception
     */
    void setHttpProperties(final HttpURLConnection connection, final HttpSettings settings) throws UncheckedIOException {
        final HttpHeaders headers = settings.headers();

        if (headers != null) {
            Object headerValue = null;

            for (final String headerName : headers.headerNameSet()) {
                // lazy set content-encoding
                // because if content-encoding(lz4/snappy/kryo...) is set but no parameter/result write to OutputStream,
                // error may happen when read the input stream in sever side.

                if (Names.CONTENT_ENCODING.equalsIgnoreCase(headerName)) {
                    continue;
                }

                headerValue = headers.get(headerName);

                connection.setRequestProperty(headerName, HttpHeaders.valueOf(headerValue));
            }
        }
    }

    /**
     *
     * @param os
     * @param is
     * @param connection
     */
    void close(final OutputStream os, final InputStream is, @SuppressWarnings("unused") final HttpURLConnection connection) { //NOSONAR
        try {
            IOUtil.closeQuietly(os);
            IOUtil.closeQuietly(is);
        } finally {
            _activeConnectionCounter.decrementAndGet();
        }

        // connection.disconnect();
    }

    public ContinuableFuture<String> asyncGet() {
        return asyncGet(String.class);
    }

    /**
     *
     * @param settings
     * @return
     */
    public ContinuableFuture<String> asyncGet(final HttpSettings settings) {
        return asyncGet(settings, String.class);
    }

    /**
     *
     * @param queryParameters
     * @return
     */
    public ContinuableFuture<String> asyncGet(final Object queryParameters) {
        return asyncGet(queryParameters, String.class);
    }

    /**
     *
     * @param queryParameters
     * @param settings
     * @return
     */
    public ContinuableFuture<String> asyncGet(final Object queryParameters, final HttpSettings settings) {
        return asyncGet(queryParameters, settings, String.class);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @return
     */
    public <T> ContinuableFuture<T> asyncGet(final Class<T> resultClass) {
        return asyncGet(null, _settings, resultClass);
    }

    /**
     *
     * @param <T>
     * @param queryParameters
     * @param resultClass
     * @return
     */
    public <T> ContinuableFuture<T> asyncGet(final Object queryParameters, final Class<T> resultClass) {
        return asyncGet(queryParameters, _settings, resultClass);
    }

    /**
     *
     * @param <T>
     * @param settings
     * @param resultClass
     * @return
     */
    public <T> ContinuableFuture<T> asyncGet(final HttpSettings settings, final Class<T> resultClass) {
        return asyncGet(null, settings, resultClass);
    }

    /**
     *
     * @param <T>
     * @param queryParameters
     * @param settings
     * @param resultClass
     * @return
     */
    public <T> ContinuableFuture<T> asyncGet(final Object queryParameters, final HttpSettings settings, final Class<T> resultClass) {
        return asyncExecute(HttpMethod.GET, queryParameters, settings, resultClass);
    }

    public ContinuableFuture<String> asyncDelete() {
        return asyncDelete(String.class);
    }

    /**
     *
     * @param queryParameters
     * @return
     */
    public ContinuableFuture<String> asyncDelete(final Object queryParameters) {
        return asyncDelete(queryParameters, String.class);
    }

    /**
     *
     * @param settings
     * @return
     */
    public ContinuableFuture<String> asyncDelete(final HttpSettings settings) {
        return asyncDelete(settings, String.class);
    }

    /**
     *
     * @param queryParameters
     * @param settings
     * @return
     */
    public ContinuableFuture<String> asyncDelete(final Object queryParameters, final HttpSettings settings) {
        return asyncDelete(queryParameters, settings, String.class);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @return
     */
    public <T> ContinuableFuture<T> asyncDelete(final Class<T> resultClass) {
        return asyncDelete(null, _settings, resultClass);
    }

    /**
     *
     * @param <T>
     * @param queryParameters
     * @param resultClass
     * @return
     */
    public <T> ContinuableFuture<T> asyncDelete(final Object queryParameters, final Class<T> resultClass) {
        return asyncDelete(queryParameters, _settings, resultClass);
    }

    /**
     *
     * @param <T>
     * @param settings
     * @param resultClass
     * @return
     */
    public <T> ContinuableFuture<T> asyncDelete(final HttpSettings settings, final Class<T> resultClass) {
        return asyncDelete(null, settings, resultClass);
    }

    /**
     *
     * @param <T>
     * @param queryParameters
     * @param settings
     * @param resultClass
     * @return
     */
    public <T> ContinuableFuture<T> asyncDelete(final Object queryParameters, final HttpSettings settings, final Class<T> resultClass) {
        return asyncExecute(HttpMethod.DELETE, queryParameters, settings, resultClass);
    }

    /**
     *
     * @param request
     * @return
     */
    public ContinuableFuture<String> asyncPost(final Object request) {
        return asyncPost(request, String.class);
    }

    /**
     *
     * @param <T>
     * @param request
     * @param resultClass
     * @return
     */
    public <T> ContinuableFuture<T> asyncPost(final Object request, final Class<T> resultClass) {
        return asyncPost(request, _settings, resultClass);
    }

    /**
     *
     * @param request
     * @param settings
     * @return
     */
    public ContinuableFuture<String> asyncPost(final Object request, final HttpSettings settings) {
        return asyncPost(request, settings, String.class);
    }

    /**
     *
     * @param <T>
     * @param request
     * @param settings
     * @param resultClass
     * @return
     */
    public <T> ContinuableFuture<T> asyncPost(final Object request, final HttpSettings settings, final Class<T> resultClass) {
        return asyncExecute(HttpMethod.POST, request, settings, resultClass);
    }

    /**
     *
     * @param request
     * @return
     */
    public ContinuableFuture<String> asyncPut(final Object request) {
        return asyncPut(request, String.class);
    }

    /**
     *
     * @param <T>
     * @param request
     * @param resultClass
     * @return
     */
    public <T> ContinuableFuture<T> asyncPut(final Object request, final Class<T> resultClass) {
        return asyncPut(request, _settings, resultClass);
    }

    /**
     *
     * @param request
     * @param settings
     * @return
     */
    public ContinuableFuture<String> asyncPut(final Object request, final HttpSettings settings) {
        return asyncPut(request, settings, String.class);
    }

    /**
     *
     * @param <T>
     * @param request
     * @param settings
     * @param resultClass
     * @return
     */
    public <T> ContinuableFuture<T> asyncPut(final Object request, final HttpSettings settings, final Class<T> resultClass) {
        return asyncExecute(HttpMethod.PUT, request, settings, resultClass);
    }

    // TODO HTTP METHOD PATCH is not supported by HttpURLConnection.
    //    /**
    //     *
    //     * @param request
    //     * @return
    //     * @throws UncheckedIOException the unchecked IO exception
    //     */
    //    public String patch(final Object request) throws UncheckedIOException {
    //        return patch(request, String.class);
    //    }
    //
    //    /**
    //     *
    //     * @param request
    //     * @param settings
    //     * @return
    //     * @throws UncheckedIOException the unchecked IO exception
    //     */
    //    public String patch(final Object request, final HttpSettings settings) throws UncheckedIOException {
    //        return patch(request, settings, String.class);
    //    }
    //
    //    /**
    //     *
    //     * @param <T>
    //     * @param request
    //     * @param resultClass
    //     * @return
    //     * @throws UncheckedIOException the unchecked IO exception
    //     */
    //    public <T> T patch(final Object request, final Class<T> resultClass) throws UncheckedIOException {
    //        return patch(request, _settings, resultClass);
    //    }
    //
    //    /**
    //     *
    //     * @param <T>
    //     * @param request
    //     * @param settings
    //     * @param resultClass
    //     * @return
    //     * @throws UncheckedIOException the unchecked IO exception
    //     */
    //    public <T> T patch(final Object request, final HttpSettings settings, final Class<T> resultClass) throws UncheckedIOException {
    //        return execute(HttpMethod.PATCH, request, settings, resultClass);
    //    }
    //
    //    /**
    //     *
    //     * @param request
    //     * @return
    //     */
    //    public ContinuableFuture<String> asyncPatch(final Object request) {
    //        return asyncPatch(request, String.class);
    //    }
    //
    //    /**
    //     *
    //     * @param request
    //     * @param settings
    //     * @return
    //     */
    //    public ContinuableFuture<String> asyncPatch(final Object request, final HttpSettings settings) {
    //        return asyncPatch(request, settings, String.class);
    //    }
    //
    //    /**
    //     *
    //     * @param <T>
    //     * @param request
    //     * @param resultClass
    //     * @return
    //     */
    //    public <T> ContinuableFuture<T> asyncPatch(final Object request, final Class<T> resultClass) {
    //        return asyncPatch(request, _settings, resultClass);
    //    }
    //
    //    /**
    //     *
    //     * @param <T>
    //     * @param request
    //     * @param settings
    //     * @param resultClass
    //     * @return
    //     */
    //    public <T> ContinuableFuture<T> asyncPatch(final Object request, final HttpSettings settings, final Class<T> resultClass) {
    //        return asyncExecute(HttpMethod.PATCH, request, settings, resultClass);
    //    }

    public ContinuableFuture<Void> asyncHead() {
        return asyncHead(_settings);
    }

    /**
     *
     * @param settings
     * @return
     */
    public ContinuableFuture<Void> asyncHead(final HttpSettings settings) {
        return asyncExecute(HttpMethod.HEAD, null, settings, Void.class);
    }

    /**
     *
     * @param httpMethod
     * @param request
     * @return
     */
    public ContinuableFuture<String> asyncExecute(final HttpMethod httpMethod, final Object request) {
        return asyncExecute(httpMethod, request, String.class);
    }

    /**
     *
     * @param <T>
     * @param httpMethod
     * @param request
     * @param resultClass
     * @return
     */
    public <T> ContinuableFuture<T> asyncExecute(final HttpMethod httpMethod, final Object request, final Class<T> resultClass) {
        return asyncExecute(httpMethod, request, _settings, resultClass);
    }

    /**
     *
     * @param httpMethod
     * @param request
     * @param settings
     * @return
     */
    public ContinuableFuture<String> asyncExecute(final HttpMethod httpMethod, final Object request, final HttpSettings settings) {
        return asyncExecute(httpMethod, request, settings, String.class);
    }

    /**
     *
     * @param <T>
     * @param httpMethod
     * @param request
     * @param settings
     * @param resultClass
     * @return
     */
    public <T> ContinuableFuture<T> asyncExecute(final HttpMethod httpMethod, final Object request, final HttpSettings settings, final Class<T> resultClass) {
        final Callable<T> cmd = () -> execute(httpMethod, request, settings, resultClass);

        return _asyncExecutor.execute(cmd);
    }

    /**
     *
     * @param httpMethod
     * @param request
     * @param settings
     * @param output
     * @return
     */
    public ContinuableFuture<Void> asyncExecute(final HttpMethod httpMethod, final Object request, final HttpSettings settings, final File output) {
        final Callable<Void> cmd = () -> {
            execute(httpMethod, request, settings, output);

            return null;
        };

        return _asyncExecutor.execute(cmd);
    }

    /**
     *
     * @param httpMethod
     * @param request
     * @param settings
     * @param output
     * @return
     */
    public ContinuableFuture<Void> asyncExecute(final HttpMethod httpMethod, final Object request, final HttpSettings settings, final OutputStream output) {
        final Callable<Void> cmd = () -> {
            execute(httpMethod, request, settings, output);

            return null;
        };

        return _asyncExecutor.execute(cmd);
    }

    /**
     *
     * @param httpMethod
     * @param request
     * @param settings
     * @param output
     * @return
     */
    public ContinuableFuture<Void> asyncExecute(final HttpMethod httpMethod, final Object request, final HttpSettings settings, final Writer output) {
        final Callable<Void> cmd = () -> {
            execute(httpMethod, request, settings, output);

            return null;
        };

        return _asyncExecutor.execute(cmd);
    }

    /**
     * Close.
     */
    public synchronized void close() {
        // do nothing.
    }
}
