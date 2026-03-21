package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

public class IEEE754rUtilTest extends AbstractTest {

    @Test
    public void testLang381() {
        assertEquals(1.2, IEEE754rUtil.min(1.2, 2.5, Double.NaN), 0.01);
        assertEquals(2.5, IEEE754rUtil.max(1.2, 2.5, Double.NaN), 0.01);
        assertTrue(Double.isNaN(IEEE754rUtil.max(Double.NaN, Double.NaN, Double.NaN)));
        assertEquals(1.2f, IEEE754rUtil.min(1.2f, 2.5f, Float.NaN), 0.01);
        assertEquals(2.5f, IEEE754rUtil.max(1.2f, 2.5f, Float.NaN), 0.01);
        assertTrue(Float.isNaN(IEEE754rUtil.max(Float.NaN, Float.NaN, Float.NaN)));

        final double[] a = { 1.2, Double.NaN, 3.7, 27.0, 42.0, Double.NaN };
        assertEquals(42.0, IEEE754rUtil.max(a), 0.01);
        assertEquals(1.2, IEEE754rUtil.min(a), 0.01);

        final double[] b = { Double.NaN, 1.2, Double.NaN, 3.7, 27.0, 42.0, Double.NaN };
        assertEquals(42.0, IEEE754rUtil.max(b), 0.01);
        assertEquals(1.2, IEEE754rUtil.min(b), 0.01);

        final float[] aF = { 1.2f, Float.NaN, 3.7f, 27.0f, 42.0f, Float.NaN };
        assertEquals(1.2f, IEEE754rUtil.min(aF), 0.01);
        assertEquals(42.0f, IEEE754rUtil.max(aF), 0.01);

        final float[] bF = { Float.NaN, 1.2f, Float.NaN, 3.7f, 27.0f, 42.0f, Float.NaN };
        assertEquals(1.2f, IEEE754rUtil.min(bF), 0.01);
        assertEquals(42.0f, IEEE754rUtil.max(bF), 0.01);
    }

    // ===== min(float, float) =====

    @Test
    public void test_min_float_float_normalValues() {
        assertEquals(3.0f, IEEE754rUtil.min(3.0f, 5.0f));
        assertEquals(3.0f, IEEE754rUtil.min(5.0f, 3.0f));
        assertEquals(-5.0f, IEEE754rUtil.min(-5.0f, -3.0f));
        assertEquals(-5.0f, IEEE754rUtil.min(-3.0f, -5.0f));
    }

    @Test
    public void test_min_float_float_equalValues() {
        assertEquals(5.0f, IEEE754rUtil.min(5.0f, 5.0f));
        assertEquals(0.0f, IEEE754rUtil.min(0.0f, 0.0f));
        assertEquals(-5.0f, IEEE754rUtil.min(-5.0f, -5.0f));
    }

    @Test
    public void test_min_float_float_withNaN() {
        assertEquals(5.0f, IEEE754rUtil.min(Float.NaN, 5.0f));
        assertEquals(5.0f, IEEE754rUtil.min(5.0f, Float.NaN));
        assertTrue(Float.isNaN(IEEE754rUtil.min(Float.NaN, Float.NaN)));
    }

    @Test
    public void test_min_float_float_withInfinity() {
        assertEquals(Float.NEGATIVE_INFINITY, IEEE754rUtil.min(Float.NEGATIVE_INFINITY, 5.0f));
        assertEquals(5.0f, IEEE754rUtil.min(Float.POSITIVE_INFINITY, 5.0f));
        assertEquals(Float.NEGATIVE_INFINITY, IEEE754rUtil.min(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY));
    }

    @Test
    public void test_min_float_float_withZero() {
        assertEquals(0.0f, IEEE754rUtil.min(0.0f, 5.0f));
        assertEquals(-0.0f, IEEE754rUtil.min(-0.0f, 0.0f));
    }

    @Test
    public void test_min_float_float_withMaxMinValue() {
        assertEquals(Float.MIN_VALUE, IEEE754rUtil.min(Float.MIN_VALUE, Float.MAX_VALUE));
        assertEquals(Float.MIN_VALUE, IEEE754rUtil.min(Float.MAX_VALUE, Float.MIN_VALUE));
        assertEquals(Float.MAX_VALUE, IEEE754rUtil.min(Float.MAX_VALUE, Float.MAX_VALUE));
    }

    // ===== min(float, float, float) =====

    @Test
    public void test_min_float_float_float_normalValues() {
        assertEquals(1.0f, IEEE754rUtil.min(3.0f, 5.0f, 1.0f));
        assertEquals(1.0f, IEEE754rUtil.min(1.0f, 5.0f, 3.0f));
        assertEquals(1.0f, IEEE754rUtil.min(5.0f, 1.0f, 3.0f));
        assertEquals(-5.0f, IEEE754rUtil.min(-5.0f, -3.0f, -1.0f));
    }

    @Test
    public void test_min_float_float_float_withNaN() {
        assertEquals(1.0f, IEEE754rUtil.min(Float.NaN, 5.0f, 1.0f));
        assertEquals(1.0f, IEEE754rUtil.min(5.0f, Float.NaN, 1.0f));
        assertEquals(1.0f, IEEE754rUtil.min(5.0f, 1.0f, Float.NaN));
        assertEquals(5.0f, IEEE754rUtil.min(Float.NaN, Float.NaN, 5.0f));
        assertTrue(Float.isNaN(IEEE754rUtil.min(Float.NaN, Float.NaN, Float.NaN)));
    }

    @Test
    public void test_min_float_float_float_allEqual() {
        assertEquals(5.0f, IEEE754rUtil.min(5.0f, 5.0f, 5.0f));
    }

    @Test
    public void test_min_float_float_float_withInfinity() {
        assertEquals(Float.NEGATIVE_INFINITY, IEEE754rUtil.min(Float.NEGATIVE_INFINITY, 3.0f, 5.0f));
        assertEquals(1.0f, IEEE754rUtil.min(Float.POSITIVE_INFINITY, 1.0f, 5.0f));
        assertEquals(Float.NEGATIVE_INFINITY, IEEE754rUtil.min(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, 0.0f));
    }

    // ===== min(float...) =====

    @Test
    public void test_min_float_array_normalValues() {
        assertEquals(1.0f, IEEE754rUtil.min(new float[] { 3.0f, 5.0f, 1.0f, 7.0f }));
        assertEquals(-5.0f, IEEE754rUtil.min(new float[] { 3.0f, -5.0f, 1.0f, 0.0f }));
    }

    @Test
    public void test_min_float_array_singleElement() {
        assertEquals(5.0f, IEEE754rUtil.min(new float[] { 5.0f }));
        assertEquals(Float.NaN, IEEE754rUtil.min(new float[] { Float.NaN }));
    }

    @Test
    public void test_min_float_array_withNaN() {
        assertEquals(1.0f, IEEE754rUtil.min(new float[] { 3.0f, Float.NaN, 1.0f, 5.0f }));
        assertEquals(1.0f, IEEE754rUtil.min(new float[] { Float.NaN, Float.NaN, 1.0f }));
        assertTrue(Float.isNaN(IEEE754rUtil.min(new float[] { Float.NaN, Float.NaN, Float.NaN })));
    }

    @Test
    public void test_min_float_array_withInfinity() {
        assertEquals(Float.NEGATIVE_INFINITY, IEEE754rUtil.min(new float[] { 3.0f, Float.NEGATIVE_INFINITY, 5.0f }));
        assertEquals(1.0f, IEEE754rUtil.min(new float[] { 3.0f, Float.POSITIVE_INFINITY, 1.0f, 5.0f }));
    }

    @Test
    public void test_min_float_array_allSameValues() {
        assertEquals(5.0f, IEEE754rUtil.min(new float[] { 5.0f, 5.0f, 5.0f, 5.0f }));
    }

    @Test
    public void test_min_float_array_largeArray() {
        float[] largeArray = new float[1000];
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = i * 1.5f;
        }
        largeArray[500] = -999.0f;
        assertEquals(-999.0f, IEEE754rUtil.min(largeArray));
    }

    // ===== min(double, double) =====

    @Test
    public void test_min_double_double_normalValues() {
        assertEquals(3.0, IEEE754rUtil.min(3.0, 5.0));
        assertEquals(3.0, IEEE754rUtil.min(5.0, 3.0));
        assertEquals(-5.0, IEEE754rUtil.min(-5.0, -3.0));
        assertEquals(-5.0, IEEE754rUtil.min(-3.0, -5.0));
    }

    @Test
    public void test_min_double_double_equalValues() {
        assertEquals(5.0, IEEE754rUtil.min(5.0, 5.0));
        assertEquals(0.0, IEEE754rUtil.min(0.0, 0.0));
        assertEquals(-5.0, IEEE754rUtil.min(-5.0, -5.0));
    }

    @Test
    public void test_min_double_double_withNaN() {
        assertEquals(5.0, IEEE754rUtil.min(Double.NaN, 5.0));
        assertEquals(5.0, IEEE754rUtil.min(5.0, Double.NaN));
        assertTrue(Double.isNaN(IEEE754rUtil.min(Double.NaN, Double.NaN)));
    }

    @Test
    public void test_min_double_double_withInfinity() {
        assertEquals(Double.NEGATIVE_INFINITY, IEEE754rUtil.min(Double.NEGATIVE_INFINITY, 5.0));
        assertEquals(5.0, IEEE754rUtil.min(Double.POSITIVE_INFINITY, 5.0));
        assertEquals(Double.NEGATIVE_INFINITY, IEEE754rUtil.min(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
    }

    @Test
    public void test_min_double_double_withZero() {
        assertEquals(0.0, IEEE754rUtil.min(0.0, 5.0));
        assertEquals(-0.0, IEEE754rUtil.min(-0.0, 0.0));
    }

    @Test
    public void test_min_double_double_withMaxMinValue() {
        assertEquals(Double.MIN_VALUE, IEEE754rUtil.min(Double.MIN_VALUE, Double.MAX_VALUE));
        assertEquals(Double.MIN_VALUE, IEEE754rUtil.min(Double.MAX_VALUE, Double.MIN_VALUE));
        assertEquals(Double.MAX_VALUE, IEEE754rUtil.min(Double.MAX_VALUE, Double.MAX_VALUE));
    }

    // ===== min(double, double, double) =====

    @Test
    public void test_min_double_double_double_normalValues() {
        assertEquals(1.0, IEEE754rUtil.min(3.0, 5.0, 1.0));
        assertEquals(1.0, IEEE754rUtil.min(1.0, 5.0, 3.0));
        assertEquals(1.0, IEEE754rUtil.min(5.0, 1.0, 3.0));
        assertEquals(-5.0, IEEE754rUtil.min(-5.0, -3.0, -1.0));
    }

    @Test
    public void test_min_double_double_double_withNaN() {
        assertEquals(1.0, IEEE754rUtil.min(Double.NaN, 5.0, 1.0));
        assertEquals(1.0, IEEE754rUtil.min(5.0, Double.NaN, 1.0));
        assertEquals(1.0, IEEE754rUtil.min(5.0, 1.0, Double.NaN));
        assertEquals(5.0, IEEE754rUtil.min(Double.NaN, Double.NaN, 5.0));
        assertTrue(Double.isNaN(IEEE754rUtil.min(Double.NaN, Double.NaN, Double.NaN)));
    }

    @Test
    public void test_min_double_double_double_allEqual() {
        assertEquals(5.0, IEEE754rUtil.min(5.0, 5.0, 5.0));
    }

    @Test
    public void test_min_double_double_double_withInfinity() {
        assertEquals(Double.NEGATIVE_INFINITY, IEEE754rUtil.min(Double.NEGATIVE_INFINITY, 3.0, 5.0));
        assertEquals(1.0, IEEE754rUtil.min(Double.POSITIVE_INFINITY, 1.0, 5.0));
        assertEquals(Double.NEGATIVE_INFINITY, IEEE754rUtil.min(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.0));
    }

    // ===== min(double...) =====

    @Test
    public void test_min_double_array_normalValues() {
        assertEquals(1.0, IEEE754rUtil.min(new double[] { 3.0, 5.0, 1.0, 7.0 }));
        assertEquals(-5.0, IEEE754rUtil.min(new double[] { 3.0, -5.0, 1.0, 0.0 }));
    }

    @Test
    public void test_min_double_array_singleElement() {
        assertEquals(5.0, IEEE754rUtil.min(new double[] { 5.0 }));
        assertTrue(Double.isNaN(IEEE754rUtil.min(new double[] { Double.NaN })));
    }

    @Test
    public void test_min_double_array_withNaN() {
        assertEquals(1.0, IEEE754rUtil.min(new double[] { 3.0, Double.NaN, 1.0, 5.0 }));
        assertEquals(1.0, IEEE754rUtil.min(new double[] { Double.NaN, Double.NaN, 1.0 }));
        assertTrue(Double.isNaN(IEEE754rUtil.min(new double[] { Double.NaN, Double.NaN, Double.NaN })));
    }

    @Test
    public void test_min_double_array_withInfinity() {
        assertEquals(Double.NEGATIVE_INFINITY, IEEE754rUtil.min(new double[] { 3.0, Double.NEGATIVE_INFINITY, 5.0 }));
        assertEquals(1.0, IEEE754rUtil.min(new double[] { 3.0, Double.POSITIVE_INFINITY, 1.0, 5.0 }));
    }

    @Test
    public void test_min_double_array_allSameValues() {
        assertEquals(5.0, IEEE754rUtil.min(new double[] { 5.0, 5.0, 5.0, 5.0 }));
    }

    // ===== Cross-method tests =====

    @Test
    public void test_min_max_symmetry_float() {
        float[] values = { 3.0f, 5.0f, 1.0f, 7.0f };
        float min = IEEE754rUtil.min(values);
        float max = IEEE754rUtil.max(values);
        assertEquals(1.0f, min);
        assertEquals(7.0f, max);
        assertTrue(min < max);
    }

    @Test
    public void test_min_max_symmetry_double() {
        double[] values = { 3.0, 5.0, 1.0, 7.0 };
        double min = IEEE754rUtil.min(values);
        double max = IEEE754rUtil.max(values);
        assertEquals(1.0, min);
        assertEquals(7.0, max);
        assertTrue(min < max);
    }

    @Test
    public void testEnforceExceptions() {
        try {
            IEEE754rUtil.min((float[]) null);
            fail("IllegalArgumentException expected for null input");
        } catch (final IllegalArgumentException iae) {
        }

        try {
            IEEE754rUtil.min();
            fail("IllegalArgumentException expected for empty input");
        } catch (final IllegalArgumentException iae) {
        }

        try {
            IEEE754rUtil.max((float[]) null);
            fail("IllegalArgumentException expected for null input");
        } catch (final IllegalArgumentException iae) {
        }

        try {
            IEEE754rUtil.max();
            fail("IllegalArgumentException expected for empty input");
        } catch (final IllegalArgumentException iae) {
        }

        try {
            IEEE754rUtil.min((double[]) null);
            fail("IllegalArgumentException expected for null input");
        } catch (final IllegalArgumentException iae) {
        }

        try {
            IEEE754rUtil.min();
            fail("IllegalArgumentException expected for empty input");
        } catch (final IllegalArgumentException iae) {
        }

        try {
            IEEE754rUtil.max((double[]) null);
            fail("IllegalArgumentException expected for null input");
        } catch (final IllegalArgumentException iae) {
        }

        try {
            IEEE754rUtil.max();
            fail("IllegalArgumentException expected for empty input");
        } catch (final IllegalArgumentException iae) {
        }

    }

    @Test
    public void test_min_float_array_nullArray() {
        assertThrows(IllegalArgumentException.class, () -> IEEE754rUtil.min((float[]) null));
    }

    @Test
    public void test_min_float_array_emptyArray() {
        assertThrows(IllegalArgumentException.class, () -> IEEE754rUtil.min(new float[] {}));
    }

    @Test
    public void test_min_double_array_nullArray() {
        assertThrows(IllegalArgumentException.class, () -> IEEE754rUtil.min((double[]) null));
    }

    @Test
    public void test_min_double_array_emptyArray() {
        assertThrows(IllegalArgumentException.class, () -> IEEE754rUtil.min(new double[] {}));
    }

    // ===== max(float, float) =====

    @Test
    public void test_max_float_float_normalValues() {
        assertEquals(5.0f, IEEE754rUtil.max(3.0f, 5.0f));
        assertEquals(5.0f, IEEE754rUtil.max(5.0f, 3.0f));
        assertEquals(-3.0f, IEEE754rUtil.max(-5.0f, -3.0f));
        assertEquals(-3.0f, IEEE754rUtil.max(-3.0f, -5.0f));
    }

    @Test
    public void test_max_float_float_equalValues() {
        assertEquals(5.0f, IEEE754rUtil.max(5.0f, 5.0f));
        assertEquals(0.0f, IEEE754rUtil.max(0.0f, 0.0f));
        assertEquals(-5.0f, IEEE754rUtil.max(-5.0f, -5.0f));
    }

    @Test
    public void test_max_float_float_withNaN() {
        assertEquals(5.0f, IEEE754rUtil.max(Float.NaN, 5.0f));
        assertEquals(5.0f, IEEE754rUtil.max(5.0f, Float.NaN));
        assertTrue(Float.isNaN(IEEE754rUtil.max(Float.NaN, Float.NaN)));
    }

    @Test
    public void test_max_float_float_withInfinity() {
        assertEquals(5.0f, IEEE754rUtil.max(Float.NEGATIVE_INFINITY, 5.0f));
        assertEquals(Float.POSITIVE_INFINITY, IEEE754rUtil.max(Float.POSITIVE_INFINITY, 5.0f));
        assertEquals(Float.POSITIVE_INFINITY, IEEE754rUtil.max(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY));
    }

    @Test
    public void test_max_float_float_withZero() {
        assertEquals(5.0f, IEEE754rUtil.max(0.0f, 5.0f));
        assertEquals(0.0f, IEEE754rUtil.max(-0.0f, 0.0f));
    }

    @Test
    public void test_max_float_float_withMaxMinValue() {
        assertEquals(Float.MAX_VALUE, IEEE754rUtil.max(Float.MIN_VALUE, Float.MAX_VALUE));
        assertEquals(Float.MAX_VALUE, IEEE754rUtil.max(Float.MAX_VALUE, Float.MIN_VALUE));
        assertEquals(Float.MIN_VALUE, IEEE754rUtil.max(Float.MIN_VALUE, Float.MIN_VALUE));
    }

    // ===== max(float, float, float) =====

    @Test
    public void test_max_float_float_float_normalValues() {
        assertEquals(5.0f, IEEE754rUtil.max(3.0f, 5.0f, 1.0f));
        assertEquals(5.0f, IEEE754rUtil.max(1.0f, 5.0f, 3.0f));
        assertEquals(5.0f, IEEE754rUtil.max(5.0f, 1.0f, 3.0f));
        assertEquals(-1.0f, IEEE754rUtil.max(-5.0f, -3.0f, -1.0f));
    }

    @Test
    public void test_max_float_float_float_withNaN() {
        assertEquals(5.0f, IEEE754rUtil.max(Float.NaN, 5.0f, 1.0f));
        assertEquals(5.0f, IEEE754rUtil.max(5.0f, Float.NaN, 1.0f));
        assertEquals(5.0f, IEEE754rUtil.max(5.0f, 1.0f, Float.NaN));
        assertEquals(5.0f, IEEE754rUtil.max(Float.NaN, Float.NaN, 5.0f));
        assertTrue(Float.isNaN(IEEE754rUtil.max(Float.NaN, Float.NaN, Float.NaN)));
    }

    @Test
    public void test_max_float_float_float_allEqual() {
        assertEquals(5.0f, IEEE754rUtil.max(5.0f, 5.0f, 5.0f));
    }

    @Test
    public void test_max_float_float_float_withInfinity() {
        assertEquals(5.0f, IEEE754rUtil.max(Float.NEGATIVE_INFINITY, 3.0f, 5.0f));
        assertEquals(Float.POSITIVE_INFINITY, IEEE754rUtil.max(Float.POSITIVE_INFINITY, 1.0f, 5.0f));
        assertEquals(Float.POSITIVE_INFINITY, IEEE754rUtil.max(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, 0.0f));
    }

    // ===== max(float...) =====

    @Test
    public void test_max_float_array_normalValues() {
        assertEquals(7.0f, IEEE754rUtil.max(new float[] { 3.0f, 5.0f, 1.0f, 7.0f }));
        assertEquals(3.0f, IEEE754rUtil.max(new float[] { 3.0f, -5.0f, 1.0f, 0.0f }));
    }

    @Test
    public void test_max_float_array_singleElement() {
        assertEquals(5.0f, IEEE754rUtil.max(new float[] { 5.0f }));
        assertTrue(Float.isNaN(IEEE754rUtil.max(new float[] { Float.NaN })));
    }

    @Test
    public void test_max_float_array_withNaN() {
        assertEquals(5.0f, IEEE754rUtil.max(new float[] { 3.0f, Float.NaN, 1.0f, 5.0f }));
        assertEquals(1.0f, IEEE754rUtil.max(new float[] { Float.NaN, Float.NaN, 1.0f }));
        assertTrue(Float.isNaN(IEEE754rUtil.max(new float[] { Float.NaN, Float.NaN, Float.NaN })));
    }

    @Test
    public void test_max_float_array_withInfinity() {
        assertEquals(5.0f, IEEE754rUtil.max(new float[] { 3.0f, Float.NEGATIVE_INFINITY, 5.0f }));
        assertEquals(Float.POSITIVE_INFINITY, IEEE754rUtil.max(new float[] { 3.0f, Float.POSITIVE_INFINITY, 1.0f, 5.0f }));
    }

    @Test
    public void test_max_float_array_allSameValues() {
        assertEquals(5.0f, IEEE754rUtil.max(new float[] { 5.0f, 5.0f, 5.0f, 5.0f }));
    }

    // ===== max(double, double) =====

    @Test
    public void test_max_double_double_normalValues() {
        assertEquals(5.0, IEEE754rUtil.max(3.0, 5.0));
        assertEquals(5.0, IEEE754rUtil.max(5.0, 3.0));
        assertEquals(-3.0, IEEE754rUtil.max(-5.0, -3.0));
        assertEquals(-3.0, IEEE754rUtil.max(-3.0, -5.0));
    }

    @Test
    public void test_max_double_double_equalValues() {
        assertEquals(5.0, IEEE754rUtil.max(5.0, 5.0));
        assertEquals(0.0, IEEE754rUtil.max(0.0, 0.0));
        assertEquals(-5.0, IEEE754rUtil.max(-5.0, -5.0));
    }

    @Test
    public void test_max_double_double_withNaN() {
        assertEquals(5.0, IEEE754rUtil.max(Double.NaN, 5.0));
        assertEquals(5.0, IEEE754rUtil.max(5.0, Double.NaN));
        assertTrue(Double.isNaN(IEEE754rUtil.max(Double.NaN, Double.NaN)));
    }

    @Test
    public void test_max_double_double_withInfinity() {
        assertEquals(5.0, IEEE754rUtil.max(Double.NEGATIVE_INFINITY, 5.0));
        assertEquals(Double.POSITIVE_INFINITY, IEEE754rUtil.max(Double.POSITIVE_INFINITY, 5.0));
        assertEquals(Double.POSITIVE_INFINITY, IEEE754rUtil.max(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
    }

    @Test
    public void test_max_double_double_withZero() {
        assertEquals(5.0, IEEE754rUtil.max(0.0, 5.0));
        assertEquals(0.0, IEEE754rUtil.max(-0.0, 0.0));
    }

    @Test
    public void test_max_double_double_withMaxMinValue() {
        assertEquals(Double.MAX_VALUE, IEEE754rUtil.max(Double.MIN_VALUE, Double.MAX_VALUE));
        assertEquals(Double.MAX_VALUE, IEEE754rUtil.max(Double.MAX_VALUE, Double.MIN_VALUE));
        assertEquals(Double.MIN_VALUE, IEEE754rUtil.max(Double.MIN_VALUE, Double.MIN_VALUE));
    }

    // ===== max(double, double, double) =====

    @Test
    public void test_max_double_double_double_normalValues() {
        assertEquals(5.0, IEEE754rUtil.max(3.0, 5.0, 1.0));
        assertEquals(5.0, IEEE754rUtil.max(1.0, 5.0, 3.0));
        assertEquals(5.0, IEEE754rUtil.max(5.0, 1.0, 3.0));
        assertEquals(-1.0, IEEE754rUtil.max(-5.0, -3.0, -1.0));
    }

    @Test
    public void test_max_double_double_double_withNaN() {
        assertEquals(5.0, IEEE754rUtil.max(Double.NaN, 5.0, 1.0));
        assertEquals(5.0, IEEE754rUtil.max(5.0, Double.NaN, 1.0));
        assertEquals(5.0, IEEE754rUtil.max(5.0, 1.0, Double.NaN));
        assertEquals(5.0, IEEE754rUtil.max(Double.NaN, Double.NaN, 5.0));
        assertTrue(Double.isNaN(IEEE754rUtil.max(Double.NaN, Double.NaN, Double.NaN)));
    }

    @Test
    public void test_max_double_double_double_allEqual() {
        assertEquals(5.0, IEEE754rUtil.max(5.0, 5.0, 5.0));
    }

    @Test
    public void test_max_double_double_double_withInfinity() {
        assertEquals(5.0, IEEE754rUtil.max(Double.NEGATIVE_INFINITY, 3.0, 5.0));
        assertEquals(Double.POSITIVE_INFINITY, IEEE754rUtil.max(Double.POSITIVE_INFINITY, 1.0, 5.0));
        assertEquals(Double.POSITIVE_INFINITY, IEEE754rUtil.max(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.0));
    }

    // ===== max(double...) =====

    @Test
    public void test_max_double_array_normalValues() {
        assertEquals(7.0, IEEE754rUtil.max(new double[] { 3.0, 5.0, 1.0, 7.0 }));
        assertEquals(3.0, IEEE754rUtil.max(new double[] { 3.0, -5.0, 1.0, 0.0 }));
    }

    @Test
    public void test_max_double_array_singleElement() {
        assertEquals(5.0, IEEE754rUtil.max(new double[] { 5.0 }));
        assertTrue(Double.isNaN(IEEE754rUtil.max(new double[] { Double.NaN })));
    }

    @Test
    public void test_max_double_array_withNaN() {
        assertEquals(5.0, IEEE754rUtil.max(new double[] { 3.0, Double.NaN, 1.0, 5.0 }));
        assertEquals(1.0, IEEE754rUtil.max(new double[] { Double.NaN, Double.NaN, 1.0 }));
        assertTrue(Double.isNaN(IEEE754rUtil.max(new double[] { Double.NaN, Double.NaN, Double.NaN })));
    }

    @Test
    public void test_max_double_array_withInfinity() {
        assertEquals(5.0, IEEE754rUtil.max(new double[] { 3.0, Double.NEGATIVE_INFINITY, 5.0 }));
        assertEquals(Double.POSITIVE_INFINITY, IEEE754rUtil.max(new double[] { 3.0, Double.POSITIVE_INFINITY, 1.0, 5.0 }));
    }

    @Test
    public void test_max_double_array_allSameValues() {
        assertEquals(5.0, IEEE754rUtil.max(new double[] { 5.0, 5.0, 5.0, 5.0 }));
    }

    @Test
    public void test_max_double_array_largeArray() {
        double[] largeArray = new double[1000];
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = i * 1.5;
        }
        largeArray[500] = 9999.0;
        assertEquals(9999.0, IEEE754rUtil.max(largeArray));
    }

    @Test
    public void test_max_float_array_nullArray() {
        assertThrows(IllegalArgumentException.class, () -> IEEE754rUtil.max((float[]) null));
    }

    @Test
    public void test_max_float_array_emptyArray() {
        assertThrows(IllegalArgumentException.class, () -> IEEE754rUtil.max(new float[] {}));
    }

    @Test
    public void test_max_double_array_nullArray() {
        assertThrows(IllegalArgumentException.class, () -> IEEE754rUtil.max((double[]) null));
    }

    @Test
    public void test_max_double_array_emptyArray() {
        assertThrows(IllegalArgumentException.class, () -> IEEE754rUtil.max(new double[] {}));
    }

}
