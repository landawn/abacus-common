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
public class ByteStreamPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G088 end ----
    // ---- perf review 2026-10-08 G091 begin ----
    @Nested
    class G091 {
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
        private ByteStream concatOverPrivateArray(final int n) {
            final byte[] payload = new byte[n];

            for (int i = 0; i < n; i++) {
                payload[i] = (byte) (i % 100);
            }

            payloadRef = new WeakReference<>(payload);

            return ByteStream.concat(Arrays.asList(ByteStream.of(payload), null, ByteStream.of((byte) -1, (byte) -2)));
        }

        // Built in a separate frame so that only the returned stream references the sorted() stream (and its sorted copy).
        private ByteStream sortedThenAppend(final int n) {
            final byte[] values = new byte[n];

            for (int i = 0; i < n; i++) {
                values[i] = (byte) (99 - i % 100);
            }

            final ByteStream sorted = ByteStream.of(values).sorted();
            payloadRef = new WeakReference<>(sorted);

            return sorted.append(ByteStream.of((byte) -1, (byte) -2));
        }

        // G091-01: an exhausted (and closed) input of concat(Collection) is no longer reachable from the concatenation while later inputs run
        @Test
        void testConcat_releasesExhaustedInputWhileLaterInputsRun() {
            final ByteStream stream = concatOverPrivateArray(1 << 20);
            final boolean[] released = { false };
            final long[] sum = { 0 };
            final List<Byte> tail = new ArrayList<>();

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

            long expected = 0;

            for (int i = 0; i < 1 << 20; i++) {
                expected += i % 100;
            }

            assertEquals(expected, sum[0]);
            assertEquals(Arrays.asList((byte) -1, (byte) -2), tail);
            assertTrue(released[0], "the exhausted first input (and its array) is still reachable from the concatenation");
        }

        // G091-01: append(ByteStream) delegates to concat(this, other): the exhausted sorted() receiver (and its sorted copy) is released too
        @Test
        void testAppend_releasesExhaustedSortedReceiverWhileAppendedStreamRuns() {
            final ByteStream stream = sortedThenAppend(1 << 20);
            final boolean[] released = { false };
            final List<Byte> seen = new ArrayList<>();

            stream.forEach(x -> {
                if (x == -1 && seen.size() == 3) {
                    released[0] = awaitCleared(payloadRef);
                }

                if (x < 0 || seen.size() < 3) {
                    seen.add(x);
                }
            });

            assertEquals(Arrays.asList((byte) 0, (byte) 0, (byte) 0, (byte) -1, (byte) -2), seen);
            assertTrue(released[0], "the exhausted sorted receiver (and its sorted copy) is still reachable from the appended stream");
        }

        // G091-01: element order, null inputs, close order (each exhausted input before the next one is opened, the rest on close),
        // and the caller's collection / varargs array are never modified
        @Test
        void testConcat_orderNullInputsAndCloseOrder() {
            final List<String> log = new ArrayList<>();
            final ByteStream a = ByteStream.of((byte) 1, (byte) 2).onClose(() -> log.add("closeA"));
            final ByteStream b = ByteStream.of((byte) 3).onClose(() -> log.add("closeB"));
            final ByteStream c = ByteStream.of((byte) 4, (byte) 5).onClose(() -> log.add("closeC"));
            final ByteStream d = ByteStream.of((byte) 6).onClose(() -> log.add("closeD"));
            final List<ByteStream> input = new ArrayList<>(Arrays.asList(a, null, b, null, c, d));
            final ByteStream stream = ByteStream.concat(input);
            final ByteIteratorEx iter = stream.iteratorEx();
            final List<Byte> seen = new ArrayList<>();

            while (seen.size() < 4 && iter.hasNext()) {
                final byte x = iter.nextByte();
                seen.add(x);
                log.add("next" + x);
            }

            stream.close();

            assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4), seen);
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

            final ByteStream e = ByteStream.of((byte) 7, (byte) 8);
            final ByteStream f = ByteStream.of((byte) 9);
            final ByteStream[] array = { e, null, f };
            assertArrayEquals(new byte[] { 7, 8, 9 }, ByteStream.concat(array).toArray());
            assertSame(e, array[0]);
            assertNull(array[1]);
            assertSame(f, array[2]);

            assertEquals(0, ByteStream.concat(Arrays.asList((ByteStream) null, null)).count());
            assertEquals(0, ByteStream.concat(new ArrayList<ByteStream>()).count());
            assertArrayEquals(new byte[] { 1, 2, 3, 4 }, ByteStream.of((byte) 1, (byte) 2).append(ByteStream.of((byte) 3, (byte) 4)).toArray());
            assertArrayEquals(new byte[] { 3, 4, 1, 2 }, ByteStream.of((byte) 1, (byte) 2).prepend(ByteStream.of((byte) 3, (byte) 4)).toArray());
            assertEquals(5, ByteStream.concat(ByteStream.of((byte) 1, (byte) 2), ByteStream.empty(), ByteStream.of((byte) 3, (byte) 4, (byte) 5)).count());
            assertEquals(com.landawn.abacus.util.ByteList.of((byte) 1, (byte) 3, (byte) 4),
                    ByteStream.concat(Arrays.asList(ByteStream.of((byte) 1, (byte) 2), ByteStream.of((byte) 3, (byte) 4))).filter(x -> x != 2).toByteList());
            assertArrayEquals(new byte[] { 2, 3 },
                    ByteStream.concat(Arrays.asList(ByteStream.of((byte) 1, (byte) 2), ByteStream.of((byte) 3, (byte) 4))).skip(1).limit(2).toArray());
        }

        // G091-01: a failing close of an exhausted input and an already-closed input propagate from hasNext(); a retry continues;
        // the remaining inputs are closed with the concatenation
        @Test
        void testConcat_failingCloseAndClosedInputThenRetry() {
            final List<String> log = new ArrayList<>();
            final ByteStream a = ByteStream.of((byte) 1).onClose(() -> {
                log.add("closeA");
                throw new IllegalStateException("closeA failed");
            });
            final ByteStream closed = ByteStream.of((byte) 99);
            closed.close();
            final ByteStream c = ByteStream.of((byte) 3).onClose(() -> log.add("closeC"));
            final ByteStream stream = ByteStream.concat(Arrays.asList(a, closed, c));
            final ByteIteratorEx iter = stream.iteratorEx();

            assertEquals(1, iter.nextByte());
            final IllegalStateException closeFailure = assertThrows(IllegalStateException.class, iter::hasNext);
            assertEquals("closeA failed", closeFailure.getMessage());
            assertEquals(Arrays.asList("closeA"), log);

            // retry: `a` is closed already (no second close call); the next input is closed and is rejected when reached
            final IllegalStateException rejected = assertThrows(IllegalStateException.class, iter::hasNext);
            assertEquals("This stream is already terminated.", rejected.getMessage());
            assertEquals(Arrays.asList("closeA"), log);

            // retry again: traversal continues with the input after the rejected one
            assertTrue(iter.hasNext());
            assertEquals(3, iter.nextByte());
            assertFalse(iter.hasNext());
            assertThrows(java.util.NoSuchElementException.class, iter::nextByte);

            stream.close();
            assertEquals(Arrays.asList("closeA", "closeC"), log);

            // the same stream twice: the second occurrence is rejected when it is reached
            final ByteStream twice = ByteStream.of((byte) 1, (byte) 2);
            final List<Byte> collected = new ArrayList<>();
            final IllegalStateException repeated = assertThrows(IllegalStateException.class, () -> ByteStream.concat(twice, twice).forEach(collected::add));
            assertEquals("This stream is already terminated.", repeated.getMessage());
            assertEquals(Arrays.asList((byte) 1, (byte) 2), collected);

            // a terminal operation that fails on a closed input closes every other input
            final List<String> log2 = new ArrayList<>();
            final ByteStream x = ByteStream.of((byte) 1).onClose(() -> log2.add("closeX"));
            final ByteStream closed2 = ByteStream.of((byte) 2);
            closed2.close();
            final ByteStream z = ByteStream.of((byte) 3).onClose(() -> log2.add("closeZ"));
            assertThrows(IllegalStateException.class, () -> ByteStream.concat(x, closed2, z).toArray());
            assertEquals(Arrays.asList("closeX", "closeZ"), log2);

            // a failing close handler of a not yet exhausted input still runs when the concatenation is closed
            final List<String> log3 = new ArrayList<>();
            final ByteStream p = ByteStream.of((byte) 1, (byte) 2).onClose(() -> log3.add("closeP"));
            final ByteStream q = ByteStream.of((byte) 3).onClose(() -> {
                log3.add("closeQ");
                throw new IllegalStateException("closeQ failed");
            });
            final ByteStream r = ByteStream.of((byte) 4).onClose(() -> log3.add("closeR"));
            final ByteStream pqr = ByteStream.concat(Arrays.asList(p, q, r));
            assertEquals(1, pqr.iteratorEx().nextByte());
            final IllegalStateException onClose = assertThrows(IllegalStateException.class, pqr::close);
            assertEquals("closeQ failed", onClose.getMessage());
            assertEquals(Arrays.asList("closeP", "closeQ", "closeR"), log3);
        }

        // G091-01: parallel append/concat (the concatenation's iterator is shared by the workers) delivers every element and closes every input
        @Test
        void testConcat_parallelDeliversAllElementsAndClosesInputs() {
            for (int round = 0; round < 20; round++) {
                final AtomicInteger closes = new AtomicInteger();
                final List<ByteStream> inputs = new ArrayList<>();
                final List<Byte> expected = new ArrayList<>();

                for (int i = 0; i < 50; i++) {
                    final byte[] values = new byte[i % 7];

                    for (int j = 0; j < values.length; j++) {
                        values[j] = (byte) (i + j);
                        expected.add(values[j]);
                    }

                    inputs.add(i % 10 == 3 ? null : ByteStream.of(values).onClose(closes::incrementAndGet));

                    if (i % 10 == 3) {
                        for (int j = 0; j < values.length; j++) {
                            expected.remove(expected.size() - 1);
                        }
                    }
                }

                final byte[] actual = ByteStream.concat(inputs).parallel(4).map(v -> (byte) (v + 1)).toArray();
                final byte[] sortedExpected = new byte[expected.size()];

                for (int i = 0; i < sortedExpected.length; i++) {
                    sortedExpected[i] = (byte) (expected.get(i) + 1);
                }

                Arrays.sort(sortedExpected);
                Arrays.sort(actual);
                assertArrayEquals(sortedExpected, actual);
                assertEquals(45, closes.get());

                final AtomicInteger closes2 = new AtomicInteger();
                final byte[] appended = ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .onClose(closes2::incrementAndGet)
                        .parallel(3)
                        .append(ByteStream.of((byte) 4, (byte) 5).onClose(closes2::incrementAndGet))
                        .toArray();
                Arrays.sort(appended);
                assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, appended);
                assertEquals(2, closes2.get());
            }
        }
    }
}
