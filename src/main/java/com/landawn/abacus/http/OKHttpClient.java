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
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLSocketFactory;

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.BufferedReader;
import com.landawn.abacus.util.BufferedWriter;
import com.landawn.abacus.util.ByteArrayOutputStream;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.URLEncodedUtil;

import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;

/**
 * Any header can be set into the parameter <code>settings</code>
 *
 * <br>HttpClient is thread safe.</br>
 *
 * @author Haiyang Li
 * @since 0.8
 */
public final class OKHttpClient extends AbstractHttpClient {

    /** The Constant logger. */
    static final Logger logger = LoggerFactory.getLogger(OKHttpClient.class);

    /** The Constant mediaTypePool. */
    private static final Map<String, MediaType> mediaTypePool = new ConcurrentHashMap<>();

    /** The client. */
    private final OkHttpClient client;

    /** The active connection counter. */
    private final AtomicInteger _activeConnectionCounter;

    /**
     * Instantiates a new OK http client.
     *
     * @param url
     */
    protected OKHttpClient(String url) {
        this(url, DEFAULT_MAX_CONNECTION);
    }

    /**
     * Instantiates a new OK http client.
     *
     * @param url
     * @param maxConnection
     */
    protected OKHttpClient(String url, int maxConnection) {
        this(url, maxConnection, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    /**
     * Instantiates a new OK http client.
     *
     * @param url
     * @param maxConnection
     * @param connectionTimeout
     * @param readTimeout
     */
    protected OKHttpClient(String url, int maxConnection, long connectionTimeout, long readTimeout) {
        this(url, maxConnection, connectionTimeout, readTimeout, null);
    }

    /**
     * Instantiates a new OK http client.
     *
     * @param url
     * @param maxConnection
     * @param connectionTimeout
     * @param readTimeout
     * @param settings
     * @throws UncheckedIOException the unchecked IO exception
     */
    protected OKHttpClient(String url, int maxConnection, long connectionTimeout, long readTimeout, HttpSettings settings) throws UncheckedIOException {
        this(url, maxConnection, connectionTimeout, readTimeout, settings, new AtomicInteger(0));
    }

    /**
     * Instantiates a new OK http client.
     *
     * @param url
     * @param maxConnection
     * @param connectionTimeout
     * @param readTimeout
     * @param settings
     * @param sharedActiveConnectionCounter
     */
    protected OKHttpClient(String url, int maxConnection, long connectionTimeout, long readTimeout, HttpSettings settings,
            final AtomicInteger sharedActiveConnectionCounter) {
        super(url, maxConnection, connectionTimeout, readTimeout, settings);

        final SSLSocketFactory ssf = settings == null ? null : settings.getSSLSocketFactory();
        final OkHttpClient.Builder builder = new OkHttpClient.Builder();

        if (ssf != null) {
            builder.socketFactory(ssf);
        }

        this.client = builder.connectionPool(new ConnectionPool(Math.min(8, maxConnection), 5, TimeUnit.MINUTES))
                .connectTimeout(connectionTimeout, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
                .build();

        this._activeConnectionCounter = sharedActiveConnectionCounter;
    }

    /**
     * Instantiates a new OK http client.
     *
     * @param client
     * @param url
     * @param maxConnection
     */
    protected OKHttpClient(OkHttpClient client, String url, int maxConnection) {
        this(client, url, maxConnection, null);
    }

    /**
     * Instantiates a new OK http client.
     *
     * @param client
     * @param url
     * @param maxConnection
     * @param settings
     * @throws UncheckedIOException the unchecked IO exception
     */
    protected OKHttpClient(OkHttpClient client, String url, int maxConnection, HttpSettings settings) throws UncheckedIOException {
        this(client, url, maxConnection, settings, new AtomicInteger(0));
    }

    /**
     * Instantiates a new OK http client.
     *
     * @param client
     * @param url
     * @param maxConnection
     * @param settings
     * @param sharedActiveConnectionCounter
     */
    protected OKHttpClient(OkHttpClient client, String url, int maxConnection, HttpSettings settings, final AtomicInteger sharedActiveConnectionCounter) {
        super(url, maxConnection, client.connectTimeoutMillis(), client.readTimeoutMillis(), settings);
        this.client = client;
        this._activeConnectionCounter = sharedActiveConnectionCounter;
    }

    /**
     *
     * @param url
     * @return
     */
    public static OKHttpClient create(String url) {
        return new OKHttpClient(url);
    }

    /**
     *
     * @param url
     * @param maxConnection
     * @return
     */
    public static OKHttpClient create(String url, int maxConnection) {
        return new OKHttpClient(url, maxConnection);
    }

    /**
     *
     * @param url
     * @param connectionTimeout
     * @param readTimeout
     * @return
     */
    public static OKHttpClient create(String url, long connectionTimeout, long readTimeout) {
        return new OKHttpClient(url, DEFAULT_MAX_CONNECTION, connectionTimeout, readTimeout);
    }

    /**
     *
     * @param url
     * @param maxConnection
     * @param connectionTimeout
     * @param readTimeout
     * @return
     */
    public static OKHttpClient create(String url, int maxConnection, long connectionTimeout, long readTimeout) {
        return new OKHttpClient(url, maxConnection, connectionTimeout, readTimeout);
    }

    /**
     *
     * @param url
     * @param maxConnection
     * @param connectionTimeout
     * @param readTimeout
     * @param settings
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static OKHttpClient create(String url, int maxConnection, long connectionTimeout, long readTimeout, HttpSettings settings)
            throws UncheckedIOException {
        return new OKHttpClient(url, maxConnection, connectionTimeout, readTimeout, settings);
    }

    /**
     *
     * @param url
     * @param maxConnection
     * @param connectionTimeout
     * @param readTimeout
     * @param settings
     * @param sharedActiveConnectionCounter
     * @return
     */
    public static OKHttpClient create(String url, int maxConnection, long connectionTimeout, long readTimeout, HttpSettings settings,
            final AtomicInteger sharedActiveConnectionCounter) {
        return new OKHttpClient(url, maxConnection, connectionTimeout, readTimeout, settings, sharedActiveConnectionCounter);
    }

    /**
     *
     * @param client
     * @param url
     * @param maxConnection
     * @return
     */
    public static OKHttpClient create(OkHttpClient client, String url, int maxConnection) {
        return new OKHttpClient(client, url, maxConnection);
    }

    /**
     *
     * @param client
     * @param url
     * @param maxConnection
     * @param settings
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static OKHttpClient create(OkHttpClient client, String url, int maxConnection, HttpSettings settings) throws UncheckedIOException {
        return new OKHttpClient(client, url, maxConnection, settings);
    }

    /**
     *
     * @param client
     * @param url
     * @param maxConnection
     * @param settings
     * @param sharedActiveConnectionCounter
     * @return
     */
    public static OKHttpClient create(OkHttpClient client, String url, int maxConnection, HttpSettings settings,
            final AtomicInteger sharedActiveConnectionCounter) {
        return new OKHttpClient(client, url, maxConnection, settings, sharedActiveConnectionCounter);
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
    @Override
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

    /**
     *
     * @param output
     * @param httpMethod
     * @param request
     * @param settings
     * @throws UncheckedIOException the unchecked IO exception
     */
    @Override
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
    @Override
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

        if (_activeConnectionCounter.incrementAndGet() > _maxConnection) {
            _activeConnectionCounter.decrementAndGet();
            throw new RuntimeException("Can not get connection, exceeded max connection number: " + _maxConnection);
        }

        final ContentFormat requestContentFormat = getContentFormat(settings);
        final String contentType = getContentType(settings);
        final String contentEncoding = getContentEncoding(settings);
        final Charset requestCharset = HttpUtil.getCharset(settings == null || settings.headers().isEmpty() ? _settings.headers() : settings.headers());

        okhttp3.Request httpRequest = null;
        okhttp3.Response httpResponse = null;
        boolean closeOkHttpResponse = true;

        try {
            final String url = (request != null && (httpMethod.equals(HttpMethod.GET) || httpMethod.equals(HttpMethod.DELETE)))
                    ? URLEncodedUtil.encode(_url, request)
                    : _url;

            final okhttp3.Request.Builder requestBuilder = new okhttp3.Request.Builder().url(url);

            setHeaders(requestBuilder, settings == null ? _settings : settings);

            if (request != null && (httpMethod.equals(HttpMethod.POST) || httpMethod.equals(HttpMethod.PUT))) {
                MediaType mediaType = null;

                if (N.notNullOrEmpty(contentType)) {
                    mediaType = mediaTypePool.get(contentType);
                    if (mediaType == null) {
                        mediaType = MediaType.parse(contentType);

                        if (mediaType != null) {
                            mediaTypePool.put(contentType, mediaType);
                        }
                    }
                }

                RequestBody body = null;
                final Type<Object> type = N.typeOf(request.getClass());
                final ByteArrayOutputStream bos = Objectory.createByteArrayOutputStream();

                try {
                    final OutputStream os = HttpUtil.wrapOutputStream(bos, requestContentFormat);

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

                    body = RequestBody.create(mediaType, bos.toByteArray());
                } finally {
                    Objectory.recycle(bos);
                }

                requestBuilder.method(httpMethod.name(), body);

                if (N.notNullOrEmpty(contentType)) {
                    requestBuilder.addHeader(HttpHeaders.Names.CONTENT_TYPE, contentType);
                }
                if (N.notNullOrEmpty(contentEncoding)) {
                    requestBuilder.addHeader(HttpHeaders.Names.CONTENT_ENCODING, contentEncoding);
                }
            } else {
                requestBuilder.method(httpMethod.name(), null);
            }

            httpRequest = requestBuilder.build();
            httpResponse = client.newCall(httpRequest).execute();

            final Map<String, List<String>> respHeaders = httpResponse.headers().toMultimap();
            final Charset respCharset = HttpUtil.getCharset(respHeaders);
            final ContentFormat respContentFormat = HttpUtil.getResponseContentFormat(respHeaders, requestContentFormat);
            final InputStream is = N.defaultIfNull(HttpUtil.wrapInputStream(httpResponse.body().byteStream(), respContentFormat), N.emptyInputStream());

            if (httpResponse.isSuccessful() == false
                    && (resultClass == null || !(resultClass.equals(HttpResponse.class) || resultClass.equals(okhttp3.Response.class)))) {
                throw new UncheckedIOException(
                        new IOException(httpResponse.code() + ": " + httpResponse.message() + ". " + IOUtil.readString(is, respCharset)));
            }

            if (isOneWayRequest(settings)) {
                return null;
            } else if (resultClass != null && resultClass.equals(okhttp3.Response.class)) {
                closeOkHttpResponse = false;

                return (T) httpResponse;
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
                        return (T) new HttpResponse(httpResponse.sentRequestAtMillis(), httpResponse.receivedResponseAtMillis(), httpResponse.code(),
                                httpResponse.message(), respHeaders, IOUtil.readAllBytes(is), respContentFormat, respCharset);
                    } else {
                        if (resultClass == null || resultClass.equals(String.class)) {
                            return (T) IOUtil.readString(is, respCharset);
                        } else if (byte[].class.equals(resultClass)) {
                            return (T) IOUtil.readAllBytes(is);
                        } else {
                            if (respContentFormat == ContentFormat.KRYO && HttpUtil.kryoParser != null) {
                                return HttpUtil.kryoParser.deserialize(resultClass, is);
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
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            _activeConnectionCounter.decrementAndGet();

            if (httpResponse != null && closeOkHttpResponse) {
                httpResponse.close();
            }
        }
    }

    /**
     * Sets the headers.
     *
     * @param requestBuilder
     * @param settings
     * @throws UncheckedIOException the unchecked IO exception
     */
    private void setHeaders(okhttp3.Request.Builder requestBuilder, HttpSettings settings) throws UncheckedIOException {
        final HttpHeaders headers = settings.headers();

        if (headers != null) {

            Object headerValue = null;

            for (String headerName : headers.headerNameSet()) {
                headerValue = headers.get(headerName);

                if (headerValue instanceof Collection) {
                    final Iterator<Object> iter = ((Collection<Object>) headerValue).iterator();

                    if (iter.hasNext()) {
                        requestBuilder.header(headerName, N.stringOf(iter.next()));
                    }

                    while (iter.hasNext()) {
                        requestBuilder.addHeader(headerName, N.stringOf(iter.next()));
                    }
                } else {
                    requestBuilder.header(headerName, N.stringOf(headerValue));
                }
            }
        }
    }
}
