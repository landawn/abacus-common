package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.function.BiPredicate;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.TriPredicate;

public class FnNotTest extends FnTestSupport {

    @Test
    public void testNotNull() {
        assertFalse(Fn.notNull().test(null));
        assertTrue(Fn.notNull().test("x"));
        final Predicate<Map.Entry<String, String>> byValue = Fn.notNull(Map.Entry::getValue);
        assertTrue(byValue.test(new AbstractMap.SimpleEntry<>("k", "v")));
        assertFalse(byValue.test(new AbstractMap.SimpleEntry<>("k", null)));
        assertThrows(IllegalArgumentException.class, () -> Fn.notNull(null));
    }

    @Test
    public void testNotEmpty() {
        assertFalse(Fn.notEmpty().test(""));
        assertFalse(Fn.notEmpty().test(null));
        assertTrue(Fn.notEmpty().test("x"));
        final Predicate<Map.Entry<String, String>> byValue = Fn.notEmpty(Map.Entry::getValue);
        assertTrue(byValue.test(new AbstractMap.SimpleEntry<>("k", "hello")));
        assertFalse(byValue.test(new AbstractMap.SimpleEntry<>("k", "")));
        assertThrows(IllegalArgumentException.class, () -> Fn.notEmpty(null));
    }

    @Test
    public void testNotBlank() {
        assertFalse(Fn.notBlank().test("   "));
        assertFalse(Fn.notBlank().test(null));
        assertTrue(Fn.notBlank().test("hello"));
        final Predicate<Map.Entry<String, String>> byValue = Fn.notBlank(Map.Entry::getValue);
        assertTrue(byValue.test(new AbstractMap.SimpleEntry<>("k", "hello")));
        assertFalse(byValue.test(new AbstractMap.SimpleEntry<>("k", "  ")));
        assertThrows(IllegalArgumentException.class, () -> Fn.notBlank(null));
    }

    @Test
    public void testNotEmptyArray() {
        assertFalse(Fn.notEmptyArray().test(null));
        assertFalse(Fn.notEmptyArray().test(new String[0]));
        assertTrue(Fn.notEmptyArray().test(new String[] { "a" }));
    }

    @Test
    public void testNotEmptyCollection() {
        assertFalse(Fn.<List<String>> notEmptyCollection().test(null));
        assertFalse(Fn.notEmptyCollection().test(Collections.emptyList()));
        assertTrue(Fn.notEmptyCollection().test(List.of("a")));
    }

    @Test
    public void testNotEmptyMap() {
        assertFalse(Fn.<Map<String, String>> notEmptyMap().test(null));
        assertFalse(Fn.notEmptyMap().test(Collections.emptyMap()));
        assertTrue(Fn.notEmptyMap().test(Map.of("a", "b")));
    }

    @Test
    public void testNotEqual() {
        assertFalse(Fn.notEqual("a").test("a"));
        assertTrue(Fn.notEqual("a").test("b"));
        assertTrue(Fn.notEqual("a").test(null));
        assertFalse(Fn.notEqual(null).test(null));
        assertTrue(Fn.<String, String> notEqual().test("a", "b"));
        assertFalse(Fn.<String, String> notEqual().test("a", "a"));
    }

    @Test
    public void testNotIn() {
        assertFalse(Fn.notIn(List.of("a", "b")).test("a"));
        assertTrue(Fn.notIn(List.of("a", "b")).test("c"));
        assertTrue(Fn.notIn(new java.util.ArrayList<>(List.of("a"))).test(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.notIn(null));
    }

    @Test
    public void testNotStartsWith() {
        assertFalse(Fn.notStartsWith("ab").test("abc"));
        assertTrue(Fn.notStartsWith("ab").test("xbc"));
        assertTrue(Fn.notStartsWith("ab").test(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.notStartsWith(null));
    }

    @Test
    public void testNotEndsWith() {
        assertFalse(Fn.notEndsWith("bc").test("abc"));
        assertTrue(Fn.notEndsWith("bc").test("abx"));
        assertThrows(IllegalArgumentException.class, () -> Fn.notEndsWith(null));
    }

    @Test
    public void testNotContains() {
        assertFalse(Fn.notContains("b").test("abc"));
        assertTrue(Fn.notContains("z").test("abc"));
        assertThrows(IllegalArgumentException.class, () -> Fn.notContains(null));
    }

    @Test
    public void testNot() {
        assertFalse(Fn.not((Predicate<String>) s -> s.startsWith("a")).test("abc"));
        assertTrue(Fn.not((Predicate<String>) s -> s.startsWith("a")).test("xbc"));
        final BiPredicate<Integer, Integer> notGreater = Fn.not((Integer a, Integer b) -> a > b);
        assertTrue(notGreater.test(1, 2));
        assertFalse(notGreater.test(3, 1));
        final TriPredicate<Integer, Integer, Integer> notAllPositive = Fn.not((a, b, c) -> a > 0 && b > 0 && c > 0);
        assertTrue(notAllPositive.test(1, -1, 1));
        assertFalse(notAllPositive.test(1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> Fn.not((java.util.function.Predicate<Object>) null));
        assertThrows(IllegalArgumentException.class, () -> Fn.not((java.util.function.BiPredicate<Object, Object>) null));
        assertThrows(IllegalArgumentException.class, () -> Fn.not((TriPredicate<Object, Object, Object>) null));
    }
}
