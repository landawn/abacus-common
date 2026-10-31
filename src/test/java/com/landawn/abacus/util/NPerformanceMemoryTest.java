package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Performance and memory regression contracts for N. */
@Tag("slow-test")
public class NPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G045 end ----
    // ---- perf review 2026-10-08 G046 begin ----
    @Nested
    class G046 {

        private List<List<Object>> rowsOf(final Dataset ds) {
            final List<List<Object>> rows = new ArrayList<>();

            for (int rowIndex = 0; rowIndex < ds.size(); rowIndex++) {
                final List<Object> row = new ArrayList<>();

                for (int columnIndex = 0; columnIndex < ds.columnCount(); columnIndex++) {
                    final Object value = ds.get(rowIndex, columnIndex);
                    row.add(value);
                }

                rows.add(row);
            }

            return rows;
        }

        private List<String> commonColumnsInFirstOrder(final Dataset a, final Dataset b) {
            final Set<String> namesOfB = new HashSet<>(b.columnNames());
            final List<String> common = new ArrayList<>();

            for (final String name : a.columnNames()) {
                if (namesOfB.contains(name)) {
                    common.add(name);
                }
            }

            return common;
        }

        // G046-01: no common column -> same IllegalArgumentException and message from every key-less overload; null checks first
        @Test
        void testIntersection_datasetNoCommonColumns_messageAndNullChecksPinned() {
            final Dataset a = Dataset.rows(Arrays.asList("a1", "a2"), new Object[][] { { 1, 2 } });
            final Dataset b = Dataset.rows(Arrays.asList("b1", "b2", "b3"), new Object[][] { { 1, 2, 3 } });
            final String expected = "These two Datasets do not have any common column names: [a1, a2], [b1, b2, b3]";

            assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> N.intersection(a, b)).getMessage());
            assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> N.intersection(a, b, true)).getMessage());
            assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> N.difference(a, b)).getMessage());
            assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> N.difference(a, b, false)).getMessage());
            assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> N.symmetricDifference(a, b)).getMessage());
            assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> N.symmetricDifference(a, b, true)).getMessage());
            assertEquals("These two Datasets do not have any common column names: [b1, b2, b3], [a1, a2]",
                    assertThrows(IllegalArgumentException.class, () -> N.intersection(b, a)).getMessage());
            assertEquals("These two Datasets do not have any common column names: [], [b1, b2, b3]",
                    assertThrows(IllegalArgumentException.class, () -> N.difference(Dataset.empty(), b)).getMessage());
            assertEquals("These two Datasets do not have any common column names: [a1, a2], []",
                    assertThrows(IllegalArgumentException.class, () -> N.intersection(a, Dataset.empty())).getMessage());

            assertEquals("The first specified Dataset is null", assertThrows(IllegalArgumentException.class, () -> N.intersection(null, b)).getMessage());
            assertEquals("The second specified Dataset is null", assertThrows(IllegalArgumentException.class, () -> N.difference(a, null)).getMessage());
            assertEquals("The second specified Dataset is null",
                    assertThrows(IllegalArgumentException.class, () -> N.symmetricDifference(a, null)).getMessage());

            // nothing was changed on either side
            assertEquals(Arrays.asList("a1", "a2"), a.columnNames());
            assertEquals(Arrays.asList("b1", "b2", "b3"), b.columnNames());
        }

        // G046-01: key columns = a's columns that b also has (b reordered, extra columns on both sides), occurrence semantics kept
        @Test
        void testIntersection_datasetPartialOverlapReorderedColumns() {
            final Dataset a = Dataset.rows(Arrays.asList("id", "name", "deptA", "k"),
                    new Object[][] { { 1, "x", "d1", null }, { 2, "y", "d2", 5 }, { 1, "x", "d3", null }, { 3, "z", "d4", 7 }, { 2, "y", "d5", 5 } });
            final Dataset b = Dataset.rows(Arrays.asList("extraB", "k", "name", "id"),
                    new Object[][] { { "e1", null, "x", 1 }, { "e2", 5, "y", 2 }, { "e3", 9, "q", 4 } });
            final List<String> keys = Arrays.asList("id", "name", "k");

            final Dataset intersection = N.intersection(a, b);
            assertEquals(Arrays.asList("id", "name", "deptA", "k"), intersection.columnNames());
            assertEquals(Arrays.asList(Arrays.asList(1, "x", "d1", null), Arrays.asList(2, "y", "d2", 5)), rowsOf(intersection));
            assertEquals(rowsOf(N.intersection(a, b, keys)), rowsOf(intersection));
            assertEquals(rowsOf(intersection), rowsOf(N.intersection(a, b, false)));

            final Dataset difference = N.difference(a, b);
            assertEquals(Arrays.asList("id", "name", "deptA", "k"), difference.columnNames());
            assertEquals(Arrays.asList(Arrays.asList(1, "x", "d3", null), Arrays.asList(3, "z", "d4", 7), Arrays.asList(2, "y", "d5", 5)), rowsOf(difference));
            assertEquals(rowsOf(N.difference(a, b, keys)), rowsOf(difference));

            final Dataset reversedIntersection = N.intersection(b, a);
            assertEquals(Arrays.asList("extraB", "k", "name", "id"), reversedIntersection.columnNames());
            assertEquals(Arrays.asList(Arrays.asList("e1", null, "x", 1), Arrays.asList("e2", 5, "y", 2)), rowsOf(reversedIntersection));
            assertEquals(Arrays.asList(Arrays.asList("e3", 9, "q", 4)), rowsOf(N.difference(b, a)));

            final Dataset symmetric = N.symmetricDifference(a, b);
            final Dataset symmetricByKeys = N.symmetricDifference(a, b, keys);
            assertEquals(symmetricByKeys.columnNames(), symmetric.columnNames());
            assertEquals(rowsOf(symmetricByKeys), rowsOf(symmetric));
            assertEquals(4, symmetric.size());

            final Dataset noRows = Dataset.rows(Arrays.asList("deptA", "k", "id", "name"), new Object[0][]);
            assertEquals(0, N.intersection(noRows, b).size());
            assertEquals(Arrays.asList("deptA", "k", "id", "name"), N.difference(noRows, b).columnNames());
            assertEquals(0, N.intersection(a, noRows).size());
            assertEquals(rowsOf(a), rowsOf(N.difference(a, noRows)));
        }

        // G046-01: wide Datasets (hundreds of columns, b reversed with gaps and extras) match the explicit-key overloads
        @Test
        void testIntersection_datasetWideReorderedColumnsMatchExplicitKeys() {
            final int width = 300;
            final List<String> namesA = new ArrayList<>();

            for (int i = 0; i < width; i++) {
                namesA.add("c" + i);
            }

            final List<String> namesB = new ArrayList<>();

            for (int i = width - 1; i >= 0; i--) {
                if (i % 7 != 3) {
                    namesB.add("c" + i);
                }
            }

            namesB.add("onlyB1");
            namesB.add("onlyB2");

            final Object[][] rowsA = new Object[6][width];

            for (int r = 0; r < 6; r++) {
                for (int c = 0; c < width; c++) {
                    rowsA[r][c] = (r % 3) * 1000 + c; // rows r and r + 3 are equal
                }
            }

            final int[] patternsB = { 0, 0, 1, 9 };
            final Object[][] rowsB = new Object[patternsB.length][namesB.size()];

            for (int r = 0; r < patternsB.length; r++) {
                for (int j = 0; j < namesB.size(); j++) {
                    final String name = namesB.get(j);
                    rowsB[r][j] = name.startsWith("only") ? name + r : (Object) (patternsB[r] * 1000 + Integer.parseInt(name.substring(1)));
                }
            }

            final Dataset a = Dataset.rows(namesA, rowsA);
            final Dataset b = Dataset.rows(namesB, rowsB);
            final List<String> keys = commonColumnsInFirstOrder(a, b);
            assertEquals(width - 43, keys.size());

            final Dataset intersection = N.intersection(a, b);
            assertEquals(namesA, intersection.columnNames());
            assertEquals(Arrays.asList(rowsOf(a).get(0), rowsOf(a).get(1), rowsOf(a).get(3)), rowsOf(intersection));
            assertEquals(rowsOf(N.intersection(a, b, keys)), rowsOf(intersection));

            final Dataset difference = N.difference(a, b);
            assertEquals(Arrays.asList(rowsOf(a).get(2), rowsOf(a).get(4), rowsOf(a).get(5)), rowsOf(difference));
            assertEquals(rowsOf(N.difference(a, b, keys)), rowsOf(difference));

            final Dataset symmetric = N.symmetricDifference(a, b);
            assertEquals(4, symmetric.size());
            assertEquals(width + 2, symmetric.columnCount());
            assertEquals(rowsOf(N.symmetricDifference(a, b, keys)), rowsOf(symmetric));

            final Dataset reversed = N.intersection(b, a);
            assertEquals(namesB, reversed.columnNames());
            assertEquals(Arrays.asList(rowsOf(b).get(0), rowsOf(b).get(1), rowsOf(b).get(2)), rowsOf(reversed));
            assertEquals(rowsOf(N.intersection(b, a, commonColumnsInFirstOrder(b, a))), rowsOf(reversed));
        }

        // G046-01: requiresSameColumns accepts the same column set in another order and still rejects a different set
        @Test
        void testIntersection_datasetRequiresSameColumnsReordered() {
            final Dataset a = Dataset.rows(Arrays.asList("id", "name", "v"), new Object[][] { { 1, "x", "p" }, { 2, "y", "q" }, { 2, "y", "q" } });
            final Dataset b = Dataset.rows(Arrays.asList("v", "id", "name"), new Object[][] { { "p", 1, "x" }, { "q", 2, "y" } });

            assertEquals(Arrays.asList(Arrays.asList(1, "x", "p"), Arrays.asList(2, "y", "q")), rowsOf(N.intersection(a, b, true)));
            assertEquals(Arrays.asList(Arrays.asList(2, "y", "q")), rowsOf(N.difference(a, b, true)));
            assertEquals(Arrays.asList("id", "name", "v"), N.symmetricDifference(a, b, true).columnNames());
            assertEquals(Arrays.asList(Arrays.asList(2, "y", "q")), rowsOf(N.symmetricDifference(a, b, true)));

            final Dataset extra = Dataset.rows(Arrays.asList("v", "id", "name", "extra"), new Object[][] { { "p", 1, "x", "e" }, { "q", 2, "y", "f" } });

            assertEquals("These two Datasets do not have the same column names: [id, name, v], [v, id, name, extra]",
                    assertThrows(IllegalArgumentException.class, () -> N.intersection(a, extra, true)).getMessage());
            assertEquals("These two Datasets do not have the same column names: [id, name, v], [v, id, name, extra]",
                    assertThrows(IllegalArgumentException.class, () -> N.symmetricDifference(a, extra, true)).getMessage());
            assertEquals(Arrays.asList(Arrays.asList(1, "x", "p"), Arrays.asList(2, "y", "q")), rowsOf(N.intersection(a, extra, false)));
        }

        // G046-01: the column lookup follows b's current columns after rename/remove/add (index built before) and for slices
        @Test
        void testIntersection_datasetColumnIndexFollowsRenameRemoveAddAndSlice() {
            final Dataset a = Dataset.rows(Arrays.asList("id", "name", "v"), new Object[][] { { 1, "x", "a" }, { 1, "y", "b" }, { 2, "x", "c" } });
            final Dataset b = Dataset.rows(Arrays.asList("id", "tmp"), new Object[][] { { 1, "y" }, { 2, "z" } });

            assertEquals(1, b.getColumnIndex("tmp")); // the column index of b exists before the changes below
            assertEquals(Arrays.asList(Arrays.asList(1, "x", "a"), Arrays.asList(2, "x", "c")), rowsOf(N.intersection(a, b)));

            b.renameColumn("tmp", "name");
            assertEquals(Arrays.asList(Arrays.asList(1, "y", "b")), rowsOf(N.intersection(a, b)));
            assertEquals(Arrays.asList(Arrays.asList(1, "x", "a"), Arrays.asList(2, "x", "c")), rowsOf(N.difference(a, b)));

            b.removeColumn("name");
            assertEquals(Arrays.asList("id"), b.columnNames());
            assertEquals(Arrays.asList(Arrays.asList(1, "x", "a"), Arrays.asList(2, "x", "c")), rowsOf(N.intersection(a, b)));

            b.addColumn("name", Arrays.asList("x", "q"));
            assertEquals(Arrays.asList(Arrays.asList(1, "x", "a")), rowsOf(N.intersection(a, b)));

            final Dataset firstRow = b.slice(0, 1);
            assertEquals(Arrays.asList(Arrays.asList(1, "x", "a")), rowsOf(N.intersection(a, firstRow)));
            assertEquals(Arrays.asList(Arrays.asList(1, "y", "b"), Arrays.asList(2, "x", "c")), rowsOf(N.difference(a, firstRow)));

            final Dataset idOnly = b.slice(Arrays.asList("id"));
            assertEquals(Arrays.asList(Arrays.asList(1, "x", "a"), Arrays.asList(2, "x", "c")), rowsOf(N.intersection(a, idOnly)));
            assertEquals(Arrays.asList(Arrays.asList(1), Arrays.asList(2)), rowsOf(N.intersection(idOnly, a)));
        }
    }

    // ---- perf review 2026-10-08 G046 end ----
    // ---- perf review 2026-10-08 G047 begin ----
    @Nested
    class G047 {

        // G047-01: unsorted char ranges of <= 32 elements are de-duplicated by a scan instead of an 8 KB bitmap; pins the result on both sides of that limit
        @Test
        void testRemoveDuplicates_charSmallRangeScanMatchesFirstOccurrenceModel() {
            final Random rnd = new Random(20261008L);
            final char[] pool = { (char) 0, (char) 0xFFFF, 'a', 'b', 'A', (char) 0xD83D, (char) 0xDE80, (char) 0x4E00, (char) 0xE9, '0' };

            for (int trial = 0; trial < 6000; trial++) {
                final int len = rnd.nextInt(70);
                final char[] a = new char[len];
                final int mode = trial % 3;

                for (int i = 0; i < len; i++) {
                    a[i] = mode == 0 ? pool[rnd.nextInt(pool.length)] : mode == 1 ? (char) rnd.nextInt(65536) : (char) ('a' + rnd.nextInt(1 + trial % 40));
                }

                final char[] copy = a.clone();
                final int from = rnd.nextInt(len + 1);
                final int to = from + rnd.nextInt(len - from + 1);

                assertDistinct(charModel(a, from, to), N.removeDuplicates(a, from, to, false), a);
                assertDistinct(charModel(a, 0, len), N.removeDuplicates(a, 0, len, false), a);
                assertDistinct(charModel(a, 0, len), N.distinct(a), a);
                assertDistinct(charModel(a, from, to), N.distinct(a, from, to), a);
                assertDistinct(charModel(a, 0, len), N.removeDuplicates(a, false), a);
                assertDistinct(charModel(a, from, to), CharList.of(a.clone()).distinct(from, to).toArray(), a);
                assertArrayEquals(copy, a);
            }
        }

        // G047-01: the same for short (negative values, both extremes, -1 next to 0xFFFF-looking values)
        @Test
        void testRemoveDuplicates_shortSmallRangeScanMatchesFirstOccurrenceModel() {
            final Random rnd = new Random(8102026L);
            final short[] pool = { Short.MIN_VALUE, Short.MAX_VALUE, -1, 0, 1, -32767, 255, -256, 128, -129 };

            for (int trial = 0; trial < 6000; trial++) {
                final int len = rnd.nextInt(70);
                final short[] a = new short[len];
                final int mode = trial % 3;

                for (int i = 0; i < len; i++) {
                    a[i] = mode == 0 ? pool[rnd.nextInt(pool.length)] : mode == 1 ? (short) rnd.nextInt(65536) : (short) (rnd.nextInt(1 + trial % 40) - 20);
                }

                final short[] copy = a.clone();
                final int from = rnd.nextInt(len + 1);
                final int to = from + rnd.nextInt(len - from + 1);

                assertDistinct(shortModel(a, from, to), N.removeDuplicates(a, from, to, false), a);
                assertDistinct(shortModel(a, 0, len), N.removeDuplicates(a, 0, len, false), a);
                assertDistinct(shortModel(a, 0, len), N.distinct(a), a);
                assertDistinct(shortModel(a, from, to), N.distinct(a, from, to), a);
                assertDistinct(shortModel(a, 0, len), N.removeDuplicates(a, false), a);
                assertDistinct(shortModel(a, from, to), ShortList.of(a.clone()).distinct(from, to).toArray(), a);
                assertArrayEquals(copy, a);
            }
        }

        // G047-01: exact boundary of the scan limit (31/32/33 elements), all distinct and with a single repeat at either end
        @Test
        void testRemoveDuplicates_charShortScanLimitBoundary() {
            for (final int len : new int[] { 2, 3, 31, 32, 33, 34 }) {
                final char[] distinctChars = new char[len];
                final short[] distinctShorts = new short[len];

                for (int i = 0; i < len; i++) {
                    distinctChars[i] = (char) (0xFFFF - i * 997);
                    distinctShorts[i] = (short) (Short.MIN_VALUE + i * 2039);
                }

                assertDistinct(distinctChars.clone(), N.distinct(distinctChars), distinctChars);
                assertDistinct(distinctShorts.clone(), N.distinct(distinctShorts), distinctShorts);

                final char[] repeatLastChar = distinctChars.clone();
                repeatLastChar[len - 1] = repeatLastChar[0];
                assertDistinct(charModel(repeatLastChar, 0, len), N.distinct(repeatLastChar), repeatLastChar);
                assertEquals(len - 1, N.distinct(repeatLastChar).length);

                final short[] repeatFirstShort = distinctShorts.clone();
                repeatFirstShort[0] = repeatFirstShort[len - 1];
                assertDistinct(shortModel(repeatFirstShort, 0, len), N.distinct(repeatFirstShort), repeatFirstShort);
                assertEquals(len - 1, N.distinct(repeatFirstShort).length);

                final char[] allSameChar = new char[len];
                Arrays.fill(allSameChar, (char) 0xFFFF);
                assertArrayEquals(new char[] { (char) 0xFFFF }, N.distinct(allSameChar));

                final short[] allSameShort = new short[len];
                Arrays.fill(allSameShort, (short) -1);
                assertArrayEquals(new short[] { -1 }, N.distinct(allSameShort));
            }
        }

        // G047-02: an all-distinct unsorted range is returned as a fresh copy of the range (byte/char/short), also past the bitmap capacity
        @Test
        void testRemoveDuplicates_allDistinctRangeReturnsFreshCopyOfRange() {
            for (final int len : new int[] { 2, 32, 33, 200, 255, 256 }) {
                final byte[] bytes = new byte[len + 2];

                for (int i = 0; i < bytes.length; i++) {
                    bytes[i] = (byte) (i * 7 + 3);
                }

                if (len + 2 <= 256) {
                    assertDistinct(bytes.clone(), N.distinct(bytes), bytes);
                    assertDistinct(bytes.clone(), N.removeDuplicates(bytes, false), bytes);
                }

                assertDistinct(Arrays.copyOfRange(bytes, 1, len + 1), N.removeDuplicates(bytes, 1, len + 1, false), bytes);
            }

            final byte[] allBytes = new byte[300];
            for (int i = 0; i < allBytes.length; i++) {
                allBytes[i] = (byte) i;
            }
            assertDistinct(Arrays.copyOf(allBytes, 256), N.distinct(allBytes), allBytes);
            assertDistinct(Arrays.copyOfRange(allBytes, 44, 300), N.removeDuplicates(allBytes, 44, 300, false), allBytes);

            for (final int len : new int[] { 2, 32, 33, 1000, 65535, 65536 }) {
                final char[] chars = new char[len + 2];
                final short[] shorts = new short[len + 2];

                for (int i = 0; i < chars.length; i++) {
                    chars[i] = (char) (i * 3 + 1); // 3 is odd, so i * 3 + 1 is distinct for 65536 consecutive i
                    shorts[i] = (short) (i * 5 - 7);
                }

                if (len + 2 <= 65536) {
                    assertDistinct(chars.clone(), N.distinct(chars), chars);
                    assertDistinct(shorts.clone(), N.distinct(shorts), shorts);
                    assertDistinct(chars.clone(), CharList.of(chars.clone()).distinct(0, chars.length).toArray(), chars);
                }

                assertDistinct(Arrays.copyOfRange(chars, 1, len + 1), N.removeDuplicates(chars, 1, len + 1, false), chars);
                assertDistinct(Arrays.copyOfRange(shorts, 2, len + 2), N.removeDuplicates(shorts, 2, len + 2, false), shorts);
            }

            // the returned array is independent of the input
            final char[] src = { 'x', 'y', 'z' };
            final char[] res = N.distinct(src);
            res[0] = 'q';
            assertArrayEquals(new char[] { 'x', 'y', 'z' }, src);
            final byte[] srcBytes = { 1, 2, 3 };
            final byte[] resBytes = N.distinct(srcBytes);
            resBytes[0] = 9;
            assertArrayEquals(new byte[] { 1, 2, 3 }, srcBytes);
        }

        // G047-01: guard against a return of the per-call 8 KB bitmap for tiny ranges (base: ~8,300 bytes per call)
        @Test
        void testDistinct_charShortTinyRangeAllocatesNoBitmap() {
            final com.sun.management.ThreadMXBean threadMx = (com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean();
            final char[] chars = "abcabcdefghijklm".toCharArray();
            final short[] shorts = { 5, -5, 5, 7, 300, -300, 7, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
            long sink = 0;

            for (int i = 0; i < 2000; i++) {
                sink += N.distinct(chars).length + N.distinct(shorts).length;
            }

            final long before = threadMx.getCurrentThreadAllocatedBytes();

            for (int i = 0; i < 2000; i++) {
                sink += N.distinct(chars).length + N.distinct(shorts).length;
            }

            final long perCall = (threadMx.getCurrentThreadAllocatedBytes() - before) / 4000;
            assertEquals(4000L * (13 + 12), sink);
            assertTrue(perCall < 2048, "N.distinct(char[16]/short[16]) allocated " + perCall + " bytes per call");
        }

        private char[] charModel(final char[] a, final int from, final int to) {
            final Set<Character> set = new LinkedHashSet<>();

            for (int i = from; i < to; i++) {
                set.add(a[i]);
            }

            final char[] result = new char[set.size()];
            int k = 0;

            for (final Character c : set) {
                result[k++] = c;
            }

            return result;
        }

        private short[] shortModel(final short[] a, final int from, final int to) {
            final Set<Short> set = new LinkedHashSet<>();

            for (int i = from; i < to; i++) {
                set.add(a[i]);
            }

            final short[] result = new short[set.size()];
            int k = 0;

            for (final Short s : set) {
                result[k++] = s;
            }

            return result;
        }

        private void assertDistinct(final char[] expected, final char[] actual, final char[] input) {
            assertArrayEquals(expected, actual);
            assertNotSame(input, actual);
        }

        private void assertDistinct(final short[] expected, final short[] actual, final short[] input) {
            assertArrayEquals(expected, actual);
            assertNotSame(input, actual);
        }

        private void assertDistinct(final byte[] expected, final byte[] actual, final byte[] input) {
            assertArrayEquals(expected, actual);
            assertNotSame(input, actual);
        }
    }

    // ---- perf review 2026-10-08 G047 end ----
    // ---- perf review 2026-10-08 G048 begin ----
    @Nested
    class G048 {

        private boolean referenceContainsDuplicates(final char[] a, final int fromIndex, final int toIndex) {
            final Set<Character> seen = new HashSet<>();

            for (int i = fromIndex; i < toIndex; i++) {
                if (!seen.add(a[i])) {
                    return true;
                }
            }

            return false;
        }

        private boolean referenceContainsDuplicates(final short[] a, final int fromIndex, final int toIndex) {
            final Set<Short> seen = new HashSet<>();

            for (int i = fromIndex; i < toIndex; i++) {
                if (!seen.add(a[i])) {
                    return true;
                }
            }

            return false;
        }

        private char[] distinctChars(final int n) {
            final char[] a = new char[n];

            // odd multiplier: n distinct values for n <= 65536, wrapping through 0xFFFF and 0x0000
            for (int i = 0; i < n; i++) {
                a[i] = (char) (i * 2053 + 0xFFF0);
            }

            return a;
        }

        private short[] distinctShorts(final int n) {
            final short[] a = new short[n];

            // odd multiplier: n distinct values for n <= 65536, crossing -1/0 and Short.MAX_VALUE/Short.MIN_VALUE
            for (int i = 0; i < n; i++) {
                a[i] = (short) (i * 2053 + 0x7FF0);
            }

            return a;
        }

        // G048-01: unsorted char ranges of at most 32 elements are compared pairwise instead of through the 8 KB bitmap;
        // every single duplicate pair, ranges that include/exclude one of its ends, both sides of the threshold.
        @Test
        void testContainsDuplicates_charEveryDuplicatePairAroundPairwiseThreshold() {
            for (int n = 0; n <= 70; n++) {
                final char[] a = distinctChars(n);

                assertFalse(N.containsDuplicates(a, false), "n=" + n);
                assertFalse(N.containsDuplicates(a), "n=" + n);
                assertFalse(CharList.of(a.clone()).containsDuplicates(), "n=" + n);

                for (int i = 0; i < n; i++) {
                    for (int j = i + 1; j < n; j++) {
                        final char[] b = a.clone();
                        b[j] = b[i];
                        final String msg = "n=" + n + ", pair=(" + i + ", " + j + ")";

                        assertTrue(N.containsDuplicates(b, false), msg);
                        assertTrue(N.containsDuplicates(b, i, j + 1, false), msg);
                        assertFalse(N.containsDuplicates(b, i + 1, n, false), msg);
                        assertFalse(N.containsDuplicates(b, 0, j, false), msg);
                        assertTrue(CharList.of(b).containsDuplicates(), msg);
                    }
                }
            }
        }

        // G048-01: same for short ranges.
        @Test
        void testContainsDuplicates_shortEveryDuplicatePairAroundPairwiseThreshold() {
            for (int n = 0; n <= 70; n++) {
                final short[] a = distinctShorts(n);

                assertFalse(N.containsDuplicates(a, false), "n=" + n);
                assertFalse(N.containsDuplicates(a), "n=" + n);
                assertFalse(ShortList.of(a.clone()).containsDuplicates(), "n=" + n);

                for (int i = 0; i < n; i++) {
                    for (int j = i + 1; j < n; j++) {
                        final short[] b = a.clone();
                        b[j] = b[i];
                        final String msg = "n=" + n + ", pair=(" + i + ", " + j + ")";

                        assertTrue(N.containsDuplicates(b, false), msg);
                        assertTrue(N.containsDuplicates(b, i, j + 1, false), msg);
                        assertFalse(N.containsDuplicates(b, i + 1, n, false), msg);
                        assertFalse(N.containsDuplicates(b, 0, j, false), msg);
                        assertTrue(ShortList.of(b).containsDuplicates(), msg);
                    }
                }
            }
        }

        // G048-01: random ranges over small and large alphabets (dense duplicates, few duplicates, none) match a HashSet scan.
        @Test
        void testContainsDuplicates_charAndShortRandomRangesMatchReference() {
            final Random rnd = new Random(20261008L);
            final int[] alphabets = { 1, 2, 5, 26, 40, 100, 1000, 65536 };

            for (int round = 0; round < 20000; round++) {
                final int length = rnd.nextInt(80);
                final int alphabet = alphabets[rnd.nextInt(alphabets.length)];
                final int base = rnd.nextInt(65536);
                final char[] c = new char[length];
                final short[] s = new short[length];

                for (int i = 0; i < length; i++) {
                    final int v = base + rnd.nextInt(alphabet);
                    c[i] = (char) v;
                    s[i] = (short) v;
                }

                final int fromIndex = length == 0 ? 0 : rnd.nextInt(length + 1);
                final int toIndex = fromIndex + rnd.nextInt(length - fromIndex + 1);
                final String msg = "round=" + round + ", c=" + Arrays.toString(c) + ", range=[" + fromIndex + ", " + toIndex + ")";

                assertEquals(referenceContainsDuplicates(c, fromIndex, toIndex), N.containsDuplicates(c, fromIndex, toIndex, false), msg);
                assertEquals(referenceContainsDuplicates(s, fromIndex, toIndex), N.containsDuplicates(s, fromIndex, toIndex, false), msg);
                assertEquals(referenceContainsDuplicates(c, 0, length), N.containsDuplicates(c, false), msg);
                assertEquals(referenceContainsDuplicates(s, 0, length), N.containsDuplicates(s, false), msg);
            }
        }

        // G048-01: a list's spare capacity (zeros after size) is outside the scanned range, also on the pairwise path.
        @Test
        void testContainsDuplicates_charAndShortListSpareCapacityIsNotScanned() {
            for (int n = 1; n <= 40; n++) {
                final CharList chars = new CharList(80);
                final ShortList shorts = new ShortList(80);

                chars.add('\u0000');
                shorts.add((short) 0);

                for (int i = 1; i < n; i++) {
                    chars.add((char) ('a' + i));
                    shorts.add((short) (-i));
                }

                assertFalse(chars.containsDuplicates(), "n=" + n);
                assertFalse(shorts.containsDuplicates(), "n=" + n);

                chars.add('\u0000');
                shorts.add((short) 0);

                assertTrue(chars.containsDuplicates(), "n=" + n);
                assertTrue(shorts.containsDuplicates(), "n=" + n);
            }
        }

        // G048-01: isSorted == true keeps comparing adjacent elements only (a non-adjacent duplicate is missed), and the
        // equal-value identity of short keys: (short) 0xFFFF is -1.
        @Test
        void testContainsDuplicates_charAndShortSortedFlagAndShortIdentity() {
            for (int n = 4; n <= 40; n++) {
                final char[] c = distinctChars(n);
                final short[] s = distinctShorts(n);
                c[n - 1] = c[0];
                s[n - 1] = s[0];

                assertFalse(N.containsDuplicates(c, true), "n=" + n);
                assertFalse(N.containsDuplicates(s, true), "n=" + n);
                assertTrue(N.containsDuplicates(c, false), "n=" + n);
                assertTrue(N.containsDuplicates(s, false), "n=" + n);
            }

            assertTrue(N.containsDuplicates(new short[] { 7, (short) 0xFFFF, 3, -1 }, false));
            assertFalse(N.containsDuplicates(new short[] { 7, Short.MIN_VALUE, 3, Short.MAX_VALUE, -1, 0 }, false));
            assertTrue(N.containsDuplicates(new char[] { 'x', '\uFFFF', 'y', (char) -1 }, false));
            assertFalse(N.containsDuplicates(new char[] { 'x', '\uFFFF', 'y', '\u0000', '\uD83D', '\uDE80' }, false));
        }

        // G048-01: range validation is unchanged (before any scan) and a null array is length zero.
        @Test
        void testContainsDuplicates_charAndShortRangeValidation() {
            final char[] c = { 'a', 'b', 'a', 'c' };
            final short[] s = { 1, 2, 1, 3 };

            assertThrows(IndexOutOfBoundsException.class, () -> N.containsDuplicates(c, -1, 2, false));
            assertThrows(IndexOutOfBoundsException.class, () -> N.containsDuplicates(c, 3, 2, false));
            assertThrows(IndexOutOfBoundsException.class, () -> N.containsDuplicates(c, 0, 5, false));
            assertThrows(IndexOutOfBoundsException.class, () -> N.containsDuplicates(s, -1, 2, false));
            assertThrows(IndexOutOfBoundsException.class, () -> N.containsDuplicates(s, 3, 2, false));
            assertThrows(IndexOutOfBoundsException.class, () -> N.containsDuplicates(s, 0, 5, false));
            assertThrows(IndexOutOfBoundsException.class, () -> N.containsDuplicates((char[]) null, 0, 1, false));
            assertThrows(IndexOutOfBoundsException.class, () -> N.containsDuplicates((short[]) null, 0, 1, false));
            assertFalse(N.containsDuplicates((char[]) null, 0, 0, false));
            assertFalse(N.containsDuplicates((short[]) null, 0, 0, false));
            assertFalse(N.containsDuplicates((char[]) null, false));
            assertFalse(N.containsDuplicates((short[]) null, false));
            assertTrue(N.containsDuplicates(c, 0, 4, false));
            assertFalse(N.containsDuplicates(c, 1, 4, false));
            assertTrue(N.containsDuplicates(s, 0, 3, false));
            assertFalse(N.containsDuplicates(s, 1, 4, false));
        }
    }

    // ---- perf review 2026-10-08 G048 end ----
    // ---- perf review 2026-10-08 G050 begin ----
    @Nested
    class G050 {

        private Integer[] sample(final int size) {
            final Integer[] a = new Integer[size];

            for (int i = 0; i < size; i++) {
                a[i] = i % 7 == 3 ? null : Integer.valueOf(i % 5); // nulls and duplicates
            }

            return a;
        }

        private List<Integer> suffix(final Integer[] a, final int fromIndex) {
            final List<Integer> expected = new ArrayList<>();

            for (int i = fromIndex; i < a.length; i++) {
                expected.add(a[i]);
            }

            return expected;
        }

        private void assertFreshMutableArrayList(final List<Integer> result, final List<Integer> expected) {
            assertEquals(ArrayList.class, result.getClass());
            assertEquals(expected, result);

            result.add(-1);
            result.add(0, -2);
            assertEquals(expected.size() + 2, result.size());
            assertEquals(Integer.valueOf(-2), result.get(0));
            assertEquals(Integer.valueOf(-1), result.get(result.size() - 1));
        }

        // G050-01: dropWhile(T[]) allocates its result once the dropped prefix is known; contents, list type and predicate calls are unchanged.
        @Test
        public void testDropWhile_arrayKeepsSuffixForEveryPrefixLength() {
            for (final int size : new int[] { 1, 2, 3, 8, 9, 10, 11, 17, 1000 }) {
                final Integer[] a = sample(size);
                final Integer[] copy = a.clone();

                for (int dropCount = 0; dropCount <= size; dropCount++) {
                    final int limit = dropCount;
                    final List<Integer> calls = new ArrayList<>();
                    final AtomicInteger counter = new AtomicInteger();

                    final List<Integer> result = N.dropWhile(a, e -> {
                        calls.add(e);
                        return counter.getAndIncrement() < limit;
                    });

                    assertFreshMutableArrayList(result, suffix(a, dropCount));
                    assertEquals(Arrays.asList(a).subList(0, Math.min(dropCount + 1, size)), calls);
                    assertArrayEquals(copy, a);
                }
            }

            final Integer[] big = sample(100_000);

            for (final int dropCount : new int[] { 0, 1, 50_000, 99_999, 100_000 }) {
                final AtomicInteger counter = new AtomicInteger();
                final List<Integer> result = N.dropWhile(big, e -> counter.getAndIncrement() < dropCount);

                assertEquals(suffix(big, dropCount), result);
                assertEquals(Math.min(dropCount + 1, big.length), counter.get());
            }
        }

        // G050-01: skipUntil(T[]) allocates its result once the skipped prefix is known; contents, list type and predicate calls are unchanged.
        @Test
        public void testSkipUntil_arrayKeepsSuffixForEveryPrefixLength() {
            for (final int size : new int[] { 1, 2, 3, 8, 9, 10, 11, 17, 1000 }) {
                final Integer[] a = sample(size);
                final Integer[] copy = a.clone();

                for (int skipCount = 0; skipCount <= size; skipCount++) {
                    final int limit = skipCount;
                    final List<Integer> calls = new ArrayList<>();
                    final AtomicInteger counter = new AtomicInteger();

                    final List<Integer> result = N.skipUntil(a, e -> {
                        calls.add(e);
                        return counter.getAndIncrement() >= limit;
                    });

                    assertFreshMutableArrayList(result, suffix(a, skipCount));
                    assertEquals(Arrays.asList(a).subList(0, Math.min(skipCount + 1, size)), calls);
                    assertArrayEquals(copy, a);
                }
            }

            final Integer[] big = sample(100_000);

            for (final int skipCount : new int[] { 0, 1, 50_000, 99_999, 100_000 }) {
                final AtomicInteger counter = new AtomicInteger();
                final List<Integer> result = N.skipUntil(big, e -> counter.getAndIncrement() >= skipCount);

                assertEquals(suffix(big, skipCount), result);
                assertEquals(Math.min(skipCount + 1, big.length), counter.get());
            }
        }

        // G050-01: empty / null arrays: a fresh empty mutable ArrayList and no predicate call.
        @Test
        public void testDropWhileAndSkipUntil_emptyAndNullArrays() {
            final AtomicInteger calls = new AtomicInteger();

            for (final Integer[] a : new Integer[][] { null, {} }) {
                final List<Integer> dropped = N.dropWhile(a, e -> calls.incrementAndGet() > 0);
                final List<Integer> skipped = N.skipUntil(a, e -> calls.incrementAndGet() > 0);

                assertFreshMutableArrayList(dropped, Collections.emptyList());
                assertFreshMutableArrayList(skipped, Collections.emptyList());
                assertNotSame(dropped, N.dropWhile(a, e -> true));
            }

            assertEquals(0, calls.get());
            assertThrows(IllegalArgumentException.class, () -> N.dropWhile(new Integer[] { 1 }, null));
            assertThrows(IllegalArgumentException.class, () -> N.skipUntil(new Integer[] { 1 }, null));
        }

        // G050-01: a throwing predicate propagates its own exception after the same calls; nothing is returned.
        @Test
        public void testDropWhileAndSkipUntil_arrayThrowingPredicate() {
            final Integer[] a = sample(20);

            for (int failAt = 0; failAt < a.length; failAt++) {
                final int limit = failAt;
                final RuntimeException failure = new IllegalStateException("fail at " + failAt);
                final AtomicInteger dropCalls = new AtomicInteger();
                final AtomicInteger skipCalls = new AtomicInteger();

                assertSame(failure, assertThrows(IllegalStateException.class, () -> N.dropWhile(a, e -> {
                    if (dropCalls.getAndIncrement() == limit) {
                        throw failure;
                    }

                    return true;
                })));

                assertSame(failure, assertThrows(IllegalStateException.class, () -> N.skipUntil(a, e -> {
                    if (skipCalls.getAndIncrement() == limit) {
                        throw failure;
                    }

                    return false;
                })));

                assertEquals(failAt + 1, dropCalls.get());
                assertEquals(failAt + 1, skipCalls.get());
            }
        }

        // G050-01: the kept elements are read from the array after the predicate loop, so writes made by the predicate are seen.
        @Test
        public void testDropWhileAndSkipUntil_arrayPredicateWritingToTheArray() {
            final Integer[] a = { 1, 2, 3, 4, 5 };

            final List<Integer> dropped = N.dropWhile(a, e -> {
                a[4] = e * 100; // overwrites a kept element while dropping
                a[2] = e == 3 ? 30 : a[2]; // overwrites the first kept element while it is tested
                return e < 3;
            });

            assertEquals(Arrays.asList(30, 4, 300), dropped);

            final Integer[] b = { 1, 2, 3, 4, 5 };

            final List<Integer> skipped = N.skipUntil(b, e -> {
                b[3] = e * 10;
                b[2] = e == 3 ? -3 : b[2];
                return e >= 3;
            });

            assertEquals(Arrays.asList(-3, 30, 5), skipped);
        }
    }

    // ---- perf review 2026-10-08 G050 end ----
    // ---- perf review 2026-10-08 G051 begin ----
    @Nested
    class G051 {
        private String recordList(final int records) {
            final StringBuilder sb = new StringBuilder("<list>");

            for (int i = 0; i < records; i++) {
                sb.append("\n  <item id=\"")
                        .append(i)
                        .append("\" note=\"a&amp;b &lt;i&gt; &quot;q&quot;\"><name>n&amp;")
                        .append(i)
                        .append("</name><value>")
                        .append(i)
                        .append("</value></item>");
            }

            return sb.append("\n</list>").toString();
        }

        private String expectedRecordList(final int records, final String indentation) {
            final StringBuilder sb = new StringBuilder("<list>");

            for (int i = 0; i < records; i++) {
                sb.append('\n').append(indentation).append("<item id=\"").append(i).append("\" note=\"a&amp;b &lt;i&gt; &quot;q&quot;\">");
                sb.append('\n').append(indentation).append(indentation).append("<name>n&amp;").append(i).append("</name>");
                sb.append('\n').append(indentation).append(indentation).append("<value>").append(i).append("</value>");
                sb.append('\n').append(indentation).append("</item>");
            }

            return sb.append("\n</list>").toString();
        }

        // G051-01: wide element in the one-child-per-line layout (children are now walked by sibling links)
        @Test
        void testFormatXml_wideRecordList_layoutUnchanged() {
            for (final int records : new int[] { 0, 1, 2, 3, 2500 }) {
                final String xml = recordList(records);

                assertEquals(expectedRecordList(records, "    "), N.formatXml(xml), "records=" + records);
                assertEquals(expectedRecordList(records, "    "), N.formatXml(xml, (com.landawn.abacus.parser.XmlSerConfig) null), "records=" + records);
                assertEquals(expectedRecordList(records, "\t"), N.formatXml(xml, com.landawn.abacus.parser.XmlSerConfig.create().setIndentation("\t")),
                        "records=" + records);
            }
        }

        // G051-01: wide mixed-content and xml:space="preserve" elements are still written verbatim
        @Test
        void testFormatXml_wideVerbatimContent_unchanged() {
            final StringBuilder mixed = new StringBuilder();

            for (int i = 0; i < 2500; i++) {
                mixed.append("t&amp;").append(i).append("<b>x<i>y</i></b>");
            }

            assertEquals("<root>\n    <p>" + mixed + "</p>\n</root>", N.formatXml("<root><p>" + mixed + "</p></root>"));

            final StringBuilder preserved = new StringBuilder();

            for (int i = 0; i < 2500; i++) {
                preserved.append("\n  <k a=\"").append(i).append("\"><v>").append(i).append("</v><v/></k>");
            }

            assertEquals("<root>\n    <s xml:space=\"preserve\">" + preserved + "\n</s>\n</root>",
                    N.formatXml("<root><s xml:space=\"preserve\">" + preserved + "\n</s></root>"));
        }

        // G051-01: deep nesting, every level holding whitespace text plus one element child
        @Test
        void testFormatXml_deepChain_unchanged() {
            final int depth = 60;
            final StringBuilder xml = new StringBuilder();
            final StringBuilder expected = new StringBuilder();

            for (int i = 0; i < depth; i++) {
                xml.append("<e i=\"").append(i).append("\">\n ");

                if (i > 0) {
                    expected.append('\n').append("  ".repeat(i));
                }

                expected.append("<e i=\"").append(i).append("\">");
            }

            xml.append("<leaf>x</leaf><leaf/>");
            expected.append('\n').append("  ".repeat(depth)).append("<leaf>x</leaf>");
            expected.append('\n').append("  ".repeat(depth)).append("<leaf/>");

            for (int i = depth - 1; i >= 0; i--) {
                xml.append(" </e>");
                expected.append('\n').append("  ".repeat(i)).append("</e>");
            }

            assertEquals(expected.toString(), N.formatXml(xml.toString(), com.landawn.abacus.parser.XmlSerConfig.create().setIndentation("  ")));
        }

        // G051-01: every child kind, empty elements, attribute escapes and top-level misc nodes (output recorded on the pristine build)
        @Test
        void testFormatXml_childKinds_unchanged() {
            final String ideographicSpace = String.valueOf((char) 0x3000);
            final String kinds = "<!-- top --><root a=\"x&#9;y&#10;z&#13;\" b=\"&lt;&amp;&gt;&quot;'\">\n"
                    + "  <!-- c1 -->\n  <?pi data 1?>\n  <?nodata?>\n  <e/>\n  <w>  </w>\n  <c><![CDATA[<x>&]]></c>\n"
                    + "  <s xml:space=\"preserve\">\n    <k>1</k>\n  </s>\n  <n><m><o>1</o><o>2</o></m><m/></n>\n"
                    + "  <t>a&#13;b&gt;c&#12288;</t>\n  <mix>t<b>x</b>u</mix>\n</root><?tail end?>";

            assertEquals("<!-- top -->\n<root a=\"x&#9;y&#10;z&#13;\" b=\"&lt;&amp;&gt;&quot;'\">\n    <!-- c1 -->\n    <?pi data 1?>\n    <?nodata?>\n"
                    + "    <e/>\n    <w>  </w>\n    <c><![CDATA[<x>&]]></c>\n    <s xml:space=\"preserve\">\n    <k>1</k>\n  </s>\n    <n>\n        <m>\n"
                    + "            <o>1</o>\n            <o>2</o>\n        </m>\n        <m/>\n    </n>\n    <t>a&#13;b&gt;c" + ideographicSpace + "</t>\n"
                    + "    <mix>t<b>x</b>u</mix>\n</root>\n<?tail end?>", N.formatXml(kinds));

            assertEquals(
                    "<!-- top -->\n<root a=\"x&#9;y&#10;z&#13;\" b=\"&lt;&amp;&gt;&quot;'\">\n\t<!-- c1 -->\n\t<?pi data 1?>\n\t<?nodata?>\n\t<e/>\n"
                            + "\t<w>  </w>\n\t<c><![CDATA[<x>&]]></c>\n\t<s xml:space=\"preserve\">\n    <k>1</k>\n  </s>\n\t<n>\n\t\t<m>\n\t\t\t<o>1</o>\n"
                            + "\t\t\t<o>2</o>\n\t\t</m>\n\t\t<m/>\n\t</n>\n\t<t>a&#13;b&gt;c" + ideographicSpace
                            + "</t>\n\t<mix>t<b>x</b>u</mix>\n</root>\n<?tail end?>",
                    N.formatXml(kinds, com.landawn.abacus.parser.XmlSerConfig.create().setIndentation("\t")));
        }

        // G051-01: XML 1.1 declaration and character references in a structured document (output recorded on the pristine build)
        @Test
        void testFormatXml_xml11Structure_unchanged() {
            final String xml = "<?xml version=\"1.1\"?><r>\n<c k=\"&#1;\">&#133;x&#8232;</c>\n<c k=\"&#127;\"><d>&#2;</d><d/></c>\n</r>";

            assertEquals("<?xml version=\"1.1\"?>\n<r>\n    <c k=\"&#1;\">&#133;x&#8232;</c>\n    <c k=\"&#127;\">\n        <d>&#2;</d>\n        <d/>\n"
                    + "    </c>\n</r>", N.formatXml(xml));
        }
    }
}
