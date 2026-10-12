package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Performance and memory regression contracts for IntList. */
@Tag("slow-test")
public class IntListPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G037 end ----
    // ---- perf review 2026-10-08 G038 begin ----
    @Nested
    class G038 {
        /**
         * Reference for {@code IntList.removeDuplicates()}: the first occurrence of each value in encounter order.
         */
        private int[] firstOccurrences(final int[] a, final int size) {
            final Set<Integer> kept = new LinkedHashSet<>();

            for (int i = 0; i < size; i++) {
                kept.add(a[i]);
            }

            final int[] result = new int[kept.size()];
            int k = 0;

            for (final Integer x : kept) {
                result[k++] = x;
            }

            return result;
        }

        @SuppressWarnings("deprecation")
        private void assertRemoveDuplicatesMatchesReference(final int[] a, final int size, final String label) {
            final int[] backing = a.clone();
            final int[] expected = firstOccurrences(backing, size);
            final int[] spare = Arrays.copyOfRange(backing, size, backing.length);
            final IntList list = IntList.of(backing, size);

            assertEquals(expected.length != size, list.removeDuplicates(), label);
            assertEquals(expected.length, list.size(), label);
            assertSame(backing, list.internalArray(), label);
            assertArrayEquals(expected, Arrays.copyOfRange(backing, 0, expected.length), label);

            for (int i = expected.length; i < size; i++) {
                assertEquals(0, backing[i], label + " cleared slot " + i);
            }

            assertArrayEquals(spare, Arrays.copyOfRange(backing, size, backing.length), label + " spare slots");

            // the list keeps its capacity and stays usable after the shrink
            list.add(Integer.MIN_VALUE);
            assertEquals(expected.length + 1, list.size(), label);
            assertEquals(Integer.MIN_VALUE, list.get(expected.length), label);
        }

        private int randomValue(final Random rnd, final int domain, final int size) {
            switch (domain) {
                case 0: { // extremes and small values around the Integer cache bounds
                    final int[] specials = { 0, -1, 1, Integer.MIN_VALUE, Integer.MAX_VALUE, 127, 128, -128, -129 };
                    return specials[rnd.nextInt(specials.length)];
                }
                case 1: // small (cached) values with duplicates: a narrow span
                    return rnd.nextInt(Math.max(1, size / 2));
                case 2: // large values with duplicates: a wide span
                    return (rnd.nextInt(Math.max(1, size / 2)) - size / 4) * 1_000_003;
                case 3: // dense values just below Integer.MAX_VALUE
                    return Integer.MAX_VALUE - rnd.nextInt(Math.max(1, size));
                case 4: // dense values just above Integer.MIN_VALUE
                    return Integer.MIN_VALUE + rnd.nextInt(Math.max(1, 2 * size));
                case 5: // dense negative and positive values around zero, mostly distinct
                    return rnd.nextInt(Math.max(1, 8 * size)) - 4 * size;
                default: // full range, mostly distinct
                    return rnd.nextInt();
            }
        }

        // G038-01: removeDuplicates' unsorted path (a bit set when the value range fits in 8 bits per element, else the unchanged hash
        // set) keeps the first occurrences in order and the backing array, zeroes the cleared slots and leaves spare capacity untouched
        @Test
        public void testRemoveDuplicates_matchesFirstOccurrenceReference() {
            final Random rnd = new Random(20261008L);
            final int[] sizes = { 2, 3, 4, 5, 8, 9, 15, 16, 17, 31, 32, 33, 63, 64, 65, 100, 127, 128, 129, 257, 1000, 3000 };

            for (final int size : sizes) {
                for (int domain = 0; domain < 7; domain++) {
                    for (int trial = 0; trial < 5; trial++) {
                        final int[] a = new int[size + rnd.nextInt(3)];

                        for (int i = 0; i < a.length; i++) {
                            a[i] = randomValue(rnd, domain, size);
                        }

                        assertRemoveDuplicatesMatchesReference(a, size, "size=" + size + " domain=" + domain + " trial=" + trial);
                    }
                }
            }
        }

        // G038-01: wide-range keys crafted to share one probe run of a multiplicative hash (the one of N's primitive key set) take the
        // hash-set path and give the same result
        @Test
        public void testRemoveDuplicates_collidingKeysMatchReference() {
            final int hashMultiplier = 0x9E3779B9; // multiplier of the hash used by N's primitive key set (N.distinct)
            int inverse = hashMultiplier;

            for (int i = 0; i < 5; i++) {
                inverse *= 2 - hashMultiplier * inverse;
            }

            final Random rnd = new Random(8L);

            for (final int distinct : new int[] { 70, 200, 5000 }) {
                final int[] a = new int[distinct * 2];

                for (int i = 0; i < a.length; i++) {
                    // the products with the multiplier are consecutive, so all keys start probing at the same slot
                    a[i] = (0x12340000 + rnd.nextInt(distinct)) * inverse;
                }

                assertRemoveDuplicatesMatchesReference(a, a.length, "colliding distinct=" + distinct);
            }
        }

        // G038-01: value spans just inside (max - min == 8 * size - 1, bit set) and just outside (max - min == 8 * size, hash set) of the
        // dense-span rule, at both ends of the int range, give the same result
        @Test
        public void testRemoveDuplicates_spanThresholdBoundary() {
            final Random rnd = new Random(38L);

            for (final int size : new int[] { 2, 3, 8, 63, 64, 65, 1000 }) {
                for (final long spanMinusOne : new long[] { 8L * size - 1, 8L * size }) {
                    for (final long min : new long[] { 0, -4L * size, Integer.MAX_VALUE - spanMinusOne, Integer.MIN_VALUE }) {
                        for (int trial = 0; trial < 3; trial++) {
                            final int[] a = new int[size];
                            a[0] = (int) min;
                            a[1] = (int) (min + spanMinusOne);

                            for (int i = 2; i < size; i++) {
                                // values inside [min, min + spanMinusOne], some repeated
                                a[i] = rnd.nextInt(3) == 0 ? a[rnd.nextInt(i)] : (int) (min + (long) (rnd.nextDouble() * (spanMinusOne + 1)));
                            }

                            for (int i = size - 1; i > 0; i--) {
                                final int j = rnd.nextInt(i + 1);
                                final int t = a[i];
                                a[i] = a[j];
                                a[j] = t;
                            }

                            assertRemoveDuplicatesMatchesReference(a, size, "size=" + size + " span-1=" + spanMinusOne + " min=" + min + " trial=" + trial);
                        }
                    }
                }
            }
        }

        // G038-01: the range check stops early: a range that only becomes too wide at the last element (or at the first ones) still gives
        // the same result, and so does one that stays narrow up to the last element
        @Test
        public void testRemoveDuplicates_rangeWidensLate() {
            for (final int size : new int[] { 3, 9, 64, 1000 }) {
                final long limit = 8L * size;

                for (final long lastOffset : new long[] { limit - 1, limit, limit + 1, -limit + 1, -limit, 1L << 31 }) {
                    final int[] a = new int[size];

                    for (int i = 0; i < size - 1; i++) {
                        a[i] = (i * 7) % Math.max(1, size / 2) + 1; // narrow, unsorted, with duplicates
                    }

                    final long last = Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, 1 + lastOffset));
                    a[size - 1] = (int) last;

                    assertRemoveDuplicatesMatchesReference(a, size, "size=" + size + " last=" + last);

                    final int[] wideFirst = a.clone();
                    wideFirst[0] = a[size - 1];
                    wideFirst[size - 1] = a[0];

                    assertRemoveDuplicatesMatchesReference(wideFirst, size, "size=" + size + " first=" + last);
                }
            }
        }

        // G038-01: in-place semantics: shared backing array, unchanged when all values are distinct, sorted path, capacity kept
        @Test
        @SuppressWarnings("deprecation")
        public void testRemoveDuplicates_sharedBackingArrayExamples() {
            final int[] shared = { 300, 100, 300, 200, 100, 900, 900 };
            final IntList list = IntList.of(shared, 5);
            final IntList other = IntList.of(shared, 5);

            assertTrue(list.removeDuplicates());
            assertSame(shared, list.internalArray());
            assertEquals(3, list.size());
            assertArrayEquals(new int[] { 300, 100, 200, 0, 0, 900, 900 }, shared);
            assertEquals(5, other.size());
            assertArrayEquals(new int[] { 300, 100, 200, 0, 0 }, other.toArray());

            final int[] unique = { 3, 1, 2, Integer.MIN_VALUE, Integer.MAX_VALUE, -1, 0 };
            final IntList uniqueList = IntList.of(unique);

            assertFalse(uniqueList.removeDuplicates());
            assertSame(unique, uniqueList.internalArray());
            assertEquals(7, uniqueList.size());
            assertArrayEquals(new int[] { 3, 1, 2, Integer.MIN_VALUE, Integer.MAX_VALUE, -1, 0 }, unique);

            final int[] sorted = { 1, 1, 2, 3, 3, 3, 7 };
            final IntList sortedList = IntList.of(sorted);

            assertTrue(sortedList.removeDuplicates());
            assertArrayEquals(new int[] { 1, 2, 3, 7, 0, 0, 0 }, sorted);

            final IntList grown = new IntList(64);

            for (int i = 0; i < 40; i++) {
                grown.add((i * 7) % 13);
            }

            final int[] before = grown.internalArray();

            assertTrue(grown.removeDuplicates());
            assertSame(before, grown.internalArray());
            assertEquals(64, grown.internalArray().length);
            assertArrayEquals(new int[] { 0, 7, 1, 8, 2, 9, 3, 10, 4, 11, 5, 12, 6 }, grown.toArray());

            final IntList single = IntList.of(5);
            assertFalse(single.removeDuplicates());
            assertFalse(new IntList().removeDuplicates());
        }
    }
}
