/*
 * Copyright (C) 2016 HaiYang Li
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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.N;

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public final class HttpHeaders {

    /**
     * Copied from Google Guava under Apache License v2.
     */
    public static final class Names {
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
        /** The HTTP {@code Accept-Charset} header field name. */
        public static final String ACCEPT_CHARSET = "Accept-Charset";
        /** The HTTP {@code Accept-Encoding} header field name. */
        public static final String ACCEPT_ENCODING = "Accept-Encoding";
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
         *
         * @since 27.0
         */
        public static final String EARLY_DATA = "Early-Data";
        /** The HTTP {@code Expect} header field name. */
        public static final String EXPECT = "Expect";
        /** The HTTP {@code From} header field name. */
        public static final String FROM = "From";
        /**
         * The HTTP <a href="https://tools.ietf.org/html/rfc7239">{@code Forwarded}</a> header field name.
         *
         * @since 20.0
         */
        public static final String FORWARDED = "Forwarded";
        /**
         * The HTTP {@code Follow-Only-When-Prerender-Shown} header field name.
         *
         * @since 17.0
         */
        @Beta
        public static final String FOLLOW_ONLY_WHEN_PRERENDER_SHOWN = "Follow-Only-When-Prerender-Shown";
        /** The HTTP {@code Host} header field name. */
        public static final String HOST = "Host";
        /**
         * The HTTP <a href="https://tools.ietf.org/html/rfc7540#section-3.2.1">{@code HTTP2-Settings}
         * </a> header field name.
         *
         * @since 24.0
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
         *
         * @since 23.4
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
        /** The HTTP {@code Content-Encoding} header field name. */
        public static final String CONTENT_ENCODING = "Content-Encoding";
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
         *
         * @since 15.0
         */
        public static final String CONTENT_SECURITY_POLICY = "Content-Security-Policy";
        /**
         * The HTTP <a href="http://w3.org/TR/CSP/#content-security-policy-report-only-header-field">
         * {@code Content-Security-Policy-Report-Only}</a> header field name.
         *
         * @since 15.0
         */
        public static final String CONTENT_SECURITY_POLICY_REPORT_ONLY = "Content-Security-Policy-Report-Only";
        /**
         * The HTTP nonstandard {@code X-Content-Security-Policy} header field name. It was introduced in
         * <a href="https://www.w3.org/TR/2011/WD-CSP-20111129/">CSP v.1</a> and used by the Firefox until
         * version 23 and the Internet Explorer version 10. Please, use {@link #CONTENT_SECURITY_POLICY}
         * to pass the CSP.
         *
         * @since 20.0
         */
        public static final String X_CONTENT_SECURITY_POLICY = "X-Content-Security-Policy";
        /**
         * The HTTP nonstandard {@code X-Content-Security-Policy-Report-Only} header field name. It was
         * introduced in <a href="https://www.w3.org/TR/2011/WD-CSP-20111129/">CSP v.1</a> and used by the
         * Firefox until version 23 and the Internet Explorer version 10. Please, use {@link
         * #CONTENT_SECURITY_POLICY_REPORT_ONLY} to pass the CSP.
         *
         * @since 20.0
         */
        public static final String X_CONTENT_SECURITY_POLICY_REPORT_ONLY = "X-Content-Security-Policy-Report-Only";
        /**
         * The HTTP nonstandard {@code X-WebKit-CSP} header field name. It was introduced in <a
         * href="https://www.w3.org/TR/2011/WD-CSP-20111129/">CSP v.1</a> and used by the Chrome until
         * version 25. Please, use {@link #CONTENT_SECURITY_POLICY} to pass the CSP.
         *
         * @since 20.0
         */
        public static final String X_WEBKIT_CSP = "X-WebKit-CSP";
        /**
         * The HTTP nonstandard {@code X-WebKit-CSP-Report-Only} header field name. It was introduced in
         * <a href="https://www.w3.org/TR/2011/WD-CSP-20111129/">CSP v.1</a> and used by the Chrome until
         * version 25. Please, use {@link #CONTENT_SECURITY_POLICY_REPORT_ONLY} to pass the CSP.
         *
         * @since 20.0
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
         *
         * @since 23.6
         */
        public static final String SERVER_TIMING = "Server-Timing";
        /**
         * The HTTP <a href="https://www.w3.org/TR/service-workers/#update-algorithm">{@code
         * Service-Worker-Allowed}</a> header field name.
         *
         * @since 20.0
         */
        public static final String SERVICE_WORKER_ALLOWED = "Service-Worker-Allowed";
        /** The HTTP {@code Set-Cookie} header field name. */
        public static final String SET_COOKIE = "Set-Cookie";
        /** The HTTP {@code Set-Cookie2} header field name. */
        public static final String SET_COOKIE2 = "Set-Cookie2";
        /**
         * The HTTP <a href="http://tools.ietf.org/html/rfc6797#section-6.1">{@code
         * Strict-Transport-Security}</a> header field name.
         *
         * @since 15.0
         */
        public static final String STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";
        /**
         * The HTTP <a href="http://www.w3.org/TR/resource-timing/#cross-origin-resources">{@code
         * Timing-Allow-Origin}</a> header field name.
         *
         * @since 15.0
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
         *
         * @since 20.0
         */
        public static final String X_FORWARDED_HOST = "X-Forwarded-Host";
        /**
         * The HTTP <a href="http://goo.gl/YtV2at">{@code X-Forwarded-Port}</a> header field name.
         *
         * @since 20.0
         */
        public static final String X_FORWARDED_PORT = "X-Forwarded-Port";
        /** The HTTP {@code X-Frame-Options} header field name. */
        public static final String X_FRAME_OPTIONS = "X-Frame-Options";
        /** The HTTP {@code X-Powered-By} header field name. */
        public static final String X_POWERED_BY = "X-Powered-By";
        /**
         * The HTTP <a href="http://tools.ietf.org/html/draft-evans-palmer-key-pinning">{@code
         * Public-Key-Pins}</a> header field name.
         *
         * @since 15.0
         */
        @Beta
        public static final String PUBLIC_KEY_PINS = "Public-Key-Pins";
        /**
         * The HTTP <a href="http://tools.ietf.org/html/draft-evans-palmer-key-pinning">{@code
         * Public-Key-Pins-Report-Only}</a> header field name.
         *
         * @since 15.0
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
         *
         * @since 24.1
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
         *
         * @since 19.0
         */
        public static final String PING_FROM = "Ping-From";
        /**
         * The HTTP <a href="http://html.spec.whatwg.org/multipage/semantics.html#hyperlink-auditing">
         * {@code Ping-To}</a> header field name.
         *
         * @since 19.0
         */
        public static final String PING_TO = "Ping-To";

        /**
         * The HTTP <a href="https://github.com/mikewest/sec-metadata">{@code Sec-Metadata}</a> header
         * field name.
         *
         * @since 26.0
         */
        public static final String SEC_METADATA = "Sec-Metadata";
        /**
         * The HTTP <a href="https://tools.ietf.org/html/draft-ietf-tokbind-https">{@code
         * Sec-Token-Binding}</a> header field name.
         *
         * @since 25.1
         */
        public static final String SEC_TOKEN_BINDING = "Sec-Token-Binding";
        /**
         * The HTTP <a href="https://tools.ietf.org/html/draft-ietf-tokbind-ttrp">{@code
         * Sec-Provided-Token-Binding-ID}</a> header field name.
         *
         * @since 25.1
         */
        public static final String SEC_PROVIDED_TOKEN_BINDING_ID = "Sec-Provided-Token-Binding-ID";
        /**
         * The HTTP <a href="https://tools.ietf.org/html/draft-ietf-tokbind-ttrp">{@code
         * Sec-Referred-Token-Binding-ID}</a> header field name.
         *
         * @since 25.1
         */
        public static final String SEC_REFERRED_TOKEN_BINDING_ID = "Sec-Referred-Token-Binding-ID";

    }

    public static final class Values {
        private Values() {
            // singleton.
        }

        // application/content type.
        /**
         * The default HTML form content type.
         */
        public static final String APPLICATION_URL_ENCODED = "application/x-www-form-urlencoded";
        public static final String APPLICATION_XML = "application/xml";
        public static final String APPLICATION_JSON = "application/json";
        public static final String APPLICATION_KRYO = "application/kryo";
        public static final String TEXT_HTML = "text/html";
        public static final String TEXT_JSON = "text/json";
        public static final String TEXT_XML = "text/xml";
        public static final String IMAGE_GIF = "image/gif";
        public static final String IMAGE_JPG = "image/jpg";
        public static final String UTF_8 = "UTF-8";

    }

    /**
     * Copied from Google Guava under Apache License v2.
     * 
     * <br />
     * 
     * Values for the <a href="https://www.w3.org/TR/referrer-policy/">{@code Referrer-Policy}</a>
     * header.
     *
     * @since 23.4
     */
    public static final class ReferrerPolicyValues {
        private ReferrerPolicyValues() {
        }

        public static final String NO_REFERRER = "no-referrer";
        public static final String NO_REFFERER_WHEN_DOWNGRADE = "no-referrer-when-downgrade";
        public static final String SAME_ORIGIN = "same-origin";
        public static final String ORIGIN = "origin";
        public static final String STRICT_ORIGIN = "strict-origin";
        public static final String ORIGIN_WHEN_CROSS_ORIGIN = "origin-when-cross-origin";
        public static final String STRICT_ORIGIN_WHEN_CROSS_ORIGIN = "strict-origin-when-cross-origin";
        public static final String UNSAFE_URL = "unsafe-url";
    }

    final Map<String, Object> map;

    HttpHeaders() {
        this.map = new HashMap<>();
    }

    HttpHeaders(final Map<String, Object> headers) {
        this.map = headers;
    }

    public static HttpHeaders create() {
        return new HttpHeaders();
    }

    public static HttpHeaders of(final Map<String, Object> headers) {
        N.checkArgNotNull(headers);

        return new HttpHeaders(headers);
    }

    public static HttpHeaders copyOf(final Map<String, Object> headers) {
        N.checkArgNotNull(headers);

        return new HttpHeaders(new HashMap<>(headers));
    }

    public HttpHeaders setAcceptCharset(String acceptCharset) {
        set(Names.ACCEPT_CHARSET, acceptCharset);

        return this;
    }

    public HttpHeaders setAcceptEncoding(String acceptEncoding) {
        set(Names.ACCEPT_ENCODING, acceptEncoding);

        return this;
    }

    public HttpHeaders setAcceptLanguage(String acceptLanguage) {
        set(Names.ACCEPT_LANGUAGE, acceptLanguage);

        return this;
    }

    public HttpHeaders setAcceptRanges(String acceptRanges) {
        set(Names.ACCEPT_RANGES, acceptRanges);

        return this;
    }

    public HttpHeaders setContentType(String contentType) {
        //    if (hasHeader(HTTP.CONTENT_FORMAT)) {
        //        throw new IllegalArgumentException("The parameter 'contentFormat' has already been set");
        //    }

        set(Names.CONTENT_TYPE, contentType);

        return this;
    }

    public HttpHeaders setContentEncoding(String acceptEncoding) {
        set(Names.CONTENT_ENCODING, acceptEncoding);

        return this;
    }

    public HttpHeaders setContentLanguage(String acceptLanguage) {
        set(Names.CONTENT_LANGUAGE, acceptLanguage);

        return this;
    }

    public HttpHeaders setContentLength(long contentLength) {
        set(Names.CONTENT_LENGTH, contentLength);

        return this;
    }

    public HttpHeaders setUserAgent(String userAgent) {
        set(Names.USER_AGENT, userAgent);

        return this;
    }

    public HttpHeaders setCookie(String cookie) {
        set(Names.COOKIE, cookie);

        return this;
    }

    public HttpHeaders set(String name, Object value) {
        map.put(name, value);

        return this;
    }

    public HttpHeaders setAll(Map<? extends String, ? extends Object> m) {
        map.putAll(m);

        return this;
    }

    public Object get(String headerName) {
        return map.get(headerName);
    }

    public Object remove(String headerName) {
        return map.remove(headerName);
    }

    public Set<String> headerNameSet() {
        return map.keySet();
    }

    public void clear() {
        map.clear();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public HttpHeaders copy() {
        return new HttpHeaders().setAll(this.map);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof HttpHeaders && this.map.equals(((HttpHeaders) obj).map);
    }

    @Override
    public String toString() {
        return map.toString();
    }
}
