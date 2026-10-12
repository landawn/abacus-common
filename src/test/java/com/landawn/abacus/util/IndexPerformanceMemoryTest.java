package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Performance and memory regression contracts for Index. */
@Tag("slow-test")
public class IndexPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G035 end ----
    // ---- perf review 2026-10-08 G037 begin ----
    @Nested
    class G037 {
        private final Class<?>[] primitiveTypes = { boolean.class, char.class, byte.class, short.class, int.class, long.class, float.class, double.class };

        // Codes 0..7 select a distinct value per type; float/double include -0, NaN, a NaN with payload and infinities.
        private Object primitiveArray(final Class<?> type, final int... codes) {
            final float[] floats = { 0f, -0f, Float.NaN, Float.intBitsToFloat(0x7fa12345), Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 1f, -2f };
            final double[] doubles = { 0d, -0d, Double.NaN, Double.longBitsToDouble(0x7ff0123412341234L), Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                    1d, -2d };
            final Object result = java.lang.reflect.Array.newInstance(type, codes.length);

            for (int i = 0; i < codes.length; i++) {
                final int code = codes[i];

                if (type == boolean.class) {
                    java.lang.reflect.Array.setBoolean(result, i, (code & 1) != 0);
                } else if (type == char.class) {
                    java.lang.reflect.Array.setChar(result, i, (char) ('a' + code));
                } else if (type == byte.class) {
                    java.lang.reflect.Array.setByte(result, i, (byte) (code - 3));
                } else if (type == short.class) {
                    java.lang.reflect.Array.setShort(result, i, (short) (code * 1000));
                } else if (type == int.class) {
                    java.lang.reflect.Array.setInt(result, i, code * 100_000);
                } else if (type == long.class) {
                    java.lang.reflect.Array.setLong(result, i, code * 0x100000001L);
                } else if (type == float.class) {
                    java.lang.reflect.Array.setFloat(result, i, floats[code & 7]);
                } else {
                    java.lang.reflect.Array.setDouble(result, i, doubles[code & 7]);
                }
            }

            return result;
        }

        private int search(final Class<?> type, final Object source, final int bound, final Object pattern, final int offset, final int size,
                final boolean reverse) throws Exception {
            final Class<?> arrayType = java.lang.reflect.Array.newInstance(type, 0).getClass();
            final java.lang.reflect.Method method = Index.class.getMethod(reverse ? "lastOfSubArray" : "ofSubArray", arrayType, int.class, arrayType, int.class,
                    int.class);

            try {
                return ((u.OptionalInt) method.invoke(null, source, bound, pattern, offset, size)).orElse(-1);
            } catch (final java.lang.reflect.InvocationTargetException e) {
                throw e.getCause() instanceof Exception cause ? cause : e;
            }
        }

        // Plain candidate-by-candidate scan; boxed equals has the same canonical-NaN and signed-zero rules as N.equals.
        private int naive(final Object source, final int bound, final Object pattern, final int offset, final int size, final boolean reverse) {
            final int length = java.lang.reflect.Array.getLength(source);
            final int first = reverse ? Math.min(bound, length - size) : Math.max(bound, 0);

            for (int i = first; i >= 0 && i <= length - size; i += reverse ? -1 : 1) {
                boolean matches = true;

                for (int j = 0; j < size && matches; j++) {
                    matches = java.util.Objects.equals(java.lang.reflect.Array.get(source, i + j), java.lang.reflect.Array.get(pattern, offset + j));
                }

                if (matches) {
                    return i;
                }
            }

            return -1;
        }

        private void assertAllBounds(final Class<?> type, final int[] sourceCodes, final int[] patternCodes, final int offset, final int size)
                throws Exception {
            final Object source = primitiveArray(type, sourceCodes);
            final Object pattern = primitiveArray(type, patternCodes);

            for (int bound = -2; bound <= sourceCodes.length + 1; bound++) {
                for (final boolean reverse : new boolean[] { false, true }) {
                    if (reverse && bound < 0) {
                        assertEquals(-1, search(type, source, bound, pattern, offset, size, true));
                    } else {
                        assertEquals(naive(source, bound, pattern, offset, size, reverse), search(type, source, bound, pattern, offset, size, reverse),
                                () -> type + " " + Arrays.toString(sourceCodes) + " / " + Arrays.toString(patternCodes) + " [" + offset + ", +" + size
                                        + ") reverse=" + reverse);
                    }
                }
            }
        }

        // G037-01: the scan for the pattern's first element must decide every edge exactly like a candidate-by-candidate scan
        @Test
        void testOfSubArray_firstElementScanEdges() throws Exception {
            final int[][] sources = { { 1, 2, 3, 4, 5, 6 }, // first element absent
                    { 1, 2, 3, 4, 5, 0 }, // first element only at the last index, past the last possible start
                    { 1, 2, 3, 4, 0, 6 }, // first element at the last possible start, tail differs
                    { 1, 2, 3, 4, 0, 7 }, // match at the last possible start
                    { 0, 7, 0, 7, 0, 7 }, // matches at the first position and repeatedly
                    { 0, 0, 0, 0, 0, 7 }, // runs of the first element before the match
                    { 0, 0, 7, 0, 7, 7 }, { 7 }, {} };
            final int[][] patterns = { { 0, 7 }, { 0 }, { 7 }, { 3, 0, 7 }, { 0, 0, 7 }, { 5, 0, 7, 4 } };

            for (final Class<?> type : primitiveTypes) {
                for (final int[] sourceCodes : sources) {
                    for (final int[] patternCodes : patterns) {
                        for (int offset = 0; offset < patternCodes.length; offset++) {
                            for (int size = 1; offset + size <= patternCodes.length; size++) {
                                assertAllBounds(type, sourceCodes, patternCodes, offset, size);
                            }
                        }
                    }
                }
            }

            assertEquals(4, Index.ofSubArray(new int[] { 1, 2, 3, 4, 0, 7 }, new int[] { 0, 7 }).orElse(-1));
            assertEquals(-1, Index.ofSubArray(new long[] { 1, 2, 3, 4, 5, 0 }, new long[] { 0, 7 }).orElse(-1));
            assertEquals(2, Index.ofSubArray("xyabcab".toCharArray(), 1, "zab".toCharArray(), 1, 2).orElse(-1));
            assertEquals(5, Index.ofSubArray("xyabcab".toCharArray(), 3, "zab".toCharArray(), 1, 2).orElse(-1));
            assertEquals(-1, Index.ofSubArray("xyabcab".toCharArray(), 6, "zab".toCharArray(), 1, 2).orElse(-1));
        }

        // G037-01: same edges searching backwards
        @Test
        void testLastOfSubArray_firstElementScanEdges() throws Exception {
            for (final Class<?> type : primitiveTypes) {
                assertAllBounds(type, new int[] { 7, 0, 7, 0, 7, 0 }, new int[] { 0, 7 }, 0, 2);
                assertAllBounds(type, new int[] { 0, 1, 2, 3, 4, 5 }, new int[] { 0, 1 }, 0, 2);
                assertAllBounds(type, new int[] { 0, 1, 2, 3, 4, 5 }, new int[] { 9, 0, 2 }, 1, 2);
                assertAllBounds(type, new int[] { 6, 6, 6, 6, 6, 6 }, new int[] { 6, 6, 6 }, 0, 3);
                assertAllBounds(type, new int[] { 1, 2, 3 }, new int[] { 4 }, 0, 1);
            }

            assertEquals(5, Index.lastOfSubArray("xyabcab".toCharArray(), "ab".toCharArray()).orElse(-1));
            assertEquals(2, Index.lastOfSubArray("xyabcab".toCharArray(), 3, "zab".toCharArray(), 1, 2).orElse(-1));
            assertEquals(-1, Index.lastOfSubArray("xyabcab".toCharArray(), 1, "zab".toCharArray(), 1, 2).orElse(-1));
            assertEquals(0, Index.lastOfSubArray(new double[] { -0.0, 1, 0.0, 1 }, 1, new double[] { -0.0, 1 }, 0, 2).orElse(-1));
        }

        // G037-01: low-entropy data (the first element matches often), both sides of the 16-element fallback threshold
        @Test
        void testOfSubArray_lowEntropyAgreesWithNaiveScan() throws Exception {
            final Random random = new Random(20261008);

            for (final Class<?> type : primitiveTypes) {
                for (int trial = 0; trial < 120; trial++) {
                    final int alphabet = 1 + trial % 4;
                    final int[] sourceCodes = new int[random.nextInt(90)];
                    final int[] patternCodes = new int[1 + random.nextInt(trial % 3 == 0 ? 24 : 6)];

                    for (int i = 0; i < sourceCodes.length; i++) {
                        sourceCodes[i] = random.nextInt(alphabet);
                    }

                    for (int i = 0; i < patternCodes.length; i++) {
                        patternCodes[i] = random.nextInt(alphabet);
                    }

                    final int offset = random.nextInt(patternCodes.length);
                    final int size = 1 + random.nextInt(patternCodes.length - offset);

                    if (size <= sourceCodes.length && trial % 2 == 0) {
                        System.arraycopy(patternCodes, offset, sourceCodes, random.nextInt(sourceCodes.length - size + 1), size);
                    }

                    assertAllBounds(type, sourceCodes, patternCodes, offset, size);
                }
            }
        }

        // G037-01: the linear-work fallback is still reached when every candidate passes the first-element check
        @Test
        void testLastOfSubArray_repeatedPrefixFallbackAfterFirstElementHits() throws Exception {
            for (final Class<?> type : primitiveTypes) {
                final int[] sourceCodes = new int[3000];
                final int[] patternCodes = new int[260];
                patternCodes[0] = 7; // outside the slice [1, 258)
                patternCodes[257] = 1;
                sourceCodes[1200] = 1;
                sourceCodes[2900] = 1;
                final Object source = primitiveArray(type, sourceCodes);
                final Object pattern = primitiveArray(type, patternCodes);

                for (final int bound : new int[] { -1, 0, 900, 944, 945, 946, 2643, 2644, 2645, 3000, Integer.MAX_VALUE, Integer.MIN_VALUE }) {
                    for (final boolean reverse : new boolean[] { false, true }) {
                        final int expected = reverse && bound < 0 ? -1 : naive(source, bound, pattern, 1, 257, reverse);
                        assertEquals(expected, search(type, source, bound, pattern, 1, 257, reverse), type + " bound=" + bound + " reverse=" + reverse);
                    }
                }
            }

            final int[] zeros = new int[5000];
            zeros[4000] = 1;
            final int[] run = new int[100];
            run[99] = 1;
            assertEquals(3901, Index.ofSubArray(zeros, run).orElse(-1));
            assertEquals(3901, Index.lastOfSubArray(zeros, run).orElse(-1));
            assertEquals(-1, Index.ofSubArray(zeros, 3902, run).orElse(-1));
            assertEquals(-1, Index.lastOfSubArray(zeros, 3900, run).orElse(-1));
        }

        // G037-01: NaN (any payload), signed zeros and infinities as the pattern's first element
        @Test
        void testOfSubArray_floatingFirstElementKeepsCompareSemantics() throws Exception {
            for (final Class<?> type : new Class<?>[] { float.class, double.class }) {
                assertAllBounds(type, new int[] { 0, 1, 2, 3, 2, 6, 3, 6, 4, 5, 1, 6 }, new int[] { 2, 6 }, 0, 2);
                assertAllBounds(type, new int[] { 0, 1, 2, 3, 2, 6, 3, 6, 4, 5, 1, 6 }, new int[] { 1, 6 }, 0, 2);
                assertAllBounds(type, new int[] { 0, 1, 2, 3, 2, 6, 3, 6, 4, 5, 1, 6 }, new int[] { 0, 1 }, 0, 2);
                assertAllBounds(type, new int[] { 0, 1, 2, 3, 2, 6, 3, 6, 4, 5, 1, 6 }, new int[] { 4, 5 }, 0, 2);
                assertAllBounds(type, new int[] { 3, 3, 3, 3, 3, 3 }, new int[] { 2, 2, 3 }, 0, 3);
            }

            assertEquals(3,
                    Index.ofSubArray(new float[] { 0f, 1f, Float.NaN, Float.intBitsToFloat(0x7fa12345), 2f }, new float[] { Float.NaN, 2f }).orElse(-1));
            assertEquals(2, Index.ofSubArray(new double[] { 0.0, 1, -0.0, 1 }, new double[] { -0.0, 1 }).orElse(-1));
            assertEquals(-1, Index.ofSubArray(new double[] { 0.0, 1, 0.0, 1 }, new double[] { -0.0, 1 }).orElse(-1));
        }

        // The candidate-by-candidate scan of the Object[] overloads: every element comparison in its documented order.
        private int candidateScan(final Object[] source, final int bound, final Object[] pattern, final int offset, final int size, final boolean reverse) {
            final int len = source.length;
            final int end = offset + size;

            if (reverse) {
                if (bound < 0 || len < size) {
                    return -1;
                }

                for (int i = Math.min(bound, len - size); i >= 0; i--) {
                    for (int k = i, j = offset; j < end; k++, j++) {
                        if (!CommonUtil.equals(source[k], pattern[j])) {
                            break;
                        } else if (j == end - 1) {
                            return i;
                        }
                    }
                }
            } else {
                if (bound >= len || len - Math.max(bound, 0) < size) {
                    return -1;
                }

                for (int i = Math.max(bound, 0); i <= len - size; i++) {
                    for (int k = i, j = offset; j < end; k++, j++) {
                        if (!CommonUtil.equals(source[k], pattern[j])) {
                            break;
                        } else if (j == end - 1) {
                            return i;
                        }
                    }
                }
            }

            return -1;
        }

        private void assertSameEqualsCalls(final boolean reverse) {
            final List<String> calls = new ArrayList<>();

            final class Element {
                final String name;

                Element(final String name) {
                    this.name = name;
                }

                @Override
                public boolean equals(final Object obj) {
                    calls.add(name + "=" + (obj instanceof Element e ? e.name : String.valueOf(obj)));
                    return obj instanceof Element e && name.charAt(0) == e.name.charAt(0);
                }

                @Override
                public int hashCode() {
                    return name.charAt(0);
                }
            }

            final Random random = new Random(reverse ? 37 : 73);

            for (int trial = 0; trial < 300; trial++) {
                final Object[] source = new Object[random.nextInt(40)];
                final Object[] pattern = new Object[1 + random.nextInt(trial % 3 == 0 ? 20 : 5)];
                final int alphabet = 1 + trial % 4;

                for (int i = 0; i < source.length; i++) {
                    source[i] = random.nextInt(9) == 0 ? null : new Element((char) ('a' + random.nextInt(alphabet)) + "s" + i);
                }

                for (int i = 0; i < pattern.length; i++) {
                    pattern[i] = random.nextInt(9) == 0 ? null : new Element((char) ('a' + random.nextInt(alphabet)) + "p" + i);
                }

                final int offset = random.nextInt(pattern.length);
                final int size = 1 + random.nextInt(pattern.length - offset);

                if (size <= source.length && trial % 2 == 0) {
                    System.arraycopy(pattern, offset, source, random.nextInt(source.length - size + 1), size);
                }

                for (int bound = -2; bound <= source.length + 1; bound++) {
                    calls.clear();
                    final int expected = candidateScan(source, bound, pattern, offset, size, reverse);
                    final List<String> expectedCalls = new ArrayList<>(calls);
                    calls.clear();
                    final int actual = (reverse ? Index.lastOfSubArray(source, bound, pattern, offset, size)
                            : Index.ofSubArray(source, bound, pattern, offset, size)).orElse(-1);
                    final int b = bound;
                    final int t = trial;
                    assertEquals(expected, actual, () -> "trial=" + t + " bound=" + b);
                    assertEquals(expectedCalls, calls, () -> "trial=" + t + " bound=" + b);
                }
            }
        }

        // G037-02: the Object[] search makes the same equals calls (receiver, argument, order) as a candidate-by-candidate scan
        @Test
        void testOfSubArray_objectEqualsCallSequenceUnchanged() {
            assertSameEqualsCalls(false);
        }

        // G037-02: same for the backward search
        @Test
        void testLastOfSubArray_objectEqualsCallSequenceUnchanged() {
            assertSameEqualsCalls(true);
        }
    }
}
