package com.landawn.abacus.guava.hash;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.google.common.hash.Funnel;
import com.google.common.hash.HashCode;
import com.landawn.abacus.TestBase;

/**
 * Comprehensive unit tests for GuavaHasher class.
 * Tests all public methods including those inherited from Hasher interface.
 */
@Tag("2025")
public class GuavaHasher2025Test extends TestBase {

    // Test wrap() factory method

    @Test
    @DisplayName("Test wrap creates GuavaHasher instance")
    public void testWrap() {
        com.google.common.hash.Hasher guavaHasher = com.google.common.hash.Hashing.murmur3_128().newHasher();
        GuavaHasher hasher = GuavaHasher.wrap(guavaHasher);

        assertNotNull(hasher);
        assertSame(guavaHasher, hasher.gHasher);
    }

    @Test
    @DisplayName("Test wrap with different Guava hashers")
    public void testWrapWithDifferentHashers() {
        com.google.common.hash.Hasher murmur3Hasher = com.google.common.hash.Hashing.murmur3_32_fixed().newHasher();
        com.google.common.hash.Hasher sha256Hasher = com.google.common.hash.Hashing.sha256().newHasher();

        GuavaHasher hasher1 = GuavaHasher.wrap(murmur3Hasher);
        GuavaHasher hasher2 = GuavaHasher.wrap(sha256Hasher);

        assertNotNull(hasher1);
        assertNotNull(hasher2);
        assertNotSame(hasher1, hasher2);
    }

    // Test put(byte) method

    @Test
    @DisplayName("Test put single byte")
    public void testPutByte() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());

        Hasher result = hasher.put((byte) 42);

        assertNotNull(result);
        assertSame(hasher, result); // Test chaining
    }

    @Test
    @DisplayName("Test put byte boundary values")
    public void testPutByteBoundaries() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());

        hasher.put(Byte.MIN_VALUE).put((byte) 0).put(Byte.MAX_VALUE);
        HashCode hash = hasher.hash();

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test put byte produces consistent hash")
    public void testPutByteConsistency() {
        HashCode hash1 = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher())
            .put((byte) 100)
            .hash();

        HashCode hash2 = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher())
            .put((byte) 100)
            .hash();

        assertEquals(hash1, hash2);
    }

    // Test put(byte[]) method

    @Test
    @DisplayName("Test put byte array")
    public void testPutByteArray() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());
        byte[] bytes = {1, 2, 3, 4, 5};

        Hasher result = hasher.put(bytes);

        assertNotNull(result);
        assertSame(hasher, result);
    }

    @Test
    @DisplayName("Test put empty byte array")
    public void testPutEmptyByteArray() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());
        byte[] bytes = new byte[0];

        Hasher result = hasher.put(bytes);
        HashCode hash = result.hash();

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test put large byte array")
    public void testPutLargeByteArray() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());
        byte[] bytes = new byte[10000];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (i % 256);
        }

        HashCode hash = hasher.put(bytes).hash();

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test put byte array produces consistent hash")
    public void testPutByteArrayConsistency() {
        byte[] bytes = "test data".getBytes(StandardCharsets.UTF_8);

        HashCode hash1 = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher())
            .put(bytes)
            .hash();

        HashCode hash2 = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher())
            .put(bytes)
            .hash();

        assertEquals(hash1, hash2);
    }

    // Test put(byte[], int, int) method

    @Test
    @DisplayName("Test put byte array with offset and length")
    public void testPutByteArrayOffsetLength() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());
        byte[] bytes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

        Hasher result = hasher.put(bytes, 2, 5);
        HashCode hash = result.hash();

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test put byte array with zero length")
    public void testPutByteArrayZeroLength() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());
        byte[] bytes = {1, 2, 3};

        HashCode hash = hasher.put(bytes, 0, 0).hash();

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test put byte array full range")
    public void testPutByteArrayFullRange() {
        byte[] bytes = {10, 20, 30, 40, 50};

        HashCode hash1 = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher())
            .put(bytes)
            .hash();

        HashCode hash2 = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher())
            .put(bytes, 0, bytes.length)
            .hash();

        assertEquals(hash1, hash2);
    }

    // Test put(ByteBuffer) method

    @Test
    @DisplayName("Test put ByteBuffer")
    public void testPutByteBuffer() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());
        ByteBuffer buffer = ByteBuffer.wrap(new byte[]{1, 2, 3, 4, 5});

        HashCode hash = hasher.put(buffer).hash();

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test put ByteBuffer with position and limit")
    public void testPutByteBufferWithPositionLimit() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());
        ByteBuffer buffer = ByteBuffer.wrap(new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9});
        buffer.position(2);
        buffer.limit(7);

        HashCode hash = hasher.put(buffer).hash();

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test put empty ByteBuffer")
    public void testPutEmptyByteBuffer() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());
        ByteBuffer buffer = ByteBuffer.allocate(0);

        HashCode hash = hasher.put(buffer).hash();

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test put direct ByteBuffer")
    public void testPutDirectByteBuffer() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());
        ByteBuffer buffer = ByteBuffer.allocateDirect(10);
        buffer.put(new byte[]{1, 2, 3, 4, 5});
        buffer.flip();

        HashCode hash = hasher.put(buffer).hash();

        assertNotNull(hash);
    }

    // Test put(short) method

    @Test
    @DisplayName("Test put short")
    public void testPutShort() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());

        HashCode hash = hasher.put((short) 1234).hash();

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test put short boundary values")
    public void testPutShortBoundaries() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());

        hasher.put(Short.MIN_VALUE).put((short) 0).put(Short.MAX_VALUE);
        HashCode hash = hasher.hash();

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test put negative short")
    public void testPutNegativeShort() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());

        HashCode hash = hasher.put((short) -500).hash();

        assertNotNull(hash);
    }

    // Test put(int) method

    @Test
    @DisplayName("Test put int")
    public void testPutInt() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());

        HashCode hash = hasher.put(42).hash();

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test put int boundary values")
    public void testPutIntBoundaries() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());

        hasher.put(Integer.MIN_VALUE).put(0).put(Integer.MAX_VALUE);
        HashCode hash = hasher.hash();

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test put int consistency")
    public void testPutIntConsistency() {
        HashCode hash1 = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher())
            .put(12345)
            .hash();

        HashCode hash2 = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher())
            .put(12345)
            .hash();

        assertEquals(hash1, hash2);
    }

    // Test put(long) method

    @Test
    @DisplayName("Test put long")
    public void testPutLong() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());

        HashCode hash = hasher.put(1234567890123L).hash();

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test put long boundary values")
    public void testPutLongBoundaries() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());

        hasher.put(Long.MIN_VALUE).put(0L).put(Long.MAX_VALUE);
        HashCode hash = hasher.hash();

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test put long consistency")
    public void testPutLongConsistency() {
        long value = System.currentTimeMillis();

        HashCode hash1 = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher())
            .put(value)
            .hash();

        HashCode hash2 = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher())
            .put(value)
            .hash();

        assertEquals(hash1, hash2);
    }

    // Test put(float) method

    @Test
    @DisplayName("Test put float")
    public void testPutFloat() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());

        HashCode hash = hasher.put(3.14159f).hash();

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test put float special values")
    public void testPutFloatSpecialValues() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());

        hasher.put(Float.NaN)
              .put(Float.POSITIVE_INFINITY)
              .put(Float.NEGATIVE_INFINITY)
              .put(0.0f)
              .put(-0.0f);
        HashCode hash = hasher.hash();

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test put float boundary values")
    public void testPutFloatBoundaries() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());

        hasher.put(Float.MIN_VALUE).put(Float.MAX_VALUE);
        HashCode hash = hasher.hash();

        assertNotNull(hash);
    }

    // Test put(double) method

    @Test
    @DisplayName("Test put double")
    public void testPutDouble() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());

        HashCode hash = hasher.put(Math.PI).hash();

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test put double special values")
    public void testPutDoubleSpecialValues() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());

        hasher.put(Double.NaN)
              .put(Double.POSITIVE_INFINITY)
              .put(Double.NEGATIVE_INFINITY)
              .put(0.0)
              .put(-0.0);
        HashCode hash = hasher.hash();

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test put double boundary values")
    public void testPutDoubleBoundaries() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());

        hasher.put(Double.MIN_VALUE).put(Double.MAX_VALUE);
        HashCode hash = hasher.hash();

        assertNotNull(hash);
    }

    // Test put(boolean) method

    @Test
    @DisplayName("Test put boolean true")
    public void testPutBooleanTrue() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());

        HashCode hash = hasher.put(true).hash();

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test put boolean false")
    public void testPutBooleanFalse() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());

        HashCode hash = hasher.put(false).hash();

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test put boolean different values produce different hashes")
    public void testPutBooleanDifferentHashes() {
        HashCode hashTrue = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher())
            .put(true)
            .hash();

        HashCode hashFalse = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher())
            .put(false)
            .hash();

        assertNotEquals(hashTrue, hashFalse);
    }

    // Test put(char) method

    @Test
    @DisplayName("Test put char")
    public void testPutChar() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());

        HashCode hash = hasher.put('A').hash();

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test put char special characters")
    public void testPutCharSpecialCharacters() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());

        hasher.put('\n').put('\t').put('\0').put('中').put('\uFFFF');
        HashCode hash = hasher.hash();

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test put char boundary values")
    public void testPutCharBoundaries() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());

        hasher.put(Character.MIN_VALUE).put(Character.MAX_VALUE);
        HashCode hash = hasher.hash();

        assertNotNull(hash);
    }

    // Test put(char[]) method

    @Test
    @DisplayName("Test put char array")
    public void testPutCharArray() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());
        char[] chars = {'h', 'e', 'l', 'l', 'o'};

        HashCode hash = hasher.put(chars).hash();

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test put empty char array")
    public void testPutEmptyCharArray() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());
        char[] chars = new char[0];

        HashCode hash = hasher.put(chars).hash();

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test put null char array")
    public void testPutNullCharArray() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());
        char[] chars = null;

        HashCode hash = hasher.put(chars).hash();

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test put char array with unicode")
    public void testPutCharArrayUnicode() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());
        char[] chars = "Hello 世界".toCharArray();

        HashCode hash = hasher.put(chars).hash();

        assertNotNull(hash);
    }

    // Test put(char[], int, int) method

    @Test
    @DisplayName("Test put char array with offset and length")
    public void testPutCharArrayOffsetLength() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());
        char[] chars = {'a', 'b', 'c', 'd', 'e'};

        HashCode hash = hasher.put(chars, 1, 3).hash();

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test put char array with zero length")
    public void testPutCharArrayZeroLength() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());
        char[] chars = {'a', 'b', 'c'};

        HashCode hash = hasher.put(chars, 0, 0).hash();

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test put char array bounds validation")
    public void testPutCharArrayBoundsValidation() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());
        char[] chars = {'a', 'b', 'c'};

        assertThrows(IndexOutOfBoundsException.class, () -> hasher.put(chars, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> hasher.put(chars, 0, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> hasher.put(chars, 2, 3));
    }

    // Test put(CharSequence) method

    @Test
    @DisplayName("Test put CharSequence")
    public void testPutCharSequence() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());

        HashCode hash = hasher.put("Hello World").hash();

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test put empty CharSequence")
    public void testPutEmptyCharSequence() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());

        HashCode hash = hasher.put("").hash();

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test put CharSequence with StringBuilder")
    public void testPutCharSequenceStringBuilder() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());
        StringBuilder sb = new StringBuilder("test");

        HashCode hash = hasher.put(sb).hash();

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test put CharSequence with unicode")
    public void testPutCharSequenceUnicode() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());

        HashCode hash = hasher.put("Hello 世界 🌍").hash();

        assertNotNull(hash);
    }

    // Test put(CharSequence, Charset) method

    @Test
    @DisplayName("Test put CharSequence with charset")
    public void testPutCharSequenceWithCharset() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());

        HashCode hash = hasher.put("Hello", StandardCharsets.UTF_8).hash();

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test put CharSequence with different charsets")
    public void testPutCharSequenceMultipleCharsets() {
        String text = "Test";

        HashCode hashUTF8 = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher())
            .put(text, StandardCharsets.UTF_8)
            .hash();

        HashCode hashUTF16 = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher())
            .put(text, StandardCharsets.UTF_16)
            .hash();

        assertNotEquals(hashUTF8, hashUTF16);
    }

    @Test
    @DisplayName("Test put CharSequence with charset unicode")
    public void testPutCharSequenceWithCharsetUnicode() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());

        HashCode hash = hasher.put("世界", StandardCharsets.UTF_8).hash();

        assertNotNull(hash);
    }

    // Test put(T, Funnel<T>) method

    @Test
    @DisplayName("Test put with Funnel")
    public void testPutWithFunnel() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());

        Funnel<Person> personFunnel = (person, into) -> {
            into.putString(person.name, StandardCharsets.UTF_8);
            into.putInt(person.age);
        };

        Person person = new Person("Alice", 30);
        HashCode hash = hasher.put(person, personFunnel).hash();

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test put with Funnel consistency")
    public void testPutWithFunnelConsistency() {
        Funnel<Person> personFunnel = (person, into) -> {
            into.putString(person.name, StandardCharsets.UTF_8);
            into.putInt(person.age);
        };

        Person person = new Person("Bob", 25);

        HashCode hash1 = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher())
            .put(person, personFunnel)
            .hash();

        HashCode hash2 = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher())
            .put(person, personFunnel)
            .hash();

        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Test put with complex Funnel")
    public void testPutWithComplexFunnel() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());

        Funnel<Person> complexFunnel = (person, into) -> {
            into.putString(person.name, StandardCharsets.UTF_8);
            into.putInt(person.age);
            into.putBoolean(person.age >= 18);
            into.putLong(person.name.length());
        };

        Person person = new Person("Charlie", 35);
        HashCode hash = hasher.put(person, complexFunnel).hash();

        assertNotNull(hash);
    }

    // Test hash() method

    @Test
    @DisplayName("Test hash method")
    public void testHash() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());

        hasher.put("test");
        HashCode hash = hasher.hash();

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test hash produces consistent results")
    public void testHashConsistency() {
        HashCode hash1 = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher())
            .put("test data")
            .put(123)
            .hash();

        HashCode hash2 = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher())
            .put("test data")
            .put(123)
            .hash();

        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Test hash with different hash functions")
    public void testHashDifferentFunctions() {
        byte[] data = "test".getBytes(StandardCharsets.UTF_8);

        HashCode murmurHash = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher())
            .put(data)
            .hash();

        HashCode sha256Hash = GuavaHasher.wrap(com.google.common.hash.Hashing.sha256().newHasher())
            .put(data)
            .hash();

        assertNotNull(murmurHash);
        assertNotNull(sha256Hash);
        assertNotEquals(murmurHash.bits(), sha256Hash.bits());
    }

    // Test chaining

    @Test
    @DisplayName("Test method chaining")
    public void testMethodChaining() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());

        HashCode hash = hasher
            .put((byte) 1)
            .put(new byte[]{2, 3})
            .put((short) 4)
            .put(5)
            .put(6L)
            .put(7.0f)
            .put(8.0)
            .put(true)
            .put('c')
            .put("test")
            .hash();

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test complex chaining scenario")
    public void testComplexChaining() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());

        Funnel<Person> personFunnel = (person, into) -> {
            into.putString(person.name, StandardCharsets.UTF_8);
            into.putInt(person.age);
        };

        Person person = new Person("Dave", 40);

        HashCode hash = hasher
            .put("User:")
            .put(person, personFunnel)
            .put(System.currentTimeMillis())
            .put(true)
            .hash();

        assertNotNull(hash);
    }

    // Test integration scenarios

    @Test
    @DisplayName("Test hashing user credentials")
    public void testHashingUserCredentials() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.sha256().newHasher());

        String username = "john.doe@example.com";
        String password = "secret123";
        long timestamp = System.currentTimeMillis();

        HashCode hash = hasher
            .put(username, StandardCharsets.UTF_8)
            .put(password, StandardCharsets.UTF_8)
            .put(timestamp)
            .hash();

        assertNotNull(hash);
        assertEquals(256, hash.bits());
    }

    @Test
    @DisplayName("Test hashing file metadata")
    public void testHashingFileMetadata() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());

        String filename = "document.pdf";
        long fileSize = 1024 * 1024;
        byte[] header = {0x25, 0x50, 0x44, 0x46};

        HashCode hash = hasher
            .put(filename, StandardCharsets.UTF_8)
            .put(fileSize)
            .put(header)
            .hash();

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test hashing with mixed types")
    public void testHashingMixedTypes() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());

        HashCode hash = hasher
            .put((byte) 127)
            .put((short) 32767)
            .put(2147483647)
            .put(9223372036854775807L)
            .put(3.14f)
            .put(2.718281828459045)
            .put(true)
            .put('Z')
            .put("mixed types")
            .hash();

        assertNotNull(hash);
    }

    // Edge cases

    @Test
    @DisplayName("Test empty hasher produces hash")
    public void testEmptyHasher() {
        GuavaHasher hasher = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());

        HashCode hash = hasher.hash();

        assertNotNull(hash);
    }

    @Test
    @DisplayName("Test hashing same data produces same result")
    public void testHashingDeterminism() {
        byte[] data = "deterministic test".getBytes(StandardCharsets.UTF_8);

        HashCode hash1 = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher())
            .put(data)
            .hash();

        HashCode hash2 = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher())
            .put(data)
            .hash();

        HashCode hash3 = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher())
            .put(data)
            .hash();

        assertEquals(hash1, hash2);
        assertEquals(hash2, hash3);
    }

    @Test
    @DisplayName("Test hashing order matters")
    public void testHashingOrderMatters() {
        HashCode hash1 = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher())
            .put("first")
            .put("second")
            .hash();

        HashCode hash2 = GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher())
            .put("second")
            .put("first")
            .hash();

        assertNotEquals(hash1, hash2);
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
