/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.http;

import java.io.ByteArrayInputStream;
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
import com.landawn.abacus.parser.DeserializationConfig;
import com.landawn.abacus.parser.JsonParser;
import com.landawn.abacus.parser.KryoParser;
import com.landawn.abacus.parser.Parser;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.parser.SerializationConfig;
import com.landawn.abacus.parser.XmlParser;
import com.landawn.abacus.util.AndroidUtil;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.AsyncExecutor;
import com.landawn.abacus.util.Charsets;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.LZ4BlockOutputStream;
import com.landawn.abacus.util.MoreExecutors;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.ObjectPool;
import com.landawn.abacus.util.RegExUtil;
import com.landawn.abacus.util.Strings;

/**
 * Internal utility class for HTTP operations. This class is not intended for direct use by application code.
 * It provides static utility methods for working with HTTP connections,
 * including content type/encoding handling, stream wrapping, and response validation.
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Content type and encoding detection</li>
 *   <li>Stream compression/decompression support</li>
 *   <li>Charset handling</li>
 *   <li>HTTP header utilities</li>
 *   <li>Response code validation</li>
 *   <li>SSL/TLS utilities</li>
 * </ul>
 *
 * <p>This class cannot be instantiated.</p>
 *
 * @see HttpClient
 * @see HttpRequest
 * @see HttpResponse
 * @see HttpSettings
 * @see ContentFormat
 */
@Internal
public final class HttpUtil {
    static final Executor DEFAULT_EXECUTOR;

    static {
        if (IOUtil.IS_PLATFORM_ANDROID) {
            DEFAULT_EXECUTOR = AndroidUtil.getThreadPoolExecutor();
        } else {
            final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(//
                    N.max(64, IOUtil.CPU_CORES * 8), // coreThreadPoolSize
                    N.max(128, IOUtil.CPU_CORES * 16), // maxThreadPoolSize
                    180L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

            DEFAULT_EXECUTOR = threadPoolExecutor;

            MoreExecutors.addDelayedShutdownHook(threadPoolExecutor, 120, TimeUnit.SECONDS);
        }
    }

    static final AsyncExecutor DEFAULT_ASYNC_EXECUTOR = new AsyncExecutor(DEFAULT_EXECUTOR);

    /**
     * Default charset for HTTP operations (UTF-8).
     * This charset is used when no specific charset is specified in the Content-Type header.
     */
    public static final Charset DEFAULT_CHARSET = Charsets.UTF_8; // It should be utf-8 for web service or http call by default. // IOUtil.DEFAULT_CHARSET

    /**
     * Default content format for HTTP operations (JSON).
     * This format is used when no specific content format is specified.
     */
    public static final ContentFormat DEFAULT_CONTENT_FORMAT = ContentFormat.JSON;

    static final String JSON = "json";

    static final String XML = "xml";

    static final String GZIP = "gzip";

    static final String BR = "br";

    static final String SNAPPY = "snappy";

    static final String LZ4 = "lz4";

    static final String KRYO = "kryo";

    static final String URL_ENCODED = "urlencoded";

    static final JsonParser jsonParser = ParserFactory.createJsonParser();

    static final XmlParser xmlParser = ParserFactory.isXmlParserAvailable() ? ParserFactory.createXmlParser() : null;

    static final KryoParser kryoParser = ParserFactory.isKryoParserAvailable() ? ParserFactory.createKryoParser() : null;

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
        contentFormat2Parser.put(ContentFormat.FORM_URL_ENCODED, jsonParser);
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
        contentFormat2Type.put(ContentFormat.FORM_URL_ENCODED, HttpHeaders.Values.APPLICATION_URL_ENCODED);
        contentFormat2Type.put(ContentFormat.KRYO, HttpHeaders.Values.APPLICATION_KRYO);
    }

    private static final Map<String, Map<String, ContentFormat>> contentTypeEncoding2Format = new ObjectPool<>(64);

    static {
        for (final Map.Entry<ContentFormat, String> entry : contentFormat2Type.entrySet()) {
            final Map<String, ContentFormat> contentEncoding2Format = contentTypeEncoding2Format.computeIfAbsent(entry.getValue(), k -> new HashMap<>());

            if (Strings.containsIgnoreCase(entry.getKey().name(), GZIP)) {
                contentEncoding2Format.put(GZIP, entry.getKey());
            } else if (Strings.containsIgnoreCase(entry.getKey().name(), BR)) {
                contentEncoding2Format.put(BR, entry.getKey());
            } else if (Strings.containsIgnoreCase(entry.getKey().name(), SNAPPY)) {
                contentEncoding2Format.put(SNAPPY, entry.getKey());
            } else if (Strings.containsIgnoreCase(entry.getKey().name(), LZ4)) {
                contentEncoding2Format.put(LZ4, entry.getKey());
            } else {
                if (Strings.containsIgnoreCase(entry.getKey().name(), KRYO)) {
                    contentEncoding2Format.put(KRYO, entry.getKey());
                }
                contentEncoding2Format.put(Strings.EMPTY, entry.getKey());
            }
        }

        final Map<String, ContentFormat> contentEncoding2Format = contentTypeEncoding2Format.computeIfAbsent(Strings.EMPTY, k -> new HashMap<>());

        contentEncoding2Format.put(GZIP, ContentFormat.GZIP);
        contentEncoding2Format.put(BR, ContentFormat.BR);
        contentEncoding2Format.put(SNAPPY, ContentFormat.SNAPPY);
        contentEncoding2Format.put(LZ4, ContentFormat.LZ4);
        contentEncoding2Format.put(KRYO, ContentFormat.KRYO);
        contentEncoding2Format.put(Strings.EMPTY, ContentFormat.NONE);
    }

    private HttpUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Checks if the HTTP response code indicates success.
     * A response code is considered successful if it's in the range [200, 300).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * if (HttpUtil.isSuccessfulResponseCode(response.getResponseCode())) {
     *     // Process successful response
     * }
     * }</pre>
     *
     * @param code The HTTP response code to check
     * @return {@code true} if the code indicates success, {@code false} otherwise
     */
    public static boolean isSuccessfulResponseCode(final int code) {
        return code >= 200 && code < 300;
    }

    /**
     * Validates an HTTP header key-value pair.
     * Checks that the key is not empty, doesn't contain line separators or colons,
     * and that the value doesn't contain unescaped line separators.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Valid headers
     * boolean valid1 = HttpUtil.isValidHttpHeader("Content-Type", "application/json");       // true
     * boolean valid2 = HttpUtil.isValidHttpHeader("Authorization", "Bearer token123");       // true
     *
     * // Invalid headers
     * boolean invalid1 = HttpUtil.isValidHttpHeader("", "value");                            // false (empty key)
     * boolean invalid2 = HttpUtil.isValidHttpHeader("Key:Name", "value");                    // false (contains colon)
     * boolean invalid3 = HttpUtil.isValidHttpHeader("Key\nName", "value");                   // false (contains line separator)
     * boolean invalid4 = HttpUtil.isValidHttpHeader("Key", "value\nwithout continuation");   // false (unescaped newline)
     * }</pre>
     *
     * @param key The header key to validate
     * @param value The header value to validate
     * @return {@code true} if the header is valid, {@code false} otherwise
     */
    public static boolean isValidHttpHeader(final String key, final String value) {
        if (Strings.isEmpty(key) || RegExUtil.LINE_SEPARATOR.matcher(key).find() || key.indexOf(':') >= 0) {
            return false;
        }

        if (Strings.isNotEmpty(value)) {
            final int len = value.length();
            char c = 0;

            for (int i = 0; i < len; i++) {
                c = value.charAt(i);

                if (c == '\r' || c == HttpHeaders.LF) {
                    // RFC 7230: only CRLF is valid for obs-fold (header field folding).
                    // A bare LF (\n) must be rejected to prevent header injection.

                    // c == '\r': expect LF next
                    // After CRLF, expect at least one SP or HTAB (obs-fold continuation)
                    if ((c == HttpHeaders.LF) || ++i >= len || value.charAt(i) != HttpHeaders.LF || (++i >= len)) {
                        return false;
                    }

                    c = value.charAt(i);

                    if (c != ' ' && c != '\t') {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Reads an HTTP header value from various object types.
     * If the value is a {@link Collection}, an empty collection yields an empty string, a single-element
     * collection yields that element's string form, and multiple values are joined with commas.
     * Otherwise, converts the value to a string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Single value
     * String header1 = HttpUtil.readHttpHeaderValue("text/plain");
     *
     * // Multiple values
     * List<String> values = Arrays.asList("gzip", "deflate");
     * String header2 = HttpUtil.readHttpHeaderValue(values);   // "gzip,deflate"
     * }</pre>
     *
     * @param value The header value (can be {@code null}, String, Collection, or any object)
     * @return The header value as a string, or {@code null} if value is null
     */
    public static String readHttpHeaderValue(final Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Collection<?> c) {
            if (N.isEmpty(c)) {
                return Strings.EMPTY;
            } else if (c.size() == 1) {
                return N.stringOf(N.firstOrNullIfEmpty(c));
            } else {
                return Strings.join(c, ",");
            }
        } else {
            return N.stringOf(value);
        }
    }

    /**
     * Gets the Content-Type header value from HTTP headers.
     * Performs a case-insensitive lookup so it works with maps populated from
     * {@link HttpURLConnection#getHeaderFields()}, which preserves the casing the server
     * sent on the wire (often non-canonical like {@code "CONTENT-TYPE"}).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> headers = new HashMap<>();
     * headers.put("Content-Type", "application/json");
     * String contentType = HttpUtil.getContentType(headers);  // "application/json"
     *
     * // Works with any casing
     * headers.put("CONTENT-TYPE", "text/html");
     * contentType = HttpUtil.getContentType(headers);  // "text/html"
     * }</pre>
     *
     * @param httpHeaders The HTTP headers map
     * @return The Content-Type value, or {@code null} if not found
     */
    public static String getContentType(final Map<String, ?> httpHeaders) {
        if (httpHeaders == null) {
            return null;
        }

        return readHttpHeaderValue(findHeaderValueCaseInsensitive(httpHeaders, HttpHeaders.Names.CONTENT_TYPE, HttpHeaders.Names.L_CONTENT_TYPE));
    }

    /**
     * Gets the Content-Type header value from HttpHeaders.
     * Performs a case-insensitive lookup of the "Content-Type" header name.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpHeaders headers = HttpHeaders.create();
     * headers.set("Content-Type", "application/xml");
     * String contentType = HttpUtil.getContentType(headers);  // "application/xml"
     * }</pre>
     *
     * @param httpHeaders The HttpHeaders object
     * @return The Content-Type value, or {@code null} if not found
     */
    public static String getContentType(final HttpHeaders httpHeaders) {
        if (httpHeaders == null) {
            return null;
        }

        return readHttpHeaderValue(findHeaderValueCaseInsensitive(httpHeaders.map, HttpHeaders.Names.CONTENT_TYPE, HttpHeaders.Names.L_CONTENT_TYPE));
    }

    /**
     * Gets the Content-Type from HttpSettings.
     * Extracts the Content-Type from the headers within the settings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpSettings settings = HttpSettings.create()
     *     .header("Content-Type", "application/json");
     * String contentType = HttpUtil.getContentType(settings);  // "application/json"
     * }</pre>
     *
     * @param httpSettings The HttpSettings object
     * @return The Content-Type value, or {@code null} if not found
     */
    public static String getContentType(final HttpSettings httpSettings) {
        if (httpSettings == null || httpSettings.headers() == null) {
            return null;
        }

        return getContentType(httpSettings.headers());
    }

    /**
     * Gets the Content-Type from the response headers of an HttpURLConnection.
     * Extracts the Content-Type from the connection's response header fields.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpURLConnection conn = (HttpURLConnection) new URL("http://example.com/api").openConnection();
     * conn.connect();
     * String contentType = HttpUtil.getContentType(conn);  // "application/json", for example
     * }</pre>
     *
     * @param connection The HTTP connection
     * @return The Content-Type value from the response headers, or {@code null} if not found
     */
    public static String getContentType(final HttpURLConnection connection) {
        return getContentType(connection.getHeaderFields());
    }

    /**
     * Gets the Content-Encoding header value from HTTP headers.
     * Performs a case-insensitive lookup of the "Content-Encoding" header name.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> headers = new HashMap<>();
     * headers.put("Content-Encoding", "gzip");
     * String encoding = HttpUtil.getContentEncoding(headers);  // "gzip"
     *
     * // Works with either case
     * headers.put("content-encoding", "deflate");
     * encoding = HttpUtil.getContentEncoding(headers);  // "deflate"
     * }</pre>
     *
     * @param httpHeaders The HTTP headers map
     * @return The Content-Encoding value, or {@code null} if not found
     */
    public static String getContentEncoding(final Map<String, ?> httpHeaders) {
        if (httpHeaders == null) {
            return null;
        }

        return readHttpHeaderValue(findHeaderValueCaseInsensitive(httpHeaders, HttpHeaders.Names.CONTENT_ENCODING, HttpHeaders.Names.L_CONTENT_ENCODING));
    }

    /**
     * Gets the Content-Encoding header value from HttpHeaders.
     * Performs a case-insensitive lookup of the "Content-Encoding" header name.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpHeaders headers = HttpHeaders.create();
     * headers.set("Content-Encoding", "gzip");
     * String encoding = HttpUtil.getContentEncoding(headers);  // "gzip"
     * }</pre>
     *
     * @param httpHeaders The HttpHeaders object
     * @return The Content-Encoding value, or {@code null} if not found
     */
    public static String getContentEncoding(final HttpHeaders httpHeaders) {
        if (httpHeaders == null) {
            return null;
        }

        return readHttpHeaderValue(findHeaderValueCaseInsensitive(httpHeaders.map, HttpHeaders.Names.CONTENT_ENCODING, HttpHeaders.Names.L_CONTENT_ENCODING));
    }

    /**
     * Gets the Content-Encoding from HttpSettings.
     * Extracts the Content-Encoding from the headers within the settings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpSettings settings = HttpSettings.create()
     *     .header("Content-Encoding", "gzip");
     * String encoding = HttpUtil.getContentEncoding(settings);  // "gzip"
     * }</pre>
     *
     * @param httpSettings The HttpSettings object
     * @return The Content-Encoding value, or {@code null} if not found
     */
    public static String getContentEncoding(final HttpSettings httpSettings) {
        if (httpSettings == null || httpSettings.headers() == null) {
            return null;
        }

        return getContentEncoding(httpSettings.headers());
    }

    /**
     * Gets the Content-Encoding from the response headers of an HttpURLConnection.
     * Extracts the Content-Encoding from the connection's response header fields.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpURLConnection conn = (HttpURLConnection) new URL("http://example.com/api").openConnection();
     * conn.connect();
     * String encoding = HttpUtil.getContentEncoding(conn);  // "gzip", for example
     * }</pre>
     *
     * @param connection The HTTP connection
     * @return The Content-Encoding value from the response headers, or {@code null} if not found
     */
    public static String getContentEncoding(final HttpURLConnection connection) {
        return getContentEncoding(connection.getHeaderFields());
    }

    /**
     * Gets the Accept header value from HTTP headers.
     * Performs a case-insensitive lookup of the "Accept" header name.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> headers = new HashMap<>();
     * headers.put("Accept", "application/json");
     * String accept = HttpUtil.getAccept(headers);  // "application/json"
     *
     * // Works with either case
     * headers.put("accept", "text/html");
     * accept = HttpUtil.getAccept(headers);  // "text/html"
     * }</pre>
     *
     * @param httpHeaders The HTTP headers map
     * @return The Accept value, or {@code null} if not found
     */
    public static String getAccept(final Map<String, ?> httpHeaders) {
        if (httpHeaders == null) {
            return null;
        }

        return readHttpHeaderValue(findHeaderValueCaseInsensitive(httpHeaders, HttpHeaders.Names.ACCEPT, HttpHeaders.Names.L_ACCEPT));
    }

    /**
     * Gets the Accept header value from HttpHeaders.
     * Looks for both "Accept" and "accept" keys.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpHeaders headers = HttpHeaders.create();
     * headers.set("Accept", "application/json");
     * String accept = HttpUtil.getAccept(headers);  // "application/json"
     * }</pre>
     *
     * @param httpHeaders The HttpHeaders object
     * @return The Accept value, or {@code null} if not found
     */
    public static String getAccept(final HttpHeaders httpHeaders) {
        if (httpHeaders == null) {
            return null;
        }

        Object value = httpHeaders.get(HttpHeaders.Names.ACCEPT);

        if (value == null) {
            value = httpHeaders.get(HttpHeaders.Names.L_ACCEPT);
        }

        return readHttpHeaderValue(value);
    }

    /**
     * Gets the Accept header from HttpSettings.
     * Extracts the Accept header from the headers within the settings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpSettings settings = HttpSettings.create()
     *     .header("Accept", "application/xml");
     * String accept = HttpUtil.getAccept(settings);  // "application/xml"
     * }</pre>
     *
     * @param httpSettings The HttpSettings object
     * @return The Accept value, or {@code null} if not found
     */
    public static String getAccept(final HttpSettings httpSettings) {
        if (httpSettings == null || httpSettings.headers() == null) {
            return null;
        }

        return getAccept(httpSettings.headers());
    }

    /**
     * Gets the Accept header from the response headers of an HttpURLConnection.
     * Extracts the Accept header from the connection's response header fields.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpURLConnection conn = (HttpURLConnection) new URL("http://example.com/api").openConnection();
     * conn.connect();
     * String accept = HttpUtil.getAccept(conn);
     * }</pre>
     *
     * @param connection The HTTP connection
     * @return The Accept value from the response headers, or {@code null} if not found
     */
    public static String getAccept(final HttpURLConnection connection) {
        return getAccept(connection.getHeaderFields());
    }

    /**
     * Gets the Accept-Encoding header value from HTTP headers.
     * Performs a case-insensitive lookup of the "Accept-Encoding" header name.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> headers = new HashMap<>();
     * headers.put("Accept-Encoding", "gzip, deflate");
     * String acceptEncoding = HttpUtil.getAcceptEncoding(headers);  // "gzip, deflate"
     *
     * // Works with either case
     * headers.put("accept-encoding", "br");
     * acceptEncoding = HttpUtil.getAcceptEncoding(headers);  // "br"
     * }</pre>
     *
     * @param httpHeaders The HTTP headers map
     * @return The Accept-Encoding value, or {@code null} if not found
     */
    public static String getAcceptEncoding(final Map<String, ?> httpHeaders) {
        if (httpHeaders == null) {
            return null;
        }

        return readHttpHeaderValue(findHeaderValueCaseInsensitive(httpHeaders, HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Names.L_ACCEPT_ENCODING));
    }

    /**
     * Gets the Accept-Encoding header value from HttpHeaders.
     * Looks for both "Accept-Encoding" and "accept-encoding" keys.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpHeaders headers = HttpHeaders.create();
     * headers.set("Accept-Encoding", "gzip, deflate");
     * String acceptEncoding = HttpUtil.getAcceptEncoding(headers);  // "gzip, deflate"
     * }</pre>
     *
     * @param httpHeaders The HttpHeaders object
     * @return The Accept-Encoding value, or {@code null} if not found
     */
    public static String getAcceptEncoding(final HttpHeaders httpHeaders) {
        if (httpHeaders == null) {
            return null;
        }

        Object value = httpHeaders.get(HttpHeaders.Names.ACCEPT_ENCODING);

        if (value == null) {
            value = httpHeaders.get(HttpHeaders.Names.L_ACCEPT_ENCODING);
        }

        return readHttpHeaderValue(value);
    }

    /**
     * Gets the Accept-Encoding header from HttpSettings.
     * Extracts the Accept-Encoding header from the headers within the settings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpSettings settings = HttpSettings.create()
     *     .header("Accept-Encoding", "gzip");
     * String acceptEncoding = HttpUtil.getAcceptEncoding(settings);  // "gzip"
     * }</pre>
     *
     * @param httpSettings The HttpSettings object
     * @return The Accept-Encoding value, or {@code null} if not found
     */
    public static String getAcceptEncoding(final HttpSettings httpSettings) {
        if (httpSettings == null || httpSettings.headers() == null) {
            return null;
        }

        return getAcceptEncoding(httpSettings.headers());
    }

    /**
     * Gets the Accept-Encoding header from the response headers of an HttpURLConnection.
     * Extracts the Accept-Encoding header from the connection's response header fields.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpURLConnection conn = (HttpURLConnection) new URL("http://example.com/api").openConnection();
     * conn.connect();
     * String acceptEncoding = HttpUtil.getAcceptEncoding(conn);
     * }</pre>
     *
     * @param connection The HTTP connection
     * @return The Accept-Encoding value from the response headers, or {@code null} if not found
     */
    public static String getAcceptEncoding(final HttpURLConnection connection) {
        return getAcceptEncoding(connection.getHeaderFields());
    }

    /**
     * Gets the Accept-Charset header value from HTTP headers.
     * Performs a case-insensitive lookup of the "Accept-Charset" header name.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> headers = new HashMap<>();
     * headers.put("Accept-Charset", "utf-8");
     * String acceptCharset = HttpUtil.getAcceptCharset(headers);  // "utf-8"
     *
     * // Works with either case
     * headers.put("accept-charset", "iso-8859-1");
     * acceptCharset = HttpUtil.getAcceptCharset(headers);  // "iso-8859-1"
     * }</pre>
     *
     * @param httpHeaders The HTTP headers map
     * @return The Accept-Charset value, or {@code null} if not found
     */
    public static String getAcceptCharset(final Map<String, ?> httpHeaders) {
        if (httpHeaders == null) {
            return null;
        }

        return readHttpHeaderValue(findHeaderValueCaseInsensitive(httpHeaders, HttpHeaders.Names.ACCEPT_CHARSET, HttpHeaders.Names.L_ACCEPT_CHARSET));
    }

    /**
     * Gets the Accept-Charset header value from HttpHeaders.
     * Looks for both "Accept-Charset" and "accept-charset" keys.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpHeaders headers = HttpHeaders.create();
     * headers.set("Accept-Charset", "utf-8");
     * String acceptCharset = HttpUtil.getAcceptCharset(headers);  // "utf-8"
     * }</pre>
     *
     * @param httpHeaders The HttpHeaders object
     * @return The Accept-Charset value, or {@code null} if not found
     */
    public static String getAcceptCharset(final HttpHeaders httpHeaders) {
        if (httpHeaders == null) {
            return null;
        }

        Object value = httpHeaders.get(HttpHeaders.Names.ACCEPT_CHARSET);

        if (value == null) {
            value = httpHeaders.get(HttpHeaders.Names.L_ACCEPT_CHARSET);
        }

        return readHttpHeaderValue(value);
    }

    /**
     * Gets the Accept-Charset header from HttpSettings.
     * Extracts the Accept-Charset header from the headers within the settings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpSettings settings = HttpSettings.create()
     *     .header("Accept-Charset", "utf-8");
     * String acceptCharset = HttpUtil.getAcceptCharset(settings);  // "utf-8"
     * }</pre>
     *
     * @param httpSettings The HttpSettings object
     * @return The Accept-Charset value, or {@code null} if not found
     */
    public static String getAcceptCharset(final HttpSettings httpSettings) {
        if (httpSettings == null || httpSettings.headers() == null) {
            return null;
        }

        return getAcceptCharset(httpSettings.headers());
    }

    /**
     * Gets the Accept-Charset header from the response headers of an HttpURLConnection.
     * Extracts the Accept-Charset header from the connection's response header fields.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpURLConnection conn = (HttpURLConnection) new URL("http://example.com/api").openConnection();
     * conn.connect();
     * String acceptCharset = HttpUtil.getAcceptCharset(conn);
     * }</pre>
     *
     * @param connection The HTTP connection
     * @return The Accept-Charset value from the response headers, or {@code null} if not found
     */
    public static String getAcceptCharset(final HttpURLConnection connection) {
        return getAcceptCharset(connection.getHeaderFields());
    }

    /**
     * Gets the content type string for a ContentFormat.
     * For example, {@link ContentFormat#JSON} returns {@code "application/json"}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String json = HttpUtil.getContentType(ContentFormat.JSON);          // returns "application/json"
     * String xmlGzip = HttpUtil.getContentType(ContentFormat.XML_GZIP);   // returns "application/xml"
     * String none = HttpUtil.getContentType(ContentFormat.NONE);          // returns "" (empty string)
     * }</pre>
     *
     * @param contentFormat The content format
     * @return The content type string, or an empty string {@code ""} if {@code contentFormat} is {@code null},
     *         {@link ContentFormat#NONE}, or a format that has no associated content type
     */
    public static String getContentType(final ContentFormat contentFormat) {
        if (contentFormat == null || contentFormat == ContentFormat.NONE) {
            return Strings.EMPTY;
        }

        return contentFormat.contentType();
    }

    /**
     * Gets the content encoding string for a ContentFormat.
     * For example, {@link ContentFormat#JSON_GZIP} returns {@code "gzip"}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String gzip = HttpUtil.getContentEncoding(ContentFormat.JSON_GZIP);   // returns "gzip"
     * String lz4 = HttpUtil.getContentEncoding(ContentFormat.XML_LZ4);      // returns "lz4"
     * String none = HttpUtil.getContentEncoding(ContentFormat.JSON);        // returns "" (no compression)
     * }</pre>
     *
     * @param contentFormat The content format
     * @return The content encoding string, or an empty string {@code ""} if {@code contentFormat} is {@code null},
     *         {@link ContentFormat#NONE}, or a format that uses no compression
     */
    public static String getContentEncoding(final ContentFormat contentFormat) {
        if (contentFormat == null || contentFormat == ContentFormat.NONE) {
            return Strings.EMPTY;
        }

        return contentFormat.contentEncoding();
    }

    /**
     * Determines the ContentFormat from content type and encoding strings.
     * This method matches the content type and encoding to find the appropriate ContentFormat.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContentFormat format = HttpUtil.getContentFormat("application/json", "gzip");
     * // Returns ContentFormat.JSON_GZIP
     * }</pre>
     *
     * @param contentType The content type (e.g., "application/json")
     * @param contentEncoding The content encoding (e.g., "gzip")
     * @return The matching ContentFormat, or ContentFormat.NONE if no match is found
     */
    public static ContentFormat getContentFormat(String contentType, String contentEncoding) {
        if (contentType == null) {
            contentType = Strings.EMPTY;
        }

        if (contentEncoding == null) {
            contentEncoding = Strings.EMPTY;
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
                contentEncoding2Format = contentTypeEncoding2Format.get(Strings.EMPTY);
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
                contentFormat = contentEncoding2Format.get(Strings.EMPTY);
            }
        }

        return contentFormat == null ? ContentFormat.NONE : contentFormat;
    }

    /**
     * Gets the ContentFormat from an HttpURLConnection.
     * Determines the format based on the Content-Type and Content-Encoding headers.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpURLConnection conn = (HttpURLConnection) new URL("http://example.com/api").openConnection();
     * conn.connect();
     * ContentFormat format = HttpUtil.getContentFormat(conn);
     * // For a "Content-Type: application/json" + "Content-Encoding: gzip" response,
     * // returns ContentFormat.JSON_GZIP (when executed against a live server)
     * }</pre>
     *
     * @param connection The HTTP connection
     * @return The ContentFormat, or ContentFormat.NONE if not determined
     */
    public static ContentFormat getContentFormat(final HttpURLConnection connection) {
        return getContentFormat(getContentType(connection), getContentEncoding(connection));
    }

    /**
     * Gets the response ContentFormat based on response headers and request format.
     * If the response doesn't specify a Content-Type, falls back to the request format's content type.
     * The Content-Encoding is always taken from the response headers (no fallback) to avoid
     * incorrectly applying decompression to a response that wasn't compressed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> respHeaders = new HashMap<>();
     * respHeaders.put("Content-Type", "application/json");
     * respHeaders.put("Content-Encoding", "gzip");
     * ContentFormat format = HttpUtil.getResponseContentFormat(respHeaders, null);
     * // returns ContentFormat.JSON_GZIP
     *
     * // When the response omits Content-Type, the request format supplies it as a fallback:
     * ContentFormat fallback = HttpUtil.getResponseContentFormat(new HashMap<>(), ContentFormat.XML);
     * // returns ContentFormat.XML
     * }</pre>
     *
     * @param respHeaders The response headers
     * @param requestContentFormat The request content format (used only as a content-type fallback, may be {@code null})
     * @return The response ContentFormat, or {@link ContentFormat#NONE} if neither could be determined
     */
    public static ContentFormat getResponseContentFormat(final Map<String, ?> respHeaders, final ContentFormat requestContentFormat) {
        String contentType = getContentType(respHeaders);

        if (Strings.isEmpty(contentType) && requestContentFormat != null) {
            contentType = requestContentFormat.contentType();
        }

        final String contentEncoding = getContentEncoding(respHeaders);

        // Content encoding should be specified explicitly
        //    if (N.isEmpty(contentEncoding) && requestContentFormat != null) {
        //        contentEncoding = requestContentFormat.contentEncoding();
        //    }

        return getContentFormat(contentType, contentEncoding);
    }

    /**
     * Gets the parser for a specific ContentFormat.
     * The parser is used for serialization and deserialization of request/response bodies.
     * If {@code contentFormat} is {@code null}, the default JSON parser is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Parser<?, ?> jsonParser = HttpUtil.getParser(ContentFormat.JSON);   // a JSON parser
     * Parser<?, ?> xmlParser = HttpUtil.getParser(ContentFormat.XML);     // an XML parser
     * Parser<?, ?> defaultParser = HttpUtil.getParser(null);              // the default JSON parser
     * }</pre>
     *
     * @param <SC> The serialization config type
     * @param <DC> The deserialization config type
     * @param contentFormat The content format
     * @return The parser for the content format, or the default JSON parser if {@code contentFormat} is {@code null}
     * @throws IllegalArgumentException if the content format is not supported
     */
    public static <SC extends SerializationConfig<?>, DC extends DeserializationConfig<?>> Parser<SC, DC> getParser(final ContentFormat contentFormat) {
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
     * Wraps an input stream with decompression based on the content format.
     * Supports GZIP, Brotli, Snappy, and LZ4 decompression.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * InputStream wrapped = HttpUtil.wrapInputStream(inputStream, ContentFormat.JSON_GZIP);
     * // wrapped stream will automatically decompress GZIP data
     * }</pre>
     *
     * @param is The input stream to wrap
     * @param contentFormat The content format indicating compression
     * @return The wrapped input stream, or the original stream if no decompression is needed.
     *         Returns an empty stream if {@code is} is {@code null}.
     */
    public static InputStream wrapInputStream(final InputStream is, final ContentFormat contentFormat) {
        if (is == null) {
            return new ByteArrayInputStream(N.EMPTY_BYTE_ARRAY);
        }

        if (contentFormat == null || contentFormat == ContentFormat.NONE) {
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
     * Wraps an output stream with compression based on the content format.
     * Supports GZIP, Snappy, and LZ4 compression. Brotli compression is not supported for output.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OutputStream wrapped = HttpUtil.wrapOutputStream(outputStream, ContentFormat.JSON_GZIP);
     * // Data written to wrapped stream will be automatically compressed
     * }</pre>
     *
     * @param os The output stream to wrap
     * @param contentFormat The content format indicating compression
     * @return The wrapped output stream, or the original stream if no compression is needed
     * @throws UnsupportedOperationException if Brotli compression is requested
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
     * Gets an output stream from an HttpURLConnection with appropriate headers and wrapping.
     * Sets Content-Type and Content-Encoding headers based on the content format,
     * and wraps the stream with compression if needed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpURLConnection conn = (HttpURLConnection) new URL("http://example.com/api").openConnection();
     * conn.setRequestMethod("POST");
     * conn.setDoOutput(true);
     * OutputStream os = HttpUtil.getOutputStream(conn, ContentFormat.JSON_GZIP, null, null);
     * // sets "Content-Type: application/json" and "Content-Encoding: gzip" on the connection,
     * // then returns a GZIP-wrapping output stream (when executed against a live server)
     * }</pre>
     *
     * @param connection The HTTP connection
     * @param contentFormat The content format for the request body
     * @param contentType The Content-Type header value (can be null)
     * @param contentEncoding The Content-Encoding header value (can be null)
     * @return The output stream, possibly wrapped with compression
     * @throws IOException if an I/O error occurs
     */
    public static OutputStream getOutputStream(final HttpURLConnection connection, final ContentFormat contentFormat, String contentType, // NOSONAR
            String contentEncoding) throws IOException {
        if (Strings.isEmpty(contentType) && contentFormat != null) {
            contentType = getContentType(contentFormat);
        }

        if (Strings.isNotEmpty(contentType)) {
            connection.setRequestProperty(HttpHeaders.Names.CONTENT_TYPE, contentType);
        }

        if (Strings.isEmpty(contentEncoding) && contentFormat != null) {
            contentEncoding = getContentEncoding(contentFormat);
        }

        if (Strings.isNotEmpty(contentEncoding)) {
            connection.setRequestProperty(HttpHeaders.Names.CONTENT_ENCODING, contentEncoding);
        }

        return wrapOutputStream(connection.getOutputStream(), contentFormat);
    }

    /**
     * Gets an input stream from an HttpURLConnection with appropriate unwrapping.
     * Automatically handles both successful responses and error streams,
     * and wraps the stream with decompression based on the content format.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpURLConnection conn = (HttpURLConnection) new URL("http://example.com/api").openConnection();
     * conn.connect();
     * InputStream is = HttpUtil.getInputStream(conn, ContentFormat.JSON_GZIP);
     * // returns a GZIP-decompressing input stream over the response body
     * // (falls back to the error stream on a non-2xx response; when executed against a live server)
     * }</pre>
     *
     * @param connection The HTTP connection
     * @param contentFormat The content format for decompression
     * @return The input stream, possibly wrapped with decompression
     */
    public static InputStream getInputStream(final HttpURLConnection connection, final ContentFormat contentFormat) {
        try {
            return N.defaultIfNull(wrapInputStream(connection.getInputStream(), contentFormat), N.emptyInputStream());
        } catch (final IOException e) {
            return N.defaultIfNull(wrapInputStream(connection.getErrorStream(), contentFormat), N.emptyInputStream());
        }
    }

    /**
     * Flushes an output stream, handling special cases for compression streams.
     * For LZ4 and GZIP streams, calls finish() before flush() to ensure all data is written.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OutputStream os = HttpUtil.wrapOutputStream(connection.getOutputStream(), ContentFormat.JSON_GZIP);
     * os.write(payloadBytes);
     * HttpUtil.flush(os);  // finishes the GZIP stream, then flushes all buffered data
     * }</pre>
     *
     * @param os The output stream to flush
     * @throws IOException if an I/O error occurs
     */
    public static void flush(final OutputStream os) throws IOException {
        if (os instanceof LZ4BlockOutputStream) {
            ((LZ4BlockOutputStream) os).finish();
        } else if (os instanceof GZIPOutputStream) {
            ((GZIPOutputStream) os).finish();
        }

        os.flush();
    }

    /**
     * Gets the charset to use for HTTP requests based on headers.
     * Extracts the charset from the Content-Type header if present,
     * otherwise returns the default charset (UTF-8).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpHeaders headers = HttpHeaders.create().setContentType("application/json; charset=ISO-8859-1");
     * Charset charset = HttpUtil.getRequestCharset(headers);   // returns ISO-8859-1
     *
     * // No charset parameter -> the default (UTF-8) is used
     * Charset dft = HttpUtil.getRequestCharset(HttpHeaders.create().setContentType("application/json"));
     * // returns UTF-8
     * }</pre>
     *
     * @param headers The HTTP headers
     * @return The charset to use for the request
     */
    public static Charset getRequestCharset(final HttpHeaders headers) {
        return getCharset(getContentType(headers), HttpUtil.DEFAULT_CHARSET);
    }

    /**
     * Gets the charset to use for HTTP responses based on headers.
     * Extracts the charset from the Content-Type header if present,
     * otherwise returns the request charset as a fallback.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> respHeaders = new HashMap<>();
     * respHeaders.put("Content-Type", "text/html; charset=UTF-16");
     * Charset charset = HttpUtil.getResponseCharset(respHeaders, Charsets.UTF_8);  // returns UTF-16
     *
     * // No charset in the response -> the request charset is used as a fallback
     * Charset fallback = HttpUtil.getResponseCharset(new HashMap<>(), Charsets.UTF_8);  // returns UTF-8
     * }</pre>
     *
     * @param headers The response headers
     * @param requestCharset The charset used in the request (as fallback)
     * @return The charset to use for the response
     */
    public static Charset getResponseCharset(final Map<String, ?> headers, final Charset requestCharset) {
        return getCharset(getContentType(headers), requestCharset);
    }

    /**
     * Extracts the charset from a Content-Type header value.
     * Parses strings like "text/html; charset=UTF-8" to extract the charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Charset charset = HttpUtil.getCharset("text/html; charset=ISO-8859-1");  // returns ISO-8859-1
     * Charset dft = HttpUtil.getCharset("application/json");                   // returns UTF-8 (no charset)
     * Charset blank = HttpUtil.getCharset(null);                               // returns UTF-8 (default)
     * }</pre>
     *
     * @param contentType The Content-Type header value
     * @return The charset, or the default charset (UTF-8) if not found
     */
    public static Charset getCharset(final String contentType) {
        return getCharset(contentType, DEFAULT_CHARSET);
    }

    /**
     * Extracts the charset from a Content-Type header value with a specified default.
     * Parses strings like "text/html; charset=UTF-8" to extract the charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Charset charset = HttpUtil.getCharset("application/json; charset=ISO-8859-1", Charsets.UTF_8);
     * // Returns Charset for ISO-8859-1
     * }</pre>
     *
     * @param contentType The Content-Type header value
     * @param defaultIfNull The default charset to return if none is found
     * @return The charset from the content type, or the default if not found
     */
    public static Charset getCharset(final String contentType, final Charset defaultIfNull) {
        if (Strings.isEmpty(contentType)) {
            return defaultIfNull;
        }

        final String contentTypeLowerCase = contentType.toLowerCase(Locale.ROOT);
        int fromIndex = -1;

        // Find an occurrence of "charset" that is an actual parameter name (i.e. not a
        // substring of some other token like "x-charset-hint"). A parameter name is
        // delimited on the left by start-of-string or a non-token char (e.g. ';' or space)
        // and is followed (optionally after whitespace) by '='.
        int searchFrom = 0;

        while ((searchFrom = contentTypeLowerCase.indexOf("charset", searchFrom)) >= 0) {
            final boolean leftBoundaryOk = searchFrom == 0 || !isContentTypeTokenChar(contentTypeLowerCase.charAt(searchFrom - 1));

            int afterToken = searchFrom + "charset".length();

            while (afterToken < contentTypeLowerCase.length()
                    && (contentTypeLowerCase.charAt(afterToken) == ' ' || contentTypeLowerCase.charAt(afterToken) == '\t')) {
                afterToken++;
            }

            if (leftBoundaryOk && afterToken < contentTypeLowerCase.length() && contentTypeLowerCase.charAt(afterToken) == '=') {
                fromIndex = searchFrom;
                break;
            }

            searchFrom += "charset".length();
        }

        if (fromIndex >= 0) {
            fromIndex = contentType.indexOf('=', fromIndex);

            if (fromIndex < 0) {
                return defaultIfNull;
            }

            fromIndex += 1;
            final int toIndex = Strings.indexOfAny(contentType, fromIndex, Array.of(';', ','));

            String charsetName = (toIndex < 0 ? contentType.substring(fromIndex) : contentType.substring(fromIndex, toIndex)).trim();

            if (charsetName.length() > 1) {
                final char first = charsetName.charAt(0);
                final char last = charsetName.charAt(charsetName.length() - 1);

                if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                    charsetName = charsetName.substring(1, charsetName.length() - 1).trim();
                }
            }

            try {
                return Charset.forName(charsetName);
            } catch (final RuntimeException e) {
                return defaultIfNull;
            }
        }

        return defaultIfNull;
    }

    /**
     * Returns whether {@code c} can be part of an HTTP token (RFC 7230) so that the
     * "charset" parameter detection in {@link #getCharset(String, Charset)} does not
     * match "charset" appearing inside a larger token (e.g. {@code x-charset-hint}).
     *
     * @param c the character to test
     * @return {@code true} if {@code c} is a letter, digit, or one of {@code - _ .}
     */
    private static boolean isContentTypeTokenChar(final char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-' || c == '_' || c == '.';
    }

    /**
     * Performs a case-insensitive lookup against a header map. First tries the two
     * conventional spellings (preserved-case, then all-lower) so the common case is O(1);
     * falls back to scanning the entry set with {@link String#equalsIgnoreCase} only when
     * neither direct lookup hits. This is necessary because {@code HttpURLConnection.getHeaderFields()}
     * preserves server-supplied casing (e.g. some servers emit {@code CONTENT-TYPE}).
     *
     * @param httpHeaders the headers map (must not be {@code null})
     * @param canonical the canonical (mixed-case) header name to try first
     * @param lower the lowercase header name to try next
     * @return the header value found, or {@code null} if no entry matches case-insensitively
     */
    private static Object findHeaderValueCaseInsensitive(final Map<String, ?> httpHeaders, final String canonical, final String lower) {
        Object value = httpHeaders.get(canonical);

        if (value != null) {
            return value;
        }

        value = httpHeaders.get(lower);

        if (value != null) {
            return value;
        }

        // Fall back to a scan for arbitrary casing (e.g. server emits CONTENT-TYPE).
        for (final Map.Entry<String, ?> entry : httpHeaders.entrySet()) {
            final String name = entry.getKey();

            if (name != null && name.equalsIgnoreCase(canonical)) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Turns off certificate validation for HTTPS connections.
     * This method disables all certificate and hostname verification by installing
     * a trust-all {@link TrustManager} and a permissive {@link HostnameVerifier}.
     *
     * <p><b>WARNING:</b> This is extremely insecure and should NEVER be used in production code.
     * It makes the application vulnerable to man-in-the-middle attacks.
     * This method is provided for testing purposes only and affects the default
     * {@link HttpsURLConnection} behavior globally in the current JVM.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Test-only setup for self-signed certificates
     * HttpUtil.disableCertificateValidation();
     * HttpsURLConnection conn = (HttpsURLConnection) new URL("https://localhost:8443").openConnection();
     * }</pre>
     *
     * <p>Copied from: <a href="https://nakov.com/blog/2009/07/16/disable-certificate-validation-in-java-ssl-connections/">disable-certificate-validation-in-java-ssl-connections</a></p>
     *
     * @deprecated For test only. Don't use it in production.
     * @throws RuntimeException if SSL context initialization fails
     */
    @Deprecated
    public static void disableCertificateValidation() {
        // Create a trust manager that does not validate certificate chains
        final TrustManager[] trustAllCerts = { new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }

            @Override
            public void checkClientTrusted(final X509Certificate[] certs, final String authType) { //NOSONAR
            }

            @Override
            public void checkServerTrusted(final X509Certificate[] certs, final String authType) { //NOSONAR
            }
        } };

        try {
            // Install the all-trusting trust manager
            final SSLContext sc = SSLContext.getInstance("SSL"); //NOSONAR
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Create all-trusting host name verifier
            final HostnameVerifier allHostsValid = (hostname, session) -> true; //NOSONAR

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
     *      https://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     */

    /**
     * HTTP date parser and formatter.
     * This class provides methods for parsing and formatting HTTP dates according to RFC 7231.
     * It supports multiple date formats for compatibility with various HTTP implementations.
     *
     * <p>Copied from OkHttp under Apache License, Version 2.0.</p>
     */
    public static final class HttpDate {
        /**
         * The UTC timezone used for HTTP date formatting and parsing.
         * GMT and UTC are equivalent for HTTP date purposes.
         * All HTTP dates should be in GMT/UTC according to RFC 7231.
         */
        public static final TimeZone UTC = TimeZone.getTimeZone("GMT");

        /**
         * The maximum representable HTTP date in milliseconds since epoch.
         * Represents the last four-digit year: "Fri, 31 Dec 9999 23:59:59 GMT".
         * This is the value 253,402,300,799,999 milliseconds (December 31, 9999 23:59:59.999 GMT).
         */
        public static final long MAX_DATE = 253_402_300_799_999L;

        /**
         * Most websites serve cookies in the blessed format. Eagerly create the parser to ensure such
         * cookies are on the fast path.
         */
        private static final ThreadLocal<DateFormat> STANDARD_DATE_FORMAT = ThreadLocal.withInitial(() -> { //NOSONAR
            // Date format specified by RFC 7231 section 7.1.1.1.
            final DateFormat rfc1123 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
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

        /**
         * Parses an HTTP date string into a Date object.
         * Supports multiple date formats for compatibility.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Date date = HttpDate.parse("Wed, 21 Oct 2015 07:28:00 GMT");
         * }</pre>
         *
         * @param value The date string to parse
         * @return The parsed Date, or {@code null} if the value couldn't be parsed
         */
        public static Date parse(final String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }

            final ParsePosition position = new ParsePosition(0);
            Date result = STANDARD_DATE_FORMAT.get().parse(value, position);
            if (position.getIndex() == value.length()) {
                // STANDARD_DATE_FORMAT must match exactly; all text must be consumed, e.g., no ignored
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
                        // Something was parsed. It's possible the entire string was not consumed, but we ignore
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

        /**
         * Formats a Date into an HTTP date string.
         * Uses the standard RFC 7231 format.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String dateStr = HttpDate.format(new Date());
         * // Returns something like "Wed, 21 Oct 2015 07:28:00 GMT"
         * }</pre>
         *
         * @param value The date to format
         * @return The formatted date string
         */
        public static String format(final Date value) {
            return STANDARD_DATE_FORMAT.get().format(value);
        }

        private HttpDate() {
        }
    }
}
