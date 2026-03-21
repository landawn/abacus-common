package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
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

public class ParallelArrayCharStreamTest extends TestBase {

    private static final int testMaxThreadNum = 4;
    private static final char[] TEST_ARRAY = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
            'w', 'x', 'y', 'z' };

    private CharStream stream;

    protected CharStream createCharStream(char... elements) {
        return CharStream.of(elements).parallel(PS.create(Splitor.ARRAY).maxThreadNum(testMaxThreadNum));
    }

    protected CharStream createIteratorSplitorCharStream(char... elements) {
        return new ParallelArrayCharStream(elements, 0, elements.length, false, testMaxThreadNum, Splitor.ITERATOR, null, false, new ArrayList<>());
    }

    @BeforeEach
    public void setUp() {
        stream = createCharStream(TEST_ARRAY);
    }

    // Covers the iterator-based terminal-operation branch in ParallelArrayCharStream.
    @Test
    public void testReduceAndFindMethods_IteratorSplitor() {
        assertEquals('e', createIteratorSplitorCharStream('d', 'b', 'a', 'c', 'e').reduce((left, right) -> (char) Math.max(left, right)).get());

        OptionalChar reduced = createIteratorSplitorCharStream('d', 'b', 'a', 'c', 'e').reduce((left, right) -> (char) Math.max(left, right));
        assertTrue(reduced.isPresent());
        assertEquals('e', reduced.get());

        OptionalChar firstVowel = createIteratorSplitorCharStream('d', 'b', 'a', 'c', 'e').findFirst(c -> c == 'a' || c == 'e');
        assertTrue(firstVowel.isPresent());
        assertEquals('a', firstVowel.get());

        OptionalChar anyVowel = createIteratorSplitorCharStream('d', 'b', 'a', 'c', 'e').findAny(c -> c == 'a' || c == 'e');
        assertTrue(anyVowel.isPresent());
        assertTrue(anyVowel.get() == 'a' || anyVowel.get() == 'e');

        OptionalChar lastVowel = createIteratorSplitorCharStream('d', 'b', 'a', 'c', 'e').findLast(c -> c == 'a' || c == 'e');
        assertTrue(lastVowel.isPresent());
        assertEquals('e', lastVowel.get());

        OptionalChar notFound = createIteratorSplitorCharStream('d', 'b', 'a', 'c', 'e').findAny(c -> c == 'z');
        assertFalse(notFound.isPresent());
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
    public void testLargeArrays() {
        char[] largeArray = new char[1000];
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = (char) ('a' + (i % 26));
        }

        CharStream largeStream = createCharStream(largeArray);
        assertEquals(1000, largeStream.count());

        largeStream = createCharStream(largeArray);
        long aCount = largeStream.filter(c -> c == 'a').count();
        assertTrue(aCount > 0);
    }

    @Test
    public void testEdgeCases() {
        CharStream emptyStream = createCharStream(new char[0]);
        assertEquals(0, emptyStream.count());

        CharStream singleStream = createCharStream(new char[] { 'z' });
        assertEquals('z', singleStream.first().get());

        CharStream mixedStream = createCharStream(new char[] { 'A', 'z', '0', '9' });
        long digitCount = mixedStream.filter(Character::isDigit).count();
        assertEquals(2, digitCount);
    }

    // ---- Sequential-fallback path tests (single-element array => canBeSequential == true) ----

    /** Single-element triggers canBeSequential path in filter. */
    @Test
    public void testFilter_SequentialFallback() {
        List<Character> result = createCharStream(new char[] { 'm' }).filter(c -> c == 'm').toList();
        assertEquals(1, result.size());
        assertEquals('m', result.get(0));

        List<Character> empty = createCharStream(new char[] { 'm' }).filter(c -> c == 'z').toList();
        assertEquals(0, empty.size());
    }

    @Test
    public void testStreamStateAfterOperations() {
        CharStream s = createCharStream(TEST_ARRAY);
        s.count();

        assertThrows(IllegalStateException.class, () -> {
            s.filter(c -> c > 'm').count();
        });
    }

    @Test
    public void testExceptionHandling() {
        assertThrows(RuntimeException.class, () -> {
            createCharStream(TEST_ARRAY).filter(c -> {
                if (c == 'e')
                    throw new RuntimeException("Test exception in filter");
                return true;
            }).count();
        });

        assertThrows(RuntimeException.class, () -> {
            createCharStream(TEST_ARRAY).map(c -> {
                if (c == 'g')
                    throw new RuntimeException("Test exception in map");
                return c;
            }).toArray();
        });
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
        List<Character> result = createCharStream(new char[] { 'a' }).takeWhile(c -> c < 'z').toList();
        assertEquals(1, result.size());
        assertEquals('a', result.get(0));
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
    public void testDropWhile_SequentialFallback() {
        List<Character> result = createCharStream(new char[] { 'a' }).dropWhile(c -> c < 'z').toList();
        assertEquals(0, result.size());

        List<Character> result2 = createCharStream(new char[] { 'z' }).dropWhile(c -> c < 'a').toList();
        assertEquals(1, result2.size());
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
        List<Character> result = createCharStream(new char[] { 'a' }).map(c -> (char) (c + 1)).toList();
        assertEquals(1, result.size());
        assertEquals('b', result.get(0));
    }

    @Test
    public void testMapToInt() {
        CharStream stream = createCharStream(TEST_ARRAY);
        CharToIntFunction mapper = c -> c - 'a';
        List<Integer> result = stream.mapToInt(mapper).toList();
        assertEquals(TEST_ARRAY.length, result.size());
        assertTrue(result.contains(0));
        assertTrue(result.contains(25));
    }

    @Test
    public void testMapToInt_SequentialFallback() {
        List<Integer> result = createCharStream(new char[] { 'a' }).mapToInt(c -> c - 'a').toList();
        assertEquals(1, result.size());
        assertEquals(0, result.get(0));
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
        List<String> result = createCharStream(new char[] { 'a' }).mapToObj(c -> String.valueOf(c).toUpperCase()).toList();
        assertEquals(1, result.size());
        assertEquals("A", result.get(0));
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
    public void testFlatMap_SequentialFallback() {
        List<Character> result = createCharStream(new char[] { 'a' }).flatMap(c -> CharStream.of(c, (char) (c + 1))).toList();
        assertEquals(2, result.size());
        assertTrue(result.contains('a'));
        assertTrue(result.contains('b'));
    }

    @Test
    public void testFlatmap_SequentialFallback() {
        List<Character> result = createCharStream(new char[] { 'a' }).flatmap(c -> new char[] { c, (char) (c + 1) }).toList();
        assertEquals(2, result.size());
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
    public void testFlatmapCharArray() {
        List<Character> result = createCharStream(new char[] { 'a', 'b' }).flatmap(c -> new char[] { c, Character.toUpperCase(c) }).boxed().toList();
        assertEquals(4, result.size());
        assertTrue(result.containsAll(Arrays.asList('a', 'A', 'b', 'B')));
    }

    // Tests covering parallel code paths with large array (26 elements forces parallel path)

    @Test
    public void testFlatMap_ParallelPath() {
        long count = createCharStream(TEST_ARRAY).flatMap(c -> CharStream.of(c, Character.toUpperCase(c))).count();
        assertEquals(52, count);
    }

    @Test
    public void testFlatmap_ParallelPath() {
        long count = createCharStream(TEST_ARRAY).flatmap(c -> new char[] { c, Character.toUpperCase(c) }).count();
        assertEquals(52, count);
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
    public void testFlatMapToInt_ParallelPath() {
        long count = createCharStream(TEST_ARRAY).flatMapToInt(c -> IntStream.of((int) c)).count();
        assertEquals(26, count);
    }

    @Test
    public void testFlatMapToInt_SequentialFallback() {
        List<Integer> result = createCharStream(new char[] { 'a' }).flatMapToInt(c -> IntStream.of((int) c)).toList();
        assertEquals(1, result.size());
        assertEquals((int) 'a', result.get(0));
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
    public void testFlatMapToObj_ParallelPath() {
        long count = createCharStream(TEST_ARRAY).flatMapToObj(c -> Stream.of(String.valueOf(c))).count();
        assertEquals(26, count);
    }

    @Test
    public void testFlatmapToObj_ParallelPath() {
        long count = createCharStream(TEST_ARRAY).flatmapToObj(c -> Arrays.asList(String.valueOf(c))).count();
        assertEquals(26, count);
    }

    @Test
    public void testFlatMapToObj_SequentialFallback() {
        List<String> result = createCharStream(new char[] { 'a' }).flatMapToObj(c -> Stream.of(String.valueOf(c))).toList();
        assertEquals(1, result.size());
        assertEquals("a", result.get(0));
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
    public void testFlatmapToObj_SequentialFallback() {
        List<String> result = createCharStream(new char[] { 'a' }).flatmapToObj(c -> Arrays.asList(String.valueOf(c), String.valueOf(Character.toUpperCase(c))))
                .toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testOnEach_SequentialFallback() {
        AtomicInteger count = new AtomicInteger(0);
        List<Character> result = createCharStream(new char[] { 'a' }).peek(c -> count.incrementAndGet()).toList();
        assertEquals(1, result.size());
        assertEquals(1, count.get());
    }

    @Test
    public void testOnEach_ParallelPath() {
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
    public void testForEach_IteratorSplitor() {
        AtomicInteger count = new AtomicInteger(0);
        createIteratorSplitorCharStream('a', 'b', 'c', 'd', 'e').forEach(c -> count.incrementAndGet());
        assertEquals(5, count.get());
    }

    @Test
    public void testForEach_ParallelPath() {
        AtomicInteger count = new AtomicInteger(0);
        createCharStream(TEST_ARRAY).forEach(c -> count.incrementAndGet());
        assertEquals(TEST_ARRAY.length, count.get());
    }

    @Test
    public void testForEach_SequentialFallback() {
        AtomicInteger count = new AtomicInteger(0);
        createCharStream(new char[] { 'a' }).forEach(c -> count.incrementAndGet());
        assertEquals(1, count.get());
    }

    @Test
    public void testForEach() {
        CharStream stream = createCharStream(TEST_ARRAY);
        List<Character> consumed = new ArrayList<>();
        CharConsumer action = it -> {
            synchronized (consumed) {
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
    public void testToMapWithMergeFunction() {
        Map<String, Integer> result = createCharStream(new char[] { 'a', 'b', 'a' }).toMap(c -> String.valueOf(c), c -> (int) c, (v1, v2) -> v1 + v2);
        assertEquals(2, result.size());
        assertEquals(97 + 97, (int) result.get("a"));
        assertEquals(98, (int) result.get("b"));
    }

    @Test
    public void testToMap_ParallelPath() {
        Map<Character, Integer> result = createCharStream(TEST_ARRAY).toMap(c -> c, c -> (int) c, (a, b) -> a, java.util.HashMap::new);
        assertEquals(TEST_ARRAY.length, result.size());
    }

    @Test
    public void testToMap_SequentialFallback() {
        Map<String, String> result = createCharStream(new char[] { 'a' }).toMap(c -> String.valueOf(c), c -> String.valueOf(c).toUpperCase(), (v1, v2) -> v1,
                java.util.HashMap::new);
        assertEquals(1, result.size());
        assertEquals("A", result.get("a"));
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
    public void testGroupTo_ParallelPath() {
        Map<Boolean, List<Character>> result = createCharStream(TEST_ARRAY).groupTo(c -> c < 'n', Collectors.toList(), java.util.HashMap::new);
        assertEquals(2, result.size());
    }

    @Test
    public void testGroupTo_SequentialFallback() {
        Map<Boolean, List<Character>> result = createCharStream(new char[] { 'a' }).groupTo(c -> Character.isLowerCase(c), Collectors.toList(),
                java.util.HashMap::new);
        assertEquals(1, result.size());
        assertEquals(1, result.get(true).size());
    }

    @Test
    public void testReduceIdentity() {
        int sum = stream.reduce((char) 0, (c1, c2) -> (char) (c1 + c2));

        int sumExpected = N.toList(TEST_ARRAY).stream().mapToInt(c -> (int) c).sum();
        assertEquals(sumExpected, sum);

        sum = createCharStream(new char[] {}).reduce('Z', (c1, c2) -> c1);
        assertEquals('Z', sum);
    }

    @Test
    public void testReduceWithIdentity() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c' });
        CharBinaryOperator op = (c1, c2) -> (char) (c1 + c2 - 'a');
        char result = stream.reduce('a', op);
        assertEquals('d', result);

        CharStream emptyStream = createCharStream(new char[] {});
        char emptyResult = emptyStream.reduce('X', op);
        assertEquals('X', emptyResult);
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
    public void testReduce_WithIdentity_ParallelPath() {
        // reduce with identity using large array
        char result = createCharStream(TEST_ARRAY).reduce((char) 0, (c1, c2) -> (char) Math.max(c1, c2));
        assertEquals('z', result);
    }

    @Test
    public void testReduce_NoIdentity_ParallelPath() {
        OptionalChar result = createCharStream(TEST_ARRAY).reduce((c1, c2) -> (char) Math.max(c1, c2));
        assertTrue(result.isPresent());
        assertEquals('z', result.get());
    }

    @Test
    public void testReduce_SequentialFallback() {
        char result = createCharStream(new char[] { 'a' }).reduce('a', (c1, c2) -> (char) Math.max(c1, c2));
        assertEquals('a', result);

        OptionalChar opt = createCharStream(new char[] { 'z' }).reduce((c1, c2) -> (char) Math.max(c1, c2));
        assertTrue(opt.isPresent());
        assertEquals('z', opt.get());
    }

    @Test
    public void testIteratorSplitorReduceAndFindOperations_SparseMatch() {
        assertEquals('o', createIteratorSplitorCharStream('e', 'b', 'd', 'i', 'k', 'o').reduce('a', (left, right) -> (char) Math.max(left, right)));

        OptionalChar reduced = createIteratorSplitorCharStream('e', 'b', 'd', 'i', 'k', 'o').reduce((left, right) -> (char) Math.max(left, right));
        assertTrue(reduced.isPresent());
        assertEquals('o', reduced.get());

        OptionalChar firstMatch = createIteratorSplitorCharStream('e', 'b', 'd', 'i', 'k', 'o').findFirst(c -> {
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

        OptionalChar anyMatch = createIteratorSplitorCharStream('e', 'b', 'd', 'i', 'k', 'o').findAny(c -> c == 'e' || c == 'i' || c == 'o');
        assertTrue(anyMatch.isPresent());
        assertTrue(anyMatch.get() == 'e' || anyMatch.get() == 'i' || anyMatch.get() == 'o');

        OptionalChar lastMatch = createIteratorSplitorCharStream('e', 'b', 'd', 'i', 'k', 'o').findLast(c -> {
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
        assertEquals("abc", sb.toString());
    }

    @Test
    public void testCollect_ParallelPath() {
        List<Character> result = createCharStream(TEST_ARRAY).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertEquals(TEST_ARRAY.length, result.size());
    }

    @Test
    public void testCollect_SequentialFallback() {
        StringBuilder sb = createCharStream(new char[] { 'a' }).collect(StringBuilder::new, StringBuilder::append, StringBuilder::append);
        assertEquals("a", sb.toString());
    }

    @Test
    public void testCollect_IteratorSplitor() {
        List<Character> result = createIteratorSplitorCharStream('a', 'b', 'c').collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertHaveSameElements(List.of('a', 'b', 'c'), result);
    }

    @Test
    public void testAnyMatch() {
        assertTrue(createCharStream(TEST_ARRAY).anyMatch(c -> c == 'm'));
        assertFalse(createCharStream(TEST_ARRAY).anyMatch(c -> c == 'Z'));
        assertFalse(createCharStream(new char[] {}).anyMatch(c -> true));
    }

    @Test
    public void testAnyMatch_SequentialFallback() {
        assertTrue(createCharStream(new char[] { 'a' }).anyMatch(c -> c == 'a'));
        assertFalse(createCharStream(new char[] { 'a' }).anyMatch(c -> c == 'z'));
    }

    @Test
    public void testAllMatch() {
        assertTrue(createCharStream(TEST_ARRAY).allMatch(c -> c >= 'a' && c <= 'z'));
        assertFalse(createCharStream(TEST_ARRAY).allMatch(c -> c < 'z'));
        assertTrue(createCharStream(new char[] {}).allMatch(c -> false));
    }

    @Test
    public void testAllMatch_SequentialFallback() {
        assertTrue(createCharStream(new char[] { 'a' }).allMatch(c -> Character.isLowerCase(c)));
        assertFalse(createCharStream(new char[] { 'A' }).allMatch(c -> Character.isLowerCase(c)));
    }

    @Test
    public void testNoneMatch() {
        assertTrue(createCharStream(TEST_ARRAY).noneMatch(c -> c == 'Z'));
        assertFalse(createCharStream(TEST_ARRAY).noneMatch(c -> c == 'a'));
        assertTrue(createCharStream(new char[] {}).noneMatch(c -> true));
    }

    // Cover the ITERATOR splitor path for anyMatch / allMatch / noneMatch / collect / forEach
    @Test
    public void testAnyMatchAllMatchNoneMatch_IteratorSplitor() {
        assertTrue(createIteratorSplitorCharStream('a', 'b', 'c', 'd', 'e').anyMatch(c -> c == 'e'));
        assertFalse(createIteratorSplitorCharStream('a', 'b', 'c').anyMatch(c -> c == 'z'));

        assertTrue(createIteratorSplitorCharStream('a', 'b', 'c').allMatch(c -> c >= 'a' && c <= 'z'));
        assertFalse(createIteratorSplitorCharStream('a', 'b', 'C').allMatch(c -> c >= 'a' && c <= 'z'));

        assertTrue(createIteratorSplitorCharStream('a', 'b', 'c').noneMatch(c -> c == 'z'));
        assertFalse(createIteratorSplitorCharStream('a', 'b', 'c').noneMatch(c -> c == 'c'));
    }

    @Test
    public void testNoneMatch_SequentialFallback() {
        assertTrue(createCharStream(new char[] { 'a' }).noneMatch(c -> c == 'Z'));
        assertFalse(createCharStream(new char[] { 'a' }).noneMatch(c -> c == 'a'));
    }

    @Test
    public void testFindFirst_SequentialFallback() {
        OptionalChar found = createCharStream(new char[] { 'a' }).findFirst(c -> c == 'a');
        assertTrue(found.isPresent());
        assertEquals('a', found.get());

        OptionalChar notFound = createCharStream(new char[] { 'a' }).findFirst(c -> c == 'z');
        assertFalse(notFound.isPresent());
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
    public void testFindAny_SequentialFallback() {
        OptionalChar found = createCharStream(new char[] { 'a' }).findAny(c -> c == 'a');
        assertTrue(found.isPresent());
        assertEquals('a', found.get());

        OptionalChar notFound = createCharStream(new char[] { 'a' }).findAny(c -> c == 'z');
        assertFalse(notFound.isPresent());
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
    public void testFindLast_SequentialFallback() {
        OptionalChar found = createCharStream(new char[] { 'a' }).findLast(c -> c == 'a');
        assertTrue(found.isPresent());
        assertEquals('a', found.get());

        OptionalChar notFound = createCharStream(new char[] { 'a' }).findLast(c -> c == 'z');
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
    public void testZipWithBinaryOperator() {
        CharStream streamA = createCharStream(new char[] { 'a', 'b', 'c' });
        CharStream streamB = CharStream.of('x', 'y', 'z');
        CharBinaryOperator zipper = (c1, c2) -> (char) (c1 + c2 - 'a');
        List<Character> result = streamA.zipWith(streamB, zipper).toList();
        assertEquals(3, result.size());
        assertHaveSameElements(List.of('x', (char) ('y' + 1), (char) ('z' + 2)), result);
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
    public void testZipWithBinary_ParallelPath() {
        long count = createCharStream(TEST_ARRAY).zipWith(CharStream.of(TEST_ARRAY), (a, b) -> (char) Math.max(a, b)).count();
        assertEquals(TEST_ARRAY.length, count);
    }

    @Test
    public void testZipWithTernary_ParallelPath() {
        long count = createCharStream(TEST_ARRAY).zipWith(CharStream.of(TEST_ARRAY), CharStream.of(TEST_ARRAY), (a, b, c) -> (char) Math.max(a, Math.max(b, c)))
                .count();
        assertEquals(TEST_ARRAY.length, count);
    }

    @Test
    public void testZipWithBinaryDefaults_ParallelPath() {
        char[] shorter = java.util.Arrays.copyOf(TEST_ARRAY, 10);
        long count = createCharStream(TEST_ARRAY).zipWith(CharStream.of(shorter), (char) 0, (char) 0, (a, b) -> (char) Math.max(a, b)).count();
        assertEquals(TEST_ARRAY.length, count);
    }

    @Test
    public void testZipWithTernaryDefaults_ParallelPath() {
        char[] shorter = java.util.Arrays.copyOf(TEST_ARRAY, 10);
        long count = createCharStream(TEST_ARRAY)
                .zipWith(CharStream.of(shorter), CharStream.of(shorter), (char) 0, (char) 0, (char) 0, (a, b, c) -> (char) Math.max(a, Math.max(b, c)))
                .count();
        assertEquals(TEST_ARRAY.length, count);
    }

    @Test
    public void testZipWithBinaryDefaults_SequentialFallback() {
        List<Character> result = CharStream.of('a', 'b')
                .parallel(PS.create(Splitor.ARRAY).maxThreadNum(1))
                .zipWith(CharStream.of('x'), '0', '?', (a, b) -> b == '?' ? a : b)
                .toList();

        assertEquals(Arrays.asList('x', 'b'), result);
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
    public void testSequential_ParallelPath() {
        CharStream parallel = createCharStream(TEST_ARRAY);
        assertTrue(parallel.isParallel());
        CharStream seq = parallel.sequential();
        assertFalse(seq.isParallel());
        assertEquals(TEST_ARRAY.length, seq.count());
    }

    @Test
    public void testMaxThreadNum() throws IllegalAccessException, NoSuchFieldException {
        assertEquals(testMaxThreadNum, ((ParallelArrayCharStream) createCharStream(TEST_ARRAY)).maxThreadNum());
    }

    @Test
    public void testSplitor() throws IllegalAccessException, NoSuchFieldException {
        assertEquals(Splitor.ARRAY, ((ParallelArrayCharStream) createCharStream(TEST_ARRAY)).splitor());
    }

    @Test
    public void testAsyncExecutor() throws IllegalAccessException, NoSuchFieldException {
        assertTrue(((ParallelArrayCharStream) createCharStream(TEST_ARRAY)).asyncExecutor() != null);
    }

    @Test
    public void testOnClose() {
        CharStream stream = createCharStream(TEST_ARRAY);
        AtomicBoolean closedFlag = new AtomicBoolean(false);
        Runnable closeHandler = () -> closedFlag.set(true);

        CharStream newStream = stream.onClose(closeHandler);
        assertFalse(closedFlag.get());
        newStream.close();
        assertTrue(closedFlag.get());
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
        assertSame(stream, newStream);
        newStream.close();
    }
}
