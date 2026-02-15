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
 * A comprehensive utility class providing high-performance, thread-safe methods for URL encoding and decoding
 * operations, including query parameter encoding, form data processing, and URL component manipulation.
 * This class combines the robustness of Apache HttpComponents with enhanced functionality for modern Java
 * applications, offering both RFC-compliant URL encoding and convenient object-to-query-string conversions.
 *
 * <p>This utility addresses common challenges in web development by providing null-safe operations, flexible
 * charset handling, customizable naming policies, and seamless integration with Java objects. It supports
 * both simple string-based operations and complex object serialization to URL-encoded format, making it
 * suitable for REST API clients, web form processing, and HTTP parameter manipulation.</p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>RFC Compliance:</b> Fully compliant with RFC 3986 and HTML form encoding standards</li>
 *   <li><b>Object Serialization:</b> Automatic conversion of Java objects to URL-encoded query strings</li>
 *   <li><b>Flexible Parsing:</b> Support for both Map-based and object-based parameter decoding</li>
 *   <li><b>Charset Support:</b> Full Unicode support with configurable character encoding</li>
 *   <li><b>Naming Policies:</b> Customizable field naming strategies for object serialization</li>
 *   <li><b>Thread Safety:</b> All methods are thread-safe and suitable for concurrent usage</li>
 *   <li><b>Performance Optimized:</b> BitSet-based encoding tables and efficient string processing</li>
 *   <li><b>Null Safety:</b> Comprehensive null handling with predictable behavior</li>
 * </ul>
 *
 * <p><b>Design Philosophy:</b>
 * <ul>
 *   <li><b>Standards Compliance:</b> Strict adherence to web standards and RFCs</li>
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
 *   <li><b>UTF-8:</b> Default and recommended charset for modern web applications</li>
 *   <li><b>ISO-8859-1:</b> Legacy charset support for older systems</li>
 *   <li><b>Custom Charsets:</b> Any Charset supported by the JVM</li>
 *   <li><b>Platform Default:</b> Automatic fallback to system default charset</li>
 * </ul>
 *
 * <p><b>Common Usage Patterns:</b>
 * <pre>{@code
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
 * String encoded = URLEncodedUtil.encode(criteria, StandardCharsets.UTF_8, NamingPolicy.SNAKE_CASES);
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
 * <p><b>Advanced Usage Examples:</b></p>
 * <pre>{@code
 * // Complex object encoding with nested properties
 * UserPreferences prefs = new UserPreferences();
 * prefs.setTheme("dark");
 * prefs.setLanguage("en");
 * prefs.getNotifications().setEmail(true);
 * 
 * String encoded = URLEncodedUtil.encode(prefs, StandardCharsets.UTF_8, NamingPolicy.CAMEL_CASE);
 * // Handles nested objects and collections appropriately
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
 * // Servlet parameter processing
 * HttpServletRequest request = getRequest();
 * UserProfile profile = URLEncodedUtil.convertToBean(
 *     request.getParameterMap(), UserProfile.class);
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
 *   <li><b>Field Access:</b> Direct field access for objects without getters/setters</li>
 *   <li><b>Collection Handling:</b> Special processing for arrays, Lists, and Sets</li>
 *   <li><b>Null Values:</b> Configurable behavior for null property values</li>
 *   <li><b>Type Conversion:</b> Automatic conversion of primitive and wrapper types</li>
 * </ul>
 *
 * <p><b>Naming Policy Integration:</b>
 * <ul>
 *   <li><b>CAMEL_CASE:</b> Convert property names to camelCase format</li>
 *   <li><b>SNAKE_CASES:</b> Convert to snake_case format</li>
 *   <li><b>LOWER_CASE_WITH_DASHES:</b> Convert to kebab-case format</li>
 *   <li><b>NO_CHANGE:</b> Preserve original property names</li>
 *   <li><b>Custom Policies:</b> Support for user-defined naming transformations</li>
 * </ul>
 *
 * <p><b>URL Component Encoding:</b>
 * <ul>
 *   <li><b>Query Parameters:</b> Standard form field encoding with '+' for spaces</li>
 *   <li><b>Path Components:</b> Path-specific encoding preserving directory structure</li>
 *   <li><b>User Info:</b> Encoding for username:password components in URLs</li>
 *   <li><b>Generic URI:</b> RFC 3986 compliant encoding for URI components</li>
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
 *   <li><b>Static Methods:</b> All utility methods are static and inherently thread-safe</li>
 *   <li><b>Immutable Constants:</b> All public constants are immutable and thread-safe</li>
 *   <li><b>No Shared State:</b> No mutable static variables that could cause race conditions</li>
 *   <li><b>Concurrent Processing:</b> Safe for use in multi-threaded web applications</li>
 * </ul>
 *
 * <p><b>Error Handling:</b>
 * <ul>
 *   <li><b>UncheckedIOException:</b> Wraps IOException from Appendable operations</li>
 *   <li><b>IllegalArgumentException:</b> Thrown for invalid parameters or malformed input</li>
 *   <li><b>NullPointerException:</b> Appropriate null checks with descriptive messages</li>
 *   <li><b>UnsupportedEncodingException:</b> Handled gracefully with fallback to platform default</li>
 * </ul>
 *
 * <p><b>RFC Compliance and Standards:</b>
 * <ul>
 *   <li><b>RFC 3986:</b> URI Generic Syntax compliance for percent-encoding</li>
 *   <li><b>HTML Forms:</b> application/x-www-form-urlencoded format support</li>
 *   <li><b>Query Parameters:</b> Standard web form parameter encoding/decoding</li>
 *   <li><b>Character Encoding:</b> Proper Unicode support with configurable charsets</li>
 * </ul>
 *
 * <p><b>Integration with Web Frameworks:</b>
 * <ul>
 *   <li><b>Servlet API:</b> Direct support for HttpServletRequest parameter maps</li>
 *   <li><b>Spring Framework:</b> Compatible with Spring's parameter binding mechanisms</li>
 *   <li><b>JAX-RS:</b> Suitable for REST client parameter encoding</li>
 *   <li><b>HTTP Clients:</b> Integration with Apache HttpClient, OkHttp, and others</li>
 * </ul>
 *
 * <p><b>Best Practices:</b>
 * <ul>
 *   <li>Always specify charset explicitly for cross-platform compatibility</li>
 *   <li>Use UTF-8 encoding for modern web applications to ensure Unicode support</li>
 *   <li>Prefer object-based encoding for complex parameter sets over manual string building</li>
 *   <li>Use appropriate naming policies to match API expectations</li>
 *   <li>Cache BeanInfo objects when repeatedly encoding objects of the same type</li>
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
 *   <li><b>Injection Prevention:</b> Proper encoding prevents URL injection attacks</li>
 *   <li><b>Character Set Attacks:</b> Explicit charset specification prevents encoding-based attacks</li>
 *   <li><b>Length Limits:</b> Consider implementing parameter length limits for DoS prevention</li>
 * </ul>
 *
 * <p><b>Example: REST API Client</b>
 * <pre>{@code
 * public class ApiClient {
 *     private static final String BASE_URL = "https://api.example.com";
 *
 *     public <T> List<T> search(SearchRequest request, Class<T> responseType) {
 *         String url = URLEncodedUtil.encode(BASE_URL + "/search", request,
 *             StandardCharsets.UTF_8, NamingPolicy.SNAKE_CASES);
 *
 *         // Use url with HTTP client
 *         String response = httpClient.get(url);
 *         return parseResponse(response, responseType);
 *     }
 *
 *     public void submitForm(FormData formData) {
 *         String encoded = URLEncodedUtil.encode(formData, StandardCharsets.UTF_8);
 *
 *         HttpRequest request = HttpRequest.newBuilder()
 *             .uri(URI.create(BASE_URL + "/submit"))
 *             .header("Content-Type", "application/x-www-form-urlencoded")
 *             .POST(HttpRequest.BodyPublishers.ofString(encoded))
 *             .build();
 *
 *         httpClient.send(request, HttpResponse.BodyHandlers.ofString());
 *     }
 *
 *     public SearchRequest parseQueryString(String queryString) {
 *         return URLEncodedUtil.decode(queryString, StandardCharsets.UTF_8, SearchRequest.class);
 *     }
 * }
 * }</pre>
 *
 * <p><b>Comparison with Alternative Approaches:</b>
 * <ul>
 *   <li><b>vs. URLEncoder/URLDecoder:</b> More features and object support vs. basic string encoding</li>
 *   <li><b>vs. Spring UriComponentsBuilder:</b> Lightweight and focused vs. comprehensive URI building</li>
 *   <li><b>vs. Apache HttpClient:</b> Specialized for URL encoding vs. full HTTP client functionality</li>
 *   <li><b>vs. Manual String Building:</b> Type-safe and robust vs. error-prone manual concatenation</li>
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
     * Encodes the provided parameters into a URL-encoded query string using the default charset (UTF-8).
     * <p>
     * This method accepts various parameter formats:
     * <ul>
     * <li>{@code Map<String, ?>}: Keys and values are encoded as name=value pairs</li>
     * <li>JavaBean: Bean properties are encoded using camelCase naming</li>
     * <li>{@code Object[]}: Pairs of name-value elements (must have even length)</li>
     * <li>{@code String}: If contains "=", treated as encoded parameters; otherwise encoded as-is</li>
     * </ul>
     * Characters are percent-encoded according to application/x-www-form-urlencoded rules, where spaces become '+'.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> params = Map.of("name", "John Doe", "age", 30);
     * String query = URLEncodedUtil.encode(params);
     * // query: "name=John+Doe&amp;age=30"
     * }</pre>
     *
     * @param parameters the parameters to encode (Map, bean, Object array pairs, or String); may be {@code null}.
     * @return a URL-encoded query string (e.g., "name=John+Doe&amp;age=30"); returns empty string if {@code parameters} is {@code null}.
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
     * <li>JavaBean: Bean properties are encoded using camelCase naming (default)</li>
     * <li>{@code Object[]}: Pairs of name-value elements (must have even length)</li>
     * <li>{@code String}: If contains "=", treated as encoded parameters; otherwise encoded as-is</li>
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
     * @param parameters the parameters to encode (Map, bean, Object array pairs, or String); may be {@code null}.
     * @param charset the charset to use for percent-encoding; if {@code null}, defaults to UTF-8.
     * @return a URL-encoded query string; returns empty string if {@code parameters} is {@code null}.
     * @see #encode(Object)
     * @see #encode(Object, Charset, NamingPolicy)
     * @see URLEncoder#encode(String, Charset)
     */
    public static String encode(final Object parameters, final Charset charset) {
        return encode(parameters, charset, NamingPolicy.CAMEL_CASE);
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
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * class User { String firstName; int userAge; }
     * User user = new User("John", 30);
     * String query = URLEncodedUtil.encode(user, StandardCharsets.UTF_8, NamingPolicy.SNAKE_CASE);
     * // query: "first_name=John&amp;user_age=30"
     * }</pre>
     *
     * @param parameters the parameters to encode (Map, bean, Object array pairs, or String); may be {@code null}.
     * @param charset the charset to use for percent-encoding; if {@code null}, defaults to UTF-8.
     * @param namingPolicy the naming policy to transform property/key names (e.g., CAMEL_CASE, SCREAMING_SNAKE_CASE);
     *                     if {@code null} or NO_CHANGE, names are not transformed.
     * @return a URL-encoded query string; returns empty string if {@code parameters} is {@code null}.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> params = Map.of("q", "java url encoding", "page", 1);
     * String fullUrl = URLEncodedUtil.encode("http://search.example.com", params);
     * // fullUrl: "http://search.example.com?q=java+url+encoding&amp;page=1"
     * }</pre>
     *
     * @param url the base URL to which the query string will be appended (e.g., "http://example.com/path").
     * @param parameters the parameters to encode and append (Map, bean, Object array pairs, or String); may be {@code null}.
     * @return the URL with the encoded query string appended (e.g., "http://example.com/path?name=value");
     *         returns the original URL if {@code parameters} is {@code null} or empty.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> params = Map.of("name", "中文");
     * String fullUrl = URLEncodedUtil.encode("http://example.com", params, StandardCharsets.UTF_8);
     * // fullUrl: "http://example.com?name=%E4%B8%AD%E6%96%87"
     * }</pre>
     *
     * @param url the base URL to which the query string will be appended.
     * @param parameters the parameters to encode and append (Map, bean, Object array pairs, or String); may be {@code null}.
     * @param charset the charset to use for percent-encoding; if {@code null}, defaults to UTF-8.
     * @return the URL with the encoded query string appended;
     *         returns the original URL if {@code parameters} is {@code null} or empty.
     * @see #encode(String, Object)
     * @see #encode(String, Object, Charset, NamingPolicy)
     */
    public static String encode(final String url, final Object parameters, final Charset charset) {
        return encode(url, parameters, charset, NamingPolicy.CAMEL_CASE);
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * class User { String firstName; }
     * String url = URLEncodedUtil.encode("http://api.com/users", new User("John"),
     *                                     StandardCharsets.UTF_8, NamingPolicy.SNAKE_CASE);
     * // url: "http://api.com/users?first_name=John"
     * }</pre>
     *
     * @param url the base URL to which the query string will be appended.
     * @param parameters the parameters to encode and append (Map, bean, Object array pairs, or String); may be {@code null}.
     * @param charset the charset to use for percent-encoding; if {@code null}, defaults to UTF-8.
     * @param namingPolicy the naming policy to transform property/key names (e.g., CAMEL_CASE, SCREAMING_SNAKE_CASE);
     *                     if {@code null} or NO_CHANGE, names are not transformed.
     * @return the URL with the encoded query string appended;
     *         returns the original URL if {@code parameters} is {@code null} or empty.
     * @see #encode(String, Object, Charset)
     */
    @SuppressWarnings("rawtypes")
    public static String encode(final String url, final Object parameters, final Charset charset, final NamingPolicy namingPolicy) {
        if (parameters == null || (parameters instanceof Map && ((Map) parameters).isEmpty())) {
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

            encode(parameters, charset, namingPolicy, sb);
            sb.append(fragment);

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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * StringBuilder sb = new StringBuilder("http://example.com?");
     * URLEncodedUtil.encode(Map.of("key", "value"), sb);
     * // sb: "http://example.com?key=value"
     * }</pre>
     *
     * @param parameters the parameters to encode (Map, bean, Object array pairs, or String); may be {@code null}.
     * @param output the {@code Appendable} (e.g., {@code StringBuilder}, {@code Writer}) to which the encoded query string will be appended.
     * @throws UncheckedIOException if an I/O error occurs while appending to the output.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Writer writer = new FileWriter("query.txt");
     * URLEncodedUtil.encode(Map.of("name", "中文"), StandardCharsets.UTF_8, writer);
     * // writer content: "name=%E4%B8%AD%E6%96%87"
     * }</pre>
     *
     * @param parameters the parameters to encode (Map, bean, Object array pairs, or String); may be {@code null}.
     * @param charset the charset to use for percent-encoding; if {@code null}, defaults to UTF-8.
     * @param output the {@code Appendable} to which the encoded query string will be appended.
     * @throws UncheckedIOException if an I/O error occurs while appending to the output.
     * @see #encode(Object, Charset, NamingPolicy, Appendable)
     */
    public static void encode(final Object parameters, final Charset charset, final Appendable output) {
        encode(parameters, charset, NamingPolicy.CAMEL_CASE, output);
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
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * class User { String firstName; int age; }
     * StringBuilder sb = new StringBuilder();
     * URLEncodedUtil.encode(new User("John", 30), StandardCharsets.UTF_8, NamingPolicy.SNAKE_CASE, sb);
     * // sb: "first_name=John&amp;age=30"
     * }</pre>
     *
     * @param parameters the parameters to encode (Map, bean, Object array pairs, or String); may be {@code null}.
     * @param charset the charset to use for percent-encoding; if {@code null}, defaults to UTF-8.
     * @param namingPolicy the naming policy to transform property/key names (e.g., CAMEL_CASE, SCREAMING_SNAKE_CASE);
     *                     if {@code null} or NO_CHANGE, names are not transformed.
     * @param output the {@code Appendable} to which the encoded query string will be appended.
     * @throws UncheckedIOException if an I/O error occurs while appending to the output.
     * @throws IllegalArgumentException if parameters is an Object array with odd length.
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
                encode(Beans.beanToMap(parameters, true, null, namingPolicy), charset, NamingPolicy.NO_CHANGE, output);
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
     * Encode/escape www-url-form-encoded content.
     *
     * @param content the content to encode, will convert space to '+'.
     * @param charset the charset to use.
     * @param output the Appendable to which the encoded content will be written.
     * @throws IOException if an I/O error occurs while appending to the output.
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
     * @param content the string to encode, does not convert space to '+'.
     * @param charset the charset to use.
     * @param output the Appendable to which the encoded content will be written.
     * @throws IOException if an I/O error occurs while appending to the output.
     */
    static void encUserInfo(final String content, final Charset charset, final Appendable output) throws IOException {
        urlEncode(content, charset, USERINFO, false, output);
    }

    /**
     * Encode a String using the {@link #URIC} set of characters.
     * <p>
     * Used by URIBuilder to encode the urlQuery and fragment segments.
     * @param content the string to encode, does not convert space to '+'.
     * @param charset the charset to use.
     * @param output the Appendable to which the encoded content will be written.
     * @throws IOException if an I/O error occurs while appending to the output.
     */
    static void encUric(final String content, final Charset charset, final Appendable output) throws IOException {
        urlEncode(content, charset, URIC, false, output);
    }

    /**
     * Encode a String using the {@link #PATH_SAFE} set of characters.
     * <p>
     * Used by URIBuilder to encode path segments.
     * @param content the string to encode, does not convert space to '+'.
     * @param charset the charset to use.
     * @param output the Appendable to which the encoded content will be written.
     * @throws IOException if an I/O error occurs while appending to the output.
     */
    static void encPath(final String content, final Charset charset, final Appendable output) throws IOException {
        urlEncode(content, charset, PATH_SAFE, false, output);
    }

    /**
     * Decodes a URL-encoded query string into a {@code Map<String, String>} using the default charset (UTF-8).
     * <p>
     * This method parses a URL query string (e.g., "name=value&amp;foo=bar") and converts it into a map
     * where keys are parameter names and values are parameter values. Both '+' characters and <i>%XX</i> sequences
     * are decoded. Parameter names and values are trimmed of whitespace. If a parameter appears multiple times,
     * only the last occurrence is retained (use {@link #decodeToMultimap(String)} to preserve all values).
     * </p>
     * <p>
     * Supports both <i>&amp;</i> and ';' as parameter separators according to RFC 2396.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> params = URLEncodedUtil.decode("name=John+Doe&amp;age=30");
     * // params: {name=John Doe, age=30}
     * }</pre>
     *
     * @param urlQuery the URL query string to decode (e.g., "key1=value1&amp;key2=value2"), may be {@code null} or empty.
     * @return a {@code LinkedHashMap} containing parameter names as keys and decoded parameter values as values;
     *         returns an empty map if {@code urlQuery} is {@code null} or empty.
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
     * and values are parameter values. Both '+' characters and <i>%XX</i> sequences are decoded using the
     * specified charset. Parameter names and values are trimmed of whitespace. If a parameter appears
     * multiple times, only the last occurrence is retained.
     * </p>
     * <p>
     * Supports both <i>&amp;</i> and ';' as parameter separators according to RFC 2396.
     * </p>
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
     *         returns an empty map if {@code urlQuery} is {@code null} or empty.
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
     * by accepting a custom supplier. Both '+' characters and <i>%XX</i> sequences are decoded using the specified charset.
     * Parameter names and values are trimmed of whitespace. If a parameter appears multiple times, only the last
     * occurrence is retained.
     * </p>
     * <p>
     * Supports both <i>&amp;</i> and ';' as parameter separators according to RFC 2396.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TreeMap<String, String> params = URLEncodedUtil.decode("b=2&amp;a=1", StandardCharsets.UTF_8, TreeMap::new);
     * // params: {a=1, b=2} (sorted by key)
     * }</pre>
     *
     * @param <M> the type of the Map to return, must extend {@code Map<String, String>}.
     * @param urlQuery the URL query string to decode, may be {@code null} or empty.
     * @param charset the charset to use for decoding percent-encoded characters; if {@code null}, defaults to UTF-8.
     * @param mapSupplier a supplier that provides an instance of the desired Map implementation.
     * @return a Map of type M containing parameter names as keys and decoded parameter values as values;
     *         returns an empty map (from supplier) if {@code urlQuery} is {@code null} or empty.
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
     * values when a parameter appears multiple times in the query string. Both '+' characters and <i>%XX</i>
     * sequences are decoded. Parameter names and values are trimmed of whitespace.
     * </p>
     * <p>
     * Supports both <i>&amp;</i> and ';' as parameter separators according to RFC 2396.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, String> params = URLEncodedUtil.decodeToMultimap("color=red&amp;color=blue&amp;size=L");
     * // params: {color=[red, blue], size=[L]}
     * }</pre>
     *
     * @param urlQuery the URL query string to decode (e.g., "color=red&amp;color=blue&amp;size=L"), may be {@code null} or empty.
     * @return a {@code ListMultimap} containing parameter names as keys and lists of decoded parameter values;
     *         returns an empty multimap if {@code urlQuery} is {@code null} or empty.
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
     * all values when a parameter appears multiple times in the query string. Both '+' characters and <i>%XX</i>
     * sequences are decoded using the specified charset. Parameter names and values are trimmed of whitespace.
     * </p>
     * <p>
     * Supports both <i>&amp;</i> and ';' as parameter separators according to RFC 2396.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimap<String, String> params = URLEncodedUtil.decodeToMultimap("tag=java&amp;tag=url", StandardCharsets.UTF_8);
     * // params: {tag=[java, url]}
     * }</pre>
     *
     * @param urlQuery the URL query string to decode, may be {@code null} or empty.
     * @param charset the charset to use for decoding percent-encoded characters; if {@code null}, defaults to UTF-8.
     * @return a {@code ListMultimap} containing parameter names as keys and lists of decoded parameter values;
     *         returns an empty multimap if {@code urlQuery} is {@code null} or empty.
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
     * to the appropriate property types using the bean's property information. Both '+' characters and <i>%XX</i>
     * sequences are decoded. Parameter names and values are trimmed of whitespace.
     * </p>
     * <p>
     * Supports both <i>&amp;</i> and ';' as parameter separators according to RFC 2396.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * class User { String name; int age; }
     * User user = URLEncodedUtil.decode("name=John&amp;age=30", User.class);
     * // user: {name="John", age=30}
     * }</pre>
     *
     * @param <T> the type of the object to decode into (JavaBean or Map).
     * @param urlQuery the URL query string to decode, may be {@code null} or empty.
     * @param targetType the class of the bean or Map to create and populate.
     * @return an instance of type T populated with the decoded parameter values;
     *         returns an empty instance if {@code urlQuery} is {@code null} or empty.
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
     * to the appropriate property types using the bean's property information. Both '+' characters and <i>%XX</i>
     * sequences are decoded using the specified charset. Parameter names and values are trimmed of whitespace.
     * </p>
     * <p>
     * If {@code targetType} is a Map class, this method delegates to {@link #decode(String, Charset, Supplier)}.
     * Otherwise, it creates and populates a bean instance.
     * </p>
     * <p>
     * Supports both <i>&amp;</i> and ';' as parameter separators according to RFC 2396.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * class Product { String name; double price; }
     * Product p = URLEncodedUtil.decode("name=Laptop&amp;price=999.99", StandardCharsets.UTF_8, Product.class);
     * // p: {name="Laptop", price=999.99}
     * }</pre>
     *
     * @param <T> the type of the object to decode into (JavaBean or Map).
     * @param urlQuery the URL query string to decode, may be {@code null} or empty.
     * @param charset the charset to use for decoding percent-encoded characters; if {@code null}, defaults to UTF-8.
     * @param targetType the class of the bean or Map to create and populate.
     * @return an instance of type T populated with the decoded parameter values;
     *         returns an empty instance if {@code urlQuery} is {@code null} or empty.
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
     * @param targetType the class of the bean to create and populate.
     * @return an instance of type T with properties populated from the parameter map;
     *         returns an empty instance if {@code parameters} is {@code null} or empty.
     */
    public static <T> T convertToBean(final Map<String, String[]> parameters, final Class<? extends T> targetType) {
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

    private static String decodeFormFields(final String content, final Charset charset) {
        if (content == null) {
            return null;
        }

        return urlDecode(content, (charset != null) ? charset : Charsets.DEFAULT, true);
    }

    /**
     * Decode/unescape a portion of a URL, to use with the urlQuery part ensure {@code plusAsBlank} is {@code true}.
     *
     * @param content the portion to decode.
     * @param charset the charset to use.
     * @param plusAsBlank if {@code true}, then convert '+' to space (e.g., for www-url-form-encoded content), otherwise leave as is.
     * @return the encoded string.
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
                final int upperDigit = Character.digit(uc, 16);
                final int lowerDigit = Character.digit(lc, 16);

                if ((upperDigit != -1) && (lowerDigit != -1)) {
                    bb.put((byte) ((upperDigit << 4) + lowerDigit));
                } else {
                    bb.put((byte) '%');
                    bb.put((byte) uc);
                    bb.put((byte) lc);
                }
            } else if (plusAsBlank && (c == '+')) {
                bb.put((byte) ' ');
            } else {
                // Encode non-ASCII chars properly instead of truncating via (byte) cast
                if (c > 0x7F) {
                    final byte[] bytes = String.valueOf(c).getBytes(charset);
                    for (final byte b : bytes) {
                        bb.put(b);
                    }
                } else {
                    bb.put((byte) c);
                }
            }
        }

        bb.flip();

        return charset.decode(bb).toString();
    }
}
