package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.fail;

import java.net.InetSocketAddress;
import java.net.URL;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

@Tag("old-test")
public class AddrUtilTest extends AbstractTest {

    @Test
    public void test_getServerList() throws Exception {
        for (InetSocketAddress addr : AddrUtil.getAddressList("localhost:11 localhost:22")) {
            N.println(addr);
        }

        for (String str : AddrUtil.getServerList("localhost:11 localhost:22")) {
            N.println(str);
        }

        for (String str : AddrUtil.getServerList("localhost:11, localhost:22")) {
            N.println(str);
        }

        for (String str : AddrUtil.getServerList("localhost:11 , localhost:22")) {
            N.println(str);
        }

        for (InetSocketAddress add : AddrUtil.getAddressList(CommonUtil.asList("localhost:11", "localhost:22"))) {
            N.println(add.toString());
        }

        N.println(AddrUtil.getAddressListFromURL(CommonUtil.asList(new URL("https://www.google.com:443/"))));

        try {
            AddrUtil.getServerList(Strings.EMPTY);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        try {
            AddrUtil.getAddressList((String) null);
            fail("Should throw NullPointerException");
        } catch (IllegalArgumentException e) {
        }

        try {
            AddrUtil.getAddressList(Strings.EMPTY);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }
}
