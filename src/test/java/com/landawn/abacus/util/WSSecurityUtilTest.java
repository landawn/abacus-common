package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class WSSecurityUtilTest extends TestBase {

    @Test
    public void testGenerateNonce() throws InterruptedException {
        byte[] nonce16 = WSSecurityUtil.generateNonce(16);
        assertEquals(16, nonce16.length);
        assertFalse(Arrays.equals(nonce16, WSSecurityUtil.generateNonce(16)));
        assertEquals(32, WSSecurityUtil.generateNonce(32).length);
        assertEquals(0, WSSecurityUtil.generateNonce(0).length);
        assertEquals(1, WSSecurityUtil.generateNonce(1).length);
        assertThrows(IllegalArgumentException.class, () -> WSSecurityUtil.generateNonce(-1));
        assertThrows(IllegalArgumentException.class, () -> WSSecurityUtil.generateNonce(-100));

        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    assertEquals(16, WSSecurityUtil.generateNonce(16).length);
                }
            });
            threads[i].start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
    }

    @Test
    public void testGenerateDigest() {
        byte[] input = "test data".getBytes(StandardCharsets.UTF_8);
        byte[] digest = WSSecurityUtil.generateDigest(input);
        assertEquals(20, digest.length);
        assertArrayEquals(digest, WSSecurityUtil.generateDigest(input));
        assertArrayEquals(digest, WSSecurityUtil.generateDigest(input, "SHA-1"));
        assertEquals(20, WSSecurityUtil.generateDigest("Hello World".getBytes(StandardCharsets.UTF_8)).length);
        assertEquals(20, WSSecurityUtil.generateDigest(new byte[0]).length);
        assertFalse(CommonUtil.equals(WSSecurityUtil.generateDigest("input1".getBytes(StandardCharsets.UTF_8)),
                WSSecurityUtil.generateDigest("input2".getBytes(StandardCharsets.UTF_8))));

        byte[] sha256 = WSSecurityUtil.generateDigest("Hello World".getBytes(StandardCharsets.UTF_8), "SHA-256");
        assertEquals(32, sha256.length);
        assertArrayEquals(sha256, WSSecurityUtil.generateDigest("Hello World".getBytes(StandardCharsets.UTF_8), "SHA-256"));
        assertEquals(64, WSSecurityUtil.generateDigest("Hello World".getBytes(StandardCharsets.UTF_8), "SHA-512").length);
        assertFalse(CommonUtil.equals(WSSecurityUtil.generateDigest("input1".getBytes(StandardCharsets.UTF_8), "SHA-256"),
                WSSecurityUtil.generateDigest("input2".getBytes(StandardCharsets.UTF_8), "SHA-256")));

        assertThrows(IllegalArgumentException.class, () -> WSSecurityUtil.generateDigest(null));
        assertThrows(IllegalArgumentException.class, () -> WSSecurityUtil.generateDigest(null, "SHA-256"));
        assertThrows(IllegalArgumentException.class, () -> WSSecurityUtil.generateDigest("test".getBytes(StandardCharsets.UTF_8), null));
        assertThrows(IllegalArgumentException.class, () -> WSSecurityUtil.generateDigest("test".getBytes(StandardCharsets.UTF_8), "INVALID-ALG"));
    }

    @Test
    public void testComputePasswordDigestBytes() {
        byte[] nonce = new byte[] { 1, 2, 3, 4 };
        byte[] created = "2024-01-01T12:00:00Z".getBytes(StandardCharsets.UTF_8);
        byte[] password = "secret".getBytes(StandardCharsets.UTF_8);

        assertEquals("ANVG+rR8Ea6eARzR7LEvAEd0FA8=", WSSecurityUtil.computePasswordDigest(nonce, created, password));
        assertEquals("PQRs7zedW68UVxCH7bQ4HcU7oo/oF0mo11zEPRi7qVE=", WSSecurityUtil.computePasswordDigest(nonce, created, password, "SHA-256"));
        assertEquals(WSSecurityUtil.computePasswordDigest(nonce, created, password), WSSecurityUtil.computePasswordDigest(nonce, created, password, "SHA-1"));
        assertNotEquals(WSSecurityUtil.computePasswordDigest(nonce, created, password, "SHA-1"),
                WSSecurityUtil.computePasswordDigest(nonce, created, password, "SHA-256"));

        byte[] n = "nonce".getBytes(StandardCharsets.UTF_8);
        byte[] c = "created".getBytes(StandardCharsets.UTF_8);
        byte[] p = "password".getBytes(StandardCharsets.UTF_8);
        String digest = WSSecurityUtil.computePasswordDigest(n, c, p);
        assertFalse(digest.isEmpty());
        assertEquals(digest, WSSecurityUtil.computePasswordDigest(n, c, p));
        assertNotEquals(digest, WSSecurityUtil.computePasswordDigest(c, n, p));
        assertNotEquals(digest, WSSecurityUtil.computePasswordDigest(p, c, n));
        assertDoesNotThrow(() -> Base64.getDecoder().decode(digest));
        assertNotNull(WSSecurityUtil.computePasswordDigest(new byte[0], new byte[0], new byte[0]));

        byte[] randomNonce = WSSecurityUtil.generateNonce(16);
        byte[] createdUtf8 = "2024-01-01T12:00:00Z".getBytes(StandardCharsets.UTF_8);
        byte[] secret = "secretPassword".getBytes(StandardCharsets.UTF_8);
        assertDoesNotThrow(() -> Base64.getDecoder().decode(WSSecurityUtil.computePasswordDigest(randomNonce, createdUtf8, secret)));
        assertDoesNotThrow(() -> Base64.getDecoder().decode(WSSecurityUtil.computePasswordDigest(randomNonce, createdUtf8, secret, "SHA-256")));

        assertThrows(IllegalArgumentException.class, () -> WSSecurityUtil.computePasswordDigest(null, created, password));
        assertThrows(IllegalArgumentException.class, () -> WSSecurityUtil.computePasswordDigest(nonce, null, password));
        assertThrows(IllegalArgumentException.class, () -> WSSecurityUtil.computePasswordDigest(nonce, created, null));
        assertThrows(IllegalArgumentException.class, () -> WSSecurityUtil.computePasswordDigest(null, created, password, "SHA-256"));
        assertThrows(IllegalArgumentException.class, () -> WSSecurityUtil.computePasswordDigest(nonce, null, password, "SHA-256"));
        assertThrows(IllegalArgumentException.class, () -> WSSecurityUtil.computePasswordDigest(nonce, created, null, "SHA-256"));
        assertThrows(IllegalArgumentException.class, () -> WSSecurityUtil.computePasswordDigest(nonce, created, password, (String) null));
        assertThrows(IllegalArgumentException.class, () -> WSSecurityUtil.computePasswordDigest(nonce, created, password, "INVALID-ALG"));
    }

    @Test
    public void testComputePasswordDigestStrings() {
        String nonce = "testNonce";
        String created = "2024-01-01T12:00:00Z";
        String password = "testPassword";

        String defaultDigest = WSSecurityUtil.computePasswordDigest(nonce, created, password);
        assertEquals(defaultDigest, WSSecurityUtil.computePasswordDigest(nonce, created, password, "SHA-1"));
        assertNotEquals(defaultDigest, WSSecurityUtil.computePasswordDigest(nonce, created, password, "SHA-256"));
        assertEquals(defaultDigest, WSSecurityUtil.computePasswordDigest(nonce.getBytes(Charsets.DEFAULT), created.getBytes(Charsets.DEFAULT),
                password.getBytes(Charsets.DEFAULT)));
        assertEquals(WSSecurityUtil.computePasswordDigest(nonce, created, password, "SHA-256"), WSSecurityUtil
                .computePasswordDigest(nonce.getBytes(Charsets.DEFAULT), created.getBytes(Charsets.DEFAULT), password.getBytes(Charsets.DEFAULT), "SHA-256"));
        assertDoesNotThrow(() -> Base64.getDecoder().decode(defaultDigest));
        assertDoesNotThrow(() -> Base64.getDecoder().decode(WSSecurityUtil.computePasswordDigest("randomNonce", created, "secretPassword", "SHA-256")));
        assertNotNull(WSSecurityUtil.computePasswordDigest("", "", ""));
        assertNotNull(WSSecurityUtil.computePasswordDigest("", "", "", "SHA-256"));

        assertThrows(IllegalArgumentException.class, () -> WSSecurityUtil.computePasswordDigest(null, created, password));
        assertThrows(IllegalArgumentException.class, () -> WSSecurityUtil.computePasswordDigest(nonce, null, password));
        assertThrows(IllegalArgumentException.class, () -> WSSecurityUtil.computePasswordDigest(nonce, created, null));
        assertThrows(IllegalArgumentException.class, () -> WSSecurityUtil.computePasswordDigest(null, created, password, "SHA-256"));
        assertThrows(IllegalArgumentException.class, () -> WSSecurityUtil.computePasswordDigest(nonce, null, password, "SHA-256"));
        assertThrows(IllegalArgumentException.class, () -> WSSecurityUtil.computePasswordDigest(nonce, created, null, "SHA-256"));
        assertThrows(IllegalArgumentException.class, () -> WSSecurityUtil.computePasswordDigest(nonce, created, password, (String) null));
        assertThrows(IllegalArgumentException.class, () -> WSSecurityUtil.computePasswordDigest(nonce, created, password, "INVALID-ALG"));
    }
}
