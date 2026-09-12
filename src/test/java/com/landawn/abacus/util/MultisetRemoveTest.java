package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.function.ObjIntPredicate;

public class MultisetRemoveTest extends MultisetTestSupport {
    @Test
    public void testRemoveOccurrencesWithLiveElementSet() {
        final Multiset<String> values = new Multiset<>(LinkedHashMap.class);
        values.add("a");
        values.add("b", 2);
        values.add("c");

        assertTrue(values.removeOccurrences(Collections.unmodifiableSet(values.elementSet()), 1));
        assertEquals(0, values.getCount("a"));
        assertEquals(1, values.getCount("b"));
        assertEquals(0, values.getCount("c"));
        assertEquals(1, values.size());
    }

    @Test
    public void testRemoveOccurrencesPreservesInputMultiplicity() {
        final Multiset<String> values = Multiset.of("a", "a", "a", "a", "a", "b", "b", "b");
        final Multiset<String> removals = Multiset.of("a", "a", "b");

        assertTrue(values.removeOccurrences(removals, 2));
        assertEquals(1, values.getCount("a"));
        assertEquals(1, values.getCount("b"));
        assertEquals(2, removals.getCount("a"));
        assertEquals(1, removals.getCount("b"));
    }

    @Test
    public void testRemoveOccurrencesUsesBoundedArithmeticForLargeInputCounts() {
        final Object element = new Object() {
            private int lookups;

            @Override
            public int hashCode() {
                // Fail promptly if removal regresses to a lookup for every occurrence.
                if (++lookups > 32) {
                    throw new AssertionError("Removal must use counts without expanding billions of occurrences");
                }
                return 1;
            }
        };
        final Multiset<Object> values = new Multiset<>();
        values.add(element, Integer.MAX_VALUE);
        final Multiset<Object> removals = new Multiset<>();
        removals.add(element, Integer.MAX_VALUE);

        assertFalse(values.removeOccurrences(removals, 0));
        assertEquals(Integer.MAX_VALUE, values.getCount(element));
        assertTrue(values.removeOccurrences(removals, Integer.MAX_VALUE));
        assertTrue(values.isEmpty());
        assertEquals(Integer.MAX_VALUE, removals.getCount(element));
        assertFalse(values.removeOccurrences(removals, 1));
    }

    @Test
    public void testRemove_LastOccurrence() {
        multiset.add("apple");
        assertTrue(multiset.remove("apple"));
        assertEquals(0, multiset.getCount("apple"));
        assertFalse(multiset.contains("apple"));
    }

    @Test
    public void testRemove_MoreThanExists() {
        multiset.add("apple", 3);
        int oldCount = multiset.remove("apple", 5);
        assertEquals(3, oldCount);
        assertEquals(0, multiset.getCount("apple"));
        assertFalse(multiset.contains("apple"));
    }

    @Test
    public void testRemoveElementWithOccurrences() {
        Multiset<String> multiset = Multiset.of("a", "a", "a", "a", "b", "b");
        assertEquals(4, multiset.remove("a", 2));
        assertEquals(2, multiset.getCount("a"));
        assertEquals(4, multiset.size());

        assertEquals(2, multiset.remove("a", 3));
        assertEquals(0, multiset.getCount("a"));
        assertEquals(2, multiset.size());

        assertEquals(0, multiset.remove("c", 1));
        assertEquals(0, multiset.getCount("c"));
        assertEquals(2, multiset.size());

        assertEquals(2, multiset.remove("b", 0));
        assertEquals(2, multiset.getCount("b"));
        assertEquals(2, multiset.size());
    }

    @Test
    public void testRemove() {
        multiset.add("apple", 3);

        assertTrue(multiset.remove("apple"));
        assertEquals(2, multiset.getCount("apple"));

        assertTrue(multiset.remove("apple"));
        assertEquals(1, multiset.getCount("apple"));

        assertTrue(multiset.remove("apple"));
        assertEquals(0, multiset.getCount("apple"));
        assertFalse(multiset.contains("apple"));

        assertFalse(multiset.remove("banana"));
    }

    @Test
    public void testRemove_SingleOccurrence() {
        multiset.add("apple", 3);
        assertTrue(multiset.remove("apple"));
        assertEquals(2, multiset.getCount("apple"));
    }

    @Test
    public void testRemove_NonExistent() {
        assertFalse(multiset.remove("nonexistent"));
    }

    @Test
    public void testRemove_MultipleOccurrences() {
        multiset.add("apple", 5);
        int oldCount = multiset.remove("apple", 2);
        assertEquals(5, oldCount);
        assertEquals(3, multiset.getCount("apple"));
    }

    @Test
    public void testRemove_Zero() {
        multiset.add("apple", 3);
        int oldCount = multiset.remove("apple", 0);
        assertEquals(3, oldCount);
        assertEquals(3, multiset.getCount("apple"));
    }

    @Test
    public void testRemoveSingleElement() {
        Multiset<String> multiset = Multiset.of("a", "a", "b");
        assertTrue(multiset.remove("a"));
        assertEquals(1, multiset.getCount("a"));
        assertEquals(2, multiset.size());

        assertTrue(multiset.remove("a"));
        assertEquals(0, multiset.getCount("a"));
        assertEquals(1, multiset.size());

        assertFalse(multiset.remove("a"));
        assertEquals(0, multiset.getCount("a"));
        assertEquals(1, multiset.size());

        assertTrue(multiset.remove("b"));
        assertEquals(0, multiset.getCount("b"));
        assertTrue(multiset.isEmpty());

        assertFalse(multiset.remove("c"));
    }

    @Test
    @DisplayName("Test remove() single occurrence")
    public void testRemoveSingle() {
        multiset.add("apple", 3);

        assertTrue(multiset.remove("apple"));
        assertEquals(2, multiset.getCount("apple"));

        assertFalse(multiset.remove("banana"));
    }

    @Test
    public void testRemove_Negative() {
        assertThrows(IllegalArgumentException.class, () -> multiset.remove("apple", -1));
    }

    @Test
    public void testRemoveElementWithOccurrences_negative() {
        Multiset<String> multiset = new Multiset<>();
        assertThrows(IllegalArgumentException.class, () -> multiset.remove("a", -1));
    }

    @Test
    public void testRemoveWithOccurrences() {
        multiset.add("apple", 5);

        assertEquals(5, multiset.remove("apple", 2));
        assertEquals(3, multiset.getCount("apple"));

        assertEquals(3, multiset.remove("apple", 10));
        assertEquals(0, multiset.getCount("apple"));
        assertFalse(multiset.contains("apple"));

        assertEquals(0, multiset.remove("banana", 5));

        assertThrows(IllegalArgumentException.class, () -> multiset.remove("test", -1));
    }

    @Test
    public void testRemoveAndGetCount() {
        multiset.add("apple", 5);
        int newCount = multiset.removeAndGetCount("apple", 2);
        assertEquals(3, newCount);
        assertEquals(3, multiset.getCount("apple"));
    }

    @Test
    public void testRemoveAndGetCount_AllOccurrences() {
        multiset.add("apple", 3);
        int newCount = multiset.removeAndGetCount("apple", 5);
        assertEquals(0, newCount);
        assertFalse(multiset.contains("apple"));
    }

    @Test
    public void testRemoveAndGetCount_PartialRemoval() {
        multiset.add("a", 5);
        assertEquals(3, multiset.removeAndGetCount("a", 2));
        assertEquals(3, multiset.getCount("a"));
    }

    @Test
    public void testRemoveAndGetCount_NonExistent() {
        int newCount = multiset.removeAndGetCount("apple", 2);
        assertEquals(0, newCount);
    }

    @Test
    public void testRemoveAndGetCount_Zero() {
        multiset.add("apple", 5);
        int newCount = multiset.removeAndGetCount("apple", 0);
        assertEquals(5, newCount);
    }

    @Test
    public void testRemoveAndGetCount_Negative() {
        assertThrows(IllegalArgumentException.class, () -> multiset.removeAndGetCount("apple", -1));
    }

    @Test
    public void testRemoveAndGetCount_negative() {
        Multiset<String> multiset = new Multiset<>();
        assertThrows(IllegalArgumentException.class, () -> multiset.removeAndGetCount("a", -1));
    }

    @Test
    public void testRemoveAll() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        multiset.add("c", 1);
        assertTrue(multiset.removeAll(Arrays.asList("a", "b")));
        assertFalse(multiset.contains("a"));
        assertFalse(multiset.contains("b"));
        assertTrue(multiset.contains("c"));
    }

    @Test
    public void testRemoveAll_WithOccurrences() {
        multiset.add("a", 5);
        multiset.add("b", 3);
        assertTrue(multiset.removeAll(Arrays.asList("a", "b"), 2));
        assertEquals(3, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b"));
    }

    @Test
    public void testRemoveAll_Collection_WithOccurrences() {
        Multiset<String> multiset = Multiset.of("a", "a", "a", "b", "b", "b", "c");
        Collection<String> toRemove = Arrays.asList("a", "b", "d");

        assertTrue(multiset.removeAll(toRemove, 2));
        assertEquals(1, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b"));
        assertEquals(1, multiset.getCount("c"));
        assertEquals(0, multiset.getCount("d"));
        assertEquals(3, multiset.size());

        assertTrue(multiset.removeAll(Arrays.asList("a", "b"), 2));
        assertEquals(0, multiset.getCount("a"));
        assertEquals(0, multiset.getCount("b"));
        assertEquals(1, multiset.getCount("c"));
        assertEquals(1, multiset.size());
    }

    @Test
    @DisplayName("Test removeAll() collection")
    public void testRemoveAllCollection() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        multiset.add("c", 1);

        assertTrue(multiset.removeAll(Arrays.asList("a", "c")));
        assertEquals(2, multiset.size());
        assertEquals(2, multiset.getCount("b"));
        assertFalse(multiset.contains("a"));
        assertFalse(multiset.contains("c"));
    }

    @Test
    public void testRemoveAll_Empty() {
        multiset.add("a", 3);
        assertFalse(multiset.removeAll(new ArrayList<>()));
        assertEquals(3, multiset.getCount("a"));
    }

    @Test
    public void testRemoveAll_Null() {
        multiset.add("a", 3);
        assertFalse(multiset.removeAll(null));
        assertEquals(3, multiset.getCount("a"));
    }

    @Test
    public void testRemoveAll_WithOccurrences_Zero() {
        multiset.add("a", 3);
        assertFalse(multiset.removeAll(Arrays.asList("a"), 0));
        assertEquals(3, multiset.getCount("a"));
    }

    @Test
    public void testRemoveAll_Collection_WithOccurrences_zero() {
        Multiset<String> multiset = Multiset.of("a", "a", "b");
        assertFalse(multiset.removeAll(Arrays.asList("a", "c"), 0));
        assertEquals(2, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b"));
    }

    @Test
    public void testRemoveAll_Collection_WithOccurrences_emptyCollection() {
        Multiset<String> multiset = Multiset.of("a");
        assertFalse(multiset.removeAll(Collections.emptyList(), 1));
        assertEquals(1, multiset.getCount("a"));
    }

    @Test
    public void testRemoveAll_Collection_WithOccurrences_nullCollection() {
        Multiset<String> multiset = Multiset.of("a");
        assertFalse(multiset.removeAll(null, 1));
        assertEquals(1, multiset.getCount("a"));
    }

    @Test
    public void testRemoveAll_WithOccurrences_Negative() {
        assertThrows(IllegalArgumentException.class, () -> multiset.removeAll(Arrays.asList("a"), -1));
    }

    @Test
    public void testRemoveAll_Collection_WithOccurrences_negative() {
        Multiset<String> multiset = new Multiset<>();
        Collection<String> toRemove = Arrays.asList("a");
        assertThrows(IllegalArgumentException.class, () -> multiset.removeAll(toRemove, -1));
    }

    @Test
    public void testRemoveAllWithOccurrences() {
        multiset.add("a", 5);
        multiset.add("b", 3);
        multiset.add("c", 1);

        List<String> toRemove = Arrays.asList("a", "b");
        assertTrue(multiset.removeAll(toRemove, 2));
        assertEquals(3, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b"));
        assertEquals(1, multiset.getCount("c"));

        assertFalse(multiset.removeAll(toRemove, 0));
        assertFalse(multiset.removeAll(Collections.emptyList(), 5));
        assertFalse(multiset.removeAll(null, 5));

        assertThrows(IllegalArgumentException.class, () -> multiset.removeAll(toRemove, -1));
    }

    @Test
    public void testRemoveAllOccurrences_Element() {
        multiset.add("apple", 5);
        int removed = multiset.removeAllOccurrencesOf("apple");
        assertEquals(5, removed);
        assertFalse(multiset.contains("apple"));
    }

    @Test
    public void testRemoveAllOccurrences_Collection() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        multiset.add("c", 1);
        assertTrue(multiset.removeAllOccurrencesOf(Arrays.asList("a", "c")));
        assertFalse(multiset.contains("a"));
        assertTrue(multiset.contains("b"));
        assertFalse(multiset.contains("c"));
    }

    @Test
    public void testRemoveAllOccurrences_Object() {
        Multiset<String> multiset = Multiset.of("a", "a", "a", "b");
        assertEquals(3, multiset.removeAllOccurrencesOf("a"));
        assertEquals(0, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b"));
        assertEquals(1, multiset.size());

        assertEquals(0, multiset.removeAllOccurrencesOf("c"));
        assertEquals(1, multiset.getCount("b"));
    }

    @Test
    public void testRemoveAllOccurrences() {
        multiset.add("apple", 5);
        multiset.add("banana", 3);

        assertEquals(5, multiset.removeAllOccurrencesOf("apple"));
        assertFalse(multiset.contains("apple"));
        assertEquals(3, multiset.getCount("banana"));

        assertEquals(0, multiset.removeAllOccurrencesOf("cherry"));
    }

    @Test
    public void testRemoveAllOccurrencesCollection() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        multiset.add("c", 1);

        List<String> toRemove = Arrays.asList("a", "b");
        assertTrue(multiset.removeAllOccurrencesOf(toRemove));
        assertFalse(multiset.contains("a"));
        assertFalse(multiset.contains("b"));
        assertTrue(multiset.contains("c"));
    }

    @Test
    public void testRemoveAllOccurrences_NonExistent() {
        int removed = multiset.removeAllOccurrencesOf("nonexistent");
        assertEquals(0, removed);
    }

    @Test
    @DisplayName("Test removeAllOccurrences() single element")
    public void testRemoveAllOccurrencesSingle() {
        multiset.add("apple", 5);

        assertEquals(5, multiset.removeAllOccurrencesOf("apple"));
        assertFalse(multiset.contains("apple"));

        assertEquals(0, multiset.removeAllOccurrencesOf("banana"));
    }

    @Test
    public void testRemoveAllOccurrences_EmptyCollection() {
        multiset.add("a", 3);
        assertFalse(multiset.removeAllOccurrencesOf(Collections.emptyList()));
        assertEquals(3, multiset.getCount("a"));
    }

    @Test
    public void testRemoveAllOccurrencesIf_Predicate() {
        multiset.add("apple", 3);
        multiset.add("banana", 2);
        multiset.add("cherry", 1);
        assertTrue(multiset.removeAllOccurrencesIf(s -> s.startsWith("a")));
        assertFalse(multiset.contains("apple"));
        assertTrue(multiset.contains("banana"));
        assertTrue(multiset.contains("cherry"));
    }

    @Test
    public void testRemoveAllOccurrencesIf_Predicate_NoMatch() {
        multiset.add("apple", 3);
        assertFalse(multiset.removeAllOccurrencesIf(s -> s.startsWith("z")));
        assertTrue(multiset.contains("apple"));
    }

    @Test
    public void testRemoveAllOccurrencesIf_ObjIntPredicate() {
        multiset.add("a", 1);
        multiset.add("b", 3);
        multiset.add("c", 5);
        assertTrue(multiset.removeAllOccurrencesIf((element, count) -> count >= 3));
        assertTrue(multiset.contains("a"));
        assertFalse(multiset.contains("b"));
        assertFalse(multiset.contains("c"));
    }

    @Test
    public void testRemoveAllOccurrencesIf_ObjIntPredicate_NoMatch() {
        multiset.add("a", 1);
        assertFalse(multiset.removeAllOccurrencesIf((element, count) -> count > 10));
        assertTrue(multiset.contains("a"));
    }

    @Test
    public void testRemoveAllOccurrencesIf_Predicate_noMatch() {
        Multiset<String> multiset = Multiset.of("apple", "banana");
        assertFalse(multiset.removeAllOccurrencesIf(s -> s.startsWith("z")));
        assertEquals(1, multiset.getCount("apple"));
        assertEquals(1, multiset.getCount("banana"));
    }

    @Test
    public void testRemoveAllOccurrencesIf_ObjIntPredicate_noMatch() {
        Multiset<String> multiset = Multiset.of("a", "b");
        assertFalse(multiset.removeAllOccurrencesIf((s, count) -> count > 1));
        assertEquals(1, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b"));
    }

    @Test
    @DisplayName("Test removeAllOccurrencesIf() with predicate")
    public void testRemoveAllOccurrencesIfPredicate() {
        multiset.add("apple", 3);
        multiset.add("apricot", 2);
        multiset.add("banana", 1);

        assertTrue(multiset.removeAllOccurrencesIf(s -> s.startsWith("ap")));
        assertEquals(1, multiset.size());
        assertEquals(1, multiset.getCount("banana"));
    }

    @Test
    @DisplayName("Test removeAllOccurrencesIf() with ObjIntPredicate")
    public void testRemoveAllOccurrencesIfObjIntPredicate() {
        multiset.add("a", 1);
        multiset.add("b", 2);
        multiset.add("c", 3);
        multiset.add("d", 4);

        assertTrue(multiset.removeAllOccurrencesIf((element, count) -> count >= 3));
        assertEquals(3, multiset.size());
        assertTrue(multiset.contains("a"));
        assertTrue(multiset.contains("b"));
        assertFalse(multiset.contains("c"));
        assertFalse(multiset.contains("d"));
    }

    @Test
    public void testRemoveAllOccurrencesIf_Predicate_Null() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> multiset.removeAllOccurrencesIf((java.util.function.Predicate<String>) null));
    }

    @Test
    public void testRemoveAllOccurrencesIf_ObjIntPredicate_Null() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> multiset.removeAllOccurrencesIf((com.landawn.abacus.util.function.ObjIntPredicate<String>) null));
    }

    @Test
    public void testRemoveAllOccurrencesIf_Predicate_null() {
        Multiset<String> multiset = new Multiset<>();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> multiset.removeAllOccurrencesIf((Predicate<String>) null));
    }

    @Test
    public void testRemoveAllOccurrencesIf_ObjIntPredicate_null() {
        Multiset<String> multiset = new Multiset<>();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> multiset.removeAllOccurrencesIf((ObjIntPredicate<String>) null));
    }

    @Test
    public void testRemoveAllOccurrencesIf() {
        multiset.add("apple", 3);
        multiset.add("banana", 2);
        multiset.add("cherry", 1);

        Predicate<String> startsWithA = s -> s.startsWith("a");
        assertTrue(multiset.removeAllOccurrencesIf(startsWithA));
        assertFalse(multiset.contains("apple"));
        assertTrue(multiset.contains("banana"));
        assertTrue(multiset.contains("cherry"));

        assertFalse(multiset.removeAllOccurrencesIf(startsWithA));

        assertThrows(IllegalArgumentException.class, () -> multiset.removeAllOccurrencesIf((Predicate) null));
    }

    @Test
    public void testRemoveAllOccurrencesIfObjInt() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        multiset.add("c", 1);

        ObjIntPredicate<String> countGreaterThan2 = (element, count) -> count > 2;
        assertTrue(multiset.removeAllOccurrencesIf(countGreaterThan2));
        assertFalse(multiset.contains("a"));
        assertTrue(multiset.contains("b"));
        assertTrue(multiset.contains("c"));

        assertFalse(multiset.removeAllOccurrencesIf(countGreaterThan2));

        assertThrows(IllegalArgumentException.class, () -> multiset.removeAllOccurrencesIf((ObjIntPredicate<String>) null));
    }

    @Test
    public void testRemoveAllSelfClearsWithoutConcurrentModification() {
        Multiset<String> set = Multiset.of("a", "a", "b");

        assertTrue(set.removeAll(set));
        assertTrue(set.isEmpty());

        assertFalse(set.removeAll(set));
    }

    @Test
    public void testRemoveAllOccurrencesSelfClearsWithoutConcurrentModification() {
        Multiset<String> set = Multiset.of("a", "a", "b");

        assertTrue(set.removeAllOccurrencesOf(set));
        assertTrue(set.isEmpty());
    }

    @Test
    public void testRemoveOccurrencesSelfClearsWithoutConcurrentModification() {
        Multiset<String> set = Multiset.of("a", "a", "b");

        assertTrue(set.removeOccurrences(set, 1));
        assertTrue(set.isEmpty());

        assertFalse(set.removeOccurrences(set, 1));
    }

    @Test
    public void testRemoveAllElementSetViewClearsWithoutConcurrentModification() {
        final Multiset<String> set = Multiset.of("a", "a", "b");

        assertTrue(set.removeAll(set.elementSet()));
        assertTrue(set.isEmpty());
    }

    @Test
    public void testRemoveAllOccurrencesOf_multisetSourceUsesDistinctKeys() {
        // High counts must not force materializing every occurrence via Multiset.toArray().
        final Multiset<String> target = Multiset.of("a", "a", "b", "c");
        final Multiset<String> source = new Multiset<>();
        source.setCount("a", 1_000_000);
        source.setCount("c", 2_000_000);
        source.setCount("z", 5_000_000);

        assertTrue(target.removeAllOccurrencesOf(source));
        assertEquals(0, target.getCount("a"));
        assertEquals(1, target.getCount("b"));
        assertEquals(0, target.getCount("c"));
        assertEquals(1, target.size());
    }

    /**
     * Collection.removeIf must remove matching elements. Pre-fix: iterator() had no remove(),
     * so the JDK default removeIf threw UnsupportedOperationException on the first match.
     */
    @Test
    public void testRemoveIf_removesMatchingElements() {
        final Multiset<String> multiset = Multiset.of("a", "a", "b", "c", "c");
        assertTrue(multiset.removeIf(s -> s.equals("a") || s.equals("c")));
        assertEquals(0, multiset.getCount("a"));
        assertEquals(0, multiset.getCount("c"));
        assertEquals(1, multiset.getCount("b"));
        assertEquals(1, multiset.size());

        assertFalse(multiset.removeIf(s -> s.equals("a")));
        assertEquals(1, multiset.getCount("b"));
    }

    @Test
    public void testRemoveIf_evaluatesOncePerDistinctElement() {
        final Multiset<String> multiset = Multiset.of("a", "a", "a", "b", "b");
        final int[] invocationCount = { 0 };

        assertTrue(multiset.removeIf(s -> {
            invocationCount[0]++;
            return s.equals("a");
        }));

        assertEquals(2, invocationCount[0]);
        assertEquals(0, multiset.getCount("a"));
        assertEquals(2, multiset.getCount("b"));
        assertThrows(NullPointerException.class, () -> multiset.removeIf(null));
    }
}
