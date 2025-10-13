package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class Keyed2025Test extends TestBase {

    @Test
    public void test_of_withNonNullKeyAndValue() {
        Keyed<String, Integer> keyed = Keyed.of("key1", 100);

        assertNotNull(keyed);
        assertEquals("key1", keyed.key());
        assertEquals(100, keyed.val());
    }

    @Test
    public void test_of_withNullKey() {
        Keyed<String, Integer> keyed = Keyed.of(null, 100);

        assertNotNull(keyed);
        assertNull(keyed.key());
        assertEquals(100, keyed.val());
    }

    @Test
    public void test_of_withNullValue() {
        Keyed<String, Integer> keyed = Keyed.of("key1", null);

        assertNotNull(keyed);
        assertEquals("key1", keyed.key());
        assertNull(keyed.val());
    }

    @Test
    public void test_of_withBothNull() {
        Keyed<String, Integer> keyed = Keyed.of(null, null);

        assertNotNull(keyed);
        assertNull(keyed.key());
        assertNull(keyed.val());
    }

    @Test
    public void test_of_withDifferentTypes() {
        Keyed<Integer, String> keyed1 = Keyed.of(42, "The Answer");
        assertEquals(42, keyed1.key());
        assertEquals("The Answer", keyed1.val());

        Keyed<Long, Double> keyed2 = Keyed.of(100L, 3.14);
        assertEquals(100L, keyed2.key());
        assertEquals(3.14, keyed2.val());
    }

    @Test
    public void test_key_returnsCorrectKey() {
        Keyed<String, Integer> keyed = Keyed.of("testKey", 50);
        assertEquals("testKey", keyed.key());
    }

    @Test
    public void test_key_withNullKey() {
        Keyed<String, Integer> keyed = Keyed.of(null, 50);
        assertNull(keyed.key());
    }

    @Test
    public void test_val_returnsCorrectValue() {
        Keyed<String, Integer> keyed = Keyed.of("testKey", 75);
        assertEquals(75, keyed.val());
    }

    @Test
    public void test_val_withNullValue() {
        Keyed<String, Integer> keyed = Keyed.of("testKey", null);
        assertNull(keyed.val());
    }

    @Test
    public void test_hashCode_sameKeysSameHash() {
        Keyed<String, Integer> keyed1 = Keyed.of("key", 100);
        Keyed<String, Integer> keyed2 = Keyed.of("key", 200);

        assertEquals(keyed1.hashCode(), keyed2.hashCode());
    }

    @Test
    public void test_hashCode_differentKeysDifferentHash() {
        Keyed<String, Integer> keyed1 = Keyed.of("key1", 100);
        Keyed<String, Integer> keyed2 = Keyed.of("key2", 100);

        assertNotEquals(keyed1.hashCode(), keyed2.hashCode());
    }

    @Test
    public void test_hashCode_withNullKey() {
        Keyed<String, Integer> keyed = Keyed.of(null, 100);
        assertEquals(0, keyed.hashCode());
    }

    @Test
    public void test_hashCode_consistency() {
        Keyed<String, Integer> keyed = Keyed.of("consistentKey", 100);
        int hash1 = keyed.hashCode();
        int hash2 = keyed.hashCode();

        assertEquals(hash1, hash2);
    }

    @Test
    public void test_equals_sameInstance() {
        Keyed<String, Integer> keyed = Keyed.of("key", 100);
        assertTrue(keyed.equals(keyed));
    }

    @Test
    public void test_equals_sameKeyDifferentValues() {
        Keyed<String, Integer> keyed1 = Keyed.of("key", 100);
        Keyed<String, Integer> keyed2 = Keyed.of("key", 200);

        assertTrue(keyed1.equals(keyed2));
        assertTrue(keyed2.equals(keyed1));
    }

    @Test
    public void test_equals_differentKeysSameValues() {
        Keyed<String, Integer> keyed1 = Keyed.of("key1", 100);
        Keyed<String, Integer> keyed2 = Keyed.of("key2", 100);

        assertFalse(keyed1.equals(keyed2));
        assertFalse(keyed2.equals(keyed1));
    }

    @Test
    public void test_equals_differentKeysDifferentValues() {
        Keyed<String, Integer> keyed1 = Keyed.of("key1", 100);
        Keyed<String, Integer> keyed2 = Keyed.of("key2", 200);

        assertFalse(keyed1.equals(keyed2));
    }

    @Test
    public void test_equals_withNull() {
        Keyed<String, Integer> keyed = Keyed.of("key", 100);
        assertFalse(keyed.equals(null));
    }

    @Test
    public void test_equals_withDifferentClass() {
        Keyed<String, Integer> keyed = Keyed.of("key", 100);
        String differentClass = "key";

        assertFalse(keyed.equals(differentClass));
    }

    @Test
    public void test_equals_withNullKeys() {
        Keyed<String, Integer> keyed1 = Keyed.of(null, 100);
        Keyed<String, Integer> keyed2 = Keyed.of(null, 200);

        assertTrue(keyed1.equals(keyed2));
    }

    @Test
    public void test_equals_withOneNullKey() {
        Keyed<String, Integer> keyed1 = Keyed.of(null, 100);
        Keyed<String, Integer> keyed2 = Keyed.of("key", 100);

        assertFalse(keyed1.equals(keyed2));
        assertFalse(keyed2.equals(keyed1));
    }

    @Test
    public void test_equals_symmetry() {
        Keyed<String, Integer> keyed1 = Keyed.of("key", 100);
        Keyed<String, Integer> keyed2 = Keyed.of("key", 200);

        assertEquals(keyed1.equals(keyed2), keyed2.equals(keyed1));
    }

    @Test
    public void test_equals_transitivity() {
        Keyed<String, Integer> keyed1 = Keyed.of("key", 100);
        Keyed<String, Integer> keyed2 = Keyed.of("key", 200);
        Keyed<String, Integer> keyed3 = Keyed.of("key", 300);

        assertTrue(keyed1.equals(keyed2));
        assertTrue(keyed2.equals(keyed3));
        assertTrue(keyed1.equals(keyed3));
    }

    @Test
    public void test_toString_withNonNullKeyAndValue() {
        Keyed<String, Integer> keyed = Keyed.of("abc", 123);
        assertEquals("{key=abc, val=123}", keyed.toString());
    }

    @Test
    public void test_toString_withNullKey() {
        Keyed<String, Integer> keyed = Keyed.of(null, 100);
        assertEquals("{key=null, val=100}", keyed.toString());
    }

    @Test
    public void test_toString_withNullValue() {
        Keyed<String, Integer> keyed = Keyed.of("key", null);
        assertEquals("{key=key, val=null}", keyed.toString());
    }

    @Test
    public void test_toString_withBothNull() {
        Keyed<String, Integer> keyed = Keyed.of(null, null);
        assertEquals("{key=null, val=null}", keyed.toString());
    }

    @Test
    public void test_toString_withDifferentTypes() {
        Keyed<Integer, String> keyed = Keyed.of(42, "answer");
        assertEquals("{key=42, val=answer}", keyed.toString());
    }

    @Test
    public void test_hashCodeEqualsContract() {
        Keyed<String, Integer> keyed1 = Keyed.of("key", 100);
        Keyed<String, Integer> keyed2 = Keyed.of("key", 200);

        if (keyed1.equals(keyed2)) {
            assertEquals(keyed1.hashCode(), keyed2.hashCode());
        }
    }

    @Test
    public void test_withComplexObjectAsValue() {
        Pair<String, Integer> complexValue = Pair.of("left", 10);
        Keyed<String, Pair<String, Integer>> keyed = Keyed.of("complexKey", complexValue);

        assertEquals("complexKey", keyed.key());
        assertEquals(complexValue, keyed.val());
        assertEquals("left", keyed.val().left());
        assertEquals(10, keyed.val().right());
    }

    @Test
    public void test_withComplexObjectAsKey() {
        Pair<String, Integer> complexKey = Pair.of("keyLeft", 5);
        Keyed<Pair<String, Integer>, String> keyed = Keyed.of(complexKey, "value");

        assertEquals(complexKey, keyed.key());
        assertEquals("value", keyed.val());
        assertEquals("keyLeft", keyed.key().left());
        assertEquals(5, keyed.key().right());
    }

    @Test
    public void test_immutability() {
        String originalKey = "immutableKey";
        Integer originalValue = 500;

        Keyed<String, Integer> keyed = Keyed.of(originalKey, originalValue);

        assertEquals(originalKey, keyed.key());
        assertEquals(originalValue, keyed.val());

        assertTrue(keyed.key() == originalKey);
        assertTrue(keyed.val() == originalValue);
    }
}
