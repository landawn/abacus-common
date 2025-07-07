/*
 * Copyright (c) 2022, Haiyang Li.
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

package com.landawn.abacus.http;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.landawn.abacus.util.EscapeUtil;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.ImmutableBiMap;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.cs;

/**
 * The WebUtil class is a utility class that provides methods for handling HTTP requests and responses.
 * It includes methods for creating HTTP requests, parsing cURL commands, and building cURL commands from HTTP requests.
 * This class is final and cannot be instantiated.
 */
public final class WebUtil {

    static final ImmutableBiMap<HttpMethod, String> httpMethodMap = N.enumMapOf(HttpMethod.class);

    /**
     * Converts a cURL command string into Java code for creating an HttpRequest.
     * 
     * <p>This method parses a cURL command and generates the equivalent Java code
     * using the HttpRequest API. It extracts the URL, HTTP method, headers, and
     * request body from the cURL command.</p>
     * 
     * <p>Supported cURL options:</p>
     * <ul>
     *   <li>-X, --request: HTTP method</li>
     *   <li>-H, --header: HTTP headers</li>
     *   <li>-d, --data, --data-raw: Request body</li>
     * </ul>
     * 
     * <p>Example input:</p>
     * <pre>{@code
     * curl -X POST https://api.example.com/users \
     *   -H "Content-Type: application/json" \
     *   -H "Authorization: Bearer token123" \
     *   -d '{"name":"John","age":30}'
     * }</pre>
     * 
     * <p>Example output:</p>
     * <pre>{@code
     * String requestBody = "{\"name\":\"John\",\"age\":30}";
     * 
     * HttpRequest.url("https://api.example.com/users")
     *     .header("Content-Type", "application/json")
     *     .header("Authorization", "Bearer token123")
     *     .post(requestBody);
     * }</pre>
     * 
     * @param curl the cURL command string to convert
     * @return Java code string for creating an equivalent HttpRequest
     * @throws IllegalArgumentException if the curl parameter is null, empty, or doesn't start with "curl"
     */
    public static String curl2HttpRequest(final String curl) {
        final String indent = "\n    ";
        final List<String> tokens = parseCurl(curl);

        String url = null;
        HttpMethod httpMethod = null;
        StringBuilder headers = new StringBuilder();
        String token = "";
        String body = "";
        for (int i = 0, size = tokens.size(); i < size; i++) {
            token = tokens.get(i);

            if (httpMethod == null && (token.equals("-X") || token.equals("--request")) && httpMethodMap.containsValue(tokens.get(i + 1).toUpperCase())) {
                httpMethod = HttpMethod.valueOf(tokens.get(++i).toUpperCase());
            } else if (Strings.isEmpty(url) && (Strings.startsWithIgnoreCase(token, "https://") || Strings.startsWithIgnoreCase(token, "http://"))) {
                url = token;
            } else if (Strings.equals(token, "--header") || Strings.equals(token, "-H")) {
                final String header = tokens.get(++i);
                final int idx = header.indexOf(':');
                headers.append(indent)
                        .append(".header(\"")
                        .append(header.substring(0, idx).trim())
                        .append("\", \"")
                        .append(escapeJava(header.substring(idx + 1).trim()))
                        .append("\")"); //NOSONAR
            } else if (Strings.equals(token, "--data-raw") || Strings.equals(token, "--data") || Strings.equals(token, "-d")) {
                body = tokens.get(++i);
            }
        }

        String requestBody = null;

        if (Strings.isNotEmpty(body)) {
            requestBody = "  String requestBody = \"" + escapeJava(body) + "\";";
        }

        final StringBuilder sb = new StringBuilder(IOUtil.LINE_SEPARATOR);

        if (Strings.isNotEmpty(requestBody)) {
            sb.append(requestBody).append(IOUtil.LINE_SEPARATOR).append(IOUtil.LINE_SEPARATOR);
        }

        sb.append("  HttpRequest.url(\"").append(url).append("\")");

        if (Strings.isNotEmpty(headers.toString())) {
            sb.append(headers);
        }

        if (httpMethod == null) {
            httpMethod = Strings.contains(curl, " -d ") ? HttpMethod.POST : (Strings.contains(curl, " -I ") ? HttpMethod.HEAD : HttpMethod.GET);
        }

        if (httpMethod == HttpMethod.GET) {
            sb.append(indent).append(".get();");
        } else if (httpMethod == HttpMethod.DELETE) {
            sb.append(indent).append(".delete();");
        } else {
            if (httpMethod == HttpMethod.POST) {
                sb.append(indent).append(".post(");

                if (Strings.isNotEmpty(body)) {
                    sb.append("requestBody");
                }
            } else if (httpMethod == HttpMethod.PUT) {
                sb.append(indent).append(".put(");

                if (Strings.isNotEmpty(body)) {
                    sb.append("requestBody");
                }
            } else {
                //noinspection DataFlowIssue
                sb.append(indent).append(".execute(HttpMethod.").append(httpMethod.name());

                if (Strings.isNotEmpty(body)) {
                    sb.append(", requestBody");
                }
            }
            sb.append(");");
        }

        return sb.toString();
    }

    /**
     * Converts a cURL command string into Java code for creating an OkHttpRequest.
     * 
     * <p>This method parses a cURL command and generates the equivalent Java code
     * using the OkHttpRequest API. It handles the same cURL options as
     * {@link #curl2HttpRequest(String)} but generates code for OkHttp client.</p>
     * 
     * <p>The generated code includes:</p>
     * <ul>
     *   <li>RequestBody creation with appropriate MediaType</li>
     *   <li>OkHttpRequest builder chain with headers and body</li>
     *   <li>Appropriate HTTP method call</li>
     * </ul>
     * 
     * <p>Example output:</p>
     * <pre>{@code
     * RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), "{\"name\":\"John\"}");
     * 
     * OkHttpRequest.url("https://api.example.com/users")
     *     .header("Content-Type", "application/json")
     *     .body(requestBody)
     *     .post();
     * }</pre>
     * 
     * @param curl the cURL command string to convert
     * @return Java code string for creating an equivalent OkHttpRequest
     * @throws IllegalArgumentException if the curl parameter is null, empty, or doesn't start with "curl"
     */
    public static String curl2OkHttpRequest(final String curl) {
        final String indent = "\n    ";
        final List<String> tokens = parseCurl(curl);

        String url = null;
        HttpMethod httpMethod = null;
        StringBuilder headers = new StringBuilder();
        String token = "";
        String body = "";
        String mediaType = null;
        for (int i = 0, size = tokens.size(); i < size; i++) {
            token = tokens.get(i);

            if (httpMethod == null && (token.equals("-X") || token.equals("--request")) && httpMethodMap.containsValue(tokens.get(i + 1).toUpperCase())) {
                httpMethod = HttpMethod.valueOf(tokens.get(++i).toUpperCase());
            } else if (Strings.isEmpty(url) && (Strings.startsWithIgnoreCase(token, "https://") || Strings.startsWithIgnoreCase(token, "http://"))) {
                url = token;
            } else if (Strings.equals(token, "--header") || Strings.equals(token, "-H")) {
                final String header = tokens.get(++i);
                final int idx = header.indexOf(':');
                headers.append(indent)
                        .append(".header(\"")
                        .append(header.substring(0, idx).trim())
                        .append("\", \"")
                        .append(escapeJava(header.substring(idx + 1).trim()))
                        .append("\")"); //NOSONAR

                if ("Content-Type".equalsIgnoreCase(header.substring(0, idx).trim())) {
                    mediaType = "MediaType.parse(\"" + header.substring(idx + 1).trim() + "\")";
                }
            } else if (Strings.equals(token, "--data-raw") || Strings.equals(token, "--data") || Strings.equals(token, "-d")) {
                body = tokens.get(++i);
            }
        }

        String requestBody = null;

        if (Strings.isNotEmpty(body)) {
            requestBody = "  RequestBody requestBody = RequestBody.create(" + mediaType + ", \"" + escapeJava(body) + "\");";
        }

        final StringBuilder sb = new StringBuilder(IOUtil.LINE_SEPARATOR);

        if (Strings.isNotEmpty(requestBody)) {
            sb.append(requestBody).append(IOUtil.LINE_SEPARATOR).append(IOUtil.LINE_SEPARATOR);
        }

        sb.append("  OkHttpRequest.url(\"").append(url).append("\")");

        if (Strings.isNotEmpty(headers.toString())) {
            sb.append(headers);
        }

        if (Strings.isNotEmpty(requestBody)) {
            sb.append(indent).append(".body(requestBody)");
        }

        if (httpMethod == null) {
            httpMethod = Strings.contains(curl, " -d ") ? HttpMethod.POST : (Strings.contains(curl, " -I ") ? HttpMethod.HEAD : HttpMethod.GET);
        }

        if (httpMethod == HttpMethod.GET) {
            sb.append(indent).append(".get();");
        } else if (httpMethod == HttpMethod.DELETE) {
            sb.append(indent).append(".delete();");
        } else if (httpMethod == HttpMethod.POST) {
            sb.append(indent).append(".post();");
        } else if (httpMethod == HttpMethod.PUT) {
            sb.append(indent).append(".put();");
        } else {
            //noinspection DataFlowIssue
            sb.append(indent).append(".execute(HttpMethod.").append(httpMethod.name()).append(");");
        }

        return sb.toString();
    }

    private static String escapeJava(final String str) {
        return EscapeUtil.escapeJava(str);
    }

    private static List<String> parseCurl(final String curl) {
        N.checkArgNotEmpty(curl, cs.curl);
        final String str = curl.trim();
        N.checkArgument(Strings.startsWithIgnoreCase(str, "curl"), "Input curl script doesn't start with 'curl'");

        final List<String> tokens = new ArrayList<>();
        final int len = str.length();

        int cursor = 0;
        char ch = 0;

        for (int i = 0; i < len; i++) {
            ch = str.charAt(i);

            if (ch == '\'' || ch == '"') {
                final char quoteChar = ch;

                if (cursor < i) {
                    tokens.add(str.substring(cursor, i));
                }

                cursor = ++i;

                do {
                    ch = str.charAt(i);

                    if (ch == '\\') {
                        i++;
                    } else if (ch == quoteChar) {
                        break;
                    }
                } while (++i < len);

                if (ch != quoteChar) {
                    throw new IllegalArgumentException("It's not quoted with ' from position: " + cursor + " to position: " + i);
                }

                tokens.add(str.substring(cursor, i));

                cursor = i + 1;
            } else if (Character.isWhitespace(ch) || ch == '\\') {
                if (cursor < i) {
                    tokens.add(str.substring(cursor, i));
                }

                cursor = i + 1;
            } else if (ch == '/' && i + 1 < len && str.charAt(i + 1) == '/' && str.charAt(i - 1) != ':') {
                if (cursor < i) {
                    tokens.add(str.substring(cursor, i));
                }

                while (++i < len) {
                    if (str.charAt(i) == '\n' || str.charAt(i) == '\r' || str.charAt(i) == '\\') {
                        break;
                    }
                }

                cursor = i + 1;
            }
        }

        if (cursor < len) {
            tokens.add(str.substring(cursor, len));
        }

        return tokens;
    }

    /**
     * Creates an OkHttpRequest configured to log cURL commands for each request.
     * 
     * <p>This method creates an OkHttpRequest with an interceptor that generates and logs
     * the equivalent cURL command for each HTTP request. This is useful for debugging
     * and sharing reproducible API calls.</p>
     * 
     * <p>The generated cURL commands use single quotes (') by default.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * OkHttpRequest request = WebUtil.createOkHttpRequestForCurl(
     *     "https://api.example.com",
     *     curl -> logger.debug("cURL command: {}", curl)
     * );
     * 
     * // When you make a request, it will log the cURL equivalent
     * request.header("Authorization", "Bearer token123")
     *        .jsonBody(requestData)
     *        .post();
     * }</pre>
     * 
     * @param url the base URL for the HTTP request
     * @param logHandler consumer that receives the generated cURL command string
     * @return an OkHttpRequest configured with cURL logging
     * @see <a href="https://github.com/mrmike/Ok2Curl">Ok2Curl - OkHttp to cURL converter</a>
     */
    public static OkHttpRequest createOkHttpRequestForCurl(final String url, final Consumer<? super String> logHandler) {
        return createOkHttpRequestForCurl(url, CurlInterceptor.DEFAULT_QUOTE_CHAR, logHandler);
    }

    /**
     * Creates an OkHttpRequest configured to log cURL commands with a custom quote character.
     * 
     * <p>This method creates an OkHttpRequest with an interceptor that generates and logs
     * the equivalent cURL command for each HTTP request. You can specify the quote character
     * to use in the generated cURL commands (typically ' or ").</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * // Use double quotes in generated cURL commands
     * OkHttpRequest request = WebUtil.createOkHttpRequestForCurl(
     *     "https://api.example.com",
     *     '"',
     *     System.out::println
     * );
     * }</pre>
     * 
     * @param url the base URL for the HTTP request
     * @param quoteChar the character to use for quoting in cURL commands (' or ")
     * @param logHandler consumer that receives the generated cURL command string
     * @return an OkHttpRequest configured with cURL logging
     */
    public static OkHttpRequest createOkHttpRequestForCurl(final String url, final char quoteChar, final Consumer<? super String> logHandler) {
        final okhttp3.OkHttpClient client = new okhttp3.OkHttpClient().newBuilder().addInterceptor(new CurlInterceptor(quoteChar, logHandler)).build();

        return OkHttpRequest.create(url, client);
    }

    /**
     * Builds a cURL command string from HTTP request parameters.
     * 
     * <p>This method constructs a complete cURL command that can be executed in a shell
     * to reproduce the HTTP request. It includes all headers, request body, and proper
     * escaping for shell execution.</p>
     * 
     * <p>The generated cURL command includes:</p>
     * <ul>
     *   <li>HTTP method (-X flag)</li>
     *   <li>URL (properly quoted)</li>
     *   <li>Headers (-H flags)</li>
     *   <li>Request body (-d flag) if present</li>
     *   <li>Content-Type header if not already present but body type is specified</li>
     * </ul>
     * 
     * <p>Example output:</p>
     * <pre>{@code
     * curl -X POST 'https://api.example.com/users' \
     *   -H 'Content-Type: application/json' \
     *   -H 'Authorization: Bearer token123' \
     *   -d '{"name":"John","email":"john@example.com"}'
     * }</pre>
     * 
     * @param httpMethod the HTTP method (GET, POST, PUT, etc.)
     * @param url the target URL
     * @param headers map of HTTP headers (can be null or empty)
     * @param body the request body string (can be null or empty)
     * @param bodyType the MIME type of the body (used if Content-Type header is not present)
     * @param quoteChar the character to use for quoting (' or ")
     * @return a formatted cURL command string
     */
    public static String buildCurl(final String httpMethod, final String url, final Map<String, ?> headers, final String body, final String bodyType,
            final char quoteChar) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            sb.append(IOUtil.LINE_SEPARATOR);
            sb.append("curl -X ").append(httpMethod).append(" ").append(quoteChar).append(url).append(quoteChar);

            if (N.notEmpty(headers)) {
                String headerValue = null;

                for (final Map.Entry<String, ?> e : headers.entrySet()) {
                    headerValue = HttpUtil.readHttpHeadValue(e.getValue());

                    sb.append(" -H ").append(quoteChar).append(e.getKey()).append(": ").append(Strings.quoteEscaped(headerValue, quoteChar)).append(quoteChar);
                }
            }

            if (Strings.isNotEmpty(body)) {
                final String contentType = N.isEmpty(headers) ? null : HttpUtil.readHttpHeadValue(headers.get(HttpHeaders.Names.CONTENT_TYPE));

                if (Strings.isEmpty(contentType) && Strings.isNotEmpty(bodyType)) {
                    sb.append(" -H ")
                            .append(quoteChar)
                            .append(HttpHeaders.Names.CONTENT_TYPE)
                            .append(": ")
                            .append(Strings.quoteEscaped(bodyType, quoteChar))
                            .append(quoteChar);
                }

                sb.append(" -d ").append(quoteChar).append(Strings.quoteEscaped(body, quoteChar)).append(quoteChar);
            }

            sb.append(IOUtil.LINE_SEPARATOR);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Sets the Content-Type header based on the request body type if not already present.
     * 
     * <p>This utility method checks if the Content-Type header is already set in the
     * provided HttpHeaders. If not present and a bodyType is specified, it sets the
     * Content-Type header to the bodyType value.</p>
     * 
     * <p>This is commonly used when processing HAR files or other scenarios where the
     * content type needs to be inferred from the request body type.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * HttpHeaders headers = HttpHeaders.create();
     * WebUtil.setContentTypeByRequestBodyType("application/json", headers);
     * // headers now contains: Content-Type: application/json
     * }</pre>
     * 
     * @param requestBodyType the MIME type of the request body (e.g., "application/json")
     * @param httpHeaders the HttpHeaders object to update
     */
    public static void setContentTypeByRequestBodyType(final String requestBodyType, final HttpHeaders httpHeaders) {
        if (Strings.isNotEmpty(requestBodyType) && httpHeaders.get(HttpHeaders.Names.CONTENT_TYPE) == null) {
            httpHeaders.setContentType(requestBodyType);
        }
    }

    private WebUtil() {
        // Utility class.
    }
}