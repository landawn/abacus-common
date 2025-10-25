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
public class DoubleTriConsumer2025Test extends TestBase {

    @Test
    public void testAccept() {
        List<String> results = new ArrayList<>();
        DoubleTriConsumer consumer = (a, b, c) -> results.add("a=" + a + ", b=" + b + ", c=" + c);

        consumer.accept(1.0, 2.0, 3.0);

        assertEquals(1, results.size());
        assertEquals("a=1.0, b=2.0, c=3.0", results.get(0));
    }

    @Test
    public void testAccept_WithAnonymousClass() {
        List<Double> results = new ArrayList<>();
        DoubleTriConsumer consumer = new DoubleTriConsumer() {
            @Override
            public void accept(double a, double b, double c) {
                results.add(a + b + c);
            }
        };

        consumer.accept(1.0, 2.0, 3.0);

        assertEquals(1, results.size());
        assertEquals(6.0, results.get(0), 0.0001);
    }

    @Test
    public void testAndThen() {
        List<String> results = new ArrayList<>();
        DoubleTriConsumer consumer1 = (a, b, c) -> results.add("first:" + (a + b + c));
        DoubleTriConsumer consumer2 = (a, b, c) -> results.add("second:" + (a * b * c));

        DoubleTriConsumer chained = consumer1.andThen(consumer2);
        chained.accept(2.0, 3.0, 4.0);

        assertEquals(2, results.size());
        assertEquals("first:9.0", results.get(0));
        assertEquals("second:24.0", results.get(1));
    }

    @Test
    public void testAndThen_MultipleChains() {
        List<Integer> results = new ArrayList<>();
        DoubleTriConsumer consumer1 = (a, b, c) -> results.add(1);
        DoubleTriConsumer consumer2 = (a, b, c) -> results.add(2);
        DoubleTriConsumer consumer3 = (a, b, c) -> results.add(3);

        DoubleTriConsumer chained = consumer1.andThen(consumer2).andThen(consumer3);
        chained.accept(1.0, 2.0, 3.0);

        assertEquals(3, results.size());
        assertEquals(1, results.get(0));
        assertEquals(2, results.get(1));
        assertEquals(3, results.get(2));
    }

    @Test
    public void testFunctionalInterfaceContract() {
        DoubleTriConsumer lambda = (a, b, c) -> {
        };
        assertNotNull(lambda);
        assertDoesNotThrow(() -> lambda.accept(1.0, 2.0, 3.0));
    }
}
