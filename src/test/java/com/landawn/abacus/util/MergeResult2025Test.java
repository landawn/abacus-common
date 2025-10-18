package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Comparator;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.function.BiFunction;

@Tag("2025")
public class MergeResult2025Test extends TestBase {

    @Test
    public void testMinFirst_comparable_firstSmaller() {
        MergeResult result = MergeResult.minFirst(5, 10);
        assertEquals(MergeResult.TAKE_FIRST, result);
    }

    @Test
    public void testMinFirst_comparable_secondSmaller() {
        MergeResult result = MergeResult.minFirst(10, 5);
        assertEquals(MergeResult.TAKE_SECOND, result);
    }

    @Test
    public void testMinFirst_comparable_equal() {
        MergeResult result = MergeResult.minFirst(5, 5);
        assertEquals(MergeResult.TAKE_FIRST, result);
    }

    @Test
    public void testMinFirst_comparable_strings() {
        MergeResult result = MergeResult.minFirst("a", "b");
        assertEquals(MergeResult.TAKE_FIRST, result);

        result = MergeResult.minFirst("b", "a");
        assertEquals(MergeResult.TAKE_SECOND, result);
    }

    @Test
    public void testMinFirst_comparator_firstSmaller() {
        Comparator<Integer> cmp = Integer::compare;
        MergeResult result = MergeResult.minFirst(3, 7, cmp);
        assertEquals(MergeResult.TAKE_FIRST, result);
    }

    @Test
    public void testMinFirst_comparator_secondSmaller() {
        Comparator<Integer> cmp = Integer::compare;
        MergeResult result = MergeResult.minFirst(7, 3, cmp);
        assertEquals(MergeResult.TAKE_SECOND, result);
    }

    @Test
    public void testMinFirst_comparator_equal() {
        Comparator<Integer> cmp = Integer::compare;
        MergeResult result = MergeResult.minFirst(5, 5, cmp);
        assertEquals(MergeResult.TAKE_FIRST, result);
    }

    @Test
    public void testMinFirst_comparator_stringLength() {
        Comparator<String> lengthComparator = Comparator.comparing(String::length);
        MergeResult result = MergeResult.minFirst("hi", "hello", lengthComparator);
        assertEquals(MergeResult.TAKE_FIRST, result);

        result = MergeResult.minFirst("hello", "hi", lengthComparator);
        assertEquals(MergeResult.TAKE_SECOND, result);
    }

    @Test
    public void testMaxFirst_comparable_firstLarger() {
        MergeResult result = MergeResult.maxFirst(10, 5);
        assertEquals(MergeResult.TAKE_FIRST, result);
    }

    @Test
    public void testMaxFirst_comparable_secondLarger() {
        MergeResult result = MergeResult.maxFirst(5, 10);
        assertEquals(MergeResult.TAKE_SECOND, result);
    }

    @Test
    public void testMaxFirst_comparable_equal() {
        MergeResult result = MergeResult.maxFirst(5, 5);
        assertEquals(MergeResult.TAKE_FIRST, result);
    }

    @Test
    public void testMaxFirst_comparable_strings() {
        MergeResult result = MergeResult.maxFirst("b", "a");
        assertEquals(MergeResult.TAKE_FIRST, result);

        result = MergeResult.maxFirst("a", "b");
        assertEquals(MergeResult.TAKE_SECOND, result);
    }

    @Test
    public void testMaxFirst_comparator_firstLarger() {
        Comparator<Integer> cmp = Integer::compare;
        MergeResult result = MergeResult.maxFirst(7, 3, cmp);
        assertEquals(MergeResult.TAKE_FIRST, result);
    }

    @Test
    public void testMaxFirst_comparator_secondLarger() {
        Comparator<Integer> cmp = Integer::compare;
        MergeResult result = MergeResult.maxFirst(3, 7, cmp);
        assertEquals(MergeResult.TAKE_SECOND, result);
    }

    @Test
    public void testMaxFirst_comparator_equal() {
        Comparator<Integer> cmp = Integer::compare;
        MergeResult result = MergeResult.maxFirst(5, 5, cmp);
        assertEquals(MergeResult.TAKE_FIRST, result);
    }

    @Test
    public void testMaxFirst_comparator_stringLength() {
        Comparator<String> lengthComparator = Comparator.comparing(String::length);
        MergeResult result = MergeResult.maxFirst("hello", "hi", lengthComparator);
        assertEquals(MergeResult.TAKE_FIRST, result);

        result = MergeResult.maxFirst("hi", "hello", lengthComparator);
        assertEquals(MergeResult.TAKE_SECOND, result);
    }

    @Test
    public void testMinFirst_biFunction() {
        BiFunction<Integer, Integer, MergeResult> minMerger = MergeResult.minFirst();
        assertNotNull(minMerger);

        MergeResult result = minMerger.apply(3, 7);
        assertEquals(MergeResult.TAKE_FIRST, result);

        result = minMerger.apply(7, 3);
        assertEquals(MergeResult.TAKE_SECOND, result);
    }

    @Test
    public void testMinFirst_biFunction_equal() {
        BiFunction<Integer, Integer, MergeResult> minMerger = MergeResult.minFirst();
        MergeResult result = minMerger.apply(5, 5);
        assertEquals(MergeResult.TAKE_FIRST, result);
    }

    @Test
    public void testMinFirst_biFunction_withComparator() {
        Comparator<String> lengthComparator = Comparator.comparing(String::length);
        BiFunction<String, String, MergeResult> minMerger = MergeResult.minFirst(lengthComparator);
        assertNotNull(minMerger);

        MergeResult result = minMerger.apply("hi", "hello");
        assertEquals(MergeResult.TAKE_FIRST, result);

        result = minMerger.apply("hello", "hi");
        assertEquals(MergeResult.TAKE_SECOND, result);
    }

    @Test
    public void testMinFirst_biFunction_withComparator_nullThrows() {
        assertThrows(IllegalArgumentException.class, () -> MergeResult.minFirst(null));
    }

    @Test
    public void testMaxFirst_biFunction() {
        BiFunction<Integer, Integer, MergeResult> maxMerger = MergeResult.maxFirst();
        assertNotNull(maxMerger);

        MergeResult result = maxMerger.apply(7, 3);
        assertEquals(MergeResult.TAKE_FIRST, result);

        result = maxMerger.apply(3, 7);
        assertEquals(MergeResult.TAKE_SECOND, result);
    }

    @Test
    public void testMaxFirst_biFunction_equal() {
        BiFunction<Integer, Integer, MergeResult> maxMerger = MergeResult.maxFirst();
        MergeResult result = maxMerger.apply(5, 5);
        assertEquals(MergeResult.TAKE_FIRST, result);
    }

    @Test
    public void testMaxFirst_biFunction_withComparator() {
        Comparator<String> lengthComparator = Comparator.comparing(String::length);
        BiFunction<String, String, MergeResult> maxMerger = MergeResult.maxFirst(lengthComparator);
        assertNotNull(maxMerger);

        MergeResult result = maxMerger.apply("hello", "hi");
        assertEquals(MergeResult.TAKE_FIRST, result);

        result = maxMerger.apply("hi", "hello");
        assertEquals(MergeResult.TAKE_SECOND, result);
    }

    @Test
    public void testMaxFirst_biFunction_withComparator_nullThrows() {
        assertThrows(IllegalArgumentException.class, () -> MergeResult.maxFirst(null));
    }

    @Test
    public void testAlternate() {
        BiFunction<String, String, MergeResult> alternator = MergeResult.alternate();
        assertNotNull(alternator);

        MergeResult result1 = alternator.apply("a", "b");
        assertEquals(MergeResult.TAKE_FIRST, result1);

        MergeResult result2 = alternator.apply("c", "d");
        assertEquals(MergeResult.TAKE_SECOND, result2);

        MergeResult result3 = alternator.apply("e", "f");
        assertEquals(MergeResult.TAKE_FIRST, result3);
    }

    @Test
    public void testValues() {
        MergeResult[] values = MergeResult.values();
        assertNotNull(values);
        assertEquals(2, values.length);
        assertEquals(MergeResult.TAKE_FIRST, values[0]);
        assertEquals(MergeResult.TAKE_SECOND, values[1]);
    }

    @Test
    public void testValueOf_byName() {
        assertEquals(MergeResult.TAKE_FIRST, MergeResult.valueOf("TAKE_FIRST"));
        assertEquals(MergeResult.TAKE_SECOND, MergeResult.valueOf("TAKE_SECOND"));
    }

    @Test
    public void testIntegration_minFirst_multipleValues() {
        Integer[] values = { 5, 2, 8, 1, 9, 3 };
        Integer min = values[0];
        for (int i = 1; i < values.length; i++) {
            MergeResult result = MergeResult.minFirst(min, values[i]);
            if (result == MergeResult.TAKE_SECOND) {
                min = values[i];
            }
        }
        assertEquals(Integer.valueOf(1), min);
    }

    @Test
    public void testIntegration_maxFirst_multipleValues() {
        Integer[] values = { 5, 2, 8, 1, 9, 3 };
        Integer max = values[0];
        for (int i = 1; i < values.length; i++) {
            MergeResult result = MergeResult.maxFirst(max, values[i]);
            if (result == MergeResult.TAKE_SECOND) {
                max = values[i];
            }
        }
        assertEquals(Integer.valueOf(9), max);
    }

    @Test
    public void testIntegration_customComparator() {
        class Person {
            String name;
            int age;

            Person(String name, int age) {
                this.name = name;
                this.age = age;
            }
        }

        Person p1 = new Person("Alice", 30);
        Person p2 = new Person("Bob", 25);

        Comparator<Person> ageComparator = Comparator.comparing(p -> p.age);

        MergeResult result = MergeResult.minFirst(p1, p2, ageComparator);
        assertEquals(MergeResult.TAKE_SECOND, result);

        result = MergeResult.maxFirst(p1, p2, ageComparator);
        assertEquals(MergeResult.TAKE_FIRST, result);
    }
}
