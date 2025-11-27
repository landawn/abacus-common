package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

@Tag("old-test")
public class JDK8Base64Test extends AbstractTest {

    @Test
    public void test_url_encoder_decoder() {

        String str = "askfjeiw里的非阿斯兰阿 -- .. __ // \\ 卡90249啊是否//、、、、||\rkf32034 .sfkf klasf\f\n\r";

        N.println(Strings.base64UrlEncode(str.getBytes()));

        assertEquals(str, Strings.base64UrlDecodeToString(Strings.base64UrlEncode(str.getBytes())));

    }

    @Test
    public void test_encoder_decoder() throws Exception {
        String str = "askfjeiw里的非阿斯兰阿 -- .. __ // \\ 卡90249啊是否//、、、、||\rkf32034 .sfkf klasf\f\n\r";

        N.println(Strings.base64UrlEncode(str.getBytes()));

        assertEquals(str, Strings.base64DecodeToUtf8String(Strings.base64Encode(str.getBytes())));

    }
}
