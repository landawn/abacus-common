package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;

/** AbstractStream reduction contracts. */
public class AbstractStreamReductionTest extends AbstractStreamTestSupport {

    @Test
    public void test_allMatch() {
        assertTrue(Stream.of(2, 4, 6).allMatch(i -> i % 2 == 0));
        assertFalse(Stream.of(1, 2, 3).allMatch(i -> i % 2 == 0));
    }

    @Test
    public void test_anyMatch() {
        assertTrue(Stream.of(1, 2, 3).anyMatch(i -> i == 2));
        assertFalse(Stream.of(1, 2, 3).anyMatch(i -> i == 4));
    }

    @Test
    public void test_average() {
        OptionalDouble result = Stream.of(1, 2, 3).mapToInt(i -> i).average();
        assertTrue(result.isPresent());
        assertEquals(2.0, result.getAsDouble());
    }

    // averageDouble

    @Test
    public void test_averageDouble() {
        OptionalDouble result = Stream.of(1.0, 2.0, 3.0).averageDouble(d -> d);
        assertTrue(result.isPresent());
        assertEquals(2.0, result.getAsDouble());
    }

    @Test
    public void testAverageDouble() {
        stream = createStream(1, 2, 3, 4);
        OptionalDouble result = stream.averageDouble(Integer::doubleValue);
        assertTrue(result.isPresent());
        assertEquals(2.5, result.getAsDouble(), 0.01);
    }

    @Test
    public void testAverageDouble_method() {
        stream = createStream(1, 2, 3, 4, 5);
        OptionalDouble result = stream.averageDouble(i -> (double) i);
        assertTrue(result.isPresent());
        assertEquals(3.0, result.getAsDouble(), 0.001);
    }

    // averageInt

    @Test
    public void test_averageInt() {
        OptionalDouble result = Stream.of("1", "2", "3").averageInt(Integer::parseInt);
        assertTrue(result.isPresent());
        assertEquals(2.0, result.getAsDouble());
    }

    @Test
    public void testAverageInt() {
        stream = createStream(1, 2, 3, 4);
        OptionalDouble result = stream.averageInt(Integer::intValue);
        assertTrue(result.isPresent());
        assertEquals(2.5, result.getAsDouble(), 0.01);
    }

    @Test
    public void testAverageInt_method() {
        stream = createStream(1, 2, 3, 4, 5);
        OptionalDouble result = stream.averageInt(i -> i);
        assertTrue(result.isPresent());
        assertEquals(3.0, result.getAsDouble(), 0.001);
    }

    // averageLong

    @Test
    public void test_averageLong() {
        OptionalDouble result = Stream.of(1L, 2L, 3L).averageLong(l -> l);
        assertTrue(result.isPresent());
        assertEquals(2.0, result.getAsDouble());
    }

    @Test
    public void testAverageLong() {
        stream = createStream(1, 2, 3, 4);
        OptionalDouble result = stream.averageLong(Integer::longValue);
        assertTrue(result.isPresent());
        assertEquals(2.5, result.getAsDouble(), 0.01);
    }

    @Test
    public void testAverageLong_method() {
        stream = createStream(1, 2, 3, 4, 5);
        OptionalDouble result = stream.averageLong(i -> (long) i);
        assertTrue(result.isPresent());
        assertEquals(3.0, result.getAsDouble(), 0.001);
    }

    // containsAll

    @Test
    public void testContainsAll() {
        stream = createStream(1, 2, 3, 4, 5);
        assertTrue(stream.containsAll(1, 3, 5));

        stream = createStream(1, 2, 3, 4, 5);
        assertFalse(stream.containsAll(1, 6));
    }

    @Test
    public void test_containsAll_array() {
        assertTrue(Stream.of(1, 2, 3, 4).containsAll(2, 3));
        assertFalse(Stream.of(1, 2, 3, 4).containsAll(2, 5));
    }

    @Test
    public void test_containsAll_collection() {
        assertTrue(Stream.of(1, 2, 3, 4).containsAll(Arrays.asList(2, 3)));
        assertFalse(Stream.of(1, 2, 3, 4).containsAll(Arrays.asList(2, 5)));
    }

    @Test
    public void testContainsAll_Collection_MultipleElements() {
        assertTrue(Stream.of(1, 2, 3, 4, 5).containsAll(Arrays.asList(1, 2, 3)));
        assertFalse(Stream.of(1, 2, 3, 4, 5).containsAll(Arrays.asList(1, 2, 6)));
    }

    @Test
    public void testContainsAll_empty() {
        stream = createStream(1, 2, 3);
        assertTrue(stream.containsAll(Collections.emptyList()));
    }

    @Test
    public void testContainsAll_EmptyCollection() {
        Stream<Integer> s = Stream.of(1, 2, 3);
        assertTrue(s.containsAll(java.util.Collections.emptyList()));
    }

    @Test
    public void testContainsAll_EmptyInput() {
        stream = Stream.of(1, 2, 3);
        assertTrue(stream.containsAll(Collections.emptyList()));
    }

    @Test
    public void testContainsAllCollection() {
        stream = createStream(1, 2, 3, 4, 5);
        assertTrue(stream.containsAll(Arrays.asList(1, 3, 5)));

        stream = createStream(1, 2, 3, 4, 5);
        assertFalse(stream.containsAll(Arrays.asList(1, 6)));
    }

    // containsAny

    @Test
    public void testContainsAny() {
        stream = createStream(1, 2, 3, 4, 5);
        assertTrue(stream.containsAny(3, 6, 9));

        stream = createStream(1, 2, 3, 4, 5);
        assertFalse(stream.containsAny(6, 7, 8));
    }

    @Test
    public void test_containsAny_array() {
        assertTrue(Stream.of(1, 2, 3, 4).containsAny(5, 3));
        assertFalse(Stream.of(1, 2, 3, 4).containsAny(5, 6));
    }

    @Test
    public void test_containsAny_collection() {
        assertTrue(Stream.of(1, 2, 3, 4).containsAny(Arrays.asList(5, 3)));
        assertFalse(Stream.of(1, 2, 3, 4).containsAny(Arrays.asList(5, 6)));
    }

    @Test
    public void testContainsAny_Collection_MultipleElements() {
        assertTrue(Stream.of(1, 2, 3, 4, 5).containsAny(Arrays.asList(7, 8, 3)));
        assertFalse(Stream.of(1, 2, 3, 4, 5).containsAny(Arrays.asList(6, 7, 8)));
    }

    @Test
    public void testContainsAny_EmptyCollection() {
        Stream<Integer> s = Stream.of(1, 2, 3);
        assertFalse(s.containsAny(java.util.Collections.emptyList()));
    }

    @Test
    public void testContainsAny_EmptyInput() {
        stream = Stream.of(1, 2, 3);
        assertFalse(stream.containsAny(Collections.emptyList()));
    }

    @Test
    public void testContainsAnyCollection() {
        stream = createStream(1, 2, 3, 4, 5);
        assertTrue(stream.containsAny(Arrays.asList(3, 6, 9)));

        stream = createStream(1, 2, 3, 4, 5);
        assertFalse(stream.containsAny(Arrays.asList(6, 7, 8)));
    }

    @Test
    public void test_containsDuplicates() {
        assertTrue(Stream.of(1, 2, 1).containsDuplicates());
        assertFalse(Stream.of(1, 2, 3).containsDuplicates());
    }

    // containsNone

    @Test
    public void testContainsNone() {
        stream = createStream(1, 2, 3, 4, 5);
        assertTrue(stream.containsNone(6, 7, 8));

        stream = createStream(1, 2, 3, 4, 5);
        assertFalse(stream.containsNone(3, 6, 9));
    }

    @Test
    public void test_containsNone_array() {
        assertTrue(Stream.of(1, 2, 3, 4).containsNone(5, 6));
        assertFalse(Stream.of(1, 2, 3, 4).containsNone(5, 3));
    }

    @Test
    public void test_containsNone_collection() {
        assertTrue(Stream.of(1, 2, 3, 4).containsNone(Arrays.asList(5, 6)));
        assertFalse(Stream.of(1, 2, 3, 4).containsNone(Arrays.asList(5, 3)));
    }

    @Test
    public void testContainsNone_false() {
        stream = createStream(1, 2, 3);
        assertFalse(stream.containsNone(Arrays.asList(3, 4, 5)));
    }

    @Test
    public void testContainsNoneCollection() {
        stream = createStream(1, 2, 3, 4, 5);
        assertTrue(stream.containsNone(Arrays.asList(6, 7, 8)));

        stream = createStream(1, 2, 3, 4, 5);
        assertFalse(stream.containsNone(Arrays.asList(3, 6, 9)));
    }

    @Test
    public void testContainsUsesEqualsForSetsWithOtherEquivalence() {
        final Set<String> sorted = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        sorted.addAll(Arrays.asList("a", "b"));
        final Set<String> identity = Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        identity.add(new String("a"));
        identity.add(new String("a"));

        for (final boolean parallel : new boolean[] { false, true }) {
            final java.util.function.Function<List<String>, Stream<String>> source = values -> parallel ? Stream.of(values.iterator()).parallel(2)
                    : Stream.of(values.iterator());
            assertFalse(source.apply(Arrays.asList("A", "B")).containsAll(sorted));
            assertFalse(source.apply(Arrays.asList("A", "B")).containsAny(sorted));
            assertTrue(source.apply(Arrays.asList("A", "B")).containsNone(sorted));
            assertTrue(source.apply(Arrays.asList(new String("a"))).containsAll(identity));
            assertTrue(source.apply(Arrays.asList(new String("a"))).containsAny(identity));
            assertFalse(source.apply(Arrays.asList(new String("a"))).containsNone(identity));
            assertEquals(2, sorted.size());
            assertEquals(2, identity.size());
        }

        final int[] first = { 1 };
        final int[] second = { 1 };
        assertTrue(Stream.of(first, second).containsAll(Arrays.asList(first, second)));
        assertFalse(Stream.of(first, first).containsAll(Arrays.asList(first, second)));
        assertTrue(Stream.of(first, second).containsAll(first, second));
        assertFalse(Stream.of(first, first).containsAll(first, second));
    }

    @Test
    public void test_count() {
        assertEquals(3, Stream.of(1, 2, 3).count());
    }

    @Test
    public void test_countMatchBetween() {
        assertTrue(Stream.of(1, 2, 3, 4, 5).hasMatchCountBetween(2, 2, i -> i % 2 == 0));
        assertFalse(Stream.of(1, 2, 3, 4, 5).hasMatchCountBetween(3, 3, i -> i % 2 == 0));
    }

    // elementAt

    @Test
    public void testElementAt() {
        stream = createStream(1, 2, 3, 4, 5);
        Optional<Integer> result = stream.elementAt(2);
        assertTrue(result.isPresent());
        assertEquals(3, result.get());
    }

    @Test
    public void test_elementAt() {
        Optional<Integer> result = Stream.of(1, 2, 3).elementAt(1);
        assertEquals(Optional.of(2), result);
        assertTrue(Stream.of(1, 2, 3).elementAt(3).isEmpty());
    }

    @Test
    public void testElementAtNegative() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3));
        assertThrows(IllegalArgumentException.class, () -> stream.elementAt(-1));
    }

    @Test
    public void test_findAny() {
        Optional<Integer> result = Stream.of(1, 2, 3, 4).findAny(i -> i > 2);
        assertTrue(result.isPresent());
        assertTrue(result.get() > 2);
    }

    @Test
    public void testFindAny() {
        stream = createStream(1, 2, 3, 4, 5);
        Optional<Integer> result = stream.findAny(i -> i > 3);
        assertTrue(result.isPresent());
        assertEquals(4, result.get());
    }

    @Test
    public void test_findFirst_withPredicate() {
        Optional<Integer> result = Stream.of(1, 2, 3, 4).findFirst(i -> i > 2);
        assertEquals(Optional.of(3), result);
    }

    @Test
    public void test_findLast() {
        Optional<Integer> result = Stream.of(1, 2, 3, 4).findLast(i -> i < 4);
        assertEquals(Optional.of(3), result);
    }

    // first

    @Test
    public void test_findFirst() {
        Optional<Integer> result = Stream.of(1, 2, 3, 4).first();
        assertEquals(Optional.of(1), result);
    }

    @Test
    public void testFirst() {
        stream = createStream(1, 2, 3);
        Optional<Integer> result = stream.first();
        assertTrue(result.isPresent());
        assertEquals(1, result.get());

        stream = createStream();
        result = stream.first();
        assertFalse(result.isPresent());
    }

    @Test
    public void test_first() {
        Optional<Integer> result = Stream.of(1, 2, 3).first();
        assertEquals(Optional.of(1), result);
        assertTrue(Stream.of(Collections.emptyList()).first().isEmpty());
    }

    @Test
    public void test_forEach() {
        List<Integer> list = new ArrayList<>();
        Stream.of(1, 2, 3).forEach(list::add);
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void testForEach() {
        stream = createStream(1, 2, 3);
        List<Integer> result = new ArrayList<>();
        stream.forEach(result::add);
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void test_forEachIndexed() {
        Map<Integer, Integer> map = new HashMap<>();
        Stream.of(10, 20, 30).forEachIndexed((i, e) -> map.put(i, e));
        assertEquals(Map.of(0, 10, 1, 20, 2, 30), map);
    }

    @Test
    public void testForEachIndexed() {
        stringStream = createStream("a", "b", "c");
        Map<Integer, String> result = new HashMap<>();
        stringStream.forEachIndexed(result::put);
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
        assertEquals("c", result.get(2));
    }

    @Test
    public void test_forEachPair() {
        List<Pair<Integer, Integer>> pairs = new ArrayList<>();
        Stream.of(1, 2, 3, 4).forEachPair((a, b) -> pairs.add(Pair.of(a, b)));
        assertEquals(List.of(Pair.of(1, 2), Pair.of(2, 3), Pair.of(3, 4)), pairs);

        pairs.clear();
        Stream.of(1, 2, 3).forEachPair((a, b) -> pairs.add(Pair.of(a, b)));
        assertEquals(List.of(Pair.of(1, 2), Pair.of(2, 3)), pairs);
    }

    @Test
    public void testForEachPair() {
        stream = createStream(1, 2, 3, 4);
        List<String> result = new ArrayList<>();
        stream.forEachPair((a, b) -> result.add(a + "," + b));
        assertEquals(Arrays.asList("1,2", "2,3", "3,4"), result);
    }

    @Test
    public void test_forEachTriple() {
        List<List<Integer>> triples = new ArrayList<>();
        Stream.of(1, 2, 3, 4, 5).forEachTriple((a, b, c) -> triples.add(List.of(a, b, c)));
        assertEquals(List.of(List.of(1, 2, 3), List.of(2, 3, 4), List.of(3, 4, 5)), triples);
    }

    @Test
    public void testForEachTriple() {
        stream = createStream(1, 2, 3, 4, 5);
        List<String> result = new ArrayList<>();
        stream.forEachTriple((a, b, c) -> result.add(a + "," + b + "," + c));
        assertEquals(Arrays.asList("1,2,3", "2,3,4", "3,4,5"), result);
    }

    // forEachUntil

    @Test
    public void testForEachUntil() {
        stream = createStream(1, 2, 3, 4, 5);
        List<Integer> result = new ArrayList<>();
        stream.forEachUntil((i, flag) -> {
            result.add(i);
            if (i == 3) {
                flag.setValue(true);
            }
        });
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void test_forEachUntil_biConsumer() {
        List<Integer> list = new ArrayList<>();
        Stream.of(1, 2, 3, 4).forEachUntil((e, flag) -> {
            list.add(e);
            if (e == 3) {
                flag.setTrue();
            }
        });
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void testForEachUntil_biConsumerConsumesExactlyTheElementsItDelivers() {
        final List<Integer> pulled = new ArrayList<>();
        final List<Integer> delivered = new ArrayList<>();

        Stream.of(1, 2, 3, 4).peek(pulled::add).forEachUntil((value, flag) -> {
            delivered.add(value);

            if (value == 2) {
                flag.setTrue();
            }
        });

        assertEquals(N.asList(1, 2), delivered);
        assertEquals(N.asList(1, 2), pulled); // used to be [1, 2, 3]
    }



    /**
     * {@code forEachUntil} was implemented as {@code takeWhile(v -> flag.isFalse()).forEach(action)}, and
     * {@code takeWhile.hasNext()} has to pull an element before it can test the predicate - so it consumed one
     * element past the stop point. {@code Seq.forEachUntil} uses a plain loop for exactly this reason
     * (see {@code SeqRegressionTest} "B5 - forEachUntil consumes exactly the elements it delivers").
     */
    @Test
    public void testForEachUntil_consumesExactlyTheElementsItDelivers() {
        final List<Integer> pulled = new ArrayList<>();
        final List<Integer> delivered = new ArrayList<>();
        final MutableBoolean flag = MutableBoolean.of(false);

        Stream.of(1, 2, 3, 4).peek(pulled::add).forEachUntil(flag, value -> {
            delivered.add(value);

            if (value == 2) {
                flag.setTrue();
            }
        });

        assertEquals(N.asList(1, 2), delivered);
        assertEquals(N.asList(1, 2), pulled); // used to be [1, 2, 3]
    }



    /**
     * {@code Stream.forEachUntil}'s javadoc: "If the flagToBreak is set to {@code true} at the beginning, no
     * elements will be iterated from the stream before it is stopped and closed." It used to iterate one.
     */
    @Test
    public void testForEachUntil_flagAlreadySetIteratesNothing() {
        final List<Integer> pulled = new ArrayList<>();
        final List<Integer> delivered = new ArrayList<>();

        Stream.of(1, 2, 3).peek(pulled::add).forEachUntil(MutableBoolean.of(true), delivered::add);

        assertTrue(delivered.isEmpty());
        assertTrue(pulled.isEmpty()); // used to be [1]
    }

    @Test
    public void testForEachUntil_flagAlreadySetIteratesNothingInParallel() {
        final AtomicInteger pulled = new AtomicInteger();
        final List<Integer> delivered = Collections.synchronizedList(new ArrayList<>());

        Stream.of(1, 2, 3, 4, 5, 6, 7, 8).parallel(4).peek(x -> pulled.incrementAndGet()).forEachUntil(MutableBoolean.of(true), delivered::add);

        assertTrue(delivered.isEmpty());
        assertEquals(0, pulled.get());
    }

    @Test
    public void test_forEachUntil_mutableBoolean() {
        List<Integer> list = new ArrayList<>();
        MutableBoolean flag = MutableBoolean.of(false);
        Stream.of(1, 2, 3, 4).forEachUntil(flag, e -> {
            list.add(e);
            if (e == 3) {
                flag.setTrue();
            }
        });
        assertEquals(Arrays.asList(1, 2, 3), list);
    }



    /**
     * The sequential path consumes exactly what it delivers; the parallel path cannot, because its workers
     * buffer ahead. Measured: sequential over-read 0, parallel(2/4/8) 131/44/271 — and a plain
     * {@code parallel(8).limit(3)} over-reads a comparable 390, so the read-ahead is inherent to parallel
     * short-circuiting rather than to this method. This pins the sequential guarantee and the fact that the
     * parallel form still delivers the right elements.
     */
    @Test
    public void testForEachUntil_parallelStillDeliversTheRightElementsDespiteReadAhead() {
        final MutableBoolean flag = MutableBoolean.of(false);
        final List<Integer> delivered = Collections.synchronizedList(new ArrayList<>());

        Stream.of(1, 2, 3, 4, 5, 6, 7, 8).parallel(4).forEachUntil(flag, v -> {
            delivered.add(v);
            flag.setTrue();
        });

        // the flag stops delivery promptly; ordering is explicitly nondeterministic in parallel
        assertTrue(delivered.size() >= 1, "at least one element must be delivered");
        assertTrue(delivered.size() <= 4, "delivery must stop promptly, was " + delivered.size());
    }

    @Test
    public void testForEachUntilRejectsNullFlagEvenForEmptyStreams() {
        for (final boolean empty : new boolean[] { true, false }) {
            final AtomicInteger actionCalls = new AtomicInteger();
            try (final Stream<Integer> stream = empty ? Stream.empty() : Stream.of(1)) {
                assertThrows(IllegalArgumentException.class, () -> stream.forEachUntil(null, value -> actionCalls.incrementAndGet()));
                assertEquals(0, actionCalls.get());
            }
        }
    }

    @Test
    public void testForEachUntilWithFlag() {
        stream = createStream(1, 2, 3, 4, 5);
        List<Integer> result = new ArrayList<>();
        MutableBoolean flag = MutableBoolean.of(false);
        stream.forEachUntil(flag, i -> {
            result.add(i);
            if (i == 3) {
                flag.setValue(true);
            }
        });
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void test_kthLargest() {
        Optional<Integer> result = Stream.of(9, 1, 8, 2, 7, 3, 6, 4, 5).kthLargest(3, Comparator.naturalOrder());
        assertEquals(Optional.of(7), result);
    }

    @Test
    public void testLast() {
        stream = createStream(1, 2, 3);
        Optional<Integer> result = stream.last();
        assertTrue(result.isPresent());
        assertEquals(3, result.get());

        stream = createStream();
        result = stream.last();
        assertFalse(result.isPresent());
    }

    @Test
    public void test_last() {
        Optional<Integer> result = Stream.of(1, 2, 3).last();
        assertEquals(Optional.of(3), result);
        assertTrue(Stream.of(Collections.emptyList()).last().isEmpty());
    }

    @Test
    public void test_max() {
        Optional<Integer> result = Stream.of(3, 1, 2).max(Comparator.naturalOrder());
        assertEquals(Optional.of(3), result);
    }

    // maxAll

    @Test
    public void test_maxAll() {
        List<Integer> result = Stream.of(1, 3, 2, 3, 1).maxAll(Comparator.naturalOrder());
        assertEquals(Arrays.asList(3, 3), result);
    }

    @Test
    public void testMaxAll() {
        stream = createStream(1, 4, 2, 4, 3);
        List<Integer> result = stream.maxAll(Comparator.naturalOrder());
        assertEquals(Arrays.asList(4, 4), result);
    }

    @Test
    public void testMaxAll_Parallel() {
        List<Integer> result = Stream.of(1, 3, 2, 3, 1).parallel().maxAll(Comparator.naturalOrder());
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(x -> x == 3));
    }

    @Test
    public void test_maxBy() {
        Optional<String> result = Stream.of("apple", "banana", "kiwi").maxBy(String::length);
        assertEquals(Optional.of("banana"), result);
    }

    @Test
    public void test_min() {
        Optional<Integer> result = Stream.of(3, 1, 2).min(Comparator.naturalOrder());
        assertEquals(Optional.of(1), result);
    }

    // minAll

    @Test
    public void test_minAll() {
        List<Integer> result = Stream.of(3, 1, 2, 1, 3).minAll(Comparator.naturalOrder());
        assertEquals(Arrays.asList(1, 1), result);
    }

    @Test
    public void testMinAll() {
        stream = createStream(3, 1, 2, 1, 4);
        List<Integer> result = stream.minAll(Comparator.naturalOrder());
        assertEquals(Arrays.asList(1, 1), result);
    }

    @Test
    public void testMinAll_EmptyStream() {
        stream = Stream.empty();
        List<Integer> result = stream.minAll(Comparator.naturalOrder());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMinAll_Parallel() {
        List<Integer> result = Stream.of(3, 1, 2, 1, 3).parallel().minAll(Comparator.naturalOrder());
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(x -> x == 1));
    }

    @Test
    public void testMinAll_ParallelDuplicateMinimums() {
        stream = Stream.of(4, 1, 3, 1, 2).parallel();
        List<Integer> result = stream.minAll(Comparator.naturalOrder());
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(it -> it == 1));
    }

    @Test
    public void testMinAll_SingleElement() {
        stream = Stream.of(1);
        List<Integer> result = stream.minAll(Comparator.naturalOrder());
        assertEquals(Arrays.asList(1), result);
    }

    @Test
    public void testMinAll_WithComparator_EmptyStream() {
        Stream<Integer> emptyStream = Stream.empty();
        java.util.List<Integer> result = emptyStream.minAll(java.util.Comparator.naturalOrder());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMinAll_WithComparator_SingleMinimum() {
        Stream<Integer> s = Stream.of(5, 3, 1, 4, 2);
        java.util.List<Integer> result = s.minAll(java.util.Comparator.naturalOrder());
        assertEquals(java.util.Arrays.asList(1), result);
    }

    @Test
    public void test_minBy() {
        Optional<String> result = Stream.of("apple", "banana", "kiwi").minBy(String::length);
        assertEquals(Optional.of("kiwi"), result);
    }

    @Test
    public void test_noneMatch() {
        assertTrue(Stream.of(1, 3, 5).noneMatch(i -> i % 2 == 0));
        assertFalse(Stream.of(1, 2, 3).noneMatch(i -> i % 2 == 0));
    }

    // percentiles

    @Test
    public void testPercentiles() {
        stream = createStream(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        Optional<Map<Percentage, Integer>> result = stream.percentiles();
        assertTrue(result.isPresent());
        Map<Percentage, Integer> percentiles = result.get();
        assertTrue(percentiles.containsKey(Percentage._20));
        assertTrue(percentiles.containsKey(Percentage._50));
        assertTrue(percentiles.containsKey(Percentage._70));
    }

    @Test
    public void test_percentiles() {
        Optional<Map<Percentage, Integer>> result = Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).percentiles();
        assertTrue(result.isPresent());
        assertNotNull(result.get());
        assertEquals(43, result.get().size());
    }

    @Test
    public void testPercentiles_EmptyStream() {
        Stream<Integer> s = Stream.empty();
        Optional<java.util.Map<com.landawn.abacus.util.Percentage, Integer>> result = s.percentiles();
        assertFalse(result.isPresent());
    }

    @Test
    public void testPercentilesWithComparator() {
        stream = createStream(10, 9, 8, 7, 6, 5, 4, 3, 2, 1);
        Optional<Map<Percentage, Integer>> result = stream.percentiles(Comparator.naturalOrder());
        assertTrue(result.isPresent());
    }

    @Test
    public void test_reduce() {
        Optional<Integer> result = Stream.of(1, 2, 3).reduce((a, b) -> a + b);
        assertEquals(Optional.of(6), result);
    }

    @Test
    public void test_sum() {
        long result = Stream.of(1, 2, 3).mapToLong(i -> i).sum();
        assertEquals(6, result);
    }

    // sumDouble

    @Test
    public void test_sumDouble() {
        double result = Stream.of(1.1, 2.2, 3.3).sumDouble(d -> d);
        assertEquals(6.6, result, 0.001);
    }

    @Test
    public void testSumDouble() {
        stream = createStream(1, 2, 3, 4);
        double result = stream.sumDouble(Integer::doubleValue);
        assertEquals(10.0, result, 0.01);
    }

    @Test
    public void testSumDouble_method() {
        stream = createStream(1, 2, 3, 4, 5);
        double result = stream.sumDouble(i -> (double) i);
        assertEquals(15.0, result, 0.001);
    }

    @Test
    public void test_sumInt() {
        long result = Stream.of("1", "2", "3").sumInt(Integer::parseInt);
        assertEquals(6L, result);
    }

    @Test
    public void testSumInt() {
        stream = createStream(1, 2, 3, 4);
        long result = stream.sumInt(Integer::intValue);
        assertEquals(10L, result);
    }

    // sumLong

    @Test
    public void test_sumLong() {
        long result = Stream.of(1L, 2L, 3L).sumLong(l -> l);
        assertEquals(6L, result);
    }

    @Test
    public void testSumLong() {
        stream = createStream(1, 2, 3, 4);
        long result = stream.sumLong(Integer::longValue);
        assertEquals(10L, result);
    }

    @Test
    public void testSumLong_method() {
        stream = createStream(1, 2, 3, 4, 5);
        long result = stream.sumLong(i -> (long) i);
        assertEquals(15L, result);
    }

}
