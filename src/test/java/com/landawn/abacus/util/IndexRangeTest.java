package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.util.u.Optional;

@SuppressWarnings("boxing")
public class IndexRangeTest extends AbstractTest {

    @Test
    public void testConstructor() {
        new IndexRange(0, 0);
        new IndexRange(0, 5);
        new IndexRange(5, 5);

        assertThrows(IllegalArgumentException.class, () -> new IndexRange(-1, 3));
        assertThrows(IllegalArgumentException.class, () -> new IndexRange(4, 2));
    }

    @Test
    public void test_isEmpty_emptyRange() {
        assertTrue(new IndexRange(5, 5).isEmpty());
    }

    @Test
    public void test_isEmpty_nonEmptyRange() {
        assertFalse(new IndexRange(0, 5).isEmpty());
        assertFalse(new IndexRange(5, 6).isEmpty());
        assertFalse(new IndexRange(1, 10).isEmpty());
    }

    @Test
    public void test_containsRange_fullyContained() {
        IndexRange range1 = new IndexRange(1, 10);
        IndexRange range2 = new IndexRange(3, 7);
        assertTrue(range1.containsRange(range2));
    }

    @Test
    public void test_containsRange_extendsBelow() {
        IndexRange range1 = new IndexRange(1, 10);
        IndexRange range2 = new IndexRange(0, 5);
        assertFalse(range1.containsRange(range2));
    }

    @Test
    public void test_containsRange_extendsAbove() {
        IndexRange range1 = new IndexRange(1, 10);
        IndexRange range2 = new IndexRange(5, 15);
        assertFalse(range1.containsRange(range2));
    }

    @Test
    public void test_containsRange_touching() {
        IndexRange range1 = new IndexRange(1, 10);
        assertTrue(range1.containsRange(new IndexRange(1, 10)));
        assertTrue(range1.containsRange(new IndexRange(1, 5)));
        assertTrue(range1.containsRange(new IndexRange(5, 10)));
        assertFalse(range1.containsRange(new IndexRange(0, 10)));
        assertFalse(range1.containsRange(new IndexRange(1, 11)));
    }

    @Test
    public void test_containsRange_emptyRange() {
        IndexRange range = new IndexRange(1, 10);

        assertTrue(range.containsRange(new IndexRange(0, 0)));
        assertTrue(range.containsRange(new IndexRange(5, 5)));
        assertTrue(range.containsRange(new IndexRange(20, 20)));
        assertTrue(new IndexRange(5, 5).containsRange(new IndexRange(0, 0)));
    }

    @Test
    public void test_containsRange_same() {
        IndexRange range1 = new IndexRange(1, 10);
        IndexRange range2 = new IndexRange(1, 10);
        assertTrue(range1.containsRange(range2));
    }

    @Test
    public void test_containsRange_null() {
        IndexRange range = new IndexRange(1, 10);
        assertFalse(range.containsRange(null));
    }

    @Test
    public void test_isAfterRange_rangeAfter() {
        IndexRange range1 = new IndexRange(10, 15);
        IndexRange range2 = new IndexRange(1, 5);
        assertTrue(range1.isAfterRange(range2));
    }

    @Test
    public void test_isAfterRange_rangeOverlaps() {
        IndexRange range1 = new IndexRange(10, 15);
        IndexRange range2 = new IndexRange(5, 12);
        assertFalse(range1.isAfterRange(range2));
    }

    @Test
    public void test_isAfterRange_touching() {
        IndexRange range1 = new IndexRange(10, 15);
        IndexRange range2 = new IndexRange(5, 10);
        assertTrue(range1.isAfterRange(range2));
    }

    @Test
    public void test_isAfterRange_null() {
        IndexRange range = new IndexRange(10, 15);
        assertFalse(range.isAfterRange(null));
    }

    @Test
    public void test_isBeforeRange_rangeBefore() {
        IndexRange range1 = new IndexRange(1, 5);
        IndexRange range2 = new IndexRange(10, 15);
        assertTrue(range1.isBeforeRange(range2));
    }

    @Test
    public void test_isBeforeRange_rangeOverlaps() {
        IndexRange range1 = new IndexRange(1, 10);
        IndexRange range2 = new IndexRange(5, 15);
        assertFalse(range1.isBeforeRange(range2));
    }

    @Test
    public void test_isBeforeRange_touching() {
        IndexRange range1 = new IndexRange(1, 5);
        IndexRange range2 = new IndexRange(5, 10);
        assertTrue(range1.isBeforeRange(range2));
    }

    @Test
    public void test_isBeforeRange_null() {
        IndexRange range = new IndexRange(1, 5);
        assertFalse(range.isBeforeRange(null));
    }

    @Test
    public void test_overlaps_overlapping() {
        IndexRange range1 = new IndexRange(1, 5);
        IndexRange range2 = new IndexRange(3, 8);
        assertTrue(range1.overlaps(range2));
        assertTrue(range1.overlaps(range1));
    }

    @Test
    public void test_overlaps_notOverlapping() {
        IndexRange range1 = new IndexRange(1, 5);
        IndexRange range2 = new IndexRange(6, 10);
        assertFalse(range1.overlaps(range2));
    }

    @Test
    public void test_overlaps_touching() {
        IndexRange range1 = new IndexRange(1, 5);
        IndexRange range2 = new IndexRange(5, 10);
        assertFalse(range1.overlaps(range2));
    }

    @Test
    public void test_overlaps_fullyContained() {
        IndexRange range1 = new IndexRange(1, 10);
        IndexRange range2 = new IndexRange(3, 7);
        assertTrue(range1.overlaps(range2));
    }

    @Test
    public void test_overlaps_null() {
        IndexRange range = new IndexRange(1, 5);
        assertFalse(range.overlaps(null));
    }

    @Test
    public void test_overlaps_emptyRanges() {
        IndexRange range = new IndexRange(1, 5);
        assertFalse(range.overlaps(new IndexRange(3, 3)));
        assertFalse(new IndexRange(3, 3).overlaps(range));
        assertFalse(new IndexRange(3, 3).overlaps(new IndexRange(3, 3)));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testIsOverlappedByDeprecatedAlias() {
        IndexRange range = new IndexRange(1, 5);
        assertTrue(range.isOverlappedBy(new IndexRange(3, 8)));
        assertFalse(range.isOverlappedBy(new IndexRange(5, 10)));
        assertFalse(range.isOverlappedBy(null));
    }

    @Test
    public void test_intersection_overlapping() {
        IndexRange range1 = new IndexRange(1, 5);
        IndexRange range2 = new IndexRange(3, 8);
        Optional<IndexRange> result = range1.intersection(range2);
        assertTrue(result.isPresent());
        assertEquals(new IndexRange(3, 5), result.get());
    }

    @Test
    public void test_intersection_notOverlapping() {
        IndexRange range1 = new IndexRange(1, 5);
        IndexRange range2 = new IndexRange(6, 10);
        Optional<IndexRange> result = range1.intersection(range2);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_intersection_touching() {
        IndexRange range1 = new IndexRange(1, 5);
        IndexRange range2 = new IndexRange(5, 10);
        Optional<IndexRange> result = range1.intersection(range2);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_intersection_equal() {
        IndexRange range1 = new IndexRange(1, 5);
        IndexRange range2 = new IndexRange(1, 5);
        Optional<IndexRange> result = range1.intersection(range2);
        assertTrue(result.isPresent());
        assertEquals(range1, result.get());
    }

    @Test
    public void test_intersection_fullyContained() {
        IndexRange range1 = new IndexRange(1, 10);
        IndexRange range2 = new IndexRange(3, 7);
        Optional<IndexRange> result = range1.intersection(range2);
        assertTrue(result.isPresent());
        assertEquals(range2, result.get());
    }

    @Test
    public void test_intersection_nullAndEmpty() {
        IndexRange range = new IndexRange(1, 5);
        assertFalse(range.intersection(null).isPresent());
        assertFalse(range.intersection(new IndexRange(3, 3)).isPresent());
    }

    @Test
    public void test_span_disconnected() {
        IndexRange range1 = new IndexRange(1, 3);
        IndexRange range2 = new IndexRange(5, 7);
        IndexRange result = range1.span(range2);
        assertEquals(new IndexRange(1, 7), result);
    }

    @Test
    public void test_span_overlapping() {
        IndexRange range1 = new IndexRange(1, 5);
        IndexRange range2 = new IndexRange(3, 7);
        IndexRange result = range1.span(range2);
        assertEquals(new IndexRange(1, 7), result);
    }

    @Test
    public void test_span_equal() {
        IndexRange range1 = new IndexRange(1, 5);
        IndexRange range2 = new IndexRange(1, 5);
        IndexRange result = range1.span(range2);
        assertEquals(range1, result);
    }

    @Test
    public void test_span_touching() {
        IndexRange range1 = new IndexRange(1, 5);
        IndexRange range2 = new IndexRange(5, 10);
        IndexRange result = range1.span(range2);
        assertEquals(new IndexRange(1, 10), result);
    }

    @Test
    public void test_span_withEarlierRangeAsArgument() {
        IndexRange range1 = new IndexRange(1, 5);
        IndexRange range2 = new IndexRange(0, 3);
        IndexRange result = range1.span(range2);
        assertEquals(new IndexRange(0, 5), result);
    }

    @Test
    public void test_span_commutative() {
        IndexRange range1 = new IndexRange(1, 3);
        IndexRange range2 = new IndexRange(5, 7);
        assertEquals(range1.span(range2), range2.span(range1));
    }

    @Test
    public void test_span_null() {
        IndexRange range = new IndexRange(1, 5);
        assertThrows(IllegalArgumentException.class, () -> range.span(null));
    }

    /**
     * Empty ranges contribute no indices: span must not enlarge a non-empty range with an empty one,
     * and spanning two empties must remain empty (not invent a non-empty interval).
     */
    @Test
    public void test_span_emptyRangesDoNotEnlarge() {
        final IndexRange nonEmpty = new IndexRange(1, 3);
        final IndexRange emptyAt5 = new IndexRange(5, 5);
        final IndexRange emptyAt3 = new IndexRange(3, 3);

        assertEquals(nonEmpty, nonEmpty.span(emptyAt5));
        assertEquals(nonEmpty, emptyAt5.span(nonEmpty));
        assertEquals(nonEmpty, nonEmpty.span(emptyAt3));

        final IndexRange bothEmpty = emptyAt5.span(emptyAt3);
        assertTrue(bothEmpty.isEmpty(), "span of two empties must stay empty, got " + bothEmpty);
        // Deterministic: prefer smaller start when both empty
        assertEquals(emptyAt3, emptyAt5.span(emptyAt3));
        assertEquals(emptyAt3, emptyAt3.span(emptyAt5));
    }

    @Test
    public void test_toRange() {
        assertEquals(Range.closedOpen(1, 5), new IndexRange(1, 5).toRange());
        assertEquals(Range.closedOpen(5, 5), new IndexRange(5, 5).toRange());
    }

    @Test
    public void test_of() {
        assertEquals(new IndexRange(2, 5), IndexRange.of(2, 5));
        assertEquals(new IndexRange(0, 0), IndexRange.of(0, 0));
        assertThrows(IllegalArgumentException.class, () -> IndexRange.of(-1, 3));
        assertThrows(IllegalArgumentException.class, () -> IndexRange.of(4, 2));
    }

    @Test
    public void test_contains_withinRange() {
        IndexRange range = new IndexRange(2, 5);
        assertTrue(range.contains(2));
        assertTrue(range.contains(3));
        assertTrue(range.contains(4));
    }

    @Test
    public void test_contains_outsideRange() {
        IndexRange range = new IndexRange(2, 5);
        assertFalse(range.contains(1));
        assertFalse(range.contains(5));
        assertFalse(range.contains(0));
        assertFalse(range.contains(-1));
    }

    @Test
    public void test_contains_emptyRange() {
        IndexRange range = new IndexRange(5, 5);
        assertFalse(range.contains(5));
    }

    @Test
    public void test_isAfter_elementBefore() {
        IndexRange range = new IndexRange(2, 5);
        assertTrue(range.isAfter(1));
        assertTrue(range.isAfter(0));
        assertTrue(range.isAfter(-1));
    }

    @Test
    public void test_isAfter_elementAtStart() {
        IndexRange range = new IndexRange(2, 5);
        assertFalse(range.isAfter(2));
    }

    @Test
    public void test_isAfter_elementWithin() {
        IndexRange range = new IndexRange(2, 5);
        assertFalse(range.isAfter(3));
    }

    @Test
    public void test_isBefore_elementAfter() {
        IndexRange range = new IndexRange(2, 5);
        assertTrue(range.isBefore(5));
        assertTrue(range.isBefore(6));
    }

    @Test
    public void test_isBefore_elementAtEndMinusOne() {
        IndexRange range = new IndexRange(2, 5);
        assertFalse(range.isBefore(4));
    }

    @Test
    public void test_isBefore_elementWithin() {
        IndexRange range = new IndexRange(2, 5);
        assertFalse(range.isBefore(3));
    }

    @Test
    public void test_length() {
        assertEquals(0, new IndexRange(5, 5).length());
        assertEquals(3, new IndexRange(2, 5).length());
        assertEquals("bcd".length(), new IndexRange(2, 5).length());
    }

    @Test
    public void test_shift_positive() {
        assertEquals(new IndexRange(5, 8), new IndexRange(2, 5).shift(3));
    }

    @Test
    public void test_shift_negative() {
        assertEquals(new IndexRange(1, 4), new IndexRange(2, 5).shift(-1));
        assertEquals(new IndexRange(0, 3), new IndexRange(2, 5).shift(-2));
    }

    @Test
    public void test_shift_zero() {
        assertEquals(new IndexRange(2, 5), new IndexRange(2, 5).shift(0));
    }

    @Test
    public void test_shift_preservesLength() {
        IndexRange range = new IndexRange(2, 5);
        assertEquals(range.length(), range.shift(10).length());
        assertEquals(range.length(), range.shift(-1).length());
    }

    @Test
    public void test_shift_negativeStartThrows() {
        assertThrows(IllegalArgumentException.class, () -> new IndexRange(2, 5).shift(-3));
        assertThrows(IllegalArgumentException.class, () -> new IndexRange(0, 5).shift(-1));
    }

    @Test
    public void test_shift_overflowThrows() {
        assertThrows(IllegalArgumentException.class, () -> new IndexRange(Integer.MAX_VALUE - 1, Integer.MAX_VALUE).shift(1));
        assertThrows(IllegalArgumentException.class, () -> new IndexRange(0, Integer.MAX_VALUE).shift(1));
    }

    @Test
    public void test_forEach() {
        java.util.List<Integer> indices = new java.util.ArrayList<>();
        new IndexRange(2, 5).forEach(indices::add);
        assertEquals(java.util.List.of(2, 3, 4), indices);
    }

    @Test
    public void test_forEach_emptyRange() {
        java.util.List<Integer> indices = new java.util.ArrayList<>();
        new IndexRange(5, 5).forEach(indices::add);
        assertTrue(indices.isEmpty());
    }

    @Test
    public void test_forEach_nullAction() {
        assertThrows(IllegalArgumentException.class, () -> new IndexRange(2, 5).forEach(null));
    }

    @Test
    public void test_forEach_endIsIntegerMaxValue_doesNotHang() {
        final int[] last = { -1 };
        final int[] count = { 0 };
        // Only the last two indices: MAX_VALUE-2 and MAX_VALUE-1. A wrapping i++ would loop forever.
        new IndexRange(Integer.MAX_VALUE - 2, Integer.MAX_VALUE).forEach(i -> {
            last[0] = i;
            count[0]++;
        });
        assertEquals(2, count[0]);
        assertEquals(Integer.MAX_VALUE - 1, last[0]);
    }

    @Test
    public void test_intStream() {
        assertEquals(9, new IndexRange(2, 5).intStream().sum());
        assertEquals(java.util.List.of(2, 3, 4), new IndexRange(2, 5).intStream().boxed().toList());
    }

    @Test
    public void test_intStream_emptyRange() {
        assertEquals(0, new IndexRange(5, 5).intStream().count());
    }

    @Test
    public void test_toString() {
        assertEquals("[2, 5)", new IndexRange(2, 5).toString());
        assertEquals("[5, 5)", new IndexRange(5, 5).toString());
        assertEquals("[0, 1)", new IndexRange(0, 1).toString());
    }
}
