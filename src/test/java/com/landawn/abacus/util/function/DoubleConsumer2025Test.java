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
public class DoubleConsumer2025Test extends TestBase {

    @Test
    public void testAccept() {
        List<Double> results = new ArrayList<>();
        DoubleConsumer consumer = t -> results.add(t);

        consumer.accept(5.5);

        assertEquals(1, results.size());
        assertEquals(5.5, results.get(0), 0.0001);
    }

    @Test
    public void testAccept_WithMethodReference() {
        List<Double> results = new ArrayList<>();
        DoubleConsumer consumer = results::add;

        consumer.accept(10.5);

        assertEquals(1, results.size());
        assertEquals(10.5, results.get(0), 0.0001);
    }

    @Test
    public void testAccept_WithAnonymousClass() {
        List<String> results = new ArrayList<>();
        DoubleConsumer consumer = new DoubleConsumer() {
            @Override
            public void accept(double t) {
                results.add("value=" + t);
            }
        };

        consumer.accept(15.5);

        assertEquals(1, results.size());
        assertEquals("value=15.5", results.get(0));
    }

    @Test
    public void testAndThen() {
        List<String> results = new ArrayList<>();
        DoubleConsumer consumer1 = t -> results.add("first:" + t);
        DoubleConsumer consumer2 = t -> results.add("second:" + t);

        DoubleConsumer chained = consumer1.andThen(consumer2);
        chained.accept(5.0);

        assertEquals(2, results.size());
        assertEquals("first:5.0", results.get(0));
        assertEquals("second:5.0", results.get(1));
    }

    @Test
    public void testAndThen_MultipleChains() {
        List<Integer> results = new ArrayList<>();
        DoubleConsumer consumer1 = t -> results.add(1);
        DoubleConsumer consumer2 = t -> results.add(2);
        DoubleConsumer consumer3 = t -> results.add(3);

        DoubleConsumer chained = consumer1.andThen(consumer2).andThen(consumer3);
        chained.accept(1.0);

        assertEquals(3, results.size());
        assertEquals(1, results.get(0));
        assertEquals(2, results.get(1));
        assertEquals(3, results.get(2));
    }

    @Test
    public void testAccept_WithNegativeValue() {
        List<Double> results = new ArrayList<>();
        DoubleConsumer consumer = t -> results.add(t * 2);

        consumer.accept(-5.0);

        assertEquals(1, results.size());
        assertEquals(-10.0, results.get(0), 0.0001);
    }

    @Test
    public void testAccept_WithZeroValue() {
        List<Double> results = new ArrayList<>();
        DoubleConsumer consumer = t -> results.add(t);

        consumer.accept(0.0);

        assertEquals(1, results.size());
        assertEquals(0.0, results.get(0), 0.0001);
    }

    @Test
    public void testAccept_WithSpecialValues() {
        List<Double> results = new ArrayList<>();
        DoubleConsumer consumer = results::add;

        consumer.accept(Double.MAX_VALUE);
        consumer.accept(Double.MIN_VALUE);
        consumer.accept(Double.POSITIVE_INFINITY);
        consumer.accept(Double.NEGATIVE_INFINITY);
        consumer.accept(Double.NaN);

        assertEquals(5, results.size());
        assertEquals(Double.MAX_VALUE, results.get(0), 0.0001);
        assertEquals(Double.MIN_VALUE, results.get(1), 0.0001);
        assertEquals(Double.POSITIVE_INFINITY, results.get(2), 0.0001);
        assertEquals(Double.NEGATIVE_INFINITY, results.get(3), 0.0001);
        assertEquals(Double.NaN, results.get(4), 0.0001);
    }

    @Test
    public void testAndThen_ExecutionOrder() {
        List<String> results = new ArrayList<>();
        DoubleConsumer consumer1 = t -> results.add("A");
        DoubleConsumer consumer2 = t -> results.add("B");

        DoubleConsumer chained = consumer1.andThen(consumer2);
        chained.accept(1.0);

        assertEquals("A", results.get(0));
        assertEquals("B", results.get(1));
    }

    @Test
    public void testJavaUtilFunctionCompatibility() {
        List<Double> results = new ArrayList<>();
        DoubleConsumer consumer = t -> results.add(t);
        java.util.function.DoubleConsumer javaConsumer = consumer;

        javaConsumer.accept(5.5);

        assertEquals(1, results.size());
        assertEquals(5.5, results.get(0), 0.0001);
    }

    @Test
    public void testAndThen_WithJavaUtilFunction() {
        List<Double> results = new ArrayList<>();
        DoubleConsumer consumer1 = t -> results.add(t);
        java.util.function.DoubleConsumer javaConsumer = t -> results.add(t * 2);

        DoubleConsumer chained = consumer1.andThen(javaConsumer);
        chained.accept(5.0);

        assertEquals(2, results.size());
        assertEquals(5.0, results.get(0), 0.0001);
        assertEquals(10.0, results.get(1), 0.0001);
    }

    @Test
    public void testFunctionalInterfaceContract() {
        DoubleConsumer lambda = t -> {};
        assertNotNull(lambda);
        assertDoesNotThrow(() -> lambda.accept(1.0));
    }
}
