package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BuilderCompareTest extends BuilderTestSupport {
    @Test
    public void testCompare_comparable() {
        int result = Builder.compare("a", "b").result();
        assertTrue(result < 0);
    }

    @Test
    public void testCompare_comparableEqual() {
        int result = Builder.compare("a", "a").result();
        assertEquals(0, result);
    }

    @Test
    public void testCompare_char() {
        int result = Builder.compare('a', 'b').result();
        assertTrue(result < 0);
    }

    @Test
    public void testCompare_byte() {
        int result = Builder.compare((byte) 1, (byte) 2).result();
        assertTrue(result < 0);
    }

    @Test
    public void testCompare_short() {
        int result = Builder.compare((short) 1, (short) 2).result();
        assertTrue(result < 0);
    }

    @Test
    public void testCompare_int() {
        int result = Builder.compare(1, 2).result();
        assertTrue(result < 0);
    }

    @Test
    public void testCompare_long() {
        int result = Builder.compare(1L, 2L).result();
        assertTrue(result < 0);
    }

    @Test
    public void testCompare_float() {
        int result = Builder.compare(1.0f, 2.0f).result();
        assertTrue(result < 0);
    }

    @Test
    public void testCompare_floatWithTolerance() {
        int result = Builder.compare(1.0001f, 1.0002f, 0.001f).result();
        assertEquals(0, result);
    }

    @Test
    public void testCompare_double() {
        int result = Builder.compare(1.0, 2.0).result();
        assertTrue(result < 0);
    }

    @Test
    public void testCompare_doubleWithTolerance() {
        int result = Builder.compare(1.00001, 1.00002, 0.0001).result();
        assertEquals(0, result);
    }

    @Test
    public void testCompare() {
        int result = Builder.compare("a", "b").result();
        Assertions.assertTrue(result < 0);

        result = Builder.compare("b", "a").result();
        Assertions.assertTrue(result > 0);

        result = Builder.compare("a", "a").result();
        Assertions.assertEquals(0, result);
    }

    @Test
    public void testCompareWithComparator() {
        Comparator<String> reverseComparator = Comparator.reverseOrder();
        int result = Builder.compare("a", "b", reverseComparator).result();
        Assertions.assertTrue(result > 0);
    }

    @Test
    public void testComparePrimitives() {
        Assertions.assertEquals(0, Builder.compare('a', 'a').result());
        Assertions.assertTrue(Builder.compare('a', 'b').result() < 0);

        Assertions.assertEquals(0, Builder.compare((byte) 1, (byte) 1).result());
        Assertions.assertTrue(Builder.compare((byte) 1, (byte) 2).result() < 0);

        Assertions.assertEquals(0, Builder.compare((short) 1, (short) 1).result());
        Assertions.assertTrue(Builder.compare((short) 1, (short) 2).result() < 0);

        Assertions.assertEquals(0, Builder.compare(1, 1).result());
        Assertions.assertTrue(Builder.compare(1, 2).result() < 0);

        Assertions.assertEquals(0, Builder.compare(1L, 1L).result());
        Assertions.assertTrue(Builder.compare(1L, 2L).result() < 0);

        Assertions.assertEquals(0, Builder.compare(1.0f, 1.0f).result());
        Assertions.assertTrue(Builder.compare(1.0f, 2.0f).result() < 0);

        Assertions.assertEquals(0, Builder.compare(1.0, 1.0).result());
        Assertions.assertTrue(Builder.compare(1.0, 2.0).result() < 0);
    }

    @Test
    public void testCompareWithTolerance() {
        Assertions.assertEquals(0, Builder.compare(1.0f, 1.001f, 0.01f).result());
        Assertions.assertTrue(Builder.compare(1.0f, 1.1f, 0.01f).result() < 0);

        Assertions.assertEquals(0, Builder.compare(1.0, 1.001, 0.01).result());
        Assertions.assertTrue(Builder.compare(1.0, 1.1, 0.01).result() < 0);
    }

    @Test
    public void testCompare_static_char() {
        int result = Builder.compare('b', 'a').result();
        assertTrue(result > 0);

        result = Builder.compare('a', 'a').result();
        assertEquals(0, result);
    }

    @Test
    public void testCompare_static_byte() {
        int result = Builder.compare((byte) 2, (byte) 1).result();
        assertTrue(result > 0);
    }

    @Test
    public void testCompare_static_short() {
        int result = Builder.compare((short) 2, (short) 1).result();
        assertTrue(result > 0);
    }

    @Test
    public void testCompare_static_int() {
        int result = Builder.compare(10, 5).result();
        assertTrue(result > 0);
    }

    @Test
    public void testCompare_static_long() {
        int result = Builder.compare(100L, 50L).result();
        assertTrue(result > 0);
    }

    @Test
    public void testCompare_static_float() {
        int result = Builder.compare(2.0f, 1.0f).result();
        assertTrue(result > 0);
    }

    @Test
    public void testCompare_static_floatWithTolerance() {
        int result = Builder.compare(1.0f, 1.0001f, 0.001f).result();
        assertEquals(0, result);
    }

    @Test
    public void testCompare_static_double() {
        int result = Builder.compare(2.0, 1.0).result();
        assertTrue(result > 0);
    }

    @Test
    public void testCompare_static_doubleWithTolerance() {
        int result = Builder.compare(1.0, 1.0001, 0.001).result();
        assertEquals(0, result);
    }

    @Test
    public void testCompareFalseLess_trueGreaterThanFalse() {
        int result = Builder.compareFalseLess(true, false).result();
        assertTrue(result > 0);
    }

    @Test
    public void testCompareTrueLess_falseGreaterThanTrue() {
        int result = Builder.compareTrueLess(false, true).result();
        assertTrue(result > 0);
    }

    @Test
    public void testCompare_char_equal() {
        assertEquals(0, Builder.compare('x', 'x').result());
    }

    @Test
    public void testCompare_byte_equal() {
        assertEquals(0, Builder.compare((byte) 3, (byte) 3).result());
    }

    @Test
    public void testCompare_short_equal() {
        assertEquals(0, Builder.compare((short) 3, (short) 3).result());
    }

    @Test
    public void testCompare_int_equal() {
        assertEquals(0, Builder.compare(3, 3).result());
    }

    @Test
    public void testCompare_long_equal() {
        assertEquals(0, Builder.compare(3L, 3L).result());
    }

    @Test
    public void testCompare_float_equal() {
        assertEquals(0, Builder.compare(3.0f, 3.0f).result());
    }

    @Test
    public void testCompare_double_equal() {
        assertEquals(0, Builder.compare(3.0, 3.0).result());
    }

    @Test
    public void testCompare_floatWithTolerance_notEqual() {
        int result = Builder.compare(1.0f, 2.0f, 0.001f).result();
        assertTrue(result < 0);
    }

    @Test
    public void testCompare_doubleWithTolerance_notEqual() {
        int result = Builder.compare(1.0, 2.0, 0.001).result();
        assertTrue(result < 0);
    }

    @Test
    public void testCompare_withComparator() {
        int result = Builder.compare("a", "B", String.CASE_INSENSITIVE_ORDER).result();
        assertTrue(result < 0);
    }

    @Test
    public void testCompareNullLess_bothNonNull() {
        int result = Builder.compareNullLess("b", "a").result();
        assertTrue(result > 0);
    }

    @Test
    public void testCompareNullLess_rightNull() {
        int result = Builder.compareNullLess("a", null).result();
        assertTrue(result > 0);
    }

    @Test
    public void testCompareNullBigger_bothNonNull() {
        int result = Builder.compareNullBigger("a", "b").result();
        assertTrue(result < 0);
    }

    @Test
    public void testCompareNullBigger_rightNull() {
        int result = Builder.compareNullBigger("a", null).result();
        assertTrue(result < 0);
    }

    @Test
    public void testCompare_char_positive() {
        int result = Builder.compare('z', 'a').result();
        assertTrue(result > 0);
    }

    @Test
    public void testCompare_byte_positive() {
        int result = Builder.compare((byte) 5, (byte) 1).result();
        assertTrue(result > 0);
    }

    @Test
    public void testCompare_short_positive() {
        int result = Builder.compare((short) 5, (short) 1).result();
        assertTrue(result > 0);
    }

    @Test
    public void testCompare_int_positive() {
        int result = Builder.compare(5, 1).result();
        assertTrue(result > 0);
    }

    @Test
    public void testCompare_long_positive() {
        int result = Builder.compare(5L, 1L).result();
        assertTrue(result > 0);
    }

    @Test
    public void testCompare_float_positive() {
        int result = Builder.compare(5.0f, 1.0f).result();
        assertTrue(result > 0);
    }

    @Test
    public void testCompare_double_positive() {
        int result = Builder.compare(5.0, 1.0).result();
        assertTrue(result > 0);
    }

    @Test
    public void testCompareNullLess() {
        int result = Builder.compareNullLess(null, "value").result();
        assertTrue(result < 0);
    }

    @Test
    public void testCompareNullLess_bothNull() {
        int result = Builder.compareNullLess(null, (String) null).result();
        assertEquals(0, result);
    }

    @Test
    public void testCompareNullBigger() {
        int result = Builder.compareNullBigger(null, "value").result();
        assertTrue(result > 0);
    }

    @Test
    public void testCompareNullBigger_bothNull() {
        int result = Builder.compareNullBigger(null, (String) null).result();
        assertEquals(0, result);
    }

    @Test
    public void testCompareFalseLess() {
        int result = Builder.compareFalseLess(false, true).result();
        assertTrue(result < 0);
    }

    @Test
    public void testCompareFalseLess_equal() {
        int result = Builder.compareFalseLess(true, true).result();
        assertEquals(0, result);
    }

    @Test
    public void testCompareTrueLess() {
        int result = Builder.compareTrueLess(true, false).result();
        assertTrue(result < 0);
    }

    @Test
    public void testCompareTrueLess_equal() {
        int result = Builder.compareTrueLess(false, false).result();
        assertEquals(0, result);
    }

    // ---- G28-003: compare(left, right, comparator) hands both arguments over unchanged, null included ----

    @Test
    public void testCompare_withComparator_nullArgumentReachesComparator() {
        assertThrows(NullPointerException.class, () -> Builder.compare((String) null, "a", Comparator.<String> naturalOrder()));
        assertThrows(NullPointerException.class, () -> Builder.compare(1, 1).compare((String) null, "a", Comparator.<String> naturalOrder()));
        assertThrows(NullPointerException.class, () -> Builder.compare("a", (String) null, Comparator.comparing(String::length)));

        // a null-tolerant comparator is handed the nulls and decides
        assertEquals(0, Builder.compare((String) null, (String) null, Comparator.nullsFirst(Comparator.<String> naturalOrder())).result());
        assertTrue(Builder.compare((String) null, "a", Comparator.nullsFirst(Comparator.<String> naturalOrder())).result() < 0);
        assertTrue(Builder.compare((String) null, "a", Comparator.nullsLast(Comparator.<String> naturalOrder())).result() > 0);

        // contrast: compareNullLess / compareNullBigger handle null themselves
        assertTrue(Builder.compareNullLess((String) null, "a").result() < 0);
        assertTrue(Builder.compareNullBigger((String) null, "a").result() > 0);
    }

    @Test
    public void testCompare_withComparator_decidedChainDoesNotInvokeComparator() {
        // ComparisonBuilder.compare(T, T, Comparator) only calls the comparator while result == 0, so on an
        // already-decided chain a null argument is harmless and the comparator is never invoked at all.
        assertEquals(1, Builder.compare(2, 1).compare((String) null, "a", Comparator.<String> naturalOrder()).result());

        final boolean[] called = { false };
        final int result = Builder.compare(2, 1).compare("x", "y", (a, b) -> {
            called[0] = true;
            throw new IllegalStateException("must not be invoked");
        }).result();
        assertEquals(1, result);
        assertFalse(called[0]);

        // the null-comparator check runs before the short-circuit, so it still fires on a decided chain
        assertThrows(IllegalArgumentException.class, () -> Builder.compare(2, 1).compare("x", "y", (Comparator<String>) null));
    }
}
