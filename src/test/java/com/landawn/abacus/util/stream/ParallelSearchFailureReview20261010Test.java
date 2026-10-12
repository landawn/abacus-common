package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.AsyncExecutor;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.u.Optional;

/** Selected null values fail before stream cleanup, just like other terminal result failures. */
@Tag("unit")
class ParallelSearchFailureReview20261010Test extends TestBase {
    private enum Search {
        FIRST, ANY, LAST;

        <E extends Exception> Optional<String> apply(final Stream<String> stream, final Throwables.Predicate<String, E> predicate) throws E {
            return switch (this) {
                case FIRST -> stream.findFirst(predicate);
                case ANY -> stream.findAny(predicate);
                case LAST -> stream.findLast(predicate);
            };
        }
    }

    private static Stream<String> source(final boolean iterator, final BaseStream.SplitStrategy strategy, final String... values) {
        final AsyncExecutor executor = new AsyncExecutor(Runnable::run);
        return iterator ? new ParallelIteratorStream<>(Arrays.asList(values).iterator(), false, null, 2, strategy, executor, false, null)
                : new ParallelArrayStream<>(values, 0, values.length, false, null, 2, strategy, executor, false, null);
    }

    @Test
    void selectedNullKeepsPrecedenceOverCloseFailure() {
        for (final boolean iterator : new boolean[] { false, true }) {
            for (final BaseStream.SplitStrategy strategy : BaseStream.SplitStrategy.values()) {
                for (final Search search : Search.values()) {
                    final IllegalStateException closeFailure = new IllegalStateException("close");
                    final AtomicInteger closes = new AtomicInteger();
                    final Stream<String> stream = source(iterator, strategy, null, null).onClose(() -> {
                        closes.incrementAndGet();
                        throw closeFailure;
                    });

                    final NullPointerException failure = assertThrows(NullPointerException.class, () -> search.apply(stream, value -> true));
                    assertEquals(1, failure.getSuppressed().length);
                    assertSame(closeFailure, failure.getSuppressed()[0]);
                    assertEquals(1, closes.get());
                    assertTrue(stream.isClosed());
                }
            }
        }
    }

    @Test
    void successfulAndEmptySearchesStillPropagateCloseFailure() {
        for (final boolean iterator : new boolean[] { false, true }) {
            for (final BaseStream.SplitStrategy strategy : BaseStream.SplitStrategy.values()) {
                for (final Search search : Search.values()) {
                    for (final boolean matches : new boolean[] { false, true }) {
                        final IllegalStateException closeFailure = new IllegalStateException("close");
                        final Stream<String> stream = source(iterator, strategy, "a", "b").onClose(() -> {
                            throw closeFailure;
                        });
                        assertSame(closeFailure, assertThrows(IllegalStateException.class, () -> search.apply(stream, value -> matches)));
                        assertTrue(stream.isClosed());
                    }
                }
            }
        }
    }

    @Test
    void predicateFailureKeepsPrecedenceOverCloseFailure() {
        for (final boolean iterator : new boolean[] { false, true }) {
            for (final BaseStream.SplitStrategy strategy : BaseStream.SplitStrategy.values()) {
                for (final Search search : Search.values()) {
                    final IllegalArgumentException predicateFailure = new IllegalArgumentException("predicate");
                    final IllegalStateException closeFailure = new IllegalStateException("close");
                    final Stream<String> stream = source(iterator, strategy, "a", "b").onClose(() -> {
                        throw closeFailure;
                    });
                    assertSame(predicateFailure, assertThrows(IllegalArgumentException.class, () -> search.apply(stream, value -> {
                        throw predicateFailure;
                    })));
                    assertEquals(1, predicateFailure.getSuppressed().length);
                    assertSame(closeFailure, predicateFailure.getSuppressed()[0]);
                    assertTrue(stream.isClosed());
                }
            }
        }
    }

    @Test
    void checkedPredicateFailureKeepsItsTypeAndCleanupFailure() {
        for (final boolean iterator : new boolean[] { false, true }) {
            for (final BaseStream.SplitStrategy strategy : BaseStream.SplitStrategy.values()) {
                for (final Search search : Search.values()) {
                    final IOException predicateFailure = new IOException("predicate");
                    final IllegalStateException closeFailure = new IllegalStateException("close");
                    final Stream<String> stream = source(iterator, strategy, "a", "b").onClose(() -> {
                        throw closeFailure;
                    });
                    assertSame(predicateFailure, assertThrows(IOException.class, () -> search.apply(stream, value -> {
                        throw predicateFailure;
                    })));
                    assertEquals(1, predicateFailure.getSuppressed().length);
                    assertSame(closeFailure, predicateFailure.getSuppressed()[0]);
                    assertTrue(stream.isClosed());
                }
            }
        }
    }

    @Test
    void onlyTheSelectedResultNeedsToBeNonNull() {
        for (final boolean iterator : new boolean[] { false, true }) {
            for (final BaseStream.SplitStrategy strategy : BaseStream.SplitStrategy.values()) {
                assertEquals("a", source(iterator, strategy, "a", null).findFirst(value -> true).get());
                assertEquals("a", source(iterator, strategy, null, "a").findLast(value -> true).get());
                assertEquals("a", source(iterator, strategy, "a", null).findAny(value -> true).get());
            }
        }
    }
}
