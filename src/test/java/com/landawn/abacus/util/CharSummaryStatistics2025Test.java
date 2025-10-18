package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class CharSummaryStatistics2025Test extends TestBase {

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
}
