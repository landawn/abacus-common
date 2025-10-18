package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class FloatBiConsumer2025Test extends TestBase {

    @Test
    public void testAccept() {
        final List<Float> results = new ArrayList<>();
        final FloatBiConsumer consumer = (t, u) -> results.add(t + u);

        consumer.accept(10.5f, 20.3f);

        assertEquals(1, results.size());
        assertEquals(30.8f, results.get(0), 0.001f);
    }

    @Test
    public void testAccept_withLambda() {
        final AtomicReference<Float> result = new AtomicReference<>(0f);
        final FloatBiConsumer consumer = (a, b) -> result.set(a * b);

        consumer.accept(5.5f, 2.0f);

        assertEquals(11.0f, result.get(), 0.001f);
    }

    @Test
    public void testAccept_withAnonymousClass() {
        final List<String> results = new ArrayList<>();
        final FloatBiConsumer consumer = new FloatBiConsumer() {
            @Override
            public void accept(final float t, final float u) {
                results.add(String.format("%.1f,%.1f", t, u));
            }
        };

        consumer.accept(10.5f, 20.3f);

        assertEquals(1, results.size());
        assertEquals("10.5,20.3", results.get(0));
    }

    @Test
    public void testAndThen() {
        final List<Float> results = new ArrayList<>();
        final FloatBiConsumer first = (t, u) -> results.add(t + u);
        final FloatBiConsumer second = (t, u) -> results.add(t - u);

        final FloatBiConsumer chained = first.andThen(second);
        chained.accept(20.5f, 5.3f);

        assertEquals(2, results.size());
        assertEquals(25.8f, results.get(0), 0.001f);
        assertEquals(15.2f, results.get(1), 0.001f);
    }

    @Test
    public void testAndThen_orderOfExecution() {
        final List<String> executionOrder = new ArrayList<>();
        final FloatBiConsumer first = (t, u) -> executionOrder.add("first");
        final FloatBiConsumer second = (t, u) -> executionOrder.add("second");

        final FloatBiConsumer chained = first.andThen(second);
        chained.accept(1.0f, 2.0f);

        assertEquals(2, executionOrder.size());
        assertEquals("first", executionOrder.get(0));
        assertEquals("second", executionOrder.get(1));
    }

    @Test
    public void testAccept_withNegativeValues() {
        final AtomicReference<Float> result = new AtomicReference<>(0f);
        final FloatBiConsumer consumer = (t, u) -> result.set(t + u);

        consumer.accept(-10.5f, -20.3f);

        assertEquals(-30.8f, result.get(), 0.001f);
    }

    @Test
    public void testAccept_withZeroValues() {
        final AtomicReference<Float> result = new AtomicReference<>(1f);
        final FloatBiConsumer consumer = (t, u) -> result.set(t + u);

        consumer.accept(0f, 0f);

        assertEquals(0f, result.get(), 0.001f);
    }

    @Test
    public void testFunctionalInterfaceContract() {
        assertDoesNotThrow(() -> {
            final FloatBiConsumer consumer = (t, u) -> { };
            consumer.accept(1.0f, 2.0f);
        });
    }
}
