package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.function.TriFunction;

public class Iterators100Test extends TestBase {

    private List<Integer> intList;
    private List<String> stringList;
    private Iterator<Integer> intIterator;
    private Iterator<String> stringIterator;

    @BeforeEach
    public void setUp() {
        intList = Arrays.asList(1, 2, 3, 4, 5);
        stringList = Arrays.asList("apple", "banana", "cherry", "date");
        intIterator = intList.iterator();
        stringIterator = stringList.iterator();
    }

    // Tests for get method
    @Test
    public void testGet() {
        Iterator<String> iter = Arrays.asList("a", "b", "c", "d").iterator();

        Nullable<String> result = Iterators.get(iter, 2);
        assertTrue(result.isPresent());
        assertEquals("c", result.get());

        // Test index out of bounds
        result = Iterators.get(iter, 10);
        assertFalse(result.isPresent());

        // Test null iterator
        result = Iterators.get(null, 0);
        assertFalse(result.isPresent());

        // Test negative index
        assertThrows(IllegalArgumentException.class, () -> Iterators.get(intList.iterator(), -1));
    }

    // Tests for occurrencesOf method
    @Test
    public void testOccurrencesOf() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 2, 4, 2, 5);
        assertEquals(3, Iterators.occurrencesOf(numbers.iterator(), 2));
        assertEquals(1, Iterators.occurrencesOf(numbers.iterator(), 5));
        assertEquals(0, Iterators.occurrencesOf(numbers.iterator(), 6));

        // Test with nulls
        List<String> withNulls = Arrays.asList("a", null, "b", null, "c");
        assertEquals(2, Iterators.occurrencesOf(withNulls.iterator(), null));

        // Test null iterator
        assertEquals(0, Iterators.occurrencesOf(null, "test"));
    }

    // Tests for count methods
    @Test
    public void testCount() {
        assertEquals(5, Iterators.count(intList.iterator()));
        assertEquals(0, Iterators.count(null));
        assertEquals(0, Iterators.count(new ArrayList<>().iterator()));
    }

    @Test
    public void testCountWithPredicate() {
        Predicate<Integer> isEven = n -> n % 2 == 0;
        assertEquals(2, Iterators.count(intList.iterator(), isEven));

        assertEquals(0, Iterators.count(null, isEven));

        assertThrows(IllegalArgumentException.class, () -> Iterators.count(intList.iterator(), null));
    }

    // Tests for indexOf methods
    @Test
    public void testIndexOf() {
        List<String> list = Arrays.asList("a", "b", "c", "b", "d");
        assertEquals(1, Iterators.indexOf(list.iterator(), "b"));
        assertEquals(-1, Iterators.indexOf(list.iterator(), "z"));
        assertEquals(-1, Iterators.indexOf(null, "a"));
    }

    @Test
    public void testIndexOfWithFromIndex() {
        List<String> list = Arrays.asList("a", "b", "c", "b", "d");
        assertEquals(3, Iterators.indexOf(list.iterator(), "b", 2));
        assertEquals(-1, Iterators.indexOf(list.iterator(), "b", 4));
    }

    // Tests for elementsEqual
    @Test
    public void testElementsEqual() {
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        List<Integer> list2 = Arrays.asList(1, 2, 3);
        List<Integer> list3 = Arrays.asList(1, 2, 4);

        assertTrue(Iterators.elementsEqual(list1.iterator(), list2.iterator()));
        assertFalse(Iterators.elementsEqual(list1.iterator(), list3.iterator()));

        // Different lengths
        assertFalse(Iterators.elementsEqual(Arrays.asList(1, 2).iterator(), Arrays.asList(1, 2, 3).iterator()));
    }

    // Tests for repeat methods
    @Test
    public void testRepeatElement() {
        ObjIterator<String> iter = Iterators.repeat("test", 3);
        assertEquals("test", iter.next());
        assertEquals("test", iter.next());
        assertEquals("test", iter.next());
        assertFalse(iter.hasNext());

        // Test with 0
        iter = Iterators.repeat("test", 0);
        assertFalse(iter.hasNext());

        // Test negative
        assertThrows(IllegalArgumentException.class, () -> Iterators.repeat("test", -1));
    }

    @Test
    public void testRepeatElementLong() {
        ObjIterator<String> iter = Iterators.repeat("test", 2L);
        int count = 0;
        while (iter.hasNext()) {
            assertEquals("test", iter.next());
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    public void testRepeatElements() {
        List<String> list = Arrays.asList("a", "b");
        ObjIterator<String> iter = Iterators.repeatElements(list, 2L);

        assertEquals("a", iter.next());
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("b", iter.next());
        assertFalse(iter.hasNext());

        // Test with empty collection
        iter = Iterators.repeatElements(new ArrayList<>(), 5L);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testRepeatCollection() {
        List<String> list = Arrays.asList("a", "b");
        ObjIterator<String> iter = Iterators.repeatCollection(list, 2L);

        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testRepeatElementsToSize() {
        List<String> list = Arrays.asList("a", "b");
        ObjIterator<String> iter = Iterators.repeatElementsToSize(list, 5L);

        assertEquals("a", iter.next());
        assertEquals("a", iter.next());
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("b", iter.next());
        assertFalse(iter.hasNext());

        // Test invalid cases
        assertThrows(IllegalArgumentException.class, () -> Iterators.repeatElementsToSize(list, -1L));
        assertThrows(IllegalArgumentException.class, () -> Iterators.repeatElementsToSize(new ArrayList<>(), 5L));
    }

    @Test
    public void testRepeatCollectionToSize() {
        List<String> list = Arrays.asList("a", "b", "c");
        ObjIterator<String> iter = Iterators.repeatCollectionToSize(list, 7L);

        List<String> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        assertEquals(Arrays.asList("a", "b", "c", "a", "b", "c", "a"), result);
    }

    // Tests for cycle methods
    @Test
    public void testCycleArray() {
        ObjIterator<String> iter = Iterators.cycle("a", "b", "c");

        // Test cycling behavior
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());

        // Always has next
        assertTrue(iter.hasNext());

        // Test empty array
        iter = Iterators.cycle();
        assertFalse(iter.hasNext());
    }

    @Test
    public void testCycleIterable() {
        List<String> list = Arrays.asList("x", "y");
        ObjIterator<String> iter = Iterators.cycle(list);

        assertEquals("x", iter.next());
        assertEquals("y", iter.next());
        assertEquals("x", iter.next());
        assertEquals("y", iter.next());

        // Test empty iterable
        iter = Iterators.cycle(new ArrayList<>());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testCycleWithRounds() {
        List<String> list = Arrays.asList("a", "b");
        ObjIterator<String> iter = Iterators.cycle(list, 3L);

        List<String> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        assertEquals(Arrays.asList("a", "b", "a", "b", "a", "b"), result);

        // Test with 0 rounds
        iter = Iterators.cycle(list, 0L);
        assertFalse(iter.hasNext());

        // Test negative rounds
        assertThrows(IllegalArgumentException.class, () -> Iterators.cycle(list, -1L));
    }

    // Tests for concat methods
    @Test
    public void testConcatBooleanArrays() {
        boolean[] arr1 = { true, false };
        boolean[] arr2 = { false, true, true };
        BooleanIterator iter = Iterators.concat(arr1, arr2);

        assertTrue(iter.nextBoolean());
        assertFalse(iter.nextBoolean());
        assertFalse(iter.nextBoolean());
        assertTrue(iter.nextBoolean());
        assertTrue(iter.nextBoolean());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatIntArrays() {
        int[] arr1 = { 1, 2 };
        int[] arr2 = { 3, 4, 5 };
        IntIterator iter = Iterators.concat(arr1, arr2);

        assertEquals(1, iter.nextInt());
        assertEquals(2, iter.nextInt());
        assertEquals(3, iter.nextInt());
        assertEquals(4, iter.nextInt());
        assertEquals(5, iter.nextInt());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatIterators() {
        Iterator<String> iter1 = Arrays.asList("a", "b").iterator();
        Iterator<String> iter2 = Arrays.asList("c", "d").iterator();
        ObjIterator<String> result = Iterators.concat(iter1, iter2);

        assertEquals("a", result.next());
        assertEquals("b", result.next());
        assertEquals("c", result.next());
        assertEquals("d", result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatIterables() {
        List<String> list1 = Arrays.asList("a", "b");
        List<String> list2 = Arrays.asList("c", "d");
        ObjIterator<String> result = Iterators.concat(list1, list2);

        List<String> collected = new ArrayList<>();
        while (result.hasNext()) {
            collected.add(result.next());
        }
        assertEquals(Arrays.asList("a", "b", "c", "d"), collected);
    }

    @Test
    public void testConcatMaps() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("c", 3);

        ObjIterator<Map.Entry<String, Integer>> result = Iterators.concat(map1, map2);

        Set<String> keys = new HashSet<>();
        while (result.hasNext()) {
            keys.add(result.next().getKey());
        }
        assertEquals(new HashSet<>(Arrays.asList("a", "b", "c")), keys);
    }

    @Test
    public void testConcatIteratorCollection() {
        List<Iterator<String>> iterators = Arrays.asList(Arrays.asList("a", "b").iterator(), Arrays.asList("c").iterator(), Arrays.asList("d", "e").iterator());

        ObjIterator<String> result = Iterators.concat(iterators);

        List<String> collected = new ArrayList<>();
        while (result.hasNext()) {
            collected.add(result.next());
        }
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), collected);
    }

    @Test
    public void testConcatBiIterators() {
        BiIterator<String, Integer> bi1 = BiIterator.of(Map.of("a", 1));
        BiIterator<String, Integer> bi2 = BiIterator.of(Map.of("b", 2));

        BiIterator<String, Integer> result = Iterators.concat(bi1, bi2);

        Pair<String, Integer> pair1 = result.next();
        assertEquals("a", pair1.left());
        assertEquals(Integer.valueOf(1), pair1.right());

        Pair<String, Integer> pair2 = result.next();
        assertEquals("b", pair2.left());
        assertEquals(Integer.valueOf(2), pair2.right());

        assertFalse(result.hasNext());
    }

    // Tests for merge methods
    @Test
    public void testMerge() {
        Iterator<Integer> iter1 = Arrays.asList(1, 3, 5).iterator();
        Iterator<Integer> iter2 = Arrays.asList(2, 4, 6).iterator();

        BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        ObjIterator<Integer> result = Iterators.merge(iter1, iter2, selector);

        List<Integer> collected = new ArrayList<>();
        while (result.hasNext()) {
            collected.add(result.next());
        }
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), collected);
    }

    @Test
    public void testMergeCollection() {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 4, 7).iterator(), Arrays.asList(2, 5, 8).iterator(),
                Arrays.asList(3, 6, 9).iterator());

        BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        ObjIterator<Integer> result = Iterators.merge(iterators, selector);

        List<Integer> collected = new ArrayList<>();
        while (result.hasNext()) {
            collected.add(result.next());
        }
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), collected);
    }

    @Test
    public void testMergeSorted() {
        Iterator<Integer> iter1 = Arrays.asList(1, 3, 5, 7).iterator();
        Iterator<Integer> iter2 = Arrays.asList(2, 4, 6, 8).iterator();

        ObjIterator<Integer> result = Iterators.mergeSorted(iter1, iter2);

        List<Integer> collected = new ArrayList<>();
        while (result.hasNext()) {
            collected.add(result.next());
        }
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8), collected);
    }

    @Test
    public void testMergeSortedWithComparator() {
        Iterator<String> iter1 = Arrays.asList("apple", "cherry").iterator();
        Iterator<String> iter2 = Arrays.asList("date", "banana").iterator();

        ObjIterator<String> result = Iterators.mergeSorted(iter1, iter2, Comparator.comparingInt(String::length));

        List<String> collected = new ArrayList<>();
        while (result.hasNext()) {
            collected.add(result.next());
        }
        assertEquals(Arrays.asList("date", "apple", "cherry", "banana"), collected);
    }

    // Tests for zip methods
    @Test
    public void testZip() {
        Iterator<String> iter1 = Arrays.asList("a", "b", "c").iterator();
        Iterator<Integer> iter2 = Arrays.asList(1, 2, 3).iterator();

        BiFunction<String, Integer, String> zipFunction = (s, i) -> s + i;
        ObjIterator<String> result = Iterators.zip(iter1, iter2, zipFunction);

        assertEquals("a1", result.next());
        assertEquals("b2", result.next());
        assertEquals("c3", result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testZipThree() {
        Iterator<String> iter1 = Arrays.asList("a", "b").iterator();
        Iterator<Integer> iter2 = Arrays.asList(1, 2).iterator();
        Iterator<Boolean> iter3 = Arrays.asList(true, false).iterator();

        TriFunction<String, Integer, Boolean, String> zipFunction = (s, i, b) -> s + i + ":" + b;

        ObjIterator<String> result = Iterators.zip(iter1, iter2, iter3, zipFunction);

        assertEquals("a1:true", result.next());
        assertEquals("b2:false", result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testZipWithDefaults() {
        Iterator<String> iter1 = Arrays.asList("a", "b", "c").iterator();
        Iterator<Integer> iter2 = Arrays.asList(1, 2).iterator();

        BiFunction<String, Integer, String> zipFunction = (s, i) -> s + i;
        ObjIterator<String> result = Iterators.zip(iter1, iter2, "X", 0, zipFunction);

        assertEquals("a1", result.next());
        assertEquals("b2", result.next());
        assertEquals("c0", result.next());
        assertFalse(result.hasNext());
    }

    // Tests for unzip methods
    @Test
    public void testUnzip() {
        List<Pair<String, Integer>> pairs = Arrays.asList(Pair.of("a", 1), Pair.of("b", 2), Pair.of("c", 3));

        BiConsumer<Pair<String, Integer>, Pair<String, Integer>> unzipper = (source, target) -> {
            target.setLeft(source.left());
            target.setRight(source.right());
        };

        BiIterator<String, Integer> result = Iterators.unzip(pairs.iterator(), unzipper);

        List<String> lefts = new ArrayList<>();
        List<Integer> rights = new ArrayList<>();

        result.forEachRemaining((l, r) -> {
            lefts.add(l);
            rights.add(r);
        });

        assertEquals(Arrays.asList("a", "b", "c"), lefts);
        assertEquals(Arrays.asList(1, 2, 3), rights);
    }

    // Tests for advance
    @Test
    public void testAdvance() {
        Iterator<String> iter = Arrays.asList("a", "b", "c", "d", "e").iterator();

        long advanced = Iterators.advance(iter, 3);
        assertEquals(3, advanced);
        assertEquals("d", iter.next());

        // Test advancing past end
        advanced = Iterators.advance(iter, 10);
        assertEquals(1, advanced); // Only "e" was left
        assertFalse(iter.hasNext());

        // Test negative
        assertThrows(IllegalArgumentException.class, () -> Iterators.advance(intList.iterator(), -1));
    }

    // Tests for skip
    @Test
    public void testSkip() {
        ObjIterator<String> iter = Iterators.skip(stringList.iterator(), 2);

        assertEquals("cherry", iter.next());
        assertEquals("date", iter.next());
        assertFalse(iter.hasNext());

        // Test skip 0
        iter = Iterators.skip(stringList.iterator(), 0);
        assertEquals("apple", iter.next());

        // Test skip more than size
        iter = Iterators.skip(stringList.iterator(), 10);
        assertFalse(iter.hasNext());

        // Test null iterator
        iter = Iterators.skip(null, 2);
        assertFalse(iter.hasNext());
    }

    // Tests for limit
    @Test
    public void testLimit() {
        ObjIterator<String> iter = Iterators.limit(stringList.iterator(), 2);

        assertEquals("apple", iter.next());
        assertEquals("banana", iter.next());
        assertFalse(iter.hasNext());

        // Test limit 0
        iter = Iterators.limit(stringList.iterator(), 0);
        assertFalse(iter.hasNext());

        // Test limit more than size
        iter = Iterators.limit(stringList.iterator(), 10);
        int count = 0;
        while (iter.hasNext()) {
            iter.next();
            count++;
        }
        assertEquals(4, count);
    }

    // Tests for skipAndLimit
    @Test
    public void testSkipAndLimit() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e", "f");
        ObjIterator<String> iter = Iterators.skipAndLimit(list.iterator(), 2, 3);

        assertEquals("c", iter.next());
        assertEquals("d", iter.next());
        assertEquals("e", iter.next());
        assertFalse(iter.hasNext());

        // Test with iterable
        iter = Iterators.skipAndLimit(list, 1, 2);
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());
    }

    // Tests for skipNulls
    @Test
    public void testSkipNulls() {
        List<String> withNulls = Arrays.asList("a", null, "b", null, "c");
        ObjIterator<String> iter = Iterators.skipNulls(withNulls.iterator());

        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());

        // Test with iterable
        iter = Iterators.skipNulls(withNulls);
        List<String> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    // Tests for distinct
    @Test
    public void testDistinct() {
        List<Integer> withDuplicates = Arrays.asList(1, 2, 3, 2, 4, 3, 5);
        ObjIterator<Integer> iter = Iterators.distinct(withDuplicates.iterator());

        List<Integer> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);

        // Test with iterable
        iter = Iterators.distinct(withDuplicates);
        result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testDistinctBy() {
        List<String> words = Arrays.asList("apple", "apricot", "banana", "berry", "cherry");
        Function<String, Character> firstLetter = s -> s.charAt(0);

        ObjIterator<String> iter = Iterators.distinctBy(words.iterator(), firstLetter);

        List<String> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        assertEquals(Arrays.asList("apple", "banana", "cherry"), result);
    }

    // Tests for filter
    @Test
    public void testFilter() {
        Predicate<Integer> isEven = n -> n % 2 == 0;
        ObjIterator<Integer> iter = Iterators.filter(intList.iterator(), isEven);

        assertEquals(Integer.valueOf(2), iter.next());
        assertEquals(Integer.valueOf(4), iter.next());
        assertFalse(iter.hasNext());

        // Test with iterable
        iter = Iterators.filter(intList, isEven);
        List<Integer> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        assertEquals(Arrays.asList(2, 4), result);
    }

    // Tests for takeWhile
    @Test
    public void testTakeWhile() {
        Predicate<Integer> lessThan4 = n -> n < 4;
        ObjIterator<Integer> iter = Iterators.takeWhile(intList.iterator(), lessThan4);

        assertEquals(Integer.valueOf(1), iter.next());
        assertEquals(Integer.valueOf(2), iter.next());
        assertEquals(Integer.valueOf(3), iter.next());
        assertFalse(iter.hasNext());

        // Test with iterable
        iter = Iterators.takeWhile(intList, lessThan4);
        List<Integer> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testTakeWhileInclusive() {
        Predicate<Integer> lessThan4 = n -> n < 4;
        ObjIterator<Integer> iter = Iterators.takeWhileInclusive(intList.iterator(), lessThan4);

        assertEquals(Integer.valueOf(1), iter.next());
        assertEquals(Integer.valueOf(2), iter.next());
        assertEquals(Integer.valueOf(3), iter.next());
        assertEquals(Integer.valueOf(4), iter.next()); // Includes the first false element
        assertFalse(iter.hasNext());
    }

    // Tests for dropWhile
    @Test
    public void testDropWhile() {
        Predicate<Integer> lessThan3 = n -> n < 3;
        ObjIterator<Integer> iter = Iterators.dropWhile(intList.iterator(), lessThan3);

        assertEquals(Integer.valueOf(3), iter.next());
        assertEquals(Integer.valueOf(4), iter.next());
        assertEquals(Integer.valueOf(5), iter.next());
        assertFalse(iter.hasNext());

        // Test with iterable
        iter = Iterators.dropWhile(intList, lessThan3);
        List<Integer> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        assertEquals(Arrays.asList(3, 4, 5), result);
    }

    // Tests for skipUntil
    @Test
    public void testSkipUntil() {
        Predicate<Integer> greaterThan2 = n -> n > 2;
        ObjIterator<Integer> iter = Iterators.skipUntil(intList.iterator(), greaterThan2);

        assertEquals(Integer.valueOf(3), iter.next());
        assertEquals(Integer.valueOf(4), iter.next());
        assertEquals(Integer.valueOf(5), iter.next());
        assertFalse(iter.hasNext());

        // Test with iterable
        iter = Iterators.skipUntil(intList, greaterThan2);
        List<Integer> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        assertEquals(Arrays.asList(3, 4, 5), result);
    }

    // Tests for map
    @Test
    public void testMap() {
        Function<Integer, String> toString = Object::toString;
        ObjIterator<String> iter = Iterators.map(intList.iterator(), toString);

        assertEquals("1", iter.next());
        assertEquals("2", iter.next());
        assertEquals("3", iter.next());
        assertEquals("4", iter.next());
        assertEquals("5", iter.next());
        assertFalse(iter.hasNext());

        // Test with iterable
        iter = Iterators.map(intList, toString);
        assertTrue(iter.hasNext());
        assertEquals("1", iter.next());
    }

    // Tests for flatMap
    @Test
    public void testFlatMap() {
        Function<Integer, List<String>> duplicateAsString = n -> Arrays.asList(String.valueOf(n), String.valueOf(n));

        ObjIterator<String> iter = Iterators.flatMap(Arrays.asList(1, 2, 3).iterator(), duplicateAsString);

        assertEquals("1", iter.next());
        assertEquals("1", iter.next());
        assertEquals("2", iter.next());
        assertEquals("2", iter.next());
        assertEquals("3", iter.next());
        assertEquals("3", iter.next());
        assertFalse(iter.hasNext());

        // Test with iterable
        iter = Iterators.flatMap(Arrays.asList(1, 2), duplicateAsString);
        List<String> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        assertEquals(Arrays.asList("1", "1", "2", "2"), result);
    }

    @Test
    public void testFlatmapArray() {
        Function<Integer, String[]> duplicateAsArray = n -> new String[] { String.valueOf(n), String.valueOf(n) + "!" };

        ObjIterator<String> iter = Iterators.flatmap(Arrays.asList(1, 2).iterator(), duplicateAsArray);

        assertEquals("1", iter.next());
        assertEquals("1!", iter.next());
        assertEquals("2", iter.next());
        assertEquals("2!", iter.next());
        assertFalse(iter.hasNext());
    }

    // Tests for forEach methods
    @Test
    public void testForEach() throws Exception {
        List<Integer> collected = new ArrayList<>();
        Iterators.forEach(intList.iterator(), collected::add);
        assertEquals(intList, collected);

        // Test with offset and count
        collected.clear();
        Iterators.forEach(intList.iterator(), 1, 3, collected::add);
        assertEquals(Arrays.asList(2, 3, 4), collected);

        // Test with onComplete
        AtomicInteger completeCount = new AtomicInteger(0);
        collected.clear();
        Iterators.forEach(intList.iterator(), collected::add, completeCount::incrementAndGet);
        assertEquals(intList, collected);
        assertEquals(1, completeCount.get());
    }

    @Test
    public void testForEachMultipleIterators() throws Exception {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2).iterator(), Arrays.asList(3, 4).iterator(), Arrays.asList(5, 6).iterator());

        List<Integer> collected = new ArrayList<>();
        Iterators.forEach(iterators, collected::add);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), collected);

        // Test with offset and count
        iterators = Arrays.asList(Arrays.asList(1, 2, 3).iterator(), Arrays.asList(4, 5, 6).iterator());
        collected.clear();
        Iterators.forEach(iterators, 2, 3, collected::add);
        assertEquals(Arrays.asList(3, 4, 5), collected);
    }

    @Test
    public void testForEachWithThreads() throws Exception {
        List<Integer> largeList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            largeList.add(i);
        }

        Set<Integer> collected = Collections.synchronizedSet(new HashSet<>());
        Iterators.forEach(largeList.iterator(), 0, 100, 4, 10, collected::add);

        assertEquals(100, collected.size());
        for (int i = 0; i < 100; i++) {
            assertTrue(collected.contains(i));
        }
    }

    // Edge cases and error conditions
    @Test
    public void testNullIteratorHandling() {
        assertFalse(Iterators.skip(null, 5).hasNext());
        assertFalse(Iterators.limit(null, 5).hasNext());
        assertFalse(Iterators.filter((Iterator<String>) null, n -> true).hasNext());
        assertFalse(Iterators.distinct((Iterator<String>) null).hasNext());
        assertFalse(Iterators.map((Iterator<String>) null, Function.identity()).hasNext());
    }

    @Test
    public void testEmptyIteratorHandling() {
        Iterator<String> empty = new ArrayList<String>().iterator();

        assertFalse(Iterators.skip(empty, 5).hasNext());
        assertFalse(Iterators.limit(empty, 5).hasNext());
        assertFalse(Iterators.filter(empty, n -> true).hasNext());
        assertFalse(Iterators.distinct(empty).hasNext());
    }

    @Test
    public void testNoSuchElementException() {
        ObjIterator<String> iter = Iterators.limit(Arrays.asList("a").iterator(), 1);
        iter.next();
        assertThrows(NoSuchElementException.class, () -> iter.next());

        ObjIterator<Integer> iter2 = Iterators.filter(Arrays.asList(1, 2, 3).iterator(), n -> n > 10);
        assertThrows(NoSuchElementException.class, () -> iter2.next());
    }

    // Test primitive array concatenation
    @Test
    public void testConcatPrimitiveArrays() {
        // Test char arrays
        char[] charArr1 = { 'a', 'b' };
        char[] charArr2 = { 'c', 'd' };
        CharIterator charIter = Iterators.concat(charArr1, charArr2);
        assertEquals('a', charIter.nextChar());
        assertEquals('b', charIter.nextChar());
        assertEquals('c', charIter.nextChar());
        assertEquals('d', charIter.nextChar());
        assertFalse(charIter.hasNext());

        // Test byte arrays
        byte[] byteArr1 = { 1, 2 };
        byte[] byteArr2 = { 3, 4 };
        ByteIterator byteIter = Iterators.concat(byteArr1, byteArr2);
        assertEquals((byte) 1, byteIter.nextByte());
        assertEquals((byte) 2, byteIter.nextByte());
        assertEquals((byte) 3, byteIter.nextByte());
        assertEquals((byte) 4, byteIter.nextByte());
        assertFalse(byteIter.hasNext());

        // Test short arrays
        short[] shortArr1 = { 10, 20 };
        short[] shortArr2 = { 30, 40 };
        ShortIterator shortIter = Iterators.concat(shortArr1, shortArr2);
        assertEquals((short) 10, shortIter.nextShort());
        assertEquals((short) 20, shortIter.nextShort());
        assertEquals((short) 30, shortIter.nextShort());
        assertEquals((short) 40, shortIter.nextShort());
        assertFalse(shortIter.hasNext());

        // Test long arrays
        long[] longArr1 = { 100L, 200L };
        long[] longArr2 = { 300L, 400L };
        LongIterator longIter = Iterators.concat(longArr1, longArr2);
        assertEquals(100L, longIter.nextLong());
        assertEquals(200L, longIter.nextLong());
        assertEquals(300L, longIter.nextLong());
        assertEquals(400L, longIter.nextLong());
        assertFalse(longIter.hasNext());

        // Test float arrays
        float[] floatArr1 = { 1.1f, 2.2f };
        float[] floatArr2 = { 3.3f, 4.4f };
        FloatIterator floatIter = Iterators.concat(floatArr1, floatArr2);
        assertEquals(1.1f, floatIter.nextFloat(), 0.001f);
        assertEquals(2.2f, floatIter.nextFloat(), 0.001f);
        assertEquals(3.3f, floatIter.nextFloat(), 0.001f);
        assertEquals(4.4f, floatIter.nextFloat(), 0.001f);
        assertFalse(floatIter.hasNext());

        // Test double arrays
        double[] doubleArr1 = { 1.11, 2.22 };
        double[] doubleArr2 = { 3.33, 4.44 };
        DoubleIterator doubleIter = Iterators.concat(doubleArr1, doubleArr2);
        assertEquals(1.11, doubleIter.nextDouble(), 0.001);
        assertEquals(2.22, doubleIter.nextDouble(), 0.001);
        assertEquals(3.33, doubleIter.nextDouble(), 0.001);
        assertEquals(4.44, doubleIter.nextDouble(), 0.001);
        assertFalse(doubleIter.hasNext());
    }

    // Test primitive iterator concatenation
    @Test
    public void testConcatPrimitiveIterators() {
        // Test IntIterator concatenation
        IntIterator intIter1 = IntIterator.of(1, 2);
        IntIterator intIter2 = IntIterator.of(3, 4);
        IntIterator concatenated = Iterators.concat(intIter1, intIter2);
        assertEquals(1, concatenated.nextInt());
        assertEquals(2, concatenated.nextInt());
        assertEquals(3, concatenated.nextInt());
        assertEquals(4, concatenated.nextInt());
        assertFalse(concatenated.hasNext());

        // Test empty iterator handling
        IntIterator empty = IntIterator.empty();
        IntIterator nonEmpty = IntIterator.of(5, 6);
        concatenated = Iterators.concat(empty, nonEmpty);
        assertEquals(5, concatenated.nextInt());
        assertEquals(6, concatenated.nextInt());
        assertFalse(concatenated.hasNext());
    }

    // Test TriIterator operations
    @Test
    public void testTriIteratorConcat() {
        TriIterator<String, Integer, Boolean> tri1 = TriIterator.zip(N.asList("a"), N.asList(1), N.asList(true));
        TriIterator<String, Integer, Boolean> tri2 = TriIterator.zip(N.asList("b"), N.asList(2), N.asList(false));

        TriIterator<String, Integer, Boolean> result = Iterators.concat(tri1, tri2);

        Triple<String, Integer, Boolean> triple1 = result.next();
        assertEquals("a", triple1.left());
        assertEquals(Integer.valueOf(1), triple1.middle());
        assertEquals(true, triple1.right());

        Triple<String, Integer, Boolean> triple2 = result.next();
        assertEquals("b", triple2.left());
        assertEquals(Integer.valueOf(2), triple2.middle());
        assertEquals(false, triple2.right());

        assertFalse(result.hasNext());
    }

    // Complex scenarios
    @Test
    public void testChainedOperations() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        // Skip 2, filter even, map to string, limit to 3
        ObjIterator<Integer> iter1 = Iterators.skip(numbers.iterator(), 2);
        ObjIterator<Integer> iter2 = Iterators.filter(iter1, n -> n % 2 == 0);
        ObjIterator<String> iter3 = Iterators.map(iter2, Object::toString);
        ObjIterator<String> iter4 = Iterators.limit(iter3, 3);

        List<String> result = new ArrayList<>();
        while (iter4.hasNext()) {
            result.add(iter4.next());
        }
        assertEquals(Arrays.asList("4", "6", "8"), result);
    }

    @Test
    public void testMergeIterablesEdgeCases() {
        // Test with empty collection
        List<Iterable<Integer>> empty = new ArrayList<>();
        ObjIterator<Integer> result = Iterators.mergeIterables(empty, (a, b) -> MergeResult.TAKE_FIRST);
        assertFalse(result.hasNext());

        // Test with single iterable
        List<Iterable<Integer>> single = Arrays.asList(Arrays.asList(1, 2, 3));
        result = Iterators.mergeIterables(single, (a, b) -> MergeResult.TAKE_FIRST);
        assertEquals(Integer.valueOf(1), result.next());
        assertEquals(Integer.valueOf(2), result.next());
        assertEquals(Integer.valueOf(3), result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatIterablesEdgeCases() {
        // Test with null in collection
        List<Iterable<String>> withNull = new ArrayList<>();
        withNull.add(Arrays.asList("a", "b"));
        withNull.add(null);
        withNull.add(Arrays.asList("c", "d"));

        ObjIterator<String> result = Iterators.concatIterables(withNull);
        List<String> collected = new ArrayList<>();
        while (result.hasNext()) {
            collected.add(result.next());
        }
        assertEquals(Arrays.asList("a", "b", "c", "d"), collected);
    }
}
