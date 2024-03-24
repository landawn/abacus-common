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
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
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
import com.landawn.abacus.util.BufferedReader;
import com.landawn.abacus.util.BufferedWriter;
import com.landawn.abacus.util.ContinuableFuture;
import com.landawn.abacus.util.ExceptionUtil;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Strings;
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
    protected final String _url; //NOSONAR

    protected final int _maxConnection; //NOSONAR

    protected final long _connectionTimeoutInMillis; //NOSONAR

    protected final long _readTimeoutInMillis; //NOSONAR

    protected final HttpSettings _settings; //NOSONAR

    protected final AsyncExecutor _asyncExecutor; //NOSONAR

    protected final URL _netURL; //NOSONAR

    protected final AtomicInteger _activeConnectionCounter; //NOSONAR

    protected HttpClient(final String url, int maxConnection, long connectionTimeoutInMillis, long readTimeoutInMillis, HttpSettings settings,
            final AtomicInteger sharedActiveConnectionCounter, final Executor executor) {
        this(null, url, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, settings, sharedActiveConnectionCounter, executor);
    }

    protected HttpClient(final URL netUrl, final String url, int maxConnection, long connectionTimeoutInMillis, long readTimeoutInMillis, HttpSettings settings,
            final AtomicInteger sharedActiveConnectionCounter, final Executor executor) {
        N.checkArgument(netUrl != null || Strings.isNotEmpty(url), "url can not be null or empty");

        if ((maxConnection < 0) || (connectionTimeoutInMillis < 0) || (readTimeoutInMillis < 0)) {
            throw new IllegalArgumentException("maxConnection, connectionTimeoutInMillis or readTimeoutInMillis can't be less than 0: " + maxConnection + ", "
                    + connectionTimeoutInMillis + ", " + readTimeoutInMillis);
        }

        this._netURL = netUrl == null ? createNetUrl(url) : netUrl;
        this._url = Strings.isEmpty(url) ? netUrl.toString() : url;
        this._maxConnection = (maxConnection == 0) ? DEFAULT_MAX_CONNECTION : maxConnection;
        this._connectionTimeoutInMillis = (connectionTimeoutInMillis == 0) ? DEFAULT_CONNECTION_TIMEOUT : connectionTimeoutInMillis;
        this._readTimeoutInMillis = (readTimeoutInMillis == 0) ? DEFAULT_READ_TIMEOUT : readTimeoutInMillis;
        this._settings = settings == null ? HttpSettings.create() : settings;

        _asyncExecutor = executor == null ? HttpUtil.DEFAULT_ASYNC_EXECUTOR : new AsyncExecutor(executor);

        this._activeConnectionCounter = sharedActiveConnectionCounter;
    }

    private static URL createNetUrl(final String url) {
        try {
            return URI.create(N.checkArgNotNull(url, "url")).toURL();
        } catch (MalformedURLException e) {
            throw ExceptionUtil.toRuntimeException(e);
        }
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

    /**
     *
     * @param url
     * @return
     */
    public static HttpClient create(URL url) {
        return create(url, DEFAULT_MAX_CONNECTION);
    }

    /**
     *
     * @param url
     * @param maxConnection
     * @return
     */
    public static HttpClient create(URL url, int maxConnection) {
        return create(url, maxConnection, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    /**
     *
     * @param url
     * @param connectionTimeoutInMillis
     * @param readTimeoutInMillis
     * @return
     */
    public static HttpClient create(URL url, long connectionTimeoutInMillis, long readTimeoutInMillis) {
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
    public static HttpClient create(URL url, int maxConnection, long connectionTimeoutInMillis, long readTimeoutInMillis) {
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
    public static HttpClient create(URL url, int maxConnection, long connectionTimeoutInMillis, long readTimeoutInMillis, HttpSettings settings)
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
    public static HttpClient create(URL url, int maxConnection, long connectionTimeoutInMillis, long readTimeoutInMillis, HttpSettings settings,
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
    public static HttpClient create(URL url, int maxConnection, long connectionTimeoutInMillis, long readTimeoutInMillis, final Executor executor) {
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
    public static HttpClient create(URL url, int maxConnection, long connectionTimeoutInMillis, long readTimeoutInMillis, HttpSettings settings,
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
    public static HttpClient create(URL url, int maxConnection, long connectionTimeoutInMillis, long readTimeoutInMillis, HttpSettings settings,
            final AtomicInteger sharedActiveConnectionCounter, final Executor executor) {
        return new HttpClient(url, null, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, settings, sharedActiveConnectionCounter, executor);
    }

    /**
     *
     *
     * @return
     */
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
        return execute(HttpMethod.GET, resultClass, queryParameters, settings);
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
        return execute(HttpMethod.DELETE, resultClass, queryParameters, settings);
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
        return execute(HttpMethod.POST, resultClass, request, settings);
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
        return execute(HttpMethod.PUT, resultClass, request, settings);
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
        return execute(httpMethod, String.class, request);
    }

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
        execute(HttpMethod.HEAD, Void.class, null, settings);
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
        return execute(httpMethod, String.class, request, settings);
    }

    /**
     * 
     *
     * @param <T> 
     * @param httpMethod 
     * @param resultClass 
     * @param request 
     * @return 
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T execute(final HttpMethod httpMethod, final Class<T> resultClass, final Object request) throws UncheckedIOException {
        return execute(httpMethod, resultClass, request, _settings);
    }

    /**
     * 
     *
     * @param <T> 
     * @param httpMethod 
     * @param resultClass 
     * @param request 
     * @param settings 
     * @return 
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T execute(final HttpMethod httpMethod, final Class<T> resultClass, final Object request, final HttpSettings settings)
            throws UncheckedIOException {
        return execute(httpMethod, resultClass, null, null, request, settings);
    }

    /**
     *
     * @param httpMethod
     * @param output
     * @param request
     * @param settings
     * @throws UncheckedIOException the unchecked IO exception
     */
    public void execute(final HttpMethod httpMethod, final File output, final Object request, final HttpSettings settings) throws UncheckedIOException {
        OutputStream os = null;

        try {
            os = IOUtil.newFileOutputStream(output);
            execute(httpMethod, os, request, settings);
        } finally {
            IOUtil.close(os);
        }
    }

    /**
     *
     * @param httpMethod
     * @param output
     * @param request
     * @param settings
     * @throws UncheckedIOException the unchecked IO exception
     */
    public void execute(final HttpMethod httpMethod, final OutputStream output, final Object request, final HttpSettings settings) throws UncheckedIOException {
        execute(httpMethod, null, output, null, request, settings);
    }

    /**
     *
     * @param httpMethod
     * @param output
     * @param request
     * @param settings
     * @throws UncheckedIOException the unchecked IO exception
     */
    public void execute(final HttpMethod httpMethod, final Writer output, final Object request, final HttpSettings settings) throws UncheckedIOException {
        execute(httpMethod, null, null, output, request, settings);
    }

    /**
     *
     * @param httpMethod
     * @param resultClass
     * @param outputStream
     * @param outputWriter
     * @param request
     * @param settings
     * @param <T>
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    private <T> T execute(final HttpMethod httpMethod, final Class<T> resultClass, final OutputStream outputStream, final Writer outputWriter,
            final Object request, final HttpSettings settings) throws UncheckedIOException {
        final Charset requestCharset = HttpUtil.getRequestCharset(settings == null || settings.headers().isEmpty() ? _settings.headers() : settings.headers());
        final ContentFormat requestContentFormat = getContentFormat(settings);
        final boolean doOutput = request != null && !(httpMethod == HttpMethod.GET || httpMethod == HttpMethod.DELETE);

        final HttpURLConnection connection = openConnection(httpMethod, resultClass, request, doOutput, settings);
        final long sentRequestAtMillis = System.currentTimeMillis();
        InputStream is = null;
        OutputStream os = null;

        try { //NOSONAR
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
                    final BufferedWriter bw = Objectory.createBufferedWriter(IOUtil.newOutputStreamWriter(os, requestCharset));

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
                            final BufferedWriter bw = Objectory.createBufferedWriter(IOUtil.newOutputStreamWriter(os, requestCharset));

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

            final int statusCode = connection.getResponseCode();
            final Map<String, List<String>> respHeaders = connection.getHeaderFields();
            final Charset respCharset = HttpUtil.getResponseCharset(respHeaders, requestCharset);
            final ContentFormat respContentFormat = HttpUtil.getResponseContentFormat(respHeaders, requestContentFormat);

            is = HttpUtil.getInputStream(connection, respContentFormat);

            if (!HttpUtil.isSuccessfulResponseCode(statusCode) && (resultClass == null || !resultClass.equals(HttpResponse.class))) {
                throw new UncheckedIOException(
                        new IOException(statusCode + ": " + connection.getResponseMessage() + ". " + IOUtil.readAllToString(is, respCharset)));
            }

            if (isOneWayRequest(httpMethod, resultClass, settings)) {
                return null;
            } else {
                if (outputStream != null) {
                    IOUtil.write(outputStream, is, true);

                    return null;
                } else if (outputWriter != null) {
                    final BufferedReader br = Objectory.createBufferedReader(IOUtil.newInputStreamReader(is, respCharset));

                    try {
                        IOUtil.write(outputWriter, br, true);
                    } finally {
                        Objectory.recycle(br);
                    }

                    return null;
                } else {
                    if (resultClass != null && resultClass.equals(HttpResponse.class)) {
                        return (T) new HttpResponse(_url, sentRequestAtMillis, System.currentTimeMillis(), statusCode, connection.getResponseMessage(),
                                respHeaders, IOUtil.readAllBytes(is), respContentFormat, respCharset);
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
                                final BufferedReader br = Objectory.createBufferedReader(IOUtil.newInputStreamReader(is, respCharset));

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
     * @param httpMethod
     * @param resultClass
     * @param settings
     * @return true, if is one way request
     */
    protected boolean isOneWayRequest(final HttpMethod httpMethod, final Class<?> resultClass, final HttpSettings settings) {
        return HttpMethod.HEAD == httpMethod || resultClass == null || Void.class.equals(resultClass)
                || (settings == null ? _settings.isOneWayRequest() : settings.isOneWayRequest());
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
    protected String getContentEncoding(HttpSettings settings) {
        String contentEncoding = null;

        if (settings != null) {
            contentEncoding = settings.getContentEncoding();
        }

        if (Strings.isEmpty(contentEncoding)) {
            contentEncoding = _settings.getContentEncoding();
        }

        return contentEncoding;
    }

    private boolean requireBody(HttpMethod httpMethod) {
        return httpMethod == HttpMethod.POST || httpMethod == HttpMethod.PUT || httpMethod == HttpMethod.PATCH;
    }

    /**
     *
     * @param httpMethod
     * @param resultClass
     * @param doOutput
     * @param settings
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public HttpURLConnection openConnection(HttpMethod httpMethod, final Class<?> resultClass, boolean doOutput, HttpSettings settings)
            throws UncheckedIOException {
        return openConnection(httpMethod, resultClass, null, doOutput, settings);
    }

    /**
     *
     * @param httpMethod
     * @param resultClass
     * @param queryParameters
     * @param doOutput
     * @param settings
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public HttpURLConnection openConnection(HttpMethod httpMethod, final Class<?> resultClass, final Object queryParameters, boolean doOutput,
            HttpSettings settings) throws UncheckedIOException {
        HttpURLConnection connection = null;

        if (_activeConnectionCounter.incrementAndGet() > _maxConnection) {
            _activeConnectionCounter.decrementAndGet();
            throw new RuntimeException("Can not get connection, exceeded max connection number: " + _maxConnection);
        }

        try {
            synchronized (_netURL) {
                if (queryParameters != null && (httpMethod == HttpMethod.GET || httpMethod == HttpMethod.DELETE)) {
                    connection = (HttpURLConnection) URI.create(URLEncodedUtil.encode(_url, queryParameters)).toURL().openConnection();
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

            if (isOneWayRequest(httpMethod, resultClass, settings)) {
                connection.setDoInput(false);
            } else {
                connection.setDoOutput(doOutput);
            }

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
    void close(OutputStream os, InputStream is, @SuppressWarnings("unused") HttpURLConnection connection) { //NOSONAR
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
     *
     * @return
     */
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
        return asyncExecute(HttpMethod.GET, resultClass, queryParameters, settings);
    }

    /**
     *
     *
     * @return
     */
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
        return asyncExecute(HttpMethod.DELETE, resultClass, queryParameters, settings);
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
        return asyncExecute(HttpMethod.POST, resultClass, request, settings);
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
        return asyncExecute(HttpMethod.PUT, resultClass, request, settings);
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
     *
     * @return
     */
    public ContinuableFuture<Void> asyncHead() {
        return asyncHead(_settings);
    }

    /**
     *
     * @param settings
     * @return
     */
    public ContinuableFuture<Void> asyncHead(final HttpSettings settings) {
        return asyncExecute(HttpMethod.HEAD, Void.class, null, settings);
    }

    /**
     *
     * @param httpMethod
     * @param request
     * @return
     */
    public ContinuableFuture<String> asyncExecute(final HttpMethod httpMethod, final Object request) {
        return asyncExecute(httpMethod, String.class, request);
    }

    /**
     *
     * @param httpMethod
     * @param request
     * @param settings
     * @return
     */
    public ContinuableFuture<String> asyncExecute(final HttpMethod httpMethod, final Object request, final HttpSettings settings) {
        return asyncExecute(httpMethod, String.class, request, settings);
    }

    /**
     * 
     *
     * @param <T> 
     * @param httpMethod 
     * @param resultClass 
     * @param request 
     * @return 
     */
    public <T> ContinuableFuture<T> asyncExecute(final HttpMethod httpMethod, final Class<T> resultClass, final Object request) {
        return asyncExecute(httpMethod, resultClass, request, _settings);
    }

    /**
     * 
     *
     * @param <T> 
     * @param httpMethod 
     * @param resultClass 
     * @param request 
     * @param settings 
     * @return 
     */
    public <T> ContinuableFuture<T> asyncExecute(final HttpMethod httpMethod, final Class<T> resultClass, final Object request, final HttpSettings settings) {
        final Callable<T> cmd = () -> execute(httpMethod, resultClass, request, settings);

        return _asyncExecutor.execute(cmd);
    }

    /**
     *
     * @param httpMethod
     * @param output
     * @param request
     * @param settings
     * @return
     */
    public ContinuableFuture<Void> asyncExecute(final HttpMethod httpMethod, final File output, final Object request, final HttpSettings settings) {
        final Callable<Void> cmd = () -> {
            execute(httpMethod, output, request, settings);

            return null;
        };

        return _asyncExecutor.execute(cmd);
    }

    /**
     *
     * @param httpMethod
     * @param output
     * @param request
     * @param settings
     * @return
     */
    public ContinuableFuture<Void> asyncExecute(final HttpMethod httpMethod, final OutputStream output, final Object request, final HttpSettings settings) {
        final Callable<Void> cmd = () -> {
            execute(httpMethod, output, request, settings);

            return null;
        };

        return _asyncExecutor.execute(cmd);
    }

    /**
     *
     * @param httpMethod
     * @param output
     * @param request
     * @param settings
     * @return
     */
    public ContinuableFuture<Void> asyncExecute(final HttpMethod httpMethod, final Writer output, final Object request, final HttpSettings settings) {
        final Callable<Void> cmd = () -> {
            execute(httpMethod, output, request, settings);

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
