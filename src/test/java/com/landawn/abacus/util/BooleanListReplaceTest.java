package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class BooleanListReplaceTest extends BooleanListTestSupport {

    @Test
    public void testReplaceRange() {
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, false, true);
            list.replaceRange(1, 3, new boolean[] { true, true, true, true });
            assertArrayEquals(new boolean[] { true, true, true, true, true, true }, list.toArray());
        }
        {
            list = new BooleanList();
            // Replace with array
            BooleanList a = BooleanList.of(true, false, true, false, true);
            boolean[] rep = { false, false, false };
            a.replaceRange(1, 3, rep);
            // [true, false, false, false, false, true]
            assertEquals(6, a.size());
            assertTrue(a.get(0));
            assertFalse(a.get(1));
            assertFalse(a.get(2));
            assertFalse(a.get(3));
            assertFalse(a.get(4));
            assertTrue(a.get(5));
        }
        {
            list = new BooleanList();
            // replace 2 elements with 2 elements: size unchanged
            BooleanList a = BooleanList.of(true, false, true, false, true);
            BooleanList replacement = BooleanList.of(false, false);
            a.replaceRange(1, 3, replacement);
            assertEquals(5, a.size());
            assertTrue(a.get(0));
            assertFalse(a.get(1));
            assertFalse(a.get(2));
            assertFalse(a.get(3));
            assertTrue(a.get(4));
        }
        {
            list = new BooleanList();
            list.add(true);
            list.add(false);
            list.add(true);
            list.replaceRange(1, 2, new boolean[] { false, true, false });
            assertEquals(5, list.size());
        }
    }

    @Test
    public void testReplaceRange_Empty() {
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, true, false);
            BooleanList replacement = new BooleanList();
            list.replaceRange(1, 3, replacement);
            assertEquals(2, list.size());
            assertTrue(list.get(0));
            assertFalse(list.get(1));
        }
        {
            list = new BooleanList();
            list.add(true);
            list.add(false);
            list.add(true);
            list.replaceRange(1, 2, new boolean[] {});
            assertEquals(2, list.size());
            assertTrue(list.get(0));
            assertTrue(list.get(1));
        }
    }

    @Test
    public void testReplaceAll() {
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, true);
            list.replaceAll(val -> !val);
            assertArrayEquals(new boolean[] { false, true, false }, list.toArray());
        }
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, true);
            int replacements = list.replaceAll(true, false);
            assertEquals(2, replacements);
            assertArrayEquals(new boolean[] { false, false, false }, list.toArray());
        }
        {
            list = new BooleanList();
            list.add(true);
            list.add(false);
            int count = list.replaceAll(true, true);
            assertEquals(1, count);
        }
    }

    @Test
    public void testReplaceAll_Null() {
        BooleanList nonEmpty = BooleanList.of(true, false);
        assertThrows(IllegalArgumentException.class, () -> nonEmpty.replaceAll((com.landawn.abacus.util.function.BooleanUnaryOperator) null));

        BooleanList empty = new BooleanList();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> empty.replaceAll((com.landawn.abacus.util.function.BooleanUnaryOperator) null));
    }

    @Test
    public void testReplaceAll_Empty() {
        BooleanList list = new BooleanList();
        int count = list.replaceAll(true, false);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceIf() {
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, true, false);
            boolean result = list.replaceIf(b -> b, false);
            assertTrue(result);
            assertFalse(list.get(0));
            assertFalse(list.get(1));
            assertFalse(list.get(2));
            assertFalse(list.get(3));
        }
        {
            list = new BooleanList();
            list.add(true);
            list.add(true);
            list.add(true);
            assertTrue(list.replaceIf(b -> b, false));
            assertEquals(3, list.size());
            for (int i = 0; i < 3; i++) {
                assertFalse(list.get(i));
            }
        }
    }
}
