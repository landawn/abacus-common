package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class CharSummaryStatisticsTest extends TestBase {

    @Test
    public void testDefaultConstructor() {
        CharSummaryStatistics stats = new CharSummaryStatistics();
        assertEquals(0L, stats.getCount());
        assertEquals(0L, stats.getSum());
        assertEquals(Character.MAX_VALUE, stats.getMin());
        assertEquals(Character.MIN_VALUE, stats.getMax());
        assertEquals(0.0, stats.getAverage());
    }

    @Test
    public void testConstructorWithValues() {
        CharSummaryStatistics stats = new CharSummaryStatistics(3, 'A', 'C', 198);
        assertEquals(3L, stats.getCount());
        assertEquals('A', stats.getMin());
        assertEquals('C', stats.getMax());
        assertEquals(198L, stats.getSum());
    }

    @Test
    public void testAcceptSingleValue() {
        CharSummaryStatistics stats = new CharSummaryStatistics();
        stats.accept('M');

        assertEquals(1L, stats.getCount());
        assertEquals('M', stats.getSum());
        assertEquals('M', stats.getMin());
        assertEquals('M', stats.getMax());
    }

    @Test
    public void testAcceptMultipleValues() {
        CharSummaryStatistics stats = new CharSummaryStatistics();
        stats.accept('A');
        stats.accept('B');
        stats.accept('C');

        assertEquals(3L, stats.getCount());
        assertEquals(198L, stats.getSum());
        assertEquals('A', stats.getMin());
        assertEquals('C', stats.getMax());
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

        assertEquals(4L, stats1.getCount());
        assertEquals('A', stats1.getMin());
        assertEquals('Z', stats1.getMax());
    }

    @Test
    public void testGetMin() {
        CharSummaryStatistics stats = new CharSummaryStatistics();
        stats.accept('M');
        stats.accept('A');
        stats.accept('Z');

        assertEquals('A', stats.getMin());
    }

    @Test
    public void testGetMax() {
        CharSummaryStatistics stats = new CharSummaryStatistics();
        stats.accept('M');
        stats.accept('A');
        stats.accept('Z');

        assertEquals('Z', stats.getMax());
    }

    @Test
    public void testGetCount() {
        CharSummaryStatistics stats = new CharSummaryStatistics();
        assertEquals(0L, stats.getCount());

        stats.accept('A');
        assertEquals(1L, stats.getCount());

        stats.accept('B');
        assertEquals(2L, stats.getCount());
    }

    @Test
    public void testGetSum() {
        CharSummaryStatistics stats = new CharSummaryStatistics();
        assertEquals(0L, stats.getSum());

        stats.accept('A');
        assertEquals(65L, stats.getSum());

        stats.accept('B');
        assertEquals(131L, stats.getSum());
    }

    @Test
    public void testGetAverage() {
        CharSummaryStatistics stats = new CharSummaryStatistics();
        stats.accept('A');
        stats.accept('B');
        stats.accept('C');

        double expected = 198.0 / 3.0;
        assertEquals(expected, stats.getAverage());
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

        String str = stats.toString();
        assertNotNull(str);
        assertTrue(str.contains("min="));
        assertTrue(str.contains("max="));
        assertTrue(str.contains("count="));
        assertTrue(str.contains("sum="));
        assertTrue(str.contains("average="));
    }

    @Test
    public void testNumericCharacters() {
        CharSummaryStatistics stats = new CharSummaryStatistics();
        stats.accept('0');
        stats.accept('5');
        stats.accept('9');

        assertEquals(3L, stats.getCount());
        assertEquals('0', stats.getMin());
        assertEquals('9', stats.getMax());
    }

    @Test
    public void testSpecialCharacters() {
        CharSummaryStatistics stats = new CharSummaryStatistics();
        stats.accept('!');
        stats.accept('@');
        stats.accept('#');

        assertEquals(3L, stats.getCount());
        assertTrue(stats.getSum() > 0);
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

    @Test
    public void testAcceptNullChar() {
        CharSummaryStatistics stats = new CharSummaryStatistics();
        stats.accept('\0');

        assertEquals(1L, stats.getCount());
        assertEquals(0L, stats.getSum());
        assertEquals('\0', stats.getMin());
        assertEquals('\0', stats.getMax());
        assertEquals(0.0, stats.getAverage());
    }

    @Test
    public void testAcceptSameCharMultipleTimes() {
        CharSummaryStatistics stats = new CharSummaryStatistics();
        stats.accept('X');
        stats.accept('X');
        stats.accept('X');

        assertEquals(3L, stats.getCount());
        assertEquals('X', stats.getMin());
        assertEquals('X', stats.getMax());
        assertEquals(3L * 'X', stats.getSum());
        assertEquals((double) 'X', stats.getAverage());
    }

    @Test
    public void testAcceptUpdatesMinAndMax() {
        CharSummaryStatistics stats = new CharSummaryStatistics();

        stats.accept('M');
        assertEquals('M', stats.getMin());
        assertEquals('M', stats.getMax());

        stats.accept('A');
        assertEquals('A', stats.getMin());
        assertEquals('M', stats.getMax());

        stats.accept('Z');
        assertEquals('A', stats.getMin());
        assertEquals('Z', stats.getMax());
    }

    @Test
    public void testAcceptUnicodeChar() {
        CharSummaryStatistics stats = new CharSummaryStatistics();
        stats.accept('\u00FF');
        stats.accept('\u0100');

        assertEquals(2L, stats.getCount());
        assertEquals('\u00FF', stats.getMin());
        assertEquals('\u0100', stats.getMax());
    }

}
