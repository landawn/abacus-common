package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Optional;

@Tag("2025")
public class BiIterator2025Test extends TestBase {

    @Test
    public void test_empty() {
        BiIterator<String, Integer> iter = BiIterator.empty();
        assertNotNull(iter);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_empty_next_throwsException() {
        BiIterator<String, Integer> iter = BiIterator.empty();
        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void test_empty_forEachRemaining_doesNothing() {
        BiIterator<String, Integer> iter = BiIterator.empty();
        AtomicInteger count = new AtomicInteger(0);
        iter.forEachRemaining((a, b) -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    @Test
    public void test_of_map_null() {
        BiIterator<String, Integer> iter = BiIterator.of((Map<String, Integer>) null);
        assertNotNull(iter);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_of_map_empty() {
        Map<String, Integer> map = new HashMap<>();
        BiIterator<String, Integer> iter = BiIterator.of(map);
        assertNotNull(iter);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_of_map_withElements() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        BiIterator<String, Integer> iter = BiIterator.of(map);
        assertTrue(iter.hasNext());

        List<Pair<String, Integer>> collected = new ArrayList<>();
        iter.forEachRemaining((k, v) -> collected.add(Pair.of(k, v)));
        assertEquals(2, collected.size());
    }

    @Test
    public void test_of_iterator_null() {
        BiIterator<String, Integer> iter = BiIterator.of((Iterator<Map.Entry<String, Integer>>) null);
        assertNotNull(iter);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_of_iterator_empty() {
        Iterator<Map.Entry<String, Integer>> emptyIter = new HashMap<String, Integer>().entrySet().iterator();
        BiIterator<String, Integer> iter = BiIterator.of(emptyIter);
        assertNotNull(iter);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_of_iterator_withElements() {
        Map<String, Integer> map = Map.of("a", 1, "b", 2);
        BiIterator<String, Integer> iter = BiIterator.of(map.entrySet().iterator());
        assertTrue(iter.hasNext());

        int count = 0;
        while (iter.hasNext()) {
            Pair<String, Integer> pair = iter.next();
            assertNotNull(pair.left());
            assertNotNull(pair.right());
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    public void test_generate_consumer_infinite() {
        AtomicInteger counter = new AtomicInteger(0);
        BiIterator<Integer, String> iter = BiIterator.generate(pair -> {
            int n = counter.incrementAndGet();
            pair.set(n, "value" + n);
        });

        assertTrue(iter.hasNext());
        Pair<Integer, String> first = iter.next();
        assertEquals(1, first.left());
        assertEquals("value1", first.right());

        BiIterator<Integer, String> limited = BiIterator.<Integer, String> generate(pair -> {
            int n = counter.incrementAndGet();
            pair.set(n, "value" + n);
        }).limit(3);

        int count = 0;
        while (limited.hasNext()) {
            limited.next();
            count++;
        }
        assertEquals(3, count);
    }

    @Test
    public void test_generate_booleanSupplier_consumer() {
        AtomicInteger counter = new AtomicInteger(0);
        BiIterator<Integer, String> iter = BiIterator.generate(() -> counter.get() < 5, pair -> {
            int n = counter.incrementAndGet();
            pair.set(n, "value" + n);
        });

        int count = 0;
        while (iter.hasNext()) {
            Pair<Integer, String> pair = iter.next();
            count++;
            assertEquals(count, pair.left());
            assertEquals("value" + count, pair.right());
        }
        assertEquals(5, count);
    }

    @Test
    public void test_generate_booleanSupplier_nullChecks() {
        assertThrows(IllegalArgumentException.class, () -> BiIterator.generate(null, pair -> {
        }));

        assertThrows(IllegalArgumentException.class, () -> BiIterator.generate(() -> true, null));
    }

    @Test
    public void test_generate_indexed() {
        BiIterator<Integer, Integer> iter = BiIterator.generate(0, 5, (i, pair) -> {
            pair.set(i, i * i);
        });

        List<Pair<Integer, Integer>> collected = new ArrayList<>();
        iter.forEachRemaining((a, b) -> collected.add(Pair.of(a, b)));

        assertEquals(5, collected.size());
        assertEquals(0, collected.get(0).left());
        assertEquals(0, collected.get(0).right());
        assertEquals(4, collected.get(4).left());
        assertEquals(16, collected.get(4).right());
    }

    @Test
    public void test_generate_indexed_emptyRange() {
        BiIterator<Integer, Integer> iter = BiIterator.generate(5, 5, (i, pair) -> {
            pair.set(i, i);
        });
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_generate_indexed_invalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> BiIterator.generate(5, 2, (i, pair) -> {
        }));
    }

    @Test
    public void test_generate_indexed_nullOutput() {
        assertThrows(IllegalArgumentException.class, () -> BiIterator.generate(0, 5, null));
    }

    @Test
    public void test_zip_arrays() {
        String[] names = { "Alice", "Bob", "Charlie" };
        Integer[] ages = { 25, 30, 35 };

        BiIterator<String, Integer> iter = BiIterator.zip(names, ages);

        List<Pair<String, Integer>> collected = new ArrayList<>();
        iter.forEachRemaining((name, age) -> collected.add(Pair.of(name, age)));

        assertEquals(3, collected.size());
        assertEquals("Alice", collected.get(0).left());
        assertEquals(25, collected.get(0).right());
    }

    @Test
    public void test_zip_arrays_differentLengths() {
        String[] names = { "Alice", "Bob" };
        Integer[] ages = { 25, 30, 35 };

        BiIterator<String, Integer> iter = BiIterator.zip(names, ages);

        int count = 0;
        while (iter.hasNext()) {
            iter.next();
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    public void test_zip_arrays_null() {
        BiIterator<String, Integer> iter = BiIterator.zip((String[]) null, (Integer[]) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_zip_arrays_withDefaults() {
        String[] names = { "Alice", "Bob" };
        Integer[] ages = { 25, 30, 35, 40 };

        BiIterator<String, Integer> iter = BiIterator.zip(names, ages, "Unknown", 0);

        List<Pair<String, Integer>> collected = new ArrayList<>();
        iter.forEachRemaining((name, age) -> collected.add(Pair.of(name, age)));

        assertEquals(4, collected.size());
        assertEquals("Alice", collected.get(0).left());
        assertEquals("Unknown", collected.get(2).left());
        assertEquals(35, collected.get(2).right());
    }

    @Test
    public void test_zip_iterables() {
        List<String> names = List.of("Alice", "Bob");
        List<Integer> ages = List.of(25, 30);

        BiIterator<String, Integer> iter = BiIterator.zip(names, ages);

        assertTrue(iter.hasNext());
        Pair<String, Integer> first = iter.next();
        assertEquals("Alice", first.left());
        assertEquals(25, first.right());
    }

    @Test
    public void test_zip_iterables_null() {
        BiIterator<String, Integer> iter = BiIterator.zip((Iterable<String>) null, (Iterable<Integer>) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_zip_iterables_withDefaults() {
        List<String> names = List.of("Alice");
        List<Integer> ages = List.of(25, 30, 35);

        BiIterator<String, Integer> iter = BiIterator.zip(names, ages, "Unknown", 0);

        List<Pair<String, Integer>> collected = new ArrayList<>();
        iter.forEachRemaining((name, age) -> collected.add(Pair.of(name, age)));

        assertEquals(3, collected.size());
        assertEquals("Unknown", collected.get(1).left());
    }

    @Test
    public void test_zip_iterators() {
        Iterator<String> names = List.of("Alice", "Bob").iterator();
        Iterator<Integer> ages = List.of(25, 30).iterator();

        BiIterator<String, Integer> iter = BiIterator.zip(names, ages);

        assertTrue(iter.hasNext());
        Pair<String, Integer> pair = iter.next();
        assertEquals("Alice", pair.left());
        assertEquals(25, pair.right());
    }

    @Test
    public void test_zip_iterators_null() {
        BiIterator<String, Integer> iter = BiIterator.zip((Iterator<String>) null, (Iterator<Integer>) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_zip_iterators_differentLengths() {
        Iterator<String> names = List.of("Alice", "Bob", "Charlie").iterator();
        Iterator<Integer> ages = List.of(25, 30).iterator();

        BiIterator<String, Integer> iter = BiIterator.zip(names, ages);

        int count = 0;
        while (iter.hasNext()) {
            iter.next();
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    public void test_zip_iterators_withDefaults() {
        Iterator<String> names = List.of("Alice").iterator();
        Iterator<Integer> ages = List.of(25, 30).iterator();

        BiIterator<String, Integer> iter = BiIterator.zip(names, ages, "Unknown", 0);

        List<Pair<String, Integer>> collected = new ArrayList<>();
        iter.forEachRemaining((name, age) -> collected.add(Pair.of(name, age)));

        assertEquals(2, collected.size());
        assertEquals("Unknown", collected.get(1).left());
        assertEquals(30, collected.get(1).right());
    }

    @Test
    public void test_zip_iterators_withDefaults_nullIterators() {
        BiIterator<String, Integer> iter = BiIterator.zip((Iterator<String>) null, (Iterator<Integer>) null, "Default", 0);
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_unzip_iterable() {
        List<String> data = List.of("Alice:25", "Bob:30");

        BiIterator<String, Integer> iter = BiIterator.unzip(data, (s, pair) -> {
            String[] parts = s.split(":");
            pair.set(parts[0], Integer.parseInt(parts[1]));
        });

        List<Pair<String, Integer>> collected = new ArrayList<>();
        iter.forEachRemaining((name, age) -> collected.add(Pair.of(name, age)));

        assertEquals(2, collected.size());
        assertEquals("Alice", collected.get(0).left());
        assertEquals(25, collected.get(0).right());
    }

    @Test
    public void test_unzip_iterable_null() {
        BiIterator<String, Integer> iter = BiIterator.unzip((Iterable<String>) null, (s, pair) -> {
        });
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_unzip_iterator() {
        Iterator<String> data = List.of("Alice:25", "Bob:30").iterator();

        BiIterator<String, Integer> iter = BiIterator.unzip(data, (s, pair) -> {
            String[] parts = s.split(":");
            pair.set(parts[0], Integer.parseInt(parts[1]));
        });

        assertTrue(iter.hasNext());
        Pair<String, Integer> first = iter.next();
        assertEquals("Alice", first.left());
        assertEquals(25, first.right());
    }

    @Test
    public void test_unzip_iterator_null() {
        BiIterator<String, Integer> iter = BiIterator.unzip((Iterator<String>) null, (s, pair) -> {
        });
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_forEachRemaining_biConsumer() {
        Map<String, Integer> map = Map.of("a", 1, "b", 2, "c", 3);
        BiIterator<String, Integer> iter = BiIterator.of(map);

        List<String> keys = new ArrayList<>();
        List<Integer> values = new ArrayList<>();

        iter.forEachRemaining((k, v) -> {
            keys.add(k);
            values.add(v);
        });

        assertEquals(3, keys.size());
        assertEquals(3, values.size());
    }

    @Test
    public void test_forEachRemaining_biConsumer_empty() {
        BiIterator<String, Integer> iter = BiIterator.empty();
        AtomicInteger count = new AtomicInteger(0);
        iter.forEachRemaining((k, v) -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    @Test
    public void test_foreachRemaining_throwable() throws Exception {
        Map<String, Integer> map = Map.of("a", 1, "b", 2);
        BiIterator<String, Integer> iter = BiIterator.of(map);

        List<String> collected = new ArrayList<>();
        iter.foreachRemaining((k, v) -> {
            collected.add(k + "=" + v);
        });

        assertEquals(2, collected.size());
    }

    @Test
    public void test_skip() {
        BiIterator<Integer, Integer> iter = BiIterator.generate(0, 10, (i, pair) -> pair.set(i, i));
        BiIterator<Integer, Integer> skipped = iter.skip(5);

        Pair<Integer, Integer> first = skipped.next();
        assertEquals(5, first.left());
    }

    @Test
    public void test_skip_zero() {
        BiIterator<Integer, Integer> iter = BiIterator.generate(0, 5, (i, pair) -> pair.set(i, i));
        BiIterator<Integer, Integer> skipped = iter.skip(0);

        assertSame(iter, skipped);
    }

    @Test
    public void test_skip_negative() {
        BiIterator<Integer, Integer> iter = BiIterator.generate(0, 5, (i, pair) -> pair.set(i, i));
        assertThrows(IllegalArgumentException.class, () -> iter.skip(-1));
    }

    @Test
    public void test_skip_moreThanAvailable() {
        BiIterator<Integer, Integer> iter = BiIterator.generate(0, 3, (i, pair) -> pair.set(i, i));
        BiIterator<Integer, Integer> skipped = iter.skip(10);

        assertFalse(skipped.hasNext());
    }

    @Test
    public void test_limit() {
        BiIterator<Integer, Integer> iter = BiIterator.generate(0, 10, (i, pair) -> pair.set(i, i));
        BiIterator<Integer, Integer> limited = iter.limit(3);

        int count = 0;
        while (limited.hasNext()) {
            limited.next();
            count++;
        }
        assertEquals(3, count);
    }

    @Test
    public void test_limit_zero() {
        BiIterator<Integer, Integer> iter = BiIterator.generate(0, 5, (i, pair) -> pair.set(i, i));
        BiIterator<Integer, Integer> limited = iter.limit(0);

        assertFalse(limited.hasNext());
    }

    @Test
    public void test_limit_negative() {
        BiIterator<Integer, Integer> iter = BiIterator.generate(0, 5, (i, pair) -> pair.set(i, i));
        assertThrows(IllegalArgumentException.class, () -> iter.limit(-1));
    }

    @Test
    public void test_limit_moreThanAvailable() {
        BiIterator<Integer, Integer> iter = BiIterator.generate(0, 3, (i, pair) -> pair.set(i, i));
        BiIterator<Integer, Integer> limited = iter.limit(10);

        int count = 0;
        while (limited.hasNext()) {
            limited.next();
            count++;
        }
        assertEquals(3, count);
    }

    @Test
    public void test_filter() {
        Map<String, Integer> map = Map.of("a", 1, "b", 2, "c", 3, "d", 4);
        BiIterator<String, Integer> iter = BiIterator.of(map);
        BiIterator<String, Integer> filtered = iter.filter((k, v) -> v > 2);

        int count = 0;
        while (filtered.hasNext()) {
            Pair<String, Integer> pair = filtered.next();
            assertTrue(pair.right() > 2);
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    public void test_filter_noneMatch() {
        BiIterator<Integer, Integer> iter = BiIterator.generate(0, 5, (i, pair) -> pair.set(i, i));
        BiIterator<Integer, Integer> filtered = iter.filter((a, b) -> a > 10);

        assertFalse(filtered.hasNext());
    }

    @Test
    public void test_filter_allMatch() {
        BiIterator<Integer, Integer> iter = BiIterator.generate(0, 5, (i, pair) -> pair.set(i, i));
        BiIterator<Integer, Integer> filtered = iter.filter((a, b) -> a >= 0);

        int count = 0;
        while (filtered.hasNext()) {
            filtered.next();
            count++;
        }
        assertEquals(5, count);
    }

    @Test
    public void test_filter_null() {
        BiIterator<Integer, Integer> iter = BiIterator.generate(0, 5, (i, pair) -> pair.set(i, i));
        assertThrows(IllegalArgumentException.class, () -> iter.filter(null));
    }

    @Test
    public void test_map() {
        Map<String, Integer> map = Map.of("a", 1, "b", 2);
        BiIterator<String, Integer> iter = BiIterator.of(map);
        ObjIterator<String> mapped = iter.map((k, v) -> k + "=" + v);

        List<String> collected = new ArrayList<>();
        while (mapped.hasNext()) {
            collected.add(mapped.next());
        }
        assertEquals(2, collected.size());
    }

    @Test
    public void test_map_toInteger() {
        BiIterator<Integer, Integer> iter = BiIterator.generate(0, 5, (i, pair) -> pair.set(i, i * 2));
        ObjIterator<Integer> mapped = iter.map((a, b) -> a + b);

        List<Integer> collected = new ArrayList<>();
        while (mapped.hasNext()) {
            collected.add(mapped.next());
        }
        assertEquals(5, collected.size());
        assertEquals(N.asList(0, 3, 6, 9, 12), collected);
    }

    @Test
    public void test_first() {
        Map<String, Integer> map = Map.of("a", 1, "b", 2);
        BiIterator<String, Integer> iter = BiIterator.of(map);

        Optional<Pair<String, Integer>> first = iter.first();
        assertTrue(first.isPresent());
        assertNotNull(first.get().left());
        assertNotNull(first.get().right());
    }

    @Test
    public void test_first_empty() {
        BiIterator<String, Integer> iter = BiIterator.empty();
        Optional<Pair<String, Integer>> first = iter.first();
        assertFalse(first.isPresent());
    }

    @Test
    public void test_last() {
        BiIterator<Integer, Integer> iter = BiIterator.generate(0, 5, (i, pair) -> pair.set(i, i * 2));

        Optional<Pair<Integer, Integer>> last = iter.last();
        assertTrue(last.isPresent());
        assertEquals(4, last.get().left());
        assertEquals(8, last.get().right());
    }

    @Test
    public void test_last_empty() {
        BiIterator<String, Integer> iter = BiIterator.empty();
        Optional<Pair<String, Integer>> last = iter.last();
        assertFalse(last.isPresent());
    }

    @Test
    public void test_last_singleElement() {
        BiIterator<Integer, Integer> iter = BiIterator.generate(0, 1, (i, pair) -> pair.set(i, i));

        Optional<Pair<Integer, Integer>> last = iter.last();
        assertTrue(last.isPresent());
        assertEquals(0, last.get().left());
    }

    @Test
    public void test_stream() {
        Map<String, Integer> map = Map.of("a", 1, "b", 2);
        BiIterator<String, Integer> iter = BiIterator.of(map);

        com.landawn.abacus.util.stream.EntryStream<String, Integer> stream = iter.stream();
        assertNotNull(stream);

        long count = stream.count();
        assertEquals(2, count);
    }

    @Test
    public void test_stream_empty() {
        BiIterator<String, Integer> iter = BiIterator.empty();
        com.landawn.abacus.util.stream.EntryStream<String, Integer> stream = iter.stream();

        long count = stream.count();
        assertEquals(0, count);
    }

    @Test
    public void test_stream_withMapper() {
        Map<String, Integer> map = Map.of("a", 1, "b", 2);
        BiIterator<String, Integer> iter = BiIterator.of(map);

        com.landawn.abacus.util.stream.Stream<String> stream = iter.stream((k, v) -> k + "=" + v);
        assertNotNull(stream);

        List<String> collected = stream.toList();
        assertEquals(2, collected.size());
    }

    @Test
    public void test_toArray() {
        BiIterator<Integer, Integer> iter = BiIterator.generate(0, 3, (i, pair) -> pair.set(i, i * 2));

        Pair<Integer, Integer>[] array = iter.toArray();
        assertEquals(3, array.length);
        assertEquals(0, array[0].left());
        assertEquals(0, array[0].right());
        assertEquals(2, array[2].left());
        assertEquals(4, array[2].right());
    }

    @Test
    public void test_toArray_empty() {
        BiIterator<String, Integer> iter = BiIterator.empty();
        Pair<String, Integer>[] array = iter.toArray();
        assertEquals(0, array.length);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void test_toArray_withArray() {
        BiIterator<Integer, Integer> iter = BiIterator.generate(0, 2, (i, pair) -> pair.set(i, i));

        Pair<Integer, Integer>[] array = new Pair[2];
        Pair<Integer, Integer>[] result = iter.toArray(array);

        assertNotNull(result);
        assertEquals(2, result.length);
    }

    @Test
    public void test_toList() {
        BiIterator<Integer, Integer> iter = BiIterator.generate(0, 5, (i, pair) -> pair.set(i, i * 2));

        List<Pair<Integer, Integer>> list = iter.toList();
        assertEquals(5, list.size());
        assertEquals(0, list.get(0).left());
        assertEquals(4, list.get(4).left());
        assertEquals(8, list.get(4).right());
    }

    @Test
    public void test_toList_empty() {
        BiIterator<String, Integer> iter = BiIterator.empty();
        List<Pair<String, Integer>> list = iter.toList();
        assertTrue(list.isEmpty());
    }

    @Test
    public void test_toMultiList() {
        Map<String, Integer> map = Map.of("a", 1, "b", 2, "c", 3);
        BiIterator<String, Integer> iter = BiIterator.of(map);

        Pair<List<String>, List<Integer>> lists = iter.toMultiList(ArrayList::new);

        assertEquals(3, lists.left().size());
        assertEquals(3, lists.right().size());

        assertTrue(lists.left().contains("a"));
        assertTrue(lists.right().contains(1));
    }

    @Test
    public void test_toMultiList_empty() {
        BiIterator<String, Integer> iter = BiIterator.empty();
        Pair<List<String>, List<Integer>> lists = iter.toMultiList(ArrayList::new);

        assertTrue(lists.left().isEmpty());
        assertTrue(lists.right().isEmpty());
    }

    @Test
    public void test_toMultiSet() {
        BiIterator<String, Integer> iter = BiIterator.generate(0, 5, (i, pair) -> {
            pair.set(i % 2 == 0 ? "even" : "odd", i);
        });

        Pair<Set<String>, Set<Integer>> sets = iter.toMultiSet(HashSet::new);

        assertEquals(2, sets.left().size());
        assertEquals(5, sets.right().size());

        assertTrue(sets.left().contains("even"));
        assertTrue(sets.left().contains("odd"));
    }

    @Test
    public void test_toMultiSet_empty() {
        BiIterator<String, Integer> iter = BiIterator.empty();
        Pair<Set<String>, Set<Integer>> sets = iter.toMultiSet(HashSet::new);

        assertTrue(sets.left().isEmpty());
        assertTrue(sets.right().isEmpty());
    }

    @Test
    public void test_chained_operations() {
        BiIterator<Integer, Integer> iter = BiIterator.<Integer, Integer> generate(0, 10, (i, pair) -> pair.set(i, i * 2))
                .skip(2)
                .filter((a, b) -> a % 2 == 0)
                .limit(3);

        List<Pair<Integer, Integer>> collected = iter.toList();
        assertEquals(3, collected.size());
    }

    @Test
    public void test_map_then_filter() {
        Map<String, Integer> map = Map.of("a", 1, "b", 2, "c", 3);
        BiIterator<String, Integer> iter = BiIterator.of(map);

        ObjIterator<String> mapped = iter.filter((k, v) -> v > 1).map((k, v) -> k + "=" + v);

        List<String> collected = new ArrayList<>();
        while (mapped.hasNext()) {
            collected.add(mapped.next());
        }
        assertEquals(2, collected.size());
    }

    @Test
    public void test_forEachRemaining_consumer_deprecated() {
        Map<String, Integer> map = Map.of("a", 1, "b", 2);
        BiIterator<String, Integer> iter = BiIterator.of(map);

        List<Pair<String, Integer>> collected = new ArrayList<>();
        iter.forEachRemaining(pair -> collected.add(pair));

        assertEquals(2, collected.size());
    }
}
