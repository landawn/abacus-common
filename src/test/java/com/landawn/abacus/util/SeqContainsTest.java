package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

public class SeqContainsTest extends SeqTestSupport {

    @Test
    public void testContainsAll() throws Exception {
        assertTrue(Seq.of(1, 2, 3, 4).containsAll(2, 3));
        assertTrue(Seq.of(1, 2, 3, 4).containsAll(2, 4));
        assertTrue(Seq.of(1, 2, 3).containsAll());
        assertTrue(Seq.of(1, 2, 3).containsAll(2));
        assertTrue(Seq.of(1, 2, 3).containsAll(2, 2));
        assertFalse(Seq.of(1, 2, 3).containsAll(2, 4));
        assertFalse(Seq.of(1, 2, 3).containsAll(2, 3, 4));
        assertFalse(Seq.of(1, 2, 3).containsAll(Arrays.asList(1, 4)));

        assertTrue(Seq.of(1, 2, 3, 4).containsAll(Arrays.asList(2, 4)));
        assertTrue(Seq.of(1, 2, 3).containsAll(Collections.emptyList()));
        assertTrue(Seq.of(1, 2, 3).containsAll(Collections.singletonList(2)));
        assertTrue(Seq.of(1, 2, 3, 4).containsAll(new HashSet<>(Arrays.asList(2, 3))));
        assertFalse(Seq.of(1, 2, 3, 4).containsAll(Arrays.asList(2, 5)));
    }

    @Test
    public void testContainsAny() throws Exception {
        assertTrue(Seq.of(1, 2, 3).containsAny(2, 4));
        assertTrue(Seq.of(1, 2, 3).containsAny(2));
        assertTrue(Seq.of(1, 2, 3).containsAny(2, 2));
        assertTrue(Seq.of(1, 2, 3).containsAny(5, 6, 7, 1));
        assertFalse(Seq.of(1, 2, 3).containsAny(4, 5));
        assertFalse(Seq.of(1, 2, 3).containsAny());

        assertTrue(Seq.of(1, 2, 3, 4, 5).containsAny(Arrays.asList(7, 3, 9)));
        assertTrue(Seq.of(1, 2, 3).containsAny(Collections.singletonList(2)));
        assertTrue(Seq.of(1, 2, 3).containsAny(new HashSet<>(Arrays.asList(5, 2))));
        assertFalse(Seq.of(1, 2, 3).containsAny(Collections.emptyList()));
        assertFalse(Seq.of(1, 2, 3).containsAny(Arrays.asList(4, 5, 6)));
    }

    @Test
    public void testContainsNone() throws Exception {
        assertTrue(Seq.of(1, 2, 3).containsNone(4, 5));
        assertTrue(Seq.of(1, 2, 3).containsNone());
        assertFalse(Seq.of(1, 2, 3).containsNone(2, 4));
        assertFalse(Seq.of(1, 2, 3).containsNone(4, 2, 5));

        assertTrue(Seq.of(1, 2, 3, 4, 5).containsNone(Arrays.asList(6, 7, 8)));
        assertTrue(Seq.of(1, 2, 3).containsNone(Collections.emptyList()));
        assertFalse(Seq.of(1, 2, 3).containsNone(Arrays.asList(4, 2, 5)));
    }

    @Test
    public void testContainsDuplicates() throws Exception {
        assertTrue(Seq.of(1, 2, 2, 3).containsDuplicates());
        assertFalse(Seq.of(1, 2, 3, 4).containsDuplicates());
        assertFalse(Seq.<Integer, Exception> empty().containsDuplicates());
        assertTrue(Seq.of((Integer) null, null).containsDuplicates());
        assertFalse(Seq.of((Integer) null, 1).containsDuplicates());
    }

    @Test
    public void testContainsDuplicates_ArrayElements() throws Exception {
        final int[] a1 = { 1, 2 };
        final int[] a2 = { 1, 2 };

        assertEquals(1, Seq.of(a1, a2).distinct().count());
        assertTrue(Seq.of(a1, a2).containsDuplicates());
        assertEquals(2, Seq.of(new int[] { 1 }, new int[] { 2 }).distinct().count());
        assertFalse(Seq.of(new int[] { 1 }, new int[] { 2 }).containsDuplicates());
        assertTrue(Seq.of(new String[] { "x" }, new String[] { "x" }).containsDuplicates());
    }

    // --- G12-005 (doc): distinct()/containsDuplicates() normalize an array element to content equality;
    // --- containsAll/containsAny use the elements' own equals/hashCode, i.e. identity for arrays. The two rules
    // --- really do differ, so distinct()'s javadoc must not claim they are shared.
    @Test
    public void testDistinctComparesArraysByContentButContainsAllDoesNot() throws Exception {
        final int[] a1 = { 1, 2 };
        final int[] a2 = { 1, 2 };
        final List<int[]> single = Arrays.asList(a1);

        assertEquals(1, Seq.of(a1, a2).distinct().count());
        assertTrue(Seq.of(a1, a2).containsDuplicates());

        assertFalse(Seq.of(single).containsAll(Arrays.asList(a2)));
        assertFalse(Seq.of(single).containsAny(Arrays.asList(a2)));
        assertTrue(Seq.of(single).containsAll(Arrays.asList(a1)));

        // null counts as a value of its own rather than raising.
        assertEquals(Arrays.asList("a", null, "b"), Seq.of("a", null, "b", null).distinct().toList());
    }
}
