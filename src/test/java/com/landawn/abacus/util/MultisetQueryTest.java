package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;

public class MultisetQueryTest extends MultisetTestSupport {

    // allMaxOccurrences

    @Test
    public void testAllMaxOccurrences() {
        Multiset<String> multiset = new Multiset<>();
        assertFalse(multiset.allMaxOccurrences().isPresent());

        multiset.addAll(Arrays.asList("a", "a", "a", "b", "c", "c", "d", "d", "d"));
        Optional<Pair<Integer, List<String>>> allMax = multiset.allMaxOccurrences();
        assertTrue(allMax.isPresent());
        assertEquals(3, allMax.get().left().intValue());
        assertEquals(CommonUtil.toSet("a", "d"), new HashSet<>(allMax.get().right()));

        multiset.clear();
        multiset.add("x", 1);
        multiset.add("y", 1);
        allMax = multiset.allMaxOccurrences();
        assertTrue(allMax.isPresent());
        assertEquals(1, allMax.get().left().intValue());
        assertEquals(CommonUtil.toSet("x", "y"), new HashSet<>(allMax.get().right()));
    }

    @Test
    public void testAllMaxOccurrences_Empty() {
        assertFalse(multiset.allMaxOccurrences().isPresent());
    }

    @Test
    public void testAllMaxOccurrences_MultipleMax() {
        multiset.add("a", 5);
        multiset.add("b", 5);
        multiset.add("c", 2);
        Optional<Pair<Integer, List<String>>> allMax = multiset.allMaxOccurrences();
        assertTrue(allMax.isPresent());
        assertEquals(5, allMax.get().left().intValue());
        assertEquals(2, allMax.get().right().size());
        assertTrue(allMax.get().right().contains("a"));
        assertTrue(allMax.get().right().contains("b"));
    }

    @Test
    public void testAllMaxOccurrences_SingleMax() {
        multiset.add("a", 5);
        multiset.add("b", 3);
        multiset.add("c", 2);
        Optional<Pair<Integer, List<String>>> allMax = multiset.allMaxOccurrences();
        assertTrue(allMax.isPresent());
        assertEquals(5, allMax.get().left().intValue());
        assertEquals(1, allMax.get().right().size());
        assertTrue(allMax.get().right().contains("a"));
    }

    @Test
    public void testAllMinAndAllMaxOccurrences_WithOrderedTies() {
        Multiset<String> ordered = new Multiset<>(LinkedHashMap.class);
        ordered.add("alpha", 1);
        ordered.add("beta", 3);
        ordered.add("gamma", 3);
        ordered.add("delta", 1);

        Optional<Pair<Integer, List<String>>> min = ordered.allMinOccurrences();
        Optional<Pair<Integer, List<String>>> max = ordered.allMaxOccurrences();

        assertTrue(min.isPresent());
        assertEquals(1, min.get().left().intValue());
        assertEquals(Arrays.asList("alpha", "delta"), min.get().right());
        assertTrue(max.isPresent());
        assertEquals(3, max.get().left().intValue());
        assertEquals(Arrays.asList("beta", "gamma"), max.get().right());
    }

    // allMinOccurrences

    @Test
    public void testAllMinOccurrences() {
        Multiset<String> multiset = new Multiset<>();
        assertFalse(multiset.allMinOccurrences().isPresent());

        multiset.addAll(Arrays.asList("a", "a", "b", "c", "c", "c", "d"));
        Optional<Pair<Integer, List<String>>> allMin = multiset.allMinOccurrences();
        assertTrue(allMin.isPresent());
        assertEquals(1, allMin.get().left().intValue());
        assertEquals(CommonUtil.toSet("b", "d"), new HashSet<>(allMin.get().right()));

        multiset.clear();
        multiset.add("x", 5);
        multiset.add("y", 5);
        allMin = multiset.allMinOccurrences();
        assertTrue(allMin.isPresent());
        assertEquals(5, allMin.get().left().intValue());
        assertEquals(CommonUtil.toSet("x", "y"), new HashSet<>(allMin.get().right()));
    }

    @Test
    public void testAllMinOccurrences_Empty() {
        assertFalse(multiset.allMinOccurrences().isPresent());
    }

    @Test
    public void testAllMinOccurrences_MultipleMin() {
        multiset.add("a", 1);
        multiset.add("b", 1);
        multiset.add("c", 3);
        Optional<Pair<Integer, List<String>>> allMin = multiset.allMinOccurrences();
        assertTrue(allMin.isPresent());
        assertEquals(1, allMin.get().left().intValue());
        assertEquals(2, allMin.get().right().size());
        assertTrue(allMin.get().right().contains("a"));
        assertTrue(allMin.get().right().contains("b"));
    }

    @Test
    public void testAllMinOccurrences_SingleMin() {
        multiset.add("a", 2);
        multiset.add("b", 3);
        multiset.add("c", 4);
        Optional<Pair<Integer, List<String>>> allMin = multiset.allMinOccurrences();
        assertTrue(allMin.isPresent());
        assertEquals(2, allMin.get().left().intValue());
        assertEquals(1, allMin.get().right().size());
        assertTrue(allMin.get().right().contains("a"));
    }

    @Test
    public void testAverageOfOccurrences() {
        multiset.add("a", 2);
        multiset.add("b", 4);
        multiset.add("c", 6);
        OptionalDouble avg = multiset.averageOfOccurrences();
        assertTrue(avg.isPresent());
        assertEquals(4.0, avg.getAsDouble(), 0.001);
    }

    @Test
    public void testAverageOfOccurrences_Empty() {
        assertFalse(multiset.averageOfOccurrences().isPresent());
    }

    // contains

    @Test
    public void testContains() {
        Multiset<String> multiset = Multiset.of("a", "b");
        assertTrue(multiset.contains("a"));
        assertTrue(multiset.contains("b"));
        assertFalse(multiset.contains("c"));
        assertFalse(multiset.contains(null));
    }

    @Test
    public void testContains_Absent() {
        assertFalse(multiset.contains("apple"));
    }

    @Test
    public void testContains_Null() {
        multiset.add(null, 1);
        assertTrue(multiset.contains(null));
    }

    @Test
    public void testContains_Present() {
        multiset.add("apple", 3);
        assertTrue(multiset.contains("apple"));
    }

    // containsAll

    @Test
    public void testContainsAll() {
        Multiset<String> multiset = Multiset.of("a", "b", "c", "a");
        assertTrue(multiset.containsAll(Arrays.asList("a", "b")));
        assertTrue(multiset.containsAll(Arrays.asList("c")));
        assertTrue(multiset.containsAll(Collections.emptyList()));
        assertFalse(multiset.containsAll(Arrays.asList("a", "d")));
        assertFalse(multiset.containsAll(Arrays.asList("d", "e")));
    }

    @Test
    public void testContainsAll_Empty() {
        assertTrue(multiset.containsAll(new ArrayList<>()));
    }

    @Test
    public void testContainsAll_False() {
        multiset.add("a", 2);
        assertFalse(multiset.containsAll(Arrays.asList("a", "b")));
    }

    @Test
    public void testContainsAll_Null() {
        assertTrue(multiset.containsAll(null));
    }

    @Test
    public void testContainsAll_True() {
        multiset.add("a", 2);
        multiset.add("b", 3);
        multiset.add("c", 1);
        assertTrue(multiset.containsAll(Arrays.asList("a", "b")));
    }

    // count

    @Test
    public void testCount() {
        assertEquals(0, multiset.count("test"));

        multiset.add("test", 5);
        assertEquals(5, multiset.count("test"));
    }

    @Test
    public void testCount_Deprecated() {
        multiset.add("apple", 5);
        assertEquals(5, multiset.count("apple"));
        assertEquals(0, multiset.count("nonexistent"));
    }

    @Test
    @DisplayName("Test deprecated count() method")
    public void testCountDeprecated() {
        multiset.add("item", 7);
        assertEquals(7, multiset.count("item"));
    }

    @Test
    public void testDeprecatedCount() {
        Multiset<String> multiset = Multiset.of("a", "b", "a");
        assertEquals(2, multiset.count("a"));
    }

    // countOfDistinctElements

    @Test
    public void testCountOfDistinctElements() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        multiset.add("c", 5);
        assertEquals(3, multiset.countOfDistinctElements());
    }

    @Test
    public void testCountOfDistinctElements_AfterRemoval() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        multiset.add("c", 1);
        assertEquals(3, multiset.countOfDistinctElements());

        multiset.removeAllOccurrencesOf("b");
        assertEquals(2, multiset.countOfDistinctElements());
    }

    @Test
    public void testCountOfDistinctElements_Empty() {
        assertEquals(0, multiset.countOfDistinctElements());
    }

    @Test
    public void testGetCount() {
        assertEquals(0, multiset.getCount("test"));

        multiset.add("test", 5);
        assertEquals(5, multiset.getCount("test"));

        assertEquals(0, multiset.getCount(null));
        multiset.add(null, 2);
        assertEquals(2, multiset.getCount(null));
    }

    @Test
    public void testOccurrencesOfAndGetCount() {
        Multiset<String> multiset = Multiset.of("a", "b", "a");
        assertEquals(2, multiset.getCount("a"));
        assertEquals(2, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b"));
        assertEquals(1, multiset.getCount("b"));
        assertEquals(0, multiset.getCount("c"));
        assertEquals(0, multiset.getCount("c"));
        assertEquals(0, multiset.getCount(null));
    }

    // isEmpty

    @Test
    public void testIsEmpty() {
        Multiset<String> multiset = new Multiset<>();
        assertTrue(multiset.isEmpty());
        multiset.add("a");
        assertFalse(multiset.isEmpty());
        multiset.remove("a");
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testIsEmpty_False() {
        multiset.add("a");
        assertFalse(multiset.isEmpty());
    }

    @Test
    public void testIsEmpty_True() {
        assertTrue(multiset.isEmpty());
    }

    // maxOccurrences

    @Test
    public void testMaxOccurrences() {
        Multiset<String> multiset = new Multiset<>();
        assertFalse(multiset.maxOccurrences().isPresent());

        multiset.addAll(Arrays.asList("a", "a", "b", "c", "c", "c"));
        Optional<Pair<Integer, String>> max = multiset.maxOccurrences();
        assertTrue(max.isPresent());
        assertEquals(3, max.get().left().intValue());
        assertEquals("c", max.get().right());

        multiset.add("d", 3);
        max = multiset.maxOccurrences();
        assertTrue(max.isPresent());
        assertEquals(3, max.get().left().intValue());
        assertTrue(CommonUtil.toSet("c", "d").contains(max.get().right()));

        multiset.clear();
        multiset.add("y", 2);
        max = multiset.maxOccurrences();
        assertTrue(max.isPresent());
        assertEquals(2, max.get().left().intValue());
        assertEquals("y", max.get().right());
    }

    @Test
    public void testMaxOccurrences_Empty() {
        assertFalse(multiset.maxOccurrences().isPresent());
    }

    @Test
    public void testMaxOccurrences_MultipleElements() {
        multiset.add("a", 3);
        multiset.add("b", 5);
        multiset.add("c", 2);
        Optional<Pair<Integer, String>> max = multiset.maxOccurrences();
        assertTrue(max.isPresent());
        assertEquals(5, max.get().left().intValue());
        assertEquals("b", max.get().right());
    }

    @Test
    public void testMaxOccurrences_SingleElement() {
        multiset.add("a", 5);
        Optional<Pair<Integer, String>> max = multiset.maxOccurrences();
        assertTrue(max.isPresent());
        assertEquals(5, max.get().left().intValue());
        assertEquals("a", max.get().right());
    }

    // minOccurrences

    @Test
    public void testMinOccurrences() {
        Multiset<String> multiset = new Multiset<>();
        assertFalse(multiset.minOccurrences().isPresent());

        multiset.addAll(Arrays.asList("a", "a", "b", "c", "c", "c"));
        Optional<Pair<Integer, String>> min = multiset.minOccurrences();
        assertTrue(min.isPresent());
        assertEquals(1, min.get().left().intValue());
        assertEquals("b", min.get().right());

        multiset.add("d", 1);
        min = multiset.minOccurrences();
        assertTrue(min.isPresent());
        assertEquals(1, min.get().left().intValue());
        assertTrue(CommonUtil.toSet("b", "d").contains(min.get().right()));

        multiset.clear();
        multiset.add("x", 5);
        min = multiset.minOccurrences();
        assertTrue(min.isPresent());
        assertEquals(5, min.get().left().intValue());
        assertEquals("x", min.get().right());
    }

    @Test
    public void testMinOccurrences_Empty() {
        assertFalse(multiset.minOccurrences().isPresent());
    }

    @Test
    public void testMinOccurrences_MultipleElements() {
        multiset.add("a", 3);
        multiset.add("b", 1);
        multiset.add("c", 2);
        Optional<Pair<Integer, String>> min = multiset.minOccurrences();
        assertTrue(min.isPresent());
        assertEquals(1, min.get().left().intValue());
        assertEquals("b", min.get().right());
    }

    @Test
    public void testMinOccurrences_SingleElement() {
        multiset.add("a", 5);
        Optional<Pair<Integer, String>> min = multiset.minOccurrences();
        assertTrue(min.isPresent());
        assertEquals(5, min.get().left().intValue());
        assertEquals("a", min.get().right());
    }

    @Test
    public void testOccurrencesOf() {
        multiset.add("apple", 3);
        assertEquals(3, multiset.getCount("apple"));
        assertEquals(0, multiset.getCount("banana"));
        assertEquals(0, multiset.getCount("nonexistent"));

        multiset.add(null, 3);
        assertEquals(3, multiset.getCount(null));
    }

    // size

    @Test
    public void testSize() {
        multiset.add("a", 3);
        multiset.add("b", 2);
        assertEquals(5, multiset.size());
    }

    @Test
    public void testSize_Empty() {
        assertEquals(0, multiset.size());
    }

    @Test
    public void testSize_saturatesAtIntegerMaxValue() {
        Multiset<String> multiset = new Multiset<>();
        multiset.add("a", Integer.MAX_VALUE);
        multiset.add("b", 1);

        assertEquals(Integer.MAX_VALUE, multiset.size());
        assertEquals((long) Integer.MAX_VALUE + 1, multiset.sumOfOccurrences());
    }

    @Test
    @DisplayName("Test size saturation with multiple maximum counts")
    public void testSizeSaturationWithMultipleMaximumCounts() {
        intMultiset.add(1, Integer.MAX_VALUE);
        intMultiset.add(2, Integer.MAX_VALUE);

        assertEquals(Integer.MAX_VALUE, intMultiset.size());
        assertEquals(2L * Integer.MAX_VALUE, intMultiset.sumOfOccurrences());
    }

    // sumOfOccurrences

    @Test
    public void testSumOfOccurrences() {
        multiset.add("a", 3);
        multiset.add("b", 5);
        multiset.add("c", 2);
        assertEquals(10, multiset.sumOfOccurrences());
    }

    @Test
    public void testSumOfOccurrences_Empty() {
        assertEquals(0, multiset.sumOfOccurrences());
    }

    @Test
    public void testSumOfOccurrences_Large() {
        multiset.add("a", Integer.MAX_VALUE / 2);
        multiset.add("b", Integer.MAX_VALUE / 2);
        long sum = multiset.sumOfOccurrences();
        assertEquals((long) (Integer.MAX_VALUE / 2) * 2, sum);
    }
}
