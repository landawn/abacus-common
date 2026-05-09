package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

public class PasswordTest extends AbstractTest {

    @Test
    public void testMD5Encryption() {
        Password pwd = new Password("MD5");
        String st = "abcksfe里飞×……￥%……&×（!@#$%^&*()_@#$^&*()_)(*&）23ro lseif";
        String encryptedPassword = pwd.encrypt(st);
        N.println(encryptedPassword);
        assertTrue(pwd.isEqual(st, encryptedPassword));
        assertFalse(pwd.isEqual(st + " ", encryptedPassword));
    }

    @Test
    public void testSHA256Encryption() {
        Password pwd = new Password("SHA-256");
        String st = "abcksfe里飞×……￥%……&×（!@#$%^&*()_@#$^&*()_)(*&）23ro lseif";
        String encryptedPassword = pwd.encrypt(st);
        N.println(encryptedPassword);
        assertTrue(pwd.isEqual(st, encryptedPassword));
        assertFalse(pwd.isEqual(st + " ", encryptedPassword));

        N.println(pwd.toString());

        Password pwd2 = new Password("SHA-256");
        Set<Password> set = N.toSet(pwd);
        assertTrue(set.contains(pwd2));

        assertEquals("SHA-256", pwd2.getAlgorithm());

        try {
            new Password("none");
            fail("Should throw RuntimeException");
        } catch (RuntimeException e) {
        }
    }

    @Test
    public void testGetAlgorithm() {
        Password password = new Password("SHA-256");
        assertEquals("SHA-256", password.getAlgorithm());
    }

    @Test
    public void testEncrypt_consistent() {
        Password password = new Password("SHA-256");
        String encrypted1 = password.encrypt("testPassword");
        String encrypted2 = password.encrypt("testPassword");
        assertEquals(encrypted1, encrypted2);
    }

    @Test
    public void testEncrypt_notNull() {
        Password password = new Password("SHA-256");
        String encrypted = password.encrypt("testPassword");
        assertNotNull(encrypted);
    }

    @Test
    public void testEncrypt_null() {
        Password password = new Password("SHA-256");
        String encrypted = password.encrypt(null);
        assertNull(encrypted);
    }

    @Test
    public void testIsEqual_match() {
        Password password = new Password("SHA-256");
        String encrypted = password.encrypt("testPassword");
        assertTrue(password.isEqual("testPassword", encrypted));
    }

    @Test
    public void testIsEqual_noMatch() {
        Password password = new Password("SHA-256");
        String encrypted = password.encrypt("testPassword");
        assertFalse(password.isEqual("wrongPassword", encrypted));
    }

    @Test
    public void testIsEqual_bothNull() {
        Password password = new Password("SHA-256");
        assertTrue(password.isEqual(null, null));
    }

    @Test
    public void testIsEqual_plainNull() {
        Password password = new Password("SHA-256");
        assertFalse(password.isEqual(null, "encrypted"));
    }

    @Test
    public void testHashCode_consistent() {
        Password password = new Password("SHA-256");
        assertEquals(password.hashCode(), password.hashCode());
    }

    @Test
    public void testEquals_differentAlgorithm() {
        Password p1 = new Password("SHA-256");
        Password p2 = new Password("MD5");
        assertNotEquals(p1, p2);
    }

    @Test
    public void testEquals_sameAlgorithm() {
        Password p1 = new Password("SHA-256");
        Password p2 = new Password("SHA-256");
        assertEquals(p1, p2);
    }

    @Test
    public void testToString() {
        Password password = new Password("SHA-256");
        String str = password.toString();
        assertTrue(str.contains("SHA-256"));
    }

    @Test
    public void testIsEqual_encryptedNull() {
        Password password = new Password("SHA-256");
        assertFalse(password.isEqual("anything", null));
    }

    @Test
    public void testEncrypt_unicodeUsesUtf8() {
        // The same string must hash to the same value regardless of platform charset.
        // We verify this by hashing the UTF-8 bytes directly with a fresh digest and
        // comparing to Password.encrypt's Base64 output.
        Password password = new Password("SHA-256");
        String input = "pässwörd-中文";
        String encrypted = password.encrypt(input);

        java.security.MessageDigest md;
        try {
            md = java.security.MessageDigest.getInstance("SHA-256");
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
        byte[] expected = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String expectedBase64 = java.util.Base64.getEncoder().encodeToString(expected);
        assertEquals(expectedBase64, encrypted);
    }

    @Test
    public void testEncrypt_threadSafety() throws InterruptedException {
        // encrypt() reuses a single MessageDigest and is synchronized; concurrent
        // callers must still receive correct, deterministic Base64 hashes.
        final Password password = new Password("SHA-256");
        final String input = "concurrent-input-value";
        final String expected = password.encrypt(input);

        final int numThreads = 16;
        final int iterations = 200;
        final Thread[] threads = new Thread[numThreads];
        final boolean[] failures = new boolean[numThreads];

        for (int t = 0; t < numThreads; t++) {
            final int idx = t;
            threads[t] = new Thread(() -> {
                for (int i = 0; i < iterations; i++) {
                    String got = password.encrypt(input);
                    if (!expected.equals(got)) {
                        failures[idx] = true;
                        return;
                    }
                }
            });
            threads[t].start();
        }
        for (Thread th : threads) {
            th.join();
        }
        for (boolean f : failures) {
            assertFalse(f, "encrypt() produced inconsistent output under concurrency");
        }
    }

    @Test
    public void testIsEqual_constantTimeContract() {
        // We can't reliably benchmark constant-time behavior in a unit test, but we can
        // pin the contract that isEqual delegates to MessageDigest.isEqual semantics:
        // - returns true for matching credentials
        // - returns false for any single-character difference, regardless of position.
        Password password = new Password("SHA-256");
        String correct = "correctHorseBatteryStaple";
        String encrypted = password.encrypt(correct);

        // differs in first char
        assertFalse(password.isEqual("XorrectHorseBatteryStaple", encrypted));
        // differs in middle
        assertFalse(password.isEqual("correctHorseBXtteryStaple", encrypted));
        // differs in last char
        assertFalse(password.isEqual("correctHorseBatteryStaplX", encrypted));
        // exact match
        assertTrue(password.isEqual(correct, encrypted));
    }

    @Test
    public void testIsEqual_sameLengthDifferentEncrypted() {
        Password password = new Password("SHA-256");
        String encrypted = password.encrypt("realPassword");
        // Build a same-length but completely different Base64 string.
        StringBuilder bogus = new StringBuilder(encrypted.length());
        for (int i = 0; i < encrypted.length(); i++) {
            char c = encrypted.charAt(i);
            // flip 'A' <-> 'B' to keep Base64 alphabet valid; '=' padding preserved.
            if (c == 'A') {
                bogus.append('B');
            } else if (c == 'B') {
                bogus.append('A');
            } else {
                bogus.append(c);
            }
        }
        assertFalse(password.isEqual("realPassword", bogus.toString()));
    }

}
