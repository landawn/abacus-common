package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.CharSummaryStatistics;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.IndexedChar;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.function.CharBiFunction;
import com.landawn.abacus.util.function.CharBiPredicate;
import com.landawn.abacus.util.function.CharBinaryOperator;
import com.landawn.abacus.util.function.CharConsumer;
import com.landawn.abacus.util.function.CharFunction;
import com.landawn.abacus.util.function.CharPredicate;
import com.landawn.abacus.util.function.CharTernaryOperator;
import com.landawn.abacus.util.function.CharTriPredicate;

@Tag("new-test")
public class AbstractCharStream100Test extends TestBase {

    private CharStream charStream;

    @BeforeEach
    public void setUp() {
        charStream = createCharStream(new char[] { 'a', 'b', 'c', 'd', 'e' });
    }

    protected CharStream createCharStream(char... elements) {
        return CharStream.of(elements);
    }

    @Test
    public void testRateLimited() {
        RateLimiter rateLimiter = RateLimiter.create(10);
        CharStream result = charStream.rateLimited(rateLimiter);
        assertNotNull(result);

        assertThrows(IllegalArgumentException.class, () -> {
            charStream.rateLimited(null);
        });
    }

    @Test
    public void testDelay() {
        Duration delay = Duration.ofMillis(10);
        CharStream result = charStream.delay(delay);
        assertNotNull(result);

        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c', 'd' });
        assertThrows(IllegalArgumentException.class, () -> stream.delay((Duration) null));
    }

    @Test
    public void testSkipUntil() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c', 'd' });
        CharPredicate predicate = c -> c == 'c';
        char[] result = stream.skipUntil(predicate).toArray();
        assertArrayEquals(new char[] { 'c', 'd' }, result);
    }

    @Test
    public void testDistinct() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'a', 'c', 'b', 'd' });
        char[] result = stream.distinct().toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, result);
    }

    @Test
    public void testFlatmapWithCharArray() {
        CharStream stream = createCharStream(new char[] { 'a', 'b' });
        CharFunction<char[]> mapper = c -> new char[] { c, Character.toUpperCase(c) };
        char[] result = stream.flatmap(mapper).toArray();
        assertArrayEquals(new char[] { 'a', 'A', 'b', 'B' }, result);
    }

    @Test
    public void testFlatmapToObj() {
        CharStream stream = createCharStream(new char[] { 'a', 'b' });
        CharFunction<List<String>> mapper = c -> Arrays.asList(String.valueOf(c), String.valueOf(Character.toUpperCase(c)));
        List<String> result = stream.flatmapToObj(mapper).toList();
        assertEquals(Arrays.asList("a", "A", "b", "B"), result);
    }

    @Test
    public void testFlattMapToObj() {
        CharStream stream = createCharStream(new char[] { 'a', 'b' });
        CharFunction<String[]> mapper = c -> new String[] { String.valueOf(c), String.valueOf(Character.toUpperCase(c)) };
        List<String> result = stream.flattmapToObj(mapper).toList();
        assertEquals(Arrays.asList("a", "A", "b", "B"), result);
    }

    @Test
    public void testMapPartial() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c' });
        CharFunction<OptionalChar> mapper = c -> c == 'b' ? OptionalChar.empty() : OptionalChar.of(Character.toUpperCase(c));
        char[] result = stream.mapPartial(mapper).toArray();
        assertArrayEquals(new char[] { 'A', 'C' }, result);
    }

    @Test
    public void testRangeMap() {
        CharStream stream = createCharStream(new char[] { 'a', 'a', 'b', 'b', 'c' });
        CharBiPredicate sameRange = (a, b) -> a == b;
        CharBinaryOperator mapper = (first, last) -> last;
        char[] result = stream.rangeMap(sameRange, mapper).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    public void testRangeMapToObj() {
        CharStream stream = createCharStream(new char[] { 'a', 'a', 'b', 'b', 'c' });
        CharBiPredicate sameRange = (a, b) -> a == b;
        CharBiFunction<String> mapper = (first, last) -> String.valueOf(first) + String.valueOf(last);
        List<String> result = stream.rangeMapToObj(sameRange, mapper).toList();
        assertEquals(Arrays.asList("aa", "bb", "cc"), result);
    }

    @Test
    public void testCollapse() {
        CharStream stream = createCharStream(new char[] { 'a', 'a', 'b', 'b', 'c' });
        CharBiPredicate collapsible = (a, b) -> a == b;
        List<CharList> result = stream.collapse(collapsible).toList();
        assertEquals(3, result.size());
        assertEquals(CharList.of('a', 'a'), result.get(0));
        assertEquals(CharList.of('b', 'b'), result.get(1));
        assertEquals(CharList.of('c'), result.get(2));
    }

    @Test
    public void testCollapseWithMergeFunction() {
        CharStream stream = createCharStream(new char[] { 'a', 'a', 'b', 'b', 'c' });
        CharBiPredicate collapsible = (a, b) -> a == b;
        CharBinaryOperator mergeFunction = (a, b) -> b;
        char[] result = stream.collapse(collapsible, mergeFunction).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    public void testCollapseWithTriPredicate() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c', 'd', 'e' });
        CharTriPredicate collapsible = (first, prev, curr) -> Math.abs(curr - prev) == 1;
        CharBinaryOperator mergeFunction = (a, b) -> b;
        char[] result = stream.collapse(collapsible, mergeFunction).toArray();
        assertEquals(1, result.length);
        assertEquals('e', result[0]);
    }

    @Test
    public void testSkipWithAction() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c', 'd', 'e' });
        List<Character> skipped = new ArrayList<>();
        CharConsumer action = skipped::add;
        char[] result = stream.skip(2, action).toArray();
        assertArrayEquals(new char[] { 'c', 'd', 'e' }, result);
        assertEquals(Arrays.asList('a', 'b'), skipped);

        assertThrows(IllegalArgumentException.class, () -> {
            createCharStream(new char[] { 'a' }).skip(-1, action);
        });
    }

    @Test
    public void testFilterWithAction() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c', 'd' });
        List<Character> dropped = new ArrayList<>();
        CharPredicate predicate = c -> c > 'b';
        CharConsumer action = dropped::add;
        char[] result = stream.filter(predicate, action).toArray();
        assertArrayEquals(new char[] { 'c', 'd' }, result);
        assertEquals(Arrays.asList('a', 'b'), dropped);
    }

    @Test
    public void testDropWhileWithAction() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c', 'd' });
        List<Character> dropped = new ArrayList<>();
        CharPredicate predicate = c -> c < 'c';
        CharConsumer action = dropped::add;
        char[] result = stream.dropWhile(predicate, action).toArray();
        assertArrayEquals(new char[] { 'c', 'd' }, result);
        assertEquals(Arrays.asList('a', 'b'), dropped);
    }

    @Test
    public void testStep() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c', 'd', 'e', 'f' });
        char[] result = stream.step(2).toArray();
        assertArrayEquals(new char[] { 'a', 'c', 'e' }, result);

        stream = createCharStream(new char[] { 'a', 'b', 'c' });
        result = stream.step(1).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);

        assertThrows(IllegalArgumentException.class, () -> {
            createCharStream(new char[] { 'a' }).step(0);
        });
    }

    @Test
    public void testScan() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c' });
        CharBinaryOperator accumulator = (a, b) -> (char) Math.max(a, b);
        char[] result = stream.scan(accumulator).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    public void testScanWithInit() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c' });
        char init = 'z';
        CharBinaryOperator accumulator = (a, b) -> (char) Math.max(a, b);
        char[] result = stream.scan(init, accumulator).toArray();
        assertArrayEquals(new char[] { 'z', 'z', 'z' }, result);
    }

    @Test
    public void testScanWithInitIncluded() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c' });
        char init = 'z';
        CharBinaryOperator accumulator = (a, b) -> (char) Math.max(a, b);

        char[] result = stream.scan(init, true, accumulator).toArray();
        assertArrayEquals(new char[] { 'z', 'z', 'z', 'z' }, result);

        stream = createCharStream(new char[] { 'a', 'b', 'c' });
        result = stream.scan(init, false, accumulator).toArray();
        assertArrayEquals(new char[] { 'z', 'z', 'z' }, result);
    }

    @Test
    public void testIntersection() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c', 'd' });
        List<Character> c = Arrays.asList('b', 'c', 'e');
        char[] result = stream.intersection(c).toArray();
        assertArrayEquals(new char[] { 'b', 'c' }, result);
    }

    @Test
    public void testDifference() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c', 'd' });
        List<Character> c = Arrays.asList('b', 'c', 'e');
        char[] result = stream.difference(c).toArray();
        assertArrayEquals(new char[] { 'a', 'd' }, result);
    }

    @Test
    public void testSymmetricDifference() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c', 'd' });
        List<Character> c = Arrays.asList('b', 'c', 'e');
        char[] result = stream.symmetricDifference(c).toArray();
        char[] expected = { 'a', 'd', 'e' };
        Arrays.sort(result);
        Arrays.sort(expected);
        assertArrayEquals(expected, result);
    }

    @Test
    public void testReversed() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c', 'd', 'e' });
        char[] result = stream.reversed().toArray();
        assertArrayEquals(new char[] { 'e', 'd', 'c', 'b', 'a' }, result);
    }

    @Test
    public void testRotated() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c', 'd', 'e' });

        char[] result = stream.rotated(2).toArray();
        assertArrayEquals(new char[] { 'd', 'e', 'a', 'b', 'c' }, result);

        stream = createCharStream(new char[] { 'a', 'b', 'c', 'd', 'e' });
        result = stream.rotated(-2).toArray();
        assertArrayEquals(new char[] { 'c', 'd', 'e', 'a', 'b' }, result);

        stream = createCharStream(new char[] { 'a', 'b', 'c' });
        result = stream.rotated(0).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    public void testShuffled() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c', 'd', 'e' });
        Random rnd = new Random(42);
        char[] result = stream.shuffled(rnd).toArray();
        assertNotNull(result);
        assertEquals(5, result.length);

        assertThrows(IllegalArgumentException.class, () -> {
            createCharStream(new char[] { 'a' }).shuffled(null);
        });
    }

    @Test
    public void testSorted() {
        CharStream stream = createCharStream(new char[] { 'e', 'b', 'd', 'a', 'c' });
        char[] result = stream.sorted().toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, result);
    }

    @Test
    public void testReverseSorted() {
        CharStream stream = createCharStream(new char[] { 'b', 'e', 'a', 'd', 'c' });
        char[] result = stream.reverseSorted().toArray();
        assertArrayEquals(new char[] { 'e', 'd', 'c', 'b', 'a' }, result);
    }

    @Test
    public void testCycled() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c' });
        char[] result = stream.cycled().limit(10).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'a', 'b', 'c', 'a', 'b', 'c', 'a' }, result);
    }

    @Test
    public void testCycledWithRounds() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c' });

        char[] result = stream.cycled(2).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'a', 'b', 'c' }, result);

        stream = createCharStream(new char[] { 'a', 'b', 'c' });
        result = stream.cycled(0).toArray();
        assertArrayEquals(new char[] {}, result);

        stream = createCharStream(new char[] { 'a', 'b', 'c' });
        result = stream.cycled(1).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);

        assertThrows(IllegalArgumentException.class, () -> {
            createCharStream(new char[] { 'a' }).cycled(-1);
        });
    }

    @Test
    public void testIndexed() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c' });
        List<IndexedChar> result = stream.indexed().toList();
        assertEquals(3, result.size());
        assertEquals(IndexedChar.of('a', 0), result.get(0));
        assertEquals(IndexedChar.of('b', 1), result.get(1));
        assertEquals(IndexedChar.of('c', 2), result.get(2));
    }

    @Test
    public void testBoxed() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c' });
        List<Character> result = stream.boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c'), result);
    }

    @Test
    public void testPrependArray() {
        CharStream stream = createCharStream(new char[] { 'c', 'd', 'e' });
        char[] result = stream.prepend('a', 'b').toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, result);
    }

    @Test
    public void testPrependStream() {
        CharStream stream = createCharStream(new char[] { 'c', 'd', 'e' });
        CharStream toPrePend = createCharStream(new char[] { 'a', 'b' });
        char[] result = stream.prepend(toPrePend).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, result);
    }

    @Test
    public void testPrependOptional() {
        CharStream stream = createCharStream(new char[] { 'b', 'c' });

        char[] result = stream.prepend(OptionalChar.of('a')).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);

        stream = createCharStream(new char[] { 'b', 'c' });
        result = stream.prepend(OptionalChar.empty()).toArray();
        assertArrayEquals(new char[] { 'b', 'c' }, result);
    }

    @Test
    public void testAppendArray() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c' });
        char[] result = stream.append('d', 'e').toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, result);
    }

    @Test
    public void testAppendStream() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c' });
        CharStream toAppend = createCharStream(new char[] { 'd', 'e' });
        char[] result = stream.append(toAppend).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, result);
    }

    @Test
    public void testAppendOptional() {
        CharStream stream = createCharStream(new char[] { 'a', 'b' });

        char[] result = stream.append(OptionalChar.of('c')).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);

        stream = createCharStream(new char[] { 'a', 'b' });
        result = stream.append(OptionalChar.empty()).toArray();
        assertArrayEquals(new char[] { 'a', 'b' }, result);
    }

    @Test
    public void testAppendIfEmpty() {
        CharStream stream = createCharStream(new char[] {});
        char[] result = stream.appendIfEmpty('a', 'b').toArray();
        assertArrayEquals(new char[] { 'a', 'b' }, result);

        stream = createCharStream(new char[] { 'x', 'y' });
        result = stream.appendIfEmpty('a', 'b').toArray();
        assertArrayEquals(new char[] { 'x', 'y' }, result);
    }

    @Test
    public void testMergeWith() {
        CharStream streamA = createCharStream(new char[] { 'a', 'c', 'e' });
        CharStream streamB = createCharStream(new char[] { 'b', 'd', 'f' });
        CharBiFunction<MergeResult> selector = (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        char[] result = streamA.mergeWith(streamB, selector).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e', 'f' }, result);
    }

    @Test
    public void testZipWith() {
        CharStream streamA = createCharStream(new char[] { 'a', 'b', 'c' });
        CharStream streamB = createCharStream(new char[] { 'x', 'y', 'z' });
        CharBinaryOperator zipFunction = (a, b) -> (char) Math.max(a, b);
        char[] result = streamA.zipWith(streamB, zipFunction).toArray();
        assertArrayEquals(new char[] { 'x', 'y', 'z' }, result);
    }

    @Test
    public void testZipWithThreeStreams() {
        CharStream streamA = createCharStream(new char[] { 'a', 'b', 'c' });
        CharStream streamB = createCharStream(new char[] { 'x', 'y', 'z' });
        CharStream streamC = createCharStream(new char[] { '1', '2', '3' });
        CharTernaryOperator zipFunction = (a, b, c) -> c;
        char[] result = streamA.zipWith(streamB, streamC, zipFunction).toArray();
        assertArrayEquals(new char[] { '1', '2', '3' }, result);
    }

    @Test
    public void testZipWithDefaultValues() {
        CharStream streamA = createCharStream(new char[] { 'a', 'b' });
        CharStream streamB = createCharStream(new char[] { 'x', 'y', 'z' });
        char valueForNoneA = '0';
        char valueForNoneB = '9';
        CharBinaryOperator zipFunction = (a, b) -> (char) Math.max(a, b);
        char[] result = streamA.zipWith(streamB, valueForNoneA, valueForNoneB, zipFunction).toArray();
        assertArrayEquals(new char[] { 'x', 'y', 'z' }, result);
    }

    @Test
    public void testZipWithThreeStreamsAndDefaultValues() {
        CharStream streamA = createCharStream(new char[] { 'a' });
        CharStream streamB = createCharStream(new char[] { 'x', 'y' });
        CharStream streamC = createCharStream(new char[] { '1', '2', '3' });
        char valueForNoneA = '0';
        char valueForNoneB = '9';
        char valueForNoneC = '#';
        CharTernaryOperator zipFunction = (a, b, c) -> c;
        char[] result = streamA.zipWith(streamB, streamC, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction).toArray();
        assertArrayEquals(new char[] { '1', '2', '3' }, result);
    }

    @Test
    public void testToMap() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c' });
        Map<Character, String> result = stream.toMap(c -> c, c -> String.valueOf(c));
        assertEquals(3, result.size());
        assertEquals("a", result.get('a'));
        assertEquals("b", result.get('b'));
        assertEquals("c", result.get('c'));
    }

    @Test
    public void testToMapWithMapFactory() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c' });
        Map<Character, String> result = stream.toMap(c -> c, c -> String.valueOf(c), Suppliers.ofLinkedHashMap());
        assertTrue(result instanceof LinkedHashMap);
        assertEquals(3, result.size());
    }

    @Test
    public void testToMapWithMergeFunction() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'a' });
        Map<Character, String> result = stream.toMap(c -> c, c -> String.valueOf(c), (v1, v2) -> v1 + v2);
        assertEquals(2, result.size());
        assertEquals("aa", result.get('a'));
        assertEquals("b", result.get('b'));
    }

    @Test
    public void testGroupTo() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'a', 'c', 'b' });
        Map<Character, Long> result = stream.groupTo(c -> c, Collectors.counting());
        assertEquals(3, result.size());
        assertEquals(2L, result.get('a'));
        assertEquals(2L, result.get('b'));
        assertEquals(1L, result.get('c'));
    }

    @Test
    public void testForEachIndexed() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c' });
        List<String> result = new ArrayList<>();
        stream.forEachIndexed((index, value) -> result.add(index + ":" + value));
        assertEquals(Arrays.asList("0:a", "1:b", "2:c"), result);
    }

    @Test
    public void testFirst() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c' });
        OptionalChar result = stream.first();
        assertTrue(result.isPresent());
        assertEquals('a', result.get());

        stream = createCharStream(new char[] {});
        result = stream.first();
        assertFalse(result.isPresent());
    }

    @Test
    public void testLast() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c' });
        OptionalChar result = stream.last();
        assertTrue(result.isPresent());
        assertEquals('c', result.get());

        stream = createCharStream(new char[] {});
        result = stream.last();
        assertFalse(result.isPresent());
    }

    @Test
    public void testOnlyOne() {
        CharStream stream = createCharStream(new char[] { 'a' });
        OptionalChar result = stream.onlyOne();
        assertTrue(result.isPresent());
        assertEquals('a', result.get());

        stream = createCharStream(new char[] {});
        result = stream.onlyOne();
        assertFalse(result.isPresent());

        CharStream stream2 = createCharStream(new char[] { 'a', 'b' });
        assertThrows(TooManyElementsException.class, () -> stream2.onlyOne());
    }

    @Test
    public void testFindAny() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c', 'd' });
        OptionalChar result = stream.findAny(c -> c > 'b');
        assertTrue(result.isPresent());
        assertEquals('c', result.get());

        stream = createCharStream(new char[] { 'a', 'b' });
        result = stream.findAny(c -> c > 'z');
        assertFalse(result.isPresent());
    }

    @Test
    public void testPercentiles() {
        CharStream stream = createCharStream(new char[] { 'a', 'e', 'c', 'b', 'd' });
        Optional<Map<Percentage, Character>> result = stream.percentiles();
        assertTrue(result.isPresent());
        Map<Percentage, Character> percentiles = result.get();
        assertNotNull(percentiles);
        assertTrue(percentiles.containsKey(Percentage._0_0001));
        assertTrue(percentiles.containsKey(Percentage._50));
        assertTrue(percentiles.containsKey(Percentage._99_9999));

        stream = createCharStream(new char[] {});
        result = stream.percentiles();
        assertFalse(result.isPresent());
    }

    @Test
    public void testSummarizeAndPercentiles() {
        CharStream stream = createCharStream(new char[] { 'a', 'e', 'c', 'b', 'd' });
        Pair<CharSummaryStatistics, Optional<Map<Percentage, Character>>> result = stream.summarizeAndPercentiles();

        CharSummaryStatistics stats = result.left();
        assertEquals(5, stats.getCount());
        assertEquals('a', stats.getMin());
        assertEquals('e', stats.getMax());

        Optional<Map<Percentage, Character>> percentiles = result.right();
        assertTrue(percentiles.isPresent());

        stream = createCharStream(new char[] {});
        result = stream.summarizeAndPercentiles();
        stats = result.left();
        assertEquals(0, stats.getCount());
        assertFalse(result.right().isPresent());
    }

    @Test
    public void testJoin() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c' });
        String result = stream.join(", ", "[", "]");
        assertEquals("[a, b, c]", result);

        stream = createCharStream(new char[] { 'a', 'b', 'c' });
        result = stream.join("", "", "");
        assertEquals("abc", result);
    }

    @Test
    public void testJoinTo() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c' });
        Joiner joiner = Joiner.with(", ");
        Joiner result = stream.joinTo(joiner);
        assertEquals("a, b, c", result.toString());
    }

    @Test
    public void testCollect() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c' });
        StringBuilder result = stream.collect(StringBuilder::new, (sb, c) -> sb.append(c));
        assertEquals("abc", result.toString());
    }

    @Test
    public void testIterator() {
        CharStream stream = createCharStream(new char[] { 'a', 'b', 'c' });
        CharIterator iterator = stream.iterator();
        assertTrue(iterator.hasNext());
        assertEquals('a', iterator.nextChar());
        assertEquals('b', iterator.nextChar());
        assertEquals('c', iterator.nextChar());
        assertFalse(iterator.hasNext());
    }
}
