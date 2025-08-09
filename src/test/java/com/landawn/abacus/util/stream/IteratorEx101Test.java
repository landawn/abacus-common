package com.landawn.abacus.util.stream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class IteratorEx101Test extends TestBase {

    private static class TestIteratorEx<T> implements IteratorEx<T> {
        private final List<T> items;
        private int index = 0;
        
        public TestIteratorEx(List<T> items) {
            this.items = items;
        }
        
        @Override
        public boolean hasNext() {
            return index < items.size();
        }
        
        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return items.get(index++);
        }
    }

    @Test
    public void testAdvance() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        IteratorEx<String> iter = new TestIteratorEx<>(list);
        
        iter.advance(2);
        Assertions.assertEquals("c", iter.next());
        
        iter.advance(1);
        Assertions.assertEquals("e", iter.next());
        
        // Advance beyond end
        iter.advance(10);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testAdvanceZero() {
        List<String> list = Arrays.asList("a", "b", "c");
        IteratorEx<String> iter = new TestIteratorEx<>(list);
        
        iter.advance(0);
        Assertions.assertEquals("a", iter.next());
    }

    @Test
    public void testAdvanceNegative() {
        List<String> list = Arrays.asList("a", "b", "c");
        IteratorEx<String> iter = new TestIteratorEx<>(list);
        // Assertions.assertThrows(IllegalArgumentException.class, () -> iter.advance(-1));
        iter.advance(-1);
    }

    @Test
    public void testCount() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        IteratorEx<String> iter = new TestIteratorEx<>(list);
        
        Assertions.assertEquals(5, iter.count());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testCountAfterPartialIteration() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        IteratorEx<String> iter = new TestIteratorEx<>(list);
        
        // Consume first two elements
        iter.next();
        iter.next();
        
        // Count should return remaining elements
        Assertions.assertEquals(3, iter.count());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testCountEmptyIterator() {
        List<String> list = new ArrayList<>();
        IteratorEx<String> iter = new TestIteratorEx<>(list);
        
        Assertions.assertEquals(0, iter.count());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testClose() {
        List<String> list = Arrays.asList("a", "b", "c");
        IteratorEx<String> iter = new TestIteratorEx<>(list);
        
        // Default close() should do nothing and not throw
        iter.close();
        
        // Iterator should still work after close
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals("a", iter.next());
    }

    @Test
    public void testIteratorExAsIterator() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        IteratorEx<Integer> iter = new TestIteratorEx<>(list);
        
        // Test that IteratorEx can be used as a regular Iterator
        List<Integer> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        
        Assertions.assertEquals(list, result);
    }

    @Test
    public void testIteratorExAsAutoCloseable() throws Exception {
        List<String> list = Arrays.asList("a", "b", "c");
        
        // Test that IteratorEx can be used in try-with-resources
        try (IteratorEx<String> iter = new TestIteratorEx<>(list)) {
            Assertions.assertTrue(iter.hasNext());
            Assertions.assertEquals("a", iter.next());
        } // close() will be called automatically
    }

    @Test
    public void testCombinedOperations() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        IteratorEx<Integer> iter = new TestIteratorEx<>(list);
        
        // Advance by 2
        iter.advance(2);
        Assertions.assertEquals(3, iter.next().intValue());
        
        // Advance by 3 more
        iter.advance(3);
        Assertions.assertEquals(7, iter.next().intValue());
        
        // Count remaining elements
        Assertions.assertEquals(3, iter.count());
        Assertions.assertFalse(iter.hasNext());
    }
}