package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.Indexed;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.Function;

/** Stream transformation contracts. */
public class StreamTransformationTest extends StreamTestSupport {

    // collapse

    @Test
    public void testCollapse() {
        Stream<List<Integer>> collapsed = createStream(1, 2, 3, 3, 2, 1).collapse((a, b) -> a < b);
        List<List<Integer>> result = collapsed.toList();
        assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(3), Arrays.asList(2), Arrays.asList(1)), result);

        Stream<Integer> collapsedWithMerge = createStream(1, 2, 3, 3, 2, 1).collapse((a, b) -> a < b, (a, b) -> a + b);
        assertEquals(Arrays.asList(6, 3, 2, 1), collapsedWithMerge.toList());

        Stream<Integer> collapsedWithCollector = createStream(1, 2, 3, 3, 2, 1).collapse((a, b) -> a < b, Collectors.summingInt(Integer::intValue));
        assertEquals(Arrays.asList(6, 3, 2, 1), collapsedWithCollector.toList());
    }

    @Test
    public void testCollapse_BiPredicate() {
        // Collapse consecutive equal elements into groups
        List<List<Integer>> result = Stream.of(1, 1, 2, 2, 2, 3, 3, 1).collapse((a, b) -> a.equals(b)).toList();
        assertEquals(4, result.size());
        assertEquals(Arrays.asList(1, 1), result.get(0));
        assertEquals(Arrays.asList(2, 2, 2), result.get(1));
        assertEquals(Arrays.asList(3, 3), result.get(2));
        assertEquals(Arrays.asList(1), result.get(3));
    }

    @Test
    public void testCollapse_BiPredicate_Empty() {
        List<List<Integer>> result = Stream.<Integer> empty().collapse((a, b) -> a.equals(b)).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCollapse_WithCollector() {
        List<Long> result = Stream.of(1, 1, 2, 2, 2, 3).collapse((a, b) -> a.equals(b), java.util.stream.Collectors.counting()).toList();
        assertEquals(Arrays.asList(2L, 3L, 1L), result);
    }

    @Test
    public void testCollapse_WithMergeFunction() {
        // Collapse and merge with sum
        List<Integer> result = Stream.of(1, 1, 2, 2, 2, 3, 3, 1).collapse((a, b) -> a.equals(b), Integer::sum).toList();
        assertEquals(Arrays.asList(2, 6, 6, 1), result);
    }

    @Test
    public void testCollapseBiPredicate() {
        Stream<List<Integer>> stream = Stream.of(1, 1, 2, 2, 2, 3).collapse((a, b) -> a.equals(b));
        List<List<Integer>> result = stream.toList();
        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1, 1), result.get(0));
        assertEquals(Arrays.asList(2, 2, 2), result.get(1));
        assertEquals(Arrays.asList(3), result.get(2));
    }

    @Test
    public void testCollapseBiPredicateAndMerger() {
        List<Integer> result = Stream.of(1, 1, 2, 3, 3, 1, 2).collapse((p, c) -> p.equals(c), (r, c) -> r + c).toList();
        assertEquals(Arrays.asList(2, 2, 6, 1, 2), result);
    }

    @Test
    public void testCollapseTriPredicate() {
        List<List<Integer>> result = Stream.of(10, 11, 12, 9, 5, 4, 6, 20)
                .collapse((first, prev, curr) -> Math.abs(curr - first) <= 2 && Math.abs(curr - prev) <= 1)
                .toList();

        assertEquals(Arrays.asList(Arrays.asList(10, 11, 12), Collections.singletonList(9), Arrays.asList(5, 4), Collections.singletonList(6),
                Collections.singletonList(20)), result);
    }

    @Test
    public void testCollapseTriPredicateWithMerge() {
        List<Integer> result = Stream.of(1, 1, 2, 2, 2, 3, 3).collapse((prev, cur, next) -> Objects.equals(prev, cur), Integer::sum).toList();
        assertNotNull(result);
        assertTrue(result.size() >= 1);
    }

    @Test
    public void testCollapseWithCollector() {
        Stream<Integer> stream = Stream.of(1, 1, 2, 2, 2, 3).collapse((a, b) -> a.equals(b), Collectors.summingInt(Integer::intValue));
        assertEquals(Arrays.asList(2, 6, 3), stream.toList());
    }

    @Test
    public void testCollapseWithMergeFunction() {
        Stream<Integer> stream = Stream.of(1, 1, 2, 2, 2, 3).collapse((a, b) -> a.equals(b), Integer::sum);
        assertEquals(Arrays.asList(2, 6, 3), stream.toList());
    }

    // cycled

    @Test
    public void testCycled() {
        List<Integer> result = Stream.of(1, 2, 3).cycled().limit(7).toList();
        assertEquals(Arrays.asList(1, 2, 3, 1, 2, 3, 1), result);
    }

    @Test
    public void testCycledWithRounds() {
        List<Integer> result = Stream.of(1, 2, 3).cycled(2).toList();
        assertEquals(Arrays.asList(1, 2, 3, 1, 2, 3), result);
    }

    @Test
    public void testCycledZeroRounds() {
        List<Integer> result = Stream.of(1, 2, 3).cycled(0).toList();
        assertEquals(0, result.size());
    }

    @Test
    public void testDifference() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4).difference(Arrays.asList(2, 3, 5));
        assertEquals(Arrays.asList(1, 4), stream.toList());
    }

    @Test
    public void testDifferenceNone() {
        Stream<Integer> stream = Stream.of(1, 2, 3).difference(Arrays.asList(4, 5, 6));
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    // distinct

    @Test
    public void testDistinct() {
        Stream<Integer> stream = Stream.of(1, 2, 2, 3, 3, 3).distinct();
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    // Covers Stream.distinct(BinaryOperator) parallel path
    @Test
    public void testDistinct_BinaryOperator_Parallel() {
        List<Integer> result = Stream.of(1, 2, 1, 3, 2).parallel().distinct(Integer::sum).sorted().toList();
        assertEquals(3, result.size());
        assertTrue(result.contains(2)); // 1+1=2? No - distinct uses identity as key, merged value is 1+1=2
    }

    @Test
    public void testDistinctAllDuplicates() {
        Stream<Integer> stream = Stream.of(1, 1, 1).distinct();
        assertEquals(Arrays.asList(1), stream.toList());
    }

    @Test
    public void testDistinctEmpty() {
        Stream<Integer> stream = Stream.<Integer> empty().distinct();
        assertEquals(0, stream.count());
    }

    @Test
    public void testDistinctNoDuplicates() {
        Stream<Integer> stream = Stream.of(1, 2, 3).distinct();
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testDistinctWithMergeFunction() {
        List<String> result = Stream.of("a1", "b1", "a2", "b2").distinct((s1, s2) -> s1 + "+" + s2).toList();
        // Since distinct uses identity as key, each string is unique => no merge
        assertEquals(4, result.size());

        // Test with actual duplicates
        List<Integer> intResult = Stream.of(1, 2, 1, 3, 2).distinct(Integer::sum).toList();
        assertEquals(3, intResult.size());
        assertTrue(intResult.contains(3)); // 1+1=2, but merged as sum
    }

    // distinctBy

    @Test
    public void testDistinctBy() {
        Stream<String> stream = Stream.of("a", "bb", "c", "dd").distinctBy(String::length);
        assertEquals(Arrays.asList("a", "bb"), stream.toList());
    }

    // Covers Stream.distinctBy(Function, BinaryOperator) parallel path
    @Test
    public void testDistinctBy_FunctionAndMerge_Parallel() {
        List<String> result = Stream.of("a1", "b1", "a2", "b2").parallel().distinctBy(s -> s.charAt(0), (s1, s2) -> s1 + "+" + s2).sorted().toList();
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(s -> s.startsWith("a")));
        assertTrue(result.stream().anyMatch(s -> s.startsWith("b")));
    }

    @Test
    public void testDistinctByFunction() {
        List<String> result = Stream.of("apple", "apricot", "banana", "blueberry", "Apple").distinctBy(s -> s.toLowerCase().charAt(0)).toList();
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(s -> s.toLowerCase().startsWith("a")));
        assertTrue(result.stream().anyMatch(s -> s.toLowerCase().startsWith("b")));
    }

    @Test
    public void testDistinctByWithMerge() {
        Stream<String> stream = Stream.of("a", "bb", "c", "dd").distinctBy(String::length, (s1, s2) -> s1 + s2);
        assertEquals(Arrays.asList("ac", "bbdd"), stream.toList());
    }

    @Test
    public void testDistinctByWithMergeFunction() {
        List<String> result = Stream.of("a1", "b1", "a2", "b2").distinctBy(s -> s.charAt(0), (s1, s2) -> s1 + "+" + s2).toList();
        assertEquals(2, result.size());
        assertEquals("a1+a2", result.get(0));
        assertEquals("b1+b2", result.get(1));
    }

    @Test
    public void testDistinctWithMerge() {
        List<Pair<String, Integer>> data = Arrays.asList(Pair.of("a", 1), Pair.of("b", 2), Pair.of("a", 3));
        List<Pair<String, Integer>> result = Stream.of(data)
                .distinctBy(Pair::left, (p1, p2) -> Pair.of(p1.left(), p1.right() + p2.right()))
                .sorted(Comparator.comparing(Pair::left))
                .toList();

        assertEquals(Arrays.asList(Pair.of("a", 4), Pair.of("b", 2)), result);
    }

    // dropWhile

    @Test
    public void testDropWhile() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).dropWhile(n -> n < 3);
        assertEquals(Arrays.asList(3, 4, 5), stream.toList());
    }

    @Test
    public void testDropWhileAllDrop() {
        Stream<Integer> stream = Stream.of(1, 2, 3).dropWhile(n -> n < 10);
        assertEquals(0, stream.count());
    }

    @Test
    public void testDropWhileEmpty() {
        Stream<Integer> stream = Stream.<Integer> empty().dropWhile(n -> n < 3);
        assertEquals(0, stream.count());
    }

    @Test
    public void testDropWhileNoneDrop() {
        Stream<Integer> stream = Stream.of(5, 6, 7).dropWhile(n -> n < 3);
        assertEquals(Arrays.asList(5, 6, 7), stream.toList());
    }

    @Test
    public void testDropWhileWithAction() {
        List<Integer> dropped = new ArrayList<>();
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).dropWhile(n -> n < 3, dropped::add);
        assertEquals(Arrays.asList(3, 4, 5), stream.toList());
        assertEquals(Arrays.asList(1, 2), dropped);
    }

    // filter

    @Test
    public void testFilter() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).filter(n -> n % 2 == 0);
        assertEquals(Arrays.asList(2, 4), stream.toList());
    }

    @Test
    public void testFilterAllMatch() {
        Stream<Integer> stream = Stream.of(2, 4, 6).filter(n -> n % 2 == 0);
        assertEquals(Arrays.asList(2, 4, 6), stream.toList());
    }

    @Test
    public void testFilterEmpty() {
        Stream<Integer> stream = Stream.<Integer> empty().filter(n -> n % 2 == 0);
        assertEquals(0, stream.count());
    }

    @Test
    public void testFilterNoneMatch() {
        Stream<Integer> stream = Stream.of(1, 3, 5).filter(n -> n % 2 == 0);
        assertEquals(0, stream.count());
    }

    @Test
    public void testFilterWithAction() {
        List<Integer> dropped = new ArrayList<>();
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).filter(n -> n % 2 == 0, dropped::add);
        assertEquals(Arrays.asList(2, 4), stream.toList());
        assertEquals(Arrays.asList(1, 3, 5), dropped);
    }

    @Test
    public void testFilterWithActionOnDroppedItem() {
        List<Integer> droppedItems = new ArrayList<>();
        assertEquals(Arrays.asList(2, 4), Stream.of(1, 2, 3, 4, 5).filter(x -> x % 2 == 0, droppedItems::add).toList());
        assertEquals(Arrays.asList(1, 3, 5), droppedItems);

        droppedItems.clear();
        assertEquals(Collections.emptyList(), Stream.<Integer> empty().filter(x -> x % 2 == 0, droppedItems::add).toList());
        assertTrue(droppedItems.isEmpty());
    }

    @Test
    public void testFilterE() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).filterE(x -> x > 3).toList();
        assertEquals(Arrays.asList(4, 5), result);
    }

    @Test
    public void testIndexed() {
        Stream<Indexed<String>> stream = Stream.of("a", "b", "c").indexed();
        List<Indexed<String>> list = stream.toList();
        assertEquals(0, list.get(0).index());
        assertEquals("a", list.get(0).value());
        assertEquals(1, list.get(1).index());
        assertEquals("b", list.get(1).value());
        assertEquals(2, list.get(2).index());
        assertEquals("c", list.get(2).value());
    }

    @Test
    public void testIntersection() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4).intersection(Arrays.asList(2, 3, 5));
        assertEquals(Arrays.asList(2, 3), stream.toList());
    }

    @Test
    public void testIntersectionEmpty() {
        Stream<Integer> stream = Stream.of(1, 2, 3).intersection(Arrays.asList(4, 5, 6));
        assertEquals(0, stream.count());
    }

    // intersperse

    @Test
    public void testIntersperse() {
        List<Integer> result = Stream.of(1, 2, 3).intersperse(0).toList();
        assertEquals(Arrays.asList(1, 0, 2, 0, 3), result);
    }

    @Test
    public void testIntersperse_Empty() {
        List<Integer> result = Stream.<Integer> empty().intersperse(0).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testIntersperse_SingleElement() {
        List<Integer> result = Stream.of(1).intersperse(0).toList();
        assertEquals(Arrays.asList(1), result);
    }

    @Test
    public void testIntersperse_TwoElements() {
        List<Integer> result = Stream.of(1, 2).intersperse(0).toList();
        assertEquals(Arrays.asList(1, 0, 2), result);
    }

    @Test
    public void testIntersperseEmpty() {
        List<Integer> result = Stream.<Integer> empty().intersperse(0).toList();
        assertEquals(0, result.size());
    }

    // limit

    @Test
    public void testLimit() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).limit(3);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testLimitGreaterThanSize() {
        Stream<Integer> stream = Stream.of(1, 2, 3).limit(10);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testLimitZero() {
        Stream<Integer> stream = Stream.of(1, 2, 3).limit(0);
        assertEquals(0, stream.count());
    }

    @Test
    public void testOnEach() {
        List<Integer> processed = new ArrayList<>();
        Stream<Integer> stream = Stream.of(1, 2, 3).peek(processed::add);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
        assertEquals(Arrays.asList(1, 2, 3), processed);
    }

    // onEachSave

    @Test
    public void testonEachSaveToFileDefault() throws IOException {
        File tempFile = new File(tempDir.toFile(), "onEachSaveDefault.txt");
        Stream.of("hello", "world").onEachSave(tempFile).forEach(e -> {
        });
        assertTrue(tempFile.exists());
        String content = IOUtil.readAllToString(tempFile);
        assertTrue(content.contains("hello"));
        assertTrue(content.contains("world"));
    }

    @Test
    public void testOnEachSaveToLineNullOutputStreamRejectedOnFirstPull() {
        // The null output is not validated when the operation is added; it is rejected when the first element is pulled.
        final Stream<String> s = Stream.of("a", "b").onEachSave(e -> e, (java.io.OutputStream) null);
        assertThrows(IllegalArgumentException.class, s::count);
        assertEquals(0, Stream.<String> empty().onEachSave(e -> e, (java.io.OutputStream) null).count());
    }

    @Test
    public void testOnEachSaveToLineNullWriterRejectedOnFirstPull() {
        final Stream<String> s = Stream.of("a", "b").onEachSave(e -> e, (java.io.Writer) null);
        assertThrows(IllegalArgumentException.class, s::count);
        assertEquals(0, Stream.<String> empty().onEachSave(e -> e, (java.io.Writer) null).count());
    }

    @Test
    public void testOnEachSaveWriteNullWriterRejectedOnFirstPull() {
        final Stream<String> s = Stream.of("a", "b").onEachSave((e, w) -> w.write(e), (java.io.Writer) null);
        assertThrows(IllegalArgumentException.class, s::count);
        assertEquals(0, Stream.<String> empty().onEachSave((e, w) -> w.write(e), (java.io.Writer) null).count());
    }

    @Test
    public void testOnFirst() {
        AtomicReference<Integer> first = new AtomicReference<>();
        Stream<Integer> stream = Stream.of(1, 2, 3).onFirst(first::set);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
        assertEquals(Integer.valueOf(1), first.get());
    }

    @Test
    public void testOnFirst_Empty() {
        List<String> sideEffects = new ArrayList<>();
        List<Integer> result = Stream.<Integer> empty().onFirst(e -> sideEffects.add("first:" + e)).toList();
        assertTrue(result.isEmpty());
        assertTrue(sideEffects.isEmpty());
    }

    @Test
    public void testOnLast() {
        AtomicReference<Integer> last = new AtomicReference<>();
        Stream<Integer> stream = Stream.of(1, 2, 3).onLast(last::set);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
        assertEquals(Integer.valueOf(3), last.get());
    }

    @Test
    public void testOnlyOne() {
        assertEquals(Integer.valueOf(42), Stream.of(42).onlyOne().get());
        assertFalse(Stream.<Integer> empty().onlyOne().isPresent());
        assertThrows(TooManyElementsException.class, () -> Stream.of(1, 2).onlyOne());
    }

    @Test
    public void testOnEachAndPeek() {
        List<Integer> peeked = new ArrayList<>();
        List<Integer> onEached = new ArrayList<>();

        List<Integer> resultPeek = Stream.of(1, 2).peek(peeked::add).map(x -> x * 2).toList();
        assertEquals(Arrays.asList(1, 2), peeked);
        assertEquals(Arrays.asList(2, 4), resultPeek);

        List<Integer> resultOnEach = Stream.of(1, 2).peek(onEached::add).map(x -> x * 2).toList();
        assertEquals(Arrays.asList(1, 2), onEached);
        assertEquals(Arrays.asList(2, 4), resultOnEach);
    }

    @Test
    public void testPeek() {
        List<Integer> peeked = new ArrayList<>();
        Stream<Integer> stream = Stream.of(1, 2, 3).peek(peeked::add);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
        assertEquals(Arrays.asList(1, 2, 3), peeked);
    }

    @Test
    public void testPeekFirst() {
        AtomicReference<Integer> firstRef = new AtomicReference<>();
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).peekFirst(firstRef::set).toList();
        assertEquals(1, firstRef.get());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    // peekIf

    @Test
    public void testPeekIf() {
        List<Integer> peeked = new ArrayList<>();
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).peekIf(n -> n % 2 == 0, peeked::add).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
        assertEquals(Arrays.asList(2, 4), peeked);
    }

    @Test
    public void testPeekIf_BiPredicate() {
        List<Integer> peeked = new ArrayList<>();
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).peekIf((val, count) -> count % 2 == 0, peeked::add).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
        // count starts at 1 and increments, even counts are 2 and 4 -> elements 2 and 4
        assertEquals(2, peeked.size());
        assertTrue(peeked.contains(2));
        assertTrue(peeked.contains(4));
    }

    @Test
    public void testPeekIf_BiPredicate_Parallel() {
        // Covers the parallel branch of peekIf(BiPredicate, Consumer)
        List<Integer> peeked = new java.util.concurrent.CopyOnWriteArrayList<>();
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).parallel().peekIf((val, count) -> count % 2 == 0, peeked::add).sorted().toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
        assertFalse(peeked.isEmpty());
    }

    @Test
    public void testPeekIfEmpty() {
        List<Integer> peeked = new ArrayList<>();
        List<Integer> result = Stream.<Integer> empty().peekIf(n -> n % 2 == 0, peeked::add).toList();
        assertTrue(result.isEmpty());
        assertTrue(peeked.isEmpty());
    }

    @Test
    public void testPeekIfWithBiPredicate() {
        List<Integer> peeked = new ArrayList<>();
        Stream.of(1, 2, 3, 4, 5, 6).peekIf((val, idx) -> val % 2 == 0 && idx > 1, peeked::add).toList();
        assertEquals(Arrays.asList(2, 4, 6), peeked);
    }

    @Test
    public void testPeekIfWithIndex() {
        List<Integer> peeked = new ArrayList<>();
        List<Integer> result = Stream.of(10, 20, 30, 40, 50).peekIf((e, count) -> count % 2 == 0, peeked::add).toList();
        assertEquals(Arrays.asList(20, 40), peeked); // count starts at 1, so even counts are 2nd, 4th
        assertEquals(Arrays.asList(10, 20, 30, 40, 50), result);
    }

    @Test
    public void testPeekLast() {
        AtomicReference<Integer> lastRef = new AtomicReference<>();
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).peekLast(lastRef::set).toList();
        assertEquals(5, lastRef.get());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    // reversed

    @Test
    public void testReversed() {
        Stream<Integer> stream = Stream.of(1, 2, 3).reversed();
        assertEquals(Arrays.asList(3, 2, 1), stream.toList());
    }

    @Test
    public void testReversedEmpty() {
        Stream<Integer> stream = Stream.<Integer> empty().reversed();
        assertEquals(0, stream.count());
    }

    @Test
    public void testReversedSingleElement() {
        Stream<Integer> stream = Stream.of(42).reversed();
        assertEquals(Arrays.asList(42), stream.toList());
    }

    @Test
    public void testReverseSorted() {
        Stream<Integer> stream = Stream.of(1, 2, 3).reverseSorted();
        assertEquals(Arrays.asList(3, 2, 1), stream.toList());
    }

    @Test
    public void testReverseSortedComparator() {
        Stream<String> stream = Stream.of("a", "bbb", "cc").reverseSorted(Comparator.comparing(String::length));
        assertEquals(Arrays.asList("bbb", "cc", "a"), stream.toList());
    }

    @Test
    public void testReverseSortedBy() {
        List<String> result = Stream.of("a", "bbb", "cc").reverseSortedBy(String::length).toList();
        assertEquals(Arrays.asList("bbb", "cc", "a"), result);
    }

    @Test
    public void testReverseSortedByPlacesNullKeysLast() {
        final List<String> result = Stream.of("a", "bb", "ccc").reverseSortedBy(s -> s.length() == 1 ? null : s.length()).toList();

        assertEquals(Arrays.asList("ccc", "bb", "a"), result);
    }

    @Test
    public void testReverseSortedByDouble() {
        List<Integer> result = Stream.of(1, 3, 2).reverseSortedByDouble(n -> n.doubleValue()).toList();
        assertEquals(Arrays.asList(3, 2, 1), result);
    }

    @Test
    public void testReverseSortedByInt() {
        List<String> result = Stream.of("a", "bbb", "cc").reverseSortedByInt(String::length).toList();
        assertEquals(Arrays.asList("bbb", "cc", "a"), result);
    }

    @Test
    public void testReverseSortedByLong() {
        List<String> result = Stream.of("a", "bbb", "cc").reverseSortedByLong(String::length).toList();
        assertEquals(Arrays.asList("bbb", "cc", "a"), result);
    }

    // rotated

    @Test
    public void testRotatedGreaterThanSize() {
        Stream<Integer> stream = Stream.of(1, 2, 3).rotated(5);
        assertHaveSameElements(Arrays.asList(3, 1, 2), stream.toList());
    }

    @Test
    public void testRotatedNegative() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).rotated(-2);
        assertEquals(Arrays.asList(3, 4, 5, 1, 2), stream.toList());
    }

    @Test
    public void testRotatedPositive() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).rotated(2);
        assertEquals(Arrays.asList(4, 5, 1, 2, 3), stream.toList());
    }

    @Test
    public void testRotatedZero() {
        Stream<Integer> stream = Stream.of(1, 2, 3).rotated(0);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    // scan

    @Test
    public void testScan() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4).scan(Integer::sum);
        assertEquals(Arrays.asList(1, 3, 6, 10), stream.toList());
    }

    @Test
    public void testScan_Empty() {
        List<Integer> result = Stream.<Integer> empty().scan((a, b) -> a + b).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testScan_SingleElement() {
        List<Integer> result = Stream.of(42).scan((a, b) -> a + b).toList();
        assertEquals(Arrays.asList(42), result);
    }

    @Test
    public void testScan_WithInit() {
        List<Integer> result = Stream.of(1, 2, 3, 4).scan(10, (a, b) -> a + b).toList();
        assertEquals(Arrays.asList(11, 13, 16, 20), result);
    }

    @Test
    public void testScan_WithInitIncluded() {
        List<Integer> result = Stream.of(1, 2, 3).scan(10, true, (a, b) -> a + b).toList();
        assertEquals(Arrays.asList(10, 11, 13, 16), result);
    }

    @Test
    public void testScanSimple() {
        List<Integer> result = Stream.of(1, 2, 3, 4).scan((a, b) -> a + b).toList();
        assertEquals(Arrays.asList(1, 3, 6, 10), result);
        assertTrue(Stream.<Integer> empty().scan((a, b) -> a + b).toList().isEmpty());
        assertEquals(Arrays.asList(1), Stream.of(1).scan((a, b) -> a + b).toList());
    }

    @Test
    public void testScanWithInit() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4).scan(0, Integer::sum);
        assertEquals(Arrays.asList(1, 3, 6, 10), stream.toList());
    }

    @Test
    public void testScanWithInitialValue() {
        List<Integer> result = Stream.of(1, 2, 3).scan(10, (a, b) -> a + b).toList();
        assertEquals(Arrays.asList(11, 13, 16), result);
        assertTrue(Stream.<Integer> empty().scan(10, (a, b) -> a + b).toList().isEmpty());
    }

    @Test
    public void testScanWithInitialValueAndInclusion() {
        List<Integer> result = Stream.of(1, 2, 3).scan(10, true, (a, b) -> a + b).toList();
        assertEquals(Arrays.asList(10, 11, 13, 16), result);

        result = Stream.of(1, 2, 3).scan(10, false, (a, b) -> a + b).toList();
        assertEquals(Arrays.asList(11, 13, 16), result);

        result = Stream.<Integer> empty().scan(10, true, (a, b) -> a + b).toList();
        assertEquals(Arrays.asList(10), result);

        result = Stream.<Integer> empty().scan(10, false, (a, b) -> a + b).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testScanWithInitIncluded() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4).scan(0, true, Integer::sum);
        assertEquals(Arrays.asList(0, 1, 3, 6, 10), stream.toList());
    }

    @Test
    public void testShuffled() {
        List<Integer> original = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> shuffled = Stream.of(original).shuffled().toList();
        assertEquals(5, shuffled.size());
        assertTrue(shuffled.containsAll(original));
    }

    @Test
    public void testShuffledWithRandom() {
        List<Integer> original = Arrays.asList(1, 2, 3, 4, 5);
        Random random = new Random(12345);
        List<Integer> shuffled1 = Stream.of(original).shuffled(new Random(12345)).toList();
        List<Integer> shuffled2 = Stream.of(original).shuffled(new Random(12345)).toList();
        assertEquals(shuffled1, shuffled2);
    }

    // skip

    @Test
    public void testSkip() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).skip(2);
        assertEquals(Arrays.asList(3, 4, 5), stream.toList());
    }

    @Test
    public void testSkipGreaterThanSize() {
        Stream<Integer> stream = Stream.of(1, 2, 3).skip(10);
        assertEquals(0, stream.count());
    }

    @Test
    public void testSkipWithAction() {
        List<Integer> skipped = new ArrayList<>();
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).skip(2, skipped::add);
        assertEquals(Arrays.asList(3, 4, 5), stream.toList());
        assertEquals(Arrays.asList(1, 2), skipped);
    }

    @Test
    public void testSkipZero() {
        Stream<Integer> stream = Stream.of(1, 2, 3).skip(0);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    // skipLast

    @Test
    public void testSkipLast() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).skipLast(2);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void test_skipLast() {
        assertDoesNotThrow(() -> {
            N.println(Strings.repeat("=", 80));

            IntStream.range(0, 20).boxed().skipLast(0).println();
            IntStream.range(0, 20).boxed().skipLast(10).println();
            IntStream.range(0, 20).boxed().skipLast(19).println();
            IntStream.range(0, 20).boxed().skipLast(20).println();
            IntStream.range(0, 20).boxed().skipLast(21).println();

            N.println(Strings.repeat("=", 80));

            IntStream.range(0, 20).boxed().parallel().skipLast(0).println();
            IntStream.range(0, 20).boxed().parallel().skipLast(10).println();
            IntStream.range(0, 20).boxed().parallel().skipLast(19).println();
            IntStream.range(0, 20).boxed().parallel().skipLast(20).println();
            IntStream.range(0, 20).boxed().parallel().skipLast(21).println();

            N.println(Strings.repeat("=", 80));
        });
    }

    @Test
    public void testSkipLastTakeLastSliding_tolerateNullElements() {
        // Regression: skipLast/takeLast/sliding are value-agnostic structural ops and must not NPE on a
        // null element (the iterator-backed impls previously buffered into a null-rejecting ArrayDeque).
        // map(e -> e) forces the IteratorStream path where the buffer lives.
        assertEquals(N.asList("a", null), Stream.of("a", null, "c").map(e -> e).skipLast(1).toList());
        assertEquals(N.asList(null, "c"), Stream.of("a", null, "c").map(e -> e).takeLast(2).toList());

        List<List<String>> windows = Stream.of("a", null, "c").map(e -> e).sliding(2).toList();
        assertEquals(2, windows.size());
        assertEquals(N.asList("a", null), windows.get(0));
        assertEquals(N.asList(null, "c"), windows.get(1));
    }

    @Test
    public void testSkipNulls() {
        List<Integer> result = Stream.of(1, null, 2, null, 3).skipNulls().toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testSkipNulls_NoNulls() {
        List<String> result = Stream.of("a", "b", "c").skipNulls().toList();
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    // skipRange

    @Test
    public void testSkipRange() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).skipRange(1, 3).toList();
        assertEquals(Arrays.asList(1, 4, 5), result);
    }

    @Test
    public void testSkipRange_EmptyRange() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).skipRange(2, 2).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testSkipRange_FullRange() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).skipRange(0, 5).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSkipRangeEmpty() {
        List<Integer> result = Stream.<Integer> empty().skipRange(1, 3).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSkipRangeOutOfBounds() {
        List<Integer> result = Stream.of(1, 2, 3).skipRange(5, 10).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testSkipUntil() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).skipUntil(n -> n >= 3);
        assertEquals(Arrays.asList(3, 4, 5), stream.toList());
    }

    // sorted

    @Test
    public void testSorted() {
        Stream<Integer> stream = Stream.of(3, 1, 2).sorted();
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testSortedAlreadySorted() {
        Stream<Integer> stream = Stream.of(1, 2, 3).sorted();
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testSortedComparator() {
        Stream<Integer> stream = Stream.of(1, 2, 3).sorted(Comparator.reverseOrder());
        assertEquals(Arrays.asList(3, 2, 1), stream.toList());
    }

    @Test
    public void testSortedEmpty() {
        Stream<Integer> stream = Stream.<Integer> empty().sorted();
        assertEquals(0, stream.count());
    }

    @Test
    public void testSortedBy() {
        Stream<String> stream = Stream.of("aaa", "b", "cc").sortedBy(String::length);
        assertEquals(Arrays.asList("b", "cc", "aaa"), stream.toList());
    }

    @Test
    public void testSortedByPlacesNullKeysFirst() {
        final List<String> result = Stream.of("ccc", "a", "bb").sortedBy(s -> s.length() == 1 ? null : s.length()).toList();

        assertEquals(Arrays.asList("a", "bb", "ccc"), result);
    }

    @Test
    public void testSortedByDouble() {
        List<Integer> result = Stream.of(3, 1, 2).sortedByDouble(n -> n.doubleValue()).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testSortedByInt() {
        Stream<String> stream = Stream.of("aaa", "b", "cc").sortedByInt(String::length);
        assertEquals(Arrays.asList("b", "cc", "aaa"), stream.toList());
    }

    @Test
    public void testSortedByLong() {
        List<String> result = Stream.of("a", "bbb", "cc").sortedByLong(String::length).toList();
        assertEquals(Arrays.asList("a", "cc", "bbb"), result);
    }

    // step

    @Test
    public void testStep() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5, 6).step(2);
        assertEquals(Arrays.asList(1, 3, 5), stream.toList());
    }

    @Test
    public void test_step() {
        assertDoesNotThrow(() -> {
            IntStream.range(0, 13).step(2).println();
            IntStream.range(0, 13).step(1).println();
            IntStream.range(0, 13).skip(2).limit(7).step(2).println();
            IntStream.range(0, 13).skip(1).limit(7).step(1).println();
        });
    }

    @Test
    public void testStepOne() {
        Stream<Integer> stream = Stream.of(1, 2, 3).step(1);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testSymmetricDifference() {
        Stream<Integer> stream = Stream.of(1, 2, 3).symmetricDifference(Arrays.asList(2, 3, 4));
        List<Integer> result = stream.toList();
        assertTrue(result.containsAll(Arrays.asList(1, 4)));
        assertEquals(2, result.size());
    }

    // takeLast

    @Test
    public void testTakeLast() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).takeLast(2);
        assertEquals(Arrays.asList(4, 5), stream.toList());
    }

    @Test
    public void testTakeLast_MoreThanSize() {
        List<Integer> result = Stream.of(1, 2, 3).takeLast(10).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testTakeLast_Zero() {
        List<Integer> result = Stream.of(1, 2, 3).takeLast(0).toList();
        assertTrue(result.isEmpty());
    }

    // takeWhile

    @Test
    public void testTakeWhile() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).takeWhile(n -> n < 4);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testTakeWhileAllTrue() {
        Stream<Integer> stream = Stream.of(1, 2, 3).takeWhile(n -> n < 10);
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void testTakeWhileEmpty() {
        Stream<Integer> stream = Stream.<Integer> empty().takeWhile(n -> n < 4);
        assertEquals(0, stream.count());
    }

    @Test
    public void testTakeWhileFirstFalse() {
        Stream<Integer> stream = Stream.of(5, 6, 7).takeWhile(n -> n < 4);
        assertEquals(0, stream.count());
    }

    // top

    @Test
    public void testTop() {
        Stream<Integer> stream = Stream.of(5, 1, 3, 2, 4).top(3);
        assertEquals(Arrays.asList(3, 5, 4), stream.toList());
    }

    @Test
    public void testTopGreaterThanSize() {
        Stream<Integer> stream = Stream.of(1, 2, 3).top(10);
        assertHaveSameElements(Arrays.asList(3, 2, 1), stream.toList());
    }

    @Test
    public void testTopN() {
        assertEquals(Arrays.asList(4, 5), Stream.of(1, 5, 2, 4, 3).top(2).toList());
        assertEquals(Arrays.asList(4, 5), Stream.of(1, 5, 2, 4, 3).top(2, Comparator.naturalOrder()).toList());
        assertEquals(Arrays.asList(2, 1), Stream.of(1, 5, 2, 4, 3).top(2, Comparator.reverseOrder()).toList());
        assertEquals(Arrays.asList(1, 2, 3), Stream.of(1, 2, 3).top(5).toList());
    }

    @Test
    public void testTopWithComparator() {
        Stream<Integer> stream = Stream.of(5, 1, 3, 2, 4).top(3, Comparator.naturalOrder());
        assertEquals(Arrays.asList(3, 5, 4), stream.toList());
    }

}
