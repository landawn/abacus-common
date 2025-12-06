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
public class BooleanNConsumer2025Test extends TestBase {

    @Test
    public void testAccept() {
        List<String> results = new ArrayList<>();
        BooleanNConsumer consumer = args -> {
            StringBuilder sb = new StringBuilder();
            for (boolean arg : args) {
                sb.append(arg).append(",");
            }
            results.add(sb.toString());
        };

        consumer.accept(true, false, true);

        assertEquals(1, results.size());
        assertEquals("true,false,true,", results.get(0));
    }

    @Test
    public void testAcceptWithEmptyArray() {
        AtomicInteger callCount = new AtomicInteger(0);
        BooleanNConsumer consumer = args -> callCount.incrementAndGet();

        consumer.accept();

        assertEquals(1, callCount.get());
    }

    @Test
    public void testAcceptWithSingleElement() {
        List<Boolean> results = new ArrayList<>();
        BooleanNConsumer consumer = args -> {
            for (boolean arg : args) {
                results.add(arg);
            }
        };

        consumer.accept(true);

        assertEquals(1, results.size());
        assertEquals(true, results.get(0));
    }

    @Test
    public void testAcceptWithMultipleElements() {
        AtomicInteger trueCount = new AtomicInteger(0);
        BooleanNConsumer consumer = args -> {
            for (boolean arg : args) {
                if (arg)
                    trueCount.incrementAndGet();
            }
        };

        consumer.accept(true, false, true, true, false);

        assertEquals(3, trueCount.get());
    }

    @Test
    public void testAndThen() {
        List<String> results = new ArrayList<>();
        BooleanNConsumer consumer1 = args -> results.add("first:" + args.length);
        BooleanNConsumer consumer2 = args -> results.add("second:" + args.length);

        BooleanNConsumer chainedConsumer = consumer1.andThen(consumer2);
        chainedConsumer.accept(true, false, true);

        assertEquals(2, results.size());
        assertEquals("first:3", results.get(0));
        assertEquals("second:3", results.get(1));
    }

    @Test
    public void testAndThenMultipleChains() {
        List<String> results = new ArrayList<>();
        BooleanNConsumer consumer1 = args -> results.add("1");
        BooleanNConsumer consumer2 = args -> results.add("2");
        BooleanNConsumer consumer3 = args -> results.add("3");

        BooleanNConsumer chainedConsumer = consumer1.andThen(consumer2).andThen(consumer3);
        chainedConsumer.accept(true, false);

        assertEquals(3, results.size());
        assertEquals("1", results.get(0));
        assertEquals("2", results.get(1));
        assertEquals("3", results.get(2));
    }

    @Test
    public void testAndThenWithException() {
        List<String> results = new ArrayList<>();
        BooleanNConsumer consumer1 = args -> results.add("executed");
        BooleanNConsumer consumer2 = args -> {
            throw new RuntimeException("Test exception");
        };

        BooleanNConsumer chainedConsumer = consumer1.andThen(consumer2);

        assertThrows(RuntimeException.class, () -> chainedConsumer.accept(true, false));
        assertEquals(1, results.size());   // First consumer should have executed
    }

    @Test
    public void testAcceptWithException() {
        BooleanNConsumer consumer = args -> {
            throw new RuntimeException("Test exception");
        };

        assertThrows(RuntimeException.class, () -> consumer.accept(true));
    }

    @Test
    public void testAnonymousClass() {
        List<Integer> results = new ArrayList<>();
        BooleanNConsumer consumer = new BooleanNConsumer() {
            @Override
            public void accept(boolean... args) {
                results.add(args.length);
            }
        };

        consumer.accept(true, false, true, false, true);

        assertEquals(1, results.size());
        assertEquals(5, results.get(0));
    }

    @Test
    public void testCountingTrueAndFalse() {
        AtomicInteger trueCount = new AtomicInteger(0);
        AtomicInteger falseCount = new AtomicInteger(0);

        BooleanNConsumer consumer = args -> {
            for (boolean arg : args) {
                if (arg) {
                    trueCount.incrementAndGet();
                } else {
                    falseCount.incrementAndGet();
                }
            }
        };

        consumer.accept(true, false, true, true, false, false);

        assertEquals(3, trueCount.get());
        assertEquals(3, falseCount.get());
    }

    @Test
    public void testVarArgs() {
        List<String> results = new ArrayList<>();
        BooleanNConsumer consumer = args -> results.add("count:" + args.length);

        consumer.accept();
        consumer.accept(true);
        consumer.accept(true, false);
        consumer.accept(true, false, true, false);

        assertEquals(4, results.size());
        assertEquals("count:0", results.get(0));
        assertEquals("count:1", results.get(1));
        assertEquals("count:2", results.get(2));
        assertEquals("count:4", results.get(3));
    }
}
