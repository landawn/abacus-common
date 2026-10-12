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
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.CharSummaryStatistics;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;

/** CharStream reduction contracts. */
public class CharStreamReductionTest extends CharStreamTestSupport {

    @Test
    public void testAcceptIfNotEmpty() {
        CharStream stream = createCharStream('a', 'b', 'c');
        List<Character> collected = new ArrayList<>();
        stream.acceptIfNotEmpty(s -> s.forEach(c -> collected.add(c)));
        assertEquals(3, collected.size());
    }

    @Test
    public void testAcceptIfNotEmpty_EmptyStream() {
        CharStream stream = CharStream.empty();
        AtomicBoolean called = new AtomicBoolean(false);
        stream.acceptIfNotEmpty(s -> called.set(true));
        assertFalse(called.get());
    }

    // allMatch

    @Test
    public void testAllMatch() {
        assertTrue(stream.allMatch(c -> c >= 'a' && c <= 'z'));
        assertFalse(stream2.allMatch(c -> c == 'l'));
        assertTrue(createCharStream(new char[] {}).allMatch(c -> true));
    }

    @Test
    public void testAllMatch_False() {
        CharStream stream = createCharStream('a', 'b', 'c');
        assertFalse(stream.allMatch(c -> c == 'a'));
    }

    @Test
    public void testAllMatch_True() {
        CharStream stream = createCharStream('a', 'b', 'c');
        assertTrue(stream.allMatch(c -> c < 'z'));
    }

    @Test
    public void testAnyMatch_False() {
        CharStream stream = createCharStream('a', 'b', 'c');
        assertFalse(stream.anyMatch(c -> c == 'z'));
    }

    @Test
    public void testAnyMatch_True() {
        CharStream stream = createCharStream('a', 'b', 'c');
        assertTrue(stream.anyMatch(c -> c == 'b'));
    }

    @Test
    public void testApplyIfNotEmpty() {
        CharStream stream = createCharStream('a', 'b', 'c');
        Optional<char[]> result = stream.applyIfNotEmpty(s -> s.toArray());
        assertTrue(result.isPresent());
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result.get());
    }

    @Test
    public void testApplyIfNotEmpty_EmptyStream() {
        CharStream stream = CharStream.empty();
        Optional<char[]> result = stream.applyIfNotEmpty(s -> s.toArray());
        assertFalse(result.isPresent());
    }

    @Test
    public void testAverage() {
        CharStream stream = createCharStream('a', 'b', 'c');
        OptionalDouble result = stream.average();
        assertTrue(result.isPresent());
        assertEquals(98.0, result.getAsDouble(), 0.001);
    }

    @Test
    public void testAverage_EmptyStream() {
        CharStream stream = createCharStream();
        OptionalDouble result = stream.average();
        assertFalse(result.isPresent());
    }

    @Test
    public void testCount() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd', 'e');
        assertEquals(5, stream.count());
    }

    @Test
    public void testCount_EmptyStream() {
        CharStream stream = createCharStream();
        assertEquals(0, stream.count());
    }

    // elementAt

    @Test
    public void testElementAt() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd', 'e');
        OptionalChar result = stream.elementAt(2);
        assertTrue(result.isPresent());
        assertEquals('c', result.get());
    }

    @Test
    public void testElementAt_FirstElement() {
        CharStream stream = createCharStream('x', 'y', 'z');
        OptionalChar result = stream.elementAt(0);
        assertTrue(result.isPresent());
        assertEquals('x', result.get());
    }

    @Test
    public void testElementAt_NegativePosition() {
        CharStream stream = createCharStream('a', 'b', 'c');
        assertThrows(IllegalArgumentException.class, () -> stream.elementAt(-1));
    }

    @Test
    public void testElementAt_OutOfBounds() {
        CharStream stream = createCharStream('a', 'b');
        OptionalChar result = stream.elementAt(5);
        assertFalse(result.isPresent());
    }

    // findAny

    @Test
    public void testFindAny() {
        OptionalChar any = stream.findAny(c -> c == 'l');
        assertTrue(any.isPresent());
        assertEquals('l', any.get());

        any = stream2.findAny(c -> c == 'x');
        assertFalse(any.isPresent());
    }

    @Test
    public void testFindAny_NoCondition() {
        CharStream stream = createCharStream('a', 'b', 'c');
        OptionalChar result = stream.findAny();
        assertTrue(result.isPresent());
    }

    @Test
    public void testFindAny_WithCondition() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd');
        OptionalChar result = stream.findAny(c -> c > 'b');
        assertTrue(result.isPresent());
        assertTrue(result.get() >= 'c');
    }

    @Test
    public void testFindAnyNoPredicate_EmptyStream() {
        OptionalChar result = CharStream.empty().findAny();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindAnyNoPredicateDedicated() {
        OptionalChar result = createCharStream('a', 'b').findAny();
        assertTrue(result.isPresent());
    }

    // findFirst

    @Test
    public void testFindFirst() {
        OptionalChar first = stream.findFirst(c -> c == 'l');
        assertTrue(first.isPresent());
        assertEquals('l', first.get());

        first = stream2.findFirst(c -> c == 'x');
        assertFalse(first.isPresent());

        first = createCharStream(new char[] {}).findFirst(c -> true);
        assertFalse(first.isPresent());
    }

    @Test
    public void testFindFirst_NoCondition() {
        CharStream stream = createCharStream('a', 'b', 'c');
        OptionalChar result = stream.findFirst();
        assertTrue(result.isPresent());
        assertEquals('a', result.get());
    }

    @Test
    public void testFindFirst_NotFound() {
        CharStream stream = createCharStream('a', 'b', 'c');
        OptionalChar result = stream.findFirst(c -> c > 'z');
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindFirst_WithCondition() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd');
        OptionalChar result = stream.findFirst(c -> c > 'b');
        assertTrue(result.isPresent());
        assertEquals('c', result.get());
    }

    @Test
    public void testFindFirstNoPredicate_EmptyStream() {
        OptionalChar result = CharStream.empty().findFirst();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindFirstNoPredicateDedicated() {
        OptionalChar result = createCharStream('x', 'y', 'z').findFirst();
        assertTrue(result.isPresent());
        assertEquals('x', result.get());
    }

    // findLast

    @Test
    public void testFindLast() {
        OptionalChar last = stream.findLast(c -> c == 'l');
        assertTrue(last.isPresent());
        assertEquals('l', last.get());

        last = stream2.findLast(c -> c == 'o');
        assertTrue(last.isPresent());
        assertEquals('o', last.get());

        last = stream3.findLast(c -> c == 'x');
        assertFalse(last.isPresent());

        last = createCharStream(new char[] {}).findLast(c -> true);
        assertFalse(last.isPresent());
    }

    @Test
    public void testFindLast_NoMatch() {
        CharStream stream = createCharStream('a', 'b', 'c');
        OptionalChar result = stream.findLast(c -> c > 'z');
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindLast_WithCondition() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd');
        OptionalChar result = stream.findLast(c -> c < 'd');
        assertTrue(result.isPresent());
        assertEquals('c', result.get());
    }

    @Test
    public void testFirst() {
        CharStream stream = createCharStream('a', 'b', 'c');
        OptionalChar result = stream.first();
        assertTrue(result.isPresent());
        assertEquals('a', result.get());
    }

    @Test
    public void testFirst_EmptyStream() {
        CharStream stream = createCharStream();
        OptionalChar result = stream.first();
        assertFalse(result.isPresent());
    }

    @Test
    public void testForEach() {
        CharStream stream = createCharStream('a', 'b', 'c');
        List<Character> result = new ArrayList<>();
        stream.forEach(result::add);
        assertEquals(Arrays.asList('a', 'b', 'c'), result);
    }

    @Test
    public void testForEachIndexed() {
        CharStream stream = createCharStream('a', 'b', 'c');
        List<String> result = new ArrayList<>();
        stream.forEachIndexed((index, value) -> result.add(index + ":" + value));
        assertEquals(Arrays.asList("0:a", "1:b", "2:c"), result);
    }

    // kthLargest

    @Test
    public void testKthLargest() {
        CharStream stream = createCharStream('a', 'e', 'c', 'b', 'd');
        OptionalChar result = stream.kthLargest(2);
        assertTrue(result.isPresent());
        assertEquals('d', result.get());
    }

    @Test
    public void testKthLargest_K1() {
        CharStream stream = createCharStream('a', 'e', 'c', 'b', 'd');
        OptionalChar result = stream.kthLargest(1);
        assertTrue(result.isPresent());
        assertEquals('e', result.get());
    }

    @Test
    public void testKthLargest_SecondLargest() {
        CharStream stream = createCharStream('a', 'c', 'e', 'b', 'd');
        OptionalChar result = stream.kthLargest(2);
        assertTrue(result.isPresent());
        assertEquals('d', result.get());
    }

    @Test
    public void testKthLargest_TooLarge() {
        CharStream stream = createCharStream('a', 'b', 'c');
        OptionalChar result = stream.kthLargest(10);
        assertFalse(result.isPresent());
    }

    @Test
    public void testLast() {
        CharStream stream = createCharStream('a', 'b', 'c');
        OptionalChar result = stream.last();
        assertTrue(result.isPresent());
        assertEquals('c', result.get());
    }

    @Test
    public void testLast_EmptyStream() {
        CharStream stream = createCharStream();
        OptionalChar result = stream.last();
        assertFalse(result.isPresent());
    }

    @Test
    public void testMax() {
        CharStream stream = createCharStream('c', 'a', 'e', 'b', 'd');
        OptionalChar result = stream.max();
        assertTrue(result.isPresent());
        assertEquals('e', result.get());
    }

    @Test
    public void testMax_EmptyStream() {
        CharStream stream = createCharStream();
        OptionalChar result = stream.max();
        assertFalse(result.isPresent());
    }

    @Test
    public void testMin() {
        CharStream stream = createCharStream('c', 'a', 'e', 'b', 'd');
        OptionalChar result = stream.min();
        assertTrue(result.isPresent());
        assertEquals('a', result.get());
    }

    @Test
    public void testMin_EmptyStream() {
        CharStream stream = createCharStream();
        OptionalChar result = stream.min();
        assertFalse(result.isPresent());
    }

    // noneMatch

    @Test
    public void testNoneMatch() {
        assertTrue(stream.noneMatch(c -> c == 'x'));
        assertFalse(stream2.noneMatch(c -> c == 'l'));
        assertTrue(createCharStream(new char[] {}).noneMatch(c -> true));
    }

    @Test
    public void testNoneMatch_False() {
        CharStream stream = createCharStream('a', 'b', 'c');
        assertFalse(stream.noneMatch(c -> c == 'b'));
    }

    @Test
    public void testNoneMatch_True() {
        CharStream stream = createCharStream('a', 'b', 'c');
        assertTrue(stream.noneMatch(c -> c > 'z'));
    }

    @Test
    public void testPercentiles() {
        CharStream stream = createCharStream('a', 'e', 'c', 'b', 'd');
        Optional<Map<Percentage, Character>> result = stream.percentiles();
        assertTrue(result.isPresent());
        Map<Percentage, Character> percentiles = result.get();
        assertNotNull(percentiles);
        assertTrue(percentiles.containsKey(Percentage._0_0001));
        assertTrue(percentiles.containsKey(Percentage._50));
        assertTrue(percentiles.containsKey(Percentage._99_9999));
    }

    @Test
    public void testPercentiles_EmptyStream() {
        CharStream stream = createCharStream();
        Optional<Map<Percentage, Character>> result = stream.percentiles();
        assertFalse(result.isPresent());
    }

    // reduce

    @Test
    public void testReduce_EmptyStream() {
        CharStream stream = createCharStream();
        OptionalChar result = stream.reduce((a, b) -> (char) Math.max(a, b));
        assertFalse(result.isPresent());
    }

    @Test
    public void testReduce_WithIdentity() {
        CharStream stream = createCharStream('a', 'b', 'c');
        char result = stream.reduce('0', (a, b) -> (char) Math.max(a, b));
        assertEquals('c', result);
    }

    @Test
    public void testReduce_WithoutIdentity() {
        CharStream stream = createCharStream('a', 'b', 'c');
        OptionalChar result = stream.reduce((a, b) -> (char) Math.max(a, b));
        assertTrue(result.isPresent());
        assertEquals('c', result.get());
    }

    @Test
    public void testReduceIdentity() {
        char sum = createCharStream(new char[] { '1', '2', '3' }).reduce('0', (c1, c2) -> (char) (c1 + c2 - '0'));
        assertEquals((char) ('0' + '1' + '2' + '3' - '0' - '0' - '0'), sum);

        sum = createCharStream(new char[] {}).reduce('Z', (c1, c2) -> c1);
        assertEquals('Z', sum);
    }

    @Test
    public void testReduceWithIdentityOnEmpty() {
        char result = CharStream.empty().reduce('z', (a, b) -> (char) (a + b));
        assertEquals('z', result);
    }

    @Test
    public void testReduceWithoutIdentity() {
        OptionalChar result = createCharStream('a', 'b', 'c').reduce((c1, c2) -> c2);
        assertThat(result).isEqualTo(OptionalChar.of('c'));
    }

    @Test
    public void testSum() {
        CharStream stream = createCharStream('a', 'b', 'c');
        int result = stream.sum();
        assertEquals(294, result);
    }

    @Test
    public void testSum_EmptyStream() {
        CharStream stream = createCharStream();
        int result = stream.sum();
        assertEquals(0, result);
    }

    @Test
    public void testSummarize_EmptyStream() {
        CharStream stream = createCharStream();
        CharSummaryStatistics stats = stream.summaryStatistics();
        assertEquals(0, stats.getCount());
    }

    @Test
    public void testsummaryStatistics() {
        CharStream stream = createCharStream('a', 'e', 'c', 'b', 'd');
        CharSummaryStatistics stats = stream.summaryStatistics();
        assertEquals(5, stats.getCount());
        assertEquals('a', stats.getMin());
        assertEquals('e', stats.getMax());
        assertEquals(0 + 'a' + 'e' + 'c', +'b' + 'd', stats.getSum());
    }

    @Test
    public void testSummaryStatistics() {
        CharStream stream = createCharStream('a', 'b', 'c');
        CharSummaryStatistics stats = stream.summaryStatistics();
        assertEquals(3, stats.getCount());
        assertEquals('a', stats.getMin());
        assertEquals('c', stats.getMax());
    }

    // summaryStatisticsAndPercentiles

    @Test
    public void testsummaryStatisticsAndPercentiles() {
        CharStream stream = createCharStream('a', 'e', 'c', 'b', 'd');
        Pair<CharSummaryStatistics, Optional<Map<Percentage, Character>>> result = stream.summaryStatisticsAndPercentiles();

        CharSummaryStatistics stats = result.left();
        assertEquals(5, stats.getCount());
        assertEquals('a', stats.getMin());
        assertEquals('e', stats.getMax());

        Optional<Map<Percentage, Character>> percentiles = result.right();
        assertTrue(percentiles.isPresent());
    }

    @Test
    public void testSummaryStatisticsAndPercentiles() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd', 'e');
        Pair<CharSummaryStatistics, Optional<Map<Percentage, Character>>> result = stream.summaryStatisticsAndPercentiles();
        assertNotNull(result);
        assertEquals(5, result.left().getCount());
    }

    @Test
    public void testsummaryStatisticsAndPercentiles_EmptyStream() {
        CharStream stream = createCharStream();
        Pair<CharSummaryStatistics, Optional<Map<Percentage, Character>>> result = stream.summaryStatisticsAndPercentiles();
        CharSummaryStatistics stats = result.left();
        assertEquals(0, stats.getCount());
        assertFalse(result.right().isPresent());
    }

}
