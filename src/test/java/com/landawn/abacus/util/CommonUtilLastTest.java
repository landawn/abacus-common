package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CommonUtilLastTest extends CommonUtilTestSupport {
    @Test
    public void testLastElement() {
        List<String> list = Arrays.asList("a", "b", "c");
        com.landawn.abacus.util.u.Nullable<String> result = CommonUtil.lastElement(list);
        assertTrue(result.isPresent());
        assertEquals("c", result.get());
    }

    @Test
    public void testLastElementFromIterable() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("c", CommonUtil.lastElement(list).get());

        Assertions.assertFalse(CommonUtil.lastElement(Collections.emptyList()).isPresent());

        Assertions.assertFalse(CommonUtil.lastElement((Iterable<String>) null).isPresent());

        ArrayList<String> arrayList = new ArrayList<>(list);
        Assertions.assertEquals("c", CommonUtil.lastElement(arrayList).get());

        Set<String> set = new LinkedHashSet<>(list);
        Assertions.assertEquals("c", CommonUtil.lastElement(set).get());

        Deque<String> deque = new ArrayDeque<>(list);
        Assertions.assertEquals("c", CommonUtil.lastElement(deque).get());
    }

    @Test
    public void testLastElementFromIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("c", CommonUtil.lastElement(list.iterator()).get());

        Assertions.assertFalse(CommonUtil.lastElement(Collections.emptyIterator()).isPresent());

        Assertions.assertFalse(CommonUtil.lastElement((Iterator<String>) null).isPresent());
    }

    @Test
    public void testLastElements() {
        List<String> list = Arrays.asList("a", "b", "c", "d");
        List<String> result = CommonUtil.lastElements(list, 2);
        assertEquals(2, result.size());
        assertEquals("c", result.get(0));
        assertEquals("d", result.get(1));
    }

    @Test
    public void testLastElements_Array() {
        String[] arr = { "a", "b", "c", "d", "e" };
        List<String> last3 = CommonUtil.lastElements(arr, 3);
        assertEquals(Arrays.asList("c", "d", "e"), last3);
        List<String> lastAll = CommonUtil.lastElements(arr, 10);
        assertEquals(5, lastAll.size());
        List<String> lastNone = CommonUtil.lastElements(arr, 0);
        assertTrue(lastNone.isEmpty());
        List<String> fromNull = CommonUtil.lastElements((String[]) null, 2);
        assertTrue(fromNull.isEmpty());
    }

    @Test
    public void testLastElementsFromIterable() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");

        Assertions.assertEquals(Arrays.asList("c", "d", "e"), CommonUtil.lastElements(list, 3));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e"), CommonUtil.lastElements(list, 5));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e"), CommonUtil.lastElements(list, 10));
        Assertions.assertEquals(Collections.emptyList(), CommonUtil.lastElements(list, 0));

        Assertions.assertEquals(Collections.emptyList(), CommonUtil.lastElements(Collections.emptyList(), 5));

        Assertions.assertEquals(Collections.emptyList(), CommonUtil.lastElements((Iterable<String>) null, 5));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.lastElements(list, -1));

        Set<String> set = new LinkedHashSet<>(list);
        Assertions.assertEquals(Arrays.asList("c", "d", "e"), CommonUtil.lastElements(set, 3));
    }

    @Test
    public void testLastElementsFromIterator() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");

        Assertions.assertEquals(Arrays.asList("c", "d", "e"), CommonUtil.lastElements(list.iterator(), 3));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e"), CommonUtil.lastElements(list.iterator(), 5));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e"), CommonUtil.lastElements(list.iterator(), 10));
        Assertions.assertEquals(Collections.emptyList(), CommonUtil.lastElements(list.iterator(), 0));

        Assertions.assertEquals(Collections.emptyList(), CommonUtil.lastElements(Collections.emptyIterator(), 5));

        Assertions.assertEquals(Collections.emptyList(), CommonUtil.lastElements((Iterator<String>) null, 5));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.lastElements(list.iterator(), -1));
    }

    @Test
    public void testLastNonNull() {
        com.landawn.abacus.util.u.Optional<String> result = CommonUtil.lastNonNull(null, "first", null, "second", null);
        assertTrue(result.isPresent());
        assertEquals("second", result.get());
    }

    @Test
    public void testLastNonNullTwo() {
        Assertions.assertEquals("b", CommonUtil.lastNonNull("a", "b").get());

        Assertions.assertEquals("b", CommonUtil.lastNonNull(null, "b").get());

        Assertions.assertEquals("a", CommonUtil.lastNonNull("a", null).get());

        Assertions.assertFalse(CommonUtil.lastNonNull(null, null).isPresent());
    }

    @Test
    public void testLastNonNullThree() {
        Assertions.assertEquals("c", CommonUtil.lastNonNull("a", "b", "c").get());

        Assertions.assertEquals("b", CommonUtil.lastNonNull("a", "b", null).get());

        Assertions.assertEquals("a", CommonUtil.lastNonNull("a", null, null).get());

        Assertions.assertFalse(CommonUtil.lastNonNull(null, null, null).isPresent());
    }

    @Test
    public void testLastNonNullVarargs() {
        Assertions.assertEquals("d", CommonUtil.lastNonNull("a", "b", null, "d", null).get());

        Assertions.assertFalse(CommonUtil.lastNonNull(new String[] {}).isPresent());

        Assertions.assertFalse(CommonUtil.lastNonNull((String[]) null).isPresent());

        Assertions.assertFalse(CommonUtil.lastNonNull(new String[] { null, null, null }).isPresent());
    }

    @Test
    public void testLastNonNullIterable() {
        List<String> list = Arrays.asList("a", "b", null, "d", null);
        Assertions.assertEquals("d", CommonUtil.lastNonNull(list).get());

        Assertions.assertFalse(CommonUtil.lastNonNull(Collections.emptyList()).isPresent());

        Assertions.assertFalse(CommonUtil.lastNonNull((Iterable<String>) null).isPresent());

        List<String> allNulls = Arrays.asList(null, null, null);
        Assertions.assertFalse(CommonUtil.lastNonNull(allNulls).isPresent());

        ArrayList<String> arrayList = new ArrayList<>(list);
        Assertions.assertEquals("d", CommonUtil.lastNonNull(arrayList).get());

    }

    @Test
    public void testLastNonNullIterator() {
        List<String> list = Arrays.asList("a", "b", null, "d", null);
        Assertions.assertEquals("d", CommonUtil.lastNonNull(list.iterator()).get());

        Assertions.assertFalse(CommonUtil.lastNonNull(Collections.emptyIterator()).isPresent());

        Assertions.assertFalse(CommonUtil.lastNonNull((Iterator<String>) null).isPresent());

        List<String> allNulls = Arrays.asList(null, null, null);
        Assertions.assertFalse(CommonUtil.lastNonNull(allNulls.iterator()).isPresent());
    }

    @Test
    public void testLastNonNull2() {
        assertEquals("b", CommonUtil.lastNonNull("a", "b").get());
        assertEquals("a", CommonUtil.lastNonNull("a", null).get());
        assertFalse(CommonUtil.lastNonNull(null, null).isPresent());
    }

    // ========== lastNonNull(Iterable) - Deque and non-list Iterable paths ==========

    @Test
    public void testLastNonNull_Iterable_Deque() {
        // LinkedList implements Deque and supports null; descendingIterator is used
        java.util.LinkedList<String> deque = new java.util.LinkedList<>(Arrays.asList(null, "first", null, "last"));
        com.landawn.abacus.util.u.Optional<String> result = CommonUtil.lastNonNull(deque);
        assertTrue(result.isPresent());
        assertEquals("last", result.get());
    }

    @Test
    public void testLastNonNull_Iterable_Deque_AllNull() {
        java.util.LinkedList<String> deque = new java.util.LinkedList<>(Arrays.asList(null, null));
        com.landawn.abacus.util.u.Optional<String> result = CommonUtil.lastNonNull(deque);
        assertFalse(result.isPresent());
    }

    @Test
    public void testLastNonNullOrDefault_Array() {
        String[] arr = { "first", "second", null, null };
        assertEquals("second", CommonUtil.lastNonNullOrDefault(arr, "default"));
        String[] allNull = { null, null };
        assertEquals("default", CommonUtil.lastNonNullOrDefault(allNull, "default"));
        assertEquals("default", CommonUtil.lastNonNullOrDefault((String[]) null, "default"));
    }

    @Test
    public void testLastEntry() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        com.landawn.abacus.util.u.Optional<Map.Entry<String, Integer>> entry = CommonUtil.lastEntry(map);
        assertTrue(entry.isPresent());
        assertEquals("b", entry.get().getKey());
        assertEquals(Integer.valueOf(2), entry.get().getValue());
    }

    @Test
    public void testLastOrNullIfEmpty() {
        List<String> list = Arrays.asList("a", "b");
        assertEquals("b", CommonUtil.lastOrNullIfEmpty(list));
        assertNull(CommonUtil.lastOrNullIfEmpty(new ArrayList<>()));
    }

    @Test
    public void testLastOrNullIfEmptyArray() {
        String[] arr = { "a", "b", "c" };
        Assertions.assertEquals("c", CommonUtil.lastOrNullIfEmpty(arr));

        Assertions.assertNull(CommonUtil.lastOrNullIfEmpty(new String[] {}));

        Assertions.assertNull(CommonUtil.lastOrNullIfEmpty((String[]) null));
    }

    @Test
    public void testLastOrNullIfEmptyIterable() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("c", CommonUtil.lastOrNullIfEmpty(list));

        Assertions.assertNull(CommonUtil.lastOrNullIfEmpty(Collections.emptyList()));

        Assertions.assertNull(CommonUtil.lastOrNullIfEmpty((Iterable<String>) null));
    }

    @Test
    public void testLastOrNullIfEmptyIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("c", CommonUtil.lastOrNullIfEmpty(list.iterator()));

        Assertions.assertNull(CommonUtil.lastOrNullIfEmpty(Collections.emptyIterator()));

        Assertions.assertNull(CommonUtil.lastOrNullIfEmpty((Iterator<String>) null));
    }

    @Test
    public void testLastOrDefaultIfEmpty() {
        List<String> list = Arrays.asList("a", "b");
        assertEquals("b", CommonUtil.lastOrDefaultIfEmpty(list, "default"));
        assertEquals("default", CommonUtil.lastOrDefaultIfEmpty(new ArrayList<>(), "default"));
    }

    @Test
    public void testLastOrDefaultIfEmptyArray() {
        String[] arr = { "a", "b", "c" };
        Assertions.assertEquals("c", CommonUtil.lastOrDefaultIfEmpty(arr, "default"));

        Assertions.assertEquals("default", CommonUtil.lastOrDefaultIfEmpty(new String[] {}, "default"));

        Assertions.assertEquals("default", CommonUtil.lastOrDefaultIfEmpty((String[]) null, "default"));
    }

    @Test
    public void testLastOrDefaultIfEmptyIterable() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("c", CommonUtil.lastOrDefaultIfEmpty(list, "default"));

        Assertions.assertEquals("default", CommonUtil.lastOrDefaultIfEmpty(Collections.emptyList(), "default"));

        Assertions.assertEquals("default", CommonUtil.lastOrDefaultIfEmpty((Iterable<String>) null, "default"));

        ArrayList<String> arrayList = new ArrayList<>(list);
        Assertions.assertEquals("c", CommonUtil.lastOrDefaultIfEmpty(arrayList, "default"));

        Deque<String> deque = new ArrayDeque<>(list);
        Assertions.assertEquals("c", CommonUtil.lastOrDefaultIfEmpty(deque, "default"));
    }

    @Test
    public void testLastOrDefaultIfEmptyIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("c", CommonUtil.lastOrDefaultIfEmpty(list.iterator(), "default"));

        Assertions.assertEquals("default", CommonUtil.lastOrDefaultIfEmpty(Collections.emptyIterator(), "default"));

        Assertions.assertEquals("default", CommonUtil.lastOrDefaultIfEmpty((Iterator<String>) null, "default"));
    }

    @Test
    public void testLastIndexOf() {
        assertEquals(4, CommonUtil.lastIndexOf(new boolean[] { true, false, true, false, true }, true));
        assertEquals(-1, CommonUtil.lastIndexOf((boolean[]) null, true));
        assertEquals(2, CommonUtil.lastIndexOf(new boolean[] { true, false, true, false, true }, true, 3));

        assertEquals(4, CommonUtil.lastIndexOf(new char[] { 'a', 'b', 'c', 'd', 'c' }, 'c'));
        assertEquals(-1, CommonUtil.lastIndexOf(new char[] {}, 'c'));
        assertEquals(2, CommonUtil.lastIndexOf(new char[] { 'a', 'b', 'c', 'd', 'c' }, 'c', 3));

        assertEquals(4, CommonUtil.lastIndexOf(new byte[] { 1, 2, 3, 4, 3 }, (byte) 3));
        assertEquals(4, CommonUtil.lastIndexOf(new short[] { 1, 2, 3, 4, 3 }, (short) 3));
        assertEquals(4, CommonUtil.lastIndexOf(new int[] { 1, 2, 3, 4, 3 }, 3));
        assertEquals(-1, CommonUtil.lastIndexOf((int[]) null, 3));
        assertEquals(2, CommonUtil.lastIndexOf(new int[] { 1, 2, 3, 4, 3 }, 3, 3));
        assertEquals(4, CommonUtil.lastIndexOf(new long[] { 1L, 2L, 3L, 4L, 3L }, 3L));
        assertEquals(4, CommonUtil.lastIndexOf(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 3.0f }, 3.0f));
        assertEquals(4, CommonUtil.lastIndexOf(new double[] { 1.0, 2.0, 3.0, 4.0, 3.0 }, 3.0));

        String[] arr = { "a", "b", "c", "d", "c" };
        assertEquals(4, CommonUtil.lastIndexOf(arr, "c"));
        assertEquals(3, CommonUtil.lastIndexOf(new String[] { "a", null, "c", null }, null));
        assertEquals(2, CommonUtil.lastIndexOf(arr, "c", 3));

        List<String> list = Arrays.asList("a", "b", "c", "b", "d");
        assertEquals(3, CommonUtil.lastIndexOf(list, "b"));
        assertEquals(-1, CommonUtil.lastIndexOf((Collection<?>) null, "c"));
        assertEquals(3, CommonUtil.lastIndexOf(list, "b", 4));
        assertEquals(1, CommonUtil.lastIndexOf(list, "b", 2));
        assertEquals(-1, CommonUtil.lastIndexOf(new ArrayList<>(), "a", 0));
        assertEquals(2, CommonUtil.lastIndexOf(new LinkedList<>(Arrays.asList("a", "b", "c", "d", "c")), "c", 3));
    }

    @Test
    public void testLastIndexOf_tolerance() {
        float[] floats = { 1.0f, 2.0f, 3.001f, 4.0f, 3.002f };
        assertEquals(4, CommonUtil.lastIndexOf(floats, 3.0f, floats.length - 1, 0.01f));
        assertEquals(2, CommonUtil.lastIndexOf(floats, 3.0f, 3, 0.01f));
        assertEquals(-1, CommonUtil.lastIndexOf(floats, 3.0f, 3, 0.0001f));
        assertEquals(-1, CommonUtil.lastIndexOf((float[]) null, 3.0f, 0, 0.01f));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.lastIndexOf(floats, 3.0f, floats.length - 1, -0.01f));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.lastIndexOf((float[]) null, 1.0f, 0, -0.01f));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.lastIndexOf(new float[0], 1.0f, -1, Float.NaN));

        double[] doubles = { 1.0, 2.0, 3.001, 4.0, 3.002 };
        assertEquals(4, CommonUtil.lastIndexOf(doubles, 3.0, doubles.length - 1, 0.01));
        assertEquals(2, CommonUtil.lastIndexOf(doubles, 3.0, 3, 0.01));
        assertEquals(-1, CommonUtil.lastIndexOf(doubles, 3.0, 3, 0.0001));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.lastIndexOf((double[]) null, 1.0, 0, -0.01));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.lastIndexOf(new double[0], 1.0, -1, Double.NaN));
    }

    @Test
    public void testLastIndexOfSubList_withStartIndex() {
        List<String> source = Arrays.asList("a", "b", "c", "d", "c", "d");
        List<String> sub = Arrays.asList("c", "d");
        Assertions.assertEquals(2, CommonUtil.lastIndexOfSubList(source, sub, 3));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOfSubList(source, sub, 1));
    }

    @Test
    public void testLastIndexOfSubList() {
        List<Integer> list = Arrays.asList(1, 2, 3, 2, 3, 4);
        List<Integer> subList = Arrays.asList(2, 3);
        assertEquals(3, CommonUtil.lastIndexOfSubList(list, subList));

        assertEquals(0, Strings.lastIndexOf("", ""));
        assertEquals(0, "".lastIndexOf(""));
        assertEquals(0, Collections.lastIndexOfSubList(CommonUtil.emptyList(), CommonUtil.emptyList()));
        assertEquals(0, CommonUtil.lastIndexOfSubList(CommonUtil.emptyList(), CommonUtil.emptyList()));
        assertEquals(0, Index.lastOfSubList(CommonUtil.emptyList(), CommonUtil.emptyList()).orElseThrow());
        assertEquals(0, Index.lastOfSubList(CommonUtil.emptyList(), 0, CommonUtil.emptyList()).orElseThrow());
        assertEquals(Collections.lastIndexOfSubList(CommonUtil.emptyList(), CommonUtil.emptyList()),
                CommonUtil.lastIndexOfSubList(CommonUtil.emptyList(), CommonUtil.emptyList()));
    }

    @Test
    public void testLastIndexOfIgnoreCase() {
        String[] arr = { "Apple", "Banana", "apple" };
        assertEquals(2, CommonUtil.lastIndexOfIgnoreCase(arr, "APPLE"));
        assertEquals(-1, CommonUtil.lastIndexOfIgnoreCase(arr, "grape"));
    }

    @Test
    public void testLastIndexOfIgnoreCase_withStartIndex() {
        String[] arr = { "A", "B", "C", "D", "c" };
        Assertions.assertEquals(2, CommonUtil.lastIndexOfIgnoreCase(arr, "C", 3));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOfIgnoreCase(arr, "C", -1));
    }

    @Test
    public void testLastNonNull_Iterable_LinkedHashSet_uncovered() {
        LinkedHashSet<String> set = new LinkedHashSet<>(Arrays.asList("a", "b", "c"));
        com.landawn.abacus.util.u.Optional<String> result = CommonUtil.lastNonNull(set);
        assertTrue(result.isPresent());
        assertEquals("c", result.get());
    }

    @Test
    public void testLastElementsSupportsNullElements() {
        final Set<String> set = new LinkedHashSet<>(Arrays.asList("a", null, "c"));

        assertEquals(Arrays.asList(null, "c"), CommonUtil.lastElements(set, 2));
        assertEquals(Arrays.asList(null, "c"), CommonUtil.lastElements(Arrays.asList("a", null, "c").iterator(), 2));
    }

    // G05-130: the window is collected in a pre-sized ArrayDeque again, with a null element held as an internal
    // sentinel; the sentinel must never reach the caller and the window must keep its encounter order.
    @Test
    public void testLastElementsMapsTheNullSentinelBack() {
        final Set<String> set = new LinkedHashSet<>(Arrays.asList("a", null, "c"));

        assertEquals(Arrays.asList("a", null, "c"), CommonUtil.lastElements(set, 3));
        assertEquals(Arrays.asList("a", null, "c"), CommonUtil.lastElements(set, 9));
        assertEquals(Collections.singletonList("c"), CommonUtil.lastElements(set, 1));
        assertEquals(Arrays.asList(null, null), CommonUtil.lastElements(Arrays.asList(null, null, null).iterator(), 2));

        // a real null, not a placeholder that merely prints as one, and a list the caller may modify
        final List<String> window = CommonUtil.lastElements(Arrays.asList("a", null, "c").iterator(), 3);
        assertNull(window.get(1));
        window.set(0, "z");
        assertEquals(Arrays.asList("z", null, "c"), window);

        // a window wider than the ArrayDeque's initial capacity keeps every element, in order
        final List<Integer> source = new ArrayList<>();

        for (int i = 0; i < 3000; i++) {
            source.add(i % 7 == 0 ? null : i); // many nulls: the iterator overload sees them all
        }

        assertEquals(source.subList(1000, 3000), CommonUtil.lastElements(source.iterator(), 2000));

        // the same for a non-List Collection (a Set holds one null, so the elements are distinct here)
        final List<Integer> distinct = new ArrayList<>();

        for (int i = 0; i < 3000; i++) {
            distinct.add(i == 5 ? null : i);
        }

        assertEquals(distinct.subList(1000, 3000), CommonUtil.lastElements(new LinkedHashSet<>(distinct), 2000));
    }

    @Test
    public void testLastIndexOf_toleranceStartIndexFromBackClamping() {
        final float[] floats = { 1.0f, 2.0f, 1.0f };
        assertEquals(-1, CommonUtil.lastIndexOf(floats, 1.0f, -1, 0.01f));
        assertEquals(-1, CommonUtil.lastIndexOf(floats, 1.0f, -100, 0.01f));
        assertEquals(2, CommonUtil.lastIndexOf(floats, 1.0f, floats.length, 0.01f));
        assertEquals(2, CommonUtil.lastIndexOf(floats, 1.0f, 99, 0.01f));
        assertEquals(0, CommonUtil.lastIndexOf(floats, 1.0f, 1, 0.01f));
        assertEquals(-1, CommonUtil.lastIndexOf((float[]) null, 1.0f, 1, 0.01f));

        final double[] doubles = { 1.0d, 2.0d, 1.0d };
        assertEquals(-1, CommonUtil.lastIndexOf(doubles, 1.0d, -1, 0.01d));
        assertEquals(-1, CommonUtil.lastIndexOf(doubles, 1.0d, -100, 0.01d));
        assertEquals(2, CommonUtil.lastIndexOf(doubles, 1.0d, doubles.length, 0.01d));
        assertEquals(2, CommonUtil.lastIndexOf(doubles, 1.0d, 99, 0.01d));
        assertEquals(0, CommonUtil.lastIndexOf(doubles, 1.0d, 1, 0.01d));
        assertEquals(-1, CommonUtil.lastIndexOf((double[]) null, 1.0d, 1, 0.01d));
    }

}
