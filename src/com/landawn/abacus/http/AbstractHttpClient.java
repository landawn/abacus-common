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
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public abstract class AbstractHttpClient implements Closeable {
    // ...
    public static final int DEFAULT_MAX_CONNECTION = 16;
    /**
     * Unit is milliseconds
     */
    public static final int DEFAULT_CONNECTION_TIMEOUT = 8000;
    public static final int DEFAULT_READ_TIMEOUT = 16000;

    // for static asynchronization operation.
    protected static final AsyncExecutor asyncExecutor = new AsyncExecutor(256, 300L, TimeUnit.SECONDS);

    // ...
    protected final String _url;
    protected final int _maxConnection;
    protected final long _connTimeout;
    protected final long _readTimeout;
    protected final HttpSettings _settings;

    protected final AsyncExecutor _asyncExecutor;

    protected AbstractHttpClient(String url) {
        this(url, DEFAULT_MAX_CONNECTION, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    protected AbstractHttpClient(String url, int maxConnection) {
        this(url, maxConnection, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    protected AbstractHttpClient(String url, int maxConnection, long connTimeout, long readTimeout) {
        this(url, maxConnection, connTimeout, readTimeout, null);
    }

    protected AbstractHttpClient(String url, int maxConnection, long connTimeout, long readTimeout, HttpSettings settings) throws UncheckedIOException {
        if (N.isNullOrEmpty(url)) {
            throw new IllegalArgumentException("url can't be null or empty");
        }

        if ((maxConnection < 0) || (connTimeout < 0) || (readTimeout < 0)) {
            throw new IllegalArgumentException(
                    "maxConnection, connTimeout or readTimeout can't be less than 0: " + maxConnection + ", " + connTimeout + ", " + readTimeout);
        }

        this._url = url;
        this._maxConnection = (maxConnection == 0) ? DEFAULT_MAX_CONNECTION : maxConnection;
        this._connTimeout = (connTimeout == 0) ? DEFAULT_CONNECTION_TIMEOUT : connTimeout;
        this._readTimeout = (readTimeout == 0) ? DEFAULT_READ_TIMEOUT : readTimeout;
        this._settings = settings == null ? HttpSettings.create() : settings;

        _asyncExecutor = new AsyncExecutor(this._maxConnection, 300L, TimeUnit.SECONDS);
    }

    public String url() {
        return _url;
    }

    public String get() throws UncheckedIOException {
        return get(String.class);
    }

    public String get(final HttpSettings settings) throws UncheckedIOException {
        return get(String.class, settings);
    }

    public String get(final Object queryParameters) throws UncheckedIOException {
        return get(String.class, queryParameters);
    }

    public String get(final Object queryParameters, final HttpSettings settings) throws UncheckedIOException {
        return get(String.class, queryParameters, settings);
    }

    public <T> T get(final Class<T> resultClass) throws UncheckedIOException {
        return get(resultClass, null, _settings);
    }

    public <T> T get(final Class<T> resultClass, final HttpSettings settings) throws UncheckedIOException {
        return get(resultClass, null, settings);
    }

    public <T> T get(final Class<T> resultClass, final Object queryParameters) throws UncheckedIOException {
        return get(resultClass, queryParameters, _settings);
    }

    public <T> T get(final Class<T> resultClass, final Object queryParameters, final HttpSettings settings) throws UncheckedIOException {
        return execute(resultClass, HttpMethod.GET, queryParameters, settings);
    }

    public ContinuableFuture<String> asyncGet() {
        return asyncGet(String.class);
    }

    public ContinuableFuture<String> asyncGet(final HttpSettings settings) {
        return asyncGet(String.class, settings);
    }

    public ContinuableFuture<String> asyncGet(final Object queryParameters) {
        return asyncGet(String.class, queryParameters);
    }

    public ContinuableFuture<String> asyncGet(final Object queryParameters, final HttpSettings settings) {
        return asyncGet(String.class, queryParameters, settings);
    }

    public <T> ContinuableFuture<T> asyncGet(final Class<T> resultClass) {
        return asyncGet(resultClass, null, _settings);
    }

    public <T> ContinuableFuture<T> asyncGet(final Class<T> resultClass, final HttpSettings settings) {
        return asyncGet(resultClass, null, settings);
    }

    public <T> ContinuableFuture<T> asyncGet(final Class<T> resultClass, final Object queryParameters) {
        return asyncGet(resultClass, queryParameters, _settings);
    }

    public <T> ContinuableFuture<T> asyncGet(final Class<T> resultClass, final Object queryParameters, final HttpSettings settings) {
        return asyncExecute(resultClass, HttpMethod.GET, queryParameters, settings);
    }

    public String delete() throws UncheckedIOException {
        return delete(String.class);
    }

    public String delete(final HttpSettings settings) throws UncheckedIOException {
        return delete(String.class, settings);
    }

    public String delete(final Object queryParameters) throws UncheckedIOException {
        return delete(String.class, queryParameters);
    }

    public String delete(final Object queryParameters, final HttpSettings settings) throws UncheckedIOException {
        return delete(String.class, queryParameters, settings);
    }

    public <T> T delete(final Class<T> resultClass) throws UncheckedIOException {
        return delete(resultClass, null, _settings);
    }

    public <T> T delete(final Class<T> resultClass, final HttpSettings settings) throws UncheckedIOException {
        return delete(resultClass, null, settings);
    }

    public <T> T delete(final Class<T> resultClass, final Object queryParameters) throws UncheckedIOException {
        return delete(resultClass, queryParameters, _settings);
    }

    public <T> T delete(final Class<T> resultClass, final Object queryParameters, final HttpSettings settings) throws UncheckedIOException {
        return execute(resultClass, HttpMethod.DELETE, queryParameters, settings);
    }

    public ContinuableFuture<String> asyncDelete() {
        return asyncDelete(String.class);
    }

    public ContinuableFuture<String> asyncDelete(final HttpSettings settings) {
        return asyncDelete(String.class, settings);
    }

    public ContinuableFuture<String> asyncDelete(final Object queryParameters) {
        return asyncDelete(String.class, queryParameters);
    }

    public ContinuableFuture<String> asyncDelete(final Object queryParameters, final HttpSettings settings) {
        return asyncDelete(String.class, queryParameters, settings);
    }

    public <T> ContinuableFuture<T> asyncDelete(final Class<T> resultClass) {
        return asyncDelete(resultClass, null, _settings);
    }

    public <T> ContinuableFuture<T> asyncDelete(final Class<T> resultClass, final HttpSettings settings) {
        return asyncDelete(resultClass, null, settings);
    }

    public <T> ContinuableFuture<T> asyncDelete(final Class<T> resultClass, final Object queryParameters) {
        return asyncDelete(resultClass, queryParameters, _settings);
    }

    public <T> ContinuableFuture<T> asyncDelete(final Class<T> resultClass, final Object queryParameters, final HttpSettings settings) {
        return asyncExecute(resultClass, HttpMethod.DELETE, queryParameters, settings);
    }

    public String post(final Object request) throws UncheckedIOException {
        return post(String.class, request);
    }

    public String post(final Object request, final HttpSettings settings) throws UncheckedIOException {
        return post(String.class, request, settings);
    }

    public <T> T post(final Class<T> resultClass, final Object request) throws UncheckedIOException {
        return post(resultClass, request, _settings);
    }

    public <T> T post(final Class<T> resultClass, final Object request, final HttpSettings settings) throws UncheckedIOException {
        return execute(resultClass, HttpMethod.POST, request, settings);
    }

    public ContinuableFuture<String> asyncPost(final Object request) {
        return asyncPost(String.class, request);
    }

    public ContinuableFuture<String> asyncPost(final Object request, final HttpSettings settings) {
        return asyncPost(String.class, request, settings);
    }

    public <T> ContinuableFuture<T> asyncPost(final Class<T> resultClass, final Object request) {
        return asyncPost(resultClass, request, _settings);
    }

    public <T> ContinuableFuture<T> asyncPost(final Class<T> resultClass, final Object request, final HttpSettings settings) {
        return asyncExecute(resultClass, HttpMethod.POST, request, settings);
    }

    public String put(final Object request) throws UncheckedIOException {
        return put(String.class, request);
    }

    public String put(final Object request, final HttpSettings settings) throws UncheckedIOException {
        return put(String.class, request, settings);
    }

    public <T> T put(final Class<T> resultClass, final Object request) throws UncheckedIOException {
        return put(resultClass, request, _settings);
    }

    public <T> T put(final Class<T> resultClass, final Object request, final HttpSettings settings) throws UncheckedIOException {
        return execute(resultClass, HttpMethod.PUT, request, settings);
    }

    public ContinuableFuture<String> asyncPut(final Object request) {
        return asyncPut(String.class, request);
    }

    public ContinuableFuture<String> asyncPut(final Object request, final HttpSettings settings) {
        return asyncPut(String.class, request, settings);
    }

    public <T> ContinuableFuture<T> asyncPut(final Class<T> resultClass, final Object request) {
        return asyncPut(resultClass, request, _settings);
    }

    public <T> ContinuableFuture<T> asyncPut(final Class<T> resultClass, final Object request, final HttpSettings settings) {
        return asyncExecute(resultClass, HttpMethod.PUT, request, settings);
    }

    public String execute(final HttpMethod httpMethod, final Object request) throws UncheckedIOException {
        return execute(String.class, httpMethod, request);
    }

    public String execute(final HttpMethod httpMethod, final Object request, final HttpSettings settings) throws UncheckedIOException {
        return execute(String.class, httpMethod, request, settings);
    }

    public <T> T execute(final Class<T> resultClass, final HttpMethod httpMethod, final Object request) throws UncheckedIOException {
        return execute(resultClass, httpMethod, request, _settings);
    }

    /**
     * Write the specified <code>request</code> to request body.
     * 
     * @param resultClass
     * @param methodName
     * @param request can be String/Map/Entity/InputStream/Reader...
     * @param settings
     * @return
     */
    public abstract <T> T execute(final Class<T> resultClass, final HttpMethod httpMethod, final Object request, final HttpSettings settings)
            throws UncheckedIOException;

    /**
     * 
     * @param output write the InputStream in the response to this specified File.
     * @param httpMethod
     * @param request
     * @param settings
     */
    public abstract void execute(final File output, final HttpMethod httpMethod, final Object request, final HttpSettings settings) throws UncheckedIOException;

    /**
     * 
     * @param output write the InputStream in the response to this specified OutputStream.
     * @param httpMethod
     * @param request
     * @param settings
     */
    public abstract void execute(final OutputStream output, final HttpMethod httpMethod, final Object request, final HttpSettings settings)
            throws UncheckedIOException;

    /**
     * 
     * @param output write the InputStream in the response to this specified Writer.
     * @param httpMethod
     * @param request
     * @param settings
     */
    public abstract void execute(final Writer output, final HttpMethod httpMethod, final Object request, final HttpSettings settings)
            throws UncheckedIOException;

    public ContinuableFuture<String> asyncExecute(final HttpMethod httpMethod, final Object request) {
        return asyncExecute(String.class, httpMethod, request);
    }

    public ContinuableFuture<String> asyncExecute(final HttpMethod httpMethod, final Object request, final HttpSettings settings) {
        return asyncExecute(String.class, httpMethod, request, settings);
    }

    public <T> ContinuableFuture<T> asyncExecute(final Class<T> resultClass, final HttpMethod httpMethod, final Object request) {
        return asyncExecute(resultClass, httpMethod, request, _settings);
    }

    public <T> ContinuableFuture<T> asyncExecute(final Class<T> resultClass, final HttpMethod httpMethod, final Object request, final HttpSettings settings) {
        final Callable<T> cmd = new Callable<T>() {
            @Override
            public T call() throws Exception {
                return execute(resultClass, httpMethod, request, settings);
            }
        };

        return _asyncExecutor.execute(cmd);
    }

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

    @Override
    public void close() {
        // do nothing.
    }

    protected boolean isOneWayRequest(HttpSettings settings) {
        return _settings.isOneWayRequest() || ((settings != null) && settings.isOneWayRequest());
    }

    protected ContentFormat getContentFormat(HttpSettings settings) {
        ContentFormat contentFormat = null;

        if (settings != null) {
            contentFormat = settings.getContentFormat();

            if (contentFormat == null) {
                String contentType = (String) settings.headers().get(HttpHeaders.Names.CONTENT_TYPE);
                String contentEncoding = (String) settings.headers().get(HttpHeaders.Names.CONTENT_ENCODING);

                contentFormat = HTTP.getContentFormat(contentType, contentEncoding);
            }
        }

        if (contentFormat == null) {
            contentFormat = _settings.getContentFormat();

            if (contentFormat == null) {
                String contentType = (String) _settings.headers().get(HttpHeaders.Names.CONTENT_TYPE);
                String contentEncoding = (String) _settings.headers().get(HttpHeaders.Names.CONTENT_ENCODING);

                contentFormat = HTTP.getContentFormat(contentType, contentEncoding);
            }
        }

        return contentFormat;
    }

    protected String getContentType(HttpSettings settings) {
        String contentType = null;

        if (settings != null) {
            if (settings.getContentFormat() != null) {
                contentType = HTTP.getContentType(settings.getContentFormat());
            } else {
                contentType = (String) settings.headers().get(HttpHeaders.Names.CONTENT_TYPE);
            }
        }

        if (N.isNullOrEmpty(contentType)) {
            if (_settings.getContentFormat() != null) {
                contentType = HTTP.getContentType(_settings.getContentFormat());
            } else {
                contentType = (String) _settings.headers().get(HttpHeaders.Names.CONTENT_TYPE);
            }
        }

        return contentType;
    }

    protected String getContentEncoding(HttpSettings settings) {
        String contentEncoding = null;

        if (settings != null) {
            if (settings.getContentFormat() != null) {
                contentEncoding = HTTP.getContentEncoding(settings.getContentFormat());
            } else {
                contentEncoding = (String) settings.headers().get(HttpHeaders.Names.CONTENT_ENCODING);
            }
        }

        if (N.isNullOrEmpty(contentEncoding)) {
            if (_settings.getContentFormat() != null) {
                contentEncoding = HTTP.getContentEncoding(_settings.getContentFormat());
            } else {
                contentEncoding = (String) _settings.headers().get(HttpHeaders.Names.CONTENT_ENCODING);
            }
        }

        return contentEncoding;
    }
}
