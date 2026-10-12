package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;

@Tag("unit")
class EntryStreamCollectorSetupPass2Review20261010Test extends TestBase {
    record SetupCase(boolean byKey, String accessor, boolean error) {
    }

    static java.util.stream.Stream<SetupCase> setupCases() {
        final List<SetupCase> cases = new ArrayList<>();
        for (final boolean byKey : new boolean[] { false, true }) {
            for (final String accessor : List.of("supplier", "accumulator", "combiner", "finisher", "characteristics")) {
                for (final boolean error : new boolean[] { false, true }) {
                    cases.add(new SetupCase(byKey, accessor, error));
                }
            }
        }
        return cases.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("setupCases")
    void failedCollectorAdapterClosesInputAndPreservesFailure(final SetupCase testCase) {
        final Throwable failure = testCase.error() ? new AssertionError("collector accessor") : new IllegalStateException("collector accessor");
        final IllegalArgumentException closeFailure = new IllegalArgumentException("close");
        final AtomicInteger closes = new AtomicInteger();
        final AtomicInteger mapped = new AtomicInteger();
        final EntryStream<Integer, Integer> input = EntryStream.of(1, 1, 1, 2).onClose(() -> {
            closes.incrementAndGet();
            throw closeFailure;
        });
        final Function<Map.Entry<Integer, Integer>, Integer> mapper = entry -> {
            mapped.incrementAndGet();
            return entry.getValue();
        };

        try {
            assertSame(failure, assertThrows(Throwable.class, () -> collapse(input, testCase.byKey(), mapper, failingCollector(testCase.accessor(), failure))));
            assertEquals(0, mapped.get());
            assertEquals(1, closes.get());
            assertEquals(1, failure.getSuppressed().length);
            assertSame(closeFailure, failure.getSuppressed()[0]);
            assertThrows(IllegalStateException.class, input::count);
        } finally {
            // The unfixed adapter leaves its source open, so clean it up even when the regression assertion fails.
            try {
                input.close();
            } catch (final IllegalArgumentException ignored) {
            }
        }
        assertEquals(1, closes.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void identicalSetupAndCloseFailuresAreNotSelfSuppressed(final boolean byKey) {
        final IllegalStateException failure = new IllegalStateException("shared failure");
        final AtomicInteger closes = new AtomicInteger();
        final EntryStream<Integer, Integer> input = EntryStream.of(1, 1).onClose(() -> {
            closes.incrementAndGet();
            throw failure;
        });
        try {
            assertSame(failure,
                    assertThrows(IllegalStateException.class, () -> collapse(input, byKey, Map.Entry::getValue, failingCollector("accumulator", failure))));
            assertEquals(1, closes.get());
            assertEquals(0, failure.getSuppressed().length);
        } finally {
            try {
                input.close();
            } catch (final IllegalStateException ignored) {
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void successfulCollapseStaysLazyAndPreservesNullsAndGroups(final boolean byKey) {
        final AtomicInteger mapped = new AtomicInteger();
        final AtomicInteger closes = new AtomicInteger();
        final EntryStream<Integer, Integer> input = EntryStream.of(1, 1, 1, 1, 2, 2).onClose(closes::incrementAndGet);
        final Stream<List<Integer>> groups = collapse(input, byKey, entry -> {
            mapped.incrementAndGet();
            return entry.getKey() == 1 ? null : entry.getValue();
        }, Collectors.toList());
        try (groups) {
            assertEquals(0, mapped.get());
            assertEquals(0, closes.get());
            assertEquals(List.of(java.util.Arrays.asList(null, null), List.of(2)), groups.toList());
            assertEquals(3, mapped.get());
            assertEquals(1, closes.get());
        }
        assertEquals(1, closes.get());
    }

    private static Stream<List<Integer>> collapse(final EntryStream<Integer, Integer> input, final boolean byKey,
            final Function<Map.Entry<Integer, Integer>, Integer> mapper, final Collector<Integer, ?, List<Integer>> collector) {
        return byKey ? input.collapseByKey(Objects::equals, mapper, collector) : input.collapseByValue(Objects::equals, mapper, collector);
    }

    private static Collector<Integer, List<Integer>, List<Integer>> failingCollector(final String accessor, final Throwable failure) {
        return new Collector<>() {
            private void fail(final String name) {
                if (accessor.equals(name)) {
                    if (failure instanceof Error error) {
                        throw error;
                    }
                    throw (RuntimeException) failure;
                }
            }

            @Override
            public Supplier<List<Integer>> supplier() {
                fail("supplier");
                return ArrayList::new;
            }

            @Override
            public BiConsumer<List<Integer>, Integer> accumulator() {
                fail("accumulator");
                return List::add;
            }

            @Override
            public BinaryOperator<List<Integer>> combiner() {
                fail("combiner");
                return (left, right) -> {
                    left.addAll(right);
                    return left;
                };
            }

            @Override
            public Function<List<Integer>, List<Integer>> finisher() {
                fail("finisher");
                return Function.identity();
            }

            @Override
            public Set<Characteristics> characteristics() {
                fail("characteristics");
                return Set.of();
            }
        };
    }
}
