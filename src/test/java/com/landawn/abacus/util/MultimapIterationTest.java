package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.stream.Stream;

public class MultimapIterationTest extends MultimapTestSupport {

    @Test
    public void testConcurrentModificationDuringIteration() {
        multimap.put("key1", 1);
        multimap.put("key2", 2);

        assertThrows(ConcurrentModificationException.class, () -> {
            for (Map.Entry<String, List<Integer>> entry : multimap) {
                multimap.put("key3", 3);
            }
        });
    }

    @Test
    public void testFlatForEach() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2));
        mm.put("b", 3);
        List<Pair<String, Integer>> pairs = new ArrayList<>();
        mm.forEachKeyValue((k, e) -> pairs.add(Pair.of(k, e)));

        assertEquals(3, pairs.size());
        assertTrue(pairs.contains(Pair.of("a", 1)));
        assertTrue(pairs.contains(Pair.of("a", 2)));
        assertTrue(pairs.contains(Pair.of("b", 3)));
    }

    @Test
    public void testFlatForEach_EmptyMultimap() {
        AtomicInteger count = new AtomicInteger(0);
        listMultimap.forEachKeyValue((k, e) -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    @Test
    public void testFlatForEach_ModificationDuringIteration() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);
        List<Integer> collected = new ArrayList<>();
        listMultimap.forEachKeyValue((k, e) -> collected.add(e));
        assertEquals(3, collected.size());
        assertTrue(collected.contains(10));
        assertTrue(collected.contains(20));
        assertTrue(collected.contains(30));
    }

    @Test
    public void testFlatForEach_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);

        AtomicInteger count = new AtomicInteger(0);
        listMultimap.forEachKeyValue((k, e) -> count.incrementAndGet());
        assertEquals(3, count.get());
    }

    @Test
    public void testForEach() {
        listMultimap.put("a", 1);
        listMultimap.put("a", 2);
        listMultimap.put("b", 3);

        List<String> keys = new ArrayList<>();
        List<Integer> vals = new ArrayList<>();
        listMultimap.forEachKeyValue((k, e) -> {
            keys.add(k);
            vals.add(e);
        });

        assertEquals(3, keys.size());
        assertEquals(3, vals.size());
        assertTrue(vals.containsAll(Arrays.asList(1, 2, 3)));
    }

    @Test
    public void testForEach_NullAction() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> listMultimap.forEachKeyValue((java.util.function.BiConsumer<? super String, ? super Integer>) null));
    }

    // iterator

    @Test
    public void testIterator() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2));
        mm.put("b", 3);
        Iterator<Map.Entry<String, List<Integer>>> it = mm.iterator();
        int count = 0;
        while (it.hasNext()) {
            it.next();
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    public void testIterator_EmptyMultimap() {
        Iterator<Map.Entry<String, List<Integer>>> iter = listMultimap.iterator();
        assertNotNull(iter);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_ForEachRemainingOnEmpty() {
        Iterator<Map.Entry<String, List<Integer>>> iter = listMultimap.iterator();
        AtomicInteger count = new AtomicInteger(0);
        iter.forEachRemaining(e -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    @Test
    public void testIterator_Remove() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);
        Iterator<Map.Entry<String, List<Integer>>> iter = listMultimap.iterator();
        if (iter.hasNext()) {
            iter.next();
            iter.remove();
        }
        assertEquals(1, listMultimap.totalValueCount());
    }

    @Test
    public void testIterator_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        Iterator<Map.Entry<String, List<Integer>>> iter = listMultimap.iterator();
        int count = 0;
        while (iter.hasNext()) {
            iter.next();
            count++;
        }
        assertEquals(2, count);
    }

    // stream

    @Test
    public void testStream() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2));
        mm.put("b", 3);
        assertEquals(2, mm.stream().count());
    }

    @Test
    public void testStream_EmptyMultimap() {
        Stream<Map.Entry<String, List<Integer>>> stream = listMultimap.stream();
        assertNotNull(stream);
        assertEquals(0, stream.count());
    }

    @Test
    public void testStream_Operations() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);
        listMultimap.put("key3", 30);
        long count = listMultimap.stream().filter(e -> e.getKey().startsWith("key")).count();
        assertEquals(3, count);
    }

    @Test
    public void testStream_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        long count = listMultimap.stream().count();
        assertEquals(2, count);
    }

    @Test
    public void testStreamOperations() {
        multimap.putValues("key1", Arrays.asList(1, 2, 3));
        multimap.putValues("key2", Arrays.asList(4, 5));
        multimap.putValues("key3", Arrays.asList(6));

        int sum = multimap.stream().mapToInt(e -> e.getValue().size()).sum();
        assertEquals(6, sum);

        //        Map<String, Integer> maxValues = multimap.entryStream().entries().toMap(Map.Entry::getKey, e -> e.getValue().stream().max(Integer::compare).orElse(0));
        //
        //        assertEquals(Integer.valueOf(3), maxValues.get("key1"));
        //        assertEquals(Integer.valueOf(5), maxValues.get("key2"));
        //        assertEquals(Integer.valueOf(6), maxValues.get("key3"));
    }
}
