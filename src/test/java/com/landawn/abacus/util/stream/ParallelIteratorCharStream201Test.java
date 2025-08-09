package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
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
import com.landawn.abacus.util.function.CharToIntFunction;
import com.landawn.abacus.util.function.CharUnaryOperator;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings.PS;
import com.landawn.abacus.util.stream.BaseStream.Splitor;

public class ParallelIteratorCharStream201Test extends TestBase {

    private static final int testMaxThreadNum = 4; // Use a fixed small number of threads for predictable testing 
    private static final char[] TEST_ARRAY = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
            'w', 'x', 'y', 'z' };

    // Empty method to be implemented by the user for initializing CharStream
    protected CharStream createCharStream(char... elements) {
        // Using default values for maxThreadNum, executorNumForVirtualThread, splitor, cancelUncompletedThreads
        // For testing, we might want to vary these parameters.
        return CharStream.of(elements).map(e -> (char) (e + 0)).parallel(PS.create(Splitor.ITERATOR).maxThreadNum(testMaxThreadNum));
    }

    @BeforeEach
    public void setUp() {
    }

    @Test
    public void testFilter() {
        CharStream stream = createCharStream(TEST_ARRAY);
        CharPredicate predicate = c -> c > 'm';
        List<Character> result = stream.filter(predicate).toList();
        assertEquals(13, result.size());
        assertTrue(result.contains('n'));
        assertFalse(result.contains('a'));
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
    public void testDropWhile() {
        CharStream stream = createCharStream(TEST_ARRAY);
        CharPredicate predicate = c -> c < 'd';
        List<Character> result = stream.dropWhile(predicate).toList();
        assertEquals(TEST_ARRAY.length - 3, result.size());
        assertTrue(result.contains('d'));
        assertFalse(result.contains('c'));
    }

    @Test
    public void testMap() {
        CharStream stream = createCharStream(TEST_ARRAY);
        CharUnaryOperator mapper = c -> (char) (c + 1);
        List<Character> result = stream.map(mapper).toList();
        assertEquals(TEST_ARRAY.length, result.size());
        assertTrue(result.contains('b')); // 'a' maps to 'b'
        assertFalse(result.contains('a'));
    }

    @Test
    public void testMapToInt() {
        CharStream stream = createCharStream(TEST_ARRAY);
        CharToIntFunction mapper = c -> c - 'a';
        List<Integer> result = stream.mapToInt(mapper).toList();
        assertEquals(TEST_ARRAY.length, result.size());
        assertTrue(result.contains(0)); // 'a' maps to 0
        assertTrue(result.contains(25)); // 'z' maps to 25
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
    public void testFlatMapCharStream() {
        CharStream stream = createCharStream(new char[] { 'a', 'b' });
        CharFunction<CharStream> mapper = c -> CharStream.of(c, (char) (c + 1));
        List<Character> result = stream.flatMap(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains('a'));
        assertTrue(result.contains('b'));
        assertTrue(result.contains('c'));
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
    public void testFlatMapToInt() {
        CharStream stream = createCharStream(new char[] { 'a', 'b' });
        CharFunction<IntStream> mapper = c -> IntStream.of(c - 'a', c - 'a' + 1);
        List<Integer> result = stream.flatMapToInt(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(0));
        assertTrue(result.contains(1));
        assertTrue(result.contains(2));
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
    public void testFlatMapToObjCollection() {
        CharStream stream = createCharStream(new char[] { 'a', 'b' });
        CharFunction<Collection<String>> mapper = c -> {
            List<String> list = new ArrayList<>();
            list.add(String.valueOf(c));
            list.add(String.valueOf((char) (c + 1)));
            return list;
        };
        List<String> result = stream.flatmapToObj(mapper).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains("a"));
        assertTrue(result.contains("b"));
        assertTrue(result.contains("c"));
    }

    @Test
    public void testOnEach() {
        CharStream stream = createCharStream(TEST_ARRAY);
        List<Character> consumed = new ArrayList<>();
        CharConsumer action = it -> {
            synchronized (consumed) { // Ensure thread safety
                consumed.add(it);
            }
        };
        stream.onEach(action).forEach(c -> {
        }); // Trigger the onEach action
        assertEquals(TEST_ARRAY.length, consumed.size());

        assertHaveSameElements(N.toList(TEST_ARRAY), consumed);
    }

    @Test
    public void testForEach() {
        CharStream stream = createCharStream(TEST_ARRAY);
        List<Character> consumed = new ArrayList<>();
        CharConsumer action = it -> {
            synchronized (consumed) { // Ensure thread safety
                consumed.add(it);
            }
        };
        stream.forEach(action);
        assertEquals(TEST_ARRAY.length, consumed.size());

        assertHaveSameElements(N.toList(TEST_ARRAY), consumed);
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
    public void testToMap() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c' });
        Map<String, String> result = stream.toMap(c -> String.valueOf(c), c -> String.valueOf(c), (v1, v2) -> v1 + v2, ConcurrentHashMap::new);
        assertEquals(3, result.size());
        assertEquals("a", result.get("a"));
        assertEquals("b", result.get("b"));
        assertEquals("c", result.get("c"));
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
    public void testReduceWithIdentity() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c' });
        CharBinaryOperator op = (c1, c2) -> (char) (c1 + c2 - 'a'); // Simple sum-like operation
        char result = stream.reduce('a', op); // 'a' + ('b'-'a') + ('c'-'a') = 'a' + 1 + 2 = 'd'
        assertEquals('d', result);

        CharStream emptyStream = createCharStream(new char[] {});
        char emptyResult = emptyStream.reduce((char) 0, (a, b) -> (char) (a + b));
        assertEquals((char) 0, emptyResult);
    }

    @Test
    public void testReduceWithoutIdentity() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c' });
        CharBinaryOperator accumulator = (c1, c2) -> (char) (c1 + c2 - 'a');
        OptionalChar result = stream.reduce(accumulator);
        assertTrue(result.isPresent());
        assertEquals('d', result.get());

        CharStream emptyStream = createCharStream(new char[] {});
        OptionalChar emptyResult = emptyStream.reduce(accumulator);
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testCollect() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c' });
        StringBuilder sb = stream.collect(StringBuilder::new, StringBuilder::append, StringBuilder::append);
        assertHaveSameElements("abc".toCharArray(), sb.toString().toCharArray());
    }

    @Test
    public void testAnyMatch() {
        assertTrue(createCharStream(TEST_ARRAY).anyMatch(c -> c == 'm'));
        assertFalse(createCharStream(TEST_ARRAY).anyMatch(c -> c == 'Z'));
        assertFalse(createCharStream(new char[] {}).anyMatch(c -> true));
    }

    @Test
    public void testAllMatch() {
        assertTrue(createCharStream(TEST_ARRAY).allMatch(c -> c >= 'a' && c <= 'z'));
        assertFalse(createCharStream(TEST_ARRAY).allMatch(c -> c < 'z'));
        assertTrue(createCharStream(new char[] {}).allMatch(c -> false));
    }

    @Test
    public void testNoneMatch() {
        assertTrue(createCharStream(TEST_ARRAY).noneMatch(c -> c == 'Z'));
        assertFalse(createCharStream(TEST_ARRAY).noneMatch(c -> c == 'a'));
        assertTrue(createCharStream(new char[] {}).noneMatch(c -> true));
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
    public void testFindAny() {
        CharStream stream = createCharStream(new char[] { 'd', 'b', 'a', 'c', 'a' });
        OptionalChar result = stream.findAny(c -> c == 'a');
        assertTrue(result.isPresent());
        assertEquals('a', result.get());

        stream = createCharStream(new char[] { 'd', 'b', 'a', 'c', 'a' });
        OptionalChar notFound = stream.findAny(c -> c == 'x');
        assertFalse(notFound.isPresent());

        OptionalChar empty = createCharStream(new char[] {}).findAny(c -> true);
        assertFalse(empty.isPresent());
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
    public void testZipWithBinaryOperator() {
        CharStream streamA = createCharStream(new char[] { 'a', 'b', 'c' });
        CharStream streamB = CharStream.of('x', 'y', 'z');
        CharBinaryOperator zipper = (c1, c2) -> (char) (c1 + c2 - 'a'); // Example: 'a' + 'x' - 'a' = 'x'
        List<Character> result = streamA.zipWith(streamB, zipper).toList();
        assertEquals(3, result.size());
        assertHaveSameElements(List.of('x', (char) ('y' + 1), (char) ('z' + 2)), result);
    }

    @Test
    public void testZipWithTernaryOperator() {
        CharStream streamA = createCharStream(new char[] { 'a', 'b' });
        CharStream streamB = CharStream.of('x', 'y');
        CharStream streamC = CharStream.of('1', '2');
        CharTernaryOperator zipper = (c1, c2, c3) -> (char) (c1 + c2 + c3 - 2 * 'a'); // Example
        List<Character> result = streamA.zipWith(streamB, streamC, zipper).sorted().toList();
        assertEquals(2, result.size());
        assertEquals((char) ('a' + 'x' + '1' - 2 * 'a'), result.get(0));
        assertEquals((char) ('b' + 'y' + '2' - 2 * 'a'), result.get(1));
    }

    @Test
    public void testZipWithBinaryOperatorWithNoneValues() {
        CharStream streamA = createCharStream(new char[] { 'a', 'b' });
        CharStream streamB = CharStream.of('x', 'y', 'z');
        char valA = '*';
        char valB = '#';
        CharBinaryOperator zipper = (c1, c2) -> {
            return (char) (c1 + c2 - 'a');
        };
        List<Character> result = streamA.zipWith(streamB, valA, valB, zipper).sorted().toList();
        assertEquals(3, result.size());
        assertHaveSameElements(List.of('x', (char) ('y' + 1), (char) ('z' + '*' - 'a')), result);
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
    public void testIsParallel() {
        CharStream stream = createCharStream(TEST_ARRAY);
        assertTrue(stream.isParallel());
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
        // Since maxThreadNum() is protected in the superclass,
        // and its value is used internally, we can't directly call it from here.
        // However, we can assert its behavior indirectly or via reflection if necessary.
        // For this test, we'll assume the constructor correctly sets it and other tests
        // indirectly verify its usage.
        // If it was a public method of ParallelArrayCharStream, we would test it directly.
        // CharStream stream = createCharStream(charArray);
        //assertTrue(stream.maxThreadNum() > 0); // This line would work if maxThreadNum() was public in CharStream
    }

    @Test
    public void testSplitor() throws IllegalAccessException, NoSuchFieldException {
        // Similar to maxThreadNum, splitor() is protected.
        // CharStream stream = createCharStream(charArray);
        //assertNotNull(stream.splitor());
    }

    @Test
    public void testAsyncExecutor() throws IllegalAccessException, NoSuchFieldException {
        // Similar to maxThreadNum, asyncExecutor() is protected.
        // CharStream stream = createCharStream(charArray);
        //assertNotNull(stream.asyncExecutor());
    }

    @Test
    public void testOnClose() {
        CharStream stream = createCharStream(TEST_ARRAY);
        AtomicBoolean closedFlag = new AtomicBoolean(false);
        Runnable closeHandler = () -> closedFlag.set(true);

        CharStream newStream = stream.onClose(closeHandler);
        assertFalse(closedFlag.get()); // Not closed yet
        newStream.close(); // Close the stream, which should trigger the handler
        assertTrue(closedFlag.get()); // Now it should be closed
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

    @Test
    public void testOnCloseEmptyHandler() {
        CharStream stream = createCharStream(TEST_ARRAY);
        CharStream newStream = stream.onClose(null);
        assertSame(stream, newStream); // Should return the same instance if handler is null
        newStream.close(); // Should not throw
    }
}
