package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.function.IntBiFunction;
import com.landawn.abacus.util.function.ObjIntFunction;

public class MultisetUpdateTest extends MultisetTestSupport {

    // compute

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
    public void testCompute_Absent() {
        int newCount = multiset.compute("apple", (e, count) -> count + 5);
        assertEquals(5, newCount);
        assertEquals(5, multiset.getCount("apple"));
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
    public void testCompute_Present() {
        multiset.add("apple", 3);
        int newCount = multiset.compute("apple", (e, count) -> count + 2);
        assertEquals(5, newCount);
        assertEquals(5, multiset.getCount("apple"));
    }

    @Test
    public void testCompute_ReturnZero() {
        multiset.add("a", 3);
        int result = multiset.compute("a", (e, count) -> 0);
        assertEquals(0, result);
        assertFalse(multiset.contains("a"));
    }

    @Test
    public void testCompute_ToZero() {
        multiset.add("apple", 3);
        int newCount = multiset.compute("apple", (e, count) -> 0);
        assertEquals(0, newCount);
        assertFalse(multiset.contains("apple"));
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

    // computeIfAbsent

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
    public void testComputeIfAbsent_Absent() {
        int count = multiset.computeIfAbsent("apple", e -> 5);
        assertEquals(5, count);
        assertEquals(5, multiset.getCount("apple"));
    }

    @Test
    public void testComputeIfAbsent_NegativeValue() {
        int count = multiset.computeIfAbsent("apple", e -> -1);
        assertEquals(0, count);
        assertEquals(0, multiset.getCount("apple"));
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
    public void testComputeIfAbsent_Present() {
        multiset.add("apple", 3);
        int count = multiset.computeIfAbsent("apple", e -> 10);
        assertEquals(3, count);
        assertEquals(3, multiset.getCount("apple"));
    }

    @Test
    public void testComputeIfAbsent_ReturnZero() {
        int result = multiset.computeIfAbsent("a", e -> 0);
        assertEquals(0, result);
        assertFalse(multiset.contains("a"));
    }

    @Test
    public void testComputeIfAbsent_ZeroValue() {
        int count = multiset.computeIfAbsent("apple", e -> 0);
        assertEquals(0, count);
        assertEquals(0, multiset.getCount("apple"));
    }

    // computeIfPresent

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
    public void testComputeIfPresent_Absent() {
        int newCount = multiset.computeIfPresent("apple", (e, count) -> 10);
        assertEquals(0, newCount);
        assertEquals(0, multiset.getCount("apple"));
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
    public void testComputeIfPresent_Present() {
        multiset.add("apple", 3);
        int newCount = multiset.computeIfPresent("apple", (e, count) -> count * 2);
        assertEquals(6, newCount);
        assertEquals(6, multiset.getCount("apple"));
    }

    @Test
    public void testComputeIfPresent_ReturnZero() {
        multiset.add("a", 3);
        int result = multiset.computeIfPresent("a", (e, count) -> 0);
        assertEquals(0, result);
        assertFalse(multiset.contains("a"));
    }

    @Test
    public void testComputeIfPresent_ToZero() {
        multiset.add("apple", 3);
        int newCount = multiset.computeIfPresent("apple", (e, count) -> 0);
        assertEquals(0, newCount);
        assertFalse(multiset.contains("apple"));
    }

    // merge

    @Test
    public void testMerge() {
        Multiset<String> multiset = new Multiset<>();
        IntBiFunction<Integer> merger = (oldCount, value) -> oldCount + value;

        assertEquals(3, multiset.merge("a", 3, merger));
        assertEquals(3, multiset.getCount("a"));

        assertEquals(5, multiset.merge("a", 2, merger));
        assertEquals(5, multiset.getCount("a"));

        assertEquals(1, multiset.merge("b", 1, merger));
        assertEquals(1, multiset.getCount("b"));

        IntBiFunction<Integer> subtractMerger = (oldCount, value) -> oldCount - value;
        multiset.setCount("c", 5);
        assertEquals(0, multiset.merge("c", 5, subtractMerger));
        assertEquals(0, multiset.getCount("c"));

        assertEquals(0, multiset.merge("d", 0, merger));
        assertEquals(0, multiset.getCount("d"));

        assertEquals(7, multiset.merge("e", 7, (ov, v) -> ov + v + 100));
        assertEquals(7, multiset.getCount("e"));

        assertEquals(7 + 8 + 100, multiset.merge("e", 8, (ov, v) -> ov + v + 100));
        assertEquals(115, multiset.getCount("e"));
    }

    @Test
    public void testMerge_Absent() {
        int newCount = multiset.merge("apple", 5, (oldCount, value) -> oldCount + value);
        assertEquals(5, newCount);
        assertEquals(5, multiset.getCount("apple"));
    }

    @Test
    public void testMerge_Null() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> multiset.merge("apple", 5, null));
    }

    @Test
    public void testMerge_nullFunction() {
        Multiset<String> multiset = new Multiset<>();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> multiset.merge("a", 1, null));
    }

    @Test
    public void testMerge_Present() {
        multiset.add("apple", 3);
        int newCount = multiset.merge("apple", 2, (oldCount, value) -> oldCount + value);
        assertEquals(5, newCount);
        assertEquals(5, multiset.getCount("apple"));
    }

    @Test
    public void testMerge_rejectsNegativeValue() {
        final Multiset<String> multiset = Multiset.of("a");
        assertThrows(IllegalArgumentException.class, () -> multiset.merge("a", -1, (oldCount, value) -> oldCount + value));
        assertThrows(IllegalArgumentException.class, () -> multiset.merge("missing", -5, (oldCount, value) -> value));
        assertEquals(1, multiset.getCount("a"));
    }

    @Test
    public void testMerge_ReturnZero() {
        multiset.add("a", 3);
        int result = multiset.merge("a", 1, (oldVal, newVal) -> 0);
        assertEquals(0, result);
        assertFalse(multiset.contains("a"));
    }

    @Test
    public void testMerge_ToZero() {
        multiset.add("apple", 3);
        int newCount = multiset.merge("apple", 0, (oldCount, value) -> 0);
        assertEquals(0, newCount);
        assertFalse(multiset.contains("apple"));
    }

    // setCount

    @Test
    @DisplayName("Test conditional setCount()")
    public void testConditionalSetCount() {
        multiset.add("apple", 3);

        assertTrue(multiset.setCount("apple", 3, 5));
        assertEquals(5, multiset.getCount("apple"));

        assertFalse(multiset.setCount("apple", 3, 7));
        assertEquals(5, multiset.getCount("apple"));

        assertTrue(multiset.setCount("apple", 5, 0));
        assertFalse(multiset.contains("apple"));
    }

    @Test
    public void testSetCount() {
        Multiset<String> multiset = new Multiset<>();
        assertEquals(0, multiset.setCount("a", 3));
        assertEquals(3, multiset.getCount("a"));
        assertEquals(3, multiset.size());

        assertEquals(3, multiset.setCount("a", 5));
        assertEquals(5, multiset.getCount("a"));
        assertEquals(5, multiset.size());

        assertEquals(5, multiset.setCount("a", 0));
        assertEquals(0, multiset.getCount("a"));
        assertTrue(multiset.isEmpty());

        multiset.add("b", 2);
        assertEquals(0, multiset.setCount("c", 1));
        assertEquals(1, multiset.getCount("c"));
        assertEquals(2 + 1, multiset.size());
    }

    @Test
    public void testSetCount_Conditional_Failure() {
        multiset.add("apple", 2);
        boolean updated = multiset.setCount("apple", 3, 5);
        assertFalse(updated);
        assertEquals(2, multiset.getCount("apple"));
    }

    @Test
    public void testSetCount_Conditional_FromZero() {
        boolean updated = multiset.setCount("apple", 0, 5);
        assertTrue(updated);
        assertEquals(5, multiset.getCount("apple"));
    }

    @Test
    public void testSetCount_Conditional_NegativeNew() {
        assertThrows(IllegalArgumentException.class, () -> multiset.setCount("apple", 2, -1));
    }

    @Test
    public void testSetCount_Conditional_NegativeOld() {
        assertThrows(IllegalArgumentException.class, () -> multiset.setCount("apple", -1, 5));
    }

    @Test
    public void testSetCount_Conditional_Success() {
        multiset.add("apple", 2);
        boolean updated = multiset.setCount("apple", 2, 5);
        assertTrue(updated);
        assertEquals(5, multiset.getCount("apple"));
    }

    @Test
    public void testSetCount_Conditional_ToZero() {
        multiset.add("apple", 2);
        boolean updated = multiset.setCount("apple", 2, 0);
        assertTrue(updated);
        assertFalse(multiset.contains("apple"));
    }

    @Test
    public void testSetCount_elementNotPresent_toZero() {
        Multiset<String> multiset = new Multiset<>();
        assertEquals(0, multiset.setCount("a", 0));
        assertEquals(0, multiset.getCount("a"));
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testSetCount_ExistingElement() {
        multiset.add("apple", 3);
        int oldCount = multiset.setCount("apple", 7);
        assertEquals(3, oldCount);
        assertEquals(7, multiset.getCount("apple"));
    }

    @Test
    public void testSetCount_Negative() {
        assertThrows(IllegalArgumentException.class, () -> multiset.setCount("apple", -1));
        assertThrows(IllegalArgumentException.class, () -> new Multiset<String>().setCount("a", -1));
    }

    @Test
    public void testSetCount_NewElement() {
        int oldCount = multiset.setCount("apple", 5);
        assertEquals(0, oldCount);
        assertEquals(5, multiset.getCount("apple"));
    }

    @Test
    public void testSetCount_OverwriteExistingWithHigherValue() {
        multiset.add("a", 2);
        int old = multiset.setCount("a", 10);
        assertEquals(2, old);
        assertEquals(10, multiset.getCount("a"));
    }

    @Test
    public void testSetCount_ToZero() {
        multiset.add("apple", 3);
        int oldCount = multiset.setCount("apple", 0);
        assertEquals(3, oldCount);
        assertEquals(0, multiset.getCount("apple"));
        assertFalse(multiset.contains("apple"));
    }

    @Test
    public void testSetCountConditional() {
        multiset.add("apple", 2);

        assertTrue(multiset.setCount("apple", 2, 5));
        assertEquals(5, multiset.getCount("apple"));

        assertFalse(multiset.setCount("apple", 2, 10));
        assertEquals(5, multiset.getCount("apple"));

        assertTrue(multiset.setCount("banana", 0, 3));
        assertEquals(3, multiset.getCount("banana"));

        assertTrue(multiset.setCount("apple", 5, 0));
        assertFalse(multiset.contains("apple"));

        assertThrows(IllegalArgumentException.class, () -> multiset.setCount("test", -1, 5));
        assertThrows(IllegalArgumentException.class, () -> multiset.setCount("test", 1, -1));
    }

    @Test
    public void testSetCountWithOldOccurrences() {
        Multiset<String> multiset = new Multiset<>();
        assertTrue(multiset.setCount("a", 0, 3));
        assertEquals(3, multiset.getCount("a"));

        assertFalse(multiset.setCount("a", 2, 5));
        assertEquals(3, multiset.getCount("a"));

        assertTrue(multiset.setCount("a", 3, 1));
        assertEquals(1, multiset.getCount("a"));

        assertTrue(multiset.setCount("a", 1, 0));
        assertEquals(0, multiset.getCount("a"));
        assertTrue(multiset.isEmpty());

        assertTrue(multiset.setCount("b", 0, 0));
        assertEquals(0, multiset.getCount("b"));
        assertTrue(multiset.isEmpty());

        multiset.add("c", 5);
        assertTrue(multiset.setCount("c", 5, 5));
        assertEquals(5, multiset.getCount("c"));
    }

    @Test
    public void testSetCountWithOldOccurrences_negativeNew() {
        Multiset<String> multiset = new Multiset<>();
        assertThrows(IllegalArgumentException.class, () -> multiset.setCount("a", 0, -1));
    }

    @Test
    public void testSetCountWithOldOccurrences_negativeOld() {
        Multiset<String> multiset = new Multiset<>();
        assertThrows(IllegalArgumentException.class, () -> multiset.setCount("a", -1, 3));
    }

    // updateAllOccurrences

    @Test
    public void testUpdateAllOccurrences() {
        multiset.add("a", 2);
        multiset.add("b", 3);
        multiset.updateAllOccurrences((element, count) -> count * 2);
        assertEquals(4, multiset.getCount("a"));
        assertEquals(6, multiset.getCount("b"));
    }

    @Test
    public void testUpdateAllOccurrences_DoubleValues() {
        multiset.add("a", 2);
        multiset.add("b", 3);
        multiset.updateAllOccurrences((e, count) -> count * 2);
        assertEquals(4, multiset.getCount("a"));
        assertEquals(6, multiset.getCount("b"));
    }

    @Test
    public void testUpdateAllOccurrences_MixedRetainAndRemove() {
        multiset.add("a", 1);
        multiset.add("b", 2);
        multiset.add("c", 3);

        multiset.updateAllOccurrences((element, count) -> count == 1 ? 0 : count + 1);

        assertEquals(0, multiset.getCount("a"));
        assertEquals(3, multiset.getCount("b"));
        assertEquals(4, multiset.getCount("c"));
    }

    @Test
    public void testUpdateAllOccurrences_Null() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> multiset.updateAllOccurrences(null));
    }

    @Test
    public void testUpdateAllOccurrences_nullFunction() {
        Multiset<String> multiset = new Multiset<>();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> multiset.updateAllOccurrences(null));
    }

    @Test
    public void testUpdateAllOccurrences_returnNull() {
        Multiset<String> multiset = Multiset.of("a", "b");
        multiset.updateAllOccurrences((el, count) -> el.equals("a") ? null : count);
        assertEquals(0, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b"));
    }

    @Test
    public void testUpdateAllOccurrences_ToNull() {
        multiset.add("a", 5);
        multiset.updateAllOccurrences((element, count) -> null);
        assertFalse(multiset.contains("a"));
    }

    @Test
    public void testUpdateAllOccurrences_ToZero() {
        multiset.add("a", 5);
        multiset.add("b", 3);
        multiset.updateAllOccurrences((element, count) -> 0);
        assertFalse(multiset.contains("a"));
        assertFalse(multiset.contains("b"));
    }

    @Test
    public void testC343_updateAllOccurrencesAppliesUpdatesAndRemovals() {
        final Multiset<String> ms = new Multiset<>();
        ms.add("a", 1);
        ms.add("b", 2);
        ms.add(null, 3);
        ms.add("😀", 4);

        ms.updateAllOccurrences((e, n) -> e == null ? null : (n == 1 ? -5 : n * 10));

        assertEquals(0, ms.getCount("a"));
        assertEquals(20, ms.getCount("b"));
        assertEquals(0, ms.getCount(null));
        assertFalse(ms.contains(null));
        assertEquals(40, ms.getCount("😀"));
        assertEquals(2, ms.countOfDistinctElements());
    }

    @Test
    public void testC343_updateAllOccurrencesIsAllOrNothingWhenFunctionThrows() {
        final Supplier<Map<String, Object>> supplier = LinkedHashMap::new;
        final Multiset<String> ms = new Multiset<>(supplier);
        ms.add("a", 1);
        ms.add("b", 2);
        ms.add("c", 3);

        assertThrows(IllegalStateException.class, () -> ms.updateAllOccurrences((e, n) -> {
            if (e.equals("a")) {
                return 0;
            } else if (e.equals("b")) {
                return n * 10;
            }
            throw new IllegalStateException("boom");
        }));

        assertEquals(1, ms.getCount("a"));
        assertEquals(2, ms.getCount("b"));
        assertEquals(3, ms.getCount("c"));
        assertEquals(6, ms.size());
    }

    @Test
    public void testC343_updateAllOccurrencesOnEmptyNeverCallsFunction() {
        final Multiset<String> ms = new Multiset<>();
        ms.updateAllOccurrences((e, n) -> {
            throw new AssertionError("must not be called");
        });
        assertTrue(ms.isEmpty());
    }

    @Test
    @DisplayName("Test updateAllOccurrences() removing elements")
    public void testUpdateAllOccurrencesRemove() {
        multiset.add("a", 1);
        multiset.add("b", 2);
        multiset.add("c", 3);

        multiset.updateAllOccurrences((element, count) -> count >= 2 ? count : 0);

        assertFalse(multiset.contains("a"));
        assertEquals(2, multiset.getCount("b"));
        assertEquals(3, multiset.getCount("c"));
    }

    @Test
    public void testC343_updateAllOccurrencesRemovingEverything() {
        final Multiset<String> ms = Multiset.of("a", "b", "b");
        ms.updateAllOccurrences((e, n) -> 0);
        assertTrue(ms.isEmpty());
    }
}
