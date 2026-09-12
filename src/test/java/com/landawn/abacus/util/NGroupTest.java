package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

public class NGroupTest extends NTestSupport {

    @Test
    public void testGroupBy_array() {
        String[] arr = { "apple", "apricot", "banana", "blueberry", "avocado" };
        Map<Character, List<String>> grouped = N.groupBy(arr, s -> s.charAt(0));
        assertEquals(List.of("apple", "apricot", "avocado"), grouped.get('a'));
        assertEquals(List.of("banana", "blueberry"), grouped.get('b'));
        assertTrue(N.groupBy((String[]) null, s -> s.charAt(0)).isEmpty());

        TreeMap<Character, List<String>> tree = N.groupBy(arr, s -> s.charAt(0), TreeMap::new);
        assertEquals(Arrays.asList('a', 'b'), new ArrayList<>(tree.keySet()));

        String[] ranged = { "one", "two", "three", "four", "five" };
        Map<Character, List<String>> range = N.groupBy(ranged, 1, 4, s -> s.charAt(0));
        assertEquals(List.of("two", "three"), range.get('t'));
        assertEquals(List.of("four"), range.get('f'));
        assertNull(range.get('o'));
        TreeMap<Character, List<String>> rangeTree = N.groupBy(ranged, 1, 4, s -> s.charAt(0), TreeMap::new);
        assertEquals(Arrays.asList('f', 't'), new ArrayList<>(rangeTree.keySet()));

        assertEquals(4, N.groupBy(new String[] { "aa", "bb", "cc", "dd" }, String::length).get(2).size());

        Map<Integer, List<String>> withNulls = N.groupBy(new String[] { "one", null, "two", null, "three" }, s -> s == null ? null : s.length());
        assertEquals(Arrays.asList(null, null), withNulls.get(null));
        assertEquals(Arrays.asList("one", "two"), withNulls.get(3));
    }

    @Test
    public void testGroupBy_iterableIterator() {
        List<String> list = Arrays.asList("apple", "apricot", "banana");
        assertEquals(List.of("apple", "apricot"), N.groupBy(list, s -> s.charAt(0)).get('a'));
        assertEquals(List.of("apple", "apricot"), N.groupBy(list, s -> s.charAt(0), Suppliers.ofTreeMap()).get('a'));
        assertEquals(List.of("apple", "apricot"), N.groupBy(list.iterator(), s -> s.charAt(0)).get('a'));
        assertEquals(List.of("apple", "apricot"), N.groupBy(list.iterator(), s -> s.charAt(0), Suppliers.ofTreeMap()).get('a'));
        assertTrue(N.groupBy(Collections.<String> emptyList(), s -> s).isEmpty());

        List<String> ranged = Arrays.asList("one", "two", "three", "four", "five");
        assertEquals(List.of("two", "three"), N.groupBy(ranged, 1, 4, s -> s.charAt(0)).get('t'));
        assertEquals(List.of("four"), N.groupBy(CommonUtil.toLinkedList("one", "two", "three", "four", "five"), 1, 4, s -> s.charAt(0), TreeMap::new).get('f'));
        LinkedHashSet<String> set = new LinkedHashSet<>(ranged);
        assertTrue(
                N.groupBy(set, 1, 4, s -> s.charAt(0), TreeMap::new).containsKey('t') || N.groupBy(set, 1, 4, s -> s.charAt(0), TreeMap::new).containsKey('f'));

        List<String> colored = Arrays.asList("apple:red", "banana:yellow", "apricot:orange");
        assertEquals(List.of("red", "orange"), N.groupBy(colored, s -> s.charAt(0), s -> s.split(":")[1]).get('a'));
        assertEquals(List.of("red", "orange"), N.groupBy(colored, s -> s.charAt(0), s -> s.split(":")[1], TreeMap::new).get('a'));
        assertEquals(List.of("red", "orange"), N.groupBy(colored.iterator(), s -> s.charAt(0), s -> s.split(":")[1]).get('a'));
        assertEquals(List.of("red", "orange"), N.groupBy(colored.iterator(), s -> s.charAt(0), s -> s.split(":")[1], TreeMap::new).get('a'));

        assertEquals(Arrays.asList('o', 't'), N.groupBy(stringList, String::length, s -> s.charAt(0)).get(3));
    }

    @Test
    public void testGroupBy_collector() {
        List<String> list = Arrays.asList("apple", "apricot", "banana", "blueberry");
        assertEquals("5,7", N.groupBy(list, s -> s.charAt(0), Collectors.mapping(it -> String.valueOf(it.length()), Collectors.joining(","))).get('a'));
        assertEquals(2L, N.groupBy(list, s -> s.charAt(0), Collectors.counting(), TreeMap::new).get('a'));
        assertEquals(2L, N.groupBy(Arrays.asList("apple", "apricot", "banana").iterator(), s -> s.charAt(0), Collectors.counting()).get('a'));
        assertEquals(2L, N.groupBy(Arrays.asList("apple", "apricot", "banana").iterator(), s -> s.charAt(0), Collectors.counting(), TreeMap::new).get('a'));

        assertThrows(IllegalArgumentException.class, () -> N.groupBy(list, s -> s.charAt(0), (Collector<String, ?, Long>) null));
        assertThrows(IllegalArgumentException.class, () -> N.groupBy(list.iterator(), s -> s.charAt(0), (Collector<String, ?, Long>) null, TreeMap::new));

        assertEquals(Long.valueOf(2), N.groupBy(stringList, String::length, Collectors.counting()).get(3));

        String[] words = { "one", "two", "three", "four", "five", "six" };
        assertEquals("one-two-six", N.groupBy(Arrays.asList(words), String::length, Collectors.joining("-")).get(3));
        assertEquals("one", N.groupBy(Arrays.asList(words), String::length, Collectors.reducing((a, b) -> a)).get(3).get());

        List<Person> people = Arrays.asList(new Person("Alice", 25, "Engineering"), new Person("Bob", 30, "Engineering"), new Person("Charlie", 35, "Sales"),
                new Person("David", 25, "Sales"), new Person("Eve", 30, "Engineering"));
        Map<String, Double> avgAgeByDept = N.groupBy(people, Person::getDepartment, Collectors.averagingInt(Person::getAge));
        assertEquals(28.33, avgAgeByDept.get("Engineering"), 0.01);
        assertEquals(30.0, avgAgeByDept.get("Sales"), 0.01);
    }
}
