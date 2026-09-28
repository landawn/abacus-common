package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.ShortIterator;

/**
 * Review fixes 2026-09-25 (fixer F11b, tests only): LST/C-114 close-failure precedence pinned over EVERY sequential terminal
 * of the seven primitive families, for each implementation that carries its own {@code catch (Throwable) { closeAfterFailure }}
 * block (U28-02: the Abstract*Stream terminals first/last/onlyOne/percentiles/summaryStatisticsAndPercentiles/join/joinTo and
 * Float/Double sum/average; U29-02: the Array*Stream terminals, which fail only through the caller's callback, a null-returning
 * factory, onlyOne() or a bare argument check; U30-03: the 22 Iterator*Stream terminals), plus U31-03: the parallel
 * zipWith(closed argument) and collect(null container) sites keep their primary ISE/NPE over a failing receiver close handler,
 * and an already-closed zip argument's handlers are not re-run.
 * <p>
 * Every case asserts: the primary failure propagates, {@code getSuppressed()[0]} is the close failure, the close handler ran
 * exactly once and the stream is closed afterwards. The behaviour pinned here is the CURRENT one (verified by the review probes
 * U28/p2, U29/p1, U30/p2 and U31/p1); the tests exist so that a lost catch block in a later merge is detected.
 */
public class PrimitiveStreamsCloseFailureReview20260925Test extends TestBase {

    private static final class Boom extends RuntimeException {
        private static final long serialVersionUID = 1L;

        Boom(final String message) {
            super(message);
        }
    }

    /** Fails on the element 2 (so that a 3-element source fails in the middle of the terminal's loop). */
    private static int failOn2(final double x) {
        if (x == 2) {
            throw new Boom("up boom");
        }
        return (int) x;
    }

    /** Fails on every element (so that first() and friends fail on their very first pull). */
    private static int boom(final double x) {
        throw new Boom("up boom");
    }

    private static final class Term<S> {
        final String name;
        final Consumer<S> op;
        final Class<? extends Throwable> expected;
        final String messagePart;

        Term(final String name, final Consumer<S> op, final Class<? extends Throwable> expected, final String messagePart) {
            this.name = name;
            this.op = op;
            this.expected = expected;
            this.messagePart = messagePart;
        }
    }

    private static final class Terms<S> {
        final List<Term<S>> list = new ArrayList<>();

        Terms<S> up(final String name, final Consumer<S> op) {
            list.add(new Term<>(name, op, Boom.class, "up boom"));
            return this;
        }

        Terms<S> expect(final String name, final Consumer<S> op, final Class<? extends Throwable> expected, final String messagePart) {
            list.add(new Term<>(name, op, expected, messagePart));
            return this;
        }
    }

    /**
     * For every terminal: the terminal's own failure wins, the close failure is suppressed onto it exactly once, the close
     * handler ran exactly once, and the stream is closed afterwards.
     */
    private static <S extends BaseStream<?, ?, ?, ?, ?, ?, ?, S>> void assertPrimaryWins(final String family, final String sourceName,
            final Supplier<S> source, final Terms<S> terms) {
        for (final Term<S> t : terms.list) {
            final String label = family + " " + sourceName + " " + t.name;
            final Boom closeFailure = new Boom("closeX");
            final AtomicInteger closes = new AtomicInteger();
            final S s = source.get().onClose(() -> {
                closes.incrementAndGet();
                throw closeFailure;
            });

            final Throwable thrown = assertThrows(Throwable.class, () -> t.op.accept(s), label);
            assertTrue(t.expected.isInstance(thrown), label + ": " + thrown);
            if (t.messagePart != null) {
                assertTrue(thrown.getMessage() != null && thrown.getMessage().contains(t.messagePart), label + ": " + thrown.getMessage());
            }
            assertEquals(1, thrown.getSuppressed().length, label + ": suppressed " + Arrays.toString(thrown.getSuppressed()));
            assertSame(closeFailure, thrown.getSuppressed()[0], label);
            assertEquals(1, closes.get(), label + ": close handler runs");
            assertThrows(IllegalStateException.class, s::count, label + ": the stream must be closed");
        }
    }

    /** A SUCCESSFUL terminal still surfaces the close failure alone (nothing is swallowed), and the handler ran once. */
    private static <S extends BaseStream<?, ?, ?, ?, ?, ?, ?, S>> void assertCloseFailureSurfacesOnSuccess(final String family, final Supplier<S> source,
            final Terms<S> terms) {
        for (final Term<S> t : terms.list) {
            final String label = family + " success path " + t.name;
            final AtomicInteger closes = new AtomicInteger();
            final S s = source.get().onClose(() -> {
                closes.incrementAndGet();
                throw new Boom("closeX");
            });

            final Boom thrown = assertThrows(Boom.class, () -> t.op.accept(s), label);
            assertEquals("closeX", thrown.getMessage(), label);
            assertEquals(0, thrown.getSuppressed().length, label);
            assertEquals(1, closes.get(), label);
        }
    }

    private static BaseStream.ParallelSettings ps(final BaseStream.SplitStrategy strategy) {
        return BaseStream.ParallelSettings.builder().splitStrategy(strategy).maxThreadNum(4).build();
    }

    // ============================================================================================================
    // Byte
    // ============================================================================================================

    /** U28-02: the AbstractByteStream terminals (the source fails on its first pull, so first() fails too). */
    private static Terms<ByteStream> byteAbstractTerminals() {
        return new Terms<ByteStream>().up("first", s -> s.first())
                .up("last", s -> s.last())
                .up("onlyOne", s -> s.onlyOne())
                .up("percentiles", s -> s.percentiles())
                .up("summaryStatisticsAndPercentiles", s -> s.summaryStatisticsAndPercentiles())
                .up("join", s -> s.join(",", "[", "]"))
                .up("joinTo", s -> s.joinTo(Joiner.with(",")));
    }

    /** U30-03: the 22 sequential terminals of IteratorByteStream (the source itself fails on its 2nd element). */
    private static Terms<ByteStream> byteIteratorTerminals() {
        return new Terms<ByteStream>().up("forEach", s -> s.forEach(x -> {
        }))
                .up("toArray", s -> s.toArray())
                .up("toList", s -> s.toList())
                .up("toCollection", s -> s.toCollection(ArrayList::new))
                .up("toMultiset", s -> s.toMultiset(Multiset::new))
                .up("toMap", s -> s.toMap(x -> x, x -> x, (a, b) -> a, () -> new HashMap<>()))
                .up("groupTo", s -> s.groupTo(x -> x, Collectors.counting(), () -> new HashMap<>()))
                .up("reduce(identity)", s -> s.reduce((byte) 0, (a, b) -> (byte) (a + b)))
                .up("reduce", s -> s.reduce((a, b) -> (byte) (a + b)))
                .up("collect", s -> s.collect(ArrayList::new, (c, x) -> c.add(x), (a, b) -> a.addAll(b)))
                .up("min", s -> s.min())
                .up("max", s -> s.max())
                .up("kthLargest", s -> s.kthLargest(1))
                .up("sum", s -> s.sum())
                .up("average", s -> s.average())
                .up("count", s -> s.count())
                .up("summaryStatistics", s -> s.summaryStatistics())
                .up("anyMatch", s -> s.anyMatch(x -> false))
                .up("allMatch", s -> s.allMatch(x -> true))
                .up("noneMatch", s -> s.noneMatch(x -> false))
                .up("findFirst", s -> s.findFirst(x -> false))
                .up("findLast", s -> s.findLast(x -> true));
    }

    /** U29-02: the ArrayByteStream terminals fail only through the caller's callback, a null factory, onlyOne() or a bare check. */
    private static Terms<ByteStream> byteArrayTerminals() {
        return new Terms<ByteStream>().up("forEach(action)", s -> s.forEach(x -> failOn2(x)))
                .up("anyMatch(predicate)", s -> s.anyMatch(x -> failOn2(x) < 0))
                .up("allMatch(predicate)", s -> s.allMatch(x -> failOn2(x) > 0))
                .up("noneMatch(predicate)", s -> s.noneMatch(x -> failOn2(x) < 0))
                .up("findFirst(predicate)", s -> s.findFirst(x -> failOn2(x) < 0))
                .up("findLast(predicate)", s -> s.findLast(x -> failOn2(x) < 0))
                .up("reduce(identity, accumulator)", s -> s.reduce((byte) 0, (a, b) -> (byte) failOn2(b)))
                .up("reduce(accumulator)", s -> s.reduce((a, b) -> (byte) failOn2(b)))
                .up("toMap(keyMapper)", s -> s.toMap(x -> failOn2(x), x -> x, (a, b) -> a, () -> new HashMap<>()))
                .up("groupTo(keyMapper)", s -> s.groupTo(x -> failOn2(x), Collectors.counting(), () -> new HashMap<>()))
                .up("collect(accumulator)", s -> s.collect(ArrayList::new, (c, x) -> c.add(failOn2(x)), (a, b) -> a.addAll(b)))
                .expect("onlyOne(2+ elements)", s -> s.onlyOne(), TooManyElementsException.class, null)
                .expect("elementAt(-1) bare check", s -> s.elementAt(-1), IllegalArgumentException.class, null)
                .expect("kthLargest(0) bare check", s -> s.kthLargest(0), IllegalArgumentException.class, null)
                .expect("toCollection(null) bare check", s -> s.toCollection(null), IllegalArgumentException.class, null);
    }

    /** C-016/C-101 null factories: NPE "... returned null" wins over the close failure (also on an EMPTY array range). */
    private static Terms<ByteStream> byteNullFactoryTerminals() {
        return new Terms<ByteStream>().expect("toCollection(null supplier)", s -> s.toCollection(() -> null), NullPointerException.class, "supplier returned null")
                .expect("toMultiset(null supplier)", s -> s.toMultiset(() -> null), NullPointerException.class, "supplier returned null")
                .expect("toMap(null mapFactory)", s -> s.toMap(x -> x, x -> x, (a, b) -> a, () -> null), NullPointerException.class, "mapFactory returned null")
                .expect("groupTo(null mapFactory)", s -> s.groupTo(x -> x, Collectors.counting(), () -> null), NullPointerException.class,
                        "mapFactory returned null")
                .expect("collect(null supplier)", s -> s.collect(() -> null, (c, x) -> {
                }, (a, b) -> {
                }), NullPointerException.class, "supplier returned null");
    }

    @Test
    public void u28_byteAbstractTerminalsKeepThePrimaryFailure() {
        assertPrimaryWins("byte", "array.map(boom)", () -> ByteStream.of((byte) 1, (byte) 2, (byte) 3).map(x -> (byte) boom(x)), byteAbstractTerminals());
        assertPrimaryWins("byte", "iterator.map(boom)", () -> ByteStream.of(ByteIterator.of((byte) 1, (byte) 2, (byte) 3)).map(x -> (byte) boom(x)),
                byteAbstractTerminals());
        // onlyOne(): the TooManyElementsException raised INSIDE the try also wins over the close failure (Abstract + Array impl)
        assertPrimaryWins("byte", "iterator (2 elements)", () -> ByteStream.of(ByteIterator.of((byte) 1, (byte) 2)),
                new Terms<ByteStream>().expect("onlyOne", s -> s.onlyOne(), TooManyElementsException.class, null));
        assertPrimaryWins("byte", "array (2 elements)", () -> ByteStream.of((byte) 1, (byte) 2),
                new Terms<ByteStream>().expect("onlyOne", s -> s.onlyOne(), TooManyElementsException.class, null));
        // a successful terminal still surfaces the close failure alone
        assertCloseFailureSurfacesOnSuccess("byte", () -> ByteStream.of((byte) 7), byteAbstractTerminals());
        assertCloseFailureSurfacesOnSuccess("byte", () -> ByteStream.of(ByteIterator.of((byte) 7)), byteAbstractTerminals());
    }

    @Test
    public void u30_byteIteratorTerminalsKeepThePrimaryFailure() {
        assertPrimaryWins("byte", "iterator.map(failOn2)", () -> ByteStream.of(ByteIterator.of((byte) 1, (byte) 2, (byte) 3)).map(x -> (byte) failOn2(x)),
                byteIteratorTerminals());
        // the failure raised from hasNext() (filter predicate) instead of nextX()
        assertPrimaryWins("byte", "iterator.filter(failOn2)", () -> ByteStream.of(ByteIterator.of((byte) 1, (byte) 2, (byte) 3)).filter(x -> failOn2(x) > 0),
                byteIteratorTerminals());
        // null factories on iterator-backed sources, non-empty and empty
        assertPrimaryWins("byte", "iterator", () -> ByteStream.of(ByteIterator.of((byte) 1, (byte) 2, (byte) 3)), byteNullFactoryTerminals());
        assertPrimaryWins("byte", "empty iterator", () -> ByteStream.of(ByteIterator.of((byte) 1)).filter(x -> false), byteNullFactoryTerminals());
        // a successful terminal still surfaces the close failure alone
        assertCloseFailureSurfacesOnSuccess("byte", () -> ByteStream.of(ByteIterator.of((byte) 1, (byte) 2, (byte) 3)), byteIteratorTerminals());
    }

    @Test
    public void u29_byteArrayTerminalsKeepThePrimaryFailure() {
        assertPrimaryWins("byte", "array", () -> ByteStream.of((byte) 1, (byte) 2, (byte) 3), byteArrayTerminals());
        assertPrimaryWins("byte", "array sub-range", () -> ByteStream.of(new byte[] { 0, 1, 2, 3 }, 1, 3), byteArrayTerminals());
        assertPrimaryWins("byte", "array", () -> ByteStream.of((byte) 1, (byte) 2, (byte) 3), byteNullFactoryTerminals());
        assertPrimaryWins("byte", "array sub-range", () -> ByteStream.of(new byte[] { 0, 1, 2, 3 }, 1, 3), byteNullFactoryTerminals());
        assertPrimaryWins("byte", "empty array range", () -> ByteStream.of(new byte[] { 0, 1, 2, 3 }, 1, 1), byteNullFactoryTerminals());
        // a successful terminal still surfaces the close failure alone (the same 22 terminals, array-backed)
        assertCloseFailureSurfacesOnSuccess("byte", () -> ByteStream.of((byte) 1, (byte) 2, (byte) 3), byteIteratorTerminals());
    }

    @Test
    public void u31_byteParallelZipWithClosedArgumentKeepsThePrimaryOverACloseFailure() {
        for (int src = 0; src < 2; src++) {
            for (int overload = 0; overload < 4; overload++) {
                final String label = "byte src " + src + " overload " + overload;
                final Boom closeFailure = new Boom("closeX");
                final AtomicInteger aClosed = new AtomicInteger();
                final AtomicInteger bClosed = new AtomicInteger();
                final AtomicInteger cClosed = new AtomicInteger();
                // src 0: ParallelArrayByteStream, src 1: ParallelIteratorByteStream
                final ByteStream a = (src == 0 ? ByteStream.of(new byte[] { 1, 2, 3 }) : ByteStream.of(ByteIterator.of(new byte[] { 1, 2, 3 }))).parallel(3)
                        .onClose(() -> {
                            aClosed.incrementAndGet();
                            throw closeFailure;
                        });
                assertTrue(a instanceof ParallelArrayByteStream == (src == 0), label);
                final ByteStream b = ByteStream.of(new byte[] { 4, 5 }).onClose(bClosed::incrementAndGet);
                final ByteStream c = ByteStream.of(new byte[] { 4, 5 }).onClose(cClosed::incrementAndGet);
                // overloads 0/2: b is closed; overloads 1/3: b is open and c is closed
                final boolean cIsClosed = overload == 1 || overload == 3;
                (cIsClosed ? c : b).close();
                assertEquals(1, (cIsClosed ? cClosed : bClosed).get(), label);
                final int ov = overload;
                final IllegalStateException e = assertThrows(IllegalStateException.class, () -> {
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
                }, label);
                // the ISE of the closed argument is the primary; the receiver's close failure is suppressed onto it
                assertEquals("This stream is already terminated.", e.getMessage(), label);
                assertEquals(1, e.getSuppressed().length, label + ": suppressed " + Arrays.toString(e.getSuppressed()));
                assertSame(closeFailure, e.getSuppressed()[0], label);
                assertEquals(1, aClosed.get(), label + ": receiver close handler runs once");
                assertThrows(IllegalStateException.class, a::count, label + ": receiver closed");
                // the already-closed argument's handlers are NOT re-run (only opened sources are closed)
                assertEquals(1, (cIsClosed ? cClosed : bClosed).get(), label + ": closed argument not re-closed");
                if (cIsClosed) {
                    assertEquals(1, bClosed.get(), label + ": b was opened before c failed, so it is closed once");
                } else {
                    assertEquals(0, cClosed.get(), label + ": c is not involved");
                    assertEquals(2, c.count(), label + ": c stays usable");
                }
            }
        }
    }

    @Test
    public void u31_byteParallelCollectNullContainerKeepsTheNpeOverACloseFailure() {
        for (int kind = 0; kind < 3; kind++) {
            // 0: ParallelArrayByteStream / ARRAY split, 1: ParallelArrayByteStream / ITERATOR split, 2: ParallelIteratorByteStream
            final String label = "byte kind " + kind;
            final Boom closeFailure = new Boom("closeX");
            final AtomicInteger closes = new AtomicInteger();
            final ByteStream s = (kind == 2 ? ByteStream.of(ByteIterator.of(new byte[100])).parallel(4)
                    : ByteStream.of(new byte[100]).parallel(ps(kind == 0 ? BaseStream.SplitStrategy.ARRAY : BaseStream.SplitStrategy.ITERATOR))).onClose(() -> {
                        closes.incrementAndGet();
                        throw closeFailure;
                    });
            assertTrue(s instanceof ParallelArrayByteStream == (kind < 2), label);

            final NullPointerException e = assertThrows(NullPointerException.class, () -> s.collect(() -> null, (c, x) -> {
            }, (a, b) -> {
            }), label);
            assertEquals("supplier returned null", e.getMessage(), label);
            // other workers may add their own identical NPE as suppressed; the close failure must be among them, never the primary
            assertTrue(Arrays.stream(e.getSuppressed()).anyMatch(x -> x == closeFailure), label + ": suppressed " + Arrays.toString(e.getSuppressed()));
            assertEquals(1, Arrays.stream(e.getSuppressed()).filter(x -> x == closeFailure).count(), label);
            assertEquals(1, closes.get(), label + ": close handler runs once");
            assertThrows(IllegalStateException.class, s::count, label + ": closed");
        }
    }

    // ============================================================================================================
    // Char
    // ============================================================================================================

    /** U28-02: the AbstractCharStream terminals (the source fails on its first pull, so first() fails too). */
    private static Terms<CharStream> charAbstractTerminals() {
        return new Terms<CharStream>().up("first", s -> s.first())
                .up("last", s -> s.last())
                .up("onlyOne", s -> s.onlyOne())
                .up("percentiles", s -> s.percentiles())
                .up("summaryStatisticsAndPercentiles", s -> s.summaryStatisticsAndPercentiles())
                .up("join", s -> s.join(",", "[", "]"))
                .up("joinTo", s -> s.joinTo(Joiner.with(",")));
    }

    /** U30-03: the 22 sequential terminals of IteratorCharStream (the source itself fails on its 2nd element). */
    private static Terms<CharStream> charIteratorTerminals() {
        return new Terms<CharStream>().up("forEach", s -> s.forEach(x -> {
        }))
                .up("toArray", s -> s.toArray())
                .up("toList", s -> s.toList())
                .up("toCollection", s -> s.toCollection(ArrayList::new))
                .up("toMultiset", s -> s.toMultiset(Multiset::new))
                .up("toMap", s -> s.toMap(x -> x, x -> x, (a, b) -> a, () -> new HashMap<>()))
                .up("groupTo", s -> s.groupTo(x -> x, Collectors.counting(), () -> new HashMap<>()))
                .up("reduce(identity)", s -> s.reduce((char) 0, (a, b) -> (char) (a + b)))
                .up("reduce", s -> s.reduce((a, b) -> (char) (a + b)))
                .up("collect", s -> s.collect(ArrayList::new, (c, x) -> c.add(x), (a, b) -> a.addAll(b)))
                .up("min", s -> s.min())
                .up("max", s -> s.max())
                .up("kthLargest", s -> s.kthLargest(1))
                .up("sum", s -> s.sum())
                .up("average", s -> s.average())
                .up("count", s -> s.count())
                .up("summaryStatistics", s -> s.summaryStatistics())
                .up("anyMatch", s -> s.anyMatch(x -> false))
                .up("allMatch", s -> s.allMatch(x -> true))
                .up("noneMatch", s -> s.noneMatch(x -> false))
                .up("findFirst", s -> s.findFirst(x -> false))
                .up("findLast", s -> s.findLast(x -> true));
    }

    /** U29-02: the ArrayCharStream terminals fail only through the caller's callback, a null factory, onlyOne() or a bare check. */
    private static Terms<CharStream> charArrayTerminals() {
        return new Terms<CharStream>().up("forEach(action)", s -> s.forEach(x -> failOn2(x)))
                .up("anyMatch(predicate)", s -> s.anyMatch(x -> failOn2(x) < 0))
                .up("allMatch(predicate)", s -> s.allMatch(x -> failOn2(x) > 0))
                .up("noneMatch(predicate)", s -> s.noneMatch(x -> failOn2(x) < 0))
                .up("findFirst(predicate)", s -> s.findFirst(x -> failOn2(x) < 0))
                .up("findLast(predicate)", s -> s.findLast(x -> failOn2(x) < 0))
                .up("reduce(identity, accumulator)", s -> s.reduce((char) 0, (a, b) -> (char) failOn2(b)))
                .up("reduce(accumulator)", s -> s.reduce((a, b) -> (char) failOn2(b)))
                .up("toMap(keyMapper)", s -> s.toMap(x -> failOn2(x), x -> x, (a, b) -> a, () -> new HashMap<>()))
                .up("groupTo(keyMapper)", s -> s.groupTo(x -> failOn2(x), Collectors.counting(), () -> new HashMap<>()))
                .up("collect(accumulator)", s -> s.collect(ArrayList::new, (c, x) -> c.add(failOn2(x)), (a, b) -> a.addAll(b)))
                .expect("onlyOne(2+ elements)", s -> s.onlyOne(), TooManyElementsException.class, null)
                .expect("elementAt(-1) bare check", s -> s.elementAt(-1), IllegalArgumentException.class, null)
                .expect("kthLargest(0) bare check", s -> s.kthLargest(0), IllegalArgumentException.class, null)
                .expect("toCollection(null) bare check", s -> s.toCollection(null), IllegalArgumentException.class, null);
    }

    /** C-016/C-101 null factories: NPE "... returned null" wins over the close failure (also on an EMPTY array range). */
    private static Terms<CharStream> charNullFactoryTerminals() {
        return new Terms<CharStream>().expect("toCollection(null supplier)", s -> s.toCollection(() -> null), NullPointerException.class, "supplier returned null")
                .expect("toMultiset(null supplier)", s -> s.toMultiset(() -> null), NullPointerException.class, "supplier returned null")
                .expect("toMap(null mapFactory)", s -> s.toMap(x -> x, x -> x, (a, b) -> a, () -> null), NullPointerException.class, "mapFactory returned null")
                .expect("groupTo(null mapFactory)", s -> s.groupTo(x -> x, Collectors.counting(), () -> null), NullPointerException.class,
                        "mapFactory returned null")
                .expect("collect(null supplier)", s -> s.collect(() -> null, (c, x) -> {
                }, (a, b) -> {
                }), NullPointerException.class, "supplier returned null");
    }

    @Test
    public void u28_charAbstractTerminalsKeepThePrimaryFailure() {
        assertPrimaryWins("char", "array.map(boom)", () -> CharStream.of((char) 1, (char) 2, (char) 3).map(x -> (char) boom(x)), charAbstractTerminals());
        assertPrimaryWins("char", "iterator.map(boom)", () -> CharStream.of(CharIterator.of((char) 1, (char) 2, (char) 3)).map(x -> (char) boom(x)),
                charAbstractTerminals());
        // onlyOne(): the TooManyElementsException raised INSIDE the try also wins over the close failure (Abstract + Array impl)
        assertPrimaryWins("char", "iterator (2 elements)", () -> CharStream.of(CharIterator.of((char) 1, (char) 2)),
                new Terms<CharStream>().expect("onlyOne", s -> s.onlyOne(), TooManyElementsException.class, null));
        assertPrimaryWins("char", "array (2 elements)", () -> CharStream.of((char) 1, (char) 2),
                new Terms<CharStream>().expect("onlyOne", s -> s.onlyOne(), TooManyElementsException.class, null));
        // a successful terminal still surfaces the close failure alone
        assertCloseFailureSurfacesOnSuccess("char", () -> CharStream.of((char) 7), charAbstractTerminals());
        assertCloseFailureSurfacesOnSuccess("char", () -> CharStream.of(CharIterator.of((char) 7)), charAbstractTerminals());
    }

    @Test
    public void u30_charIteratorTerminalsKeepThePrimaryFailure() {
        assertPrimaryWins("char", "iterator.map(failOn2)", () -> CharStream.of(CharIterator.of((char) 1, (char) 2, (char) 3)).map(x -> (char) failOn2(x)),
                charIteratorTerminals());
        // the failure raised from hasNext() (filter predicate) instead of nextX()
        assertPrimaryWins("char", "iterator.filter(failOn2)", () -> CharStream.of(CharIterator.of((char) 1, (char) 2, (char) 3)).filter(x -> failOn2(x) > 0),
                charIteratorTerminals());
        // null factories on iterator-backed sources, non-empty and empty
        assertPrimaryWins("char", "iterator", () -> CharStream.of(CharIterator.of((char) 1, (char) 2, (char) 3)), charNullFactoryTerminals());
        assertPrimaryWins("char", "empty iterator", () -> CharStream.of(CharIterator.of((char) 1)).filter(x -> false), charNullFactoryTerminals());
        // a successful terminal still surfaces the close failure alone
        assertCloseFailureSurfacesOnSuccess("char", () -> CharStream.of(CharIterator.of((char) 1, (char) 2, (char) 3)), charIteratorTerminals());
    }

    @Test
    public void u29_charArrayTerminalsKeepThePrimaryFailure() {
        assertPrimaryWins("char", "array", () -> CharStream.of((char) 1, (char) 2, (char) 3), charArrayTerminals());
        assertPrimaryWins("char", "array sub-range", () -> CharStream.of(new char[] { 0, 1, 2, 3 }, 1, 3), charArrayTerminals());
        assertPrimaryWins("char", "array", () -> CharStream.of((char) 1, (char) 2, (char) 3), charNullFactoryTerminals());
        assertPrimaryWins("char", "array sub-range", () -> CharStream.of(new char[] { 0, 1, 2, 3 }, 1, 3), charNullFactoryTerminals());
        assertPrimaryWins("char", "empty array range", () -> CharStream.of(new char[] { 0, 1, 2, 3 }, 1, 1), charNullFactoryTerminals());
        // a successful terminal still surfaces the close failure alone (the same 22 terminals, array-backed)
        assertCloseFailureSurfacesOnSuccess("char", () -> CharStream.of((char) 1, (char) 2, (char) 3), charIteratorTerminals());
    }

    @Test
    public void u31_charParallelZipWithClosedArgumentKeepsThePrimaryOverACloseFailure() {
        for (int src = 0; src < 2; src++) {
            for (int overload = 0; overload < 4; overload++) {
                final String label = "char src " + src + " overload " + overload;
                final Boom closeFailure = new Boom("closeX");
                final AtomicInteger aClosed = new AtomicInteger();
                final AtomicInteger bClosed = new AtomicInteger();
                final AtomicInteger cClosed = new AtomicInteger();
                // src 0: ParallelArrayCharStream, src 1: ParallelIteratorCharStream
                final CharStream a = (src == 0 ? CharStream.of(new char[] { 1, 2, 3 }) : CharStream.of(CharIterator.of(new char[] { 1, 2, 3 }))).parallel(3)
                        .onClose(() -> {
                            aClosed.incrementAndGet();
                            throw closeFailure;
                        });
                assertTrue(a instanceof ParallelArrayCharStream == (src == 0), label);
                final CharStream b = CharStream.of(new char[] { 4, 5 }).onClose(bClosed::incrementAndGet);
                final CharStream c = CharStream.of(new char[] { 4, 5 }).onClose(cClosed::incrementAndGet);
                // overloads 0/2: b is closed; overloads 1/3: b is open and c is closed
                final boolean cIsClosed = overload == 1 || overload == 3;
                (cIsClosed ? c : b).close();
                assertEquals(1, (cIsClosed ? cClosed : bClosed).get(), label);
                final int ov = overload;
                final IllegalStateException e = assertThrows(IllegalStateException.class, () -> {
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
                }, label);
                // the ISE of the closed argument is the primary; the receiver's close failure is suppressed onto it
                assertEquals("This stream is already terminated.", e.getMessage(), label);
                assertEquals(1, e.getSuppressed().length, label + ": suppressed " + Arrays.toString(e.getSuppressed()));
                assertSame(closeFailure, e.getSuppressed()[0], label);
                assertEquals(1, aClosed.get(), label + ": receiver close handler runs once");
                assertThrows(IllegalStateException.class, a::count, label + ": receiver closed");
                // the already-closed argument's handlers are NOT re-run (only opened sources are closed)
                assertEquals(1, (cIsClosed ? cClosed : bClosed).get(), label + ": closed argument not re-closed");
                if (cIsClosed) {
                    assertEquals(1, bClosed.get(), label + ": b was opened before c failed, so it is closed once");
                } else {
                    assertEquals(0, cClosed.get(), label + ": c is not involved");
                    assertEquals(2, c.count(), label + ": c stays usable");
                }
            }
        }
    }

    @Test
    public void u31_charParallelCollectNullContainerKeepsTheNpeOverACloseFailure() {
        for (int kind = 0; kind < 3; kind++) {
            // 0: ParallelArrayCharStream / ARRAY split, 1: ParallelArrayCharStream / ITERATOR split, 2: ParallelIteratorCharStream
            final String label = "char kind " + kind;
            final Boom closeFailure = new Boom("closeX");
            final AtomicInteger closes = new AtomicInteger();
            final CharStream s = (kind == 2 ? CharStream.of(CharIterator.of(new char[100])).parallel(4)
                    : CharStream.of(new char[100]).parallel(ps(kind == 0 ? BaseStream.SplitStrategy.ARRAY : BaseStream.SplitStrategy.ITERATOR))).onClose(() -> {
                        closes.incrementAndGet();
                        throw closeFailure;
                    });
            assertTrue(s instanceof ParallelArrayCharStream == (kind < 2), label);

            final NullPointerException e = assertThrows(NullPointerException.class, () -> s.collect(() -> null, (c, x) -> {
            }, (a, b) -> {
            }), label);
            assertEquals("supplier returned null", e.getMessage(), label);
            // other workers may add their own identical NPE as suppressed; the close failure must be among them, never the primary
            assertTrue(Arrays.stream(e.getSuppressed()).anyMatch(x -> x == closeFailure), label + ": suppressed " + Arrays.toString(e.getSuppressed()));
            assertEquals(1, Arrays.stream(e.getSuppressed()).filter(x -> x == closeFailure).count(), label);
            assertEquals(1, closes.get(), label + ": close handler runs once");
            assertThrows(IllegalStateException.class, s::count, label + ": closed");
        }
    }

    // ============================================================================================================
    // Short
    // ============================================================================================================

    /** U28-02: the AbstractShortStream terminals (the source fails on its first pull, so first() fails too). */
    private static Terms<ShortStream> shortAbstractTerminals() {
        return new Terms<ShortStream>().up("first", s -> s.first())
                .up("last", s -> s.last())
                .up("onlyOne", s -> s.onlyOne())
                .up("percentiles", s -> s.percentiles())
                .up("summaryStatisticsAndPercentiles", s -> s.summaryStatisticsAndPercentiles())
                .up("join", s -> s.join(",", "[", "]"))
                .up("joinTo", s -> s.joinTo(Joiner.with(",")));
    }

    /** U30-03: the 22 sequential terminals of IteratorShortStream (the source itself fails on its 2nd element). */
    private static Terms<ShortStream> shortIteratorTerminals() {
        return new Terms<ShortStream>().up("forEach", s -> s.forEach(x -> {
        }))
                .up("toArray", s -> s.toArray())
                .up("toList", s -> s.toList())
                .up("toCollection", s -> s.toCollection(ArrayList::new))
                .up("toMultiset", s -> s.toMultiset(Multiset::new))
                .up("toMap", s -> s.toMap(x -> x, x -> x, (a, b) -> a, () -> new HashMap<>()))
                .up("groupTo", s -> s.groupTo(x -> x, Collectors.counting(), () -> new HashMap<>()))
                .up("reduce(identity)", s -> s.reduce((short) 0, (a, b) -> (short) (a + b)))
                .up("reduce", s -> s.reduce((a, b) -> (short) (a + b)))
                .up("collect", s -> s.collect(ArrayList::new, (c, x) -> c.add(x), (a, b) -> a.addAll(b)))
                .up("min", s -> s.min())
                .up("max", s -> s.max())
                .up("kthLargest", s -> s.kthLargest(1))
                .up("sum", s -> s.sum())
                .up("average", s -> s.average())
                .up("count", s -> s.count())
                .up("summaryStatistics", s -> s.summaryStatistics())
                .up("anyMatch", s -> s.anyMatch(x -> false))
                .up("allMatch", s -> s.allMatch(x -> true))
                .up("noneMatch", s -> s.noneMatch(x -> false))
                .up("findFirst", s -> s.findFirst(x -> false))
                .up("findLast", s -> s.findLast(x -> true));
    }

    /** U29-02: the ArrayShortStream terminals fail only through the caller's callback, a null factory, onlyOne() or a bare check. */
    private static Terms<ShortStream> shortArrayTerminals() {
        return new Terms<ShortStream>().up("forEach(action)", s -> s.forEach(x -> failOn2(x)))
                .up("anyMatch(predicate)", s -> s.anyMatch(x -> failOn2(x) < 0))
                .up("allMatch(predicate)", s -> s.allMatch(x -> failOn2(x) > 0))
                .up("noneMatch(predicate)", s -> s.noneMatch(x -> failOn2(x) < 0))
                .up("findFirst(predicate)", s -> s.findFirst(x -> failOn2(x) < 0))
                .up("findLast(predicate)", s -> s.findLast(x -> failOn2(x) < 0))
                .up("reduce(identity, accumulator)", s -> s.reduce((short) 0, (a, b) -> (short) failOn2(b)))
                .up("reduce(accumulator)", s -> s.reduce((a, b) -> (short) failOn2(b)))
                .up("toMap(keyMapper)", s -> s.toMap(x -> failOn2(x), x -> x, (a, b) -> a, () -> new HashMap<>()))
                .up("groupTo(keyMapper)", s -> s.groupTo(x -> failOn2(x), Collectors.counting(), () -> new HashMap<>()))
                .up("collect(accumulator)", s -> s.collect(ArrayList::new, (c, x) -> c.add(failOn2(x)), (a, b) -> a.addAll(b)))
                .expect("onlyOne(2+ elements)", s -> s.onlyOne(), TooManyElementsException.class, null)
                .expect("elementAt(-1) bare check", s -> s.elementAt(-1), IllegalArgumentException.class, null)
                .expect("kthLargest(0) bare check", s -> s.kthLargest(0), IllegalArgumentException.class, null)
                .expect("toCollection(null) bare check", s -> s.toCollection(null), IllegalArgumentException.class, null);
    }

    /** C-016/C-101 null factories: NPE "... returned null" wins over the close failure (also on an EMPTY array range). */
    private static Terms<ShortStream> shortNullFactoryTerminals() {
        return new Terms<ShortStream>().expect("toCollection(null supplier)", s -> s.toCollection(() -> null), NullPointerException.class, "supplier returned null")
                .expect("toMultiset(null supplier)", s -> s.toMultiset(() -> null), NullPointerException.class, "supplier returned null")
                .expect("toMap(null mapFactory)", s -> s.toMap(x -> x, x -> x, (a, b) -> a, () -> null), NullPointerException.class, "mapFactory returned null")
                .expect("groupTo(null mapFactory)", s -> s.groupTo(x -> x, Collectors.counting(), () -> null), NullPointerException.class,
                        "mapFactory returned null")
                .expect("collect(null supplier)", s -> s.collect(() -> null, (c, x) -> {
                }, (a, b) -> {
                }), NullPointerException.class, "supplier returned null");
    }

    @Test
    public void u28_shortAbstractTerminalsKeepThePrimaryFailure() {
        assertPrimaryWins("short", "array.map(boom)", () -> ShortStream.of((short) 1, (short) 2, (short) 3).map(x -> (short) boom(x)), shortAbstractTerminals());
        assertPrimaryWins("short", "iterator.map(boom)", () -> ShortStream.of(ShortIterator.of((short) 1, (short) 2, (short) 3)).map(x -> (short) boom(x)),
                shortAbstractTerminals());
        // onlyOne(): the TooManyElementsException raised INSIDE the try also wins over the close failure (Abstract + Array impl)
        assertPrimaryWins("short", "iterator (2 elements)", () -> ShortStream.of(ShortIterator.of((short) 1, (short) 2)),
                new Terms<ShortStream>().expect("onlyOne", s -> s.onlyOne(), TooManyElementsException.class, null));
        assertPrimaryWins("short", "array (2 elements)", () -> ShortStream.of((short) 1, (short) 2),
                new Terms<ShortStream>().expect("onlyOne", s -> s.onlyOne(), TooManyElementsException.class, null));
        // a successful terminal still surfaces the close failure alone
        assertCloseFailureSurfacesOnSuccess("short", () -> ShortStream.of((short) 7), shortAbstractTerminals());
        assertCloseFailureSurfacesOnSuccess("short", () -> ShortStream.of(ShortIterator.of((short) 7)), shortAbstractTerminals());
    }

    @Test
    public void u30_shortIteratorTerminalsKeepThePrimaryFailure() {
        assertPrimaryWins("short", "iterator.map(failOn2)", () -> ShortStream.of(ShortIterator.of((short) 1, (short) 2, (short) 3)).map(x -> (short) failOn2(x)),
                shortIteratorTerminals());
        // the failure raised from hasNext() (filter predicate) instead of nextX()
        assertPrimaryWins("short", "iterator.filter(failOn2)", () -> ShortStream.of(ShortIterator.of((short) 1, (short) 2, (short) 3)).filter(x -> failOn2(x) > 0),
                shortIteratorTerminals());
        // null factories on iterator-backed sources, non-empty and empty
        assertPrimaryWins("short", "iterator", () -> ShortStream.of(ShortIterator.of((short) 1, (short) 2, (short) 3)), shortNullFactoryTerminals());
        assertPrimaryWins("short", "empty iterator", () -> ShortStream.of(ShortIterator.of((short) 1)).filter(x -> false), shortNullFactoryTerminals());
        // a successful terminal still surfaces the close failure alone
        assertCloseFailureSurfacesOnSuccess("short", () -> ShortStream.of(ShortIterator.of((short) 1, (short) 2, (short) 3)), shortIteratorTerminals());
    }

    @Test
    public void u29_shortArrayTerminalsKeepThePrimaryFailure() {
        assertPrimaryWins("short", "array", () -> ShortStream.of((short) 1, (short) 2, (short) 3), shortArrayTerminals());
        assertPrimaryWins("short", "array sub-range", () -> ShortStream.of(new short[] { 0, 1, 2, 3 }, 1, 3), shortArrayTerminals());
        assertPrimaryWins("short", "array", () -> ShortStream.of((short) 1, (short) 2, (short) 3), shortNullFactoryTerminals());
        assertPrimaryWins("short", "array sub-range", () -> ShortStream.of(new short[] { 0, 1, 2, 3 }, 1, 3), shortNullFactoryTerminals());
        assertPrimaryWins("short", "empty array range", () -> ShortStream.of(new short[] { 0, 1, 2, 3 }, 1, 1), shortNullFactoryTerminals());
        // a successful terminal still surfaces the close failure alone (the same 22 terminals, array-backed)
        assertCloseFailureSurfacesOnSuccess("short", () -> ShortStream.of((short) 1, (short) 2, (short) 3), shortIteratorTerminals());
    }

    @Test
    public void u31_shortParallelZipWithClosedArgumentKeepsThePrimaryOverACloseFailure() {
        for (int src = 0; src < 2; src++) {
            for (int overload = 0; overload < 4; overload++) {
                final String label = "short src " + src + " overload " + overload;
                final Boom closeFailure = new Boom("closeX");
                final AtomicInteger aClosed = new AtomicInteger();
                final AtomicInteger bClosed = new AtomicInteger();
                final AtomicInteger cClosed = new AtomicInteger();
                // src 0: ParallelArrayShortStream, src 1: ParallelIteratorShortStream
                final ShortStream a = (src == 0 ? ShortStream.of(new short[] { 1, 2, 3 }) : ShortStream.of(ShortIterator.of(new short[] { 1, 2, 3 }))).parallel(3)
                        .onClose(() -> {
                            aClosed.incrementAndGet();
                            throw closeFailure;
                        });
                assertTrue(a instanceof ParallelArrayShortStream == (src == 0), label);
                final ShortStream b = ShortStream.of(new short[] { 4, 5 }).onClose(bClosed::incrementAndGet);
                final ShortStream c = ShortStream.of(new short[] { 4, 5 }).onClose(cClosed::incrementAndGet);
                // overloads 0/2: b is closed; overloads 1/3: b is open and c is closed
                final boolean cIsClosed = overload == 1 || overload == 3;
                (cIsClosed ? c : b).close();
                assertEquals(1, (cIsClosed ? cClosed : bClosed).get(), label);
                final int ov = overload;
                final IllegalStateException e = assertThrows(IllegalStateException.class, () -> {
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
                }, label);
                // the ISE of the closed argument is the primary; the receiver's close failure is suppressed onto it
                assertEquals("This stream is already terminated.", e.getMessage(), label);
                assertEquals(1, e.getSuppressed().length, label + ": suppressed " + Arrays.toString(e.getSuppressed()));
                assertSame(closeFailure, e.getSuppressed()[0], label);
                assertEquals(1, aClosed.get(), label + ": receiver close handler runs once");
                assertThrows(IllegalStateException.class, a::count, label + ": receiver closed");
                // the already-closed argument's handlers are NOT re-run (only opened sources are closed)
                assertEquals(1, (cIsClosed ? cClosed : bClosed).get(), label + ": closed argument not re-closed");
                if (cIsClosed) {
                    assertEquals(1, bClosed.get(), label + ": b was opened before c failed, so it is closed once");
                } else {
                    assertEquals(0, cClosed.get(), label + ": c is not involved");
                    assertEquals(2, c.count(), label + ": c stays usable");
                }
            }
        }
    }

    @Test
    public void u31_shortParallelCollectNullContainerKeepsTheNpeOverACloseFailure() {
        for (int kind = 0; kind < 3; kind++) {
            // 0: ParallelArrayShortStream / ARRAY split, 1: ParallelArrayShortStream / ITERATOR split, 2: ParallelIteratorShortStream
            final String label = "short kind " + kind;
            final Boom closeFailure = new Boom("closeX");
            final AtomicInteger closes = new AtomicInteger();
            final ShortStream s = (kind == 2 ? ShortStream.of(ShortIterator.of(new short[100])).parallel(4)
                    : ShortStream.of(new short[100]).parallel(ps(kind == 0 ? BaseStream.SplitStrategy.ARRAY : BaseStream.SplitStrategy.ITERATOR))).onClose(() -> {
                        closes.incrementAndGet();
                        throw closeFailure;
                    });
            assertTrue(s instanceof ParallelArrayShortStream == (kind < 2), label);

            final NullPointerException e = assertThrows(NullPointerException.class, () -> s.collect(() -> null, (c, x) -> {
            }, (a, b) -> {
            }), label);
            assertEquals("supplier returned null", e.getMessage(), label);
            // other workers may add their own identical NPE as suppressed; the close failure must be among them, never the primary
            assertTrue(Arrays.stream(e.getSuppressed()).anyMatch(x -> x == closeFailure), label + ": suppressed " + Arrays.toString(e.getSuppressed()));
            assertEquals(1, Arrays.stream(e.getSuppressed()).filter(x -> x == closeFailure).count(), label);
            assertEquals(1, closes.get(), label + ": close handler runs once");
            assertThrows(IllegalStateException.class, s::count, label + ": closed");
        }
    }

    // ============================================================================================================
    // Int
    // ============================================================================================================

    /** U28-02: the AbstractIntStream terminals (the source fails on its first pull, so first() fails too). */
    private static Terms<IntStream> intAbstractTerminals() {
        return new Terms<IntStream>().up("first", s -> s.first())
                .up("last", s -> s.last())
                .up("onlyOne", s -> s.onlyOne())
                .up("percentiles", s -> s.percentiles())
                .up("summaryStatisticsAndPercentiles", s -> s.summaryStatisticsAndPercentiles())
                .up("join", s -> s.join(",", "[", "]"))
                .up("joinTo", s -> s.joinTo(Joiner.with(",")));
    }

    /** U30-03: the 22 sequential terminals of IteratorIntStream (the source itself fails on its 2nd element). */
    private static Terms<IntStream> intIteratorTerminals() {
        return new Terms<IntStream>().up("forEach", s -> s.forEach(x -> {
        }))
                .up("toArray", s -> s.toArray())
                .up("toList", s -> s.toList())
                .up("toCollection", s -> s.toCollection(ArrayList::new))
                .up("toMultiset", s -> s.toMultiset(Multiset::new))
                .up("toMap", s -> s.toMap(x -> x, x -> x, (a, b) -> a, () -> new HashMap<>()))
                .up("groupTo", s -> s.groupTo(x -> x, Collectors.counting(), () -> new HashMap<>()))
                .up("reduce(identity)", s -> s.reduce((int) 0, (a, b) -> (int) (a + b)))
                .up("reduce", s -> s.reduce((a, b) -> (int) (a + b)))
                .up("collect", s -> s.collect(ArrayList::new, (c, x) -> c.add(x), (a, b) -> a.addAll(b)))
                .up("min", s -> s.min())
                .up("max", s -> s.max())
                .up("kthLargest", s -> s.kthLargest(1))
                .up("sum", s -> s.sum())
                .up("average", s -> s.average())
                .up("count", s -> s.count())
                .up("summaryStatistics", s -> s.summaryStatistics())
                .up("anyMatch", s -> s.anyMatch(x -> false))
                .up("allMatch", s -> s.allMatch(x -> true))
                .up("noneMatch", s -> s.noneMatch(x -> false))
                .up("findFirst", s -> s.findFirst(x -> false))
                .up("findLast", s -> s.findLast(x -> true));
    }

    /** U29-02: the ArrayIntStream terminals fail only through the caller's callback, a null factory, onlyOne() or a bare check. */
    private static Terms<IntStream> intArrayTerminals() {
        return new Terms<IntStream>().up("forEach(action)", s -> s.forEach(x -> failOn2(x)))
                .up("anyMatch(predicate)", s -> s.anyMatch(x -> failOn2(x) < 0))
                .up("allMatch(predicate)", s -> s.allMatch(x -> failOn2(x) > 0))
                .up("noneMatch(predicate)", s -> s.noneMatch(x -> failOn2(x) < 0))
                .up("findFirst(predicate)", s -> s.findFirst(x -> failOn2(x) < 0))
                .up("findLast(predicate)", s -> s.findLast(x -> failOn2(x) < 0))
                .up("reduce(identity, accumulator)", s -> s.reduce((int) 0, (a, b) -> (int) failOn2(b)))
                .up("reduce(accumulator)", s -> s.reduce((a, b) -> (int) failOn2(b)))
                .up("toMap(keyMapper)", s -> s.toMap(x -> failOn2(x), x -> x, (a, b) -> a, () -> new HashMap<>()))
                .up("groupTo(keyMapper)", s -> s.groupTo(x -> failOn2(x), Collectors.counting(), () -> new HashMap<>()))
                .up("collect(accumulator)", s -> s.collect(ArrayList::new, (c, x) -> c.add(failOn2(x)), (a, b) -> a.addAll(b)))
                .expect("onlyOne(2+ elements)", s -> s.onlyOne(), TooManyElementsException.class, null)
                .expect("elementAt(-1) bare check", s -> s.elementAt(-1), IllegalArgumentException.class, null)
                .expect("kthLargest(0) bare check", s -> s.kthLargest(0), IllegalArgumentException.class, null)
                .expect("toCollection(null) bare check", s -> s.toCollection(null), IllegalArgumentException.class, null);
    }

    /** C-016/C-101 null factories: NPE "... returned null" wins over the close failure (also on an EMPTY array range). */
    private static Terms<IntStream> intNullFactoryTerminals() {
        return new Terms<IntStream>().expect("toCollection(null supplier)", s -> s.toCollection(() -> null), NullPointerException.class, "supplier returned null")
                .expect("toMultiset(null supplier)", s -> s.toMultiset(() -> null), NullPointerException.class, "supplier returned null")
                .expect("toMap(null mapFactory)", s -> s.toMap(x -> x, x -> x, (a, b) -> a, () -> null), NullPointerException.class, "mapFactory returned null")
                .expect("groupTo(null mapFactory)", s -> s.groupTo(x -> x, Collectors.counting(), () -> null), NullPointerException.class,
                        "mapFactory returned null")
                .expect("collect(null supplier)", s -> s.collect(() -> null, (c, x) -> {
                }, (a, b) -> {
                }), NullPointerException.class, "supplier returned null");
    }

    @Test
    public void u28_intAbstractTerminalsKeepThePrimaryFailure() {
        assertPrimaryWins("int", "array.map(boom)", () -> IntStream.of((int) 1, (int) 2, (int) 3).map(x -> (int) boom(x)), intAbstractTerminals());
        assertPrimaryWins("int", "iterator.map(boom)", () -> IntStream.of(IntIterator.of((int) 1, (int) 2, (int) 3)).map(x -> (int) boom(x)),
                intAbstractTerminals());
        // onlyOne(): the TooManyElementsException raised INSIDE the try also wins over the close failure (Abstract + Array impl)
        assertPrimaryWins("int", "iterator (2 elements)", () -> IntStream.of(IntIterator.of((int) 1, (int) 2)),
                new Terms<IntStream>().expect("onlyOne", s -> s.onlyOne(), TooManyElementsException.class, null));
        assertPrimaryWins("int", "array (2 elements)", () -> IntStream.of((int) 1, (int) 2),
                new Terms<IntStream>().expect("onlyOne", s -> s.onlyOne(), TooManyElementsException.class, null));
        // a successful terminal still surfaces the close failure alone
        assertCloseFailureSurfacesOnSuccess("int", () -> IntStream.of((int) 7), intAbstractTerminals());
        assertCloseFailureSurfacesOnSuccess("int", () -> IntStream.of(IntIterator.of((int) 7)), intAbstractTerminals());
    }

    @Test
    public void u30_intIteratorTerminalsKeepThePrimaryFailure() {
        assertPrimaryWins("int", "iterator.map(failOn2)", () -> IntStream.of(IntIterator.of((int) 1, (int) 2, (int) 3)).map(x -> (int) failOn2(x)),
                intIteratorTerminals());
        // the failure raised from hasNext() (filter predicate) instead of nextX()
        assertPrimaryWins("int", "iterator.filter(failOn2)", () -> IntStream.of(IntIterator.of((int) 1, (int) 2, (int) 3)).filter(x -> failOn2(x) > 0),
                intIteratorTerminals());
        // null factories on iterator-backed sources, non-empty and empty
        assertPrimaryWins("int", "iterator", () -> IntStream.of(IntIterator.of((int) 1, (int) 2, (int) 3)), intNullFactoryTerminals());
        assertPrimaryWins("int", "empty iterator", () -> IntStream.of(IntIterator.of((int) 1)).filter(x -> false), intNullFactoryTerminals());
        // a successful terminal still surfaces the close failure alone
        assertCloseFailureSurfacesOnSuccess("int", () -> IntStream.of(IntIterator.of((int) 1, (int) 2, (int) 3)), intIteratorTerminals());
    }

    @Test
    public void u29_intArrayTerminalsKeepThePrimaryFailure() {
        assertPrimaryWins("int", "array", () -> IntStream.of((int) 1, (int) 2, (int) 3), intArrayTerminals());
        assertPrimaryWins("int", "array sub-range", () -> IntStream.of(new int[] { 0, 1, 2, 3 }, 1, 3), intArrayTerminals());
        assertPrimaryWins("int", "array", () -> IntStream.of((int) 1, (int) 2, (int) 3), intNullFactoryTerminals());
        assertPrimaryWins("int", "array sub-range", () -> IntStream.of(new int[] { 0, 1, 2, 3 }, 1, 3), intNullFactoryTerminals());
        assertPrimaryWins("int", "empty array range", () -> IntStream.of(new int[] { 0, 1, 2, 3 }, 1, 1), intNullFactoryTerminals());
        // a successful terminal still surfaces the close failure alone (the same 22 terminals, array-backed)
        assertCloseFailureSurfacesOnSuccess("int", () -> IntStream.of((int) 1, (int) 2, (int) 3), intIteratorTerminals());
    }

    @Test
    public void u31_intParallelZipWithClosedArgumentKeepsThePrimaryOverACloseFailure() {
        for (int src = 0; src < 2; src++) {
            for (int overload = 0; overload < 4; overload++) {
                final String label = "int src " + src + " overload " + overload;
                final Boom closeFailure = new Boom("closeX");
                final AtomicInteger aClosed = new AtomicInteger();
                final AtomicInteger bClosed = new AtomicInteger();
                final AtomicInteger cClosed = new AtomicInteger();
                // src 0: ParallelArrayIntStream, src 1: ParallelIteratorIntStream
                final IntStream a = (src == 0 ? IntStream.of(new int[] { 1, 2, 3 }) : IntStream.of(IntIterator.of(new int[] { 1, 2, 3 }))).parallel(3)
                        .onClose(() -> {
                            aClosed.incrementAndGet();
                            throw closeFailure;
                        });
                assertTrue(a instanceof ParallelArrayIntStream == (src == 0), label);
                final IntStream b = IntStream.of(new int[] { 4, 5 }).onClose(bClosed::incrementAndGet);
                final IntStream c = IntStream.of(new int[] { 4, 5 }).onClose(cClosed::incrementAndGet);
                // overloads 0/2: b is closed; overloads 1/3: b is open and c is closed
                final boolean cIsClosed = overload == 1 || overload == 3;
                (cIsClosed ? c : b).close();
                assertEquals(1, (cIsClosed ? cClosed : bClosed).get(), label);
                final int ov = overload;
                final IllegalStateException e = assertThrows(IllegalStateException.class, () -> {
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
                }, label);
                // the ISE of the closed argument is the primary; the receiver's close failure is suppressed onto it
                assertEquals("This stream is already terminated.", e.getMessage(), label);
                assertEquals(1, e.getSuppressed().length, label + ": suppressed " + Arrays.toString(e.getSuppressed()));
                assertSame(closeFailure, e.getSuppressed()[0], label);
                assertEquals(1, aClosed.get(), label + ": receiver close handler runs once");
                assertThrows(IllegalStateException.class, a::count, label + ": receiver closed");
                // the already-closed argument's handlers are NOT re-run (only opened sources are closed)
                assertEquals(1, (cIsClosed ? cClosed : bClosed).get(), label + ": closed argument not re-closed");
                if (cIsClosed) {
                    assertEquals(1, bClosed.get(), label + ": b was opened before c failed, so it is closed once");
                } else {
                    assertEquals(0, cClosed.get(), label + ": c is not involved");
                    assertEquals(2, c.count(), label + ": c stays usable");
                }
            }
        }
    }

    @Test
    public void u31_intParallelCollectNullContainerKeepsTheNpeOverACloseFailure() {
        for (int kind = 0; kind < 3; kind++) {
            // 0: ParallelArrayIntStream / ARRAY split, 1: ParallelArrayIntStream / ITERATOR split, 2: ParallelIteratorIntStream
            final String label = "int kind " + kind;
            final Boom closeFailure = new Boom("closeX");
            final AtomicInteger closes = new AtomicInteger();
            final IntStream s = (kind == 2 ? IntStream.of(IntIterator.of(new int[100])).parallel(4)
                    : IntStream.of(new int[100]).parallel(ps(kind == 0 ? BaseStream.SplitStrategy.ARRAY : BaseStream.SplitStrategy.ITERATOR))).onClose(() -> {
                        closes.incrementAndGet();
                        throw closeFailure;
                    });
            assertTrue(s instanceof ParallelArrayIntStream == (kind < 2), label);

            final NullPointerException e = assertThrows(NullPointerException.class, () -> s.collect(() -> null, (c, x) -> {
            }, (a, b) -> {
            }), label);
            assertEquals("supplier returned null", e.getMessage(), label);
            // other workers may add their own identical NPE as suppressed; the close failure must be among them, never the primary
            assertTrue(Arrays.stream(e.getSuppressed()).anyMatch(x -> x == closeFailure), label + ": suppressed " + Arrays.toString(e.getSuppressed()));
            assertEquals(1, Arrays.stream(e.getSuppressed()).filter(x -> x == closeFailure).count(), label);
            assertEquals(1, closes.get(), label + ": close handler runs once");
            assertThrows(IllegalStateException.class, s::count, label + ": closed");
        }
    }

    // ============================================================================================================
    // Long
    // ============================================================================================================

    /** U28-02: the AbstractLongStream terminals (the source fails on its first pull, so first() fails too). */
    private static Terms<LongStream> longAbstractTerminals() {
        return new Terms<LongStream>().up("first", s -> s.first())
                .up("last", s -> s.last())
                .up("onlyOne", s -> s.onlyOne())
                .up("percentiles", s -> s.percentiles())
                .up("summaryStatisticsAndPercentiles", s -> s.summaryStatisticsAndPercentiles())
                .up("join", s -> s.join(",", "[", "]"))
                .up("joinTo", s -> s.joinTo(Joiner.with(",")));
    }

    /** U30-03: the 22 sequential terminals of IteratorLongStream (the source itself fails on its 2nd element). */
    private static Terms<LongStream> longIteratorTerminals() {
        return new Terms<LongStream>().up("forEach", s -> s.forEach(x -> {
        }))
                .up("toArray", s -> s.toArray())
                .up("toList", s -> s.toList())
                .up("toCollection", s -> s.toCollection(ArrayList::new))
                .up("toMultiset", s -> s.toMultiset(Multiset::new))
                .up("toMap", s -> s.toMap(x -> x, x -> x, (a, b) -> a, () -> new HashMap<>()))
                .up("groupTo", s -> s.groupTo(x -> x, Collectors.counting(), () -> new HashMap<>()))
                .up("reduce(identity)", s -> s.reduce((long) 0, (a, b) -> (long) (a + b)))
                .up("reduce", s -> s.reduce((a, b) -> (long) (a + b)))
                .up("collect", s -> s.collect(ArrayList::new, (c, x) -> c.add(x), (a, b) -> a.addAll(b)))
                .up("min", s -> s.min())
                .up("max", s -> s.max())
                .up("kthLargest", s -> s.kthLargest(1))
                .up("sum", s -> s.sum())
                .up("average", s -> s.average())
                .up("count", s -> s.count())
                .up("summaryStatistics", s -> s.summaryStatistics())
                .up("anyMatch", s -> s.anyMatch(x -> false))
                .up("allMatch", s -> s.allMatch(x -> true))
                .up("noneMatch", s -> s.noneMatch(x -> false))
                .up("findFirst", s -> s.findFirst(x -> false))
                .up("findLast", s -> s.findLast(x -> true));
    }

    /** U29-02: the ArrayLongStream terminals fail only through the caller's callback, a null factory, onlyOne() or a bare check. */
    private static Terms<LongStream> longArrayTerminals() {
        return new Terms<LongStream>().up("forEach(action)", s -> s.forEach(x -> failOn2(x)))
                .up("anyMatch(predicate)", s -> s.anyMatch(x -> failOn2(x) < 0))
                .up("allMatch(predicate)", s -> s.allMatch(x -> failOn2(x) > 0))
                .up("noneMatch(predicate)", s -> s.noneMatch(x -> failOn2(x) < 0))
                .up("findFirst(predicate)", s -> s.findFirst(x -> failOn2(x) < 0))
                .up("findLast(predicate)", s -> s.findLast(x -> failOn2(x) < 0))
                .up("reduce(identity, accumulator)", s -> s.reduce((long) 0, (a, b) -> (long) failOn2(b)))
                .up("reduce(accumulator)", s -> s.reduce((a, b) -> (long) failOn2(b)))
                .up("toMap(keyMapper)", s -> s.toMap(x -> failOn2(x), x -> x, (a, b) -> a, () -> new HashMap<>()))
                .up("groupTo(keyMapper)", s -> s.groupTo(x -> failOn2(x), Collectors.counting(), () -> new HashMap<>()))
                .up("collect(accumulator)", s -> s.collect(ArrayList::new, (c, x) -> c.add(failOn2(x)), (a, b) -> a.addAll(b)))
                .expect("onlyOne(2+ elements)", s -> s.onlyOne(), TooManyElementsException.class, null)
                .expect("elementAt(-1) bare check", s -> s.elementAt(-1), IllegalArgumentException.class, null)
                .expect("kthLargest(0) bare check", s -> s.kthLargest(0), IllegalArgumentException.class, null)
                .expect("toCollection(null) bare check", s -> s.toCollection(null), IllegalArgumentException.class, null);
    }

    /** C-016/C-101 null factories: NPE "... returned null" wins over the close failure (also on an EMPTY array range). */
    private static Terms<LongStream> longNullFactoryTerminals() {
        return new Terms<LongStream>().expect("toCollection(null supplier)", s -> s.toCollection(() -> null), NullPointerException.class, "supplier returned null")
                .expect("toMultiset(null supplier)", s -> s.toMultiset(() -> null), NullPointerException.class, "supplier returned null")
                .expect("toMap(null mapFactory)", s -> s.toMap(x -> x, x -> x, (a, b) -> a, () -> null), NullPointerException.class, "mapFactory returned null")
                .expect("groupTo(null mapFactory)", s -> s.groupTo(x -> x, Collectors.counting(), () -> null), NullPointerException.class,
                        "mapFactory returned null")
                .expect("collect(null supplier)", s -> s.collect(() -> null, (c, x) -> {
                }, (a, b) -> {
                }), NullPointerException.class, "supplier returned null");
    }

    @Test
    public void u28_longAbstractTerminalsKeepThePrimaryFailure() {
        assertPrimaryWins("long", "array.map(boom)", () -> LongStream.of((long) 1, (long) 2, (long) 3).map(x -> (long) boom(x)), longAbstractTerminals());
        assertPrimaryWins("long", "iterator.map(boom)", () -> LongStream.of(LongIterator.of((long) 1, (long) 2, (long) 3)).map(x -> (long) boom(x)),
                longAbstractTerminals());
        // onlyOne(): the TooManyElementsException raised INSIDE the try also wins over the close failure (Abstract + Array impl)
        assertPrimaryWins("long", "iterator (2 elements)", () -> LongStream.of(LongIterator.of((long) 1, (long) 2)),
                new Terms<LongStream>().expect("onlyOne", s -> s.onlyOne(), TooManyElementsException.class, null));
        assertPrimaryWins("long", "array (2 elements)", () -> LongStream.of((long) 1, (long) 2),
                new Terms<LongStream>().expect("onlyOne", s -> s.onlyOne(), TooManyElementsException.class, null));
        // a successful terminal still surfaces the close failure alone
        assertCloseFailureSurfacesOnSuccess("long", () -> LongStream.of((long) 7), longAbstractTerminals());
        assertCloseFailureSurfacesOnSuccess("long", () -> LongStream.of(LongIterator.of((long) 7)), longAbstractTerminals());
    }

    @Test
    public void u30_longIteratorTerminalsKeepThePrimaryFailure() {
        assertPrimaryWins("long", "iterator.map(failOn2)", () -> LongStream.of(LongIterator.of((long) 1, (long) 2, (long) 3)).map(x -> (long) failOn2(x)),
                longIteratorTerminals());
        // the failure raised from hasNext() (filter predicate) instead of nextX()
        assertPrimaryWins("long", "iterator.filter(failOn2)", () -> LongStream.of(LongIterator.of((long) 1, (long) 2, (long) 3)).filter(x -> failOn2(x) > 0),
                longIteratorTerminals());
        // null factories on iterator-backed sources, non-empty and empty
        assertPrimaryWins("long", "iterator", () -> LongStream.of(LongIterator.of((long) 1, (long) 2, (long) 3)), longNullFactoryTerminals());
        assertPrimaryWins("long", "empty iterator", () -> LongStream.of(LongIterator.of((long) 1)).filter(x -> false), longNullFactoryTerminals());
        // a successful terminal still surfaces the close failure alone
        assertCloseFailureSurfacesOnSuccess("long", () -> LongStream.of(LongIterator.of((long) 1, (long) 2, (long) 3)), longIteratorTerminals());
    }

    @Test
    public void u29_longArrayTerminalsKeepThePrimaryFailure() {
        assertPrimaryWins("long", "array", () -> LongStream.of((long) 1, (long) 2, (long) 3), longArrayTerminals());
        assertPrimaryWins("long", "array sub-range", () -> LongStream.of(new long[] { 0, 1, 2, 3 }, 1, 3), longArrayTerminals());
        assertPrimaryWins("long", "array", () -> LongStream.of((long) 1, (long) 2, (long) 3), longNullFactoryTerminals());
        assertPrimaryWins("long", "array sub-range", () -> LongStream.of(new long[] { 0, 1, 2, 3 }, 1, 3), longNullFactoryTerminals());
        assertPrimaryWins("long", "empty array range", () -> LongStream.of(new long[] { 0, 1, 2, 3 }, 1, 1), longNullFactoryTerminals());
        // a successful terminal still surfaces the close failure alone (the same 22 terminals, array-backed)
        assertCloseFailureSurfacesOnSuccess("long", () -> LongStream.of((long) 1, (long) 2, (long) 3), longIteratorTerminals());
    }

    @Test
    public void u31_longParallelZipWithClosedArgumentKeepsThePrimaryOverACloseFailure() {
        for (int src = 0; src < 2; src++) {
            for (int overload = 0; overload < 4; overload++) {
                final String label = "long src " + src + " overload " + overload;
                final Boom closeFailure = new Boom("closeX");
                final AtomicInteger aClosed = new AtomicInteger();
                final AtomicInteger bClosed = new AtomicInteger();
                final AtomicInteger cClosed = new AtomicInteger();
                // src 0: ParallelArrayLongStream, src 1: ParallelIteratorLongStream
                final LongStream a = (src == 0 ? LongStream.of(new long[] { 1, 2, 3 }) : LongStream.of(LongIterator.of(new long[] { 1, 2, 3 }))).parallel(3)
                        .onClose(() -> {
                            aClosed.incrementAndGet();
                            throw closeFailure;
                        });
                assertTrue(a instanceof ParallelArrayLongStream == (src == 0), label);
                final LongStream b = LongStream.of(new long[] { 4, 5 }).onClose(bClosed::incrementAndGet);
                final LongStream c = LongStream.of(new long[] { 4, 5 }).onClose(cClosed::incrementAndGet);
                // overloads 0/2: b is closed; overloads 1/3: b is open and c is closed
                final boolean cIsClosed = overload == 1 || overload == 3;
                (cIsClosed ? c : b).close();
                assertEquals(1, (cIsClosed ? cClosed : bClosed).get(), label);
                final int ov = overload;
                final IllegalStateException e = assertThrows(IllegalStateException.class, () -> {
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
                }, label);
                // the ISE of the closed argument is the primary; the receiver's close failure is suppressed onto it
                assertEquals("This stream is already terminated.", e.getMessage(), label);
                assertEquals(1, e.getSuppressed().length, label + ": suppressed " + Arrays.toString(e.getSuppressed()));
                assertSame(closeFailure, e.getSuppressed()[0], label);
                assertEquals(1, aClosed.get(), label + ": receiver close handler runs once");
                assertThrows(IllegalStateException.class, a::count, label + ": receiver closed");
                // the already-closed argument's handlers are NOT re-run (only opened sources are closed)
                assertEquals(1, (cIsClosed ? cClosed : bClosed).get(), label + ": closed argument not re-closed");
                if (cIsClosed) {
                    assertEquals(1, bClosed.get(), label + ": b was opened before c failed, so it is closed once");
                } else {
                    assertEquals(0, cClosed.get(), label + ": c is not involved");
                    assertEquals(2, c.count(), label + ": c stays usable");
                }
            }
        }
    }

    @Test
    public void u31_longParallelCollectNullContainerKeepsTheNpeOverACloseFailure() {
        for (int kind = 0; kind < 3; kind++) {
            // 0: ParallelArrayLongStream / ARRAY split, 1: ParallelArrayLongStream / ITERATOR split, 2: ParallelIteratorLongStream
            final String label = "long kind " + kind;
            final Boom closeFailure = new Boom("closeX");
            final AtomicInteger closes = new AtomicInteger();
            final LongStream s = (kind == 2 ? LongStream.of(LongIterator.of(new long[100])).parallel(4)
                    : LongStream.of(new long[100]).parallel(ps(kind == 0 ? BaseStream.SplitStrategy.ARRAY : BaseStream.SplitStrategy.ITERATOR))).onClose(() -> {
                        closes.incrementAndGet();
                        throw closeFailure;
                    });
            assertTrue(s instanceof ParallelArrayLongStream == (kind < 2), label);

            final NullPointerException e = assertThrows(NullPointerException.class, () -> s.collect(() -> null, (c, x) -> {
            }, (a, b) -> {
            }), label);
            assertEquals("supplier returned null", e.getMessage(), label);
            // other workers may add their own identical NPE as suppressed; the close failure must be among them, never the primary
            assertTrue(Arrays.stream(e.getSuppressed()).anyMatch(x -> x == closeFailure), label + ": suppressed " + Arrays.toString(e.getSuppressed()));
            assertEquals(1, Arrays.stream(e.getSuppressed()).filter(x -> x == closeFailure).count(), label);
            assertEquals(1, closes.get(), label + ": close handler runs once");
            assertThrows(IllegalStateException.class, s::count, label + ": closed");
        }
    }

    // ============================================================================================================
    // Float
    // ============================================================================================================

    /** U28-02: the AbstractFloatStream terminals (the source fails on its first pull, so first() fails too). */
    private static Terms<FloatStream> floatAbstractTerminals() {
        return new Terms<FloatStream>().up("first", s -> s.first())
                .up("last", s -> s.last())
                .up("onlyOne", s -> s.onlyOne())
                .up("percentiles", s -> s.percentiles())
                .up("summaryStatisticsAndPercentiles", s -> s.summaryStatisticsAndPercentiles())
                .up("join", s -> s.join(",", "[", "]"))
                .up("joinTo", s -> s.joinTo(Joiner.with(",")))
                .up("sum", s -> s.sum())
                .up("average", s -> s.average());
    }

    /** U30-03: the 22 sequential terminals of IteratorFloatStream (the source itself fails on its 2nd element). */
    private static Terms<FloatStream> floatIteratorTerminals() {
        return new Terms<FloatStream>().up("forEach", s -> s.forEach(x -> {
        }))
                .up("toArray", s -> s.toArray())
                .up("toList", s -> s.toList())
                .up("toCollection", s -> s.toCollection(ArrayList::new))
                .up("toMultiset", s -> s.toMultiset(Multiset::new))
                .up("toMap", s -> s.toMap(x -> x, x -> x, (a, b) -> a, () -> new HashMap<>()))
                .up("groupTo", s -> s.groupTo(x -> x, Collectors.counting(), () -> new HashMap<>()))
                .up("reduce(identity)", s -> s.reduce((float) 0, (a, b) -> (float) (a + b)))
                .up("reduce", s -> s.reduce((a, b) -> (float) (a + b)))
                .up("collect", s -> s.collect(ArrayList::new, (c, x) -> c.add(x), (a, b) -> a.addAll(b)))
                .up("min", s -> s.min())
                .up("max", s -> s.max())
                .up("kthLargest", s -> s.kthLargest(1))
                .up("sum", s -> s.sum())
                .up("average", s -> s.average())
                .up("count", s -> s.count())
                .up("summaryStatistics", s -> s.summaryStatistics())
                .up("anyMatch", s -> s.anyMatch(x -> false))
                .up("allMatch", s -> s.allMatch(x -> true))
                .up("noneMatch", s -> s.noneMatch(x -> false))
                .up("findFirst", s -> s.findFirst(x -> false))
                .up("findLast", s -> s.findLast(x -> true));
    }

    /** U29-02: the ArrayFloatStream terminals fail only through the caller's callback, a null factory, onlyOne() or a bare check. */
    private static Terms<FloatStream> floatArrayTerminals() {
        return new Terms<FloatStream>().up("forEach(action)", s -> s.forEach(x -> failOn2(x)))
                .up("anyMatch(predicate)", s -> s.anyMatch(x -> failOn2(x) < 0))
                .up("allMatch(predicate)", s -> s.allMatch(x -> failOn2(x) > 0))
                .up("noneMatch(predicate)", s -> s.noneMatch(x -> failOn2(x) < 0))
                .up("findFirst(predicate)", s -> s.findFirst(x -> failOn2(x) < 0))
                .up("findLast(predicate)", s -> s.findLast(x -> failOn2(x) < 0))
                .up("reduce(identity, accumulator)", s -> s.reduce((float) 0, (a, b) -> (float) failOn2(b)))
                .up("reduce(accumulator)", s -> s.reduce((a, b) -> (float) failOn2(b)))
                .up("toMap(keyMapper)", s -> s.toMap(x -> failOn2(x), x -> x, (a, b) -> a, () -> new HashMap<>()))
                .up("groupTo(keyMapper)", s -> s.groupTo(x -> failOn2(x), Collectors.counting(), () -> new HashMap<>()))
                .up("collect(accumulator)", s -> s.collect(ArrayList::new, (c, x) -> c.add(failOn2(x)), (a, b) -> a.addAll(b)))
                .expect("onlyOne(2+ elements)", s -> s.onlyOne(), TooManyElementsException.class, null)
                .expect("elementAt(-1) bare check", s -> s.elementAt(-1), IllegalArgumentException.class, null)
                .expect("kthLargest(0) bare check", s -> s.kthLargest(0), IllegalArgumentException.class, null)
                .expect("toCollection(null) bare check", s -> s.toCollection(null), IllegalArgumentException.class, null);
    }

    /** C-016/C-101 null factories: NPE "... returned null" wins over the close failure (also on an EMPTY array range). */
    private static Terms<FloatStream> floatNullFactoryTerminals() {
        return new Terms<FloatStream>().expect("toCollection(null supplier)", s -> s.toCollection(() -> null), NullPointerException.class, "supplier returned null")
                .expect("toMultiset(null supplier)", s -> s.toMultiset(() -> null), NullPointerException.class, "supplier returned null")
                .expect("toMap(null mapFactory)", s -> s.toMap(x -> x, x -> x, (a, b) -> a, () -> null), NullPointerException.class, "mapFactory returned null")
                .expect("groupTo(null mapFactory)", s -> s.groupTo(x -> x, Collectors.counting(), () -> null), NullPointerException.class,
                        "mapFactory returned null")
                .expect("collect(null supplier)", s -> s.collect(() -> null, (c, x) -> {
                }, (a, b) -> {
                }), NullPointerException.class, "supplier returned null");
    }

    @Test
    public void u28_floatAbstractTerminalsKeepThePrimaryFailure() {
        assertPrimaryWins("float", "array.map(boom)", () -> FloatStream.of((float) 1, (float) 2, (float) 3).map(x -> (float) boom(x)), floatAbstractTerminals());
        assertPrimaryWins("float", "iterator.map(boom)", () -> FloatStream.of(FloatIterator.of((float) 1, (float) 2, (float) 3)).map(x -> (float) boom(x)),
                floatAbstractTerminals());
        // onlyOne(): the TooManyElementsException raised INSIDE the try also wins over the close failure (Abstract + Array impl)
        assertPrimaryWins("float", "iterator (2 elements)", () -> FloatStream.of(FloatIterator.of((float) 1, (float) 2)),
                new Terms<FloatStream>().expect("onlyOne", s -> s.onlyOne(), TooManyElementsException.class, null));
        assertPrimaryWins("float", "array (2 elements)", () -> FloatStream.of((float) 1, (float) 2),
                new Terms<FloatStream>().expect("onlyOne", s -> s.onlyOne(), TooManyElementsException.class, null));
        // a successful terminal still surfaces the close failure alone
        assertCloseFailureSurfacesOnSuccess("float", () -> FloatStream.of((float) 7), floatAbstractTerminals());
        assertCloseFailureSurfacesOnSuccess("float", () -> FloatStream.of(FloatIterator.of((float) 7)), floatAbstractTerminals());
    }

    @Test
    public void u30_floatIteratorTerminalsKeepThePrimaryFailure() {
        assertPrimaryWins("float", "iterator.map(failOn2)", () -> FloatStream.of(FloatIterator.of((float) 1, (float) 2, (float) 3)).map(x -> (float) failOn2(x)),
                floatIteratorTerminals());
        // the failure raised from hasNext() (filter predicate) instead of nextX()
        assertPrimaryWins("float", "iterator.filter(failOn2)", () -> FloatStream.of(FloatIterator.of((float) 1, (float) 2, (float) 3)).filter(x -> failOn2(x) > 0),
                floatIteratorTerminals());
        // null factories on iterator-backed sources, non-empty and empty
        assertPrimaryWins("float", "iterator", () -> FloatStream.of(FloatIterator.of((float) 1, (float) 2, (float) 3)), floatNullFactoryTerminals());
        assertPrimaryWins("float", "empty iterator", () -> FloatStream.of(FloatIterator.of((float) 1)).filter(x -> false), floatNullFactoryTerminals());
        // a successful terminal still surfaces the close failure alone
        assertCloseFailureSurfacesOnSuccess("float", () -> FloatStream.of(FloatIterator.of((float) 1, (float) 2, (float) 3)), floatIteratorTerminals());
    }

    @Test
    public void u29_floatArrayTerminalsKeepThePrimaryFailure() {
        assertPrimaryWins("float", "array", () -> FloatStream.of((float) 1, (float) 2, (float) 3), floatArrayTerminals());
        assertPrimaryWins("float", "array sub-range", () -> FloatStream.of(new float[] { 0, 1, 2, 3 }, 1, 3), floatArrayTerminals());
        assertPrimaryWins("float", "array", () -> FloatStream.of((float) 1, (float) 2, (float) 3), floatNullFactoryTerminals());
        assertPrimaryWins("float", "array sub-range", () -> FloatStream.of(new float[] { 0, 1, 2, 3 }, 1, 3), floatNullFactoryTerminals());
        assertPrimaryWins("float", "empty array range", () -> FloatStream.of(new float[] { 0, 1, 2, 3 }, 1, 1), floatNullFactoryTerminals());
        // a successful terminal still surfaces the close failure alone (the same 22 terminals, array-backed)
        assertCloseFailureSurfacesOnSuccess("float", () -> FloatStream.of((float) 1, (float) 2, (float) 3), floatIteratorTerminals());
    }

    @Test
    public void u31_floatParallelZipWithClosedArgumentKeepsThePrimaryOverACloseFailure() {
        for (int src = 0; src < 2; src++) {
            for (int overload = 0; overload < 4; overload++) {
                final String label = "float src " + src + " overload " + overload;
                final Boom closeFailure = new Boom("closeX");
                final AtomicInteger aClosed = new AtomicInteger();
                final AtomicInteger bClosed = new AtomicInteger();
                final AtomicInteger cClosed = new AtomicInteger();
                // src 0: ParallelArrayFloatStream, src 1: ParallelIteratorFloatStream
                final FloatStream a = (src == 0 ? FloatStream.of(new float[] { 1, 2, 3 }) : FloatStream.of(FloatIterator.of(new float[] { 1, 2, 3 }))).parallel(3)
                        .onClose(() -> {
                            aClosed.incrementAndGet();
                            throw closeFailure;
                        });
                assertTrue(a instanceof ParallelArrayFloatStream == (src == 0), label);
                final FloatStream b = FloatStream.of(new float[] { 4, 5 }).onClose(bClosed::incrementAndGet);
                final FloatStream c = FloatStream.of(new float[] { 4, 5 }).onClose(cClosed::incrementAndGet);
                // overloads 0/2: b is closed; overloads 1/3: b is open and c is closed
                final boolean cIsClosed = overload == 1 || overload == 3;
                (cIsClosed ? c : b).close();
                assertEquals(1, (cIsClosed ? cClosed : bClosed).get(), label);
                final int ov = overload;
                final IllegalStateException e = assertThrows(IllegalStateException.class, () -> {
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
                }, label);
                // the ISE of the closed argument is the primary; the receiver's close failure is suppressed onto it
                assertEquals("This stream is already terminated.", e.getMessage(), label);
                assertEquals(1, e.getSuppressed().length, label + ": suppressed " + Arrays.toString(e.getSuppressed()));
                assertSame(closeFailure, e.getSuppressed()[0], label);
                assertEquals(1, aClosed.get(), label + ": receiver close handler runs once");
                assertThrows(IllegalStateException.class, a::count, label + ": receiver closed");
                // the already-closed argument's handlers are NOT re-run (only opened sources are closed)
                assertEquals(1, (cIsClosed ? cClosed : bClosed).get(), label + ": closed argument not re-closed");
                if (cIsClosed) {
                    assertEquals(1, bClosed.get(), label + ": b was opened before c failed, so it is closed once");
                } else {
                    assertEquals(0, cClosed.get(), label + ": c is not involved");
                    assertEquals(2, c.count(), label + ": c stays usable");
                }
            }
        }
    }

    @Test
    public void u31_floatParallelCollectNullContainerKeepsTheNpeOverACloseFailure() {
        for (int kind = 0; kind < 3; kind++) {
            // 0: ParallelArrayFloatStream / ARRAY split, 1: ParallelArrayFloatStream / ITERATOR split, 2: ParallelIteratorFloatStream
            final String label = "float kind " + kind;
            final Boom closeFailure = new Boom("closeX");
            final AtomicInteger closes = new AtomicInteger();
            final FloatStream s = (kind == 2 ? FloatStream.of(FloatIterator.of(new float[100])).parallel(4)
                    : FloatStream.of(new float[100]).parallel(ps(kind == 0 ? BaseStream.SplitStrategy.ARRAY : BaseStream.SplitStrategy.ITERATOR))).onClose(() -> {
                        closes.incrementAndGet();
                        throw closeFailure;
                    });
            assertTrue(s instanceof ParallelArrayFloatStream == (kind < 2), label);

            final NullPointerException e = assertThrows(NullPointerException.class, () -> s.collect(() -> null, (c, x) -> {
            }, (a, b) -> {
            }), label);
            assertEquals("supplier returned null", e.getMessage(), label);
            // other workers may add their own identical NPE as suppressed; the close failure must be among them, never the primary
            assertTrue(Arrays.stream(e.getSuppressed()).anyMatch(x -> x == closeFailure), label + ": suppressed " + Arrays.toString(e.getSuppressed()));
            assertEquals(1, Arrays.stream(e.getSuppressed()).filter(x -> x == closeFailure).count(), label);
            assertEquals(1, closes.get(), label + ": close handler runs once");
            assertThrows(IllegalStateException.class, s::count, label + ": closed");
        }
    }

    // ============================================================================================================
    // Double
    // ============================================================================================================

    /** U28-02: the AbstractDoubleStream terminals (the source fails on its first pull, so first() fails too). */
    private static Terms<DoubleStream> doubleAbstractTerminals() {
        return new Terms<DoubleStream>().up("first", s -> s.first())
                .up("last", s -> s.last())
                .up("onlyOne", s -> s.onlyOne())
                .up("percentiles", s -> s.percentiles())
                .up("summaryStatisticsAndPercentiles", s -> s.summaryStatisticsAndPercentiles())
                .up("join", s -> s.join(",", "[", "]"))
                .up("joinTo", s -> s.joinTo(Joiner.with(",")))
                .up("sum", s -> s.sum())
                .up("average", s -> s.average());
    }

    /** U30-03: the 22 sequential terminals of IteratorDoubleStream (the source itself fails on its 2nd element). */
    private static Terms<DoubleStream> doubleIteratorTerminals() {
        return new Terms<DoubleStream>().up("forEach", s -> s.forEach(x -> {
        }))
                .up("toArray", s -> s.toArray())
                .up("toList", s -> s.toList())
                .up("toCollection", s -> s.toCollection(ArrayList::new))
                .up("toMultiset", s -> s.toMultiset(Multiset::new))
                .up("toMap", s -> s.toMap(x -> x, x -> x, (a, b) -> a, () -> new HashMap<>()))
                .up("groupTo", s -> s.groupTo(x -> x, Collectors.counting(), () -> new HashMap<>()))
                .up("reduce(identity)", s -> s.reduce((double) 0, (a, b) -> (double) (a + b)))
                .up("reduce", s -> s.reduce((a, b) -> (double) (a + b)))
                .up("collect", s -> s.collect(ArrayList::new, (c, x) -> c.add(x), (a, b) -> a.addAll(b)))
                .up("min", s -> s.min())
                .up("max", s -> s.max())
                .up("kthLargest", s -> s.kthLargest(1))
                .up("sum", s -> s.sum())
                .up("average", s -> s.average())
                .up("count", s -> s.count())
                .up("summaryStatistics", s -> s.summaryStatistics())
                .up("anyMatch", s -> s.anyMatch(x -> false))
                .up("allMatch", s -> s.allMatch(x -> true))
                .up("noneMatch", s -> s.noneMatch(x -> false))
                .up("findFirst", s -> s.findFirst(x -> false))
                .up("findLast", s -> s.findLast(x -> true));
    }

    /** U29-02: the ArrayDoubleStream terminals fail only through the caller's callback, a null factory, onlyOne() or a bare check. */
    private static Terms<DoubleStream> doubleArrayTerminals() {
        return new Terms<DoubleStream>().up("forEach(action)", s -> s.forEach(x -> failOn2(x)))
                .up("anyMatch(predicate)", s -> s.anyMatch(x -> failOn2(x) < 0))
                .up("allMatch(predicate)", s -> s.allMatch(x -> failOn2(x) > 0))
                .up("noneMatch(predicate)", s -> s.noneMatch(x -> failOn2(x) < 0))
                .up("findFirst(predicate)", s -> s.findFirst(x -> failOn2(x) < 0))
                .up("findLast(predicate)", s -> s.findLast(x -> failOn2(x) < 0))
                .up("reduce(identity, accumulator)", s -> s.reduce((double) 0, (a, b) -> (double) failOn2(b)))
                .up("reduce(accumulator)", s -> s.reduce((a, b) -> (double) failOn2(b)))
                .up("toMap(keyMapper)", s -> s.toMap(x -> failOn2(x), x -> x, (a, b) -> a, () -> new HashMap<>()))
                .up("groupTo(keyMapper)", s -> s.groupTo(x -> failOn2(x), Collectors.counting(), () -> new HashMap<>()))
                .up("collect(accumulator)", s -> s.collect(ArrayList::new, (c, x) -> c.add(failOn2(x)), (a, b) -> a.addAll(b)))
                .expect("onlyOne(2+ elements)", s -> s.onlyOne(), TooManyElementsException.class, null)
                .expect("elementAt(-1) bare check", s -> s.elementAt(-1), IllegalArgumentException.class, null)
                .expect("kthLargest(0) bare check", s -> s.kthLargest(0), IllegalArgumentException.class, null)
                .expect("toCollection(null) bare check", s -> s.toCollection(null), IllegalArgumentException.class, null);
    }

    /** C-016/C-101 null factories: NPE "... returned null" wins over the close failure (also on an EMPTY array range). */
    private static Terms<DoubleStream> doubleNullFactoryTerminals() {
        return new Terms<DoubleStream>().expect("toCollection(null supplier)", s -> s.toCollection(() -> null), NullPointerException.class, "supplier returned null")
                .expect("toMultiset(null supplier)", s -> s.toMultiset(() -> null), NullPointerException.class, "supplier returned null")
                .expect("toMap(null mapFactory)", s -> s.toMap(x -> x, x -> x, (a, b) -> a, () -> null), NullPointerException.class, "mapFactory returned null")
                .expect("groupTo(null mapFactory)", s -> s.groupTo(x -> x, Collectors.counting(), () -> null), NullPointerException.class,
                        "mapFactory returned null")
                .expect("collect(null supplier)", s -> s.collect(() -> null, (c, x) -> {
                }, (a, b) -> {
                }), NullPointerException.class, "supplier returned null");
    }

    @Test
    public void u28_doubleAbstractTerminalsKeepThePrimaryFailure() {
        assertPrimaryWins("double", "array.map(boom)", () -> DoubleStream.of((double) 1, (double) 2, (double) 3).map(x -> (double) boom(x)), doubleAbstractTerminals());
        assertPrimaryWins("double", "iterator.map(boom)", () -> DoubleStream.of(DoubleIterator.of((double) 1, (double) 2, (double) 3)).map(x -> (double) boom(x)),
                doubleAbstractTerminals());
        // onlyOne(): the TooManyElementsException raised INSIDE the try also wins over the close failure (Abstract + Array impl)
        assertPrimaryWins("double", "iterator (2 elements)", () -> DoubleStream.of(DoubleIterator.of((double) 1, (double) 2)),
                new Terms<DoubleStream>().expect("onlyOne", s -> s.onlyOne(), TooManyElementsException.class, null));
        assertPrimaryWins("double", "array (2 elements)", () -> DoubleStream.of((double) 1, (double) 2),
                new Terms<DoubleStream>().expect("onlyOne", s -> s.onlyOne(), TooManyElementsException.class, null));
        // a successful terminal still surfaces the close failure alone
        assertCloseFailureSurfacesOnSuccess("double", () -> DoubleStream.of((double) 7), doubleAbstractTerminals());
        assertCloseFailureSurfacesOnSuccess("double", () -> DoubleStream.of(DoubleIterator.of((double) 7)), doubleAbstractTerminals());
    }

    @Test
    public void u30_doubleIteratorTerminalsKeepThePrimaryFailure() {
        assertPrimaryWins("double", "iterator.map(failOn2)", () -> DoubleStream.of(DoubleIterator.of((double) 1, (double) 2, (double) 3)).map(x -> (double) failOn2(x)),
                doubleIteratorTerminals());
        // the failure raised from hasNext() (filter predicate) instead of nextX()
        assertPrimaryWins("double", "iterator.filter(failOn2)", () -> DoubleStream.of(DoubleIterator.of((double) 1, (double) 2, (double) 3)).filter(x -> failOn2(x) > 0),
                doubleIteratorTerminals());
        // null factories on iterator-backed sources, non-empty and empty
        assertPrimaryWins("double", "iterator", () -> DoubleStream.of(DoubleIterator.of((double) 1, (double) 2, (double) 3)), doubleNullFactoryTerminals());
        assertPrimaryWins("double", "empty iterator", () -> DoubleStream.of(DoubleIterator.of((double) 1)).filter(x -> false), doubleNullFactoryTerminals());
        // a successful terminal still surfaces the close failure alone
        assertCloseFailureSurfacesOnSuccess("double", () -> DoubleStream.of(DoubleIterator.of((double) 1, (double) 2, (double) 3)), doubleIteratorTerminals());
    }

    @Test
    public void u29_doubleArrayTerminalsKeepThePrimaryFailure() {
        assertPrimaryWins("double", "array", () -> DoubleStream.of((double) 1, (double) 2, (double) 3), doubleArrayTerminals());
        assertPrimaryWins("double", "array sub-range", () -> DoubleStream.of(new double[] { 0, 1, 2, 3 }, 1, 3), doubleArrayTerminals());
        assertPrimaryWins("double", "array", () -> DoubleStream.of((double) 1, (double) 2, (double) 3), doubleNullFactoryTerminals());
        assertPrimaryWins("double", "array sub-range", () -> DoubleStream.of(new double[] { 0, 1, 2, 3 }, 1, 3), doubleNullFactoryTerminals());
        assertPrimaryWins("double", "empty array range", () -> DoubleStream.of(new double[] { 0, 1, 2, 3 }, 1, 1), doubleNullFactoryTerminals());
        // a successful terminal still surfaces the close failure alone (the same 22 terminals, array-backed)
        assertCloseFailureSurfacesOnSuccess("double", () -> DoubleStream.of((double) 1, (double) 2, (double) 3), doubleIteratorTerminals());
    }

    @Test
    public void u31_doubleParallelZipWithClosedArgumentKeepsThePrimaryOverACloseFailure() {
        for (int src = 0; src < 2; src++) {
            for (int overload = 0; overload < 4; overload++) {
                final String label = "double src " + src + " overload " + overload;
                final Boom closeFailure = new Boom("closeX");
                final AtomicInteger aClosed = new AtomicInteger();
                final AtomicInteger bClosed = new AtomicInteger();
                final AtomicInteger cClosed = new AtomicInteger();
                // src 0: ParallelArrayDoubleStream, src 1: ParallelIteratorDoubleStream
                final DoubleStream a = (src == 0 ? DoubleStream.of(new double[] { 1, 2, 3 }) : DoubleStream.of(DoubleIterator.of(new double[] { 1, 2, 3 }))).parallel(3)
                        .onClose(() -> {
                            aClosed.incrementAndGet();
                            throw closeFailure;
                        });
                assertTrue(a instanceof ParallelArrayDoubleStream == (src == 0), label);
                final DoubleStream b = DoubleStream.of(new double[] { 4, 5 }).onClose(bClosed::incrementAndGet);
                final DoubleStream c = DoubleStream.of(new double[] { 4, 5 }).onClose(cClosed::incrementAndGet);
                // overloads 0/2: b is closed; overloads 1/3: b is open and c is closed
                final boolean cIsClosed = overload == 1 || overload == 3;
                (cIsClosed ? c : b).close();
                assertEquals(1, (cIsClosed ? cClosed : bClosed).get(), label);
                final int ov = overload;
                final IllegalStateException e = assertThrows(IllegalStateException.class, () -> {
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
                }, label);
                // the ISE of the closed argument is the primary; the receiver's close failure is suppressed onto it
                assertEquals("This stream is already terminated.", e.getMessage(), label);
                assertEquals(1, e.getSuppressed().length, label + ": suppressed " + Arrays.toString(e.getSuppressed()));
                assertSame(closeFailure, e.getSuppressed()[0], label);
                assertEquals(1, aClosed.get(), label + ": receiver close handler runs once");
                assertThrows(IllegalStateException.class, a::count, label + ": receiver closed");
                // the already-closed argument's handlers are NOT re-run (only opened sources are closed)
                assertEquals(1, (cIsClosed ? cClosed : bClosed).get(), label + ": closed argument not re-closed");
                if (cIsClosed) {
                    assertEquals(1, bClosed.get(), label + ": b was opened before c failed, so it is closed once");
                } else {
                    assertEquals(0, cClosed.get(), label + ": c is not involved");
                    assertEquals(2, c.count(), label + ": c stays usable");
                }
            }
        }
    }

    @Test
    public void u31_doubleParallelCollectNullContainerKeepsTheNpeOverACloseFailure() {
        for (int kind = 0; kind < 3; kind++) {
            // 0: ParallelArrayDoubleStream / ARRAY split, 1: ParallelArrayDoubleStream / ITERATOR split, 2: ParallelIteratorDoubleStream
            final String label = "double kind " + kind;
            final Boom closeFailure = new Boom("closeX");
            final AtomicInteger closes = new AtomicInteger();
            final DoubleStream s = (kind == 2 ? DoubleStream.of(DoubleIterator.of(new double[100])).parallel(4)
                    : DoubleStream.of(new double[100]).parallel(ps(kind == 0 ? BaseStream.SplitStrategy.ARRAY : BaseStream.SplitStrategy.ITERATOR))).onClose(() -> {
                        closes.incrementAndGet();
                        throw closeFailure;
                    });
            assertTrue(s instanceof ParallelArrayDoubleStream == (kind < 2), label);

            final NullPointerException e = assertThrows(NullPointerException.class, () -> s.collect(() -> null, (c, x) -> {
            }, (a, b) -> {
            }), label);
            assertEquals("supplier returned null", e.getMessage(), label);
            // other workers may add their own identical NPE as suppressed; the close failure must be among them, never the primary
            assertTrue(Arrays.stream(e.getSuppressed()).anyMatch(x -> x == closeFailure), label + ": suppressed " + Arrays.toString(e.getSuppressed()));
            assertEquals(1, Arrays.stream(e.getSuppressed()).filter(x -> x == closeFailure).count(), label);
            assertEquals(1, closes.get(), label + ": close handler runs once");
            assertThrows(IllegalStateException.class, s::count, label + ": closed");
        }
    }
}
