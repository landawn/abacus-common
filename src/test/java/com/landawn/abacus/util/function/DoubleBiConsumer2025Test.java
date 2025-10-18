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
public class DoubleBiConsumer2025Test extends TestBase {

    @Test
    public void testAccept() {
        List<String> results = new ArrayList<>();
        DoubleBiConsumer consumer = (t, u) -> results.add("t=" + t + ", u=" + u);

        consumer.accept(1.5, 2.5);

        assertEquals(1, results.size());
        assertEquals("t=1.5, u=2.5", results.get(0));
    }

    @Test
    public void testAccept_WithMethodReference() {
        List<Double> results = new ArrayList<>();
        DoubleBiConsumer consumer = (t, u) -> results.add(t + u);

        consumer.accept(3.0, 4.0);

        assertEquals(1, results.size());
        assertEquals(7.0, results.get(0), 0.0001);
    }

    @Test
    public void testAccept_WithAnonymousClass() {
        List<String> results = new ArrayList<>();
        DoubleBiConsumer consumer = new DoubleBiConsumer() {
            @Override
            public void accept(double t, double u) {
                results.add("sum=" + (t + u));
            }
        };

        consumer.accept(10.5, 20.5);

        assertEquals(1, results.size());
        assertEquals("sum=31.0", results.get(0));
    }

    @Test
    public void testAndThen() {
        List<String> results = new ArrayList<>();
        DoubleBiConsumer consumer1 = (t, u) -> results.add("first: " + t + "," + u);
        DoubleBiConsumer consumer2 = (t, u) -> results.add("second: " + (t + u));

        DoubleBiConsumer chained = consumer1.andThen(consumer2);
        chained.accept(5.0, 10.0);

        assertEquals(2, results.size());
        assertEquals("first: 5.0,10.0", results.get(0));
        assertEquals("second: 15.0", results.get(1));
    }

    @Test
    public void testAndThen_MultipleChains() {
        List<Integer> results = new ArrayList<>();
        DoubleBiConsumer consumer1 = (t, u) -> results.add(1);
        DoubleBiConsumer consumer2 = (t, u) -> results.add(2);
        DoubleBiConsumer consumer3 = (t, u) -> results.add(3);

        DoubleBiConsumer chained = consumer1.andThen(consumer2).andThen(consumer3);
        chained.accept(1.0, 2.0);

        assertEquals(3, results.size());
        assertEquals(1, results.get(0));
        assertEquals(2, results.get(1));
        assertEquals(3, results.get(2));
    }

    @Test
    public void testAccept_WithNegativeValues() {
        List<Double> results = new ArrayList<>();
        DoubleBiConsumer consumer = (t, u) -> results.add(t * u);

        consumer.accept(-5.0, 3.0);

        assertEquals(1, results.size());
        assertEquals(-15.0, results.get(0), 0.0001);
    }

    @Test
    public void testAccept_WithZeroValues() {
        List<String> results = new ArrayList<>();
        DoubleBiConsumer consumer = (t, u) -> results.add("t=" + t + ", u=" + u);

        consumer.accept(0.0, 0.0);

        assertEquals(1, results.size());
        assertEquals("t=0.0, u=0.0", results.get(0));
    }

    @Test
    public void testAccept_WithSpecialValues() {
        List<String> results = new ArrayList<>();
        DoubleBiConsumer consumer = (t, u) -> results.add("t=" + t + ", u=" + u);

        consumer.accept(Double.MAX_VALUE, Double.MIN_VALUE);
        consumer.accept(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
        consumer.accept(Double.NaN, 1.0);

        assertEquals(3, results.size());
    }

    @Test
    public void testAndThen_ExecutionOrder() {
        List<String> results = new ArrayList<>();
        DoubleBiConsumer consumer1 = (t, u) -> results.add("A");
        DoubleBiConsumer consumer2 = (t, u) -> results.add("B");

        DoubleBiConsumer chained = consumer1.andThen(consumer2);
        chained.accept(1.0, 2.0);

        assertEquals("A", results.get(0));
        assertEquals("B", results.get(1));
    }

    @Test
    public void testFunctionalInterfaceContract() {
        DoubleBiConsumer lambda = (t, u) -> {};
        assertNotNull(lambda);
        assertDoesNotThrow(() -> lambda.accept(1.0, 2.0));
    }
}
