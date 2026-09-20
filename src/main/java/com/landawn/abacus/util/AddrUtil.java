/**
 * Copyright (C) 2006-2009 Dustin Sallings
 * Copyright (C) 2009-2011 Couchbase, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
 */

package com.landawn.abacus.util;

import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Utility class for handling and parsing network addresses.
 * Provides methods to convert various address formats (strings, {@link java.net.URL URLs}) into
 * {@link java.net.InetSocketAddress} instances suitable for network operations.
 *
 * <p>This class supports parsing of multiple address formats including:</p>
 * <ul>
 *   <li>Space-separated addresses: {@code "host1:port1 host2:port2"}</li>
 *   <li>Comma-separated addresses: {@code "host1:port1, host2:port2"}</li>
 *   <li>Bracketed IPv6: {@code "[::1]:11211"}, {@code "[fe80::1]:8080"} - always unambiguous</li>
 *   <li>Unbracketed IPv6 whose trailing token cannot be a hextet, so the split is the only reading:
 *       {@code "::1:11211"}</li>
 * </ul>
 *
 * <p>This is a utility class and cannot be instantiated.</p>
 */
public final class AddrUtil {

    private static final String URL_SEPARATOR = "(?:\\s|,)+"; //NOSONAR

    private static final Splitter URL_SPLITTER = Splitter.pattern(URL_SEPARATOR).omitEmptyStrings().trimResults();

    private AddrUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Splits a string containing whitespace or comma separated host or IP addresses
     * and port numbers into a List of server strings without parsing or validating them.
     *
     * <p>Separators are one or more whitespace characters or commas. Returned strings are trimmed
     * of leading and trailing whitespace, and empty tokens are omitted from the result.</p>
     *
     * <p>The input string can be in any of the following formats:</p>
     * <ul>
     *   <li>Space-separated: {@code "host:port host2:port"}</li>
     *   <li>Comma-separated: {@code "host:port, host2:port"}</li>
     *   <li>Mixed separators: {@code "host:port, host2:port  host3:port"}</li>
     *   <li>IPv4 addresses: {@code "192.168.1.1:8080 192.168.1.2:8080"}</li>
     *   <li>IPv6 addresses: {@code "[::1]:11211, [fe80::1]:8080"}</li>
     * </ul>
     *
     * <p>Unlike {@link #getAddressList(String)}, this method does not validate the format
     * or parse the host and port components. It simply returns the individual server strings
     * after splitting and trimming.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> servers = AddrUtil.getServerList("server1:8080, server2:8080");
     * // Returns: ["server1:8080", "server2:8080"]
     * }</pre>
     *
     * @param servers the string containing server addresses separated by whitespace or commas
     * @return a non-empty list of trimmed server address strings
     * @throws IllegalArgumentException if {@code servers} is {@code null}, empty, or results in an empty list after
     *         splitting and trimming.
     */
    public static List<String> getServerList(final String servers) throws IllegalArgumentException {
        final List<String> serverList = URL_SPLITTER.split(servers);

        if (N.isEmpty(serverList)) {
            throw new IllegalArgumentException("Invalid serverUrl: " + servers);
        }

        return serverList;
    }

    /**
     * Parses a string containing whitespace or comma separated host or IP addresses
     * and port numbers into a List of {@link InetSocketAddress} instances.
     *
     * <p>Each address is split into host and port at the last colon. Bracketed IPv6 hosts of the form
     * {@code "[::1]:port"} are accepted; the brackets are stripped from the host. An unbracketed address holding
     * more than one colon is split only when that reading is the only possible one: if the whole token is itself a
     * valid IPv6 literal it is rejected as ambiguous ({@code "fe80::1:8080"}, {@code "::1:1121"}), and otherwise
     * the part before the last colon must itself be a valid IPv6 literal ({@code "::1:11211"} is accepted,
     * {@code "abc:def:1234"} is not). Brackets always remove the ambiguity.</p>
     *
     * <p>This method supports various address formats including:</p>
     * <ul>
     *   <li>Standard format: {@code "host:port host2:port"} or {@code "host:port, host2:port"}</li>
     *   <li>IPv4 addresses: {@code "192.168.1.1:8080"}</li>
     *   <li>IPv6, bracketed: {@code "[::1]:11211"}, {@code "[fe80::1]:8080"}</li>
     *   <li>IPv6, unbracketed and unambiguous: {@code "::1:11211"} (but {@code "::1:1121"} is rejected - it is
     *       itself a valid address)</li>
     *   <li>Hostnames: {@code "localhost:8080"} or {@code "example.com:443"}</li>
     * </ul>
     *
     * <p>The port number must be a valid integer in the range 0-65535. Each address must contain at least one colon
     * separating the host and port. Both the host part and port part must be non-empty.</p>
     *
     * <p><b>Name resolution:</b> each address is built with
     * {@link InetSocketAddress#InetSocketAddress(String, int)}, which attempts to resolve the host.
     * That call may block on DNS, and a host that cannot be resolved yields an <i>unresolved</i>
     * address rather than an exception - test {@link InetSocketAddress#isUnresolved()} if that matters
     * to the caller.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<InetSocketAddress> addrs = AddrUtil.getAddressList("localhost:11211, 192.168.1.100:11211");
     * // Can be used with network clients:
     * // MemcachedClient client = new MemcachedClient(addrs);
     * }</pre>
     *
     * @param servers the string containing server addresses to parse; must not be {@code null} or empty
     * @return a non-empty list of {@link InetSocketAddress} instances corresponding to the parsed addresses
     * @throws IllegalArgumentException if the servers string is {@code null}, empty, or contains invalid addresses
     *         (missing colon, an unbracketed multi-colon address that is itself a valid IPv6 literal or whose host
     *         part is not one, invalid port number, empty host or port, or port out of valid range 0-65535).
     * @see #getAddressList(Collection)
     * @see #getServerList(String)
     */
    public static List<InetSocketAddress> getAddressList(final String servers) throws IllegalArgumentException {
        if (Strings.isEmpty(servers)) {
            throw new IllegalArgumentException("Null or empty host list");
        }

        final List<String> addressStrings = URL_SPLITTER.split(servers);

        if (N.isEmpty(addressStrings)) {
            throw new IllegalArgumentException("Invalid addresses: " + servers);
        }

        final List<InetSocketAddress> addrs = new ArrayList<>();

        for (final String addressString : addressStrings) {
            addrs.add(parseHostPort(addressString, servers));
        }

        return addrs;
    }

    /**
     * Converts a collection of server address strings into a list of {@link InetSocketAddress} instances.
     *
     * <p>Each string in the collection should be in the format {@code "host:port"}. This method
     * parses each address string individually by finding the last colon to separate the host from
     * the port; an unbracketed address holding more than one colon is split only when that reading is the only
     * possible one, exactly as in {@link #getAddressList(String)}.</p>
     *
     * <p>Unlike {@link #getAddressList(String)}, this method does not perform any splitting of the
     * input strings. Each element in the collection is expected to be a complete, individual server
     * address in the format {@code "host:port"}.</p>
     *
     * <p>Supported address formats:</p>
     * <ul>
     *   <li>Hostnames: {@code "localhost:8080"}, {@code "example.com:443"}</li>
     *   <li>IPv4 addresses: {@code "192.168.1.1:8080"}</li>
     *   <li>IPv6, bracketed: {@code "[::1]:11211"}, {@code "[fe80::1]:8080"}</li>
     *   <li>IPv6, unbracketed and unambiguous: {@code "::1:11211"}</li>
     * </ul>
     *
     * <p>Each address must contain at least one colon separating the host and port.
     * The port must be a valid integer in the range 0-65535. Both host and port parts must be non-empty.
     * Surrounding whitespace is trimmed from each element, matching {@link #getAddressList(String)}.</p>
     *
     * <p><b>Name resolution:</b> each address is built with
     * {@link InetSocketAddress#InetSocketAddress(String, int)}, which attempts to resolve the host.
     * That call may block on DNS, and a host that cannot be resolved yields an <i>unresolved</i>
     * address rather than an exception - test {@link InetSocketAddress#isUnresolved()} if that matters
     * to the caller.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> serverStrings = Arrays.asList("127.0.0.1:8080", "192.168.1.1:8080");
     * List<InetSocketAddress> addresses = AddrUtil.getAddressList(serverStrings);
     * }</pre>
     *
     * @param servers a collection of server addresses where each string is in the format {@code "host:port"}; must not be {@code null}
     * @return a non-empty list of {@link InetSocketAddress} instances corresponding to the server addresses
     * @throws IllegalArgumentException if {@code servers} is {@code null}, any server address is invalid (missing
     *         colon, an unbracketed multi-colon address that is itself a valid IPv6 literal or whose host part is
     *         not one, invalid port number, empty host or port, or port out of valid range 0-65535), or if the
     *         collection results in an empty address list.
     * @see #getAddressList(String)
     * @see #getServerList(String)
     */
    public static List<InetSocketAddress> getAddressList(final Collection<String> servers) throws IllegalArgumentException {
        N.checkArgNotNull(servers, cs.servers);

        if (servers.isEmpty()) {
            throw new IllegalArgumentException("servers cannot be empty");
        }

        final List<InetSocketAddress> addrs = new ArrayList<>(servers.size());

        for (final String url : servers) {
            if (url == null) {
                throw new IllegalArgumentException("Invalid server 'null' in list: " + servers);
            }

            addrs.add(parseHostPort(url, servers));
        }

        return addrs;
    }

    /**
     * Creates an {@link InetSocketAddress} from a {@link URL} object by extracting its host and port information.
     *
     * <p>This method directly uses {@link URL#getHost()} and {@link URL#getPort()} to construct
     * the socket address. The host is extracted from the URL's authority component, and the port
     * is taken from the URL's explicit port (if specified) or the default port for the protocol.</p>
     *
     * <p>If the URL does not specify a port explicitly and {@link URL#getPort()} returns -1,
     * the method will attempt to use {@link URL#getDefaultPort()} to get the protocol's default port.
     * If neither an explicit nor a default port is available (i.e., the resolved port is outside
     * the valid range 0-65535), an {@link IllegalArgumentException} is thrown.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * URL url = new URL("http://localhost:8080/path");
     * InetSocketAddress addr = AddrUtil.getAddressFromUrl(url);
     * // Returns InetSocketAddress with host="localhost" and port=8080
     * }</pre>
     *
     * @param url a {@link URL} from which the host and port are to be extracted; must not be {@code null}
     * @return an {@link InetSocketAddress} instance corresponding to the host and port of the URL
     * @throws IllegalArgumentException if {@code url} is {@code null}
     *         or if the URL has no host or no usable port (resolved port is outside the range 0-65535).
     * @see #getAddressList(Collection)
     */
    public static InetSocketAddress getAddressFromUrl(final URL url) throws IllegalArgumentException {
        N.checkArgNotNull(url, cs.url);

        N.checkArgument(!N.isEmpty(url.getHost()), "URL must have a host: %s", url);
        return new InetSocketAddress(url.getHost(), getPort(url));
    }

    /**
     * Converts a collection of {@link URL} objects into a list of {@link InetSocketAddress} instances.
     *
     * <p>Each URL in the collection is converted by extracting its host and port information using
     * {@link URL#getHost()} and {@link URL#getPort()}. This method is useful for converting a list
     * of web service endpoints or server URLs into socket addresses for network communication.</p>
     *
     * <p>This method handles {@code null} or empty input collections gracefully by returning an
     * empty list. If a URL does not specify a port explicitly and {@link URL#getPort()} returns -1,
     * the method will attempt to use {@link URL#getDefaultPort()} to get the protocol's default port.
     * If neither an explicit nor a default port is available (i.e., the resolved port is outside
     * the valid range 0-65535), an {@link IllegalArgumentException} is thrown.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<URL> urls = Arrays.asList(
     *     new URL("http://localhost:8080"),
     *     new URL("http://127.0.0.1:9090")
     * );
     * List<InetSocketAddress> addresses = AddrUtil.getAddressListFromUrls(urls);
     * // Returns a list with two InetSocketAddress instances
     * }</pre>
     *
     * @param urls a collection of {@link URL} objects to be converted; may be {@code null} or empty
     * @return a list of {@link InetSocketAddress} instances corresponding to the URLs,
     *         or an empty list if the input collection is {@code null} or empty
     * @throws IllegalArgumentException if the collection contains a {@code null} URL, or any URL has no host
     *         or no usable port (resolved port is outside the range 0-65535).
     * @see #getAddressFromUrl(URL)
     */
    public static List<InetSocketAddress> getAddressListFromUrls(final Collection<URL> urls) throws IllegalArgumentException {
        if (N.isEmpty(urls)) {
            return new ArrayList<>();
        }

        final List<InetSocketAddress> addrs = new ArrayList<>(urls.size());

        for (final URL url : urls) {
            addrs.add(getAddressFromUrl(url));
        }

        return addrs;
    }

    /**
     * Splits {@code host:port}, including bracketed IPv6 {@code [host]:port}.
     *
     * <p>An unbracketed token that holds more than one {@code ':'} is split at the last one only when that
     * reading is the <i>only</i> possible one:</p>
     * <ul>
     *   <li>if the whole token is itself a valid IPv6 literal it is ambiguous - {@code fe80::1:8080} is both the
     *       address {@code fe80::1:8080} and the endpoint {@code fe80::1} port 8080 - and is rejected;</li>
     *   <li>otherwise the part before the last {@code ':'} must itself be a valid IPv6 literal, so
     *       {@code ::1:11211} is the endpoint {@code ::1} port 11211 (no IPv6 group can hold five hex digits,
     *       so the whole token cannot be an address) while {@code abc:def:1234} is rejected outright.</li>
     * </ul>
     *
     * <p>A plain "more than one colon" count was tried and reverted: it also rejected the unambiguous
     * {@code ::1:11211}, the form this class has always accepted (see {@code MappedIpv6Test} and
     * {@code MultiClassRegressionGTest.ipv6FormsThatWereAcceptedStillAre}). The literal test below is purely
     * syntactic - unlike an earlier {@link java.net.InetAddress#getByName(String)} version, it never reaches the
     * platform name service.</p>
     * @throws NullPointerException if {@code rawAddressString} is {@code null}
     * @throws IllegalArgumentException if the address has no host or port, has malformed or ambiguous IPv6 brackets
     *         or syntax, or has a nonnumeric port or a port outside {@code [0, 65535]}
     */
    private static InetSocketAddress parseHostPort(final String rawAddressString, final Object listForError)
            throws NullPointerException, IllegalArgumentException {
        // Trim here rather than only in the String overload's splitter: getAddressList(Collection) does
        // no splitting, so without this " host:80 " parsed from a String succeeded while the same text
        // in a Collection failed with "Invalid port number '80 '".
        final String addressString = rawAddressString.trim();
        final int lastColonIndex = addressString.lastIndexOf(':');

        if (lastColonIndex == -1) {
            throw new IllegalArgumentException("Invalid server '" + addressString + "' in list: " + listForError);
        }

        String hostPart = addressString.substring(0, lastColonIndex);
        final String portNum = addressString.substring(lastColonIndex + 1);

        if (addressString.startsWith("[") && addressString.endsWith("]")) {
            // The whole token is a bracketed literal, so the last ':' is inside the brackets and the
            // split above produced nonsense. Report the real fault (no port) rather than the misleading
            // "no matching closing ']'" this used to produce for input such as "[::1]".
            throw new IllegalArgumentException(
                    "Invalid server '" + addressString + "' in list: " + listForError + ". Bracketed IPv6 host has no port; use [host]:port");
        }

        if (hostPart.startsWith("[")) {
            if (!hostPart.endsWith("]")) {
                throw new IllegalArgumentException(
                        "Invalid server '" + addressString + "' in list: " + listForError + ". IPv6 host has opening '[' but no matching closing ']'");
            }
            hostPart = hostPart.substring(1, hostPart.length() - 1);
        } else if (hostPart.endsWith("]")) {
            throw new IllegalArgumentException(
                    "Invalid server '" + addressString + "' in list: " + listForError + ". IPv6 host has closing ']' but no matching opening '['");
        } else if (hostPart.indexOf(':') >= 0) {
            if (isIPv6Literal(addressString)) {
                throw new IllegalArgumentException("Invalid server '" + addressString + "' in list: " + listForError
                        + ". Unbracketed IPv6 address is ambiguous without an explicit port; use [host]:port");
            }

            if (!isIPv6Literal(hostPart)) {
                throw new IllegalArgumentException("Invalid server '" + addressString + "' in list: " + listForError + ". Host '" + hostPart
                        + "' contains ':' but is not a valid IPv6 address; use [host]:port");
            }
        }

        if (hostPart.isEmpty() || portNum.isEmpty()) {
            throw new IllegalArgumentException("Invalid server '" + addressString + "' in list: " + listForError);
        }

        final int port;
        try {
            port = Integer.parseInt(portNum);
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException("Invalid port number '" + portNum + "' in server: " + addressString, e);
        }

        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid port number '" + portNum + "' in server: " + addressString + ". Expected range: 0-65535");
        }

        return new InetSocketAddress(hostPart, port);
    }

    /**
     * Returns {@code true} when {@code token} is syntactically a valid IPv6 address literal, optionally followed
     * by a {@code %zone} scope id (RFC 4007). The test is purely textual: it never resolves a name and never
     * touches the platform name service, so it cannot block and cannot depend on what DNS happens to answer.
     *
     * <p>Accepted: eight groups of one to four hexadecimal digits, at most one {@code "::"} run standing for one
     * or more all-zero groups (RFC 4291 section 2.2, so {@code 1:2:3:4:5:6:7::} is a literal), and an optional
     * dotted-quad IPv4 tail occupying the last two groups
     * ({@code ::ffff:192.0.2.1}). Anything else - a group of five hex digits such as the {@code 11211} in
     * {@code ::1:11211}, a second {@code "::"}, a stray leading or trailing single {@code ':'} - is not a
     * literal.</p>
     *
     * <p>This is RFC 4291 grammar, which is very slightly stricter than
     * {@link java.net.InetAddress#getByName(String)} on this JVM in two obscure spots: a group written with more
     * than four digits ({@code ::00000}) and a dotted-quad octet with a leading zero ({@code ::01.2.3.4}) are not
     * literals here. Both make the token fall through to the "is the host part a literal" test, which rejects
     * them - the parse never silently mis-splits because of the difference. The one-to-four-digit rule is also
     * exactly what makes {@code ::1:11211} unambiguous, so relaxing it would defeat the point. Conversely a
     * {@code %zone} suffix is judged by shape alone, so an interface name that does not exist on this host no
     * longer changes how the address parses.</p>
     *
     * @param token the candidate text; may be empty
     * @return {@code true} if {@code token} is an IPv6 address literal
     */
    private static boolean isIPv6Literal(final String token) {
        final int zoneIndex = token.indexOf('%');

        if (zoneIndex >= 0 && (zoneIndex == 0 || zoneIndex == token.length() - 1)) {
            return false;
        }

        // A zone id cannot contain ':'. If it did, the suffix is a port (or other trailing token),
        // not a scope — otherwise fe80::1%eth0:8080 would be judged a complete literal and rejected
        // as ambiguous even though the only legal split is host fe80::1%eth0 port 8080.
        if (zoneIndex >= 0 && token.indexOf(':', zoneIndex + 1) >= 0) {
            return false;
        }

        final String address = zoneIndex < 0 ? token : token.substring(0, zoneIndex);

        if (address.indexOf(':') < 0) {
            return false;
        }

        final int compression = address.indexOf("::");

        if (compression < 0) {
            return countIPv6Groups(address, true) == 8;
        }

        if (address.indexOf("::", compression + 2) >= 0) {
            // A second "::" makes the number of elided groups undecidable.
            return false;
        }

        final String head = address.substring(0, compression);
        final String tail = address.substring(compression + 2);

        if (head.endsWith(":") || tail.startsWith(":")) {
            return false;
        }

        // The embedded IPv4 form is only legal as the very last element, so it can never appear in the head.
        final int headGroups = countIPv6Groups(head, false);
        final int tailGroups = countIPv6Groups(tail, true);

        // "::" must stand for at least one group, so the written groups can never fill all eight.
        return headGroups >= 0 && tailGroups >= 0 && headGroups + tailGroups < 8;
    }

    /**
     * Counts the 16-bit groups written by a colon-separated run of an IPv6 literal.
     *
     * @param run the run to count; empty contributes no group
     * @param ipv4TailAllowed whether the last element may be a dotted-quad IPv4 address, which contributes two groups
     * @return the number of groups, or {@code -1} if the run is not well formed
     */
    private static int countIPv6Groups(final String run, final boolean ipv4TailAllowed) {
        if (run.isEmpty()) {
            return 0;
        }

        int groups = 0;
        int start = 0;

        while (true) {
            final int end = run.indexOf(':', start);
            final boolean isLast = end < 0;
            final String part = isLast ? run.substring(start) : run.substring(start, end);

            if (isLast && ipv4TailAllowed && part.indexOf('.') >= 0) {
                if (!isIPv4Literal(part)) {
                    return -1;
                }

                groups += 2;
            } else {
                if (part.isEmpty() || part.length() > 4) {
                    return -1;
                }

                for (int i = 0, len = part.length(); i < len; i++) {
                    final char ch = part.charAt(i);

                    // Explicit ASCII test: Character.digit(ch, 16) also accepts non-ASCII digits such as U+0660.
                    if ((ch < '0' || ch > '9') && (ch < 'a' || ch > 'f') && (ch < 'A' || ch > 'F')) {
                        return -1;
                    }
                }

                groups++;
            }

            if (isLast) {
                return groups;
            }

            start = end + 1;
        }
    }

    /**
     * Returns {@code true} when {@code token} is a dotted-quad IPv4 literal: four decimal octets of one to three
     * digits, no leading zero and none above 255 - the RFC 4291 dotted-quad tail of an IPv6 literal, decided
     * textually without resolving anything.
     *
     * @param token the candidate text
     * @return {@code true} if {@code token} is an IPv4 address literal
     */
    private static boolean isIPv4Literal(final String token) {
        int octets = 0;
        int start = 0;

        while (true) {
            final int end = token.indexOf('.', start);
            final boolean isLast = end < 0;
            final String part = isLast ? token.substring(start) : token.substring(start, end);
            final int len = part.length();

            if (len < 1 || len > 3 || (len > 1 && part.charAt(0) == '0')) {
                return false;
            }

            int value = 0;

            for (int i = 0; i < len; i++) {
                final char ch = part.charAt(i);

                if (ch < '0' || ch > '9') {
                    return false;
                }

                value = value * 10 + (ch - '0');
            }

            if (value > 255) {
                return false;
            }

            octets++;

            if (isLast) {
                return octets == 4;
            }

            if (octets == 4) {
                // A fifth element follows the fourth octet.
                return false;
            }

            start = end + 1;
        }
    }

    /**
     * Extracts the port number from a URL, falling back to the default port if not explicitly specified.
     *
     * @param url the URL from which to extract the port
     * @return the explicit port if specified, or the default port for the URL's protocol
     * @throws IllegalArgumentException if neither an explicit port nor a default port is available (i.e., the
     *         resolved port is not in the range 0-65535).
     */
    private static int getPort(final URL url) throws IllegalArgumentException {
        int port = url.getPort();

        if (port == -1) {
            port = url.getDefaultPort();
        }

        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("URL has no usable port: " + url);
        }

        return port;
    }
}
