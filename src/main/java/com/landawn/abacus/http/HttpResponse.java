/*
 * Copyright (C) 2019 HaiYang Li
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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.URLEncodedUtil;
import com.landawn.abacus.util.cs;

/**
 * Represents an immutable HTTP response containing status code, headers, and body.
 * This class encapsulates all the information returned from an HTTP request,
 * including timing information, response status, headers, and the response body.
 *
 * <p>The response body is stored as a byte array and can be deserialized to various types
 * using the {@link #body(Class)} or {@link #body(Type)} methods. {@link #body()} and
 * {@link #headers()} return defensive copies (or unmodifiable views) so the internal state
 * cannot be altered through them.</p>
 *
 * <p><b>Thread Safety:</b> Instances are effectively immutable and safe to share between threads.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * HttpResponse response = httpClient.get(HttpResponse.class);
 *
 * // Check if request was successful
 * if (response.isSuccessful()) {
 *     // Get response as string
 *     String body = response.body(String.class);
 *
 *     // Get status code
 *     int status = response.statusCode();
 *
 *     // Get headers
 *     Map<String, List<String>> headers = response.headers();
 * }
 * }</pre>
 *
 * @see HttpClient
 * @see HttpRequest
 * @see HttpHeaders
 */
public class HttpResponse {
    private final String requestUrl;

    private final long requestSentAtMillis;

    private final long responseReceivedAtMillis;

    private final int statusCode;

    private final String message;

    private final Map<String, List<String>> headers;

    private final byte[] body;

    private final ContentFormat bodyFormat;

    private final Charset respCharset;

    HttpResponse(final String requestUrl, final long requestSentAtMillis, final long responseReceivedAtMillis, final int statusCode, final String message,
            final Map<String, List<String>> headers, final byte[] body, final ContentFormat bodyFormat, final Charset respCharset) {
        this.requestUrl = requestUrl;
        this.requestSentAtMillis = requestSentAtMillis;
        this.responseReceivedAtMillis = responseReceivedAtMillis;
        this.statusCode = statusCode;
        this.message = message;
        this.headers = copyHeaders(headers);
        this.body = copyBody(body);
        this.bodyFormat = bodyFormat == null ? ContentFormat.NONE : bodyFormat;
        this.respCharset = respCharset;
    }

    /**
     * Checks if the response status code indicates success.
     * Returns {@code true} if the status code is in the range {@code [200, 300)}, which means the request
     * was successfully received, understood, and accepted.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpResponse response = client.get(HttpResponse.class);
     * if (response.isSuccessful()) {
     *     // Process successful response
     * } else {
     *     // Handle error
     * }
     * }</pre>
     *
     * @return {@code true} if the response indicates success, {@code false} otherwise
     */
    public boolean isSuccessful() {
        return HttpUtil.isSuccessfulResponseCode(statusCode);
    }

    /**
     * Gets the URL of the original request that generated this response.
     *
     * @return The request URL as a string
     */
    public String requestUrl() {
        return requestUrl;
    }

    /**
     * Gets the timestamp when the request was sent, in milliseconds since epoch.
     *
     * @return The request sent timestamp in milliseconds
     */
    public long requestSentAtMillis() {
        return requestSentAtMillis;
    }

    /**
     * Gets the timestamp when the response was received, in milliseconds since epoch.
     *
     * @return The response received timestamp in milliseconds
     */
    public long responseReceivedAtMillis() {
        return responseReceivedAtMillis;
    }

    /**
     * Gets the HTTP status code of the response.
     * Common status codes include:
     * <ul>
     *   <li>200 - OK</li>
     *   <li>201 - Created</li>
     *   <li>400 - Bad Request</li>
     *   <li>404 - Not Found</li>
     *   <li>500 - Internal Server Error</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int status = response.statusCode();
     * if (status == 404) {
     *     // Handle not found
     * }
     * }</pre>
     *
     * @return The HTTP status code
     */
    public int statusCode() {
        return statusCode;
    }

    /**
     * Gets the status message associated with the status code.
     * For example, "OK" for status code 200, "Not Found" for 404.
     *
     * @return The status message
     */
    public String message() {
        return message;
    }

    /**
     * Gets all response headers as a map.
     * Each header name maps to a list of values, as headers can have multiple values.
     * The returned map and its value lists are unmodifiable.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, List<String>> headers = response.headers();
     * List<String> contentType = headers.get("Content-Type");
     * }</pre>
     *
     * @return an unmodifiable map of header names to their values, or {@code null} if no headers were received
     */
    public Map<String, List<String>> headers() {
        return headers;
    }

    /**
     * Returns a copy of the raw response body as a byte array.
     * Each call returns a new defensive copy of the internal byte array to prevent
     * external modification of the response state.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] rawData = response.body();
     * // Process raw bytes
     * }</pre>
     *
     * @return a copy of the response body as a byte array, or {@code null} if no body was received
     */
    public byte[] body() {
        return copyBody(body);
    }

    /**
     * Deserializes the response body to the specified type.
     * The deserialization method is determined by the {@code Content-Type} header of the response.
     * Supported types include:
     * <ul>
     *   <li>{@code String.class} - returns the body decoded with the response charset
     *       (derived from the {@code Content-Type} header, defaulting to UTF-8 if absent)</li>
     *   <li>{@code byte[].class} - returns a copy of the raw response bytes</li>
     *   <li>Any other class - deserializes based on content format (JSON, XML, Kryo,
     *       form URL-encoded). For unknown/{@code NONE} content formats a best-effort
     *       {@link N#convert(Object, Class)} is attempted before falling through to the JSON parser.</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get as string
     * String text = response.body(String.class);
     *
     * // Get as custom object (assumes JSON response)
     * User user = response.body(User.class);
     *
     * // Get as raw bytes
     * byte[] data = response.body(byte[].class);
     * }</pre>
     *
     * @param <T> The type to deserialize to
     * @param resultClass The class of the expected response object. Must not be {@code null}.
     * @return The deserialized response body, or {@code null} if no body was received
     * @throws IllegalArgumentException if {@code resultClass} is {@code null}
     */
    public <T> T body(final Class<T> resultClass) throws IllegalArgumentException {
        N.checkArgNotNull(resultClass, cs.resultClass);

        if (body == null) {
            return null;
        }

        if (resultClass.equals(String.class)) {
            return (T) new String(body, respCharset);
        } else if (byte[].class.equals(resultClass)) {
            return (T) copyBody(body);
        } else {
            if (bodyFormat == ContentFormat.KRYO && HttpUtil.kryoParser != null) {
                return HttpUtil.kryoParser.deserialize(new ByteArrayInputStream(body), resultClass);
            } else if (bodyFormat == ContentFormat.FORM_URL_ENCODED) {
                return URLEncodedUtil.decode(new String(body, respCharset), resultClass);
            } else {
                // For NONE / unrecognized content formats, the configured parser falls back to
                // JSON. That works for JSON-shaped bodies but produces confusing parse errors
                // for plain-text responses being decoded into scalar types. Try a direct String
                // conversion first via N.convert (handles primitives, enums, dates, etc.) and
                // fall through to the parser only when N.convert can't handle the target type.
                final String text = new String(body, respCharset);
                if (bodyFormat == null || bodyFormat == ContentFormat.NONE) {
                    try {
                        return N.convert(text, resultClass);
                    } catch (final RuntimeException ignored) {
                        // N.convert can't handle compound types (beans, collections); fall through.
                    }
                }
                return HttpUtil.getParser(bodyFormat).deserialize(text, resultClass);
            }
        }
    }

    /**
     * Deserializes the response body to the specified parameterized type.
     * This method is useful for deserializing to generic types like {@code List<User>} or {@code Map<String, Object>}.
     * The body bytes are decoded using the response charset derived from the {@code Content-Type}
     * header (defaulting to UTF-8 if absent) before parsing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Deserialize to a list of users
     * Type<List<User>> listType = Type.of("List<User>");
     * List<User> users = response.body(listType);
     *
     * // Deserialize to a map
     * Type<Map<String, Object>> mapType = Type.of("Map<String, Object>");
     * Map<String, Object> data = response.body(mapType);
     *
     * // Alternative: using Type.of()
     * List<User> users2 = response.body(Type.of("List<User>"));
     * }</pre>
     *
     * @param <T> The type to deserialize to
     * @param resultType The type information including generic parameters. Must not be {@code null}.
     * @return The deserialized response body, or {@code null} if no body was received
     * @throws IllegalArgumentException if {@code resultType} is {@code null}
     */
    public <T> T body(final Type<T> resultType) throws IllegalArgumentException {
        N.checkArgNotNull(resultType, cs.resultType);

        if (body == null) {
            return null;
        }

        if (resultType.javaType().equals(String.class)) {
            return (T) new String(body, respCharset);
        } else if (resultType.javaType().equals(byte[].class)) {
            return (T) copyBody(body);
        } else {
            if (bodyFormat == ContentFormat.KRYO && HttpUtil.kryoParser != null) {
                return HttpUtil.kryoParser.deserialize(new ByteArrayInputStream(body), null, resultType);
            } else if (bodyFormat == ContentFormat.FORM_URL_ENCODED) {
                return N.convert(URLEncodedUtil.decode(new String(body, respCharset), resultType.javaType()), resultType);
            } else if (bodyFormat != null && bodyFormat.name().contains("JSON")) {
                return N.fromJson(new String(body, respCharset), resultType);
            } else if (bodyFormat != null && bodyFormat.name().contains("XML")) {
                return N.fromXml(new String(body, respCharset), resultType);
            } else {
                // Same defensive path as body(Class): for NONE / unknown formats, try a String
                // conversion before falling through to the JSON parser default.
                final String text = new String(body, respCharset);
                if (bodyFormat == null || bodyFormat == ContentFormat.NONE) {
                    try {
                        return N.convert(text, resultType);
                    } catch (final RuntimeException ignored) {
                        // Fall through to parser path for compound types.
                    }
                }
                return HttpUtil.getParser(bodyFormat).deserialize(text, resultType);
            }
        }
    }

    private static Map<String, List<String>> copyHeaders(final Map<String, List<String>> headers) {
        if (headers == null) {
            return null;
        }

        final Map<String, List<String>> copiedHeaders = new LinkedHashMap<>(headers.size());

        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            final List<String> values = entry.getValue();
            copiedHeaders.put(entry.getKey(), values == null ? null : Collections.unmodifiableList(new ArrayList<>(values)));
        }

        return Collections.unmodifiableMap(copiedHeaders);
    }

    private static byte[] copyBody(final byte[] body) {
        return body == null ? null : body.clone();
    }

    /**
     * Computes the hash code for this HttpResponse.
     * The hash code is based on the request URL, status code, message, headers, body format, and body content.
     *
     * @return the hash code value for this object
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((requestUrl == null) ? 0 : requestUrl.hashCode());
        result = prime * result + statusCode;
        result = prime * result + ((message == null) ? 0 : message.hashCode());
        result = prime * result + ((headers == null) ? 0 : headers.hashCode());
        result = prime * result + bodyFormat.hashCode();
        return prime * result + ((body == null) ? 0 : N.hashCode(body));
    }

    /**
     * Determines whether this HttpResponse is equal to another object.
     * Two HttpResponse objects are considered equal if they have the same request URL,
     * status code, message, headers, body format, and body content.
     *
     * @param obj The object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @SuppressFBWarnings
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof HttpResponse other) {
            return N.equals(requestUrl, other.requestUrl) && statusCode == other.statusCode && N.equals(message, other.message)
                    && N.equals(headers, other.headers) && N.equals(bodyFormat, other.bodyFormat) && N.equals(body, other.body);
        }

        return false;
    }

    /**
     * Returns a string representation of this HttpResponse.
     * The string includes the status code, message, request URL, and elapsed time.
     *
     * <p><b>Example output:</b></p>
     * <pre>{@code
     * HttpResponse{statusCode=200, message=OK, url=http://localhost:18080/users, elapsedTime=123}
     * }</pre>
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return "HttpResponse{statusCode=" + statusCode + ", message=" + message + ", url=" + requestUrl + ", elapsedTime="
                + (responseReceivedAtMillis - requestSentAtMillis) + '}';
    }
}
