package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

public class DoubleListRemoveTest extends DoubleListTestSupport {

    @Test
    public void testRemove() {
        {
            list = new DoubleList();
            list.add(Double.NaN);
            list.add(1.1);
            boolean result = list.remove(Double.NaN);
            assertTrue(result);
            assertEquals(1, list.size());

            list.add(Double.POSITIVE_INFINITY);
            result = list.remove(Double.POSITIVE_INFINITY);
            assertTrue(result);
            assertEquals(1, list.size());
        }
        {
            list = new DoubleList();
            list.add(10.5);
            list.add(20.5);
            list.add(30.5);

            boolean result = list.remove(20.5);
            assertTrue(result);
            assertEquals(2, list.size());
            assertEquals(10.5, list.get(0), DELTA);
            assertEquals(30.5, list.get(1), DELTA);
        }
    }

    @Test
    public void testRemove_NaN() {
        list.addAll(new double[] { 1.1, Double.NaN, 2.2 });

        boolean result = list.remove(Double.NaN);
        assertTrue(result);
        assertEquals(2, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(2.2, list.get(1), DELTA);
    }

    @Test
    public void testRemoveAllOccurrences() {
        list.add(10.5);
        list.add(20.5);
        list.add(10.5);
        list.add(30.5);
        list.add(10.5);

        boolean result = list.removeAllOccurrences(10.5);
        assertTrue(result);
        assertEquals(2, list.size());
        assertEquals(20.5, list.get(0), DELTA);
        assertEquals(30.5, list.get(1), DELTA);
    }

    @Test
    public void testRemoveAllOccurrences_NaN() {
        list.addAll(new double[] { Double.NaN, 1.1, Double.NaN, 2.2, Double.NaN });

        boolean result = list.removeAllOccurrences(Double.NaN);
        assertTrue(result);
        assertEquals(2, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(2.2, list.get(1), DELTA);
    }

    @Test
    public void testRemoveAll() {
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
            DoubleList toRemove = DoubleList.of(2.2, 4.4);

            boolean result = list.removeAll(toRemove);
            assertTrue(result);
            assertEquals(3, list.size());
            assertEquals(1.1, list.get(0), DELTA);
            assertEquals(3.3, list.get(1), DELTA);
            assertEquals(5.5, list.get(2), DELTA);
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3, 2.2, 4.4 });

            // removeAll(DoubleList)
            assertTrue(list.removeAll(DoubleList.of(2.2, 4.4)));
            assertEquals(2, list.size());
            assertEquals(1.1, list.get(0), DELTA);
            assertEquals(3.3, list.get(1), DELTA);

            // removeAll with empty list
            assertFalse(list.removeAll(new DoubleList()));

            // removeAll(double[])
            list.addAll(new double[] { 5.5, 6.6 });
            assertTrue(list.removeAll(new double[] { 1.1, 5.5 }));
            assertEquals(2, list.size());
            assertEquals(3.3, list.get(0), DELTA);
            assertEquals(6.6, list.get(1), DELTA);

            // removeAll with empty array
            assertFalse(list.removeAll(new double[0]));
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
            double[] toRemove = { 2.2, 4.4 };

            boolean result = list.removeAll(toRemove);
            assertTrue(result);
            assertEquals(3, list.size());
        }
    }

    @Test
    public void testRemoveAll_Empty() {
        list.add(1.1);
        DoubleList toRemove = new DoubleList();
        boolean result = list.removeAll(toRemove);
        assertFalse(result);
        assertEquals(1, list.size());
    }

    @Test
    public void testRemoveAll_SharedBackingArray() {
        final double[] shared = { 1, 2, 3, 1 };
        final DoubleList values = DoubleList.of(shared);
        final DoubleList removed = DoubleList.of(shared, 2);

        assertTrue(values.removeAll(removed));
        assertArrayEquals(new double[] { 3 }, values.toArray());
    }

    @Test
    public void testRemoveIf() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });

        boolean result = list.removeIf(x -> x > 3.0);
        assertTrue(result);
        assertEquals(2, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(2.2, list.get(1), DELTA);
    }

    @Test
    public void testRemoveIf_NaN() {
        list.addAll(new double[] { 1.1, Double.NaN, 2.2, Double.NaN, 3.3 });

        boolean result = list.removeIf(Double::isNaN);
        assertTrue(result);
        assertEquals(3, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(2.2, list.get(1), DELTA);
        assertEquals(3.3, list.get(2), DELTA);
    }

    @Test
    public void testRemoveIf_Empty() {
        assertFalse(list.removeIf(x -> true));
    }

    @Test
    public void testRemoveIf_Infinity() {
        list.addAll(new double[] { 1.1, Double.POSITIVE_INFINITY, 2.2, Double.NEGATIVE_INFINITY, 3.3 });

        boolean result = list.removeIf(Double::isInfinite);
        assertTrue(result);
        assertEquals(3, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(2.2, list.get(1), DELTA);
        assertEquals(3.3, list.get(2), DELTA);
    }

    @Test
    public void testRemoveDuplicates() {
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 2.2, 3.3, 3.3, 3.3, 4.4 });

            boolean result = list.removeDuplicates();
            assertTrue(result);
            assertEquals(4, list.size());
            assertEquals(1.1, list.get(0), DELTA);
            assertEquals(2.2, list.get(1), DELTA);
            assertEquals(3.3, list.get(2), DELTA);
            assertEquals(4.4, list.get(3), DELTA);
        }
        {
            list = new DoubleList();
            list.add(1.1);
            boolean result = list.removeDuplicates();
            assertFalse(result);
            assertEquals(1, list.size());
        }
    }

    @Test
    public void testRemoveDuplicates_NaN() {
        list.addAll(new double[] { 1.1, Double.NaN, Double.NaN, 2.2 });
        assertTrue(list.removeDuplicates());
        assertEquals(3, list.size());
    }

    @Test
    public void testRemoveDuplicates_Empty() {
        boolean result = list.removeDuplicates();
        assertFalse(result);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testRemoveAt() {
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });

            // removeAt(int) - single index
            double removed = list.removeAt(2);
            assertEquals(3.3, removed, DELTA);
            assertEquals(4, list.size());

            // removeAllAt(int...) - multiple indices
            list.removeAllAt(0, 2);
            assertEquals(2, list.size());
            assertEquals(2.2, list.get(0), DELTA);
            assertEquals(5.5, list.get(1), DELTA);

            // removeAt with out of bounds
            assertThrows(IndexOutOfBoundsException.class, () -> list.removeAt(10));
        }
        {
            list = new DoubleList();
            DoubleList dl = DoubleList.of(1.0, 2.0, 3.0, 4.0, 5.0);
            dl.removeAllAt(1, 3);
            assertEquals(3, dl.size());
            assertEquals(1.0, dl.get(0), DELTA);
            assertEquals(3.0, dl.get(1), DELTA);
            assertEquals(5.0, dl.get(2), DELTA);
        }
    }

    @Test
    public void testRemoveRange() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });

        list.removeRange(1, 3);
        assertEquals(3, list.size());
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(4.4, list.get(1), DELTA);
        assertEquals(5.5, list.get(2), DELTA);

        // Empty range
        list.removeRange(1, 1);
        assertEquals(3, list.size());

        // Invalid range
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeRange(0, 10));
    }

    @Test
    public void testRemoveFirst() {
        {
            list = new DoubleList();
            list.add(5.5);
            assertEquals(5.5, list.removeFirst(), DELTA);
            assertTrue(list.isEmpty());

            list.add(10.5);
            assertEquals(10.5, list.removeLast(), DELTA);
            assertTrue(list.isEmpty());
        }
        {
            list = new DoubleList();
            list.add(10.5);
            list.add(20.5);
            list.add(10.5);

            boolean result = list.remove(10.5);
            assertTrue(result);
            assertEquals(2, list.size());
            assertEquals(20.5, list.get(0), DELTA);
            assertEquals(10.5, list.get(1), DELTA);
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3 });
            double removed = list.removeFirst();
            assertEquals(1.1, removed, DELTA);
            assertEquals(2, list.size());
            assertEquals(2.2, list.get(0), DELTA);
        }
    }

    @Test
    public void testRemoveFirst_Empty() {
        assertThrows(NoSuchElementException.class, () -> list.removeFirst());
    }

    @Test
    public void testRemoveLast() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        double removed = list.removeLast();
        assertEquals(3.3, removed, DELTA);
        assertEquals(2, list.size());
        assertEquals(2.2, list.get(1), DELTA);
    }

    @Test
    public void testRemoveLast_Empty() {
        assertThrows(NoSuchElementException.class, () -> list.removeLast());
    }
}
