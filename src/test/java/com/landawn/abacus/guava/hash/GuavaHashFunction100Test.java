package com.landawn.abacus.guava.hash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.google.common.hash.Funnel;
import com.google.common.hash.HashCode;
import com.google.common.hash.PrimitiveSink;
import com.landawn.abacus.TestBase;

@Tag("new-test")
public class GuavaHashFunction100Test extends TestBase {

    @Test
    public void testWrapFunctionality() {
        HashFunction hf = Hashing.sha256();
        assertNotNull(hf);
        
        assertEquals(256, hf.bits());
        
        Hasher hasher1 = hf.newHasher();
        Hasher hasher2 = hf.newHasher(100);
        assertNotNull(hasher1);
        assertNotNull(hasher2);
        assertNotSame(hasher1, hasher2);
        
        byte[] data = "test".getBytes();
        HashCode hash1 = hf.hash(42);
        HashCode hash2 = hf.hash(123L);
        HashCode hash3 = hf.hash(data);
        HashCode hash4 = hf.hash(data, 0, data.length);
        HashCode hash5 = hf.hash("test");
        HashCode hash6 = hf.hash("test", StandardCharsets.UTF_8);
        
        assertNotNull(hash1);
        assertNotNull(hash2);
        assertNotNull(hash3);
        assertNotNull(hash4);
        assertNotNull(hash5);
        assertNotNull(hash6);
        
        assertEquals(hash3, hash4);
    }

    @Test
    public void testDifferentHashFunctions() {
        HashFunction sha256 = Hashing.sha256();
        HashFunction md5 = Hashing.md5();
        HashFunction murmur = Hashing.murmur3_128();
        
        byte[] data = "same data for all".getBytes();
        
        HashCode hashSha = sha256.hash(data);
        HashCode hashMd5 = md5.hash(data);
        HashCode hashMurmur = murmur.hash(data);
        
        assertNotEquals(hashSha, hashMd5);
        assertNotEquals(hashSha, hashMurmur);
        assertNotEquals(hashMd5, hashMurmur);
        
        assertNotEquals(sha256.bits(), md5.bits());
        assertEquals(md5.bits(), murmur.bits());
    }

    @Test
    public void testHasherCreation() {
        HashFunction hf = Hashing.sha256();
        
        Hasher hasher1 = hf.newHasher();
        hasher1.put("test".getBytes());
        HashCode hash1 = hasher1.hash();
        
        Hasher hasher2 = hf.newHasher(1000);
        hasher2.put("test".getBytes());
        HashCode hash2 = hasher2.hash();
        
        assertEquals(hash1, hash2);
        
        assertThrows(IllegalArgumentException.class, () -> hf.newHasher(-1));
    }

    @Test
    public void testHashMethodsDelegation() {
        HashFunction hf = Hashing.murmur3_32();
        
        int intValue = 42;
        HashCode hashInt1 = hf.hash(intValue);
        HashCode hashInt2 = hf.newHasher().put(intValue).hash();
        assertEquals(hashInt1, hashInt2);
        
        long longValue = 123456789L;
        HashCode hashLong1 = hf.hash(longValue);
        HashCode hashLong2 = hf.newHasher().put(longValue).hash();
        assertEquals(hashLong1, hashLong2);
        
        byte[] bytes = "test bytes".getBytes();
        HashCode hashBytes1 = hf.hash(bytes);
        HashCode hashBytes2 = hf.newHasher().put(bytes).hash();
        assertEquals(hashBytes1, hashBytes2);
        
        HashCode hashPartial1 = hf.hash(bytes, 0, 4);
        HashCode hashPartial2 = hf.newHasher().put(bytes, 0, 4).hash();
        assertEquals(hashPartial1, hashPartial2);
        
        CharSequence cs = "char sequence";
        HashCode hashCs1 = hf.hash(cs);
        HashCode hashCs2 = hf.newHasher().put(cs).hash();
        assertEquals(hashCs1, hashCs2);
        
        HashCode hashEncoded1 = hf.hash(cs, StandardCharsets.UTF_8);
        HashCode hashEncoded2 = hf.newHasher().put(cs, StandardCharsets.UTF_8).hash();
        assertEquals(hashEncoded1, hashEncoded2);
    }

    @Test
    public void testHashObjectWithFunnel() {
        HashFunction hf = Hashing.sha256();
        
        TestObject obj = new TestObject("test", 42);
        Funnel<TestObject> funnel = new Funnel<TestObject>() {
            @Override
            public void funnel(TestObject from, PrimitiveSink into) {
                into.putString(from.name, StandardCharsets.UTF_8)
                    .putInt(from.value);
            }
        };
        
        HashCode hash1 = hf.hash(obj, funnel);
        HashCode hash2 = hf.newHasher().put(obj, funnel).hash();
        assertEquals(hash1, hash2);
        
        TestObject obj2 = new TestObject("different", 99);
        HashCode hash3 = hf.hash(obj2, funnel);
        assertNotEquals(hash1, hash3);
    }

    @Test
    public void testBitsMethod() {
        assertEquals(32, Hashing.murmur3_32().bits());
        assertEquals(32, Hashing.murmur3_32(42).bits());
        assertEquals(128, Hashing.murmur3_128().bits());
        assertEquals(128, Hashing.murmur3_128(42).bits());
        assertEquals(64, Hashing.sipHash24().bits());
        assertEquals(64, Hashing.sipHash24(1L, 2L).bits());
        assertEquals(128, Hashing.md5().bits());
        assertEquals(160, Hashing.sha1().bits());
        assertEquals(256, Hashing.sha256().bits());
        assertEquals(384, Hashing.sha384().bits());
        assertEquals(512, Hashing.sha512().bits());
        assertEquals(32, Hashing.crc32c().bits());
        assertEquals(32, Hashing.crc32().bits());
        assertEquals(32, Hashing.adler32().bits());
        assertEquals(64, Hashing.farmHashFingerprint64().bits());
        
        assertTrue(Hashing.goodFastHash(32).bits() >= 32);
        assertTrue(Hashing.goodFastHash(64).bits() >= 64);
        assertTrue(Hashing.goodFastHash(128).bits() >= 128);
        
        HashFunction concat = Hashing.concatenating(
            Hashing.murmur3_32(),
            Hashing.murmur3_32()
        );
        assertEquals(64, concat.bits());
    }

    @Test
    public void testConsistencyAcrossInvocations() {
        HashFunction hf1 = Hashing.sha256();
        HashFunction hf2 = Hashing.sha256();
        
        byte[] data = "consistency test".getBytes();
        
        HashCode hash1 = hf1.hash(data);
        HashCode hash2 = hf1.hash(data);
        HashCode hash3 = hf2.hash(data);
        
        assertEquals(hash1, hash2);
        assertEquals(hash1, hash3);
        
        HashFunction murmur1 = Hashing.murmur3_128(42);
        HashFunction murmur2 = Hashing.murmur3_128(42);
        
        HashCode murmurHash1 = murmur1.hash(data);
        HashCode murmurHash2 = murmur2.hash(data);
        
        assertEquals(murmurHash1, murmurHash2);
    }

    @Test
    public void testNullHandling() {
        HashFunction hf = Hashing.sha256();
        
        assertThrows(NullPointerException.class, () -> hf.hash((byte[]) null));
        assertThrows(NullPointerException.class, () -> hf.hash(null, 0, 0));
        assertThrows(NullPointerException.class, () -> hf.hash((CharSequence) null));
        assertThrows(NullPointerException.class, () -> hf.hash(null, StandardCharsets.UTF_8));
        assertThrows(NullPointerException.class, () -> hf.hash("text", (Charset) null));
        
        TestObject obj = new TestObject("test", 1);
        Funnel<TestObject> funnel = (from, into) -> {};
        
        hf.hash(null, funnel);
        assertThrows(NullPointerException.class, () -> hf.hash(obj, null));
    }

    @Test
    public void testEmptyInputs() {
        HashFunction hf = Hashing.sha256();
        
        HashCode hashEmptyBytes = hf.hash(new byte[0]);
        HashCode hashEmptyString = hf.hash("");
        HashCode hashEmptyStringEncoded = hf.hash("", StandardCharsets.UTF_8);
        HashCode hashEmptyPartial = hf.hash(new byte[10], 5, 0);
        
        assertNotNull(hashEmptyBytes);
        assertNotNull(hashEmptyString);
        assertNotNull(hashEmptyStringEncoded);
        assertNotNull(hashEmptyPartial);
        
        HashCode hashEmptyHasher = hf.newHasher().hash();
        assertNotNull(hashEmptyHasher);
        
        assertEquals(hashEmptyString, hf.newHasher().put("").hash());
    }

    @Test
    public void testHashCodeProperties() {
        HashFunction hf = Hashing.sha256();
        byte[] data = "test data".getBytes();
        
        HashCode hash = hf.hash(data);
        
        assertEquals(32, hash.asBytes().length);
        assertEquals(256, hash.bits());
        assertNotNull(hash.toString());
        
        HashCode hash2 = hf.hash(data);
        assertEquals(hash, hash2);
        assertEquals(hash.hashCode(), hash2.hashCode());
        assertEquals(hash.toString(), hash2.toString());
    }

    private static class TestObject {
        final String name;
        final int value;
        
        TestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }
    }
}
