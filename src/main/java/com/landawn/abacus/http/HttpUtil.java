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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.parser.DeserializationConfig;
import com.landawn.abacus.parser.JSONParser;
import com.landawn.abacus.parser.KryoParser;
import com.landawn.abacus.parser.Parser;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.parser.SerializationConfig;
import com.landawn.abacus.parser.XMLParser;
import com.landawn.abacus.util.AndroidUtil;
import com.landawn.abacus.util.AsyncExecutor;
import com.landawn.abacus.util.Charsets;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.LZ4BlockOutputStream;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.ObjectPool;
import com.landawn.abacus.util.Strings;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
@Internal
public final class HttpUtil {
    private static final Logger logger = LoggerFactory.getLogger(HttpUtil.class);

    static final Executor DEFAULT_EXECUTOR;

    static {
        if (IOUtil.IS_PLATFORM_ANDROID) {
            DEFAULT_EXECUTOR = AndroidUtil.getThreadPoolExecutor();
        } else {
            DEFAULT_EXECUTOR = new ThreadPoolExecutor(//
                    N.max(64, IOUtil.CPU_CORES * 8, (IOUtil.MAX_MEMORY_IN_MB / 1024) * 8), // coreThreadPoolSize
                    N.max(128, IOUtil.CPU_CORES * 16, (IOUtil.MAX_MEMORY_IN_MB / 1024) * 16), // // maxThreadPoolSize
                    180L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (DEFAULT_EXECUTOR instanceof ExecutorService) {
                    final ExecutorService executorService = (ExecutorService) DEFAULT_EXECUTOR;

                    logger.warn("Starting to shutdown tasks for Http Request");

                    try {
                        executorService.shutdown();

                        executorService.awaitTermination(60, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        logger.warn("Not all the requests/tasks for Http Request are completed successfully before shutdown.");
                    } finally {
                        logger.warn("Completed to shutdown tasks for Http Request");
                    }
                }
            }
        });
    }

    static final AsyncExecutor DEFAULT_ASYNC_EXECUTOR = new AsyncExecutor(DEFAULT_EXECUTOR);

    public static final Charset DEFAULT_CHARSET = Charsets.UTF_8;

    public static final ContentFormat DEFAULT_CONTENT_FORMAT = ContentFormat.JSON;

    static final String JSON = "json";

    static final String XML = "xml";

    static final String GZIP = "gzip";

    static final String BR = "br";

    static final String SNAPPY = "snappy";

    static final String LZ4 = "lz4";

    static final String KRYO = "kryo";

    static final String URL_ENCODED = "urlencoded";

    static final JSONParser jsonParser = ParserFactory.createJSONParser();

    static final XMLParser xmlParser = ParserFactory.isXMLAvailable() ? ParserFactory.createXMLParser() : null;

    static final KryoParser kryoParser = ParserFactory.isKryoAvailable() ? ParserFactory.createKryoParser() : null;

    private static final Map<ContentFormat, Parser<?, ?>> contentFormat2Parser = new EnumMap<>(ContentFormat.class);

    static {
        contentFormat2Parser.put(ContentFormat.JSON, jsonParser);
        contentFormat2Parser.put(ContentFormat.JSON_LZ4, jsonParser);
        contentFormat2Parser.put(ContentFormat.JSON_SNAPPY, jsonParser);
        contentFormat2Parser.put(ContentFormat.JSON_GZIP, jsonParser);
        contentFormat2Parser.put(ContentFormat.JSON_BR, jsonParser);
        contentFormat2Parser.put(ContentFormat.XML, xmlParser);
        contentFormat2Parser.put(ContentFormat.XML_LZ4, xmlParser);
        contentFormat2Parser.put(ContentFormat.XML_SNAPPY, xmlParser);
        contentFormat2Parser.put(ContentFormat.XML_GZIP, xmlParser);
        contentFormat2Parser.put(ContentFormat.XML_BR, xmlParser);
        contentFormat2Parser.put(ContentFormat.FormUrlEncoded, jsonParser);
        contentFormat2Parser.put(ContentFormat.KRYO, kryoParser);

        // by default
        contentFormat2Parser.put(ContentFormat.NONE, jsonParser);
        contentFormat2Parser.put(ContentFormat.LZ4, jsonParser);
        contentFormat2Parser.put(ContentFormat.SNAPPY, jsonParser);
        contentFormat2Parser.put(ContentFormat.GZIP, jsonParser);
        contentFormat2Parser.put(ContentFormat.BR, jsonParser);
    }

    private static final Map<ContentFormat, String> contentFormat2Type = new EnumMap<>(ContentFormat.class);

    static {
        contentFormat2Type.put(ContentFormat.JSON, HttpHeaders.Values.APPLICATION_JSON);
        contentFormat2Type.put(ContentFormat.JSON_LZ4, HttpHeaders.Values.APPLICATION_JSON);
        contentFormat2Type.put(ContentFormat.JSON_SNAPPY, HttpHeaders.Values.APPLICATION_JSON);
        contentFormat2Type.put(ContentFormat.JSON_GZIP, HttpHeaders.Values.APPLICATION_JSON);
        contentFormat2Type.put(ContentFormat.JSON_BR, HttpHeaders.Values.APPLICATION_JSON);
        contentFormat2Type.put(ContentFormat.XML, HttpHeaders.Values.APPLICATION_XML);
        contentFormat2Type.put(ContentFormat.XML_LZ4, HttpHeaders.Values.APPLICATION_XML);
        contentFormat2Type.put(ContentFormat.XML_SNAPPY, HttpHeaders.Values.APPLICATION_XML);
        contentFormat2Type.put(ContentFormat.XML_GZIP, HttpHeaders.Values.APPLICATION_XML);
        contentFormat2Type.put(ContentFormat.XML_BR, HttpHeaders.Values.APPLICATION_XML);
        contentFormat2Type.put(ContentFormat.FormUrlEncoded, HttpHeaders.Values.APPLICATION_URL_ENCODED);
        contentFormat2Type.put(ContentFormat.KRYO, HttpHeaders.Values.APPLICATION_KRYO);
    }

    private static final Map<ContentFormat, String> contentFormat2Encoding = new EnumMap<>(ContentFormat.class);

    static {
        contentFormat2Encoding.put(ContentFormat.XML_GZIP, GZIP);
        contentFormat2Encoding.put(ContentFormat.XML_BR, BR);
        contentFormat2Encoding.put(ContentFormat.XML_SNAPPY, SNAPPY);
        contentFormat2Encoding.put(ContentFormat.XML_LZ4, LZ4);
        contentFormat2Encoding.put(ContentFormat.JSON_GZIP, GZIP);
        contentFormat2Encoding.put(ContentFormat.JSON_BR, BR);
        contentFormat2Encoding.put(ContentFormat.JSON_SNAPPY, SNAPPY);
        contentFormat2Encoding.put(ContentFormat.JSON_LZ4, LZ4);
        contentFormat2Encoding.put(ContentFormat.GZIP, GZIP);
        contentFormat2Encoding.put(ContentFormat.BR, BR);
        contentFormat2Encoding.put(ContentFormat.SNAPPY, SNAPPY);
        contentFormat2Encoding.put(ContentFormat.LZ4, LZ4);
        contentFormat2Encoding.put(ContentFormat.KRYO, KRYO);
    }

    private static final Map<String, Map<String, ContentFormat>> contentTypeEncoding2Format = new ObjectPool<>(64);

    static {
        for (Map.Entry<ContentFormat, String> entry : contentFormat2Type.entrySet()) {
            Map<String, ContentFormat> contentEncoding2Format = contentTypeEncoding2Format.get(entry.getValue());

            if (contentEncoding2Format == null) {
                contentEncoding2Format = new HashMap<>();
                contentTypeEncoding2Format.put(entry.getValue(), contentEncoding2Format);
            }

            if (Strings.containsIgnoreCase(entry.getKey().name(), GZIP)) {
                contentEncoding2Format.put(GZIP, entry.getKey());
            } else if (Strings.containsIgnoreCase(entry.getKey().name(), BR)) {
                contentEncoding2Format.put(BR, entry.getKey());
            } else if (Strings.containsIgnoreCase(entry.getKey().name(), SNAPPY)) {
                contentEncoding2Format.put(SNAPPY, entry.getKey());
            } else if (Strings.containsIgnoreCase(entry.getKey().name(), LZ4)) {
                contentEncoding2Format.put(LZ4, entry.getKey());
            } else if (Strings.containsIgnoreCase(entry.getKey().name(), KRYO)) {
                contentEncoding2Format.put(KRYO, entry.getKey());
                contentEncoding2Format.put(N.EMPTY_STRING, entry.getKey());
            } else {
                contentEncoding2Format.put(N.EMPTY_STRING, entry.getKey());
            }
        }

        Map<String, ContentFormat> contentEncoding2Format = contentTypeEncoding2Format.get(N.EMPTY_STRING);

        if (contentEncoding2Format == null) {
            contentEncoding2Format = new HashMap<>();
            contentTypeEncoding2Format.put(N.EMPTY_STRING, contentEncoding2Format);
        }

        contentEncoding2Format.put(GZIP, ContentFormat.GZIP);
        contentEncoding2Format.put(BR, ContentFormat.BR);
        contentEncoding2Format.put(SNAPPY, ContentFormat.SNAPPY);
        contentEncoding2Format.put(LZ4, ContentFormat.LZ4);
        contentEncoding2Format.put(KRYO, ContentFormat.KRYO);
        contentEncoding2Format.put(N.EMPTY_STRING, ContentFormat.NONE);
    }

    private HttpUtil() {
        // Singleton for utility class.
    }

    public static boolean isSuccessfulResponseCode(int code) {
        return code >= 200 && code < 300;
    }

    public static boolean isValidHttpHeader(String key, String value) {
        if (N.isNullOrEmpty(key) || key.indexOf(HttpHeaders.LF) >= 0 || key.indexOf(':') >= 0) {
            return false;
        }

        if (N.notNullOrEmpty(value)) {
            final int len = value.length();
            int idx = value.indexOf(HttpHeaders.LF);

            while (idx != -1) {
                idx++;

                if (idx < len) {
                    char c = value.charAt(idx);

                    if ((c == ' ') || (c == '\t')) {
                        idx = value.indexOf(HttpHeaders.LF, idx);

                        continue;
                    }
                }

                return false;
            }
        }

        return true;
    }

    @SuppressWarnings("rawtypes")
    public static String readHttpHeadValue(Object value) {
        if (value == null) {
            return Strings.EMPTY;
        }

        if (value instanceof Collection) {
            final Collection c = (Collection) value;

            if (N.isNullOrEmpty(c)) {
                return Strings.EMPTY;
            } else if (c.size() == 1) {
                return N.stringOf(N.firstOrNullIfEmpty(c));
            } else {
                return Strings.join((Collection) value, ",");
            }
        } else {
            return N.stringOf(value);
        }
    }

    public static String getContentType(final Map<String, ?> httpHeaders) {
        if (httpHeaders == null) {
            return null;
        }

        Object value = httpHeaders.get(HttpHeaders.Names.CONTENT_TYPE);

        if (value == null) {
            value = httpHeaders.get(HttpHeaders.Names.L_CONTENT_TYPE);
        }

        return readHttpHeadValue(value);
    }

    public static String getContentType(final HttpHeaders httpHeaders) {
        if (httpHeaders == null) {
            return null;
        }

        Object value = httpHeaders.get(HttpHeaders.Names.CONTENT_TYPE);

        if (value == null) {
            value = httpHeaders.get(HttpHeaders.Names.L_CONTENT_TYPE);
        }

        return readHttpHeadValue(value);
    }

    public static String getContentType(final HttpSettings httpSettings) {
        if (httpSettings == null || httpSettings.headers() == null) {
            return null;
        }

        return getContentType(httpSettings.headers());
    }

    public static String getContentType(final HttpURLConnection connection) {
        return getContentType(connection.getHeaderFields());
    }

    public static String getContentEncoding(final Map<String, ?> httpHeaders) {
        if (httpHeaders == null) {
            return null;
        }

        Object value = httpHeaders.get(HttpHeaders.Names.CONTENT_ENCODING);

        if (value == null) {
            value = httpHeaders.get(HttpHeaders.Names.L_CONTENT_ENCODING);
        }

        return readHttpHeadValue(value);
    }

    public static String getContentEncoding(final HttpHeaders httpHeaders) {
        if (httpHeaders == null) {
            return null;
        }

        Object value = httpHeaders.get(HttpHeaders.Names.CONTENT_ENCODING);

        if (value == null) {
            value = httpHeaders.get(HttpHeaders.Names.L_CONTENT_ENCODING);
        }

        return readHttpHeadValue(value);
    }

    public static String getContentEncoding(final HttpSettings httpSettings) {
        if (httpSettings == null || httpSettings.headers() == null) {
            return null;
        }

        return getContentEncoding(httpSettings.headers());
    }

    public static String getContentEncoding(final HttpURLConnection connection) {
        return getContentEncoding(connection.getHeaderFields());
    }

    public static String getAccept(final Map<String, ?> httpHeaders) {
        if (httpHeaders == null) {
            return null;
        }

        Object value = httpHeaders.get(HttpHeaders.Names.ACCEPT);

        if (value == null) {
            value = httpHeaders.get(HttpHeaders.Names.L_ACCEPT);
        }

        return readHttpHeadValue(value);
    }

    public static String getAccept(final HttpHeaders httpHeaders) {
        if (httpHeaders == null) {
            return null;
        }

        Object value = httpHeaders.get(HttpHeaders.Names.ACCEPT);

        if (value == null) {
            value = httpHeaders.get(HttpHeaders.Names.L_ACCEPT);
        }

        return readHttpHeadValue(value);
    }

    public static String getAccept(final HttpSettings httpSettings) {
        if (httpSettings == null || httpSettings.headers() == null) {
            return null;
        }

        return getAccept(httpSettings.headers());
    }

    public static String getAccept(final HttpURLConnection connection) {
        return getAccept(connection.getHeaderFields());
    }

    public static String getAcceptEncoding(final Map<String, ?> httpHeaders) {
        if (httpHeaders == null) {
            return null;
        }

        Object value = httpHeaders.get(HttpHeaders.Names.ACCEPT_ENCODING);

        if (value == null) {
            value = httpHeaders.get(HttpHeaders.Names.L_ACCEPT_ENCODING);
        }

        return readHttpHeadValue(value);
    }

    public static String getAcceptEncoding(final HttpHeaders httpHeaders) {
        if (httpHeaders == null) {
            return null;
        }

        Object value = httpHeaders.get(HttpHeaders.Names.ACCEPT_ENCODING);

        if (value == null) {
            value = httpHeaders.get(HttpHeaders.Names.L_ACCEPT_ENCODING);
        }

        return readHttpHeadValue(value);
    }

    public static String getAcceptEncoding(final HttpSettings httpSettings) {
        if (httpSettings == null || httpSettings.headers() == null) {
            return null;
        }

        return getAcceptEncoding(httpSettings.headers());
    }

    public static String getAcceptEncoding(final HttpURLConnection connection) {
        return getAcceptEncoding(connection.getHeaderFields());
    }

    public static String getAcceptCharset(final Map<String, ?> httpHeaders) {
        if (httpHeaders == null) {
            return null;
        }

        Object value = httpHeaders.get(HttpHeaders.Names.ACCEPT_CHARSET);

        if (value == null) {
            value = httpHeaders.get(HttpHeaders.Names.L_ACCEPT_CHARSET);
        }

        return readHttpHeadValue(value);
    }

    public static String getAcceptCharset(final HttpHeaders httpHeaders) {
        if (httpHeaders == null) {
            return null;
        }

        Object value = httpHeaders.get(HttpHeaders.Names.ACCEPT_CHARSET);

        if (value == null) {
            value = httpHeaders.get(HttpHeaders.Names.L_ACCEPT_CHARSET);
        }

        return readHttpHeadValue(value);
    }

    public static String getAcceptCharset(final HttpSettings httpSettings) {
        if (httpSettings == null || httpSettings.headers() == null) {
            return null;
        }

        return getAcceptCharset(httpSettings.headers());
    }

    public static String getAcceptCharset(final HttpURLConnection connection) {
        return getAcceptCharset(connection.getHeaderFields());
    }

    /**
     * Gets the content type.
     *
     * @param contentFormat
     * @return
     */
    public static String getContentType(final ContentFormat contentFormat) {
        if (contentFormat == null || contentFormat == ContentFormat.NONE) {
            return null;
        }

        return contentFormat.contentType();
    }

    /**
     * Gets the content encoding.
     *
     * @param contentFormat
     * @return
     */
    public static String getContentEncoding(final ContentFormat contentFormat) {
        if (contentFormat == null || contentFormat == ContentFormat.NONE) {
            return null;
        }

        return contentFormat.contentEncoding();
    }

    /**
     * Gets the content format.
     *
     * @param contentType
     * @param contentEncoding
     * @return
     */
    public static ContentFormat getContentFormat(String contentType, String contentEncoding) {
        if (contentType == null) {
            contentType = N.EMPTY_STRING;
        }

        if (contentEncoding == null) {
            contentEncoding = N.EMPTY_STRING;
        }

        Map<String, ContentFormat> contentEncoding2Format = contentTypeEncoding2Format.get(contentType);

        if (contentEncoding2Format == null) {
            if (Strings.containsIgnoreCase(contentType, HttpHeaders.Values.APPLICATION_JSON)) {
                contentEncoding2Format = contentTypeEncoding2Format.get(HttpHeaders.Values.APPLICATION_JSON);
            } else if (Strings.containsIgnoreCase(contentType, HttpHeaders.Values.APPLICATION_XML)) {
                contentEncoding2Format = contentTypeEncoding2Format.get(HttpHeaders.Values.APPLICATION_XML);
            } else if (Strings.containsIgnoreCase(contentType, HttpHeaders.Values.APPLICATION_URL_ENCODED)) {
                contentEncoding2Format = contentTypeEncoding2Format.get(HttpHeaders.Values.APPLICATION_URL_ENCODED);
            } else if (Strings.containsIgnoreCase(contentType, HttpHeaders.Values.APPLICATION_KRYO)) {
                contentEncoding2Format = contentTypeEncoding2Format.get(HttpHeaders.Values.APPLICATION_KRYO);
            }
        }

        if (contentEncoding2Format == null) {
            if (Strings.containsIgnoreCase(contentType, JSON)) {
                contentEncoding2Format = contentTypeEncoding2Format.get(HttpHeaders.Values.APPLICATION_JSON);
            } else if (Strings.containsIgnoreCase(contentType, XML)) {
                contentEncoding2Format = contentTypeEncoding2Format.get(HttpHeaders.Values.APPLICATION_XML);
            } else if (Strings.containsIgnoreCase(contentType, URL_ENCODED)) {
                contentEncoding2Format = contentTypeEncoding2Format.get(HttpHeaders.Values.APPLICATION_URL_ENCODED);
            } else if (Strings.containsIgnoreCase(contentType, KRYO)) {
                contentEncoding2Format = contentTypeEncoding2Format.get(HttpHeaders.Values.APPLICATION_KRYO);
            } else {
                contentEncoding2Format = contentTypeEncoding2Format.get(N.EMPTY_STRING);
            }
        }

        ContentFormat contentFormat = contentEncoding2Format.get(contentEncoding);

        if (contentFormat == null) {
            if (Strings.containsIgnoreCase(contentEncoding, GZIP)) {
                contentFormat = contentEncoding2Format.get(GZIP);
            } else if (Strings.containsIgnoreCase(contentEncoding, BR)) {
                contentFormat = contentEncoding2Format.get(BR);
            } else if (Strings.containsIgnoreCase(contentEncoding, SNAPPY)) {
                contentFormat = contentEncoding2Format.get(SNAPPY);
            } else if (Strings.containsIgnoreCase(contentEncoding, LZ4)) {
                contentFormat = contentEncoding2Format.get(LZ4);
            } else if (Strings.containsIgnoreCase(contentEncoding, KRYO)) {
                contentFormat = contentEncoding2Format.get(KRYO);
            } else {
                contentFormat = contentEncoding2Format.get(N.EMPTY_STRING);
            }
        }

        return contentFormat == null ? ContentFormat.NONE : contentFormat;
    }

    /**
     * Gets the content format.
     *
     * @param connection
     * @return
     */
    public static ContentFormat getContentFormat(final HttpURLConnection connection) {
        return getContentFormat(getContentType(connection), getContentEncoding(connection));
    }

    public static ContentFormat getResponseContentFormat(final Map<String, ?> respHeaders, final ContentFormat requestContentFormat) {
        String contentType = getContentType(respHeaders);

        if (N.isNullOrEmpty(contentType) && requestContentFormat != null) {
            contentType = requestContentFormat.contentType();
        }

        String contentEncoding = getContentEncoding(respHeaders);

        // Content encoding should be specified explicitly
        //    if (N.isNullOrEmpty(contentEncoding) && requestContentFormat != null) {
        //        contentEncoding = requestContentFormat.contentEncoding();
        //    }

        return getContentFormat(contentType, contentEncoding);
    }

    /**
     * Gets the parser.
     *
     * @param <SC>
     * @param <DC>
     * @param contentFormat
     * @return
     */
    public static <SC extends SerializationConfig<?>, DC extends DeserializationConfig<?>> Parser<SC, DC> getParser(ContentFormat contentFormat) {
        if (contentFormat == null) {
            return (Parser<SC, DC>) jsonParser;
        }

        final Parser<SC, DC> parser = (Parser<SC, DC>) contentFormat2Parser.get(contentFormat);

        if (parser == null) {
            throw new IllegalArgumentException("Unsupported content format: " + contentFormat);
        }

        return parser;
    }

    /**
     * Wrap input stream.
     *
     * @param is
     * @param contentFormat
     * @return
     */
    public static InputStream wrapInputStream(final InputStream is, final ContentFormat contentFormat) {
        if (contentFormat == null || contentFormat == ContentFormat.NONE || is == null) {
            return is;
        }

        final String contentFormatName = contentFormat.name();

        if (Strings.containsIgnoreCase(contentFormatName, GZIP)) {
            return IOUtil.newGZIPInputStream(is);
        } else if (Strings.containsIgnoreCase(contentFormatName, BR)) {
            return IOUtil.newBrotliInputStream(is);
        } else if (Strings.containsIgnoreCase(contentFormatName, SNAPPY)) {
            return IOUtil.newSnappyInputStream(is);
        } else if (Strings.containsIgnoreCase(contentFormatName, LZ4)) {
            return IOUtil.newLZ4BlockInputStream(is);
        } else {
            return is;
        }
    }

    /**
     * Wrap output stream.
     *
     * @param os
     * @param contentFormat
     * @return
     */
    public static OutputStream wrapOutputStream(final OutputStream os, final ContentFormat contentFormat) {
        if (contentFormat == null || contentFormat == ContentFormat.NONE || os == null) {
            return os;
        }

        final String contentFormatName = contentFormat.name();

        if (Strings.containsIgnoreCase(contentFormatName, GZIP)) {
            return IOUtil.newGZIPOutputStream(os);
        } else if (Strings.containsIgnoreCase(contentFormatName, BR)) {
            // return IOUtil.newBrotliOutputStream(os);
            throw new UnsupportedOperationException("Unsupported content encoding: Brotli for http request");
        } else if (Strings.containsIgnoreCase(contentFormatName, SNAPPY)) {
            return IOUtil.newSnappyOutputStream(os);
        } else if (Strings.containsIgnoreCase(contentFormatName, LZ4)) {
            return IOUtil.newLZ4BlockOutputStream(os);
        } else {
            return os;
        }
    }

    /**
     * Gets the output stream.
     *
     * @param connection
     * @param contentFormat
     * @param contentType
     * @param contentEncoding
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static OutputStream getOutputStream(final HttpURLConnection connection, final ContentFormat contentFormat, String contentType,
            String contentEncoding) throws IOException {

        if (N.isNullOrEmpty(contentType) && contentFormat != null) {
            contentType = getContentType(contentFormat);
        }

        if (N.notNullOrEmpty(contentType)) {
            connection.setRequestProperty(HttpHeaders.Names.CONTENT_TYPE, contentType);
        }

        if (N.isNullOrEmpty(contentEncoding) && contentFormat != null) {
            contentEncoding = getContentEncoding(contentFormat);
        }

        if (N.notNullOrEmpty(contentEncoding)) {
            connection.setRequestProperty(HttpHeaders.Names.CONTENT_ENCODING, contentEncoding);
        }

        return wrapOutputStream(connection.getOutputStream(), contentFormat);
    }

    /**
     * Gets the input stream.
     *
     * @param connection
     * @param contentFormat
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static InputStream getInputStream(final HttpURLConnection connection, ContentFormat contentFormat) throws IOException {
        try {
            return N.defaultIfNull(wrapInputStream(connection.getInputStream(), contentFormat), N.emptyInputStream());
        } catch (IOException e) {
            return N.defaultIfNull(wrapInputStream(connection.getErrorStream(), contentFormat), N.emptyInputStream());
        }
    }

    /**
     *
     * @param os
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void flush(OutputStream os) throws IOException {
        if (os instanceof LZ4BlockOutputStream) {
            ((LZ4BlockOutputStream) os).finish();
        } else if (os instanceof GZIPOutputStream) {
            ((GZIPOutputStream) os).finish();
        }

        os.flush();
    }

    public static Charset getRequestCharset(final HttpHeaders headers) {
        return getCharset(getContentType(headers), HttpUtil.DEFAULT_CHARSET);
    }

    public static Charset getResponseCharset(Map<String, ?> headers, final Charset requestCharset) {
        return getCharset(getContentType(headers), requestCharset);
    }

    public static Charset getCharset(String contentType) {
        return getCharset(contentType, DEFAULT_CHARSET);
    }

    public static Charset getCharset(final String contentType, final Charset defaultIfNull) {
        if (N.isNullOrEmpty(contentType)) {
            return defaultIfNull;
        }

        int fromIndex = -1;

        if ((fromIndex = contentType.indexOf("charset")) >= 0) {
            fromIndex = contentType.indexOf('=', fromIndex) + 1;
            int endIndex = Strings.indexOfAny(contentType, fromIndex, ';', ',');

            return Charset.forName(endIndex < 0 ? contentType.substring(fromIndex).trim() : contentType.substring(fromIndex, endIndex).trim());
        }

        return defaultIfNull;
    }

    /**
     * For test only. Don't use it on production.
     */
    // copied from: https://nakov.com/blog/2009/07/16/disable-certificate-validation-in-java-ssl-connections/
    @Deprecated
    public static void turnOffCertificateValidation() {
        // Create a trust manager that does not validate certificate chains
        final TrustManager[] trustAllCerts = { new X509TrustManager() {
            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        } };

        try {
            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = (hostname, session) -> true;

            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * Copyright (C) 2011 The Android Open Source Project
     *
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     *
     *      http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     */

    /**
     * Copied from OkHttp under Apache License, Version 2.0.
     *
     * Best-effort parser for HTTP dates.
     */
    public static final class HttpDate {
        /** GMT and UTC are equivalent for our purposes. */
        public static final TimeZone UTC = TimeZone.getTimeZone("GMT");
        /** The last four-digit year: "Fri, 31 Dec 9999 23:59:59 GMT". */
        public static final long MAX_DATE = 253402300799999L;

        /**
         * Most websites serve cookies in the blessed format. Eagerly create the parser to ensure such
         * cookies are on the fast path.
         */
        private static final ThreadLocal<DateFormat> STANDARD_DATE_FORMAT = ThreadLocal.withInitial(() -> {
            // Date format specified by RFC 7231 section 7.1.1.1.
            DateFormat rfc1123 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
            rfc1123.setLenient(false);
            rfc1123.setTimeZone(UTC);
            return rfc1123;
        });

        /** If we fail to parse a date in a non-standard format, try each of these formats in sequence. */
        private static final String[] BROWSER_COMPATIBLE_DATE_FORMAT_STRINGS = {
                // HTTP formats required by RFC2616 but with any timezone.
                "EEE, dd MMM yyyy HH:mm:ss zzz", // RFC 822, updated by RFC 1123 with any TZ
                "EEEE, dd-MMM-yy HH:mm:ss zzz", // RFC 850, obsoleted by RFC 1036 with any TZ.
                "EEE MMM d HH:mm:ss yyyy", // ANSI C's asctime() format
                // Alternative formats.
                "EEE, dd-MMM-yyyy HH:mm:ss z", "EEE, dd-MMM-yyyy HH-mm-ss z", "EEE, dd MMM yy HH:mm:ss z", "EEE dd-MMM-yyyy HH:mm:ss z",
                "EEE dd MMM yyyy HH:mm:ss z", "EEE dd-MMM-yyyy HH-mm-ss z", "EEE dd-MMM-yy HH:mm:ss z", "EEE dd MMM yy HH:mm:ss z", "EEE,dd-MMM-yy HH:mm:ss z",
                "EEE,dd-MMM-yyyy HH:mm:ss z", "EEE, dd-MM-yyyy HH:mm:ss z",

                /* RI bug 6641315 claims a cookie of this format was once served by www.yahoo.com */
                "EEE MMM d yyyy HH:mm:ss z", };

        private static final DateFormat[] BROWSER_COMPATIBLE_DATE_FORMATS = new DateFormat[BROWSER_COMPATIBLE_DATE_FORMAT_STRINGS.length];

        /** Returns the date for {@code value}. Returns null if the value couldn't be parsed. */
        public static Date parse(String value) {
            if (value.length() == 0) {
                return null;
            }

            ParsePosition position = new ParsePosition(0);
            Date result = STANDARD_DATE_FORMAT.get().parse(value, position);
            if (position.getIndex() == value.length()) {
                // STANDARD_DATE_FORMAT must match exactly; all text must be consumed, e.g. no ignored
                // non-standard trailing "+01:00". Those cases are covered below.
                return result;
            }
            synchronized (BROWSER_COMPATIBLE_DATE_FORMAT_STRINGS) {
                for (int i = 0, count = BROWSER_COMPATIBLE_DATE_FORMAT_STRINGS.length; i < count; i++) {
                    DateFormat format = BROWSER_COMPATIBLE_DATE_FORMATS[i];
                    if (format == null) {
                        format = new SimpleDateFormat(BROWSER_COMPATIBLE_DATE_FORMAT_STRINGS[i], Locale.US);
                        // Set the timezone to use when interpreting formats that don't have a timezone. GMT is
                        // specified by RFC 7231.
                        format.setTimeZone(UTC);
                        BROWSER_COMPATIBLE_DATE_FORMATS[i] = format;
                    }
                    position.setIndex(0);
                    result = format.parse(value, position);
                    if (position.getIndex() != 0) {
                        // Something was parsed. It's possible the entire string was not consumed but we ignore
                        // that. If any of the BROWSER_COMPATIBLE_DATE_FORMAT_STRINGS ended in "'GMT'" we'd have
                        // to also check that position.getIndex() == value.length() otherwise parsing might have
                        // terminated early, ignoring things like "+01:00". Leaving this as != 0 means that any
                        // trailing junk is ignored.
                        return result;
                    }
                }
            }
            return null;
        }

        /** Returns the string for {@code value}. */
        public static String format(Date value) {
            return STANDARD_DATE_FORMAT.get().format(value);
        }

        private HttpDate() {
        }
    }
}
