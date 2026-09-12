package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.Predicate;

public class FnApplyTest extends FnTestSupport {

    @Test
    public void testApplyByKey() {
        final Map.Entry<String, Integer> entry = CommonUtil.newImmutableEntry("key", 100);
        assertEquals(3, Fn.<String, Integer, Integer> applyByKey(String::length).apply(entry));
        assertThrows(IllegalArgumentException.class, () -> Fn.applyByKey(null));
    }

    @Test
    public void testApplyByValue() {
        final Map.Entry<String, Integer> entry = CommonUtil.newImmutableEntry("key", 100);
        assertEquals(200, Fn.<String, Integer, Integer> applyByValue(v -> v * 2).apply(entry));
        assertThrows(IllegalArgumentException.class, () -> Fn.applyByValue(null));
    }

    @Test
    public void testApplyKeyVal() {
        assertEquals("key=123", Fn.<String, Integer, String> applyKeyVal((k, v) -> k + "=" + v).apply(new AbstractMap.SimpleEntry<>("key", 123)));
        assertThrows(IllegalArgumentException.class, () -> Fn.applyKeyVal(null));
    }

    @Test
    public void testMapKeyAndValue() {
        assertEquals(CommonUtil.newEntry("KEY", "val"), Fn.<String, String, String> mapKey(String::toUpperCase).apply(Map.entry("key", "val")));
        assertEquals(CommonUtil.newEntry("k", 10), Fn.<String, Integer, Integer> mapValue(v -> v * 2).apply(Map.entry("k", 5)));
        assertThrows(IllegalArgumentException.class, () -> Fn.mapKey(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.mapValue(null));
    }

    @Test
    public void testTestByKeyAndValue() {
        assertTrue(Fn.<Integer, String> testByKey(k -> k > 5).test(Map.entry(10, "v")));
        assertTrue(Fn.<String, Integer> testByValue(v -> v > 50).test(Map.entry("k", 100)));
        assertTrue(Fn.<String, Integer> testKeyVal((k, v) -> k.equals("a") && v == 1).test(Map.entry("a", 1)));
        assertThrows(IllegalArgumentException.class, () -> Fn.testByKey(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.testByValue(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.testKeyVal(null));
    }

    @Test
    public void testAcceptByKeyAndValue() {
        final AtomicReference<String> key = new AtomicReference<>();
        final AtomicInteger value = new AtomicInteger();
        Fn.<String, Integer> acceptByKey(key::set).accept(Map.entry("k", 1));
        Fn.<String, Integer> acceptByValue(value::set).accept(Map.entry("k", 7));
        Fn.<String, Integer> acceptKeyVal((k, v) -> {
            key.set(k);
            value.set(v);
        }).accept(Map.entry("a", 2));
        assertEquals("a", key.get());
        assertEquals(2, value.get());
        assertThrows(IllegalArgumentException.class, () -> Fn.acceptByKey(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.acceptByValue(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.acceptKeyVal(null));
    }

    @Test
    public void testAcceptIf() {
        final List<String> seen = new ArrayList<>();
        final Consumer<String> acceptIfNotNull = Fn.acceptIfNotNull(seen::add);
        acceptIfNotNull.accept("a");
        acceptIfNotNull.accept(null);
        assertEquals(List.of("a"), seen);

        seen.clear();
        Fn.acceptIf((Predicate<String>) s -> s.startsWith("a"), seen::add).accept("abc");
        Fn.acceptIf((Predicate<String>) s -> s.startsWith("a"), seen::add).accept("x");
        assertEquals(List.of("abc"), seen);

        final AtomicReference<String> other = new AtomicReference<>();
        Fn.acceptIfOrElse((String s) -> s.length() > 1, seen::add, other::set).accept("z");
        assertEquals("z", other.get());
        assertThrows(IllegalArgumentException.class, () -> Fn.acceptIfNotNull(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.acceptIf(null, (Consumer<String>) seen::add));
    }

    @Test
    public void testApplyIfNotNull() {
        final Function<String, Collection<Integer>> emptyOnNull = Fn.applyIfNotNullOrEmpty(s -> List.of(s.length()));
        assertEquals(List.of(3), emptyOnNull.apply("abc"));
        assertTrue(emptyOnNull.apply(null).isEmpty());

        assertEquals(5, Fn.applyIfNotNullOrDefault(String::length, (Integer n) -> n, -1).apply("hello"));
        assertEquals(-1, Fn.applyIfNotNullOrDefault(String::length, (Integer n) -> n, -1).apply(null));
        assertEquals(0, Fn.applyIfNotNullOrElseGet(String::length, (Integer n) -> n, () -> 0).apply(null));
        assertEquals("HI", Fn.applyIfOrElseDefault((Predicate<String>) s -> s != null, String::toUpperCase, "n/a").apply("hi"));
        assertEquals("n/a", Fn.applyIfOrElseDefault((Predicate<String>) s -> s != null, String::toUpperCase, "n/a").apply(null));
        assertEquals("gone", Fn.applyIfOrElseGet((Predicate<String>) s -> s != null, String::toUpperCase, () -> "gone").apply(null));
    }

    @Test
    public void testFlatmapValue() {
        final Map<String, List<Integer>> src = Map.of("a", List.of(1, 2), "b", List.of(3));
        final List<Map<String, Integer>> rows = Fn.<String, Integer> flatmapValue().apply(src);
        assertEquals(2, rows.size());
    }

    // FINDING G20-008 (doc-only contract pin): the applyKeyVal javadoc example now builds its source map through
    // a TreeMap, so the element order it prints is reproducible (Map.of alone randomizes it per JVM run).
    @Test
    public void testApplyKeyVal_OrderedSourceMapYieldsTheDocumentedOrder() {
        final Map<String, Integer> inventory = new TreeMap<>(Map.of("apple", 5, "banana", 12, "cherry", 3));

        final List<String> descriptions = inventory.entrySet()
                .stream()
                .map(Fn.<String, Integer, String> applyKeyVal((fruit, count) -> String.format("%s: %d in stock", fruit, count)))
                .collect(Collectors.toList());
        assertEquals(List.of("apple: 5 in stock", "banana: 12 in stock", "cherry: 3 in stock"), descriptions);

        final int totalValue = inventory.entrySet()
                .stream()
                .map(Fn.<String, Integer, Integer> applyKeyVal((k, v) -> k.length() * v))
                .mapToInt(Integer::intValue)
                .sum();
        assertEquals(115, totalValue);
    }
}
