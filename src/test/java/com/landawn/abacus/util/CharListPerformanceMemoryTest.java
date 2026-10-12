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

/** Performance and memory regression contracts for CharList. */
@Tag("slow-test")
public class CharListPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G008 end ----
    // ---- perf review 2026-10-08 G009 begin ----
    @Nested
    class G009 {

        private static final String[] RANGES = { "ascii", "cjk", "full" };

        private char[] randomChars(final Random random, final int length, final String range) {
            final char[] a = new char[length];

            for (int i = 0; i < length; i++) {
                if ("ascii".equals(range)) {
                    a[i] = (char) (0x20 + random.nextInt(95));
                } else if ("cjk".equals(range)) {
                    a[i] = (char) (0x4E00 + random.nextInt(2000));
                } else {
                    a[i] = (char) random.nextInt(65536);
                }
            }

            if (length > 4 && "full".equals(range)) {
                a[0] = Character.MIN_VALUE;
                a[1] = Character.MAX_VALUE;
                a[2] = (char) 63;
                a[3] = (char) 64;
                a[4] = (char) 0xD800;
            }

            return a;
        }

        private Map<Character, Integer> counts(final char[] a) {
            final Map<Character, Integer> counts = new HashMap<>();

            for (final char e : a) {
                counts.merge(e, 1, Integer::sum);
            }

            return counts;
        }

        private boolean take(final Map<Character, Integer> counts, final char e) {
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
        private char[][] reference(final char[] a, final char[] b) {
            final char[] intersection = new char[a.length];
            final char[] difference = new char[a.length];
            final char[] symmetricDifference = new char[a.length + b.length];
            int ni = 0;
            int nd = 0;
            int ns = 0;

            Map<Character, Integer> counts = counts(b);

            for (final char e : a) {
                if (take(counts, e)) {
                    intersection[ni++] = e;
                } else {
                    difference[nd++] = e;
                }
            }

            counts = counts(b);

            for (final char e : a) {
                if (!take(counts, e)) {
                    symmetricDifference[ns++] = e;
                }
            }

            for (final char e : b) {
                if (take(counts, e)) {
                    symmetricDifference[ns++] = e;
                }
            }

            return new char[][] { Arrays.copyOf(intersection, ni), Arrays.copyOf(difference, nd), Arrays.copyOf(symmetricDifference, ns) };
        }

        private void assertMultisetOps(final char[] a, final char[] b, final String msg) {
            final char[][] expected = reference(a, b);
            final CharList list = CharList.of(a.clone());
            final CharList other = CharList.of(b.clone());

            final CharList intersection = list.intersection(other);
            final CharList difference = list.difference(other);
            final CharList symmetricDifference = list.symmetricDifference(other);

            assertArrayEquals(expected[0], intersection.toArray(), msg);
            assertArrayEquals(expected[1], difference.toArray(), msg);
            assertArrayEquals(expected[2], symmetricDifference.toArray(), msg);
            assertNotSame(list, intersection, msg);
            assertNotSame(list, difference, msg);
            assertNotSame(list, symmetricDifference, msg);
            assertNotSame(other, symmetricDifference, msg);

            assertArrayEquals(expected[0], list.intersection(b.clone()).toArray(), msg);
            assertArrayEquals(expected[1], list.difference(b.clone()).toArray(), msg);
            assertArrayEquals(expected[2], list.symmetricDifference(b.clone()).toArray(), msg);

            // neither operand is modified
            assertArrayEquals(a, list.toArray(), msg);
            assertArrayEquals(b, other.toArray(), msg);
        }

        // G009-01/G009-02: intersection/difference/symmetricDifference take the count-table path when this list has more than
        // 65,536 elements even if the argument is small (<= 1024); results must match a boxed counting reference on both sides
        // of both thresholds (receiver 65,536/65,537, argument 1,024/1,025), for both overloads and three value ranges.
        @Test
        public void testMultisetOps_largeReceiverMatchesBoxedReference() {
            final Random random = new Random(20261008L);
            final int[] receiverLengths = { 200, 65535, 65536, 65537, 66000 };
            final int[] argumentLengths = { 1, 2, 16, 500, 1024, 1025 };

            for (final int lengthA : receiverLengths) {
                for (final int lengthB : argumentLengths) {
                    for (final String range : RANGES) {
                        final char[] a = randomChars(random, lengthA, range);
                        final char[] b = randomChars(random, lengthB, range);

                        assertMultisetOps(a, b, "a=" + lengthA + ", b=" + lengthB + ", range=" + range);
                    }
                }
            }
        }

        // G009-01: edge cases of the large-receiver count-table path - one repeated value, argument values absent from this list,
        // partially cancelled duplicates in the argument, a self argument, and a receiver holding every char value.
        @Test
        public void testMultisetOps_largeReceiverEdgeCases() {
            final char[] sameValue = new char[70000];
            Arrays.fill(sameValue, 'x');
            assertMultisetOps(sameValue, new char[] { 'x', 'x', 'y' }, "same value");
            assertMultisetOps(sameValue, new char[] { 'y', 'z', (char) 0xFFFF, (char) 0 }, "argument absent");
            assertMultisetOps(sameValue, new char[] { 'x' }, "single argument");

            final CharList xs = CharList.of(sameValue.clone());
            assertEquals(2, xs.intersection(CharList.of('x', 'x', 'y')).size());
            assertEquals(69998, xs.difference(CharList.of('x', 'x', 'y')).size());
            final CharList sym = xs.symmetricDifference(CharList.of('x', 'x', 'y'));
            assertEquals(69999, sym.size());
            assertEquals('y', sym.getLast());

            // one 'b' cancels; b's surviving occurrences are its earliest ones in index order: [.., b, a]
            final char[] oneB = new char[70000];
            Arrays.fill(oneB, 'z');
            oneB[12345] = 'b';
            final CharList oneBList = CharList.of(oneB.clone());
            final CharList oneBSym = oneBList.symmetricDifference(CharList.of('b', 'a', 'b'));
            assertEquals(70001, oneBSym.size());
            assertEquals('b', oneBSym.get(69999));
            assertEquals('a', oneBSym.get(70000));
            assertMultisetOps(oneB, new char[] { 'b', 'a', 'b' }, "partial cancellation");

            final char[] all = new char[65536 + 7];
            for (int i = 0; i < all.length; i++) {
                all[i] = (char) (i * 7919);
            }
            assertMultisetOps(all, new char[] { (char) 0, (char) 0, (char) 0xFFFF, (char) 0xD800, 'a', 'a' }, "every value");

            final Random random = new Random(7L);
            final char[] big = randomChars(random, 70000, "ascii");
            final CharList self = CharList.of(big.clone());
            assertArrayEquals(big, self.intersection(self).toArray());
            assertEquals(0, self.difference(self).size());
            assertEquals(0, self.symmetricDifference(self).size());
            assertArrayEquals(big, self.toArray());
        }
    }
}
