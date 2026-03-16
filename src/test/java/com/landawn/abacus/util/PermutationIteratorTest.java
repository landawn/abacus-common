package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class PermutationIteratorTest extends TestBase {

    @Test
    public void testOf() {
        List<Integer> elements = Arrays.asList(1, 2, 3);
        ObjIterator<List<Integer>> iter = PermutationIterator.of(elements);

        int count = 0;
        while (iter.hasNext()) {
            List<Integer> perm = iter.next();
            assertEquals(3, perm.size());
            count++;
        }
        // 3! = 6 permutations
        assertEquals(6, count);
    }

    @Test
    public void testOf_EmptyCollection() {
        ObjIterator<List<Integer>> iter = PermutationIterator.of(Arrays.asList());
        assertTrue(iter.hasNext());
        List<Integer> perm = iter.next();
        assertEquals(0, perm.size());
        assertTrue(!iter.hasNext());
    }

    @Test
    public void testOf_SingleElement() {
        ObjIterator<List<String>> iter = PermutationIterator.of(Arrays.asList("a"));
        assertTrue(iter.hasNext());
        assertEquals(Arrays.asList("a"), iter.next());
        assertTrue(!iter.hasNext());
    }

    @Test
    public void testOrderedRejectsNullComparator() {
        assertThrows(IllegalArgumentException.class, () -> PermutationIterator.ordered(Arrays.asList(1, 2), null));
    }

    @Test
    public void testOrderedWithComparatorStillWorks() {
        ObjIterator<List<Integer>> iter = PermutationIterator.ordered(Arrays.asList(2, 1), Comparator.naturalOrder());

        assertTrue(iter.hasNext());
        assertEquals(Arrays.asList(1, 2), iter.next());
    }

    @Test
    public void testOf_NullThrows() {
        assertThrows(IllegalArgumentException.class, () -> PermutationIterator.of(null));
    }

    @Test
    public void testOf_TwoElements() {
        List<Integer> elements = Arrays.asList(1, 2);
        ObjIterator<List<Integer>> iter = PermutationIterator.of(elements);

        Set<List<Integer>> perms = new HashSet<>();
        while (iter.hasNext()) {
            perms.add(iter.next());
        }
        assertEquals(2, perms.size());
        assertTrue(perms.contains(Arrays.asList(1, 2)));
        assertTrue(perms.contains(Arrays.asList(2, 1)));
    }

    @Test
    public void testOf_FourElements() {
        List<Integer> elements = Arrays.asList(1, 2, 3, 4);
        ObjIterator<List<Integer>> iter = PermutationIterator.of(elements);

        int count = 0;
        while (iter.hasNext()) {
            iter.next();
            count++;
        }
        // 4! = 24
        assertEquals(24, count);
    }

    @Test
    public void testOf_ExhaustionThrows() {
        ObjIterator<List<Integer>> iter = PermutationIterator.of(Arrays.asList(1));
        iter.next();
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testOf_EmptyCollection_ExhaustionThrows() {
        ObjIterator<List<Integer>> iter = PermutationIterator.of(Arrays.asList());
        iter.next();
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testOf_AllPermutationsAreDistinctLists() {
        List<Integer> elements = Arrays.asList(1, 2, 3);
        ObjIterator<List<Integer>> iter = PermutationIterator.of(elements);

        List<List<Integer>> all = new ArrayList<>();
        while (iter.hasNext()) {
            all.add(iter.next());
        }
        assertEquals(6, all.size());
        // Each permutation should contain the same elements
        for (List<Integer> perm : all) {
            assertTrue(perm.containsAll(elements));
            assertTrue(elements.containsAll(perm));
        }
    }

    @Test
    public void testOrdered_NullThrows() {
        assertThrows(IllegalArgumentException.class, () -> PermutationIterator.ordered(null));
    }

    @Test
    public void testOrdered_EmptyCollection() {
        ObjIterator<List<Integer>> iter = PermutationIterator.ordered(java.util.Collections.<Integer> emptyList());
        assertTrue(iter.hasNext());
        List<Integer> perm = iter.next();
        assertEquals(0, perm.size());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOrdered_EmptyCollection_ExhaustionThrows() {
        ObjIterator<List<Integer>> iter = PermutationIterator.ordered(java.util.Collections.<Integer> emptyList());
        iter.next();
        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testOrdered_SingleElement() {
        ObjIterator<List<Integer>> iter = PermutationIterator.ordered(Arrays.asList(42));
        assertTrue(iter.hasNext());
        assertEquals(Arrays.asList(42), iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOrdered_TwoElements() {
        ObjIterator<List<Integer>> iter = PermutationIterator.ordered(Arrays.asList(2, 1));

        assertTrue(iter.hasNext());
        assertEquals(Arrays.asList(1, 2), iter.next());
        assertTrue(iter.hasNext());
        assertEquals(Arrays.asList(2, 1), iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOrdered_ThreeElements_FullIteration() {
        ObjIterator<List<Integer>> iter = PermutationIterator.ordered(Arrays.asList(3, 1, 2));

        List<List<Integer>> all = new ArrayList<>();
        while (iter.hasNext()) {
            all.add(iter.next());
        }
        assertEquals(6, all.size());
        // First should be sorted ascending, last should be sorted descending
        assertEquals(Arrays.asList(1, 2, 3), all.get(0));
        assertEquals(Arrays.asList(3, 2, 1), all.get(5));
    }

    @Test
    public void testOrdered_WithDuplicates() {
        ObjIterator<List<Integer>> iter = PermutationIterator.ordered(Arrays.asList(1, 1, 2));

        List<List<Integer>> all = new ArrayList<>();
        while (iter.hasNext()) {
            all.add(iter.next());
        }
        // With duplicates [1,1,2], ordered generates only unique permutations: 3!/2! = 3
        assertEquals(3, all.size());
        assertEquals(Arrays.asList(1, 1, 2), all.get(0));
        assertEquals(Arrays.asList(1, 2, 1), all.get(1));
        assertEquals(Arrays.asList(2, 1, 1), all.get(2));
    }

    @Test
    public void testOrdered_AllDuplicates() {
        ObjIterator<List<Integer>> iter = PermutationIterator.ordered(Arrays.asList(1, 1));

        assertTrue(iter.hasNext());
        assertEquals(Arrays.asList(1, 1), iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOrdered_ExhaustionThrows() {
        ObjIterator<List<Integer>> iter = PermutationIterator.ordered(Arrays.asList(1, 2));
        iter.next();
        iter.next();
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testOrdered_WithComparator_FullIteration() {
        ObjIterator<List<String>> iter = PermutationIterator.ordered(Arrays.asList("c", "a", "b"), Comparator.naturalOrder());

        List<List<String>> all = new ArrayList<>();
        while (iter.hasNext()) {
            all.add(iter.next());
        }
        assertEquals(6, all.size());
        assertEquals(Arrays.asList("a", "b", "c"), all.get(0));
        assertEquals(Arrays.asList("c", "b", "a"), all.get(5));
    }

    @Test
    public void testOrdered_WithComparator_ReverseOrder() {
        ObjIterator<List<Integer>> iter = PermutationIterator.ordered(Arrays.asList(1, 2, 3), Comparator.reverseOrder());

        List<List<Integer>> all = new ArrayList<>();
        while (iter.hasNext()) {
            all.add(iter.next());
        }
        assertEquals(6, all.size());
        // With reverse order, first should be descending, last should be ascending
        assertEquals(Arrays.asList(3, 2, 1), all.get(0));
        assertEquals(Arrays.asList(1, 2, 3), all.get(5));
    }

    @Test
    public void testOrdered_WithComparator_Duplicates() {
        ObjIterator<List<Integer>> iter = PermutationIterator.ordered(Arrays.asList(2, 1, 2), Comparator.naturalOrder());

        List<List<Integer>> all = new ArrayList<>();
        while (iter.hasNext()) {
            all.add(iter.next());
        }
        assertEquals(3, all.size());
    }

    @Test
    public void testOrdered_WithComparator_NullElements() {
        assertThrows(IllegalArgumentException.class, () -> PermutationIterator.ordered(null, Comparator.naturalOrder()));
    }
}
