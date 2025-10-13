package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class CharSummaryStatistics100Test extends TestBase {

    @Test
    public void testDefaultConstructor() {
        CharSummaryStatistics stats = new CharSummaryStatistics();
        assertEquals(0, stats.getCount());
        assertEquals(Character.MAX_VALUE, stats.getMin());
        assertEquals(Character.MIN_VALUE, stats.getMax());
        assertEquals(0L, stats.getSum());
        assertEquals(0.0, stats.getAverage());
    }

    @Test
    public void testParameterizedConstructor() {
        CharSummaryStatistics stats = new CharSummaryStatistics(5, 'A', 'E', 335);
        assertEquals(5, stats.getCount());
        assertEquals('A', stats.getMin());
        assertEquals('E', stats.getMax());
        assertEquals(335L, stats.getSum());
        assertEquals(67.0, stats.getAverage());
    }

    @Test
    public void testAcceptSingleValue() {
        CharSummaryStatistics stats = new CharSummaryStatistics();
        stats.accept('X');

        assertEquals(1, stats.getCount());
        assertEquals('X', stats.getMin());
        assertEquals('X', stats.getMax());
        assertEquals(88L, stats.getSum());
        assertEquals(88.0, stats.getAverage());
    }

    @Test
    public void testAcceptMultipleValues() {
        CharSummaryStatistics stats = new CharSummaryStatistics();
        stats.accept('A');
        stats.accept('B');
        stats.accept('C');

        assertEquals(3, stats.getCount());
        assertEquals('A', stats.getMin());
        assertEquals('C', stats.getMax());
        assertEquals(198L, stats.getSum());
        assertEquals(66.0, stats.getAverage());
    }

    @Test
    public void testAcceptMixedCase() {
        CharSummaryStatistics stats = new CharSummaryStatistics();
        stats.accept('a');
        stats.accept('A');
        stats.accept('Z');

        assertEquals(3, stats.getCount());
        assertEquals('A', stats.getMin());
        assertEquals('a', stats.getMax());
        assertEquals(252L, stats.getSum());
        assertEquals(84.0, stats.getAverage());
    }

    @Test
    public void testCombine() {
        CharSummaryStatistics stats1 = new CharSummaryStatistics();
        stats1.accept('A');
        stats1.accept('B');

        CharSummaryStatistics stats2 = new CharSummaryStatistics();
        stats2.accept('Y');
        stats2.accept('Z');

        stats1.combine(stats2);

        assertEquals(4, stats1.getCount());
        assertEquals('A', stats1.getMin());
        assertEquals('Z', stats1.getMax());
        assertEquals(310L, stats1.getSum());
        assertEquals(77.5, stats1.getAverage());
    }

    @Test
    public void testCombineWithEmpty() {
        CharSummaryStatistics stats1 = new CharSummaryStatistics();
        stats1.accept('M');
        stats1.accept('N');

        CharSummaryStatistics stats2 = new CharSummaryStatistics();

        stats1.combine(stats2);

        assertEquals(2, stats1.getCount());
        assertEquals('M', stats1.getMin());
        assertEquals('N', stats1.getMax());
        assertEquals(155L, stats1.getSum());
        assertEquals(77.5, stats1.getAverage());
    }

    @Test
    public void testCombineEmptyWithNonEmpty() {
        CharSummaryStatistics stats1 = new CharSummaryStatistics();

        CharSummaryStatistics stats2 = new CharSummaryStatistics();
        stats2.accept('X');
        stats2.accept('Y');

        stats1.combine(stats2);

        assertEquals(2, stats1.getCount());
        assertEquals('X', stats1.getMin());
        assertEquals('Y', stats1.getMax());
        assertEquals(177L, stats1.getSum());
        assertEquals(88.5, stats1.getAverage());
    }

    @Test
    public void testGetMinWithNoValues() {
        CharSummaryStatistics stats = new CharSummaryStatistics();
        assertEquals(Character.MAX_VALUE, stats.getMin());
    }

    @Test
    public void testGetMaxWithNoValues() {
        CharSummaryStatistics stats = new CharSummaryStatistics();
        assertEquals(Character.MIN_VALUE, stats.getMax());
    }

    @Test
    public void testGetCountEmpty() {
        CharSummaryStatistics stats = new CharSummaryStatistics();
        assertEquals(0, stats.getCount());
    }

    @Test
    public void testGetSumEmpty() {
        CharSummaryStatistics stats = new CharSummaryStatistics();
        assertEquals(0L, stats.getSum());
    }

    @Test
    public void testGetAverageEmpty() {
        CharSummaryStatistics stats = new CharSummaryStatistics();
        assertEquals(0.0, stats.getAverage());
    }

    @Test
    public void testToString() {
        CharSummaryStatistics stats = new CharSummaryStatistics();
        stats.accept('A');
        stats.accept('B');

        String result = stats.toString();
        assertTrue(result.contains("min=A"));
        assertTrue(result.contains("max=B"));
        assertTrue(result.contains("count=2"));
        assertTrue(result.contains("sum=131"));
        assertTrue(result.contains("average=65.500000"));
    }

    @Test
    public void testSpecialCharacters() {
        CharSummaryStatistics stats = new CharSummaryStatistics();
        stats.accept('\n');
        stats.accept('\t');
        stats.accept(' ');

        assertEquals(3, stats.getCount());
        assertEquals('\t', stats.getMin());
        assertEquals(' ', stats.getMax());
        assertEquals(51L, stats.getSum());
        assertEquals(17.0, stats.getAverage());
    }

    @Test
    public void testDigitCharacters() {
        CharSummaryStatistics stats = new CharSummaryStatistics();
        stats.accept('0');
        stats.accept('5');
        stats.accept('9');

        assertEquals(3, stats.getCount());
        assertEquals('0', stats.getMin());
        assertEquals('9', stats.getMax());
        assertEquals(158L, stats.getSum());
        assertEquals(52.666666666666664, stats.getAverage(), 0.000001);
    }
}
