package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Optional;

@Tag("new-test")
public class TriIterator200Test extends TestBase {

    @Test
    public void test_empty() {
        TriIterator<Object, Object, Object> empty = TriIterator.empty();
        assertFalse(empty.hasNext());
        assertThrows(NoSuchElementException.class, empty::next);
    }

    @Test
    public void test_generate_infinite() {
        AtomicInteger counter = new AtomicInteger(0);
        TriIterator<Integer, Integer, Integer> iter = TriIterator.generate(t -> {
            int val = counter.getAndIncrement();
            t.set(val, val + 1, val + 2);
        });

        assertTrue(iter.hasNext());
        assertEquals(Triple.of(0, 1, 2), iter.next());
        assertEquals(Triple.of(1, 2, 3), iter.next());
    }

    @Test
    public void test_generate_withCondition() {
        AtomicInteger counter = new AtomicInteger(0);
        TriIterator<Integer, Integer, Integer> iter = TriIterator.generate(() -> counter.get() < 3, t -> {
            int val = counter.getAndIncrement();
            t.set(val, val, val);
        });

        List<Triple<Integer, Integer, Integer>> result = iter.toList();
        assertEquals(3, result.size());
        assertEquals(Arrays.asList(Triple.of(0, 0, 0), Triple.of(1, 1, 1), Triple.of(2, 2, 2)), result);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_generate_emptyRange() {
        TriIterator<Integer, Integer, Integer> iter = TriIterator.generate(1, 1, (i, t) -> t.set(i, i, i));
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_generate_withIndex() {
        TriIterator<Integer, Integer, Integer> iter = TriIterator.generate(1, 4, (i, t) -> t.set(i, i * 2, i * 3));
        assertEquals(Arrays.asList(Triple.of(1, 2, 3), Triple.of(2, 4, 6), Triple.of(3, 6, 9)), iter.toList());
    }

    @Test
    public void test_zip_arrays() {
        TriIterator<Integer, String, Boolean> iter = TriIterator.zip(new Integer[] { 1, 2 }, new String[] { "a", "b", "c" }, new Boolean[] { true });
        assertTrue(iter.hasNext());
        assertEquals(Triple.of(1, "a", true), iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_zip_arraysWithDefault() {
        TriIterator<Integer, String, Boolean> iter = TriIterator.zip(new Integer[] { 1 }, new String[] { "a", "b" }, new Boolean[] {}, -1, "z", false);
        assertEquals(Triple.of(1, "a", false), iter.next());
        assertEquals(Triple.of(-1, "b", false), iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_zip_nullAndEmptyInputs() {
        assertFalse(TriIterator.zip(new Integer[] { 1 }, null, new Boolean[] { true }).hasNext());
        assertFalse(TriIterator.zip(new Integer[] {}, new String[] { "a" }, new Boolean[] { true }).hasNext());
        assertFalse(TriIterator.zip(new Integer[] {}, new String[] {}, new Boolean[] {}).hasNext());
        TriIterator<Integer, String, Boolean> iter = TriIterator.zip((Integer[]) null, (String[]) null, (Boolean[]) null, 1, "a", true);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_zip_iterables() {
        TriIterator<Integer, String, Double> iter = TriIterator.zip(Arrays.asList(1, 2), Arrays.asList("a"), Arrays.asList(1.1, 2.2));
        assertTrue(iter.hasNext());
        assertEquals(Triple.of(1, "a", 1.1), iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_zip_iterablesWithDefault() {
        TriIterator<Integer, String, Boolean> iter = TriIterator.zip(Arrays.asList(1), Arrays.asList("a", "b"), new ArrayList<>(), 99, "nn", true);
        assertEquals(Triple.of(1, "a", true), iter.next());
        assertEquals(Triple.of(99, "b", true), iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_unzip() {
        List<Triple<Integer, String, Boolean>> source = Arrays.asList(Triple.of(1, "a", true), Triple.of(2, "b", false));
        TriIterator<Integer, String, Boolean> iter = TriIterator.unzip(source, (src, target) -> target.set(src.left(), src.middle(), src.right()));
        assertEquals(source, iter.toList());
    }

    @Test
    public void test_unzip_emptyInput() {
        TriIterator<Object, Object, Object> iter = TriIterator.unzip(Collections.emptyList(), (src, target) -> {
        });
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_next_action_throwsException() {
        TriIterator<Integer, Integer, Integer> iter = TriIterator.zip(new Integer[] { 1 }, new Integer[] { 1 }, new Integer[] { 1 });
        iter.next();
        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void test_forEachRemaining() {
        TriIterator<Integer, Integer, Integer> iter = TriIterator.zip(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 });
        List<Integer> listA = new ArrayList<>();
        List<Integer> listB = new ArrayList<>();
        List<Integer> listC = new ArrayList<>();
        iter.forEachRemaining((a, b, c) -> {
            listA.add(a);
            listB.add(b);
            listC.add(c);
        });
        assertEquals(Arrays.asList(1, 2), listA);
        assertEquals(Arrays.asList(3, 4), listB);
        assertEquals(Arrays.asList(5, 6), listC);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_foreachRemaining_throwable() {
        TriIterator<Integer, Integer, Integer> iter = TriIterator.zip(new Integer[] { 1 }, new Integer[] { 1 }, new Integer[] { 1 });
        assertThrows(IOException.class, () -> iter.foreachRemaining((a, b, c) -> {
            throw new IOException("test");
        }));
    }

    @Test
    public void test_skip() {
        TriIterator<Integer, Integer, Integer> iter = TriIterator.zip(new Integer[] { 1, 2, 3 }, new Integer[] { 4, 5, 6 }, new Integer[] { 7, 8, 9 });
        TriIterator<Integer, Integer, Integer> skipped = iter.skip(2);
        assertTrue(skipped.hasNext());
        assertEquals(Triple.of(3, 6, 9), skipped.next());
        assertFalse(skipped.hasNext());

        iter = TriIterator.zip(new Integer[] { 1 }, new Integer[] { 2 }, new Integer[] { 3 });
        skipped = iter.skip(5);
        assertFalse(skipped.hasNext());
    }

    @Test
    public void test_limit() {
        TriIterator<Integer, Integer, Integer> iter = TriIterator.zip(new Integer[] { 1, 2, 3 }, new Integer[] { 4, 5, 6 }, new Integer[] { 7, 8, 9 });
        TriIterator<Integer, Integer, Integer> limited = iter.limit(2);
        assertEquals(2, limited.toList().size());

        iter = TriIterator.zip(new Integer[] { 1, 2, 3 }, new Integer[] { 4, 5, 6 }, new Integer[] { 7, 8, 9 });
        limited = iter.limit(5);
        assertEquals(3, limited.toList().size());
    }

    @Test
    public void test_filter() {
        TriIterator<Integer, Integer, Integer> iter = TriIterator.zip(new Integer[] { 1, 2, 3 }, new Integer[] { 4, 5, 6 }, new Integer[] { 7, 8, 9 });
        TriIterator<Integer, Integer, Integer> filtered = iter.filter((a, b, c) -> (a + b + c) > 15);
        assertTrue(filtered.hasNext());
        assertEquals(Triple.of(3, 6, 9), filtered.next());
        assertFalse(filtered.hasNext());
    }

    @Test
    public void test_chainedOperations() {
        TriIterator<Integer, Integer, Integer> iter = TriIterator.generate(0, 10, (i, t) -> t.set(i, i, i));
        List<Triple<Integer, Integer, Integer>> result = iter.skip(2).limit(5).filter((a, b, c) -> a % 2 == 0).toList();
        assertEquals(Arrays.asList(Triple.of(2, 2, 2), Triple.of(4, 4, 4), Triple.of(6, 6, 6)), result);
    }

    @Test
    public void test_map() {
        TriIterator<Integer, Integer, Integer> iter = TriIterator.zip(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 });
        ObjIterator<String> mapped = iter.map((a, b, c) -> a + ":" + b + ":" + c);
        assertEquals(Arrays.asList("1:3:5", "2:4:6"), mapped.toList());
    }

    @Test
    public void test_map_after_filter() {
        TriIterator<Integer, Integer, Integer> iter = TriIterator.generate(0, 5, (i, t) -> t.set(i, i, i));
        ObjIterator<String> mapped = iter.filter((a, b, c) -> a > 2).map((a, b, c) -> "v" + a);
        assertEquals(Arrays.asList("v3", "v4"), mapped.toList());
    }

    @Test
    public void test_firstAndLast() {
        TriIterator<Integer, Integer, Integer> iterEmpty = TriIterator.empty();
        assertFalse(iterEmpty.first().isPresent());
        assertFalse(iterEmpty.last().isPresent());

        TriIterator<Integer, Integer, Integer> iter = TriIterator.zip(new Integer[] { 1, 2, 3 }, new Integer[] { 4, 5, 6 }, new Integer[] { 7, 8, 9 });
        Optional<Triple<Integer, Integer, Integer>> first = iter.first();
        assertTrue(first.isPresent());
        assertEquals(Triple.of(1, 4, 7), first.get());

        Optional<Triple<Integer, Integer, Integer>> last = iter.last();
        assertTrue(last.isPresent());
        assertEquals(Triple.of(3, 6, 9), last.get());

        iter = TriIterator.zip(new Integer[] { 1, 2, 3 }, new Integer[] { 4, 5, 6 }, new Integer[] { 7, 8, 9 });
        assertEquals(Triple.of(3, 6, 9), iter.last().get());

    }

    @Test
    public void test_stream() {
        TriIterator<Integer, String, Double> iter = TriIterator.zip(Arrays.asList(1, 2), Arrays.asList("a", "b"), Arrays.asList(1.1, 2.2));
        long count = iter.stream((a, b, c) -> a).count();
        assertEquals(2, count);
    }

    @Test
    public void test_toArray() {
        TriIterator<Integer, String, Boolean> iter = TriIterator.zip(new Integer[] { 1, 2 }, new String[] { "a", "b" }, new Boolean[] { true, false });
        Triple<Integer, String, Boolean>[] result = iter.toArray();
        assertEquals(2, result.length);
        assertEquals(Triple.of(1, "a", true), result[0]);
        assertEquals(Triple.of(2, "b", false), result[1]);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void test_toArray_deprecated() {
        TriIterator<Integer, Integer, Integer> iter = TriIterator.zip(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 });
        Triple<Integer, Integer, Integer>[] result = iter.toArray(new Triple[0]);
        assertEquals(2, result.length);

        iter = TriIterator.zip(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 });
        Triple<Integer, Integer, Integer>[] preallocated = new Triple[3];
        preallocated[2] = Triple.of(9, 9, 9);
        iter.toArray(preallocated);
        assertEquals(Triple.of(1, 3, 5), preallocated[0]);
        assertEquals(Triple.of(2, 4, 6), preallocated[1]);
        assertNull(preallocated[2]);
    }

    @Test
    public void test_toMultiList() {
        TriIterator<Integer, String, Double> iter = TriIterator.zip(Arrays.asList(1, 2), Arrays.asList("a", "b"), Arrays.asList(1.1, 2.2));
        Triple<List<Integer>, List<String>, List<Double>> result = iter.toMultiList(ArrayList::new);

        assertEquals(Arrays.asList(1, 2), result.left());
        assertEquals(Arrays.asList("a", "b"), result.middle());
        assertEquals(Arrays.asList(1.1, 2.2), result.right());
    }

    @Test
    public void test_toMultiSet() {
        TriIterator<Integer, String, String> iter = TriIterator.zip(Arrays.asList(1, 1), Arrays.asList("a", "b"), Arrays.asList("x", "x"));
        Triple<Set<Integer>, Set<String>, Set<String>> result = iter.toMultiSet(HashSet::new);

        assertEquals(new HashSet<>(Arrays.asList(1)), result.left());
        assertEquals(new HashSet<>(Arrays.asList("a", "b")), result.middle());
        assertEquals(new HashSet<>(Arrays.asList("x")), result.right());
    }
}
