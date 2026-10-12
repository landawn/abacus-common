package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Boundary regressions from the cumulative production-change audit. */
@Tag("unit")
class UtilitiesProductionChangeAudit20261008Test extends TestBase {
    @Test
    void exactLongAccessorNamesRemainAvailableBeyondTheCallerCacheLimit() throws Exception {
        final String name = "propertyWithANameThatExceedsTheCallerCacheAdmissionLimitButStillMustResolveByItsExactDeclaredGetterAndSetterNameWithoutDependingOnFuzzyAliasMatching";
        assertTrue(name.length() > 128);
        final LongAccessorBean bean = new LongAccessorBean();
        final var getter = Beans.getPropGetter(LongAccessorBean.class, name);
        final var setter = Beans.getPropSetter(LongAccessorBean.class, name);
        setter.invoke(bean, 37);
        assertEquals(37, getter.invoke(bean));
    }

    public static final class LongAccessorBean {
        private int value;

        public int getPropertyWithANameThatExceedsTheCallerCacheAdmissionLimitButStillMustResolveByItsExactDeclaredGetterAndSetterNameWithoutDependingOnFuzzyAliasMatching() {
            return value;
        }

        public void setPropertyWithANameThatExceedsTheCallerCacheAdmissionLimitButStillMustResolveByItsExactDeclaredGetterAndSetterNameWithoutDependingOnFuzzyAliasMatching(
                final int value) {
            this.value = value;
        }
    }

    @Test
    void medianPairsKeepBothSortedNanPayloadsAndLeaveEveryInputBitUnchanged() {
        // A single NaN never exercises the even-length lower-median tie: the lower partition needs multiple
        // different NaN payloads. Compare with a sorted copy rather than assuming how Arrays.sort orders NaNs.
        for (final int size : new int[] { 4, 5, 16, 17, 64, 65 }) {
            final float[] floats = new float[size + 2];
            final double[] doubles = new double[size + 2];
            for (int i = 0; i < floats.length; i++) {
                floats[i] = Float.intBitsToFloat(0x7fc00000 | (i + 1));
                doubles[i] = Double.longBitsToDouble(0x7ff8000000000000L | (i + 1));
            }
            // Padding is outside the requested range; retaining its bits also checks the defensive-copy boundary.
            floats[0] = -0f;
            doubles[0] = -0d;
            final int[] originalFloats = rawBits(floats);
            final long[] originalDoubles = rawBits(doubles);
            final float[] sortedFloats = Arrays.copyOfRange(floats, 1, size + 1);
            final double[] sortedDoubles = Arrays.copyOfRange(doubles, 1, size + 1);
            Arrays.sort(sortedFloats);
            Arrays.sort(sortedDoubles);

            final var actualFloats = Median.of(floats, 1, size + 1);
            final var actualDoubles = Median.of(doubles, 1, size + 1);
            final int lower = (size - 1) / 2;
            assertEquals(Float.floatToRawIntBits(sortedFloats[lower]), Float.floatToRawIntBits(actualFloats.left()));
            assertEquals(Double.doubleToRawLongBits(sortedDoubles[lower]), Double.doubleToRawLongBits(actualDoubles.left()));
            assertEquals(size % 2 == 0, actualFloats.right().isPresent());
            assertEquals(size % 2 == 0, actualDoubles.right().isPresent());
            if (size % 2 == 0) {
                assertEquals(Float.floatToRawIntBits(sortedFloats[size / 2]), Float.floatToRawIntBits(actualFloats.right().get()));
                assertEquals(Double.doubleToRawLongBits(sortedDoubles[size / 2]), Double.doubleToRawLongBits(actualDoubles.right().get()));
                assertTrue(Float.floatToRawIntBits(sortedFloats[0]) != Float.floatToRawIntBits(sortedFloats[lower]),
                        "distinct payloads must exercise selecting the last equal maximum in the lower partition");
            }
            assertArrayEquals(originalFloats, rawBits(floats));
            assertArrayEquals(originalDoubles, rawBits(doubles));
        }
    }

    @Test
    void longMedianSelectionKeepsCallerStorageAndExactBoundaryValues() {
        final long[] values = new long[70];
        for (int i = 0; i < values.length; i++) {
            values[i] = i % 2 == 0 ? Long.MAX_VALUE - i : Long.MIN_VALUE + i;
        }
        final long[] original = values.clone();
        for (final int end : new int[] { 67, 68 }) {
            final long[] sorted = Arrays.copyOfRange(values, 2, end);
            Arrays.sort(sorted);
            final var actual = Median.of(values, 2, end);
            assertEquals(sorted[(sorted.length - 1) / 2], actual.left().longValue());
            assertEquals(sorted.length % 2 == 0, actual.right().isPresent());
            if (actual.right().isPresent()) {
                assertEquals(sorted[sorted.length / 2], actual.right().get());
            }
            assertArrayEquals(original, values);
        }
    }

    private static int[] rawBits(final float[] values) {
        final int[] bits = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            bits[i] = Float.floatToRawIntBits(values[i]);
        }
        return bits;
    }

    private static long[] rawBits(final double[] values) {
        final long[] bits = new long[values.length];
        for (int i = 0; i < values.length; i++) {
            bits[i] = Double.doubleToRawLongBits(values[i]);
        }
        return bits;
    }
}
