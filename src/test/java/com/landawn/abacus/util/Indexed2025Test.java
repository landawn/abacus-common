package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class Indexed2025Test extends TestBase {

    @Test
    public void test_of_int_basicCreation() {
        Indexed<String> indexed = Indexed.of("Hello", 0);
        assertNotNull(indexed);
        assertEquals("Hello", indexed.value());
        assertEquals(0, indexed.index());
    }

    @Test
    public void test_of_int_positiveIndex() {
        Indexed<String> indexed = Indexed.of("Test", 100);
        assertEquals("Test", indexed.value());
        assertEquals(100, indexed.index());
    }

    @Test
    public void test_of_int_zeroIndex() {
        Indexed<Integer> indexed = Indexed.of(42, 0);
        assertEquals(42, indexed.value());
        assertEquals(0, indexed.index());
    }

    @Test
    public void test_of_int_nullValue() {
        Indexed<String> indexed = Indexed.of(null, 5);
        assertEquals(null, indexed.value());
        assertEquals(5, indexed.index());
    }

    @Test
    public void test_of_int_negativeIndexThrows() {
        assertThrows(IllegalArgumentException.class, () -> Indexed.of("Test", -1));
        assertThrows(IllegalArgumentException.class, () -> Indexed.of("Test", -100));
    }

    @Test
    public void test_of_int_maxIntValue() {
        Indexed<String> indexed = Indexed.of("Max", Integer.MAX_VALUE);
        assertEquals("Max", indexed.value());
        assertEquals(Integer.MAX_VALUE, indexed.index());
    }

    @Test
    public void test_of_long_basicCreation() {
        Indexed<String> indexed = Indexed.of("Hello", 0L);
        assertNotNull(indexed);
        assertEquals("Hello", indexed.value());
        assertEquals(0L, indexed.index());
    }

    @Test
    public void test_of_long_largeIndex() {
        Indexed<String> indexed = Indexed.of("Test", 5000000000L);
        assertEquals("Test", indexed.value());
        assertEquals(5000000000L, indexed.longIndex());
    }

    @Test
    public void test_of_long_zeroIndex() {
        Indexed<Double> indexed = Indexed.of(3.14, 0L);
        assertEquals(3.14, indexed.value());
        assertEquals(0L, indexed.index());
    }

    @Test
    public void test_of_long_nullValue() {
        Indexed<Object> indexed = Indexed.of(null, 10L);
        assertEquals(null, indexed.value());
        assertEquals(10L, indexed.index());
    }

    @Test
    public void test_of_long_negativeIndexThrows() {
        assertThrows(IllegalArgumentException.class, () -> Indexed.of("Test", -1L));
        assertThrows(IllegalArgumentException.class, () -> Indexed.of("Test", -1000L));
    }

    @Test
    public void test_of_long_maxLongValue() {
        Indexed<String> indexed = Indexed.of("Max", Long.MAX_VALUE);
        assertEquals("Max", indexed.value());
        assertEquals(Long.MAX_VALUE, indexed.longIndex());
    }

    @Test
    public void test_value_string() {
        Indexed<String> indexed = Indexed.of("Hello World", 0);
        assertEquals("Hello World", indexed.value());
    }

    @Test
    public void test_value_integer() {
        Indexed<Integer> indexed = Indexed.of(42, 5);
        assertEquals(42, indexed.value());
    }

    @Test
    public void test_value_null() {
        Indexed<String> indexed = Indexed.of(null, 10);
        assertEquals(null, indexed.value());
    }

    @Test
    public void test_value_complexObject() {
        java.util.List<String> list = java.util.Arrays.asList("a", "b", "c");
        Indexed<java.util.List<String>> indexed = Indexed.of(list, 3);
        assertEquals(list, indexed.value());
    }

    @Test
    public void test_index_zero() {
        Indexed<String> indexed = Indexed.of("Test", 0);
        assertEquals(0, indexed.index());
    }

    @Test
    public void test_index_positive() {
        Indexed<String> indexed = Indexed.of("Test", 42);
        assertEquals(42, indexed.index());
    }

    @Test
    public void test_index_large() {
        Indexed<String> indexed = Indexed.of("Test", 999999999L);
        assertEquals(999999999L, indexed.index());
    }

    @Test
    public void test_hashCode_sameValuesProduceSameHash() {
        Indexed<String> indexed1 = Indexed.of("Hello", 5);
        Indexed<String> indexed2 = Indexed.of("Hello", 5);
        assertEquals(indexed1.hashCode(), indexed2.hashCode());
    }

    @Test
    public void test_hashCode_differentValuesDifferentHash() {
        Indexed<String> indexed1 = Indexed.of("Hello", 5);
        Indexed<String> indexed2 = Indexed.of("World", 5);
        assertNotEquals(indexed1.hashCode(), indexed2.hashCode());
    }

    @Test
    public void test_hashCode_differentIndexDifferentHash() {
        Indexed<String> indexed1 = Indexed.of("Hello", 5);
        Indexed<String> indexed2 = Indexed.of("Hello", 10);
        assertNotEquals(indexed1.hashCode(), indexed2.hashCode());
    }

    @Test
    public void test_hashCode_nullValue() {
        Indexed<String> indexed1 = Indexed.of(null, 5);
        Indexed<String> indexed2 = Indexed.of(null, 5);
        assertEquals(indexed1.hashCode(), indexed2.hashCode());
    }

    @Test
    public void test_hashCode_consistency() {
        Indexed<String> indexed = Indexed.of("Test", 10);
        int hash1 = indexed.hashCode();
        int hash2 = indexed.hashCode();
        assertEquals(hash1, hash2);
    }

    @Test
    public void test_equals_sameObject() {
        Indexed<String> indexed = Indexed.of("Hello", 5);
        assertTrue(indexed.equals(indexed));
    }

    @Test
    public void test_equals_identicalValues() {
        Indexed<String> indexed1 = Indexed.of("Hello", 5);
        Indexed<String> indexed2 = Indexed.of("Hello", 5);
        assertTrue(indexed1.equals(indexed2));
        assertTrue(indexed2.equals(indexed1));
    }

    @Test
    public void test_equals_differentValues() {
        Indexed<String> indexed1 = Indexed.of("Hello", 5);
        Indexed<String> indexed2 = Indexed.of("World", 5);
        assertFalse(indexed1.equals(indexed2));
    }

    @Test
    public void test_equals_differentIndex() {
        Indexed<String> indexed1 = Indexed.of("Hello", 5);
        Indexed<String> indexed2 = Indexed.of("Hello", 10);
        assertFalse(indexed1.equals(indexed2));
    }

    @Test
    public void test_equals_bothDifferent() {
        Indexed<String> indexed1 = Indexed.of("Hello", 5);
        Indexed<String> indexed2 = Indexed.of("World", 10);
        assertFalse(indexed1.equals(indexed2));
    }

    @Test
    public void test_equals_nullValues() {
        Indexed<String> indexed1 = Indexed.of(null, 5);
        Indexed<String> indexed2 = Indexed.of(null, 5);
        assertTrue(indexed1.equals(indexed2));
    }

    @Test
    public void test_equals_oneNullValue() {
        Indexed<String> indexed1 = Indexed.of(null, 5);
        Indexed<String> indexed2 = Indexed.of("Hello", 5);
        assertFalse(indexed1.equals(indexed2));
        assertFalse(indexed2.equals(indexed1));
    }

    @Test
    public void test_equals_nullObject() {
        Indexed<String> indexed = Indexed.of("Hello", 5);
        assertFalse(indexed.equals(null));
    }

    @Test
    public void test_equals_differentType() {
        Indexed<String> indexed = Indexed.of("Hello", 5);
        assertFalse(indexed.equals("Hello"));
        assertFalse(indexed.equals(5));
        assertFalse(indexed.equals(new Object()));
    }

    @Test
    public void test_equals_symmetry() {
        Indexed<String> indexed1 = Indexed.of("Test", 10);
        Indexed<String> indexed2 = Indexed.of("Test", 10);
        assertTrue(indexed1.equals(indexed2));
        assertTrue(indexed2.equals(indexed1));
    }

    @Test
    public void test_equals_transitivity() {
        Indexed<String> indexed1 = Indexed.of("Test", 10);
        Indexed<String> indexed2 = Indexed.of("Test", 10);
        Indexed<String> indexed3 = Indexed.of("Test", 10);
        assertTrue(indexed1.equals(indexed2));
        assertTrue(indexed2.equals(indexed3));
        assertTrue(indexed1.equals(indexed3));
    }

    @Test
    public void test_toString_basicFormat() {
        Indexed<String> indexed = Indexed.of("Hello", 5);
        String result = indexed.toString();
        assertEquals("[5]=Hello", result);
    }

    @Test
    public void test_toString_zeroIndex() {
        Indexed<String> indexed = Indexed.of("Test", 0);
        assertEquals("[0]=Test", indexed.toString());
    }

    @Test
    public void test_toString_largeIndex() {
        Indexed<String> indexed = Indexed.of("Test", 999999);
        assertEquals("[999999]=Test", indexed.toString());
    }

    @Test
    public void test_toString_nullValue() {
        Indexed<String> indexed = Indexed.of(null, 5);
        assertEquals("[5]=null", indexed.toString());
    }

    @Test
    public void test_toString_integerValue() {
        Indexed<Integer> indexed = Indexed.of(42, 10);
        assertEquals("[10]=42", indexed.toString());
    }

    @Test
    public void test_toString_longIndex() {
        Indexed<String> indexed = Indexed.of("Test", 5000000000L);
        assertEquals("[5000000000]=Test", indexed.toString());
    }

    @Test
    public void test_toString_complexValue() {
        java.util.List<String> list = java.util.Arrays.asList("a", "b");
        Indexed<java.util.List<String>> indexed = Indexed.of(list, 3);
        String result = indexed.toString();
        assertTrue(result.startsWith("[3]="));
        assertTrue(result.contains("a"));
        assertTrue(result.contains("b"));
    }

    @Test
    public void test_multipleTypesOfValues() {
        Indexed<String> stringIndexed = Indexed.of("Hello", 0);
        Indexed<Integer> intIndexed = Indexed.of(42, 1);
        Indexed<Double> doubleIndexed = Indexed.of(3.14, 2);
        Indexed<Boolean> boolIndexed = Indexed.of(true, 3);

        assertEquals("Hello", stringIndexed.value());
        assertEquals(42, intIndexed.value());
        assertEquals(3.14, doubleIndexed.value());
        assertEquals(true, boolIndexed.value());
    }

    @Test
    public void test_largeIndexWithIntOverload() {
        Indexed<String> indexed = Indexed.of("Test", Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, indexed.index());
    }

    @Test
    public void test_veryLargeIndexWithLongOverload() {
        long largeIndex = Integer.MAX_VALUE + 1000L;
        Indexed<String> indexed = Indexed.of("Test", largeIndex);
        assertEquals(largeIndex, indexed.longIndex());
    }

    @Test
    public void test_differentGenericTypes() {
        Indexed<String> stringIdx = Indexed.of("text", 0);
        Indexed<Number> numberIdx = Indexed.of(100, 0);

        assertFalse(stringIdx.equals(numberIdx));
    }
}
