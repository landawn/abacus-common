package com.landawn.abacus.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class Indexed100Test extends TestBase {

    @Test
    public void testOfWithValueAndIntIndex() {
        // Test with various types
        Indexed<String> stringIndexed = Indexed.of("Hello", 5);
        Assertions.assertEquals("Hello", stringIndexed.value());
        Assertions.assertEquals(5, stringIndexed.index());

        Indexed<Integer> intIndexed = Indexed.of(42, 0);
        Assertions.assertEquals(42, intIndexed.value());
        Assertions.assertEquals(0, intIndexed.index());

        // Test with null value
        Indexed<String> nullIndexed = Indexed.of(null, 10);
        Assertions.assertNull(nullIndexed.value());
        Assertions.assertEquals(10, nullIndexed.index());
    }

    @Test
    public void testOfWithValueAndLongIndex() {
        // Test with long index
        Indexed<String> indexed = Indexed.of("World", 1000000000000L);
        Assertions.assertEquals("World", indexed.value());
        Assertions.assertEquals(1000000000000L, indexed.longIndex());

        // Test with max long value
        Indexed<Double> maxIndexed = Indexed.of(3.14, Long.MAX_VALUE);
        Assertions.assertEquals(3.14, maxIndexed.value());
        Assertions.assertEquals(Long.MAX_VALUE, maxIndexed.longIndex());
    }

    @Test
    public void testOfWithNegativeIndexThrowsException() {
        // Test negative int index
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Indexed.of("test", -1);
        });

        // Test negative long index
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Indexed.of("test", -1L);
        });
    }

    @Test
    public void testValue() {
        Indexed<String> indexed = Indexed.of("test value", 3);
        Assertions.assertEquals("test value", indexed.value());

        // Test with different types
        Indexed<Integer> intIndexed = Indexed.of(100, 5);
        Assertions.assertEquals(100, intIndexed.value());

        Indexed<Object> objIndexed = Indexed.of(new Object(), 7);
        Assertions.assertNotNull(objIndexed.value());
    }

    @Test
    public void testHashCode() {
        Indexed<String> indexed1 = Indexed.of("test", 5);
        Indexed<String> indexed2 = Indexed.of("test", 5);
        Indexed<String> indexed3 = Indexed.of("test", 6);
        Indexed<String> indexed4 = Indexed.of("different", 5);
        Indexed<String> indexed5 = Indexed.of(null, 5);

        // Same content should have same hash code
        Assertions.assertEquals(indexed1.hashCode(), indexed2.hashCode());

        // Different index should have different hash code
        Assertions.assertNotEquals(indexed1.hashCode(), indexed3.hashCode());

        // Different value should have different hash code
        Assertions.assertNotEquals(indexed1.hashCode(), indexed4.hashCode());

        // Test null value hash code
        int nullHash = indexed5.hashCode();
        Assertions.assertEquals((int) (5 * 31), nullHash);
    }

    @Test
    public void testEquals() {
        Indexed<String> indexed1 = Indexed.of("test", 5);
        Indexed<String> indexed2 = Indexed.of("test", 5);
        Indexed<String> indexed3 = Indexed.of("test", 6);
        Indexed<String> indexed4 = Indexed.of("different", 5);
        Indexed<Integer> indexed5 = Indexed.of(5, 5);
        Indexed<String> indexed6 = Indexed.of(null, 5);
        Indexed<String> indexed7 = Indexed.of(null, 5);

        // Test equals with same object
        Assertions.assertTrue(indexed1.equals(indexed1));

        // Test equals with equal content
        Assertions.assertTrue(indexed1.equals(indexed2));
        Assertions.assertTrue(indexed2.equals(indexed1));

        // Test not equals with different index
        Assertions.assertFalse(indexed1.equals(indexed3));

        // Test not equals with different value
        Assertions.assertFalse(indexed1.equals(indexed4));

        // Test not equals with different type
        Assertions.assertFalse(indexed1.equals(indexed5));

        // Test not equals with null
        Assertions.assertFalse(indexed1.equals(null));

        // Test not equals with different class
        Assertions.assertFalse(indexed1.equals("not an Indexed"));

        // Test equals with null values
        Assertions.assertTrue(indexed6.equals(indexed7));
        Assertions.assertFalse(indexed1.equals(indexed6));
    }

    @Test
    public void testToString() {
        Indexed<String> stringIndexed = Indexed.of("Hello", 5);
        Assertions.assertEquals("[5]=Hello", stringIndexed.toString());

        Indexed<Integer> intIndexed = Indexed.of(42, 10);
        Assertions.assertEquals("[10]=42", intIndexed.toString());

        Indexed<String> nullIndexed = Indexed.of(null, 3);
        String nullStr = nullIndexed.toString();
        Assertions.assertTrue(nullStr.startsWith("[3]="));

        // Test with long index
        Indexed<String> longIndexed = Indexed.of("test", 9999999999L);
        Assertions.assertEquals("[9999999999]=test", longIndexed.toString());
    }

    @Test
    public void testWithComplexObjects() {
        // Test with custom objects
        Object customObj = new Object() {
            @Override
            public String toString() {
                return "CustomObject";
            }
        };

        Indexed<Object> objIndexed = Indexed.of(customObj, 7);
        Assertions.assertEquals(customObj, objIndexed.value());
        Assertions.assertEquals(7, objIndexed.index());
        Assertions.assertEquals("[7]=CustomObject", objIndexed.toString());
    }
}