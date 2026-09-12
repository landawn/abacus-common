package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Verifies that the <b>primitive</b> {@code float}/{@code double} {@code min}/{@code max} overloads of
 * {@link N} propagate NaN per the {@link Math#min(float, float)} / {@link Math#max(float, float)} contract:
 * any NaN input yields a NaN result. Behavior for non-NaN inputs (including {@code +0.0} vs {@code -0.0})
 * and empty/null inputs is also covered.
 *
 * <p>The boxed and extractor-based families deliberately follow a <i>different</i> rule - the
 * {@code Double.compare} total order, in which NaN is the largest value and so is never a minimum - and are
 * covered by {@code CommonUtilNReviewFixes20260831Test} instead.
 */
public class MinMaxNaNPropagationTest extends TestBase {

    private static final float DELTA_F = 0f;
    private static final double DELTA = 0d;

    // -----------------------------------------------------------------
    // N.min / N.max — 2-arg primitives (already correct, tested for completeness)
    // -----------------------------------------------------------------

    @Test
    public void nMin2_float_propagatesNaN() {
        assertTrue(Float.isNaN(N.min(Float.NaN, 1.0f)));
        assertTrue(Float.isNaN(N.min(1.0f, Float.NaN)));
        assertTrue(Float.isNaN(N.min(Float.NaN, Float.NaN)));
        assertEquals(1.0f, N.min(1.0f, 2.0f), DELTA_F);
        // -0.0 < +0.0 per Math.min
        assertEquals(-0.0f, N.min(0.0f, -0.0f), DELTA_F);
        assertTrue(Float.floatToRawIntBits(-0.0f) == Float.floatToRawIntBits(N.min(0.0f, -0.0f)));
    }

    @Test
    public void nMax2_float_propagatesNaN() {
        assertTrue(Float.isNaN(N.max(Float.NaN, 1.0f)));
        assertTrue(Float.isNaN(N.max(1.0f, Float.NaN)));
        assertTrue(Float.isNaN(N.max(Float.NaN, Float.NaN)));
        assertEquals(2.0f, N.max(1.0f, 2.0f), DELTA_F);
        // +0.0 > -0.0 per Math.max
        assertEquals(0.0f, N.max(0.0f, -0.0f), DELTA_F);
        assertTrue(Float.floatToRawIntBits(0.0f) == Float.floatToRawIntBits(N.max(0.0f, -0.0f)));
    }

    @Test
    public void nMin2_double_propagatesNaN() {
        assertTrue(Double.isNaN(N.min(Double.NaN, 1.0)));
        assertTrue(Double.isNaN(N.min(1.0, Double.NaN)));
        assertTrue(Double.isNaN(N.min(Double.NaN, Double.NaN)));
        assertEquals(1.0, N.min(1.0, 2.0), DELTA);
        assertEquals(-0.0, N.min(0.0, -0.0), DELTA);
        assertTrue(Double.doubleToRawLongBits(-0.0) == Double.doubleToRawLongBits(N.min(0.0, -0.0)));
    }

    @Test
    public void nMax2_double_propagatesNaN() {
        assertTrue(Double.isNaN(N.max(Double.NaN, 1.0)));
        assertTrue(Double.isNaN(N.max(1.0, Double.NaN)));
        assertTrue(Double.isNaN(N.max(Double.NaN, Double.NaN)));
        assertEquals(2.0, N.max(1.0, 2.0), DELTA);
        assertEquals(0.0, N.max(0.0, -0.0), DELTA);
        assertTrue(Double.doubleToRawLongBits(0.0) == Double.doubleToRawLongBits(N.max(0.0, -0.0)));
    }

    // -----------------------------------------------------------------
    // N.min / N.max — 3-arg primitives
    // -----------------------------------------------------------------

    @Test
    public void nMin3_float_propagatesNaN_anyPosition() {
        assertTrue(Float.isNaN(N.min(Float.NaN, 1.0f, 2.0f))); // first
        assertTrue(Float.isNaN(N.min(1.0f, Float.NaN, 2.0f))); // middle
        assertTrue(Float.isNaN(N.min(1.0f, 2.0f, Float.NaN))); // last
        assertEquals(1.0f, N.min(2.0f, 1.0f, 3.0f), DELTA_F);
    }

    @Test
    public void nMax3_float_propagatesNaN_anyPosition() {
        assertTrue(Float.isNaN(N.max(Float.NaN, 1.0f, 2.0f)));
        assertTrue(Float.isNaN(N.max(1.0f, Float.NaN, 2.0f)));
        assertTrue(Float.isNaN(N.max(1.0f, 2.0f, Float.NaN)));
        assertEquals(3.0f, N.max(2.0f, 1.0f, 3.0f), DELTA_F);
    }

    @Test
    public void nMin3_double_propagatesNaN_anyPosition() {
        assertTrue(Double.isNaN(N.min(Double.NaN, 1.0, 2.0)));
        assertTrue(Double.isNaN(N.min(1.0, Double.NaN, 2.0)));
        assertTrue(Double.isNaN(N.min(1.0, 2.0, Double.NaN)));
        assertEquals(1.0, N.min(2.0, 1.0, 3.0), DELTA);
    }

    @Test
    public void nMax3_double_propagatesNaN_anyPosition() {
        assertTrue(Double.isNaN(N.max(Double.NaN, 1.0, 2.0)));
        assertTrue(Double.isNaN(N.max(1.0, Double.NaN, 2.0)));
        assertTrue(Double.isNaN(N.max(1.0, 2.0, Double.NaN)));
        assertEquals(3.0, N.max(2.0, 1.0, 3.0), DELTA);
    }

    // -----------------------------------------------------------------
    // N.min(float...) / N.max(float...)  — varargs / array
    // -----------------------------------------------------------------

    @Test
    public void nMinFloatArray_singleElementNaN_returnsNaN() {
        assertTrue(Float.isNaN(N.min(new float[] { Float.NaN })));
    }

    @Test
    public void nMinFloatArray_nanFirst_returnsNaN() {
        assertTrue(Float.isNaN(N.min(new float[] { Float.NaN, 1.0f, 2.0f, 3.0f })));
    }

    @Test
    public void nMinFloatArray_nanLast_returnsNaN() {
        assertTrue(Float.isNaN(N.min(new float[] { 1.0f, 2.0f, 3.0f, Float.NaN })));
    }

    @Test
    public void nMinFloatArray_nanMiddle_returnsNaN() {
        assertTrue(Float.isNaN(N.min(new float[] { 1.0f, Float.NaN, 3.0f })));
    }

    @Test
    public void nMinFloatArray_multipleNaN_returnsNaN() {
        assertTrue(Float.isNaN(N.min(new float[] { Float.NaN, 1.0f, Float.NaN, 2.0f, Float.NaN })));
    }

    @Test
    public void nMinFloatArray_nanWithInfinities_returnsNaN() {
        assertTrue(Float.isNaN(N.min(new float[] { Float.NEGATIVE_INFINITY, Float.NaN, Float.POSITIVE_INFINITY })));
        assertTrue(Float.isNaN(N.min(new float[] { Float.POSITIVE_INFINITY, Float.NaN })));
        assertTrue(Float.isNaN(N.min(new float[] { Float.NEGATIVE_INFINITY, Float.NaN })));
    }

    @Test
    public void nMinFloatArray_noNaN_normal() {
        assertEquals(0.5f, N.min(new float[] { 1.5f, 2.3f, 0.5f }), DELTA_F);
        assertEquals(Float.NEGATIVE_INFINITY, N.min(new float[] { -1.0f, Float.NEGATIVE_INFINITY, 1.0f }), DELTA_F);
    }

    @Test
    public void nMaxFloatArray_singleElementNaN_returnsNaN() {
        assertTrue(Float.isNaN(N.max(new float[] { Float.NaN })));
    }

    @Test
    public void nMaxFloatArray_nanFirst_returnsNaN() {
        assertTrue(Float.isNaN(N.max(new float[] { Float.NaN, 1.0f, 2.0f, 3.0f })));
    }

    @Test
    public void nMaxFloatArray_nanLast_returnsNaN() {
        assertTrue(Float.isNaN(N.max(new float[] { 1.0f, 2.0f, 3.0f, Float.NaN })));
    }

    @Test
    public void nMaxFloatArray_nanMiddle_returnsNaN() {
        assertTrue(Float.isNaN(N.max(new float[] { 1.0f, Float.NaN, 3.0f })));
    }

    @Test
    public void nMaxFloatArray_multipleNaN_returnsNaN() {
        assertTrue(Float.isNaN(N.max(new float[] { Float.NaN, 1.0f, Float.NaN, 2.0f, Float.NaN })));
    }

    @Test
    public void nMaxFloatArray_nanWithInfinities_returnsNaN() {
        assertTrue(Float.isNaN(N.max(new float[] { Float.NEGATIVE_INFINITY, Float.NaN, Float.POSITIVE_INFINITY })));
        assertTrue(Float.isNaN(N.max(new float[] { Float.POSITIVE_INFINITY, Float.NaN })));
        assertTrue(Float.isNaN(N.max(new float[] { Float.NEGATIVE_INFINITY, Float.NaN })));
    }

    @Test
    public void nMaxFloatArray_noNaN_normal() {
        assertEquals(3.2f, N.max(new float[] { 1.5f, 2.8f, 0.5f, 3.2f }), DELTA_F);
        assertEquals(Float.POSITIVE_INFINITY, N.max(new float[] { -1.0f, Float.POSITIVE_INFINITY, 1.0f }), DELTA_F);
    }

    // -----------------------------------------------------------------
    // N.min(float[], int, int) / N.max(float[], int, int) — range overload
    // -----------------------------------------------------------------

    @Test
    public void nMinFloatArrayRange_propagatesNaNInsideRange() {
        final float[] withNaN = { 1.0f, Float.NaN, 3.0f, 4.0f };
        assertTrue(Float.isNaN(N.min(withNaN, 0, 4)));
        assertTrue(Float.isNaN(N.min(withNaN, 1, 4)));
        // Range that excludes NaN: still normal.
        assertEquals(3.0f, N.min(withNaN, 2, 4), DELTA_F);
        assertEquals(1.0f, N.min(withNaN, 0, 1), DELTA_F);
    }

    @Test
    public void nMaxFloatArrayRange_propagatesNaNInsideRange() {
        final float[] withNaN = { 1.0f, Float.NaN, 3.0f, 4.0f };
        assertTrue(Float.isNaN(N.max(withNaN, 0, 4)));
        assertTrue(Float.isNaN(N.max(withNaN, 1, 4)));
        // Range that excludes NaN: still normal.
        assertEquals(4.0f, N.max(withNaN, 2, 4), DELTA_F);
        assertEquals(1.0f, N.max(withNaN, 0, 1), DELTA_F);
    }

    // -----------------------------------------------------------------
    // N.min(double...) / N.max(double...) — varargs / array
    // -----------------------------------------------------------------

    @Test
    public void nMinDoubleArray_singleElementNaN_returnsNaN() {
        assertTrue(Double.isNaN(N.min(new double[] { Double.NaN })));
    }

    @Test
    public void nMinDoubleArray_nanFirst_returnsNaN() {
        assertTrue(Double.isNaN(N.min(new double[] { Double.NaN, 1.0, 2.0, 3.0 })));
    }

    @Test
    public void nMinDoubleArray_nanLast_returnsNaN() {
        assertTrue(Double.isNaN(N.min(new double[] { 1.0, 2.0, 3.0, Double.NaN })));
    }

    @Test
    public void nMinDoubleArray_nanMiddle_returnsNaN() {
        assertTrue(Double.isNaN(N.min(new double[] { 1.0, Double.NaN, 3.0 })));
    }

    @Test
    public void nMinDoubleArray_multipleNaN_returnsNaN() {
        assertTrue(Double.isNaN(N.min(new double[] { Double.NaN, 1.0, Double.NaN, 2.0, Double.NaN })));
    }

    @Test
    public void nMinDoubleArray_nanWithInfinities_returnsNaN() {
        assertTrue(Double.isNaN(N.min(new double[] { Double.NEGATIVE_INFINITY, Double.NaN, Double.POSITIVE_INFINITY })));
        assertTrue(Double.isNaN(N.min(new double[] { Double.POSITIVE_INFINITY, Double.NaN })));
        assertTrue(Double.isNaN(N.min(new double[] { Double.NEGATIVE_INFINITY, Double.NaN })));
    }

    @Test
    public void nMinDoubleArray_noNaN_normal() {
        assertEquals(0.5, N.min(new double[] { 1.5, 2.3, 0.5 }), DELTA);
        assertEquals(Double.NEGATIVE_INFINITY, N.min(new double[] { -1.0, Double.NEGATIVE_INFINITY, 1.0 }), DELTA);
    }

    @Test
    public void nMaxDoubleArray_singleElementNaN_returnsNaN() {
        assertTrue(Double.isNaN(N.max(new double[] { Double.NaN })));
    }

    @Test
    public void nMaxDoubleArray_nanFirst_returnsNaN() {
        assertTrue(Double.isNaN(N.max(new double[] { Double.NaN, 1.0, 2.0, 3.0 })));
    }

    @Test
    public void nMaxDoubleArray_nanLast_returnsNaN() {
        assertTrue(Double.isNaN(N.max(new double[] { 1.0, 2.0, 3.0, Double.NaN })));
    }

    @Test
    public void nMaxDoubleArray_nanMiddle_returnsNaN() {
        assertTrue(Double.isNaN(N.max(new double[] { 1.0, Double.NaN, 3.0 })));
    }

    @Test
    public void nMaxDoubleArray_multipleNaN_returnsNaN() {
        assertTrue(Double.isNaN(N.max(new double[] { Double.NaN, 1.0, Double.NaN, 2.0, Double.NaN })));
    }

    @Test
    public void nMaxDoubleArray_nanWithInfinities_returnsNaN() {
        assertTrue(Double.isNaN(N.max(new double[] { Double.NEGATIVE_INFINITY, Double.NaN, Double.POSITIVE_INFINITY })));
        assertTrue(Double.isNaN(N.max(new double[] { Double.POSITIVE_INFINITY, Double.NaN })));
        assertTrue(Double.isNaN(N.max(new double[] { Double.NEGATIVE_INFINITY, Double.NaN })));
    }

    @Test
    public void nMaxDoubleArray_noNaN_normal() {
        assertEquals(3.2, N.max(new double[] { 1.5, 2.8, 0.5, 3.2 }), DELTA);
        assertEquals(Double.POSITIVE_INFINITY, N.max(new double[] { -1.0, Double.POSITIVE_INFINITY, 1.0 }), DELTA);
    }

    // -----------------------------------------------------------------
    // N.min(double[], int, int) / N.max(double[], int, int) — range overload
    // -----------------------------------------------------------------

    @Test
    public void nMinDoubleArrayRange_propagatesNaNInsideRange() {
        final double[] withNaN = { 1.0, Double.NaN, 3.0, 4.0 };
        assertTrue(Double.isNaN(N.min(withNaN, 0, 4)));
        assertTrue(Double.isNaN(N.min(withNaN, 1, 4)));
        assertEquals(3.0, N.min(withNaN, 2, 4), DELTA);
        assertEquals(1.0, N.min(withNaN, 0, 1), DELTA);
    }

    @Test
    public void nMaxDoubleArrayRange_propagatesNaNInsideRange() {
        final double[] withNaN = { 1.0, Double.NaN, 3.0, 4.0 };
        assertTrue(Double.isNaN(N.max(withNaN, 0, 4)));
        assertTrue(Double.isNaN(N.max(withNaN, 1, 4)));
        assertEquals(4.0, N.max(withNaN, 2, 4), DELTA);
        assertEquals(1.0, N.max(withNaN, 0, 1), DELTA);
    }

    // -----------------------------------------------------------------
    // Iterables.min(float...) / max(float...) — wraps result in OptionalFloat
    // -----------------------------------------------------------------

    @Test
    public void iterablesMinFloat_propagatesNaN() {
        assertTrue(Iterables.min(new float[] { Float.NaN }).isPresent());
        assertTrue(Float.isNaN(Iterables.min(new float[] { Float.NaN }).get()));
        assertTrue(Float.isNaN(Iterables.min(new float[] { Float.NaN, 1.0f, 2.0f }).get()));
        assertTrue(Float.isNaN(Iterables.min(new float[] { 1.0f, Float.NaN, 2.0f }).get()));
        assertTrue(Float.isNaN(Iterables.min(new float[] { 1.0f, 2.0f, Float.NaN }).get()));
        assertTrue(Float.isNaN(Iterables.min(new float[] { Float.NaN, Float.NaN }).get()));
        assertTrue(Float.isNaN(Iterables.min(new float[] { Float.NEGATIVE_INFINITY, Float.NaN }).get()));
        assertTrue(Float.isNaN(Iterables.min(new float[] { Float.POSITIVE_INFINITY, Float.NaN }).get()));

        assertEquals(0.5f, Iterables.min(new float[] { 1.5f, 2.3f, 0.5f }).get(), DELTA_F);
    }

    @Test
    public void iterablesMaxFloat_propagatesNaN() {
        assertTrue(Float.isNaN(Iterables.max(new float[] { Float.NaN }).get()));
        assertTrue(Float.isNaN(Iterables.max(new float[] { Float.NaN, 1.0f, 2.0f }).get()));
        assertTrue(Float.isNaN(Iterables.max(new float[] { 1.0f, Float.NaN, 2.0f }).get()));
        assertTrue(Float.isNaN(Iterables.max(new float[] { 1.0f, 2.0f, Float.NaN }).get()));
        assertTrue(Float.isNaN(Iterables.max(new float[] { Float.NaN, Float.NaN }).get()));
        assertTrue(Float.isNaN(Iterables.max(new float[] { Float.NEGATIVE_INFINITY, Float.NaN }).get()));
        assertTrue(Float.isNaN(Iterables.max(new float[] { Float.POSITIVE_INFINITY, Float.NaN }).get()));

        assertEquals(3.2f, Iterables.max(new float[] { 1.5f, 2.8f, 0.5f, 3.2f }).get(), DELTA_F);
    }

    @Test
    public void iterablesMinFloat_emptyOrNull_returnsEmptyOptional() {
        assertFalse(Iterables.min(new float[0]).isPresent());
        assertFalse(Iterables.min((float[]) null).isPresent());
    }

    @Test
    public void iterablesMaxFloat_emptyOrNull_returnsEmptyOptional() {
        assertFalse(Iterables.max(new float[0]).isPresent());
        assertFalse(Iterables.max((float[]) null).isPresent());
    }

    // -----------------------------------------------------------------
    // Iterables.min(double...) / max(double...) — wraps result in OptionalDouble
    // -----------------------------------------------------------------

    @Test
    public void iterablesMinDouble_propagatesNaN() {
        assertTrue(Double.isNaN(Iterables.min(new double[] { Double.NaN }).get()));
        assertTrue(Double.isNaN(Iterables.min(new double[] { Double.NaN, 1.0, 2.0 }).get()));
        assertTrue(Double.isNaN(Iterables.min(new double[] { 1.0, Double.NaN, 2.0 }).get()));
        assertTrue(Double.isNaN(Iterables.min(new double[] { 1.0, 2.0, Double.NaN }).get()));
        assertTrue(Double.isNaN(Iterables.min(new double[] { Double.NaN, Double.NaN }).get()));
        assertTrue(Double.isNaN(Iterables.min(new double[] { Double.NEGATIVE_INFINITY, Double.NaN }).get()));
        assertTrue(Double.isNaN(Iterables.min(new double[] { Double.POSITIVE_INFINITY, Double.NaN }).get()));

        assertEquals(0.5, Iterables.min(new double[] { 1.5, 2.3, 0.5 }).get(), DELTA);
    }

    @Test
    public void iterablesMaxDouble_propagatesNaN() {
        assertTrue(Double.isNaN(Iterables.max(new double[] { Double.NaN }).get()));
        assertTrue(Double.isNaN(Iterables.max(new double[] { Double.NaN, 1.0, 2.0 }).get()));
        assertTrue(Double.isNaN(Iterables.max(new double[] { 1.0, Double.NaN, 2.0 }).get()));
        assertTrue(Double.isNaN(Iterables.max(new double[] { 1.0, 2.0, Double.NaN }).get()));
        assertTrue(Double.isNaN(Iterables.max(new double[] { Double.NaN, Double.NaN }).get()));
        assertTrue(Double.isNaN(Iterables.max(new double[] { Double.NEGATIVE_INFINITY, Double.NaN }).get()));
        assertTrue(Double.isNaN(Iterables.max(new double[] { Double.POSITIVE_INFINITY, Double.NaN }).get()));

        assertEquals(3.2, Iterables.max(new double[] { 1.5, 2.8, 0.5, 3.2 }).get(), DELTA);
    }

    @Test
    public void iterablesMinDouble_emptyOrNull_returnsEmptyOptional() {
        assertFalse(Iterables.min(new double[0]).isPresent());
        assertFalse(Iterables.min((double[]) null).isPresent());
    }

    @Test
    public void iterablesMaxDouble_emptyOrNull_returnsEmptyOptional() {
        assertFalse(Iterables.max(new double[0]).isPresent());
        assertFalse(Iterables.max((double[]) null).isPresent());
    }

    // -----------------------------------------------------------------
    // Cross-class consistency: N and Iterables must agree on the same input
    // -----------------------------------------------------------------

    @Test
    public void nAndIterables_floatArrayMin_agree() {
        final float[][] inputs = { { 1.0f, 2.0f, 3.0f }, { 1.0f, Float.NaN, 3.0f }, { Float.NaN },
                { Float.NEGATIVE_INFINITY, Float.NaN, Float.POSITIVE_INFINITY }, { Float.NaN, Float.NaN, Float.NaN } };
        for (float[] in : inputs) {
            final float byN = N.min(in);
            final float byIter = Iterables.min(in).get();
            if (Float.isNaN(byN)) {
                assertTrue(Float.isNaN(byIter), "Iterables.min disagreed with N.min for " + java.util.Arrays.toString(in));
            } else {
                assertEquals(byN, byIter, DELTA_F, "Iterables.min disagreed with N.min for " + java.util.Arrays.toString(in));
            }
        }
    }

    @Test
    public void nAndIterables_floatArrayMax_agree() {
        final float[][] inputs = { { 1.0f, 2.0f, 3.0f }, { 1.0f, Float.NaN, 3.0f }, { Float.NaN },
                { Float.NEGATIVE_INFINITY, Float.NaN, Float.POSITIVE_INFINITY }, { Float.NaN, Float.NaN, Float.NaN } };
        for (float[] in : inputs) {
            final float byN = N.max(in);
            final float byIter = Iterables.max(in).get();
            if (Float.isNaN(byN)) {
                assertTrue(Float.isNaN(byIter), "Iterables.max disagreed with N.max for " + java.util.Arrays.toString(in));
            } else {
                assertEquals(byN, byIter, DELTA_F, "Iterables.max disagreed with N.max for " + java.util.Arrays.toString(in));
            }
        }
    }

    @Test
    public void nAndIterables_doubleArrayMin_agree() {
        final double[][] inputs = { { 1.0, 2.0, 3.0 }, { 1.0, Double.NaN, 3.0 }, { Double.NaN },
                { Double.NEGATIVE_INFINITY, Double.NaN, Double.POSITIVE_INFINITY }, { Double.NaN, Double.NaN, Double.NaN } };
        for (double[] in : inputs) {
            final double byN = N.min(in);
            final double byIter = Iterables.min(in).get();
            if (Double.isNaN(byN)) {
                assertTrue(Double.isNaN(byIter), "Iterables.min disagreed with N.min for " + java.util.Arrays.toString(in));
            } else {
                assertEquals(byN, byIter, DELTA, "Iterables.min disagreed with N.min for " + java.util.Arrays.toString(in));
            }
        }
    }

    @Test
    public void nAndIterables_doubleArrayMax_agree() {
        final double[][] inputs = { { 1.0, 2.0, 3.0 }, { 1.0, Double.NaN, 3.0 }, { Double.NaN },
                { Double.NEGATIVE_INFINITY, Double.NaN, Double.POSITIVE_INFINITY }, { Double.NaN, Double.NaN, Double.NaN } };
        for (double[] in : inputs) {
            final double byN = N.max(in);
            final double byIter = Iterables.max(in).get();
            if (Double.isNaN(byN)) {
                assertTrue(Double.isNaN(byIter), "Iterables.max disagreed with N.max for " + java.util.Arrays.toString(in));
            } else {
                assertEquals(byN, byIter, DELTA, "Iterables.max disagreed with N.max for " + java.util.Arrays.toString(in));
            }
        }
    }

    // -----------------------------------------------------------------
    // Math.min/max parity check — for every NaN-containing input, our result
    // must match the result of folding via Math.min/max over the same array.
    // -----------------------------------------------------------------

    @Test
    public void floatArrayMin_matchesMathMinFold() {
        final float[][] cases = { { 1.0f, 2.0f, 3.0f }, { 1.0f, Float.NaN, 3.0f }, { Float.NaN, 1.0f, 2.0f }, { 1.0f, 2.0f, Float.NaN },
                { Float.POSITIVE_INFINITY, Float.NaN }, { -0.0f, 0.0f, Float.NaN }, { 5.0f } };
        for (float[] in : cases) {
            float expected = in[0];
            for (int i = 1; i < in.length; i++) {
                expected = Math.min(expected, in[i]);
            }
            final float actual = N.min(in);
            if (Float.isNaN(expected)) {
                assertTrue(Float.isNaN(actual));
            } else {
                assertEquals(expected, actual, DELTA_F);
            }
        }
    }

    @Test
    public void doubleArrayMax_matchesMathMaxFold() {
        final double[][] cases = { { 1.0, 2.0, 3.0 }, { 1.0, Double.NaN, 3.0 }, { Double.NaN, 1.0, 2.0 }, { 1.0, 2.0, Double.NaN },
                { Double.NEGATIVE_INFINITY, Double.NaN }, { -0.0, 0.0, Double.NaN }, { 5.0 } };
        for (double[] in : cases) {
            double expected = in[0];
            for (int i = 1; i < in.length; i++) {
                expected = Math.max(expected, in[i]);
            }
            final double actual = N.max(in);
            if (Double.isNaN(expected)) {
                assertTrue(Double.isNaN(actual));
            } else {
                assertEquals(expected, actual, DELTA);
            }
        }
    }
}
