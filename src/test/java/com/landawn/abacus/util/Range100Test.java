package com.landawn.abacus.util;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class Range100Test extends TestBase {

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
    public void testOpenClosed() {
        Range<Integer> range = Range.openClosed(1, 5);
        Assertions.assertFalse(range.contains(1));
        Assertions.assertTrue(range.contains(2));
        Assertions.assertTrue(range.contains(5));
        Assertions.assertEquals(Range.BoundType.OPEN_CLOSED, range.boundType());
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
    public void testInvalidRanges() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Range.open(null, 5));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Range.open(1, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Range.open(null, null));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Range.open(5, 1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Range.closed(10, 5));
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
    public void testContains() {
        Range<Integer> range = Range.closed(10, 20);

        Assertions.assertFalse(range.contains(null));

        Assertions.assertTrue(range.contains(10));
        Assertions.assertTrue(range.contains(15));
        Assertions.assertTrue(range.contains(20));
        Assertions.assertFalse(range.contains(9));
        Assertions.assertFalse(range.contains(21));
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
    public void testIsStartedBy() {
        Range<Integer> closedRange = Range.closed(5, 10);
        Range<Integer> openRange = Range.open(5, 10);

        Assertions.assertTrue(closedRange.isStartedBy(5));
        Assertions.assertFalse(openRange.isStartedBy(5));
        Assertions.assertFalse(closedRange.isStartedBy(6));
        Assertions.assertFalse(closedRange.isStartedBy(null));
    }

    @Test
    public void testIsEndedBy() {
        Range<Integer> closedRange = Range.closed(5, 10);
        Range<Integer> openClosedRange = Range.openClosed(5, 10);

        Assertions.assertTrue(closedRange.isEndedBy(10));
        Assertions.assertTrue(openClosedRange.isEndedBy(10));
        Assertions.assertFalse(closedRange.isEndedBy(9));
        Assertions.assertFalse(closedRange.isEndedBy(null));
    }

    @Test
    public void testIsAfter() {
        Range<Integer> range = Range.closed(5, 10);

        Assertions.assertTrue(range.isAfter(3));
        Assertions.assertTrue(range.isAfter(4));
        Assertions.assertFalse(range.isAfter(5));
        Assertions.assertFalse(range.isAfter(7));
        Assertions.assertFalse(range.isAfter(null));
    }

    @Test
    public void testIsBefore() {
        Range<Integer> range = Range.closed(5, 10);

        Assertions.assertTrue(range.isBefore(12));
        Assertions.assertTrue(range.isBefore(11));
        Assertions.assertFalse(range.isBefore(10));
        Assertions.assertFalse(range.isBefore(7));
        Assertions.assertFalse(range.isBefore(null));
    }

    @Test
    public void testCompareTo() {
        Range<Integer> range = Range.closed(5, 10);

        Assertions.assertEquals(1, range.compareTo(3));
        Assertions.assertEquals(0, range.compareTo(7));
        Assertions.assertEquals(-1, range.compareTo(12));

        Assertions.assertThrows(IllegalArgumentException.class, () -> range.compareTo(null));
    }

    @Test
    public void testContainsRange() {
        Range<Integer> outer = Range.closed(1, 10);
        Range<Integer> inner = Range.closed(3, 7);
        Range<Integer> partial = Range.closed(5, 15);
        Range<Integer> disjoint = Range.closed(15, 20);

        Assertions.assertTrue(outer.containsRange(inner));
        Assertions.assertFalse(outer.containsRange(partial));
        Assertions.assertFalse(outer.containsRange(disjoint));
        Assertions.assertFalse(outer.containsRange(null));

        Range<Integer> openInner = Range.open(3, 7);
        Assertions.assertTrue(outer.containsRange(openInner));
    }

    @Test
    public void testIsAfterRange() {
        Range<Integer> range1 = Range.closed(10, 15);
        Range<Integer> range2 = Range.closed(1, 5);
        Range<Integer> range3 = Range.closed(1, 10);

        Assertions.assertTrue(range1.isAfterRange(range2));
        Assertions.assertFalse(range1.isAfterRange(range3));
        Assertions.assertFalse(range1.isAfterRange(null));
    }

    @Test
    public void testIsBeforeRange() {
        Range<Integer> range1 = Range.closed(1, 5);
        Range<Integer> range2 = Range.closed(10, 15);
        Range<Integer> range3 = Range.closed(5, 10);

        Assertions.assertTrue(range1.isBeforeRange(range2));
        Assertions.assertFalse(range1.isBeforeRange(range3));
        Assertions.assertFalse(range1.isBeforeRange(null));
    }

    @Test
    public void testIsOverlappedBy() {
        Range<Integer> range1 = Range.closed(1, 5);
        Range<Integer> range2 = Range.closed(3, 8);
        Range<Integer> range3 = Range.closed(6, 10);
        Range<Integer> range4 = Range.open(5, 10);

        Assertions.assertTrue(range1.isOverlappedBy(range2));
        Assertions.assertFalse(range1.isOverlappedBy(range3));
        Assertions.assertFalse(range1.isOverlappedBy(range4));
        Assertions.assertFalse(range1.isOverlappedBy(null));
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
    public void testIsEmpty() {
        Range<Integer> emptyRange = Range.open(5, 5);
        Assertions.assertTrue(emptyRange.isEmpty());

        Range<Integer> pointRange = Range.closed(5, 5);
        Assertions.assertFalse(pointRange.isEmpty());

        Range<Integer> normalRange = Range.open(5, 6);
        Assertions.assertFalse(normalRange.isEmpty());
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
        Range<Integer> range1 = Range.closed(1, 5);
        Range<Integer> range2 = Range.closed(1, 5);
        Range<Integer> range3 = Range.open(1, 5);

        Assertions.assertEquals(range1.hashCode(), range2.hashCode());
        Assertions.assertNotEquals(range1.hashCode(), range3.hashCode());
    }

    @Test
    public void testToString() {
        Range<Integer> closedRange = Range.closed(1, 5);
        Assertions.assertEquals("[1, 5]", closedRange.toString());

        Range<Integer> openRange = Range.open(1, 5);
        Assertions.assertEquals("(1, 5)", openRange.toString());

        Range<Integer> openClosedRange = Range.openClosed(1, 5);
        Assertions.assertEquals("(1, 5]", openClosedRange.toString());

        Range<Integer> closedOpenRange = Range.closedOpen(1, 5);
        Assertions.assertEquals("[1, 5)", closedOpenRange.toString());
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
}
