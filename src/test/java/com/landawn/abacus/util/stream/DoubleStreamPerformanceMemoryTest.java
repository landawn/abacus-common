package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Stream performance and memory regression contracts. */
@Tag("slow-test")
public class DoubleStreamPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G093 end ----
    // ---- perf review 2026-10-08 G095 begin ----
    @Nested
    class G095 {
        private final double[] samples = { 0.0, -0.0, 1.5, -2.25, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.MIN_VALUE, 1e300 };

        private double[] randomArray(final Random rnd, final int maxLength) {
            final double[] a = new double[rnd.nextInt(maxLength + 1)];

            for (int i = 0; i < a.length; i++) {
                a[i] = rnd.nextInt(4) == 0 ? samples[rnd.nextInt(samples.length)] : rnd.nextInt(1000) / 8.0;
            }

            return a;
        }

        private double[] drain(final com.landawn.abacus.util.DoubleIterator iterator) {
            final com.landawn.abacus.util.DoubleList list = new com.landawn.abacus.util.DoubleList();

            while (iterator.hasNext()) {
                list.add(iterator.nextDouble());
            }

            return list.toArray();
        }

        private double[] suffix(final double[] a, final int from) {
            return from >= a.length ? new double[0] : Arrays.copyOfRange(a, from, a.length);
        }

        private double f2(final double x, final double y) {
            return x * 3 + y;
        }

        private double f3(final double x, final double y, final double z) {
            return x * 3 + y - z;
        }

        // variant 0: zip(a, b), 1: zip(a, b, c), 2: zip(a, b, -0.0, NaN), 3: zip(a, b, c, -0.0, NaN, 7.5); every call is recorded
        private DoubleStream zipVariant(final int variant, final double[] a, final double[] b, final double[] c, final List<String> calls) {
            switch (variant) {
                case 0:
                    return DoubleStream.zip(a, b, (x, y) -> {
                        calls.add(x + "," + y);
                        return f2(x, y);
                    });
                case 1:
                    return DoubleStream.zip(a, b, c, (x, y, z) -> {
                        calls.add(x + "," + y + "," + z);
                        return f3(x, y, z);
                    });
                case 2:
                    return DoubleStream.zip(a, b, -0.0, Double.NaN, (x, y) -> {
                        calls.add(x + "," + y);
                        return f2(x, y);
                    });
                default:
                    return DoubleStream.zip(a, b, c, -0.0, Double.NaN, 7.5, (x, y, z) -> {
                        calls.add(x + "," + y + "," + z);
                        return f3(x, y, z);
                    });
            }
        }

        private double[] zipReference(final int variant, final double[] a, final double[] b, final double[] c) {
            final int len = variant == 0 ? Math.min(a.length, b.length)
                    : variant == 1 ? Math.min(a.length, Math.min(b.length, c.length))
                            : variant == 2 ? Math.max(a.length, b.length) : Math.max(a.length, Math.max(b.length, c.length));
            final double[] result = new double[len];

            for (int i = 0; i < len; i++) {
                final double x = i < a.length ? a[i] : -0.0;
                final double y = i < b.length ? b[i] : Double.NaN;
                final double z = i < c.length ? c[i] : 7.5;
                result[i] = variant == 0 || variant == 2 ? f2(x, y) : f3(x, y, z);
            }

            return result;
        }

        // G095-01: zip(double[]...) toArray()/toDoubleList()/skip(k).toArray()/iterator().toArray() fill an exactly sized array;
        // values and the zipFunction call sequence equal the element-by-element traversal
        @Test
        public void testZipArrays_bulkTerminalsMatchElementwiseTraversal() {
            final Random rnd = new Random(95);

            for (int round = 0; round < 400; round++) {
                final double[] a = randomArray(rnd, 12);
                final double[] b = randomArray(rnd, 12);
                final double[] c = randomArray(rnd, 12);

                for (int variant = 0; variant < 4; variant++) {
                    final double[] expected = zipReference(variant, a, b, c);
                    final List<String> expectedCalls = new ArrayList<>();
                    assertArrayEquals(expected, drain(zipVariant(variant, a, b, c, expectedCalls).iterator()));
                    assertEquals(expected.length, expectedCalls.size());

                    final List<String> calls = new ArrayList<>();
                    assertArrayEquals(expected, zipVariant(variant, a, b, c, calls).toArray());
                    assertEquals(expectedCalls, calls);

                    calls.clear();
                    final com.landawn.abacus.util.DoubleList list = zipVariant(variant, a, b, c, calls).toDoubleList();
                    assertArrayEquals(expected, list.toArray());
                    assertEquals(expectedCalls, calls);
                    list.add(42.0);
                    assertEquals(expected.length + 1, list.size());

                    final int k = rnd.nextInt(expected.length + 2);
                    calls.clear();
                    assertArrayEquals(suffix(expected, k), zipVariant(variant, a, b, c, calls).skip(k).toArray());
                    assertEquals(expectedCalls, calls);

                    calls.clear();
                    final com.landawn.abacus.util.DoubleIterator iter = zipVariant(variant, a, b, c, calls).iterator();
                    final int consumed = Math.min(k, expected.length);

                    for (int i = 0; i < consumed; i++) {
                        iter.nextDouble();
                    }

                    assertArrayEquals(suffix(expected, consumed), rnd.nextBoolean() ? iter.toArray() : iter.toList().toArray());
                    assertEquals(expectedCalls, calls);
                    assertFalse(iter.hasNext());
                    assertSame(com.landawn.abacus.util.N.EMPTY_DOUBLE_ARRAY, iter.toArray());
                    assertEquals(0, iter.toList().size());

                    calls.clear();
                    final double[] sorted = expected.clone();
                    Arrays.sort(sorted);
                    assertArrayEquals(sorted, zipVariant(variant, a, b, c, calls).sorted().toArray());
                    assertEquals(expectedCalls, calls);
                }
            }
        }

        // G095-01/03: a failing zipFunction or nextSelector inside toArray()/toList() leaves the iterator exactly where element-by-element
        // traversal leaves it (zip(a, b)/zip(a, b, c) advance before the call, the padded zips and merge do not), and the stream rethrows it
        @Test
        public void testZipAndMergeArrays_failingCallbackKeepsIteratorState() {
            final double[] a = { 1, 2, 3, 4, 5 };
            final double[] b = { 10, 20, 30, 40, 50, 60 };
            final double[] c = { 100, 200, 300, 400, 500, 600, 700 };

            for (int viaList = 0; viaList < 2; viaList++) {
                final boolean useList = viaList == 1;
                final int[] calls = { 0 };

                com.landawn.abacus.util.DoubleIterator iter = DoubleStream.zip(a, b, (x, y) -> {
                    if (++calls[0] == 3) {
                        throw new IllegalStateException("zip2");
                    }

                    return x + y;
                }).iterator();
                final com.landawn.abacus.util.DoubleIterator zip2 = iter;
                assertEquals("zip2", assertThrows(IllegalStateException.class, () -> bulk(zip2, useList)).getMessage());
                assertEquals(3, calls[0]);
                assertArrayEquals(new double[] { 44, 55 }, drain(iter));
                assertEquals(5, calls[0]);

                calls[0] = 0;
                iter = DoubleStream.zip(a, b, c, (x, y, z) -> {
                    if (++calls[0] == 2) {
                        throw new IllegalStateException("zip3");
                    }

                    return x + y + z;
                }).iterator();
                final com.landawn.abacus.util.DoubleIterator zip3 = iter;
                assertEquals("zip3", assertThrows(IllegalStateException.class, () -> bulk(zip3, useList)).getMessage());
                assertArrayEquals(new double[] { 333, 444, 555 }, drain(iter));
                assertEquals(5, calls[0]);

                calls[0] = 0;
                iter = DoubleStream.zip(a, b, 0.5, 0.25, (x, y) -> {
                    if (++calls[0] == 3) {
                        throw new IllegalStateException("pad2");
                    }

                    return x + y;
                }).iterator();
                final com.landawn.abacus.util.DoubleIterator pad2 = iter;
                assertEquals("pad2", assertThrows(IllegalStateException.class, () -> bulk(pad2, useList)).getMessage());
                assertArrayEquals(new double[] { 33, 44, 55, 60.5 }, drain(iter));
                assertEquals(7, calls[0]);

                calls[0] = 0;
                iter = DoubleStream.zip(a, b, c, 0.5, 0.25, 0.125, (x, y, z) -> {
                    if (++calls[0] == 6) {
                        throw new IllegalStateException("pad3");
                    }

                    return x + y + z;
                }).iterator();
                final com.landawn.abacus.util.DoubleIterator pad3 = iter;
                assertEquals("pad3", assertThrows(IllegalStateException.class, () -> bulk(pad3, useList)).getMessage());
                assertArrayEquals(new double[] { 660.5, 700.75 }, drain(iter));
                assertEquals(8, calls[0]);

                calls[0] = 0;
                iter = DoubleStream.merge(new double[] { 1, 3, 5 }, new double[] { 2, 4, 6 }, (x, y) -> {
                    if (++calls[0] == 3) {
                        throw new IllegalStateException("merge");
                    }

                    return x <= y ? com.landawn.abacus.util.MergeResult.TAKE_FIRST : com.landawn.abacus.util.MergeResult.TAKE_SECOND;
                }).iterator();
                final com.landawn.abacus.util.DoubleIterator merge = iter;
                assertEquals("merge", assertThrows(IllegalStateException.class, () -> bulk(merge, useList)).getMessage());
                assertArrayEquals(new double[] { 3, 4, 5, 6 }, drain(iter));
                assertEquals(6, calls[0]);
            }

            final IllegalStateException e = assertThrows(IllegalStateException.class, () -> DoubleStream.zip(a, b, (x, y) -> {
                if (x == 4) {
                    throw new IllegalStateException("stream");
                }

                return x + y;
            }).toArray());
            assertEquals("stream", e.getMessage());
        }

        private Object bulk(final com.landawn.abacus.util.DoubleIterator iterator, final boolean useList) {
            return useList ? iterator.toList() : iterator.toArray();
        }

        // G095-03: merge(double[], double[]) toArray()/toDoubleList() fill an exactly sized array; values and the order and number of
        // nextSelector calls (a stateful selector makes the result depend on them) equal the element-by-element traversal
        @Test
        public void testMergeArrays_bulkTerminalsMatchElementwiseTraversal() {
            final Random rnd = new Random(9503);

            for (int round = 0; round < 400; round++) {
                final double[] a = randomArray(rnd, 10);
                final double[] b = randomArray(rnd, 10);

                if (rnd.nextBoolean()) {
                    Arrays.sort(a);
                    Arrays.sort(b);
                }

                final boolean stateful = rnd.nextBoolean();

                // reference: the merge loop written out, with the same selector rule
                final com.landawn.abacus.util.DoubleList expectedList = new com.landawn.abacus.util.DoubleList();
                final List<String> expectedCalls = new ArrayList<>();
                int i = 0;
                int j = 0;

                while (i < a.length || j < b.length) {
                    if (i < a.length && (j >= b.length || takeFirst(stateful, a[i], b[j], expectedCalls))) {
                        expectedList.add(a[i++]);
                    } else {
                        expectedList.add(b[j++]);
                    }
                }

                final double[] expected = expectedList.toArray();

                final List<String> calls = new ArrayList<>();
                assertArrayEquals(expected, drain(mergeOf(a, b, stateful, calls).iterator()));
                assertEquals(expectedCalls, calls);

                calls.clear();
                assertArrayEquals(expected, mergeOf(a, b, stateful, calls).toArray());
                assertEquals(expectedCalls, calls);

                calls.clear();
                final com.landawn.abacus.util.DoubleList list = mergeOf(a, b, stateful, calls).toDoubleList();
                assertArrayEquals(expected, list.toArray());
                assertEquals(expectedCalls, calls);
                list.add(1.0);
                assertEquals(expected.length + 1, list.size());

                final int k = rnd.nextInt(expected.length + 2);
                calls.clear();
                assertArrayEquals(suffix(expected, k), mergeOf(a, b, stateful, calls).skip(k).toArray());
                assertEquals(expectedCalls, calls);

                calls.clear();
                final com.landawn.abacus.util.DoubleIterator iter = mergeOf(a, b, stateful, calls).iterator();
                final int consumed = Math.min(k, expected.length);

                for (int n = 0; n < consumed; n++) {
                    iter.nextDouble();
                }

                assertArrayEquals(suffix(expected, consumed), rnd.nextBoolean() ? iter.toArray() : iter.toList().toArray());
                assertEquals(expectedCalls, calls);
                assertFalse(iter.hasNext());
                assertEquals(0, iter.toArray().length);
            }
        }

        private boolean takeFirst(final boolean stateful, final double x, final double y, final List<String> calls) {
            calls.add(x + "," + y);
            return stateful ? calls.size() % 3 != 0 : Double.compare(x, y) <= 0;
        }

        private DoubleStream mergeOf(final double[] a, final double[] b, final boolean stateful, final List<String> calls) {
            return DoubleStream.merge(a, b, (x, y) -> takeFirst(stateful, x, y, calls) ? com.landawn.abacus.util.MergeResult.TAKE_FIRST
                    : com.landawn.abacus.util.MergeResult.TAKE_SECOND);
        }

        // G095-02: flatten(double[][], true) (rectangular/mildly jagged walk and the sparse jagged walk) and
        // flatten(double[][], value, vertically) toArray()/toDoubleList() fill an exactly sized array; pinned against plain
        // column-major / row-major references, also after skip(k) and partial iteration
        @Test
        public void testFlatten_bulkTerminalsMatchReference() {
            final Random rnd = new Random(9502);

            for (int round = 0; round < 600; round++) {
                final int shape = rnd.nextInt(3);
                final int rows = shape == 2 ? 5 + rnd.nextInt(6) : rnd.nextInt(7);
                final double[][] a = new double[rows][];
                final int width = 1 + rnd.nextInt(6);

                for (int r = 0; r < rows; r++) {
                    if (shape == 1) {
                        a[r] = new double[width];
                        Arrays.fill(a[r], r + 0.5);
                    } else if (shape == 2) {
                        // one long row among null, empty and one-element rows: rows * maxLen > 4 * n selects the jagged walk
                        a[r] = r == rows / 2 ? randomArray(rnd, 40) : rnd.nextInt(3) == 0 ? null : new double[rnd.nextInt(2)];
                    } else {
                        a[r] = rnd.nextInt(5) == 0 ? null : randomArray(rnd, 6);
                    }
                }

                int maxLen = 0;

                for (final double[] row : a) {
                    maxLen = Math.max(maxLen, row == null ? 0 : row.length);
                }

                final double pad = samples[rnd.nextInt(samples.length)];
                final com.landawn.abacus.util.DoubleList vertical = new com.landawn.abacus.util.DoubleList();
                final com.landawn.abacus.util.DoubleList alignedVertical = new com.landawn.abacus.util.DoubleList();
                final com.landawn.abacus.util.DoubleList alignedHorizontal = new com.landawn.abacus.util.DoubleList();

                for (int col = 0; col < maxLen; col++) {
                    for (final double[] row : a) {
                        if (row != null && col < row.length) {
                            vertical.add(row[col]);
                            alignedVertical.add(row[col]);
                        } else {
                            alignedVertical.add(pad);
                        }
                    }
                }

                for (final double[] row : a) {
                    for (int col = 0; col < maxLen; col++) {
                        alignedHorizontal.add(row != null && col < row.length ? row[col] : pad);
                    }
                }

                for (int mode = 0; mode < 3; mode++) {
                    final double[] expected = (mode == 0 ? vertical : mode == 1 ? alignedVertical : alignedHorizontal).toArray();

                    assertArrayEquals(expected, flattenOf(a, mode, pad).toArray());
                    assertArrayEquals(expected, flattenOf(a, mode, pad).toDoubleList().toArray());
                    assertArrayEquals(expected, drain(flattenOf(a, mode, pad).iterator()));

                    final int k = rnd.nextInt(expected.length + 2);
                    assertArrayEquals(suffix(expected, k), flattenOf(a, mode, pad).skip(k).toArray());

                    final com.landawn.abacus.util.DoubleIterator iter = flattenOf(a, mode, pad).iterator();
                    final int consumed = Math.min(k, expected.length);

                    for (int n = 0; n < consumed; n++) {
                        iter.nextDouble();
                    }

                    assertArrayEquals(suffix(expected, consumed), rnd.nextBoolean() ? iter.toArray() : iter.toList().toArray());
                    assertFalse(iter.hasNext());
                    assertEquals(0, iter.toList().size());
                }
            }
        }

        private DoubleStream flattenOf(final double[][] a, final int mode, final double pad) {
            return mode == 0 ? DoubleStream.flatten(a, true) : DoubleStream.flatten(a, pad, mode == 1);
        }

        private WeakReference<Object> payloadRef;

        private boolean awaitCleared(final WeakReference<?> ref) {
            for (int i = 0; i < 50 && ref.get() != null; i++) {
                System.gc();

                try {
                    Thread.sleep(10);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            return ref.get() == null;
        }

        // Built in a separate frame so that only the returned stream references the payload array.
        private DoubleStream concatOverPrivateArray(final int n) {
            final double[] payload = new double[n];

            for (int i = 0; i < n; i++) {
                payload[i] = i % 100;
            }

            payloadRef = new WeakReference<>(payload);

            return DoubleStream.concat(Arrays.asList(DoubleStream.of(payload), null, DoubleStream.of(-1, -2)));
        }

        // Built in a separate frame so that only the returned stream references the sorted() stream (and its sorted copy).
        private DoubleStream sortedThenAppend(final int n) {
            final double[] values = new double[n];

            for (int i = 0; i < n; i++) {
                values[i] = 99 - i % 100;
            }

            final DoubleStream sorted = DoubleStream.of(values).sorted();
            payloadRef = new WeakReference<>(sorted);

            return sorted.append(DoubleStream.of(-1, -2));
        }

        // G095-04: an exhausted (and closed) input of concat(Collection) is no longer reachable from the concatenation while later inputs run
        @Test
        public void testConcat_releasesExhaustedInputWhileLaterInputsRun() {
            final DoubleStream stream = concatOverPrivateArray(1 << 20);
            final boolean[] released = { false };
            final double[] sum = { 0 };
            final List<Double> tail = new ArrayList<>();

            stream.forEach(x -> {
                if (x < 0) {
                    if (x == -1) {
                        released[0] = awaitCleared(payloadRef);
                    }

                    tail.add(x);
                } else {
                    sum[0] += x;
                }
            });

            double expected = 0;

            for (int i = 0; i < 1 << 20; i++) {
                expected += i % 100;
            }

            assertEquals(expected, sum[0]);
            assertEquals(Arrays.asList(-1.0, -2.0), tail);
            assertTrue(released[0], "the exhausted first input (and its array) is still reachable from the concatenation");
        }

        // G095-04: append(DoubleStream) delegates to concat(this, other): the exhausted sorted() receiver (and its sorted copy) is released too
        @Test
        public void testAppend_releasesExhaustedSortedReceiverWhileAppendedStreamRuns() {
            final DoubleStream stream = sortedThenAppend(1 << 20);
            final boolean[] released = { false };
            final List<Double> seen = new ArrayList<>();

            stream.forEach(x -> {
                if (x == -1 && seen.size() == 3) {
                    released[0] = awaitCleared(payloadRef);
                }

                if (x < 0 || seen.size() < 3) {
                    seen.add(x);
                }
            });

            assertEquals(Arrays.asList(0.0, 0.0, 0.0, -1.0, -2.0), seen);
            assertTrue(released[0], "the exhausted sorted receiver (and its sorted copy) is still reachable from the appended stream");
        }

        // G095-04: element order, null inputs, close order (each exhausted input before the next one is opened, the rest on close),
        // and the caller's collection / varargs array are never modified
        @Test
        public void testConcat_orderNullInputsAndCloseOrder() {
            final List<String> log = new ArrayList<>();
            final DoubleStream a = DoubleStream.of(1, 2).onClose(() -> log.add("closeA"));
            final DoubleStream b = DoubleStream.of(3).onClose(() -> log.add("closeB"));
            final DoubleStream c = DoubleStream.of(4, 5).onClose(() -> log.add("closeC"));
            final DoubleStream d = DoubleStream.of(6).onClose(() -> log.add("closeD"));
            final List<DoubleStream> input = new ArrayList<>(Arrays.asList(a, null, b, null, c, d));
            final DoubleStream stream = DoubleStream.concat(input);
            final DoubleIteratorEx iter = stream.iteratorEx();
            final List<Double> seen = new ArrayList<>();

            while (seen.size() < 4 && iter.hasNext()) {
                final double x = iter.nextDouble();
                seen.add(x);
                log.add("next" + x);
            }

            stream.close();

            assertEquals(Arrays.asList(1.0, 2.0, 3.0, 4.0), seen);
            assertEquals(Arrays.asList("next1.0", "next2.0", "closeA", "next3.0", "closeB", "next4.0", "closeC", "closeD"), log);
            assertEquals(6, input.size());
            assertSame(a, input.get(0));
            assertNull(input.get(1));
            assertSame(b, input.get(2));
            assertNull(input.get(3));
            assertSame(c, input.get(4));
            assertSame(d, input.get(5));
            assertThrows(IllegalStateException.class, a::toArray);
            assertThrows(IllegalStateException.class, d::toArray);

            final DoubleStream e = DoubleStream.of(7, 8);
            final DoubleStream f = DoubleStream.of(9);
            final DoubleStream[] array = { e, null, f };
            assertArrayEquals(new double[] { 7, 8, 9 }, DoubleStream.concat(array).toArray());
            assertSame(e, array[0]);
            assertNull(array[1]);
            assertSame(f, array[2]);

            assertEquals(0, DoubleStream.concat(Arrays.asList((DoubleStream) null, null)).count());
            assertEquals(0, DoubleStream.concat(new ArrayList<DoubleStream>()).count());
            assertArrayEquals(new double[] { 1, 2, 3, 4 }, DoubleStream.of(1, 2).append(DoubleStream.of(3, 4)).toArray());
            assertArrayEquals(new double[] { 3, 4, 1, 2 }, DoubleStream.of(1, 2).prepend(DoubleStream.of(3, 4)).toArray());
            assertEquals(5, DoubleStream.concat(DoubleStream.of(1, 2), DoubleStream.empty(), DoubleStream.of(3, 4, 5)).count());
            assertEquals(com.landawn.abacus.util.DoubleList.of(1, 3, 4),
                    DoubleStream.concat(Arrays.asList(DoubleStream.of(1, 2), DoubleStream.of(3, 4))).filter(x -> x != 2).toDoubleList());
            assertArrayEquals(new double[] { 2, 3 },
                    DoubleStream.concat(Arrays.asList(DoubleStream.of(1, 2), DoubleStream.of(3, 4))).skip(1).limit(2).toArray());
        }

        // G095-04: a failing close of an exhausted input and an already-closed input propagate from hasNext(); a retry continues;
        // the remaining inputs are closed with the concatenation
        @Test
        public void testConcat_failingCloseAndClosedInputThenRetry() {
            final List<String> log = new ArrayList<>();
            final DoubleStream a = DoubleStream.of(1).onClose(() -> {
                log.add("closeA");
                throw new IllegalStateException("closeA failed");
            });
            final DoubleStream closed = DoubleStream.of(99);
            closed.close();
            final DoubleStream c = DoubleStream.of(3).onClose(() -> log.add("closeC"));
            final DoubleStream stream = DoubleStream.concat(Arrays.asList(a, closed, c));
            final DoubleIteratorEx iter = stream.iteratorEx();

            assertEquals(1, iter.nextDouble());
            final IllegalStateException closeFailure = assertThrows(IllegalStateException.class, iter::hasNext);
            assertEquals("closeA failed", closeFailure.getMessage());
            assertEquals(Arrays.asList("closeA"), log);

            // retry: `a` is closed already (no second close call); the next input is closed and is rejected when reached
            final IllegalStateException rejected = assertThrows(IllegalStateException.class, iter::hasNext);
            assertEquals("This stream is already terminated.", rejected.getMessage());
            assertEquals(Arrays.asList("closeA"), log);

            // retry again: traversal continues with the input after the rejected one
            assertTrue(iter.hasNext());
            assertEquals(3, iter.nextDouble());
            assertFalse(iter.hasNext());
            assertThrows(java.util.NoSuchElementException.class, iter::nextDouble);

            stream.close();
            assertEquals(Arrays.asList("closeA", "closeC"), log);

            // the same stream twice: the second occurrence is rejected when it is reached
            final DoubleStream twice = DoubleStream.of(1, 2);
            final List<Double> collected = new ArrayList<>();
            final IllegalStateException repeated = assertThrows(IllegalStateException.class, () -> DoubleStream.concat(twice, twice).forEach(collected::add));
            assertEquals("This stream is already terminated.", repeated.getMessage());
            assertEquals(Arrays.asList(1.0, 2.0), collected);

            // a terminal operation that fails on a closed input closes every other input
            final List<String> log2 = new ArrayList<>();
            final DoubleStream x = DoubleStream.of(1).onClose(() -> log2.add("closeX"));
            final DoubleStream closed2 = DoubleStream.of(2);
            closed2.close();
            final DoubleStream z = DoubleStream.of(3).onClose(() -> log2.add("closeZ"));
            assertThrows(IllegalStateException.class, () -> DoubleStream.concat(x, closed2, z).toArray());
            assertEquals(Arrays.asList("closeX", "closeZ"), log2);

            // a failing close handler of a not yet exhausted input still runs when the concatenation is closed
            final List<String> log3 = new ArrayList<>();
            final DoubleStream p = DoubleStream.of(1, 2).onClose(() -> log3.add("closeP"));
            final DoubleStream q = DoubleStream.of(3).onClose(() -> {
                log3.add("closeQ");
                throw new IllegalStateException("closeQ failed");
            });
            final DoubleStream r = DoubleStream.of(4).onClose(() -> log3.add("closeR"));
            final DoubleStream pqr = DoubleStream.concat(Arrays.asList(p, q, r));
            assertEquals(1, pqr.iteratorEx().nextDouble());
            final IllegalStateException onClose = assertThrows(IllegalStateException.class, pqr::close);
            assertEquals("closeQ failed", onClose.getMessage());
            assertEquals(Arrays.asList("closeP", "closeQ", "closeR"), log3);
        }

        // G095-04: parallel append/concat (the concatenation's iterator is shared by the workers) delivers every element and closes every input
        @Test
        public void testConcat_parallelDeliversAllElementsAndClosesInputs() {
            for (int round = 0; round < 20; round++) {
                final AtomicInteger closes = new AtomicInteger();
                final List<DoubleStream> inputs = new ArrayList<>();
                final com.landawn.abacus.util.DoubleList expected = new com.landawn.abacus.util.DoubleList();

                for (int i = 0; i < 50; i++) {
                    final double[] values = new double[i % 7];

                    for (int j = 0; j < values.length; j++) {
                        values[j] = i + j;
                    }

                    if (i % 10 == 3) {
                        inputs.add(null);
                    } else {
                        inputs.add(DoubleStream.of(values).onClose(closes::incrementAndGet));
                        expected.addAll(values);
                    }
                }

                final double[] actual = DoubleStream.concat(inputs).parallel(4).map(v -> v + 1).toArray();
                final double[] sortedExpected = expected.toArray();

                for (int i = 0; i < sortedExpected.length; i++) {
                    sortedExpected[i] += 1;
                }

                Arrays.sort(sortedExpected);
                Arrays.sort(actual);
                assertArrayEquals(sortedExpected, actual);
                assertEquals(45, closes.get());

                final AtomicInteger closes2 = new AtomicInteger();
                final double[] appended = DoubleStream.of(1, 2, 3)
                        .onClose(closes2::incrementAndGet)
                        .parallel(3)
                        .append(DoubleStream.of(4, 5).onClose(closes2::incrementAndGet))
                        .toArray();
                Arrays.sort(appended);
                assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, appended);
                assertEquals(2, closes2.get());
            }
        }
    }
}
