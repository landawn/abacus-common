package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;

import org.junit.jupiter.api.Test;

public class CommonUtilToTest extends CommonUtilTestSupport {

    @Test
    public void testToString_primitives() {
        assertEquals("true", CommonUtil.toString(true));
        assertEquals("false", CommonUtil.toString(false));
        assertEquals("a", CommonUtil.toString('a'));
        assertEquals("5", CommonUtil.toString((byte) 5));
        assertEquals("-1", CommonUtil.toString((byte) -1));
        assertEquals("100", CommonUtil.toString((short) 100));
        assertEquals("42", CommonUtil.toString(42));
        assertEquals("-42", CommonUtil.toString(-42));
        assertEquals("100", CommonUtil.toString(100L));
        assertEquals("10.5", CommonUtil.toString(10.5f));
        assertEquals("10.5", CommonUtil.toString(10.5));
    }

    @Test
    public void testToString_arrays() {
        assertEquals("[true, false, true]", CommonUtil.toString(new boolean[] { true, false, true }));
        assertEquals("null", CommonUtil.toString((boolean[]) null));
        assertEquals("[]", CommonUtil.toString(new boolean[0]));
        assertEquals("[a, b, c]", CommonUtil.toString(new char[] { 'a', 'b', 'c' }));
        assertEquals("null", CommonUtil.toString((char[]) null));
        assertEquals("[]", CommonUtil.toString(new char[0]));
        assertEquals("[1, 2, 3]", CommonUtil.toString(new byte[] { 1, 2, 3 }));
        assertEquals("null", CommonUtil.toString((byte[]) null));
        assertEquals("[1, 2, 3]", CommonUtil.toString(new short[] { 1, 2, 3 }));
        assertEquals("null", CommonUtil.toString((short[]) null));
        assertEquals("[1, 2, 3]", CommonUtil.toString(new int[] { 1, 2, 3 }));
        assertEquals("null", CommonUtil.toString((int[]) null));
        assertEquals("[1, 2, 3]", CommonUtil.toString(new long[] { 1L, 2L, 3L }));
        assertEquals("null", CommonUtil.toString((long[]) null));
        assertEquals("[1.0, 2.0, 3.0]", CommonUtil.toString(new float[] { 1.0f, 2.0f, 3.0f }));
        assertEquals("null", CommonUtil.toString((float[]) null));
        assertEquals("[1.0, 2.0, 3.0]", CommonUtil.toString(new double[] { 1.0, 2.0, 3.0 }));
        assertEquals("null", CommonUtil.toString((double[]) null));
        assertEquals("[a, b, c]", CommonUtil.toString(new String[] { "a", "b", "c" }));
        assertEquals("null", CommonUtil.toString((Object[]) null));
        assertEquals("[]", CommonUtil.toString(new Object[0]));
    }

    @Test
    public void testToString_arrayRange() {
        assertEquals("[false, true]", CommonUtil.toString(new boolean[] { true, false, true, false }, 1, 3));
        assertEquals("null", CommonUtil.toString((boolean[]) null, 0, 0));
        assertEquals("[]", CommonUtil.toString(new boolean[0], 0, 0));
        assertEquals("[]", CommonUtil.toString(new boolean[] { true, false }, 1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.toString(new boolean[] { true }, 0, 6));
        assertEquals("[b, c]", CommonUtil.toString(new char[] { 'a', 'b', 'c', 'd' }, 1, 3));
        assertEquals("null", CommonUtil.toString((char[]) null, 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.toString(new char[] { 'a' }, 0, 6));
        assertEquals("[2, 3]", CommonUtil.toString(new byte[] { 1, 2, 3, 4, 5 }, 1, 3));
        assertEquals("null", CommonUtil.toString((byte[]) null, 0, 0));
        assertEquals("[]", CommonUtil.toString(new byte[0], 0, 0));
        assertEquals("[2, 3]", CommonUtil.toString(new short[] { 1, 2, 3, 4, 5 }, 1, 3));
        assertEquals("[20, 30]", CommonUtil.toString(new int[] { 10, 20, 30, 40 }, 1, 3));
        assertEquals("[2, 3]", CommonUtil.toString(new long[] { 1L, 2L, 3L, 4L, 5L }, 1, 3));
        assertEquals("[2.0, 3.0]", CommonUtil.toString(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, 1, 3));
        assertEquals("[2.0, 3.0]", CommonUtil.toString(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, 1, 3));
        assertEquals("[2, three, null]", CommonUtil.toString(new Object[] { "one", 2, "three", null, 5.0 }, 1, 4));
        assertEquals("[]", CommonUtil.toString(new Object[] { "a", "b" }, 1, 1));
    }

    @Test
    public void testToString_objects() {
        assertEquals("test", CommonUtil.toString("test"));
        assertEquals("null", CommonUtil.toString((Object) null));
        assertEquals("default", CommonUtil.toString(null, "default"));
        assertEquals("test", CommonUtil.toString("test", "default"));
        assertEquals("[a, b, c]", CommonUtil.toString(Arrays.asList("a", "b", "c")));
        Iterator<String> iter = Arrays.asList("a", "b", "c").iterator();
        assertNotEquals("[a, b, c]", CommonUtil.toString(iter));
        Object obj = new Object();
        assertEquals(obj.toString(), CommonUtil.toString(obj));
    }

    @Test
    public void testToString_stringBuilder() {
        StringBuilder sb = new StringBuilder();
        CommonUtil.toString(sb, (boolean[]) null);
        assertEquals("null", sb.toString());
        sb.setLength(0);
        CommonUtil.toString(sb, new boolean[0]);
        assertEquals("[]", sb.toString());
        sb.setLength(0);
        CommonUtil.toString(sb, new boolean[] { true, false });
        assertTrue(sb.toString().contains("true"));
        sb.setLength(0);
        CommonUtil.toString(sb, (char[]) null);
        assertEquals("null", sb.toString());
        sb.setLength(0);
        CommonUtil.toString(sb, (byte[]) null);
        assertEquals("null", sb.toString());
        sb.setLength(0);
        CommonUtil.toString(sb, new byte[0]);
        assertEquals("[]", sb.toString());
        sb.setLength(0);
        CommonUtil.toString(sb, new byte[] { 1, 2 });
        assertTrue(sb.toString().contains("1"));
        sb.setLength(0);
        CommonUtil.toString(sb, (short[]) null);
        assertEquals("null", sb.toString());
        sb.setLength(0);
        CommonUtil.toString(sb, new short[0]);
        assertEquals("[]", sb.toString());
        sb.setLength(0);
        CommonUtil.toString(sb, new short[] { 1, 2, 3 });
        assertEquals("[1, 2, 3]", sb.toString());
        sb.setLength(0);
        CommonUtil.toString(sb, (int[]) null);
        assertEquals("null", sb.toString());
        sb.setLength(0);
        CommonUtil.toString(sb, new int[0]);
        assertEquals("[]", sb.toString());
        sb.setLength(0);
        CommonUtil.toString(sb, new int[] { 10, 20 });
        assertEquals("[10, 20]", sb.toString());
        sb.setLength(0);
        CommonUtil.toString(sb, (long[]) null);
        assertEquals("null", sb.toString());
        sb.setLength(0);
        CommonUtil.toString(sb, new long[0]);
        assertEquals("[]", sb.toString());
        sb.setLength(0);
        CommonUtil.toString(sb, new long[] { 100L, 200L });
        assertEquals("[100, 200]", sb.toString());
        sb.setLength(0);
        CommonUtil.toString(sb, (float[]) null);
        assertEquals("null", sb.toString());
        sb.setLength(0);
        CommonUtil.toString(sb, new float[0]);
        assertEquals("[]", sb.toString());
        sb.setLength(0);
        CommonUtil.toString(sb, new float[] { 1.0f, 2.0f });
        assertEquals("[1.0, 2.0]", sb.toString());
        sb.setLength(0);
        CommonUtil.toString(sb, (double[]) null);
        assertEquals("null", sb.toString());
        sb.setLength(0);
        CommonUtil.toString(sb, new double[0]);
        assertEquals("[]", sb.toString());
        sb.setLength(0);
        CommonUtil.toString(sb, new double[] { 1.5d, 2.5d });
        assertEquals("[1.5, 2.5]", sb.toString());
    }

    @Test
    public void testToArray() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        Object[] arr = CommonUtil.toArray(list);
        assertArrayEquals(new Object[] { "a", "b", "c", "d", "e" }, arr);
        assertArrayEquals(new Object[] { "b", "c", "d" }, CommonUtil.toArray(list, 1, 4));
        assertArrayEquals(new String[] { "b", "c", "d" }, CommonUtil.toArray(list, 1, 4, String[]::new));
        assertArrayEquals(new String[] { "a", "b", "c" }, CommonUtil.toArray(Arrays.asList("a", "b", "c"), String[]::new));
        assertArrayEquals(new String[] { "a", "b", "c" }, CommonUtil.toArray(Arrays.asList("a", "b", "c"), String[].class));
        assertArrayEquals(new Object[] { "b", "c", "d" }, CommonUtil.toArray(list, 1, 4, String[].class));
        LinkedList<String> linked = new LinkedList<>(list);
        assertArrayEquals(new String[] { "b", "c", "d" }, CommonUtil.toArray(linked, 1, 4, String[]::new));
        assertArrayEquals(new Object[] { "b", "c", "d" }, CommonUtil.toArray(linked, 1, 4));
        Collection<String> set = CommonUtil.toLinkedHashSet("a", "b", "c", "d", "e");
        assertArrayEquals(new Object[] { "b", "c", "d" }, CommonUtil.toArray(set, 1, 4));
        assertArrayEquals(new Object[] { "b", "c", "d" }, CommonUtil.toArray(set, 1, 4, new String[1]));
        assertArrayEquals(new Object[] { "b", "c", "d" }, CommonUtil.toArray(set, 1, 4, String[]::new));
        assertArrayEquals(new Object[] { "b", "c", "d" }, CommonUtil.toArray(set, 1, 4, String[].class));
        String[] target = new String[5];
        String[] result = CommonUtil.toArray(Arrays.asList("a", "b", "c"), target);
        assertSame(target, result);
        assertEquals("a", result[0]);
        assertNull(result[3]);
        String[] small = new String[2];
        String[] grown = CommonUtil.toArray(Arrays.asList("a", "b", "c"), small);
        assertNotSame(small, grown);
        assertEquals(3, grown.length);
        String[] rangeTarget = new String[3];
        assertSame(rangeTarget, CommonUtil.toArray(list, 1, 4, rangeTarget));
        assertArrayEquals(new String[] { "b", "c", "d" }, rangeTarget);
    }

    @Test
    public void testToArray_EdgeCase() {
        assertEquals(0, CommonUtil.toArray(new ArrayList<>()).length);
        assertSame(CommonUtil.toArray((Collection<?>) null), CommonUtil.toArray((Collection<?>) null));
        assertEquals(0, CommonUtil.toArray(new ArrayList<String>(), String[]::new).length);
        assertEquals(0, CommonUtil.toArray(Arrays.asList("a", "b", "c"), 1, 1).length);
        assertEquals(0, CommonUtil.toArray(Arrays.asList("a", "b", "c"), 1, 1, String[]::new).length);
        String[] a = { "x", "y" };
        String[] emptyInto = CommonUtil.toArray(new ArrayList<String>(), a);
        assertSame(a, emptyInto);
        assertNull(emptyInto[0]);
        String[] rangeEmpty = CommonUtil.toArray(new ArrayList<String>(), 0, 0, a);
        assertSame(a, rangeEmpty);
        assertNull(rangeEmpty[0]);
        LinkedHashSet<String> set = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d", "e"));
        String[] oversized = new String[5];
        String[] sliced = CommonUtil.toArray(set, 1, 4, oversized);
        assertEquals("b", sliced[0]);
        assertEquals("d", sliced[2]);
        assertNull(oversized[3]);
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.toArray(list, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.toArray(list, 0, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.toArray(list, 3, 2));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.toArray(list, (String[]) null));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.toArray(list, 1, 4, (String[]) null));
    }

    @Test
    public void testToBooleanArray() {
        assertArrayEquals(new boolean[] { true, false, true }, CommonUtil.toBooleanArray(Arrays.asList(true, false, true)));
        assertArrayEquals(new boolean[] { false, true, false }, CommonUtil.toBooleanArray(Arrays.asList(true, false, true, null, false), 1, 4));
        assertArrayEquals(new boolean[] { false, true, true }, CommonUtil.toBooleanArray(Arrays.asList(true, false, true, null, false), 1, 4, true));
        assertEquals(0, CommonUtil.toBooleanArray(Arrays.asList(true, false), 1, 1).length);
        assertArrayEquals(new boolean[] { false, false, true }, CommonUtil.toBooleanArray(Arrays.asList(true, null, false, true, null), 1, 4, false));
        assertArrayEquals(new boolean[] { false, true, false, true, false }, CommonUtil.toBooleanArray(new byte[] { 0, 1, -1, 127, -128 }));
        assertArrayEquals(new boolean[0], CommonUtil.toBooleanArray(new byte[0]));
        assertArrayEquals(new boolean[0], CommonUtil.toBooleanArray((byte[]) null));
        assertArrayEquals(new boolean[] { false, true, false, true, false }, CommonUtil.toBooleanArray(new int[] { 0, 1, -1, 100, -100 }));
        assertArrayEquals(new boolean[0], CommonUtil.toBooleanArray((int[]) null));
        Collection<Boolean> c = CommonUtil.toLinkedList(true, true, false, null, true);
        assertArrayEquals(new boolean[] { true, false, false }, CommonUtil.toBooleanArray(c, 1, 4, false));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.toBooleanArray(Arrays.asList(true), -1, 2));
    }

    @Test
    public void testToCharArray() {
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, CommonUtil.toCharArray(Arrays.asList('a', 'b', 'c')));
        assertArrayEquals(new char[] { 'b', 'c', '\0' }, CommonUtil.toCharArray(Arrays.asList('a', 'b', 'c', null, 'd'), 1, 4));
        assertArrayEquals(new char[] { 'b', 'c', 'X' }, CommonUtil.toCharArray(Arrays.asList('a', 'b', 'c', null, 'd'), 1, 4, 'X'));
        LinkedList<Character> linked = new LinkedList<>(Arrays.asList('a', 'b', null, 'd', 'e'));
        assertArrayEquals(new char[] { 'b', 'X', 'd' }, CommonUtil.toCharArray(linked, 1, 4, 'X'));
        assertEquals(0, CommonUtil.toCharArray(linked, 2, 2, 'X').length);
    }

    @Test
    public void testToByteArray() {
        assertArrayEquals(new byte[] { 1, 2, 3 }, CommonUtil.toByteArray(Arrays.asList((byte) 1, (byte) 2, (byte) 3)));
        assertArrayEquals(new byte[] { 2, 3, 0 }, CommonUtil.toByteArray(Arrays.asList((byte) 1, 2, 3L, null, 5.0), 1, 4));
        assertArrayEquals(new byte[] { 2, 3, -1 }, CommonUtil.toByteArray(Arrays.asList((byte) 1, 2, 3L, null, 5.0), 1, 4, (byte) -1));
        assertArrayEquals(new byte[] { 99, 3 }, CommonUtil.toByteArray(Arrays.asList((byte) 1, null, (byte) 3, (byte) 4), 1, 3, (byte) 99));
        assertArrayEquals(new byte[] { 1, 0, 1, 0 }, CommonUtil.toByteArray(new boolean[] { true, false, true, false }));
        assertArrayEquals(new byte[0], CommonUtil.toByteArray((boolean[]) null));
        Collection<Byte> c = CommonUtil.toLinkedList((byte) 1, (byte) 2, (byte) 3, null, (byte) 4);
        assertArrayEquals(new byte[] { 2, 3, 0 }, CommonUtil.toByteArray(c, 1, 4, (byte) 0));
    }

    @Test
    public void testToShortArray() {
        assertArrayEquals(new short[] { 1, 2, 3 }, CommonUtil.toShortArray(Arrays.asList((short) 1, (short) 2, (short) 3)));
        assertArrayEquals(new short[] { 2, 3, 0 }, CommonUtil.toShortArray(Arrays.asList((short) 1, 2, 3L, null, 5.0), 1, 4));
        assertArrayEquals(new short[] { 99, 3 }, CommonUtil.toShortArray(Arrays.asList((short) 1, null, (short) 3, (short) 4), 1, 3, (short) 99));
        Collection<Short> c = CommonUtil.toLinkedList((short) 1, (short) 2, (short) 3, null, (short) 4);
        assertArrayEquals(new short[] { 2, 3, 0 }, CommonUtil.toShortArray(c, 1, 4, (short) 0));
    }

    @Test
    public void testToIntArray() {
        assertArrayEquals(new int[] { 1, 2, 3 }, CommonUtil.toIntArray(Arrays.asList(1, 2, 3)));
        assertArrayEquals(new int[] { 2, 3, 0 }, CommonUtil.toIntArray(Arrays.asList(1, 2L, 3.0, null, (byte) 5), 1, 4));
        assertArrayEquals(new int[] { 99, 3 }, CommonUtil.toIntArray(Arrays.asList(1, null, 3, 4), 1, 3, 99));
        assertArrayEquals(new int[] { 65, 66, 67 }, CommonUtil.toIntArray(new char[] { 'A', 'B', 'C' }));
        assertArrayEquals(new int[0], CommonUtil.toIntArray((char[]) null));
        assertArrayEquals(new int[] { 1, 0, 1, 0 }, CommonUtil.toIntArray(new boolean[] { true, false, true, false }));
        Collection<Integer> c = CommonUtil.toLinkedList(1, 2, 3, null, 4);
        assertArrayEquals(new int[] { 2, 3, 0 }, CommonUtil.toIntArray(c, 1, 4, 0));
    }

    @Test
    public void testToLongArray() {
        assertArrayEquals(new long[] { 1L, 2L, 3L }, CommonUtil.toLongArray(Arrays.asList(1L, 2L, 3L)));
        assertArrayEquals(new long[] { 2, 3, 0 }, CommonUtil.toLongArray(Arrays.asList(1, 2L, 3.0, null, (byte) 5), 1, 4));
        assertArrayEquals(new long[] { 99L, 3L }, CommonUtil.toLongArray(Arrays.asList(1L, null, 3L, 4L), 1, 3, 99L));
        Collection<Long> c = CommonUtil.toLinkedList(1L, 2L, 3L, null, 4L);
        assertArrayEquals(new long[] { 2L, 3L, 0L }, CommonUtil.toLongArray(c, 1, 4, 0L));
    }

    @Test
    public void testToFloatArray() {
        float[] arr = CommonUtil.toFloatArray(Arrays.asList(1.1f, 2.2f, 3.3f));
        assertEquals(1.1f, arr[0], 0.01);
        assertArrayEquals(new float[] { 2, 3, 0 }, CommonUtil.toFloatArray(Arrays.asList(1, 2L, 3.0, null, (byte) 5), 1, 4), 0.0f);
        assertArrayEquals(new float[] { 99.0f, 3.0f }, CommonUtil.toFloatArray(Arrays.asList(1.0f, null, 3.0f, 4.0f), 1, 3, 99.0f), 0.0f);
        Collection<Float> c = CommonUtil.toLinkedList(1.0f, 2.0f, 3.0f, null, 4.0f);
        assertArrayEquals(new float[] { 2.0f, 3.0f, 0.0f }, CommonUtil.toFloatArray(c, 1, 4, 0.0f), 0.0f);
    }

    @Test
    public void testToDoubleArray() {
        double[] arr = CommonUtil.toDoubleArray(Arrays.asList(1.1, 2.2, 3.3));
        assertEquals(1.1, arr[0], 0.01);
        assertArrayEquals(new double[] { 2, 3, 0 }, CommonUtil.toDoubleArray(Arrays.asList(1, 2L, 3.0, null, (byte) 5), 1, 4), 0.0);
        assertArrayEquals(new double[] { 99.0, 3.0 }, CommonUtil.toDoubleArray(Arrays.asList(1.0, null, 3.0, 4.0), 1, 3, 99.0), 0.0);
        Collection<Double> c = CommonUtil.toLinkedList(1.0d, 2.0d, 3.0d, null, 4.0d);
        assertArrayEquals(new double[] { 2.0d, 3.0d, 0.0d }, CommonUtil.toDoubleArray(c, 1, 4, 0.0d), 0.0);
    }

    @Test
    public void testToList() {
        assertEquals(Arrays.asList("a", "b", "c"), CommonUtil.toList(new String[] { "a", "b", "c" }));
        assertEquals(new ArrayList<>(), CommonUtil.toList(new String[0]));
        assertEquals(new ArrayList<>(), CommonUtil.toList((String[]) null));
        assertEquals(Arrays.asList(1, 2, 3), CommonUtil.toList(new int[] { 1, 2, 3 }));
        assertEquals(Arrays.asList(true, false, true), CommonUtil.toList(new boolean[] { true, false, true }));
        assertEquals(Arrays.asList('a', 'b', 'c'), CommonUtil.toList(new char[] { 'a', 'b', 'c' }));
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), CommonUtil.toList(new byte[] { 1, 2, 3 }));
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3), CommonUtil.toList(new short[] { 1, 2, 3 }));
        assertEquals(Arrays.asList(1L, 2L, 3L), CommonUtil.toList(new long[] { 1L, 2L, 3L }));
        assertEquals(Arrays.asList(1.0f, 2.0f, 3.0f), CommonUtil.toList(new float[] { 1.0f, 2.0f, 3.0f }));
        assertEquals(Arrays.asList(1.0, 2.0, 3.0), CommonUtil.toList(new double[] { 1.0, 2.0, 3.0 }));
        assertEquals(new ArrayList<>(), CommonUtil.toList((double[]) null));
        assertEquals(Arrays.asList(false, true, false), CommonUtil.toList(new boolean[] { true, false, true, false, true }, 1, 4));
        assertEquals(Arrays.asList('b', 'c', 'd'), CommonUtil.toList(new char[] { 'a', 'b', 'c', 'd', 'e' }, 1, 4));
        assertEquals(Arrays.asList((byte) 2, (byte) 3, (byte) 4), CommonUtil.toList(new byte[] { 1, 2, 3, 4, 5 }, 1, 4));
        assertEquals(Arrays.asList((short) 2, (short) 3, (short) 4), CommonUtil.toList(new short[] { 1, 2, 3, 4, 5 }, 1, 4));
        assertEquals(Arrays.asList(2, 3, 4), CommonUtil.toList(new int[] { 1, 2, 3, 4, 5 }, 1, 4));
        assertEquals(Arrays.asList(2L, 3L, 4L), CommonUtil.toList(new long[] { 1L, 2L, 3L, 4L, 5L }, 1, 4));
        assertEquals(Arrays.asList(2.0f, 3.0f, 4.0f), CommonUtil.toList(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, 1, 4));
        assertEquals(Arrays.asList(2.0, 3.0, 4.0), CommonUtil.toList(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, 1, 4));
        assertEquals(Arrays.asList("b", "c", "d"), CommonUtil.toList(new String[] { "a", "b", "c", "d", "e" }, 1, 4));
        assertEquals(Arrays.asList("a", "b", "c"), CommonUtil.toList(Arrays.asList("a", "b", "c").iterator()));
        assertEquals(new ArrayList<>(), CommonUtil.toList((Iterator<String>) null));
        assertEquals(new ArrayList<>(), CommonUtil.toList(new boolean[] { true, false }, 1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.toList(new boolean[] { true }, -1, 2));
        LinkedList<String> linked = CommonUtil.toLinkedList("a", "b", "c");
        assertEquals("a", linked.getFirst());
        assertEquals("c", linked.getLast());
    }

    @Test
    public void testToSet() {
        assertEquals(new HashSet<>(Arrays.asList("a", "b", "c")), CommonUtil.toSet(new String[] { "a", "b", "c", "a" }));
        assertEquals(new HashSet<>(), CommonUtil.toSet(new String[0]));
        assertEquals(new HashSet<>(), CommonUtil.toSet((String[]) null));
        assertEquals(new HashSet<>(Arrays.asList(true, false)), CommonUtil.toSet(new boolean[] { true, false, true, false }));
        assertEquals(new HashSet<>(), CommonUtil.toSet((boolean[]) null));
        assertEquals(3, CommonUtil.toSet(new char[] { 'a', 'b', 'c', 'a' }).size());
        assertEquals(3, CommonUtil.toSet(new byte[] { 1, 2, 3, 1, 2 }).size());
        assertEquals(3, CommonUtil.toSet(new short[] { 1, 2, 3, 1, 2 }).size());
        assertEquals(3, CommonUtil.toSet(new int[] { 1, 2, 3, 1, 2 }).size());
        assertEquals(3, CommonUtil.toSet(new long[] { 1L, 2L, 3L, 1L, 2L }).size());
        assertEquals(3, CommonUtil.toSet(new float[] { 1.0f, 2.0f, 3.0f, 1.0f, 2.0f }).size());
        assertEquals(3, CommonUtil.toSet(new double[] { 1.0, 2.0, 3.0, 1.0, 2.0 }).size());
        assertEquals(2, CommonUtil.toSet(new boolean[] { true, false, true, false, true }, 1, 4).size());
        assertTrue(CommonUtil.toSet(new char[] { 'a', 'b', 'c', 'd', 'e' }, 1, 4).contains('b'));
        assertTrue(CommonUtil.toSet(new byte[] { 1, 2, 3, 4, 5 }, 1, 4).contains((byte) 2));
        assertTrue(CommonUtil.toSet(new short[] { 1, 2, 3, 4, 5 }, 1, 4).contains((short) 2));
        assertTrue(CommonUtil.toSet(new int[] { 1, 2, 3, 4, 5 }, 1, 4).contains(2));
        assertTrue(CommonUtil.toSet(new long[] { 1L, 2L, 3L, 4L, 5L }, 1, 4).contains(2L));
        assertTrue(CommonUtil.toSet(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, 1, 4).contains(2.0f));
        assertTrue(CommonUtil.toSet(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, 1, 4).contains(2.0));
        assertTrue(CommonUtil.toSet(new String[] { "a", "b", "c", "d", "e" }, 1, 4).contains("b"));
        assertEquals(3, CommonUtil.toSet(Arrays.asList("a", "b", "c", "a", "b").iterator()).size());
        assertEquals(new HashSet<>(), CommonUtil.toSet((Iterator<String>) null));
        Set<String> linked = CommonUtil.toLinkedHashSet("a", "b", "c", "a");
        assertEquals(3, linked.size());
        Iterator<String> it = linked.iterator();
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        SortedSet<String> sorted = CommonUtil.toSortedSet("b", "a", "c");
        assertEquals("a", sorted.first());
        NavigableSet<String> nav = CommonUtil.toNavigableSet("b", "a", "c");
        assertEquals("a", nav.first());
    }

    @Test
    public void testToCollection() {
        assertEquals(Arrays.asList(true, false), new ArrayList<>(CommonUtil.toCollection(new boolean[] { true, false }, ArrayList::new)));
        assertEquals(Arrays.asList(false, true), new ArrayList<>(CommonUtil.toCollection(new boolean[] { true, false, true, false }, 1, 3, ArrayList::new)));
        assertEquals(Arrays.asList('a', 'b'), new ArrayList<>(CommonUtil.toCollection(new char[] { 'a', 'b' }, ArrayList::new)));
        assertEquals(Arrays.asList('b', 'c'), new ArrayList<>(CommonUtil.toCollection(new char[] { 'a', 'b', 'c', 'd' }, 1, 3, ArrayList::new)));
        assertEquals(Arrays.asList((byte) 1, (byte) 2), new ArrayList<>(CommonUtil.toCollection(new byte[] { 1, 2 }, ArrayList::new)));
        assertEquals(Arrays.asList((byte) 2, (byte) 3), new ArrayList<>(CommonUtil.toCollection(new byte[] { 1, 2, 3, 4 }, 1, 3, ArrayList::new)));
        assertEquals(Arrays.asList((short) 1, (short) 2), new ArrayList<>(CommonUtil.toCollection(new short[] { 1, 2 }, ArrayList::new)));
        assertEquals(Arrays.asList((short) 2, (short) 3), new ArrayList<>(CommonUtil.toCollection(new short[] { 1, 2, 3, 4 }, 1, 3, ArrayList::new)));
        assertEquals(Arrays.asList(1, 2), new ArrayList<>(CommonUtil.toCollection(new int[] { 1, 2 }, ArrayList::new)));
        assertEquals(Arrays.asList(2, 3), new ArrayList<>(CommonUtil.toCollection(new int[] { 1, 2, 3, 4 }, 1, 3, ArrayList::new)));
        assertEquals(Arrays.asList(1L, 2L), new ArrayList<>(CommonUtil.toCollection(new long[] { 1L, 2L }, ArrayList::new)));
        assertEquals(Arrays.asList(2L, 3L), new ArrayList<>(CommonUtil.toCollection(new long[] { 1L, 2L, 3L, 4L }, 1, 3, ArrayList::new)));
        assertEquals(Arrays.asList(1.0f, 2.0f), new ArrayList<>(CommonUtil.toCollection(new float[] { 1.0f, 2.0f }, ArrayList::new)));
        assertEquals(Arrays.asList(2.0f, 3.0f), new ArrayList<>(CommonUtil.toCollection(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, 1, 3, ArrayList::new)));
        assertEquals(Arrays.asList(1.0, 2.0), new ArrayList<>(CommonUtil.toCollection(new double[] { 1.0, 2.0 }, ArrayList::new)));
        assertEquals(Arrays.asList(2.0, 3.0), new ArrayList<>(CommonUtil.toCollection(new double[] { 1.0, 2.0, 3.0, 4.0 }, 1, 3, ArrayList::new)));
        assertEquals(Arrays.asList("a", "b"), new ArrayList<>(CommonUtil.toCollection(new String[] { "a", "b" }, ArrayList::new)));
        assertEquals(Arrays.asList("b", "c"), CommonUtil.toCollection(new String[] { "a", "b", "c" }, 1, 3, n -> new ArrayList<>(n)));
        assertTrue(CommonUtil.toCollection(new String[] { "a", "b", "c" }, 1, 1, n -> new ArrayList<>(n)).isEmpty());
        assertEquals(Arrays.asList("a", "b"), CommonUtil.toCollection(Arrays.asList("a", "b"), ArrayList::new));
        assertEquals(Arrays.asList("a", "b"), CommonUtil.toCollection(Arrays.asList("a", "b").iterator(), ArrayList::new));
        Queue<String> queue = CommonUtil.toQueue("a", "b");
        assertEquals(2, queue.size());
        Deque<String> deque = CommonUtil.toDeque("a", "b");
        assertEquals(2, deque.size());
        ArrayDeque<String> arrayDeque = CommonUtil.toArrayDeque("a", "b");
        assertEquals(2, arrayDeque.size());
        Multiset<String> ms = CommonUtil.toMultiset("a", "a", "b");
        assertEquals(2, ms.getCount("a"));
    }

    @Test
    public void testToMap() {
        class Pair {
            final String key;
            final Integer value;

            Pair(String k, Integer v) {
                key = k;
                value = v;
            }
        }
        List<Pair> pairs = Arrays.asList(new Pair("a", 1), new Pair("b", 2), new Pair("c", 3));
        Map<String, Integer> map = CommonUtil.toMap(pairs, p -> p.key, p -> p.value);
        assertEquals(Integer.valueOf(1), map.get("a"));
        assertEquals("apple", CommonUtil.toMap(Arrays.asList("apple", "banana", "cherry").iterator(), String::length).get(5));
        assertTrue(CommonUtil.toMap((Iterator<String>) null, String::length).isEmpty());
        Map<String, Integer> byId = CommonUtil.toMap(Arrays.asList("apple", "banana").iterator(), java.util.function.Function.identity(), String::length);
        assertEquals(5, byId.get("apple"));
        Map<String, Integer> linked = CommonUtil.toMap(Arrays.asList("apple", "banana").iterator(), java.util.function.Function.identity(), String::length,
                LinkedHashMap::new);
        assertTrue(linked instanceof LinkedHashMap);
        Map<String, Integer> fromList = CommonUtil.toMap(Arrays.asList("apple", "banana", "cherry"), java.util.function.Function.identity(), String::length,
                n -> new LinkedHashMap<>());
        assertEquals(5, fromList.get("apple"));
        assertTrue(CommonUtil.toMap(new ArrayList<String>(), String::length).isEmpty());
        assertEquals("hi", CommonUtil.toMap(Arrays.asList("hi", "hello"), String::length).get(2));
        assertTrue(CommonUtil.toMap((Iterator<String>) null, s -> s, String::length, Integer::sum, LinkedHashMap::new).isEmpty());
        assertEquals(6, CommonUtil.toMap(Arrays.asList("ab", "cd", "ef").iterator(), String::length, String::length, Integer::sum, HashMap::new).get(2));
        assertTrue(CommonUtil.toMap(new ArrayList<String>(), s -> s, String::length, Integer::sum, n -> new LinkedHashMap<String, Integer>()).isEmpty());
        Map<String, Integer> merged = CommonUtil.toMap(Arrays.asList("aa", "bb", "aa"), s -> s, String::length, Integer::sum,
                n -> new HashMap<String, Integer>());
        assertEquals(4, merged.get("aa"));
        assertEquals(2, merged.get("bb"));
        Map<String, Integer> lhm = CommonUtil.toLinkedHashMap("a", 1, "b", 2, "c", 3);
        assertTrue(lhm instanceof LinkedHashMap);
        assertEquals(3, lhm.size());
        Map<Object, Object> five = CommonUtil.toLinkedHashMap("a", 1, "b", 2, "c", 3, "d", 4, "e", 5);
        Iterator<Object> keys = five.keySet().iterator();
        assertEquals("a", keys.next());
        assertEquals("e", CommonUtil.toLinkedHashMap("a", 1, "b", 2, "c", 3, "d", 4, "e", 5).keySet().toArray()[4]);
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.toLinkedHashMap("a", 1, "b"));
    }

    @Test
    public void testToString_nestedArrayIsRenderedByContent() {
        final Object[] nested = { new int[] { 1, 2 } };
        assertEquals("[[1, 2]]", CommonUtil.toString(nested));
        assertEquals("[[1, 2]]", CommonUtil.toString(nested, 0, 1));
        assertNotEquals(Arrays.toString(nested), CommonUtil.toString(nested));

        final Object[] self = new Object[1];
        self[0] = self;
        assertThrows(StackOverflowError.class, () -> CommonUtil.toString(self));
        assertEquals("[[...]]", CommonUtil.deepToString(self));
    }

}
