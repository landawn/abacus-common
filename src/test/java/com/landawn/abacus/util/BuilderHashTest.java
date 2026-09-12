package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.ToIntFunction;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Builder.HashCodeBuilder;

public class BuilderHashTest extends BuilderTestSupport {
    @Test
    public void testHash_object() {
        int hash = Builder.hash("test").result();
        assertEquals("test".hashCode(), hash);
    }

    @Test
    public void testHash_withFunction() {
        int hash = Builder.hash("test", String::length).result();
        assertEquals(4, hash);
    }

    @Test
    public void testHash_boolean() {
        int hash = Builder.hash(true).result();
        assertEquals(1231, hash);
    }

    @Test
    public void testHash_booleanFalse() {
        int hash = Builder.hash(false).result();
        assertEquals(1237, hash);
    }

    @Test
    public void testHash_char() {
        int hash = Builder.hash('a').result();
        assertEquals('a', hash);
    }

    @Test
    public void testHash_byte() {
        int hash = Builder.hash((byte) 1).result();
        assertEquals(1, hash);
    }

    @Test
    public void testHash_short() {
        int hash = Builder.hash((short) 1).result();
        assertEquals(1, hash);
    }

    @Test
    public void testHash_int() {
        int hash = Builder.hash(1).result();
        assertEquals(1, hash);
    }

    @Test
    public void testHash_long() {
        long value = 1L;
        int expectedHash = (int) (value ^ (value >>> 32));
        int hash = Builder.hash(value).result();
        assertEquals(expectedHash, hash);
    }

    @Test
    public void testHash_float() {
        float value = 1.0f;
        int expectedHash = Float.floatToIntBits(value);
        int hash = Builder.hash(value).result();
        assertEquals(expectedHash, hash);
    }

    @Test
    public void testHash_double() {
        double value = 1.0;
        long bits = Double.doubleToLongBits(value);
        int expectedHash = (int) (bits ^ (bits >>> 32));
        int hash = Builder.hash(value).result();
        assertEquals(expectedHash, hash);
    }

    @Test
    public void testHashCodeBuilder() {
        int expected = 31 * CommonUtil.hashCode(1) + CommonUtil.hashCode("a");
        assertEquals(expected, Builder.hash(1).hash("a").result());

        expected = 31 * (31 * CommonUtil.hashCode(1) + CommonUtil.hashCode("a")) + CommonUtil.hashCode("b");
        assertEquals(expected, Builder.hash(1).hash("a").hash("b").result());

        ToIntFunction<String> lenFunc = String::length;
        expected = 31 * CommonUtil.hashCode(1) + lenFunc.applyAsInt("abc");
        assertEquals(expected, Builder.hash(1).hash("abc", lenFunc).result());
    }

    @Test
    public void testHash() {
        int hash1 = Builder.hash("test").result();
        int hash2 = Builder.hash("test").result();
        Assertions.assertEquals(hash1, hash2);

        int hash3 = Builder.hash("different").result();
        Assertions.assertNotEquals(hash1, hash3);
    }

    @Test
    public void testHashCodeBuilderConsistency() {
        Object[] values = { "test", 123, true, 45.6, 'x' };

        int hash1 = Builder.hash(values[0]).hash(values[1]).hash((boolean) values[2]).hash((double) values[3]).hash((char) values[4]).result();

        int hash2 = Builder.hash(values[0]).hash(values[1]).hash((boolean) values[2]).hash((double) values[3]).hash((char) values[4]).result();

        assertEquals(hash1, hash2);
    }

    @Test
    public void testHashCodeBuilderChain() {
        int hash1 = Builder.hash("a").hash(5).hash(true).result();

        int hash2 = Builder.hash("a").hash(5).hash(true).result();

        assertEquals(hash1, hash2);

        int hash3 = Builder.hash("a").hash(5).hash(false).result();

        assertNotEquals(hash1, hash3);
    }

    @Test
    public void testHashCodeBuilderPrimitives() {
        HashCodeBuilder builder = Builder.hash(true).hash('a').hash((byte) 5).hash((short) 10).hash(100).hash(100L).hash(1.5f).hash(2.5);

        int result = builder.result();
        assertTrue(result != 0);
    }

    @Test
    public void testHashCodeBuilder_instance_hash_object() {
        int result = Builder.hash("hello").hash("world").result();
        assertNotEquals(0, result);
    }

    @Test
    public void testHashCodeBuilder_instance_hash_withFunction() {
        int result = Builder.hash("hello").hash("world", String::length).result();
        assertNotEquals(0, result);
    }

    @Test
    public void testHashCodeBuilder_instance_hash_boolean() {
        int result = Builder.hash(1).hash(true).result();
        int result2 = Builder.hash(1).hash(false).result();
        assertNotEquals(result, result2);
    }

    @Test
    public void testHashCodeBuilder_instance_hash_char() {
        int result = Builder.hash(1).hash('a').result();
        assertNotEquals(0, result);
    }

    @Test
    public void testHashCodeBuilder_instance_hash_byte() {
        int result = Builder.hash(1).hash((byte) 5).result();
        assertNotEquals(0, result);
    }

    @Test
    public void testHashCodeBuilder_instance_hash_short() {
        int result = Builder.hash(1).hash((short) 5).result();
        assertNotEquals(0, result);
    }

    @Test
    public void testHashCodeBuilder_instance_hash_int() {
        int result = Builder.hash(1).hash(42).result();
        assertNotEquals(0, result);
    }

    @Test
    public void testHashCodeBuilder_instance_hash_long() {
        int result = Builder.hash(1).hash(100L).result();
        assertNotEquals(0, result);
    }

    @Test
    public void testHashCodeBuilder_instance_hash_float() {
        int result = Builder.hash(1).hash(3.14f).result();
        assertNotEquals(0, result);
    }

    @Test
    public void testHashCodeBuilder_instance_hash_double() {
        int result = Builder.hash(1).hash(3.14).result();
        assertNotEquals(0, result);
    }

    @Test
    public void testHash_static_boolean() {
        int hashTrue = Builder.hash(true).result();
        int hashFalse = Builder.hash(false).result();
        assertNotEquals(hashTrue, hashFalse);
    }

    @Test
    public void testHash_static_char() {
        int result = Builder.hash('a').result();
        assertNotEquals(0, result);
    }

    @Test
    public void testHash_static_byte() {
        int result = Builder.hash((byte) 5).result();
        assertNotEquals(0, result);
    }

    @Test
    public void testHash_static_short() {
        int result = Builder.hash((short) 5).result();
        assertNotEquals(0, result);
    }

    @Test
    public void testHash_static_int() {
        int result = Builder.hash(42).result();
        assertNotEquals(0, result);
    }

    @Test
    public void testHash_static_long() {
        int result = Builder.hash(100L).result();
        assertNotEquals(0, result);
    }

    @Test
    public void testHash_static_float() {
        int result = Builder.hash(3.14f).result();
        assertNotEquals(0, result);
    }

    @Test
    public void testHash_static_double() {
        int result = Builder.hash(3.14).result();
        assertNotEquals(0, result);
    }

    @Test
    public void testHash_static_withFunction() {
        int result = Builder.hash("hello", String::length).result();
        assertEquals(5, result);
    }

    @Test
    public void testHashCodeBuilder_hash_withFunction_chain() {
        int hash = Builder.hash("abc").hash("xyz", String::length).result();
        int expected = 31 * "abc".hashCode() + 3;
        assertEquals(expected, hash);
    }

    @Test
    public void testHash_booleanFalse_value() {
        assertEquals(1237, Builder.hash(false).result());
    }

    @Test
    public void testHash_booleanTrue_value() {
        assertEquals(1231, Builder.hash(true).result());
    }

    @Test
    public void testHashCodeBuilder_chain_correctness() {
        int h1 = 0;
        h1 = h1 * 31 + CommonUtil.hashCode("a");
        h1 = h1 * 31 + CommonUtil.hashCode(42);
        h1 = h1 * 31 + (true ? 1231 : 1237);
        h1 = h1 * 31 + CommonUtil.hashCode('z');
        h1 = h1 * 31 + CommonUtil.hashCode((byte) 1);
        h1 = h1 * 31 + CommonUtil.hashCode((short) 2);
        h1 = h1 * 31 + CommonUtil.hashCode(100L);
        h1 = h1 * 31 + CommonUtil.hashCode(1.5f);
        h1 = h1 * 31 + CommonUtil.hashCode(2.5);

        int h2 = Builder.hash("a").hash(42).hash(true).hash('z').hash((byte) 1).hash((short) 2).hash(100L).hash(1.5f).hash(2.5).result();
        assertEquals(h1, h2);
    }

    @Test
    public void testHash_objectWithNull() {
        int hash = Builder.hash((Object) null).result();
        assertEquals(0, hash);
    }

    @Test
    public void testHashCodeBuilder_chaining() {
        int hash = Builder.hash("test").hash(1).hash(true).result();
        assertNotNull(hash);
    }

    @Test
    public void testHashCodeBuilder_multipleValues() {
        int hash1 = Builder.hash("a").hash(1).result();
        int hash2 = Builder.hash("b").hash(1).result();
        assertTrue(hash1 != hash2);
    }

    @Test
    public void testHashWithFunction() {
        ToIntFunction<String> lengthHash = String::length;
        int hash1 = Builder.hash("test", lengthHash).result();
        int hash2 = Builder.hash("same", lengthHash).result();
        Assertions.assertEquals(hash1, hash2);
    }

    @Test
    public void testHash_static_object() {
        int result = Builder.hash("hello").result();
        assertNotEquals(0, result);

        // null should produce 0
        int nullResult = Builder.hash((Object) null).result();
        assertEquals(0, nullResult);
    }

    @Test
    public void testHashCodeBuilder_hash_object_nonNull() {
        int hash = Builder.hash("hello").result();
        assertEquals("hello".hashCode(), hash);
    }

    // ---- G28-002: every value the class javadoc lists as hashing to 0, and the ones that do not ----

    @Test
    public void testHash_zeroHashValues_primitiveZerosAndNull() {
        assertEquals(0, Builder.hash((Object) null).result());
        assertEquals(0, Builder.hash(0).result());
        assertEquals(0, Builder.hash(0L).result());
        assertEquals(0, Builder.hash(0.0f).result());
        assertEquals(0, Builder.hash(0.0d).result());
        assertEquals(0, Builder.hash((byte) 0).result());
        assertEquals(0, Builder.hash((short) 0).result());
        assertEquals(0, Builder.hash((char) 0).result());

        // ... so a leading zero-hash value does not change the chain
        final int lone = Builder.hash(7).result();
        assertEquals(lone, Builder.hash(0.0d).hash(7).result());
        assertEquals(lone, Builder.hash(0.0f).hash(7).result());
        assertEquals(lone, Builder.hash((byte) 0).hash(7).result());
        assertEquals(lone, Builder.hash((short) 0).hash(7).result());
        assertEquals(lone, Builder.hash((char) 0).hash(7).result());
    }

    @Test
    public void testHash_zeroHashValues_anyObjectWhoseHashCodeIsZero() {
        // hash(Object) delegates to N.hashCode, so the invisible-leading-value property is not limited to the
        // primitive zeros: ANY object hashing to 0 behaves the same.
        final int lone = Builder.hash(7).result();
        for (final Object v : new Object[] { "", "\0", new HashSet<>(), new HashMap<>(), BigInteger.ZERO }) {
            assertEquals(0, N.hashCode(v));
            assertEquals(0, Builder.hash(v).result());
            assertEquals(lone, Builder.hash(v).hash(7).result());
        }
    }

    @Test
    public void testHash_valuesThatDoNotHashToZero() {
        assertEquals(1237, Builder.hash(false).result());
        assertEquals(Integer.MIN_VALUE, Builder.hash(-0.0f).result());
        assertEquals(Integer.MIN_VALUE, Builder.hash(-0.0d).result());
        assertNotEquals(Builder.hash(7).result(), Builder.hash(-0.0d).hash(7).result());

        // an empty List hashes to 1, not 0 - the contrast the class javadoc now draws
        assertEquals(1, N.hashCode(new ArrayList<>()));
        assertNotEquals(Builder.hash(7).result(), Builder.hash(new ArrayList<>()).hash(7).result());
    }

    // ---- G28-003: hash(value, func) hands the value to func unchanged, null included ----

    @Test
    public void testHash_withFunction_nullValueReachesFunction() {
        // Unlike hash(Object), no null-handling is applied: the NPE comes from the function itself.
        assertThrows(NullPointerException.class, () -> Builder.hash((String) null, String::length));
        assertThrows(NullPointerException.class, () -> Builder.hash(1).hash((String) null, String::length));

        // a null-tolerant function is called with the null and its result is used
        assertEquals(0, Builder.hash((int[]) null, java.util.Arrays::hashCode).result());
        assertEquals(-7, Builder.hash((String) null, s -> s == null ? -7 : s.length()).result());

        // contrast: the hash(Object) neighbour really is null-safe
        assertEquals(0, Builder.hash((Object) null).result());
    }
}
