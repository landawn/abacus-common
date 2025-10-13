package com.landawn.abacus.util;

import static org.junit.Assert.assertThrows;

import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class AddrUtil2025Test extends TestBase {

    @Test
    public void testGetServerList_SpaceSeparated() {
        List<String> servers = AddrUtil.getServerList("server1:8080 server2:8081");
        Assertions.assertEquals(2, servers.size());
        Assertions.assertEquals("server1:8080", servers.get(0));
        Assertions.assertEquals("server2:8081", servers.get(1));
    }

    @Test
    public void testGetServerList_CommaSeparated() {
        List<String> servers = AddrUtil.getServerList("server1:8080, server2:8081, server3:8082");
        Assertions.assertEquals(3, servers.size());
        Assertions.assertEquals("server1:8080", servers.get(0));
        Assertions.assertEquals("server2:8081", servers.get(1));
        Assertions.assertEquals("server3:8082", servers.get(2));
    }

    @Test
    public void testGetServerList_MixedSeparators() {
        List<String> servers = AddrUtil.getServerList("server1:8080  ,  server2:8081    server3:8082");
        Assertions.assertEquals(3, servers.size());
        Assertions.assertEquals("server1:8080", servers.get(0));
        Assertions.assertEquals("server2:8081", servers.get(1));
        Assertions.assertEquals("server3:8082", servers.get(2));
    }

    @Test
    public void testGetServerList_IPv4Addresses() {
        List<String> servers = AddrUtil.getServerList("192.168.1.1:8080 192.168.1.2:8080");
        Assertions.assertEquals(2, servers.size());
        Assertions.assertEquals("192.168.1.1:8080", servers.get(0));
        Assertions.assertEquals("192.168.1.2:8080", servers.get(1));
    }

    @Test
    public void testGetServerList_IPv6Addresses() {
        List<String> servers = AddrUtil.getServerList("::1:11211, fe80::1:8080");
        Assertions.assertEquals(2, servers.size());
        Assertions.assertEquals("::1:11211", servers.get(0));
        Assertions.assertEquals("fe80::1:8080", servers.get(1));
    }

    @Test
    public void testGetServerList_SingleServer() {
        List<String> servers = AddrUtil.getServerList("localhost:9090");
        Assertions.assertEquals(1, servers.size());
        Assertions.assertEquals("localhost:9090", servers.get(0));
    }

    @Test
    public void testGetServerList_WithExtraWhitespace() {
        List<String> servers = AddrUtil.getServerList("  server1:8080   ,   server2:8081  ");
        Assertions.assertEquals(2, servers.size());
        Assertions.assertEquals("server1:8080", servers.get(0));
        Assertions.assertEquals("server2:8081", servers.get(1));
    }

    @Test
    public void testGetServerList_EmptyString() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            AddrUtil.getServerList("");
        });
    }

    @Test
    public void testGetServerList_OnlyWhitespace() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            AddrUtil.getServerList("   ");
        });
    }

    @Test
    public void testGetServerList_OnlyCommas() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            AddrUtil.getServerList(",,,");
        });
    }

    @Test
    public void testGetAddressList_String_Basic() {
        List<InetSocketAddress> addrs = AddrUtil.getAddressList("localhost:8080, 192.168.1.1:9090");
        Assertions.assertEquals(2, addrs.size());
        Assertions.assertEquals("localhost", addrs.get(0).getHostString());
        Assertions.assertEquals(8080, addrs.get(0).getPort());
        Assertions.assertEquals("192.168.1.1", addrs.get(1).getHostString());
        Assertions.assertEquals(9090, addrs.get(1).getPort());
    }

    @Test
    public void testGetAddressList_String_SpaceSeparated() {
        List<InetSocketAddress> addrs = AddrUtil.getAddressList("host1:1111 host2:2222");
        Assertions.assertEquals(2, addrs.size());
        Assertions.assertEquals(1111, addrs.get(0).getPort());
        Assertions.assertEquals(2222, addrs.get(1).getPort());
    }

    @Test
    public void testGetAddressList_String_IPv6() {
        List<InetSocketAddress> addrs = AddrUtil.getAddressList("::1:11211");
        Assertions.assertEquals(1, addrs.size());
        Assertions.assertEquals("0:0:0:0:0:0:0:1", addrs.get(0).getHostString());
        Assertions.assertEquals(11211, addrs.get(0).getPort());
    }

    @Test
    public void testGetAddressList_String_IPv6Multiple() {
        List<InetSocketAddress> addrs = AddrUtil.getAddressList("::1:11211, fe80::1:8080");
        Assertions.assertEquals(2, addrs.size());
        Assertions.assertEquals("0:0:0:0:0:0:0:1", addrs.get(0).getHostString());
        Assertions.assertEquals(11211, addrs.get(0).getPort());
        Assertions.assertEquals("fe80:0:0:0:0:0:0:1", addrs.get(1).getHostString());
        Assertions.assertEquals(8080, addrs.get(1).getPort());
    }

    @Test
    public void testGetAddressList_String_IPv4() {
        List<InetSocketAddress> addrs = AddrUtil.getAddressList("192.168.1.1:8080");
        Assertions.assertEquals(1, addrs.size());
        Assertions.assertEquals("192.168.1.1", addrs.get(0).getHostString());
        Assertions.assertEquals(8080, addrs.get(0).getPort());
    }

    @Test
    public void testGetAddressList_String_MixedFormats() {
        List<InetSocketAddress> addrs = AddrUtil.getAddressList("localhost:8080, 192.168.1.1:9090, ::1:11211");
        Assertions.assertEquals(3, addrs.size());
    }

    @Test
    public void testGetAddressList_String_WithWhitespace() {
        List<InetSocketAddress> addrs = AddrUtil.getAddressList("  host1:1234  ,  host2:5678  ");
        Assertions.assertEquals(2, addrs.size());
        Assertions.assertEquals(1234, addrs.get(0).getPort());
        Assertions.assertEquals(5678, addrs.get(1).getPort());
    }

    @Test
    public void testGetAddressList_String_EmptyString() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            AddrUtil.getAddressList("");
        });
    }

    @Test
    public void testGetAddressList_String_NullString() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            AddrUtil.getAddressList((String) null);
        });
    }

    @Test
    public void testGetAddressList_String_NoColon() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            AddrUtil.getAddressList("localhost8080");
        });
    }

    @Test
    public void testGetAddressList_String_EmptyHost() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            AddrUtil.getAddressList(":8080");
        });
    }

    @Test
    public void testGetAddressList_String_EmptyPort() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            AddrUtil.getAddressList("localhost:");
        });
    }

    @Test
    public void testGetAddressList_String_InvalidPort() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            AddrUtil.getAddressList("localhost:abc");
        });
    }

    @Test
    public void testGetAddressList_String_InvalidPortFormat() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            AddrUtil.getAddressList("localhost:80.5");
        });
    }

    @Test
    public void testGetAddressList_String_MultipleInvalidWithOneValid() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            AddrUtil.getAddressList("localhost:8080, invalidserver");
        });
    }

    @Test
    public void testGetAddressList_Collection_Basic() {
        List<String> servers = Arrays.asList("server1:8080", "server2:9090");
        List<InetSocketAddress> addrs = AddrUtil.getAddressList(servers);
        Assertions.assertEquals(2, addrs.size());
        Assertions.assertEquals(8080, addrs.get(0).getPort());
        Assertions.assertEquals(9090, addrs.get(1).getPort());
    }

    @Test
    public void testGetAddressList_Collection_IPv4() {
        Collection<String> servers = Arrays.asList("192.168.1.1:8080", "10.0.0.1:9090");
        List<InetSocketAddress> addrs = AddrUtil.getAddressList(servers);
        Assertions.assertEquals(2, addrs.size());
        Assertions.assertEquals("192.168.1.1", addrs.get(0).getHostString());
        Assertions.assertEquals("10.0.0.1", addrs.get(1).getHostString());
    }

    @Test
    public void testGetAddressList_Collection_IPv6() {
        Collection<String> servers = Arrays.asList("::1:11211", "fe80::1:8080");
        List<InetSocketAddress> addrs = AddrUtil.getAddressList(servers);
        Assertions.assertEquals(2, addrs.size());
        Assertions.assertEquals("0:0:0:0:0:0:0:1", addrs.get(0).getHostString());
        Assertions.assertEquals(11211, addrs.get(0).getPort());
        Assertions.assertEquals("fe80:0:0:0:0:0:0:1", addrs.get(1).getHostString());
        Assertions.assertEquals(8080, addrs.get(1).getPort());
    }

    @Test
    public void testGetAddressList_Collection_SingleElement() {
        Collection<String> servers = Arrays.asList("localhost:3000");
        List<InetSocketAddress> addrs = AddrUtil.getAddressList(servers);
        Assertions.assertEquals(1, addrs.size());
        Assertions.assertEquals(3000, addrs.get(0).getPort());
    }

    @Test
    public void testGetAddressList_Collection_MixedFormats() {
        Collection<String> servers = Arrays.asList("localhost:8080", "192.168.1.1:9090", "::1:11211");
        List<InetSocketAddress> addrs = AddrUtil.getAddressList(servers);
        Assertions.assertEquals(3, addrs.size());
    }

    @Test
    public void testGetAddressList_Collection_EmptyCollection() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            AddrUtil.getAddressList(new ArrayList<String>());
        });
    }

    @Test
    public void testGetAddressList_Collection_NoColon() {
        Collection<String> servers = Arrays.asList("localhost8080");
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            AddrUtil.getAddressList(servers);
        });
    }

    @Test
    public void testGetAddressList_Collection_EmptyHost() {
        Collection<String> servers = Arrays.asList(":8080");
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            AddrUtil.getAddressList(servers);
        });
    }

    @Test
    public void testGetAddressList_Collection_EmptyPort() {
        Collection<String> servers = Arrays.asList("localhost:");
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            AddrUtil.getAddressList(servers);
        });
    }

    @Test
    public void testGetAddressList_Collection_InvalidPort() {
        Collection<String> servers = Arrays.asList("localhost:invalid");
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            AddrUtil.getAddressList(servers);
        });
    }

    @Test
    public void testGetAddressList_Collection_NegativePort() {
        Collection<String> servers = Arrays.asList("localhost:-8080");
        assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList(servers));
    }

    @Test
    public void testGetAddressList_Collection_LargePort() {
        Collection<String> servers = Arrays.asList("localhost:65535");
        List<InetSocketAddress> addrs = AddrUtil.getAddressList(servers);
        Assertions.assertEquals(1, addrs.size());
        Assertions.assertEquals(65535, addrs.get(0).getPort());
    }

    @Test
    public void testGetAddressFromURL_Basic() throws Exception {
        URL url = new URL("http://example.com:8080/path");
        InetSocketAddress addr = AddrUtil.getAddressFromURL(url);
        Assertions.assertEquals("example.com", addr.getHostName());
        Assertions.assertEquals(8080, addr.getPort());
    }

    @Test
    public void testGetAddressFromURL_WithoutPort() throws Exception {
        URL url = new URL("https://example.com/path");
        InetSocketAddress addr = AddrUtil.getAddressFromURL(url);
        Assertions.assertEquals("example.com", addr.getHostName());
        Assertions.assertEquals(443, addr.getPort());
    }

    @Test
    public void testGetAddressFromURL_HTTPS() throws Exception {
        URL url = new URL("https://secure.example.com:443/api");
        InetSocketAddress addr = AddrUtil.getAddressFromURL(url);
        Assertions.assertEquals("secure.example.com", addr.getHostName());
        Assertions.assertEquals(443, addr.getPort());
    }

    @Test
    public void testGetAddressFromURL_IPv4() throws Exception {
        URL url = new URL("http://192.168.1.1:8080/path");
        InetSocketAddress addr = AddrUtil.getAddressFromURL(url);
        Assertions.assertEquals("192.168.1.1", addr.getHostName());
        Assertions.assertEquals(8080, addr.getPort());
    }

    @Test
    public void testGetAddressFromURL_DifferentPort() throws Exception {
        URL url = new URL("http://localhost:3000");
        InetSocketAddress addr = AddrUtil.getAddressFromURL(url);
        Assertions.assertEquals(3000, addr.getPort());
    }

    @Test
    public void testGetAddressFromURL_Null() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            AddrUtil.getAddressFromURL(null);
        });
    }

    @Test
    public void testGetAddressListFromURL_Basic() throws Exception {
        List<URL> urls = Arrays.asList(new URL("http://server1.com:8080"), new URL("http://server2.com:9090"));
        List<InetSocketAddress> addrs = AddrUtil.getAddressListFromURL(urls);
        Assertions.assertEquals(2, addrs.size());
        Assertions.assertEquals(8080, addrs.get(0).getPort());
        Assertions.assertEquals(9090, addrs.get(1).getPort());
    }

    @Test
    public void testGetAddressListFromURL_SingleURL() throws Exception {
        List<URL> urls = Arrays.asList(new URL("http://example.com:7070"));
        List<InetSocketAddress> addrs = AddrUtil.getAddressListFromURL(urls);
        Assertions.assertEquals(1, addrs.size());
        Assertions.assertEquals(7070, addrs.get(0).getPort());
    }

    @Test
    public void testGetAddressListFromURL_MultipleURLs() throws Exception {
        List<URL> urls = Arrays.asList(new URL("http://server1.com:8080"), new URL("http://server2.com:9090"), new URL("http://server3.com:10000"));
        List<InetSocketAddress> addrs = AddrUtil.getAddressListFromURL(urls);
        Assertions.assertEquals(3, addrs.size());
        Assertions.assertEquals("server1.com", addrs.get(0).getHostName());
        Assertions.assertEquals("server2.com", addrs.get(1).getHostName());
        Assertions.assertEquals("server3.com", addrs.get(2).getHostName());
    }

    @Test
    public void testGetAddressListFromURL_WithoutPorts() throws Exception {
        List<URL> urls = Arrays.asList(new URL("http://server1.com"), new URL("http://server2.com"));
        List<InetSocketAddress> addrs = AddrUtil.getAddressListFromURL(urls);
        Assertions.assertEquals(2, addrs.size());
        Assertions.assertEquals(80, addrs.get(0).getPort());
        Assertions.assertEquals(80, addrs.get(1).getPort());
    }

    @Test
    public void testGetAddressListFromURL_Null() {
        List<InetSocketAddress> addrs = AddrUtil.getAddressListFromURL(null);
        Assertions.assertNotNull(addrs);
        Assertions.assertTrue(addrs.isEmpty());
    }

    @Test
    public void testGetAddressListFromURL_EmptyList() {
        List<InetSocketAddress> addrs = AddrUtil.getAddressListFromURL(new ArrayList<URL>());
        Assertions.assertNotNull(addrs);
        Assertions.assertTrue(addrs.isEmpty());
    }

    @Test
    public void testGetAddressListFromURL_MixedProtocols() throws Exception {
        List<URL> urls = Arrays.asList(new URL("http://server1.com:8080"), new URL("https://server2.com:443"), new URL("ftp://server3.com:21"));
        List<InetSocketAddress> addrs = AddrUtil.getAddressListFromURL(urls);
        Assertions.assertEquals(3, addrs.size());
        Assertions.assertEquals(8080, addrs.get(0).getPort());
        Assertions.assertEquals(443, addrs.get(1).getPort());
        Assertions.assertEquals(21, addrs.get(2).getPort());
    }
}
