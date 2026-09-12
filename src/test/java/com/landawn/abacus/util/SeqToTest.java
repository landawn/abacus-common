package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

public class SeqToTest extends SeqTestSupport {

    @Test
    public void testToArray() throws Exception {
        assertArrayEquals(new Object[] { 1, 2, 3 }, Seq.of(1, 2, 3).toArray());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Seq.of(1, 2, 3).toArray(Integer[]::new));
        assertArrayEquals(new String[] { "a", "b", "c" }, Seq.of("a", "b", "c").toArray(String[]::new));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2).toArray(null));
        assertArrayEquals(new Object[] { 1, 2, 3 }, Seq.of(1, 2, 3).toArrayAndClose());
    }

    @Test
    public void testToList() throws Exception {
        assertEquals(Arrays.asList(1, 2, 3), Seq.of(1, 2, 3).toList());
    }

    @Test
    public void testToSet() throws Exception {
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), Seq.of(1, 2, 2, 3).toSet());
    }

    @Test
    public void testToCollection() throws Exception {
        assertEquals(Arrays.asList(1, 2, 3), Seq.of(1, 2, 3).toCollection(ArrayList::new));
        assertEquals(new LinkedList<>(Arrays.asList(1, 2, 3)), Seq.of(1, 2, 3).toCollection(LinkedList::new));
        TreeSet<Integer> tree = Seq.of(3, 1, 4, 1, 5).toCollection(TreeSet::new);
        assertEquals(4, tree.size());
        assertEquals(Integer.valueOf(1), tree.first());
    }

    @Test
    public void testToImmutableList() throws Exception {
        ImmutableList<Integer> list = Seq.of(1, 2, 3).toImmutableList();
        assertEquals(Arrays.asList(1, 2, 3), list);
        assertThrows(UnsupportedOperationException.class, () -> list.add(4));
    }

    @Test
    public void testToImmutableSet() throws Exception {
        ImmutableSet<Integer> set = Seq.of(1, 2, 2, 3).toImmutableSet();
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), set);
        assertThrows(UnsupportedOperationException.class, () -> set.add(4));
    }

    @Test
    public void testToListThenApply() throws Exception {
        assertEquals(3, Seq.of(1, 2, 3).toListThenApply(List::size).intValue());
        assertEquals(6, Seq.of(1, 2, 3).toListThenApply(list -> list.stream().mapToInt(Integer::intValue).sum()).intValue());
    }

    @Test
    public void testToListThenAccept() throws Exception {
        AtomicInteger size = new AtomicInteger();
        Seq.of(1, 2, 3).toListThenAccept(list -> size.set(list.size()));
        assertEquals(3, size.get());
        List<Integer> target = new ArrayList<>();
        Seq.of(1, 2, 3).toListThenAccept(target::addAll);
        assertEquals(Arrays.asList(1, 2, 3), target);
    }

    @Test
    public void testToSetThenApply() throws Exception {
        assertEquals(3, Seq.of(1, 2, 3, 2, 1).toSetThenApply(Set::size).intValue());
        assertEquals(6, Seq.of(1, 2, 3, 2).toSetThenApply(set -> set.stream().mapToInt(Integer::intValue).sum()).intValue());
    }

    @Test
    public void testToSetThenAccept() throws Exception {
        AtomicInteger size = new AtomicInteger();
        Seq.of(1, 2, 3, 2, 1).toSetThenAccept(set -> size.set(set.size()));
        assertEquals(3, size.get());
        Set<Integer> target = new HashSet<>();
        Seq.of(1, 2, 3, 2).toSetThenAccept(target::addAll);
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), target);
    }

    @Test
    public void testToCollectionThenApply() throws Exception {
        assertEquals("1-2-3", Seq.of(1, 2, 3)
                .toCollectionThenApply(LinkedList::new, list -> list.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining("-"))));
        assertEquals("apple", Seq.of("apple", "banana", "cherry").toCollectionThenApply(LinkedList::new, LinkedList::getFirst));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2).toCollectionThenApply(null, Fn.identity()));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2).toCollectionThenApply(ArrayList::new, null));
    }

    @Test
    public void testToCollectionThenAccept() throws Exception {
        List<Integer> target = new ArrayList<>();
        Seq.of(1, 2, 3).toCollectionThenAccept(LinkedList::new, target::addAll);
        assertEquals(Arrays.asList(1, 2, 3), target);
        List<Integer> sorted = new ArrayList<>();
        Seq.of(3, 1, 4, 1, 5).toCollectionThenAccept(TreeSet::new, set -> set.forEach(sorted::add));
        assertEquals(Arrays.asList(1, 3, 4, 5), sorted);
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2).toCollectionThenAccept(null, s -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2).toCollectionThenAccept(ArrayList::new, null));
    }

    @Test
    public void testToMap() throws Exception {
        Map<String, Integer> map = Seq.of("a", "bb", "ccc").toMap(s -> s, String::length);
        assertEquals(1, map.get("a").intValue());
        assertEquals(2, map.get("bb").intValue());
        assertEquals(3, map.get("ccc").intValue());
        assertThrows(IllegalStateException.class, () -> Seq.of("apple", "apricot").map(s -> Pair.of(s.charAt(0), s.length())).toMap(Pair::left, Pair::right));

        Map<String, Integer> linked = Seq.of("a", "bb").toMap(s -> s, String::length, Suppliers.ofLinkedHashMap());
        assertTrue(linked instanceof LinkedHashMap);

        Map<Character, Integer> merged = Seq.of("apple", "apricot", "banana").toMap(s -> s.charAt(0), String::length, Integer::sum);
        assertEquals(Integer.valueOf(12), merged.get('a'));
        assertEquals(Integer.valueOf(6), merged.get('b'));

        Map<Character, Integer> tree = Seq.of("apple", "apricot", "banana").toMap(s -> s.charAt(0), String::length, Integer::sum, TreeMap::new);
        assertTrue(tree instanceof TreeMap);

        TreeMap<Boolean, Integer> sums = Seq.of(1, 2, 3, 4, 5).toMap(n -> n % 2 == 0, Fn.identity(), Integer::sum, TreeMap::new);
        assertEquals(6, sums.get(true).intValue());
        assertEquals(9, sums.get(false).intValue());
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).toMap(null, Fn.identity(), Integer::sum, TreeMap::new));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).toMap(Fn.identity(), null, Integer::sum, TreeMap::new));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 1).toMap(Fn.identity(), Fn.identity(), null, TreeMap::new));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).toMap(Fn.identity(), Fn.identity(), Integer::sum, null));
    }

    @Test
    public void testToImmutableMap() throws Exception {
        ImmutableMap<String, Integer> map = Seq.of("a", "bb").toImmutableMap(s -> s, String::length);
        assertEquals(1, map.get("a").intValue());
        assertThrows(UnsupportedOperationException.class, () -> map.put("c", 3));

        ImmutableMap<Character, Integer> merged = Seq.of("apple", "apricot", "banana").toImmutableMap(s -> s.charAt(0), String::length, Integer::sum);
        assertEquals(Integer.valueOf(12), merged.get('a'));
        assertThrows(UnsupportedOperationException.class, () -> merged.put('d', 3));
    }

    @Test
    public void testToMultimap() throws Exception {
        ListMultimap<Integer, String> byLen = Seq.of("a", "bb", "c", "dd").toMultimap(String::length);
        assertEquals(Arrays.asList("a", "c"), byLen.get(1));
        assertEquals(Arrays.asList("bb", "dd"), byLen.get(2));

        ListMultimap<Character, String> byFirst = Seq.of("apple", "apricot", "banana", "avocado").toMultimap(s -> s.charAt(0));
        assertEquals(Arrays.asList("apple", "apricot", "avocado"), byFirst.get('a'));

        ListMultimap<Character, String> tree = Seq.of("apple", "apricot", "banana", "avocado")
                .toMultimap(s -> s.charAt(0), Suppliers.ofListMultimap(TreeMap.class));
        assertTrue(tree.toMap() instanceof TreeMap);

        ListMultimap<Character, Integer> lengths = Seq.of("apple", "apricot", "banana").toMultimap(s -> s.charAt(0), String::length);
        assertEquals(Arrays.asList(5, 7), lengths.get('a'));

        Multimap<Integer, Integer, ? extends java.util.Collection<Integer>> setMm = Seq.of(1, 2, 3, 4, 5, 6).toMultimap(n -> n % 3, Suppliers.ofSetMultimap());
        assertTrue(setMm.get(0).contains(3));
        assertTrue(setMm.get(0).contains(6));

        assertThrows(IllegalArgumentException.class, () -> Seq.of("a").toMultimap(null, String::length, Suppliers.ofListMultimap()));
        assertThrows(IllegalArgumentException.class, () -> Seq.of("a").toMultimap(s -> s.charAt(0), null, Suppliers.ofListMultimap()));
        assertThrows(IllegalArgumentException.class, () -> Seq.of("a").toMultimap(s -> s.charAt(0), String::length, null));
    }

    @Test
    public void testToMultiset() throws Exception {
        Multiset<Integer> multiset = Seq.of(1, 2, 2, 3, 3, 3).toMultiset();
        assertEquals(1, multiset.count(1));
        assertEquals(2, multiset.count(2));
        assertEquals(3, multiset.count(3));
        Multiset<Integer> supplied = Seq.of(1, 2, 2, 3, 3, 3).toMultiset(Multiset::new);
        assertEquals(2, supplied.count(2));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2).toMultiset(null));
    }

    @Test
    public void testToDataset() throws Exception {
        List<Map<String, Object>> data = Arrays.asList(CommonUtil.asMap("id", 1, "name", "Alice"), CommonUtil.asMap("id", 2, "name", "Bob"));
        Dataset dataset = Seq.of(data).toDataset();
        assertEquals(2, dataset.size());
        assertTrue(dataset.columnNames().containsAll(Arrays.asList("id", "name")));
        assertEquals((Integer) 1, dataset.moveToRow(0).get("id"));
        assertEquals("Bob", dataset.moveToRow(1).get("name"));

        List<String> columnNames = Arrays.asList("UserID", "UserName");
        Dataset named = Seq.of(Arrays.asList(Arrays.asList(1, "Alice"), Arrays.asList(2, "Bob"))).toDataset(columnNames);
        assertEquals(columnNames, named.columnNames());
        assertEquals((Integer) 1, named.moveToRow(0).get("UserID"));
        assertNotNull(Seq.of(1, 2, 3).toDataset(Arrays.asList("value")));
    }
}
