package com.landawn.abacus.util;

import static org.junit.Assert.assertThrows;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class AbstractIndexedTest extends TestBase {

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
    public void testNegativeIndex() {
        ConcreteIndexed indexed = new ConcreteIndexed(-100L);
        Assertions.assertEquals(-100, indexed.index());
        Assertions.assertEquals(-100L, indexed.longIndex());
    }

    @Test
    public void testIndexWithLargeValue() {
        ConcreteIndexed indexed = new ConcreteIndexed(Integer.MAX_VALUE + 1L);
        assertThrows(ArithmeticException.class, () -> indexed.index());
    }

    @Test
    public void testLongIndex() {
        ConcreteIndexed indexed = new ConcreteIndexed(Long.MAX_VALUE);
        Assertions.assertEquals(Long.MAX_VALUE, indexed.longIndex());
    }
}
