package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.ShortIterator;

/**
 * Cycle-2 review fixes 2026-09-24 for the seven primitive stream families (fixer FX2-PRIM).
 */
public class PrimitiveStreamsReview20260924bTest extends TestBase {

    private static final String NULL_OPTIONAL_MESSAGE = "mapper returned a null (java\\.util\\.)?Optional(Byte|Char|Short|Int|Long|Float|Double); "
            + "return (java\\.util\\.)?Optional(Byte|Char|Short|Int|Long|Float|Double)\\.empty\\(\\) for no result";

    private static void assertClosed(final BaseStream<?, ?, ?, ?, ?, ?, ?, ?> s) {
        assertThrows(IllegalStateException.class, s::count);
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-110 parallel zipWith: when boxing b (or c) fails, the receiver is closed, as on the sequential path
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void c110_parallelByteZipWithClosedArgumentClosesReceiver() {
        for (int src = 0; src < 2; src++) {
            for (int overload = 0; overload < 4; overload++) {
                final AtomicInteger aClosed = new AtomicInteger();
                final AtomicInteger bClosed = new AtomicInteger();
                final AtomicInteger cClosed = new AtomicInteger();
                // src 0: ParallelArrayByteStream, src 1: ParallelIteratorByteStream
                final ByteStream a = (src == 0 ? ByteStream.of(new byte[] { 1, 2, 3 }) : ByteStream.of(ByteIterator.of(new byte[] { 1, 2, 3 }))).parallel(3)
                        .onClose(aClosed::incrementAndGet);
                assertTrue(a instanceof ParallelArrayByteStream == (src == 0));
                final ByteStream b = ByteStream.of(new byte[] { 4, 5 }).onClose(bClosed::incrementAndGet);
                final ByteStream c = ByteStream.of(new byte[] { 4, 5 }).onClose(cClosed::incrementAndGet);
                // overloads 0/2: b is closed; overloads 1/3: b is open and c is closed
                if (overload == 1 || overload == 3) {
                    c.close();
                } else {
                    b.close();
                }
                // U31-03 (2026-09-25): the counters are deliberately not reset - the closed argument's handler ran once via close()
                assertEquals(1, (overload == 1 || overload == 3 ? cClosed : bClosed).get());
                final int ov = overload;
                assertThrows(IllegalStateException.class, () -> {
                    switch (ov) {
                        case 0:
                            a.zipWith(b, (x, y) -> x);
                            break;
                        case 1:
                            a.zipWith(b, c, (x, y, w) -> x);
                            break;
                        case 2:
                            a.zipWith(b, (byte) 0, (byte) 0, (x, y) -> x);
                            break;
                        default:
                            a.zipWith(b, c, (byte) 0, (byte) 0, (byte) 0, (x, y, w) -> x);
                    }
                });
                assertEquals(1, aClosed.get(), "receiver close handler, overload " + ov + ", src " + src);
                assertClosed(a);
                // U31-03 (2026-09-25): the already-closed argument was never opened, so its handlers are not re-run
                assertEquals(1, (ov == 1 || ov == 3 ? cClosed : bClosed).get(), "closed argument re-closed, overload " + ov + ", src " + src);
                if (ov == 1 || ov == 3) {
                    // b had been opened (boxed) before c failed, so it is closed too; like the sequential zip
                    assertEquals(1, bClosed.get());
                    assertClosed(b);
                }
            }
        }
    }

    @Test
    public void c110_parallelCharZipWithClosedArgumentClosesReceiver() {
        for (int src = 0; src < 2; src++) {
            for (int overload = 0; overload < 4; overload++) {
                final AtomicInteger aClosed = new AtomicInteger();
                final AtomicInteger bClosed = new AtomicInteger();
                final AtomicInteger cClosed = new AtomicInteger();
                // src 0: ParallelArrayCharStream, src 1: ParallelIteratorCharStream
                final CharStream a = (src == 0 ? CharStream.of(new char[] { 1, 2, 3 }) : CharStream.of(CharIterator.of(new char[] { 1, 2, 3 }))).parallel(3)
                        .onClose(aClosed::incrementAndGet);
                assertTrue(a instanceof ParallelArrayCharStream == (src == 0));
                final CharStream b = CharStream.of(new char[] { 4, 5 }).onClose(bClosed::incrementAndGet);
                final CharStream c = CharStream.of(new char[] { 4, 5 }).onClose(cClosed::incrementAndGet);
                // overloads 0/2: b is closed; overloads 1/3: b is open and c is closed
                if (overload == 1 || overload == 3) {
                    c.close();
                } else {
                    b.close();
                }
                // U31-03 (2026-09-25): the counters are deliberately not reset - the closed argument's handler ran once via close()
                assertEquals(1, (overload == 1 || overload == 3 ? cClosed : bClosed).get());
                final int ov = overload;
                assertThrows(IllegalStateException.class, () -> {
                    switch (ov) {
                        case 0:
                            a.zipWith(b, (x, y) -> x);
                            break;
                        case 1:
                            a.zipWith(b, c, (x, y, w) -> x);
                            break;
                        case 2:
                            a.zipWith(b, (char) 0, (char) 0, (x, y) -> x);
                            break;
                        default:
                            a.zipWith(b, c, (char) 0, (char) 0, (char) 0, (x, y, w) -> x);
                    }
                });
                assertEquals(1, aClosed.get(), "receiver close handler, overload " + ov + ", src " + src);
                assertClosed(a);
                // U31-03 (2026-09-25): the already-closed argument was never opened, so its handlers are not re-run
                assertEquals(1, (ov == 1 || ov == 3 ? cClosed : bClosed).get(), "closed argument re-closed, overload " + ov + ", src " + src);
                if (ov == 1 || ov == 3) {
                    // b had been opened (boxed) before c failed, so it is closed too; like the sequential zip
                    assertEquals(1, bClosed.get());
                    assertClosed(b);
                }
            }
        }
    }

    @Test
    public void c110_parallelShortZipWithClosedArgumentClosesReceiver() {
        for (int src = 0; src < 2; src++) {
            for (int overload = 0; overload < 4; overload++) {
                final AtomicInteger aClosed = new AtomicInteger();
                final AtomicInteger bClosed = new AtomicInteger();
                final AtomicInteger cClosed = new AtomicInteger();
                // src 0: ParallelArrayShortStream, src 1: ParallelIteratorShortStream
                final ShortStream a = (src == 0 ? ShortStream.of(new short[] { 1, 2, 3 }) : ShortStream.of(ShortIterator.of(new short[] { 1, 2, 3 }))).parallel(3)
                        .onClose(aClosed::incrementAndGet);
                assertTrue(a instanceof ParallelArrayShortStream == (src == 0));
                final ShortStream b = ShortStream.of(new short[] { 4, 5 }).onClose(bClosed::incrementAndGet);
                final ShortStream c = ShortStream.of(new short[] { 4, 5 }).onClose(cClosed::incrementAndGet);
                // overloads 0/2: b is closed; overloads 1/3: b is open and c is closed
                if (overload == 1 || overload == 3) {
                    c.close();
                } else {
                    b.close();
                }
                // U31-03 (2026-09-25): the counters are deliberately not reset - the closed argument's handler ran once via close()
                assertEquals(1, (overload == 1 || overload == 3 ? cClosed : bClosed).get());
                final int ov = overload;
                assertThrows(IllegalStateException.class, () -> {
                    switch (ov) {
                        case 0:
                            a.zipWith(b, (x, y) -> x);
                            break;
                        case 1:
                            a.zipWith(b, c, (x, y, w) -> x);
                            break;
                        case 2:
                            a.zipWith(b, (short) 0, (short) 0, (x, y) -> x);
                            break;
                        default:
                            a.zipWith(b, c, (short) 0, (short) 0, (short) 0, (x, y, w) -> x);
                    }
                });
                assertEquals(1, aClosed.get(), "receiver close handler, overload " + ov + ", src " + src);
                assertClosed(a);
                // U31-03 (2026-09-25): the already-closed argument was never opened, so its handlers are not re-run
                assertEquals(1, (ov == 1 || ov == 3 ? cClosed : bClosed).get(), "closed argument re-closed, overload " + ov + ", src " + src);
                if (ov == 1 || ov == 3) {
                    // b had been opened (boxed) before c failed, so it is closed too; like the sequential zip
                    assertEquals(1, bClosed.get());
                    assertClosed(b);
                }
            }
        }
    }

    @Test
    public void c110_parallelIntZipWithClosedArgumentClosesReceiver() {
        for (int src = 0; src < 2; src++) {
            for (int overload = 0; overload < 4; overload++) {
                final AtomicInteger aClosed = new AtomicInteger();
                final AtomicInteger bClosed = new AtomicInteger();
                final AtomicInteger cClosed = new AtomicInteger();
                // src 0: ParallelArrayIntStream, src 1: ParallelIteratorIntStream
                final IntStream a = (src == 0 ? IntStream.of(new int[] { 1, 2, 3 }) : IntStream.of(IntIterator.of(new int[] { 1, 2, 3 }))).parallel(3)
                        .onClose(aClosed::incrementAndGet);
                assertTrue(a instanceof ParallelArrayIntStream == (src == 0));
                final IntStream b = IntStream.of(new int[] { 4, 5 }).onClose(bClosed::incrementAndGet);
                final IntStream c = IntStream.of(new int[] { 4, 5 }).onClose(cClosed::incrementAndGet);
                // overloads 0/2: b is closed; overloads 1/3: b is open and c is closed
                if (overload == 1 || overload == 3) {
                    c.close();
                } else {
                    b.close();
                }
                // U31-03 (2026-09-25): the counters are deliberately not reset - the closed argument's handler ran once via close()
                assertEquals(1, (overload == 1 || overload == 3 ? cClosed : bClosed).get());
                final int ov = overload;
                assertThrows(IllegalStateException.class, () -> {
                    switch (ov) {
                        case 0:
                            a.zipWith(b, (x, y) -> x);
                            break;
                        case 1:
                            a.zipWith(b, c, (x, y, w) -> x);
                            break;
                        case 2:
                            a.zipWith(b, (int) 0, (int) 0, (x, y) -> x);
                            break;
                        default:
                            a.zipWith(b, c, (int) 0, (int) 0, (int) 0, (x, y, w) -> x);
                    }
                });
                assertEquals(1, aClosed.get(), "receiver close handler, overload " + ov + ", src " + src);
                assertClosed(a);
                // U31-03 (2026-09-25): the already-closed argument was never opened, so its handlers are not re-run
                assertEquals(1, (ov == 1 || ov == 3 ? cClosed : bClosed).get(), "closed argument re-closed, overload " + ov + ", src " + src);
                if (ov == 1 || ov == 3) {
                    // b had been opened (boxed) before c failed, so it is closed too; like the sequential zip
                    assertEquals(1, bClosed.get());
                    assertClosed(b);
                }
            }
        }
    }

    @Test
    public void c110_parallelLongZipWithClosedArgumentClosesReceiver() {
        for (int src = 0; src < 2; src++) {
            for (int overload = 0; overload < 4; overload++) {
                final AtomicInteger aClosed = new AtomicInteger();
                final AtomicInteger bClosed = new AtomicInteger();
                final AtomicInteger cClosed = new AtomicInteger();
                // src 0: ParallelArrayLongStream, src 1: ParallelIteratorLongStream
                final LongStream a = (src == 0 ? LongStream.of(new long[] { 1, 2, 3 }) : LongStream.of(LongIterator.of(new long[] { 1, 2, 3 }))).parallel(3)
                        .onClose(aClosed::incrementAndGet);
                assertTrue(a instanceof ParallelArrayLongStream == (src == 0));
                final LongStream b = LongStream.of(new long[] { 4, 5 }).onClose(bClosed::incrementAndGet);
                final LongStream c = LongStream.of(new long[] { 4, 5 }).onClose(cClosed::incrementAndGet);
                // overloads 0/2: b is closed; overloads 1/3: b is open and c is closed
                if (overload == 1 || overload == 3) {
                    c.close();
                } else {
                    b.close();
                }
                // U31-03 (2026-09-25): the counters are deliberately not reset - the closed argument's handler ran once via close()
                assertEquals(1, (overload == 1 || overload == 3 ? cClosed : bClosed).get());
                final int ov = overload;
                assertThrows(IllegalStateException.class, () -> {
                    switch (ov) {
                        case 0:
                            a.zipWith(b, (x, y) -> x);
                            break;
                        case 1:
                            a.zipWith(b, c, (x, y, w) -> x);
                            break;
                        case 2:
                            a.zipWith(b, (long) 0, (long) 0, (x, y) -> x);
                            break;
                        default:
                            a.zipWith(b, c, (long) 0, (long) 0, (long) 0, (x, y, w) -> x);
                    }
                });
                assertEquals(1, aClosed.get(), "receiver close handler, overload " + ov + ", src " + src);
                assertClosed(a);
                // U31-03 (2026-09-25): the already-closed argument was never opened, so its handlers are not re-run
                assertEquals(1, (ov == 1 || ov == 3 ? cClosed : bClosed).get(), "closed argument re-closed, overload " + ov + ", src " + src);
                if (ov == 1 || ov == 3) {
                    // b had been opened (boxed) before c failed, so it is closed too; like the sequential zip
                    assertEquals(1, bClosed.get());
                    assertClosed(b);
                }
            }
        }
    }

    @Test
    public void c110_parallelFloatZipWithClosedArgumentClosesReceiver() {
        for (int src = 0; src < 2; src++) {
            for (int overload = 0; overload < 4; overload++) {
                final AtomicInteger aClosed = new AtomicInteger();
                final AtomicInteger bClosed = new AtomicInteger();
                final AtomicInteger cClosed = new AtomicInteger();
                // src 0: ParallelArrayFloatStream, src 1: ParallelIteratorFloatStream
                final FloatStream a = (src == 0 ? FloatStream.of(new float[] { 1, 2, 3 }) : FloatStream.of(FloatIterator.of(new float[] { 1, 2, 3 }))).parallel(3)
                        .onClose(aClosed::incrementAndGet);
                assertTrue(a instanceof ParallelArrayFloatStream == (src == 0));
                final FloatStream b = FloatStream.of(new float[] { 4, 5 }).onClose(bClosed::incrementAndGet);
                final FloatStream c = FloatStream.of(new float[] { 4, 5 }).onClose(cClosed::incrementAndGet);
                // overloads 0/2: b is closed; overloads 1/3: b is open and c is closed
                if (overload == 1 || overload == 3) {
                    c.close();
                } else {
                    b.close();
                }
                // U31-03 (2026-09-25): the counters are deliberately not reset - the closed argument's handler ran once via close()
                assertEquals(1, (overload == 1 || overload == 3 ? cClosed : bClosed).get());
                final int ov = overload;
                assertThrows(IllegalStateException.class, () -> {
                    switch (ov) {
                        case 0:
                            a.zipWith(b, (x, y) -> x);
                            break;
                        case 1:
                            a.zipWith(b, c, (x, y, w) -> x);
                            break;
                        case 2:
                            a.zipWith(b, (float) 0, (float) 0, (x, y) -> x);
                            break;
                        default:
                            a.zipWith(b, c, (float) 0, (float) 0, (float) 0, (x, y, w) -> x);
                    }
                });
                assertEquals(1, aClosed.get(), "receiver close handler, overload " + ov + ", src " + src);
                assertClosed(a);
                // U31-03 (2026-09-25): the already-closed argument was never opened, so its handlers are not re-run
                assertEquals(1, (ov == 1 || ov == 3 ? cClosed : bClosed).get(), "closed argument re-closed, overload " + ov + ", src " + src);
                if (ov == 1 || ov == 3) {
                    // b had been opened (boxed) before c failed, so it is closed too; like the sequential zip
                    assertEquals(1, bClosed.get());
                    assertClosed(b);
                }
            }
        }
    }

    @Test
    public void c110_parallelDoubleZipWithClosedArgumentClosesReceiver() {
        for (int src = 0; src < 2; src++) {
            for (int overload = 0; overload < 4; overload++) {
                final AtomicInteger aClosed = new AtomicInteger();
                final AtomicInteger bClosed = new AtomicInteger();
                final AtomicInteger cClosed = new AtomicInteger();
                // src 0: ParallelArrayDoubleStream, src 1: ParallelIteratorDoubleStream
                final DoubleStream a = (src == 0 ? DoubleStream.of(new double[] { 1, 2, 3 }) : DoubleStream.of(DoubleIterator.of(new double[] { 1, 2, 3 }))).parallel(3)
                        .onClose(aClosed::incrementAndGet);
                assertTrue(a instanceof ParallelArrayDoubleStream == (src == 0));
                final DoubleStream b = DoubleStream.of(new double[] { 4, 5 }).onClose(bClosed::incrementAndGet);
                final DoubleStream c = DoubleStream.of(new double[] { 4, 5 }).onClose(cClosed::incrementAndGet);
                // overloads 0/2: b is closed; overloads 1/3: b is open and c is closed
                if (overload == 1 || overload == 3) {
                    c.close();
                } else {
                    b.close();
                }
                // U31-03 (2026-09-25): the counters are deliberately not reset - the closed argument's handler ran once via close()
                assertEquals(1, (overload == 1 || overload == 3 ? cClosed : bClosed).get());
                final int ov = overload;
                assertThrows(IllegalStateException.class, () -> {
                    switch (ov) {
                        case 0:
                            a.zipWith(b, (x, y) -> x);
                            break;
                        case 1:
                            a.zipWith(b, c, (x, y, w) -> x);
                            break;
                        case 2:
                            a.zipWith(b, (double) 0, (double) 0, (x, y) -> x);
                            break;
                        default:
                            a.zipWith(b, c, (double) 0, (double) 0, (double) 0, (x, y, w) -> x);
                    }
                });
                assertEquals(1, aClosed.get(), "receiver close handler, overload " + ov + ", src " + src);
                assertClosed(a);
                // U31-03 (2026-09-25): the already-closed argument was never opened, so its handlers are not re-run
                assertEquals(1, (ov == 1 || ov == 3 ? cClosed : bClosed).get(), "closed argument re-closed, overload " + ov + ", src " + src);
                if (ov == 1 || ov == 3) {
                    // b had been opened (boxed) before c failed, so it is closed too; like the sequential zip
                    assertEquals(1, bClosed.get());
                    assertClosed(b);
                }
            }
        }
    }

    @Test
    public void c110_parallelZipWithHappyPathAndSequentialParity() {
        final int[] expected = { 5, 7, 9 };
        int[] r = IntStream.of(1, 2, 3).parallel(3).zipWith(IntStream.of(4, 5, 6), Integer::sum).sorted().toArray();
        assertArrayEquals(expected, r);
        r = IntStream.of(IntIterator.of(1, 2, 3)).parallel(3).zipWith(IntStream.of(4, 5, 6), IntStream.of(0, 0, 0), (x, y, z) -> x + y + z)
                .sorted()
                .toArray();
        assertArrayEquals(expected, r);
        r = IntStream.of(1, 2, 3).parallel(3).zipWith(IntStream.of(4, 5), 0, 100, Integer::sum).sorted().toArray();
        assertArrayEquals(new int[] { 5, 7, 103 }, r);
        final double[] d = DoubleStream.of(DoubleIterator.of(1, 2))
                .parallel(2)
                .zipWith(DoubleStream.of(10), DoubleStream.of(100, 200, 300), 0, -1, -2, (x, y, z) -> x + y + z)
                .sorted()
                .toArray();
        assertArrayEquals(new double[] { 111, 201, 299 }, d);

        // the closed-argument failure now leaves the receiver in the same state on the sequential and parallel paths
        for (final int threads : new int[] { 1, 3 }) {
            final AtomicInteger closed = new AtomicInteger();
            final IntStream a = IntStream.of(1, 2, 3).parallel(threads).onClose(closed::incrementAndGet);
            final IntStream b = IntStream.of(4, 5);
            b.close();
            assertThrows(IllegalStateException.class, () -> a.zipWith(b, Integer::sum));
            assertEquals(1, closed.get(), "threads " + threads);
            assertClosed(a);
        }

        // null arguments are still rejected by validation (closing the receiver), not by the new helper
        final IntStream a = IntStream.of(1, 2, 3).parallel(3);
        assertThrows(IllegalArgumentException.class, () -> a.zipWith(null, Integer::sum));
        assertClosed(a);
    }

    // ------------------------------------------------------------------------------------------------------------
    // R8 N1 / R9-03: primitive mapPartial/mapPartialJdk report a null Optional with Stream's message
    // ------------------------------------------------------------------------------------------------------------

    private static void assertNullOptionalNpe(final org.junit.jupiter.api.function.Executable e) {
        final NullPointerException npe = assertThrows(NullPointerException.class, e);
        // The message names the mapper's own optional type (OptionalInt, java.util.OptionalInt, ...), see D5-07.
        assertTrue(npe.getMessage().matches(NULL_OPTIONAL_MESSAGE), npe.getMessage());
    }

    @Test
    public void n1_primitiveMapPartialNullOptionalMessage() {
        assertNullOptionalNpe(() -> ByteStream.of((byte) 1).mapPartial(x -> null).toArray());
        assertNullOptionalNpe(() -> ByteStream.of(ByteIterator.of((byte) 1)).mapPartial(x -> null).count());
        assertNullOptionalNpe(() -> ByteStream.of(new byte[200]).parallel(4).mapPartial(x -> null).toArray());
        assertNullOptionalNpe(() -> CharStream.of((char) 1).mapPartial(x -> null).toArray());
        assertNullOptionalNpe(() -> CharStream.of(CharIterator.of((char) 1)).mapPartial(x -> null).count());
        assertNullOptionalNpe(() -> CharStream.of(new char[200]).parallel(4).mapPartial(x -> null).toArray());
        assertNullOptionalNpe(() -> ShortStream.of((short) 1).mapPartial(x -> null).toArray());
        assertNullOptionalNpe(() -> ShortStream.of(ShortIterator.of((short) 1)).mapPartial(x -> null).count());
        assertNullOptionalNpe(() -> ShortStream.of(new short[200]).parallel(4).mapPartial(x -> null).toArray());
        assertNullOptionalNpe(() -> IntStream.of((int) 1).mapPartial(x -> null).toArray());
        assertNullOptionalNpe(() -> IntStream.of(IntIterator.of((int) 1)).mapPartial(x -> null).count());
        assertNullOptionalNpe(() -> IntStream.of(new int[200]).parallel(4).mapPartial(x -> null).toArray());
        assertNullOptionalNpe(() -> LongStream.of((long) 1).mapPartial(x -> null).toArray());
        assertNullOptionalNpe(() -> LongStream.of(LongIterator.of((long) 1)).mapPartial(x -> null).count());
        assertNullOptionalNpe(() -> LongStream.of(new long[200]).parallel(4).mapPartial(x -> null).toArray());
        assertNullOptionalNpe(() -> FloatStream.of((float) 1).mapPartial(x -> null).toArray());
        assertNullOptionalNpe(() -> FloatStream.of(FloatIterator.of((float) 1)).mapPartial(x -> null).count());
        assertNullOptionalNpe(() -> FloatStream.of(new float[200]).parallel(4).mapPartial(x -> null).toArray());
        assertNullOptionalNpe(() -> DoubleStream.of((double) 1).mapPartial(x -> null).toArray());
        assertNullOptionalNpe(() -> DoubleStream.of(DoubleIterator.of((double) 1)).mapPartial(x -> null).count());
        assertNullOptionalNpe(() -> DoubleStream.of(new double[200]).parallel(4).mapPartial(x -> null).toArray());
        assertNullOptionalNpe(() -> IntStream.of((int) 1).mapPartialJdk(x -> null).toArray());
        assertNullOptionalNpe(() -> IntStream.of(new int[200]).parallel(4).mapPartialJdk(x -> null).toArray());
        assertNullOptionalNpe(() -> LongStream.of((long) 1).mapPartialJdk(x -> null).toArray());
        assertNullOptionalNpe(() -> LongStream.of(new long[200]).parallel(4).mapPartialJdk(x -> null).toArray());
        assertNullOptionalNpe(() -> DoubleStream.of((double) 1).mapPartialJdk(x -> null).toArray());
        assertNullOptionalNpe(() -> DoubleStream.of(new double[200]).parallel(4).mapPartialJdk(x -> null).toArray());
    }

    @Test
    public void n1_primitiveMapPartialValidResultsUnchanged() {
        assertArrayEquals(new int[] { 2, 4 },
                IntStream.of(1, 2, 3, 4).mapPartial(x -> x % 2 == 0 ? com.landawn.abacus.util.u.OptionalInt.of(x) : com.landawn.abacus.util.u.OptionalInt.empty())
                        .toArray());
        assertArrayEquals(new long[] { 1, 3 },
                LongStream.of(1, 2, 3).mapPartialJdk(x -> x % 2 == 1 ? java.util.OptionalLong.of(x) : java.util.OptionalLong.empty()).toArray());
        assertArrayEquals(new char[] { 'B' },
                CharStream.of('a', 'b').mapPartial(c -> c == 'b' ? com.landawn.abacus.util.u.OptionalChar.of('B') : com.landawn.abacus.util.u.OptionalChar.empty())
                        .toArray());
        assertEquals(0, FloatStream.empty().mapPartial(x -> null).count()); // the mapper is never called on an empty stream
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-112 (doc lock): the 2-arg collect fails only when per-thread containers are actually combined
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void c112_primitiveCollectFailsOnlyWhenContainersAreCombined() {
        assertArrayEquals(new long[] { 1 }, ByteStream.of((byte) 5).parallel(4).collect(() -> new long[1], (acc, x) -> acc[0]++));
        assertArrayEquals(new long[] { 1000 }, ByteStream.of(new byte[1000]).collect(() -> new long[1], (acc, x) -> acc[0]++));
        assertThrows(IllegalArgumentException.class, () -> ByteStream.of(new byte[1000]).parallel(4).collect(() -> new long[1], (acc, x) -> acc[0]++));
        assertArrayEquals(new long[] { 1 }, CharStream.of((char) 5).parallel(4).collect(() -> new long[1], (acc, x) -> acc[0]++));
        assertArrayEquals(new long[] { 1000 }, CharStream.of(new char[1000]).collect(() -> new long[1], (acc, x) -> acc[0]++));
        assertThrows(IllegalArgumentException.class, () -> CharStream.of(new char[1000]).parallel(4).collect(() -> new long[1], (acc, x) -> acc[0]++));
        assertArrayEquals(new long[] { 1 }, ShortStream.of((short) 5).parallel(4).collect(() -> new long[1], (acc, x) -> acc[0]++));
        assertArrayEquals(new long[] { 1000 }, ShortStream.of(new short[1000]).collect(() -> new long[1], (acc, x) -> acc[0]++));
        assertThrows(IllegalArgumentException.class, () -> ShortStream.of(new short[1000]).parallel(4).collect(() -> new long[1], (acc, x) -> acc[0]++));
        assertArrayEquals(new long[] { 1 }, IntStream.of((int) 5).parallel(4).collect(() -> new long[1], (acc, x) -> acc[0]++));
        assertArrayEquals(new long[] { 1000 }, IntStream.of(new int[1000]).collect(() -> new long[1], (acc, x) -> acc[0]++));
        assertThrows(IllegalArgumentException.class, () -> IntStream.of(new int[1000]).parallel(4).collect(() -> new long[1], (acc, x) -> acc[0]++));
        assertArrayEquals(new long[] { 1 }, LongStream.of((long) 5).parallel(4).collect(() -> new long[1], (acc, x) -> acc[0]++));
        assertArrayEquals(new long[] { 1000 }, LongStream.of(new long[1000]).collect(() -> new long[1], (acc, x) -> acc[0]++));
        assertThrows(IllegalArgumentException.class, () -> LongStream.of(new long[1000]).parallel(4).collect(() -> new long[1], (acc, x) -> acc[0]++));
        assertArrayEquals(new long[] { 1 }, FloatStream.of((float) 5).parallel(4).collect(() -> new long[1], (acc, x) -> acc[0]++));
        assertArrayEquals(new long[] { 1000 }, FloatStream.of(new float[1000]).collect(() -> new long[1], (acc, x) -> acc[0]++));
        assertThrows(IllegalArgumentException.class, () -> FloatStream.of(new float[1000]).parallel(4).collect(() -> new long[1], (acc, x) -> acc[0]++));
        assertArrayEquals(new long[] { 1 }, DoubleStream.of((double) 5).parallel(4).collect(() -> new long[1], (acc, x) -> acc[0]++));
        assertArrayEquals(new long[] { 1000 }, DoubleStream.of(new double[1000]).collect(() -> new long[1], (acc, x) -> acc[0]++));
        assertThrows(IllegalArgumentException.class, () -> DoubleStream.of(new double[1000]).parallel(4).collect(() -> new long[1], (acc, x) -> acc[0]++));
        // supported container types combine fine in parallel
        assertEquals(1000, IntStream.range(0, 1000).parallel(4).collect(com.landawn.abacus.util.IntList::new, com.landawn.abacus.util.IntList::add).size());
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-101: collect(Supplier, ..) rejects a null container with NullPointerException, on empty input too,
    // sequential and parallel (every worker's container), and closes the stream
    // ------------------------------------------------------------------------------------------------------------

    private static void assertSupplierReturnedNull(final AtomicInteger closed, final BaseStream<?, ?, ?, ?, ?, ?, ?, ?> s,
            final org.junit.jupiter.api.function.Executable e) {
        final NullPointerException npe = assertThrows(NullPointerException.class, e);
        assertEquals("supplier returned null", npe.getMessage());
        assertEquals(1, closed.get());
        assertClosed(s);
        closed.set(0);
    }

    private static BaseStream.ParallelSettings ps(final BaseStream.SplitStrategy strategy) {
        return BaseStream.ParallelSettings.builder().splitStrategy(strategy).maxThreadNum(4).build();
    }

    @Test
    public void c101_byteCollectNullContainerIsNpe() {
        final AtomicInteger closed = new AtomicInteger();
        ByteStream s = ByteStream.of(new byte[] { 1, 2 }).onClose(closed::incrementAndGet);
        final ByteStream s1 = s;
        assertSupplierReturnedNull(closed, s1, () -> s1.collect(() -> null, (c, x) -> { }));
        s = ByteStream.of(new byte[] { 1, 2 }).onClose(closed::incrementAndGet);
        final ByteStream s2 = s;
        assertSupplierReturnedNull(closed, s2, () -> s2.collect(() -> null, (c, x) -> { }, (c1, c2) -> { }));
        // empty input: the JDK and the old code returned null here
        s = ByteStream.of(new byte[0]).onClose(closed::incrementAndGet);
        final ByteStream s3 = s;
        assertSupplierReturnedNull(closed, s3, () -> s3.collect(() -> null, (c, x) -> { }));
        s = ByteStream.of(ByteIterator.of(new byte[] { 1, 2 })).filter(x -> false).onClose(closed::incrementAndGet);
        final ByteStream s4 = s;
        assertSupplierReturnedNull(closed, s4, () -> s4.collect(() -> null, (c, x) -> { }, (c1, c2) -> { }));
        s = ByteStream.of(ByteIterator.of(new byte[] { 1, 2 })).onClose(closed::incrementAndGet);
        final ByteStream s5 = s;
        assertSupplierReturnedNull(closed, s5, () -> s5.collect(() -> null, (c, x) -> { }));
        // parallel: array (both split strategies) and iterator sources
        for (final BaseStream.SplitStrategy strategy : BaseStream.SplitStrategy.values()) {
            s = ByteStream.of(new byte[100]).parallel(ps(strategy)).onClose(closed::incrementAndGet);
            final ByteStream s6 = s;
            assertSupplierReturnedNull(closed, s6, () -> s6.collect(() -> null, (c, x) -> { }, (c1, c2) -> { }));
        }
        s = ByteStream.of(ByteIterator.of(new byte[100])).parallel(4).onClose(closed::incrementAndGet);
        final ByteStream s7 = s;
        assertSupplierReturnedNull(closed, s7, () -> s7.collect(() -> null, (c, x) -> { }));
        // a supplier that returns null only for some workers is still caught
        final AtomicInteger calls = new AtomicInteger();
        s = ByteStream.of(new byte[100]).parallel(ps(BaseStream.SplitStrategy.ARRAY)).onClose(closed::incrementAndGet);
        final ByteStream s8 = s;
        assertSupplierReturnedNull(closed, s8,
                () -> s8.collect(() -> calls.incrementAndGet() == 2 ? null : new java.util.ArrayList<Object>(), (c, x) -> c.add(x), java.util.List::addAll));
        // a real container is unaffected
        assertEquals(100, ByteStream.of(new byte[100]).parallel(4).collect(java.util.ArrayList<Object>::new, (c, x) -> c.add(x)).size());
        assertEquals(0, ByteStream.of(new byte[0]).collect(java.util.ArrayList<Object>::new, (c, x) -> c.add(x)).size());
    }

    @Test
    public void c101_charCollectNullContainerIsNpe() {
        final AtomicInteger closed = new AtomicInteger();
        CharStream s = CharStream.of(new char[] { 1, 2 }).onClose(closed::incrementAndGet);
        final CharStream s1 = s;
        assertSupplierReturnedNull(closed, s1, () -> s1.collect(() -> null, (c, x) -> { }));
        s = CharStream.of(new char[] { 1, 2 }).onClose(closed::incrementAndGet);
        final CharStream s2 = s;
        assertSupplierReturnedNull(closed, s2, () -> s2.collect(() -> null, (c, x) -> { }, (c1, c2) -> { }));
        // empty input: the JDK and the old code returned null here
        s = CharStream.of(new char[0]).onClose(closed::incrementAndGet);
        final CharStream s3 = s;
        assertSupplierReturnedNull(closed, s3, () -> s3.collect(() -> null, (c, x) -> { }));
        s = CharStream.of(CharIterator.of(new char[] { 1, 2 })).filter(x -> false).onClose(closed::incrementAndGet);
        final CharStream s4 = s;
        assertSupplierReturnedNull(closed, s4, () -> s4.collect(() -> null, (c, x) -> { }, (c1, c2) -> { }));
        s = CharStream.of(CharIterator.of(new char[] { 1, 2 })).onClose(closed::incrementAndGet);
        final CharStream s5 = s;
        assertSupplierReturnedNull(closed, s5, () -> s5.collect(() -> null, (c, x) -> { }));
        // parallel: array (both split strategies) and iterator sources
        for (final BaseStream.SplitStrategy strategy : BaseStream.SplitStrategy.values()) {
            s = CharStream.of(new char[100]).parallel(ps(strategy)).onClose(closed::incrementAndGet);
            final CharStream s6 = s;
            assertSupplierReturnedNull(closed, s6, () -> s6.collect(() -> null, (c, x) -> { }, (c1, c2) -> { }));
        }
        s = CharStream.of(CharIterator.of(new char[100])).parallel(4).onClose(closed::incrementAndGet);
        final CharStream s7 = s;
        assertSupplierReturnedNull(closed, s7, () -> s7.collect(() -> null, (c, x) -> { }));
        // a supplier that returns null only for some workers is still caught
        final AtomicInteger calls = new AtomicInteger();
        s = CharStream.of(new char[100]).parallel(ps(BaseStream.SplitStrategy.ARRAY)).onClose(closed::incrementAndGet);
        final CharStream s8 = s;
        assertSupplierReturnedNull(closed, s8,
                () -> s8.collect(() -> calls.incrementAndGet() == 2 ? null : new java.util.ArrayList<Object>(), (c, x) -> c.add(x), java.util.List::addAll));
        // a real container is unaffected
        assertEquals(100, CharStream.of(new char[100]).parallel(4).collect(java.util.ArrayList<Object>::new, (c, x) -> c.add(x)).size());
        assertEquals(0, CharStream.of(new char[0]).collect(java.util.ArrayList<Object>::new, (c, x) -> c.add(x)).size());
    }

    @Test
    public void c101_shortCollectNullContainerIsNpe() {
        final AtomicInteger closed = new AtomicInteger();
        ShortStream s = ShortStream.of(new short[] { 1, 2 }).onClose(closed::incrementAndGet);
        final ShortStream s1 = s;
        assertSupplierReturnedNull(closed, s1, () -> s1.collect(() -> null, (c, x) -> { }));
        s = ShortStream.of(new short[] { 1, 2 }).onClose(closed::incrementAndGet);
        final ShortStream s2 = s;
        assertSupplierReturnedNull(closed, s2, () -> s2.collect(() -> null, (c, x) -> { }, (c1, c2) -> { }));
        // empty input: the JDK and the old code returned null here
        s = ShortStream.of(new short[0]).onClose(closed::incrementAndGet);
        final ShortStream s3 = s;
        assertSupplierReturnedNull(closed, s3, () -> s3.collect(() -> null, (c, x) -> { }));
        s = ShortStream.of(ShortIterator.of(new short[] { 1, 2 })).filter(x -> false).onClose(closed::incrementAndGet);
        final ShortStream s4 = s;
        assertSupplierReturnedNull(closed, s4, () -> s4.collect(() -> null, (c, x) -> { }, (c1, c2) -> { }));
        s = ShortStream.of(ShortIterator.of(new short[] { 1, 2 })).onClose(closed::incrementAndGet);
        final ShortStream s5 = s;
        assertSupplierReturnedNull(closed, s5, () -> s5.collect(() -> null, (c, x) -> { }));
        // parallel: array (both split strategies) and iterator sources
        for (final BaseStream.SplitStrategy strategy : BaseStream.SplitStrategy.values()) {
            s = ShortStream.of(new short[100]).parallel(ps(strategy)).onClose(closed::incrementAndGet);
            final ShortStream s6 = s;
            assertSupplierReturnedNull(closed, s6, () -> s6.collect(() -> null, (c, x) -> { }, (c1, c2) -> { }));
        }
        s = ShortStream.of(ShortIterator.of(new short[100])).parallel(4).onClose(closed::incrementAndGet);
        final ShortStream s7 = s;
        assertSupplierReturnedNull(closed, s7, () -> s7.collect(() -> null, (c, x) -> { }));
        // a supplier that returns null only for some workers is still caught
        final AtomicInteger calls = new AtomicInteger();
        s = ShortStream.of(new short[100]).parallel(ps(BaseStream.SplitStrategy.ARRAY)).onClose(closed::incrementAndGet);
        final ShortStream s8 = s;
        assertSupplierReturnedNull(closed, s8,
                () -> s8.collect(() -> calls.incrementAndGet() == 2 ? null : new java.util.ArrayList<Object>(), (c, x) -> c.add(x), java.util.List::addAll));
        // a real container is unaffected
        assertEquals(100, ShortStream.of(new short[100]).parallel(4).collect(java.util.ArrayList<Object>::new, (c, x) -> c.add(x)).size());
        assertEquals(0, ShortStream.of(new short[0]).collect(java.util.ArrayList<Object>::new, (c, x) -> c.add(x)).size());
    }

    @Test
    public void c101_intCollectNullContainerIsNpe() {
        final AtomicInteger closed = new AtomicInteger();
        IntStream s = IntStream.of(new int[] { 1, 2 }).onClose(closed::incrementAndGet);
        final IntStream s1 = s;
        assertSupplierReturnedNull(closed, s1, () -> s1.collect(() -> null, (c, x) -> { }));
        s = IntStream.of(new int[] { 1, 2 }).onClose(closed::incrementAndGet);
        final IntStream s2 = s;
        assertSupplierReturnedNull(closed, s2, () -> s2.collect(() -> null, (c, x) -> { }, (c1, c2) -> { }));
        // empty input: the JDK and the old code returned null here
        s = IntStream.of(new int[0]).onClose(closed::incrementAndGet);
        final IntStream s3 = s;
        assertSupplierReturnedNull(closed, s3, () -> s3.collect(() -> null, (c, x) -> { }));
        s = IntStream.of(IntIterator.of(new int[] { 1, 2 })).filter(x -> false).onClose(closed::incrementAndGet);
        final IntStream s4 = s;
        assertSupplierReturnedNull(closed, s4, () -> s4.collect(() -> null, (c, x) -> { }, (c1, c2) -> { }));
        s = IntStream.of(IntIterator.of(new int[] { 1, 2 })).onClose(closed::incrementAndGet);
        final IntStream s5 = s;
        assertSupplierReturnedNull(closed, s5, () -> s5.collect(() -> null, (c, x) -> { }));
        // parallel: array (both split strategies) and iterator sources
        for (final BaseStream.SplitStrategy strategy : BaseStream.SplitStrategy.values()) {
            s = IntStream.of(new int[100]).parallel(ps(strategy)).onClose(closed::incrementAndGet);
            final IntStream s6 = s;
            assertSupplierReturnedNull(closed, s6, () -> s6.collect(() -> null, (c, x) -> { }, (c1, c2) -> { }));
        }
        s = IntStream.of(IntIterator.of(new int[100])).parallel(4).onClose(closed::incrementAndGet);
        final IntStream s7 = s;
        assertSupplierReturnedNull(closed, s7, () -> s7.collect(() -> null, (c, x) -> { }));
        // a supplier that returns null only for some workers is still caught
        final AtomicInteger calls = new AtomicInteger();
        s = IntStream.of(new int[100]).parallel(ps(BaseStream.SplitStrategy.ARRAY)).onClose(closed::incrementAndGet);
        final IntStream s8 = s;
        assertSupplierReturnedNull(closed, s8,
                () -> s8.collect(() -> calls.incrementAndGet() == 2 ? null : new java.util.ArrayList<Object>(), (c, x) -> c.add(x), java.util.List::addAll));
        // a real container is unaffected
        assertEquals(100, IntStream.of(new int[100]).parallel(4).collect(java.util.ArrayList<Object>::new, (c, x) -> c.add(x)).size());
        assertEquals(0, IntStream.of(new int[0]).collect(java.util.ArrayList<Object>::new, (c, x) -> c.add(x)).size());
    }

    @Test
    public void c101_longCollectNullContainerIsNpe() {
        final AtomicInteger closed = new AtomicInteger();
        LongStream s = LongStream.of(new long[] { 1, 2 }).onClose(closed::incrementAndGet);
        final LongStream s1 = s;
        assertSupplierReturnedNull(closed, s1, () -> s1.collect(() -> null, (c, x) -> { }));
        s = LongStream.of(new long[] { 1, 2 }).onClose(closed::incrementAndGet);
        final LongStream s2 = s;
        assertSupplierReturnedNull(closed, s2, () -> s2.collect(() -> null, (c, x) -> { }, (c1, c2) -> { }));
        // empty input: the JDK and the old code returned null here
        s = LongStream.of(new long[0]).onClose(closed::incrementAndGet);
        final LongStream s3 = s;
        assertSupplierReturnedNull(closed, s3, () -> s3.collect(() -> null, (c, x) -> { }));
        s = LongStream.of(LongIterator.of(new long[] { 1, 2 })).filter(x -> false).onClose(closed::incrementAndGet);
        final LongStream s4 = s;
        assertSupplierReturnedNull(closed, s4, () -> s4.collect(() -> null, (c, x) -> { }, (c1, c2) -> { }));
        s = LongStream.of(LongIterator.of(new long[] { 1, 2 })).onClose(closed::incrementAndGet);
        final LongStream s5 = s;
        assertSupplierReturnedNull(closed, s5, () -> s5.collect(() -> null, (c, x) -> { }));
        // parallel: array (both split strategies) and iterator sources
        for (final BaseStream.SplitStrategy strategy : BaseStream.SplitStrategy.values()) {
            s = LongStream.of(new long[100]).parallel(ps(strategy)).onClose(closed::incrementAndGet);
            final LongStream s6 = s;
            assertSupplierReturnedNull(closed, s6, () -> s6.collect(() -> null, (c, x) -> { }, (c1, c2) -> { }));
        }
        s = LongStream.of(LongIterator.of(new long[100])).parallel(4).onClose(closed::incrementAndGet);
        final LongStream s7 = s;
        assertSupplierReturnedNull(closed, s7, () -> s7.collect(() -> null, (c, x) -> { }));
        // a supplier that returns null only for some workers is still caught
        final AtomicInteger calls = new AtomicInteger();
        s = LongStream.of(new long[100]).parallel(ps(BaseStream.SplitStrategy.ARRAY)).onClose(closed::incrementAndGet);
        final LongStream s8 = s;
        assertSupplierReturnedNull(closed, s8,
                () -> s8.collect(() -> calls.incrementAndGet() == 2 ? null : new java.util.ArrayList<Object>(), (c, x) -> c.add(x), java.util.List::addAll));
        // a real container is unaffected
        assertEquals(100, LongStream.of(new long[100]).parallel(4).collect(java.util.ArrayList<Object>::new, (c, x) -> c.add(x)).size());
        assertEquals(0, LongStream.of(new long[0]).collect(java.util.ArrayList<Object>::new, (c, x) -> c.add(x)).size());
    }

    @Test
    public void c101_floatCollectNullContainerIsNpe() {
        final AtomicInteger closed = new AtomicInteger();
        FloatStream s = FloatStream.of(new float[] { 1, 2 }).onClose(closed::incrementAndGet);
        final FloatStream s1 = s;
        assertSupplierReturnedNull(closed, s1, () -> s1.collect(() -> null, (c, x) -> { }));
        s = FloatStream.of(new float[] { 1, 2 }).onClose(closed::incrementAndGet);
        final FloatStream s2 = s;
        assertSupplierReturnedNull(closed, s2, () -> s2.collect(() -> null, (c, x) -> { }, (c1, c2) -> { }));
        // empty input: the JDK and the old code returned null here
        s = FloatStream.of(new float[0]).onClose(closed::incrementAndGet);
        final FloatStream s3 = s;
        assertSupplierReturnedNull(closed, s3, () -> s3.collect(() -> null, (c, x) -> { }));
        s = FloatStream.of(FloatIterator.of(new float[] { 1, 2 })).filter(x -> false).onClose(closed::incrementAndGet);
        final FloatStream s4 = s;
        assertSupplierReturnedNull(closed, s4, () -> s4.collect(() -> null, (c, x) -> { }, (c1, c2) -> { }));
        s = FloatStream.of(FloatIterator.of(new float[] { 1, 2 })).onClose(closed::incrementAndGet);
        final FloatStream s5 = s;
        assertSupplierReturnedNull(closed, s5, () -> s5.collect(() -> null, (c, x) -> { }));
        // parallel: array (both split strategies) and iterator sources
        for (final BaseStream.SplitStrategy strategy : BaseStream.SplitStrategy.values()) {
            s = FloatStream.of(new float[100]).parallel(ps(strategy)).onClose(closed::incrementAndGet);
            final FloatStream s6 = s;
            assertSupplierReturnedNull(closed, s6, () -> s6.collect(() -> null, (c, x) -> { }, (c1, c2) -> { }));
        }
        s = FloatStream.of(FloatIterator.of(new float[100])).parallel(4).onClose(closed::incrementAndGet);
        final FloatStream s7 = s;
        assertSupplierReturnedNull(closed, s7, () -> s7.collect(() -> null, (c, x) -> { }));
        // a supplier that returns null only for some workers is still caught
        final AtomicInteger calls = new AtomicInteger();
        s = FloatStream.of(new float[100]).parallel(ps(BaseStream.SplitStrategy.ARRAY)).onClose(closed::incrementAndGet);
        final FloatStream s8 = s;
        assertSupplierReturnedNull(closed, s8,
                () -> s8.collect(() -> calls.incrementAndGet() == 2 ? null : new java.util.ArrayList<Object>(), (c, x) -> c.add(x), java.util.List::addAll));
        // a real container is unaffected
        assertEquals(100, FloatStream.of(new float[100]).parallel(4).collect(java.util.ArrayList<Object>::new, (c, x) -> c.add(x)).size());
        assertEquals(0, FloatStream.of(new float[0]).collect(java.util.ArrayList<Object>::new, (c, x) -> c.add(x)).size());
    }

    @Test
    public void c101_doubleCollectNullContainerIsNpe() {
        final AtomicInteger closed = new AtomicInteger();
        DoubleStream s = DoubleStream.of(new double[] { 1, 2 }).onClose(closed::incrementAndGet);
        final DoubleStream s1 = s;
        assertSupplierReturnedNull(closed, s1, () -> s1.collect(() -> null, (c, x) -> { }));
        s = DoubleStream.of(new double[] { 1, 2 }).onClose(closed::incrementAndGet);
        final DoubleStream s2 = s;
        assertSupplierReturnedNull(closed, s2, () -> s2.collect(() -> null, (c, x) -> { }, (c1, c2) -> { }));
        // empty input: the JDK and the old code returned null here
        s = DoubleStream.of(new double[0]).onClose(closed::incrementAndGet);
        final DoubleStream s3 = s;
        assertSupplierReturnedNull(closed, s3, () -> s3.collect(() -> null, (c, x) -> { }));
        s = DoubleStream.of(DoubleIterator.of(new double[] { 1, 2 })).filter(x -> false).onClose(closed::incrementAndGet);
        final DoubleStream s4 = s;
        assertSupplierReturnedNull(closed, s4, () -> s4.collect(() -> null, (c, x) -> { }, (c1, c2) -> { }));
        s = DoubleStream.of(DoubleIterator.of(new double[] { 1, 2 })).onClose(closed::incrementAndGet);
        final DoubleStream s5 = s;
        assertSupplierReturnedNull(closed, s5, () -> s5.collect(() -> null, (c, x) -> { }));
        // parallel: array (both split strategies) and iterator sources
        for (final BaseStream.SplitStrategy strategy : BaseStream.SplitStrategy.values()) {
            s = DoubleStream.of(new double[100]).parallel(ps(strategy)).onClose(closed::incrementAndGet);
            final DoubleStream s6 = s;
            assertSupplierReturnedNull(closed, s6, () -> s6.collect(() -> null, (c, x) -> { }, (c1, c2) -> { }));
        }
        s = DoubleStream.of(DoubleIterator.of(new double[100])).parallel(4).onClose(closed::incrementAndGet);
        final DoubleStream s7 = s;
        assertSupplierReturnedNull(closed, s7, () -> s7.collect(() -> null, (c, x) -> { }));
        // a supplier that returns null only for some workers is still caught
        final AtomicInteger calls = new AtomicInteger();
        s = DoubleStream.of(new double[100]).parallel(ps(BaseStream.SplitStrategy.ARRAY)).onClose(closed::incrementAndGet);
        final DoubleStream s8 = s;
        assertSupplierReturnedNull(closed, s8,
                () -> s8.collect(() -> calls.incrementAndGet() == 2 ? null : new java.util.ArrayList<Object>(), (c, x) -> c.add(x), java.util.List::addAll));
        // a real container is unaffected
        assertEquals(100, DoubleStream.of(new double[100]).parallel(4).collect(java.util.ArrayList<Object>::new, (c, x) -> c.add(x)).size());
        assertEquals(0, DoubleStream.of(new double[0]).collect(java.util.ArrayList<Object>::new, (c, x) -> c.add(x)).size());
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-116 (doc-only + CharBuffer bulk copy): snapshot semantics kept; the documented lazy alternatives work
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void c116_charBufferTypedAsCharSequenceIsBulkCopiedSnapshot() {
        final CharBuffer cb = CharBuffer.wrap("xabcdy".toCharArray());
        cb.position(1).limit(5); // "abcd"
        final CharSequence cs = cb;
        final CharStream all = CharStream.of(cs);
        final CharStream part = CharStream.of(cs, 1, 3);
        cb.put(2, 'Q'); // after creation: not reflected (snapshot, unlike of(CharBuffer))
        assertEquals("abcd", all.join(""));
        assertEquals("bc", part.join(""));
        assertEquals(1, cb.position());
        assertEquals(5, cb.limit());
        assertEquals("aQcd", CharStream.of(cb).join("")); // the typed overload stays a live view
        assertEquals("", CharStream.of(cs, 2, 2).join(""));
        assertThrows(IndexOutOfBoundsException.class, () -> CharStream.of(cs, 0, 5));
        // no backing array / read-only buffers take the same bulk path
        final CharSequence ro = CharBuffer.wrap("hello world").position(6);
        assertEquals("world", CharStream.of(ro).join(""));
        assertEquals("orl", CharStream.of(ro, 1, 4).join(""));
        final CharSequence roArr = CharBuffer.wrap("0123456".toCharArray(), 2, 3).asReadOnlyBuffer(); // "234"
        assertEquals("234", CharStream.of(roArr).join(""));
        final CharSequence sliced = CharBuffer.wrap("0123456".toCharArray()).position(3).slice(); // arrayOffset 3
        assertEquals("3456", CharStream.of(sliced).join(""));
        assertEquals("45", CharStream.of(sliced, 1, 3).join(""));
        final CharSequence surrogates = CharBuffer.wrap("a\uD83D\uDE00b");
        assertArrayEquals(new char[] { 'a', '\uD83D', '\uDE00', 'b' }, CharStream.of(surrogates).toArray());
    }

    @Test
    public void c116_documentedLazyAlternativesReadOnlyWhatIsNeeded() {
        final AtomicInteger reads = new AtomicInteger();
        final CharSequence virtual = new CharSequence() {
            @Override
            public int length() {
                return Integer.MAX_VALUE - 8;
            }

            @Override
            public char charAt(final int index) {
                reads.incrementAndGet();
                return (char) ('a' + index % 26);
            }

            @Override
            public CharSequence subSequence(final int start, final int end) {
                throw new UnsupportedOperationException();
            }
        };
        assertEquals("abcde", IntStream.range(0, virtual.length()).mapToChar(virtual::charAt).limit(5).join(""));
        assertEquals(5, reads.get());
        assertArrayEquals(new int[] { 'a', 'b', 'c' }, IntStream.from(virtual.codePoints()).limit(3).toArray());
        final StringBuilder sb = new StringBuilder("0123456789");
        assertEquals("345", IntStream.range(3, 6).mapToChar(sb::charAt).join(""));
    }

    // ------------------------------------------------------------------------------------------------------------
    // D6-06: ascending Char/Byte/Short ranges and single-element rangeClosed are flagged sorted like Int/Long ranges
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void d606_ascendingRangesAreFlaggedSorted() {
        CharStream c = CharStream.range('a', 'e');
        assertSame(c, c.sorted());
        c = CharStream.range('a', 'k', 3);
        assertSame(c, c.sorted());
        c = CharStream.rangeClosed('a', 'e');
        assertSame(c, c.sorted());
        c = CharStream.rangeClosed('a', 'k', 3);
        assertSame(c, c.sorted());
        c = CharStream.rangeClosed('x', 'x');
        assertSame(c, c.sorted());
        c = CharStream.rangeClosed('x', 'x', -1);
        assertSame(c, c.sorted());

        ByteStream b = ByteStream.range((byte) 1, (byte) 5);
        assertSame(b, b.sorted());
        b = ByteStream.range((byte) 1, (byte) 9, (byte) 3);
        assertSame(b, b.sorted());
        b = ByteStream.rangeClosed(Byte.MIN_VALUE, Byte.MAX_VALUE);
        assertSame(b, b.sorted());
        b = ByteStream.rangeClosed((byte) 1, (byte) 9, (byte) 3);
        assertSame(b, b.sorted());
        b = ByteStream.rangeClosed((byte) 7, (byte) 7);
        assertSame(b, b.sorted());
        b = ByteStream.rangeClosed((byte) 7, (byte) 7, (byte) 2);
        assertSame(b, b.sorted());

        ShortStream s = ShortStream.range((short) 1, (short) 5);
        assertSame(s, s.sorted());
        s = ShortStream.range((short) 1, (short) 9, (short) 3);
        assertSame(s, s.sorted());
        s = ShortStream.rangeClosed((short) 1, (short) 5);
        assertSame(s, s.sorted());
        s = ShortStream.rangeClosed((short) 1, (short) 9, (short) 3);
        assertSame(s, s.sorted());
        s = ShortStream.rangeClosed((short) 7, (short) 7);
        assertSame(s, s.sorted());
        s = ShortStream.rangeClosed((short) 7, (short) 7, (short) -2);
        assertSame(s, s.sorted());

        IntStream i = IntStream.rangeClosed(5, 5);
        assertSame(i, i.sorted());
        i = IntStream.rangeClosed(5, 5, -3);
        assertSame(i, i.sorted());
        LongStream l = LongStream.rangeClosed(5, 5, -3);
        assertSame(l, l.sorted());
        l = LongStream.rangeClosed(5, 5);
        assertSame(l, l.sorted());
    }

    @Test
    public void d606_descendingRangesStayUnflaggedAndSortCorrectly() {
        CharStream c = CharStream.range('e', 'a', -1);
        assertNotSame(c, c.sorted());
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, CharStream.range('e', 'a', -1).sorted().toArray());
        assertArrayEquals(new char[] { 'a', 'c', 'e' }, CharStream.rangeClosed('e', 'a', -2).sorted().toArray());
        assertArrayEquals(new byte[] { 1, 3, 5 }, ByteStream.rangeClosed((byte) 5, (byte) 1, (byte) -2).sorted().toArray());
        assertArrayEquals(new short[] { 2, 3, 4, 5 }, ShortStream.range((short) 5, (short) 1, (short) -1).sorted().toArray());
        assertEquals('b', CharStream.range('e', 'a', -1).min().get());
        assertEquals((short) 5, ShortStream.range((short) 5, (short) 1, (short) -1).max().get());
        assertEquals(0, CharStream.range('e', 'a').count());
        assertEquals(0, ByteStream.rangeClosed((byte) 5, (byte) 1).count());
    }

    @Test
    public void d606_sortedFastPathsOnFlaggedRangesMatchUnflaggedReference() {
        final char[] chars = CharStream.range('a', 'k').toArray();
        // reference: an unflagged array stream over the same (ascending) data
        assertArrayEquals(CharStream.of(chars).distinct().toArray(), CharStream.range('a', 'k').distinct().toArray());
        assertEquals(CharStream.of(chars).min(), CharStream.range('a', 'k').min());
        assertEquals(CharStream.of(chars).max(), CharStream.range('a', 'k').max());
        assertEquals(CharStream.of(chars).kthLargest(3), CharStream.range('a', 'k').kthLargest(3));
        assertArrayEquals(CharStream.of(chars).reverseSorted().toArray(), CharStream.range('a', 'k').reverseSorted().toArray());
        assertEquals(CharStream.of(chars).percentiles(), CharStream.range('a', 'k').percentiles());
        assertEquals(CharStream.of(chars).boxed().sorted().toList(), CharStream.range('a', 'k').boxed().sorted().toList());
        assertArrayEquals(chars, CharStream.range('a', 'k').parallel(3).sorted().toArray());
        assertArrayEquals(new char[] { 'b', 'd' }, CharStream.range('a', 'k').filter(ch -> ch == 'b' || ch == 'd').sorted().toArray());
        assertArrayEquals(new char[] { 'a', 'd', 'g', 'j' }, CharStream.range('a', 'k', 3).distinct().toArray());
        assertEquals('j', CharStream.rangeClosed('a', 'j', 3).max().get());

        final byte[] bytes = ByteStream.rangeClosed(Byte.MIN_VALUE, Byte.MAX_VALUE).toArray();
        assertEquals(256, bytes.length);
        assertEquals(256, ByteStream.rangeClosed(Byte.MIN_VALUE, Byte.MAX_VALUE).distinct().count());
        assertEquals(Byte.MIN_VALUE, ByteStream.rangeClosed(Byte.MIN_VALUE, Byte.MAX_VALUE).min().get());
        assertEquals(Byte.MAX_VALUE, ByteStream.rangeClosed(Byte.MIN_VALUE, Byte.MAX_VALUE).max().get());
        assertEquals((byte) 125, ByteStream.rangeClosed(Byte.MIN_VALUE, Byte.MAX_VALUE).kthLargest(3).get());
        assertEquals(ByteStream.of(bytes).percentiles(), ByteStream.rangeClosed(Byte.MIN_VALUE, Byte.MAX_VALUE).percentiles());
        assertArrayEquals(ByteStream.of(bytes).reverseSorted().toArray(), ByteStream.rangeClosed(Byte.MIN_VALUE, Byte.MAX_VALUE).reverseSorted().toArray());

        final short[] shorts = ShortStream.range((short) -3, (short) 7, (short) 2).toArray();
        assertArrayEquals(new short[] { -3, -1, 1, 3, 5 }, shorts);
        final ShortStream top = ShortStream.range((short) -3, (short) 7, (short) 2).top(2);
        assertArrayEquals(new short[] { 3, 5 }, top.sorted().toArray());
        assertEquals(ShortStream.of(shorts).kthLargest(2), ShortStream.range((short) -3, (short) 7, (short) 2).kthLargest(2));
        assertEquals((short) -3, ShortStream.range((short) -3, (short) 7, (short) 2).min().get());
        assertEquals(ShortStream.of(shorts).boxed().sorted().toList(), ShortStream.range((short) -3, (short) 7, (short) 2).boxed().sorted().toList());
        assertArrayEquals(new short[] { 5 }, ShortStream.rangeClosed((short) 5, (short) 5).toArray());
        assertArrayEquals(new int[] { 5 }, IntStream.rangeClosed(5, 5, 2).toArray());
        assertArrayEquals(new long[] { 5 }, LongStream.rangeClosed(5, 5, -2).toArray());
        assertArrayEquals(new char[] { 'x' }, CharStream.rangeClosed('x', 'x', 5).map(ch -> ch).sorted().toArray());
    }

    @Test
    public void n2_ofIndicesSortedFlagMatchesRangeDoc() {
        IntStream s = IntStream.ofIndices(10);
        assertSame(s, s.sorted());
        s = IntStream.ofIndices(10, 3);
        assertSame(s, s.sorted());
        s = IntStream.ofIndices(10, -3);
        assertNotSame(s, s.sorted());
        assertArrayEquals(new int[] { 0, 3, 6, 9 }, IntStream.ofIndices(10, -3).sorted().toArray());
        // the indexFunc overloads are not flagged: the index function need not be monotone
        s = IntStream.ofIndices(new int[] { 1, 0, 1 }, (a, from) -> com.landawn.abacus.util.N.indexOf(a, 1, from));
        assertNotSame(s, s.sorted());
    }

    // ------------------------------------------------------------------------------------------------------------
    // D6-04/D6-05/R9-02/D6-09: array-backed buffers (all 7 families) and the documented window/live-view contract
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void d605_shortFloatDoubleHeapBuffersUseTheArrayPath() {
        final short[] sa = { 0, 1, 2, 3, 4, 5 };
        final ShortBuffer ss = ShortBuffer.wrap(sa).position(2).slice(); // arrayOffset 2
        ss.position(1);
        final ShortStream s1 = ShortStream.of(ss);
        assertTrue(s1 instanceof ArrayShortStream);
        sa[3] = 42; // written after the call, before traversal: seen (live view)
        assertArrayEquals(new short[] { 42, 4, 5 }, s1.toArray());
        assertEquals(1, ss.position());
        assertEquals(2, ShortStream.of(ss).skip(1).count());
        final ShortStream ro = ShortStream.of(ShortBuffer.wrap(sa, 1, 3).asReadOnlyBuffer());
        assertFalse(ro instanceof ArrayShortStream);
        assertArrayEquals(new short[] { 1, 2, 42 }, ro.toArray());
        final ShortBuffer direct = ByteBuffer.allocateDirect(6).asShortBuffer().put(new short[] { 7, 8, 9 }).flip();
        assertArrayEquals(new short[] { 8, 9 }, ShortStream.of(direct.position(1)).toArray());
        assertEquals(1, direct.position());
        assertEquals(0, ShortStream.of((ShortBuffer) null).count());
        assertEquals(0, ShortStream.of(ShortBuffer.wrap(sa, 6, 0)).count());

        final float[] fa = { 0, 1, 2, 3, 4, 5 };
        final FloatBuffer fs = FloatBuffer.wrap(fa).position(2).slice();
        fs.position(1).limit(3);
        final FloatStream f1 = FloatStream.of(fs);
        assertTrue(f1 instanceof ArrayFloatStream);
        fa[4] = -1;
        assertArrayEquals(new float[] { 3, -1 }, f1.toArray());
        assertEquals(1, fs.position());
        assertEquals(3, fs.limit());
        assertFalse(FloatStream.of(FloatBuffer.wrap(fa).asReadOnlyBuffer()) instanceof ArrayFloatStream);
        assertArrayEquals(fa, FloatStream.of(FloatBuffer.wrap(fa).asReadOnlyBuffer()).toArray());

        final double[] da = { 0, 1, 2, 3, 4, 5 };
        final DoubleBuffer ds = DoubleBuffer.wrap(da, 1, 4).slice(); // arrayOffset 1, [1..4]
        final DoubleStream d1 = DoubleStream.of(ds);
        assertTrue(d1 instanceof ArrayDoubleStream);
        assertArrayEquals(new double[] { 1, 2, 3, 4 }, d1.toArray());
        assertEquals(3, DoubleStream.of(ds).skip(1).count());
        assertArrayEquals(new double[] { 2, 3 }, DoubleStream.of(ds.position(1).limit(3)).toArray());
        final DoubleBuffer ddirect = ByteBuffer.allocateDirect(16).asDoubleBuffer().put(new double[] { 1.5, 2.5 }).flip();
        assertArrayEquals(new double[] { 1.5, 2.5 }, DoubleStream.of(ddirect).toArray());
    }

    @Test
    public void d605_arrayBackedBufferStreamsNeverMutateTheBackingArray() {
        final short[] sa = { 5, 1, 4, 2, 3 };
        final short[] sCopy = sa.clone();
        final ShortBuffer sb = ShortBuffer.wrap(sa);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, ShortStream.of(sb).sorted().toArray());
        assertArrayEquals(new short[] { 5, 4, 3, 2, 1 }, ShortStream.of(sb).reverseSorted().toArray());
        assertArrayEquals(new short[] { 3, 2, 4, 1, 5 }, ShortStream.of(sb).reversed().toArray());
        assertArrayEquals(new short[] { 3, 5, 1, 4, 2 }, ShortStream.of(sb).rotated(1).toArray());
        assertEquals(5, ShortStream.of(sb).shuffled().count());
        assertArrayEquals(new short[] { 4, 5 }, ShortStream.of(sb).top(2).sorted().toArray());
        assertEquals((short) 4, ShortStream.of(sb).kthLargest(2).get());
        assertTrue(ShortStream.of(sb).percentiles().isPresent());
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, ShortStream.of(sb).parallel(2).sorted().toArray());
        assertArrayEquals(sCopy, sa);

        final float[] fa = { 5, 1, 4, 2, 3 };
        final float[] fCopy = fa.clone();
        final FloatBuffer fb = FloatBuffer.wrap(fa);
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(fb).sorted().toArray());
        assertArrayEquals(new float[] { 5, 4, 3, 2, 1 }, FloatStream.of(fb).reverseSorted().toArray());
        assertArrayEquals(new float[] { 3, 2, 4, 1, 5 }, FloatStream.of(fb).reversed().toArray());
        assertEquals(5, FloatStream.of(fb).shuffled().count());
        assertEquals(4f, FloatStream.of(fb).kthLargest(2).get());
        assertTrue(FloatStream.of(fb).percentiles().isPresent());
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, FloatStream.of(fb).parallel(2).sorted().toArray());
        assertArrayEquals(fCopy, fa);

        final double[] da = { 5, 1, 4, 2, 3 };
        final double[] dCopy = da.clone();
        final DoubleBuffer db = DoubleBuffer.wrap(da);
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStream.of(db).sorted().toArray());
        assertArrayEquals(new double[] { 5, 4, 3, 2, 1 }, DoubleStream.of(db).reverseSorted().toArray());
        assertArrayEquals(new double[] { 3, 5, 1, 4, 2 }, DoubleStream.of(db).rotated(1).toArray());
        assertEquals(5, DoubleStream.of(db).shuffled().count());
        assertArrayEquals(new double[] { 4, 5 }, DoubleStream.of(db).top(2).sorted().toArray());
        assertEquals(4d, DoubleStream.of(db).kthLargest(2).get());
        assertTrue(DoubleStream.of(db).summaryStatisticsAndPercentiles().right().isPresent());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, DoubleStream.of(db).parallel(2).sorted().toArray());
        assertArrayEquals(dCopy, da);
    }

    @Test
    public void d609_nonzeroArrayOffsetForLongCharByteBuffers() {
        final long[] la = { 0, 1, 2, 3, 4, 5 };
        final LongBuffer ls = LongBuffer.wrap(la).position(2).slice(); // arrayOffset 2
        ls.position(1).limit(3);
        assertArrayEquals(new long[] { 3, 4 }, LongStream.of(ls).toArray());
        assertEquals(1, LongStream.of(ls).skip(1).count());
        la[3] = 33;
        assertArrayEquals(new long[] { 33, 4 }, LongStream.of(ls).toArray());
        assertEquals(1, ls.position());

        final char[] ca = { 'a', 'b', 'c', 'd', 'e' };
        final CharBuffer cs = CharBuffer.wrap(ca).position(1).slice(); // arrayOffset 1
        cs.position(2);
        assertArrayEquals(new char[] { 'd', 'e' }, CharStream.of(cs).toArray());
        assertEquals(2, cs.position());
        final CharBuffer cs2 = CharBuffer.wrap(ca, 1, 3).slice(); // arrayOffset 1, [b, c, d]
        assertArrayEquals(new char[] { 'b', 'c', 'd' }, CharStream.of(cs2).toArray());

        final byte[] ba = { 9, 8, 7, 6, 5 };
        final ByteBuffer bs = ByteBuffer.wrap(ba).position(3).slice(); // arrayOffset 3
        assertArrayEquals(new byte[] { 6, 5 }, ByteStream.of(bs).toArray());
        bs.position(1);
        assertArrayEquals(new byte[] { 5 }, ByteStream.of(bs).toArray());
        assertEquals(0, ByteStream.of(bs.position(2)).count());
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-103 / D6-09 (doc locks): IntStream.summaryStatistics wraps beyond 2^32 elements; average() is exact,
    // including the positive-carry direction
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void c103_intSummaryStatisticsWrapsWhileAverageIsExact() {
        final long n = (1L << 32) + 4;
        final IntSummaryStatistics stats = IntStream.repeat(Integer.MAX_VALUE, n).summaryStatistics();
        assertEquals(n, stats.getCount());
        assertEquals(-9223372032559808516L, stats.getSum()); // wrapped, as documented
        assertEquals(-2.147483645E9, stats.getAverage(), 1.0);
        // positive carry (+1) in IteratorIntStream.average()
        assertEquals(2.147483647E9, IntStream.repeat(Integer.MAX_VALUE, n).average().getAsDouble(), 0.0);
        assertThrows(ArithmeticException.class, () -> IntStream.repeat(Integer.MAX_VALUE, n).sum());
    }

    @Test
    public void c113_intSumThrowsWhereJdkWraps() {
        assertThrows(ArithmeticException.class, () -> IntStream.of(Integer.MAX_VALUE, 1).sum());
        assertEquals(Integer.MIN_VALUE, java.util.stream.IntStream.of(Integer.MAX_VALUE, 1).sum());
        assertEquals(2147483648L, IntStream.of(Integer.MAX_VALUE, 1).asLongStream().sum());
    }

    @Test
    public void n3_summaryStatisticsAndPercentilesNaNMinMaxMatchSummaryStatistics() {
        final double[] d = { 1, Double.NaN, 2 };
        assertEquals(DoubleStream.of(d).summaryStatistics().getMin(), DoubleStream.of(d).summaryStatisticsAndPercentiles().left().getMin());
        assertTrue(Double.isNaN(DoubleStream.of(d).summaryStatisticsAndPercentiles().left().getMin()));
        assertTrue(Double.isNaN(DoubleStream.of(d).summaryStatisticsAndPercentiles().left().getMax()));
        final float[] f = { 1, Float.NaN, 2 };
        assertTrue(Float.isNaN(FloatStream.of(f).summaryStatisticsAndPercentiles().left().getMin()));
        assertTrue(Float.isNaN(FloatStream.of(f).summaryStatisticsAndPercentiles().left().getMax()));
        assertTrue(Float.isNaN(FloatStream.of(f).summaryStatistics().getMin()));
    }

    @Test
    public void c102_doubleZipCollectionNullResultThrowsNotUnboxed() {
        final List<DoubleStream> streams = Arrays.asList(DoubleStream.of(1, 2), DoubleStream.of(3, 4));
        assertThrows(NullPointerException.class, () -> DoubleStream.zip(streams, v -> null).toArray());
        final List<DoubleStream> streams2 = Arrays.asList(DoubleStream.of(1), DoubleStream.of(3, 4));
        assertThrows(NullPointerException.class, () -> DoubleStream.zip(streams2, new double[] { 0, 0 }, v -> null).toArray());
    }
}
