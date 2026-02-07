package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class IndexedKeyed2025Test extends TestBase {

    @Test
    public void test_of_withNonNullKeyAndValue() {
        IndexedKeyed<String, Integer> indexed = IndexedKeyed.of("key1", 100, 0);

        assertNotNull(indexed);
        assertEquals(0, indexed.index());
        assertEquals("key1", indexed.key());
        assertEquals(100, indexed.val());
    }

    @Test
    public void test_of_withNullKey() {
        IndexedKeyed<String, Integer> indexed = IndexedKeyed.of(null, 100, 1);

        assertNotNull(indexed);
        assertEquals(1, indexed.index());
        assertNull(indexed.key());
        assertEquals(100, indexed.val());
    }

    @Test
    public void test_of_withNullValue() {
        IndexedKeyed<String, Integer> indexed = IndexedKeyed.of("key1", null, 2);

        assertNotNull(indexed);
        assertEquals(2, indexed.index());
        assertEquals("key1", indexed.key());
        assertNull(indexed.val());
    }

    @Test
    public void test_of_withBothNull() {
        IndexedKeyed<String, Integer> indexed = IndexedKeyed.of(null, null, 3);

        assertNotNull(indexed);
        assertEquals(3, indexed.index());
        assertNull(indexed.key());
        assertNull(indexed.val());
    }

    @Test
    public void test_of_withNegativeIndex() {
        IndexedKeyed<String, Integer> indexed = IndexedKeyed.of("key", 100, -5);

        assertNotNull(indexed);
        assertEquals(-5, indexed.index());
        assertEquals("key", indexed.key());
        assertEquals(100, indexed.val());
    }

    @Test
    public void test_of_withZeroIndex() {
        IndexedKeyed<String, Integer> indexed = IndexedKeyed.of("key", 100, 0);

        assertNotNull(indexed);
        assertEquals(0, indexed.index());
    }

    @Test
    public void test_of_withLargePositiveIndex() {
        IndexedKeyed<String, Integer> indexed = IndexedKeyed.of("key", 100, Integer.MAX_VALUE);

        assertNotNull(indexed);
        assertEquals(Integer.MAX_VALUE, indexed.index());
    }

    @Test
    public void test_of_withDifferentTypes() {
        IndexedKeyed<Integer, String> indexed1 = IndexedKeyed.of(42, "The Answer", 10);
        assertEquals(10, indexed1.index());
        assertEquals(42, indexed1.key());
        assertEquals("The Answer", indexed1.val());

        IndexedKeyed<Long, Double> indexed2 = IndexedKeyed.of(100L, 3.14, 20);
        assertEquals(20, indexed2.index());
        assertEquals(100L, indexed2.key());
        assertEquals(3.14, indexed2.val());
    }

    @Test
    public void test_index_returnsCorrectIndex() {
        IndexedKeyed<String, Integer> indexed = IndexedKeyed.of("testKey", 50, 5);
        assertEquals(5, indexed.index());
    }

    @Test
    public void test_index_withNegativeIndex() {
        IndexedKeyed<String, Integer> indexed = IndexedKeyed.of("testKey", 50, -10);
        assertEquals(-10, indexed.index());
    }

    @Test
    public void test_index_consistency() {
        IndexedKeyed<String, Integer> indexed = IndexedKeyed.of("testKey", 50, 7);
        assertEquals(indexed.index(), indexed.index());
    }

    @Test
    public void test_key_returnsCorrectKey() {
        IndexedKeyed<String, Integer> indexed = IndexedKeyed.of("testKey", 50, 0);
        assertEquals("testKey", indexed.key());
    }

    @Test
    public void test_key_withNullKey() {
        IndexedKeyed<String, Integer> indexed = IndexedKeyed.of(null, 50, 0);
        assertNull(indexed.key());
    }

    @Test
    public void test_val_returnsCorrectValue() {
        IndexedKeyed<String, Integer> indexed = IndexedKeyed.of("testKey", 75, 0);
        assertEquals(75, indexed.val());
    }

    @Test
    public void test_val_withNullValue() {
        IndexedKeyed<String, Integer> indexed = IndexedKeyed.of("testKey", null, 0);
        assertNull(indexed.val());
    }

    @Test
    public void test_hashCode_sameIndexAndKeySameHash() {
        IndexedKeyed<String, Integer> indexed1 = IndexedKeyed.of("key", 100, 1);
        IndexedKeyed<String, Integer> indexed2 = IndexedKeyed.of("key", 200, 1);

        assertEquals(indexed1.hashCode(), indexed2.hashCode());
    }

    @Test
    public void test_hashCode_differentIndexesDifferentHash() {
        IndexedKeyed<String, Integer> indexed1 = IndexedKeyed.of("key", 100, 1);
        IndexedKeyed<String, Integer> indexed2 = IndexedKeyed.of("key", 100, 2);

        assertNotEquals(indexed1.hashCode(), indexed2.hashCode());
    }

    @Test
    public void test_hashCode_differentKeysDifferentHash() {
        IndexedKeyed<String, Integer> indexed1 = IndexedKeyed.of("key1", 100, 1);
        IndexedKeyed<String, Integer> indexed2 = IndexedKeyed.of("key2", 100, 1);

        assertNotEquals(indexed1.hashCode(), indexed2.hashCode());
    }

    @Test
    public void test_hashCode_withNullKey() {
        IndexedKeyed<String, Integer> indexed1 = IndexedKeyed.of(null, 100, 5);
        IndexedKeyed<String, Integer> indexed2 = IndexedKeyed.of(null, 200, 5);

        assertEquals(indexed1.hashCode(), indexed2.hashCode());
    }

    @Test
    public void test_hashCode_withNegativeIndex() {
        IndexedKeyed<String, Integer> indexed1 = IndexedKeyed.of("key", 100, -3);
        IndexedKeyed<String, Integer> indexed2 = IndexedKeyed.of("key", 200, -3);

        assertEquals(indexed1.hashCode(), indexed2.hashCode());
    }

    @Test
    public void test_hashCode_consistency() {
        IndexedKeyed<String, Integer> indexed = IndexedKeyed.of("consistentKey", 100, 10);
        int hash1 = indexed.hashCode();
        int hash2 = indexed.hashCode();

        assertEquals(hash1, hash2);
    }

    @Test
    public void test_equals_sameInstance() {
        IndexedKeyed<String, Integer> indexed = IndexedKeyed.of("key", 100, 1);
        assertTrue(indexed.equals(indexed));
    }

    @Test
    public void test_equals_sameIndexAndKeyDifferentValues() {
        IndexedKeyed<String, Integer> indexed1 = IndexedKeyed.of("key", 100, 1);
        IndexedKeyed<String, Integer> indexed2 = IndexedKeyed.of("key", 200, 1);

        assertTrue(indexed1.equals(indexed2));
        assertTrue(indexed2.equals(indexed1));
    }

    @Test
    public void test_equals_sameIndexDifferentKeys() {
        IndexedKeyed<String, Integer> indexed1 = IndexedKeyed.of("key1", 100, 1);
        IndexedKeyed<String, Integer> indexed2 = IndexedKeyed.of("key2", 100, 1);

        assertFalse(indexed1.equals(indexed2));
        assertFalse(indexed2.equals(indexed1));
    }

    @Test
    public void test_equals_differentIndexesSameKey() {
        IndexedKeyed<String, Integer> indexed1 = IndexedKeyed.of("key", 100, 1);
        IndexedKeyed<String, Integer> indexed2 = IndexedKeyed.of("key", 100, 2);

        assertFalse(indexed1.equals(indexed2));
        assertFalse(indexed2.equals(indexed1));
    }

    @Test
    public void test_equals_differentIndexesAndKeys() {
        IndexedKeyed<String, Integer> indexed1 = IndexedKeyed.of("key1", 100, 1);
        IndexedKeyed<String, Integer> indexed2 = IndexedKeyed.of("key2", 100, 2);

        assertFalse(indexed1.equals(indexed2));
    }

    @Test
    public void test_equals_withNull() {
        IndexedKeyed<String, Integer> indexed = IndexedKeyed.of("key", 100, 1);
        assertFalse(indexed.equals(null));
    }

    @Test
    public void test_equals_withDifferentClass() {
        IndexedKeyed<String, Integer> indexed = IndexedKeyed.of("key", 100, 1);
        String differentClass = "key";

        assertFalse(indexed.equals(differentClass));
    }

    @Test
    public void test_equals_withKeyedParentClass() {
        IndexedKeyed<String, Integer> indexed = IndexedKeyed.of("key", 100, 1);
        Keyed<String, Integer> keyed = Keyed.of("key", 100);

        assertFalse(indexed.equals(keyed));
    }

    @Test
    public void test_equals_withNullKeys() {
        IndexedKeyed<String, Integer> indexed1 = IndexedKeyed.of(null, 100, 1);
        IndexedKeyed<String, Integer> indexed2 = IndexedKeyed.of(null, 200, 1);

        assertTrue(indexed1.equals(indexed2));
    }

    @Test
    public void test_equals_withOneNullKey() {
        IndexedKeyed<String, Integer> indexed1 = IndexedKeyed.of(null, 100, 1);
        IndexedKeyed<String, Integer> indexed2 = IndexedKeyed.of("key", 100, 1);

        assertFalse(indexed1.equals(indexed2));
        assertFalse(indexed2.equals(indexed1));
    }

    @Test
    public void test_equals_withNegativeIndexes() {
        IndexedKeyed<String, Integer> indexed1 = IndexedKeyed.of("key", 100, -5);
        IndexedKeyed<String, Integer> indexed2 = IndexedKeyed.of("key", 200, -5);

        assertTrue(indexed1.equals(indexed2));
    }

    @Test
    public void test_equals_symmetry() {
        IndexedKeyed<String, Integer> indexed1 = IndexedKeyed.of("key", 100, 1);
        IndexedKeyed<String, Integer> indexed2 = IndexedKeyed.of("key", 200, 1);

        assertEquals(indexed1.equals(indexed2), indexed2.equals(indexed1));
    }

    @Test
    public void test_equals_transitivity() {
        IndexedKeyed<String, Integer> indexed1 = IndexedKeyed.of("key", 100, 1);
        IndexedKeyed<String, Integer> indexed2 = IndexedKeyed.of("key", 200, 1);
        IndexedKeyed<String, Integer> indexed3 = IndexedKeyed.of("key", 300, 1);

        assertTrue(indexed1.equals(indexed2));
        assertTrue(indexed2.equals(indexed3));
        assertTrue(indexed1.equals(indexed3));
    }

    @Test
    public void test_toString_withNonNullIndexKeyAndValue() {
        IndexedKeyed<String, Integer> indexed = IndexedKeyed.of("abc", 123, 2);
        assertEquals("{index=2, key=abc, val=123}", indexed.toString());
    }

    @Test
    public void test_toString_withNullKey() {
        IndexedKeyed<String, Integer> indexed = IndexedKeyed.of(null, 100, 5);
        assertEquals("{index=5, key=null, val=100}", indexed.toString());
    }

    @Test
    public void test_toString_withNullValue() {
        IndexedKeyed<String, Integer> indexed = IndexedKeyed.of("key", null, 0);
        assertEquals("{index=0, key=key, val=null}", indexed.toString());
    }

    @Test
    public void test_toString_withBothNull() {
        IndexedKeyed<String, Integer> indexed = IndexedKeyed.of(null, null, 3);
        assertEquals("{index=3, key=null, val=null}", indexed.toString());
    }

    @Test
    public void test_toString_withNegativeIndex() {
        IndexedKeyed<String, Integer> indexed = IndexedKeyed.of("key", 100, -10);
        assertEquals("{index=-10, key=key, val=100}", indexed.toString());
    }

    @Test
    public void test_toString_withDifferentTypes() {
        IndexedKeyed<Integer, String> indexed = IndexedKeyed.of(42, "answer", 1);
        assertEquals("{index=1, key=42, val=answer}", indexed.toString());
    }

    @Test
    public void test_toString_withZeroIndex() {
        IndexedKeyed<String, Integer> indexed = IndexedKeyed.of("key", 100, 0);
        assertEquals("{index=0, key=key, val=100}", indexed.toString());
    }

    @Test
    public void test_hashCodeEqualsContract() {
        IndexedKeyed<String, Integer> indexed1 = IndexedKeyed.of("key", 100, 1);
        IndexedKeyed<String, Integer> indexed2 = IndexedKeyed.of("key", 200, 1);

        if (indexed1.equals(indexed2)) {
            assertEquals(indexed1.hashCode(), indexed2.hashCode());
        }
    }

    @Test
    public void test_withComplexObjectAsValue() {
        Pair<String, Integer> complexValue = Pair.of("left", 10);
        IndexedKeyed<String, Pair<String, Integer>> indexed = IndexedKeyed.of("complexKey", complexValue, 5);

        assertEquals(5, indexed.index());
        assertEquals("complexKey", indexed.key());
        assertEquals(complexValue, indexed.val());
        assertEquals("left", indexed.val().left());
        assertEquals(10, indexed.val().right());
    }

    @Test
    public void test_withComplexObjectAsKey() {
        Pair<String, Integer> complexKey = Pair.of("keyLeft", 5);
        IndexedKeyed<Pair<String, Integer>, String> indexed = IndexedKeyed.of(complexKey, "value", 10);

        assertEquals(10, indexed.index());
        assertEquals(complexKey, indexed.key());
        assertEquals("value", indexed.val());
        assertEquals("keyLeft", indexed.key().left());
        assertEquals(5, indexed.key().right());
    }

    @Test
    public void test_immutability() {
        int originalIndex = 7;
        String originalKey = "immutableKey";
        Integer originalValue = 500;

        IndexedKeyed<String, Integer> indexed = IndexedKeyed.of(originalKey, originalValue, originalIndex);

        assertEquals(originalIndex, indexed.index());
        assertEquals(originalKey, indexed.key());
        assertEquals(originalValue, indexed.val());

        assertTrue(indexed.key() == originalKey);
        assertTrue(indexed.val() == originalValue);
    }

    @Test
    public void test_hashCode_consistencyAcrossInstances() {
        IndexedKeyed<String, Integer> indexed1 = IndexedKeyed.of("key", 100, 1);
        IndexedKeyed<String, Integer> indexed2 = IndexedKeyed.of("key", 200, 1);
        IndexedKeyed<String, Integer> indexed3 = IndexedKeyed.of("key", 300, 1);

        assertEquals(indexed1.hashCode(), indexed2.hashCode());
        assertEquals(indexed2.hashCode(), indexed3.hashCode());
        assertEquals(indexed1.hashCode(), indexed3.hashCode());
    }

    @Test
    public void test_equals_withMaxIntegerIndex() {
        IndexedKeyed<String, Integer> indexed1 = IndexedKeyed.of("key", 100, Integer.MAX_VALUE);
        IndexedKeyed<String, Integer> indexed2 = IndexedKeyed.of("key", 200, Integer.MAX_VALUE);

        assertTrue(indexed1.equals(indexed2));
    }

    @Test
    public void test_equals_withMinIntegerIndex() {
        IndexedKeyed<String, Integer> indexed1 = IndexedKeyed.of("key", 100, Integer.MIN_VALUE);
        IndexedKeyed<String, Integer> indexed2 = IndexedKeyed.of("key", 200, Integer.MIN_VALUE);

        assertTrue(indexed1.equals(indexed2));
    }

    @Test
    public void test_hashCode_withExtremeIndexValues() {
        IndexedKeyed<String, Integer> indexedMax = IndexedKeyed.of("key", 100, Integer.MAX_VALUE);
        IndexedKeyed<String, Integer> indexedMin = IndexedKeyed.of("key", 100, Integer.MIN_VALUE);

        assertNotEquals(indexedMax.hashCode(), indexedMin.hashCode());
    }

    @Test
    public void test_notEqualToKeyedType() {
        IndexedKeyed<String, Integer> indexed = IndexedKeyed.of("key", 100, 0);
        Keyed<String, Integer> keyed = Keyed.of("key", 100);

        assertFalse(indexed.equals(keyed));
        assertFalse(keyed.equals(indexed));
    }

    @Test
    public void test_toString_consistency() {
        IndexedKeyed<String, Integer> indexed = IndexedKeyed.of("key", 100, 5);
        String str1 = indexed.toString();
        String str2 = indexed.toString();

        assertEquals(str1, str2);
    }

    @Test
    public void test_index_boundaryValues() {
        IndexedKeyed<String, Integer> indexedMax = IndexedKeyed.of("key", 100, Integer.MAX_VALUE);
        IndexedKeyed<String, Integer> indexedMin = IndexedKeyed.of("key", 100, Integer.MIN_VALUE);
        IndexedKeyed<String, Integer> indexedZero = IndexedKeyed.of("key", 100, 0);

        assertEquals(Integer.MAX_VALUE, indexedMax.index());
        assertEquals(Integer.MIN_VALUE, indexedMin.index());
        assertEquals(0, indexedZero.index());
    }
}
