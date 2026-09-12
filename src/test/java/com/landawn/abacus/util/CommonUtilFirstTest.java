package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CommonUtilFirstTest extends CommonUtilTestSupport {
    @Test
    public void testFirstElement() {
        List<String> list = Arrays.asList("a", "b", "c");
        com.landawn.abacus.util.u.Nullable<String> result = CommonUtil.firstElement(list);
        assertTrue(result.isPresent());
        assertEquals("a", result.get());
    }

    @Test
    public void testFirstElementFromIterable() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("a", CommonUtil.firstElement(list).get());

        Assertions.assertFalse(CommonUtil.firstElement(Collections.emptyList()).isPresent());

        Assertions.assertFalse(CommonUtil.firstElement((Iterable<String>) null).isPresent());

        ArrayList<String> arrayList = new ArrayList<>(list);
        Assertions.assertEquals("a", CommonUtil.firstElement(arrayList).get());

        Set<String> set = new LinkedHashSet<>(list);
        Assertions.assertEquals("a", CommonUtil.firstElement(set).get());
    }

    @Test
    public void testFirstElementFromIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("a", CommonUtil.firstElement(list.iterator()).get());

        Assertions.assertFalse(CommonUtil.firstElement(Collections.emptyIterator()).isPresent());

        Assertions.assertFalse(CommonUtil.firstElement((Iterator<String>) null).isPresent());
    }

    @Test
    public void testFirstElements() {
        List<String> list = Arrays.asList("a", "b", "c", "d");
        List<String> result = CommonUtil.firstElements(list, 2);
        assertEquals(2, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
    }

    @Test
    public void testFirstElements_Iterable_NonCollection() {
        // Use a non-Collection Iterable to hit the iterator path
        Iterable<String> iterable = () -> Arrays.asList("x", "y", "z", "w").iterator();
        List<String> result = CommonUtil.firstElements(iterable, 2);
        assertEquals(2, result.size());
        assertEquals("x", result.get(0));
        assertEquals("y", result.get(1));
    }

    @Test
    public void testFirstElements_Iterable_MoreThanAvailable() {
        Iterable<Integer> iterable = () -> Arrays.asList(1, 2).iterator();
        List<Integer> result = CommonUtil.firstElements(iterable, 10);
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(1), result.get(0));
    }

    @Test
    public void testFirstElements_Array() {
        String[] arr = { "a", "b", "c", "d", "e" };
        List<String> first3 = CommonUtil.firstElements(arr, 3);
        assertEquals(Arrays.asList("a", "b", "c"), first3);
        List<String> firstAll = CommonUtil.firstElements(arr, 10);
        assertEquals(5, firstAll.size());
        List<String> firstNone = CommonUtil.firstElements(arr, 0);
        assertTrue(firstNone.isEmpty());
        List<String> fromNull = CommonUtil.firstElements((String[]) null, 2);
        assertTrue(fromNull.isEmpty());
    }

    @Test
    public void testFirstElements_Iterable_ZeroN() {
        List<String> result = CommonUtil.firstElements(Arrays.asList("a", "b"), 0);
        assertEquals(0, result.size());
    }

    @Test
    public void testFirstElementsFromIterable() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");

        Assertions.assertEquals(Arrays.asList("a", "b", "c"), CommonUtil.firstElements(list, 3));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e"), CommonUtil.firstElements(list, 5));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e"), CommonUtil.firstElements(list, 10));
        Assertions.assertEquals(Collections.emptyList(), CommonUtil.firstElements(list, 0));

        Assertions.assertEquals(Collections.emptyList(), CommonUtil.firstElements(Collections.emptyList(), 5));

        Assertions.assertEquals(Collections.emptyList(), CommonUtil.firstElements((Iterable<String>) null, 5));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.firstElements(list, -1));

        Set<String> set = new LinkedHashSet<>(list);
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), CommonUtil.firstElements(set, 3));
    }

    @Test
    public void testFirstElementsFromIterator() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");

        Assertions.assertEquals(Arrays.asList("a", "b", "c"), CommonUtil.firstElements(list.iterator(), 3));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e"), CommonUtil.firstElements(list.iterator(), 5));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e"), CommonUtil.firstElements(list.iterator(), 10));
        Assertions.assertEquals(Collections.emptyList(), CommonUtil.firstElements(list.iterator(), 0));

        Assertions.assertEquals(Collections.emptyList(), CommonUtil.firstElements(Collections.emptyIterator(), 5));

        Assertions.assertEquals(Collections.emptyList(), CommonUtil.firstElements((Iterator<String>) null, 5));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.firstElements(list.iterator(), -1));
    }

    @Test
    public void testFirstElements_Iterable_NegativeN() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.firstElements(Arrays.asList("a"), -1));
    }

    @Test
    public void testFirstNonNull() {
        com.landawn.abacus.util.u.Optional<String> result = CommonUtil.firstNonNull(null, "first", null, "second");
        assertTrue(result.isPresent());
        assertEquals("first", result.get());
    }

    @Test
    public void testFirstNonNullTwo() {
        Assertions.assertEquals("a", CommonUtil.firstNonNull("a", "b").get());

        Assertions.assertEquals("b", CommonUtil.firstNonNull(null, "b").get());

        Assertions.assertEquals("a", CommonUtil.firstNonNull("a", null).get());

        Assertions.assertFalse(CommonUtil.firstNonNull(null, null).isPresent());
    }

    @Test
    public void testFirstNonNullThree() {
        Assertions.assertEquals("a", CommonUtil.firstNonNull("a", "b", "c").get());

        Assertions.assertEquals("b", CommonUtil.firstNonNull(null, "b", "c").get());

        Assertions.assertEquals("c", CommonUtil.firstNonNull(null, null, "c").get());

        Assertions.assertFalse(CommonUtil.firstNonNull(null, null, null).isPresent());
    }

    @Test
    public void testFirstNonNullVarargs() {
        Assertions.assertEquals("c", CommonUtil.firstNonNull(null, null, "c", "d").get());

        Assertions.assertFalse(CommonUtil.firstNonNull(new String[] {}).isPresent());

        Assertions.assertFalse(CommonUtil.firstNonNull((String[]) null).isPresent());

        Assertions.assertFalse(CommonUtil.firstNonNull(new String[] { null, null, null }).isPresent());
    }

    @Test
    public void testFirstNonNullIterable() {
        List<String> list = Arrays.asList(null, null, "c", "d");
        Assertions.assertEquals("c", CommonUtil.firstNonNull(list).get());

        Assertions.assertFalse(CommonUtil.firstNonNull(Collections.emptyList()).isPresent());

        Assertions.assertFalse(CommonUtil.firstNonNull((Iterable<String>) null).isPresent());

        List<String> allNulls = Arrays.asList(null, null, null);
        Assertions.assertFalse(CommonUtil.firstNonNull(allNulls).isPresent());
    }

    @Test
    public void testFirstNonNullIterator() {
        List<String> list = Arrays.asList(null, null, "c", "d");
        Assertions.assertEquals("c", CommonUtil.firstNonNull(list.iterator()).get());

        Assertions.assertFalse(CommonUtil.firstNonNull(Collections.emptyIterator()).isPresent());

        Assertions.assertFalse(CommonUtil.firstNonNull((Iterator<String>) null).isPresent());

        List<String> allNulls = Arrays.asList(null, null, null);
        Assertions.assertFalse(CommonUtil.firstNonNull(allNulls.iterator()).isPresent());
    }

    @Test
    public void testFirstNonNull2() {
        assertEquals("a", CommonUtil.firstNonNull("a", "b").get());
        assertEquals("b", CommonUtil.firstNonNull(null, "b").get());
        assertFalse(CommonUtil.firstNonNull(null, null).isPresent());
    }

    @Test
    public void testFirstNonNullOrDefault_Array() {
        String[] arr = { null, null, "found", "other" };
        assertEquals("found", CommonUtil.firstNonNullOrDefault(arr, "default"));
        String[] allNull = { null, null };
        assertEquals("default", CommonUtil.firstNonNullOrDefault(allNull, "default"));
        assertEquals("default", CommonUtil.firstNonNullOrDefault((String[]) null, "default"));
        assertEquals("default", CommonUtil.firstNonNullOrDefault(new String[0], "default"));
    }

    @Test
    public void testFirstNonEmpty() {
        com.landawn.abacus.util.u.Optional<String> result = CommonUtil.firstNonEmpty("", null, "first", "second");
        assertTrue(result.isPresent());
        assertEquals("first", result.get());
    }

    @Test
    public void testFirstNonEmptyArraysTwo() {
        String[] arr1 = { "a", "b" };
        String[] arr2 = { "c", "d" };
        String[] empty = {};

        Assertions.assertArrayEquals(arr1, CommonUtil.firstNonEmpty(arr1, arr2).get());

        Assertions.assertArrayEquals(arr2, CommonUtil.firstNonEmpty(empty, arr2).get());

        Assertions.assertArrayEquals(arr1, CommonUtil.firstNonEmpty(arr1, empty).get());

        Assertions.assertFalse(CommonUtil.firstNonEmpty(empty, empty).isPresent());

        Assertions.assertArrayEquals(arr1, CommonUtil.firstNonEmpty(null, arr1).get());
        Assertions.assertArrayEquals(arr1, CommonUtil.firstNonEmpty(arr1, null).get());
        Assertions.assertFalse(CommonUtil.firstNonEmpty((String[]) null, (String[]) null).isPresent());
    }

    @Test
    public void testFirstNonEmptyArraysThree() {
        String[] arr1 = { "a", "b" };
        String[] arr2 = { "c", "d" };
        String[] arr3 = { "e", "f" };
        String[] empty = {};

        Assertions.assertArrayEquals(arr1, CommonUtil.firstNonEmpty(arr1, arr2, arr3).get());

        Assertions.assertArrayEquals(arr2, CommonUtil.firstNonEmpty(empty, arr2, arr3).get());

        Assertions.assertArrayEquals(arr3, CommonUtil.firstNonEmpty(empty, empty, arr3).get());

        Assertions.assertFalse(CommonUtil.firstNonEmpty(empty, empty, empty).isPresent());
    }

    @Test
    public void testFirstNonEmptyCollectionsTwo() {
        List<String> list1 = Arrays.asList("a", "b");
        List<String> list2 = Arrays.asList("c", "d");
        List<String> empty = Collections.emptyList();

        Assertions.assertEquals(list1, CommonUtil.firstNonEmpty(list1, list2).get());

        Assertions.assertEquals(list2, CommonUtil.firstNonEmpty(empty, list2).get());

        Assertions.assertEquals(list1, CommonUtil.firstNonEmpty(list1, empty).get());

        Assertions.assertFalse(CommonUtil.firstNonEmpty(empty, empty).isPresent());

        Assertions.assertEquals(list1, CommonUtil.firstNonEmpty(null, list1).get());
        Assertions.assertEquals(list1, CommonUtil.firstNonEmpty(list1, null).get());
        Assertions.assertFalse(CommonUtil.firstNonEmpty((List<String>) null, (List<String>) null).isPresent());
    }

    @Test
    public void testFirstNonEmptyCollectionsThree() {
        List<String> list1 = Arrays.asList("a", "b");
        List<String> list2 = Arrays.asList("c", "d");
        List<String> list3 = Arrays.asList("e", "f");
        List<String> empty = Collections.emptyList();

        Assertions.assertEquals(list1, CommonUtil.firstNonEmpty(list1, list2, list3).get());

        Assertions.assertEquals(list2, CommonUtil.firstNonEmpty(empty, list2, list3).get());

        Assertions.assertEquals(list3, CommonUtil.firstNonEmpty(empty, empty, list3).get());

        Assertions.assertFalse(CommonUtil.firstNonEmpty(empty, empty, empty).isPresent());
    }

    @Test
    public void testFirstNonEmptyMapsTwo() {
        Map<String, String> map1 = new HashMap<>();
        map1.put("a", "1");
        Map<String, String> map2 = new HashMap<>();
        map2.put("b", "2");
        Map<String, String> empty = Collections.emptyMap();

        Assertions.assertEquals(map1, CommonUtil.firstNonEmpty(map1, map2).get());

        Assertions.assertEquals(map2, CommonUtil.firstNonEmpty(empty, map2).get());

        Assertions.assertEquals(map1, CommonUtil.firstNonEmpty(map1, empty).get());

        Assertions.assertFalse(CommonUtil.firstNonEmpty(empty, empty).isPresent());

        Assertions.assertEquals(map1, CommonUtil.firstNonEmpty(null, map1).get());
        Assertions.assertEquals(map1, CommonUtil.firstNonEmpty(map1, null).get());
        Assertions.assertFalse(CommonUtil.firstNonEmpty((Map<String, String>) null, (Map<String, String>) null).isPresent());
    }

    @Test
    public void testFirstNonEmptyMapsThree() {
        Map<String, String> map1 = new HashMap<>();
        map1.put("a", "1");
        Map<String, String> map2 = new HashMap<>();
        map2.put("b", "2");
        Map<String, String> map3 = new HashMap<>();
        map3.put("c", "3");
        Map<String, String> empty = Collections.emptyMap();

        Assertions.assertEquals(map1, CommonUtil.firstNonEmpty(map1, map2, map3).get());

        Assertions.assertEquals(map2, CommonUtil.firstNonEmpty(empty, map2, map3).get());

        Assertions.assertEquals(map3, CommonUtil.firstNonEmpty(empty, empty, map3).get());

        Assertions.assertFalse(CommonUtil.firstNonEmpty(empty, empty, empty).isPresent());
    }

    @Test
    public void testFirstNonEmptyCharSequencesTwo() {
        Assertions.assertEquals("hello", CommonUtil.firstNonEmpty("hello", "world").get());

        Assertions.assertEquals("world", CommonUtil.firstNonEmpty("", "world").get());

        Assertions.assertEquals("hello", CommonUtil.firstNonEmpty("hello", "").get());

        Assertions.assertFalse(CommonUtil.firstNonEmpty("", "").isPresent());

        Assertions.assertEquals("hello", CommonUtil.firstNonEmpty(null, "hello").get());
        Assertions.assertEquals("hello", CommonUtil.firstNonEmpty("hello", null).get());
        Assertions.assertFalse(CommonUtil.firstNonEmpty((String) null, (String) null).isPresent());

        StringBuilder sb = new StringBuilder("builder");
        StringBuffer buf = new StringBuffer("buffer");
        Assertions.assertEquals(sb, CommonUtil.firstNonEmpty(sb, buf).get());
    }

    @Test
    public void testFirstNonEmptyCharSequencesThree() {
        Assertions.assertEquals("a", CommonUtil.firstNonEmpty("a", "b", "c").get());

        Assertions.assertEquals("b", CommonUtil.firstNonEmpty("", "b", "c").get());

        Assertions.assertEquals("c", CommonUtil.firstNonEmpty("", "", "c").get());

        Assertions.assertFalse(CommonUtil.firstNonEmpty("", "", "").isPresent());

        Assertions.assertEquals("c", CommonUtil.firstNonEmpty(null, "", "c").get());
    }

    @Test
    public void testFirstNonEmptyCharSequencesVarargs() {
        Assertions.assertEquals("c", CommonUtil.firstNonEmpty("", null, "c", "d").get());

        Assertions.assertFalse(CommonUtil.firstNonEmpty(new String[] {}).isPresent());

        Assertions.assertFalse(CommonUtil.firstNonEmpty((String[]) null).isPresent());

        Assertions.assertFalse(CommonUtil.firstNonEmpty("", "", "").isPresent());
    }

    @Test
    public void testFirstNonEmptyCharSequencesIterable() {
        List<String> list = Arrays.asList("", null, "c", "d");
        Assertions.assertEquals("c", CommonUtil.firstNonEmpty(list).get());

        Assertions.assertFalse(CommonUtil.firstNonEmpty(Collections.<String> emptyList()).isPresent());

        Assertions.assertFalse(CommonUtil.firstNonEmpty((Iterable<String>) null).isPresent());

        List<String> allEmpty = Arrays.asList("", "", null);
        Assertions.assertFalse(CommonUtil.firstNonEmpty(allEmpty).isPresent());
    }

    @Test
    public void testFirstNonEmptyArrays() {
        String[] a1 = { "a", "b" };
        String[] a2 = { "c", "d" };
        String[] empty = {};

        assertEquals(a1, CommonUtil.firstNonEmpty(a1, a2).get());
        assertEquals(a2, CommonUtil.firstNonEmpty(null, a2).get());
        assertEquals(a2, CommonUtil.firstNonEmpty(empty, a2).get());
        assertFalse(CommonUtil.firstNonEmpty((String[]) null, (String[]) null).isPresent());
        assertFalse(CommonUtil.firstNonEmpty(empty, empty).isPresent());

        assertEquals(a1, CommonUtil.firstNonEmpty(a1, a2, empty).get());
        assertEquals(a2, CommonUtil.firstNonEmpty(empty, a2, a1).get());
        assertEquals(a1, CommonUtil.firstNonEmpty(empty, empty, a1).get());
    }

    @Test
    public void testFirstNonEmptyCollections() {
        List<String> list1 = Arrays.asList("a", "b");
        List<String> list2 = Arrays.asList("c", "d");
        List<String> empty = Collections.emptyList();

        assertEquals(list1, CommonUtil.firstNonEmpty(list1, list2).get());
        assertEquals(list2, CommonUtil.firstNonEmpty(null, list2).get());
        assertEquals(list2, CommonUtil.firstNonEmpty(empty, list2).get());
        assertFalse(CommonUtil.firstNonEmpty((List<String>) null, (List<String>) null).isPresent());
        assertFalse(CommonUtil.firstNonEmpty(empty, empty).isPresent());

        assertEquals(list1, CommonUtil.firstNonEmpty(list1, list2, empty).get());
    }

    @Test
    public void testFirstNonEmptyMaps() {
        Map<String, String> map1 = new HashMap<>();
        map1.put("a", "1");
        Map<String, String> map2 = new HashMap<>();
        map2.put("b", "2");
        Map<String, String> empty = Collections.emptyMap();

        assertEquals(map1, CommonUtil.firstNonEmpty(map1, map2).get());
        assertEquals(map2, CommonUtil.firstNonEmpty(null, map2).get());
        assertEquals(map2, CommonUtil.firstNonEmpty(empty, map2).get());
        assertFalse(CommonUtil.firstNonEmpty((Map<String, String>) null, (Map<String, String>) null).isPresent());

        assertEquals(map1, CommonUtil.firstNonEmpty(map1, map2, empty).get());
    }

    @Test
    public void testFirstNonEmptyCharSequences() {
        assertEquals("abc", CommonUtil.firstNonEmpty("abc", "def").get());
        assertEquals("def", CommonUtil.firstNonEmpty("", "def").get());
        assertEquals("def", CommonUtil.firstNonEmpty(null, "def").get());
        assertFalse(CommonUtil.firstNonEmpty("", "").isPresent());
        assertFalse(CommonUtil.firstNonEmpty((String) null, (String) null).isPresent());

        assertEquals("abc", CommonUtil.firstNonEmpty("abc", "def", "ghi").get());
        assertEquals("def", CommonUtil.firstNonEmpty("", "def", "ghi").get());
        assertEquals("ghi", CommonUtil.firstNonEmpty("", "", "ghi").get());
    }

    @Test
    public void testFirstNonEmptyOrDefault_Array() {
        String[] arr = { "", null, "found", "other" };
        assertEquals("found", CommonUtil.firstNonEmptyOrDefault(arr, "default"));
        String[] allEmpty = { "", null };
        assertEquals("default", CommonUtil.firstNonEmptyOrDefault(allEmpty, "default"));
        assertEquals("default", CommonUtil.firstNonEmptyOrDefault((String[]) null, "default"));
    }

    @Test
    public void testFirstNonEmptyOrDefault_Iterable() {
        List<String> list = Arrays.asList("", null, "found", "other");
        assertEquals("found", CommonUtil.firstNonEmptyOrDefault(list, "default"));
        List<String> allEmpty = Arrays.asList("", null);
        assertEquals("default", CommonUtil.firstNonEmptyOrDefault(allEmpty, "default"));
        assertEquals("default", CommonUtil.firstNonEmptyOrDefault((Iterable<String>) null, "default"));
    }

    @Test
    public void testFirstNonBlank() {
        com.landawn.abacus.util.u.Optional<String> result = CommonUtil.firstNonBlank("", "  ", "first", "second");
        assertTrue(result.isPresent());
        assertEquals("first", result.get());
    }

    @Test
    public void testFirstNonBlankTwo() {
        Assertions.assertEquals("hello", CommonUtil.firstNonBlank("hello", "world").get());

        Assertions.assertEquals("world", CommonUtil.firstNonBlank("  ", "world").get());

        Assertions.assertEquals("hello", CommonUtil.firstNonBlank("hello", "  ").get());

        Assertions.assertFalse(CommonUtil.firstNonBlank("  ", "  ").isPresent());

        Assertions.assertEquals("hello", CommonUtil.firstNonBlank(null, "hello").get());
        Assertions.assertEquals("hello", CommonUtil.firstNonBlank("hello", null).get());
        Assertions.assertFalse(CommonUtil.firstNonBlank((String) null, (String) null).isPresent());
    }

    @Test
    public void testFirstNonBlankThree() {
        Assertions.assertEquals("a", CommonUtil.firstNonBlank("a", "b", "c").get());

        Assertions.assertEquals("b", CommonUtil.firstNonBlank("  ", "b", "c").get());

        Assertions.assertEquals("c", CommonUtil.firstNonBlank("  ", "  ", "c").get());

        Assertions.assertFalse(CommonUtil.firstNonBlank("  ", "  ", "  ").isPresent());

        Assertions.assertEquals("c", CommonUtil.firstNonBlank(null, "", "c").get());
        Assertions.assertEquals("c", CommonUtil.firstNonBlank("  ", "\t", "c").get());
    }

    @Test
    public void testFirstNonBlankVarargs() {
        Assertions.assertEquals("c", CommonUtil.firstNonBlank("  ", null, "c", "d").get());

        Assertions.assertFalse(CommonUtil.firstNonBlank(new String[] {}).isPresent());

        Assertions.assertFalse(CommonUtil.firstNonBlank((String[]) null).isPresent());

        Assertions.assertFalse(CommonUtil.firstNonBlank("  ", "\t", "\n").isPresent());
    }

    @Test
    public void testFirstNonBlankIterable() {
        List<String> list = Arrays.asList("  ", null, "c", "d");
        Assertions.assertEquals("c", CommonUtil.firstNonBlank(list).get());

        Assertions.assertFalse(CommonUtil.firstNonBlank(Collections.<String> emptyList()).isPresent());

        Assertions.assertFalse(CommonUtil.firstNonBlank((Iterable<String>) null).isPresent());

        List<String> allBlank = Arrays.asList("  ", "\t", null);
        Assertions.assertFalse(CommonUtil.firstNonBlank(allBlank).isPresent());
    }

    @Test
    public void testFirstNonBlank2() {
        assertEquals("abc", CommonUtil.firstNonBlank("abc", "def").get());
        assertEquals("def", CommonUtil.firstNonBlank("  ", "def").get());
        assertEquals("def", CommonUtil.firstNonBlank(null, "def").get());
        assertFalse(CommonUtil.firstNonBlank("  ", "  ").isPresent());
        assertFalse(CommonUtil.firstNonBlank((String) null, (String) null).isPresent());
    }

    @Test
    public void testFirstNonBlank3() {
        assertEquals("abc", CommonUtil.firstNonBlank("abc", "def", "ghi").get());
        assertEquals("def", CommonUtil.firstNonBlank("  ", "def", "ghi").get());
        assertEquals("ghi", CommonUtil.firstNonBlank("  ", "  ", "ghi").get());
        assertFalse(CommonUtil.firstNonBlank("  ", null, "  ").isPresent());
    }

    @Test
    public void testFirstNonBlankOrDefault_Array() {
        String[] arr = { "  ", "", null, "found" };
        assertEquals("found", CommonUtil.firstNonBlankOrDefault(arr, "default"));
        String[] allBlank = { "  ", "" };
        assertEquals("default", CommonUtil.firstNonBlankOrDefault(allBlank, "default"));
        assertEquals("default", CommonUtil.firstNonBlankOrDefault((String[]) null, "default"));
    }

    @Test
    public void testFirstNonBlankOrDefault_Iterable() {
        List<String> list = Arrays.asList("  ", "", null, "found");
        assertEquals("found", CommonUtil.firstNonBlankOrDefault(list, "default"));
        List<String> allBlank = Arrays.asList("  ", "");
        assertEquals("default", CommonUtil.firstNonBlankOrDefault(allBlank, "default"));
        assertEquals("default", CommonUtil.firstNonBlankOrDefault((Iterable<String>) null, "default"));
    }

    @Test
    public void testFirstEntry() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        com.landawn.abacus.util.u.Optional<Map.Entry<String, Integer>> entry = CommonUtil.firstEntry(map);
        assertTrue(entry.isPresent());
        assertEquals("a", entry.get().getKey());
        assertEquals(Integer.valueOf(1), entry.get().getValue());
    }

    @Test
    public void testFirstOrNullIfEmpty() {
        List<String> list = Arrays.asList("a", "b");
        assertEquals("a", CommonUtil.firstOrNullIfEmpty(list));
        assertNull(CommonUtil.firstOrNullIfEmpty(new ArrayList<>()));
    }

    @Test
    public void testFirstOrNullIfEmptyArray() {
        String[] arr = { "a", "b", "c" };
        Assertions.assertEquals("a", CommonUtil.firstOrNullIfEmpty(arr));

        Assertions.assertNull(CommonUtil.firstOrNullIfEmpty(new String[] {}));

        Assertions.assertNull(CommonUtil.firstOrNullIfEmpty((String[]) null));
    }

    @Test
    public void testFirstOrNullIfEmptyIterable() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("a", CommonUtil.firstOrNullIfEmpty(list));

        Assertions.assertNull(CommonUtil.firstOrNullIfEmpty(Collections.emptyList()));

        Assertions.assertNull(CommonUtil.firstOrNullIfEmpty((Iterable<String>) null));
    }

    @Test
    public void testFirstOrNullIfEmptyIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("a", CommonUtil.firstOrNullIfEmpty(list.iterator()));

        Assertions.assertNull(CommonUtil.firstOrNullIfEmpty(Collections.emptyIterator()));

        Assertions.assertNull(CommonUtil.firstOrNullIfEmpty((Iterator<String>) null));
    }

    @Test
    public void testFirstOrDefaultIfEmpty() {
        List<String> list = Arrays.asList("a", "b");
        assertEquals("a", CommonUtil.firstOrDefaultIfEmpty(list, "default"));
        assertEquals("default", CommonUtil.firstOrDefaultIfEmpty(new ArrayList<>(), "default"));
    }

    @Test
    public void testFirstOrDefaultIfEmptyArray() {
        String[] arr = { "a", "b", "c" };
        Assertions.assertEquals("a", CommonUtil.firstOrDefaultIfEmpty(arr, "default"));

        Assertions.assertEquals("default", CommonUtil.firstOrDefaultIfEmpty(new String[] {}, "default"));

        Assertions.assertEquals("default", CommonUtil.firstOrDefaultIfEmpty((String[]) null, "default"));
    }

    @Test
    public void testFirstOrDefaultIfEmptyIterable() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("a", CommonUtil.firstOrDefaultIfEmpty(list, "default"));

        Assertions.assertEquals("default", CommonUtil.firstOrDefaultIfEmpty(Collections.emptyList(), "default"));

        Assertions.assertEquals("default", CommonUtil.firstOrDefaultIfEmpty((Iterable<String>) null, "default"));

        ArrayList<String> arrayList = new ArrayList<>(list);
        Assertions.assertEquals("a", CommonUtil.firstOrDefaultIfEmpty(arrayList, "default"));
    }

    @Test
    public void testFirstOrDefaultIfEmptyIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("a", CommonUtil.firstOrDefaultIfEmpty(list.iterator(), "default"));

        Assertions.assertEquals("default", CommonUtil.firstOrDefaultIfEmpty(Collections.emptyIterator(), "default"));

        Assertions.assertEquals("default", CommonUtil.firstOrDefaultIfEmpty((Iterator<String>) null, "default"));
    }
}
