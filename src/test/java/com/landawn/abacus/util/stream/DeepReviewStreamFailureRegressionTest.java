package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collector;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("unit")
class DeepReviewStreamFailureRegressionTest extends TestBase {

    @Test
    void transformPreservesFailureSharedWithCloseHandler() {
        final RuntimeException failure = new RuntimeException("shared failure");
        final Stream<Integer> stream = Stream.of(1).onClose(() -> {
            throw failure;
        });

        assertSame(failure, assertThrows(RuntimeException.class, () -> stream.transform(s -> {
            throw failure;
        })));
        assertEquals(0, failure.getSuppressed().length);
        assertThrows(IllegalStateException.class, stream::count);
    }

    @Test
    void primitiveParallelTransformPreservesFailureSharedWithCloseHandler() {
        final AssertionError failure = new AssertionError("shared failure");
        final IntStream stream = IntStream.of(1).onClose(() -> {
            throw failure;
        });

        assertSame(failure, assertThrows(AssertionError.class, () -> stream.sps(s -> {
            throw failure;
        })));
        assertEquals(0, failure.getSuppressed().length);
        assertThrows(IllegalStateException.class, stream::count);
    }

    @Test
    void transformSuppressesDistinctCloseFailure() {
        final RuntimeException primary = new RuntimeException("callback failure");
        final RuntimeException secondary = new RuntimeException("close failure");
        final Stream<Integer> stream = Stream.of(1).onClose(() -> {
            throw secondary;
        });

        assertSame(primary, assertThrows(RuntimeException.class, () -> stream.psp(s -> {
            throw primary;
        })));
        assertEquals(1, primary.getSuppressed().length);
        assertSame(secondary, primary.getSuppressed()[0]);
    }

    @Test
    void jdbcBatchCleanupPreservesReusedFailureAndClosesStream() {
        final SQLException failure = new SQLException("driver failure");
        final AtomicInteger clears = new AtomicInteger();
        final AtomicInteger closes = new AtomicInteger();
        final PreparedStatement statement = (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { PreparedStatement.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("clearBatch")) {
                        clears.incrementAndGet();
                        throw failure;
                    } else if (method.getName().equals("addBatch")) {
                        throw failure;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        final Stream<Integer> stream = Stream.of(1).onClose(closes::incrementAndGet);

        assertSame(failure, assertThrows(SQLException.class, () -> stream.persist(statement, 2, 0, (value, stmt) -> {
        })));
        assertEquals(1, clears.get());
        assertEquals(1, closes.get());
        assertEquals(0, failure.getSuppressed().length);
        assertThrows(IllegalStateException.class, stream::count);
    }

    @Test
    void flatMappingPreservesAccumulatorFailureSharedWithCloseHandler() {
        final RuntimeException failure = new RuntimeException("shared failure");
        final AtomicInteger closes = new AtomicInteger();
        final Stream<Integer> mapped = Stream.of(1).onClose(() -> {
            closes.incrementAndGet();
            throw failure;
        });
        final Collector<Integer, List<Integer>, List<Integer>> downstream = Collectors.create(ArrayList::new, (values, value) -> {
            throw failure;
        }, (left, right) -> left);

        assertSame(failure, assertThrows(RuntimeException.class, () -> Stream.of(1).collect(Collectors.flatMapping(value -> mapped, downstream))));
        assertEquals(1, closes.get());
        assertEquals(0, failure.getSuppressed().length);
        assertThrows(IllegalStateException.class, mapped::count);
    }

    @Test
    void flatMappingPreservesIteratorErrorSharedWithCloseHandler() {
        final AssertionError failure = new AssertionError("shared failure");
        final AtomicInteger closes = new AtomicInteger();
        final Stream<Integer> mapped = Stream.<Integer> generate(() -> {
            throw failure;
        }).onClose(() -> {
            closes.incrementAndGet();
            throw failure;
        });

        assertSame(failure, assertThrows(AssertionError.class, () -> Stream.of(1).collect(Collectors.flatMapping(value -> mapped, Collectors.toList()))));
        assertEquals(1, closes.get());
        assertEquals(0, failure.getSuppressed().length);
        assertThrows(IllegalStateException.class, mapped::count);
    }

    @Test
    void flatMappingSuppressesDistinctCloseFailure() {
        final RuntimeException primary = new RuntimeException("traversal failure");
        final RuntimeException secondary = new RuntimeException("close failure");
        final Stream<Integer> mapped = Stream.<Integer> generate(() -> {
            throw primary;
        }).onClose(() -> {
            throw secondary;
        });

        assertSame(primary, assertThrows(RuntimeException.class, () -> Stream.of(1).collect(Collectors.flatMapping(value -> mapped, Collectors.toList()))));
        assertEquals(1, primary.getSuppressed().length);
        assertSame(secondary, primary.getSuppressed()[0]);
    }

    @Test
    void flatMappingAccumulatesAndClosesMappedStreams() {
        final AtomicInteger closes = new AtomicInteger();

        assertEquals(List.of(1, 2, 2, 3),
                Stream.of(1, 2).collect(Collectors.flatMapping(value -> Stream.of(value, value + 1).onClose(closes::incrementAndGet), Collectors.toList())));
        assertEquals(2, closes.get());
    }

    // ---- bug review 2026-10-02 final G132 begin ----

    // Other entry points of the same close-on-failure step (eager transformViaJdkStream, a primitive and an EntryStream transform)
    // keep a failure that the close handler throws too.
    @Test
    void otherEagerTransformsPreserveFailureSharedWithCloseHandler() {
        final RuntimeException viaJdkFailure = new RuntimeException("shared failure");
        final Stream<Integer> stream = Stream.of(1).onClose(() -> {
            throw viaJdkFailure;
        });
        assertSame(viaJdkFailure, assertThrows(RuntimeException.class, () -> stream.transformViaJdkStream(s -> {
            throw viaJdkFailure;
        }, false)));

        final RuntimeException longFailure = new RuntimeException("shared failure");
        assertSame(longFailure, assertThrows(RuntimeException.class, () -> LongStream.of(1L).onClose(() -> {
            throw longFailure;
        }).transform(s -> {
            throw longFailure;
        })));

        final RuntimeException entryFailure = new RuntimeException("shared failure");
        assertSame(entryFailure, assertThrows(RuntimeException.class, () -> EntryStream.of("a", 1).onClose(() -> {
            throw entryFailure;
        }).transform(s -> {
            throw entryFailure;
        })));

        assertEquals(0, viaJdkFailure.getSuppressed().length + longFailure.getSuppressed().length + entryFailure.getSuppressed().length);
    }

    // flatMapping still treats a null mapped stream as empty, and a close failure after a successful traversal still propagates.
    @Test
    void flatMappingNullStreamIsEmptyAndCloseFailureAfterSuccessPropagates() {
        assertEquals(List.of(2, 2),
                Stream.of(1, 2, 3).collect(Collectors.flatMapping(value -> value == 2 ? Stream.of(value, value) : null, Collectors.toList())));

        final RuntimeException closeFailure = new RuntimeException("close failure");
        final AtomicInteger closes = new AtomicInteger();

        assertSame(closeFailure,
                assertThrows(RuntimeException.class, () -> Stream.of(1).collect(Collectors.flatMapping(value -> Stream.of(value).onClose(() -> {
                    closes.incrementAndGet();
                    throw closeFailure;
                }), Collectors.toList()))));
        assertEquals(1, closes.get());
        assertEquals(0, closeFailure.getSuppressed().length);
    }

    // flatMappingValueToMultimap/flatMappingKeyToMultimap traverse their mapped streams with a terminal forEach, which closes them
    // without self-suppression before the try-with-resources close (then a no-op): a shared failure propagates unchanged,
    // for sequential and parallel mapped streams.
    @Test
    void flatMappingToMultimapPreservesFailureSharedWithCloseHandler() {
        for (final boolean parallel : new boolean[] { false, true }) {
            final RuntimeException failure = new RuntimeException("shared failure");
            final AtomicInteger closes = new AtomicInteger();
            final java.util.function.Supplier<Stream<Integer>> mapped = () -> (parallel ? Stream.of(1, 2).parallel(2) : Stream.of(1, 2)).peek(value -> {
                throw failure;
            }).onClose(() -> {
                closes.incrementAndGet();
                throw failure;
            });

            assertSame(failure, assertThrows(RuntimeException.class,
                    () -> Stream.of(1).collect(Collectors.flatMappingValueToMultimap(value -> "k", value -> mapped.get()))));
            assertSame(failure,
                    assertThrows(RuntimeException.class, () -> Stream.of(1).collect(Collectors.flatMappingKeyToMultimap(value -> mapped.get(), value -> "v"))));
            assertEquals(2, closes.get());
            assertEquals(0, failure.getSuppressed().length);
        }
    }

    // ---- bug review 2026-10-02 final G132 end ----
}
