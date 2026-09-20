package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("unit")
public class SeqOptionalAndFailureContractTest extends TestBase {
    /**
     * Seq rule (2026-09-20): every element-valued terminal returns a {@code u.Nullable}. A selected {@code null}
     * element is a PRESENT Nullable holding {@code null}; an empty Nullable means only that nothing was selected.
     * The r9599 contract (Optional + NullPointerException) now applies to {@code Stream} alone, see
     * {@link #streamConversionSwitchesToTheStrictOptionalContract()}.
     */
    @Test
    void selectedNullIsPresentWhileEmptyRemainsEmpty() throws Exception {
        final List<Throwables.Function<Seq<Integer, Exception>, u.Nullable<Integer>, Exception>> terminals = List.of(
                Seq::first, Seq::last, Seq::findFirst, Seq::findAny, Seq::onlyOne, s -> s.elementAt(0),
                s -> s.findFirst(v -> true), s -> s.findLast(v -> true), s -> s.findAny(v -> true),
                s -> s.kthLargest(1, Comparator.nullsFirst(Comparator.naturalOrder())),
                s -> s.min(Comparator.nullsFirst(Comparator.naturalOrder())),
                s -> s.max(Comparator.nullsLast(Comparator.naturalOrder())), s -> s.minBy(v -> v), s -> s.maxBy(v -> v),
                s -> s.reduce((a, b) -> null));
        for (final var terminal : terminals) {
            final AtomicInteger closes = new AtomicInteger();
            final Seq<Integer, Exception> seq = Seq.<Integer, Exception>of((Integer) null).onClose(closes::incrementAndGet);
            final u.Nullable<Integer> selected = terminal.apply(seq);
            assertTrue(selected.isPresent());
            assertTrue(selected.isNull());
            assertNull(selected.get());
            assertEquals(1, closes.get());
            assertFalse(terminal.apply(Seq.<Integer, Exception>empty()).isPresent());
            assertEquals(u.Nullable.of(7), terminal.apply(Seq.<Integer, Exception>of(7)));
        }
    }

    @Test
    void streamConversionSwitchesToTheStrictOptionalContract() throws Exception {
        // Deliberate split: Seq terminals return Nullable (null-tolerant); the corresponding Stream terminals
        // return Optional and throw NullPointerException for a selected null element (r9599). Converting a Seq
        // with stream() therefore switches contracts, as the Seq class javadoc states.
        assertTrue(Seq.<Integer, Exception>of((Integer) null).first().isNull());
        assertThrows(NullPointerException.class, () -> Seq.<Integer, Exception>of((Integer) null).stream().first());
        assertFalse(Seq.<Integer, Exception>empty().stream().first().isPresent());
    }

    @Test
    void terminalFailuresRetainIdentityAndSuppressCleanup() {
        final IllegalStateException primary = new IllegalStateException("operation failed");
        final List<Throwables.Consumer<Seq<Integer, Exception>, Exception>> terminals = List.of(
                s -> s.forEach(v -> { throw primary; }),
                s -> s.toCollection(() -> { throw primary; }),
                s -> s.toMultiset(() -> { throw primary; }),
                s -> s.reduce((a, b) -> { throw primary; }),
                s -> s.reduce(0, (a, b) -> { throw primary; }),
                s -> s.collect(ArrayList<Integer>::new, (a, b) -> { throw primary; }),
                s -> s.anyMatch(v -> { throw primary; }),
                s -> s.allMatch(v -> { throw primary; }),
                s -> s.noneMatch(v -> { throw primary; }),
                s -> s.findFirst(v -> { throw primary; }),
                s -> s.findLast(v -> { throw primary; }),
                s -> s.applyIfNotEmpty(v -> { throw primary; }),
                s -> s.acceptIfNotEmpty(v -> { throw primary; }));
        final List<Throwable> suppressed = new ArrayList<>();
        for (final var terminal : terminals) {
            final AssertionError cleanup = new AssertionError("cleanup failed");
            suppressed.add(cleanup);
            final AtomicInteger closes = new AtomicInteger();
            final Seq<Integer, Exception> seq = Seq.<Integer, Exception>of(1, 2).onClose(() -> {
                closes.incrementAndGet();
                throw cleanup;
            });
            assertSame(primary, assertThrows(IllegalStateException.class, () -> terminal.accept(seq)));
            assertArrayEquals(suppressed.toArray(Throwable[]::new), primary.getSuppressed());
            assertEquals(1, closes.get());
            seq.close();
            assertEquals(1, closes.get());
        }
    }

    @Test
    void checkedPredicateFailureRetainsIdentity() {
        final IOException primary = new IOException("predicate failed");
        final AssertionError cleanup = new AssertionError("cleanup failed");
        final Seq<Integer, Exception> seq = Seq.<Integer, Exception>of(1).onClose(() -> { throw cleanup; });
        assertSame(primary, assertThrows(IOException.class, () -> seq.findFirst(v -> { throw primary; })));
        assertArrayEquals(new Throwable[] { cleanup }, primary.getSuppressed());
    }

    @Test
    void nullApplyResultThrowsAndSuppressesCleanup() {
        final AssertionError cleanup = new AssertionError("cleanup failed");
        final AtomicInteger closes = new AtomicInteger();
        final Seq<Integer, Exception> seq = Seq.<Integer, Exception>of((Integer) null).onClose(() -> {
            closes.incrementAndGet();
            throw cleanup;
        });
        final NullPointerException failure = assertThrows(NullPointerException.class, () -> seq.applyIfNotEmpty(s -> null));
        assertArrayEquals(new Throwable[] { cleanup }, failure.getSuppressed());
        assertEquals(1, closes.get());
        seq.close();
        assertEquals(1, closes.get());
    }

    @Test
    void applyIfNotEmptyStillSkipsEmptyAndRetainsNonNullResults() throws Exception {
        assertFalse(Seq.<Integer, Exception>empty().applyIfNotEmpty(s -> {
            throw new AssertionError("must not run");
        }).isPresent());
        assertEquals(u.Optional.of(2L), Seq.<Integer, Exception>of(null, 1).applyIfNotEmpty(Seq::count));
    }

    @Test
    void nullCallbackValidationSuppressesCleanupEvenForEmptyInput() {
        final List<Throwables.Consumer<Seq<Integer, Exception>, Exception>> terminals = List.of(
                s -> s.forEach(null), s -> s.toCollection(null), s -> s.toMultiset(null), s -> s.reduce(null),
                s -> s.anyMatch(null), s -> s.allMatch(null), s -> s.noneMatch(null),
                s -> s.findFirst(null), s -> s.findLast(null), s -> s.applyIfNotEmpty(null), s -> s.acceptIfNotEmpty(null));
        for (final var terminal : terminals) {
            final AssertionError cleanup = new AssertionError("cleanup failed");
            final AtomicInteger closes = new AtomicInteger();
            final Seq<Integer, Exception> seq = Seq.<Integer, Exception>empty().onClose(() -> {
                closes.incrementAndGet();
                throw cleanup;
            });
            final IllegalArgumentException primary = assertThrows(IllegalArgumentException.class, () -> terminal.accept(seq));
            assertArrayEquals(new Throwable[] { cleanup }, primary.getSuppressed());
            assertEquals(1, closes.get());
        }
    }
}
