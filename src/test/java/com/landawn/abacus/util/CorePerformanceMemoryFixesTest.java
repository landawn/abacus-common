package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;

import testfixtures.CorePerformanceMemoryFixtures.CalendarType;
import testfixtures.CorePerformanceMemoryFixtures.ReusedList;
import testfixtures.CorePerformanceMemoryFixtures.Supplied;

@Tag("unit")
class CorePerformanceMemoryFixesTest extends TestBase {
    public static final class Bean {
        public int value;
    }

    public static final class LongNamedBean {
        public int propertyWithANameThatIsLongerThanTheCallerCacheAdmissionLimitAndMustStillBeAvailableThroughExactMetadataLookupEvenThoughAliasesAndCallerCachesAreBounded;
    }

    @Test
    void gregorianReconstructionPreservesSettingsAndIndependence() {
        final GregorianCalendar source = new GregorianCalendar(new SimpleTimeZone(1_800_000, "custom-half-hour"), Locale.FRANCE);
        source.setGregorianChange(new Date(-8_000_000_000_000L));
        source.setLenient(false);
        source.setFirstDayOfWeek(Calendar.THURSDAY);
        source.setMinimalDaysInFirstWeek(6);
        source.setTimeInMillis(-9_000_000_000_000L);
        final GregorianCalendar unchanged = (GregorianCalendar) source.clone();

        for (final int amount : new int[] { -100, 0, 100 }) {
            final GregorianCalendar expected = (GregorianCalendar) source.clone();
            expected.setTimeInMillis(source.getTimeInMillis() + amount);
            final GregorianCalendar result = Dates.addMilliseconds(source, amount);
            assertNotSame(source, result);
            assertEquals(GregorianCalendar.class, result.getClass());
            assertEquals(expected, result);
            result.setTimeZone(TimeZone.getTimeZone("UTC"));
            result.setGregorianChange(new Date(Long.MIN_VALUE));
            result.setLenient(true);
            assertEquals(unchanged, source);
        }
    }

    @Test
    void registeredSubclassCreatorStillReceivesAnIsolatedTemplate() {
        final CalendarType source = new CalendarType(1_700_000_000_000L);
        source.setTimeZone(new SimpleTimeZone(7_200_000, "creator-zone"));
        source.setGregorianChange(new Date(Long.MIN_VALUE));
        source.setLenient(false);
        source.setFirstDayOfWeek(Calendar.TUESDAY);
        source.setMinimalDaysInFirstWeek(5);
        final CalendarType unchanged = (CalendarType) source.clone();
        final AtomicInteger calls = new AtomicInteger();
        assertTrue(Dates.registerCalendarCreator(CalendarType.class, (millis, template) -> {
            calls.incrementAndGet();
            assertNotSame(source, template);
            assertEquals(source, template);
            template.clear();
            template.setTimeZone(TimeZone.getTimeZone("UTC"));
            return new CalendarType(millis);
        }));
        try {
            final CalendarType result = Dates.addMilliseconds(source, 1);
            assertEquals(1, calls.get());
            assertEquals(source.getTimeInMillis() + 1, result.getTimeInMillis());
            final CalendarType expected = (CalendarType) source.clone();
            expected.setTimeInMillis(result.getTimeInMillis());
            assertEquals(expected, result);
            assertEquals(unchanged, source);
        } finally {
            assertTrue(Dates.unregisterCalendarCreator(CalendarType.class));
        }
    }

    @Test
    void oversizedCallerNamesAreNotRetainedButExactLongPropertiesStillResolve() throws Exception {
        Beans.getPropGetters(Bean.class);
        final String spelling = "unknown".repeat(10_000);
        assertNull(Beans.getPropGetter(Bean.class, spelling));
        assertNull(Beans.getPropSetter(Bean.class, spelling));
        assertNull(Beans.getPropField(Bean.class, spelling));
        for (final String pool : List.of("beanPropGetMethodPool", "beanPropSetMethodPool", "beanPropFieldPool")) {
            assertFalse(propertyCache(pool, Bean.class).containsKey(spelling));
        }
        final String exact = LongNamedBean.class.getFields()[0].getName();
        assertTrue(exact.length() > 128);
        assertNotNull(Beans.getPropField(LongNamedBean.class, exact));
        assertNotNull(Beans.getPropField(Bean.class, "value"));
    }

    @Test
    void oversizedNameConversionsKeepValuesWithoutPopulatingNamePools() throws Exception {
        final String name = "coreMemoryAdmission_".repeat(40) + "last_part";
        final String camel = Strings.toCamelCase(name);
        assertEquals(camel, Beans.toCamelCase(name));
        assertEquals(camel, Beans.normalizePropName(name));
        assertEquals(Strings.toSnakeCase(name), Beans.toSnakeCase(name));
        assertEquals(Strings.toScreamingSnakeCase(name), Beans.toScreamingSnakeCase(name));
        for (final String pool : List.of("formalizedPropNamePool", "camelCasePropNamePool", "snakeCasePropNamePool", "screamingSnakeCasePropNamePool")) {
            final Field field = Beans.class.getDeclaredField(pool);
            field.setAccessible(true);
            assertFalse(((Map<?, ?>) field.get(null)).containsKey(name));
        }
        assertFalse(NameUtil.isCachedName(camel), "Beans must not move an oversized spelling into the global name pool");
    }

    private static Map<?, ?> propertyCache(final String fieldName, final Class<?> type) throws Exception {
        final Field field = Beans.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        final Object cache = field.get(null);
        final Method get = cache.getClass().getDeclaredMethod("get", Class.class);
        get.setAccessible(true);
        return (Map<?, ?>) get.invoke(cache, type);
    }

    @Test
    void rangeRemovalPreservesRemovalOnlyListsAndSubclassDispatch() {
        final ArrayList<Integer> array = new ArrayList<>(List.of(0, 1, 2, 3, 4, 5, 6));
        assertTrue(N.removeRange(array, 2, 6));
        assertEquals(List.of(0, 1, 6), array);
        assertFalse(N.removeRange(array, 1, 1));

        final RemovalOnlyList removalOnly = new RemovalOnlyList();
        assertTrue(N.removeRange(removalOnly, 1, 5));
        assertEquals(List.of(0, 5), removalOnly);
        final ArraySubclass subclass = new ArraySubclass();
        subclass.addAll(List.of(0, 1, 2, 3, 4, 5));
        assertTrue(N.removeRange(subclass, 1, 5));
        assertTrue(subclass.removeIfCalled);
        assertEquals(List.of(0, 5), subclass);
        assertThrows(UnsupportedOperationException.class, () -> N.removeRange(List.of(0, 1, 2, 3, 4), 0, 4));
    }

    private static final class RemovalOnlyList extends AbstractList<Integer> {
        private final List<Integer> values = new ArrayList<>(List.of(0, 1, 2, 3, 4, 5));

        @Override
        public Integer get(final int index) {
            return values.get(index);
        }

        @Override
        public int size() {
            return values.size();
        }

        @Override
        public Integer remove(final int index) {
            return values.remove(index);
        }
    }

    private static final class ArraySubclass extends ArrayList<Integer> {
        private static final long serialVersionUID = 1L;
        boolean removeIfCalled;

        @Override
        public boolean removeIf(final Predicate<? super Integer> predicate) {
            removeIfCalled = true;
            return super.removeIf(predicate);
        }
    }

    @Test
    void lazyConversionTrackingStillDetectsCyclesAndPreservesAliases() {
        final String text = new String("identity");
        assertSame(text, CommonUtil.convert(text, String.class));
        assertSame(text, CommonUtil.convert(text, CommonUtil.typeOf(String.class)));
        assertEquals(12345L, CommonUtil.convert(12345, Long.class));
        final List<Object> cycle = new ArrayList<>();
        cycle.add(cycle);
        assertSame(cycle, CommonUtil.convert(cycle, List.class));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.convert(cycle, new TypeReference<List<List<String>>>() {
        }.type()));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.convert(cycle, String.class));

        final List<String> child = new ArrayList<>(List.of("1", "2"));
        final List<List<Integer>> converted = CommonUtil.convert(List.of(child, child), new TypeReference<List<List<Integer>>>() {
        }.type());
        assertEquals(List.of(List.of(1, 2), List.of(1, 2)), converted);
        assertEquals(List.of("1", "2"), child);
        assertEquals(List.of(text), CommonUtil.convert(text, new TypeReference<List<String>>() {
        }.type()));
        final Map<String, Object> beanMap = CommonUtil.convert(new Bean(), new TypeReference<Map<String, Object>>() {
        }.type());
        assertEquals(Map.of("value", 0), beanMap);
    }

    /**
     * Converters and collection factories are JVM-global registries without an unregister API. The fixture types are
     * registered once per JVM, on first use of this holder, and read per-run state. A repeated run in the same JVM (an
     * IDE repeat, surefire rerunFailingTestsCount, the class selected twice) then starts from a fresh counter and
     * destination instead of failing the registration assertion, and no destination list stays reachable afterwards.
     */
    private static final class GlobalFixtures {
        static final AtomicInteger SUPPLIED_CONVERSIONS = new AtomicInteger();
        static final AtomicReference<ReusedList> REUSED_LIST_DESTINATION = new AtomicReference<>();
        static final boolean SUPPLIED_CONVERTER_REGISTERED = Converters.register(Supplied.class, (source, target) -> {
            SUPPLIED_CONVERSIONS.incrementAndGet();
            return source.value;
        });
        static final boolean REUSED_LIST_FACTORY_REGISTERED = IntFunctions.registerForCollection(ReusedList.class, capacity -> REUSED_LIST_DESTINATION.get());
    }

    @Test
    void lazyConversionTrackingKeepsConverterAndFactoryGuards() {
        assertTrue(GlobalFixtures.SUPPLIED_CONVERTER_REGISTERED);
        final AtomicInteger calls = GlobalFixtures.SUPPLIED_CONVERSIONS;
        calls.set(0);
        final Supplied source = new Supplied(List.of("4", "5"));
        assertSame(source, CommonUtil.convert(source, Supplied.class));
        assertEquals(0, calls.get());
        assertEquals(List.of(4, 5), CommonUtil.convert(source, new TypeReference<List<Integer>>() {
        }.type()));
        assertEquals(1, calls.get());
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.convert(source, Integer.class));

        final ReusedList destination = new ReusedList();
        GlobalFixtures.REUSED_LIST_DESTINATION.set(destination);
        try {
            assertTrue(GlobalFixtures.REUSED_LIST_FACTORY_REGISTERED);
            assertThrows(IllegalArgumentException.class, () -> CommonUtil.convert(List.of("first", "second"), new TypeReference<List<ReusedList>>() {
            }.type()));
            assertEquals(List.of("first"), destination);
        } finally {
            GlobalFixtures.REUSED_LIST_DESTINATION.set(null);
        }
    }

    @Test
    void scalarConversionsTrackIdentitiesExactlyWhenAContainerIsInvolved() throws Exception {
        final Method predicate = CommonUtil.class.getDeclaredMethod("mayTrackConversionGraphs", Object.class, Type.class);
        predicate.setAccessible(true);
        // Container sources, and the collection and map targets whose fallbacks record what they create, need the map...
        for (final Object[] call : new Object[][] { { List.of(1), String.class }, { Map.of(), String.class }, { new Object[0], String.class },
                { "a", List.class }, { "a", Map.class } }) {
            assertTrue((Boolean) predicate.invoke(null, call[0], CommonUtil.typeOf((Class<?>) call[1])), Arrays.deepToString(call));
        }
        // ...plain scalar conversions, and the object-array fallback, which records nothing, run without allocating it.
        for (final Object[] call : new Object[][] { { "1", Integer.class }, { 1, Long.class }, { "a", String.class }, { 1, int[].class },
                { "a", String[].class }, { 1, Object[].class } }) {
            assertFalse((Boolean) predicate.invoke(null, call[0], CommonUtil.typeOf((Class<?>) call[1])), Arrays.deepToString(call));
        }
        assertArrayEquals(new String[] { "a" }, CommonUtil.convert("a", String[].class));
        assertArrayEquals(new Object[] { 1 }, CommonUtil.convert(1, Object[].class));
        assertEquals(List.of("a"), CommonUtil.convert("a", List.class));
        assertEquals(12345L, (long) CommonUtil.convert(12345, Long.class));
    }

    @Test
    void globalConverterAndFactoryFixturesSurviveARepeatedRunInTheSameJvm() {
        // The same registry checks as above, run twice in one JVM: the second run must not depend on re-registering.
        for (int run = 0; run < 2; run++) {
            lazyConversionTrackingKeepsConverterAndFactoryGuards();
        }
        assertNull(GlobalFixtures.REUSED_LIST_DESTINATION.get());
    }

    @Test
    void allPrimitiveFiltersKeepOneOrderedPassAfterAnEarlyDensePrefix() {
        final int length = 20_000;
        final boolean[] booleans = new boolean[length];
        final byte[] bytes = new byte[length];
        final short[] shorts = new short[length];
        final char[] chars = new char[length];
        final int[] ints = new int[length];
        final long[] longs = new long[length];
        final float[] floats = new float[length];
        final double[] doubles = new double[length];
        for (int i = 0; i < length; i++) {
            booleans[i] = (i & 1) == 0;
            bytes[i] = (byte) i;
            shorts[i] = (short) i;
            chars[i] = (char) i;
            ints[i] = i;
            longs[i] = i;
            floats[i] = i;
            doubles[i] = i;
        }
        final int from = 7;
        final int to = length - 9;
        final int[] position = { from };
        assertArrayEquals(Arrays.copyOfRange(booleans, from, from + 17), N.filter(booleans, from, to, value -> {
            final int i = position[0]++;
            assertEquals(booleans[i], value);
            return i < from + 17;
        }));
        assertEquals(to, position[0]);
        position[0] = from;
        assertArrayEquals(Arrays.copyOfRange(bytes, from, from + 17), N.filter(bytes, from, to, value -> {
            final int i = position[0]++;
            assertEquals(bytes[i], value);
            return i < from + 17;
        }));
        assertEquals(to, position[0]);
        position[0] = from;
        assertArrayEquals(Arrays.copyOfRange(shorts, from, from + 17), N.filter(shorts, from, to, value -> {
            final int i = position[0]++;
            assertEquals(shorts[i], value);
            return i < from + 17;
        }));
        assertEquals(to, position[0]);
        position[0] = from;
        assertArrayEquals(Arrays.copyOfRange(chars, from, from + 17), N.filter(chars, from, to, value -> {
            final int i = position[0]++;
            assertEquals(chars[i], value);
            return i < from + 17;
        }));
        assertEquals(to, position[0]);
        position[0] = from;
        assertArrayEquals(Arrays.copyOfRange(ints, from, from + 17), N.filter(ints, from, to, value -> {
            final int i = position[0]++;
            assertEquals(i, value);
            return i < from + 17;
        }));
        assertEquals(to, position[0]);
        position[0] = from;
        assertArrayEquals(Arrays.copyOfRange(longs, from, from + 17), N.filter(longs, from, to, value -> {
            final int i = position[0]++;
            assertEquals(longs[i], value);
            return i < from + 17;
        }));
        assertEquals(to, position[0]);
        position[0] = from;
        assertArrayEquals(Arrays.copyOfRange(floats, from, from + 17), N.filter(floats, from, to, value -> {
            final int i = position[0]++;
            assertEquals(floats[i], value);
            return i < from + 17;
        }));
        assertEquals(to, position[0]);
        position[0] = from;
        assertArrayEquals(Arrays.copyOfRange(doubles, from, from + 17), N.filter(doubles, from, to, value -> {
            final int i = position[0]++;
            assertEquals(doubles[i], value);
            return i < from + 17;
        }));
        assertEquals(to, position[0]);
    }

    private static long allocatedBytes(final Runnable action) {
        final com.sun.management.ThreadMXBean mx = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        action.run(); // first call: lambda linkage and class initialization are not part of the filter
        final long before = mx.getCurrentThreadAllocatedBytes();
        action.run();
        return mx.getCurrentThreadAllocatedBytes() - before;
    }

    private static boolean allocationMeasurable() {
        return ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean mx && mx.isThreadAllocatedMemorySupported()
                && mx.isThreadAllocatedMemoryEnabled();
    }

    @Test
    void mediumDensityFiltersReserveTheProjectedResultInsteadOfTheRange() {
        assumeTrue(allocationMeasurable());
        final int n = 200_000;
        final int[] ints = new int[n];
        final long[] longs = new long[n];
        final boolean[] coin = new boolean[n];
        final Random random = new Random(31);
        for (int i = 0; i < n; i++) {
            ints[i] = i;
            longs[i] = i;
            coin[i] = random.nextBoolean();
        }
        // Allocation per accepted element, in result sizes (4 bytes per int). Reserving the whole range only once a
        // quarter of it had matched cost 4.3 (every second), 6.0 (every third), 4.3 (random half) and 4.3 (sorted tail)
        // result sizes; reserving the projected count once enough inputs were seen costs about one result size.
        final Object[][] cases = { { "all", (IntPredicate) x -> true, 1.6 }, { "every 2nd", (IntPredicate) x -> (x & 1) == 0, 1.15 },
                { "every 3rd", (IntPredicate) x -> x % 3 == 0, 1.15 }, { "every 4th", (IntPredicate) x -> (x & 3) == 0, 1.15 },
                { "every 10th", (IntPredicate) x -> x % 10 == 0, 1.15 }, { "random half", (IntPredicate) x -> coin[x], 2.25 },
                { "sorted tail (x > t)", (IntPredicate) x -> x >= n / 2, 1.15 },
                { "sparse then dense", (IntPredicate) x -> x >= n * 9 / 10 || x % 10 == 0, 2.5 } };
        for (final Object[] testCase : cases) {
            final IntPredicate predicate = (IntPredicate) testCase[1];
            final int[] expected = Arrays.stream(ints).filter(predicate).toArray();
            assertArrayEquals(expected, N.filter(ints, predicate::test));
            final long bytes = allocatedBytes(() -> N.filter(ints, predicate::test));
            assertTrue(bytes <= (double) testCase[2] * 4 * expected.length + 4096, testCase[0] + ": " + bytes + " bytes for " + expected.length + " ints");
            final long longBytes = allocatedBytes(() -> N.filter(longs, value -> predicate.test((int) value)));
            assertTrue(longBytes <= (double) testCase[2] * 8 * expected.length + 4096, testCase[0] + " (long): " + longBytes + " bytes");
        }
        // CORE-06's case stays bounded by the accepted count: a dense prefix that ends before the evidence threshold
        // (max(64, range / 128) inputs) only doubles, whether it is 17 values or a thousand.
        assertTrue(allocatedBytes(() -> N.filter(ints, x -> x < 17)) < 1024);
        assertTrue(allocatedBytes(() -> N.filter(ints, x -> x < 1000)) < 4 * 4 * 1000 + 1024);
    }

    @Test
    void filterCapacityDoublesBeforeTheEvidenceThresholdAndNeverOverReserves() throws Exception {
        final Method filterCapacity = N.class.getDeclaredMethod("filterCapacity", int.class, int.class, int.class, int.class, int.class);
        filterCapacity.setAccessible(true);
        for (final int n : new int[] { 100, 5_000, 100_000 }) {
            final int threshold = Math.max(64, n >>> 7);
            final List<IntPredicate> patterns = List.of(x -> true, x -> x < 17, x -> x < threshold, x -> x < n / 3, x -> x >= n / 2, x -> (x & 1) == 0,
                    x -> x % 7 == 0, x -> x == n - 1, x -> (x / 1000 & 1) == 0,
                    x -> Integer.toUnsignedLong(x * 0x9E3779B9) < (long) ((double) x / n * 4294967296.0));
            for (final IntPredicate pattern : patterns) {
                // The production loop's bookkeeping, over a 0-based range.
                int len = 0, count = 0, lastGrowth = 0, lastLen = 0;
                for (int i = 0; i < n; i++) {
                    if (pattern.test(i)) {
                        if (count == len) {
                            final int capacity = (Integer) filterCapacity.invoke(null, len, lastLen, i - lastGrowth, i, n - i);
                            assertTrue(capacity > len, "room for the current match");
                            assertTrue(capacity <= (long) len + n - i, "never more than the rest of the range can add");
                            if (i < threshold) {
                                assertTrue(capacity <= Math.max(16, 2L * len), "only doubling before the evidence threshold: " + capacity + " at " + i);
                            }
                            final long cap = Math.max(2L * len, n >>> 1);
                            assertTrue(capacity <= cap + (cap >>> 6) + 16, "a reservation is at most half the range or a doubling: " + capacity);
                            lastGrowth = i;
                            lastLen = len;
                            len = capacity;
                        }
                        count++;
                    }
                }
            }
        }
    }

    @Test
    void denseFilterGrowthPreservesMutationAndFailureTiming() {
        final int[] input = new int[10_000];
        for (int i = 0; i < input.length; i++) {
            input[i] = i;
        }
        final int[] calls = { 0 };
        final int[] output = N.filter(input, value -> {
            final int index = calls[0]++;
            assertEquals(index, value);
            input[index] = -index;
            return true;
        });
        assertEquals(input.length, calls[0]);
        assertNotSame(input, output);
        assertArrayEquals(input, output, "accepted values are read after the predicate, including its mutation");

        calls[0] = 0;
        final RuntimeException failure = new IllegalStateException("predicate failed");
        assertSame(failure, assertThrows(RuntimeException.class, () -> N.filter(input, value -> {
            if (calls[0]++ == 777) {
                throw failure;
            }
            return true;
        })));
        assertEquals(778, calls[0]);
    }

    @Test
    void primitiveCollisionFallbackPreservesOrderAndCallerArrays() {
        final int count = 4_096;
        final int inverse32 = BigInteger.valueOf(0x9E3779B9L).modInverse(BigInteger.ONE.shiftLeft(32)).intValue();
        final long inverse64 = new BigInteger("9E3779B97F4A7C15", 16).modInverse(BigInteger.ONE.shiftLeft(64)).longValue();
        final int[] ints = new int[count];
        final long[] longs = new long[count];
        for (int i = 0; i < count; i++) {
            ints[i] = (count - i) * inverse32;
            longs[i] = (count - i) * inverse64;
        }
        final int[] unchangedInts = ints.clone();
        final long[] unchangedLongs = longs.clone();
        assertFalse(N.containsDuplicates(ints));
        assertFalse(N.containsDuplicates(longs));
        assertArrayEquals(ints, N.removeDuplicates(ints));
        assertArrayEquals(longs, N.removeDuplicates(longs));
        final int[] repeatedInts = Arrays.copyOf(ints, count + 2);
        final long[] repeatedLongs = Arrays.copyOf(longs, count + 2);
        repeatedInts[count] = ints[0];
        repeatedInts[count + 1] = ints[count - 1];
        repeatedLongs[count] = longs[0];
        repeatedLongs[count + 1] = longs[count - 1];
        assertTrue(N.containsDuplicates(repeatedInts));
        assertTrue(N.containsDuplicates(repeatedLongs));
        assertArrayEquals(ints, N.removeDuplicates(repeatedInts));
        assertArrayEquals(longs, N.removeDuplicates(repeatedLongs));
        assertArrayEquals(unchangedInts, ints);
        assertArrayEquals(unchangedLongs, longs);
    }

    private static final int INVERSE32 = BigInteger.valueOf(0x9E3779B9L).modInverse(BigInteger.ONE.shiftLeft(32)).intValue();
    private static final long INVERSE64 = new BigInteger("9E3779B97F4A7C15", 16).modInverse(BigInteger.ONE.shiftLeft(64)).longValue();

    // A white-box handle on N.IntKeySet / N.LongKeySet, whose fallback is invisible through the public results.
    private static final class KeySet {
        final boolean intKeys;
        final Object set;
        final Method add;

        KeySet(final boolean intKeys, final int expectedSize) throws Exception {
            this.intKeys = intKeys;
            final Class<?> type = Class.forName(N.class.getName() + (intKeys ? "$IntKeySet" : "$LongKeySet"));
            final Constructor<?> constructor = type.getDeclaredConstructor(int.class);
            constructor.setAccessible(true);
            set = constructor.newInstance(expectedSize);
            add = type.getDeclaredMethod("add", intKeys ? int.class : long.class);
            add.setAccessible(true);
        }

        boolean add(final long key) throws Exception {
            return (Boolean) (intKeys ? add.invoke(set, (int) key) : add.invoke(set, key));
        }

        Object field(final String name) throws Exception {
            final Field field = set.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(set);
        }

        boolean boxed() throws Exception {
            return field("boxed") != null;
        }

        // A key whose multiplicative hash puts it in `slot` of a table with `bits` index bits; `salt` keeps keys distinct.
        long keyAt(final int bits, final long slot, final long salt) {
            return intKeys ? (int) (((slot << (32 - bits)) + salt) * INVERSE32) : ((slot << (64 - bits)) + salt) * INVERSE64;
        }
    }

    @Test
    void longButCheapProbeRunsKeepThePrimitiveKeySet() throws Exception {
        for (final boolean intKeys : new boolean[] { true, false }) {
            // 128 slots: 64 keys own consecutive home slots without a single collision; one more key with home slot 0
            // then probes the whole run of 64 occupied slots. That run used to switch the set to its boxed fallback.
            final KeySet set = new KeySet(intKeys, 64);
            final List<Long> keys = new ArrayList<>();
            for (int slot = 0; slot < 64; slot++) {
                keys.add(set.keyAt(7, slot, 0));
            }
            keys.add(set.keyAt(7, 0, 1));
            for (final long key : keys) {
                assertTrue(set.add(key));
            }
            assertFalse(set.boxed(), "a single 64-probe run of otherwise collision-free insertions stays primitive");
            for (final long key : keys) {
                assertFalse(set.add(key));
            }
            assertTrue(set.add(set.keyAt(7, 100, 2)));
            assertFalse(set.boxed());
        }

        // A million consecutive whole-number doubles reach a 64-probe run under the multiplicative hash but average
        // far less than one probe per key; with the fixed cap, removeDuplicates/containsDuplicates on them fell back.
        final KeySet doubles = new KeySet(false, 64);
        for (int i = 0; i < 1_000_000; i++) {
            assertTrue(doubles.add(Double.doubleToLongBits(600_000.0 + i)));
        }
        assertFalse(doubles.boxed(), "whole-number doubles are ordinary keys");
        assertFalse(doubles.add(Double.doubleToLongBits(600_000.0)));
    }

    @Test
    void collisionFloodsStillFallBackAfterBoundedWork() throws Exception {
        for (final boolean intKeys : new boolean[] { true, false }) {
            final KeySet set = new KeySet(intKeys, 64);
            int added = 0;
            while (!set.boxed()) {
                // Every key hashes to slot 0 at every table size, so each insertion probes the whole cluster.
                assertTrue(set.add(set.keyAt(7, 0, ++added)));
                assertTrue(added < 200, "colliding keys must reach the boxed fallback after a bounded cluster");
            }
            assertTrue(added > 64, "the first 64 probes of each insertion are free");
            for (int salt = 1; salt <= added; salt++) {
                assertFalse(set.add(set.keyAt(7, 0, salt)), "every key added before the fallback is kept");
            }
            assertTrue(set.add(set.keyAt(7, 0, added + 1)));
        }
    }

    @Test
    void boxedFallbackIsAHashSetThatKeepsKeysWhoseHashCodesCollide() throws Exception {
        for (final boolean intKeys : new boolean[] { true, false }) {
            final KeySet set = new KeySet(intKeys, 64);
            int salt = 0;
            while (!set.boxed()) {
                assertTrue(set.add(set.keyAt(7, 0, ++salt)));
            }
            // Keys crafted onto one slot of the primitive table spread out in a HashSet; a TreeSet paid a cache-missing
            // logarithmic descent for every key.
            assertInstanceOf(java.util.HashSet.class, set.field("boxed"));

            // Keys sharing one bucket, which HashMap keeps as a balanced tree of Comparable keys.
            final int count = 20_000;
            for (int i = 0; i < count; i++) {
                assertTrue(set.add(collidingKey(intKeys, i)));
            }
            for (int i = 0; i < count; i++) {
                assertFalse(set.add(collidingKey(intKeys, i)), "key " + i);
            }
            for (int i = 1; i <= salt; i++) {
                assertFalse(set.add(set.keyAt(7, 0, i)), "every key added before the fallback is kept");
            }
            assertEquals(salt + count, ((java.util.Set<?>) set.field("boxed")).size());
        }
    }

    // Distinct keys in one HashMap bucket once boxed: Long keys with equal hashCode(), and int keys whose spread hash
    // (h ^ h >>> 16) agrees in its low 16 bits, which index every table of up to 2^16 buckets.
    private static long collidingKey(final boolean intKeys, final int i) {
        final long x = i + 1;
        return intKeys ? (x << 16) | ((x ^ 0x5A5A) & 0xFFFF) : (x << 32) | ((x ^ 0x5A5A5A5AL) & 0xFFFFFFFFL);
    }

    @Test
    void rehashFallbackMigratesTheCompleteOldTable() throws Exception {
        for (final boolean intKeys : new boolean[] { true, false }) {
            // 512 slots (9 bits), growing past 256 keys. 106 keys share home slot 0, before and after the growth.
            // Inserting them charges 861 probes (only probes beyond 64 per insertion count), within the budget of
            // size + 1024. The rehash places the cluster again and charges as much once more, past the 1281 allowed at
            // 257 keys, so the set falls back part-way through the rehash. 102-112 such keys behave this way; fewer
            // never exhaust the budget, and 114 or more exhaust it while being inserted.
            final KeySet set = new KeySet(intKeys, 256);
            final List<Long> keys = new ArrayList<>();
            for (int salt = 1; salt <= 106; salt++) {
                keys.add(set.keyAt(9, 0, salt));
            }
            for (int slot = 128; keys.size() < 256; slot++) {
                keys.add(set.keyAt(9, slot, 0));
            }
            for (final long key : keys) {
                assertTrue(set.add(key));
            }
            assertFalse(set.boxed(), "inserting the cluster stays within the budget");
            final long trigger = set.keyAt(9, 400, 0);
            keys.add(trigger);
            assertTrue(set.add(trigger)); // size 257 > 256: grow, whose rehash of the cluster exhausts the budget
            assertTrue(set.boxed(), "the rehash fell back");
            assertNull(set.field("keys"));
            assertEquals(keys.size(), ((java.util.Set<?>) set.field("boxed")).size());
            for (final long key : keys) {
                assertFalse(set.add(key), "no key is lost from the partially rehashed table");
            }
        }
    }

    @Test
    void floatingCollisionFallbackKeepsFirstNanPayloadAndSignedZeros() {
        final int inverse32 = BigInteger.valueOf(0x9E3779B9L).modInverse(BigInteger.ONE.shiftLeft(32)).intValue();
        final long inverse64 = new BigInteger("9E3779B97F4A7C15", 16).modInverse(BigInteger.ONE.shiftLeft(64)).longValue();
        final float[] floats = new float[260];
        final double[] doubles = new double[260];
        for (int i = 0; i < 256; i++) {
            floats[i] = Float.intBitsToFloat((i + 1) * inverse32);
            doubles[i] = Double.longBitsToDouble((i + 1) * inverse64);
        }
        floats[256] = -0f;
        floats[257] = 0f;
        floats[258] = Float.intBitsToFloat(0x7fc00001);
        floats[259] = Float.intBitsToFloat(0x7fc00002);
        doubles[256] = -0d;
        doubles[257] = 0d;
        doubles[258] = Double.longBitsToDouble(0x7ff8000000000001L);
        doubles[259] = Double.longBitsToDouble(0x7ff8000000000002L);
        final Map<Integer, Integer> floatExpected = new LinkedHashMap<>();
        final Map<Long, Long> doubleExpected = new LinkedHashMap<>();
        for (final float value : floats) {
            floatExpected.putIfAbsent(Float.floatToIntBits(value), Float.floatToRawIntBits(value));
        }
        for (final double value : doubles) {
            doubleExpected.putIfAbsent(Double.doubleToLongBits(value), Double.doubleToRawLongBits(value));
        }
        final float[] actualFloats = N.removeDuplicates(floats);
        final double[] actualDoubles = N.removeDuplicates(doubles);
        final List<Integer> floatBits = new ArrayList<>();
        final List<Long> doubleBits = new ArrayList<>();
        for (final float value : actualFloats) {
            floatBits.add(Float.floatToRawIntBits(value));
        }
        for (final double value : actualDoubles) {
            doubleBits.add(Double.doubleToRawLongBits(value));
        }
        assertEquals(new ArrayList<>(floatExpected.values()), floatBits);
        assertEquals(new ArrayList<>(doubleExpected.values()), doubleBits);
        assertTrue(N.containsDuplicates(floats));
        assertTrue(N.containsDuplicates(doubles));
    }

    @Test
    void sharedDoubleSumStateKeepsOverflowCancellationAndSimpleFiniteFallback() {
        assertEquals(5d, N.sum(1e308, 1e308, -1e308, -1e308, 5d));
        assertEquals(Double.MAX_VALUE, N.sum(Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE));
        final double step = Math.scalb(1d, 969);
        assertEquals(Double.MAX_VALUE, N.sum(Double.MAX_VALUE, step, step));
        assertEquals(-Double.MAX_VALUE, N.sum(-Double.MAX_VALUE, -step, -step));
        assertEquals(step, N.sum(Double.MAX_VALUE, step, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE));
        assertEquals(Double.POSITIVE_INFINITY, N.sum(Double.POSITIVE_INFINITY, 1d));
        assertTrue(Double.isNaN(N.sum(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)));
        assertTrue(Double.isNaN(N.sum(Double.NaN, 1d)));
        assertEquals(Double.doubleToRawLongBits(0d), Double.doubleToRawLongBits(N.sum(-0d, -0d)));
        assertEquals(Double.POSITIVE_INFINITY, KahanSummation.of(Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE).sum());
        assertTrue(Double.isNaN(KahanSummation.of(Double.MAX_VALUE, step, step).sum()), "public Kahan sum retains its existing IEEE result");
    }
}
