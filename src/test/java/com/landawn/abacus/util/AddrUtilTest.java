package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class AddrUtilTest extends TestBase {

    @Test
    public void testGetServerList() {
        assertEquals(List.of("server1:8080", "server2:8081"), AddrUtil.getServerList("server1:8080 server2:8081"));
        assertEquals(List.of("server1:8080", "server2:8081", "server3:8082"), AddrUtil.getServerList("server1:8080, server2:8081, server3:8082"));
        assertEquals(List.of("server1:8080", "server2:8081", "server3:8082"), AddrUtil.getServerList("server1:8080  ,  server2:8081    server3:8082"));
        assertEquals(List.of("192.168.1.1:8080", "192.168.1.2:8080"), AddrUtil.getServerList("192.168.1.1:8080 192.168.1.2:8080"));
        assertEquals(List.of("::1:11211", "fe80::1:8080"), AddrUtil.getServerList("::1:11211, fe80::1:8080"));
        assertEquals(List.of("server1:8080", "server2:8081"), AddrUtil.getServerList("  server1:8080   ,   server2:8081  "));
        assertEquals(List.of("localhost:9090"), AddrUtil.getServerList("localhost:9090"));
        assertEquals(List.of("localhost:11", "localhost:22"), AddrUtil.getServerList("localhost:11 localhost:22"));
        assertEquals(List.of("localhost:11", "localhost:22"), AddrUtil.getServerList("localhost:11, localhost:22"));
        assertEquals(List.of("localhost:11", "localhost:22"), AddrUtil.getServerList("localhost:11 , localhost:22"));

        assertThrows(IllegalArgumentException.class, () -> AddrUtil.getServerList(Strings.EMPTY));
        assertThrows(IllegalArgumentException.class, () -> AddrUtil.getServerList("   "));
        assertThrows(IllegalArgumentException.class, () -> AddrUtil.getServerList(",,,"));
    }

    @Test
    public void testGetAddressListFromString() {
        List<InetSocketAddress> addrs = AddrUtil.getAddressList("localhost:8080, 192.168.1.1:9090");
        assertEquals(2, addrs.size());
        assertEquals("localhost", addrs.get(0).getHostString());
        assertEquals(8080, addrs.get(0).getPort());
        assertEquals("192.168.1.1", addrs.get(1).getHostString());
        assertEquals(9090, addrs.get(1).getPort());

        addrs = AddrUtil.getAddressList("192.0.2.1:1111 192.0.2.2:2222");
        assertEquals(2, addrs.size());
        assertEquals(1111, addrs.get(0).getPort());
        assertEquals(2222, addrs.get(1).getPort());

        addrs = AddrUtil.getAddressList("[::1]:11211");
        assertEquals(1, addrs.size());
        assertEquals("0:0:0:0:0:0:0:1", addrs.get(0).getHostString());
        assertEquals(11211, addrs.get(0).getPort());

        addrs = AddrUtil.getAddressList("192.168.1.1:8080");
        assertEquals("192.168.1.1", addrs.get(0).getHostString());
        assertEquals(8080, addrs.get(0).getPort());

        assertEquals(3, AddrUtil.getAddressList("localhost:8080, 192.168.1.1:9090, [::1]:11211").size());

        addrs = AddrUtil.getAddressList("  192.0.2.1:1234  ,  192.0.2.2:5678  ");
        assertEquals(1234, addrs.get(0).getPort());
        assertEquals(5678, addrs.get(1).getPort());

        addrs = AddrUtil.getAddressList("[::1]:11211, [fe80::1]:8080");
        assertEquals("0:0:0:0:0:0:0:1", addrs.get(0).getHostString());
        assertEquals(11211, addrs.get(0).getPort());
        assertEquals("fe80:0:0:0:0:0:0:1", addrs.get(1).getHostString());
        assertEquals(8080, addrs.get(1).getPort());

        addrs = AddrUtil.getAddressList("[::1]:11211");
        assertEquals(1, addrs.size());
        assertEquals(11211, addrs.get(0).getPort());
    }

    @Test
    public void testGetAddressListFromCollection() {
        List<InetSocketAddress> addrs = AddrUtil.getAddressList(Arrays.asList("192.0.2.1:8080", "192.0.2.2:9090"));
        assertEquals(8080, addrs.get(0).getPort());
        assertEquals(9090, addrs.get(1).getPort());

        addrs = AddrUtil.getAddressList(Arrays.asList("192.168.1.1:8080", "10.0.0.1:9090"));
        assertEquals("192.168.1.1", addrs.get(0).getHostString());
        assertEquals("10.0.0.1", addrs.get(1).getHostString());

        addrs = AddrUtil.getAddressList(Arrays.asList("[::1]:11211", "[fe80::1]:8080"));
        assertEquals("0:0:0:0:0:0:0:1", addrs.get(0).getHostString());
        assertEquals(11211, addrs.get(0).getPort());
        assertEquals("fe80:0:0:0:0:0:0:1", addrs.get(1).getHostString());
        assertEquals(8080, addrs.get(1).getPort());

        assertEquals(3, AddrUtil.getAddressList(Arrays.asList("localhost:8080", "192.168.1.1:9090", "[::1]:11211")).size());
        assertEquals(65535, AddrUtil.getAddressList(Arrays.asList("localhost:65535")).get(0).getPort());
        assertEquals(11211, AddrUtil.getAddressList(Arrays.asList("[::1]:11211")).get(0).getPort());
        assertEquals(3000, AddrUtil.getAddressList(Arrays.asList("localhost:3000")).get(0).getPort());

        addrs = AddrUtil.getAddressList(CommonUtil.toList("localhost:11", "localhost:22"));
        assertEquals(2, addrs.size());
        assertEquals(11, addrs.get(0).getPort());
        assertEquals(22, addrs.get(1).getPort());
    }

    @Test
    public void testGetAddressList_EdgeCase() {
        assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList((String) null));
        assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList(""));
        assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList(Strings.EMPTY));
        assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList("localhost8080"));
        assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList(":8080"));
        assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList("localhost:"));
        assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList("localhost:abc"));
        assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList("localhost:80.5"));
        assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList("localhost:8080, invalidserver"));
        assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList("[::1:11211"));
        assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList("::1]:11211"));
        assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList("[]:8080"));
        assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList("::1"));
        assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList("fe80::1"));
        assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList("2001:db8::8a2e:370:7334"));
        assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList("fe80::1:8080"));
        assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList("abc:def:1234"));
        assertEquals(11211, AddrUtil.getAddressList("::1:11211").get(0).getPort());

        assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList(new ArrayList<>()));
        assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList(Arrays.asList("localhost8080")));
        assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList(Arrays.asList(":8080")));
        assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList(Arrays.asList("localhost:")));
        assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList(Arrays.asList("localhost:invalid")));
        assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList(Arrays.asList("localhost:-8080")));
        assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList(Arrays.asList("[::1:11211")));
        assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList(Arrays.asList("::1]:11211")));
        assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList(Arrays.asList("::1")));
        assertEquals(11211, AddrUtil.getAddressList(Arrays.asList("::1:11211")).get(0).getPort());
        IllegalArgumentException nullElement = assertThrows(IllegalArgumentException.class,
                () -> AddrUtil.getAddressList(Arrays.asList("localhost:8080", null)));
        assertTrue(nullElement.getMessage().contains("Invalid server 'null'"));
    }

    @Test
    public void testGetAddressFromUrl() throws Exception {
        assertTrue(AddrUtil.getAddressListFromUrls(null).isEmpty());
        assertTrue(AddrUtil.getAddressListFromUrls(new ArrayList<>()).isEmpty());

        List<InetSocketAddress> addrs = AddrUtil.getAddressListFromUrls(Arrays.asList(new URL("http://127.0.0.1:8080"), new URL("http://127.0.0.2:9090")));
        assertEquals(2, addrs.size());
        assertEquals(8080, addrs.get(0).getPort());
        assertEquals(9090, addrs.get(1).getPort());
        assertEquals(7070, AddrUtil.getAddressListFromUrls(Arrays.asList(new URL("http://127.0.0.1:7070"))).get(0).getPort());

        addrs = AddrUtil.getAddressListFromUrls(Arrays.asList(new URL("http://localhost"), new URL("http://127.0.0.1")));
        assertEquals(80, addrs.get(0).getPort());
        assertEquals(80, addrs.get(1).getPort());

        addrs = AddrUtil
                .getAddressListFromUrls(Arrays.asList(new URL("http://127.0.0.1:8080"), new URL("https://127.0.0.2:443"), new URL("ftp://127.0.0.3:21")));
        assertEquals(8080, addrs.get(0).getPort());
        assertEquals(443, addrs.get(1).getPort());
        assertEquals(21, addrs.get(2).getPort());

        addrs = AddrUtil.getAddressListFromUrls(CommonUtil.toList(new URL("https://127.0.0.1:443/")));
        assertEquals(1, addrs.size());
        assertEquals(443, addrs.get(0).getPort());

        assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressListFromUrls(Arrays.asList(new URL("file:/tmp/test"))));

        InetSocketAddress addr = AddrUtil.getAddressFromUrl(new URL("http://127.0.0.1:8080/path"));
        assertEquals("127.0.0.1", addr.getHostString());
        assertEquals(8080, addr.getPort());
        addr = AddrUtil.getAddressFromUrl(new URL("https://localhost/path"));
        assertEquals("localhost", addr.getHostString());
        assertEquals(443, addr.getPort());
        assertEquals(443, AddrUtil.getAddressFromUrl(new URL("https://127.0.0.1:443/api")).getPort());
        assertEquals(3000, AddrUtil.getAddressFromUrl(new URL("http://localhost:3000")).getPort());
        assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressFromUrl(null));
        assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressFromUrl(new URL("file:/tmp/test")));
    }

    // R07 (2026-09-08): r9506 replaced the "is the whole token a valid IPv6 literal" ambiguity test with a plain
    // "more than one colon" count, on the premise that the two assertions above ("::1:11211" splits, the
    // "identically shaped" "fe80::1:8080" throws) were mutually contradictory. They are not: 11211 needs five hex
    // digits, so "::1:11211" cannot BE an address and host+port is its only reading, while "fe80::1:8080" is a
    // valid address AND a valid endpoint. The colon count also broke MappedIpv6Test and
    // MultiClassRegressionGTest.ipv6FormsThatWereAcceptedStillAre, which pin "::1:11211" as a compatibility
    // guarantee. The literal test is back, now purely syntactic (no InetAddress, no platform name service).
    @Test
    public void reviewFixes20260908_onlyTrulyAmbiguousUnbracketedTokensAreRejected() {
        // The whole token is itself a valid IPv6 literal -> two readings -> rejected as ambiguous.
        for (final String token : new String[] { "::1", "fe80::1", "2001:db8::8a2e:370:7334", "fe80::1:8080", "::1:1121", "::", "::ffff:127.0.0.1",
                "::ffff:c000:201", "1:2:3:4:5:6:7:8" }) {
            final IllegalArgumentException fromString = assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList(token), token);
            final IllegalArgumentException fromCollection = assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList(Arrays.asList(token)),
                    token);
            assertTrue(fromString.getMessage().contains("Unbracketed IPv6 address is ambiguous"), token + " -> " + fromString.getMessage());
            assertTrue(fromCollection.getMessage().contains("Unbracketed IPv6 address is ambiguous"), token + " -> " + fromCollection.getMessage());
        }

        // The token cannot be an address and the host part is not one either -> rejected as an invalid host.
        for (final String token : new String[] { "abc:def:1234", "1:2:3:4:5:6:7:8:9:9999" }) {
            final IllegalArgumentException fromString = assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList(token), token);
            final IllegalArgumentException fromCollection = assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList(Arrays.asList(token)),
                    token);
            assertTrue(fromString.getMessage().contains("is not a valid IPv6 address"), token + " -> " + fromString.getMessage());
            assertTrue(fromCollection.getMessage().contains("is not a valid IPv6 address"), token + " -> " + fromCollection.getMessage());
        }

        // The token cannot be an address but the host part is one -> host+port is the only reading -> accepted.
        assertEquals("0:0:0:0:0:0:0:1", AddrUtil.getAddressList("::1:11211").get(0).getHostString());
        assertEquals(11211, AddrUtil.getAddressList("::1:11211").get(0).getPort());
        assertEquals(11211, AddrUtil.getAddressList(Arrays.asList("::1:11211")).get(0).getPort());
        assertEquals(80, AddrUtil.getAddressList("::ffff:192.0.2.1:80").get(0).getPort());
        assertEquals(65535, AddrUtil.getAddressList("1:2:3:4:5:6:7:8:65535").get(0).getPort());

        // A five-hex-digit tail is not a hextet, but it is still range-checked as a port.
        assertTrue(assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList("::1:99999")).getMessage().contains("Invalid port number"));

        // Brackets always remove the ambiguity, and a single-colon host:port is untouched.
        assertEquals(11211, AddrUtil.getAddressList("[::1]:11211").get(0).getPort());
        assertEquals(8080, AddrUtil.getAddressList("[fe80::1]:8080").get(0).getPort());
        assertEquals(80, AddrUtil.getAddressList("[2001:db8::8a2e:370:7334]:80").get(0).getPort());
        assertEquals(8080, AddrUtil.getAddressList("[fe80::1:8080]:8080").get(0).getPort());
        assertEquals("localhost", AddrUtil.getAddressList("localhost:8080").get(0).getHostString());
        assertEquals("192.168.1.1", AddrUtil.getAddressList("192.168.1.1:8080").get(0).getHostString());
    }

    @Test
    public void unbracketedIpv6WithZoneAndPortIsHostPortNotAmbiguousLiteral() {
        final InetSocketAddress addr = AddrUtil.getAddressList("fe80::1%eth0:8080").get(0);
        assertTrue(addr.getHostString().contains("fe80"), addr.getHostString());
        assertTrue(addr.getHostString().contains("%eth0"), addr.getHostString());
        assertEquals(8080, addr.getPort());

        final InetSocketAddress fromList = AddrUtil.getAddressList(List.of("fe80::1%lo:11211")).get(0);
        assertTrue(fromList.getHostString().contains("%lo"), fromList.getHostString());
        assertEquals(11211, fromList.getPort());

        assertEquals(8080, AddrUtil.getAddressList("[fe80::1%eth0]:8080").get(0).getPort());
        assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList("fe80::1%eth0"));
        assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList("fe80::1:8080"));
    }
}
