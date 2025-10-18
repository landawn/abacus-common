package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class BooleanBiConsumer2025Test extends TestBase {

    @Test
    public void testAccept() {
        List<String> results = new ArrayList<>();
        BooleanBiConsumer consumer = (t, u) -> results.add(t + "," + u);

        consumer.accept(true, false);

        assertEquals(1, results.size());
        assertEquals("true,false", results.get(0));
    }

    @Test
    public void testAcceptWithLambda() {
        AtomicInteger counter = new AtomicInteger(0);
        BooleanBiConsumer consumer = (t, u) -> {
            if (t && u) counter.incrementAndGet();
        };

        consumer.accept(true, true);
        consumer.accept(true, false);
        consumer.accept(false, true);

        assertEquals(1, counter.get());
    }

    @Test
    public void testAcceptAllCombinations() {
        List<String> results = new ArrayList<>();
        BooleanBiConsumer consumer = (t, u) -> results.add(String.format("t=%s,u=%s", t, u));

        consumer.accept(true, true);
        consumer.accept(true, false);
        consumer.accept(false, true);
        consumer.accept(false, false);

        assertEquals(4, results.size());
        assertEquals("t=true,u=true", results.get(0));
        assertEquals("t=true,u=false", results.get(1));
        assertEquals("t=false,u=true", results.get(2));
        assertEquals("t=false,u=false", results.get(3));
    }

    @Test
    public void testAndThen() {
        List<String> results = new ArrayList<>();
        BooleanBiConsumer consumer1 = (t, u) -> results.add("first:" + (t && u));
        BooleanBiConsumer consumer2 = (t, u) -> results.add("second:" + (t || u));

        BooleanBiConsumer chainedConsumer = consumer1.andThen(consumer2);
        chainedConsumer.accept(true, false);

        assertEquals(2, results.size());
        assertEquals("first:false", results.get(0));
        assertEquals("second:true", results.get(1));
    }

    @Test
    public void testAndThenMultipleChains() {
        List<String> results = new ArrayList<>();
        BooleanBiConsumer consumer1 = (t, u) -> results.add("1:" + t);
        BooleanBiConsumer consumer2 = (t, u) -> results.add("2:" + u);
        BooleanBiConsumer consumer3 = (t, u) -> results.add("3:" + (t == u));

        BooleanBiConsumer chainedConsumer = consumer1.andThen(consumer2).andThen(consumer3);
        chainedConsumer.accept(true, true);

        assertEquals(3, results.size());
        assertEquals("1:true", results.get(0));
        assertEquals("2:true", results.get(1));
        assertEquals("3:true", results.get(2));
    }

    @Test
    public void testAndThenWithException() {
        List<String> results = new ArrayList<>();
        BooleanBiConsumer consumer1 = (t, u) -> results.add("executed");
        BooleanBiConsumer consumer2 = (t, u) -> {
            throw new RuntimeException("Test exception");
        };

        BooleanBiConsumer chainedConsumer = consumer1.andThen(consumer2);

        assertThrows(RuntimeException.class, () -> chainedConsumer.accept(true, false));
        assertEquals(1, results.size());  // First consumer should have executed
    }

    @Test
    public void testAcceptWithException() {
        BooleanBiConsumer consumer = (t, u) -> {
            throw new RuntimeException("Test exception");
        };

        assertThrows(RuntimeException.class, () -> consumer.accept(true, false));
    }

    @Test
    public void testAnonymousClass() {
        List<String> results = new ArrayList<>();
        BooleanBiConsumer consumer = new BooleanBiConsumer() {
            @Override
            public void accept(boolean t, boolean u) {
                results.add(String.valueOf(t) + "-" + String.valueOf(u));
            }
        };

        consumer.accept(false, true);

        assertEquals(1, results.size());
        assertEquals("false-true", results.get(0));
    }

    @Test
    public void testLogicalOperations() {
        List<String> results = new ArrayList<>();
        BooleanBiConsumer consumer = (t, u) -> {
            results.add("AND:" + (t && u));
            results.add("OR:" + (t || u));
            results.add("XOR:" + (t != u));
        };

        consumer.accept(true, false);

        assertEquals(3, results.size());
        assertEquals("AND:false", results.get(0));
        assertEquals("OR:true", results.get(1));
        assertEquals("XOR:true", results.get(2));
    }

    @Test
    public void testCountingPattern() {
        AtomicInteger trueCount = new AtomicInteger(0);
        AtomicInteger falseCount = new AtomicInteger(0);

        BooleanBiConsumer consumer = (t, u) -> {
            if (t) trueCount.incrementAndGet();
            if (u) trueCount.incrementAndGet();
            if (!t) falseCount.incrementAndGet();
            if (!u) falseCount.incrementAndGet();
        };

        consumer.accept(true, false);
        consumer.accept(true, true);
        consumer.accept(false, false);

        assertEquals(3, trueCount.get());   // true,true,true from the three calls
        assertEquals(3, falseCount.get());  // false,false,false from the three calls
    }
}
