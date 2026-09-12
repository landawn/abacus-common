package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class CommonUtilIndicesTest extends CommonUtilTestSupport {

    @Test
    public void testIndicesOfAll() {
        assertArrayEquals(new int[] { 1, 3, 5 }, CommonUtil.indicesOfAll(new String[] { "a", "b", "c", "b", "d", "b" }, "b"));
        assertArrayEquals(new int[] {}, CommonUtil.indicesOfAll(new String[] { "a", "b" }, "z"));
        assertArrayEquals(new int[] { 1, 3 }, CommonUtil.indicesOfAll(new String[] { "a", null, "b", null }, (String) null));
        assertArrayEquals(new int[] {}, CommonUtil.indicesOfAll(new String[0], "a"));
        assertArrayEquals(new int[] { 3, 5 }, CommonUtil.indicesOfAll(new String[] { "a", "b", "c", "b", "d", "b" }, "b", 2));
        assertArrayEquals(new int[] {}, CommonUtil.indicesOfAll(new String[] { "a", "b", "c", "b" }, "b", 10));
        assertArrayEquals(new int[] { 1, 3, 5 }, CommonUtil.indicesOfAll(new String[] { "a", "b", "c", "b", "d", "b" }, "b", -1));

        String[] fruits = { "apple", "banana", "apricot", "cherry", "avocado" };
        assertArrayEquals(new int[] { 0, 2, 4 }, CommonUtil.indicesOfAll(fruits, s -> s.startsWith("a")));
        assertArrayEquals(new int[] { 2, 4 }, CommonUtil.indicesOfAll(fruits, s -> s.startsWith("a"), 1));
        assertArrayEquals(new int[] {}, CommonUtil.indicesOfAll(fruits, s -> s.startsWith("a"), 10));
        assertArrayEquals(new int[] {}, CommonUtil.indicesOfAll(new String[0], s -> true));

        List<String> list = Arrays.asList("a", "b", "c", "b", "d", "b");
        assertArrayEquals(new int[] { 1, 3, 5 }, CommonUtil.indicesOfAll(list, "b"));
        assertArrayEquals(new int[] {}, CommonUtil.indicesOfAll(new ArrayList<>(), "a"));
        assertArrayEquals(new int[] { 1, 3, 5 }, CommonUtil.indicesOfAll(new LinkedList<>(list), "b"));
        assertArrayEquals(new int[] { 3, 5 }, CommonUtil.indicesOfAll(list, "b", 2));
        assertArrayEquals(new int[] {}, CommonUtil.indicesOfAll(list, "b", 10));
        assertArrayEquals(new int[] {}, CommonUtil.indicesOfAll(Arrays.asList(1, 2, 3, 4), 9, 0));

        LinkedList<String> linked = new LinkedList<>(Arrays.asList("a", "b", "a", "c", "a"));
        assertArrayEquals(new int[] { 2, 4 }, CommonUtil.indicesOfAll(linked, "a", 1));

        List<String> words = Arrays.asList("apple", "banana", "apricot", "cherry", "avocado");
        assertArrayEquals(new int[] { 0, 2, 4 }, CommonUtil.indicesOfAll(words, s -> s.startsWith("a")));
        assertArrayEquals(new int[] { 2, 4 }, CommonUtil.indicesOfAll(words, s -> s.startsWith("a"), 1));
        assertArrayEquals(new int[] { 0, 2, 4 }, CommonUtil.indicesOfAll(new LinkedList<>(words), s -> s.startsWith("a")));
        assertArrayEquals(new int[] {}, CommonUtil.indicesOfAll(new ArrayList<String>(), s -> true));
        assertArrayEquals(new int[] { 3, 5 }, CommonUtil.indicesOfAll(Arrays.asList(1, 4, 3, 6, 5, 8), n -> n % 2 == 0, 2));
        assertArrayEquals(new int[] { 2, 3 },
                CommonUtil.indicesOfAll(new LinkedList<>(Arrays.asList("ab", "cd", "ef", "gh")), s -> s.startsWith("e") || s.startsWith("g"), 1));
        assertArrayEquals(new int[] {}, CommonUtil.indicesOfAll(Arrays.asList(1, 3, 5, 7), n -> n % 2 == 0, 0));
    }

    @Test
    public void testIndicesOfMinMax() {
        assertArrayEquals(new int[] { 1, 3 }, CommonUtil.indicesOfMin(Arrays.asList(5, 1, 3, 1, 2)));
        assertArrayEquals(new int[] { 1, 3, 5 }, CommonUtil.indicesOfMin(new Integer[] { 3, 1, 4, 1, 5, 1 }));
        assertArrayEquals(new int[] { 0 }, CommonUtil.indicesOfMin(new Integer[] { 5 }));
        assertArrayEquals(new int[] {}, CommonUtil.indicesOfMin(new Integer[0]));
        assertArrayEquals(new int[] { 1, 3, 5 }, CommonUtil.indicesOfMin(Arrays.asList(3, 1, 4, 1, 5, 1)));
        assertArrayEquals(new int[] {}, CommonUtil.indicesOfMin(new ArrayList<Integer>()));
        assertArrayEquals(new int[] { 1, 3, 5 },
                CommonUtil.indicesOfMin(new String[] { "cat", "a", "dog", "a", "bird", "a" }, Comparator.comparing(String::length)));
        assertArrayEquals(new int[] { 1, 3, 5 },
                CommonUtil.indicesOfMin(Arrays.asList("cat", "a", "dog", "a", "bird", "a"), Comparator.comparing(String::length)));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.indicesOfMin(new String[] { null, "a" }, null));

        assertArrayEquals(new int[] { 1, 3 }, CommonUtil.indicesOfMax(Arrays.asList(1, 5, 3, 5, 2)));
        assertArrayEquals(new int[] { 1, 3, 5 }, CommonUtil.indicesOfMax(new Integer[] { 3, 5, 4, 5, 1, 5 }));
        assertArrayEquals(new int[] { 0 }, CommonUtil.indicesOfMax(new Integer[] { 5 }));
        assertArrayEquals(new int[] {}, CommonUtil.indicesOfMax(new Integer[0]));
        assertArrayEquals(new int[] { 1, 3, 5 }, CommonUtil.indicesOfMax(Arrays.asList(3, 5, 4, 5, 1, 5)));
        assertArrayEquals(new int[] {}, CommonUtil.indicesOfMax(new ArrayList<Integer>()));
        assertArrayEquals(new int[] { 1, 3, 5 },
                CommonUtil.indicesOfMax(new String[] { "a", "cat", "b", "dog", "c", "dog" }, Comparator.comparing(String::length)));
        assertArrayEquals(new int[] { 1, 3, 5 },
                CommonUtil.indicesOfMax(Arrays.asList("a", "cat", "b", "dog", "c", "dog"), Comparator.comparing(String::length)));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.indicesOfMax(new String[] { "a", null }, null));

        Integer[] withNull = { 2, null, 1 };
        assertArrayEquals(new int[] { 2 }, CommonUtil.indicesOfMin(withNull));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.indicesOfMin(withNull, null));
        assertArrayEquals(new int[] { 2 }, CommonUtil.indicesOfMin(Arrays.asList(2, null, 1)));
        assertArrayEquals(new int[] { 0 }, CommonUtil.indicesOfMax(new Integer[] { 5, null, 1 }));
        assertArrayEquals(new int[] { 1, 3 }, CommonUtil.indicesOfMin(new Integer[] { 3, 1, 4, 1, 5 }));
    }
}
