package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Unit tests for public methods in Iterators class after line 2390
 */
public class Iterators102Test extends TestBase {

    private List<Integer> testList;
    private List<Integer> emptyList;

    @BeforeEach
    public void setUp() {
        testList = Arrays.asList(1, 2, 3, 4, 5);
        emptyList = new ArrayList<>();
    }

    // Tests for advance method
    @Test
    public void testAdvance() {
        Iterator<Integer> iter = testList.iterator();
        long advanced = Iterators.advance(iter, 3);

        assertEquals(3, advanced);
        assertTrue(iter.hasNext());
        assertEquals(4, iter.next());
    }

    @Test
    public void testAdvanceMoreThanAvailable() {
        Iterator<Integer> iter = testList.iterator();
        long advanced = Iterators.advance(iter, 10);

        assertEquals(5, advanced);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testAdvanceZero() {
        Iterator<Integer> iter = testList.iterator();
        long advanced = Iterators.advance(iter, 0);

        assertEquals(0, advanced);
        assertTrue(iter.hasNext());
        assertEquals(1, iter.next());
    }

    @Test
    public void testAdvanceNegativeThrowsException() {
        Iterator<Integer> iter = testList.iterator();
        assertThrows(IllegalArgumentException.class, () -> Iterators.advance(iter, -1));
    }

    @Test
    public void testAdvanceEmptyIterator() {
        Iterator<Integer> iter = emptyList.iterator();
        long advanced = Iterators.advance(iter, 5);

        assertEquals(0, advanced);
        assertFalse(iter.hasNext());
    }

    // Tests for skip method
    @Test
    public void testSkip() {
        Iterator<Integer> iter = testList.iterator();
        ObjIterator<Integer> skipped = Iterators.skip(iter, 2);

        List<Integer> result = new ArrayList<>();
        while (skipped.hasNext()) {
            result.add(skipped.next());
        }

        assertEquals(Arrays.asList(3, 4, 5), result);
    }

    @Test
    public void testSkipZero() {
        Iterator<Integer> iter = testList.iterator();
        ObjIterator<Integer> skipped = Iterators.skip(iter, 0);

        List<Integer> result = new ArrayList<>();
        while (skipped.hasNext()) {
            result.add(skipped.next());
        }

        assertEquals(testList, result);
    }

    @Test
    public void testSkipMoreThanSize() {
        Iterator<Integer> iter = testList.iterator();
        ObjIterator<Integer> skipped = Iterators.skip(iter, 10);

        assertFalse(skipped.hasNext());
    }

    @Test
    public void testSkipNullIterator() {
        ObjIterator<Integer> skipped = Iterators.skip(null, 2);
        assertFalse(skipped.hasNext());
    }

    @Test
    public void testSkipNegativeThrowsException() {
        Iterator<Integer> iter = testList.iterator();
        assertThrows(IllegalArgumentException.class, () -> Iterators.skip(iter, -1));
    }

    // Tests for limit method
    @Test
    public void testLimit() {
        Iterator<Integer> iter = testList.iterator();
        ObjIterator<Integer> limited = Iterators.limit(iter, 3);

        List<Integer> result = new ArrayList<>();
        while (limited.hasNext()) {
            result.add(limited.next());
        }

        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testLimitZero() {
        Iterator<Integer> iter = testList.iterator();
        ObjIterator<Integer> limited = Iterators.limit(iter, 0);

        assertFalse(limited.hasNext());
    }

    @Test
    public void testLimitMoreThanSize() {
        Iterator<Integer> iter = testList.iterator();
        ObjIterator<Integer> limited = Iterators.limit(iter, 10);

        List<Integer> result = new ArrayList<>();
        while (limited.hasNext()) {
            result.add(limited.next());
        }

        assertEquals(testList, result);
    }

    @Test
    public void testLimitNullIterator() {
        ObjIterator<Integer> limited = Iterators.limit(null, 3);
        assertFalse(limited.hasNext());
    }

    @Test
    public void testLimitNegativeThrowsException() {
        Iterator<Integer> iter = testList.iterator();
        assertThrows(IllegalArgumentException.class, () -> Iterators.limit(iter, -1));
    }

    // Tests for skipAndLimit method
    @Test
    public void testSkipAndLimit() {
        Iterator<Integer> iter = testList.iterator();
        ObjIterator<Integer> result = Iterators.skipAndLimit(iter, 1, 3);

        List<Integer> resultList = new ArrayList<>();
        while (result.hasNext()) {
            resultList.add(result.next());
        }

        assertEquals(Arrays.asList(2, 3, 4), resultList);
    }

    @Test
    public void testSkipAndLimitZeroOffset() {
        Iterator<Integer> iter = testList.iterator();
        ObjIterator<Integer> result = Iterators.skipAndLimit(iter, 0, 3);

        List<Integer> resultList = new ArrayList<>();
        while (result.hasNext()) {
            resultList.add(result.next());
        }

        assertEquals(Arrays.asList(1, 2, 3), resultList);
    }

    @Test
    public void testSkipAndLimitMaxCount() {
        Iterator<Integer> iter = testList.iterator();
        ObjIterator<Integer> result = Iterators.skipAndLimit(iter, 2, Long.MAX_VALUE);

        List<Integer> resultList = new ArrayList<>();
        while (result.hasNext()) {
            resultList.add(result.next());
        }

        assertEquals(Arrays.asList(3, 4, 5), resultList);
    }

    @Test
    public void testSkipAndLimitIterable() {
        ObjIterator<Integer> result = Iterators.skipAndLimit(testList, 1, 3);

        List<Integer> resultList = new ArrayList<>();
        while (result.hasNext()) {
            resultList.add(result.next());
        }

        assertEquals(Arrays.asList(2, 3, 4), resultList);
    }

    // Tests for skipNulls method
    @Test
    public void testSkipNullsIterable() {
        List<String> listWithNulls = Arrays.asList("a", null, "b", null, "c");
        ObjIterator<String> result = Iterators.skipNulls(listWithNulls);

        List<String> resultList = new ArrayList<>();
        while (result.hasNext()) {
            resultList.add(result.next());
        }

        assertEquals(Arrays.asList("a", "b", "c"), resultList);
    }

    @Test
    public void testSkipNullsIterator() {
        List<String> listWithNulls = Arrays.asList("a", null, "b", null, "c");
        ObjIterator<String> result = Iterators.skipNulls(listWithNulls.iterator());

        List<String> resultList = new ArrayList<>();
        while (result.hasNext()) {
            resultList.add(result.next());
        }

        assertEquals(Arrays.asList("a", "b", "c"), resultList);
    }

    @Test
    public void testSkipNullsAllNulls() {
        List<String> allNulls = Arrays.asList(null, null, null);
        ObjIterator<String> result = Iterators.skipNulls(allNulls);

        assertFalse(result.hasNext());
    }

    // Tests for distinct method
    @Test
    public void testDistinctIterable() {
        List<Integer> listWithDuplicates = Arrays.asList(1, 2, 2, 3, 3, 3, 4);
        ObjIterator<Integer> result = Iterators.distinct(listWithDuplicates);

        List<Integer> resultList = new ArrayList<>();
        while (result.hasNext()) {
            resultList.add(result.next());
        }

        assertEquals(Arrays.asList(1, 2, 3, 4), resultList);
    }

    @Test
    public void testDistinctIterator() {
        List<Integer> listWithDuplicates = Arrays.asList(1, 2, 2, 3, 3, 3, 4);
        ObjIterator<Integer> result = Iterators.distinct(listWithDuplicates.iterator());

        List<Integer> resultList = new ArrayList<>();
        while (result.hasNext()) {
            resultList.add(result.next());
        }

        assertEquals(Arrays.asList(1, 2, 3, 4), resultList);
    }

    @Test
    public void testDistinctNullIterator() {
        ObjIterator<Integer> result = Iterators.distinct((Iterator<Integer>) null);
        assertFalse(result.hasNext());
    }

    @Test
    public void testDistinctNullIterable() {
        ObjIterator<Integer> result = Iterators.distinct((Iterable<Integer>) null);
        assertFalse(result.hasNext());
    }

    // Tests for distinctBy method
    @Test
    public void testDistinctByIterable() {
        List<String> strings = Arrays.asList("a", "bb", "c", "dd", "eee");
        Function<String, Integer> keyExtractor = String::length;
        ObjIterator<String> result = Iterators.distinctBy(strings, keyExtractor);

        List<String> resultList = new ArrayList<>();
        while (result.hasNext()) {
            resultList.add(result.next());
        }

        assertEquals(Arrays.asList("a", "bb", "eee"), resultList);
    }

    @Test
    public void testDistinctByIterator() {
        List<String> strings = Arrays.asList("a", "bb", "c", "dd", "eee");
        Function<String, Integer> keyExtractor = String::length;
        ObjIterator<String> result = Iterators.distinctBy(strings.iterator(), keyExtractor);

        List<String> resultList = new ArrayList<>();
        while (result.hasNext()) {
            resultList.add(result.next());
        }

        assertEquals(Arrays.asList("a", "bb", "eee"), resultList);
    }

    @Test
    public void testDistinctByNullKeyExtractor() {
        assertThrows(IllegalArgumentException.class, () -> Iterators.distinctBy(testList, null));
    }

    // Tests for filter method
    @Test
    public void testFilterIterable() {
        Predicate<Integer> isEven = x -> x % 2 == 0;
        ObjIterator<Integer> result = Iterators.filter(testList, isEven);

        List<Integer> resultList = new ArrayList<>();
        while (result.hasNext()) {
            resultList.add(result.next());
        }

        assertEquals(Arrays.asList(2, 4), resultList);
    }

    @Test
    public void testFilterIterator() {
        Predicate<Integer> isOdd = x -> x % 2 == 1;
        ObjIterator<Integer> result = Iterators.filter(testList.iterator(), isOdd);

        List<Integer> resultList = new ArrayList<>();
        while (result.hasNext()) {
            resultList.add(result.next());
        }

        assertEquals(Arrays.asList(1, 3, 5), resultList);
    }

    @Test
    public void testFilterNullIterable() {
        Predicate<Integer> predicate = x -> true;
        ObjIterator<Integer> result = Iterators.filter((Iterable<Integer>) null, predicate);
        assertFalse(result.hasNext());
    }

    @Test
    public void testFilterNullIterator() {
        Predicate<Integer> predicate = x -> true;
        ObjIterator<Integer> result = Iterators.filter((Iterator<Integer>) null, predicate);
        assertFalse(result.hasNext());
    }

    // Tests for takeWhile method
    @Test
    public void testTakeWhileIterable() {
        Predicate<Integer> lessThanFour = x -> x < 4;
        ObjIterator<Integer> result = Iterators.takeWhile(testList, lessThanFour);

        List<Integer> resultList = new ArrayList<>();
        while (result.hasNext()) {
            resultList.add(result.next());
        }

        assertEquals(Arrays.asList(1, 2, 3), resultList);
    }

    @Test
    public void testTakeWhileIterator() {
        Predicate<Integer> lessThanThree = x -> x < 3;
        ObjIterator<Integer> result = Iterators.takeWhile(testList.iterator(), lessThanThree);

        List<Integer> resultList = new ArrayList<>();
        while (result.hasNext()) {
            resultList.add(result.next());
        }

        assertEquals(Arrays.asList(1, 2), resultList);
    }

    @Test
    public void testTakeWhileNone() {
        Predicate<Integer> greaterThanTen = x -> x > 10;
        ObjIterator<Integer> result = Iterators.takeWhile(testList, greaterThanTen);

        assertFalse(result.hasNext());
    }

    // Tests for takeWhileInclusive method
    @Test
    public void testTakeWhileInclusiveIterable() {
        Predicate<Integer> lessThanThree = x -> x < 3;
        ObjIterator<Integer> result = Iterators.takeWhileInclusive(testList, lessThanThree);

        List<Integer> resultList = new ArrayList<>();
        while (result.hasNext()) {
            resultList.add(result.next());
        }

        assertEquals(Arrays.asList(1, 2, 3), resultList); // includes the first element that fails predicate
    }

    @Test
    public void testTakeWhileInclusiveIterator() {
        Predicate<Integer> lessThanTwo = x -> x < 2;
        ObjIterator<Integer> result = Iterators.takeWhileInclusive(testList.iterator(), lessThanTwo);

        List<Integer> resultList = new ArrayList<>();
        while (result.hasNext()) {
            resultList.add(result.next());
        }

        assertEquals(Arrays.asList(1, 2), resultList);
    }

    // Tests for dropWhile method
    @Test
    public void testDropWhileIterable() {
        Predicate<Integer> lessThanThree = x -> x < 3;
        ObjIterator<Integer> result = Iterators.dropWhile(testList, lessThanThree);

        List<Integer> resultList = new ArrayList<>();
        while (result.hasNext()) {
            resultList.add(result.next());
        }

        assertEquals(Arrays.asList(3, 4, 5), resultList);
    }

    @Test
    public void testDropWhileIterator() {
        Predicate<Integer> lessThanFour = x -> x < 4;
        ObjIterator<Integer> result = Iterators.dropWhile(testList.iterator(), lessThanFour);

        List<Integer> resultList = new ArrayList<>();
        while (result.hasNext()) {
            resultList.add(result.next());
        }

        assertEquals(Arrays.asList(4, 5), resultList);
    }

    @Test
    public void testDropWhileAll() {
        Predicate<Integer> alwaysTrue = x -> true;
        ObjIterator<Integer> result = Iterators.dropWhile(testList, alwaysTrue);

        assertFalse(result.hasNext());
    }

    // Tests for skipUntil method
    @Test
    public void testSkipUntilIterable() {
        Predicate<Integer> equalsThree = x -> x == 3;
        ObjIterator<Integer> result = Iterators.skipUntil(testList, equalsThree);

        List<Integer> resultList = new ArrayList<>();
        while (result.hasNext()) {
            resultList.add(result.next());
        }

        assertEquals(Arrays.asList(3, 4, 5), resultList);
    }

    @Test
    public void testSkipUntilIterator() {
        Predicate<Integer> equalsFour = x -> x == 4;
        ObjIterator<Integer> result = Iterators.skipUntil(testList.iterator(), equalsFour);

        List<Integer> resultList = new ArrayList<>();
        while (result.hasNext()) {
            resultList.add(result.next());
        }

        assertEquals(Arrays.asList(4, 5), resultList);
    }

    @Test
    public void testSkipUntilNeverTrue() {
        Predicate<Integer> greaterThanTen = x -> x > 10;
        ObjIterator<Integer> result = Iterators.skipUntil(testList, greaterThanTen);

        assertFalse(result.hasNext());
    }

    // Tests for map method
    @Test
    public void testMapIterable() {
        Function<Integer, String> toString = Object::toString;
        ObjIterator<String> result = Iterators.map(testList, toString);

        List<String> resultList = new ArrayList<>();
        while (result.hasNext()) {
            resultList.add(result.next());
        }

        assertEquals(Arrays.asList("1", "2", "3", "4", "5"), resultList);
    }

    @Test
    public void testMapIterator() {
        Function<Integer, Integer> square = x -> x * x;
        ObjIterator<Integer> result = Iterators.map(testList.iterator(), square);

        List<Integer> resultList = new ArrayList<>();
        while (result.hasNext()) {
            resultList.add(result.next());
        }

        assertEquals(Arrays.asList(1, 4, 9, 16, 25), resultList);
    }

    @Test
    public void testMapNullIterable() {
        Function<Integer, String> mapper = Object::toString;
        ObjIterator<String> result = Iterators.map((Iterable<Integer>) null, mapper);
        assertFalse(result.hasNext());
    }

    @Test
    public void testMapNullIterator() {
        Function<Integer, String> mapper = Object::toString;
        ObjIterator<String> result = Iterators.map((Iterator<Integer>) null, mapper);
        assertFalse(result.hasNext());
    }

    // Tests for flatMap method
    @Test
    public void testFlatMapIterable() {
        List<List<Integer>> nestedList = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5));
        Function<List<Integer>, List<Integer>> identity = x -> x;
        ObjIterator<Integer> result = Iterators.flatMap(nestedList, identity);

        List<Integer> resultList = new ArrayList<>();
        while (result.hasNext()) {
            resultList.add(result.next());
        }

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), resultList);
    }

    @Test
    public void testFlatMapIterator() {
        List<String> words = Arrays.asList("ab", "cd");
        Function<String, List<Character>> charList = s -> {
            List<Character> chars = new ArrayList<>();
            for (char c : s.toCharArray()) {
                chars.add(c);
            }
            return chars;
        };
        ObjIterator<Character> result = Iterators.flatMap(words.iterator(), charList);

        List<Character> resultList = new ArrayList<>();
        while (result.hasNext()) {
            resultList.add(result.next());
        }

        assertEquals(Arrays.asList('a', 'b', 'c', 'd'), resultList);
    }

    // Tests for flatmap method (array version)
    @Test
    public void testFlatmapIterable() {
        List<String> words = Arrays.asList("ab", "cd");
        Function<String, Character[]> charArray = s -> {
            Character[] chars = new Character[s.length()];
            for (int i = 0; i < s.length(); i++) {
                chars[i] = s.charAt(i);
            }
            return chars;
        };
        ObjIterator<Character> result = Iterators.flatmap(words, charArray);

        List<Character> resultList = new ArrayList<>();
        while (result.hasNext()) {
            resultList.add(result.next());
        }

        assertEquals(Arrays.asList('a', 'b', 'c', 'd'), resultList);
    }

    @Test
    public void testFlatmapIterator() {
        List<Integer> numbers = Arrays.asList(2, 3);
        Function<Integer, Integer[]> duplicate = n -> new Integer[] { n, n };
        ObjIterator<Integer> result = Iterators.flatmap(numbers.iterator(), duplicate);

        List<Integer> resultList = new ArrayList<>();
        while (result.hasNext()) {
            resultList.add(result.next());
        }

        assertEquals(Arrays.asList(2, 2, 3, 3), resultList);
    }

    // Tests for forEach methods
    @Test
    public void testForEachBasic() {
        List<Integer> processed = new ArrayList<>();
        Throwables.Consumer<Integer, RuntimeException> consumer = processed::add;

        Iterators.forEach(testList.iterator(), consumer);

        assertEquals(testList, processed);
    }

    @Test
    public void testForEachWithOnComplete() {
        List<Integer> processed = new ArrayList<>();
        AtomicInteger completeCalled = new AtomicInteger(0);

        Throwables.Consumer<Integer, RuntimeException> consumer = processed::add;
        Throwables.Runnable<RuntimeException> onComplete = () -> completeCalled.incrementAndGet();

        Iterators.forEach(testList.iterator(), consumer, onComplete);

        assertEquals(testList, processed);
        assertEquals(1, completeCalled.get());
    }

    @Test
    public void testForEachWithOffsetAndCount() {
        List<Integer> processed = new ArrayList<>();
        Throwables.Consumer<Integer, RuntimeException> consumer = processed::add;

        Iterators.forEach(testList.iterator(), 1, 3, consumer);

        assertEquals(Arrays.asList(2, 3, 4), processed);
    }

    @Test
    public void testForEachWithThreads() {
        List<Integer> processed = Collections.synchronizedList(new ArrayList<>());
        Throwables.Consumer<Integer, RuntimeException> consumer = processed::add;

        Iterators.forEach(testList.iterator(), 0, Long.MAX_VALUE, 2, 4, consumer);

        // Sort the result since order is not guaranteed with multiple threads
        processed.sort(Integer::compareTo);
        assertEquals(testList, processed);
    }

    @Test
    public void testForEachCollectionOfIterators() {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2).iterator(), Arrays.asList(3, 4).iterator(), Arrays.asList(5).iterator());

        List<Integer> processed = new ArrayList<>();
        Throwables.Consumer<Integer, RuntimeException> consumer = processed::add;

        Iterators.forEach(iterators, consumer);

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), processed);
    }

    @Test
    public void testForEachCollectionWithThreads() {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2).iterator(), Arrays.asList(3, 4).iterator(), Arrays.asList(5).iterator());

        List<Integer> processed = Collections.synchronizedList(new ArrayList<>());
        Throwables.Consumer<Integer, RuntimeException> consumer = processed::add;

        Iterators.forEach(iterators, 2, 2, 4, consumer);

        // Sort the result since order is not guaranteed with multiple threads
        processed.sort(Integer::compareTo);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), processed);
    }

    @Test
    public void testForEachEmptyCollection() {
        List<Iterator<Integer>> emptyIterators = new ArrayList<>();
        List<Integer> processed = new ArrayList<>();
        Throwables.Consumer<Integer, RuntimeException> consumer = processed::add;

        Iterators.forEach(emptyIterators, consumer);

        assertTrue(processed.isEmpty());
    }

    // Additional edge case tests
    @Test
    public void testLazyEvaluationSkip() {
        AtomicInteger callCount = new AtomicInteger(0);
        Iterator<Integer> countingIterator = new Iterator<>() {
            private int current = 1;

            @Override
            public boolean hasNext() {
                return current <= 5;
            }

            @Override
            public Integer next() {
                callCount.incrementAndGet();
                return current++;
            }
        };

        ObjIterator<Integer> skipped = Iterators.skip(countingIterator, 2);
        assertEquals(0, callCount.get()); // No elements consumed yet

        skipped.next(); // Should skip 2 and return 3
        assertEquals(3, callCount.get()); // Should have consumed 3 elements (1, 2, 3)
    }

    @Test
    public void testFilterLazyEvaluation() {
        AtomicInteger callCount = new AtomicInteger(0);
        Iterator<Integer> countingIterator = new Iterator<>() {
            private int current = 1;

            @Override
            public boolean hasNext() {
                return current <= 5;
            }

            @Override
            public Integer next() {
                callCount.incrementAndGet();
                return current++;
            }
        };

        ObjIterator<Integer> filtered = Iterators.filter(countingIterator, x -> x % 2 == 0);
        assertEquals(0, callCount.get()); // No elements consumed yet

        filtered.next(); // Should return 2
        assertTrue(callCount.get() >= 2); // Should have consumed at least elements 1 and 2
    }
}
