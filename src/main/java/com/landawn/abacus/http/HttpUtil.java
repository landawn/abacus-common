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
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.parser.DeserializationConfig;
import com.landawn.abacus.parser.JSONParser;
import com.landawn.abacus.parser.KryoParser;
import com.landawn.abacus.parser.Parser;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.parser.SerializationConfig;
import com.landawn.abacus.parser.XMLParser;
import com.landawn.abacus.util.Charsets;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.LZ4BlockOutputStream;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.ObjectPool;
import com.landawn.abacus.util.StringUtil;
import com.landawn.abacus.util.WD;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
@Internal
public final class HttpUtil {
    public static final Charset DEFAULT_CHARSET = Charsets.UTF_8;

    public static final ContentFormat DEFAULT_CONTENT_FORMAT = ContentFormat.JSON;

    static final String JSON = "json";

    static final String XML = "xml";

    static final String GZIP = "gzip";

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
        contentFormat2Parser.put(ContentFormat.XML, xmlParser);
        contentFormat2Parser.put(ContentFormat.XML_LZ4, xmlParser);
        contentFormat2Parser.put(ContentFormat.XML_SNAPPY, xmlParser);
        contentFormat2Parser.put(ContentFormat.XML_GZIP, xmlParser);
        contentFormat2Parser.put(ContentFormat.FormUrlEncoded, jsonParser);
        contentFormat2Parser.put(ContentFormat.KRYO, kryoParser);

        // by default
        contentFormat2Parser.put(ContentFormat.NONE, jsonParser);
        contentFormat2Parser.put(ContentFormat.LZ4, jsonParser);
        contentFormat2Parser.put(ContentFormat.SNAPPY, jsonParser);
        contentFormat2Parser.put(ContentFormat.GZIP, jsonParser);
    }

    private static final Map<ContentFormat, String> contentFormat2Type = new EnumMap<>(ContentFormat.class);

    static {
        contentFormat2Type.put(ContentFormat.JSON, HttpHeaders.Values.APPLICATION_JSON);
        contentFormat2Type.put(ContentFormat.JSON_LZ4, HttpHeaders.Values.APPLICATION_JSON);
        contentFormat2Type.put(ContentFormat.JSON_SNAPPY, HttpHeaders.Values.APPLICATION_JSON);
        contentFormat2Type.put(ContentFormat.JSON_GZIP, HttpHeaders.Values.APPLICATION_JSON);
        contentFormat2Type.put(ContentFormat.XML, HttpHeaders.Values.APPLICATION_XML);
        contentFormat2Type.put(ContentFormat.XML_LZ4, HttpHeaders.Values.APPLICATION_XML);
        contentFormat2Type.put(ContentFormat.XML_SNAPPY, HttpHeaders.Values.APPLICATION_XML);
        contentFormat2Type.put(ContentFormat.XML_GZIP, HttpHeaders.Values.APPLICATION_XML);
        contentFormat2Type.put(ContentFormat.FormUrlEncoded, HttpHeaders.Values.APPLICATION_URL_ENCODED);
        contentFormat2Type.put(ContentFormat.KRYO, HttpHeaders.Values.APPLICATION_KRYO);
    }

    private static final Map<ContentFormat, String> contentFormat2Encoding = new EnumMap<>(ContentFormat.class);

    static {
        contentFormat2Encoding.put(ContentFormat.XML_GZIP, GZIP);
        contentFormat2Encoding.put(ContentFormat.XML_SNAPPY, SNAPPY);
        contentFormat2Encoding.put(ContentFormat.XML_LZ4, LZ4);
        contentFormat2Encoding.put(ContentFormat.JSON_GZIP, GZIP);
        contentFormat2Encoding.put(ContentFormat.JSON_SNAPPY, SNAPPY);
        contentFormat2Encoding.put(ContentFormat.JSON_LZ4, LZ4);
        contentFormat2Encoding.put(ContentFormat.GZIP, GZIP);
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

            if (StringUtil.containsIgnoreCase(entry.getKey().name(), GZIP)) {
                contentEncoding2Format.put(GZIP, entry.getKey());
            } else if (StringUtil.containsIgnoreCase(entry.getKey().name(), SNAPPY)) {
                contentEncoding2Format.put(SNAPPY, entry.getKey());
            } else if (StringUtil.containsIgnoreCase(entry.getKey().name(), LZ4)) {
                contentEncoding2Format.put(LZ4, entry.getKey());
            } else if (StringUtil.containsIgnoreCase(entry.getKey().name(), KRYO)) {
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
        contentEncoding2Format.put(SNAPPY, ContentFormat.SNAPPY);
        contentEncoding2Format.put(LZ4, ContentFormat.LZ4);
        contentEncoding2Format.put(KRYO, ContentFormat.KRYO);
        contentEncoding2Format.put(N.EMPTY_STRING, ContentFormat.NONE);
    }

    private HttpUtil() {
        // Singleton for utility class.
    }

    private static String readValue(Object value) {
        if (value != null) {
            if (value instanceof Collection) {
                return N.firstOrNullIfEmpty((Collection<String>) value);
            } else {
                return N.stringOf(value);
            }
        }

        return null;
    }

    public static String getContentType(final Map<String, ?> httpHeaders) {
        if (httpHeaders == null) {
            return null;
        }

        Object value = httpHeaders.get(HttpHeaders.Names.CONTENT_TYPE);

        if (value == null) {
            value = httpHeaders.get(HttpHeaders.Names.L_CONTENT_TYPE);
        }

        return readValue(value);
    }

    public static String getContentType(final HttpHeaders httpHeaders) {
        if (httpHeaders == null) {
            return null;
        }

        Object value = httpHeaders.get(HttpHeaders.Names.CONTENT_TYPE);

        if (value == null) {
            value = httpHeaders.get(HttpHeaders.Names.L_CONTENT_TYPE);
        }

        return readValue(value);
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

        return readValue(value);
    }

    public static String getContentEncoding(final HttpHeaders httpHeaders) {
        if (httpHeaders == null) {
            return null;
        }

        Object value = httpHeaders.get(HttpHeaders.Names.CONTENT_ENCODING);

        if (value == null) {
            value = httpHeaders.get(HttpHeaders.Names.L_CONTENT_ENCODING);
        }

        return readValue(value);
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

        return readValue(value);
    }

    public static String getAccept(final HttpHeaders httpHeaders) {
        if (httpHeaders == null) {
            return null;
        }

        Object value = httpHeaders.get(HttpHeaders.Names.ACCEPT);

        if (value == null) {
            value = httpHeaders.get(HttpHeaders.Names.L_ACCEPT);
        }

        return readValue(value);
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

        return readValue(value);
    }

    public static String getAcceptEncoding(final HttpHeaders httpHeaders) {
        if (httpHeaders == null) {
            return null;
        }

        Object value = httpHeaders.get(HttpHeaders.Names.ACCEPT_ENCODING);

        if (value == null) {
            value = httpHeaders.get(HttpHeaders.Names.L_ACCEPT_ENCODING);
        }

        return readValue(value);
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

        return readValue(value);
    }

    public static String getAcceptCharset(final HttpHeaders httpHeaders) {
        if (httpHeaders == null) {
            return null;
        }

        Object value = httpHeaders.get(HttpHeaders.Names.ACCEPT_CHARSET);

        if (value == null) {
            value = httpHeaders.get(HttpHeaders.Names.L_ACCEPT_CHARSET);
        }

        return readValue(value);
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
            if (StringUtil.containsIgnoreCase(contentType, HttpHeaders.Values.APPLICATION_JSON)) {
                contentEncoding2Format = contentTypeEncoding2Format.get(HttpHeaders.Values.APPLICATION_JSON);
            } else if (StringUtil.containsIgnoreCase(contentType, HttpHeaders.Values.APPLICATION_XML)) {
                contentEncoding2Format = contentTypeEncoding2Format.get(HttpHeaders.Values.APPLICATION_XML);
            } else if (StringUtil.containsIgnoreCase(contentType, HttpHeaders.Values.APPLICATION_URL_ENCODED)) {
                contentEncoding2Format = contentTypeEncoding2Format.get(HttpHeaders.Values.APPLICATION_URL_ENCODED);
            } else if (StringUtil.containsIgnoreCase(contentType, HttpHeaders.Values.APPLICATION_KRYO)) {
                contentEncoding2Format = contentTypeEncoding2Format.get(HttpHeaders.Values.APPLICATION_KRYO);
            }
        }

        if (contentEncoding2Format == null) {
            if (StringUtil.containsIgnoreCase(contentType, JSON)) {
                contentEncoding2Format = contentTypeEncoding2Format.get(HttpHeaders.Values.APPLICATION_JSON);
            } else if (StringUtil.containsIgnoreCase(contentType, XML)) {
                contentEncoding2Format = contentTypeEncoding2Format.get(HttpHeaders.Values.APPLICATION_XML);
            } else if (StringUtil.containsIgnoreCase(contentType, URL_ENCODED)) {
                contentEncoding2Format = contentTypeEncoding2Format.get(HttpHeaders.Values.APPLICATION_URL_ENCODED);
            } else if (StringUtil.containsIgnoreCase(contentType, KRYO)) {
                contentEncoding2Format = contentTypeEncoding2Format.get(HttpHeaders.Values.APPLICATION_KRYO);
            } else {
                contentEncoding2Format = contentTypeEncoding2Format.get(N.EMPTY_STRING);
            }
        }

        ContentFormat contentFormat = contentEncoding2Format.get(contentEncoding);

        if (contentFormat == null) {
            if (StringUtil.containsIgnoreCase(contentEncoding, GZIP)) {
                contentFormat = contentEncoding2Format.get(GZIP);
            } else if (StringUtil.containsIgnoreCase(contentEncoding, SNAPPY)) {
                contentFormat = contentEncoding2Format.get(SNAPPY);
            } else if (StringUtil.containsIgnoreCase(contentEncoding, LZ4)) {
                contentFormat = contentEncoding2Format.get(LZ4);
            } else if (StringUtil.containsIgnoreCase(contentEncoding, KRYO)) {
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

        if (StringUtil.containsIgnoreCase(contentFormatName, GZIP)) {
            return IOUtil.newGZIPInputStream(is);
        } else if (StringUtil.containsIgnoreCase(contentFormatName, SNAPPY)) {
            return IOUtil.newSnappyInputStream(is);
        } else if (StringUtil.containsIgnoreCase(contentFormatName, LZ4)) {
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

        if (StringUtil.containsIgnoreCase(contentFormatName, GZIP)) {
            return IOUtil.newGZIPOutputStream(os);
        } else if (StringUtil.containsIgnoreCase(contentFormatName, SNAPPY)) {
            return IOUtil.newSnappyOutputStream(os);
        } else if (StringUtil.containsIgnoreCase(contentFormatName, LZ4)) {
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
            return N.defaultIfNull(connection.getErrorStream(), N.emptyInputStream());
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

    private static final String CHARSET_SEQUAL = "charset=";

    public static Charset getCharset(final String contentType, final Charset defaultIfNull) {
        if (N.isNullOrEmpty(contentType)) {
            return defaultIfNull;
        }

        int fromIndex = StringUtil.indexOfIgnoreCase(contentType, CHARSET_SEQUAL);

        if (fromIndex < 0) {
            return defaultIfNull;
        }

        int toIndex = contentType.indexOf(WD._SEMICOLON, fromIndex);

        return Charsets.get(contentType.substring(fromIndex + CHARSET_SEQUAL.length(), toIndex > 0 ? toIndex : contentType.length()));
    }

    /**
     * For test only. Don't use it on production.
     */
    // copied from: https://nakov.com/blog/2009/07/16/disable-certificate-validation-in-java-ssl-connections/
    @Deprecated
    public static void turnOffCertificateValidation() {
        // Create a trust manager that does not validate certificate chains
        final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
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
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
