package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class BiObjIntConsumer2025Test extends TestBase {

    @Test
    public void testAccept() {
        List<String> results = new ArrayList<>();
        BiObjIntConsumer<String, String> consumer = (t, u, i) -> results.add(t + u + i);

        consumer.accept("Hello", "World", 42);

        assertEquals(1, results.size());
        assertEquals("HelloWorld42", results.get(0));
    }

    @Test
    public void testAcceptWithLambda() {
        List<String> results = new ArrayList<>();
        BiObjIntConsumer<String, Integer> consumer = (s, obj, i) -> results.add(s + ":" + obj + ":" + i);

        consumer.accept("test", 100, 5);

        assertEquals(1, results.size());
        assertEquals("test:100:5", results.get(0));
    }

    @Test
    public void testAcceptWithListModification() {
        List<String> list = new ArrayList<>();
        BiObjIntConsumer<List<String>, String> consumer = (l, s, i) -> {
            for (int j = 0; j < i; j++) {
                l.add(s);
            }
        };

        consumer.accept(list, "item", 3);

        assertEquals(3, list.size());
        assertEquals("item", list.get(0));
        assertEquals("item", list.get(1));
        assertEquals("item", list.get(2));
    }

    @Test
    public void testAndThen() {
        List<String> results = new ArrayList<>();
        BiObjIntConsumer<String, String> consumer1 = (t, u, i) -> results.add(t + u);
        BiObjIntConsumer<String, String> consumer2 = (t, u, i) -> results.add(String.valueOf(i));

        BiObjIntConsumer<String, String> chainedConsumer = consumer1.andThen(consumer2);
        chainedConsumer.accept("Hello", "World", 42);

        assertEquals(2, results.size());
        assertEquals("HelloWorld", results.get(0));
        assertEquals("42", results.get(1));
    }

    @Test
    public void testAndThenMultipleChains() {
        List<String> results = new ArrayList<>();
        BiObjIntConsumer<String, String> consumer1 = (t, u, i) -> results.add("first:" + t);
        BiObjIntConsumer<String, String> consumer2 = (t, u, i) -> results.add("second:" + u);
        BiObjIntConsumer<String, String> consumer3 = (t, u, i) -> results.add("third:" + i);

        BiObjIntConsumer<String, String> chainedConsumer = consumer1.andThen(consumer2).andThen(consumer3);
        chainedConsumer.accept("A", "B", 123);

        assertEquals(3, results.size());
        assertEquals("first:A", results.get(0));
        assertEquals("second:B", results.get(1));
        assertEquals("third:123", results.get(2));
    }

    @Test
    public void testAndThenWithException() {
        List<String> results = new ArrayList<>();
        BiObjIntConsumer<String, String> consumer1 = (t, u, i) -> results.add(t);
        BiObjIntConsumer<String, String> consumer2 = (t, u, i) -> {
            throw new RuntimeException("Test exception");
        };

        BiObjIntConsumer<String, String> chainedConsumer = consumer1.andThen(consumer2);

        assertThrows(RuntimeException.class, () -> chainedConsumer.accept("test", "value", 5));
        assertEquals(1, results.size()); // First consumer should have executed
        assertEquals("test", results.get(0));
    }

    @Test
    public void testAcceptWithException() {
        BiObjIntConsumer<String, String> consumer = (t, u, i) -> {
            throw new RuntimeException("Test exception");
        };

        assertThrows(RuntimeException.class, () -> consumer.accept("test", "value", 5));
    }

    @Test
    public void testAnonymousClass() {
        List<String> results = new ArrayList<>();
        BiObjIntConsumer<String, String> consumer = new BiObjIntConsumer<String, String>() {
            @Override
            public void accept(String t, String u, int i) {
                results.add(t + "-" + u + "-" + i);
            }
        };

        consumer.accept("foo", "bar", 99);

        assertEquals(1, results.size());
        assertEquals("foo-bar-99", results.get(0));
    }

    @Test
    public void testWithNullObjects() {
        List<String> results = new ArrayList<>();
        BiObjIntConsumer<String, String> consumer = (t, u, i) -> results.add(String.valueOf(t) + "," + String.valueOf(u) + "," + i);

        consumer.accept(null, null, 42);

        assertEquals(1, results.size());
        assertEquals("null,null,42", results.get(0));
    }

    @Test
    public void testWithNegativeInt() {
        List<String> results = new ArrayList<>();
        BiObjIntConsumer<String, String> consumer = (t, u, i) -> results.add(i + ":" + t + u);

        consumer.accept("A", "B", -10);

        assertEquals(1, results.size());
        assertEquals("-10:AB", results.get(0));
    }

    @Test
    public void testComplexOperation() {
        StringBuilder sb = new StringBuilder();
        BiObjIntConsumer<String, String> consumer = (t, u, i) -> {
            sb.append("[").append(t).append(", ").append(u).append("] repeated ").append(i).append(" times: ");
            for (int j = 0; j < i; j++) {
                sb.append(t).append(u);
                if (j < i - 1)
                    sb.append(", ");
            }
        };

        consumer.accept("X", "Y", 3);

        assertEquals("[X, Y] repeated 3 times: XY, XY, XY", sb.toString());
    }
}
