package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class LongTriConsumer2025Test extends TestBase {

    @Test
    public void testAccept() {
        final AtomicLong sum = new AtomicLong(0);
        final LongTriConsumer consumer = (a, b, c) -> sum.set(a + b + c);

        consumer.accept(10L, 20L, 30L);

        assertEquals(60L, sum.get());
    }

    @Test
    public void testAccept_withLambda() {
        final List<Long> results = new ArrayList<>();
        final LongTriConsumer consumer = (a, b, c) -> results.add(a * b * c);

        consumer.accept(2L, 3L, 4L);

        assertEquals(1, results.size());
        assertEquals(24L, results.get(0));
    }

    @Test
    public void testAccept_withAnonymousClass() {
        final List<String> results = new ArrayList<>();
        final LongTriConsumer consumer = new LongTriConsumer() {
            @Override
            public void accept(final long a, final long b, final long c) {
                results.add(String.format("%d,%d,%d", a, b, c));
            }
        };

        consumer.accept(1L, 2L, 3L);

        assertEquals(1, results.size());
        assertEquals("1,2,3", results.get(0));
    }

    @Test
    public void testAndThen() {
        final List<Long> results = new ArrayList<>();
        final LongTriConsumer first = (a, b, c) -> results.add(a + b + c);
        final LongTriConsumer second = (a, b, c) -> results.add(a * b * c);

        final LongTriConsumer chained = first.andThen(second);
        chained.accept(2L, 3L, 4L);

        assertEquals(2, results.size());
        assertEquals(9L, results.get(0)); // 2+3+4
        assertEquals(24L, results.get(1)); // 2*3*4
    }

    @Test
    public void testAndThen_orderOfExecution() {
        final List<String> executionOrder = new ArrayList<>();
        final LongTriConsumer first = (a, b, c) -> executionOrder.add("first");
        final LongTriConsumer second = (a, b, c) -> executionOrder.add("second");

        final LongTriConsumer chained = first.andThen(second);
        chained.accept(1L, 2L, 3L);

        assertEquals(2, executionOrder.size());
        assertEquals("first", executionOrder.get(0));
        assertEquals("second", executionOrder.get(1));
    }

    @Test
    public void testAccept_withNegativeValues() {
        final AtomicLong sum = new AtomicLong(0);
        final LongTriConsumer consumer = (a, b, c) -> sum.set(a + b + c);

        consumer.accept(-10L, -20L, -30L);

        assertEquals(-60L, sum.get());
    }

    @Test
    public void testAccept_withZeroValues() {
        final AtomicLong product = new AtomicLong(1);
        final LongTriConsumer consumer = (a, b, c) -> product.set(a * b * c);

        consumer.accept(0L, 5L, 10L);

        assertEquals(0L, product.get());
    }

    @Test
    public void testAccept_findMax() {
        final AtomicLong max = new AtomicLong(Long.MIN_VALUE);
        final LongTriConsumer consumer = (a, b, c) -> {
            long maxVal = Math.max(a, Math.max(b, c));
            max.set(maxVal);
        };

        consumer.accept(5L, 100L, 50L);

        assertEquals(100L, max.get());
    }

    @Test
    public void testAccept_sideEffects() {
        final long[] array = new long[3];
        final LongTriConsumer consumer = (a, b, c) -> {
            array[0] = a;
            array[1] = b;
            array[2] = c;
        };

        consumer.accept(10L, 20L, 30L);

        assertEquals(10L, array[0]);
        assertEquals(20L, array[1]);
        assertEquals(30L, array[2]);
    }

    @Test
    public void testFunctionalInterfaceContract() {
        assertDoesNotThrow(() -> {
            final LongTriConsumer consumer = (a, b, c) -> { };
            consumer.accept(1L, 2L, 3L);
        });
    }

    @Test
    public void testMethodReference() {
        final List<Long> results = new ArrayList<>();
        final LongTriConsumer consumer = this::processThreeValues;

        consumer.accept(1L, 2L, 3L);
    }

    private void processThreeValues(final long a, final long b, final long c) {
        // Process values (empty for testing)
    }
}
