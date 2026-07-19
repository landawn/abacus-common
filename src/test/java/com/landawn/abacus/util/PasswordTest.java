package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

public class PasswordTest extends AbstractTest {

    @Test
    public void testMD5Digestion() {
        Password pwd = new Password("MD5");
        String st = "abcksfe里飞×……￥%……&×（!@#$%^&*()_@#$^&*()_)(*&）23ro lseif";
        String encodedDigest = pwd.digest(st);
        N.println(encodedDigest);
        assertTrue(pwd.isEqual(st, encodedDigest));
        assertFalse(pwd.isEqual(st + " ", encodedDigest));
    }

    @Test
    public void testSHA256Digestion() {
        Password pwd = new Password("SHA-256");
        String st = "abcksfe里飞×……￥%……&×（!@#$%^&*()_@#$^&*()_)(*&）23ro lseif";
        String encodedDigest = pwd.digest(st);
        N.println(encodedDigest);
        assertTrue(pwd.isEqual(st, encodedDigest));
        assertFalse(pwd.isEqual(st + " ", encodedDigest));

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
    public void testDigest_consistent() {
        Password password = new Password("SHA-256");
        String digest1 = password.digest("testPassword");
        String digest2 = password.digest("testPassword");
        assertEquals(digest1, digest2);
    }

    @Test
    public void testDigest_notNull() {
        Password password = new Password("SHA-256");
        String digest = password.digest("testPassword");
        assertNotNull(digest);
    }

    @Test
    public void testDigest_null() {
        Password password = new Password("SHA-256");
        String digest = password.digest(null);
        assertNull(digest);
    }

    @Test
    public void testIsEqual_match() {
        Password password = new Password("SHA-256");
        String digest = password.digest("testPassword");
        assertTrue(password.isEqual("testPassword", digest));
    }

    @Test
    public void testIsEqual_noMatch() {
        Password password = new Password("SHA-256");
        String digest = password.digest("testPassword");
        assertFalse(password.isEqual("wrongPassword", digest));
    }

    @Test
    public void testIsEqual_bothNull() {
        Password password = new Password("SHA-256");
        assertTrue(password.isEqual(null, null));
    }

    @Test
    public void testIsEqual_plainNull() {
        Password password = new Password("SHA-256");
        assertFalse(password.isEqual(null, "digest"));
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
    public void testAlgorithmAliasesUseCanonicalIdentity() {
        Password upperCase = new Password("SHA-256");
        Password lowerCase = new Password("sha-256");

        assertEquals(upperCase, lowerCase);
        assertEquals(upperCase.hashCode(), lowerCase.hashCode());
        assertEquals(upperCase.getAlgorithm(), lowerCase.getAlgorithm());
        assertThrows(NullPointerException.class, () -> new Password(null));
    }

    @Test
    public void testToString() {
        Password password = new Password("SHA-256");
        String str = password.toString();
        assertTrue(str.contains("SHA-256"));
    }

    @Test
    public void testIsEqual_digestNull() {
        Password password = new Password("SHA-256");
        assertFalse(password.isEqual("anything", null));
    }

    @Test
    public void testDigest_unicodeUsesUtf8() {
        // The same string must hash to the same value regardless of platform charset.
        // We verify this by hashing the UTF-8 bytes directly with a fresh digest and
        // comparing to Password.digest's Base64 output.
        Password password = new Password("SHA-256");
        String input = "pässwörd-中文";
        String digest = password.digest(input);

        java.security.MessageDigest md;
        try {
            md = java.security.MessageDigest.getInstance("SHA-256");
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
        byte[] expected = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String expectedBase64 = java.util.Base64.getEncoder().encodeToString(expected);
        assertEquals(expectedBase64, digest);
    }

    @Test
    public void testDigest_threadSafety() throws InterruptedException {
        // digest() reuses a single MessageDigest and is synchronized; concurrent
        // callers must still receive correct, deterministic Base64 hashes.
        final Password password = new Password("SHA-256");
        final String input = "concurrent-input-value";
        final String expected = password.digest(input);

        final int numThreads = 16;
        final int iterations = 200;
        final Thread[] threads = new Thread[numThreads];
        final boolean[] failures = new boolean[numThreads];

        for (int t = 0; t < numThreads; t++) {
            final int idx = t;
            threads[t] = new Thread(() -> {
                for (int i = 0; i < iterations; i++) {
                    String got = password.digest(input);
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
            assertFalse(f, "digest() produced inconsistent output under concurrency");
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
        String digest = password.digest(correct);

        // differs in first char
        assertFalse(password.isEqual("XorrectHorseBatteryStaple", digest));
        // differs in middle
        assertFalse(password.isEqual("correctHorseBXtteryStaple", digest));
        // differs in last char
        assertFalse(password.isEqual("correctHorseBatteryStaplX", digest));
        // exact match
        assertTrue(password.isEqual(correct, digest));
    }

    @Test
    public void testIsEqual_sameLengthDifferentDigested() {
        Password password = new Password("SHA-256");
        String digest = password.digest("realPassword");
        // Build a same-length but completely different Base64 string.
        StringBuilder bogus = new StringBuilder(digest.length());
        for (int i = 0; i < digest.length(); i++) {
            char c = digest.charAt(i);
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
