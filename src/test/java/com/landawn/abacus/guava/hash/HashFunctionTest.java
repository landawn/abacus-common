package com.landawn.abacus.guava.hash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.hash.Funnel;
import com.landawn.abacus.TestBase;

public class HashFunctionTest extends TestBase {

    private HashFunction hashFunction;

    @BeforeEach
    public void setUp() {
        hashFunction = Hashing.sha256();
    }

    private static class Person {
        final String name;
        final int age;
        final long id;

        Person(String name, int age, long id) {
            this.name = name;
            this.age = age;
            this.id = id;
        }
    }

    @Test
    public void testNewHasher() {
        HashFunction hashFunc = Hashing.murmur3_128();
        Hasher hasher1 = hashFunc.newHasher();
        Hasher hasher2 = hashFunc.newHasher();
        Hasher sized = hashFunc.newHasher(1024);

        assertNotNull(hasher1);
        assertNotSame(hasher1, hasher2);
        assertNotNull(sized);
        assertNotNull(hashFunc.newHasher(0));
        assertEquals(hashFunc.newHasher().put("test").hash(), hashFunc.newHasher().put("test").hash());

        byte[] data = "test data".getBytes(StandardCharsets.UTF_8);
        assertEquals(hashFunc.newHasher().put(data).hash(), hashFunc.newHasher(10).put(data).hash());
        assertEquals(hashFunc.newHasher().put(data).hash(), hashFunc.newHasher(1000).put(data).hash());
        assertEquals(hashFunc.newHasher().put(42).hash(), hashFunc.hash(42));
        assertThrows(IllegalArgumentException.class, () -> hashFunction.newHasher(-1));
    }

    @Test
    public void testHashPrimitivesAndBytes() {
        HashFunction hashFunc = Hashing.murmur3_128();
        byte[] data = { 10, 20, 30, 40, 50 };
        byte[] buffer = "Hello, World!".getBytes(StandardCharsets.UTF_8);

        assertEquals(hashFunc.hash(12345), hashFunc.hash(12345));
        assertNotEquals(hashFunc.hash(Integer.MIN_VALUE), hashFunc.hash(Integer.MAX_VALUE));
        assertEquals(hashFunc.hash(1L), hashFunc.hash(1L));
        assertNotEquals(hashFunc.hash(Long.MIN_VALUE), hashFunc.hash(Long.MAX_VALUE));
        assertEquals(hashFunc.hash(data), hashFunc.hash(data, 0, data.length));
        assertEquals(hashFunc.hash(new byte[] { 2, 3, 4 }), hashFunc.hash(new byte[] { 1, 2, 3, 4, 5 }, 1, 3));
        assertEquals(hashFunction.hash(buffer, 0, 5), hashFunction.hash("Hello".getBytes(StandardCharsets.UTF_8)));
        assertNotEquals(hashFunction.hash(buffer, 0, 5), hashFunction.hash(buffer, 7, 5));
        assertNotNull(hashFunc.hash(new byte[0]));
        assertNotNull(hashFunc.hash(data, 0, 0));
        assertNotNull(hashFunction.hash(new byte[10000]));

        assertThrows(IndexOutOfBoundsException.class, () -> hashFunction.hash(buffer, -1, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> hashFunction.hash(buffer, 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> hashFunction.hash(buffer, 0, buffer.length + 1));
        assertThrows(IndexOutOfBoundsException.class, () -> hashFunction.hash(buffer, buffer.length, 1));
        assertThrows(NullPointerException.class, () -> hashFunction.hash(null, 0, 0));
    }

    @Test
    public void testHashCharSequenceAndFunnel() {
        HashFunction hashFunc = Hashing.murmur3_128();
        Funnel<Person> personFunnel = (from, into) -> into.putString(from.name, StandardCharsets.UTF_8).putInt(from.age).putLong(from.id);
        Person alice = new Person("Alice", 30, 12345L);
        Person alice2 = new Person("Alice", 30, 12345L);
        Person bob = new Person("Bob", 25, 54321L);

        assertEquals(hashFunc.hash("test"), hashFunc.hash("test"));
        assertNotNull(hashFunc.hash(""));
        assertNotNull(hashFunc.hash("Hello 世界 🌍"));
        assertEquals(hashFunc.hash("Test", StandardCharsets.UTF_8), hashFunc.hash("Test", StandardCharsets.ISO_8859_1));
        assertNotEquals(hashFunc.hash("Test", StandardCharsets.UTF_8), hashFunc.hash("Test", StandardCharsets.UTF_16));
        assertEquals(hashFunction.hash(alice, personFunnel), hashFunction.hash(alice2, personFunnel));
        assertNotEquals(hashFunction.hash(alice, personFunnel), hashFunction.hash(bob, personFunnel));
        assertEquals(hashFunction.hash("hello"), hashFunction.newHasher().put("hello").hash());
        assertEquals(hashFunction.hash("hello", StandardCharsets.UTF_8), hashFunction.newHasher().put("hello", StandardCharsets.UTF_8).hash());

        assertThrows(IllegalArgumentException.class, () -> hashFunction.hash(alice, null));
        assertThrows(NullPointerException.class, () -> hashFunction.hash(null, personFunnel));
    }

    @Test
    public void testBitsAndImmutability() {
        HashFunction murmur32 = Hashing.murmur3_32();
        HashFunction murmur128 = Hashing.murmur3_128();
        HashFunction sha256 = Hashing.sha256();

        assertEquals(32, murmur32.bits());
        assertEquals(128, murmur128.bits());
        assertEquals(256, sha256.bits());
        assertEquals(128, murmur128.bits());
        assertEquals(murmur128.hash("test1"), murmur128.hash("test1"));
        assertNotEquals(murmur128.hash("test1"), murmur128.hash("test2"));
        assertNotEquals(murmur128.hash("test"), murmur128.hash("Test"));
        assertNotEquals(murmur128.hash(0), murmur128.hash(1));
    }

    @Test
    public void testHashCharSequenceWithCharset_UnpairedSurrogateCollides() {
        for (HashFunction hf : Arrays.asList(Hashing.sha256(), Hashing.murmur3_32(), Hashing.murmur3_128(), Hashing.crc32(), Hashing.farmHashFingerprint64())) {
            assertEquals(hf.hash("a?", StandardCharsets.UTF_8), hf.hash("a\uD800", StandardCharsets.UTF_8), hf.toString());
            assertNotEquals(hf.hash("a?"), hf.hash("a\uD800"), hf.toString());
        }
    }

    @Test
    public void testHashCharSequenceWithCharset_UnmappableCharCollides() {
        for (HashFunction hf : Arrays.asList(Hashing.sha256(), Hashing.murmur3_128())) {
            assertEquals(hf.hash("?", StandardCharsets.ISO_8859_1), hf.hash("世", StandardCharsets.ISO_8859_1), hf.toString());
            assertNotEquals(hf.hash("?"), hf.hash("世"), hf.toString());
            assertNotEquals(hf.hash("?", StandardCharsets.UTF_8), hf.hash("世", StandardCharsets.UTF_8), hf.toString());
        }
    }

    @Test
    public void testHashCharSequenceWithCharset_EquivalenceAndNulls() {
        HashFunction hf = Hashing.sha256();
        assertEquals(hf.newHasher().put("Hello 世界", StandardCharsets.UTF_8).hash(), hf.hash("Hello 世界", StandardCharsets.UTF_8));
        assertThrows(NullPointerException.class, () -> hf.hash((CharSequence) null, StandardCharsets.UTF_8));
        assertThrows(NullPointerException.class, () -> hf.hash("x", (Charset) null));
    }

    @Test
    public void testHashObjectWithFunnel_NullContracts() {
        HashFunction hf = Hashing.sha256();

        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () -> hf.hash("x", (Funnel<String>) null));
        assertNotNull(iae.getMessage());
        assertTrue(iae.getMessage().contains("funnel"));

        Funnel<Object> noop = (from, into) -> {
        };
        assertNotNull(hf.hash(null, noop));

        Funnel<Person> readsName = (p, into) -> into.putUnencodedChars(p.name);
        assertThrows(NullPointerException.class, () -> hf.hash((Person) null, readsName));
    }
}
