package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.N;

/** DoubleStream combination contracts. */
public class DoubleStreamCombinationTest extends DoubleStreamTestSupport {

    @Test
    public void testAppend() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0);
        double[] result = stream.append(3.0, 4.0).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, result, 0.0001);
    }

    @Test
    public void testAppendArray() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0);
        double[] result = stream.append(4.0, 5.0).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, result, 0.0001);
    }

    @Test
    public void testAppendIfEmpty() {
        DoubleStream stream = DoubleStream.empty();
        double[] result = stream.appendIfEmpty(1.0, 2.0).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0 }, result, 0.0001);

        DoubleStream stream2 = DoubleStream.of(5.0);
        double[] result2 = stream2.appendIfEmpty(1.0, 2.0).toArray();
        assertArrayEquals(new double[] { 5.0 }, result2, 0.0001);
    }

    // concat

    @Test
    public void testConcat() {
        DoubleStream a = DoubleStream.of(1.0, 2.0);
        DoubleStream b = DoubleStream.of(3.0, 4.0);
        DoubleStream c = DoubleStream.of(5.0, 6.0);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 }, DoubleStream.concat(a, b, c).toArray());
    }

    @Test
    public void test_concat_02() {
        double[] a = { 1.0d, 2.0d, 3.0d };
        double[] b = { 4.0d, 5.0d, 6.0d };
        double[] result = DoubleStream.concat(a, b).toArray();
        assertArrayEquals(new double[] { 1.0d, 2.0d, 3.0d, 4.0d, 5.0d, 6.0d }, result);

        result = DoubleStream.concat(N.toList(a, b)).toArray();
        assertArrayEquals(new double[] { 1.0d, 2.0d, 3.0d, 4.0d, 5.0d, 6.0d }, result);

        result = DoubleStream.concatIterators(N.toList(DoubleIterator.of(a), DoubleIterator.of(b))).toArray();
        assertArrayEquals(new double[] { 1.0d, 2.0d, 3.0d, 4.0d, 5.0d, 6.0d }, result);
    }

    @Test
    public void testConcatArrays() {
        double[] a1 = { 1.0, 2.0 };
        double[] a2 = { 3.0, 4.0 };
        double[] a3 = { 5.0 };
        DoubleStream stream = DoubleStream.concat(a1, a2, a3);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testConcatCollectionOfStreams() {
        Collection<DoubleStream> streams = Arrays.asList(DoubleStream.of(1.0, 2.0), DoubleStream.of(3.0, 4.0), DoubleStream.of(5.0));
        DoubleStream stream = DoubleStream.concat(streams);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testConcatCollectionSnapshotsSources() {
        List<DoubleStream> sources = new ArrayList<>(Arrays.asList(DoubleStream.of(1.0, 2.0), DoubleStream.of(3.0, 4.0)));
        DoubleStream concatenated = DoubleStream.concat(sources);

        sources.clear();

        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, concatenated.toArray());
    }

    @Test
    public void testConcatIteratorVarargs() {
        DoubleIterator iter1 = DoubleIterator.of(new double[] { 1.0, 2.0 });
        DoubleIterator iter2 = DoubleIterator.of(new double[] { 3.0, 4.0 });
        DoubleStream stream = DoubleStream.concat(iter1, iter2);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testConcatListOfArrays() {
        List<double[]> list = Arrays.asList(new double[] { 1.0, 2.0 }, new double[] { 3.0, 4.0 }, new double[] { 5.0 });
        DoubleStream stream = DoubleStream.concat(list);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, stream.toArray(), 0.0001);
    }



    // G094-01: bulk paths after partial consumption (skip lands inside a segment, at a segment end, beyond the end)
    @Test
    public void testConcatListOfArrays_bulkOpsAfterSkip() {
        final List<double[]> list = Arrays.asList(new double[] { 1, 2 }, null, new double[] { 3 }, new double[0], new double[] { 4, 5, 6 });

        for (int n = 0; n <= 8; n++) {
            final double[] expected = new double[Math.max(0, 6 - n)];
            for (int i = 0; i < expected.length; i++) {
                expected[i] = n + i + 1;
            }

            assertArrayEquals(expected, DoubleStream.concat(list).skip(n).toArray(), "skip " + n);
            assertEquals(expected.length, DoubleStream.concat(list).skip(n).count(), "skip " + n);
            assertArrayEquals(expected, DoubleStream.concat(list).skip(n).toDoubleList().toArray(), "skip " + n);
        }

        // iterator consumed partially, then drained through the stream-level bulk operation
        final DoubleIterator iter = DoubleStream.concat(list).iterator();
        assertEquals(1.0, iter.nextDouble());
        assertArrayEquals(new double[] { 2, 3, 4, 5, 6 }, iter.toArray());
        assertFalse(iter.hasNext());
    }



    // G094-01: long segments take the bulk-copy path (whole segments and a partially consumed head segment)
    @Test
    public void testConcatListOfArrays_longSegmentsBulkPath() {
        final double[] a = new double[20];
        final double[] b = new double[40];
        final double[] c = { -1.5, -2.5, -3.5 };
        for (int i = 0; i < a.length; i++) {
            a[i] = i;
        }
        for (int i = 0; i < b.length; i++) {
            b[i] = 100 + i;
        }
        final List<double[]> list = Arrays.asList(a, null, new double[0], c, b, c);
        final double[] all = new double[66];
        System.arraycopy(a, 0, all, 0, 20);
        System.arraycopy(c, 0, all, 20, 3);
        System.arraycopy(b, 0, all, 23, 40);
        System.arraycopy(c, 0, all, 63, 3);

        for (final int n : new int[] { 0, 1, 4, 5, 19, 20, 21, 23, 24, 27, 30, 62, 63, 64, 66, 70 }) {
            final double[] expected = Arrays.copyOfRange(all, Math.min(n, all.length), all.length);
            assertArrayEquals(expected, DoubleStream.concat(list).skip(n).toArray(), "skip " + n);
            assertEquals(expected.length, DoubleStream.concat(list).skip(n).count(), "skip " + n);
            final DoubleList dl = DoubleStream.concat(list).skip(n).toDoubleList();
            assertArrayEquals(expected, dl.toArray(), "skip " + n);
            dl.add(1.0);
            assertEquals(expected.length + 1, dl.size());
        }

        final double[] result = DoubleStream.concat(list).toArray();
        assertArrayEquals(all, result);
        result[0] = 99;
        assertEquals(0.0, a[0]);
        final double[] result2 = DoubleStream.concat(Arrays.asList(b)).toArray();
        assertArrayEquals(b, result2);
        assertFalse(result2 == b);
        assertEquals(100.0, DoubleStream.concat(b, a).toArray()[0]);
    }



    // G094-01: concat(List<double[]>) bulk toArray/count/toDoubleList must equal the element-wise results
    @Test
    public void testConcatListOfArrays_toArrayCountToList_edgeCases() {
        final double[] a = { 1.0, -0.0, Double.NaN };
        final double[] b = {};
        final double[] c = { Double.NEGATIVE_INFINITY, 5.5 };
        final List<double[]> list = Arrays.asList(null, a, b, null, c, b);
        final double[] expected = { 1.0, -0.0, Double.NaN, Double.NEGATIVE_INFINITY, 5.5 };

        assertArrayEquals(expected, DoubleStream.concat(list).toArray());
        assertEquals(5, DoubleStream.concat(list).count());
        assertArrayEquals(expected, DoubleStream.concat(list).toDoubleList().toArray());

        // the result is a fresh array / a fresh mutable list
        final double[] arr = DoubleStream.concat(list).toArray();
        arr[0] = 42.0;
        assertEquals(1.0, a[0]);
        final DoubleList dl = DoubleStream.concat(list).toDoubleList();
        dl.add(7.0);
        assertEquals(6, dl.size());
        assertEquals(1.0, a[0]);

        // only empty / null arrays
        final List<double[]> empties = Arrays.asList(b, null, new double[0]);
        assertEquals(0, DoubleStream.concat(empties).toArray().length);
        assertEquals(0, DoubleStream.concat(empties).count());
        final DoubleList emptyList = DoubleStream.concat(empties).toDoubleList();
        assertEquals(0, emptyList.size());
        emptyList.add(1.0);
        assertEquals(1, emptyList.size());

        // single array
        assertArrayEquals(new double[] { 3.0, 4.0 }, DoubleStream.concat(Arrays.asList(new double[][] { { 3.0, 4.0 } })).toArray());
    }

    @Test
    public void testConcatStreams() {
        DoubleStream s1 = DoubleStream.of(1.0, 2.0);
        DoubleStream s2 = DoubleStream.of(3.0, 4.0);
        DoubleStream s3 = DoubleStream.of(5.0);
        DoubleStream stream = DoubleStream.concat(s1, s2, s3);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testConcatTwoArrays() {
        double[] a1 = { 1.0, 2.0 };
        double[] a2 = { 3.0, 4.0 };
        DoubleStream stream = DoubleStream.concat(a1, a2);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testConcatTwoStreams() {
        DoubleStream s1 = DoubleStream.of(1.0, 2.0);
        DoubleStream s2 = DoubleStream.of(3.0, 4.0);
        DoubleStream stream = DoubleStream.concat(s1, s2);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testConcatIteratorList() {
        List<DoubleIterator> list = Arrays.asList(DoubleIterator.of(new double[] { 1.0, 2.0 }), DoubleIterator.of(new double[] { 3.0, 4.0 }));
        DoubleStream stream = DoubleStream.concatIterators(list);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, stream.toArray(), 0.0001);
    }

    // concatIterators

    @Test
    public void testConcatIterators_Collection() {
        List<DoubleIterator> iters = new ArrayList<>();
        iters.add(DoubleIterator.of(new double[] { 1.0, 2.0 }));
        iters.add(DoubleIterator.of(new double[] { 3.0, 4.0 }));
        double[] result = DoubleStream.concatIterators(iters).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, result, 0.001);
    }

    @Test
    public void testConcatIterators_EmptyCollection() {
        List<DoubleIterator> iters = new ArrayList<>();
        double[] result = DoubleStream.concatIterators(iters).toArray();
        assertArrayEquals(new double[0], result, 0.001);
    }

    @Test
    public void testConcatIteratorsCollection() {
        List<DoubleIterator> iterators = Arrays.asList(DoubleIterator.of(new double[] { 1.0, 2.0 }), DoubleIterator.of(new double[] { 3.0, 4.0 }));
        DoubleStream result = DoubleStream.concatIterators(iterators);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, result.toArray(), 0.001);
    }

    @Test
    public void testJoin_WithDelimiter() {
        String result = DoubleStream.of(1.0, 2.0, 3.0).join(", ");
        assertNotNull(result);
        assertTrue(result.contains("1.0"));
        assertTrue(result.contains("2.0"));
    }

    @Test
    public void testJoin_WithDelimiterAndPrefixSuffix() {
        String result = DoubleStream.of(1.0, 2.0, 3.0).join(", ", "[", "]");
        assertTrue(result.startsWith("["));
        assertTrue(result.endsWith("]"));
    }

    // merge

    @Test
    public void test_merge() {
        double[] a = { 1.0d, 2.0d, 3.0d };
        double[] b = { 4.0d, 5.0d, 6.0d };
        double[] c = { 7.0d, 8.0d, 9.0d };
        double[] result = DoubleStream.merge(N.toList(DoubleStream.of(a)), (p1, p2) -> p1 <= p2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
        assertArrayEquals(new double[] { 1.0d, 2.0d, 3.0d }, result);

        result = DoubleStream.merge(N.toList(DoubleStream.of(a), DoubleStream.of(b)), (p1, p2) -> p1 <= p2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new double[] { 1.0d, 2.0d, 3.0d, 4.0d, 5.0d, 6.0d }, result);

        result = DoubleStream
                .merge(N.toList(DoubleStream.of(a), DoubleStream.of(b), DoubleStream.of(c)),
                        (p1, p2) -> p1 <= p2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new double[] { 1.0d, 2.0d, 3.0d, 4.0d, 5.0d, 6.0d, 7.0d, 8.0d, 9.0d }, result);

    }

    @Test
    public void testMerge_CollectionOfStreams() {
        List<DoubleStream> streams = Arrays.asList(DoubleStream.of(1.0, 3.0, 5.0), DoubleStream.of(2.0, 4.0, 6.0));
        double[] result = DoubleStream.merge(streams, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 }, result, 0.001);
    }

    @Test
    public void testMerge_ThreeArrays() {
        double[] result = DoubleStream
                .merge(new double[] { 1.0, 4.0 }, new double[] { 2.0, 5.0 }, new double[] { 3.0, 6.0 },
                        (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 }, result, 0.001);
    }

    @Test
    public void testMerge_ThreeIterators() {
        double[] result = DoubleStream
                .merge(DoubleIterator.of(new double[] { 1.0, 4.0 }), DoubleIterator.of(new double[] { 2.0, 5.0 }), DoubleIterator.of(new double[] { 3.0, 6.0 }),
                        (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 }, result, 0.001);
    }

    @Test
    public void testMergeArrays() {
        double[] a = { 1.0, 3.0, 5.0 };
        double[] b = { 2.0, 4.0, 6.0 };
        DoubleStream stream = DoubleStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testMergeCollectionOfStreams() {
        Collection<DoubleStream> streams = Arrays.asList(DoubleStream.of(1.0, 3.0), DoubleStream.of(2.0, 4.0));
        DoubleStream stream = DoubleStream.merge(streams, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testMergeCollectionTreatsNullStreamAsEmpty() {
        assertEquals(0, DoubleStream.merge(Arrays.asList((DoubleStream) null), (a, b) -> MergeResult.TAKE_FIRST).count());
    }

    @Test
    public void testMergeIterators() {
        DoubleIterator a = DoubleIterator.of(new double[] { 1.0, 3.0, 5.0 });
        DoubleIterator b = DoubleIterator.of(new double[] { 2.0, 4.0, 6.0 });
        DoubleStream stream = DoubleStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testMergeIteratorThreeWay() {
        DoubleIterator a = DoubleIterator.of(new double[] { 1.0, 4.0, 7.0 });
        DoubleIterator b = DoubleIterator.of(new double[] { 2.0, 5.0, 8.0 });
        DoubleIterator c = DoubleIterator.of(new double[] { 3.0, 6.0, 9.0 });
        double[] result = DoubleStream.merge(a, b, c, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0 }, result, 0.001);
    }

    @Test
    public void testMergeStreams() {
        DoubleStream a = DoubleStream.of(1.0, 3.0, 5.0);
        DoubleStream b = DoubleStream.of(2.0, 4.0, 6.0);
        DoubleStream stream = DoubleStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testMergeStreamsThreeWay() {
        double[] result = DoubleStream
                .merge(DoubleStream.of(1.0, 4.0, 7.0), DoubleStream.of(2.0, 5.0, 8.0), DoubleStream.of(3.0, 6.0, 9.0),
                        (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0 }, result, 0.001);
    }

    @Test
    public void testMergeThreeArraysAdditional() {
        double[] a1 = { 1.0, 7.0 };
        double[] a2 = { 3.0, 8.0 };
        double[] a3 = { 5.0, 9.0 };
        DoubleStream stream = DoubleStream.merge(a1, a2, a3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(6, stream.count());
    }

    @Test
    public void testMergeThreeStreamsAdditional() {
        DoubleStream s1 = DoubleStream.of(1.0, 7.0);
        DoubleStream s2 = DoubleStream.of(3.0, 8.0);
        DoubleStream s3 = DoubleStream.of(5.0, 9.0);
        DoubleStream result = DoubleStream.merge(s1, s2, s3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(6, result.count());
    }

    @Test
    public void testMergeWithCollection() {
        Collection<DoubleStream> streams = Arrays.asList(DoubleStream.of(1.0, 5.0), DoubleStream.of(2.0, 6.0), DoubleStream.of(3.0, 7.0));
        DoubleStream result = DoubleStream.merge(streams, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(6, result.count());
    }

    @Test
    public void testMergeListOfIterators() {
        DoubleIterator iter1 = DoubleIterator.of(new double[] { 1.0, 3.0 });
        DoubleIterator iter2 = DoubleIterator.of(new double[] { 2.0, 4.0 });
        DoubleIterator iter3 = DoubleIterator.of(new double[] { 5.0, 6.0 });
        Collection<DoubleIterator> iters = Arrays.asList(iter1, iter2, iter3);
        DoubleStream stream = DoubleStream.concatIterators(iters);
        assertEquals(6, stream.count());
    }

    @Test
    public void testMergeWith() {
        DoubleStream stream1 = DoubleStream.of(1.0, 3.0, 5.0);
        DoubleStream stream2 = DoubleStream.of(2.0, 4.0, 6.0);
        double[] result = stream1.mergeWith(stream2, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 }, result, 0.0001);
    }

    @Test
    public void testPrepend() {
        DoubleStream stream = DoubleStream.of(3.0, 4.0);
        double[] result = stream.prepend(1.0, 2.0).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, result, 0.0001);
    }

    @Test
    public void testPrependArray() {
        DoubleStream stream = createDoubleStream(3.0, 4.0, 5.0);
        double[] result = stream.prepend(1.0, 2.0).toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, result, 0.0001);
    }

    // zip

    @Test
    public void test_zip_02() {
        double[] a = { 1.0d };
        double[] b = { 4.0d, 5.0d };
        double[] c = { 7.0d, 8.0d, 9.0d };
        double[] result = DoubleStream.zip(a, b, c, 100, 200, 300, (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new double[] { 12.0d, 113.0d, 309.0d }, result);
    }

    @Test
    public void testZipArrays() {
        double[] a = { 1.0, 2.0, 3.0 };
        double[] b = { 4.0, 5.0, 6.0 };
        DoubleStream stream = DoubleStream.zip(a, b, (x, y) -> x + y);
        assertArrayEquals(new double[] { 5.0, 7.0, 9.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testZipArraysWithDefaults() {
        double[] a = { 1.0, 2.0 };
        double[] b = { 3.0, 4.0, 5.0 };
        DoubleStream stream = DoubleStream.zip(a, b, 10.0, 20.0, (x, y) -> x + y);
        assertArrayEquals(new double[] { 4.0, 6.0, 15.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testZipCollection() {
        Collection<DoubleStream> streams = Arrays.asList(DoubleStream.of(1.0, 2.0), DoubleStream.of(3.0, 4.0), DoubleStream.of(5.0, 6.0));
        DoubleStream stream = DoubleStream.zip(streams, array -> Arrays.stream(array).sum());
        assertArrayEquals(new double[] { 9.0, 12.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testZipCollectionOfStreams() {
        List<DoubleStream> streams = Arrays.asList(DoubleStream.of(1.0, 2.0), DoubleStream.of(10.0, 20.0));
        double[] result = DoubleStream.zip(streams, values -> values[0] + values[1]).toArray();
        assertArrayEquals(new double[] { 11.0, 22.0 }, result, 0.001);
    }

    @Test
    public void testZipCollectionWithDefaults() {
        Collection<DoubleStream> streams = Arrays.asList(DoubleStream.of(1.0), DoubleStream.of(2.0, 3.0));
        double[] defaults = new double[] { 100.0, 200.0 };
        DoubleStream stream = DoubleStream.zip(streams, defaults, array -> Arrays.stream(array).sum());
        assertEquals(2, stream.count());
    }

    @Test
    public void testZipIterators() {
        DoubleIterator a = DoubleIterator.of(new double[] { 1.0, 2.0, 3.0 });
        DoubleIterator b = DoubleIterator.of(new double[] { 4.0, 5.0, 6.0 });
        DoubleStream stream = DoubleStream.zip(a, b, (x, y) -> x * y);
        assertArrayEquals(new double[] { 4.0, 10.0, 18.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testZipIteratorsThreeWay() {
        DoubleIterator a = DoubleIterator.of(new double[] { 1.0, 2.0 });
        DoubleIterator b = DoubleIterator.of(new double[] { 10.0, 20.0 });
        DoubleIterator c = DoubleIterator.of(new double[] { 100.0, 200.0 });
        double[] result = DoubleStream.zip(a, b, c, (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new double[] { 111.0, 222.0 }, result, 0.001);
    }

    @Test
    public void testZipIteratorsThreeWayWithDefaults() {
        DoubleIterator a = DoubleIterator.of(new double[] { 1.0 });
        DoubleIterator b = DoubleIterator.of(new double[] { 10.0, 20.0 });
        DoubleIterator c = DoubleIterator.of(new double[] { 100.0, 200.0, 300.0 });
        double[] result = DoubleStream.zip(a, b, c, 0.0, 0.0, 0.0, (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new double[] { 111.0, 220.0, 300.0 }, result, 0.001);
    }

    @Test
    public void testZipIteratorsWithDefaults() {
        DoubleIterator a = DoubleIterator.of(new double[] { 1.0, 2.0, 3.0 });
        DoubleIterator b = DoubleIterator.of(new double[] { 10.0, 20.0 });
        double[] result = DoubleStream.zip(a, b, 0.0, 0.0, Double::sum).toArray();
        assertArrayEquals(new double[] { 11.0, 22.0, 3.0 }, result, 0.001);
    }

    @Test
    public void testZipStreams() {
        DoubleStream a = DoubleStream.of(1.0, 2.0, 3.0);
        DoubleStream b = DoubleStream.of(4.0, 5.0, 6.0);
        DoubleStream stream = DoubleStream.zip(a, b, (x, y) -> x - y);
        assertArrayEquals(new double[] { -3.0, -3.0, -3.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testZipStreamsThreeWayWithDefaults() {
        double[] result = DoubleStream
                .zip(DoubleStream.of(1.0), DoubleStream.of(10.0, 20.0), DoubleStream.of(100.0, 200.0, 300.0), 0.0, 0.0, 0.0, (x, y, z) -> x + y + z)
                .toArray();
        assertArrayEquals(new double[] { 111.0, 220.0, 300.0 }, result, 0.001);
    }

    @Test
    public void testZipStreamsWithDefaults() {
        DoubleStream a = DoubleStream.of(1.0, 2.0);
        DoubleStream b = DoubleStream.of(3.0, 4.0, 5.0);
        DoubleStream stream = DoubleStream.zip(a, b, 10.0, 20.0, (x, y) -> x + y);
        assertArrayEquals(new double[] { 4.0, 6.0, 15.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testZipThreeArrays() {
        double[] a = { 1.0, 2.0 };
        double[] b = { 3.0, 4.0 };
        double[] c = { 5.0, 6.0 };
        DoubleStream stream = DoubleStream.zip(a, b, c, (x, y, z) -> x + y + z);
        assertArrayEquals(new double[] { 9.0, 12.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testZipThreeArraysWithDefaults() {
        double[] a = { 1.0 };
        double[] b = { 2.0, 3.0 };
        double[] c = { 4.0, 5.0, 6.0 };
        DoubleStream stream = DoubleStream.zip(a, b, c, 100.0, 200.0, 300.0, (x, y, z) -> x + y + z);
        assertEquals(3, stream.count());
    }

    @Test
    public void testZipThreeArraysWithDefaultsAdditional() {
        double[] a = { 1.0 };
        double[] b = { 2.0, 3.0 };
        double[] c = { 4.0, 5.0, 6.0 };
        DoubleStream stream = DoubleStream.zip(a, b, c, 100.0, 200.0, 300.0, (x, y, z) -> x + y + z);
        double[] result = stream.toArray();
        assertEquals(3, result.length);
    }

    @Test
    public void testZipThreeIterators() {
        DoubleIterator a = DoubleIterator.of(new double[] { 1.0, 2.0 });
        DoubleIterator b = DoubleIterator.of(new double[] { 3.0, 4.0 });
        DoubleIterator c = DoubleIterator.of(new double[] { 5.0, 6.0 });
        DoubleStream stream = DoubleStream.zip(a, b, c, (x, y, z) -> x + y + z);
        assertArrayEquals(new double[] { 9.0, 12.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testZipThreeIteratorsWithDefaultsAdditional() {
        DoubleIterator a = DoubleIterator.of(new double[] { 1.0 });
        DoubleIterator b = DoubleIterator.of(new double[] { 2.0, 3.0 });
        DoubleIterator c = DoubleIterator.of(new double[] { 4.0, 5.0, 6.0 });
        DoubleStream stream = DoubleStream.zip(a, b, c, 100.0, 200.0, 300.0, (x, y, z) -> x + y + z);
        assertEquals(3, stream.count());
    }

    @Test
    public void testZipThreeStreams() {
        DoubleStream a = DoubleStream.of(1.0, 2.0);
        DoubleStream b = DoubleStream.of(3.0, 4.0);
        DoubleStream c = DoubleStream.of(5.0, 6.0);
        DoubleStream stream = DoubleStream.zip(a, b, c, (x, y, z) -> x + y + z);
        assertArrayEquals(new double[] { 9.0, 12.0 }, stream.toArray(), 0.0001);
    }

    @Test
    public void testZipThreeStreamsWithDefaults() {
        DoubleStream a = DoubleStream.of(1.0);
        DoubleStream b = DoubleStream.of(2.0, 3.0);
        DoubleStream c = DoubleStream.of(4.0, 5.0, 6.0);
        DoubleStream stream = DoubleStream.zip(a, b, c, 100.0, 200.0, 300.0, (x, y, z) -> x + y + z);
        assertEquals(3, stream.count());
    }

    @Test
    public void testZipThreeStreamsWithDefaultsAdditional() {
        DoubleStream a = DoubleStream.of(1.0);
        DoubleStream b = DoubleStream.of(2.0, 3.0);
        DoubleStream c = DoubleStream.of(4.0, 5.0, 6.0);
        DoubleStream stream = DoubleStream.zip(a, b, c, 100.0, 200.0, 300.0, (x, y, z) -> x + y + z);
        double[] result = stream.toArray();
        assertEquals(3, result.length);
    }

    @Test
    public void testZipWith_CollectionOfStreams() {
        List<DoubleStream> streams = Arrays.asList(DoubleStream.of(1.0, 2.0, 3.0), DoubleStream.of(10.0, 20.0, 30.0));
        double[] result = DoubleStream.zip(streams, args -> args[0] + args[1]).toArray();
        assertArrayEquals(new double[] { 11.0, 22.0, 33.0 }, result, 0.001);
    }

    @Test
    public void testZipWith_CollectionOfStreamsWithDefaults() {
        List<DoubleStream> streams = Arrays.asList(DoubleStream.of(1.0, 2.0), DoubleStream.of(10.0, 20.0, 30.0));
        double[] result = DoubleStream.zip(streams, new double[] { 0.0, 0.0 }, args -> args[0] + args[1]).toArray();
        assertArrayEquals(new double[] { 11.0, 22.0, 30.0 }, result, 0.001);
    }

    @Test
    public void testZipWithCollectionDefaultValues() {
        Collection<DoubleStream> streams = Arrays.asList(DoubleStream.of(1.0, 2.0), DoubleStream.of(10.0));
        double[] defaults = new double[] { 0.0, 0.0 };
        DoubleStream result = DoubleStream.zip(streams, defaults, doubles -> {
            double sum = 0.0;
            for (Double d : doubles) {
                sum += d;
            }
            return sum;
        });
        assertEquals(2, result.count());
    }



    // Additional coverage tests for 2025
    @Test
    public void testZipWithCollectionOfStreams() {
        Collection<DoubleStream> streams = Arrays.asList(DoubleStream.of(1.0, 2.0), DoubleStream.of(10.0, 20.0), DoubleStream.of(100.0, 200.0));
        DoubleStream result = DoubleStream.zip(streams, doubles -> {
            double sum = 0.0;
            for (Double d : doubles) {
                sum += d;
            }
            return sum;
        });
        assertArrayEquals(new double[] { 111.0, 222.0 }, result.toArray(), 0.0001);
    }

    // zipWith

    @Test
    public void testZipWith() {
        DoubleStream stream1 = DoubleStream.of(1.0, 2.0, 3.0);
        DoubleStream stream2 = DoubleStream.of(4.0, 5.0, 6.0);
        double[] result = stream1.zipWith(stream2, (a, b) -> a + b).toArray();
        assertArrayEquals(new double[] { 5.0, 7.0, 9.0 }, result, 0.0001);
    }

    @Test
    public void testZipWithDefaults() {
        DoubleStream stream1 = DoubleStream.of(1.0, 2.0);
        DoubleStream stream2 = DoubleStream.of(3.0, 4.0, 5.0);
        double[] result = stream1.zipWith(stream2, 10.0, 20.0, (a, b) -> a + b).toArray();
        assertArrayEquals(new double[] { 4.0, 6.0, 15.0 }, result, 0.0001);
    }

    @Test
    public void testZipWithDefaultsThreeStreams() {
        DoubleStream stream1 = DoubleStream.of(1.0);
        DoubleStream stream2 = DoubleStream.of(2.0, 3.0);
        DoubleStream stream3 = DoubleStream.of(4.0, 5.0, 6.0);
        double[] result = stream1.zipWith(stream2, stream3, 100.0, 200.0, 300.0, (a, b, c) -> a + b + c).toArray();
        assertEquals(3, result.length);
    }

    @Test
    public void testZipWithThreeStreams() {
        DoubleStream stream1 = DoubleStream.of(1.0, 2.0);
        DoubleStream stream2 = DoubleStream.of(3.0, 4.0);
        DoubleStream stream3 = DoubleStream.of(5.0, 6.0);
        double[] result = stream1.zipWith(stream2, stream3, (a, b, c) -> a + b + c).toArray();
        assertArrayEquals(new double[] { 9.0, 12.0 }, result, 0.0001);
    }

}
