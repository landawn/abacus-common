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
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.ShortIterator;
import com.landawn.abacus.util.ShortList;
import com.landawn.abacus.util.function.ShortNFunction;
import com.landawn.abacus.util.u.OptionalShort;

/** ShortStream combination contracts. */
public class ShortStreamCombinationTest extends ShortStreamTestSupport {

    // append

    @Test
    public void testAppend() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2).append((short) 3, (short) 4);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testAppend_Empty() {
        short[] result = ShortStream.empty().append((short) 1, (short) 2).toArray();
        assertArrayEquals(new short[] { 1, 2 }, result);
    }

    @Test
    public void testAppendOptional() {
        ShortStream stream1 = createShortStream((short) 1, (short) 2).append(OptionalShort.of((short) 3));
        assertArrayEquals(new short[] { 1, 2, 3 }, stream1.toArray());

        ShortStream stream2 = createShortStream((short) 1, (short) 2).append(OptionalShort.empty());
        assertArrayEquals(new short[] { 1, 2 }, stream2.toArray());
    }

    @Test
    public void testAppendOptional_Empty() {
        short[] result = createShortStream((short) 1, (short) 2).append(OptionalShort.empty()).toArray();
        assertArrayEquals(new short[] { 1, 2 }, result);
    }

    @Test
    public void testAppendOptionalMethod() {
        short[] result = createShortStream((short) 1, (short) 2).append(OptionalShort.of((short) 3)).toArray();
        assertArrayEquals(new short[] { 1, 2, 3 }, result);
    }

    @Test
    public void testAppendStream() {
        ShortStream stream = createShortStream((short) 1, (short) 2).append(ShortStream.of((short) 3, (short) 4));
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testAppendStreamMethod() {
        short[] result = createShortStream((short) 1, (short) 2).append(ShortStream.of((short) 3, (short) 4)).toArray();
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, result);
    }

    // appendIfEmpty

    @Test
    public void testAppendIfEmpty() {
        ShortStream stream1 = ShortStream.of((short) 1, (short) 2).appendIfEmpty((short) 3, (short) 4);
        assertArrayEquals(new short[] { 1, 2 }, stream1.toArray());

        ShortStream stream2 = ShortStream.empty().appendIfEmpty((short) 3, (short) 4);
        assertArrayEquals(new short[] { 3, 4 }, stream2.toArray());
    }

    @Test
    public void testAppendIfEmptySupplier() {
        ShortStream stream = ShortStream.empty().appendIfEmpty(() -> createShortStream((short) 10, (short) 20));
        assertArrayEquals(new short[] { 10, 20 }, stream.toArray());
    }

    @Test
    public void testAppendIfEmptySupplier_NonEmpty() {
        short[] result = createShortStream((short) 1, (short) 2).appendIfEmpty(() -> ShortStream.of((short) 10, (short) 20)).toArray();
        assertArrayEquals(new short[] { 1, 2 }, result);
    }

    @Test
    public void testAppendIfEmptySupplierMethod() {
        short[] result = ShortStream.empty().appendIfEmpty(() -> ShortStream.of((short) 10, (short) 20)).toArray();
        assertArrayEquals(new short[] { 10, 20 }, result);
    }

    @Test
    public void testAppendIfEmptyWithSupplierOnEmpty() {
        short[] result = ShortStream.empty().appendIfEmpty(() -> ShortStream.of((short) 77)).toArray();
        assertArrayEquals(new short[] { 77 }, result);
    }

    @Test
    public void testAppendIfEmptyWithSupplierOnNonEmpty() {
        short[] result = createShortStream((short) 1).appendIfEmpty(() -> ShortStream.of((short) 77)).toArray();
        assertArrayEquals(new short[] { 1 }, result);
    }

    // concat

    @Test
    public void testConcat() {
        ShortStream s1 = createShortStream((short) 1, (short) 2);
        ShortStream s2 = createShortStream((short) 3, (short) 4);
        ShortStream s3 = createShortStream((short) 5, (short) 6);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6 }, ShortStream.concat(s1, s2, s3).toArray());
    }

    @Test
    public void testConcatArrayCollection() {
        List<short[]> arrays = Arrays.asList(new short[] { 1, 2 }, new short[] { 3, 4 }, new short[] { 5 });

        ShortStream stream = ShortStream.concat(arrays);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testConcatArrays() {
        short[] a1 = { 1, 2 };
        short[] a2 = { 3, 4 };
        ShortStream stream = ShortStream.concat(a1, a2);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, stream.toArray());

        short[] a3 = { 5, 6 };
        ShortStream stream2 = ShortStream.concat(a1, a2, a3);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6 }, stream2.toArray());
    }

    @Test
    public void testConcatCollectionSnapshotsSources() {
        List<ShortStream> sources = new ArrayList<>(Arrays.asList(ShortStream.of((short) 1, (short) 2), ShortStream.of((short) 3, (short) 4)));
        ShortStream concatenated = ShortStream.concat(sources);

        sources.clear();

        assertArrayEquals(new short[] { 1, 2, 3, 4 }, concatenated.toArray());
    }

    @Test
    public void testConcatEmptyArrays() {
        ShortStream stream = ShortStream.concat(new short[0], new short[0]);
        assertEquals(0, stream.count());
    }

    @Test
    public void testConcatIterators() {
        ShortIterator it1 = ShortIterator.of((short) 1, (short) 2);
        ShortIterator it2 = ShortIterator.of((short) 3, (short) 4);

        ShortStream stream = ShortStream.concat(it1, it2);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testConcatList() {
        List<short[]> list = Arrays.asList(new short[] { 1, 2 }, new short[] { 3, 4 }, new short[] { 5 });

        ShortStream stream = ShortStream.concat(list);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, stream.toArray());
    }



    // G112-01: bulk count()/toShortList()/toArray() of concat(List<short[]>) / concat(short[]...) - null/empty arrays, skip into/at/after a
    // segment, short (element-wise) and long (bulk copy) segments, fresh results, lazy read of the list, same list traversal calls
    @Test
    public void testConcatListOfArrays_bulkOpsMatchElementwise_G112() {
        final short[] a = new short[20];
        final short[] b = new short[40];
        final short[] c = { (short) 1, (short) 2, (short) 3 };
        for (int i = 0; i < a.length; i++) {
            a[i] = (short) (10 + i);
        }
        for (int i = 0; i < b.length; i++) {
            b[i] = (short) (100 + i);
        }
        final List<short[]> list = Arrays.asList(null, a, null, new short[0], c, b, c, new short[0]);
        final short[] all = new short[66];
        System.arraycopy(a, 0, all, 0, 20);
        System.arraycopy(c, 0, all, 20, 3);
        System.arraycopy(b, 0, all, 23, 40);
        System.arraycopy(c, 0, all, 63, 3);

        for (final int n : new int[] { 0, 1, 5, 19, 20, 21, 22, 23, 24, 40, 62, 63, 64, 65, 66, 70 }) {
            final short[] expected = Arrays.copyOfRange(all, Math.min(n, all.length), all.length);
            assertArrayEquals(expected, ShortStream.concat(list).skip(n).toArray(), "skip " + n);
            assertEquals(expected.length, ShortStream.concat(list).skip(n).count(), "skip " + n);
            final ShortList xl = ShortStream.concat(list).skip(n).toShortList();
            assertArrayEquals(expected, xl.toArray(), "skip " + n);
            xl.add((short) 7);
            assertEquals(expected.length + 1, xl.size());
        }

        assertArrayEquals(all, ShortStream.concat(a, c, b, c).toArray());
        assertEquals(66, ShortStream.concat(a, null, c, b, c).count());

        final List<short[]> empties = Arrays.asList(null, new short[0]);
        assertEquals(0, ShortStream.concat(empties).toArray().length);
        assertEquals(0, ShortStream.concat(empties).count());
        final ShortList emptyList = ShortStream.concat(empties).toShortList();
        emptyList.add((short) 9);
        assertEquals(1, emptyList.size());

        final short[] result = ShortStream.concat(Arrays.asList(b)).toArray();
        assertArrayEquals(b, result);
        assertFalse(result == b);
        result[0] = (short) 0;
        assertEquals((short) 100, b[0]);

        // the list is read when the terminal operation runs; close handlers still run
        final List<short[]> live = new ArrayList<>(Arrays.asList(new short[] { (short) 1 }, new short[] { (short) 2 }));
        final ShortStream stream = ShortStream.concat(live);
        live.set(1, b);
        final AtomicInteger closed = new AtomicInteger();
        assertEquals(41, stream.onClose(closed::incrementAndGet).count());
        assertEquals(1, closed.get());

        // partially consumed iterator, then drained in bulk
        final ShortIterator iter = ShortStream.concat(list).iterator();
        assertEquals((short) 10, iter.nextShort());
        assertArrayEquals(Arrays.copyOfRange(all, 1, all.length), iter.toArray());
        assertFalse(iter.hasNext());

        // the list iterator is advanced exactly as element-by-element iteration advances it
        final int[] calls = new int[2];
        final List<short[]> counting = new ArrayList<>(list) {
            private static final long serialVersionUID = 1L;

            @Override
            public java.util.Iterator<short[]> iterator() {
                final java.util.Iterator<short[]> it = super.iterator();

                return new java.util.Iterator<>() {
                    @Override
                    public boolean hasNext() {
                        calls[0]++;
                        return it.hasNext();
                    }

                    @Override
                    public short[] next() {
                        calls[1]++;
                        return it.next();
                    }
                };
            }
        };

        assertArrayEquals(all, ShortStream.concat(counting).toArray());
        assertEquals(9, calls[0]);
        assertEquals(8, calls[1]);
        calls[0] = calls[1] = 0;
        assertEquals(66, ShortStream.concat(counting).count());
        assertEquals(9, calls[0]);
        assertEquals(8, calls[1]);
        calls[0] = calls[1] = 0;
        assertEquals(66, ShortStream.concat(counting).toShortList().size());
        assertEquals(9, calls[0]);
        assertEquals(8, calls[1]);
    }

    @Test
    public void testConcatStreamCollection() {
        Collection<ShortStream> streams = Arrays.asList(ShortStream.of((short) 1, (short) 2), ShortStream.of((short) 3, (short) 4), ShortStream.of((short) 5));

        ShortStream stream = ShortStream.concat(streams);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, stream.toArray());
    }

    @Test
    public void testConcatStreams() {
        ShortStream s1 = ShortStream.of((short) 1, (short) 2);
        ShortStream s2 = ShortStream.of((short) 3, (short) 4);
        ShortStream stream = ShortStream.concat(s1, s2);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testConcatIteratorCollection() {
        Collection<ShortIterator> iterators = Arrays.asList(ShortIterator.of((short) 1, (short) 2), ShortIterator.of((short) 3, (short) 4));

        ShortStream stream = ShortStream.concatIterators(iterators);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testConcatIterators2() {
        List<ShortIterator> iterators = Arrays.asList(ShortList.of((short) 1, (short) 2).iterator(), ShortList.of((short) 3, (short) 4).iterator());

        ShortStream stream = ShortStream.concatIterators(iterators);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, stream.toArray());
    }

    // join

    @Test
    public void testJoin() {
        assertEquals("1, 2, 3, 4, 5", multiElementStream.join(", "));
        assertEquals("[1-2-3-4-5]", createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).join("-", "[", "]"));
    }

    @Test
    public void testJoin_Empty() {
        String result = ShortStream.empty().join(", ");
        assertEquals("", result);
    }

    @Test
    public void testJoinWithDelimiterOnly() {
        String result = createShortStream((short) 1, (short) 2, (short) 3).join(", ");
        assertNotNull(result);
        assertTrue(result.contains("1"));
        assertTrue(result.contains("2"));
        assertTrue(result.contains("3"));
    }

    @Test
    public void testJoinWithPrefixSuffixMethod() {
        String result = createShortStream((short) 1, (short) 2, (short) 3).join(", ", "[", "]");
        assertEquals("[1, 2, 3]", result);
    }

    @Test
    @DisplayName("Test joinTo() method")
    public void testJoinTo() {
        Joiner joiner = Joiner.with(",", "[", "]");
        stream.joinTo(joiner);
        assertEquals("[1,2,3,4,5]", joiner.toString());
    }

    @Test
    public void testJoinToMethod() {
        Joiner joiner = Joiner.with(", ", "[", "]");
        createShortStream((short) 1, (short) 2, (short) 3).joinTo(joiner);
        String result = joiner.toString();
        assertTrue(result.startsWith("["));
        assertTrue(result.endsWith("]"));
        assertTrue(result.contains("1"));
    }

    // merge

    @Test
    public void testMerge() {
        short[] a = { 1, 3, 5 };
        short[] b = { 2, 4, 6 };

        ShortStream stream = ShortStream.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testMergeCollectionTreatsNullStreamAsEmpty() {
        assertEquals(0, ShortStream.merge(Arrays.asList((ShortStream) null), (a, b) -> MergeResult.TAKE_FIRST).count());
    }

    @Test
    public void testMergeIterators() {
        ShortIterator it1 = ShortIterator.of((short) 1, (short) 3, (short) 5);
        ShortIterator it2 = ShortIterator.of((short) 2, (short) 4, (short) 6);

        ShortStream stream = ShortStream.merge(it1, it2, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testMergeMultipleStreams() {
        List<ShortStream> streams = Arrays.asList(ShortStream.of((short) 1, (short) 4), createShortStream((short) 2, (short) 5),
                createShortStream((short) 3, (short) 6));

        ShortStream result = ShortStream.merge(streams, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6 }, result.toArray());
    }

    @Test
    public void testMergeStreams() {
        ShortStream s1 = ShortStream.of((short) 1, (short) 3, (short) 5);
        ShortStream s2 = ShortStream.of((short) 2, (short) 4, (short) 6);

        ShortStream stream = ShortStream.merge(s1, s2, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testMergeThreeArrays() {
        short[] a = { 1, 4, 7 };
        short[] b = { 2, 5, 8 };
        short[] c = { 3, 6, 9 };

        ShortStream stream = ShortStream.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, stream.toArray());
    }

    @Test
    public void testMergeThreeIterators() {
        ShortIterator it1 = ShortIterator.of((short) 1, (short) 4);
        ShortIterator it2 = ShortIterator.of((short) 2, (short) 5);
        ShortIterator it3 = ShortIterator.of((short) 3, (short) 6);

        ShortStream result = ShortStream.merge(it1, it2, it3, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6 }, result.toArray());
    }

    @Test
    public void testMergeThreeStreams() {
        ShortStream s1 = ShortStream.of((short) 1, (short) 4, (short) 7);
        ShortStream s2 = ShortStream.of((short) 2, (short) 5, (short) 8);
        ShortStream s3 = ShortStream.of((short) 3, (short) 6, (short) 9);

        ShortStream result = ShortStream.merge(s1, s2, s3, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, result.toArray());
    }

    @Test
    public void testMergeWithCollectionOfStreams() {
        Collection<ShortStream> streams = Arrays.asList(ShortStream.of((short) 1, (short) 5), ShortStream.of((short) 2, (short) 6),
                ShortStream.of((short) 3, (short) 7));
        ShortStream result = ShortStream.merge(streams, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new short[] { 1, 2, 3, 5, 6, 7 }, result.toArray());
    }

    // mergeWith

    @Test
    public void testMergeWith() {
        ShortStream s1 = ShortStream.of((short) 1, (short) 3, (short) 5);
        ShortStream s2 = ShortStream.of((short) 2, (short) 4, (short) 6);

        ShortStream stream = s1.mergeWith(s2, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6 }, stream.toArray());
    }

    @Test
    public void testMergeWith_Empty() {
        short[] result = createShortStream((short) 1, (short) 3, (short) 5)
                .mergeWith(ShortStream.empty(), (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray();
        assertArrayEquals(new short[] { 1, 3, 5 }, result);
    }

    @Test
    public void testMergeWithDedicated() {
        ShortStream a = createShortStream((short) 1, (short) 3, (short) 5);
        ShortStream b = ShortStream.of((short) 2, (short) 4, (short) 6);
        short[] result = a.mergeWith(b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6 }, result);
    }

    // prepend

    @Test
    public void testPrepend() {
        ShortStream stream = ShortStream.of((short) 3, (short) 4).prepend((short) 1, (short) 2);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, stream.toArray());
    }

    @Test
    public void testPrepend_Empty() {
        short[] result = ShortStream.empty().prepend((short) 1, (short) 2).toArray();
        assertArrayEquals(new short[] { 1, 2 }, result);
    }

    @Test
    public void testPrependOptional() {
        ShortStream stream1 = createShortStream((short) 2, (short) 3).prepend(OptionalShort.of((short) 1));
        assertArrayEquals(new short[] { 1, 2, 3 }, stream1.toArray());

        ShortStream stream2 = createShortStream((short) 2, (short) 3).prepend(OptionalShort.empty());
        assertArrayEquals(new short[] { 2, 3 }, stream2.toArray());
    }

    @Test
    public void testPrependOptional_Empty() {
        short[] result = createShortStream((short) 2, (short) 3).prepend(OptionalShort.empty()).toArray();
        assertArrayEquals(new short[] { 2, 3 }, result);
    }

    @Test
    public void testPrependStream() {
        ShortStream stream = createShortStream((short) 3, (short) 4).prepend(ShortStream.of((short) 1, (short) 2));
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, stream.toArray());
    }

    // zip

    @Test
    public void testZip() {
        short[] a = { 1, 2, 3 };
        short[] b = { 10, 20, 30 };

        ShortStream stream = ShortStream.zip(a, b, (x, y) -> (short) (x + y));
        assertArrayEquals(new short[] { 11, 22, 33 }, stream.toArray());
    }

    @Test
    public void testZipIterators() {
        ShortIterator it1 = ShortIterator.of((short) 1, (short) 2);
        ShortIterator it2 = ShortIterator.of((short) 10, (short) 20);

        ShortStream stream = ShortStream.zip(it1, it2, (x, y) -> (short) (x + y));
        assertArrayEquals(new short[] { 11, 22 }, stream.toArray());
    }

    @Test
    public void testZipIteratorsWithDefaults() {
        ShortIterator it1 = ShortIterator.of((short) 1, (short) 2, (short) 3);
        ShortIterator it2 = ShortIterator.of((short) 10);

        ShortStream stream = ShortStream.zip(it1, it2, (short) 0, (short) 0, (x, y) -> (short) (x + y));
        assertArrayEquals(new short[] { 11, 2, 3 }, stream.toArray());
    }

    @Test
    public void testZipMultipleStreams() {
        List<ShortStream> streams = Arrays.asList(ShortStream.of((short) 1, (short) 2), createShortStream((short) 10, (short) 20),
                createShortStream((short) 100, (short) 200));

        ShortNFunction<Short> zipFunction = args -> {
            short sum = 0;
            for (short s : args) {
                sum += s;
            }
            return sum;
        };

        ShortStream result = ShortStream.zip(streams, zipFunction);
        assertArrayEquals(new short[] { 111, 222 }, result.toArray());
    }

    @Test
    public void testZipMultipleStreamsWithDefaults() {
        List<ShortStream> streams = Arrays.asList(ShortStream.of((short) 1, (short) 2, (short) 3), createShortStream((short) 10, (short) 20),
                createShortStream((short) 100));

        short[] defaults = { 0, 0, 0 };
        ShortNFunction<Short> zipFunction = args -> {
            short sum = 0;
            for (short s : args) {
                sum += s;
            }
            return sum;
        };

        ShortStream result = ShortStream.zip(streams, defaults, zipFunction);
        assertArrayEquals(new short[] { 111, 22, 3 }, result.toArray());
    }

    @Test
    public void testZipStreams() {
        ShortStream s1 = ShortStream.of((short) 1, (short) 2);
        ShortStream s2 = ShortStream.of((short) 10, (short) 20);

        ShortStream stream = ShortStream.zip(s1, s2, (x, y) -> (short) (x + y));
        assertArrayEquals(new short[] { 11, 22 }, stream.toArray());
    }

    @Test
    public void testZipStreamsWithDefaults() {
        ShortStream s1 = ShortStream.of((short) 1, (short) 2);
        ShortStream s2 = ShortStream.of((short) 10, (short) 20, (short) 30);

        ShortStream stream = ShortStream.zip(s1, s2, (short) 0, (short) 0, (x, y) -> (short) (x + y));
        assertArrayEquals(new short[] { 11, 22, 30 }, stream.toArray());
    }

    @Test
    public void testZipThreeArrays() {
        short[] a = { 1, 2, 3 };
        short[] b = { 10, 20, 30 };
        short[] c = { 100, 200, 300 };

        ShortStream stream = ShortStream.zip(a, b, c, (x, y, z) -> (short) (x + y + z));
        assertArrayEquals(new short[] { 111, 222, 333 }, stream.toArray());
    }

    @Test
    public void testZipThreeArraysWithDefaults() {
        short[] a = { 1, 2, 3, 4 };
        short[] b = { 10, 20 };
        short[] c = { 100 };

        ShortStream stream = ShortStream.zip(a, b, c, (short) 0, (short) 0, (short) 0, (x, y, z) -> (short) (x + y + z));
        assertArrayEquals(new short[] { 111, 22, 3, 4 }, stream.toArray());
    }

    @Test
    public void testZipThreeIterators() {
        ShortIterator it1 = ShortIterator.of((short) 1, (short) 2);
        ShortIterator it2 = ShortIterator.of((short) 10, (short) 20);
        ShortIterator it3 = ShortIterator.of((short) 100, (short) 200);

        ShortStream stream = ShortStream.zip(it1, it2, it3, (x, y, z) -> (short) (x + y + z));
        assertArrayEquals(new short[] { 111, 222 }, stream.toArray());
    }

    @Test
    public void testZipThreeIteratorsWithDefaults() {
        ShortIterator it1 = ShortIterator.of((short) 1, (short) 2, (short) 3);
        ShortIterator it2 = ShortIterator.of((short) 10);
        ShortIterator it3 = ShortIterator.of((short) 100, (short) 200);

        ShortStream stream = ShortStream.zip(it1, it2, it3, (short) 0, (short) 0, (short) 0, (x, y, z) -> (short) (x + y + z));
        assertArrayEquals(new short[] { 111, 202, 3 }, stream.toArray());
    }

    @Test
    public void testZipThreeStreams() {
        ShortStream s1 = ShortStream.of((short) 1, (short) 2);
        ShortStream s2 = ShortStream.of((short) 10, (short) 20);
        ShortStream s3 = ShortStream.of((short) 100, (short) 200);

        ShortStream stream = ShortStream.zip(s1, s2, s3, (x, y, z) -> (short) (x + y + z));
        assertArrayEquals(new short[] { 111, 222 }, stream.toArray());
    }

    @Test
    public void testZipThreeWithDefaults() {
        short[] a = { 1, 2, 3 };
        short[] b = { 10, 20 };
        short[] c = { 100 };

        ShortStream stream = ShortStream.zip(a, b, c, (short) 0, (short) 0, (short) 0, (x, y, z) -> (short) (x + y + z));
        assertArrayEquals(new short[] { 111, 22, 3 }, stream.toArray());
    }

    @Test
    public void testZipWithCollectionAndValuesForNone() {
        Collection<ShortStream> streams = Arrays.asList(ShortStream.of((short) 1, (short) 2, (short) 3), ShortStream.of((short) 10),
                ShortStream.of((short) 100, (short) 200));
        short[] defaults = { 0, 0, 0 };
        ShortStream result = ShortStream.zip(streams, defaults, shorts -> {
            short sum = 0;
            for (Short s : shorts) {
                sum += s;
            }
            return sum;
        });
        assertArrayEquals(new short[] { 111, 202, 3 }, result.toArray());
    }

    @Test
    public void testZipWithCollectionOfStreams() {
        Collection<ShortStream> streams = Arrays.asList(ShortStream.of((short) 1, (short) 2), ShortStream.of((short) 10, (short) 20),
                ShortStream.of((short) 100, (short) 200));
        ShortStream result = ShortStream.zip(streams, shorts -> {
            short sum = 0;
            for (Short s : shorts) {
                sum += s;
            }
            return sum;
        });
        assertArrayEquals(new short[] { 111, 222 }, result.toArray());
    }

    @Test
    public void testZipWithDefault() {
        short[] a = { 1, 2 };
        short[] b = { 4, 5, 6, 7 };
        short[] result = ShortStream.zip(a, b, (short) 99, (short) 100, (x, y) -> (short) (x + y)).toArray();
        assertArrayEquals(new short[] { 5, 7, 105, 106 }, result);
    }

    @Test
    public void testZipWithDifferentLengths() {
        short[] a = { 1, 2, 3, 4 };
        short[] b = { 10, 20 };

        ShortStream stream = ShortStream.zip(a, b, (x, y) -> (short) (x + y));
        assertArrayEquals(new short[] { 11, 22 }, stream.toArray());
    }

    @Test
    public void testZipThreeStreamsWithDefaults() {
        ShortStream s1 = ShortStream.of((short) 1, (short) 2);
        ShortStream s2 = ShortStream.of((short) 10, (short) 20, (short) 30);
        ShortStream s3 = ShortStream.of((short) 100);

        ShortStream stream = s1.zipWith(s2, s3, (short) 0, (short) 0, (short) 0, (x, y, z) -> (short) (x + y + z));
        assertArrayEquals(new short[] { 111, 22, 30 }, stream.toArray());
    }

    // zipWith

    @Test
    public void testZipWith() {
        ShortStream s1 = ShortStream.of((short) 1, (short) 2, (short) 3);
        ShortStream s2 = ShortStream.of((short) 10, (short) 20, (short) 30);

        ShortStream stream = s1.zipWith(s2, (x, y) -> (short) (x + y));
        assertArrayEquals(new short[] { 11, 22, 33 }, stream.toArray());
    }

    @Test
    public void testZipWith_DifferentLengths() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3).zipWith(ShortStream.of((short) 10, (short) 20), (a, b) -> (short) (a + b))
                .toArray();
        assertArrayEquals(new short[] { 11, 22 }, result);
    }

    @Test
    public void testZipWithDefaults() {
        ShortStream s1 = ShortStream.of((short) 1, (short) 2, (short) 3);
        ShortStream s2 = ShortStream.of((short) 10, (short) 20);

        ShortStream stream = s1.zipWith(s2, (short) 0, (short) 0, (x, y) -> (short) (x + y));
        assertArrayEquals(new short[] { 11, 22, 3 }, stream.toArray());
    }

    @Test
    public void testZipWithPadding_DifferentLengths() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3)
                .zipWith(ShortStream.of((short) 10, (short) 20), (short) 0, (short) 0, (a, b) -> (short) (a + b))
                .toArray();
        assertArrayEquals(new short[] { 11, 22, 3 }, result);
    }

    @Test
    public void testZipWithThree() {
        ShortStream s1 = ShortStream.of((short) 1, (short) 2);
        ShortStream s2 = ShortStream.of((short) 10, (short) 20);
        ShortStream s3 = ShortStream.of((short) 100, (short) 200);

        ShortStream stream = s1.zipWith(s2, s3, (x, y, z) -> (short) (x + y + z));
        assertArrayEquals(new short[] { 111, 222 }, stream.toArray());
    }

    @Test
    public void testZipWithThreeDefaults() {
        ShortStream s1 = ShortStream.of((short) 1, (short) 2);
        ShortStream s2 = ShortStream.of((short) 10);
        ShortStream s3 = ShortStream.of((short) 100, (short) 200, (short) 300);

        ShortStream stream = s1.zipWith(s2, s3, (short) 0, (short) 0, (short) 0, (x, y, z) -> (short) (x + y + z));
        assertArrayEquals(new short[] { 111, 202, 300 }, stream.toArray());
    }

    @Test
    public void testZipWithThreeStreamsDedicated() {
        ShortStream a = createShortStream((short) 1, (short) 2);
        ShortStream b = ShortStream.of((short) 10, (short) 20);
        ShortStream c = ShortStream.of((short) 100);
        short[] result = a.zipWith(b, c, (x, y, z) -> (short) (x + y + z)).toArray();
        assertArrayEquals(new short[] { 111 }, result);
    }

    @Test
    public void testZipWithThreeStreamsWithDefaultsDedicated() {
        ShortStream a = createShortStream((short) 1, (short) 2);
        ShortStream b = ShortStream.of((short) 10);
        ShortStream c = ShortStream.of((short) 100);
        short[] result = a.zipWith(b, c, (short) 0, (short) 0, (short) 0, (x, y, z) -> (short) (x + y + z)).toArray();
        assertArrayEquals(new short[] { 111, 2 }, result);
    }

}
