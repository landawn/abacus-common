package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

@Tag("old-test")
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

}
