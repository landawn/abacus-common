package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.Stream;

@Tag("2025")
public class TriIterator2025Test extends TestBase {

    @Test
    public void testEmpty() {
        TriIterator<String, Integer, Boolean> iter = TriIterator.empty();
        assertNotNull(iter);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testEmpty_next_throwsException() {
        TriIterator<String, Integer, Boolean> iter = TriIterator.empty();
        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testGenerate_consumer() {
        AtomicInteger counter = new AtomicInteger(0);
        TriIterator<Integer, Integer, Integer> iter = TriIterator.<Integer, Integer, Integer> generate(triple -> {
            int n = counter.getAndIncrement();
            triple.setLeft(n);
            triple.setMiddle(n * 2);
            triple.setRight(n * 3);
        }).limit(5);

        int count = 0;
        while (iter.hasNext()) {
            Triple<Integer, Integer, Integer> t = iter.next();
            assertEquals(count, t.left().intValue());
            assertEquals(count * 2, t.middle().intValue());
            assertEquals(count * 3, t.right().intValue());
            count++;
        }
        assertEquals(5, count);
    }

    @Test
    public void testGenerate_booleanSupplierAndConsumer() {
        AtomicInteger counter = new AtomicInteger(0);
        TriIterator<Integer, String, Boolean> iter = TriIterator.generate(() -> counter.get() < 3, triple -> {
            int n = counter.getAndIncrement();
            triple.setLeft(n);
            triple.setMiddle("Item " + n);
            triple.setRight(n % 2 == 0);
        });

        int count = 0;
        while (iter.hasNext()) {
            Triple<Integer, String, Boolean> t = iter.next();
            assertEquals(count, t.left().intValue());
            assertEquals("Item " + count, t.middle());
            assertEquals(count % 2 == 0, t.right());
            count++;
        }
        assertEquals(3, count);
    }

    @Test
    public void testGenerate_booleanSupplierAndConsumer_nullArgs() {
        assertThrows(IllegalArgumentException.class, () -> TriIterator.generate(null, triple -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> TriIterator.generate(() -> true, null));
    }

    @Test
    public void testGenerate_intRange() {
        TriIterator<Integer, String, Double> iter = TriIterator.generate(0, 5, (index, triple) -> {
            triple.setLeft(index);
            triple.setMiddle("Index: " + index);
            triple.setRight(Math.sqrt(index));
        });

        int count = 0;
        while (iter.hasNext()) {
            Triple<Integer, String, Double> t = iter.next();
            assertEquals(count, t.left().intValue());
            assertEquals("Index: " + count, t.middle());
            assertEquals(Math.sqrt(count), t.right(), 0.0001);
            count++;
        }
        assertEquals(5, count);
    }

    @Test
    public void testGenerate_intRange_invalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> TriIterator.generate(5, 2, (idx, triple) -> {
        }));
    }

    @Test
    public void testGenerate_intRange_nullOutput() {
        assertThrows(IllegalArgumentException.class, () -> TriIterator.generate(0, 5, null));
    }

    @Test
    public void testZip_arrays() {
        String[] names = { "Alice", "Bob", "Charlie" };
        Integer[] ages = { 25, 30, 35 };
        Boolean[] active = { true, false, true };

        TriIterator<String, Integer, Boolean> iter = TriIterator.zip(names, ages, active);

        int count = 0;
        while (iter.hasNext()) {
            Triple<String, Integer, Boolean> t = iter.next();
            assertEquals(names[count], t.left());
            assertEquals(ages[count], t.middle());
            assertEquals(active[count], t.right());
            count++;
        }
        assertEquals(3, count);
    }

    @Test
    public void testZip_arrays_differentLengths() {
        String[] names = { "Alice", "Bob" };
        Integer[] ages = { 25, 30, 35 };
        Boolean[] active = { true };

        TriIterator<String, Integer, Boolean> iter = TriIterator.zip(names, ages, active);

        assertTrue(iter.hasNext());
        Triple<String, Integer, Boolean> t = iter.next();
        assertEquals("Alice", t.left());
        assertEquals(25, t.middle());
        assertTrue(t.right());

        assertFalse(iter.hasNext());
    }

    @Test
    public void testZip_arrays_nullArray() {
        String[] names = { "Alice" };
        Integer[] ages = { 25 };

        TriIterator<String, Integer, Boolean> iter = TriIterator.zip(names, ages, (Boolean[]) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testZip_arrays_withDefaults() {
        String[] names = { "Alice", "Bob" };
        Integer[] ages = { 25, 30, 35 };
        Boolean[] active = { true };

        TriIterator<String, Integer, Boolean> iter = TriIterator.zip(names, ages, active, "Unknown", 0, false);

        List<Triple<String, Integer, Boolean>> result = iter.toList();
        assertEquals(3, result.size());

        assertEquals("Alice", result.get(0).left());
        assertEquals("Bob", result.get(1).left());
        assertEquals("Unknown", result.get(2).left());

        assertEquals(35, result.get(2).middle().intValue());
        assertFalse(result.get(2).right());
    }

    @Test
    public void testZip_iterables() {
        List<String> names = CommonUtil.asList("Alice", "Bob");
        List<Integer> ages = CommonUtil.asList(25, 30);
        List<Boolean> active = CommonUtil.asList(true, false);

        TriIterator<String, Integer, Boolean> iter = TriIterator.zip(names, ages, active);

        int count = 0;
        while (iter.hasNext()) {
            Triple<String, Integer, Boolean> t = iter.next();
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    public void testZip_iterables_nullIterable() {
        List<String> names = CommonUtil.asList("Alice");
        List<Integer> ages = CommonUtil.asList(25);

        TriIterator<String, Integer, Boolean> iter = TriIterator.zip(names, ages, (Iterable<Boolean>) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testZip_iterables_withDefaults() {
        List<String> names = CommonUtil.asList("Alice");
        List<Integer> ages = CommonUtil.asList(25, 30);
        List<Boolean> active = CommonUtil.asList();

        TriIterator<String, Integer, Boolean> iter = TriIterator.zip(names, ages, active, "Unknown", 0, false);

        List<Triple<String, Integer, Boolean>> result = iter.toList();
        assertEquals(2, result.size());

        assertEquals("Alice", result.get(0).left());
        assertEquals("Unknown", result.get(1).left());
    }

    @Test
    public void testZip_iterators() {
        List<String> names = CommonUtil.asList("Alice", "Bob", "Charlie");
        List<Integer> ages = CommonUtil.asList(25, 30, 35);
        List<Boolean> active = CommonUtil.asList(true, false, true);

        TriIterator<String, Integer, Boolean> iter = TriIterator.zip(names.iterator(), ages.iterator(), active.iterator());

        int count = 0;
        while (iter.hasNext()) {
            iter.next();
            count++;
        }
        assertEquals(3, count);
    }

    @Test
    public void testZip_iterators_nullIterator() {
        List<String> names = CommonUtil.asList("Alice");
        List<Integer> ages = CommonUtil.asList(25);

        TriIterator<String, Integer, Boolean> iter = TriIterator.zip(names.iterator(), ages.iterator(), (Iterator<Boolean>) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testZip_iterators_withDefaults() {
        List<String> names = CommonUtil.asList("Alice");
        List<Integer> ages = CommonUtil.asList(25, 30);
        List<Boolean> active = CommonUtil.asList();

        TriIterator<String, Integer, Boolean> iter = TriIterator.zip(names.iterator(), ages.iterator(), active.iterator(), "Unknown", 0, false);

        List<Triple<String, Integer, Boolean>> result = iter.toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testUnzip_iterable() {
        List<String> items = CommonUtil.asList("1:a:true", "2:b:false", "3:c:true");

        TriIterator<Integer, String, Boolean> iter = TriIterator.unzip(items, (item, triple) -> {
            String[] parts = item.split(":");
            triple.setLeft(Integer.parseInt(parts[0]));
            triple.setMiddle(parts[1]);
            triple.setRight(Boolean.parseBoolean(parts[2]));
        });

        int count = 0;
        while (iter.hasNext()) {
            Triple<Integer, String, Boolean> t = iter.next();
            count++;
            assertEquals(count, t.left().intValue());
        }
        assertEquals(3, count);
    }

    @Test
    public void testUnzip_iterable_null() {
        TriIterator<Integer, String, Boolean> iter = TriIterator.unzip((Iterable<String>) null, (item, triple) -> {
        });
        assertFalse(iter.hasNext());
    }

    @Test
    public void testUnzip_iterator() {
        List<String> items = CommonUtil.asList("1:a", "2:b");
        Iterator<String> iterator = items.iterator();

        TriIterator<Integer, String, String> iter = TriIterator.unzip(iterator, (item, triple) -> {
            String[] parts = item.split(":");
            triple.setLeft(Integer.parseInt(parts[0]));
            triple.setMiddle(parts[1]);
            triple.setRight(parts[1].toUpperCase());
        });

        List<Triple<Integer, String, String>> result = iter.toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testUnzip_iterator_null() {
        TriIterator<Integer, String, Boolean> iter = TriIterator.unzip((Iterator<String>) null, (item, triple) -> {
        });
        assertFalse(iter.hasNext());
    }

    @Test
    public void testForEachRemaining_consumer() {
        String[] names = { "Alice", "Bob" };
        Integer[] ages = { 25, 30 };
        Boolean[] active = { true, false };

        TriIterator<String, Integer, Boolean> iter = TriIterator.zip(names, ages, active);

        List<Triple<String, Integer, Boolean>> result = new ArrayList<>();
        iter.forEachRemaining(result::add);

        assertEquals(2, result.size());
    }

    @Test
    public void testForEachRemaining_triConsumer() {
        String[] names = { "Alice", "Bob", "Charlie" };
        Integer[] ages = { 25, 30, 35 };
        Boolean[] active = { true, false, true };

        TriIterator<String, Integer, Boolean> iter = TriIterator.zip(names, ages, active);

        List<String> results = new ArrayList<>();
        iter.forEachRemaining((name, age, isActive) -> results.add(name + ":" + age + ":" + isActive));

        assertEquals(3, results.size());
        assertEquals("Alice:25:true", results.get(0));
    }

    @Test
    public void testForeachRemaining_throwable() {
        String[] names = { "Alice", "Bob" };
        Integer[] ages = { 25, 30 };
        Boolean[] active = { true, false };

        TriIterator<String, Integer, Boolean> iter = TriIterator.zip(names, ages, active);

        List<String> results = new ArrayList<>();
        iter.foreachRemaining((name, age, isActive) -> results.add(name + ":" + age));

        assertEquals(2, results.size());
    }

    @Test
    public void testSkip() {
        Integer[] nums = { 1, 2, 3, 4, 5 };
        String[] strs = { "a", "b", "c", "d", "e" };
        Boolean[] flags = { true, false, true, false, true };

        TriIterator<Integer, String, Boolean> iter = TriIterator.zip(nums, strs, flags).skip(2);

        List<Triple<Integer, String, Boolean>> result = iter.toList();
        assertEquals(3, result.size());
        assertEquals(3, result.get(0).left().intValue());
    }

    @Test
    public void testSkip_zero() {
        Integer[] nums = { 1, 2 };
        String[] strs = { "a", "b" };
        Boolean[] flags = { true, false };

        TriIterator<Integer, String, Boolean> iter = TriIterator.zip(nums, strs, flags).skip(0);

        List<Triple<Integer, String, Boolean>> result = iter.toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testSkip_negative() {
        Integer[] nums = { 1 };
        String[] strs = { "a" };
        Boolean[] flags = { true };

        assertThrows(IllegalArgumentException.class, () -> TriIterator.zip(nums, strs, flags).skip(-1));
    }

    @Test
    public void testSkip_moreThanAvailable() {
        Integer[] nums = { 1, 2 };
        String[] strs = { "a", "b" };
        Boolean[] flags = { true, false };

        TriIterator<Integer, String, Boolean> iter = TriIterator.zip(nums, strs, flags).skip(10);

        assertFalse(iter.hasNext());
    }

    @Test
    public void testLimit() {
        AtomicInteger counter = new AtomicInteger(0);
        TriIterator<Integer, Integer, Integer> iter = TriIterator.<Integer, Integer, Integer> generate(triple -> {
            int n = counter.getAndIncrement();
            triple.set(n, n * 2, n * 3);
        }).limit(5);

        List<Triple<Integer, Integer, Integer>> result = iter.toList();
        assertEquals(5, result.size());
    }

    @Test
    public void testLimit_zero() {
        Integer[] nums = { 1, 2, 3 };
        String[] strs = { "a", "b", "c" };
        Boolean[] flags = { true, false, true };

        TriIterator<Integer, String, Boolean> iter = TriIterator.zip(nums, strs, flags).limit(0);

        assertFalse(iter.hasNext());
    }

    @Test
    public void testLimit_negative() {
        Integer[] nums = { 1 };
        String[] strs = { "a" };
        Boolean[] flags = { true };

        assertThrows(IllegalArgumentException.class, () -> TriIterator.zip(nums, strs, flags).limit(-1));
    }

    @Test
    public void testFilter() {
        Integer[] nums = { 1, 2, 3, 4, 5 };
        String[] strs = { "a", "bb", "ccc", "dddd", "eeeee" };
        Boolean[] flags = { true, false, true, false, true };

        TriIterator<Integer, String, Boolean> iter = TriIterator.zip(nums, strs, flags).filter((num, str, flag) -> flag && num > 2);

        List<Triple<Integer, String, Boolean>> result = iter.toList();
        assertEquals(2, result.size());
        assertEquals(3, result.get(0).left().intValue());
        assertEquals(5, result.get(1).left().intValue());
    }

    @Test
    public void testFilter_nullPredicate() {
        Integer[] nums = { 1 };
        String[] strs = { "a" };
        Boolean[] flags = { true };

        assertThrows(IllegalArgumentException.class, () -> TriIterator.zip(nums, strs, flags).filter(null));
    }

    @Test
    public void testFilter_noneMatch() {
        Integer[] nums = { 1, 2 };
        String[] strs = { "a", "b" };
        Boolean[] flags = { false, false };

        TriIterator<Integer, String, Boolean> iter = TriIterator.zip(nums, strs, flags).filter((num, str, flag) -> flag);

        assertFalse(iter.hasNext());
    }

    @Test
    public void testMap() {
        String[] names = { "Alice", "Bob", "Charlie" };
        Integer[] ages = { 25, 30, 35 };
        String[] cities = { "NYC", "LA", "Chicago" };

        ObjIterator<String> iter = TriIterator.zip(names, ages, cities).map((name, age, city) -> name + " (" + age + ") from " + city);

        List<String> result = iter.toList();
        assertEquals(3, result.size());
        assertEquals("Alice (25) from NYC", result.get(0));
    }

    @Test
    public void testFirst() {
        String[] names = { "Alice", "Bob" };
        Integer[] ages = { 25, 30 };
        Boolean[] active = { true, false };

        TriIterator<String, Integer, Boolean> iter = TriIterator.zip(names, ages, active);

        Optional<Triple<String, Integer, Boolean>> first = iter.first();
        assertTrue(first.isPresent());
        assertEquals("Alice", first.get().left());
    }

    @Test
    public void testFirst_empty() {
        TriIterator<String, Integer, Boolean> iter = TriIterator.empty();

        Optional<Triple<String, Integer, Boolean>> first = iter.first();
        assertFalse(first.isPresent());
    }

    @Test
    public void testLast() {
        String[] names = { "Alice", "Bob", "Charlie" };
        Integer[] ages = { 25, 30, 35 };
        Boolean[] active = { true, false, true };

        TriIterator<String, Integer, Boolean> iter = TriIterator.zip(names, ages, active);

        Optional<Triple<String, Integer, Boolean>> last = iter.last();
        assertTrue(last.isPresent());
        assertEquals("Charlie", last.get().left());
    }

    @Test
    public void testLast_empty() {
        TriIterator<String, Integer, Boolean> iter = TriIterator.empty();

        Optional<Triple<String, Integer, Boolean>> last = iter.last();
        assertFalse(last.isPresent());
    }

    @Test
    public void testStream() {
        String[] names = { "Alice", "Bob" };
        Integer[] ages = { 25, 30 };
        Boolean[] active = { true, false };

        Stream<String> stream = TriIterator.zip(names, ages, active).stream((name, age, isActive) -> name + ":" + age);

        List<String> result = stream.toList();
        assertEquals(2, result.size());
        assertEquals("Alice:25", result.get(0));
    }

    @Test
    public void testToArray() {
        String[] names = { "Alice", "Bob" };
        Integer[] ages = { 25, 30 };
        Boolean[] active = { true, false };

        TriIterator<String, Integer, Boolean> iter = TriIterator.zip(names, ages, active);

        Triple<String, Integer, Boolean>[] array = iter.toArray();
        assertEquals(2, array.length);
        assertEquals("Alice", array[0].left());
    }

    @Test
    public void testToArray_withArray() {
        String[] names = { "Alice" };
        Integer[] ages = { 25 };
        Boolean[] active = { true };

        TriIterator<String, Integer, Boolean> iter = TriIterator.zip(names, ages, active);

        Triple[] array = new Triple[0];
        Triple[] result = iter.toArray(array);
        assertEquals(1, result.length);
    }

    @Test
    public void testToList() {
        String[] names = { "Alice", "Bob", "Charlie" };
        Integer[] ages = { 25, 30, 35 };
        Boolean[] active = { true, false, true };

        TriIterator<String, Integer, Boolean> iter = TriIterator.zip(names, ages, active);

        List<Triple<String, Integer, Boolean>> list = iter.toList();
        assertEquals(3, list.size());
        assertEquals("Bob", list.get(1).left());
    }

    @Test
    public void testToMultiList() {
        String[] names = { "Alice", "Bob", "Charlie" };
        Integer[] ages = { 25, 30, 35 };
        Boolean[] active = { true, false, true };

        TriIterator<String, Integer, Boolean> iter = TriIterator.zip(names, ages, active);

        Triple<List<String>, List<Integer>, List<Boolean>> result = iter.toMultiList(ArrayList::new);

        assertEquals(3, result.left().size());
        assertEquals(3, result.middle().size());
        assertEquals(3, result.right().size());

        assertEquals("Alice", result.left().get(0));
        assertEquals(30, result.middle().get(1).intValue());
        assertTrue(result.right().get(2));
    }

    @Test
    public void testToMultiSet() {
        String[] names = { "Alice", "Bob", "Alice" };
        Integer[] ages = { 25, 30, 25 };
        Boolean[] active = { true, false, true };

        TriIterator<String, Integer, Boolean> iter = TriIterator.zip(names, ages, active);

        Triple<Set<String>, Set<Integer>, Set<Boolean>> result = iter.toMultiSet(HashSet::new);

        assertEquals(2, result.left().size());
        assertEquals(2, result.middle().size());
        assertEquals(2, result.right().size());

        assertTrue(result.left().contains("Alice"));
        assertTrue(result.left().contains("Bob"));
    }

    @Test
    public void testSkipLimitFilter() {
        Integer[] nums = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        String[] strs = new String[10];
        Boolean[] flags = new Boolean[10];
        for (int i = 0; i < 10; i++) {
            strs[i] = "str" + i;
            flags[i] = i % 2 == 0;
        }

        TriIterator<Integer, String, Boolean> iter = TriIterator.zip(nums, strs, flags).skip(2).limit(5).filter((num, str, flag) -> flag);

        List<Triple<Integer, String, Boolean>> result = iter.toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testMapAfterFilter() {
        Integer[] nums = { 1, 2, 3, 4 };
        String[] strs = { "a", "b", "c", "d" };
        Boolean[] flags = { true, false, true, false };

        ObjIterator<Integer> iter = TriIterator.zip(nums, strs, flags).filter((num, str, flag) -> flag).map((num, str, flag) -> num * 10);

        List<Integer> result = iter.toList();
        assertEquals(2, result.size());
        assertEquals(10, result.get(0).intValue());
        assertEquals(30, result.get(1).intValue());
    }
}
