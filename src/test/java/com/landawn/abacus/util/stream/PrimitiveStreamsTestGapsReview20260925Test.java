package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.function.ToIntFunction;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.MergeResult;

/**
 * Review fixes 2026-09-25 (fixer F11b, tests only) - coverage gaps of the primitive stream families that the 2026-09-24
 * cycles left open: U28-01 (lazy step() skips the gap with ONE bulk advance(), plus the failure-atomic retry rule, per family),
 * U25-03 / U26-03 / U27-02 (the C-132 merge(Collection) contract sentences pinned for all seven families: TAKE_FIRST-on-ties
 * equals the left fold, an already-closed input closes only the streams preceding it, null elements are empty and closing the
 * result closes every source), U26-04 (the Char/Byte sum() thresholds of the C-046 javadoc test-locked like Short's) and
 * U27-04 (the H1-02 concat paragraphs doc-locked for Double and Float).
 * <p>
 * All tests pin the CURRENT behaviour (the reviews found no defect at these sites); they exist so that a regression of the
 * "gap skipped element by element" / "merge rollback" / "sum threshold" kind is caught by the suite instead of by hand.
 */
public class PrimitiveStreamsTestGapsReview20260925Test extends TestBase {

    // ------------------------------------------------------------------------------------------------------------
    // shared helpers: values of any primitive stream as ints (via toArray() + reflection), generic over the family
    // ------------------------------------------------------------------------------------------------------------

    private static int toInt(final Object v) {
        return v instanceof Character ? (Character) v : ((Number) v).intValue();
    }

    /** Drains the stream through toArray() (a real terminal, so the pull counts are those of a terminal operation). */
    private static List<Integer> values(final BaseStream<?, ?, ?, ?, ?, ?, ?, ?> s) {
        final Object array = s.toArray();
        final int n = java.lang.reflect.Array.getLength(array);
        final List<Integer> r = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            r.add(toInt(java.lang.reflect.Array.get(array, i)));
        }
        return r;
    }

    // ------------------------------------------------------------------------------------------------------------
    // U28-01 (LST/C-001, cycle-1 integration catch #1): step(n) skips the gap with ONE bulk advance(gap), never element
    // by element; a retried hasNext()/nextX() re-issues the full gap only for a failure-atomic upstream.
    // ------------------------------------------------------------------------------------------------------------

    /** The counters and behaviour shared by the seven counting iterators below. */
    static final class Counter {
        final int limit;
        int cursor = 0;
        int nextCalls = 0;
        int advanceCalls = 0;
        int failAdvance = 0;
        int partialMove = 0;
        long lastAdvance = -1;
        boolean atomic = false;
        final List<Long> advances = new ArrayList<>();

        Counter(final int limit) {
            this.limit = limit;
        }

        boolean hasNext() {
            return cursor < limit;
        }

        int next() {
            if (cursor >= limit) {
                throw new NoSuchElementException();
            }
            nextCalls++;
            return cursor++;
        }

        void advance(final long n) {
            advanceCalls++;
            lastAdvance = n;
            advances.add(n);
            if (failAdvance > 0) {
                failAdvance--;
                cursor += partialMove;
                throw new IllegalStateException("advance failed");
            }
            cursor = (int) Math.min(limit, cursor + n);
        }
    }

    static final class CByte extends ByteIteratorEx {
        final Counter c;

        CByte(final Counter c) {
            this.c = c;
        }

        @Override
        public boolean hasNext() {
            return c.hasNext();
        }

        @Override
        public byte nextByte() {
            return (byte) c.next();
        }

        @Override
        public void advance(final long n) {
            c.advance(n);
        }

        @Override
        boolean supportsFailureAtomicAdvance() {
            return c.atomic;
        }
    }

    static final class CChar extends CharIteratorEx {
        final Counter c;

        CChar(final Counter c) {
            this.c = c;
        }

        @Override
        public boolean hasNext() {
            return c.hasNext();
        }

        @Override
        public char nextChar() {
            return (char) c.next();
        }

        @Override
        public void advance(final long n) {
            c.advance(n);
        }

        @Override
        boolean supportsFailureAtomicAdvance() {
            return c.atomic;
        }
    }

    static final class CShort extends ShortIteratorEx {
        final Counter c;

        CShort(final Counter c) {
            this.c = c;
        }

        @Override
        public boolean hasNext() {
            return c.hasNext();
        }

        @Override
        public short nextShort() {
            return (short) c.next();
        }

        @Override
        public void advance(final long n) {
            c.advance(n);
        }

        @Override
        boolean supportsFailureAtomicAdvance() {
            return c.atomic;
        }
    }

    static final class CInt extends IntIteratorEx {
        final Counter c;

        CInt(final Counter c) {
            this.c = c;
        }

        @Override
        public boolean hasNext() {
            return c.hasNext();
        }

        @Override
        public int nextInt() {
            return c.next();
        }

        @Override
        public void advance(final long n) {
            c.advance(n);
        }

        @Override
        boolean supportsFailureAtomicAdvance() {
            return c.atomic;
        }
    }

    static final class CLong extends LongIteratorEx {
        final Counter c;

        CLong(final Counter c) {
            this.c = c;
        }

        @Override
        public boolean hasNext() {
            return c.hasNext();
        }

        @Override
        public long nextLong() {
            return c.next();
        }

        @Override
        public void advance(final long n) {
            c.advance(n);
        }

        @Override
        boolean supportsFailureAtomicAdvance() {
            return c.atomic;
        }
    }

    static final class CFloat extends FloatIteratorEx {
        final Counter c;

        CFloat(final Counter c) {
            this.c = c;
        }

        @Override
        public boolean hasNext() {
            return c.hasNext();
        }

        @Override
        public float nextFloat() {
            return c.next();
        }

        @Override
        public void advance(final long n) {
            c.advance(n);
        }

        @Override
        boolean supportsFailureAtomicAdvance() {
            return c.atomic;
        }
    }

    static final class CDouble extends DoubleIteratorEx {
        final Counter c;

        CDouble(final Counter c) {
            this.c = c;
        }

        @Override
        public boolean hasNext() {
            return c.hasNext();
        }

        @Override
        public double nextDouble() {
            return c.next();
        }

        @Override
        public void advance(final long n) {
            c.advance(n);
        }

        @Override
        boolean supportsFailureAtomicAdvance() {
            return c.atomic;
        }
    }

    /**
     * @param of builds the family's stream over a counting iterator
     * @param first {@code s.first().get()} of the family
     * @param iteratorEx {@code s.iteratorEx()} of the family (no "remember to close" log line, unlike iterator())
     * @param narrow how the family narrows the counter's int values ({@code (byte) x} for byte, identity for int ...)
     */
    private static <S extends BaseStream<?, ?, ?, ?, ?, ?, ?, S>> void assertStepSkipsTheGapWithOneBulkAdvance(final String family,
            final Function<Counter, S> of, final ToIntFunction<S> first, final Function<S, Iterator<?>> iteratorEx, final IntUnaryOperator narrow) {
        // (a) step(1000).limit(2): exactly two elements read, ONE advance(999) for the gap, nothing pulled beyond the 2nd element
        Counter c = new Counter(10_000);
        assertEquals(Arrays.asList(0, narrow.applyAsInt(1000)), values(of.apply(c).step(1000).limit(2)), family);
        assertEquals(2, c.nextCalls, family + ": next calls");
        assertEquals(1, c.advanceCalls, family + ": the gap is skipped with ONE bulk advance");
        assertEquals(999, c.lastAdvance, family + ": advance(step - 1)");

        // (b) step(1000).first(): one element read, the trailing gap is never skipped (lazy)
        c = new Counter(10_000);
        assertEquals(0, first.applyAsInt(of.apply(c).step(1000)), family);
        assertEquals(1, c.nextCalls, family + ": first() reads one element");
        assertEquals(0, c.advanceCalls, family + ": first() never advances");

        // (c) full traversal step(3) over 10: one advance per gap, exhaustion inside the last gap is graceful
        c = new Counter(10);
        assertEquals(Arrays.asList(0, 3, 6, 9), values(of.apply(c).step(3)), family);
        assertEquals(4, c.nextCalls, family);
        assertEquals(Arrays.asList(2L, 2L, 2L, 2L), c.advances, family + ": one advance(2) per gap, also for the trailing one");

        c = new Counter(5);
        assertEquals(Arrays.asList(0, 4), values(of.apply(c).step(4)), family + ": exhaustion inside the gap");
        assertEquals(2, c.advanceCalls, family);

        c = new Counter(0);
        assertEquals(Collections.emptyList(), values(of.apply(c).step(4)), family + ": empty source");
        assertEquals(0, c.advanceCalls, family + ": an empty source is never advanced");

        c = new Counter(7);
        assertEquals(1, of.apply(c).step(Long.MAX_VALUE).count(), family + ": step(Long.MAX_VALUE) yields one element");
        assertEquals(1, c.advanceCalls, family);
        assertEquals(Long.MAX_VALUE - 1, c.lastAdvance, family + ": one huge advance, no overflow");

        // (d) failure-atomic upstream: a failing advance leaves the gap pending, the retried hasNext() re-issues the FULL gap
        c = new Counter(100);
        c.atomic = true;
        c.failAdvance = 1;
        Iterator<?> it = iteratorEx.apply(of.apply(c).step(10));
        assertEquals(0, toInt(it.next()), family);
        final Iterator<?> atomicIt = it;
        assertThrows(IllegalStateException.class, atomicIt::hasNext, family + ": the failing advance propagates");
        assertTrue(it.hasNext(), family);
        assertEquals(10, toInt(it.next()), family + ": the full gap was re-skipped on the retry");
        assertEquals(Arrays.asList(9L, 9L), c.advances, family + ": atomic upstream -> advance(9) twice");

        // (e) non-atomic upstream: a failing (partial) advance is never re-applied; the retry continues from where it stopped
        c = new Counter(100);
        c.atomic = false;
        c.failAdvance = 1;
        c.partialMove = 4;
        it = iteratorEx.apply(of.apply(c).step(10));
        assertEquals(0, toInt(it.next()), family);
        final Iterator<?> nonAtomicIt = it;
        assertThrows(IllegalStateException.class, nonAtomicIt::hasNext, family);
        assertTrue(it.hasNext(), family);
        assertEquals(5, toInt(it.next()), family + ": non-atomic upstream -> the cursor after the partial move, no re-skip");
        assertEquals(Arrays.asList(9L), c.advances, family + ": non-atomic upstream -> advance(9) once");

        // (f) the retry through nextX() directly (not hasNext()) also re-skips for an atomic upstream
        c = new Counter(100);
        c.atomic = true;
        c.failAdvance = 1;
        it = iteratorEx.apply(of.apply(c).step(10));
        assertEquals(0, toInt(it.next()), family);
        final Iterator<?> directIt = it;
        assertThrows(IllegalStateException.class, directIt::next, family);
        assertEquals(10, toInt(it.next()), family);
        assertEquals(Arrays.asList(9L, 9L), c.advances, family);

        // (g) step after a skip() stage: the step gap is still ONE bulk advance (skip's own advance is separate)
        c = new Counter(10_000);
        c.atomic = true;
        assertEquals(Arrays.asList(5, narrow.applyAsInt(1005)), values(of.apply(c).skip(5).step(1000).limit(2)), family);
        assertEquals(2, c.nextCalls, family + ": atomic upstream: skip delegates one advance(5), step one advance(999)");
        assertEquals(Arrays.asList(5L, 999L), c.advances, family);
    }

    @Test
    public void c001_byteStepSkipsTheGapWithOneBulkAdvance() {
        assertStepSkipsTheGapWithOneBulkAdvance("byte", c -> ByteStream.of(new CByte(c)), s -> s.first().get(), s -> s.iteratorEx(), x -> (byte) x);
    }

    @Test
    public void c001_charStepSkipsTheGapWithOneBulkAdvance() {
        assertStepSkipsTheGapWithOneBulkAdvance("char", c -> CharStream.of(new CChar(c)), s -> s.first().get(), s -> s.iteratorEx(), x -> (char) x);
    }

    @Test
    public void c001_shortStepSkipsTheGapWithOneBulkAdvance() {
        assertStepSkipsTheGapWithOneBulkAdvance("short", c -> ShortStream.of(new CShort(c)), s -> s.first().get(), s -> s.iteratorEx(), x -> (short) x);
    }

    @Test
    public void c001_intStepSkipsTheGapWithOneBulkAdvance() {
        assertStepSkipsTheGapWithOneBulkAdvance("int", c -> IntStream.of(new CInt(c)), s -> s.first().get(), s -> s.iteratorEx(), x -> x);
    }

    @Test
    public void c001_longStepSkipsTheGapWithOneBulkAdvance() {
        assertStepSkipsTheGapWithOneBulkAdvance("long", c -> LongStream.of(new CLong(c)), s -> (int) s.first().get(), s -> s.iteratorEx(), x -> x);
    }

    @Test
    public void c001_floatStepSkipsTheGapWithOneBulkAdvance() {
        assertStepSkipsTheGapWithOneBulkAdvance("float", c -> FloatStream.of(new CFloat(c)), s -> (int) s.first().get(), s -> s.iteratorEx(), x -> x);
    }

    @Test
    public void c001_doubleStepSkipsTheGapWithOneBulkAdvance() {
        assertStepSkipsTheGapWithOneBulkAdvance("double", c -> DoubleStream.of(new CDouble(c)), s -> (int) s.first().get(), s -> s.iteratorEx(), x -> x);
    }

    // ------------------------------------------------------------------------------------------------------------
    // U25-03 / U26-03 / U27-02 (LST/C-132): the three merge(Collection) contract sentences, all seven families
    // ------------------------------------------------------------------------------------------------------------

    /** One primitive family: how to build a stream from int values and the two merge overloads under test. */
    private static final class Family<S extends BaseStream<?, ?, ?, ?, ?, ?, ?, S>> {
        final String name;
        final Function<int[], S> of;
        /** merge(Collection) with the ascending selector {@code x <= y ? TAKE_FIRST : TAKE_SECOND}. */
        final Function<List<S>, S> mergeAscending;
        /** merge(Collection) with the tie-revealing selector {@code x % 10 <= y % 10 ? TAKE_FIRST : TAKE_SECOND}. */
        final Function<List<S>, S> mergeOnLastDigit;
        /** merge(a, b) with the same tie-revealing selector - the left fold's building block. */
        final BinaryOperator<S> merge2OnLastDigit;

        Family(final String name, final Function<int[], S> of, final Function<List<S>, S> mergeAscending, final Function<List<S>, S> mergeOnLastDigit,
                final BinaryOperator<S> merge2OnLastDigit) {
            this.name = name;
            this.of = of;
            this.mergeAscending = mergeAscending;
            this.mergeOnLastDigit = mergeOnLastDigit;
            this.merge2OnLastDigit = merge2OnLastDigit;
        }
    }

    private static byte[] bytes(final int[] a) {
        final byte[] r = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            r[i] = (byte) a[i];
        }
        return r;
    }

    private static char[] chars(final int[] a) {
        final char[] r = new char[a.length];
        for (int i = 0; i < a.length; i++) {
            r[i] = (char) a[i];
        }
        return r;
    }

    private static short[] shorts(final int[] a) {
        final short[] r = new short[a.length];
        for (int i = 0; i < a.length; i++) {
            r[i] = (short) a[i];
        }
        return r;
    }

    private static long[] longs(final int[] a) {
        final long[] r = new long[a.length];
        for (int i = 0; i < a.length; i++) {
            r[i] = a[i];
        }
        return r;
    }

    private static float[] floats(final int[] a) {
        final float[] r = new float[a.length];
        for (int i = 0; i < a.length; i++) {
            r[i] = a[i];
        }
        return r;
    }

    private static double[] doubles(final int[] a) {
        final double[] r = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            r[i] = a[i];
        }
        return r;
    }

    private static final Family<ByteStream> BYTE = new Family<>("byte", a -> ByteStream.of(bytes(a)),
            l -> ByteStream.merge(l, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND),
            l -> ByteStream.merge(l, (x, y) -> x % 10 <= y % 10 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND),
            (a, b) -> ByteStream.merge(a, b, (x, y) -> x % 10 <= y % 10 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND));

    private static final Family<CharStream> CHAR = new Family<>("char", a -> CharStream.of(chars(a)),
            l -> CharStream.merge(l, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND),
            l -> CharStream.merge(l, (x, y) -> x % 10 <= y % 10 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND),
            (a, b) -> CharStream.merge(a, b, (x, y) -> x % 10 <= y % 10 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND));

    private static final Family<ShortStream> SHORT = new Family<>("short", a -> ShortStream.of(shorts(a)),
            l -> ShortStream.merge(l, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND),
            l -> ShortStream.merge(l, (x, y) -> x % 10 <= y % 10 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND),
            (a, b) -> ShortStream.merge(a, b, (x, y) -> x % 10 <= y % 10 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND));

    private static final Family<IntStream> INT = new Family<>("int", a -> IntStream.of(a.clone()),
            l -> IntStream.merge(l, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND),
            l -> IntStream.merge(l, (x, y) -> x % 10 <= y % 10 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND),
            (a, b) -> IntStream.merge(a, b, (x, y) -> x % 10 <= y % 10 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND));

    private static final Family<LongStream> LONG = new Family<>("long", a -> LongStream.of(longs(a)),
            l -> LongStream.merge(l, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND),
            l -> LongStream.merge(l, (x, y) -> x % 10 <= y % 10 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND),
            (a, b) -> LongStream.merge(a, b, (x, y) -> x % 10 <= y % 10 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND));

    private static final Family<FloatStream> FLOAT = new Family<>("float", a -> FloatStream.of(floats(a)),
            l -> FloatStream.merge(l, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND),
            l -> FloatStream.merge(l, (x, y) -> x % 10 <= y % 10 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND),
            (a, b) -> FloatStream.merge(a, b, (x, y) -> x % 10 <= y % 10 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND));

    private static final Family<DoubleStream> DOUBLE = new Family<>("double", a -> DoubleStream.of(doubles(a)),
            l -> DoubleStream.merge(l, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND),
            l -> DoubleStream.merge(l, (x, y) -> x % 10 <= y % 10 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND),
            (a, b) -> DoubleStream.merge(a, b, (x, y) -> x % 10 <= y % 10 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND));

    private static final List<Family<?>> FAMILIES = Arrays.asList(BYTE, CHAR, SHORT, INT, LONG, FLOAT, DOUBLE);

    /**
     * "If an input stream is found to be already closed, the streams that precede it in the collection are closed, the
     * ones after it are left untouched" - and the closed one's own handlers are not re-run.
     */
    private static <S extends BaseStream<?, ?, ?, ?, ?, ?, ?, S>> void assertClosedSourceClosesOnlyThePrecedingStreams(final Family<S> f) {
        for (final int k : new int[] { 3, 4, 5, 7, 8, 9 }) {
            for (int bad = 0; bad < k; bad++) {
                final String label = f.name + " k=" + k + " bad=" + bad;
                final AtomicInteger[] closed = new AtomicInteger[k];
                final List<S> sources = new ArrayList<>(k);
                for (int j = 0; j < k; j++) {
                    closed[j] = new AtomicInteger();
                    sources.add(f.of.apply(new int[] { j }).onClose(closed[j]::incrementAndGet));
                }
                sources.get(bad).close();
                assertEquals(1, closed[bad].get(), label);

                assertThrows(IllegalStateException.class, () -> f.mergeAscending.apply(sources), label);

                for (int j = 0; j < k; j++) {
                    if (j < bad) {
                        assertEquals(1, closed[j].get(), label + " j=" + j + ": a preceding stream is closed exactly once");
                    } else if (j == bad) {
                        assertEquals(1, closed[j].get(), label + ": the closed input's handlers are not re-run");
                    } else {
                        assertEquals(0, closed[j].get(), label + " j=" + j + ": a following stream is untouched");
                        assertEquals(1, sources.get(j).count(), label + " j=" + j + ": an untouched later stream stays usable");
                    }
                }
            }
        }
    }

    /** "null elements are treated as empty streams ... closes all input streams when it is closed". */
    private static <S extends BaseStream<?, ?, ?, ?, ?, ?, ?, S>> void assertNullElementsAreEmptyAndClosingTheResultClosesEverySource(final Family<S> f) {
        final AtomicInteger closed = new AtomicInteger();
        final List<S> sources = new ArrayList<>();
        for (int j = 0; j < 9; j++) {
            sources.add(j % 3 == 0 ? null : f.of.apply(new int[] { j, j + 10 }).onClose(closed::incrementAndGet));
        }
        final S merged = f.mergeAscending.apply(sources);
        assertEquals(0, closed.get(), f.name);
        merged.close();
        assertEquals(6, closed.get(), f.name + ": every non-null source closed once");

        assertEquals(Arrays.asList(1, 2, 4, 5, 7, 8, 11, 12, 14, 15, 17, 18),
                values(f.mergeAscending.apply(Arrays.asList(null, f.of.apply(new int[] { 1, 11 }), f.of.apply(new int[] { 2, 12 }), null,
                        f.of.apply(new int[] { 4, 14 }), f.of.apply(new int[] { 5, 15 }), null, f.of.apply(new int[] { 7, 17 }), f.of.apply(new int[] { 8, 18 })))),
                f.name);
        assertEquals(0, f.mergeAscending.apply(Collections.<S> emptyList()).count(), f.name);
        assertEquals(0, f.mergeAscending.apply(Arrays.asList(null, null, null)).count(), f.name + ": all-null collection");
    }

    /**
     * "for a TAKE_FIRST-on-ties selector the result is the same as merging left to right". The values carry their source
     * index in the tens digit (source j holds j*10 + v, v ascending), and the selector compares the units digit only, so
     * ties are frequent and the output order REVEALS which source each element came from.
     */
    private static <S extends BaseStream<?, ?, ?, ?, ?, ?, ?, S>> void assertTakeFirstOnTiesEqualsTheLeftFold(final Family<S> f, final Random rnd) {
        for (int round = 0; round < 80; round++) {
            final int k = 3 + rnd.nextInt(10); // at most 12 sources, so every value (<= 119) fits a byte
            final List<S> tree = new ArrayList<>(k);
            final List<S> fold = new ArrayList<>(k);
            for (int j = 0; j < k; j++) {
                final int[] v = new int[rnd.nextInt(7)];
                for (int i = 0; i < v.length; i++) {
                    v[i] = rnd.nextInt(5);
                }
                Arrays.sort(v);
                for (int i = 0; i < v.length; i++) {
                    v[i] += j * 10;
                }
                tree.add(f.of.apply(v));
                fold.add(f.of.apply(v));
            }

            S left = f.merge2OnLastDigit.apply(fold.get(0), fold.get(1));
            for (int j = 2; j < k; j++) {
                left = f.merge2OnLastDigit.apply(left, fold.get(j));
            }

            assertEquals(values(left), values(f.mergeOnLastDigit.apply(tree)), f.name + " round " + round + " k=" + k);
        }
    }

    @Test
    public void c132_alreadyClosedSourceClosesOnlyThePrecedingStreams_allSevenFamilies() {
        for (final Family<?> f : FAMILIES) {
            assertClosedSourceClosesOnlyThePrecedingStreams(f);
        }
    }

    @Test
    public void c132_nullElementsAreEmptyAndClosingTheResultClosesEverySource_allSevenFamilies() {
        for (final Family<?> f : FAMILIES) {
            assertNullElementsAreEmptyAndClosingTheResultClosesEverySource(f);
        }
    }

    @Test
    public void c132_takeFirstOnTiesEqualsTheLeftFold_allSevenFamilies() {
        final Random rnd = new Random(20260925);
        for (final Family<?> f : FAMILIES) {
            assertTakeFirstOnTiesEqualsTheLeftFold(f, rnd);
        }
    }

    @Test
    public void c132_longTieOrderIsTheLeftFoldsForTheJavadocShape() {
        // The LongStream twin of PrimitiveStreamsReview20260924cTest#c132_javadocExamples: adjacent pairing keeps ties in source order.
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9),
                values(LongStream.merge(Arrays.asList(LongStream.of(1, 5, 9), LongStream.of(2, 6), LongStream.of(3, 7), LongStream.of(4, 8)),
                        (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)));
        // equal values from four sources with a units-digit selector: 10, 20, 30, 40 all tie -> source order, as in the left fold
        assertEquals(Arrays.asList(10, 20, 30, 40, 11, 21, 31, 41),
                values(LongStream.merge(Arrays.asList(LongStream.of(10, 11), LongStream.of(20, 21), LongStream.of(30, 31), LongStream.of(40, 41)),
                        (a, b) -> a % 10 <= b % 10 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)));
    }

    // ------------------------------------------------------------------------------------------------------------
    // U26-04 (LST/C-046): the exact sum() thresholds stated in the CharStream / ByteStream javadoc, test-locked like Short's
    // (PrimitiveStreamsBReview20260924Test#testC046_shortSumThreshold_docLock). Pinned at the int boundary only: the outcome
    // does not depend on how the long accumulator is carried (U30-02 is F11a's).
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void c046_charSumThreshold_docLock() {
        // "32,769 elements of U+FFFF" overflow; 32,768 x 65,535 = 2,147,450,880 still fits
        assertEquals(2_147_450_880, CharStream.repeat('\uFFFF', 32_768).sum());
        assertThrows(ArithmeticException.class, () -> CharStream.repeat('\uFFFF', 32_769).sum());
        final char[] maxes = new char[32_769];
        Arrays.fill(maxes, '\uFFFF');
        assertEquals(2_147_450_880, CharStream.of(maxes, 0, 32_768).sum());
        assertThrows(ArithmeticException.class, () -> CharStream.of(maxes).sum());
        assertThrows(ArithmeticException.class, () -> CharStream.of(maxes).map(x -> x).sum());
        // summaryStatistics().getSum() is a long and cannot overflow in practice
        assertEquals(2_147_516_415L, CharStream.of(maxes).summaryStatistics().getSum());
        assertEquals(2_147_516_415L, CharStream.repeat('\uFFFF', 32_769).summaryStatistics().getSum());
    }

    @Test
    public void c046_byteSumThreshold_docLock() {
        // "16,909,321 elements of 127, or 16,777,217 elements of -128" overflow; one fewer of each still fits
        assertEquals(2_147_483_640, ByteStream.repeat((byte) 127, 16_909_320).sum());
        assertThrows(ArithmeticException.class, () -> ByteStream.repeat((byte) 127, 16_909_321).sum());
        assertEquals(Integer.MIN_VALUE, ByteStream.repeat((byte) -128, 16_777_216).sum());
        assertThrows(ArithmeticException.class, () -> ByteStream.repeat((byte) -128, 16_777_217).sum());
        // summaryStatistics().getSum() is a long and cannot overflow in practice
        assertEquals(2_147_483_767L, ByteStream.repeat((byte) 127, 16_909_321).summaryStatistics().getSum());
    }

    // ------------------------------------------------------------------------------------------------------------
    // U27-04 (LST nit H1-02): the concat paragraphs doc-locked for Double and Float, as cTest#h102 does for Int/Char
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void h102_doubleConcatClosesEachExhaustedInputAndACloseFailureEndsTheTraversal() {
        for (int overload = 0; overload < 2; overload++) {
            final AtomicInteger aClosed = new AtomicInteger();
            final DoubleStream a = DoubleStream.of(1, 2).onClose(aClosed::incrementAndGet);
            final DoubleStream b = DoubleStream.of(3, 4);
            final DoubleStream s = overload == 0 ? DoubleStream.concat(a, b) : DoubleStream.concat(Arrays.asList(a, b));
            final DoubleIterator it = s.iterator();
            assertEquals(1.0, it.nextDouble());
            assertEquals(2.0, it.nextDouble());
            assertEquals(0, aClosed.get());
            assertTrue(it.hasNext()); // moving on to b closes the exhausted a
            assertEquals(1, aClosed.get());
            assertEquals(3.0, it.nextDouble());
            s.close();
            assertEquals(1, aClosed.get());
        }

        for (int overload = 0; overload < 2; overload++) {
            final List<Double> seen = new ArrayList<>();
            final DoubleStream failing = DoubleStream.of(1).onClose(() -> {
                throw new IllegalStateException("close failed");
            });
            final DoubleStream concat = overload == 0 ? DoubleStream.concat(failing, DoubleStream.of(2, 3))
                    : DoubleStream.concat(Arrays.asList(failing, DoubleStream.of(2, 3)));
            final IllegalStateException e = assertThrows(IllegalStateException.class, () -> concat.forEach(seen::add));
            assertEquals("close failed", e.getMessage());
            assertEquals(Arrays.asList(1.0), seen); // the elements of the remaining input are not delivered
            assertFalse(seen.contains(2.0));
        }
    }

    @Test
    public void h102_floatConcatClosesEachExhaustedInputAndACloseFailureEndsTheTraversal() {
        for (int overload = 0; overload < 2; overload++) {
            final AtomicInteger aClosed = new AtomicInteger();
            final FloatStream a = FloatStream.of(1, 2).onClose(aClosed::incrementAndGet);
            final FloatStream b = FloatStream.of(3, 4);
            final FloatStream s = overload == 0 ? FloatStream.concat(a, b) : FloatStream.concat(Arrays.asList(a, b));
            final FloatIterator it = s.iterator();
            assertEquals(1.0f, it.nextFloat());
            assertEquals(2.0f, it.nextFloat());
            assertEquals(0, aClosed.get());
            assertTrue(it.hasNext()); // moving on to b closes the exhausted a
            assertEquals(1, aClosed.get());
            assertEquals(3.0f, it.nextFloat());
            s.close();
            assertEquals(1, aClosed.get());
        }

        for (int overload = 0; overload < 2; overload++) {
            final List<Float> seen = new ArrayList<>();
            final FloatStream failing = FloatStream.of(1).onClose(() -> {
                throw new IllegalStateException("close failed");
            });
            final FloatStream concat = overload == 0 ? FloatStream.concat(failing, FloatStream.of(2, 3))
                    : FloatStream.concat(Arrays.asList(failing, FloatStream.of(2, 3)));
            final IllegalStateException e = assertThrows(IllegalStateException.class, () -> concat.forEach(seen::add));
            assertEquals("close failed", e.getMessage());
            assertEquals(Arrays.asList(1.0f), seen); // the elements of the remaining input are not delivered
        }
    }
}
