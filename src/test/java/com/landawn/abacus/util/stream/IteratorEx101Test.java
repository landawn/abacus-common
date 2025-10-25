package com.landawn.abacus.util.stream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
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

        iter.next();
        iter.next();

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

        iter.close();

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals("a", iter.next());
    }

    @Test
    public void testIteratorExAsIterator() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        IteratorEx<Integer> iter = new TestIteratorEx<>(list);

        List<Integer> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.next());
        }

        Assertions.assertEquals(list, result);
    }

    @Test
    public void testIteratorExAsAutoCloseable() throws Exception {
        List<String> list = Arrays.asList("a", "b", "c");

        try (IteratorEx<String> iter = new TestIteratorEx<>(list)) {
            Assertions.assertTrue(iter.hasNext());
            Assertions.assertEquals("a", iter.next());
        }
    }

    @Test
    public void testCombinedOperations() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        IteratorEx<Integer> iter = new TestIteratorEx<>(list);

        iter.advance(2);
        Assertions.assertEquals(3, iter.next().intValue());

        iter.advance(3);
        Assertions.assertEquals(7, iter.next().intValue());

        Assertions.assertEquals(3, iter.count());
        Assertions.assertFalse(iter.hasNext());
    }
}
