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
 *   https://www.apache.org/licenses/LICENSE-2.0
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;

/**
 * <p>
 * Note: it's copied from Apache HttpComponents developed at <a href="http://www.apache.org/">The Apache Software Foundation</a>, or
 * under the Apache License 2.0. The methods copied from other products/frameworks may be modified in this class.
 * </p>
 *
 * A collection of utilities for encoding URLs.
 *
 */
public final class URLEncodedUtil {
    /**
     * The constant representing the ampersand character ('&') used as a separator in URL query parameters.
     */
    public static final char QP_SEP_A = '&';

    /**
     * The constant representing the semicolon character (';') used as a separator in URL query parameters.
     */
    public static final char QP_SEP_S = ';';

    /**
     * The constant representing the equals character ('=') used as a name-value separator in URL query parameters.
     */
    public static final String NAME_VALUE_SEPARATOR = "=";

    /**
     * urlQuery parameter separators.
     */
    private static final char[] QP_SEPS = { QP_SEP_A, QP_SEP_S };

    /**
     * urlQuery parameter separator pattern.
     */
    private static final String QP_SEP_PATTERN = "[" + String.valueOf(QP_SEPS) + "]";

    /**
     * Unreserved characters, i.e., alphanumeric, plus: {@code _ - ! . ~ ' ( ) *}
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
     * Characters which are safe to use in userinfo, i.e., {@link #UNRESERVED} plus {@link #PUNCT} ration
     */
    private static final BitSet USERINFO = new BitSet(256);

    /**
     * Characters which are safe to use in a path, i.e., {@link #UNRESERVED} plus {@link #PUNCT} ration plus / @
     */
    private static final BitSet PATH_SAFE = new BitSet(256);

    /**
     * Characters which are safe to use in a urlQuery or a fragment, i.e., {@link #RESERVED} plus {@link #UNRESERVED}
     */
    private static final BitSet URIC = new BitSet(256);

    /**
     * Reserved characters, i.e., {@code ;/?:@&=+$,[]}
     * <p>
     * This list is the same as the {@code reserved} list in <a href="http://www.ietf.org/rfc/rfc2396.txt">RFC 2396</a>
     * as augmented by <a href="http://www.ietf.org/rfc/rfc2732.txt">RFC 2732</a>
     */
    private static final BitSet RESERVED = new BitSet(256);

    /**
     * Safe characters for x-www-form-urlencoded data, as per java.net.URLEncoder and browser behaviour, i.e.
     * alphanumeric plus {@code "-", "_", ".", "*"}
     */
    private static final BitSet URL_ENCODER = new BitSet(256);

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

        UNRESERVED.set('_'); // these are the characters of the "mark" list
        UNRESERVED.set('-');
        UNRESERVED.set('.');
        UNRESERVED.set('*');
        URL_ENCODER.or(UNRESERVED); // skip remaining unreserved characters
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
        PATH_SAFE.or(UNRESERVED);
        PATH_SAFE.set('/'); // segment separator
        PATH_SAFE.set(';'); // param separator
        PATH_SAFE.set(':'); // rest as per list in 2396, i.e., : @ & = + $ ,
        PATH_SAFE.set('@');
        PATH_SAFE.set('&');
        PATH_SAFE.set('=');
        PATH_SAFE.set('+');
        PATH_SAFE.set('$');
        PATH_SAFE.set(',');

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

    /**
     * Decodes a URL query string into a Map using the default charset.
     * The keys and values in the Map are the parameter names and values from the URL query string.
     *
     * @param urlQuery The URL query string to decode.
     * @return A Map containing parameter names as keys and parameter values as values.
     */
    public static Map<String, String> decode(final String urlQuery) {
        return decode(urlQuery, Charsets.DEFAULT);
    }

    /**
     * Decodes a URL query string into a Map using the provided charset.
     * The keys and values in the Map are the parameter names and values from the URL query string.
     *
     * @param urlQuery The URL query string to decode.
     * @param charset The charset to use for decoding the URL query string.
     * @return A Map containing parameter names as keys and parameter values as values.
     */
    public static Map<String, String> decode(final String urlQuery, final Charset charset) {
        final Map<String, String> result = new LinkedHashMap<>();

        if (Strings.isEmpty(urlQuery)) {
            return result;
        }

        try (final Scanner scanner = new Scanner(urlQuery)) {
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
        }

        return result;
    }

    /**
     * Decodes a URL query string into a ListMultimap using the default charset.
     * The keys and values in the ListMultimap are the parameter names and values from the URL query string.
     * If a parameter name appears multiple times in the query string, all its values will be stored in the ListMultimap.
     *
     * @param urlQuery The URL query string to decode.
     * @return A ListMultimap containing parameter names as keys and parameter values as values.
     */
    public static ListMultimap<String, String> decodeToMultimap(final String urlQuery) {
        return decodeToMultimap(urlQuery, Charsets.DEFAULT);
    }

    /**
     * Decodes a URL query string into a ListMultimap using the provided charset.
     * The keys and values in the ListMultimap are the parameter names and values from the URL query string.
     * If a parameter name appears multiple times in the query string, all its values will be stored in the ListMultimap.
     *
     * @param urlQuery The URL query string to decode.
     * @param charset The charset to use for decoding the URL query string.
     * @return A ListMultimap containing parameter names as keys and parameter values as values.
     */
    public static ListMultimap<String, String> decodeToMultimap(final String urlQuery, final Charset charset) {
        final ListMultimap<String, String> result = N.newLinkedListMultimap();

        if (Strings.isEmpty(urlQuery)) {
            return result;
        }

        try (final Scanner scanner = new Scanner(urlQuery)) {
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
        }

        return result;
    }

    /**
     * Decodes a URL query string into a bean of the specified type using the default charset.
     * The keys and values in the object are the parameter names and values from the URL query string.
     *
     * @param <T> The type of the object to decode into.
     * @param urlQuery The URL query string to decode.
     * @param targetType The bean class of the object to decode into.
     * @return An object of type T containing parameter names as keys and parameter values as values.
     */
    public static <T> T decode(final String urlQuery, final Class<? extends T> targetType) {
        return decode(urlQuery, Charsets.DEFAULT, targetType);
    }

    /**
     * Decodes a URL query string into a bean of the specified type using the provided charset.
     * The keys and values in the object are the parameter names and values from the URL query string.
     *
     * @param <T> The type of the object to decode into.
     * @param urlQuery The URL query string to decode.
     * @param charset The charset to use for decoding the URL query string.
     * @param targetType The bean class of the object to decode into.
     * @return An object of type T containing parameter names as keys and parameter values as values.
     */
    public static <T> T decode(final String urlQuery, final Charset charset, final Class<? extends T> targetType) {
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(targetType);
        final Object result = beanInfo.createBeanResult();

        if (Strings.isEmpty(urlQuery)) {
            return beanInfo.finishBeanResult(result);
        }

        try (final Scanner scanner = new Scanner(urlQuery)) {
            scanner.useDelimiter(QP_SEP_PATTERN);

            PropInfo propInfo = null;
            Object propValue = null;
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

                propInfo = beanInfo.getPropInfo(name);

                if (value == null) {
                    propValue = propInfo.jsonXmlType.defaultValue();
                } else {
                    propValue = propInfo.readPropValue(value);
                }

                beanInfo.setPropValue(result, name, propValue);
            }
        }

        return beanInfo.finishBeanResult(result);
    }

    /**
     * Converts a Map of parameters into a bean of the specified type.
     * The keys in the Map should correspond to the property names of the bean.
     * The values in the Map should be String arrays, where each String in the array is a value for the corresponding property in the bean.
     * If a property in the bean corresponds to multiple values in the Map, the property in the bean should be an array or a collection.
     *
     * @param <T> The type of the bean to create.
     * @param parameters The Map of parameters to convert into a bean.
     * @param targetType The class of the bean to create.
     * @return A bean of type T with its properties set to the values from the Map.
     */
    public static <T> T parameters2Bean(final Map<String, String[]> parameters, final Class<? extends T> targetType) {
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(targetType);
        final Object result = beanInfo.createBeanResult();

        if (N.isEmpty(parameters)) {
            return beanInfo.finishBeanResult(result);
        }

        PropInfo propInfo = null;
        Object propValue = null;
        String[] values = null;

        for (final Map.Entry<String, String[]> entry : parameters.entrySet()) { //NOSONAR
            propInfo = beanInfo.getPropInfo(entry.getKey());
            values = entry.getValue();

            if (N.isEmpty(values) || (values.length == 1 && Strings.isEmpty(values[0]))) {
                propValue = propInfo.jsonXmlType.defaultValue();
            } else {
                if (propInfo.jsonXmlType.clazz().equals(String[].class)) {
                    propValue = values;
                } else {
                    propValue = propInfo.readPropValue(Strings.join(values, ", "));
                }
            }

            propInfo.setPropValue(result, propValue);
        }

        return beanInfo.finishBeanResult(result);
    }

    /**
     * Encodes the provided parameters into a URL query string using the default charset.
     * The parameters can be a Map, a bean class with getter/setter methods, or an array of property name and value pairs.
     *
     * @param parameters The parameters to be encoded.
     * @return A URL query string with the encoded parameters.
     */
    public static String encode(final Object parameters) {
        return encode(parameters, Charsets.DEFAULT);
    }

    /**
     * Encodes the provided parameters into a URL query string using the specified charset.
     * The parameters can be a Map, a bean class with getter/setter methods, or an array of property name and value pairs.
     *
     * @param parameters The parameters to be encoded.
     * @param charset The charset to use for encoding.
     * @return A URL query string with the encoded parameters.
     */
    public static String encode(final Object parameters, final Charset charset) {
        return encode(parameters, charset, NamingPolicy.LOWER_CAMEL_CASE);
    }

    /**
     * Encodes the provided parameters into a URL query string using the specified charset and naming policy.
     * The parameters can be a Map, a bean class with getter/setter methods, or an array of property name and value pairs.
     *
     * @param parameters The parameters to be encoded.
     * @param charset The charset to use for encoding.
     * @param namingPolicy The naming policy to be used for property names during encoding.
     * @return A URL query string with the encoded parameters.
     */
    public static String encode(final Object parameters, final Charset charset, final NamingPolicy namingPolicy) {
        if (parameters == null) {
            return Strings.EMPTY;
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            encode(parameters, charset, namingPolicy, sb);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Encodes the provided parameters into a URL query string using the default charset.
     * The parameters can be a String URL and an Object which can be a Map, a bean class with getter/setter methods, or an array of property name and value pairs.
     *
     * @param url The URL to which the encoded parameters will be appended.
     * @param parameters The parameters to be encoded.
     * @return A URL query string with the encoded parameters appended to the provided URL.
     */
    public static String encode(final String url, final Object parameters) {
        return encode(url, parameters, Charsets.DEFAULT);
    }

    /**
     * Encodes the provided parameters into a URL query string using the specified charset.
     * The parameters can be a String URL and an Object which can be a Map, a bean class with getter/setter methods, or an array of property name and value pairs.
     *
     * @param url The URL to which the encoded parameters will be appended.
     * @param parameters The parameters to be encoded.
     * @param charset The charset to use for encoding.
     * @return A URL query string with the encoded parameters appended to the provided URL.
     */
    public static String encode(final String url, final Object parameters, final Charset charset) {
        return encode(url, parameters, charset, NamingPolicy.LOWER_CAMEL_CASE);
    }

    /**
     * Encodes the provided parameters into a URL query string using the specified charset and naming policy.
     * The parameters can be a String URL and an Object which can be a Map, a bean class with getter/setter methods, or an array of property name and value pairs.
     *
     * @param url The URL to which the encoded parameters will be appended.
     * @param parameters The parameters to be encoded.
     * @param charset The charset to use for encoding.
     * @param namingPolicy The naming policy to be used for property names during encoding.
     * @return A URL query string with the encoded parameters appended to the provided URL.
     */
    @SuppressWarnings("rawtypes")
    public static String encode(final String url, final Object parameters, final Charset charset, final NamingPolicy namingPolicy) {
        if (parameters == null || (parameters instanceof Map && ((Map) parameters).isEmpty())) {
            return url;
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            sb.append(url);
            sb.append(WD._QUESTION_MARK);
            encode(parameters, charset, namingPolicy, sb);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Encodes the provided parameters into a URL query string using the default charset.
     * The parameters can be a Map, a bean class with getter/setter methods, or an array of property name and value pairs.
     * The encoded URL query string is appended to the provided Appendable.
     *
     * @param parameters The parameters to be encoded.
     * @param output The Appendable where the encoded URL query string will be appended.
     */
    public static void encode(final Object parameters, final Appendable output) {
        encode(parameters, Charsets.DEFAULT, output);
    }

    /**
     * Encodes the provided parameters into a URL query string using the specified charset.
     * The parameters can be a Map, a bean class with getter/setter methods, or an array of property name and value pairs.
     * The encoded URL query string is appended to the provided Appendable.
     *
     * @param parameters The parameters to be encoded.
     * @param charset The charset to use for encoding.
     * @param output The Appendable where the encoded URL query string will be appended.
     */
    public static void encode(final Object parameters, final Charset charset, final Appendable output) {
        encode(parameters, charset, NamingPolicy.LOWER_CAMEL_CASE, output);
    }

    /**
     * Encodes the provided parameters into a URL query string using the specified charset and naming policy.
     * The parameters can be a Map, a bean class with getter/setter methods, or an array of property name and value pairs.
     * The encoded URL query string is appended to the provided Appendable.
     *
     * @param parameters The parameters to be encoded.
     * @param charset The charset to be used for encoding.
     * @param namingPolicy The naming policy to be used for property names during encoding.
     * @param output The Appendable where the encoded URL query string will be appended.
     */
    @SuppressWarnings("rawtypes")
    public static void encode(final Object parameters, final Charset charset, final NamingPolicy namingPolicy, final Appendable output)
            throws UncheckedIOException {
        if (parameters == null || (parameters instanceof Map && ((Map) parameters).isEmpty())) {
            return;
        }

        final boolean isNoChange = namingPolicy == null || namingPolicy == NamingPolicy.NO_CHANGE;

        try {
            if (parameters instanceof Map) {
                final Map<String, Object> map = (Map<String, Object>) parameters;
                int i = 0;
                for (final Map.Entry<String, Object> entry : map.entrySet()) {
                    if (i++ > 0) {
                        output.append(QP_SEP_A);
                    }

                    if (isNoChange) {
                        encodeFormFields(entry.getKey(), charset, output);
                    } else {
                        encodeFormFields(namingPolicy.convert(entry.getKey()), charset, output);
                    }

                    output.append(NAME_VALUE_SEPARATOR);

                    encodeFormFields(N.stringOf(entry.getValue()), charset, output);
                }
            } else if (ClassUtil.isBeanClass(parameters.getClass())) {
                encode(Maps.bean2Map(parameters, true, null, namingPolicy), charset, NamingPolicy.NO_CHANGE, output);
            } else if (parameters instanceof Object[] a) {
                if (0 != (a.length % 2)) {
                    throw new IllegalArgumentException(
                            "The parameters must be the pairs of property name and value, or Map, or a bean class with getter/setter methods.");
                }

                for (int i = 0, len = a.length; i < len; i++) {
                    if (i > 0) {
                        output.append(QP_SEP_A);
                    }

                    if (isNoChange) {
                        encodeFormFields((String) a[i], charset, output);
                    } else {
                        encodeFormFields(namingPolicy.convert((String) a[i]), charset, output);
                    }

                    output.append(NAME_VALUE_SEPARATOR);

                    encodeFormFields(N.stringOf(a[++i]), charset, output);
                }
            } else {
                encodeFormFields(N.stringOf(parameters), charset, output);
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
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

        return urlDecode(content, (charset != null) ? charset : Charsets.DEFAULT, true);
    }

    /**
     * Decode/unescape a portion of a URL, to use with the urlQuery part ensure {@code plusAsBlank} is {@code true}.
     *
     * @param content
     *            the portion to decode
     * @param charset
     *            the charset to use
     * @param plusAsBlank
     *            if {@code true}, then convert '+' to space (e.g., for www-url-form-encoded content), otherwise leave as
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
     *
     * @param content the content to encode, will convert space to '+'
     * @param charset the charset to use
     * @param output
     *
     * @return encoded string
     * @throws IOException
     */
    private static void encodeFormFields(final String content, final Charset charset, final Appendable output) throws IOException {
        urlEncode(content, (charset != null) ? charset : Charsets.DEFAULT, URL_ENCODER, true, output);
    }

    private static void urlEncode(final String content, final Charset charset, final BitSet safeChars, final boolean blankAsPlus, final Appendable output)
            throws IOException {
        if (content == null) {
            output.append(Strings.NULL);

            return;
        }

        final ByteBuffer bb = charset.encode(content);

        while (bb.hasRemaining()) {
            final int b = bb.get() & 0xff;

            if (safeChars.get(b)) {
                output.append((char) b);
            } else if (blankAsPlus && (b == ' ')) {
                output.append('+');
            } else {
                output.append('%');

                final char hex1 = Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, RADIX));
                final char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, RADIX));
                output.append(hex1);
                output.append(hex2);
            }
        }
    }

    /**
     * Encode a String using the {@link #USERINFO} set of characters.
     * <p>
     * Used by URIBuilder to encode the userinfo segment.
     * @param content the string to encode, does not convert space to '+'
     * @param charset the charset to use
     * @param output
     *
     * @return
     * @throws IOException
     */
    static void encUserInfo(final String content, final Charset charset, final Appendable output) throws IOException {
        urlEncode(content, charset, USERINFO, false, output);
    }

    /**
     * Encode a String using the {@link #URIC} set of characters.
     * <p>
     * Used by URIBuilder to encode the urlQuery and fragment segments.
     * @param content the string to encode, does not convert space to '+'
     * @param charset the charset to use
     * @param output
     *
     * @return
     * @throws IOException
     */
    static void encUric(final String content, final Charset charset, final Appendable output) throws IOException {
        urlEncode(content, charset, URIC, false, output);
    }

    /**
     * Encode a String using the {@link #PATH_SAFE} set of characters.
     * <p>
     * Used by URIBuilder to encode path segments.
     * @param content the string to encode, does not convert space to '+'
     * @param charset the charset to use
     * @param output
     *
     * @return
     * @throws IOException
     */
    static void encPath(final String content, final Charset charset, final Appendable output) throws IOException {
        urlEncode(content, charset, PATH_SAFE, false, output);
    }
}
