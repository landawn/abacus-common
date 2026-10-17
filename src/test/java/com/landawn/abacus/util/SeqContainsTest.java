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

    // ---- deep review 2026-10-16 G139 begin ----
    // G139-02: containsAll(T...) builds its membership set once; duplicates (also collapsing to one value) and nulls.
    @Test
    public void testContainsAll_varargsDuplicatesAndNulls() throws Exception {
        assertTrue(Seq.of("a", null, "b").containsAll("a", "a", "a"));
        assertTrue(Seq.of("a", null, "b").containsAll((String) null, null, null));
        assertTrue(Seq.of("a", null, "b").containsAll("a", null, "b", "a"));
        assertTrue(Seq.of("b", "a", "b", null).containsAll("b", "a", "b"));
        assertFalse(Seq.of("a", "b").containsAll("a", "a", null));
        assertFalse(Seq.of("a", null, "b").containsAll("a", "c", "a"));
        assertFalse(Seq.of("b").containsAll("a", "a", "a"));
        assertFalse(Seq.<String, Exception> empty().containsAll("a", "a", "a"));
        assertTrue(Seq.of("a", "b", "c", "d", "e").containsAll("e", "d", "c", "b", "a"));

        // arrays are matched by identity, also when duplicates collapse the request to one element
        final int[] first = { 1 };
        final int[] sameContent = { 1 };
        assertTrue(Seq.of(Arrays.asList(first, sameContent)).containsAll(first, first, first));
        assertFalse(Seq.of(Collections.singletonList(sameContent)).containsAll(first, first, first));
        assertFalse(Seq.of(Arrays.asList(sameContent, new int[] { 2 })).containsAll(first, first, sameContent));
    }

    // G139-02: containsAll(Collection) consumes a private copy, so a caller's HashSet is never modified; null probes
    // against a null-hostile argument set are safe.
    @Test
    public void testContainsAll_callerSetNotModifiedAndNullElements() throws Exception {
        final HashSet<String> wanted = new HashSet<>(Arrays.asList("b", null, "q"));

        assertTrue(Seq.of("q", null, "b", "z").containsAll(wanted));
        assertFalse(Seq.of("q", "b").containsAll(wanted));
        assertTrue(Seq.of("x", null, "a").containsAny(wanted));
        assertFalse(Seq.of("x", "a").containsAny(wanted));
        assertTrue(Seq.of("x", null, "a").containsAny(java.util.Set.of("a", "b")));
        assertTrue(Seq.of("x", null, "a", "b").containsAll(java.util.Set.of("a", "b")));
        assertTrue(Seq.of("x", null, "a", "b").containsAll(new java.util.TreeSet<>(Arrays.asList("b", "a"))));
        assertEquals(new HashSet<>(Arrays.asList("b", null, "q")), wanted);
    }
    // ---- deep review 2026-10-16 G139 end ----
}
