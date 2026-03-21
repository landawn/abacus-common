package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.function.CharBinaryOperator;
import com.landawn.abacus.util.function.CharConsumer;
import com.landawn.abacus.util.function.CharFunction;
import com.landawn.abacus.util.function.CharPredicate;
import com.landawn.abacus.util.function.CharTernaryOperator;
import com.landawn.abacus.util.function.CharUnaryOperator;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings.PS;
import com.landawn.abacus.util.stream.BaseStream.Splitor;

public class ParallelIteratorCharStreamTest extends TestBase {

    private static final int testMaxThreadNum = 4;
    private static final char[] TEST_ARRAY = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
            'w', 'x', 'y', 'z' };

    private CharStream stream;

    protected CharStream createCharStream(char... elements) {
        return CharStream.of(elements).map(e -> (char) (e + 0)).parallel(PS.create(Splitor.ITERATOR).maxThreadNum(testMaxThreadNum));
    }

    @BeforeEach
    public void setUp() {
        stream = createCharStream(TEST_ARRAY);
    }

    // ---- Sequential-fallback path: 1-thread iterator stream => canBeSequential(maxThreadNum) == true ----

    private CharStream createSingleThreadStream(char... elements) {
        return CharStream.of(elements).map(e -> (char) (e + 0)).parallel(PS.create(Splitor.ITERATOR).maxThreadNum(1));
    }

    @Test
    public void testFilter() {
        CharStream stream = createCharStream(TEST_ARRAY);
        List<Character> result = stream.filter(c -> c >= 'x').toList();
        assertEquals(3, result.size());
        assertTrue(result.contains('x'));
        assertTrue(result.contains('y'));
        assertTrue(result.contains('z'));

        CharStream emptyStream = createCharStream(new char[] {});
        List<Character> emptyResult = emptyStream.filter(c -> true).toList();
        assertEquals(0, emptyResult.size());
    }

    @Test
    public void testFilter_SequentialFallback() {
        List<Character> result = createSingleThreadStream('a', 'b', 'c', 'd').filter(c -> c > 'b').toList();
        assertHaveSameElements(Arrays.asList('c', 'd'), result);
    }

    @Test
    public void testTakeWhile() {
        CharStream stream = createCharStream(TEST_ARRAY);
        CharPredicate predicate = c -> c < 'd';
        List<Character> result = stream.takeWhile(predicate).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains('a'));
        assertTrue(result.contains('b'));
        assertTrue(result.contains('c'));
        assertFalse(result.contains('d'));
    }

    @Test
    public void testTakeWhile_SequentialFallback() {
        List<Character> result = createSingleThreadStream('a', 'b', 'c', 'd').takeWhile(c -> c < 'c').toList();
        assertHaveSameElements(Arrays.asList('a', 'b'), result);
    }

    @Test
    public void testDropWhile() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c', 'd', 'e' });
        List<Character> result = stream.dropWhile(c -> c < 'd').toList();
        assertHaveSameElements(Arrays.asList('d', 'e'), result);

        CharStream stream2 = createCharStream(new char[] { 'a', 'b', 'c' });
        List<Character> result2 = stream2.dropWhile(c -> true).toList();
        assertEquals(0, result2.size());
    }

    @Test
    public void testDropWhile_SequentialFallback() {
        List<Character> result = createSingleThreadStream('a', 'b', 'c', 'd').dropWhile(c -> c < 'c').toList();
        assertHaveSameElements(Arrays.asList('c', 'd'), result);
    }

    @Test
    public void testMap() {
        CharStream stream = createCharStream(TEST_ARRAY);
        CharUnaryOperator mapper = c -> (char) (c + 1);
        List<Character> result = stream.map(mapper).toList();
        assertEquals(TEST_ARRAY.length, result.size());
        assertTrue(result.contains('b'));
        assertFalse(result.contains('a'));
    }

    @Test
    public void testMap_SequentialFallback() {
        List<Character> result = createSingleThreadStream('a', 'b', 'c').map(c -> (char) (c + 1)).toList();
        assertHaveSameElements(Arrays.asList('b', 'c', 'd'), result);
    }

    @Test
    public void testMapToInt() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c' });
        List<Integer> result = stream.mapToInt(c -> (int) c).toList();
        assertEquals(3, result.size());
        assertTrue(result.contains((int) 'a'));
        assertTrue(result.contains((int) 'b'));
        assertTrue(result.contains((int) 'c'));
    }

    @Test
    public void testMapToInt_SequentialFallback() {
        List<Integer> result = createSingleThreadStream('a', 'b', 'c').mapToInt(c -> c - 'a').toList();
        assertHaveSameElements(Arrays.asList(0, 1, 2), result);
    }

    @Test
    public void testMapToObj() {
        CharStream stream = createCharStream(TEST_ARRAY);
        CharFunction<String> mapper = c -> String.valueOf(c).toUpperCase();
        List<String> result = stream.mapToObj(mapper).toList();
        assertEquals(TEST_ARRAY.length, result.size());
        assertTrue(result.contains("A"));
        assertTrue(result.contains("Z"));
    }

    @Test
    public void testMapToObj_SequentialFallback() {
        List<String> result = createSingleThreadStream('a', 'b').mapToObj(c -> String.valueOf(c).toUpperCase()).toList();
        assertHaveSameElements(Arrays.asList("A", "B"), result);
    }

    @Test
    public void testFlatMapCharArray() {
        CharStream stream = createCharStream(new char[] { 'a', 'b' });
        CharFunction<char[]> mapper = c -> new char[] { c, (char) (c + 1) };
        List<Character> result = stream.flatmap(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains('a'));
        assertTrue(result.contains('b'));
        assertTrue(result.contains('c'));
    }

    @Test
    public void testFlatMap() {
        List<Character> result = createCharStream(new char[] { 'a', 'b' }).flatMap(c -> createCharStream(new char[] { c, Character.toUpperCase(c) }))
                .boxed()
                .toList();
        assertEquals(4, result.size());
        assertTrue(result.containsAll(Arrays.asList('a', 'A', 'b', 'B')));
    }

    @Test
    public void testFlatMap_SequentialFallback() {
        List<Character> result = createSingleThreadStream('a', 'b').flatMap(c -> CharStream.of(c, (char) (c + 1))).toList();
        assertHaveSameElements(Arrays.asList('a', 'b', 'b', 'c'), result);
    }

    @Test
    public void testFlatmap_SequentialFallback() {
        List<Character> result = createSingleThreadStream('a', 'b').flatmap(c -> new char[] { c, Character.toUpperCase(c) }).toList();
        assertHaveSameElements(Arrays.asList('a', 'A', 'b', 'B'), result);
    }

    @Test
    public void testFlatMapToInt() {
        CharStream stream = createCharStream(new char[] { 'a', 'b' });
        List<Integer> result = stream.flatMapToInt(c -> IntStream.of((int) c, (int) c + 1)).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains((int) 'a'));
        assertTrue(result.contains((int) 'a' + 1));
    }

    @Test
    public void testFlatMapToInt_SequentialFallback() {
        List<Integer> result = createSingleThreadStream('a', 'b').flatMapToInt(c -> IntStream.of((int) c)).toList();
        assertHaveSameElements(Arrays.asList((int) 'a', (int) 'b'), result);
    }

    @Test
    public void testFlatMapToObjStream() {
        CharStream stream = createCharStream(new char[] { 'a', 'b' });
        CharFunction<Stream<String>> mapper = c -> Stream.of(String.valueOf(c), String.valueOf((char) (c + 1)));
        List<String> result = stream.flatMapToObj(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains("a"));
        assertTrue(result.contains("b"));
        assertTrue(result.contains("c"));
    }

    @Test
    public void testFlatMapToObj_largeArray() {
        List<String> result = createCharStream(TEST_ARRAY).flatMapToObj(c -> Stream.of(new String[] { String.valueOf(c) })).toList();
        assertEquals(26, result.size());
        assertTrue(result.contains("a"));
        assertTrue(result.contains("z"));
    }

    @Test
    public void testFlatmapToObj_largeArray() {
        List<String> result = createCharStream(TEST_ARRAY).flatmapToObj(c -> Arrays.asList(String.valueOf(c), String.valueOf((char) (c + 1)))).toList();
        assertEquals(52, result.size());
    }

    @Test
    public void testFlatMapToObj() {
        List<String> result = createCharStream(new char[] { 'x', 'y' })
                .flatMapToObj(c -> Stream.of(String.valueOf(c), String.valueOf(Character.toUpperCase(c))))
                .toList();
        assertEquals(4, result.size());
        assertTrue(result.containsAll(Arrays.asList("x", "X", "y", "Y")));
    }

    @Test
    public void testFlatmapToObj() {
        List<String> result = createCharStream(new char[] { 'a', 'b' })
                .flatmapToObj(c -> Arrays.asList(String.valueOf(c), String.valueOf(Character.toUpperCase(c))))
                .toList();
        assertEquals(4, result.size());
        assertTrue(result.containsAll(Arrays.asList("a", "A", "b", "B")));
    }

    @Test
    public void testFlatMapToObj_SequentialFallback() {
        List<String> result = createSingleThreadStream('a', 'b').flatMapToObj(c -> Stream.of(String.valueOf(c))).toList();
        assertHaveSameElements(Arrays.asList("a", "b"), result);
    }

    @Test
    public void testFlatmapToObj_SequentialFallback() {
        List<String> result = createSingleThreadStream('a', 'b').flatmapToObj(c -> Arrays.asList(String.valueOf(c), String.valueOf(Character.toUpperCase(c))))
                .toList();
        assertHaveSameElements(Arrays.asList("a", "A", "b", "B"), result);
    }

    @Test
    public void testOnEach_ParallelPath_Iterator() {
        AtomicInteger count = new AtomicInteger(0);
        long total = createCharStream(TEST_ARRAY).peek(c -> count.incrementAndGet()).count();
        assertEquals(TEST_ARRAY.length, total);
        assertEquals(TEST_ARRAY.length, count.get());
    }

    @Test
    public void testOnEach() {
        CharStream stream = createCharStream(TEST_ARRAY);
        List<Character> consumed = new ArrayList<>();
        CharConsumer action = it -> {
            synchronized (consumed) {
                consumed.add(it);
            }
        };
        stream.peek(action).forEach(c -> {
        });
        assertEquals(TEST_ARRAY.length, consumed.size());

        assertHaveSameElements(N.toList(TEST_ARRAY), consumed);
    }

    @Test
    public void testOnEach_SequentialFallback() {
        AtomicInteger count = new AtomicInteger(0);
        createSingleThreadStream('a', 'b', 'c').peek(c -> count.incrementAndGet()).count();
        assertEquals(3, count.get());
    }

    @Test
    public void testForEach_ParallelPath_Iterator() {
        AtomicInteger count = new AtomicInteger(0);
        createCharStream(TEST_ARRAY).forEach(c -> count.incrementAndGet());
        assertEquals(TEST_ARRAY.length, count.get());
    }

    @Test
    public void testForEach_SequentialFallback() {
        AtomicInteger count = new AtomicInteger(0);
        createSingleThreadStream('a', 'b', 'c').forEach(c -> count.incrementAndGet());
        assertEquals(3, count.get());
    }

    @Test
    public void testForEachWithException() {
        CharStream stream = createCharStream(TEST_ARRAY);
        AtomicInteger count = new AtomicInteger(0);
        assertThrows(RuntimeException.class, () -> {
            stream.forEach(c -> {
                if (count.incrementAndGet() > 5) {
                    throw new RuntimeException("Test Exception");
                }
            });
        });
    }

    @Test
    public void testToMapWithMergeFunction() {
        Map<String, Integer> result = createCharStream(new char[] { 'a', 'b', 'a' }).toMap(c -> String.valueOf(c), c -> (int) c, (v1, v2) -> v1 + v2);
        assertEquals(2, result.size());
        assertEquals(97 + 97, (int) result.get("a"));
        assertEquals(98, (int) result.get("b"));
    }

    @Test
    public void testToMap_SequentialFallback() {
        Map<String, Integer> result = createSingleThreadStream('a', 'b').toMap(c -> String.valueOf(c), c -> (int) c, (v1, v2) -> v1, ConcurrentHashMap::new);
        assertEquals(2, result.size());
        assertEquals((int) 'a', (int) result.get("a"));
    }

    @Test
    public void testGroupTo() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'a', 'c', 'b' });
        Map<String, List<Character>> result = stream.groupTo(c -> String.valueOf(c), Collectors.toList(), ConcurrentHashMap::new);
        assertEquals(3, result.size());
        assertEquals(List.of('a', 'a'), result.get("a"));
        assertEquals(List.of('b', 'b'), result.get("b"));
        assertEquals(List.of('c'), result.get("c"));
    }

    @Test
    public void testGroupTo_SequentialFallback() {
        Map<Boolean, List<Character>> result = createSingleThreadStream('a', 'b', 'c', 'd').groupTo(c -> c < 'c', Collectors.toList(), ConcurrentHashMap::new);
        assertEquals(2, result.size());
        assertEquals(2, result.get(true).size());
        assertEquals(2, result.get(false).size());
    }

    @Test
    public void testReduceWithIdentity() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c' });
        CharBinaryOperator op = (c1, c2) -> (char) (c1 + c2 - 'a');
        char result = stream.reduce('a', op);
        assertEquals('d', result);

        CharStream emptyStream = createCharStream(new char[] {});
        char emptyResult = emptyStream.reduce((char) 0, (a, b) -> (char) (a + b));
        assertEquals((char) 0, emptyResult);
    }

    @Test
    public void testReduceWithoutIdentity() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c' });
        OptionalChar result = stream.reduce((c1, c2) -> (char) (c1 + c2 - 'a'));
        assertTrue(result.isPresent());

        CharStream emptyStream = createCharStream(new char[] {});
        OptionalChar emptyResult = emptyStream.reduce((c1, c2) -> (char) (c1 + c2));
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testReduce_parallelWithLargeArray() {
        // Just ensure reduce works with parallel execution
        OptionalChar opt = createCharStream(TEST_ARRAY).reduce((a, b) -> (char) Math.max(a, b));
        assertTrue(opt.isPresent());
        assertEquals('z', opt.get());
    }

    @Test
    public void testReduce_withIdentityParallel() {
        char result = createCharStream(new char[] { 'a', 'b', 'c' }).reduce('a', (a, b) -> (char) Math.max(a, b));
        assertEquals('c', result);
    }

    @Test
    public void testReduce_emptyStreamParallel() {
        // With max operator, identity 'a' accumulated across threads: max('a','a') = 'a'
        char result = createCharStream(new char[0]).reduce('a', (a, b) -> (char) Math.max(a, b));
        assertEquals('a', result);

        OptionalChar opt = createCharStream(new char[0]).reduce((a, b) -> (char) Math.max(a, b));
        assertFalse(opt.isPresent());
    }

    @Test
    public void testConstructor_withDefaultValues() {
        CharStream stream = CharStream.of('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't').parallel();
        OptionalChar opt = stream.reduce((a, b) -> (char) Math.max(a, b));
        assertTrue(opt.isPresent());
        assertEquals('t', opt.get());
    }

    // Covers delayed-match ordering for iterator-backed terminal operations.
    @Test
    public void testReduceAndFindOperations_DelayedMatchOrdering() {
        assertEquals('o', createCharStream('e', 'b', 'd', 'i', 'k', 'o').reduce('a', (left, right) -> (char) Math.max(left, right)));

        OptionalChar reduced = createCharStream('e', 'b', 'd', 'i', 'k', 'o').reduce((left, right) -> (char) Math.max(left, right));
        assertTrue(reduced.isPresent());
        assertEquals('o', reduced.get());

        OptionalChar firstMatch = createCharStream('e', 'b', 'd', 'i', 'k', 'o').findFirst(c -> {
            if (c == 'e') {
                try {
                    Thread.sleep(10L);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }

            return c == 'e' || c == 'i' || c == 'o';
        });
        assertTrue(firstMatch.isPresent());
        assertEquals('e', firstMatch.get());

        OptionalChar anyMatch = createCharStream('e', 'b', 'd', 'i', 'k', 'o').findAny(c -> c == 'e' || c == 'i' || c == 'o');
        assertTrue(anyMatch.isPresent());
        assertTrue(anyMatch.get() == 'e' || anyMatch.get() == 'i' || anyMatch.get() == 'o');

        OptionalChar lastMatch = createCharStream('e', 'b', 'd', 'i', 'k', 'o').findLast(c -> {
            if (c == 'o') {
                try {
                    Thread.sleep(10L);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }

            return c == 'e' || c == 'i' || c == 'o';
        });
        assertTrue(lastMatch.isPresent());
        assertEquals('o', lastMatch.get());
    }

    @Test
    public void testCollect() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c' });
        StringBuilder sb = stream.collect(StringBuilder::new, StringBuilder::append, StringBuilder::append);
        assertHaveSameElements("abc".toCharArray(), sb.toString().toCharArray());
    }

    @Test
    public void testCollect_SequentialFallback() {
        StringBuilder sb = createSingleThreadStream('a', 'b', 'c').collect(StringBuilder::new, StringBuilder::append, StringBuilder::append);
        assertEquals(3, sb.length());
        assertTrue(sb.toString().contains("a"));
    }

    @Test
    public void testAnyMatch() {
        assertTrue(createCharStream(TEST_ARRAY).anyMatch(c -> c == 'z'));
        assertFalse(createCharStream(TEST_ARRAY).anyMatch(c -> c == '1'));
        assertFalse(createCharStream(new char[] {}).anyMatch(c -> true));
    }

    @Test
    public void testAnyMatch_SequentialFallback() {
        assertTrue(createSingleThreadStream('a', 'b', 'c').anyMatch(c -> c == 'c'));
        assertFalse(createSingleThreadStream('a', 'b', 'c').anyMatch(c -> c == 'z'));
    }

    @Test
    public void testAllMatch() {
        assertTrue(createCharStream(TEST_ARRAY).allMatch(c -> c >= 'a' && c <= 'z'));
        assertFalse(createCharStream(TEST_ARRAY).allMatch(c -> c < 'z'));
        assertTrue(createCharStream(new char[] {}).allMatch(c -> false));
    }

    @Test
    public void testAllMatch_SequentialFallback() {
        assertTrue(createSingleThreadStream('a', 'b', 'c').allMatch(c -> c >= 'a' && c <= 'z'));
        assertFalse(createSingleThreadStream('a', 'B', 'c').allMatch(c -> c >= 'a' && c <= 'z'));
    }

    @Test
    public void testNoneMatch() {
        assertTrue(createCharStream(TEST_ARRAY).noneMatch(c -> c == '1'));
        assertFalse(createCharStream(TEST_ARRAY).noneMatch(c -> c == 'a'));
        assertTrue(createCharStream(new char[] {}).noneMatch(c -> true));
    }

    @Test
    public void testNoneMatch_SequentialFallback() {
        assertTrue(createSingleThreadStream('a', 'b', 'c').noneMatch(c -> c == 'z'));
        assertFalse(createSingleThreadStream('a', 'b', 'c').noneMatch(c -> c == 'a'));
    }

    @Test
    public void testFindFirst() {
        CharStream stream = createCharStream(new char[] { 'd', 'b', 'a', 'c', 'a' });
        OptionalChar result = stream.findFirst(c -> c == 'a');
        assertTrue(result.isPresent());
        assertEquals('a', result.get());

        stream = createCharStream(new char[] { 'd', 'b', 'a', 'c', 'a' });
        OptionalChar notFound = stream.findFirst(c -> c == 'x');
        assertFalse(notFound.isPresent());

        OptionalChar empty = createCharStream(new char[] {}).findFirst(c -> true);
        assertFalse(empty.isPresent());
    }

    @Test
    public void testFindFirst_SequentialFallback() {
        OptionalChar found = createSingleThreadStream('a', 'b', 'c').findFirst(c -> c == 'b');
        assertTrue(found.isPresent());
        assertEquals('b', found.get());

        OptionalChar notFound = createSingleThreadStream('a', 'b', 'c').findFirst(c -> c == 'z');
        assertFalse(notFound.isPresent());
    }

    @Test
    public void testFindAny() {
        CharStream stream = createCharStream(TEST_ARRAY);
        OptionalChar result = stream.findAny(c -> c == 'm');
        assertTrue(result.isPresent());
        assertEquals('m', result.get());

        CharStream stream2 = createCharStream(TEST_ARRAY);
        OptionalChar notFound = stream2.findAny(c -> c == '1');
        assertFalse(notFound.isPresent());

        OptionalChar empty = createCharStream(new char[] {}).findAny(c -> true);
        assertFalse(empty.isPresent());
    }

    @Test
    public void testFindAny_SequentialFallback() {
        OptionalChar found = createSingleThreadStream('a', 'b', 'c').findAny(c -> c == 'b');
        assertTrue(found.isPresent());

        OptionalChar notFound = createSingleThreadStream('a', 'b', 'c').findAny(c -> c == 'z');
        assertFalse(notFound.isPresent());
    }

    @Test
    public void testFindLast() {
        CharStream stream = createCharStream(new char[] { 'd', 'b', 'a', 'c', 'a' });
        OptionalChar result = stream.findLast(c -> c == 'a');
        assertTrue(result.isPresent());
        assertEquals('a', result.get());

        stream = createCharStream(new char[] { 'd', 'b', 'a', 'c', 'a' });
        OptionalChar notFound = stream.findLast(c -> c == 'x');
        assertFalse(notFound.isPresent());

        OptionalChar empty = createCharStream(new char[] {}).findLast(c -> true);
        assertFalse(empty.isPresent());
    }

    @Test
    public void testFindLast_SequentialFallback() {
        OptionalChar found = createSingleThreadStream('a', 'b', 'a', 'c').findLast(c -> c == 'a');
        assertTrue(found.isPresent());
        assertEquals('a', found.get());

        OptionalChar notFound = createSingleThreadStream('a', 'b', 'c').findLast(c -> c == 'z');
        assertFalse(notFound.isPresent());
    }

    @Test
    public void testZipWithTwoStreams() {
        CharStream streamA = createCharStream(new char[] { 'a', 'b', 'c' });
        CharStream streamB = CharStream.of('x', 'y', 'z');
        List<Character> result = streamA.zipWith(streamB, (c1, c2) -> (char) (c1 + c2 - 'a')).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testZipWithTwoStreamsWithDefaultValues() {
        CharStream streamA = createCharStream(new char[] { 'a', 'b', 'c' });
        CharStream streamB = CharStream.of('x');
        char defaultA = '*';
        char defaultB = '#';
        List<Character> result = streamA.zipWith(streamB, defaultA, defaultB, (c1, c2) -> (char) (c1 + c2 - 'a')).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testZipWithTernaryOperator() {
        CharStream streamA = createCharStream(new char[] { 'a', 'b' });
        CharStream streamB = CharStream.of('x', 'y');
        CharStream streamC = CharStream.of('1', '2');
        CharTernaryOperator zipper = (c1, c2, c3) -> (char) (c1 + c2 + c3 - 2 * 'a');
        List<Character> result = streamA.zipWith(streamB, streamC, zipper).sorted().toList();
        assertEquals(2, result.size());
        assertEquals((char) ('a' + 'x' + '1' - 2 * 'a'), result.get(0));
        assertEquals((char) ('b' + 'y' + '2' - 2 * 'a'), result.get(1));
    }

    @Test
    public void testZipWithTernaryOperatorWithNoneValues() {
        CharStream streamA = createCharStream(new char[] { 'a' });
        CharStream streamB = CharStream.of('x', 'y');
        CharStream streamC = CharStream.of('1', '2', '3');
        char valA = '*';
        char valB = '#';
        char valC = '@';
        CharTernaryOperator zipper = (c1, c2, c3) -> {
            return (char) (c1 + c2 + c3 - 2 * 'a');
        };
        List<Character> result = streamA.zipWith(streamB, streamC, valA, valB, valC, zipper).sorted().toList();
        assertEquals(3, result.size());
        assertHaveSameElements(List.of((char) ('a' + 'x' + '1' - 2 * 'a'), (char) ('*' + 'y' + '2' - 2 * 'a'), (char) ('*' + '#' + '3' - 2 * 'a')), result);
    }

    @Test
    public void testZipWithDefaultValues_SequentialFallback() {
        List<Character> result = CharStream.of('a', 'b')
                .map(e -> (char) (e + 0))
                .parallel(PS.create(Splitor.ITERATOR).maxThreadNum(1))
                .zipWith(CharStream.of('x'), '0', '?', (a, b) -> b == '?' ? a : b)
                .toList();

        assertEquals(Arrays.asList('x', 'b'), result);
    }

    @Test
    public void testIsParallel() {
        CharStream stream = createCharStream(TEST_ARRAY);
        assertTrue(stream.isParallel());
        stream.close();
    }

    @Test
    public void testSequential() {
        CharStream parallelStream = createCharStream(TEST_ARRAY);
        CharStream sequentialStream = parallelStream.sequential();
        assertFalse(sequentialStream.isParallel());
        List<Character> result = sequentialStream.toList();
        assertEquals(TEST_ARRAY.length, result.size());
        assertArrayEquals(TEST_ARRAY, result.stream().map(Object::toString).collect(Collectors.joining()).toCharArray());
        parallelStream.close();
        sequentialStream.close();
    }

    @Test
    public void testMaxThreadNum() throws IllegalAccessException, NoSuchFieldException {
        assertEquals(testMaxThreadNum, ((ParallelIteratorCharStream) createCharStream(TEST_ARRAY)).maxThreadNum());
    }

    @Test
    public void testSplitor() throws IllegalAccessException, NoSuchFieldException {
        assertEquals(Splitor.ITERATOR, ((ParallelIteratorCharStream) createCharStream(TEST_ARRAY)).splitor());
    }

    @Test
    public void testAsyncExecutor() throws IllegalAccessException, NoSuchFieldException {
        assertTrue(((ParallelIteratorCharStream) createCharStream(TEST_ARRAY)).asyncExecutor() != null);
    }

    @Test
    public void testOnCloseMultipleHandlers() {
        CharStream stream = createCharStream(TEST_ARRAY);
        AtomicInteger closedCount = new AtomicInteger(0);
        Runnable handler1 = () -> closedCount.incrementAndGet();
        Runnable handler2 = () -> closedCount.incrementAndGet();

        CharStream newStream = stream.onClose(handler1).onClose(handler2);
        assertEquals(0, closedCount.get());
        newStream.close();
        assertEquals(2, closedCount.get());
    }
}
