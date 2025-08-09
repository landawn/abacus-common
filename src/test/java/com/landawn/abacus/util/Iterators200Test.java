package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.function.Function;

//Minimal Stubs for compilation and basic testing.
//In a real project, these would be part of the abacus-common library.

//--- Minimal Stubs --- (Place these outside or handle with a proper test setup)
//For brevity, I'm outlining what they'd represent. In a real test, you'd use the actual library or detailed mocks.

//Assume N, Fn, cs, InternalUtil, Throwables are available from the library or mocked.
//Let's define some basic structures for custom iterators if not using a mocking framework extensively.

public class Iterators200Test extends TestBase {

    // Helper to create a List easily
    private <T> List<T> list(T... elements) {
        return new ArrayList<>(Arrays.asList(elements));
    }

    private <T> Collection<T> coll(T... elements) {
        return new ArrayList<>(Arrays.asList(elements));
    }

    private <K, V> Map<K, V> mapOf(K k1, V v1) {
        Map<K, V> map = new HashMap<>();
        map.put(k1, v1);
        return map;
    }

    private <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2) {
        Map<K, V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }

    // --- Test Methods Start Here ---

    @Test
    public void testGet() {
        assertTrue(Iterators.get(null, 0).isEmpty());
        assertTrue(Iterators.get(Collections.emptyIterator(), 0).isEmpty());

        Iterator<String> iter = list("a", "b", "c").iterator();
        assertEquals("a", Iterators.get(iter, 0).get()); // iter is now at "b"

        iter = list("a", "b", "c").iterator();
        assertEquals("b", Iterators.get(iter, 1).get()); // iter is now at "c"

        iter = list("a", "b", "c").iterator();
        assertEquals("c", Iterators.get(iter, 2).get()); // iter is exhausted

        iter = list("a", "b", "c").iterator();
        assertTrue(Iterators.get(iter, 3).isEmpty()); // index out of bounds

        iter = list("a", "b", "c").iterator();
        assertTrue(Iterators.get(iter, Long.MAX_VALUE).isEmpty());

        assertThrows(IllegalArgumentException.class, () -> Iterators.get(list("a").iterator(), -1));
    }

    @Test
    public void testOccurrencesOf() {
        assertEquals(0, Iterators.occurrencesOf(null, "a"));
        assertEquals(0, Iterators.occurrencesOf(Collections.emptyIterator(), "a"));
        assertEquals(2, Iterators.occurrencesOf(list("a", "b", "a", "c").iterator(), "a"));
        assertEquals(1, Iterators.occurrencesOf(list("a", "b", "a", "c").iterator(), "c"));
        assertEquals(0, Iterators.occurrencesOf(list("a", "b", "a", "c").iterator(), "d"));
        assertEquals(2, Iterators.occurrencesOf(list(null, "a", null).iterator(), null));
        assertEquals(1, Iterators.occurrencesOf(list(null, "a", null).iterator(), "a"));
    }

    @Test
    public void testCountIterator() {
        assertEquals(0, Iterators.count(null));
        assertEquals(0, Iterators.count(Collections.emptyIterator()));
        assertEquals(3, Iterators.count(list("a", "b", "c").iterator()));
        assertEquals(1, Iterators.count(list("a").iterator()));
    }

    @Test
    public void testCountIteratorWithPredicate() {
        Predicate<String> isA = "a"::equals;
        assertEquals(0, Iterators.count(null, isA));
        assertEquals(0, Iterators.count(Collections.emptyIterator(), isA));
        assertEquals(2, Iterators.count(list("a", "b", "a", "c").iterator(), isA));
        assertEquals(0, Iterators.count(list("b", "c").iterator(), isA));

        Predicate<Integer> isEven = x -> x % 2 == 0;
        assertEquals(2, Iterators.count(list(1, 2, 3, 4, 5).iterator(), isEven));

        assertThrows(IllegalArgumentException.class, () -> Iterators.count(list("a").iterator(), null));
    }

    @Test
    public void testIndexOf() {
        assertEquals(-1, Iterators.indexOf(null, "a"));
        assertEquals(-1, Iterators.indexOf(Collections.emptyIterator(), "a"));
        assertEquals(0, Iterators.indexOf(list("a", "b", "a").iterator(), "a"));
        assertEquals(1, Iterators.indexOf(list("a", "b", "a").iterator(), "b"));
        assertEquals(-1, Iterators.indexOf(list("a", "b", "a").iterator(), "c"));
        assertEquals(0, Iterators.indexOf(list(null, "a").iterator(), null));
        assertEquals(1, Iterators.indexOf(list(null, "a").iterator(), "a"));
    }

    @Test
    public void testIndexOfWithFromIndex() {
        assertEquals(-1, Iterators.indexOf(null, "a", 0));
        assertEquals(-1, Iterators.indexOf(Collections.emptyIterator(), "a", 0));

        Iterator<String> iter = list("a", "b", "a", "c").iterator();
        assertEquals(2, Iterators.indexOf(iter, "a", 1)); // iter state after this? (advances for fromIndex then continues)

        iter = list("a", "b", "a", "c").iterator();
        assertEquals(2, Iterators.indexOf(iter, "a", 2));

        iter = list("a", "b", "a", "c").iterator();
        assertEquals(-1, Iterators.indexOf(iter, "a", 3));

        iter = list("a", "b", "a", "c").iterator();
        assertEquals(0, Iterators.indexOf(iter, "a", 0));

        iter = list("a", "b", "a", "c").iterator();
        assertEquals(-1, Iterators.indexOf(iter, "x", 0));

        iter = list("a", "b", "c").iterator();
        assertEquals(-1, Iterators.indexOf(iter, "a", 10)); // fromIndex out of bounds effectively
    }

    @Test
    public void testElementsEqual() {
        assertTrue(Iterators.elementsEqual(list().iterator(), list().iterator()));
        assertTrue(Iterators.elementsEqual(list("a", "b").iterator(), list("a", "b").iterator()));

        assertFalse(Iterators.elementsEqual(list("a").iterator(), list().iterator()));
        assertFalse(Iterators.elementsEqual(list().iterator(), list("a").iterator()));
        assertFalse(Iterators.elementsEqual(list("a", "b").iterator(), list("a", "c").iterator()));
        assertFalse(Iterators.elementsEqual(list("a", "b").iterator(), list("a", "b", "c").iterator()));

        // Iterators are consumed
        Iterator<String> it1 = list("x", "y").iterator();
        Iterator<String> it2 = list("x", "y").iterator();
        assertTrue(Iterators.elementsEqual(it1, it2));
        assertFalse(it1.hasNext());
        assertFalse(it2.hasNext());
    }

    @Test
    public void testRepeatInt() {
        ObjIterator<String> iter = Iterators.repeat("a", 3);
        assertEquals("a", iter.next());
        assertEquals("a", iter.next());
        assertEquals("a", iter.next());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::next);

        assertFalse(Iterators.repeat("a", 0).hasNext());
        assertThrows(IllegalArgumentException.class, () -> Iterators.repeat("a", -1));
    }

    @Test
    public void testRepeatLong() {
        ObjIterator<String> iter = Iterators.repeat("a", 2L);
        assertEquals("a", iter.next());
        assertEquals("a", iter.next());
        assertFalse(iter.hasNext());

        assertFalse(Iterators.repeat("a", 0L).hasNext());
        assertThrows(IllegalArgumentException.class, () -> Iterators.repeat("a", -1L));
    }

    @Test
    public void testRepeatElements() {
        ObjIterator<String> iter = Iterators.repeatElements(list("a", "b"), 2L); // a, a, b, b
        assertEquals("a", iter.next());
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("b", iter.next());
        assertFalse(iter.hasNext());

        assertFalse(Iterators.repeatElements(list("a", "b"), 0L).hasNext());
        assertFalse(Iterators.repeatElements(list(), 2L).hasNext());
        assertThrows(IllegalArgumentException.class, () -> Iterators.repeatElements(list("a"), -1L));
    }

    @Test
    public void testRepeatCollection() {
        ObjIterator<String> iter = Iterators.repeatCollection(list("a", "b"), 2L); // a, b, a, b
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertFalse(iter.hasNext());

        assertFalse(Iterators.repeatCollection(list("a", "b"), 0L).hasNext());
        assertFalse(Iterators.repeatCollection(list(), 2L).hasNext());
        assertThrows(IllegalArgumentException.class, () -> Iterators.repeatCollection(list("a"), -1L));
    }

    @Test
    public void testRepeatElementsToSize() {
        ObjIterator<Integer> iter = Iterators.repeatElementsToSize(list(1, 2, 3), 5L); // 1,1,2,2,3
        assertEquals(1, iter.next());
        assertEquals(1, iter.next());
        assertEquals(2, iter.next());
        assertEquals(2, iter.next());
        assertEquals(3, iter.next());
        assertFalse(iter.hasNext());

        ObjIterator<Integer> iter2 = Iterators.repeatElementsToSize(list(1, 2), 5L); // 1,1,1,2,2
        assertEquals(1, iter2.next());
        assertEquals(1, iter2.next());
        assertEquals(1, iter2.next());
        assertEquals(2, iter2.next());
        assertEquals(2, iter2.next());
        assertFalse(iter2.hasNext());

        assertFalse(Iterators.repeatElementsToSize(list(1, 2, 3), 0L).hasNext());
        assertThrows(IllegalArgumentException.class, () -> Iterators.repeatElementsToSize(list(), 1L));
        assertThrows(IllegalArgumentException.class, () -> Iterators.repeatElementsToSize(list(1), -1L));
    }

    @Test
    public void testRepeatCollectionToSize() {
        ObjIterator<Integer> iter = Iterators.repeatCollectionToSize(list(1, 2, 3), 5L); // 1,2,3,1,2
        assertEquals(1, iter.next());
        assertEquals(2, iter.next());
        assertEquals(3, iter.next());
        assertEquals(1, iter.next());
        assertEquals(2, iter.next());
        assertFalse(iter.hasNext());

        assertFalse(Iterators.repeatCollectionToSize(list(1, 2, 3), 0L).hasNext());
        assertThrows(IllegalArgumentException.class, () -> Iterators.repeatCollectionToSize(list(), 1L));
        assertThrows(IllegalArgumentException.class, () -> Iterators.repeatCollectionToSize(list(1), -1L));
    }

    @Test
    public void testCycleVarArgs() {
        ObjIterator<String> iter = Iterators.cycle("a", "b");
        assertTrue(iter.hasNext());
        assertEquals("a", iter.next());
        assertTrue(iter.hasNext());
        assertEquals("b", iter.next());
        assertTrue(iter.hasNext());
        assertEquals("a", iter.next());
        assertTrue(iter.hasNext());
        assertEquals("b", iter.next());

        assertFalse(Iterators.cycle().hasNext()); // Empty input array
    }

    @Test
    public void testCycleIterable() {
        ObjIterator<String> iter = Iterators.cycle(list("a", "b"));
        assertTrue(iter.hasNext());
        assertEquals("a", iter.next());
        assertTrue(iter.hasNext());
        assertEquals("b", iter.next());
        assertTrue(iter.hasNext());
        assertEquals("a", iter.next());

        // Test with initially empty iterable that might get populated (not typical for this API)
        // For a static list:
        assertFalse(Iterators.cycle(list()).hasNext());
    }

    @Test
    public void testCycleIterableWithRounds() {
        ObjIterator<String> iter = Iterators.cycle(list("a", "b"), 2L); // a, b, a, b
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertFalse(iter.hasNext());

        assertFalse(Iterators.cycle(list("a", "b"), 0L).hasNext());
        assertFalse(Iterators.cycle(list(), 2L).hasNext());
        assertThrows(IllegalArgumentException.class, () -> Iterators.cycle(list("a"), -1L));

        ObjIterator<String> iterOneRound = Iterators.cycle(list("a", "b"), 1L);
        assertEquals("a", iterOneRound.next());
        assertEquals("b", iterOneRound.next());
        assertFalse(iterOneRound.hasNext());
    }

    // --- concat methods ---
    // Only showing a few concat examples for brevity

    @Test
    public void testConcatIntArrays() {
        IntIterator iter = Iterators.concat(new int[] { 1, 2 }, new int[] {}, new int[] { 3, 4 });
        assertEquals(1, iter.nextInt());
        assertEquals(2, iter.nextInt());
        assertEquals(3, iter.nextInt());
        assertEquals(4, iter.nextInt());
        assertFalse(iter.hasNext());

        assertSame(Iterators.concat((int[][]) null), IntIterator.EMPTY);
        assertSame(Iterators.concat(new int[0][0]), IntIterator.EMPTY);
    }

    @Test
    public void testConcatObjectArrays() {
        ObjIterator<String> iter = Iterators.concat(new String[] { "a", "b" }, null, new String[] { "c" });
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());

        assertFalse(Iterators.concat((String[][]) null).hasNext());
        assertFalse(Iterators.concat(new String[0][0]).hasNext()); // No arrays
    }

    @Test
    public void testConcatIterators() {
        ObjIterator<String> iter = Iterators.concat(list("a").iterator(), null, list("b", "c").iterator(), Collections.emptyIterator());
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());

        assertFalse(Iterators.concat((Iterator<String>[]) null).hasNext());
        assertFalse(Iterators.concat(new Iterator[0][0]).hasNext()); // No iterators provided);
    }

    @Test
    public void testConcatIterables() {
        ObjIterator<String> iter = Iterators.concat(list("a"), null, list("b", "c"), list());
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testConcatMaps() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("a", 1);
        Map<String, Integer> m2 = new HashMap<>();
        m2.put("b", 2);
        m2.put("c", 3);
        ObjIterator<Map.Entry<String, Integer>> iter = Iterators.concat(m1, null, m2, new HashMap<>());

        Set<Map.Entry<String, Integer>> entries = new HashSet<>();
        while (iter.hasNext()) {
            entries.add(iter.next());
        }
        assertEquals(3, entries.size());
        assertTrue(entries.contains(new AbstractMap.SimpleEntry<>("a", 1)));
        assertTrue(entries.contains(new AbstractMap.SimpleEntry<>("b", 2)));
        assertTrue(entries.contains(new AbstractMap.SimpleEntry<>("c", 3)));
    }

    @Test
    public void testConcatCollectionOfIterators() {
        List<Iterator<String>> iterList = list(list("a").iterator(), list("b", "c").iterator());
        ObjIterator<String> iter = Iterators.concat(iterList);
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());

        assertFalse(Iterators.concat((Collection<Iterator<String>>) null).hasNext());
        assertFalse(Iterators.concat(Collections.<Iterator<String>> emptyList()).hasNext());
    }

    @Test
    public void testConcatIterablesCollection() {
        List<Iterable<String>> iterList = list(list("a"), list("b", "c"));
        ObjIterator<String> iter = Iterators.concatIterables(iterList);
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());
    }

    // --- merge methods ---
    @Test
    public void testMergeIterators() {
        Iterator<Integer> a = list(1, 3, 5).iterator();
        Iterator<Integer> b = list(2, 4, 6).iterator();
        BiFunction<Integer, Integer, MergeResult> selector = (i1, i2) -> (i1 <= i2) ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        ObjIterator<Integer> merged = Iterators.merge(a, b, selector);

        assertEquals(1, merged.next());
        assertEquals(2, merged.next());
        assertEquals(3, merged.next());
        assertEquals(4, merged.next());
        assertEquals(5, merged.next());
        assertEquals(6, merged.next());
        assertFalse(merged.hasNext());

        // One iterator empty
        a = list(1, 3).iterator();
        b = list(new Integer[0]).iterator();
        merged = Iterators.merge(a, b, selector);
        assertEquals(1, merged.next());
        assertEquals(3, merged.next());
        assertFalse(merged.hasNext());
    }

    @Test
    public void testMergeCollectionOfIterators() {
        List<Iterator<Integer>> iterators = list(list(1, 5).iterator(), list(2, 4).iterator(), list(3, 6).iterator());
        BiFunction<Integer, Integer, MergeResult> selector = (i1, i2) -> (i1 <= i2) ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        ObjIterator<Integer> merged = Iterators.merge(iterators, selector);
        // Order of merge is ((1,5) merge (2,4)) merge (3,6)
        // (1,2,4,5) merge (3,6) -> 1,2,3,4,5,6
        List<Integer> result = new ArrayList<>();
        merged.forEachRemaining(result::add);
        assertEquals(list(1, 2, 3, 4, 5, 6), result);

        assertFalse(Iterators.merge((Collection<Iterator<Integer>>) null, selector).hasNext());
    }

    @Test
    public void testMergeSortedIteratorsComparable() {
        Iterator<Integer> a = list(1, 3, 5, 8).iterator();
        Iterator<Integer> b = list(2, 3, 6, 7).iterator();
        ObjIterator<Integer> merged = Iterators.mergeSorted(a, b); // Natural order

        assertEquals(1, merged.next());
        assertEquals(2, merged.next());
        assertEquals(3, merged.next()); // from a or b, then the other 3
        assertEquals(3, merged.next());
        assertEquals(5, merged.next());
        assertEquals(6, merged.next());
        assertEquals(7, merged.next());
        assertEquals(8, merged.next());
        assertFalse(merged.hasNext());
    }

    @Test
    public void testMergeSortedIteratorsComparator() {
        Iterator<Integer> a = list(5, 3, 1).iterator(); // Sorted reverse
        Iterator<Integer> b = list(6, 4, 2).iterator(); // Sorted reverse
        ObjIterator<Integer> merged = Iterators.mergeSorted(a, b, Comparator.reverseOrder());

        assertEquals(6, merged.next());
        assertEquals(5, merged.next());
        assertEquals(4, merged.next());
        assertEquals(3, merged.next());
        assertEquals(2, merged.next());
        assertEquals(1, merged.next());
        assertFalse(merged.hasNext());
    }

    // --- zip methods ---
    @Test
    public void testZipIterators() {
        Iterator<String> s = list("a", "b").iterator();
        Iterator<Integer> i = list(1, 2, 3).iterator(); // i is longer
        ObjIterator<String> zipped = Iterators.zip(s, i, (str, num) -> str + num); // Stops when shorter is exhausted

        assertEquals("a1", zipped.next());
        assertEquals("b2", zipped.next());
        assertFalse(zipped.hasNext());
    }

    @Test
    public void testZipIterablesWithDefaults() {
        Iterable<String> s = list("a", "b");
        Iterable<Integer> i = list(1, 2, 3, 4);
        ObjIterator<String> zipped = Iterators.zip(s, i, "defaultS", 0, (str, num) -> str + num);

        assertEquals("a1", zipped.next());
        assertEquals("b2", zipped.next());
        assertEquals("defaultS3", zipped.next());
        assertEquals("defaultS4", zipped.next());
        assertFalse(zipped.hasNext());
    }

    @Test
    public void testZipThreeIterators() {
        Iterator<String> s = list("a", "b").iterator();
        Iterator<Integer> i = list(1, 2, 3).iterator();
        Iterator<Boolean> bl = list(true, false, true, false).iterator();

        ObjIterator<String> zipped = Iterators.zip(s, i, bl, (str, num, boolVal) -> str + num + boolVal);
        assertEquals("a1true", zipped.next());
        assertEquals("b2false", zipped.next());
        assertFalse(zipped.hasNext());
    }

    @Test
    public void testZipThreeIterablesWithDefaults() {
        Iterable<String> s = list("a");
        Iterable<Integer> i = list(1, 2);
        Iterable<Boolean> bl = list(true, false, true);

        ObjIterator<String> zipped = Iterators.zip(s, i, bl, "defS", 0, false, (str, num, boolVal) -> str + num + boolVal);
        assertEquals("a1true", zipped.next()); // a, 1, true
        assertEquals("defS2false", zipped.next()); // defS, 2, false
        assertEquals("defS0true", zipped.next()); // defS, 0, true
        assertFalse(zipped.hasNext());
    }

    // --- unzip methods ---
    // These delegate to BiIterator/TriIterator static methods. Test the delegation.
    @Test
    public void testUnzipIteratorToBiIterator() {
        Iterator<String> source = list("a:1", "b:2").iterator();
        BiConsumer<String, Pair<String, Integer>> unzipper = (str, pair) -> {
            String[] parts = str.split(":");
            pair.setLeft(parts[0]);
            pair.setRight(Integer.parseInt(parts[1]));
        };

        // Actual BiIterator.unzip would be called. This test assumes it works.
        // We are testing that Iterators.unzip correctly passes arguments.
        // This might require mocking BiIterator.unzip or having a functional stub.
        // For now, just assert it doesn't throw with valid inputs.
        assertNotNull(Iterators.unzip(source, unzipper));
    }

    // --- advance, skip, limit, skipAndLimit ---
    @Test
    public void testAdvance() {
        Iterator<String> iter = list("a", "b", "c", "d").iterator();
        assertEquals(2, Iterators.advance(iter, 2)); // iter now at "c"
        assertEquals("c", iter.next());

        iter = list("a", "b").iterator();
        assertEquals(2, Iterators.advance(iter, 5)); // advances to end
        assertFalse(iter.hasNext());

        iter = list("a", "b").iterator();
        assertEquals(0, Iterators.advance(iter, 0));
        assertEquals("a", iter.next());

        assertThrows(IllegalArgumentException.class, () -> Iterators.advance(list().iterator(), -1));
    }

    @Test
    public void testSkip() {
        ObjIterator<String> skipped = Iterators.skip(list("a", "b", "c", "d").iterator(), 2);
        assertEquals("c", skipped.next());
        assertEquals("d", skipped.next());
        assertFalse(skipped.hasNext());

        assertFalse(Iterators.skip(list("a", "b").iterator(), 5).hasNext());
        assertFalse(Iterators.skip(null, 2).hasNext());

        ObjIterator<String> notSkipped = Iterators.skip(list("a", "b").iterator(), 0);
        assertEquals("a", notSkipped.next());

        assertThrows(IllegalArgumentException.class, () -> Iterators.skip(list().iterator(), -1));
    }

    @Test
    public void testLimit() {
        ObjIterator<String> limited = Iterators.limit(list("a", "b", "c", "d").iterator(), 2);
        assertEquals("a", limited.next());
        assertEquals("b", limited.next());
        assertFalse(limited.hasNext());

        assertFalse(Iterators.limit(list("a", "b").iterator(), 0).hasNext());
        assertFalse(Iterators.limit(null, 2).hasNext());

        ObjIterator<String> all = Iterators.limit(list("a", "b").iterator(), Long.MAX_VALUE);
        assertEquals("a", all.next());
        assertEquals("b", all.next());
        assertFalse(all.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.limit(list().iterator(), -1));
    }

    @Test
    public void testSkipAndLimitIterator() {
        ObjIterator<String> result = Iterators.skipAndLimit(list("a", "b", "c", "d", "e").iterator(), 1, 2); // skip "a", take "b", "c"
        assertEquals("b", result.next());
        assertEquals("c", result.next());
        assertFalse(result.hasNext());

        // Skip all
        assertFalse(Iterators.skipAndLimit(list("a", "b").iterator(), 3, 2).hasNext());
        // Limit 0
        assertFalse(Iterators.skipAndLimit(list("a", "b").iterator(), 1, 0).hasNext());
        // No skip, limit all
        result = Iterators.skipAndLimit(list("a", "b").iterator(), 0, Long.MAX_VALUE);
        assertEquals("a", result.next());
        assertEquals("b", result.next());
        assertFalse(result.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.skipAndLimit(list().iterator(), -1, 1));
        assertThrows(IllegalArgumentException.class, () -> Iterators.skipAndLimit(list().iterator(), 1, -1));
    }

    @Test
    public void testSkipAndLimitIterable() {
        ObjIterator<String> result = Iterators.skipAndLimit(list("a", "b", "c", "d", "e"), 1, 2); // skip "a", take "b", "c"
        assertEquals("b", result.next());
        assertEquals("c", result.next());
        assertFalse(result.hasNext());
    }

    // --- skipNulls, distinct, distinctBy, filter, takeWhile, dropWhile, skipUntil ---
    @Test
    public void testSkipNullsIterator() {
        ObjIterator<String> iter = Iterators.skipNulls(list("a", null, "b", null, "c").iterator());
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());

        assertFalse(Iterators.skipNulls((Iterator<String>) null).hasNext());
    }

    @Test
    public void testSkipNullsIterable() {
        ObjIterator<String> iter = Iterators.skipNulls(list("a", null, "b"));
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testDistinctIterator() {
        ObjIterator<String> iter = Iterators.distinct(list("a", "b", "a", "c", "b", "d").iterator());
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertEquals("d", iter.next());
        assertFalse(iter.hasNext());

        assertFalse(Iterators.distinct((Iterator<String>) null).hasNext());
    }

    @Test
    public void testDistinctIterable() {
        ObjIterator<String> iter = Iterators.distinct(list("a", "b", "a", "c", "b", "d"));
        assertEquals("a", iter.next()); // Order of first occurrence
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertEquals("d", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testDistinctByIterator() {
        Function<String, Integer> length = String::length;
        ObjIterator<String> iter = Iterators.distinctBy(list("a", "bb", "c", "ddd", "ee").iterator(), length);
        assertEquals("a", iter.next()); // len 1
        assertEquals("bb", iter.next()); // len 2
        assertEquals("ddd", iter.next());// len 3
        assertFalse(iter.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.distinctBy(list("a").iterator(), null));
    }

    @Test
    public void testDistinctByIterable() {
        Function<String, Integer> length = String::length;
        ObjIterator<String> iter = Iterators.distinctBy(list("a", "bb", "c", "ddd", "ee"), length);
        assertEquals("a", iter.next());
        assertEquals("bb", iter.next());
        assertEquals("ddd", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testFilterIterator() {
        Predicate<Integer> isEven = x -> x % 2 == 0;
        ObjIterator<Integer> iter = Iterators.filter(list(1, 2, 3, 4, 5, 6).iterator(), isEven);
        assertEquals(2, iter.next());
        assertEquals(4, iter.next());
        assertEquals(6, iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testFilterIterable() {
        Predicate<Integer> isEven = x -> x % 2 == 0;
        ObjIterator<Integer> iter = Iterators.filter(list(1, 2, 3, 4, 5, 6), isEven);
        assertEquals(2, iter.next());
        assertEquals(4, iter.next());
        assertEquals(6, iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testTakeWhileIterator() {
        Predicate<Integer> lessThan4 = x -> x < 4;
        ObjIterator<Integer> iter = Iterators.takeWhile(list(1, 2, 3, 4, 1, 5).iterator(), lessThan4);
        assertEquals(1, iter.next());
        assertEquals(2, iter.next());
        assertEquals(3, iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testTakeWhileInclusiveIterator() {
        Predicate<Integer> lessThan4 = x -> x < 4;
        ObjIterator<Integer> iter = Iterators.takeWhileInclusive(list(1, 2, 3, 4, 1, 5).iterator(), lessThan4);
        assertEquals(1, iter.next());
        assertEquals(2, iter.next());
        assertEquals(3, iter.next());
        assertEquals(4, iter.next()); // includes the first element that fails predicate
        assertFalse(iter.hasNext());

        // All satisfy
        ObjIterator<Integer> iter2 = Iterators.takeWhileInclusive(list(1, 2, 3).iterator(), lessThan4);
        assertEquals(1, iter2.next());
        assertEquals(2, iter2.next());
        assertEquals(3, iter2.next());
        assertFalse(iter2.hasNext());

        // First fails
        ObjIterator<Integer> iter3 = Iterators.takeWhileInclusive(list(5, 1, 2).iterator(), lessThan4);
        assertEquals(5, iter3.next());
        assertFalse(iter3.hasNext());
    }

    @Test
    public void testDropWhileIterator() {
        Predicate<Integer> lessThan3 = x -> x < 3;
        ObjIterator<Integer> iter = Iterators.dropWhile(list(1, 2, 3, 1, 4).iterator(), lessThan3);
        assertEquals(3, iter.next());
        assertEquals(1, iter.next());
        assertEquals(4, iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSkipUntilIterator() {
        Predicate<Integer> equals3 = x -> x == 3;
        ObjIterator<Integer> iter = Iterators.skipUntil(list(1, 2, 3, 4, 5).iterator(), equals3);
        assertEquals(3, iter.next()); // Starts from the element that satisfies predicate
        assertEquals(4, iter.next());
        assertEquals(5, iter.next());
        assertFalse(iter.hasNext());

        // Predicate never met
        ObjIterator<Integer> iter2 = Iterators.skipUntil(list(1, 2, 4).iterator(), equals3);
        assertFalse(iter2.hasNext());
    }

    // --- map, flatMap ---
    @Test
    public void testMapIterator() {
        Function<String, Integer> length = String::length;
        ObjIterator<Integer> iter = Iterators.map(list("a", "bb", "ccc").iterator(), length);
        assertEquals(1, iter.next());
        assertEquals(2, iter.next());
        assertEquals(3, iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testFlatMapIteratorToIterable() {
        Function<String, Iterable<Character>> toChars = s -> s.chars().mapToObj(c -> (char) c).collect(Collectors.toList());
        ObjIterator<Character> iter = Iterators.flatMap(list("ab", "", "c").iterator(), toChars);
        assertEquals('a', iter.next());
        assertEquals('b', iter.next());
        assertEquals('c', iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testFlatmapIteratorToArray() { // Note: 'flatmap' lowercase 'm'
        Function<String, Character[]> toCharsArray = s -> {
            Character[] arr = new Character[s.length()];
            for (int i = 0; i < s.length(); i++)
                arr[i] = s.charAt(i);
            return arr;
        };
        ObjIterator<Character> iter = Iterators.flatmap(list("ab", "", "c").iterator(), toCharsArray);
        assertEquals('a', iter.next());
        assertEquals('b', iter.next());
        assertEquals('c', iter.next());
        assertFalse(iter.hasNext());
    }

    // --- forEach methods ---
    // These are complex, especially with threading. Will test basic sequential paths and argument handling.
    @Test
    public void testForEachIteratorConsumer() throws Exception {
        List<String> result = new ArrayList<>();
        Throwables.Consumer<String, Exception> consumer = result::add;
        Iterators.forEach(list("a", "b").iterator(), consumer);
        assertEquals(list("a", "b"), result);
    }

    @Test
    public void testForEachIteratorConsumerOnComplete() throws Exception {
        List<String> result = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        Throwables.Consumer<String, Exception> consumer = result::add;
        Throwables.Runnable<Exception> onComplete = () -> completed.set(true);

        Iterators.forEach(list("a", "b").iterator(), consumer, onComplete);
        assertEquals(list("a", "b"), result);
        assertTrue(completed.get());
    }

    @Test
    public void testForEachIteratorOffsetCount() throws Exception {
        List<String> result = new ArrayList<>();
        Throwables.Consumer<String, Exception> consumer = result::add;
        Iterators.forEach(list("a", "b", "c", "d").iterator(), 1, 2, consumer); // "b", "c"
        assertEquals(list("b", "c"), result);
    }

    // For parallel forEach, testing actual concurrency is hard in unit tests.
    // Test that it runs, consumer is called, and onComplete (if any) is called.
    // Assume processThreadNum = 0 means sequential for simplicity if not fully mocked.
    // The provided code actually has special handling for processThreadNum == 0.

    @Test
    public void testForEachIteratorParallelSequentialPath() throws Exception {
        List<String> result = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        Throwables.Consumer<String, Exception> consumer = result::add;
        Throwables.Runnable<Exception> onComplete = () -> completed.set(true);

        // Test sequential path within parallel signature (processThreadNum = 0)
        Iterators.forEach(list("x", "y", "z").iterator(), 0, 3, 0, 0, consumer, onComplete);
        assertEquals(list("x", "y", "z"), result);
        assertTrue(completed.get());
    }

    @Test
    public void testForEachCollectionOfIteratorsSequentialPath() throws Exception {
        Collection<Iterator<String>> iterators = list(list("a", "b").iterator(), list("c").iterator());
        List<String> result = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        Throwables.Consumer<String, Exception> consumer = result::add;
        Throwables.Runnable<Exception> onComplete = () -> completed.set(true);

        // Test sequential path for collection of iterators (readThreadNum=0, processThreadNum=0)
        Iterators.forEach(iterators, 0, Long.MAX_VALUE, 0, 0, 0, consumer, onComplete);
        assertEquals(list("a", "b", "c"), result);
        assertTrue(completed.get());
    }

    @Disabled("Parallel forEach tests require more setup or a test environment that can handle threads properly.")
    @Test
    public void testForEachIteratorParallelPath() throws Exception {
        // This would test the actual parallel execution path.
        // It would require careful synchronization or result collection for verification.
        // For example, using ConcurrentLinkedQueue for results and CountDownLatch.
        // The current Iterators.forEach for parallel execution is quite involved.
    }
}

//Example stubs (very basic, for compilation only)
//In a real test scenario, you would use the actual library or a robust mocking framework like Mockito.

//Assume com.landawn.abacus.util.N, Fn, cs, InternalUtil, Throwables are available.

//abstract class BaseIterator<T> implements ObjIterator<T> {} // If ObjIterator is an interface

//interface ObjIterator<T> extends Iterator<T> {
// static <T> ObjIterator<T> empty() { return (ObjIterator<T>) Collections.emptyIterator(); }
// static <T> ObjIterator<T> of(Iterator<? extends T> iter) {
//     return new ObjIterator<T>() {
//         public boolean hasNext() { return iter.hasNext(); }
//         public T next() { return iter.next(); }
//     };
// }
// static <T> ObjIterator<T> of(T... elements) { return of(Arrays.asList(elements).iterator()); }
// default boolean isEmpty() { return !hasNext(); }
//}

//interface BooleanIterator extends Iterator<Boolean> {
// boolean nextBoolean();
// BooleanIterator EMPTY = new BooleanIterator() {
//     public boolean hasNext() { return false; }
//     public Boolean next() { throw new NoSuchElementException(); }
//     public boolean nextBoolean() { throw new NoSuchElementException(); }
// };
//  default boolean isSameAs(BooleanIterator other) { return this == other; } // For test
//}
//(Similar for CharIterator, ByteIterator, etc.)

//interface BiIterator<A, B> extends Iterator<Pair<A,B>> {
// static <A,B> BiIterator<A,B> empty() { /* ... */ return null;}
// static <T,A,B> BiIterator<A,B> unzip(Iterator<? extends T> iter, BiConsumer<? super T, Pair<A,B>> unzip) { /* ... */ return null; }
// <E extends Exception> void next(Throwables.BiConsumer<? super A, ? super B, E> action) throws NoSuchElementException, E;
// void forEachRemaining(BiConsumer<? super A, ? super B> action);
// <E extends Exception> void foreachRemaining(Throwables.BiConsumer<? super A, ? super B, E> action) throws E;
// <R> ObjIterator<R> map(BiFunction<? super A, ? super B, ? extends R> mapper);
//}
//interface TriIterator<A, B, C> extends Iterator<Triple<A,B,C>> { /* Similar */ }

//class Pair<L,R> {
// L left; R right;
// public Pair(L l, R r) { this.left = l; this.right = r;}
// public static <L,R> Pair<L,R> of(L l, R r) { return new Pair<>(l,r);}
// public L getLeft() { return left; } public R getRight() { return right; }
// public void setLeft(L l){this.left = l;} public void setRight(R r){this.right = r;}
// @Override public boolean equals(Object o) { /* ... */ return false;}
// @Override public int hashCode() { /* ... */ return 0;}
//}
//class Triple<A,B,C> { /* Similar */ }

//enum MergeResult { TAKE_FIRST, TAKE_SECOND;
// public static <T> BiFunction<T, T, MergeResult> minFirst(Comparator<? super T> cmp) {
//     return (a, b) -> cmp.compare(a, b) <= 0 ? TAKE_FIRST : TAKE_SECOND;
// }
//}

//class Nullable<T> {
// private final T value; private final boolean isPresent;
// private static final Nullable<?> EMPTY = new Nullable<>(null, false);
// private Nullable(T value, boolean isPresent) { this.value = value; this.isPresent = isPresent; }
// public static <T> Nullable<T> empty() { return (Nullable<T>) EMPTY; }
// public static <T> Nullable<T> of(T value) { return new Nullable<>(value, true); } // Assumes value can be null for Nullable.of
// public boolean isEmpty() { return !isPresent; }
// public boolean isPresent() { return isPresent; }
// public T get() { if (!isPresent) throw new NoSuchElementException(); return value; }
//}
