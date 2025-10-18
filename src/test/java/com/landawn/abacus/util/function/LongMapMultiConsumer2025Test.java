package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongConsumer;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class LongMapMultiConsumer2025Test extends TestBase {

    @Test
    public void testAccept() {
        final List<Long> result = new ArrayList<>();
        LongMapMultiConsumer consumer = (value, ic) -> {
            ic.accept(value);
            ic.accept(value * 2);
        };

        consumer.accept(5L, result::add);

        assertEquals(2, result.size());
        assertEquals(5L, result.get(0));
        assertEquals(10L, result.get(1));
    }

    @Test
    public void testAcceptWithMultipleValues() {
        final List<Long> result = new ArrayList<>();
        LongMapMultiConsumer consumer = (value, ic) -> {
            ic.accept(value);
            ic.accept(value + 1);
            ic.accept(value + 2);
        };

        consumer.accept(10L, result::add);

        assertEquals(3, result.size());
        assertEquals(10L, result.get(0));
        assertEquals(11L, result.get(1));
        assertEquals(12L, result.get(2));
    }

    @Test
    public void testAcceptWithNoValues() {
        final List<Long> result = new ArrayList<>();
        LongMapMultiConsumer consumer = (value, ic) -> {
            // Do nothing - emit no values
        };

        consumer.accept(5L, result::add);

        assertEquals(0, result.size());
    }

    @Test
    public void testAcceptWithFilteringLogic() {
        final List<Long> result = new ArrayList<>();
        LongMapMultiConsumer consumer = (value, ic) -> {
            if (value > 0) {
                ic.accept(value);
                ic.accept(value * 2);
            }
        };

        consumer.accept(5L, result::add);
        assertEquals(2, result.size());

        result.clear();
        consumer.accept(-5L, result::add);
        assertEquals(0, result.size());
    }

    @Test
    public void testAcceptWithAnonymousClass() {
        final List<Long> result = new ArrayList<>();
        LongMapMultiConsumer consumer = new LongMapMultiConsumer() {
            @Override
            public void accept(long value, LongConsumer ic) {
                ic.accept(value);
                ic.accept(value / 2);
            }
        };

        consumer.accept(20L, result::add);

        assertEquals(2, result.size());
        assertEquals(20L, result.get(0));
        assertEquals(10L, result.get(1));
    }

    @Test
    public void testAcceptWithNegativeValues() {
        final List<Long> result = new ArrayList<>();
        LongMapMultiConsumer consumer = (value, ic) -> {
            ic.accept(value);
            ic.accept(value * -1);
        };

        consumer.accept(-5L, result::add);

        assertEquals(2, result.size());
        assertEquals(-5L, result.get(0));
        assertEquals(5L, result.get(1));
    }

    @Test
    public void testAcceptWithBoundaryValues() {
        final List<Long> result = new ArrayList<>();
        LongMapMultiConsumer consumer = (value, ic) -> {
            ic.accept(value);
        };

        consumer.accept(Long.MAX_VALUE, result::add);
        consumer.accept(Long.MIN_VALUE, result::add);
        consumer.accept(0L, result::add);

        assertEquals(3, result.size());
        assertEquals(Long.MAX_VALUE, result.get(0));
        assertEquals(Long.MIN_VALUE, result.get(1));
        assertEquals(0L, result.get(2));
    }

    @Test
    public void testAcceptFlatMapScenario() {
        final List<Long> result = new ArrayList<>();
        LongMapMultiConsumer consumer = (value, ic) -> {
            for (long i = 0; i < 3; i++) {
                ic.accept(value + i);
            }
        };

        consumer.accept(1L, result::add);

        assertEquals(3, result.size());
        assertEquals(1L, result.get(0));
        assertEquals(2L, result.get(1));
        assertEquals(3L, result.get(2));
    }

    @Test
    public void testAcceptWithRangeGeneration() {
        final List<Long> result = new ArrayList<>();
        LongMapMultiConsumer consumer = (value, ic) -> {
            for (long i = 0; i < value; i++) {
                ic.accept(i);
            }
        };

        consumer.accept(3L, result::add);

        assertEquals(3, result.size());
        assertEquals(0L, result.get(0));
        assertEquals(1L, result.get(1));
        assertEquals(2L, result.get(2));
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(LongMapMultiConsumer.class.getAnnotation(FunctionalInterface.class));
    }
}
