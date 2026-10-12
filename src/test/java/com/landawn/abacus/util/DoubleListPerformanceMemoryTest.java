package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Performance and memory regression contracts for DoubleList. */
@Tag("slow-test")
public class DoubleListPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G024 end ----
    // ---- perf review 2026-10-08 G025 begin ----
    @Nested
    class G025 {
        // quiet NaN bit patterns with different signs and payloads; Double.equals treats them all as one value
        private final long[] quietNaNBits = { 0x7ff8000000000000L, 0x7ff8000000000001L, 0xfff8000000000000L, 0x7fffffffffffffffL, 0xfff80000000abcdeL };

        /**
         * Reference for {@code DoubleList.removeDuplicates()}: the raw bits of the first occurrence of each value under
         * {@code Double.equals} (that is, {@code Double.doubleToLongBits}), in encounter order.
         */
        private long[] firstOccurrenceBits(final double[] a, final int size) {
            final Set<Long> seen = new HashSet<>();
            final List<Long> kept = new ArrayList<>();

            for (int i = 0; i < size; i++) {
                if (seen.add(Double.doubleToLongBits(a[i]))) {
                    kept.add(Double.doubleToRawLongBits(a[i]));
                }
            }

            final long[] result = new long[kept.size()];

            for (int i = 0; i < result.length; i++) {
                result[i] = kept.get(i);
            }

            return result;
        }

        @SuppressWarnings("deprecation")
        private void assertRemoveDuplicatesMatchesReference(final double[] a, final int size, final String label) {
            final double[] backing = a.clone();
            final long[] expected = firstOccurrenceBits(backing, size);
            final double[] spare = Arrays.copyOfRange(backing, size, backing.length);
            final DoubleList list = DoubleList.of(backing, size);

            assertEquals(expected.length != size, list.removeDuplicates(), label);
            assertEquals(expected.length, list.size(), label);
            assertSame(backing, list.internalArray(), label);

            for (int i = 0; i < expected.length; i++) {
                assertEquals(expected[i], Double.doubleToRawLongBits(backing[i]), label + " index " + i);
            }

            for (int i = expected.length; i < size; i++) {
                assertEquals(0L, Double.doubleToRawLongBits(backing[i]), label + " cleared slot " + i);
            }

            for (int i = size; i < backing.length; i++) {
                assertEquals(Double.doubleToRawLongBits(spare[i - size]), Double.doubleToRawLongBits(backing[i]), label + " spare slot " + i);
            }
        }

        private double randomValue(final Random rnd, final int domain, final int size, final double[] specials) {
            switch (domain) {
                case 0: // few distinct values: NaN payload variants, signed zeros, infinities, extremes
                    return rnd.nextInt(4) == 0 ? Double.longBitsToDouble(quietNaNBits[rnd.nextInt(quietNaNBits.length)])
                            : specials[rnd.nextInt(specials.length)];
                case 1: // whole numbers with duplicates
                    return rnd.nextInt(Math.max(1, size / 2));
                case 2: // mostly distinct values
                    return rnd.nextDouble();
                default: { // random bit patterns; NaN patterns are replaced by quiet NaN payload variants
                    final double d = Double.longBitsToDouble(rnd.nextLong());
                    return Double.isNaN(d) ? Double.longBitsToDouble(quietNaNBits[rnd.nextInt(quietNaNBits.length)]) : d;
                }
            }
        }

        // G025-02: removeDuplicates keeps the first occurrence of each value bit for bit, the backing array, zeroed cleared slots and untouched spare slots
        @Test
        public void testRemoveDuplicates_matchesFirstOccurrenceReference() {
            final Random rnd = new Random(20261008L);
            final double[] specials = { 0.0d, -0.0d, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.MIN_VALUE, -Double.MIN_VALUE, Double.MAX_VALUE,
                    -Double.MAX_VALUE, 1d, -1d };
            final int[] sizes = { 2, 3, 4, 5, 8, 9, 15, 16, 17, 31, 32, 33, 63, 64, 65, 100, 127, 128, 129, 257, 1000, 3000 };

            for (final int size : sizes) {
                for (int domain = 0; domain < 4; domain++) {
                    for (int trial = 0; trial < 5; trial++) {
                        final double[] a = new double[size + rnd.nextInt(3)];

                        for (int i = 0; i < a.length; i++) {
                            a[i] = randomValue(rnd, domain, size, specials);
                        }

                        assertRemoveDuplicatesMatchesReference(a, size, "size=" + size + " domain=" + domain + " trial=" + trial);
                    }
                }
            }
        }

        // G025-02: keys that all share one home slot of N's primitive key set (an N.distinct-based removeDuplicates would hit its boxed fallback) keep the reference result
        @Test
        public void testRemoveDuplicates_collidingKeysMatchReference() {
            final long hashMultiplier = 0x9E3779B97F4A7C15L; // multiplier of the hash used by the primitive key set behind N.distinct
            long inverse = hashMultiplier;

            for (int i = 0; i < 6; i++) {
                inverse *= 2 - hashMultiplier * inverse;
            }

            final Random rnd = new Random(8L);

            for (final int distinct : new int[] { 70, 200, 5000 }) {
                final double[] a = new double[distinct * 2];

                for (int i = 0; i < a.length; i++) {
                    // the products with the multiplier are consecutive, so all keys start probing at the same slot
                    final double d = Double.longBitsToDouble((0x123456789L + rnd.nextInt(distinct)) * inverse);
                    a[i] = Double.isNaN(d) ? Double.longBitsToDouble(quietNaNBits[rnd.nextInt(quietNaNBits.length)]) : d;
                }

                assertRemoveDuplicatesMatchesReference(a, a.length, "colliding distinct=" + distinct);
            }
        }

        // G025-02: in-place semantics on a shared array: kept order, cleared slots, untouched spare slots, unchanged when unique, first NaN payload kept
        @Test
        @SuppressWarnings("deprecation")
        public void testRemoveDuplicates_sharedBackingArrayExamples() {
            final double[] shared = { 3d, 1d, 3d, 2d, 1d, 9d, 9d };
            final DoubleList list = DoubleList.of(shared, 5);

            assertTrue(list.removeDuplicates());
            assertSame(shared, list.internalArray());
            assertEquals(3, list.size());
            assertArrayEquals(new double[] { 3d, 1d, 2d, 0d, 0d, 9d, 9d }, shared);

            final double[] unique = { 3d, 1d, 2d, -0.0d, 0.0d, Double.NaN };
            final DoubleList uniqueList = DoubleList.of(unique);

            assertFalse(uniqueList.removeDuplicates());
            assertSame(unique, uniqueList.internalArray());
            assertEquals(6, uniqueList.size());
            assertArrayEquals(new double[] { 3d, 1d, 2d, -0.0d, 0.0d, Double.NaN }, unique);

            final double firstNaN = Double.longBitsToDouble(0x7ff8000000000001L);
            final double[] nans = { 2d, firstNaN, 1d, Double.NaN, 2d };
            final DoubleList nanList = DoubleList.of(nans);

            assertTrue(nanList.removeDuplicates());
            assertEquals(3, nanList.size());
            assertEquals(0x7ff8000000000001L, Double.doubleToRawLongBits(nans[1]));
            assertArrayEquals(new double[] { 2d, firstNaN, 1d, 0d, 0d }, nans);
        }
    }
}
