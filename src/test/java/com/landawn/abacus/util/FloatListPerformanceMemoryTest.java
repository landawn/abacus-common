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

/** Performance and memory regression contracts for FloatList. */
@Tag("slow-test")
public class FloatListPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G026 end ----
    // ---- perf review 2026-10-08 G027 begin ----
    @Nested
    class G027 {
        // quiet NaN bit patterns with different signs and payloads; Float.equals treats them all as one value
        private final int[] quietNaNBits = { 0x7fc00000, 0x7fc00001, 0xffc00000, 0x7fffffff, 0xfff00abc };

        /**
         * Reference for {@code FloatList.removeDuplicates()}: the raw bits of the first occurrence of each value under
         * {@code Float.equals} (that is, {@code Float.floatToIntBits}), in encounter order.
         */
        private int[] firstOccurrenceBits(final float[] a, final int size) {
            final Set<Integer> seen = new HashSet<>();
            final List<Integer> kept = new ArrayList<>();

            for (int i = 0; i < size; i++) {
                if (seen.add(Float.floatToIntBits(a[i]))) {
                    kept.add(Float.floatToRawIntBits(a[i]));
                }
            }

            final int[] result = new int[kept.size()];

            for (int i = 0; i < result.length; i++) {
                result[i] = kept.get(i);
            }

            return result;
        }

        @SuppressWarnings("deprecation")
        private void assertRemoveDuplicatesMatchesReference(final float[] a, final int size, final String label) {
            final float[] backing = a.clone();
            final int[] expected = firstOccurrenceBits(backing, size);
            final float[] spare = Arrays.copyOfRange(backing, size, backing.length);
            final FloatList list = FloatList.of(backing, size);

            assertEquals(expected.length != size, list.removeDuplicates(), label);
            assertEquals(expected.length, list.size(), label);
            assertSame(backing, list.internalArray(), label);

            for (int i = 0; i < expected.length; i++) {
                assertEquals(expected[i], Float.floatToRawIntBits(backing[i]), label + " index " + i);
            }

            for (int i = expected.length; i < size; i++) {
                assertEquals(0, Float.floatToRawIntBits(backing[i]), label + " cleared slot " + i);
            }

            for (int i = size; i < backing.length; i++) {
                assertEquals(Float.floatToRawIntBits(spare[i - size]), Float.floatToRawIntBits(backing[i]), label + " spare slot " + i);
            }
        }

        private float randomValue(final Random rnd, final int domain, final int size, final float[] specials) {
            switch (domain) {
                case 0: // few distinct values: NaN payload variants, signed zeros, infinities, extremes
                    return rnd.nextInt(4) == 0 ? Float.intBitsToFloat(quietNaNBits[rnd.nextInt(quietNaNBits.length)]) : specials[rnd.nextInt(specials.length)];
                case 1: // whole numbers with duplicates
                    return rnd.nextInt(Math.max(1, size / 2));
                case 2: // mostly distinct values
                    return rnd.nextFloat();
                default: { // random bit patterns; NaN patterns are replaced by quiet NaN payload variants
                    final float f = Float.intBitsToFloat(rnd.nextInt());
                    return Float.isNaN(f) ? Float.intBitsToFloat(quietNaNBits[rnd.nextInt(quietNaNBits.length)]) : f;
                }
            }
        }

        // G027-01 (OPEN): removeDuplicates keeps first occurrences bit for bit, the backing array, cleared slots and spare capacity; pins any primitive-set port of the unsorted path
        @Test
        public void testRemoveDuplicates_matchesFirstOccurrenceReference() {
            final Random rnd = new Random(20261008L);
            final float[] specials = { 0.0f, -0.0f, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.MIN_VALUE, -Float.MIN_VALUE, Float.MAX_VALUE,
                    -Float.MAX_VALUE, 1f, -1f };
            final int[] sizes = { 2, 3, 4, 5, 8, 9, 15, 16, 17, 31, 32, 33, 63, 64, 65, 100, 127, 128, 129, 257, 1000, 3000 };

            for (final int size : sizes) {
                for (int domain = 0; domain < 4; domain++) {
                    for (int trial = 0; trial < 5; trial++) {
                        final float[] a = new float[size + rnd.nextInt(3)];

                        for (int i = 0; i < a.length; i++) {
                            a[i] = randomValue(rnd, domain, size, specials);
                        }

                        assertRemoveDuplicatesMatchesReference(a, size, "size=" + size + " domain=" + domain + " trial=" + trial);
                    }
                }
            }
        }

        // G027-01 (OPEN): keys that all share one home slot of the primitive key set in N (the structure a port would use; they force its boxed fallback) keep the same result
        @Test
        public void testRemoveDuplicates_collidingKeysMatchReference() {
            final int hashMultiplier = 0x9E3779B9; // multiplier of the hash used by the primitive key set in N (N.distinct)
            int inverse = hashMultiplier;

            for (int i = 0; i < 5; i++) {
                inverse *= 2 - hashMultiplier * inverse;
            }

            final Random rnd = new Random(8L);

            for (final int distinct : new int[] { 70, 200, 5000 }) {
                final float[] a = new float[distinct * 2];

                for (int i = 0; i < a.length; i++) {
                    // the products with the multiplier are consecutive, so all keys start probing at the same slot
                    final float f = Float.intBitsToFloat((0x91A00000 + rnd.nextInt(distinct)) * inverse);
                    a[i] = Float.isNaN(f) ? Float.intBitsToFloat(quietNaNBits[rnd.nextInt(quietNaNBits.length)]) : f;
                }

                assertRemoveDuplicatesMatchesReference(a, a.length, "colliding distinct=" + distinct);
            }
        }

        // G027-01 (OPEN): in-place semantics on a shared array: kept order, cleared slots, untouched spare slots, unchanged when unique, first NaN payload kept
        @Test
        @SuppressWarnings("deprecation")
        public void testRemoveDuplicates_sharedBackingArrayExamples() {
            final float[] shared = { 3f, 1f, 3f, 2f, 1f, 9f, 9f };
            final FloatList list = FloatList.of(shared, 5);

            assertTrue(list.removeDuplicates());
            assertSame(shared, list.internalArray());
            assertEquals(3, list.size());
            assertArrayEquals(new float[] { 3f, 1f, 2f, 0f, 0f, 9f, 9f }, shared);

            final float[] unique = { 3f, 1f, 2f, -0.0f, 0.0f, Float.NaN };
            final FloatList uniqueList = FloatList.of(unique);

            assertFalse(uniqueList.removeDuplicates());
            assertSame(unique, uniqueList.internalArray());
            assertEquals(6, uniqueList.size());
            assertArrayEquals(new float[] { 3f, 1f, 2f, -0.0f, 0.0f, Float.NaN }, unique);

            final float firstNaN = Float.intBitsToFloat(0x7fc00001);
            final float[] nans = { 2f, firstNaN, 1f, Float.NaN, 2f };
            final FloatList nanList = FloatList.of(nans);

            assertTrue(nanList.removeDuplicates());
            assertEquals(3, nanList.size());
            assertEquals(0x7fc00001, Float.floatToRawIntBits(nans[1]));
            assertArrayEquals(new float[] { 2f, firstNaN, 1f, 0f, 0f }, nans);
        }
    }
}
