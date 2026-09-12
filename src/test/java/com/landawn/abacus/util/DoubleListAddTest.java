package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class DoubleListAddTest extends DoubleListTestSupport {

    @Test
    public void testAdd() {
        {
            list = new DoubleList();
            list.add(10.5);
            list.add(30.5);
            list.add(1, 20.5);

            assertEquals(3, list.size());
            assertEquals(10.5, list.get(0), DELTA);
            assertEquals(20.5, list.get(1), DELTA);
            assertEquals(30.5, list.get(2), DELTA);
        }
        {
            list = new DoubleList();
            assertThrows(IndexOutOfBoundsException.class, () -> list.add(-1, 10.5));
        }
    }

    @Test
    public void testAdd_LargeData() {
        int count = 1000;
        for (int i = 0; i < count; i++) {
            list.add(i * 0.1);
        }
        assertEquals(count, list.size());

        list.removeIf(x -> ((int) (x * 10)) % 2 == 0);
        assertEquals(count / 2, list.size());
    }

    @Test
    public void testAdd_OutOfBounds() {
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(1, 10.5));
    }

    @Test
    public void testAddAll() {
        {
            list = new DoubleList();
            list.add(1.1);
            list.add(2.2);

            DoubleList other = DoubleList.of(3.3, 4.4, 5.5);
            boolean result = list.addAll(other);

            assertTrue(result);
            assertEquals(5, list.size());
            assertEquals(3.3, list.get(2), DELTA);
            assertEquals(5.5, list.get(4), DELTA);
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.0, 4.0 });
            DoubleList toAdd = DoubleList.of(2.0, 3.0);
            assertTrue(list.addAll(1, toAdd));
            assertEquals(4, list.size());
            assertEquals(2.0, list.get(1), DELTA);
            assertEquals(3.0, list.get(2), DELTA);
        }
        {
            list = new DoubleList();
            list.add(1.1);
            list.add(4.4);
            double[] array = { 2.2, 3.3 };
            boolean result = list.addAll(1, array);

            assertTrue(result);
            assertEquals(4, list.size());
            assertEquals(2.2, list.get(1), DELTA);
            assertEquals(3.3, list.get(2), DELTA);
        }
    }

    @Test
    public void testAddAll_Empty() {
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3 });
            DoubleList empty = new DoubleList();

            assertFalse(list.addAll(0, empty));
            assertFalse(list.addAll(1, empty));
            assertFalse(list.addAll(list.size(), empty));
        }
        {
            list = new DoubleList();
            list.add(1.1);
            DoubleList empty = new DoubleList();
            boolean result = list.addAll(empty);

            assertFalse(result);
            assertEquals(1, list.size());
        }
        {
            list = new DoubleList();
            list.add(1.1);
            double[] array = {};
            boolean result = list.addAll(array);

            assertFalse(result);
            assertEquals(1, list.size());
        }
        {
            list = new DoubleList();
            DoubleList dl = DoubleList.of(1.0, 2.0);
            assertFalse(dl.addAll(1, DoubleList.of()));
            assertEquals(2, dl.size());
        }
    }

    @Test
    public void testAddAll_Null() {
        {
            list = new DoubleList();
            list.add(1.1);
            boolean result = list.addAll((double[]) null);
            assertFalse(result);
            assertEquals(1, list.size());
        }
        {
            list = new DoubleList();
            list.add(1.1);
            boolean result = list.addAll((DoubleList) null);
            assertFalse(result);
            assertEquals(1, list.size());
        }
    }

    @Test
    public void testAddAll_OutOfBounds() {
        list.add(1.1);
        DoubleList other = DoubleList.of(2.2, 3.3);
        assertThrows(IndexOutOfBoundsException.class, () -> list.addAll(2, other));
    }

    @Test
    public void testAddFirst() {
        list.addAll(new double[] { 2.2, 3.3 });
        list.addFirst(1.1);
        assertEquals(3, list.size());
        assertEquals(1.1, list.get(0), DELTA);
    }

    @Test
    public void testAddLast() {
        list.addAll(new double[] { 1.1, 2.2 });
        list.addLast(3.3);
        assertEquals(3, list.size());
        assertEquals(3.3, list.get(2), DELTA);
    }
}
