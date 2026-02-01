package com.landawn.abacus.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class WSSecurityUtil2025Test extends TestBase {

    @Test
    public void testGenerateNonce_withValidLength() {
        byte[] nonce16 = WSSecurityUtil.generateNonce(16);
        Assertions.assertNotNull(nonce16);
        Assertions.assertEquals(16, nonce16.length);

        byte[] nonce32 = WSSecurityUtil.generateNonce(32);
        Assertions.assertNotNull(nonce32);
        Assertions.assertEquals(32, nonce32.length);

        byte[] nonce0 = WSSecurityUtil.generateNonce(0);
        Assertions.assertNotNull(nonce0);
        Assertions.assertEquals(0, nonce0.length);

        byte[] nonce1 = WSSecurityUtil.generateNonce(1);
        Assertions.assertNotNull(nonce1);
        Assertions.assertEquals(1, nonce1.length);
    }

    @Test
    public void testGenerateNonce_randomness() {
        byte[] nonce1 = WSSecurityUtil.generateNonce(16);
        byte[] nonce2 = WSSecurityUtil.generateNonce(16);

        Assertions.assertNotNull(nonce1);
        Assertions.assertNotNull(nonce2);
        Assertions.assertFalse(CommonUtil.equals(nonce1, nonce2), "Two consecutive nonces should be different");
    }

    @Test
    public void testGenerateNonce_withNegativeLength() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            WSSecurityUtil.generateNonce(-1);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            WSSecurityUtil.generateNonce(-100);
        });
    }

    @Test
    public void testGenerateDigest_withValidInput() {
        String input = "Hello World";
        byte[] digest = WSSecurityUtil.generateDigest(input.getBytes(StandardCharsets.UTF_8));

        Assertions.assertNotNull(digest);
        Assertions.assertEquals(20, digest.length, "SHA-1 digest should be 20 bytes");

        byte[] emptyDigest = WSSecurityUtil.generateDigest(new byte[0]);
        Assertions.assertNotNull(emptyDigest);
        Assertions.assertEquals(20, emptyDigest.length);
    }

    @Test
    public void testGenerateDigest_consistency() {
        String input = "Test String";
        byte[] digest1 = WSSecurityUtil.generateDigest(input.getBytes(StandardCharsets.UTF_8));
        byte[] digest2 = WSSecurityUtil.generateDigest(input.getBytes(StandardCharsets.UTF_8));

        Assertions.assertArrayEquals(digest1, digest2, "Same input should produce same digest");
    }

    @Test
    public void testGenerateDigest_withNullInput() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            WSSecurityUtil.generateDigest(null);
        });
    }

    @Test
    public void testGenerateDigest_withDifferentInputs() {
        byte[] digest1 = WSSecurityUtil.generateDigest("input1".getBytes(StandardCharsets.UTF_8));
        byte[] digest2 = WSSecurityUtil.generateDigest("input2".getBytes(StandardCharsets.UTF_8));

        Assertions.assertFalse(CommonUtil.equals(digest1, digest2), "Different inputs should produce different digests");
    }

    @Test
    public void testDoPasswordDigest_withByteArrays() {
        byte[] nonce = WSSecurityUtil.generateNonce(16);
        byte[] created = "2024-01-01T12:00:00Z".getBytes(StandardCharsets.UTF_8);
        byte[] password = "secretPassword".getBytes(StandardCharsets.UTF_8);

        String digest = WSSecurityUtil.computePasswordDigest(nonce, created, password);

        Assertions.assertNotNull(digest);
        Assertions.assertTrue(digest.length() > 0);

        Assertions.assertDoesNotThrow(() -> {
            Base64.getDecoder().decode(digest);
        });
    }

    @Test
    public void testDoPasswordDigest_byteArrays_consistency() {
        byte[] nonce = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
        byte[] created = "2024-01-01T12:00:00Z".getBytes(StandardCharsets.UTF_8);
        byte[] password = "secretPassword".getBytes(StandardCharsets.UTF_8);

        String digest1 = WSSecurityUtil.computePasswordDigest(nonce, created, password);
        String digest2 = WSSecurityUtil.computePasswordDigest(nonce, created, password);

        Assertions.assertEquals(digest1, digest2, "Same inputs should produce same password digest");
    }

    @Test
    public void testDoPasswordDigest_byteArrays_withNullNonce() {
        byte[] created = "2024-01-01T12:00:00Z".getBytes(StandardCharsets.UTF_8);
        byte[] password = "secretPassword".getBytes(StandardCharsets.UTF_8);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            WSSecurityUtil.computePasswordDigest(null, created, password);
        });
    }

    @Test
    public void testDoPasswordDigest_byteArrays_withNullCreated() {
        byte[] nonce = WSSecurityUtil.generateNonce(16);
        byte[] password = "secretPassword".getBytes(StandardCharsets.UTF_8);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            WSSecurityUtil.computePasswordDigest(nonce, null, password);
        });
    }

    @Test
    public void testDoPasswordDigest_byteArrays_withNullPassword() {
        byte[] nonce = WSSecurityUtil.generateNonce(16);
        byte[] created = "2024-01-01T12:00:00Z".getBytes(StandardCharsets.UTF_8);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            WSSecurityUtil.computePasswordDigest(nonce, created, null);
        });
    }

    @Test
    public void testDoPasswordDigest_byteArrays_withEmptyArrays() {
        byte[] nonce = new byte[0];
        byte[] created = new byte[0];
        byte[] password = new byte[0];

        String digest = WSSecurityUtil.computePasswordDigest(nonce, created, password);

        Assertions.assertNotNull(digest);
        Assertions.assertTrue(digest.length() > 0);
    }

    @Test
    public void testDoPasswordDigest_withStrings() {
        String nonce = "randomNonceString";
        String created = "2024-01-01T12:00:00Z";
        String password = "secretPassword";

        String digest = WSSecurityUtil.computePasswordDigest(nonce, created, password);

        Assertions.assertNotNull(digest);
        Assertions.assertTrue(digest.length() > 0);

        Assertions.assertDoesNotThrow(() -> {
            Base64.getDecoder().decode(digest);
        });
    }

    @Test
    public void testDoPasswordDigest_strings_consistency() {
        String nonce = "randomNonceString";
        String created = "2024-01-01T12:00:00Z";
        String password = "secretPassword";

        String digest1 = WSSecurityUtil.computePasswordDigest(nonce, created, password);
        String digest2 = WSSecurityUtil.computePasswordDigest(nonce, created, password);

        Assertions.assertEquals(digest1, digest2, "Same inputs should produce same password digest");
    }

    @Test
    public void testDoPasswordDigest_strings_withNullNonce() {
        String created = "2024-01-01T12:00:00Z";
        String password = "secretPassword";

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            WSSecurityUtil.computePasswordDigest(null, created, password);
        });
    }

    @Test
    public void testDoPasswordDigest_strings_withNullCreated() {
        String nonce = "randomNonceString";
        String password = "secretPassword";

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            WSSecurityUtil.computePasswordDigest(nonce, null, password);
        });
    }

    @Test
    public void testDoPasswordDigest_strings_withNullPassword() {
        String nonce = "randomNonceString";
        String created = "2024-01-01T12:00:00Z";

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            WSSecurityUtil.computePasswordDigest(nonce, created, null);
        });
    }

    @Test
    public void testDoPasswordDigest_strings_withEmptyStrings() {
        String nonce = "";
        String created = "";
        String password = "";

        String digest = WSSecurityUtil.computePasswordDigest(nonce, created, password);

        Assertions.assertNotNull(digest);
        Assertions.assertTrue(digest.length() > 0);
    }

    @Test
    public void testDoPasswordDigest_orderMatters() {
        byte[] nonce = "nonce".getBytes(StandardCharsets.UTF_8);
        byte[] created = "created".getBytes(StandardCharsets.UTF_8);
        byte[] password = "password".getBytes(StandardCharsets.UTF_8);

        String correctDigest = WSSecurityUtil.computePasswordDigest(nonce, created, password);

        String wrongOrderDigest1 = WSSecurityUtil.computePasswordDigest(created, nonce, password);
        String wrongOrderDigest2 = WSSecurityUtil.computePasswordDigest(password, created, nonce);

        Assertions.assertNotEquals(correctDigest, wrongOrderDigest1);
        Assertions.assertNotEquals(correctDigest, wrongOrderDigest2);
    }

    @Test
    public void testDoPasswordDigest_stringAndByteArrayConsistency() {
        String nonceStr = "testNonce";
        String createdStr = "2024-01-01T12:00:00Z";
        String passwordStr = "testPassword";

        byte[] nonceBytes = nonceStr.getBytes(Charsets.DEFAULT);
        byte[] createdBytes = createdStr.getBytes(Charsets.DEFAULT);
        byte[] passwordBytes = passwordStr.getBytes(Charsets.DEFAULT);

        String digestFromString = WSSecurityUtil.computePasswordDigest(nonceStr, createdStr, passwordStr);
        String digestFromBytes = WSSecurityUtil.computePasswordDigest(nonceBytes, createdBytes, passwordBytes);

        Assertions.assertEquals(digestFromString, digestFromBytes, "String and byte array versions should produce same digest");
    }

    @Test
    public void testThreadSafety_generateNonce() throws InterruptedException {
        final int numThreads = 10;
        final int numIterations = 100;
        Thread[] threads = new Thread[numThreads];

        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < numIterations; j++) {
                    byte[] nonce = WSSecurityUtil.generateNonce(16);
                    Assertions.assertEquals(16, nonce.length);
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }
    }

    @Test
    public void testThreadSafety_generateDigest() throws InterruptedException {
        final int numThreads = 10;
        final int numIterations = 100;
        Thread[] threads = new Thread[numThreads];

        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < numIterations; j++) {
                    String input = "Thread-" + threadId + "-" + j;
                    byte[] digest = WSSecurityUtil.generateDigest(input.getBytes(StandardCharsets.UTF_8));
                    Assertions.assertEquals(20, digest.length);
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }
    }
}
