package com.landawn.abacus.util;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class N2025Test extends TestBase {

    @Test
    public void testMergeTwoArrays() {
        Integer[] a = { 1, 3, 5, 7 };
        Integer[] b = { 2, 4, 6, 8 };

        BiFunction<Integer, Integer, MergeResult> selector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Integer> result = N.merge(a, b, selector);

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8), result);
    }

    @Test
    public void testMergeTwoArraysFirstNull() {
        Integer[] a = null;
        Integer[] b = { 1, 2, 3 };

        BiFunction<Integer, Integer, MergeResult> selector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Integer> result = N.merge(a, b, selector);

        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testMergeTwoArraysSecondNull() {
        Integer[] a = { 1, 2, 3 };
        Integer[] b = null;

        BiFunction<Integer, Integer, MergeResult> selector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Integer> result = N.merge(a, b, selector);

        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testMergeTwoArraysBothNull() {
        Integer[] a = null;
        Integer[] b = null;

        BiFunction<Integer, Integer, MergeResult> selector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Integer> result = N.merge(a, b, selector);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testMergeTwoArraysBothEmpty() {
        Integer[] a = {};
        Integer[] b = {};

        BiFunction<Integer, Integer, MergeResult> selector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Integer> result = N.merge(a, b, selector);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testMergeTwoIterables() {
        List<Integer> a = Arrays.asList(1, 3, 5);
        List<Integer> b = Arrays.asList(2, 4, 6);

        BiFunction<Integer, Integer, MergeResult> selector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Integer> result = N.merge(a, b, selector);

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testMergeTwoIterablesNullInputs() {
        List<Integer> a = null;
        List<Integer> b = Arrays.asList(1, 2, 3);

        BiFunction<Integer, Integer, MergeResult> selector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Integer> result = N.merge(a, b, selector);

        assertNotNull(result);
    }

    @Test
    public void testMergeCollectionOfIterables() {
        List<List<Integer>> lists = Arrays.asList(Arrays.asList(1, 4, 7), Arrays.asList(2, 5, 8), Arrays.asList(3, 6, 9));

        BiFunction<Integer, Integer, MergeResult> selector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Integer> result = N.merge(lists, selector);

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), result);
    }

    @Test
    public void testMergeCollectionOfIterablesEmpty() {
        List<List<Integer>> lists = new ArrayList<>();

        BiFunction<Integer, Integer, MergeResult> selector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Integer> result = N.merge(lists, selector);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testMergeCollectionOfIterablesNull() {
        List<List<Integer>> lists = null;

        BiFunction<Integer, Integer, MergeResult> selector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Integer> result = N.merge(lists, selector);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testMergeCollectionOfIterablesWithSupplier() {
        List<List<Integer>> lists = Arrays.asList(Arrays.asList(1, 4), Arrays.asList(2, 5), Arrays.asList(3, 6));

        BiFunction<Integer, Integer, MergeResult> selector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        Set<Integer> result = N.merge(lists, selector, HashSet::new);

        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3, 4, 5, 6)), result);
    }

    @Test
    public void testZipTwoArrays() {
        String[] a = { "a", "b", "c" };
        Integer[] b = { 1, 2, 3 };

        List<Pair<String, Integer>> result = N.zip(a, b, Pair::of);

        assertEquals(3, result.size());
        assertEquals("a", result.get(0).left());
        assertEquals(1, result.get(0).right());
        assertEquals("b", result.get(1).left());
        assertEquals(2, result.get(1).right());
        assertEquals("c", result.get(2).left());
        assertEquals(3, result.get(2).right());
    }

    @Test
    public void testZipTwoArraysDifferentLengths() {
        String[] a = { "a", "b", "c", "d", "e" };
        Integer[] b = { 1, 2, 3 };

        List<Pair<String, Integer>> result = N.zip(a, b, Pair::of);

        assertEquals(3, result.size());
        assertEquals("a", result.get(0).left());
        assertEquals(1, result.get(0).right());
    }

    @Test
    public void testZipTwoArraysFirstNull() {
        String[] a = null;
        Integer[] b = { 1, 2, 3 };

        List<Pair<String, Integer>> result = N.zip(a, b, Pair::of);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testZipTwoArraysSecondNull() {
        String[] a = { "a", "b", "c" };
        Integer[] b = null;

        List<Pair<String, Integer>> result = N.zip(a, b, Pair::of);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testZipTwoArraysBothNull() {
        String[] a = null;
        Integer[] b = null;

        List<Pair<String, Integer>> result = N.zip(a, b, Pair::of);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testZipTwoArraysEmpty() {
        String[] a = {};
        Integer[] b = {};

        List<Pair<String, Integer>> result = N.zip(a, b, Pair::of);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testZipTwoIterables() {
        List<String> a = Arrays.asList("a", "b", "c");
        List<Integer> b = Arrays.asList(1, 2, 3);

        List<Pair<String, Integer>> result = N.zip(a, b, Pair::of);

        assertEquals(3, result.size());
        assertEquals("a", result.get(0).left());
        assertEquals(1, result.get(0).right());
    }

    @Test
    public void testZipTwoIterablesNull() {
        List<String> a = null;
        List<Integer> b = Arrays.asList(1, 2, 3);

        List<Pair<String, Integer>> result = N.zip(a, b, Pair::of);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testZipTwoIterablesEmpty() {
        List<String> a = new ArrayList<>();
        List<Integer> b = new ArrayList<>();

        List<Pair<String, Integer>> result = N.zip(a, b, Pair::of);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testZipThreeArrays() {
        String[] a = { "a", "b", "c" };
        Integer[] b = { 1, 2, 3 };
        Boolean[] c = { true, false, true };

        List<Triple<String, Integer, Boolean>> result = N.zip(a, b, c, Triple::of);

        assertEquals(3, result.size());
        assertEquals("a", result.get(0).left());
        assertEquals(1, result.get(0).middle());
        assertEquals(true, result.get(0).right());
    }

    @Test
    public void testZipThreeArraysDifferentLengths() {
        String[] a = { "a", "b", "c", "d", "e" };
        Integer[] b = { 1, 2, 3 };
        Boolean[] c = { true, false };

        List<Triple<String, Integer, Boolean>> result = N.zip(a, b, c, Triple::of);

        assertEquals(2, result.size());
    }

    @Test
    public void testZipThreeArraysOneNull() {
        String[] a = { "a", "b", "c" };
        Integer[] b = null;
        Boolean[] c = { true, false, true };

        List<Triple<String, Integer, Boolean>> result = N.zip(a, b, c, Triple::of);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testZipThreeIterables() {
        List<String> a = Arrays.asList("a", "b", "c");
        List<Integer> b = Arrays.asList(1, 2, 3);
        List<Boolean> c = Arrays.asList(true, false, true);

        List<Triple<String, Integer, Boolean>> result = N.zip(a, b, c, Triple::of);

        assertEquals(3, result.size());
        assertEquals("a", result.get(0).left());
        assertEquals(1, result.get(0).middle());
        assertEquals(true, result.get(0).right());
    }

    @Test
    public void testZipThreeIterablesOneNull() {
        List<String> a = Arrays.asList("a", "b");
        List<Integer> b = null;
        List<Boolean> c = Arrays.asList(true, false);

        List<Triple<String, Integer, Boolean>> result = N.zip(a, b, c, Triple::of);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testZipTwoArraysWithDefaults() {
        String[] a = { "a", "b", "c" };
        Integer[] b = { 1, 2, 3, 4, 5 };

        List<Pair<String, Integer>> result = N.zip(a, b, "default", 0, Pair::of);

        assertEquals(5, result.size());
        assertEquals("a", result.get(0).left());
        assertEquals(1, result.get(0).right());
        assertEquals("default", result.get(3).left());
        assertEquals(4, result.get(3).right());
        assertEquals("default", result.get(4).left());
        assertEquals(5, result.get(4).right());
    }

    @Test
    public void testZipTwoArraysWithDefaultsFirstLonger() {
        String[] a = { "a", "b", "c", "d", "e" };
        Integer[] b = { 1, 2 };

        List<Pair<String, Integer>> result = N.zip(a, b, "default", 0, Pair::of);

        assertEquals(5, result.size());
        assertEquals("a", result.get(0).left());
        assertEquals(1, result.get(0).right());
        assertEquals("c", result.get(2).left());
        assertEquals(0, result.get(2).right());
    }

    @Test
    public void testZipTwoArraysWithDefaultsBothNull() {
        String[] a = null;
        Integer[] b = null;

        List<Pair<String, Integer>> result = N.zip(a, b, "default", 0, Pair::of);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testZipTwoArraysWithDefaultsFirstNull() {
        String[] a = null;
        Integer[] b = { 1, 2, 3 };

        List<Pair<String, Integer>> result = N.zip(a, b, "default", 0, Pair::of);

        assertEquals(3, result.size());
        assertEquals("default", result.get(0).left());
        assertEquals(1, result.get(0).right());
    }

    @Test
    public void testZipTwoIterablesWithDefaults() {
        List<String> a = Arrays.asList("a", "b");
        List<Integer> b = Arrays.asList(1, 2, 3, 4);

        List<Pair<String, Integer>> result = N.zip(a, b, "default", 0, Pair::of);

        assertEquals(4, result.size());
        assertEquals("a", result.get(0).left());
        assertEquals(1, result.get(0).right());
        assertEquals("default", result.get(2).left());
        assertEquals(3, result.get(2).right());
    }

    @Test
    public void testZipTwoIterablesWithDefaultsFirstNull() {
        List<String> a = null;
        List<Integer> b = Arrays.asList(1, 2, 3);

        List<Pair<String, Integer>> result = N.zip(a, b, "default", 0, Pair::of);

        assertEquals(3, result.size());
        assertEquals("default", result.get(0).left());
        assertEquals(1, result.get(0).right());
    }

    @Test
    public void testZipThreeArraysWithDefaults() {
        String[] a = { "a", "b" };
        Integer[] b = { 1, 2, 3 };
        Boolean[] c = { true };

        List<Triple<String, Integer, Boolean>> result = N.zip(a, b, c, "default", 0, false, Triple::of);

        assertEquals(3, result.size());
        assertEquals("a", result.get(0).left());
        assertEquals(1, result.get(0).middle());
        assertEquals(true, result.get(0).right());
        assertEquals("default", result.get(2).left());
        assertEquals(3, result.get(2).middle());
        assertEquals(false, result.get(2).right());
    }

    @Test
    public void testZipThreeArraysWithDefaultsAllDifferentLengths() {
        String[] a = { "a", "b", "c", "d", "e" };
        Integer[] b = { 1, 2, 3 };
        Boolean[] c = { true };

        List<Triple<String, Integer, Boolean>> result = N.zip(a, b, c, "default", 0, false, Triple::of);

        assertEquals(5, result.size());
        assertEquals("d", result.get(3).left());
        assertEquals(0, result.get(3).middle());
        assertEquals(false, result.get(3).right());
    }

    @Test
    public void testZipThreeIterablesWithDefaults() {
        List<String> a = Arrays.asList("a", "b");
        List<Integer> b = Arrays.asList(1, 2, 3, 4);
        List<Boolean> c = Arrays.asList(true);

        List<Triple<String, Integer, Boolean>> result = N.zip(a, b, c, "default", 0, false, Triple::of);

        assertEquals(4, result.size());
        assertEquals("a", result.get(0).left());
        assertEquals(1, result.get(0).middle());
        assertEquals(true, result.get(0).right());
        assertEquals("default", result.get(3).left());
        assertEquals(4, result.get(3).middle());
        assertEquals(false, result.get(3).right());
    }

    @Test
    public void testZipTwoArraysToArray() {
        String[] a = { "a", "b", "c" };
        Integer[] b = { 1, 2, 3 };

        Pair<String, Integer>[] result = N.zip(a, b, Pair::of, Pair.class);

        assertEquals(3, result.length);
        assertEquals("a", result[0].left());
        assertEquals(1, result[0].right());
        assertEquals("b", result[1].left());
        assertEquals(2, result[1].right());
    }

    @Test
    public void testZipTwoArraysToArrayDifferentLengths() {
        String[] a = { "a", "b", "c", "d", "e" };
        Integer[] b = { 1, 2 };

        Pair<String, Integer>[] result = N.zip(a, b, Pair::of, Pair.class);

        assertEquals(2, result.length);
    }

    @Test
    public void testZipTwoArraysToArrayFirstNull() {
        String[] a = null;
        Integer[] b = { 1, 2, 3 };

        Pair<String, Integer>[] result = N.zip(a, b, Pair::of, Pair.class);

        assertEquals(0, result.length);
    }

    @Test
    public void testZipTwoArraysToArrayWithDefaults() {
        String[] a = { "a", "b" };
        Integer[] b = { 1, 2, 3, 4 };

        Pair<String, Integer>[] result = N.zip(a, b, "default", 0, Pair::of, Pair.class);

        assertEquals(4, result.length);
        assertEquals("a", result[0].left());
        assertEquals(1, result[0].right());
        assertEquals("default", result[2].left());
        assertEquals(3, result[2].right());
    }

    @Test
    public void testZipThreeArraysToArray() {
        String[] a = { "a", "b", "c" };
        Integer[] b = { 1, 2, 3 };
        Boolean[] c = { true, false, true };

        Triple<String, Integer, Boolean>[] result = N.zip(a, b, c, Triple::of, Triple.class);

        assertEquals(3, result.length);
        assertEquals("a", result[0].left());
        assertEquals(1, result[0].middle());
        assertEquals(true, result[0].right());
    }

    @Test
    public void testZipThreeArraysToArrayDifferentLengths() {
        String[] a = { "a", "b", "c", "d", "e" };
        Integer[] b = { 1, 2, 3 };
        Boolean[] c = { true };

        Triple<String, Integer, Boolean>[] result = N.zip(a, b, c, Triple::of, Triple.class);

        assertEquals(1, result.length);
    }

    @Test
    public void testZipThreeArraysToArrayWithDefaults() {
        String[] a = { "a", "b" };
        Integer[] b = { 1, 2, 3 };
        Boolean[] c = { true };

        Triple<String, Integer, Boolean>[] result = N.zip(a, b, c, "default", 0, false, Triple::of, Triple.class);

        assertEquals(3, result.length);
        assertEquals("a", result[0].left());
        assertEquals(1, result[0].middle());
        assertEquals(true, result[0].right());
        assertEquals("default", result[2].left());
        assertEquals(3, result[2].middle());
        assertEquals(false, result[2].right());
    }

    @Test
    public void testUnzipIterable() {
        List<Pair<String, Integer>> pairs = Arrays.asList(Pair.of("a", 1), Pair.of("b", 2), Pair.of("c", 3));

        Pair<List<String>, List<Integer>> result = N.unzip(pairs, (pair, output) -> {
            output.setLeft(pair.left());
            output.setRight(pair.right());
        });

        assertEquals(Arrays.asList("a", "b", "c"), result.left());
        assertEquals(Arrays.asList(1, 2, 3), result.right());
    }

    @Test
    public void testUnzipIterableNull() {
        List<Pair<String, Integer>> pairs = null;

        Pair<List<String>, List<Integer>> result = N.unzip(pairs, (pair, output) -> {
            output.setLeft(pair.left());
            output.setRight(pair.right());
        });

        assertTrue(result.left().isEmpty());
        assertTrue(result.right().isEmpty());
    }

    @Test
    public void testUnzipIterableEmpty() {
        List<Pair<String, Integer>> pairs = new ArrayList<>();

        Pair<List<String>, List<Integer>> result = N.unzip(pairs, (pair, output) -> {
            output.setLeft(pair.left());
            output.setRight(pair.right());
        });

        assertTrue(result.left().isEmpty());
        assertTrue(result.right().isEmpty());
    }

    @Test
    public void testUnzipIterableWithSupplier() {
        List<Pair<String, Integer>> pairs = Arrays.asList(Pair.of("a", 1), Pair.of("b", 2), Pair.of("c", 3));

        Pair<Set<String>, Set<Integer>> result = N.unzip(pairs, (pair, output) -> {
            output.setLeft(pair.left());
            output.setRight(pair.right());
        }, HashSet::new);

        assertEquals(new HashSet<>(Arrays.asList("a", "b", "c")), result.left());
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), result.right());
    }

    @Test
    public void testUnzipIterableComplexObjects() {
        List<String> strings = Arrays.asList("a:1", "b:2", "c:3");

        Pair<List<String>, List<Integer>> result = N.unzip(strings, (str, output) -> {
            String[] parts = str.split(":");
            output.setLeft(parts[0]);
            output.setRight(Integer.parseInt(parts[1]));
        });

        assertEquals(Arrays.asList("a", "b", "c"), result.left());
        assertEquals(Arrays.asList(1, 2, 3), result.right());
    }

    @Test
    public void testUnzippIterable() {
        List<Triple<String, Integer, Boolean>> triples = Arrays.asList(Triple.of("a", 1, true), Triple.of("b", 2, false), Triple.of("c", 3, true));

        Triple<List<String>, List<Integer>, List<Boolean>> result = N.unzipp(triples, (triple, output) -> {
            output.setLeft(triple.left());
            output.setMiddle(triple.middle());
            output.setRight(triple.right());
        });

        assertEquals(Arrays.asList("a", "b", "c"), result.left());
        assertEquals(Arrays.asList(1, 2, 3), result.middle());
        assertEquals(Arrays.asList(true, false, true), result.right());
    }

    @Test
    public void testUnzippIterableNull() {
        List<Triple<String, Integer, Boolean>> triples = null;

        Triple<List<String>, List<Integer>, List<Boolean>> result = N.unzipp(triples, (triple, output) -> {
            output.setLeft(triple.left());
            output.setMiddle(triple.middle());
            output.setRight(triple.right());
        });

        assertTrue(result.left().isEmpty());
        assertTrue(result.middle().isEmpty());
        assertTrue(result.right().isEmpty());
    }

    @Test
    public void testUnzippIterableEmpty() {
        List<Triple<String, Integer, Boolean>> triples = new ArrayList<>();

        Triple<List<String>, List<Integer>, List<Boolean>> result = N.unzipp(triples, (triple, output) -> {
            output.setLeft(triple.left());
            output.setMiddle(triple.middle());
            output.setRight(triple.right());
        });

        assertTrue(result.left().isEmpty());
        assertTrue(result.middle().isEmpty());
        assertTrue(result.right().isEmpty());
    }

    @Test
    public void testUnzippIterableWithSupplier() {
        List<Triple<String, Integer, Boolean>> triples = Arrays.asList(Triple.of("a", 1, true), Triple.of("b", 2, false));

        Triple<Set<String>, Set<Integer>, Set<Boolean>> result = N.unzipp(triples, (triple, output) -> {
            output.setLeft(triple.left());
            output.setMiddle(triple.middle());
            output.setRight(triple.right());
        }, HashSet::new);

        assertEquals(new HashSet<>(Arrays.asList("a", "b")), result.left());
        assertEquals(new HashSet<>(Arrays.asList(1, 2)), result.middle());
        assertEquals(new HashSet<>(Arrays.asList(true, false)), result.right());
    }

    @Test
    public void testZipWithFunctionReturningNull() {
        String[] a = { "a", "b", "c" };
        Integer[] b = { 1, 2, 3 };

        BiFunction<String, Integer, Pair<String, Integer>> zipFunc = (s, i) -> i == 2 ? null : Pair.of(s, i);
        List<Pair<String, Integer>> result = N.zip(a, b, zipFunc);

        assertEquals(3, result.size());
        assertEquals("a", result.get(0).left());
        assertEquals(1, result.get(0).right());
        assertEquals(null, result.get(1));
        assertEquals("c", result.get(2).left());
        assertEquals(3, result.get(2).right());
    }

    @Test
    public void testMergeWithAlwaysTakeFirst() {
        Integer[] a = { 1, 2, 3 };
        Integer[] b = { 4, 5, 6 };

        BiFunction<Integer, Integer, MergeResult> selector = (x, y) -> MergeResult.TAKE_FIRST;
        List<Integer> result = N.merge(a, b, selector);

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testMergeWithAlwaysTakeSecond() {
        Integer[] a = { 1, 2, 3 };
        Integer[] b = { 4, 5, 6 };

        BiFunction<Integer, Integer, MergeResult> selector = (x, y) -> MergeResult.TAKE_SECOND;
        List<Integer> result = N.merge(a, b, selector);

        assertEquals(Arrays.asList(4, 5, 6, 1, 2, 3), result);
    }

    @Test
    public void testZipTwoIterablesFirstLonger() {
        List<String> a = Arrays.asList("a", "b", "c", "d", "e");
        List<Integer> b = Arrays.asList(1, 2);

        List<Pair<String, Integer>> result = N.zip(a, b, Pair::of);

        assertEquals(2, result.size());
        assertEquals("a", result.get(0).left());
        assertEquals(1, result.get(0).right());
        assertEquals("b", result.get(1).left());
        assertEquals(2, result.get(1).right());
    }

    @Test
    public void testZipTwoIterablesSecondLonger() {
        List<String> a = Arrays.asList("a", "b");
        List<Integer> b = Arrays.asList(1, 2, 3, 4, 5);

        List<Pair<String, Integer>> result = N.zip(a, b, Pair::of);

        assertEquals(2, result.size());
    }

    @Test
    public void testZipThreeIterablesAllDifferentLengths() {
        List<String> a = Arrays.asList("a", "b", "c", "d", "e");
        List<Integer> b = Arrays.asList(1, 2, 3);
        List<Boolean> c = Arrays.asList(true);

        List<Triple<String, Integer, Boolean>> result = N.zip(a, b, c, Triple::of);

        assertEquals(1, result.size());
        assertEquals("a", result.get(0).left());
        assertEquals(1, result.get(0).middle());
        assertEquals(true, result.get(0).right());
    }

    @Test
    public void testUnzipWithNullElements() {
        List<Pair<String, Integer>> pairs = Arrays.asList(Pair.of("a", 1), null, Pair.of("c", 3));

        assertThrows(NullPointerException.class, () -> {
            N.unzip(pairs, (pair, output) -> {
                output.setLeft(pair.left());
                output.setRight(pair.right());
            });
        });
    }

    @Test
    public void testZipTwoArraysWithDefaultsSameLength() {
        String[] a = { "a", "b", "c" };
        Integer[] b = { 1, 2, 3 };

        List<Pair<String, Integer>> result = N.zip(a, b, "default", 0, Pair::of);

        assertEquals(3, result.size());
        assertEquals("a", result.get(0).left());
        assertEquals(1, result.get(0).right());
        assertEquals("b", result.get(1).left());
        assertEquals(2, result.get(1).right());
        assertEquals("c", result.get(2).left());
        assertEquals(3, result.get(2).right());
    }

    @Test
    public void testZipThreeArraysWithDefaultsSameLength() {
        String[] a = { "a", "b" };
        Integer[] b = { 1, 2 };
        Boolean[] c = { true, false };

        List<Triple<String, Integer, Boolean>> result = N.zip(a, b, c, "default", 0, false, Triple::of);

        assertEquals(2, result.size());
        assertEquals("a", result.get(0).left());
        assertEquals(1, result.get(0).middle());
        assertEquals(true, result.get(0).right());
    }

    @Test
    public void testOccurrencesOfBooleanArray() {
        boolean[] flags = { true, false, true, true, false };
        assertEquals(3, N.occurrencesOf(flags, true));
        assertEquals(2, N.occurrencesOf(flags, false));
    }

    @Test
    public void testOccurrencesOfBooleanArrayEmpty() {
        boolean[] empty = {};
        assertEquals(0, N.occurrencesOf(empty, true));
        assertEquals(0, N.occurrencesOf(empty, false));
    }

    @Test
    public void testOccurrencesOfBooleanArrayNull() {
        boolean[] nullArray = null;
        assertEquals(0, N.occurrencesOf(nullArray, true));
        assertEquals(0, N.occurrencesOf(nullArray, false));
    }

    @Test
    public void testOccurrencesOfBooleanArrayAllSame() {
        boolean[] allTrue = { true, true, true };
        assertEquals(3, N.occurrencesOf(allTrue, true));
        assertEquals(0, N.occurrencesOf(allTrue, false));

        boolean[] allFalse = { false, false, false, false };
        assertEquals(0, N.occurrencesOf(allFalse, true));
        assertEquals(4, N.occurrencesOf(allFalse, false));
    }

    @Test
    public void testOccurrencesOfCharArray() {
        char[] letters = { 'a', 'b', 'c', 'a', 'b', 'a' };
        assertEquals(3, N.occurrencesOf(letters, 'a'));
        assertEquals(2, N.occurrencesOf(letters, 'b'));
        assertEquals(1, N.occurrencesOf(letters, 'c'));
        assertEquals(0, N.occurrencesOf(letters, 'z'));
    }

    @Test
    public void testOccurrencesOfCharArrayEmpty() {
        char[] empty = {};
        assertEquals(0, N.occurrencesOf(empty, 'x'));
    }

    @Test
    public void testOccurrencesOfCharArrayNull() {
        char[] nullArray = null;
        assertEquals(0, N.occurrencesOf(nullArray, 'y'));
    }

    @Test
    public void testOccurrencesOfCharArraySingleElement() {
        char[] single = { 'a' };
        assertEquals(1, N.occurrencesOf(single, 'a'));
        assertEquals(0, N.occurrencesOf(single, 'b'));
    }

    @Test
    public void testOccurrencesOfByteArray() {
        byte[] data = { 1, 2, 3, 1, 2, 1 };
        assertEquals(3, N.occurrencesOf(data, (byte) 1));
        assertEquals(2, N.occurrencesOf(data, (byte) 2));
        assertEquals(1, N.occurrencesOf(data, (byte) 3));
        assertEquals(0, N.occurrencesOf(data, (byte) 0));
    }

    @Test
    public void testOccurrencesOfByteArrayEmpty() {
        byte[] empty = {};
        assertEquals(0, N.occurrencesOf(empty, (byte) 5));
    }

    @Test
    public void testOccurrencesOfByteArrayNull() {
        byte[] nullArray = null;
        assertEquals(0, N.occurrencesOf(nullArray, (byte) 1));
    }

    @Test
    public void testOccurrencesOfByteArrayNegativeValues() {
        byte[] negatives = { -1, -2, -1, 0, -1 };
        assertEquals(3, N.occurrencesOf(negatives, (byte) -1));
        assertEquals(1, N.occurrencesOf(negatives, (byte) -2));
        assertEquals(1, N.occurrencesOf(negatives, (byte) 0));
    }

    @Test
    public void testOccurrencesOfShortArray() {
        short[] values = { 10, 20, 30, 10, 20, 10 };
        assertEquals(3, N.occurrencesOf(values, (short) 10));
        assertEquals(2, N.occurrencesOf(values, (short) 20));
        assertEquals(1, N.occurrencesOf(values, (short) 30));
        assertEquals(0, N.occurrencesOf(values, (short) 0));
    }

    @Test
    public void testOccurrencesOfShortArrayEmpty() {
        short[] empty = {};
        assertEquals(0, N.occurrencesOf(empty, (short) 5));
    }

    @Test
    public void testOccurrencesOfShortArrayNull() {
        short[] nullArray = null;
        assertEquals(0, N.occurrencesOf(nullArray, (short) 10));
    }

    @Test
    public void testOccurrencesOfIntArray() {
        int[] numbers = { 1, 2, 3, 2, 4, 2, 5 };
        assertEquals(3, N.occurrencesOf(numbers, 2));
        assertEquals(1, N.occurrencesOf(numbers, 1));
        assertEquals(0, N.occurrencesOf(numbers, 10));
    }

    @Test
    public void testOccurrencesOfIntArrayEmpty() {
        int[] empty = {};
        assertEquals(0, N.occurrencesOf(empty, 1));
    }

    @Test
    public void testOccurrencesOfIntArrayNull() {
        int[] nullArray = null;
        assertEquals(0, N.occurrencesOf(nullArray, 1));
    }

    @Test
    public void testOccurrencesOfIntArrayLargeNumbers() {
        int[] large = { 1000000, 2000000, 1000000, 3000000 };
        assertEquals(2, N.occurrencesOf(large, 1000000));
        assertEquals(1, N.occurrencesOf(large, 2000000));
    }

    @Test
    public void testOccurrencesOfLongArray() {
        long[] ids = { 1000L, 2000L, 3000L, 1000L, 2000L, 1000L };
        assertEquals(3, N.occurrencesOf(ids, 1000L));
        assertEquals(2, N.occurrencesOf(ids, 2000L));
        assertEquals(1, N.occurrencesOf(ids, 3000L));
        assertEquals(0, N.occurrencesOf(ids, 5000L));
    }

    @Test
    public void testOccurrencesOfLongArrayEmpty() {
        long[] empty = {};
        assertEquals(0, N.occurrencesOf(empty, 100L));
    }

    @Test
    public void testOccurrencesOfLongArrayNull() {
        long[] nullArray = null;
        assertEquals(0, N.occurrencesOf(nullArray, 1000L));
    }

    @Test
    public void testOccurrencesOfFloatArray() {
        float[] prices = { 1.5f, 2.3f, 1.5f, 3.7f, 1.5f };
        assertEquals(3, N.occurrencesOf(prices, 1.5f));
        assertEquals(1, N.occurrencesOf(prices, 2.3f));
        assertEquals(0, N.occurrencesOf(prices, 9.9f));
    }

    @Test
    public void testOccurrencesOfFloatArrayWithNaN() {
        float[] withNaN = { 1.0f, Float.NaN, 2.0f, Float.NaN, 3.0f };
        assertEquals(2, N.occurrencesOf(withNaN, Float.NaN));
        assertEquals(1, N.occurrencesOf(withNaN, 1.0f));
    }

    @Test
    public void testOccurrencesOfFloatArrayEmpty() {
        float[] empty = {};
        assertEquals(0, N.occurrencesOf(empty, 1.0f));
    }

    @Test
    public void testOccurrencesOfFloatArrayNull() {
        float[] nullArray = null;
        assertEquals(0, N.occurrencesOf(nullArray, 1.5f));
    }

    @Test
    public void testOccurrencesOfFloatArrayZero() {
        float[] zeros = { 0.0f, -0.0f, 0.0f };
        assertEquals(2, N.occurrencesOf(zeros, 0.0f));
        assertEquals(1, N.occurrencesOf(zeros, -0.0f));
    }

    @Test
    public void testOccurrencesOfDoubleArray() {
        double[] measurements = { 1.5, 2.3, 1.5, 3.7, 1.5 };
        assertEquals(3, N.occurrencesOf(measurements, 1.5));
        assertEquals(1, N.occurrencesOf(measurements, 2.3));
        assertEquals(0, N.occurrencesOf(measurements, 9.9));
    }

    @Test
    public void testOccurrencesOfDoubleArrayWithNaN() {
        double[] withNaN = { 1.0, Double.NaN, 2.0, Double.NaN };
        assertEquals(2, N.occurrencesOf(withNaN, Double.NaN));
        assertEquals(1, N.occurrencesOf(withNaN, 1.0));
    }

    @Test
    public void testOccurrencesOfDoubleArrayEmpty() {
        double[] empty = {};
        assertEquals(0, N.occurrencesOf(empty, 1.0));
    }

    @Test
    public void testOccurrencesOfDoubleArrayNull() {
        double[] nullArray = null;
        assertEquals(0, N.occurrencesOf(nullArray, 1.5));
    }

    @Test
    public void testOccurrencesOfDoubleArrayInfinity() {
        double[] infinities = { Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY };
        assertEquals(2, N.occurrencesOf(infinities, Double.POSITIVE_INFINITY));
        assertEquals(1, N.occurrencesOf(infinities, Double.NEGATIVE_INFINITY));
    }

    @Test
    public void testOccurrencesOfObjectArray() {
        String[] words = { "hello", "world", "hello", "java", "hello" };
        assertEquals(3, N.occurrencesOf(words, "hello"));
        assertEquals(1, N.occurrencesOf(words, "world"));
        assertEquals(0, N.occurrencesOf(words, "missing"));
    }

    @Test
    public void testOccurrencesOfObjectArrayWithNulls() {
        String[] withNulls = { "a", null, "b", null, "c" };
        assertEquals(2, N.occurrencesOf(withNulls, null));
        assertEquals(1, N.occurrencesOf(withNulls, "a"));
    }

    @Test
    public void testOccurrencesOfObjectArrayEmpty() {
        Object[] empty = {};
        assertEquals(0, N.occurrencesOf(empty, "test"));
    }

    @Test
    public void testOccurrencesOfObjectArrayNull() {
        Object[] nullArray = null;
        assertEquals(0, N.occurrencesOf(nullArray, "test"));
    }

    @Test
    public void testOccurrencesOfObjectArrayDifferentTypes() {
        Object[] mixed = { 1, "hello", 1, 2.5, "hello", 1 };
        assertEquals(3, N.occurrencesOf(mixed, 1));
        assertEquals(2, N.occurrencesOf(mixed, "hello"));
        assertEquals(1, N.occurrencesOf(mixed, 2.5));
    }

    @Test
    public void testOccurrencesOfIterable() {
        List<String> words = Arrays.asList("hello", "world", "hello", "java");
        assertEquals(2, N.occurrencesOf(words, "hello"));
        assertEquals(1, N.occurrencesOf(words, "world"));
        assertEquals(0, N.occurrencesOf(words, "missing"));
    }

    @Test
    public void testOccurrencesOfIterableWithNulls() {
        List<String> withNulls = Arrays.asList("a", null, "b", null);
        assertEquals(2, N.occurrencesOf(withNulls, null));
        assertEquals(1, N.occurrencesOf(withNulls, "a"));
    }

    @Test
    public void testOccurrencesOfIterableEmpty() {
        List<String> empty = new ArrayList<>();
        assertEquals(0, N.occurrencesOf(empty, "test"));
    }

    @Test
    public void testOccurrencesOfIterableNull() {
        Iterable<String> nullIterable = null;
        assertEquals(0, N.occurrencesOf(nullIterable, "test"));
    }

    @Test
    public void testOccurrencesOfIterableSet() {
        Set<Integer> numbers = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5));
        assertEquals(1, N.occurrencesOf(numbers, 3));
        assertEquals(0, N.occurrencesOf(numbers, 10));
    }

    @Test
    public void testOccurrencesOfIterator() {
        List<String> words = Arrays.asList("hello", "world", "hello", "java");
        Iterator<String> iter = words.iterator();
        assertEquals(2, N.occurrencesOf(iter, "hello"));
    }

    @Test
    public void testOccurrencesOfIteratorNull() {
        Iterator<String> nullIter = null;
        assertEquals(0, N.occurrencesOf(nullIter, "test"));
    }

    @Test
    public void testOccurrencesOfIteratorEmpty() {
        Iterator<String> empty = Collections.emptyIterator();
        assertEquals(0, N.occurrencesOf(empty, "test"));
    }

    @Test
    public void testOccurrencesOfStringChar() {
        String text = "hello world";
        assertEquals(3, N.occurrencesOf(text, 'l'));
        assertEquals(2, N.occurrencesOf(text, 'o'));
        assertEquals(1, N.occurrencesOf(text, ' '));
        assertEquals(0, N.occurrencesOf(text, 'z'));
    }

    @Test
    public void testOccurrencesOfStringCharEmpty() {
        String empty = "";
        assertEquals(0, N.occurrencesOf(empty, 'a'));
    }

    @Test
    public void testOccurrencesOfStringCharNull() {
        String nullStr = null;
        assertEquals(0, N.occurrencesOf(nullStr, 'x'));
    }

    @Test
    public void testOccurrencesOfStringSubstring() {
        String text = "hello hello world hello";
        assertEquals(3, N.occurrencesOf(text, "hello"));
        assertEquals(1, N.occurrencesOf(text, "world"));
        assertEquals(0, N.occurrencesOf(text, "java"));
    }

    @Test
    public void testOccurrencesOfStringSubstringOverlapping() {
        String pattern = "ababab";
        assertEquals(3, N.occurrencesOf(pattern, "ab"));
    }

    @Test
    public void testOccurrencesOfStringSubstringEmpty() {
        String empty = "";
        assertEquals(0, N.occurrencesOf(empty, "test"));
    }

    @Test
    public void testOccurrencesOfStringSubstringNull() {
        String nullStr = null;
        assertEquals(0, N.occurrencesOf(nullStr, "test"));
    }

    @Test
    public void testOccurrencesMapArray() {
        String[] words = { "apple", "banana", "apple", "cherry", "banana", "apple" };
        Map<String, Integer> counts = N.occurrencesMap(words);

        assertEquals(3, counts.size());
        assertEquals(3, counts.get("apple").intValue());
        assertEquals(2, counts.get("banana").intValue());
        assertEquals(1, counts.get("cherry").intValue());
    }

    @Test
    public void testOccurrencesMapArrayEmpty() {
        String[] empty = {};
        Map<String, Integer> counts = N.occurrencesMap(empty);
        assertTrue(counts.isEmpty());
    }

    @Test
    public void testOccurrencesMapArrayNull() {
        String[] nullArray = null;
        Map<String, Integer> counts = N.occurrencesMap(nullArray);
        assertTrue(counts.isEmpty());
    }

    @Test
    public void testOccurrencesMapArrayWithNulls() {
        String[] withNulls = { "a", null, "b", null, "a" };
        Map<String, Integer> counts = N.occurrencesMap(withNulls);

        assertEquals(3, counts.size());
        assertEquals(2, counts.get("a").intValue());
        assertEquals(1, counts.get("b").intValue());
        assertEquals(2, counts.get(null).intValue());
    }

    @Test
    public void testOccurrencesMapArrayWithSupplier() {
        String[] words = { "c", "a", "b", "a", "c" };
        Map<String, Integer> sorted = N.occurrencesMap(words, TreeMap::new);

        assertTrue(sorted instanceof TreeMap);
        assertEquals(Arrays.asList("a", "b", "c"), new ArrayList<>(sorted.keySet()));
    }

    @Test
    public void testOccurrencesMapArrayLinkedHashMap() {
        String[] words = { "c", "a", "b", "a", "c" };
        Map<String, Integer> ordered = N.occurrencesMap(words, LinkedHashMap::new);

        assertTrue(ordered instanceof LinkedHashMap);
        assertEquals(Arrays.asList("c", "a", "b"), new ArrayList<>(ordered.keySet()));
    }

    @Test
    public void testOccurrencesMapIterable() {
        List<String> words = Arrays.asList("apple", "banana", "apple", "cherry");
        Map<String, Integer> counts = N.occurrencesMap(words);

        assertEquals(3, counts.size());
        assertEquals(2, counts.get("apple").intValue());
        assertEquals(1, counts.get("banana").intValue());
        assertEquals(1, counts.get("cherry").intValue());
    }

    @Test
    public void testOccurrencesMapIterableEmpty() {
        List<String> empty = new ArrayList<>();
        Map<String, Integer> counts = N.occurrencesMap(empty);
        assertTrue(counts.isEmpty());
    }

    @Test
    public void testOccurrencesMapIterableNull() {
        Iterable<String> nullIterable = null;
        Map<String, Integer> counts = N.occurrencesMap(nullIterable);
        assertTrue(counts.isEmpty());
    }

    @Test
    public void testOccurrencesMapIterableSet() {
        Set<Integer> uniqueNumbers = new HashSet<>(Arrays.asList(1, 2, 3));
        Map<Integer, Integer> setCounts = N.occurrencesMap(uniqueNumbers);

        assertEquals(3, setCounts.size());
        assertEquals(1, setCounts.get(1).intValue());
        assertEquals(1, setCounts.get(2).intValue());
        assertEquals(1, setCounts.get(3).intValue());
    }

    @Test
    public void testOccurrencesMapIterableWithSupplier() {
        List<String> words = Arrays.asList("c", "a", "b", "a", "c");
        Map<String, Integer> sorted = N.occurrencesMap(words, TreeMap::new);

        assertTrue(sorted instanceof TreeMap);
        assertEquals(Arrays.asList("a", "b", "c"), new ArrayList<>(sorted.keySet()));
    }

    @Test
    public void testOccurrencesMapIterator() {
        List<String> words = Arrays.asList("apple", "banana", "apple", "cherry");
        Iterator<String> iter = words.iterator();
        Map<String, Integer> counts = N.occurrencesMap(iter);

        assertEquals(3, counts.size());
        assertEquals(2, counts.get("apple").intValue());
        assertEquals(1, counts.get("banana").intValue());
    }

    @Test
    public void testOccurrencesMapIteratorNull() {
        Iterator<String> nullIter = null;
        Map<String, Integer> counts = N.occurrencesMap(nullIter);
        assertTrue(counts.isEmpty());
    }

    @Test
    public void testOccurrencesMapIteratorEmpty() {
        Iterator<String> empty = Collections.emptyIterator();
        Map<String, Integer> counts = N.occurrencesMap(empty);
        assertTrue(counts.isEmpty());
    }

    @Test
    public void testOccurrencesMapIteratorWithSupplier() {
        List<String> words = Arrays.asList("c", "a", "b", "a", "c");
        Iterator<String> iter = words.iterator();
        Map<String, Integer> sorted = N.occurrencesMap(iter, TreeMap::new);

        assertTrue(sorted instanceof TreeMap);
        assertEquals(Arrays.asList("a", "b", "c"), new ArrayList<>(sorted.keySet()));
    }

    @Test
    public void testContainsBooleanArray() {
        boolean[] flags = { true, false, true };
        assertTrue(N.contains(flags, true));
        assertTrue(N.contains(flags, false));
    }

    @Test
    public void testContainsBooleanArrayEmpty() {
        boolean[] empty = {};
        assertTrue(!N.contains(empty, true));
    }

    @Test
    public void testContainsBooleanArrayNull() {
        boolean[] nullArray = null;
        assertTrue(!N.contains(nullArray, false));
    }

    @Test
    public void testContainsCharArray() {
        char[] letters = { 'a', 'b', 'c', 'd' };
        assertTrue(N.contains(letters, 'c'));
        assertTrue(!N.contains(letters, 'z'));
    }

    @Test
    public void testContainsCharArrayEmpty() {
        char[] empty = {};
        assertTrue(!N.contains(empty, 'x'));
    }

    @Test
    public void testContainsCharArrayNull() {
        char[] nullArray = null;
        assertTrue(!N.contains(nullArray, 'y'));
    }

    @Test
    public void testContainsByteArray() {
        byte[] data = { 1, 2, 3, 4, 5 };
        assertTrue(N.contains(data, (byte) 3));
        assertTrue(!N.contains(data, (byte) 10));
    }

    @Test
    public void testContainsByteArrayEmpty() {
        byte[] empty = {};
        assertTrue(!N.contains(empty, (byte) 1));
    }

    @Test
    public void testContainsByteArrayNull() {
        byte[] nullArray = null;
        assertTrue(!N.contains(nullArray, (byte) 2));
    }

    @Test
    public void testContainsShortArray() {
        short[] values = { 10, 20, 30, 40, 50 };
        assertTrue(N.contains(values, (short) 30));
        assertTrue(!N.contains(values, (short) 100));
    }

    @Test
    public void testContainsShortArrayEmpty() {
        short[] empty = {};
        assertTrue(!N.contains(empty, (short) 10));
    }

    @Test
    public void testContainsShortArrayNull() {
        short[] nullArray = null;
        assertTrue(!N.contains(nullArray, (short) 20));
    }

    @Test
    public void testContainsIntArray() {
        int[] numbers = { 1, 2, 3, 4, 5 };
        assertTrue(N.contains(numbers, 3));
        assertTrue(!N.contains(numbers, 7));
    }

    @Test
    public void testContainsIntArrayEmpty() {
        int[] empty = {};
        assertTrue(!N.contains(empty, 1));
    }

    @Test
    public void testContainsIntArrayNull() {
        int[] nullArray = null;
        assertTrue(!N.contains(nullArray, 1));
    }

    @Test
    public void testContainsLongArray() {
        long[] ids = { 1000L, 2000L, 3000L, 4000L };
        assertTrue(N.contains(ids, 1000L));
        assertTrue(!N.contains(ids, 5000L));
    }

    @Test
    public void testContainsLongArrayEmpty() {
        long[] empty = {};
        assertTrue(!N.contains(empty, 100L));
    }

    @Test
    public void testContainsLongArrayNull() {
        long[] nullArray = null;
        assertTrue(!N.contains(nullArray, 200L));
    }

    @Test
    public void testContainsFloatArray() {
        float[] values = { 1.5f, 2.3f, 3.7f, 4.1f };
        assertTrue(N.contains(values, 1.5f));
        assertTrue(!N.contains(values, 9.9f));
    }

    @Test
    public void testContainsFloatArrayWithNaN() {
        float[] withNaN = { 1.0f, Float.NaN, 2.0f };
        assertTrue(N.contains(withNaN, Float.NaN));
    }

    @Test
    public void testContainsFloatArrayEmpty() {
        float[] empty = {};
        assertTrue(!N.contains(empty, 1.0f));
    }

    @Test
    public void testContainsFloatArrayNull() {
        float[] nullArray = null;
        assertTrue(!N.contains(nullArray, 1.5f));
    }

    @Test
    public void testContainsDoubleArray() {
        double[] values = { 1.5, 2.3, 3.7, 4.1 };
        assertTrue(N.contains(values, 1.5));
        assertTrue(!N.contains(values, 9.9));
    }

    @Test
    public void testContainsDoubleArrayWithNaN() {
        double[] withNaN = { 1.0, Double.NaN, 2.0 };
        assertTrue(N.contains(withNaN, Double.NaN));
    }

    @Test
    public void testContainsDoubleArrayEmpty() {
        double[] empty = {};
        assertTrue(!N.contains(empty, 1.0));
    }

    @Test
    public void testContainsDoubleArrayNull() {
        double[] nullArray = null;
        assertTrue(!N.contains(nullArray, 1.5));
    }

    @Test
    public void testContainsObjectArray() {
        String[] words = { "hello", "world", null, "test" };
        assertTrue(N.contains(words, "hello"));
        assertTrue(N.contains(words, null));
        assertTrue(!N.contains(words, "missing"));
    }

    @Test
    public void testContainsObjectArrayIntegers() {
        Integer[] nums = { 1, 2, 3 };
        assertTrue(N.contains(nums, 2));
        assertTrue(!N.contains(nums, null));
    }

    @Test
    public void testContainsObjectArrayNull() {
        Object[] nullArray = null;
        assertTrue(!N.contains(nullArray, "any"));
    }

    @Test
    public void testContainsObjectArrayEmpty() {
        Object[] empty = {};
        assertTrue(!N.contains(empty, "test"));
    }

    @Test
    public void testContainsCollection() {
        List<String> words = Arrays.asList("apple", "banana", "cherry");
        assertTrue(N.contains(words, "apple"));
        assertTrue(!N.contains(words, "grape"));
    }

    @Test
    public void testContainsCollectionSet() {
        Set<Integer> numbers = new HashSet<>(Arrays.asList(1, 2, 3));
        assertTrue(N.contains(numbers, 2));
        assertTrue(!N.contains(numbers, 5));
    }

    @Test
    public void testContainsCollectionNull() {
        Collection<String> nullCollection = null;
        assertTrue(!N.contains(nullCollection, "test"));
    }

    @Test
    public void testContainsCollectionEmpty() {
        Collection<String> empty = new ArrayList<>();
        assertTrue(!N.contains(empty, "test"));
    }

    @Test
    public void testContainsIterable() {
        List<String> words = Arrays.asList("apple", "banana", null, "cherry");
        assertTrue(N.contains((Iterable<String>) words, "apple"));
        assertTrue(N.contains((Iterable<String>) words, null));
        assertTrue(!N.contains((Iterable<String>) words, "grape"));
    }

    @Test
    public void testContainsIterableSet() {
        Set<Integer> numbers = new HashSet<>(Arrays.asList(1, 2, 3));
        assertTrue(N.contains((Iterable<Integer>) numbers, 2));
        assertTrue(!N.contains((Iterable<Integer>) numbers, 5));
    }

    @Test
    public void testContainsIterableNull() {
        Iterable<String> nullIterable = null;
        assertTrue(!N.contains(nullIterable, "test"));
    }

    @Test
    public void testContainsIterableEmpty() {
        Iterable<String> empty = new ArrayList<>();
        assertTrue(!N.contains(empty, "test"));
    }

    @Test
    public void testContainsIterator() {
        Iterator<String> iterator = Arrays.asList("a", "b", "c").iterator();
        assertTrue(N.contains(iterator, "b"));
    }

    @Test
    public void testContainsIteratorNotFound() {
        Iterator<String> iterator = Arrays.asList("a", "b", "c").iterator();
        assertTrue(!N.contains(iterator, "z"));
    }

    @Test
    public void testContainsIteratorNull() {
        Iterator<String> nullIter = null;
        assertTrue(!N.contains(nullIter, "test"));
    }

    @Test
    public void testContainsIteratorEmpty() {
        Iterator<String> empty = Collections.emptyIterator();
        assertTrue(!N.contains(empty, "test"));
    }

    @Test
    public void testContainsAllCollection() {
        Set<String> colors = new HashSet<>(Arrays.asList("red", "green", "blue", "yellow"));
        List<String> primary = Arrays.asList("red", "blue", "yellow");
        assertTrue(N.containsAll(colors, primary));
    }

    @Test
    public void testContainsAllCollectionMissing() {
        Set<String> colors = new HashSet<>(Arrays.asList("red", "green", "blue", "yellow"));
        List<String> missing = Arrays.asList("red", "purple");
        assertTrue(!N.containsAll(colors, missing));
    }

    @Test
    public void testContainsAllCollectionEmpty() {
        Set<String> colors = new HashSet<>(Arrays.asList("red", "green", "blue"));
        assertTrue(N.containsAll(colors, Collections.emptyList()));
    }

    @Test
    public void testContainsAllCollectionNull() {
        Set<String> colors = new HashSet<>(Arrays.asList("red", "green", "blue"));
        assertTrue(N.containsAll(colors, (Collection) null));
    }

    @Test
    public void testContainsAllCollectionNullCollection() {
        List<String> primary = Arrays.asList("red", "blue");
        assertTrue(!N.containsAll((Collection) null, primary));
    }

    @Test
    public void testContainsAllVarargs() {
        Set<String> colors = new HashSet<>(Arrays.asList("red", "green", "blue", "yellow"));
        assertTrue(N.containsAll(colors, "red", "green", "blue"));
        assertTrue(!N.containsAll(colors, "red", "purple"));
    }

    @Test
    public void testContainsAllVarargsEmpty() {
        Set<String> colors = new HashSet<>(Arrays.asList("red", "green", "blue"));
        assertTrue(N.containsAll(colors));
    }

    @Test
    public void testContainsAllVarargsNull() {
        assertTrue(!N.containsAll(null, "red", "blue"));
    }

    @Test
    public void testContainsAllVarargsIntegers() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        assertTrue(N.containsAll(numbers, 2, 4, 5));
        assertTrue(!N.containsAll(numbers, 2, 10));
    }

    @Test
    public void testContainsAllIterable() {
        Set<String> searchFor = new HashSet<>(Arrays.asList("apple", "banana"));
        List<String> fruits = Arrays.asList("apple", "banana", "cherry", "date");
        assertTrue(N.containsAll((Iterable<String>) fruits, searchFor));
    }

    @Test
    public void testContainsAllIterableMissing() {
        Set<String> searchFor = new HashSet<>(Arrays.asList("apple", "grape"));
        List<String> fruits = Arrays.asList("apple", "banana", "cherry");
        assertTrue(!N.containsAll((Iterable<String>) fruits, searchFor));
    }

    @Test
    public void testContainsAllIterableEmpty() {
        Set<String> searchFor = new HashSet<>(Arrays.asList("apple", "banana"));
        Iterable<String> empty = new ArrayList<>();
        assertTrue(!N.containsAll(empty, searchFor));
    }

    @Test
    public void testContainsAllIterableNull() {
        Set<String> searchFor = new HashSet<>(Arrays.asList("apple", "banana"));
        assertTrue(!N.containsAll((Iterable<String>) null, searchFor));
    }

    @Test
    public void testContainsAllIterator() {
        List<String> words = Arrays.asList("hello", "world", "java", "stream");
        Set<String> searchFor = new HashSet<>(Arrays.asList("hello", "java"));
        assertTrue(N.containsAll(words.iterator(), searchFor));
    }

    @Test
    public void testContainsAllIteratorMissing() {
        List<String> words = Arrays.asList("hello", "world", "java");
        Set<String> searchFor = new HashSet<>(Arrays.asList("hello", "missing"));
        assertTrue(!N.containsAll(words.iterator(), searchFor));
    }

    @Test
    public void testContainsAllIteratorNull() {
        Set<String> searchFor = new HashSet<>(Arrays.asList("hello", "java"));
        assertTrue(!N.containsAll((Iterator<String>) null, searchFor));
    }

    @Test
    public void testContainsAllIteratorEmpty() {
        Set<String> searchFor = new HashSet<>(Arrays.asList("test"));
        assertTrue(N.containsAll(Collections.emptyIterator(), Collections.emptySet()));
    }

    @Test
    public void testContainsAnyCollection() {
        Set<String> colors = new HashSet<>(Arrays.asList("red", "green", "blue"));
        List<String> warm = Arrays.asList("red", "orange", "yellow");
        assertTrue(N.containsAny(colors, warm));
    }

    @Test
    public void testContainsAnyCollectionNoMatch() {
        Set<String> colors = new HashSet<>(Arrays.asList("red", "green", "blue"));
        List<String> cool = Arrays.asList("purple", "cyan");
        assertTrue(!N.containsAny(colors, cool));
    }

    @Test
    public void testContainsAnyCollectionEmpty() {
        Set<String> colors = new HashSet<>(Arrays.asList("red", "green", "blue"));
        assertTrue(!N.containsAny(colors, Collections.emptyList()));
    }

    @Test
    public void testContainsAnyCollectionNull() {
        List<String> warm = Arrays.asList("red", "orange");
        assertTrue(!N.containsAny(null, warm));
    }

    @Test
    public void testContainsAnyVarargs() {
        Set<String> colors = new HashSet<>(Arrays.asList("red", "green", "blue"));
        assertTrue(N.containsAny(colors, "red", "orange", "yellow"));
        assertTrue(!N.containsAny(colors, "purple", "cyan"));
    }

    @Test
    public void testContainsAnyVarargsEmpty() {
        Set<String> colors = new HashSet<>(Arrays.asList("red", "green", "blue"));
        assertTrue(!N.containsAny(colors));
    }

    @Test
    public void testContainsAnyVarargsIntegers() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        assertTrue(N.containsAny(numbers, 2, 6, 8));
        assertTrue(!N.containsAny(numbers, 10, 11, 12));
    }

    @Test
    public void testContainsAnyIterable() {
        List<String> fruits = Arrays.asList("apple", "banana", "cherry", "date");
        Set<String> common = new HashSet<>(Arrays.asList("apple", "grape"));
        assertTrue(N.containsAny((Iterable<String>) fruits, common));
    }

    @Test
    public void testContainsAnyIterableNoMatch() {
        List<String> fruits = Arrays.asList("apple", "banana", "cherry");
        Set<String> citrus = new HashSet<>(Arrays.asList("orange", "lemon", "lime"));
        assertTrue(!N.containsAny((Iterable<String>) fruits, citrus));
    }

    @Test
    public void testContainsAnyIterableNull() {
        Set<String> common = new HashSet<>(Arrays.asList("apple", "grape"));
        assertTrue(!N.containsAny((Iterable<String>) null, common));
    }

    @Test
    public void testContainsAnyIterableEmpty() {
        Iterable<String> empty = new ArrayList<>();
        Set<String> common = new HashSet<>(Arrays.asList("apple"));
        assertTrue(!N.containsAny(empty, common));
    }

    @Test
    public void testContainsAnyIterator() {
        List<String> words = Arrays.asList("hello", "world", "java", "stream");
        Set<String> keywords = new HashSet<>(Arrays.asList("java", "python", "c++"));
        assertTrue(N.containsAny(words.iterator(), keywords));
    }

    @Test
    public void testContainsAnyIteratorNoMatch() {
        List<String> words = Arrays.asList("hello", "world");
        Set<String> keywords = new HashSet<>(Arrays.asList("java", "python"));
        assertTrue(!N.containsAny(words.iterator(), keywords));
    }

    @Test
    public void testContainsAnyIteratorNull() {
        Set<String> keywords = new HashSet<>(Arrays.asList("java"));
        assertTrue(!N.containsAny((Iterator<String>) null, keywords));
    }

    @Test
    public void testContainsNoneCollection() {
        Set<String> allowedColors = new HashSet<>(Arrays.asList("red", "green", "blue"));
        List<String> invalidColors = Arrays.asList("yellow");
        assertTrue(N.containsNone(allowedColors, invalidColors));
    }

    @Test
    public void testContainsNoneCollectionHasMatch() {
        List<String> userColors = Arrays.asList("red", "yellow");
        List<String> invalidColors = Arrays.asList("yellow");
        assertTrue(!N.containsNone(userColors, invalidColors));
    }

    @Test
    public void testContainsNoneCollectionEmpty() {
        Set<String> allowedColors = new HashSet<>(Arrays.asList("red", "green", "blue"));
        assertTrue(N.containsNone(allowedColors, Collections.emptyList()));
    }

    @Test
    public void testContainsNoneCollectionNull() {
        List<String> invalidColors = Arrays.asList("yellow");
        assertTrue(N.containsNone(null, invalidColors));
    }

    @Test
    public void testContainsNoneVarargs() {
        Set<String> userInput = new HashSet<>(Arrays.asList("safe", "clean", "valid"));
        assertTrue(N.containsNone(userInput, "script", "eval", "exec"));
    }

    @Test
    public void testContainsNoneVarargsHasMatch() {
        List<String> mixed = Arrays.asList("good", "bad", "neutral");
        assertTrue(!N.containsNone(mixed, "bad", "evil"));
    }

    @Test
    public void testContainsNoneVarargsEmpty() {
        Set<String> userInput = new HashSet<>(Arrays.asList("safe", "clean"));
        assertTrue(N.containsNone(userInput));
    }

    @Test
    public void testContainsNoneVarargsNull() {
        assertTrue(N.containsNone(null, "test"));
    }

    @Test
    public void testContainsNoneIterable() {
        List<String> document = Arrays.asList("the", "quick", "brown", "fox");
        Set<String> stopWords = new HashSet<>(Arrays.asList("and", "or", "but", "if"));
        assertTrue(N.containsNone((Iterable<String>) document, stopWords));
    }

    @Test
    public void testContainsNoneIterableHasMatch() {
        List<String> document = Arrays.asList("the", "quick", "brown", "fox");
        Set<String> commonWords = new HashSet<>(Arrays.asList("the", "and", "or"));
        assertTrue(!N.containsNone((Iterable<String>) document, commonWords));
    }

    @Test
    public void testContainsNoneIterableNull() {
        Set<String> stopWords = new HashSet<>(Arrays.asList("and", "or"));
        assertTrue(N.containsNone((Iterable<String>) null, stopWords));
    }

    @Test
    public void testContainsNoneIterableEmpty() {
        Iterable<String> empty = new ArrayList<>();
        Set<String> stopWords = new HashSet<>(Arrays.asList("and", "or"));
        assertTrue(N.containsNone(empty, stopWords));
    }

    @Test
    public void testContainsNoneIterator() {
        List<String> userInput = Arrays.asList("hello", "world", "test");
        Set<String> forbidden = new HashSet<>(Arrays.asList("admin", "root", "system"));
        assertTrue(N.containsNone(userInput.iterator(), forbidden));
    }

    @Test
    public void testContainsNoneIteratorHasMatch() {
        List<String> userInput = Arrays.asList("hello", "admin", "test");
        Set<String> forbidden = new HashSet<>(Arrays.asList("admin", "root"));
        assertTrue(!N.containsNone(userInput.iterator(), forbidden));
    }

    @Test
    public void testContainsNoneIteratorNull() {
        Set<String> forbidden = new HashSet<>(Arrays.asList("admin"));
        assertTrue(N.containsNone((Iterator<String>) null, forbidden));
    }

    @Test
    public void testFlattenBooleanArray() {
        boolean[][] array = { { true, false }, { true }, { false, false, true } };
        boolean[] result = N.flatten(array);
        assertArrayEquals(new boolean[] { true, false, true, false, false, true }, result);
    }

    @Test
    public void testFlattenBooleanArrayNull() {
        boolean[][] array = null;
        boolean[] result = N.flatten(array);
        assertEquals(0, result.length);
    }

    @Test
    public void testFlattenBooleanArrayEmpty() {
        boolean[][] array = {};
        boolean[] result = N.flatten(array);
        assertEquals(0, result.length);
    }

    @Test
    public void testFlattenBooleanArrayWithNulls() {
        boolean[][] array = { { true, false }, null, { true } };
        boolean[] result = N.flatten(array);
        assertArrayEquals(new boolean[] { true, false, true }, result);
    }

    @Test
    public void testFlattenCharArray() {
        char[][] array = { { 'a', 'b' }, { 'c' }, { 'd', 'e', 'f' } };
        char[] result = N.flatten(array);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e', 'f' }, result);
    }

    @Test
    public void testFlattenCharArrayNull() {
        char[][] array = null;
        char[] result = N.flatten(array);
        assertEquals(0, result.length);
    }

    @Test
    public void testFlattenCharArrayEmpty() {
        char[][] array = {};
        char[] result = N.flatten(array);
        assertEquals(0, result.length);
    }

    @Test
    public void testFlattenByteArray() {
        byte[][] array = { { 1, 2 }, { 3 }, { 4, 5 } };
        byte[] result = N.flatten(array);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testFlattenByteArrayNull() {
        byte[][] array = null;
        byte[] result = N.flatten(array);
        assertEquals(0, result.length);
    }

    @Test
    public void testFlattenByteArrayWithNulls() {
        byte[][] array = { { 1, 2 }, null, { 3 } };
        byte[] result = N.flatten(array);
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
    }

    @Test
    public void testFlattenShortArray() {
        short[][] array = { { 10, 20 }, { 30 }, { 40, 50 } };
        short[] result = N.flatten(array);
        assertArrayEquals(new short[] { 10, 20, 30, 40, 50 }, result);
    }

    @Test
    public void testFlattenShortArrayNull() {
        short[][] array = null;
        short[] result = N.flatten(array);
        assertEquals(0, result.length);
    }

    @Test
    public void testFlattenShortArrayEmpty() {
        short[][] array = {};
        short[] result = N.flatten(array);
        assertEquals(0, result.length);
    }

    @Test
    public void testFlattenIntArray() {
        int[][] array = { { 1, 2, 3 }, { 4, 5 }, { 6 } };
        int[] result = N.flatten(array);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, result);
    }

    @Test
    public void testFlattenIntArrayNull() {
        int[][] array = null;
        int[] result = N.flatten(array);
        assertEquals(0, result.length);
    }

    @Test
    public void testFlattenIntArrayEmpty() {
        int[][] array = {};
        int[] result = N.flatten(array);
        assertEquals(0, result.length);
    }

    @Test
    public void testFlattenIntArrayWithEmptyArrays() {
        int[][] array = { { 1, 2 }, {}, { 3, 4 } };
        int[] result = N.flatten(array);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testFlattenLongArray() {
        long[][] array = { { 1000L, 2000L }, { 3000L }, { 4000L, 5000L } };
        long[] result = N.flatten(array);
        assertArrayEquals(new long[] { 1000L, 2000L, 3000L, 4000L, 5000L }, result);
    }

    @Test
    public void testFlattenLongArrayNull() {
        long[][] array = null;
        long[] result = N.flatten(array);
        assertEquals(0, result.length);
    }

    @Test
    public void testFlattenLongArrayEmpty() {
        long[][] array = {};
        long[] result = N.flatten(array);
        assertEquals(0, result.length);
    }

    @Test
    public void testFlattenFloatArray() {
        float[][] array = { { 1.5f, 2.5f }, { 3.5f }, { 4.5f, 5.5f } };
        float[] result = N.flatten(array);
        assertArrayEquals(new float[] { 1.5f, 2.5f, 3.5f, 4.5f, 5.5f }, result, 0.001f);
    }

    @Test
    public void testFlattenFloatArrayNull() {
        float[][] array = null;
        float[] result = N.flatten(array);
        assertEquals(0, result.length);
    }

    @Test
    public void testFlattenFloatArrayEmpty() {
        float[][] array = {};
        float[] result = N.flatten(array);
        assertEquals(0, result.length);
    }

    @Test
    public void testFlattenDoubleArray() {
        double[][] array = { { 1.1, 2.2 }, { 3.3 }, { 4.4, 5.5 } };
        double[] result = N.flatten(array);
        assertArrayEquals(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 }, result, 0.001);
    }

    @Test
    public void testFlattenDoubleArrayNull() {
        double[][] array = null;
        double[] result = N.flatten(array);
        assertEquals(0, result.length);
    }

    @Test
    public void testFlattenDoubleArrayEmpty() {
        double[][] array = {};
        double[] result = N.flatten(array);
        assertEquals(0, result.length);
    }

    @Test
    public void testFlattenObjectArray() {
        String[][] array = { { "a", "b" }, { "c" }, { "d", "e" } };
        String[] result = N.flatten(array);
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" }, result);
    }

    @Test
    public void testFlattenObjectArrayNull() {
        String[][] array = null;
        String[] result = N.flatten(array);
        assertNull(result);
    }

    @Test
    public void testFlattenObjectArrayEmpty() {
        String[][] array = {};
        String[] result = N.flatten(array);
        assertEquals(0, result.length);
    }

    @Test
    public void testFlattenObjectArrayWithNulls() {
        String[][] array = { { "a", "b" }, null, { "c" } };
        String[] result = N.flatten(array);
        assertArrayEquals(new String[] { "a", "b", "c" }, result);
    }

    @Test
    public void testFlattenIterables() {
        List<List<String>> lists = Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c"), Arrays.asList("d", "e", "f"));
        List<String> result = N.flatten(lists);
        assertEquals(Arrays.asList("a", "b", "c", "d", "e", "f"), result);
    }

    @Test
    public void testFlattenIterablesNull() {
        List<List<String>> lists = null;
        List<String> result = N.flatten(lists);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlattenIterablesEmpty() {
        List<List<String>> lists = new ArrayList<>();
        List<String> result = N.flatten(lists);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlattenIterablesWithNulls() {
        List<List<String>> lists = Arrays.asList(Arrays.asList("a", "b"), null, Arrays.asList("c"));
        List<String> result = N.flatten(lists);
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testFlattenIterablesWithSupplier() {
        List<List<String>> lists = Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c", "d"));
        Set<String> result = N.flatten(lists, (size) -> new HashSet<>());
        assertEquals(new HashSet<>(Arrays.asList("a", "b", "c", "d")), result);
    }

    @Test
    public void testFlattenIterator() {
        List<Iterator<String>> iters = Arrays.asList(Arrays.asList("a", "b").iterator(), Arrays.asList("c").iterator());
        Iterator<String> result = N.flatten(iters.iterator());
        List<String> resultList = new ArrayList<>();
        while (result.hasNext()) {
            resultList.add(result.next());
        }
        assertEquals(Arrays.asList("a", "b", "c"), resultList);
    }

    @Test
    public void testFlattenIteratorNull() {
        Iterator<Iterator<String>> iters = null;
        Iterator<String> result = N.flatten(iters);
        assertTrue(!result.hasNext());
    }

    @Test
    public void testFlattenEachElement() {
        List<Object> mixed = Arrays.asList("a", Arrays.asList("b", "c"), "d", Arrays.asList("e", "f"));
        List<?> result = N.flattenEachElement(mixed);
        assertEquals(Arrays.asList("a", "b", "c", "d", "e", "f"), result);
    }

    @Test
    public void testFlattenEachElementNull() {
        List<Object> mixed = null;
        List<?> result = N.flattenEachElement(mixed);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlattenEachElementEmpty() {
        List<Object> mixed = new ArrayList<>();
        List<?> result = N.flattenEachElement(mixed);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlattenEachElementNestedIterables() {
        List<Object> nested = Arrays.asList(Arrays.asList("a", Arrays.asList("b", "c")), "d", Arrays.asList("e"));
        List<?> result = N.flattenEachElement(nested);
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), result);
    }

    @Test
    public void testFlattenEachElementWithSupplier() {
        List<Object> mixed = Arrays.asList("a", Arrays.asList("b", "c"), "d");
        Set<?> result = N.flattenEachElement(mixed, () -> new HashSet<>());
        assertEquals(new HashSet<>(Arrays.asList("a", "b", "c", "d")), result);
    }

    @Test
    public void testIntersectionBooleanArray() {
        boolean[] a = { true, false, false, true };
        boolean[] b = { false, false, true };
        boolean[] result = N.intersection(a, b);
        assertArrayEquals(new boolean[] { true, false, false }, result);
    }

    @Test
    public void testIntersectionBooleanArrayNull() {
        boolean[] a = null;
        boolean[] b = { true, false };
        boolean[] result = N.intersection(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testIntersectionBooleanArrayEmpty() {
        boolean[] a = {};
        boolean[] b = { true, false };
        boolean[] result = N.intersection(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testIntersectionCharArray() {
        char[] a = { 'a', 'b', 'b', 'c' };
        char[] b = { 'a', 'b', 'b', 'b', 'd' };
        char[] result = N.intersection(a, b);
        assertArrayEquals(new char[] { 'a', 'b', 'b' }, result);
    }

    @Test
    public void testIntersectionCharArrayNoCommon() {
        char[] a = { 'x', 'y' };
        char[] b = { 'z', 'w' };
        char[] result = N.intersection(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testIntersectionCharArrayNull() {
        char[] a = null;
        char[] b = { 'a', 'b' };
        char[] result = N.intersection(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testIntersectionByteArray() {
        byte[] a = { 1, 2, 2, 3, 4 };
        byte[] b = { 1, 2, 2, 2, 5, 6 };
        byte[] result = N.intersection(a, b);
        assertArrayEquals(new byte[] { 1, 2, 2 }, result);
    }

    @Test
    public void testIntersectionByteArrayNoCommon() {
        byte[] a = { 1, 2, 3 };
        byte[] b = { 4, 5, 6 };
        byte[] result = N.intersection(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testIntersectionByteArrayNull() {
        byte[] a = null;
        byte[] b = { 1, 2 };
        byte[] result = N.intersection(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testIntersectionShortArray() {
        short[] a = { 1, 2, 2, 3, 4 };
        short[] b = { 1, 2, 2, 2, 5, 6 };
        short[] result = N.intersection(a, b);
        assertArrayEquals(new short[] { 1, 2, 2 }, result);
    }

    @Test
    public void testIntersectionShortArrayNull() {
        short[] a = null;
        short[] b = { 1, 2 };
        short[] result = N.intersection(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testIntersectionIntArray() {
        int[] a = { 1, 2, 2, 3, 4 };
        int[] b = { 1, 2, 2, 2, 5, 6 };
        int[] result = N.intersection(a, b);
        assertArrayEquals(new int[] { 1, 2, 2 }, result);
    }

    @Test
    public void testIntersectionIntArrayNoCommon() {
        int[] a = { 1, 2, 3 };
        int[] b = { 4, 5, 6 };
        int[] result = N.intersection(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testIntersectionIntArrayNull() {
        int[] a = null;
        int[] b = { 1, 2 };
        int[] result = N.intersection(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testIntersectionLongArray() {
        long[] a = { 1L, 2L, 2L, 3L, 4L };
        long[] b = { 1L, 2L, 2L, 2L, 5L, 6L };
        long[] result = N.intersection(a, b);
        assertArrayEquals(new long[] { 1L, 2L, 2L }, result);
    }

    @Test
    public void testIntersectionLongArrayNull() {
        long[] a = null;
        long[] b = { 1L, 2L };
        long[] result = N.intersection(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testIntersectionFloatArray() {
        float[] a = { 1.5f, 2.5f, 2.5f, 3.5f };
        float[] b = { 1.5f, 2.5f, 2.5f, 2.5f, 4.5f };
        float[] result = N.intersection(a, b);
        assertArrayEquals(new float[] { 1.5f, 2.5f, 2.5f }, result, 0.001f);
    }

    @Test
    public void testIntersectionFloatArrayNull() {
        float[] a = null;
        float[] b = { 1.5f, 2.5f };
        float[] result = N.intersection(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testIntersectionDoubleArray() {
        double[] a = { 1.1, 2.2, 2.2, 3.3 };
        double[] b = { 1.1, 2.2, 2.2, 2.2, 4.4 };
        double[] result = N.intersection(a, b);
        assertArrayEquals(new double[] { 1.1, 2.2, 2.2 }, result, 0.001);
    }

    @Test
    public void testIntersectionDoubleArrayNull() {
        double[] a = null;
        double[] b = { 1.1, 2.2 };
        double[] result = N.intersection(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testIntersectionObjectArray() {
        String[] a = { "A", "B", "B", "C", "D" };
        String[] b = { "A", "B", "B", "B", "E", "F" };
        List<String> result = N.intersection(a, b);
        assertEquals(Arrays.asList("A", "B", "B"), result);
    }

    @Test
    public void testIntersectionObjectArrayNull() {
        String[] a = null;
        String[] b = { "A", "B" };
        List<String> result = N.intersection(a, b);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testIntersectionObjectArrayEmpty() {
        String[] a = {};
        String[] b = { "A", "B" };
        List<String> result = N.intersection(a, b);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testIntersectionCollection() {
        List<String> a = Arrays.asList("A", "B", "B", "C", "D");
        List<String> b = Arrays.asList("A", "B", "B", "B", "E", "F");
        List<String> result = N.intersection(a, b);
        assertEquals(Arrays.asList("A", "B", "B"), result);
    }

    @Test
    public void testIntersectionCollectionNoCommon() {
        List<Integer> a = Arrays.asList(1, 2, 3);
        List<Integer> b = Arrays.asList(4, 5, 6);
        List<Integer> result = N.intersection(a, b);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testIntersectionCollectionNull() {
        List<String> a = null;
        List<String> b = Arrays.asList("A", "B");
        List<String> result = N.intersection(a, b);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testIntersectionCollectionOfCollections() {
        List<String> a = Arrays.asList("A", "B", "B", "C");
        List<String> b = Arrays.asList("A", "B", "B", "B", "D");
        List<String> c = Arrays.asList("A", "B", "E");
        List<List<String>> collections = Arrays.asList(a, b, c);
        List<String> result = N.intersection(collections);
        assertEquals(Arrays.asList("A", "B"), result);
    }

    @Test
    public void testIntersectionCollectionOfCollectionsEmpty() {
        List<List<String>> collections = new ArrayList<>();
        List<String> result = N.intersection(collections);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testIntersectionCollectionOfCollectionsSingle() {
        List<List<String>> collections = Arrays.asList(Arrays.asList("A", "B", "C"));
        List<String> result = N.intersection(collections);
        assertEquals(Arrays.asList("A", "B", "C"), result);
    }

    @Test
    public void testIntersectionCollectionOfCollectionsWithEmpty() {
        List<String> a = Arrays.asList("A", "B");
        List<String> b = new ArrayList<>();
        List<List<String>> collections = Arrays.asList(a, b);
        List<String> result = N.intersection(collections);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDifferenceBooleanArray() {
        boolean[] a = { true, true, false };
        boolean[] b = { true, false };
        boolean[] result = N.difference(a, b);
        assertArrayEquals(new boolean[] { true }, result);
    }

    @Test
    public void testDifferenceBooleanArrayNull() {
        boolean[] a = null;
        boolean[] b = { true, false };
        boolean[] result = N.difference(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testDifferenceBooleanArraySecondNull() {
        boolean[] a = { true, false };
        boolean[] b = null;
        boolean[] result = N.difference(a, b);
        assertArrayEquals(new boolean[] { true, false }, result);
    }

    @Test
    public void testDifferenceCharArray() {
        char[] a = { 'a', 'b', 'b', 'c', 'd' };
        char[] b = { 'a', 'b', 'e' };
        char[] result = N.difference(a, b);
        assertArrayEquals(new char[] { 'b', 'c', 'd' }, result);
    }

    @Test
    public void testDifferenceCharArrayNull() {
        char[] a = null;
        char[] b = { 'a', 'b' };
        char[] result = N.difference(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testDifferenceCharArraySecondNull() {
        char[] a = { 'a', 'b' };
        char[] b = null;
        char[] result = N.difference(a, b);
        assertArrayEquals(new char[] { 'a', 'b' }, result);
    }

    @Test
    public void testDifferenceByteArray() {
        byte[] a = { 1, 2, 2, 3, 4 };
        byte[] b = { 2, 5 };
        byte[] result = N.difference(a, b);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testDifferenceByteArrayNull() {
        byte[] a = null;
        byte[] b = { 1, 2 };
        byte[] result = N.difference(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testDifferenceShortArray() {
        short[] a = { 1, 2, 2, 3, 4 };
        short[] b = { 2, 5 };
        short[] result = N.difference(a, b);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testDifferenceShortArrayNull() {
        short[] a = null;
        short[] b = { 1, 2 };
        short[] result = N.difference(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testDifferenceIntArray() {
        int[] a = { 1, 2, 2, 3, 4 };
        int[] b = { 2, 5 };
        int[] result = N.difference(a, b);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testDifferenceIntArrayNull() {
        int[] a = null;
        int[] b = { 1, 2 };
        int[] result = N.difference(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testDifferenceIntArraySecondNull() {
        int[] a = { 1, 2, 3 };
        int[] b = null;
        int[] result = N.difference(a, b);
        assertArrayEquals(new int[] { 1, 2, 3 }, result);
    }

    @Test
    public void testDifferenceLongArray() {
        long[] a = { 1L, 2L, 2L, 3L, 4L };
        long[] b = { 2L, 5L };
        long[] result = N.difference(a, b);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, result);
    }

    @Test
    public void testDifferenceLongArrayNull() {
        long[] a = null;
        long[] b = { 1L, 2L };
        long[] result = N.difference(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testDifferenceFloatArray() {
        float[] a = { 1.5f, 2.5f, 2.5f, 3.5f };
        float[] b = { 2.5f, 4.5f };
        float[] result = N.difference(a, b);
        assertArrayEquals(new float[] { 1.5f, 2.5f, 3.5f }, result, 0.001f);
    }

    @Test
    public void testDifferenceFloatArrayNull() {
        float[] a = null;
        float[] b = { 1.5f, 2.5f };
        float[] result = N.difference(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testDifferenceDoubleArray() {
        double[] a = { 1.1, 2.2, 2.2, 3.3 };
        double[] b = { 2.2, 4.4 };
        double[] result = N.difference(a, b);
        assertArrayEquals(new double[] { 1.1, 2.2, 3.3 }, result, 0.001);
    }

    @Test
    public void testDifferenceDoubleArrayNull() {
        double[] a = null;
        double[] b = { 1.1, 2.2 };
        double[] result = N.difference(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testDifferenceObjectArray() {
        String[] a = { "A", "B", "B", "C", "D" };
        String[] b = { "A", "B", "E" };
        List<String> result = N.difference(a, b);
        assertEquals(Arrays.asList("B", "C", "D"), result);
    }

    @Test
    public void testDifferenceObjectArrayNull() {
        String[] a = null;
        String[] b = { "A", "B" };
        List<String> result = N.difference(a, b);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDifferenceObjectArraySecondNull() {
        String[] a = { "A", "B", "C" };
        String[] b = null;
        List<String> result = N.difference(a, b);
        assertEquals(Arrays.asList("A", "B", "C"), result);
    }

    @Test
    public void testDifferenceCollection() {
        List<String> a = Arrays.asList("A", "B", "B", "C", "D");
        List<String> b = Arrays.asList("A", "B", "E");
        List<String> result = N.difference(a, b);
        assertEquals(Arrays.asList("B", "C", "D"), result);
    }

    @Test
    public void testDifferenceCollectionAllRemoved() {
        List<Integer> a = Arrays.asList(1, 2, 2, 3);
        List<Integer> b = Arrays.asList(2, 2, 2, 4);
        List<Integer> result = N.difference(a, b);
        assertEquals(Arrays.asList(1, 3), result);
    }

    @Test
    public void testDifferenceCollectionNull() {
        List<String> a = null;
        List<String> b = Arrays.asList("A", "B");
        List<String> result = N.difference(a, b);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDifferenceCollectionSecondNull() {
        List<String> a = Arrays.asList("A", "B", "C");
        List<String> b = null;
        List<String> result = N.difference(a, b);
        assertEquals(Arrays.asList("A", "B", "C"), result);
    }

    @Test
    public void testSymmetricDifferenceBooleanArray() {
        boolean[] a = { true, true, false };
        boolean[] b = { true, false };
        boolean[] result = N.symmetricDifference(a, b);
        assertArrayEquals(new boolean[] { true }, result);
    }

    @Test
    public void testSymmetricDifferenceBooleanArrayNull() {
        boolean[] a = null;
        boolean[] b = { true, false };
        boolean[] result = N.symmetricDifference(a, b);
        assertArrayEquals(new boolean[] { true, false }, result);
    }

    @Test
    public void testSymmetricDifferenceBooleanArrayBothNull() {
        boolean[] a = null;
        boolean[] b = null;
        boolean[] result = N.symmetricDifference(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testSymmetricDifferenceCharArray() {
        char[] a = { 'a', 'b', 'b', 'c' };
        char[] b = { 'b', 'd', 'a' };
        char[] result = N.symmetricDifference(a, b);
        assertArrayEquals(new char[] { 'b', 'c', 'd' }, result);
    }

    @Test
    public void testSymmetricDifferenceCharArrayNull() {
        char[] a = null;
        char[] b = { 'a', 'b' };
        char[] result = N.symmetricDifference(a, b);
        assertArrayEquals(new char[] { 'a', 'b' }, result);
    }

    @Test
    public void testSymmetricDifferenceByteArray() {
        byte[] a = { 0, 1, 2, 2, 3 };
        byte[] b = { 2, 5, 1 };
        byte[] result = N.symmetricDifference(a, b);
        assertArrayEquals(new byte[] { 0, 2, 3, 5 }, result);
    }

    @Test
    public void testSymmetricDifferenceByteArrayNull() {
        byte[] a = null;
        byte[] b = { 1, 2 };
        byte[] result = N.symmetricDifference(a, b);
        assertArrayEquals(new byte[] { 1, 2 }, result);
    }

    @Test
    public void testSymmetricDifferenceShortArray() {
        short[] a = { 1, 2, 2, 3, 4 };
        short[] b = { 2, 5, 1 };
        short[] result = N.symmetricDifference(a, b);
        assertArrayEquals(new short[] { 2, 3, 4, 5 }, result);
    }

    @Test
    public void testSymmetricDifferenceShortArrayNull() {
        short[] a = null;
        short[] b = { 1, 2 };
        short[] result = N.symmetricDifference(a, b);
        assertArrayEquals(new short[] { 1, 2 }, result);
    }

    @Test
    public void testSymmetricDifferenceIntArray() {
        int[] a = { 0, 1, 2, 2, 3 };
        int[] b = { 2, 5, 1 };
        int[] result = N.symmetricDifference(a, b);
        assertArrayEquals(new int[] { 0, 2, 3, 5 }, result);
    }

    @Test
    public void testSymmetricDifferenceIntArrayNull() {
        int[] a = null;
        int[] b = { 1, 2 };
        int[] result = N.symmetricDifference(a, b);
        assertArrayEquals(new int[] { 1, 2 }, result);
    }

    @Test
    public void testSymmetricDifferenceIntArrayBothNull() {
        int[] a = null;
        int[] b = null;
        int[] result = N.symmetricDifference(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testSymmetricDifferenceLongArray() {
        long[] a = { 1L, 2L, 2L, 3L, 4L };
        long[] b = { 2L, 5L, 1L };
        long[] result = N.symmetricDifference(a, b);
        assertArrayEquals(new long[] { 2L, 3L, 4L, 5L }, result);
    }

    @Test
    public void testSymmetricDifferenceLongArrayNull() {
        long[] a = null;
        long[] b = { 1L, 2L };
        long[] result = N.symmetricDifference(a, b);
        assertArrayEquals(new long[] { 1L, 2L }, result);
    }

    @Test
    public void testSymmetricDifferenceFloatArray() {
        float[] a = { 1.5f, 2.5f, 2.5f, 3.5f };
        float[] b = { 2.5f, 4.5f, 1.5f };
        float[] result = N.symmetricDifference(a, b);
        assertArrayEquals(new float[] { 2.5f, 3.5f, 4.5f }, result, 0.001f);
    }

    @Test
    public void testSymmetricDifferenceFloatArrayNull() {
        float[] a = null;
        float[] b = { 1.5f, 2.5f };
        float[] result = N.symmetricDifference(a, b);
        assertArrayEquals(new float[] { 1.5f, 2.5f }, result, 0.001f);
    }

    @Test
    public void testSymmetricDifferenceDoubleArray() {
        double[] a = { 1.1, 2.2, 2.2, 3.3 };
        double[] b = { 2.2, 4.4, 1.1 };
        double[] result = N.symmetricDifference(a, b);
        assertArrayEquals(new double[] { 2.2, 3.3, 4.4 }, result, 0.001);
    }

    @Test
    public void testSymmetricDifferenceDoubleArrayNull() {
        double[] a = null;
        double[] b = { 1.1, 2.2 };
        double[] result = N.symmetricDifference(a, b);
        assertArrayEquals(new double[] { 1.1, 2.2 }, result, 0.001);
    }

    @Test
    public void testSymmetricDifferenceObjectArray() {
        String[] a = { "A", "B", "B", "C", "D" };
        String[] b = { "B", "E", "A" };
        List<String> result = N.symmetricDifference(a, b);
        assertEquals(Arrays.asList("B", "C", "D", "E"), result);
    }

    @Test
    public void testSymmetricDifferenceObjectArrayNull() {
        String[] a = null;
        String[] b = { "A", "B" };
        List<String> result = N.symmetricDifference(a, b);
        assertEquals(Arrays.asList("A", "B"), result);
    }

    @Test
    public void testSymmetricDifferenceObjectArrayBothNull() {
        String[] a = null;
        String[] b = null;
        List<String> result = N.symmetricDifference(a, b);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSymmetricDifferenceCollection() {
        List<String> a = Arrays.asList("A", "B", "B", "C", "D");
        List<String> b = Arrays.asList("B", "E", "A");
        List<String> result = N.symmetricDifference(a, b);
        assertEquals(Arrays.asList("B", "C", "D", "E"), result);
    }

    @Test
    public void testSymmetricDifferenceCollectionUnequal() {
        List<Integer> a = Arrays.asList(1, 2, 2);
        List<Integer> b = Arrays.asList(2, 2, 2);
        List<Integer> result = N.symmetricDifference(a, b);
        assertEquals(Arrays.asList(1, 2), result);
    }

    @Test
    public void testSymmetricDifferenceCollectionNull() {
        List<String> a = null;
        List<String> b = Arrays.asList("A", "B");
        List<String> result = N.symmetricDifference(a, b);
        assertEquals(Arrays.asList("A", "B"), result);
    }

    @Test
    public void testSymmetricDifferenceCollectionBothNull() {
        List<String> a = null;
        List<String> b = null;
        List<String> result = N.symmetricDifference(a, b);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSymmetricDifferenceCollectionSecondNull() {
        List<String> a = Arrays.asList("A", "B", "C");
        List<String> b = null;
        List<String> result = N.symmetricDifference(a, b);
        assertEquals(Arrays.asList("A", "B", "C"), result);
    }

    @Test
    public void testReplaceIfBooleanArray() {
        boolean[] a = { true, false, true, false, true };
        int count = N.replaceIf(a, x -> x, false);
        assertEquals(3, count);
        assertArrayEquals(new boolean[] { false, false, false, false, false }, a);
    }

    @Test
    public void testReplaceIfBooleanArrayNoPredicate() {
        boolean[] a = { true, false, true };
        int count = N.replaceIf(a, x -> false, true);
        assertEquals(0, count);
        assertArrayEquals(new boolean[] { true, false, true }, a);
    }

    @Test
    public void testReplaceIfBooleanArrayNull() {
        boolean[] a = null;
        int count = N.replaceIf(a, x -> true, false);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceIfBooleanArrayEmpty() {
        boolean[] a = {};
        int count = N.replaceIf(a, x -> true, false);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceIfCharArray() {
        char[] a = { 'a', 'b', 'c', 'd', 'e' };
        int count = N.replaceIf(a, x -> x < 'c', 'x');
        assertEquals(2, count);
        assertArrayEquals(new char[] { 'x', 'x', 'c', 'd', 'e' }, a);
    }

    @Test
    public void testReplaceIfCharArrayNull() {
        char[] a = null;
        int count = N.replaceIf(a, x -> true, 'x');
        assertEquals(0, count);
    }

    @Test
    public void testReplaceIfByteArray() {
        byte[] a = { 1, 2, 3, 4, 5 };
        int count = N.replaceIf(a, x -> x % 2 == 0, (byte) 0);
        assertEquals(2, count);
        assertArrayEquals(new byte[] { 1, 0, 3, 0, 5 }, a);
    }

    @Test
    public void testReplaceIfByteArrayNull() {
        byte[] a = null;
        int count = N.replaceIf(a, x -> true, (byte) 0);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceIfShortArray() {
        short[] a = { 10, 20, 30, 40, 50 };
        int count = N.replaceIf(a, x -> x > 25, (short) 99);
        assertEquals(3, count);
        assertArrayEquals(new short[] { 10, 20, 99, 99, 99 }, a);
    }

    @Test
    public void testReplaceIfShortArrayNull() {
        short[] a = null;
        int count = N.replaceIf(a, x -> true, (short) 0);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceIfIntArray() {
        int[] a = { 1, 2, 3, 4, 5, 6, 7, 8 };
        int count = N.replaceIf(a, x -> x % 3 == 0, 0);
        assertEquals(2, count);
        assertArrayEquals(new int[] { 1, 2, 0, 4, 5, 0, 7, 8 }, a);
    }

    @Test
    public void testReplaceIfIntArrayNull() {
        int[] a = null;
        int count = N.replaceIf(a, x -> true, 0);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceIfLongArray() {
        long[] a = { 100L, 200L, 300L, 400L };
        int count = N.replaceIf(a, x -> x >= 300L, 0L);
        assertEquals(2, count);
        assertArrayEquals(new long[] { 100L, 200L, 0L, 0L }, a);
    }

    @Test
    public void testReplaceIfLongArrayNull() {
        long[] a = null;
        int count = N.replaceIf(a, x -> true, 0L);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceIfFloatArray() {
        float[] a = { 1.5f, 2.5f, 3.5f, 4.5f };
        int count = N.replaceIf(a, x -> x > 3.0f, 0.0f);
        assertEquals(2, count);
        assertArrayEquals(new float[] { 1.5f, 2.5f, 0.0f, 0.0f }, a);
    }

    @Test
    public void testReplaceIfFloatArrayNull() {
        float[] a = null;
        int count = N.replaceIf(a, x -> true, 0.0f);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceIfDoubleArray() {
        double[] a = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        int count = N.replaceIf(a, x -> x < 3.0, 99.0);
        assertEquals(2, count);
        assertArrayEquals(new double[] { 99.0, 99.0, 3.0, 4.0, 5.0 }, a);
    }

    @Test
    public void testReplaceIfDoubleArrayNull() {
        double[] a = null;
        int count = N.replaceIf(a, x -> true, 0.0);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceIfObjectArray() {
        String[] a = { "apple", "banana", "cherry", "date" };
        int count = N.replaceIf(a, x -> x.startsWith("b") || x.startsWith("c"), "REPLACED");
        assertEquals(2, count);
        assertArrayEquals(new String[] { "apple", "REPLACED", "REPLACED", "date" }, a);
    }

    @Test
    public void testReplaceIfObjectArrayNull() {
        String[] a = null;
        int count = N.replaceIf(a, x -> true, "REPLACED");
        assertEquals(0, count);
    }

    @Test
    public void testReplaceIfObjectArrayEmpty() {
        String[] a = {};
        int count = N.replaceIf(a, x -> true, "REPLACED");
        assertEquals(0, count);
    }

    @Test
    public void testReplaceIfList() {
        List<String> list = new ArrayList<>(Arrays.asList("apple", "banana", "cherry", "date"));
        int count = N.replaceIf(list, x -> x.length() > 5, "LONG");
        assertEquals(2, count);
        assertEquals(Arrays.asList("apple", "LONG", "LONG", "date"), list);
    }

    @Test
    public void testReplaceIfListNull() {
        List<String> list = null;
        int count = N.replaceIf(list, x -> true, "REPLACED");
        assertEquals(0, count);
    }

    @Test
    public void testReplaceIfListEmpty() {
        List<String> list = new ArrayList<>();
        int count = N.replaceIf(list, x -> true, "REPLACED");
        assertEquals(0, count);
    }

    @Test
    public void testReplaceAllBooleanArray() {
        boolean[] a = { true, false, true, false, true };
        int count = N.replaceAll(a, true, false);
        assertEquals(3, count);
        assertArrayEquals(new boolean[] { false, false, false, false, false }, a);
    }

    @Test
    public void testReplaceAllBooleanArrayNoMatch() {
        boolean[] a = { true, true, true };
        int count = N.replaceAll(a, false, true);
        assertEquals(0, count);
        assertArrayEquals(new boolean[] { true, true, true }, a);
    }

    @Test
    public void testReplaceAllBooleanArrayNull() {
        boolean[] a = null;
        int count = N.replaceAll(a, true, false);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceAllCharArray() {
        char[] a = { 'a', 'b', 'a', 'c', 'a' };
        int count = N.replaceAll(a, 'a', 'x');
        assertEquals(3, count);
        assertArrayEquals(new char[] { 'x', 'b', 'x', 'c', 'x' }, a);
    }

    @Test
    public void testReplaceAllCharArrayNull() {
        char[] a = null;
        int count = N.replaceAll(a, 'a', 'x');
        assertEquals(0, count);
    }

    @Test
    public void testReplaceAllByteArray() {
        byte[] a = { 1, 2, 1, 3, 1 };
        int count = N.replaceAll(a, (byte) 1, (byte) 9);
        assertEquals(3, count);
        assertArrayEquals(new byte[] { 9, 2, 9, 3, 9 }, a);
    }

    @Test
    public void testReplaceAllByteArrayNull() {
        byte[] a = null;
        int count = N.replaceAll(a, (byte) 1, (byte) 9);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceAllShortArray() {
        short[] a = { 10, 20, 10, 30, 10 };
        int count = N.replaceAll(a, (short) 10, (short) 99);
        assertEquals(3, count);
        assertArrayEquals(new short[] { 99, 20, 99, 30, 99 }, a);
    }

    @Test
    public void testReplaceAllShortArrayNull() {
        short[] a = null;
        int count = N.replaceAll(a, (short) 10, (short) 99);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceAllIntArray() {
        int[] a = { 1, 2, 3, 2, 4, 2, 5 };
        int count = N.replaceAll(a, 2, 99);
        assertEquals(3, count);
        assertArrayEquals(new int[] { 1, 99, 3, 99, 4, 99, 5 }, a);
    }

    @Test
    public void testReplaceAllIntArrayNull() {
        int[] a = null;
        int count = N.replaceAll(a, 2, 99);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceAllLongArray() {
        long[] a = { 100L, 200L, 100L, 300L };
        int count = N.replaceAll(a, 100L, 999L);
        assertEquals(2, count);
        assertArrayEquals(new long[] { 999L, 200L, 999L, 300L }, a);
    }

    @Test
    public void testReplaceAllLongArrayNull() {
        long[] a = null;
        int count = N.replaceAll(a, 100L, 999L);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceAllFloatArray() {
        float[] a = { 1.5f, 2.5f, 1.5f, 3.5f };
        int count = N.replaceAll(a, 1.5f, 9.9f);
        assertEquals(2, count);
        assertArrayEquals(new float[] { 9.9f, 2.5f, 9.9f, 3.5f }, a);
    }

    @Test
    public void testReplaceAllFloatArrayNull() {
        float[] a = null;
        int count = N.replaceAll(a, 1.5f, 9.9f);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceAllDoubleArray() {
        double[] a = { 1.0, 2.0, 1.0, 3.0 };
        int count = N.replaceAll(a, 1.0, 99.0);
        assertEquals(2, count);
        assertArrayEquals(new double[] { 99.0, 2.0, 99.0, 3.0 }, a);
    }

    @Test
    public void testReplaceAllDoubleArrayNull() {
        double[] a = null;
        int count = N.replaceAll(a, 1.0, 99.0);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceAllObjectArray() {
        String[] a = { "apple", "banana", "apple", "cherry" };
        int count = N.replaceAll(a, "apple", "FRUIT");
        assertEquals(2, count);
        assertArrayEquals(new String[] { "FRUIT", "banana", "FRUIT", "cherry" }, a);
    }

    @Test
    public void testReplaceAllObjectArrayNull() {
        String[] a = null;
        int count = N.replaceAll(a, "apple", "FRUIT");
        assertEquals(0, count);
    }

    @Test
    public void testReplaceAllList() {
        List<String> list = new ArrayList<>(Arrays.asList("apple", "banana", "apple", "cherry"));
        int count = N.replaceAll(list, "apple", "FRUIT");
        assertEquals(2, count);
        assertEquals(Arrays.asList("FRUIT", "banana", "FRUIT", "cherry"), list);
    }

    @Test
    public void testReplaceAllListNull() {
        List<String> list = null;
        int count = N.replaceAll(list, "apple", "FRUIT");
        assertEquals(0, count);
    }

    @Test
    public void testReplaceAllBooleanArrayOperator() {
        boolean[] a = { true, false, true, false };
        N.replaceAll(a, x -> !x);
        assertArrayEquals(new boolean[] { false, true, false, true }, a);
    }

    @Test
    public void testReplaceAllBooleanArrayOperatorNull() {
        boolean[] a = null;
        N.replaceAll(a, x -> !x);
    }

    @Test
    public void testReplaceAllCharArrayOperator() {
        char[] a = { 'a', 'b', 'c' };
        N.replaceAll(a, x -> (char) (x + 1));
        assertArrayEquals(new char[] { 'b', 'c', 'd' }, a);
    }

    @Test
    public void testReplaceAllCharArrayOperatorNull() {
        char[] a = null;
        N.replaceAll(a, x -> x);
    }

    @Test
    public void testReplaceAllByteArrayOperator() {
        byte[] a = { 1, 2, 3 };
        N.replaceAll(a, x -> (byte) (x * 2));
        assertArrayEquals(new byte[] { 2, 4, 6 }, a);
    }

    @Test
    public void testReplaceAllByteArrayOperatorNull() {
        byte[] a = null;
        N.replaceAll(a, x -> x);
    }

    @Test
    public void testReplaceAllShortArrayOperator() {
        short[] a = { 10, 20, 30 };
        N.replaceAll(a, x -> (short) (x + 5));
        assertArrayEquals(new short[] { 15, 25, 35 }, a);
    }

    @Test
    public void testReplaceAllShortArrayOperatorNull() {
        short[] a = null;
        N.replaceAll(a, x -> x);
    }

    @Test
    public void testReplaceAllIntArrayOperator() {
        int[] a = { 1, 2, 3, 4 };
        N.replaceAll(a, x -> x * x);
        assertArrayEquals(new int[] { 1, 4, 9, 16 }, a);
    }

    @Test
    public void testReplaceAllIntArrayOperatorNull() {
        int[] a = null;
        N.replaceAll(a, x -> x);
    }

    @Test
    public void testReplaceAllLongArrayOperator() {
        long[] a = { 100L, 200L, 300L };
        N.replaceAll(a, x -> x / 10);
        assertArrayEquals(new long[] { 10L, 20L, 30L }, a);
    }

    @Test
    public void testReplaceAllLongArrayOperatorNull() {
        long[] a = null;
        N.replaceAll(a, x -> x);
    }

    @Test
    public void testReplaceAllFloatArrayOperator() {
        float[] a = { 1.0f, 2.0f, 3.0f };
        N.replaceAll(a, x -> x * 2.0f);
        assertArrayEquals(new float[] { 2.0f, 4.0f, 6.0f }, a);
    }

    @Test
    public void testReplaceAllFloatArrayOperatorNull() {
        float[] a = null;
        N.replaceAll(a, x -> x);
    }

    @Test
    public void testReplaceAllDoubleArrayOperator() {
        double[] a = { 1.0, 2.0, 3.0 };
        N.replaceAll(a, x -> x + 10.0);
        assertArrayEquals(new double[] { 11.0, 12.0, 13.0 }, a);
    }

    @Test
    public void testReplaceAllDoubleArrayOperatorNull() {
        double[] a = null;
        N.replaceAll(a, x -> x);
    }

    @Test
    public void testReplaceAllObjectArrayOperator() {
        String[] a = { "hello", "world", "test" };
        N.replaceAll(a, String::toUpperCase);
        assertArrayEquals(new String[] { "HELLO", "WORLD", "TEST" }, a);
    }

    @Test
    public void testReplaceAllObjectArrayOperatorNull() {
        String[] a = null;
        N.replaceAll(a, x -> x);
    }

    @Test
    public void testReplaceAllListOperator() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
        N.replaceAll(list, x -> x * 10);
        assertEquals(Arrays.asList(10, 20, 30, 40), list);
    }

    @Test
    public void testReplaceAllListOperatorNull() {
        List<String> list = null;
        N.replaceAll(list, x -> x);
    }

    @Test
    public void testSetAllBooleanArray() {
        boolean[] a = new boolean[5];
        N.setAll(a, i -> i % 2 == 0);
        assertArrayEquals(new boolean[] { true, false, true, false, true }, a);
    }

    @Test
    public void testSetAllBooleanArrayNull() {
        boolean[] a = null;
        N.setAll(a, i -> true);
    }

    @Test
    public void testSetAllCharArray() {
        char[] a = new char[3];
        N.setAll(a, i -> (char) ('a' + i));
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, a);
    }

    @Test
    public void testSetAllCharArrayNull() {
        char[] a = null;
        N.setAll(a, i -> 'x');
    }

    @Test
    public void testSetAllByteArray() {
        byte[] a = new byte[4];
        N.setAll(a, i -> (byte) (i * 10));
        assertArrayEquals(new byte[] { 0, 10, 20, 30 }, a);
    }

    @Test
    public void testSetAllByteArrayNull() {
        byte[] a = null;
        N.setAll(a, i -> (byte) i);
    }

    @Test
    public void testSetAllShortArray() {
        short[] a = new short[3];
        N.setAll(a, i -> (short) (i + 100));
        assertArrayEquals(new short[] { 100, 101, 102 }, a);
    }

    @Test
    public void testSetAllShortArrayNull() {
        short[] a = null;
        N.setAll(a, i -> (short) i);
    }

    @Test
    public void testSetAllIntArray() {
        int[] a = new int[5];
        N.setAll(a, i -> i * i);
        assertArrayEquals(new int[] { 0, 1, 4, 9, 16 }, a);
    }

    @Test
    public void testSetAllIntArrayNull() {
        int[] a = null;
        N.setAll(a, i -> i);
    }

    @Test
    public void testSetAllLongArray() {
        long[] a = new long[4];
        N.setAll(a, i -> (long) i * 1000);
        assertArrayEquals(new long[] { 0L, 1000L, 2000L, 3000L }, a);
    }

    @Test
    public void testSetAllLongArrayNull() {
        long[] a = null;
        N.setAll(a, i -> (long) i);
    }

    @Test
    public void testSetAllFloatArray() {
        float[] a = new float[3];
        N.setAll(a, i -> i * 1.5f);
        assertArrayEquals(new float[] { 0.0f, 1.5f, 3.0f }, a);
    }

    @Test
    public void testSetAllFloatArrayNull() {
        float[] a = null;
        N.setAll(a, i -> (float) i);
    }

    @Test
    public void testSetAllDoubleArray() {
        double[] a = new double[3];
        N.setAll(a, i -> i * 2.5);
        assertArrayEquals(new double[] { 0.0, 2.5, 5.0 }, a);
    }

    @Test
    public void testSetAllDoubleArrayNull() {
        double[] a = null;
        N.setAll(a, i -> (double) i);
    }

    @Test
    public void testSetAllObjectArray() {
        String[] a = new String[3];
        N.setAll(a, i -> "Item" + i);
        assertArrayEquals(new String[] { "Item0", "Item1", "Item2" }, a);
    }

    @Test
    public void testSetAllObjectArrayNull() {
        String[] a = null;
        N.setAll(a, i -> "Item" + i);
    }

    @Test
    public void testSetAllList() {
        List<String> list = new ArrayList<>(Arrays.asList("", "", ""));
        N.setAll(list, i -> "Value" + i);
        assertEquals(Arrays.asList("Value0", "Value1", "Value2"), list);
    }

    @Test
    public void testSetAllListNull() {
        List<String> list = null;
        N.setAll(list, i -> "Value" + i);
    }

    @Test
    public void testCopyThenSetAllObjectArray() {
        String[] a = { "a", "b", "c" };
        String[] result = N.copyThenSetAll(a, i -> "Item" + i);
        assertArrayEquals(new String[] { "Item0", "Item1", "Item2" }, result);
        assertArrayEquals(new String[] { "a", "b", "c" }, a);
    }

    @Test
    public void testCopyThenSetAllObjectArrayNull() {
        String[] a = null;
        String[] result = N.copyThenSetAll(a, i -> "Item" + i);
        assertEquals(null, result);
    }

    @Test
    public void testCopyThenSetAllObjectArrayEmpty() {
        String[] a = {};
        String[] result = N.copyThenSetAll(a, i -> "Item" + i);
        assertEquals(0, result.length);
    }

    @Test
    public void testCopyThenReplaceAllObjectArray() {
        String[] a = { "hello", "world", "test" };
        String[] result = N.copyThenReplaceAll(a, String::toUpperCase);
        assertArrayEquals(new String[] { "HELLO", "WORLD", "TEST" }, result);
        assertArrayEquals(new String[] { "hello", "world", "test" }, a);
    }

    @Test
    public void testCopyThenReplaceAllObjectArrayNull() {
        String[] a = null;
        String[] result = N.copyThenReplaceAll(a, String::toUpperCase);
        assertEquals(null, result);
    }

    @Test
    public void testCopyThenReplaceAllObjectArrayEmpty() {
        String[] a = {};
        String[] result = N.copyThenReplaceAll(a, x -> x);
        assertEquals(0, result.length);
    }

    @Test
    public void testAddBooleanArray() {
        boolean[] a = { true, false };
        boolean[] result = N.add(a, true);
        assertArrayEquals(new boolean[] { true, false, true }, result);
        assertArrayEquals(new boolean[] { true, false }, a);
    }

    @Test
    public void testAddBooleanArrayNull() {
        boolean[] a = null;
        boolean[] result = N.add(a, true);
        assertArrayEquals(new boolean[] { true }, result);
    }

    @Test
    public void testAddBooleanArrayEmpty() {
        boolean[] a = {};
        boolean[] result = N.add(a, false);
        assertArrayEquals(new boolean[] { false }, result);
    }

    @Test
    public void testAddCharArray() {
        char[] a = { 'a', 'b' };
        char[] result = N.add(a, 'c');
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
        assertArrayEquals(new char[] { 'a', 'b' }, a);
    }

    @Test
    public void testAddCharArrayNull() {
        char[] a = null;
        char[] result = N.add(a, 'x');
        assertArrayEquals(new char[] { 'x' }, result);
    }

    @Test
    public void testAddByteArray() {
        byte[] a = { 1, 2 };
        byte[] result = N.add(a, (byte) 3);
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
        assertArrayEquals(new byte[] { 1, 2 }, a);
    }

    @Test
    public void testAddByteArrayNull() {
        byte[] a = null;
        byte[] result = N.add(a, (byte) 5);
        assertArrayEquals(new byte[] { 5 }, result);
    }

    @Test
    public void testAddShortArray() {
        short[] a = { 10, 20 };
        short[] result = N.add(a, (short) 30);
        assertArrayEquals(new short[] { 10, 20, 30 }, result);
        assertArrayEquals(new short[] { 10, 20 }, a);
    }

    @Test
    public void testAddShortArrayNull() {
        short[] a = null;
        short[] result = N.add(a, (short) 50);
        assertArrayEquals(new short[] { 50 }, result);
    }

    @Test
    public void testAddIntArray() {
        int[] a = { 1, 2, 3 };
        int[] result = N.add(a, 4);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, result);
        assertArrayEquals(new int[] { 1, 2, 3 }, a);
    }

    @Test
    public void testAddIntArrayNull() {
        int[] a = null;
        int[] result = N.add(a, 99);
        assertArrayEquals(new int[] { 99 }, result);
    }

    @Test
    public void testAddLongArray() {
        long[] a = { 100L, 200L };
        long[] result = N.add(a, 300L);
        assertArrayEquals(new long[] { 100L, 200L, 300L }, result);
        assertArrayEquals(new long[] { 100L, 200L }, a);
    }

    @Test
    public void testAddLongArrayNull() {
        long[] a = null;
        long[] result = N.add(a, 999L);
        assertArrayEquals(new long[] { 999L }, result);
    }

    @Test
    public void testAddFloatArray() {
        float[] a = { 1.5f, 2.5f };
        float[] result = N.add(a, 3.5f);
        assertArrayEquals(new float[] { 1.5f, 2.5f, 3.5f }, result);
        assertArrayEquals(new float[] { 1.5f, 2.5f }, a);
    }

    @Test
    public void testAddFloatArrayNull() {
        float[] a = null;
        float[] result = N.add(a, 9.9f);
        assertArrayEquals(new float[] { 9.9f }, result);
    }

    @Test
    public void testAddDoubleArray() {
        double[] a = { 1.0, 2.0 };
        double[] result = N.add(a, 3.0);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result);
        assertArrayEquals(new double[] { 1.0, 2.0 }, a);
    }

    @Test
    public void testAddDoubleArrayNull() {
        double[] a = null;
        double[] result = N.add(a, 99.9);
        assertArrayEquals(new double[] { 99.9 }, result);
    }

    @Test
    public void testAddStringArray() {
        String[] a = { "hello", "world" };
        String[] result = N.add(a, "test");
        assertArrayEquals(new String[] { "hello", "world", "test" }, result);
        assertArrayEquals(new String[] { "hello", "world" }, a);
    }

    @Test
    public void testAddStringArrayNull() {
        String[] a = null;
        String[] result = N.add(a, "new");
        assertArrayEquals(new String[] { "new" }, result);
    }

    @Test
    public void testAddObjectArray() {
        Integer[] a = { 1, 2, 3 };
        Integer[] result = N.add(a, 4);
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 }, result);
        assertArrayEquals(new Integer[] { 1, 2, 3 }, a);
    }

    @Test
    public void testAddAllBooleanArray() {
        boolean[] a = { true, false };
        boolean[] result = N.addAll(a, true, true);
        assertArrayEquals(new boolean[] { true, false, true, true }, result);
        assertArrayEquals(new boolean[] { true, false }, a);
    }

    @Test
    public void testAddAllBooleanArrayNull() {
        boolean[] a = null;
        boolean[] result = N.addAll(a, true, false);
        assertArrayEquals(new boolean[] { true, false }, result);
    }

    @Test
    public void testAddAllBooleanArrayEmpty() {
        boolean[] a = { true };
        boolean[] result = N.addAll(a);
        assertArrayEquals(new boolean[] { true }, result);
    }

    @Test
    public void testAddAllCharArray() {
        char[] a = { 'a', 'b' };
        char[] result = N.addAll(a, 'c', 'd');
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, result);
        assertArrayEquals(new char[] { 'a', 'b' }, a);
    }

    @Test
    public void testAddAllCharArrayNull() {
        char[] a = null;
        char[] result = N.addAll(a, 'x', 'y');
        assertArrayEquals(new char[] { 'x', 'y' }, result);
    }

    @Test
    public void testAddAllByteArray() {
        byte[] a = { 1, 2 };
        byte[] result = N.addAll(a, (byte) 3, (byte) 4);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, result);
        assertArrayEquals(new byte[] { 1, 2 }, a);
    }

    @Test
    public void testAddAllByteArrayNull() {
        byte[] a = null;
        byte[] result = N.addAll(a, (byte) 5, (byte) 6);
        assertArrayEquals(new byte[] { 5, 6 }, result);
    }

    @Test
    public void testAddAllShortArray() {
        short[] a = { 10, 20 };
        short[] result = N.addAll(a, (short) 30, (short) 40);
        assertArrayEquals(new short[] { 10, 20, 30, 40 }, result);
        assertArrayEquals(new short[] { 10, 20 }, a);
    }

    @Test
    public void testAddAllShortArrayNull() {
        short[] a = null;
        short[] result = N.addAll(a, (short) 50, (short) 60);
        assertArrayEquals(new short[] { 50, 60 }, result);
    }

    @Test
    public void testAddAllIntArray() {
        int[] a = { 1, 2 };
        int[] result = N.addAll(a, 3, 4, 5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, result);
        assertArrayEquals(new int[] { 1, 2 }, a);
    }

    @Test
    public void testAddAllIntArrayNull() {
        int[] a = null;
        int[] result = N.addAll(a, 99, 100);
        assertArrayEquals(new int[] { 99, 100 }, result);
    }

    @Test
    public void testAddAllLongArray() {
        long[] a = { 100L, 200L };
        long[] result = N.addAll(a, 300L, 400L);
        assertArrayEquals(new long[] { 100L, 200L, 300L, 400L }, result);
        assertArrayEquals(new long[] { 100L, 200L }, a);
    }

    @Test
    public void testAddAllLongArrayNull() {
        long[] a = null;
        long[] result = N.addAll(a, 999L);
        assertArrayEquals(new long[] { 999L }, result);
    }

    @Test
    public void testAddAllFloatArray() {
        float[] a = { 1.5f, 2.5f };
        float[] result = N.addAll(a, 3.5f, 4.5f);
        assertArrayEquals(new float[] { 1.5f, 2.5f, 3.5f, 4.5f }, result);
        assertArrayEquals(new float[] { 1.5f, 2.5f }, a);
    }

    @Test
    public void testAddAllFloatArrayNull() {
        float[] a = null;
        float[] result = N.addAll(a, 9.9f);
        assertArrayEquals(new float[] { 9.9f }, result);
    }

    @Test
    public void testAddAllDoubleArray() {
        double[] a = { 1.0, 2.0 };
        double[] result = N.addAll(a, 3.0, 4.0);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, result);
        assertArrayEquals(new double[] { 1.0, 2.0 }, a);
    }

    @Test
    public void testAddAllDoubleArrayNull() {
        double[] a = null;
        double[] result = N.addAll(a, 99.9);
        assertArrayEquals(new double[] { 99.9 }, result);
    }

    @Test
    public void testAddAllStringArray() {
        String[] a = { "hello", "world" };
        String[] result = N.addAll(a, "foo", "bar");
        assertArrayEquals(new String[] { "hello", "world", "foo", "bar" }, result);
        assertArrayEquals(new String[] { "hello", "world" }, a);
    }

    @Test
    public void testAddAllStringArrayNull() {
        String[] a = null;
        String[] result = N.addAll(a, "new");
        assertArrayEquals(new String[] { "new" }, result);
    }

    @Test
    public void testAddAllObjectArray() {
        Integer[] a = { 1, 2 };
        Integer[] result = N.addAll(a, 3, 4);
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 }, result);
        assertArrayEquals(new Integer[] { 1, 2 }, a);
    }

    @Test
    public void testAddAllCollection() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        boolean changed = N.addAll(list, "c", "d");
        assertTrue(changed);
        assertEquals(Arrays.asList("a", "b", "c", "d"), list);
    }

    @Test
    public void testAddAllCollectionNoElements() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        boolean changed = N.addAll(list);
        assertTrue(!changed);
        assertEquals(Arrays.asList("a", "b"), list);
    }

    @Test
    public void testAddAllCollectionIterable() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        Iterable<String> toAdd = Arrays.asList("c", "d");
        boolean changed = N.addAll(list, toAdd);
        assertTrue(changed);
        assertEquals(Arrays.asList("a", "b", "c", "d"), list);
    }

    @Test
    public void testAddAllCollectionIterableEmpty() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        Iterable<String> toAdd = new ArrayList<>();
        boolean changed = N.addAll(list, toAdd);
        assertTrue(!changed);
        assertEquals(Arrays.asList("a", "b"), list);
    }

    @Test
    public void testAddAllCollectionIterator() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        Iterator<String> toAdd = Arrays.asList("c", "d").iterator();
        boolean changed = N.addAll(list, toAdd);
        assertTrue(changed);
        assertEquals(Arrays.asList("a", "b", "c", "d"), list);
    }

    @Test
    public void testAddAllCollectionIteratorEmpty() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        Iterator<String> toAdd = Collections.emptyIterator();
        boolean changed = N.addAll(list, toAdd);
        assertTrue(!changed);
        assertEquals(Arrays.asList("a", "b"), list);
    }

    @Test
    public void testSliceArray() {
        String[] words = { "a", "b", "c", "d", "e" };
        ImmutableList<String> slice = N.slice(words, 1, 4);

        assertEquals(3, slice.size());
        assertEquals(Arrays.asList("b", "c", "d"), slice);
    }

    @Test
    public void testSliceArrayFullRange() {
        Integer[] numbers = { 1, 2, 3, 4, 5 };
        ImmutableList<Integer> slice = N.slice(numbers, 0, 5);

        assertEquals(5, slice.size());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), slice);
    }

    @Test
    public void testSliceArrayEmpty() {
        String[] empty = {};
        ImmutableList<String> slice = N.slice(empty, 0, 0);

        assertTrue(slice.isEmpty());
    }

    @Test
    public void testSliceArrayNull() {
        String[] nullArray = null;
        ImmutableList<String> slice = N.slice(nullArray, 0, 0);

        assertTrue(slice.isEmpty());
    }

    @Test
    public void testSliceArraySingleElement() {
        Integer[] single = { 42 };
        ImmutableList<Integer> slice = N.slice(single, 0, 1);

        assertEquals(1, slice.size());
        assertEquals(42, slice.get(0));
    }

    @Test
    public void testSliceArrayInvalidIndices() {
        String[] words = { "a", "b", "c" };
        assertThrows(IndexOutOfBoundsException.class, () -> N.slice(words, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.slice(words, 0, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> N.slice(words, 2, 1));
    }

    @Test
    public void testSliceList() {
        List<String> words = Arrays.asList("a", "b", "c", "d", "e");
        ImmutableList<String> slice = N.slice(words, 1, 4);

        assertEquals(3, slice.size());
        assertEquals(Arrays.asList("b", "c", "d"), slice);
    }

    @Test
    public void testSliceListFullRange() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        ImmutableList<Integer> slice = N.slice(numbers, 0, 5);

        assertEquals(5, slice.size());
    }

    @Test
    public void testSliceListEmpty() {
        List<String> empty = new ArrayList<>();
        ImmutableList<String> slice = N.slice(empty, 0, 0);

        assertTrue(slice.isEmpty());
    }

    @Test
    public void testSliceListNull() {
        List<String> nullList = null;
        ImmutableList<String> slice = N.slice(nullList, 0, 0);

        assertTrue(slice.isEmpty());
    }

    @Test
    public void testSliceListInvalidIndices() {
        List<String> words = Arrays.asList("a", "b", "c");
        assertThrows(IndexOutOfBoundsException.class, () -> N.slice(words, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.slice(words, 0, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> N.slice(words, 2, 1));
    }

    @Test
    public void testSliceCollection() {
        Set<String> words = new HashSet<>(Arrays.asList("a", "b", "c", "d", "e"));
        ImmutableCollection<String> slice = N.slice(words, 0, 3);

        assertEquals(3, slice.size());
    }

    @Test
    public void testSliceCollectionList() {
        Collection<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        ImmutableCollection<Integer> slice = N.slice(numbers, 1, 4);

        assertEquals(3, slice.size());
    }

    @Test
    public void testSliceCollectionEmpty() {
        Collection<String> empty = new ArrayList<>();
        ImmutableCollection<String> slice = N.slice(empty, 0, 0);

        assertTrue(slice.isEmpty());
    }

    @Test
    public void testSliceCollectionNull() {
        Collection<String> nullColl = null;
        ImmutableCollection<String> slice = N.slice(nullColl, 0, 0);

        assertTrue(slice.isEmpty());
    }

    @Test
    public void testSliceCollectionInvalidIndices() {
        Collection<String> words = Arrays.asList("a", "b", "c");
        assertThrows(IndexOutOfBoundsException.class, () -> N.slice(words, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.slice(words, 0, 5));
    }

    @Test
    public void testSliceIterator() {
        List<String> words = Arrays.asList("a", "b", "c", "d", "e");
        ObjIterator<String> slice = N.slice(words.iterator(), 1, 4);

        List<String> result = new ArrayList<>();
        while (slice.hasNext()) {
            result.add(slice.next());
        }
        assertEquals(Arrays.asList("b", "c", "d"), result);
    }

    @Test
    public void testSliceIteratorFromStart() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        ObjIterator<Integer> slice = N.slice(numbers.iterator(), 0, 3);

        List<Integer> result = new ArrayList<>();
        while (slice.hasNext()) {
            result.add(slice.next());
        }
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testSliceIteratorEmpty() {
        List<String> words = Arrays.asList("a", "b", "c");
        ObjIterator<String> slice = N.slice(words.iterator(), 2, 2);

        assertTrue(!slice.hasNext());
    }

    @Test
    public void testSliceIteratorNull() {
        Iterator<String> nullIter = null;
        ObjIterator<String> slice = N.slice(nullIter, 0, 5);

        assertTrue(!slice.hasNext());
    }

    @Test
    public void testSliceIteratorInvalidIndices() {
        List<String> words = Arrays.asList("a", "b", "c");
        assertThrows(IllegalArgumentException.class, () -> N.slice(words.iterator(), -1, 2));
        assertThrows(IllegalArgumentException.class, () -> N.slice(words.iterator(), 2, 1));
    }

    @Test
    public void testSplitBooleanArray() {
        boolean[] flags = { true, false, true, false, true, false, true };
        List<boolean[]> chunks = N.split(flags, 3);

        assertEquals(3, chunks.size());
        assertArrayEquals(new boolean[] { true, false, true }, chunks.get(0));
        assertArrayEquals(new boolean[] { false, true, false }, chunks.get(1));
        assertArrayEquals(new boolean[] { true }, chunks.get(2));
    }

    @Test
    public void testSplitBooleanArrayEvenSplit() {
        boolean[] flags = { true, false, true, false };
        List<boolean[]> chunks = N.split(flags, 2);

        assertEquals(2, chunks.size());
        assertArrayEquals(new boolean[] { true, false }, chunks.get(0));
        assertArrayEquals(new boolean[] { true, false }, chunks.get(1));
    }

    @Test
    public void testSplitBooleanArrayEmpty() {
        boolean[] empty = {};
        List<boolean[]> chunks = N.split(empty, 3);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitBooleanArrayNull() {
        boolean[] nullArray = null;
        List<boolean[]> chunks = N.split(nullArray, 3);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitBooleanArrayLargeChunkSize() {
        boolean[] flags = { true, false };
        List<boolean[]> chunks = N.split(flags, 5);

        assertEquals(1, chunks.size());
        assertArrayEquals(new boolean[] { true, false }, chunks.get(0));
    }

    @Test
    public void testSplitBooleanArrayInvalidChunkSize() {
        boolean[] flags = { true, false, true };
        assertThrows(IllegalArgumentException.class, () -> N.split(flags, 0));
        assertThrows(IllegalArgumentException.class, () -> N.split(flags, -1));
    }

    @Test
    public void testSplitBooleanArrayWithRange() {
        boolean[] flags = { true, false, true, false, true, false, true, false };
        List<boolean[]> chunks = N.split(flags, 1, 7, 2);

        assertEquals(3, chunks.size());
        assertArrayEquals(new boolean[] { false, true }, chunks.get(0));
        assertArrayEquals(new boolean[] { false, true }, chunks.get(1));
        assertArrayEquals(new boolean[] { false, true }, chunks.get(2));
    }

    @Test
    public void testSplitBooleanArrayWithRangePartial() {
        boolean[] flags = { true, true, false, false, true };
        List<boolean[]> chunks = N.split(flags, 1, 4, 2);

        assertEquals(2, chunks.size());
        assertArrayEquals(new boolean[] { true, false }, chunks.get(0));
        assertArrayEquals(new boolean[] { false }, chunks.get(1));
    }

    @Test
    public void testSplitBooleanArrayWithRangeEmpty() {
        boolean[] flags = { true, false, true };
        List<boolean[]> chunks = N.split(flags, 1, 1, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitCharArray() {
        char[] letters = { 'a', 'b', 'c', 'd', 'e' };
        List<char[]> chunks = N.split(letters, 2);

        assertEquals(3, chunks.size());
        assertArrayEquals(new char[] { 'a', 'b' }, chunks.get(0));
        assertArrayEquals(new char[] { 'c', 'd' }, chunks.get(1));
        assertArrayEquals(new char[] { 'e' }, chunks.get(2));
    }

    @Test
    public void testSplitCharArrayEmpty() {
        char[] empty = {};
        List<char[]> chunks = N.split(empty, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitCharArrayNull() {
        char[] nullArray = null;
        List<char[]> chunks = N.split(nullArray, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitCharArrayWithRange() {
        char[] letters = { 'a', 'b', 'c', 'd', 'e', 'f' };
        List<char[]> chunks = N.split(letters, 1, 5, 2);

        assertEquals(2, chunks.size());
        assertArrayEquals(new char[] { 'b', 'c' }, chunks.get(0));
        assertArrayEquals(new char[] { 'd', 'e' }, chunks.get(1));
    }

    @Test
    public void testSplitByteArray() {
        byte[] data = { 1, 2, 3, 4, 5, 6, 7 };
        List<byte[]> chunks = N.split(data, 3);

        assertEquals(3, chunks.size());
        assertArrayEquals(new byte[] { 1, 2, 3 }, chunks.get(0));
        assertArrayEquals(new byte[] { 4, 5, 6 }, chunks.get(1));
        assertArrayEquals(new byte[] { 7 }, chunks.get(2));
    }

    @Test
    public void testSplitByteArrayEmpty() {
        byte[] empty = {};
        List<byte[]> chunks = N.split(empty, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitByteArrayNull() {
        byte[] nullArray = null;
        List<byte[]> chunks = N.split(nullArray, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitByteArrayWithRange() {
        byte[] data = { 1, 2, 3, 4, 5, 6 };
        List<byte[]> chunks = N.split(data, 0, 5, 2);

        assertEquals(3, chunks.size());
        assertArrayEquals(new byte[] { 1, 2 }, chunks.get(0));
        assertArrayEquals(new byte[] { 3, 4 }, chunks.get(1));
        assertArrayEquals(new byte[] { 5 }, chunks.get(2));
    }

    @Test
    public void testSplitShortArray() {
        short[] data = { 10, 20, 30, 40, 50 };
        List<short[]> chunks = N.split(data, 2);

        assertEquals(3, chunks.size());
        assertArrayEquals(new short[] { 10, 20 }, chunks.get(0));
        assertArrayEquals(new short[] { 30, 40 }, chunks.get(1));
        assertArrayEquals(new short[] { 50 }, chunks.get(2));
    }

    @Test
    public void testSplitShortArrayEmpty() {
        short[] empty = {};
        List<short[]> chunks = N.split(empty, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitShortArrayNull() {
        short[] nullArray = null;
        List<short[]> chunks = N.split(nullArray, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitShortArrayWithRange() {
        short[] data = { 10, 20, 30, 40, 50, 60 };
        List<short[]> chunks = N.split(data, 1, 5, 2);

        assertEquals(2, chunks.size());
        assertArrayEquals(new short[] { 20, 30 }, chunks.get(0));
        assertArrayEquals(new short[] { 40, 50 }, chunks.get(1));
    }

    @Test
    public void testSplitIntArray() {
        int[] numbers = { 1, 2, 3, 4, 5, 6, 7 };
        List<int[]> chunks = N.split(numbers, 3);

        assertEquals(3, chunks.size());
        assertArrayEquals(new int[] { 1, 2, 3 }, chunks.get(0));
        assertArrayEquals(new int[] { 4, 5, 6 }, chunks.get(1));
        assertArrayEquals(new int[] { 7 }, chunks.get(2));
    }

    @Test
    public void testSplitIntArrayEmpty() {
        int[] empty = {};
        List<int[]> chunks = N.split(empty, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitIntArrayNull() {
        int[] nullArray = null;
        List<int[]> chunks = N.split(nullArray, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitIntArrayWithRange() {
        int[] numbers = { 1, 2, 3, 4, 5, 6, 7, 8 };
        List<int[]> chunks = N.split(numbers, 2, 7, 2);

        assertEquals(3, chunks.size());
        assertArrayEquals(new int[] { 3, 4 }, chunks.get(0));
        assertArrayEquals(new int[] { 5, 6 }, chunks.get(1));
        assertArrayEquals(new int[] { 7 }, chunks.get(2));
    }

    @Test
    public void testSplitLongArray() {
        long[] numbers = { 1L, 2L, 3L, 4L, 5L };
        List<long[]> chunks = N.split(numbers, 2);

        assertEquals(3, chunks.size());
        assertArrayEquals(new long[] { 1L, 2L }, chunks.get(0));
        assertArrayEquals(new long[] { 3L, 4L }, chunks.get(1));
        assertArrayEquals(new long[] { 5L }, chunks.get(2));
    }

    @Test
    public void testSplitLongArrayEmpty() {
        long[] empty = {};
        List<long[]> chunks = N.split(empty, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitLongArrayNull() {
        long[] nullArray = null;
        List<long[]> chunks = N.split(nullArray, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitLongArrayWithRange() {
        long[] numbers = { 1L, 2L, 3L, 4L, 5L, 6L };
        List<long[]> chunks = N.split(numbers, 1, 5, 2);

        assertEquals(2, chunks.size());
        assertArrayEquals(new long[] { 2L, 3L }, chunks.get(0));
        assertArrayEquals(new long[] { 4L, 5L }, chunks.get(1));
    }

    @Test
    public void testSplitFloatArray() {
        float[] numbers = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        List<float[]> chunks = N.split(numbers, 2);

        assertEquals(3, chunks.size());
        assertArrayEquals(new float[] { 1.0f, 2.0f }, chunks.get(0));
        assertArrayEquals(new float[] { 3.0f, 4.0f }, chunks.get(1));
        assertArrayEquals(new float[] { 5.0f }, chunks.get(2));
    }

    @Test
    public void testSplitFloatArrayEmpty() {
        float[] empty = {};
        List<float[]> chunks = N.split(empty, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitFloatArrayNull() {
        float[] nullArray = null;
        List<float[]> chunks = N.split(nullArray, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitFloatArrayWithRange() {
        float[] numbers = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f };
        List<float[]> chunks = N.split(numbers, 1, 5, 2);

        assertEquals(2, chunks.size());
        assertArrayEquals(new float[] { 2.0f, 3.0f }, chunks.get(0));
        assertArrayEquals(new float[] { 4.0f, 5.0f }, chunks.get(1));
    }

    @Test
    public void testSplitDoubleArray() {
        double[] numbers = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        List<double[]> chunks = N.split(numbers, 2);

        assertEquals(3, chunks.size());
        assertArrayEquals(new double[] { 1.0, 2.0 }, chunks.get(0));
        assertArrayEquals(new double[] { 3.0, 4.0 }, chunks.get(1));
        assertArrayEquals(new double[] { 5.0 }, chunks.get(2));
    }

    @Test
    public void testSplitDoubleArrayEmpty() {
        double[] empty = {};
        List<double[]> chunks = N.split(empty, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitDoubleArrayNull() {
        double[] nullArray = null;
        List<double[]> chunks = N.split(nullArray, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitDoubleArrayWithRange() {
        double[] numbers = { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 };
        List<double[]> chunks = N.split(numbers, 1, 5, 2);

        assertEquals(2, chunks.size());
        assertArrayEquals(new double[] { 2.0, 3.0 }, chunks.get(0));
        assertArrayEquals(new double[] { 4.0, 5.0 }, chunks.get(1));
    }

    @Test
    public void testSplitObjectArray() {
        String[] words = { "a", "b", "c", "d", "e" };
        List<String[]> chunks = N.split(words, 2);

        assertEquals(3, chunks.size());
        assertArrayEquals(new String[] { "a", "b" }, chunks.get(0));
        assertArrayEquals(new String[] { "c", "d" }, chunks.get(1));
        assertArrayEquals(new String[] { "e" }, chunks.get(2));
    }

    @Test
    public void testSplitObjectArrayEmpty() {
        String[] empty = {};
        List<String[]> chunks = N.split(empty, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitObjectArrayNull() {
        String[] nullArray = null;
        List<String[]> chunks = N.split(nullArray, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitObjectArrayWithRange() {
        Integer[] numbers = { 1, 2, 3, 4, 5, 6 };
        List<Integer[]> chunks = N.split(numbers, 1, 5, 2);

        assertEquals(2, chunks.size());
        assertArrayEquals(new Integer[] { 2, 3 }, chunks.get(0));
        assertArrayEquals(new Integer[] { 4, 5 }, chunks.get(1));
    }

    @Test
    public void testSplitCollection() {
        List<String> words = Arrays.asList("a", "b", "c", "d", "e");
        List<List<String>> chunks = N.split(words, 2);

        assertEquals(3, chunks.size());
        assertEquals(Arrays.asList("a", "b"), chunks.get(0));
        assertEquals(Arrays.asList("c", "d"), chunks.get(1));
        assertEquals(Arrays.asList("e"), chunks.get(2));
    }

    @Test
    public void testSplitCollectionEmpty() {
        List<String> empty = new ArrayList<>();
        List<List<String>> chunks = N.split(empty, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitCollectionNull() {
        Collection<String> nullColl = null;
        List<List<String>> chunks = N.split(nullColl, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitCollectionWithRange() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7);
        List<List<Integer>> chunks = N.split(numbers, 1, 6, 2);

        assertEquals(3, chunks.size());
        assertEquals(Arrays.asList(2, 3), chunks.get(0));
        assertEquals(Arrays.asList(4, 5), chunks.get(1));
        assertEquals(Arrays.asList(6), chunks.get(2));
    }

    @Test
    public void testSplitIterable() {
        Iterable<String> words = Arrays.asList("a", "b", "c", "d", "e");
        List<List<String>> chunks = N.split(words, 2);

        assertEquals(3, chunks.size());
        assertEquals(Arrays.asList("a", "b"), chunks.get(0));
        assertEquals(Arrays.asList("c", "d"), chunks.get(1));
        assertEquals(Arrays.asList("e"), chunks.get(2));
    }

    @Test
    public void testSplitIterableEmpty() {
        Iterable<String> empty = new ArrayList<>();
        List<List<String>> chunks = N.split(empty, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitIterableNull() {
        Iterable<String> nullIterable = null;
        List<List<String>> chunks = N.split(nullIterable, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitIterator() {
        List<String> words = Arrays.asList("a", "b", "c", "d", "e");
        ObjIterator<List<String>> chunks = N.split(words.iterator(), 2);

        List<List<String>> result = new ArrayList<>();
        while (chunks.hasNext()) {
            result.add(chunks.next());
        }

        assertEquals(3, result.size());
        assertEquals(Arrays.asList("a", "b"), result.get(0));
        assertEquals(Arrays.asList("c", "d"), result.get(1));
        assertEquals(Arrays.asList("e"), result.get(2));
    }

    @Test
    public void testSplitIteratorEmpty() {
        Iterator<String> empty = Collections.emptyIterator();
        ObjIterator<List<String>> chunks = N.split(empty, 2);

        assertTrue(!chunks.hasNext());
    }

    @Test
    public void testSplitIteratorNull() {
        Iterator<String> nullIter = null;
        ObjIterator<List<String>> chunks = N.split(nullIter, 2);

        assertTrue(!chunks.hasNext());
    }

    @Test
    public void testSplitString() {
        String text = "abcdefg";
        List<String> chunks = N.split(text, 3);

        assertEquals(3, chunks.size());
        assertEquals("abc", chunks.get(0));
        assertEquals("def", chunks.get(1));
        assertEquals("g", chunks.get(2));
    }

    @Test
    public void testSplitStringEmpty() {
        String empty = "";
        List<String> chunks = N.split(empty, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitStringNull() {
        String nullStr = null;
        List<String> chunks = N.split(nullStr, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitStringWithRange() {
        String text = "abcdefgh";
        List<String> chunks = N.split(text, 1, 7, 2);

        assertEquals(3, chunks.size());
        assertEquals("bc", chunks.get(0));
        assertEquals("de", chunks.get(1));
        assertEquals("fg", chunks.get(2));
    }

    @Test
    public void testSplitStringLargeChunkSize() {
        String text = "hello";
        List<String> chunks = N.split(text, 10);

        assertEquals(1, chunks.size());
        assertEquals("hello", chunks.get(0));
    }

    @Test
    public void testSplitByChunkCountWithFunction() {
        int[] a = { 1, 2, 3, 4, 5, 6, 7 };
        List<int[]> chunks = N.splitByChunkCount(7, 5, (fromIndex, toIndex) -> N.copyOfRange(a, fromIndex, toIndex));

        assertEquals(5, chunks.size());
        assertArrayEquals(new int[] { 1, 2 }, chunks.get(0));
        assertArrayEquals(new int[] { 3, 4 }, chunks.get(1));
        assertArrayEquals(new int[] { 5 }, chunks.get(2));
        assertArrayEquals(new int[] { 6 }, chunks.get(3));
        assertArrayEquals(new int[] { 7 }, chunks.get(4));
    }

    @Test
    public void testSplitByChunkCountWithFunctionSizeSmallerFirst() {
        int[] a = { 1, 2, 3, 4, 5, 6, 7 };
        List<int[]> chunks = N.splitByChunkCount(7, 5, true, (fromIndex, toIndex) -> N.copyOfRange(a, fromIndex, toIndex));

        assertEquals(5, chunks.size());
        assertArrayEquals(new int[] { 1 }, chunks.get(0));
        assertArrayEquals(new int[] { 2 }, chunks.get(1));
        assertArrayEquals(new int[] { 3 }, chunks.get(2));
        assertArrayEquals(new int[] { 4, 5 }, chunks.get(3));
        assertArrayEquals(new int[] { 6, 7 }, chunks.get(4));
    }

    @Test
    public void testSplitByChunkCountWithFunctionExactDivision() {
        int[] a = { 1, 2, 3, 4, 5, 6 };
        List<int[]> chunks = N.splitByChunkCount(6, 3, (fromIndex, toIndex) -> N.copyOfRange(a, fromIndex, toIndex));

        assertEquals(3, chunks.size());
        assertArrayEquals(new int[] { 1, 2 }, chunks.get(0));
        assertArrayEquals(new int[] { 3, 4 }, chunks.get(1));
        assertArrayEquals(new int[] { 5, 6 }, chunks.get(2));
    }

    @Test
    public void testSplitByChunkCountWithFunctionSizeOne() {
        int[] a = { 1, 2, 3 };
        List<int[]> chunks = N.splitByChunkCount(3, 1, (fromIndex, toIndex) -> N.copyOfRange(a, fromIndex, toIndex));

        assertEquals(1, chunks.size());
        assertArrayEquals(new int[] { 1, 2, 3 }, chunks.get(0));
    }

    @Test
    public void testSplitByChunkCountWithFunctionMoreChunksThanSize() {
        int[] a = { 1, 2, 3 };
        List<int[]> chunks = N.splitByChunkCount(3, 5, (fromIndex, toIndex) -> N.copyOfRange(a, fromIndex, toIndex));

        assertEquals(3, chunks.size());
        assertArrayEquals(new int[] { 1 }, chunks.get(0));
        assertArrayEquals(new int[] { 2 }, chunks.get(1));
        assertArrayEquals(new int[] { 3 }, chunks.get(2));
    }

    @Test
    public void testSplitByChunkCountWithFunctionInvalidArgs() {
        assertThrows(IllegalArgumentException.class, () -> N.splitByChunkCount(-1, 5, (f, t) -> null));
        assertThrows(IllegalArgumentException.class, () -> N.splitByChunkCount(10, 0, (f, t) -> null));
    }

    @Test
    public void testSplitByChunkCountCollection() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7);
        List<List<Integer>> chunks = N.splitByChunkCount(numbers, 5);

        assertEquals(5, chunks.size());
        assertEquals(Arrays.asList(1, 2), chunks.get(0));
        assertEquals(Arrays.asList(3, 4), chunks.get(1));
        assertEquals(Arrays.asList(5), chunks.get(2));
        assertEquals(Arrays.asList(6), chunks.get(3));
        assertEquals(Arrays.asList(7), chunks.get(4));
    }

    @Test
    public void testSplitByChunkCountCollectionSizeSmallerFirst() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7);
        List<List<Integer>> chunks = N.splitByChunkCount(numbers, 5, true);

        assertEquals(5, chunks.size());
        assertEquals(Arrays.asList(1), chunks.get(0));
        assertEquals(Arrays.asList(2), chunks.get(1));
        assertEquals(Arrays.asList(3), chunks.get(2));
        assertEquals(Arrays.asList(4, 5), chunks.get(3));
        assertEquals(Arrays.asList(6, 7), chunks.get(4));
    }

    @Test
    public void testSplitByChunkCountCollectionEmpty() {
        List<String> empty = new ArrayList<>();
        List<List<String>> chunks = N.splitByChunkCount(empty, 3);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitByChunkCountCollectionNull() {
        Collection<String> nullColl = null;
        List<List<String>> chunks = N.splitByChunkCount(nullColl, 3);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitByChunkCountCollectionExactDivision() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6);
        List<List<Integer>> chunks = N.splitByChunkCount(numbers, 3);

        assertEquals(3, chunks.size());
        assertEquals(Arrays.asList(1, 2), chunks.get(0));
        assertEquals(Arrays.asList(3, 4), chunks.get(1));
        assertEquals(Arrays.asList(5, 6), chunks.get(2));
    }

    @Test
    public void testSplitByChunkCountCollectionMoreChunksThanElements() {
        List<Integer> numbers = Arrays.asList(1, 2, 3);
        List<List<Integer>> chunks = N.splitByChunkCount(numbers, 5);

        assertEquals(3, chunks.size());
        assertEquals(Arrays.asList(1), chunks.get(0));
        assertEquals(Arrays.asList(2), chunks.get(1));
        assertEquals(Arrays.asList(3), chunks.get(2));
    }

    @Test
    public void testSplitByChunkCountCollectionInvalidChunkCount() {
        List<Integer> numbers = Arrays.asList(1, 2, 3);
        assertThrows(IllegalArgumentException.class, () -> N.splitByChunkCount(numbers, 0));
        assertThrows(IllegalArgumentException.class, () -> N.splitByChunkCount(numbers, -1));
    }

    @Test
    public void testConcatBooleanArrayTwoArrays() {
        boolean[] a = { true, false };
        boolean[] b = { true, true };
        boolean[] result = N.concat(a, b);

        assertArrayEquals(new boolean[] { true, false, true, true }, result);
    }

    @Test
    public void testConcatBooleanArrayFirstEmpty() {
        boolean[] a = {};
        boolean[] b = { true, false };
        boolean[] result = N.concat(a, b);

        assertArrayEquals(new boolean[] { true, false }, result);
    }

    @Test
    public void testConcatBooleanArraySecondEmpty() {
        boolean[] a = { true, false };
        boolean[] b = {};
        boolean[] result = N.concat(a, b);

        assertArrayEquals(new boolean[] { true, false }, result);
    }

    @Test
    public void testConcatBooleanArrayBothEmpty() {
        boolean[] a = {};
        boolean[] b = {};
        boolean[] result = N.concat(a, b);

        assertEquals(0, result.length);
    }

    @Test
    public void testConcatBooleanArrayFirstNull() {
        boolean[] a = null;
        boolean[] b = { true, false };
        boolean[] result = N.concat(a, b);

        assertArrayEquals(new boolean[] { true, false }, result);
    }

    @Test
    public void testConcatBooleanArraySecondNull() {
        boolean[] a = { true, false };
        boolean[] b = null;
        boolean[] result = N.concat(a, b);

        assertArrayEquals(new boolean[] { true, false }, result);
    }

    @Test
    public void testConcatBooleanArrayBothNull() {
        boolean[] a = null;
        boolean[] b = null;
        boolean[] result = N.concat(a, b);

        assertEquals(0, result.length);
    }

    @Test
    public void testConcatBooleanArrayVarargs() {
        boolean[] a = { true };
        boolean[] b = { false };
        boolean[] c = { true, false };
        boolean[] result = N.concat(a, b, c);

        assertArrayEquals(new boolean[] { true, false, true, false }, result);
    }

    @Test
    public void testConcatBooleanArrayVarargsEmpty() {
        boolean[][] empty = {};
        boolean[] result = N.concat(empty);

        assertEquals(0, result.length);
    }

    @Test
    public void testConcatBooleanArrayVarargsSingleArray() {
        boolean[] a = { true, false };
        boolean[] result = N.concat(a);

        assertArrayEquals(new boolean[] { true, false }, result);
    }

    @Test
    public void testConcatCharArrayTwoArrays() {
        char[] a = { 'a', 'b' };
        char[] b = { 'c', 'd' };
        char[] result = N.concat(a, b);

        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, result);
    }

    @Test
    public void testConcatCharArrayEmpty() {
        char[] a = {};
        char[] b = { 'x', 'y' };
        char[] result = N.concat(a, b);

        assertArrayEquals(new char[] { 'x', 'y' }, result);
    }

    @Test
    public void testConcatCharArrayNull() {
        char[] a = null;
        char[] b = { 'x' };
        char[] result = N.concat(a, b);

        assertArrayEquals(new char[] { 'x' }, result);
    }

    @Test
    public void testConcatCharArrayVarargs() {
        char[] a = { 'a', 'b' };
        char[] b = { 'c' };
        char[] c = { 'd', 'e' };
        char[] result = N.concat(a, b, c);

        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, result);
    }

    @Test
    public void testConcatByteArrayTwoArrays() {
        byte[] a = { 1, 2 };
        byte[] b = { 3, 4 };
        byte[] result = N.concat(a, b);

        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testConcatByteArrayEmpty() {
        byte[] a = {};
        byte[] b = { 5, 6 };
        byte[] result = N.concat(a, b);

        assertArrayEquals(new byte[] { 5, 6 }, result);
    }

    @Test
    public void testConcatByteArrayNull() {
        byte[] a = null;
        byte[] b = { 1 };
        byte[] result = N.concat(a, b);

        assertArrayEquals(new byte[] { 1 }, result);
    }

    @Test
    public void testConcatByteArrayVarargs() {
        byte[] a = { 1 };
        byte[] b = { 2, 3 };
        byte[] c = { 4, 5, 6 };
        byte[] result = N.concat(a, b, c);

        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 }, result);
    }

    @Test
    public void testConcatShortArrayTwoArrays() {
        short[] a = { 10, 20 };
        short[] b = { 30, 40 };
        short[] result = N.concat(a, b);

        assertArrayEquals(new short[] { 10, 20, 30, 40 }, result);
    }

    @Test
    public void testConcatShortArrayEmpty() {
        short[] a = {};
        short[] b = { 50 };
        short[] result = N.concat(a, b);

        assertArrayEquals(new short[] { 50 }, result);
    }

    @Test
    public void testConcatShortArrayNull() {
        short[] a = null;
        short[] b = { 10 };
        short[] result = N.concat(a, b);

        assertArrayEquals(new short[] { 10 }, result);
    }

    @Test
    public void testConcatShortArrayVarargs() {
        short[] a = { 10 };
        short[] b = { 20, 30 };
        short[] result = N.concat(a, b);

        assertArrayEquals(new short[] { 10, 20, 30 }, result);
    }

    @Test
    public void testConcatIntArrayTwoArrays() {
        int[] a = { 1, 2, 3 };
        int[] b = { 4, 5 };
        int[] result = N.concat(a, b);

        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testConcatIntArrayEmpty() {
        int[] a = {};
        int[] b = { 10, 20 };
        int[] result = N.concat(a, b);

        assertArrayEquals(new int[] { 10, 20 }, result);
    }

    @Test
    public void testConcatIntArrayNull() {
        int[] a = null;
        int[] b = { 1, 2 };
        int[] result = N.concat(a, b);

        assertArrayEquals(new int[] { 1, 2 }, result);
    }

    @Test
    public void testConcatIntArrayVarargs() {
        int[] a = { 1, 2 };
        int[] b = { 3 };
        int[] c = { 4, 5, 6 };
        int[] result = N.concat(a, b, c);

        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, result);
    }

    @Test
    public void testConcatIntArrayVarargsWithNulls() {
        int[] a = { 1 };
        int[] b = null;
        int[] c = { 2 };
        int[] result = N.concat(a, b, c);

        assertArrayEquals(new int[] { 1, 2 }, result);
    }

    @Test
    public void testConcatLongArrayTwoArrays() {
        long[] a = { 1L, 2L };
        long[] b = { 3L, 4L };
        long[] result = N.concat(a, b);

        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, result);
    }

    @Test
    public void testConcatLongArrayEmpty() {
        long[] a = {};
        long[] b = { 100L };
        long[] result = N.concat(a, b);

        assertArrayEquals(new long[] { 100L }, result);
    }

    @Test
    public void testConcatLongArrayNull() {
        long[] a = null;
        long[] b = { 5L };
        long[] result = N.concat(a, b);

        assertArrayEquals(new long[] { 5L }, result);
    }

    @Test
    public void testConcatLongArrayVarargs() {
        long[] a = { 1L };
        long[] b = { 2L, 3L };
        long[] c = { 4L };
        long[] result = N.concat(a, b, c);

        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, result);
    }

    @Test
    public void testConcatFloatArrayTwoArrays() {
        float[] a = { 1.0f, 2.0f };
        float[] b = { 3.0f, 4.0f };
        float[] result = N.concat(a, b);

        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result);
    }

    @Test
    public void testConcatFloatArrayEmpty() {
        float[] a = {};
        float[] b = { 5.5f };
        float[] result = N.concat(a, b);

        assertArrayEquals(new float[] { 5.5f }, result);
    }

    @Test
    public void testConcatFloatArrayNull() {
        float[] a = null;
        float[] b = { 1.1f };
        float[] result = N.concat(a, b);

        assertArrayEquals(new float[] { 1.1f }, result);
    }

    @Test
    public void testConcatFloatArrayVarargs() {
        float[] a = { 1.0f };
        float[] b = { 2.0f };
        float[] c = { 3.0f };
        float[] result = N.concat(a, b, c);

        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result);
    }

    @Test
    public void testConcatDoubleArrayTwoArrays() {
        double[] a = { 1.0, 2.0 };
        double[] b = { 3.0, 4.0 };
        double[] result = N.concat(a, b);

        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, result);
    }

    @Test
    public void testConcatDoubleArrayEmpty() {
        double[] a = {};
        double[] b = { 5.5 };
        double[] result = N.concat(a, b);

        assertArrayEquals(new double[] { 5.5 }, result);
    }

    @Test
    public void testConcatDoubleArrayNull() {
        double[] a = null;
        double[] b = { 1.1 };
        double[] result = N.concat(a, b);

        assertArrayEquals(new double[] { 1.1 }, result);
    }

    @Test
    public void testConcatDoubleArrayVarargs() {
        double[] a = { 1.0 };
        double[] b = { 2.0, 3.0 };
        double[] c = { 4.0 };
        double[] result = N.concat(a, b, c);

        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, result);
    }

    @Test
    public void testConcatObjectArrayTwoArrays() {
        String[] a = { "a", "b" };
        String[] b = { "c", "d" };
        String[] result = N.concat(a, b);

        assertArrayEquals(new String[] { "a", "b", "c", "d" }, result);
    }

    @Test
    public void testConcatObjectArrayEmpty() {
        String[] a = {};
        String[] b = { "x", "y" };
        String[] result = N.concat(a, b);

        assertArrayEquals(new String[] { "x", "y" }, result);
    }

    @Test
    public void testConcatObjectArrayNull() {
        String[] a = null;
        String[] b = { "test" };
        String[] result = N.concat(a, b);

        assertArrayEquals(new String[] { "test" }, result);
    }

    @Test
    public void testConcatObjectArrayBothNull() {
        String[] a = null;
        String[] b = null;
        String[] result = N.concat(a, b);

        assertNull(result);
    }

    @Test
    public void testConcatObjectArrayVarargs() {
        Integer[] a = { 1, 2 };
        Integer[] b = { 3 };
        Integer[] c = { 4, 5 };
        Integer[] result = N.concat(a, b, c);

        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testConcatObjectArrayVarargsWithNulls() {
        String[] a = { "a" };
        String[] b = null;
        String[] c = { "b" };
        String[] result = N.concat(a, b, c);

        assertArrayEquals(new String[] { "a", "b" }, result);
    }

    @Test
    public void testConcatObjectArrayVarargsEmpty() {
        String[][] empty = {};
        String[] result = N.concat(empty);

        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testConcatIterableTwoIterables() {
        List<String> a = Arrays.asList("a", "b");
        List<String> b = Arrays.asList("c", "d");
        List<String> result = N.concat(a, b);

        assertEquals(Arrays.asList("a", "b", "c", "d"), result);
    }

    @Test
    public void testConcatIterableEmpty() {
        List<String> a = new ArrayList<>();
        List<String> b = Arrays.asList("x", "y");
        List<String> result = N.concat(a, b);

        assertEquals(Arrays.asList("x", "y"), result);
    }

    @Test
    public void testConcatIterableNull() {
        List<String> a = null;
        List<String> b = Arrays.asList("test");
        List<String> result = N.concat(a, b);

        assertEquals(Arrays.asList("test"), result);
    }

    @Test
    public void testConcatIterableVarargs() {
        List<Integer> a = Arrays.asList(1, 2);
        List<Integer> b = Arrays.asList(3);
        List<Integer> c = Arrays.asList(4, 5);
        List<Integer> result = N.concat(a, b, c);

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testConcatIterableVarargsEmpty() {
        @SuppressWarnings("unchecked")
        Iterable<String>[] empty = new Iterable[0];
        List<String> result = N.concat(empty);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testConcatIterableCollection() {
        List<List<String>> lists = Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c"), Arrays.asList("d", "e"));
        List<String> result = N.concat(lists);

        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), result);
    }

    @Test
    public void testConcatIterableCollectionEmpty() {
        List<List<String>> empty = new ArrayList<>();
        List<String> result = N.concat(empty);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testConcatIterableCollectionNull() {
        Collection<List<String>> nullColl = null;
        List<String> result = N.concat(nullColl);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testConcatIterableCollectionWithSupplier() {
        List<List<String>> lists = Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c", "d"));
        Set<String> result = N.concat(lists, IntFunctions.ofSet());

        assertEquals(new HashSet<>(Arrays.asList("a", "b", "c", "d")), result);
    }

    @Test
    public void testConcatIteratorTwoIterators() {
        Iterator<String> a = Arrays.asList("a", "b").iterator();
        Iterator<String> b = Arrays.asList("c", "d").iterator();
        ObjIterator<String> result = N.concat(a, b);

        List<String> list = new ArrayList<>();
        while (result.hasNext()) {
            list.add(result.next());
        }
        assertEquals(Arrays.asList("a", "b", "c", "d"), list);
    }

    @Test
    public void testConcatIteratorEmpty() {
        Iterator<String> a = Collections.emptyIterator();
        Iterator<String> b = Arrays.asList("x").iterator();
        ObjIterator<String> result = N.concat(a, b);

        assertTrue(result.hasNext());
        assertEquals("x", result.next());
    }

    @Test
    public void testConcatIteratorNull() {
        Iterator<String> a = null;
        Iterator<String> b = Arrays.asList("test").iterator();
        ObjIterator<String> result = N.concat(a, b);

        assertTrue(result.hasNext());
        assertEquals("test", result.next());
    }

    @Test
    public void testConcatIteratorVarargs() {
        Iterator<Integer> a = Arrays.asList(1, 2).iterator();
        Iterator<Integer> b = Arrays.asList(3).iterator();
        Iterator<Integer> c = Arrays.asList(4, 5).iterator();
        ObjIterator<Integer> result = N.concat(a, b, c);

        List<Integer> list = new ArrayList<>();
        while (result.hasNext()) {
            list.add(result.next());
        }
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }

    @Test
    public void testConcatIteratorVarargsEmpty() {
        @SuppressWarnings("unchecked")
        Iterator<String>[] empty = new Iterator[0];
        ObjIterator<String> result = N.concat(empty);

        assertTrue(!result.hasNext());
    }

    @Test
    public void testConcatIteratorVarargsWithNulls() {
        Iterator<String> a = Arrays.asList("a").iterator();
        Iterator<String> b = null;
        Iterator<String> c = Arrays.asList("b").iterator();
        ObjIterator<String> result = N.concat(a, b, c);

        List<String> list = new ArrayList<>();
        while (result.hasNext()) {
            list.add(result.next());
        }
        assertEquals(Arrays.asList("a", "b"), list);
    }
}
