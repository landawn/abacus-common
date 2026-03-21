package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.util.u.Optional;

@SuppressWarnings("boxing")
public class RangeTest extends AbstractTest {

    private Range<Byte> byteRange;
    private Range<Byte> byteRange2;
    private Range<Byte> byteRange3;

    private Range<Integer> intRange;
    private Range<Long> longRange;
    private Range<Float> floatRange;
    private Range<Double> doubleRange;

    @BeforeEach
    public void setUp() {
        byteRange = Range.closed((byte) 0, (byte) 5);
        byteRange2 = Range.closed((byte) 0, (byte) 5);
        byteRange3 = Range.closed((byte) 0, (byte) 10);

        intRange = Range.closed(10, 20);
        longRange = Range.closed((long) 10, (long) 20);
        floatRange = Range.closed((float) 10, (float) 20);
        doubleRange = Range.closed((double) 10, (double) 20);
    }

    @Test
    public void testRangeOfChars() {
        final Range<Character> chars = Range.closed('a', 'z');
        assertTrue(chars.contains('b'));
        assertFalse(chars.contains('B'));
    }

    @Test
    public void testJust() {
        Range<Integer> range = Range.just(5);
        Assertions.assertTrue(range.contains(5));
        Assertions.assertFalse(range.contains(4));
        Assertions.assertFalse(range.contains(6));
        Assertions.assertEquals(5, range.lowerEndpoint());
        Assertions.assertEquals(5, range.upperEndpoint());
        Assertions.assertEquals(Range.BoundType.CLOSED_CLOSED, range.boundType());
    }

    @Test
    public void test_just() {
        Range<Integer> range = Range.just(5);
        assertNotNull(range);
        assertEquals(5, range.lowerEndpoint());
        assertEquals(5, range.upperEndpoint());
        assertTrue(range.contains(5));
        assertFalse(range.contains(4));
        assertFalse(range.contains(6));
        assertEquals(Range.BoundType.CLOSED_CLOSED, range.boundType());
    }

    @Test
    public void test_just_null() {
        assertThrows(IllegalArgumentException.class, () -> Range.just(null));
    }

    @Test
    public void testOpen() {
        Range<Integer> range = Range.open(1, 5);
        Assertions.assertFalse(range.contains(1));
        Assertions.assertTrue(range.contains(2));
        Assertions.assertTrue(range.contains(3));
        Assertions.assertTrue(range.contains(4));
        Assertions.assertFalse(range.contains(5));
        Assertions.assertEquals(Range.BoundType.OPEN_OPEN, range.boundType());
    }

    @Test
    public void test_open() {
        Range<Integer> range = Range.open(1, 5);
        assertNotNull(range);
        assertEquals(1, range.lowerEndpoint());
        assertEquals(5, range.upperEndpoint());
        assertFalse(range.contains(1));
        assertTrue(range.contains(3));
        assertFalse(range.contains(5));
        assertEquals(Range.BoundType.OPEN_OPEN, range.boundType());
    }

    @Test
    public void test_open_equalEndpoints() {
        Range<Integer> range = Range.open(5, 5);
        assertNotNull(range);
        assertTrue(range.isEmpty());
    }

    @Test
    public void test_open_nullMin() {
        assertThrows(IllegalArgumentException.class, () -> Range.open(null, 5));
    }

    @Test
    public void test_open_nullMax() {
        assertThrows(IllegalArgumentException.class, () -> Range.open(1, null));
    }

    @Test
    public void test_open_minGreaterThanMax() {
        assertThrows(IllegalArgumentException.class, () -> Range.open(10, 5));
    }

    @Test
    public void testInvalidRanges() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Range.open(null, 5));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Range.open(1, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Range.open(null, null));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Range.open(5, 1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Range.closed(10, 5));
    }

    @Test
    public void testOpenClosed() {
        Range<Integer> range = Range.openClosed(1, 5);
        Assertions.assertFalse(range.contains(1));
        Assertions.assertTrue(range.contains(2));
        Assertions.assertTrue(range.contains(5));
        Assertions.assertEquals(Range.BoundType.OPEN_CLOSED, range.boundType());
    }

    @Test
    public void test_openClosed() {
        Range<Integer> range = Range.openClosed(1, 5);
        assertNotNull(range);
        assertEquals(1, range.lowerEndpoint());
        assertEquals(5, range.upperEndpoint());
        assertFalse(range.contains(1));
        assertTrue(range.contains(3));
        assertTrue(range.contains(5));
        assertEquals(Range.BoundType.OPEN_CLOSED, range.boundType());
    }

    @Test
    public void test_openClosed_nullMin() {
        assertThrows(IllegalArgumentException.class, () -> Range.openClosed(null, 5));
    }

    @Test
    public void test_openClosed_nullMax() {
        assertThrows(IllegalArgumentException.class, () -> Range.openClosed(1, null));
    }

    @Test
    public void test_openClosed_minGreaterThanMax() {
        assertThrows(IllegalArgumentException.class, () -> Range.openClosed(10, 5));
    }

    @Test
    public void testClosedOpen() {
        Range<Integer> range = Range.closedOpen(1, 5);
        Assertions.assertTrue(range.contains(1));
        Assertions.assertTrue(range.contains(4));
        Assertions.assertFalse(range.contains(5));
        Assertions.assertEquals(Range.BoundType.CLOSED_OPEN, range.boundType());
    }

    @Test
    public void test_closedOpen() {
        Range<Integer> range = Range.closedOpen(1, 5);
        assertNotNull(range);
        assertEquals(1, range.lowerEndpoint());
        assertEquals(5, range.upperEndpoint());
        assertTrue(range.contains(1));
        assertTrue(range.contains(3));
        assertFalse(range.contains(5));
        assertEquals(Range.BoundType.CLOSED_OPEN, range.boundType());
    }

    @Test
    public void test_closedOpen_nullMin() {
        assertThrows(IllegalArgumentException.class, () -> Range.closedOpen(null, 5));
    }

    @Test
    public void test_closedOpen_nullMax() {
        assertThrows(IllegalArgumentException.class, () -> Range.closedOpen(1, null));
    }

    @Test
    public void test_closedOpen_minGreaterThanMax() {
        assertThrows(IllegalArgumentException.class, () -> Range.closedOpen(10, 5));
    }

    @Test
    public void test_compareTo_elementBefore() {
        Range<Integer> range = Range.closed(5, 10);
        assertEquals(1, range.positionOf(3));
    }

    @Test
    public void test_compareTo_elementWithin() {
        Range<Integer> range = Range.closed(5, 10);
        assertEquals(0, range.positionOf(7));
    }

    @Test
    public void test_compareTo_elementAfter() {
        Range<Integer> range = Range.closed(5, 10);
        assertEquals(-1, range.positionOf(12));
    }

    @Test
    public void test_compareTo_atBoundaries() {
        Range<Integer> range = Range.closed(5, 10);
        assertEquals(0, range.positionOf(5));
        assertEquals(0, range.positionOf(10));
    }

    @Test
    public void testClosed() {
        Range<Integer> range = Range.closed(1, 5);
        Assertions.assertTrue(range.contains(1));
        Assertions.assertTrue(range.contains(3));
        Assertions.assertTrue(range.contains(5));
        Assertions.assertFalse(range.contains(0));
        Assertions.assertFalse(range.contains(6));
        Assertions.assertEquals(Range.BoundType.CLOSED_CLOSED, range.boundType());
    }

    @Test
    public void testWithStrings() {
        Range<String> range = Range.closed("apple", "banana");

        Assertions.assertTrue(range.contains("apple"));
        Assertions.assertTrue(range.contains("apricot"));
        Assertions.assertTrue(range.contains("banana"));
        Assertions.assertFalse(range.contains("cherry"));
        Assertions.assertFalse(range.contains("aardvark"));
    }

    @Test
    public void test_closed() {
        Range<Integer> range = Range.closed(1, 5);
        assertNotNull(range);
        assertEquals(1, range.lowerEndpoint());
        assertEquals(5, range.upperEndpoint());
        assertTrue(range.contains(1));
        assertTrue(range.contains(3));
        assertTrue(range.contains(5));
        assertEquals(Range.BoundType.CLOSED_CLOSED, range.boundType());
    }

    @Test
    public void test_closed_equalEndpoints() {
        Range<Integer> range = Range.closed(5, 5);
        assertNotNull(range);
        assertFalse(range.isEmpty());
        assertTrue(range.contains(5));
    }

    @Test
    public void test_edgeCase_largeNumbers() {
        Range<Long> range = Range.closed(Long.MAX_VALUE - 100, Long.MAX_VALUE);
        assertTrue(range.contains(Long.MAX_VALUE - 50));
        assertTrue(range.contains(Long.MAX_VALUE));
    }

    @Test
    public void test_edgeCase_strings() {
        Range<String> range = Range.closed("apple", "zebra");
        assertTrue(range.contains("banana"));
        assertTrue(range.contains("apple"));
        assertFalse(range.contains("aardvark"));
    }

    @Test
    public void test_edgeCase_doubles() {
        Range<Double> range = Range.closed(1.0, 10.0);
        assertTrue(range.contains(5.5));
        assertTrue(range.contains(1.0));
        assertFalse(range.contains(0.9999));
    }

    @Test
    public void test_closed_nullMin() {
        assertThrows(IllegalArgumentException.class, () -> Range.closed(null, 5));
    }

    @Test
    public void test_closed_nullMax() {
        assertThrows(IllegalArgumentException.class, () -> Range.closed(1, null));
    }

    @Test
    public void test_closed_minGreaterThanMax() {
        assertThrows(IllegalArgumentException.class, () -> Range.closed(10, 5));
    }

    @Test
    public void test_compareTo_null() {
        Range<Integer> range = Range.closed(5, 10);
        assertThrows(IllegalArgumentException.class, () -> range.positionOf(null));
    }

    @Test
    public void testCompareTo() {
        Range<Integer> range = Range.closed(5, 10);

        Assertions.assertEquals(1, range.positionOf(3));
        Assertions.assertEquals(0, range.positionOf(7));
        Assertions.assertEquals(-1, range.positionOf(12));

        Assertions.assertThrows(IllegalArgumentException.class, () -> range.positionOf(null));
    }

    @Test
    public void test_map_preservesBoundType() {
        Range<Integer> openRange = Range.open(1, 5);
        Range<String> strRange = openRange.map(String::valueOf);
        assertEquals(Range.BoundType.OPEN_OPEN, strRange.boundType());
        assertFalse(strRange.contains("1"));
        assertFalse(strRange.contains("5"));
    }

    @Test
    public void test_map_openClosed() {
        Range<Integer> range = Range.openClosed(1, 5);
        Range<Long> longRange = range.map(Integer::longValue);
        assertEquals(Range.BoundType.OPEN_CLOSED, longRange.boundType());
        assertFalse(longRange.contains(1L));
        assertTrue(longRange.contains(5L));
    }

    @Test
    public void test_map_closedOpen() {
        Range<Integer> range = Range.closedOpen(1, 5);
        Range<Long> longRange = range.map(Integer::longValue);
        assertEquals(Range.BoundType.CLOSED_OPEN, longRange.boundType());
        assertTrue(longRange.contains(1L));
        assertFalse(longRange.contains(5L));
    }

    @Test
    public void testMap() {
        Range<Integer> intRange = Range.closed(1, 5);
        Range<String> strRange = intRange.map(String::valueOf);

        Assertions.assertEquals("1", strRange.lowerEndpoint());
        Assertions.assertEquals("5", strRange.upperEndpoint());
        Assertions.assertEquals(Range.BoundType.CLOSED_CLOSED, strRange.boundType());
    }

    @Test
    public void test_map() {
        Range<Integer> intRange = Range.closed(1, 5);
        Range<String> strRange = intRange.map(String::valueOf);
        assertNotNull(strRange);
        assertEquals("1", strRange.lowerEndpoint());
        assertEquals("5", strRange.upperEndpoint());
        assertTrue(strRange.contains("3"));
        assertEquals(Range.BoundType.CLOSED_CLOSED, strRange.boundType());
    }

    @Test
    public void test_boundType_open() {
        Range<Integer> range = Range.open(1, 5);
        assertEquals(Range.BoundType.OPEN_OPEN, range.boundType());
    }

    @Test
    public void test_boundType_openClosed() {
        Range<Integer> range = Range.openClosed(1, 5);
        assertEquals(Range.BoundType.OPEN_CLOSED, range.boundType());
    }

    @Test
    public void test_boundType_closedOpen() {
        Range<Integer> range = Range.closedOpen(1, 5);
        assertEquals(Range.BoundType.CLOSED_OPEN, range.boundType());
    }

    @Test
    public void test_boundType_closed() {
        Range<Integer> range = Range.closed(1, 5);
        assertEquals(Range.BoundType.CLOSED_CLOSED, range.boundType());
    }

    @Test
    public void test_lowerEndpoint() {
        Range<Integer> range = Range.closed(10, 20);
        assertEquals(10, range.lowerEndpoint());
    }

    @Test
    public void test_lowerEndpoint_differentTypes() {
        Range<String> range = Range.closed("a", "z");
        assertEquals("a", range.lowerEndpoint());
    }

    @Test
    public void testGetMinimum() {
        assertEquals(10, (int) intRange.lowerEndpoint());
        assertEquals(10L, (long) longRange.lowerEndpoint());
        assertEquals(10f, floatRange.lowerEndpoint(), 0.00001f);
        assertEquals(10d, doubleRange.lowerEndpoint(), 0.00001d);
    }

    @Test
    public void test_upperEndpoint() {
        Range<Integer> range = Range.closed(10, 20);
        assertEquals(20, range.upperEndpoint());
    }

    @Test
    public void test_upperEndpoint_differentTypes() {
        Range<String> range = Range.closed("a", "z");
        assertEquals("z", range.upperEndpoint());
    }

    @Test
    public void testGetMaximum() {
        assertEquals(20, (int) intRange.upperEndpoint());
        assertEquals(20L, (long) longRange.upperEndpoint());
        assertEquals(20f, floatRange.upperEndpoint(), 0.00001f);
        assertEquals(20d, doubleRange.upperEndpoint(), 0.00001d);
    }

    @Test
    public void test_contains_withinRange() {
        Range<Integer> range = Range.closed(1, 10);
        assertTrue(range.contains(5));
        assertTrue(range.contains(1));
        assertTrue(range.contains(10));
    }

    @Test
    public void test_contains_outsideRange() {
        Range<Integer> range = Range.closed(1, 10);
        assertFalse(range.contains(0));
        assertFalse(range.contains(11));
    }

    @Test
    public void test_contains_openBounds() {
        Range<Integer> range = Range.open(1, 10);
        assertFalse(range.contains(1));
        assertTrue(range.contains(5));
        assertFalse(range.contains(10));
    }

    @Test
    public void test_contains_openClosedBounds() {
        Range<Integer> range = Range.openClosed(1, 10);
        assertFalse(range.contains(1));
        assertTrue(range.contains(5));
        assertTrue(range.contains(10));
    }

    @Test
    public void test_contains_closedOpenBounds() {
        Range<Integer> range = Range.closedOpen(1, 10);
        assertTrue(range.contains(1));
        assertTrue(range.contains(5));
        assertFalse(range.contains(10));
    }

    @Test
    public void testContains() {
        assertFalse(intRange.contains(null));

        assertFalse(intRange.contains(5));
        assertTrue(intRange.contains(10));
        assertTrue(intRange.contains(15));
        assertTrue(intRange.contains(20));
        assertFalse(intRange.contains(25));
    }

    @Test
    public void test_contains_null() {
        Range<Integer> range = Range.closed(1, 10);
        assertFalse(range.contains(null));
    }

    @Test
    public void test_containsAll_allWithin() {
        Range<Integer> range = Range.closed(1, 10);
        List<Integer> values = Arrays.asList(2, 5, 8);
        assertTrue(range.containsAll(values));
    }

    @Test
    public void test_containsAll_someOutside() {
        Range<Integer> range = Range.closed(1, 10);
        List<Integer> values = Arrays.asList(2, 5, 15);
        assertFalse(range.containsAll(values));
    }

    @Test
    public void test_containsAll_emptyCollection() {
        Range<Integer> range = Range.closed(1, 10);
        assertTrue(range.containsAll(Collections.emptyList()));
    }

    @Test
    public void test_containsAll_nullCollection() {
        Range<Integer> range = Range.closed(1, 10);
        assertTrue(range.containsAll(null));
    }

    @Test
    public void test_containsAll_withNullElements() {
        Range<Integer> range = Range.closed(1, 10);
        List<Integer> values = Arrays.asList(2, null, 5);
        assertFalse(range.containsAll(values));
    }

    @Test
    public void testContainsAll() {
        Range<Integer> range = Range.closed(1, 10);

        List<Integer> valid = Arrays.asList(2, 5, 8);
        Assertions.assertTrue(range.containsAll(valid));

        List<Integer> invalid = Arrays.asList(2, 5, 15);
        Assertions.assertFalse(range.containsAll(invalid));

        Assertions.assertTrue(range.containsAll(Arrays.asList()));

        Assertions.assertTrue(range.containsAll(null));

        List<Integer> endpoints = Arrays.asList(1, 10);
        Assertions.assertTrue(range.containsAll(endpoints));
    }

    @Test
    public void test_containsAny_someWithin() {
        Range<Integer> range = Range.closed(5, 10);
        assertTrue(range.containsAny(Arrays.asList(3, 7, 12)));
    }

    @Test
    public void test_containsAny_allWithin() {
        Range<Integer> range = Range.closed(5, 10);
        assertTrue(range.containsAny(Arrays.asList(5, 7, 10)));
    }

    @Test
    public void test_containsAny_openBounds() {
        Range<Integer> range = Range.open(5, 10);
        assertFalse(range.containsAny(Arrays.asList(5, 10)));
        assertTrue(range.containsAny(Arrays.asList(5, 6, 10)));
    }

    @Test
    public void test_containsAny_noneWithin() {
        Range<Integer> range = Range.closed(5, 10);
        assertFalse(range.containsAny(Arrays.asList(1, 2, 11, 15)));
    }

    @Test
    public void test_containsAny_emptyCollection() {
        Range<Integer> range = Range.closed(5, 10);
        assertFalse(range.containsAny(Collections.emptyList()));
    }

    @Test
    public void test_containsAny_nullCollection() {
        Range<Integer> range = Range.closed(5, 10);
        assertFalse(range.containsAny(null));
    }

    @Test
    public void test_containsAny_atBoundary() {
        Range<Integer> range = Range.closed(5, 10);
        assertTrue(range.containsAny(Arrays.asList(5)));
        assertTrue(range.containsAny(Arrays.asList(10)));

        Range<Integer> openRange = Range.open(5, 10);
        assertFalse(openRange.containsAny(Arrays.asList(5)));
        assertFalse(openRange.containsAny(Arrays.asList(10)));
        assertTrue(openRange.containsAny(Arrays.asList(6)));
    }

    @Test
    public void test_containsAny_singleElement() {
        Range<Integer> range = Range.closed(5, 10);
        assertTrue(range.containsAny(Arrays.asList(7)));
        assertFalse(range.containsAny(Arrays.asList(11)));
    }

    @Test
    public void testContainsAny() {
        Range<Integer> range = Range.closed(5, 15);

        Assertions.assertTrue(range.containsAny(Arrays.asList(1, 10, 20)));
        Assertions.assertFalse(range.containsAny(Arrays.asList(1, 2, 3)));
        Assertions.assertFalse(range.containsAny(Arrays.asList(16, 20, 25)));
        Assertions.assertFalse(range.containsAny(Collections.emptyList()));
        Assertions.assertFalse(range.containsAny(null));

        // Boundary tests
        Assertions.assertTrue(range.containsAny(Arrays.asList(5)));
        Assertions.assertTrue(range.containsAny(Arrays.asList(15)));

        Range<Integer> openRange = Range.open(5, 15);
        Assertions.assertFalse(openRange.containsAny(Arrays.asList(5, 15)));
        Assertions.assertTrue(openRange.containsAny(Arrays.asList(5, 6, 15)));
    }

    @Test
    public void test_isStartedBy_closedLowerBound() {
        Range<Integer> range = Range.closed(5, 10);
        assertTrue(range.isStartedBy(5));
        assertFalse(range.isStartedBy(6));
    }

    @Test
    public void test_isStartedBy_openLowerBound() {
        Range<Integer> range = Range.open(5, 10);
        assertFalse(range.isStartedBy(5));
    }

    @Test
    public void testIsStartedBy() {
        assertFalse(intRange.isStartedBy(null));

        assertFalse(intRange.isStartedBy(5));
        assertTrue(intRange.isStartedBy(10));
        assertFalse(intRange.isStartedBy(15));
        assertFalse(intRange.isStartedBy(20));
        assertFalse(intRange.isStartedBy(25));
    }

    @Test
    public void test_isStartedBy_null() {
        Range<Integer> range = Range.closed(5, 10);
        assertFalse(range.isStartedBy(null));
    }

    @Test
    public void test_isEndedBy_closedUpperBound() {
        Range<Integer> range = Range.closed(5, 10);
        assertTrue(range.isEndedBy(10));
        assertFalse(range.isEndedBy(9));
    }

    @Test
    public void test_isEndedBy_openUpperBound() {
        Range<Integer> range = Range.closedOpen(5, 10);
        assertFalse(range.isEndedBy(10));
    }

    @Test
    public void testIsEndedBy() {
        assertFalse(intRange.isEndedBy(null));

        assertFalse(intRange.isEndedBy(5));
        assertFalse(intRange.isEndedBy(10));
        assertFalse(intRange.isEndedBy(15));
        assertTrue(intRange.isEndedBy(20));
        assertFalse(intRange.isEndedBy(25));
    }

    @Test
    public void test_isEndedBy_null() {
        Range<Integer> range = Range.closed(5, 10);
        assertFalse(range.isEndedBy(null));
    }

    @Test
    public void test_isAfter_elementBefore() {
        Range<Integer> range = Range.closed(5, 10);
        assertTrue(range.isAfter(3));
    }

    @Test
    public void test_isAfter_elementAtLowerBound() {
        Range<Integer> range = Range.closed(5, 10);
        assertFalse(range.isAfter(5));
    }

    @Test
    public void test_isAfter_elementWithin() {
        Range<Integer> range = Range.closed(5, 10);
        assertFalse(range.isAfter(7));
    }

    @Test
    public void test_isAfter_openLowerBound() {
        Range<Integer> range = Range.open(5, 10);
        assertTrue(range.isAfter(5));
        assertFalse(range.isAfter(7));
    }

    @Test
    public void testIsAfter() {
        assertFalse(intRange.isAfter(null));

        assertTrue(intRange.isAfter(5));
        assertFalse(intRange.isAfter(10));
        assertFalse(intRange.isAfter(15));
        assertFalse(intRange.isAfter(20));
        assertFalse(intRange.isAfter(25));
    }

    @Test
    public void test_isAfter_null() {
        Range<Integer> range = Range.closed(5, 10);
        assertFalse(range.isAfter(null));
    }

    @Test
    public void test_isBefore_elementAfter() {
        Range<Integer> range = Range.closed(5, 10);
        assertTrue(range.isBefore(12));
    }

    @Test
    public void test_isBefore_elementAtUpperBound() {
        Range<Integer> range = Range.closed(5, 10);
        assertFalse(range.isBefore(10));
    }

    @Test
    public void test_isBefore_elementWithin() {
        Range<Integer> range = Range.closed(5, 10);
        assertFalse(range.isBefore(7));
    }

    @Test
    public void testIsBefore() {
        assertFalse(intRange.isBefore(null));

        assertFalse(intRange.isBefore(5));
        assertFalse(intRange.isBefore(10));
        assertFalse(intRange.isBefore(15));
        assertFalse(intRange.isBefore(20));
        assertTrue(intRange.isBefore(25));
    }

    @Test
    public void test_isBefore_null() {
        Range<Integer> range = Range.closed(5, 10);
        assertFalse(range.isBefore(null));
    }

    @Test
    public void testElementCompareTo() {
        try {
            intRange.positionOf(null);
            fail("NullPointerException should have been thrown");
        } catch (final IllegalArgumentException npe) {
        }

        assertEquals(1, intRange.positionOf(5));
        assertEquals(0, intRange.positionOf(10));
        assertEquals(0, intRange.positionOf(15));
        assertEquals(0, intRange.positionOf(20));
        assertEquals(-1, intRange.positionOf(25));
    }

    @Test
    public void test_containsRange_fullyContained() {
        Range<Integer> range1 = Range.closed(1, 10);
        Range<Integer> range2 = Range.closed(3, 7);
        assertTrue(range1.containsRange(range2));
    }

    @Test
    public void test_containsRange_extendsBelow() {
        Range<Integer> range1 = Range.closed(1, 10);
        Range<Integer> range2 = Range.closed(0, 5);
        assertFalse(range1.containsRange(range2));
    }

    @Test
    public void test_containsRange_extendsAbove() {
        Range<Integer> range1 = Range.closed(1, 10);
        Range<Integer> range2 = Range.closed(5, 15);
        assertFalse(range1.containsRange(range2));
    }

    @Test
    public void test_containsRange_openInClosed() {
        Range<Integer> range1 = Range.closed(1, 10);
        Range<Integer> range2 = Range.open(1, 10);
        assertTrue(range1.containsRange(range2));
    }

    @Test
    public void test_containsRange_closedInOpen() {
        Range<Integer> range1 = Range.open(1, 10);
        Range<Integer> range2 = Range.closed(1, 10);
        assertFalse(range1.containsRange(range2));
    }

    @Test
    public void test_containsRange_openLowerBound() {
        Range<Integer> range1 = Range.closed(1, 10);
        Range<Integer> range2 = Range.open(2, 8);
        assertTrue(range1.containsRange(range2));
    }

    @Test
    public void test_containsRange_openUpperBound() {
        Range<Integer> range1 = Range.closed(1, 10);
        Range<Integer> range2 = Range.closedOpen(3, 9);
        assertTrue(range1.containsRange(range2));
    }

    @Test
    public void testContainsRange() {

        assertFalse(intRange.containsRange(null));

        assertTrue(intRange.containsRange(Range.closed(12, 18)));

        assertFalse(intRange.containsRange(Range.closed(32, 45)));
        assertFalse(intRange.containsRange(Range.closed(2, 8)));

        assertTrue(intRange.containsRange(Range.closed(10, 20)));

        assertFalse(intRange.containsRange(Range.closed(9, 14)));
        assertFalse(intRange.containsRange(Range.closed(16, 21)));

        assertTrue(intRange.containsRange(Range.closed(10, 19)));
        assertFalse(intRange.containsRange(Range.closed(10, 21)));

        assertTrue(intRange.containsRange(Range.closed(11, 20)));
        assertFalse(intRange.containsRange(Range.closed(9, 20)));

    }

    @Test
    public void test_containsRange_null() {
        Range<Integer> range = Range.closed(1, 10);
        assertFalse(range.containsRange(null));
    }

    @Test
    public void test_containsRange_same() {
        Range<Integer> range1 = Range.closed(1, 10);
        Range<Integer> range2 = Range.closed(1, 10);
        assertTrue(range1.containsRange(range2));
    }

    @Test
    public void test_isAfterRange_rangeAfter() {
        Range<Integer> range1 = Range.closed(10, 15);
        Range<Integer> range2 = Range.closed(1, 5);
        assertTrue(range1.isAfterRange(range2));
    }

    @Test
    public void test_isAfterRange_rangeOverlaps() {
        Range<Integer> range1 = Range.closed(10, 15);
        Range<Integer> range2 = Range.closed(5, 12);
        assertFalse(range1.isAfterRange(range2));
    }

    @Test
    public void test_isAfterRange_touching() {
        Range<Integer> range1 = Range.closed(10, 15);
        Range<Integer> range2 = Range.closed(5, 10);
        assertFalse(range1.isAfterRange(range2));
    }

    @Test
    public void test_isAfterRange_touchingOpen() {
        Range<Integer> range1 = Range.closed(10, 15);
        Range<Integer> range2 = Range.closedOpen(5, 10);
        assertTrue(range1.isAfterRange(range2));
    }

    @Test
    public void testIsAfterRange() {
        assertFalse(intRange.isAfterRange(null));

        assertTrue(intRange.isAfterRange(Range.closed(5, 9)));

        assertFalse(intRange.isAfterRange(Range.closed(5, 10)));
        assertFalse(intRange.isAfterRange(Range.closed(5, 20)));
        assertFalse(intRange.isAfterRange(Range.closed(5, 25)));
        assertFalse(intRange.isAfterRange(Range.closed(15, 25)));

        assertFalse(intRange.isAfterRange(Range.closed(21, 25)));

        assertFalse(intRange.isAfterRange(Range.closed(10, 20)));
    }

    @Test
    public void test_isAfterRange_null() {
        Range<Integer> range = Range.closed(10, 15);
        assertFalse(range.isAfterRange(null));
    }

    @Test
    public void test_isBeforeRange_rangeBefore() {
        Range<Integer> range1 = Range.closed(1, 5);
        Range<Integer> range2 = Range.closed(10, 15);
        assertTrue(range1.isBeforeRange(range2));
    }

    @Test
    public void test_isBeforeRange_rangeOverlaps() {
        Range<Integer> range1 = Range.closed(1, 10);
        Range<Integer> range2 = Range.closed(5, 15);
        assertFalse(range1.isBeforeRange(range2));
    }

    @Test
    public void test_isBeforeRange_touching() {
        Range<Integer> range1 = Range.closed(1, 5);
        Range<Integer> range2 = Range.closed(5, 10);
        assertFalse(range1.isBeforeRange(range2));
    }

    @Test
    public void test_isBeforeRange_touchingOpen() {
        Range<Integer> range1 = Range.closedOpen(1, 5);
        Range<Integer> range2 = Range.closed(5, 10);
        assertTrue(range1.isBeforeRange(range2));
    }

    @Test
    public void testIsBeforeRange() {
        assertFalse(intRange.isBeforeRange(null));

        assertFalse(intRange.isBeforeRange(Range.closed(5, 9)));

        assertFalse(intRange.isBeforeRange(Range.closed(5, 10)));
        assertFalse(intRange.isBeforeRange(Range.closed(5, 20)));
        assertFalse(intRange.isBeforeRange(Range.closed(5, 25)));
        assertFalse(intRange.isBeforeRange(Range.closed(15, 25)));

        assertTrue(intRange.isBeforeRange(Range.closed(21, 25)));

        assertFalse(intRange.isBeforeRange(Range.closed(10, 20)));
    }

    @Test
    public void test_isBeforeRange_null() {
        Range<Integer> range = Range.closed(1, 5);
        assertFalse(range.isBeforeRange(null));
    }

    @Test
    public void test_isOverlappedBy_overlapping() {
        Range<Integer> range1 = Range.closed(1, 5);
        Range<Integer> range2 = Range.closed(3, 8);
        assertTrue(range1.isOverlappedBy(range2));
    }

    @Test
    public void test_isOverlappedBy_notOverlapping() {
        Range<Integer> range1 = Range.closed(1, 5);
        Range<Integer> range2 = Range.closed(6, 10);
        assertFalse(range1.isOverlappedBy(range2));
    }

    @Test
    public void test_isOverlappedBy_touching() {
        Range<Integer> range1 = Range.closed(1, 5);
        Range<Integer> range2 = Range.closed(5, 10);
        assertTrue(range1.isOverlappedBy(range2));
    }

    @Test
    public void test_isOverlappedBy_touchingOpenBounds() {
        Range<Integer> range1 = Range.closedOpen(1, 5);
        Range<Integer> range2 = Range.open(5, 10);
        assertFalse(range1.isOverlappedBy(range2));
    }

    @Test
    public void test_isOverlappedBy_fullyContained() {
        Range<Integer> range1 = Range.closed(1, 10);
        Range<Integer> range2 = Range.closed(3, 7);
        assertTrue(range1.isOverlappedBy(range2));
    }

    @Test
    public void testIsOverlappedBy() {

        assertFalse(intRange.isOverlappedBy(null));

        assertTrue(intRange.isOverlappedBy(Range.closed(12, 18)));

        assertFalse(intRange.isOverlappedBy(Range.closed(32, 45)));
        assertFalse(intRange.isOverlappedBy(Range.closed(2, 8)));

        assertTrue(intRange.isOverlappedBy(Range.closed(10, 20)));

        assertTrue(intRange.isOverlappedBy(Range.closed(9, 14)));
        assertTrue(intRange.isOverlappedBy(Range.closed(16, 21)));

        assertTrue(intRange.isOverlappedBy(Range.closed(10, 19)));
        assertTrue(intRange.isOverlappedBy(Range.closed(10, 21)));

        assertTrue(intRange.isOverlappedBy(Range.closed(11, 20)));
        assertTrue(intRange.isOverlappedBy(Range.closed(9, 20)));

    }

    @Test
    public void test_isOverlappedBy_null() {
        Range<Integer> range = Range.closed(1, 5);
        assertFalse(range.isOverlappedBy(null));
    }

    @Test
    public void test_intersection_overlapping() {
        Range<Integer> range1 = Range.closed(1, 5);
        Range<Integer> range2 = Range.closed(3, 8);
        Optional<Range<Integer>> result = range1.intersection(range2);
        assertTrue(result.isPresent());
        assertEquals(3, result.get().lowerEndpoint());
        assertEquals(5, result.get().upperEndpoint());
        assertEquals(Range.BoundType.CLOSED_CLOSED, result.get().boundType());
    }

    @Test
    public void test_intersection_notOverlapping() {
        Range<Integer> range1 = Range.closed(1, 5);
        Range<Integer> range2 = Range.closed(6, 10);
        Optional<Range<Integer>> result = range1.intersection(range2);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_intersection_equal() {
        Range<Integer> range1 = Range.closed(1, 5);
        Range<Integer> range2 = Range.closed(1, 5);
        Optional<Range<Integer>> result = range1.intersection(range2);
        assertTrue(result.isPresent());
        assertEquals(range1, result.get());
    }

    @Test
    public void test_intersection_openClosed() {
        Range<Integer> range1 = Range.open(1, 5);
        Range<Integer> range2 = Range.closed(1, 5);
        Optional<Range<Integer>> result = range1.intersection(range2);
        assertTrue(result.isPresent());
        assertEquals(Range.BoundType.OPEN_OPEN, result.get().boundType());
    }

    @Test
    public void test_intersection_closedOpen() {
        Range<Integer> range1 = Range.closed(1, 5);
        Range<Integer> range2 = Range.closedOpen(3, 8);
        Optional<Range<Integer>> result = range1.intersection(range2);
        assertTrue(result.isPresent());
        assertEquals(3, result.get().lowerEndpoint());
        assertEquals(5, result.get().upperEndpoint());
        assertEquals(Range.BoundType.CLOSED_CLOSED, result.get().boundType());
    }

    @Test
    public void test_intersection_bothOpen() {
        Range<Integer> range1 = Range.open(1, 8);
        Range<Integer> range2 = Range.open(3, 10);
        Optional<Range<Integer>> result = range1.intersection(range2);
        assertTrue(result.isPresent());
        assertEquals(Range.BoundType.OPEN_OPEN, result.get().boundType());
    }

    @Test
    public void testIntersection() {
        Range<Integer> range1 = Range.closed(1, 5);
        Range<Integer> range2 = Range.closed(3, 8);

        u.Optional<Range<Integer>> intersection = range1.intersection(range2);
        Assertions.assertTrue(intersection.isPresent());
        Range<Integer> result = intersection.get();
        Assertions.assertEquals(3, result.lowerEndpoint());
        Assertions.assertEquals(5, result.upperEndpoint());
        Assertions.assertEquals(Range.BoundType.CLOSED_CLOSED, result.boundType());

        Range<Integer> range3 = Range.closed(6, 10);
        u.Optional<Range<Integer>> noIntersection = range1.intersection(range3);
        Assertions.assertFalse(noIntersection.isPresent());

        u.Optional<Range<Integer>> sameRange = range1.intersection(range1);
        Assertions.assertTrue(sameRange.isPresent());
        Assertions.assertEquals(range1, sameRange.get());
    }

    @Test
    public void test_span() {
        N.println(Range.closed(1, 3).span(Range.open(5, 7)));
        assertTrue(Range.closedOpen(1, 7).equals(Range.closed(1, 3).span(Range.open(5, 7))));

        N.println(Range.closed(1, 7).span(Range.open(5, 7)));
        assertTrue(Range.closed(1, 7).equals(Range.closed(1, 7).span(Range.open(5, 7))));

        N.println(Range.closed(1, 5).span(Range.open(5, 7)));
        assertTrue(Range.closedOpen(1, 7).equals(Range.closed(1, 5).span(Range.open(5, 7))));
    }

    @Test
    public void test_span_disconnected() {
        Range<Integer> range1 = Range.closed(1, 3);
        Range<Integer> range2 = Range.closed(5, 7);
        Range<Integer> result = range1.span(range2);
        assertEquals(1, result.lowerEndpoint());
        assertEquals(7, result.upperEndpoint());
        assertEquals(Range.BoundType.CLOSED_CLOSED, result.boundType());
    }

    @Test
    public void test_span_overlapping() {
        Range<Integer> range1 = Range.closed(1, 5);
        Range<Integer> range2 = Range.closed(3, 7);
        Range<Integer> result = range1.span(range2);
        assertEquals(1, result.lowerEndpoint());
        assertEquals(7, result.upperEndpoint());
        assertEquals(Range.BoundType.CLOSED_CLOSED, result.boundType());
    }

    @Test
    public void test_span_withEarlierRangeAsArgument() {
        Range<Integer> range1 = Range.closed(1, 5);
        Range<Integer> range2 = Range.open(0, 3);
        Range<Integer> result = range1.span(range2);
        assertEquals(0, result.lowerEndpoint());
        assertEquals(5, result.upperEndpoint());
        assertEquals(Range.BoundType.OPEN_CLOSED, result.boundType());
    }

    @Test
    public void test_span_equal() {
        Range<Integer> range1 = Range.closed(1, 5);
        Range<Integer> range2 = Range.closed(1, 5);
        Range<Integer> result = range1.span(range2);
        assertEquals(range1, result);
    }

    @Test
    public void test_span_openBounds() {
        Range<Integer> range1 = Range.open(1, 3);
        Range<Integer> range2 = Range.open(5, 7);
        Range<Integer> result = range1.span(range2);
        assertEquals(Range.BoundType.OPEN_OPEN, result.boundType());
    }

    @Test
    public void test_span_mixedBounds() {
        Range<Integer> range1 = Range.open(1, 5);
        Range<Integer> range2 = Range.closed(3, 7);
        Range<Integer> result = range1.span(range2);
        assertEquals(Range.BoundType.OPEN_CLOSED, result.boundType());
    }

    @Test
    public void test_span_fullyContained() {
        Range<Integer> range1 = Range.closed(1, 10);
        Range<Integer> range2 = Range.closed(3, 7);
        Range<Integer> result = range1.span(range2);
        assertEquals(range1, result);
    }

    @Test
    public void testSpan() {
        Range<Integer> range1 = Range.closed(1, 3);
        Range<Integer> range2 = Range.closed(5, 7);

        Range<Integer> span = range1.span(range2);
        Assertions.assertEquals(1, span.lowerEndpoint());
        Assertions.assertEquals(7, span.upperEndpoint());
        Assertions.assertEquals(Range.BoundType.CLOSED_CLOSED, span.boundType());

        Range<Integer> range3 = Range.open(1, 3);
        Range<Integer> range4 = Range.open(5, 7);
        Range<Integer> span2 = range3.span(range4);
        Assertions.assertEquals(Range.BoundType.OPEN_OPEN, span2.boundType());
    }

    @Test
    public void test_isEmpty_emptyRange() {
        Range<Integer> range = Range.open(5, 5);
        assertTrue(range.isEmpty());
    }

    @Test
    public void test_isEmpty_pointRange() {
        Range<Integer> range = Range.closed(5, 5);
        assertFalse(range.isEmpty());
    }

    @Test
    public void test_isEmpty_normalRange() {
        Range<Integer> range = Range.closed(1, 5);
        assertFalse(range.isEmpty());
    }

    @Test
    public void test_isEmpty_openClosedSameValue() {
        Range<Integer> range = Range.openClosed(5, 5);
        assertFalse(range.isEmpty());
    }

    @Test
    public void test_isEmpty_closedOpenSameValue() {
        Range<Integer> range = Range.closedOpen(5, 5);
        assertFalse(range.isEmpty());
    }

    @Test
    public void testIsEmpty() {
        Range<Integer> emptyRange = Range.open(5, 5);
        Assertions.assertTrue(emptyRange.isEmpty());

        Range<Integer> pointRange = Range.closed(5, 5);
        Assertions.assertFalse(pointRange.isEmpty());

        Range<Integer> normalRange = Range.open(5, 6);
        Assertions.assertFalse(normalRange.isEmpty());
    }

    @Test
    public void test_equals_differentEndpoints() {
        Range<Integer> range1 = Range.closed(1, 5);
        Range<Integer> range2 = Range.closed(1, 10);
        assertFalse(range1.equals(range2));
    }

    @Test
    public void test_equals_differentBoundTypes() {
        Range<Integer> range1 = Range.closed(1, 5);
        Range<Integer> range2 = Range.open(1, 5);
        assertFalse(range1.equals(range2));
    }

    @Test
    public void test_equals_differentType() {
        Range<Integer> range = Range.closed(1, 5);
        assertFalse(range.equals("not a range"));
    }

    @Test
    public void testEqualsObject() {
        assertEquals(byteRange, byteRange);
        assertEquals(byteRange, byteRange2);
        assertEquals(byteRange2, byteRange2);
        assertTrue(byteRange.equals(byteRange));
        assertTrue(byteRange2.equals(byteRange2));
        assertTrue(byteRange3.equals(byteRange3));
        assertFalse(byteRange2.equals(byteRange3));
        assertFalse(byteRange2.equals(null));
        assertFalse(byteRange2.equals("Ni!"));
    }

    @Test
    public void test_equals_sameRange() {
        Range<Integer> range1 = Range.closed(1, 5);
        Range<Integer> range2 = Range.closed(1, 5);
        assertTrue(range1.equals(range2));
        assertTrue(range2.equals(range1));
    }

    @Test
    public void test_equals_null() {
        Range<Integer> range = Range.closed(1, 5);
        assertFalse(range.equals(null));
    }

    @Test
    public void test_equals_self() {
        Range<Integer> range = Range.closed(1, 5);
        assertTrue(range.equals(range));
    }

    @Test
    public void testEquals() {
        Range<Integer> range1 = Range.closed(1, 5);
        Range<Integer> range2 = Range.closed(1, 5);
        Range<Integer> range3 = Range.open(1, 5);
        Range<Integer> range4 = Range.closed(1, 6);

        Assertions.assertTrue(range1.equals(range1));
        Assertions.assertTrue(range1.equals(range2));
        Assertions.assertFalse(range1.equals(range3));
        Assertions.assertFalse(range1.equals(range4));
        Assertions.assertFalse(range1.equals(null));
        Assertions.assertFalse(range1.equals("not a range"));
    }

    @Test
    public void testHashCode() {
        assertEquals(byteRange.hashCode(), byteRange2.hashCode());
        assertFalse(byteRange.hashCode() == byteRange3.hashCode());

        assertEquals(intRange.hashCode(), intRange.hashCode());
        assertTrue(intRange.hashCode() != 0);
    }

    @Test
    public void test_hashCode_consistency() {
        Range<Integer> range = Range.closed(1, 5);
        int hash1 = range.hashCode();
        int hash2 = range.hashCode();
        assertEquals(hash1, hash2);
    }

    @Test
    public void test_hashCode_sameRange() {
        Range<Integer> range1 = Range.closed(1, 5);
        Range<Integer> range2 = Range.closed(1, 5);
        assertEquals(range1.hashCode(), range2.hashCode());
    }

    @Test
    public void test_hashCode_differentRanges() {
        Range<Integer> range1 = Range.closed(1, 5);
        Range<Integer> range2 = Range.closed(1, 10);
        assertNotNull(range1.hashCode());
        assertNotNull(range2.hashCode());
    }

    @Test
    public void test_toString_closed() {
        Range<Integer> range = Range.closed(1, 5);
        String str = range.toString();
        assertTrue(str.contains("1"));
        assertTrue(str.contains("5"));
        assertTrue(str.startsWith("["));
        assertTrue(str.endsWith("]"));
    }

    @Test
    public void test_toString_open() {
        Range<Integer> range = Range.open(1, 5);
        String str = range.toString();
        assertTrue(str.contains("1"));
        assertTrue(str.contains("5"));
        assertTrue(str.startsWith("("));
        assertTrue(str.endsWith(")"));
    }

    @Test
    public void test_toString_openClosed() {
        Range<Integer> range = Range.openClosed(1, 5);
        String str = range.toString();
        assertTrue(str.startsWith("("));
        assertTrue(str.endsWith("]"));
    }

    @Test
    public void test_toString_closedOpen() {
        Range<Integer> range = Range.closedOpen(1, 5);
        String str = range.toString();
        assertTrue(str.startsWith("["));
        assertTrue(str.endsWith(")"));
    }

    @Test
    public void testToString() {
        assertNotNull(byteRange.toString());

        final String str = intRange.toString();
        assertEquals("[10, 20]", str);
        assertEquals("[-20, -10]", Range.closed(-20, -10).toString());
    }

}
