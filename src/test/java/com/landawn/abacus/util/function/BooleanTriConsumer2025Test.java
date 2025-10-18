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
public class BooleanTriConsumer2025Test extends TestBase {

    @Test
    public void testAccept() {
        List<String> results = new ArrayList<>();
        BooleanTriConsumer consumer = (a, b, c) -> results.add(a + "," + b + "," + c);

        consumer.accept(true, false, true);

        assertEquals(1, results.size());
        assertEquals("true,false,true", results.get(0));
    }

    @Test
    public void testAcceptWithLambda() {
        AtomicInteger trueCount = new AtomicInteger(0);
        BooleanTriConsumer consumer = (a, b, c) -> {
            if (a) trueCount.incrementAndGet();
            if (b) trueCount.incrementAndGet();
            if (c) trueCount.incrementAndGet();
        };

        consumer.accept(true, false, true);

        assertEquals(2, trueCount.get());
    }

    @Test
    public void testAndThen() {
        List<String> results = new ArrayList<>();
        BooleanTriConsumer consumer1 = (a, b, c) -> results.add("first");
        BooleanTriConsumer consumer2 = (a, b, c) -> results.add("second");

        BooleanTriConsumer chainedConsumer = consumer1.andThen(consumer2);
        chainedConsumer.accept(true, false, true);

        assertEquals(2, results.size());
        assertEquals("first", results.get(0));
        assertEquals("second", results.get(1));
    }

    @Test
    public void testAndThenWithException() {
        List<String> results = new ArrayList<>();
        BooleanTriConsumer consumer1 = (a, b, c) -> results.add("executed");
        BooleanTriConsumer consumer2 = (a, b, c) -> {
            throw new RuntimeException("Test exception");
        };

        BooleanTriConsumer chainedConsumer = consumer1.andThen(consumer2);

        assertThrows(RuntimeException.class, () -> chainedConsumer.accept(true, false, true));
        assertEquals(1, results.size());
    }

    @Test
    public void testAcceptWithException() {
        BooleanTriConsumer consumer = (a, b, c) -> {
            throw new RuntimeException("Test exception");
        };

        assertThrows(RuntimeException.class, () -> consumer.accept(true, false, true));
    }

    @Test
    public void testAnonymousClass() {
        List<String> results = new ArrayList<>();
        BooleanTriConsumer consumer = new BooleanTriConsumer() {
            @Override
            public void accept(boolean a, boolean b, boolean c) {
                results.add(String.format("a=%b,b=%b,c=%b", a, b, c));
            }
        };

        consumer.accept(true, false, true);

        assertEquals(1, results.size());
        assertEquals("a=true,b=false,c=true", results.get(0));
    }
}
