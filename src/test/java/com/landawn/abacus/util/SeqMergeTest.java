package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

public class SeqMergeTest extends SeqTestSupport {

    private static MergeResult takeSmaller(Integer a, Integer b) {
        return a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
    }

    @Test
    public void testMerge_Arrays() throws Exception {
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), Seq.merge(new Integer[] { 1, 3, 5 }, new Integer[] { 2, 4, 6 }, SeqMergeTest::takeSmaller).toList());
        assertTrue(Seq.merge(new Integer[] {}, new Integer[] {}, (x, y) -> MergeResult.TAKE_FIRST).toList().isEmpty());
        assertEquals(Collections.singletonList(1), Seq.merge(new Integer[] { 1 }, new Integer[] {}, (x, y) -> MergeResult.TAKE_FIRST).toList());
        assertEquals(Arrays.asList(2, 4, 6), Seq.merge(new Integer[] {}, new Integer[] { 2, 4, 6 }, (x, y) -> MergeResult.TAKE_FIRST).toList());
        assertEquals(Arrays.asList(1, 1, 2, 2, 3, 3, 4, 4, 5, 6),
                Seq.merge(new Integer[] { 1, 1, 3, 3, 5 }, new Integer[] { 2, 2, 4, 4, 6 }, SeqMergeTest::takeSmaller).toList());
    }

    @Test
    public void testMerge_ThreeArrays() throws Exception {
        assertEquals(
                Arrays.asList(1, 2, 3, 4, 5, 6, 7), Seq
                        .merge(new Integer[] { 1, 5 }, new Integer[] { 2, 6 }, new Integer[] { 3, 4, 7 },
                                (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toList());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9),
                Seq.merge(new Integer[] { 1, 4, 7 }, new Integer[] { 2, 5, 8 }, new Integer[] { 3, 6, 9 }, SeqMergeTest::takeSmaller).toList());
    }

    @Test
    public void testMerge_Iterables() throws Exception {
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), Seq.merge(Arrays.asList(1, 3, 5), Arrays.asList(2, 4, 6), SeqMergeTest::takeSmaller).toList());
        assertEquals(
                Arrays.asList("apple", "banana", "cherry", "date", "fig", "grape"), Seq
                        .merge(Arrays.asList("apple", "cherry", "grape"), Arrays.asList("banana", "date", "fig"),
                                (a, b) -> a.compareTo(b) <= 0 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toList());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7),
                Seq.merge(Arrays.asList(1, 5), Arrays.asList(2, 6), Arrays.asList(3, 4, 7), (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toList());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6),
                Seq.merge(Arrays.asList(1, 4), Arrays.asList(2, 5), Arrays.asList(3, 6), SeqMergeTest::takeSmaller).toList());
    }

    @Test
    public void testMerge_Iterators() throws Exception {
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6),
                Seq.merge(Arrays.asList(1, 3, 5).iterator(), Arrays.asList(2, 4, 6).iterator(), SeqMergeTest::takeSmaller).toList());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8),
                Seq.merge(Arrays.asList(1, 3, 5, 7).iterator(), Arrays.asList(2, 4, 6, 8).iterator(), SeqMergeTest::takeSmaller).toList());
        Iterator<Integer> a = Arrays.asList(1, 5).iterator();
        Iterator<Integer> b = Arrays.asList(2, 6).iterator();
        Iterator<Integer> c = Arrays.asList(3, 4, 7).iterator();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7), Seq.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9),
                Seq.merge(Arrays.asList(1, 4, 7).iterator(), Arrays.asList(2, 5, 8).iterator(), Arrays.asList(3, 6, 9).iterator(), SeqMergeTest::takeSmaller)
                        .toList());
    }

    @Test
    public void testMerge_Seqs() throws Exception {
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6),
                Seq.merge(Seq.of(1, 3, 5), Seq.of(2, 4, 6), (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7),
                Seq.merge(Seq.of(1, 5), Seq.of(2, 6), Seq.of(3, 4, 7), (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), Seq.merge(Seq.of(1, 4), Seq.of(2, 5), Seq.of(3, 6), SeqMergeTest::takeSmaller).toList());
    }

    @Test
    public void testMergeWith() throws Exception {
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6),
                Seq.of(1, 3, 5).mergeWith(Arrays.asList(2, 4, 6), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6),
                Seq.of(1, 3, 5).mergeWith(Seq.of(2, 4, 6), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());

        List<String> skipped = Seq.<String, RuntimeException> of("a", "c", "e")
                .mergeWith(Arrays.asList("b", "d"), (x, y) -> x.compareTo(y) <= 0 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .skip(2)
                .toList();
        assertEquals(Arrays.asList("c", "d", "e"), skipped);
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "c", "e")
                        .mergeWith(Arrays.asList("b", "d"), (x, y) -> x.compareTo(y) <= 0 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toArray(String[]::new));
    }
}
