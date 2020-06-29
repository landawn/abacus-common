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

import java.io.Closeable;
import java.io.File;
import java.io.OutputStream;
import java.io.Writer;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.util.AsyncExecutor;
import com.landawn.abacus.util.ContinuableFuture;
import com.landawn.abacus.util.N;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public abstract class AbstractHttpClient implements Closeable {

    // ...
    public static final int DEFAULT_MAX_CONNECTION = 16;

    /** Unit is milliseconds. */
    public static final int DEFAULT_CONNECTION_TIMEOUT = 8000;

    public static final int DEFAULT_READ_TIMEOUT = 16000;

    // ...
    protected final String _url;

    protected final int _maxConnection;

    protected final long _connectionTimeout;

    protected final long _readTimeout;

    protected final HttpSettings _settings;

    protected final AsyncExecutor _asyncExecutor;

    protected AbstractHttpClient(String url) {
        this(url, DEFAULT_MAX_CONNECTION, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    protected AbstractHttpClient(String url, int maxConnection) {
        this(url, maxConnection, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    protected AbstractHttpClient(String url, int maxConnection, long connectionTimeout, long readTimeout) {
        this(url, maxConnection, connectionTimeout, readTimeout, null);
    }

    protected AbstractHttpClient(String url, int maxConnection, long connectionTimeout, long readTimeout, HttpSettings settings) throws UncheckedIOException {
        if (N.isNullOrEmpty(url)) {
            throw new IllegalArgumentException("url can't be null or empty");
        }

        if ((maxConnection < 0) || (connectionTimeout < 0) || (readTimeout < 0)) {
            throw new IllegalArgumentException(
                    "maxConnection, connectionTimeout or readTimeout can't be less than 0: " + maxConnection + ", " + connectionTimeout + ", " + readTimeout);
        }

        this._url = url;
        this._maxConnection = (maxConnection == 0) ? DEFAULT_MAX_CONNECTION : maxConnection;
        this._connectionTimeout = (connectionTimeout == 0) ? DEFAULT_CONNECTION_TIMEOUT : connectionTimeout;
        this._readTimeout = (readTimeout == 0) ? DEFAULT_READ_TIMEOUT : readTimeout;
        this._settings = settings == null ? HttpSettings.create() : settings;

        _asyncExecutor = new AsyncExecutor(Math.min(8, this._maxConnection), this._maxConnection, 300L, TimeUnit.SECONDS);
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
     * Write the specified <code>request</code> to request body.
     *
     * @param <T>
     * @param resultClass
     * @param httpMethod
     * @param request can be String/Map/Entity/InputStream/Reader...
     * @param settings
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public abstract <T> T execute(final Class<T> resultClass, final HttpMethod httpMethod, final Object request, final HttpSettings settings)
            throws UncheckedIOException;

    /**
     *
     * @param output write the InputStream in the response to this specified File.
     * @param httpMethod
     * @param request
     * @param settings
     * @throws UncheckedIOException the unchecked IO exception
     */
    public abstract void execute(final File output, final HttpMethod httpMethod, final Object request, final HttpSettings settings) throws UncheckedIOException;

    /**
     *
     * @param output write the InputStream in the response to this specified OutputStream.
     * @param httpMethod
     * @param request
     * @param settings
     * @throws UncheckedIOException the unchecked IO exception
     */
    public abstract void execute(final OutputStream output, final HttpMethod httpMethod, final Object request, final HttpSettings settings)
            throws UncheckedIOException;

    /**
     *
     * @param output write the InputStream in the response to this specified Writer.
     * @param httpMethod
     * @param request
     * @param settings
     * @throws UncheckedIOException the unchecked IO exception
     */
    public abstract void execute(final Writer output, final HttpMethod httpMethod, final Object request, final HttpSettings settings)
            throws UncheckedIOException;

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
        final Callable<T> cmd = new Callable<T>() {
            @Override
            public T call() throws Exception {
                return execute(resultClass, httpMethod, request, settings);
            }
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
    public ContinuableFuture<Void> asyncExecute(final File output, final HttpMethod httpMethod, final Object request, final HttpSettings settings) {
        final Callable<Void> cmd = new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                execute(output, httpMethod, request, settings);

                return null;
            }
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
        final Callable<Void> cmd = new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                execute(output, httpMethod, request, settings);

                return null;
            }
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
        final Callable<Void> cmd = new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                execute(output, httpMethod, request, settings);

                return null;
            }
        };

        return _asyncExecutor.execute(cmd);
    }

    /**
     * Close.
     */
    @Override
    public void close() {
        // do nothing.
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
}
