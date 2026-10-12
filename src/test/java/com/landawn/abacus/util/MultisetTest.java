package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeMap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.u.Optional;

public class MultisetTest extends MultisetTestSupport {

    @Test
    public void test_01() {
        Multiset<String> set = CommonUtil.toMultiset("a", "b", "c", "C");
        set.add("a");

        assertEquals(2, set.getCount("a"));
        set.remove("a");

        assertEquals(1, set.getCount("a"));
        set.add("b", 100);
        set.remove("b", 90);

        assertEquals(11, set.getCount("b"));
        set.remove("b", 11);
        assertEquals(0, set.getCount("b"));

        set.add("C");
        assertEquals(2, set.getCount("C"));
        assertEquals(1, set.getCount("a"));
        assertEquals(1, set.getCount("c"));
    }

    @Test
    public void test_02() {
        Multiset<String> set = CommonUtil.toMultiset("a", "b", "c");
        set.setCount("a", 0);
        set.setCount("a", 3);
        assertEquals(3, set.getCount("a"));

        assertThrows(IllegalArgumentException.class, () -> set.setCount("a", -1));

        assertEquals(3, set.maxOccurrences().get().left().intValue());

        assertThrows(IllegalArgumentException.class, () -> set.add("a", -1));
        assertThrows(IllegalArgumentException.class, () -> set.add("a", Integer.MAX_VALUE));

        assertEquals(3, set.getCount("a"));

        assertTrue(set.contains("a"));
        assertFalse(set.contains("e"));

        assertTrue(set.containsAll(CommonUtil.toList("a", "b")));
        assertFalse(set.contains(CommonUtil.toList("b", "e")));

        assertTrue(set.containsAll(CommonUtil.toList("a")));
        assertFalse(set.contains(CommonUtil.toList("e")));

        assertThrows(IllegalArgumentException.class, () -> set.remove("a", -1));

        set.remove("a", 2);
        assertEquals(1, set.getCount("a"));

        set.remove("a", 2);
        assertEquals(0, set.getCount("a"));

        set.add("a", 3);
        assertEquals(3, set.getCount("a"));

        CommonUtil.toList("a").forEach(e -> set.remove(e));
        assertEquals(2, set.getCount("a"));
        assertEquals(1, set.getCount("b"));

        CommonUtil.toList("a", "b", "e").forEach(e -> set.remove(e, 2));
        assertEquals(0, set.getCount("a"));
        assertEquals(0, set.getCount("b"));

        set.add("a", 3);
        set.add("b", 3);

        set.retainAll(CommonUtil.toList("a", "b", "e"));

        assertEquals(3, set.getCount("a"));
        assertEquals(3, set.getCount("b"));

        Multiset<String> set2 = CommonUtil.toMultiset();
        set2.setCount("a", 3);
        set2.setCount("b", 3);

        assertTrue(CommonUtil.toSet(set).contains(set2));

        set.clear();
        assertTrue(set.isEmpty());
    }

    @Test
    public void testAccept() {
        multiset.add("a", 3);

        List<Integer> sizes = new ArrayList<>();
        multiset.accept(ms -> sizes.add(ms.size()));

        assertEquals(1, sizes.size());
        assertEquals(3, sizes.get(0).intValue());
    }

    @Test
    public void testAccept_WithException() {
        assertThrows(RuntimeException.class, () -> {
            multiset.accept(ms -> {
                throw new RuntimeException("test");
            });
        });
    }

    // acceptIfNotEmpty

    @Test
    public void testAcceptIfNotEmpty() {
        final boolean[] visited = { false };

        multiset.acceptIfNotEmpty(ms -> visited[0] = true).orElse(() -> visited[0] = false);
        assertFalse(visited[0]);

        multiset.add("test", 1);
        multiset.acceptIfNotEmpty(ms -> visited[0] = true).orElse(() -> visited[0] = false);
        assertTrue(visited[0]);
    }

    @Test
    public void testAcceptIfNotEmpty_Empty() {
        List<Integer> sizes = new ArrayList<>();
        List<String> orElseRun = new ArrayList<>();

        multiset.acceptIfNotEmpty(ms -> sizes.add(ms.size())).orElse(() -> orElseRun.add("executed"));

        assertEquals(0, sizes.size());
        assertEquals(1, orElseRun.size());
    }

    @Test
    public void testAcceptIfNotEmpty_NotEmpty() {
        multiset.add("a", 3);

        List<Integer> sizes = new ArrayList<>();
        multiset.acceptIfNotEmpty(ms -> sizes.add(ms.size()));

        assertEquals(1, sizes.size());
        assertEquals(3, sizes.get(0).intValue());
    }

    @Test
    public void testApply() {
        multiset.add("a", 3);
        multiset.add("b", 2);

        int result = multiset.apply(ms -> ms.size());
        assertEquals(5, result);
    }

    @Test
    public void testApply_WithException() {
        assertThrows(RuntimeException.class, () -> {
            multiset.apply(ms -> {
                throw new RuntimeException("test");
            });
        });
    }

    // applyIfNotEmpty

    @Test
    public void testApplyIfNotEmpty() {
        Optional<Integer> emptyResult = multiset.applyIfNotEmpty(ms -> ms.size());
        assertFalse(emptyResult.isPresent());

        multiset.add("test", 1);
        Optional<Integer> result = multiset.applyIfNotEmpty(ms -> ms.size());
        assertTrue(result.isPresent());
        assertEquals(1, result.get().intValue());
    }

    @Test
    public void testApplyIfNotEmpty_Empty() {
        Optional<Integer> result = multiset.applyIfNotEmpty(ms -> ms.size());
        assertFalse(result.isPresent());
    }

    @Test
    public void testApplyIfNotEmpty_NotEmpty() {
        multiset.add("a", 3);
        Optional<Integer> result = multiset.applyIfNotEmpty(ms -> ms.size());
        assertTrue(result.isPresent());
        assertEquals(3, result.get().intValue());
    }

    @Test
    public void testApplyIfNotEmptyThrowsNPEForANullResult() {
        final Multiset<String> multiset = Multiset.of("x");
        assertFalse(multiset.isEmpty());
        assertThrows(NullPointerException.class, () -> multiset.applyIfNotEmpty(ms -> (String) null));

        assertEquals(Optional.empty(), new Multiset<String>().applyIfNotEmpty(ms -> "value"));
        assertEquals(Optional.of(1), multiset.applyIfNotEmpty(Multiset::size));
    }

    @Test
    public void testEdgeCases() {
        multiset.add(null, 3);
        assertEquals(3, multiset.getCount(null));
        assertTrue(multiset.contains(null));

        assertEquals(0, multiset.add("zero", 0));
        assertEquals(0, multiset.getCount("zero"));
        assertFalse(multiset.contains("zero"));

        Multiset<Integer> largeMs = new Multiset<>();
        for (int i = 0; i < 1000; i++) {
            largeMs.add(i, i % 10 + 1);
        }
        assertEquals(1000, largeMs.countOfDistinctElements());

        Multiset<String> overflowMs = new Multiset<>();
        overflowMs.add("max", Integer.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> overflowMs.add("max", 1));
    }

    @Test
    public void testEmptyAfterOperations() {
        multiset.add("a", 3);
        multiset.add("b", 2);

        multiset.clear();
        assertTrue(multiset.isEmpty());
        assertEquals(0, multiset.size());

        assertFalse(multiset.minOccurrences().isPresent());
        assertFalse(multiset.maxOccurrences().isPresent());
    }

    @Test
    @DisplayName("Test operations on empty multiset")
    public void testEmptyOperations() {
        assertEquals(0, multiset.size());
        assertEquals(0, multiset.countOfDistinctElements());
        assertTrue(multiset.isEmpty());
        assertEquals(0, multiset.sumOfOccurrences());
        assertTrue(multiset.averageOfOccurrences().isEmpty());
        assertTrue(multiset.minOccurrences().isEmpty());
        assertTrue(multiset.maxOccurrences().isEmpty());
        assertFalse(multiset.iterator().hasNext());
        assertTrue(multiset.elementSet().isEmpty());
        assertTrue(multiset.entrySet().isEmpty());
    }

    // equals

    @Test
    public void testEquals() {
        Multiset<String> m1 = Multiset.of("a", "b", "a");
        Multiset<String> m2 = Multiset.of("b", "a", "a");
        Multiset<String> m3 = new Multiset<>();
        m3.add("a", 2);
        m3.add("b", 1);

        assertTrue(m1.equals(m1));
        assertTrue(m1.equals(m2));
        assertTrue(m2.equals(m1));
        assertTrue(m1.equals(m3));
        assertTrue(m3.equals(m1));

        Multiset<String> m4 = Multiset.of("a", "b");
        assertFalse(m1.equals(m4));
        assertFalse(m4.equals(m1));

        Multiset<String> m5 = Multiset.of("a", "c", "a");
        assertFalse(m1.equals(m5));

        assertFalse(m1.equals(null));
        assertFalse(m1.equals(new Object()));

        Multiset<String> empty1 = new Multiset<>();
        Multiset<String> empty2 = new Multiset<>();
        assertTrue(empty1.equals(empty2));
        assertFalse(empty1.equals(m1));
    }

    @Test
    public void testEquals_DifferentCounts() {
        multiset.add("a", 3);

        Multiset<String> other = new Multiset<>();
        other.add("a", 2);

        assertFalse(multiset.equals(other));
    }

    @Test
    public void testEquals_DifferentElements() {
        multiset.add("a", 3);

        Multiset<String> other = new Multiset<>();
        other.add("b", 3);

        assertFalse(multiset.equals(other));
    }

    @Test
    public void testEquals_Equal() {
        multiset.add("a", 3);
        multiset.add("b", 2);

        Multiset<String> other = new Multiset<>();
        other.add("a", 3);
        other.add("b", 2);

        assertTrue(multiset.equals(other));
    }

    @Test
    public void testEquals_NotMultiset() {
        assertFalse(multiset.equals("not a multiset"));
    }

    @Test
    public void testEquals_Null() {
        assertFalse(multiset.equals(null));
    }

    @Test
    public void testEquals_SameInstance() {
        assertTrue(multiset.equals(multiset));
    }

    @Test
    public void testHashCode() {
        multiset.add("a", 3);
        multiset.add("b", 2);

        Multiset<String> other = new Multiset<>();
        other.add("a", 3);
        other.add("b", 2);

        assertEquals(multiset.hashCode(), other.hashCode());
    }

    @Test
    @DisplayName("Test integer overflow protection")
    public void testIntegerOverflowProtection() {
        intMultiset.add(1, Integer.MAX_VALUE);

        assertThrows(IllegalArgumentException.class, () -> intMultiset.add(1, 1));

        assertThrows(IllegalArgumentException.class, () -> intMultiset.addAndGetCount(1, 1));
    }

    @Test
    public void testLargeCount() {
        int largeCount = 1000000;
        multiset.add("test", largeCount);
        assertEquals(largeCount, multiset.getCount("test"));
        assertEquals(largeCount, multiset.size());
    }

    @Test
    public void testLinkedHashMapOrdering() {
        Multiset<String> ordered = new Multiset<>(LinkedHashMap.class);
        ordered.add("z", 1);
        ordered.add("a", 1);
        ordered.add("m", 1);

        List<String> keys = new ArrayList<>(ordered.elementSet());
        assertEquals("z", keys.get(0));
        assertEquals("a", keys.get(1));
        assertEquals("m", keys.get(2));
    }

    @Test
    public void testMultipleOperations_Complex() {
        multiset.add("a", 5);
        multiset.add("b", 3);
        multiset.add("c", 7);

        assertEquals(15, multiset.size());
        assertEquals(3, multiset.countOfDistinctElements());

        multiset.remove("b", 2);
        assertEquals(13, multiset.size());

        multiset.setCount("a", 10);
        assertEquals(18, multiset.size());

        multiset.removeAllOccurrencesOf("c");
        assertEquals(11, multiset.size());
        assertEquals(2, multiset.countOfDistinctElements());
    }

    @Test
    public void testNullElements() {
        Multiset<String> testMultiset = new Multiset<>();
        testMultiset.add(null, 5);
        assertEquals(5, testMultiset.getCount(null));
        assertTrue(testMultiset.contains(null));

        testMultiset.remove(null, 2);
        assertEquals(3, testMultiset.getCount(null));
        assertTrue(testMultiset.contains(null));

        testMultiset.clear();
        assertEquals(0, testMultiset.getCount(null));
        assertFalse(testMultiset.contains(null));

        testMultiset.add(null, 7);
        int removed = testMultiset.removeAllOccurrencesOf((String) null);
        assertEquals(7, removed);
    }

    @Test
    @DisplayName("an EMPTY comparator-less TreeMap backing still throws ClassCastException for a non-Comparable key")
    public void reviewFixes20260906_emptySortedBackingWithForeignKey() {
        final Multiset<Integer> emptyTree = new Multiset<>(TreeMap.class);

        // TreeMap.getEntry casts the key to Comparable BEFORE it looks at the root, so "empty never compares"
        // does not save a comparator-less tree.
        assertThrows(ClassCastException.class, () -> emptyTree.contains(new Object()));
        assertThrows(ClassCastException.class, () -> emptyTree.getCount(new Object()));
        assertThrows(ClassCastException.class, () -> emptyTree.remove(new Object(), 1));
        assertThrows(ClassCastException.class, () -> emptyTree.removeAllOccurrencesOf(new Object()));

        // A foreign key that IS Comparable passes the cast, so the empty tree reports "absent".
        assertFalse(emptyTree.contains("not an Integer"));

        // An explicitly comparator-based empty tree never casts at all.
        final Multiset<Integer> emptyComparatorTree = new Multiset<>(() -> new TreeMap<Integer, Object>(Comparator.<Integer> naturalOrder()));
        assertFalse(emptyComparatorTree.contains(new Object()));

        // Control: the non-empty cases and the HashMap backing are unchanged.
        final Multiset<Integer> nonEmptyTree = new Multiset<>(TreeMap.class);
        nonEmptyTree.add(1);
        assertThrows(ClassCastException.class, () -> nonEmptyTree.contains(new Object()));
        assertFalse(new Multiset<Integer>().contains(new Object()));
    }

    @Test
    public void testTreeMapOrdering() {
        Multiset<String> sorted = new Multiset<>(TreeMap.class);
        sorted.add("z", 1);
        sorted.add("a", 1);
        sorted.add("m", 1);

        List<String> keys = new ArrayList<>(sorted.elementSet());
        assertEquals("a", keys.get(0));
        assertEquals("m", keys.get(1));
        assertEquals("z", keys.get(2));
    }
}
