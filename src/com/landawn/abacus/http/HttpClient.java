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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import com.landawn.abacus.exception.AbacusException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.BufferedReader;
import com.landawn.abacus.util.BufferedWriter;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.URLEncodedUtil;

/**
 * Any header can be set into the parameter <code>settings</code>
 * 
 * <br>HttpClient is thread safe.</br>
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public final class HttpClient extends AbstractHttpClient {
    static final Logger logger = LoggerFactory.getLogger(HttpClient.class);

    static {
        if (IOUtil.IS_PLATFORM_ANDROID) {
            // ignore
        } else {
            final int maxConnections = IOUtil.CPU_CORES * 8;

            System.setProperty("http.keepAlive", "true");
            System.setProperty("http.maxConnections", String.valueOf(maxConnections));
        }
    }

    protected final URL _netURL;

    protected final AtomicInteger _activeConnectionCounter;

    protected HttpClient(String url) {
        this(url, DEFAULT_MAX_CONNECTION);
    }

    protected HttpClient(String url, int maxConnection) {
        this(url, maxConnection, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    protected HttpClient(String url, int maxConnection, long connTimeout, long readTimeout) {
        this(url, maxConnection, connTimeout, readTimeout, null);
    }

    protected HttpClient(String url, int maxConnection, long connTimeout, long readTimeout, HttpSettings settings) throws UncheckedIOException {
        this(url, maxConnection, connTimeout, readTimeout, settings, new AtomicInteger(0));
    }

    protected HttpClient(String url, int maxConnection, long connTimeout, long readTimeout, HttpSettings settings,
            final AtomicInteger sharedActiveConnectionCounter) {
        super(url, maxConnection, connTimeout, readTimeout, settings);

        try {
            this._netURL = new URL(url);
        } catch (MalformedURLException e) {
            throw N.toRuntimeException(e);
        }

        this._activeConnectionCounter = sharedActiveConnectionCounter;
    }

    public static HttpClient create(String url) {
        return new HttpClient(url);
    }

    public static HttpClient create(String url, int maxConnection) {
        return new HttpClient(url, maxConnection);
    }

    public static HttpClient create(String url, long connTimeout, long readTimeout) {
        return new HttpClient(url, DEFAULT_MAX_CONNECTION, connTimeout, readTimeout);
    }

    public static HttpClient create(String url, int maxConnection, long connTimeout, long readTimeout) {
        return new HttpClient(url, maxConnection, connTimeout, readTimeout);
    }

    public static HttpClient create(String url, int maxConnection, long connTimeout, long readTimeout, HttpSettings settings) throws UncheckedIOException {
        return new HttpClient(url, maxConnection, connTimeout, readTimeout, settings);
    }

    public static HttpClient create(String url, int maxConnection, long connTimeout, long readTimeout, HttpSettings settings,
            final AtomicInteger sharedActiveConnectionCounter) {
        return new HttpClient(url, maxConnection, connTimeout, readTimeout, settings, sharedActiveConnectionCounter);
    }

    @Override
    public <T> T execute(final Class<T> resultClass, final HttpMethod httpMethod, final Object request, final HttpSettings settings)
            throws UncheckedIOException {
        return execute(resultClass, null, null, httpMethod, request, settings);
    }

    @Override
    public void execute(final File output, final HttpMethod httpMethod, final Object request, final HttpSettings settings) throws UncheckedIOException {
        OutputStream os = null;

        try {
            os = new FileOutputStream(output);
            execute(os, httpMethod, request, settings);
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(os);
        }
    }

    @Override
    public void execute(final OutputStream output, final HttpMethod httpMethod, final Object request, final HttpSettings settings) throws UncheckedIOException {
        execute(null, output, null, httpMethod, request, settings);
    }

    @Override
    public void execute(final Writer output, final HttpMethod httpMethod, final Object request, final HttpSettings settings) throws UncheckedIOException {
        execute(null, null, output, httpMethod, request, settings);
    }

    private <T> T execute(final Class<T> resultClass, final OutputStream outputStream, final Writer outputWriter, final HttpMethod httpMethod,
            final Object request, final HttpSettings settings) throws UncheckedIOException {
        final ContentFormat requestContentFormat = getContentFormat(settings);
        final HttpURLConnection connection = openConnection(httpMethod, request, request != null, settings);
        final Charset requestCharset = HTTP.getCharset(settings == null || settings.headers().isEmpty() ? _settings.headers() : settings.headers());
        final long sentRequestAtMillis = System.currentTimeMillis();
        InputStream is = null;
        OutputStream os = null;

        try {
            if (request != null && (httpMethod.equals(HttpMethod.POST) || httpMethod.equals(HttpMethod.PUT))) {
                os = HTTP.getOutputStream(connection, requestContentFormat, getContentType(settings), getContentEncoding(settings));

                Type<Object> type = N.typeOf(request.getClass());

                if (request instanceof File) {
                    try (InputStream fileInputStream = new FileInputStream((File) request)) {
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
                    if (type.isSerializable()) {
                        os.write(type.stringOf(request).getBytes(requestCharset));
                    } else {
                        HTTP.getParser(requestContentFormat).serialize(new OutputStreamWriter(os, requestCharset), request);
                    }
                }

                HTTP.flush(os);
            }

            final ContentFormat responseContentFormat = HTTP.getContentFormat(connection);
            final int code = connection.getResponseCode();
            final Map<String, List<String>> respHeaders = connection.getHeaderFields();
            final Charset respCharset = HTTP.getCharset(respHeaders);
            is = HTTP.getInputOrErrorStream(connection, responseContentFormat);

            if ((code < 200 || code >= 300) && (resultClass == null || !resultClass.equals(HttpResponse.class))) {
                throw new UncheckedIOException(new IOException(code + ": " + connection.getResponseMessage() + ". " + IOUtil.readString(is, respCharset)));
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
                        final byte[] respBody = IOUtil.readBytes(is);

                        return (T) new HttpResponse(sentRequestAtMillis, System.currentTimeMillis(), code, connection.getResponseMessage(), respHeaders,
                                respBody, responseContentFormat);
                    } else {
                        final Type<Object> type = resultClass == null ? null : N.typeOf(resultClass);

                        if (type == null) {
                            return (T) IOUtil.readString(is, respCharset);
                        } else if (byte[].class.equals(resultClass)) {
                            return (T) IOUtil.readBytes(is);
                        } else if (type.isSerializable()) {
                            return (T) type.valueOf(IOUtil.readString(is, respCharset));
                        } else {
                            return HTTP.getParser(responseContentFormat).deserialize(resultClass, IOUtil.newBufferedReader(is, respCharset));
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

    HttpURLConnection openConnection(HttpMethod httpMethod) throws UncheckedIOException {
        return openConnection(httpMethod, HttpMethod.POST.equals(httpMethod) || HttpMethod.PUT.equals(httpMethod));
    }

    HttpURLConnection openConnection(HttpMethod httpMethod, boolean doOutput) throws UncheckedIOException {
        return openConnection(httpMethod, doOutput, _settings);
    }

    HttpURLConnection openConnection(HttpMethod httpMethod, HttpSettings settings) throws UncheckedIOException {
        return openConnection(httpMethod, HttpMethod.POST.equals(httpMethod) || HttpMethod.PUT.equals(httpMethod), settings);
    }

    HttpURLConnection openConnection(HttpMethod httpMethod, boolean doOutput, HttpSettings settings) throws UncheckedIOException {
        return openConnection(httpMethod, null, doOutput, settings);
    }

    HttpURLConnection openConnection(HttpMethod httpMethod, final Object queryParameters, boolean doOutput, HttpSettings settings) throws UncheckedIOException {
        HttpURLConnection connection = null;

        if (_activeConnectionCounter.incrementAndGet() > _maxConnection) {
            _activeConnectionCounter.decrementAndGet();
            throw new AbacusException("Can not get connection, exceeded max connection number: " + _maxConnection);
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

            int connTimeout = _connTimeout > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) _connTimeout;

            if (settings != null) {
                connTimeout = settings.getConnectionTimeout();
            }

            if (connTimeout > 0) {
                connection.setConnectTimeout(connTimeout);
            }

            int readTimeout = _readTimeout > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) _readTimeout;

            if (settings != null) {
                readTimeout = settings.getReadTimeout();
            }

            if (readTimeout > 0) {
                connection.setReadTimeout(readTimeout);
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

    void setHttpProperties(HttpURLConnection connection, HttpSettings settings) throws UncheckedIOException {
        final HttpHeaders headers = settings.headers();

        if (headers != null) {
            Object headerValue = null;

            for (String headerName : headers.headerNameSet()) {
                headerValue = headers.get(headerName);

                if (headerValue instanceof Collection) {
                    final Iterator<Object> iter = ((Collection<Object>) headerValue).iterator();

                    if (iter.hasNext()) {
                        connection.setRequestProperty(headerName, N.stringOf(iter.next()));
                    }

                    while (iter.hasNext()) {
                        connection.addRequestProperty(headerName, N.stringOf(iter.next()));
                    }
                } else {
                    connection.setRequestProperty(headerName, N.stringOf(headerValue));
                }
            }
        }
    }

    void close(OutputStream os, InputStream is, HttpURLConnection connection) {
        try {
            IOUtil.closeQuietly(os);
            IOUtil.closeQuietly(is);
        } finally {
            _activeConnectionCounter.decrementAndGet();
        }

        // connection.disconnect();
    }
}
