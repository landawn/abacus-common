package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class MultisetAddTest extends MultisetTestSupport {
    @Test
    public void testAdd_IncrementExisting() {
        multiset.add("apple", 2);
        int oldCount = multiset.add("apple", 3);
        assertEquals(2, oldCount);
        assertEquals(5, multiset.getCount("apple"));
    }

    @Test
    public void testAddElementWithOccurrences() {
        Multiset<String> multiset = new Multiset<>();
        assertEquals(0, multiset.add("a", 3));
        assertEquals(3, multiset.getCount("a"));
        assertEquals(3, multiset.size());

        assertEquals(3, multiset.add("a", 2));
        assertEquals(5, multiset.getCount("a"));
        assertEquals(5, multiset.size());

        assertEquals(0, multiset.add("b", 0));
        assertEquals(0, multiset.getCount("b"));
        assertEquals(5, multiset.size());

        assertEquals(0, multiset.add("c", 1));
        assertEquals(1, multiset.getCount("c"));
        assertEquals(6, multiset.size());
    }

    @Test
    public void testAdd() {
        assertTrue(multiset.add("apple"));
        assertEquals(1, multiset.getCount("apple"));
        assertEquals(1, multiset.size());

        assertTrue(multiset.add("apple"));
        assertEquals(2, multiset.getCount("apple"));
        assertEquals(2, multiset.size());
    }

    @Test
    public void testAdd_SingleOccurrence() {
        assertTrue(multiset.add("apple"));
        assertEquals(1, multiset.getCount("apple"));
    }

    @Test
    public void testAdd_MultipleOccurrences() {
        int oldCount = multiset.add("apple", 3);
        assertEquals(0, oldCount);
        assertEquals(3, multiset.getCount("apple"));
    }

    @Test
    public void testAdd_ZeroOccurrences() {
        int oldCount = multiset.add("apple", 0);
        assertEquals(0, oldCount);
        assertEquals(0, multiset.getCount("apple"));
    }

    @Test
    public void testAdd_Null() {
        assertTrue(multiset.add(null));
        assertEquals(1, multiset.getCount(null));
    }

    @Test
    public void testAddSingleElement() {
        Multiset<String> multiset = new Multiset<>();
        assertTrue(multiset.add("a"));
        assertEquals(1, multiset.getCount("a"));
        assertEquals(1, multiset.size());

        assertTrue(multiset.add("a"));
        assertEquals(2, multiset.getCount("a"));
        assertEquals(2, multiset.size());

        assertTrue(multiset.add("b"));
        assertEquals(1, multiset.getCount("b"));
        assertEquals(3, multiset.size());
    }

    @Test
    public void testAddSingleElement_nullNotPermittedByDefaultMap() {
        Multiset<String> multiset = new Multiset<>();
        assertTrue(multiset.add(null));
        assertEquals(1, multiset.getCount(null));
        assertTrue(multiset.add(null));
        assertEquals(2, multiset.getCount(null));
    }

    @Test
    @DisplayName("Test add() single element")
    public void testAddSingle() {
        assertTrue(multiset.add("apple"));
        assertEquals(1, multiset.getCount("apple"));

        assertTrue(multiset.add("apple"));
        assertEquals(2, multiset.getCount("apple"));
    }

    @Test
    public void testAdd_NegativeOccurrences() {
        assertThrows(IllegalArgumentException.class, () -> multiset.add("apple", -1));
    }

    @Test
    public void testAdd_Overflow() {
        multiset.add("apple", Integer.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> multiset.add("apple", 1));
    }

    @Test
    public void testAddSingleElement_maxOccurrences() {
        Multiset<String> multiset = new Multiset<>();
        multiset.setCount("a", Integer.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> multiset.add("a"));
    }

    @Test
    public void testAddElementWithOccurrences_negative() {
        Multiset<String> multiset = new Multiset<>();
        assertThrows(IllegalArgumentException.class, () -> multiset.add("a", -1));
    }

    @Test
    public void testAddElementWithOccurrences_overflow() {
        Multiset<String> multiset = new Multiset<>();
        multiset.add("a", Integer.MAX_VALUE - 1);
        assertThrows(IllegalArgumentException.class, () -> multiset.add("a", 2));

        assertEquals(Integer.MAX_VALUE - 1, multiset.getCount("a"));

        Multiset<String> multiset2 = new Multiset<>();

        multiset2.add("b", 10);
        multiset2.add("b", Integer.MAX_VALUE - 10);
        assertEquals(Integer.MAX_VALUE, multiset2.getCount("b"));

    }

    @Test
    public void testAddWithOccurrences() {
        assertEquals(0, multiset.add("apple", 3));
        assertEquals(3, multiset.getCount("apple"));

        assertEquals(3, multiset.add("apple", 2));
        assertEquals(5, multiset.getCount("apple"));

        assertEquals(0, multiset.add("banana", 0));
        assertEquals(0, multiset.getCount("banana"));

        assertThrows(IllegalArgumentException.class, () -> multiset.add("test", -1));

        multiset.add("overflow", Integer.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> multiset.add("overflow", 1));
    }

    @Test
    public void testAddAndGetCount_NewElement() {
        int newCount = multiset.addAndGetCount("apple", 3);
        assertEquals(3, newCount);
        assertEquals(3, multiset.getCount("apple"));
    }

    @Test
    public void testAddAndGetCount_ExistingElement() {
        multiset.add("apple", 2);
        int newCount = multiset.addAndGetCount("apple", 3);
        assertEquals(5, newCount);
        assertEquals(5, multiset.getCount("apple"));
    }

    @Test
    public void testAddAndGetCount() {
        Multiset<String> multiset = new Multiset<>();
        assertEquals(3, multiset.addAndGetCount("a", 3));
        assertEquals(3, multiset.getCount("a"));

        assertEquals(5, multiset.addAndGetCount("a", 2));
        assertEquals(5, multiset.getCount("a"));

        assertEquals(0, multiset.addAndGetCount("b", 0));
        assertEquals(0, multiset.getCount("b"));

        assertEquals(5, multiset.getCount("a"));
    }

    @Test
    public void testAddAndGetCount_Zero() {
        multiset.add("apple", 5);
        int newCount = multiset.addAndGetCount("apple", 0);
        assertEquals(5, newCount);
    }

    @Test
    public void testAddAndGetCount_ZeroOnNewElement() {
        int newCount = multiset.addAndGetCount("apple", 0);
        assertEquals(0, newCount);
        assertEquals(0, multiset.getCount("apple"));
        assertFalse(multiset.contains("apple"));
    }

    @Test
    public void testAddAndGetCount_MultipleAdds() {
        assertEquals(3, multiset.addAndGetCount("a", 3));
        assertEquals(5, multiset.addAndGetCount("a", 2));
        assertEquals(5, multiset.getCount("a"));
    }

    @Test
    public void testAddAndGetCount_Negative() {
        assertThrows(IllegalArgumentException.class, () -> multiset.addAndGetCount("apple", -1));
    }

    @Test
    public void testAddAndGetCount_Overflow() {
        multiset.add("apple", Integer.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> multiset.addAndGetCount("apple", 1));
    }

    @Test
    public void testAddAndGetCount_negative() {
        Multiset<String> multiset = new Multiset<>();
        assertThrows(IllegalArgumentException.class, () -> multiset.addAndGetCount("a", -1));
    }

    @Test
    public void testAddAndGetCount_overflow() {
        Multiset<String> multiset = new Multiset<>();
        multiset.setCount("a", Integer.MAX_VALUE - 1);
        assertThrows(IllegalArgumentException.class, () -> multiset.addAndGetCount("a", 2));
        assertEquals(Integer.MAX_VALUE - 1, multiset.getCount("a"));
    }

    @Test
    public void testAddAll() {
        List<String> list = Arrays.asList("a", "b", "a");
        assertTrue(multiset.addAll(list));
        assertEquals(2, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b"));
    }

    @Test
    public void testAddAll_WithOccurrences() {
        List<String> list = Arrays.asList("a", "b");
        assertTrue(multiset.addAll(list, 3));
        assertEquals(3, multiset.getCount("a"));
        assertEquals(3, multiset.getCount("b"));
    }

    @Test
    public void testAddAllCollection() {
        Multiset<String> multiset = new Multiset<>();
        multiset.add("a");
        Collection<String> toAdd = Arrays.asList("a", "b", "c", "b");

        assertTrue(multiset.addAll(toAdd));
        assertEquals(2, multiset.getCount("a"));
        assertEquals(2, multiset.getCount("b"));
        assertEquals(1, multiset.getCount("c"));
        assertEquals(1 + 4, multiset.size());
    }

    @Test
    public void testAddAllCollectionWithOccurrences() {
        Multiset<String> multiset = new Multiset<>();
        multiset.add("a", 1);
        Collection<String> toAdd = Arrays.asList("a", "b");

        assertTrue(multiset.addAll(toAdd, 2));
        assertEquals(1 + 2, multiset.getCount("a"));
        assertEquals(2, multiset.getCount("b"));
        assertEquals(1 + 2 + 2, multiset.size());
    }

    @Test
    public void testAddAll_Empty() {
        assertFalse(multiset.addAll(new ArrayList<>()));
    }

    @Test
    public void testAddAll_Null() {
        assertFalse(multiset.addAll(null));
    }

    @Test
    public void testAddAll_WithOccurrences_Zero() {
        List<String> list = Arrays.asList("a", "b");
        assertFalse(multiset.addAll(list, 0));
        assertEquals(0, multiset.getCount("a"));
    }

    @Test
    public void testAddAllCollection_empty() {
        Multiset<String> multiset = Multiset.of("a");
        assertFalse(multiset.addAll(Collections.emptyList()));
        assertEquals(1, multiset.getCount("a"));
    }

    @Test
    public void testAddAllCollection_null() {
        Multiset<String> multiset = Multiset.of("a");
        assertFalse(multiset.addAll(null));
        assertEquals(1, multiset.getCount("a"));
    }

    @Test
    public void testAddAllCollectionWithOccurrences_zero() {
        Multiset<String> multiset = Multiset.of("a", "b");
        Collection<String> toAdd = Arrays.asList("a", "c");
        assertFalse(multiset.addAll(toAdd, 0));
        assertEquals(1, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b"));
        assertEquals(0, multiset.getCount("c"));
    }

    @Test
    public void testAddAllCollectionWithOccurrences_emptyCollection() {
        Multiset<String> multiset = Multiset.of("a");
        assertFalse(multiset.addAll(Collections.emptyList(), 2));
        assertEquals(1, multiset.getCount("a"));
    }

    @Test
    public void testAddAllCollectionWithOccurrences_nullCollection() {
        Multiset<String> multiset = Multiset.of("a");
        assertFalse(multiset.addAll(null, 2));
        assertEquals(1, multiset.getCount("a"));
    }

    @Test
    @DisplayName("Test addAll() empty collection")
    public void testAddAllEmptyCollection() {
        assertFalse(multiset.addAll(Collections.emptyList()));
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testAddAll_WithOccurrences_Negative() {
        List<String> list = Arrays.asList("a", "b");
        assertThrows(IllegalArgumentException.class, () -> multiset.addAll(list, -1));
    }

    @Test
    public void testAddAllCollectionWithOccurrences_negative() {
        Multiset<String> multiset = new Multiset<>();
        Collection<String> toAdd = Arrays.asList("a", "b");
        assertThrows(IllegalArgumentException.class, () -> multiset.addAll(toAdd, -1));
    }

    @Test
    public void testAddAllWithOccurrences() {
        List<String> list = Arrays.asList("a", "b");
        assertTrue(multiset.addAll(list, 3));
        assertEquals(3, multiset.getCount("a"));
        assertEquals(3, multiset.getCount("b"));

        assertFalse(multiset.addAll(list, 0));
        assertFalse(multiset.addAll(Collections.emptyList(), 5));
        assertFalse(multiset.addAll(null, 5));

        assertThrows(IllegalArgumentException.class, () -> multiset.addAll(list, -1));
    }
}
