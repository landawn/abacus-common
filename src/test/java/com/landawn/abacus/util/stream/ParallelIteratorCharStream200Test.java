package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings.PS;

public class ParallelIteratorCharStream200Test extends TestBase {

    private static final char[] TEST_ARRAY = new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j' };
    private static final int testMaxThreadNum = 4; // Use a fixed small number of threads for predictable testing 
    private CharStream stream;
    private CharStream stream2;
    private CharStream stream3;

    // This method will be used to initialize CharStream instances for tests.
    protected CharStream createCharStream(char[] array) {
        // By default, create a parallel stream with a small number of threads.
        // For testing specific parallel behavior, direct instantiation might be needed.
        return CharStream.of(array).map(c -> (char) (c + 0)).parallel(PS.create(Stream.Splitor.ITERATOR).maxThreadNum(testMaxThreadNum));
    }

    // Helper to create a parallel stream with specific configuration
    private CharStream createParallelStreamWithConfig(char[] array, int maxThreadNum, Stream.Splitor splitor) {
        final ExecutorService testAsyncExecutor = Executors.newFixedThreadPool(maxThreadNum);
        return CharStream.of(array).map(c -> (char) (c + 0)).parallel(PS.create(splitor).maxThreadNum(maxThreadNum).executor(testAsyncExecutor)).onClose(() -> {
            if (testAsyncExecutor != null && !testAsyncExecutor.isTerminated()) {
                testAsyncExecutor.shutdown();
            }
        });
    }

    @BeforeEach
    public void setUp() {
        // Initialize with some default data for general tests
        stream = createCharStream(TEST_ARRAY);
        stream2 = createCharStream(TEST_ARRAY);
        stream3 = createCharStream(TEST_ARRAY);
    }

    // Helper to shut down the executor after tests
    @AfterEach
    public void tearDown() {
    }

    @Test
    public void testIsParallel() {
        assertTrue(stream.isParallel());
        assertTrue(createParallelStreamWithConfig(new char[] { 'a' }, 1, CharStream.Splitor.ARRAY).isParallel());
    }

    @Test
    public void testSequential() {
        CharStream sequentialStream = stream.sequential();
        assertFalse(sequentialStream.isParallel());

        // Verify that operations on the sequential stream still work correctly
        List<Character> result = sequentialStream.filter(c -> c > 'c').boxed().toList();
        assertEquals(Arrays.asList('d', 'e', 'f', 'g', 'h', 'i', 'j'), result);
    }

    @Test
    public void testFilter() {
        List<Character> result = stream.filter(c -> c % 2 == 0).boxed().toList(); // 'b', 'd', 'f', 'h', 'j'
        assertHaveSameElements(Arrays.asList('b', 'd', 'f', 'h', 'j'), result);

        result = createCharStream(new char[] {}).filter(c -> true).boxed().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testTakeWhile() {
        List<Character> result = stream.takeWhile(c -> c < 'f').boxed().toList();
        assertHaveSameElements(Arrays.asList('a', 'b', 'c', 'd', 'e'), result);

        result = createCharStream(new char[] { 'a', 'b', 'c' }).takeWhile(c -> c > 'z').boxed().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDropWhile() {
        List<Character> result = stream.dropWhile(c -> c < 'f').boxed().toList();
        assertHaveSameElements(Arrays.asList('f', 'g', 'h', 'i', 'j'), result);

        result = createCharStream(new char[] { 'a', 'b', 'c' }).dropWhile(c -> c == 'x').boxed().toList();
        assertHaveSameElements(Arrays.asList('a', 'b', 'c'), result);
    }

    @Test
    public void testMap() {
        List<Character> result = stream.map(c -> Character.toUpperCase(c)).boxed().toList();
        assertHaveSameElements(Arrays.asList('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J'), result);
    }

    @Test
    public void testMapToInt() {
        List<Integer> result = stream.mapToInt(c -> (int) c).boxed().toList();
        List<Integer> expected = N.map(Arrays.asList('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j'), c -> (int) c);
        assertHaveSameElements(expected, result);
    }

    @Test
    public void testMapToObj() {
        List<String> result = stream.mapToObj(c -> String.valueOf(c) + "!").toList();
        assertHaveSameElements(Arrays.asList("a!", "b!", "c!", "d!", "e!", "f!", "g!", "h!", "i!", "j!"), result);
    }

    @Test
    public void testFlatMap() {
        List<Character> result = createCharStream(new char[] { 'a', 'b' }).flatMap(c -> createCharStream(new char[] { c, Character.toUpperCase(c) }))
                .boxed()
                .toList();
        // Parallel flatMap can produce elements in a non-deterministic order for intermediate streams.
        // We only assert the content is correct, not the order.
        assertEquals(4, result.size());
        assertTrue(result.containsAll(Arrays.asList('a', 'A', 'b', 'B')));
    }

    @Test
    public void testFlatmapCharArray() {
        List<Character> result = createCharStream(new char[] { 'a', 'b' }).flatmap(c -> new char[] { c, Character.toUpperCase(c) }).boxed().toList();
        // Similar to flatMap, order can be non-deterministic due to parallel processing.
        assertEquals(4, result.size());
        assertTrue(result.containsAll(Arrays.asList('a', 'A', 'b', 'B')));
    }

    @Test
    public void testFlatMapToInt() {
        List<Integer> result = createCharStream(new char[] { '1', '2' }).flatMapToInt(c -> IntStream.of(c - '0', (c - '0') * 10)).boxed().toList();
        assertEquals(4, result.size());
        assertTrue(result.containsAll(Arrays.asList(1, 10, 2, 20)));
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
    public void testFlatmapToObjCollection() {
        List<String> result = createCharStream(new char[] { 'a', 'b' })
                .flatmapToObj(c -> Arrays.asList(String.valueOf(c), String.valueOf(Character.toUpperCase(c))))
                .toList();
        assertEquals(4, result.size());
        assertTrue(result.containsAll(Arrays.asList("a", "A", "b", "B")));
    }

    @Test
    public void testOnEach() {
        StringBuilder sb = new StringBuilder();
        List<Character> result = stream.onEach(e -> {
            synchronized (sb) { // Ensure thread-safe access to StringBuilder
                sb.append(e);
            }
        }).boxed().toList();
        // onEach can be executed on multiple threads, so order is not guaranteed.
        // However, all elements should be processed.
        assertEquals(10, sb.length());
        assertEquals(10, result.size());
        assertTrue(sb.toString().contains("a"));
        assertTrue(sb.toString().contains("j"));
    }

    @Test
    public void testForEach() {
        AtomicInteger count = new AtomicInteger(0);
        char[] elements = new char[] { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
                'Y', 'Z' };
        createCharStream(elements).forEach(c -> count.incrementAndGet());
        assertEquals(elements.length, count.get());

    }

    @Test
    public void testToMap() {
        // Parallel map operations usually lose order, so only test content correctness.
        Map<String, Integer> result = stream.toMap(c -> String.valueOf(c), c -> (int) c);
        assertEquals(10, result.size());
        assertEquals(97, (int) result.get("a"));
        assertEquals(106, (int) result.get("j"));
    }

    @Test
    public void testToMapDuplicateKeys() {
        assertThrows(IllegalStateException.class, () -> createCharStream(new char[] { 'a', 'a' }).toMap(c -> String.valueOf(c), c -> (int) c));
    }

    @Test
    public void testToMapWithMergeFunction() {
        Map<String, Integer> result = createCharStream(new char[] { 'a', 'b', 'a' }).toMap(c -> String.valueOf(c), c -> (int) c, (v1, v2) -> v1 + v2);
        assertEquals(2, result.size());
        assertEquals(97 + 97, (int) result.get("a"));
        assertEquals(98, (int) result.get("b"));
    }

    @Test
    public void testGroupTo() {
        Map<Boolean, List<Character>> result = stream.groupTo(c -> c % 2 == 0, Collectors.toList());
        assertEquals(2, result.size());
        assertHaveSameElements(Arrays.asList('b', 'd', 'f', 'h', 'j'), result.get(true)); // Even ASCII values
        assertHaveSameElements(Arrays.asList('a', 'c', 'e', 'g', 'i'), result.get(false)); // Odd ASCII values
    }

    @Test
    public void testReduceIdentity() {
        int sum = stream.reduce((char) 0, (c1, c2) -> (char) (c1 + c2)); // Summing numerical char values
        // This sum is ASCII based, '0' + 'a' + 'b' ...
        // Need to calculate expected value accurately: '0' + 97 + 98 + ...
        // Sum of ASCII 'a' through 'j' is 97+98+99+100+101+102+103+104+105+106 = 1015
        // Initial '0' (ASCII 48). So 48 + 1015 = 1063.

        int sumExpected = N.toList(TEST_ARRAY).stream().mapToInt(c -> (int) c).sum(); // Adding ASCII value of '0'
        assertEquals(sumExpected, sum);

        sum = createCharStream(new char[] {}).reduce('Z', (c1, c2) -> c1);
        assertEquals('Z', sum);
    }

    @Test
    public void testReduce() {
        OptionalChar sum = createCharStream(new char[] { '1', '2', '3' }).reduce((c1, c2) -> (char) (c1 + c2 - '0'));
        assertTrue(sum.isPresent());
        assertEquals((char) ('1' + '2' + '3' - '0' - '0'), sum.get());

        OptionalChar emptySum = createCharStream(new char[] {}).reduce((c1, c2) -> c1);
        assertFalse(emptySum.isPresent());
    }

    @Test
    public void testCollect() {
        StringBuilder sb = stream.collect(StringBuilder::new, StringBuilder::append, StringBuilder::append);
        assertHaveSameElements(N.toList("abcdefghij".toCharArray()), N.toList(sb.toString().toCharArray()));

        sb = createCharStream(new char[] {}).collect(StringBuilder::new, StringBuilder::append, StringBuilder::append);
        assertEquals("", sb.toString());
    }

    @Test
    public void testAnyMatch() {
        assertTrue(stream.anyMatch(c -> c == 'e'));
        assertFalse(stream2.anyMatch(c -> c == 'x'));
        assertFalse(createCharStream(new char[] {}).anyMatch(c -> true));
    }

    @Test
    public void testAllMatch() {
        assertTrue(stream.allMatch(c -> c >= 'a' && c <= 'j'));
        assertFalse(stream2.allMatch(c -> c == 'c'));
        assertTrue(createCharStream(new char[] {}).allMatch(c -> true));
    }

    @Test
    public void testNoneMatch() {
        assertTrue(stream.noneMatch(c -> c == 'z'));
        assertFalse(stream2.noneMatch(c -> c == 'c'));
        assertTrue(createCharStream(new char[] {}).noneMatch(c -> true));
    }

    @Test
    public void testFindFirst() {
        OptionalChar first = stream.findFirst(c -> c == 'c');
        assertTrue(first.isPresent());
        assertEquals('c', first.get());

        first = stream2.findFirst(c -> c == 'x');
        assertFalse(first.isPresent());
    }

    @Test
    public void testFindAny() {
        // FindAny is non-deterministic for parallel streams, so we just check if it's present for a known element.
        OptionalChar any = stream.findAny(c -> c == 'c');
        assertTrue(any.isPresent());
        assertTrue(stream2.boxed().toList().contains(any.get())); // It should be one of the elements
    }

    @Test
    public void testFindLast() {
        OptionalChar last = stream.findLast(c -> c == 'c');
        assertTrue(last.isPresent());
        assertEquals('c', last.get());

        last = stream2.findLast(c -> c == 'j');
        assertTrue(last.isPresent());
        assertEquals('j', last.get());

        last = stream3.findLast(c -> c == 'x');
        assertFalse(last.isPresent());
    }

    @Test
    public void testZipWithBinaryOperator() {
        CharStream s1 = createCharStream(new char[] { '1', '2', '3' });
        CharStream s2 = createCharStream(new char[] { 'a', 'b', 'c', 'd' });

        List<Character> result = s1.zipWith(s2, (c1, c2) -> (char) (c1 + c2)).boxed().toList();
        assertHaveSameElements(Arrays.asList((char) ('1' + 'a'), (char) ('2' + 'b'), (char) ('3' + 'c')), result);
    }

    @Test
    public void testZipWithTernaryOperator() {
        CharStream s1 = createCharStream(new char[] { '1', '2', '3' });
        CharStream s2 = createCharStream(new char[] { 'a', 'b', 'c' });
        CharStream s3 = createCharStream(new char[] { 'x', 'y' });

        List<Character> result = s1.zipWith(s2, s3, (c1, c2, c3) -> (char) (c1 + c2 + c3)).boxed().toList();
        assertHaveSameElements(Arrays.asList((char) ('1' + 'a' + 'x'), (char) ('2' + 'b' + 'y')), result);
    }

    @Test
    public void testZipWithBinaryOperatorWithNoneValues() {
        CharStream s1 = createCharStream(new char[] { '1', '2' });
        CharStream s2 = createCharStream(new char[] { 'a', 'b', 'c' });

        List<Character> result = s1.zipWith(s2, 'X', 'Y', (c1, c2) -> (char) (c1 + c2)).boxed().toList();
        assertHaveSameElements(Arrays.asList((char) ('1' + 'a'), (char) ('2' + 'b'), (char) ('X' + 'c')), result);
    }

    @Test
    public void testZipWithTernaryOperatorWithNoneValues() {
        CharStream s1 = createCharStream(new char[] { '1', '2' });
        CharStream s2 = createCharStream(new char[] { 'a', 'b', 'c' });
        CharStream s3 = createCharStream(new char[] { 'x' });

        List<Character> result = s1.zipWith(s2, s3, 'X', 'Y', 'Z', (c1, c2, c3) -> (char) (c1 + c2 + c3)).boxed().toList();
        assertHaveSameElements(Arrays.asList((char) ('1' + 'a' + 'x'), (char) ('2' + 'b' + 'Z'), (char) ('X' + 'c' + 'Z')), result);
    }

    @Test
    public void testOnClose() {
        AtomicInteger closeCount = new AtomicInteger(0);
        CharStream parallelStream = createCharStream(new char[] { 'a', 'b', 'c' }).onClose(() -> closeCount.incrementAndGet());
        parallelStream.count(); // Terminal operation
        assertEquals(1, closeCount.get());

        closeCount.set(0);
        parallelStream = createCharStream(new char[] { 'a', 'b', 'c' }).onClose(() -> closeCount.incrementAndGet()).onClose(() -> closeCount.incrementAndGet());
        parallelStream.toList(); // Another terminal operation
        assertEquals(2, closeCount.get());
    }

    // Since CharStream does not directly expose static methods that differ from CharStream,
    // and its constructors are internal/protected, testing of static methods would typically go into CharStreamTest.
    // The createStream method here already uses CharStream indirectly for convenience.
}
