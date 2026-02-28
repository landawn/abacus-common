package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class BooleanConsumer2025Test extends TestBase {

    @Test
    public void testAccept() {
        List<Boolean> results = new ArrayList<>();
        BooleanConsumer consumer = results::add;

        consumer.accept(true);
        consumer.accept(false);

        assertEquals(2, results.size());
        assertEquals(true, results.get(0));
        assertEquals(false, results.get(1));
    }

    @Test
    public void testAcceptWithLambda() {
        AtomicBoolean flag = new AtomicBoolean(false);
        BooleanConsumer consumer = value -> flag.set(value);

        consumer.accept(true);
        assertTrue(flag.get());
    }

    @Test
    public void testAcceptWithCounter() {
        AtomicInteger trueCount = new AtomicInteger(0);
        BooleanConsumer consumer = value -> {
            if (value)
                trueCount.incrementAndGet();
        };

        consumer.accept(true);
        consumer.accept(false);
        consumer.accept(true);

        assertEquals(2, trueCount.get());
    }

    @Test
    public void testAndThen() {
        List<String> results = new ArrayList<>();
        BooleanConsumer consumer1 = value -> results.add("first:" + value);
        BooleanConsumer consumer2 = value -> results.add("second:" + value);

        BooleanConsumer chainedConsumer = consumer1.andThen(consumer2);
        chainedConsumer.accept(true);

        assertEquals(2, results.size());
        assertEquals("first:true", results.get(0));
        assertEquals("second:true", results.get(1));
    }

    @Test
    public void testAndThenMultipleChains() {
        List<String> results = new ArrayList<>();
        BooleanConsumer consumer1 = value -> results.add("1");
        BooleanConsumer consumer2 = value -> results.add("2");
        BooleanConsumer consumer3 = value -> results.add("3");

        BooleanConsumer chainedConsumer = consumer1.andThen(consumer2).andThen(consumer3);
        chainedConsumer.accept(true);

        assertEquals(3, results.size());
        assertEquals("1", results.get(0));
        assertEquals("2", results.get(1));
        assertEquals("3", results.get(2));
    }

    @Test
    public void testAndThenWithException() {
        List<String> results = new ArrayList<>();
        BooleanConsumer consumer1 = value -> results.add("executed");
        BooleanConsumer consumer2 = value -> {
            throw new RuntimeException("Test exception");
        };

        BooleanConsumer chainedConsumer = consumer1.andThen(consumer2);

        assertThrows(RuntimeException.class, () -> chainedConsumer.accept(true));
        assertEquals(1, results.size()); // First consumer should have executed
    }

    @Test
    public void testAcceptWithException() {
        BooleanConsumer consumer = value -> {
            throw new RuntimeException("Test exception");
        };

        assertThrows(RuntimeException.class, () -> consumer.accept(true));
    }

    @Test
    public void testAnonymousClass() {
        List<String> results = new ArrayList<>();
        BooleanConsumer consumer = new BooleanConsumer() {
            @Override
            public void accept(boolean value) {
                results.add(value ? "YES" : "NO");
            }
        };

        consumer.accept(true);
        consumer.accept(false);

        assertEquals(2, results.size());
        assertEquals("YES", results.get(0));
        assertEquals("NO", results.get(1));
    }

    @Test
    public void testConditionalLogic() {
        List<String> results = new ArrayList<>();
        BooleanConsumer consumer = value -> {
            if (value) {
                results.add("positive");
            } else {
                results.add("negative");
            }
        };

        consumer.accept(true);
        consumer.accept(false);

        assertEquals("positive", results.get(0));
        assertEquals("negative", results.get(1));
    }

    @Test
    public void testMultipleAcceptCalls() {
        AtomicInteger callCount = new AtomicInteger(0);
        BooleanConsumer consumer = value -> callCount.incrementAndGet();

        for (int i = 0; i < 5; i++) {
            consumer.accept(i % 2 == 0);
        }

        assertEquals(5, callCount.get());
    }
}
