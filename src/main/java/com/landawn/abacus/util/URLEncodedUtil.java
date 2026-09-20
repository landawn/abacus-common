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
 * <a href="http://www.apache.org/">http://www.apache.org/</a>.
 */
package com.landawn.abacus.util;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.util.AbstractMap;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;

/**
 * Utility methods for URL encoding and decoding
 * operations, including query parameter encoding, form data processing, and URL component manipulation.
 * This class combines the robustness of Apache HttpComponents with enhanced functionality for modern Java
 * applications, offering component-specific percent encoding and convenient object-to-query-string conversions.
 *
 * <p>This utility addresses common challenges in web development by providing null-safe operations, flexible
 * charset handling, customizable naming policies, and seamless integration with Java objects. It supports
 * both simple string-based operations and complex object serialization to URL-encoded format, making it
 * suitable for REST API clients, web form processing, and HTTP parameter manipulation.</p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Encoding modes:</b> HTML form encoding plus URI-component safe sets derived from RFC 2396</li>
 *   <li><b>Object Serialization:</b> Automatic conversion of Java objects to URL-encoded query strings</li>
 *   <li><b>Flexible Parsing:</b> Support for both Map-based and object-based parameter decoding</li>
 *   <li><b>Charset Support:</b> Configurable, lossless character encoding; malformed UTF-16 and characters not
 *       representable in the selected charset are rejected rather than replaced</li>
 *   <li><b>Naming Policies:</b> Customizable field naming strategies for object serialization</li>
 *
 *   <li><b>Stateless operation:</b> Separate calls share no mutable encoding state</li>
 *   <li><b>Performance Optimized:</b> BitSet-based encoding tables and efficient string processing</li>
 *   <li><b>Null Safety:</b> Comprehensive {@code null} handling with predictable behavior</li>
 * </ul>
 *
 * <p><b>Design Philosophy:</b>
 * <ul>
 *   <li><b>Explicit rules:</b> Form data and URI components use distinct documented safe-character sets</li>
 *   <li><b>Developer Productivity:</b> Simplified API that handles complex encoding scenarios</li>
 *   <li><b>Performance First:</b> Optimized algorithms and pre-computed lookup tables</li>
 *   <li><b>Flexibility:</b> Support for various input types and output formats</li>
 *   <li><b>Apache Heritage:</b> Built on proven Apache HttpComponents foundations</li>
 * </ul>
 *
 * <p><b>Encoding Constants and Separators:</b>
 * <ul>
 *   <li><b>{@link #QP_SEP_A}:</b> Ampersand ('&amp;') separator for query parameters</li>
 *   <li><b>{@link #QP_SEP_S}:</b> Semicolon (';') separator for alternative parameter formatting</li>
 *   <li><b>{@link #NAME_VALUE_SEPARATOR}:</b> Equals ('=') separator for name-value pairs</li>
 *   <li><b>URL_ENCODER BitSet:</b> Pre-computed safe character table for URL encoding</li>
 * </ul>
 *
 * <p><b>Supported Character Sets:</b>
 * <ul>
 *   <li><b>Default (UTF-8):</b> Used when no charset is specified. This library always defaults to UTF-8 ({@code IOUtil.DEFAULT_CHARSET}), <b>not</b> the JVM's {@link Charset#defaultCharset()}, for cross-platform consistency</li>
 *   <li><b>UTF-8:</b> Recommended charset for modern web applications</li>
 *   <li><b>ISO-8859-1:</b> Legacy charset support for older systems</li>
 *   <li><b>Custom Charsets:</b> Any Charset supported by the JVM</li>
 * </ul>
 *
 * <p><b>Common Usage Patterns:</b>
 * <pre>{@code
 * // SearchCriteria and Person below are conventional JavaBeans with public no-arg constructors
 * // and standard property accessors.
 * // Basic object to query string encoding
 * SearchCriteria criteria = new SearchCriteria("java", "programming", 10);
 * String queryString = URLEncodedUtil.encode(criteria);
 * // Result: "query=java&category=programming&limit=10"
 *
 * // URL construction with parameters
 * String url = URLEncodedUtil.encode("/api/search", criteria);
 * // Result: "/api/search?query=java&category=programming&limit=10"
 *
 * // Custom charset and naming policy
 * String encoded = URLEncodedUtil.encode(criteria, StandardCharsets.UTF_8, NamingPolicy.SNAKE_CASE);
 * // Result: "query=java&category=programming&limit=10"
 *
 * // Decoding query strings to Maps
 * Map<String, String> params = URLEncodedUtil.decode("name=John&age=30");
 * // Result: {"name": "John", "age": "30"}
 *
 * // Decoding directly to objects
 * Person person = URLEncodedUtil.decode("name=John&age=30", Person.class);
 * // Result: Person object with name="John" and age=30
 * }</pre>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Bean property values are encoded as strings; nested objects are not flattened into paths
 * UserPreferences prefs = new UserPreferences();
 * prefs.setTheme("dark");
 * prefs.setLanguage("en");
 * prefs.getNotifications().setEmail(true);
 *
 * String encoded = URLEncodedUtil.encode(prefs, StandardCharsets.UTF_8, NamingPolicy.CAMEL_CASE);
 * // The notifications property is represented by its normal string form
 *
 * // Multimap decoding for duplicate parameter names
 * ListMultimap<String, String> params = URLEncodedUtil.decodeToMultimap("tag=java&tag=web&tag=api");
 * // Result: {"tag": ["java", "web", "api"]}
 *
 * // Custom Map supplier for specific Map implementations
 * TreeMap<String, String> sortedParams = URLEncodedUtil.decode(
 *     "c=3&a=1&b=2", StandardCharsets.UTF_8, TreeMap::new);
 * // Result: Sorted map with natural key ordering
 *
 * // Servlet-style parameter processing
 * Map<String, String[]> requestParameters = getRequestParameters();
 * UserProfile profile = URLEncodedUtil.convertToBean(
 *     requestParameters, UserProfile.class);
 * }</pre>
 *
 * <p><b>Method Categories:</b>
 * <ul>
 *   <li><b>Encoding Methods:</b> Convert objects and parameters to URL-encoded strings</li>
 *   <li><b>URL Construction:</b> Combine base URLs with encoded parameter strings</li>
 *   <li><b>Decoding Methods:</b> Parse URL-encoded strings into Maps or objects</li>
 *   <li><b>Specialized Encoding:</b> Component-specific encoding (user info, path, query)</li>
 *   <li><b>Stream Processing:</b> Methods accepting Appendable for memory-efficient output</li>
 * </ul>
 *
 * <p><b>Object Serialization Support:</b>
 * <ul>
 *   <li><b>Bean Properties:</b> Automatic discovery and encoding of JavaBean properties</li>
 *   <li><b>Bean Access:</b> Properties are discovered through the library's JavaBean metadata</li>
 *   <li><b>Value Handling:</b> Map values, array-pair values, and bean property values are converted with their normal string representation</li>
 *   <li><b>Null Values:</b> Null bean properties are omitted; explicit null Map/array values are encoded as valueless tokens (the name only, matching how decode parses a token without {@code '='})</li>
 *   <li><b>Type Conversion:</b> Automatic conversion of primitive and wrapper types</li>
 * </ul>
 *
 * <p><b>Naming Policy Integration:</b> the supported policies are the values of the
 * {@link NamingPolicy} enum:
 * <ul>
 *   <li><b>CAMEL_CASE:</b> Convert property names to camelCase format</li>
 *   <li><b>UPPER_CAMEL_CASE:</b> Convert to UpperCamelCase (Pascal case) format</li>
 *   <li><b>SNAKE_CASE:</b> Convert to snake_case format</li>
 *   <li><b>SCREAMING_SNAKE_CASE:</b> Convert to SCREAMING_SNAKE_CASE format</li>
 *   <li><b>KEBAB_CASE:</b> Convert to kebab-case format</li>
 *   <li><b>NO_CHANGE:</b> Preserve original property names</li>
 * </ul>
 *
 * <p><b>URL Component Encoding:</b>
 * <ul>
 *   <li><b>Query Parameters:</b> Standard form field encoding with '+' for spaces</li>
 *   <li><b>Path Components:</b> Path-specific encoding preserving directory structure</li>
 *   <li><b>User Info:</b> Encoding for username:password components in URLs</li>
 *   <li><b>Generic URI:</b> Component encoding using the documented RFC 2396-era safe sets</li>
 * </ul>
 *
 * <p><b>Performance Characteristics:</b>
 * <ul>
 *   <li><b>BitSet Optimization:</b> O(1) character safety lookup using pre-computed tables</li>
 *   <li><b>StringBuilder Usage:</b> Efficient string building for large parameter sets</li>
 *   <li><b>Streaming Support:</b> Memory-efficient processing via Appendable interface</li>
 *   <li><b>Reflection Caching:</b> Cached BeanInfo objects for repeated object processing</li>
 *   <li><b>Charset Efficiency:</b> Optimized byte-to-character conversions</li>
 * </ul>
 *
 * <p><b>Thread Safety and Concurrency:</b>
 * <ul>
 *   <li><b>Internal State:</b> Encoding tables are initialized once and are not mutated afterward</li>
 *   <li><b>Caller State:</b> A caller must not concurrently mutate an input Map/bean or a supplied Appendable</li>
 * </ul>
 *
 * <p><b>Error Handling:</b>
 * <ul>
 *   <li><b>UncheckedIOException:</b> Wraps IOException from Appendable operations</li>
 *   <li><b>IllegalArgumentException:</b> Thrown for invalid argument shapes or unsupported target types</li>
 *   <li><b>NullPointerException:</b> A destination map rejects a decoded {@code null} value</li>
 *   <li><b>Charset Fallback:</b> A {@code null} charset is handled gracefully with a fallback to UTF-8</li>
 * </ul>
 *
 * <p><b>Encoding Standards:</b>
 * <ul>
 *   <li><b>URI components:</b> Percent encoding with safe sets inherited from Apache HttpComponents and RFC 2396</li>
 *   <li><b>HTML Forms:</b> application/x-www-form-urlencoded format support</li>
 *   <li><b>Query Parameters:</b> Standard web form parameter encoding/decoding</li>
 *   <li><b>Character Encoding:</b> Proper Unicode support with configurable charsets</li>
 * </ul>
 *
 * <p><b>Integration with Web Frameworks:</b>
 * <ul>
 *   <li><b>Servlet API:</b> Conversion from the {@code Map<String, String[]>} shape returned by servlet requests</li>
 *   <li><b>Spring Framework:</b> Compatible with Spring's parameter binding mechanisms</li>
 *   <li><b>JAX-RS:</b> Suitable for REST client parameter encoding</li>
 *   <li><b>HTTP Clients:</b> Integration with Apache HttpClient, OkHttp, and others</li>
 * </ul>
 *
 * <p><b>Best Practices:</b>
 * <ul>
 *   <li>Always specify charset explicitly for cross-platform compatibility</li>
 *   <li>Use UTF-8 encoding for modern web applications to ensure Unicode support</li>
 *   <li>Use Map or flat JavaBean inputs for structured parameter sets</li>
 *   <li>Use appropriate naming policies to match API expectations</li>
 *   <li>Bean metadata is cached internally for repeated processing of the same type</li>
 *   <li>Use streaming methods (Appendable) for very large parameter sets</li>
 *   <li>Validate decoded objects to ensure data integrity and security</li>
 * </ul>
 *
 * <p><b>Common Anti-Patterns to Avoid:</b>
 * <ul>
 *   <li>Manual string concatenation for query parameter building</li>
 *   <li>Ignoring character encoding issues in international applications</li>
 *   <li>Using platform default charset instead of explicitly specifying UTF-8</li>
 *   <li>Assuming all query parameters have single values (use multimap when appropriate)</li>
 *   <li>Not validating decoded object properties for security vulnerabilities</li>
 *   <li>Repeatedly encoding the same object types without utilizing caching mechanisms</li>
 * </ul>
 *
 * <p><b>Security Considerations:</b>
 * <ul>
 *   <li><b>Input Validation:</b> Always validate decoded parameters before use</li>
 *   <li><b>Context:</b> Percent encoding is not a substitute for validating schemes, hosts, paths, or redirect targets</li>
 *   <li><b>Character Sets:</b> Use the same explicit charset at both ends to avoid ambiguous decoding</li>
 *   <li><b>Length Limits:</b> Consider implementing parameter length limits for DoS prevention</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * Map<String, Object> parameters = new LinkedHashMap<>();
 * parameters.put("searchTerm", "Java & XML");
 * parameters.put("page", 2);
 *
 * String url = URLEncodedUtil.encode("https://api.example.com/search", parameters,
 *         StandardCharsets.UTF_8, NamingPolicy.SNAKE_CASE);
 * // url: "https://api.example.com/search?search_term=Java+%26+XML&page=2"
 * }</pre>
 *
 * <p><b>Comparison with Alternative Approaches:</b>
 * <ul>
 *   <li><b>vs. URLEncoder/URLDecoder:</b> More features and object support vs. basic string encoding</li>
 *   <li><b>vs. Spring UriComponentsBuilder:</b> Lightweight and focused vs. comprehensive URI building</li>
 *   <li><b>vs. Apache HttpClient:</b> Specialized for URL encoding vs. full HTTP client functionality</li>
 *   <li><b>vs. Manual String Building:</b> Centralized percent-encoding instead of ad hoc concatenation</li>
 * </ul>
 *
 * <p><b>Attribution:</b>
 * This class includes code adapted from Apache HttpComponents under the Apache License 2.0.
 * Methods from these libraries may have been modified for consistency, performance optimization, and null-safety enhancement.
 *
 * @see java.net.URLEncoder
 * @see java.net.URLDecoder
 * @see java.nio.charset.Charset
 * @see java.nio.charset.StandardCharsets
 * @see com.landawn.abacus.util.NamingPolicy
 * @see com.landawn.abacus.util.ListMultimap
 * @see com.landawn.abacus.util.Splitter
 * @see <a href="https://tools.ietf.org/html/rfc3986">RFC 3986: Uniform Resource Identifier (URI): Generic Syntax</a>
 * @see <a href="https://www.w3.org/TR/html401/interact/forms.html#h-17.13.4.1">HTML 4.01 Form Content Types</a>
 * @see <a href="https://hc.apache.org/">Apache HttpComponents</a>
 */
public final class URLEncodedUtil {
    /**
     * The constant representing the ampersand character ('&amp;') used as a separator in URL query parameters.
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
     * Unreserved characters, i.e., alphanumeric, plus: {@code _ - ! . ~ ' ( ) *}
     * <p>
     * This list is the same as the {@code unreserved} list in <a href="http://www.ietf.org/rfc/rfc2396.txt">RFC
     * 2396</a>
     */
    private static final BitSet UNRESERVED = new BitSet(256);

    /**
     * Punctuation characters: , ; : $ &amp; + =
     * <p>
     * These are the additional characters allowed by userinfo.
     */
    private static final BitSet PUNCT = new BitSet(256);

    /**
     * Characters which are safe to use in userinfo, i.e., {@link #UNRESERVED} plus {@link #PUNCT}
     */
    private static final BitSet USERINFO = new BitSet(256);

    /**
     * Characters which are safe to use in a path, i.e., {@link #UNRESERVED} plus {@link #PUNCT} plus / @
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
        // Utility class - prevent instantiation
    }

    /**
     * Encodes the provided parameters into a URL-encoded query string using UTF-8.
     * <p>
     * This method accepts various parameter formats:
     * <ul>
     * <li>{@code Map<String, ?>}: non-null String keys and their values are encoded as name=value pairs</li>
     * <li>JavaBean: Bean properties are encoded with their original names ({@link NamingPolicy#NO_CHANGE})</li>
     * <li>{@code Object[]}: pairs of non-null String names and arbitrary values (must have even length)</li>
     * <li>{@code CharSequence}: if it contains {@code '='} it is taken to be an ALREADY-ENCODED query string and is
     *     appended verbatim (nothing is escaped, and duplicate names are preserved); otherwise it is encoded as a
     *     single form field. See the {@code parameters} note below.</li>
     * <li>Any other value - including a primitive array (an {@code int[]} is not an {@code Object[]}), a
     *     {@code Collection} and a bare scalar - is converted with {@link N#stringOf(Object)} and encoded as a
     *     single valueless form field (the text only, with no {@code '='} and no value); it is NOT split into
     *     name/value pairs.</li>
     * </ul>
     * Characters are percent-encoded according to application/x-www-form-urlencoded rules, where spaces become '+'.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> params = new LinkedHashMap<>();
     * params.put("name", "John Doe");
     * params.put("age", 30);
     * String query = URLEncodedUtil.encode(params);
     * // query: "name=John+Doe&age=30"
     * }</pre>
     *
     * <p><b>Note:</b> This overload applies {@link NamingPolicy#NO_CHANGE} as the naming policy, so Map keys,
     * {@code Object[]} names, and bean property names are emitted verbatim ({@code first_name} stays
     * {@code first_name}). To rewrite names, use
     * {@link #encode(Object, Charset, NamingPolicy)} with another policy such as {@link NamingPolicy#CAMEL_CASE}.
     *
     * @param parameters the parameters to encode (Map, bean, Object array pairs, String, or any other value); may be
     *        {@code null}. A {@code CharSequence} containing {@code '='} is treated as an already URL-encoded
     *        query string and appended verbatim; one without {@code '='} is encoded as a single form field.
     *        Any other value - including a primitive array and a {@code Collection} - is converted with
     *        {@link N#stringOf(Object)} and encoded as a single valueless form field; it is NOT split into
     *        name/value pairs.
     * @return a URL-encoded query string (e.g., "name=John+Doe&amp;age=30"); returns empty string if {@code parameters} is {@code null}.
     * @throws IllegalArgumentException if a Map key or {@code Object[]} name element is {@code null}, an
     *         {@code Object[]} has odd length, an effective name is empty and its value is null, or text to encode
     *         contains malformed UTF-16
     * @throws ClassCastException if a non-null Map key or {@code Object[]} name element is not a String
     * @see #encode(Object, Charset)
     * @see #encode(Object, Charset, NamingPolicy)
     * @see URLEncoder#encode(String, Charset)
     */
    public static String encode(final Object parameters) throws IllegalArgumentException, ClassCastException {
        return encode(parameters, IOUtil.DEFAULT_CHARSET);
    }

    /**
     * Encodes the provided parameters into a URL-encoded query string using the specified charset.
     * <p>
     * This method accepts various parameter formats:
     * <ul>
     * <li>{@code Map<String, ?>}: non-null String keys and their values are encoded as name=value pairs</li>
     * <li>JavaBean: Bean properties are encoded with their original names ({@link NamingPolicy#NO_CHANGE})</li>
     * <li>{@code Object[]}: pairs of non-null String names and arbitrary values (must have even length)</li>
     * <li>{@code CharSequence}: if it contains {@code '='} it is taken to be an ALREADY-ENCODED query string and is
     *     appended verbatim (nothing is escaped, and duplicate names are preserved); otherwise it is encoded as a
     *     single form field. See the {@code parameters} note below.</li>
     * <li>Any other value - including a primitive array (an {@code int[]} is not an {@code Object[]}), a
     *     {@code Collection} and a bare scalar - is converted with {@link N#stringOf(Object)} and encoded as a
     *     single valueless form field (the text only, with no {@code '='} and no value); it is NOT split into
     *     name/value pairs.</li>
     * </ul>
     * Characters are percent-encoded using the specified charset according to application/x-www-form-urlencoded rules.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> params = Map.of("name", "中文");
     * String query = URLEncodedUtil.encode(params, StandardCharsets.UTF_8);
     * // query: "name=%E4%B8%AD%E6%96%87"
     * }</pre>
     *
     * <p><b>Note:</b> This overload applies {@link NamingPolicy#NO_CHANGE} as the naming policy, so Map keys,
     * {@code Object[]} names, and bean property names are emitted verbatim ({@code first_name} stays
     * {@code first_name}). To rewrite names, use
     * {@link #encode(Object, Charset, NamingPolicy)} with another policy such as {@link NamingPolicy#CAMEL_CASE}.
     *
     * @param parameters the parameters to encode (Map, bean, Object array pairs, String, or any other value); may be
     *        {@code null}. A {@code CharSequence} containing {@code '='} is treated as an already URL-encoded
     *        query string and appended verbatim; one without {@code '='} is encoded as a single form field.
     *        Any other value - including a primitive array and a {@code Collection} - is converted with
     *        {@link N#stringOf(Object)} and encoded as a single valueless form field; it is NOT split into
     *        name/value pairs.
     * @param charset the charset to use for percent-encoding; if {@code null}, defaults to UTF-8.
     * @return a URL-encoded query string; returns empty string if {@code parameters} is {@code null}.
     * @throws IllegalArgumentException if a Map key or {@code Object[]} name element is {@code null}, an
     *         {@code Object[]} has odd length, an effective name is empty and its value is null, or text to encode contains malformed UTF-16 or a character that
     *         {@code charset} cannot represent
     * @throws ClassCastException if a non-null Map key or {@code Object[]} name element is not a String
     * @see #encode(Object)
     * @see #encode(Object, Charset, NamingPolicy)
     * @see URLEncoder#encode(String, Charset)
     */
    public static String encode(final Object parameters, final Charset charset) throws IllegalArgumentException, ClassCastException {
        return encode(parameters, charset, NamingPolicy.NO_CHANGE);
    }

    /**
     * Encodes the provided parameters into a URL-encoded query string using the specified charset and naming policy.
     * <p>
     * This method accepts various parameter formats:
     * <ul>
     * <li>{@code Map<String, ?>}: non-null String keys and arbitrary values are encoded as name=value pairs (keys transformed by naming policy)</li>
     * <li>JavaBean: Bean properties are encoded with names transformed according to the naming policy</li>
     * <li>{@code Object[]}: pairs of non-null String names and arbitrary values (must have even length; names transformed by naming policy)</li>
     * <li>{@code CharSequence}: if it contains {@code '='} it is taken to be an ALREADY-ENCODED query string and is
     *     appended verbatim (nothing is escaped, and duplicate names are preserved); otherwise it is encoded as a
     *     single form field. See the {@code parameters} note below.</li>
     * <li>Any other value - including a primitive array (an {@code int[]} is not an {@code Object[]}), a
     *     {@code Collection} and a bare scalar - is converted with {@link N#stringOf(Object)} and encoded as a
     *     single valueless form field (the text only, with no {@code '='} and no value); it is NOT split into
     *     name/value pairs.</li>
     * </ul>
     * Characters are percent-encoded using the specified charset according to application/x-www-form-urlencoded rules.
     * A {@code null} Map value or {@code Object[]} value element is encoded as a valueless token (the name only,
     * without {@code '='}), matching how {@link #decode(String)} parses such a token, so a decode→encode round trip
     * preserves {@code null}. An empty effective name with a null value is rejected because its empty token would disappear on decoding. A String value of {@code "null"} is encoded as {@code name=null}. Null JavaBean
     * properties are omitted.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> user = new LinkedHashMap<>();
     * user.put("firstName", "John");
     * user.put("userAge", 30);
     * String query = URLEncodedUtil.encode(user, StandardCharsets.UTF_8, NamingPolicy.SNAKE_CASE);
     * // query: "first_name=John&user_age=30"
     * }</pre>
     *
     * @param parameters the parameters to encode (Map, bean, Object array pairs, String, or any other value); may be
     *        {@code null}. A {@code CharSequence} containing {@code '='} is treated as an already URL-encoded
     *        query string and appended verbatim; one without {@code '='} is encoded as a single form field.
     *        Any other value - including a primitive array and a {@code Collection} - is converted with
     *        {@link N#stringOf(Object)} and encoded as a single valueless form field; it is NOT split into
     *        name/value pairs.
     * @param charset the charset to use for percent-encoding; if {@code null}, defaults to UTF-8.
     * @param namingPolicy the naming policy to transform property/key names (e.g., CAMEL_CASE, SCREAMING_SNAKE_CASE);
     *                     if {@code null} or NO_CHANGE, names are not transformed.
     * @return a URL-encoded query string; returns empty string if {@code parameters} is {@code null}.
     * @throws IllegalArgumentException if a Map key or {@code Object[]} name element is {@code null}, an
     *         {@code Object[]} has odd length, an effective name is empty and its value is null, or text to encode contains malformed UTF-16 or a character that
     *         {@code charset} cannot represent
     * @throws ClassCastException if a non-null Map key or {@code Object[]} name element is not a String
     * @see #encode(Object, Charset)
     */
    public static String encode(final Object parameters, final Charset charset, final NamingPolicy namingPolicy)
            throws IllegalArgumentException, ClassCastException {
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
     * Encodes parameters and appends them to a URL as a query string using UTF-8.
     * <p>
     * This method takes a base URL and parameters, encodes the parameters according to
     * application/x-www-form-urlencoded rules, and appends them to the URL with a '?' separator.
     * If the URL already contains a '?' the encoded parameters are joined with <i>&amp;</i>. Any fragment
     * identifier ({@code #...}) in the URL is preserved and placed after the encoded parameters.
     * If {@code parameters} is a {@code CharSequence} containing {@code '='}, it is treated as an
     * already URL-encoded query string and appended verbatim (duplicate parameter names are preserved).
     * If {@code parameters} is {@code null}, an empty Map, an empty CharSequence, an empty Object array, or an
     * object whose encodable fields are all omitted (for example, an all-null bean), the original URL is returned
     * unchanged.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> params = new LinkedHashMap<>();
     * params.put("q", "java url encoding");
     * params.put("page", 1);
     * String fullUrl = URLEncodedUtil.encode("http://search.example.com", params);
     * // fullUrl: "http://search.example.com?q=java+url+encoding&page=1"
     * }</pre>
     *
     * <p><b>Note:</b> This overload applies {@link NamingPolicy#NO_CHANGE} as the naming policy, so Map keys,
     * {@code Object[]} names, and bean property names are emitted verbatim ({@code first_name} stays
     * {@code first_name}). To rewrite names, use
     * {@link #encode(String, Object, Charset, NamingPolicy)} with another policy such as {@link NamingPolicy#CAMEL_CASE}.
     *
     * @param url the base URL to which the query string will be appended (e.g., "http://example.com/path").
     * @param parameters the parameters to encode and append (Map, bean, Object array pairs, String, or any other
     *        value); may be {@code null}. Any other value - including a primitive array and a {@code Collection} -
     *        is converted with {@link N#stringOf(Object)} and encoded as a single valueless form field; it is NOT
     *        split into name/value pairs. An empty {@code Collection} or empty primitive array is therefore
     *        <b>not</b> one of the empty cases below - it encodes as the text {@code []}.
     * @return the URL with the encoded query string appended (e.g., "http://example.com/path?name=value");
     *         returns the original URL if {@code parameters} is {@code null}, an empty {@code Map},
     *         an empty {@code CharSequence}, an empty {@code Object[]}, or otherwise produces no encoded fields.
     * @throws IllegalArgumentException if {@code url}, a Map key, or an {@code Object[]} name element is
     *         {@code null}; if an {@code Object[]} has odd length; if an effective name is empty and its value
     *         is {@code null}; or if text to encode contains malformed UTF-16.
     * @throws ClassCastException if a non-null Map key or {@code Object[]} name element is not a String
     * @see #encode(String, Object, Charset)
     * @see #encode(Object)
     */
    public static String encode(final String url, final Object parameters) throws IllegalArgumentException, ClassCastException {
        return encode(url, parameters, IOUtil.DEFAULT_CHARSET);
    }

    /**
     * Encodes parameters and appends them to a URL as a query string using the specified charset.
     * <p>
     * This method takes a base URL and parameters, encodes the parameters using the specified charset
     * according to application/x-www-form-urlencoded rules, and appends them to the URL with a '?'
     * separator. If the URL already contains a '?' the encoded parameters are joined with <i>&amp;</i>. Any
     * fragment identifier ({@code #...}) in the URL is preserved and placed after the encoded parameters.
     * If {@code parameters} is a {@code CharSequence} containing {@code '='}, it is treated as an
     * already URL-encoded query string and appended verbatim (duplicate parameter names are preserved).
     * If {@code parameters} is {@code null}, an empty Map, an empty CharSequence, an empty Object array, or an
     * object whose encodable fields are all omitted (for example, an all-null bean), the original URL is returned
     * unchanged.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> params = Map.of("name", "中文");
     * String fullUrl = URLEncodedUtil.encode("http://example.com", params, StandardCharsets.UTF_8);
     * // fullUrl: "http://example.com?name=%E4%B8%AD%E6%96%87"
     * }</pre>
     *
     * <p><b>Note:</b> This overload applies {@link NamingPolicy#NO_CHANGE} as the naming policy, so Map keys,
     * {@code Object[]} names, and bean property names are emitted verbatim ({@code first_name} stays
     * {@code first_name}). To rewrite names, use
     * {@link #encode(String, Object, Charset, NamingPolicy)} with another policy such as {@link NamingPolicy#CAMEL_CASE}.
     *
     * @param url the base URL to which the query string will be appended.
     * @param parameters the parameters to encode and append (Map, bean, Object array pairs, String, or any other
     *        value); may be {@code null}. Any other value - including a primitive array and a {@code Collection} -
     *        is converted with {@link N#stringOf(Object)} and encoded as a single valueless form field; it is NOT
     *        split into name/value pairs. An empty {@code Collection} or empty primitive array is therefore
     *        <b>not</b> one of the empty cases below - it encodes as the text {@code []}.
     * @param charset the charset to use for percent-encoding; if {@code null}, defaults to UTF-8.
     * @return the URL with the encoded query string appended;
     *         returns the original URL if {@code parameters} is {@code null}, an empty {@code Map},
     *         an empty {@code CharSequence}, an empty {@code Object[]}, or otherwise produces no encoded fields.
     * @throws IllegalArgumentException if {@code url}, a Map key, or an {@code Object[]} name element is
     *         {@code null}; if an {@code Object[]} has odd length; if an effective name is empty and its value
     *         is {@code null}; or if text to encode contains malformed UTF-16 or a character that {@code charset}
     *         cannot represent.
     * @throws ClassCastException if a non-null Map key or {@code Object[]} name element is not a String
     * @see #encode(String, Object)
     * @see #encode(String, Object, Charset, NamingPolicy)
     */
    public static String encode(final String url, final Object parameters, final Charset charset) throws IllegalArgumentException, ClassCastException {
        return encode(url, parameters, charset, NamingPolicy.NO_CHANGE);
    }

    /**
     * Encodes parameters and appends them to a URL as a query string using the specified charset and naming policy.
     * <p>
     * This method takes a base URL and parameters, encodes the parameters using the specified charset and
     * naming policy according to application/x-www-form-urlencoded rules, and appends them to the URL with
     * a '?' separator. If the URL already contains a '?' the encoded parameters are joined with <i>&amp;</i>.
     * Any fragment identifier ({@code #...}) present in the URL is preserved and appended after the parameters.
     * Property/key names are transformed according to the naming policy before encoding.
     * If {@code parameters} is a {@code CharSequence} containing {@code '='}, it is treated as an
     * already URL-encoded query string and appended verbatim (duplicate parameter names are preserved;
     * the naming policy is not applied).
     * If {@code parameters} is {@code null}, an empty Map, an empty CharSequence, an empty Object array, or an
     * object whose encodable fields are all omitted (for example, an all-null bean), the original URL is returned
     * unchanged.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> user = new LinkedHashMap<>();
     * user.put("firstName", "John");
     * String url = URLEncodedUtil.encode("http://api.com/users", user,
     *                                     StandardCharsets.UTF_8, NamingPolicy.SNAKE_CASE);
     * // url: "http://api.com/users?first_name=John"
     * }</pre>
     *
     * @param url the base URL to which the query string will be appended.
     * @param parameters the parameters to encode and append (Map, bean, Object array pairs, String, or any other
     *        value); may be {@code null}. Any other value - including a primitive array and a {@code Collection} -
     *        is converted with {@link N#stringOf(Object)} and encoded as a single valueless form field; it is NOT
     *        split into name/value pairs. An empty {@code Collection} or empty primitive array is therefore
     *        <b>not</b> one of the empty cases below - it encodes as the text {@code []}.
     * @param charset the charset to use for percent-encoding; if {@code null}, defaults to UTF-8.
     * @param namingPolicy the naming policy to transform property/key names (e.g., CAMEL_CASE, SCREAMING_SNAKE_CASE);
     *                     if {@code null} or NO_CHANGE, names are not transformed.
     * @return the URL with the encoded query string appended;
     *         returns the original URL if {@code parameters} is {@code null}, an empty {@code Map},
     *         an empty {@code CharSequence}, an empty {@code Object[]}, or otherwise produces no encoded fields.
     * @throws IllegalArgumentException if {@code url} is {@code null}, or if a Map key or {@code Object[]} name element is {@code null}, an
     *         {@code Object[]} has odd length, an effective name is empty and its value is null, or text to encode contains malformed UTF-16 or a character that
     *         {@code charset} cannot represent
     * @throws ClassCastException if a non-null Map key or {@code Object[]} name element is not a String
     * @see #encode(String, Object, Charset)
     */
    @SuppressWarnings("rawtypes")
    public static String encode(final String url, final Object parameters, final Charset charset, final NamingPolicy namingPolicy)
            throws IllegalArgumentException, ClassCastException {
        N.checkArgNotNull(url, cs.url);

        if (parameters == null || (parameters instanceof Map && ((Map) parameters).isEmpty()) || (parameters instanceof CharSequence seq && seq.isEmpty())
                || (parameters instanceof Object[] a && a.length == 0)) {
            return url;
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            final int fragmentIndex = url.indexOf('#');
            final String baseUrl = fragmentIndex < 0 ? url : url.substring(0, fragmentIndex);
            final String fragment = fragmentIndex < 0 ? Strings.EMPTY : url.substring(fragmentIndex);

            sb.append(baseUrl);

            if (baseUrl.indexOf('?') >= 0) {
                if (!baseUrl.endsWith("?") && !baseUrl.endsWith("&")) {
                    sb.append('&');
                }
            } else {
                sb.append('?');
            }

            final int parameterStart = sb.length();

            if (parameters instanceof CharSequence queryString && queryString.toString().contains(NAME_VALUE_SEPARATOR)) {
                // A pre-built query STRING is documented (HttpRequest.query(String)) as already
                // URL-encoded: append it verbatim. Routing it through the map-based splitter
                // silently dropped duplicate parameter names (a=1&a=2 -> a=2) and re-percent-
                // encoded the already-encoded text (q=a%20b -> q=a%2520b).
                sb.append(queryString);
            } else {
                encode(parameters, charset, namingPolicy, sb);
            }

            if (sb.length() == parameterStart) {
                return url;
            }

            sb.append(fragment);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Encodes the provided parameters into a URL-encoded query string and appends the result to an {@code Appendable} using UTF-8.
     * <p>
     * This method appends the query directly without first materializing the complete encoded query as a String.
     * The parameters are encoded according to application/x-www-form-urlencoded rules and directly appended
     * to the provided output.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * StringBuilder sb = new StringBuilder("http://example.com?");
     * URLEncodedUtil.encode(Map.of("key", "value"), sb);
     * // sb: "http://example.com?key=value"
     * }</pre>
     *
     * <p><b>Note:</b> This overload applies {@link NamingPolicy#NO_CHANGE} as the naming policy, so Map keys,
     * {@code Object[]} names, and bean property names are emitted verbatim ({@code first_name} stays
     * {@code first_name}). To rewrite names, use
     * {@link #encode(Object, Charset, NamingPolicy, Appendable)} with another policy such as {@link NamingPolicy#CAMEL_CASE}.
     *
     * @param parameters the parameters to encode (Map, bean, Object array pairs, String, or any other value); may be
     *        {@code null}. A {@code CharSequence} containing {@code '='} is treated as an already URL-encoded
     *        query string and appended verbatim; one without {@code '='} is encoded as a single form field.
     *        Any other value - including a primitive array and a {@code Collection} - is converted with
     *        {@link N#stringOf(Object)} and encoded as a single valueless form field; it is NOT split into
     *        name/value pairs.
     * @param output the {@code Appendable} (e.g., {@code StringBuilder}, {@code Writer}) to which the encoded query string will be appended.
     * @throws IllegalArgumentException if {@code output}, a Map key, or an {@code Object[]} name element is
     *         {@code null}; if an {@code Object[]} has odd length; if an effective name is empty and its value
     *         is {@code null}; or if text to encode contains malformed UTF-16.
     * @throws ClassCastException if a non-null Map key or {@code Object[]} name element is not a String
     * @throws UncheckedIOException if appending percent-encoded names, values, or separators to {@code output} fails
     * @see #encode(Object, Charset, Appendable)
     */
    public static void encode(final Object parameters, final Appendable output) throws IllegalArgumentException, ClassCastException, UncheckedIOException {
        encode(parameters, IOUtil.DEFAULT_CHARSET, output);
    }

    /**
     * Encodes the provided parameters into a URL-encoded query string and appends the result to an {@code Appendable} using the specified charset.
     * <p>
     * This method appends the query directly without first materializing the complete encoded query as a String.
     * The parameters are encoded using the specified charset according to application/x-www-form-urlencoded rules
     * and directly appended to the provided output.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Writer writer = new FileWriter("query.txt")) {
     *     URLEncodedUtil.encode(Map.of("name", "中文"), StandardCharsets.UTF_8, writer);
     * }
     * // query.txt contains: "name=%E4%B8%AD%E6%96%87"
     * }</pre>
     *
     * <p><b>Note:</b> This overload applies {@link NamingPolicy#NO_CHANGE} as the naming policy, so Map keys,
     * {@code Object[]} names, and bean property names are emitted verbatim ({@code first_name} stays
     * {@code first_name}). To rewrite names, use
     * {@link #encode(Object, Charset, NamingPolicy, Appendable)} with another policy such as {@link NamingPolicy#CAMEL_CASE}.
     *
     * @param parameters the parameters to encode (Map, bean, Object array pairs, String, or any other value); may be
     *        {@code null}. A {@code CharSequence} containing {@code '='} is treated as an already URL-encoded
     *        query string and appended verbatim; one without {@code '='} is encoded as a single form field.
     *        Any other value - including a primitive array and a {@code Collection} - is converted with
     *        {@link N#stringOf(Object)} and encoded as a single valueless form field; it is NOT split into
     *        name/value pairs.
     * @param charset the charset to use for percent-encoding; if {@code null}, defaults to UTF-8.
     * @param output the {@code Appendable} to which the encoded query string will be appended.
     * @throws IllegalArgumentException if {@code output}, a Map key, or an {@code Object[]} name element is
     *         {@code null}; if an {@code Object[]} has odd length; if an effective name is empty and its value
     *         is {@code null}; or if text to encode contains malformed UTF-16 or a character that {@code charset}
     *         cannot represent.
     * @throws ClassCastException if a non-null Map key or {@code Object[]} name element is not a String
     * @throws UncheckedIOException if appending percent-encoded names, values, or separators to {@code output} fails
     * @see #encode(Object, Charset, NamingPolicy, Appendable)
     */
    public static void encode(final Object parameters, final Charset charset, final Appendable output)
            throws IllegalArgumentException, ClassCastException, UncheckedIOException {
        encode(parameters, charset, NamingPolicy.NO_CHANGE, output);
    }

    /**
     * Encodes the provided parameters into a URL-encoded query string and appends the result to an {@code Appendable}
     * using the specified charset and naming policy.
     * <p>
     * This method appends the query directly without first materializing the complete encoded query as a String.
     * Property/key names are transformed according to the naming policy before being percent-encoded using the
     * specified charset according to application/x-www-form-urlencoded rules.
     * </p>
     * <p>
     * The method accepts various parameter formats:
     * <ul>
     * <li>{@code Map<String, ?>}: non-null String keys and their values are encoded as name=value pairs</li>
     * <li>JavaBean: Bean properties are encoded with names transformed by the naming policy</li>
     * <li>{@code Object[]}: pairs of non-null String names and arbitrary values (must have even length)</li>
     * <li>{@code CharSequence}: if it contains {@code '='} it is taken to be an ALREADY-ENCODED query string and is
     *     appended verbatim (nothing is escaped, and duplicate names are preserved); otherwise it is encoded as a
     *     single form field. See the {@code parameters} note below.</li>
     * <li>Any other value - including a primitive array (an {@code int[]} is not an {@code Object[]}), a
     *     {@code Collection} and a bare scalar - is converted with {@link N#stringOf(Object)} and encoded as a
     *     single valueless form field (the text only, with no {@code '='} and no value); it is NOT split into
     *     name/value pairs.</li>
     * </ul>
     * A {@code null} Map value or {@code Object[]} value element is encoded as a valueless token (the name only,
     * without {@code '='}), matching how {@link #decode(String)} parses such a token, so a decode→encode round trip
     * preserves {@code null}. An empty effective name with a null value is rejected because its empty token would disappear on decoding. A String value of {@code "null"} is encoded as {@code name=null}. Null JavaBean
     * properties are omitted.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> user = new LinkedHashMap<>();
     * user.put("firstName", "John");
     * user.put("age", 30);
     * StringBuilder sb = new StringBuilder();
     * URLEncodedUtil.encode(user, StandardCharsets.UTF_8, NamingPolicy.SNAKE_CASE, sb);
     * // sb: "first_name=John&age=30"
     * }</pre>
     *
     * @param parameters the parameters to encode (Map, bean, Object array pairs, String, or any other value); may be
     *        {@code null}. A {@code CharSequence} containing {@code '='} is treated as an already URL-encoded
     *        query string and appended verbatim; one without {@code '='} is encoded as a single form field.
     *        Any other value - including a primitive array and a {@code Collection} - is converted with
     *        {@link N#stringOf(Object)} and encoded as a single valueless form field; it is NOT split into
     *        name/value pairs.
     * @param charset the charset to use for percent-encoding; if {@code null}, defaults to UTF-8.
     * @param namingPolicy the naming policy to transform property/key names (e.g., CAMEL_CASE, SCREAMING_SNAKE_CASE);
     *                     if {@code null} or NO_CHANGE, names are not transformed.
     * @param output the {@code Appendable} to which the encoded query string will be appended.
     * @throws IllegalArgumentException if {@code output} is {@code null}, or if a Map key or {@code Object[]} name element is {@code null}, an
     *         {@code Object[]} has odd length, an effective name is empty and its value is null, or text to encode contains malformed UTF-16 or a character that
     *         {@code charset} cannot represent
     * @throws ClassCastException if a non-null Map key or {@code Object[]} name element is not a String
     * @throws UncheckedIOException if appending percent-encoded names, values, or separators to {@code output} fails
     * @see #encode(Object, Charset, Appendable)
     */
    @SuppressWarnings("rawtypes")
    public static void encode(final Object parameters, final Charset charset, final NamingPolicy namingPolicy, final Appendable output)
            throws IllegalArgumentException, ClassCastException, UncheckedIOException {
        N.checkArgNotNull(output, cs.output);

        if (parameters == null || (parameters instanceof Map && ((Map) parameters).isEmpty())) {
            return;
        }

        final boolean isNoChange = namingPolicy == null || namingPolicy == NamingPolicy.NO_CHANGE;

        try {
            if (parameters instanceof Map) {
                final Map<String, Object> map = (Map<String, Object>) parameters;
                int i = 0;
                for (final Map.Entry<String, Object> entry : map.entrySet()) {
                    final String rawName = requireParameterName(entry.getKey());
                    final String parameterName = isNoChange ? rawName : namingPolicy.convert(rawName);
                    if (parameterName.isEmpty() && entry.getValue() == null) {
                        throw new IllegalArgumentException("An empty parameter name requires a non-null value");
                    }

                    if (i++ > 0) {
                        output.append(QP_SEP_A);
                    }

                    encodeFormFields(parameterName, charset, output);

                    // A null value is a valueless token ("flag"), matching decode: emit the name only so
                    // decode(encode(map)) preserves null instead of turning it into the literal string "null".
                    if (entry.getValue() != null) {
                        output.append(NAME_VALUE_SEPARATOR);

                        encodeFormFields(N.stringOf(entry.getValue()), charset, output);
                    }
                }
            } else if (Beans.isBeanClass(parameters.getClass())) {
                encode(Beans.beanToMap(parameters, true, null, namingPolicy), charset, NamingPolicy.NO_CHANGE, output);
            } else if (parameters instanceof Object[] a) {
                if (0 != (a.length % 2)) {
                    throw new IllegalArgumentException(
                            "The parameters must be the pairs of property name and value, or Map, or a bean class with getter/setter methods.");
                }

                for (int i = 0, len = a.length; i < len; i += 2) {
                    final String rawName = requireParameterName(a[i]);
                    final String parameterName = isNoChange ? rawName : namingPolicy.convert(rawName);
                    if (parameterName.isEmpty() && a[i + 1] == null) {
                        throw new IllegalArgumentException("An empty parameter name requires a non-null value");
                    }

                    if (i > 0) {
                        output.append(QP_SEP_A);
                    }

                    encodeFormFields(parameterName, charset, output);

                    // Same valueless-token rule as the Map path above: a null value emits the name only.
                    if (a[i + 1] != null) {
                        output.append(NAME_VALUE_SEPARATOR);

                        encodeFormFields(N.stringOf(a[i + 1]), charset, output);
                    }
                }
            } else if (parameters instanceof CharSequence source) {
                final String str = source.toString();

                if (str.contains(NAME_VALUE_SEPARATOR)) {
                    // A CharSequence that already reads as a query string ("a=1&b=2") is a pre-built,
                    // already-encoded query and is appended verbatim - the same rule
                    // encode(String, Object, Charset, NamingPolicy) applies. Re-splitting it into
                    // name/value pairs and re-encoding them escaped the existing escapes a second time
                    // ("q=a%20b" -> "q=a%2520b"), so the two overloads disagreed on the same input.
                    output.append(str);
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

    // Renders a caller-supplied query token for an exception message: bounded to 64 UTF-16 code units, with
    // every ISO control character, U+2028/U+2029 and unpaired surrogate replaced by a backslash-u hex escape.
    // Mirrors Numbers.escapeForErrorMessage, which exists for the same reason and is private to its own class.
    private static String forErrorMessage(final String token) {
        final String bounded = Strings.abbreviate(token, 64);

        if (Strings.isEmpty(bounded)) {
            return bounded;
        }

        StringBuilder sb = null;

        for (int i = 0, len = bounded.length(); i < len; i++) {
            final char ch = bounded.charAt(i);

            // a well-formed surrogate pair (an emoji, say) is printable: pass it through together
            if (Character.isHighSurrogate(ch) && i + 1 < len && Character.isLowSurrogate(bounded.charAt(i + 1))) {
                if (sb != null) {
                    sb.append(ch).append(bounded.charAt(i + 1));
                }

                i++;
            } else if (Character.isISOControl(ch) || ch == 0x2028 || ch == 0x2029 || Character.isSurrogate(ch)) {
                if (sb == null) {
                    sb = new StringBuilder(len + 16).append(bounded, 0, i);
                }

                sb.append("\\u")
                        .append(Character.toUpperCase(Character.forDigit((ch >> 12) & 0xF, RADIX)))
                        .append(Character.toUpperCase(Character.forDigit((ch >> 8) & 0xF, RADIX)))
                        .append(Character.toUpperCase(Character.forDigit((ch >> 4) & 0xF, RADIX)))
                        .append(Character.toUpperCase(Character.forDigit(ch & 0xF, RADIX)));
            } else if (sb != null) {
                sb.append(ch);
            }
        }

        return sb == null ? bounded : sb.toString();
    }

    /**
     * Requires a non-null string parameter name.
     *
     * @throws IllegalArgumentException if {@code name} is {@code null}
     * @throws ClassCastException if {@code name} is not a {@link String}
     */
    private static String requireParameterName(final Object name) throws IllegalArgumentException, ClassCastException {
        if (name == null) {
            throw new IllegalArgumentException("Parameter name must not be null");
        }

        return (String) name;
    }

    /**
     * Encodes a single field value using {@code application/x-www-form-urlencoded} rules.
     * Alphanumeric characters and the characters {@code -}, {@code _}, {@code .}, and {@code *}
     * are passed through unchanged; space is converted to {@code '+'}, and all other bytes
     * are percent-encoded as {@code %XX} using the given charset. Malformed UTF-16 and characters that the charset
     * cannot represent are rejected rather than replaced.
     *
     * @param content the string to encode; {@code null} is written as the literal text {@code "null"}.
     * @param charset the charset used to convert characters to bytes before percent-encoding;
     *                if {@code null}, defaults to UTF-8.
     * @param output the {@code Appendable} to which the encoded content is appended.
     * @throws IllegalArgumentException if {@code content} contains malformed UTF-16 or a character that the selected
     *         charset cannot represent
     * @throws IOException if appending percent-encoded names, values, or separators to {@code output} fails
     */
    private static void encodeFormFields(final String content, final Charset charset, final Appendable output) throws IllegalArgumentException, IOException {
        urlEncode(content, (charset != null) ? charset : IOUtil.DEFAULT_CHARSET, URL_ENCODER, true, output);
    }

    /**
     * @throws IllegalArgumentException if {@code charset} is null, or {@code content} contains malformed UTF-16
     *         or characters that {@code charset} cannot represent
     * @throws IOException if appending the encoded characters to {@code output} fails
     */
    private static void urlEncode(final String content, final Charset charset, final BitSet safeChars, final boolean blankAsPlus, final Appendable output)
            throws IllegalArgumentException, IOException {
        N.checkArgNotNull(charset, cs.charset);

        if (content == null) {
            output.append(Strings.NULL);

            return;
        }

        // Safe characters are ASCII and therefore identical in any ASCII-superset charset, so they pass
        // through literally. Every byte of a run of non-safe characters is percent-escaped, so the decoder
        // always sees a contiguous %XX run holding the characters' complete encoded byte sequence. Emitting
        // individual safe-valued bytes from a multi-byte encoding (e.g. the 0x61 in UTF-16's 00 61) would
        // split a character across the literal/escape boundary and make the run undecodable.
        for (int i = 0, len = content.length(); i < len;) {
            final char ch = content.charAt(i);

            if (ch < 128 && safeChars.get(ch)) {
                output.append(ch);
                i++;
            } else if (blankAsPlus && ch == ' ') {
                output.append('+');
                i++;
            } else {
                int runEnd = i + 1;

                while (runEnd < len) {
                    final char next = content.charAt(runEnd);

                    if (next < 128 && (safeChars.get(next) || (blankAsPlus && next == ' '))) {
                        break;
                    }

                    runEnd++;
                }

                final ByteBuffer bb;

                try {
                    bb = charset.newEncoder()
                            .onMalformedInput(CodingErrorAction.REPORT)
                            .onUnmappableCharacter(CodingErrorAction.REPORT)
                            .encode(CharBuffer.wrap(content, i, runEnd));
                } catch (final CharacterCodingException e) {
                    throw new IllegalArgumentException(
                            "Input contains malformed UTF-16 or a character not representable in " + charset.name() + " in the run beginning at index " + i, e);
                }

                while (bb.hasRemaining()) {
                    final int b = bb.get() & 0xff;
                    output.append('%');

                    final char hex1 = Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, RADIX));
                    final char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, RADIX));
                    output.append(hex1);
                    output.append(hex2);
                }

                i = runEnd;
            }
        }
    }

    /**
     * Encodes a string using the {@link #USERINFO} safe-character set (unreserved characters plus
     * punctuation: {@code , ; : $ & + =}). Spaces are percent-encoded rather than converted to
     * {@code '+'}, as required for the {@code userinfo} component of a URI.
     *
     * <p>Used internally to encode the {@code userinfo} segment of a URI (the
     * {@code username:password} portion before the {@code @} in the authority).</p>
     *
     * @param content the string to encode; {@code null} is written as the literal text {@code "null"}.
     * @param charset the charset used to convert characters to bytes before percent-encoding;
     *                must not be {@code null}.
     * @param output the {@code Appendable} to which the encoded content is appended.
     * @throws IllegalArgumentException if {@code charset} is {@code null}, or if {@code content} contains malformed
     *         UTF-16 or a character that the selected charset cannot represent
     * @throws IOException if appending percent-encoded names, values, or separators to {@code output} fails
     */
    static void encUserInfo(final String content, final Charset charset, final Appendable output) throws IllegalArgumentException, IOException {
        urlEncode(content, charset, USERINFO, false, output);
    }

    /**
     * Encodes a string using the {@link #URIC} safe-character set (the union of
     * {@link #RESERVED} and {@link #UNRESERVED} characters as defined in RFC 2396).
     * Spaces are percent-encoded rather than converted to {@code '+'}.
     *
     * <p>Used internally to encode the query and fragment segments of a URI.</p>
     *
     * @param content the string to encode; {@code null} is written as the literal text {@code "null"}.
     * @param charset the charset used to convert characters to bytes before percent-encoding;
     *                must not be {@code null}.
     * @param output the {@code Appendable} to which the encoded content is appended.
     * @throws IllegalArgumentException if {@code charset} is {@code null}, or if {@code content} contains malformed
     *         UTF-16 or a character that the selected charset cannot represent
     * @throws IOException if appending percent-encoded names, values, or separators to {@code output} fails
     */
    static void encUric(final String content, final Charset charset, final Appendable output) throws IllegalArgumentException, IOException {
        urlEncode(content, charset, URIC, false, output);
    }

    /**
     * Encodes a string using the {@link #PATH_SAFE} safe-character set (unreserved characters
     * plus {@code / ; : @ & = + $ ,}). Spaces are percent-encoded rather than converted to
     * {@code '+'}, preserving valid path separator and parameter syntax.
     *
     * <p>Used internally to encode a URI path while preserving path separators.</p>
     *
     * @param content the string to encode; {@code null} is written as the literal text {@code "null"}.
     * @param charset the charset used to convert characters to bytes before percent-encoding;
     *                must not be {@code null}.
     * @param output the {@code Appendable} to which the encoded content is appended.
     * @throws IllegalArgumentException if {@code charset} is {@code null}, or if {@code content} contains malformed
     *         UTF-16 or a character that the selected charset cannot represent
     * @throws IOException if appending percent-encoded names, values, or separators to {@code output} fails
     */
    static void encPath(final String content, final Charset charset, final Appendable output) throws IllegalArgumentException, IOException {
        urlEncode(content, charset, PATH_SAFE, false, output);
    }

    /**
     * Decodes a URL-encoded query string into a {@code Map<String, String>} using UTF-8.
     * <p>
     * This method parses a URL query string (e.g., "name=value&amp;foo=bar") and converts it into a map
     * where keys are parameter names and values are parameter values. Both '+' characters and <i>%XX</i> sequences
     * are decoded. Leading and trailing whitespace in parameter names and values is preserved. If a parameter appears multiple times,
     * only the last occurrence is retained (use {@link #decodeToMultimap(String)} to preserve all values).
     * </p>
     * <p>
     * Supports both <i>&amp;</i> and ';' as parameter separators (the semicolon separator follows
     * the W3C HTML 4.01 recommendation, Appendix B.2.2).
     * </p>
     * <p>Decoding is strict: a percent escape must contain two ASCII hexadecimal digits, and each
     * contiguous run of encoded bytes must be valid UTF-8. Use {@link #decodeLenient(String)} to
     * preserve malformed escapes and replace malformed byte sequences.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> params = URLEncodedUtil.decode("name=John+Doe&age=30");
     * // params: {name=John Doe, age=30}
     * }</pre>
     *
     * @param urlQuery the URL query string to decode (e.g., "key1=value1&amp;key2=value2"), may be {@code null} or empty.
     * @return a {@code LinkedHashMap} containing parameter names as keys and decoded parameter values as values;
     *         returns an empty map if {@code urlQuery} is {@code null} or empty. A token without
     *         {@code '='} is stored with a {@code null} value.
     * @throws IllegalArgumentException if a percent escape or its UTF-8 byte sequence is malformed
     * @see #decode(String, Charset)
     * @see #decodeToMultimap(String)
     * @see URLDecoder#decode(String, String)
     */
    public static Map<String, String> decode(final String urlQuery) throws IllegalArgumentException {
        return decode(urlQuery, IOUtil.DEFAULT_CHARSET);
    }

    /**
     * Decodes a URL-encoded query string into a {@code Map<String, String>} using the specified charset.
     * <p>
     * This method parses a URL query string and converts it into a map where keys are parameter names
     * and values are parameter values. Both '+' characters and <i>%XX</i> sequences are decoded using the
     * specified charset. Leading and trailing whitespace in parameter names and values is preserved. If a parameter appears
     * multiple times, only the last occurrence is retained.
     * </p>
     * <p>
     * Supports both <i>&amp;</i> and ';' as parameter separators (the semicolon separator follows
     * the W3C HTML 4.01 recommendation, Appendix B.2.2).
     * </p>
     * <p>Decoding is strict: percent escapes accept ASCII hexadecimal digits only, and encoded bytes
     * must be valid in the selected charset. Use {@link #decodeLenient(String, Charset)} for explicit
     * replacement/preservation behavior.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> params = URLEncodedUtil.decode("name=%E4%B8%AD%E6%96%87", StandardCharsets.UTF_8);
     * // params: {name=中文}
     * }</pre>
     *
     * @param urlQuery the URL query string to decode, may be {@code null} or empty.
     * @param charset the charset to use for decoding percent-encoded characters; if {@code null}, defaults to UTF-8.
     * @return a {@code LinkedHashMap} containing parameter names as keys and decoded parameter values as values;
     *         returns an empty map if {@code urlQuery} is {@code null} or empty. A token without
     *         {@code '='} is stored with a {@code null} value.
     * @throws IllegalArgumentException if a percent escape or its encoded byte sequence is malformed
     * @see #decode(String)
     * @see #decode(String, Charset, Supplier)
     * @see URLDecoder#decode(String, Charset)
     */
    public static Map<String, String> decode(final String urlQuery, final Charset charset) throws IllegalArgumentException {
        return decode(urlQuery, charset, Suppliers.of(LinkedHashMap::new));
    }

    /**
     * Leniently decodes a URL-encoded query string into a {@code Map<String, String>} using UTF-8.
     * Invalid or incomplete percent escapes are retained literally, and malformed encoded byte
     * sequences are replaced according to the charset decoder's replacement policy. Only ASCII
     * hexadecimal digits are recognized in percent escapes.
     *
     * @param urlQuery the URL query string to decode, may be {@code null} or empty
     * @return a {@code LinkedHashMap} containing the decoded parameters
     * @see #decode(String)
     * @see #decodeLenient(String, Charset)
     */
    public static Map<String, String> decodeLenient(final String urlQuery) {
        return decodeLenient(urlQuery, IOUtil.DEFAULT_CHARSET);
    }

    /**
     * Leniently decodes a URL-encoded query string into a {@code Map<String, String>}.
     * Invalid or incomplete percent escapes are retained literally, and malformed encoded byte
     * sequences are replaced. Only ASCII hexadecimal digits are recognized in percent escapes.
     *
     * @param urlQuery the URL query string to decode, may be {@code null} or empty
     * @param charset the charset used to decode percent-encoded bytes; {@code null} selects UTF-8
     * @return a {@code LinkedHashMap} containing the decoded parameters
     * @see #decode(String, Charset)
     */
    public static Map<String, String> decodeLenient(final String urlQuery, final Charset charset) {
        return decodeLenient(urlQuery, charset, Suppliers.of(LinkedHashMap::new));
    }

    /**
     * Decodes a URL-encoded query string into a custom {@code Map} implementation using the specified charset and map supplier.
     * <p>
     * This method provides flexibility in choosing the Map implementation (e.g., {@code TreeMap}, {@code HashMap}, etc.)
     * by accepting a custom supplier. Both '+' characters and <i>%XX</i> sequences are decoded using the specified charset.
     * Leading and trailing whitespace in parameter names and values is preserved. If a parameter appears multiple times, only the last
     * occurrence is retained.
     * </p>
     * <p>
     * Supports both <i>&amp;</i> and ';' as parameter separators (the semicolon separator follows
     * the W3C HTML 4.01 recommendation, Appendix B.2.2).
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TreeMap<String, String> params = URLEncodedUtil.decode("b=2&a=1", StandardCharsets.UTF_8, TreeMap::new);
     * // params: {a=1, b=2} (sorted by key)
     * }</pre>
     *
     * @param <M> the type of the Map to return, must extend {@code Map<String, String>}.
     * @param urlQuery the URL query string to decode, may be {@code null} or empty.
     * @param charset the charset to use for decoding percent-encoded characters; if {@code null}, defaults to UTF-8.
     * @param mapSupplier a supplier that provides an instance of the desired Map implementation; must not be
     *         {@code null} and must not return {@code null}.
     * @return a Map of type M containing parameter names as keys and decoded parameter values as values;
     *         returns an empty map (from supplier) if {@code urlQuery} is {@code null} or empty. A token
     *         without {@code '='} is stored with a {@code null} value, so the supplied {@code Map} must
     *         tolerate {@code null} values.
     * @throws IllegalArgumentException if {@code mapSupplier} is {@code null} or returns {@code null},
     *         or if a percent escape or its encoded byte sequence is malformed
     * @throws NullPointerException if the {@code Map} returned by {@code mapSupplier} does not permit
     *         {@code null} values (for example {@code Hashtable} or {@code ConcurrentHashMap}) and
     *         {@code urlQuery} contains a token without {@code '='}; use a {@code null}-tolerant
     *         {@code Map} or {@link #decodeToMultimap(String)} instead
     * @see #decode(String, Charset)
     */
    public static <M extends Map<String, String>> M decode(final String urlQuery, final Charset charset, final Supplier<M> mapSupplier)
            throws IllegalArgumentException, NullPointerException {
        return decode(urlQuery, charset, mapSupplier, true);
    }

    /**
     * Lenient counterpart to {@link #decode(String, Charset, Supplier)}.
     * Invalid or incomplete percent escapes are retained literally, and malformed encoded byte
     * sequences are replaced. Only ASCII hexadecimal digits are recognized in percent escapes.
     *
     * @param <M> the map type
     * @param urlQuery the URL query string to decode, may be {@code null} or empty
     * @param charset the charset used to decode percent-encoded bytes; {@code null} selects UTF-8
     * @param mapSupplier supplier for the result map; must not be {@code null} and must not return {@code null}
     * @return the supplied map populated with decoded parameters; a token without {@code '='} is stored
     *         with a {@code null} value, so the supplied {@code Map} must tolerate {@code null} values
     * @throws IllegalArgumentException if {@code mapSupplier} is {@code null} or returns {@code null}
     * @throws NullPointerException if the {@code Map} returned by {@code mapSupplier} does not permit
     *         {@code null} values (for example {@code Hashtable} or {@code ConcurrentHashMap}) and
     *         {@code urlQuery} contains a token without {@code '='}; use a {@code null}-tolerant
     *         {@code Map} or {@link #decodeToMultimap(String)} instead
     */
    public static <M extends Map<String, String>> M decodeLenient(final String urlQuery, final Charset charset, final Supplier<M> mapSupplier)
            throws IllegalArgumentException, NullPointerException {
        return decode(urlQuery, charset, mapSupplier, false);
    }

    /**
     * @throws IllegalArgumentException if {@code mapSupplier} is null or returns null, or strict decoding rejects malformed input
     * @throws NullPointerException if a valueless query parameter is inserted into a map that does not permit null values
     */
    private static <M extends Map<String, String>> M decode(final String urlQuery, final Charset charset, final Supplier<M> mapSupplier, final boolean strict)
            throws IllegalArgumentException, NullPointerException {
        N.checkArgNotNull(mapSupplier, cs.mapSupplier);

        final M result = N.checkArgNotNull(mapSupplier.get(), "mapSupplier result");

        if (Strings.isEmpty(urlQuery)) {
            return result;
        }

        forEachDecodedQueryParameter(urlQuery, charset, strict, (name, value) -> {
            if (value == null) {
                // A token without '=' is documented to be stored with a null value, but a null-hostile Map
                // (Hashtable/ConcurrentHashMap/ConcurrentSkipListMap - which ConcurrentMap.class and
                // ConcurrentNavigableMap.class also resolve to) rejects it with a message-less NPE, against
                // this class's promise of descriptive NPE messages. The type stays NullPointerException
                // because Map.put is required to throw it for a value the map rejects; only the message is
                // added. Gated on value == null so an unrelated NPE from the map is not misreported.
                try {
                    result.put(name, null);
                } catch (final NullPointerException e) {
                    // The token is a decoded piece of the query, i.e. caller- and in any server-side use
                    // attacker-controlled, so it is bounded and its control characters escaped before it is
                    // interpolated: a raw token could otherwise forge lines in a log that records this message,
                    // and a huge one would allocate a message as large as the query.
                    final NullPointerException npe = new NullPointerException(
                            "The Map created for this call (" + result.getClass().getName() + ") does not permit null values, but the query contains"
                                    + " the valueless token \"" + forErrorMessage(name) + "\". Use a null-tolerant Map or decodeToMultimap(..).");
                    npe.initCause(e);
                    throw npe;
                }
            } else {
                result.put(name, value);
            }
        });

        return result;
    }

    /**
     * Decodes a URL-encoded query string into a {@code ListMultimap<String, String>} using UTF-8.
     * <p>
     * This method parses a URL query string and converts it into a multimap where keys are parameter names
     * and values are lists of parameter values. Unlike {@link #decode(String)}, this method preserves all
     * values when a parameter appears multiple times in the query string. Both '+' characters and <i>%XX</i>
     * sequences are decoded. Leading and trailing whitespace in parameter names and values is preserved.
     * </p>
     * <p>
     * Supports both <i>&amp;</i> and ';' as parameter separators (the semicolon separator follows
     * the W3C HTML 4.01 recommendation, Appendix B.2.2).
     * </p>
     * <p>Percent escapes and UTF-8 byte sequences are validated strictly. Use
     * {@link #decodeToMultimapLenient(String)} for explicit lenient handling.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, String> params = URLEncodedUtil.decodeToMultimap("color=red&color=blue&size=L");
     * // params: {color=[red, blue], size=[L]}
     * }</pre>
     *
     * @param urlQuery the URL query string to decode (e.g., "color=red&amp;color=blue&amp;size=L"), may be {@code null} or empty.
     * @return a {@code ListMultimap} containing parameter names as keys and lists of decoded parameter values;
     *         returns an empty multimap if {@code urlQuery} is {@code null} or empty. A token without
     *         {@code '='} is stored with a {@code null} value.
     * @throws IllegalArgumentException if a percent escape or its UTF-8 byte sequence is malformed
     * @see #decodeToMultimap(String, Charset)
     * @see #decode(String)
     */
    public static ListMultimap<String, String> decodeToMultimap(final String urlQuery) throws IllegalArgumentException {
        return decodeToMultimap(urlQuery, IOUtil.DEFAULT_CHARSET);
    }

    /**
     * Decodes a URL-encoded query string into a {@code ListMultimap<String, String>} using the specified charset.
     * <p>
     * This method parses a URL query string and converts it into a multimap where keys are parameter names
     * and values are lists of parameter values. Unlike {@link #decode(String, Charset)}, this method preserves
     * all values when a parameter appears multiple times in the query string. Both '+' characters and <i>%XX</i>
     * sequences are decoded using the specified charset. Leading and trailing whitespace in parameter names and values is preserved.
     * </p>
     * <p>
     * Supports both <i>&amp;</i> and ';' as parameter separators (the semicolon separator follows
     * the W3C HTML 4.01 recommendation, Appendix B.2.2).
     * </p>
     * <p>Percent escapes accept ASCII hexadecimal digits only, and encoded byte sequences are
     * validated strictly in the selected charset.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, String> params = URLEncodedUtil.decodeToMultimap("tag=java&tag=url", StandardCharsets.UTF_8);
     * // params: {tag=[java, url]}
     * }</pre>
     *
     * @param urlQuery the URL query string to decode, may be {@code null} or empty.
     * @param charset the charset to use for decoding percent-encoded characters; if {@code null}, defaults to UTF-8.
     * @return a {@code ListMultimap} containing parameter names as keys and lists of decoded parameter values;
     *         returns an empty multimap if {@code urlQuery} is {@code null} or empty. A token without
     *         {@code '='} is stored with a {@code null} value.
     * @throws IllegalArgumentException if a percent escape or its encoded byte sequence is malformed
     * @see #decodeToMultimap(String)
     * @see #decode(String, Charset)
     */
    public static ListMultimap<String, String> decodeToMultimap(final String urlQuery, final Charset charset) throws IllegalArgumentException {
        return decodeToMultimap(urlQuery, charset, true);
    }

    /**
     * Leniently decodes a URL-encoded query string into a {@code ListMultimap<String, String>}
     * using UTF-8, preserving repeated parameters in encounter order.
     *
     * @param urlQuery the URL query string to decode, may be {@code null} or empty
     * @return a multimap containing all decoded parameter values
     * @see #decodeToMultimap(String)
     */
    public static ListMultimap<String, String> decodeToMultimapLenient(final String urlQuery) {
        return decodeToMultimapLenient(urlQuery, IOUtil.DEFAULT_CHARSET);
    }

    /**
     * Leniently decodes a URL-encoded query string into a {@code ListMultimap<String, String>},
     * preserving repeated parameters in encounter order. Invalid or incomplete percent escapes
     * are retained literally, and malformed encoded byte sequences are replaced.
     *
     * @param urlQuery the URL query string to decode, may be {@code null} or empty
     * @param charset the charset used to decode percent-encoded bytes; {@code null} selects UTF-8
     * @return a multimap containing all decoded parameter values
     * @see #decodeToMultimap(String, Charset)
     */
    public static ListMultimap<String, String> decodeToMultimapLenient(final String urlQuery, final Charset charset) {
        return decodeToMultimap(urlQuery, charset, false);
    }

    private static ListMultimap<String, String> decodeToMultimap(final String urlQuery, final Charset charset, final boolean strict) {
        final ListMultimap<String, String> result = N.newLinkedListMultimap();

        if (Strings.isEmpty(urlQuery)) {
            return result;
        }

        forEachDecodedQueryParameter(urlQuery, charset, strict, result::put);

        return result;
    }

    /**
     * Decodes a URL-encoded query string into a bean or Map of the specified type using UTF-8.
     * <p>
     * This method parses a URL query string and populates a JavaBean or Map instance with the decoded parameters.
     * Parameter names are matched to bean property names (exact match first, falling back to case-insensitive matching). Values are automatically converted
     * to the appropriate property types using the bean's property information. Both '+' characters and <i>%XX</i>
     * sequences are decoded. Leading and trailing whitespace in parameter names and values is preserved.
     * </p>
     * <p>
     * Supports both <i>&amp;</i> and ';' as parameter separators (the semicolon separator follows
     * the W3C HTML 4.01 recommendation, Appendix B.2.2).
     * </p>
     * <p>Percent escapes and UTF-8 byte sequences are validated strictly.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Given a public User JavaBean with a no-arg constructor and name/age accessors:
     * User user = URLEncodedUtil.decode("name=John&age=30", User.class);
     * // user.getName() returns "John" and user.getAge() returns 30
     * }</pre>
     *
     * @param <T> the type of the object to decode into (JavaBean or Map).
     * @param urlQuery the URL query string to decode, may be {@code null} or empty.
     * @param targetType the class of the bean or Map to create and populate; must not be {@code null}.
     * @return an instance of type T populated with the decoded parameter values;
     *         returns an empty instance if {@code urlQuery} is {@code null} or empty.
     * @throws IllegalArgumentException if {@code targetType} is {@code null}, neither a {@code Map} type nor a
     *         supported bean class, a {@code Map} type for which no assignable mutable instance can be created
     *         (for example {@link ImmutableMap}, {@link ImmutableSortedMap}, {@link ImmutableNavigableMap} or
     *         {@link java.util.EnumMap}), a percent escape or its UTF-8 byte sequence is malformed, or conversion
     *         fails
     * @throws NullPointerException if the {@code Map} created for {@code targetType} does not permit
     *         {@code null} values (for example {@code Hashtable}, {@code ConcurrentHashMap} or
     *         {@code ConcurrentSkipListMap}, which {@code ConcurrentMap.class} and
     *         {@code ConcurrentNavigableMap.class} also resolve to) and {@code urlQuery} contains a token
     *         without {@code '='}; use a {@code null}-tolerant {@code Map} or
     *         {@link #decodeToMultimap(String)} instead
     * @see #decode(String, Charset, Class)
     */
    public static <T> T decode(final String urlQuery, final Class<? extends T> targetType) throws IllegalArgumentException, NullPointerException {
        return decode(urlQuery, IOUtil.DEFAULT_CHARSET, targetType);
    }

    /**
     * Decodes a URL-encoded query string into a bean or Map of the specified type using the specified charset.
     * <p>
     * This method parses a URL query string and populates a JavaBean or Map instance with the decoded parameters.
     * Parameter names are matched to bean property names (exact match first, falling back to case-insensitive matching). Values are automatically converted
     * to the appropriate property types using the bean's property information. Both '+' characters and <i>%XX</i>
     * sequences are decoded using the specified charset. Leading and trailing whitespace in parameter names and values is preserved.
     * </p>
     * <p>
     * If {@code targetType} is a Map class, this method delegates to {@link #decode(String, Charset, Supplier)}.
     * Otherwise, it creates and populates a bean instance.
     * </p>
     * <p>
     * Supports both <i>&amp;</i> and ';' as parameter separators (the semicolon separator follows
     * the W3C HTML 4.01 recommendation, Appendix B.2.2).
     * </p>
     * <p>Percent escapes accept ASCII hexadecimal digits only, and encoded byte sequences are
     * validated strictly in the selected charset.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Given a public Product JavaBean with a no-arg constructor and name/price accessors:
     * Product p = URLEncodedUtil.decode("name=Laptop&price=999.99", StandardCharsets.UTF_8, Product.class);
     * // p.getName() returns "Laptop" and p.getPrice() returns 999.99
     * }</pre>
     *
     * @param <T> the type of the object to decode into (JavaBean or Map).
     * @param urlQuery the URL query string to decode, may be {@code null} or empty.
     * @param charset the charset to use for decoding percent-encoded characters; if {@code null}, defaults to UTF-8.
     * @param targetType the class of the bean or Map to create and populate; must not be {@code null}.
     * @return an instance of type T populated with the decoded parameter values;
     *         returns an empty instance if {@code urlQuery} is {@code null} or empty.
     * @throws IllegalArgumentException if {@code targetType} is {@code null}, neither a {@code Map} type nor a
     *         supported bean class, a {@code Map} type for which no assignable mutable instance can be created
     *         (for example {@link ImmutableMap}, {@link ImmutableSortedMap}, {@link ImmutableNavigableMap} or
     *         {@link java.util.EnumMap}), a percent escape or its encoded byte sequence is malformed, or conversion
     *         fails
     * @throws NullPointerException if the {@code Map} created for {@code targetType} does not permit
     *         {@code null} values (for example {@code Hashtable}, {@code ConcurrentHashMap} or
     *         {@code ConcurrentSkipListMap}, which {@code ConcurrentMap.class} and
     *         {@code ConcurrentNavigableMap.class} also resolve to) and {@code urlQuery} contains a token
     *         without {@code '='}; use a {@code null}-tolerant {@code Map} or
     *         {@link #decodeToMultimap(String)} instead
     * @see #decode(String, Class)
     */
    public static <T> T decode(final String urlQuery, final Charset charset, final Class<? extends T> targetType)
            throws IllegalArgumentException, NullPointerException {
        return decode(urlQuery, charset, targetType, true);
    }

    /**
     * Leniently decodes a URL-encoded query string into a bean or map using UTF-8.
     *
     * @param <T> the target type
     * @param urlQuery the URL query string to decode, may be {@code null} or empty
     * @param targetType the supported bean or map type; must not be {@code null}
     * @return a populated target instance
     * @throws IllegalArgumentException if {@code targetType} is invalid - including a {@code Map} type for which
     *         no assignable mutable instance can be created, such as {@link ImmutableMap} or
     *         {@link java.util.EnumMap} - or a decoded value cannot be converted
     * @throws NullPointerException if the {@code Map} created for {@code targetType} does not permit
     *         {@code null} values (for example {@code Hashtable}, {@code ConcurrentHashMap} or
     *         {@code ConcurrentSkipListMap}, which {@code ConcurrentMap.class} and
     *         {@code ConcurrentNavigableMap.class} also resolve to) and {@code urlQuery} contains a token
     *         without {@code '='}; use a {@code null}-tolerant {@code Map} or
     *         {@link #decodeToMultimap(String)} instead
     * @see #decode(String, Class)
     */
    public static <T> T decodeLenient(final String urlQuery, final Class<? extends T> targetType) throws IllegalArgumentException, NullPointerException {
        return decodeLenient(urlQuery, IOUtil.DEFAULT_CHARSET, targetType);
    }

    /**
     * Leniently decodes a URL-encoded query string into a bean or map. Invalid or incomplete
     * percent escapes are retained literally, and malformed encoded byte sequences are replaced.
     *
     * @param <T> the target type
     * @param urlQuery the URL query string to decode, may be {@code null} or empty
     * @param charset the charset used to decode percent-encoded bytes; {@code null} selects UTF-8
     * @param targetType the supported bean or map type; must not be {@code null}
     * @return a populated target instance
     * @throws IllegalArgumentException if {@code targetType} is invalid - including a {@code Map} type for which
     *         no assignable mutable instance can be created, such as {@link ImmutableMap} or
     *         {@link java.util.EnumMap} - or a decoded value cannot be converted
     * @throws NullPointerException if the {@code Map} created for {@code targetType} does not permit
     *         {@code null} values (for example {@code Hashtable}, {@code ConcurrentHashMap} or
     *         {@code ConcurrentSkipListMap}, which {@code ConcurrentMap.class} and
     *         {@code ConcurrentNavigableMap.class} also resolve to) and {@code urlQuery} contains a token
     *         without {@code '='}; use a {@code null}-tolerant {@code Map} or
     *         {@link #decodeToMultimap(String)} instead
     * @see #decode(String, Charset, Class)
     */
    public static <T> T decodeLenient(final String urlQuery, final Charset charset, final Class<? extends T> targetType)
            throws IllegalArgumentException, NullPointerException {
        return decode(urlQuery, charset, targetType, false);
    }

    /**
     * @throws IllegalArgumentException if {@code targetType} is null, a mutable instance of the requested map type
     *         cannot be created, the target is not a map or bean type, or strict decoding rejects malformed input
     * @throws NullPointerException if a valueless query parameter is inserted into a map that does not permit null values
     */
    @SuppressWarnings("rawtypes")
    private static <T> T decode(final String urlQuery, final Charset charset, final Class<? extends T> targetType, final boolean strict)
            throws IllegalArgumentException, NullPointerException {
        N.checkArgNotNull(targetType, cs.targetType);

        if (Map.class.isAssignableFrom(targetType)) {
            // Suppliers.ofMap maps the bare Map/AbstractMap interfaces to HashMap, which would make
            // decode(q, charset, Map.class) lose the parameter order that every other decode overload
            // preserves. Only the unspecific types are redirected; a concrete request is honored, or rejected
            // just below if no assignable instance can be created for it.
            final Supplier<Map<String, String>> supplier;

            if (Map.class.equals(targetType) || AbstractMap.class.equals(targetType)) {
                supplier = Suppliers.of(LinkedHashMap::new);
            } else {
                supplier = Suppliers.ofMap((Class) targetType);
            }

            // Suppliers.ofMap substitutes a plain HashMap/TreeMap for the Map types it cannot build
            // (EnumMap, ImmutableMap, ImmutableSortedMap, ImmutableNavigableMap), and the unchecked cast
            // below erases, so the caller used to get a bare ClassCastException in its own frame. Reject
            // them here, the way Suppliers.ofMap already rejects the sibling ImmutableBiMap.
            final Map<String, String> result = supplier.get();

            if (!targetType.isInstance(result)) {
                throw new IllegalArgumentException("Cannot decode into " + targetType.getName() + ": no mutable instance of that type can be created");
            }

            return (T) decode(urlQuery, charset, () -> result, strict);
        }

        final BeanInfo beanInfo = ParserUtil.getBeanInfo(targetType);
        final Object result = beanInfo.createBeanResult();

        if (Strings.isEmpty(urlQuery)) {
            return beanInfo.finishBeanResult(result);
        }

        forEachDecodedQueryParameter(urlQuery, charset, strict, (name, value) -> {
            final PropInfo propInfo = beanInfo.getPropInfo(name);
            final Object propValue = value == null ? propInfo == null ? null : propInfo.type.defaultValue()
                    : propInfo == null ? value : propInfo.readPropValue(value);

            beanInfo.setPropValue(result, name, propValue, true);
        });

        return beanInfo.finishBeanResult(result);
    }

    /**
     * Visits the non-empty query tokens separated by {@code '&'} or {@code ';'} after form decoding.
     * Empty tokens caused by leading, trailing, or consecutive separators are skipped, while whitespace-only
     * tokens are deliberately preserved. A token without {@code '='} is reported with a {@code null} value.
     *
     * @param urlQuery the raw query string, without any leading {@code '?'}; must not be {@code null}
     * @param charset the charset used to decode percent escapes and {@code '+'}
     * @param strict {@code true} to reject a malformed percent escape with an
     *        {@link IllegalArgumentException}; {@code false} to pass it through unchanged
     * @param action receives each decoded name and its value, or {@code null} for a valueless token
     */
    private static void forEachDecodedQueryParameter(final String urlQuery, final Charset charset, final boolean strict,
            final BiConsumer<String, String> action) {
        final int len = urlQuery.length();
        int tokenStart = 0;
        // The first '=' of the current token, tracked while the token is being scanned. Looking it up with
        // indexOf(..) instead searched to the END of the whole query and then discarded anything past the
        // token, so a query of valueless tokens ("a&b&c&...") re-scanned the tail once per token - quadratic
        // in the query length.
        int separatorIndex = -1;

        for (int tokenEnd = 0; tokenEnd <= len; tokenEnd++) {
            if (tokenEnd < len) {
                final char ch = urlQuery.charAt(tokenEnd);

                if (ch != QP_SEP_A && ch != QP_SEP_S) {
                    if (separatorIndex < tokenStart && ch == '=') {
                        separatorIndex = tokenEnd;
                    }

                    continue;
                }
            }

            if (tokenStart < tokenEnd) {
                final String name;
                final String value;

                if (separatorIndex < tokenStart) {
                    name = decodeFormFields(urlQuery.substring(tokenStart, tokenEnd), charset, strict);
                    value = null;
                } else {
                    name = decodeFormFields(urlQuery.substring(tokenStart, separatorIndex), charset, strict);
                    value = decodeFormFields(urlQuery.substring(separatorIndex + 1, tokenEnd), charset, strict);
                }

                action.accept(name, value);
            }

            tokenStart = tokenEnd + 1;
        }
    }

    /**
     * Converts a parameter map (with String array values) into a bean of the specified type.
     * <p>
     * This method is typically used to convert HTTP servlet request parameters into a JavaBean.
     * The keys in the map should correspond to bean property names (exact match first, falling back to case-insensitive matching). Values are String arrays,
     * where each array contains one or more values for the corresponding property. If a property has multiple
     * values and the target property type is {@code String[]}, all values are set. Otherwise, values are joined
     * with ", " and converted to the property type.
     * </p>
     * <p>
     * If a parameter value is {@code null}, empty, or contains only a single empty or {@code null} string, the
     * property is set to its default value (as determined by the property type). A {@code null} element inside a
     * multi-element array is joined as the text {@code "null"}.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String[]> params = new HashMap<>();
     * params.put("name", new String[] {"Alice"});
     * params.put("tags", new String[] {"java", "coding"});
     * User user = URLEncodedUtil.convertToBean(params, User.class);
     * // user: {name="Alice", tags="java, coding"}
     * }</pre>
     *
     * @param <T> the type of the bean to create.
     * @param parameters the map of parameters to convert, where keys are property names and values are String arrays;
     *                   may be {@code null} or empty.
     * @param targetType the class of the bean to create and populate; must not be {@code null}.
     * @return an instance of type T with properties populated from the parameter map;
     *         returns an empty instance if {@code parameters} is {@code null} or empty.
     * @throws IllegalArgumentException if {@code targetType} is {@code null} or not a supported bean type.
     */
    public static <T> T convertToBean(final Map<String, String[]> parameters, final Class<? extends T> targetType) throws IllegalArgumentException {
        N.checkArgNotNull(targetType, cs.targetType);

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
                propValue = propInfo == null ? null : propInfo.type.defaultValue();
            } else {
                if (propInfo != null && propInfo.type.javaType().equals(String[].class)) {
                    propValue = values;
                } else {
                    final String mergedValue = Strings.join(values, ", ");
                    propValue = propInfo == null ? mergedValue : propInfo.readPropValue(mergedValue);
                }
            }

            beanInfo.setPropValue(result, entry.getKey(), propValue, true);
        }

        return beanInfo.finishBeanResult(result);
    }

    private static String decodeFormFields(final String content, final Charset charset, final boolean strict) {
        if (content == null) {
            return null;
        }

        return urlDecode(content, (charset != null) ? charset : IOUtil.DEFAULT_CHARSET, true, strict);
    }

    /**
     * Decodes a percent-encoded URL string back to its original text representation.
     * {@code %XX} sequences are interpreted as byte values and decoded using the given charset.
     * Characters that appear literally in the string (i.e., not encoded) are retained unchanged.
     *
     * @param content the string to decode; {@code null} returns {@code null}.
     * @param charset the charset used to decode percent-encoded byte sequences;
     *                must not be {@code null}.
     * @param plusAsBlank if {@code true}, {@code '+'} characters are converted to spaces
     *                    (required for {@code application/x-www-form-urlencoded} query strings);
     *                    if {@code false}, {@code '+'} is left as-is.
     * @param strict if {@code true}, malformed escapes and invalid encoded byte sequences are rejected;
     *               otherwise malformed escapes are retained and invalid byte sequences are replaced
     * @return the decoded string.
     * @throws IllegalArgumentException in strict mode if a percent escape or encoded byte sequence is malformed
     */
    private static String urlDecode(final String content, final Charset charset, final boolean plusAsBlank, final boolean strict)
            throws IllegalArgumentException {
        if (content == null) {
            return null;
        }

        // Decode each contiguous escape run as bytes. Literal characters must not be round-tripped
        // through the selected charset: URLDecoder semantics retain them as-is, and doing otherwise
        // both corrupts unrepresentable Unicode and requires unsafe fixed-size byte estimates for
        // stateful encoders such as ISO-2022-JP.
        final StringBuilder result = new StringBuilder(content.length());

        for (int i = 0, len = content.length(); i < len;) {
            final char c = content.charAt(i);

            if (c == '%') {
                final int escapeStart = i;
                final int upperDigit = i + 1 < len ? asciiHexDigit(content.charAt(i + 1)) : -1;
                final int lowerDigit = i + 2 < len ? asciiHexDigit(content.charAt(i + 2)) : -1;

                if (upperDigit < 0 || lowerDigit < 0) {
                    if (strict) {
                        throw new IllegalArgumentException(
                                "Invalid percent escape at index " + escapeStart + ": '%' must be followed by two ASCII hexadecimal digits");
                    }

                    result.append(c);
                    i++;
                    continue;
                }

                final java.io.ByteArrayOutputStream escapedBytes = new java.io.ByteArrayOutputStream();

                while (i + 2 < len && content.charAt(i) == '%') {
                    final int nextUpperDigit = asciiHexDigit(content.charAt(i + 1));
                    final int nextLowerDigit = asciiHexDigit(content.charAt(i + 2));

                    if (nextUpperDigit < 0 || nextLowerDigit < 0) {
                        break;
                    }

                    escapedBytes.write((nextUpperDigit << 4) + nextLowerDigit);
                    i += 3;
                }

                final byte[] bytes = escapedBytes.toByteArray();

                if (strict) {
                    try {
                        result.append(charset.newDecoder()
                                .onMalformedInput(CodingErrorAction.REPORT)
                                .onUnmappableCharacter(CodingErrorAction.REPORT)
                                .decode(ByteBuffer.wrap(bytes)));
                    } catch (final CharacterCodingException e) {
                        throw new IllegalArgumentException("Invalid percent-encoded byte sequence at index " + escapeStart + " for charset " + charset.name(),
                                e);
                    }
                } else {
                    result.append(new String(bytes, charset));
                }
            } else if (plusAsBlank && c == '+') {
                result.append(' ');
                i++;
            } else {
                // Invalid/incomplete percent escapes are retained verbatim, one character at a
                // time, so Unicode following an invalid '%' is never narrowed to a byte.
                result.append(c);
                i++;
            }
        }

        return result.toString();
    }

    private static int asciiHexDigit(final char ch) {
        if (ch >= '0' && ch <= '9') {
            return ch - '0';
        } else if (ch >= 'A' && ch <= 'F') {
            return ch - 'A' + 10;
        } else if (ch >= 'a' && ch <= 'f') {
            return ch - 'a' + 10;
        }

        return -1;
    }
}
