package com.landawn.abacus.guava.hash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.google.common.hash.Funnel;
import com.landawn.abacus.TestBase;

public class GuavaHashFunctionTest extends TestBase {

    private static class Person {
        final String name;
        final int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    private static GuavaHashFunction wrapMurmur() {
        return GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());
    }

    @Test
    public void testWrap() {
        com.google.common.hash.HashFunction murmur = com.google.common.hash.Hashing.murmur3_128();
        com.google.common.hash.HashFunction sha = com.google.common.hash.Hashing.sha256();
        GuavaHashFunction hashFunc = GuavaHashFunction.wrap(murmur);

        assertNotNull(hashFunc);
        assertSame(murmur, hashFunc.gHashFunction);
        assertNotSame(hashFunc.gHashFunction, GuavaHashFunction.wrap(sha).gHashFunction);
        assertThrows(NullPointerException.class, () -> GuavaHashFunction.wrap(null));
    }

    @Test
    public void testNewHasherAndHashOverloads() {
        GuavaHashFunction hashFunc = wrapMurmur();
        byte[] data = "test data".getBytes(StandardCharsets.UTF_8);
        Person person = new Person("Alice", 30);
        Funnel<Person> funnel = (from, into) -> into.putString(from.name, StandardCharsets.UTF_8).putInt(from.age);

        Hasher hasher = hashFunc.newHasher();
        Hasher sized = hashFunc.newHasher(1024);
        assertNotNull(hasher);
        assertTrue(hasher instanceof GuavaHasher);
        assertNotSame(hasher, sized);
        assertEquals(hashFunc.newHasher().put(data).hash(), hashFunc.newHasher(data.length).put(data).hash());
        assertEquals(hashFunc.newHasher().put(42).hash(), hashFunc.hash(42));
        assertThrows(IllegalArgumentException.class, () -> hashFunc.newHasher(-1));

        assertEquals(hashFunc.hash(12345), hashFunc.hash(12345));
        assertNotEquals(hashFunc.hash(1), hashFunc.hash(2));
        assertEquals(hashFunc.hash(data), hashFunc.hash(data, 0, data.length));
        assertEquals(hashFunc.hash(new byte[] { 2, 3, 4 }), hashFunc.hash(new byte[] { 1, 2, 3, 4, 5 }, 1, 3));
        assertEquals(hashFunc.hash("test"), hashFunc.hash(new StringBuilder("test")));
        assertNotEquals(hashFunc.hash("Test", StandardCharsets.UTF_8), hashFunc.hash("Test", StandardCharsets.UTF_16));
        assertEquals(hashFunc.hash(person, funnel), hashFunc.hash(person, funnel));
        assertNotEquals(hashFunc.hash(person, funnel), hashFunc.hash(new Person("Bob", 30), funnel));
        assertNotNull(hashFunc.hash(new byte[0]));
        assertNotNull(hashFunc.hash(""));
        assertNotNull(hashFunc.hash("Hello 世界 🌍"));
        assertThrows(NullPointerException.class, () -> hashFunc.hash((byte[]) null));
        assertThrows(NullPointerException.class, () -> hashFunc.hash((CharSequence) null));
        assertThrows(NullPointerException.class, () -> hashFunc.hash("x", (Charset) null));
        assertThrows(IllegalArgumentException.class, () -> hashFunc.hash(person, null));
    }

    @Test
    public void testBits() {
        assertEquals(32, GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_32_fixed()).bits());
        assertEquals(128, wrapMurmur().bits());
        assertEquals(256, GuavaHashFunction.wrap(com.google.common.hash.Hashing.sha256()).bits());
        assertEquals(512, GuavaHashFunction.wrap(com.google.common.hash.Hashing.sha512()).bits());
        assertEquals(32, GuavaHashFunction.wrap(com.google.common.hash.Hashing.crc32()).bits());
        @SuppressWarnings("deprecation")
        GuavaHashFunction md5 = GuavaHashFunction.wrap(com.google.common.hash.Hashing.md5());
        assertEquals(128, md5.bits());
    }

    @Test
    public void testEquals_ValueBasedFunctions() {
        assertEquals(Hashing.murmur3_128(42), Hashing.murmur3_128(42));
        assertEquals(Hashing.murmur3_128(42).hashCode(), Hashing.murmur3_128(42).hashCode());
        assertNotEquals(Hashing.murmur3_128(42), Hashing.murmur3_128(43));

        assertEquals(Hashing.murmur3_32(7), Hashing.murmur3_32(7));
        assertEquals(Hashing.sipHash24(1L, 2L), Hashing.sipHash24(1L, 2L));
        assertNotEquals(Hashing.sipHash24(1L, 2L), Hashing.sipHash24(2L, 1L));

        assertEquals(Hashing.goodFastHash(300), Hashing.goodFastHash(300));
        assertEquals(Hashing.goodFastHash(128), Hashing.goodFastHash(100));

        HashFunction c1 = Hashing.concatenating(Hashing.murmur3_128(), Hashing.sha256());
        HashFunction c2 = Hashing.concatenating(Hashing.murmur3_128(), Hashing.sha256());
        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
        assertNotEquals(c1, Hashing.concatenating(Hashing.sha256(), Hashing.murmur3_128()));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testEquals_SingletonFunctions() {
        assertSame(Hashing.sha256(), Hashing.sha256());
        assertEquals(Hashing.sha256(), Hashing.sha256());
        assertEquals(Hashing.md5(), Hashing.md5());
        assertEquals(Hashing.crc32(), Hashing.crc32());
        assertEquals(Hashing.crc32c(), Hashing.crc32c());
        assertEquals(Hashing.adler32(), Hashing.adler32());
        assertEquals(Hashing.farmHashFingerprint64(), Hashing.farmHashFingerprint64());
        assertNotEquals(Hashing.sha256(), Hashing.sha512());
    }

    @Test
    public void testEquals_HmacIsIdentityBased() {
        byte[] key = "secret".getBytes(StandardCharsets.UTF_8);

        HashFunction h1 = Hashing.hmacSha256(key);
        HashFunction h2 = Hashing.hmacSha256(key);
        assertNotSame(h1, h2);
        assertNotEquals(h1, h2);
        assertEquals(h1, h1);
        assertEquals(h1.hash("m".getBytes(StandardCharsets.UTF_8)), h2.hash("m".getBytes(StandardCharsets.UTF_8)));

        assertNotEquals(Hashing.hmacMd5(key), Hashing.hmacMd5(key));
        assertNotEquals(Hashing.hmacSha1(key), Hashing.hmacSha1(key));
        assertNotEquals(Hashing.hmacSha512(key), Hashing.hmacSha512(key));
        assertNotEquals(h1, null);
        assertNotEquals(h1, "Hashing.hmacSha256");
    }

    @Test
    public void testToString_DelegatesToGuava() {
        assertEquals("Hashing.murmur3_32(7)", Hashing.murmur3_32(7).toString());
        assertEquals("Hashing.murmur3_128(42)", Hashing.murmur3_128(42).toString());
        assertEquals("Hashing.sha256()", Hashing.sha256().toString());

        String concat = Hashing.concatenating(Hashing.murmur3_128(), Hashing.sha256()).toString();
        assertTrue(concat.startsWith("com.google.common.hash.Hashing$ConcatenatedHashFunction@"), concat);
    }
}
