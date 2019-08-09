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
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

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

// TODO: Auto-generated Javadoc
/**
 * The Class HTTP.
 *
 * @author Haiyang Li
 * @since 0.8
 */
@Internal
public final class HTTP {

    /** The Constant JSON. */
    static final String JSON = "json";

    /** The Constant XML. */
    static final String XML = "xml";

    /** The Constant GZIP. */
    static final String GZIP = "gzip";

    /** The Constant SNAPPY. */
    static final String SNAPPY = "snappy";

    /** The Constant LZ4. */
    static final String LZ4 = "lz4";

    /** The Constant KRYO. */
    static final String KRYO = "kryo";

    /** The Constant URL_ENCODED. */
    static final String URL_ENCODED = "urlencoded";

    /** The Constant jsonParser. */
    static final JSONParser jsonParser = ParserFactory.createJSONParser();

    /** The Constant xmlParser. */
    static final XMLParser xmlParser = ParserFactory.isXMLAvailable() ? ParserFactory.createXMLParser() : null;

    /** The Constant kryoParser. */
    static final KryoParser kryoParser = ParserFactory.isKryoAvailable() ? ParserFactory.createKryoParser() : null;

    /** The Constant contentFormat2Parser. */
    private static final Map<ContentFormat, Parser<?, ?>> contentFormat2Parser = new EnumMap<ContentFormat, Parser<?, ?>>(ContentFormat.class);

    static {
        contentFormat2Parser.put(ContentFormat.JSON, jsonParser);
        contentFormat2Parser.put(ContentFormat.JSON_LZ4, jsonParser);
        contentFormat2Parser.put(ContentFormat.JSON_SNAPPY, jsonParser);
        contentFormat2Parser.put(ContentFormat.JSON_GZIP, jsonParser);
        contentFormat2Parser.put(ContentFormat.XML, xmlParser);
        contentFormat2Parser.put(ContentFormat.XML_LZ4, xmlParser);
        contentFormat2Parser.put(ContentFormat.XML_SNAPPY, xmlParser);
        contentFormat2Parser.put(ContentFormat.XML_GZIP, xmlParser);
        contentFormat2Parser.put(ContentFormat.KRYO, kryoParser);

        // by default
        contentFormat2Parser.put(ContentFormat.NONE, jsonParser);
        contentFormat2Parser.put(ContentFormat.LZ4, jsonParser);
        contentFormat2Parser.put(ContentFormat.SNAPPY, jsonParser);
        contentFormat2Parser.put(ContentFormat.GZIP, jsonParser);
    }

    /** The Constant contentFormat2Type. */
    private static final Map<ContentFormat, String> contentFormat2Type = new EnumMap<ContentFormat, String>(ContentFormat.class);

    static {
        contentFormat2Type.put(ContentFormat.XML, HttpHeaders.Values.APPLICATION_XML);
        contentFormat2Type.put(ContentFormat.XML_LZ4, HttpHeaders.Values.APPLICATION_XML);
        contentFormat2Type.put(ContentFormat.XML_SNAPPY, HttpHeaders.Values.APPLICATION_XML);
        contentFormat2Type.put(ContentFormat.XML_GZIP, HttpHeaders.Values.APPLICATION_XML);
        contentFormat2Type.put(ContentFormat.JSON, HttpHeaders.Values.APPLICATION_JSON);
        contentFormat2Type.put(ContentFormat.JSON_LZ4, HttpHeaders.Values.APPLICATION_JSON);
        contentFormat2Type.put(ContentFormat.JSON_SNAPPY, HttpHeaders.Values.APPLICATION_JSON);
        contentFormat2Type.put(ContentFormat.JSON_GZIP, HttpHeaders.Values.APPLICATION_JSON);
        contentFormat2Type.put(ContentFormat.KRYO, HttpHeaders.Values.APPLICATION_KRYO);
    }

    /** The Constant contentFormat2Encoding. */
    private static final Map<ContentFormat, String> contentFormat2Encoding = new EnumMap<ContentFormat, String>(ContentFormat.class);

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

    /** The Constant contentTypeEncoding2Format. */
    private static final Map<String, Map<String, ContentFormat>> contentTypeEncoding2Format = new ObjectPool<String, Map<String, ContentFormat>>(64);

    static {
        for (Map.Entry<ContentFormat, String> entry : contentFormat2Type.entrySet()) {
            Map<String, ContentFormat> contentEncoding2Format = contentTypeEncoding2Format.get(entry.getValue());

            if (contentEncoding2Format == null) {
                contentEncoding2Format = new HashMap<String, ContentFormat>();
                contentTypeEncoding2Format.put(entry.getValue(), contentEncoding2Format);
            }

            if (entry.getKey().name().contains("GZIP")) {
                contentEncoding2Format.put(GZIP, entry.getKey());
            } else if (entry.getKey().name().contains("SNAPPY")) {
                contentEncoding2Format.put(SNAPPY, entry.getKey());
            } else if (entry.getKey().name().contains("LZ4")) {
                contentEncoding2Format.put(LZ4, entry.getKey());
            } else if (entry.getKey().name().contains("KRYO")) {
                contentEncoding2Format.put(KRYO, entry.getKey());
                contentEncoding2Format.put(N.EMPTY_STRING, entry.getKey());
            } else {
                contentEncoding2Format.put(N.EMPTY_STRING, entry.getKey());
            }
        }

        Map<String, ContentFormat> contentEncoding2Format = contentTypeEncoding2Format.get(N.EMPTY_STRING);

        if (contentEncoding2Format == null) {
            contentEncoding2Format = new HashMap<String, ContentFormat>();
            contentTypeEncoding2Format.put(N.EMPTY_STRING, contentEncoding2Format);
        }

        contentEncoding2Format.put(GZIP, ContentFormat.GZIP);
        contentEncoding2Format.put(SNAPPY, ContentFormat.SNAPPY);
        contentEncoding2Format.put(LZ4, ContentFormat.LZ4);
        contentEncoding2Format.put(KRYO, ContentFormat.KRYO);
        contentEncoding2Format.put(N.EMPTY_STRING, ContentFormat.NONE);
    }

    /**
     * Gets the content type.
     *
     * @param contentFormat the content format
     * @return the content type
     */
    public static String getContentType(final ContentFormat contentFormat) {
        if (contentFormat == null || contentFormat == ContentFormat.NONE) {
            return null;
        }

        return contentFormat2Type.get(contentFormat);
    }

    /**
     * Gets the content encoding.
     *
     * @param contentFormat the content format
     * @return the content encoding
     */
    public static String getContentEncoding(final ContentFormat contentFormat) {
        if (contentFormat == null || contentFormat == ContentFormat.NONE) {
            return null;
        }

        return contentFormat2Encoding.get(contentFormat);
    }

    /**
     * Gets the content format.
     *
     * @param contentType the content type
     * @param contentEncoding the content encoding
     * @return the content format
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
            if (contentType.contains("json")) {
                contentEncoding2Format = contentTypeEncoding2Format.get(HttpHeaders.Values.APPLICATION_JSON);
            } else if (contentType.contains("xml")) {
                contentEncoding2Format = contentTypeEncoding2Format.get(HttpHeaders.Values.APPLICATION_XML);
            } else if (contentType.contains("kryo")) {
                contentEncoding2Format = contentTypeEncoding2Format.get(HttpHeaders.Values.APPLICATION_KRYO);
            } else {
                contentEncoding2Format = contentTypeEncoding2Format.get(N.EMPTY_STRING);
            }
        }

        return N.defaultIfNull(contentEncoding2Format.get(contentEncoding), ContentFormat.NONE);
    }

    /**
     * Gets the content format.
     *
     * @param connection the connection
     * @return the content format
     */
    public static ContentFormat getContentFormat(final HttpURLConnection connection) {
        return getContentFormat(connection.getHeaderField(HttpHeaders.Names.CONTENT_TYPE), connection.getHeaderField(HttpHeaders.Names.CONTENT_ENCODING));
    }

    /**
     * Gets the parser.
     *
     * @param <SC> the generic type
     * @param <DC> the generic type
     * @param contentFormat the content format
     * @return the parser
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
     * @param is the is
     * @param contentFormat the content format
     * @return the input stream
     */
    public static InputStream wrapInputStream(final InputStream is, final ContentFormat contentFormat) {
        if (contentFormat == null || contentFormat == ContentFormat.NONE || is == null) {
            return is;
        }

        if (contentFormat.name().contains("GZIP")) {
            return IOUtil.newGZIPInputStream(is);
        } else if (contentFormat.name().contains("SNAPPY")) {
            return IOUtil.newSnappyInputStream(is);
        } else if (contentFormat.name().contains("LZ4")) {
            return IOUtil.newLZ4BlockInputStream(is);
        } else {
            return is;
        }
    }

    /**
     * Wrap output stream.
     *
     * @param os the os
     * @param contentFormat the content format
     * @return the output stream
     */
    public static OutputStream wrapOutputStream(final OutputStream os, final ContentFormat contentFormat) {
        if (contentFormat == null || contentFormat == ContentFormat.NONE || os == null) {
            return os;
        }

        if (contentFormat.name().contains("GZIP")) {
            return IOUtil.newGZIPOutputStream(os);
        } else if (contentFormat.name().contains("SNAPPY")) {
            return IOUtil.newSnappyOutputStream(os);
        } else if (contentFormat.name().contains("LZ4")) {
            return IOUtil.newLZ4BlockOutputStream(os);
        } else {
            return os;
        }
    }

    /**
     * Gets the output stream.
     *
     * @param connection the connection
     * @param contentFormat the content format
     * @return the output stream
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static OutputStream getOutputStream(final HttpURLConnection connection, final ContentFormat contentFormat) throws IOException {
        return getOutputStream(connection, contentFormat, HTTP.getContentType(contentFormat), HTTP.getContentEncoding(contentFormat));
    }

    /**
     * Gets the output stream.
     *
     * @param connection the connection
     * @param contentFormat the content format
     * @param contentType the content type
     * @param contentEncoding the content encoding
     * @return the output stream
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
     * @param connection the connection
     * @return the input stream
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static InputStream getInputStream(final HttpURLConnection connection) throws IOException {
        return getInputStream(connection, getContentFormat(connection));
    }

    /**
     * Gets the input stream.
     *
     * @param connection the connection
     * @param contentFormat the content format
     * @return the input stream
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static InputStream getInputStream(final HttpURLConnection connection, ContentFormat contentFormat) throws IOException {
        return wrapInputStream(connection.getInputStream(), contentFormat);
    }

    /**
     * Gets the input or error stream.
     *
     * @param connection the connection
     * @return the input or error stream
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static InputStream getInputOrErrorStream(final HttpURLConnection connection) throws IOException {
        return getInputOrErrorStream(connection, getContentFormat(connection));
    }

    /**
     * Gets the input or error stream.
     *
     * @param connection the connection
     * @param contentFormat the content format
     * @return the input or error stream
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static InputStream getInputOrErrorStream(final HttpURLConnection connection, ContentFormat contentFormat) throws IOException {
        try {
            return N.defaultIfNull(wrapInputStream(connection.getInputStream(), contentFormat), N.emptyInputStream());
        } catch (IOException e) {
            return N.defaultIfNull(wrapInputStream(connection.getErrorStream(), contentFormat), N.emptyInputStream());
        }
    }

    /**
     * Flush.
     *
     * @param os the os
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

    /**
     * Gets the charset.
     *
     * @param headers the headers
     * @return the charset
     */
    public static Charset getCharset(HttpHeaders headers) {
        Charset charset = Charsets.UTF_8;

        if (headers != null && headers.headerNameSet().contains(HttpHeaders.Names.CONTENT_TYPE)) {
            String contentType = N.stringOf(headers.get(HttpHeaders.Names.CONTENT_TYPE));

            if (N.notNullOrEmpty(contentType) && contentType.indexOf("charset=") >= 0) {
                charset = getCharset(contentType);
            }
        }

        return charset;
    }

    /**
     * Gets the charset.
     *
     * @param headers the headers
     * @return the charset
     */
    public static Charset getCharset(Map<String, ?> headers) {
        Charset charset = Charsets.UTF_8;

        if (headers != null && headers.containsKey(HttpHeaders.Names.CONTENT_TYPE)) {
            final Object values = headers.get(HttpHeaders.Names.CONTENT_TYPE);

            if (values instanceof Collection) {
                for (String contentType : ((Collection<String>) values)) {
                    if (N.notNullOrEmpty(contentType) && contentType.indexOf("charset=") >= 0) {
                        charset = getCharset(contentType);
                        break;
                    }
                }
            } else {
                final String str = N.stringOf(values);

                if (N.notNullOrEmpty(str) && str.indexOf("charset=") >= 0) {
                    charset = getCharset(str);
                }
            }
        }

        return charset;
    }

    /**
     * Gets the charset.
     *
     * @param contentType the content type
     * @return the charset
     */
    public static Charset getCharset(String contentType) {
        if (N.notNullOrEmpty(contentType)) {
            return Charsets.UTF_8;
        }

        int fromIndex = contentType.indexOf("charset=");
        int toIndex = contentType.indexOf(';', fromIndex);

        return Charsets.get(contentType.substring(fromIndex + "charset=".length(), toIndex > 0 ? toIndex : contentType.length()));
    }
}
