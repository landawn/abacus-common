package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.function.ToIntFunction;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.function.ObjIntFunction;

public class MultisetComputeTest extends MultisetTestSupport {
    @Test
    public void testComputeIfAbsent_Absent() {
        int count = multiset.computeIfAbsent("apple", e -> 5);
        assertEquals(5, count);
        assertEquals(5, multiset.getCount("apple"));
    }

    @Test
    public void testComputeIfAbsent_Present() {
        multiset.add("apple", 3);
        int count = multiset.computeIfAbsent("apple", e -> 10);
        assertEquals(3, count);
        assertEquals(3, multiset.getCount("apple"));
    }

    @Test
    public void testComputeIfAbsent_ZeroValue() {
        int count = multiset.computeIfAbsent("apple", e -> 0);
        assertEquals(0, count);
        assertEquals(0, multiset.getCount("apple"));
    }

    @Test
    public void testComputeIfAbsent_NegativeValue() {
        int count = multiset.computeIfAbsent("apple", e -> -1);
        assertEquals(0, count);
        assertEquals(0, multiset.getCount("apple"));
    }

    @Test
    public void testComputeIfAbsent() {
        Multiset<String> multiset = new Multiset<>();
        ToIntFunction<String> computer = s -> s.length();

        assertEquals(1, multiset.computeIfAbsent("a", computer));
        assertEquals(1, multiset.getCount("a"));

        assertEquals(1, multiset.computeIfAbsent("a", computer));
        assertEquals(1, multiset.getCount("a"));

        assertEquals(3, multiset.computeIfAbsent("xyz", computer));
        assertEquals(3, multiset.getCount("xyz"));

        assertEquals(0, multiset.computeIfAbsent("zero", s -> 0));
        assertEquals(0, multiset.getCount("zero"));

    }

    @Test
    public void testComputeIfAbsent_ReturnZero() {
        int result = multiset.computeIfAbsent("a", e -> 0);
        assertEquals(0, result);
        assertFalse(multiset.contains("a"));
    }

    @Test
    public void testComputeIfAbsent_Null() {
        assertThrows(IllegalArgumentException.class, () -> multiset.computeIfAbsent("apple", null));
    }

    @Test
    public void testComputeIfAbsent_nullFunction() {
        Multiset<String> multiset = new Multiset<>();
        assertThrows(IllegalArgumentException.class, () -> multiset.computeIfAbsent("a", null));
    }

    @Test
    public void testComputeIfPresent_Present() {
        multiset.add("apple", 3);
        int newCount = multiset.computeIfPresent("apple", (e, count) -> count * 2);
        assertEquals(6, newCount);
        assertEquals(6, multiset.getCount("apple"));
    }

    @Test
    public void testComputeIfPresent_Absent() {
        int newCount = multiset.computeIfPresent("apple", (e, count) -> 10);
        assertEquals(0, newCount);
        assertEquals(0, multiset.getCount("apple"));
    }

    @Test
    public void testComputeIfPresent() {
        Multiset<String> multiset = Multiset.of("a", "a", "b");
        ObjIntFunction<String, Integer> remapper = (s, count) -> s.equals("a") ? count + 1 : 0;

        assertEquals(3, multiset.computeIfPresent("a", remapper));
        assertEquals(3, multiset.getCount("a"));

        assertEquals(0, multiset.computeIfPresent("b", remapper));
        assertEquals(0, multiset.getCount("b"));

        assertEquals(0, multiset.computeIfPresent("c", remapper));
        assertEquals(0, multiset.getCount("c"));
        assertEquals(3, multiset.getCount("a"));
    }

    @Test
    public void testComputeIfPresent_ToZero() {
        multiset.add("apple", 3);
        int newCount = multiset.computeIfPresent("apple", (e, count) -> 0);
        assertEquals(0, newCount);
        assertFalse(multiset.contains("apple"));
    }

    @Test
    public void testComputeIfPresent_ReturnZero() {
        multiset.add("a", 3);
        int result = multiset.computeIfPresent("a", (e, count) -> 0);
        assertEquals(0, result);
        assertFalse(multiset.contains("a"));
    }

    @Test
    public void testComputeIfPresent_Null() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> multiset.computeIfPresent("apple", null));
    }

    @Test
    public void testComputeIfPresent_nullFunction() {
        Multiset<String> multiset = Multiset.of("a");
        assertThrows(IllegalArgumentException.class, () -> multiset.computeIfPresent("a", null));
    }

    @Test
    public void testCompute_Absent() {
        int newCount = multiset.compute("apple", (e, count) -> count + 5);
        assertEquals(5, newCount);
        assertEquals(5, multiset.getCount("apple"));
    }

    @Test
    public void testCompute_Present() {
        multiset.add("apple", 3);
        int newCount = multiset.compute("apple", (e, count) -> count + 2);
        assertEquals(5, newCount);
        assertEquals(5, multiset.getCount("apple"));
    }

    @Test
    public void testCompute() {
        Multiset<String> multiset = new Multiset<>();
        ObjIntFunction<String, Integer> computer = (s, oldCount) -> {
            if (s.equals("add")) {
                return oldCount + 2;
            }
            if (s.equals("set")) {
                return 5;
            }
            if (s.equals("remove")) {
                return 0;
            }
            if (s.equals("no_change_if_present")) {
                return oldCount > 0 ? oldCount : 0;
            }
            if (s.equals("add_if_absent")) {
                return oldCount == 0 ? 1 : oldCount;
            }
            return 0;
        };

        assertEquals(2, multiset.compute("add", computer));
        assertEquals(2, multiset.getCount("add"));

        assertEquals(4, multiset.compute("add", computer));
        assertEquals(4, multiset.getCount("add"));

        assertEquals(5, multiset.compute("set", computer));
        assertEquals(5, multiset.getCount("set"));

        assertEquals(5, multiset.compute("set", computer));
        assertEquals(5, multiset.getCount("set"));

        multiset.setCount("remove_target", 3);
        assertEquals(0, multiset.compute("remove_target", computer));
        assertEquals(0, multiset.getCount("remove_target"));

        assertEquals(0, multiset.compute("remove_absent", computer));
        assertEquals(0, multiset.getCount("remove_absent"));
    }

    @Test
    public void testCompute_ToZero() {
        multiset.add("apple", 3);
        int newCount = multiset.compute("apple", (e, count) -> 0);
        assertEquals(0, newCount);
        assertFalse(multiset.contains("apple"));
    }

    @Test
    public void testCompute_ReturnZero() {
        multiset.add("a", 3);
        int result = multiset.compute("a", (e, count) -> 0);
        assertEquals(0, result);
        assertFalse(multiset.contains("a"));
    }

    @Test
    public void testCompute_Null() {
        assertThrows(IllegalArgumentException.class, () -> multiset.compute("apple", null));
    }

    @Test
    public void testCompute_nullFunction() {
        Multiset<String> multiset = new Multiset<>();
        assertThrows(IllegalArgumentException.class, () -> multiset.compute("a", null));
    }

    @Test
    public void testComputeMethodsReturnZeroWhenNonPositiveResultRemovesEntry() {
        Multiset<String> set = Multiset.of("a", "a");

        assertEquals(0, set.computeIfPresent("a", (e, count) -> -1));
        assertEquals(0, set.getCount("a"));

        set = Multiset.of("a", "a");
        assertEquals(0, set.compute("a", (e, count) -> -1));
        assertEquals(0, set.getCount("a"));

        set = Multiset.of("a", "a");
        assertEquals(0, set.merge("a", 1, (oldCount, value) -> -1));
        assertEquals(0, set.getCount("a"));
    }
}
