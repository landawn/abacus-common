package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.google.common.collect.Lists;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.parser.JsonDeserConfig;
import com.landawn.abacus.parser.JsonSerConfig;

/**
 * Pins for the 2026-09-25 fixes in {@link N} (review findings U01-01, U01-04, U01-05, U01-08, U02-02/U19-01, U03-01,
 * U03-02, U03-04, U03-06).
 */
public class NReview20260925Test extends com.landawn.abacus.TestBase {

    // ------------------------------------------------------------------ U01-01 (L12/C-248)

    @Test
    public void testU0101_removeAllFromAListViewThatRematerialisesOnlySomeElements() {
        // Integer.valueOf caches -128..127: the view hands out the SAME instance for 100 and a NEW one for 200/300 on
        // every traversal, so an identity-only removal scored a hit for 100 and silently kept 200 (backing [2, 3]).
        final List<Integer> backing = new ArrayList<>(Arrays.asList(1, 2, 3));
        final List<Integer> view = Lists.transform(backing, i -> i * 100);
        assertTrue(N.removeAll(view, new HashSet<>(Arrays.asList(100, 200))));
        assertEquals(Arrays.asList(3), backing);

        // the other way round (cached last), and a Comparator-based Set argument
        final List<Integer> backing2 = new ArrayList<>(Arrays.asList(3, 2, 1));
        assertTrue(N.removeAll(Lists.transform(backing2, i -> i * 100), new TreeSet<>(Arrays.asList(300, 100))));
        assertEquals(Arrays.asList(2), backing2);

        // all uncached (the C-248 case) and all cached still work
        final List<Integer> backing3 = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
        assertTrue(N.removeAll(Lists.transform(backing3, i -> i * 1000L), new HashSet<>(Arrays.asList(2000L, 3000L))));
        assertEquals(Arrays.asList(1, 4), backing3);
        final List<Integer> backing4 = new ArrayList<>(Arrays.asList(1, 2, 3));
        assertTrue(N.removeAll(Lists.transform(backing4, i -> i + 1), new HashSet<>(Arrays.asList(2, 4))));
        assertEquals(Arrays.asList(2), backing4);

        // nothing matches: false, untouched
        final List<Integer> backing5 = new ArrayList<>(Arrays.asList(1, 2, 3));
        assertFalse(N.removeAll(Lists.transform(backing5, i -> i * 100), new HashSet<>(Arrays.asList(7, 8))));
        assertEquals(Arrays.asList(1, 2, 3), backing5);
        assertFalse(N.removeAll(backing5, new HashSet<Integer>()));
        assertFalse(N.removeAll(backing5, (Set<Integer>) null));

        // the identity-sibling rule of a Set argument still holds for a List receiver (phase 1 decides per position)
        final String s1 = new String("s");
        final String s2 = new String("s");
        final List<String> siblings = new ArrayList<>(Arrays.asList(s1, s2, s1));
        final Set<String> onlyS1 = Collections.newSetFromMap(new IdentityHashMap<>());
        onlyS1.add(s1);
        assertTrue(N.removeAll(siblings, onlyS1));
        assertEquals(1, siblings.size());
        assertTrue(siblings.get(0) == s2);

        // a sequential List, duplicates and nulls (a null element is probed against the Set argument, so a
        // natural-order TreeSet rejects it - documented - and a HashSet absorbs it)
        final List<Integer> linked = new LinkedList<>(Arrays.asList(1, 2, 3, 2, null));
        assertThrows(NullPointerException.class, () -> N.removeAll(linked, new TreeSet<>(Arrays.asList(2))));
        assertEquals(Arrays.asList(1, 2, 3, 2, null), linked);
        assertTrue(N.removeAll(linked, new HashSet<>(Arrays.asList(2, null))));
        assertEquals(Arrays.asList(1, 3), linked);
        assertTrue(N.removeAll(linked, new TreeSet<>(Arrays.asList(3))));
        assertEquals(Arrays.asList(1), linked);

        // a Set argument backed by the receiver itself
        final List<String> self = new ArrayList<>(Arrays.asList("a", "b", "a", "c"));
        assertTrue(N.removeAll(self, new HashSet<>(self.subList(0, 1))));
        assertEquals(Arrays.asList("b", "c"), self);

        // non-List receivers keep the identity rule (Multiset: removeIf visits distinct elements)
        final Multiset<String> ms = Multiset.of("a", "a", "b");
        assertTrue(N.removeAll(ms, new HashSet<>(Arrays.asList("b"))));
        assertEquals(2, ms.getCount("a"));
        assertEquals(0, ms.getCount("b"));

        // an unmodifiable List still fails the same way when something matches
        assertThrows(UnsupportedOperationException.class, () -> N.removeAll(Arrays.asList(1, 2), new HashSet<>(Arrays.asList(1))));
    }

    // ------------------------------------------------------------------ U01-04 (L12/C-252)

    @Test
    public void testU0104_replaceAllAndUpdateAllOnArraysRaiseArrayStoreException() throws Exception {
        final Number[] a = new Integer[] { 1 };
        assertThrows(ArrayStoreException.class, () -> N.replaceAll(a, n -> 1.5));
        assertThrows(ArrayStoreException.class, () -> N.updateAll(a, n -> 1.5));

        N.replaceAll(a, n -> n.intValue() + 1);
        assertEquals(2, a[0]);
        N.updateAll(a, n -> n.intValue() + 1);
        assertEquals(3, a[0]);
    }

    // ------------------------------------------------------------------ U01-05 (L12/C-230)

    @Test
    public void testU0105_frequencyMapCoarseMergeOverflowIsArithmeticExceptionLikeCountBy() {
        final Supplier<Map<String, Integer>> full = () -> new HashMap<>(Map.of("a", Integer.MAX_VALUE));
        assertThrows(ArithmeticException.class, () -> N.frequencyMap(Arrays.asList("a"), full));
        assertThrows(ArithmeticException.class, () -> N.frequencyMap(Arrays.asList("a").iterator(), full));

        final Supplier<Map<String, Integer>> caseInsensitive = () -> {
            final TreeMap<String, Integer> m = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            m.put("a", Integer.MAX_VALUE - 1);
            return m;
        };
        assertThrows(ArithmeticException.class, () -> N.frequencyMap(Arrays.asList("a", "A"), caseInsensitive));
        assertThrows(ArithmeticException.class, () -> N.frequencyMap(Arrays.asList("a", "A").iterator(), caseInsensitive));

        // exactly Integer.MAX_VALUE is fine, and so is countBy's documented sibling behaviour
        final Supplier<Map<String, Integer>> almostFull = () -> new HashMap<>(Map.of("a", Integer.MAX_VALUE - 1));
        assertEquals(Integer.MAX_VALUE, N.frequencyMap(Arrays.asList("a"), almostFull).get("a"));
        assertEquals(Integer.MAX_VALUE, N.frequencyMap(Arrays.asList("a").iterator(), almostFull).get("a"));
        assertThrows(ArithmeticException.class, () -> N.countBy(Arrays.asList("a"), s -> s, full));
    }

    // ------------------------------------------------------------------ U01-08 (L12/C-218, C-263)

    @Test
    public void testU0108_charAndShortDistinctBeyond65536Elements() {
        final char[] chars = new char[70_000];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = (char) (i % 65536);
        }
        final char[] copy = chars.clone();
        final char[] distinctChars = N.distinct(chars);
        assertEquals(65536, distinctChars.length);
        assertEquals(0, distinctChars[0]);
        assertEquals(65535, distinctChars[65535]);
        assertArrayEquals(copy, chars);

        // range overload past the 65536th element: the scan stops once every value has been seen
        final char[] fromOne = N.removeDuplicates(chars, 1, chars.length, false);
        assertEquals(65536, fromOne.length);
        assertEquals(1, fromOne[0]);
        assertEquals(0, fromOne[65535]);

        // longer than 65536 with few distinct values: the result is cut to the distinct count
        final char[] few = new char[70_000];
        for (int i = 0; i < few.length; i++) {
            few[i] = (char) ('a' + i % 3);
        }
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, N.distinct(few));

        final short[] shorts = new short[70_000];
        for (int i = 0; i < shorts.length; i++) {
            shorts[i] = (short) (i % 65536);
        }
        final short[] distinctShorts = N.distinct(shorts);
        assertEquals(65536, distinctShorts.length);
        assertEquals(0, distinctShorts[0]);
        assertEquals(-1, distinctShorts[65535]);
        assertEquals(65536, N.removeDuplicates(shorts, 1, shorts.length, false).length);

        final short[] fewShorts = new short[70_000];
        for (int i = 0; i < fewShorts.length; i++) {
            fewShorts[i] = (short) (i % 5 - 2);
        }
        assertArrayEquals(new short[] { -2, -1, 0, 1, 2 }, N.distinct(fewShorts));
    }

    @Test
    public void testU0108_primitiveSymmetricDifferenceOrderIsAsDocumented() {
        assertArrayEquals(new int[] { 1, 2 }, N.symmetricDifference(new int[] { 1 }, new int[] { 1, 2, 1 }));
        assertArrayEquals(new int[] { 2, 1 }, N.difference(new int[] { 1, 2, 1 }, new int[] { 1 }));
        assertArrayEquals(new char[] { 'a', 'b' }, N.symmetricDifference(new char[] { 'a' }, new char[] { 'a', 'b', 'a' }));
        assertArrayEquals(new long[] { 1, 2 }, N.symmetricDifference(new long[] { 1 }, new long[] { 1, 2, 1 }));
        assertArrayEquals(new short[] { 1, 2 }, N.symmetricDifference(new short[] { 1 }, new short[] { 1, 2, 1 }));
        assertArrayEquals(new byte[] { 1, 2 }, N.symmetricDifference(new byte[] { 1 }, new byte[] { 1, 2, 1 }));
        assertArrayEquals(new double[] { 1, 2 }, N.symmetricDifference(new double[] { 1 }, new double[] { 1, 2, 1 }), 0.0);
        assertArrayEquals(new float[] { 1, 2 }, N.symmetricDifference(new float[] { 1 }, new float[] { 1, 2, 1 }), 0.0f);
        assertArrayEquals(new boolean[] { true, false }, N.symmetricDifference(new boolean[] { true }, new boolean[] { true, false, true }));

        // a-side surplus first, in encounter order
        assertArrayEquals(new int[] { 3, 1, 2 }, N.symmetricDifference(new int[] { 3, 1, 1 }, new int[] { 1, 2 }));
        assertArrayEquals(new int[0], N.symmetricDifference(new int[] { 1, 2 }, new int[] { 2, 1 }));
        assertArrayEquals(new int[] { 1 }, N.symmetricDifference(new int[] { 1 }, null));
    }

    // ------------------------------------------------------------------ U02-02 / U19-01 (L12/C-311)

    @Test
    public void testU0202_exactNumberConversionFastPathsKeepTheResults() {
        // int-holders are read through intValue(); results identical to the Numbers text path
        assertEquals(6, N.sumInt(Arrays.<Number> asList(MutableInt.of(1), new AtomicInteger(2), MutableShort.of((short) 3), MutableByte.of((byte) 0), null)));
        assertEquals(6, N.sumInt(new Number[] { MutableInt.of(1), new AtomicInteger(2), (short) 3, (byte) 0, 0 }));
        assertEquals(-1, N.sumInt(Arrays.<Number> asList(MutableInt.of(Integer.MIN_VALUE), new AtomicInteger(Integer.MAX_VALUE))));
        assertEquals(4L, N.sumIntToLong(Arrays.<Number> asList(MutableInt.of(1), new AtomicInteger(3))));
        assertEquals(2.0, N.averageInt(Arrays.<Number> asList(MutableInt.of(1), new AtomicInteger(3))));
        assertEquals(2.0, N.averageInt(new Number[] { MutableShort.of((short) 1), MutableByte.of((byte) 3) }));

        // out-of-int-range values are still ArithmeticException for the int variant, whatever the holder
        assertThrows(ArithmeticException.class, () -> N.sumInt(Arrays.<Number> asList(3_000_000_000L)));
        assertThrows(ArithmeticException.class, () -> N.sumInt(new Number[] { 1, 4294967297L }));
        assertThrows(ArithmeticException.class, () -> N.sumInt(Arrays.<Number> asList(MutableLong.of(3_000_000_000L))));
        assertThrows(ArithmeticException.class, () -> N.sumInt(Arrays.<Number> asList(new AtomicLong(-3_000_000_000L))));
        final LongAdder bigAdder = new LongAdder();
        bigAdder.add(3_000_000_000L);
        assertThrows(ArithmeticException.class, () -> N.sumInt(Arrays.<Number> asList(bigAdder))); // intValue() would truncate
        assertThrows(ArithmeticException.class, () -> N.averageInt(Arrays.<Number> asList(MutableLong.of(Integer.MAX_VALUE + 1L))));
        assertThrows(ArithmeticException.class, () -> N.sumIntToLong(Arrays.<Number> asList(MutableDouble.of(Double.NaN))));

        // in-range long-holders and fractions still convert as before
        assertEquals(5, N.sumInt(Arrays.<Number> asList(MutableLong.of(2), new AtomicLong(3))));
        assertEquals(1, N.sumInt(Arrays.<Number> asList(MutableDouble.of(1.7))));
        assertEquals(Integer.MAX_VALUE, N.sumInt(Arrays.<Number> asList(MutableLong.of(Integer.MAX_VALUE))));

        // long variant: every exact-by-construction integral type is read through longValue()
        final LongAdder adder = new LongAdder();
        adder.add(5);
        final LongAccumulator accumulator = new LongAccumulator(Long::max, 0);
        accumulator.accumulate(7);
        final List<Number> longs = Arrays.asList(MutableLong.of(1), new AtomicLong(2), adder, accumulator, MutableInt.of(3), new AtomicInteger(6), 4L, 1,
                (short) 3, (byte) 5);
        assertEquals(3.7, N.averageLong(longs), 1e-12);
        assertEquals(3.7, N.averageLong(longs.toArray(new Number[0])), 1e-12);
        assertEquals((double) Long.MAX_VALUE, N.averageLong(Arrays.<Number> asList(MutableLong.of(Long.MAX_VALUE))));
        assertEquals((double) Long.MIN_VALUE, N.averageLong(new Number[] { new AtomicLong(Long.MIN_VALUE) }));
        assertEquals(1.0, N.averageLong(Arrays.<Number> asList(MutableDouble.of(1.7), null, MutableLong.of(2))));
        assertThrows(ArithmeticException.class, () -> N.averageLong(Arrays.<Number> asList(MutableDouble.of(1e19))));

        // the shared constants agree with Numbers on every fast-pathed type
        final Number[] samples = { 7, (short) 7, (byte) 7, new AtomicInteger(7), MutableInt.of(7), MutableShort.of((short) 7), MutableByte.of((byte) 7), 7L,
                new AtomicLong(7), MutableLong.of(7) };
        for (final Number n : samples) {
            assertEquals(Numbers.toInt(n), N.NUM_TO_INT_EXACT.applyAsInt(n), n.getClass().getSimpleName());
            assertEquals(Numbers.toLong(n), N.NUM_TO_LONG_EXACT.applyAsLong(n), n.getClass().getSimpleName());
        }
        assertEquals(0, N.NUM_TO_INT_EXACT.applyAsInt(null));
        assertEquals(0L, N.NUM_TO_LONG_EXACT.applyAsLong(null));
    }

    // ------------------------------------------------------------------ U03-01 (L12/C-242)

    @Test
    public void testU0301_formatXmlKeepsTextMadeOfNonXmlWhitespace() {
        // U+3000 / U+2028 are Character.isWhitespace but XML character data: mixed content, written verbatim
        assertEquals("<r><a>1</a>\u3000<b>2</b></r>", N.formatXml("<r><a>1</a>\u3000<b>2</b></r>"));
        assertEquals("<r><a>1</a>\u2028<b>2</b></r>", N.formatXml("<r><a>1</a>\u2028<b>2</b></r>"));
        assertEquals("<r><a>1</a>\u3000</r>", N.formatXml("<r><a>1</a>\u3000</r>"));
        assertEquals("<r>\u2029<a>1</a></r>", N.formatXml("<r>\u2029<a>1</a></r>"));
        assertEquals("<r>\n    <a>\u3000</a>\n</r>", N.formatXml("<r><a>\u3000</a></r>"));

        // XML whitespace (space, tab, CR, LF) between elements is still replaced by the indentation
        assertEquals("<r>\n    <a>1</a>\n    <b>2</b>\n</r>", N.formatXml("<r> <a>1</a> \t\r\n<b>2</b>\n</r>"));
        assertEquals("<r><a>1</a> x <b>2</b></r>", N.formatXml("<r><a>1</a> x <b>2</b></r>"));
    }

    // ------------------------------------------------------------------ U03-02 (L12/C-259)

    private static void assertTargetTypeMessage(final Executable e) {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, e);
        assertTrue(ex.getMessage().contains("targetType"), ex.getMessage());
        assertFalse(ex.getMessage().contains("targetClass"), ex.getMessage());
    }

    @Test
    public void testU0302_everyFromJsonClassOverloadValidatesTargetTypeUnderItsOwnName() {
        final Class<Object> nullType = null;
        final JsonDeserConfig cfg = JsonDeserConfig.create();
        final byte[] bytes = "{}".getBytes(StandardCharsets.UTF_8);

        assertTargetTypeMessage(() -> N.fromJson("{}", nullType));
        assertTargetTypeMessage(() -> N.fromJson("{}", cfg, nullType));
        assertTargetTypeMessage(() -> N.fromJson(new File("no-such-file.json"), nullType));
        assertTargetTypeMessage(() -> N.fromJson(new File("no-such-file.json"), cfg, nullType));
        assertTargetTypeMessage(() -> N.fromJson(new ByteArrayInputStream(bytes), nullType));
        assertTargetTypeMessage(() -> N.fromJson(new ByteArrayInputStream(bytes), cfg, nullType));
        assertTargetTypeMessage(() -> N.fromJson(new StringReader("{}"), nullType));
        assertTargetTypeMessage(() -> N.fromJson(new StringReader("{}"), cfg, nullType));
        assertTargetTypeMessage(() -> N.fromJson("{}", 0, 2, nullType));
        assertTargetTypeMessage(() -> N.fromJson("{}", 0, 2, cfg, nullType));

        // the happy paths of the six newly guarded overloads
        assertEquals(Map.of("a", 1), N.fromJson(new ByteArrayInputStream("{\"a\":1}".getBytes(StandardCharsets.UTF_8)), Map.class));
        assertEquals(Map.of("a", 1), N.fromJson(new ByteArrayInputStream("{\"a\":1}".getBytes(StandardCharsets.UTF_8)), cfg, Map.class));
        assertEquals(Map.of("a", 1), N.fromJson(new StringReader("{\"a\":1}"), Map.class));
        assertEquals(Map.of("a", 1), N.fromJson(new StringReader("{\"a\":1}"), cfg, Map.class));
        assertEquals(Map.of("a", 1), N.fromJson("xx{\"a\":1}yy", 2, 9, Map.class));
        assertEquals(Map.of("a", 1), N.fromJson("xx{\"a\":1}yy", 2, 9, cfg, Map.class));
    }

    // ------------------------------------------------------------------ U03-04 (L12/C-204 family)

    private static void assertSupplierReturnedNull(final String name, final Executable e) {
        final NullPointerException ex = assertThrows(NullPointerException.class, e);
        assertEquals(name + " returned null", ex.getMessage());
    }

    @Test
    public void testU0304_supplierReturningNullIsNullPointerAtEveryRemainingSite() {
        final IntFunction<List<String>> nullList = n -> null;
        final Supplier<Map<String, List<String>>> nullGroups = () -> null;
        final Supplier<Map<String, Long>> nullCounts = () -> null;
        final Supplier<Map<String, Integer>> nullIntCounts = () -> null;
        final Function<String, List<String>> one = s -> Arrays.asList(s);
        final Function<String, String> id = s -> s;
        final Predicate<String> yes = s -> true;
        final List<String> some = Arrays.asList("a");
        final List<String> none = Collections.emptyList();

        // two-level flatMap: T[] / Iterable / Iterator, non-empty and empty input
        assertSupplierReturnedNull("supplier", () -> N.flatMap(new String[] { "a" }, one, one, nullList));
        assertSupplierReturnedNull("supplier", () -> N.flatMap(new String[0], one, one, nullList));
        assertSupplierReturnedNull("supplier", () -> N.flatMap(some, one, one, nullList));
        assertSupplierReturnedNull("supplier", () -> N.flatMap(none, one, one, nullList));
        assertSupplierReturnedNull("supplier", () -> N.flatMap(some.iterator(), one, one, nullList));
        assertSupplierReturnedNull("supplier", () -> N.flatMap(none.iterator(), one, one, nullList));

        // mapAndFilter / filterAndMap / flatMapAndFilter / filterAndFlatMap
        assertSupplierReturnedNull("supplier", () -> N.mapAndFilter(some, id, yes, nullList));
        assertSupplierReturnedNull("supplier", () -> N.mapAndFilter(none, id, yes, nullList));
        assertSupplierReturnedNull("supplier", () -> N.filterAndMap(some, yes, id, nullList));
        assertSupplierReturnedNull("supplier", () -> N.filterAndMap(none, yes, id, nullList));
        assertSupplierReturnedNull("supplier", () -> N.flatMapAndFilter(some, one, yes, nullList));
        assertSupplierReturnedNull("supplier", () -> N.flatMapAndFilter(none, one, yes, nullList));
        assertSupplierReturnedNull("supplier", () -> N.filterAndFlatMap(some, yes, one, nullList));
        assertSupplierReturnedNull("supplier", () -> N.filterAndFlatMap(none, yes, one, nullList));

        // distinctBy Iterable / Iterator
        assertSupplierReturnedNull("supplier", () -> N.distinctBy(some, id, nullList));
        assertSupplierReturnedNull("supplier", () -> N.distinctBy(none, id, nullList));
        assertSupplierReturnedNull("supplier", () -> N.distinctBy(some.iterator(), id, nullList));
        assertSupplierReturnedNull("supplier", () -> N.distinctBy(none.iterator(), id, nullList));

        // groupBy: T[] range, Collection range, Iterator, valueExtractor x2, Collector x2
        assertSupplierReturnedNull("mapSupplier", () -> N.groupBy(new String[] { "a" }, 0, 1, id, nullGroups));
        assertSupplierReturnedNull("mapSupplier", () -> N.groupBy(new String[0], 0, 0, id, nullGroups));
        assertSupplierReturnedNull("mapSupplier", () -> N.groupBy(some, 0, 1, id, nullGroups));
        assertSupplierReturnedNull("mapSupplier", () -> N.groupBy(none, 0, 0, id, nullGroups));
        assertSupplierReturnedNull("mapSupplier", () -> N.groupBy(some.iterator(), id, nullGroups));
        assertSupplierReturnedNull("mapSupplier", () -> N.groupBy(none.iterator(), id, nullGroups));
        assertSupplierReturnedNull("mapSupplier", () -> N.groupBy(some, id, id, nullGroups));
        assertSupplierReturnedNull("mapSupplier", () -> N.groupBy(none, id, id, nullGroups));
        assertSupplierReturnedNull("mapSupplier", () -> N.groupBy(some.iterator(), id, id, nullGroups));
        assertSupplierReturnedNull("mapSupplier", () -> N.groupBy(none.iterator(), id, id, nullGroups));
        assertSupplierReturnedNull("mapSupplier", () -> N.groupBy(some, id, Collectors.counting(), nullCounts));
        assertSupplierReturnedNull("mapSupplier", () -> N.groupBy(none, id, Collectors.counting(), nullCounts));
        assertSupplierReturnedNull("mapSupplier", () -> N.groupBy(some.iterator(), id, Collectors.counting(), nullCounts));
        assertSupplierReturnedNull("mapSupplier", () -> N.groupBy(none.iterator(), id, Collectors.counting(), nullCounts));

        // countBy Iterator
        assertSupplierReturnedNull("mapSupplier", () -> N.countBy(some.iterator(), id, nullIntCounts));
        assertSupplierReturnedNull("mapSupplier", () -> N.countBy(none.iterator(), id, nullIntCounts));
    }

    // ------------------------------------------------------------------ U03-06 (L12/C-245)

    @Test
    public void testU0306_formatJsonCopiesANonPrettyConfigAndRejectsMultipleValues() {
        final JsonSerConfig plain = JsonSerConfig.create();
        assertFalse(plain.isPrettyFormat());
        final String formatted = N.formatJson("{\"a\":1}", plain);
        assertTrue(formatted.contains("\n"), formatted);
        assertTrue(formatted.contains("\"a\""), formatted);
        assertFalse(plain.isPrettyFormat()); // the supplied config is copied, not mutated

        final ParsingException two = assertThrows(ParsingException.class, () -> N.formatJson("\"a\" , 1"));
        assertTrue(two.getMessage().contains("Not a single JSON value"), two.getMessage());
        final ParsingException trailing = assertThrows(ParsingException.class, () -> N.formatJson("{\"a\":1} xyz"));
        assertTrue(trailing.getMessage().contains("Unexpected content after the root JSON value"), trailing.getMessage());
        assertThrows(ParsingException.class, () -> N.formatJson("[1] [2]"));
    }
}
