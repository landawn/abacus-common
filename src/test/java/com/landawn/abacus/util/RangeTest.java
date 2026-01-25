package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

@SuppressWarnings("boxing")
@Tag("old-test")
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
    public void testHashCode() {
        assertEquals(byteRange.hashCode(), byteRange2.hashCode());
        assertFalse(byteRange.hashCode() == byteRange3.hashCode());

        assertEquals(intRange.hashCode(), intRange.hashCode());
        assertTrue(intRange.hashCode() != 0);
    }

    @Test
    public void testToString() {
        assertNotNull(byteRange.toString());

        final String str = intRange.toString();
        assertEquals("[10, 20]", str);
        assertEquals("[-20, -10]", Range.closed(-20, -10).toString());
    }

    @Test
    public void testGetMinimum() {
        assertEquals(10, (int) intRange.lowerEndpoint());
        assertEquals(10L, (long) longRange.lowerEndpoint());
        assertEquals(10f, floatRange.lowerEndpoint(), 0.00001f);
        assertEquals(10d, doubleRange.lowerEndpoint(), 0.00001d);
    }

    @Test
    public void testGetMaximum() {
        assertEquals(20, (int) intRange.upperEndpoint());
        assertEquals(20L, (long) longRange.upperEndpoint());
        assertEquals(20f, floatRange.upperEndpoint(), 0.00001f);
        assertEquals(20d, doubleRange.upperEndpoint(), 0.00001d);
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
    public void testIsAfter() {
        assertFalse(intRange.isAfter(null));

        assertTrue(intRange.isAfter(5));
        assertFalse(intRange.isAfter(10));
        assertFalse(intRange.isAfter(15));
        assertFalse(intRange.isAfter(20));
        assertFalse(intRange.isAfter(25));
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
    public void testIsEndedBy() {
        assertFalse(intRange.isEndedBy(null));

        assertFalse(intRange.isEndedBy(5));
        assertFalse(intRange.isEndedBy(10));
        assertFalse(intRange.isEndedBy(15));
        assertTrue(intRange.isEndedBy(20));
        assertFalse(intRange.isEndedBy(25));
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
    public void test_span() {
        N.println(Range.closed(1, 3).span(Range.open(5, 7)));
        assertTrue(Range.closedOpen(1, 7).equals(Range.closed(1, 3).span(Range.open(5, 7))));

        N.println(Range.closed(1, 7).span(Range.open(5, 7)));
        assertTrue(Range.closed(1, 7).equals(Range.closed(1, 7).span(Range.open(5, 7))));

        N.println(Range.closed(1, 5).span(Range.open(5, 7)));
        assertTrue(Range.closedOpen(1, 7).equals(Range.closed(1, 5).span(Range.open(5, 7))));
    }

}
