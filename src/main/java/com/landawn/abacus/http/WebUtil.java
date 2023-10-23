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
import com.landawn.abacus.util.Strings.StringUtil;

public class WebUtil {

    static final ImmutableBiMap<HttpMethod, String> httpMethodMap = N.enumMapOf(HttpMethod.class);

    /**
     * 
     *
     * @param curl 
     * @return 
     */
    public static String curl2HttpRequest(String curl) {
        String indent = "\n    ";
        final List<String> tokens = parseCurl(curl);

        String url = null;
        HttpMethod httpMethod = null;
        String headers = "";
        String token = "";
        String body = "";
        for (int i = 0, size = tokens.size(); i < size; i++) {
            token = tokens.get(i);

            if (httpMethod == null && (token.equals("-X") || token.equals("--request")) && httpMethodMap.containsValue(tokens.get(i + 1).toUpperCase())) {
                httpMethod = HttpMethod.valueOf(tokens.get(++i).toUpperCase());
            } else if (Strings.isEmpty(url) && (StringUtil.startsWithIgnoreCase(token, "https://") || StringUtil.startsWithIgnoreCase(token, "http://"))) {
                url = token;
            } else if (StringUtil.equals(token, "--header") || StringUtil.equals(token, "-H")) {
                String header = tokens.get(++i);
                int idx = header.indexOf(':');
                headers = headers + indent + ".header(\"" + header.substring(0, idx).trim() + "\", \"" + escapeJava(header.substring(idx + 1).trim()) + "\")"; //NOSONAR
            } else if (StringUtil.equals(token, "--data-raw") || StringUtil.equals(token, "--data") || StringUtil.equals(token, "-d")) {
                body = tokens.get(++i);
            }
        }

        String requestBody = null;

        if (Strings.isNotEmpty(body)) {
            requestBody = "  String requestBody = \"" + escapeJava(body) + "\";";
        }

        StringBuilder sb = new StringBuilder(IOUtil.LINE_SEPARATOR);

        if (Strings.isNotEmpty(requestBody)) {
            sb.append(requestBody).append(IOUtil.LINE_SEPARATOR).append(IOUtil.LINE_SEPARATOR);
        }

        sb.append("  HttpRequest.url(\"" + url + "\")");

        if (Strings.isNotEmpty(headers)) {
            sb.append(headers);
        }

        if (httpMethod == HttpMethod.GET) {
            sb.append(indent).append(".get();");
        } else if (httpMethod == HttpMethod.DELETE) {
            sb.append(indent).append(".delete();");
        } else if (httpMethod == HttpMethod.POST) {
            sb.append(indent).append(".post(");

            if (StringUtil.isNotEmpty(body)) {
                sb.append("requestBody");
            }

            sb.append(");");
        } else if (httpMethod == HttpMethod.PUT) {
            sb.append(indent).append(".put(");

            if (StringUtil.isNotEmpty(body)) {
                sb.append("requestBody");
            }

            sb.append(");");
        } else {
            sb.append(indent).append(".execute(HttpMethod.").append(httpMethod.name());

            if (StringUtil.isNotEmpty(body)) {
                sb.append(", requestBody");
            }

            sb.append(");");
        }

        return sb.toString();
    }

    /**
     * 
     *
     * @param curl 
     * @return 
     */
    public static String curl2OkHttpRequest(String curl) {
        String indent = "\n    ";
        final List<String> tokens = parseCurl(curl);

        String url = null;
        HttpMethod httpMethod = null;
        String headers = "";
        String token = "";
        String body = "";
        String mediaType = null;
        for (int i = 0, size = tokens.size(); i < size; i++) {
            token = tokens.get(i);

            if (httpMethod == null && (token.equals("-X") || token.equals("--request")) && httpMethodMap.containsValue(tokens.get(i + 1).toUpperCase())) {
                httpMethod = HttpMethod.valueOf(tokens.get(++i).toUpperCase());
            } else if (Strings.isEmpty(url) && (StringUtil.startsWithIgnoreCase(token, "https://") || StringUtil.startsWithIgnoreCase(token, "http://"))) {
                url = token;
            } else if (StringUtil.equals(token, "--header") || StringUtil.equals(token, "-H")) {
                String header = tokens.get(++i);
                int idx = header.indexOf(':');
                headers = headers + indent + ".header(\"" + header.substring(0, idx).trim() + "\", \"" + escapeJava(header.substring(idx + 1).trim()) + "\")"; //NOSONAR

                if ("Content-Type".equalsIgnoreCase(header.substring(0, idx).trim())) {
                    mediaType = "MediaType.parse(\"" + header.substring(idx + 1).trim() + "\")";
                }
            } else if (StringUtil.equals(token, "--data-raw") || StringUtil.equals(token, "--data") || StringUtil.equals(token, "-d")) {
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

        sb.append("  OkHttpRequest.url(\"" + url + "\")");

        if (Strings.isNotEmpty(headers)) {
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
            sb.append(indent).append(".execute(HttpMethod.").append(httpMethod.name()).append(");");
        }

        return sb.toString();
    }

    private static String escapeJava(String str) {
        return EscapeUtil.escapeJava(str);
    }

    private static List<String> parseCurl(final String curl) {
        N.checkArgNotEmpty(curl, "curl");
        String str = curl.trim();
        N.checkArgument(StringUtil.startsWithIgnoreCase(str, "curl"), "Input curl script doesn't start with 'curl'");

        final List<String> tokens = new ArrayList<>();
        final int len = str.length();

        int cursor = 0;
        char ch = 0;

        for (int i = 0; i < len; i++) {
            ch = str.charAt(i);

            if (ch == '\'' || ch == '"') {
                char quoteChar = ch;

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
     * @see https://github.com/mrmike/Ok2Curl
     */
    public static OkHttpRequest createOkHttpRequestForCurl(final String url, Consumer<String> logHandler) {
        return createOkHttpRequestForCurl(url, CurlInterceptor.DEFAULT_QUOTE_CHAR, logHandler);
    }

    /**
     * 
     *
     * @param url 
     * @param quoteChar 
     * @param logHandler 
     * @return 
     */
    public static OkHttpRequest createOkHttpRequestForCurl(final String url, final char quoteChar, Consumer<String> logHandler) {
        final okhttp3.OkHttpClient client = new okhttp3.OkHttpClient().newBuilder().addInterceptor(new CurlInterceptor(quoteChar, logHandler)).build();

        return OkHttpRequest.create(url, client);
    }

    /**
     * 
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

                for (Map.Entry<String, ?> e : headers.entrySet()) {
                    headerValue = HttpUtil.readHttpHeadValue(e.getValue());

                    sb.append(" -H ")
                            .append(quoteChar)
                            .append(e.getKey())
                            .append(": ")
                            .append(StringUtil.quoteEscaped(headerValue, quoteChar))
                            .append(quoteChar);
                }
            }

            if (Strings.isNotEmpty(body)) {
                String contentType = HttpUtil.readHttpHeadValue(headers.get(HttpHeaders.Names.CONTENT_TYPE));

                if (Strings.isEmpty(contentType) && Strings.isNotEmpty(bodyType)) {
                    sb.append(" -H ")
                            .append(quoteChar)
                            .append(HttpHeaders.Names.CONTENT_TYPE)
                            .append(": ")
                            .append(StringUtil.quoteEscaped(bodyType, quoteChar))
                            .append(quoteChar);
                }

                sb.append(" -d ").append(quoteChar).append(StringUtil.quoteEscaped(body, quoteChar)).append(quoteChar);
            }

            sb.append(IOUtil.LINE_SEPARATOR);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * 
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
