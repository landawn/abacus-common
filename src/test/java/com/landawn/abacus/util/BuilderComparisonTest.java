package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BuilderComparisonTest extends BuilderTestSupport {
    @Test
    public void testComparisonBuilder_shortCircuit() {
        int result = Builder.compare("a", "b").compare(2, 1).result();
        assertTrue(result < 0);
    }

    @Test
    public void testComparisonChain() {
        int result = Builder.compare("a", "a").compare(1, 2).result();
        Assertions.assertTrue(result < 0);

        result = Builder.compare("a", "b").compare(2, 1).result();
        Assertions.assertTrue(result < 0);
    }

    @Test
    public void testComparisonBuilderChain() {
        int result = Builder.compare("a", "a").compare(5, 10).compare(true, false).result();

        assertEquals(-1, result);

        result = Builder.compare("b", "a").compare(5, 10).result();

        assertEquals(1, result);
    }

    @Test
    public void testComparisonBuilderPrimitives() {
        assertEquals(0, Builder.compare('a', 'a').result());
        assertEquals(0, Builder.compare((byte) 5, (byte) 5).result());
        assertEquals(0, Builder.compare((short) 10, (short) 10).result());
        assertEquals(0, Builder.compare(100L, 100L).result());
        assertEquals(0, Builder.compare(1.5f, 1.5f).result());
        assertEquals(0, Builder.compare(2.5, 2.5).result());

        assertEquals(-1, Builder.compareFalseLess(false, true).result());
        assertEquals(-1, Builder.compareTrueLess(true, false).result());
    }

    @Test
    public void testComparisonBuilder_instance_compareFalseLess() {
        int result = Builder.compare(1, 1).compareFalseLess(false, true).result();
        assertTrue(result < 0);

        result = Builder.compare(1, 1).compareFalseLess(true, false).result();
        assertTrue(result > 0);

        result = Builder.compare(1, 1).compareFalseLess(true, true).result();
        assertEquals(0, result);
    }

    @Test
    public void testComparisonBuilder_instance_compareTrueLess() {
        int result = Builder.compare(1, 1).compareTrueLess(true, false).result();
        assertTrue(result < 0);

        result = Builder.compare(1, 1).compareTrueLess(false, true).result();
        assertTrue(result > 0);

        result = Builder.compare(1, 1).compareTrueLess(false, false).result();
        assertEquals(0, result);
    }

    @Test
    public void testComparisonBuilder_instance_compare_withComparator() {
        int result = Builder.compare("a", "a").compare("b", "a", Comparator.naturalOrder()).result();
        assertTrue(result > 0);
    }

    @Test
    public void testComparisonBuilder_instance_compare_char() {
        int result = Builder.compare(1, 1).compare('b', 'a').result();
        assertTrue(result > 0);
    }

    @Test
    public void testComparisonBuilder_instance_compare_byte() {
        int result = Builder.compare(1, 1).compare((byte) 2, (byte) 1).result();
        assertTrue(result > 0);
    }

    @Test
    public void testComparisonBuilder_instance_compare_short() {
        int result = Builder.compare(1, 1).compare((short) 2, (short) 1).result();
        assertTrue(result > 0);
    }

    @Test
    public void testComparisonBuilder_instance_compare_int() {
        int result = Builder.compare(1, 1).compare(10, 5).result();
        assertTrue(result > 0);
    }

    @Test
    public void testComparisonBuilder_instance_compare_long() {
        int result = Builder.compare(1, 1).compare(100L, 50L).result();
        assertTrue(result > 0);
    }

    @Test
    public void testComparisonBuilder_instance_compare_float() {
        int result = Builder.compare(1, 1).compare(2.0f, 1.0f).result();
        assertTrue(result > 0);
    }

    @Test
    public void testComparisonBuilder_instance_compare_floatWithTolerance() {
        int result = Builder.compare(1, 1).compare(1.0f, 1.0001f, 0.001f).result();
        assertEquals(0, result);
    }

    @Test
    public void testComparisonBuilder_instance_compare_double() {
        int result = Builder.compare(1, 1).compare(2.0, 1.0).result();
        assertTrue(result > 0);
    }

    @Test
    public void testComparisonBuilder_instance_compare_doubleWithTolerance() {
        int result = Builder.compare(1, 1).compare(1.0, 1.0001, 0.001).result();
        assertEquals(0, result);
    }

    @Test
    public void testComparisonBuilder_instance_compare_comparable() {
        int result = Builder.compare("a", "a").compare("x", "y").result();
        assertTrue(result < 0);
    }

    @Test
    public void testComparisonBuilder_allEqual() {
        int result = Builder.compare("a", "a").compare(1, 1).compare(1L, 1L).compare(1.0f, 1.0f).compare(1.0, 1.0).result();
        assertEquals(0, result);
    }

    @Test
    public void testComparisonBuilder() {
        assertEquals(0, Builder.compare(1, 1).result());
        assertEquals(1, Builder.compare(2, 1).result());
        assertEquals(-1, Builder.compare(1, 2).result());

        assertEquals(-1, Builder.compare("a", "b").result());

        assertEquals(0, Builder.compare((String) null, (String) null, Comparators.naturalOrder()).result());
        assertEquals(-1, Builder.compare(null, "a", Comparators.naturalOrder()).result());

        assertEquals(-1, Builder.compare(1, 2).compare(3, 1).result());

        assertEquals(-1, Builder.compareNullLess(null, "a").result());
        assertEquals(1, Builder.compareNullBigger(null, "a").result());

        assertEquals(-1, Builder.compareFalseLess(false, true).result());
        assertEquals(1, Builder.compareTrueLess(false, true).result());
    }

    @Test
    public void testComparisonBuilderWithCustomComparator() {
        Comparator<String> caseInsensitive = String.CASE_INSENSITIVE_ORDER;

        int result = Builder.compare("Hello", "hello", caseInsensitive).result();
        assertEquals(0, result);

        result = Builder.compare("ABC", "xyz", caseInsensitive).result();
        assertTrue(result < 0);
    }

    @Test
    public void testComparisonBuilder_instance_compareNullLess() {
        // When first comparison is 0, compareNullLess should be evaluated
        int result = Builder.compare("a", "a").compareNullLess(null, "b").result();
        assertTrue(result < 0);

        result = Builder.compare("a", "a").compareNullLess("b", null).result();
        assertTrue(result > 0);

        result = Builder.compare("a", "a").compareNullLess((String) null, (String) null).result();
        assertEquals(0, result);
    }

    @Test
    public void testComparisonBuilder_instance_compareNullBigger() {
        int result = Builder.compare("a", "a").compareNullBigger(null, "b").result();
        assertTrue(result > 0);

        result = Builder.compare("a", "a").compareNullBigger("b", null).result();
        assertTrue(result < 0);

        result = Builder.compare("a", "a").compareNullBigger((String) null, (String) null).result();
        assertEquals(0, result);
    }

    @Test
    public void testComparisonBuilder_shortCircuit_skipsSubsequent() {
        // When first comparison is non-zero, subsequent should be skipped
        int result = Builder.compare("b", "a").compare("z", "a").compare(1, 2).result();
        // result should be from first comparison only
        assertTrue(result > 0);
    }

    @Test
    public void testComparisonBuilder_compare_rejectsNullComparator() {
        assertThrows(IllegalArgumentException.class, () -> Builder.compare("a", "a").compare("a", "b", (Comparator<String>) null));
    }

    @Test
    public void testComparisonBuilderNullHandling() {
        assertEquals(-1, Builder.compareNullLess(null, "a").result());
        assertEquals(1, Builder.compareNullLess("a", null).result());
        assertEquals(0, Builder.compareNullLess(null, null).result());

        assertEquals(1, Builder.compareNullBigger(null, "a").result());
        assertEquals(-1, Builder.compareNullBigger("a", null).result());
        assertEquals(0, Builder.compareNullBigger(null, null).result());
    }

    @Test
    public void testComparisonBuilder_instance_compareNullLess_skippedWhenDecided() {
        int result = Builder.compare("b", "a").compareNullLess(null, "x").result();
        assertTrue(result > 0);
    }

    @Test
    public void testComparisonBuilder_instance_compareNullBigger_skippedWhenDecided() {
        int result = Builder.compare("b", "a").compareNullBigger(null, "x").result();
        assertTrue(result > 0);
    }

    @Test
    public void testComparisonBuilder_instance_compareFalseLess_skippedWhenDecided() {
        int result = Builder.compare("b", "a").compareFalseLess(false, true).result();
        assertTrue(result > 0);
    }

    @Test
    public void testComparisonBuilder_instance_compareTrueLess_skippedWhenDecided() {
        int result = Builder.compare("b", "a").compareTrueLess(true, false).result();
        assertTrue(result > 0);
    }

    @Test
    public void testComparisonBuilder_chaining() {
        int result = Builder.compare("a", "a").compare(1, 2).result();
        assertTrue(result < 0);
    }
}
