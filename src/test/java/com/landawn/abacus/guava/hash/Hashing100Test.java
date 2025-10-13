package com.landawn.abacus.guava.hash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.google.common.hash.Funnel;
import com.google.common.hash.HashCode;
import com.google.common.hash.PrimitiveSink;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class Hashing100Test extends TestBase {

    @Test
    public void testGoodFastHash() {
        HashFunction hf1 = Hashing.goodFastHash(32);
        assertNotNull(hf1);
        assertTrue(hf1.bits() >= 32);
        
        HashFunction hf2 = Hashing.goodFastHash(128);
        assertNotNull(hf2);
        assertTrue(hf2.bits() >= 128);
        
        HashFunction hf3 = Hashing.goodFastHash(128);
        assertEquals(hf2.bits(), hf3.bits());
        
        byte[] data = "test data".getBytes();
        HashCode hash1 = hf1.hash(data);
        HashCode hash2 = hf1.hash(data);
        assertEquals(hash1, hash2);
        
        assertThrows(IllegalArgumentException.class, () -> Hashing.goodFastHash(-1));
        assertThrows(IllegalArgumentException.class, () -> Hashing.goodFastHash(0));
    }

    @Test
    public void testMurmur3_32WithSeed() {
        HashFunction hf1 = Hashing.murmur3_32(42);
        HashFunction hf2 = Hashing.murmur3_32(42);
        HashFunction hf3 = Hashing.murmur3_32(123);
        
        assertEquals(32, hf1.bits());
        
        byte[] data = "test".getBytes();
        HashCode hash1 = hf1.hash(data);
        HashCode hash2 = hf2.hash(data);
        HashCode hash3 = hf3.hash(data);
        
        assertEquals(hash1, hash2);
        assertNotEquals(hash1, hash3);
    }

    @Test
    public void testMurmur3_32NoSeed() {
        HashFunction hf = Hashing.murmur3_32();
        assertNotNull(hf);
        assertEquals(32, hf.bits());
        
        byte[] data = "hello".getBytes();
        HashCode hash1 = hf.hash(data);
        HashCode hash2 = hf.hash(data);
        assertEquals(hash1, hash2);
    }

    @Test
    public void testMurmur3_128WithSeed() {
        HashFunction hf1 = Hashing.murmur3_128(42);
        HashFunction hf2 = Hashing.murmur3_128(123);
        
        assertEquals(128, hf1.bits());
        
        byte[] data = "test data".getBytes();
        HashCode hash1 = hf1.hash(data);
        HashCode hash2 = hf2.hash(data);
        
        assertNotEquals(hash1, hash2);
    }

    @Test
    public void testMurmur3_128NoSeed() {
        HashFunction hf = Hashing.murmur3_128();
        assertNotNull(hf);
        assertEquals(128, hf.bits());
        
        HashCode hash = hf.hash("test".getBytes());
        assertEquals(16, hash.asBytes().length);
    }

    @Test
    public void testSipHash24() {
        HashFunction hf = Hashing.sipHash24();
        assertNotNull(hf);
        assertEquals(64, hf.bits());
        
        HashCode hash = hf.hash("siphash test".getBytes());
        assertEquals(8, hash.asBytes().length);
    }

    @Test
    public void testSipHash24WithKey() {
        long k0 = 0x0706050403020100L;
        long k1 = 0x0f0e0d0c0b0a0908L;
        
        HashFunction hf1 = Hashing.sipHash24(k0, k1);
        HashFunction hf2 = Hashing.sipHash24(k0, k1);
        HashFunction hf3 = Hashing.sipHash24(k1, k0);
        
        byte[] data = "test".getBytes();
        HashCode hash1 = hf1.hash(data);
        HashCode hash2 = hf2.hash(data);
        HashCode hash3 = hf3.hash(data);
        
        assertEquals(hash1, hash2);
        assertNotEquals(hash1, hash3);
    }

    @Test
    public void testMd5() {
        HashFunction hf = Hashing.md5();
        assertNotNull(hf);
        assertEquals(128, hf.bits());
        
        HashCode hash = hf.hash("MD5 test".getBytes());
        assertEquals(16, hash.asBytes().length);
    }

    @Test
    public void testSha1() {
        HashFunction hf = Hashing.sha1();
        assertNotNull(hf);
        assertEquals(160, hf.bits());
        
        HashCode hash = hf.hash("SHA-1 test".getBytes());
        assertEquals(20, hash.asBytes().length);
    }

    @Test
    public void testSha256() {
        HashFunction hf = Hashing.sha256();
        assertNotNull(hf);
        assertEquals(256, hf.bits());
        
        HashCode hash = hf.hash("SHA-256 test".getBytes());
        assertEquals(32, hash.asBytes().length);
    }

    @Test
    public void testSha384() {
        HashFunction hf = Hashing.sha384();
        assertNotNull(hf);
        assertEquals(384, hf.bits());
        
        HashCode hash = hf.hash("SHA-384 test".getBytes());
        assertEquals(48, hash.asBytes().length);
    }

    @Test
    public void testSha512() {
        HashFunction hf = Hashing.sha512();
        assertNotNull(hf);
        assertEquals(512, hf.bits());
        
        HashCode hash = hf.hash("SHA-512 test".getBytes());
        assertEquals(64, hash.asBytes().length);
    }

    @Test
    public void testHmacMd5WithKey() {
        Key key = new SecretKeySpec("secret".getBytes(), "HmacMD5");
        HashFunction hf = Hashing.hmacMd5(key);
        assertNotNull(hf);
        
        HashCode hash = hf.hash("message".getBytes());
        assertNotNull(hash);
    }

    @Test
    public void testHmacMd5WithByteArray() {
        byte[] keyBytes = "secret key".getBytes(StandardCharsets.UTF_8);
        HashFunction hf = Hashing.hmacMd5(keyBytes);
        assertNotNull(hf);
        
        HashCode hash = hf.hash("message".getBytes());
        assertNotNull(hash);
        
        assertThrows(IllegalArgumentException.class, () -> Hashing.hmacMd5(new byte[0]));
    }

    @Test
    public void testHmacSha1WithKey() {
        Key key = new SecretKeySpec("secret".getBytes(), "HmacSHA1");
        HashFunction hf = Hashing.hmacSha1(key);
        assertNotNull(hf);
        
        HashCode hash = hf.hash("message".getBytes());
        assertNotNull(hash);
    }

    @Test
    public void testHmacSha1WithByteArray() {
        byte[] keyBytes = "secret key".getBytes();
        HashFunction hf = Hashing.hmacSha1(keyBytes);
        assertNotNull(hf);
        
        HashCode hash = hf.hash("message".getBytes());
        assertNotNull(hash);
    }

    @Test
    public void testHmacSha256WithKey() {
        Key key = new SecretKeySpec("secret".getBytes(), "HmacSHA256");
        HashFunction hf = Hashing.hmacSha256(key);
        assertNotNull(hf);
        
        HashCode hash = hf.hash("message".getBytes());
        assertNotNull(hash);
    }

    @Test
    public void testHmacSha256WithByteArray() {
        byte[] keyBytes = "strongSecretKey".getBytes(StandardCharsets.UTF_8);
        HashFunction hf = Hashing.hmacSha256(keyBytes);
        assertNotNull(hf);
        
        HashCode hash = hf.hash("important data".getBytes());
        assertNotNull(hash);
    }

    @Test
    public void testHmacSha512WithKey() {
        Key key = new SecretKeySpec("topsecret".getBytes(), "HmacSHA512");
        HashFunction hf = Hashing.hmacSha512(key);
        assertNotNull(hf);
        
        HashCode hash = hf.hash("critical data".getBytes());
        assertNotNull(hash);
    }

    @Test
    public void testHmacSha512WithByteArray() {
        byte[] keyBytes = new byte[64];
        Arrays.fill(keyBytes, (byte) 0x42);
        HashFunction hf = Hashing.hmacSha512(keyBytes);
        assertNotNull(hf);
        
        HashCode hash = hf.hash("top secret data".getBytes());
        assertNotNull(hash);
    }

    @Test
    public void testCrc32c() {
        HashFunction hf = Hashing.crc32c();
        assertNotNull(hf);
        assertEquals(32, hf.bits());
        
        HashCode hash = hf.hash("data to check".getBytes());
        assertEquals(4, hash.asBytes().length);
    }

    @Test
    public void testCrc32() {
        HashFunction hf = Hashing.crc32();
        assertNotNull(hf);
        assertEquals(32, hf.bits());
        
        HashCode hash = hf.hash("file contents".getBytes());
        assertEquals(4, hash.asBytes().length);
        
        long crcValue = hash.padToLong();
        assertTrue(crcValue >= 0);
    }

    @Test
    public void testAdler32() {
        HashFunction hf = Hashing.adler32();
        assertNotNull(hf);
        assertEquals(32, hf.bits());
        
        HashCode hash = hf.hash("compressed data".getBytes());
        assertEquals(4, hash.asBytes().length);
        
        long adlerValue = hash.padToLong();
        assertTrue(adlerValue >= 0);
    }

    @Test
    public void testFarmHashFingerprint64() {
        HashFunction hf = Hashing.farmHashFingerprint64();
        assertNotNull(hf);
        assertEquals(64, hf.bits());
        
        HashCode hash = hf.hash("unique identifier".getBytes());
        assertEquals(8, hash.asBytes().length);
    }

    @Test
    public void testConcatenatingTwoFunctions() {
        HashFunction first = Hashing.murmur3_32();
        HashFunction second = Hashing.murmur3_32(42);
        
        HashFunction concatenated = Hashing.concatenating(first, second);
        assertNotNull(concatenated);
        assertEquals(64, concatenated.bits());
        
        HashCode hash = concatenated.hash("test".getBytes());
        assertEquals(8, hash.asBytes().length);
    }

    @Test
    public void testConcatenatingThreeFunctions() {
        HashFunction first = Hashing.murmur3_128();
        HashFunction second = Hashing.murmur3_128(42);
        HashFunction third = Hashing.murmur3_128(123);
        
        HashFunction concatenated = Hashing.concatenating(first, second, third);
        assertNotNull(concatenated);
        assertEquals(384, concatenated.bits());
        
        HashCode hash = concatenated.hash("test".getBytes());
        assertEquals(48, hash.asBytes().length);
    }

    @Test
    public void testConcatenatingIterable() {
        List<HashFunction> functions = Arrays.asList(
            Hashing.sha256(),
            Hashing.sha256()
        );
        
        HashFunction concatenated = Hashing.concatenating(functions);
        assertNotNull(concatenated);
        assertEquals(512, concatenated.bits());
        
        HashCode hash = concatenated.hash("test".getBytes());
        assertEquals(64, hash.asBytes().length);
        
        List<HashFunction> emptyList = new ArrayList<>();
        assertThrows(IllegalArgumentException.class, () -> Hashing.concatenating(emptyList));
    }

    @Test
    public void testCombineOrderedTwo() {
        HashCode hash1 = Hashing.sha256().hash("first".getBytes());
        HashCode hash2 = Hashing.sha256().hash("second".getBytes());
        
        HashCode combined1 = Hashing.combineOrdered(hash1, hash2);
        HashCode combined2 = Hashing.combineOrdered(hash2, hash1);
        
        assertNotNull(combined1);
        assertNotNull(combined2);
        assertNotEquals(combined1, combined2);
        
        HashCode hash3 = Hashing.sha512().hash("third".getBytes());
        assertThrows(IllegalArgumentException.class, () -> Hashing.combineOrdered(hash1, hash3));
    }

    @Test
    public void testCombineOrderedThree() {
        HashCode hash1 = Hashing.md5().hash("part1".getBytes());
        HashCode hash2 = Hashing.md5().hash("part2".getBytes());
        HashCode hash3 = Hashing.md5().hash("part3".getBytes());
        
        HashCode combined = Hashing.combineOrdered(hash1, hash2, hash3);
        assertNotNull(combined);
        assertEquals(hash1.bits(), combined.bits());
    }

    @Test
    public void testCombineOrderedIterable() {
        List<HashCode> hashes = Arrays.asList(
            Hashing.sha256().hash("a".getBytes()),
            Hashing.sha256().hash("b".getBytes()),
            Hashing.sha256().hash("c".getBytes())
        );
        
        HashCode combined = Hashing.combineOrdered(hashes);
        assertNotNull(combined);
        
        List<HashCode> emptyList = new ArrayList<>();
        assertThrows(IllegalArgumentException.class, () -> Hashing.combineOrdered(emptyList));
    }

    @Test
    public void testCombineUnorderedTwo() {
        HashCode hash1 = Hashing.sha256().hash("item1".getBytes());
        HashCode hash2 = Hashing.sha256().hash("item2".getBytes());
        
        HashCode combined1 = Hashing.combineUnordered(hash1, hash2);
        HashCode combined2 = Hashing.combineUnordered(hash2, hash1);
        
        assertNotNull(combined1);
        assertNotNull(combined2);
        assertEquals(combined1, combined2);
    }

    @Test
    public void testCombineUnorderedThree() {
        HashCode hash1 = Hashing.murmur3_128().hash("a".getBytes());
        HashCode hash2 = Hashing.murmur3_128().hash("b".getBytes());
        HashCode hash3 = Hashing.murmur3_128().hash("c".getBytes());
        
        HashCode combined1 = Hashing.combineUnordered(hash1, hash2, hash3);
        HashCode combined2 = Hashing.combineUnordered(hash3, hash1, hash2);
        
        assertEquals(combined1, combined2);
    }

    @Test
    public void testCombineUnorderedIterable() {
        List<HashCode> hashes = Arrays.asList(
            Hashing.sha256().hash("x".getBytes()),
            Hashing.sha256().hash("y".getBytes()),
            Hashing.sha256().hash("z".getBytes())
        );
        
        HashCode combined = Hashing.combineUnordered(hashes);
        assertNotNull(combined);
    }

    @Test
    public void testConsistentHashWithHashCode() {
        HashCode hashCode = Hashing.sha256().hash("user123".getBytes());
        
        int bucket1 = Hashing.consistentHash(hashCode, 10);
        int bucket2 = Hashing.consistentHash(hashCode, 10);
        
        assertEquals(bucket1, bucket2);
        assertTrue(bucket1 >= 0 && bucket1 < 10);
        
        int bucket3 = Hashing.consistentHash(hashCode, 100);
        assertTrue(bucket3 >= 0 && bucket3 < 100);
        
        assertThrows(IllegalArgumentException.class, () -> Hashing.consistentHash(hashCode, 0));
        assertThrows(IllegalArgumentException.class, () -> Hashing.consistentHash(hashCode, -1));
    }

    @Test
    public void testConsistentHashWithLong() {
        long input = 12345L;
        
        int bucket1 = Hashing.consistentHash(input, 100);
        int bucket2 = Hashing.consistentHash(input, 100);
        
        assertEquals(bucket1, bucket2);
        assertTrue(bucket1 >= 0 && bucket1 < 100);
        
        int bucket3 = Hashing.consistentHash(54321L, 100);
        
        assertThrows(IllegalArgumentException.class, () -> Hashing.consistentHash(input, 0));
    }

    private static class Person {
        String name;
        int age;
        
        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    private static final Funnel<Person> PERSON_FUNNEL = new Funnel<Person>() {
        @Override
        public void funnel(Person from, PrimitiveSink into) {
            into.putString(from.name, StandardCharsets.UTF_8)
                .putInt(from.age);
        }
    };

    @Test
    public void testHashingWithFunnel() {
        Person person1 = new Person("Alice", 30);
        Person person2 = new Person("Alice", 30);
        Person person3 = new Person("Bob", 25);
        
        HashFunction hf = Hashing.sha256();
        
        HashCode hash1 = hf.hash(person1, PERSON_FUNNEL);
        HashCode hash2 = hf.hash(person2, PERSON_FUNNEL);
        HashCode hash3 = hf.hash(person3, PERSON_FUNNEL);
        
        assertEquals(hash1, hash2);
        assertNotEquals(hash1, hash3);
    }
}
