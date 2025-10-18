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
public class LongBiConsumer2025Test extends TestBase {

    @Test
    public void testAccept() {
        final List<Long> results = new ArrayList<>();
        final LongBiConsumer consumer = (t, u) -> results.add(t + u);

        consumer.accept(10L, 20L);

        assertEquals(1, results.size());
        assertEquals(30L, results.get(0));
    }

    @Test
    public void testAccept_withLambda() {
        final AtomicLong result = new AtomicLong(0);
        final LongBiConsumer consumer = (a, b) -> result.set(a * b);

        consumer.accept(5L, 7L);

        assertEquals(35L, result.get());
    }

    @Test
    public void testAccept_withAnonymousClass() {
        final List<String> results = new ArrayList<>();
        final LongBiConsumer consumer = new LongBiConsumer() {
            @Override
            public void accept(final long t, final long u) {
                results.add(t + "," + u);
            }
        };

        consumer.accept(100L, 200L);

        assertEquals(1, results.size());
        assertEquals("100,200", results.get(0));
    }

    @Test
    public void testAndThen() {
        final List<Long> results = new ArrayList<>();
        final LongBiConsumer first = (t, u) -> results.add(t + u);
        final LongBiConsumer second = (t, u) -> results.add(t - u);

        final LongBiConsumer chained = first.andThen(second);
        chained.accept(20L, 5L);

        assertEquals(2, results.size());
        assertEquals(25L, results.get(0));
        assertEquals(15L, results.get(1));
    }

    @Test
    public void testAndThen_multipleChains() {
        final List<Long> results = new ArrayList<>();
        final LongBiConsumer first = (t, u) -> results.add(t + u);
        final LongBiConsumer second = (t, u) -> results.add(t * u);
        final LongBiConsumer third = (t, u) -> results.add(Math.max(t, u));

        final LongBiConsumer chained = first.andThen(second).andThen(third);
        chained.accept(3L, 4L);

        assertEquals(3, results.size());
        assertEquals(7L, results.get(0));
        assertEquals(12L, results.get(1));
        assertEquals(4L, results.get(2));
    }

    @Test
    public void testAndThen_orderOfExecution() {
        final List<String> executionOrder = new ArrayList<>();
        final LongBiConsumer first = (t, u) -> executionOrder.add("first");
        final LongBiConsumer second = (t, u) -> executionOrder.add("second");

        final LongBiConsumer chained = first.andThen(second);
        chained.accept(1L, 2L);

        assertEquals(2, executionOrder.size());
        assertEquals("first", executionOrder.get(0));
        assertEquals("second", executionOrder.get(1));
    }

    @Test
    public void testAccept_withNegativeValues() {
        final AtomicLong result = new AtomicLong(0);
        final LongBiConsumer consumer = (t, u) -> result.set(t + u);

        consumer.accept(-10L, -20L);

        assertEquals(-30L, result.get());
    }

    @Test
    public void testAccept_withZeroValues() {
        final AtomicLong result = new AtomicLong(1);
        final LongBiConsumer consumer = (t, u) -> result.set(t + u);

        consumer.accept(0L, 0L);

        assertEquals(0L, result.get());
    }

    @Test
    public void testAccept_withMaxValues() {
        final List<Long> results = new ArrayList<>();
        final LongBiConsumer consumer = (t, u) -> results.add(Math.max(t, u));

        consumer.accept(Long.MAX_VALUE, Long.MIN_VALUE);

        assertEquals(1, results.size());
        assertEquals(Long.MAX_VALUE, results.get(0));
    }

    @Test
    public void testFunctionalInterfaceContract() {
        // Test that LongBiConsumer can be used as a functional interface
        assertDoesNotThrow(() -> {
            final LongBiConsumer consumer = (t, u) -> { };
            consumer.accept(1L, 2L);
        });
    }

    @Test
    public void testMethodReference() {
        final List<Long> results = new ArrayList<>();

        // Cannot directly use method reference for primitive types, but can use wrapper method
        final LongBiConsumer consumer = (t, u) -> addToList(results, t, u);
        consumer.accept(5L, 10L);

        assertEquals(1, results.size());
        assertEquals(15L, results.get(0));
    }

    private void addToList(final List<Long> list, final long a, final long b) {
        list.add(a + b);
    }

    @Test
    public void testAccept_sideEffects() {
        final long[] array = new long[2];
        final LongBiConsumer consumer = (t, u) -> {
            array[0] = t;
            array[1] = u;
        };

        consumer.accept(42L, 84L);

        assertEquals(42L, array[0]);
        assertEquals(84L, array[1]);
    }
}
