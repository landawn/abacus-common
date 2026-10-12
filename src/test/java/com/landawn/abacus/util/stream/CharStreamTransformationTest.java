package com.landawn.abacus.util.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.IndexedChar;
import com.landawn.abacus.util.function.CharBiPredicate;
import com.landawn.abacus.util.function.CharBinaryOperator;
import com.landawn.abacus.util.function.CharConsumer;
import com.landawn.abacus.util.function.CharPredicate;
import com.landawn.abacus.util.function.CharTriPredicate;
import com.landawn.abacus.util.u.OptionalChar;

/** CharStream transformation contracts. */
public class CharStreamTransformationTest extends CharStreamTestSupport {

    // collapse

    @Test
    public void testCollapse() {
        Stream<CharList> result = createCharStream('a', 'a', 'b', 'b', 'c').collapse((c1, c2) -> c1 == c2);
        List<CharList> lists = result.toList();
        assertThat(lists).hasSize(3);
        assertThat(lists.get(0).toArray()).containsExactly('a', 'a');
        assertThat(lists.get(1).toArray()).containsExactly('b', 'b');
        assertThat(lists.get(2).toArray()).containsExactly('c');
    }

    @Test
    public void testCollapse_BiPredicate() {
        CharStream stream = createCharStream('a', 'a', 'b', 'b', 'c');
        CharBiPredicate collapsible = (a, b) -> a == b;
        List<CharList> result = stream.collapse(collapsible).toList();
        assertEquals(3, result.size());
        assertEquals(CharList.of('a', 'a'), result.get(0));
        assertEquals(CharList.of('b', 'b'), result.get(1));
        assertEquals(CharList.of('c'), result.get(2));
    }

    @Test
    public void testCollapse_WithMergeFunction() {
        CharStream stream = createCharStream('a', 'a', 'b', 'b', 'c');
        CharBiPredicate collapsible = (a, b) -> a == b;
        CharBinaryOperator mergeFunction = (a, b) -> b;
        char[] result = stream.collapse(collapsible, mergeFunction).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    public void testCollapse_WithTriPredicate() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd', 'e');
        CharTriPredicate collapsible = (first, prev, curr) -> Math.abs(curr - prev) == 1;
        CharBinaryOperator mergeFunction = (a, b) -> b;
        char[] result = stream.collapse(collapsible, mergeFunction).toArray();
        assertEquals(1, result.length);
        assertEquals('e', result[0]);
    }

    @Test
    public void testCollapseWithNoCollapsibleElementsForCoverage() {
        CharStream stream = CharStream.of('a', 'p', 'z', 'k');
        char[] result = stream.collapse((a, b) -> Math.abs(b - a) <= 2, (a, b) -> b).toArray();
        assertTrue(result.length > 0);
    }

    // cycled

    @Test
    public void testCycled() {
        CharStream stream = createCharStream('a', 'b').cycled().limit(5);
        assertThat(stream.toArray()).containsExactly('a', 'b', 'a', 'b', 'a');
    }

    @Test
    public void testCycled_Infinite() {
        CharStream stream = createCharStream('a', 'b', 'c');
        char[] result = stream.cycled().limit(10).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'a', 'b', 'c', 'a', 'b', 'c', 'a' }, result);
    }

    @Test
    public void testCycled_NegativeRounds() {
        assertThrows(IllegalArgumentException.class, () -> {
            createCharStream('a').cycled(-1);
        });
    }

    @Test
    public void testCycled_WithRounds() {
        CharStream stream = createCharStream('a', 'b', 'c');
        char[] result = stream.cycled(2).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'a', 'b', 'c' }, result);
    }

    @Test
    public void testCycled_ZeroRounds() {
        CharStream stream = createCharStream('a', 'b', 'c');
        char[] result = stream.cycled(0).toArray();
        assertArrayEquals(new char[] {}, result);
    }

    @Test
    public void testCycledWithRounds() {
        CharStream stream = createCharStream('a', 'b').cycled(2);
        assertThat(stream.toArray()).containsExactly('a', 'b', 'a', 'b');
    }

    @Test
    public void testDifference() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd');
        List<Character> c = Arrays.asList('b', 'c', 'e');
        char[] result = stream.difference(c).toArray();
        assertArrayEquals(new char[] { 'a', 'd' }, result);
    }

    @Test
    public void testDistinct() {
        CharStream stream = createCharStream('a', 'b', 'a', 'c', 'b', 'd');
        char[] result = stream.distinct().toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, result);
    }

    @Test
    public void testDistinct_AlreadyDistinct() {
        CharStream stream = createCharStream('a', 'b', 'c');
        char[] result = stream.distinct().toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    // dropWhile

    @Test
    public void testDropWhile() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd').dropWhile(c -> c < 'c');
        assertThat(stream.toArray()).containsExactly('c', 'd');
    }

    @Test
    public void testDropWhile_AllMatch() {
        CharStream stream = createCharStream('a', 'b', 'c');
        char[] result = stream.dropWhile(c -> c < 'z').toArray();
        assertArrayEquals(new char[] {}, result);
    }

    @Test
    public void testDropWhile_BasicPredicate() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd', 'e');
        char[] result = stream.dropWhile(c -> c < 'c').toArray();
        assertArrayEquals(new char[] { 'c', 'd', 'e' }, result);
    }

    @Test
    public void testDropWhile_WithAction() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd');
        List<Character> dropped = new ArrayList<>();
        CharConsumer action = dropped::add;
        char[] result = stream.dropWhile(c -> c < 'c', action).toArray();
        assertArrayEquals(new char[] { 'c', 'd' }, result);
        assertEquals(Arrays.asList('a', 'b'), dropped);
    }

    // filter

    @Test
    public void testFilter() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd').filter(c -> c % 2 == 0);
        assertThat(stream.toArray()).containsExactly('b', 'd');
    }

    @Test
    public void testFilter_BasicPredicate() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd', 'e');
        char[] result = stream.filter(c -> c > 'b').toArray();
        assertArrayEquals(new char[] { 'c', 'd', 'e' }, result);
    }

    @Test
    public void testFilter_NoMatch() {
        CharStream stream = createCharStream('a', 'b', 'c');
        char[] result = stream.filter(c -> c > 'z').toArray();
        assertArrayEquals(new char[] {}, result);
    }

    @Test
    public void testFilter_WithAction() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd');
        List<Character> dropped = new ArrayList<>();
        CharConsumer action = dropped::add;
        char[] result = stream.filter(c -> c > 'b', action).toArray();
        assertArrayEquals(new char[] { 'c', 'd' }, result);
        assertEquals(Arrays.asList('a', 'b'), dropped);
    }

    @Test
    public void testIndexed() {
        CharStream stream = createCharStream('a', 'b', 'c');
        List<IndexedChar> result = stream.indexed().toList();
        assertEquals(3, result.size());
        assertEquals(IndexedChar.of('a', 0), result.get(0));
        assertEquals(IndexedChar.of('b', 1), result.get(1));
        assertEquals(IndexedChar.of('c', 2), result.get(2));
    }

    @Test
    public void testIntersection() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd');
        List<Character> c = Arrays.asList('b', 'c', 'e');
        char[] result = stream.intersection(c).toArray();
        assertArrayEquals(new char[] { 'b', 'c' }, result);
    }

    @Test
    public void testIntersection_NoCommonElements() {
        CharStream stream = createCharStream('a', 'b', 'c');
        List<Character> c = Arrays.asList('x', 'y', 'z');
        char[] result = stream.intersection(c).toArray();
        assertArrayEquals(new char[] {}, result);
    }

    @Test
    public void testLimit() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd', 'e');
        char[] result = stream.limit(3).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    public void testLimit_ZeroElements() {
        CharStream stream = createCharStream('a', 'b', 'c');
        char[] result = stream.limit(0).toArray();
        assertArrayEquals(new char[] {}, result);
    }

    // onlyOne

    @Test
    public void testOnlyOne_EmptyStream() {
        CharStream stream = createCharStream();
        OptionalChar result = stream.onlyOne();
        assertFalse(result.isPresent());
    }

    @Test
    public void testOnlyOne_MultipleElements() {
        CharStream stream = createCharStream('a', 'b');
        assertThrows(TooManyElementsException.class, () -> stream.onlyOne());
    }

    @Test
    public void testOnlyOne_SingleElement() {
        CharStream stream = createCharStream('a');
        OptionalChar result = stream.onlyOne();
        assertTrue(result.isPresent());
        assertEquals('a', result.get());
    }

    @Test
    public void testPeek() {
        CharStream stream = createCharStream('a', 'b', 'c');
        List<Character> peeked = new ArrayList<>();
        char[] result = stream.peek(peeked::add).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
        assertEquals(Arrays.asList('a', 'b', 'c'), peeked);
    }

    @Test
    public void testPeekIsAliasForOnEach() {
        List<Character> peeked = new ArrayList<>();
        char[] result = createCharStream('a', 'b', 'c').peek(c -> peeked.add(c)).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
        assertEquals(3, peeked.size());
    }

    @Test
    public void testReversed() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd', 'e');
        char[] result = stream.reversed().toArray();
        assertArrayEquals(new char[] { 'e', 'd', 'c', 'b', 'a' }, result);
    }

    @Test
    public void testReverseSorted() {
        CharStream stream = createCharStream('b', 'e', 'a', 'd', 'c');
        char[] result = stream.reverseSorted().toArray();
        assertArrayEquals(new char[] { 'e', 'd', 'c', 'b', 'a' }, result);
    }

    // rotated

    @Test
    public void testRotated() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd').rotated(2);
        assertThat(stream.toArray()).containsExactly('c', 'd', 'a', 'b');
    }

    @Test
    public void testRotated_Negative() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd', 'e');
        char[] result = stream.rotated(-2).toArray();
        assertArrayEquals(new char[] { 'c', 'd', 'e', 'a', 'b' }, result);
    }

    @Test
    public void testRotated_Positive() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd', 'e');
        char[] result = stream.rotated(2).toArray();
        assertArrayEquals(new char[] { 'd', 'e', 'a', 'b', 'c' }, result);
    }

    @Test
    public void testRotated_Zero() {
        CharStream stream = createCharStream('a', 'b', 'c');
        char[] result = stream.rotated(0).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    // scan

    @Test
    public void testScan() {
        CharStream stream = createCharStream('a', 'b', 'c');
        CharBinaryOperator accumulator = (a, b) -> (char) Math.max(a, b);
        char[] result = stream.scan(accumulator).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    public void testScan_WithInit() {
        CharStream stream = createCharStream('a', 'b', 'c');
        char init = 'z';
        CharBinaryOperator accumulator = (a, b) -> (char) Math.max(a, b);
        char[] result = stream.scan(init, accumulator).toArray();
        assertArrayEquals(new char[] { 'z', 'z', 'z' }, result);
    }

    @Test
    public void testScan_WithInitIncluded() {
        CharStream stream = createCharStream('a', 'b', 'c');
        char init = 'z';
        CharBinaryOperator accumulator = (a, b) -> (char) Math.max(a, b);

        char[] result = stream.scan(init, true, accumulator).toArray();
        assertArrayEquals(new char[] { 'z', 'z', 'z', 'z' }, result);

        stream = createCharStream('a', 'b', 'c');
        result = stream.scan(init, false, accumulator).toArray();
        assertArrayEquals(new char[] { 'z', 'z', 'z' }, result);
    }

    @Test
    public void testScanOnEmptyStreamForCoverage() {
        CharStream emptyStream = CharStream.empty();
        char[] result = emptyStream.scan((a, b) -> (char) Math.max(a, b)).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testScanWithSeed() {
        CharStream stream = createCharStream('a', 'b', 'c').scan('x', (c1, c2) -> c2);
        assertThat(stream.toArray()).containsExactly('a', 'b', 'c');
    }

    // shuffled

    @Test
    public void testShuffled() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd', 'e');
        Random rnd = new Random(42);
        char[] result = stream.shuffled(rnd).toArray();
        assertNotNull(result);
        assertEquals(5, result.length);
    }

    @Test
    public void testShuffled_NoArg() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd', 'e');
        char[] result = stream.shuffled().toArray();
        assertNotNull(result);
        assertEquals(5, result.length);
    }

    @Test
    public void testShuffled_NullRandom() {
        assertThrows(IllegalArgumentException.class, () -> {
            createCharStream('a').shuffled(null);
        });
    }

    @Test
    public void testShuffled_WithRandom() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd', 'e');
        char[] result = stream.shuffled(new Random(42)).toArray();
        assertEquals(5, result.length);
        // Verify all elements are present
        Arrays.sort(result);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, result);
    }

    // skip

    @Test
    public void testSkip() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd', 'e');
        char[] result = stream.skip(2).toArray();
        assertArrayEquals(new char[] { 'c', 'd', 'e' }, result);
    }

    @Test
    public void testSkip_AllElements() {
        CharStream stream = createCharStream('a', 'b', 'c');
        char[] result = stream.skip(10).toArray();
        assertArrayEquals(new char[] {}, result);
    }

    @Test
    public void testSkip_WithAction() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd', 'e');
        List<Character> skipped = new ArrayList<>();
        CharConsumer action = skipped::add;
        char[] result = stream.skip(2, action).toArray();
        assertArrayEquals(new char[] { 'c', 'd', 'e' }, result);
        assertEquals(Arrays.asList('a', 'b'), skipped);
    }

    @Test
    public void testSkipAndLimit() {
        char[] result = createCharStream('a', 'b', 'c', 'd', 'e').skipAndLimit(1, 3).toArray();
        assertArrayEquals(new char[] { 'b', 'c', 'd' }, result);
    }

    @Test
    public void testSkipAndLimit_ZeroOffset() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd', 'e');
        char[] result = stream.skipAndLimit(0, 2).toArray();
        assertArrayEquals(new char[] { 'a', 'b' }, result);
    }

    @Test
    public void testSkipUntil() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd');
        CharPredicate predicate = c -> c == 'c';
        char[] result = stream.skipUntil(predicate).toArray();
        assertArrayEquals(new char[] { 'c', 'd' }, result);
    }

    @Test
    public void testSorted() {
        CharStream stream = createCharStream('e', 'b', 'd', 'a', 'c');
        char[] result = stream.sorted().toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, result);
    }

    @Test
    public void testSortedBy() {
        CharStream stream = createCharStream('c', 'a', 'b');
        List<Character> result = stream.boxed().sorted((a, b) -> Character.compare(b, a)).toList();
        assertEquals(Character.valueOf('c'), result.get(0));
        assertEquals(Character.valueOf('b'), result.get(1));
        assertEquals(Character.valueOf('a'), result.get(2));
    }

    // step

    @Test
    public void testStep() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd', 'e', 'f');
        char[] result = stream.step(2).toArray();
        assertArrayEquals(new char[] { 'a', 'c', 'e' }, result);
    }

    @Test
    public void testStep_InvalidStep() {
        assertThrows(IllegalArgumentException.class, () -> {
            createCharStream('a').step(0);
        });
    }

    @Test
    public void testStep_StepOne() {
        CharStream stream = createCharStream('a', 'b', 'c');
        char[] result = stream.step(1).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    public void testSymmetricDifference() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd');
        List<Character> c = Arrays.asList('b', 'c', 'e');
        char[] result = stream.symmetricDifference(c).toArray();
        char[] expected = { 'a', 'd', 'e' };
        Arrays.sort(result);
        Arrays.sort(expected);
        assertArrayEquals(expected, result);
    }

    // takeWhile

    @Test
    public void testTakeWhile() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd').takeWhile(c -> c < 'c');
        assertThat(stream.toArray()).containsExactly('a', 'b');
    }

    @Test
    public void testTakeWhile_BasicPredicate() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd', 'e');
        char[] result = stream.takeWhile(c -> c < 'd').toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    public void testTakeWhile_NoneMatch() {
        CharStream stream = createCharStream('a', 'b', 'c');
        char[] result = stream.takeWhile(c -> c > 'z').toArray();
        assertArrayEquals(new char[] {}, result);
    }

}
