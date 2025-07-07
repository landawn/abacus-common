package com.landawn.abacus.util;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ImmutableEntry100Test extends TestBase {

    @Test
    public void testOf() {
        ImmutableEntry<String, Integer> entry = ImmutableEntry.of("key", 42);
        
        Assertions.assertEquals("key", entry.getKey());
        Assertions.assertEquals(42, entry.getValue());
    }

    @Test
    public void testOf_WithNullKey() {
        ImmutableEntry<String, Integer> entry = ImmutableEntry.of(null, 100);
        
        Assertions.assertNull(entry.getKey());
        Assertions.assertEquals(100, entry.getValue());
    }

    @Test
    public void testOf_WithNullValue() {
        ImmutableEntry<String, Integer> entry = ImmutableEntry.of("key", null);
        
        Assertions.assertEquals("key", entry.getKey());
        Assertions.assertNull(entry.getValue());
    }

    @Test
    public void testOf_WithBothNull() {
        ImmutableEntry<String, String> entry = ImmutableEntry.of(null, null);
        
        Assertions.assertNull(entry.getKey());
        Assertions.assertNull(entry.getValue());
    }

    @Test
    public void testCopyOf() {
        Map.Entry<String, Integer> mutableEntry = new AbstractMap.SimpleEntry<>("original", 123);
        ImmutableEntry<String, Integer> immutableCopy = ImmutableEntry.copyOf(mutableEntry);
        
        Assertions.assertEquals("original", immutableCopy.getKey());
        Assertions.assertEquals(123, immutableCopy.getValue());
        
        // Verify it's a copy - changes to original don't affect copy
        mutableEntry.setValue(456);
        Assertions.assertEquals(123, immutableCopy.getValue());
    }

    @Test
    public void testCopyOf_FromMapEntry() {
        Map<String, Integer> map = new HashMap<>();
        map.put("test", 789);
        
        Map.Entry<String, Integer> mapEntry = map.entrySet().iterator().next();
        ImmutableEntry<String, Integer> immutableCopy = ImmutableEntry.copyOf(mapEntry);
        
        Assertions.assertEquals("test", immutableCopy.getKey());
        Assertions.assertEquals(789, immutableCopy.getValue());
    }

    @Test
    public void testCopyOf_WithNullEntry() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            ImmutableEntry.copyOf(null);
        });
    }

    @Test
    public void testCopyOf_EntryWithNulls() {
        Map.Entry<String, String> entryWithNulls = new AbstractMap.SimpleEntry<>(null, null);
        ImmutableEntry<String, String> immutableCopy = ImmutableEntry.copyOf(entryWithNulls);
        
        Assertions.assertNull(immutableCopy.getKey());
        Assertions.assertNull(immutableCopy.getValue());
    }

    @Test
    public void testSetValue_ThrowsUnsupported() {
        ImmutableEntry<String, Integer> entry = ImmutableEntry.of("key", 42);
        
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            entry.setValue(100);
        });
    }

    @Test
    public void testEquals() {
        ImmutableEntry<String, Integer> entry1 = ImmutableEntry.of("key", 42);
        ImmutableEntry<String, Integer> entry2 = ImmutableEntry.of("key", 42);
        ImmutableEntry<String, Integer> entry3 = ImmutableEntry.of("key", 43);
        ImmutableEntry<String, Integer> entry4 = ImmutableEntry.of("different", 42);
        
        // Test equality
        Assertions.assertEquals(entry1, entry2);
        Assertions.assertNotEquals(entry1, entry3);
        Assertions.assertNotEquals(entry1, entry4);
        
        // Test with nulls
        ImmutableEntry<String, Integer> nullKey1 = ImmutableEntry.of(null, 42);
        ImmutableEntry<String, Integer> nullKey2 = ImmutableEntry.of(null, 42);
        ImmutableEntry<String, Integer> nullValue1 = ImmutableEntry.of("key", null);
        ImmutableEntry<String, Integer> nullValue2 = ImmutableEntry.of("key", null);
        
        Assertions.assertEquals(nullKey1, nullKey2);
        Assertions.assertEquals(nullValue1, nullValue2);
        
        // Test with other Map.Entry implementations
        Map.Entry<String, Integer> simpleEntry = new AbstractMap.SimpleEntry<>("key", 42);
        Assertions.assertEquals(entry1, simpleEntry);
        Assertions.assertEquals(simpleEntry, entry1);
    }

    @Test
    public void testHashCode() {
        ImmutableEntry<String, Integer> entry1 = ImmutableEntry.of("key", 42);
        ImmutableEntry<String, Integer> entry2 = ImmutableEntry.of("key", 42);
        
        Assertions.assertEquals(entry1.hashCode(), entry2.hashCode());
        
        // Test with nulls
        ImmutableEntry<String, Integer> nullEntry = ImmutableEntry.of(null, null);
        // Should not throw exception
        int hashCode = nullEntry.hashCode();
        Assertions.assertEquals(0, hashCode);
    }

    @Test
    public void testToString() {
        ImmutableEntry<String, Integer> entry = ImmutableEntry.of("key", 42);
        String str = entry.toString();
        
        Assertions.assertTrue(str.contains("key"));
        Assertions.assertTrue(str.contains("42"));
        
        // Test with nulls
        ImmutableEntry<String, String> nullEntry = ImmutableEntry.of(null, null);
        String nullStr = nullEntry.toString();
        Assertions.assertNotNull(nullStr);
    }
}