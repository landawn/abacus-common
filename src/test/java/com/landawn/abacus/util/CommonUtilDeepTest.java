package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class CommonUtilDeepTest extends CommonUtilTestSupport {

    @Test
    public void testDeepEquals() {
        assertTrue(CommonUtil.deepEquals(5, 5));
        assertFalse(CommonUtil.deepEquals(5, 6));
        assertTrue(CommonUtil.deepEquals("test", "test"));
        assertFalse(CommonUtil.deepEquals("test", "Test"));
        assertTrue(CommonUtil.deepEquals(null, null));
        assertFalse(CommonUtil.deepEquals(null, "test"));
        assertFalse(CommonUtil.deepEquals("test", null));

        assertTrue(CommonUtil.deepEquals(new int[] { 1, 2 }, new int[] { 1, 2 }));
        assertFalse(CommonUtil.deepEquals(new int[] { 1, 2 }, new int[] { 2, 1 }));
        assertTrue(CommonUtil.deepEquals(new String[] { "a", "b" }, new String[] { "a", "b" }));

        Object[] a = { 1, new String[] { "a", "b" }, new int[] { 10, 20 } };
        Object[] b = { 1, new String[] { "a", "b" }, new int[] { 10, 20 } };
        Object[] c = { 1, new String[] { "a", "DIFFERENT" }, new int[] { 10, 20 } };
        assertTrue(CommonUtil.deepEquals(a, b));
        assertFalse(CommonUtil.deepEquals(a, c));
        assertTrue(CommonUtil.deepEquals((Object[]) null, (Object[]) null));
        assertFalse(CommonUtil.deepEquals(a, null));

        assertTrue(CommonUtil.deepEquals((Object) new String[] { "a" }, (Object) new Object[] { "a" }));
        assertTrue(CommonUtil.deepEquals((Object) new Integer[] { 1 }, (Object) new Number[] { 1 }));
        assertFalse(CommonUtil.deepEquals((Object) new String[] { "a" }, (Object) new Object[] { "b" }));
        assertFalse(CommonUtil.deepEquals(new int[] { 1 }, new long[] { 1 }));
    }

    @Test
    public void testDeepEquals_range() {
        String[] a = { "a", "b", "c", "d" };
        String[] b = { "x", "b", "c", "y" };
        assertTrue(CommonUtil.deepEquals(a, 1, b, 1, 2));
        assertFalse(CommonUtil.deepEquals(a, 0, b, 0, 2));

        Object[] nested1 = { "a", new int[] { 1, 2 }, "c", new int[] { 3, 4 } };
        Object[] nested2 = { "x", new int[] { 1, 2 }, "c", new int[] { 5, 6 } };
        assertTrue(CommonUtil.deepEquals(nested1, 1, nested2, 1, 2));
        assertFalse(CommonUtil.deepEquals(nested1, 2, nested2, 2, 2));
        assertFalse(CommonUtil.deepEquals(new Integer[] { 1 }, 0, new Long[] { 1L }, 0, 1));

        Object[] arr1 = { "first", new int[] { 1, 2 }, "mid", new String[] { "x", "y" }, "last" };
        Object[] arr2 = { "other", new int[] { 1, 2 }, "mid", new String[] { "x", "y" }, "end" };
        Object[] arr3 = { "first", new int[] { 1, 2 }, "mid", new String[] { "x", "y" }, "last" };
        assertTrue(CommonUtil.deepEquals(arr1, 1, arr3, 1, 3));
        assertFalse(CommonUtil.deepEquals(arr1, 0, arr2, 0, 2));
        assertTrue(CommonUtil.deepEquals(arr1, 0, arr1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.deepEquals(arr1, 0, arr3, 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.deepEquals(arr1, 0, arr3, 0, 6));
    }

    @Test
    public void testDeepHashCode() {
        assertEquals(0, CommonUtil.deepHashCode((Object) null));
        assertEquals("test".hashCode(), CommonUtil.deepHashCode("test"));
        assertEquals(Arrays.hashCode(new int[] { 1, 2, 3 }), CommonUtil.deepHashCode(new int[] { 1, 2, 3 }));
        assertEquals(Arrays.deepHashCode(new Object[] { 1, new int[] { 2, 3 }, "test" }),
                CommonUtil.deepHashCode(new Object[] { 1, new int[] { 2, 3 }, "test" }));
        assertEquals(0, CommonUtil.deepHashCode((Object[]) null));
        assertEquals(CommonUtil.hashCode(new boolean[] { true, false, true }), CommonUtil.deepHashCode(new boolean[] { true, false, true }));
        assertEquals(CommonUtil.hashCode(new char[] { 'a', 'b' }), CommonUtil.deepHashCode(new char[] { 'a', 'b' }));
        assertEquals(CommonUtil.hashCode(new byte[] { 1, 2, 3 }), CommonUtil.deepHashCode(new byte[] { 1, 2, 3 }));
        assertEquals(CommonUtil.hashCode(new int[] { 10, 20 }), CommonUtil.deepHashCode(new int[] { 10, 20 }));
        assertTrue(CommonUtil.deepHashCode((Object) new String[][] { { "a", "b" }, { "c" } }) != 0);

        Object[] arr = { "X", new int[] { 100, 200 }, new String[] { "deep" } };
        int expected = 1;
        expected = 31 * expected + CommonUtil.deepHashCode(new int[] { 100, 200 });
        expected = 31 * expected + CommonUtil.deepHashCode(new String[] { "deep" });
        assertEquals(expected, CommonUtil.deepHashCode(arr, 1, 3));
        assertEquals(1, CommonUtil.deepHashCode(arr, 1, 1));

        Object[] mixed = { "a", new int[] { 1, 2 }, "c", new int[] { 3, 4 } };
        expected = 1;
        expected = 31 * expected + Arrays.hashCode(new int[] { 1, 2 });
        expected = 31 * expected + "c".hashCode();
        assertEquals(expected, CommonUtil.deepHashCode(mixed, 1, 3));
    }

    @Test
    public void testDeepToString() {
        assertEquals("null", CommonUtil.deepToString(null));
        assertEquals("test", CommonUtil.deepToString("test"));
        assertEquals("[1, 2, 3]", CommonUtil.deepToString(new int[] { 1, 2, 3 }));
        assertEquals("[1, [2, 3], test]", CommonUtil.deepToString(new Object[] { 1, new int[] { 2, 3 }, "test" }));
        assertEquals("[true, false]", CommonUtil.deepToString(new boolean[] { true, false }));
        assertEquals("[a, b]", CommonUtil.deepToString(new char[] { 'a', 'b' }));
        assertEquals("[1, 2]", CommonUtil.deepToString(new byte[] { 1, 2 }));
        assertEquals("[1, 2]", CommonUtil.deepToString(new short[] { 1, 2 }));
        assertEquals("[1, 2]", CommonUtil.deepToString(new long[] { 1L, 2L }));
        assertEquals("[1.0, 2.0]", CommonUtil.deepToString(new float[] { 1.0f, 2.0f }));
        assertEquals("[1.0, 2.0]", CommonUtil.deepToString(new double[] { 1.0, 2.0 }));
        assertEquals("[a, b]", CommonUtil.deepToString(new String[] { "a", "b" }));
        assertEquals("[a, 1, true]", CommonUtil.deepToString(new Object[] { "a", 1, true }));
        assertEquals("null", CommonUtil.deepToString((Object[]) null));
        assertEquals("[]", CommonUtil.deepToString(new Object[0]));
        assertEquals("[[1, 2], [a, b]]", CommonUtil.deepToString(new Object[] { new int[] { 1, 2 }, new String[] { "a", "b" } }));
        assertTrue(CommonUtil.deepToString(new Object[] { "first", null, "third" }).contains("null"));

        Object[] cyclic = new Object[2];
        cyclic[0] = "Element 1";
        cyclic[1] = cyclic;
        assertEquals("[Element 1, [...]]", CommonUtil.deepToString(cyclic));
        Object[] self = new Object[1];
        self[0] = self;
        assertEquals("[[...]]", CommonUtil.deepToString(self));
        Object[] parent = new Object[1];
        Object[] child = new Object[1];
        parent[0] = child;
        child[0] = parent;
        assertEquals("[[[...]]]", CommonUtil.deepToString(parent));
        Object[] shared = new Object[2];
        Object[] inner = new Object[] { "b" };
        shared[0] = inner;
        shared[1] = inner;
        assertEquals("[[b], [b]]", CommonUtil.deepToString(shared));

        assertEquals("NULL", CommonUtil.deepToString((Object[]) null, "NULL"));
        assertEquals("[]", CommonUtil.deepToString(new Object[0], "fallback"));
        assertEquals("[a, b]", CommonUtil.deepToString(new Object[] { "a", "b" }, "default"));
        assertEquals("null", CommonUtil.deepToString((Object[]) null, 0, 0));
        assertEquals("[[1, 2], c]", CommonUtil.deepToString(new Object[] { "a", new int[] { 1, 2 }, "c", new int[] { 3, 4 } }, 1, 3));
        Object[] ranged = { "start", new Object[] { "nested1", new int[] { 10, 20 } }, "middle", new String[] { "s1", "s2" }, "end" };
        assertEquals("[[nested1, [10, 20]], middle, [s1, s2]]", CommonUtil.deepToString(ranged, 1, 4));
        assertEquals("[]", CommonUtil.deepToString(ranged, 1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.deepToString(ranged, 0, 6));
    }
}
