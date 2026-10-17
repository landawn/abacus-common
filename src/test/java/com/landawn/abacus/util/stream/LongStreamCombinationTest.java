package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.LongList;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.function.LongBiFunction;
import com.landawn.abacus.util.function.LongNFunction;
import com.landawn.abacus.util.u.OptionalLong;

/** LongStream combination contracts. */
public class LongStreamCombinationTest extends LongStreamTestSupport {

    // append

    @Test
    public void testAppend() {
        LongStream stream = LongStream.of(1L, 2L, 3L);
        long[] result = stream.append(4L, 5L).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, result);
    }

    @Test
    public void testAppendOptionalLong() {
        long[] result1 = createLongStream(1L, 2L).append(OptionalLong.of(3L)).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result1);

        long[] result2 = createLongStream(1L, 2L).append(OptionalLong.empty()).toArray();
        assertArrayEquals(new long[] { 1L, 2L }, result2);
    }

    @Test
    public void testAppendStream() {
        LongStream s1 = LongStream.of(1L, 2L);
        LongStream s2 = LongStream.of(3L, 4L);
        long[] result = s1.append(s2).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, result);
    }

    @Test
    public void testAppendIfEmpty() {
        LongStream stream1 = LongStream.empty();
        long[] result1 = stream1.appendIfEmpty(1L, 2L, 3L).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result1);

        LongStream stream2 = LongStream.of(10L, 20L);
        long[] result2 = stream2.appendIfEmpty(1L, 2L, 3L).toArray();
        assertArrayEquals(new long[] { 10L, 20L }, result2);
    }

    // concat

    @Test
    public void testConcat() {
        long[] a1 = { 1L, 2L };
        long[] a2 = { 3L, 4L };
        long[] a3 = { 5L, 6L };

        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L }, LongStream.concat(a1, a2, a3).toArray());
    }

    @Test
    public void test_concat_02() {
        long[] a = { 1, 2, 3 };
        long[] b = { 4, 5, 6 };
        long[] result = LongStream.concat(a, b).toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6 }, result);

        result = LongStream.concat(N.toList(a, b)).toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6 }, result);

        result = LongStream.concatIterators(N.toList(LongIterator.of(a), LongIterator.of(b))).toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6 }, result);
    }

    @Test
    public void testConcatArrayList() {
        List<long[]> arrays = Arrays.asList(new long[] { 1L, 2L }, new long[] { 3L, 4L }, new long[] { 5L, 6L });

        long[] result = LongStream.concat(arrays).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L }, result);
    }

    @Test
    public void testConcatArrays() {
        assertEquals(9, LongStream.concat(new long[] { 1, 2, 3 }, new long[] { 4, 5 }, new long[] { 6, 7, 8, 9 }).count());
        assertEquals(8, LongStream.concat(new long[] { 1, 2, 3 }, new long[] { 4, 5 }, new long[] { 6, 7, 8, 9 }).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 },
                LongStream.concat(new long[] { 1, 2, 3 }, new long[] { 4, 5 }, new long[] { 6, 7, 8, 9 }).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5, 6, 7, 8, 9 },
                LongStream.concat(new long[] { 1, 2, 3 }, new long[] { 4, 5 }, new long[] { 6, 7, 8, 9 }).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L),
                LongStream.concat(new long[] { 1, 2, 3 }, new long[] { 4, 5 }, new long[] { 6, 7, 8, 9 }).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L),
                LongStream.concat(new long[] { 1, 2, 3 }, new long[] { 4, 5 }, new long[] { 6, 7, 8, 9 }).skip(1).toList());
        assertEquals(9, LongStream.concat(new long[] { 1, 2, 3 }, new long[] { 4, 5 }, new long[] { 6, 7, 8, 9 }).map(e -> e).count());
        assertEquals(8, LongStream.concat(new long[] { 1, 2, 3 }, new long[] { 4, 5 }, new long[] { 6, 7, 8, 9 }).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 },
                LongStream.concat(new long[] { 1, 2, 3 }, new long[] { 4, 5 }, new long[] { 6, 7, 8, 9 }).map(e -> e).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5, 6, 7, 8, 9 },
                LongStream.concat(new long[] { 1, 2, 3 }, new long[] { 4, 5 }, new long[] { 6, 7, 8, 9 }).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L),
                LongStream.concat(new long[] { 1, 2, 3 }, new long[] { 4, 5 }, new long[] { 6, 7, 8, 9 }).map(e -> e).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L),
                LongStream.concat(new long[] { 1, 2, 3 }, new long[] { 4, 5 }, new long[] { 6, 7, 8, 9 }).map(e -> e).skip(1).toList());
    }

    @Test
    public void testConcatArrays_Empty() {
        assertEquals(0L, LongStream.concat(new long[0][]).count());
    }

    @Test
    public void testConcatArrays_TwoArrays() {
        long[] result = LongStream.concat(new long[] { 1L, 2L }, new long[] { 3L, 4L }).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, result);
    }

    @Test
    public void testConcatCollection_Empty() {
        assertEquals(0L, LongStream.concat(new ArrayList<LongStream>()).count());
    }

    @Test
    public void testConcatCollection_OfStreams() {
        List<LongStream> streams = Arrays.asList(LongStream.of(10L, 20L), LongStream.of(30L));
        long[] result = LongStream.concat(streams).toArray();
        assertArrayEquals(new long[] { 10L, 20L, 30L }, result);
    }

    @Test
    public void testConcatCollectionOfStreams() {
        Collection<LongStream> streams = Arrays.asList(LongStream.of(1, 2, 3), LongStream.of(4, 5), LongStream.of(6, 7, 8, 9));
        assertEquals(9, LongStream.concat(streams).count());

        streams = Arrays.asList(LongStream.of(1, 2, 3), LongStream.of(4, 5), LongStream.of(6, 7, 8, 9));
        assertEquals(8, LongStream.concat(streams).skip(1).count());

        streams = Arrays.asList(LongStream.of(1, 2, 3), LongStream.of(4, 5), LongStream.of(6, 7, 8, 9));
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, LongStream.concat(streams).toArray());

        streams = Arrays.asList(LongStream.of(1, 2, 3), LongStream.of(4, 5), LongStream.of(6, 7, 8, 9));
        assertArrayEquals(new long[] { 2, 3, 4, 5, 6, 7, 8, 9 }, LongStream.concat(streams).skip(1).toArray());

        streams = Arrays.asList(LongStream.of(1, 2, 3), LongStream.of(4, 5), LongStream.of(6, 7, 8, 9));
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L), LongStream.concat(streams).toList());

        streams = Arrays.asList(LongStream.of(1, 2, 3), LongStream.of(4, 5), LongStream.of(6, 7, 8, 9));
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L), LongStream.concat(streams).skip(1).toList());
    }

    @Test
    public void testConcatCollectionSnapshotsSources() {
        List<LongStream> sources = new ArrayList<>(Arrays.asList(LongStream.of(1L, 2L), LongStream.of(3L, 4L)));
        LongStream concatenated = LongStream.concat(sources);

        sources.clear();

        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, concatenated.toArray());
    }

    @Test
    public void testConcatEmptyArrays() {
        assertArrayEquals(new long[] {}, LongStream.concat(new long[0], new long[0]).toArray());
    }

    @Test
    public void testConcatIteratorArray() {
        LongIterator a = LongIterator.of(new long[] { 1L, 2L });
        LongIterator b = LongIterator.of(new long[] { 3L, 4L });
        long[] result = LongStream.concat(new LongIterator[] { a, b }).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, result);
    }

    @Test
    public void testConcatIteratorArray_Empty() {
        assertEquals(0L, LongStream.concat(new LongIterator[0]).count());
    }

    @Test
    public void testConcatIterators() {
        LongIterator i1 = LongIterator.of(new long[] { 1L, 2L });
        LongIterator i2 = LongIterator.of(new long[] { 3L, 4L });

        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, LongStream.concat(i1, i2).toArray());
    }

    @Test
    public void testConcatList() {
        List<long[]> list = Arrays.asList(new long[] { 1L, 2L }, new long[] { 3L, 4L });
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, LongStream.concat(list).toArray());
    }

    @Test
    public void testConcatList_Empty() {
        assertEquals(0L, LongStream.concat(new ArrayList<long[]>()).count());
    }

    @Test
    public void testConcatList_OfArrays() {
        List<long[]> listOfArrays = Arrays.asList(new long[] { 1L, 2L }, new long[] { 3L, 4L, 5L });
        long[] result = LongStream.concat(listOfArrays).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, result);
    }

    @Test
    public void testConcatListOfArrays() {
        List<long[]> list = Arrays.asList(new long[] { 1, 2, 3 }, new long[] { 4, 5 }, new long[] { 6, 7, 8, 9 });
        assertEquals(9, LongStream.concat(list).count());
        assertEquals(8, LongStream.concat(list).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, LongStream.concat(list).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5, 6, 7, 8, 9 }, LongStream.concat(list).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L), LongStream.concat(list).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L), LongStream.concat(list).skip(1).toList());
        assertEquals(9, LongStream.concat(list).map(e -> e).count());
        assertEquals(8, LongStream.concat(list).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, LongStream.concat(list).map(e -> e).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5, 6, 7, 8, 9 }, LongStream.concat(list).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L), LongStream.concat(list).map(e -> e).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L), LongStream.concat(list).map(e -> e).skip(1).toList());
    }

    // G100-06: bulk paths after partial consumption (skip inside a segment, at a segment end, beyond the end)
    @Test
    public void testConcatListOfArrays_bulkOpsAfterSkip() {
        final List<long[]> list = Arrays.asList(new long[] { 1, 2 }, null, new long[] { 3 }, new long[0], new long[] { 4, 5, 6 });

        for (int n = 0; n <= 8; n++) {
            final long[] expected = new long[Math.max(0, 6 - n)];
            for (int i = 0; i < expected.length; i++) {
                expected[i] = n + i + 1;
            }

            assertArrayEquals(expected, LongStream.concat(list).skip(n).toArray(), "skip " + n);
            assertEquals(expected.length, LongStream.concat(list).skip(n).count(), "skip " + n);
            assertArrayEquals(expected, LongStream.concat(list).skip(n).toLongList().toArray(), "skip " + n);
        }

        final LongIterator iter = LongStream.concat(list).iterator();
        assertEquals(1L, iter.nextLong());
        assertArrayEquals(new long[] { 2, 3, 4, 5, 6 }, iter.toArray());
        assertFalse(iter.hasNext());
    }

    // G100-06: long segments take the bulk-copy path (whole segments and a partially consumed head segment)
    @Test
    public void testConcatListOfArrays_longSegmentsBulkPath() {
        final long[] a = new long[20];
        final long[] b = new long[40];
        final long[] c = { -1L, -2L, -3L };
        for (int i = 0; i < a.length; i++) {
            a[i] = i;
        }
        for (int i = 0; i < b.length; i++) {
            b[i] = 100 + i;
        }
        final List<long[]> list = Arrays.asList(a, null, new long[0], c, b, c);
        final long[] all = new long[66];
        System.arraycopy(a, 0, all, 0, 20);
        System.arraycopy(c, 0, all, 20, 3);
        System.arraycopy(b, 0, all, 23, 40);
        System.arraycopy(c, 0, all, 63, 3);

        for (final int n : new int[] { 0, 1, 4, 5, 19, 20, 21, 23, 24, 27, 30, 62, 63, 64, 66, 70 }) {
            final long[] expected = Arrays.copyOfRange(all, Math.min(n, all.length), all.length);
            assertArrayEquals(expected, LongStream.concat(list).skip(n).toArray(), "skip " + n);
            assertEquals(expected.length, LongStream.concat(list).skip(n).count(), "skip " + n);
            final LongList ll = LongStream.concat(list).skip(n).toLongList();
            assertArrayEquals(expected, ll.toArray(), "skip " + n);
            ll.add(1L);
            assertEquals(expected.length + 1, ll.size());
        }

        final long[] result = LongStream.concat(list).toArray();
        assertArrayEquals(all, result);
        result[0] = 99;
        assertEquals(0L, a[0]);
        final long[] result2 = LongStream.concat(Arrays.asList(b)).toArray();
        assertArrayEquals(b, result2);
        assertFalse(result2 == b);
        assertEquals(100L, LongStream.concat(b, a).toArray()[0]);
    }

    // G100-06: concat(List<long[]>) bulk toArray/count/toLongList must equal the element-wise results
    @Test
    public void testConcatListOfArrays_toArrayCountToList_edgeCases() {
        final long[] a = { 1L, Long.MIN_VALUE, -1L };
        final long[] b = {};
        final long[] c = { Long.MAX_VALUE, 5L };
        final List<long[]> list = Arrays.asList(null, a, b, null, c, b);
        final long[] expected = { 1L, Long.MIN_VALUE, -1L, Long.MAX_VALUE, 5L };

        assertArrayEquals(expected, LongStream.concat(list).toArray());
        assertEquals(5, LongStream.concat(list).count());
        assertArrayEquals(expected, LongStream.concat(list).toLongList().toArray());

        final long[] arr = LongStream.concat(list).toArray();
        arr[0] = 42L;
        assertEquals(1L, a[0]);
        final LongList ll = LongStream.concat(list).toLongList();
        ll.add(7L);
        assertEquals(6, ll.size());
        assertEquals(1L, a[0]);

        final List<long[]> empties = Arrays.asList(b, null, new long[0]);
        assertEquals(0, LongStream.concat(empties).toArray().length);
        assertEquals(0, LongStream.concat(empties).count());
        final LongList emptyList = LongStream.concat(empties).toLongList();
        assertEquals(0, emptyList.size());
        emptyList.add(1L);
        assertEquals(1, emptyList.size());

        assertArrayEquals(new long[] { 3L, 4L }, LongStream.concat(Arrays.asList(new long[][] { { 3L, 4L } })).toArray());
    }

    @Test
    public void testConcatStreamArray() {
        LongStream s1 = LongStream.of(1L, 2L);
        LongStream s2 = LongStream.of(3L, 4L);
        long[] result = LongStream.concat(new LongStream[] { s1, s2 }).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, result);
    }

    @Test
    public void testConcatStreamArray_Empty() {
        assertEquals(0L, LongStream.concat(new LongStream[0]).count());
    }

    @Test
    public void testConcatStreamCollection() {
        Collection<LongStream> streams = Arrays.asList(LongStream.of(1L, 2L), LongStream.of(3L, 4L), LongStream.of(5L));
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, LongStream.concat(streams).toArray());
    }

    @Test
    public void testConcatStreams() {
        LongStream s1 = LongStream.of(1L, 2L);
        LongStream s2 = LongStream.of(3L, 4L);
        LongStream s3 = LongStream.of(5L, 6L);

        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L }, LongStream.concat(s1, s2, s3).toArray());
    }

    @Test
    public void testConcatWithEmptyStream() {
        LongStream s1 = LongStream.of(1, 2);
        LongStream s2 = LongStream.empty();
        assertArrayEquals(new long[] { 1, 2 }, LongStream.concat(s1, s2).toArray());
    }

    @Test
    public void testConcatIteratorCollection() {
        List<LongIterator> iterators = Arrays.asList(LongIterator.of(new long[] { 1L, 2L }), LongIterator.of(new long[] { 3L, 4L }),
                LongIterator.of(new long[] { 5L, 6L }));

        long[] result = LongStream.concatIterators(iterators).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L }, result);
    }

    // concatIterators

    @Test
    public void testConcatIterators2() {
        Collection<LongIterator> iterators = Arrays.asList(LongIterator.of(new long[] { 1, 2, 3 }), LongIterator.of(new long[] { 4, 5 }),
                LongIterator.of(new long[] { 6, 7, 8, 9 }));
        assertEquals(9, LongStream.concatIterators(iterators).count());

        iterators = Arrays.asList(LongIterator.of(new long[] { 1, 2, 3 }), LongIterator.of(new long[] { 4, 5 }), LongIterator.of(new long[] { 6, 7, 8, 9 }));
        assertEquals(8, LongStream.concatIterators(iterators).skip(1).count());

        iterators = Arrays.asList(LongIterator.of(new long[] { 1, 2, 3 }), LongIterator.of(new long[] { 4, 5 }), LongIterator.of(new long[] { 6, 7, 8, 9 }));
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, LongStream.concatIterators(iterators).toArray());
    }

    @Test
    public void testConcatIterators_Collection() {
        List<LongIterator> iters = new ArrayList<>();
        iters.add(LongIterator.of(new long[] { 1L, 2L }));
        iters.add(LongIterator.of(new long[] { 3L, 4L }));
        long[] result = LongStream.concatIterators(iters).toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testConcatIterators_EmptyCollection() {
        List<LongIterator> iters = new ArrayList<>();
        long[] result = LongStream.concatIterators(iters).toArray();
        assertArrayEquals(new long[0], result);
    }

    @Test
    public void testConcatIteratorsCollection() {
        Collection<LongIterator> iterators = Arrays.asList(LongIterator.of(new long[] { 1L, 2L }), LongIterator.of(new long[] { 3L, 4L }));
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, LongStream.concatIterators(iterators).toArray());
    }

    @Test
    public void testConcatIteratorsCollection2() {
        List<LongIterator> iterators = Arrays.asList(LongIterator.of(new long[] { 1, 2 }), LongIterator.of(new long[] { 3, 4 }));
        LongStream result = LongStream.concatIterators(iterators);
        assertArrayEquals(new long[] { 1, 2, 3, 4 }, result.toArray());
    }

    // join

    @Test
    public void testJoin() {
        String result = LongStream.of(1L, 2L, 3L).join(", ");
        assertEquals("1, 2, 3", result);
    }

    @Test
    public void testJoin_WithDelimiter() {
        String result = LongStream.of(1, 2, 3).join(", ");
        assertEquals("1, 2, 3", result);
    }

    @Test
    public void testJoin_WithDelimiterAndPrefixSuffix() {
        String result = LongStream.of(1, 2, 3).join(", ", "[", "]");
        assertEquals("[1, 2, 3]", result);
    }

    @Test
    public void testJoinEmpty() {
        String result = LongStream.empty().join(", ");
        assertEquals("", result);
    }

    @Test
    public void testJoinWithPrefixSuffix() {
        String result = LongStream.of(1L, 2L, 3L).join(", ", "[", "]");
        assertEquals("[1, 2, 3]", result);
    }

    @Test
    public void testJoinTo() {
        Joiner joiner = Joiner.with(", ", "[", "]");
        Joiner result = createLongStream(1L, 2L, 3L).joinTo(joiner);
        assertEquals("[1, 2, 3]", result.toString());
    }

    // merge

    @Test
    public void testMerge() {
        long[] a = { 1L, 3L, 5L };
        long[] b = { 2L, 4L, 6L };

        LongStream merged = LongStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L }, merged.toArray());
    }

    @Test
    public void test_merge() {
        long[] a = { 1, 2, 3 };
        long[] b = { 4, 5, 6 };
        long[] c = { 7, 8, 9 };
        long[] result = LongStream.merge(N.toList(LongStream.of(a)), (p1, p2) -> p1 <= p2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
        assertArrayEquals(new long[] { 1, 2, 3 }, result);

        result = LongStream.merge(N.toList(LongStream.of(a), LongStream.of(b)), (p1, p2) -> p1 <= p2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6 }, result);

        result = LongStream
                .merge(N.toList(LongStream.of(a), LongStream.of(b), LongStream.of(c)), (p1, p2) -> p1 <= p2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, result);

    }

    @Test
    public void testMerge3Arrays() {
        assertEquals(7,
                LongStream
                        .merge(new long[] { 1, 4, 7 }, new long[] { 2, 5 }, new long[] { 3, 6 },
                                (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .count());
        assertEquals(6, LongStream
                .merge(new long[] { 1, 4, 7 }, new long[] { 2, 5 }, new long[] { 3, 6 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .skip(1)
                .count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6, 7 },
                LongStream
                        .merge(new long[] { 1, 4, 7 }, new long[] { 2, 5 }, new long[] { 3, 6 },
                                (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5, 6, 7 }, LongStream
                .merge(new long[] { 1, 4, 7 }, new long[] { 2, 5 }, new long[] { 3, 6 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .skip(1)
                .toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L),
                LongStream
                        .merge(new long[] { 1, 4, 7 }, new long[] { 2, 5 }, new long[] { 3, 6 },
                                (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L, 6L, 7L), LongStream
                .merge(new long[] { 1, 4, 7 }, new long[] { 2, 5 }, new long[] { 3, 6 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .skip(1)
                .toList());
        assertEquals(7, LongStream
                .merge(new long[] { 1, 4, 7 }, new long[] { 2, 5 }, new long[] { 3, 6 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .map(e -> e)
                .count());
        assertEquals(6, LongStream
                .merge(new long[] { 1, 4, 7 }, new long[] { 2, 5 }, new long[] { 3, 6 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .map(e -> e)
                .skip(1)
                .count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6, 7 }, LongStream
                .merge(new long[] { 1, 4, 7 }, new long[] { 2, 5 }, new long[] { 3, 6 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .map(e -> e)
                .toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5, 6, 7 }, LongStream
                .merge(new long[] { 1, 4, 7 }, new long[] { 2, 5 }, new long[] { 3, 6 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .map(e -> e)
                .skip(1)
                .toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L), LongStream
                .merge(new long[] { 1, 4, 7 }, new long[] { 2, 5 }, new long[] { 3, 6 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .map(e -> e)
                .toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L, 6L, 7L), LongStream
                .merge(new long[] { 1, 4, 7 }, new long[] { 2, 5 }, new long[] { 3, 6 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .map(e -> e)
                .skip(1)
                .toList());
    }

    @Test
    public void testMerge_CollectionOfStreams() {
        List<LongStream> streams = Arrays.asList(LongStream.of(1, 3, 5), LongStream.of(2, 4, 6));
        long[] result = LongStream.merge(streams, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6 }, result);
    }

    @Test
    public void testMerge_ThreeArrays() {
        long[] result = LongStream
                .merge(new long[] { 1, 4 }, new long[] { 2, 5 }, new long[] { 3, 6 }, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6 }, result);
    }

    @Test
    public void testMerge_ThreeIterators() {
        long[] result = LongStream
                .merge(LongIterator.of(new long[] { 1, 4 }), LongIterator.of(new long[] { 2, 5 }), LongIterator.of(new long[] { 3, 6 }),
                        (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6 }, result);
    }

    @Test
    public void testMergeArrays() {
        assertEquals(5,
                LongStream.merge(new long[] { 1, 3, 5 }, new long[] { 2, 4 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).count());
        assertEquals(4,
                LongStream.merge(new long[] { 1, 3, 5 }, new long[] { 2, 4 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 },
                LongStream.merge(new long[] { 1, 3, 5 }, new long[] { 2, 4 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 },
                LongStream.merge(new long[] { 1, 3, 5 }, new long[] { 2, 4 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L),
                LongStream.merge(new long[] { 1, 3, 5 }, new long[] { 2, 4 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L),
                LongStream.merge(new long[] { 1, 3, 5 }, new long[] { 2, 4 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .toList());
        assertEquals(5,
                LongStream.merge(new long[] { 1, 3, 5 }, new long[] { 2, 4 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .map(e -> e)
                        .count());
        assertEquals(4,
                LongStream.merge(new long[] { 1, 3, 5 }, new long[] { 2, 4 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .map(e -> e)
                        .skip(1)
                        .count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 },
                LongStream.merge(new long[] { 1, 3, 5 }, new long[] { 2, 4 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .map(e -> e)
                        .toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 },
                LongStream.merge(new long[] { 1, 3, 5 }, new long[] { 2, 4 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .map(e -> e)
                        .skip(1)
                        .toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L),
                LongStream.merge(new long[] { 1, 3, 5 }, new long[] { 2, 4 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .map(e -> e)
                        .toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L),
                LongStream.merge(new long[] { 1, 3, 5 }, new long[] { 2, 4 }, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .map(e -> e)
                        .skip(1)
                        .toList());
    }

    @Test
    public void testMergeArrays_TwoArrays_Basic() {
        // merge(long[], long[], LongBiFunction) - exercises the "first array exhausted" branch
        long[] a = { 1L, 3L, 5L };
        long[] b = { 2L, 4L };
        long[] result = LongStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, result);
    }

    @Test
    public void testMergeArrays_TwoArrays_EmptyFirst() {
        long[] result = LongStream.merge(new long[0], new long[] { 1L, 2L }, (x, y) -> MergeResult.TAKE_FIRST).toArray();
        assertArrayEquals(new long[] { 1L, 2L }, result);
    }

    @Test
    public void testMergeCollection_Empty() {
        assertEquals(0L, LongStream.merge(new ArrayList<>(), (x, y) -> MergeResult.TAKE_FIRST).count());
    }

    @Test
    public void testMergeCollection_SingleStream() {
        List<LongStream> streams = Arrays.asList(LongStream.of(7L, 8L, 9L));
        long[] result = LongStream.merge(streams, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
        assertArrayEquals(new long[] { 7L, 8L, 9L }, result);
    }

    @Test
    public void testMergeCollection_TwoStreams() {
        List<LongStream> streams = Arrays.asList(LongStream.of(1L, 3L), LongStream.of(2L, 4L));
        long[] result = LongStream.merge(streams, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, result);
    }

    @Test
    public void testMergeCollectionOfStreams() {
        Collection<LongStream> streams = Arrays.asList(LongStream.of(1, 4), LongStream.of(2, 5), LongStream.of(3, 6));
        LongBiFunction<MergeResult> selector = (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        assertEquals(6, LongStream.merge(streams, selector).count());

        streams = Arrays.asList(LongStream.of(1, 4), LongStream.of(2, 5), LongStream.of(3, 6));
        assertEquals(5, LongStream.merge(streams, selector).skip(1).count());

        streams = Arrays.asList(LongStream.of(1, 4), LongStream.of(2, 5), LongStream.of(3, 6));
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6 }, LongStream.merge(streams, selector).toArray());

        streams = Arrays.asList(LongStream.of(1, 4), LongStream.of(2, 5), LongStream.of(3, 6));
        assertArrayEquals(new long[] { 2, 3, 4, 5, 6 }, LongStream.merge(streams, selector).skip(1).toArray());

        streams = Arrays.asList(LongStream.of(1, 4), LongStream.of(2, 5), LongStream.of(3, 6));
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L), LongStream.merge(streams, selector).toList());

        streams = Arrays.asList(LongStream.of(1, 4), LongStream.of(2, 5), LongStream.of(3, 6));
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L, 6L), LongStream.merge(streams, selector).skip(1).toList());

        streams = Arrays.asList(LongStream.of(1, 4), LongStream.of(2, 5), LongStream.of(3, 6));
        assertEquals(6, LongStream.merge(streams, selector).map(e -> e).count());

        streams = Arrays.asList(LongStream.of(1, 4), LongStream.of(2, 5), LongStream.of(3, 6));
        assertEquals(5, LongStream.merge(streams, selector).map(e -> e).skip(1).count());

        streams = Arrays.asList(LongStream.of(1, 4), LongStream.of(2, 5), LongStream.of(3, 6));
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6 }, LongStream.merge(streams, selector).map(e -> e).toArray());

        streams = Arrays.asList(LongStream.of(1, 4), LongStream.of(2, 5), LongStream.of(3, 6));
        assertArrayEquals(new long[] { 2, 3, 4, 5, 6 }, LongStream.merge(streams, selector).map(e -> e).skip(1).toArray());

        streams = Arrays.asList(LongStream.of(1, 4), LongStream.of(2, 5), LongStream.of(3, 6));
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L), LongStream.merge(streams, selector).map(e -> e).toList());

        streams = Arrays.asList(LongStream.of(1, 4), LongStream.of(2, 5), LongStream.of(3, 6));
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L, 6L), LongStream.merge(streams, selector).map(e -> e).skip(1).toList());
    }

    @Test
    public void testMergeCollectionTreatsNullStreamAsEmpty() {
        assertEquals(0, LongStream.merge(Arrays.asList((LongStream) null), (a, b) -> MergeResult.TAKE_FIRST).count());
    }

    @Test
    public void testMergeIterators() {
        LongIterator i1 = LongIterator.of(new long[] { 1L, 3L, 5L });
        LongIterator i2 = LongIterator.of(new long[] { 2L, 4L, 6L });

        LongStream merged = LongStream.merge(i1, i2, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L }, merged.toArray());
    }

    @Test
    public void testMergeIteratorThreeWay() {
        LongIterator a = LongIterator.of(new long[] { 1, 4, 7 });
        LongIterator b = LongIterator.of(new long[] { 2, 5, 8 });
        LongIterator c = LongIterator.of(new long[] { 3, 6, 9 });
        long[] result = LongStream.merge(a, b, c, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, result);
    }

    @Test
    public void testMergeMultipleStreams() {
        Collection<LongStream> streams = Arrays.asList(createLongStream(1L, 4L, 7L), createLongStream(2L, 5L, 8L), createLongStream(3L, 6L, 9L));

        LongStream result = LongStream.merge(streams, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L }, result.toArray());
    }

    @Test
    public void testMergeStreams() {
        assertEquals(5,
                LongStream.merge(LongStream.of(1, 3, 5), LongStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).count());
        assertEquals(4,
                LongStream.merge(LongStream.of(1, 3, 5), LongStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 },
                LongStream.merge(LongStream.of(1, 3, 5), LongStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 },
                LongStream.merge(LongStream.of(1, 3, 5), LongStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L),
                LongStream.merge(LongStream.of(1, 3, 5), LongStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L),
                LongStream.merge(LongStream.of(1, 3, 5), LongStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .toList());
        assertEquals(5,
                LongStream.merge(LongStream.of(1, 3, 5), LongStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .map(e -> e)
                        .count());
        assertEquals(4,
                LongStream.merge(LongStream.of(1, 3, 5), LongStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .map(e -> e)
                        .skip(1)
                        .count());
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 },
                LongStream.merge(LongStream.of(1, 3, 5), LongStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .map(e -> e)
                        .toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 5 },
                LongStream.merge(LongStream.of(1, 3, 5), LongStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .map(e -> e)
                        .skip(1)
                        .toArray());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L),
                LongStream.merge(LongStream.of(1, 3, 5), LongStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .map(e -> e)
                        .toList());
        assertEquals(Arrays.asList(2L, 3L, 4L, 5L),
                LongStream.merge(LongStream.of(1, 3, 5), LongStream.of(2, 4), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .map(e -> e)
                        .skip(1)
                        .toList());
    }

    @Test
    public void testMergeStreamsThreeWay() {
        long[] result = LongStream
                .merge(LongStream.of(1, 4, 7), LongStream.of(2, 5, 8), LongStream.of(3, 6, 9),
                        (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, result);
    }

    @Test
    public void testMergeThreeArrays() {
        long[] a = { 1L, 4L, 7L };
        long[] b = { 2L, 5L, 8L };
        long[] c = { 3L, 6L, 9L };

        LongStream result = LongStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L }, result.toArray());
    }

    @Test
    public void testMergeThreeStreams() {
        LongStream s1 = LongStream.of(1L, 4L, 7L);
        LongStream s2 = LongStream.of(2L, 5L, 8L);
        LongStream s3 = LongStream.of(3L, 6L, 9L);

        LongStream result = LongStream.merge(s1, s2, s3, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L }, result.toArray());
    }

    @Test
    public void testMergeWithCollectionOfStreams() {
        Collection<LongStream> streams = Arrays.asList(LongStream.of(1L, 5L), LongStream.of(2L, 6L), LongStream.of(3L, 7L));
        LongStream result = LongStream.merge(streams, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 5L, 6L, 7L }, result.toArray());
    }

    @Test
    public void testMergeWithThreeArrays() {
        long[] a = { 1L, 4L, 7L };
        long[] b = { 2L, 5L, 8L };
        long[] c = { 3L, 6L, 9L };

        LongStream merged = LongStream.merge(a, b, c, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L }, merged.toArray());
    }

    @Test
    public void testMergeWith() {
        LongStream s1 = LongStream.of(1L, 3L, 5L);
        LongStream s2 = LongStream.of(2L, 4L, 6L);

        long[] result = s1.mergeWith(s2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L }, result);
    }

    // prepend

    @Test
    public void testPrepend() {
        LongStream stream = LongStream.of(3L, 4L, 5L);
        long[] result = stream.prepend(1L, 2L).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, result);
    }

    @Test
    public void testPrependOptionalLong() {
        long[] result1 = createLongStream(2L, 3L).prepend(OptionalLong.of(1L)).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result1);

        long[] result2 = createLongStream(2L, 3L).prepend(OptionalLong.empty()).toArray();
        assertArrayEquals(new long[] { 2L, 3L }, result2);
    }

    @Test
    public void testPrependStream() {
        LongStream s1 = LongStream.of(1L, 2L);
        LongStream s2 = LongStream.of(3L, 4L);
        long[] result = s2.prepend(s1).toArray();
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, result);
    }

    // zip

    @Test
    public void testZip() {
        long[] a = { 1L, 2L, 3L };
        long[] b = { 4L, 5L, 6L };

        LongStream zipped = LongStream.zip(a, b, (x, y) -> x + y);
        assertArrayEquals(new long[] { 5L, 7L, 9L }, zipped.toArray());
    }

    @Test
    public void testZip3Arrays() {
        assertEquals(3, LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, new long[] { 100, 200, 300 }, (a, b, c) -> a + b + c).count());
        assertEquals(2,
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, new long[] { 100, 200, 300 }, (a, b, c) -> a + b + c).skip(1).count());
        assertArrayEquals(new long[] { 111, 222, 333 },
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, new long[] { 100, 200, 300 }, (a, b, c) -> a + b + c).toArray());
        assertArrayEquals(new long[] { 222, 333 },
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, new long[] { 100, 200, 300 }, (a, b, c) -> a + b + c).skip(1).toArray());
        assertEquals(Arrays.asList(111L, 222L, 333L),
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, new long[] { 100, 200, 300 }, (a, b, c) -> a + b + c).toList());
        assertEquals(Arrays.asList(222L, 333L),
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, new long[] { 100, 200, 300 }, (a, b, c) -> a + b + c).skip(1).toList());
        assertEquals(3,
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, new long[] { 100, 200, 300 }, (a, b, c) -> a + b + c)
                        .map(e -> e)
                        .count());
        assertEquals(2,
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, new long[] { 100, 200, 300 }, (a, b, c) -> a + b + c)
                        .map(e -> e)
                        .skip(1)
                        .count());
        assertArrayEquals(new long[] { 111, 222, 333 },
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, new long[] { 100, 200, 300 }, (a, b, c) -> a + b + c)
                        .map(e -> e)
                        .toArray());
        assertArrayEquals(new long[] { 222, 333 },
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, new long[] { 100, 200, 300 }, (a, b, c) -> a + b + c)
                        .map(e -> e)
                        .skip(1)
                        .toArray());
        assertEquals(Arrays.asList(111L, 222L, 333L),
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, new long[] { 100, 200, 300 }, (a, b, c) -> a + b + c)
                        .map(e -> e)
                        .toList());
        assertEquals(Arrays.asList(222L, 333L),
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, new long[] { 100, 200, 300 }, (a, b, c) -> a + b + c)
                        .map(e -> e)
                        .skip(1)
                        .toList());
    }

    @Test
    public void test_zip_02() {
        long[] a = { 1 };
        long[] b = { 4, 5 };
        long[] c = { 7, 8, 9 };
        long[] result = LongStream.zip(a, b, c, 100, 200, 300, (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new long[] { 12, 113, 309 }, result);
    }

    @Test
    public void testZipArrays() {
        assertEquals(3, LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, (a, b) -> a + b).count());
        assertEquals(2, LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new long[] { 11, 22, 33 }, LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, (a, b) -> a + b).toArray());
        assertArrayEquals(new long[] { 22, 33 }, LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, (a, b) -> a + b).skip(1).toArray());
        assertEquals(Arrays.asList(11L, 22L, 33L), LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, (a, b) -> a + b).toList());
        assertEquals(Arrays.asList(22L, 33L), LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, (a, b) -> a + b).skip(1).toList());
        assertEquals(3, LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, (a, b) -> a + b).map(e -> e).count());
        assertEquals(2, LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, (a, b) -> a + b).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 11, 22, 33 },
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, (a, b) -> a + b).map(e -> e).toArray());
        assertArrayEquals(new long[] { 22, 33 },
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, (a, b) -> a + b).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(11L, 22L, 33L), LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, (a, b) -> a + b).map(e -> e).toList());
        assertEquals(Arrays.asList(22L, 33L),
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20, 30, 40 }, (a, b) -> a + b).map(e -> e).skip(1).toList());
    }

    @Test
    public void testZipArrays_WithDefaults_BothEmpty() {
        assertEquals(0L, LongStream.zip(new long[0], new long[0], 0L, 0L, Long::sum).count());
    }

    @Test
    public void testZipArrays_WithDefaults_TwoArraysDifferentLength() {
        // zip(long[], long[], long, long, LongBinaryOperator)
        long[] a = { 1L, 2L, 3L };
        long[] b = { 10L, 20L };
        long[] result = LongStream.zip(a, b, 0L, 100L, Long::sum).toArray();
        // [1+10, 2+20, 3+100]
        assertArrayEquals(new long[] { 11L, 22L, 103L }, result);
    }

    @Test
    public void testZipArraysWithDefaults() {
        assertEquals(3, LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20 }, 0L, 0L, (a, b) -> a + b).count());
        assertEquals(2, LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20 }, 0L, 0L, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new long[] { 11, 22, 3 }, LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20 }, 0L, 0L, (a, b) -> a + b).toArray());
        assertArrayEquals(new long[] { 22, 3 }, LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20 }, 0L, 0L, (a, b) -> a + b).skip(1).toArray());
        assertEquals(Arrays.asList(11L, 22L, 3L), LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20 }, 0L, 0L, (a, b) -> a + b).toList());
        assertEquals(Arrays.asList(22L, 3L), LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20 }, 0L, 0L, (a, b) -> a + b).skip(1).toList());
        assertEquals(3, LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20 }, 0L, 0L, (a, b) -> a + b).map(e -> e).count());
        assertEquals(2, LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20 }, 0L, 0L, (a, b) -> a + b).map(e -> e).skip(1).count());
        assertArrayEquals(new long[] { 11, 22, 3 },
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20 }, 0L, 0L, (a, b) -> a + b).map(e -> e).toArray());
        assertArrayEquals(new long[] { 22, 3 },
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20 }, 0L, 0L, (a, b) -> a + b).map(e -> e).skip(1).toArray());
        assertEquals(Arrays.asList(11L, 22L, 3L), LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20 }, 0L, 0L, (a, b) -> a + b).map(e -> e).toList());
        assertEquals(Arrays.asList(22L, 3L),
                LongStream.zip(new long[] { 1, 2, 3 }, new long[] { 10, 20 }, 0L, 0L, (a, b) -> a + b).map(e -> e).skip(1).toList());
    }

    @Test
    public void testZipCollectionOfStreams() {
        Collection<LongStream> streams = Arrays.asList(LongStream.of(1, 2, 3), LongStream.of(10, 20, 30), LongStream.of(100, 200, 300));
        LongNFunction<Long> zipFunc = values -> Stream.of(values).mapToLong(v -> v).sum();

        assertEquals(3, LongStream.zip(streams, zipFunc).count());

        streams = Arrays.asList(LongStream.of(1, 2, 3), LongStream.of(10, 20, 30), LongStream.of(100, 200, 300));
        assertEquals(2, LongStream.zip(streams, zipFunc).skip(1).count());

        streams = Arrays.asList(LongStream.of(1, 2, 3), LongStream.of(10, 20, 30), LongStream.of(100, 200, 300));
        assertArrayEquals(new long[] { 111, 222, 333 }, LongStream.zip(streams, zipFunc).toArray());

        streams = Arrays.asList(LongStream.of(1, 2, 3), LongStream.of(10, 20, 30), LongStream.of(100, 200, 300));
        assertArrayEquals(new long[] { 222, 333 }, LongStream.zip(streams, zipFunc).skip(1).toArray());
    }

    @Test
    public void testZipCollectionWithDefaults() {
        List<LongStream> streams = Arrays.asList(LongStream.of(1, 2, 3), LongStream.of(10, 20));
        long[] result = LongStream.zip(streams, new long[] { 0, 0 }, values -> values[0] + values[1]).toArray();
        assertArrayEquals(new long[] { 11, 22, 3 }, result);
    }

    @Test
    public void testZipIterators() {
        LongIterator i1 = LongIterator.of(new long[] { 1L, 2L, 3L });
        LongIterator i2 = LongIterator.of(new long[] { 4L, 5L, 6L });

        LongStream zipped = LongStream.zip(i1, i2, (x, y) -> x * y);
        assertArrayEquals(new long[] { 4L, 10L, 18L }, zipped.toArray());
    }

    @Test
    public void testZipIteratorsThreeWay() {
        LongIterator a = LongIterator.of(new long[] { 1, 2 });
        LongIterator b = LongIterator.of(new long[] { 10, 20 });
        LongIterator c = LongIterator.of(new long[] { 100, 200 });
        long[] result = LongStream.zip(a, b, c, (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new long[] { 111, 222 }, result);
    }

    @Test
    public void testZipIteratorsThreeWayWithDefaults() {
        LongIterator a = LongIterator.of(new long[] { 1 });
        LongIterator b = LongIterator.of(new long[] { 10, 20 });
        LongIterator c = LongIterator.of(new long[] { 100, 200, 300 });
        long[] result = LongStream.zip(a, b, c, 0L, 0L, 0L, (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new long[] { 111, 220, 300 }, result);
    }

    @Test
    public void testZipIteratorsWithDefaults() {
        LongIterator a = LongIterator.of(new long[] { 1, 2, 3 });
        LongIterator b = LongIterator.of(new long[] { 10, 20 });
        long[] result = LongStream.zip(a, b, 0L, 0L, Long::sum).toArray();
        assertArrayEquals(new long[] { 11, 22, 3 }, result);
    }

    @Test
    public void testZipMultipleStreams() {
        Collection<LongStream> streams = Arrays.asList(createLongStream(1L, 2L, 3L), createLongStream(4L, 5L, 6L), createLongStream(7L, 8L, 9L));

        LongStream result = LongStream.zip(streams, values -> Stream.of(values).reduce(0L, Long::sum));
        assertArrayEquals(new long[] { 12L, 15L, 18L }, result.toArray());
    }

    @Test
    public void testZipMultipleStreamsWithDefaults() {
        Collection<LongStream> streams = Arrays.asList(createLongStream(1L, 2L), createLongStream(3L, 4L, 5L), createLongStream(6L));

        long[] defaults = { 10L, 20L, 30L };
        LongStream result = LongStream.zip(streams, defaults, values -> Stream.of(values).reduce(0L, Long::sum));
        assertArrayEquals(new long[] { 10L, 36L, 45L }, result.toArray());
    }

    @Test
    public void testZipStreams() {
        LongStream s1 = LongStream.of(1L, 2L, 3L);
        LongStream s2 = LongStream.of(10L, 20L, 30L);

        LongStream zipped = LongStream.zip(s1, s2, (x, y) -> x + y);
        assertArrayEquals(new long[] { 11L, 22L, 33L }, zipped.toArray());
    }

    @Test
    public void testZipStreamsThreeWayWithDefaults() {
        long[] result = LongStream.zip(LongStream.of(1), LongStream.of(10, 20), LongStream.of(100, 200, 300), 0L, 0L, 0L, (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new long[] { 111, 220, 300 }, result);
    }

    @Test
    public void testZipStreamsWithDefaults() {
        long[] result = LongStream.zip(LongStream.of(1, 2, 3), LongStream.of(10, 20), 0L, 0L, Long::sum).toArray();
        assertArrayEquals(new long[] { 11, 22, 3 }, result);
    }

    @Test
    public void testZipThreeArrays() {
        long[] a = { 1L, 2L, 3L };
        long[] b = { 4L, 5L, 6L };
        long[] c = { 7L, 8L, 9L };

        LongStream zipped = LongStream.zip(a, b, c, (x, y, z) -> x + y + z);
        assertArrayEquals(new long[] { 12L, 15L, 18L }, zipped.toArray());
    }

    @Test
    public void testZipThreeArrays_WithDefaults() {
        // zip(long[], long[], long[], long, long, long, LongTernaryOperator)
        long[] a = { 1L, 2L };
        long[] b = { 10L };
        long[] c = { 100L, 200L, 300L };
        long[] result = LongStream.zip(a, b, c, 0L, 0L, 0L, (x, y, z) -> x + y + z).toArray();
        // [1+10+100, 2+0+200, 0+0+300]
        assertArrayEquals(new long[] { 111L, 202L, 300L }, result);
    }

    @Test
    public void testZipThreeArrays_WithDefaults_AllEmpty() {
        assertEquals(0L, LongStream.zip(new long[0], new long[0], new long[0], 0L, 0L, 0L, (x, y, z) -> x + y + z).count());
    }

    @Test
    public void testZipThreeArraysWithDefaults() {
        long[] a = { 1L, 2L, 3L, 4L };
        long[] b = { 10L, 20L };
        long[] c = { 100L };

        LongStream stream = LongStream.zip(a, b, c, 0L, 0L, 0L, (x, y, z) -> x + y + z);
        assertArrayEquals(new long[] { 111L, 22L, 3L, 4L }, stream.toArray());
    }

    @Test
    public void testZipThreeIteratorsWithDefaults() {
        LongIterator it1 = LongIterator.of(new long[] { 1L, 2L, 3L });
        LongIterator it2 = LongIterator.of(new long[] { 10L });
        LongIterator it3 = LongIterator.of(new long[] { 100L, 200L });

        LongStream stream = LongStream.zip(it1, it2, it3, 0L, 0L, 0L, (x, y, z) -> x + y + z);
        assertArrayEquals(new long[] { 111L, 202L, 3L }, stream.toArray());
    }

    @Test
    public void testZipWith_CollectionOfStreams() {
        List<LongStream> streams = Arrays.asList(LongStream.of(1, 2, 3), LongStream.of(10, 20, 30));
        long[] result = LongStream.zip(streams, args -> args[0] + args[1]).toArray();
        assertArrayEquals(new long[] { 11, 22, 33 }, result);
    }

    @Test
    public void testZipWith_CollectionOfStreamsWithDefaults() {
        List<LongStream> streams = Arrays.asList(LongStream.of(1, 2), LongStream.of(10, 20, 30));
        long[] result = LongStream.zip(streams, new long[] { 0, 0 }, args -> args[0] + args[1]).toArray();
        assertArrayEquals(new long[] { 11, 22, 30 }, result);
    }

    @Test
    public void testZipWithCollectionAndValuesForNone() {
        Collection<LongStream> streams = Arrays.asList(LongStream.of(1L, 2L, 3L), LongStream.of(10L), LongStream.of(100L, 200L));
        long[] defaults = { 0L, 0L, 0L };
        LongStream result = LongStream.zip(streams, defaults, longs -> {
            long sum = 0;
            for (Long l : longs) {
                sum += l;
            }
            return sum;
        });
        assertArrayEquals(new long[] { 111L, 202L, 3L }, result.toArray());
    }

    @Test
    public void testZipWithCollectionOfStreams() {
        Collection<LongStream> streams = Arrays.asList(LongStream.of(1L, 2L), LongStream.of(10L, 20L), LongStream.of(100L, 200L));
        LongStream result = LongStream.zip(streams, longs -> {
            long sum = 0;
            for (Long l : longs) {
                sum += l;
            }
            return sum;
        });
        assertArrayEquals(new long[] { 111L, 222L }, result.toArray());
    }

    @Test
    public void testZipWithDefaultValues() {
        long[] a = { 1L, 2L };
        long[] b = { 3L, 4L, 5L };

        LongStream zipped = LongStream.zip(a, b, 0L, 0L, (x, y) -> x + y);
        assertArrayEquals(new long[] { 4L, 6L, 5L }, zipped.toArray());
    }

    @Test
    public void testZipWithDifferentLength() {
        long[] a = { 1, 2 };
        long[] b = { 4, 5, 6 };
        assertArrayEquals(new long[] { 5, 7 }, LongStream.zip(a, b, (x, y) -> x + y).toArray());
    }

    @Test
    public void testZipWithDifferentLengths() {
        long[] a = { 1L, 2L, 3L, 4L };
        long[] b = { 5L, 6L };

        LongStream zipped = LongStream.zip(a, b, (x, y) -> x + y);
        assertArrayEquals(new long[] { 6L, 8L }, zipped.toArray());
    }

    @Test
    public void testZipWithThreeArrays() {
        long[] a = { 1L, 2L, 3L };
        long[] b = { 4L, 5L, 6L };
        long[] c = { 7L, 8L, 9L };

        long[] result = LongStream.zip(a, b, c, (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new long[] { 12L, 15L, 18L }, result);
    }

    @Test
    public void testZipThreeStreamsWithDefaults() {
        LongStream s1 = LongStream.of(1L, 2L);
        LongStream s2 = LongStream.of(10L, 20L, 30L);
        LongStream s3 = LongStream.of(100L);

        LongStream stream = s1.zipWith(s2, s3, 0L, 0L, 0L, (x, y, z) -> x + y + z);
        assertArrayEquals(new long[] { 111L, 22, 30L }, stream.toArray());
    }

    // zipWith

    @Test
    public void testZipWith() {
        LongStream s1 = LongStream.of(1L, 2L, 3L);
        LongStream s2 = LongStream.of(10L, 20L, 30L);

        long[] result = s1.zipWith(s2, (a, b) -> a + b).toArray();
        assertArrayEquals(new long[] { 11L, 22L, 33L }, result);
    }

    @Test
    public void testZipWithDefaultValuesForStreams() {
        LongStream s1 = LongStream.of(1L, 2L);
        LongStream s2 = LongStream.of(10L, 20L, 30L);

        long[] result = s1.zipWith(s2, 0L, 0L, (a, b) -> a + b).toArray();
        assertArrayEquals(new long[] { 11L, 22L, 30L }, result);
    }

    @Test
    public void testZipWithStreams() {
        LongStream a = createLongStream(1L, 2L, 3L);
        LongStream b = createLongStream(4L, 5L, 6L);

        long[] result = a.zipWith(b, (x, y) -> x + y).toArray();
        assertArrayEquals(new long[] { 5L, 7L, 9L }, result);
    }

    @Test
    public void testZipWithThreeStreams() {
        LongStream s1 = LongStream.of(1L, 2L);
        LongStream s2 = LongStream.of(10L, 20L);
        LongStream s3 = LongStream.of(100L, 200L);

        long[] result = s1.zipWith(s2, s3, (a, b, c) -> a + b + c).toArray();
        assertArrayEquals(new long[] { 111L, 222L }, result);
    }

    @Test
    public void testZipWithThreeStreamsAndDefaults() {
        LongStream a = createLongStream(1L, 2L);
        LongStream b = createLongStream(3L, 4L, 5L);
        LongStream c = createLongStream(6L);

        long[] result = a.zipWith(b, c, 10L, 20L, 30L, (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new long[] { 10L, 36L, 45L }, result);
    }

}
