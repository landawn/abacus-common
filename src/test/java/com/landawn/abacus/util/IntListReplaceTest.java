package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;

import org.junit.jupiter.api.Test;

public class IntListReplaceTest extends IntListTestSupport {

    @Test
    public void testReplaceRange() {
        {
            list = new IntList();
            IntList list = IntList.of(0, 1, 2, 3, 4, 5);
            int[] replacement = { 9, 8, 7 };
            list.replaceRange(1, 4, replacement);
            assertArrayEquals(new int[] { 0, 9, 8, 7, 4, 5 }, list.toArray());
        }
        {
            list = new IntList();
            // replace 2 elements with 2 elements: size unchanged
            IntList a = IntList.of(1, 2, 3, 4, 5);
            IntList replacement = IntList.of(20, 30);
            a.replaceRange(1, 3, replacement);
            assertEquals(5, a.size());
            assertEquals(1, a.get(0));
            assertEquals(20, a.get(1));
            assertEquals(30, a.get(2));
            assertEquals(4, a.get(3));
            assertEquals(5, a.get(4));
        }
        {
            list = new IntList();
            list.addAll(new int[] { 1, 2, 3 });
            list.replaceRange(1, 2, IntList.of(10, 20, 30));
            assertEquals(5, list.size());
            assertEquals(1, list.get(0));
            assertEquals(10, list.get(1));
            assertEquals(20, list.get(2));
            assertEquals(30, list.get(3));
            assertEquals(3, list.get(4));
        }
    }

    @Test
    public void testReplaceRange_Empty() {
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3, 4, 5);
            IntList replacement = new IntList();
            list.replaceRange(1, 4, replacement);
            assertEquals(2, list.size());
            assertEquals(1, list.get(0));
            assertEquals(5, list.get(1));
        }
        {
            list = new IntList();
            list.addAll(new int[] { 1, 2, 3, 4, 5 });
            list.replaceRange(1, 3, new IntList());
            assertEquals(3, list.size());
            assertEquals(1, list.get(0));
            assertEquals(4, list.get(1));
            assertEquals(5, list.get(2));
        }
    }

    @Test
    public void testReplaceAll() {
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3, 4);
            IntUnaryOperator operator = (n) -> n * 2;
            list.replaceAll(operator);
            assertArrayEquals(new int[] { 2, 4, 6, 8 }, list.toArray());
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 1, 3, 1);
            int count = list.replaceAll(1, 99);
            assertEquals(3, count);
            assertArrayEquals(new int[] { 99, 2, 99, 3, 99 }, list.toArray());
        }
        {
            list = new IntList();
            list.addAll(new int[] { 1, 2, 3 });
            int count = list.replaceAll(5, 10);
            assertEquals(0, count);
        }
    }

    @Test
    public void testReplaceAll_Empty() {
        IntList a = new IntList();
        int count = a.replaceAll(1, 2);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceAll_Null() {
        IntList list = IntList.of(1, 2, 3);
        assertThrows(IllegalArgumentException.class, () -> list.replaceAll((java.util.function.IntUnaryOperator) null));

        IntList empty = new IntList();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> empty.replaceAll((java.util.function.IntUnaryOperator) null));
    }

    @Test
    public void testReplaceIf() {
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3, 4, 5);
            IntPredicate predicate = (n) -> n > 3;
            assertTrue(list.replaceIf(predicate, 99));
            assertArrayEquals(new int[] { 1, 2, 3, 99, 99 }, list.toArray());
        }
        {
            list = new IntList();
            list.addAll(new int[] { 1, 2, 3 });
            boolean result = list.replaceIf(x -> false, 10);
            assertFalse(result);
        }
    }

    @Test
    public void testReplaceIf_Null() {
        IntList list = IntList.of(1, 2, 3);
        assertThrows(IllegalArgumentException.class, () -> list.replaceIf(null, 99));
    }
}
