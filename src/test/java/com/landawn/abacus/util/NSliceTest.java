package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class NSliceTest extends NTestSupport {

    @Test
    public void testSlice_array() {
        String[] arr = { "a", "b", "c", "d", "e" };
        assertIterableEquals(Arrays.asList("b", "c", "d"), CommonUtil.slice(arr, 1, 4));
        assertIterableEquals(Arrays.asList("a", "b", "c", "d", "e"), CommonUtil.slice(arr, 0, 5));
        assertEquals(List.of(42), CommonUtil.slice(new Integer[] { 42 }, 0, 1));
        assertTrue(CommonUtil.slice(arr, 1, 1).isEmpty());
        assertTrue(CommonUtil.slice(new String[] {}, 0, 0).isEmpty());
        assertTrue(CommonUtil.slice((String[]) null, 0, 0).isEmpty());
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.slice(arr, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.slice(arr, 0, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.slice(arr, 3, 1));
    }

    @Test
    public void testSlice_list() {
        List<String> list = Arrays.asList("a", "b", "c", "d");
        assertIterableEquals(Arrays.asList("b", "c"), CommonUtil.slice(list, 1, 3));
        assertIterableEquals(Arrays.asList("a", "b", "c", "d"), CommonUtil.slice(list, 0, 4));
        assertTrue(CommonUtil.slice(list, 1, 1).isEmpty());
        assertTrue(CommonUtil.slice(Collections.<String> emptyList(), 0, 0).isEmpty());
        assertTrue(CommonUtil.slice((List<String>) null, 0, 0).isEmpty());
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.slice(list, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.slice(list, 0, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.slice(list, 3, 1));
    }

    @Test
    public void testSlice_collection() {
        Collection<String> coll = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d"));
        assertIterableEquals(Arrays.asList("b", "c"), CommonUtil.slice(coll, 1, 3));
        assertIterableEquals(Arrays.asList("a", "b", "c", "d"), CommonUtil.slice(coll, 0, 4));
        assertTrue(CommonUtil.slice(coll, 1, 1).isEmpty());
        assertTrue(CommonUtil.slice(Collections.<String> emptySet(), 0, 0).isEmpty());
        assertTrue(CommonUtil.slice((Collection<String>) null, 0, 0).isEmpty());
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.slice(coll, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.slice(coll, 0, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.slice(coll, 3, 1));

        List<String> list = Arrays.asList("a", "b", "c", "d");
        assertTrue(CommonUtil.slice((Collection<String>) list, 1, 3) instanceof ImmutableList);
        assertIterableEquals(Arrays.asList("b", "c"), CommonUtil.slice((Collection<String>) list, 1, 3));

        Set<String> words = new HashSet<>(Arrays.asList("a", "b", "c", "d", "e"));
        assertEquals(3, CommonUtil.slice(words, 0, 3).size());
    }

    @Test
    public void testSlice_iterator() {
        assertIterableEquals(Arrays.asList("b", "c"), iteratorToList(CommonUtil.slice(Arrays.asList("a", "b", "c", "d").iterator(), 1, 3)));
        assertIterableEquals(Arrays.asList("a", "b", "c", "d"), iteratorToList(CommonUtil.slice(Arrays.asList("a", "b", "c", "d").iterator(), 0, 4)));
        assertTrue(iteratorToList(CommonUtil.slice(Arrays.asList("a", "b", "c", "d").iterator(), 1, 1)).isEmpty());
        assertTrue(iteratorToList(CommonUtil.slice(Collections.<String> emptyIterator(), 0, 0)).isEmpty());
        assertTrue(iteratorToList(CommonUtil.slice((Iterator<String>) null, 0, 0)).isEmpty());
        assertIterableEquals(Arrays.asList("a"), iteratorToList(CommonUtil.slice(Arrays.asList("a", "b").iterator(), 0, 1)));
        assertIterableEquals(Arrays.asList("b"), iteratorToList(CommonUtil.slice(Arrays.asList("a", "b").iterator(), 1, 2)));
        assertTrue(iteratorToList(CommonUtil.slice(Arrays.asList("a", "b").iterator(), 2, 2)).isEmpty());
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.slice(Arrays.asList("a").iterator(), -1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.slice(Arrays.asList("a").iterator(), 1, 0));
    }
}
