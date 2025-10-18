package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Throwables;

@Tag("2025")
public class BiConsumer2025Test extends TestBase {

    @Test
    public void testAccept() {
        List<String> results = new ArrayList<>();
        BiConsumer<String, Integer> consumer = (s, i) -> results.add(s + ":" + i);

        consumer.accept("test", 5);

        assertEquals(1, results.size());
        assertEquals("test:5", results.get(0));
    }

    @Test
    public void testAcceptWithLambda() {
        List<String> results = new ArrayList<>();
        BiConsumer<String, String> consumer = (s1, s2) -> results.add(s1 + s2);

        consumer.accept("Hello", "World");

        assertEquals(1, results.size());
        assertEquals("HelloWorld", results.get(0));
    }

    @Test
    public void testAcceptWithMethodReference() {
        List<String> results = new ArrayList<>();
        BiConsumer<List<String>, String> consumer = List::add;

        consumer.accept(results, "item");

        assertEquals(1, results.size());
        assertEquals("item", results.get(0));
    }

    @Test
    public void testAndThen() {
        List<String> results = new ArrayList<>();
        BiConsumer<String, Integer> consumer1 = (s, i) -> results.add(s + ":" + i);
        BiConsumer<String, Integer> consumer2 = (s, i) -> results.add(s.toUpperCase() + ":" + (i * 2));

        BiConsumer<String, Integer> chainedConsumer = consumer1.andThen(consumer2);
        chainedConsumer.accept("test", 5);

        assertEquals(2, results.size());
        assertEquals("test:5", results.get(0));
        assertEquals("TEST:10", results.get(1));
    }

    @Test
    public void testAndThenMultipleChains() {
        List<Integer> results = new ArrayList<>();
        BiConsumer<Integer, Integer> consumer1 = (a, b) -> results.add(a + b);
        BiConsumer<Integer, Integer> consumer2 = (a, b) -> results.add(a - b);
        BiConsumer<Integer, Integer> consumer3 = (a, b) -> results.add(a * b);

        BiConsumer<Integer, Integer> chainedConsumer = consumer1.andThen(consumer2).andThen(consumer3);
        chainedConsumer.accept(10, 5);

        assertEquals(3, results.size());
        assertEquals(15, results.get(0));  // 10 + 5
        assertEquals(5, results.get(1));   // 10 - 5
        assertEquals(50, results.get(2));  // 10 * 5
    }

    @Test
    public void testAndThenWithException() {
        List<String> results = new ArrayList<>();
        BiConsumer<String, Integer> consumer1 = (s, i) -> results.add(s + ":" + i);
        BiConsumer<String, Integer> consumer2 = (s, i) -> {
            throw new RuntimeException("Test exception");
        };

        BiConsumer<String, Integer> chainedConsumer = consumer1.andThen(consumer2);

        assertThrows(RuntimeException.class, () -> chainedConsumer.accept("test", 5));
        assertEquals(1, results.size());  // First consumer should have executed
        assertEquals("test:5", results.get(0));
    }

    @Test
    public void testAndThenFirstThrowsException() {
        List<String> results = new ArrayList<>();
        BiConsumer<String, Integer> consumer1 = (s, i) -> {
            throw new RuntimeException("First exception");
        };
        BiConsumer<String, Integer> consumer2 = (s, i) -> results.add(s + ":" + i);

        BiConsumer<String, Integer> chainedConsumer = consumer1.andThen(consumer2);

        assertThrows(RuntimeException.class, () -> chainedConsumer.accept("test", 5));
        assertEquals(0, results.size());  // Second consumer should not have executed
    }

    @Test
    public void testToThrowable() {
        BiConsumer<String, Integer> consumer = (s, i) -> {};
        Throwables.BiConsumer<String, Integer, ?> throwableConsumer = consumer.toThrowable();

        assertNotNull(throwableConsumer);
    }

    @Test
    public void testToThrowableWithExecution() throws Throwable {
        List<String> results = new ArrayList<>();
        BiConsumer<String, Integer> consumer = (s, i) -> results.add(s + ":" + i);
        Throwables.BiConsumer<String, Integer, ?> throwableConsumer = consumer.toThrowable();

        throwableConsumer.accept("test", 100);

        assertEquals(1, results.size());
        assertEquals("test:100", results.get(0));
    }

    @Test
    public void testAnonymousClass() {
        List<String> results = new ArrayList<>();
        BiConsumer<String, Integer> consumer = new BiConsumer<String, Integer>() {
            @Override
            public void accept(String s, Integer i) {
                results.add(s + ":" + i);
            }
        };

        consumer.accept("anonymous", 42);

        assertEquals(1, results.size());
        assertEquals("anonymous:42", results.get(0));
    }

    @Test
    public void testWithNullValues() {
        List<String> results = new ArrayList<>();
        BiConsumer<String, Integer> consumer = (s, i) -> results.add(String.valueOf(s) + ":" + String.valueOf(i));

        consumer.accept(null, null);

        assertEquals(1, results.size());
        assertEquals("null:null", results.get(0));
    }

    @Test
    public void testWithDifferentTypes() {
        List<String> results = new ArrayList<>();
        BiConsumer<Integer, Double> consumer = (i, d) -> results.add(i + ":" + d);

        consumer.accept(42, 3.14);

        assertEquals(1, results.size());
        assertEquals("42:3.14", results.get(0));
    }
}
