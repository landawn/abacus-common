package com.landawn.abacus.guava.hash;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.google.common.hash.Funnel;
import com.google.common.hash.HashCode;
import com.landawn.abacus.TestBase;

/**
 * Comprehensive unit tests for GuavaHashFunction class.
 * Tests all public methods including those inherited from HashFunction interface.
 */
@Tag("2025")
public class GuavaHashFunction2025Test extends TestBase {

    // Test wrap() factory method

    @Test
    @DisplayName("Test wrap creates GuavaHashFunction instance")
    public void testWrap() {
        com.google.common.hash.HashFunction guavaHashFunc = com.google.common.hash.Hashing.murmur3_128();
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(guavaHashFunc);

        assertNotNull(hashFunc);
        assertSame(guavaHashFunc, hashFunc.gHashFunction);
    }

    @Test
    @DisplayName("Test wrap with different Guava hash functions")
    public void testWrapWithDifferentHashFunctions() {
        com.google.common.hash.HashFunction murmur3 = com.google.common.hash.Hashing.murmur3_32_fixed();
        com.google.common.hash.HashFunction sha256 = com.google.common.hash.Hashing.sha256();

        GuavaHashFunction hashFunc1 = GuavaHashFunction.wrap(murmur3);
        GuavaHashFunction hashFunc2 = GuavaHashFunction.wrap(sha256);

        assertNotNull(hashFunc1);
        assertNotNull(hashFunc2);
        assertNotSame(hashFunc1.gHashFunction, hashFunc2.gHashFunction);
    }

    // Test newHasher() method

    @Test
    @DisplayName("Test newHasher creates Hasher instance")
    public void testNewHasher() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());

        Hasher hasher = hashFunc.newHasher();

        assertNotNull(hasher);
        assertTrue(hasher instanceof GuavaHasher);
    }

    @Test
    @DisplayName("Test newHasher creates independent instances")
    public void testNewHasherIndependentInstances() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());

        Hasher hasher1 = hashFunc.newHasher();
        Hasher hasher2 = hashFunc.newHasher();

        assertNotNull(hasher1);
        assertNotNull(hasher2);
        assertNotSame(hasher1, hasher2);
    }

    @Test
    @DisplayName("Test newHasher can be used for hashing")
    public void testNewHasherUsage() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());

        Hasher hasher = hashFunc.newHasher();
        HashCode hash = hasher.put("test data").hash();

        assertNotNull(hash);
    }

    // Test newHasher(int) method

    @Test
    @DisplayName("Test newHasher with expected input size")
    public void testNewHasherWithExpectedSize() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());

        Hasher hasher = hashFunc.newHasher(1024);

        assertNotNull(hasher);
        assertTrue(hasher instanceof GuavaHasher);
    }

    @Test
    @DisplayName("Test newHasher with zero expected size")
    public void testNewHasherWithZeroSize() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());

        Hasher hasher = hashFunc.newHasher(0);

        assertNotNull(hasher);
    }

    @Test
    @DisplayName("Test newHasher with large expected size")
    public void testNewHasherWithLargeSize() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());

        Hasher hasher = hashFunc.newHasher(1024 * 1024);

        assertNotNull(hasher);
        HashCode hash = hasher.put("test").hash();
        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test newHasher with expected size produces same hash")
    public void testNewHasherExpectedSizeSameHash() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());

        byte[] data = "test data".getBytes(StandardCharsets.UTF_8);

        HashCode hash1 = hashFunc.newHasher().put(data).hash();
        HashCode hash2 = hashFunc.newHasher(data.length).put(data).hash();

        assertEquals(hash1, hash2);
    }

    // Test hash(int) method

    @Test
    @DisplayName("Test hash int")
    public void testHashInt() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());

        HashCode hash = hashFunc.hash(42);

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test hash int boundary values")
    public void testHashIntBoundaries() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());

        HashCode hashMin = hashFunc.hash(Integer.MIN_VALUE);
        HashCode hashZero = hashFunc.hash(0);
        HashCode hashMax = hashFunc.hash(Integer.MAX_VALUE);

        assertNotNull(hashMin);
        assertNotNull(hashZero);
        assertNotNull(hashMax);
        assertNotEquals(hashMin, hashMax);
    }

    @Test
    @DisplayName("Test hash int consistency")
    public void testHashIntConsistency() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());

        HashCode hash1 = hashFunc.hash(12345);
        HashCode hash2 = hashFunc.hash(12345);

        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Test hash different ints produce different hashes")
    public void testHashDifferentInts() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());

        HashCode hash1 = hashFunc.hash(1);
        HashCode hash2 = hashFunc.hash(2);

        assertNotEquals(hash1, hash2);
    }

    // Test hash(long) method

    @Test
    @DisplayName("Test hash long")
    public void testHashLong() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());

        HashCode hash = hashFunc.hash(1234567890123L);

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test hash long boundary values")
    public void testHashLongBoundaries() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());

        HashCode hashMin = hashFunc.hash(Long.MIN_VALUE);
        HashCode hashZero = hashFunc.hash(0L);
        HashCode hashMax = hashFunc.hash(Long.MAX_VALUE);

        assertNotNull(hashMin);
        assertNotNull(hashZero);
        assertNotNull(hashMax);
        assertNotEquals(hashMin, hashMax);
    }

    @Test
    @DisplayName("Test hash long consistency")
    public void testHashLongConsistency() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());

        long value = System.currentTimeMillis();
        HashCode hash1 = hashFunc.hash(value);
        HashCode hash2 = hashFunc.hash(value);

        assertEquals(hash1, hash2);
    }

    // Test hash(byte[]) method

    @Test
    @DisplayName("Test hash byte array")
    public void testHashByteArray() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());
        byte[] data = "Hello World".getBytes(StandardCharsets.UTF_8);

        HashCode hash = hashFunc.hash(data);

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test hash empty byte array")
    public void testHashEmptyByteArray() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());
        byte[] data = new byte[0];

        HashCode hash = hashFunc.hash(data);

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test hash large byte array")
    public void testHashLargeByteArray() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());
        byte[] data = new byte[10000];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i % 256);
        }

        HashCode hash = hashFunc.hash(data);

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test hash byte array consistency")
    public void testHashByteArrayConsistency() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());
        byte[] data = "test data".getBytes(StandardCharsets.UTF_8);

        HashCode hash1 = hashFunc.hash(data);
        HashCode hash2 = hashFunc.hash(data);

        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Test hash different byte arrays produce different hashes")
    public void testHashDifferentByteArrays() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());

        byte[] data1 = "data1".getBytes(StandardCharsets.UTF_8);
        byte[] data2 = "data2".getBytes(StandardCharsets.UTF_8);

        HashCode hash1 = hashFunc.hash(data1);
        HashCode hash2 = hashFunc.hash(data2);

        assertNotEquals(hash1, hash2);
    }

    // Test hash(byte[], int, int) method

    @Test
    @DisplayName("Test hash byte array with offset and length")
    public void testHashByteArrayOffsetLength() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());
        byte[] data = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

        HashCode hash = hashFunc.hash(data, 2, 5);

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test hash byte array with zero length")
    public void testHashByteArrayZeroLength() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());
        byte[] data = {1, 2, 3};

        HashCode hash = hashFunc.hash(data, 0, 0);

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test hash byte array full range")
    public void testHashByteArrayFullRange() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());
        byte[] data = {10, 20, 30, 40, 50};

        HashCode hash1 = hashFunc.hash(data);
        HashCode hash2 = hashFunc.hash(data, 0, data.length);

        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Test hash byte array subset")
    public void testHashByteArraySubset() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());
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
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());

        HashCode hash = hashFunc.hash("Hello World");

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test hash empty CharSequence")
    public void testHashEmptyCharSequence() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());

        HashCode hash = hashFunc.hash("");

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test hash CharSequence with StringBuilder")
    public void testHashCharSequenceStringBuilder() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());
        StringBuilder sb = new StringBuilder("test");

        HashCode hash = hashFunc.hash(sb);

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test hash CharSequence with unicode")
    public void testHashCharSequenceUnicode() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());

        HashCode hash = hashFunc.hash("Hello 世界 🌍");

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test hash CharSequence consistency")
    public void testHashCharSequenceConsistency() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());

        HashCode hash1 = hashFunc.hash("test");
        HashCode hash2 = hashFunc.hash("test");

        assertEquals(hash1, hash2);
    }

    // Test hash(CharSequence, Charset) method

    @Test
    @DisplayName("Test hash CharSequence with charset")
    public void testHashCharSequenceWithCharset() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());

        HashCode hash = hashFunc.hash("Hello", StandardCharsets.UTF_8);

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test hash CharSequence with different charsets")
    public void testHashCharSequenceMultipleCharsets() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());
        String text = "Test";

        HashCode hashUTF8 = hashFunc.hash(text, StandardCharsets.UTF_8);
        HashCode hashUTF16 = hashFunc.hash(text, StandardCharsets.UTF_16);

        assertNotEquals(hashUTF8, hashUTF16);
    }

    @Test
    @DisplayName("Test hash CharSequence with charset unicode")
    public void testHashCharSequenceWithCharsetUnicode() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());

        HashCode hash = hashFunc.hash("世界", StandardCharsets.UTF_8);

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test hash CharSequence with charset consistency")
    public void testHashCharSequenceWithCharsetConsistency() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());

        HashCode hash1 = hashFunc.hash("test", StandardCharsets.UTF_8);
        HashCode hash2 = hashFunc.hash("test", StandardCharsets.UTF_8);

        assertEquals(hash1, hash2);
    }

    // Test hash(T, Funnel<T>) method

    @Test
    @DisplayName("Test hash with Funnel")
    public void testHashWithFunnel() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());

        Funnel<Person> personFunnel = (person, into) -> {
            into.putString(person.name, StandardCharsets.UTF_8);
            into.putInt(person.age);
        };

        Person person = new Person("Alice", 30);
        HashCode hash = hashFunc.hash(person, personFunnel);

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test hash with Funnel consistency")
    public void testHashWithFunnelConsistency() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());

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
    @DisplayName("Test hash with complex Funnel")
    public void testHashWithComplexFunnel() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());

        Funnel<Person> complexFunnel = (person, into) -> {
            into.putString(person.name, StandardCharsets.UTF_8);
            into.putInt(person.age);
            into.putBoolean(person.age >= 18);
            into.putLong(person.name.hashCode());
        };

        Person person = new Person("Charlie", 35);
        HashCode hash = hashFunc.hash(person, complexFunnel);

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test hash with Funnel different objects")
    public void testHashWithFunnelDifferentObjects() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());

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
    @DisplayName("Test bits for murmur3_32")
    public void testBitsMurmur32() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_32_fixed());

        int bits = hashFunc.bits();

        assertEquals(32, bits);
    }

    @Test
    @DisplayName("Test bits for murmur3_128")
    public void testBitsMurmur128() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());

        int bits = hashFunc.bits();

        assertEquals(128, bits);
    }

    @Test
    @DisplayName("Test bits for sha256")
    public void testBitsSha256() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.sha256());

        int bits = hashFunc.bits();

        assertEquals(256, bits);
    }

    @Test
    @DisplayName("Test bits for sha512")
    public void testBitsSha512() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.sha512());

        int bits = hashFunc.bits();

        assertEquals(512, bits);
    }

    @Test
    @DisplayName("Test bits for md5")
    public void testBitsMd5() {
        @SuppressWarnings("deprecation")
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.md5());

        int bits = hashFunc.bits();

        assertEquals(128, bits);
    }

    @Test
    @DisplayName("Test bits for crc32")
    public void testBitsCrc32() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.crc32());

        int bits = hashFunc.bits();

        assertEquals(32, bits);
    }

    // Integration tests

    @Test
    @DisplayName("Test hash function with different algorithms")
    public void testDifferentAlgorithms() {
        byte[] data = "test data".getBytes(StandardCharsets.UTF_8);

        HashCode murmur32Hash = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_32_fixed())
            .hash(data);

        HashCode murmur128Hash = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128())
            .hash(data);

        HashCode sha256Hash = GuavaHashFunction.wrap(com.google.common.hash.Hashing.sha256())
            .hash(data);

        assertNotNull(murmur32Hash);
        assertNotNull(murmur128Hash);
        assertNotNull(sha256Hash);

        assertEquals(32, murmur32Hash.bits());
        assertEquals(128, murmur128Hash.bits());
        assertEquals(256, sha256Hash.bits());
    }

    @Test
    @DisplayName("Test hash function immutability")
    public void testHashFunctionImmutability() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());

        HashCode hash1 = hashFunc.hash("test1");
        HashCode hash2 = hashFunc.hash("test2");
        HashCode hash3 = hashFunc.hash("test1");

        assertNotEquals(hash1, hash2);
        assertEquals(hash1, hash3);
    }

    @Test
    @DisplayName("Test comparing newHasher and convenience methods")
    public void testNewHasherVsConvenienceMethods() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());

        // Using newHasher
        HashCode hashViaHasher = hashFunc.newHasher()
            .put(42)
            .hash();

        // Using convenience method
        HashCode hashDirect = hashFunc.hash(42);

        assertEquals(hashViaHasher, hashDirect);
    }

    @Test
    @DisplayName("Test hashing user authentication scenario")
    public void testUserAuthenticationScenario() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.sha256());

        String username = "user@example.com";
        String password = "secret123";

        byte[] credentials = (username + ":" + password).getBytes(StandardCharsets.UTF_8);
        HashCode hash = hashFunc.hash(credentials);

        assertNotNull(hash);
        assertEquals(256, hash.bits());
    }

    @Test
    @DisplayName("Test hashing file checksum scenario")
    public void testFileChecksumScenario() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.sha256());

        byte[] fileContent = "file content here".getBytes(StandardCharsets.UTF_8);
        HashCode checksum = hashFunc.hash(fileContent);

        assertNotNull(checksum);
        assertTrue(checksum.toString().length() > 0);
    }

    @Test
    @DisplayName("Test consistent hashing with same function")
    public void testConsistentHashingWithSameFunction() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());

        String data = "consistent data";

        HashCode hash1 = hashFunc.hash(data);
        HashCode hash2 = hashFunc.hash(data);
        HashCode hash3 = hashFunc.hash(data);

        assertEquals(hash1, hash2);
        assertEquals(hash2, hash3);
    }

    @Test
    @DisplayName("Test hash distribution with sequential inputs")
    public void testHashDistribution() {
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());

        HashCode hash1 = hashFunc.hash(1);
        HashCode hash2 = hashFunc.hash(2);
        HashCode hash3 = hashFunc.hash(3);

        // All hashes should be different
        assertNotEquals(hash1, hash2);
        assertNotEquals(hash2, hash3);
        assertNotEquals(hash1, hash3);
    }

    @Test
    @DisplayName("Test wrapping null Guava hash function")
    public void testWrapNull() {
        assertThrows(NullPointerException.class, () -> {
            GuavaHashFunction.wrap(null);
        });
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
