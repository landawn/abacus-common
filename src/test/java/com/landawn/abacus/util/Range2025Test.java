package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Optional;

@SuppressWarnings("boxing")
@Tag("2025")
public class Range2025Test extends TestBase {

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
    public void test_open_equalEndpoints() {
        Range<Integer> range = Range.open(5, 5);
        assertNotNull(range);
        assertTrue(range.isEmpty());
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
    public void test_closed_equalEndpoints() {
        Range<Integer> range = Range.closed(5, 5);
        assertNotNull(range);
        assertFalse(range.isEmpty());
        assertTrue(range.contains(5));
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
    public void test_contains_null() {
        Range<Integer> range = Range.closed(1, 10);
        assertFalse(range.contains(null));
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
    public void test_isAfter_null() {
        Range<Integer> range = Range.closed(5, 10);
        assertFalse(range.isAfter(null));
    }

    @Test
    public void test_isAfter_openLowerBound() {
        Range<Integer> range = Range.open(5, 10);
        assertTrue(range.isAfter(5));
        assertFalse(range.isAfter(7));
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
    public void test_isBefore_null() {
        Range<Integer> range = Range.closed(5, 10);
        assertFalse(range.isBefore(null));
    }

    @Test
    public void test_isBefore_openUpperBound() {
        Range<Integer> range = Range.closed(5, 10);
        assertFalse(range.isBefore(10));
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
    public void test_compareTo_null() {
        Range<Integer> range = Range.closed(5, 10);
        assertThrows(IllegalArgumentException.class, () -> range.positionOf(null));
    }

    @Test
    public void test_compareTo_atBoundaries() {
        Range<Integer> range = Range.closed(5, 10);
        assertEquals(0, range.positionOf(5));
        assertEquals(0, range.positionOf(10));
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
    public void test_isAfterRange_null() {
        Range<Integer> range = Range.closed(10, 15);
        assertFalse(range.isAfterRange(null));
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
    public void test_isBeforeRange_null() {
        Range<Integer> range = Range.closed(1, 5);
        assertFalse(range.isBeforeRange(null));
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
    public void test_isOverlappedBy_null() {
        Range<Integer> range = Range.closed(1, 5);
        assertFalse(range.isOverlappedBy(null));
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
    public void test_equals_sameRange() {
        Range<Integer> range1 = Range.closed(1, 5);
        Range<Integer> range2 = Range.closed(1, 5);
        assertTrue(range1.equals(range2));
        assertTrue(range2.equals(range1));
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
    public void test_equals_null() {
        Range<Integer> range = Range.closed(1, 5);
        assertFalse(range.equals(null));
    }

    @Test
    public void test_equals_differentType() {
        Range<Integer> range = Range.closed(1, 5);
        assertFalse(range.equals("not a range"));
    }

    @Test
    public void test_equals_self() {
        Range<Integer> range = Range.closed(1, 5);
        assertTrue(range.equals(range));
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
    public void test_hashCode_consistency() {
        Range<Integer> range = Range.closed(1, 5);
        int hash1 = range.hashCode();
        int hash2 = range.hashCode();
        assertEquals(hash1, hash2);
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
}
