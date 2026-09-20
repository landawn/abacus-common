/*
 * Copyright (C) 2024 HaiYang Li
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.Charsets;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.Maps;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.cs;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.URLEncodedUtil;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.Stream;

/**
 * Utility class for working with HTTP Archive (HAR) files.
 *
 * <p>HAR (HTTP Archive) is a JSON-formatted archive format for logging a web browser's
 * interaction with a site. This utility provides methods to parse HAR content and replay the
 * captured HTTP requests, optionally filtering them by URL.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Parse HAR files (or HAR JSON strings) and extract HTTP request information</li>
 *   <li>Filter requests by URL with a {@link Predicate}</li>
 *   <li>Replay captured requests with their original method, headers, and body</li>
 *   <li>Optional curl command generation/logging for replayed requests</li>
 *   <li>Configurable per-thread HTTP header filtering</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> This class is a stateless utility ({@code final} with a private
 * constructor). Configuration applied via {@link #setThreadLocalHeaderFilter(BiPredicate)} and the
 * {@code configureCurlLoggingForCurrentThread(...)} methods is stored in {@link ThreadLocal}
 * variables and only affects calls made on the same thread.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Send a single request from HAR file
 * String response = HARUtil.sendRequest(new File("capture.har"), "http://localhost:18080/data");
 *
 * // Send multiple requests matching a pattern
 * List<String> responses = HARUtil.sendRequests(
 *     new File("capture.har"),
 *     url -> url.contains("/api/")
 * );
 * }</pre>
 *
 * <p>Archive traversal validates all entries before applying URL filters or issuing requests. Missing optional
 * nodes and explicit null optional nodes are empty; other malformed nodes fail with an IllegalArgumentException naming their HAR path.</p>
 *
 * @see HttpRequest
 * @see HttpResponse
 * @see <a href="http://www.softwareishard.com/har/viewer/">HAR Viewer</a>
 * @see <a href="https://confluence.atlassian.com/kb/generating-har-files-and-analyzing-web-requests-720420612.html">Generating HAR files</a>
 */
public final class HARUtil {

    private static final Logger logger = LoggerFactory.getLogger(HARUtil.class);

    private static final BiPredicate<? super String, String> defaultHttpHeaderFilterForHARRequest = HttpUtil::isValidHttpHeader;

    private static final ThreadLocal<BiPredicate<? super String, String>> httpHeaderFilterForHARRequest_TL = ThreadLocal //NOSONAR
            .withInitial(() -> defaultHttpHeaderFilterForHARRequest);

    private static final Consumer<String> defaultCurlLogHandler = curl -> {
        if (logger.isInfoEnabled()) {
            logger.info(curl);
        }
    };

    private static final ThreadLocal<Tuple3<Boolean, Character, Consumer<? super String>>> logCurl_TL = ThreadLocal //NOSONAR
            .withInitial(() -> Tuple.of(false, '\'', defaultCurlLogHandler));

    /**
     * Sets a custom HTTP header filter for HAR request processing.
     *
     * <p>The filter is used to determine which headers from the HAR file should be included
     * when replaying requests. By default, all valid HTTP headers are included.</p>
     *
     * <p>The filter receives the header name and value as parameters and should return
     * {@code true} to include the header or {@code false} to exclude it.</p>
     *
     * <p><b>Note:</b> This setting is stored in thread-local state and only affects HAR request
     * replay on the current thread.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Exclude authorization headers when replaying requests
     * HARUtil.setThreadLocalHeaderFilter((name, value) ->
     *     !name.equalsIgnoreCase("Authorization"));
     * }</pre>
     *
     * @param httpHeaderFilterForHARRequest the filter to apply to headers; must not be {@code null}.
     *        Use {@link #resetThreadLocalHeaderFilter()} to restore the default filter.
     * @throws IllegalArgumentException if {@code httpHeaderFilterForHARRequest} is {@code null}.
     */
    public static void setThreadLocalHeaderFilter(final BiPredicate<? super String, String> httpHeaderFilterForHARRequest) throws IllegalArgumentException {
        N.checkArgNotNull(httpHeaderFilterForHARRequest, cs.httpHeaderFilterForHARRequest);

        httpHeaderFilterForHARRequest_TL.set(httpHeaderFilterForHARRequest);
    }

    /**
     * Resets the HTTP header filter to the default implementation.
     *
     * <p>The default filter accepts all valid HTTP headers as determined by
     * {@link HttpUtil#isValidHttpHeader(String, String)}.</p>
     *
     * <p><b>Note:</b> This method resets the filter for the current thread only,
     * as the header filter is stored in thread-local storage.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HARUtil.resetThreadLocalHeaderFilter();
     * }</pre>
     *
     * @see #setThreadLocalHeaderFilter(BiPredicate)
     */
    public static void resetThreadLocalHeaderFilter() {
        httpHeaderFilterForHARRequest_TL.set(defaultHttpHeaderFilterForHARRequest);
    }

    /**
     * Enables or disables logging of curl commands for HAR requests.
     *
     * <p>When enabled, a curl command equivalent to each HAR request will be logged
     * using the default logger at {@code INFO} level. The curl commands use single quotes ({@code '})
     * for string quoting.</p>
     *
     * <p><b>Note:</b> This method sets the logging configuration for the current thread only,
     * as the curl logging settings are stored in thread-local storage.</p>
     *
     * <p><b>&#9888;&#65039;</b> The logged curl command includes every request header (e.g. {@code Authorization},
     * {@code Cookie}, {@code X-Api-Key}) and the request body verbatim, with no masking. HAR files
     * captured from browsers routinely contain credentials and session tokens; avoid enabling this in
     * production or when the log destination is untrusted.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HARUtil.configureCurlLoggingForCurrentThread(true);
     * }</pre>
     *
     * @param logRequest {@code true} to enable curl logging, {@code false} to disable.
     * @see #configureCurlLoggingForCurrentThread(boolean, char)
     * @see #configureCurlLoggingForCurrentThread(boolean, char, Consumer)
     */
    public static void configureCurlLoggingForCurrentThread(final boolean logRequest) {
        configureCurlLoggingForCurrentThread(logRequest, '\'');
    }

    /**
     * Enables or disables logging of curl commands for HAR requests with a custom quote character.
     *
     * <p>When enabled, a curl command equivalent to each HAR request will be logged
     * using the default logger at {@code INFO} level.</p>
     *
     * <p><b>Note:</b> This method sets the logging configuration for the current thread only,
     * as the curl logging settings are stored in thread-local storage.</p>
     *
     * <p><b>&#9888;&#65039;</b> The logged curl command includes every request header (e.g. {@code Authorization},
     * {@code Cookie}, {@code X-Api-Key}) and the request body verbatim, with no masking. HAR files
     * captured from browsers routinely contain credentials and session tokens; avoid enabling this in
     * production or when the log destination is untrusted.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HARUtil.configureCurlLoggingForCurrentThread(true, '"');
     * }</pre>
     *
     * @param logRequest {@code true} to enable curl logging, {@code false} to disable.
     * @param quoteChar the character to use for quoting in curl commands (typically {@code '} or {@code "}).
     * @see #configureCurlLoggingForCurrentThread(boolean)
     * @see #configureCurlLoggingForCurrentThread(boolean, char, Consumer)
     */
    public static void configureCurlLoggingForCurrentThread(final boolean logRequest, final char quoteChar) {
        logCurl_TL.set(Tuple.of(logRequest, quoteChar, defaultCurlLogHandler));
    }

    /**
     * Enables or disables logging of curl commands for HAR requests with custom settings.
     *
     * <p>This method provides full control over curl command logging, including the
     * ability to specify a custom log handler for processing the generated curl commands.</p>
     *
     * <p><b>Note:</b> This method sets the logging configuration for the current thread only,
     * as the curl logging settings are stored in thread-local storage.</p>
     *
     * <p><b>&#9888;&#65039;</b> The generated curl command includes every request header (e.g. {@code Authorization},
     * {@code Cookie}, {@code X-Api-Key}) and the request body verbatim, with no masking. If your
     * {@code logHandler} persists or forwards these strings, ensure the destination is trusted.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Log curl commands to a file instead of standard logger
     * HARUtil.configureCurlLoggingForCurrentThread(true, '"', curl -> {
     *     try {
     *         Files.write(Paths.get("curl-commands.txt"),
     *                 (curl + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
     *                 StandardOpenOption.CREATE, StandardOpenOption.APPEND);
     *     } catch (IOException e) {
     *         throw new java.io.UncheckedIOException(e);
     *     }
     * });
     * }</pre>
     *
     * @param logRequest {@code true} to enable curl logging, {@code false} to disable.
     * @param quoteChar the character to use for quoting in curl commands.
     * @param logHandler the consumer that will handle the generated curl command strings; must not be {@code null}
     * @throws IllegalArgumentException if {@code logHandler} is {@code null}.
     * @see #configureCurlLoggingForCurrentThread(boolean)
     * @see #configureCurlLoggingForCurrentThread(boolean, char)
     */
    public static void configureCurlLoggingForCurrentThread(final boolean logRequest, final char quoteChar, final Consumer<? super String> logHandler)
            throws IllegalArgumentException {
        N.checkArgNotNull(logHandler, cs.logHandler);

        logCurl_TL.set(Tuple.of(logRequest, quoteChar, logHandler));
    }

    /**
     * Resets curl logging for the current thread back to the default (logging disabled, single-quote
     * style, default log handler).
     *
     * <p>Companion to the {@code configureCurlLoggingForCurrentThread(...)} overloads, mirroring how
     * {@link #resetThreadLocalHeaderFilter()} reverses {@link #setThreadLocalHeaderFilter(BiPredicate)}.
     * After this call the current thread no longer logs HAR-replay requests as cURL commands until it
     * is reconfigured.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HARUtil.configureCurlLoggingForCurrentThread(true);
     * try {
     *     String response = HARUtil.sendRequest(
     *         new File("capture.har"),
     *         "http://localhost:18080/data"
     *     );
     * } finally {
     *     HARUtil.resetCurlLoggingForCurrentThread();
     * }
     * }</pre>
     *
     * @see #configureCurlLoggingForCurrentThread(boolean)
     * @see #configureCurlLoggingForCurrentThread(boolean, char)
     * @see #configureCurlLoggingForCurrentThread(boolean, char, Consumer)
     */
    public static void resetCurlLoggingForCurrentThread() {
        logCurl_TL.remove();
    }

    /**
     * Sends an HTTP request extracted from a HAR file for the specified target URL.
     *
     * <p>This method reads the HAR file, finds the first request entry matching the exact
     * target URL, and replays that request with all its original headers and body.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String response = HARUtil.sendRequest(
     *     new File("capture.har"),
     *     "http://localhost:18080/users/123"
     * );
     * }</pre>
     *
     * @param har the HAR file containing captured HTTP requests.
     * @param targetUrl the exact URL to match in the HAR file.
     * @return the response body as a string.
     * @throws IllegalArgumentException if {@code har} is {@code null}, the HAR content contains no entries under {@code log.entries}
     *         , or the matching entry cannot be replayed
     *         (see {@link #sendRequestByRequestEntry(Map, Class)})
     * @throws UncheckedIOException if the HAR file cannot be read or an I/O error occurs while replaying a matching request
     * @throws ParsingException if the HAR content cannot be parsed as a JSON object
     * @throws UnsupportedOperationException if a matching entry's method is {@code PATCH} or {@code CONNECT} (see {@link HttpMethod#PATCH} and {@link HttpMethod#CONNECT})
     * @throws NoSuchElementException if no request entry matches {@code targetUrl}
     * @see <a href="http://www.softwareishard.com/har/viewer/">HAR Viewer</a>
     * @see <a href="https://confluence.atlassian.com/kb/generating-har-files-and-analyzing-web-requests-720420612.html">Generating HAR files</a>
     */
    public static String sendRequest(final File har, final String targetUrl)
            throws IllegalArgumentException, UncheckedIOException, ParsingException, UnsupportedOperationException, NoSuchElementException {
        return sendRequest(har, Fn.equal(targetUrl));
    }

    /**
     * Sends an HTTP request extracted from a HAR file for URLs matching the given filter.
     *
     * <p>This method reads the HAR file, finds the first request entry whose URL matches
     * the provided filter predicate, and replays that request.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Send request for first URL containing "/api/users"
     * String response = HARUtil.sendRequest(
     *     new File("capture.har"),
     *     url -> url.contains("/api/users")
     * );
     * }</pre>
     *
     * @param har the HAR file containing captured HTTP requests.
     * @param filterForTargetUrl predicate to test URLs; the first matching URL's request will be sent.
     * @return the response body as a string.
     * @throws IllegalArgumentException if {@code har} is {@code null}, {@code filterForTargetUrl} is {@code null}, the HAR content
     *         contains no entries under {@code log.entries}, or the matching entry cannot be replayed
     *         (see {@link #sendRequestByRequestEntry(Map, Class)})
     * @throws UncheckedIOException if the HAR file cannot be read or an I/O error occurs while replaying a matching request
     * @throws ParsingException if the HAR content cannot be parsed as a JSON object
     * @throws UnsupportedOperationException if a matching entry's method is {@code PATCH} or {@code CONNECT} (see {@link HttpMethod#PATCH} and {@link HttpMethod#CONNECT})
     * @throws NoSuchElementException if no request entry matches {@code filterForTargetUrl}
     * @see <a href="http://www.softwareishard.com/har/viewer/">HAR Viewer</a>
     * @see <a href="https://confluence.atlassian.com/kb/generating-har-files-and-analyzing-web-requests-720420612.html">Generating HAR files</a>
     */
    public static String sendRequest(final File har, final Predicate<? super String> filterForTargetUrl)
            throws IllegalArgumentException, UncheckedIOException, ParsingException, UnsupportedOperationException, NoSuchElementException {
        N.checkArgNotNull(har, cs.har);
        N.checkArgNotNull(filterForTargetUrl, cs.filterForTargetUrl);

        return sendRequest(IOUtil.readAllToString(har), filterForTargetUrl);
    }

    /**
     * Sends an HTTP request extracted from a HAR string for the specified target URL.
     *
     * <p>This method parses the HAR JSON string, finds the first request entry matching
     * the exact target URL, and replays that request.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String response = HARUtil.sendRequest(harContent, "http://localhost:18080/users/123");
     * }</pre>
     *
     * @param har the HAR content as a JSON string.
     * @param targetUrl the exact URL to match in the HAR content.
     * @return the response body as a string.
     * @throws IllegalArgumentException if {@code har} is {@code null}, the HAR content contains no entries under {@code log.entries}, or the matching entry cannot
     *         be replayed
     *         (see {@link #sendRequestByRequestEntry(Map, Class)})
     * @throws ParsingException if the HAR content cannot be parsed as a JSON object
     * @throws UnsupportedOperationException if a matching entry's method is {@code PATCH} or {@code CONNECT} (see {@link HttpMethod#PATCH} and {@link HttpMethod#CONNECT})
     * @throws UncheckedIOException if connecting to the selected HAR request URL, transmitting its body or reading its response fails
     * @throws NoSuchElementException if no request entry matches {@code targetUrl}
     * @see <a href="http://www.softwareishard.com/har/viewer/">HAR Viewer</a>
     * @see <a href="https://confluence.atlassian.com/kb/generating-har-files-and-analyzing-web-requests-720420612.html">Generating HAR files</a>
     */
    public static String sendRequest(final String har, final String targetUrl)
            throws IllegalArgumentException, ParsingException, UnsupportedOperationException, UncheckedIOException, NoSuchElementException {
        return sendRequest(har, Fn.equal(targetUrl));
    }

    /**
     * Sends an HTTP request extracted from a HAR string for URLs matching the given filter.
     *
     * <p>This method parses the HAR JSON string, finds the first request entry whose URL
     * matches the provided filter predicate, and replays that request with all its
     * original headers and body.</p>
     *
     * <p>The method will:</p>
     * <ul>
     *   <li>Parse the HAR JSON structure</li>
     *   <li>Extract request entries from {@code log.entries}</li>
     *   <li>Find the first request matching the URL filter</li>
     *   <li>Replay the request with original HTTP method, headers, and body</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String response = HARUtil.sendRequest(harContent, url -> url.contains("/api/users"));
     * }</pre>
     *
     * @param har the HAR content as a JSON string.
     * @param filterForTargetUrl predicate to test URLs; the first matching URL's request will be sent.
     * @return the response body as a string.
     * @throws IllegalArgumentException if {@code har} is {@code null}, {@code filterForTargetUrl} is {@code null}, the HAR content contains no entries under
     *         {@code log.entries}, or the matching entry cannot be replayed
     *         (see {@link #sendRequestByRequestEntry(Map, Class)})
     * @throws ParsingException if the HAR content cannot be parsed as a JSON object
     * @throws UnsupportedOperationException if a matching entry's method is {@code PATCH} or {@code CONNECT} (see {@link HttpMethod#PATCH} and {@link HttpMethod#CONNECT})
     * @throws UncheckedIOException if connecting to the selected HAR request URL, transmitting its body or reading its response fails
     * @throws NoSuchElementException if no request entry matches {@code filterForTargetUrl}
     * @see <a href="http://www.softwareishard.com/har/viewer/">HAR Viewer</a>
     * @see <a href="https://confluence.atlassian.com/kb/generating-har-files-and-analyzing-web-requests-720420612.html">Generating HAR files</a>
     */
    public static String sendRequest(final String har, final Predicate<? super String> filterForTargetUrl)
            throws IllegalArgumentException, ParsingException, UnsupportedOperationException, UncheckedIOException, NoSuchElementException {
        N.checkArgNotNull(har, cs.har);
        N.checkArgNotNull(filterForTargetUrl, cs.filterForTargetUrl);

        final List<Map<String, Object>> entries = readRequestEntries(har);

        if (entries.isEmpty()) {
            throw new IllegalArgumentException("HAR content must contain at least one entry under log.entries");
        }

        return Stream.of(entries) //
                .filter(m -> filterForTargetUrl.test((String) m.get("url")))
                .map(requestEntry -> sendRequestByRequestEntry(requestEntry, String.class))
                .first()
                .orElseThrow();

    }

    /**
     * Sends multiple HTTP requests extracted from a HAR file for URLs matching the given filter.
     *
     * <p>This method reads the HAR file and replays all requests whose URLs match the
     * provided filter predicate. Each matching request is sent with its original
     * HTTP method, headers, and body.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Send all API requests from the HAR file
     * List<String> responses = HARUtil.sendRequests(
     *     new File("capture.har"),
     *     url -> url.startsWith("http://localhost:18080/")
     * );
     * }</pre>
     *
     * <p>Replay stops at the first failing entry: its exception propagates and the responses already
     * received for earlier entries are not returned.</p>
     *
     * @param har the HAR file containing captured HTTP requests.
     * @param filterForTargetUrl predicate to test URLs; all matching URLs' requests will be sent.
     * @return a list of response bodies as strings, in the order they appear in the HAR file;
     *         an empty list if the HAR has no entries or none of them match
     * @throws IllegalArgumentException if {@code har} is {@code null}, {@code filterForTargetUrl} is {@code null}, or a matching
     *         entry cannot be replayed
     *         (see {@link #sendRequestByRequestEntry(Map, Class)})
     * @throws UncheckedIOException if the HAR file cannot be read or an I/O error occurs while replaying a matching request
     * @throws ParsingException if the HAR content cannot be parsed as a JSON object
     * @throws UnsupportedOperationException if a matching entry's method is {@code PATCH} or {@code CONNECT} (see {@link HttpMethod#PATCH} and {@link HttpMethod#CONNECT})
     * @see <a href="http://www.softwareishard.com/har/viewer/">HAR Viewer</a>
     * @see <a href="https://confluence.atlassian.com/kb/generating-har-files-and-analyzing-web-requests-720420612.html">Generating HAR files</a>
     */
    public static List<String> sendRequests(final File har, final Predicate<? super String> filterForTargetUrl)
            throws IllegalArgumentException, UncheckedIOException, ParsingException, UnsupportedOperationException {
        N.checkArgNotNull(har, cs.har);
        N.checkArgNotNull(filterForTargetUrl, cs.filterForTargetUrl);

        return sendRequests(IOUtil.readAllToString(har), filterForTargetUrl);
    }

    /**
     * Sends multiple HTTP requests extracted from a HAR string for URLs matching the given filter.
     *
     * <p>This method parses the HAR JSON string and replays all requests whose URLs match
     * the provided filter predicate. Requests are sent in the order they appear in the HAR file.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> responses = HARUtil.sendRequests(harContent, url -> url.startsWith("http://localhost:18080/"));
     * }</pre>
     *
     * <p>Replay stops at the first failing entry: its exception propagates and the responses already
     * received for earlier entries are not returned.</p>
     *
     * @param har the HAR content as a JSON string.
     * @param filterForTargetUrl predicate to test URLs; all matching URLs' requests will be sent.
     * @return a list of response bodies as strings, in the order they appear in the HAR content;
     *         an empty list if the HAR has no entries under {@code log.entries} or none of them match
     * @throws IllegalArgumentException if {@code har} is {@code null}, {@code filterForTargetUrl} is {@code null}, or a matching entry cannot be replayed
     *         (see {@link #sendRequestByRequestEntry(Map, Class)})
     * @throws ParsingException if the HAR content cannot be parsed as a JSON object
     * @throws UnsupportedOperationException if a matching entry's method is {@code PATCH} or {@code CONNECT} (see {@link HttpMethod#PATCH} and {@link HttpMethod#CONNECT})
     * @throws UncheckedIOException if connecting to the selected HAR request URL, transmitting its body or reading its response fails
     * @see <a href="http://www.softwareishard.com/har/viewer/">HAR Viewer</a>
     * @see <a href="https://confluence.atlassian.com/kb/generating-har-files-and-analyzing-web-requests-720420612.html">Generating HAR files</a>
     */
    public static List<String> sendRequests(final String har, final Predicate<? super String> filterForTargetUrl)
            throws IllegalArgumentException, ParsingException, UnsupportedOperationException, UncheckedIOException {
        N.checkArgNotNull(har, cs.har);
        N.checkArgNotNull(filterForTargetUrl, cs.filterForTargetUrl);

        final List<Map<String, Object>> entries = readRequestEntries(har);

        if (entries.isEmpty()) {
            return N.emptyList();
        }

        return Stream.of(entries) //
                .filter(m -> filterForTargetUrl.test((String) m.get("url")))
                .map(requestEntry -> sendRequestByRequestEntry(requestEntry, String.class))
                .toList();

    }

    /**
     * Creates a stream of HTTP requests and their responses from a HAR file.
     *
     * <p>This method provides a streaming interface for processing HAR entries. Each element
     * in the stream is a tuple containing the request entry map and the corresponding
     * HTTP response after the request is sent.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HARUtil.streamRequests(harFile, url -> url.contains("/api/"))
     *     .forEach(tuple -> {
     *         Map<String, Object> request = tuple._1;
     *         com.landawn.abacus.http.HttpResponse response = tuple._2;
     *         System.out.println("URL: " + request.get("url"));
     *         System.out.println("Status: " + response.statusCode());
     *     });
     * }</pre>
     *
     * <p>Replay stops at the first failing entry: its exception propagates out of the stream operation
     * that pulled it, and no later entry is sent.</p>
     *
     * <p>During stream consumption, replay may throw {@link IllegalArgumentException} for an invalid request entry,
     * {@link UnsupportedOperationException} for a {@code PATCH} or {@code CONNECT} request, or {@link UncheckedIOException} for an I/O error.
     * These failures propagate from the consuming operation; see {@link #sendRequestByRequestEntry(Map, Class)}.</p>
     *
     * @param har the HAR file containing captured HTTP requests.
     * @param filterForTargetUrl predicate to test URLs; only matching URLs will be included in the stream.
     * @return a stream of tuples where the first element is the request entry map and the second is the
     *         {@code HttpResponse}; an empty stream if the HAR has no entries or none of them match
     * @throws IllegalArgumentException if {@code har} or {@code filterForTargetUrl} is {@code null}
     * @throws UncheckedIOException if the HAR file cannot be read
     * @throws ParsingException if the HAR content cannot be parsed as a JSON object
     * @see <a href="http://www.softwareishard.com/har/viewer/">HAR Viewer</a>
     * @see <a href="https://confluence.atlassian.com/kb/generating-har-files-and-analyzing-web-requests-720420612.html">Generating HAR files</a>
     */
    public static Stream<Tuple2<Map<String, Object>, HttpResponse>> streamRequests(final File har, final Predicate<? super String> filterForTargetUrl)
            throws IllegalArgumentException, UncheckedIOException, ParsingException {
        N.checkArgNotNull(har, cs.har);
        N.checkArgNotNull(filterForTargetUrl, cs.filterForTargetUrl);

        return streamRequests(IOUtil.readAllToString(har), filterForTargetUrl);
    }

    /**
     * Creates a stream of HTTP requests and their responses from a HAR string.
     *
     * <p>This method parses the HAR content eagerly to locate {@code log.entries}. Request replay
     * remains lazy: matching requests are only sent when the returned stream is consumed.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HARUtil.streamRequests(harContent, url -> url.contains("/api/"))
     *     .map(tp -> tp._2.statusCode())
     *     .forEach(System.out::println);
     * }</pre>
     *
     * <p>Replay stops at the first failing entry: its exception propagates out of the stream operation
     * that pulled it, and no later entry is sent.</p>
     *
     * <p>During stream consumption, replay may throw {@link IllegalArgumentException} for an invalid request entry,
     * {@link UnsupportedOperationException} for a {@code PATCH} or {@code CONNECT} request, or {@link UncheckedIOException} for an I/O error.
     * These failures propagate from the consuming operation; see {@link #sendRequestByRequestEntry(Map, Class)}.</p>
     *
     * @param har the HAR content as a JSON string.
     * @param filterForTargetUrl predicate to test URLs; only matching URLs will be included in the stream.
     * @return a stream of tuples where the first element is the request entry map and the second is the
     *         {@code HttpResponse}; an empty stream if the HAR has no entries under {@code log.entries}
     *         or none of them match
     * @throws IllegalArgumentException if {@code har} or {@code filterForTargetUrl} is {@code null}
     * @throws ParsingException if the HAR content cannot be parsed as a JSON object
     * @see <a href="http://www.softwareishard.com/har/viewer/">HAR Viewer</a>
     * @see <a href="https://confluence.atlassian.com/kb/generating-har-files-and-analyzing-web-requests-720420612.html">Generating HAR files</a>
     */
    public static Stream<Tuple2<Map<String, Object>, HttpResponse>> streamRequests(final String har, final Predicate<? super String> filterForTargetUrl)
            throws IllegalArgumentException, ParsingException {
        N.checkArgNotNull(har, cs.har);
        N.checkArgNotNull(filterForTargetUrl, cs.filterForTargetUrl);

        final List<Map<String, Object>> entries = readRequestEntries(har);

        if (entries.isEmpty()) {
            return Stream.empty();
        }

        return Stream.of(entries) //
                .filter(m -> filterForTargetUrl.test((String) m.get("url")))
                .map(requestEntry -> Tuple.of(requestEntry, sendRequestByRequestEntry(requestEntry, HttpResponse.class)));

    }

    /**
     * Sends an HTTP request based on a HAR request entry.
     *
     * <p>This method extracts all necessary information from a HAR request entry map
     * (URL, HTTP method, headers, body) and sends the corresponding HTTP request.
     * The response is deserialized into the specified response class.</p>
     *
     * <p>The method will:</p>
     * <ul>
     *   <li>Extract URL, HTTP method, headers from the request entry</li>
     *   <li>Apply the configured header filter to include/exclude headers</li>
     *   <li>Extract request body and MIME type if present</li>
     *   <li>Log curl command if logging is enabled</li>
     *   <li>Send the HTTP request and return the response</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> requestEntry = new HashMap<>();
     * requestEntry.put("url", "http://localhost:18080/users");
     * requestEntry.put("method", "GET");
     * HttpResponse response = HARUtil.sendRequestByRequestEntry(requestEntry, HttpResponse.class);
     * // returns the HttpResponse from replaying the HAR entry (when executed against a live server)
     * }</pre>
     *
     * <p>Among supported methods, the body is attached for {@code POST}, {@code PUT}, {@code DELETE}, and
     * {@code OPTIONS}. For {@code GET}, {@code HEAD}, and {@code TRACE}, an <i>empty</i>
     * {@code postData.text} is dropped silently (no body is sent and no {@code Content-Type} is derived from
     * {@code postData.mimeType}), whereas a non-empty body is rejected, because dropping it would
     * change the replayed request. {@code PATCH} and {@code CONNECT} cannot be replayed by the underlying
     * client. Body validation runs first: {@code PATCH} accepts a body before the unsupported-method failure,
     * while {@code CONNECT} with a non-empty body fails body validation.</p>
     *
     * @param <T> the type of the response.
     * @param requestEntry the HAR request entry map containing request details.
     * @param responseClass the class to deserialize the response into.
     * @return the response deserialized into the specified type.
     * @throws IllegalArgumentException if {@code requestEntry} is {@code null}, has no {@code method} field, or its value is not a
     *         recognized {@link HttpMethod}, or the entry carries a non-empty body ({@code postData.text} or
     *         {@code postData.params}) but its method is none of {@code POST}, {@code PUT}, {@code DELETE},
     *         {@code OPTIONS}, {@code PATCH}.
     * @throws ClassCastException if the entry's {@code url} field is present but is not a {@code String}
     * @throws UnsupportedOperationException if the entry's method is {@code PATCH} or {@code CONNECT}, which the underlying
     *         {@code java.net.HttpURLConnection} cannot issue (see {@link HttpMethod#PATCH} and {@link HttpMethod#CONNECT}).
     * @throws UncheckedIOException if the HTTP request execution fails with an I/O error.
     */
    public static <T> T sendRequestByRequestEntry(final Map<String, Object> requestEntry, final Class<T> responseClass)
            throws IllegalArgumentException, ClassCastException, UnsupportedOperationException, UncheckedIOException {
        N.checkArgNotNull(requestEntry, cs.requestEntry);

        final String url = getRequestUrl(requestEntry);
        final HttpMethod httpMethod = getHttpMethodByRequestEntry(requestEntry);

        final HttpHeaders httpHeaders = getHeadersByRequestEntry(requestEntry);

        final Tuple2<String, String> bodyAndMimeType = getBodyAndMimeTypeByRequestEntry(requestEntry);
        final String requestBody = bodyAndMimeType._1;
        final String bodyContentType = bodyAndMimeType._2;

        // The body-method set is HttpRequest.body(..)'s (DELETE included), not HttpClient's routing table.
        // PATCH is kept in it so that a PATCH entry reaches HttpRequest and fails with its documented
        // UnsupportedOperationException (which names the workarounds) rather than with a body complaint.
        final boolean bodyMethod = httpMethod == HttpMethod.POST || httpMethod == HttpMethod.PUT || httpMethod == HttpMethod.DELETE
                || httpMethod == HttpMethod.OPTIONS || httpMethod == HttpMethod.PATCH;

        if (requestBody != null && !bodyMethod && !requestBody.isEmpty()) {
            throw new IllegalArgumentException("HAR entry carries a request body but its method " + httpMethod + " cannot send one: " + url);
        }

        // An empty postData.text on a body-less method contributes nothing; body("") would only make
        // HttpRequest reject the replay, so neither the body nor its Content-Type is applied.
        final boolean attachBody = requestBody != null && bodyMethod;

        final String originalBody = N.stringOf(Maps.<Object> getByPath(requestEntry, "postData.text"));

        if (attachBody) {
            if (Strings.isEmpty(originalBody) && Strings.isNotEmpty(requestBody)) {
                // Synthesized params are UTF-8 URL-encoded, regardless of the captured body's headers.
                httpHeaders.setContentType(HttpHeaders.Values.APPLICATION_URL_ENCODED + "; charset=UTF-8");
            } else if (Strings.isNotEmpty(bodyContentType)) {
                WebUtil.setContentTypeByRequestBodyType(bodyContentType, httpHeaders);
            }
        }

        final Tuple3<Boolean, Character, Consumer<? super String>> tp = logCurl_TL.get();

        if (tp._1 && (tp._3 != defaultCurlLogHandler || logger.isInfoEnabled())) {
            tp._3.accept(WebUtil.buildCurl(httpMethod, url, httpHeaders.toMap(), attachBody ? requestBody : null, attachBody ? bodyContentType : null, tp._2));
        }

        final HttpRequest request = HttpRequest.url(url).setHeaders(httpHeaders);

        if (attachBody) {
            request.body(requestBody);
        }

        return request.execute(httpMethod, responseClass);
    }

    /**
     * Retrieves a request entry from a HAR file based on URL filtering.
     *
     * <p>This method searches through all entries in the HAR file and returns the first
     * request entry whose URL matches the provided filter predicate.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * com.landawn.abacus.util.u.Optional<Map<String, Object>> requestOpt = HARUtil.findRequestEntry(
     *     new File("capture.har"),
     *     url -> url.contains("/api/users")
     * );
     *
     * requestOpt.ifPresent(request -> {
     *     String url = HARUtil.getRequestUrl(request);
     *     HttpMethod method = HARUtil.getHttpMethodByRequestEntry(request);
     *     com.landawn.abacus.http.HttpHeaders headers = HARUtil.getHeadersByRequestEntry(request);
     *     System.out.println("Found request: " + method + " " + url);
     * });
     * }</pre>
     *
     * @param har the HAR file containing captured HTTP requests.
     * @param filterForTargetUrl predicate to test URLs.
     * @return an {@code Optional} containing the first matching request entry map, or empty if no match is found.
     * @throws IllegalArgumentException if {@code har} or {@code filterForTargetUrl} is {@code null}.
     * @throws UncheckedIOException if the HAR file cannot be read.
     * @throws ParsingException if the HAR content cannot be parsed as a JSON object
     */
    public static Optional<Map<String, Object>> findRequestEntry(final File har, final Predicate<? super String> filterForTargetUrl)
            throws IllegalArgumentException, UncheckedIOException, ParsingException {
        N.checkArgNotNull(har, cs.har);
        N.checkArgNotNull(filterForTargetUrl, cs.filterForTargetUrl);

        return findRequestEntry(IOUtil.readAllToString(har), filterForTargetUrl);
    }

    /**
     * Retrieves a request entry from a HAR string based on URL filtering.
     *
     * <p>This method searches through all entries in the HAR content and returns the first
     * request entry whose URL matches the provided filter predicate. This is useful for
     * inspecting request details without sending the actual HTTP request.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * com.landawn.abacus.util.u.Optional<Map<String, Object>> requestOpt = HARUtil.findRequestEntry(
     *     harContent,
     *     url -> url.endsWith("/login")
     * );
     *
     * requestOpt.ifPresent(request -> {
     *     String method = HARUtil.getHttpMethodByRequestEntry(request).name();
     *     com.landawn.abacus.http.HttpHeaders headers = HARUtil.getHeadersByRequestEntry(request);
     *     System.out.println("Method: " + method);
     *     headers.forEach((name, value) -> System.out.println(name + ": " + value));
     * });
     * }</pre>
     *
     * @param har the HAR content as a JSON string.
     * @param filterForTargetUrl predicate to test URLs.
     * @return an {@code Optional} containing the first matching request entry map, or empty if no match is found.
     * @throws IllegalArgumentException if {@code har} or {@code filterForTargetUrl} is {@code null}.
     * @throws ParsingException if the HAR content cannot be parsed as a JSON object
     */
    public static Optional<Map<String, Object>> findRequestEntry(final String har, final Predicate<? super String> filterForTargetUrl)
            throws IllegalArgumentException, ParsingException {
        N.checkArgNotNull(har, cs.har);
        N.checkArgNotNull(filterForTargetUrl, cs.filterForTargetUrl);

        final List<Map<String, Object>> entries = readRequestEntries(har);

        if (entries.isEmpty()) {
            return Optional.empty();
        }

        return Stream.of(entries) //
                .filter(m -> filterForTargetUrl.test((String) m.get("url")))
                .first();
    }

    /**
     * Extracts the URL from a HAR request entry.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String url = HARUtil.getRequestUrl(requestEntry);
     * }</pre>
     *
     * @param requestEntry the HAR request entry map.
     * @return the URL string from the request entry, or {@code null} if the entry has no {@code url} field
     * @throws IllegalArgumentException if {@code requestEntry} is {@code null}
     * @throws ClassCastException if the {@code url} field is present but is not a {@code String}
     */
    public static String getRequestUrl(final Map<String, Object> requestEntry) throws IllegalArgumentException, ClassCastException {
        N.checkArgNotNull(requestEntry, cs.requestEntry);

        return (String) requestEntry.get("url");
    }

    /**
     * Extracts the HTTP method from a HAR request entry.
     *
     * <p>The method name in the HAR entry is converted to uppercase and mapped
     * to the corresponding {@link HttpMethod} enum value.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpMethod method = HARUtil.getHttpMethodByRequestEntry(requestEntry);
     * }</pre>
     *
     * @param requestEntry the HAR request entry map.
     * @return the HTTP method enum value ({@code GET}, {@code POST}, {@code PUT}, {@code DELETE}, etc.).
     * @throws IllegalArgumentException if {@code requestEntry} is {@code null}, has no {@code method} field, or its value is not a
     *         recognized {@link HttpMethod}.
     */
    public static HttpMethod getHttpMethodByRequestEntry(final Map<String, Object> requestEntry) throws IllegalArgumentException {
        N.checkArgNotNull(requestEntry, cs.requestEntry);

        final Object method = requestEntry.get("method");

        if (method == null) {
            throw new IllegalArgumentException("HAR request entry has no \"method\" field");
        }

        return HttpMethod.valueOf(method.toString().toUpperCase(java.util.Locale.ROOT));
    }

    /**
     * Extracts and filters HTTP headers from a HAR request entry.
     *
     * <p>This method retrieves all headers from the request entry and applies the
     * configured header filter to determine which headers should be included.
     * Headers that don't pass the filter are excluded from the returned {@code HttpHeaders} object.
     * A missing or null headers field produces no headers. Malformed non-null containers, non-object entries,
     * and missing names are rejected with their HAR path. Numbers and booleans in scalar text slots
     * retain the existing {@code N.stringOf(..)} conversion; structured values are rejected.</p>
     *
     * <p>Repeated names follow the combination/rejection policy documented by
     * {@link WebUtil#curlToHttpRequestCode(String)}: supported list fields preserve encounter order,
     * Cookie uses semicolons, and unsupported duplicate fields are rejected. Filtering is applied
     * before combination; a single null value is preserved whenever the filter accepts it - the default
     * filter {@link HttpUtil#isValidHttpHeader(String, String)} does accept a header with no value.</p>
     *
     * <p>The header filter can be configured using {@link #setThreadLocalHeaderFilter(BiPredicate)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpHeaders headers = HARUtil.getHeadersByRequestEntry(requestEntry);
     * String contentType = (String) headers.get(HttpHeaders.Names.CONTENT_TYPE);
     * }</pre>
     *
     * @param requestEntry the HAR request entry map containing a "headers" array.
     * @return a new {@code HttpHeaders} object containing the filtered headers; empty (never
     *         {@code null}) if the {@code headers} field is absent or null, or nothing passes the filter
     * @throws IllegalArgumentException if {@code requestEntry} is {@code null}, the headers container or entries are malformed,
     *         or included repeated headers cannot be combined without changing their semantics
     * @see #setThreadLocalHeaderFilter(BiPredicate)
     */
    public static HttpHeaders getHeadersByRequestEntry(final Map<String, Object> requestEntry) throws IllegalArgumentException {
        N.checkArgNotNull(requestEntry, cs.requestEntry);

        final BiPredicate<? super String, String> httpHeaderValidatorForHARRequest = httpHeaderFilterForHARRequest_TL.get();
        final HttpHeaders httpHeaders = HttpHeaders.wrap(new java.util.LinkedHashMap<>());
        // Validate every header before invoking the filter; a rejected header must not hide malformed input.
        final List<?> headers = validatedHeaders(requestEntry, "request");

        for (final Object e : headers) {
            final Map<?, ?> header = (Map<?, ?>) e;
            final String headerName = N.stringOf(header.get("name"));
            final String headerValue = N.stringOf(header.get("value"));

            if (httpHeaderValidatorForHARRequest.test(headerName, headerValue)) {
                WebUtil.addCapturedHeader(httpHeaders, headerName, headerValue);
            }
        }

        return httpHeaders;
    }

    /**
     * Extracts the request body and MIME type from a HAR request entry.
     *
     * <p>This method retrieves the POST data from the request entry, including both
     * the text content and the MIME type. If {@code postData.text} is absent or empty and HAR
     * {@code postData.params} are present, the params are encoded as
     * {@code application/x-www-form-urlencoded} form data. Because the synthesized bytes are URL-encoded,
     * the returned MIME type is also {@code application/x-www-form-urlencoded}, even if a different
     * (for example multipart) MIME type was recorded without its original body text. Replaying such
     * synthesized data replaces any captured Content-Type with this form type and UTF-8 charset;
     * replaying original text preserves an explicitly supplied Content-Type header. Malformed present nodes
     * are rejected with their HAR path. Scalar numbers and booleans retain the existing text conversion.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Tuple2<String, String> bodyAndType = HARUtil.getBodyAndMimeTypeByRequestEntry(requestEntry);
     * String requestBody = bodyAndType._1;  // {"user":"john","pass":"secret"}, for example
     * String mimeType = bodyAndType._2;     // "application/json", for example
     * }</pre>
     *
     * @param requestEntry the HAR request entry map. A {@code null} entry is treated as having no
     *         {@code postData} and yields a tuple of {@code null} body and {@code null} MIME type.
     * @return a tuple where the first element is the request body text (may be {@code null} if the
     *         request has no body) and the second element is the MIME type (may be {@code null} if
     *         no MIME type is present).
     */
    public static Tuple2<String, String> getBodyAndMimeTypeByRequestEntry(final Map<String, Object> requestEntry) {
        return bodyAndMimeType(requestEntry, "request");
    }

    private static Tuple2<String, String> bodyAndMimeType(final Map<String, Object> requestEntry, final String path) {
        if (requestEntry == null) {
            return Tuple.of(null, null);
        }
        final Map<?, ?> postData = optionalHarMap(requestEntry, "postData", path + ".postData");
        final String requestBody = harText(postData, "text", path + ".postData.text", false);
        final String mimeType = harText(postData, "mimeType", path + ".postData.mimeType", false);
        final List<?> params = validatedPostDataParams(postData, path + ".postData.params");

        // Validate params even when original text wins, but synthesize a body only when it will be used.
        if (Strings.isEmpty(requestBody) && !params.isEmpty()) {
            final String paramsBody = getRequestBodyFromPostDataParams(params);
            if (Strings.isNotEmpty(paramsBody)) {
                return Tuple.of(paramsBody, HttpHeaders.Values.APPLICATION_URL_ENCODED);
            }
        }
        return Tuple.of(requestBody, mimeType);
    }

    private static List<?> validatedPostDataParams(final Map<?, ?> postData, final String path) {
        final List<?> params = optionalHarList(postData, "params", path);
        for (int i = 0; i < params.size(); i++) {
            final Map<?, ?> param = harMap(params.get(i), path + "[" + i + "]");
            harScalar(param, "name", path + "[" + i + "].name", true);
            harScalar(param, "value", path + "[" + i + "].value", false);
        }
        return params;
    }

    private static String getRequestBodyFromPostDataParams(final List<?> params) {
        final List<Object> pairs = new ArrayList<>(params.size() * 2);
        for (final Object value : params) {
            final Map<?, ?> param = (Map<?, ?>) value;
            pairs.add(N.stringOf(param.get("name")));
            pairs.add(N.defaultIfNull(N.stringOf(param.get("value")), Strings.EMPTY));
        }
        return URLEncodedUtil.encode(pairs.toArray(), Charsets.UTF_8, NamingPolicy.NO_CHANGE);
    }

    // Validate the whole archive before returning any replayable entry, including entries after the first match.
    // Missing/null optional data is empty; malformed non-null data must not produce a partial replay.
    private static List<Map<String, Object>> readRequestEntries(final String har) {
        final Map<?, ?> root = harMap(N.fromJson(har, Object.class), "$");
        final Map<?, ?> log = optionalHarMap(root, "log", "log");
        final List<?> entries = optionalHarList(log, "entries", "log.entries");
        final List<Map<String, Object>> requests = new ArrayList<>(entries.size());

        for (int i = 0; i < entries.size(); i++) {
            final String path = "log.entries[" + i + "]";
            final Map<?, ?> entry = harMap(entries.get(i), path);
            final Map<String, Object> request = (Map<String, Object>) harMap(entry.get("request"), path + ".request");
            harString(request, "url", path + ".request.url", true);
            harString(request, "method", path + ".request.method", false);
            validatedHeaders(request, path + ".request");
            // Archive validation must not encode bodies for metadata lookups or entries that will be filtered out.
            final Map<?, ?> postData = optionalHarMap(request, "postData", path + ".request.postData");
            harScalar(postData, "text", path + ".request.postData.text", false);
            harScalar(postData, "mimeType", path + ".request.postData.mimeType", false);
            validatedPostDataParams(postData, path + ".request.postData.params");
            requests.add(request);
        }

        return requests;
    }

    private static List<?> validatedHeaders(final Map<?, ?> request, final String path) {
        final List<?> headers = optionalHarList(request, "headers", path + ".headers");
        for (int i = 0; i < headers.size(); i++) {
            final String headerPath = path + ".headers[" + i + "]";
            final Map<?, ?> header = harMap(headers.get(i), headerPath);
            harText(header, "name", headerPath + ".name", true);
            harText(header, "value", headerPath + ".value", false);
        }
        return headers;
    }

    private static Map<?, ?> optionalHarMap(final Map<?, ?> parent, final String key, final String path) {
        final Object value = parent.get(key);
        return value == null ? Map.of() : harMap(value, path);
    }

    private static Map<?, ?> harMap(final Object value, final String path) {
        if (!(value instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("Malformed HAR at " + path + ": expected an object");
        }
        return (Map<?, ?>) value;
    }

    private static List<?> optionalHarList(final Map<?, ?> parent, final String key, final String path) {
        final Object value = parent.get(key);
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?>)) {
            throw new IllegalArgumentException("Malformed HAR at " + path + ": expected an array");
        }
        return (List<?>) value;
    }

    private static String harText(final Map<?, ?> parent, final String key, final String path, final boolean required) {
        return N.stringOf(harScalar(parent, key, path, required));
    }

    private static Object harScalar(final Map<?, ?> parent, final String key, final String path, final boolean required) {
        final Object value = parent.get(key);
        if (value == null && !required) {
            return null;
        }
        if (!(value instanceof String || value instanceof Number || value instanceof Boolean)) {
            throw new IllegalArgumentException("Malformed HAR at " + path + ": expected a scalar text value");
        }
        return value;
    }

    private static String harString(final Map<?, ?> parent, final String key, final String path, final boolean required) {
        if (!required && parent.get(key) == null) {
            return null;
        }
        if (!(parent.get(key) instanceof String)) {
            throw new IllegalArgumentException("Malformed HAR at " + path + ": expected a string");
        }
        return (String) parent.get(key);
    }

    private HARUtil() {
        // Utility class.
    }
}
