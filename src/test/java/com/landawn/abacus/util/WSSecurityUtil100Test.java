package com.landawn.abacus.util;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class WSSecurityUtil100Test extends TestBase {

    @Test
    public void testGenerateNonce() {
        int length = 16;
        byte[] nonce = WSSecurityUtil.generateNonce(length);

        Assertions.assertNotNull(nonce);
        Assertions.assertEquals(length, nonce.length);

        byte[] nonce2 = WSSecurityUtil.generateNonce(length);
        Assertions.assertFalse(java.util.Arrays.equals(nonce, nonce2));
    }

    @Test
    public void testGenerateDigest() {
        String input = "test data";
        byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);

        byte[] digest = WSSecurityUtil.generateDigest(inputBytes);

        Assertions.assertNotNull(digest);
        Assertions.assertEquals(20, digest.length);

        byte[] digest2 = WSSecurityUtil.generateDigest(inputBytes);
        Assertions.assertArrayEquals(digest, digest2);
    }

    @Test
    public void testDoPasswordDigestBytes() {
        byte[] nonce = "nonce".getBytes(StandardCharsets.UTF_8);
        byte[] created = "created".getBytes(StandardCharsets.UTF_8);
        byte[] password = "password".getBytes(StandardCharsets.UTF_8);

        String digest = WSSecurityUtil.doPasswordDigest(nonce, created, password);

        Assertions.assertNotNull(digest);
        Assertions.assertFalse(digest.isEmpty());

        String digest2 = WSSecurityUtil.doPasswordDigest(nonce, created, password);
        Assertions.assertEquals(digest, digest2);
    }

    @Test
    public void testDoPasswordDigestStrings() {
        String nonce = "nonce";
        String created = "created";
        String password = "password";

        String digest = WSSecurityUtil.doPasswordDigest(nonce, created, password);

        Assertions.assertNotNull(digest);
        Assertions.assertFalse(digest.isEmpty());

        String digest2 = WSSecurityUtil.doPasswordDigest(nonce, created, password);
        Assertions.assertEquals(digest, digest2);
    }
}
