package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class BiIntObjConsumer2025Test extends TestBase {

    @Test
    public void testAccept() {
        AtomicReference<String> result = new AtomicReference<>();
        BiIntObjConsumer<String> consumer = (i, j, s) -> result.set(s + (i + j));

        consumer.accept(10, 20, "Sum: ");

        assertEquals("Sum: 30", result.get());
    }

    @Test
    public void testAcceptWithLambda() {
        List<String> results = new ArrayList<>();
        BiIntObjConsumer<String> consumer = (i, j, s) -> results.add(s + ":" + i + "," + j);

        consumer.accept(5, 10, "values");

        assertEquals(1, results.size());
        assertEquals("values:5,10", results.get(0));
    }

    @Test
    public void testAcceptWithMethodReference() {
        List<Integer> results = new ArrayList<>();
        BiIntObjConsumer<List<Integer>> consumer = (i, j, list) -> {
            list.add(i);
            list.add(j);
        };

        consumer.accept(42, 99, results);

        assertEquals(2, results.size());
        assertEquals(42, results.get(0));
        assertEquals(99, results.get(1));
    }

    @Test
    public void testAndThen() {
        List<String> results = new ArrayList<>();
        BiIntObjConsumer<String> consumer1 = (i, j, s) -> results.add(s + ":" + (i + j));
        BiIntObjConsumer<String> consumer2 = (i, j, s) -> results.add(s + ":" + (i * j));

        BiIntObjConsumer<String> chainedConsumer = consumer1.andThen(consumer2);
        chainedConsumer.accept(5, 3, "result");

        assertEquals(2, results.size());
        assertEquals("result:8", results.get(0)); // 5 + 3
        assertEquals("result:15", results.get(1)); // 5 * 3
    }

    @Test
    public void testAndThenMultipleChains() {
        List<String> results = new ArrayList<>();
        BiIntObjConsumer<String> consumer1 = (i, j, s) -> results.add("first:" + i);
        BiIntObjConsumer<String> consumer2 = (i, j, s) -> results.add("second:" + j);
        BiIntObjConsumer<String> consumer3 = (i, j, s) -> results.add("third:" + s);

        BiIntObjConsumer<String> chainedConsumer = consumer1.andThen(consumer2).andThen(consumer3);
        chainedConsumer.accept(10, 20, "test");

        assertEquals(3, results.size());
        assertEquals("first:10", results.get(0));
        assertEquals("second:20", results.get(1));
        assertEquals("third:test", results.get(2));
    }

    @Test
    public void testAndThenWithException() {
        List<String> results = new ArrayList<>();
        BiIntObjConsumer<String> consumer1 = (i, j, s) -> results.add(s);
        BiIntObjConsumer<String> consumer2 = (i, j, s) -> {
            throw new RuntimeException("Test exception");
        };

        BiIntObjConsumer<String> chainedConsumer = consumer1.andThen(consumer2);

        assertThrows(RuntimeException.class, () -> chainedConsumer.accept(5, 10, "test"));
        assertEquals(1, results.size()); // First consumer should have executed
        assertEquals("test", results.get(0));
    }

    @Test
    public void testAcceptWithException() {
        BiIntObjConsumer<String> consumer = (i, j, s) -> {
            throw new RuntimeException("Test exception");
        };

        assertThrows(RuntimeException.class, () -> consumer.accept(5, 10, "test"));
    }

    @Test
    public void testAnonymousClass() {
        List<String> results = new ArrayList<>();
        BiIntObjConsumer<String> consumer = new BiIntObjConsumer<String>() {
            @Override
            public void accept(int i, int j, String s) {
                results.add(i + "," + j + "," + s);
            }
        };

        consumer.accept(1, 2, "value");

        assertEquals(1, results.size());
        assertEquals("1,2,value", results.get(0));
    }

    @Test
    public void testWithNullObject() {
        List<String> results = new ArrayList<>();
        BiIntObjConsumer<String> consumer = (i, j, s) -> results.add(i + "," + j + "," + s);

        consumer.accept(5, 10, null);

        assertEquals(1, results.size());
        assertEquals("5,10,null", results.get(0));
    }

    @Test
    public void testWithNegativeInts() {
        AtomicReference<String> result = new AtomicReference<>();
        BiIntObjConsumer<String> consumer = (i, j, s) -> result.set(s + (i - j));

        consumer.accept(-5, 10, "diff: ");

        assertEquals("diff: -15", result.get());
    }

    @Test
    public void testComplexOperation() {
        StringBuilder sb = new StringBuilder();
        BiIntObjConsumer<String> consumer = (i, j, s) -> {
            sb.append(s).append(" [").append(i).append(",").append(j).append("] = ").append(i * j);
        };

        consumer.accept(7, 6, "Product:");

        assertEquals("Product: [7,6] = 42", sb.toString());
    }
}
