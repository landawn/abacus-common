package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
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
class StreamCollectorSetupReview20261010Test extends TestBase {
    record Case(boolean array, boolean parallel, String operation, String accessor, boolean error) {
    }

    static java.util.stream.Stream<Case> failures() {
        final List<Case> cases = new ArrayList<>();
        for (final boolean array : new boolean[] { false, true }) {
            for (final boolean parallel : new boolean[] { false, true }) {
                for (final String operation : List.of("collect", "split", "splitPredicate", "sliding", "collapsePair", "collapseTriple", "join", "joinTail",
                        "group", "flatGroup", "partition")) {
                    for (final String accessor : List.of("supplier", "accumulator", "finisher")) {
                        cases.add(new Case(array, parallel, operation, accessor, false));
                        cases.add(new Case(array, parallel, operation, accessor, true));
                    }
                }
                if (parallel) {
                    for (final String accessor : List.of("combiner", "characteristics")) {
                        cases.add(new Case(array, true, "collect", accessor, false));
                        cases.add(new Case(array, true, "collect", accessor, true));
                    }
                    cases.add(new Case(array, true, "group", "combiner", false));
                    cases.add(new Case(array, true, "flatGroup", "combiner", true));
                }
            }
        }
        return cases.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("failures")
    void failedCollectorSetupClosesSourceAndPreservesFailure(final Case testCase) {
        final Throwable failure = testCase.error() ? new AssertionError("accessor") : new IllegalStateException("accessor");
        final IllegalArgumentException closeFailure = new IllegalArgumentException("close");
        final AtomicInteger closes = new AtomicInteger();
        Stream<Integer> source = testCase.array() ? Stream.of(1, 2, 3) : Stream.of(List.of(1, 2, 3).iterator());
        if (testCase.parallel()) {
            source = source.parallel(2);
        }
        final Stream<Integer> stream = source.onClose(() -> {
            closes.incrementAndGet();
            throw closeFailure;
        });
        final Collector<Integer, List<Integer>, List<Integer>> collector = failingCollector(testCase.accessor(), failure);
        try {
            assertSame(failure, assertThrows(Throwable.class, () -> {
                switch (testCase.operation()) {
                    case "collect" -> stream.collect(collector);
                    case "split" -> stream.split(2, collector);
                    case "splitPredicate" -> stream.split(value -> value < 2, collector);
                    case "sliding" -> stream.sliding(2, 1, collector);
                    case "collapsePair" -> stream.collapse((left, right) -> true, collector);
                    case "collapseTriple" -> stream.collapse((first, previous, next) -> true, collector);
                    case "join" -> stream.joinByRange(List.of(1, 2).iterator(), Integer::equals, collector);
                    case "joinTail" -> stream.joinByRange(List.of(1, 2).iterator(), Integer::equals, collector, (value, matches) -> matches,
                            remaining -> Stream.empty());
                    case "group" -> stream.groupTo(value -> value % 2, collector);
                    case "flatGroup" -> stream.flatGroupTo(value -> List.of(value % 2), collector);
                    case "partition" -> stream.partitionTo(value -> value % 2 == 0, collector);
                    default -> throw new AssertionError(testCase);
                }
            }));
            assertEquals(1, closes.get());
            assertEquals(1, failure.getSuppressed().length);
            assertSame(closeFailure, failure.getSuppressed()[0]);
            assertThrows(IllegalStateException.class, stream::count);
        } finally {
            // The baseline leaks here; release it even when a regression assertion fails.
            try {
                stream.close();
            } catch (final IllegalArgumentException ignored) {
            }
        }
        assertEquals(1, closes.get());
    }

    static java.util.stream.Stream<Case> joinFailures() {
        return failures().filter(testCase -> testCase.operation().equals("join") || testCase.operation().equals("joinTail"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("joinFailures")
    void failedJoinSetupClosesBothStreams(final Case testCase) {
        final Throwable failure = testCase.error() ? new AssertionError("accessor") : new IllegalStateException("accessor");
        final IllegalArgumentException leftCloseFailure = new IllegalArgumentException("left close");
        final IllegalArgumentException rightCloseFailure = new IllegalArgumentException("right close");
        final AtomicInteger leftCloses = new AtomicInteger();
        final AtomicInteger rightCloses = new AtomicInteger();
        Stream<Integer> source = testCase.array() ? Stream.of(1, 2, 3) : Stream.of(List.of(1, 2, 3).iterator());
        if (testCase.parallel()) {
            source = source.parallel(2);
        }
        final Stream<Integer> left = source.onClose(() -> {
            leftCloses.incrementAndGet();
            throw leftCloseFailure;
        });
        final Stream<Integer> right = Stream.of(1, 2).onClose(() -> {
            rightCloses.incrementAndGet();
            throw rightCloseFailure;
        });
        final Collector<Integer, List<Integer>, List<Integer>> collector = failingCollector(testCase.accessor(), failure);
        try {
            assertSame(failure, assertThrows(Throwable.class, () -> {
                if (testCase.operation().equals("join")) {
                    left.joinByRange(right, Integer::equals, collector);
                } else {
                    left.joinByRange(right, Integer::equals, collector, (value, matches) -> matches, remaining -> Stream.empty());
                }
            }));
            assertEquals(1, leftCloses.get());
            assertEquals(1, rightCloses.get());
            assertEquals(List.of(leftCloseFailure, rightCloseFailure), List.of(failure.getSuppressed()));
            assertThrows(IllegalStateException.class, left::count);
            assertThrows(IllegalStateException.class, right::count);
        } finally {
            for (final Stream<Integer> stream : List.of(left, right)) {
                try {
                    stream.close();
                } catch (final IllegalArgumentException ignored) {
                }
            }
        }
        assertEquals(1, leftCloses.get());
        assertEquals(1, rightCloses.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void joiningAnAlreadyClosedRightStreamClosesTheReceiver(final boolean withRemainder) {
        final AtomicInteger leftCloses = new AtomicInteger();
        final AtomicInteger rightCloses = new AtomicInteger();
        final IllegalArgumentException closeFailure = new IllegalArgumentException("left close");
        final Stream<Integer> left = Stream.of(1, 2).onClose(() -> {
            leftCloses.incrementAndGet();
            throw closeFailure;
        });
        final Stream<Integer> right = Stream.of(1, 2).onClose(rightCloses::incrementAndGet);
        right.close();

        try {
            final IllegalStateException failure = assertThrows(IllegalStateException.class, () -> joinWithCollector(left, right, withRemainder));
            assertEquals(List.of(closeFailure), List.of(failure.getSuppressed()));
            assertEquals(1, leftCloses.get());
            assertEquals(1, rightCloses.get());
            assertTrue(left.isClosed());
            assertTrue(right.isClosed());
        } finally {
            closeAfterAssertion(left, right);
        }
    }

    record JoinAcquisitionCase(boolean withRemainder, boolean error) {
    }

    static java.util.stream.Stream<JoinAcquisitionCase> acquisitionFailures() {
        return java.util.stream.Stream.of(new JoinAcquisitionCase(false, false), new JoinAcquisitionCase(false, true), new JoinAcquisitionCase(true, false),
                new JoinAcquisitionCase(true, true));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("acquisitionFailures")
    void failedRightIteratorAcquisitionClosesBothInputsAndPreservesFailure(final JoinAcquisitionCase testCase) {
        final Throwable failure = testCase.error() ? new AssertionError("right iterator") : new IllegalStateException("right iterator");
        final IllegalArgumentException leftCloseFailure = new IllegalArgumentException("left close");
        final IllegalArgumentException rightCloseFailure = new IllegalArgumentException("right close");
        final List<String> closeOrder = new ArrayList<>();
        final AtomicInteger acquisitions = new AtomicInteger();
        final Stream<Integer> left = Stream.of(List.of(1, 2).iterator()).onClose(() -> {
            closeOrder.add("left");
            throw leftCloseFailure;
        });
        final Stream<Integer> right = new AcquisitionStream(acquisitions, failure).onClose(() -> {
            closeOrder.add("right");
            throw rightCloseFailure;
        });

        try {
            assertSame(failure, assertThrows(Throwable.class, () -> joinWithCollector(left, right, testCase.withRemainder())));
            assertEquals(1, acquisitions.get());
            assertEquals(List.of("left", "right"), closeOrder);
            assertEquals(List.of(leftCloseFailure, rightCloseFailure), List.of(failure.getSuppressed()));
            assertTrue(left.isClosed());
            assertTrue(right.isClosed());
            left.close();
            right.close();
            assertEquals(List.of("left", "right"), closeOrder);
        } finally {
            closeAfterAssertion(left, right);
        }
    }

    record JoinValidationCase(boolean withRemainder, String argument) {
    }

    static java.util.stream.Stream<JoinValidationCase> joinValidationFailures() {
        final List<JoinValidationCase> cases = new ArrayList<>();
        for (final boolean withRemainder : new boolean[] { false, true }) {
            for (final String argument : List.of("predicate", "collector", "function")) {
                cases.add(new JoinValidationCase(withRemainder, argument));
            }
        }
        cases.add(new JoinValidationCase(true, "mapperForUnJoinedElements"));
        return cases.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("joinValidationFailures")
    void joinValidationClosesOnlyTheReceiverAndLeavesTheRightInputUsable(final JoinValidationCase testCase) {
        final AtomicInteger leftCloses = new AtomicInteger();
        final AtomicInteger rightCloses = new AtomicInteger();
        final AtomicInteger acquisitions = new AtomicInteger();
        final IllegalArgumentException closeFailure = new IllegalArgumentException("left close");
        final Stream<Integer> left = Stream.of(1, 2).onClose(() -> {
            leftCloses.incrementAndGet();
            throw closeFailure;
        });
        final Stream<Integer> right = new AcquisitionStream(acquisitions, null).onClose(rightCloses::incrementAndGet);
        final BiPredicate<Integer, Integer> predicate = testCase.argument().equals("predicate") ? null : Integer::equals;
        final Collector<Integer, List<Integer>, List<Integer>> collector = testCase.argument().equals("collector") ? null
                : failingCollector("supplier", new AssertionError("Collector must remain uninitialized during validation"));
        final BiFunction<Integer, List<Integer>, List<Integer>> function = testCase.argument().equals("function") ? null : (value, matches) -> matches;
        final Function<Iterator<Integer>, Stream<List<Integer>>> remainderMapper = testCase.argument().equals("mapperForUnJoinedElements") ? null
                : remaining -> Stream.empty();

        try {
            final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> {
                if (testCase.withRemainder()) {
                    left.joinByRange(right, predicate, collector, function, remainderMapper);
                } else {
                    left.joinByRange(right, predicate, collector, function);
                }
            });
            assertTrue(failure.getMessage().contains(testCase.argument()));
            assertEquals(List.of(closeFailure), List.of(failure.getSuppressed()));
            assertEquals(1, leftCloses.get());
            assertEquals(0, acquisitions.get());
            assertEquals(0, rightCloses.get());
            assertTrue(left.isClosed());
            assertEquals(List.of(1, 2), right.toList());
            assertEquals(1, rightCloses.get());
        } finally {
            closeAfterAssertion(left, right);
        }
    }

    private static Stream<List<Integer>> joinWithCollector(final Stream<Integer> left, final Stream<Integer> right, final boolean withRemainder) {
        final Collector<Integer, List<Integer>, List<Integer>> collector = failingCollector("supplier",
                new AssertionError("Collector must remain uninitialized when right iterator acquisition fails"));
        return withRemainder ? left.joinByRange(right, Integer::equals, collector, (value, matches) -> matches, remaining -> Stream.empty())
                : left.joinByRange(right, Integer::equals, collector, (value, matches) -> matches);
    }

    private static void closeAfterAssertion(final Stream<?>... streams) {
        // Also release inputs when testing the old behavior, where construction failure leaves them open.
        for (final Stream<?> stream : streams) {
            try {
                stream.close();
            } catch (final IllegalArgumentException ignored) {
            }
        }
    }

    private static final class AcquisitionStream extends IteratorStream<Integer> {
        private final AtomicInteger acquisitions;
        private final Throwable failure;

        private AcquisitionStream(final AtomicInteger acquisitions, final Throwable failure) {
            super(List.of(1, 2).iterator());
            this.acquisitions = acquisitions;
            this.failure = failure;
        }

        @Override
        ObjIteratorEx<Integer> iteratorEx() {
            acquisitions.incrementAndGet();
            if (failure instanceof Error error) {
                throw error;
            } else if (failure != null) {
                throw (RuntimeException) failure;
            }
            return super.iteratorEx();
        }
    }

    private static Collector<Integer, List<Integer>, List<Integer>> failingCollector(final String accessor, final Throwable failure) {
        return new Collector<>() {
            private void check(final String called) {
                if (called.equals(accessor)) {
                    if (failure instanceof Error error) {
                        throw error;
                    }
                    throw (RuntimeException) failure;
                }
            }

            @Override
            public Supplier<List<Integer>> supplier() {
                check("supplier");
                return ArrayList::new;
            }

            @Override
            public BiConsumer<List<Integer>, Integer> accumulator() {
                check("accumulator");
                return List::add;
            }

            @Override
            public BinaryOperator<List<Integer>> combiner() {
                check("combiner");
                return (left, right) -> {
                    left.addAll(right);
                    return left;
                };
            }

            @Override
            public Function<List<Integer>, List<Integer>> finisher() {
                check("finisher");
                return Function.identity();
            }

            @Override
            public Set<Characteristics> characteristics() {
                check("characteristics");
                return Set.of();
            }
        };
    }
}
