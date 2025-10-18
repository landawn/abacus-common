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
public class DoubleNConsumer2025Test extends TestBase {

    @Test
    public void testAccept_NoArgs() {
        List<Integer> results = new ArrayList<>();
        DoubleNConsumer consumer = args -> results.add(args.length);

        consumer.accept();

        assertEquals(1, results.size());
        assertEquals(0, results.get(0));
    }

    @Test
    public void testAccept_OneArg() {
        List<Double> results = new ArrayList<>();
        DoubleNConsumer consumer = args -> {
            for (double arg : args) {
                results.add(arg);
            }
        };

        consumer.accept(5.5);

        assertEquals(1, results.size());
        assertEquals(5.5, results.get(0), 0.0001);
    }

    @Test
    public void testAccept_MultipleArgs() {
        List<Double> results = new ArrayList<>();
        DoubleNConsumer consumer = args -> {
            for (double arg : args) {
                results.add(arg);
            }
        };

        consumer.accept(1.0, 2.0, 3.0, 4.0, 5.0);

        assertEquals(5, results.size());
        assertEquals(1.0, results.get(0), 0.0001);
        assertEquals(5.0, results.get(4), 0.0001);
    }

    @Test
    public void testAccept_WithAnonymousClass() {
        List<String> results = new ArrayList<>();
        DoubleNConsumer consumer = new DoubleNConsumer() {
            @Override
            public void accept(double... args) {
                double sum = 0;
                for (double arg : args) {
                    sum += arg;
                }
                results.add("sum=" + sum);
            }
        };

        consumer.accept(1.0, 2.0, 3.0);

        assertEquals(1, results.size());
        assertEquals("sum=6.0", results.get(0));
    }

    @Test
    public void testAndThen() {
        List<String> results = new ArrayList<>();
        DoubleNConsumer consumer1 = args -> results.add("first:" + args.length);
        DoubleNConsumer consumer2 = args -> results.add("second:" + args.length);

        DoubleNConsumer chained = consumer1.andThen(consumer2);
        chained.accept(1.0, 2.0, 3.0);

        assertEquals(2, results.size());
        assertEquals("first:3", results.get(0));
        assertEquals("second:3", results.get(1));
    }

    @Test
    public void testAndThen_MultipleChains() {
        List<Integer> results = new ArrayList<>();
        DoubleNConsumer consumer1 = args -> results.add(1);
        DoubleNConsumer consumer2 = args -> results.add(2);
        DoubleNConsumer consumer3 = args -> results.add(3);

        DoubleNConsumer chained = consumer1.andThen(consumer2).andThen(consumer3);
        chained.accept(1.0, 2.0);

        assertEquals(3, results.size());
        assertEquals(1, results.get(0));
        assertEquals(2, results.get(1));
        assertEquals(3, results.get(2));
    }

    @Test
    public void testAccept_CalculatingSum() {
        List<Double> results = new ArrayList<>();
        DoubleNConsumer consumer = args -> {
            double sum = 0;
            for (double arg : args) {
                sum += arg;
            }
            results.add(sum);
        };

        consumer.accept(1.5, 2.5, 3.5);

        assertEquals(1, results.size());
        assertEquals(7.5, results.get(0), 0.0001);
    }

    @Test
    public void testAccept_CalculatingAverage() {
        List<Double> results = new ArrayList<>();
        DoubleNConsumer consumer = args -> {
            if (args.length > 0) {
                double sum = 0;
                for (double arg : args) {
                    sum += arg;
                }
                results.add(sum / args.length);
            }
        };

        consumer.accept(2.0, 4.0, 6.0, 8.0);

        assertEquals(1, results.size());
        assertEquals(5.0, results.get(0), 0.0001);
    }

    @Test
    public void testAccept_WithNegativeValues() {
        List<Double> results = new ArrayList<>();
        DoubleNConsumer consumer = args -> {
            for (double arg : args) {
                results.add(arg);
            }
        };

        consumer.accept(-1.0, -2.0, -3.0);

        assertEquals(3, results.size());
        assertEquals(-1.0, results.get(0), 0.0001);
        assertEquals(-3.0, results.get(2), 0.0001);
    }

    @Test
    public void testAccept_WithZeroValues() {
        List<Double> results = new ArrayList<>();
        DoubleNConsumer consumer = args -> {
            for (double arg : args) {
                results.add(arg);
            }
        };

        consumer.accept(0.0, 0.0, 0.0);

        assertEquals(3, results.size());
        assertEquals(0.0, results.get(0), 0.0001);
    }

    @Test
    public void testAccept_WithVarargsArray() {
        List<Double> results = new ArrayList<>();
        DoubleNConsumer consumer = args -> {
            for (double arg : args) {
                results.add(arg);
            }
        };

        double[] values = {1.0, 2.0, 3.0, 4.0};
        consumer.accept(values);

        assertEquals(4, results.size());
        assertEquals(1.0, results.get(0), 0.0001);
        assertEquals(4.0, results.get(3), 0.0001);
    }

    @Test
    public void testFunctionalInterfaceContract() {
        DoubleNConsumer lambda = args -> {};
        assertNotNull(lambda);
        assertDoesNotThrow(() -> lambda.accept(1.0, 2.0, 3.0));
    }
}
