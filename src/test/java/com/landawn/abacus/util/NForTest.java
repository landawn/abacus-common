package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

public class NForTest extends NTestSupport {

    @Test
    public void testForEach_intRange() {
        AtomicInteger count = new AtomicInteger(0);
        N.forEach(0, 5, count::incrementAndGet);
        assertEquals(5, count.get());
        count.set(0);
        N.forEach(5, 0, count::incrementAndGet);
        assertEquals(0, count.get());

        AtomicInteger sum = new AtomicInteger(0);
        N.forEach(0, 5, sum::addAndGet);
        assertEquals(10, sum.get());

        List<Integer> stepped = new ArrayList<>();
        N.forEach(0, 6, 2, stepped::add);
        assertEquals(Arrays.asList(0, 2, 4), stepped);
        stepped.clear();
        N.forEach(5, -1, -2, stepped::add);
        assertEquals(Arrays.asList(5, 3, 1), stepped);
        stepped.clear();
        N.forEach(0, 5, 10, stepped::add);
        assertEquals(Arrays.asList(0), stepped);
        stepped.clear();
        N.forEach(5, 5, stepped::add);
        assertTrue(stepped.isEmpty());
        assertThrows(IllegalArgumentException.class, () -> N.forEach(0, 10, 0, i -> {
        }));

        StringBuilder sb = new StringBuilder();
        N.forEach(0, 3, "val:", (i, p) -> sb.append(p).append(i).append(" "));
        assertEquals("val:0 val:1 val:2 ", sb.toString());
    }

    @Test
    public void testForEach_arrayCollectionIterator() {
        String[] array = { "a", "b", "c" };
        List<String> result = new ArrayList<>();
        N.forEach(array, result::add);
        assertEquals(Arrays.asList("a", "b", "c"), result);
        N.forEach((String[]) null, result::add);
        N.forEach(new String[0], result::add);
        assertEquals(3, result.size());

        result.clear();
        N.forEach(array, 1, 3, result::add);
        assertEquals(Arrays.asList("b", "c"), result);
        result.clear();
        N.forEach(new String[] { "a", "b", "c", "d", "e" }, 1, 4, result::add);
        assertEquals(Arrays.asList("b", "c", "d"), result);
        result.clear();
        N.forEach(new String[] { "a", "b", "c", "d", "e" }, 4, -1, result::add);
        assertEquals(Arrays.asList("e", "d", "c", "b", "a"), result);
        assertThrows(IndexOutOfBoundsException.class, () -> N.forEach(array, 0, 10, result::add));

        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        result.clear();
        N.forEach(list, result::add);
        assertEquals(list, result);
        N.forEach((List<String>) null, result::add);
        N.forEach((Iterator<String>) null, result::add);

        result.clear();
        N.forEach(list, 1, 4, result::add);
        assertEquals(Arrays.asList("b", "c", "d"), result);
        result.clear();
        N.forEach(list, 3, 1, result::add);
        assertEquals(Arrays.asList("d", "c"), result);
        result.clear();
        N.forEach(new LinkedList<>(list), 1, 4, result::add);
        assertEquals(Arrays.asList("b", "c", "d"), result);
        result.clear();
        N.forEach(CommonUtil.toLinkedHashSet("a", "b", "c", "d", "e"), 4, 1, result::add);
        assertEquals(Arrays.asList("e", "d", "c"), result);

        for (final Collection<String> c : Arrays.asList(new ArrayList<>(Arrays.asList("A", "B", "C")), new LinkedList<>(Arrays.asList("A", "B", "C")),
                new LinkedHashSet<>(Arrays.asList("A", "B", "C")))) {
            final List<String> got = new ArrayList<>();
            N.forEach(c, c.size(), c.size() - 1, got::add);
            assertTrue(got.isEmpty(), "forEach on " + c.getClass().getSimpleName() + " should process nothing");
            got.clear();
            N.forEachIndexed(c, c.size(), c.size() - 1, (i, e) -> got.add(e));
            assertTrue(got.isEmpty(), "forEachIndexed on " + c.getClass().getSimpleName() + " should process nothing");
        }

        result.clear();
        N.forEach(list.iterator(), result::add);
        assertEquals(list, result);

        final List<String> seen = new ArrayList<>();
        N.forEach((Collection<String>) null, 0, -1, seen::add);
        N.forEachIndexed((Collection<String>) null, 0, -1, (i, e) -> seen.add(i + ":" + e));
        N.forEach(new ArrayList<String>(), 0, -1, seen::add);
        assertTrue(seen.isEmpty());
        N.forEach(Arrays.asList("a", "b", "c"), 2, -1, seen::add);
        assertEquals(Arrays.asList("c", "b", "a"), seen);

        List<Integer> mutating = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        assertThrows(ConcurrentModificationException.class, () -> N.forEach(mutating, i -> {
            if (i == 3) {
                mutating.add(6);
            }
        }));

        List<Integer> processed = new ArrayList<>();
        try {
            N.forEach(Arrays.asList(1, 2, 3, 4, 5), i -> {
                processed.add(i);
                if (i == 3) {
                    throw new RuntimeException("Failed at 3");
                }
            });
            fail("expected RuntimeException");
        } catch (RuntimeException e) {
            assertEquals("Failed at 3", e.getMessage());
        }
        assertEquals(Arrays.asList(1, 2, 3), processed);
    }

    @Test
    public void testForEach_map() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        AtomicInteger sum = new AtomicInteger(0);
        N.forEach(map, (k, v) -> sum.addAndGet(v));
        assertEquals(6, sum.get());

        List<String> entries = new ArrayList<>();
        N.forEach(map, entry -> entries.add(entry.getKey() + "=" + entry.getValue()));
        assertEquals(Arrays.asList("a=1", "b=2", "c=3"), entries);

        AtomicInteger empty = new AtomicInteger(0);
        N.forEach(new HashMap<String, Integer>(), (k, v) -> empty.incrementAndGet());
        assertEquals(0, empty.get());
    }

    @Test
    public void testForEach_zip() {
        List<String> two = new ArrayList<>();
        N.forEach(new String[] { "a", "b", "c" }, new String[] { "1", "2", "3" }, (a, b) -> two.add(a + b));
        assertEquals(Arrays.asList("a1", "b2", "c3"), two);
        two.clear();
        N.forEach(new String[] { "a", "b", "c", "d" }, new String[] { "1", "2" }, (a, b) -> two.add(a + b));
        assertEquals(Arrays.asList("a1", "b2"), two);
        two.clear();
        N.forEach(new String[] { "a", "b" }, new String[] { "1", "2", "3", "4" }, "X", "Y", (a, b) -> two.add(a + b));
        assertEquals(Arrays.asList("a1", "b2", "X3", "X4"), two);
        two.clear();
        N.forEach(Arrays.asList("a", "b", "c"), Arrays.asList("1", "2", "3", "4"), "X", "0", (s, i) -> two.add(s + i));
        assertEquals(Arrays.asList("a1", "b2", "c3", "X4"), two);
        two.clear();
        N.forEach(Arrays.asList("a", "b", "c").iterator(), Arrays.asList("1", "2", "3", "4").iterator(), "X", "0", (s, i) -> two.add(s + i));
        assertEquals(Arrays.asList("a1", "b2", "c3", "X4"), two);

        AtomicInteger emptyGuard = new AtomicInteger();
        N.forEach(new String[0], new Integer[] { 1 }, (x, y) -> emptyGuard.incrementAndGet());
        assertEquals(0, emptyGuard.get());
        N.forEach(Collections.<String> emptyList(), Arrays.asList(1), (x, y) -> emptyGuard.incrementAndGet());
        assertEquals(0, emptyGuard.get());

        List<String> three = new ArrayList<>();
        N.forEach(new String[] { "a", "b", "c" }, new String[] { "1", "2", "3" }, new String[] { "x", "y", "z" }, (a, b, c) -> three.add(a + b + c));
        assertEquals(Arrays.asList("a1x", "b2y", "c3z"), three);
        three.clear();
        N.forEach(new String[] { "a", "b", "c" }, new String[] { "1", "2", "3", "4" }, new Boolean[] { true, false }, "X", "0", false,
                (s, i, j) -> three.add(s + i + j));
        assertEquals(Arrays.asList("a1true", "b2false", "c3false", "X4false"), three);
        three.clear();
        N.forEach(Arrays.asList("a", "b", "c"), Arrays.asList("1", "2", "3", "4"), CommonUtil.toList(true, false), "X", "0", false,
                (s, i, j) -> three.add(s + i + j));
        assertEquals(Arrays.asList("a1true", "b2false", "c3false", "X4false"), three);

        AtomicInteger threeGuard = new AtomicInteger();
        N.forEach(new String[0], new Integer[] { 1 }, new Long[] { 1L }, (x, y, z) -> threeGuard.incrementAndGet());
        assertEquals(0, threeGuard.get());
        N.forEach(new String[] { "a", "b", "c" }, new Integer[] { 1, 2 }, new Long[] { 1L, 2L, 3L }, (x, y, z) -> threeGuard.incrementAndGet());
        assertEquals(2, threeGuard.get());
        threeGuard.set(0);
        N.forEach(Collections.<String> emptyList(), Arrays.asList(1), Arrays.asList(1L), (x, y, z) -> threeGuard.incrementAndGet());
        assertEquals(0, threeGuard.get());
        N.forEach(Arrays.asList("a", "b"), Arrays.asList(1, 2, 3), Arrays.asList(1L, 2L), (x, y, z) -> threeGuard.incrementAndGet());
        assertEquals(2, threeGuard.get());

        List<String> mixed = new ArrayList<>();
        N.forEach(Arrays.asList(1, 2, 3), new LinkedHashSet<>(Arrays.asList("a", "b", "c")), (i, s) -> mixed.add(i + s));
        assertEquals(Arrays.asList("1a", "2b", "3c"), mixed);
    }

    @Test
    public void testForEach_flatMapper() {
        String[] array = { "ab", "cd", "ef" };
        List<Character> result = new ArrayList<>();
        N.forEach(array, s -> Arrays.asList(s.charAt(0), s.charAt(1)), (s, c) -> result.add(c));
        assertEquals(Arrays.asList('a', 'b', 'c', 'd', 'e', 'f'), result);

        result.clear();
        N.forEach(CommonUtil.toList("ab", "cd", "ef"), s -> Arrays.asList(s.charAt(0), s.charAt(1)), (s, c) -> result.add(c));
        assertEquals(Arrays.asList('a', 'b', 'c', 'd', 'e', 'f'), result);

        result.clear();
        N.forEach(CommonUtil.toList("ab", "cd", "ef").iterator(), s -> Arrays.asList(s.charAt(0), s.charAt(1)), (s, c) -> result.add(c));
        assertEquals(Arrays.asList('a', 'b', 'c', 'd', 'e', 'f'), result);

        result.clear();
        N.forEach(new String[] { "ab", null, "cd" }, s -> s != null ? Arrays.asList(s.charAt(0), s.charAt(1)) : null, (s, c) -> result.add(c));
        assertEquals(Arrays.asList('a', 'b', 'c', 'd'), result);

        List<Character> nested = new ArrayList<>();
        N.forEach(array, s -> Arrays.asList(s.charAt(0), s.charAt(1)), e -> CommonUtil.toList(e, e), (s, c, x) -> nested.add(c));
        assertEquals(Arrays.asList('a', 'a', 'b', 'b', 'c', 'c', 'd', 'd', 'e', 'e', 'f', 'f'), nested);

        List<String> twoLevel = new ArrayList<>();
        N.forEach(new String[] { "ab", "cd" }, s -> charsOf(s), c -> Arrays.asList(c.toString().toUpperCase()),
                (s, c, u) -> twoLevel.add(s + ":" + c + ":" + u));
        assertEquals(4, twoLevel.size());
        assertTrue(twoLevel.contains("ab:a:A"));
        N.forEach(new String[0], s -> charsOf(s), c -> Arrays.asList(c.toString()), (s, c, u) -> {
        });
        N.forEach((Iterator<String>) null, s -> charsOf(s), (s, c) -> {
        });
    }

    @Test
    public void testForEachNonNull() {
        assertEquals(Arrays.asList("a", "b", "c"), collectNonNull(new String[] { "a", null, "b", null, "c" }));
        assertTrue(collectNonNull(new String[] { null, null, null }).isEmpty());

        List<String> flat = new ArrayList<>();
        N.forEachNonNull(new String[] { "a", null, "b" }, e -> CommonUtil.toList(e, e), (s, i) -> flat.add(s + i));
        assertEquals(Arrays.asList("aa", "aa", "bb", "bb"), flat);

        List<String> triple = new ArrayList<>();
        N.forEachNonNull(new String[] { "ab", null, "cd" }, s -> charsOf(s), c -> Arrays.asList(c.toString().toUpperCase(), c.toString().toLowerCase()),
                (original, ch, str) -> triple.add(original + ":" + ch + ":" + str));
        assertEquals(8, triple.size());
        assertTrue(triple.contains("ab:a:A"));

        List<String> fromList = new ArrayList<>();
        N.forEachNonNull(CommonUtil.toList("a", null, "b", null, "c"), fromList::add);
        assertEquals(Arrays.asList("a", "b", "c"), fromList);

        List<String> fromIter = new ArrayList<>();
        N.forEachNonNull(CommonUtil.toList("a", null, "b").iterator(), fromIter::add);
        assertEquals(Arrays.asList("a", "b"), fromIter);

        List<String> iterFlat = new ArrayList<>();
        N.forEachNonNull(Arrays.asList("ab", null, "cd").iterator(), s -> charsOf(s), (s, c) -> iterFlat.add(s + ":" + c));
        assertEquals(Arrays.asList("ab:a", "ab:b", "cd:c", "cd:d"), iterFlat);
        N.forEachNonNull((Iterator<String>) null, s -> charsOf(s), (s, c) -> {
        });
    }

    @Test
    public void testForEachIndexed() {
        Map<Integer, String> result = new HashMap<>();
        N.forEachIndexed(new String[] { "a", "b", "c" }, result::put);
        assertEquals("a", result.get(0));
        assertEquals("c", result.get(2));

        result.clear();
        N.forEachIndexed(new String[] { "a", "b", "c", "d", "e" }, 1, 4, result::put);
        assertEquals(3, result.size());
        assertEquals("b", result.get(1));

        result.clear();
        N.forEachIndexed(Arrays.asList("a", "b", "c", "d", "e"), 3, 1, result::put);
        assertEquals("d", result.get(3));
        assertEquals("c", result.get(2));
        assertEquals(2, result.size());

        result.clear();
        N.forEachIndexed(CommonUtil.toLinkedHashSet("a", "b", "c", "d", "e"), 3, 1, result::put);
        assertEquals("d", result.get(3));

        result.clear();
        N.forEachIndexed(CommonUtil.toLinkedList("a", "b", "c").iterator(), result::put);
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));

        Map<Integer, String> shifted = new HashMap<>();
        N.forEachIndexed(result, (i, e) -> shifted.put(e.getKey() + 1, e.getValue()));
        assertEquals("a", shifted.get(1));
        Map<Integer, String> byKey = new HashMap<>();
        N.forEachIndexed(result, (i, k, v) -> byKey.put(k + 1, v));
        assertEquals("a", byKey.get(1));

        AtomicInteger empty = new AtomicInteger(0);
        N.forEachIndexed(Collections.emptyMap(), (idx, entry) -> empty.incrementAndGet());
        assertEquals(0, empty.get());
    }

    @Test
    public void testForEachInParallel() throws Exception {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        ConcurrentLinkedQueue<Integer> result = new ConcurrentLinkedQueue<>();
        N.forEachInParallel(list, result::add, 2);
        assertEquals(5, result.size());
        assertTrue(result.containsAll(list));

        assertThrows(RuntimeException.class, () -> N.forEachInParallel(list, i -> {
            if (i == 3) {
                throw new RuntimeException("Test exception");
            }
        }, 2));

        final List<Integer> results = Collections.synchronizedList(new ArrayList<>());
        assertThrows(IllegalArgumentException.class, () -> N.forEachInParallel(list, results::add, 0));
        assertThrows(IllegalArgumentException.class, () -> N.forEachInParallel(list.iterator(), results::add, 0));
        assertThrows(IllegalArgumentException.class, () -> N.forEachIndexedInParallel(list, (idx, e) -> results.add(e), 0));
        assertThrows(IllegalArgumentException.class, () -> N.forEachInParallel(list, results::add, -1));
        N.forEachInParallel(new ArrayList<Integer>(), results::add, 4);
        assertTrue(results.isEmpty());

        CopyOnWriteArrayList<String> fromIter = new CopyOnWriteArrayList<>();
        N.forEachInParallel(Arrays.asList("a", "b", "c", "d", "e").iterator(), (Throwables.Consumer<String, Exception>) fromIter::add, 2);
        assertEquals(5, fromIter.size());

        AtomicInteger sum = new AtomicInteger(0);
        N.forEachInParallel(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), (Throwables.Consumer<Integer, RuntimeException>) sum::addAndGet, 2, executorService);
        assertEquals(55, sum.get());

        Throwables.Consumer<Integer, Exception> failing = val -> {
            if (val == 3) {
                throw new IOException("Test exception");
            }
        };
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> N.forEachInParallel(list.iterator(), failing, 2, executorService));
        assertTrue(thrown.getCause() instanceof IOException
                || (thrown.getCause() instanceof ExecutionException && thrown.getCause().getCause() instanceof IOException));

        ConcurrentHashMap<Integer, Integer> indexed = new ConcurrentHashMap<>();
        N.forEachIndexedInParallel(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), indexed::put, 3);
        assertEquals(10, indexed.size());
        assertEquals(1, indexed.get(0));

        ExecutorService custom = Executors.newFixedThreadPool(2);
        try {
            ConcurrentHashMap<Integer, Integer> customResult = new ConcurrentHashMap<>();
            N.forEachIndexedInParallel(Arrays.asList(1, 2, 3, 4, 5), customResult::put, 2, custom);
            assertEquals(5, customResult.size());
        } finally {
            custom.shutdown();
        }

        CopyOnWriteArrayList<String> indexedIter = new CopyOnWriteArrayList<>();
        N.forEachIndexedInParallel(Arrays.asList("a", "b", "c", "d", "e").iterator(), (idx, item) -> indexedIter.add(idx + ":" + item), 2);
        assertEquals(5, indexedIter.size());
    }

    @Test
    public void testForEachPair() {
        List<String> result = new ArrayList<>();
        N.forEachPair(new Integer[] { 1, 2, 3, 4, 5 }, (a, b) -> result.add(a + "-" + (b != null ? b : "null")));
        assertEquals(Arrays.asList("1-2", "2-3", "3-4", "4-5"), result);

        result.clear();
        N.forEachPair(new Integer[] { 1, 2, 3, 4, 5, 6 }, 2, (a, b) -> result.add(a + "-" + (b != null ? b : "null")));
        assertEquals(Arrays.asList("1-2", "3-4", "5-6"), result);

        result.clear();
        N.forEachPair(Collections.emptyList(), (a, b) -> result.add(a + "-" + b));
        assertTrue(result.isEmpty());

        result.clear();
        N.forEachPair(Arrays.asList("only"), (a, b) -> result.add(a + "-" + (b != null ? b : "null")));
        assertEquals(Arrays.asList("only-null"), result);

        result.clear();
        N.forEachPair(CommonUtil.toList("a", "b", "c", "d", "e"), 2, (a, b) -> result.add(a + "-" + (b != null ? b : "null")));
        assertEquals(Arrays.asList("a-b", "c-d", "e-null"), result);

        result.clear();
        N.forEachPair(CommonUtil.toList("a", "b", "c", "d", "e").iterator(), 3, (a, b) -> result.add(a + "-" + (b != null ? b : "null")));
        assertEquals(Arrays.asList("a-b", "d-e"), result);

        result.clear();
        N.forEachPair(new String[] { "a" }, (e1, e2) -> result.add(e1 + (e2 == null ? "_null" : e2)));
        assertEquals(Arrays.asList("a_null"), result);
        N.forEachPair((Iterator<Integer>) null, 1, (a, b) -> {
        });
    }

    @Test
    public void testForEachTriple() {
        List<String> result = new ArrayList<>();
        N.forEachTriple(new Integer[] { 1, 2, 3, 4, 5 }, (a, b, c) -> result.add(a + "-" + nz(b) + "-" + nz(c)));
        assertEquals(Arrays.asList("1-2-3", "2-3-4", "3-4-5"), result);

        result.clear();
        N.forEachTriple(new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, 3, (a, b, c) -> result.add(a + "-" + nz(b) + "-" + nz(c)));
        assertEquals(Arrays.asList("1-2-3", "4-5-6", "7-8-9"), result);

        result.clear();
        N.forEachTriple(CommonUtil.toList("a", "b", "c", "d", "e"), 3, (i, j, k) -> result.add(i + "-" + nz(j) + "-" + nz(k)));
        assertEquals(Arrays.asList("a-b-c", "d-e-null"), result);

        result.clear();
        N.forEachTriple(CommonUtil.toList("a", "b", "c", "d", "e").iterator(), 4, (i, j, k) -> result.add(i + "-" + nz(j) + "-" + nz(k)));
        assertEquals(Arrays.asList("a-b-c", "e-null-null"), result);

        result.clear();
        N.forEachTriple(CommonUtil.toList("a", "b", "c", "d", "e"), 6, (i, j, k) -> result.add(i + "-" + nz(j) + "-" + nz(k)));
        assertEquals(Arrays.asList("a-b-c"), result);

        result.clear();
        N.forEach(Arrays.asList("A", "B"), s1 -> Arrays.asList(s1 + "1", s1 + "2"), s2 -> Arrays.asList(s2 + "a", s2 + "b"),
                (original, mid, end) -> result.add(original + "-" + mid + "-" + end));
        assertEquals(8, result.size());
        assertTrue(result.contains("A-A1-A1a"));
    }

    @Test
    public void testForClass() {
        assertEquals(int.class, ClassUtil.forName("int"));
        assertEquals(char.class, ClassUtil.forName("char"));
        assertEquals(boolean.class, ClassUtil.forName("boolean"));
        assertEquals(String.class, ClassUtil.forName("String"));
        assertEquals(Object.class, ClassUtil.forName("Object"));
        assertEquals(java.lang.Math.class, ClassUtil.forName("Math"));
        assertEquals(java.util.Date.class, ClassUtil.forName("java.util.Date"));
        assertEquals(int[].class, ClassUtil.forName("int[]"));
        assertEquals(String[].class, ClassUtil.forName("String[]"));
        assertEquals(int[][].class, ClassUtil.forName("int[][]"));
        assertEquals(String[][].class, ClassUtil.forName("String[][]"));
        assertEquals(int[][][].class, ClassUtil.forName("int[][][]"));
        assertEquals(java.util.Date[][][].class, ClassUtil.forName("java.util.Date[][][]"));
        assertThrows(IllegalArgumentException.class, () -> ClassUtil.forName("string"));
        assertThrows(IllegalArgumentException.class, () -> ClassUtil.forName("object[]"));
        assertThrows(IllegalArgumentException.class, () -> ClassUtil.forName("int[];[]"));
    }

    private static List<Character> charsOf(String s) {
        List<Character> chars = new ArrayList<>();
        for (char c : s.toCharArray()) {
            chars.add(c);
        }
        return chars;
    }

    private static List<String> collectNonNull(String[] array) {
        List<String> result = new ArrayList<>();
        N.forEachNonNull(array, result::add);
        return result;
    }

    private static String nz(Object value) {
        return value == null ? "null" : value.toString();
    }
}
