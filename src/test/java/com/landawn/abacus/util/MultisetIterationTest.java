package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.ObjIntConsumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Multiset.Entry;
import com.landawn.abacus.util.stream.Stream;

public class MultisetIterationTest extends MultisetTestSupport {

    // elements

    @Test
    public void testElements_Empty() {
        Stream<String> stream = multiset.elements();
        assertEquals(0, stream.count());
    }

    @Test
    public void testElementsStream() {
        Multiset<String> multiset = Multiset.of("a", "b", "a", "c");
        List<String> streamed = multiset.elements().toList();

        assertEquals(4, streamed.size());
        assertEquals(2, Collections.frequency(streamed, "a"));
        assertEquals(1, Collections.frequency(streamed, "b"));
        assertEquals(1, Collections.frequency(streamed, "c"));
    }

    @Test
    public void testElementsStream_CollectToList() {
        multiset.add("a", 2);
        multiset.add("b", 1);
        List<String> list = multiset.elements().toList();
        assertEquals(3, list.size());
    }

    @Test
    public void testElementsStream_empty() {
        Multiset<String> multiset = new Multiset<>();
        assertEquals(0, multiset.elements().count());
    }

    // elementSet

    @Test
    public void testElementSet() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        multiset.add("c", 1);
        Set<String> elements = multiset.elementSet();
        assertEquals(3, elements.size());
        assertTrue(elements.contains("a"));
        assertTrue(elements.contains("b"));
        assertTrue(elements.contains("c"));
    }

    @Test
    public void testElementSet_Empty() {
        Set<String> elements = multiset.elementSet();
        assertTrue(elements.isEmpty());
    }

    @Test
    public void testElementSet_removeObject_supported() {
        Multiset<String> multiset = Multiset.of("a", "b", "a", "c");
        Set<String> elementSet = multiset.elementSet();

        assertTrue(elementSet.contains("a"));
        assertTrue(multiset.contains("a"));

        assertTrue(elementSet.remove("a"), "elementSet.remove('a') should return true");

        assertFalse(elementSet.contains("a"), "Element 'a' should be removed from elementSet");
        assertFalse(multiset.contains("a"), "Element 'a' should be removed from multiset");
        assertEquals(0, multiset.getCount("a"), "Count of 'a' should be 0 in multiset");

        assertEquals(2, multiset.size(), "Multiset size should be updated");
        assertEquals(2, elementSet.size(), "elementSet size should be updated");

        assertFalse(elementSet.remove("x"), "Removing non-existent element from elementSet should return false");
    }

    @Test
    public void testElementSetIterator_remove_supported() {
        Multiset<String> multiset = Multiset.of("a", "b", "a", "c");
        Set<String> elementSet = multiset.elementSet();
        Iterator<String> it = elementSet.iterator();

        assertTrue(it.hasNext());
        String firstElement = it.next();
        multiset.removeAllOccurrencesOf(firstElement);

        assertEquals(0, multiset.getCount(firstElement), "Element should be removed from multiset after elementSet().iterator().remove()");
        assertFalse(multiset.contains(firstElement), "Multiset should not contain element after elementSet().iterator().remove()");
        assertFalse(elementSet.contains(firstElement), "ElementSet should not contain element after its iterator.remove()");

        it = elementSet.iterator();

        assertTrue(it.hasNext());
        String secondElement = it.next();
        it.remove();
        assertEquals(0, multiset.getCount(secondElement));

        while (it.hasNext()) {
            it.next();
            it.remove();
        }
        assertTrue(multiset.isEmpty(), "Multiset should be empty after removing all from elementSet iterator");
        assertTrue(elementSet.isEmpty(), "ElementSet should be empty");
    }

    // entries

    @Test
    public void testEntries() {
        multiset.add("a", 3);
        multiset.add("b", 2);

        List<Multiset.Entry<String>> entries = multiset.entries().toList();
        assertEquals(2, entries.size());
    }

    @Test
    public void testEntries_Empty() {
        Stream<Multiset.Entry<String>> stream = multiset.entries();
        assertEquals(0, stream.count());
    }

    @Test
    public void testEntriesStream() {
        Multiset<String> multiset = Multiset.of("a", "b", "a", "c");
        Map<String, Integer> streamedEntries = multiset.entries().toMap(Multiset.Entry::element, Multiset.Entry::count);

        assertEquals(3, streamedEntries.size());
        assertEquals(2, streamedEntries.get("a").intValue());
        assertEquals(1, streamedEntries.get("b").intValue());
        assertEquals(1, streamedEntries.get("c").intValue());
    }

    @Test
    public void testEntriesStream_CollectToList() {
        multiset.add("a", 2);
        multiset.add("b", 1);
        List<Multiset.Entry<String>> entries = multiset.entries().toList();
        assertEquals(2, entries.size());
    }

    @Test
    public void testEntriesStream_empty() {
        Multiset<String> multiset = new Multiset<>();
        assertEquals(0, multiset.entries().count());
    }

    // Entry

    @Test
    public void testEntry_Count() {
        Multiset.Entry<String> entry = new Multiset.ImmutableEntry<>("test", 5);
        assertEquals(5, entry.count());
    }

    @Test
    public void testEntry_Element() {
        Multiset.Entry<String> entry = new Multiset.ImmutableEntry<>("test", 5);
        assertEquals("test", entry.element());
    }

    @Test
    public void testEntry_Equals_DifferentCount() {
        Multiset.Entry<String> entry1 = new Multiset.ImmutableEntry<>("test", 5);
        Multiset.Entry<String> entry2 = new Multiset.ImmutableEntry<>("test", 3);
        assertFalse(entry1.equals(entry2));
    }

    @Test
    public void testEntry_Equals_DifferentElement() {
        Multiset.Entry<String> entry1 = new Multiset.ImmutableEntry<>("test", 5);
        Multiset.Entry<String> entry2 = new Multiset.ImmutableEntry<>("other", 5);
        assertFalse(entry1.equals(entry2));
    }

    @Test
    public void testEntry_Equals_NotEntry() {
        Multiset.Entry<String> entry = new Multiset.ImmutableEntry<>("test", 5);
        assertFalse(entry.equals("not an entry"));
    }

    @Test
    public void testEntry_Equals_Null() {
        Multiset.Entry<String> entry = new Multiset.ImmutableEntry<>("test", 5);
        assertFalse(entry.equals(null));
    }

    @Test
    public void testEntry_Equals_NullElement() {
        Multiset.Entry<String> entry1 = new Multiset.ImmutableEntry<>(null, 5);
        Multiset.Entry<String> entry2 = new Multiset.ImmutableEntry<>(null, 5);
        assertTrue(entry1.equals(entry2));
    }

    @Test
    public void testEntry_Equals_True() {
        Multiset.Entry<String> entry1 = new Multiset.ImmutableEntry<>("test", 5);
        Multiset.Entry<String> entry2 = new Multiset.ImmutableEntry<>("test", 5);
        assertTrue(entry1.equals(entry2));
    }

    @Test
    public void testEntry_HashCode() {
        Multiset.Entry<String> entry = new Multiset.ImmutableEntry<>("test", 5);
        int expectedHashCode = "test".hashCode() ^ 5;
        assertEquals(expectedHashCode, entry.hashCode());
    }

    @Test
    public void testEntry_HashCode_NullElement() {
        Multiset.Entry<String> entry = new Multiset.ImmutableEntry<>(null, 5);
        int expectedHashCode = 0 ^ 5;
        assertEquals(expectedHashCode, entry.hashCode());
    }

    @Test
    public void testEntry_ToString_CountMultiple() {
        Multiset.Entry<String> entry = new Multiset.ImmutableEntry<>("test", 5);
        assertEquals("test x 5", entry.toString());
    }

    @Test
    public void testEntry_ToString_CountOne() {
        Multiset.Entry<String> entry = new Multiset.ImmutableEntry<>("test", 1);
        assertEquals("test", entry.toString());
    }

    @Test
    @DisplayName("Test Entry with count 1")
    public void testEntryCountOne() {
        multiset.add("single");

        Multiset.Entry<String> entry = multiset.entrySet().iterator().next();
        assertEquals("single", entry.toString());
    }

    @Test
    @DisplayName("Test Entry interface")
    public void testEntryInterface() {
        multiset.add("test", 5);

        Multiset.Entry<String> entry = multiset.entrySet().iterator().next();

        assertEquals("test", entry.element());
        assertEquals(5, entry.count());

        assertEquals("test x 5", entry.toString());

        Multiset.Entry<String> sameEntry = new Multiset.Entry<>() {
            @Override
            public String element() {
                return "test";
            }

            @Override
            public int count() {
                return 5;
            }

            @Override
            public boolean equals(Object o) {
                if (o instanceof Multiset.Entry<?> e) {
                    return count() == e.count() && Objects.equals(element(), e.element());
                }
                return false;
            }

            @Override
            public int hashCode() {
                return Objects.hashCode(element()) ^ count();
            }

            @Override
            public String toString() {
                return element() + " x " + count();
            }
        };

        assertEquals(entry, sameEntry);
        assertEquals(entry.hashCode(), sameEntry.hashCode());
    }

    // entrySet

    @Test
    public void testEntrySet() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        Set<Multiset.Entry<String>> entries = multiset.entrySet();
        assertEquals(2, entries.size());

        boolean foundA = false;
        boolean foundB = false;
        for (Multiset.Entry<String> entry : entries) {
            if (entry.element().equals("a") && entry.count() == 3) {
                foundA = true;
            }
            if (entry.element().equals("b") && entry.count() == 2) {
                foundB = true;
            }
        }
        assertTrue(foundA);
        assertTrue(foundB);
    }

    @Test
    public void testEntrySet_Contains() {
        multiset.add("a", 3);
        Set<Multiset.Entry<String>> entries = multiset.entrySet();

        Multiset.Entry<String> entry = new Multiset.ImmutableEntry<>("a", 3);
        assertTrue(entries.contains(entry));

        Multiset.Entry<String> wrongCount = new Multiset.ImmutableEntry<>("a", 2);
        assertFalse(entries.contains(wrongCount));
    }

    @Test
    public void testEntrySet_iterator_next_without_hasNext() {
        Multiset<String> multiset = Multiset.of("a");
        Iterator<Multiset.Entry<String>> it = multiset.entrySet().iterator();
        assertNotNull(it.next());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    public void testEntrySet_removeObject() {
        Multiset<String> multiset = Multiset.of("a", "b", "a");
        Set<Multiset.Entry<String>> entrySet = multiset.entrySet();

        Multiset.Entry<String> entryOfA = null;
        for (Multiset.Entry<String> entry : entrySet) {
            if (entry.element().equals("a")) {
                entryOfA = entry;
                break;
            }
        }
        assertNotNull(entryOfA);
        assertEquals("a", entryOfA.element());
        assertEquals(2, entryOfA.count());

    }

    @Test
    public void testEntrySet_Size() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        assertEquals(2, multiset.entrySet().size());
    }

    @Test
    @DisplayName("Test entrySet() contains")
    public void testEntrySetContains() {
        multiset.add("a", 3);

        Set<Multiset.Entry<String>> entries = multiset.entrySet();

        Multiset.Entry<String> testEntry = new Multiset.Entry<>() {
            @Override
            public String element() {
                return "a";
            }

            @Override
            public int count() {
                return 3;
            }

            @Override
            public boolean equals(Object o) {
                if (o instanceof Multiset.Entry<?> e) {
                    return count() == e.count() && Objects.equals(element(), e.element());
                }
                return false;
            }

            @Override
            public int hashCode() {
                return Objects.hashCode(element()) ^ count();
            }

            @Override
            public String toString() {
                return element() + " x " + count();
            }
        };

        assertTrue(entries.contains(testEntry));
    }

    @Test
    public void testEntrySetIterator() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        Set<Multiset.Entry<String>> entrySet = multiset.entrySet();
        int count = 0;
        for (Entry<String> entry : entrySet) {
            assertNotNull(entry.element());
            assertTrue(entry.count() > 0);
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    public void testEntrySetIterator_remove() {
        Multiset<String> multiset = Multiset.of("a", "b");
        Set<Multiset.Entry<String>> entrySet = multiset.entrySet();
        Iterator<Multiset.Entry<String>> it = entrySet.iterator();

        assertTrue(it.hasNext());
        it.next();

        assertThrows(UnsupportedOperationException.class, it::remove,
                "entrySet().iterator().remove() should throw UnsupportedOperationException if not implemented.");
    }

    // forEach

    @Test
    public void testForEach() {
        multiset.add("a", 2);
        multiset.add("b", 1);

        List<String> collected = new ArrayList<>();
        multiset.forEach((java.util.function.Consumer<String>) collected::add);

        assertEquals(3, collected.size());
    }

    @Test
    public void testForEach_Consumer() {
        Multiset<String> multiset = Multiset.of("a", "b", "a", "c");
        List<String> seen = new ArrayList<>();
        Consumer<String> consumer = seen::add;

        multiset.forEach(consumer);

        assertEquals(4, seen.size());
        assertEquals(2, Collections.frequency(seen, "a"));
        assertEquals(1, Collections.frequency(seen, "b"));
        assertEquals(1, Collections.frequency(seen, "c"));
    }

    @Test
    public void testForEach_Consumer_nullAction() {
        Multiset<String> multiset = new Multiset<>();
        // Iterable.forEach specifies NullPointerException.
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> multiset.forEach((Consumer<String>) null));
    }

    @Test
    public void testForEach_Null() {
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> multiset.forEach((java.util.function.Consumer<String>) null));
    }

    @Test
    public void testForEach_ObjIntConsumer() {
        multiset.add("a", 3);
        multiset.add("b", 2);

        Map<String, Integer> collected = new HashMap<>();
        multiset.forEach(collected::put);

        assertEquals(2, collected.size());
        assertEquals(Integer.valueOf(3), collected.get("a"));
        assertEquals(Integer.valueOf(2), collected.get("b"));
    }

    @Test
    public void testForEach_ObjIntConsumer_Null() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> multiset.forEach((com.landawn.abacus.util.function.ObjIntConsumer<String>) null));
    }

    @Test
    public void testForEach_ObjIntConsumer_nullAction() {
        Multiset<String> multiset = new Multiset<>();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> multiset.forEach((ObjIntConsumer<String>) null));
    }

    @Test
    @DisplayName("Test forEach(Consumer)")
    public void testForEachConsumer() {
        multiset.add("a", 3);
        multiset.add("b", 2);

        List<String> elements = new ArrayList<>();
        multiset.forEach(e -> elements.add(e));

        assertEquals(5, elements.size());
        assertEquals(3, Collections.frequency(elements, "a"));
        assertEquals(2, Collections.frequency(elements, "b"));
    }

    @Test
    public void testForEachObjInt() {
        multiset.add("a", 2);
        multiset.add("b", 3);

        Map<String, Integer> visited = new HashMap<>();
        ObjIntConsumer<String> visitor = visited::put;

        multiset.forEach(visitor);
        assertEquals(2, visited.size());
        assertEquals(Integer.valueOf(2), visited.get("a"));
        assertEquals(Integer.valueOf(3), visited.get("b"));

        assertThrows(IllegalArgumentException.class, () -> multiset.forEach((ObjIntConsumer<String>) null));
    }

    @Test
    @DisplayName("Test forEach(ObjIntConsumer)")
    public void testForEachObjIntConsumer() {
        multiset.add("a", 3);
        multiset.add("b", 2);

        Map<String, Integer> counts = new HashMap<>();
        multiset.forEach((element, count) -> counts.put(element, count));

        assertEquals(2, counts.size());
        assertEquals(3, counts.get("a").intValue());
        assertEquals(2, counts.get("b").intValue());
    }

    @Test
    public void testImmutableEntry() {
        Multiset.ImmutableEntry<String> entry1 = new Multiset.ImmutableEntry<>("test", 5);
        assertEquals("test", entry1.element());
        assertEquals(5, entry1.count());

        Multiset.ImmutableEntry<String> entry2 = new Multiset.ImmutableEntry<>("test", 5);
        assertEquals(entry1, entry2);
        assertEquals(entry1.hashCode(), entry2.hashCode());

        Multiset.ImmutableEntry<String> entry3 = new Multiset.ImmutableEntry<>("test", 3);
        assertNotEquals(entry1, entry3);

        Multiset.ImmutableEntry<String> entry4 = new Multiset.ImmutableEntry<>("other", 5);
        assertNotEquals(entry1, entry4);

        assertNotEquals(entry1, "not an entry");

        assertEquals("test x 5", entry1.toString());

        Multiset.ImmutableEntry<String> entryMultiple = new Multiset.ImmutableEntry<>("test", 3);
        assertEquals("test x 3", entryMultiple.toString());

        Multiset.ImmutableEntry<String> nullEntry = new Multiset.ImmutableEntry<>(null, 2);
        assertEquals("null x 2", nullEntry.toString());
        assertEquals(2, nullEntry.hashCode());
    }

    // iterator

    @Test
    public void testIterator() {
        multiset.add("a", 2);
        multiset.add("b", 1);

        List<String> collected = new ArrayList<>();
        for (String s : multiset) {
            collected.add(s);
        }

        assertEquals(3, collected.size());
        assertTrue(collected.contains("a"));
        assertTrue(collected.contains("b"));
    }

    @Test
    public void testIterator_Empty() {
        Iterator<String> iter = multiset.iterator();
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_empty() {
        Multiset<String> multiset = new Multiset<>();
        Iterator<String> it = multiset.iterator();
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    public void testIterator_multipleCallsToHasNext() {
        Multiset<String> multiset = Multiset.of("a", "b");
        Iterator<String> it = multiset.iterator();
        assertTrue(it.hasNext());
        assertTrue(it.hasNext());
        assertNotNull(it.next());
        assertTrue(it.hasNext());
        assertNotNull(it.next());
        assertFalse(it.hasNext());
        assertFalse(it.hasNext());
    }

    @Test
    public void testIterator_MultipleOccurrences() {
        multiset.add("a", 3);
        Iterator<String> iter = multiset.iterator();

        int count = 0;
        while (iter.hasNext()) {
            assertEquals("a", iter.next());
            count++;
        }
        assertEquals(3, count);
    }

    @Test
    public void testIterator_NoSuchElement() {
        Iterator<String> iter = multiset.iterator();
        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testIterator_singleElementMultipleTimes() {
        Multiset<String> multiset = Multiset.of("a", "a", "a");
        List<String> elements = new ArrayList<>();
        multiset.iterator().forEachRemaining(elements::add);
        assertEquals(Arrays.asList("a", "a", "a"), elements);
    }

    @Test
    @DisplayName("Test NoSuchElementException in iterator")
    public void testIteratorNoSuchElement() {
        ObjIterator<String> iter = multiset.iterator();
        assertThrows(NoSuchElementException.class, iter::next);
    }

    @Test
    public void testMultisetIterator_remove() {
        Multiset<String> multiset = Multiset.of("a", "b", "a");
        Iterator<String> it = multiset.iterator();

        assertTrue(it.hasNext());
        it.next();

        assertThrows(UnsupportedOperationException.class, it::remove, "Multiset.iterator().remove() should throw UnsupportedOperationException");
    }
}
