package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class CommonUtilHashTest extends CommonUtilTestSupport {

    @Test
    public void testHashCode_primitives() {
        assertEquals(1231, CommonUtil.hashCode(true));
        assertEquals(1237, CommonUtil.hashCode(false));
        assertEquals('a', CommonUtil.hashCode('a'));
        assertEquals('Z', CommonUtil.hashCode('Z'));
        assertEquals(5, CommonUtil.hashCode((byte) 5));
        assertEquals(-1, CommonUtil.hashCode((byte) -1));
        assertEquals(100, CommonUtil.hashCode((short) 100));
        assertEquals(-50, CommonUtil.hashCode((short) -50));
        assertEquals(42, CommonUtil.hashCode(42));
        assertEquals(-42, CommonUtil.hashCode(-42));
        assertEquals(Long.hashCode(100L), CommonUtil.hashCode(100L));
        assertEquals(Long.hashCode(-100L), CommonUtil.hashCode(-100L));
        assertEquals(Float.floatToIntBits(3.14f), CommonUtil.hashCode(3.14f));
        assertEquals(Float.floatToIntBits(Float.NaN), CommonUtil.hashCode(Float.NaN));
        assertEquals(Double.hashCode(3.14), CommonUtil.hashCode(3.14));
        assertEquals(Double.hashCode(Double.NaN), CommonUtil.hashCode(Double.NaN));
    }

    @Test
    public void testHashCode_arrays() {
        assertEquals(0, CommonUtil.hashCode((boolean[]) null));
        assertEquals(0, CommonUtil.hashCode((char[]) null));
        assertEquals(0, CommonUtil.hashCode((byte[]) null));
        assertEquals(0, CommonUtil.hashCode((short[]) null));
        assertEquals(0, CommonUtil.hashCode((int[]) null));
        assertEquals(0, CommonUtil.hashCode((long[]) null));
        assertEquals(0, CommonUtil.hashCode((float[]) null));
        assertEquals(0, CommonUtil.hashCode((double[]) null));
        assertEquals(0, CommonUtil.hashCode((Object[]) null));

        boolean[] ba1 = { true, false };
        boolean[] ba2 = { true, false };
        boolean[] ba3 = { false, true };
        assertEquals(CommonUtil.hashCode(ba1), CommonUtil.hashCode(ba2));
        assertNotEquals(CommonUtil.hashCode(ba1), CommonUtil.hashCode(ba3));
        assertEquals(Arrays.hashCode(new boolean[0]), CommonUtil.hashCode(new boolean[0]));

        assertEquals(Arrays.hashCode(new boolean[] { true, false, true }), CommonUtil.hashCode(new boolean[] { true, false, true }));
        assertEquals(Arrays.hashCode(new char[] { 'a', 'b', 'c' }), CommonUtil.hashCode(new char[] { 'a', 'b', 'c' }));
        assertEquals(Arrays.hashCode(new byte[] { 1, 2, 3 }), CommonUtil.hashCode(new byte[] { 1, 2, 3 }));
        assertEquals(Arrays.hashCode(new short[] { 1, 2, 3 }), CommonUtil.hashCode(new short[] { 1, 2, 3 }));
        assertEquals(Arrays.hashCode(new int[] { 1, 2, 3 }), CommonUtil.hashCode(new int[] { 1, 2, 3 }));
        assertEquals(Arrays.hashCode(new long[] { 1L, 2L, 3L }), CommonUtil.hashCode(new long[] { 1L, 2L, 3L }));
        assertEquals(Arrays.hashCode(new float[] { 1.0f, 2.0f, 3.0f }), CommonUtil.hashCode(new float[] { 1.0f, 2.0f, 3.0f }));
        assertEquals(Arrays.hashCode(new double[] { 1.0, 2.0, 3.0 }), CommonUtil.hashCode(new double[] { 1.0, 2.0, 3.0 }));
        assertEquals(Arrays.hashCode(new String[] { "a", "b", "c" }), CommonUtil.hashCode(new String[] { "a", "b", "c" }));

        int[] ints = { 1, 2 };
        int expected = 1;
        expected = 31 * expected + 1;
        expected = 31 * expected + 2;
        assertEquals(expected, CommonUtil.hashCode(ints));
    }

    @Test
    public void testHashCode_arrayRange() {
        assertEquals(0, CommonUtil.hashCode((boolean[]) null, 0, 0));
        assertEquals(0, CommonUtil.hashCode((char[]) null, 0, 0));
        assertEquals(0, CommonUtil.hashCode((byte[]) null, 0, 0));
        assertEquals(0, CommonUtil.hashCode((int[]) null, 0, 0));

        boolean[] bools = { true, false, true, false, true };
        assertEquals(1, CommonUtil.hashCode(bools, 1, 1));
        assertNotEquals(CommonUtil.hashCode(bools, 0, bools.length), CommonUtil.hashCode(bools, 1, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.hashCode(bools, 0, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.hashCode(bools, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.hashCode(bools, 3, 2));

        int range = 1;
        range = 31 * range + 1231;
        range = 31 * range + 1237;
        assertEquals(range, CommonUtil.hashCode(new boolean[] { false, true, false, true }, 1, 3));

        int expected = 1;
        expected = 31 * expected + 2;
        expected = 31 * expected + 3;
        assertEquals(expected, CommonUtil.hashCode(new byte[] { 1, 2, 3, 4, 5 }, 1, 3));
        assertEquals(expected, CommonUtil.hashCode(new short[] { 1, 2, 3, 4, 5 }, 1, 3));
        assertEquals(expected, CommonUtil.hashCode(new int[] { 1, 2, 3, 4, 5 }, 1, 3));
        assertEquals(1, CommonUtil.hashCode(new int[] { 10, 20, 30, 40, 50 }, 1, 1));

        expected = 1;
        expected = 31 * expected + Long.hashCode(2L);
        expected = 31 * expected + Long.hashCode(3L);
        assertEquals(expected, CommonUtil.hashCode(new long[] { 1L, 2L, 3L, 4L, 5L }, 1, 3));

        expected = 1;
        expected = 31 * expected + Float.floatToIntBits(2.0f);
        expected = 31 * expected + Float.floatToIntBits(3.0f);
        assertEquals(expected, CommonUtil.hashCode(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, 1, 3));

        expected = 1;
        expected = 31 * expected + Double.hashCode(2.0);
        expected = 31 * expected + Double.hashCode(3.0);
        assertEquals(expected, CommonUtil.hashCode(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, 1, 3));

        expected = 1;
        expected = 31 * expected + 'a';
        expected = 31 * expected + 'b';
        assertEquals(expected, CommonUtil.hashCode(new char[] { 'x', 'a', 'b', 'y' }, 1, 3));

        expected = 1;
        expected = 31 * expected + "b".hashCode();
        expected = 31 * expected + "c".hashCode();
        assertEquals(expected, CommonUtil.hashCode(new String[] { "a", "b", "c", "d", "e" }, 1, 3));

        expected = 1;
        expected = 31 * expected + "B".hashCode();
        expected = 31 * expected + 0;
        assertEquals(expected, CommonUtil.hashCode(new Object[] { "A", "B", null, "D" }, 1, 3));
        assertEquals(1, CommonUtil.hashCode(new Object[] { "A", "B", null, "D" }, 1, 1));
    }

    @Test
    public void testHashCode_object() {
        assertEquals(0, CommonUtil.hashCode((Object) null));
        assertEquals("test".hashCode(), CommonUtil.hashCode("test"));
        assertEquals(Arrays.hashCode(new int[] { 1, 2, 3 }), CommonUtil.hashCode(new int[] { 1, 2, 3 }));
        assertEquals(Arrays.hashCode(new String[] { "a", "b" }), CommonUtil.hashCode(new String[] { "a", "b" }));
        assertTrue(CommonUtil.equals(new int[] { 1, 2, 3 }, new int[] { 1, 2, 3 }));
        assertEquals(CommonUtil.hashCode(new int[] { 1, 2, 3 }), CommonUtil.hashCode(new int[] { 1, 2, 3 }));
    }

    @Test
    public void testHashCodeEverything() {
        assertEquals(0L, CommonUtil.hashCodeEverything(null));
        assertEquals(CommonUtil.hashCodeEverything("hello"), CommonUtil.hashCodeEverything("hello"));
        assertTrue(CommonUtil.hashCodeEverything("test") != 0);

        assertEquals(CommonUtil.hashCodeEverything(Arrays.asList(1, 2, 3)), CommonUtil.hashCodeEverything(Arrays.asList(1, 2, 3)));
        assertTrue(CommonUtil.hashCodeEverything(Arrays.asList(1, 2, 3)) != CommonUtil.hashCodeEverything(Arrays.asList(1, 2, 4)));

        LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertTrue(CommonUtil.hashCodeEverything(map) != 0);

        assertTrue(CommonUtil.hashCodeEverything(new int[] { 1, 2, 3 }) != 0);
        assertEquals(CommonUtil.hashCodeEverything(new Object[] { "a", "b" }), CommonUtil.hashCodeEverything(new Object[] { "a", "b" }));

        DatasetRowBean b1 = new DatasetRowBean("Tom", 10);
        DatasetRowBean b2 = new DatasetRowBean("Tom", 10);
        DatasetRowBean b3 = new DatasetRowBean("Jerry", 12);
        assertEquals(CommonUtil.hashCodeEverything(b1), CommonUtil.hashCodeEverything(b2));
        assertNotEquals(CommonUtil.hashCodeEverything(b1), CommonUtil.hashCodeEverything(b3));

        List<Object> nested = Arrays.asList("alpha", Arrays.asList(1, 2), new int[] { 3, 4 });
        Map<String, Object> left = new LinkedHashMap<>();
        left.put("values", nested);
        left.put("flag", true);
        Map<String, Object> right = new LinkedHashMap<>();
        right.put("values", Arrays.asList("alpha", Arrays.asList(1, 2), new int[] { 3, 4 }));
        right.put("flag", true);
        assertEquals(CommonUtil.hashCodeEverything(left), CommonUtil.hashCodeEverything(right));
        assertEquals(CommonUtil.hashCodeEverything(nested), CommonUtil.hashCodeEverything(nested.iterator()));
        assertNotEquals(CommonUtil.hashCodeEverything(left), CommonUtil.hashCodeEverything(Collections.singletonMap("values", nested)));
    }
    @Test
    public void testHashCode_arrayRangeNullVsEmptyIsInconsistentWithRangeEquals() {
        assertTrue(CommonUtil.equals((Object[]) null, 0, new Object[0], 0, 0));
        assertEquals(0, CommonUtil.hashCode((Object[]) null, 0, 0));
        assertEquals(1, CommonUtil.hashCode(new Object[0], 0, 0));

        assertTrue(CommonUtil.equals((int[]) null, 0, new int[0], 0, 0));
        assertEquals(0, CommonUtil.hashCode((int[]) null, 0, 0));
        assertEquals(1, CommonUtil.hashCode(new int[0], 0, 0));

        final Object[] a = { "a", "b" };
        final Object[] b = { "a", "b" };
        assertTrue(CommonUtil.equals(a, 0, b, 0, 2));
        assertEquals(CommonUtil.hashCode(a, 0, 2), CommonUtil.hashCode(b, 0, 2));

        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.hashCode((Object[]) null, 0, 1));
    }

}
