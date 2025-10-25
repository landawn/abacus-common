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
public class FloatConsumer2025Test extends TestBase {

    @Test
    public void testAccept() {
        final AtomicReference<Float> result = new AtomicReference<>(0f);
        final FloatConsumer consumer = result::set;

        consumer.accept(42.5f);

        assertEquals(42.5f, result.get(), 0.001f);
    }

    @Test
    public void testAccept_withLambda() {
        final List<Float> results = new ArrayList<>();
        final FloatConsumer consumer = results::add;

        consumer.accept(10.5f);

        assertEquals(1, results.size());
        assertEquals(10.5f, results.get(0), 0.001f);
    }

    @Test
    public void testAndThen() {
        final List<Float> results = new ArrayList<>();
        final FloatConsumer first = results::add;
        final FloatConsumer second = val -> results.add(val * 2);

        final FloatConsumer chained = first.andThen(second);
        chained.accept(10.5f);

        assertEquals(2, results.size());
        assertEquals(10.5f, results.get(0), 0.001f);
        assertEquals(21.0f, results.get(1), 0.001f);
    }

    @Test
    public void testAccept_withNegativeValue() {
        final AtomicReference<Float> result = new AtomicReference<>(0f);
        final FloatConsumer consumer = result::set;

        consumer.accept(-42.5f);

        assertEquals(-42.5f, result.get(), 0.001f);
    }

    @Test
    public void testAccept_withZero() {
        final AtomicReference<Float> result = new AtomicReference<>(10.0f);
        final FloatConsumer consumer = result::set;

        consumer.accept(0f);

        assertEquals(0f, result.get(), 0.001f);
    }

    @Test
    public void testFunctionalInterfaceContract() {
        assertDoesNotThrow(() -> {
            final FloatConsumer consumer = val -> {
            };
            consumer.accept(1.0f);
        });
    }
}
