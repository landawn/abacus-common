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
     * and port numbers into a List of server strings.
     * 
     * <p>The input string can be in formats like:</p>
     * <ul>
     *   <li>"host:port host2:port"</li>
     *   <li>"host:port, host2:port"</li>
     *   <li>"192.168.1.1:8080 192.168.1.2:8080"</li>
     * </ul>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * List<String> servers = AddrUtil.getServerList("server1:8080, server2:8080");
     * // Returns: ["server1:8080", "server2:8080"]
     * }</pre>
     *
     * @param servers the string containing server addresses
     * @return a list of server address strings
     * @throws IllegalArgumentException if the servers string is invalid or empty
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
     * and port numbers into a List of InetSocketAddress instances.
     * 
     * <p>This method supports various address formats including:</p>
     * <ul>
     *   <li>Standard format: "host:port host2:port" or "host:port, host2:port"</li>
     *   <li>IPv4 addresses: "192.168.1.1:8080"</li>
     *   <li>IPv6 addresses: "::1:11211" (colon-delimited IPv6 is supported)</li>
     * </ul>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * List<InetSocketAddress> addrs = AddrUtil.getAddressList("localhost:11211, 192.168.1.100:11211");
     * // Can be used with MemcachedClient:
     * // MemcachedClient client = new MemcachedClient(addrs);
     * }</pre>
     *
     * @param servers the string containing server addresses to parse
     * @return a list of InetSocketAddress instances
     * @throws IllegalArgumentException if the servers string is null, empty, or contains invalid addresses
     */
    public static List<InetSocketAddress> getAddressList(final String servers) {
        if (Strings.isEmpty(servers)) {
            throw new IllegalArgumentException("Null or empty host list");
        }

        final String[] hoststuffs = servers.split(URL_SEPARATOR); // NOSONAR

        if (N.isEmpty(hoststuffs)) {
            throw new IllegalArgumentException("Invalid addresses: " + servers);
        }

        final List<InetSocketAddress> addrs = new ArrayList<>();

        for (final String hoststuff : hoststuffs) {
            if (hoststuff.isEmpty()) {
                continue;
            }

            final String[] strs = hoststuff.split(":");

            if (strs.length < 1) {
                throw new IllegalArgumentException("Invalid server '" + hoststuff + "' in list:  " + servers);
            }

            final String hostPart = strs[0];
            final String portNum = strs[1];

            addrs.add(new InetSocketAddress(hostPart, Integer.parseInt(portNum)));
        }
        assert !addrs.isEmpty() : "No addrs found";

        return addrs;
    }

    /**
     * Converts a collection of server address strings into a list of InetSocketAddress instances.
     * Each string in the collection should be in the format "host:port".
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * List<String> serverStrings = Arrays.asList("server1:8080", "server2:8080");
     * List<InetSocketAddress> addresses = AddrUtil.getAddressList(serverStrings);
     * }</pre>
     *
     * @param servers a collection of server addresses in the format "host:port"
     * @return a list of InetSocketAddress instances corresponding to the server addresses
     * @throws IllegalArgumentException if a server address is invalid or if the collection is empty
     */
    public static List<InetSocketAddress> getAddressList(final Collection<String> servers) {
        final List<InetSocketAddress> addrs = new ArrayList<>(servers.size());

        for (final String url : servers) {
            final String[] strs = url.split(":");

            if (strs.length < 1) {
                throw new IllegalArgumentException("Invalid server '" + url + "' in list:  " + servers);
            }

            final String hostPart = strs[0];
            final String portNum = strs[1];

            addrs.add(new InetSocketAddress(hostPart, Integer.parseInt(portNum)));
        }

        if (addrs.isEmpty()) {
            throw new IllegalArgumentException("servers cannot be empty");
        }

        return addrs;
    }

    /**
     * Creates an InetSocketAddress from a URL object using its host and port information.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * URL url = new URL("http://example.com:8080/path");
     * InetSocketAddress addr = AddrUtil.getAddressFromURL(url);
     * // Returns InetSocketAddress with host="example.com" and port=8080
     * }</pre>
     *
     * @param url a URL from which the host and port are to be extracted
     * @return an InetSocketAddress instance corresponding to the host and port of the URL
     * @throws IllegalArgumentException if the URL is {@code null}
     */
    public static InetSocketAddress getAddressFromURL(final URL url) {
        return new InetSocketAddress(url.getHost(), url.getPort());
    }

    /**
     * Converts a collection of URL objects into a list of InetSocketAddress instances.
     * Each URL is converted using its host and port information.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * List<URL> urls = Arrays.asList(
     *     new URL("http://server1.com:8080"),
     *     new URL("http://server2.com:9090")
     * );
     * List<InetSocketAddress> addresses = AddrUtil.getAddressListFromURL(urls);
     * }</pre>
     *
     * @param urls a collection of URLs to be converted
     * @return a list of InetSocketAddress instances corresponding to the URLs,
     *         or an empty list if the input collection is null or empty
     */
    public static List<InetSocketAddress> getAddressListFromURL(final Collection<URL> urls) {
        if (N.isEmpty(urls)) {
            return new ArrayList<>();
        }

        final List<InetSocketAddress> addrs = new ArrayList<>(urls.size());

        for (final URL server : urls) {
            addrs.add(new InetSocketAddress(server.getHost(), server.getPort()));
        }

        return addrs;
    }
}