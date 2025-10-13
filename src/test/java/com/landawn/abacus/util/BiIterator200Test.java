package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Optional;

@Tag("new-test")
public class BiIterator200Test extends TestBase {

    @Test
    public void test_empty() {
        BiIterator<Object, Object> empty = BiIterator.empty();
        assertFalse(empty.hasNext());
        assertThrows(NoSuchElementException.class, empty::next);
    }

    @Test
    public void test_of_Map() {
        Map<String, Integer> map = Map.of("a", 1, "b", 2);
        BiIterator<String, Integer> biIterator = BiIterator.of(map);

        List<Pair<String, Integer>> result = biIterator.toList();
        assertEquals(2, result.size());
        assertTrue(result.contains(Pair.of("a", 1)));
        assertTrue(result.contains(Pair.of("b", 2)));
    }

    @Test
    public void test_of_Map_empty() {
        BiIterator<String, Integer> biIterator = BiIterator.of(Collections.emptyMap());
        assertFalse(biIterator.hasNext());
    }

    @Test
    public void test_of_Iterator() {
        Iterator<Map.Entry<String, Integer>> iterator = Map.of("a", 1, "b", 2).entrySet().iterator();
        BiIterator<String, Integer> biIterator = BiIterator.of(iterator);

        List<Pair<String, Integer>> result = biIterator.toList();
        assertEquals(2, result.size());
        assertTrue(result.contains(Pair.of("a", 1)));
        assertTrue(result.contains(Pair.of("b", 2)));
    }

    @Test
    public void test_generate_infinite() {
        AtomicInteger counter = new AtomicInteger(0);
        BiIterator<Integer, Integer> generated = BiIterator.generate(p -> p.set(counter.get(), counter.getAndIncrement()));
        List<Pair<Integer, Integer>> result = generated.limit(3).toList();

        assertEquals(Arrays.asList(Pair.of(0, 0), Pair.of(1, 1), Pair.of(2, 2)), result);
    }

    @Test
    public void test_generate_withCondition() {
        AtomicInteger counter = new AtomicInteger(0);
        BooleanSupplier hasNext = () -> counter.get() < 3;
        Consumer<Pair<Integer, Integer>> output = p -> p.set(counter.get(), counter.getAndIncrement());

        BiIterator<Integer, Integer> generated = BiIterator.generate(hasNext, output);
        assertEquals(3, generated.toList().size());
    }

    @Test
    public void test_generate_withIndex() {
        BiIterator<Integer, String> generated = BiIterator.generate(1, 4, (i, p) -> p.set(i, "v" + i));
        List<Pair<Integer, String>> result = generated.toList();

        assertEquals(Arrays.asList(Pair.of(1, "v1"), Pair.of(2, "v2"), Pair.of(3, "v3")), result);
    }

    @Test
    public void test_zip_arrays_equalLength() {
        String[] a = { "a", "b" };
        Integer[] b = { 1, 2 };
        BiIterator<String, Integer> zipped = BiIterator.zip(a, b);
        assertEquals(Arrays.asList(Pair.of("a", 1), Pair.of("b", 2)), zipped.toList());
    }

    @Test
    public void test_zip_arrays_differentLength() {
        String[] a = { "a", "b", "c" };
        Integer[] b = { 1, 2 };
        BiIterator<String, Integer> zipped = BiIterator.zip(a, b);
        assertEquals(2, zipped.toList().size());
    }

    @Test
    public void test_zip_arrays_withDefaultValues() {
        String[] a = { "a" };
        Integer[] b = { 1, 2 };
        BiIterator<String, Integer> zipped = BiIterator.zip(a, b, "defaultA", 0);
        assertEquals(Arrays.asList(Pair.of("a", 1), Pair.of("defaultA", 2)), zipped.toList());
    }

    @Test
    public void test_zip_iterables() {
        List<String> a = List.of("a", "b");
        Set<Integer> b = Set.of(1, 2);
        BiIterator<String, Integer> zipped = BiIterator.zip(a, b);
        assertEquals(2, zipped.toList().size());
    }

    @Test
    public void test_zip_iterators_withDefaultValues() {
        Iterator<String> a = List.of("a").iterator();
        Iterator<Integer> b = List.of(1, 2).iterator();
        BiIterator<String, Integer> zipped = BiIterator.zip(a, b, "defaultA", 0);
        assertEquals(Arrays.asList(Pair.of("a", 1), Pair.of("defaultA", 2)), zipped.toList());
    }

    @Test
    public void test_unzip_iterator() {
        List<String> source = List.of("a-1", "b-2");
        BiIterator<String, Integer> unzipped = BiIterator.unzip(source.iterator(), (s, p) -> {
            String[] parts = s.split("-");
            p.set(parts[0], Integer.parseInt(parts[1]));
        });

        assertEquals(Arrays.asList(Pair.of("a", 1), Pair.of("b", 2)), unzipped.toList());
    }

    @Test
    public void test_forEachRemaining_BiConsumer() {
        BiIterator<String, Integer> biIterator = BiIterator.of(Map.of("a", 1, "b", 2));
        List<String> results = new ArrayList<>();
        biIterator.forEachRemaining((k, v) -> results.add(k + v));
        assertEquals(2, results.size());
        assertTrue(results.contains("a1"));
        assertTrue(results.contains("b2"));
    }

    @Test
    public void test_skip() {
        BiIterator<String, Integer> biIterator = BiIterator.of(Map.of("a", 1, "b", 2, "c", 3));
        BiIterator<String, Integer> skipped = biIterator.skip(1);
        assertEquals(2, skipped.toList().size());
    }

    @Test
    public void test_limit() {
        BiIterator<String, Integer> biIterator = BiIterator.of(Map.of("a", 1, "b", 2, "c", 3));
        BiIterator<String, Integer> limited = biIterator.limit(2);
        assertEquals(2, limited.toList().size());
    }

    @Test
    public void test_filter() {
        BiIterator<String, Integer> biIterator = BiIterator.of(Map.of("a", 1, "b", 2, "c", 3));
        BiIterator<String, Integer> filtered = biIterator.filter((k, v) -> v > 1);
        List<Pair<String, Integer>> result = filtered.toList();
        assertEquals(2, result.size());
        assertFalse(result.contains(Pair.of("a", 1)));
    }

    @Test
    public void test_map() {
        BiIterator<String, Integer> biIterator = BiIterator.of(Map.of("a", 1, "b", 2));
        ObjIterator<String> mapped = biIterator.map((k, v) -> k + ":" + v);
        List<String> result = mapped.toList();
        assertTrue(result.contains("a:1"));
        assertTrue(result.contains("b:2"));
    }

    @Test
    public void test_first() {
        BiIterator<String, Integer> biIterator = BiIterator.of(Map.of("a", 1, "b", 2));
        Optional<Pair<String, Integer>> first = biIterator.first();
        assertTrue(first.isPresent());
    }

    @Test
    public void test_first_empty() {
        Optional<Pair<Object, Object>> first = BiIterator.empty().first();
        assertFalse(first.isPresent());
    }

    @Test
    public void test_last() {
        BiIterator<Integer, String> biIterator = BiIterator.generate(0, 5, (i, p) -> p.set(i, "v" + i));
        Optional<Pair<Integer, String>> last = biIterator.last();
        assertTrue(last.isPresent());
        assertEquals(Pair.of(4, "v4"), last.get());
    }

    @Test
    public void test_last_empty() {
        Optional<Pair<Object, Object>> last = BiIterator.empty().last();
        assertFalse(last.isPresent());
    }

    @Test
    public void test_stream() {
        BiIterator<String, Integer> biIterator = BiIterator.of(Map.of("a", 1, "b", 2));
        long count = biIterator.stream().count();
        assertEquals(2, count);
    }

    @Test
    public void test_stream_withMapper() {
        BiIterator<String, Integer> biIterator = BiIterator.of(Map.of("a", 1, "b", 2));
        List<String> result = biIterator.stream((k, v) -> k + v).toList();
        assertTrue(result.contains("a1"));
        assertTrue(result.contains("b2"));
    }

    @Test
    public void test_toArray() {
        BiIterator<String, Integer> biIterator = BiIterator.of(Map.of("a", 1));
        Pair<String, Integer>[] array = biIterator.toArray();
        assertArrayEquals(new Pair[] { Pair.of("a", 1) }, array);
    }

    @Test
    public void test_toList() {
        BiIterator<String, Integer> biIterator = BiIterator.of(Map.of("a", 1, "b", 2));
        List<Pair<String, Integer>> list = biIterator.toList();
        assertEquals(2, list.size());
    }

    @Test
    public void test_toMultiList() {
        BiIterator<String, Integer> biIterator = BiIterator.zip(new String[] { "a", "b" }, new Integer[] { 1, 2 });
        Pair<List<String>, List<Integer>> multiList = biIterator.toMultiList(ArrayList::new);

        assertEquals(List.of("a", "b"), multiList.left());
        assertEquals(List.of(1, 2), multiList.right());
    }

    @Test
    public void test_toMultiSet() {
        BiIterator<String, Integer> biIterator = BiIterator.zip(new String[] { "a", "b", "a" }, new Integer[] { 1, 2, 1 });
        Pair<Set<String>, Set<Integer>> multiSet = biIterator.toMultiSet(HashSet::new);

        assertEquals(Set.of("a", "b"), multiSet.left());
        assertEquals(Set.of(1, 2), multiSet.right());
    }
}
