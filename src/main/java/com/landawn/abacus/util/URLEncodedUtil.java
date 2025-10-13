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
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Supplier;

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.util.Splitter.MapSplitter;

/**
 * <p>
 * Note: it's copied from Apache HttpComponents developed at <a href="http://www.apache.org/">The Apache Software Foundation</a>, or
 * under the Apache License 2.0. The methods copied from other products/frameworks may be modified in this class.
 * </p>
 *
 * A collection of utilities for encoding URLs.
 * @see URLEncoder
 * @see URLDecoder
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
    private static final MapSplitter PARAMS_SPLITTER = MapSplitter.with(String.valueOf(QP_SEP_A), NAME_VALUE_SEPARATOR).trimResults();

    private URLEncodedUtil() {
        // singleton.
    }

    /**
     * Decodes a URL-encoded query string into a {@code Map<String, String>} using the default charset (UTF-8).
     * <p>
     * This method parses a URL query string (e.g., "name=value&amp;foo=bar") and converts it into a map
     * where keys are parameter names and values are parameter values. Both '+' characters and '%XX' sequences
     * are decoded. Parameter names and values are trimmed of whitespace. If a parameter appears multiple times,
     * only the last occurrence is retained (use {@link #decodeToMultimap(String)} to preserve all values).
     * </p>
     * <p>
     * Supports both '&amp;' and ';' as parameter separators according to RFC 2396.
     * </p>
     *
     * <pre>
     * Map&lt;String, String&gt; params = URLEncodedUtil.decode("name=John+Doe&amp;age=30");
     * // params: {name=John Doe, age=30}
     * </pre>
     *
     * @param urlQuery the URL query string to decode (e.g., "key1=value1&amp;key2=value2"), may be {@code null} or empty
     * @return a {@code LinkedHashMap} containing parameter names as keys and decoded parameter values as values;
     *         returns an empty map if {@code urlQuery} is {@code null} or empty
     * @see #decode(String, Charset)
     * @see #decodeToMultimap(String)
     * @see URLDecoder#decode(String)
     */
    public static Map<String, String> decode(final String urlQuery) {
        return decode(urlQuery, Charsets.DEFAULT);
    }

    /**
     * Decodes a URL-encoded query string into a {@code Map<String, String>} using the specified charset.
     * <p>
     * This method parses a URL query string and converts it into a map where keys are parameter names
     * and values are parameter values. Both '+' characters and '%XX' sequences are decoded using the
     * specified charset. Parameter names and values are trimmed of whitespace. If a parameter appears
     * multiple times, only the last occurrence is retained.
     * </p>
     * <p>
     * Supports both '&amp;' and ';' as parameter separators according to RFC 2396.
     * </p>
     *
     * <pre>
     * Map&lt;String, String&gt; params = URLEncodedUtil.decode("name=%E4%B8%AD%E6%96%87", StandardCharsets.UTF_8);
     * // params: {name=中文}
     * </pre>
     *
     * @param urlQuery the URL query string to decode, may be {@code null} or empty
     * @param charset the charset to use for decoding percent-encoded characters; if {@code null}, defaults to UTF-8
     * @return a {@code LinkedHashMap} containing parameter names as keys and decoded parameter values as values;
     *         returns an empty map if {@code urlQuery} is {@code null} or empty
     * @see #decode(String)
     * @see #decode(String, Charset, Supplier)
     * @see URLDecoder#decode(String, Charset)
     */
    public static Map<String, String> decode(final String urlQuery, final Charset charset) {
        return decode(urlQuery, charset, Suppliers.of(LinkedHashMap::new));
    }

    /**
     * Decodes a URL-encoded query string into a custom {@code Map} implementation using the specified charset and map supplier.
     * <p>
     * This method provides flexibility in choosing the Map implementation (e.g., {@code TreeMap}, {@code HashMap}, etc.)
     * by accepting a custom supplier. Both '+' characters and '%XX' sequences are decoded using the specified charset.
     * Parameter names and values are trimmed of whitespace. If a parameter appears multiple times, only the last
     * occurrence is retained.
     * </p>
     * <p>
     * Supports both '&amp;' and ';' as parameter separators according to RFC 2396.
     * </p>
     *
     * <pre>
     * TreeMap&lt;String, String&gt; params = URLEncodedUtil.decode("b=2&amp;a=1", StandardCharsets.UTF_8, TreeMap::new);
     * // params: {a=1, b=2} (sorted by key)
     * </pre>
     *
     * @param <M> the type of the Map to return, must extend {@code Map<String, String>}
     * @param urlQuery the URL query string to decode, may be {@code null} or empty
     * @param charset the charset to use for decoding percent-encoded characters; if {@code null}, defaults to UTF-8
     * @param mapSupplier a supplier that provides an instance of the desired Map implementation
     * @return a Map of type M containing parameter names as keys and decoded parameter values as values;
     *         returns an empty map (from supplier) if {@code urlQuery} is {@code null} or empty
     * @see #decode(String, Charset)
     */
    public static <M extends Map<String, String>> M decode(final String urlQuery, final Charset charset, final Supplier<M> mapSupplier) {
        final M result = mapSupplier.get();

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
     * Decodes a URL-encoded query string into a {@code ListMultimap<String, String>} using the default charset (UTF-8).
     * <p>
     * This method parses a URL query string and converts it into a multimap where keys are parameter names
     * and values are lists of parameter values. Unlike {@link #decode(String)}, this method preserves all
     * values when a parameter appears multiple times in the query string. Both '+' characters and '%XX'
     * sequences are decoded. Parameter names and values are trimmed of whitespace.
     * </p>
     * <p>
     * Supports both '&amp;' and ';' as parameter separators according to RFC 2396.
     * </p>
     *
     * <pre>
     * ListMultimap&lt;String, String&gt; params = URLEncodedUtil.decodeToMultimap("color=red&amp;color=blue&amp;size=L");
     * // params: {color=[red, blue], size=[L]}
     * </pre>
     *
     * @param urlQuery the URL query string to decode (e.g., "color=red&amp;color=blue&amp;size=L"), may be {@code null} or empty
     * @return a {@code ListMultimap} containing parameter names as keys and lists of decoded parameter values;
     *         returns an empty multimap if {@code urlQuery} is {@code null} or empty
     * @see #decodeToMultimap(String, Charset)
     * @see #decode(String)
     */
    public static ListMultimap<String, String> decodeToMultimap(final String urlQuery) {
        return decodeToMultimap(urlQuery, Charsets.DEFAULT);
    }

    /**
     * Decodes a URL-encoded query string into a {@code ListMultimap<String, String>} using the specified charset.
     * <p>
     * This method parses a URL query string and converts it into a multimap where keys are parameter names
     * and values are lists of parameter values. Unlike {@link #decode(String, Charset)}, this method preserves
     * all values when a parameter appears multiple times in the query string. Both '+' characters and '%XX'
     * sequences are decoded using the specified charset. Parameter names and values are trimmed of whitespace.
     * </p>
     * <p>
     * Supports both '&amp;' and ';' as parameter separators according to RFC 2396.
     * </p>
     *
     * <pre>
     * ListMultimap&lt;String, String&gt; params = URLEncodedUtil.decodeToMultimap("tag=java&amp;tag=url", StandardCharsets.UTF_8);
     * // params: {tag=[java, url]}
     * </pre>
     *
     * @param urlQuery the URL query string to decode, may be {@code null} or empty
     * @param charset the charset to use for decoding percent-encoded characters; if {@code null}, defaults to UTF-8
     * @return a {@code ListMultimap} containing parameter names as keys and lists of decoded parameter values;
     *         returns an empty multimap if {@code urlQuery} is {@code null} or empty
     * @see #decodeToMultimap(String)
     * @see #decode(String, Charset)
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
     * Decodes a URL-encoded query string into a bean or Map of the specified type using the default charset (UTF-8).
     * <p>
     * This method parses a URL query string and populates a JavaBean or Map instance with the decoded parameters.
     * Parameter names are matched to bean property names (case-sensitive). Values are automatically converted
     * to the appropriate property types using the bean's property information. Both '+' characters and '%XX'
     * sequences are decoded. Parameter names and values are trimmed of whitespace.
     * </p>
     * <p>
     * Supports both '&amp;' and ';' as parameter separators according to RFC 2396.
     * </p>
     *
     * <pre>
     * class User { String name; int age; }
     * User user = URLEncodedUtil.decode("name=John&amp;age=30", User.class);
     * // user: {name="John", age=30}
     * </pre>
     *
     * @param <T> the type of the object to decode into (JavaBean or Map)
     * @param urlQuery the URL query string to decode, may be {@code null} or empty
     * @param targetType the class of the bean or Map to create and populate
     * @return an instance of type T populated with the decoded parameter values;
     *         returns an empty instance if {@code urlQuery} is {@code null} or empty
     * @see #decode(String, Charset, Class)
     */
    public static <T> T decode(final String urlQuery, final Class<? extends T> targetType) {
        return decode(urlQuery, Charsets.DEFAULT, targetType);
    }

    /**
     * Decodes a URL-encoded query string into a bean or Map of the specified type using the specified charset.
     * <p>
     * This method parses a URL query string and populates a JavaBean or Map instance with the decoded parameters.
     * Parameter names are matched to bean property names (case-sensitive). Values are automatically converted
     * to the appropriate property types using the bean's property information. Both '+' characters and '%XX'
     * sequences are decoded using the specified charset. Parameter names and values are trimmed of whitespace.
     * </p>
     * <p>
     * If {@code targetType} is a Map class, this method delegates to {@link #decode(String, Charset, Supplier)}.
     * Otherwise, it creates and populates a bean instance.
     * </p>
     * <p>
     * Supports both '&amp;' and ';' as parameter separators according to RFC 2396.
     * </p>
     *
     * <pre>
     * class Product { String name; double price; }
     * Product p = URLEncodedUtil.decode("name=Laptop&amp;price=999.99", StandardCharsets.UTF_8, Product.class);
     * // p: {name="Laptop", price=999.99}
     * </pre>
     *
     * @param <T> the type of the object to decode into (JavaBean or Map)
     * @param urlQuery the URL query string to decode, may be {@code null} or empty
     * @param charset the charset to use for decoding percent-encoded characters; if {@code null}, defaults to UTF-8
     * @param targetType the class of the bean or Map to create and populate
     * @return an instance of type T populated with the decoded parameter values;
     *         returns an empty instance if {@code urlQuery} is {@code null} or empty
     * @see #decode(String, Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> T decode(final String urlQuery, final Charset charset, final Class<? extends T> targetType) {
        if (Map.class.isAssignableFrom(targetType)) {
            final Supplier<Map<String, String>> supplier = Suppliers.ofMap((Class) targetType);

            return (T) decode(urlQuery, charset, supplier);
        }

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
     * Converts a parameter map (with String array values) into a bean of the specified type.
     * <p>
     * This method is typically used to convert HTTP servlet request parameters into a JavaBean.
     * The keys in the map should correspond to bean property names (case-sensitive). Values are String arrays,
     * where each array contains one or more values for the corresponding property. If a property has multiple
     * values and the target property type is {@code String[]}, all values are set. Otherwise, values are joined
     * with ", " and converted to the property type.
     * </p>
     * <p>
     * If a parameter value is {@code null}, empty, or contains only a single empty string, the property is set
     * to its default value (as determined by the property type).
     * </p>
     *
     * <pre>
     * Map&lt;String, String[]&gt; params = new HashMap&lt;&gt;();
     * params.put("name", new String[]{"Alice"});
     * params.put("tags", new String[]{"java", "coding"});
     * User user = URLEncodedUtil.parameters2Bean(params, User.class);
     * // user: {name="Alice", tags="java, coding"}
     * </pre>
     *
     * @param <T> the type of the bean to create
     * @param parameters the map of parameters to convert, where keys are property names and values are String arrays;
     *                   may be {@code null} or empty
     * @param targetType the class of the bean to create and populate
     * @return an instance of type T with properties populated from the parameter map;
     *         returns an empty instance if {@code parameters} is {@code null} or empty
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
     * Encodes the provided parameters into a URL-encoded query string using the default charset (UTF-8).
     * <p>
     * This method accepts various parameter formats:
     * <ul>
     * <li>{@code Map<String, ?>}: Keys and values are encoded as name=value pairs</li>
     * <li>JavaBean: Bean properties are encoded using lowerCamelCase naming</li>
     * <li>{@code Object[]}: Pairs of name-value elements (must have even length)</li>
     * <li>{@code String}: If contains "=", treated as encoded parameters; otherwise encoded as-is</li>
     * </ul>
     * Characters are percent-encoded according to application/x-www-form-urlencoded rules, where spaces become '+'.
     * </p>
     *
     * <pre>
     * Map&lt;String, Object&gt; params = Map.of("name", "John Doe", "age", 30);
     * String query = URLEncodedUtil.encode(params);
     * // query: "name=John+Doe&amp;age=30"
     * </pre>
     *
     * @param parameters the parameters to encode (Map, bean, Object array pairs, or String); may be {@code null}
     * @return a URL-encoded query string (e.g., "name=John+Doe&amp;age=30"); returns empty string if {@code parameters} is {@code null}
     * @see #encode(Object, Charset)
     * @see #encode(Object, Charset, NamingPolicy)
     * @see URLEncoder#encode(String, String)
     */
    public static String encode(final Object parameters) {
        return encode(parameters, Charsets.DEFAULT);
    }

    /**
     * Encodes the provided parameters into a URL-encoded query string using the specified charset.
     * <p>
     * This method accepts various parameter formats:
     * <ul>
     * <li>{@code Map<String, ?>}: Keys and values are encoded as name=value pairs</li>
     * <li>JavaBean: Bean properties are encoded using lowerCamelCase naming (default)</li>
     * <li>{@code Object[]}: Pairs of name-value elements (must have even length)</li>
     * <li>{@code String}: If contains "=", treated as encoded parameters; otherwise encoded as-is</li>
     * </ul>
     * Characters are percent-encoded using the specified charset according to application/x-www-form-urlencoded rules.
     * </p>
     *
     * <pre>
     * Map&lt;String, Object&gt; params = Map.of("name", "中文");
     * String query = URLEncodedUtil.encode(params, StandardCharsets.UTF_8);
     * // query: "name=%E4%B8%AD%E6%96%87"
     * </pre>
     *
     * @param parameters the parameters to encode (Map, bean, Object array pairs, or String); may be {@code null}
     * @param charset the charset to use for percent-encoding; if {@code null}, defaults to UTF-8
     * @return a URL-encoded query string; returns empty string if {@code parameters} is {@code null}
     * @see #encode(Object)
     * @see #encode(Object, Charset, NamingPolicy)
     * @see URLEncoder#encode(String, Charset)
     */
    public static String encode(final Object parameters, final Charset charset) {
        return encode(parameters, charset, NamingPolicy.LOWER_CAMEL_CASE);
    }

    /**
     * Encodes the provided parameters into a URL-encoded query string using the specified charset and naming policy.
     * <p>
     * This method accepts various parameter formats:
     * <ul>
     * <li>{@code Map<String, ?>}: Keys and values are encoded as name=value pairs (keys transformed by naming policy)</li>
     * <li>JavaBean: Bean properties are encoded with names transformed according to the naming policy</li>
     * <li>{@code Object[]}: Pairs of name-value elements (must have even length; names transformed by naming policy)</li>
     * <li>{@code String}: If contains "=", treated as encoded parameters; otherwise encoded as-is</li>
     * </ul>
     * Characters are percent-encoded using the specified charset according to application/x-www-form-urlencoded rules.
     * </p>
     *
     * <pre>
     * class User { String firstName; int userAge; }
     * User user = new User("John", 30);
     * String query = URLEncodedUtil.encode(user, StandardCharsets.UTF_8, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
     * // query: "first_name=John&amp;user_age=30"
     * </pre>
     *
     * @param parameters the parameters to encode (Map, bean, Object array pairs, or String); may be {@code null}
     * @param charset the charset to use for percent-encoding; if {@code null}, defaults to UTF-8
     * @param namingPolicy the naming policy to transform property/key names (e.g., LOWER_CAMEL_CASE, UPPER_CASE_WITH_UNDERSCORE);
     *                     if {@code null} or NO_CHANGE, names are not transformed
     * @return a URL-encoded query string; returns empty string if {@code parameters} is {@code null}
     * @see #encode(Object, Charset)
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
     * Encodes parameters and appends them to a URL as a query string using the default charset (UTF-8).
     * <p>
     * This method takes a base URL and parameters, encodes the parameters according to
     * application/x-www-form-urlencoded rules, and appends them to the URL with a '?' separator.
     * If {@code parameters} is {@code null} or an empty Map, the original URL is returned unchanged.
     * </p>
     *
     * <pre>
     * Map&lt;String, Object&gt; params = Map.of("q", "java url encoding", "page", 1);
     * String fullUrl = URLEncodedUtil.encode("http://search.example.com", params);
     * // fullUrl: "http://search.example.com?q=java+url+encoding&amp;page=1"
     * </pre>
     *
     * @param url the base URL to which the query string will be appended (e.g., "http://example.com/path")
     * @param parameters the parameters to encode and append (Map, bean, Object array pairs, or String); may be {@code null}
     * @return the URL with the encoded query string appended (e.g., "http://example.com/path?name=value");
     *         returns the original URL if {@code parameters} is {@code null} or empty
     * @see #encode(String, Object, Charset)
     * @see #encode(Object)
     */
    public static String encode(final String url, final Object parameters) {
        return encode(url, parameters, Charsets.DEFAULT);
    }

    /**
     * Encodes parameters and appends them to a URL as a query string using the specified charset.
     * <p>
     * This method takes a base URL and parameters, encodes the parameters using the specified charset
     * according to application/x-www-form-urlencoded rules, and appends them to the URL with a '?'
     * separator. If {@code parameters} is {@code null} or an empty Map, the original URL is returned unchanged.
     * </p>
     *
     * <pre>
     * Map&lt;String, Object&gt; params = Map.of("name", "中文");
     * String fullUrl = URLEncodedUtil.encode("http://example.com", params, StandardCharsets.UTF_8);
     * // fullUrl: "http://example.com?name=%E4%B8%AD%E6%96%87"
     * </pre>
     *
     * @param url the base URL to which the query string will be appended
     * @param parameters the parameters to encode and append (Map, bean, Object array pairs, or String); may be {@code null}
     * @param charset the charset to use for percent-encoding; if {@code null}, defaults to UTF-8
     * @return the URL with the encoded query string appended;
     *         returns the original URL if {@code parameters} is {@code null} or empty
     * @see #encode(String, Object)
     * @see #encode(String, Object, Charset, NamingPolicy)
     */
    public static String encode(final String url, final Object parameters, final Charset charset) {
        return encode(url, parameters, charset, NamingPolicy.LOWER_CAMEL_CASE);
    }

    /**
     * Encodes parameters and appends them to a URL as a query string using the specified charset and naming policy.
     * <p>
     * This method takes a base URL and parameters, encodes the parameters using the specified charset and
     * naming policy according to application/x-www-form-urlencoded rules, and appends them to the URL with
     * a '?' separator. Property/key names are transformed according to the naming policy before encoding.
     * If {@code parameters} is {@code null} or an empty Map, the original URL is returned unchanged.
     * </p>
     *
     * <pre>
     * class User { String firstName; }
     * String url = URLEncodedUtil.encode("http://api.com/users", new User("John"),
     *                                     StandardCharsets.UTF_8, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
     * // url: "http://api.com/users?first_name=John"
     * </pre>
     *
     * @param url the base URL to which the query string will be appended
     * @param parameters the parameters to encode and append (Map, bean, Object array pairs, or String); may be {@code null}
     * @param charset the charset to use for percent-encoding; if {@code null}, defaults to UTF-8
     * @param namingPolicy the naming policy to transform property/key names (e.g., LOWER_CAMEL_CASE, UPPER_CASE_WITH_UNDERSCORE);
     *                     if {@code null} or NO_CHANGE, names are not transformed
     * @return the URL with the encoded query string appended;
     *         returns the original URL if {@code parameters} is {@code null} or empty
     * @see #encode(String, Object, Charset)
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
     * Encodes the provided parameters into a URL-encoded query string and appends the result to an {@code Appendable} using the default charset (UTF-8).
     * <p>
     * This method is useful for streaming or building query strings without creating intermediate String objects.
     * The parameters are encoded according to application/x-www-form-urlencoded rules and directly appended
     * to the provided output.
     * </p>
     *
     * <pre>
     * StringBuilder sb = new StringBuilder("http://example.com?");
     * URLEncodedUtil.encode(Map.of("key", "value"), sb);
     * // sb: "http://example.com?key=value"
     * </pre>
     *
     * @param parameters the parameters to encode (Map, bean, Object array pairs, or String); may be {@code null}
     * @param output the {@code Appendable} (e.g., {@code StringBuilder}, {@code Writer}) to which the encoded query string will be appended
     * @throws UncheckedIOException if an I/O error occurs while appending to the output
     * @see #encode(Object, Charset, Appendable)
     */
    public static void encode(final Object parameters, final Appendable output) {
        encode(parameters, Charsets.DEFAULT, output);
    }

    /**
     * Encodes the provided parameters into a URL-encoded query string and appends the result to an {@code Appendable} using the specified charset.
     * <p>
     * This method is useful for streaming or building query strings without creating intermediate String objects.
     * The parameters are encoded using the specified charset according to application/x-www-form-urlencoded rules
     * and directly appended to the provided output.
     * </p>
     *
     * <pre>
     * Writer writer = new FileWriter("query.txt");
     * URLEncodedUtil.encode(Map.of("name", "中文"), StandardCharsets.UTF_8, writer);
     * // writer content: "name=%E4%B8%AD%E6%96%87"
     * </pre>
     *
     * @param parameters the parameters to encode (Map, bean, Object array pairs, or String); may be {@code null}
     * @param charset the charset to use for percent-encoding; if {@code null}, defaults to UTF-8
     * @param output the {@code Appendable} to which the encoded query string will be appended
     * @throws UncheckedIOException if an I/O error occurs while appending to the output
     * @see #encode(Object, Charset, NamingPolicy, Appendable)
     */
    public static void encode(final Object parameters, final Charset charset, final Appendable output) {
        encode(parameters, charset, NamingPolicy.LOWER_CAMEL_CASE, output);
    }

    /**
     * Encodes the provided parameters into a URL-encoded query string and appends the result to an {@code Appendable}
     * using the specified charset and naming policy.
     * <p>
     * This method is useful for streaming or building query strings without creating intermediate String objects.
     * Property/key names are transformed according to the naming policy before being percent-encoded using the
     * specified charset according to application/x-www-form-urlencoded rules.
     * </p>
     * <p>
     * The method accepts various parameter formats:
     * <ul>
     * <li>{@code Map<String, ?>}: Keys and values are encoded as name=value pairs</li>
     * <li>JavaBean: Bean properties are encoded with names transformed by the naming policy</li>
     * <li>{@code Object[]}: Pairs of name-value elements (must have even length)</li>
     * <li>{@code String}: If contains "=", treated as pre-encoded parameters; otherwise encoded as-is</li>
     * </ul>
     * </p>
     *
     * <pre>
     * class User { String firstName; int age; }
     * StringBuilder sb = new StringBuilder();
     * URLEncodedUtil.encode(new User("John", 30), StandardCharsets.UTF_8, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, sb);
     * // sb: "first_name=John&amp;age=30"
     * </pre>
     *
     * @param parameters the parameters to encode (Map, bean, Object array pairs, or String); may be {@code null}
     * @param charset the charset to use for percent-encoding; if {@code null}, defaults to UTF-8
     * @param namingPolicy the naming policy to transform property/key names (e.g., LOWER_CAMEL_CASE, UPPER_CASE_WITH_UNDERSCORE);
     *                     if {@code null} or NO_CHANGE, names are not transformed
     * @param output the {@code Appendable} to which the encoded query string will be appended
     * @throws UncheckedIOException if an I/O error occurs while appending to the output
     * @throws IllegalArgumentException if parameters is an Object array with odd length
     * @see #encode(Object, Charset, Appendable)
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
            } else if (Beans.isBeanClass(parameters.getClass())) {
                encode(Beans.bean2Map(parameters, true, null, namingPolicy), charset, NamingPolicy.NO_CHANGE, output);
            } else if (parameters instanceof Object[] a) {
                if (0 != (a.length % 2)) {
                    throw new IllegalArgumentException(
                            "The parameters must be the pairs of property name and value, or Map, or a bean class with getter/setter methods.");
                }

                for (int i = 0, len = a.length; i < len; i += 2) {
                    if (i > 0) {
                        output.append(QP_SEP_A);
                    }

                    if (isNoChange) {
                        encodeFormFields((String) a[i], charset, output);
                    } else {
                        encodeFormFields(namingPolicy.convert((String) a[i]), charset, output);
                    }

                    output.append(NAME_VALUE_SEPARATOR);

                    encodeFormFields(N.stringOf(a[i + 1]), charset, output);
                }
            } else if (parameters instanceof CharSequence source) {
                final String str = source.toString();

                if (str.contains(NAME_VALUE_SEPARATOR)) {
                    encode(PARAMS_SPLITTER.split(str), charset, NamingPolicy.NO_CHANGE, output);
                } else {
                    encodeFormFields(str, charset, output);
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
