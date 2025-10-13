package com.landawn.abacus.util;

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
import com.landawn.abacus.guava.hash.HashFunction;
import com.landawn.abacus.guava.hash.Hashing;

@Tag("2025")
public class Hashing2025Test extends TestBase {

    @Test
    public void testGoodFastHash_basic() {
        HashFunction hf = Hashing.goodFastHash(32);
        assertNotNull(hf);
        assertTrue(hf.bits() >= 32);

        byte[] data = "test data".getBytes(StandardCharsets.UTF_8);
        HashCode hash1 = hf.hash(data);
        HashCode hash2 = hf.hash(data);
        assertEquals(hash1, hash2);
    }

    @Test
    public void testGoodFastHash_variousBitSizes() {
        HashFunction hf64 = Hashing.goodFastHash(64);
        HashFunction hf128 = Hashing.goodFastHash(128);
        HashFunction hf256 = Hashing.goodFastHash(256);

        assertTrue(hf64.bits() >= 64);
        assertTrue(hf128.bits() >= 128);
        assertTrue(hf256.bits() >= 256);

        byte[] data = "sample".getBytes();
        assertNotNull(hf64.hash(data));
        assertNotNull(hf128.hash(data));
        assertNotNull(hf256.hash(data));
    }

    @Test
    public void testGoodFastHash_consistency() {
        HashFunction hf1 = Hashing.goodFastHash(128);
        HashFunction hf2 = Hashing.goodFastHash(128);
        assertEquals(hf1.bits(), hf2.bits());
    }

    @Test
    public void testGoodFastHash_invalidInput() {
        assertThrows(IllegalArgumentException.class, () -> Hashing.goodFastHash(-1));
        assertThrows(IllegalArgumentException.class, () -> Hashing.goodFastHash(0));
    }

    @Test
    public void testMurmur3_32_withSeed_basic() {
        HashFunction hf = Hashing.murmur3_32(42);
        assertNotNull(hf);
        assertEquals(32, hf.bits());

        byte[] data = "test".getBytes();
        HashCode hash = hf.hash(data);
        assertEquals(4, hash.asBytes().length);
    }

    @Test
    public void testMurmur3_32_withSeed_consistency() {
        HashFunction hf1 = Hashing.murmur3_32(42);
        HashFunction hf2 = Hashing.murmur3_32(42);

        byte[] data = "consistent data".getBytes();
        assertEquals(hf1.hash(data), hf2.hash(data));
    }

    @Test
    public void testMurmur3_32_withSeed_differentSeeds() {
        HashFunction hf1 = Hashing.murmur3_32(42);
        HashFunction hf2 = Hashing.murmur3_32(123);
        HashFunction hf3 = Hashing.murmur3_32(0);
        HashFunction hf4 = Hashing.murmur3_32(-1);

        byte[] data = "test".getBytes();
        HashCode hash1 = hf1.hash(data);
        HashCode hash2 = hf2.hash(data);
        HashCode hash3 = hf3.hash(data);
        HashCode hash4 = hf4.hash(data);

        assertNotEquals(hash1, hash2);
        assertNotEquals(hash1, hash3);
        assertNotEquals(hash1, hash4);
    }

    @Test
    public void testMurmur3_32_noSeed() {
        HashFunction hf = Hashing.murmur3_32();
        assertNotNull(hf);
        assertEquals(32, hf.bits());

        byte[] data = "hello world".getBytes();
        HashCode hash1 = hf.hash(data);
        HashCode hash2 = hf.hash(data);
        assertEquals(hash1, hash2);
    }

    @Test
    public void testMurmur3_32_noSeed_emptyData() {
        HashFunction hf = Hashing.murmur3_32();
        HashCode hash = hf.hash(new byte[0]);
        assertNotNull(hash);
        assertEquals(4, hash.asBytes().length);
    }

    @Test
    public void testMurmur3_128_withSeed_basic() {
        HashFunction hf = Hashing.murmur3_128(42);
        assertNotNull(hf);
        assertEquals(128, hf.bits());

        byte[] data = "test data".getBytes();
        HashCode hash = hf.hash(data);
        assertEquals(16, hash.asBytes().length);
    }

    @Test
    public void testMurmur3_128_withSeed_differentSeeds() {
        HashFunction hf1 = Hashing.murmur3_128(1);
        HashFunction hf2 = Hashing.murmur3_128(2);

        byte[] data = "data".getBytes();
        assertNotEquals(hf1.hash(data), hf2.hash(data));
    }

    @Test
    public void testMurmur3_128_noSeed() {
        HashFunction hf = Hashing.murmur3_128();
        assertNotNull(hf);
        assertEquals(128, hf.bits());

        HashCode hash = hf.hash("test".getBytes());
        assertEquals(16, hash.asBytes().length);
    }

    @Test
    public void testMurmur3_128_noSeed_largeData() {
        HashFunction hf = Hashing.murmur3_128();
        byte[] largeData = new byte[1024];
        Arrays.fill(largeData, (byte) 0x42);

        HashCode hash = hf.hash(largeData);
        assertNotNull(hash);
        assertEquals(16, hash.asBytes().length);
    }

    @Test
    public void testSipHash24_noKey() {
        HashFunction hf = Hashing.sipHash24();
        assertNotNull(hf);
        assertEquals(64, hf.bits());

        HashCode hash = hf.hash("siphash test".getBytes());
        assertEquals(8, hash.asBytes().length);
    }

    @Test
    public void testSipHash24_noKey_consistency() {
        HashFunction hf = Hashing.sipHash24();
        byte[] data = "consistent".getBytes();

        assertEquals(hf.hash(data), hf.hash(data));
    }

    @Test
    public void testSipHash24_withKey_basic() {
        long k0 = 0x0706050403020100L;
        long k1 = 0x0f0e0d0c0b0a0908L;

        HashFunction hf = Hashing.sipHash24(k0, k1);
        assertNotNull(hf);
        assertEquals(64, hf.bits());

        HashCode hash = hf.hash("test".getBytes());
        assertEquals(8, hash.asBytes().length);
    }

    @Test
    public void testSipHash24_withKey_consistency() {
        long k0 = 0x0706050403020100L;
        long k1 = 0x0f0e0d0c0b0a0908L;

        HashFunction hf1 = Hashing.sipHash24(k0, k1);
        HashFunction hf2 = Hashing.sipHash24(k0, k1);

        byte[] data = "test".getBytes();
        assertEquals(hf1.hash(data), hf2.hash(data));
    }

    @Test
    public void testSipHash24_withKey_differentKeys() {
        long k0 = 0x0706050403020100L;
        long k1 = 0x0f0e0d0c0b0a0908L;

        HashFunction hf1 = Hashing.sipHash24(k0, k1);
        HashFunction hf2 = Hashing.sipHash24(k1, k0);
        HashFunction hf3 = Hashing.sipHash24(0, 0);

        byte[] data = "test".getBytes();
        assertNotEquals(hf1.hash(data), hf2.hash(data));
        assertNotEquals(hf1.hash(data), hf3.hash(data));
    }

    @Test
    public void testMd5_basic() {
        HashFunction hf = Hashing.md5();
        assertNotNull(hf);
        assertEquals(128, hf.bits());

        HashCode hash = hf.hash("MD5 test".getBytes());
        assertEquals(16, hash.asBytes().length);
    }

    @Test
    public void testMd5_knownVector() {
        HashFunction hf = Hashing.md5();
        byte[] emptyData = new byte[0];
        HashCode hash = hf.hash(emptyData);

        assertNotNull(hash);
        assertEquals(16, hash.asBytes().length);
    }

    @Test
    public void testSha1_basic() {
        HashFunction hf = Hashing.sha1();
        assertNotNull(hf);
        assertEquals(160, hf.bits());

        HashCode hash = hf.hash("SHA-1 test".getBytes());
        assertEquals(20, hash.asBytes().length);
    }

    @Test
    public void testSha1_differentData() {
        HashFunction hf = Hashing.sha1();

        HashCode hash1 = hf.hash("data1".getBytes());
        HashCode hash2 = hf.hash("data2".getBytes());

        assertNotEquals(hash1, hash2);
    }

    @Test
    public void testSha256_basic() {
        HashFunction hf = Hashing.sha256();
        assertNotNull(hf);
        assertEquals(256, hf.bits());

        HashCode hash = hf.hash("SHA-256 test".getBytes());
        assertEquals(32, hash.asBytes().length);
    }

    @Test
    public void testSha256_consistency() {
        HashFunction hf = Hashing.sha256();
        byte[] data = "test data".getBytes();

        assertEquals(hf.hash(data), hf.hash(data));
    }

    @Test
    public void testSha384_basic() {
        HashFunction hf = Hashing.sha384();
        assertNotNull(hf);
        assertEquals(384, hf.bits());

        HashCode hash = hf.hash("SHA-384 test".getBytes());
        assertEquals(48, hash.asBytes().length);
    }

    @Test
    public void testSha384_largeData() {
        HashFunction hf = Hashing.sha384();
        byte[] data = new byte[10000];
        Arrays.fill(data, (byte) 'A');

        HashCode hash = hf.hash(data);
        assertNotNull(hash);
        assertEquals(48, hash.asBytes().length);
    }

    @Test
    public void testSha512_basic() {
        HashFunction hf = Hashing.sha512();
        assertNotNull(hf);
        assertEquals(512, hf.bits());

        HashCode hash = hf.hash("SHA-512 test".getBytes());
        assertEquals(64, hash.asBytes().length);
    }

    @Test
    public void testSha512_emptyInput() {
        HashFunction hf = Hashing.sha512();
        HashCode hash = hf.hash(new byte[0]);

        assertNotNull(hash);
        assertEquals(64, hash.asBytes().length);
    }

    @Test
    public void testHmacMd5_withKey_basic() {
        Key key = new SecretKeySpec("secret".getBytes(), "HmacMD5");
        HashFunction hf = Hashing.hmacMd5(key);
        assertNotNull(hf);

        HashCode hash = hf.hash("message".getBytes());
        assertNotNull(hash);
    }

    @Test
    public void testHmacMd5_withKey_consistency() {
        Key key = new SecretKeySpec("secret".getBytes(), "HmacMD5");
        HashFunction hf = Hashing.hmacMd5(key);

        byte[] data = "message".getBytes();
        assertEquals(hf.hash(data), hf.hash(data));
    }

    @Test
    public void testHmacMd5_withKey_differentKeys() {
        Key key1 = new SecretKeySpec("secret1".getBytes(), "HmacMD5");
        Key key2 = new SecretKeySpec("secret2".getBytes(), "HmacMD5");

        HashFunction hf1 = Hashing.hmacMd5(key1);
        HashFunction hf2 = Hashing.hmacMd5(key2);

        byte[] data = "message".getBytes();
        assertNotEquals(hf1.hash(data), hf2.hash(data));
    }

    @Test
    public void testHmacMd5_withByteArray_basic() {
        byte[] keyBytes = "secret key".getBytes(StandardCharsets.UTF_8);
        HashFunction hf = Hashing.hmacMd5(keyBytes);
        assertNotNull(hf);

        HashCode hash = hf.hash("message".getBytes());
        assertNotNull(hash);
    }

    @Test
    public void testHmacMd5_withByteArray_emptyKey() {
        assertThrows(IllegalArgumentException.class, () -> Hashing.hmacMd5(new byte[0]));
    }

    @Test
    public void testHmacMd5_withByteArray_differentData() {
        byte[] keyBytes = "key".getBytes();
        HashFunction hf = Hashing.hmacMd5(keyBytes);

        HashCode hash1 = hf.hash("message1".getBytes());
        HashCode hash2 = hf.hash("message2".getBytes());

        assertNotEquals(hash1, hash2);
    }

    @Test
    public void testHmacSha1_withKey_basic() {
        Key key = new SecretKeySpec("secret".getBytes(), "HmacSHA1");
        HashFunction hf = Hashing.hmacSha1(key);
        assertNotNull(hf);

        HashCode hash = hf.hash("message".getBytes());
        assertNotNull(hash);
    }

    @Test
    public void testHmacSha1_withKey_longKey() {
        byte[] longKey = new byte[256];
        Arrays.fill(longKey, (byte) 0x5A);
        Key key = new SecretKeySpec(longKey, "HmacSHA1");

        HashFunction hf = Hashing.hmacSha1(key);
        HashCode hash = hf.hash("test".getBytes());
        assertNotNull(hash);
    }

    @Test
    public void testHmacSha1_withByteArray_basic() {
        byte[] keyBytes = "secret key".getBytes();
        HashFunction hf = Hashing.hmacSha1(keyBytes);
        assertNotNull(hf);

        HashCode hash = hf.hash("message".getBytes());
        assertNotNull(hash);
    }

    @Test
    public void testHmacSha1_withByteArray_consistency() {
        byte[] keyBytes = "key".getBytes();
        HashFunction hf = Hashing.hmacSha1(keyBytes);
        byte[] data = "data".getBytes();

        assertEquals(hf.hash(data), hf.hash(data));
    }

    @Test
    public void testHmacSha256_withKey_basic() {
        Key key = new SecretKeySpec("secret".getBytes(), "HmacSHA256");
        HashFunction hf = Hashing.hmacSha256(key);
        assertNotNull(hf);

        HashCode hash = hf.hash("message".getBytes());
        assertNotNull(hash);
    }

    @Test
    public void testHmacSha256_withKey_emptyMessage() {
        Key key = new SecretKeySpec("secret".getBytes(), "HmacSHA256");
        HashFunction hf = Hashing.hmacSha256(key);

        HashCode hash = hf.hash(new byte[0]);
        assertNotNull(hash);
    }

    @Test
    public void testHmacSha256_withByteArray_basic() {
        byte[] keyBytes = "strongSecretKey".getBytes(StandardCharsets.UTF_8);
        HashFunction hf = Hashing.hmacSha256(keyBytes);
        assertNotNull(hf);

        HashCode hash = hf.hash("important data".getBytes());
        assertNotNull(hash);
    }

    @Test
    public void testHmacSha256_withByteArray_variousKeySizes() {
        byte[] key16 = new byte[16];
        byte[] key32 = new byte[32];
        byte[] key64 = new byte[64];

        Arrays.fill(key16, (byte) 1);
        Arrays.fill(key32, (byte) 2);
        Arrays.fill(key64, (byte) 3);

        HashFunction hf16 = Hashing.hmacSha256(key16);
        HashFunction hf32 = Hashing.hmacSha256(key32);
        HashFunction hf64 = Hashing.hmacSha256(key64);

        byte[] data = "test".getBytes();
        assertNotEquals(hf16.hash(data), hf32.hash(data));
        assertNotEquals(hf16.hash(data), hf64.hash(data));
    }

    @Test
    public void testHmacSha512_withKey_basic() {
        Key key = new SecretKeySpec("topsecret".getBytes(), "HmacSHA512");
        HashFunction hf = Hashing.hmacSha512(key);
        assertNotNull(hf);

        HashCode hash = hf.hash("critical data".getBytes());
        assertNotNull(hash);
    }

    @Test
    public void testHmacSha512_withKey_consistency() {
        Key key = new SecretKeySpec("key".getBytes(), "HmacSHA512");
        HashFunction hf = Hashing.hmacSha512(key);
        byte[] data = "data".getBytes();

        assertEquals(hf.hash(data), hf.hash(data));
    }

    @Test
    public void testHmacSha512_withByteArray_basic() {
        byte[] keyBytes = new byte[64];
        Arrays.fill(keyBytes, (byte) 0x42);
        HashFunction hf = Hashing.hmacSha512(keyBytes);
        assertNotNull(hf);

        HashCode hash = hf.hash("top secret data".getBytes());
        assertNotNull(hash);
    }

    @Test
    public void testHmacSha512_withByteArray_differentKeys() {
        byte[] key1 = new byte[32];
        byte[] key2 = new byte[32];
        Arrays.fill(key1, (byte) 1);
        Arrays.fill(key2, (byte) 2);

        HashFunction hf1 = Hashing.hmacSha512(key1);
        HashFunction hf2 = Hashing.hmacSha512(key2);

        byte[] data = "data".getBytes();
        assertNotEquals(hf1.hash(data), hf2.hash(data));
    }

    @Test
    public void testCrc32c_basic() {
        HashFunction hf = Hashing.crc32c();
        assertNotNull(hf);
        assertEquals(32, hf.bits());

        HashCode hash = hf.hash("data to check".getBytes());
        assertEquals(4, hash.asBytes().length);
    }

    @Test
    public void testCrc32c_consistency() {
        HashFunction hf = Hashing.crc32c();
        byte[] data = "test".getBytes();

        assertEquals(hf.hash(data), hf.hash(data));
    }

    @Test
    public void testCrc32c_differentData() {
        HashFunction hf = Hashing.crc32c();

        HashCode hash1 = hf.hash("abc".getBytes());
        HashCode hash2 = hf.hash("def".getBytes());

        assertNotEquals(hash1, hash2);
    }

    @Test
    public void testCrc32_basic() {
        HashFunction hf = Hashing.crc32();
        assertNotNull(hf);
        assertEquals(32, hf.bits());

        HashCode hash = hf.hash("file contents".getBytes());
        assertEquals(4, hash.asBytes().length);
    }

    @Test
    public void testCrc32_padToLong() {
        HashFunction hf = Hashing.crc32();
        HashCode hash = hf.hash("test".getBytes());

        long crcValue = hash.padToLong();
        assertTrue(crcValue >= 0);
    }

    @Test
    public void testCrc32_emptyData() {
        HashFunction hf = Hashing.crc32();
        HashCode hash = hf.hash(new byte[0]);

        assertNotNull(hash);
        assertEquals(4, hash.asBytes().length);
    }

    @Test
    public void testAdler32_basic() {
        HashFunction hf = Hashing.adler32();
        assertNotNull(hf);
        assertEquals(32, hf.bits());

        HashCode hash = hf.hash("compressed data".getBytes());
        assertEquals(4, hash.asBytes().length);
    }

    @Test
    public void testAdler32_padToLong() {
        HashFunction hf = Hashing.adler32();
        HashCode hash = hf.hash("test".getBytes());

        long adlerValue = hash.padToLong();
        assertTrue(adlerValue >= 0);
    }

    @Test
    public void testAdler32_largeData() {
        HashFunction hf = Hashing.adler32();
        byte[] largeData = new byte[5000];
        Arrays.fill(largeData, (byte) 'X');

        HashCode hash = hf.hash(largeData);
        assertNotNull(hash);
    }

    @Test
    public void testFarmHashFingerprint64_basic() {
        HashFunction hf = Hashing.farmHashFingerprint64();
        assertNotNull(hf);
        assertEquals(64, hf.bits());

        HashCode hash = hf.hash("unique identifier".getBytes());
        assertEquals(8, hash.asBytes().length);
    }

    @Test
    public void testFarmHashFingerprint64_consistency() {
        HashFunction hf = Hashing.farmHashFingerprint64();
        byte[] data = "fingerprint".getBytes();

        assertEquals(hf.hash(data), hf.hash(data));
    }

    @Test
    public void testFarmHashFingerprint64_differentData() {
        HashFunction hf = Hashing.farmHashFingerprint64();

        HashCode hash1 = hf.hash("string1".getBytes());
        HashCode hash2 = hf.hash("string2".getBytes());

        assertNotEquals(hash1, hash2);
    }

    @Test
    public void testConcatenating_twoFunctions_basic() {
        HashFunction first = Hashing.murmur3_32();
        HashFunction second = Hashing.murmur3_32(42);

        HashFunction concatenated = Hashing.concatenating(first, second);
        assertNotNull(concatenated);
        assertEquals(64, concatenated.bits());

        HashCode hash = concatenated.hash("test".getBytes());
        assertEquals(8, hash.asBytes().length);
    }

    @Test
    public void testConcatenating_twoFunctions_consistency() {
        HashFunction first = Hashing.sha256();
        HashFunction second = Hashing.sha256();

        HashFunction concat = Hashing.concatenating(first, second);
        byte[] data = "data".getBytes();

        assertEquals(concat.hash(data), concat.hash(data));
    }

    @Test
    public void testConcatenating_twoFunctions_differentCombinations() {
        HashFunction murmur = Hashing.murmur3_32();
        HashFunction sha = Hashing.sha256();

        HashFunction concat1 = Hashing.concatenating(murmur, sha);
        HashFunction concat2 = Hashing.concatenating(sha, murmur);

        assertEquals(288, concat1.bits());
        assertEquals(288, concat2.bits());
    }

    @Test
    public void testConcatenating_threeFunctions_basic() {
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
    public void testConcatenating_threeFunctions_mixedTypes() {
        HashFunction f1 = Hashing.md5();
        HashFunction f2 = Hashing.sha1();
        HashFunction f3 = Hashing.sha256();

        HashFunction concatenated = Hashing.concatenating(f1, f2, f3);
        assertEquals(128 + 160 + 256, concatenated.bits());
    }

    @Test
    public void testConcatenating_iterable_basic() {
        List<HashFunction> functions = Arrays.asList(Hashing.sha256(), Hashing.sha256());

        HashFunction concatenated = Hashing.concatenating(functions);
        assertNotNull(concatenated);
        assertEquals(512, concatenated.bits());

        HashCode hash = concatenated.hash("test".getBytes());
        assertEquals(64, hash.asBytes().length);
    }

    @Test
    public void testConcatenating_iterable_singleFunction() {
        List<HashFunction> functions = Arrays.asList(Hashing.sha256());

        HashFunction concatenated = Hashing.concatenating(functions);
        assertEquals(256, concatenated.bits());
    }

    @Test
    public void testConcatenating_iterable_manyFunctions() {
        List<HashFunction> functions = Arrays.asList(Hashing.murmur3_32(), Hashing.murmur3_32(1), Hashing.murmur3_32(2), Hashing.murmur3_32(3),
                Hashing.murmur3_32(4));

        HashFunction concatenated = Hashing.concatenating(functions);
        assertEquals(160, concatenated.bits());
    }

    @Test
    public void testConcatenating_iterable_emptyList() {
        List<HashFunction> emptyList = new ArrayList<>();
        assertThrows(IllegalArgumentException.class, () -> Hashing.concatenating(emptyList));
    }

    @Test
    public void testCombineOrdered_two_basic() {
        HashCode hash1 = Hashing.sha256().hash("first".getBytes());
        HashCode hash2 = Hashing.sha256().hash("second".getBytes());

        HashCode combined = Hashing.combineOrdered(hash1, hash2);
        assertNotNull(combined);
        assertEquals(256, combined.bits());
    }

    @Test
    public void testCombineOrdered_two_orderMatters() {
        HashCode hash1 = Hashing.sha256().hash("first".getBytes());
        HashCode hash2 = Hashing.sha256().hash("second".getBytes());

        HashCode combined1 = Hashing.combineOrdered(hash1, hash2);
        HashCode combined2 = Hashing.combineOrdered(hash2, hash1);

        assertNotEquals(combined1, combined2);
    }

    @Test
    public void testCombineOrdered_two_sameBits() {
        HashCode hash1 = Hashing.murmur3_128().hash("a".getBytes());
        HashCode hash2 = Hashing.murmur3_128().hash("b".getBytes());

        HashCode combined = Hashing.combineOrdered(hash1, hash2);
        assertEquals(hash1.bits(), combined.bits());
    }

    @Test
    public void testCombineOrdered_two_differentBitLengths() {
        HashCode hash256 = Hashing.sha256().hash("test".getBytes());
        HashCode hash512 = Hashing.sha512().hash("test".getBytes());

        assertThrows(IllegalArgumentException.class, () -> Hashing.combineOrdered(hash256, hash512));
    }

    @Test
    public void testCombineOrdered_three_basic() {
        HashCode hash1 = Hashing.md5().hash("part1".getBytes());
        HashCode hash2 = Hashing.md5().hash("part2".getBytes());
        HashCode hash3 = Hashing.md5().hash("part3".getBytes());

        HashCode combined = Hashing.combineOrdered(hash1, hash2, hash3);
        assertNotNull(combined);
        assertEquals(128, combined.bits());
    }

    @Test
    public void testCombineOrdered_three_orderMatters() {
        HashCode hash1 = Hashing.sha256().hash("a".getBytes());
        HashCode hash2 = Hashing.sha256().hash("b".getBytes());
        HashCode hash3 = Hashing.sha256().hash("c".getBytes());

        HashCode combined1 = Hashing.combineOrdered(hash1, hash2, hash3);
        HashCode combined2 = Hashing.combineOrdered(hash3, hash2, hash1);

        assertNotEquals(combined1, combined2);
    }

    @Test
    public void testCombineOrdered_iterable_basic() {
        List<HashCode> hashes = Arrays.asList(Hashing.sha256().hash("a".getBytes()), Hashing.sha256().hash("b".getBytes()),
                Hashing.sha256().hash("c".getBytes()));

        HashCode combined = Hashing.combineOrdered(hashes);
        assertNotNull(combined);
        assertEquals(256, combined.bits());
    }

    @Test
    public void testCombineOrdered_iterable_singleHash() {
        List<HashCode> hashes = Arrays.asList(Hashing.sha256().hash("single".getBytes()));

        HashCode combined = Hashing.combineOrdered(hashes);
        assertEquals(hashes.get(0), combined);
    }

    @Test
    public void testCombineOrdered_iterable_manyHashes() {
        List<HashCode> hashes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            hashes.add(Hashing.murmur3_128().hash(("item" + i).getBytes()));
        }

        HashCode combined = Hashing.combineOrdered(hashes);
        assertNotNull(combined);
    }

    @Test
    public void testCombineOrdered_iterable_emptyList() {
        List<HashCode> emptyList = new ArrayList<>();
        assertThrows(IllegalArgumentException.class, () -> Hashing.combineOrdered(emptyList));
    }

    @Test
    public void testCombineUnordered_two_basic() {
        HashCode hash1 = Hashing.sha256().hash("item1".getBytes());
        HashCode hash2 = Hashing.sha256().hash("item2".getBytes());

        HashCode combined = Hashing.combineUnordered(hash1, hash2);
        assertNotNull(combined);
        assertEquals(256, combined.bits());
    }

    @Test
    public void testCombineUnordered_two_orderDoesNotMatter() {
        HashCode hash1 = Hashing.sha256().hash("item1".getBytes());
        HashCode hash2 = Hashing.sha256().hash("item2".getBytes());

        HashCode combined1 = Hashing.combineUnordered(hash1, hash2);
        HashCode combined2 = Hashing.combineUnordered(hash2, hash1);

        assertEquals(combined1, combined2);
    }

    @Test
    public void testCombineUnordered_two_sameHash() {
        HashCode hash = Hashing.murmur3_128().hash("data".getBytes());

        HashCode combined = Hashing.combineUnordered(hash, hash);
        assertNotNull(combined);
    }

    @Test
    public void testCombineUnordered_three_basic() {
        HashCode hash1 = Hashing.murmur3_128().hash("a".getBytes());
        HashCode hash2 = Hashing.murmur3_128().hash("b".getBytes());
        HashCode hash3 = Hashing.murmur3_128().hash("c".getBytes());

        HashCode combined = Hashing.combineUnordered(hash1, hash2, hash3);
        assertNotNull(combined);
        assertEquals(128, combined.bits());
    }

    @Test
    public void testCombineUnordered_three_orderDoesNotMatter() {
        HashCode hash1 = Hashing.murmur3_128().hash("a".getBytes());
        HashCode hash2 = Hashing.murmur3_128().hash("b".getBytes());
        HashCode hash3 = Hashing.murmur3_128().hash("c".getBytes());

        HashCode combined1 = Hashing.combineUnordered(hash1, hash2, hash3);
        HashCode combined2 = Hashing.combineUnordered(hash3, hash1, hash2);
        HashCode combined3 = Hashing.combineUnordered(hash2, hash3, hash1);

        assertEquals(combined1, combined2);
        assertEquals(combined1, combined3);
    }

    @Test
    public void testCombineUnordered_iterable_basic() {
        List<HashCode> hashes = Arrays.asList(Hashing.sha256().hash("x".getBytes()), Hashing.sha256().hash("y".getBytes()),
                Hashing.sha256().hash("z".getBytes()));

        HashCode combined = Hashing.combineUnordered(hashes);
        assertNotNull(combined);
        assertEquals(256, combined.bits());
    }

    @Test
    public void testCombineUnordered_iterable_orderDoesNotMatter() {
        List<HashCode> hashes1 = Arrays.asList(Hashing.sha256().hash("1".getBytes()), Hashing.sha256().hash("2".getBytes()),
                Hashing.sha256().hash("3".getBytes()));

        List<HashCode> hashes2 = Arrays.asList(Hashing.sha256().hash("3".getBytes()), Hashing.sha256().hash("1".getBytes()),
                Hashing.sha256().hash("2".getBytes()));

        HashCode combined1 = Hashing.combineUnordered(hashes1);
        HashCode combined2 = Hashing.combineUnordered(hashes2);

        assertEquals(combined1, combined2);
    }

    @Test
    public void testConsistentHash_hashCode_basic() {
        HashCode hashCode = Hashing.sha256().hash("user123".getBytes());

        int bucket = Hashing.consistentHash(hashCode, 10);
        assertTrue(bucket >= 0 && bucket < 10);
    }

    @Test
    public void testConsistentHash_hashCode_consistency() {
        HashCode hashCode = Hashing.sha256().hash("user123".getBytes());

        int bucket1 = Hashing.consistentHash(hashCode, 10);
        int bucket2 = Hashing.consistentHash(hashCode, 10);

        assertEquals(bucket1, bucket2);
    }

    @Test
    public void testConsistentHash_hashCode_variousBucketCounts() {
        HashCode hashCode = Hashing.sha256().hash("test".getBytes());

        int bucket10 = Hashing.consistentHash(hashCode, 10);
        int bucket100 = Hashing.consistentHash(hashCode, 100);
        int bucket1000 = Hashing.consistentHash(hashCode, 1000);

        assertTrue(bucket10 >= 0 && bucket10 < 10);
        assertTrue(bucket100 >= 0 && bucket100 < 100);
        assertTrue(bucket1000 >= 0 && bucket1000 < 1000);
    }

    @Test
    public void testConsistentHash_hashCode_invalidBuckets() {
        HashCode hashCode = Hashing.sha256().hash("test".getBytes());

        assertThrows(IllegalArgumentException.class, () -> Hashing.consistentHash(hashCode, 0));
        assertThrows(IllegalArgumentException.class, () -> Hashing.consistentHash(hashCode, -1));
        assertThrows(IllegalArgumentException.class, () -> Hashing.consistentHash(hashCode, -100));
    }

    @Test
    public void testConsistentHash_long_basic() {
        long input = 12345L;

        int bucket = Hashing.consistentHash(input, 100);
        assertTrue(bucket >= 0 && bucket < 100);
    }

    @Test
    public void testConsistentHash_long_consistency() {
        long input = 12345L;

        int bucket1 = Hashing.consistentHash(input, 100);
        int bucket2 = Hashing.consistentHash(input, 100);

        assertEquals(bucket1, bucket2);
    }

    @Test
    public void testConsistentHash_long_differentInputs() {
        long input1 = 12345L;
        long input2 = 54321L;

        int bucket1 = Hashing.consistentHash(input1, 100);
        int bucket2 = Hashing.consistentHash(input2, 100);

        assertTrue(bucket1 >= 0 && bucket1 < 100);
        assertTrue(bucket2 >= 0 && bucket2 < 100);
    }

    @Test
    public void testConsistentHash_long_negativeInput() {
        long negativeInput = -12345L;

        int bucket = Hashing.consistentHash(negativeInput, 50);
        assertTrue(bucket >= 0 && bucket < 50);
    }

    @Test
    public void testConsistentHash_long_zeroInput() {
        long zeroInput = 0L;

        int bucket = Hashing.consistentHash(zeroInput, 10);
        assertTrue(bucket >= 0 && bucket < 10);
    }

    @Test
    public void testConsistentHash_long_invalidBuckets() {
        long input = 123L;

        assertThrows(IllegalArgumentException.class, () -> Hashing.consistentHash(input, 0));
        assertThrows(IllegalArgumentException.class, () -> Hashing.consistentHash(input, -1));
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
            into.putString(from.name, StandardCharsets.UTF_8).putInt(from.age);
        }
    };

    @Test
    public void testHashingWithFunnel_basic() {
        Person person = new Person("Alice", 30);

        HashFunction hf = Hashing.sha256();
        HashCode hash = hf.hash(person, PERSON_FUNNEL);

        assertNotNull(hash);
        assertEquals(32, hash.asBytes().length);
    }

    @Test
    public void testHashingWithFunnel_consistency() {
        Person person1 = new Person("Alice", 30);
        Person person2 = new Person("Alice", 30);

        HashFunction hf = Hashing.sha256();

        HashCode hash1 = hf.hash(person1, PERSON_FUNNEL);
        HashCode hash2 = hf.hash(person2, PERSON_FUNNEL);

        assertEquals(hash1, hash2);
    }

    @Test
    public void testHashingWithFunnel_differentData() {
        Person person1 = new Person("Alice", 30);
        Person person2 = new Person("Bob", 25);

        HashFunction hf = Hashing.sha256();

        HashCode hash1 = hf.hash(person1, PERSON_FUNNEL);
        HashCode hash2 = hf.hash(person2, PERSON_FUNNEL);

        assertNotEquals(hash1, hash2);
    }
}
