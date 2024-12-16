/*
 * Copyright (c) 2022, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
     *
     * @param curl
     * @return
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
     *
     * @param curl
     * @return
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
     * <pre>
     * <code>
     * final OkHttpClient client = new OkHttpClient().newBuilder().addInterceptor(new com.moczul.ok2curl.CurlInterceptor(System.out::println)).build();
     * </code>
     * </pre>
     *
     * @param url
     * @param logHandler
     * @return
     * @see <a href="https://github.com/mrmike/Ok2Curl">https://github.com/mrmike/Ok2Curl</a>
     */
    public static OkHttpRequest createOkHttpRequestForCurl(final String url, final Consumer<? super String> logHandler) {
        return createOkHttpRequestForCurl(url, CurlInterceptor.DEFAULT_QUOTE_CHAR, logHandler);
    }

    /**
     *
     * @param url
     * @param quoteChar
     * @param logHandler
     * @return
     */
    public static OkHttpRequest createOkHttpRequestForCurl(final String url, final char quoteChar, final Consumer<? super String> logHandler) {
        final okhttp3.OkHttpClient client = new okhttp3.OkHttpClient().newBuilder().addInterceptor(new CurlInterceptor(quoteChar, logHandler)).build();

        return OkHttpRequest.create(url, client);
    }

    /**
     *
     * @param httpMethod
     * @param url
     * @param headers
     * @param body
     * @param bodyType
     * @param quoteChar
     * @return
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
                final String contentType = HttpUtil.readHttpHeadValue(headers.get(HttpHeaders.Names.CONTENT_TYPE));

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
     *
     * @param requestBodyType
     * @param httpHeaders
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
