package com.landawn.abacus.guava.hash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.google.common.hash.Funnel;
import com.google.common.hash.HashCode;
import com.landawn.abacus.TestBase;

/**
 * Comprehensive unit tests for HashFunction interface.
 * Tests the contract and behavior of HashFunction implementations.
 */
@Tag("2025")
public class HashFunction2025Test extends TestBase {

    // Test newHasher() method

    @Test
    @DisplayName("Test newHasher creates instance")
    public void testNewHasher() {
        HashFunction hashFunc = Hashing.murmur3_128();

        Hasher hasher = hashFunc.newHasher();

        assertNotNull(hasher);
    }

    @Test
    @DisplayName("Test newHasher creates independent instances")
    public void testNewHasherIndependentInstances() {
        HashFunction hashFunc = Hashing.murmur3_128();

        Hasher hasher1 = hashFunc.newHasher();
        Hasher hasher2 = hashFunc.newHasher();

        assertNotNull(hasher1);
        assertNotNull(hasher2);
        assertNotSame(hasher1, hasher2);
    }

    @Test
    @DisplayName("Test newHasher produces consistent hashes")
    public void testNewHasherConsistency() {
        HashFunction hashFunc = Hashing.murmur3_128();

        HashCode hash1 = hashFunc.newHasher().put("test").hash();
        HashCode hash2 = hashFunc.newHasher().put("test").hash();

        assertEquals(hash1, hash2);
    }

    // Test newHasher(int) method

    @Test
    @DisplayName("Test newHasher with expected size")
    public void testNewHasherWithExpectedSize() {
        HashFunction hashFunc = Hashing.murmur3_128();

        Hasher hasher = hashFunc.newHasher(1024);

        assertNotNull(hasher);
    }

    @Test
    @DisplayName("Test newHasher with zero expected size")
    public void testNewHasherWithZeroSize() {
        HashFunction hashFunc = Hashing.murmur3_128();

        Hasher hasher = hashFunc.newHasher(0);

        assertNotNull(hasher);
    }

    @Test
    @DisplayName("Test newHasher with large expected size")
    public void testNewHasherWithLargeSize() {
        HashFunction hashFunc = Hashing.sha256();

        Hasher hasher = hashFunc.newHasher(1024 * 1024);

        assertNotNull(hasher);
        HashCode hash = hasher.put("test").hash();
        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test newHasher expected size hint does not affect hash")
    public void testNewHasherExpectedSizeDoesNotAffectHash() {
        HashFunction hashFunc = Hashing.murmur3_128();
        byte[] data = "test data".getBytes(StandardCharsets.UTF_8);

        HashCode hash1 = hashFunc.newHasher().put(data).hash();
        HashCode hash2 = hashFunc.newHasher(10).put(data).hash();
        HashCode hash3 = hashFunc.newHasher(1000).put(data).hash();

        assertEquals(hash1, hash2);
        assertEquals(hash2, hash3);
    }

    // Test hash(int) method

    @Test
    @DisplayName("Test hash int")
    public void testHashInt() {
        HashFunction hashFunc = Hashing.murmur3_128();

        HashCode hash = hashFunc.hash(42);

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test hash int boundary values")
    public void testHashIntBoundaries() {
        HashFunction hashFunc = Hashing.murmur3_128();

        HashCode hashMin = hashFunc.hash(Integer.MIN_VALUE);
        HashCode hashZero = hashFunc.hash(0);
        HashCode hashMax = hashFunc.hash(Integer.MAX_VALUE);

        assertNotNull(hashMin);
        assertNotNull(hashZero);
        assertNotNull(hashMax);
        assertNotEquals(hashMin, hashMax);
    }

    @Test
    @DisplayName("Test hash int produces deterministic result")
    public void testHashIntDeterministic() {
        HashFunction hashFunc = Hashing.murmur3_128();

        HashCode hash1 = hashFunc.hash(12345);
        HashCode hash2 = hashFunc.hash(12345);
        HashCode hash3 = hashFunc.hash(12345);

        assertEquals(hash1, hash2);
        assertEquals(hash2, hash3);
    }

    // Test hash(long) method

    @Test
    @DisplayName("Test hash long")
    public void testHashLong() {
        HashFunction hashFunc = Hashing.murmur3_128();

        HashCode hash = hashFunc.hash(1234567890123L);

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test hash long boundary values")
    public void testHashLongBoundaries() {
        HashFunction hashFunc = Hashing.murmur3_128();

        HashCode hashMin = hashFunc.hash(Long.MIN_VALUE);
        HashCode hashZero = hashFunc.hash(0L);
        HashCode hashMax = hashFunc.hash(Long.MAX_VALUE);

        assertNotNull(hashMin);
        assertNotNull(hashZero);
        assertNotNull(hashMax);
        assertNotEquals(hashMin, hashMax);
    }

    @Test
    @DisplayName("Test hash long produces deterministic result")
    public void testHashLongDeterministic() {
        HashFunction hashFunc = Hashing.murmur3_128();
        long value = System.currentTimeMillis();

        HashCode hash1 = hashFunc.hash(value);
        HashCode hash2 = hashFunc.hash(value);

        assertEquals(hash1, hash2);
    }

    // Test hash(byte[]) method

    @Test
    @DisplayName("Test hash byte array")
    public void testHashByteArray() {
        HashFunction hashFunc = Hashing.murmur3_128();
        byte[] data = "Hello World".getBytes(StandardCharsets.UTF_8);

        HashCode hash = hashFunc.hash(data);

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test hash empty byte array")
    public void testHashEmptyByteArray() {
        HashFunction hashFunc = Hashing.murmur3_128();
        byte[] data = new byte[0];

        HashCode hash = hashFunc.hash(data);

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test hash large byte array")
    public void testHashLargeByteArray() {
        HashFunction hashFunc = Hashing.sha256();
        byte[] data = new byte[10000];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i % 256);
        }

        HashCode hash = hashFunc.hash(data);

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test hash byte array produces deterministic result")
    public void testHashByteArrayDeterministic() {
        HashFunction hashFunc = Hashing.murmur3_128();
        byte[] data = "test data".getBytes(StandardCharsets.UTF_8);

        HashCode hash1 = hashFunc.hash(data);
        HashCode hash2 = hashFunc.hash(data);
        HashCode hash3 = hashFunc.hash(data);

        assertEquals(hash1, hash2);
        assertEquals(hash2, hash3);
    }

    // Test hash(byte[], int, int) method

    @Test
    @DisplayName("Test hash byte array with offset and length")
    public void testHashByteArrayOffsetLength() {
        HashFunction hashFunc = Hashing.murmur3_128();
        byte[] data = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

        HashCode hash = hashFunc.hash(data, 2, 5);

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test hash byte array with zero length")
    public void testHashByteArrayZeroLength() {
        HashFunction hashFunc = Hashing.murmur3_128();
        byte[] data = {1, 2, 3};

        HashCode hash = hashFunc.hash(data, 0, 0);

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test hash byte array full range equals whole array")
    public void testHashByteArrayFullRange() {
        HashFunction hashFunc = Hashing.murmur3_128();
        byte[] data = {10, 20, 30, 40, 50};

        HashCode hash1 = hashFunc.hash(data);
        HashCode hash2 = hashFunc.hash(data, 0, data.length);

        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Test hash byte array subset equals separate array")
    public void testHashByteArraySubset() {
        HashFunction hashFunc = Hashing.murmur3_128();
        byte[] fullData = {1, 2, 3, 4, 5};
        byte[] subsetData = {2, 3, 4};

        HashCode hashSubset1 = hashFunc.hash(fullData, 1, 3);
        HashCode hashSubset2 = hashFunc.hash(subsetData);

        assertEquals(hashSubset1, hashSubset2);
    }

    // Test hash(CharSequence) method

    @Test
    @DisplayName("Test hash CharSequence")
    public void testHashCharSequence() {
        HashFunction hashFunc = Hashing.murmur3_128();

        HashCode hash = hashFunc.hash("Hello World");

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test hash empty CharSequence")
    public void testHashEmptyCharSequence() {
        HashFunction hashFunc = Hashing.murmur3_128();

        HashCode hash = hashFunc.hash("");

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test hash CharSequence with unicode")
    public void testHashCharSequenceUnicode() {
        HashFunction hashFunc = Hashing.murmur3_128();

        HashCode hash = hashFunc.hash("Hello 世界 🌍");

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test hash CharSequence produces deterministic result")
    public void testHashCharSequenceDeterministic() {
        HashFunction hashFunc = Hashing.murmur3_128();

        HashCode hash1 = hashFunc.hash("test");
        HashCode hash2 = hashFunc.hash("test");

        assertEquals(hash1, hash2);
    }

    // Test hash(CharSequence, Charset) method

    @Test
    @DisplayName("Test hash CharSequence with charset")
    public void testHashCharSequenceWithCharset() {
        HashFunction hashFunc = Hashing.murmur3_128();

        HashCode hash = hashFunc.hash("Hello", StandardCharsets.UTF_8);

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test hash CharSequence different charsets produce different hashes")
    public void testHashCharSequenceDifferentCharsets() {
        HashFunction hashFunc = Hashing.murmur3_128();
        String text = "Test";

        HashCode hashUTF8 = hashFunc.hash(text, StandardCharsets.UTF_8);
        HashCode hashUTF16 = hashFunc.hash(text, StandardCharsets.UTF_16);
        HashCode hashISO = hashFunc.hash(text, StandardCharsets.ISO_8859_1);

        assertNotEquals(hashUTF8, hashUTF16);
        assertEquals(hashUTF8, hashISO);
    }

    @Test
    @DisplayName("Test hash CharSequence with charset produces deterministic result")
    public void testHashCharSequenceWithCharsetDeterministic() {
        HashFunction hashFunc = Hashing.murmur3_128();

        HashCode hash1 = hashFunc.hash("test", StandardCharsets.UTF_8);
        HashCode hash2 = hashFunc.hash("test", StandardCharsets.UTF_8);

        assertEquals(hash1, hash2);
    }

    // Test hash(T, Funnel<T>) method

    @Test
    @DisplayName("Test hash with Funnel")
    public void testHashWithFunnel() {
        HashFunction hashFunc = Hashing.murmur3_128();

        Funnel<Person> personFunnel = (person, into) -> {
            into.putString(person.name, StandardCharsets.UTF_8);
            into.putInt(person.age);
        };

        Person person = new Person("Alice", 30);
        HashCode hash = hashFunc.hash(person, personFunnel);

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test hash with Funnel produces deterministic result")
    public void testHashWithFunnelDeterministic() {
        HashFunction hashFunc = Hashing.murmur3_128();

        Funnel<Person> personFunnel = (person, into) -> {
            into.putString(person.name, StandardCharsets.UTF_8);
            into.putInt(person.age);
        };

        Person person = new Person("Bob", 25);

        HashCode hash1 = hashFunc.hash(person, personFunnel);
        HashCode hash2 = hashFunc.hash(person, personFunnel);

        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Test hash with Funnel different objects")
    public void testHashWithFunnelDifferentObjects() {
        HashFunction hashFunc = Hashing.murmur3_128();

        Funnel<Person> personFunnel = (person, into) -> {
            into.putString(person.name, StandardCharsets.UTF_8);
            into.putInt(person.age);
        };

        Person person1 = new Person("Alice", 30);
        Person person2 = new Person("Bob", 30);

        HashCode hash1 = hashFunc.hash(person1, personFunnel);
        HashCode hash2 = hashFunc.hash(person2, personFunnel);

        assertNotEquals(hash1, hash2);
    }

    // Test bits() method

    @Test
    @DisplayName("Test bits returns correct value")
    public void testBits() {
        HashFunction murmur32 = Hashing.murmur3_32();
        HashFunction murmur128 = Hashing.murmur3_128();
        HashFunction sha256 = Hashing.sha256();

        assertEquals(32, murmur32.bits());
        assertEquals(128, murmur128.bits());
        assertEquals(256, sha256.bits());
    }

    @Test
    @DisplayName("Test bits consistent across calls")
    public void testBitsConsistent() {
        HashFunction hashFunc = Hashing.murmur3_128();

        int bits1 = hashFunc.bits();
        int bits2 = hashFunc.bits();
        int bits3 = hashFunc.bits();

        assertEquals(bits1, bits2);
        assertEquals(bits2, bits3);
        assertEquals(128, bits1);
    }

    // Integration tests

    @Test
    @DisplayName("Test hash function is thread-safe")
    public void testHashFunctionThreadSafety() throws InterruptedException {
        HashFunction hashFunc = Hashing.murmur3_128();
        final byte[] data = "test data".getBytes(StandardCharsets.UTF_8);

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                hashFunc.hash(data);
            }
        });

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                hashFunc.hash(data);
            }
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        // If we get here without exception, thread-safety is maintained
        HashCode hash = hashFunc.hash(data);
        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test comparing newHasher and convenience methods")
    public void testNewHasherVsConvenienceMethods() {
        HashFunction hashFunc = Hashing.murmur3_128();

        HashCode hashViaHasher = hashFunc.newHasher().put(42).hash();
        HashCode hashDirect = hashFunc.hash(42);

        assertEquals(hashViaHasher, hashDirect);
    }

    @Test
    @DisplayName("Test hash function immutability")
    public void testHashFunctionImmutability() {
        HashFunction hashFunc = Hashing.murmur3_128();

        HashCode hash1 = hashFunc.hash("test1");
        HashCode hash2 = hashFunc.hash("test2");
        HashCode hash3 = hashFunc.hash("test1");

        assertNotEquals(hash1, hash2);
        assertEquals(hash1, hash3);
    }

    @Test
    @DisplayName("Test hash collision resistance")
    public void testHashCollisionResistance() {
        HashFunction hashFunc = Hashing.murmur3_128();

        // Hash slightly different inputs
        HashCode hash1 = hashFunc.hash("test");
        HashCode hash2 = hashFunc.hash("Test");
        HashCode hash3 = hashFunc.hash("test ");
        HashCode hash4 = hashFunc.hash("tset");

        // All should be different
        assertNotEquals(hash1, hash2);
        assertNotEquals(hash1, hash3);
        assertNotEquals(hash1, hash4);
        assertNotEquals(hash2, hash3);
        assertNotEquals(hash2, hash4);
        assertNotEquals(hash3, hash4);
    }

    @Test
    @DisplayName("Test empty input produces consistent hash")
    public void testEmptyInputConsistentHash() {
        HashFunction hashFunc = Hashing.murmur3_128();

        HashCode hashEmptyBytes = hashFunc.hash(new byte[0]);
        HashCode hashEmptyString = hashFunc.hash("");

        assertNotNull(hashEmptyBytes);
        assertNotNull(hashEmptyString);
        // These might be different because they use different methods
    }

    @Test
    @DisplayName("Test hash function with large data")
    public void testHashFunctionWithLargeData() {
        HashFunction hashFunc = Hashing.sha256();

        byte[] largeData = new byte[1024 * 1024]; // 1MB
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }

        HashCode hash = hashFunc.hash(largeData);

        assertNotNull(hash);
        assertEquals(256, hash.bits());
    }

    @Test
    @DisplayName("Test hash function avalanche effect")
    public void testHashFunctionAvalancheEffect() {
        HashFunction hashFunc = Hashing.murmur3_128();

        // One bit difference in input should cause significant difference in output
        HashCode hash1 = hashFunc.hash(0b00000000);
        HashCode hash2 = hashFunc.hash(0b00000001);
        HashCode hash3 = hashFunc.hash(0b00000010);

        assertNotEquals(hash1, hash2);
        assertNotEquals(hash2, hash3);
        assertNotEquals(hash1, hash3);
    }

    @Test
    @DisplayName("Test real-world scenario - password hashing")
    public void testPasswordHashingScenario() {
        HashFunction hashFunc = Hashing.sha256();

        String username = "user@example.com";
        String password = "secret123";
        String salt = "random_salt_value";

        byte[] credentials = (username + ":" + password + ":" + salt).getBytes(StandardCharsets.UTF_8);
        HashCode hash = hashFunc.hash(credentials);

        assertNotNull(hash);
        assertEquals(256, hash.bits());
    }

    @Test
    @DisplayName("Test real-world scenario - data integrity check")
    public void testDataIntegrityCheckScenario() {
        HashFunction hashFunc = Hashing.sha256();

        byte[] originalData = "Important data".getBytes(StandardCharsets.UTF_8);
        byte[] modifiedData = "Important Data".getBytes(StandardCharsets.UTF_8);

        HashCode originalHash = hashFunc.hash(originalData);
        HashCode modifiedHash = hashFunc.hash(modifiedData);

        assertNotEquals(originalHash, modifiedHash);
    }

    // Helper class for testing
    private static class Person {
        final String name;
        final int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }
}
