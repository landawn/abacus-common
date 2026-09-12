package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.OptionalInt;

public class IterablesFindTest extends IterablesTestSupport {
    @Test
    public void testFindFirstOrLastArray() {
        Integer[] arr = { 1, 2, 3, 4, 5 };
        Predicate<Integer> isEven = n -> n % 2 == 0;
        Predicate<Integer> isOdd = n -> n % 2 == 1;

        Nullable<Integer> result = Iterables.findFirstOrLast(arr, isEven, isOdd);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(2), result.get());

        Predicate<Integer> isGreaterThan10 = n -> n > 10;
        result = Iterables.findFirstOrLast(arr, isGreaterThan10, isOdd);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(5), result.get());

        assertTrue(Iterables.findFirstOrLast((Integer[]) null, isEven, isOdd).isEmpty());
    }

    @Test
    public void testFindFirstOrLastCollection() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        Predicate<Integer> isEven = n -> n % 2 == 0;
        Predicate<Integer> isOdd = n -> n % 2 == 1;

        Nullable<Integer> result = Iterables.findFirstOrLast(list, isEven, isOdd);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(2), result.get());

        assertTrue(Iterables.findFirstOrLast((Collection<Integer>) null, isEven, isOdd).isEmpty());
    }

    @Test
    public void testFindFirstOrLast() {
        String[] array = { "a", "bb", "ccc", "dd" };

        Nullable<String> result = Iterables.findFirstOrLast(array, s -> s.length() > 2, s -> s.length() == 2);
        assertTrue(result.isPresent());
        assertEquals("ccc", result.get());

        Nullable<String> fallbackResult = Iterables.findFirstOrLast(array, s -> s.length() > 10, s -> s.length() == 2);
        assertTrue(fallbackResult.isPresent());
        assertEquals("dd", fallbackResult.get());
    }

    // ===================== findFirstOrLast Array/Collection =====================

    @Test
    public void testFindFirstOrLastArray_Dedicated() {
        Nullable<Integer> result = Iterables.findFirstOrLast(new Integer[] { 1, 2, 3, 4, 5 }, i -> i > 10, i -> i < 3);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(2), result.get());
    }

    @Test
    public void testFindFirstOrLastArray_FirstFound() {
        Nullable<Integer> result = Iterables.findFirstOrLast(new Integer[] { 1, 2, 3, 4, 5 }, i -> i > 3, i -> i < 3);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(4), result.get());
    }

    @Test
    public void testFindFirstOrLastArray_NoneFound() {
        Nullable<Integer> result = Iterables.findFirstOrLast(new Integer[] { 1, 2, 3 }, i -> i > 10, i -> i > 10);
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindFirstOrLast_Collection_NoneMatch() {
        List<Integer> list = Arrays.asList(1, 3, 5, 7);
        Predicate<Integer> isEven = x -> x % 2 == 0;
        Predicate<Integer> isGreaterThan10 = x -> x > 10;
        assertTrue(Iterables.findFirstOrLast(list, isEven, isGreaterThan10).isEmpty());
    }

    @Test
    public void testFindFirstOrLastIndex() {
        String[] array = { "a", "bb", "ccc", "dd" };

        OptionalInt result = Iterables.findFirstOrLastIndex(array, s -> s.length() > 2, s -> s.length() == 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.get());
    }

    // ===================== findFirstOrLastIndex Array/Collection =====================

    @Test
    public void testFindFirstOrLastIndexArray_Dedicated() {
        OptionalInt result = Iterables.findFirstOrLastIndex(new Integer[] { 1, 2, 3, 4, 5 }, i -> i > 10, i -> i < 3);
        assertTrue(result.isPresent());
        assertEquals(1, result.get());
    }

    @Test
    public void testFindFirstOrLastIndex_Collection_OnlyLastMatches() {
        List<Integer> list = Arrays.asList(1, 3, 5, 7, 9);
        Predicate<Integer> isEven = x -> x % 2 == 0;
        Predicate<Integer> isGreaterThan4 = x -> x > 4;
        OptionalInt idx = Iterables.findFirstOrLastIndex(list, isEven, isGreaterThan4);
        assertTrue(idx.isPresent());
        assertEquals(4, idx.getAsInt()); // index of 9
    }

    @Test
    public void testFindFirstOrLastIndexArray() {
        Integer[] arr = { 1, 2, 3, 4, 5 };
        Predicate<Integer> isEven = n -> n % 2 == 0;
        Predicate<Integer> isOdd = n -> n % 2 == 1;

        OptionalInt result = Iterables.findFirstOrLastIndex(arr, isEven, isOdd);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        Predicate<Integer> isGreaterThan10 = n -> n > 10;
        result = Iterables.findFirstOrLastIndex(arr, isGreaterThan10, isOdd);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        assertFalse(Iterables.findFirstOrLastIndex((Integer[]) null, isEven, isOdd).isPresent());
    }

    @Test
    public void testFindFirstOrLastIndexCollection() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        Predicate<Integer> isEven = n -> n % 2 == 0;
        Predicate<Integer> isOdd = n -> n % 2 == 1;

        OptionalInt result = Iterables.findFirstOrLastIndex(list, isEven, isOdd);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        assertFalse(Iterables.findFirstOrLastIndex((Collection<Integer>) null, isEven, isOdd).isPresent());
    }

    @Test
    public void testFindFirstOrLastIndexArray_NoneFound() {
        OptionalInt result = Iterables.findFirstOrLastIndex(new Integer[] { 1, 2, 3 }, i -> i > 10, i -> i > 10);
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindFirstAndLastArraySinglePredicate() {
        Integer[] arr = { 1, 2, 3, 4, 5 };
        Predicate<Integer> isEven = n -> n % 2 == 0;

        Pair<Nullable<Integer>, Nullable<Integer>> result = Iterables.findFirstAndLast(arr, isEven);
        assertTrue(result.left().isPresent());
        assertTrue(result.right().isPresent());
        assertEquals(Integer.valueOf(2), result.left().get());
        assertEquals(Integer.valueOf(4), result.right().get());

        Pair<Nullable<Integer>, Nullable<Integer>> emptyResult = Iterables.findFirstAndLast((Integer[]) null, isEven);
        assertFalse(emptyResult.left().isPresent());
        assertFalse(emptyResult.right().isPresent());
    }

    @Test
    public void testFindFirstAndLastArrayTwoPredicates() {
        Integer[] arr = { 1, 2, 3, 4, 5, 6 };
        Predicate<Integer> isEven = n -> n % 2 == 0;
        Predicate<Integer> greaterThan3 = n -> n > 3;

        Pair<Nullable<Integer>, Nullable<Integer>> result = Iterables.findFirstAndLast(arr, isEven, greaterThan3);
        assertTrue(result.left().isPresent());
        assertTrue(result.right().isPresent());
        assertEquals(Integer.valueOf(2), result.left().get());
        assertEquals(Integer.valueOf(6), result.right().get());
    }

    @Test
    public void testFindFirstAndLastCollectionSinglePredicate() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        Predicate<Integer> isEven = n -> n % 2 == 0;

        Pair<Nullable<Integer>, Nullable<Integer>> result = Iterables.findFirstAndLast(list, isEven);
        assertTrue(result.left().isPresent());
        assertTrue(result.right().isPresent());
        assertEquals(Integer.valueOf(2), result.left().get());
        assertEquals(Integer.valueOf(4), result.right().get());

        Pair<Nullable<Integer>, Nullable<Integer>> emptyResult = Iterables.findFirstAndLast((Collection<Integer>) null, isEven);
        assertFalse(emptyResult.left().isPresent());
        assertFalse(emptyResult.right().isPresent());
    }

    @Test
    public void testFindFirstAndLastCollectionTwoPredicates() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6);
        Predicate<Integer> isEven = n -> n % 2 == 0;
        Predicate<Integer> greaterThan3 = n -> n > 3;

        Pair<Nullable<Integer>, Nullable<Integer>> result = Iterables.findFirstAndLast(list, isEven, greaterThan3);
        assertTrue(result.left().isPresent());
        assertTrue(result.right().isPresent());
        assertEquals(Integer.valueOf(2), result.left().get());
        assertEquals(Integer.valueOf(6), result.right().get());
    }

    @Test
    public void testFindFirstAndLastArray() {
        Predicate<Integer> isEven = x -> x != null && x % 2 == 0;
        Pair<Nullable<Integer>, Nullable<Integer>> result;

        result = Iterables.findFirstAndLast((Integer[]) null, isEven);
        assertTrue(result.left().isEmpty() && result.right().isEmpty());

        result = Iterables.findFirstAndLast(new Integer[0], isEven);
        assertTrue(result.left().isEmpty() && result.right().isEmpty());

        result = Iterables.findFirstAndLast(new Integer[] { 1, 2, 3, 4, 5, 6 }, isEven);
        assertEquals(2, result.left().orElse(null));
        assertEquals(6, result.right().orElse(null));

        result = Iterables.findFirstAndLast(new Integer[] { 1, 3, 5 }, isEven);
        assertTrue(result.left().isEmpty() && result.right().isEmpty());
    }

    @Test
    public void testFindFirstAndLastArrayWithTwoPredicates() {
        Predicate<Integer> isSmallEven = x -> x != null && x % 2 == 0 && x < 5;
        Predicate<Integer> isLargeOdd = x -> x != null && x % 2 != 0 && x > 3;
        Pair<Nullable<Integer>, Nullable<Integer>> result;

        result = Iterables.findFirstAndLast(new Integer[] { 1, 2, 3, 4, 5, 6 }, isSmallEven, isLargeOdd);
        assertEquals(2, result.left().orElse(null));
        assertEquals(5, result.right().orElse(null));
    }

    @Test
    public void testFindFirstAndLastCollection() {
        Predicate<Integer> isEven = x -> x != null && x % 2 == 0;
        Pair<Nullable<Integer>, Nullable<Integer>> result;

        result = Iterables.findFirstAndLast((Collection<Integer>) null, isEven);
        assertTrue(result.left().isEmpty() && result.right().isEmpty());

        result = Iterables.findFirstAndLast(Collections.emptyList(), isEven);
        assertTrue(result.left().isEmpty() && result.right().isEmpty());

        result = Iterables.findFirstAndLast(Arrays.asList(1, 2, 3, 4, 5, 6), isEven);
        assertEquals(2, result.left().orElse(null));
        assertEquals(6, result.right().orElse(null));
    }

    @Test
    public void testFindFirstAndLastCollectionWithTwoPredicates() {
        Predicate<Integer> isSmallEven = x -> x != null && x % 2 == 0 && x < 5;
        Predicate<Integer> isLargeOdd = x -> x != null && x % 2 != 0 && x > 3;
        Pair<Nullable<Integer>, Nullable<Integer>> result;

        result = Iterables.findFirstAndLast(Arrays.asList(1, 2, 3, 4, 5, 6), isSmallEven, isLargeOdd);
        assertEquals(2, result.left().orElse(null));
        assertEquals(5, result.right().orElse(null));
    }

    @Test
    public void testFindFirstAndLastArrayDifferentPredicates() {
        Predicate<String> startsA = s -> s.startsWith("a");
        Predicate<String> endsO = s -> s.endsWith("o");
        Pair<Nullable<String>, Nullable<String>> result = Iterables.findFirstAndLast(new String[] { "apple", "avocado", "banana", "mango", "orange" }, startsA,
                endsO);
        assertEquals("apple", result.left().get());
        assertEquals("mango", result.right().get());

        result = Iterables.findFirstAndLast(new String[] { "apple", "avocado" }, s -> s.startsWith("x"), endsO);
        assertTrue(result.left().isEmpty());
        assertEquals("avocado", result.right().get());
    }

    @Test
    public void testFindFirstAndLast() {
        String[] array = { "a", "bb", "ccc", "dd", "eee" };

        Pair<Nullable<String>, Nullable<String>> result = Iterables.findFirstAndLast(array, s -> s.length() >= 2);

        assertTrue(result.left().isPresent());
        assertTrue(result.right().isPresent());
        assertEquals("bb", result.left().get());
        assertEquals("eee", result.right().get());
    }

    @Test
    public void testFindFirstAndLastWithDifferentPredicates() {
        String[] array = { "a", "bb", "ccc", "dd", "eee" };

        Pair<Nullable<String>, Nullable<String>> result = Iterables.findFirstAndLast(array, s -> s.length() == 2, s -> s.length() == 3);

        assertTrue(result.left().isPresent());
        assertTrue(result.right().isPresent());
        assertEquals("bb", result.left().get());
        assertEquals("eee", result.right().get());
    }

    // ===================== findFirstAndLast =====================

    @Test
    public void testFindFirstAndLastArray_SinglePredicate_Dedicated() {
        Pair<Nullable<Integer>, Nullable<Integer>> result = Iterables.findFirstAndLast(new Integer[] { 1, 2, 3, 4, 5 }, i -> i % 2 == 0);
        assertTrue(result.left().isPresent());
        assertEquals(Integer.valueOf(2), result.left().get());
        assertTrue(result.right().isPresent());
        assertEquals(Integer.valueOf(4), result.right().get());
    }

    @Test
    public void testFindFirstAndLastArray_TwoPredicates_Dedicated() {
        Pair<Nullable<Integer>, Nullable<Integer>> result = Iterables.findFirstAndLast(new Integer[] { 1, 2, 3, 4, 5 }, i -> i > 3, i -> i < 3);
        assertTrue(result.left().isPresent());
        assertEquals(Integer.valueOf(4), result.left().get());
        assertTrue(result.right().isPresent());
        assertEquals(Integer.valueOf(2), result.right().get());
    }

    @Test
    public void testFindFirstAndLastIndexArrayTwoPredicates() {
        Integer[] arr = { 1, 2, 3, 4, 5, 6 };
        Predicate<Integer> isEven = n -> n % 2 == 0;
        Predicate<Integer> greaterThan3 = n -> n > 3;

        Pair<OptionalInt, OptionalInt> result = Iterables.findFirstAndLastIndex(arr, isEven, greaterThan3);
        assertTrue(result.left().isPresent());
        assertTrue(result.right().isPresent());
        assertEquals(1, result.left().getAsInt());
        assertEquals(5, result.right().getAsInt());
    }

    @Test
    public void testFindFirstAndLastIndexCollectionTwoPredicates() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6);
        Predicate<Integer> isEven = n -> n % 2 == 0;
        Predicate<Integer> greaterThan3 = n -> n > 3;

        Pair<OptionalInt, OptionalInt> result = Iterables.findFirstAndLastIndex(list, isEven, greaterThan3);
        assertTrue(result.left().isPresent());
        assertTrue(result.right().isPresent());
        assertEquals(1, result.left().getAsInt());
        assertEquals(5, result.right().getAsInt());
    }

    @Test
    public void testFindFirstAndLastIndex() {
        String[] array = { "a", "bb", "ccc", "dd", "eee" };

        Pair<OptionalInt, OptionalInt> result = Iterables.findFirstAndLastIndex(array, s -> s.length() >= 2);

        assertTrue(result.left().isPresent());
        assertTrue(result.right().isPresent());
        assertEquals(1, result.left().get());
        assertEquals(4, result.right().get());
    }

    @Test
    public void testFindFirstAndLastIndexArraySinglePredicate() {
        Integer[] arr = { 1, 2, 3, 4, 5 };
        Predicate<Integer> isEven = n -> n % 2 == 0;

        Pair<OptionalInt, OptionalInt> result = Iterables.findFirstAndLastIndex(arr, isEven);
        assertTrue(result.left().isPresent());
        assertTrue(result.right().isPresent());
        assertEquals(1, result.left().getAsInt());
        assertEquals(3, result.right().getAsInt());

        Pair<OptionalInt, OptionalInt> emptyResult = Iterables.findFirstAndLastIndex((Integer[]) null, isEven);
        assertFalse(emptyResult.left().isPresent());
        assertFalse(emptyResult.right().isPresent());
    }

    @Test
    public void testFindFirstAndLastIndexCollectionSinglePredicate() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        Predicate<Integer> isEven = n -> n % 2 == 0;

        Pair<OptionalInt, OptionalInt> result = Iterables.findFirstAndLastIndex(list, isEven);
        assertTrue(result.left().isPresent());
        assertTrue(result.right().isPresent());
        assertEquals(1, result.left().getAsInt());
        assertEquals(3, result.right().getAsInt());

        Pair<OptionalInt, OptionalInt> emptyResult = Iterables.findFirstAndLastIndex((Collection<Integer>) null, isEven);
        assertFalse(emptyResult.left().isPresent());
        assertFalse(emptyResult.right().isPresent());
    }

    @Test
    public void testFindFirstAndLastIndexArray() {
        Predicate<Integer> isEven = x -> x != null && x % 2 == 0;
        Pair<OptionalInt, OptionalInt> result;

        result = Iterables.findFirstAndLastIndex((Integer[]) null, isEven);
        assertTrue(result.left().isEmpty() && result.right().isEmpty());

        result = Iterables.findFirstAndLastIndex(new Integer[0], isEven);
        assertTrue(result.left().isEmpty() && result.right().isEmpty());

        result = Iterables.findFirstAndLastIndex(new Integer[] { 1, 2, 3, 4, 5, 6 }, isEven);
        assertEquals(1, result.left().orElse(-1));
        assertEquals(5, result.right().orElse(-1));

        result = Iterables.findFirstAndLastIndex(new Integer[] { 1, 3, 5 }, isEven);
        assertTrue(result.left().isEmpty() && result.right().isEmpty());
    }

    @Test
    public void testFindFirstAndLastIndexArrayWithTwoPredicates() {
        Predicate<Integer> isSmallEven = x -> x != null && x % 2 == 0 && x < 5;
        Predicate<Integer> isLargeOdd = x -> x != null && x % 2 != 0 && x > 3;
        Pair<OptionalInt, OptionalInt> result;

        result = Iterables.findFirstAndLastIndex(new Integer[] { 1, 2, 3, 4, 5, 6 }, isSmallEven, isLargeOdd);
        assertEquals(1, result.left().orElse(-1));
        assertEquals(4, result.right().orElse(-1));
    }

    @Test
    public void testFindFirstAndLastIndexCollection() {
        Predicate<Integer> isEven = x -> x != null && x % 2 == 0;
        Pair<OptionalInt, OptionalInt> result;

        result = Iterables.findFirstAndLastIndex((Collection<Integer>) null, isEven);
        assertTrue(result.left().isEmpty() && result.right().isEmpty());

        result = Iterables.findFirstAndLastIndex(Collections.emptyList(), isEven);
        assertTrue(result.left().isEmpty() && result.right().isEmpty());

        result = Iterables.findFirstAndLastIndex(Arrays.asList(1, 2, 3, 4, 5, 6), isEven);
        assertEquals(1, result.left().orElse(-1));
        assertEquals(5, result.right().orElse(-1));
    }

    @Test
    public void testFindFirstAndLastIndexCollectionWithTwoPredicates() {
        Predicate<Integer> isSmallEven = x -> x != null && x % 2 == 0 && x < 5;
        Predicate<Integer> isLargeOdd = x -> x != null && x % 2 != 0 && x > 3;
        Pair<OptionalInt, OptionalInt> result;

        result = Iterables.findFirstAndLastIndex(Arrays.asList(1, 2, 3, 4, 5, 6), isSmallEven, isLargeOdd);
        assertEquals(1, result.left().orElse(-1));
        assertEquals(4, result.right().orElse(-1));
    }

    // ===================== findFirstAndLastIndex =====================

    @Test
    public void testFindFirstAndLastIndexArray_SinglePredicate_Dedicated() {
        Pair<OptionalInt, OptionalInt> result = Iterables.findFirstAndLastIndex(new Integer[] { 1, 2, 3, 4, 5 }, i -> i % 2 == 0);
        assertTrue(result.left().isPresent());
        assertEquals(1, result.left().get());
        assertTrue(result.right().isPresent());
        assertEquals(3, result.right().get());
    }

    @Test
    public void testFindFirstAndLastIndexCollection_SinglePredicate_Dedicated() {
        Pair<OptionalInt, OptionalInt> result = Iterables.findFirstAndLastIndex(Arrays.asList(1, 2, 3, 4, 5), i -> i % 2 == 0);
        assertTrue(result.left().isPresent());
        assertEquals(1, result.left().get());
        assertTrue(result.right().isPresent());
        assertEquals(3, result.right().get());
    }
}
