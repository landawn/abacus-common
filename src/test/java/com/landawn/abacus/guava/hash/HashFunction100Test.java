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
import com.google.common.hash.HashCode;
import com.google.common.hash.PrimitiveSink;

import com.landawn.abacus.TestBase;


public class HashFunction100Test extends TestBase {

    private HashFunction hashFunction;
    
    @BeforeEach
    public void setUp() {
        // Use a concrete implementation for testing
        hashFunction = Hashing.sha256();
    }

    @Test
    public void testNewHasher() {
        Hasher hasher1 = hashFunction.newHasher();
        Hasher hasher2 = hashFunction.newHasher();
        
        assertNotNull(hasher1);
        assertNotNull(hasher2);
        assertNotSame(hasher1, hasher2); // Different instances
        
        // Test that hashers produce same result for same input
        byte[] data = "test data".getBytes();
        HashCode hash1 = hasher1.put(data).hash();
        HashCode hash2 = hasher2.put(data).hash();
        assertEquals(hash1, hash2);
    }

    @Test
    public void testNewHasherWithExpectedInputSize() {
        Hasher hasher1 = hashFunction.newHasher(100);
        Hasher hasher2 = hashFunction.newHasher(1000);
        
        assertNotNull(hasher1);
        assertNotNull(hasher2);
        
        // Both should work correctly regardless of hint
        byte[] smallData = "small".getBytes();
        byte[] largeData = new byte[1000];
        Arrays.fill(largeData, (byte) 42);
        
        HashCode hash1 = hasher1.put(smallData).hash();
        HashCode hash2 = hasher2.put(smallData).hash();
        assertEquals(hash1, hash2);
        
        // Test negative size throws exception
        assertThrows(IllegalArgumentException.class, () -> hashFunction.newHasher(-1));
    }

    @Test
    public void testHashInt() {
        HashCode hash1 = hashFunction.hash(42);
        HashCode hash2 = hashFunction.hash(42);
        HashCode hash3 = hashFunction.hash(43);
        
        assertEquals(hash1, hash2);
        assertNotEquals(hash1, hash3);
        
        // Test edge cases
        HashCode hashMin = hashFunction.hash(Integer.MIN_VALUE);
        HashCode hashMax = hashFunction.hash(Integer.MAX_VALUE);
        HashCode hashZero = hashFunction.hash(0);
        
        assertNotNull(hashMin);
        assertNotNull(hashMax);
        assertNotNull(hashZero);
        assertNotEquals(hashMin, hashMax);
    }

    @Test
    public void testHashLong() {
        HashCode hash1 = hashFunction.hash(12345L);
        HashCode hash2 = hashFunction.hash(12345L);
        HashCode hash3 = hashFunction.hash(54321L);
        
        assertEquals(hash1, hash2);
        assertNotEquals(hash1, hash3);
        
        // Test edge cases
        HashCode hashMin = hashFunction.hash(Long.MIN_VALUE);
        HashCode hashMax = hashFunction.hash(Long.MAX_VALUE);
        HashCode hashZero = hashFunction.hash(0L);
        
        assertNotNull(hashMin);
        assertNotNull(hashMax);
        assertNotNull(hashZero);
    }

    @Test
    public void testHashByteArray() {
        byte[] data1 = "Hello, World!".getBytes(StandardCharsets.UTF_8);
        byte[] data2 = "Hello, World!".getBytes(StandardCharsets.UTF_8);
        byte[] data3 = "Goodbye, World!".getBytes(StandardCharsets.UTF_8);
        
        HashCode hash1 = hashFunction.hash(data1);
        HashCode hash2 = hashFunction.hash(data2);
        HashCode hash3 = hashFunction.hash(data3);
        
        assertEquals(hash1, hash2);
        assertNotEquals(hash1, hash3);
        
        // Test empty array
        HashCode hashEmpty = hashFunction.hash(new byte[0]);
        assertNotNull(hashEmpty);
        
        // Test null throws exception
        assertThrows(NullPointerException.class, () -> hashFunction.hash((byte[]) null));
    }

    @Test
    public void testHashByteArrayWithOffsetAndLength() {
        byte[] buffer = "Hello, World!".getBytes();
        
        HashCode hash1 = hashFunction.hash(buffer, 0, 5); // "Hello"
        HashCode hash2 = hashFunction.hash(buffer, 7, 5); // "World"
        HashCode hash3 = hashFunction.hash(buffer, 0, buffer.length); // Full array
        
        assertNotNull(hash1);
        assertNotNull(hash2);
        assertNotNull(hash3);
        assertNotEquals(hash1, hash2);
        
        // Test same content produces same hash
        byte[] hello = "Hello".getBytes();
        HashCode hash4 = hashFunction.hash(hello);
        assertEquals(hash1, hash4);
        
        // Test boundary conditions
        HashCode hashEmpty = hashFunction.hash(buffer, 0, 0);
        assertNotNull(hashEmpty);
        
        // Test exceptions
        assertThrows(IndexOutOfBoundsException.class, () -> hashFunction.hash(buffer, -1, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> hashFunction.hash(buffer, 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> hashFunction.hash(buffer, 0, buffer.length + 1));
        assertThrows(IndexOutOfBoundsException.class, () -> hashFunction.hash(buffer, buffer.length, 1));
        assertThrows(NullPointerException.class, () -> hashFunction.hash(null, 0, 0));
    }

    @Test
    public void testHashCharSequence() {
        CharSequence cs1 = "fast hash";
        CharSequence cs2 = new StringBuilder("fast hash");
        CharSequence cs3 = "different";
        
        HashCode hash1 = hashFunction.hash(cs1);
        HashCode hash2 = hashFunction.hash(cs2);
        HashCode hash3 = hashFunction.hash(cs3);
        
        assertEquals(hash1, hash2);
        assertNotEquals(hash1, hash3);
        
        // Test empty string
        HashCode hashEmpty = hashFunction.hash("");
        assertNotNull(hashEmpty);
        
        // Test null throws exception
        assertThrows(NullPointerException.class, () -> hashFunction.hash((CharSequence) null));
    }

    @Test
    public void testHashCharSequenceWithCharset() {
        String text = "Hello 世界";
        
        HashCode hashUtf8 = hashFunction.hash(text, StandardCharsets.UTF_8);
        HashCode hashUtf16 = hashFunction.hash(text, StandardCharsets.UTF_16);
        HashCode hashAscii = hashFunction.hash("Hello", StandardCharsets.US_ASCII);
        
        assertNotNull(hashUtf8);
        assertNotNull(hashUtf16);
        assertNotNull(hashAscii);
        assertNotEquals(hashUtf8, hashUtf16); // Different encodings
        
        // Test consistency
        HashCode hashUtf8_2 = hashFunction.hash(text, StandardCharsets.UTF_8);
        assertEquals(hashUtf8, hashUtf8_2);
        
        // Test null throws exception
        assertThrows(NullPointerException.class, () -> hashFunction.hash(text, (Charset) null));
        assertThrows(NullPointerException.class, () -> hashFunction.hash(null, StandardCharsets.UTF_8));
    }

    @Test
    public void testHashObjectWithFunnel() {
        Person person1 = new Person("Alice", 30, 12345L);
        Person person2 = new Person("Alice", 30, 12345L);
        Person person3 = new Person("Bob", 25, 54321L);
        
        Funnel<Person> personFunnel = new Funnel<Person>() {
            @Override
            public void funnel(Person from, PrimitiveSink into) {
                into.putString(from.name, StandardCharsets.UTF_8)
                    .putInt(from.age)
                    .putLong(from.id);
            }
        };
        
        HashCode hash1 = hashFunction.hash(person1, personFunnel);
        HashCode hash2 = hashFunction.hash(person2, personFunnel);
        HashCode hash3 = hashFunction.hash(person3, personFunnel);
        
        assertEquals(hash1, hash2);
        assertNotEquals(hash1, hash3);
        
        // Test null throws exception
        assertThrows(NullPointerException.class, () -> hashFunction.hash(person1, null));
        assertThrows(NullPointerException.class, () -> hashFunction.hash(null, personFunnel));
    }

    @Test
    public void testBits() {
        assertEquals(256, hashFunction.bits()); // SHA-256
        
        // Test different hash functions have correct bit counts
        assertEquals(32, Hashing.murmur3_32().bits());
        assertEquals(128, Hashing.murmur3_128().bits());
        assertEquals(128, Hashing.md5().bits());
        assertEquals(160, Hashing.sha1().bits());
        assertEquals(384, Hashing.sha384().bits());
        assertEquals(512, Hashing.sha512().bits());
        assertEquals(64, Hashing.sipHash24().bits());
        assertEquals(32, Hashing.crc32().bits());
        assertEquals(32, Hashing.adler32().bits());
        assertEquals(64, Hashing.farmHashFingerprint64().bits());
        
        // Test that bits() is always positive
        assertTrue(Hashing.goodFastHash(1).bits() > 0);
        assertTrue(Hashing.goodFastHash(256).bits() > 0);
    }

    @Test
    public void testComparisonBetweenHashMethods() {
        // Test that convenience methods produce same results as using Hasher
        byte[] data = "test data".getBytes();
        
        // Compare hash(byte[]) with newHasher().putBytes()
        HashCode hash1 = hashFunction.hash(data);
        HashCode hash2 = hashFunction.newHasher().put(data).hash();
        assertEquals(hash1, hash2);
        
        // Compare hash(int) with newHasher().putInt()
        HashCode hash3 = hashFunction.hash(42);
        HashCode hash4 = hashFunction.newHasher().put(42).hash();
        assertEquals(hash3, hash4);
        
        // Compare hash(CharSequence) with newHasher().putUnencodedChars()
        CharSequence text = "hello";
        HashCode hash5 = hashFunction.hash(text);
        HashCode hash6 = hashFunction.newHasher().put(text).hash();
        assertEquals(hash5, hash6);
        
        // Compare hash(CharSequence, Charset) with newHasher().putString()
        HashCode hash7 = hashFunction.hash(text, StandardCharsets.UTF_8);
        HashCode hash8 = hashFunction.newHasher().put(text, StandardCharsets.UTF_8).hash();
        assertEquals(hash7, hash8);
    }

    // Helper class for testing
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
}
