package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collector;

import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.stream.Collectors;
import com.landawn.abacus.util.stream.Stream;

/**
 * Pins the results of the code paths changed by the 2026-09-27 external performance review fixes (all behaviour-preserving):
 * {@code N.moveRange} buffers the shorter block, {@code minAll}/{@code maxAll} drop a list grown by earlier ties,
 * the default-list {@code mapAndFilter} family caps its initial capacity, {@code Strings.swapCase} maps ASCII directly on its
 * Unicode path, {@code Dates} caches the formatters that re-read a custom zoned pattern, and {@code RowDataset.pivot} fills
 * the Sheet directly, and N's scheduled executor drops a cancelled task from its queue at once. The flatMap,
 * {@code Maps.getValuesIfPresent} and {@code Beans.setPropValue(Object, Method, Object)} tests pin their current results.
 */
public class ExternalPerfReview20260927Test extends TestBase {

    // ---- N.moveRange ----

    private static int[] moveRangeReference(final int[] a, final int fromIndex, final int toIndex, final int newPositionAfterMove) {
        final List<Integer> list = new ArrayList<>();

        for (final int e : a) {
            list.add(e);
        }

        final List<Integer> range = new ArrayList<>(list.subList(fromIndex, toIndex));
        list.subList(fromIndex, toIndex).clear();
        list.addAll(newPositionAfterMove, range);

        return list.stream().mapToInt(Integer::intValue).toArray();
    }

    @Test
    public void moveRangeMatchesReferenceForEveryShape() {
        for (int len = 0; len <= 12; len++) {
            for (int from = 0; from <= len; from++) {
                for (int to = from; to <= len; to++) {
                    for (int newPos = 0; newPos <= len - (to - from); newPos++) {
                        final int[] src = new int[len];

                        for (int i = 0; i < len; i++) {
                            src[i] = i + 1;
                        }

                        final int[] expected = moveRangeReference(src, from, to, newPos);
                        final String msg = len + ":" + from + ".." + to + "->" + newPos;

                        final int[] ints = src.clone();
                        N.moveRange(ints, from, to, newPos);
                        assertArrayEquals(expected, ints, msg);

                        final long[] longs = new long[len];
                        final double[] doubles = new double[len];
                        final float[] floats = new float[len];
                        final short[] shorts = new short[len];
                        final byte[] bytes = new byte[len];
                        final char[] chars = new char[len];
                        final boolean[] booleans = new boolean[len];
                        final Integer[] objects = new Integer[len];

                        for (int i = 0; i < len; i++) {
                            longs[i] = src[i];
                            doubles[i] = src[i];
                            floats[i] = src[i];
                            shorts[i] = (short) src[i];
                            bytes[i] = (byte) src[i];
                            chars[i] = (char) ('a' + src[i]);
                            booleans[i] = src[i] % 3 == 0;
                            objects[i] = src[i] % 4 == 0 ? null : src[i];
                        }

                        N.moveRange(longs, from, to, newPos);
                        N.moveRange(doubles, from, to, newPos);
                        N.moveRange(floats, from, to, newPos);
                        N.moveRange(shorts, from, to, newPos);
                        N.moveRange(bytes, from, to, newPos);
                        N.moveRange(chars, from, to, newPos);
                        N.moveRange(booleans, from, to, newPos);
                        N.moveRange(objects, from, to, newPos);

                        for (int i = 0; i < len; i++) {
                            assertEquals(expected[i], longs[i], msg);
                            assertEquals(expected[i], doubles[i], msg);
                            assertEquals(expected[i], floats[i], msg);
                            assertEquals((short) expected[i], shorts[i], msg);
                            assertEquals((byte) expected[i], bytes[i], msg);
                            assertEquals((char) ('a' + expected[i]), chars[i], msg);
                            assertEquals(expected[i] % 3 == 0, booleans[i], msg);
                            assertEquals(expected[i] % 4 == 0 ? null : expected[i], objects[i], msg);
                        }
                    }
                }
            }
        }
    }

    @Test
    public void moveRangeLargeAndInvalid() {
        final Random rnd = new Random(20260927);

        for (int k = 0; k < 200; k++) {
            final int len = rnd.nextInt(3000);
            final int from = rnd.nextInt(len + 1);
            final int to = from + rnd.nextInt(len - from + 1);
            final int newPos = rnd.nextInt(len - (to - from) + 1);
            final int[] src = rnd.ints(len).toArray();
            final int[] a = src.clone();
            N.moveRange(a, from, to, newPos);
            assertArrayEquals(moveRangeReference(src, from, to, newPos), a);
        }

        final String[] strings = { "a", "b", "c", "d", "e" };
        N.moveRange(strings, 0, 4, 1);
        assertArrayEquals(new String[] { "e", "a", "b", "c", "d" }, strings);
        N.moveRange(strings, 1, 5, 0);
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" }, strings);

        assertThrows(IndexOutOfBoundsException.class, () -> N.moveRange(new int[5], 0, 5, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> N.moveRange(new int[5], 3, 2, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.moveRange(new String[5], 1, 3, 4));
        N.moveRange((int[]) null, 0, 0, 0);
    }

    // ---- minAll / maxAll ----

    @Test
    public void minAllMaxAllAfterLongTieRuns() {
        final Integer[] minInput = new Integer[1000];
        Arrays.fill(minInput, 5);
        minInput[500] = 1;
        minInput[900] = 1;
        final Integer[] maxInput = new Integer[1000];
        Arrays.fill(maxInput, 5);
        maxInput[500] = 9;
        maxInput[900] = 9;
        final Comparator<Integer> cmp = Comparator.naturalOrder();

        final List<List<Integer>> minResults = List.of(N.minAll(minInput, cmp), N.minAll(Arrays.asList(minInput).iterator(), cmp),
                Stream.of(minInput).minAll(cmp), Stream.of(Arrays.asList(minInput).iterator()).minAll(cmp), Stream.of(minInput).collect(Collectors.minAll(cmp)),
                Stream.of(minInput).parallel().minAll(cmp));
        final List<List<Integer>> maxResults = List.of(N.maxAll(maxInput, cmp), N.maxAll(Arrays.asList(maxInput).iterator(), cmp),
                Stream.of(maxInput).maxAll(cmp), Stream.of(Arrays.asList(maxInput).iterator()).maxAll(cmp), Stream.of(maxInput).collect(Collectors.maxAll(cmp)),
                Stream.of(maxInput).parallel().maxAll(cmp));

        for (final List<Integer> result : minResults) {
            assertEquals(List.of(1, 1), result);
            result.add(0); // still a mutable list
            assertEquals(3, result.size());
        }

        for (final List<Integer> result : maxResults) {
            assertEquals(List.of(9, 9), result);
            result.add(0);
            assertEquals(3, result.size());
        }

        // ties after the new extreme keep accumulating, in encounter order
        final String[] words = new String[40];
        Arrays.fill(words, "bb");
        words[20] = "c";
        words[30] = "c";
        words[39] = "c";
        final Comparator<String> byLength = Comparator.comparingInt(String::length);
        assertEquals(List.of("c", "c", "c"), N.minAll(words, byLength));
        assertEquals(List.of("c", "c", "c"), Stream.of(words).minAll(byLength));

        final Collector<String, ?, List<String>> atMost2 = Collectors.maxAll(Comparator.<String> naturalOrder(), 2);
        final String[] letters = new String[50];
        Arrays.fill(letters, "a");
        letters[45] = "z";
        letters[47] = "z";
        letters[49] = "z";
        assertEquals(List.of("z", "z"), Stream.of(letters).collect(atMost2));
    }

    // ---- default-list mapAndFilter family ----

    @Test
    public void mapAndFilterFamilyDefaultLists() {
        final List<Integer> input = new ArrayList<>();

        for (int i = 0; i < 1000; i++) {
            input.add(i);
        }

        final List<Integer> sparse = N.mapAndFilter(input, i -> i * 2, v -> v == 20 || v == 1990);
        assertEquals(List.of(20, 1990), sparse);
        sparse.add(1);
        assertEquals(3, sparse.size());
        assertEquals(1000, N.mapAndFilter(input, i -> i, v -> true).size());
        assertEquals(List.of("7"), N.filterAndMap(input, i -> i == 7, String::valueOf));
        assertEquals(List.of(3, 3), N.flatMapAndFilter(input, i -> List.of(i, i), v -> v == 3));
        assertEquals(List.of(5, 6), N.filterAndFlatMap(input, i -> i == 5, i -> List.of(i, i + 1)));
        assertEquals(List.of(), N.mapAndFilter(input, i -> i, v -> false));
        assertEquals(List.of(), N.mapAndFilter(List.<Integer> of(), i -> i, v -> true));

        final Iterable<Integer> iterableOnly = input::iterator;
        assertEquals(List.of(999), N.mapAndFilter(iterableOnly, i -> i, v -> v == 999));
        assertEquals(input.size(), N.filterAndFlatMap(iterableOnly, i -> true, List::of).size());

        // a caller's supplier still receives the same capacity hint (half the input size)
        final List<Integer> hints = new ArrayList<>();
        N.mapAndFilter(input, i -> i, v -> false, n -> {
            hints.add(n);
            return new ArrayList<>(n);
        });
        assertEquals(List.of(500), hints);
    }

    @Test
    public void flatMapDefaultListsSparseAndDense() {
        final Integer[] array = new Integer[1000];
        final List<Integer> list = new ArrayList<>();

        for (int i = 0; i < array.length; i++) {
            array[i] = i;
            list.add(i);
        }

        final Iterable<Integer> iterableOnly = list::iterator;
        final java.util.function.Function<Integer, List<Integer>> sparse = i -> i % 400 == 7 ? List.of(i) : List.of();
        final java.util.function.Function<Integer, List<Integer>> dense = i -> List.of(i, -i);

        final List<List<Integer>> sparseResults = List.of(N.flatMap(array, 0, array.length, sparse), N.flatMap(list, 0, list.size(), sparse),
                N.flatMap(list, sparse), N.flatMap(iterableOnly, sparse), N.flatMap(array, i -> List.of(i), sparse),
                N.flatMap(list, i -> List.of(i), sparse));

        for (final List<Integer> result : sparseResults) {
            assertEquals(List.of(7, 407, 807), result);
            result.add(1); // still a mutable ArrayList
            assertEquals(4, result.size());
        }

        final List<Integer> expectedDense = new ArrayList<>();

        for (int i = 0; i < 1000; i++) {
            expectedDense.add(i);
            expectedDense.add(-i);
        }

        assertEquals(expectedDense, N.flatMap(array, 0, array.length, dense));
        assertEquals(expectedDense, N.flatMap(list, dense));
        assertEquals(expectedDense, N.flatMap(array, i -> List.of(i), dense));
        assertEquals(expectedDense.subList(20, 40), N.flatMap(list, 10, 20, dense));
        assertEquals(List.of(), N.flatMap(new Integer[0], sparse, sparse));
        assertEquals(List.of(), N.flatMap((List<Integer>) null, sparse));
        assertEquals(List.of(), N.flatMap(array, 5, 5, sparse));
    }

    @Test
    public void getValuesIfPresentSparseDuplicatesAndDense() {
        final Map<String, Integer> map = new HashMap<>();

        for (int i = 0; i < 100; i++) {
            map.put("k" + i, i);
        }

        map.put("nullValue", null);

        final List<String> mostlyMissing = new ArrayList<>();

        for (int i = 0; i < 1000; i++) {
            mostlyMissing.add("missing" + i);
        }

        mostlyMissing.add(500, "k5");
        mostlyMissing.add("nullValue");
        final List<Integer> sparse = Maps.getValuesIfPresent(map, mostlyMissing);
        assertEquals(List.of(5), sparse);
        sparse.add(6);
        assertEquals(List.of(5, 6), sparse);

        // more hits than the map has entries: duplicate keys
        final List<String> duplicates = new ArrayList<>();

        for (int i = 0; i < 300; i++) {
            duplicates.add("k" + (i % 3));
        }

        final List<Integer> repeated = Maps.getValuesIfPresent(Map.of("k0", 0, "k1", 1, "k2", 2), duplicates);
        assertEquals(300, repeated.size());
        assertEquals(List.of(0, 1, 2, 0), repeated.subList(0, 4));

        final List<String> all = new ArrayList<>(map.keySet());
        all.remove("nullValue");
        assertEquals(100, Maps.getValuesIfPresent(map, all).size());
        assertEquals(List.of(), Maps.getValuesIfPresent(map, List.of("x", "y")));
        assertEquals(List.of(), Maps.getValuesIfPresent(null, List.of("k1")));
        assertEquals(List.of(), Maps.getValuesIfPresent(map, null));
    }

    // ---- Beans.setPropValue(Object, Method, Object) ----

    public static class SetterTarget {
        private int count;
        private double ratio;
        private Long id;
        private String label;
        private List<String> tags;

        public void setCount(final int count) {
            this.count = count;
        }

        public void setRatio(final double ratio) {
            if (ratio < 0) {
                throw new IllegalStateException("negative ratio");
            }

            this.ratio = ratio;
        }

        public void setId(final Long id) {
            this.id = id;
        }

        public void setLabel(final String label) {
            this.label = label;
        }

        public void setTags(final List<String> tags) {
            this.tags = tags;
        }

        public void setPair(final int a, final int b) {
            this.count = a + b;
        }

        @SuppressWarnings("unused")
        private void setHidden(final int count) {
            this.count = count;
        }
    }

    @Test
    public void beansSetPropValueByMethod() throws Exception {
        final SetterTarget target = new SetterTarget();
        final Method setCount = SetterTarget.class.getMethod("setCount", int.class);
        final Method setRatio = SetterTarget.class.getMethod("setRatio", double.class);
        final Method setId = SetterTarget.class.getMethod("setId", Long.class);
        final Method setLabel = SetterTarget.class.getMethod("setLabel", String.class);
        final Method setTags = SetterTarget.class.getMethod("setTags", List.class);

        for (int i = 0; i < 250; i++) {
            assertEquals(i, Beans.setPropValue(target, setCount, String.valueOf(i)));
            assertEquals(i, target.count);
        }

        assertEquals((short) 3, Beans.setPropValue(target, setCount, (short) 3)); // widened by reflection, not converted
        assertEquals(3, target.count);
        assertEquals(1.1f, Beans.setPropValue(target, setRatio, 1.1f));
        assertEquals((double) 1.1f, target.ratio);
        assertEquals(7L, Beans.setPropValue(target, setId, 7));
        assertEquals(Long.valueOf(7), target.id);
        assertEquals("12", Beans.setPropValue(target, setLabel, 12));
        assertEquals("12", target.label);
        Beans.setPropValue(target, setTags, "a");
        assertEquals(List.of("a"), target.tags);
        assertEquals(0, Beans.setPropValue(target, setCount, null));
        assertEquals(0, target.count);

        // the setter's own failure, for a value that needed conversion and for one that did not
        assertThrows(IllegalStateException.class, () -> Beans.setPropValue(target, setRatio, "-2"));
        assertThrows(IllegalStateException.class, () -> Beans.setPropValue(target, setRatio, -2.0));
        // a conversion that cannot succeed
        assertThrows(NumberFormatException.class, () -> Beans.setPropValue(target, setCount, "x"));
        // an instance setter without a target
        assertThrows(NullPointerException.class, () -> Beans.setPropValue(null, setCount, "5"));
        assertThrows(NullPointerException.class, () -> Beans.setPropValue(null, setCount, 5));
        // not a one-argument setter
        final Method setPair = SetterTarget.class.getMethod("setPair", int.class, int.class);
        assertThrows(IllegalArgumentException.class, () -> Beans.setPropValue(target, setPair, "5"));
        // an inaccessible setter fails on access (not on the value), before any conversion
        final Method setHidden = SetterTarget.class.getDeclaredMethod("setHidden", int.class);
        final RuntimeException e = assertThrows(RuntimeException.class, () -> Beans.setPropValue(target, setHidden, "x"));
        assertTrue(e.getCause() instanceof IllegalAccessException, e.toString());
    }

    // ---- Strings.swapCase ----

    private static String swapCaseReferenceWithoutSigma(final String str) {
        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < str.length();) {
            final int cp = str.codePointAt(i);
            final int end = i + Character.charCount(cp);

            if (Character.isUpperCase(cp) || Character.isTitleCase(cp)) {
                sb.append(str.substring(i, end).toLowerCase(Locale.ROOT));
            } else if (Character.isLowerCase(cp)) {
                sb.append(str.substring(i, end).toUpperCase(Locale.ROOT));
            } else {
                sb.append(str, i, end);
            }

            i = end;
        }

        return sb.toString();
    }

    @Test
    public void swapCaseMixedAsciiAndUnicode() {
        final Random rnd = new Random(927);
        final String alphabet = "aZzA09 _-.,'\t\n\u00C0\u00E9\u00DF\u01C5\u0130\u0131\u0391\u03B1\u03D0\u0416\u0436\u1E9E\u4E2D\uD801\uDC00\uD801\uDC28\uFB00\u0301";

        for (int k = 0; k < 3000; k++) {
            final int len = rnd.nextInt(30);
            final StringBuilder sb = new StringBuilder();

            for (int i = 0; i < len; i++) {
                final int idx = rnd.nextInt(alphabet.length());

                if (Character.isHighSurrogate(alphabet.charAt(idx)) && idx + 1 < alphabet.length()) {
                    sb.append(alphabet, idx, idx + 2);
                } else if (!Character.isLowSurrogate(alphabet.charAt(idx))) {
                    sb.append(alphabet.charAt(idx));
                }
            }

            final String s = sb.toString();
            assertEquals(swapCaseReferenceWithoutSigma(s), Strings.swapCase(s), s);
        }

        final String longMixed = "a".repeat(10_000) + "é" + "B".repeat(10_000);
        assertEquals("A".repeat(10_000) + "É" + "b".repeat(10_000), Strings.swapCase(longMixed));

        // final-sigma context with ASCII letters around it (ASCII letters are cased)
        assertEquals("Aς", Strings.swapCase("aΣ"));
        assertEquals("σA", Strings.swapCase("Σa"));
        assertEquals("AσB", Strings.swapCase("aΣb"));
        assertEquals("ος-α", Strings.swapCase("ΟΣ-Α"));
        assertEquals("ος2Α", Strings.swapCase("ΟΣ2α"));
        assertEquals("tHE DOG HAS A bone é", Strings.swapCase("The dog has a BONE É"));
    }

    // ---- Dates: cached formatters for the zone re-read of a custom zoned pattern ----

    private static String describe(final Calendar c) {
        return c.getTimeInMillis() + "|" + c.getTimeZone().getID() + "|" + c.getTimeZone().getRawOffset() + "|" + c.get(Calendar.YEAR) + "-"
                + c.get(Calendar.MONTH) + "-" + c.get(Calendar.DAY_OF_MONTH) + " " + c.get(Calendar.HOUR_OF_DAY) + ":" + c.get(Calendar.MINUTE) + ":"
                + c.get(Calendar.SECOND);
    }

    @Test
    public void zonedCustomPatternParsesAreRepeatable() {
        final TimeZone brisbane = TimeZone.getTimeZone("Australia/Brisbane");
        final TimeZone tokyo = TimeZone.getTimeZone("Asia/Tokyo");
        final Object[][] cases = { { "2025-01-15 10:30:45 -0500", "yyyy-MM-dd HH:mm:ss Z", tokyo, Locale.US },
                { "2025-01-15 10:30:45 pst", "yyyy-MM-dd HH:mm:ss ZZZZ", brisbane, Locale.US },
                { "2025-07-15 20:30:45 AEST", "yyyy-MM-dd HH:mm:ss z", brisbane, Locale.US },
                { "15/07/2025 10:30:45 AM -0500", "dd/MM/yyyy hh:mm:ss aa Z", tokyo, Locale.US },
                { "2025-01-15 10:30:45 CST", "yyyy-MM-dd HH:mm:ss z", TimeZone.getTimeZone("Asia/Shanghai"), Locale.US },
                { "2025-01-15 10:30:45 NZDT", "yyyy-MM-dd HH:mm:ss z", tokyo, Locale.US },
                { "2025-01-15 10:30:45 +0100", "yyyy-MM-dd HH:mm:ss Z", tokyo, Locale.FRANCE },
                { "2025-01-15 10:30:45 +0100", "yyyy-MM-dd HH:mm:ss Z", tokyo, Locale.GERMANY } };

        final List<String> first = new ArrayList<>();

        for (int round = 0; round < 3; round++) {
            int idx = 0;

            for (final Object[] c : cases) {
                final String text = (String) c[0];
                final String format = (String) c[1];
                final TimeZone tz = (TimeZone) c[2];
                final Locale locale = (Locale) c[3];

                final Calendar cal = Dates.parseToCalendar(text, format, tz, locale);
                final GregorianCalendar gcal = Dates.parseToGregorianCalendar(text, format, tz, locale);
                final XMLGregorianCalendar xcal = Dates.parseToXMLGregorianCalendar(text, format, tz, locale);
                final String described = describe(cal) + " / " + describe(gcal) + " / " + xcal.toXMLFormat();

                if (round == 0) {
                    first.add(described);
                } else {
                    assertEquals(first.get(idx), described, text + " | " + format + " round " + round);
                }

                idx++;
            }
        }

        assertTrue(first.get(0).startsWith(String.valueOf(Dates.parseToCalendar("2025-01-15T15:30:45Z").getTimeInMillis())), first.get(0));
    }

    @Test
    public void zonedCustomPatternsBeyondTheFormatterCacheBound() {
        // More distinct patterns than the formatter cache admits (256): the ones it does not keep are built per call,
        // and every parse gives the same result either way.
        final TimeZone tokyo = TimeZone.getTimeZone("Asia/Tokyo");
        final long expectedMillis = Dates.parseToCalendar("2025-01-15T15:30:45Z").getTimeInMillis();

        for (int round = 0; round < 2; round++) {
            for (int i = 0; i < 300; i++) {
                final String format = "'p" + i + "' yyyy-MM-dd HH:mm:ss Z";
                final Calendar cal = Dates.parseToCalendar("p" + i + " 2025-01-15 10:30:45 -0500", format, tokyo, Locale.US);
                assertEquals(expectedMillis, cal.getTimeInMillis(), format);
                assertEquals(-5 * 60 * 60 * 1000, cal.getTimeZone().getRawOffset(), format);
                assertEquals(10, cal.get(Calendar.HOUR_OF_DAY), format);
            }
        }
    }

    // ---- N.SCHEDULED_EXECUTOR: removeOnCancelPolicy ----

    private static WeakReference<ScheduledFuture<?>> scheduleAndCancel() {
        final ScheduledFuture<?> task = N.SCHEDULED_EXECUTOR.schedule(() -> {
        }, 1, TimeUnit.HOURS);
        assertTrue(task.cancel(false));
        return new WeakReference<>(task);
    }

    // Without removeOnCancelPolicy the executor's delay queue keeps a cancelled task until its deadline (an hour here).
    @Test
    public void cancelledScheduledTaskLeavesTheQueue() throws InterruptedException {
        final WeakReference<ScheduledFuture<?>> ref = scheduleAndCancel();

        for (int i = 0; i < 100 && ref.get() != null; i++) {
            System.gc();
            Thread.sleep(20);
        }

        assertNull(ref.get(), "the cancelled task is still reachable (held by the executor's work queue)");
    }

    // ---- RowDataset.pivot ----

    @Test
    public void pivotFillsSheetLikeSheetRows() {
        final List<String> columnNames = List.of("row", "col", "value");
        final List<List<Object>> columnValues = new ArrayList<>();
        columnValues.add(new ArrayList<>(List.of("A", "A", "B", "C", "A", "C")));
        columnValues.add(new ArrayList<>(List.of("X", "Y", "X", "Z", "X", "Y")));
        columnValues.add(new ArrayList<>(List.of(1, 2, 3, 4, 5, 6)));
        final RowDataset ds = new RowDataset(columnNames, columnValues);

        final Sheet<String, String, Integer> pivot = ds.pivot("row", "col", "value", java.util.stream.Collectors.summingInt(o -> (Integer) o));

        final List<List<Integer>> rows = new ArrayList<>();
        rows.add(Arrays.asList(6, 2, null));
        rows.add(Arrays.asList(3, null, null));
        rows.add(Arrays.asList(null, 6, 4));
        final Sheet<String, String, Integer> expected = Sheet.rows(List.of("A", "B", "C"), List.of("X", "Y", "Z"), rows);

        assertEquals(expected, pivot);
        assertEquals(List.of("A", "B", "C"), new ArrayList<>(pivot.rowKeySet()));
        assertEquals(List.of("X", "Y", "Z"), new ArrayList<>(pivot.columnKeySet()));
        assertEquals(null, pivot.get("B", "Z"));
        assertEquals(6, pivot.get("C", "Y"));
        pivot.set("B", "Z", 99); // still a mutable, unfrozen sheet
        assertEquals(99, pivot.get("B", "Z"));

        final List<List<Object>> emptyValues = new ArrayList<>();
        emptyValues.add(new ArrayList<>());
        emptyValues.add(new ArrayList<>());
        emptyValues.add(new ArrayList<>());
        final Sheet<String, String, Integer> emptyPivot = new RowDataset(columnNames, emptyValues).pivot("row", "col", "value",
                java.util.stream.Collectors.summingInt(o -> (Integer) o));
        assertEquals(Sheet.rows(List.<String> of(), List.<String> of(), new ArrayList<List<Integer>>()), emptyPivot);
        assertEquals(0, emptyPivot.rowCount());
        assertEquals(0, emptyPivot.columnCount());
    }
}
