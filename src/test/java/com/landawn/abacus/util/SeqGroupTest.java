package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.Collectors;

public class SeqGroupTest extends SeqTestSupport {

    @Test
    public void testGroupBy() throws Exception {
        Map<Integer, List<String>> grouped = Seq.of("a", "bb", "ccc", "dd", "e").groupBy(String::length).toMap(Map.Entry::getKey, Map.Entry::getValue);
        assertEquals(Arrays.asList("a", "e"), grouped.get(1));
        assertEquals(Arrays.asList("bb", "dd"), grouped.get(2));
        assertEquals(Arrays.asList("ccc"), grouped.get(3));
        assertTrue(Seq.<String, Exception> empty().groupBy(String::length).toList().isEmpty());
        assertEquals(1, Seq.of("hello").groupBy(String::length).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "bb", "aa", "ccc", "bbb").groupBy(String::length).skip(2).count());
    }

    @Test
    public void testGroupBy_MapFactory() throws Exception {
        List<Map.Entry<Integer, List<String>>> ordered = Seq.of("a", "bb", "ccc", "dd", "e").groupBy(String::length, Suppliers.ofTreeMap()).toList();
        assertEquals(1, ordered.get(0).getKey().intValue());
        assertEquals(2, ordered.get(1).getKey().intValue());
        assertEquals(3, ordered.get(2).getKey().intValue());

        Map<Integer, List<String>> insertion = Seq.of("ccc", "a", "bb")
                .groupBy(String::length, Suppliers.ofLinkedHashMap())
                .toMap(Map.Entry::getKey, Map.Entry::getValue, Suppliers.ofLinkedHashMap());
        assertEquals(Arrays.asList(3, 1, 2), new ArrayList<>(insertion.keySet()));
    }

    @Test
    public void testGroupBy_ValueMapper() throws Exception {
        Map<Integer, List<String>> groups = Seq.of("Alice:25", "Bob:30", "Charlie:25", "David:30")
                .groupBy(s -> Integer.parseInt(s.split(":")[1]), s -> s.split(":")[0])
                .toMap(Map.Entry::getKey, Map.Entry::getValue);
        assertEquals(Arrays.asList("Alice", "Charlie"), groups.get(25));
        assertEquals(Arrays.asList("Bob", "David"), groups.get(30));

        List<Map.Entry<String, List<Integer>>> tree = Seq.of("1:Electronics", "2:Books", "3:Electronics", "4:Books")
                .groupBy(s -> s.split(":")[1], s -> Integer.parseInt(s.split(":")[0]), TreeMap::new)
                .toList();
        assertEquals("Books", tree.get(0).getKey());
        assertEquals(Arrays.asList(2, 4), tree.get(0).getValue());
    }

    @Test
    public void testGroupBy_MergeFunction() throws Exception {
        Map<Integer, Integer> sums = Seq.of(1, 4, 7, 2, 5, 8, 3, 6, 9).groupBy(n -> n % 3, n -> n, Integer::sum).toMap(Map.Entry::getKey, Map.Entry::getValue);
        assertEquals(18, sums.get(0).intValue());
        assertEquals(12, sums.get(1).intValue());
        assertEquals(15, sums.get(2).intValue());

        List<Map.Entry<String, Integer>> maxByDept = Seq.of("Sales:100", "IT:150", "Sales:200", "IT:120")
                .groupBy(s -> s.split(":")[0], s -> Integer.parseInt(s.split(":")[1]), Integer::max, TreeMap::new)
                .toList();
        assertEquals("IT", maxByDept.get(0).getKey());
        assertEquals(150, maxByDept.get(0).getValue().intValue());
        assertEquals("Sales", maxByDept.get(1).getKey());
        assertEquals(200, maxByDept.get(1).getValue().intValue());
    }

    @Test
    public void testGroupBy_Collector() throws Exception {
        Map<Integer, Long> counts = Seq.of("a", "bb", "ccc", "dd", "e")
                .groupBy(String::length, Collectors.counting())
                .toMap(Map.Entry::getKey, Map.Entry::getValue);
        assertEquals(2L, counts.get(1).longValue());
        assertEquals(2L, counts.get(2).longValue());
        assertEquals(1L, counts.get(3).longValue());

        Map<Character, String> joined = Seq.of("apple", "apricot", "banana", "avocado")
                .groupBy(s -> s.charAt(0), Collectors.joining(","), TreeMap::new)
                .toMap(Map.Entry::getKey, Map.Entry::getValue);
        assertFalse(joined instanceof TreeMap);
        assertEquals("apple,apricot,avocado", joined.get('a'));

        Map<Character, Long> lens = Seq.of("apple", "apricot", "banana", "avocado")
                .groupBy(s -> s.charAt(0), String::length, Collectors.summingLong(len -> (long) len))
                .toMap(Map.Entry::getKey, Map.Entry::getValue);
        assertEquals(19L, lens.get('a'));
        assertEquals(6L, lens.get('b'));

        Map<Integer, String> upper = Seq.of("a", "bb", "ccc", "dd", "e")
                .groupBy(String::length, String::toUpperCase, Collectors.joining(","))
                .toMap(Map.Entry::getKey, Map.Entry::getValue);
        assertEquals("A,E", upper.get(1));

        List<Map.Entry<String, Long>> linked = Seq.of("Credit:100", "Debit:50", "Credit:200", "Debit:75")
                .groupBy(s -> s.split(":")[0], s -> Integer.parseInt(s.split(":")[1]), Collectors.summingLong(Integer::longValue), LinkedHashMap::new)
                .toList();
        assertEquals("Credit", linked.get(0).getKey());
        assertEquals(300L, linked.get(0).getValue().longValue());
    }

    @Test
    public void testGroupTo() throws Exception {
        Map<Integer, List<String>> byLen = Seq.of("a", "bb", "ccc", "dd").groupTo(String::length);
        assertEquals(Arrays.asList("a"), byLen.get(1));
        assertEquals(Arrays.asList("bb", "dd"), byLen.get(2));

        Map<Character, List<String>> byFirst = Seq.of("apple", "apricot", "banana", "blueberry").groupTo(s -> s.charAt(0));
        assertEquals(Arrays.asList("apple", "apricot"), byFirst.get('a'));

        TreeMap<Integer, List<Integer>> tree = Seq.of(1, 2, 3, 4, 5, 6).groupTo(n -> n % 3, Suppliers.ofTreeMap());
        assertEquals(Arrays.asList(3, 6), tree.get(0));

        Map<Integer, List<String>> upper = Seq.of("apple", "banana", "cherry").groupTo(String::length, String::toUpperCase);
        assertEquals(Arrays.asList("APPLE"), upper.get(5));
        assertEquals(Arrays.asList("BANANA", "CHERRY"), upper.get(6));

        LinkedHashMap<Character, List<Integer>> linked = Seq.of("apple", "apricot", "banana").groupTo(s -> s.charAt(0), String::length, LinkedHashMap::new);
        assertEquals(Arrays.asList(5, 7), linked.get('a'));
        assertThrows(IllegalArgumentException.class, () -> Seq.of("a").groupTo(null, String::length, Suppliers.ofLinkedHashMap()));
        assertThrows(IllegalArgumentException.class, () -> Seq.of("a").groupTo(s -> s.charAt(0), (Throwables.Function) null, Suppliers.ofLinkedHashMap()));
        assertThrows(IllegalArgumentException.class, () -> Seq.of("a").groupTo(s -> s.charAt(0), String::length, (Supplier) null));
    }

    @Test
    public void testGroupTo_Collector() throws Exception {
        Map<Character, Long> counts = Seq.of("apple", "apricot", "banana", "blueberry").groupTo(s -> s.charAt(0), Collectors.counting());
        assertEquals(2L, counts.get('a'));
        assertEquals(2L, counts.get('b'));

        TreeMap<Character, String> joined = Seq.of("apple", "apricot", "banana").groupTo(s -> s.charAt(0), Collectors.joining(", "), TreeMap::new);
        assertEquals("apple, apricot", joined.get('a'));

        Map<Character, String> upper = Seq.of("apple", "apricot", "banana", "blueberry")
                .groupTo(s -> s.charAt(0), String::toUpperCase, Collectors.joining(", "));
        assertEquals("APPLE, APRICOT", upper.get('a'));

        Map<Character, Optional<Integer>> maxLen = Seq.of("apple", "apricot", "banana")
                .groupTo(s -> s.charAt(0), String::length, Collectors.max(), Suppliers.ofLinkedHashMap());
        assertEquals(7, maxLen.get('a').get().intValue());
        assertThrows(IllegalArgumentException.class, () -> Seq.of("a").groupTo(null, String::length, Collectors.counting(), LinkedHashMap::new));
        assertThrows(IllegalArgumentException.class, () -> Seq.of("a").groupTo(s -> s.charAt(0), null, Collectors.counting(), LinkedHashMap::new));
        assertThrows(IllegalArgumentException.class, () -> Seq.of("a").groupTo(s -> s.charAt(0), String::length, null, LinkedHashMap::new));
        assertThrows(IllegalArgumentException.class, () -> Seq.of("a").groupTo(s -> s.charAt(0), String::length, Collectors.counting(), null));
    }

    // --- G12-004 (doc): the javadoc example for groupBy(keyMapper, mapFactory) used an inexact TreeMap::new, which
    // --- leaves this overload and groupBy(keyMapper, valueMapper) equally applicable - it did not compile. The
    // --- corrected example (a lambda factory) is pinned here.
    @Test
    public void testGroupBy_MapFactoryExampleUsesALambdaFactory() throws Exception {
        final Seq<String, Exception> seq = Seq.of("a", "bb", "ccc");
        final List<String> lines = new ArrayList<>();

        seq.groupBy(String::length, () -> new TreeMap<Integer, List<String>>())
                .forEach(entry -> lines.add("Length " + entry.getKey() + ": " + entry.getValue()));

        assertEquals(Arrays.asList("Length 1: [a]", "Length 2: [bb]", "Length 3: [ccc]"), lines);
    }
}
