/*
 * Copyright (c) 2026, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.exception;

import java.io.IOException;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.landawn.abacus.http.HttpClient;
import com.landawn.abacus.http.HttpResponse;
import com.landawn.abacus.http.HttpUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

/**
 * Thrown when an HTTP request completes with a non-successful status code and the caller did not ask
 * for the raw {@link HttpResponse}.
 *
 * <p>This is a subclass of {@link UncheckedIOException}, so existing {@code catch} blocks keep
 * working; it additionally exposes the status code, status message, response headers and a bounded
 * prefix of the error body as structured data, instead of leaving them only inside a message
 * string.</p>
 *
 * <p>The captured body is deliberately truncated (see {@link HttpUtil#MAX_ERROR_BODY_SIZE}) so
 * that an arbitrarily large error page cannot be materialized into an exception. To read an error
 * response in full, request {@link HttpResponse} as the result type instead, which returns the
 * response rather than throwing.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * try {
 *     User user = client.get(User.class);
 * } catch (HttpResponseException e) {
 *     if (e.statusCode() == 404) {
 *         return null;
 *     }
 *     logger.warn("{} failed: {} {}", e.requestUrl(), e.statusCode(), e.responseBody());
 *     throw e;
 * }
 * }</pre>
 *
 * @see HttpResponse
 * @see HttpClient
 */
public class HttpResponseException extends UncheckedIOException {

    @Serial
    private static final long serialVersionUID = 6237952747839574661L;

    /** The URL of the failed request, or {@code null} if unavailable. */
    private final String requestUrl;

    /** The HTTP response status code. */
    private final int statusCode;

    /** The HTTP response status message, or {@code null} if unavailable. */
    private final String responseMessage;

    /** Immutable snapshot, including value lists, retained by Java serialization. */
    private final Map<String, List<String>> headers;

    /** The captured, possibly truncated error body; empty when no body was supplied. */
    private final String responseBody;

    /** Defensive snapshot of the bounded raw error body, or {@code null} when unavailable. */
    private final byte[] rawResponseBody;

    /** Original decoding failure, also retained in the cause chain, or {@code null}. */
    private final Throwable responseBodyDecodingFailure;

    /**
     * Creates an HTTP failure retaining an undecodable error body's raw prefix and its decoding failure.
     * Raw bytes are copied and limited to {@link HttpUtil#MAX_ERROR_BODY_SIZE}; the text is best effort.
     *
     * <p>Java serialization preserves both the raw prefix and the decoding failure. As with other exceptions
     * that retain a cause, serialization requires the supplied failure's reachable state to be serializable.</p>
     *
     * @param requestUrl request URL, or null when unavailable
     * @param statusCode HTTP status
     * @param responseMessage reason phrase, or null
     * @param headers response headers, copied
     * @param responseBody best-effort text, or null
     * @param rawResponseBody raw bytes, or null when unavailable
     * @param decodingFailure failure while decoding the body, or null
     */
    public HttpResponseException(final String requestUrl, final int statusCode, final String responseMessage, final Map<String, List<String>> headers,
            final String responseBody, final byte[] rawResponseBody, final Throwable decodingFailure) {
        this(requestUrl, statusCode, responseMessage, headers, responseBody,
                new IOException(buildMessage(statusCode, responseMessage, responseBody), decodingFailure), rawResponseBody, decodingFailure);
    }

    /**
     * Returns the raw error-body prefix captured before decoding failed.
     *
     * @return a defensive copy of at most {@link HttpUtil#MAX_ERROR_BODY_SIZE} bytes, or {@code null} when unavailable
     */
    public byte[] rawResponseBody() {
        return rawResponseBody == null ? null : rawResponseBody.clone();
    }

    /**
     * Returns the original failure encountered while decoding the error body.
     * The same failure is retained as the cause of this exception's {@link IOException} cause.
     *
     * @return the body decoding failure, or {@code null} when none was recorded
     */
    public Throwable responseBodyDecodingFailure() {
        return responseBodyDecodingFailure;
    }

    /**
     * Creates a new {@code HttpResponseException}.
     *
     * <p>The exception message is {@code "<statusCode>: <responseMessage>"}, followed by {@code ". "} and
     * at most {@link HttpUtil#MAX_ERROR_BODY_IN_MESSAGE} characters of the body (longer bodies end with
     * {@code "... (truncated)"}). A {@code null} or empty status message - the case for a status line
     * without a reason phrase, as reported by {@code HttpURLConnection} and by HTTP/2 clients - is left
     * out entirely, so the message starts with just the code (for example {@code "503"} or
     * {@code "503. body"}).</p>
     *
     * @param requestUrl the URL the failing request was sent to; may be {@code null}
     * @param statusCode the HTTP status code of the response
     * @param responseMessage the HTTP status message of the response; may be {@code null} or empty, in
     *        which case it is omitted from the message
     * @param headers the response headers, copied along with their value lists; may be {@code null},
     *        in which case an empty map is used. Null keys, lists and list elements are preserved.
     * @param responseBody the captured (possibly truncated) error body; may be {@code null}, in
     *        which case an empty string is used
     */
    public HttpResponseException(final String requestUrl, final int statusCode, final String responseMessage, final Map<String, List<String>> headers,
            final String responseBody) {
        this(requestUrl, statusCode, responseMessage, headers, responseBody, new IOException(buildMessage(statusCode, responseMessage, responseBody)), null,
                null);
    }

    /**
     * Delegated-to constructor that reuses the already-built message for both this exception and its
     * cause, so the message is rendered only once.
     *
     * @param requestUrl the URL the failing request was sent to; may be {@code null}
     * @param statusCode the HTTP status code of the response
     * @param responseMessage the HTTP status message of the response; may be {@code null}
     * @param headers the response headers; may be {@code null}
     * @param responseBody the captured (possibly truncated) error body; may be {@code null}
     * @param cause the carrier of the rendered message
     * @param rawResponseBody the raw error bytes to copy and bound, or {@code null}
     * @param decodingFailure the original body decoding failure, or {@code null}
     */
    private HttpResponseException(final String requestUrl, final int statusCode, final String responseMessage, final Map<String, List<String>> headers,
            final String responseBody, final IOException cause, final byte[] rawResponseBody, final Throwable decodingFailure) {
        super(cause.getMessage(), cause);

        this.rawResponseBody = rawResponseBody == null ? null : Arrays.copyOf(rawResponseBody, Math.min(rawResponseBody.length, HttpUtil.MAX_ERROR_BODY_SIZE));
        responseBodyDecodingFailure = decodingFailure;
        this.requestUrl = requestUrl;
        this.statusCode = statusCode;
        this.responseMessage = responseMessage;
        if (headers == null || headers.isEmpty()) {
            this.headers = Collections.emptyMap();
        } else {
            // Capture diagnostics independently of connection-owned maps and mutable caller lists.
            // JDK wrappers also preserve the snapshot when this exception is serialized.
            final Map<String, List<String>> snapshot = new LinkedHashMap<>();
            for (final Map.Entry<String, List<String>> entry : headers.entrySet()) {
                snapshot.put(entry.getKey(), entry.getValue() == null ? null : Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
            }
            this.headers = Collections.unmodifiableMap(snapshot);
        }
        this.responseBody = responseBody == null ? Strings.EMPTY : responseBody;
    }

    /**
     * Builds the exception message, rendering at most {@link HttpUtil#MAX_ERROR_BODY_IN_MESSAGE}
     * characters of the error body so that the message stays readable in a log line. The status
     * message is appended only when it is non-empty.
     *
     * @param statusCode the HTTP status code
     * @param responseMessage the HTTP status message; may be {@code null} or empty
     * @param responseBody the captured error body; may be {@code null} or empty
     * @return the message for this exception
     */
    private static String buildMessage(final int statusCode, final String responseMessage, final String responseBody) {
        final StringBuilder sb = new StringBuilder(64).append(statusCode);

        // HttpURLConnection reports null and HTTP/2 clients "" when the status line has no reason phrase;
        // rendering those as "503: null" / "503: " only adds noise to every log line.
        if (Strings.isNotEmpty(responseMessage)) {
            sb.append(": ").append(responseMessage);
        }

        if (Strings.isNotEmpty(responseBody)) {
            sb.append(". ");

            if (responseBody.length() > HttpUtil.MAX_ERROR_BODY_IN_MESSAGE) {
                sb.append(responseBody, 0, HttpUtil.MAX_ERROR_BODY_IN_MESSAGE).append("... (truncated)");
            } else {
                sb.append(responseBody);
            }
        }

        return sb.toString();
    }

    /**
     * Returns the HTTP status code of the response.
     *
     * @return the HTTP status code, for example {@code 404}
     */
    public int statusCode() {
        return statusCode;
    }

    /**
     * Returns the HTTP status message of the response.
     *
     * @return the HTTP status message, for example {@code "Not Found"}; may be {@code null}
     */
    public String responseMessage() {
        return responseMessage;
    }

    /**
     * Returns the URL the failing request was sent to.
     *
     * @return the request URL, or {@code null} if it was not available
     */
    public String requestUrl() {
        return requestUrl;
    }

    /**
     * Returns the captured response headers, keyed as reported by the underlying connection.
     * The map and its value lists are unmodifiable snapshots and survive Java serialization.
     * Older serialized instances that did not retain headers return an empty map.
     *
     * @return an unmodifiable snapshot of the response headers; never {@code null}
     */
    public Map<String, List<String>> headers() {
        return headers == null ? Collections.emptyMap() : headers;
    }

    /**
     * Returns the first value of the named response header, matched case-insensitively. When the
     * snapshot holds several keys that differ only in case, the first one in encounter order wins.
     *
     * @param name the header name to look up; {@code null} matches nothing and yields {@code null}
     * @return the first value of the header, or {@code null} if the header is absent or has no value
     */
    public String header(final String name) {
        final List<String> values = findHeaderValues(name);

        return N.isEmpty(values) ? null : values.get(0);
    }

    /**
     * Returns all values of the named response header, matched case-insensitively. When the snapshot
     * holds several keys that differ only in case, the first one in encounter order wins.
     *
     * @param name the header name to look up; {@code null} matches nothing and yields an empty list
     * @return an unmodifiable snapshot of the header values, or an empty list if the header is absent
     *         or its value list is {@code null}
     */
    public List<String> headers(final String name) {
        final List<String> values = findHeaderValues(name);

        return values == null ? Collections.emptyList() : values;
    }

    private List<String> findHeaderValues(final String name) {
        if (name == null) {
            return null;
        }

        for (final Map.Entry<String, List<String>> entry : headers().entrySet()) {
            // A null key is the status line reported by HttpURLConnection.getHeaderFields().
            if (name.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Returns the captured error response body.
     *
     * <p>The body is truncated to {@link HttpUtil#MAX_ERROR_BODY_SIZE} bytes when it is read, so
     * this may be a prefix of the full response. Request {@link HttpResponse} as the result type to
     * read an error body in full.</p>
     *
     * @return the captured error body; never {@code null}, empty when there was none
     */
    public String responseBody() {
        return responseBody;
    }
}
