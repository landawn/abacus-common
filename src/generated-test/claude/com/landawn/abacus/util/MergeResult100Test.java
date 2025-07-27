package com.landawn.abacus.util;

import java.util.Comparator;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class MergeResult100Test extends TestBase {

    @Test
    public void testEnumValues() {
        Assertions.assertNotNull(MergeResult.TAKE_FIRST);
        Assertions.assertNotNull(MergeResult.TAKE_SECOND);
    }

    @Test
    public void testMinFirstComparable() {
        MergeResult result = MergeResult.minFirst(5, 10);
        Assertions.assertEquals(MergeResult.TAKE_FIRST, result);

        result = MergeResult.minFirst(10, 5);
        Assertions.assertEquals(MergeResult.TAKE_SECOND, result);

        result = MergeResult.minFirst(5, 5);
        Assertions.assertEquals(MergeResult.TAKE_FIRST, result);
    }

    @Test
    public void testMinFirstWithComparator() {
        Comparator<String> lengthComparator = Comparator.comparing(String::length);

        MergeResult result = MergeResult.minFirst("hi", "hello", lengthComparator);
        Assertions.assertEquals(MergeResult.TAKE_FIRST, result);

        result = MergeResult.minFirst("hello", "hi", lengthComparator);
        Assertions.assertEquals(MergeResult.TAKE_SECOND, result);
    }

    @Test
    public void testMaxFirstComparable() {
        MergeResult result = MergeResult.maxFirst(5, 10);
        Assertions.assertEquals(MergeResult.TAKE_SECOND, result);

        result = MergeResult.maxFirst(10, 5);
        Assertions.assertEquals(MergeResult.TAKE_FIRST, result);

        result = MergeResult.maxFirst(5, 5);
        Assertions.assertEquals(MergeResult.TAKE_FIRST, result);
    }

    @Test
    public void testMaxFirstWithComparator() {
        Comparator<String> lengthComparator = Comparator.comparing(String::length);

        MergeResult result = MergeResult.maxFirst("hello", "hi", lengthComparator);
        Assertions.assertEquals(MergeResult.TAKE_FIRST, result);

        result = MergeResult.maxFirst("hi", "hello", lengthComparator);
        Assertions.assertEquals(MergeResult.TAKE_SECOND, result);
    }

    @Test
    public void testMinFirstFunction() {
        BiFunction<Integer, Integer, MergeResult> minFunc = MergeResult.minFirst();

        MergeResult result = minFunc.apply(5, 10);
        Assertions.assertEquals(MergeResult.TAKE_FIRST, result);

        result = minFunc.apply(10, 5);
        Assertions.assertEquals(MergeResult.TAKE_SECOND, result);
    }

    @Test
    public void testMinFirstFunctionWithComparator() {
        Comparator<String> comparator = String::compareToIgnoreCase;
        BiFunction<String, String, MergeResult> minFunc = MergeResult.minFirst(comparator);

        MergeResult result = minFunc.apply("a", "B");
        Assertions.assertEquals(MergeResult.TAKE_FIRST, result);

        result = minFunc.apply("B", "a");
        Assertions.assertEquals(MergeResult.TAKE_SECOND, result);
    }

    @Test
    public void testMaxFirstFunction() {
        BiFunction<Integer, Integer, MergeResult> maxFunc = MergeResult.maxFirst();

        MergeResult result = maxFunc.apply(5, 10);
        Assertions.assertEquals(MergeResult.TAKE_SECOND, result);

        result = maxFunc.apply(10, 5);
        Assertions.assertEquals(MergeResult.TAKE_FIRST, result);
    }

    @Test
    public void testMaxFirstFunctionWithComparator() {
        Comparator<String> comparator = String::compareToIgnoreCase;
        BiFunction<String, String, MergeResult> maxFunc = MergeResult.maxFirst(comparator);

        MergeResult result = maxFunc.apply("B", "a");
        Assertions.assertEquals(MergeResult.TAKE_FIRST, result);

        result = maxFunc.apply("a", "B");
        Assertions.assertEquals(MergeResult.TAKE_SECOND, result);
    }

    @Test
    public void testAlternate() {
        BiFunction<String, String, MergeResult> alternator = MergeResult.alternate();

        // Test alternating behavior
        MergeResult result1 = alternator.apply("a", "b");
        MergeResult result2 = alternator.apply("c", "d");
        MergeResult result3 = alternator.apply("e", "f");

        // Should alternate between TAKE_FIRST and TAKE_SECOND
        Assertions.assertNotEquals(result1, result2);
        Assertions.assertEquals(result1, result3);
    }
}