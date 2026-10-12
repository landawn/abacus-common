package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.u.OptionalInt;

/** IntStream combination contracts. */
public class IntStreamCombinationTest extends IntStreamTestSupport {

    // append

    @Test
    public void testAppend_BasicAppend() {
        stream = IntStream.of(1, 2, 3).append(4, 5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testAppend_EmptyArray() {
        stream = IntStream.of(1, 2, 3).append();
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void append_shouldAppendElements() {
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, IntStream.of(1, 2).append(3, 4).toArray());
    }

    @Test
    public void testAppendPrepend() {
        stream = createIntStream(1, 2, 3).append(4, 5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, stream.toArray());

        stream = createIntStream(3, 4, 5).prepend(1, 2);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testAppendPrependWithOptional() {
        IntStream stream = createIntStream(2, 3).prepend(OptionalInt.of(1));
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());

        stream = createIntStream(1, 2).append(OptionalInt.of(3));
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());

        stream = createIntStream(1, 2).append(OptionalInt.empty());
        assertArrayEquals(new int[] { 1, 2 }, stream.toArray());
    }

    // appendIfEmpty

    @Test
    public void testAppendIfEmpty() {
        IntStream stream = createIntStream(1, 2, 3).appendIfEmpty(4, 5, 6);
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());

        stream = IntStream.empty().appendIfEmpty(4, 5, 6);
        assertArrayEquals(new int[] { 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testAppendIfEmpty_EmptyStream() {
        stream = IntStream.empty().appendIfEmpty(1, 2, 3);
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testAppendIfEmpty_NonEmptyStream() {
        stream = IntStream.of(5, 6).appendIfEmpty(1, 2, 3);
        assertArrayEquals(new int[] { 5, 6 }, stream.toArray());
    }

    @Test
    public void testAppendIfEmptyNullSupplierResultAppendsNothing() {
        // Matches IntStream.defer(() -> null) and the object streams: a null fallback appends nothing.
        assertArrayEquals(new int[0], IntStream.of(new int[0]).appendIfEmpty(() -> null).toArray());
        assertArrayEquals(new int[0], IntStream.of(IntIterator.empty()).appendIfEmpty(() -> null).toArray());
        assertArrayEquals(new int[] { 1 }, IntStream.of(1).appendIfEmpty(() -> null).toArray());
    }

    // concat

    @Test
    public void testConcat() {
        IntStream s1 = createIntStream(1, 2, 3);
        IntStream s2 = createIntStream(4, 5, 6);
        stream = IntStream.concat(s1, s2);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void test_concat_02() {
        int[] a = { 1, 2, 3 };
        int[] b = { 4, 5, 6 };
        int[] result = IntStream.concat(a, b).toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, result);

        result = IntStream.concat(N.toList(a, b)).toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, result);

        result = IntStream.concatIterators(N.toList(IntIterator.of(a), IntIterator.of(b))).toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, result);
    }

    @Test
    public void concat_shouldConcatenateStreams() {
        IntStream a = IntStream.of(1, 2);
        IntStream b = IntStream.of(3, 4);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, IntStream.concat(a, b).toArray());
    }

    @Test
    public void testConcat_WithArrays() {
        stream = IntStream.concat(new int[] { 1, 2 }, new int[] { 3, 4 }, new int[] { 5, 6 });
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testConcat_WithIterators() {
        IntIterator iter1 = IntIterator.of(1, 2);
        IntIterator iter2 = IntIterator.of(3, 4);
        stream = IntStream.concat(iter1, iter2);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testConcat_WithList() {
        List<int[]> list = Arrays.asList(new int[] { 1, 2 }, new int[] { 3, 4 });
        stream = IntStream.concat(list);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testConcat_WithStreamCollection() {
        Collection<IntStream> streams = Arrays.asList(IntStream.of(1, 2), IntStream.of(3, 4));
        stream = IntStream.concat(streams);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testConcat_WithStreams() {
        IntStream s1 = IntStream.of(1, 2);
        IntStream s2 = IntStream.of(3, 4);
        stream = IntStream.concat(s1, s2);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testConcatCollectionSnapshotsSources() {
        List<IntStream> sources = new ArrayList<>(Arrays.asList(IntStream.of(1, 2), IntStream.of(3, 4)));
        stream = IntStream.concat(sources);

        sources.clear();

        assertArrayEquals(new int[] { 1, 2, 3, 4 }, stream.toArray());
    }



    // G097-05: bulk paths after partial consumption (skip inside a segment, at a segment end, beyond the end)
    @Test
    public void testConcatListOfArrays_bulkOpsAfterSkip() {
        final List<int[]> list = Arrays.asList(new int[] { 1, 2 }, null, new int[] { 3 }, new int[0], new int[] { 4, 5, 6 });

        for (int n = 0; n <= 8; n++) {
            final int[] expected = new int[Math.max(0, 6 - n)];
            for (int i = 0; i < expected.length; i++) {
                expected[i] = n + i + 1;
            }

            assertArrayEquals(expected, IntStream.concat(list).skip(n).toArray(), "skip " + n);
            assertEquals(expected.length, IntStream.concat(list).skip(n).count(), "skip " + n);
            assertArrayEquals(expected, IntStream.concat(list).skip(n).toIntList().toArray(), "skip " + n);
        }

        final IntIterator iter = IntStream.concat(list).iterator();
        assertEquals(1, iter.nextInt());
        assertArrayEquals(new int[] { 2, 3, 4, 5, 6 }, iter.toArray());
        assertFalse(iter.hasNext());
    }



    // G097-05: long segments take the bulk-copy path (whole segments and a partially consumed head segment)
    @Test
    public void testConcatListOfArrays_longSegmentsBulkPath() {
        final int[] a = Array.range(0, 20);
        final int[] b = Array.range(100, 140);
        final int[] c = { -1, -2, -3 };
        final List<int[]> list = Arrays.asList(a, null, new int[0], c, b, c);
        final int[] all = new int[66];
        System.arraycopy(a, 0, all, 0, 20);
        System.arraycopy(c, 0, all, 20, 3);
        System.arraycopy(b, 0, all, 23, 40);
        System.arraycopy(c, 0, all, 63, 3);

        for (final int n : new int[] { 0, 1, 4, 5, 19, 20, 21, 23, 24, 27, 30, 62, 63, 64, 66, 70 }) {
            final int[] expected = Arrays.copyOfRange(all, Math.min(n, all.length), all.length);
            assertArrayEquals(expected, IntStream.concat(list).skip(n).toArray(), "skip " + n);
            assertEquals(expected.length, IntStream.concat(list).skip(n).count(), "skip " + n);
            final IntList il = IntStream.concat(list).skip(n).toIntList();
            assertArrayEquals(expected, il.toArray(), "skip " + n);
            il.add(1);
            assertEquals(expected.length + 1, il.size());
        }

        final int[] result = IntStream.concat(list).toArray();
        assertArrayEquals(all, result);
        result[0] = 99;
        assertEquals(0, a[0]);
        final int[] result2 = IntStream.concat(Arrays.asList(b)).toArray();
        assertArrayEquals(b, result2);
        assertFalse(result2 == b);
        assertEquals(100, IntStream.concat(b, a).toArray()[0]);
    }



    // G097-05: concat(List<int[]>) bulk toArray()/toIntList()/count(); pins null/empty segments and fresh results.
    @Test
    public void testConcatListOfArrays_toArrayCountToList_edgeCases() {
        final int[] a = { 1, Integer.MIN_VALUE, Integer.MAX_VALUE };
        final int[] b = {};
        final int[] c = { -7, 5 };
        final List<int[]> list = Arrays.asList(null, a, b, null, c, b);
        final int[] expected = { 1, Integer.MIN_VALUE, Integer.MAX_VALUE, -7, 5 };

        assertArrayEquals(expected, IntStream.concat(list).toArray());
        assertEquals(5, IntStream.concat(list).count());
        assertArrayEquals(expected, IntStream.concat(list).toIntList().toArray());

        final int[] arr = IntStream.concat(list).toArray();
        arr[0] = 42;
        assertEquals(1, a[0]);
        final IntList il = IntStream.concat(list).toIntList();
        il.add(7);
        assertEquals(6, il.size());
        assertEquals(1, a[0]);

        final List<int[]> empties = Arrays.asList(b, null, new int[0]);
        assertEquals(0, IntStream.concat(empties).toArray().length);
        assertEquals(0, IntStream.concat(empties).count());
        final IntList emptyList = IntStream.concat(empties).toIntList();
        assertEquals(0, emptyList.size());
        emptyList.add(1);
        assertEquals(1, emptyList.size());

        assertArrayEquals(new int[] { 3, 4 }, IntStream.concat(Arrays.asList(new int[][] { { 3, 4 } })).toArray());
        assertArrayEquals(new int[] { 3, 4, 5 }, IntStream.concat(new int[] { 3 }, null, new int[] { 4, 5 }).toArray());
    }

    // concatIterators

    @Test
    public void testConcatIterators_Collection() {
        List<IntIterator> iters = new ArrayList<>();
        iters.add(IntIterator.of(new int[] { 1, 2 }));
        iters.add(IntIterator.of(new int[] { 3, 4 }));
        int[] result = IntStream.concatIterators(iters).toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testConcatIterators_EmptyCollection() {
        List<IntIterator> iters = new ArrayList<>();
        int[] result = IntStream.concatIterators(iters).toArray();
        assertArrayEquals(new int[0], result);
    }

    @Test
    public void testConcatIterators_SingleIterator() {
        java.util.List<IntIterator> iters = java.util.Arrays.asList(IntIterator.of(10, 20, 30));
        int[] result = IntStream.concatIterators(iters).toArray();
        assertArrayEquals(new int[] { 10, 20, 30 }, result);
    }



    // concatIterators(Collection) - collection of IntIterators
    @Test
    public void testConcatIterators_ThreeIterators() {
        java.util.List<IntIterator> iters = java.util.Arrays.asList(IntIterator.of(1, 2), IntIterator.of(3), IntIterator.of(4, 5, 6));
        int[] result = IntStream.concatIterators(iters).toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, result);
    }

    @Test
    public void testConcatIteratorsCollection() {
        List<IntIterator> iterators = Arrays.asList(IntIterator.of(new int[] { 1, 2 }), IntIterator.of(new int[] { 3, 4 }));
        IntStream result = IntStream.concatIterators(iterators);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, result.toArray());
    }

    // join

    @Test
    public void testJoin() {
        String joined = createIntStream(1, 2, 3).join(", ");
        assertEquals("1, 2, 3", joined);

        joined = createIntStream(1, 2, 3).join(", ", "[", "]");
        assertEquals("[1, 2, 3]", joined);
    }

    @Test
    public void testJoin_WithDelimiter() {
        String result = IntStream.of(1, 2, 3).join(", ");
        assertEquals("1, 2, 3", result);
    }

    @Test
    public void testJoin_WithDelimiterAndPrefixSuffix() {
        String result = IntStream.of(1, 2, 3).join(", ", "[", "]");
        assertEquals("[1, 2, 3]", result);
    }

    // merge

    @Test
    public void test_merge() {
        {
            int[] a = { 1, 2, 3 };
            int[] b = { 4, 5, 6 };
            int[] result = IntStream.merge(a, b, (p1, p2) -> p1 <= p2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
            assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, result);
        }

        {
            int[] a = { 1, 2, 3 };
            int[] b = { 4, 5, 6 };
            int[] c = { 7, 8, 9 };
            int[] result = IntStream.merge(a, b, c, (p1, p2) -> p1 <= p2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
            assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, result);
        }
        {
            int[] a = { 1, 2, 3 };
            int[] b = { 4, 5, 6 };
            int[] result = IntStream.merge(N.toList(IntStream.of(a), IntStream.of(b)), (p1, p2) -> p1 <= p2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                    .toArray();
            assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, result);
        }

        {
            int[] a = { 1, 2, 3 };
            int[] b = { 4, 5, 6 };
            int[] c = { 7, 8, 9 };
            int[] result = IntStream
                    .merge(N.toList(IntStream.of(a), IntStream.of(b), IntStream.of(c)), (p1, p2) -> p1 <= p2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                    .toArray();
            assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, result);
        }
        {
            int[] a = { 1, 2, 3 };
            int[] b = { 4, 5, 6 };
            int[] c = { 7, 8, 9 };
            int[] result = IntStream.merge(N.toList(IntStream.of(a)), (p1, p2) -> p1 <= p2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
            assertArrayEquals(new int[] { 1, 2, 3 }, result);

            result = IntStream.merge(N.toList(IntStream.of(a), IntStream.of(b)), (p1, p2) -> p1 <= p2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                    .toArray();
            assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, result);

            result = IntStream
                    .merge(N.toList(IntStream.of(a), IntStream.of(b), IntStream.of(c)), (p1, p2) -> p1 <= p2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                    .toArray();
            assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, result);
        }

    }

    @Test
    public void testMerge_CollectionOfStreams() {
        List<IntStream> streams = Arrays.asList(IntStream.of(1, 3, 5), IntStream.of(2, 4, 6));
        int[] result = IntStream.merge(streams, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, result);
    }

    @Test
    public void testMerge_ThreeArrays() {
        int[] result = IntStream
                .merge(new int[] { 1, 4 }, new int[] { 2, 5 }, new int[] { 3, 6 }, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, result);
    }

    @Test
    public void testMerge_ThreeIterators() {
        int[] result = IntStream
                .merge(IntIterator.of(new int[] { 1, 4 }), IntIterator.of(new int[] { 2, 5 }), IntIterator.of(new int[] { 3, 6 }),
                        (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, result);
    }

    @Test
    public void testMerge_WithArrays() {
        stream = IntStream.merge(new int[] { 1, 3, 5 }, new int[] { 2, 4, 6 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testMerge_WithStreams() {
        IntStream s1 = IntStream.of(1, 3, 5);
        IntStream s2 = IntStream.of(2, 4, 6);
        stream = IntStream.merge(s1, s2, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testMergeCollectionTreatsNullStreamAsEmpty() {
        assertEquals(0, IntStream.merge(Arrays.asList((IntStream) null), (a, b) -> MergeResult.TAKE_FIRST).count());
    }

    @Test
    public void testMergeIteratorThreeWay() {
        IntIterator a = IntIterator.of(new int[] { 1, 4, 7 });
        IntIterator b = IntIterator.of(new int[] { 2, 5, 8 });
        IntIterator c = IntIterator.of(new int[] { 3, 6, 9 });
        int[] result = IntStream.merge(a, b, c, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, result);
    }

    @Test
    public void testMergeRetriesPeekedHeadsAfterSelectorThrow() {
        final AtomicInteger calls = new AtomicInteger();
        final IntIterator iter = IntStream.merge(IntIterator.of(1, 3), IntIterator.of(2, 4), (x, y) -> {
            if (calls.getAndIncrement() == 0) {
                throw new IllegalStateException("selector failed");
            }
            return x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        }).iterator();

        assertThrows(IllegalStateException.class, iter::nextInt);
        assertEquals(1, iter.nextInt());
        assertEquals(2, iter.nextInt());
        assertEquals(3, iter.nextInt());
        assertEquals(4, iter.nextInt());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testMergeThreeArraysAdditional() {
        int[] a1 = { 1, 7 };
        int[] a2 = { 3, 8 };
        int[] a3 = { 5, 9 };
        stream = IntStream.merge(a1, a2, a3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(6, stream.count());
    }

    @Test
    public void testMergeThreeStreamsAdditional() {
        IntStream s1 = IntStream.of(1, 7);
        IntStream s2 = IntStream.of(3, 8);
        IntStream s3 = IntStream.of(5, 9);
        stream = IntStream.merge(s1, s2, s3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(6, stream.count());
    }

    @Test
    public void testMergeWithCollection() {
        Collection<IntStream> streams = Arrays.asList(IntStream.of(1, 5), IntStream.of(2, 6), IntStream.of(3, 7));
        stream = IntStream.merge(streams, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(6, stream.count());
    }

    @Test
    public void testMergeWith() {
        IntStream s1 = createIntStream(1, 3, 5);
        IntStream s2 = createIntStream(2, 4, 6);
        stream = s1.mergeWith(s2, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testMergeWith_BasicMerge() {
        stream = IntStream.of(1, 3, 5).mergeWith(IntStream.of(2, 4, 6), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    // prepend

    @Test
    public void testPrepend_BasicPrepend() {
        stream = IntStream.of(3, 4, 5).prepend(1, 2);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testPrepend_EmptyArray() {
        stream = IntStream.of(1, 2, 3).prepend();
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void prepend_shouldPrependElements() {
        assertArrayEquals(new int[] { 3, 4, 1, 2 }, IntStream.of(1, 2).prepend(3, 4).toArray());
    }

    // zip

    @Test
    public void testZip() {
        IntStream s1 = createIntStream(1, 2, 3);
        IntStream s2 = createIntStream(10, 20, 30);
        stream = IntStream.zip(s1, s2, Integer::sum);
        assertArrayEquals(new int[] { 11, 22, 33 }, stream.toArray());
    }

    @Test
    public void test_zip_02() {
        {
            int[] a = { 1, 2, 3 };
            int[] b = { 4, 5, 6 };
            int[] result = IntStream.zip(a, b, (x, y) -> x + y).toArray();
            assertArrayEquals(new int[] { 5, 7, 9 }, result);
        }

        {
            int[] a = { 1, 2 };
            int[] b = { 4, 5, 6 };
            int[] result = IntStream.zip(a, b, 100, 200, (x, y) -> x + y).toArray();
            assertArrayEquals(new int[] { 5, 7, 106 }, result);
        }
        {
            int[] a = { 1, 2, 3 };
            int[] b = { 4, 5, 6 };
            int[] c = { 7, 8, 9 };
            int[] result = IntStream.zip(a, b, c, (x, y, z) -> x + y + z).toArray();
            assertArrayEquals(new int[] { 12, 15, 18 }, result);
        }
        {
            int[] a = { 1 };
            int[] b = { 4, 5 };
            int[] c = { 7, 8, 9 };
            int[] result = IntStream.zip(a, b, c, 100, 200, 300, (x, y, z) -> x + y + z).toArray();
            assertArrayEquals(new int[] { 12, 113, 309 }, result);
        }
        {
            int[] a = { 1, 2, 3 };
            int[] b = { 4, 5, 6 };
            int[] c = { 7, 8, 9 };
            int[] result = IntStream.zip(N.toList(IntStream.of(a), IntStream.of(b), IntStream.of(c)), it -> it[0] + it[1] + it[2]).toArray();
            assertArrayEquals(new int[] { 12, 15, 18 }, result);
        }
    }

    @Test
    public void zip_shouldZipStreams() {
        IntStream a = IntStream.of(1, 2, 3);
        IntStream b = IntStream.of(4, 5, 6);
        IntStream zipped = IntStream.zip(a, b, (x, y) -> x + y);
        assertArrayEquals(new int[] { 5, 7, 9 }, zipped.toArray());
    }



    // zip(int[], int[], int[], int, int, int, IntTernaryOperator) - array ternary with defaults
    @Test
    public void testZip_ThreeArrays_WithDefaults() {
        int[] a = { 1, 2 };
        int[] b = { 10, 20, 30 };
        int[] c = { 100, 200, 300, 400 };
        int[] result = IntStream.zip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new int[] { 111, 222, 330, 400 }, result);
    }

    @Test
    public void testZip_ThreeArrays_WithDefaults_AllSameLength() {
        int[] a = { 1, 2, 3 };
        int[] b = { 4, 5, 6 };
        int[] c = { 7, 8, 9 };
        int[] result = IntStream.zip(a, b, c, -1, -1, -1, (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new int[] { 12, 15, 18 }, result);
    }



    // zip(IntIterator, IntIterator, IntIterator, int, int, int, IntTernaryOperator) - iterator ternary with defaults
    @Test
    public void testZip_ThreeIterators_WithDefaults() {
        IntIterator a = IntIterator.of(1);
        IntIterator b = IntIterator.of(10, 20);
        IntIterator c = IntIterator.of(100, 200, 300);
        int[] result = IntStream.zip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new int[] { 111, 220, 300 }, result);
    }

    @Test
    public void testZip_ThreeStreams_AllEmpty() {
        int[] result = IntStream.zip(IntStream.empty(), IntStream.empty(), IntStream.empty(), (a, b, c) -> a + b + c).toArray();
        assertArrayEquals(new int[0], result);
    }



    // zip(IntStream, IntStream, IntStream, IntTernaryOperator) - three-stream zip
    @Test
    public void testZip_ThreeStreams_TernaryOp() {
        int[] result = IntStream.zip(IntStream.of(1, 2, 3), IntStream.of(10, 20, 30), IntStream.of(100, 200, 300), (a, b, c) -> a + b + c).toArray();
        assertArrayEquals(new int[] { 111, 222, 333 }, result);
    }



    // zip(IntStream, IntStream, IntStream, int, int, int, IntTernaryOperator) - three-stream with defaults
    @Test
    public void testZip_ThreeStreams_WithDefaults() {
        int[] result = IntStream.zip(IntStream.of(1), IntStream.of(10, 20), IntStream.of(100, 200, 300), 0, 0, 0, (a, b, c) -> a + b + c).toArray();
        assertArrayEquals(new int[] { 111, 220, 300 }, result);
    }

    @Test
    public void testZip_ThreeStreams_WithDefaults_AllSameLength() {
        int[] result = IntStream.zip(IntStream.of(1, 2), IntStream.of(3, 4), IntStream.of(5, 6), -1, -1, -1, (a, b, c) -> a + b + c).toArray();
        assertArrayEquals(new int[] { 9, 12 }, result);
    }



    // zip(IntIterator, IntIterator, int, int, IntBinaryOperator) - iterator binary with defaults
    @Test
    public void testZip_TwoIterators_WithDefaults() {
        IntIterator a = IntIterator.of(1, 2, 3);
        IntIterator b = IntIterator.of(10, 20);
        int[] result = IntStream.zip(a, b, 0, 99, (x, y) -> x + y).toArray();
        assertArrayEquals(new int[] { 11, 22, 102 }, result);
    }



    // zip(IntStream, IntStream, IntBinaryOperator) - stream-based binary zip
    @Test
    public void testZip_TwoStreams_BinaryOp() {
        int[] result = IntStream.zip(IntStream.of(1, 2, 3), IntStream.of(10, 20, 30), (a, b) -> a + b).toArray();
        assertArrayEquals(new int[] { 11, 22, 33 }, result);
    }

    @Test
    public void testZip_TwoStreams_ShorterFirst() {
        int[] result = IntStream.zip(IntStream.of(1, 2), IntStream.of(10, 20, 30), (a, b) -> a * b).toArray();
        assertArrayEquals(new int[] { 10, 40 }, result);
    }

    @Test
    public void testZip_TwoStreams_WithDefaults_LongerFirst() {
        int[] result = IntStream.zip(IntStream.of(1, 2, 3), IntStream.of(10), 0, 99, (a, b) -> a + b).toArray();
        assertArrayEquals(new int[] { 11, 101, 102 }, result);
    }



    // zip(IntStream, IntStream, int, int, IntBinaryOperator) - binary zip with defaults
    @Test
    public void testZip_TwoStreams_WithDefaults_LongerSecond() {
        int[] result = IntStream.zip(IntStream.of(1, 2), IntStream.of(10, 20, 30), 0, 0, (a, b) -> a + b).toArray();
        assertArrayEquals(new int[] { 11, 22, 30 }, result);
    }

    @Test
    public void testZip_WithArrays() {
        stream = IntStream.zip(new int[] { 1, 2, 3 }, new int[] { 4, 5, 6 }, (a, b) -> a + b);
        assertArrayEquals(new int[] { 5, 7, 9 }, stream.toArray());
    }

    @Test
    public void testZip_WithArraysThreeWay() {
        stream = IntStream.zip(new int[] { 1, 2 }, new int[] { 3, 4 }, new int[] { 5, 6 }, (a, b, c) -> a + b + c);
        assertArrayEquals(new int[] { 9, 12 }, stream.toArray());
    }

    @Test
    public void testZip_WithDefaultValues() {
        stream = IntStream.zip(new int[] { 1, 2 }, new int[] { 3, 4, 5 }, 0, 10, (a, b) -> a + b);
        assertArrayEquals(new int[] { 4, 6, 5 }, stream.toArray());
    }

    @Test
    public void testZip_WithIterators() {
        IntIterator iter1 = IntIterator.of(1, 2, 3);
        IntIterator iter2 = IntIterator.of(4, 5, 6);
        stream = IntStream.zip(iter1, iter2, (a, b) -> a + b);
        assertArrayEquals(new int[] { 5, 7, 9 }, stream.toArray());
    }

    @Test
    public void testZip_WithStreams() {
        IntStream s1 = IntStream.of(1, 2, 3);
        IntStream s2 = IntStream.of(4, 5, 6);
        stream = IntStream.zip(s1, s2, (a, b) -> a + b);
        assertArrayEquals(new int[] { 5, 7, 9 }, stream.toArray());
    }

    @Test
    public void testZipIteratorsThreeWay() {
        IntIterator a = IntIterator.of(new int[] { 1, 2 });
        IntIterator b = IntIterator.of(new int[] { 10, 20 });
        IntIterator c = IntIterator.of(new int[] { 100, 200 });
        int[] result = IntStream.zip(a, b, c, (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new int[] { 111, 222 }, result);
    }

    @Test
    public void testZipIteratorsThreeWayWithDefaults() {
        IntIterator a = IntIterator.of(new int[] { 1 });
        IntIterator b = IntIterator.of(new int[] { 10, 20 });
        IntIterator c = IntIterator.of(new int[] { 100, 200, 300 });
        int[] result = IntStream.zip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new int[] { 111, 220, 300 }, result);
    }

    @Test
    public void testZipIteratorsWithDefaults() {
        IntIterator a = IntIterator.of(new int[] { 1, 2, 3 });
        IntIterator b = IntIterator.of(new int[] { 10, 20 });
        int[] result = IntStream.zip(a, b, 0, 0, Integer::sum).toArray();
        assertArrayEquals(new int[] { 11, 22, 3 }, result);
    }

    @Test
    public void testZipStreamsThreeWayWithDefaults() {
        int[] result = IntStream.zip(IntStream.of(1), IntStream.of(10, 20), IntStream.of(100, 200, 300), 0, 0, 0, (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new int[] { 111, 220, 300 }, result);
    }

    @Test
    public void testZipStreamsWithDefaults() {
        int[] result = IntStream.zip(IntStream.of(1, 2, 3), IntStream.of(10, 20), 0, 0, Integer::sum).toArray();
        assertArrayEquals(new int[] { 11, 22, 3 }, result);
    }

    @Test
    public void testZipThreeArraysWithDefaultsAdditional() {
        int[] a = { 1 };
        int[] b = { 2, 3 };
        int[] c = { 4, 5, 6 };
        stream = IntStream.zip(a, b, c, 100, 200, 300, (x, y, z) -> x + y + z);
        int[] result = stream.toArray();
        assertEquals(3, result.length);
    }

    @Test
    public void testZipThreeIteratorsWithDefaultsAdditional() {
        IntIterator a = IntIterator.of(new int[] { 1 });
        IntIterator b = IntIterator.of(new int[] { 2, 3 });
        IntIterator c = IntIterator.of(new int[] { 4, 5, 6 });
        stream = IntStream.zip(a, b, c, 100, 200, 300, (x, y, z) -> x + y + z);
        assertEquals(3, stream.count());
    }

    @Test
    public void testZipThreeStreamsWithDefaultsAdditional() {
        IntStream a = IntStream.of(1);
        IntStream b = IntStream.of(2, 3);
        IntStream c = IntStream.of(4, 5, 6);
        stream = IntStream.zip(a, b, c, 100, 200, 300, (x, y, z) -> x + y + z);
        int[] result = stream.toArray();
        assertEquals(3, result.length);
    }

    @Test
    public void testZipWith_CollectionOfStreams() {
        List<IntStream> streams = Arrays.asList(IntStream.of(1, 2, 3), IntStream.of(10, 20, 30));
        int[] result = IntStream.zip(streams, args -> args[0] + args[1]).toArray();
        assertArrayEquals(new int[] { 11, 22, 33 }, result);
    }

    @Test
    public void testZipWith_CollectionOfStreamsWithDefaults() {
        List<IntStream> streams = Arrays.asList(IntStream.of(1, 2), IntStream.of(10, 20, 30));
        int[] result = IntStream.zip(streams, new int[] { 0, 0 }, args -> args[0] + args[1]).toArray();
        assertArrayEquals(new int[] { 11, 22, 30 }, result);
    }

    @Test
    public void testZipWithCollectionDefaultValues() {
        Collection<IntStream> streams = Arrays.asList(IntStream.of(1, 2), IntStream.of(10));
        int[] defaults = new int[] { 0, 0 };
        stream = IntStream.zip(streams, defaults, ints -> {
            int sum = 0;
            for (Integer i : ints) {
                sum += i;
            }
            return sum;
        });
        assertEquals(2, stream.count());
    }



    // Additional coverage tests for 2025
    @Test
    public void testZipWithCollectionOfStreams() {
        Collection<IntStream> streams = Arrays.asList(IntStream.of(1, 2), IntStream.of(10, 20), IntStream.of(100, 200));
        stream = IntStream.zip(streams, ints -> {
            int sum = 0;
            for (Integer i : ints) {
                sum += i;
            }
            return sum;
        });
        assertArrayEquals(new int[] { 111, 222 }, stream.toArray());
    }

    @Test
    public void testZipThreeStreams() {
        IntStream s1 = createIntStream(1, 2, 3);
        IntStream s2 = createIntStream(10, 20, 30);
        IntStream s3 = createIntStream(100, 200, 300);
        IntStream zipped = s1.zipWith(s2, s3, (a, b, c) -> a + b + c);

        assertArrayEquals(new int[] { 111, 222, 333 }, zipped.toArray());
    }

    // zipWith

    @Test
    public void testZipWith_BasicZip() {
        stream = IntStream.of(1, 2, 3).zipWith(IntStream.of(4, 5, 6), (a, b) -> a + b);
        assertArrayEquals(new int[] { 5, 7, 9 }, stream.toArray());
    }

    @Test
    public void testZipWith_ThreeWay() {
        stream = IntStream.of(1, 2).zipWith(IntStream.of(3, 4), IntStream.of(5, 6), (a, b, c) -> a + b + c);
        assertArrayEquals(new int[] { 9, 12 }, stream.toArray());
    }

    @Test
    public void testZipWith_ThreeWayWithDefaults() {
        stream = IntStream.of(1, 2).zipWith(IntStream.of(3, 4, 5), IntStream.of(6, 7), 0, 10, 20, (a, b, c) -> a + b + c);
        assertArrayEquals(new int[] { 10, 13, 25 }, stream.toArray());
    }

    @Test
    public void testZipWith_WithDefaultValues() {
        stream = IntStream.of(1, 2).zipWith(IntStream.of(3, 4, 5), 0, 10, (a, b) -> a + b);
        assertArrayEquals(new int[] { 4, 6, 5 }, stream.toArray());
    }

    @Test
    public void testZipWithDefault() {
        IntStream s1 = createIntStream(1, 2, 3);
        IntStream s2 = createIntStream(10, 20);
        IntStream zipped = s1.zipWith(s2, 0, 100, Integer::sum);

        assertArrayEquals(new int[] { 11, 22, 103 }, zipped.toArray());
    }

}
