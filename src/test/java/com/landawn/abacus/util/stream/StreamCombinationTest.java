package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.ShortIterator;
import com.landawn.abacus.util.function.ByteBiFunction;
import com.landawn.abacus.util.function.ByteNFunction;
import com.landawn.abacus.util.function.ByteTriFunction;
import com.landawn.abacus.util.function.CharBiFunction;
import com.landawn.abacus.util.function.CharNFunction;
import com.landawn.abacus.util.function.CharTriFunction;
import com.landawn.abacus.util.function.DoubleBiFunction;
import com.landawn.abacus.util.function.DoubleNFunction;
import com.landawn.abacus.util.function.DoubleTriFunction;
import com.landawn.abacus.util.function.FloatBiFunction;
import com.landawn.abacus.util.function.FloatNFunction;
import com.landawn.abacus.util.function.FloatTriFunction;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntBiFunction;
import com.landawn.abacus.util.function.IntNFunction;
import com.landawn.abacus.util.function.IntTriFunction;
import com.landawn.abacus.util.function.LongBiFunction;
import com.landawn.abacus.util.function.LongNFunction;
import com.landawn.abacus.util.function.LongTriFunction;
import com.landawn.abacus.util.function.ShortBiFunction;
import com.landawn.abacus.util.function.ShortNFunction;
import com.landawn.abacus.util.function.ShortTriFunction;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u;

/** Stream combination contracts. */
public class StreamCombinationTest extends StreamTestSupport {

    // append

    @Test
    public void testAppend() {
        Stream<Integer> appended = stream.append(Arrays.asList(6, 7, 8));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8), appended.toList());

        Stream<Integer> appendedVarargs = createStream(1, 2, 3).append(4, 5);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), appendedVarargs.toList());
    }

    @Test
    public void testAppend_Empty() {
        List<Integer> result = Stream.of(1, 2, 3).append(Collections.emptyList()).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testAppendCollection() {
        Stream<Integer> stream = Stream.of(1, 2).append(Arrays.asList(3, 4));
        assertEquals(Arrays.asList(1, 2, 3, 4), stream.toList());
    }

    @Test
    public void testAppendOptional() {
        Stream<Integer> stream = Stream.of(1, 2).append(u.Optional.of(3));
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testAppendStream() {
        Stream<Integer> stream = Stream.of(1, 2).append(Stream.of(3, 4));
        assertEquals(Arrays.asList(1, 2, 3, 4), stream.toList());
    }

    @Test
    public void testAppendVarargs() {
        Stream<Integer> stream = Stream.of(1, 2).append(3, 4);
        assertEquals(Arrays.asList(1, 2, 3, 4), stream.toList());
    }

    @Test
    public void testAppendIfEmpty() {
        Stream<Integer> stream1 = Stream.<Integer> empty().appendIfEmpty(() -> Stream.of(1, 2));
        assertEquals(Arrays.asList(1, 2), stream1.toList());

        Stream<Integer> stream2 = Stream.of(1, 2).appendIfEmpty(() -> Stream.of(3, 4));
        assertEquals(Arrays.asList(1, 2), stream2.toList());
    }

    // cartesianProduct

    @Test
    public void testCartesianProduct() {
        List<List<Integer>> result = Stream.of(1, 2).cartesianProduct(Arrays.asList(3, 4)).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(Arrays.asList(1, 3)));
        assertTrue(result.contains(Arrays.asList(1, 4)));
        assertTrue(result.contains(Arrays.asList(2, 3)));
        assertTrue(result.contains(Arrays.asList(2, 4)));
    }



    // Stream review 2026-09-09 (pass B) - cartesianProduct null-array handling

    /**
     * {@code cartesianProduct(Collection...)} used {@code Arrays.asList(cs)}, which NPEs on a null array, while its
     * neighbours and {@code Iterables.cartesianProduct(Collection...)} - the method its own {@code @see} points at -
     * use the null-safe {@code Array.asList}.
     */
    @Test
    public void testCartesianProduct_nullArrayIsTreatedAsNoCollections() {
        final List<List<Integer>> expected = Stream.of(1, 2, 3).cartesianProduct().toList();

        assertEquals(expected, Stream.of(1, 2, 3).cartesianProduct((Collection<Integer>[]) null).toList());

        // the ordinary path is unaffected
        assertEquals(6, Stream.of(1, 2, 3).cartesianProduct(N.asList(9, 8)).count());
    }

    @Test
    public void testCartesianProductCollectionOfCollections() {
        List<List<String>> result = Stream.of("a", "b").cartesianProduct(Arrays.asList(Arrays.asList("1", "2"))).toList();
        assertEquals(Arrays.asList(Arrays.asList("a", "1"), Arrays.asList("a", "2"), Arrays.asList("b", "1"), Arrays.asList("b", "2")), result);

        List<List<String>> result2 = Stream.of("a", "b").cartesianProduct(Arrays.asList(Arrays.asList("1", "2"), Arrays.asList("true", "false"))).toList();
        assertEquals(Arrays.asList(Arrays.asList("a", "1", "true"), Arrays.asList("a", "1", "false"), Arrays.asList("a", "2", "true"),
                Arrays.asList("a", "2", "false"), Arrays.asList("b", "1", "true"), Arrays.asList("b", "1", "false"), Arrays.asList("b", "2", "true"),
                Arrays.asList("b", "2", "false")), result2);
    }

    @Test
    public void testCartesianProductVarargs() {
        List<List<Integer>> result = Stream.of(0, 1).cartesianProduct(Arrays.asList(2, 3)).toList();
        assertEquals(Arrays.asList(Arrays.asList(0, 2), Arrays.asList(0, 3), Arrays.asList(1, 2), Arrays.asList(1, 3)), result);
    }

    // combinations

    @Test
    public void testCombinations() {
        List<List<Integer>> result = Stream.of(1, 2, 3).combinations(2).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains(Arrays.asList(1, 2)));
        assertTrue(result.contains(Arrays.asList(1, 3)));
        assertTrue(result.contains(Arrays.asList(2, 3)));
    }

    @Test
    public void testCombinations_WithLen() {
        List<List<Integer>> result = Stream.of(1, 2, 3).combinations(2).toList();
        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1, 2), result.get(0));
        assertEquals(Arrays.asList(1, 3), result.get(1));
        assertEquals(Arrays.asList(2, 3), result.get(2));
    }

    @Test
    public void testCombinations_Zero() {
        List<List<Integer>> result = Stream.of(1, 2, 3).combinations(0).toList();
        assertEquals(1, result.size());
        assertTrue(result.get(0).isEmpty());
    }

    @Test
    public void testCombinationsAll() {
        List<List<Integer>> result = Stream.of(1, 2).combinations().toList();
        assertTrue(result.size() >= 3);
    }

    @Test
    public void testCombinationsLengthAboveSizeThrowsIndexOutOfBounds() {
        assertThrows(IndexOutOfBoundsException.class, () -> Stream.of(1, 2, 3).combinations(4));
        assertThrows(IndexOutOfBoundsException.class, () -> Stream.of(Arrays.asList(1, 2, 3).iterator()).combinations(4));

        final List<List<Integer>> all = Stream.of(1, 2, 3).combinations(3).toList();
        assertEquals(Arrays.asList(Arrays.asList(1, 2, 3)), all);
    }

    @Test
    public void testCombinationsNegativeLengthThrowsIllegalArgument() {
        // Array-backed, iterator-backed and parallel streams all reject a negative length with IllegalArgumentException.
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1, 2, 3).combinations(-1));
        assertThrows(IllegalArgumentException.class, () -> Stream.of(Arrays.asList(1, 2, 3).iterator()).combinations(-1));
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1, 2, 3).parallel().combinations(-1));
    }

    @Test
    public void testCombinationsNoLength() {
        List<List<Integer>> combs = Stream.of(1, 2).combinations().toList();
        assertEquals(4, combs.size());
        assertTrue(combs.contains(Collections.emptyList()));
        assertTrue(combs.contains(Arrays.asList(1)));
        assertTrue(combs.contains(Arrays.asList(2)));
        assertTrue(combs.contains(Arrays.asList(1, 2)));
    }

    @Test
    public void testCombinationsWithLength() {
        List<List<Integer>> combs = Stream.of(1, 2, 3).combinations(2).toList();
        assertEquals(3, combs.size());
        assertTrue(combs.contains(Arrays.asList(1, 2)));
        assertTrue(combs.contains(Arrays.asList(1, 3)));
        assertTrue(combs.contains(Arrays.asList(2, 3)));
    }

    @Test
    public void testCombinationsWithLengthAndRepeat() {
        List<List<Integer>> combsWithoutRepeat = Stream.of(1, 2).combinations(2, false).toList();
        assertEquals(Arrays.asList(Arrays.asList(1, 2)), combsWithoutRepeat);

        List<List<Integer>> combsWithRepeat = Stream.of(1, 2).combinations(2, true).toList();
        assertEquals(4, combsWithRepeat.size());
        assertTrue(combsWithRepeat.contains(Arrays.asList(1, 1)));
        assertTrue(combsWithRepeat.contains(Arrays.asList(1, 2)));
        assertTrue(combsWithRepeat.contains(Arrays.asList(2, 1)));
        assertTrue(combsWithRepeat.contains(Arrays.asList(2, 2)));
    }

    // concat

    @Test
    public void testConcat() {
        Stream<Integer> stream = Stream.concat(Stream.of(1, 2), Stream.of(3, 4));
        assertEquals(Arrays.asList(1, 2, 3, 4), stream.toList());
    }

    @Test
    public void testConcat_Arrays() {
        Integer[] arr1 = { 1, 2, 3 };
        Integer[] arr2 = { 4, 5, 6 };
        Integer[] arr3 = { 7, 8, 9 };

        List<Integer> result = Stream.concat(arr1, arr2, arr3).toList();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), result);
    }

    @Test
    public void testConcat_CollectionOfStreams() {
        List<Stream<String>> streams = Arrays.asList(createStream01("a", "b"), createStream01("c", "d"), createStream01("e", "f"));

        List<String> result = Stream.concat(streams).toList();

        assertEquals(Arrays.asList("a", "b", "c", "d", "e", "f"), result);
    }

    @Test
    public void testConcat_CollectionSnapshotPreservesTraversalAndClosing() {
        final AtomicInteger closeCount = new AtomicInteger();
        final List<Stream<Integer>> sources = new ArrayList<>();
        sources.add(createStream(1, 2).onClose(closeCount::incrementAndGet));

        final Stream<Integer> concatenated = Stream.concat(sources);
        sources.clear();

        try (concatenated) {
            assertEquals(Arrays.asList(1, 2), concatenated.toList());
        }

        assertEquals(1, closeCount.get());
    }

    @Test
    public void testConcat_EmptyArrays() {
        Integer[] arr1 = {};
        Integer[] arr2 = {};

        List<Integer> result = Stream.concat(arr1, arr2).toList();

        assertTrue(result.isEmpty());
    }

    @Test
    public void testConcat_Iterables() {
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        Set<Integer> set2 = new HashSet<>(Arrays.asList(4, 5, 6));
        List<Integer> list3 = Arrays.asList(7, 8, 9);

        List<Integer> result = Stream.concat(list1, set2, list3).sorted().toList();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), result);
    }

    @Test
    public void testConcat_Iterators() {
        Iterator<String> iter1 = Arrays.asList("a", "b").iterator();
        Iterator<String> iter2 = Arrays.asList("c", "d").iterator();

        List<String> result = Stream.concat(iter1, iter2).toList();

        assertEquals(Arrays.asList("a", "b", "c", "d"), result);
    }

    @Test
    public void testConcat_MixedTypes() {
        Integer[] arr = { 1, 2 };
        List<Integer> list = Arrays.asList(3, 4);
        Set<Integer> set = new HashSet<>(Arrays.asList(5, 6));

        List<Integer> result = Stream.concat(createStream01(arr), createStream(list), createStream(set)).sorted().toList();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testConcat_Streams() {
        Stream<Integer> stream1 = createStream(1, 2, 3);
        Stream<Integer> stream2 = createStream(4, 5, 6);
        Stream<Integer> stream3 = createStream(7, 8, 9);

        List<Integer> result = Stream.concat(stream1, stream2, stream3).toList();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), result);
    }

    @Test
    public void testConcat_WithNullElements() {
        Stream<String> s1 = createStream01("a", null, "b");
        Stream<String> s2 = createStream01(null, "c", "d");

        List<String> result = Stream.concat(s1, s2).toList();

        assertEquals(Arrays.asList("a", null, "b", null, "c", "d"), result);
    }

    @Test
    public void testConcatArrays() {
        Integer[] arr1 = { 1, 2, 3 };
        Integer[] arr2 = { 4, 5 };
        Integer[] arr3 = { 6, 7, 8 };

        List<Integer> result = Stream.concat(arr1, arr2, arr3).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8), result);

        result = Stream.concat(new Integer[0], arr1, new Integer[0]).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);

        result = Stream.concat(new Integer[0][0]).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testConcatIterablesVarargs_empty_uncovered() {
        @SuppressWarnings("unchecked")
        final Iterable<Integer>[] empty = new Iterable[0];
        assertEquals(0, Stream.concat(empty).count());
    }



    // concat(Iterable...) (lines 19227-19233).
    @Test
    public void testConcatIterablesVarargs_uncovered() {
        final Iterable<Integer> i1 = N.asList(1, 2);
        final Iterable<Integer> i2 = N.asList(3, 4);
        assertEquals(N.asList(1, 2, 3, 4), Stream.concat(i1, i2).toList());
    }

    @Test
    public void testConcatIteratorsVarargs_empty_uncovered() {
        @SuppressWarnings("unchecked")
        final Iterator<Integer>[] empty = new Iterator[0];
        assertEquals(0, Stream.concat(empty).count());
    }



    // concat(Iterator...) (lines 19259-19265).
    @Test
    public void testConcatIteratorsVarargs_uncovered() {
        final Iterator<Integer> it1 = N.asList(1, 2, 3).iterator();
        final Iterator<Integer> it2 = N.asList(4, 5).iterator();
        assertEquals(N.asList(1, 2, 3, 4, 5), Stream.concat(it1, it2).toList());
    }

    @Test
    public void testConcatStreamCollection() {
        List<Stream<Integer>> streams = Arrays.asList(createStream(1, 2), createStream(3, 4), createStream(5, 6));

        List<Integer> result = Stream.concat(streams).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);

        List<Stream<Integer>> emptyStreams = new ArrayList<>();
        result = Stream.concat(emptyStreams).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testConcatStreams() {
        Stream<Integer> stream1 = createStream(1, 2, 3);
        Stream<Integer> stream2 = createStream(4, 5);
        Stream<Integer> stream3 = createStream(6, 7, 8);

        List<Integer> result = Stream.concat(stream1, stream2, stream3).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8), result);
    }

    @Test
    public void testConcatVarargs() {
        Stream<Integer> stream = Stream.concat(Stream.of(1), Stream.of(2), Stream.of(3));
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    // concatIterables

    @Test
    public void testConcatIterables() {
        List<Integer> list1 = Arrays.asList(1, 2);
        List<Integer> list2 = Arrays.asList(3, 4);
        List<Integer> list3 = Arrays.asList(5, 6);

        List<Integer> result = Stream.concatIterables(Arrays.asList(list1, list2, list3)).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testConcatIterables_EmptyIterables() {
        List<List<String>> emptyLists = Arrays.asList(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        List<String> result = Stream.concatIterables(emptyLists).toList();

        assertTrue(result.isEmpty());
    }

    @Test
    public void testConcatIterablesCollection() {
        List<Iterable<Integer>> iterables = Arrays.asList(Arrays.asList(1, 2), new HashSet<>(Arrays.asList(3, 4)), Arrays.asList(5, 6));

        List<Integer> result = Stream.concatIterables(iterables).toList();
        assertEquals(6, result.size());
        assertTrue(result.containsAll(Arrays.asList(1, 2, 3, 4, 5, 6)));
    }

    @Test
    public void testConcatIterablesEmpty() {
        List<Integer> result = Stream.concatIterables(Arrays.<List<Integer>> asList()).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testConcatIterators() {
        Iterator<Integer> iter1 = Arrays.asList(1, 2).iterator();
        Iterator<Integer> iter2 = Arrays.asList(3, 4).iterator();

        List<Integer> result = Stream.concatIterators(Arrays.asList(iter1, iter2)).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testConcatIteratorsCollection() {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2).iterator(), Arrays.asList(3, 4).iterator(), Arrays.asList(5, 6).iterator());

        List<Integer> result = Stream.concatIterators(iterators).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    // crossJoin

    @Test
    public void testCrossJoin() {
        List<Pair<Integer, String>> result = Stream.of(1, 2).crossJoin(Arrays.asList("a", "b")).toList();
        assertEquals(4, result.size());
        assertEquals(Pair.of(1, "a"), result.get(0));
        assertEquals(Pair.of(1, "b"), result.get(1));
        assertEquals(Pair.of(2, "a"), result.get(2));
        assertEquals(Pair.of(2, "b"), result.get(3));
    }

    @Test
    public void testCrossJoin_Empty() {
        List<Pair<String, Integer>> result = Stream.<String> empty().crossJoin(Arrays.asList(1, 2)).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCrossJoin_EmptyCollection() {
        List<Pair<String, Integer>> result = Stream.of("A", "B").crossJoin(Collections.<Integer> emptyList()).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCrossJoin_WithFunction() {
        List<String> result = Stream.of("A", "B").crossJoin(Arrays.asList(1, 2, 3), (str, num) -> str + num).toList();
        assertEquals(Arrays.asList("A1", "A2", "A3", "B1", "B2", "B3"), result);
    }

    @Test
    public void testFullJoin() {
        List<Pair<Integer, Integer>> result = Stream.of(1, 2, 3).fullJoin(Arrays.asList(2, 3, 4), n -> n, n -> n).toList();
        assertEquals(4, result.size());
    }

    // innerJoin

    @Test
    public void testInnerJoin() {
        List<Pair<Integer, Integer>> result = Stream.of(1, 2, 3).innerJoin(Arrays.asList(2, 3, 4), n -> n, n -> n).toList();
        assertEquals(2, result.size());
        assertEquals(Pair.of(2, 2), result.get(0));
        assertEquals(Pair.of(3, 3), result.get(1));
    }

    @Test
    public void testInnerJoin_NoMatch() {
        List<Pair<Integer, Integer>> result = Stream.of(1, 3, 5)
                .innerJoin(Arrays.asList(2, 4, 6), (java.util.function.Function<Integer, Integer>) i -> i,
                        (java.util.function.Function<Integer, Integer>) i -> i)
                .toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testInnerJoin_WithKeyMapper() {
        List<Pair<String, String>> result = Stream.of("apple", "banana")
                .innerJoin(Arrays.asList("ant", "bat", "big"), (java.util.function.Function<String, Character>) s -> s.charAt(0))
                .toList();
        // "apple" matches "ant" (both start with 'a'), "banana" matches "bat" and "big" (start with 'b')
        assertEquals(3, result.size());
    }

    @Test
    public void testInnerJoinEmpty() {
        List<Pair<Integer, Integer>> result = Stream.of(1, 2, 3).innerJoin(Arrays.asList(4, 5, 6), n -> n, n -> n).toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testInnerJoinWithFunction() {
        List<String> result = Stream.of(1, 2, 3).innerJoin(Arrays.asList(2, 3, 4), n -> n, n -> n, (a, b) -> a + "-" + b).toList();
        assertEquals(Arrays.asList("2-2", "3-3"), result);
    }

    @Test
    public void testConcatIterableAndIteratorCollectionsSnapshotMembership() {
        final List<Iterable<Integer>> iterableSources = new ArrayList<>();
        iterableSources.add(Arrays.asList(1, 2));
        final Stream<Integer> iterableConcat = Stream.concatIterables(iterableSources);
        iterableSources.clear();
        assertEquals(Arrays.asList(1, 2), iterableConcat.toList());

        final List<Iterator<Integer>> iteratorSources = new ArrayList<>();
        iteratorSources.add(Arrays.asList(3, 4).iterator());
        final Stream<Integer> iteratorConcat = Stream.concatIterators(iteratorSources);
        iteratorSources.clear();
        assertEquals(Arrays.asList(3, 4), iteratorConcat.toList());
    }

    // join

    @Test
    public void testJoin() {
        assertEquals("1,2,3", Stream.of(1, 2, 3).join(","));
    }

    @Test
    public void testJoinEmpty() {
        assertEquals("", Stream.empty().join(","));
    }

    @Test
    public void testJoinWithPrefixSuffix() {
        assertEquals("[1,2,3]", Stream.of(1, 2, 3).join(",", "[", "]"));
    }

    // joinByRange

    @Test
    public void testJoinByRange() {
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        List<Integer> list2 = Arrays.asList(2, 3, 4);

        List<Pair<Integer, List<Integer>>> result = Stream.of(list1).joinByRange(list2.iterator(), (a, b) -> Math.abs(a - b) <= 1).toList();
        assertFalse(result.isEmpty());
    }

    @Test
    public void testJoinByRangeEmpty() {
        List<Integer> list2 = Arrays.asList(2, 3, 4);

        List<Pair<Integer, List<Integer>>> result = Stream.<Integer> empty().joinByRange(list2.iterator(), (a, b) -> Math.abs(a - b) <= 1).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testJoinByRangeEmptyLeftRoutesUnjoinedElements() {
        // regression: the NONE sentinel was ambiguous between "iterator exhausted" and "left stream
        // empty", so the documented mapperForUnJoinedElements step was silently skipped
        final List<String> result = Stream.<Integer> empty()
                .joinByRange(N.asList(10, 20).iterator(), (t, u) -> u < t, Collectors.toList(), (t, l) -> "joined:" + t + l,
                        rem -> Stream.of(rem).map(u -> "unjoined:" + u))
                .toList();

        assertEquals(N.asList("unjoined:10", "unjoined:20"), result);
    }

    @Test
    public void testLeftJoin() {
        List<Pair<Integer, Integer>> result = Stream.of(1, 2, 3).leftJoin(Arrays.asList(2, 3, 4), n -> n, n -> n).toList();
        assertEquals(3, result.size());
        assertEquals(Pair.of(1, null), result.get(0));
        assertEquals(Pair.of(2, 2), result.get(1));
        assertEquals(Pair.of(3, 3), result.get(2));
    }

    // merge

    @Test
    public void testMerge() {
        Stream<Integer> stream = Stream.merge(Arrays.asList(1, 3, 5), Arrays.asList(2, 4, 6),
                (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), stream.toList());
    }

    @Test
    public void test_merge_streams() {
        Integer[] a = { 1, 3, 5 };
        Integer[] b = { 2, 4, 6 };
        Integer[] c = { 7, 8, 9 };

        assertEquals(N.toList(1, 2, 3, 4, 5, 6),
                Stream.merge(Stream.of(a), Stream.of(b), (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());

        assertEquals(N.toList(1, 2, 3, 4, 5, 6),
                Stream.merge(N.toList(Stream.of(a), Stream.of(b)), (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());

        assertEquals(N.toList(1, 2, 3, 4, 5, 6, 7, 8, 9),
                Stream.merge(Stream.of(a), Stream.of(b), Stream.of(c), (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());

        assertEquals(N.toList(1, 2, 3, 4, 5, 6, 7, 8, 9),
                Stream.merge(N.toList(Stream.of(a), Stream.of(b), Stream.of(c)), (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toList());
        assertEquals(
                N.toList(1, 1, 2, 3, 3, 4, 5, 5, 6, 7, 8, 9), Stream
                        .merge(N.toList(Stream.of(a), Stream.of(b), Stream.of(c), Stream.of(a)),
                                (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toList());

    }

    @Test
    public void testMergeArray() {
        Integer[] a = { 1, 3, 5 };
        Integer[] b = { 2, 4, 6 };
        Integer[] c = { 7, 8, 9 };

        assertEquals(N.toList(1, 2, 3, 4, 5, 6), Stream.merge(a, b, (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6, 7, 8, 9),
                Stream.merge(a, b, c, (e1, e2) -> e1 <= e2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
    }

    @Test
    public void testMergeCollection_empty_uncovered() {
        final List<Stream<Integer>> streams = new ArrayList<>();
        assertEquals(0, Stream.merge(streams, (x, y) -> MergeResult.TAKE_FIRST).count());
    }



    // merge(Collection<Stream>, nextSelector): size==1 (line 26370).
    @Test
    public void testMergeCollection_singleStream_uncovered() {
        final List<Stream<Integer>> streams = N.asList(Stream.of(1, 2, 3));
        assertEquals(N.asList(1, 2, 3), Stream.merge(streams, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
    }



    // merge(Collection<Stream>, nextSelector): size>=3 iterative path (lines 26376-26383).
    @Test
    public void testMergeCollection_threeStreams_uncovered() {
        final List<Stream<Integer>> streams = N.asList(Stream.of(1, 4, 7), Stream.of(2, 5, 8), Stream.of(3, 6, 9));
        assertEquals(N.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), Stream.merge(streams, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
    }



    // merge(Collection<Stream>, nextSelector): size==2 (lines 26371-26373).
    @Test
    public void testMergeCollection_twoStreams_uncovered() {
        final List<Stream<Integer>> streams = N.asList(Stream.of(1, 4, 7), Stream.of(2, 5, 8));
        assertEquals(N.asList(1, 2, 4, 5, 7, 8), Stream.merge(streams, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
    }

    @Test
    public void testMergeCollectionTreatsNullStreamAsEmpty() {
        final List<Stream<Integer>> sources = Collections.singletonList(null);

        assertTrue(Stream.merge(sources, (x, y) -> MergeResult.TAKE_FIRST).toList().isEmpty());
        assertTrue(Stream.parallelMerge(sources, (x, y) -> MergeResult.TAKE_FIRST, 2).toList().isEmpty());
    }

    // mergeIterables

    @Test
    public void testMergeIterables() {
        List<Integer> list1 = Arrays.asList(1, 3, 5);
        List<Integer> list2 = Arrays.asList(2, 4, 6);

        List<Integer> result = Stream.mergeIterables(Arrays.asList(list1, list2), (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }



    // mergeIterables(Collection<Iterable>, nextSelector) (line 26412).
    @Test
    public void testMergeIterables_uncovered() {
        final List<Iterable<Integer>> iterables = N.asList((Iterable<Integer>) N.asList(1, 4, 7), N.asList(2, 5, 8), N.asList(3, 6, 9));
        assertEquals(N.asList(1, 2, 3, 4, 5, 6, 7, 8, 9),
                Stream.mergeIterables(iterables, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
    }

    @Test
    public void testMergeIterablesEmpty() {
        List<Integer> result = Stream.mergeIterables(Arrays.<List<Integer>> asList(), (a, b) -> MergeResult.TAKE_FIRST).toList();
        assertTrue(result.isEmpty());
    }

    // mergeIterators

    @Test
    public void testMergeIterators() {
        Iterator<Integer> iter1 = Arrays.asList(1, 3).iterator();
        Iterator<Integer> iter2 = Arrays.asList(2, 4).iterator();

        List<Integer> result = Stream.mergeIterators(Arrays.asList(iter1, iter2), (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }



    // mergeIterators(Collection<Iterator>, nextSelector): size==1 (line 26446).
    @Test
    public void testMergeIterators_single_uncovered() {
        final List<Iterator<Integer>> iterators = N.asList(N.asList(1, 2, 3).iterator());
        assertEquals(N.asList(1, 2, 3), Stream.mergeIterators(iterators, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
    }



    // mergeIterators(Collection<Iterator>, nextSelector): size==2 (lines 26447-26449).
    @Test
    public void testMergeIterators_two_uncovered() {
        final List<Iterator<Integer>> iterators = N.asList(N.asList(1, 3, 5).iterator(), N.asList(2, 4, 6).iterator());
        assertEquals(N.asList(1, 2, 3, 4, 5, 6),
                Stream.mergeIterators(iterators, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
    }

    // mergeWith

    @Test
    public void testMergeWith() {
        List<Integer> result = Stream.of(1, 3, 5).mergeWith(Stream.of(2, 4, 6), (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testMergeWith_EmptyStreams() {
        List<Integer> result = Stream.<Integer> empty().mergeWith(Collections.emptyList(), (a, b) -> MergeResult.TAKE_FIRST).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMergeWithEmpty() {
        List<Integer> result = Stream.<Integer> empty().mergeWith(Stream.of(1, 2, 3), (a, b) -> MergeResult.TAKE_FIRST).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testPermutations() {
        List<List<Integer>> result = Stream.of(1, 2, 3).permutations().toList();
        assertEquals(6, result.size());
    }

    // prepend

    @Test
    public void testPrepend() {
        Stream<Integer> prepended = stream.prepend(Arrays.asList(-2, -1, 0));
        assertEquals(Arrays.asList(-2, -1, 0, 1, 2, 3, 4, 5), prepended.toList());

        Stream<Integer> prependedVarargs = createStream(3, 4, 5).prepend(1, 2);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), prependedVarargs.toList());
    }

    @Test
    public void testPrepend_Empty() {
        List<Integer> result = Stream.of(1, 2, 3).prepend(Collections.emptyList()).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testPrependCollection() {
        Stream<Integer> stream = Stream.of(3, 4).prepend(Arrays.asList(1, 2));
        assertEquals(Arrays.asList(1, 2, 3, 4), stream.toList());
    }

    @Test
    public void testPrependOptional() {
        Stream<Integer> stream = Stream.of(2, 3).prepend(u.Optional.of(1));
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testPrependStream() {
        Stream<Integer> stream = Stream.of(3, 4).prepend(Stream.of(1, 2));
        assertEquals(Arrays.asList(1, 2, 3, 4), stream.toList());
    }

    @Test
    public void testPrependVarargs() {
        Stream<Integer> stream = Stream.of(3, 4).prepend(1, 2);
        assertEquals(Arrays.asList(1, 2, 3, 4), stream.toList());
    }

    @Test
    public void testRightJoin() {
        List<Pair<Integer, Integer>> result = Stream.of(1, 2, 3).rightJoin(Arrays.asList(2, 3, 4), n -> n, n -> n).toList();
        assertEquals(3, result.size());
        assertEquals(Pair.of(2, 2), result.get(0));
        assertEquals(Pair.of(3, 3), result.get(1));
        assertEquals(Pair.of(null, 4), result.get(2));
    }

    // zip

    @Test
    public void testZip() {
        Stream<String> stream = Stream.zip(Arrays.asList(1, 2, 3), Arrays.asList("a", "b", "c"), (i, s) -> i + s);
        assertEquals(Arrays.asList("1a", "2b", "3c"), stream.toList());
    }

    @Test
    public void testZip_ThreeIterables_WithDefaultValues() {
        // Covers Stream.zip(Iterable, Iterable, Iterable, valueA, valueB, valueC, TriFunction)
        List<Integer> a = Arrays.asList(1, 2, 3);
        List<String> b = Arrays.asList("a", "b");
        List<Double> c = Arrays.asList(10.0, 20.0, 30.0, 40.0);
        List<String> result = Stream.zip(a, b, c, 0, "x", 0.0, (i, s, d) -> i + s + d).toList();
        assertEquals(4, result.size());
        assertEquals("1a10.0", result.get(0));
        assertEquals("2b20.0", result.get(1));
        assertEquals("3x30.0", result.get(2));
        assertEquals("0x40.0", result.get(3));
    }

    @Test
    public void testZipArraysWithValueForNone() {
        String[] a = { "a", "b", "c" };
        Integer[] b = { 1, 2 };

        List<String> result = Stream.zip(a, b, "X", 99, (s, i) -> s + i).toList();
        assertEquals(Arrays.asList("a1", "b2", "c99"), result);
    }

    @Test
    public void testZipByteArrays() {
        byte[] a = { 1, 2, 3 };
        byte[] b = { 10, 20, 30 };

        List<Integer> result = Stream.zip(a, b, (b1, b2) -> (int) (b1 + b2)).toList();
        assertEquals(Arrays.asList(11, 22, 33), result);
    }

    @Test
    public void testZipByteArraysWithBiFunction() {
        byte[] a = { 1, 2, 3 };
        byte[] b = { 4, 5, 6 };

        ByteBiFunction<Integer> zipFunction = (b1, b2) -> b1 + b2;

        List<Integer> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(9), result.get(2));
    }

    @Test
    public void testZipByteArraysWithTriFunction() {
        byte[] a = { 1, 2, 3 };
        byte[] b = { 4, 5, 6 };
        byte[] c = { 7, 8, 9 };

        ByteTriFunction<Integer> zipFunction = (b1, b2, b3) -> b1 + b2 + b3;

        List<Integer> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(15), result.get(1));
        assertEquals(Integer.valueOf(18), result.get(2));
    }

    @Test
    public void testZipByteArraysWithValueForNone() {
        byte[] a = { 1, 2 };
        byte[] b = { 4, 5, 6 };

        ByteBiFunction<Integer> zipFunction = (b1, b2) -> b1 + b2;

        List<Integer> result = Stream.zip(a, b, (byte) 10, (byte) 20, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(16), result.get(2));
    }

    @Test
    public void testZipByteArraysWithValueForNoneTriFunction() {
        byte[] a = { 1 };
        byte[] b = { 4, 5 };
        byte[] c = { 7, 8, 9 };

        ByteTriFunction<Integer> zipFunction = (b1, b2, b3) -> b1 + b2 + b3;

        List<Integer> result = Stream.zip(a, b, c, (byte) 10, (byte) 20, (byte) 30, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(23), result.get(1));
        assertEquals(Integer.valueOf(39), result.get(2));
    }

    @Test
    public void testZipByteIteratorsWithBiFunction() {
        ByteIterator a = ByteIterator.of((byte) 1, (byte) 2, (byte) 3);
        ByteIterator b = ByteIterator.of((byte) 4, (byte) 5, (byte) 6);

        ByteBiFunction<Integer> zipFunction = (b1, b2) -> b1 + b2;

        List<Integer> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(9), result.get(2));
    }

    @Test
    public void testZipByteIteratorsWithTriFunction() {
        ByteIterator a = ByteIterator.of((byte) 1, (byte) 2, (byte) 3);
        ByteIterator b = ByteIterator.of((byte) 4, (byte) 5, (byte) 6);
        ByteIterator c = ByteIterator.of((byte) 7, (byte) 8, (byte) 9);

        ByteTriFunction<Integer> zipFunction = (b1, b2, b3) -> b1 + b2 + b3;

        List<Integer> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(15), result.get(1));
        assertEquals(Integer.valueOf(18), result.get(2));
    }

    @Test
    public void testZipByteIteratorsWithValueForNone() {
        ByteIterator a = ByteIterator.of((byte) 1, (byte) 2);
        ByteIterator b = ByteIterator.of((byte) 4, (byte) 5, (byte) 6);

        ByteBiFunction<Integer> zipFunction = (b1, b2) -> b1 + b2;

        List<Integer> result = Stream.zip(a, b, (byte) 10, (byte) 20, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(16), result.get(2));
    }

    @Test
    public void testZipByteIteratorsWithValueForNoneTriFunction() {
        ByteIterator a = ByteIterator.of((byte) 1);
        ByteIterator b = ByteIterator.of((byte) 4, (byte) 5);
        ByteIterator c = ByteIterator.of((byte) 7, (byte) 8, (byte) 9);

        ByteTriFunction<Integer> zipFunction = (b1, b2, b3) -> b1 + b2 + b3;

        List<Integer> result = Stream.zip(a, b, c, (byte) 10, (byte) 20, (byte) 30, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(23), result.get(1));
        assertEquals(Integer.valueOf(39), result.get(2));
    }

    @Test
    public void testZipByteStreamCollectionWithNFunction() {
        Collection<ByteStream> streams = Arrays.asList(ByteStream.of((byte) 1, (byte) 2, (byte) 3), ByteStream.of((byte) 4, (byte) 5, (byte) 6),
                ByteStream.of((byte) 7, (byte) 8, (byte) 9));

        ByteNFunction<Integer> zipFunction = bytes -> {
            int sum = 0;
            for (byte b : bytes) {
                sum += b;
            }
            return sum;
        };

        List<Integer> result = Stream.zip(streams, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(15), result.get(1));
        assertEquals(Integer.valueOf(18), result.get(2));
    }

    @Test
    public void testZipByteStreamCollectionWithValuesForNone() {
        Collection<ByteStream> streams = Arrays.asList(ByteStream.of((byte) 1), ByteStream.of((byte) 4, (byte) 5), ByteStream.of((byte) 7, (byte) 8, (byte) 9));

        byte[] valuesForNone = { 10, 20, 30 };

        ByteNFunction<Integer> zipFunction = bytes -> {
            int sum = 0;
            for (byte b : bytes) {
                sum += b;
            }
            return sum;
        };

        List<Integer> result = Stream.zip(streams, valuesForNone, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(23), result.get(1));
        assertEquals(Integer.valueOf(39), result.get(2));
    }

    @Test
    public void testZipByteStreams() {
        ByteStream a = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        ByteStream b = ByteStream.of((byte) 10, (byte) 20, (byte) 30);

        List<Integer> result = Stream.zip(a, b, (b1, b2) -> (int) (b1 + b2)).toList();
        assertEquals(Arrays.asList(11, 22, 33), result);
    }

    @Test
    public void testZipByteStreamsWithBiFunction() {
        ByteStream a = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        ByteStream b = ByteStream.of((byte) 4, (byte) 5, (byte) 6);

        ByteBiFunction<Integer> zipFunction = (b1, b2) -> b1 + b2;

        List<Integer> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(9), result.get(2));
    }

    @Test
    public void testZipByteStreamsWithTriFunction() {
        ByteStream a = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        ByteStream b = ByteStream.of((byte) 4, (byte) 5, (byte) 6);
        ByteStream c = ByteStream.of((byte) 7, (byte) 8, (byte) 9);

        ByteTriFunction<Integer> zipFunction = (b1, b2, b3) -> b1 + b2 + b3;

        List<Integer> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(15), result.get(1));
        assertEquals(Integer.valueOf(18), result.get(2));
    }

    @Test
    public void testZipByteStreamsWithValueForNone() {
        ByteStream a = ByteStream.of((byte) 1, (byte) 2);
        ByteStream b = ByteStream.of((byte) 4, (byte) 5, (byte) 6);

        ByteBiFunction<Integer> zipFunction = (b1, b2) -> b1 + b2;

        List<Integer> result = Stream.zip(a, b, (byte) 10, (byte) 20, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(16), result.get(2));
    }

    @Test
    public void testZipByteStreamsWithValueForNoneTriFunction() {
        ByteStream a = ByteStream.of((byte) 1);
        ByteStream b = ByteStream.of((byte) 4, (byte) 5);
        ByteStream c = ByteStream.of((byte) 7, (byte) 8, (byte) 9);

        ByteTriFunction<Integer> zipFunction = (b1, b2, b3) -> b1 + b2 + b3;

        List<Integer> result = Stream.zip(a, b, c, (byte) 10, (byte) 20, (byte) 30, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(23), result.get(1));
        assertEquals(Integer.valueOf(39), result.get(2));
    }

    @Test
    public void testZipCharArrays() {
        char[] a = { 'a', 'b', 'c' };
        char[] b = { 'x', 'y', 'z' };

        List<String> result = Stream.zip(a, b, (c1, c2) -> "" + c1 + c2).toList();
        assertEquals(Arrays.asList("ax", "by", "cz"), result);

        char[] c = { '1', '2' };
        result = Stream.zip(a, c, (c1, c2) -> "" + c1 + c2).toList();
        assertEquals(Arrays.asList("a1", "b2"), result);
    }

    @Test
    public void testZipCharArraysWithBiFunction() {
        char[] a = { 'a', 'b', 'c' };
        char[] b = { 'x', 'y', 'z' };

        CharBiFunction<String> zipFunction = (c1, c2) -> "" + c1 + c2;

        List<String> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax", result.get(0));
        assertEquals("by", result.get(1));
        assertEquals("cz", result.get(2));
    }

    @Test
    public void testZipCharArraysWithTriFunction() {
        char[] a = { 'a', 'b', 'c' };
        char[] b = { 'x', 'y', 'z' };
        char[] c = { '1', '2', '3' };

        CharTriFunction<String> zipFunction = (c1, c2, c3) -> "" + c1 + c2 + c3;

        List<String> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax1", result.get(0));
        assertEquals("by2", result.get(1));
        assertEquals("cz3", result.get(2));
    }

    @Test
    public void testZipCharArraysWithValueForNone() {
        char[] a = { 'a', 'b' };
        char[] b = { 'x', 'y', 'z' };

        CharBiFunction<String> zipFunction = (c1, c2) -> "" + c1 + c2;

        List<String> result = Stream.zip(a, b, '-', '+', zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax", result.get(0));
        assertEquals("by", result.get(1));
        assertEquals("-z", result.get(2));
    }

    @Test
    public void testZipCharArraysWithValueForNoneTriFunction() {
        char[] a = { 'a' };
        char[] b = { 'x', 'y' };
        char[] c = { '1', '2', '3' };

        CharTriFunction<String> zipFunction = (c1, c2, c3) -> "" + c1 + c2 + c3;

        List<String> result = Stream.zip(a, b, c, '-', '+', '*', zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax1", result.get(0));
        assertEquals("-y2", result.get(1));
        assertEquals("-+3", result.get(2));
    }

    @Test
    public void testZipCharIteratorsWithBiFunction() {
        CharIterator a = CharIterator.of('a', 'b', 'c');
        CharIterator b = CharIterator.of('x', 'y', 'z');

        CharBiFunction<String> zipFunction = (c1, c2) -> "" + c1 + c2;

        List<String> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax", result.get(0));
        assertEquals("by", result.get(1));
        assertEquals("cz", result.get(2));
    }

    @Test
    public void testZipCharIteratorsWithTriFunction() {
        CharIterator a = CharIterator.of('a', 'b', 'c');
        CharIterator b = CharIterator.of('x', 'y', 'z');
        CharIterator c = CharIterator.of('1', '2', '3');

        CharTriFunction<String> zipFunction = (c1, c2, c3) -> "" + c1 + c2 + c3;

        List<String> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax1", result.get(0));
        assertEquals("by2", result.get(1));
        assertEquals("cz3", result.get(2));
    }

    @Test
    public void testZipCharIteratorsWithValueForNone() {
        CharIterator a = CharIterator.of('a', 'b');
        CharIterator b = CharIterator.of('x', 'y', 'z');

        CharBiFunction<String> zipFunction = (c1, c2) -> "" + c1 + c2;

        List<String> result = Stream.zip(a, b, '-', '+', zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax", result.get(0));
        assertEquals("by", result.get(1));
        assertEquals("-z", result.get(2));
    }

    @Test
    public void testZipCharIteratorsWithValueForNoneTriFunction() {
        CharIterator a = CharIterator.of('a');
        CharIterator b = CharIterator.of('x', 'y');
        CharIterator c = CharIterator.of('1', '2', '3');

        CharTriFunction<String> zipFunction = (c1, c2, c3) -> "" + c1 + c2 + c3;

        List<String> result = Stream.zip(a, b, c, '-', '+', '*', zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax1", result.get(0));
        assertEquals("-y2", result.get(1));
        assertEquals("-+3", result.get(2));
    }

    @Test
    public void testZipCharStreamCollectionWithNFunction() {
        Collection<CharStream> streams = Arrays.asList(CharStream.of('a', 'b', 'c'), CharStream.of('x', 'y', 'z'), CharStream.of('1', '2', '3'));

        CharNFunction<String> zipFunction = chars -> {
            StringBuilder sb = new StringBuilder();
            for (char c : chars) {
                sb.append(c);
            }
            return sb.toString();
        };

        List<String> result = Stream.zip(streams, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax1", result.get(0));
        assertEquals("by2", result.get(1));
        assertEquals("cz3", result.get(2));
    }

    @Test
    public void testZipCharStreamCollectionWithValuesForNone() {
        Collection<CharStream> streams = Arrays.asList(CharStream.of('a'), CharStream.of('x', 'y'), CharStream.of('1', '2', '3'));

        char[] valuesForNone = { '-', '+', '*' };

        CharNFunction<String> zipFunction = chars -> {
            StringBuilder sb = new StringBuilder();
            for (char c : chars) {
                sb.append(c);
            }
            return sb.toString();
        };

        List<String> result = Stream.zip(streams, valuesForNone, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax1", result.get(0));
        assertEquals("-y2", result.get(1));
        assertEquals("-+3", result.get(2));
    }

    @Test
    public void testZipCharStreams() {
        CharStream a = CharStream.of('a', 'b', 'c');
        CharStream b = CharStream.of('x', 'y', 'z');

        List<String> result = Stream.zip(a, b, (c1, c2) -> "" + c1 + c2).toList();
        assertEquals(Arrays.asList("ax", "by", "cz"), result);
    }

    @Test
    public void testZipCharStreamsWithBiFunction() {
        CharStream a = CharStream.of('a', 'b', 'c');
        CharStream b = CharStream.of('x', 'y', 'z');

        CharBiFunction<String> zipFunction = (c1, c2) -> "" + c1 + c2;

        List<String> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax", result.get(0));
        assertEquals("by", result.get(1));
        assertEquals("cz", result.get(2));
    }

    @Test
    public void testZipCharStreamsWithTriFunction() {
        CharStream a = CharStream.of('a', 'b', 'c');
        CharStream b = CharStream.of('x', 'y', 'z');
        CharStream c = CharStream.of('1', '2', '3');

        CharTriFunction<String> zipFunction = (c1, c2, c3) -> "" + c1 + c2 + c3;

        List<String> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax1", result.get(0));
        assertEquals("by2", result.get(1));
        assertEquals("cz3", result.get(2));
    }

    @Test
    public void testZipCharStreamsWithValueForNone() {
        CharStream a = CharStream.of('a', 'b');
        CharStream b = CharStream.of('x', 'y', 'z');

        CharBiFunction<String> zipFunction = (c1, c2) -> "" + c1 + c2;

        List<String> result = Stream.zip(a, b, '-', '+', zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax", result.get(0));
        assertEquals("by", result.get(1));
        assertEquals("-z", result.get(2));
    }

    @Test
    public void testZipCharStreamsWithValueForNoneTriFunction() {
        CharStream a = CharStream.of('a');
        CharStream b = CharStream.of('x', 'y');
        CharStream c = CharStream.of('1', '2', '3');

        CharTriFunction<String> zipFunction = (c1, c2, c3) -> "" + c1 + c2 + c3;

        List<String> result = Stream.zip(a, b, c, '-', '+', '*', zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax1", result.get(0));
        assertEquals("-y2", result.get(1));
        assertEquals("-+3", result.get(2));
    }

    @Test
    public void testZipCollectionWithValuesForNone() {
        List<Stream<Integer>> streams = Arrays.asList(createStream(1, 2), createStream(10), createStream(100, 200, 300));

        List<Integer> valuesForNone = Arrays.asList(0, 0, 0);

        List<Integer> result = Stream.zip(streams, valuesForNone, list -> list.stream().mapToInt(Integer::intValue).sum()).toList();
        assertEquals(Arrays.asList(111, 202, 300), result);
    }

    @Test
    public void testZipDoubleArrays() {
        double[] a = { 1.0, 2.0, 3.0 };
        double[] b = { 10.0, 20.0, 30.0 };

        List<Double> result = Stream.zip(a, b, (d1, d2) -> d1 + d2).toList();
        assertEquals(Arrays.asList(11.0, 22.0, 33.0), result);
    }

    @Test
    public void testZipDoubleArraysWithBiFunction() {
        double[] a = { 1d, 2d, 3d };
        double[] b = { 4d, 5d, 6d };

        DoubleBiFunction<Double> zipFunction = (l1, l2) -> l1 + l2;

        List<Double> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(5d), result.get(0));
        assertEquals(Double.valueOf(7d), result.get(1));
        assertEquals(Double.valueOf(9d), result.get(2));
    }

    @Test
    public void testZipDoubleArraysWithTriFunction() {
        double[] a = { 1d, 2d, 3d };
        double[] b = { 4d, 5d, 6d };
        double[] c = { 7d, 8d, 9d };

        DoubleTriFunction<Double> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Double> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(12d), result.get(0));
        assertEquals(Double.valueOf(15d), result.get(1));
        assertEquals(Double.valueOf(18d), result.get(2));
    }

    @Test
    public void testZipDoubleArraysWithValueForNone() {
        double[] a = { 1d, 2d };
        double[] b = { 4d, 5d, 6d };

        DoubleBiFunction<Double> zipFunction = (l1, l2) -> l1 + l2;

        List<Double> result = Stream.zip(a, b, 10d, 20d, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(5d), result.get(0));
        assertEquals(Double.valueOf(7d), result.get(1));
        assertEquals(Double.valueOf(16d), result.get(2));
    }

    @Test
    public void testZipDoubleArraysWithValueForNoneTriFunction() {
        double[] a = { 1d };
        double[] b = { 4d, 5d };
        double[] c = { 7d, 8d, 9d };

        DoubleTriFunction<Double> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Double> result = Stream.zip(a, b, c, 10d, 20d, 30d, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(12d), result.get(0));
        assertEquals(Double.valueOf(23d), result.get(1));
        assertEquals(Double.valueOf(39d), result.get(2));
    }

    @Test
    public void testZipDoubleIteratorsWithBiFunction() {
        DoubleIterator a = DoubleIterator.of(1d, 2d, 3d);
        DoubleIterator b = DoubleIterator.of(4d, 5d, 6d);

        DoubleBiFunction<Double> zipFunction = (l1, l2) -> l1 + l2;

        List<Double> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(5d), result.get(0));
        assertEquals(Double.valueOf(7d), result.get(1));
        assertEquals(Double.valueOf(9d), result.get(2));
    }

    @Test
    public void testZipDoubleIteratorsWithTriFunction() {
        DoubleIterator a = DoubleIterator.of(1d, 2d, 3d);
        DoubleIterator b = DoubleIterator.of(4d, 5d, 6d);
        DoubleIterator c = DoubleIterator.of(7d, 8d, 9d);

        DoubleTriFunction<Double> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Double> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(12d), result.get(0));
        assertEquals(Double.valueOf(15d), result.get(1));
        assertEquals(Double.valueOf(18d), result.get(2));
    }

    @Test
    public void testZipDoubleIteratorsWithValueForNone() {
        DoubleIterator a = DoubleIterator.of(1d, 2d);
        DoubleIterator b = DoubleIterator.of(4d, 5d, 6d);

        DoubleBiFunction<Double> zipFunction = (l1, l2) -> l1 + l2;

        List<Double> result = Stream.zip(a, b, 10d, 20d, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(5d), result.get(0));
        assertEquals(Double.valueOf(7d), result.get(1));
        assertEquals(Double.valueOf(16d), result.get(2));
    }

    @Test
    public void testZipDoubleIteratorsWithValueForNoneTriFunction() {
        DoubleIterator a = DoubleIterator.of(1d);
        DoubleIterator b = DoubleIterator.of(4d, 5d);
        DoubleIterator c = DoubleIterator.of(7d, 8d, 9d);

        DoubleTriFunction<Double> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Double> result = Stream.zip(a, b, c, 10d, 20d, 30d, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(12d), result.get(0));
        assertEquals(Double.valueOf(23d), result.get(1));
        assertEquals(Double.valueOf(39d), result.get(2));
    }

    @Test
    public void testZipDoubleStreamCollectionWithNFunction() {
        Collection<DoubleStream> streams = Arrays.asList(DoubleStream.of(1d, 2d, 3d), DoubleStream.of(4d, 5d, 6d), DoubleStream.of(7d, 8d, 9d));

        DoubleNFunction<Double> zipFunction = doubles -> {
            double sum = 0;
            for (double l : doubles) {
                sum += l;
            }
            return sum;
        };

        List<Double> result = Stream.zip(streams, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(12d), result.get(0));
        assertEquals(Double.valueOf(15d), result.get(1));
        assertEquals(Double.valueOf(18d), result.get(2));
    }

    @Test
    public void testZipDoubleStreamCollectionWithValuesForNone() {
        Collection<DoubleStream> streams = Arrays.asList(DoubleStream.of(1d), DoubleStream.of(4d, 5d), DoubleStream.of(7d, 8d, 9d));

        double[] valuesForNone = { 10d, 20d, 30d };

        DoubleNFunction<Double> zipFunction = doubles -> {
            double sum = 0;
            for (double l : doubles) {
                sum += l;
            }
            return sum;
        };

        List<Double> result = Stream.zip(streams, valuesForNone, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(12d), result.get(0));
        assertEquals(Double.valueOf(23d), result.get(1));
        assertEquals(Double.valueOf(39d), result.get(2));
    }

    @Test
    public void testZipDoubleStreamsWithBiFunction() {
        DoubleStream a = DoubleStream.of(1d, 2d, 3d);
        DoubleStream b = DoubleStream.of(4d, 5d, 6d);

        DoubleBiFunction<Double> zipFunction = (l1, l2) -> l1 + l2;

        List<Double> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(5d), result.get(0));
        assertEquals(Double.valueOf(7d), result.get(1));
        assertEquals(Double.valueOf(9d), result.get(2));
    }

    @Test
    public void testZipDoubleStreamsWithTriFunction() {
        DoubleStream a = DoubleStream.of(1d, 2d, 3d);
        DoubleStream b = DoubleStream.of(4d, 5d, 6d);
        DoubleStream c = DoubleStream.of(7d, 8d, 9d);

        DoubleTriFunction<Double> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Double> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(12d), result.get(0));
        assertEquals(Double.valueOf(15d), result.get(1));
        assertEquals(Double.valueOf(18d), result.get(2));
    }

    @Test
    public void testZipDoubleStreamsWithValueForNone() {
        DoubleStream a = DoubleStream.of(1d, 2d);
        DoubleStream b = DoubleStream.of(4d, 5d, 6d);

        DoubleBiFunction<Double> zipFunction = (l1, l2) -> l1 + l2;

        List<Double> result = Stream.zip(a, b, 10d, 20d, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(5d), result.get(0));
        assertEquals(Double.valueOf(7d), result.get(1));
        assertEquals(Double.valueOf(16d), result.get(2));
    }

    @Test
    public void testZipDoubleStreamsWithValueForNoneTriFunction() {
        DoubleStream a = DoubleStream.of(1d);
        DoubleStream b = DoubleStream.of(4d, 5d);
        DoubleStream c = DoubleStream.of(7d, 8d, 9d);

        DoubleTriFunction<Double> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Double> result = Stream.zip(a, b, c, 10d, 20d, 30d, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(12d), result.get(0));
        assertEquals(Double.valueOf(23d), result.get(1));
        assertEquals(Double.valueOf(39d), result.get(2));
    }

    @Test
    public void testZipFloatArrays() {
        float[] a = { 1.0f, 2.0f, 3.0f };
        float[] b = { 10.0f, 20.0f, 30.0f };

        List<Float> result = Stream.zip(a, b, (f1, f2) -> f1 + f2).toList();
        assertEquals(Arrays.asList(11.0f, 22.0f, 33.0f), result);
    }

    @Test
    public void testZipFloatArraysWithBiFunction() {
        float[] a = { 1f, 2f, 3f };
        float[] b = { 4f, 5f, 6f };

        FloatBiFunction<Float> zipFunction = (l1, l2) -> l1 + l2;

        List<Float> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(5f), result.get(0));
        assertEquals(Float.valueOf(7f), result.get(1));
        assertEquals(Float.valueOf(9f), result.get(2));
    }

    @Test
    public void testZipFloatArraysWithTriFunction() {
        float[] a = { 1f, 2f, 3f };
        float[] b = { 4f, 5f, 6f };
        float[] c = { 7f, 8f, 9f };

        FloatTriFunction<Float> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Float> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(12f), result.get(0));
        assertEquals(Float.valueOf(15f), result.get(1));
        assertEquals(Float.valueOf(18f), result.get(2));
    }

    @Test
    public void testZipFloatArraysWithValueForNone() {
        float[] a = { 1, 2 };
        float[] b = { 4, 5f, 6f };

        FloatBiFunction<Float> zipFunction = (l1, l2) -> l1 + l2;

        List<Float> result = Stream.zip(a, b, 10, 20, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(5f), result.get(0));
        assertEquals(Float.valueOf(7f), result.get(1));
        assertEquals(Float.valueOf(16f), result.get(2));
    }

    @Test
    public void testZipFloatArraysWithValueForNoneTriFunction() {
        float[] a = { 1 };
        float[] b = { 4, 5f };
        float[] c = { 7f, 8f, 9f };

        FloatTriFunction<Float> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Float> result = Stream.zip(a, b, c, 10, 20, 30, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(12), result.get(0));
        assertEquals(Float.valueOf(23), result.get(1));
        assertEquals(Float.valueOf(39f), result.get(2));
    }

    @Test
    public void testZipFloatIteratorsWithBiFunction() {
        FloatIterator a = FloatIterator.of(1f, 2f, 3f);
        FloatIterator b = FloatIterator.of(4f, 5f, 6f);

        FloatBiFunction<Float> zipFunction = (l1, l2) -> l1 + l2;

        List<Float> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(5f), result.get(0));
        assertEquals(Float.valueOf(7f), result.get(1));
        assertEquals(Float.valueOf(9f), result.get(2));
    }

    @Test
    public void testZipFloatIteratorsWithTriFunction() {
        FloatIterator a = FloatIterator.of(1, 2, 3);
        FloatIterator b = FloatIterator.of(4, 5f, 6f);
        FloatIterator c = FloatIterator.of(7f, 8f, 9f);

        FloatTriFunction<Float> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Float> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(12), result.get(0));
        assertEquals(Float.valueOf(15f), result.get(1));
        assertEquals(Float.valueOf(18f), result.get(2));
    }

    @Test
    public void testZipFloatIteratorsWithValueForNone() {
        FloatIterator a = FloatIterator.of(1, 2);
        FloatIterator b = FloatIterator.of(4, 5f, 6f);

        FloatBiFunction<Float> zipFunction = (l1, l2) -> l1 + l2;

        List<Float> result = Stream.zip(a, b, 10, 20, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(5f), result.get(0));
        assertEquals(Float.valueOf(7f), result.get(1));
        assertEquals(Float.valueOf(16f), result.get(2));
    }

    @Test
    public void testZipFloatIteratorsWithValueForNoneTriFunction() {
        FloatIterator a = FloatIterator.of(1);
        FloatIterator b = FloatIterator.of(4, 5f);
        FloatIterator c = FloatIterator.of(7f, 8f, 9f);

        FloatTriFunction<Float> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Float> result = Stream.zip(a, b, c, 10, 20, 30, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(12), result.get(0));
        assertEquals(Float.valueOf(23), result.get(1));
        assertEquals(Float.valueOf(39f), result.get(2));
    }

    @Test
    public void testZipFloatStreamCollectionWithNFunction() {
        Collection<FloatStream> streams = Arrays.asList(FloatStream.of(1, 2, 3), FloatStream.of(4, 5f, 6f), FloatStream.of(7f, 8f, 9f));

        FloatNFunction<Float> zipFunction = floats -> {
            float sum = 0;
            for (float l : floats) {
                sum += l;
            }
            return sum;
        };

        List<Float> result = Stream.zip(streams, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(12), result.get(0));
        assertEquals(Float.valueOf(15f), result.get(1));
        assertEquals(Float.valueOf(18f), result.get(2));
    }

    @Test
    public void testZipFloatStreamCollectionWithValuesForNone() {
        Collection<FloatStream> streams = Arrays.asList(FloatStream.of(1), FloatStream.of(4, 5f), FloatStream.of(7f, 8f, 9f));

        float[] valuesForNone = { 10, 20, 30 };

        FloatNFunction<Float> zipFunction = floats -> {
            float sum = 0;
            for (float l : floats) {
                sum += l;
            }
            return sum;
        };

        List<Float> result = Stream.zip(streams, valuesForNone, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(12), result.get(0));
        assertEquals(Float.valueOf(23), result.get(1));
        assertEquals(Float.valueOf(39f), result.get(2));
    }

    @Test
    public void testZipFloatStreamsWithBiFunction() {
        FloatStream a = FloatStream.of(1, 2, 3);
        FloatStream b = FloatStream.of(4, 5f, 6f);

        FloatBiFunction<Float> zipFunction = (l1, l2) -> l1 + l2;

        List<Float> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(5f), result.get(0));
        assertEquals(Float.valueOf(7f), result.get(1));
        assertEquals(Float.valueOf(9f), result.get(2));
    }

    @Test
    public void testZipFloatStreamsWithTriFunction() {
        FloatStream a = FloatStream.of(1, 2, 3);
        FloatStream b = FloatStream.of(4, 5f, 6f);
        FloatStream c = FloatStream.of(7f, 8f, 9f);

        FloatTriFunction<Float> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Float> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(12), result.get(0));
        assertEquals(Float.valueOf(15f), result.get(1));
        assertEquals(Float.valueOf(18f), result.get(2));
    }

    @Test
    public void testZipFloatStreamsWithValueForNone() {
        FloatStream a = FloatStream.of(1, 2);
        FloatStream b = FloatStream.of(4, 5f, 6f);

        FloatBiFunction<Float> zipFunction = (l1, l2) -> l1 + l2;

        List<Float> result = Stream.zip(a, b, 10, 20, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(5f), result.get(0));
        assertEquals(Float.valueOf(7f), result.get(1));
        assertEquals(Float.valueOf(16f), result.get(2));
    }

    @Test
    public void testZipFloatStreamsWithValueForNoneTriFunction() {
        FloatStream a = FloatStream.of(1);
        FloatStream b = FloatStream.of(4, 5f);
        FloatStream c = FloatStream.of(7f, 8f, 9f);

        FloatTriFunction<Float> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Float> result = Stream.zip(a, b, c, 10, 20, 30, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(12), result.get(0));
        assertEquals(Float.valueOf(23), result.get(1));
        assertEquals(Float.valueOf(39f), result.get(2));
    }

    @Test
    public void testZipIntArrays() {
        int[] a = { 1, 2, 3 };
        int[] b = { 10, 20, 30 };

        List<Integer> result = Stream.zip(a, b, (i1, i2) -> i1 + i2).toList();
        assertEquals(Arrays.asList(11, 22, 33), result);
    }

    @Test
    public void testZipIntArraysWithBiFunction() {
        int[] a = { 1, 2, 3 };
        int[] b = { 4, 5, 6 };

        IntBiFunction<Integer> zipFunction = (i1, i2) -> i1 + i2;

        List<Integer> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(9), result.get(2));
    }

    @Test
    public void testZipIntArraysWithTriFunction() {
        int[] a = { 1, 2, 3 };
        int[] b = { 4, 5, 6 };
        int[] c = { 7, 8, 9 };

        IntTriFunction<Integer> zipFunction = (i1, i2, i3) -> i1 + i2 + i3;

        List<Integer> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(15), result.get(1));
        assertEquals(Integer.valueOf(18), result.get(2));
    }

    @Test
    public void testZipIntArraysWithValueForNone() {
        int[] a = { 1, 2 };
        int[] b = { 4, 5, 6 };

        IntBiFunction<Integer> zipFunction = (i1, i2) -> i1 + i2;

        List<Integer> result = Stream.zip(a, b, 10, 20, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(16), result.get(2));
    }

    @Test
    public void testZipIntArraysWithValueForNoneTriFunction() {
        int[] a = { 1 };
        int[] b = { 4, 5 };
        int[] c = { 7, 8, 9 };

        IntTriFunction<Integer> zipFunction = (i1, i2, i3) -> i1 + i2 + i3;

        List<Integer> result = Stream.zip(a, b, c, 10, 20, 30, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(23), result.get(1));
        assertEquals(Integer.valueOf(39), result.get(2));
    }

    @Test
    public void testZipIntIteratorsWithBiFunction() {
        IntIterator a = IntIterator.of(1, 2, 3);
        IntIterator b = IntIterator.of(4, 5, 6);

        IntBiFunction<Integer> zipFunction = (i1, i2) -> i1 + i2;

        List<Integer> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(9), result.get(2));
    }

    @Test
    public void testZipIntIteratorsWithTriFunction() {
        IntIterator a = IntIterator.of(1, 2, 3);
        IntIterator b = IntIterator.of(4, 5, 6);
        IntIterator c = IntIterator.of(7, 8, 9);

        IntTriFunction<Integer> zipFunction = (i1, i2, i3) -> i1 + i2 + i3;

        List<Integer> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(15), result.get(1));
        assertEquals(Integer.valueOf(18), result.get(2));
    }

    @Test
    public void testZipIntIteratorsWithValueForNone() {
        IntIterator a = IntIterator.of(1, 2);
        IntIterator b = IntIterator.of(4, 5, 6);

        IntBiFunction<Integer> zipFunction = (i1, i2) -> i1 + i2;

        List<Integer> result = Stream.zip(a, b, 10, 20, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(16), result.get(2));
    }

    @Test
    public void testZipIntIteratorsWithValueForNoneTriFunction() {
        IntIterator a = IntIterator.of(1);
        IntIterator b = IntIterator.of(4, 5);
        IntIterator c = IntIterator.of(7, 8, 9);

        IntTriFunction<Integer> zipFunction = (i1, i2, i3) -> i1 + i2 + i3;

        List<Integer> result = Stream.zip(a, b, c, 10, 20, 30, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(23), result.get(1));
        assertEquals(Integer.valueOf(39), result.get(2));
    }

    @Test
    public void testZipIntStreamCollectionWithNFunction() {
        Collection<IntStream> streams = Arrays.asList(IntStream.of(1, 2, 3), IntStream.of(4, 5, 6), IntStream.of(7, 8, 9));

        IntNFunction<Integer> zipFunction = ints -> {
            int sum = 0;
            for (int i : ints) {
                sum += i;
            }
            return sum;
        };

        List<Integer> result = Stream.zip(streams, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(15), result.get(1));
        assertEquals(Integer.valueOf(18), result.get(2));
    }

    @Test
    public void testZipIntStreamCollectionWithValuesForNone() {
        Collection<IntStream> streams = Arrays.asList(IntStream.of(1), IntStream.of(4, 5), IntStream.of(7, 8, 9));

        int[] valuesForNone = { 10, 20, 30 };

        IntNFunction<Integer> zipFunction = ints -> {
            int sum = 0;
            for (int i : ints) {
                sum += i;
            }
            return sum;
        };

        List<Integer> result = Stream.zip(streams, valuesForNone, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(23), result.get(1));
        assertEquals(Integer.valueOf(39), result.get(2));
    }

    @Test
    public void testZipIntStreams() {
        IntStream a = IntStream.of(1, 2, 3);
        IntStream b = IntStream.of(10, 20, 30);

        List<Integer> result = Stream.zip(a, b, (i1, i2) -> i1 + i2).toList();
        assertEquals(Arrays.asList(11, 22, 33), result);
    }

    @Test
    public void testZipIntStreamsWithBiFunction() {
        IntStream a = IntStream.of(1, 2, 3);
        IntStream b = IntStream.of(4, 5, 6);

        IntBiFunction<Integer> zipFunction = (i1, i2) -> i1 + i2;

        List<Integer> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(9), result.get(2));
    }

    @Test
    public void testZipIntStreamsWithTriFunction() {
        IntStream a = IntStream.of(1, 2, 3);
        IntStream b = IntStream.of(4, 5, 6);
        IntStream c = IntStream.of(7, 8, 9);

        IntTriFunction<Integer> zipFunction = (i1, i2, i3) -> i1 + i2 + i3;

        List<Integer> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(15), result.get(1));
        assertEquals(Integer.valueOf(18), result.get(2));
    }

    @Test
    public void testZipIntStreamsWithValueForNone() {
        IntStream a = IntStream.of(1, 2);
        IntStream b = IntStream.of(4, 5, 6);

        IntBiFunction<Integer> zipFunction = (i1, i2) -> i1 + i2;

        List<Integer> result = Stream.zip(a, b, 10, 20, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(16), result.get(2));
    }

    @Test
    public void testZipIntStreamsWithValueForNoneTriFunction() {
        IntStream a = IntStream.of(1);
        IntStream b = IntStream.of(4, 5);
        IntStream c = IntStream.of(7, 8, 9);

        IntTriFunction<Integer> zipFunction = (i1, i2, i3) -> i1 + i2 + i3;

        List<Integer> result = Stream.zip(a, b, c, 10, 20, 30, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(23), result.get(1));
        assertEquals(Integer.valueOf(39), result.get(2));
    }

    @Test
    public void testZipIterablesWithValueForNone() {
        List<String> a = Arrays.asList("a", "b", "c");
        List<Integer> b = Arrays.asList(1, 2);

        List<String> result = Stream.zip(a, b, "X", 99, (s, i) -> s + i).toList();
        assertEquals(Arrays.asList("a1", "b2", "c99"), result);
    }

    @Test
    public void testZipIteratorsWithValueForNone() {
        Iterator<String> a = Arrays.asList("a", "b", "c").iterator();
        Iterator<Integer> b = Arrays.asList(1, 2).iterator();

        List<String> result = Stream.zip(a, b, "X", 99, (s, i) -> s + i).toList();
        assertEquals(Arrays.asList("a1", "b2", "c99"), result);
    }

    @Test
    public void testZipLongArrays() {
        long[] a = { 1L, 2L, 3L };
        long[] b = { 10L, 20L, 30L };

        List<Long> result = Stream.zip(a, b, (l1, l2) -> l1 + l2).toList();
        assertEquals(Arrays.asList(11L, 22L, 33L), result);
    }

    @Test
    public void testZipLongArraysWithBiFunction() {
        long[] a = { 1L, 2L, 3L };
        long[] b = { 4L, 5L, 6L };

        LongBiFunction<Long> zipFunction = (l1, l2) -> l1 + l2;

        List<Long> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(5L), result.get(0));
        assertEquals(Long.valueOf(7L), result.get(1));
        assertEquals(Long.valueOf(9L), result.get(2));
    }

    @Test
    public void testZipLongArraysWithTriFunction() {
        long[] a = { 1L, 2L, 3L };
        long[] b = { 4L, 5L, 6L };
        long[] c = { 7L, 8L, 9L };

        LongTriFunction<Long> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Long> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(12L), result.get(0));
        assertEquals(Long.valueOf(15L), result.get(1));
        assertEquals(Long.valueOf(18L), result.get(2));
    }

    @Test
    public void testZipLongArraysWithValueForNone() {
        long[] a = { 1L, 2L };
        long[] b = { 4L, 5L, 6L };

        LongBiFunction<Long> zipFunction = (l1, l2) -> l1 + l2;

        List<Long> result = Stream.zip(a, b, 10L, 20L, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(5L), result.get(0));
        assertEquals(Long.valueOf(7L), result.get(1));
        assertEquals(Long.valueOf(16L), result.get(2));
    }

    @Test
    public void testZipLongArraysWithValueForNoneTriFunction() {
        long[] a = { 1L };
        long[] b = { 4L, 5L };
        long[] c = { 7L, 8L, 9L };

        LongTriFunction<Long> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Long> result = Stream.zip(a, b, c, 10L, 20L, 30L, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(12L), result.get(0));
        assertEquals(Long.valueOf(23L), result.get(1));
        assertEquals(Long.valueOf(39L), result.get(2));
    }

    @Test
    public void testZipLongIteratorsWithBiFunction() {
        LongIterator a = LongIterator.of(1L, 2L, 3L);
        LongIterator b = LongIterator.of(4L, 5L, 6L);

        LongBiFunction<Long> zipFunction = (l1, l2) -> l1 + l2;

        List<Long> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(5L), result.get(0));
        assertEquals(Long.valueOf(7L), result.get(1));
        assertEquals(Long.valueOf(9L), result.get(2));
    }

    @Test
    public void testZipLongIteratorsWithTriFunction() {
        LongIterator a = LongIterator.of(1L, 2L, 3L);
        LongIterator b = LongIterator.of(4L, 5L, 6L);
        LongIterator c = LongIterator.of(7L, 8L, 9L);

        LongTriFunction<Long> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Long> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(12L), result.get(0));
        assertEquals(Long.valueOf(15L), result.get(1));
        assertEquals(Long.valueOf(18L), result.get(2));
    }

    @Test
    public void testZipLongIteratorsWithValueForNone() {
        LongIterator a = LongIterator.of(1L, 2L);
        LongIterator b = LongIterator.of(4L, 5L, 6L);

        LongBiFunction<Long> zipFunction = (l1, l2) -> l1 + l2;

        List<Long> result = Stream.zip(a, b, 10L, 20L, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(5L), result.get(0));
        assertEquals(Long.valueOf(7L), result.get(1));
        assertEquals(Long.valueOf(16L), result.get(2));
    }

    @Test
    public void testZipLongIteratorsWithValueForNoneTriFunction() {
        LongIterator a = LongIterator.of(1L);
        LongIterator b = LongIterator.of(4L, 5L);
        LongIterator c = LongIterator.of(7L, 8L, 9L);

        LongTriFunction<Long> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Long> result = Stream.zip(a, b, c, 10L, 20L, 30L, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(12L), result.get(0));
        assertEquals(Long.valueOf(23L), result.get(1));
        assertEquals(Long.valueOf(39L), result.get(2));
    }

    @Test
    public void testZipLongStreamCollectionWithNFunction() {
        Collection<LongStream> streams = Arrays.asList(LongStream.of(1L, 2L, 3L), LongStream.of(4L, 5L, 6L), LongStream.of(7L, 8L, 9L));

        LongNFunction<Long> zipFunction = longs -> {
            long sum = 0;
            for (long l : longs) {
                sum += l;
            }
            return sum;
        };

        List<Long> result = Stream.zip(streams, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(12L), result.get(0));
        assertEquals(Long.valueOf(15L), result.get(1));
        assertEquals(Long.valueOf(18L), result.get(2));
    }

    @Test
    public void testZipLongStreamCollectionWithValuesForNone() {
        Collection<LongStream> streams = Arrays.asList(LongStream.of(1L), LongStream.of(4L, 5L), LongStream.of(7L, 8L, 9L));

        long[] valuesForNone = { 10L, 20L, 30L };

        LongNFunction<Long> zipFunction = longs -> {
            long sum = 0;
            for (long l : longs) {
                sum += l;
            }
            return sum;
        };

        List<Long> result = Stream.zip(streams, valuesForNone, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(12L), result.get(0));
        assertEquals(Long.valueOf(23L), result.get(1));
        assertEquals(Long.valueOf(39L), result.get(2));
    }

    @Test
    public void testZipLongStreamsWithBiFunction() {
        LongStream a = LongStream.of(1L, 2L, 3L);
        LongStream b = LongStream.of(4L, 5L, 6L);

        LongBiFunction<Long> zipFunction = (l1, l2) -> l1 + l2;

        List<Long> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(5L), result.get(0));
        assertEquals(Long.valueOf(7L), result.get(1));
        assertEquals(Long.valueOf(9L), result.get(2));
    }

    @Test
    public void testZipLongStreamsWithTriFunction() {
        LongStream a = LongStream.of(1L, 2L, 3L);
        LongStream b = LongStream.of(4L, 5L, 6L);
        LongStream c = LongStream.of(7L, 8L, 9L);

        LongTriFunction<Long> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Long> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(12L), result.get(0));
        assertEquals(Long.valueOf(15L), result.get(1));
        assertEquals(Long.valueOf(18L), result.get(2));
    }

    @Test
    public void testZipLongStreamsWithValueForNone() {
        LongStream a = LongStream.of(1L, 2L);
        LongStream b = LongStream.of(4L, 5L, 6L);

        LongBiFunction<Long> zipFunction = (l1, l2) -> l1 + l2;

        List<Long> result = Stream.zip(a, b, 10L, 20L, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(5L), result.get(0));
        assertEquals(Long.valueOf(7L), result.get(1));
        assertEquals(Long.valueOf(16L), result.get(2));
    }

    @Test
    public void testZipLongStreamsWithValueForNoneTriFunction() {
        LongStream a = LongStream.of(1L);
        LongStream b = LongStream.of(4L, 5L);
        LongStream c = LongStream.of(7L, 8L, 9L);

        LongTriFunction<Long> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Long> result = Stream.zip(a, b, c, 10L, 20L, 30L, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(12L), result.get(0));
        assertEquals(Long.valueOf(23L), result.get(1));
        assertEquals(Long.valueOf(39L), result.get(2));
    }

    @Test
    public void testZipMismatchedValuesForNoneDoesNotOpenSources() {
        final AtomicInteger closed = new AtomicInteger();
        final Stream<Integer> a = Stream.of(1, 2).onClose(closed::incrementAndGet);
        // An already-closed source makes the opening step observable: whichever check runs first decides
        // the exception. The size check must win, otherwise iterateAll() opens 'a' and strands it.
        final Stream<Integer> alreadyClosed = Stream.of(3);
        alreadyClosed.close();

        assertThrows(IllegalArgumentException.class, () -> Stream.zip(Arrays.asList(a, alreadyClosed), Arrays.asList(0), list -> 0));
        assertEquals(0, closed.get());
        assertEquals(Arrays.asList(1, 2), a.toList());

        // parallelZip guards the same way.
        final AtomicInteger closedParallel = new AtomicInteger();
        final Stream<Integer> pa = Stream.of(1, 2).onClose(closedParallel::incrementAndGet);
        final Stream<Integer> pClosed = Stream.of(3);
        pClosed.close();

        assertThrows(IllegalArgumentException.class, () -> Stream.parallelZip(Arrays.asList(pa, pClosed), Arrays.asList(0), list -> 0, 2));
        assertEquals(0, closedParallel.get());
        assertEquals(Arrays.asList(1, 2), pa.toList());
    }

    @Test
    public void testZipObjectArrays() {
        String[] a = { "a", "b", "c" };
        Integer[] b = { 1, 2, 3 };

        List<String> result = Stream.zip(a, b, (s, i) -> s + i).toList();
        assertEquals(Arrays.asList("a1", "b2", "c3"), result);
    }

    @Test
    public void testZipPrimitiveArraysWithValueForNone() {
        int[] a = { 1, 2, 3 };
        int[] b = { 10, 20 };

        List<Integer> result = Stream.zip(a, b, 0, 99, (i1, i2) -> i1 + i2).toList();
        assertEquals(Arrays.asList(11, 22, 102), result);
    }

    @Test
    public void testZipPrimitiveStreamCollection() {
        List<IntStream> streams = Arrays.asList(IntStream.of(1, 2, 3), IntStream.of(10, 20, 30), IntStream.of(100, 200, 300));

        List<Integer> result = Stream.zip(streams, arr -> Arrays.stream(arr).sum()).toList();
        assertEquals(Arrays.asList(111, 222, 333), result);
    }

    @Test
    public void testZipPrimitiveStreamCollectionWithValuesForNone() {
        List<IntStream> streams = Arrays.asList(IntStream.of(1, 2), IntStream.of(10), IntStream.of(100, 200, 300));

        int[] valuesForNone = { 0, 0, 0 };

        List<Integer> result = Stream.zip(streams, valuesForNone, arr -> Arrays.stream(arr).sum()).toList();
        assertEquals(Arrays.asList(111, 202, 300), result);
    }

    @Test
    public void testZipPrimitiveStreamsWithValueForNone() {
        IntStream a = IntStream.of(1, 2, 3);
        IntStream b = IntStream.of(10, 20);

        List<Integer> result = Stream.zip(a, b, 0, 99, (i1, i2) -> i1 + i2).toList();
        assertEquals(Arrays.asList(11, 22, 102), result);
    }

    @Test
    public void testZipShortArrays() {
        short[] a = { 1, 2, 3 };
        short[] b = { 10, 20, 30 };

        List<Integer> result = Stream.zip(a, b, (s1, s2) -> (int) (s1 + s2)).toList();
        assertEquals(Arrays.asList(11, 22, 33), result);
    }

    @Test
    public void testZipShortArraysWithBiFunction() {
        short[] a = { 1, 2, 3 };
        short[] b = { 4, 5, 6 };

        ShortBiFunction<Integer> zipFunction = (s1, s2) -> s1 + s2;

        List<Integer> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(9), result.get(2));
    }

    @Test
    public void testZipShortArraysWithTriFunction() {
        short[] a = { 1, 2, 3 };
        short[] b = { 4, 5, 6 };
        short[] c = { 7, 8, 9 };

        ShortTriFunction<Integer> zipFunction = (s1, s2, s3) -> s1 + s2 + s3;

        List<Integer> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(15), result.get(1));
        assertEquals(Integer.valueOf(18), result.get(2));
    }

    @Test
    public void testZipShortArraysWithValueForNone() {
        short[] a = { 1, 2 };
        short[] b = { 4, 5, 6 };

        ShortBiFunction<Integer> zipFunction = (s1, s2) -> s1 + s2;

        List<Integer> result = Stream.zip(a, b, (short) 10, (short) 20, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(16), result.get(2));
    }

    @Test
    public void testZipShortArraysWithValueForNoneTriFunction() {
        short[] a = { 1 };
        short[] b = { 4, 5 };
        short[] c = { 7, 8, 9 };

        ShortTriFunction<Integer> zipFunction = (s1, s2, s3) -> s1 + s2 + s3;

        List<Integer> result = Stream.zip(a, b, c, (short) 10, (short) 20, (short) 30, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(23), result.get(1));
        assertEquals(Integer.valueOf(39), result.get(2));
    }

    @Test
    public void testZipShortIteratorsWithBiFunction() {
        ShortIterator a = ShortIterator.of((short) 1, (short) 2, (short) 3);
        ShortIterator b = ShortIterator.of((short) 4, (short) 5, (short) 6);

        ShortBiFunction<Integer> zipFunction = (s1, s2) -> s1 + s2;

        List<Integer> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(9), result.get(2));
    }

    @Test
    public void testZipShortIteratorsWithTriFunction() {
        ShortIterator a = ShortIterator.of((short) 1, (short) 2, (short) 3);
        ShortIterator b = ShortIterator.of((short) 4, (short) 5, (short) 6);
        ShortIterator c = ShortIterator.of((short) 7, (short) 8, (short) 9);

        ShortTriFunction<Integer> zipFunction = (s1, s2, s3) -> s1 + s2 + s3;

        List<Integer> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(15), result.get(1));
        assertEquals(Integer.valueOf(18), result.get(2));
    }

    @Test
    public void testZipShortIteratorsWithValueForNone() {
        ShortIterator a = ShortIterator.of((short) 1, (short) 2);
        ShortIterator b = ShortIterator.of((short) 4, (short) 5, (short) 6);

        ShortBiFunction<Integer> zipFunction = (s1, s2) -> s1 + s2;

        List<Integer> result = Stream.zip(a, b, (short) 10, (short) 20, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(16), result.get(2));
    }

    @Test
    public void testZipShortIteratorsWithValueForNoneTriFunction() {
        ShortIterator a = ShortIterator.of((short) 1);
        ShortIterator b = ShortIterator.of((short) 4, (short) 5);
        ShortIterator c = ShortIterator.of((short) 7, (short) 8, (short) 9);

        ShortTriFunction<Integer> zipFunction = (s1, s2, s3) -> s1 + s2 + s3;

        List<Integer> result = Stream.zip(a, b, c, (short) 10, (short) 20, (short) 30, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(23), result.get(1));
        assertEquals(Integer.valueOf(39), result.get(2));
    }

    @Test
    public void testZipShortStreamCollectionWithNFunction() {
        Collection<ShortStream> streams = Arrays.asList(ShortStream.of((short) 1, (short) 2, (short) 3), ShortStream.of((short) 4, (short) 5, (short) 6),
                ShortStream.of((short) 7, (short) 8, (short) 9));

        ShortNFunction<Integer> zipFunction = shorts -> {
            int sum = 0;
            for (short s : shorts) {
                sum += s;
            }
            return sum;
        };

        List<Integer> result = Stream.zip(streams, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(15), result.get(1));
        assertEquals(Integer.valueOf(18), result.get(2));
    }

    @Test
    public void testZipShortStreamCollectionWithValuesForNone() {
        Collection<ShortStream> streams = Arrays.asList(ShortStream.of((short) 1), ShortStream.of((short) 4, (short) 5),
                ShortStream.of((short) 7, (short) 8, (short) 9));

        short[] valuesForNone = { 10, 20, 30 };

        ShortNFunction<Integer> zipFunction = shorts -> {
            int sum = 0;
            for (short s : shorts) {
                sum += s;
            }
            return sum;
        };

        List<Integer> result = Stream.zip(streams, valuesForNone, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(23), result.get(1));
        assertEquals(Integer.valueOf(39), result.get(2));
    }

    @Test
    public void testZipShortStreamsWithBiFunction() {
        ShortStream a = ShortStream.of((short) 1, (short) 2, (short) 3);
        ShortStream b = ShortStream.of((short) 4, (short) 5, (short) 6);

        ShortBiFunction<Integer> zipFunction = (s1, s2) -> s1 + s2;

        List<Integer> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(9), result.get(2));
    }

    @Test
    public void testZipShortStreamsWithTriFunction() {
        ShortStream a = ShortStream.of((short) 1, (short) 2, (short) 3);
        ShortStream b = ShortStream.of((short) 4, (short) 5, (short) 6);
        ShortStream c = ShortStream.of((short) 7, (short) 8, (short) 9);

        ShortTriFunction<Integer> zipFunction = (s1, s2, s3) -> s1 + s2 + s3;

        List<Integer> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(15), result.get(1));
        assertEquals(Integer.valueOf(18), result.get(2));
    }

    @Test
    public void testZipShortStreamsWithValueForNone() {
        ShortStream a = ShortStream.of((short) 1, (short) 2);
        ShortStream b = ShortStream.of((short) 4, (short) 5, (short) 6);

        ShortBiFunction<Integer> zipFunction = (s1, s2) -> s1 + s2;

        List<Integer> result = Stream.zip(a, b, (short) 10, (short) 20, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(16), result.get(2));
    }

    @Test
    public void testZipShortStreamsWithValueForNoneTriFunction() {
        ShortStream a = ShortStream.of((short) 1);
        ShortStream b = ShortStream.of((short) 4, (short) 5);
        ShortStream c = ShortStream.of((short) 7, (short) 8, (short) 9);

        ShortTriFunction<Integer> zipFunction = (s1, s2, s3) -> s1 + s2 + s3;

        List<Integer> result = Stream.zip(a, b, c, (short) 10, (short) 20, (short) 30, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(23), result.get(1));
        assertEquals(Integer.valueOf(39), result.get(2));
    }

    @Test
    public void testZipStreamCollection() {
        List<Stream<Integer>> streams = Arrays.asList(createStream(1, 2, 3), createStream(10, 20, 30), createStream(100, 200, 300));

        List<Integer> result = Stream.zip(streams, list -> list.stream().mapToInt(Integer::intValue).sum()).toList();
        assertEquals(Arrays.asList(111, 222, 333), result);
    }

    @Test
    public void testZipStreams() {
        Stream<String> a = createStream01("a", "b", "c");
        Stream<Integer> b = createStream(1, 2, 3);

        List<String> result = Stream.zip(a, b, (s, i) -> s + i).toList();
        assertEquals(Arrays.asList("a1", "b2", "c3"), result);
    }

    @Test
    public void testZipStreamsWithValueForNone() {
        Stream<String> a = createStream01("a", "b", "c");
        Stream<Integer> b = createStream(1, 2);

        List<String> result = Stream.zip(a, b, "X", 99, (s, i) -> s + i).toList();
        assertEquals(Arrays.asList("a1", "b2", "c99"), result);
    }

    @Test
    public void testZipThreeArraysWithValueForNone() {
        String[] a = { "a", "b" };
        Integer[] b = { 1 };
        Double[] c = { 0.1, 0.2, 0.3 };

        List<String> result = Stream.zip(a, b, c, "X", 99, 0.0, (s, i, d) -> s + i + d).toList();
        assertEquals(Arrays.asList("a10.1", "b990.2", "X990.3"), result);
    }

    @Test
    public void testZipThreeCharArrays() {
        char[] a = { 'a', 'b' };
        char[] b = { 'x', 'y' };
        char[] c = { '1', '2' };

        List<String> result = Stream.zip(a, b, c, (c1, c2, c3) -> "" + c1 + c2 + c3).toList();
        assertEquals(Arrays.asList("ax1", "by2"), result);
    }

    @Test
    public void testZipThreeObjectArrays() {
        String[] a = { "a", "b" };
        Integer[] b = { 1, 2 };
        Double[] c = { 0.1, 0.2 };

        List<String> result = Stream.zip(a, b, c, (s, i, d) -> s + i + d).toList();
        assertEquals(Arrays.asList("a10.1", "b20.2"), result);
    }

    @Test
    public void testZipThreeStreams() {
        Stream<String> a = createStream01("a", "b");
        Stream<Integer> b = createStream(1, 2);
        Stream<Double> c = createStream(0.1, 0.2);

        List<String> result = Stream.zip(a, b, c, (s, i, d) -> s + i + d).toList();
        assertEquals(Arrays.asList("a10.1", "b20.2"), result);
    }

    @Test
    public void testZipWithDefaultValues() {
        Stream<String> stream = Stream.zip(Arrays.asList(1, 2), Arrays.asList("a", "b", "c"), 0, "", (i, s) -> i + s);
        assertEquals(Arrays.asList("1a", "2b", "0c"), stream.toList());
    }

    @Test
    public void testZipWithDifferentLengths() {
        Stream<String> stream = Stream.zip(Arrays.asList(1, 2), Arrays.asList("a", "b", "c"), (i, s) -> i + s);
        assertEquals(Arrays.asList("1a", "2b"), stream.toList());
    }

    // zipIterables

    @Test
    public void testZipIterables() {
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        List<Integer> list2 = Arrays.asList(10, 20, 30);

        List<Integer> result = Stream.zipIterables(Arrays.asList(list1, list2), (List<Integer> args) -> args.get(0) + args.get(1)).toList();
        assertEquals(Arrays.asList(11, 22, 33), result);
    }

    @Test
    public void testZipIterablesCollectionWithValuesForNone() {
        List<Iterable<Integer>> iterables = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(10), Arrays.asList(100, 200, 300));

        List<Integer> valuesForNone = Arrays.asList(0, 0, 0);

        List<Integer> result = Stream.zipIterables(iterables, valuesForNone, list -> list.stream().mapToInt(Integer::intValue).sum()).toList();
        assertEquals(Arrays.asList(111, 202, 300), result);
    }

    @Test
    public void testZipIterablesEmpty() {
        List<Integer> result = Stream.zipIterables(Arrays.asList(), (List<Integer> args) -> 0).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testZipIterablesWithFunction() {
        List<Iterable<Integer>> iterables = Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(10, 20, 30), Arrays.asList(100, 200, 300));

        List<Integer> result = Stream.zipIterables(iterables, list -> list.stream().mapToInt(Integer::intValue).sum()).toList();
        assertEquals(Arrays.asList(111, 222, 333), result);
    }



    // zipIterables(Collection<Iterable>, valuesForNone, fn) ragged padding (line 24841).
    @Test
    public void testZipIterablesWithValuesForNone_uncovered() {
        final List<Iterable<Integer>> iterables = N.asList((Iterable<Integer>) N.asList(1, 2), N.asList(10, 20, 30), N.asList(100));
        final List<Integer> defaults = N.asList(0, 0, 0);
        final Function<List<Integer>, Integer> sumFn = list -> list.stream().mapToInt(Integer::intValue).sum();
        assertEquals(N.asList(111, 22, 30), Stream.zipIterables(iterables, defaults, sumFn).toList());
    }

    // zipIterators

    @Test
    public void testZipIterators() {
        Iterator<Integer> iter1 = Arrays.asList(1, 2).iterator();
        Iterator<Integer> iter2 = Arrays.asList(10, 20).iterator();

        List<Integer> result = Stream.zipIterators(Arrays.asList(iter1, iter2), (List<Integer> args) -> args.get(0) + args.get(1)).toList();
        assertEquals(Arrays.asList(11, 22), result);
    }

    @Test
    public void testZipIteratorsAllNonNullStillWorks() {
        List<Iterator<Integer>> iterators = new ArrayList<>();
        iterators.add(Arrays.asList(1, 2, 3).iterator());
        iterators.add(Arrays.asList(10, 20, 30).iterator());

        Function<List<Integer>, Integer> sumFn = list -> list.stream().mapToInt(Integer::intValue).sum();
        List<Integer> result = Stream.zipIterators(iterators, sumFn).toList();
        assertEquals(Arrays.asList(11, 22, 33), result);
    }

    @Test
    public void testZipIteratorsCollectionWithValuesForNone() {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2).iterator(), Arrays.asList(10).iterator(),
                Arrays.asList(100, 200, 300).iterator());

        List<Integer> valuesForNone = Arrays.asList(0, 0, 0);

        List<Integer> result = Stream.zipIterators(iterators, valuesForNone, list -> list.stream().mapToInt(Integer::intValue).sum()).toList();
        assertEquals(Arrays.asList(111, 202, 300), result);
    }

    @Test
    public void testZipIteratorsWithFunction() {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2, 3).iterator(), Arrays.asList(10, 20, 30).iterator(),
                Arrays.asList(100, 200, 300).iterator());

        List<Integer> result = Stream.zipIterators(iterators, list -> list.stream().mapToInt(Integer::intValue).sum()).toList();
        assertEquals(Arrays.asList(111, 222, 333), result);
    }

    @Test
    public void testZipIteratorsWithNullIteratorInCollection() {
        List<Iterator<Integer>> iterators = new ArrayList<>();
        iterators.add(Arrays.asList(1, 2, 3).iterator());
        iterators.add(null);
        iterators.add(Arrays.asList(10, 20, 30).iterator());

        // No-defaults zipIterators should be consistent with concatIterators: null iterators treated as empty.
        // Since one iterator is null/empty, zip stops immediately and returns empty.
        Function<List<Integer>, Integer> sumFn = list -> list.stream().mapToInt(Integer::intValue).sum();
        List<Integer> result = Stream.zipIterators(iterators, sumFn).toList();
        assertEquals(Collections.emptyList(), result);
    }



    // zipIterators(Collection<Iterator>, valuesForNone, fn): empty -> empty (line 24876).
    @Test
    public void testZipIteratorsWithValuesForNone_empty_uncovered() {
        final List<Iterator<Integer>> iterators = new ArrayList<>();
        final List<Integer> none = new ArrayList<>();
        final Function<List<Integer>, Integer> sumFn = list -> list.stream().mapToInt(Integer::intValue).sum();
        assertEquals(0, Stream.zipIterators(iterators, none, sumFn).count());
    }



    // zipIterators(Collection<Iterator>, valuesForNone, fn): size mismatch throws (line 24882).
    @Test
    public void testZipIteratorsWithValuesForNone_sizeMismatch_uncovered() {
        final List<Iterator<Integer>> iterators = N.asList(N.asList(1, 2).iterator(), N.asList(3, 4).iterator());
        final List<Integer> wrongSize = N.asList(0); // size 1 != 2
        final Function<List<Integer>, Integer> sumFn = list -> list.stream().mapToInt(Integer::intValue).sum();
        assertThrows(IllegalArgumentException.class, () -> Stream.zipIterators(iterators, wrongSize, sumFn));
    }

    // zipWith

    @Test
    public void testZipWith() {
        List<Integer> list1 = Arrays.asList(10, 20, 30);

        List<Integer> result = Stream.of(1, 2, 3).zipWith(list1, (a, b) -> a + b).toList();
        assertEquals(Arrays.asList(11, 22, 33), result);
    }

    @Test
    public void testZipWith_DifferentLengths() {
        List<String> result = Stream.of(1, 2, 3).zipWith(Arrays.asList("a", "b"), (i, s) -> i + s).toList();
        assertEquals(Arrays.asList("1a", "2b"), result);
    }

    @Test
    public void testZipWith_StreamAndStream_TriFunction() {
        List<String> result = Stream.of(1, 2, 3).zipWith(Stream.of("a", "b", "c"), Stream.of(true, false, true), (i, s, b) -> i + s + b).toList();
        assertEquals(3, result.size());
        assertEquals("1atrue", result.get(0));
        assertEquals("2bfalse", result.get(1));
    }

    @Test
    public void testZipWith_StreamWithDefaults() {
        List<String> result = Stream.of(1, 2).zipWith(Stream.of("a", "b", "c"), 0, "x", (i, s) -> i + s).toList();
        assertEquals(3, result.size());
        assertEquals("1a", result.get(0));
        assertEquals("0c", result.get(2)); // uses default for missing left element (0 paired with "c")
    }

    @Test
    public void testZipWith_ThreeCollections() {
        List<String> result = Stream.of(1, 2).zipWith(Arrays.asList("a", "b"), Arrays.asList(true, false), (i, s, b) -> i + s + b).toList();
        assertEquals(Arrays.asList("1atrue", "2bfalse"), result);
    }

    @Test
    public void testZipWith_WithDefaults() {
        List<String> result = Stream.of(1, 2, 3).zipWith(Arrays.asList("a", "b"), 0, "z", (i, s) -> i + s).toList();
        assertEquals(Arrays.asList("1a", "2b", "3z"), result);
    }

    @Test
    public void testZipWithDefaultValue() {
        List<Integer> list1 = Arrays.asList(10, 20);

        List<Integer> result = Stream.of(1, 2, 3).zipWith(list1, 0, 0, (a, b) -> a + b).toList();
        assertEquals(Arrays.asList(11, 22, 3), result);
    }

    @Test
    public void testZipWithEmpty() {
        List<Integer> list1 = Arrays.asList(10, 20, 30);

        List<Integer> result = Stream.<Integer> empty().zipWith(list1, (a, b) -> a + b).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testZipWithThreeWay() {
        List<Integer> list1 = Arrays.asList(10, 20, 30);
        List<Integer> list2 = Arrays.asList(100, 200, 300);

        List<Integer> result = Stream.of(1, 2, 3).zipWith(list1, list2, (a, b, c) -> a + b + c).toList();
        assertEquals(Arrays.asList(111, 222, 333), result);
    }

}
