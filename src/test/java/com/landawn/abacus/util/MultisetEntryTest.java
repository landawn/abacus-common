package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Multiset.Entry;

public class MultisetEntryTest extends MultisetTestSupport {
    @Test
    public void testEntry_Count() {
        Multiset.Entry<String> entry = new Multiset.ImmutableEntry<>("test", 5);
        assertEquals(5, entry.count());
    }

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

    @Test
    @DisplayName("Test Entry with count 1")
    public void testEntryCountOne() {
        multiset.add("single");

        Multiset.Entry<String> entry = multiset.entrySet().iterator().next();
        assertEquals("single", entry.toString());
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
    public void testEntrySet_iterator_next_without_hasNext() {
        Multiset<String> multiset = Multiset.of("a");
        Iterator<Multiset.Entry<String>> it = multiset.entrySet().iterator();
        assertNotNull(it.next());
        assertThrows(NoSuchElementException.class, it::next);
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
    public void testEntry_Equals_True() {
        Multiset.Entry<String> entry1 = new Multiset.ImmutableEntry<>("test", 5);
        Multiset.Entry<String> entry2 = new Multiset.ImmutableEntry<>("test", 5);
        assertTrue(entry1.equals(entry2));
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
    public void testEntry_ToString_CountOne() {
        Multiset.Entry<String> entry = new Multiset.ImmutableEntry<>("test", 1);
        assertEquals("test", entry.toString());
    }

    @Test
    public void testEntry_ToString_CountMultiple() {
        Multiset.Entry<String> entry = new Multiset.ImmutableEntry<>("test", 5);
        assertEquals("test x 5", entry.toString());
    }

    @Test
    public void testEntry_Element() {
        Multiset.Entry<String> entry = new Multiset.ImmutableEntry<>("test", 5);
        assertEquals("test", entry.element());
    }
}
