package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class DoubleObjConsumer2025Test extends TestBase {

    @Test
    public void testAccept() {
        final List<String> result = new ArrayList<>();
        DoubleObjConsumer<String> consumer = (d, s) -> result.add(d + ":" + s);

        consumer.accept(5.0, "test");
        consumer.accept(10.5, "value");

        assertEquals(2, result.size());
        assertEquals("5.0:test", result.get(0));
        assertEquals("10.5:value", result.get(1));
    }

    @Test
    public void testAcceptWithLambda() {
        final double[] doubleResult = new double[1];
        final String[] stringResult = new String[1];
        DoubleObjConsumer<String> consumer = (d, s) -> {
            doubleResult[0] = d * 2;
            stringResult[0] = s.toUpperCase();
        };

        consumer.accept(7.5, "hello");

        assertEquals(15.0, doubleResult[0]);
        assertEquals("HELLO", stringResult[0]);
    }

    @Test
    public void testAcceptWithAnonymousClass() {
        final List<String> result = new ArrayList<>();
        DoubleObjConsumer<String> consumer = new DoubleObjConsumer<String>() {
            @Override
            public void accept(double i, String t) {
                result.add(String.format("%.2f-%s", i, t));
            }
        };

        consumer.accept(3.14159, "pi");

        assertEquals(1, result.size());
        assertEquals("3.14-pi", result.get(0));
    }

    @Test
    public void testAndThen() {
        final List<String> result = new ArrayList<>();
        DoubleObjConsumer<String> first = (d, s) -> result.add(d + ":" + s);
        DoubleObjConsumer<String> second = (d, s) -> result.add(s + ":" + d);

        DoubleObjConsumer<String> combined = first.andThen(second);
        combined.accept(5.0, "test");

        assertEquals(2, result.size());
        assertEquals("5.0:test", result.get(0));
        assertEquals("test:5.0", result.get(1));
    }

    @Test
    public void testAndThenChaining() {
        final List<String> result = new ArrayList<>();
        DoubleObjConsumer<String> first = (d, s) -> result.add("first");
        DoubleObjConsumer<String> second = (d, s) -> result.add("second");
        DoubleObjConsumer<String> third = (d, s) -> result.add("third");

        DoubleObjConsumer<String> combined = first.andThen(second).andThen(third);
        combined.accept(5.0, "test");

        assertEquals(3, result.size());
        assertEquals("first", result.get(0));
        assertEquals("second", result.get(1));
        assertEquals("third", result.get(2));
    }

    @Test
    public void testWithNegativeValues() {
        final List<String> result = new ArrayList<>();
        DoubleObjConsumer<String> consumer = (d, s) -> result.add(d + ":" + s);

        consumer.accept(-5.5, "negative");
        consumer.accept(-100.75, "value");

        assertEquals(2, result.size());
        assertEquals("-5.5:negative", result.get(0));
        assertEquals("-100.75:value", result.get(1));
    }

    @Test
    public void testWithBoundaryValues() {
        final List<String> result = new ArrayList<>();
        DoubleObjConsumer<String> consumer = (d, s) -> result.add(d + ":" + s);

        consumer.accept(Double.MAX_VALUE, "max");
        consumer.accept(Double.MIN_VALUE, "min");
        consumer.accept(0.0, "zero");

        assertEquals(3, result.size());
        assertEquals(Double.MAX_VALUE + ":max", result.get(0));
        assertEquals(Double.MIN_VALUE + ":min", result.get(1));
        assertEquals("0.0:zero", result.get(2));
    }

    @Test
    public void testWithSpecialValues() {
        final List<String> result = new ArrayList<>();
        DoubleObjConsumer<String> consumer = (d, s) -> result.add(d + ":" + s);

        consumer.accept(Double.NaN, "nan");
        consumer.accept(Double.POSITIVE_INFINITY, "posinf");
        consumer.accept(Double.NEGATIVE_INFINITY, "neginf");

        assertEquals(3, result.size());
        assertEquals("NaN:nan", result.get(0));
        assertEquals("Infinity:posinf", result.get(1));
        assertEquals("-Infinity:neginf", result.get(2));
    }

    @Test
    public void testWithNullObject() {
        final List<String> result = new ArrayList<>();
        DoubleObjConsumer<String> consumer = (d, s) -> result.add(d + ":" + s);

        consumer.accept(5.0, null);

        assertEquals(1, result.size());
        assertEquals("5.0:null", result.get(0));
    }

    @Test
    public void testWithDifferentObjectTypes() {
        final List<String> result = new ArrayList<>();
        DoubleObjConsumer<Integer> consumer = (d, i) -> result.add(d + ":" + i);

        consumer.accept(5.5, 10);
        consumer.accept(7.5, 20);

        assertEquals(2, result.size());
        assertEquals("5.5:10", result.get(0));
        assertEquals("7.5:20", result.get(1));
    }

    @Test
    public void testSideEffects() {
        final int[] counter = {0};
        DoubleObjConsumer<String> consumer = (d, s) -> counter[0]++;

        consumer.accept(1.0, "a");
        consumer.accept(2.0, "b");
        consumer.accept(3.0, "c");

        assertEquals(3, counter[0]);
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(DoubleObjConsumer.class.getAnnotation(FunctionalInterface.class));
    }
}
