package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.FloatList;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.u.OptionalFloat;

/** FloatStream combination contracts. */
public class FloatStreamCombinationTest extends FloatStreamTestSupport {

    // append

    @Test
    @DisplayName("append() should add elements to end")
    public void testAppend() {
        FloatStream stream = createFloatStream(1.0f, 2.0f);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.append(3.0f, 4.0f).toArray());
    }

    @Test
    public void testAppend_Empty() {
        float[] result = FloatStream.empty().append(1.0f, 2.0f).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f }, result, DELTA);
    }

    @Test
    public void testAppendOptional() {
        float[] result = createFloatStream(1.0f, 2.0f).append(OptionalFloat.of(3.0f)).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result, DELTA);
    }

    @Test
    public void testAppendOptional_Empty() {
        float[] result = createFloatStream(1.0f, 2.0f).append(OptionalFloat.empty()).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f }, result, DELTA);
    }

    @Test
    public void testAppendStream() {
        float[] result = createFloatStream(1.0f, 2.0f).append(FloatStream.of(3.0f, 4.0f)).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result, DELTA);
    }

    // appendIfEmpty

    @Test
    @DisplayName("appendIfEmpty() should append only if stream is empty")
    public void testAppendIfEmpty() {
        FloatStream stream1 = createFloatStream(1.0f, 2.0f);
        assertArrayEquals(new float[] { 1.0f, 2.0f }, stream1.appendIfEmpty(99.0f).toArray());

        FloatStream stream2 = FloatStream.empty();
        assertArrayEquals(new float[] { 99.0f }, stream2.appendIfEmpty(99.0f).toArray());
    }

    @Test
    public void testAppendIfEmptySupplier() {
        float[] result = FloatStream.empty().appendIfEmpty(() -> FloatStream.of(10.0f, 20.0f)).toArray();
        assertArrayEquals(new float[] { 10.0f, 20.0f }, result, DELTA);
    }

    @Test
    public void testAppendIfEmptySupplier_NonEmpty() {
        float[] result = createFloatStream(1.0f, 2.0f).appendIfEmpty(() -> FloatStream.of(10.0f, 20.0f)).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f }, result, DELTA);
    }

    // concat

    @Test
    public void testConcat() {
        FloatStream s1 = FloatStream.of(1.1f, 2.2f);
        FloatStream s2 = FloatStream.of(3.3f, 4.4f);
        assertArrayEquals(new float[] { 1.1f, 2.2f, 3.3f, 4.4f }, FloatStream.concat(s1, s2).toArray(), DELTA);
    }

    @Test
    public void test_concat_02() {
        float[] a = { 1.0f, 2.0f, 3.0f };
        float[] b = { 4.0f, 5.0f, 6.0f };
        float[] result = FloatStream.concat(a, b).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f }, result);

        result = FloatStream.concat(N.toList(a, b)).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f }, result);

        result = FloatStream.concatIterators(N.toList(FloatIterator.of(a), FloatIterator.of(b))).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f }, result);
    }

    @Test
    @DisplayName("concat() with arrays should concatenate")
    public void testConcatArrays() {
        float[] a = { 1.0f, 2.0f };
        float[] b = { 3.0f, 4.0f };
        FloatStream stream = FloatStream.concat(a, b);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.toArray());
    }

    @Test
    public void testConcatArrays_Test() {
        float[] result = FloatStream.concat(new float[] { 1.0f, 2.0f }, new float[] { 3.0f, 4.0f }).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result, DELTA);
    }

    @Test
    @DisplayName("concat() with collection of streams should concatenate")
    public void testConcatCollectionOfStreams() {
        List<FloatStream> list = Arrays.asList(FloatStream.of(1.0f, 2.0f), FloatStream.of(3.0f, 4.0f));
        FloatStream stream = FloatStream.concat(list);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.toArray());
    }

    @Test
    public void testConcatCollectionOfStreams_Test() {
        List<FloatStream> streams = Arrays.asList(FloatStream.of(1.0f, 2.0f), FloatStream.of(3.0f, 4.0f));
        float[] result = FloatStream.concat(streams).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result, DELTA);
    }

    @Test
    public void testConcatCollectionSnapshotsSources() {
        List<FloatStream> sources = new ArrayList<>(Arrays.asList(FloatStream.of(1.0f, 2.0f), FloatStream.of(3.0f, 4.0f)));
        FloatStream concatenated = FloatStream.concat(sources);

        sources.clear();

        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, concatenated.toArray());
    }

    @Test
    @DisplayName("concat() with iterators should concatenate")
    public void testConcatIterators() {
        FloatIterator a = FloatIterator.of(1.0f, 2.0f);
        FloatIterator b = FloatIterator.of(3.0f, 4.0f);
        FloatStream stream = FloatStream.concat(a, b);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.toArray());
    }

    @Test
    public void testConcatIterators_Test() {
        float[] result = FloatStream.concat(FloatIterator.of(new float[] { 1.0f, 2.0f }), FloatIterator.of(new float[] { 3.0f, 4.0f })).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result, DELTA);
    }

    @Test
    @DisplayName("concat() with list of arrays should concatenate")
    public void testConcatListOfArrays() {
        List<float[]> list = Arrays.asList(new float[] { 1.0f, 2.0f }, new float[] { 3.0f, 4.0f });
        FloatStream stream = FloatStream.concat(list);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.toArray());
    }

    // G112-01: bulk count()/toFloatList()/toArray() of concat(List<float[]>) / concat(float[]...) - null/empty arrays, skip into/at/after a
    // segment, short (element-wise) and long (bulk copy) segments, fresh results, lazy read of the list, same list traversal calls
    @Test
    public void testConcatListOfArrays_bulkOpsMatchElementwise_G112() {
        final float[] a = new float[20];
        final float[] b = new float[40];
        final float[] c = { 1, 2, 3 };
        for (int i = 0; i < a.length; i++) {
            a[i] = 10 + i;
        }
        for (int i = 0; i < b.length; i++) {
            b[i] = 100 + i;
        }
        final List<float[]> list = Arrays.asList(null, a, null, new float[0], c, b, c, new float[0]);
        final float[] all = new float[66];
        System.arraycopy(a, 0, all, 0, 20);
        System.arraycopy(c, 0, all, 20, 3);
        System.arraycopy(b, 0, all, 23, 40);
        System.arraycopy(c, 0, all, 63, 3);

        for (final int n : new int[] { 0, 1, 5, 19, 20, 21, 22, 23, 24, 40, 62, 63, 64, 65, 66, 70 }) {
            final float[] expected = Arrays.copyOfRange(all, Math.min(n, all.length), all.length);
            assertArrayEquals(expected, FloatStream.concat(list).skip(n).toArray(), "skip " + n);
            assertEquals(expected.length, FloatStream.concat(list).skip(n).count(), "skip " + n);
            final FloatList xl = FloatStream.concat(list).skip(n).toFloatList();
            assertArrayEquals(expected, xl.toArray(), "skip " + n);
            xl.add(7);
            assertEquals(expected.length + 1, xl.size());
        }

        assertArrayEquals(all, FloatStream.concat(a, c, b, c).toArray());
        assertEquals(66, FloatStream.concat(a, null, c, b, c).count());

        final List<float[]> empties = Arrays.asList(null, new float[0]);
        assertEquals(0, FloatStream.concat(empties).toArray().length);
        assertEquals(0, FloatStream.concat(empties).count());
        final FloatList emptyList = FloatStream.concat(empties).toFloatList();
        emptyList.add(9);
        assertEquals(1, emptyList.size());

        final float[] result = FloatStream.concat(Arrays.asList(b)).toArray();
        assertArrayEquals(b, result);
        assertFalse(result == b);
        result[0] = 0;
        assertEquals(100, b[0]);

        // the list is read when the terminal operation runs; close handlers still run
        final List<float[]> live = new ArrayList<>(Arrays.asList(new float[] { 1 }, new float[] { 2 }));
        final FloatStream stream = FloatStream.concat(live);
        live.set(1, b);
        final AtomicInteger closed = new AtomicInteger();
        assertEquals(41, stream.onClose(closed::incrementAndGet).count());
        assertEquals(1, closed.get());

        // partially consumed iterator, then drained in bulk
        final FloatIterator iter = FloatStream.concat(list).iterator();
        assertEquals(10, iter.nextFloat());
        assertArrayEquals(Arrays.copyOfRange(all, 1, all.length), iter.toArray());
        assertFalse(iter.hasNext());

        // the list iterator is advanced exactly as element-by-element iteration advances it
        final int[] calls = new int[2];
        final List<float[]> counting = new ArrayList<>(list) {
            private static final long serialVersionUID = 1L;

            @Override
            public java.util.Iterator<float[]> iterator() {
                final java.util.Iterator<float[]> it = super.iterator();

                return new java.util.Iterator<>() {
                    @Override
                    public boolean hasNext() {
                        calls[0]++;
                        return it.hasNext();
                    }

                    @Override
                    public float[] next() {
                        calls[1]++;
                        return it.next();
                    }
                };
            }
        };

        assertArrayEquals(all, FloatStream.concat(counting).toArray());
        assertEquals(9, calls[0]);
        assertEquals(8, calls[1]);
        calls[0] = calls[1] = 0;
        assertEquals(66, FloatStream.concat(counting).count());
        assertEquals(9, calls[0]);
        assertEquals(8, calls[1]);
        calls[0] = calls[1] = 0;
        assertEquals(66, FloatStream.concat(counting).toFloatList().size());
        assertEquals(9, calls[0]);
        assertEquals(8, calls[1]);
    }

    @Test
    public void testConcatListOfArrays_Test() {
        List<float[]> arrays = Arrays.asList(new float[] { 1.0f, 2.0f }, new float[] { 3.0f, 4.0f });
        float[] result = FloatStream.concat(arrays).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result, DELTA);
    }

    @Test
    @DisplayName("concat() with streams should concatenate")
    public void testConcatStreams() {
        FloatStream a = FloatStream.of(1.0f, 2.0f);
        FloatStream b = FloatStream.of(3.0f, 4.0f);
        FloatStream stream = FloatStream.concat(a, b);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.toArray());
    }

    @Test
    public void testConcatStreams_Test() {
        float[] result = FloatStream.concat(FloatStream.of(1.0f, 2.0f), FloatStream.of(3.0f, 4.0f)).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result, DELTA);
    }

    @Test
    @DisplayName("concatIterators() should concatenate iterators")
    public void testConcatIteratorsCollection() {
        List<FloatIterator> list = Arrays.asList(FloatIterator.of(1.0f, 2.0f), FloatIterator.of(3.0f, 4.0f));
        FloatStream stream = FloatStream.concatIterators(list);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.toArray());
    }

    @Test
    public void testConcatIteratorsCollection_Test() {
        List<FloatIterator> iterators = Arrays.asList(FloatIterator.of(new float[] { 1.0f, 2.0f }), FloatIterator.of(new float[] { 3.0f, 4.0f }));
        float[] result = FloatStream.concatIterators(iterators).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result, DELTA);
    }

    // join

    @Test
    @DisplayName("join() with delimiter")
    public void testJoin() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        assertEquals("1.0, 2.0, 3.0", stream.join(", "));
    }

    @Test
    public void testJoin_Empty() {
        String result = FloatStream.empty().join(", ");
        assertEquals("", result);
    }

    @Test
    public void testJoin_WithPrefixSuffix() {
        String result = createFloatStream(1.0f, 2.0f, 3.0f).join(", ", "[", "]");
        assertTrue(result.startsWith("["));
        assertTrue(result.endsWith("]"));
    }

    @Test
    @DisplayName("join() with prefix and suffix")
    public void testJoinWithPrefixSuffix() {
        FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
        assertEquals("[1.0, 2.0, 3.0]", stream.join(", ", "[", "]"));
    }

    @Test
    public void testJoinTo() {
        Joiner joiner = Joiner.with(" | ");
        FloatStream.of(1.1f, 2.2f, 3.3f).joinTo(joiner);
        assertEquals("1.1 | 2.2 | 3.3", joiner.toString());
    }

    // merge

    @Test
    public void testMerge() {
        float[] a = { 1.1f, 3.3f, 5.5f };
        float[] b = { 2.2f, 4.4f, 6.6f };
        float[] expected = { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f, 6.6f };
        assertArrayEquals(expected, FloatStream.merge(a, b, (f1, f2) -> f1 <= f2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray(), DELTA);
    }

    @Test
    public void test_merge() {
        float[] a = { 1.0f, 2.0f, 3.0f };
        float[] b = { 4.0f, 5.0f, 6.0f };
        float[] c = { 7.0f, 8.0f, 9.0f };
        float[] result = FloatStream.merge(N.toList(FloatStream.of(a)), (p1, p2) -> p1 <= p2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result);

        result = FloatStream.merge(N.toList(FloatStream.of(a), FloatStream.of(b)), (p1, p2) -> p1 <= p2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f }, result);

        result = FloatStream
                .merge(N.toList(FloatStream.of(a), FloatStream.of(b), FloatStream.of(c)),
                        (p1, p2) -> p1 <= p2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f }, result);

    }

    @Test
    @DisplayName("merge() collection of streams additional")
    public void testMergeCollectionAdditional() {
        Collection<FloatStream> streams = Arrays.asList(FloatStream.of(1.0f, 7.0f), FloatStream.of(3.0f, 8.0f), FloatStream.of(5.0f, 9.0f));
        FloatStream result = FloatStream.merge(streams, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(6, result.count());
    }

    @Test
    @DisplayName("merge() collection of streams")
    public void testMergeCollectionOfStreams() {
        List<FloatStream> streams = Arrays.asList(FloatStream.of(1.0f, 3.0f), FloatStream.of(2.0f, 4.0f));
        FloatStream stream = FloatStream.merge(streams, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.toArray());
    }

    @Test
    public void testMergeCollectionOfStreams_Test() {
        List<FloatStream> streams = Arrays.asList(FloatStream.of(1.0f, 3.0f), FloatStream.of(2.0f, 4.0f));
        float[] result = FloatStream.merge(streams, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result, DELTA);
    }

    @Test
    public void testMergeCollectionTreatsNullStreamAsEmpty() {
        assertEquals(0, FloatStream.merge(Arrays.asList((FloatStream) null), (a, b) -> MergeResult.TAKE_FIRST).count());
    }

    @Test
    @DisplayName("merge() three arrays")
    public void testMergeThreeArrays() {
        float[] a = { 1.0f, 4.0f };
        float[] b = { 2.0f, 5.0f };
        float[] c = { 3.0f, 6.0f };
        FloatStream stream = FloatStream.merge(a, b, c, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f }, stream.toArray());
    }

    @Test
    public void testMergeThreeArrays_Test() {
        float[] result = FloatStream
                .merge(new float[] { 1.0f, 4.0f }, new float[] { 2.0f, 5.0f }, new float[] { 3.0f, 6.0f },
                        (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f }, result, DELTA);
    }

    @Test
    @DisplayName("merge() three arrays additional")
    public void testMergeThreeArraysAdditional() {
        float[] a1 = { 1.0f, 10.0f };
        float[] a2 = { 4.0f, 11.0f };
        float[] a3 = { 7.0f, 12.0f };
        FloatStream stream = FloatStream.merge(a1, a2, a3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(6, stream.count());
    }

    @Test
    @DisplayName("merge() three iterators")
    public void testMergeThreeIterators() {
        FloatIterator a = FloatIterator.of(1.0f, 4.0f);
        FloatIterator b = FloatIterator.of(2.0f, 5.0f);
        FloatIterator c = FloatIterator.of(3.0f, 6.0f);
        FloatStream stream = FloatStream.merge(a, b, c, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f }, stream.toArray());
    }

    @Test
    public void testMergeThreeIterators_Test() {
        float[] result = FloatStream
                .merge(FloatIterator.of(new float[] { 1.0f }), FloatIterator.of(new float[] { 2.0f }), FloatIterator.of(new float[] { 3.0f }),
                        (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result, DELTA);
    }

    @Test
    @DisplayName("merge() three streams")
    public void testMergeThreeStreams() {
        FloatStream a = FloatStream.of(1.0f, 4.0f);
        FloatStream b = FloatStream.of(2.0f, 5.0f);
        FloatStream c = FloatStream.of(3.0f, 6.0f);
        FloatStream stream = FloatStream.merge(a, b, c, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f }, stream.toArray());
    }

    @Test
    public void testMergeThreeStreams_Test() {
        float[] result = FloatStream
                .merge(FloatStream.of(1.0f), FloatStream.of(2.0f), FloatStream.of(3.0f), (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result, DELTA);
    }

    @Test
    @DisplayName("merge() three streams additional")
    public void testMergeThreeStreamsAdditional() {
        FloatStream s1 = FloatStream.of(1.0f, 15.0f);
        FloatStream s2 = FloatStream.of(8.0f, 16.0f);
        FloatStream s3 = FloatStream.of(12.0f, 17.0f);
        FloatStream stream = FloatStream.merge(s1, s2, s3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(6, stream.count());
    }

    @Test
    @DisplayName("merge() two arrays")
    public void testMergeTwoArrays() {
        float[] a = { 1.0f, 3.0f, 5.0f };
        float[] b = { 2.0f, 4.0f, 6.0f };
        FloatStream stream = FloatStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f }, stream.toArray());
    }

    @Test
    public void testMergeTwoArrays_Test() {
        float[] result = FloatStream
                .merge(new float[] { 1.0f, 3.0f, 5.0f }, new float[] { 2.0f, 4.0f, 6.0f }, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f }, result, DELTA);
    }

    @Test
    @DisplayName("merge() two iterators")
    public void testMergeTwoIterators() {
        FloatIterator a = FloatIterator.of(1.0f, 3.0f, 5.0f);
        FloatIterator b = FloatIterator.of(2.0f, 4.0f, 6.0f);
        FloatStream stream = FloatStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f }, stream.toArray());
    }

    @Test
    public void testMergeTwoIterators_Test() {
        float[] result = FloatStream
                .merge(FloatIterator.of(new float[] { 1.0f, 3.0f }), FloatIterator.of(new float[] { 2.0f, 4.0f }),
                        (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result, DELTA);
    }

    @Test
    @DisplayName("merge() two streams")
    public void testMergeTwoStreams() {
        FloatStream a = FloatStream.of(1.0f, 3.0f, 5.0f);
        FloatStream b = FloatStream.of(2.0f, 4.0f, 6.0f);
        FloatStream stream = FloatStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f }, stream.toArray());
    }

    @Test
    public void testMergeTwoStreams_Test() {
        float[] result = FloatStream
                .merge(FloatStream.of(1.0f, 3.0f), FloatStream.of(2.0f, 4.0f), (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result, DELTA);
    }

    // mergeWith

    @Test
    @DisplayName("mergeWith() should merge streams")
    public void testMergeWith() {
        FloatStream stream1 = createFloatStream(1.0f, 3.0f, 5.0f);
        FloatStream stream2 = createFloatStream(2.0f, 4.0f, 6.0f);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f },
                stream1.mergeWith(stream2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray());
    }

    @Test
    public void testMergeWith_Empty() {
        float[] result = createFloatStream(1.0f, 3.0f, 5.0f).mergeWith(FloatStream.empty(), (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new float[] { 1.0f, 3.0f, 5.0f }, result, DELTA);
    }

    @Test
    public void testMergeWith_Test() {
        float[] result = createFloatStream(1.0f, 3.0f, 5.0f)
                .mergeWith(FloatStream.of(2.0f, 4.0f, 6.0f), (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f }, result, DELTA);
    }

    // prepend

    @Test
    @DisplayName("prepend() should add elements to beginning")
    public void testPrepend() {
        FloatStream stream = createFloatStream(3.0f, 4.0f);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.prepend(1.0f, 2.0f).toArray());
    }

    @Test
    public void testPrepend_Empty() {
        float[] result = FloatStream.empty().prepend(1.0f, 2.0f).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f }, result, DELTA);
    }

    @Test
    public void testPrependAndAppend() {
        OptionalFloat opf = OptionalFloat.of(99.9f);
        FloatStream other = FloatStream.of(0.1f, 0.2f);

        float[] result = FloatStream.of(1.1f, 2.2f).prepend(opf).append(-1.1f, -2.2f).append(other).toArray();

        float[] expected = { 99.9f, 1.1f, 2.2f, -1.1f, -2.2f, 0.1f, 0.2f };
        assertArrayEquals(expected, result, DELTA);
    }

    @Test
    public void testPrependOptional() {
        float[] result = createFloatStream(2.0f, 3.0f).prepend(OptionalFloat.of(1.0f)).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result, DELTA);
    }

    @Test
    public void testPrependOptional_Empty() {
        float[] result = createFloatStream(2.0f, 3.0f).prepend(OptionalFloat.empty()).toArray();
        assertArrayEquals(new float[] { 2.0f, 3.0f }, result, DELTA);
    }

    @Test
    public void testPrependStream() {
        float[] result = createFloatStream(3.0f, 4.0f, 5.0f).prepend(FloatStream.of(1.0f, 2.0f)).toArray();
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, result, DELTA);
    }

    @Test
    public void testJoinWithJoiner() {
        Joiner joiner = Joiner.with(", ", "[", "]");
        createFloatStream(1.0f, 2.0f, 3.0f).joinTo(joiner);
        String result = joiner.toString();
        assertTrue(result.startsWith("["));
        assertTrue(result.endsWith("]"));
        assertTrue(result.contains("1.0"));
    }

    // zip

    @Test
    public void testZip() {
        float[] a = { 1.0f, 2.0f, 3.0f };
        float[] b = { 4.0f, 5.0f, 6.0f };
        assertArrayEquals(new float[] { 5.0f, 7.0f, 9.0f }, FloatStream.zip(a, b, (f1, f2) -> f1 + f2).toArray(), DELTA);
    }

    @Test
    public void testZip3() {
        float[] a = { 1f, 2f, 3f };
        float[] b = { 10f, 20f, 30f };
        float[] c = { 100f, 200f, 300f };
        float[] expected = { 111f, 222f, 333f };
        assertArrayEquals(expected, FloatStream.zip(a, b, c, (x, y, z) -> x + y + z).toArray(), DELTA);
    }

    @Test
    public void test_zip_02() {
        float[] a = { 1.0f };
        float[] b = { 4.0f, 5.0f };
        float[] c = { 7.0f, 8.0f, 9.0f };
        float[] result = FloatStream.zip(a, b, c, 100, 200, 300, (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new float[] { 12.0f, 113.0f, 309.0f }, result);
    }

    @Test
    @DisplayName("zip() collection with defaults additional")
    public void testZipCollectionDefaultsAdditional() {
        Collection<FloatStream> streams = Arrays.asList(FloatStream.of(1.0f, 2.0f, 3.0f), FloatStream.of(10.0f, 20.0f));
        float[] defaults = { 0.0f, 100.0f };
        FloatStream result = FloatStream.zip(streams, defaults, floats -> {
            float sum = 0.0f;
            for (Float f : floats) {
                sum += f;
            }
            return sum;
        });
        assertEquals(3, result.count());
    }

    @Test
    @DisplayName("zip() collection of streams with N-function")
    public void testZipCollectionOfStreams() {
        List<FloatStream> streams = Arrays.asList(FloatStream.of(1.0f, 2.0f), FloatStream.of(3.0f, 4.0f));
        FloatStream stream = FloatStream.zip(streams, floats -> floats[0] + floats[1]);
        assertArrayEquals(new float[] { 4.0f, 6.0f }, stream.toArray());
    }

    @Test
    public void testZipCollectionOfStreams_Test() {
        List<FloatStream> streams = Arrays.asList(FloatStream.of(1.0f, 2.0f), FloatStream.of(10.0f, 20.0f));
        float[] result = FloatStream.zip(streams, values -> values[0] + values[1]).toArray();
        assertArrayEquals(new float[] { 11.0f, 22.0f }, result, DELTA);
    }

    // Additional coverage improvement tests - Collection-based factory methods
    @Test
    @DisplayName("zip() collection of streams with function")
    public void testZipCollectionStreamsAdditional() {
        Collection<FloatStream> streams = Arrays.asList(FloatStream.of(1.0f, 2.0f), FloatStream.of(10.0f, 20.0f), FloatStream.of(100.0f, 200.0f));
        FloatStream result = FloatStream.zip(streams, floats -> {
            float sum = 0.0f;
            for (Float f : floats) {
                sum += f;
            }
            return sum;
        });
        assertArrayEquals(new float[] { 111.0f, 222.0f }, result.toArray());
    }

    @Test
    @DisplayName("zip() with padding for collection of streams")
    public void testZipCollectionWithPadding() {
        List<FloatStream> streams = Arrays.asList(FloatStream.of(1.0f, 2.0f), FloatStream.of(3.0f));
        float[] valuesForNone = { 0.0f, 100.0f };
        FloatStream stream = FloatStream.zip(streams, valuesForNone, floats -> floats[0] + floats[1]);
        assertArrayEquals(new float[] { 4.0f, 102.0f }, stream.toArray());
    }

    @Test
    public void testZipCollectionWithPadding_Test() {
        List<FloatStream> streams = Arrays.asList(FloatStream.of(1.0f, 2.0f, 3.0f), FloatStream.of(10.0f, 20.0f));
        float[] result = FloatStream.zip(streams, new float[] { 0.0f, 0.0f }, values -> values[0] + values[1]).toArray();
        assertArrayEquals(new float[] { 11.0f, 22.0f, 3.0f }, result, DELTA);
    }

    @Test
    @DisplayName("zip() with different lengths should stop at shorter")
    public void testZipDifferentLengths() {
        FloatStream stream1 = createFloatStream(1.0f, 2.0f);
        FloatStream stream2 = createFloatStream(4.0f, 5.0f, 6.0f);
        float[] result = FloatStream.zip(stream1, stream2, Float::sum).toArray();
        assertArrayEquals(new float[] { 5.0f, 7.0f }, result);
    }

    @Test
    @DisplayName("zip() three arrays with ternary function")
    public void testZipThreeArrays() {
        float[] a = { 1.0f, 2.0f };
        float[] b = { 3.0f, 4.0f };
        float[] c = { 5.0f, 6.0f };
        FloatStream stream = FloatStream.zip(a, b, c, (x, y, z) -> x + y + z);
        assertArrayEquals(new float[] { 9.0f, 12.0f }, stream.toArray());
    }

    @Test
    public void testZipThreeArrays_Test() {
        float[] result = FloatStream.zip(new float[] { 1.0f }, new float[] { 10.0f }, new float[] { 100.0f }, (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new float[] { 111.0f }, result, DELTA);
    }

    @Test
    @DisplayName("zip() three arrays with defaults additional")
    public void testZipThreeArraysDefaultsAdditional() {
        float[] a1 = { 1.0f, 2.0f };
        float[] a2 = { 10.0f };
        float[] a3 = { 100.0f, 200.0f, 300.0f };
        FloatStream stream = FloatStream.zip(a1, a2, a3, 0.0f, 0.0f, 0.0f, (a, b, c) -> a + b + c);
        assertEquals(3, stream.count());
    }

    @Test
    @DisplayName("zip() with padding for three arrays")
    public void testZipThreeArraysWithPadding() {
        float[] a = { 1.0f };
        float[] b = { 4.0f, 5.0f };
        float[] c = { 7.0f, 8.0f, 9.0f };
        FloatStream stream = FloatStream.zip(a, b, c, 100.0f, 200.0f, 300.0f, (x, y, z) -> x + y + z);
        assertArrayEquals(new float[] { 12.0f, 113.0f, 309.0f }, stream.toArray());
    }

    @Test
    public void testZipThreeArraysWithPadding_Test() {
        float[] result = FloatStream
                .zip(new float[] { 1.0f }, new float[] { 10.0f, 20.0f }, new float[] { 100.0f, 200.0f, 300.0f }, 0.0f, 0.0f, 0.0f, (x, y, z) -> x + y + z)
                .toArray();
        assertArrayEquals(new float[] { 111.0f, 220.0f, 300.0f }, result, DELTA);
    }

    @Test
    @DisplayName("zip() three iterators with ternary function")
    public void testZipThreeIterators() {
        FloatIterator a = FloatIterator.of(1.0f, 2.0f);
        FloatIterator b = FloatIterator.of(3.0f, 4.0f);
        FloatIterator c = FloatIterator.of(5.0f, 6.0f);
        FloatStream stream = FloatStream.zip(a, b, c, (x, y, z) -> x + y + z);
        assertArrayEquals(new float[] { 9.0f, 12.0f }, stream.toArray());
    }

    @Test
    public void testZipThreeIterators_Test() {
        float[] result = FloatStream
                .zip(FloatIterator.of(new float[] { 1.0f }), FloatIterator.of(new float[] { 10.0f }), FloatIterator.of(new float[] { 100.0f }),
                        (x, y, z) -> x + y + z)
                .toArray();
        assertArrayEquals(new float[] { 111.0f }, result, DELTA);
    }

    @Test
    @DisplayName("zip() three iterators with defaults additional")
    public void testZipThreeIteratorsDefaultsAdditional() {
        FloatIterator iter1 = FloatIterator.of(1.0f, 2.0f);
        FloatIterator iter2 = FloatIterator.of(10.0f);
        FloatIterator iter3 = FloatIterator.of(100.0f, 200.0f, 300.0f);
        FloatStream stream = FloatStream.zip(iter1, iter2, iter3, 0.0f, 0.0f, 0.0f, (a, b, c) -> a + b + c);
        assertEquals(3, stream.count());
    }

    @Test
    @DisplayName("zip() with padding for three iterators")
    public void testZipThreeIteratorsWithPadding() {
        FloatIterator a = FloatIterator.of(1.0f);
        FloatIterator b = FloatIterator.of(4.0f, 5.0f);
        FloatIterator c = FloatIterator.of(7.0f, 8.0f, 9.0f);
        FloatStream stream = FloatStream.zip(a, b, c, 0.0f, 0.0f, 0.0f, (x, y, z) -> x + y + z);
        assertArrayEquals(new float[] { 12.0f, 13.0f, 9.0f }, stream.toArray());
    }

    @Test
    public void testZipThreeIteratorsWithPadding_Test() {
        float[] result = FloatStream
                .zip(FloatIterator.of(new float[] { 1.0f }), FloatIterator.of(new float[] { 10.0f, 20.0f }),
                        FloatIterator.of(new float[] { 100.0f, 200.0f, 300.0f }), 0.0f, 0.0f, 0.0f, (x, y, z) -> x + y + z)
                .toArray();
        assertArrayEquals(new float[] { 111.0f, 220.0f, 300.0f }, result, DELTA);
    }

    @Test
    @DisplayName("zip() three streams with ternary function")
    public void testZipThreeStreams() {
        FloatStream a = FloatStream.of(1.0f, 2.0f);
        FloatStream b = FloatStream.of(3.0f, 4.0f);
        FloatStream c = FloatStream.of(5.0f, 6.0f);
        FloatStream stream = FloatStream.zip(a, b, c, (x, y, z) -> x + y + z);
        assertArrayEquals(new float[] { 9.0f, 12.0f }, stream.toArray());
    }

    @Test
    public void testZipThreeStreams_Test() {
        float[] result = FloatStream.zip(FloatStream.of(1.0f), FloatStream.of(10.0f), FloatStream.of(100.0f), (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new float[] { 111.0f }, result, DELTA);
    }

    @Test
    @DisplayName("zip() three streams with defaults additional")
    public void testZipThreeStreamsDefaultsAdditional() {
        FloatStream s1 = FloatStream.of(1.0f, 2.0f);
        FloatStream s2 = FloatStream.of(10.0f);
        FloatStream s3 = FloatStream.of(100.0f, 200.0f, 300.0f);
        FloatStream stream = FloatStream.zip(s1, s2, s3, 0.0f, 0.0f, 0.0f, (a, b, c) -> a + b + c);
        assertEquals(3, stream.count());
    }

    @Test
    @DisplayName("zip() with padding for three streams")
    public void testZipThreeStreamsWithPadding() {
        FloatStream a = FloatStream.of(1.0f);
        FloatStream b = FloatStream.of(4.0f, 5.0f);
        FloatStream c = FloatStream.of(7.0f, 8.0f, 9.0f);
        FloatStream stream = FloatStream.zip(a, b, c, 0.0f, 0.0f, 0.0f, (x, y, z) -> x + y + z);
        assertArrayEquals(new float[] { 12.0f, 13.0f, 9.0f }, stream.toArray());
    }

    @Test
    public void testZipThreeStreamsWithPadding_Test() {
        float[] result = FloatStream
                .zip(FloatStream.of(1.0f), FloatStream.of(10.0f, 20.0f), FloatStream.of(100.0f, 200.0f, 300.0f), 0.0f, 0.0f, 0.0f, (x, y, z) -> x + y + z)
                .toArray();
        assertArrayEquals(new float[] { 111.0f, 220.0f, 300.0f }, result, DELTA);
    }

    @Test
    @DisplayName("zip() two arrays with binary function")
    public void testZipTwoArrays() {
        float[] a = { 1.0f, 2.0f, 3.0f };
        float[] b = { 4.0f, 5.0f, 6.0f };
        FloatStream stream = FloatStream.zip(a, b, Float::sum);
        assertArrayEquals(new float[] { 5.0f, 7.0f, 9.0f }, stream.toArray());
    }

    @Test
    public void testZipTwoArrays_Test() {
        float[] result = FloatStream.zip(new float[] { 1.0f, 2.0f }, new float[] { 10.0f, 20.0f }, Float::sum).toArray();
        assertArrayEquals(new float[] { 11.0f, 22.0f }, result, DELTA);
    }

    @Test
    @DisplayName("zip() with padding for two arrays")
    public void testZipTwoArraysWithPadding() {
        float[] a = { 1.0f, 2.0f };
        float[] b = { 4.0f, 5.0f, 6.0f };
        FloatStream stream = FloatStream.zip(a, b, 0.0f, 0.0f, Float::sum);
        assertArrayEquals(new float[] { 5.0f, 7.0f, 6.0f }, stream.toArray());
    }

    @Test
    public void testZipTwoArraysWithPadding_Test() {
        float[] result = FloatStream.zip(new float[] { 1.0f, 2.0f, 3.0f }, new float[] { 10.0f, 20.0f }, 0.0f, 0.0f, Float::sum).toArray();
        assertArrayEquals(new float[] { 11.0f, 22.0f, 3.0f }, result, DELTA);
    }

    @Test
    @DisplayName("zip() two iterators with binary function")
    public void testZipTwoIterators() {
        FloatIterator a = FloatIterator.of(1.0f, 2.0f);
        FloatIterator b = FloatIterator.of(4.0f, 5.0f);
        FloatStream stream = FloatStream.zip(a, b, Float::sum);
        assertArrayEquals(new float[] { 5.0f, 7.0f }, stream.toArray());
    }

    @Test
    public void testZipTwoIterators_Test() {
        float[] result = FloatStream.zip(FloatIterator.of(new float[] { 1.0f, 2.0f }), FloatIterator.of(new float[] { 10.0f, 20.0f }), Float::sum).toArray();
        assertArrayEquals(new float[] { 11.0f, 22.0f }, result, DELTA);
    }

    @Test
    @DisplayName("zip() with padding for two iterators")
    public void testZipTwoIteratorsWithPadding() {
        FloatIterator a = FloatIterator.of(1.0f, 2.0f);
        FloatIterator b = FloatIterator.of(4.0f, 5.0f, 6.0f);
        FloatStream stream = FloatStream.zip(a, b, 0.0f, 0.0f, Float::sum);
        assertArrayEquals(new float[] { 5.0f, 7.0f, 6.0f }, stream.toArray());
    }

    @Test
    public void testZipTwoIteratorsWithPadding_Test() {
        float[] result = FloatStream
                .zip(FloatIterator.of(new float[] { 1.0f, 2.0f, 3.0f }), FloatIterator.of(new float[] { 10.0f, 20.0f }), 0.0f, 0.0f, Float::sum)
                .toArray();
        assertArrayEquals(new float[] { 11.0f, 22.0f, 3.0f }, result, DELTA);
    }

    @Test
    @DisplayName("zip() two streams with binary function")
    public void testZipTwoStreams() {
        FloatStream a = FloatStream.of(1.0f, 2.0f);
        FloatStream b = FloatStream.of(4.0f, 5.0f);
        FloatStream stream = FloatStream.zip(a, b, Float::sum);
        assertArrayEquals(new float[] { 5.0f, 7.0f }, stream.toArray());
    }

    @Test
    public void testZipTwoStreams_Test() {
        float[] result = FloatStream.zip(FloatStream.of(1.0f, 2.0f), FloatStream.of(10.0f, 20.0f), Float::sum).toArray();
        assertArrayEquals(new float[] { 11.0f, 22.0f }, result, DELTA);
    }

    @Test
    @DisplayName("zip() with padding for two streams")
    public void testZipTwoStreamsWithPadding() {
        FloatStream a = FloatStream.of(1.0f, 2.0f);
        FloatStream b = FloatStream.of(4.0f, 5.0f, 6.0f);
        FloatStream stream = FloatStream.zip(a, b, 0.0f, 0.0f, Float::sum);
        assertArrayEquals(new float[] { 5.0f, 7.0f, 6.0f }, stream.toArray());
    }

    @Test
    public void testZipTwoStreamsWithPadding_Test() {
        float[] result = FloatStream.zip(FloatStream.of(1.0f, 2.0f, 3.0f), FloatStream.of(10.0f, 20.0f), 0.0f, 0.0f, Float::sum).toArray();
        assertArrayEquals(new float[] { 11.0f, 22.0f, 3.0f }, result, DELTA);
    }

    @Test
    public void testZipWithDefaults() {
        float[] a = { 1.1f, 2.2f };
        float[] b = { 10.1f, 20.2f, 30.3f };
        float[] expected = { 11.2f, 22.4f, 30.3f };
        assertArrayEquals(expected, FloatStream.zip(a, b, 0.0f, 0.0f, (x, y) -> x + y).toArray(), DELTA);
    }

    // zipWith

    @Test
    @DisplayName("zipWith() two streams with binary operator")
    public void testZipWith() {
        FloatStream stream1 = createFloatStream(1.0f, 2.0f, 3.0f);
        FloatStream stream2 = createFloatStream(4.0f, 5.0f, 6.0f);
        assertArrayEquals(new float[] { 5.0f, 7.0f, 9.0f }, stream1.zipWith(stream2, Float::sum).toArray());
    }

    @Test
    public void testZipWith_DifferentLengths() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f).zipWith(FloatStream.of(10.0f, 20.0f), (a, b) -> a + b).toArray();
        assertArrayEquals(new float[] { 11.0f, 22.0f }, result, DELTA);
    }

    @Test
    public void testZipWith_Padding_Test() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f).zipWith(FloatStream.of(10.0f, 20.0f), 0.0f, 0.0f, Float::sum).toArray();
        assertArrayEquals(new float[] { 11.0f, 22.0f, 3.0f }, result, DELTA);
    }

    @Test
    public void testZipWith_Test() {
        float[] result = createFloatStream(1.0f, 2.0f).zipWith(FloatStream.of(10.0f, 20.0f), Float::sum).toArray();
        assertArrayEquals(new float[] { 11.0f, 22.0f }, result, DELTA);
    }

    @Test
    public void testZipWith_ThreeStreams_Test() {
        float[] result = createFloatStream(1.0f, 2.0f).zipWith(FloatStream.of(10.0f, 20.0f), FloatStream.of(100.0f, 200.0f), (x, y, z) -> x + y + z).toArray();
        assertArrayEquals(new float[] { 111.0f, 222.0f }, result, DELTA);
    }

    @Test
    public void testZipWith_ThreeStreamsPadding_Test() {
        float[] result = createFloatStream(1.0f)
                .zipWith(FloatStream.of(10.0f, 20.0f), FloatStream.of(100.0f, 200.0f, 300.0f), 0.0f, 0.0f, 0.0f, (x, y, z) -> x + y + z)
                .toArray();
        assertArrayEquals(new float[] { 111.0f, 220.0f, 300.0f }, result, DELTA);
    }

    @Test
    @DisplayName("zipWith() with padding for two streams")
    public void testZipWithPadding() {
        FloatStream stream1 = createFloatStream(1.0f, 2.0f, 5.0f);
        FloatStream stream2 = createFloatStream(3.0f, 4.0f);
        assertArrayEquals(new float[] { 4.0f, 6.0f, 5.0f }, stream1.zipWith(stream2, 0.0f, 0.0f, Float::sum).toArray());
    }

    @Test
    public void testZipWithPadding_DifferentLengths() {
        float[] result = createFloatStream(1.0f, 2.0f, 3.0f).zipWith(FloatStream.of(10.0f, 20.0f), 0.0f, 0.0f, (a, b) -> a + b).toArray();
        assertArrayEquals(new float[] { 11.0f, 22.0f, 3.0f }, result, DELTA);
    }

    @Test
    @DisplayName("zipWith() with padding for three streams")
    public void testZipWithPaddingThreeStreams() {
        FloatStream stream1 = createFloatStream(1.0f);
        FloatStream stream2 = createFloatStream(3.0f, 4.0f);
        FloatStream stream3 = createFloatStream(5.0f, 6.0f, 7.0f);
        assertArrayEquals(new float[] { 9.0f, 10.0f, 7.0f }, stream1.zipWith(stream2, stream3, 0.0f, 0.0f, 0.0f, (a, b, c) -> a + b + c).toArray());
    }

    @Test
    @DisplayName("zipWith() three streams with ternary operator")
    public void testZipWithThreeStreams() {
        FloatStream stream1 = createFloatStream(1.0f, 2.0f);
        FloatStream stream2 = createFloatStream(3.0f, 4.0f);
        FloatStream stream3 = createFloatStream(5.0f, 6.0f);
        assertArrayEquals(new float[] { 9.0f, 12.0f }, stream1.zipWith(stream2, stream3, (a, b, c) -> a + b + c).toArray());
    }

}
