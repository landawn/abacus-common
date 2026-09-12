package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Optional;

@SuppressWarnings("deprecation")
public class RangeTest extends TestBase {

    @Test
    public void testFactories() {
        Range<Integer> just = Range.just(5);
        assertEquals(5, just.lowerEndpoint());
        assertEquals(5, just.upperEndpoint());
        assertTrue(just.contains(5));
        assertFalse(just.contains(4));
        assertEquals(Range.BoundType.CLOSED_CLOSED, just.boundType());
        assertThrows(IllegalArgumentException.class, () -> Range.just(null));

        Range<Integer> open = Range.open(1, 5);
        assertFalse(open.contains(1));
        assertTrue(open.contains(3));
        assertFalse(open.contains(5));
        assertEquals(Range.BoundType.OPEN_OPEN, open.boundType());
        assertTrue(Range.open(5, 5).isEmpty());

        Range<Integer> openClosed = Range.openClosed(1, 5);
        assertFalse(openClosed.contains(1));
        assertTrue(openClosed.contains(5));
        assertEquals(Range.BoundType.OPEN_CLOSED, openClosed.boundType());

        Range<Integer> closedOpen = Range.closedOpen(1, 5);
        assertTrue(closedOpen.contains(1));
        assertFalse(closedOpen.contains(5));
        assertEquals(Range.BoundType.CLOSED_OPEN, closedOpen.boundType());

        Range<Integer> closed = Range.closed(1, 5);
        assertTrue(closed.contains(1));
        assertTrue(closed.contains(5));
        assertFalse(closed.contains(0));
        assertEquals(Range.BoundType.CLOSED_CLOSED, closed.boundType());
        assertFalse(Range.closed(5, 5).isEmpty());

        assertTrue(Range.closed('a', 'z').contains('b'));
        assertFalse(Range.closed('a', 'z').contains('B'));
        assertTrue(Range.closed("apple", "banana").contains("apricot"));
        assertFalse(Range.closed("apple", "banana").contains("cherry"));
        assertTrue(Range.closed(Long.MAX_VALUE - 100, Long.MAX_VALUE).contains(Long.MAX_VALUE));

        assertThrows(IllegalArgumentException.class, () -> Range.open(null, 5));
        assertThrows(IllegalArgumentException.class, () -> Range.open(1, null));
        assertThrows(IllegalArgumentException.class, () -> Range.open(10, 5));
        assertThrows(IllegalArgumentException.class, () -> Range.openClosed(null, 5));
        assertThrows(IllegalArgumentException.class, () -> Range.openClosed(1, null));
        assertThrows(IllegalArgumentException.class, () -> Range.openClosed(10, 5));
        assertThrows(IllegalArgumentException.class, () -> Range.closedOpen(null, 5));
        assertThrows(IllegalArgumentException.class, () -> Range.closedOpen(1, null));
        assertThrows(IllegalArgumentException.class, () -> Range.closedOpen(10, 5));
        assertThrows(IllegalArgumentException.class, () -> Range.closed(null, 5));
        assertThrows(IllegalArgumentException.class, () -> Range.closed(1, null));
        assertThrows(IllegalArgumentException.class, () -> Range.closed(10, 5));
    }

    @Test
    public void testMapEndpoints() {
        Range<String> mapped = Range.closed(1, 5).mapEndpoints(String::valueOf);
        assertEquals("1", mapped.lowerEndpoint());
        assertEquals("5", mapped.upperEndpoint());
        assertEquals(Range.BoundType.CLOSED_CLOSED, mapped.boundType());
        assertTrue(mapped.contains("3"));

        assertEquals(Range.BoundType.OPEN_OPEN, Range.open(1, 5).map(String::valueOf).boundType());
        assertFalse(Range.openClosed(1, 5).map(Integer::longValue).contains(1L));
        assertTrue(Range.openClosed(1, 5).map(Integer::longValue).contains(5L));
        assertTrue(Range.closedOpen(1, 5).map(Integer::longValue).contains(1L));
        assertFalse(Range.closedOpen(1, 5).map(Integer::longValue).contains(5L));
        assertThrows(IllegalArgumentException.class, () -> Range.closed(1, 2).map(v -> v == 1 ? new NullTolerantComparable(v) : null));
    }

    @Test
    public void testContains() {
        Range<Integer> range = Range.closed(10, 20);
        assertFalse(range.contains(null));
        assertFalse(range.contains(5));
        assertTrue(range.contains(10));
        assertTrue(range.contains(15));
        assertTrue(range.contains(20));
        assertFalse(range.contains(25));
        assertFalse(Range.open(1, 10).contains(1));
        assertFalse(Range.open(1, 10).contains(10));
        assertTrue(Range.openClosed(1, 10).contains(10));
        assertFalse(Range.closedOpen(1, 10).contains(10));

        assertTrue(range.containsAll(Arrays.asList(12, 15, 18)));
        assertFalse(range.containsAll(Arrays.asList(12, 25)));
        assertTrue(range.containsAll(Collections.emptyList()));
        assertTrue(range.containsAll(null));
        assertFalse(range.containsAll(Arrays.asList(12, null)));
        assertTrue(range.containsAll(Arrays.asList(10, 20)));

        assertTrue(range.containsAny(Arrays.asList(3, 15, 30)));
        assertTrue(range.containsAny(Arrays.asList(10, 15)));
        assertFalse(range.containsAny(Arrays.asList(1, 2)));
        assertFalse(range.containsAny(Collections.emptyList()));
        assertFalse(range.containsAny(null));
        assertTrue(Range.open(1, 10).containsAny(Arrays.asList(1, 5)));
        assertFalse(Range.open(1, 10).containsAny(Arrays.asList(1, 10)));
    }

    @Test
    public void testPositionAndSides() {
        Range<Integer> closed = Range.closed(10, 20);
        assertTrue(closed.isStartedBy(10));
        assertFalse(closed.isStartedBy(11));
        assertFalse(Range.open(10, 20).isStartedBy(10));
        assertFalse(closed.isStartedBy(null));
        assertTrue(closed.isEndedBy(20));
        assertFalse(closed.isEndedBy(19));
        assertFalse(Range.open(10, 20).isEndedBy(20));
        assertFalse(closed.isEndedBy(null));

        assertTrue(closed.isAfter(5));
        assertFalse(closed.isAfter(10));
        assertTrue(Range.open(10, 20).isAfter(10));
        assertFalse(closed.isAfter(null));
        assertTrue(closed.isBefore(25));
        assertFalse(closed.isBefore(20));
        assertFalse(closed.isBefore(null));

        assertEquals(1, closed.positionOf(5));
        assertEquals(0, closed.positionOf(10));
        assertEquals(0, closed.positionOf(15));
        assertEquals(0, closed.positionOf(20));
        assertEquals(-1, closed.positionOf(25));
        assertThrows(IllegalArgumentException.class, () -> closed.positionOf(null));
        assertEquals(0, closed.elementCompareTo(15));
        assertEquals(-1, closed.elementCompareTo(5));
        assertEquals(1, closed.elementCompareTo(25));
        assertThrows(IllegalStateException.class, () -> Range.open(5, 5).elementCompareTo(5));
    }

    @Test
    public void testContainsRangeAndOverlap() {
        Range<Integer> range = Range.closed(10, 20);
        assertTrue(range.containsRange(Range.closed(12, 18)));
        assertTrue(range.containsRange(Range.closed(10, 20)));
        assertTrue(range.containsRange(Range.open(10, 20)));
        assertFalse(range.containsRange(Range.closed(9, 14)));
        assertFalse(range.containsRange(Range.closed(16, 21)));
        assertFalse(Range.open(1, 10).containsRange(Range.closed(1, 10)));
        assertFalse(range.containsRange(null));
        assertTrue(range.containsRange(Range.open(-5, -5)));
        assertFalse(Range.open(5, 5).containsRange(Range.closed(5, 5)));

        assertTrue(range.isAfterRange(Range.closed(5, 9)));
        assertFalse(range.isAfterRange(Range.closed(5, 10)));
        assertTrue(Range.closed(10, 15).isAfterRange(Range.closedOpen(5, 10)));
        assertFalse(range.isAfterRange(null));
        assertTrue(range.isBeforeRange(Range.closed(21, 25)));
        assertFalse(range.isBeforeRange(Range.closed(20, 25)));
        assertTrue(Range.closedOpen(1, 5).isBeforeRange(Range.closed(5, 10)));
        assertFalse(range.isBeforeRange(null));

        assertTrue(range.overlaps(Range.closed(12, 18)));
        assertTrue(range.overlaps(Range.closed(9, 14)));
        assertTrue(Range.closed(1, 5).overlaps(Range.closed(5, 10)));
        assertFalse(Range.closedOpen(1, 5).overlaps(Range.open(5, 10)));
        assertFalse(range.overlaps(Range.closed(32, 45)));
        assertFalse(range.overlaps(null));
        assertFalse(Range.open(1, 1).overlaps(range));
        assertTrue(range.isOverlappedBy(Range.closed(12, 18)));
        assertFalse(range.isOverlappedBy(null));
    }

    @Test
    public void testIntersectionAndSpan() {
        Range<Integer> a = Range.closed(1, 5);
        Optional<Range<Integer>> overlap = a.intersection(Range.closed(3, 8));
        assertEquals(3, overlap.get().lowerEndpoint());
        assertEquals(5, overlap.get().upperEndpoint());
        assertEquals(Range.BoundType.CLOSED_CLOSED, overlap.get().boundType());
        assertFalse(a.intersection(Range.closed(6, 10)).isPresent());
        assertEquals(a, a.intersection(a).get());
        assertEquals(Range.BoundType.OPEN_OPEN, Range.open(1, 5).intersection(Range.closed(1, 5)).get().boundType());
        assertEquals(Range.BoundType.CLOSED_CLOSED, Range.closed(1, 5).intersection(Range.closedOpen(3, 8)).get().boundType());

        Range<Integer> empty = Range.open(2, 2);
        assertTrue(empty.isEmpty());
        assertFalse(empty.overlaps(Range.closed(1, 3)));
        assertFalse(empty.intersection(Range.closed(1, 3)).isPresent());

        assertEquals(Range.closedOpen(1, 7), Range.closed(1, 3).span(Range.open(5, 7)));
        assertEquals(Range.closed(1, 7), Range.closed(1, 7).span(Range.open(5, 7)));
        Range<Integer> span = Range.closed(1, 5).span(Range.open(0, 3));
        assertEquals(0, span.lowerEndpoint());
        assertEquals(5, span.upperEndpoint());
        assertEquals(Range.BoundType.OPEN_CLOSED, span.boundType());
        assertEquals(Range.BoundType.OPEN_OPEN, Range.open(1, 3).span(Range.open(5, 7)).boundType());
        assertEquals(Range.closed(1, 10), Range.closed(1, 10).span(Range.closed(3, 7)));
    }

    @Test
    public void testIsEmptyEqualsHashCodeToString() {
        assertTrue(Range.open(5, 5).isEmpty());
        assertTrue(Range.openClosed(5, 5).isEmpty());
        assertTrue(Range.closedOpen(5, 5).isEmpty());
        assertFalse(Range.closed(5, 5).isEmpty());
        assertFalse(Range.open(5, 6).isEmpty());
        assertFalse(Range.openClosed(5, 5).contains(5));

        Range<Integer> a = Range.closed(1, 5);
        Range<Integer> b = Range.closed(1, 5);
        assertEquals(a, a);
        assertEquals(a, b);
        assertNotEquals(a, Range.open(1, 5));
        assertNotEquals(a, Range.closed(1, 6));
        assertNotEquals(a, null);
        assertNotEquals(a, "not a range");
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals("[1, 5]", a.toString());
        assertEquals("(1, 5)", Range.open(1, 5).toString());
        assertEquals("(1, 5]", Range.openClosed(1, 5).toString());
        assertEquals("[1, 5)", Range.closedOpen(1, 5).toString());
        assertEquals("[-20, -10]", Range.closed(-20, -10).toString());
    }

    private static final class NullTolerantComparable implements Comparable<NullTolerantComparable> {
        private final int value;

        NullTolerantComparable(final int value) {
            this.value = value;
        }

        @Override
        public int compareTo(final NullTolerantComparable other) {
            return other == null ? -1 : Integer.compare(value, other.value);
        }
    }
}
