package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

class PrimitiveArrayTopReview20261010Test extends TestBase {
    @Test
    void skippingSelectionPreservesValuesComparatorCallsAndCloseHandlers() {
        for (Function<Comparator<Number>, BaseStream<?, ?, ?, ?, ?, ?, ?, ?>> factory : sources()) {
            final AtomicInteger expectedCalls = new AtomicInteger();
            final List<Integer> selected = values(factory.apply(countingComparator(expectedCalls)).toList());

            for (long skipped : new long[] { 0, 2, Long.MAX_VALUE }) {
                final AtomicInteger actualCalls = new AtomicInteger();
                final AtomicInteger closes = new AtomicInteger();

                try (BaseStream<?, ?, ?, ?, ?, ?, ?, ?> stream = factory.apply(countingComparator(actualCalls))
                        .onClose(closes::incrementAndGet)
                        .skip(skipped)) {
                    assertEquals(0, actualCalls.get());
                    assertEquals(selected.subList((int) Math.min(skipped, selected.size()), selected.size()), values(stream.toList()));
                    assertEquals(expectedCalls.get(), actualCalls.get());
                    assertEquals(1, closes.get());
                }

                assertEquals(1, closes.get());
            }
        }
    }

    @Test
    void failedSelectionCanRetryBeforeSkippingAnyOutput() {
        for (Function<Comparator<Number>, BaseStream<?, ?, ?, ?, ?, ?, ?, ?>> factory : sources()) {
            final AtomicInteger expectedCalls = new AtomicInteger();
            final List<Integer> selected = values(factory.apply(countingComparator(expectedCalls)).toList());
            final AtomicInteger actualCalls = new AtomicInteger();
            final AtomicInteger closes = new AtomicInteger();
            final IllegalStateException failure = new IllegalStateException("selection comparator failed");
            final Comparator<Number> comparator = (left, right) -> {
                if (actualCalls.incrementAndGet() == 3) {
                    throw failure;
                }
                return Integer.compare(left.intValue(), right.intValue());
            };

            try (BaseStream<?, ?, ?, ?, ?, ?, ?, ?> stream = factory.apply(comparator).onClose(closes::incrementAndGet).skip(2)) {
                final Iterator<?> iterator = stream.iterator();
                assertSame(failure, assertThrows(IllegalStateException.class, iterator::hasNext));
                assertEquals(0, closes.get());

                final List<Integer> actual = new ArrayList<>();
                iterator.forEachRemaining(value -> actual.add(((Number) value).intValue()));
                assertEquals(selected.subList(2, selected.size()), actual);
                assertEquals(expectedCalls.get() + 3, actualCalls.get());
            }

            assertEquals(1, closes.get());
        }
    }

    @Test
    void selectionWorksThroughNestedSkipAndLimit() {
        for (Function<Comparator<Number>, BaseStream<?, ?, ?, ?, ?, ?, ?, ?>> factory : sources()) {
            final Comparator<Number> comparator = Comparator.comparingInt(Number::intValue);
            final List<Integer> selected = values(factory.apply(comparator).toList());

            assertEquals(selected.subList(2, 3), values(factory.apply(comparator).skip(1).limit(2).skip(1).toList()));
            assertEquals(List.of(), factory.apply(comparator).limit(2).skip(Long.MAX_VALUE).toList());
        }
    }

    private static Comparator<Number> countingComparator(final AtomicInteger calls) {
        return (left, right) -> {
            calls.incrementAndGet();
            return Integer.compare(left.intValue(), right.intValue());
        };
    }

    private static List<Integer> values(final List<?> values) {
        return values.stream().map(value -> ((Number) value).intValue()).toList();
    }

    private static List<Function<Comparator<Number>, BaseStream<?, ?, ?, ?, ?, ?, ?, ?>>> sources() {
        return List.of(comparator -> ShortStream.of((short) 9, (short) 1, (short) 6, (short) 3, (short) 8, (short) 2, (short) 7).top(4, comparator),
                comparator -> IntStream.of(9, 1, 6, 3, 8, 2, 7).top(4, comparator), comparator -> LongStream.of(9L, 1L, 6L, 3L, 8L, 2L, 7L).top(4, comparator),
                comparator -> FloatStream.of(9f, 1f, 6f, 3f, 8f, 2f, 7f).top(4, comparator),
                comparator -> DoubleStream.of(9d, 1d, 6d, 3d, 8d, 2d, 7d).top(4, comparator));
    }
}
