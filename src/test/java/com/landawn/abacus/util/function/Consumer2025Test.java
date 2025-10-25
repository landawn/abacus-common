package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class Consumer2025Test extends TestBase {

    @Test
    public void testAccept() {
        List<String> results = new ArrayList<>();
        Consumer<String> consumer = s -> results.add(s);

        consumer.accept("test");

        assertEquals(1, results.size());
        assertEquals("test", results.get(0));
    }

    @Test
    public void testAccept_WithMethodReference() {
        List<String> results = new ArrayList<>();
        Consumer<String> consumer = results::add;

        consumer.accept("value");

        assertEquals(1, results.size());
        assertEquals("value", results.get(0));
    }

    @Test
    public void testAccept_WithAnonymousClass() {
        List<String> results = new ArrayList<>();
        Consumer<String> consumer = new Consumer<String>() {
            @Override
            public void accept(String t) {
                results.add(t.toUpperCase());
            }
        };

        consumer.accept("test");

        assertEquals(1, results.size());
        assertEquals("TEST", results.get(0));
    }

    @Test
    public void testAndThen() {
        List<String> results = new ArrayList<>();
        Consumer<String> consumer1 = s -> results.add("first:" + s);
        Consumer<String> consumer2 = s -> results.add("second:" + s);

        Consumer<String> chained = consumer1.andThen(consumer2);
        chained.accept("test");

        assertEquals(2, results.size());
        assertEquals("first:test", results.get(0));
        assertEquals("second:test", results.get(1));
    }

    @Test
    public void testAndThen_MultipleChains() {
        List<Integer> results = new ArrayList<>();
        Consumer<String> consumer1 = s -> results.add(1);
        Consumer<String> consumer2 = s -> results.add(2);
        Consumer<String> consumer3 = s -> results.add(3);

        Consumer<String> chained = consumer1.andThen(consumer2).andThen(consumer3);
        chained.accept("test");

        assertEquals(3, results.size());
        assertEquals(1, results.get(0));
        assertEquals(2, results.get(1));
        assertEquals(3, results.get(2));
    }

    @Test
    public void testToThrowable() {
        Consumer<String> consumer = s -> {
        };
        com.landawn.abacus.util.Throwables.Consumer<String, ?> throwableConsumer = consumer.toThrowable();
        assertNotNull(throwableConsumer);
    }

    @Test
    public void testJavaUtilFunctionCompatibility() {
        List<String> results = new ArrayList<>();
        Consumer<String> consumer = s -> results.add(s);
        java.util.function.Consumer<String> javaConsumer = consumer;

        javaConsumer.accept("test");

        assertEquals(1, results.size());
        assertEquals("test", results.get(0));
    }

    @Test
    public void testFunctionalInterfaceContract() {
        Consumer<String> lambda = s -> {
        };
        assertNotNull(lambda);
        assertDoesNotThrow(() -> lambda.accept("test"));
    }
}
