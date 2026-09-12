package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class CommonUtilCompareTest extends CommonUtilTestSupport {

    @Test
    public void testCompare_primitives() {
        assertEquals(0, CommonUtil.compare(true, true));
        assertEquals(1, CommonUtil.compare(true, false));
        assertEquals(-1, CommonUtil.compare(false, true));
        assertEquals(0, CommonUtil.compare('a', 'a'));
        assertEquals(-1, CommonUtil.compare('a', 'b'));
        assertEquals(1, CommonUtil.compare('b', 'a'));
        assertEquals(0, CommonUtil.compare((byte) 5, (byte) 5));
        assertTrue(CommonUtil.compare((byte) 3, (byte) 5) < 0);
        assertEquals(0, CommonUtil.compare((short) 5, (short) 5));
        assertTrue(CommonUtil.compare((short) 7, (short) 5) > 0);
        assertEquals(0, CommonUtil.compare(5, 5));
        assertTrue(CommonUtil.compare(3, 5) < 0);
        assertEquals(0, CommonUtil.compare(5L, 5L));
        assertTrue(CommonUtil.compare(7L, 5L) > 0);
        assertEquals(0, CommonUtil.compare(1.5f, 1.5f));
        assertEquals(-1, CommonUtil.compare(1.0f, 1.5f));
        assertEquals(1, CommonUtil.compare(Float.NaN, 1.0f));
        assertEquals(0, CommonUtil.compare(1.5, 1.5));
        assertEquals(1, CommonUtil.compare(Double.NaN, 1.0));
        assertEquals(0, CommonUtil.compare("abc", "abc"));
        assertTrue(CommonUtil.compare("abc", "def") < 0);
        assertTrue(CommonUtil.compare(null, "abc") < 0);
        assertTrue(CommonUtil.compare("abc", null) > 0);
        assertEquals(0, CommonUtil.compare((String) null, null));
    }

    @Test
    public void testCompare_arrays() {
        assertEquals(0, CommonUtil.compare(new boolean[] { true, false, true }, new boolean[] { true, false, true }));
        assertEquals(-1, CommonUtil.compare(new boolean[] { false }, new boolean[] { true }));
        assertEquals(0, CommonUtil.compare((boolean[]) null, (boolean[]) null));
        assertEquals(-1, CommonUtil.compare((boolean[]) null, new boolean[] { true }));
        assertEquals(1, CommonUtil.compare(new boolean[] { true }, (boolean[]) null));
        assertEquals(-1, CommonUtil.compare(new boolean[] {}, new boolean[] { true }));
        assertEquals(1, CommonUtil.compare(new boolean[] { true }, new boolean[] {}));
        boolean[] large1 = new boolean[2000];
        boolean[] large2 = new boolean[2000];
        Arrays.fill(large1, true);
        Arrays.fill(large2, true);
        large2[1999] = false;
        assertEquals(1, CommonUtil.compare(large1, large2));

        assertEquals(0, CommonUtil.compare(new char[] { 'a', 'b', 'c' }, new char[] { 'a', 'b', 'c' }));
        assertEquals(-1, CommonUtil.compare(new char[] { 'a' }, new char[] { 'b' }));
        assertEquals(0, CommonUtil.compare((char[]) null, (char[]) null));
        char[] largeC1 = new char[2000];
        char[] largeC2 = new char[2000];
        Arrays.fill(largeC1, 'x');
        Arrays.fill(largeC2, 'x');
        largeC2[1999] = 'y';
        assertEquals(-1, CommonUtil.compare(largeC1, largeC2));

        assertEquals(0, CommonUtil.compare(new byte[] { 1, 2, 3 }, new byte[] { 1, 2, 3 }));
        assertEquals(-1, CommonUtil.compare(new byte[] { -128 }, new byte[] { 127 }));
        assertEquals(1, CommonUtil.compare(new byte[] { 127 }, new byte[] { -128 }));
        assertEquals(-1, CommonUtil.compare(new byte[] {}, new byte[] { 1 }));
        assertEquals(0, CommonUtil.compare(new short[] { 1, 2, 3 }, new short[] { 1, 2, 3 }));
        assertEquals(-1, CommonUtil.compare(new short[] {}, new short[] { 1 }));
        assertEquals(0, CommonUtil.compare(new int[] { 1, 2, 3 }, new int[] { 1, 2, 3 }));
        assertEquals(1, CommonUtil.compare(new int[] { 1 }, new int[] {}));
        assertEquals(0, CommonUtil.compare(new long[] { 1L, 2L, 3L }, new long[] { 1L, 2L, 3L }));
        assertEquals(0, CommonUtil.compare(new float[] { 1.0f, 2.0f, 3.0f }, new float[] { 1.0f, 2.0f, 3.0f }));
        assertTrue(CommonUtil.compare(new float[] { Float.NaN }, new float[] { 1.0f }) > 0);
        assertEquals(0, CommonUtil.compare(new float[] { Float.NaN }, new float[] { Float.NaN }));
        assertEquals(0, CommonUtil.compare(new double[] { 1.0, 2.0, 3.0 }, new double[] { 1.0, 2.0, 3.0 }));
        assertTrue(CommonUtil.compare(new double[] { Double.NaN }, new double[] { 1.0 }) > 0);
        assertEquals(0, CommonUtil.compare(new double[] { Double.NaN }, new double[] { Double.NaN }));
        assertEquals(0, CommonUtil.compare(new String[] { "a", "b", "c" }, new String[] { "a", "b", "c" }));
        assertEquals(-1, CommonUtil.compare(new String[] { null }, new String[] { "a" }));
        assertEquals(1, CommonUtil.compare(new String[] { "a" }, new String[] { null }));
        assertEquals(0, CommonUtil.compare(new String[] { null }, new String[] { null }));
        assertEquals(-1, CommonUtil.compare(new String[] {}, new String[] { "a" }));
    }

    @Test
    public void testCompare_arrayRange() {
        assertEquals(0, CommonUtil.compare(new boolean[] { true, false, true, false }, 1, new boolean[] { false, false, true, true }, 1, 2));
        assertEquals(0, CommonUtil.compare(new boolean[] { true, false }, 0, new boolean[] { false, true }, 0, 0));
        assertEquals(0, CommonUtil.compare(new boolean[] { true, false, true, false }, 1, new boolean[] { true, false, true, false }, 1, 2));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.compare(new boolean[] { true }, 0, new boolean[] { true }, 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.compare(new boolean[] { true }, 0, new boolean[] { true }, 0, 10));
        boolean[] large1 = new boolean[2000];
        boolean[] large2 = new boolean[2000];
        Arrays.fill(large1, true);
        Arrays.fill(large2, true);
        large2[1500] = false;
        assertEquals(1, CommonUtil.compare(large1, 0, large2, 0, 1600));

        assertEquals(0, CommonUtil.compare(new char[] { 'a', 'b', 'c', 'd' }, 1, new char[] { 'x', 'b', 'c', 'y' }, 1, 2));
        assertTrue(CommonUtil.compare(new char[] { 'a', 'b' }, 1, new char[] { 'x', 'a' }, 1, 1) > 0);
        assertEquals(0, CommonUtil.compare(new byte[] { 1, 2, 3, 4 }, 0, new byte[] { 5, 1, 2, 3 }, 1, 3));
        assertEquals(0, CommonUtil.compare(new short[] { 1, 2, 3, 4 }, 1, new short[] { 5, 2, 3, 6 }, 1, 2));
        assertEquals(0, CommonUtil.compare(new int[] { 1, 2, 3, 4 }, 1, new int[] { 5, 2, 3, 6 }, 1, 2));
        assertEquals(0, CommonUtil.compare(new long[] { 1L, 2L, 3L, 4L }, 1, new long[] { 5L, 2L, 3L, 6L }, 1, 2));
        assertEquals(0, CommonUtil.compare(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, 1, new float[] { 5.0f, 2.0f, 3.0f, 6.0f }, 1, 2));
        assertEquals(0, CommonUtil.compare(new double[] { 1.0, 2.0, 3.0, 4.0 }, 1, new double[] { 5.0, 2.0, 3.0, 6.0 }, 1, 2));
        assertEquals(0, CommonUtil.compare(new String[] { "a", "b", "c", "d" }, 1, new String[] { "x", "b", "c", "y" }, 1, 2));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.compare(new String[] { "a" }, 0, new String[] { "a" }, 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.compare(new String[] { "a" }, 0, new String[] { "a" }, 0, 10));
    }

    @Test
    public void testCompare_withComparator() {
        Comparator<String> cmp = String.CASE_INSENSITIVE_ORDER;
        assertEquals(0, CommonUtil.compare("test", "TEST", cmp));
        assertTrue(CommonUtil.compare("abc", "xyz", cmp) < 0);
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.compare("abc", "abc", null));
        assertEquals(0, CommonUtil.compare(new String[] { "a", "b", "c" }, new String[] { "A", "B", "C" }, cmp));
        assertEquals(0, CommonUtil.compare(new String[] { "x", "b", "c", "y" }, 1, new String[] { "z", "B", "C", "w" }, 1, 2, cmp));
        Comparator<String> reverse = (a, b) -> b.compareTo(a);
        assertTrue(CommonUtil.compare(new String[] { "a", "b" }, new String[] { "a", "c" }, reverse) > 0);
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.compare(new String[] { "a" }, new String[] { "b" }, (Comparator<String>) null));
        assertEquals(0, CommonUtil.compare(Arrays.asList("a", "b", "c"), Arrays.asList("A", "B", "C"), cmp));
        assertEquals(0, CommonUtil.compare(Arrays.asList("a", "b").iterator(), Arrays.asList("A", "B").iterator(), cmp));
        assertTrue(CommonUtil.compare(Arrays.asList("a", "b", "c"), Arrays.asList("a", "b", "d"), reverse) > 0);
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.compare(Arrays.asList("a"), Arrays.asList("b"), (Comparator<String>) null));
        assertThrows(IllegalArgumentException.class,
                () -> CommonUtil.compare(Arrays.asList("a").iterator(), Arrays.asList("b").iterator(), (Comparator<String>) null));
        assertEquals(0, CommonUtil.compare(new String[] { "a", "b", "c", "d" }, 1, new String[] { "x", "b", "c", "y" }, 1, 2, reverse));
        assertTrue(CommonUtil.compare(new String[] { "a" }, 0, new String[] { "x" }, 0, 1, reverse) > 0);
    }

    @Test
    public void testCompare_iterablesAndCollections() {
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("a", "b", "c");
        Set<String> set1 = new LinkedHashSet<>(list1);
        assertEquals(0, CommonUtil.compare(list1, list2));
        assertEquals(0, CommonUtil.compare(list1, set1));
        assertTrue(CommonUtil.compare(list1, Arrays.asList("a", "b", "d")) < 0);
        assertEquals(0, CommonUtil.compare(Collections.<String> emptyList(), Collections.<String> emptyList()));
        assertEquals(-1, CommonUtil.compare(Collections.emptyList(), list1));
        assertEquals(1, CommonUtil.compare(list1, Collections.emptyList()));
        assertEquals(0, CommonUtil.compare(list1.iterator(), list2.iterator()));
        assertTrue(CommonUtil.compare(list1.iterator(), Arrays.asList("a", "b").iterator()) > 0);
        assertEquals(0, CommonUtil.compare((Iterator<String>) null, (Iterator<String>) null));
        assertEquals(-1, CommonUtil.compare((Iterator<String>) null, list1.iterator()));
        assertEquals(1, CommonUtil.compare(list1.iterator(), (Iterator<String>) null));
        List<String> a = Arrays.asList("a", "b", "c", "d");
        List<String> b = Arrays.asList("x", "b", "c", "y");
        assertEquals(0, CommonUtil.compare(a, 1, b, 1, 2));
        assertTrue(CommonUtil.compare(a, 0, b, 0, 1) < 0);
        assertEquals(0, CommonUtil.compare(a, 1, a, 1, 2));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.compare(a, 0, b, 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.compare(a, 0, b, 0, 10));
        Comparator<String> reverse = (x, y) -> y.compareTo(x);
        assertEquals(0, CommonUtil.compare(a, 1, b, 1, 2, reverse));
        assertEquals(0, CommonUtil.compare(new LinkedHashSet<>(a), 1, new LinkedHashSet<>(b), 1, 2, reverse));
    }

    @Test
    public void testCompare_multiplePairs() {
        assertEquals(0, CommonUtil.compare("a", "a", 1, 1));
        assertEquals(-1, CommonUtil.compare("a", "b", 1, 1));
        assertEquals(1, CommonUtil.compare("b", "a", 1, 1));
        assertEquals(-1, CommonUtil.compare("a", "a", 1, 2));
        assertEquals(0, CommonUtil.compare("a", "a", 1, 1, 5L, 5L));
        assertEquals(-1, CommonUtil.compare("a", "a", 1, 1, 5L, 6L));
        @SuppressWarnings("deprecation")
        int four = CommonUtil.compare("a", "a", 1, 1, 5L, 5L, 1.0, 1.0);
        assertEquals(0, four);
        @SuppressWarnings("deprecation")
        int five = CommonUtil.compare("a", "a", 1, 1, 5L, 5L, 1.0, 1.0, "y", "x");
        assertEquals(1, five);
        @SuppressWarnings("deprecation")
        int six = CommonUtil.compare("a", "a", 1, 1, 5L, 5L, 1.0, 1.0, "x", "x", false, true);
        assertEquals(-1, six);
        @SuppressWarnings("deprecation")
        int seven = CommonUtil.compare("a", "a", 1, 1, 5L, 5L, 1.0, 1.0, "x", "x", true, true, 'z', 'y');
        assertEquals(1, seven);
    }

    @Test
    public void testCompareUnsigned() {
        assertEquals(0, CommonUtil.compareUnsigned((byte) 5, (byte) 5));
        assertTrue(CommonUtil.compareUnsigned((byte) -1, (byte) 1) > 0);
        assertTrue(CommonUtil.compareUnsigned((byte) -1, (byte) 127) > 0);
        assertEquals(0, CommonUtil.compareUnsigned((short) 5, (short) 5));
        assertTrue(CommonUtil.compareUnsigned((short) -1, (short) 1) > 0);
        assertEquals(0, CommonUtil.compareUnsigned(5, 5));
        assertTrue(CommonUtil.compareUnsigned(-1, 1) > 0);
        assertTrue(CommonUtil.compareUnsigned(-1, Integer.MAX_VALUE) > 0);
        assertEquals(0, CommonUtil.compareUnsigned(5L, 5L));
        assertEquals(1, CommonUtil.compareUnsigned(-1L, Long.MAX_VALUE));
        assertEquals(0, CommonUtil.compareUnsigned(new byte[] { 1, 2, 3 }, new byte[] { 1, 2, 3 }));
        assertTrue(CommonUtil.compareUnsigned(new byte[] { (byte) 255 }, new byte[] { 1 }) > 0);
        assertEquals(-1, CommonUtil.compareUnsigned(new byte[] {}, new byte[] { 1 }));
        assertEquals(0, CommonUtil.compareUnsigned(new short[] { 1, 2, 3 }, new short[] { 1, 2, 3 }));
        assertTrue(CommonUtil.compareUnsigned(new short[] { (short) 65535 }, new short[] { 1 }) > 0);
        assertEquals(0, CommonUtil.compareUnsigned(new int[] { 1, 2, 3 }, new int[] { 1, 2, 3 }));
        assertTrue(CommonUtil.compareUnsigned(new int[] { -1 }, new int[] { 1 }) > 0);
        assertEquals(0, CommonUtil.compareUnsigned(new long[] { 1L, 2L, 3L }, new long[] { 1L, 2L, 3L }));
        assertTrue(CommonUtil.compareUnsigned(new long[] { -1L }, new long[] { 1L }) > 0);
        assertEquals(0, CommonUtil.compareUnsigned(new byte[] { 1, 2, 3, 4 }, 1, new byte[] { 0, 2, 3, 0 }, 1, 2));
        assertTrue(CommonUtil.compareUnsigned(new byte[] { 1, (byte) 255, 3 }, 1, new byte[] { 5, 1, 3 }, 1, 1) > 0);
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.compareUnsigned(new byte[] { 1 }, 0, new byte[] { 1 }, 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.compareUnsigned(new byte[] { 1 }, 0, new byte[] { 1 }, 0, 10));
        assertTrue(CommonUtil.compareUnsigned(new short[] { 1, (short) 65535, 3 }, 1, new short[] { 5, 1, 3 }, 1, 1) > 0);
        assertTrue(CommonUtil.compareUnsigned(new int[] { 1, -1, 3 }, 1, new int[] { 5, 1, 3 }, 1, 1) > 0);
        assertTrue(CommonUtil.compareUnsigned(new long[] { 1L, -1L, 3L }, 1, new long[] { 5L, 1L, 3L }, 1, 1) > 0);
        assertEquals(0, CommonUtil.compareUnsigned(new byte[0], 0, new byte[] { 1, 2, 3 }, 0, 0));
        assertEquals(0, CommonUtil.compareUnsigned(new byte[] { 1, 2, 3 }, 0, new byte[0], 0, 0));
        assertEquals(0, CommonUtil.compareUnsigned(new short[0], 0, new short[] { 1 }, 0, 0));
        assertEquals(0, CommonUtil.compareUnsigned(new int[0], 0, new int[] { 1 }, 0, 0));
        assertEquals(0, CommonUtil.compareUnsigned(new long[0], 0, new long[] { 1L }, 0, 0));
        byte[] same = { 1, 2, 3 };
        assertEquals(0, CommonUtil.compareUnsigned(same, 0, same, 0, 3));
    }

    @Test
    public void testCompareIgnoreCase() {
        assertEquals(0, CommonUtil.compareIgnoreCase("test", "TEST"));
        assertTrue(CommonUtil.compareIgnoreCase("abc", "XYZ") < 0);
        assertEquals(0, CommonUtil.compareIgnoreCase((String) null, (String) null));
        assertEquals(0, CommonUtil.compareIgnoreCase(new String[] { "Hello", "World" }, new String[] { "HELLO", "WORLD" }));
        assertTrue(CommonUtil.compareIgnoreCase(new String[] { "Hello", "World" }, new String[] { "HELLO", "EARTH" }) > 0);
        assertEquals(0, CommonUtil.compareIgnoreCase(new String[] {}, new String[] {}));
        assertEquals(-1, CommonUtil.compareIgnoreCase(new String[] {}, new String[] { "a" }));
        assertEquals(0, CommonUtil.compareIgnoreCase(new String[] { "a", "b", "c" }, new String[] { "A", "B", "C" }));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testCompareByProps() {
        TestBean left = new TestBean();
        left.setName("alpha");
        left.setValue("1");
        TestBean right = new TestBean();
        right.setName("alpha");
        right.setValue("2");
        assertTrue(CommonUtil.compareByProps(left, right, Arrays.asList("name", "value")) < 0);
        assertEquals(0, CommonUtil.compareByProps(left, left, Arrays.asList("name", "value")));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.compareByProps(null, right, Arrays.asList("name")));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.compareByProps(left, null, Arrays.asList("name")));
    }
    @Test
    public void testCompare_floatAndDoubleFollowWrapperCompareSemantics() {
        assertEquals(1, CommonUtil.compare(0.0f, -0.0f));
        assertEquals(-1, CommonUtil.compare(-0.0f, 0.0f));
        assertEquals(0, CommonUtil.compare(Float.NaN, Float.NaN));
        assertEquals(1, CommonUtil.compare(Float.NaN, Float.POSITIVE_INFINITY));

        assertEquals(1, CommonUtil.compare(0.0d, -0.0d));
        assertEquals(-1, CommonUtil.compare(-0.0d, 0.0d));
        assertEquals(0, CommonUtil.compare(Double.NaN, Double.NaN));
        assertEquals(1, CommonUtil.compare(Double.NaN, Double.MAX_VALUE));
    }

    @Test
    public void testCompareUnsigned_scalarResultsFollowUnsignedOrder() {
        assertEquals(254, CommonUtil.compareUnsigned((byte) -1, (byte) 1));
        assertEquals(-128, CommonUtil.compareUnsigned((byte) 0, (byte) -128));
        assertEquals(65534, CommonUtil.compareUnsigned((short) -1, (short) 1));
        assertTrue(CommonUtil.compareUnsigned(-1, 1) > 0);
        assertTrue(CommonUtil.compareUnsigned(Integer.MIN_VALUE, Integer.MAX_VALUE) > 0);
        assertTrue(CommonUtil.compareUnsigned(-1L, 1L) > 0);

        assertTrue(CommonUtil.compare((byte) -1, (byte) 1) < 0);
        assertTrue(CommonUtil.compare((byte) 0, (byte) -128) > 0);
        assertTrue(CommonUtil.compare(Integer.MIN_VALUE, Integer.MAX_VALUE) < 0);
    }

    @Test
    public void testCompareIgnoreCase_nullOrdersFirst() {
        assertEquals(-1, CommonUtil.compareIgnoreCase((String) null, "a"));
        assertEquals(1, CommonUtil.compareIgnoreCase("a", (String) null));
        assertEquals(0, CommonUtil.compareIgnoreCase((String) null, (String) null));
        assertEquals(0, CommonUtil.compareIgnoreCase("HELLO", "hello"));
    }

}
