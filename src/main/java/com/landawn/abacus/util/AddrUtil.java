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
 * Provides methods to convert various address formats (strings, URLs) into
 * InetSocketAddress instances suitable for network operations.
 * 
 * <p>This class supports parsing of multiple address formats including:</p>
 * <ul>
 *   <li>Space-separated addresses: "host1:port1 host2:port2"</li>
 *   <li>Comma-separated addresses: "host1:port1, host2:port2"</li>
 *   <li>IPv6 addresses with colons: "::1:11211"</li>
 * </ul>
 * 
 * @since 1.0
 */
public final class AddrUtil {

    private static final String URL_SEPARATOR = "(?:\\s|,)+"; //NOSONAR

    private static final Splitter URL_SPLITTER = Splitter.pattern(URL_SEPARATOR).omitEmptyStrings().trimResults();

    private AddrUtil() {
        // singleton
    }

    /**
     * Splits a string containing whitespace or comma separated host or IP addresses
     * and port numbers into a List of server strings without parsing or validating them.
     *
     * <p>This method performs basic string splitting and trimming operations using a regex pattern
     * that matches one or more whitespace characters or commas as separators. The returned strings
     * are trimmed of leading and trailing whitespace, and empty strings are omitted from the result.</p>
     *
     * <p>The input string can be in any of the following formats:</p>
     * <ul>
     *   <li>Space-separated: {@code "host:port host2:port"}</li>
     *   <li>Comma-separated: {@code "host:port, host2:port"}</li>
     *   <li>Mixed separators: {@code "host:port, host2:port  host3:port"}</li>
     *   <li>IPv4 addresses: {@code "192.168.1.1:8080 192.168.1.2:8080"}</li>
     *   <li>IPv6 addresses: {@code "::1:11211, [::1]:11211"}</li>
     * </ul>
     *
     * <p>Unlike {@link #getAddressList(String)}, this method does not validate the format
     * or parse the host and port components. It simply returns the individual server strings
     * after splitting and trimming.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * List<String> servers = AddrUtil.getServerList("server1:8080, server2:8080");
     * // Returns: ["server1:8080", "server2:8080"]
     * }</pre>
     *
     * @param servers the string containing server addresses separated by whitespace or commas; must not be null or result in empty list after splitting
     * @return a non-empty list of trimmed server address strings
     * @throws IllegalArgumentException if the servers string is null, or results in an empty list after splitting and trimming
     */
    public static List<String> getServerList(final String servers) {
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
     * <p>This method first splits the input string using whitespace or comma separators,
     * then parses each address string to extract the host and port. The parsing algorithm
     * finds the last colon in each address string to separate the host from the port, which
     * correctly handles IPv6 addresses that contain multiple colons.</p>
     *
     * <p>This method supports various address formats including:</p>
     * <ul>
     *   <li>Standard format: {@code "host:port host2:port"} or {@code "host:port, host2:port"}</li>
     *   <li>IPv4 addresses: {@code "192.168.1.1:8080"}</li>
     *   <li>IPv6 addresses: {@code "::1:11211"} or {@code "fe80::1:8080"} (colon-delimited IPv6 is supported)</li>
     *   <li>Hostnames: {@code "localhost:8080"} or {@code "example.com:443"}</li>
     * </ul>
     *
     * <p>The port number must be a valid integer. Each address must contain at least one colon
     * separating the host and port. Both the host part and port part must be non-empty.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * List<InetSocketAddress> addrs = AddrUtil.getAddressList("localhost:11211, 192.168.1.100:11211");
     * // Can be used with network clients:
     * // MemcachedClient client = new MemcachedClient(addrs);
     * }</pre>
     *
     * @param servers the string containing server addresses to parse; must not be null or empty
     * @return a non-empty list of {@link InetSocketAddress} instances corresponding to the parsed addresses
     * @throws IllegalArgumentException if the servers string is null, empty, or contains invalid addresses (missing colon, invalid port number, empty host or port)
     */
    public static List<InetSocketAddress> getAddressList(final String servers) {
        if (Strings.isEmpty(servers)) {
            throw new IllegalArgumentException("Null or empty host list");
        }

        final List<String> hoststuffs = URL_SPLITTER.split(servers);

        if (N.isEmpty(hoststuffs)) {
            throw new IllegalArgumentException("Invalid addresses: " + servers);
        }

        final List<InetSocketAddress> addrs = new ArrayList<>();

        for (final String hoststuff : hoststuffs) {
            if (hoststuff.isEmpty()) {
                continue;
            }

            // Handle IPv6 addresses properly
            final int lastColonIndex = hoststuff.lastIndexOf(':');

            if (lastColonIndex == -1) {
                throw new IllegalArgumentException("Invalid server '" + hoststuff + "' in list:  " + servers);
            }

            final String hostPart = hoststuff.substring(0, lastColonIndex);
            final String portNum = hoststuff.substring(lastColonIndex + 1);

            if (hostPart.isEmpty() || portNum.isEmpty()) {
                throw new IllegalArgumentException("Invalid server '" + hoststuff + "' in list:  " + servers);
            }

            try {
                final int port = Integer.parseInt(portNum);
                addrs.add(new InetSocketAddress(hostPart, port));
            } catch (final NumberFormatException e) {
                throw new IllegalArgumentException("Invalid port number '" + portNum + "' in server: " + hoststuff, e);
            }
        }
        assert !addrs.isEmpty() : "No addrs found";

        return addrs;
    }

    /**
     * Converts a collection of server address strings into a list of {@link InetSocketAddress} instances.
     *
     * <p>Each string in the collection should be in the format {@code "host:port"}. This method
     * parses each address string individually by finding the last colon to separate the host from
     * the port, which correctly handles IPv6 addresses that contain multiple colons.</p>
     *
     * <p>Unlike {@link #getAddressList(String)}, this method does not perform any splitting of the
     * input strings. Each element in the collection is expected to be a complete, individual server
     * address in the format {@code "host:port"}.</p>
     *
     * <p>Supported address formats:</p>
     * <ul>
     *   <li>Hostnames: {@code "localhost:8080"}, {@code "example.com:443"}</li>
     *   <li>IPv4 addresses: {@code "192.168.1.1:8080"}</li>
     *   <li>IPv6 addresses: {@code "::1:11211"}, {@code "fe80::1:8080"}</li>
     * </ul>
     *
     * <p>Each address must contain at least one colon separating the host and port.
     * The port must be a valid integer. Both host and port parts must be non-empty.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * List<String> serverStrings = Arrays.asList("server1:8080", "server2:8080");
     * List<InetSocketAddress> addresses = AddrUtil.getAddressList(serverStrings);
     * }</pre>
     *
     * @param servers a collection of server addresses where each string is in the format {@code "host:port"}; must not be null or empty
     * @return a non-empty list of {@link InetSocketAddress} instances corresponding to the server addresses
     * @throws IllegalArgumentException if any server address is invalid (missing colon, invalid port number, empty host or port) or if the collection results in an empty address list
     */
    public static List<InetSocketAddress> getAddressList(final Collection<String> servers) {
        final List<InetSocketAddress> addrs = new ArrayList<>(servers.size());

        for (final String url : servers) {
            // Handle IPv6 addresses properly
            final int lastColonIndex = url.lastIndexOf(':');

            if (lastColonIndex == -1) {
                throw new IllegalArgumentException("Invalid server '" + url + "' in list:  " + servers);
            }

            final String hostPart = url.substring(0, lastColonIndex);
            final String portNum = url.substring(lastColonIndex + 1);

            if (hostPart.isEmpty() || portNum.isEmpty()) {
                throw new IllegalArgumentException("Invalid server '" + url + "' in list:  " + servers);
            }

            try {
                final int port = Integer.parseInt(portNum);
                addrs.add(new InetSocketAddress(hostPart, port));
            } catch (final NumberFormatException e) {
                throw new IllegalArgumentException("Invalid port number '" + portNum + "' in server: " + url, e);
            }
        }

        if (addrs.isEmpty()) {
            throw new IllegalArgumentException("servers cannot be empty");
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
     * the resulting {@link InetSocketAddress} will be created with port -1, which typically
     * needs to be handled by the calling code.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * URL url = new URL("http://example.com:8080/path");
     * InetSocketAddress addr = AddrUtil.getAddressFromURL(url);
     * // Returns InetSocketAddress with host="example.com" and port=8080
     * }</pre>
     *
     * @param url a {@link URL} from which the host and port are to be extracted; must not be {@code null}
     * @return an {@link InetSocketAddress} instance corresponding to the host and port of the URL
     */
    public static InetSocketAddress getAddressFromURL(final URL url) {
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
     * the resulting {@link InetSocketAddress} will be created with port -1.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * List<URL> urls = Arrays.asList(
     *     new URL("http://server1.com:8080"),
     *     new URL("http://server2.com:9090")
     * );
     * List<InetSocketAddress> addresses = AddrUtil.getAddressListFromURL(urls);
     * // Returns a list with two InetSocketAddress instances
     * }</pre>
     *
     * @param urls a collection of {@link URL} objects to be converted; may be {@code null} or empty
     * @return a list of {@link InetSocketAddress} instances corresponding to the URLs,
     *         or an empty list if the input collection is {@code null} or empty
     */
    public static List<InetSocketAddress> getAddressListFromURL(final Collection<URL> urls) {
        if (N.isEmpty(urls)) {
            return new ArrayList<>();
        }

        final List<InetSocketAddress> addrs = new ArrayList<>(urls.size());

        for (final URL url : urls) {
            addrs.add(new InetSocketAddress(url.getHost(), getPort(url)));
        }

        return addrs;
    }

    private static int getPort(final URL url) {
        int port = url.getPort();

        if (port == -1) {
            port = url.getDefaultPort();
        }

        return port;
    }
}