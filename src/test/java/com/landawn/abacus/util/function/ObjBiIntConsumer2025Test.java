package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ObjBiIntConsumer2025Test extends TestBase {

    @Test
    public void testAccept() {
        final List<String> result = new ArrayList<>();
        ObjBiIntConsumer<String> consumer = (t, i, j) -> result.add(t + ":" + i + ":" + j);

        consumer.accept("test", 1, 2);
        consumer.accept("value", 3, 4);

        assertEquals(2, result.size());
        assertEquals("test:1:2", result.get(0));
        assertEquals("value:3:4", result.get(1));
    }

    @Test
    public void testAcceptWithLambda() {
        final StringBuilder result = new StringBuilder();
        ObjBiIntConsumer<String> consumer = (t, i, j) -> result.append(t.substring(i, j));

        consumer.accept("Hello World", 0, 5);

        assertEquals("Hello", result.toString());
    }

    @Test
    public void testAcceptWithAnonymousClass() {
        final List<String> result = new ArrayList<>();
        ObjBiIntConsumer<String> consumer = new ObjBiIntConsumer<String>() {
            @Override
            public void accept(String t, int i, int j) {
                result.add(String.format("%s[%d-%d]", t, i, j));
            }
        };

        consumer.accept("test", 10, 20);

        assertEquals(1, result.size());
        assertEquals("test[10-20]", result.get(0));
    }

    @Test
    public void testAndThen() {
        final List<String> result = new ArrayList<>();
        ObjBiIntConsumer<String> first = (t, i, j) -> result.add(t + ":" + i);
        ObjBiIntConsumer<String> second = (t, i, j) -> result.add(t + ":" + j);

        ObjBiIntConsumer<String> combined = first.andThen(second);
        combined.accept("test", 1, 2);

        assertEquals(2, result.size());
        assertEquals("test:1", result.get(0));
        assertEquals("test:2", result.get(1));
    }

    @Test
    public void testAndThenChaining() {
        final List<String> result = new ArrayList<>();
        ObjBiIntConsumer<String> first = (t, i, j) -> result.add("first");
        ObjBiIntConsumer<String> second = (t, i, j) -> result.add("second");
        ObjBiIntConsumer<String> third = (t, i, j) -> result.add("third");

        ObjBiIntConsumer<String> combined = first.andThen(second).andThen(third);
        combined.accept("test", 1, 2);

        assertEquals(3, result.size());
        assertEquals("first", result.get(0));
        assertEquals("second", result.get(1));
        assertEquals("third", result.get(2));
    }

    @Test
    public void testWithNegativeInts() {
        final List<String> result = new ArrayList<>();
        ObjBiIntConsumer<String> consumer = (t, i, j) -> result.add(t + ":" + i + ":" + j);

        consumer.accept("test", -5, -10);

        assertEquals(1, result.size());
        assertEquals("test:-5:-10", result.get(0));
    }

    @Test
    public void testWithZeroInts() {
        final List<String> result = new ArrayList<>();
        ObjBiIntConsumer<String> consumer = (t, i, j) -> result.add(t + ":" + i + ":" + j);

        consumer.accept("test", 0, 0);

        assertEquals(1, result.size());
        assertEquals("test:0:0", result.get(0));
    }

    @Test
    public void testWithBoundaryValues() {
        final List<String> result = new ArrayList<>();
        ObjBiIntConsumer<String> consumer = (t, i, j) -> result.add(t + ":" + i + ":" + j);

        consumer.accept("max", Integer.MAX_VALUE, Integer.MIN_VALUE);

        assertEquals(1, result.size());
        assertEquals("max:" + Integer.MAX_VALUE + ":" + Integer.MIN_VALUE, result.get(0));
    }

    @Test
    public void testWithNullObject() {
        final List<String> result = new ArrayList<>();
        ObjBiIntConsumer<String> consumer = (t, i, j) -> result.add(t + ":" + i + ":" + j);

        consumer.accept(null, 1, 2);

        assertEquals(1, result.size());
        assertEquals("null:1:2", result.get(0));
    }

    @Test
    public void testSideEffects() {
        final int[] counter = { 0 };
        ObjBiIntConsumer<String> consumer = (t, i, j) -> counter[0] = i + j;

        consumer.accept("test", 10, 20);

        assertEquals(30, counter[0]);
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(ObjBiIntConsumer.class.getAnnotation(FunctionalInterface.class));
    }
}
