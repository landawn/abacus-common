package com.landawn.abacus.util;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class AbstractIndexed100Test extends TestBase {

    // Create a concrete implementation for testing
    private static class ConcreteIndexed extends AbstractIndexed {
        public ConcreteIndexed(long index) {
            super(index);
        }
    }

    @Test
    public void testIndex() {
        ConcreteIndexed indexed = new ConcreteIndexed(42L);
        Assertions.assertEquals(42, indexed.index());
    }

    @Test
    public void testIndexWithLargeValue() {
        ConcreteIndexed indexed = new ConcreteIndexed(Integer.MAX_VALUE + 1L);
        // This will overflow when converting to int
        Assertions.assertEquals(Integer.MIN_VALUE, indexed.index());
    }

    @Test
    public void testLongIndex() {
        ConcreteIndexed indexed = new ConcreteIndexed(Long.MAX_VALUE);
        Assertions.assertEquals(Long.MAX_VALUE, indexed.longIndex());
    }

    @Test
    public void testNegativeIndex() {
        ConcreteIndexed indexed = new ConcreteIndexed(-100L);
        Assertions.assertEquals(-100, indexed.index());
        Assertions.assertEquals(-100L, indexed.longIndex());
    }
}