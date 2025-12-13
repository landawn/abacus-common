/*
 * Copyright (C) 2016 HaiYang Li
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

import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.BiConsumer;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.http.HttpUtil.HttpDate;
import com.landawn.abacus.util.Charsets;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.cs;

/**
 * <p>Note: It's copied from Google Guava under Apache License 2.0 and may be modified.</p>
 *
 * A container for HTTP headers with a fluent API for setting and retrieving header values.
 * This class provides convenient methods for working with common HTTP headers and supports
 * all standard HTTP header fields as defined in various RFCs.
 *
 * <p>Headers can be set individually or in bulk, and the class provides type-safe conversion
 * for various value types including strings, dates, collections, and numbers.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * HttpHeaders headers = HttpHeaders.create()
 *     .setContentType("application/json")
 *     .setAuthorization("Bearer token123")
 *     .setAcceptEncoding("gzip, deflate")
 *     .set("X-Custom-Header", "value");
 *
 * // Or create with initial values
 * HttpHeaders headers2 = HttpHeaders.of("Content-Type", "application/json",
 *                                       "Accept", "application/json");
 * }</pre>
 *
 * @see HttpClient
 * @see HttpSettings
 */
public final class HttpHeaders {

    static final char LF = Strings.LF.charAt(0);

    /**
     * <p>Note: It's copied from Google Guava under Apache License 2.0 and may be modified.</p>
     * 
     * Standard HTTP header field names as defined in various RFCs.
     * This class contains constants for all standard HTTP request and response headers.
     * 
     * <p>The constants are organized into categories:</p>
     * <ul>
     *   <li>Common headers used in both requests and responses</li>
     *   <li>Request-specific headers</li>
     *   <li>Response-specific headers</li>
     *   <li>Common non-standard headers</li>
     * </ul>
     */
    public static final class Names {

        /**
         * Private constructor to prevent instantiation of this constants class.
         */
        private Names() {
            // singleton.
        }

        // HTTP Request and Response header fields

        /** The HTTP {@code Cache-Control} header field name. */
        public static final String CACHE_CONTROL = "Cache-Control";
        /** The HTTP {@code Content-Length} header field name. */
        public static final String CONTENT_LENGTH = "Content-Length";
        /** The HTTP {@code Content-Type} header field name. */
        public static final String CONTENT_TYPE = "Content-Type";

        /** The HTTP {@code Content-Encoding} header field name. */
        public static final String CONTENT_ENCODING = "Content-Encoding";
        /** The HTTP {@code Content-Type} header field name. */
        static final String L_CONTENT_TYPE = "content-type";
        /** The HTTP {@code Content-Encoding} header field name. */
        static final String L_CONTENT_ENCODING = "content-encoding";

        /** The HTTP {@code Date} header field name. */
        public static final String DATE = "Date";
        /** The HTTP {@code Pragma} header field name. */
        public static final String PRAGMA = "Pragma";
        /** The HTTP {@code Via} header field name. */
        public static final String VIA = "Via";
        /** The HTTP {@code Warning} header field name. */
        public static final String WARNING = "Warning";

        // HTTP Request header fields

        /** The HTTP {@code Accept} header field name. */
        public static final String ACCEPT = "Accept";
        /** The HTTP {@code Accept} header field name. */
        static final String L_ACCEPT = "accept";
        /** The HTTP {@code Accept-Encoding} header field name. */
        public static final String ACCEPT_ENCODING = "Accept-Encoding";
        /** The HTTP {@code Accept-Encoding} header field name. */
        static final String L_ACCEPT_ENCODING = "accept-encoding";
        /** The HTTP {@code Accept-Charset} header field name. */
        public static final String ACCEPT_CHARSET = "Accept-Charset";
        /** The HTTP {@code Accept-Charset} header field name. */
        static final String L_ACCEPT_CHARSET = "accept-charset";
        /** The HTTP {@code Accept-Language} header field name. */
        public static final String ACCEPT_LANGUAGE = "Accept-Language";
        /** The HTTP {@code Access-Control-Request-Headers} header field name. */
        public static final String ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";
        /** The HTTP {@code Access-Control-Request-Method} header field name. */
        public static final String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";
        /** The HTTP {@code Authorization} header field name. */
        public static final String AUTHORIZATION = "Authorization";
        /** The HTTP {@code Connection} header field name. */
        public static final String CONNECTION = "Connection";
        /** The HTTP {@code Cookie} header field name. */
        public static final String COOKIE = "Cookie";
        /**
         * The HTTP <a href="https://tools.ietf.org/html/rfc8470">{@code Early-Data}</a> header field
         * name.
         */
        public static final String EARLY_DATA = "Early-Data";
        /** The HTTP {@code Expect} header field name. */
        public static final String EXPECT = "Expect";
        /** The HTTP {@code From} header field name. */
        public static final String FROM = "From";
        /**
         * The HTTP <a href="https://tools.ietf.org/html/rfc7239">{@code Forwarded}</a> header field name.
         */
        public static final String FORWARDED = "Forwarded";
        /**
         * The HTTP {@code Follow-Only-When-Prerender-Shown} header field name.
         */
        @Beta
        public static final String FOLLOW_ONLY_WHEN_PRERENDER_SHOWN = "Follow-Only-When-Prerender-Shown";
        /** The HTTP {@code Host} header field name. */
        public static final String HOST = "Host";
        /**
         * The HTTP <a href="https://tools.ietf.org/html/rfc7540#section-3.2.1">{@code HTTP2-Settings}
         * </a> header field name.
         */
        public static final String HTTP2_SETTINGS = "HTTP2-Settings";
        /** The HTTP {@code If-Match} header field name. */
        public static final String IF_MATCH = "If-Match";
        /** The HTTP {@code If-Modified-Since} header field name. */
        public static final String IF_MODIFIED_SINCE = "If-Modified-Since";
        /** The HTTP {@code If-None-Match} header field name. */
        public static final String IF_NONE_MATCH = "If-None-Match";
        /** The HTTP {@code If-Range} header field name. */
        public static final String IF_RANGE = "If-Range";
        /** The HTTP {@code If-Unmodified-Since} header field name. */
        public static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";
        /** The HTTP {@code Last-Event-ID} header field name. */
        public static final String LAST_EVENT_ID = "Last-Event-ID";
        /** The HTTP {@code Max-Forwards} header field name. */
        public static final String MAX_FORWARDS = "Max-Forwards";
        /** The HTTP {@code Origin} header field name. */
        public static final String ORIGIN = "Origin";
        /** The HTTP {@code Proxy-Authorization} header field name. */
        public static final String PROXY_AUTHORIZATION = "Proxy-Authorization";
        /** The HTTP {@code Range} header field name. */
        public static final String RANGE = "Range";
        /** The HTTP {@code Referer} header field name. */
        public static final String REFERER = "Referer";
        /**
         * The HTTP <a href="https://www.w3.org/TR/referrer-policy/">{@code Referrer-Policy}</a> header
         * field name.
         */
        public static final String REFERRER_POLICY = "Referrer-Policy";

        /**
         * The HTTP <a href="https://www.w3.org/TR/service-workers/#update-algorithm">{@code
         * Service-Worker}</a> header field name.
         */
        public static final String SERVICE_WORKER = "Service-Worker";
        /** The HTTP {@code TE} header field name. */
        public static final String TE = "TE";
        /** The HTTP {@code Upgrade} header field name. */
        public static final String UPGRADE = "Upgrade";
        /** The HTTP {@code User-Agent} header field name. */
        public static final String USER_AGENT = "User-Agent";

        // HTTP Response header fields

        /** The HTTP {@code Accept-Ranges} header field name. */
        public static final String ACCEPT_RANGES = "Accept-Ranges";
        /** The HTTP {@code Access-Control-Allow-Headers} header field name. */
        public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
        /** The HTTP {@code Access-Control-Allow-Methods} header field name. */
        public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
        /** The HTTP {@code Access-Control-Allow-Origin} header field name. */
        public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
        /** The HTTP {@code Access-Control-Allow-Credentials} header field name. */
        public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
        /** The HTTP {@code Access-Control-Expose-Headers} header field name. */
        public static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
        /** The HTTP {@code Access-Control-Max-Age} header field name. */
        public static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";
        /** The HTTP {@code Age} header field name. */
        public static final String AGE = "Age";
        /** The HTTP {@code Allow} header field name. */
        public static final String ALLOW = "Allow";
        /** The HTTP {@code Content-Disposition} header field name. */
        public static final String CONTENT_DISPOSITION = "Content-Disposition";
        /** The HTTP {@code Content-Language} header field name. */
        public static final String CONTENT_LANGUAGE = "Content-Language";
        /** The HTTP {@code Content-Location} header field name. */
        public static final String CONTENT_LOCATION = "Content-Location";
        /** The HTTP {@code Content-MD5} header field name. */
        public static final String CONTENT_MD5 = "Content-MD5";
        /** The HTTP {@code Content-Range} header field name. */
        public static final String CONTENT_RANGE = "Content-Range";
        /**
         * The HTTP <a href="http://w3.org/TR/CSP/#content-security-policy-header-field">{@code
         * Content-Security-Policy}</a> header field name.
         */
        public static final String CONTENT_SECURITY_POLICY = "Content-Security-Policy";
        /**
         * The HTTP <a href="http://w3.org/TR/CSP/#content-security-policy-report-only-header-field">
         * {@code Content-Security-Policy-Report-Only}</a> header field name.
         */
        public static final String CONTENT_SECURITY_POLICY_REPORT_ONLY = "Content-Security-Policy-Report-Only";
        /**
         * The HTTP nonstandard {@code X-Content-Security-Policy} header field name. It was introduced in
         * <a href="https://www.w3.org/TR/2011/WD-CSP-20111129/">CSP v.1</a> and used by the Firefox until
         * version 23 and the Internet Explorer version 10. Please, use {@link #CONTENT_SECURITY_POLICY}
         * to pass the CSP.
         */
        public static final String X_CONTENT_SECURITY_POLICY = "X-Content-Security-Policy";
        /**
         * The HTTP nonstandard {@code X-Content-Security-Policy-Report-Only} header field name. It was
         * introduced in <a href="https://www.w3.org/TR/2011/WD-CSP-20111129/">CSP v.1</a> and used by the
         * Firefox until version 23 and the Internet Explorer version 10. Please, use {@link
         * #CONTENT_SECURITY_POLICY_REPORT_ONLY} to pass the CSP.
         */
        public static final String X_CONTENT_SECURITY_POLICY_REPORT_ONLY = "X-Content-Security-Policy-Report-Only";
        /**
         * The HTTP nonstandard {@code X-WebKit-CSP} header field name. It was introduced in <a
         * href="https://www.w3.org/TR/2011/WD-CSP-20111129/">CSP v.1</a> and used by the Chrome until
         * version 25. Please, use {@link #CONTENT_SECURITY_POLICY} to pass the CSP.
         */
        public static final String X_WEBKIT_CSP = "X-WebKit-CSP";
        /**
         * The HTTP nonstandard {@code X-WebKit-CSP-Report-Only} header field name. It was introduced in
         * <a href="https://www.w3.org/TR/2011/WD-CSP-20111129/">CSP v.1</a> and used by the Chrome until
         * version 25. Please, use {@link #CONTENT_SECURITY_POLICY_REPORT_ONLY} to pass the CSP.
         */
        public static final String X_WEBKIT_CSP_REPORT_ONLY = "X-WebKit-CSP-Report-Only";
        /** The HTTP {@code ETag} header field name. */
        public static final String ETAG = "ETag";
        /** The HTTP {@code Expires} header field name. */
        public static final String EXPIRES = "Expires";
        /** The HTTP {@code Last-Modified} header field name. */
        public static final String LAST_MODIFIED = "Last-Modified";
        /** The HTTP {@code Link} header field name. */
        public static final String LINK = "Link";
        /** The HTTP {@code Location} header field name. */
        public static final String LOCATION = "Location";
        /** The HTTP {@code P3P} header field name. Limited browser support. */
        public static final String P3P = "P3P";
        /** The HTTP {@code Proxy-Authenticate} header field name. */
        public static final String PROXY_AUTHENTICATE = "Proxy-Authenticate";
        /** The HTTP {@code Refresh} header field name. Non-standard header supported by most browsers. */
        public static final String REFRESH = "Refresh";
        /** The HTTP {@code Retry-After} header field name. */
        public static final String RETRY_AFTER = "Retry-After";
        /** The HTTP {@code Server} header field name. */
        public static final String SERVER = "Server";
        /**
         * The HTTP <a href="https://www.w3.org/TR/server-timing/">{@code Server-Timing}</a> header field
         * name.
         */
        public static final String SERVER_TIMING = "Server-Timing";
        /**
         * The HTTP <a href="https://www.w3.org/TR/service-workers/#update-algorithm">{@code
         * Service-Worker-Allowed}</a> header field name.
         */
        public static final String SERVICE_WORKER_ALLOWED = "Service-Worker-Allowed";
        /** The HTTP {@code Set-Cookie} header field name. */
        public static final String SET_COOKIE = "Set-Cookie";
        /** The HTTP {@code Set-Cookie2} header field name. */
        public static final String SET_COOKIE2 = "Set-Cookie2";
        /**
         * The HTTP <a href="http://tools.ietf.org/html/rfc6797#section-6.1">{@code
         * Strict-Transport-Security}</a> header field name.
         */
        public static final String STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";
        /**
         * The HTTP <a href="http://www.w3.org/TR/resource-timing/#cross-origin-resources">{@code
         * Timing-Allow-Origin}</a> header field name.
         */
        public static final String TIMING_ALLOW_ORIGIN = "Timing-Allow-Origin";
        /** The HTTP {@code Trailer} header field name. */
        public static final String TRAILER = "Trailer";
        /** The HTTP {@code Transfer-Encoding} header field name. */
        public static final String TRANSFER_ENCODING = "Transfer-Encoding";
        /** The HTTP {@code Vary} header field name. */
        public static final String VARY = "Vary";
        /** The HTTP {@code WWW-Authenticate} header field name. */
        public static final String WWW_AUTHENTICATE = "WWW-Authenticate";

        // Common, non-standard HTTP header fields

        /** The HTTP {@code DNT} header field name. */
        public static final String DNT = "DNT";
        /** The HTTP {@code X-Content-Type-Options} header field name. */
        public static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
        /** The HTTP {@code X-Do-Not-Track} header field name. */
        public static final String X_DO_NOT_TRACK = "X-Do-Not-Track";
        /** The HTTP {@code X-Forwarded-For} header field name (superseded by {@code Forwarded}). */
        public static final String X_FORWARDED_FOR = "X-Forwarded-For";
        /** The HTTP {@code X-Forwarded-Proto} header field name. */
        public static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";
        /**
         * The HTTP <a href="http://goo.gl/lQirAH">{@code X-Forwarded-Host}</a> header field name.
         */
        public static final String X_FORWARDED_HOST = "X-Forwarded-Host";
        /**
         * The HTTP <a href="http://goo.gl/YtV2at">{@code X-Forwarded-Port}</a> header field name.
         */
        public static final String X_FORWARDED_PORT = "X-Forwarded-Port";
        /** The HTTP {@code X-Frame-Options} header field name. */
        public static final String X_FRAME_OPTIONS = "X-Frame-Options";
        /** The HTTP {@code X-Powered-By} header field name. */
        public static final String X_POWERED_BY = "X-Powered-By";
        /**
         * The HTTP <a href="http://tools.ietf.org/html/draft-evans-palmer-key-pinning">{@code
         * Public-Key-Pins}</a> header field name.
         */
        @Beta
        public static final String PUBLIC_KEY_PINS = "Public-Key-Pins";
        /**
         * The HTTP <a href="http://tools.ietf.org/html/draft-evans-palmer-key-pinning">{@code
         * Public-Key-Pins-Report-Only}</a> header field name.
         */
        @Beta
        public static final String PUBLIC_KEY_PINS_REPORT_ONLY = "Public-Key-Pins-Report-Only";
        /** The HTTP {@code X-Requested-With} header field name. */
        public static final String X_REQUESTED_WITH = "X-Requested-With";
        /** The HTTP {@code X-User-IP} header field name. */
        public static final String X_USER_IP = "X-User-IP";
        /**
         * The HTTP <a href="https://goo.gl/VKpXxa">{@code X-Download-Options}</a> header field name.
         *
         * <p>When the new X-Download-Options header is present with the value {@code noopen}, the user is
         * prevented from opening a file download directly; instead, they must first save the file
         * locally.
         */
        @Beta
        public static final String X_DOWNLOAD_OPTIONS = "X-Download-Options";
        /** The HTTP {@code X-XSS-Protection} header field name. */
        public static final String X_XSS_PROTECTION = "X-XSS-Protection";
        /**
         * The HTTP <a
         * href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-DNS-Prefetch-Control">{@code
         * X-DNS-Prefetch-Control}</a> header controls DNS prefetch behavior. Value can be "on" or "off".
         * By default, DNS prefetching is "on" for HTTP pages and "off" for HTTPS pages.
         */
        public static final String X_DNS_PREFETCH_CONTROL = "X-DNS-Prefetch-Control";
        /**
         * The HTTP <a href="http://html.spec.whatwg.org/multipage/semantics.html#hyperlink-auditing">
         * {@code Ping-From}</a> header field name.
         */
        public static final String PING_FROM = "Ping-From";
        /**
         * The HTTP <a href="http://html.spec.whatwg.org/multipage/semantics.html#hyperlink-auditing">
         * {@code Ping-To}</a> header field name.
         */
        public static final String PING_TO = "Ping-To";

        /**
         * The HTTP <a href="https://github.com/mikewest/sec-metadata">{@code Sec-Metadata}</a> header
         * field name.
         */
        public static final String SEC_METADATA = "Sec-Metadata";
        /**
         * The HTTP <a href="https://tools.ietf.org/html/draft-ietf-tokbind-https">{@code
         * Sec-Token-Binding}</a> header field name.
         */
        public static final String SEC_TOKEN_BINDING = "Sec-Token-Binding";
        /**
         * The HTTP <a href="https://tools.ietf.org/html/draft-ietf-tokbind-ttrp">{@code
         * Sec-Provided-Token-Binding-ID}</a> header field name.
         */
        public static final String SEC_PROVIDED_TOKEN_BINDING_ID = "Sec-Provided-Token-Binding-ID";
        /**
         * The HTTP <a href="https://tools.ietf.org/html/draft-ietf-tokbind-ttrp">{@code
         * Sec-Referred-Token-Binding-ID}</a> header field name.
         */
        public static final String SEC_REFERRED_TOKEN_BINDING_ID = "Sec-Referred-Token-Binding-ID";

    }

    /**
     * <p>Note: It's copied from Google Guava under Apache License 2.0 and may be modified.</p>
     * 
     * Common HTTP header values for Content-Type and other headers.
     */
    public static final class Values {

        /**
         * Private constructor to prevent instantiation of this constants class.
         */
        private Values() {
            // singleton.
        }

        // application/content type.
        /**
         * The default HTML form content type: {@code application/x-www-form-urlencoded}.
         */
        public static final String APPLICATION_URL_ENCODED = "application/x-www-form-urlencoded";

        /** The XML content type: {@code application/xml}. */
        public static final String APPLICATION_XML = "application/xml";

        /** The JSON content type: {@code application/json}. */
        public static final String APPLICATION_JSON = "application/json";

        /** The Kryo serialization content type: {@code application/kryo}. */
        public static final String APPLICATION_KRYO = "application/kryo";

        /** The HTML content type: {@code text/html}. */
        public static final String TEXT_HTML = "text/html";

        /** The JSON text content type: {@code text/json}. */
        public static final String TEXT_JSON = "text/json";

        /** The XML text content type: {@code text/xml}. */
        public static final String TEXT_XML = "text/xml";

        /** The GIF image content type: {@code image/gif}. */
        public static final String IMAGE_GIF = "image/gif";

        /** The JPEG image content type: {@code image/jpg}. */
        public static final String IMAGE_JPG = "image/jpg";

        /** The UTF-8 character encoding: {@code utf-8}. */
        public static final String UTF_8 = "utf-8";

    }

    /**
     * <p>Note: It's copied from Google Guava under Apache License 2.0 and may be modified.</p>
     *
     * Values for the <a href="https://www.w3.org/TR/referrer-policy/">{@code Referrer-Policy}</a>
     * header.
     */
    public static final class ReferrerPolicyValues {

        /**
         * Private constructor to prevent instantiation of this constants class.
         */
        private ReferrerPolicyValues() {
        }

        /** No referrer information is sent. */
        public static final String NO_REFERRER = "no-referrer";

        /** The full URL is sent as referrer, except when navigating from HTTPS to HTTP. */
        public static final String NO_REFERRER_WHEN_DOWNGRADE = "no-referrer-when-downgrade";

        /** Only send the origin as referrer when the protocol security level stays the same. */
        public static final String SAME_ORIGIN = "same-origin";

        /** Only send the origin of the document as the referrer. */
        public static final String ORIGIN = "origin";

        /** Send origin as referrer, but only when the protocol security level stays the same. */
        public static final String STRICT_ORIGIN = "strict-origin";

        /** Send full URL for same-origin, only origin for cross-origin requests. */
        public static final String ORIGIN_WHEN_CROSS_ORIGIN = "origin-when-cross-origin";

        /** Send full URL for same-origin, only origin for cross-origin with same or higher security. */
        public static final String STRICT_ORIGIN_WHEN_CROSS_ORIGIN = "strict-origin-when-cross-origin";

        /** Always send the full URL as referrer. */
        public static final String UNSAFE_URL = "unsafe-url";
    }

    final Map<String, Object> map;

    HttpHeaders() {
        map = new HashMap<>();
    }

    @SuppressWarnings("rawtypes")
    HttpHeaders(final Map<String, ?> headers) {
        map = (Map) headers;
    }

    /**
     * Creates a new empty HttpHeaders instance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpHeaders headers = HttpHeaders.create();
     * }</pre>
     *
     * @return a new HttpHeaders instance
     */
    public static HttpHeaders create() {
        return new HttpHeaders();
    }

    /**
     * Creates a new HttpHeaders instance with a single header.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpHeaders headers = HttpHeaders.of("Content-Type", "application/json");
     * }</pre>
     *
     * @param name the header name
     * @param value the header value (can be String, Collection, Date, Instant, or any object)
     * @return a new HttpHeaders instance containing the specified header
     * @throws IllegalArgumentException if name is {@code null}
     */
    public static HttpHeaders of(final String name, final Object value) throws IllegalArgumentException {
        N.checkArgNotNull(name, cs.name);

        return create().set(name, value);
    }

    /**
     * Creates a new HttpHeaders instance with two headers.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpHeaders headers = HttpHeaders.of("Content-Type", "application/json",
     *                                      "Accept", "application/json");
     * }</pre>
     *
     * @param name1 the first header name
     * @param value1 the first header value
     * @param name2 the second header name
     * @param value2 the second header value
     * @return a new HttpHeaders instance containing the specified headers
     * @throws IllegalArgumentException if any name is {@code null}
     */
    public static HttpHeaders of(final String name1, final Object value1, final String name2, final Object value2) throws IllegalArgumentException {
        N.checkArgNotNull(name1, cs.name1);
        N.checkArgNotNull(name2, cs.name2);

        return create().set(name1, value1).set(name2, value2);
    }

    /**
     * Creates a new HttpHeaders instance with three headers.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpHeaders headers = HttpHeaders.of("Content-Type", "application/json",
     *                                      "Accept", "application/json",
     *                                      "Authorization", "Bearer token123");
     * }</pre>
     *
     * @param name1 the first header name
     * @param value1 the first header value
     * @param name2 the second header name
     * @param value2 the second header value
     * @param name3 the third header name
     * @param value3 the third header value
     * @return a new HttpHeaders instance containing the specified headers
     * @throws IllegalArgumentException if any name is {@code null}
     */
    public static HttpHeaders of(final String name1, final Object value1, final String name2, final Object value2, final String name3, final Object value3)
            throws IllegalArgumentException {
        N.checkArgNotNull(name1, cs.name1);
        N.checkArgNotNull(name2, cs.name2);
        N.checkArgNotNull(name3, cs.name3);

        return create().set(name1, value1).set(name2, value2).set(name3, value3);
    }

    /**
     * Creates a new HttpHeaders instance from a Map of headers.
     * The original map is used directly, not copied.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> map = Map.of("Content-Type", "application/json");
     * HttpHeaders headers = HttpHeaders.of(map);
     * }</pre>
     *
     * @param headers the map of header names to values
     * @return a new HttpHeaders instance backed by the provided map
     * @throws IllegalArgumentException if headers is {@code null}
     */
    public static HttpHeaders of(final Map<String, ?> headers) throws IllegalArgumentException {
        N.checkArgNotNull(headers);

        return new HttpHeaders(headers);
    }

    /**
     * Creates a new HttpHeaders instance with a copy of the provided headers map.
     * The headers are copied into a new map of the same type as the input.
     * Unlike {@link #of(Map)}, this method creates a copy rather than using the original map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> originalMap = new HashMap<>();
     * originalMap.put("Content-Type", "application/json");
     * HttpHeaders headers = HttpHeaders.copyOf(originalMap);
     * // Modifications to originalMap won't affect headers
     * originalMap.put("Accept", "text/plain");  // headers is unaffected
     * }</pre>
     *
     * @param headers the map of header names to values to copy
     * @return a new HttpHeaders instance with a copy of the headers
     * @throws IllegalArgumentException if headers is {@code null}
     */
    public static HttpHeaders copyOf(final Map<String, ?> headers) throws IllegalArgumentException {
        N.checkArgNotNull(headers);

        final Map<String, Object> copyMap = N.newMap(headers.getClass(), headers.size());

        return new HttpHeaders(copyMap).setAll(headers);
    }

    /**
     * Converts a header value to its string representation.
     * Handles special cases for Collections (joined with "; "), Dates (formatted as HTTP date),
     * and Instants (converted to Date then formatted).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String value = HttpHeaders.valueOf(Arrays.asList("gzip", "deflate"));   // "gzip; deflate"
     * String date = HttpHeaders.valueOf(new Date());                          // HTTP date format
     * }</pre>
     *
     * @param headerValue the header value to convert
     * @return the string representation of the header value
     */
    public static String valueOf(final Object headerValue) {
        if (headerValue instanceof String) {
            return (String) headerValue;
        } else if (headerValue instanceof Collection) {
            return Strings.join((Collection<?>) headerValue, "; ");
        } else if (headerValue instanceof Date) {
            return HttpDate.format((Date) headerValue);
        } else if (headerValue instanceof Instant) {
            return HttpDate.format(new Date(((Instant) headerValue).toEpochMilli()));   // NOSONAR
        } else {
            return N.stringOf(headerValue);
        }
    }

    /**
     * Sets the Content-Type header.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * headers.setContentType("application/json");
     * }</pre>
     *
     * @param contentType The content type value (e.g., "application/json")
     * @return This HttpHeaders instance for method chaining
     */
    public HttpHeaders setContentType(final String contentType) {
        //    if (hasHeader(HTTP.CONTENT_FORMAT)) {
        //        throw new IllegalArgumentException("The parameter 'contentFormat' has already been set");
        //    }

        set(Names.CONTENT_TYPE, contentType);

        return this;
    }

    /**
     * Sets the Content-Encoding header.
     * This header indicates what encodings have been applied to the representation,
     * typically used for compression.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * headers.setContentEncoding("gzip");
     * }</pre>
     *
     * @param contentEncoding The content encoding value (e.g., "gzip", "deflate", "br")
     * @return This HttpHeaders instance for method chaining
     */
    public HttpHeaders setContentEncoding(final String contentEncoding) {
        set(Names.CONTENT_ENCODING, contentEncoding);

        return this;
    }

    /**
     * Sets the Content-Language header.
     * This header indicates the natural language(s) of the intended audience
     * for the representation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * headers.setContentLanguage("en-US");
     * headers.setContentLanguage("en-US, en-GB");
     * }</pre>
     *
     * @param contentLanguage The content language value (e.g., "en-US", "fr-FR")
     * @return This HttpHeaders instance for method chaining
     */
    public HttpHeaders setContentLanguage(final String contentLanguage) {
        set(Names.CONTENT_LANGUAGE, contentLanguage);

        return this;
    }

    /**
     * Sets the Content-Length header.
     * This header indicates the size of the representation in bytes (decimal number).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * headers.setContentLength(1024);
     * }</pre>
     *
     * @param contentLength The content length in bytes
     * @return This HttpHeaders instance for method chaining
     */
    public HttpHeaders setContentLength(final long contentLength) {
        set(Names.CONTENT_LENGTH, contentLength);

        return this;
    }

    /**
     * Sets the User-Agent header.
     * The User-Agent header identifies the client software making the request,
     * typically including application name, version, and platform information.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * headers.setUserAgent("MyApp/1.0");
     * headers.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
     * }</pre>
     *
     * @param userAgent The user agent string
     * @return This HttpHeaders instance for method chaining
     */
    public HttpHeaders setUserAgent(final String userAgent) {
        set(Names.USER_AGENT, userAgent);

        return this;
    }

    /**
     * Sets the Cookie header.
     * The Cookie header contains stored HTTP cookies previously sent by the server.
     * Multiple name-value pairs are separated by semicolons.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * headers.setCookie("sessionId=abc123; userId=456");
     * headers.setCookie("token=xyz789");
     * }</pre>
     *
     * @param cookie The cookie string in the format "name=value; name2=value2"
     * @return This HttpHeaders instance for method chaining
     */
    public HttpHeaders setCookie(final String cookie) {
        set(Names.COOKIE, cookie);

        return this;
    }

    /**
     * Sets the Authorization header.
     * This header contains credentials for authenticating the client with the server.
     * Common authentication schemes include Basic (base64-encoded username:password),
     * Bearer (token-based authentication), and Digest.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * headers.setAuthorization("Bearer eyJhbGciOiJIUzI1NiIs...");
     * headers.setAuthorization("Basic dXNlcjpwYXNz");
     * }</pre>
     *
     * @param value The authorization value (e.g., "Bearer token123", "Basic credentials")
     * @return This HttpHeaders instance for method chaining
     */
    public HttpHeaders setAuthorization(final String value) {
        set(Names.AUTHORIZATION, value);

        return this;
    }

    /**
     * Sets the Authorization header with Basic authentication.
     * The username and password are Base64-encoded as per the Basic authentication scheme.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * headers.setBasicAuthentication("user", "pass123");
     * // Results in: Authorization: Basic dXNlcjpwYXNzMTIz
     * }</pre>
     *
     * @param username The username
     * @param password The password
     * @return This HttpHeaders instance for method chaining
     */
    public HttpHeaders setBasicAuthentication(final String username, final String password) {
        set(Names.AUTHORIZATION, "Basic " + Strings.base64Encode((username + ":" + password).getBytes(Charsets.UTF_8)));

        return this;
    }

    /**
     * Sets the Proxy-Authorization header.
     * This header contains credentials for authenticating the client with a proxy server.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * headers.setProxyAuthorization("Basic dXNlcjpwYXNz");
     * }</pre>
     *
     * @param value The proxy authorization value (e.g., "Basic base64credentials")
     * @return This HttpHeaders instance for method chaining
     */
    public HttpHeaders setProxyAuthorization(final String value) {
        set(Names.PROXY_AUTHORIZATION, value);

        return this;
    }

    /**
     * Sets the Cache-Control header.
     * This header specifies directives for caching mechanisms in both requests and responses.
     * It controls how, and for how long, the response should be cached by browsers and intermediary caches.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * headers.setCacheControl("no-cache, no-store");
     * headers.setCacheControl("max-age=3600");
     * headers.setCacheControl("public, max-age=86400");
     * }</pre>
     *
     * @param value The cache control directives (e.g., "no-cache", "max-age=3600")
     * @return This HttpHeaders instance for method chaining
     */
    public HttpHeaders setCacheControl(final String value) {
        set(Names.CACHE_CONTROL, value);

        return this;
    }

    /**
     * Sets the Connection header.
     * This header controls whether the network connection stays open after the current transaction.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * headers.setConnection("keep-alive");
     * headers.setConnection("close");
     * }</pre>
     *
     * @param value The connection value (e.g., "keep-alive", "close")
     * @return This HttpHeaders instance for method chaining
     */
    public HttpHeaders setConnection(final String value) {
        set(Names.CONNECTION, value);

        return this;
    }

    /**
     * Sets the Host header.
     * This header specifies the domain name of the server and optionally the TCP port number.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * headers.setHost("example.com");
     * headers.setHost("example.com:8080");
     * }</pre>
     *
     * @param value The host value (e.g., "example.com", "example.com:8080")
     * @return This HttpHeaders instance for method chaining
     */
    public HttpHeaders setHost(final String value) {
        set(Names.HOST, value);

        return this;
    }

    /**
     * Sets the From header.
     * This header contains the email address of the user making the request,
     * typically used for robot/spider identification.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * headers.setFrom("bot@example.com");
     * }</pre>
     *
     * @param value The email address of the user making the request
     * @return This HttpHeaders instance for method chaining
     */
    public HttpHeaders setFrom(final String value) {
        set(Names.FROM, value);

        return this;
    }

    /**
     * Sets the Accept header.
     * This header informs the server about the types of media the client can process and accept.
     * It's used for content negotiation, allowing the server to select an appropriate representation.
     * Quality values (q-factors) can be used to indicate preference order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * headers.setAccept("application/json, text/plain");
     * headers.setAccept("application/json;q=0.9, text/plain;q=0.8");
     * }</pre>
     *
     * @param value The media types the client can accept (e.g., "application/json")
     * @return This HttpHeaders instance for method chaining
     */
    public HttpHeaders setAccept(final String value) {
        set(Names.ACCEPT, value);

        return this;
    }

    /**
     * Sets the Accept-Encoding header.
     * This header indicates which content encodings (typically compression algorithms)
     * the client can understand. The server can use this to compress the response,
     * reducing bandwidth and improving performance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * headers.setAcceptEncoding("gzip, deflate, br");
     * headers.setAcceptEncoding("gzip");
     * headers.setAcceptEncoding("*");
     * }</pre>
     *
     * @param contentEncoding The acceptable encodings (e.g., "gzip, deflate", "br")
     * @return This HttpHeaders instance for method chaining
     */
    public HttpHeaders setAcceptEncoding(final String contentEncoding) {
        set(Names.ACCEPT_ENCODING, contentEncoding);

        return this;
    }

    /**
     * Sets the Accept-Charset header.
     * This header indicates which character sets are acceptable in the response.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * headers.setAcceptCharset("utf-8, iso-8859-1");
     * headers.setAcceptCharset("utf-8");
     * }</pre>
     *
     * @param acceptCharset The acceptable character sets (e.g., "utf-8, iso-8859-1")
     * @return This HttpHeaders instance for method chaining
     */
    public HttpHeaders setAcceptCharset(final String acceptCharset) {
        set(Names.ACCEPT_CHARSET, acceptCharset);

        return this;
    }

    /**
     * Sets the Accept-Language header.
     * This header indicates which natural languages are preferred in the response.
     * Quality values (q-factors) can be used to specify preference order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * headers.setAcceptLanguage("en-US,en;q=0.9");
     * headers.setAcceptLanguage("fr-FR");
     * }</pre>
     *
     * @param acceptLanguage The acceptable languages (e.g., "en-US,en;q=0.9", "fr-FR,fr;q=0.8,en;q=0.5")
     * @return This HttpHeaders instance for method chaining
     */
    public HttpHeaders setAcceptLanguage(final String acceptLanguage) {
        set(Names.ACCEPT_LANGUAGE, acceptLanguage);

        return this;
    }

    /**
     * Sets the Accept-Ranges header.
     * This header indicates whether the server accepts range requests for a resource.
     * Typically used in responses to advertise partial content support.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * headers.setAcceptRanges("bytes");
     * headers.setAcceptRanges("none");
     * }</pre>
     *
     * @param acceptRanges The acceptable range units (e.g., "bytes", "none")
     * @return This HttpHeaders instance for method chaining
     */
    public HttpHeaders setAcceptRanges(final String acceptRanges) {
        set(Names.ACCEPT_RANGES, acceptRanges);

        return this;
    }

    /**
     * Sets a header with the specified name and value.
     * If a header with this name already exists, it is replaced.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * headers.set("X-Custom-Header", "custom-value");
     * headers.set("X-Request-ID", UUID.randomUUID());
     * }</pre>
     *
     * @param name The header name
     * @param value The header value (can be String, Collection, Date, Instant, or any object)
     * @return This HttpHeaders instance for method chaining
     */
    public HttpHeaders set(final String name, final Object value) {
        map.put(name, value);

        return this;
    }

    /**
     * Sets all headers from the provided map.
     * Existing headers with the same names are replaced.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> extraHeaders = Map.of(
     *     "X-API-Key", "key123",
     *     "X-Request-ID", "req456"
     * );
     * headers.setAll(extraHeaders);
     * }</pre>
     *
     * @param m The map of header names to values
     * @return This HttpHeaders instance for method chaining
     */
    public HttpHeaders setAll(final Map<String, ?> m) {
        map.putAll(m);

        return this;
    }

    /**
     * Gets the value of a header.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Object contentType = headers.get("Content-Type");
     * }</pre>
     *
     * @param headerName The name of the header to retrieve
     * @return The header value, or {@code null} if not present
     */
    public Object get(final String headerName) {
        return map.get(headerName);
    }

    /**
     * Removes a header.
     * This method removes the specified header from the HttpHeaders instance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Object oldValue = headers.remove("X-Custom-Header");
     * }</pre>
     *
     * @param headerName The name of the header to remove
     * @return The previous value associated with the header, or {@code null} if there was no mapping
     */
    public Object remove(final String headerName) {
        return map.remove(headerName);
    }

    /**
     * Returns a set of all header names in this HttpHeaders.
     * The returned set is backed by the underlying map, so changes to the HttpHeaders
     * are reflected in the set, and vice-versa. To remove headers, use {@link #remove(String)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Set<String> names = headers.headerNameSet();
     * for (String name : names) {
     *     System.out.println(name + ": " + headers.get(name));
     * }
     * }</pre>
     *
     * @return A set view of the header names backed by the underlying map
     */
    public Set<String> headerNameSet() {
        return map.keySet();
    }

    /**
     * Performs the given action for each header in this HttpHeaders.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * headers.forEach((name, value) -> 
     *     System.out.println(name + ": " + value));
     * }</pre>
     *
     * @param action The action to be performed for each header
     */
    public void forEach(final BiConsumer<? super String, ? super Object> action) {
        map.forEach(action);
    }

    /**
     * Removes all headers from this HttpHeaders.
     * After this call, the HttpHeaders instance will be empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * headers.clear();
     * }</pre>
     */
    public void clear() {
        map.clear();
    }

    /**
     * Checks if this HttpHeaders is empty.
     * Returns {@code true} if this HttpHeaders contains no header entries.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * if (headers.isEmpty()) {
     *     // No headers present
     * }
     * }</pre>
     *
     * @return {@code true} if this HttpHeaders contains no headers; {@code false} otherwise
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * Returns a new map containing all headers.
     * LinkedHashMap or SortedMap instances preserve their ordering in the returned map.
     * For other map types, a HashMap is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> headerMap = headers.toMap();
     * // Modifications to headerMap won't affect the original HttpHeaders
     * }</pre>
     *
     * @return A new map containing all headers
     */
    public Map<String, Object> toMap() {
        return map instanceof LinkedHashMap || map instanceof SortedMap ? new LinkedHashMap<>(map) : new HashMap<>(map);
    }

    /**
     * Creates a copy of this HttpHeaders instance.
     * The copy contains the same headers but is independent of the original.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpHeaders copy = headers.copy();
     * copy.set("X-Modified", "true");   // Original is not affected
     * }</pre>
     *
     * @return A new HttpHeaders instance with a copy of all headers
     */
    public HttpHeaders copy() {
        final Map<String, Object> copyMap = N.newMap(map.getClass(), map.size());

        return new HttpHeaders(copyMap).setAll(map);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    /**
     * Checks if this HttpHeaders is equal to another object.
     * Two HttpHeaders instances are equal if they contain the same headers.
     * 
     * @param obj The object to compare with
     * @return {@code true} if the objects are equal
     */
    @Override
    public boolean equals(final Object obj) {
        return obj instanceof HttpHeaders && map.equals(((HttpHeaders) obj).map);
    }

    @Override
    public String toString() {
        return map.toString();
    }
}
