package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.MergeResult;

/**
 * Cycle-3 review fixes 2026-09-24 for the seven primitive stream families (fixer FX3-PRIM):
 * C-124 (of(XBuffer) window fixed for buffers without an accessible array), C-132 (balanced merge(Collection)),
 * H3-03 (transformViaJdkStream keeps the parallel settings).
 */
public class PrimitiveStreamsReview20260924cTest extends TestBase {

    private static int ceilLog2(final int k) {
        return 32 - Integer.numberOfLeadingZeros(k - 1);
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-124 of(XBuffer): the window [position, limit) is fixed at the call also for direct / read-only / view buffers
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void c124_charDirectAndReadOnlyBuffersKeepTheWindowWhenTheLimitIsLoweredLater() {
        final char[] all = new char[] { 'a', 'b', 'c', 'd' };
        for (int kind = 0; kind < 3; kind++) {
            // 0: direct, 1: read-only heap (no accessible array), 2: heap (array path, the reference behaviour)
            final CharBuffer buf;
            if (kind == 0) {
                buf = ByteBuffer.allocateDirect(8).asCharBuffer().put(new char[] { 'a', 'b', 'c', 'd' }).flip();
            } else if (kind == 1) {
                buf = CharBuffer.wrap(new char[] { 'a', 'b', 'c', 'd' }).asReadOnlyBuffer();
            } else {
                buf = CharBuffer.wrap(all.clone());
            }
            assertEquals(kind == 2, buf.hasArray());
            final CharStream s = CharStream.of(buf);
            assertEquals(0, buf.position());
            assertEquals(4, buf.limit());
            buf.limit(1);
            buf.position(1);
            assertArrayEquals(all, s.toArray(), "kind " + kind);
            assertEquals(1, buf.limit());
            assertEquals(1, buf.position());
        }
    }

    @Test
    public void c124_byteDirectAndReadOnlyBuffersKeepTheWindowWhenTheLimitIsLoweredLater() {
        final byte[] all = new byte[] { 1, 2, 3, 4 };
        for (int kind = 0; kind < 3; kind++) {
            // 0: direct, 1: read-only heap (no accessible array), 2: heap (array path, the reference behaviour)
            final ByteBuffer buf;
            if (kind == 0) {
                buf = ByteBuffer.allocateDirect(4).put(new byte[] { 1, 2, 3, 4 }).flip();
            } else if (kind == 1) {
                buf = ByteBuffer.wrap(new byte[] { 1, 2, 3, 4 }).asReadOnlyBuffer();
            } else {
                buf = ByteBuffer.wrap(all.clone());
            }
            assertEquals(kind == 2, buf.hasArray());
            final ByteStream s = ByteStream.of(buf);
            assertEquals(0, buf.position());
            assertEquals(4, buf.limit());
            buf.limit(1);
            buf.position(1);
            assertArrayEquals(all, s.toArray(), "kind " + kind);
            assertEquals(1, buf.limit());
            assertEquals(1, buf.position());
        }
    }

    @Test
    public void c124_shortDirectAndReadOnlyBuffersKeepTheWindowWhenTheLimitIsLoweredLater() {
        final short[] all = new short[] { 1, 2, 3, 4 };
        for (int kind = 0; kind < 3; kind++) {
            // 0: direct, 1: read-only heap (no accessible array), 2: heap (array path, the reference behaviour)
            final ShortBuffer buf;
            if (kind == 0) {
                buf = ByteBuffer.allocateDirect(8).asShortBuffer().put(new short[] { 1, 2, 3, 4 }).flip();
            } else if (kind == 1) {
                buf = ShortBuffer.wrap(new short[] { 1, 2, 3, 4 }).asReadOnlyBuffer();
            } else {
                buf = ShortBuffer.wrap(all.clone());
            }
            assertEquals(kind == 2, buf.hasArray());
            final ShortStream s = ShortStream.of(buf);
            assertEquals(0, buf.position());
            assertEquals(4, buf.limit());
            buf.limit(1);
            buf.position(1);
            assertArrayEquals(all, s.toArray(), "kind " + kind);
            assertEquals(1, buf.limit());
            assertEquals(1, buf.position());
        }
    }

    @Test
    public void c124_intDirectAndReadOnlyBuffersKeepTheWindowWhenTheLimitIsLoweredLater() {
        final int[] all = new int[] { 1, 2, 3, 4 };
        for (int kind = 0; kind < 3; kind++) {
            // 0: direct, 1: read-only heap (no accessible array), 2: heap (array path, the reference behaviour)
            final IntBuffer buf;
            if (kind == 0) {
                buf = ByteBuffer.allocateDirect(16).asIntBuffer().put(new int[] { 1, 2, 3, 4 }).flip();
            } else if (kind == 1) {
                buf = IntBuffer.wrap(new int[] { 1, 2, 3, 4 }).asReadOnlyBuffer();
            } else {
                buf = IntBuffer.wrap(all.clone());
            }
            assertEquals(kind == 2, buf.hasArray());
            final IntStream s = IntStream.of(buf);
            assertEquals(0, buf.position());
            assertEquals(4, buf.limit());
            buf.limit(1);
            buf.position(1);
            assertArrayEquals(all, s.toArray(), "kind " + kind);
            assertEquals(1, buf.limit());
            assertEquals(1, buf.position());
        }
    }

    @Test
    public void c124_longDirectAndReadOnlyBuffersKeepTheWindowWhenTheLimitIsLoweredLater() {
        final long[] all = new long[] { 1, 2, 3, 4 };
        for (int kind = 0; kind < 3; kind++) {
            // 0: direct, 1: read-only heap (no accessible array), 2: heap (array path, the reference behaviour)
            final LongBuffer buf;
            if (kind == 0) {
                buf = ByteBuffer.allocateDirect(32).asLongBuffer().put(new long[] { 1, 2, 3, 4 }).flip();
            } else if (kind == 1) {
                buf = LongBuffer.wrap(new long[] { 1, 2, 3, 4 }).asReadOnlyBuffer();
            } else {
                buf = LongBuffer.wrap(all.clone());
            }
            assertEquals(kind == 2, buf.hasArray());
            final LongStream s = LongStream.of(buf);
            assertEquals(0, buf.position());
            assertEquals(4, buf.limit());
            buf.limit(1);
            buf.position(1);
            assertArrayEquals(all, s.toArray(), "kind " + kind);
            assertEquals(1, buf.limit());
            assertEquals(1, buf.position());
        }
    }

    @Test
    public void c124_floatDirectAndReadOnlyBuffersKeepTheWindowWhenTheLimitIsLoweredLater() {
        final float[] all = new float[] { 1, 2, 3, 4 };
        for (int kind = 0; kind < 3; kind++) {
            // 0: direct, 1: read-only heap (no accessible array), 2: heap (array path, the reference behaviour)
            final FloatBuffer buf;
            if (kind == 0) {
                buf = ByteBuffer.allocateDirect(16).asFloatBuffer().put(new float[] { 1, 2, 3, 4 }).flip();
            } else if (kind == 1) {
                buf = FloatBuffer.wrap(new float[] { 1, 2, 3, 4 }).asReadOnlyBuffer();
            } else {
                buf = FloatBuffer.wrap(all.clone());
            }
            assertEquals(kind == 2, buf.hasArray());
            final FloatStream s = FloatStream.of(buf);
            assertEquals(0, buf.position());
            assertEquals(4, buf.limit());
            buf.limit(1);
            buf.position(1);
            assertArrayEquals(all, s.toArray(), "kind " + kind);
            assertEquals(1, buf.limit());
            assertEquals(1, buf.position());
        }
    }

    @Test
    public void c124_doubleDirectAndReadOnlyBuffersKeepTheWindowWhenTheLimitIsLoweredLater() {
        final double[] all = new double[] { 1, 2, 3, 4 };
        for (int kind = 0; kind < 3; kind++) {
            // 0: direct, 1: read-only heap (no accessible array), 2: heap (array path, the reference behaviour)
            final DoubleBuffer buf;
            if (kind == 0) {
                buf = ByteBuffer.allocateDirect(32).asDoubleBuffer().put(new double[] { 1, 2, 3, 4 }).flip();
            } else if (kind == 1) {
                buf = DoubleBuffer.wrap(new double[] { 1, 2, 3, 4 }).asReadOnlyBuffer();
            } else {
                buf = DoubleBuffer.wrap(all.clone());
            }
            assertEquals(kind == 2, buf.hasArray());
            final DoubleStream s = DoubleStream.of(buf);
            assertEquals(0, buf.position());
            assertEquals(4, buf.limit());
            buf.limit(1);
            buf.position(1);
            assertArrayEquals(all, s.toArray(), "kind " + kind);
            assertEquals(1, buf.limit());
            assertEquals(1, buf.position());
        }
    }

    @Test
    public void c124_charSequenceBackedCharBufferKeepsTheWindow() {
        final CharBuffer buf = CharBuffer.wrap("abcdef");
        buf.position(1).limit(5);
        assertFalse(buf.hasArray());
        final CharStream s = CharStream.of(buf);
        buf.limit(2);
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, s.toArray());
    }

    @Test
    public void c124_directBufferReuseCycleKeepsWindowButContentStaysLive() {
        // The usual NIO reuse cycle after of(): clear(), put, flip() - the limit drops to 1 before traversal.
        final IntBuffer buf = ByteBuffer.allocateDirect(16).asIntBuffer();
        buf.put(1).put(2).put(3).flip();
        final IntStream s = IntStream.of(buf);
        buf.clear();
        buf.put(7);
        buf.flip();
        // window [0, 3) fixed at the call; the write at index 0 is seen (live view)
        assertArrayEquals(new int[] { 7, 2, 3 }, s.toArray());
    }

    @Test
    public void c124_raisingTheLimitLaterDoesNotWidenTheWindow() {
        final LongBuffer buf = ByteBuffer.allocateDirect(32).asLongBuffer();
        buf.put(new long[] { 1, 2, 3, 4 });
        buf.position(1).limit(3);
        final LongStream s = LongStream.of(buf);
        buf.clear();
        assertArrayEquals(new long[] { 2, 3 }, s.toArray());
    }

    @Test
    public void c124_limitLoweredDuringTraversal() {
        final DoubleBuffer buf = ByteBuffer.allocateDirect(32).asDoubleBuffer();
        buf.put(new double[] { 1.5, 2.5, 3.5, 4.5 }).flip();
        final DoubleIterator it = DoubleStream.of(buf).iterator();
        assertEquals(1.5, it.nextDouble());
        buf.limit(0);
        assertEquals(2.5, it.nextDouble());
        assertEquals(3.5, it.nextDouble());
        assertEquals(4.5, it.nextDouble());
        assertFalse(it.hasNext());
    }

    @Test
    public void c124_emptyAndNullBuffers() {
        assertEquals(0, IntStream.of(ByteBuffer.allocateDirect(0).asIntBuffer()).count());
        final ShortBuffer buf = ByteBuffer.allocateDirect(8).asShortBuffer();
        buf.position(4);
        final ShortStream s = ShortStream.of(buf);
        buf.position(0);
        assertEquals(0, s.count());
        assertEquals(0, FloatStream.of((FloatBuffer) null).count());
        assertEquals(0, ByteStream.of((ByteBuffer) null).count());
    }

    @Test
    public void c124_unicodeDirectCharBuffer() {
        final String text = "a\uD83D\uDE00\u00E9z";
        final CharBuffer buf = ByteBuffer.allocateDirect(text.length() * 2).asCharBuffer().put(text).flip();
        final CharStream s = CharStream.of(buf);
        buf.limit(1);
        assertEquals(text, new String(s.toArray()));
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-132 merge(Collection, selector): balanced tree - no StackOverflowError, O(n log k) selector calls, all closed
    // ------------------------------------------------------------------------------------------------------------

    private static void c132Char(final int k, final boolean countOnly) {
        final AtomicInteger closed = new AtomicInteger();
        final AtomicLong calls = new AtomicLong();
        final List<CharStream> sources = new ArrayList<>(k);
        final char[] expected = new char[k];
        for (int i = 0; i < k; i++) {
            expected[i] = (char) i;
            sources.add(CharStream.of(expected[i]).onClose(closed::incrementAndGet));
        }
        final CharStream merged = CharStream.merge(sources, (x, y) -> {
            calls.incrementAndGet();
            return x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        });
        if (countOnly) {
            assertEquals(k, merged.count());
        } else {
            assertArrayEquals(expected, merged.toArray());
        }
        assertEquals(k, closed.get(), "every source closed exactly once");
        assertTrue(calls.get() <= (long) k * ceilLog2(k), "selector calls: " + calls.get());
    }

    @Test
    public void c132_charMergeOf2000Sources() {
        c132Char(2000, false);
    }

    @Test
    public void c132_charMergeOf20000Sources() {
        c132Char(20000, true);
    }

    private static void c132Byte(final int k, final boolean countOnly) {
        final AtomicInteger closed = new AtomicInteger();
        final AtomicLong calls = new AtomicLong();
        final List<ByteStream> sources = new ArrayList<>(k);
        final byte[] expected = new byte[k];
        for (int i = 0; i < k; i++) {
            expected[i] = (byte) (-128 + (long) i * 256 / k);
            sources.add(ByteStream.of(expected[i]).onClose(closed::incrementAndGet));
        }
        final ByteStream merged = ByteStream.merge(sources, (x, y) -> {
            calls.incrementAndGet();
            return x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        });
        if (countOnly) {
            assertEquals(k, merged.count());
        } else {
            assertArrayEquals(expected, merged.toArray());
        }
        assertEquals(k, closed.get(), "every source closed exactly once");
        assertTrue(calls.get() <= (long) k * ceilLog2(k), "selector calls: " + calls.get());
    }

    @Test
    public void c132_byteMergeOf2000Sources() {
        c132Byte(2000, false);
    }

    @Test
    public void c132_byteMergeOf20000Sources() {
        c132Byte(20000, true);
    }

    private static void c132Short(final int k, final boolean countOnly) {
        final AtomicInteger closed = new AtomicInteger();
        final AtomicLong calls = new AtomicLong();
        final List<ShortStream> sources = new ArrayList<>(k);
        final short[] expected = new short[k];
        for (int i = 0; i < k; i++) {
            expected[i] = (short) (i - 10000);
            sources.add(ShortStream.of(expected[i]).onClose(closed::incrementAndGet));
        }
        final ShortStream merged = ShortStream.merge(sources, (x, y) -> {
            calls.incrementAndGet();
            return x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        });
        if (countOnly) {
            assertEquals(k, merged.count());
        } else {
            assertArrayEquals(expected, merged.toArray());
        }
        assertEquals(k, closed.get(), "every source closed exactly once");
        assertTrue(calls.get() <= (long) k * ceilLog2(k), "selector calls: " + calls.get());
    }

    @Test
    public void c132_shortMergeOf2000Sources() {
        c132Short(2000, false);
    }

    @Test
    public void c132_shortMergeOf20000Sources() {
        c132Short(20000, true);
    }

    private static void c132Int(final int k, final boolean countOnly) {
        final AtomicInteger closed = new AtomicInteger();
        final AtomicLong calls = new AtomicLong();
        final List<IntStream> sources = new ArrayList<>(k);
        final int[] expected = new int[k];
        for (int i = 0; i < k; i++) {
            expected[i] = i;
            sources.add(IntStream.of(expected[i]).onClose(closed::incrementAndGet));
        }
        final IntStream merged = IntStream.merge(sources, (x, y) -> {
            calls.incrementAndGet();
            return x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        });
        if (countOnly) {
            assertEquals(k, merged.count());
        } else {
            assertArrayEquals(expected, merged.toArray());
        }
        assertEquals(k, closed.get(), "every source closed exactly once");
        assertTrue(calls.get() <= (long) k * ceilLog2(k), "selector calls: " + calls.get());
    }

    @Test
    public void c132_intMergeOf2000Sources() {
        c132Int(2000, false);
    }

    @Test
    public void c132_intMergeOf20000Sources() {
        c132Int(20000, true);
    }

    private static void c132Long(final int k, final boolean countOnly) {
        final AtomicInteger closed = new AtomicInteger();
        final AtomicLong calls = new AtomicLong();
        final List<LongStream> sources = new ArrayList<>(k);
        final long[] expected = new long[k];
        for (int i = 0; i < k; i++) {
            expected[i] = i;
            sources.add(LongStream.of(expected[i]).onClose(closed::incrementAndGet));
        }
        final LongStream merged = LongStream.merge(sources, (x, y) -> {
            calls.incrementAndGet();
            return x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        });
        if (countOnly) {
            assertEquals(k, merged.count());
        } else {
            assertArrayEquals(expected, merged.toArray());
        }
        assertEquals(k, closed.get(), "every source closed exactly once");
        assertTrue(calls.get() <= (long) k * ceilLog2(k), "selector calls: " + calls.get());
    }

    @Test
    public void c132_longMergeOf2000Sources() {
        c132Long(2000, false);
    }

    @Test
    public void c132_longMergeOf20000Sources() {
        c132Long(20000, true);
    }

    private static void c132Float(final int k, final boolean countOnly) {
        final AtomicInteger closed = new AtomicInteger();
        final AtomicLong calls = new AtomicLong();
        final List<FloatStream> sources = new ArrayList<>(k);
        final float[] expected = new float[k];
        for (int i = 0; i < k; i++) {
            expected[i] = i;
            sources.add(FloatStream.of(expected[i]).onClose(closed::incrementAndGet));
        }
        final FloatStream merged = FloatStream.merge(sources, (x, y) -> {
            calls.incrementAndGet();
            return x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        });
        if (countOnly) {
            assertEquals(k, merged.count());
        } else {
            assertArrayEquals(expected, merged.toArray());
        }
        assertEquals(k, closed.get(), "every source closed exactly once");
        assertTrue(calls.get() <= (long) k * ceilLog2(k), "selector calls: " + calls.get());
    }

    @Test
    public void c132_floatMergeOf2000Sources() {
        c132Float(2000, false);
    }

    @Test
    public void c132_floatMergeOf20000Sources() {
        c132Float(20000, true);
    }

    private static void c132Double(final int k, final boolean countOnly) {
        final AtomicInteger closed = new AtomicInteger();
        final AtomicLong calls = new AtomicLong();
        final List<DoubleStream> sources = new ArrayList<>(k);
        final double[] expected = new double[k];
        for (int i = 0; i < k; i++) {
            expected[i] = i;
            sources.add(DoubleStream.of(expected[i]).onClose(closed::incrementAndGet));
        }
        final DoubleStream merged = DoubleStream.merge(sources, (x, y) -> {
            calls.incrementAndGet();
            return x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        });
        if (countOnly) {
            assertEquals(k, merged.count());
        } else {
            assertArrayEquals(expected, merged.toArray());
        }
        assertEquals(k, closed.get(), "every source closed exactly once");
        assertTrue(calls.get() <= (long) k * ceilLog2(k), "selector calls: " + calls.get());
    }

    @Test
    public void c132_doubleMergeOf2000Sources() {
        c132Double(2000, false);
    }

    @Test
    public void c132_doubleMergeOf20000Sources() {
        c132Double(20000, true);
    }

    @Test
    public void c132_selectorCallCountIsNLogK() {
        final int k = 1024;
        final int m = 4;
        final List<IntStream> sources = new ArrayList<>(k);
        for (int j = 0; j < k; j++) {
            sources.add(IntStream.of(j, j + k, j + 2 * k, j + 3 * k));
        }
        final AtomicLong calls = new AtomicLong();
        final int[] result = IntStream.merge(sources, (x, y) -> {
            calls.incrementAndGet();
            return x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        }).toArray();
        assertArrayEquals(IntStream.range(0, k * m).toArray(), result);
        // balanced: each element passes through at most log2(1024) = 10 selectors (left fold: about n * k / 2 = 2M calls)
        assertTrue(calls.get() <= (long) k * m * 10, "selector calls: " + calls.get());
    }

    private static int[] sortedRandom(final Random rnd, final int maxLen) {
        final int[] a = new int[rnd.nextInt(maxLen + 1)];
        for (int i = 0; i < a.length; i++) {
            a[i] = rnd.nextInt(5) - 2;
        }
        Arrays.sort(a);
        return a;
    }

    @Test
    public void c132_sameResultAsTheLeftFoldForTakeFirstOnTies() {
        final Random rnd = new Random(20260924);
        for (int round = 0; round < 300; round++) {
            final int k = 3 + rnd.nextInt(15);
            final int[][] data = new int[k][];
            for (int j = 0; j < k; j++) {
                data[j] = sortedRandom(rnd, 6);
            }

            // Double: -0.0 and 0.0 compare equal under <=, so the raw bits show which source each zero came from.
            final List<DoubleStream> ds = new ArrayList<>();
            final List<DoubleStream> dsFold = new ArrayList<>();
            final List<IntStream> is = new ArrayList<>();
            final List<IntStream> isFold = new ArrayList<>();
            for (int j = 0; j < k; j++) {
                final double[] d = new double[data[j].length];
                for (int i = 0; i < d.length; i++) {
                    d[i] = data[j][i] == 0 ? (j % 2 == 0 ? -0.0 : 0.0) : data[j][i];
                }
                ds.add(DoubleStream.of(d));
                dsFold.add(DoubleStream.of(d));
                is.add(IntStream.of(data[j]));
                isFold.add(IntStream.of(data[j]));
            }

            IntStream intFold = IntStream.merge(isFold.get(0), isFold.get(1), (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
            DoubleStream doubleFold = DoubleStream.merge(dsFold.get(0), dsFold.get(1), (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
            for (int j = 2; j < k; j++) {
                intFold = IntStream.merge(intFold, isFold.get(j), (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
                doubleFold = DoubleStream.merge(doubleFold, dsFold.get(j), (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
            }

            assertArrayEquals(intFold.toArray(), IntStream.merge(is, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray());
            final long[] expectedBits = DoubleStream.of(doubleFold.toArray()).mapToLong(Double::doubleToRawLongBits).toArray();
            final long[] actualBits = DoubleStream.of(DoubleStream.merge(ds, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray())
                    .mapToLong(Double::doubleToRawLongBits)
                    .toArray();
            assertArrayEquals(expectedBits, actualBits, "round " + round);
        }
    }

    @Test
    public void c132_alreadyClosedSourceClosesOnlyThePrecedingStreams() {
        for (final int k : new int[] { 3, 4, 7, 8, 9 }) {
            for (int bad = 0; bad < k; bad++) {
                final AtomicInteger[] closed = new AtomicInteger[k];
                final List<IntStream> sources = new ArrayList<>(k);
                for (int j = 0; j < k; j++) {
                    closed[j] = new AtomicInteger();
                    sources.add(IntStream.of(j).onClose(closed[j]::incrementAndGet));
                }
                sources.get(bad).close();
                closed[bad].set(0);

                assertThrows(IllegalStateException.class, () -> IntStream.merge(sources, (x, y) -> MergeResult.TAKE_FIRST), "k=" + k + " bad=" + bad);

                for (int j = 0; j < k; j++) {
                    if (j < bad) {
                        assertEquals(1, closed[j].get(), "k=" + k + " bad=" + bad + " j=" + j);
                    } else if (j > bad) {
                        assertEquals(0, closed[j].get(), "k=" + k + " bad=" + bad + " j=" + j);
                        assertEquals(1, sources.get(j).count(), "an untouched later stream stays usable");
                    }
                }
            }
        }
    }

    @Test
    public void c132_nullElementsAreEmptyAndClosingTheResultClosesEverySource() {
        final AtomicInteger closed = new AtomicInteger();
        final List<LongStream> sources = new ArrayList<>();
        for (int j = 0; j < 9; j++) {
            sources.add(j % 3 == 0 ? null : LongStream.of(j, j + 10L).onClose(closed::incrementAndGet));
        }
        final LongStream merged = LongStream.merge(sources, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(0, closed.get());
        merged.close();
        assertEquals(6, closed.get());

        assertArrayEquals(new long[] { 1, 2, 4, 5, 7, 8, 11, 12, 14, 15, 17, 18 },
                LongStream.merge(Arrays.asList(null, LongStream.of(1, 11), LongStream.of(2, 12), null, LongStream.of(4, 14), LongStream.of(5, 15), null,
                        LongStream.of(7, 17), LongStream.of(8, 18)), (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray());
        assertEquals(0, CharStream.merge(Collections.<CharStream> emptyList(), (x, y) -> MergeResult.TAKE_FIRST).count());
    }

    @Test
    public void c132_javadocExamples() {
        assertEquals("abcdefghi",
                new String(CharStream.merge(Arrays.asList(CharStream.of("adg"), CharStream.of("beh"), CharStream.of("cfi")),
                        (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray()));
        assertEquals("789456123",
                new String(CharStream.merge(Arrays.asList(CharStream.of("123"), CharStream.of("456"), CharStream.of("789")),
                        (x, y) -> x > y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray()));
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 },
                IntStream.merge(Arrays.asList(IntStream.of(1, 5, 9), IntStream.of(2, 6), IntStream.of(3, 7), IntStream.of(4, 8)),
                        (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray());
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 },
                ShortStream.merge(Arrays.asList(ShortStream.of((short) 1, (short) 5, (short) 9), ShortStream.of((short) 2, (short) 6, (short) 10),
                        ShortStream.of((short) 3, (short) 7, (short) 11), ShortStream.of((short) 4, (short) 8, (short) 12)),
                        (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray());
    }

    // ------------------------------------------------------------------------------------------------------------
    // H1-02 sibling (doc lock): concat closes each exhausted input from hasNext(); a close failure ends the traversal
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void h102_concatClosesEachExhaustedInputAndACloseFailureEndsTheTraversal() {
        for (int overload = 0; overload < 2; overload++) {
            final AtomicInteger aClosed = new AtomicInteger();
            final IntStream a = IntStream.of(1, 2).onClose(aClosed::incrementAndGet);
            final IntStream b = IntStream.of(3, 4);
            final IntStream s = overload == 0 ? IntStream.concat(a, b) : IntStream.concat(Arrays.asList(a, b));
            final com.landawn.abacus.util.IntIterator it = s.iterator();
            assertEquals(1, it.nextInt());
            assertEquals(2, it.nextInt());
            assertEquals(0, aClosed.get());
            assertTrue(it.hasNext()); // moving on to b closes the exhausted a
            assertEquals(1, aClosed.get());
            assertEquals(3, it.nextInt());
            s.close();
            assertEquals(1, aClosed.get());
        }

        final List<Character> seen = new ArrayList<>();
        final CharStream failing = CharStream.of('a').onClose(() -> {
            throw new IllegalStateException("close failed");
        });
        final CharStream concat = CharStream.concat(failing, CharStream.of('b', 'c'));
        final IllegalStateException e = assertThrows(IllegalStateException.class, () -> concat.forEach(seen::add));
        assertEquals("close failed", e.getMessage());
        assertEquals(Arrays.asList('a'), seen); // the elements of the remaining input are not delivered
    }

    // ------------------------------------------------------------------------------------------------------------
    // H3-03 transformViaJdkStream keeps this stream's parallel settings when the JDK pipeline stays parallel
    // ------------------------------------------------------------------------------------------------------------

    private static BaseStream.ParallelSettings ps(final ExecutorService exec) {
        return BaseStream.ParallelSettings.builder().maxThreadNum(3).splitStrategy(BaseStream.SplitStrategy.ARRAY).executor(exec).build();
    }

    private static void assertSettings(final StreamBase<?, ?, ?, ?, ?, ?, ?, ?> expected, final StreamBase<?, ?, ?, ?, ?, ?, ?, ?> actual) {
        assertTrue(actual.isParallel());
        assertEquals(3, actual.maxThreadNum());
        assertEquals(BaseStream.SplitStrategy.ARRAY, actual.splitStrategy());
        assertSame(expected.asyncExecutor(), actual.asyncExecutor());
        assertTrue(actual.cancelUncompletedThreads());
    }

    @Test
    public void h303_intTransformViaJdkStreamKeepsTheParallelSettings() {
        final ExecutorService exec = Executors.newFixedThreadPool(3);
        try {
            final AtomicInteger closed = new AtomicInteger();
            final IntStream p0 = IntStream.of(1, 2, 3, 4, 5, 6, 7, 8).parallel(ps(exec));
            // the cancel flag is only reachable through the internal 4-arg parallel(...)
            final IntStream src = p0.parallel(3, p0.splitStrategy(), p0.asyncExecutor(), true).onClose(closed::incrementAndGet);
            assertSettings(src, src);

            final IntStream r = src.transformViaJdkStream(s -> s.map(x -> x * 2));
            assertSettings(src, r);
            assertEquals(72L, (long) r.sum());
            assertEquals(1, closed.get());

            // a JDK pipeline turned sequential stays sequential
            final IntStream r2 = IntStream.of(1, 2, 3, 4, 5, 6, 7, 8).parallel(ps(exec)).transformViaJdkStream(s -> s.sequential().map(x -> x * 2));
            assertFalse(r2.isParallel());
            assertEquals(72L, (long) r2.sum());

            // a sequential receiver whose JDK pipeline turns parallel gets the default settings (nothing to carry over)
            final IntStream r3 = IntStream.of(1, 2, 3, 4, 5, 6, 7, 8).transformViaJdkStream(s -> s.parallel().map(x -> x * 2));
            assertTrue(r3.isParallel());
            assertEquals(StreamBase.DEFAULT_MAX_THREAD_NUM, r3.maxThreadNum());
            assertEquals(72L, (long) r3.sum());

            // a failing transfer still closes the receiver
            final AtomicInteger closed2 = new AtomicInteger();
            final IntStream src2 = IntStream.of(1, 2, 3, 4, 5, 6, 7, 8).parallel(ps(exec)).onClose(closed2::incrementAndGet);
            assertThrows(IllegalStateException.class, () -> src2.transformViaJdkStream(s -> {
                throw new IllegalStateException("boom");
            }));
            assertEquals(1, closed2.get());
        } finally {
            exec.shutdownNow();
        }
    }

    @Test
    public void h303_longTransformViaJdkStreamKeepsTheParallelSettings() {
        final ExecutorService exec = Executors.newFixedThreadPool(3);
        try {
            final AtomicInteger closed = new AtomicInteger();
            final LongStream p0 = LongStream.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L).parallel(ps(exec));
            // the cancel flag is only reachable through the internal 4-arg parallel(...)
            final LongStream src = p0.parallel(3, p0.splitStrategy(), p0.asyncExecutor(), true).onClose(closed::incrementAndGet);
            assertSettings(src, src);

            final LongStream r = src.transformViaJdkStream(s -> s.map(x -> x * 2));
            assertSettings(src, r);
            assertEquals(72L, (long) r.sum());
            assertEquals(1, closed.get());

            // a JDK pipeline turned sequential stays sequential
            final LongStream r2 = LongStream.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L).parallel(ps(exec)).transformViaJdkStream(s -> s.sequential().map(x -> x * 2));
            assertFalse(r2.isParallel());
            assertEquals(72L, (long) r2.sum());

            // a sequential receiver whose JDK pipeline turns parallel gets the default settings (nothing to carry over)
            final LongStream r3 = LongStream.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L).transformViaJdkStream(s -> s.parallel().map(x -> x * 2));
            assertTrue(r3.isParallel());
            assertEquals(StreamBase.DEFAULT_MAX_THREAD_NUM, r3.maxThreadNum());
            assertEquals(72L, (long) r3.sum());

            // a failing transfer still closes the receiver
            final AtomicInteger closed2 = new AtomicInteger();
            final LongStream src2 = LongStream.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L).parallel(ps(exec)).onClose(closed2::incrementAndGet);
            assertThrows(IllegalStateException.class, () -> src2.transformViaJdkStream(s -> {
                throw new IllegalStateException("boom");
            }));
            assertEquals(1, closed2.get());
        } finally {
            exec.shutdownNow();
        }
    }

    @Test
    public void h303_doubleTransformViaJdkStreamKeepsTheParallelSettings() {
        final ExecutorService exec = Executors.newFixedThreadPool(3);
        try {
            final AtomicInteger closed = new AtomicInteger();
            final DoubleStream p0 = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0).parallel(ps(exec));
            // the cancel flag is only reachable through the internal 4-arg parallel(...)
            final DoubleStream src = p0.parallel(3, p0.splitStrategy(), p0.asyncExecutor(), true).onClose(closed::incrementAndGet);
            assertSettings(src, src);

            final DoubleStream r = src.transformViaJdkStream(s -> s.map(x -> x * 2));
            assertSettings(src, r);
            assertEquals(72L, (long) r.sum());
            assertEquals(1, closed.get());

            // a JDK pipeline turned sequential stays sequential
            final DoubleStream r2 = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0).parallel(ps(exec)).transformViaJdkStream(s -> s.sequential().map(x -> x * 2));
            assertFalse(r2.isParallel());
            assertEquals(72L, (long) r2.sum());

            // a sequential receiver whose JDK pipeline turns parallel gets the default settings (nothing to carry over)
            final DoubleStream r3 = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0).transformViaJdkStream(s -> s.parallel().map(x -> x * 2));
            assertTrue(r3.isParallel());
            assertEquals(StreamBase.DEFAULT_MAX_THREAD_NUM, r3.maxThreadNum());
            assertEquals(72L, (long) r3.sum());

            // a failing transfer still closes the receiver
            final AtomicInteger closed2 = new AtomicInteger();
            final DoubleStream src2 = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0).parallel(ps(exec)).onClose(closed2::incrementAndGet);
            assertThrows(IllegalStateException.class, () -> src2.transformViaJdkStream(s -> {
                throw new IllegalStateException("boom");
            }));
            assertEquals(1, closed2.get());
        } finally {
            exec.shutdownNow();
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-133 parallel skip(n, action): runs on the sequential view, keeps encounter order and the parallel settings
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void c133_charParallelSkipWithActionKeepsEncounterOrder() {
        final int size = 20000;
        final char[] a = new char[size];
        for (int i = 0; i < size; i++) {
            a[i] = (char) i;
        }
        final char[] expectedRest = Arrays.copyOfRange(a, 3, size);
        for (int round = 0; round < 50; round++) {
            // even rounds: ParallelArrayCharStream; odd rounds: ParallelIteratorCharStream
            final CharStream src = (round % 2 == 0 ? CharStream.of(a) : CharStream.of(com.landawn.abacus.util.CharIterator.of(a))).parallel(4);
            final List<Object> skipped = Collections.synchronizedList(new ArrayList<>());
            final CharStream r = src.skip(3, skipped::add);
            assertTrue(r.isParallel());
            assertEquals(4, r.maxThreadNum());
            assertEquals(src.splitStrategy(), r.splitStrategy());
            assertSame(src.asyncExecutor(), r.asyncExecutor());
            assertArrayEquals(expectedRest, r.toArray(), "round " + round);
            assertEquals(Arrays.asList(a[0], a[1], a[2]), skipped);
        }

        final AtomicInteger calls = new AtomicInteger();
        assertEquals(0, CharStream.of(a).parallel(4).skip(size + 5, x -> calls.incrementAndGet()).count());
        assertEquals(size, calls.get());
        assertEquals(size, CharStream.of(a).parallel(4).skip(0, x -> calls.incrementAndGet()).count());
        assertEquals(size, calls.get());
    }

    @Test
    public void c133_byteParallelSkipWithActionKeepsEncounterOrder() {
        final int size = 20000;
        final byte[] a = new byte[size];
        for (int i = 0; i < size; i++) {
            a[i] = (byte) i;
        }
        final byte[] expectedRest = Arrays.copyOfRange(a, 3, size);
        for (int round = 0; round < 50; round++) {
            // even rounds: ParallelArrayByteStream; odd rounds: ParallelIteratorByteStream
            final ByteStream src = (round % 2 == 0 ? ByteStream.of(a) : ByteStream.of(com.landawn.abacus.util.ByteIterator.of(a))).parallel(4);
            final List<Object> skipped = Collections.synchronizedList(new ArrayList<>());
            final ByteStream r = src.skip(3, skipped::add);
            assertTrue(r.isParallel());
            assertEquals(4, r.maxThreadNum());
            assertEquals(src.splitStrategy(), r.splitStrategy());
            assertSame(src.asyncExecutor(), r.asyncExecutor());
            assertArrayEquals(expectedRest, r.toArray(), "round " + round);
            assertEquals(Arrays.asList(a[0], a[1], a[2]), skipped);
        }

        final AtomicInteger calls = new AtomicInteger();
        assertEquals(0, ByteStream.of(a).parallel(4).skip(size + 5, x -> calls.incrementAndGet()).count());
        assertEquals(size, calls.get());
        assertEquals(size, ByteStream.of(a).parallel(4).skip(0, x -> calls.incrementAndGet()).count());
        assertEquals(size, calls.get());
    }

    @Test
    public void c133_shortParallelSkipWithActionKeepsEncounterOrder() {
        final int size = 20000;
        final short[] a = new short[size];
        for (int i = 0; i < size; i++) {
            a[i] = (short) i;
        }
        final short[] expectedRest = Arrays.copyOfRange(a, 3, size);
        for (int round = 0; round < 50; round++) {
            // even rounds: ParallelArrayShortStream; odd rounds: ParallelIteratorShortStream
            final ShortStream src = (round % 2 == 0 ? ShortStream.of(a) : ShortStream.of(com.landawn.abacus.util.ShortIterator.of(a))).parallel(4);
            final List<Object> skipped = Collections.synchronizedList(new ArrayList<>());
            final ShortStream r = src.skip(3, skipped::add);
            assertTrue(r.isParallel());
            assertEquals(4, r.maxThreadNum());
            assertEquals(src.splitStrategy(), r.splitStrategy());
            assertSame(src.asyncExecutor(), r.asyncExecutor());
            assertArrayEquals(expectedRest, r.toArray(), "round " + round);
            assertEquals(Arrays.asList(a[0], a[1], a[2]), skipped);
        }

        final AtomicInteger calls = new AtomicInteger();
        assertEquals(0, ShortStream.of(a).parallel(4).skip(size + 5, x -> calls.incrementAndGet()).count());
        assertEquals(size, calls.get());
        assertEquals(size, ShortStream.of(a).parallel(4).skip(0, x -> calls.incrementAndGet()).count());
        assertEquals(size, calls.get());
    }

    @Test
    public void c133_intParallelSkipWithActionKeepsEncounterOrder() {
        final int size = 20000;
        final int[] a = new int[size];
        for (int i = 0; i < size; i++) {
            a[i] = i;
        }
        final int[] expectedRest = Arrays.copyOfRange(a, 3, size);
        for (int round = 0; round < 50; round++) {
            // even rounds: ParallelArrayIntStream; odd rounds: ParallelIteratorIntStream
            final IntStream src = (round % 2 == 0 ? IntStream.of(a) : IntStream.of(com.landawn.abacus.util.IntIterator.of(a))).parallel(4);
            final List<Object> skipped = Collections.synchronizedList(new ArrayList<>());
            final IntStream r = src.skip(3, skipped::add);
            assertTrue(r.isParallel());
            assertEquals(4, r.maxThreadNum());
            assertEquals(src.splitStrategy(), r.splitStrategy());
            assertSame(src.asyncExecutor(), r.asyncExecutor());
            assertArrayEquals(expectedRest, r.toArray(), "round " + round);
            assertEquals(Arrays.asList(a[0], a[1], a[2]), skipped);
        }

        final AtomicInteger calls = new AtomicInteger();
        assertEquals(0, IntStream.of(a).parallel(4).skip(size + 5, x -> calls.incrementAndGet()).count());
        assertEquals(size, calls.get());
        assertEquals(size, IntStream.of(a).parallel(4).skip(0, x -> calls.incrementAndGet()).count());
        assertEquals(size, calls.get());
    }

    @Test
    public void c133_longParallelSkipWithActionKeepsEncounterOrder() {
        final int size = 20000;
        final long[] a = new long[size];
        for (int i = 0; i < size; i++) {
            a[i] = i;
        }
        final long[] expectedRest = Arrays.copyOfRange(a, 3, size);
        for (int round = 0; round < 50; round++) {
            // even rounds: ParallelArrayLongStream; odd rounds: ParallelIteratorLongStream
            final LongStream src = (round % 2 == 0 ? LongStream.of(a) : LongStream.of(com.landawn.abacus.util.LongIterator.of(a))).parallel(4);
            final List<Object> skipped = Collections.synchronizedList(new ArrayList<>());
            final LongStream r = src.skip(3, skipped::add);
            assertTrue(r.isParallel());
            assertEquals(4, r.maxThreadNum());
            assertEquals(src.splitStrategy(), r.splitStrategy());
            assertSame(src.asyncExecutor(), r.asyncExecutor());
            assertArrayEquals(expectedRest, r.toArray(), "round " + round);
            assertEquals(Arrays.asList(a[0], a[1], a[2]), skipped);
        }

        final AtomicInteger calls = new AtomicInteger();
        assertEquals(0, LongStream.of(a).parallel(4).skip(size + 5, x -> calls.incrementAndGet()).count());
        assertEquals(size, calls.get());
        assertEquals(size, LongStream.of(a).parallel(4).skip(0, x -> calls.incrementAndGet()).count());
        assertEquals(size, calls.get());
    }

    @Test
    public void c133_floatParallelSkipWithActionKeepsEncounterOrder() {
        final int size = 20000;
        final float[] a = new float[size];
        for (int i = 0; i < size; i++) {
            a[i] = i;
        }
        final float[] expectedRest = Arrays.copyOfRange(a, 3, size);
        for (int round = 0; round < 50; round++) {
            // even rounds: ParallelArrayFloatStream; odd rounds: ParallelIteratorFloatStream
            final FloatStream src = (round % 2 == 0 ? FloatStream.of(a) : FloatStream.of(com.landawn.abacus.util.FloatIterator.of(a))).parallel(4);
            final List<Object> skipped = Collections.synchronizedList(new ArrayList<>());
            final FloatStream r = src.skip(3, skipped::add);
            assertTrue(r.isParallel());
            assertEquals(4, r.maxThreadNum());
            assertEquals(src.splitStrategy(), r.splitStrategy());
            assertSame(src.asyncExecutor(), r.asyncExecutor());
            assertArrayEquals(expectedRest, r.toArray(), "round " + round);
            assertEquals(Arrays.asList(a[0], a[1], a[2]), skipped);
        }

        final AtomicInteger calls = new AtomicInteger();
        assertEquals(0, FloatStream.of(a).parallel(4).skip(size + 5, x -> calls.incrementAndGet()).count());
        assertEquals(size, calls.get());
        assertEquals(size, FloatStream.of(a).parallel(4).skip(0, x -> calls.incrementAndGet()).count());
        assertEquals(size, calls.get());
    }

    @Test
    public void c133_doubleParallelSkipWithActionKeepsEncounterOrder() {
        final int size = 20000;
        final double[] a = new double[size];
        for (int i = 0; i < size; i++) {
            a[i] = i;
        }
        final double[] expectedRest = Arrays.copyOfRange(a, 3, size);
        for (int round = 0; round < 50; round++) {
            // even rounds: ParallelArrayDoubleStream; odd rounds: ParallelIteratorDoubleStream
            final DoubleStream src = (round % 2 == 0 ? DoubleStream.of(a) : DoubleStream.of(com.landawn.abacus.util.DoubleIterator.of(a))).parallel(4);
            final List<Object> skipped = Collections.synchronizedList(new ArrayList<>());
            final DoubleStream r = src.skip(3, skipped::add);
            assertTrue(r.isParallel());
            assertEquals(4, r.maxThreadNum());
            assertEquals(src.splitStrategy(), r.splitStrategy());
            assertSame(src.asyncExecutor(), r.asyncExecutor());
            assertArrayEquals(expectedRest, r.toArray(), "round " + round);
            assertEquals(Arrays.asList(a[0], a[1], a[2]), skipped);
        }

        final AtomicInteger calls = new AtomicInteger();
        assertEquals(0, DoubleStream.of(a).parallel(4).skip(size + 5, x -> calls.incrementAndGet()).count());
        assertEquals(size, calls.get());
        assertEquals(size, DoubleStream.of(a).parallel(4).skip(0, x -> calls.incrementAndGet()).count());
        assertEquals(size, calls.get());
    }

    @Test
    public void c133_parallelSkipWithActionClosesTheSourceAndValidates() {
        final AtomicInteger closed = new AtomicInteger();
        final IntStream s = IntStream.range(0, 100).parallel(3).onClose(closed::incrementAndGet);
        assertEquals(97, s.skip(3, x -> {
        }).count());
        assertEquals(1, closed.get());
        assertThrows(IllegalArgumentException.class, () -> IntStream.range(0, 3).parallel(2).skip(-1, x -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> IntStream.range(0, 3).parallel(2).skip(1, null));
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-131 (doc lock): LongStream.interval waits uninterruptibly, keeps the interrupt flag; takeWhile idiom stops it
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void c131_intervalWaitIsUninterruptibleAndTheTakeWhileIdiomStopsIt() throws Exception {
        final long[] count = new long[2];
        final boolean[] flag = new boolean[2];
        final Thread t1 = new Thread(() -> {
            count[0] = LongStream.interval(20).limit(10).count();
            flag[0] = Thread.currentThread().isInterrupted();
        });
        t1.start();
        Thread.sleep(60);
        t1.interrupt();
        t1.join(5000);
        assertFalse(t1.isAlive());
        assertEquals(10, count[0]); // the interrupt did not end the traversal
        assertTrue(flag[0]); // but the interrupt status is kept

        final Thread t2 = new Thread(() -> {
            count[1] = LongStream.interval(20).takeWhile(x -> !Thread.currentThread().isInterrupted()).count();
            flag[1] = Thread.currentThread().isInterrupted();
        });
        t2.start();
        Thread.sleep(100);
        t2.interrupt();
        t2.join(5000);
        assertFalse(t2.isAlive());
        assertTrue(count[1] > 0);
        assertTrue(flag[1]);
    }

}
