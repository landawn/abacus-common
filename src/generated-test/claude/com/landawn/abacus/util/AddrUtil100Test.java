package com.landawn.abacus.util;

import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;


public class AddrUtil100Test extends TestBase {

    @Test
    public void testGetServerList() {
        List<String> servers = AddrUtil.getServerList("server1:8080 server2:8081");
        Assertions.assertEquals(2, servers.size());
        Assertions.assertEquals("server1:8080", servers.get(0));
        Assertions.assertEquals("server2:8081", servers.get(1));
    }

    @Test
    public void testGetServerListWithCommas() {
        List<String> servers = AddrUtil.getServerList("server1:8080, server2:8081, server3:8082");
        Assertions.assertEquals(3, servers.size());
        Assertions.assertEquals("server1:8080", servers.get(0));
        Assertions.assertEquals("server2:8081", servers.get(1));
        Assertions.assertEquals("server3:8082", servers.get(2));
    }

    @Test
    public void testGetServerListWithMixedSeparators() {
        List<String> servers = AddrUtil.getServerList("server1:8080  ,  server2:8081    server3:8082");
        Assertions.assertEquals(3, servers.size());
    }

    @Test
    public void testGetServerListInvalid() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            AddrUtil.getServerList("");
        });
    }

    @Test
    public void testGetAddressListFromString() {
        List<InetSocketAddress> addrs = AddrUtil.getAddressList("localhost:8080, 192.168.1.1:9090");
        Assertions.assertEquals(2, addrs.size());
        Assertions.assertEquals(8080, addrs.get(0).getPort());
        Assertions.assertEquals(9090, addrs.get(1).getPort());
    }

    @Test
    public void testGetAddressListWithSpaces() {
        List<InetSocketAddress> addrs = AddrUtil.getAddressList("host1:1111 host2:2222");
        Assertions.assertEquals(2, addrs.size());
    }

    @Test
    public void testGetAddressListEmptyString() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            AddrUtil.getAddressList("");
        });
    }

    @Test
    public void testGetAddressListNullString() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            AddrUtil.getAddressList((String) null);
        });
    }

    @Test
    public void testGetAddressListFromCollection() {
        List<String> servers = Arrays.asList("server1:8080", "server2:9090");
        List<InetSocketAddress> addrs = AddrUtil.getAddressList(servers);
        Assertions.assertEquals(2, addrs.size());
        Assertions.assertEquals(8080, addrs.get(0).getPort());
        Assertions.assertEquals(9090, addrs.get(1).getPort());
    }

    @Test
    public void testGetAddressListFromEmptyCollection() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            AddrUtil.getAddressList(new ArrayList<String>());
        });
    }

    @Test
    public void testGetAddressFromURL() throws Exception {
        URL url = new URL("http://example.com:8080/path");
        InetSocketAddress addr = AddrUtil.getAddressFromURL(url);
        Assertions.assertEquals("example.com", addr.getHostName());
        Assertions.assertEquals(8080, addr.getPort());
    }

    @Test
    public void testGetAddressListFromURL() throws Exception {
        List<URL> urls = Arrays.asList(new URL("http://server1.com:8080"), new URL("http://server2.com:9090"));
        List<InetSocketAddress> addrs = AddrUtil.getAddressListFromURL(urls);
        Assertions.assertEquals(2, addrs.size());
        Assertions.assertEquals(8080, addrs.get(0).getPort());
        Assertions.assertEquals(9090, addrs.get(1).getPort());
    }

    @Test
    public void testGetAddressListFromURLWithNull() {
        List<InetSocketAddress> addrs = AddrUtil.getAddressListFromURL(null);
        Assertions.assertTrue(addrs.isEmpty());
    }

    @Test
    public void testGetAddressListFromURLWithEmpty() {
        List<InetSocketAddress> addrs = AddrUtil.getAddressListFromURL(new ArrayList<URL>());
        Assertions.assertTrue(addrs.isEmpty());
    }
}