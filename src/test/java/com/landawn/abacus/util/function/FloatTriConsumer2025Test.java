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
public class FloatTriConsumer2025Test extends TestBase {

    @Test
    public void testAccept() {
        final AtomicReference<Float> sum = new AtomicReference<>(0f);
        final FloatTriConsumer consumer = (a, b, c) -> sum.set(a + b + c);

        consumer.accept(10.5f, 20.3f, 30.2f);

        assertEquals(61.0f, sum.get(), 0.001f);
    }

    @Test
    public void testAndThen() {
        final List<Float> results = new ArrayList<>();
        final FloatTriConsumer first = (a, b, c) -> results.add(a + b + c);
        final FloatTriConsumer second = (a, b, c) -> results.add(a * b * c);

        final FloatTriConsumer chained = first.andThen(second);
        chained.accept(2.0f, 3.0f, 4.0f);

        assertEquals(2, results.size());
        assertEquals(9.0f, results.get(0), 0.001f);   // 2+3+4
        assertEquals(24.0f, results.get(1), 0.001f);   // 2*3*4
    }

    @Test
    public void testFunctionalInterfaceContract() {
        assertDoesNotThrow(() -> {
            final FloatTriConsumer consumer = (a, b, c) -> {
            };
            consumer.accept(1.0f, 2.0f, 3.0f);
        });
    }
}
