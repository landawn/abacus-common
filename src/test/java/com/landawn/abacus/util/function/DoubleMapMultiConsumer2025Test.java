package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class DoubleMapMultiConsumer2025Test extends TestBase {

    @Test
    public void testAccept() {
        final List<Double> result = new ArrayList<>();
        DoubleMapMultiConsumer consumer = (value, ic) -> {
            ic.accept(value);
            ic.accept(value * 2);
        };

        consumer.accept(5.0, result::add);

        assertEquals(2, result.size());
        assertEquals(5.0, result.get(0));
        assertEquals(10.0, result.get(1));
    }

    @Test
    public void testAcceptWithMultipleValues() {
        final List<Double> result = new ArrayList<>();
        DoubleMapMultiConsumer consumer = (value, ic) -> {
            ic.accept(value);
            ic.accept(value + 1);
            ic.accept(value + 2);
        };

        consumer.accept(10.5, result::add);

        assertEquals(3, result.size());
        assertEquals(10.5, result.get(0));
        assertEquals(11.5, result.get(1));
        assertEquals(12.5, result.get(2));
    }

    @Test
    public void testAcceptWithNoValues() {
        final List<Double> result = new ArrayList<>();
        DoubleMapMultiConsumer consumer = (value, ic) -> {
            // Do nothing - emit no values
        };

        consumer.accept(5.0, result::add);

        assertEquals(0, result.size());
    }

    @Test
    public void testAcceptWithFilteringLogic() {
        final List<Double> result = new ArrayList<>();
        DoubleMapMultiConsumer consumer = (value, ic) -> {
            if (value > 0) {
                ic.accept(value);
                ic.accept(value * 2);
            }
        };

        consumer.accept(5.0, result::add);
        assertEquals(2, result.size());

        result.clear();
        consumer.accept(-5.0, result::add);
        assertEquals(0, result.size());
    }

    @Test
    public void testAcceptWithAnonymousClass() {
        final List<Double> result = new ArrayList<>();
        DoubleMapMultiConsumer consumer = new DoubleMapMultiConsumer() {
            @Override
            public void accept(double value, DoubleConsumer ic) {
                ic.accept(value);
                ic.accept(value / 2);
            }
        };

        consumer.accept(20.0, result::add);

        assertEquals(2, result.size());
        assertEquals(20.0, result.get(0));
        assertEquals(10.0, result.get(1));
    }

    @Test
    public void testAcceptWithNegativeValues() {
        final List<Double> result = new ArrayList<>();
        DoubleMapMultiConsumer consumer = (value, ic) -> {
            ic.accept(value);
            ic.accept(value * -1);
        };

        consumer.accept(-5.5, result::add);

        assertEquals(2, result.size());
        assertEquals(-5.5, result.get(0));
        assertEquals(5.5, result.get(1));
    }

    @Test
    public void testAcceptWithBoundaryValues() {
        final List<Double> result = new ArrayList<>();
        DoubleMapMultiConsumer consumer = (value, ic) -> {
            ic.accept(value);
        };

        consumer.accept(Double.MAX_VALUE, result::add);
        consumer.accept(Double.MIN_VALUE, result::add);
        consumer.accept(0.0, result::add);
        consumer.accept(-0.0, result::add);

        assertEquals(4, result.size());
        assertEquals(Double.MAX_VALUE, result.get(0));
        assertEquals(Double.MIN_VALUE, result.get(1));
        assertEquals(0.0, result.get(2));
        assertEquals(-0.0, result.get(3));
    }

    @Test
    public void testAcceptWithSpecialValues() {
        final List<Double> result = new ArrayList<>();
        DoubleMapMultiConsumer consumer = (value, ic) -> {
            ic.accept(value);
        };

        consumer.accept(Double.NaN, result::add);
        consumer.accept(Double.POSITIVE_INFINITY, result::add);
        consumer.accept(Double.NEGATIVE_INFINITY, result::add);

        assertEquals(3, result.size());
        assertEquals(Double.NaN, result.get(0));
        assertEquals(Double.POSITIVE_INFINITY, result.get(1));
        assertEquals(Double.NEGATIVE_INFINITY, result.get(2));
    }

    @Test
    public void testAcceptFlatMapScenario() {
        final List<Double> result = new ArrayList<>();
        DoubleMapMultiConsumer consumer = (value, ic) -> {
            for (int i = 0; i < 3; i++) {
                ic.accept(value + i);
            }
        };

        consumer.accept(1.0, result::add);

        assertEquals(3, result.size());
        assertEquals(1.0, result.get(0));
        assertEquals(2.0, result.get(1));
        assertEquals(3.0, result.get(2));
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(DoubleMapMultiConsumer.class.getAnnotation(FunctionalInterface.class));
    }
}
