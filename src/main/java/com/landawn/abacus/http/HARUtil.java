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
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.Maps;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.cs;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.Stream;

/**
 * Utility class for working with HTTP Archive (HAR) files.
 * 
 * <p>HAR (HTTP Archive) is a JSON-formatted archive file format for logging of a web browser's 
 * interaction with a site. This utility provides methods to parse HAR files and replay the 
 * captured HTTP requests.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Parse HAR files and extract HTTP request information</li>
 *   <li>Filter requests by URL patterns</li>
 *   <li>Replay captured requests with the same headers and body</li>
 *   <li>Support for curl command generation from HAR entries</li>
 *   <li>Configurable HTTP header filtering</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Send a single request from HAR file
 * String response = HARUtil.sendRequestByHAR(new File("capture.har"), "http://localhost:18080/data");
 * 
 * // Send multiple requests matching a pattern
 * List<String> responses = HARUtil.sendMultiRequestsByHAR(
 *     new File("capture.har"), 
 *     url -> url.contains("/api/")
 * );
 * }</pre>
 *
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

    private static final ThreadLocal<Tuple3<Boolean, Character, Consumer<? super String>>> logRequestCurlForHARRequest_TL = ThreadLocal //NOSONAR
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Exclude authorization headers when replaying requests
     * HARUtil.setHttpHeaderFilterForHARRequest((name, value) -> 
     *     !name.equalsIgnoreCase("Authorization"));
     * }</pre>
     * 
     * @param httpHeaderFilterForHARRequest the filter to apply to headers, must not be null
     * @throws IllegalArgumentException if httpHeaderFilterForHARRequest is null
     */
    public static void setHttpHeaderFilterForHARRequest(final BiPredicate<? super String, String> httpHeaderFilterForHARRequest)
            throws IllegalArgumentException {
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
     */
    public static void resetHttpHeaderFilterForHARRequest() {
        httpHeaderFilterForHARRequest_TL.set(defaultHttpHeaderFilterForHARRequest);
    }

    /**
     * Enables or disables logging of curl commands for HAR requests.
     *
     * <p>When enabled, a curl command equivalent to each HAR request will be logged
     * using the default logger at INFO level. The curl commands use single quotes (')
     * for string quoting.</p>
     *
     * <p><b>Note:</b> This method sets the logging configuration for the current thread only,
     * as the curl logging settings are stored in thread-local storage.</p>
     *
     * @param logRequest {@code true} to enable curl logging, {@code false} to disable
     * @see #logRequestCurlForHARRequest(boolean, char)
     * @see #logRequestCurlForHARRequest(boolean, char, Consumer)
     */
    public static void logRequestCurlForHARRequest(final boolean logRequest) {
        logRequestCurlForHARRequest(logRequest, '\'');
    }

    /**
     * Enables or disables logging of curl commands for HAR requests with a custom quote character.
     *
     * <p>When enabled, a curl command equivalent to each HAR request will be logged
     * using the default logger at INFO level.</p>
     *
     * @param logRequest {@code true} to enable curl logging, {@code false} to disable
     * @param quoteChar the character to use for quoting in curl commands (typically ' or ")
     * @see #logRequestCurlForHARRequest(boolean)
     * @see #logRequestCurlForHARRequest(boolean, char, Consumer)
     */
    public static void logRequestCurlForHARRequest(final boolean logRequest, final char quoteChar) {
        logRequestCurlForHARRequest_TL.set(Tuple.of(logRequest, quoteChar, defaultCurlLogHandler));
    }

    /**
     * Enables or disables logging of curl commands for HAR requests with custom settings.
     *
     * <p>This method provides full control over curl command logging, including the
     * ability to specify a custom log handler for processing the generated curl commands.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Log curl commands to a file instead of standard logger
     * HARUtil.logRequestCurlForHARRequest(true, '"', curl -> {
     *     try {
     *         Files.write(Paths.get("curl-commands.txt"),
     *                 (curl + "\n").getBytes(),
     *                 StandardOpenOption.APPEND);
     *     } catch (IOException e) {
     *         throw new java.io.UncheckedIOException(e);
     *     }
     * });
     * }</pre>
     *
     * @param logRequest {@code true} to enable curl logging, {@code false} to disable
     * @param quoteChar the character to use for quoting in curl commands
     * @param logHandler the consumer that will handle the generated curl command strings
     * @see #logRequestCurlForHARRequest(boolean)
     * @see #logRequestCurlForHARRequest(boolean, char)
     */
    public static void logRequestCurlForHARRequest(final boolean logRequest, final char quoteChar, final Consumer<? super String> logHandler) {
        N.checkArgNotNull(logHandler, "logHandler");
        logRequestCurlForHARRequest_TL.set(Tuple.of(logRequest, quoteChar, logHandler));
    }

    /**
     * Sends an HTTP request extracted from a HAR file for the specified target URL.
     * 
     * <p>This method reads the HAR file, finds the first request entry matching the exact
     * target URL, and replays that request with all its original headers and body.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String response = HARUtil.sendRequestByHAR(
     *     new File("capture.har"), 
     *     "http://localhost:18080/users/123"
     * );
     * }</pre>
     * 
     * @param har the HAR file containing captured HTTP requests
     * @param targetUrl the exact URL to match in the HAR file
     * @return the response body as a string
     * @throws RuntimeException if no matching URL is found in the HAR file
     * @see <a href="http://www.softwareishard.com/har/viewer/">HAR Viewer</a>
     * @see <a href="https://confluence.atlassian.com/kb/generating-har-files-and-analyzing-web-requests-720420612.html">Generating HAR files</a>
     */
    public static String sendRequestByHAR(final File har, final String targetUrl) {
        return sendRequestByHAR(har, Fn.equal(targetUrl));
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
     * String response = HARUtil.sendRequestByHAR(
     *     new File("capture.har"), 
     *     url -> url.contains("/api/users")
     * );
     * }</pre>
     * 
     * @param har the HAR file containing captured HTTP requests
     * @param filterForTargetUrl predicate to test URLs; the first matching URL's request will be sent
     * @return the response body as a string
     * @throws RuntimeException if no matching URL is found in the HAR file
     * @see <a href="http://www.softwareishard.com/har/viewer/">HAR Viewer</a>
     * @see <a href="https://confluence.atlassian.com/kb/generating-har-files-and-analyzing-web-requests-720420612.html">Generating HAR files</a>
     */
    public static String sendRequestByHAR(final File har, final Predicate<? super String> filterForTargetUrl) {
        return sendRequestByHAR(IOUtil.readAllToString(har), filterForTargetUrl);
    }

    /**
     * Sends an HTTP request extracted from a HAR string for the specified target URL.
     * 
     * <p>This method parses the HAR JSON string, finds the first request entry matching
     * the exact target URL, and replays that request.</p>
     * 
     * @param har the HAR content as a JSON string
     * @param targetUrl the exact URL to match in the HAR content
     * @return the response body as a string
     * @throws RuntimeException if no matching URL is found in the HAR content
     * @see <a href="http://www.softwareishard.com/har/viewer/">HAR Viewer</a>
     * @see <a href="https://confluence.atlassian.com/kb/generating-har-files-and-analyzing-web-requests-720420612.html">Generating HAR files</a>
     */
    public static String sendRequestByHAR(final String har, final String targetUrl) {
        return sendRequestByHAR(har, Fn.equal(targetUrl));
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
     *   <li>Extract request entries from log.entries</li>
     *   <li>Find the first request matching the URL filter</li>
     *   <li>Replay the request with original HTTP method, headers, and body</li>
     * </ul>
     * 
     * @param har the HAR content as a JSON string
     * @param filterForTargetUrl predicate to test URLs; the first matching URL's request will be sent
     * @return the response body as a string
     * @throws RuntimeException if no matching URL is found in the HAR content
     * @see <a href="http://www.softwareishard.com/har/viewer/">HAR Viewer</a>
     * @see <a href="https://confluence.atlassian.com/kb/generating-har-files-and-analyzing-web-requests-720420612.html">Generating HAR files</a>
     */
    @SuppressWarnings("rawtypes")
    public static String sendRequestByHAR(final String har, final Predicate<? super String> filterForTargetUrl) {
        final Map<String, ?> map = N.fromJson(har, Map.class);
        final List<Map> entries = Maps.getByPath(map, "log.entries"); //NOSONAR

        return Stream.of(entries) //
                .map(m -> (Map<String, Object>) m.get("request")) //NOSONAR
                // .peek(m -> N.println(m.get("url")))
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
     * List<String> responses = HARUtil.sendMultiRequestsByHAR(
     *     new File("capture.har"), 
     *     url -> url.startsWith("http://localhost:18080/")
     * );
     * }</pre>
     * 
     * @param har the HAR file containing captured HTTP requests
     * @param filterForTargetUrl predicate to test URLs; all matching URLs' requests will be sent
     * @return a list of response bodies as strings, in the order they appear in the HAR file
     * @see <a href="http://www.softwareishard.com/har/viewer/">HAR Viewer</a>
     * @see <a href="https://confluence.atlassian.com/kb/generating-har-files-and-analyzing-web-requests-720420612.html">Generating HAR files</a>
     */
    public static List<String> sendMultiRequestsByHAR(final File har, final Predicate<? super String> filterForTargetUrl) {
        return sendMultiRequestsByHAR(IOUtil.readAllToString(har), filterForTargetUrl);
    }

    /**
     * Sends multiple HTTP requests extracted from a HAR string for URLs matching the given filter.
     * 
     * <p>This method parses the HAR JSON string and replays all requests whose URLs match
     * the provided filter predicate. Requests are sent in the order they appear in the HAR file.</p>
     * 
     * @param har the HAR content as a JSON string
     * @param filterForTargetUrl predicate to test URLs; all matching URLs' requests will be sent
     * @return a list of response bodies as strings, in the order they appear in the HAR content
     * @see <a href="http://www.softwareishard.com/har/viewer/">HAR Viewer</a>
     * @see <a href="https://confluence.atlassian.com/kb/generating-har-files-and-analyzing-web-requests-720420612.html">Generating HAR files</a>
     */
    @SuppressWarnings("rawtypes")
    public static List<String> sendMultiRequestsByHAR(final String har, final Predicate<? super String> filterForTargetUrl) {
        final Map<String, ?> map = N.fromJson(har, Map.class);
        final List<Map> entries = Maps.getByPath(map, "log.entries");

        return Stream.of(entries) //
                .map(m -> (Map<String, Object>) m.get("request"))
                // .peek(m -> N.println(m.get("url")))
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
     * HARUtil.streamMultiRequestsByHAR(harFile, url -> url.contains("/api/"))
     *     .forEach(tuple -> {
     *         Map<String, Object> request = tuple._1;
     *         com.landawn.abacus.http.HttpResponse response = tuple._2;
     *         System.out.println("URL: " + request.get("url"));
     *         System.out.println("Status: " + response.statusCode());
     *     });
     * }</pre>
     * 
     * @param har the HAR file containing captured HTTP requests
     * @param filterForTargetUrl predicate to test URLs; only matching URLs will be included in the stream
     * @return a stream of tuples where the first element is the request entry map and the second is the HttpResponse
     * @see <a href="http://www.softwareishard.com/har/viewer/">HAR Viewer</a>
     * @see <a href="https://confluence.atlassian.com/kb/generating-har-files-and-analyzing-web-requests-720420612.html">Generating HAR files</a>
     */
    public static Stream<Tuple2<Map<String, Object>, HttpResponse>> streamMultiRequestsByHAR(final File har,
            final Predicate<? super String> filterForTargetUrl) {
        return streamMultiRequestsByHAR(IOUtil.readAllToString(har), filterForTargetUrl);
    }

    /**
     * Creates a stream of HTTP requests and their responses from a HAR string.
     * 
     * <p>This method provides a streaming interface for processing HAR entries. The stream
     * is lazy - requests are only sent when the stream is consumed. This allows for
     * efficient processing of large HAR files.</p>
     * 
     * @param har the HAR content as a JSON string
     * @param filterForTargetUrl predicate to test URLs; only matching URLs will be included in the stream
     * @return a stream of tuples where the first element is the request entry map and the second is the HttpResponse
     * @see <a href="http://www.softwareishard.com/har/viewer/">HAR Viewer</a>
     * @see <a href="https://confluence.atlassian.com/kb/generating-har-files-and-analyzing-web-requests-720420612.html">Generating HAR files</a>
     */
    @SuppressWarnings("rawtypes")
    public static Stream<Tuple2<Map<String, Object>, HttpResponse>> streamMultiRequestsByHAR(final String har,
            final Predicate<? super String> filterForTargetUrl) {
        final Map<String, ?> map = N.fromJson(har, Map.class);
        final List<Map> entries = Maps.getByPath(map, "log.entries");

        return Stream.of(entries) //
                .map(m -> (Map<String, Object>) m.get("request"))
                // .peek(m -> N.println(m.get("url")))
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
     * @param <T> the type of the response
     * @param requestEntry the HAR request entry map containing request details
     * @param responseClass the class to deserialize the response into
     * @return the response deserialized into the specified type
     * @throws IllegalArgumentException if the HTTP method in the request entry is invalid
     * @throws RuntimeException if the HTTP request execution fails
     */
    public static <T> T sendRequestByRequestEntry(final Map<String, Object> requestEntry, final Class<T> responseClass) {
        final String url = getUrlByRequestEntry(requestEntry);
        final HttpMethod httpMethod = getHttpMethodByRequestEntry(requestEntry);

        final HttpHeaders httpHeaders = getHeadersByRequestEntry(requestEntry);

        final String requestBody = Maps.getByPath(requestEntry, "postData.text");
        final String bodyContentType = Maps.getByPath(requestEntry, "postData.mimeType");

        if (Strings.isNotEmpty(requestBody)) {
            WebUtil.setContentTypeByRequestBodyType(bodyContentType, httpHeaders);
        }

        final Tuple3<Boolean, Character, Consumer<? super String>> tp = logRequestCurlForHARRequest_TL.get();

        if (tp._1 && (tp._3 != defaultCurlLogHandler || logger.isInfoEnabled())) {
            tp._3.accept(WebUtil.buildCurl(httpMethod.name(), url, httpHeaders.toMap(), requestBody, bodyContentType, tp._2));
        }

        return HttpRequest.url(url).headers(httpHeaders).body(requestBody).execute(httpMethod, responseClass);
    }

    /**
     * Retrieves a request entry from a HAR file based on URL filtering.
     *
     * <p>This method searches through all entries in the HAR file and returns the first
     * request entry whose URL matches the provided filter predicate.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * com.landawn.abacus.util.u.Optional<Map<String, Object>> requestOpt = HARUtil.getRequestEntryByUrlFromHAR(
     *     new File("capture.har"),
     *     url -> url.contains("/api/users")
     * );
     *
     * requestOpt.ifPresent(request -> {
     *     String url = HARUtil.getUrlByRequestEntry(request);
     *     HttpMethod method = HARUtil.getHttpMethodByRequestEntry(request);
     *     com.landawn.abacus.http.HttpHeaders headers = HARUtil.getHeadersByRequestEntry(request);
     *     System.out.println("Found request: " + method + " " + url);
     * });
     * }</pre>
     *
     * @param har the HAR file containing captured HTTP requests
     * @param filterForTargetUrl predicate to test URLs
     * @return an Optional containing the first matching request entry map, or empty if no match is found
     */
    public static Optional<Map<String, Object>> getRequestEntryByUrlFromHAR(final File har, final Predicate<? super String> filterForTargetUrl) {
        return getRequestEntryByUrlFromHAR(IOUtil.readAllToString(har), filterForTargetUrl);
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
     * com.landawn.abacus.util.u.Optional<Map<String, Object>> requestOpt = HARUtil.getRequestEntryByUrlFromHAR(
     *     harContent, 
     *     url -> url.endsWith("/login")
     * );
     * 
     * requestOpt.ifPresent(request -> {
     *     String method = HARUtil.getHttpMethodByRequestEntry(request).name();
     *     com.landawn.abacus.http.HttpHeaders headers = HARUtil.getHeadersByRequestEntry(request);
     *     // Process request details...
     * });
     * }</pre>
     * 
     * @param har the HAR content as a JSON string
     * @param filterForTargetUrl predicate to test URLs
     * @return an Optional containing the first matching request entry map, or empty if no match is found
     */
    @SuppressWarnings("rawtypes")
    public static Optional<Map<String, Object>> getRequestEntryByUrlFromHAR(final String har, final Predicate<? super String> filterForTargetUrl) {
        final Map<String, ?> map = N.fromJson(har, Map.class);
        final List<Map> entries = Maps.getByPath(map, "log.entries");

        return Stream.of(entries) //
                .map(m -> (Map<String, Object>) m.get("request"))
                .filter(m -> filterForTargetUrl.test((String) m.get("url")))
                .first();
    }

    /**
     * Extracts the URL from a HAR request entry.
     * 
     * @param requestEntry the HAR request entry map
     * @return the URL string from the request entry
     */
    public static String getUrlByRequestEntry(final Map<String, Object> requestEntry) {
        return (String) requestEntry.get("url");
    }

    /**
     * Extracts the HTTP method from a HAR request entry.
     *
     * <p>The method name in the HAR entry is converted to uppercase and mapped
     * to the corresponding {@link HttpMethod} enum value.</p>
     *
     * @param requestEntry the HAR request entry map
     * @return the HTTP method enum value (GET, POST, PUT, DELETE, etc.)
     * @throws IllegalArgumentException if the method value from the request entry is not a valid HttpMethod
     */
    public static HttpMethod getHttpMethodByRequestEntry(final Map<String, Object> requestEntry) {
        return HttpMethod.valueOf(requestEntry.get("method").toString().toUpperCase());
    }

    /**
     * Extracts and filters HTTP headers from a HAR request entry.
     * 
     * <p>This method retrieves all headers from the request entry and applies the
     * configured header filter to determine which headers should be included.
     * Headers that don't pass the filter are excluded from the returned HttpHeaders object.</p>
     * 
     * <p>The header filter can be configured using {@link #setHttpHeaderFilterForHARRequest(BiPredicate)}.</p>
     * 
     * @param requestEntry the HAR request entry map containing a "headers" array
     * @return an HttpHeaders object containing the filtered headers
     */
    public static HttpHeaders getHeadersByRequestEntry(final Map<String, Object> requestEntry) {
        final BiPredicate<? super String, String> httpHeaderValidatorForHARRequest = httpHeaderFilterForHARRequest_TL.get();
        final HttpHeaders httpHeaders = HttpHeaders.create();
        final List<Map<String, String>> headers = (List<Map<String, String>>) requestEntry.get("headers");
        String headerName = null;
        String headerValue = null;

        if (N.isEmpty(headers)) {
            return httpHeaders;
        }

        for (final Map<String, String> m : headers) {
            headerName = m.get("name");
            headerValue = m.get("value");

            if (httpHeaderValidatorForHARRequest.test(headerName, headerValue)) {
                httpHeaders.set(headerName, headerValue);
            }
        }

        return httpHeaders;
    }

    /**
     * Extracts the request body and MIME type from a HAR request entry.
     * 
     * <p>This method retrieves the POST data from the request entry, including both
     * the text content and the MIME type. These values are typically found at
     * "postData.text" and "postData.mimeType" paths in the HAR structure.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Tuple2<String, String> bodyAndType = HARUtil.getBodyAndMimeTypeByRequestEntry(requestEntry);
     * String requestBody = bodyAndType._1;  // e.g., {"user":"john","pass":"secret"}
     * String mimeType = bodyAndType._2;  // e.g., "application/json"
     * }</pre>
     * 
     * @param requestEntry the HAR request entry map
     * @return a tuple where the first element is the request body text 
     *         and the second element is the MIME type
     */
    public static Tuple2<String, String> getBodyAndMimeTypeByRequestEntry(final Map<String, Object> requestEntry) {
        final String requestBody = Maps.getByPath(requestEntry, "postData.text");
        final String bodyContentType = Maps.getByPath(requestEntry, "postData.mimeType");

        return Tuple.of(requestBody, bodyContentType);
    }

    private HARUtil() {
        // Utility class.
    }
}
