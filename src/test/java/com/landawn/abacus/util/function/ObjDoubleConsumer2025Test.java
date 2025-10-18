package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ObjDoubleConsumer2025Test extends TestBase {

    @Test
    public void testAccept() {
        final List<String> result = new ArrayList<>();
        ObjDoubleConsumer<String> consumer = (t, value) -> result.add(t + ":" + value);

        consumer.accept("test", 5.5);
        consumer.accept("value", 10.25);

        assertEquals(2, result.size());
        assertEquals("test:5.5", result.get(0));
        assertEquals("value:10.25", result.get(1));
    }

    @Test
    public void testAcceptWithLambda() {
        final double[] doubleResult = new double[1];
        final String[] stringResult = new String[1];
        ObjDoubleConsumer<String> consumer = (t, value) -> {
            doubleResult[0] = value * 2;
            stringResult[0] = t.toUpperCase();
        };

        consumer.accept("hello", 7.5);

        assertEquals(15.0, doubleResult[0]);
        assertEquals("HELLO", stringResult[0]);
    }

    @Test
    public void testAcceptWithAnonymousClass() {
        final List<String> result = new ArrayList<>();
        ObjDoubleConsumer<String> consumer = new ObjDoubleConsumer<String>() {
            @Override
            public void accept(String t, double value) {
                result.add(String.format("%s[%.2f]", t, value));
            }
        };

        consumer.accept("test", 3.14159);

        assertEquals(1, result.size());
        assertEquals("test[3.14]", result.get(0));
    }

    @Test
    public void testAndThen() {
        final List<String> result = new ArrayList<>();
        ObjDoubleConsumer<String> first = (t, value) -> result.add(t + ":" + value);
        ObjDoubleConsumer<String> second = (t, value) -> result.add(value + ":" + t);

        ObjDoubleConsumer<String> combined = first.andThen(second);
        combined.accept("test", 5.5);

        assertEquals(2, result.size());
        assertEquals("test:5.5", result.get(0));
        assertEquals("5.5:test", result.get(1));
    }

    @Test
    public void testAndThenChaining() {
        final List<String> result = new ArrayList<>();
        ObjDoubleConsumer<String> first = (t, value) -> result.add("first");
        ObjDoubleConsumer<String> second = (t, value) -> result.add("second");
        ObjDoubleConsumer<String> third = (t, value) -> result.add("third");

        ObjDoubleConsumer<String> combined = first.andThen(second).andThen(third);
        combined.accept("test", 5.5);

        assertEquals(3, result.size());
        assertEquals("first", result.get(0));
        assertEquals("second", result.get(1));
        assertEquals("third", result.get(2));
    }

    @Test
    public void testWithNegativeValues() {
        final List<String> result = new ArrayList<>();
        ObjDoubleConsumer<String> consumer = (t, value) -> result.add(t + ":" + value);

        consumer.accept("negative", -5.5);
        consumer.accept("value", -100.75);

        assertEquals(2, result.size());
        assertEquals("negative:-5.5", result.get(0));
        assertEquals("value:-100.75", result.get(1));
    }

    @Test
    public void testWithBoundaryValues() {
        final List<String> result = new ArrayList<>();
        ObjDoubleConsumer<String> consumer = (t, value) -> result.add(t + ":" + value);

        consumer.accept("max", Double.MAX_VALUE);
        consumer.accept("min", Double.MIN_VALUE);
        consumer.accept("zero", 0.0);

        assertEquals(3, result.size());
        assertEquals("max:" + Double.MAX_VALUE, result.get(0));
        assertEquals("min:" + Double.MIN_VALUE, result.get(1));
        assertEquals("zero:0.0", result.get(2));
    }

    @Test
    public void testWithSpecialValues() {
        final List<String> result = new ArrayList<>();
        ObjDoubleConsumer<String> consumer = (t, value) -> result.add(t + ":" + value);

        consumer.accept("nan", Double.NaN);
        consumer.accept("posinf", Double.POSITIVE_INFINITY);
        consumer.accept("neginf", Double.NEGATIVE_INFINITY);

        assertEquals(3, result.size());
        assertEquals("nan:NaN", result.get(0));
        assertEquals("posinf:Infinity", result.get(1));
        assertEquals("neginf:-Infinity", result.get(2));
    }

    @Test
    public void testWithNullObject() {
        final List<String> result = new ArrayList<>();
        ObjDoubleConsumer<String> consumer = (t, value) -> result.add(t + ":" + value);

        consumer.accept(null, 5.5);

        assertEquals(1, result.size());
        assertEquals("null:5.5", result.get(0));
    }

    @Test
    public void testSideEffects() {
        final double[] sum = {0.0};
        ObjDoubleConsumer<String> consumer = (t, value) -> sum[0] += value;

        consumer.accept("a", 1.5);
        consumer.accept("b", 2.5);
        consumer.accept("c", 3.0);

        assertEquals(7.0, sum[0], 0.001);
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(ObjDoubleConsumer.class.getAnnotation(FunctionalInterface.class));
    }
}
