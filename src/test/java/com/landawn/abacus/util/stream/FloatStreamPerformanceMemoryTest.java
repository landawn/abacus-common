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
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Stream performance and memory regression contracts. */
@Tag("slow-test")
public class FloatStreamPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G096 end ----
    // ---- perf review 2026-10-08 G098 begin ----
    @Nested
    class G098 {
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
        private FloatStream concatOverPrivateArray(final int n) {
            final float[] payload = new float[n];

            for (int i = 0; i < n; i++) {
                payload[i] = i % 100;
            }

            payloadRef = new WeakReference<>(payload);

            return FloatStream.concat(Arrays.asList(FloatStream.of(payload), null, FloatStream.of(-1f, -2f)));
        }

        // Built in a separate frame so that only the returned stream references the sorted() stream (and its sorted copy).
        private FloatStream sortedThenAppend(final int n) {
            final float[] values = new float[n];

            for (int i = 0; i < n; i++) {
                values[i] = 99 - i % 100;
            }

            final FloatStream sorted = FloatStream.of(values).sorted();
            payloadRef = new WeakReference<>(sorted);

            return sorted.append(FloatStream.of(-1f, -2f));
        }

        // G098-03: an exhausted (and closed) input of concat(Collection) is no longer reachable from the concatenation while later inputs run
        @Test
        void testConcat_releasesExhaustedInputWhileLaterInputsRun() {
            final FloatStream stream = concatOverPrivateArray(1 << 20);
            final boolean[] released = { false };
            final double[] sum = { 0 };
            final List<Float> tail = new ArrayList<>();

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
            assertEquals(Arrays.asList(-1f, -2f), tail);
            assertTrue(released[0], "the exhausted first input (and its array) is still reachable from the concatenation");
        }

        // G098-03: append(FloatStream) delegates to concat(this, other): the exhausted sorted() receiver (and its sorted copy) is released too
        @Test
        void testAppend_releasesExhaustedSortedReceiverWhileAppendedStreamRuns() {
            final FloatStream stream = sortedThenAppend(1 << 20);
            final boolean[] released = { false };
            final List<Float> seen = new ArrayList<>();

            stream.forEach(x -> {
                if (x == -1 && seen.size() == 3) {
                    released[0] = awaitCleared(payloadRef);
                }

                if (x < 0 || seen.size() < 3) {
                    seen.add(x);
                }
            });

            assertEquals(Arrays.asList(0f, 0f, 0f, -1f, -2f), seen);
            assertTrue(released[0], "the exhausted sorted receiver (and its sorted copy) is still reachable from the appended stream");
        }

        // G098-03: element order, null inputs, close order (each exhausted input before the next one is opened, the rest on close),
        // and the caller's collection / varargs array are never modified
        @Test
        void testConcat_orderNullInputsAndCloseOrder() {
            final List<String> log = new ArrayList<>();
            final FloatStream a = FloatStream.of(1f, 2f).onClose(() -> log.add("closeA"));
            final FloatStream b = FloatStream.of(3f).onClose(() -> log.add("closeB"));
            final FloatStream c = FloatStream.of(4f, 5f).onClose(() -> log.add("closeC"));
            final FloatStream d = FloatStream.of(6f).onClose(() -> log.add("closeD"));
            final List<FloatStream> input = new ArrayList<>(Arrays.asList(a, null, b, null, c, d));
            final FloatStream stream = FloatStream.concat(input);
            final FloatIteratorEx iter = stream.iteratorEx();
            final List<Float> seen = new ArrayList<>();

            while (seen.size() < 4 && iter.hasNext()) {
                final float x = iter.nextFloat();
                seen.add(x);
                log.add("next" + (int) x);
            }

            stream.close();

            assertEquals(Arrays.asList(1f, 2f, 3f, 4f), seen);
            assertEquals(Arrays.asList("next1", "next2", "closeA", "next3", "closeB", "next4", "closeC", "closeD"), log);
            assertEquals(6, input.size());
            assertSame(a, input.get(0));
            assertNull(input.get(1));
            assertSame(b, input.get(2));
            assertNull(input.get(3));
            assertSame(c, input.get(4));
            assertSame(d, input.get(5));
            assertThrows(IllegalStateException.class, a::toArray);
            assertThrows(IllegalStateException.class, d::toArray);

            final FloatStream e = FloatStream.of(7f, 8f);
            final FloatStream f = FloatStream.of(9f);
            final FloatStream[] array = { e, null, f };
            assertArrayEquals(new float[] { 7, 8, 9 }, FloatStream.concat(array).toArray());
            assertSame(e, array[0]);
            assertNull(array[1]);
            assertSame(f, array[2]);

            assertEquals(0, FloatStream.concat(Arrays.asList((FloatStream) null, null)).count());
            assertEquals(0, FloatStream.concat(new ArrayList<FloatStream>()).count());
            assertArrayEquals(new float[] { 1, 2, 3, 4 }, FloatStream.of(1f, 2f).append(FloatStream.of(3f, 4f)).toArray());
            assertArrayEquals(new float[] { 3, 4, 1, 2 }, FloatStream.of(1f, 2f).prepend(FloatStream.of(3f, 4f)).toArray());
            assertEquals(5, FloatStream.concat(FloatStream.of(1f, 2f), FloatStream.empty(), FloatStream.of(3f, 4f, 5f)).count());
            assertEquals(com.landawn.abacus.util.FloatList.of(1f, 3f, 4f),
                    FloatStream.concat(Arrays.asList(FloatStream.of(1f, 2f), FloatStream.of(3f, 4f))).filter(x -> x != 2).toFloatList());
            assertArrayEquals(new float[] { 2, 3 },
                    FloatStream.concat(Arrays.asList(FloatStream.of(1f, 2f), FloatStream.of(3f, 4f))).skip(1).limit(2).toArray());
            final float[] special = FloatStream.concat(Arrays.asList(FloatStream.of(Float.NaN, -0.0f), null, FloatStream.of(0.0f))).toArray();
            assertEquals(Float.floatToRawIntBits(Float.NaN), Float.floatToRawIntBits(special[0]));
            assertEquals(Float.floatToRawIntBits(-0.0f), Float.floatToRawIntBits(special[1]));
            assertEquals(Float.floatToRawIntBits(0.0f), Float.floatToRawIntBits(special[2]));
        }

        // G098-03: a failing close of an exhausted input and an already-closed input propagate from hasNext(); a retry continues;
        // the remaining inputs are closed with the concatenation
        @Test
        void testConcat_failingCloseAndClosedInputThenRetry() {
            final List<String> log = new ArrayList<>();
            final FloatStream a = FloatStream.of(1f).onClose(() -> {
                log.add("closeA");
                throw new IllegalStateException("closeA failed");
            });
            final FloatStream closed = FloatStream.of(99f);
            closed.close();
            final FloatStream c = FloatStream.of(3f).onClose(() -> log.add("closeC"));
            final FloatStream stream = FloatStream.concat(Arrays.asList(a, closed, c));
            final FloatIteratorEx iter = stream.iteratorEx();

            assertEquals(1f, iter.nextFloat());
            final IllegalStateException closeFailure = assertThrows(IllegalStateException.class, iter::hasNext);
            assertEquals("closeA failed", closeFailure.getMessage());
            assertEquals(Arrays.asList("closeA"), log);

            // retry: `a` is closed already (no second close call); the next input is closed and is rejected when reached
            final IllegalStateException rejected = assertThrows(IllegalStateException.class, iter::hasNext);
            assertEquals("This stream is already terminated.", rejected.getMessage());
            assertEquals(Arrays.asList("closeA"), log);

            // retry again: traversal continues with the input after the rejected one
            assertTrue(iter.hasNext());
            assertEquals(3f, iter.nextFloat());
            assertFalse(iter.hasNext());
            assertThrows(java.util.NoSuchElementException.class, iter::nextFloat);

            stream.close();
            assertEquals(Arrays.asList("closeA", "closeC"), log);

            // the same stream twice: the second occurrence is rejected when it is reached
            final FloatStream twice = FloatStream.of(1f, 2f);
            final List<Float> collected = new ArrayList<>();
            final IllegalStateException repeated = assertThrows(IllegalStateException.class, () -> FloatStream.concat(twice, twice).forEach(collected::add));
            assertEquals("This stream is already terminated.", repeated.getMessage());
            assertEquals(Arrays.asList(1f, 2f), collected);

            // a terminal operation that fails on a closed input closes every other input
            final List<String> log2 = new ArrayList<>();
            final FloatStream x = FloatStream.of(1f).onClose(() -> log2.add("closeX"));
            final FloatStream closed2 = FloatStream.of(2f);
            closed2.close();
            final FloatStream z = FloatStream.of(3f).onClose(() -> log2.add("closeZ"));
            assertThrows(IllegalStateException.class, () -> FloatStream.concat(x, closed2, z).toArray());
            assertEquals(Arrays.asList("closeX", "closeZ"), log2);

            // a failing close handler of a not yet exhausted input still runs when the concatenation is closed
            final List<String> log3 = new ArrayList<>();
            final FloatStream p = FloatStream.of(1f, 2f).onClose(() -> log3.add("closeP"));
            final FloatStream q = FloatStream.of(3f).onClose(() -> {
                log3.add("closeQ");
                throw new IllegalStateException("closeQ failed");
            });
            final FloatStream r = FloatStream.of(4f).onClose(() -> log3.add("closeR"));
            final FloatStream pqr = FloatStream.concat(Arrays.asList(p, q, r));
            assertEquals(1f, pqr.iteratorEx().nextFloat());
            final IllegalStateException onClose = assertThrows(IllegalStateException.class, pqr::close);
            assertEquals("closeQ failed", onClose.getMessage());
            assertEquals(Arrays.asList("closeP", "closeQ", "closeR"), log3);
        }

        // G098-03: parallel append/concat (the concatenation's iterator is shared by the workers) delivers every element and closes every input
        @Test
        void testConcat_parallelDeliversAllElementsAndClosesInputs() {
            for (int round = 0; round < 20; round++) {
                final AtomicInteger closes = new AtomicInteger();
                final List<FloatStream> inputs = new ArrayList<>();
                final List<Float> expected = new ArrayList<>();

                for (int i = 0; i < 50; i++) {
                    final float[] values = new float[i % 7];

                    for (int j = 0; j < values.length; j++) {
                        values[j] = i * 10 + j;
                    }

                    if (i % 10 == 3) {
                        inputs.add(null);
                    } else {
                        inputs.add(FloatStream.of(values).onClose(closes::incrementAndGet));

                        for (final float v : values) {
                            expected.add(v);
                        }
                    }
                }

                final float[] actual = FloatStream.concat(inputs).parallel(4).map(v -> v + 1).toArray();
                final float[] sortedExpected = new float[expected.size()];

                for (int i = 0; i < sortedExpected.length; i++) {
                    sortedExpected[i] = expected.get(i) + 1;
                }

                Arrays.sort(sortedExpected);
                Arrays.sort(actual);
                assertArrayEquals(sortedExpected, actual);
                assertEquals(45, closes.get());

                final AtomicInteger closes2 = new AtomicInteger();
                final float[] appended = FloatStream.of(1f, 2f, 3f)
                        .onClose(closes2::incrementAndGet)
                        .parallel(3)
                        .append(FloatStream.of(4f, 5f).onClose(closes2::incrementAndGet))
                        .toArray();
                Arrays.sort(appended);
                assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, appended);
                assertEquals(2, closes2.get());
            }
        }
    }
}
