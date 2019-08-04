/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package com.landawn.abacus.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.EntityInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;

/**
 * <p>
 * Note: it's copied from Apache HttpComponents developed at The Apache Software Foundation (http://www.apache.org/), or
 * under the Apache License 2.0. The methods copied from other products/frameworks may be modified in this class.
 * </p>
 *
 * A collection of utilities for encoding URLs.
 *
 * @since 4.0
 */
public final class URLEncodedUtil {
    public static final char QP_SEP_A = '&';
    public static final char QP_SEP_S = ';';
    public static final String NAME_VALUE_SEPARATOR = "=";

    /**
     * urlQuery parameter separators.
     */
    private static final char[] QP_SEPS = new char[] { QP_SEP_A, QP_SEP_S };

    /**
     * urlQuery parameter separator pattern.
     */
    private static final String QP_SEP_PATTERN = "[" + new String(QP_SEPS) + "]";

    /**
     * Unreserved characters, i.e. alphanumeric, plus: {@code _ - ! . ~ ' ( ) *}
     * <p>
     * This list is the same as the {@code unreserved} list in <a href="http://www.ietf.org/rfc/rfc2396.txt">RFC
     * 2396</a>
     */
    private static final BitSet UNRESERVED = new BitSet(256);

    /**
     * Punctuation characters: , ; : $ & + =
     * <p>
     * These are the additional characters allowed by userinfo.
     */
    private static final BitSet PUNCT = new BitSet(256);

    /**
     * Characters which are safe to use in userinfo, i.e. {@link #UNRESERVED} plus {@link #PUNCT}uation
     */
    private static final BitSet USERINFO = new BitSet(256);

    /**
     * Characters which are safe to use in a path, i.e. {@link #UNRESERVED} plus {@link #PUNCT}uation plus / @
     */
    private static final BitSet PATHSAFE = new BitSet(256);

    /**
     * Characters which are safe to use in a urlQuery or a fragment, i.e. {@link #RESERVED} plus {@link #UNRESERVED}
     */
    private static final BitSet URIC = new BitSet(256);

    /**
     * Reserved characters, i.e. {@code ;/?:@&=+$,[]}
     * <p>
     * This list is the same as the {@code reserved} list in <a href="http://www.ietf.org/rfc/rfc2396.txt">RFC 2396</a>
     * as augmented by <a href="http://www.ietf.org/rfc/rfc2732.txt">RFC 2732</a>
     */
    private static final BitSet RESERVED = new BitSet(256);

    /**
     * Safe characters for x-www-form-urlencoded data, as per java.net.URLEncoder and browser behaviour, i.e.
     * alphanumeric plus {@code "-", "_", ".", "*"}
     */
    private static final BitSet URLENCODER = new BitSet(256);

    static {
        // unreserved chars
        // alpha characters
        for (int i = 'a'; i <= 'z'; i++) {
            UNRESERVED.set(i);
        }

        for (int i = 'A'; i <= 'Z'; i++) {
            UNRESERVED.set(i);
        }

        // numeric characters
        for (int i = '0'; i <= '9'; i++) {
            UNRESERVED.set(i);
        }

        UNRESERVED.set('_'); // these are the charactes of the "mark" list
        UNRESERVED.set('-');
        UNRESERVED.set('.');
        UNRESERVED.set('*');
        URLENCODER.or(UNRESERVED); // skip remaining unreserved characters
        UNRESERVED.set('!');
        UNRESERVED.set('~');
        UNRESERVED.set('\'');
        UNRESERVED.set('(');
        UNRESERVED.set(')');
        // punct chars
        PUNCT.set(',');
        PUNCT.set(';');
        PUNCT.set(':');
        PUNCT.set('$');
        PUNCT.set('&');
        PUNCT.set('+');
        PUNCT.set('=');
        // Safe for userinfo
        USERINFO.or(UNRESERVED);
        USERINFO.or(PUNCT);

        // URL path safe
        PATHSAFE.or(UNRESERVED);
        PATHSAFE.set('/'); // segment separator
        PATHSAFE.set(';'); // param separator
        PATHSAFE.set(':'); // rest as per list in 2396, i.e. : @ & = + $ ,
        PATHSAFE.set('@');
        PATHSAFE.set('&');
        PATHSAFE.set('=');
        PATHSAFE.set('+');
        PATHSAFE.set('$');
        PATHSAFE.set(',');

        RESERVED.set(';');
        RESERVED.set('/');
        RESERVED.set('?');
        RESERVED.set(':');
        RESERVED.set('@');
        RESERVED.set('&');
        RESERVED.set('=');
        RESERVED.set('+');
        RESERVED.set('$');
        RESERVED.set(',');
        RESERVED.set('['); // added by RFC 2732
        RESERVED.set(']'); // added by RFC 2732

        URIC.or(RESERVED);
        URIC.or(UNRESERVED);
    }

    private static final int RADIX = 16;

    private URLEncodedUtil() {
        // singleton.
    }

    public static Map<String, String> decode(final String urlQuery) {
        return decode(urlQuery, Charsets.UTF_8);
    }

    public static Map<String, String> decode(final String urlQuery, final Charset charset) {
        final Map<String, String> result = new LinkedHashMap<>();

        if (N.isNullOrEmpty(urlQuery)) {
            return result;
        }

        final Scanner scanner = new Scanner(urlQuery);
        scanner.useDelimiter(QP_SEP_PATTERN);

        String name = null;
        String value = null;

        while (scanner.hasNext()) {
            final String token = scanner.next();
            final int i = token.indexOf(NAME_VALUE_SEPARATOR);

            if (i != -1) {
                name = decodeFormFields(token.substring(0, i).trim(), charset);
                value = decodeFormFields(token.substring(i + 1).trim(), charset);
            } else {
                name = decodeFormFields(token.trim(), charset);
                value = null;
            }

            result.put(name, value);
        }

        scanner.close();

        return result;
    }

    public static <T> T decode(final Class<T> targetClass, final String urlQuery) {
        return decode(targetClass, urlQuery, Charsets.UTF_8);
    }

    public static <T> T decode(final Class<T> targetClass, final String urlQuery, final Charset charset) {
        final T result = N.newInstance(targetClass);

        if (N.isNullOrEmpty(urlQuery)) {
            return result;
        }

        final Scanner scanner = new Scanner(urlQuery);
        scanner.useDelimiter(QP_SEP_PATTERN);

        Type<?> propType = null;
        Object propValue = null;
        String name = null;
        String value = null;

        final EntityInfo entityInfo = ParserUtil.getEntityInfo(targetClass);

        try {
            while (scanner.hasNext()) {
                final String token = scanner.next();
                final int i = token.indexOf(NAME_VALUE_SEPARATOR);

                if (i != -1) {
                    name = decodeFormFields(token.substring(0, i).trim(), charset);
                    value = decodeFormFields(token.substring(i + 1).trim(), charset);
                } else {
                    name = decodeFormFields(token.trim(), charset);
                    value = null;
                }

                propType = entityInfo.getPropInfo(name).jsonXmlType;

                if (value == null) {
                    propValue = propType.defaultValue();
                } else {
                    propValue = propType.valueOf(value);
                }

                ClassUtil.setPropValue(result, name, propValue);
            }

        } finally {
            scanner.close();
        }

        return result;
    }

    public static <T> T parameters2Entity(final Class<T> targetClass, final Map<String, String[]> parameters) {
        final T result = N.newInstance(targetClass);

        if (N.isNullOrEmpty(parameters)) {
            return result;
        }

        final EntityInfo entityInfo = ParserUtil.getEntityInfo(targetClass);

        PropInfo propInfo = null;
        Object propValue = null;
        String[] values = null;

        for (String key : parameters.keySet()) {
            propInfo = entityInfo.getPropInfo(key);
            values = parameters.get(key);

            if (N.isNullOrEmpty(values)) {
                propValue = propInfo.jsonXmlType.defaultValue();
            } else {
                if (propInfo.jsonXmlType.clazz().equals(String[].class)) {
                    propValue = values;
                } else {
                    propValue = propInfo.jsonXmlType.valueOf(StringUtil.join(values, ", "));
                }
            }

            propInfo.setPropValue(result, propValue);
        }

        return result;
    }

    public static String encode(final Object parameters) {
        return encode(parameters, Charsets.UTF_8);
    }

    public static String encode(final Object parameters, final Charset charset) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            encode(sb, parameters, charset);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    public static String encode(final String url, final Object parameters) {
        return encode(url, parameters, Charsets.UTF_8);
    }

    @SuppressWarnings("rawtypes")
    public static String encode(final String url, final Object parameters, final Charset charset) {
        if (parameters == null || (parameters instanceof Map && ((Map) parameters).isEmpty())) {
            return url;
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            sb.append(url);
            sb.append(WD._QUESTION_MARK);
            encode(sb, parameters, charset);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    public static void encode(final StringBuilder output, final Object parameters) {
        encode(output, parameters, Charsets.UTF_8);
    }

    @SuppressWarnings("rawtypes")
    public static void encode(final StringBuilder output, final Object parameters, final Charset charset) {
        if (parameters == null || (parameters instanceof Map && ((Map) parameters).isEmpty())) {
            return;
        }

        if (parameters instanceof Map) {
            final Map<String, Object> map = (Map<String, Object>) parameters;
            int i = 0;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (i++ > 0) {
                    output.append(QP_SEP_A);
                }

                encodeFormFields(output, entry.getKey(), charset);

                output.append(NAME_VALUE_SEPARATOR);

                encodeFormFields(output, N.stringOf(entry.getValue()), charset);
            }
        } else if (ClassUtil.isEntity(parameters.getClass())) {
            encode(output, Maps.entity2Map(parameters, true), charset);
        } else if (parameters instanceof Object[]) {
            final Object[] a = (Object[]) parameters;

            if (0 != (a.length % 2)) {
                throw new IllegalArgumentException(
                        "The parameters must be the pairs of property name and value, or Map, or an entity class with getter/setter methods.");
            }

            for (int i = 0, len = a.length; i < len; i++) {
                if (i > 0) {
                    output.append(QP_SEP_A);
                }

                encodeFormFields(output, (String) a[i], charset);

                output.append(NAME_VALUE_SEPARATOR);

                encodeFormFields(output, N.stringOf(a[++i]), charset);
            }
        } else {
            encodeFormFields(output, N.stringOf(parameters), charset);
        }
    }

    /**
     * Decode/unescape www-url-form-encoded content.
     *
     * @param content
     *            the content to decode, will decode '+' as space
     * @param charset
     *            the charset to use
     * @return encoded string
     */
    private static String decodeFormFields(final String content, final Charset charset) {
        if (content == null) {
            return null;
        }

        return urlDecode(content, (charset != null) ? charset : Charsets.UTF_8, true);
    }

    /**
     * Decode/unescape a portion of a URL, to use with the urlQuery part ensure {@code plusAsBlank} is true.
     *
     * @param content
     *            the portion to decode
     * @param charset
     *            the charset to use
     * @param plusAsBlank
     *            if {@code true}, then convert '+' to space (e.g. for www-url-form-encoded content), otherwise leave as
     *            is.
     * @return encoded string
     */
    private static String urlDecode(final String content, final Charset charset, final boolean plusAsBlank) {
        if (content == null) {
            return null;
        }

        final ByteBuffer bb = ByteBuffer.allocate(content.length());
        final CharBuffer cb = CharBuffer.wrap(content);

        while (cb.hasRemaining()) {
            final char c = cb.get();

            if ((c == '%') && (cb.remaining() >= 2)) {
                final char uc = cb.get();
                final char lc = cb.get();
                final int u = Character.digit(uc, 16);
                final int l = Character.digit(lc, 16);

                if ((u != -1) && (l != -1)) {
                    bb.put((byte) ((u << 4) + l));
                } else {
                    bb.put((byte) '%');
                    bb.put((byte) uc);
                    bb.put((byte) lc);
                }
            } else if (plusAsBlank && (c == '+')) {
                bb.put((byte) ' ');
            } else {
                bb.put((byte) c);
            }
        }

        bb.flip();

        return charset.decode(bb).toString();
    }

    /**
     * Encode/escape www-url-form-encoded content.
     * <p>
     * Uses the {@link #URLENCODER} set of characters, rather than the {@link #UNRSERVED} set; this is for compatibilty
     * with previous releases, URLEncoder.encode() and most browsers.
     * @param content
     *            the content to encode, will convert space to '+'
     * @param charset
     *            the charset to use
     *
     * @return encoded string
     */
    private static void encodeFormFields(final StringBuilder sb, final String content, final Charset charset) {
        urlEncode(sb, content, (charset != null) ? charset : Charsets.UTF_8, URLENCODER, true);
    }

    private static void urlEncode(final StringBuilder sb, final String content, final Charset charset, final BitSet safechars, final boolean blankAsPlus) {
        if (content == null) {
            sb.append(N.NULL_STRING);

            return;
        }

        final ByteBuffer bb = charset.encode(content);

        while (bb.hasRemaining()) {
            final int b = bb.get() & 0xff;

            if (safechars.get(b)) {
                sb.append((char) b);
            } else if (blankAsPlus && (b == ' ')) {
                sb.append('+');
            } else {
                sb.append('%');

                final char hex1 = Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, RADIX));
                final char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, RADIX));
                sb.append(hex1);
                sb.append(hex2);
            }
        }
    }

    /**
     * Encode a String using the {@link #USERINFO} set of characters.
     * <p>
     * Used by URIBuilder to encode the userinfo segment.
     * @param content
     *            the string to encode, does not convert space to '+'
     * @param charset
     *            the charset to use
     *
     * @return the encoded string
     */
    static void encUserInfo(final StringBuilder sb, final String content, final Charset charset) {
        urlEncode(sb, content, charset, USERINFO, false);
    }

    /**
     * Encode a String using the {@link #URIC} set of characters.
     * <p>
     * Used by URIBuilder to encode the urlQuery and fragment segments.
     * @param content
     *            the string to encode, does not convert space to '+'
     * @param charset
     *            the charset to use
     *
     * @return the encoded string
     */
    static void encUric(final StringBuilder sb, final String content, final Charset charset) {
        urlEncode(sb, content, charset, URIC, false);
    }

    /**
     * Encode a String using the {@link #PATHSAFE} set of characters.
     * <p>
     * Used by URIBuilder to encode path segments.
     * @param content
     *            the string to encode, does not convert space to '+'
     * @param charset
     *            the charset to use
     *
     * @return the encoded string
     */
    static void encPath(final StringBuilder sb, final String content, final Charset charset) {
        urlEncode(sb, content, charset, PATHSAFE, false);
    }
}
