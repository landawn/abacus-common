package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.InetAddress;
import java.util.List;

import org.junit.jupiter.api.Test;

@org.junit.jupiter.api.Tag("unit")
public class MappedIpv6Test {
    @Test
    void mappedLiteralsRequireAnExplicitPort() {
        for (final String input : List.of("::ffff:0:1234", "::ffff:127.0.0.1", "::ffff:c000:201", "::1:1234")) {
            assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList(input), input);
            assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList(List.of(input)), input);
        }
    }

    @Test
    void explicitNumericEndpointsKeepTheirHostAndPort() throws Exception {
        for (final String host : List.of("::ffff:0:1234", "::ffff:127.0.0.1", "::1")) {
            for (final int port : new int[] { 0, 80, 65535 }) {
                final String endpoint = "[" + host + "]:" + port;
                final var addresses = AddrUtil.getAddressList(endpoint);
                assertEquals(1, addresses.size());
                assertEquals(InetAddress.getByName(host), addresses.get(0).getAddress());
                assertEquals(port, addresses.get(0).getPort());
                assertEquals(addresses, AddrUtil.getAddressList(List.of(endpoint)));
            }
        }
        assertEquals(11211, AddrUtil.getAddressList("::1:11211").get(0).getPort());
        assertEquals(80, AddrUtil.getAddressList("127.0.0.1:80").get(0).getPort());
        assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList("[::ffff:0:1234]:65536"));
    }
}
