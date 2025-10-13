package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.TriFunction;

@Tag("new-test")
public class Iterators101Test extends TestBase {

    private List<String> testList;
    private Iterator<String> testIterator;

    @BeforeEach
    public void setUp() {
        testList = Arrays.asList("a", "b", "c", "d", "e");
        testIterator = testList.iterator();
    }

    @AfterEach
    public void tearDown() {
        testList = null;
        testIterator = null;
    }

    @Test
    public void testGet() {
        Iterator<String> iter = Arrays.asList("a", "b", "c").iterator();

        assertEquals("a", Iterators.get(iter, 0).get());

        iter = Arrays.asList("a", "b", "c").iterator();
        assertEquals("b", Iterators.get(iter, 1).get());

        iter = Arrays.asList("a", "b", "c").iterator();
        assertFalse(Iterators.get(iter, 5).isPresent());

        assertTrue(Iterators.get(null, 0).isEmpty());

        assertTrue(Iterators.get(Collections.emptyIterator(), 0).isEmpty());
    }

    @Test
    public void testGetNegativeIndex() {
        assertThrows(IllegalArgumentException.class, () -> Iterators.get(testIterator, -1));
    }

    @Test
    public void testOccurrencesOf() {
        Iterator<String> iter = Arrays.asList("a", "b", "a", "c", "a").iterator();
        assertEquals(3, Iterators.occurrencesOf(iter, "a"));

        iter = Arrays.asList("a", "b", "c").iterator();
        assertEquals(0, Iterators.occurrencesOf(iter, "x"));

        iter = Arrays.asList("a", null, "b", null).iterator();
        assertEquals(2, Iterators.occurrencesOf(iter, null));

        assertEquals(0, Iterators.occurrencesOf(null, "a"));
    }

    @Test
    public void testCount() {
        assertEquals(5, Iterators.count(testIterator));
        assertEquals(0, Iterators.count(Collections.emptyIterator()));
        assertEquals(0, Iterators.count(null));
    }

    @Test
    public void testCountWithPredicate() {
        Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4, 5).iterator();
        assertEquals(2, Iterators.count(iter, x -> x % 2 == 0));

        iter = Arrays.asList(1, 2, 3, 4, 5).iterator();
        assertEquals(0, Iterators.count(iter, x -> x > 10));

        assertEquals(0, Iterators.count(null, x -> true));
    }

    @Test
    public void testCountWithNullPredicate() {
        assertThrows(IllegalArgumentException.class, () -> Iterators.count(testIterator, null));
    }

    @Test
    public void testIndexOf() {
        Iterator<String> iter = Arrays.asList("a", "b", "c", "b").iterator();
        assertEquals(1, Iterators.indexOf(iter, "b"));

        iter = Arrays.asList("a", "b", "c").iterator();
        assertEquals(-1, Iterators.indexOf(iter, "x"));

        assertEquals(-1, Iterators.indexOf(null, "a"));
    }

    @Test
    public void testIndexOfWithFromIndex() {
        Iterator<String> iter = Arrays.asList("a", "b", "c", "b").iterator();
        assertEquals(3, Iterators.indexOf(iter, "b", 2));

        iter = Arrays.asList("a", "b", "c").iterator();
        assertEquals(-1, Iterators.indexOf(iter, "a", 1));
    }

    @Test
    public void testElementsEqual() {
        Iterator<String> iter1 = Arrays.asList("a", "b", "c").iterator();
        Iterator<String> iter2 = Arrays.asList("a", "b", "c").iterator();
        assertTrue(Iterators.elementsEqual(iter1, iter2));

        iter1 = Arrays.asList("a", "b", "c").iterator();
        iter2 = Arrays.asList("a", "b", "d").iterator();
        assertFalse(Iterators.elementsEqual(iter1, iter2));

        iter1 = Arrays.asList("a", "b").iterator();
        iter2 = Arrays.asList("a", "b", "c").iterator();
        assertFalse(Iterators.elementsEqual(iter1, iter2));
    }

    @Test
    public void testRepeatElement() {
        ObjIterator<String> iter = Iterators.repeat("hello", 3);
        assertEquals("hello", iter.next());
        assertEquals("hello", iter.next());
        assertEquals("hello", iter.next());
        assertFalse(iter.hasNext());

        iter = Iterators.repeat("hello", 0);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testRepeatElementLong() {
        ObjIterator<String> iter = Iterators.repeat("hello", 3L);
        assertEquals("hello", iter.next());
        assertEquals("hello", iter.next());
        assertEquals("hello", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testRepeatElementNegative() {
        assertThrows(IllegalArgumentException.class, () -> Iterators.repeat("hello", -1));
    }

    @Test
    public void testRepeatElements() {
        Collection<String> collection = Arrays.asList("a", "b");
        ObjIterator<String> iter = Iterators.repeatElements(collection, 2);

        assertEquals("a", iter.next());
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("b", iter.next());
        assertFalse(iter.hasNext());

        iter = Iterators.repeatElements(Collections.emptyList(), 3);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testRepeatCollection() {
        Collection<String> collection = Arrays.asList("a", "b");
        ObjIterator<String> iter = Iterators.repeatCollection(collection, 2);

        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testRepeatElementsToSize() {
        Collection<String> collection = Arrays.asList("a", "b");
        ObjIterator<String> iter = Iterators.repeatElementsToSize(collection, 5);

        assertEquals("a", iter.next());
        assertEquals("a", iter.next());
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("b", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testRepeatCollectionToSize() {
        Collection<String> collection = Arrays.asList("a", "b");
        ObjIterator<String> iter = Iterators.repeatCollectionToSize(collection, 5);

        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("a", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testCycleArray() {
        ObjIterator<String> iter = Iterators.cycle("a", "b");

        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertTrue(iter.hasNext());

        iter = Iterators.cycle();
        assertFalse(iter.hasNext());
    }

    @Test
    public void testCycleIterable() {
        Iterable<String> iterable = Arrays.asList("a", "b");
        ObjIterator<String> iter = Iterators.cycle(iterable);

        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("a", iter.next());
        assertTrue(iter.hasNext());

        iter = Iterators.cycle(Collections.emptyList());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testCycleIterableWithRounds() {
        Iterable<String> iterable = Arrays.asList("a", "b");
        ObjIterator<String> iter = Iterators.cycle(iterable, 2);

        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertFalse(iter.hasNext());

        iter = Iterators.cycle(iterable, 0);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatArrays() {
        boolean[] arr1 = { true, false };
        boolean[] arr2 = { true };
        BooleanIterator iter = Iterators.concat(arr1, arr2);

        assertTrue(iter.nextBoolean());
        assertFalse(iter.nextBoolean());
        assertTrue(iter.nextBoolean());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatObjectArrays() {
        String[] arr1 = { "a", "b" };
        String[] arr2 = { "c", "d" };
        ObjIterator<String> iter = Iterators.concat(arr1, arr2);

        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertEquals("d", iter.next());
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
        Iterable<String> iter1 = Arrays.asList("a", "b");
        Iterable<String> iter2 = Arrays.asList("c", "d");
        ObjIterator<String> result = Iterators.concat(iter1, iter2);

        assertEquals("a", result.next());
        assertEquals("b", result.next());
        assertEquals("c", result.next());
        assertEquals("d", result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatMaps() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        Map<String, Integer> map2 = new HashMap<>();
        map2.put("b", 2);

        ObjIterator<Map.Entry<String, Integer>> result = Iterators.concat(map1, map2);

        Set<String> keys = new HashSet<>();
        Set<Integer> values = new HashSet<>();
        while (result.hasNext()) {
            Map.Entry<String, Integer> entry = result.next();
            keys.add(entry.getKey());
            values.add(entry.getValue());
        }

        assertTrue(keys.contains("a"));
        assertTrue(keys.contains("b"));
        assertTrue(values.contains(1));
        assertTrue(values.contains(2));
    }

    @Test
    public void testConcatCollection() {
        List<Iterator<String>> iterators = Arrays.asList(Arrays.asList("a", "b").iterator(), Arrays.asList("c", "d").iterator());

        ObjIterator<String> result = Iterators.concat(iterators);

        assertEquals("a", result.next());
        assertEquals("b", result.next());
        assertEquals("c", result.next());
        assertEquals("d", result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testConcatIterables_02() {
        List<Iterable<String>> iterables = Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c", "d"));

        ObjIterator<String> result = Iterators.concatIterables(iterables);

        assertEquals("a", result.next());
        assertEquals("b", result.next());
        assertEquals("c", result.next());
        assertEquals("d", result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testMerge() {
        Iterator<Integer> iter1 = Arrays.asList(1, 3, 5).iterator();
        Iterator<Integer> iter2 = Arrays.asList(2, 4, 6).iterator();

        BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        ObjIterator<Integer> result = Iterators.merge(iter1, iter2, selector);

        assertEquals(Integer.valueOf(1), result.next());
        assertEquals(Integer.valueOf(2), result.next());
        assertEquals(Integer.valueOf(3), result.next());
        assertEquals(Integer.valueOf(4), result.next());
        assertEquals(Integer.valueOf(5), result.next());
        assertEquals(Integer.valueOf(6), result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testMergeCollection() {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 4).iterator(), Arrays.asList(2, 5).iterator(), Arrays.asList(3, 6).iterator());

        BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        ObjIterator<Integer> result = Iterators.merge(iterators, selector);

        assertEquals(Integer.valueOf(1), result.next());
        assertEquals(Integer.valueOf(2), result.next());
        assertEquals(Integer.valueOf(3), result.next());
        assertTrue(result.hasNext());
    }

    @Test
    public void testMergeIterables() {
        Iterable<Integer> iter1 = Arrays.asList(1, 3, 5);
        Iterable<Integer> iter2 = Arrays.asList(2, 4, 6);

        BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        ObjIterator<Integer> result = Iterators.merge(iter1, iter2, selector);

        assertEquals(Integer.valueOf(1), result.next());
        assertEquals(Integer.valueOf(2), result.next());
        assertTrue(result.hasNext());
    }

    @Test
    public void testMergeIterablesCollection() {
        List<Iterable<Integer>> iterables = Arrays.asList(Arrays.asList(1, 4), Arrays.asList(2, 5), Arrays.asList(3, 6));

        BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        ObjIterator<Integer> result = Iterators.mergeIterables(iterables, selector);

        assertEquals(Integer.valueOf(1), result.next());
        assertEquals(Integer.valueOf(2), result.next());
        assertEquals(Integer.valueOf(3), result.next());
        assertTrue(result.hasNext());
    }

    @Test
    public void testMergeSorted() {
        Iterator<Integer> iter1 = Arrays.asList(1, 3, 5).iterator();
        Iterator<Integer> iter2 = Arrays.asList(2, 4, 6).iterator();

        ObjIterator<Integer> result = Iterators.mergeSorted(iter1, iter2);

        assertEquals(Integer.valueOf(1), result.next());
        assertEquals(Integer.valueOf(2), result.next());
        assertEquals(Integer.valueOf(3), result.next());
        assertEquals(Integer.valueOf(4), result.next());
        assertEquals(Integer.valueOf(5), result.next());
        assertEquals(Integer.valueOf(6), result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testMergeSortedWithComparator() {
        Iterator<Integer> iter1 = Arrays.asList(1, 3, 5).iterator();
        Iterator<Integer> iter2 = Arrays.asList(2, 4, 6).iterator();
        Comparator<Integer> cmp = Integer::compareTo;

        ObjIterator<Integer> result = Iterators.mergeSorted(iter1, iter2, cmp);

        assertEquals(Integer.valueOf(1), result.next());
        assertEquals(Integer.valueOf(2), result.next());
        assertEquals(Integer.valueOf(3), result.next());
        assertEquals(Integer.valueOf(4), result.next());
        assertEquals(Integer.valueOf(5), result.next());
        assertEquals(Integer.valueOf(6), result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testMergeSortedIterables() {
        Iterable<Integer> iter1 = Arrays.asList(1, 3, 5);
        Iterable<Integer> iter2 = Arrays.asList(2, 4, 6);

        ObjIterator<Integer> result = Iterators.mergeSorted(iter1, iter2);

        assertEquals(Integer.valueOf(1), result.next());
        assertEquals(Integer.valueOf(2), result.next());
        assertEquals(Integer.valueOf(3), result.next());
        assertEquals(Integer.valueOf(4), result.next());
        assertEquals(Integer.valueOf(5), result.next());
        assertEquals(Integer.valueOf(6), result.next());
        assertFalse(result.hasNext());
    }

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
    public void testZipIterables() {
        Iterable<String> iter1 = Arrays.asList("a", "b", "c");
        Iterable<Integer> iter2 = Arrays.asList(1, 2, 3);

        BiFunction<String, Integer, String> zipFunction = (s, i) -> s + i;
        ObjIterator<String> result = Iterators.zip(iter1, iter2, zipFunction);

        assertEquals("a1", result.next());
        assertEquals("b2", result.next());
        assertEquals("c3", result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testZipThreeIterators() {
        Iterator<String> iter1 = Arrays.asList("a", "b").iterator();
        Iterator<Integer> iter2 = Arrays.asList(1, 2).iterator();
        Iterator<Boolean> iter3 = Arrays.asList(true, false).iterator();

        TriFunction<String, Integer, Boolean, String> zipFunction = (s, i, b) -> s + i + b;
        ObjIterator<String> result = Iterators.zip(iter1, iter2, iter3, zipFunction);

        assertEquals("a1true", result.next());
        assertEquals("b2false", result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testZipThreeIterables() {
        Iterable<String> iter1 = Arrays.asList("a", "b");
        Iterable<Integer> iter2 = Arrays.asList(1, 2);
        Iterable<Boolean> iter3 = Arrays.asList(true, false);

        TriFunction<String, Integer, Boolean, String> zipFunction = (s, i, b) -> s + i + b;
        ObjIterator<String> result = Iterators.zip(iter1, iter2, iter3, zipFunction);

        assertEquals("a1true", result.next());
        assertEquals("b2false", result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testZipWithDefaultValues() {
        Iterator<String> iter1 = Arrays.asList("a", "b", "c").iterator();
        Iterator<Integer> iter2 = Arrays.asList(1, 2).iterator();

        BiFunction<String, Integer, String> zipFunction = (s, i) -> s + i;
        ObjIterator<String> result = Iterators.zip(iter1, iter2, "default", -1, zipFunction);

        assertEquals("a1", result.next());
        assertEquals("b2", result.next());
        assertEquals("c-1", result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testZipIterablesWithDefaultValues() {
        Iterable<String> iter1 = Arrays.asList("a", "b", "c");
        Iterable<Integer> iter2 = Arrays.asList(1, 2);

        BiFunction<String, Integer, String> zipFunction = (s, i) -> s + i;
        ObjIterator<String> result = Iterators.zip(iter1, iter2, "default", -1, zipFunction);

        assertEquals("a1", result.next());
        assertEquals("b2", result.next());
        assertEquals("c-1", result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testZipThreeWithDefaultValues() {
        Iterator<String> iter1 = Arrays.asList("a", "b", "c").iterator();
        Iterator<Integer> iter2 = Arrays.asList(1, 2).iterator();
        Iterator<Boolean> iter3 = Arrays.asList(true).iterator();

        TriFunction<String, Integer, Boolean, String> zipFunction = (s, i, b) -> s + i + b;
        ObjIterator<String> result = Iterators.zip(iter1, iter2, iter3, "default", -1, false, zipFunction);

        assertEquals("a1true", result.next());
        assertEquals("b2false", result.next());
        assertEquals("c-1false", result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testZipThreeIterablesWithDefaultValues() {
        Iterable<String> iter1 = Arrays.asList("a", "b", "c");
        Iterable<Integer> iter2 = Arrays.asList(1, 2);
        Iterable<Boolean> iter3 = Arrays.asList(true);

        TriFunction<String, Integer, Boolean, String> zipFunction = (s, i, b) -> s + i + b;
        ObjIterator<String> result = Iterators.zip(iter1, iter2, iter3, "default", -1, false, zipFunction);

        assertEquals("a1true", result.next());
        assertEquals("b2false", result.next());
        assertEquals("c-1false", result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testUnzip() {
        Iterator<String> iter = Arrays.asList("a1", "b2", "c3").iterator();

        BiConsumer<String, Pair<String, Integer>> unzip = (s, pair) -> {
            pair.setLeft(s.substring(0, 1));
            pair.setRight(Integer.parseInt(s.substring(1)));
        };

        BiIterator<String, Integer> result = Iterators.unzip(iter, unzip);

        Pair<String, Integer> pair1 = result.next();
        assertEquals("a", pair1.left());
        assertEquals(Integer.valueOf(1), pair1.right());

        Pair<String, Integer> pair2 = result.next();
        assertEquals("b", pair2.left());
        assertEquals(Integer.valueOf(2), pair2.right());

        assertTrue(result.hasNext());
    }

    @Test
    public void testUnzipIterable() {
        Iterable<String> iter = Arrays.asList("a1", "b2", "c3");

        BiConsumer<String, Pair<String, Integer>> unzip = (s, pair) -> {
            pair.setLeft(s.substring(0, 1));
            pair.setRight(Integer.parseInt(s.substring(1)));
        };

        BiIterator<String, Integer> result = Iterators.unzip(iter, unzip);

        Pair<String, Integer> pair1 = result.next();
        assertEquals("a", pair1.left());
        assertEquals(Integer.valueOf(1), pair1.right());

        assertTrue(result.hasNext());
    }

    @Test
    public void testAdvance() {
        Iterator<String> iter = Arrays.asList("a", "b", "c", "d", "e").iterator();

        long advanced = Iterators.advance(iter, 3);
        assertEquals(3, advanced);
        assertEquals("d", iter.next());

        iter = Arrays.asList("a", "b").iterator();
        advanced = Iterators.advance(iter, 5);
        assertEquals(2, advanced);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testAdvanceNegative() {
        assertThrows(IllegalArgumentException.class, () -> Iterators.advance(testIterator, -1));
    }

    @Test
    public void testSkip() {
        Iterator<String> iter = Arrays.asList("a", "b", "c", "d", "e").iterator();
        ObjIterator<String> result = Iterators.skip(iter, 2);

        assertEquals("c", result.next());
        assertEquals("d", result.next());
        assertEquals("e", result.next());
        assertFalse(result.hasNext());

        iter = Arrays.asList("a", "b").iterator();
        result = Iterators.skip(iter, 5);
        assertFalse(result.hasNext());

        result = Iterators.skip(null, 2);
        assertFalse(result.hasNext());
    }

    @Test
    public void testLimit() {
        Iterator<String> iter = Arrays.asList("a", "b", "c", "d", "e").iterator();
        ObjIterator<String> result = Iterators.limit(iter, 3);

        assertEquals("a", result.next());
        assertEquals("b", result.next());
        assertEquals("c", result.next());
        assertFalse(result.hasNext());

        iter = Arrays.asList("a", "b").iterator();
        result = Iterators.limit(iter, 5);
        assertEquals("a", result.next());
        assertEquals("b", result.next());
        assertFalse(result.hasNext());

        result = Iterators.limit(null, 3);
        assertFalse(result.hasNext());

        result = Iterators.limit(testIterator, 0);
        assertFalse(result.hasNext());
    }

    @Test
    public void testSkipAndLimit() {
        Iterator<String> iter = Arrays.asList("a", "b", "c", "d", "e").iterator();
        ObjIterator<String> result = Iterators.skipAndLimit(iter, 1, 3);

        assertEquals("b", result.next());
        assertEquals("c", result.next());
        assertEquals("d", result.next());
        assertFalse(result.hasNext());

        Iterable<String> iterable = Arrays.asList("a", "b", "c", "d", "e");
        result = Iterators.skipAndLimit(iterable, 2, 2);

        assertEquals("c", result.next());
        assertEquals("d", result.next());
        assertFalse(result.hasNext());
    }

}
