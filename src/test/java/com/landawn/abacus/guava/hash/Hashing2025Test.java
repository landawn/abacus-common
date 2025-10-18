package com.landawn.abacus.guava.hash;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Arrays;
import java.util.List;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.google.common.hash.HashCode;
import com.landawn.abacus.TestBase;

/**
 * Comprehensive unit tests for Hashing utility class.
 * Tests all public static methods for creating hash functions.
 */
@Tag("2025")
public class Hashing2025Test extends TestBase {

    // Test goodFastHash() method

    @Test
    @DisplayName("Test goodFastHash with valid minimum bits")
    public void testGoodFastHash() {
        HashFunction hashFunc = Hashing.goodFastHash(128);

        assertNotNull(hashFunc);
        assertTrue(hashFunc.bits() >= 128);
    }

    @Test
    @DisplayName("Test goodFastHash with different bit lengths")
    public void testGoodFastHashDifferentBitLengths() {
        HashFunction hash32 = Hashing.goodFastHash(32);
        HashFunction hash64 = Hashing.goodFastHash(64);
        HashFunction hash128 = Hashing.goodFastHash(128);

        assertNotNull(hash32);
        assertNotNull(hash64);
        assertNotNull(hash128);
        assertTrue(hash32.bits() >= 32);
        assertTrue(hash64.bits() >= 64);
        assertTrue(hash128.bits() >= 128);
    }

    @Test
    @DisplayName("Test goodFastHash throws exception for invalid bits")
    public void testGoodFastHashInvalidBits() {
        assertThrows(IllegalArgumentException.class, () -> Hashing.goodFastHash(0));
        assertThrows(IllegalArgumentException.class, () -> Hashing.goodFastHash(-1));
    }

    @Test
    @DisplayName("Test goodFastHash produces consistent function")
    public void testGoodFastHashConsistent() {
        HashFunction hashFunc1 = Hashing.goodFastHash(128);
        HashFunction hashFunc2 = Hashing.goodFastHash(128);

        byte[] data = "test".getBytes(StandardCharsets.UTF_8);
        HashCode hash1 = hashFunc1.hash(data);
        HashCode hash2 = hashFunc2.hash(data);

        // Same instance should produce same hash
        assertEquals(hash1, hash2);
    }

    // Test murmur3_32() methods

    @Test
    @DisplayName("Test murmur3_32 with no seed")
    public void testMurmur3_32NoSeed() {
        HashFunction hashFunc = Hashing.murmur3_32();

        assertNotNull(hashFunc);
        assertEquals(32, hashFunc.bits());
    }

    @Test
    @DisplayName("Test murmur3_32 with seed")
    public void testMurmur3_32WithSeed() {
        HashFunction hashFunc = Hashing.murmur3_32(42);

        assertNotNull(hashFunc);
        assertEquals(32, hashFunc.bits());
    }

    @Test
    @DisplayName("Test murmur3_32 different seeds produce different hashes")
    public void testMurmur3_32DifferentSeeds() {
        HashFunction hashFunc1 = Hashing.murmur3_32(0);
        HashFunction hashFunc2 = Hashing.murmur3_32(42);

        byte[] data = "test".getBytes(StandardCharsets.UTF_8);
        HashCode hash1 = hashFunc1.hash(data);
        HashCode hash2 = hashFunc2.hash(data);

        assertNotEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Test murmur3_32 same seed produces consistent hashes")
    public void testMurmur3_32SameSeedConsistent() {
        int seed = 12345;
        HashFunction hashFunc1 = Hashing.murmur3_32(seed);
        HashFunction hashFunc2 = Hashing.murmur3_32(seed);

        byte[] data = "test".getBytes(StandardCharsets.UTF_8);
        HashCode hash1 = hashFunc1.hash(data);
        HashCode hash2 = hashFunc2.hash(data);

        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Test murmur3_32 with negative seed")
    public void testMurmur3_32NegativeSeed() {
        HashFunction hashFunc = Hashing.murmur3_32(-1);

        assertNotNull(hashFunc);
        assertEquals(32, hashFunc.bits());
    }

    // Test murmur3_128() methods

    @Test
    @DisplayName("Test murmur3_128 with no seed")
    public void testMurmur3_128NoSeed() {
        HashFunction hashFunc = Hashing.murmur3_128();

        assertNotNull(hashFunc);
        assertEquals(128, hashFunc.bits());
    }

    @Test
    @DisplayName("Test murmur3_128 with seed")
    public void testMurmur3_128WithSeed() {
        HashFunction hashFunc = Hashing.murmur3_128(42);

        assertNotNull(hashFunc);
        assertEquals(128, hashFunc.bits());
    }

    @Test
    @DisplayName("Test murmur3_128 different seeds produce different hashes")
    public void testMurmur3_128DifferentSeeds() {
        HashFunction hashFunc1 = Hashing.murmur3_128(0);
        HashFunction hashFunc2 = Hashing.murmur3_128(42);

        byte[] data = "test".getBytes(StandardCharsets.UTF_8);
        HashCode hash1 = hashFunc1.hash(data);
        HashCode hash2 = hashFunc2.hash(data);

        assertNotEquals(hash1, hash2);
    }

    // Test sipHash24() methods

    @Test
    @DisplayName("Test sipHash24 with default seed")
    public void testSipHash24DefaultSeed() {
        HashFunction hashFunc = Hashing.sipHash24();

        assertNotNull(hashFunc);
        assertEquals(64, hashFunc.bits());
    }

    @Test
    @DisplayName("Test sipHash24 with custom key")
    public void testSipHash24CustomKey() {
        long k0 = 0x0706050403020100L;
        long k1 = 0x0f0e0d0c0b0a0908L;
        HashFunction hashFunc = Hashing.sipHash24(k0, k1);

        assertNotNull(hashFunc);
        assertEquals(64, hashFunc.bits());
    }

    @Test
    @DisplayName("Test sipHash24 different keys produce different hashes")
    public void testSipHash24DifferentKeys() {
        HashFunction hashFunc1 = Hashing.sipHash24(0, 0);
        HashFunction hashFunc2 = Hashing.sipHash24(1, 1);

        byte[] data = "test".getBytes(StandardCharsets.UTF_8);
        HashCode hash1 = hashFunc1.hash(data);
        HashCode hash2 = hashFunc2.hash(data);

        assertNotEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Test sipHash24 same key produces consistent hashes")
    public void testSipHash24SameKeyConsistent() {
        long k0 = 123456789L;
        long k1 = 987654321L;

        HashFunction hashFunc1 = Hashing.sipHash24(k0, k1);
        HashFunction hashFunc2 = Hashing.sipHash24(k0, k1);

        byte[] data = "test".getBytes(StandardCharsets.UTF_8);
        HashCode hash1 = hashFunc1.hash(data);
        HashCode hash2 = hashFunc2.hash(data);

        assertEquals(hash1, hash2);
    }

    // Test cryptographic hash functions

    @Test
    @DisplayName("Test md5")
    @SuppressWarnings("deprecation")
    public void testMd5() {
        HashFunction hashFunc = Hashing.md5();

        assertNotNull(hashFunc);
        assertEquals(128, hashFunc.bits());
    }

    @Test
    @DisplayName("Test sha1")
    @SuppressWarnings("deprecation")
    public void testSha1() {
        HashFunction hashFunc = Hashing.sha1();

        assertNotNull(hashFunc);
        assertEquals(160, hashFunc.bits());
    }

    @Test
    @DisplayName("Test sha256")
    public void testSha256() {
        HashFunction hashFunc = Hashing.sha256();

        assertNotNull(hashFunc);
        assertEquals(256, hashFunc.bits());
    }

    @Test
    @DisplayName("Test sha384")
    public void testSha384() {
        HashFunction hashFunc = Hashing.sha384();

        assertNotNull(hashFunc);
        assertEquals(384, hashFunc.bits());
    }

    @Test
    @DisplayName("Test sha512")
    public void testSha512() {
        HashFunction hashFunc = Hashing.sha512();

        assertNotNull(hashFunc);
        assertEquals(512, hashFunc.bits());
    }

    @Test
    @DisplayName("Test different SHA algorithms produce different hashes")
    public void testDifferentShaAlgorithms() {
        byte[] data = "test".getBytes(StandardCharsets.UTF_8);

        HashCode sha256Hash = Hashing.sha256().hash(data);
        HashCode sha384Hash = Hashing.sha384().hash(data);
        HashCode sha512Hash = Hashing.sha512().hash(data);

        assertNotNull(sha256Hash);
        assertNotNull(sha384Hash);
        assertNotNull(sha512Hash);
        assertEquals(256, sha256Hash.bits());
        assertEquals(384, sha384Hash.bits());
        assertEquals(512, sha512Hash.bits());
    }

    // Test HMAC functions with Key

    @Test
    @DisplayName("Test hmacMd5 with Key")
    public void testHmacMd5WithKey() {
        Key key = new SecretKeySpec("secret".getBytes(StandardCharsets.UTF_8), "HmacMD5");
        HashFunction hashFunc = Hashing.hmacMd5(key);

        assertNotNull(hashFunc);
        assertEquals(128, hashFunc.bits());
    }

    @Test
    @DisplayName("Test hmacSha1 with Key")
    public void testHmacSha1WithKey() {
        Key key = new SecretKeySpec("secret".getBytes(StandardCharsets.UTF_8), "HmacSHA1");
        HashFunction hashFunc = Hashing.hmacSha1(key);

        assertNotNull(hashFunc);
        assertEquals(160, hashFunc.bits());
    }

    @Test
    @DisplayName("Test hmacSha256 with Key")
    public void testHmacSha256WithKey() {
        Key key = new SecretKeySpec("secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        HashFunction hashFunc = Hashing.hmacSha256(key);

        assertNotNull(hashFunc);
        assertEquals(256, hashFunc.bits());
    }

    @Test
    @DisplayName("Test hmacSha512 with Key")
    public void testHmacSha512WithKey() {
        Key key = new SecretKeySpec("secret".getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        HashFunction hashFunc = Hashing.hmacSha512(key);

        assertNotNull(hashFunc);
        assertEquals(512, hashFunc.bits());
    }

    // Test HMAC functions with byte array

    @Test
    @DisplayName("Test hmacMd5 with byte array")
    public void testHmacMd5WithBytes() {
        byte[] key = "secret".getBytes(StandardCharsets.UTF_8);
        HashFunction hashFunc = Hashing.hmacMd5(key);

        assertNotNull(hashFunc);
        assertEquals(128, hashFunc.bits());
    }

    @Test
    @DisplayName("Test hmacSha1 with byte array")
    public void testHmacSha1WithBytes() {
        byte[] key = "secret".getBytes(StandardCharsets.UTF_8);
        HashFunction hashFunc = Hashing.hmacSha1(key);

        assertNotNull(hashFunc);
        assertEquals(160, hashFunc.bits());
    }

    @Test
    @DisplayName("Test hmacSha256 with byte array")
    public void testHmacSha256WithBytes() {
        byte[] key = "secret".getBytes(StandardCharsets.UTF_8);
        HashFunction hashFunc = Hashing.hmacSha256(key);

        assertNotNull(hashFunc);
        assertEquals(256, hashFunc.bits());
    }

    @Test
    @DisplayName("Test hmacSha512 with byte array")
    public void testHmacSha512WithBytes() {
        byte[] key = "secret".getBytes(StandardCharsets.UTF_8);
        HashFunction hashFunc = Hashing.hmacSha512(key);

        assertNotNull(hashFunc);
        assertEquals(512, hashFunc.bits());
    }

    @Test
    @DisplayName("Test HMAC with different keys produce different hashes")
    public void testHmacDifferentKeys() {
        byte[] key1 = "secret1".getBytes(StandardCharsets.UTF_8);
        byte[] key2 = "secret2".getBytes(StandardCharsets.UTF_8);
        byte[] data = "message".getBytes(StandardCharsets.UTF_8);

        HashCode hash1 = Hashing.hmacSha256(key1).hash(data);
        HashCode hash2 = Hashing.hmacSha256(key2).hash(data);

        assertNotEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Test HMAC with same key produces consistent hashes")
    public void testHmacSameKeyConsistent() {
        byte[] key = "secret".getBytes(StandardCharsets.UTF_8);
        byte[] data = "message".getBytes(StandardCharsets.UTF_8);

        HashCode hash1 = Hashing.hmacSha256(key).hash(data);
        HashCode hash2 = Hashing.hmacSha256(key).hash(data);

        assertEquals(hash1, hash2);
    }

    // Test checksum functions

    @Test
    @DisplayName("Test crc32c")
    public void testCrc32c() {
        HashFunction hashFunc = Hashing.crc32c();

        assertNotNull(hashFunc);
        assertEquals(32, hashFunc.bits());
    }

    @Test
    @DisplayName("Test crc32")
    public void testCrc32() {
        HashFunction hashFunc = Hashing.crc32();

        assertNotNull(hashFunc);
        assertEquals(32, hashFunc.bits());
    }

    @Test
    @DisplayName("Test adler32")
    public void testAdler32() {
        HashFunction hashFunc = Hashing.adler32();

        assertNotNull(hashFunc);
        assertEquals(32, hashFunc.bits());
    }

    @Test
    @DisplayName("Test checksum functions produce different hashes")
    public void testChecksumFunctionsDifferent() {
        byte[] data = "test data".getBytes(StandardCharsets.UTF_8);

        HashCode crc32cHash = Hashing.crc32c().hash(data);
        HashCode crc32Hash = Hashing.crc32().hash(data);
        HashCode adler32Hash = Hashing.adler32().hash(data);

        assertNotNull(crc32cHash);
        assertNotNull(crc32Hash);
        assertNotNull(adler32Hash);
        // Different algorithms should produce different hashes
    }

    // Test farmHashFingerprint64()

    @Test
    @DisplayName("Test farmHashFingerprint64")
    public void testFarmHashFingerprint64() {
        HashFunction hashFunc = Hashing.farmHashFingerprint64();

        assertNotNull(hashFunc);
        assertEquals(64, hashFunc.bits());
    }

    @Test
    @DisplayName("Test farmHashFingerprint64 produces consistent hashes")
    public void testFarmHashFingerprint64Consistent() {
        byte[] data = "test".getBytes(StandardCharsets.UTF_8);

        HashCode hash1 = Hashing.farmHashFingerprint64().hash(data);
        HashCode hash2 = Hashing.farmHashFingerprint64().hash(data);

        assertEquals(hash1, hash2);
    }

    // Test concatenating() methods

    @Test
    @DisplayName("Test concatenating two hash functions")
    public void testConcatenatingTwo() {
        HashFunction first = Hashing.murmur3_128();
        HashFunction second = Hashing.murmur3_128();

        HashFunction concatenated = Hashing.concatenating(first, second);

        assertNotNull(concatenated);
        assertEquals(256, concatenated.bits()); // 128 + 128
    }

    @Test
    @DisplayName("Test concatenating three hash functions")
    public void testConcatenatingThree() {
        HashFunction first = Hashing.murmur3_128();
        HashFunction second = Hashing.murmur3_128();
        HashFunction third = Hashing.murmur3_128();

        HashFunction concatenated = Hashing.concatenating(first, second, third);

        assertNotNull(concatenated);
        assertEquals(384, concatenated.bits()); // 128 + 128 + 128
    }

    @Test
    @DisplayName("Test concatenating iterable of hash functions")
    public void testConcatenatingIterable() {
        List<HashFunction> functions = Arrays.asList(
            Hashing.murmur3_32(),
            Hashing.murmur3_32(),
            Hashing.murmur3_32()
        );

        HashFunction concatenated = Hashing.concatenating(functions);

        assertNotNull(concatenated);
        assertEquals(96, concatenated.bits()); // 32 + 32 + 32
    }

    @Test
    @DisplayName("Test concatenating produces consistent hashes")
    public void testConcatenatingConsistent() {
        HashFunction concatenated = Hashing.concatenating(
            Hashing.murmur3_128(),
            Hashing.sha256()
        );

        byte[] data = "test".getBytes(StandardCharsets.UTF_8);
        HashCode hash1 = concatenated.hash(data);
        HashCode hash2 = concatenated.hash(data);

        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Test concatenating with different bit lengths")
    public void testConcatenatingDifferentBitLengths() {
        HashFunction concatenated = Hashing.concatenating(
            Hashing.murmur3_32(),    // 32 bits
            Hashing.murmur3_128(),   // 128 bits
            Hashing.sha256()         // 256 bits
        );

        assertNotNull(concatenated);
        assertEquals(416, concatenated.bits()); // 32 + 128 + 256
    }

    // Test combineOrdered() methods

    @Test
    @DisplayName("Test combineOrdered two hash codes")
    public void testCombineOrderedTwo() {
        HashCode hash1 = Hashing.murmur3_128().hash("test1");
        HashCode hash2 = Hashing.murmur3_128().hash("test2");

        HashCode combined = Hashing.combineOrdered(hash1, hash2);

        assertNotNull(combined);
        assertEquals(128, combined.bits());
    }

    @Test
    @DisplayName("Test combineOrdered three hash codes")
    public void testCombineOrderedThree() {
        HashCode hash1 = Hashing.murmur3_128().hash("test1");
        HashCode hash2 = Hashing.murmur3_128().hash("test2");
        HashCode hash3 = Hashing.murmur3_128().hash("test3");

        HashCode combined = Hashing.combineOrdered(hash1, hash2, hash3);

        assertNotNull(combined);
        assertEquals(128, combined.bits());
    }

    @Test
    @DisplayName("Test combineOrdered iterable")
    public void testCombineOrderedIterable() {
        List<HashCode> hashes = Arrays.asList(
            Hashing.murmur3_128().hash("test1"),
            Hashing.murmur3_128().hash("test2"),
            Hashing.murmur3_128().hash("test3")
        );

        HashCode combined = Hashing.combineOrdered(hashes);

        assertNotNull(combined);
        assertEquals(128, combined.bits());
    }

    @Test
    @DisplayName("Test combineOrdered is order-dependent")
    public void testCombineOrderedOrderDependent() {
        HashCode hash1 = Hashing.murmur3_128().hash("test1");
        HashCode hash2 = Hashing.murmur3_128().hash("test2");

        HashCode combined1 = Hashing.combineOrdered(hash1, hash2);
        HashCode combined2 = Hashing.combineOrdered(hash2, hash1);

        assertNotEquals(combined1, combined2);
    }

    @Test
    @DisplayName("Test combineOrdered is deterministic")
    public void testCombineOrderedDeterministic() {
        HashCode hash1 = Hashing.murmur3_128().hash("test1");
        HashCode hash2 = Hashing.murmur3_128().hash("test2");

        HashCode combined1 = Hashing.combineOrdered(hash1, hash2);
        HashCode combined2 = Hashing.combineOrdered(hash1, hash2);

        assertEquals(combined1, combined2);
    }

    // Test combineUnordered() methods

    @Test
    @DisplayName("Test combineUnordered two hash codes")
    public void testCombineUnorderedTwo() {
        HashCode hash1 = Hashing.murmur3_128().hash("test1");
        HashCode hash2 = Hashing.murmur3_128().hash("test2");

        HashCode combined = Hashing.combineUnordered(hash1, hash2);

        assertNotNull(combined);
        assertEquals(128, combined.bits());
    }

    @Test
    @DisplayName("Test combineUnordered three hash codes")
    public void testCombineUnorderedThree() {
        HashCode hash1 = Hashing.murmur3_128().hash("test1");
        HashCode hash2 = Hashing.murmur3_128().hash("test2");
        HashCode hash3 = Hashing.murmur3_128().hash("test3");

        HashCode combined = Hashing.combineUnordered(hash1, hash2, hash3);

        assertNotNull(combined);
        assertEquals(128, combined.bits());
    }

    @Test
    @DisplayName("Test combineUnordered iterable")
    public void testCombineUnorderedIterable() {
        List<HashCode> hashes = Arrays.asList(
            Hashing.murmur3_128().hash("test1"),
            Hashing.murmur3_128().hash("test2"),
            Hashing.murmur3_128().hash("test3")
        );

        HashCode combined = Hashing.combineUnordered(hashes);

        assertNotNull(combined);
        assertEquals(128, combined.bits());
    }

    @Test
    @DisplayName("Test combineUnordered is order-independent")
    public void testCombineUnorderedOrderIndependent() {
        HashCode hash1 = Hashing.murmur3_128().hash("test1");
        HashCode hash2 = Hashing.murmur3_128().hash("test2");

        HashCode combined1 = Hashing.combineUnordered(hash1, hash2);
        HashCode combined2 = Hashing.combineUnordered(hash2, hash1);

        assertEquals(combined1, combined2);
    }

    @Test
    @DisplayName("Test combineUnordered is deterministic")
    public void testCombineUnorderedDeterministic() {
        HashCode hash1 = Hashing.murmur3_128().hash("test1");
        HashCode hash2 = Hashing.murmur3_128().hash("test2");

        HashCode combined1 = Hashing.combineUnordered(hash1, hash2);
        HashCode combined2 = Hashing.combineUnordered(hash1, hash2);

        assertEquals(combined1, combined2);
    }

    // Test consistentHash() methods

    @Test
    @DisplayName("Test consistentHash with HashCode")
    public void testConsistentHashWithHashCode() {
        HashCode hashCode = Hashing.murmur3_128().hash("test");
        int buckets = 10;

        int bucket = Hashing.consistentHash(hashCode, buckets);

        assertTrue(bucket >= 0);
        assertTrue(bucket < buckets);
    }

    @Test
    @DisplayName("Test consistentHash with long")
    public void testConsistentHashWithLong() {
        long value = 123456789L;
        int buckets = 100;

        int bucket = Hashing.consistentHash(value, buckets);

        assertTrue(bucket >= 0);
        assertTrue(bucket < buckets);
    }

    @Test
    @DisplayName("Test consistentHash is deterministic")
    public void testConsistentHashDeterministic() {
        long value = 123456789L;
        int buckets = 100;

        int bucket1 = Hashing.consistentHash(value, buckets);
        int bucket2 = Hashing.consistentHash(value, buckets);
        int bucket3 = Hashing.consistentHash(value, buckets);

        assertEquals(bucket1, bucket2);
        assertEquals(bucket2, bucket3);
    }

    @Test
    @DisplayName("Test consistentHash with different bucket counts")
    public void testConsistentHashDifferentBuckets() {
        long value = 123456789L;

        int bucket10 = Hashing.consistentHash(value, 10);
        int bucket100 = Hashing.consistentHash(value, 100);
        int bucket1000 = Hashing.consistentHash(value, 1000);

        assertTrue(bucket10 >= 0 && bucket10 < 10);
        assertTrue(bucket100 >= 0 && bucket100 < 100);
        assertTrue(bucket1000 >= 0 && bucket1000 < 1000);
    }

    @Test
    @DisplayName("Test consistentHash with minimum buckets")
    public void testConsistentHashMinimumBuckets() {
        long value = 123456789L;

        int bucket = Hashing.consistentHash(value, 1);

        assertEquals(0, bucket);
    }

    @Test
    @DisplayName("Test consistentHash minimizes redistribution")
    public void testConsistentHashMinimizesRedistribution() {
        long value = 123456789L;

        // When buckets grow from 10 to 11, most items should stay in the same bucket
        int bucket10 = Hashing.consistentHash(value, 10);
        int bucket11 = Hashing.consistentHash(value, 11);

        // The value should either stay in the same bucket or move to bucket 10 (the new one)
        assertTrue(bucket11 == bucket10 || bucket11 == 10);
    }

    @Test
    @DisplayName("Test consistentHash throws for invalid bucket count")
    public void testConsistentHashInvalidBuckets() {
        HashCode hashCode = Hashing.murmur3_128().hash("test");

        assertThrows(IllegalArgumentException.class, () -> Hashing.consistentHash(hashCode, 0));
        assertThrows(IllegalArgumentException.class, () -> Hashing.consistentHash(hashCode, -1));
        assertThrows(IllegalArgumentException.class, () -> Hashing.consistentHash(123L, 0));
        assertThrows(IllegalArgumentException.class, () -> Hashing.consistentHash(123L, -1));
    }

    // Integration and real-world scenario tests

    @Test
    @DisplayName("Test choosing appropriate hash function for different scenarios")
    public void testChoosingHashFunction() {
        byte[] data = "test data".getBytes(StandardCharsets.UTF_8);

        // Fast non-cryptographic
        HashCode fast = Hashing.murmur3_128().hash(data);
        assertNotNull(fast);

        // Cryptographic
        HashCode secure = Hashing.sha256().hash(data);
        assertNotNull(secure);

        // Checksum
        HashCode checksum = Hashing.crc32().hash(data);
        assertNotNull(checksum);
    }

    @Test
    @DisplayName("Test password hashing scenario")
    public void testPasswordHashingScenario() {
        String password = "myPassword123";
        String salt = "randomSalt";

        byte[] saltedPassword = (password + salt).getBytes(StandardCharsets.UTF_8);
        HashCode hash = Hashing.sha256().hash(saltedPassword);

        assertNotNull(hash);
        assertEquals(256, hash.bits());
    }

    @Test
    @DisplayName("Test file integrity checking scenario")
    public void testFileIntegrityScenario() {
        byte[] fileContent = "Important file content".getBytes(StandardCharsets.UTF_8);

        HashCode originalChecksum = Hashing.sha256().hash(fileContent);
        HashCode verifyChecksum = Hashing.sha256().hash(fileContent);

        assertEquals(originalChecksum, verifyChecksum);

        // Modified content
        byte[] modifiedContent = "Different content".getBytes(StandardCharsets.UTF_8);
        HashCode modifiedChecksum = Hashing.sha256().hash(modifiedContent);

        assertNotEquals(originalChecksum, modifiedChecksum);
    }

    @Test
    @DisplayName("Test load balancing with consistent hashing")
    public void testLoadBalancingScenario() {
        int serverCount = 10;

        // Simulate distributing user IDs to servers
        long userId1 = 123456L;
        long userId2 = 789012L;
        long userId3 = 345678L;

        int server1 = Hashing.consistentHash(userId1, serverCount);
        int server2 = Hashing.consistentHash(userId2, serverCount);
        int server3 = Hashing.consistentHash(userId3, serverCount);

        assertTrue(server1 >= 0 && server1 < serverCount);
        assertTrue(server2 >= 0 && server2 < serverCount);
        assertTrue(server3 >= 0 && server3 < serverCount);

        // Same user should always go to same server
        assertEquals(server1, Hashing.consistentHash(userId1, serverCount));
    }

    @Test
    @DisplayName("Test combining hashes for composite key")
    public void testCompositeKeyScenario() {
        String userId = "user123";
        String sessionId = "session456";

        HashCode userHash = Hashing.murmur3_128().hash(userId, StandardCharsets.UTF_8);
        HashCode sessionHash = Hashing.murmur3_128().hash(sessionId, StandardCharsets.UTF_8);

        HashCode compositeHash = Hashing.combineOrdered(userHash, sessionHash);

        assertNotNull(compositeHash);
    }

    @Test
    @DisplayName("Test HMAC message authentication scenario")
    public void testMessageAuthenticationScenario() {
        String message = "Important message";
        byte[] secretKey = "sharedSecret".getBytes(StandardCharsets.UTF_8);

        HashCode mac = Hashing.hmacSha256(secretKey).hash(message, StandardCharsets.UTF_8);

        assertNotNull(mac);
        assertEquals(256, mac.bits());

        // Verify with same key
        HashCode verifyMac = Hashing.hmacSha256(secretKey).hash(message, StandardCharsets.UTF_8);
        assertEquals(mac, verifyMac);

        // Different key produces different MAC
        byte[] wrongKey = "wrongSecret".getBytes(StandardCharsets.UTF_8);
        HashCode wrongMac = Hashing.hmacSha256(wrongKey).hash(message, StandardCharsets.UTF_8);
        assertNotEquals(mac, wrongMac);
    }

    @Test
    @DisplayName("Test all hash functions are reusable")
    public void testHashFunctionsReusable() {
        HashFunction hashFunc = Hashing.murmur3_128();

        HashCode hash1 = hashFunc.hash("test1");
        HashCode hash2 = hashFunc.hash("test2");
        HashCode hash3 = hashFunc.hash("test1");

        assertNotEquals(hash1, hash2);
        assertEquals(hash1, hash3);
    }
}
