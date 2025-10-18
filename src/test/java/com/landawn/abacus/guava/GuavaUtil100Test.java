package com.landawn.abacus.guava;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.function.Function;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Range;
import com.landawn.abacus.TestBase;

@Tag("new-test")
public class GuavaUtil100Test extends TestBase {

    @Test
    public void testTransform_ClosedRange() {
        Range<Integer> intRange = Range.closed(1, 10);
        Range<String> stringRange = GuavaUtil.transform(intRange, Object::toString);

        assertTrue(stringRange.hasLowerBound());
        assertTrue(stringRange.hasUpperBound());
        assertEquals("1", stringRange.lowerEndpoint());
        assertEquals("10", stringRange.upperEndpoint());
        assertFalse(stringRange.contains("5"));
        assertFalse(stringRange.contains("0"));
        assertFalse(stringRange.contains("11"));
    }

    @Test
    public void testTransform_OpenRange() {
        Range<Integer> intRange = Range.open(1, 10);
        Range<String> stringRange = GuavaUtil.transform(intRange, Object::toString);

        assertTrue(stringRange.hasLowerBound());
        assertTrue(stringRange.hasUpperBound());
        assertEquals("1", stringRange.lowerEndpoint());
        assertEquals("10", stringRange.upperEndpoint());
        assertFalse(stringRange.contains("1"));
        assertFalse(stringRange.contains("10"));
        assertFalse(stringRange.contains("5"));
    }

    @Test
    public void testTransform_ClosedOpenRange() {
        Range<Integer> intRange = Range.closedOpen(1, 10);
        Range<Double> doubleRange = GuavaUtil.transform(intRange, Integer::doubleValue);

        assertTrue(doubleRange.hasLowerBound());
        assertTrue(doubleRange.hasUpperBound());
        assertEquals(1.0, doubleRange.lowerEndpoint());
        assertEquals(10.0, doubleRange.upperEndpoint());
        assertTrue(doubleRange.contains(1.0));
        assertFalse(doubleRange.contains(10.0));
    }

    @Test
    public void testTransform_UnboundedRange() {
        Range<Integer> intRange = Range.all();
        Range<String> stringRange = GuavaUtil.transform(intRange, Object::toString);

        assertFalse(stringRange.hasLowerBound());
        assertFalse(stringRange.hasUpperBound());
        assertTrue(stringRange.contains("100"));
        assertTrue(stringRange.contains("-100"));
    }

    @Test
    public void testTransform_GreaterThanRange() {
        Range<Integer> intRange = Range.greaterThan(5);
        Range<Double> doubleRange = GuavaUtil.transform(intRange, Integer::doubleValue);

        assertTrue(doubleRange.hasLowerBound());
        assertFalse(doubleRange.hasUpperBound());
        assertEquals(5.0, doubleRange.lowerEndpoint());
        assertFalse(doubleRange.contains(5.0));
        assertTrue(doubleRange.contains(6.0));
        assertTrue(doubleRange.contains(1000.0));
    }

    @Test
    public void testTransform_AtLeastRange() {
        Range<Integer> intRange = Range.atLeast(5);
        Range<String> stringRange = GuavaUtil.transform(intRange, Object::toString);

        assertTrue(stringRange.hasLowerBound());
        assertFalse(stringRange.hasUpperBound());
        assertEquals("5", stringRange.lowerEndpoint());
        assertTrue(stringRange.contains("5"));
        assertTrue(stringRange.contains("6"));
        assertFalse(stringRange.contains("4"));
    }

    @Test
    public void testTransform_LessThanRange() {
        Range<Integer> intRange = Range.lessThan(10);
        Range<Double> doubleRange = GuavaUtil.transform(intRange, Integer::doubleValue);

        assertFalse(doubleRange.hasLowerBound());
        assertTrue(doubleRange.hasUpperBound());
        assertEquals(10.0, doubleRange.upperEndpoint());
        assertFalse(doubleRange.contains(10.0));
        assertTrue(doubleRange.contains(9.0));
        assertTrue(doubleRange.contains(-100.0));
    }

    @Test
    public void testTransform_AtMostRange() {
        Range<Integer> intRange = Range.atMost(10);
        Range<String> stringRange = GuavaUtil.transform(intRange, Object::toString);

        assertFalse(stringRange.hasLowerBound());
        assertTrue(stringRange.hasUpperBound());
        assertEquals("10", stringRange.upperEndpoint());
        assertTrue(stringRange.contains("10"));
        assertFalse(stringRange.contains("9"));
        assertFalse(stringRange.contains("11"));
    }

    @Test
    public void testTransform_SingletonRange() {
        Range<Integer> intRange = Range.singleton(5);
        Range<String> stringRange = GuavaUtil.transform(intRange, Object::toString);

        assertTrue(stringRange.hasLowerBound());
        assertTrue(stringRange.hasUpperBound());
        assertEquals("5", stringRange.lowerEndpoint());
        assertEquals("5", stringRange.upperEndpoint());
        assertTrue(stringRange.contains("5"));
        assertFalse(stringRange.contains("4"));
        assertFalse(stringRange.contains("6"));
    }

    @Test
    public void testTransform_DateRange() {
        Date start = new Date(1000);
        Date end = new Date(2000);
        Range<Date> dateRange = Range.closed(start, end);
        Range<Long> timestampRange = GuavaUtil.transform(dateRange, Date::getTime);

        assertTrue(timestampRange.hasLowerBound());
        assertTrue(timestampRange.hasUpperBound());
        assertEquals(1000L, timestampRange.lowerEndpoint());
        assertEquals(2000L, timestampRange.upperEndpoint());
        assertTrue(timestampRange.contains(1500L));
    }

    @Test
    public void testTransform_ComplexMapper() {
        Range<Integer> intRange = Range.closed(1, 100);
        Function<Integer, String> complexMapper = i -> "NUM_" + (i * 2);
        Range<String> stringRange = GuavaUtil.transform(intRange, complexMapper);

        assertEquals("NUM_2", stringRange.lowerEndpoint());
        assertEquals("NUM_200", stringRange.upperEndpoint());
    }

    @Test
    public void testTransform_NullPointerException() {
        Range<Integer> intRange = Range.closed(1, 10);

        assertThrows(NullPointerException.class, () -> {
            GuavaUtil.transform(null, Object::toString);
        });

        assertThrows(NullPointerException.class, () -> {
            GuavaUtil.transform(intRange, null);
        });
    }

    @Test
    public void testTransform_EmptyRange() {
        Range<Integer> emptyRange = Range.closedOpen(5, 5);
        Range<String> transformedEmpty = GuavaUtil.transform(emptyRange, Object::toString);

        assertTrue(transformedEmpty.isEmpty());
    }

    @Test
    public void testTransform_ComparableConsistency() {
        Range<Integer> intRange = Range.closed(1, 10);
        Range<String> stringRange = GuavaUtil.transform(intRange, i -> String.format("%02d", i));

        assertTrue(stringRange.contains("05"));
        assertFalse(stringRange.contains("00"));
        assertFalse(stringRange.contains("11"));
    }
}
