package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class LongObjConsumer2025Test extends TestBase {

    @Test
    public void testAccept() {
        final List<String> result = new ArrayList<>();
        LongObjConsumer<String> consumer = (l, s) -> result.add(l + ":" + s);

        consumer.accept(5L, "test");
        consumer.accept(10L, "value");

        assertEquals(2, result.size());
        assertEquals("5:test", result.get(0));
        assertEquals("10:value", result.get(1));
    }

    @Test
    public void testAcceptWithLambda() {
        final long[] longResult = new long[1];
        final String[] stringResult = new String[1];
        LongObjConsumer<String> consumer = (l, s) -> {
            longResult[0] = l * 2;
            stringResult[0] = s.toUpperCase();
        };

        consumer.accept(7L, "hello");

        assertEquals(14L, longResult[0]);
        assertEquals("HELLO", stringResult[0]);
    }

    @Test
    public void testAcceptWithAnonymousClass() {
        final List<String> result = new ArrayList<>();
        LongObjConsumer<String> consumer = new LongObjConsumer<String>() {
            @Override
            public void accept(long i, String t) {
                result.add(String.format("%d-%s", i, t));
            }
        };

        consumer.accept(100L, "test");

        assertEquals(1, result.size());
        assertEquals("100-test", result.get(0));
    }

    @Test
    public void testAndThen() {
        final List<String> result = new ArrayList<>();
        LongObjConsumer<String> first = (l, s) -> result.add(l + ":" + s);
        LongObjConsumer<String> second = (l, s) -> result.add(s + ":" + l);

        LongObjConsumer<String> combined = first.andThen(second);
        combined.accept(5L, "test");

        assertEquals(2, result.size());
        assertEquals("5:test", result.get(0));
        assertEquals("test:5", result.get(1));
    }

    @Test
    public void testAndThenChaining() {
        final List<String> result = new ArrayList<>();
        LongObjConsumer<String> first = (l, s) -> result.add("first");
        LongObjConsumer<String> second = (l, s) -> result.add("second");
        LongObjConsumer<String> third = (l, s) -> result.add("third");

        LongObjConsumer<String> combined = first.andThen(second).andThen(third);
        combined.accept(5L, "test");

        assertEquals(3, result.size());
        assertEquals("first", result.get(0));
        assertEquals("second", result.get(1));
        assertEquals("third", result.get(2));
    }

    @Test
    public void testWithNegativeValues() {
        final List<String> result = new ArrayList<>();
        LongObjConsumer<String> consumer = (l, s) -> result.add(l + ":" + s);

        consumer.accept(-5L, "negative");
        consumer.accept(-100L, "value");

        assertEquals(2, result.size());
        assertEquals("-5:negative", result.get(0));
        assertEquals("-100:value", result.get(1));
    }

    @Test
    public void testWithBoundaryValues() {
        final List<String> result = new ArrayList<>();
        LongObjConsumer<String> consumer = (l, s) -> result.add(l + ":" + s);

        consumer.accept(Long.MAX_VALUE, "max");
        consumer.accept(Long.MIN_VALUE, "min");
        consumer.accept(0L, "zero");

        assertEquals(3, result.size());
        assertEquals(Long.MAX_VALUE + ":max", result.get(0));
        assertEquals(Long.MIN_VALUE + ":min", result.get(1));
        assertEquals("0:zero", result.get(2));
    }

    @Test
    public void testWithNullObject() {
        final List<String> result = new ArrayList<>();
        LongObjConsumer<String> consumer = (l, s) -> result.add(l + ":" + s);

        consumer.accept(5L, null);

        assertEquals(1, result.size());
        assertEquals("5:null", result.get(0));
    }

    @Test
    public void testWithDifferentObjectTypes() {
        final List<String> result = new ArrayList<>();
        LongObjConsumer<Integer> consumer = (l, i) -> result.add(l + ":" + i);

        consumer.accept(5L, 10);
        consumer.accept(7L, 20);

        assertEquals(2, result.size());
        assertEquals("5:10", result.get(0));
        assertEquals("7:20", result.get(1));
    }

    @Test
    public void testSideEffects() {
        final int[] counter = { 0 };
        LongObjConsumer<String> consumer = (l, s) -> counter[0]++;

        consumer.accept(1L, "a");
        consumer.accept(2L, "b");
        consumer.accept(3L, "c");

        assertEquals(3, counter[0]);
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(LongObjConsumer.class.getAnnotation(FunctionalInterface.class));
    }
}
