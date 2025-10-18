package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class NConsumer2025Test extends TestBase {

    @Test
    public void testAccept() {
        final List<String> result = new ArrayList<>();
        NConsumer<String> consumer = args -> {
            for (String s : args) {
                result.add(s);
            }
        };

        consumer.accept("a", "b", "c");

        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
        assertEquals("c", result.get(2));
    }

    @Test
    public void testAcceptWithNoArguments() {
        final List<String> result = new ArrayList<>();
        NConsumer<String> consumer = args -> {
            for (String s : args) {
                result.add(s);
            }
        };

        consumer.accept();

        assertEquals(0, result.size());
    }

    @Test
    public void testAcceptWithSingleArgument() {
        final List<String> result = new ArrayList<>();
        NConsumer<String> consumer = args -> {
            for (String s : args) {
                result.add(s);
            }
        };

        consumer.accept("single");

        assertEquals(1, result.size());
        assertEquals("single", result.get(0));
    }

    @Test
    public void testAcceptWithLambda() {
        final int[] sum = {0};
        NConsumer<Integer> consumer = args -> {
            for (Integer n : args) {
                sum[0] += n;
            }
        };

        consumer.accept(1, 2, 3, 4, 5);

        assertEquals(15, sum[0]);
    }

    @Test
    public void testAcceptWithAnonymousClass() {
        final List<String> result = new ArrayList<>();
        NConsumer<String> consumer = new NConsumer<String>() {
            @Override
            public void accept(String... args) {
                for (String s : args) {
                    result.add(s.toUpperCase());
                }
            }
        };

        consumer.accept("hello", "world");

        assertEquals(2, result.size());
        assertEquals("HELLO", result.get(0));
        assertEquals("WORLD", result.get(1));
    }

    @Test
    public void testAndThen() {
        final List<String> result = new ArrayList<>();
        NConsumer<String> first = args -> result.add("first:" + args.length);
        NConsumer<String> second = args -> result.add("second:" + args.length);

        NConsumer<String> combined = first.andThen(second);
        combined.accept("a", "b", "c");

        assertEquals(2, result.size());
        assertEquals("first:3", result.get(0));
        assertEquals("second:3", result.get(1));
    }

    @Test
    public void testAndThenChaining() {
        final List<String> result = new ArrayList<>();
        NConsumer<String> first = args -> result.add("first");
        NConsumer<String> second = args -> result.add("second");
        NConsumer<String> third = args -> result.add("third");

        NConsumer<String> combined = first.andThen(second).andThen(third);
        combined.accept("a");

        assertEquals(3, result.size());
        assertEquals("first", result.get(0));
        assertEquals("second", result.get(1));
        assertEquals("third", result.get(2));
    }

    @Test
    public void testWithMultipleTypes() {
        final List<Integer> result = new ArrayList<>();
        NConsumer<Integer> consumer = args -> {
            for (Integer n : args) {
                result.add(n * 2);
            }
        };

        consumer.accept(1, 2, 3, 4);

        assertEquals(4, result.size());
        assertEquals(2, result.get(0));
        assertEquals(4, result.get(1));
        assertEquals(6, result.get(2));
        assertEquals(8, result.get(3));
    }

    @Test
    public void testWithNullValues() {
        final List<String> result = new ArrayList<>();
        NConsumer<String> consumer = args -> {
            for (String s : args) {
                result.add(s);
            }
        };

        consumer.accept("a", null, "b");

        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals(null, result.get(1));
        assertEquals("b", result.get(2));
    }

    @Test
    public void testSideEffects() {
        final int[] counter = {0};
        NConsumer<String> consumer = args -> counter[0] = args.length;

        consumer.accept("a", "b", "c", "d", "e");

        assertEquals(5, counter[0]);
    }

    @Test
    public void testMethodReference() {
        final List<String> result = new ArrayList<>();
        NConsumer<String> consumer = args -> {
            for (String s : args) {
                result.add(s);
            }
        };

        consumer.accept("test1", "test2");

        assertEquals(2, result.size());
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(NConsumer.class.getAnnotation(FunctionalInterface.class));
    }
}
