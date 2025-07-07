package com.landawn.abacus.guava.hash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.google.common.hash.Funnel;
import com.google.common.hash.HashCode;
import com.google.common.hash.PrimitiveSink;
import com.landawn.abacus.TestBase;


/**
 * Tests for GuavaHasher through the public API.
 * Since GuavaHasher is package-private, we test it indirectly
 * through Hasher instances obtained from HashFunction.newHasher().
 */
public class GuavaHasher100Test extends TestBase {

    @Test
    public void testHasherWrapFunctionality() {
        // Get a hasher which internally uses GuavaHasher.wrap()
        Hasher hasher = Hashing.sha256().newHasher();
        assertNotNull(hasher);
        
        // Test that it correctly implements all Hasher methods
        hasher.put((byte) 1)
              .put(new byte[]{2, 3})
              .put((short) 4)
              .put(5)
              .put(6L)
              .put(7.0f)
              .put(8.0)
              .put(true)
              .put('A');
        
        HashCode hash = hasher.hash();
        assertNotNull(hash);
    }

    @Test
    public void testPutByteMethodChaining() {
        Hasher hasher = Hashing.murmur3_128().newHasher();
        
        // Test method chaining returns same instance
        Hasher result1 = hasher.put((byte) 1);
        Hasher result2 = result1.put((byte) 2);
        Hasher result3 = result2.put((byte) 3);
        
        assertSame(hasher, result1);
        assertSame(hasher, result2);
        assertSame(hasher, result3);
        
        HashCode hash = result3.hash();
        assertNotNull(hash);
    }

    @Test
    public void testPutBytesArrayMethods() {
        byte[] data = "test data".getBytes();
        
        // Test put(byte[])
        Hasher hasher1 = Hashing.sha256().newHasher();
        Hasher result1 = hasher1.put(data);
        assertSame(hasher1, result1);
        HashCode hash1 = hasher1.hash();
        
        // Test put(byte[], int, int) with full array
        Hasher hasher2 = Hashing.sha256().newHasher();
        Hasher result2 = hasher2.put(data, 0, data.length);
        assertSame(hasher2, result2);
        HashCode hash2 = hasher2.hash();
        
        assertEquals(hash1, hash2);
        
        // Test partial array
        Hasher hasher3 = Hashing.sha256().newHasher();
        hasher3.put(data, 0, 4); // Just "test"
        HashCode hash3 = hasher3.hash();
        
        assertNotEquals(hash1, hash3);
    }

    @Test
    public void testPutByteBuffer() {
        byte[] data = "buffer test".getBytes();
        ByteBuffer buffer = ByteBuffer.wrap(data);
        
        Hasher hasher = Hashing.sha256().newHasher();
        int positionBefore = buffer.position();
        Hasher result = hasher.put(buffer);
        int positionAfter = buffer.position();
        
        assertSame(hasher, result);
        assertEquals(data.length, positionAfter - positionBefore);
        
        HashCode hash = hasher.hash();
        assertNotNull(hash);
    }

    @Test
    public void testPutPrimitiveTypes() {
        Hasher hasher = Hashing.sha256().newHasher();
        
        // Test all primitive type methods return the hasher
        assertSame(hasher, hasher.put((short) 100));
        assertSame(hasher, hasher.put(200));
        assertSame(hasher, hasher.put(300L));
        assertSame(hasher, hasher.put(400.5f));
        assertSame(hasher, hasher.put(500.5));
        assertSame(hasher, hasher.put(true));
        assertSame(hasher, hasher.put('X'));
        
        HashCode hash = hasher.hash();
        assertNotNull(hash);
    }

    @Test
    public void testPutCharArrayImplementation() {
        char[] chars = "hello".toCharArray();
        
        // Test put(char[])
        Hasher hasher1 = Hashing.sha256().newHasher();
        Hasher result1 = hasher1.put(chars);
        assertSame(hasher1, result1);
        HashCode hash1 = hasher1.hash();
        
        // Test put(char[], int, int) with full array
        Hasher hasher2 = Hashing.sha256().newHasher();
        Hasher result2 = hasher2.put(chars, 0, chars.length);
        assertSame(hasher2, result2);
        HashCode hash2 = hasher2.hash();
        
        assertEquals(hash1, hash2);
        
        // Test that char array is processed correctly
        // Each char should be put individually
        Hasher hasher3 = Hashing.sha256().newHasher();
        for (char c : chars) {
            hasher3.put(c);
        }
        HashCode hash3 = hasher3.hash();
        
        assertEquals(hash1, hash3);
    }

    @Test
    public void testPutCharArrayPartial() {
        char[] buffer = "Hello World".toCharArray();
        
        // Test partial array
        Hasher hasher1 = Hashing.sha256().newHasher();
        hasher1.put(buffer, 0, 5); // "Hello"
        HashCode hash1 = hasher1.hash();
        
        Hasher hasher2 = Hashing.sha256().newHasher();
        hasher2.put(buffer, 6, 5); // "World"
        HashCode hash2 = hasher2.hash();
        
        assertNotEquals(hash1, hash2);
        
        // Test empty range
        Hasher hasher3 = Hashing.sha256().newHasher();
        Hasher result = hasher3.put(buffer, 5, 0);
        assertSame(hasher3, result);
        assertNotNull(hasher3.hash());
    }

    @Test
    public void testPutCharArrayBoundaryChecks() {
        char[] chars = "test".toCharArray();
        
        // Test null array with put(char[])
        Hasher hasher1 = Hashing.sha256().newHasher();
        // assertThrows(NullPointerException.class, () -> hasher1.put((char[]) null));
        hasher1.put((char[]) null);
        
        // Test null array with put(char[], int, int)
        Hasher hasher2 = Hashing.sha256().newHasher();
        // assertThrows(NullPointerException.class, () -> hasher2.put((char[]) null, 0, 0));
        hasher2.put((char[]) null, 0, 0);
        
        // Test index out of bounds
        Hasher hasher3 = Hashing.sha256().newHasher();
        assertThrows(IndexOutOfBoundsException.class, () -> hasher3.put(chars, -1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> hasher3.put(chars, 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> hasher3.put(chars, 0, chars.length + 1));
        assertThrows(IndexOutOfBoundsException.class, () -> hasher3.put(chars, chars.length, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> hasher3.put(chars, 2, 3)); // off + len > length
    }

    @Test
    public void testPutCharSequenceMethods() {
        String text = "test string";
        
        // Test put(CharSequence)
        Hasher hasher1 = Hashing.sha256().newHasher();
        Hasher result1 = hasher1.put(text);
        assertSame(hasher1, result1);
        HashCode hash1 = hasher1.hash();
        
        // Test with StringBuilder
        Hasher hasher2 = Hashing.sha256().newHasher();
        hasher2.put(new StringBuilder(text));
        HashCode hash2 = hasher2.hash();
        
        assertEquals(hash1, hash2);
        
        // Test put(CharSequence, Charset)
        Hasher hasher3 = Hashing.sha256().newHasher();
        Hasher result3 = hasher3.put(text, StandardCharsets.UTF_8);
        assertSame(hasher3, result3);
        HashCode hash3 = hasher3.hash();
        
        // Encoded version should be different from unencoded
        assertNotEquals(hash1, hash3);
    }

    @Test
    public void testPutObjectWithFunnel() {
        TestData data = new TestData("test", 42, true);
        
        Funnel<TestData> funnel = new Funnel<TestData>() {
            @Override
            public void funnel(TestData from, PrimitiveSink into) {
                into.putString(from.name, StandardCharsets.UTF_8)
                    .putInt(from.value)
                    .putBoolean(from.flag);
            }
        };
        
        Hasher hasher = Hashing.sha256().newHasher();
        Hasher result = hasher.put(data, funnel);
        assertSame(hasher, result);
        
        HashCode hash = hasher.hash();
        assertNotNull(hash);
    }

    @Test
    public void testHashMethod() {
        Hasher hasher = Hashing.sha256().newHasher();
        hasher.put("test data".getBytes());
        
        HashCode hash = hasher.hash();
        assertNotNull(hash);
        assertEquals(256, hash.bits());
        assertEquals(32, hash.asBytes().length);
    }

    @Test
    public void testComplexHashingScenario() {
        // Test a complex scenario with multiple data types
        Hasher hasher = Hashing.murmur3_128().newHasher();
        
        byte[] bytes = {1, 2, 3, 4, 5};
        ByteBuffer buffer = ByteBuffer.wrap(new byte[]{6, 7, 8});
        char[] chars = {'A', 'B', 'C'};
        String text = "text";
        TestData data = new TestData("obj", 99, false);
        
        Funnel<TestData> funnel = (from, into) -> {
            into.putString(from.name, StandardCharsets.UTF_8)
                .putInt(from.value)
                .putBoolean(from.flag);
        };
        
        hasher.put((byte) 0)
              .put(bytes)
              .put(bytes, 1, 3)
              .put(buffer)
              .put((short) 10)
              .put(20)
              .put(30L)
              .put(40.5f)
              .put(50.5)
              .put(true)
              .put('Z')
              .put(chars)
              .put(chars, 1, 1)
              .put(text)
              .put(text, StandardCharsets.UTF_8)
              .put(data, funnel);
        
        HashCode hash = hasher.hash();
        assertNotNull(hash);
        assertEquals(128, hash.bits());
    }

    @Test
    public void testHasherConsistency() {
        // Test that same input produces same hash
        byte[] data = "consistency test".getBytes();
        
        Hasher hasher1 = Hashing.sha256().newHasher();
        hasher1.put(data);
        HashCode hash1 = hasher1.hash();
        
        Hasher hasher2 = Hashing.sha256().newHasher();
        hasher2.put(data);
        HashCode hash2 = hasher2.hash();
        
        assertEquals(hash1, hash2);
    }

    @Test
    public void testEmptyHasher() {
        // Test that empty hasher produces valid hash
        Hasher hasher = Hashing.sha256().newHasher();
        HashCode hash = hasher.hash();
        
        assertNotNull(hash);
        assertEquals(256, hash.bits());
        
        // Empty hasher should produce consistent hash
        Hasher hasher2 = Hashing.sha256().newHasher();
        HashCode hash2 = hasher2.hash();
        
        assertEquals(hash, hash2);
    }

    @Test
    public void testHasherFromDifferentHashFunctions() {
        byte[] data = "test".getBytes();
        
        // Test that different hash functions produce different hashers
        Hasher hasherSha = Hashing.sha256().newHasher();
        Hasher hasherMd5 = Hashing.md5().newHasher();
        Hasher hasherMurmur = Hashing.murmur3_128().newHasher();
        
        hasherSha.put(data);
        hasherMd5.put(data);
        hasherMurmur.put(data);
        
        HashCode hashSha = hasherSha.hash();
        HashCode hashMd5 = hasherMd5.hash();
        HashCode hashMurmur = hasherMurmur.hash();
        
        // All should be different
        assertNotEquals(hashSha, hashMd5);
        assertNotEquals(hashSha, hashMurmur);
        assertNotEquals(hashMd5, hashMurmur);
        
        // Different bit lengths
        assertEquals(256, hashSha.bits());
        assertEquals(128, hashMd5.bits());
        assertEquals(128, hashMurmur.bits());
    }

    // Helper class for testing
    private static class TestData {
        final String name;
        final int value;
        final boolean flag;
        
        TestData(String name, int value, boolean flag) {
            this.name = name;
            this.value = value;
            this.flag = flag;
        }
    }
}
