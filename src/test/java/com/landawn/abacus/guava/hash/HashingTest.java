package com.landawn.abacus.guava.hash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Arrays;
import java.util.List;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

import com.google.common.hash.HashCode;
import com.landawn.abacus.TestBase;

public class HashingTest extends TestBase {

    private static final byte[] DATA = "test".getBytes(StandardCharsets.UTF_8);

    @Test
    public void testGoodFastHash() {
        assertEquals(32, Hashing.goodFastHash(1).bits());
        assertEquals(32, Hashing.goodFastHash(32).bits());
        assertEquals(128, Hashing.goodFastHash(33).bits());
        assertEquals(128, Hashing.goodFastHash(128).bits());
        assertEquals(256, Hashing.goodFastHash(129).bits());
        assertEquals(384, Hashing.goodFastHash(300).bits());
        assertEquals(Hashing.goodFastHash(128), Hashing.goodFastHash(128));
        assertEquals(Hashing.goodFastHash(128).hash(DATA), Hashing.goodFastHash(100).hash(DATA));

        HashFunction upper = Hashing.goodFastHash(Integer.MAX_VALUE - 127);
        assertTrue(upper.bits() >= Integer.MAX_VALUE - 127);
        assertEquals(0, upper.bits() % 128);

        assertThrows(IllegalArgumentException.class, () -> Hashing.goodFastHash(0));
        assertThrows(IllegalArgumentException.class, () -> Hashing.goodFastHash(-1));
        assertThrows(IllegalArgumentException.class, () -> Hashing.goodFastHash(Integer.MIN_VALUE));
        IllegalArgumentException max = assertThrows(IllegalArgumentException.class, () -> Hashing.goodFastHash(Integer.MAX_VALUE));
        assertTrue(max.getMessage().contains(String.valueOf(Integer.MAX_VALUE - 127)));
        assertThrows(IllegalArgumentException.class, () -> Hashing.goodFastHash(Integer.MAX_VALUE - 40));
        assertThrows(IllegalArgumentException.class, () -> Hashing.goodFastHash(Integer.MAX_VALUE - 126));
    }

    @Test
    public void testMurmur3_32() {
        HashFunction noSeed = Hashing.murmur3_32();
        HashFunction seed0 = Hashing.murmur3_32(0);
        HashFunction seed42 = Hashing.murmur3_32(42);
        HashFunction seedNeg = Hashing.murmur3_32(-1);

        assertEquals(32, noSeed.bits());
        assertEquals(32, seed42.bits());
        assertEquals(32, seedNeg.bits());
        assertEquals(noSeed.hash(DATA), Hashing.murmur3_32().hash(DATA));
        assertEquals(seed42.hash(DATA), Hashing.murmur3_32(42).hash(DATA));
        assertNotEquals(seed0.hash(DATA), seed42.hash(DATA));
        assertNotEquals(seed0.hash(DATA), seedNeg.hash(DATA));
    }

    @Test
    public void testMurmur3_128() {
        HashFunction noSeed = Hashing.murmur3_128();
        HashFunction seed0 = Hashing.murmur3_128(0);
        HashFunction seed42 = Hashing.murmur3_128(42);

        assertEquals(128, noSeed.bits());
        assertEquals(128, seed42.bits());
        assertEquals(noSeed.hash(DATA), Hashing.murmur3_128().hash(DATA));
        assertEquals(seed42.hash(DATA), Hashing.murmur3_128(42).hash(DATA));
        assertNotEquals(seed0.hash(DATA), seed42.hash(DATA));
    }

    @Test
    public void testSipHash24() {
        long k0 = 0x0706050403020100L;
        long k1 = 0x0f0e0d0c0b0a0908L;
        HashFunction def = Hashing.sipHash24();
        HashFunction keyed = Hashing.sipHash24(k0, k1);

        assertEquals(64, def.bits());
        assertEquals(64, keyed.bits());
        assertEquals(8, def.hash(DATA).asBytes().length);
        assertEquals(keyed.hash(DATA), Hashing.sipHash24(k0, k1).hash(DATA));
        assertNotEquals(keyed.hash(DATA), Hashing.sipHash24(k1, k0).hash(DATA));
        assertNotEquals(Hashing.sipHash24(0, 0).hash(DATA), Hashing.sipHash24(1, 1).hash(DATA));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testCryptographicAndChecksumFactories() {
        assertEquals(128, Hashing.md5().bits());
        assertEquals(160, Hashing.sha1().bits());
        assertEquals(256, Hashing.sha256().bits());
        assertEquals(384, Hashing.sha384().bits());
        assertEquals(512, Hashing.sha512().bits());
        assertEquals(32, Hashing.crc32c().bits());
        assertEquals(32, Hashing.crc32().bits());
        assertEquals(32, Hashing.adler32().bits());
        assertEquals(64, Hashing.farmHashFingerprint64().bits());

        byte[] hello = "Hello World".getBytes(StandardCharsets.UTF_8);
        assertEquals(Hashing.md5().hash(hello), Hashing.md5().hash(hello));
        assertEquals(16, Hashing.md5().hash(hello).asBytes().length);
        assertEquals(20, Hashing.sha1().hash(hello).asBytes().length);
        assertEquals(32, Hashing.sha256().hash(hello).asBytes().length);
        assertEquals(4, Hashing.crc32c().hash(hello).asBytes().length);
        assertEquals(4, Hashing.crc32().hash(hello).asBytes().length);
        assertEquals(4, Hashing.adler32().hash(hello).asBytes().length);
        assertEquals(Hashing.farmHashFingerprint64().hash(DATA), Hashing.farmHashFingerprint64().hash(DATA));

        HashCode sha256 = Hashing.sha256().hash(DATA);
        HashCode sha384 = Hashing.sha384().hash(DATA);
        HashCode sha512 = Hashing.sha512().hash(DATA);
        assertEquals(256, sha256.bits());
        assertEquals(384, sha384.bits());
        assertEquals(512, sha512.bits());
        assertNotEquals(sha256, sha384);
        assertNotEquals(Hashing.crc32c().hash(DATA), Hashing.crc32().hash(DATA));
        assertNotEquals(Hashing.crc32().hash(DATA), Hashing.adler32().hash(DATA));
    }

    @Test
    public void testHmacFactories() {
        byte[] keyBytes = "secret".getBytes(StandardCharsets.UTF_8);
        byte[] message = "message".getBytes(StandardCharsets.UTF_8);
        Key md5Key = new SecretKeySpec(keyBytes, "HmacMD5");
        Key sha1Key = new SecretKeySpec(keyBytes, "HmacSHA1");
        Key sha256Key = new SecretKeySpec(keyBytes, "HmacSHA256");
        Key sha512Key = new SecretKeySpec(keyBytes, "HmacSHA512");

        assertEquals(128, Hashing.hmacMd5(md5Key).bits());
        assertEquals(128, Hashing.hmacMd5(keyBytes).bits());
        assertEquals(160, Hashing.hmacSha1(sha1Key).bits());
        assertEquals(160, Hashing.hmacSha1(keyBytes).bits());
        assertEquals(256, Hashing.hmacSha256(sha256Key).bits());
        assertEquals(256, Hashing.hmacSha256(keyBytes).bits());
        assertEquals(512, Hashing.hmacSha512(sha512Key).bits());
        assertEquals(512, Hashing.hmacSha512(keyBytes).bits());

        assertEquals(Hashing.hmacSha1(sha1Key).hash(message), Hashing.hmacSha1(sha1Key).hash(message));
        assertEquals(Hashing.hmacSha1(keyBytes).hash(message), Hashing.hmacSha1(keyBytes).hash(message));
        assertEquals(Hashing.hmacSha256(keyBytes).hash(message), Hashing.hmacSha256(keyBytes).hash(message));
        assertEquals(Hashing.hmacSha512(sha512Key).hash(message), Hashing.hmacSha512(sha512Key).hash(message));
        assertNotEquals(Hashing.hmacSha256(keyBytes).hash(message), Hashing.hmacSha256("secret2".getBytes(StandardCharsets.UTF_8)).hash(message));

        assertEquals(Hashing.hmacMd5(keyBytes).hash(message), Hashing.hmacMd5(new SecretKeySpec(keyBytes, "HmacSHA256")).hash(message));
        assertEquals(Hashing.hmacMd5(keyBytes).hash(message), Hashing.hmacMd5(new SecretKeySpec(keyBytes, "AES")).hash(message));
        assertEquals(Hashing.hmacSha256(keyBytes).hash(message), Hashing.hmacSha256(new SecretKeySpec(keyBytes, "HmacMD5")).hash(message));

        Key publicLikeKey = new Key() {
            @Override
            public String getAlgorithm() {
                return "RSA";
            }

            @Override
            public String getFormat() {
                return "X.509";
            }

            @Override
            public byte[] getEncoded() {
                return new byte[] { 1, 2, 3 };
            }
        };
        assertThrows(IllegalArgumentException.class, () -> Hashing.hmacMd5(publicLikeKey));
        assertThrows(IllegalArgumentException.class, () -> Hashing.hmacSha1(publicLikeKey));
        assertThrows(IllegalArgumentException.class, () -> Hashing.hmacSha256(publicLikeKey));
        assertThrows(IllegalArgumentException.class, () -> Hashing.hmacSha512(publicLikeKey));
        assertThrows(NullPointerException.class, () -> Hashing.hmacMd5((Key) null));
        assertThrows(NullPointerException.class, () -> Hashing.hmacSha256((Key) null));
        assertThrows(IllegalArgumentException.class, () -> Hashing.hmacMd5(new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> Hashing.hmacSha512(new byte[0]));
    }

    @Test
    public void testConcatenating() {
        HashFunction two = Hashing.concatenating(Hashing.murmur3_32(), Hashing.murmur3_32(42));
        HashFunction three = Hashing.concatenating(Hashing.murmur3_128(), Hashing.murmur3_128(42), Hashing.murmur3_128(123));
        HashFunction iterable = Hashing.concatenating(Arrays.asList(Hashing.murmur3_32(), Hashing.murmur3_32(), Hashing.murmur3_32()));
        HashFunction mixed = Hashing.concatenating(Hashing.murmur3_32(), Hashing.murmur3_128(), Hashing.sha256());

        assertEquals(64, two.bits());
        assertEquals(8, two.hash(DATA).asBytes().length);
        assertEquals(384, three.bits());
        assertEquals(96, iterable.bits());
        assertEquals(416, mixed.bits());
        assertEquals(two.hash(DATA), Hashing.concatenating(Hashing.murmur3_32(), Hashing.murmur3_32(42)).hash(DATA));

        HashFunction unsupported = (HashFunction) java.lang.reflect.Proxy.newProxyInstance(HashFunction.class.getClassLoader(),
                new Class<?>[] { HashFunction.class }, (proxy, method, args) -> {
                    throw new UnsupportedOperationException("Proxy hash function should not be invoked");
                });
        assertThrows(IllegalArgumentException.class, () -> Hashing.concatenating(Arrays.asList(Hashing.sha256(), unsupported)));

        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class,
                () -> Hashing.concatenating(Arrays.asList(Hashing.sha256(), (HashFunction) null)));
        assertTrue(iae.getMessage().contains("null"));
        assertThrows(IllegalArgumentException.class, () -> Hashing.concatenating(List.of()));
    }

    @Test
    public void testCombineOrderedAndUnordered() {
        HashCode a = Hashing.murmur3_128().hash("test1");
        HashCode b = Hashing.murmur3_128().hash("test2");
        HashCode c = Hashing.murmur3_128().hash("test3");

        assertEquals(128, Hashing.combineOrdered(a, b).bits());
        assertEquals(128, Hashing.combineOrdered(a, b, c).bits());
        assertEquals(Hashing.combineOrdered(a, b), Hashing.combineOrdered(a, b));
        assertNotEquals(Hashing.combineOrdered(a, b), Hashing.combineOrdered(b, a));
        assertEquals(Hashing.combineOrdered(a, b, c), Hashing.combineOrdered(Arrays.asList(a, b, c)));

        assertEquals(Hashing.combineUnordered(a, b), Hashing.combineUnordered(b, a));
        assertEquals(Hashing.combineUnordered(a, b, c), Hashing.combineUnordered(c, a, b));
        assertEquals(Hashing.combineUnordered(a, b, c), Hashing.combineUnordered(Arrays.asList(a, b, c)));
        assertEquals(a, Hashing.combineOrdered(List.of(a)));
        assertThrows(IllegalArgumentException.class, () -> Hashing.combineOrdered(List.of()));
        assertThrows(IllegalArgumentException.class, () -> Hashing.combineOrdered(Hashing.sha256().hash(DATA), Hashing.sha512().hash(DATA)));
    }

    @Test
    public void testConsistentHash() {
        HashCode hashCode = Hashing.murmur3_128().hash("test");
        long value = 123456789L;

        int fromHash = Hashing.consistentHash(hashCode, 10);
        int fromLong = Hashing.consistentHash(value, 100);
        assertTrue(fromHash >= 0 && fromHash < 10);
        assertTrue(fromLong >= 0 && fromLong < 100);
        assertEquals(fromHash, Hashing.consistentHash(hashCode, 10));
        assertEquals(fromLong, Hashing.consistentHash(value, 100));
        assertEquals(0, Hashing.consistentHash(value, 1));

        int bucket10 = Hashing.consistentHash(value, 10);
        int bucket11 = Hashing.consistentHash(value, 11);
        assertTrue(bucket11 == bucket10 || bucket11 == 10);

        assertTrue(Hashing.consistentHash(-12345L, 50) >= 0);
        assertTrue(Hashing.consistentHash(0L, 10) < 10);
        assertThrows(IllegalArgumentException.class, () -> Hashing.consistentHash(hashCode, 0));
        assertThrows(IllegalArgumentException.class, () -> Hashing.consistentHash(hashCode, -1));
        assertThrows(IllegalArgumentException.class, () -> Hashing.consistentHash(123L, 0));
        assertThrows(IllegalArgumentException.class, () -> Hashing.consistentHash(123L, -1));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testCombineAndConcatenatingNullArguments() {
        HashCode hc = Hashing.sha256().hash("x".getBytes(StandardCharsets.UTF_8));

        assertThrows(NullPointerException.class, () -> Hashing.combineOrdered(hc, null));
        assertThrows(NullPointerException.class, () -> Hashing.combineOrdered(null, hc));
        assertThrows(NullPointerException.class, () -> Hashing.combineOrdered(hc, hc, null));
        assertThrows(NullPointerException.class, () -> Hashing.combineOrdered((Iterable<HashCode>) null));
        assertThrows(NullPointerException.class, () -> Hashing.combineOrdered(Arrays.asList(hc, null)));

        assertThrows(NullPointerException.class, () -> Hashing.combineUnordered(hc, null));
        assertThrows(NullPointerException.class, () -> Hashing.combineUnordered(hc, hc, null));
        assertThrows(NullPointerException.class, () -> Hashing.combineUnordered((Iterable<HashCode>) null));

        assertThrows(NullPointerException.class, () -> Hashing.consistentHash((HashCode) null, 3));

        assertThrows(NullPointerException.class, () -> Hashing.concatenating((Iterable<HashFunction>) null));
        assertThrows(IllegalArgumentException.class, () -> Hashing.concatenating(Arrays.asList(Hashing.sha256(), null)));
        assertThrows(IllegalArgumentException.class, () -> Hashing.concatenating(null, Hashing.sha256()));
        assertThrows(IllegalArgumentException.class, () -> Hashing.concatenating(Hashing.sha256(), null));
        assertThrows(IllegalArgumentException.class, () -> Hashing.concatenating(Hashing.sha256(), Hashing.md5(), null));
    }
}
