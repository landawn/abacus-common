package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Performance and memory regression contracts for ShortList. */
@Tag("slow-test")
public class ShortListPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G066 end ----
    // ---- perf review 2026-10-08 G067 begin ----
    @Nested
    class G067 {

        private static final String[] RANGES = { "small", "audio", "full" };

        private short[] randomShorts(final Random random, final int length, final String range) {
            final short[] a = new short[length];

            for (int i = 0; i < length; i++) {
                if ("small".equals(range)) {
                    a[i] = (short) (random.nextInt(256) - 128);
                } else if ("audio".equals(range)) {
                    a[i] = (short) (random.nextInt(6001) - 3000);
                } else {
                    a[i] = (short) random.nextInt(65536);
                }
            }

            if (length > 5 && "full".equals(range)) {
                a[0] = Short.MIN_VALUE;
                a[1] = Short.MAX_VALUE;
                a[2] = (short) -1;
                a[3] = (short) 0;
                a[4] = (short) 128;
                a[5] = (short) -129;
            }

            return a;
        }

        private Map<Short, Integer> counts(final short[] a) {
            final Map<Short, Integer> counts = new HashMap<>();

            for (final short e : a) {
                counts.merge(e, 1, Integer::sum);
            }

            return counts;
        }

        private boolean take(final Map<Short, Integer> counts, final short e) {
            final Integer count = counts.get(e);

            if (count == null || count == 0) {
                return false;
            }

            counts.put(e, count - 1);
            return true;
        }

        /**
         * Boxed reference for the occurrence-based set operations: {intersection, difference, symmetricDifference}, where the
         * symmetric difference appends b's unmatched occurrences in index order.
         */
        private short[][] reference(final short[] a, final short[] b) {
            final short[] intersection = new short[a.length];
            final short[] difference = new short[a.length];
            final short[] symmetricDifference = new short[a.length + b.length];
            int ni = 0;
            int nd = 0;
            int ns = 0;

            Map<Short, Integer> counts = counts(b);

            for (final short e : a) {
                if (take(counts, e)) {
                    intersection[ni++] = e;
                } else {
                    difference[nd++] = e;
                }
            }

            counts = counts(b);

            for (final short e : a) {
                if (!take(counts, e)) {
                    symmetricDifference[ns++] = e;
                }
            }

            for (final short e : b) {
                if (take(counts, e)) {
                    symmetricDifference[ns++] = e;
                }
            }

            return new short[][] { Arrays.copyOf(intersection, ni), Arrays.copyOf(difference, nd), Arrays.copyOf(symmetricDifference, ns) };
        }

        private void assertFreshGrowable(final ShortList result, final ShortList list, final ShortList other, final String msg) {
            assertNotSame(list, result, msg);
            assertNotSame(other, result, msg);

            final int size = result.size();
            result.add((short) 42);
            assertEquals(size + 1, result.size(), msg);
            assertEquals((short) 42, result.getLast(), msg);
        }

        private void assertMultisetOps(final short[] a, final short[] b, final String msg) {
            final short[][] expected = reference(a, b);
            final ShortList list = ShortList.of(a.clone());
            final ShortList other = ShortList.of(b.clone());

            final ShortList intersection = list.intersection(other);
            final ShortList difference = list.difference(other);
            final ShortList symmetricDifference = list.symmetricDifference(other);

            assertArrayEquals(expected[0], intersection.toArray(), msg);
            assertArrayEquals(expected[1], difference.toArray(), msg);
            assertArrayEquals(expected[2], symmetricDifference.toArray(), msg);
            assertFreshGrowable(intersection, list, other, msg);
            assertFreshGrowable(difference, list, other, msg);
            assertFreshGrowable(symmetricDifference, list, other, msg);

            assertArrayEquals(expected[0], list.intersection(b.clone()).toArray(), msg);
            assertArrayEquals(expected[1], list.difference(b.clone()).toArray(), msg);
            assertArrayEquals(expected[2], list.symmetricDifference(b.clone()).toArray(), msg);

            // neither operand is modified
            assertArrayEquals(a, list.toArray(), msg);
            assertArrayEquals(b, other.toArray(), msg);
        }

        // G067-01/G067-02: ShortList intersection/difference/symmetricDifference take the count-table path when this list has more
        // than 65,536 elements even if the argument is small (<= 1024); results must match a boxed counting reference on both sides of
        // both thresholds (receiver 65,536/65,537, argument 1,024/1,025), for both overloads and three value ranges.
        @Test
        public void testShortListMultisetOps_largeReceiverMatchesBoxedReference() {
            final Random random = new Random(20261008L);
            final int[] receiverLengths = { 200, 65535, 65536, 65537, 66000 };
            final int[] argumentLengths = { 1, 2, 16, 500, 1024, 1025 };

            for (final int lengthA : receiverLengths) {
                for (final int lengthB : argumentLengths) {
                    for (final String range : RANGES) {
                        final short[] a = randomShorts(random, lengthA, range);
                        final short[] b = randomShorts(random, lengthB, range);

                        assertMultisetOps(a, b, "a=" + lengthA + ", b=" + lengthB + ", range=" + range);
                    }
                }
            }
        }

        // G067-01: edge cases of the large-receiver count-table path - one repeated value, argument values absent from this list,
        // partially cancelled duplicates in the argument, extreme values, a self argument, and a receiver holding every short value.
        @Test
        public void testShortListMultisetOps_largeReceiverEdgeCases() {
            final short[] sameValue = new short[70000];
            Arrays.fill(sameValue, (short) 300);
            assertMultisetOps(sameValue, new short[] { 300, 300, 301 }, "same value");
            assertMultisetOps(sameValue, new short[] { 301, -300, Short.MAX_VALUE, Short.MIN_VALUE }, "argument absent");
            assertMultisetOps(sameValue, new short[] { 300 }, "single argument");

            final ShortList repeated = ShortList.of(sameValue.clone());
            assertEquals(2, repeated.intersection(ShortList.of((short) 300, (short) 300, (short) 301)).size());
            assertEquals(69998, repeated.difference(ShortList.of((short) 300, (short) 300, (short) 301)).size());
            final ShortList sym = repeated.symmetricDifference(ShortList.of((short) 300, (short) 300, (short) 301));
            assertEquals(69999, sym.size());
            assertEquals((short) 301, sym.getLast());

            // one -7 cancels; b's surviving occurrences are its earliest ones in index order: [.., -7, 5]
            final short[] oneValue = new short[70000];
            Arrays.fill(oneValue, (short) 9);
            oneValue[12345] = -7;
            final ShortList oneValueList = ShortList.of(oneValue.clone());
            final ShortList oneValueSym = oneValueList.symmetricDifference(ShortList.of((short) -7, (short) 5, (short) -7));
            assertEquals(70001, oneValueSym.size());
            assertEquals((short) -7, oneValueSym.get(69999));
            assertEquals((short) 5, oneValueSym.get(70000));
            assertMultisetOps(oneValue, new short[] { -7, 5, -7 }, "partial cancellation");

            final short[] extremes = new short[65537];
            for (int i = 0; i < extremes.length; i++) {
                extremes[i] = (i & 1) == 0 ? Short.MIN_VALUE : Short.MAX_VALUE;
            }
            assertMultisetOps(extremes, new short[] { Short.MAX_VALUE, Short.MIN_VALUE, Short.MIN_VALUE, 0, -1 }, "extremes");

            final short[] all = new short[65536 + 7];
            for (int i = 0; i < all.length; i++) {
                all[i] = (short) (i * 7919);
            }
            assertMultisetOps(all, new short[] { 0, 0, Short.MIN_VALUE, Short.MAX_VALUE, -1, -1, 127, 128, -128, -129 }, "every value");

            final Random random = new Random(7L);
            final short[] big = randomShorts(random, 70000, "audio");
            final ShortList self = ShortList.of(big.clone());
            assertArrayEquals(big, self.intersection(self).toArray());
            assertEquals(0, self.difference(self).size());
            assertEquals(0, self.symmetricDifference(self).size());
            assertArrayEquals(big, self.toArray());

            // empty / null arguments keep their dedicated branches
            assertEquals(0, self.intersection((ShortList) null).size());
            assertArrayEquals(big, self.difference(new short[0]).toArray());
            assertArrayEquals(big, self.symmetricDifference((ShortList) null).toArray());
            assertArrayEquals(new short[] { 1, 2 }, new ShortList().symmetricDifference(ShortList.of((short) 1, (short) 2)).toArray());
        }
    }
}
