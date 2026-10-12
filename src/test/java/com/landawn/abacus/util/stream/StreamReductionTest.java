package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToDoubleFunction;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u;

/** Stream reduction contracts. */
public class StreamReductionTest extends StreamTestSupport {

    @Test
    public void testAcceptIfNotEmpty() {
        List<Integer> holder = new ArrayList<>();
        Stream.of(1, 2, 3).acceptIfNotEmpty(s -> s.forEach(holder::add));
        assertEquals(Arrays.asList(1, 2, 3), holder);
    }

    @Test
    public void testAcceptIfNotEmptyWithEmpty() {
        List<Integer> holder = new ArrayList<>();
        Stream.<Integer> empty().acceptIfNotEmpty(s -> s.forEach(holder::add));
        assertTrue(holder.isEmpty());
    }

    @Test
    public void testAllMatch() {
        assertTrue(Stream.of(2, 4, 6).allMatch(n -> n % 2 == 0));
        assertFalse(Stream.of(2, 3, 4).allMatch(n -> n % 2 == 0));
        assertTrue(Stream.<Integer> empty().allMatch(n -> false));
    }

    @Test
    public void testAnyMatch() {
        assertTrue(Stream.of(1, 2, 3, 4).anyMatch(n -> n > 3));
        assertFalse(Stream.of(1, 2, 3).anyMatch(n -> n > 5));
        assertFalse(Stream.<Integer> empty().anyMatch(n -> true));
    }

    @Test
    public void testApplyIfNotEmpty() {
        long result = Stream.of(1, 2, 3).applyIfNotEmpty(s -> s.count()).get();
        assertEquals(3L, result);
    }

    @Test
    public void testApplyIfNotEmptyWithEmpty() {
        assertFalse(Stream.<Integer> empty().applyIfNotEmpty(s -> s.count()).isPresent());
    }

    @Test
    public void test_average() {
        final ToDoubleFunction<String> mapper = String::hashCode;

        Stream.of("a", "b", "c", "a", "d").collect(Collectors.averagingDoubleOrElseThrow(mapper));
        assertNotNull(mapper);
    }

    @Test
    public void testAverageDouble() {
        assertEquals(u.OptionalDouble.of(3.0), Stream.of(1, 2, 3, 4, 5).averageDouble(n -> n));
    }

    @Test
    public void testAverageInt() {
        assertEquals(u.OptionalDouble.of(3.0), Stream.of(1, 2, 3, 4, 5).averageInt(n -> n));
    }

    @Test
    public void testAverageInt_Empty() {
        OptionalDouble result = Stream.<String> empty().averageInt(String::length);
        assertFalse(result.isPresent());
    }

    @Test
    public void testAverageLong() {
        assertEquals(u.OptionalDouble.of(3.0), Stream.of(1, 2, 3, 4, 5).averageLong(n -> n));
    }

    // containsAll

    @Test
    public void testContainsAll() {
        assertTrue(Stream.of(1, 2, 3, 4, 5).containsAll(Arrays.asList(2, 3, 4)));
        assertFalse(Stream.of(1, 2, 3).containsAll(Arrays.asList(3, 4, 5)));
    }

    @Test
    public void testContainsAllArray() {
        assertTrue(Stream.of(1, 2, 3, 4).containsAll(2, 4));
        assertFalse(Stream.of(1, 2, 3).containsAll(2, 5));
        assertTrue(Stream.of(1, 2, 3).containsAll());
    }

    @Test
    public void testContainsAllCollection() {
        assertTrue(Stream.of(1, 2, 3, 4).containsAll(Arrays.asList(2, 4)));
        assertFalse(Stream.of(1, 2, 3).containsAll(Arrays.asList(2, 5)));
    }

    @Test
    public void testContainsAny() {
        assertTrue(Stream.of(1, 2, 3).containsAny(Arrays.asList(3, 4, 5)));
        assertFalse(Stream.of(1, 2, 3).containsAny(Arrays.asList(4, 5, 6)));
    }

    @Test
    public void testContainsAnyArray() {
        assertTrue(Stream.of(1, 2, 3).containsAny(3, 5, 6));
        assertFalse(Stream.of(1, 2, 3).containsAny(4, 5, 6));
    }

    @Test
    public void testContainsDuplicates() {
        assertTrue(Stream.of(1, 2, 3, 2, 4).containsDuplicates());
        assertFalse(Stream.of(1, 2, 3, 4, 5).containsDuplicates());
        assertFalse(Stream.<Integer> empty().containsDuplicates());
    }

    @Test
    public void testContainsDuplicates_SingleElement() {
        assertFalse(Stream.of(1).containsDuplicates());
    }

    @Test
    public void testContainsNone() {
        assertTrue(Stream.of(1, 2, 3).containsNone(Arrays.asList(4, 5, 6)));
        assertFalse(Stream.of(1, 2, 3).containsNone(Arrays.asList(3, 4, 5)));
    }

    @Test
    public void testContainsNoneArray() {
        assertTrue(Stream.of(1, 2, 3).containsNone(4, 5, 6));
        assertFalse(Stream.of(1, 2, 3).containsNone(3, 5, 6));
    }

    @Test
    public void testCount() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).count());
        assertEquals(0, Stream.empty().count());
    }

    // countBy

    @Test
    public void testCountBy() {
        Map<Integer, Long> map = Stream.of("a", "bb", "c", "dd", "eee").countBy(String::length).toMap(e -> e.getKey(), e -> Long.valueOf(e.getValue()));
        assertEquals(Long.valueOf(2), map.get(1));
        assertEquals(Long.valueOf(2), map.get(2));
        assertEquals(Long.valueOf(1), map.get(3));
    }

    @Test
    public void testCountBy_WithMapFactory() {
        Map<Integer, Integer> result = Stream.of("a", "bb", "c", "dd", "eee")
                .countBy(String::length, LinkedHashMap::new)
                .toMap(e -> e.getKey(), e -> e.getValue());
        assertEquals(3, result.size());
        assertEquals(2, (int) result.get(1));
        assertEquals(2, (int) result.get(2));
        assertEquals(1, (int) result.get(3));
    }

    @Test
    public void testCountByEmpty() {
        Map<Integer, Long> map = Stream.<String> empty().countBy(String::length).toMap(e -> e.getKey(), e -> Long.valueOf(e.getValue()));
        assertTrue(map.isEmpty());
    }

    // countByToEntry

    @Test
    public void testCountByToEntry() {
        List<Map.Entry<Integer, Integer>> list = Stream.of("a", "bb", "c", "dd", "eee").countByToEntry(String::length).toList();
        assertEquals(3, list.size());
        assertTrue(list.stream().anyMatch(e -> e.getKey() == 1 && e.getValue() == 2));
        assertTrue(list.stream().anyMatch(e -> e.getKey() == 2 && e.getValue() == 2));
        assertTrue(list.stream().anyMatch(e -> e.getKey() == 3 && e.getValue() == 1));
    }

    @Test
    public void testCountByToEntry_WithMapFactory() {
        List<Map.Entry<Integer, Integer>> result = Stream.of("a", "bb", "c", "dd").countByToEntry(String::length, LinkedHashMap::new).toList();
        assertFalse(result.isEmpty());
        assertEquals(2, result.size());
    }

    @Test
    public void testCountByToEntryEmpty() {
        List<Map.Entry<Integer, Integer>> list = Stream.<String> empty().countByToEntry(String::length).toList();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testElementAt() {
        assertEquals(Integer.valueOf(2), Stream.of(1, 2, 3).elementAt(1).get());
        assertFalse(Stream.of(1, 2, 3).elementAt(5).isPresent());
        assertFalse(Stream.<Integer> empty().elementAt(0).isPresent());
    }

    // findAny

    @Test
    public void testFindAny() {
        u.Optional<Integer> result = Stream.of(1, 2, 3).findAny();
        assertTrue(result.isPresent());
        assertTrue(Arrays.asList(1, 2, 3).contains(result.get()));
    }

    @Test
    public void testFindAny_alias() {
        Optional<Integer> result = Stream.of(1, 2, 3).findAny();
        assertTrue(result.isPresent());
    }

    @Test
    public void testFindAny_empty() {
        Optional<Integer> result = Stream.<Integer> empty().findAny();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindAnyPredicate() {
        assertEquals(Optional.of(2), Stream.of(1, 2, 3, 4).findAny(x -> x % 2 == 0));
        Optional<Integer> result = Stream.of(1, 2, 3, 4).parallel().findAny(x -> x % 2 == 0);
        assertTrue(result.isPresent() && result.get() % 2 == 0);
    }

    // findFirst

    @Test
    public void testFindFirst() {
        assertEquals(Integer.valueOf(1), Stream.of(1, 2, 3).findFirst().get());
        assertFalse(Stream.<Integer> empty().findFirst().isPresent());
    }

    @Test
    public void testFindFirst_alias() {
        Optional<Integer> result = Stream.of(1, 2, 3).findFirst();
        assertTrue(result.isPresent());
        assertEquals(1, result.get().intValue());
    }

    @Test
    public void testFindFirst_empty() {
        Optional<Integer> result = Stream.<Integer> empty().findFirst();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindFirstPredicate() {
        assertEquals(Optional.of(2), Stream.of(1, 2, 3, 4).findFirst(x -> x % 2 == 0));
        assertEquals(Optional.empty(), Stream.of(1, 3, 5).findFirst(x -> x % 2 == 0));
    }

    @Test
    public void testFindFirstWithPredicate() {
        assertEquals(Integer.valueOf(3), Stream.of(1, 2, 3, 4).findFirst(n -> n > 2).get());
        assertFalse(Stream.of(1, 2, 3).findFirst(n -> n > 5).isPresent());
    }

    @Test
    public void testFindLastPredicate() {
        assertEquals(Optional.of(4), Stream.of(1, 2, 3, 4).findLast(x -> x % 2 == 0));
        assertEquals(Optional.empty(), Stream.of(1, 3, 5).findLast(x -> x % 2 == 0));
        assertEquals(Optional.of(2), Stream.of(2).findLast(x -> x % 2 == 0));
    }

    @Test
    public void testFindLastWithPredicate() {
        assertEquals(Integer.valueOf(4), Stream.of(1, 2, 3, 4).findLast(n -> n > 2).get());
        assertFalse(Stream.of(1, 2, 3).findLast(n -> n > 5).isPresent());
    }

    @Test
    public void testFirst() {
        assertEquals(Integer.valueOf(1), Stream.of(1, 2, 3).first().get());
        assertFalse(Stream.<Integer> empty().first().isPresent());
    }

    // foldLeft

    @Test
    public void testFoldLeft() {
        String result = Stream.of("a", "b", "c", "d").foldLeft((a, b) -> a + b).get();
        assertEquals("abcd", result);
    }

    @Test
    public void testFoldLeft_Empty() {
        Optional<String> result = Stream.<String> empty().foldLeft((a, b) -> a + b);
        assertFalse(result.isPresent());
    }

    @Test
    public void testFoldLeft_SingleElement() {
        Optional<Integer> result = Stream.of(42).foldLeft((a, b) -> a + b);
        assertTrue(result.isPresent());
        assertEquals(42, result.get().intValue());
    }

    @Test
    public void testFoldLeft_WithIdentity() {
        String result = Stream.of("a", "b", "c").foldLeft("", (acc, s) -> acc + s);
        assertEquals("abc", result);
    }

    @Test
    public void testFoldLeftEmpty() {
        assertFalse(Stream.<String> empty().foldLeft((a, b) -> a + b).isPresent());
    }

    @Test
    public void testFoldLeftWithIdentity() {
        String result = Stream.of("a", "b", "c").foldLeft("start:", (a, b) -> a + b);
        assertEquals("start:abc", result);
    }

    // foldRight

    @Test
    public void testFoldRight() {
        String result = Stream.of("a", "b", "c", "d").foldRight((a, b) -> a + b).get();
        assertEquals("dcba", result);
    }

    @Test
    public void testFoldRight_SingleElement() {
        Optional<Integer> result = Stream.of(42).foldRight((a, b) -> a + b);
        assertTrue(result.isPresent());
        assertEquals(42, result.get().intValue());
    }

    @Test
    public void testFoldRight_WithIdentity() {
        String result = Stream.of("a", "b", "c").foldRight("", (acc, s) -> acc + s);
        assertEquals("cba", result);
    }

    @Test
    public void testFoldRightEmpty() {
        assertFalse(Stream.<String> empty().foldRight((a, b) -> a + b).isPresent());
    }

    @Test
    public void testFoldRightWithIdentity() {
        String result = Stream.of("a", "b", "c").foldRight(":end", (a, b) -> a + b);
        assertEquals(":endcba", result);
    }

    // forEach

    @Test
    public void testForEach() {
        List<Integer> result = new ArrayList<>();
        Stream.of(1, 2, 3).forEach(result::add);
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testForEachEmpty() {
        List<Integer> result = new ArrayList<>();
        Stream.<Integer> empty().forEach(result::add);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testForEachThrowing() {
        List<Integer> collected = new ArrayList<>();
        Stream.of(1, 2, 3).forEach(collected::add);
        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    @Test
    public void testForeachWithCompletion() {
        List<Integer> result = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        Stream.of(1, 2, 3).forEach(result::add, () -> completed.set(true));
        assertEquals(Arrays.asList(1, 2, 3), result);
        assertTrue(completed.get());
    }

    @Test
    public void testForEachWithFlatMapper() {
        List<String> collected = new ArrayList<>();
        stream.forEach(n -> Arrays.asList("a" + n, "b" + n), (n, s) -> collected.add(s));
        assertEquals(Arrays.asList("a1", "b1", "a2", "b2", "a3", "b3", "a4", "b4", "a5", "b5"), collected);
    }

    @Test
    public void testForEachWithOnComplete() {
        List<Integer> collected = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        stream.forEach(collected::add, () -> completed.set(true));
        assertEquals(testList, collected);
        assertTrue(completed.get());
    }

    @Test
    public void testForeach() {
        List<Integer> collected = new ArrayList<>();
        Stream.of(1, 2, 3).foreach(collected::add);
        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    @Test
    public void testForEachIndexed() {
        List<String> result = new ArrayList<>();
        Stream.of("a", "b", "c").forEachIndexed((i, s) -> result.add(i + ":" + s));
        assertEquals(Arrays.asList("0:a", "1:b", "2:c"), result);
    }

    @Test
    public void testForEachPair() {
        List<String> result = new ArrayList<>();
        Stream.of(1, 2, 3, 4).forEachPair((a, b) -> result.add(a + "-" + b));
        assertEquals(Arrays.asList("1-2", "2-3", "3-4"), result);
    }

    @Test
    public void testForEachTriple() {
        List<String> result = new ArrayList<>();
        Stream.of(1, 2, 3, 4).forEachTriple((a, b, c) -> result.add(a + "-" + b + "-" + c));
        assertEquals(Arrays.asList("1-2-3", "2-3-4"), result);
    }

    // forEachUntil

    @Test
    public void testForEachUntil() {
        List<Integer> collected = new ArrayList<>();
        MutableBoolean flagToBreak = MutableBoolean.of(false);
        Stream.of(1, 2, 3, 4, 5).forEachUntil(flagToBreak, n -> {
            collected.add(n);
            if (n >= 3) {
                flagToBreak.setTrue();
            }
        });
        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    @Test
    public void testForEachUntilBiConsumer() {
        List<Integer> collected = new ArrayList<>();
        Stream.of(1, 2, 3, 4, 5).forEachUntil((n, breakCondition) -> {
            collected.add(n);
            if (n >= 3) {
                breakCondition.setTrue();
            }
        });
        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    @Test
    public void testForEachUntilEmpty() {
        List<Integer> collected = new ArrayList<>();
        com.landawn.abacus.util.MutableBoolean flagToBreak = com.landawn.abacus.util.MutableBoolean.of(true);
        Stream.<Integer> empty().forEachUntil(flagToBreak, n -> {
            collected.add(n);
        });
        assertTrue(collected.isEmpty());
    }

    @Test
    public void testForEachUntilMutableBoolean() {
        List<Integer> collected = new ArrayList<>();
        MutableBoolean flag = MutableBoolean.of(false);
        Stream.of(1, 2, 3, 4, 5).forEachUntil(flag, val -> {
            collected.add(val);
            if (val == 3) {
                flag.setTrue();
            }
        });
        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    // kthLargest

    @Test
    public void testKthLargest() {
        assertEquals(Integer.valueOf(4), Stream.of(1, 2, 3, 4, 5).kthLargest(2, Comparator.naturalOrder()).get());
    }

    @Test
    public void testKthLargest_Empty() {
        Optional<Integer> result = Stream.<Integer> empty().kthLargest(1, Comparator.naturalOrder());
        assertFalse(result.isPresent());
    }

    @Test
    public void testKthLargest_Third() {
        Optional<Integer> result = Stream.of(5, 2, 8, 1, 9, 3).kthLargest(3, Comparator.naturalOrder());
        assertTrue(result.isPresent());
        assertEquals(5, result.get().intValue());
    }

    @Test
    public void testFindLast() {
        assertEquals(Integer.valueOf(3), Stream.of(1, 2, 3).last().get());
        assertFalse(Stream.<Integer> empty().last().isPresent());
    }

    @Test
    public void testLastN() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).last(3);
        assertEquals(Arrays.asList(3, 4, 5), stream.toList());
    }

    @Test
    public void testMax() {
        assertEquals(Integer.valueOf(3), Stream.of(1, 3, 2).max(Comparator.naturalOrder()).get());
        assertFalse(Stream.<Integer> empty().max(Comparator.naturalOrder()).isPresent());
    }

    @Test
    public void testMaxComparator() {
        assertEquals(Optional.of(5), Stream.of(5, 1, 3).max(Comparator.naturalOrder()));
    }

    // maxAll

    @Test
    public void testMaxAll() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 4).maxAll(Integer::compareTo);
        assertEquals(Arrays.asList(4, 4), result);
    }

    @Test
    public void testMaxAll_Empty() {
        List<Integer> result = Stream.<Integer> empty().maxAll(Comparator.naturalOrder());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMaxAllEmpty() {
        List<Integer> result = Stream.<Integer> empty().maxAll(Integer::compareTo);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMaxBy() {
        assertEquals("aaa", Stream.of("a", "bb", "aaa").maxBy(String::length).get());
    }

    @Test
    public void testMaxBy_empty() {
        Optional<String> result = Stream.<String> empty().maxBy(String::length);
        assertFalse(result.isPresent());
    }

    // maxWait

    @Test
    public void testMaxWait() {
        List<Integer> input = Arrays.asList(1, 2, 3);
        Stream<Integer> stream = Stream.of(input);

        List<Integer> result = stream.maxWait(Duration.ofMillis(50), -1).limit(5).toList();
        assertNotNull(result);
        assertTrue(result.size() > 0);
    }

    @Test
    public void testMaxWaitPreservesOrder() {
        Duration duration = Duration.ofMillis(50);
        Stream<Integer> orderedStream = Stream.of(1, 2, 3, 4, 5);

        List<Integer> result = orderedStream.maxWait(duration, -1).toList();

        int prev = Integer.MIN_VALUE;
        for (Integer val : result) {
            if (val > 0) {
                Assertions.assertTrue(val > prev);
                prev = val;
            }
        }
    }

    @Test
    public void testMaxWaitPropagatesUpstreamException() {
        // An exception thrown while fetching elements asynchronously must be propagated to the consumer,
        // not silently swallowed by truncating the stream.
        assertThrows(RuntimeException.class, () -> Stream.of(1, 2, 3, 4, 5).map(e -> {
            if (e == 3) {
                throw new IllegalStateException("failed to fetch element: " + e);
            }
            return e;
        }).maxWait(Duration.ofMillis(100), 99).toList());

        final Supplier<Integer> defaultSupplier = () -> 99;

        assertThrows(RuntimeException.class, () -> Stream.of(1, 2, 3, 4, 5).map(e -> {
            if (e == 3) {
                throw new IllegalStateException("failed to fetch element: " + e);
            }
            return e;
        }).maxWait(Duration.ofMillis(100), defaultSupplier).toList());
    }

    @Test
    public void testMaxWaitRejectsNegativeDuration() {
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1, 2, 3).maxWait(Duration.ofMillis(-1), 99));

        Supplier<Integer> defaultSupplier = () -> 99;
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1, 2, 3).maxWait(Duration.ofMillis(-1), defaultSupplier));
    }

    @Test
    public void testMaxWaitRejectsNullDuration() {
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1, 2, 3).maxWait((Duration) null, 99));

        Supplier<Integer> defaultSupplier = () -> 99;
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1, 2, 3).maxWait((Duration) null, defaultSupplier));
    }



    // TODO: Remaining Stream$5/Stream$6 gaps are anonymous iterator branches for async windowing/merge internals that require deterministic scheduler control beyond the current unit-test harness.
    @Test
    public void testMaxWaitRejectsZeroDuration() {
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1, 2, 3).maxWait(Duration.ZERO, 99));

        Supplier<Integer> defaultSupplier = () -> 99;
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1, 2, 3).maxWait(Duration.ZERO, defaultSupplier));
    }

    @Test
    public void testMaxWaitWithDurationAndDefaultValue() {
        Duration duration = Duration.ofMillis(100);
        Integer defaultValue = -1;

        Stream<Integer> result = stream.maxWait(duration, defaultValue);
        List<Integer> collected = result.toList();

        Assertions.assertNotNull(collected);
        Assertions.assertTrue(collected.size() > 0);
        for (Integer value : collected) {
            Assertions.assertTrue(testData.contains(value) || value.equals(defaultValue));
        }
    }

    @Test
    public void testMaxWaitWithDurationAndNullDefaultValue() {
        Duration duration = Duration.ofMillis(100);

        Stream<Integer> result = stream.maxWait(duration, (Integer) null);
        List<Integer> collected = result.toList();

        Assertions.assertNotNull(collected);
    }

    @Test
    public void testMaxWaitWithDurationAndSupplier() {
        Duration duration = Duration.ofMillis(100);
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<Integer> supplier = () -> -counter.incrementAndGet();

        Stream<Integer> result = stream.maxWait(duration, supplier);
        List<Integer> collected = result.toList();

        Assertions.assertNotNull(collected);
        Assertions.assertTrue(collected.size() > 0);
    }

    @Test
    public void testMaxWaitWithNullDuration() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            stream.maxWait(null, 0);
        });
    }

    @Test
    public void testMaxWaitWithNullSupplier() {
        Duration duration = Duration.ofMillis(50);
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Stream.of(1, 2, 3).delay(Duration.ofMillis(200)).maxWait(duration, (Supplier<Integer>) null).count();
        });
    }

    @Test
    public void testMaxWaitWithSupplier() {
        List<Integer> input = Arrays.asList(1, 2, 3);
        Stream<Integer> stream = Stream.of(input);
        AtomicInteger counter = new AtomicInteger(0);

        List<Integer> result = stream.maxWait(Duration.ofMillis(50), () -> -counter.incrementAndGet()).limit(5).toList();

        assertNotNull(result);
        assertTrue(result.size() > 0);
    }

    @Test
    public void testMin() {
        assertEquals(Integer.valueOf(1), Stream.of(3, 1, 2).min(Comparator.naturalOrder()).get());
        assertFalse(Stream.<Integer> empty().min(Comparator.naturalOrder()).isPresent());
    }

    @Test
    public void testMinComparator() {
        assertEquals(Optional.of(1), Stream.of(5, 1, 3).min(Comparator.naturalOrder()));
        assertEquals(Optional.empty(), Stream.<Integer> empty().min(Comparator.naturalOrder()));
    }

    // minAll

    @Test
    public void testMinAll() {
        List<Integer> result = Stream.of(1, 1, 2, 3, 4).minAll(Integer::compareTo);
        assertEquals(Arrays.asList(1, 1), result);
    }

    @Test
    public void testMinAll_Empty() {
        List<Integer> result = Stream.<Integer> empty().minAll(Comparator.naturalOrder());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMinAllEmpty() {
        List<Integer> result = Stream.<Integer> empty().minAll(Integer::compareTo);
        assertTrue(result.isEmpty());
    }

    // minBy

    @Test
    public void testMinBy() {
        assertEquals("c", Stream.of("aaa", "bb", "c").minBy(String::length).get());
    }

    @Test
    public void testMinBy_empty() {
        Optional<String> result = Stream.<String> empty().minBy(String::length);
        assertFalse(result.isPresent());
    }

    @Test
    public void testMinByAndMaxByWithOnlyNullKeys() {
        assertEquals("a", Stream.of("a", "b").minBy(value -> (String) null).get());
        assertEquals("a", Stream.of("a", "b").maxBy(value -> (String) null).get());
    }

    @Test
    public void testNoneMatch() {
        assertTrue(Stream.of(1, 3, 5).noneMatch(n -> n % 2 == 0));
        assertFalse(Stream.of(1, 2, 3).noneMatch(n -> n % 2 == 0));
        assertTrue(Stream.<Integer> empty().noneMatch(n -> true));
    }

    @Test
    public void testPercentiles() {
        u.Optional<Map<Percentage, Integer>> result = Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).percentiles(Integer::compareTo);
        assertTrue(result.isPresent());
        assertFalse(result.get().isEmpty());
    }

    @Test
    public void testPercentiles_Empty() {
        Optional<Map<Percentage, Integer>> result = Stream.<Integer> empty().percentiles(Comparator.naturalOrder());
        assertFalse(result.isPresent());
    }

    // reduce

    @Test
    public void testReduce() {
        assertEquals(Integer.valueOf(15), Stream.of(1, 2, 3, 4, 5).reduce(Integer::sum).get());
        assertFalse(Stream.<Integer> empty().reduce(Integer::sum).isPresent());
    }

    @Test
    public void testReduceBinaryOperator() {
        assertEquals(Optional.of(6), Stream.of(1, 2, 3).reduce((a, b) -> a + b));
        assertEquals(Optional.empty(), Stream.<Integer> empty().reduce((a, b) -> a + b));
    }

    @Test
    public void testReduceIdentityAccumulatorCombiner() {
        int result = Stream.of("1", "2", "3").reduce(0, (sum, str) -> sum + Integer.parseInt(str), (s1, s2) -> s1 + s2);
        assertEquals(6, result);

        result = Stream.of("1", "2", "3", "4", "5", "6").parallel().reduce(0, (sum, str) -> sum + Integer.parseInt(str), Integer::sum);
        assertEquals(21, result);
    }

    @Test
    public void testReduceIdentityBinaryOperator() {
        assertEquals(10, (int) Stream.of(1, 2, 3).reduce(4, (a, b) -> a + b));
        assertEquals(4, (int) Stream.<Integer> empty().reduce(4, (a, b) -> a + b));
    }

    @Test
    public void testReduceWithIdentity() {
        assertEquals(Integer.valueOf(15), Stream.of(1, 2, 3, 4, 5).reduce(0, Integer::sum));
        assertEquals(Integer.valueOf(0), Stream.<Integer> empty().reduce(0, Integer::sum));
    }

    @Test
    public void testReduceWithIdentity_empty() {
        int result = Stream.<Integer> empty().reduce(0, Integer::sum);
        assertEquals(0, result);
    }

    @Test
    public void testReduceWithIdentityAndCombiner() {
        assertEquals(Integer.valueOf(15), Stream.of(1, 2, 3, 4, 5).reduce(0, Integer::sum, Integer::sum));
    }

    @Test
    public void testSumDouble() {
        assertEquals(15.0, Stream.of(1, 2, 3, 4, 5).sumDouble(n -> n), 0.001);
    }

    @Test
    public void testSumInt() {
        assertEquals(15, Stream.of(1, 2, 3, 4, 5).sumInt(n -> n));
    }

    @Test
    public void testSumInt_Empty() {
        long result = Stream.<String> empty().sumInt(String::length);
        assertEquals(0L, result);
    }

    @Test
    public void testSumLong() {
        assertEquals(15L, Stream.of(1, 2, 3, 4, 5).sumLong(n -> n));
    }

}
