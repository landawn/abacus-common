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
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;

/**
 * Review fixes 2026-09-24 for the Int, Long, Char and Byte stream families (fixer FX-PA).
 */
public class PrimitiveStreamsAReview20260924Test extends TestBase {

    // ------------------------------------------------------------------------------------------------------------
    // C-014 transformViaJdkStream: a throwing eager transfer closes the source
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void c014_intTransformViaJdkStreamClosesSourceWhenTransferThrows() {
        final AtomicInteger closed = new AtomicInteger();
        final IntStream s1 = IntStream.of(1, 2).onClose(closed::incrementAndGet);
        assertThrows(IllegalStateException.class, () -> s1.transformViaJdkStream(js -> {
            throw new IllegalStateException("boom");
        }));
        assertEquals(1, closed.get());
        assertThrows(IllegalStateException.class, s1::count); // closed, not reusable

        final IntStream s2 = IntStream.of(IntIterator.of(1, 2)).onClose(closed::incrementAndGet);
        assertThrows(IllegalStateException.class, () -> s2.transformViaJdkStream(js -> {
            throw new IllegalStateException("boom");
        }, false));
        assertEquals(2, closed.get());

        // success path unchanged: closing the result closes the source exactly once
        final IntStream s3 = IntStream.of(1, 2, 3).onClose(closed::incrementAndGet);
        assertArrayEquals(new int[] { 2 }, s3.transformViaJdkStream(js -> js.filter(i -> i % 2 == 0)).toArray());
        assertEquals(3, closed.get());

        // deferred: the transfer is not invoked until traversal
        final AtomicInteger calls = new AtomicInteger();
        final IntStream deferred = IntStream.of(1, 2).transformViaJdkStream(js -> {
            calls.incrementAndGet();
            return js;
        }, true);
        assertEquals(0, calls.get());
        assertArrayEquals(new int[] { 1, 2 }, deferred.toArray());
        assertEquals(1, calls.get());
    }

    @Test
    public void c014_longTransformViaJdkStreamClosesSourceWhenTransferThrows() {
        final AtomicInteger closed = new AtomicInteger();
        final LongStream s1 = LongStream.of(1, 2).onClose(closed::incrementAndGet);
        assertThrows(IllegalStateException.class, () -> s1.transformViaJdkStream(js -> {
            throw new IllegalStateException("boom");
        }));
        assertEquals(1, closed.get());

        final LongStream s2 = LongStream.of(1, 2).parallel().onClose(closed::incrementAndGet);
        assertThrows(IllegalStateException.class, () -> s2.transformViaJdkStream(js -> {
            throw new IllegalStateException("boom");
        }, false));
        assertEquals(2, closed.get());

        final LongStream s3 = LongStream.of(1, 2, 3).onClose(closed::incrementAndGet);
        assertArrayEquals(new long[] { 2, 4, 6 }, s3.transformViaJdkStream(js -> js.map(x -> x * 2)).toArray());
        assertEquals(3, closed.get());
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-016 toCollection / toMultiset / toMap / groupTo with a null-returning factory -> NPE, stream closed
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void c016_intNullFactoryIsNpe() {
        final List<Supplier<IntStream>> sources = List.of(() -> IntStream.empty(), () -> IntStream.of(new int[0]), () -> IntStream.of(1, 2, 2),
                () -> IntStream.of(new int[] { 0, 1, 2, 3 }, 1, 3), () -> IntStream.of(IntIterator.of(1, 2)), () -> IntStream.range(0, 0),
                () -> IntStream.of(1, 2).skip(2), () -> IntStream.of(1, 2).parallel(), () -> IntStream.of(IntIterator.of(1, 2)).parallel());

        for (final Supplier<IntStream> src : sources) {
            final AtomicInteger closed = new AtomicInteger();
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> src.get().onClose(closed::incrementAndGet).toCollection(() -> (List<Integer>) null));
            assertTrue(ex.getMessage().contains("supplier returned null"), ex.getMessage());
            ex = assertThrows(NullPointerException.class, () -> src.get().onClose(closed::incrementAndGet).toMultiset(() -> null));
            assertTrue(ex.getMessage().contains("supplier returned null"), ex.getMessage());
            assertEquals(2, closed.get());
        }

        final List<Supplier<IntStream>> seqSources = List.of(() -> IntStream.empty(), () -> IntStream.of(1, 2, 2),
                () -> IntStream.of(IntIterator.of(1, 2)), () -> IntStream.of(1, 2).skip(2));
        for (final Supplier<IntStream> src : seqSources) {
            final AtomicInteger closed = new AtomicInteger();
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> src.get().onClose(closed::incrementAndGet).toMap(i -> i, i -> i, () -> (Map<Integer, Integer>) null));
            assertTrue(ex.getMessage().contains("mapFactory returned null"), ex.getMessage());
            assertThrows(NullPointerException.class,
                    () -> src.get().onClose(closed::incrementAndGet).toMap(i -> i, i -> i, Integer::sum, () -> (Map<Integer, Integer>) null));
            assertThrows(NullPointerException.class, () -> src.get()
                    .onClose(closed::incrementAndGet)
                    .groupTo(i -> i, java.util.stream.Collectors.counting(), () -> (Map<Integer, Long>) null));
            assertEquals(3, closed.get());
        }

        // valid factories still work
        assertEquals(List.of(1, 2), IntStream.of(1, 2).toCollection(ArrayList::new));
        assertEquals(2, IntStream.of(IntIterator.of(7, 7)).toMultiset(Multiset::new).count(7));
        assertEquals(Map.of(1, 1), IntStream.of(1).toMap(i -> i, i -> i, () -> new HashMap<Integer, Integer>()));
    }

    @Test
    public void c016_longCharByteNullFactoryIsNpe() {
        final List<Supplier<? extends BaseStream<?, ?, ?, ?, ?, ?, ?, ?>>> sources = List.of( //
                () -> LongStream.empty(), () -> LongStream.of(1L, 2L), () -> LongStream.of(LongIterator.of(1L)), () -> LongStream.of(1L).skip(1),
                () -> CharStream.empty(), () -> CharStream.of('a', 'b'), () -> CharStream.of(CharIterator.of('a')), () -> CharStream.of('a').skip(1),
                () -> ByteStream.empty(), () -> ByteStream.of((byte) 1, (byte) 2), () -> ByteStream.of(ByteIterator.of((byte) 1)),
                () -> ByteStream.of((byte) 1).skip(1));

        for (final Supplier<? extends BaseStream<?, ?, ?, ?, ?, ?, ?, ?>> src : sources) {
            final AtomicInteger closed = new AtomicInteger();
            @SuppressWarnings("rawtypes")
            final BaseStream s1 = src.get();
            s1.onClose(closed::incrementAndGet);
            NullPointerException ex = assertThrows(NullPointerException.class, () -> s1.toCollection(() -> null));
            assertTrue(ex.getMessage().contains("supplier returned null"), ex.getMessage());
            assertEquals(1, closed.get());
            assertThrows(IllegalStateException.class, s1::count);

            @SuppressWarnings("rawtypes")
            final BaseStream s2 = src.get();
            s2.onClose(closed::incrementAndGet);
            ex = assertThrows(NullPointerException.class, () -> s2.toMultiset(() -> null));
            assertTrue(ex.getMessage().contains("supplier returned null"), ex.getMessage());
            assertEquals(2, closed.get());
        }

        final AtomicInteger closed = new AtomicInteger();
        assertThrows(NullPointerException.class,
                () -> LongStream.of(1L).onClose(closed::incrementAndGet).toMap(x -> x, x -> x, () -> (Map<Long, Long>) null));
        assertThrows(NullPointerException.class,
                () -> LongStream.empty().onClose(closed::incrementAndGet).toMap(x -> x, x -> x, Long::sum, () -> (Map<Long, Long>) null));
        assertThrows(NullPointerException.class, () -> CharStream.of(CharIterator.of('a'))
                .onClose(closed::incrementAndGet)
                .groupTo(c -> c, java.util.stream.Collectors.counting(), () -> (Map<Character, Long>) null));
        assertThrows(NullPointerException.class,
                () -> CharStream.of('a').onClose(closed::incrementAndGet).toMap(c -> c, c -> c, () -> (Map<Character, Character>) null));
        assertThrows(NullPointerException.class,
                () -> ByteStream.of((byte) 1).onClose(closed::incrementAndGet).toMap(b -> b, b -> b, () -> (Map<Byte, Byte>) null));
        assertThrows(NullPointerException.class, () -> ByteStream.empty()
                .onClose(closed::incrementAndGet)
                .groupTo(b -> b, java.util.stream.Collectors.counting(), () -> (Map<Byte, Long>) null));
        assertEquals(6, closed.get());
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-034 iterator-backed sum()/average(): no silent wrap of the long accumulator
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void c034_iteratorIntSumBeyondLongRangeThrows() {
        // 2^33 * Integer.MIN_VALUE = -2^64: the long accumulator used to wrap back to exactly 0 and return it.
        // The fixed code throws as soon as the running total leaves the long range (after 2^32 + 1 elements).
        assertThrows(ArithmeticException.class, () -> IntStream.repeat(Integer.MIN_VALUE, 1L << 33).sum());
    }

    @Test
    public void c034_iteratorIntAverageBeyondLongRangeIsExact() {
        final com.landawn.abacus.util.u.OptionalDouble avg = IntStream.repeat(Integer.MIN_VALUE, (1L << 32) + 2).average();
        assertEquals(Integer.MIN_VALUE, avg.getAsDouble(), 0.0);
    }

    @Test
    public void c034_iteratorSumAndAverageSmallInputsUnchanged() {
        assertEquals(15, IntStream.of(IntIterator.of(1, 2, 3, 4, 5)).sum());
        assertEquals(0, IntStream.of(IntIterator.of()).sum());
        assertThrows(ArithmeticException.class, () -> IntStream.of(IntIterator.of(Integer.MAX_VALUE, 1)).sum());
        assertThrows(ArithmeticException.class, () -> IntStream.of(IntIterator.of(Integer.MIN_VALUE, -1)).sum());
        assertEquals(Integer.MIN_VALUE, IntStream.of(IntIterator.of(Integer.MIN_VALUE, 0, 1, -1)).sum());
        assertEquals(3.0, IntStream.of(IntIterator.of(1, 2, 3, 4, 5)).average().getAsDouble(), 0.0);
        assertEquals(Integer.MAX_VALUE, IntStream.of(IntIterator.of(Integer.MAX_VALUE, Integer.MAX_VALUE)).average().getAsDouble(), 0.0);
        assertFalse(IntStream.of(IntIterator.of()).average().isPresent());
        assertEquals(-0.5, IntStream.of(IntIterator.of(Integer.MIN_VALUE, Integer.MAX_VALUE)).average().getAsDouble(), 0.0);

        assertEquals(65535 * 3, CharStream.of(CharIterator.of('￿', '￿', '￿')).sum());
        assertThrows(ArithmeticException.class, () -> CharStream.repeat('￿', 32769).sum());
        assertEquals(65535.0, CharStream.repeat('￿', 32769).average().getAsDouble(), 0.0);
        assertEquals(2147516415L, CharStream.repeat('￿', 32769).summaryStatistics().getSum());
        assertEquals(-256, ByteStream.of(ByteIterator.of((byte) -128, (byte) -128)).sum());
        assertEquals(-128.0, ByteStream.of(ByteIterator.of((byte) -128, (byte) -128)).average().getAsDouble(), 0.0);
        assertFalse(ByteStream.of(ByteIterator.of()).average().isPresent());
        assertThrows(ArithmeticException.class, () -> ByteStream.repeat((byte) 127, 16_909_321).sum());
        assertEquals(2147483767L, ByteStream.repeat((byte) 127, 16_909_321).summaryStatistics().getSum());
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-035 ascending ranges are flagged sorted
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void c035_ascendingIntRangesAreSorted() {
        IntStream s = IntStream.range(0, 10);
        assertSame(s, s.sorted());
        s = IntStream.rangeClosed(-3, 3);
        assertSame(s, s.sorted());
        s = IntStream.range(0, 10, 3);
        assertSame(s, s.sorted());
        s = IntStream.rangeClosed(0, 10, 5);
        assertSame(s, s.sorted());
        s = IntStream.ofIndices(5);
        assertSame(s, s.sorted());
        s = IntStream.ofIndices(5, 2);
        assertSame(s, s.sorted());

        // descending ranges are not flagged, and sorting them still works
        s = IntStream.range(5, 0, -1);
        assertNotSame(s, s.sorted());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, IntStream.range(5, 0, -1).sorted().toArray());
        assertArrayEquals(new int[] { 0, 2, 4 }, IntStream.ofIndices(5, -2).sorted().toArray());
        assertArrayEquals(new int[] { 4, 2, 0 }, IntStream.ofIndices(5, -2).toArray());

        // sorted fast paths stay correct
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, IntStream.range(0, 5).distinct().toArray());
        assertEquals(0, IntStream.range(0, 5).min().getAsInt());
        assertEquals(4, IntStream.range(0, 5).max().getAsInt());
        assertEquals(3, IntStream.range(0, 5).kthLargest(2).getAsInt());
        assertFalse(IntStream.range(0, 1).kthLargest(2).isPresent());
        assertArrayEquals(new int[] { 7, 8, 9 }, IntStream.range(0, 10).top(3).toArray());
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, IntStream.range(0, 5).top(10).toArray());
        assertArrayEquals(new int[] { 0, 1 }, IntStream.range(0, 10).top(2, (a, b) -> Integer.compare(b, a)).sorted().toArray());
        assertArrayEquals(new int[] { 9, 8, 7 }, IntStream.range(0, 10).reverseSorted().limit(3).toArray());
        assertArrayEquals(new int[] { 2, 4 }, IntStream.range(0, 5).map(i -> 4 - i).filter(i -> i % 2 == 0).sorted().skip(1).toArray());
        assertArrayEquals(new int[] { 0, 3, 6 }, IntStream.range(0, 7).step(3).toArray());
        assertArrayEquals(IntStream.range(0, 1000).toArray(), IntStream.range(0, 1000).parallel().sorted().toArray());

        // the sorted flag makes a huge range cheap to sort/distinct instead of buffering 2^31 ints
        assertArrayEquals(new int[] { 0, 1, 2 }, IntStream.range(0, Integer.MAX_VALUE).sorted().distinct().limit(3).toArray());
        assertArrayEquals(new int[] { Integer.MIN_VALUE }, IntStream.rangeClosed(Integer.MIN_VALUE, Integer.MAX_VALUE).sorted().limit(1).toArray());
    }

    @Test
    public void c035_ascendingLongRangesAreSorted() {
        LongStream s = LongStream.range(0, 10);
        assertSame(s, s.sorted());
        s = LongStream.rangeClosed(0, 10);
        assertSame(s, s.sorted());
        s = LongStream.range(0, 10, 4);
        assertSame(s, s.sorted());
        s = LongStream.rangeClosed(Long.MIN_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);
        assertSame(s, s.sorted());
        s = LongStream.range(10, 0, -1);
        assertNotSame(s, s.sorted());

        assertArrayEquals(new long[] { 0, 1, 2 }, LongStream.range(0, 3_000_000_000L).sorted().limit(3).toArray());
        assertEquals(0L, LongStream.range(0, 1L << 40).min().getAsLong());
        assertArrayEquals(new long[] { Long.MIN_VALUE, -1, Long.MAX_VALUE - 1 },
                LongStream.rangeClosed(Long.MIN_VALUE, Long.MAX_VALUE, Long.MAX_VALUE).sorted().toArray());
        assertArrayEquals(new long[] { 1, 2, 3 }, LongStream.range(3, 0, -1).sorted().toArray());
        assertArrayEquals(new long[] { 0, 1, 2 }, LongStream.range(0, 1L << 40).distinct().limit(3).toArray());
        assertArrayEquals(new long[] { 7, 8, 9 }, LongStream.range(0, 10).top(3).toArray());
        assertEquals(8L, LongStream.range(0, 10).kthLargest(2).getAsLong());
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-036 CharStream.of(CharSequence) / IntStream.ofCodePoints: content captured at creation
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void c036_charStreamOfMutableCharSequenceIsSnapshot() {
        StringBuilder sb = new StringBuilder("hello");
        CharStream s = CharStream.of(sb);
        sb.setLength(2);
        assertEquals("hello", s.join(""));

        sb = new StringBuilder("hello");
        s = CharStream.of(sb);
        sb.append("XYZ");
        assertEquals("hello", s.join(""));

        sb = new StringBuilder("hello");
        s = CharStream.of(sb, 1, 4);
        sb.setLength(0);
        assertEquals(3, s.count());

        final StringBuffer buf = new StringBuffer("abc");
        s = CharStream.of(buf);
        buf.setCharAt(0, 'z');
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, s.toArray());

        final CharSequence cb = CharBuffer.wrap("xyz");
        assertArrayEquals(new char[] { 'y', 'z' }, CharStream.of(cb, 1, 3).toArray());
        assertEquals(1, CharStream.of(new StringBuilder("abcd"), 1, 3).skip(1).count());

        // String input and null/empty unchanged
        assertEquals("ab", CharStream.of("ab").join(""));
        assertEquals("b", CharStream.of("abc", 1, 2).join(""));
        assertEquals(0, CharStream.of((CharSequence) null).count());
        assertEquals(0, CharStream.of(new StringBuilder()).count());
        assertEquals(0, CharStream.of(new StringBuilder("ab"), 1, 1).count());
        assertThrows(IndexOutOfBoundsException.class, () -> CharStream.of(new StringBuilder("ab"), 1, 3));
        // surrogate pairs stay two code units
        assertArrayEquals(new char[] { 'a', '\uD83D', '\uDE00' }, CharStream.of(new StringBuilder("a😀")).toArray());
    }

    @Test
    public void c036_ofCodePointsIsSnapshotAndPassesUnpairedSurrogates() {
        StringBuilder sb = new StringBuilder("a");
        IntStream s = IntStream.ofCodePoints(sb);
        sb.append("xy");
        assertArrayEquals(new int[] { 'a' }, s.toArray());

        sb = new StringBuilder();
        s = IntStream.ofCodePoints(sb);
        sb.append("xy");
        assertArrayEquals(new int[0], s.toArray());

        sb = new StringBuilder("abc");
        s = IntStream.ofCodePoints(sb);
        sb.setLength(1);
        assertArrayEquals(new int[] { 'a', 'b', 'c' }, s.toArray());

        assertArrayEquals(new int[] { 97, 0x1F600, 98 }, IntStream.ofCodePoints("a😀b").toArray());
        assertArrayEquals(new int[] { 97, 55357 }, IntStream.ofCodePoints("a\uD83D").toArray());
        assertArrayEquals(new int[] { 56832, 55357 }, IntStream.ofCodePoints("\uDE00\uD83D").toArray());
        assertArrayEquals("x𝐀y".codePoints().toArray(), IntStream.ofCodePoints(new StringBuilder("x𝐀y")).toArray());
        assertEquals(0, IntStream.ofCodePoints(null).count());
        assertEquals(0, IntStream.ofCodePoints("").count());
        assertEquals(8, IntStream.ofCodePoints("programming").distinct().count());
        assertEquals(2, IntStream.ofCodePoints("😀😀z").skip(1).count());
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-037 backward ofIndices over an empty source
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void c037_backwardOfIndicesOverEmptySourceIsEmpty() {
        final int[] empty = {};
        assertArrayEquals(new int[0], IntStream.ofIndices(empty, empty.length - 1, -1, (a, f) -> N.lastIndexOf(a, 1, f)).toArray());
        assertArrayEquals(new int[0], IntStream.ofIndices(new int[] { 1 }, -5, -2, (a, f) -> N.lastIndexOf(a, 1, f)).toArray());
        assertArrayEquals(new int[0], IntStream.ofIndices("", -1, -1, (str, f) -> str.lastIndexOf('a', f)).toArray());
        assertArrayEquals(new int[0], IntStream.ofIndices(List.of(), -1, -1, (l, f) -> -1).toArray());

        final int[] source = { 1, 2, 3, 1, 5, 1 };
        assertArrayEquals(new int[] { 5, 3, 0 }, IntStream.ofIndices(source, source.length - 1, -1, (a, f) -> N.lastIndexOf(a, 1, f)).toArray());

        // forward direction still rejects a negative fromIndex; validation order unchanged
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> IntStream.ofIndices(source, -1, 1, (a, f) -> N.indexOf(a, 1, f)));
        assertTrue(ex.getMessage().contains("fromIndex"), ex.getMessage());
        ex = assertThrows(IllegalArgumentException.class, () -> IntStream.ofIndices(new int[0], -1, null));
        assertTrue(ex.getMessage().contains("fromIndex"), ex.getMessage());
        ex = assertThrows(IllegalArgumentException.class, () -> IntStream.ofIndices(new int[0], -1, 0, (a, f) -> -1));
        assertTrue(ex.getMessage().contains("fromIndex"), ex.getMessage());
        ex = assertThrows(IllegalArgumentException.class, () -> IntStream.ofIndices(new int[0], 0, 0, null));
        assertTrue(ex.getMessage().contains("increment"), ex.getMessage());
        ex = assertThrows(IllegalArgumentException.class, () -> IntStream.ofIndices(new int[0], -1, -1, null));
        assertTrue(ex.getMessage().contains("indexFunc"), ex.getMessage());
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-049 zip(Collection, NFunction): a null zip result is rejected instead of becoming 0
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void c049_nullZipResultIsNpe() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> CharStream.zip(List.of(CharStream.of('a'), CharStream.of('b')), cs -> null).toArray());
        assertEquals("zipFunction returned null", ex.getMessage());
        assertThrows(NullPointerException.class,
                () -> CharStream.zip(List.of(CharStream.of('a'), CharStream.of('b', 'c')), new char[] { 'x', 'y' }, cs -> null).toArray());
        assertThrows(NullPointerException.class, () -> ByteStream.zip(List.of(ByteStream.of((byte) 1), ByteStream.of((byte) 2)), bs -> null).toArray());
        assertThrows(NullPointerException.class,
                () -> ByteStream.zip(List.of(ByteStream.of((byte) 1)), new byte[] { 0 }, bs -> null).toArray());
        assertThrows(NullPointerException.class, () -> IntStream.zip(List.of(IntStream.of(1), IntStream.of(2)), is -> null).toArray());
        assertThrows(NullPointerException.class, () -> IntStream.zip(List.of(IntStream.of(1)), new int[] { 0 }, is -> null).toArray());
        assertThrows(NullPointerException.class, () -> LongStream.zip(List.of(LongStream.of(1), LongStream.of(2)), ls -> null).toArray());
        assertThrows(NullPointerException.class, () -> LongStream.zip(List.of(LongStream.of(1)), new long[] { 0 }, ls -> null).toArray());

        // the zipped sources are closed when the failing terminal operation closes the stream
        final AtomicInteger closed = new AtomicInteger();
        assertThrows(NullPointerException.class,
                () -> IntStream.zip(List.of(IntStream.of(1).onClose(closed::incrementAndGet), IntStream.of(2)), is -> null).count());
        assertEquals(1, closed.get());

        // non-null results unchanged
        assertArrayEquals(new char[] { 'x', 'z', '|' },
                CharStream.zip(Arrays.asList(CharStream.of('a', 'b', 'c'), CharStream.of('x', 'y', 'z')), cs -> (char) (cs[0] + cs[1] - 'a')).toArray());
        assertArrayEquals(new int[] { 3, 5 }, IntStream.zip(List.of(IntStream.of(1, 2), IntStream.of(2, 3)), is -> is[0] + is[1]).toArray());
        assertArrayEquals(new long[] { 3, 7 }, LongStream.zip(List.of(LongStream.of(1, 2), LongStream.of(2)), new long[] { 0, 5 }, ls -> ls[0] + ls[1]).toArray());
        assertArrayEquals(new byte[] { 3 }, ByteStream.zip(List.of(ByteStream.of((byte) 1), ByteStream.of((byte) 2)), bs -> (byte) (bs[0] + bs[1])).toArray());
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-001 step(n) skips the gap lazily
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void c001_intStepDoesNotReadTheGapUntilNeeded() {
        final AtomicLong pulled = new AtomicLong();
        assertArrayEquals(new int[] { 0, 1000 }, IntStream.iterate(0, x -> x + 1).onEach(x -> pulled.incrementAndGet()).step(1000).limit(2).toArray());
        assertEquals(1001, pulled.get());

        pulled.set(0);
        assertArrayEquals(new int[] { 1 },
                IntStream.generate(() -> (int) pulled.incrementAndGet()).step(1_000_000).limit(1).toArray());
        assertEquals(1, pulled.get());

        assertEquals(1, IntStream.of(1, 2, 3).map(x -> x == 2 ? 1 / 0 : x).step(2).first().getAsInt());
        assertThrows(ArithmeticException.class, () -> IntStream.of(1, 2, 3).map(x -> x == 2 ? 1 / 0 : x).step(2).toArray());

        // values unchanged: iterator source (element-by-element skip), range source (atomic advance), parallel
        assertArrayEquals(new int[] { 0, 3, 6, 9 }, IntStream.of(IntIterator.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)).step(3).toArray());
        assertArrayEquals(new int[] { 0, 3, 6, 9 }, IntStream.range(0, 10).map(x -> x).step(3).toArray());
        assertArrayEquals(new int[] { 0, 3, 6, 9 }, IntStream.range(0, 10).step(3).toArray());
        assertEquals(4, IntStream.range(0, 10).onEach(x -> {
        }).step(3).count());
        assertArrayEquals(IntStream.range(0, 100).step(7).toArray(), IntStream.range(0, 100).map(x -> x).parallel(4).step(7).sorted().toArray());
        assertArrayEquals(new int[] { 5 }, IntStream.of(IntIterator.of(5, 6)).step(Long.MAX_VALUE).toArray());
        assertArrayEquals(new int[0], IntStream.of(IntIterator.of()).step(2).toArray());
    }

    @Test
    public void c001_longCharByteStepDoesNotReadTheGapUntilNeeded() {
        final AtomicLong pulled = new AtomicLong();
        assertArrayEquals(new long[] { 0, 1000 }, LongStream.iterate(0, x -> x + 1).onEach(x -> pulled.incrementAndGet()).step(1000).limit(2).toArray());
        assertEquals(1001, pulled.get());
        assertEquals(0L, LongStream.iterate(0, x -> x + 1).step(Long.MAX_VALUE).first().getAsLong());
        assertEquals(1L, LongStream.of(1, 2, 3).map(x -> x == 2 ? 1 / 0 : x).step(2).first().getAsLong());

        pulled.set(0);
        assertArrayEquals(new char[] { 'a', 'd' },
                CharStream.iterate('a', c -> (char) (c + 1)).onEach(c -> pulled.incrementAndGet()).step(3).limit(2).toArray());
        assertEquals(4, pulled.get());
        assertEquals('a', CharStream.of('a', 'b', 'c').map(c -> c == 'b' ? (char) (1 / 0) : c).step(2).first().get());

        pulled.set(0);
        assertArrayEquals(new byte[] { 0, 5 },
                ByteStream.iterate((byte) 0, b -> (byte) (b + 1)).onEach(b -> pulled.incrementAndGet()).step(5).limit(2).toArray());
        assertEquals(6, pulled.get());
        assertEquals((byte) 1, ByteStream.of((byte) 1, (byte) 2, (byte) 3).map(b -> b == 2 ? (byte) (1 / 0) : b).step(2).first().get());

        assertArrayEquals(new char[] { 'a', 'c', 'e' }, CharStream.of(CharIterator.of('a', 'b', 'c', 'd', 'e')).step(2).toArray());
        assertArrayEquals(new byte[] { 1, 4 }, ByteStream.of(ByteIterator.of((byte) 1, (byte) 2, (byte) 3, (byte) 4)).step(3).toArray());
        assertArrayEquals(new long[] { 0, 2, 4 }, LongStream.range(0, 6).map(x -> x).parallel(3).step(2).sorted().toArray());
    }

    // ------------------------------------------------------------------------------------------------------------
    // Documentation locks (C-040, C-042, C-047, C-048) and P1-09
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void docLock_longSummaryStatisticsWrapsWhileAverageIsExact() {
        assertEquals(-2L, LongStream.of(Long.MAX_VALUE, Long.MAX_VALUE).summaryStatistics().getSum());
        assertEquals(-1.0, LongStream.of(Long.MAX_VALUE, Long.MAX_VALUE).summaryStatistics().getAverage(), 0.0);
        assertEquals(-2L, LongStream.of(Long.MAX_VALUE, Long.MAX_VALUE).summaryStatisticsAndPercentiles().left().getSum());
        assertEquals(9.223372036854776E18, LongStream.of(Long.MAX_VALUE, Long.MAX_VALUE).average().getAsDouble(), 0.0);
    }

    @Test
    public void docLock_longRangeCountAndEmptyRules() {
        assertThrows(ArithmeticException.class, () -> LongStream.range(Long.MIN_VALUE, Long.MAX_VALUE).count());
        assertThrows(ArithmeticException.class, () -> LongStream.rangeClosed(0, Long.MAX_VALUE).count());
        assertEquals(Long.MAX_VALUE, LongStream.rangeClosed(1, Long.MAX_VALUE).count());
        assertThrows(IllegalStateException.class, () -> LongStream.range(0, 3_000_000_000L).toArray());
        assertEquals(0, LongStream.range(5, 5).count());
        assertEquals(0, LongStream.range(5, 1).count());
        assertEquals(0, LongStream.rangeClosed(5, 1).count());
        assertEquals(0, LongStream.range(0, 10, -1).count());
        assertArrayEquals(new long[] { 7 }, LongStream.rangeClosed(7, 7, -3).toArray());
    }

    @Test
    public void docLock_boxedEqualityInIntersectionAndDifference() {
        assertEquals(0, LongStream.of(1, 2).intersection(List.of(1, 2)).count());
        assertArrayEquals(new long[] { 1, 2 }, LongStream.of(1, 2).intersection(List.of(1L, 2L)).toArray());
        assertArrayEquals(new long[] { 1, 2 }, LongStream.of(1, 2).difference(List.of(1, 2)).toArray());
        assertEquals(0, ByteStream.of((byte) 1, (byte) 2).intersection(List.of(1, 2)).count());
        assertEquals(0, CharStream.of('a').intersection(List.of(97)).count());
        assertEquals(0, IntStream.of(1, 2).intersection(List.of(1L, 2L)).count());
        assertArrayEquals(new int[] { 1 }, IntStream.of(1, 2).intersection(List.of(1)).toArray());
    }

    @Test
    public void docLock_charStreamSplitsSurrogatePairs() {
        final String s = "a😀b";
        assertEquals("b\uDE00\uD83Da", CharStream.of(s).reversed().join(""));
        assertEquals("a,\uD83D,\uDE00,b", CharStream.of(s).join(","));
        assertEquals("xy", CharStream.of("x𝐀y").filter(Character::isLetter).join(""));
        assertEquals("𐐨", CharStream.of("𐐨").map(Character::toUpperCase).join(""));
    }

    @Test
    public void p109_arrayBackedBuffersStreamTheirRemainingWindow() {
        final int[] ints = { 0, 1, 2, 3, 4, 5 };
        final IntBuffer ib = IntBuffer.wrap(ints, 1, 3); // position 1, limit 4
        assertArrayEquals(new int[] { 1, 2, 3 }, IntStream.of(ib).toArray());
        assertEquals(1, ib.position());
        final IntBuffer slice = IntBuffer.wrap(ints).position(2).slice(); // arrayOffset 2
        slice.position(1);
        assertArrayEquals(new int[] { 3, 4, 5 }, IntStream.of(slice).toArray());
        assertEquals(2, IntStream.of(slice).skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3 }, IntStream.of(IntBuffer.wrap(ints, 1, 3).asReadOnlyBuffer()).toArray());
        assertEquals(0, IntStream.of((IntBuffer) null).count());

        final long[] longs = { 7, 8, 9 };
        assertArrayEquals(new long[] { 8, 9 }, LongStream.of(LongBuffer.wrap(longs).position(1)).toArray());

        final char[] chars = { 'a', 'b', 'c', 'd' };
        final CharBuffer cb = CharBuffer.wrap(chars, 1, 2);
        assertArrayEquals(new char[] { 'b', 'c' }, CharStream.of(cb).toArray());
        assertArrayEquals(new char[] { 'y', 'z' }, CharStream.of(CharBuffer.wrap("xyz").position(1)).toArray()); // no backing array

        final ByteBuffer direct = ByteBuffer.allocateDirect(3).put(new byte[] { 1, 2, 3 }).flip();
        assertArrayEquals(new byte[] { 1, 2, 3 }, ByteStream.of(direct).toArray());
        final ByteBuffer heap = ByteBuffer.wrap(new byte[] { 9, 8, 7, 6 }, 1, 2);
        assertArrayEquals(new byte[] { 8, 7 }, ByteStream.of(heap).toArray());
        assertArrayEquals(new byte[0], ByteStream.of(ByteBuffer.wrap(new byte[] { 1 }, 1, 0)).toArray());
    }

    @Test
    public void p108_byteStreamNullAndFlattenContracts() {
        assertEquals(0, ByteStream.of((byte[]) null).count());
        assertEquals(0, ByteStream.of((byte[]) null, 0, 0).count());
        assertThrows(IndexOutOfBoundsException.class, () -> ByteStream.of((byte[]) null, 0, 1));
        assertNotSame(ByteStream.empty(), ByteStream.empty());
        assertArrayEquals(new byte[] { 1, 2, 3 }, ByteStream.flatten(new byte[][] { { 1 }, null, { 2, 3 } }).toArray());
        assertArrayEquals(new byte[] { 1, 2, 3 }, ByteStream.flatten(new byte[][] { { 1 }, null, { 2, 3 } }, false).toArray());
        assertArrayEquals(new byte[] { 1, 2, 3 }, ByteStream.flatten(new byte[][] { { 1, 3 }, null, { 2 } }, true).toArray());
        assertEquals(0, ByteStream.flatten((byte[][]) null).count());
        assertEquals(0, ByteStream.flatten((byte[][]) null, true).count());
        assertThrows(IllegalArgumentException.class, () -> ByteStream.range((byte) 0, (byte) 5, (byte) 0));
        assertArrayEquals(new byte[] { 5, 3, 1 }, ByteStream.range((byte) 5, (byte) 0, (byte) -2).toArray());
    }
}
