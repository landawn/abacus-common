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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
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
import com.landawn.abacus.util.BufferedReader;
import com.landawn.abacus.util.BufferedWriter;
import com.landawn.abacus.util.ContinuableFuture;
import com.landawn.abacus.util.ExceptionUtil;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.URLEncodedUtil;

/**
 * Any header can be set into the parameter <code>settings</code>
 *
 * <br>HttpClient is thread safe.</br>
 *
 * @author Haiyang Li
 * @since 0.8
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
    protected final String _url;

    protected final int _maxConnection;

    protected final long _connectionTimeoutInMillis;

    protected final long _readTimeoutInMillis;

    protected final HttpSettings _settings;

    protected final AsyncExecutor _asyncExecutor;

    protected final URL _netURL;

    protected final AtomicInteger _activeConnectionCounter;

    protected HttpClient(String url, int maxConnection, long connectionTimeoutInMillis, long readTimeoutInMillis, HttpSettings settings,
            final AtomicInteger sharedActiveConnectionCounter, final Executor executor) {
        if (N.isNullOrEmpty(url)) {
            throw new IllegalArgumentException("url can't be null or empty");
        }

        if ((maxConnection < 0) || (connectionTimeoutInMillis < 0) || (readTimeoutInMillis < 0)) {
            throw new IllegalArgumentException("maxConnection, connectionTimeoutInMillis or readTimeoutInMillis can't be less than 0: " + maxConnection + ", "
                    + connectionTimeoutInMillis + ", " + readTimeoutInMillis);
        }

        this._url = url;
        this._maxConnection = (maxConnection == 0) ? DEFAULT_MAX_CONNECTION : maxConnection;
        this._connectionTimeoutInMillis = (connectionTimeoutInMillis == 0) ? DEFAULT_CONNECTION_TIMEOUT : connectionTimeoutInMillis;
        this._readTimeoutInMillis = (readTimeoutInMillis == 0) ? DEFAULT_READ_TIMEOUT : readTimeoutInMillis;
        this._settings = settings == null ? HttpSettings.create() : settings;

        _asyncExecutor = executor == null ? HttpUtil.DEFAULT_ASYNC_EXECUTOR : new AsyncExecutor(executor);

        try {
            this._netURL = new URL(url);
        } catch (MalformedURLException e) {
            throw ExceptionUtil.toRuntimeException(e);
        }

        this._activeConnectionCounter = sharedActiveConnectionCounter;
    }

    /**
     *
     * @param url
     * @return
     */
    public static HttpClient create(String url) {
        return create(url, DEFAULT_MAX_CONNECTION);
    }

    /**
     *
     * @param url
     * @param maxConnection
     * @return
     */
    public static HttpClient create(String url, int maxConnection) {
        return create(url, maxConnection, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    /**
     *
     * @param url
     * @param connectionTimeoutInMillis
     * @param readTimeoutInMillis
     * @return
     */
    public static HttpClient create(String url, long connectionTimeoutInMillis, long readTimeoutInMillis) {
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
    public static HttpClient create(String url, int maxConnection, long connectionTimeoutInMillis, long readTimeoutInMillis) {
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
    public static HttpClient create(String url, int maxConnection, long connectionTimeoutInMillis, long readTimeoutInMillis, HttpSettings settings)
            throws UncheckedIOException {
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
    public static HttpClient create(String url, int maxConnection, long connectionTimeoutInMillis, long readTimeoutInMillis, HttpSettings settings,
            final AtomicInteger sharedActiveConnectionCounter) {
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
    public static HttpClient create(String url, int maxConnection, long connectionTimeoutInMillis, long readTimeoutInMillis, final Executor executor) {
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
    public static HttpClient create(String url, int maxConnection, long connectionTimeoutInMillis, long readTimeoutInMillis, HttpSettings settings,
            final Executor executor) throws UncheckedIOException {
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
    public static HttpClient create(String url, int maxConnection, long connectionTimeoutInMillis, long readTimeoutInMillis, HttpSettings settings,
            final AtomicInteger sharedActiveConnectionCounter, final Executor executor) {
        return new HttpClient(url, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, settings, sharedActiveConnectionCounter, executor);
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
        return get(String.class, settings);
    }

    /**
     *
     * @param queryParameters
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public String get(final Object queryParameters) throws UncheckedIOException {
        return get(String.class, queryParameters);
    }

    /**
     *
     * @param queryParameters
     * @param settings
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public String get(final Object queryParameters, final HttpSettings settings) throws UncheckedIOException {
        return get(String.class, queryParameters, settings);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T get(final Class<T> resultClass) throws UncheckedIOException {
        return get(resultClass, null, _settings);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @param settings
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T get(final Class<T> resultClass, final HttpSettings settings) throws UncheckedIOException {
        return get(resultClass, null, settings);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @param queryParameters
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T get(final Class<T> resultClass, final Object queryParameters) throws UncheckedIOException {
        return get(resultClass, queryParameters, _settings);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @param queryParameters
     * @param settings
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T get(final Class<T> resultClass, final Object queryParameters, final HttpSettings settings) throws UncheckedIOException {
        return execute(resultClass, HttpMethod.GET, queryParameters, settings);
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
        return asyncGet(String.class, settings);
    }

    /**
     *
     * @param queryParameters
     * @return
     */
    public ContinuableFuture<String> asyncGet(final Object queryParameters) {
        return asyncGet(String.class, queryParameters);
    }

    /**
     *
     * @param queryParameters
     * @param settings
     * @return
     */
    public ContinuableFuture<String> asyncGet(final Object queryParameters, final HttpSettings settings) {
        return asyncGet(String.class, queryParameters, settings);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @return
     */
    public <T> ContinuableFuture<T> asyncGet(final Class<T> resultClass) {
        return asyncGet(resultClass, null, _settings);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @param settings
     * @return
     */
    public <T> ContinuableFuture<T> asyncGet(final Class<T> resultClass, final HttpSettings settings) {
        return asyncGet(resultClass, null, settings);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @param queryParameters
     * @return
     */
    public <T> ContinuableFuture<T> asyncGet(final Class<T> resultClass, final Object queryParameters) {
        return asyncGet(resultClass, queryParameters, _settings);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @param queryParameters
     * @param settings
     * @return
     */
    public <T> ContinuableFuture<T> asyncGet(final Class<T> resultClass, final Object queryParameters, final HttpSettings settings) {
        return asyncExecute(resultClass, HttpMethod.GET, queryParameters, settings);
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
        return delete(String.class, settings);
    }

    /**
     *
     * @param queryParameters
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public String delete(final Object queryParameters) throws UncheckedIOException {
        return delete(String.class, queryParameters);
    }

    /**
     *
     * @param queryParameters
     * @param settings
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public String delete(final Object queryParameters, final HttpSettings settings) throws UncheckedIOException {
        return delete(String.class, queryParameters, settings);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T delete(final Class<T> resultClass) throws UncheckedIOException {
        return delete(resultClass, null, _settings);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @param settings
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T delete(final Class<T> resultClass, final HttpSettings settings) throws UncheckedIOException {
        return delete(resultClass, null, settings);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @param queryParameters
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T delete(final Class<T> resultClass, final Object queryParameters) throws UncheckedIOException {
        return delete(resultClass, queryParameters, _settings);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @param queryParameters
     * @param settings
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T delete(final Class<T> resultClass, final Object queryParameters, final HttpSettings settings) throws UncheckedIOException {
        return execute(resultClass, HttpMethod.DELETE, queryParameters, settings);
    }

    public ContinuableFuture<String> asyncDelete() {
        return asyncDelete(String.class);
    }

    /**
     *
     * @param settings
     * @return
     */
    public ContinuableFuture<String> asyncDelete(final HttpSettings settings) {
        return asyncDelete(String.class, settings);
    }

    /**
     *
     * @param queryParameters
     * @return
     */
    public ContinuableFuture<String> asyncDelete(final Object queryParameters) {
        return asyncDelete(String.class, queryParameters);
    }

    /**
     *
     * @param queryParameters
     * @param settings
     * @return
     */
    public ContinuableFuture<String> asyncDelete(final Object queryParameters, final HttpSettings settings) {
        return asyncDelete(String.class, queryParameters, settings);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @return
     */
    public <T> ContinuableFuture<T> asyncDelete(final Class<T> resultClass) {
        return asyncDelete(resultClass, null, _settings);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @param settings
     * @return
     */
    public <T> ContinuableFuture<T> asyncDelete(final Class<T> resultClass, final HttpSettings settings) {
        return asyncDelete(resultClass, null, settings);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @param queryParameters
     * @return
     */
    public <T> ContinuableFuture<T> asyncDelete(final Class<T> resultClass, final Object queryParameters) {
        return asyncDelete(resultClass, queryParameters, _settings);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @param queryParameters
     * @param settings
     * @return
     */
    public <T> ContinuableFuture<T> asyncDelete(final Class<T> resultClass, final Object queryParameters, final HttpSettings settings) {
        return asyncExecute(resultClass, HttpMethod.DELETE, queryParameters, settings);
    }

    /**
     *
     * @param request
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public String post(final Object request) throws UncheckedIOException {
        return post(String.class, request);
    }

    /**
     *
     * @param request
     * @param settings
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public String post(final Object request, final HttpSettings settings) throws UncheckedIOException {
        return post(String.class, request, settings);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @param request
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T post(final Class<T> resultClass, final Object request) throws UncheckedIOException {
        return post(resultClass, request, _settings);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @param request
     * @param settings
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T post(final Class<T> resultClass, final Object request, final HttpSettings settings) throws UncheckedIOException {
        return execute(resultClass, HttpMethod.POST, request, settings);
    }

    /**
     *
     * @param request
     * @return
     */
    public ContinuableFuture<String> asyncPost(final Object request) {
        return asyncPost(String.class, request);
    }

    /**
     *
     * @param request
     * @param settings
     * @return
     */
    public ContinuableFuture<String> asyncPost(final Object request, final HttpSettings settings) {
        return asyncPost(String.class, request, settings);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @param request
     * @return
     */
    public <T> ContinuableFuture<T> asyncPost(final Class<T> resultClass, final Object request) {
        return asyncPost(resultClass, request, _settings);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @param request
     * @param settings
     * @return
     */
    public <T> ContinuableFuture<T> asyncPost(final Class<T> resultClass, final Object request, final HttpSettings settings) {
        return asyncExecute(resultClass, HttpMethod.POST, request, settings);
    }

    /**
     *
     * @param request
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public String put(final Object request) throws UncheckedIOException {
        return put(String.class, request);
    }

    /**
     *
     * @param request
     * @param settings
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public String put(final Object request, final HttpSettings settings) throws UncheckedIOException {
        return put(String.class, request, settings);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @param request
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T put(final Class<T> resultClass, final Object request) throws UncheckedIOException {
        return put(resultClass, request, _settings);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @param request
     * @param settings
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T put(final Class<T> resultClass, final Object request, final HttpSettings settings) throws UncheckedIOException {
        return execute(resultClass, HttpMethod.PUT, request, settings);
    }

    /**
     *
     * @param request
     * @return
     */
    public ContinuableFuture<String> asyncPut(final Object request) {
        return asyncPut(String.class, request);
    }

    /**
     *
     * @param request
     * @param settings
     * @return
     */
    public ContinuableFuture<String> asyncPut(final Object request, final HttpSettings settings) {
        return asyncPut(String.class, request, settings);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @param request
     * @return
     */
    public <T> ContinuableFuture<T> asyncPut(final Class<T> resultClass, final Object request) {
        return asyncPut(resultClass, request, _settings);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @param request
     * @param settings
     * @return
     */
    public <T> ContinuableFuture<T> asyncPut(final Class<T> resultClass, final Object request, final HttpSettings settings) {
        return asyncExecute(resultClass, HttpMethod.PUT, request, settings);
    }

    // TODO HTTP METHOD PATCH is not supported by HttpURLConnection.
    //    /**
    //     *
    //     * @param request
    //     * @return
    //     * @throws UncheckedIOException the unchecked IO exception
    //     */
    //    public String patch(final Object request) throws UncheckedIOException {
    //        return patch(String.class, request);
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
    //        return patch(String.class, request, settings);
    //    }
    //
    //    /**
    //     *
    //     * @param <T>
    //     * @param resultClass
    //     * @param request
    //     * @return
    //     * @throws UncheckedIOException the unchecked IO exception
    //     */
    //    public <T> T patch(final Class<T> resultClass, final Object request) throws UncheckedIOException {
    //        return patch(resultClass, request, _settings);
    //    }
    //
    //    /**
    //     *
    //     * @param <T>
    //     * @param resultClass
    //     * @param request
    //     * @param settings
    //     * @return
    //     * @throws UncheckedIOException the unchecked IO exception
    //     */
    //    public <T> T patch(final Class<T> resultClass, final Object request, final HttpSettings settings) throws UncheckedIOException {
    //        return execute(resultClass, HttpMethod.PATCH, request, settings);
    //    }
    //
    //    /**
    //     *
    //     * @param request
    //     * @return
    //     */
    //    public ContinuableFuture<String> asyncPatch(final Object request) {
    //        return asyncPatch(String.class, request);
    //    }
    //
    //    /**
    //     *
    //     * @param request
    //     * @param settings
    //     * @return
    //     */
    //    public ContinuableFuture<String> asyncPatch(final Object request, final HttpSettings settings) {
    //        return asyncPatch(String.class, request, settings);
    //    }
    //
    //    /**
    //     *
    //     * @param <T>
    //     * @param resultClass
    //     * @param request
    //     * @return
    //     */
    //    public <T> ContinuableFuture<T> asyncPatch(final Class<T> resultClass, final Object request) {
    //        return asyncPatch(resultClass, request, _settings);
    //    }
    //
    //    /**
    //     *
    //     * @param <T>
    //     * @param resultClass
    //     * @param request
    //     * @param settings
    //     * @return
    //     */
    //    public <T> ContinuableFuture<T> asyncPatch(final Class<T> resultClass, final Object request, final HttpSettings settings) {
    //        return asyncExecute(resultClass, HttpMethod.PATCH, request, settings);
    //    }

    /**
     *
     * @param httpMethod
     * @param request
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public String execute(final HttpMethod httpMethod, final Object request) throws UncheckedIOException {
        return execute(String.class, httpMethod, request);
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
        return execute(String.class, httpMethod, request, settings);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @param httpMethod
     * @param request
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T execute(final Class<T> resultClass, final HttpMethod httpMethod, final Object request) throws UncheckedIOException {
        return execute(resultClass, httpMethod, request, _settings);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @param httpMethod
     * @param request
     * @param settings
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T execute(final Class<T> resultClass, final HttpMethod httpMethod, final Object request, final HttpSettings settings)
            throws UncheckedIOException {
        return execute(resultClass, null, null, httpMethod, request, settings);
    }

    /**
     *
     * @param output
     * @param httpMethod
     * @param request
     * @param settings
     * @throws UncheckedIOException the unchecked IO exception
     */
    public void execute(final File output, final HttpMethod httpMethod, final Object request, final HttpSettings settings) throws UncheckedIOException {
        OutputStream os = null;

        try {
            os = IOUtil.newFileOutputStream(output);
            execute(os, httpMethod, request, settings);
        } finally {
            IOUtil.close(os);
        }
    }

    /**
     *
     * @param output
     * @param httpMethod
     * @param request
     * @param settings
     * @throws UncheckedIOException the unchecked IO exception
     */
    public void execute(final OutputStream output, final HttpMethod httpMethod, final Object request, final HttpSettings settings) throws UncheckedIOException {
        execute(null, output, null, httpMethod, request, settings);
    }

    /**
     *
     * @param output
     * @param httpMethod
     * @param request
     * @param settings
     * @throws UncheckedIOException the unchecked IO exception
     */
    public void execute(final Writer output, final HttpMethod httpMethod, final Object request, final HttpSettings settings) throws UncheckedIOException {
        execute(null, null, output, httpMethod, request, settings);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @param outputStream
     * @param outputWriter
     * @param httpMethod
     * @param request
     * @param settings
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    private <T> T execute(final Class<T> resultClass, final OutputStream outputStream, final Writer outputWriter, final HttpMethod httpMethod,
            final Object request, final HttpSettings settings) throws UncheckedIOException {
        final Charset requestCharset = HttpUtil.getRequestCharset(settings == null || settings.headers().isEmpty() ? _settings.headers() : settings.headers());
        final ContentFormat requestContentFormat = getContentFormat(settings);
        final boolean doOutput = request != null && !(httpMethod.equals(HttpMethod.GET) || httpMethod.equals(HttpMethod.DELETE));

        final HttpURLConnection connection = openConnection(httpMethod, request, doOutput, settings);
        final long sentRequestAtMillis = System.currentTimeMillis();
        InputStream is = null;
        OutputStream os = null;

        try {
            if (request != null && (requireBody(httpMethod))) {
                os = HttpUtil.getOutputStream(connection, requestContentFormat, getContentType(settings), getContentEncoding(settings));

                Type<Object> type = N.typeOf(request.getClass());

                if (request instanceof File) {
                    try (InputStream fileInputStream = IOUtil.newFileInputStream((File) request)) {
                        IOUtil.write(os, fileInputStream);
                    }
                } else if (type.isInputStream()) {
                    IOUtil.write(os, (InputStream) request);
                } else if (type.isReader()) {
                    final BufferedWriter bw = Objectory.createBufferedWriter(new OutputStreamWriter(os, requestCharset));

                    try {
                        IOUtil.write(bw, (Reader) request);

                        bw.flush();
                    } finally {
                        Objectory.recycle(bw);
                    }
                } else {
                    if (request instanceof String) {
                        IOUtil.write(os, ((String) request).getBytes(requestCharset));
                    } else if (request.getClass().equals(byte[].class)) {
                        IOUtil.write(os, (byte[]) request);
                    } else {
                        if (requestContentFormat == ContentFormat.KRYO && HttpUtil.kryoParser != null) {
                            HttpUtil.kryoParser.serialize(os, request);
                        } else if (requestContentFormat == ContentFormat.FormUrlEncoded) {
                            IOUtil.write(os, URLEncodedUtil.encode(request, requestCharset).getBytes(requestCharset));
                        } else {
                            final BufferedWriter bw = Objectory.createBufferedWriter(new OutputStreamWriter(os, requestCharset));

                            try {
                                HttpUtil.getParser(requestContentFormat).serialize(bw, request);

                                bw.flush();
                            } finally {
                                Objectory.recycle(bw);
                            }
                        }
                    }
                }

                HttpUtil.flush(os);
            }

            final int code = connection.getResponseCode();
            final Map<String, List<String>> respHeaders = connection.getHeaderFields();
            final Charset respCharset = HttpUtil.getResponseCharset(respHeaders, requestCharset);
            final ContentFormat respContentFormat = HttpUtil.getResponseContentFormat(respHeaders, requestContentFormat);

            is = HttpUtil.getInputStream(connection, respContentFormat);

            if (!HttpUtil.isSuccessfulResponseCode(code) && (resultClass == null || !resultClass.equals(HttpResponse.class))) {
                throw new UncheckedIOException(new IOException(code + ": " + connection.getResponseMessage() + ". " + IOUtil.readAllToString(is, respCharset)));
            }

            if (isOneWayRequest(settings)) {
                return null;
            } else {
                if (outputStream != null) {
                    IOUtil.write(outputStream, is, true);

                    return null;
                } else if (outputWriter != null) {
                    final BufferedReader br = Objectory.createBufferedReader(new InputStreamReader(is, respCharset));

                    try {
                        IOUtil.write(outputWriter, br, true);
                    } finally {
                        Objectory.recycle(br);
                    }

                    return null;
                } else {
                    if (resultClass != null && resultClass.equals(HttpResponse.class)) {
                        return (T) new HttpResponse(sentRequestAtMillis, System.currentTimeMillis(), code, connection.getResponseMessage(), respHeaders,
                                IOUtil.readAllBytes(is), respContentFormat, respCharset);
                    } else {
                        if (resultClass == null || resultClass.equals(String.class)) {
                            return (T) IOUtil.readAllToString(is, respCharset);
                        } else if (byte[].class.equals(resultClass)) {
                            return (T) IOUtil.readAllBytes(is);
                        } else {
                            if (respContentFormat == ContentFormat.KRYO && HttpUtil.kryoParser != null) {
                                return HttpUtil.kryoParser.deserialize(resultClass, is);
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
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            close(os, is, connection);
        }
    }

    /**
     * Checks if is one way request.
     *
     * @param settings
     * @return true, if is one way request
     */
    protected boolean isOneWayRequest(HttpSettings settings) {
        return _settings.isOneWayRequest() || ((settings != null) && settings.isOneWayRequest());
    }

    /**
     * Gets the content format.
     *
     * @param settings
     * @return
     */
    protected ContentFormat getContentFormat(HttpSettings settings) {
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
    protected String getContentType(HttpSettings settings) {
        String contentType = null;

        if (settings != null) {
            contentType = settings.getContentType();
        }

        if (N.isNullOrEmpty(contentType)) {
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
    protected String getContentEncoding(HttpSettings settings) {
        String contentEncoding = null;

        if (settings != null) {
            contentEncoding = settings.getContentEncoding();
        }

        if (N.isNullOrEmpty(contentEncoding)) {
            contentEncoding = _settings.getContentEncoding();
        }

        return contentEncoding;
    }

    /**
     *
     * @param httpMethod
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    HttpURLConnection openConnection(HttpMethod httpMethod) throws UncheckedIOException {
        return openConnection(httpMethod, requireBody(httpMethod));
    }

    private boolean requireBody(HttpMethod httpMethod) {
        return HttpMethod.POST.equals(httpMethod) || HttpMethod.PUT.equals(httpMethod) || HttpMethod.PATCH.equals(httpMethod);
    }

    /**
     *
     * @param httpMethod
     * @param doOutput
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    HttpURLConnection openConnection(HttpMethod httpMethod, boolean doOutput) throws UncheckedIOException {
        return openConnection(httpMethod, doOutput, _settings);
    }

    /**
     *
     * @param httpMethod
     * @param settings
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    HttpURLConnection openConnection(HttpMethod httpMethod, HttpSettings settings) throws UncheckedIOException {
        return openConnection(httpMethod, requireBody(httpMethod), settings);
    }

    /**
     *
     * @param httpMethod
     * @param doOutput
     * @param settings
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    HttpURLConnection openConnection(HttpMethod httpMethod, boolean doOutput, HttpSettings settings) throws UncheckedIOException {
        return openConnection(httpMethod, null, doOutput, settings);
    }

    /**
     *
     * @param httpMethod
     * @param queryParameters
     * @param doOutput
     * @param settings
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    HttpURLConnection openConnection(HttpMethod httpMethod, final Object queryParameters, boolean doOutput, HttpSettings settings) throws UncheckedIOException {
        HttpURLConnection connection = null;

        if (_activeConnectionCounter.incrementAndGet() > _maxConnection) {
            _activeConnectionCounter.decrementAndGet();
            throw new RuntimeException("Can not get connection, exceeded max connection number: " + _maxConnection);
        }

        try {
            synchronized (_netURL) {
                if (queryParameters != null && (httpMethod.equals(HttpMethod.GET) || httpMethod.equals(HttpMethod.DELETE))) {
                    connection = (HttpURLConnection) new URL(URLEncodedUtil.encode(_url, queryParameters)).openConnection();
                } else {
                    connection = (HttpURLConnection) _netURL.openConnection();
                }
            }

            if (connection instanceof HttpsURLConnection) {
                SSLSocketFactory ssf = (settings == null ? _settings : settings).getSSLSocketFactory();

                if (ssf != null) {
                    ((HttpsURLConnection) connection).setSSLSocketFactory(ssf);
                }
            }

            int connectionTimeoutInMillis = _connectionTimeoutInMillis > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) _connectionTimeoutInMillis;

            if (settings != null) {
                connectionTimeoutInMillis = settings.getConnectionTimeout();
            }

            if (connectionTimeoutInMillis > 0) {
                connection.setConnectTimeout(connectionTimeoutInMillis);
            }

            int readTimeoutInMillis = _readTimeoutInMillis > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) _readTimeoutInMillis;

            if (settings != null) {
                readTimeoutInMillis = settings.getReadTimeout();
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

            if (isOneWayRequest(settings)) {
                connection.setDoInput(false);
            }

            connection.setDoOutput(doOutput);
            connection.setRequestMethod(httpMethod.name());

            return connection;
        } catch (IOException e) {
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
    void setHttpProperties(HttpURLConnection connection, HttpSettings settings) throws UncheckedIOException {
        final HttpHeaders headers = settings.headers();

        if (headers != null) {
            Object headerValue = null;

            for (String headerName : headers.headerNameSet()) {
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
    void close(OutputStream os, InputStream is, @SuppressWarnings("unused") HttpURLConnection connection) {
        try {
            IOUtil.closeQuietly(os);
            IOUtil.closeQuietly(is);
        } finally {
            _activeConnectionCounter.decrementAndGet();
        }

        // connection.disconnect();
    }

    /**
     *
     * @param httpMethod
     * @param request
     * @return
     */
    public ContinuableFuture<String> asyncExecute(final HttpMethod httpMethod, final Object request) {
        return asyncExecute(String.class, httpMethod, request);
    }

    /**
     *
     * @param httpMethod
     * @param request
     * @param settings
     * @return
     */
    public ContinuableFuture<String> asyncExecute(final HttpMethod httpMethod, final Object request, final HttpSettings settings) {
        return asyncExecute(String.class, httpMethod, request, settings);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @param httpMethod
     * @param request
     * @return
     */
    public <T> ContinuableFuture<T> asyncExecute(final Class<T> resultClass, final HttpMethod httpMethod, final Object request) {
        return asyncExecute(resultClass, httpMethod, request, _settings);
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @param httpMethod
     * @param request
     * @param settings
     * @return
     */
    public <T> ContinuableFuture<T> asyncExecute(final Class<T> resultClass, final HttpMethod httpMethod, final Object request, final HttpSettings settings) {
        final Callable<T> cmd = () -> execute(resultClass, httpMethod, request, settings);

        return _asyncExecutor.execute(cmd);
    }

    /**
     *
     * @param output
     * @param httpMethod
     * @param request
     * @param settings
     * @return
     */
    public ContinuableFuture<Void> asyncExecute(final File output, final HttpMethod httpMethod, final Object request, final HttpSettings settings) {
        final Callable<Void> cmd = () -> {
            execute(output, httpMethod, request, settings);

            return null;
        };

        return _asyncExecutor.execute(cmd);
    }

    /**
     *
     * @param output
     * @param httpMethod
     * @param request
     * @param settings
     * @return
     */
    public ContinuableFuture<Void> asyncExecute(final OutputStream output, final HttpMethod httpMethod, final Object request, final HttpSettings settings) {
        final Callable<Void> cmd = () -> {
            execute(output, httpMethod, request, settings);

            return null;
        };

        return _asyncExecutor.execute(cmd);
    }

    /**
     *
     * @param output
     * @param httpMethod
     * @param request
     * @param settings
     * @return
     */
    public ContinuableFuture<Void> asyncExecute(final Writer output, final HttpMethod httpMethod, final Object request, final HttpSettings settings) {
        final Callable<Void> cmd = () -> {
            execute(output, httpMethod, request, settings);

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
