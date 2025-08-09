package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.CharSummaryStatistics;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.IndexedChar;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.RateLimiter;
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

public class AbstractCharStream200Test extends TestBase {

    private static final char[] TEST_ARRAY = new char[] { 'a', 'b', 'c', 'd', 'e' };
    private CharStream stream; // Assuming ConcreteCharStream extends AbstractCharStream
    private CharStream stream2; // Assuming ConcreteCharStream extends AbstractCharStream
    private CharStream stream3; // Assuming ConcreteCharStream extends AbstractCharStream

    @BeforeEach
    public void setUp() {
        // Initialize with some test data.
        // This concrete implementation takes a char array directly.
        stream = CharStream.of(TEST_ARRAY);
        stream2 = CharStream.of(TEST_ARRAY);
        stream3 = CharStream.of(TEST_ARRAY);
    }

    @Test
    public void testRateLimited() {
        // This method modifies the stream's behavior, making direct assertion on output tricky.
        // We'll test if the RateLimiter's acquire method is called.
        RateLimiter mockRateLimiter = mock(RateLimiter.class);
        when(mockRateLimiter.acquire()).thenReturn(1d);

        CharStream rateLimitedStream = stream.rateLimited(mockRateLimiter);
        rateLimitedStream.forEach(c -> {
        }); // Consume the stream

        // Verify that acquire was called for each element in the stream
        verify(mockRateLimiter, times(5)).acquire();
    }

    @Test
    public void testDelay() {
        // Similar to rateLimited, this modifies behavior.
        // We can test if N.sleepUninterruptibly is called.
        // This requires mocking a static method or using a custom N implementation for testing.
        // For now, a basic check that it runs without error.
        Duration delay = Duration.ofMillis(10);
        CharStream delayedStream = stream.delay(delay);
        long startTime = System.currentTimeMillis();
        delayedStream.forEach(c -> {
        });
        long endTime = System.currentTimeMillis();

        // With 5 elements and 10ms delay, expected delay is around 50ms.
        // Allowing for some overhead.
        assertTrue(endTime - startTime >= 5 * 10);
    }

    @Test
    public void testSkipUntil() {
        CharPredicate predicate = c -> c == 'c';
        List<Character> result = stream.skipUntil(predicate).boxed().toList();
        assertEquals(Arrays.asList('c', 'd', 'e'), result);

        result = CharStream.of(new char[] { 'a', 'b', 'c' }).skipUntil(c -> c == 'z').boxed().toList();
        assertEquals(Arrays.asList(), result);

        result = CharStream.of(new char[] { 'a', 'b', 'c' }).skipUntil(c -> c == 'a').boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c'), result);
    }

    @Test
    public void testDistinct() {
        stream = CharStream.of(new char[] { 'a', 'b', 'a', 'c', 'b', 'd' });
        List<Character> result = stream.distinct().boxed().toList();
        // The order might not be preserved based on the Set implementation.
        // Convert to a Set for comparison to ignore order.
        assertEquals(N.asSet('a', 'b', 'c', 'd'), N.newHashSet(result));
    }

    @Test
    public void testFlatmapCharArray() {
        CharFunction<char[]> mapper = c -> new char[] { c, Character.toUpperCase(c) };
        List<Character> result = stream.flatmap(mapper).boxed().toList();
        assertEquals(Arrays.asList('a', 'A', 'b', 'B', 'c', 'C', 'd', 'D', 'e', 'E'), result);

        result = CharStream.of(new char[] {}).flatmap(mapper).boxed().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlatmapToObjCollection() {
        CharFunction<Collection<? extends String>> mapper = c -> Arrays.asList(String.valueOf(c), String.valueOf(Character.toUpperCase(c)));
        List<String> result = stream.flatmapToObj(mapper).toList();
        assertEquals(Arrays.asList("a", "A", "b", "B", "c", "C", "d", "D", "e", "E"), result);

        result = CharStream.of(new char[] {}).flatmapToObj(mapper).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlattMapToObjArray() {
        CharFunction<String[]> mapper = c -> new String[] { String.valueOf(c), String.valueOf(Character.toUpperCase(c)) };
        List<String> result = stream.flattmapToObj(mapper).toList();
        assertEquals(Arrays.asList("a", "A", "b", "B", "c", "C", "d", "D", "e", "E"), result);

        result = CharStream.of(new char[] {}).flattmapToObj(mapper).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMapPartial() {
        CharFunction<OptionalChar> mapper = c -> {
            if (c % 2 == 0) { // Assuming 'b', 'd' are even (their char value)
                return OptionalChar.of((char) (c + 1));
            }
            return OptionalChar.empty();
        };
        // For 'a', 'b', 'c', 'd', 'e' (ASCII values: 97, 98, 99, 100, 101)
        // 'b' (98) -> 'c' (99)
        // 'd' (100) -> 'e' (101)
        List<Character> result = stream.mapPartial(mapper).boxed().toList();
        assertEquals(Arrays.asList('c', 'e'), result); // Based on ASCII values of 'b' and 'd'

        result = CharStream.of(new char[] {}).mapPartial(mapper).boxed().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testRangeMap() {
        stream = CharStream.of(new char[] { 'a', 'b', 'c', 'x', 'y', 'z', 'k' }); // 97,98,99,120,121,122,107
        CharBiPredicate sameRange = (c1, c2) -> Math.abs(c1 - c2) <= 1; // Consecutive or same
        CharBinaryOperator mapper = (c1, c2) -> c1; // Just take the first char of the range

        List<Character> result = stream.rangeMap(sameRange, mapper).boxed().toList();
        assertEquals(Arrays.asList('a', 'c', 'x', 'z', 'k'), result); // 'a','b','c' -> 'a' | 'x','y','z' -> 'x' | 'k' -> 'k'

        stream = CharStream.of(new char[] { 'a', 'a', 'a' });
        result = stream.rangeMap(sameRange, mapper).boxed().toList();
        assertEquals(Arrays.asList('a'), result);

        stream = CharStream.of(new char[] {});
        result = stream.rangeMap(sameRange, mapper).boxed().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testRangeMapToObj() {
        stream = CharStream.of(new char[] { 'a', 'b', 'c', 'x', 'y', 'z', 'k' });
        CharBiPredicate sameRange = (c1, c2) -> Math.abs(c1 - c2) <= 1;
        CharBiFunction<String> mapper = (c1, c2) -> c1 + "-" + c2;

        List<String> result = stream.rangeMapToObj(sameRange, mapper).toList();
        assertEquals(Arrays.asList("a-b", "c-c", "x-y", "z-z", "k-k"), result);

        stream = CharStream.of(new char[] {});
        List<String> emptyResult = stream.rangeMapToObj(sameRange, mapper).toList();
        assertTrue(emptyResult.isEmpty());
    }

    @Test
    public void testCollapseCharList() {
        stream = CharStream.of(new char[] { 'a', 'b', 'c', 'd', 'f', 'g', 'h' });
        CharBiPredicate collapsible = (c1, c2) -> c2 == (char) (c1 + 1);

        List<CharList> result = stream.collapse(collapsible).toList();
        assertEquals(2, result.size());
        assertEquals(CharList.of('a', 'b', 'c', 'd'), result.get(0));
        assertEquals(CharList.of('f', 'g', 'h'), result.get(1));
        // assertEquals(CharList.of(), result.get(2)); // 'e' is not present, so new group starts with 'f'
        // Oh, wait. There is 'e' so the list is not 'f','g','h' but 'f' and 'g','h'. The list has 3 elements instead of 2.
        // CharStream ['a', 'b', 'c', 'd', 'f', 'g', 'h'] should result in:
        // Group 1: 'a', 'b', 'c', 'd' -> CharList('a', 'b', 'c', 'd')
        // Group 2: 'f', 'g', 'h' -> CharList('f', 'g', 'h')
        // Let's re-evaluate based on the exact char sequence and predicate.
        // 'a', 'b' -> true
        // 'b', 'c' -> true
        // 'c', 'd' -> true
        // 'd', 'f' -> false (not consecutive)
        // 'f', 'g' -> true
        // 'g', 'h' -> true
        assertEquals(Arrays.asList(new CharList(new char[] { 'a', 'b', 'c', 'd' }), new CharList(new char[] { 'f', 'g', 'h' })), result);

        result = CharStream.of(new char[] {}).collapse(collapsible).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCollapseCharBinaryOperator() {
        stream = CharStream.of(new char[] { 'a', 'b', 'c', 'f', 'g', 'h' });
        CharBiPredicate collapsible = (c1, c2) -> c2 == (char) (c1 + 1);
        CharBinaryOperator mergeFunction = (c1, c2) -> Character.MAX_VALUE; // Sentinel char for merged

        List<Character> result = stream.collapse(collapsible, mergeFunction).boxed().toList();
        assertEquals(Arrays.asList(Character.MAX_VALUE, Character.MAX_VALUE), result); // Two groups: abc -> MAX_VALUE, fgh -> MAX_VALUE

        stream = CharStream.of(new char[] { 'a', 'e', 'f' });
        result = stream.collapse(collapsible, mergeFunction).boxed().toList();
        assertEquals(Arrays.asList('a', Character.MAX_VALUE), result);

        result = CharStream.of(new char[] {}).collapse(collapsible, mergeFunction).boxed().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCollapseCharTriPredicate() {
        stream = CharStream.of(new char[] { 'a', 'b', 'c', 'f', 'g', 'h' });
        CharTriPredicate collapsible = (firstInGroup, current, next) -> {
            // Example: collapse if all chars are within a certain range from the first char in group
            return (next - firstInGroup) < 3;
        };
        CharBinaryOperator mergeFunction = (c1, c2) -> (char) (c1 + c2); // Sum of chars

        List<Character> result = stream.collapse(collapsible, mergeFunction).boxed().toList();
        // 'a', 'b', 'c' -> first='a', current='b', next='c'. (c - a) = 2 < 3. merge(merge('a','b'), 'c') = merge('a'+'b', 'c')
        // 'a' + 'b' + 'c' = 97+98+99 = 294 -> (char)294
        // 'f', 'g', 'h' -> first='f', current='g', next='h'. (h - f) = 2 < 3. merge(merge('f','g'), 'h') = merge('f'+'g', 'h')
        // 'f' + 'g' + 'h' = 102+103+104 = 309 -> (char)309
        assertEquals(Arrays.asList((char) 294, (char) 309), result);

        result = CharStream.of(new char[] {}).collapse(collapsible, mergeFunction).boxed().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSkipWithAction() {
        CharList skipped = new CharList();
        CharConsumer action = skipped::add;

        CharStream newStream = stream.skip(2, action);
        List<Character> result = newStream.boxed().toList();

        assertEquals(Arrays.asList('c', 'd', 'e'), result);
        assertEquals(Arrays.asList('a', 'b'), skipped.boxed());

        skipped.clear();
        newStream = CharStream.of(new char[] { 'a' }).skip(5, action);
        result = newStream.boxed().toList();
        assertTrue(result.isEmpty());
        assertEquals(Arrays.asList('a'), skipped.boxed());

        skipped.clear();
        newStream = CharStream.of(new char[] { 'a', 'b' }).skip(0, action);
        result = newStream.boxed().toList();
        assertEquals(Arrays.asList('a', 'b'), result);
        assertTrue(skipped.isEmpty());
    }

    @Test
    public void testFilterWithActionOnDroppedItem() {
        CharList dropped = new CharList();
        CharConsumer actionOnDroppedItem = dropped::add;
        CharPredicate predicate = c -> c != 'b' && c != 'd';

        CharStream newStream = stream.filter(predicate, actionOnDroppedItem);
        List<Character> result = newStream.boxed().toList();

        assertEquals(Arrays.asList('a', 'c', 'e'), result);
        assertEquals(Arrays.asList('b', 'd'), dropped.boxed());

        dropped.clear();
        newStream = CharStream.of(new char[] {}).filter(predicate, actionOnDroppedItem);
        result = newStream.boxed().toList();
        assertTrue(result.isEmpty());
        assertTrue(dropped.isEmpty());
    }

    @Test
    public void testDropWhileWithActionOnDroppedItem() {
        CharList dropped = new CharList();
        CharConsumer actionOnDroppedItem = dropped::add;
        CharPredicate predicate = c -> c != 'c'; // Drop 'a', 'b'

        CharStream newStream = stream.dropWhile(predicate, actionOnDroppedItem);
        List<Character> result = newStream.boxed().toList();

        assertEquals(Arrays.asList('c', 'd', 'e'), result);
        assertEquals(Arrays.asList('a', 'b'), dropped.boxed());

        dropped.clear();
        newStream = CharStream.of(new char[] {}).dropWhile(predicate, actionOnDroppedItem);
        result = newStream.boxed().toList();
        assertTrue(result.isEmpty());
        assertTrue(dropped.isEmpty());

        dropped.clear();
        newStream = CharStream.of(new char[] { 'a', 'b', 'c' }).dropWhile(c -> c < 'z', actionOnDroppedItem);
        result = newStream.boxed().toList();
        assertTrue(result.isEmpty());
        assertEquals(Arrays.asList('a', 'b', 'c'), dropped.boxed());
    }

    @Test
    public void testStep() {
        List<Character> result = stream.step(2).boxed().toList();
        assertEquals(Arrays.asList('a', 'c', 'e'), result);

        result = stream2.step(1).boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c', 'd', 'e'), result);

        result = CharStream.of(new char[] { 'a', 'b', 'c', 'd', 'e', 'f' }).step(3).boxed().toList();
        assertEquals(Arrays.asList('a', 'd'), result);

        result = CharStream.of(new char[] {}).step(2).boxed().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testScanAccumulator() {
        CharBinaryOperator accumulator = (c1, c2) -> (char) (c1 + c2);
        List<Character> result = CharStream.of(new char[] { 'a', 'b', 'c' }).scan(accumulator).boxed().toList();
        // 'a' (97)
        // 'a' + 'b' = 97 + 98 = 195
        // 'a' + 'b' + 'c' = 195 + 99 = 294
        assertEquals(Arrays.asList('a', (char) 195, (char) 294), result);

        result = CharStream.of(new char[] {}).scan(accumulator).boxed().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testScanInitAccumulator() {
        CharBinaryOperator accumulator = (c1, c2) -> (char) (c1 + c2);
        List<Character> result = CharStream.of(new char[] { 'b', 'c' }).scan('a', accumulator).boxed().toList();
        // 'a' + 'b' = 195
        // 'a' + 'b' + 'c' = 195 + 99 = 294
        assertEquals(Arrays.asList((char) 195, (char) 294), result);

        result = CharStream.of(new char[] {}).scan('z', accumulator).boxed().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testScanInitInitIncludedAccumulator() {
        CharBinaryOperator accumulator = (c1, c2) -> (char) (c1 + c2);
        List<Character> result = CharStream.of(new char[] { 'b', 'c' }).scan('a', true, accumulator).boxed().toList();
        // 'a'
        // 'a' + 'b' = 195
        // 'a' + 'b' + 'c' = 195 + 99 = 294
        assertEquals(Arrays.asList('a', (char) 195, (char) 294), result);

        result = CharStream.of(new char[] {}).scan('z', true, accumulator).boxed().toList();
        assertEquals(Arrays.asList('z'), result);

        result = CharStream.of(new char[] {}).scan('z', false, accumulator).boxed().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testIntersection() {
        Collection<Character> other = Arrays.asList('c', 'e', 'f');
        List<Character> result = stream.intersection(other).boxed().toList();
        assertEquals(Arrays.asList('c', 'e'), result);

        result = CharStream.of(new char[] {}).intersection(other).boxed().toList();
        assertTrue(result.isEmpty());

        result = stream2.intersection(Arrays.asList()).boxed().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDifference() {
        Collection<Character> other = Arrays.asList('c', 'e', 'f');
        List<Character> result = stream.difference(other).boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'd'), result);

        result = CharStream.of(new char[] {}).difference(other).boxed().toList();
        assertTrue(result.isEmpty());

        result = stream2.difference(Arrays.asList()).boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c', 'd', 'e'), result);
    }

    @Test
    public void testSymmetricDifference() {
        Collection<Character> other = Arrays.asList('c', 'e', 'f', 'g'); // Stream: a,b,c,d,e. Other: c,e,f,g
        // Elements unique to stream: a, b, d
        // Elements unique to other: f, g
        // Symmetric difference: a, b, d, f, g
        List<Character> result = stream.symmetricDifference(other).boxed().toList();
        // The order is (elements from stream not in other) then (elements from other not in stream)
        assertEquals(Arrays.asList('a', 'b', 'd', 'f', 'g'), result);

        result = CharStream.of(new char[] {}).symmetricDifference(other).boxed().toList();
        assertEquals(Arrays.asList('c', 'e', 'f', 'g'), result);

        result = stream2.symmetricDifference(Arrays.asList()).boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c', 'd', 'e'), result);
    }

    @Test
    public void testReversed() {
        List<Character> result = stream.reversed().boxed().toList();
        assertEquals(Arrays.asList('e', 'd', 'c', 'b', 'a'), result);

        result = CharStream.of(new char[] {}).reversed().boxed().toList();
        assertTrue(result.isEmpty());

        result = CharStream.of(new char[] { 'x' }).reversed().boxed().toList();
        assertEquals(Arrays.asList('x'), result);
    }

    @Test
    public void testRotated() {
        List<Character> result = stream.rotated(1).boxed().toList();
        assertEquals(Arrays.asList('e', 'a', 'b', 'c', 'd'), result);

        result = stream2.rotated(2).boxed().toList();
        assertEquals(Arrays.asList('d', 'e', 'a', 'b', 'c'), result);

        result = stream3.rotated(-1).boxed().toList();
        assertEquals(Arrays.asList('b', 'c', 'd', 'e', 'a'), result);

        result = CharStream.of(new char[] { 'x' }).rotated(10).boxed().toList();
        assertEquals(Arrays.asList('x'), result);

        result = CharStream.of(new char[] {}).rotated(1).boxed().toList();
        assertTrue(result.isEmpty());

    }

    @Test
    public void testShuffled() {
        Random rnd = new Random(123); // Fixed seed for reproducible tests
        List<Character> original = stream.boxed().toList();
        List<Character> shuffled = stream2.shuffled(rnd).boxed().toList();

        // Check if elements are the same, just order is different
        assertEquals(original.size(), shuffled.size());
        assertTrue(original.containsAll(shuffled) && shuffled.containsAll(original));

        // Unlikely to be in the same order unless stream is very small
        assertNotEquals(original, shuffled);

        shuffled = CharStream.of(new char[] {}).shuffled(rnd).boxed().toList();
        assertTrue(shuffled.isEmpty());
    }

    @Test
    public void testSorted() {
        stream = CharStream.of(new char[] { 'e', 'a', 'd', 'b', 'c' });
        List<Character> result = stream.sorted().boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c', 'd', 'e'), result);

        result = CharStream.of(new char[] {}).sorted().boxed().toList();
        assertTrue(result.isEmpty());

    }

    @Test
    public void testReverseSorted() {
        stream = CharStream.of(new char[] { 'e', 'a', 'd', 'b', 'c' });
        List<Character> result = stream.reverseSorted().boxed().toList();
        assertEquals(Arrays.asList('e', 'd', 'c', 'b', 'a'), result);

        result = CharStream.of(new char[] {}).reverseSorted().boxed().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCycled() {
        List<Character> result = stream.cycled().limit(12).boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c', 'd', 'e', 'a', 'b', 'c', 'd', 'e', 'a', 'b'), result);

        result = CharStream.of(new char[] {}).cycled().limit(5).boxed().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCycledRounds() {
        List<Character> result = stream.cycled(2).boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c', 'd', 'e', 'a', 'b', 'c', 'd', 'e'), result);

        result = stream2.cycled(0).boxed().toList();
        assertTrue(result.isEmpty());

        result = CharStream.of(new char[] {}).cycled(2).boxed().toList();
        assertTrue(result.isEmpty());

        result = CharStream.of(new char[] { 'x' }).cycled(3).boxed().toList();
        assertEquals(Arrays.asList('x', 'x', 'x'), result);
    }

    @Test
    public void testIndexed() {
        List<IndexedChar> result = stream.indexed().toList();
        assertEquals(5, result.size());
        assertEquals(IndexedChar.of('a', 0), result.get(0));
        assertEquals(IndexedChar.of('b', 1), result.get(1));
        assertEquals(IndexedChar.of('e', 4), result.get(4));

        List<IndexedChar> emptyResult = CharStream.of(new char[] {}).indexed().toList();
        assertTrue(emptyResult.isEmpty());
    }

    @Test
    public void testBoxed() {
        List<Character> result = stream.boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c', 'd', 'e'), result);

        List<Character> emptyResult = CharStream.of(new char[] {}).boxed().toList();
        assertTrue(emptyResult.isEmpty());
    }

    @Test
    public void testPrependCharArray() {
        CharStream newStream = stream.prepend('x', 'y');
        List<Character> result = newStream.boxed().toList();
        assertEquals(Arrays.asList('x', 'y', 'a', 'b', 'c', 'd', 'e'), result);

        newStream = CharStream.of(new char[] {}).prepend('x');
        result = newStream.boxed().toList();
        assertEquals(Arrays.asList('x'), result);
    }

    @Test
    public void testPrependCharStream() {
        CharStream newStream = stream.prepend(CharStream.of(new char[] { 'x', 'y' }));
        List<Character> result = newStream.boxed().toList();
        assertEquals(Arrays.asList('x', 'y', 'a', 'b', 'c', 'd', 'e'), result);

        newStream = CharStream.of(new char[] {}).prepend(CharStream.of(new char[] { 'x', 'y' }));
        result = newStream.boxed().toList();
        assertEquals(Arrays.asList('x', 'y'), result);

        newStream = stream.prepend(CharStream.empty());
        result = newStream.boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c', 'd', 'e'), result);
    }

    @Test
    public void testPrependOptionalChar() {
        CharStream newStream = stream.prepend(OptionalChar.of('x'));
        List<Character> result = newStream.boxed().toList();
        assertEquals(Arrays.asList('x', 'a', 'b', 'c', 'd', 'e'), result);

        newStream = stream.prepend(OptionalChar.empty());
        result = newStream.boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c', 'd', 'e'), result);
    }

    @Test
    public void testAppendCharArray() {
        CharStream newStream = stream.append('x', 'y');
        List<Character> result = newStream.boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c', 'd', 'e', 'x', 'y'), result);

        newStream = CharStream.of(new char[] {}).append('x');
        result = newStream.boxed().toList();
        assertEquals(Arrays.asList('x'), result);
    }

    @Test
    public void testAppendCharStream() {
        CharStream newStream = stream.append(CharStream.of(new char[] { 'x', 'y' }));
        List<Character> result = newStream.boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c', 'd', 'e', 'x', 'y'), result);

        newStream = CharStream.of(new char[] {}).append(CharStream.of(new char[] { 'x', 'y' }));
        result = newStream.boxed().toList();
        assertEquals(Arrays.asList('x', 'y'), result);

        newStream = stream2.append(CharStream.empty());
        result = newStream.boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c', 'd', 'e'), result);
    }

    @Test
    public void testAppendOptionalChar() {
        CharStream newStream = stream.append(OptionalChar.of('x'));
        List<Character> result = newStream.boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c', 'd', 'e', 'x'), result);

        newStream = stream2.append(OptionalChar.empty());
        result = newStream.boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c', 'd', 'e'), result);
    }

    @Test
    public void testAppendIfEmptyCharArray() {
        CharStream newStream = stream.appendIfEmpty('x', 'y');
        List<Character> result = newStream.boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c', 'd', 'e'), result); // Stream is not empty, so no append

        newStream = CharStream.of(new char[] {}).appendIfEmpty('x', 'y');
        result = newStream.boxed().toList();
        assertEquals(Arrays.asList('x', 'y'), result); // Stream is empty, so append
    }

    @Test
    public void testMergeWith() {
        CharStream streamA = CharStream.of(new char[] { 'a', 'c', 'e' });
        CharStream streamB = CharStream.of(new char[] { 'b', 'd', 'f' });
        CharBiFunction<MergeResult> nextSelector = (c1, c2) -> c1 < c2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        List<Character> result = streamA.mergeWith(streamB, nextSelector).boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c', 'd', 'e', 'f'), result);

        streamA = CharStream.of(new char[] { 'a', 'b' });
        streamB = CharStream.of(new char[] { 'x', 'y', 'z' });
        result = streamA.mergeWith(streamB, nextSelector).boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'x', 'y', 'z'), result);
    }

    @Test
    public void testZipWithBinaryOperator() {
        CharStream streamA = CharStream.of(new char[] { '1', '2', '3' }); // ASCII 49, 50, 51
        CharStream streamB = CharStream.of(new char[] { 'a', 'b', 'c' }); // ASCII 97, 98, 99
        CharBinaryOperator zipFunction = (c1, c2) -> (char) (c1 + c2);

        List<Character> result = streamA.zipWith(streamB, zipFunction).boxed().toList();
        assertEquals(Arrays.asList((char) (49 + 97), (char) (50 + 98), (char) (51 + 99)), result); // Sum of ASCII values

        streamA = CharStream.of(new char[] { '1', '2' });
        streamB = CharStream.of(new char[] { 'a', 'b', 'c' });
        result = streamA.zipWith(streamB, zipFunction).boxed().toList();
        assertEquals(Arrays.asList((char) (49 + 97), (char) (50 + 98)), result); // Shorter stream dictates length
    }

    @Test
    public void testZipWithTernaryOperator() {
        CharStream streamA = CharStream.of(new char[] { '1', '2' });
        CharStream streamB = CharStream.of(new char[] { 'a', 'b' });
        CharStream streamC = CharStream.of(new char[] { 'X', 'Y' });
        CharTernaryOperator zipFunction = (c1, c2, c3) -> (char) (c1 + c2 + c3);

        List<Character> result = streamA.zipWith(streamB, streamC, zipFunction).boxed().toList();
        assertEquals(Arrays.asList((char) (49 + 97 + 88), (char) (50 + 98 + 89)), result);

        streamA = CharStream.of(new char[] { '1' });
        streamB = CharStream.of(new char[] { 'a', 'b' });
        streamC = CharStream.of(new char[] { 'X', 'Y' });
        result = streamA.zipWith(streamB, streamC, zipFunction).boxed().toList();
        assertEquals(Arrays.asList((char) (49 + 97 + 88)), result); // Shortest stream dictates length
    }

    @Test
    public void testZipWithBinaryOperatorWithNoneValues() {
        CharStream streamA = CharStream.of(new char[] { '1', '2' });
        CharStream streamB = CharStream.of(new char[] { 'a', 'b', 'c' });
        char valueForNoneA = '-';
        char valueForNoneB = '+';
        CharBinaryOperator zipFunction = (c1, c2) -> {
            if (c1 == valueForNoneA)
                return c2;
            if (c2 == valueForNoneB)
                return c1;
            return (char) (c1 + c2);
        };

        List<Character> result = streamA.zipWith(streamB, valueForNoneA, valueForNoneB, zipFunction).boxed().toList();
        assertEquals(Arrays.asList((char) (49 + 97), (char) (50 + 98), 'c'), result);

        streamA = CharStream.of(new char[] { '1', '2', '3' });
        streamB = CharStream.of(new char[] { 'a', 'b' });
        result = streamA.zipWith(streamB, valueForNoneA, valueForNoneB, zipFunction).boxed().toList();
        assertEquals(Arrays.asList((char) (49 + 97), (char) (50 + 98), '3'), result);

        streamA = CharStream.of(new char[] { '1' });
        streamB = CharStream.of(new char[] { 'a' });
        result = streamA.zipWith(streamB, valueForNoneA, valueForNoneB, zipFunction).boxed().toList();
        assertEquals(Arrays.asList((char) (49 + 97)), result);

        streamA = CharStream.of(new char[] {});
        streamB = CharStream.of(new char[] {});
        result = streamA.zipWith(streamB, valueForNoneA, valueForNoneB, zipFunction).boxed().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testZipWithTernaryOperatorWithNoneValues() {
        CharStream streamA = CharStream.of(new char[] { '1' });
        CharStream streamB = CharStream.of(new char[] { 'a', 'b' });
        CharStream streamC = CharStream.of(new char[] { 'X', 'Y', 'Z' });
        char vNA = 'A', vNB = 'B', vNC = 'C';
        CharTernaryOperator zipFunction = (c1, c2, c3) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(c1 == vNA ? "" : c1);
            sb.append(c2 == vNB ? "" : c2);
            sb.append(c3 == vNC ? "" : c3);
            return sb.toString().charAt(0); // Take first char if concatenated
        };

        List<Character> result = streamA.zipWith(streamB, streamC, vNA, vNB, vNC, zipFunction).boxed().toList();
        // (1, a, X) -> '1aX' -> '1'
        // (A, b, Y) -> 'bY'  -> 'b' (A is default for streamA)
        // (A, B, Z) -> 'Z'   -> 'Z' (A, B defaults for streamA, streamB)
        assertEquals(Arrays.asList('1', 'b', 'Z'), result);
    }

    @Test
    public void testToMapKeyMapperValueMapper() throws Exception {
        stream = CharStream.of(new char[] { 'a', 'b', 'c' });
        Map<String, Integer> map = stream.toMap(c -> String.valueOf(c), c -> (int) c);
        assertEquals(3, map.size());
        assertEquals(Integer.valueOf(97), map.get("a"));
        assertEquals(Integer.valueOf(98), map.get("b"));
        assertEquals(Integer.valueOf(99), map.get("c"));

        map = CharStream.of(new char[] {}).toMap(c -> String.valueOf(c), c -> (int) c);
        assertTrue(map.isEmpty());

        try {
            CharStream.of(new char[] { 'a', 'a' }).toMap(c -> String.valueOf(c), c -> (int) c);
            fail("Expected IllegalStateException for duplicate keys");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("Duplicate key"));
        }
    }

    @Test
    public void testToMapKeyMapperValueMapperMapFactory() throws Exception {
        stream = CharStream.of(new char[] { 'a', 'b', 'c' });
        Map<String, Integer> map = stream.toMap(c -> String.valueOf(c), c -> (int) c, () -> new java.util.LinkedHashMap<>());
        assertEquals(3, map.size());
        assertEquals(Integer.valueOf(97), map.get("a"));
        assertEquals(Integer.valueOf(98), map.get("b"));
        assertEquals(Integer.valueOf(99), map.get("c"));
        assertTrue(map instanceof java.util.LinkedHashMap);
    }

    @Test
    public void testToMapKeyMapperValueMapperMergeFunction() throws Exception {
        stream = CharStream.of(new char[] { 'a', 'b', 'a' });
        BinaryOperator<Integer> mergeFunction = (v1, v2) -> v1 + v2;
        Map<String, Integer> map = stream.toMap(c -> String.valueOf(c), c -> (int) c, mergeFunction);
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(97 + 97), map.get("a")); // 194
        assertEquals(Integer.valueOf(98), map.get("b"));
    }

    @Test
    public void testToMapKeyMapperValueMapperMergeFunctionMapFactory() throws Exception {
        stream = CharStream.of(new char[] { 'a', 'b', 'a' });
        BinaryOperator<Integer> mergeFunction = (v1, v2) -> v1 + v2;
        Map<String, Integer> map = stream.toMap(c -> String.valueOf(c), c -> (int) c, mergeFunction, () -> new java.util.TreeMap<>());
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(97 + 97), map.get("a"));
        assertEquals(Integer.valueOf(98), map.get("b"));
        assertTrue(map instanceof java.util.TreeMap);
    }

    @Test
    public void testGroupToCollector() throws Exception {
        stream = CharStream.of(new char[] { 'a', 'b', 'a', 'c' });
        Map<String, List<Character>> map = stream.groupTo(c -> String.valueOf(c), Collectors.toList());
        assertEquals(3, map.size());
        assertEquals(Arrays.asList('a', 'a'), map.get("a"));
        assertEquals(Arrays.asList('b'), map.get("b"));
        assertEquals(Arrays.asList('c'), map.get("c"));

        map = CharStream.of(new char[] {}).groupTo(c -> String.valueOf(c), Collectors.toList());
        assertTrue(map.isEmpty());
    }

    @Test
    public void testGroupToCollectorMapFactory() throws Exception {
        stream = CharStream.of(new char[] { 'a', 'b', 'a', 'c' });
        Map<String, Set<Character>> map = stream.groupTo(c -> String.valueOf(c), Collectors.toSet(), () -> new LinkedHashMap<>());
        assertEquals(3, map.size());
        assertEquals(N.asSet('a'), map.get("a"));
        assertEquals(N.asSet('b'), map.get("b"));
        assertEquals(N.asSet('c'), map.get("c"));
        assertTrue(map instanceof java.util.LinkedHashMap);
    }

    @Test
    public void testForEachIndexed() throws Exception {
        CharList processedChars = new CharList();
        List<Integer> processedIndices = new java.util.ArrayList<>();
        stream.forEachIndexed((idx, c) -> {
            processedIndices.add(idx);
            processedChars.add(c);
        });

        assertEquals(Arrays.asList(0, 1, 2, 3, 4), processedIndices);
        assertEquals(Arrays.asList('a', 'b', 'c', 'd', 'e'), processedChars.boxed());
    }

    @Test
    public void testFirst() {
        OptionalChar first = stream.first();
        assertTrue(first.isPresent());
        assertEquals('a', first.get());

        first = CharStream.of(new char[] {}).first();
        assertFalse(first.isPresent());
    }

    @Test
    public void testLast() {
        OptionalChar last = stream.last();
        assertTrue(last.isPresent());
        assertEquals('e', last.get());

        last = CharStream.of(new char[] {}).last();
        assertFalse(last.isPresent());

        last = CharStream.of(new char[] { 'x' }).last();
        assertTrue(last.isPresent());
        assertEquals('x', last.get());
    }

    @Test
    public void testOnlyOne() throws TooManyElementsException {
        OptionalChar onlyOne = CharStream.of(new char[] { 'x' }).onlyOne();
        assertTrue(onlyOne.isPresent());
        assertEquals('x', onlyOne.get());

        onlyOne = CharStream.of(new char[] {}).onlyOne();
        assertFalse(onlyOne.isPresent());

        try {
            stream.onlyOne(); // 'a', 'b', 'c', 'd', 'e'
            fail("Expected TooManyElementsException");
        } catch (TooManyElementsException e) {
            assertTrue(e.getMessage().contains("at least two elements"));
        }
    }

    @Test
    public void testFindAny() throws Exception {
        OptionalChar found = stream.findAny(c -> c == 'c');
        assertTrue(found.isPresent());
        assertEquals('c', found.get());

        found = stream2.findAny(c -> c == 'z');
        assertFalse(found.isPresent());

        found = CharStream.of(new char[] {}).findAny(c -> true);
        assertFalse(found.isPresent());
    }

    @Test
    public void testPercentiles() {
        stream = CharStream.of(new char[] { 'a', 'e', 'c', 'd', 'b' }); // Sorted: a,b,c,d,e
        Optional<Map<Percentage, Character>> percentiles = stream.percentiles();
        assertTrue(percentiles.isPresent());
        Map<Percentage, Character> percentileMap = percentiles.get();

        // assertEquals(N.asList(97, 98, 99, 100, 101), percentileMap.values().stream().sorted().map(c -> (int) c).collect(Collectors.toList()));
        assertEquals(43, percentileMap.values().size());

        // Test with empty stream
        Optional<Map<Percentage, Character>> emptyPercentiles = CharStream.of(new char[] {}).percentiles();
        assertFalse(emptyPercentiles.isPresent());
    }

    @Test
    public void testSummarizeAndPercentiles() {
        stream = CharStream.of(new char[] { 'a', 'e', 'c', 'd', 'b' }); // Sorted: a,b,c,d,e
        Pair<CharSummaryStatistics, Optional<Map<Percentage, Character>>> summaryPair = stream.summarizeAndPercentiles();

        CharSummaryStatistics stats = summaryPair.left();
        assertEquals(5, stats.getCount());
        assertEquals('a', stats.getMin());
        assertEquals('e', stats.getMax());
        assertEquals(97 + 98 + 99 + 100 + 101, stats.getSum().intValue());
        assertEquals((97 + 98 + 99 + 100 + 101) / 5.0, stats.getAverage(), 0.001);

        Optional<Map<Percentage, Character>> percentiles = summaryPair.right();
        assertTrue(percentiles.isPresent());
        Map<Percentage, Character> percentileMap = percentiles.get();
        // assertEquals(N.asList(97, 98, 99, 100, 101), percentileMap.values().stream().sorted().map(c -> (int) c).collect(Collectors.toList()));
        assertEquals(43, percentileMap.size());

        // Test with empty stream
        summaryPair = CharStream.of(new char[] {}).summarizeAndPercentiles();
        stats = summaryPair.left();
        assertEquals(0, stats.getCount());
        assertFalse(summaryPair.right().isPresent());
    }

    @Test
    public void testJoinCharSequenceCharSequenceCharSequence() {
        String result = stream.join(",", "[", "]");
        assertEquals("[a,b,c,d,e]", result);

        result = stream2.join("", "(", ")");
        assertEquals("(abcde)", result);

        result = CharStream.of(new char[] {}).join(",", "[", "]");
        assertEquals("[]", result);

        result = CharStream.of(new char[] { 'x' }).join(",", "[", "]");
        assertEquals("[x]", result);
    }

    @Test
    public void testJoinToJoiner() {
        Joiner joiner = Joiner.with("-", "<<", ">>");
        Joiner resultJoiner = stream.joinTo(joiner);
        assertEquals("<<a-b-c-d-e>>", resultJoiner.toString());

        joiner = Joiner.with("");
        resultJoiner = CharStream.of(new char[] {}).joinTo(joiner);
        assertEquals("", resultJoiner.toString());

        joiner = Joiner.with("", "start", "end");
        resultJoiner = CharStream.of(new char[] { 'x' }).joinTo(joiner);
        assertEquals("startxend", resultJoiner.toString());
    }

    @Test
    public void testCollectSupplierObjCharConsumer() {
        CharList charList = stream.collect(CharList::new, (c, e) -> c.add(e));
        assertEquals(Arrays.asList('a', 'b', 'c', 'd', 'e'), charList.boxed());

        charList = CharStream.of(new char[] {}).collect(CharList::new, CharList::add);
        assertTrue(charList.isEmpty());
    }

    @Test
    public void testIterator() {
        CharIterator iterator = stream.iterator();
        assertTrue(iterator.hasNext());
        assertEquals('a', iterator.nextChar());
        assertEquals('b', iterator.nextChar());
        assertTrue(iterator.hasNext());
        // No direct way to assert on logger warning from here without more complex mocking.
    }
}
