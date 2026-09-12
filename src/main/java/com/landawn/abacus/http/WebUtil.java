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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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

    static final ImmutableBiMap<HttpMethod, String> httpMethodMap = N.enumNameMap(HttpMethod.class);

    // Request fields whose grammar is the comma-separated list of RFC 9110 5.6.1, so that combining repeated
    // field lines with ", " is exactly the field-line combination of RFC 9110 5.3 and preserves the semantics.
    // Hop-by-hop status is irrelevant here (TE is hop-by-hop and still safely comma-joinable); what matters is
    // that the field is a #list. Fields with an unknown grammar, a singleton value or a coding stack whose
    // order carries meaning (Content-Encoding, Transfer-Encoding, ..) are deliberately absent and rejected.
    private static final Set<String> COMBINABLE_CAPTURED_HEADERS = Set.of("accept", "accept-charset", "accept-encoding", "accept-language", "cache-control",
            "content-language", "if-match", "if-none-match", "pragma", "via", "forwarded", "x-forwarded-for", "te", "expect", "access-control-request-headers");

    /**
     * The semantic content of a parsed cURL command, shared by both code generators so that they
     * cannot drift apart.
     */
    private static final class CurlCommand {
        /** The request URL. Never {@code null} or empty. */
        private final String url;

        /** The resolved HTTP method. Never {@code null}. */
        private final HttpMethod httpMethod;

        /** Combined headers in first-encounter order. */
        private final HttpHeaders headers;

        /** The combined request body, or {@code null} when the command carried no data option. */
        private final String body;

        /** The value of the {@code Content-Type} header, or {@code null} when none was given. */
        private final String contentType;

        /**
         * The header names cURL was told to suppress with {@code -H 'Name:'}, in first-encounter order.
         * Names that the command also gives a value are not listed: cURL sends those. Never {@code null}.
         */
        private final List<String> removedHeaderNames;

        private CurlCommand(final String url, final HttpMethod httpMethod, final HttpHeaders headers, final String body, final String contentType,
                final List<String> removedHeaderNames) {
            this.url = url;
            this.httpMethod = httpMethod;
            this.headers = headers;
            this.body = body;
            this.contentType = contentType;
            this.removedHeaderNames = removedHeaderNames;
        }

        private boolean hasBody() {
            return body != null;
        }
    }

    /**
     * Options that carry request state this converter cannot faithfully reproduce. Silently ignoring
     * them would emit code that issues a materially different request than the cURL command, so they
     * are rejected instead.
     */
    private static final Set<String> UNSUPPORTED_OPTIONS = Set.of("--data-binary", "--data-urlencode", "--form", "-F", "--form-string", "--user", "-u",
            "--cookie", "-b", "--upload-file", "-T", "--json", "-G", "--get");

    /**
     * Options that take an argument but do not change the HTTP request that is issued (proxying,
     * output, TLS material, timing, retries, tracing). The option is ignored and its argument is
     * consumed with it, so that argument can never be mistaken for the request URL or the body.
     */
    private static final Set<String> IGNORED_OPTIONS_WITH_ARGUMENT = Set.of("-x", "--proxy", "-o", "--output", "-c", "--cookie-jar", "-D", "--dump-header",
            "--cacert", "--cert", "--key", "-m", "--max-time", "--connect-timeout", "--retry", "--retry-delay", "--retry-max-time", "-w", "--write-out",
            "--resolve", "--socks4", "--socks4a", "--socks5", "--socks5-hostname", "--interface", "-r", "--range", "--limit-rate", "-U", "--proxy-user",
            "--ciphers", "--proto", "--max-redirs", "-y", "--speed-time", "-Y", "--speed-limit", "--stderr", "--trace", "--trace-ascii", "-K", "--config");

    /**
     * cURL's argument-less short options (curl 8.x). They may be clustered ({@code -sSL}), and a
     * cluster may end in one argument-taking letter ({@code -sX POST}, {@code -sXPOST}). {@code G}
     * is deliberately absent so that {@code -sG} lands on the exact {@code -G} rejection.
     */
    private static final String BOOLEAN_SHORT_OPTIONS = "aBfghiIjJklLMnNOpqRsSvVZ0123456#:";

    /**
     * Parses a cURL command into the pieces both code generators need.
     *
     * @param curl the cURL command string
     * @return the parsed command
     * @throws IllegalArgumentException if the command is malformed, carries no URL, or uses an
     *         option that cannot be reproduced.
     */
    private static CurlCommand parseCurlCommand(final String curl) throws IllegalArgumentException {
        final List<String> tokens = parseCurl(curl);

        String url = null;
        HttpMethod httpMethod = null;
        String contentType = null;
        final HttpHeaders headers = HttpHeaders.wrap(new LinkedHashMap<>());
        // Keyed by lower-case name so a repeated suppression is reported once; the value keeps the
        // spelling the command used.
        final Map<String, String> removedHeaderNames = new LinkedHashMap<>();
        final List<String> bodyParts = new ArrayList<>();
        boolean hasDataOption = false;
        boolean hasHeadOption = false;

        for (int i = 0, size = tokens.size(); i < size; i++) {
            String token = tokens.get(i);

            // cURL short-option clustering: -sSL == -s -S -L, and the first argument-taking letter
            // takes the rest of the token (-sXPOST == -s -XPOST) or the next token (-sX POST). Peel
            // boolean flags off the front so the exact/attached rules below see the option they were
            // written for. This runs only on tokens in option position: option VALUES are consumed
            // with tokens.get(++i) and never reach this point, so "-d '-sX'" stays a literal body.
            while (token.length() > 2 && token.charAt(0) == '-' && token.charAt(1) != '-' && BOOLEAN_SHORT_OPTIONS.indexOf(token.charAt(1)) >= 0) {
                if (token.charAt(1) == 'I') {
                    hasHeadOption = true;
                }

                token = "-" + token.substring(2);
            }

            if (Strings.equals(token, "-I") || Strings.equals(token, "--head")) {
                hasHeadOption = true;
                continue;
            }

            checkSupportedOption(token, curl);

            final String inlineBody = extractInlineDataValue(token);
            final String inlineMethod = extractInlineRequestMethod(token);
            final String ignoredOptionWithArgument = findIgnoredOptionWithArgument(token);

            if (ignoredOptionWithArgument != null) {
                // Only the bare form carries its argument in the next token; -xVALUE and --name=value
                // are self-contained.
                if (ignoredOptionWithArgument.equals(token)) {
                    if (i + 1 >= size) {
                        throw new IllegalArgumentException("Missing argument after " + token);
                    }

                    i++;
                }
            } else if (inlineMethod != null || token.equals("-X") || token.equals("--request")) {
                final String methodName;

                if (inlineMethod != null) {
                    methodName = inlineMethod;
                } else if (i + 1 < size) {
                    methodName = tokens.get(++i);
                } else {
                    throw new IllegalArgumentException("Missing HTTP method after " + token);
                }

                httpMethod = parseHttpMethod(methodName);
            } else if (Strings.equals(token, "--url") || Strings.startsWith(token, "--url=")) {
                final String urlValue;

                if (Strings.startsWith(token, "--url=")) {
                    urlValue = token.substring("--url=".length());
                } else if (i + 1 < size) {
                    urlValue = tokens.get(++i);
                } else {
                    throw new IllegalArgumentException("Missing URL after " + token);
                }

                url = acceptUrl(url, urlValue, curl);
            } else if (Strings.startsWithIgnoreCase(token, "https://") || Strings.startsWithIgnoreCase(token, "http://")) {
                url = acceptUrl(url, token, curl);
            } else if (Strings.equals(token, "-A") || Strings.equals(token, "--user-agent") || Strings.equals(token, "-e") || Strings.equals(token, "--referer")
                    || extractInlineHeaderOptionValue(token) != null) {
                // -A/-e are exactly what curl puts on the wire as User-Agent/Referer, so they are
                // reproduced as headers rather than dropped.
                final boolean referer = token.charAt(1) == 'e' || Strings.startsWith(token, "--referer");
                final String inlineValue = extractInlineHeaderOptionValue(token);

                if (inlineValue == null && i + 1 >= size) {
                    throw new IllegalArgumentException("Missing argument after " + token);
                }

                String value = inlineValue == null ? tokens.get(++i) : inlineValue;

                if (referer) {
                    // "url;auto" also asks curl to update Referer across redirects; the first request
                    // carries the plain URL, and a bare ";auto" sends no Referer at all.
                    value = Strings.removeEnd(value, ";auto");

                    if (value.isEmpty()) {
                        continue;
                    }
                }

                addCapturedHeader(headers, referer ? HttpHeaders.Names.REFERER : HttpHeaders.Names.USER_AGENT, value);
            } else if (Strings.equals(token, "--header") || Strings.equals(token, "-H") || extractInlineHeaderValue(token) != null) {
                final String inlineHeader = extractInlineHeaderValue(token);

                if (inlineHeader == null && i + 1 >= size) {
                    throw new IllegalArgumentException("Missing header after " + token);
                }

                final String header = inlineHeader == null ? tokens.get(++i) : inlineHeader;

                // Same policy as -d @file: curl would read the headers from that file.
                if (header.startsWith("@")) {
                    throw new IllegalArgumentException("File/stdin header references are not supported after " + token);
                }

                final int idx = header.indexOf(':');

                if (idx >= 0) {
                    final String headerName = header.substring(0, idx).trim();
                    final String headerValue = header.substring(idx + 1).trim();

                    if (headerName.isEmpty()) {
                        throw new IllegalArgumentException("Header name is empty after " + token + ": " + header);
                    }

                    if (headerValue.isEmpty()) {
                        // Nothing after the colon is cURL's spelling for "do not send this header at all":
                        // it adds no header and suppresses the one cURL would have generated itself. The
                        // "Name;" form below is the opposite instruction - send it with an empty value.
                        removedHeaderNames.putIfAbsent(headerName.toLowerCase(Locale.ROOT), headerName);
                    } else {
                        addCapturedHeader(headers, headerName, headerValue);

                        if (HttpHeaders.Names.CONTENT_TYPE.equalsIgnoreCase(headerName)) {
                            contentType = headerValue;
                        }
                    }
                } else if (header.trim().endsWith(";")) {
                    // curl's spelling for "send this header with an empty value".
                    final String trimmed = header.trim();
                    final String headerName = trimmed.substring(0, trimmed.length() - 1).trim();

                    if (headerName.isEmpty()) {
                        throw new IllegalArgumentException("Header name is empty after " + token + ": " + header);
                    }

                    addCapturedHeader(headers, headerName, Strings.EMPTY);
                }
                // Anything else (no ':' and no trailing ';') is dropped, as curl itself does.
            } else if (isDataOptionToken(token)) {
                hasDataOption = true;
                final String data;

                if (inlineBody != null) {
                    data = inlineBody;
                } else {
                    if (i + 1 >= size) {
                        throw new IllegalArgumentException("Missing data after " + token);
                    }

                    data = tokens.get(++i);
                }

                final boolean rawData = token.equals("--data-raw") || token.startsWith("--data-raw=");

                if (!rawData && data.startsWith("@")) {
                    throw new IllegalArgumentException("File/stdin data references are not supported after " + token + "; use --data-raw for literal @ data");
                }

                bodyParts.add(data);
            }
        }

        if (Strings.isEmpty(url)) {
            throw new IllegalArgumentException("No URL found in curl command: " + curl);
        }

        if (httpMethod == null) {
            httpMethod = hasHeadOption ? HttpMethod.HEAD : (hasDataOption ? HttpMethod.POST : HttpMethod.GET);
        }

        // A name that the command also gives a value is sent with that value, so only an unmatched
        // suppression is something the generated code cannot reproduce.
        removedHeaderNames.values().removeIf(headers::containsHeader);

        return new CurlCommand(url, httpMethod, headers, hasDataOption ? combineDataValues(bodyParts) : null, contentType,
                new ArrayList<>(removedHeaderNames.values()));
    }

    /**
     * Rejects an option whose effect this converter cannot reproduce in generated code.
     *
     * @param token the token to inspect
     * @param curl the full command, for the error message
     * @throws IllegalArgumentException if the token names an unsupported option.
     */
    private static void checkSupportedOption(final String token, final String curl) throws IllegalArgumentException {
        final int equalsIndex = token.indexOf('=');
        final boolean attachedUnsupportedArgument = token.length() > 2
                && (token.startsWith("-u") || token.startsWith("-b") || token.startsWith("-T") || token.startsWith("-F"));
        final String optionName = attachedUnsupportedArgument ? token.substring(0, 2) : (equalsIndex > 0 ? token.substring(0, equalsIndex) : token);

        if (UNSUPPORTED_OPTIONS.contains(optionName)) {
            throw new IllegalArgumentException(
                    "Unsupported curl option '" + optionName + "': it changes the request in a way this converter cannot reproduce. Command: " + curl);
        }
    }

    /**
     * Identifies an ignored option that carries an argument, in any of its spellings.
     *
     * @param token the token to inspect
     * @return the option name ({@code -x}, {@code --proxy}, ...) when the token is such an option,
     *         either bare (the argument is the next token), with an attached short argument
     *         ({@code -xVALUE}) or in {@code --name=value} form; {@code null} otherwise
     */
    private static String findIgnoredOptionWithArgument(final String token) {
        if (token.length() < 2 || token.charAt(0) != '-') {
            return null;
        }

        if (token.charAt(1) != '-') {
            final String shortOption = token.substring(0, 2);

            return IGNORED_OPTIONS_WITH_ARGUMENT.contains(shortOption) ? shortOption : null;
        }

        final int equalsIndex = token.indexOf('=');
        final String longOption = equalsIndex > 0 ? token.substring(0, equalsIndex) : token;

        return IGNORED_OPTIONS_WITH_ARGUMENT.contains(longOption) ? longOption : null;
    }

    /**
     * Records the request URL, rejecting a second one: curl would issue one request per URL, which a
     * single generated request cannot reproduce. This also keeps the argument of any option missing
     * from {@link #IGNORED_OPTIONS_WITH_ARGUMENT} from silently redirecting the request.
     *
     * @param current the URL seen so far, or {@code null}
     * @param candidate the URL token just read
     * @param curl the full command, for the error message
     * @return {@code candidate}
     * @throws IllegalArgumentException if a URL was already recorded.
     */
    private static String acceptUrl(final String current, final String candidate, final String curl) throws IllegalArgumentException {
        if (current != null) {
            throw new IllegalArgumentException("Multiple URLs are not supported: '" + current + "' and '" + candidate + "'. Command: " + curl);
        }

        return candidate;
    }

    /**
     * Extracts the attached argument of {@code -A/--user-agent} and {@code -e/--referer}.
     *
     * @param token the token to inspect
     * @return the attached value ({@code -AVALUE}, {@code --user-agent=VALUE}, {@code -eVALUE},
     *         {@code --referer=VALUE}); {@code null} when the token is not such a form
     */
    private static String extractInlineHeaderOptionValue(final String token) {
        if (Strings.startsWith(token, "--user-agent=")) {
            return token.substring("--user-agent=".length());
        } else if (Strings.startsWith(token, "--referer=")) {
            return token.substring("--referer=".length());
        } else if ((Strings.startsWith(token, "-A") || Strings.startsWith(token, "-e")) && token.length() > 2) {
            return token.substring(2);
        }

        return null;
    }

    /**
     * Renders the header chain shared by both generators.
     *
     * @param command the parsed command
     * @param indent the per-line indentation prefix
     * @return the rendered {@code .header(..)} chain; empty when there are no headers
     */
    private static String renderHeaders(final CurlCommand command, final String indent) {
        if (command.headers.isEmpty()) {
            return Strings.EMPTY;
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            for (final Map.Entry<String, Object> header : command.headers.toMap().entrySet()) {
                sb.append(indent)
                        .append(".header(\"")
                        .append(escapeJava(header.getKey()))
                        .append("\", \"")
                        .append(escapeJava((String) header.getValue()))
                        .append("\")"); //NOSONAR
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Renders the note both generators emit for {@code -H 'Name:'}, cURL's spelling for suppressing a
     * header rather than sending it empty. Neither builder can stop its own client from adding a default
     * header - nor can the generated code omit the {@code Content-Type} it has to supply for an attached
     * body - so the instruction is reported instead of being silently turned into an empty-valued header.
     *
     * @param command the parsed command
     * @return the comment, ending in a blank line; empty when no suppression was requested
     */
    private static String renderRemovedHeaderNote(final CurlCommand command) {
        if (N.isEmpty(command.removedHeaderNames)) {
            return Strings.EMPTY;
        }

        // escapeJava keeps a name that carries a line break (or any control character) from breaking
        // the single-line comment this returns.
        return "  // cURL was told to suppress the header(s) " + escapeJava(Strings.join(command.removedHeaderNames, ", "))
                + " (-H 'Name:'). The generated builder chain sets no value for them, but neither builder can"
                + " remove a header, so this HTTP client - or a default the generated code itself has to supply,"
                + " such as the Content-Type of an attached body - may still send one." + IOUtil.LINE_SEPARATOR_UNIX + IOUtil.LINE_SEPARATOR_UNIX;
    }

    // Shared by HAR replay and both cURL converters. Their replacing header APIs require one
    // effective value per name; unknown grammars and coding stacks cannot safely be guessed.
    /**
     * @throws IllegalArgumentException if a repeated header cannot be represented by the generated request builder
     */
    static void addCapturedHeader(final HttpHeaders headers, final String name, final String value) throws IllegalArgumentException {
        if (!headers.containsHeader(name)) {
            headers.set(name, value);
            return;
        }

        final String normalizedName = name.toLowerCase(Locale.ROOT);
        final boolean cookie = normalizedName.equals("cookie");
        final String previous = headers.getAsString(name);

        if ((!cookie && !COMBINABLE_CAPTURED_HEADERS.contains(normalizedName)) || previous == null || value == null
                || ((normalizedName.equals("if-match") || normalizedName.equals("if-none-match"))
                        && (previous.trim().equals("*") || value.trim().equals("*")))) {
            throw new IllegalArgumentException("Cannot reproduce repeated header: " + name);
        }

        // Empty list contributions contain no values; in particular, avoid empty Cookie separators.
        headers.set(name, previous.isBlank() ? value : (value.isBlank() ? previous : previous + (cookie ? "; " : ", ") + value));
    }

    /**
     * Converts a cURL command string into Java code for creating an HttpRequest.
     *
     * <p>This method parses a cURL command and generates the equivalent Java code
     * using the HttpRequest API. It extracts the URL, HTTP method, headers, and
     * request body from the cURL command and produces properly formatted Java code that
     * compiles against the HttpRequest API. Whether the generated request can actually be
     * issued depends on the method: see "HTTP Method Detection" below for {@code PATCH} and
     * {@code CONNECT}.</p>
     *
     * <p>Supported cURL options:</p>
     * <ul>
     *   <li>{@code -X, --request}: HTTP method (including {@code -XPOST} and {@code --request=POST})</li>
     *   <li>{@code -H, --header}: HTTP headers (including attached/equals forms; can be specified multiple times).
     *       {@code -H 'Name;'} produces the header with an empty value, exactly as curl sends it.
     *       {@code -H 'Name:'} (nothing after the colon) is curl's spelling for <i>suppressing</i> a header:
     *       no {@code .header(..)} call is emitted for it, and the generated code opens with a comment saying
     *       that this client - or, for the {@code Content-Type} of an attached body, the generated code itself -
     *       may still send one, because neither builder can remove a header. A name that the command also gives
     *       a value is sent with that value, as curl does.
     *       A header without {@code :} or a trailing {@code ;} is dropped, as curl does; an empty header
     *       name ({@code -H ': value'}) and a file reference ({@code -H @headers.txt}) are rejected</li>
     *   <li>{@code -d, --data, --data-raw, --data-ascii}: Literal request body data (including attached/equals forms);
     *       file/stdin references beginning with {@code @} require {@code --data-raw} to be treated literally and are otherwise rejected</li>
     *   <li>{@code -I, --head}: HEAD request (inferred method)</li>
     *   <li>{@code -A, --user-agent} and {@code -e, --referer}: emitted as {@code User-Agent} / {@code Referer}
     *       headers (a {@code ;auto} suffix on the referer is dropped; a bare {@code ;auto} sends no Referer)</li>
     *   <li>{@code --url}: the request URL, as an alternative to a positional {@code http(s)://} argument</li>
     * </ul>
     *
     * <p>Short options may be clustered as in curl ({@code -sSL}, {@code -sX POST}, {@code -sXPOST},
     * {@code -sd 'a=1'}, {@code -Lu user:pw}): leading argument-less flags are peeled off and the first
     * argument-taking letter is dispatched with the rest of the token or the next token as its argument.</p>
     *
     * <p>Options that would change the request in a way the generated code cannot reproduce
     * ({@code --data-binary}, {@code --data-urlencode}, {@code -F/--form}, {@code -u/--user},
     * {@code -b/--cookie}, {@code -T/--upload-file}, {@code --json}, {@code -G/--get}) are rejected with an
     * {@link IllegalArgumentException} rather than silently dropped, including attached short-option
     * arguments such as {@code -uuser:password} and clustered forms such as {@code -Lu user:password}.
     * A second URL (positional or via {@code --url}) is rejected as well, because curl would issue
     * one request per URL. Options that do not affect the
     * request that is issued (for example {@code -L}, {@code --compressed}, {@code -s}) are ignored.
     * The following options are ignored <i>together with their argument</i>, so that argument can never
     * be mistaken for the URL or the body: {@code -x/--proxy}, {@code -o/--output}, {@code -c/--cookie-jar},
     * {@code -D/--dump-header}, {@code --cacert}, {@code --cert}, {@code --key}, {@code -m/--max-time},
     * {@code --connect-timeout}, {@code --retry}, {@code --retry-delay}, {@code --retry-max-time},
     * {@code -w/--write-out}, {@code --resolve}, {@code --socks4}, {@code --socks4a}, {@code --socks5},
     * {@code --socks5-hostname}, {@code --interface}, {@code -r/--range} (no {@code Range} header is emitted),
     * {@code --limit-rate}, {@code -U/--proxy-user}, {@code --ciphers}, {@code --proto}, {@code --max-redirs},
     * {@code -y/--speed-time}, {@code -Y/--speed-limit}, {@code --stderr}, {@code --trace}, {@code --trace-ascii}
     * and {@code -K/--config}. Any other unknown option is ignored without consuming an argument.</p>
     *
     * <p>HTTP Method Detection:</p>
     * <ul>
     *   <li>If {@code -X} is specified, uses that method</li>
     *   <li>If {@code -d} is present without {@code -X}, defaults to POST</li>
     *   <li>If {@code -I} (or {@code --head}) is present without {@code -X}, defaults to HEAD</li>
     *   <li>Otherwise defaults to GET</li>
     *   <li>{@code PATCH} and {@code CONNECT} are emitted as {@code .execute(HttpMethod.PATCH)} /
     *       {@code .execute(HttpMethod.CONNECT)}, which HttpRequest rejects at run time with an
     *       {@link UnsupportedOperationException} because {@code java.net.HttpURLConnection} cannot issue
     *       them. The generated code starts with a {@code //} comment saying so and pointing at
     *       {@link #curlToOkHttpRequestCode(String)} and {@link HttpMethod#PATCH}</li>
     * </ul>
     *
     * <p>The generated code properly escapes special characters in strings using
     * Java escape sequences. Headers are extracted and added individually. If a
     * request body is present <i>and</i> the resolved method accepts one, it is declared as a
     * separate {@code requestBody} variable for readability; otherwise the body is omitted and the
     * generated code carries a comment saying so. Attached data defaults to
     * {@code application/x-www-form-urlencoded} when no Content-Type header was supplied,
     * matching cURL's data options.</p>
     *
     * <p>Repeated header names are matched case-insensitively. The fields whose grammar is the
     * comma-separated list of RFC 9110 &sect;5.6.1 - Accept, Accept-Charset, Accept-Encoding, Accept-Language,
     * Access-Control-Request-Headers, Cache-Control, Content-Language, Expect, Forwarded, If-Match,
     * If-None-Match, Pragma, TE, Via and X-Forwarded-For - are combined in encounter order with commas;
     * Cookie values use semicolons. Empty contributions are ignored when combining. Other repeated fields,
     * null repeated values and repeated wildcard conditional fields are rejected because their semantics
     * cannot be reproduced by these builders.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String curl = "curl -X POST http://localhost:18080/users "
     *     + "-H \"Content-Type: application/json\" "
     *     + "-H \"Authorization: Bearer token123\" "
     *     + "-d '{\"name\":\"John\",\"age\":30}'";
     * String javaCode = WebUtil.curlToHttpRequestCode(curl);
     * System.out.println(javaCode);
     * }</pre>
     *
     * @param curl the cURL command string to convert, must not be {@code null} or empty
     *             and must start with "curl" (case-insensitive)
     * @return Java code string for creating an equivalent HttpRequest, formatted
     *         with proper indentation and line separators
     * @throws IllegalArgumentException if the curl parameter is {@code null}, empty, doesn't start with "curl",
     *         contains an unclosed quote, contains no URL or more than one URL, uses an unsupported option, a
     *         request/data/header option has a missing or unsupported argument, a header references a file
     *         ({@code -H @file}) or has an empty name, or repeated headers cannot be combined.
     * @see #curlToOkHttpRequestCode(String)
     */
    public static String curlToHttpRequestCode(final String curl) throws IllegalArgumentException {
        final String indent = "\n    ";
        final CurlCommand command = parseCurlCommand(curl);
        final HttpMethod httpMethod = command.httpMethod;
        final boolean attachBody = command.hasBody() && supportsHttpRequestBodyBuilder(httpMethod);

        final StringBuilder sb = new StringBuilder(IOUtil.LINE_SEPARATOR_UNIX);

        // The method is reproduced faithfully, but HttpRequest (java.net.HttpURLConnection) refuses
        // these two at run time; say so in the output instead of silently tunnelling through POST.
        if (httpMethod == HttpMethod.PATCH || httpMethod == HttpMethod.CONNECT) {
            sb.append("  // HttpRequest cannot issue ")
                    .append(httpMethod.name())
                    .append(": java.net.HttpURLConnection rejects this method, so .execute(HttpMethod.")
                    .append(httpMethod.name())
                    .append(") throws at run time (see HttpMethod#PATCH). Use WebUtil.curlToOkHttpRequestCode(..) instead.")
                    .append(IOUtil.LINE_SEPARATOR_UNIX)
                    .append(IOUtil.LINE_SEPARATOR_UNIX);
        }

        sb.append(renderRemovedHeaderNote(command));

        // Declared only when it will actually be attached, so the generated code has no unused local.
        if (attachBody) {
            sb.append("  String requestBody = \"")
                    .append(escapeJava(command.body))
                    .append("\";")
                    .append(IOUtil.LINE_SEPARATOR_UNIX)
                    .append(IOUtil.LINE_SEPARATOR_UNIX);
        } else if (command.hasBody()) {
            sb.append("  // HttpRequest.body(...) does not support this method. Request body omitted for ")
                    .append(httpMethod.name())
                    .append('.')
                    .append(IOUtil.LINE_SEPARATOR_UNIX)
                    .append(IOUtil.LINE_SEPARATOR_UNIX);
        }

        sb.append("  HttpRequest.url(\"").append(escapeJava(command.url)).append("\")").append(renderHeaders(command, indent));

        if (attachBody) {
            // HttpRequest otherwise labels a String body as text/plain; curl's data options
            // instead default to form data. Preserve any explicitly supplied header.
            if (command.contentType == null) {
                sb.append(indent).append(".header(\"Content-Type\", \"application/x-www-form-urlencoded\")");
            }
            sb.append(indent).append(".body(requestBody)");
        }

        if (httpMethod == HttpMethod.GET) {
            sb.append(indent).append(".get();");
        } else if (httpMethod == HttpMethod.POST) {
            sb.append(indent).append(".post();");
        } else if (httpMethod == HttpMethod.PUT) {
            sb.append(indent).append(".put();");
        } else if (httpMethod == HttpMethod.DELETE) {
            sb.append(indent).append(".delete();");
        } else if (httpMethod == HttpMethod.HEAD) {
            sb.append(indent).append(".head();");
        } else {
            sb.append(indent).append(".execute(HttpMethod.").append(httpMethod.name()).append(");");
        }

        return sb.toString();
    }

    /**
     * Converts a cURL command string into Java code for creating an OkHttpRequest.
     *
     * <p>This method parses a cURL command and generates the equivalent Java code
     * using the OkHttpRequest API. It handles the same cURL options as
     * {@link #curlToHttpRequestCode(String)} but generates code specifically for the
     * OkHttp client library.</p>
     *
     * <p>Key differences from {@link #curlToHttpRequestCode(String)}:</p>
     * <ul>
     *   <li>Creates a {@code RequestBody} via {@code RequestBody.create(content, mediaType)} when a body is present</li>
     *   <li>Takes the {@code MediaType} from the {@code Content-Type} header, defaulting to
     *       {@code application/x-www-form-urlencoded} — the type cURL itself applies to {@code -d} data</li>
     *   <li>Attaches the body using {@code .body(requestBody)} and invokes the HTTP method with no arguments (e.g. {@code .post()})</li>
     *   <li>Uses OkHttp-specific method calls ({@code .post()}, {@code .put()}, etc.)</li>
     * </ul>
     *
     * <p>The generated code includes:</p>
     * <ul>
     *   <li>{@code RequestBody} creation with the appropriate {@code MediaType}</li>
     *   <li>OkHttpRequest builder chain with URL, headers, and body</li>
     *   <li>Appropriate HTTP method call ({@code get()}, {@code post()}, {@code put()}, {@code delete()}, or {@code execute()})</li>
     *   <li>A try-with-resources block that closes the returned OkHttp {@code Response}</li>
     * </ul>
     *
     * <p>HTTP Method Detection and unsupported-option handling follow the same rules as
     * {@link #curlToHttpRequestCode(String)}, including rejection of {@code -G/--get} even without data.
     * Unlike that generator, {@code PATCH} needs no warning comment here: OkHttpRequest issues it natively.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String curl = "curl -X POST http://localhost:18080/users "
     *     + "-H \"Content-Type: application/json\" "
     *     + "-d '{\"name\":\"John\"}'";
     * String javaCode = WebUtil.curlToOkHttpRequestCode(curl);
     * System.out.println(javaCode);
     * }</pre>
     *
     * @param curl the cURL command string to convert, must not be {@code null} or empty
     *             and must start with "curl" (case-insensitive)
     * @return Java code string for creating an equivalent OkHttpRequest, formatted
     *         with proper indentation and line separators
     * @throws IllegalArgumentException if the curl parameter is {@code null}, empty, doesn't start with "curl",
     *         contains an unclosed quote, contains no URL or more than one URL, uses an unsupported option, a
     *         request/data/header option has a missing or unsupported argument, a header references a file
     *         ({@code -H @file}) or has an empty name, or repeated headers cannot be combined.
     * @see #curlToHttpRequestCode(String)
     */
    public static String curlToOkHttpRequestCode(final String curl) throws IllegalArgumentException {
        final String indent = "\n    ";
        final CurlCommand command = parseCurlCommand(curl);
        final HttpMethod httpMethod = command.httpMethod;
        final boolean attachBody = command.hasBody() && supportsGeneratedOkHttpBody(httpMethod);

        final StringBuilder sb = new StringBuilder(IOUtil.LINE_SEPARATOR_UNIX);

        sb.append(renderRemovedHeaderNote(command));

        if (attachBody) {
            // cURL sends -d data as application/x-www-form-urlencoded unless told otherwise, so a
            // missing Content-Type header must not become a null MediaType. The argument order is
            // RequestBody.create(content, mediaType): the reverse order is deprecated in OkHttp 4.
            final String mediaType = Strings.isEmpty(command.contentType) ? HttpHeaders.Values.APPLICATION_URL_ENCODED : command.contentType;

            sb.append("  RequestBody requestBody = RequestBody.create(\"")
                    .append(escapeJava(command.body))
                    .append("\", MediaType.parse(\"")
                    .append(escapeJava(mediaType))
                    .append("\"));")
                    .append(IOUtil.LINE_SEPARATOR_UNIX)
                    .append(IOUtil.LINE_SEPARATOR_UNIX);
        } else if (command.hasBody()) {
            sb.append("  // Request body omitted for ")
                    .append(httpMethod.name())
                    .append(" because this generated request does not support it.")
                    .append(IOUtil.LINE_SEPARATOR_UNIX)
                    .append(IOUtil.LINE_SEPARATOR_UNIX);
        }

        sb.append("  Response response = OkHttpRequest.url(\"").append(escapeJava(command.url)).append("\")").append(renderHeaders(command, indent));

        if (attachBody) {
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
        } else if (httpMethod == HttpMethod.HEAD) {
            sb.append(indent).append(".head();");
        } else {
            sb.append(indent).append(".execute(HttpMethod.").append(httpMethod.name()).append(");");
        }

        sb.append(IOUtil.LINE_SEPARATOR_UNIX)
                .append("  try (response) {")
                .append(IOUtil.LINE_SEPARATOR_UNIX)
                .append("      // Consume the response here.")
                .append(IOUtil.LINE_SEPARATOR_UNIX)
                .append("  }");

        return sb.toString();
    }

    private static boolean isDataOptionToken(final String token) {
        // --data-ascii is documented as a plain alias for -d, so it is treated as one here.
        return Strings.equals(token, "--data-raw") || Strings.equals(token, "--data") || Strings.equals(token, "--data-ascii") || Strings.equals(token, "-d")
                || Strings.startsWith(token, "--data-raw=") || Strings.startsWith(token, "--data=") || Strings.startsWith(token, "--data-ascii=")
                || Strings.startsWith(token, "-d=") || Strings.startsWith(token, "-d") && token.length() > 2;
    }

    private static String extractInlineDataValue(final String token) {
        final String dataRawPrefix = "--data-raw=";
        final String dataAsciiPrefix = "--data-ascii=";
        final String dataPrefix = "--data=";

        if (Strings.startsWith(token, dataRawPrefix)) {
            return token.substring(dataRawPrefix.length());
        } else if (Strings.startsWith(token, dataAsciiPrefix)) {
            return token.substring(dataAsciiPrefix.length());
        } else if (Strings.startsWith(token, dataPrefix)) {
            return token.substring(dataPrefix.length());
        } else if (Strings.startsWith(token, "-d") && token.length() > 2) {
            return token.substring(2);
        }

        return null;
    }

    private static String extractInlineRequestMethod(final String token) {
        if (Strings.startsWith(token, "--request=")) {
            return token.substring("--request=".length());
        } else if (Strings.startsWith(token, "-X") && token.length() > 2) {
            return token.substring(2);
        }

        return null;
    }

    /**
     * @throws IllegalArgumentException if the method name does not match a supported HttpMethod constant
     */
    private static HttpMethod parseHttpMethod(final String method) throws IllegalArgumentException {
        final String methodName = method.toUpperCase(Locale.ROOT);

        if (!httpMethodMap.containsValue(methodName)) {
            throw new IllegalArgumentException("Unsupported HTTP method: " + methodName);
        }

        return HttpMethod.valueOf(methodName);
    }

    private static String extractInlineHeaderValue(final String token) {
        if (Strings.startsWith(token, "--header=")) {
            return token.substring("--header=".length());
        } else if (Strings.startsWith(token, "-H") && token.length() > 2) {
            return token.substring(2);
        }

        return null;
    }

    private static String combineDataValues(final List<String> bodyParts) {
        if (bodyParts.isEmpty()) {
            return Strings.EMPTY;
        }

        if (bodyParts.size() == 1) {
            return bodyParts.get(0);
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            for (int i = 0, size = bodyParts.size(); i < size; i++) {
                if (i > 0) {
                    sb.append('&');
                }

                sb.append(bodyParts.get(i));
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    private static boolean supportsHttpRequestBodyBuilder(final HttpMethod httpMethod) {
        return httpMethod == HttpMethod.POST || httpMethod == HttpMethod.PUT || httpMethod == HttpMethod.PATCH || httpMethod == HttpMethod.DELETE
                || httpMethod == HttpMethod.OPTIONS;
    }

    private static boolean supportsGeneratedOkHttpBody(final HttpMethod httpMethod) {
        return httpMethod != HttpMethod.GET && httpMethod != HttpMethod.HEAD;
    }

    private static String escapeJava(final String str) {
        return EscapeUtil.escapeJava(str);
    }

    /**
     * @throws IllegalArgumentException if the command is null or empty, does not begin with curl, ends with an escape character, or contains an unclosed quote
     */
    private static List<String> parseCurl(final String curl) throws IllegalArgumentException {
        N.checkArgNotEmpty(curl, cs.curl);

        final String str = curl.trim();
        N.checkArgument(str.length() >= 4 && str.regionMatches(true, 0, "curl", 0, 4) && (str.length() == 4 || Character.isWhitespace(str.charAt(4))),
                "Input curl script does not start with the 'curl' command");

        final List<String> tokens = new ArrayList<>();
        final StringBuilder token = new StringBuilder();
        final int len = str.length();
        boolean tokenStarted = false;
        char quote = 0;
        int quoteStart = -1;

        for (int i = 0; i < len; i++) {
            final char ch = str.charAt(i);

            if (quote == '\'') {
                if (ch == '\'') {
                    quote = 0;
                } else {
                    token.append(ch);
                }

                continue;
            }

            if (quote == '"') {
                if (ch == '"') {
                    quote = 0;
                } else if (ch == '\\' && i + 1 < len) {
                    final char next = str.charAt(i + 1);

                    if (next == '\n') {
                        i++;
                    } else if (next == '\r') {
                        i++;

                        if (i + 1 < len && str.charAt(i + 1) == '\n') {
                            i++;
                        }
                    } else if (next == '"') {
                        token.append(next);
                        i++;
                    } else if (next == '\\' || next == '$' || next == '`') {
                        token.append(next);
                        i++;
                    } else {
                        token.append(ch);
                    }
                } else {
                    token.append(ch);
                }

                continue;
            }

            if (Character.isWhitespace(ch)) {
                if (tokenStarted) {
                    tokens.add(token.toString());
                    token.setLength(0);
                    tokenStarted = false;
                }
            } else if (ch == '\'' || ch == '"') {
                quote = ch;
                quoteStart = i;
                tokenStarted = true;
            } else if (ch == '\\') {
                if (i + 1 >= len) {
                    throw new IllegalArgumentException("Trailing escape character at position: " + i);
                }

                final char next = str.charAt(++i);

                if (next == '\r' || next == '\n') {
                    // A line continuation contributes no character, so it must NOT mark a token as
                    // started: doing so made the following whitespace flush an empty token, which the
                    // option handling then consumed as the value of the preceding -d/-H/-X. A
                    // continuation inside an already-open token (a\<NL>b) still joins, because
                    // tokenStarted is left as it was rather than reset.
                    if (next == '\r' && i + 1 < len && str.charAt(i + 1) == '\n') {
                        i++;
                    }
                } else {
                    token.append(next);
                    tokenStarted = true;
                }
            } else {
                token.append(ch);
                tokenStarted = true;
            }
        }

        if (quote != 0) {
            throw new IllegalArgumentException("Unclosed quote starting at position: " + quoteStart + ". String ends at position: " + len);
        }

        if (tokenStarted) {
            tokens.add(token.toString());
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
     * {@link #createCurlLoggingOkHttpRequest(String, char, Consumer)}.</p>
     *
     * <p>The interceptor is added to a new {@code OkHttpClient} instance specifically
     * created for this request, ensuring the logging doesn't affect other clients.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create request with cURL logging
     * OkHttpRequest request = WebUtil.createCurlLoggingOkHttpRequest(
     *     "http://localhost:18080",
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
     * // curl -X POST 'http://localhost:18080' \
     * //   -H 'Authorization: Bearer token123' \
     * //   -H 'Content-Type: application/json' \
     * //   -d '{"key":"value"}'
     * }</pre>
     *
     * @param url the base URL for the HTTP request, must not be {@code null}
     * @param logHandler consumer that receives the generated cURL command string
     *                   for each request.
     * @return an OkHttpRequest configured with cURL logging interceptor
     * @throws IllegalArgumentException if {@code url} is {@code null} or empty, or if {@code logHandler} is {@code null}.
     * @see #createCurlLoggingOkHttpRequest(String, char, Consumer)
     * @see CurlInterceptor
     * @see <a href="https://github.com/mrmike/Ok2Curl">Ok2Curl - OkHttp to cURL converter</a>
     */
    public static OkHttpRequest createCurlLoggingOkHttpRequest(final String url, final Consumer<? super String> logHandler) throws IllegalArgumentException {
        N.checkArgNotNull(logHandler, cs.logHandler);

        return createCurlLoggingOkHttpRequest(url, CurlInterceptor.DEFAULT_QUOTE_CHAR, logHandler);
    }

    /**
     * Creates an OkHttpRequest configured to log cURL commands with a custom quote character.
     *
     * <p>This method is similar to {@link #createCurlLoggingOkHttpRequest(String, Consumer)}
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
     * OkHttpRequest request = WebUtil.createCurlLoggingOkHttpRequest(
     *     "http://localhost:18080",
     *     '"',
     *     curl -> System.out.println("cURL: " + curl)
     * );
     *
     * request.header("Content-Type", "application/json")
     *        .post();
     *
     * // Generates:
     * // curl -X POST "http://localhost:18080" \
     * //   -H "Content-Type: application/json"
     * }</pre>
     *
     * @param url the base URL for the HTTP request, must not be {@code null}
     * @param quoteChar the character to use for quoting in cURL commands;
     *                  must be a single quote (') or a double quote (")
     * @param logHandler consumer that receives the generated cURL command string
     *                   for each request.
     * @return an OkHttpRequest configured with cURL logging interceptor using the
     *         specified quote character
     * @throws IllegalArgumentException if {@code url} is {@code null} or empty, or if {@code logHandler} is {@code null}.
     * @see #createCurlLoggingOkHttpRequest(String, Consumer)
     * @see CurlInterceptor
     */
    public static OkHttpRequest createCurlLoggingOkHttpRequest(final String url, final char quoteChar, final Consumer<? super String> logHandler)
            throws IllegalArgumentException {
        N.checkArgNotNull(logHandler, cs.logHandler);

        // Derived from the shared default client so every logging request reuses one dispatcher and
        // connection pool; building a standalone OkHttpClient per call leaked both with no close path.
        final okhttp3.OkHttpClient client = OkHttpRequest.DEFAULT_CLIENT.newBuilder().addInterceptor(new CurlInterceptor(quoteChar, logHandler)).build();

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
     *   <li>HTTP method using the {@code -X} flag ({@code -I} for HEAD, which is what curl requires - unless
     *       a body is given, since curl refuses {@code -I} together with a data option; then {@code -X HEAD} is used)</li>
     *   <li>URL enclosed in the specified quote character, with {@code --globoff} to preserve literal brackets and braces</li>
     *   <li>All headers using {@code -H} flags with proper quoting and escaping; a header whose value resolves
     *       to an empty or all-whitespace string is written as {@code -H 'Name;'}, curl's spelling for "send this
     *       header empty" ({@code -H 'Name: '} would instead tell curl to <i>remove</i> the header, because curl
     *       skips the whitespace after the colon)</li>
     *   <li>Request body using {@code --data-raw} if body is not empty; a leading {@code @} is sent literally</li>
     *   <li>Content-Type header if body is present, {@code headers} carries no {@code Content-Type} entry at
     *       all - one present with a {@code null}, empty or blank value counts as present, so a command never
     *       carries two {@code Content-Type} lines - and {@code bodyContentType} is not blank (a blank one
     *       could only be written as {@code -H 'Content-Type: '}, which <i>removes</i> the header)</li>
     * </ul>
     *
     * <p>Special handling:</p>
     * <ul>
     *   <li>If {@code body} is not empty but Content-Type header is not present in {@code headers},
     *       and {@code bodyContentType} is not blank, the Content-Type header is automatically added</li>
     *   <li>Header values are converted using the field-aware {@link HttpHeaders#valueOf(String, Object)},
     *       including its handling of {@link java.util.Collection} (joined with {@code ", "}, or with
     *       {@code "; "} on the {@code Cookie} field) and {@link java.util.Date}/{@link java.time.Instant}
     *       (HTTP-date formatted) values</li>
     *   <li>URLs, header names and values, and bodies are escaped for the chosen {@code quoteChar}:
     *       inside single quotes each {@code '} becomes {@code '\''}; inside double quotes
     *       {@code \ " $ `} are backslash-escaped</li>
     *   <li>The command is formatted with line separators at the beginning and end</li>
     * </ul>
     *
     * <p>Quoting targets POSIX-compatible shells. Both quote modes preserve literal values;
     * double-quote mode escapes dollar signs and backticks to prevent variable and command
     * expansion. The generated quoting is not intended for Windows cmd.exe or PowerShell.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> headers = new HashMap<>();
     * headers.put("Content-Type", "application/json");
     * headers.put("Authorization", "Bearer token123");
     *
     * String curl = WebUtil.buildCurl(
     *     HttpMethod.POST,
     *     "http://localhost:18080/users",
     *     headers,
     *     "{\"name\":\"John\",\"email\":\"john@example.com\"}",
     *     "application/json",
     *     '\''
     * );
     * System.out.println(curl);
     * }</pre>
     *
     * @param httpMethod the HTTP method (e.g., {@link HttpMethod#GET}, {@link HttpMethod#POST}), must not be {@code null}
     * @param url the target URL, must not be {@code null} or empty
     * @param headers map of HTTP headers whose values are converted by
     *                {@link HttpHeaders#valueOf(String, Object)} (can be {@code null} or empty)
     * @param body the request body string (can be {@code null} or empty for requests without a body)
     * @param bodyContentType the MIME type of the body (e.g., "application/json"), used to add
     *                 Content-Type header if not already present in headers (can be null)
     * @param quoteChar the character to use for quoting values in the cURL command;
     *                  must be a single quote (') or a double quote (")
     * @return a formatted cURL command string with line separators, ready for execution
     * @throws IllegalArgumentException if {@code httpMethod} is {@code null}, {@code url} is {@code null}
     *         or empty, {@code quoteChar} is neither a single nor a double quote, or a header name is
     *         {@code null}.
     * @see HttpHeaders#valueOf(String, Object)
     * @see Strings#escapeQuotes(String, char)
     */
    public static String buildCurl(final HttpMethod httpMethod, final String url, final Map<String, ?> headers, final String body, final String bodyContentType,
            final char quoteChar) throws IllegalArgumentException {
        N.checkArgNotNull(httpMethod, cs.httpMethod);

        return buildCurlByMethodName(httpMethod.name(), url, headers, body, bodyContentType, quoteChar);
    }

    /**
     * String-based counterpart used by the OkHttp interceptor, whose request model permits
     * extension methods (for example, {@code PROPFIND}) that are not members of {@link HttpMethod}.
     * @throws IllegalArgumentException if {@code httpMethod} or {@code url} is null or empty, or {@code quoteChar} is neither a single nor a double
     *         quote
     */
    static String buildCurlByMethodName(final String httpMethod, final String url, final Map<String, ?> headers, final String body,
            final String bodyContentType, final char quoteChar) throws IllegalArgumentException {
        N.checkArgNotEmpty(httpMethod, cs.httpMethod);
        N.checkArgNotEmpty(url, cs.url);
        N.checkArgument(quoteChar == '\'' || quoteChar == '"', "quoteChar must be a single (') or double (\") quote, but was: {}", quoteChar);

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            sb.append(IOUtil.LINE_SEPARATOR_UNIX);

            // HEAD is emitted as -I, not -X HEAD: "-X HEAD" changes the method without telling curl
            // to expect no response body, so depending on the server it hangs, times out or reports
            // a truncated response. -I is however a request-method option of its own, so curl refuses
            // it together with a data option ("You can only select one HTTP request method"). A HEAD
            // request that carries a body therefore has to fall back to -X HEAD, which at least runs
            // and sends what was asked for. This emptiness test must stay identical to the
            // Strings.isNotEmpty(body) guarding --data-raw below - -I is correct exactly when no data option
            // is emitted - and it is isEmpty, not isBlank, on both: a whitespace-only body is still a body
            // ("--data-raw '   '" sends three bytes), so it must take -X HEAD rather than be silently dropped.
            if (HttpMethod.HEAD.name().equalsIgnoreCase(httpMethod) && Strings.isEmpty(body)) {
                sb.append("curl -I ");
            } else {
                sb.append("curl -X ").append(httpMethod).append(" ");
            }

            // Shell quoting does not disable cURL's own expansion of braces and bracket ranges.
            sb.append(quoteChar).append(escapeForShellQuote(url, quoteChar)).append(quoteChar).append(" --globoff");

            // Whether the loop below already put a Content-Type line in the command. The body's fallback type
            // is keyed off this rather than off the header's VALUE: a value test cannot tell an absent field
            // from one present with a null or empty value, so "Content-Type" -> "" used to produce both a
            // "-H 'Content-Type;'" here and a "-H 'Content-Type: <bodyContentType>'" below - two Content-Type
            // lines in one command.
            boolean contentTypeEmitted = false;

            if (N.notEmpty(headers)) {
                String headerValue = null;

                for (final Map.Entry<String, ?> e : headers.entrySet()) {
                    N.checkArgNotNull(e.getKey(), "header name");

                    if (!contentTypeEmitted && HttpHeaders.Names.CONTENT_TYPE.equalsIgnoreCase(e.getKey())) {
                        contentTypeEmitted = true;
                    }

                    // Field-aware: a Collection on Cookie is joined with "; " (RFC 6265 5.4), not with the
                    // ", " of the ordinary list fields, so the emitted -H line is one curl can actually send.
                    headerValue = HttpHeaders.valueOf(e.getKey(), e.getValue());

                    sb.append(" -H ").append(quoteChar).append(escapeForShellQuote(e.getKey(), quoteChar));

                    if (Strings.isBlank(headerValue)) {
                        // "Name: " is curl's spelling for *removing* a header, and curl skips the whitespace
                        // after the colon, so "Name:   " is that same removal. "Name;" is the spelling that
                        // actually sends the header with no value; an HTTP field value has its surrounding
                        // whitespace stripped anyway, so a blank value is written that way too.
                        sb.append(';');
                    } else {
                        sb.append(": ").append(escapeForShellQuote(headerValue, quoteChar));
                    }

                    sb.append(quoteChar);
                }
            }

            if (Strings.isNotEmpty(body)) {
                // Added only when the header loop emitted no Content-Type line at all: an entry present with a
                // null, empty or blank value is still a Content-Type the caller asked to reproduce, and a
                // second line for the same field would make the command self-contradictory. Blank rather than
                // empty on bodyContentType, because "-H 'Content-Type:   '" tells curl to REMOVE the header.
                if (!contentTypeEmitted && Strings.isNotBlank(bodyContentType)) {
                    sb.append(" -H ")
                            .append(quoteChar)
                            .append(HttpHeaders.Names.CONTENT_TYPE)
                            .append(": ")
                            .append(escapeForShellQuote(bodyContentType, quoteChar))
                            .append(quoteChar);
                }

                // Shell quoting does not prevent curl's -d option from treating a leading @ as
                // a file name (or stdin for @-). The captured body is always literal text.
                sb.append(" --data-raw ").append(quoteChar).append(escapeForShellQuote(body, quoteChar)).append(quoteChar);
            }

            sb.append(IOUtil.LINE_SEPARATOR_UNIX);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Escapes a value so it survives inside a shell-quoted argument in the generated cURL command.
     *
     * <p>POSIX single-quoted strings cannot contain an escaped single quote, so each embedded
     * {@code '} is rewritten as {@code '\''} (close quote, escaped quote, reopen quote). Inside
     * double quotes the shell still expands {@code $}, {@code `} and processes {@code \}, so those
     * (and {@code "}) are backslash-escaped.</p>
     *
     * @param str the value to escape; may be {@code null} or empty (returned unchanged)
     * @param quoteChar the quote character used to wrap the value; a single or double quote
     * @return the escaped value, safe to place between two {@code quoteChar} characters
     */
    private static String escapeForShellQuote(final String str, final char quoteChar) {
        if (Strings.isEmpty(str)) {
            return str;
        }

        if (quoteChar == '\'') {
            return str.replace("'", "'\\''");
        }

        final StringBuilder sb = Objectory.createStringBuilder(str.length() + 16);

        try {
            for (int i = 0, len = str.length(); i < len; i++) {
                final char ch = str.charAt(i);

                if (ch == '\\' || ch == '"' || ch == '$' || ch == '`') {
                    sb.append('\\');
                }

                sb.append(ch);
            }

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
     *   <li>The {@code requestBodyType} parameter is not {@code null} or empty</li>
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
     *                        "text/xml", "application/x-www-form-urlencoded"), can be {@code null} or empty
     * @param httpHeaders the HttpHeaders object to conditionally update, must not be {@code null}
     *                    when {@code requestBodyType} is not {@code null} or empty
     * @throws IllegalArgumentException if {@code httpHeaders} is {@code null} and {@code requestBodyType}
     *                                  is not {@code null} or empty
     * @see HttpHeaders#setContentType(String)
     * @see HttpHeaders#get(String)
     */
    public static void setContentTypeByRequestBodyType(final String requestBodyType, final HttpHeaders httpHeaders) throws IllegalArgumentException {
        if (Strings.isNotEmpty(requestBodyType)) {
            N.checkArgNotNull(httpHeaders, cs.httpHeaders);
        }

        if (Strings.isNotEmpty(requestBodyType) && httpHeaders.get(HttpHeaders.Names.CONTENT_TYPE) == null) {
            httpHeaders.setContentType(requestBodyType);
        }
    }

    private WebUtil() {
        // Utility class.
    }
}
