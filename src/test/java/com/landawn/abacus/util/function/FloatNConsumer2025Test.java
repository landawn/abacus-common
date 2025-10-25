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
public class FloatNConsumer2025Test extends TestBase {

    @Test
    public void testAccept_noArgs() {
        final AtomicReference<Integer> counter = new AtomicReference<>(0);
        final FloatNConsumer consumer = args -> counter.set(args.length);

        consumer.accept();

        assertEquals(0, counter.get());
    }

    @Test
    public void testAccept_singleArg() {
        final AtomicReference<Float> result = new AtomicReference<>(0f);
        final FloatNConsumer consumer = args -> result.set(args[0]);

        consumer.accept(42.5f);

        assertEquals(42.5f, result.get(), 0.001f);
    }

    @Test
    public void testAccept_multipleArgs() {
        final AtomicReference<Float> sum = new AtomicReference<>(0f);
        final FloatNConsumer consumer = args -> {
            float total = 0;
            for (final float arg : args) {
                total += arg;
            }
            sum.set(total);
        };

        consumer.accept(10.5f, 20.3f, 30.2f, 40.0f);

        assertEquals(101.0f, sum.get(), 0.001f);
    }

    @Test
    public void testAndThen() {
        final List<String> results = new ArrayList<>();
        final FloatNConsumer first = args -> results.add("first:" + args.length);
        final FloatNConsumer second = args -> results.add("second:" + args.length);

        final FloatNConsumer chained = first.andThen(second);
        chained.accept(1.0f, 2.0f, 3.0f);

        assertEquals(2, results.size());
        assertEquals("first:3", results.get(0));
        assertEquals("second:3", results.get(1));
    }

    @Test
    public void testFunctionalInterfaceContract() {
        assertDoesNotThrow(() -> {
            final FloatNConsumer consumer = args -> {
            };
            consumer.accept(1.0f, 2.0f, 3.0f);
        });
    }
}
