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
 * Utility class providing methods for handling HTTP requests, responses, and cURL command conversions.
 *
 * <p>This class provides comprehensive functionality for:</p>
 * <ul>
 *   <li>Converting cURL commands to Java HTTP client code (HttpRequest and OkHttpRequest)</li>
 *   <li>Building cURL commands from HTTP request parameters</li>
 *   <li>Creating OkHttpRequest instances with cURL logging capabilities</li>
 *   <li>Managing Content-Type headers based on request body types</li>
 * </ul>
 *
 * <p>All methods in this class are static, and the class cannot be instantiated.</p>
 *
 * @see HttpRequest
 * @see OkHttpRequest
 * @see CurlInterceptor
 */
public final class WebUtil {

    static final ImmutableBiMap<HttpMethod, String> httpMethodMap = N.enumMapOf(HttpMethod.class);

    /**
     * Converts a cURL command string into Java code for creating an HttpRequest.
     *
     * <p>This method parses a cURL command and generates the equivalent Java code
     * using the HttpRequest API. It extracts the URL, HTTP method, headers, and
     * request body from the cURL command and produces properly formatted, executable
     * Java code.</p>
     *
     * <p>Supported cURL options:</p>
     * <ul>
     *   <li>{@code -X, --request}: HTTP method (GET, POST, PUT, DELETE, etc.)</li>
     *   <li>{@code -H, --header}: HTTP headers (can be specified multiple times)</li>
     *   <li>{@code -d, --data, --data-raw}: Request body data</li>
     *   <li>{@code -I}: HEAD request (inferred method)</li>
     * </ul>
     *
     * <p>HTTP Method Detection:</p>
     * <ul>
     *   <li>If {@code -X} is specified, uses that method</li>
     *   <li>If {@code -d} is present without {@code -X}, defaults to POST</li>
     *   <li>If {@code -I} is present without {@code -X}, defaults to HEAD</li>
     *   <li>Otherwise defaults to GET</li>
     * </ul>
     *
     * <p>The generated code properly escapes special characters in strings using
     * Java escape sequences. Headers are extracted and added individually. If a
     * request body is present, it's declared as a separate {@code requestBody}
     * variable for readability.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * curl -X POST https://api.example.com/users \
     *   -H "Content-Type: application/json" \
     *   -H "Authorization: Bearer token123" \
     *   -d '{"name":"John","age":30}'
     * }</pre>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String requestBody = "{\"name\":\"John\",\"age\":30}";
     *
     * HttpRequest.url("https://api.example.com/users")
     *     .header("Content-Type", "application/json")
     *     .header("Authorization", "Bearer token123")
     *     .post(requestBody);
     * }</pre>
     *
     * @param curl the cURL command string to convert, must not be null or empty
     *             and must start with "curl" (case-insensitive)
     * @return Java code string for creating an equivalent HttpRequest, formatted
     *         with proper indentation and line separators
     * @throws IllegalArgumentException if the curl parameter is null, empty, or
     *                                  doesn't start with "curl"
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

            if (httpMethod == null && (token.equals("-X") || token.equals("--request")) && i + 1 < size
                    && httpMethodMap.containsValue(tokens.get(i + 1).toUpperCase())) {
                httpMethod = HttpMethod.valueOf(tokens.get(++i).toUpperCase());
            } else if (Strings.isEmpty(url) && (Strings.startsWithIgnoreCase(token, "https://") || Strings.startsWithIgnoreCase(token, "http://"))) {
                url = token;
            } else if ((Strings.equals(token, "--header") || Strings.equals(token, "-H")) && i + 1 < size) {
                final String header = tokens.get(++i);
                final int idx = header.indexOf(':');
                if (idx > 0) {
                    headers.append(indent)
                            .append(".header(\"")
                            .append(header.substring(0, idx).trim())
                            .append("\", \"")
                            .append(escapeJava(header.substring(idx + 1).trim()))
                            .append("\")"); //NOSONAR
                }
            } else if ((Strings.equals(token, "--data-raw") || Strings.equals(token, "--data") || Strings.equals(token, "-d")) && i + 1 < size) {
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
            httpMethod = Strings.contains(curl, " -d ") || Strings.contains(curl, " --data ") || Strings.contains(curl, " --data-raw ") ? HttpMethod.POST
                    : (Strings.contains(curl, " -I ") ? HttpMethod.HEAD : HttpMethod.GET);
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
     * {@link #curl2HttpRequest(String)} but generates code specifically for the
     * OkHttp client library.</p>
     *
     * <p>Key differences from {@link #curl2HttpRequest(String)}:</p>
     * <ul>
     *   <li>Creates {@code RequestBody} with {@code MediaType} when body is present</li>
     *   <li>Automatically extracts {@code MediaType} from Content-Type header</li>
     *   <li>Attaches body using {@code .body(requestBody)} instead of passing to method</li>
     *   <li>Uses OkHttp-specific method calls ({@code .post()}, {@code .put()}, etc.)</li>
     * </ul>
     *
     * <p>The generated code includes:</p>
     * <ul>
     *   <li>{@code RequestBody} creation with appropriate {@code MediaType} extracted from headers</li>
     *   <li>OkHttpRequest builder chain with URL, headers, and body</li>
     *   <li>Appropriate HTTP method call ({@code get()}, {@code post()}, {@code put()}, {@code delete()}, or {@code execute()})</li>
     * </ul>
     *
     * <p>HTTP Method Detection follows the same rules as {@link #curl2HttpRequest(String)}:</p>
     * <ul>
     *   <li>If {@code -X} is specified, uses that method</li>
     *   <li>If {@code -d} is present without {@code -X}, defaults to POST</li>
     *   <li>If {@code -I} is present without {@code -X}, defaults to HEAD</li>
     *   <li>Otherwise defaults to GET</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * curl -X POST https://api.example.com/users \
     *   -H "Content-Type: application/json" \
     *   -d '{"name":"John"}'
     * }</pre>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), "{\"name\":\"John\"}");
     *
     * OkHttpRequest.url("https://api.example.com/users")
     *     .header("Content-Type", "application/json")
     *     .body(requestBody)
     *     .post();
     * }</pre>
     *
     * @param curl the cURL command string to convert, must not be null or empty
     *             and must start with "curl" (case-insensitive)
     * @return Java code string for creating an equivalent OkHttpRequest, formatted
     *         with proper indentation and line separators
     * @throws IllegalArgumentException if the curl parameter is null, empty, or
     *                                  doesn't start with "curl"
     * @see #curl2HttpRequest(String)
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

            if (httpMethod == null && (token.equals("-X") || token.equals("--request")) && i + 1 < size
                    && httpMethodMap.containsValue(tokens.get(i + 1).toUpperCase())) {
                httpMethod = HttpMethod.valueOf(tokens.get(++i).toUpperCase());
            } else if (Strings.isEmpty(url) && (Strings.startsWithIgnoreCase(token, "https://") || Strings.startsWithIgnoreCase(token, "http://"))) {
                url = token;
            } else if ((Strings.equals(token, "--header") || Strings.equals(token, "-H")) && i + 1 < size) {
                final String header = tokens.get(++i);
                final int idx = header.indexOf(':');
                if (idx > 0) {
                    headers.append(indent)
                            .append(".header(\"")
                            .append(header.substring(0, idx).trim())
                            .append("\", \"")
                            .append(escapeJava(header.substring(idx + 1).trim()))
                            .append("\")"); //NOSONAR

                    if ("Content-Type".equalsIgnoreCase(header.substring(0, idx).trim())) {
                        mediaType = "MediaType.parse(\"" + header.substring(idx + 1).trim() + "\")";
                    }
                }
            } else if ((Strings.equals(token, "--data-raw") || Strings.equals(token, "--data") || Strings.equals(token, "-d")) && i + 1 < size) {
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
            httpMethod = Strings.contains(curl, " -d ") || Strings.contains(curl, " --data ") || Strings.contains(curl, " --data-raw ") ? HttpMethod.POST
                    : (Strings.contains(curl, " -I ") ? HttpMethod.HEAD : HttpMethod.GET);
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
                    if (i >= len) {
                        throw new IllegalArgumentException("Unclosed quote starting at position: " + (cursor - 1) + ". String ends at position: " + len);
                    }

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
            } else if (ch == '/' && i + 1 < len && str.charAt(i + 1) == '/' && (i == 0 || str.charAt(i - 1) != ':')) {
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
     * Creates an OkHttpRequest configured to log cURL commands for each HTTP request.
     *
     * <p>This method creates an OkHttpRequest with a {@link CurlInterceptor} that
     * automatically generates and logs the equivalent cURL command for each HTTP
     * request made through the returned request object. This is extremely useful for:</p>
     * <ul>
     *   <li>Debugging API calls by reproducing them in a shell</li>
     *   <li>Sharing reproducible API calls with team members or support</li>
     *   <li>Documenting API usage in development logs</li>
     *   <li>Testing API calls outside of the application</li>
     * </ul>
     *
     * <p>The generated cURL commands use single quotes (') by default as the quote
     * character. To use a different quote character, see
     * {@link #createOkHttpRequestForCurl(String, char, Consumer)}.</p>
     *
     * <p>The interceptor is added to a new {@code OkHttpClient} instance specifically
     * created for this request, ensuring the logging doesn't affect other clients.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create request with cURL logging
     * OkHttpRequest request = WebUtil.createOkHttpRequestForCurl(
     *     "https://api.example.com",
     *     curl -> logger.debug("cURL command: {}", curl)
     * );
     *
     * // When you make a request, it will log the cURL equivalent
     * request.header("Authorization", "Bearer token123")
     *        .header("Content-Type", "application/json")
     *        .jsonBody(requestData)
     *        .post();
     *
     * // The logHandler will receive something like:
     * // curl -X POST 'https://api.example.com' \
     * //   -H 'Authorization: Bearer token123' \
     * //   -H 'Content-Type: application/json' \
     * //   -d '{"key":"value"}'
     * }</pre>
     *
     * @param url the base URL for the HTTP request, must not be null
     * @param logHandler consumer that receives the generated cURL command string
     *                   for each request, must not be null
     * @return an OkHttpRequest configured with cURL logging interceptor
     * @see #createOkHttpRequestForCurl(String, char, Consumer)
     * @see CurlInterceptor
     * @see <a href="https://github.com/mrmike/Ok2Curl">Ok2Curl - OkHttp to cURL converter</a>
     */
    public static OkHttpRequest createOkHttpRequestForCurl(final String url, final Consumer<? super String> logHandler) {
        return createOkHttpRequestForCurl(url, CurlInterceptor.DEFAULT_QUOTE_CHAR, logHandler);
    }

    /**
     * Creates an OkHttpRequest configured to log cURL commands with a custom quote character.
     *
     * <p>This method is similar to {@link #createOkHttpRequestForCurl(String, Consumer)}
     * but allows you to specify the quote character used in the generated cURL commands.
     * This is useful when you need to generate cURL commands compatible with specific
     * shell environments or documentation standards.</p>
     *
     * <p>Common quote character choices:</p>
     * <ul>
     *   <li>Single quote ('): Recommended for most Unix/Linux shells, prevents variable expansion</li>
     *   <li>Double quote ("): May be needed for Windows CMD or when variable expansion is desired</li>
     * </ul>
     *
     * <p>The generated cURL commands will use the specified quote character for:</p>
     * <ul>
     *   <li>URL</li>
     *   <li>Header values</li>
     *   <li>Request body data</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Use double quotes in generated cURL commands (e.g., for Windows compatibility)
     * OkHttpRequest request = WebUtil.createOkHttpRequestForCurl(
     *     "https://api.example.com",
     *     '"',
     *     curl -> System.out.println("cURL: " + curl)
     * );
     *
     * request.header("Content-Type", "application/json")
     *        .post();
     *
     * // Generates:
     * // curl -X POST "https://api.example.com" \
     * //   -H "Content-Type: application/json"
     * }</pre>
     *
     * @param url the base URL for the HTTP request, must not be null
     * @param quoteChar the character to use for quoting in cURL commands,
     *                  typically single quote (') or double quote (")
     * @param logHandler consumer that receives the generated cURL command string
     *                   for each request, must not be null
     * @return an OkHttpRequest configured with cURL logging interceptor using the
     *         specified quote character
     * @see #createOkHttpRequestForCurl(String, Consumer)
     * @see CurlInterceptor
     */
    public static OkHttpRequest createOkHttpRequestForCurl(final String url, final char quoteChar, final Consumer<? super String> logHandler) {
        final okhttp3.OkHttpClient client = new okhttp3.OkHttpClient().newBuilder().addInterceptor(new CurlInterceptor(quoteChar, logHandler)).build();

        return OkHttpRequest.create(url, client);
    }

    /**
     * Builds a cURL command string from HTTP request parameters.
     *
     * <p>This method constructs a complete, executable cURL command that can be run in a
     * shell to reproduce the HTTP request. It handles proper quoting and escaping of all
     * components to ensure the command works correctly when executed.</p>
     *
     * <p>The generated cURL command includes:</p>
     * <ul>
     *   <li>HTTP method using the {@code -X} flag</li>
     *   <li>URL enclosed in the specified quote character</li>
     *   <li>All headers using {@code -H} flags with proper quoting and escaping</li>
     *   <li>Request body using {@code -d} flag if body is not empty</li>
     *   <li>Content-Type header if body is present but Content-Type header is missing</li>
     * </ul>
     *
     * <p>Special handling:</p>
     * <ul>
     *   <li>If {@code body} is not empty but Content-Type header is not present in {@code headers},
     *       and {@code bodyType} is specified, the Content-Type header is automatically added</li>
     *   <li>Header values are read using {@link HttpUtil#readHttpHeadValue(Object)} to handle
     *       various value types (String, List, etc.)</li>
     *   <li>Quote characters within strings are properly escaped using
     *       {@link Strings#quoteEscaped(String, char)}</li>
     *   <li>The command is formatted with line separators at the beginning and end</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> headers = new HashMap<>();
     * headers.put("Content-Type", "application/json");
     * headers.put("Authorization", "Bearer token123");
     *
     * String curl = WebUtil.buildCurl(
     *     "POST",
     *     "https://api.example.com/users",
     *     headers,
     *     "{\"name\":\"John\",\"email\":\"john@example.com\"}",
     *     "application/json",
     *     '\''
     * );
     * }</pre>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * curl -X POST 'https://api.example.com/users' \
     *   -H 'Content-Type: application/json' \
     *   -H 'Authorization: Bearer token123' \
     *   -d '{"name":"John","email":"john@example.com"}'
     * }</pre>
     *
     * @param httpMethod the HTTP method (e.g., "GET", "POST", "PUT", "DELETE"), must not be null
     * @param url the target URL, must not be null
     * @param headers map of HTTP headers where values can be String or other types that
     *                {@link HttpUtil#readHttpHeadValue(Object)} can handle (can be null or empty)
     * @param body the request body string (can be null or empty for requests without a body)
     * @param bodyType the MIME type of the body (e.g., "application/json"), used to add
     *                 Content-Type header if not already present in headers (can be null)
     * @param quoteChar the character to use for quoting values in the cURL command,
     *                  typically single quote (') or double quote (")
     * @return a formatted cURL command string with line separators, ready for execution
     * @see HttpUtil#readHttpHeadValue(Object)
     * @see Strings#quoteEscaped(String, char)
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
     * <p>This utility method conditionally sets the Content-Type header in the provided
     * {@link HttpHeaders} object. The header is only set if:</p>
     * <ul>
     *   <li>The {@code requestBodyType} parameter is not null or empty</li>
     *   <li>The {@code httpHeaders} does not already have a Content-Type header</li>
     * </ul>
     *
     * <p>This method is particularly useful when:</p>
     * <ul>
     *   <li>Processing HAR (HTTP Archive) files where body type is specified separately</li>
     *   <li>Building HTTP requests programmatically where content type needs to be inferred</li>
     *   <li>Importing requests from external sources that specify MIME type separately</li>
     *   <li>Ensuring a default Content-Type without overriding existing values</li>
     * </ul>
     *
     * <p>If the Content-Type header is already present in {@code httpHeaders}, this method
     * does nothing, preserving the existing header value. This ensures that explicitly set
     * Content-Type headers are never overwritten.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Case 1: Setting Content-Type when not present
     * HttpHeaders headers = HttpHeaders.create();
     * WebUtil.setContentTypeByRequestBodyType("application/json", headers);
     * // headers now contains: Content-Type: application/json
     *
     * // Case 2: Preserving existing Content-Type
     * HttpHeaders headers2 = HttpHeaders.create();
     * headers2.setContentType("text/xml");
     * WebUtil.setContentTypeByRequestBodyType("application/json", headers2);
     * // headers2 still contains: Content-Type: text/xml (not changed)
     *
     * // Case 3: No-op when requestBodyType is empty
     * HttpHeaders headers3 = HttpHeaders.create();
     * WebUtil.setContentTypeByRequestBodyType("", headers3);
     * // headers3 has no Content-Type header (nothing was set)
     * }</pre>
     *
     * @param requestBodyType the MIME type of the request body (e.g., "application/json",
     *                        "text/xml", "application/x-www-form-urlencoded"), can be null or empty
     * @param httpHeaders the HttpHeaders object to conditionally update, must not be null
     * @see HttpHeaders#setContentType(String)
     * @see HttpHeaders#get(String)
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