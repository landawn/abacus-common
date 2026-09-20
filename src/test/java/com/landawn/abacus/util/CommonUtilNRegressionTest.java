package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Covers the fixes applied on 2026-08-31 after the second full review pass over {@code CommonUtil}/{@code N}:
 *
 * <ul>
 *   <li>{@code deepToString(Object[], int, int)} sized its cycle-detection set by the whole array rather than the
 *       requested range.</li>
 *   <li>The no-{@code Random} {@code shuffle} overloads used a shared {@code SecureRandom}.</li>
 *   <li>{@code nullToEmpty(T[], Class)} threw a message-less NPE for a {@code null} array type.</li>
 *   <li>{@code println} leaked a pooled {@code StringBuilder} when rendering threw.</li>
 *   <li>{@code excludeAll}/{@code excludeAllToSet}'s single-element shortcut bypassed the documented
 *       "a {@code Set} argument defines membership" contract.</li>
 *   <li>{@code removeAll(Collection, Iterable)} snapshotted a {@code List} argument into a {@code List}, making the
 *       removal quadratic.</li>
 *   <li>Assorted validation-order / naming / javadoc corrections.</li>
 * </ul>
 */
public class CommonUtilNRegressionTest extends TestBase {

    // ================================================================================================
    // B2 - deepToString(Object[], from, to) must not scale with the array length
    // ================================================================================================

    @Test
    public void deepToString_rangeResultsAreUnchanged() {
        final Object[] a = { "x", new int[] { 1, 2 }, new String[] { "p", "q" }, null };

        assertEquals("[x]", CommonUtil.deepToString(a, 0, 1));
        assertEquals("[[1, 2]]", CommonUtil.deepToString(a, 1, 2));
        assertEquals("[[1, 2], [p, q]]", CommonUtil.deepToString(a, 1, 3));
        assertEquals("[x, [1, 2], [p, q], null]", CommonUtil.deepToString(a));
        assertEquals("[]", CommonUtil.deepToString(a, 2, 2));
    }

    @Test
    public void deepToString_stillDetectsCycles() {
        final Object[] self = new Object[2];
        self[0] = "a";
        self[1] = self;

        assertEquals("[a, [...]]", CommonUtil.deepToString(self));

        final Object[] outer = new Object[1];
        final Object[] inner = new Object[1];
        outer[0] = inner;
        inner[0] = outer;

        assertEquals("[[[...]]]", CommonUtil.deepToString(outer));
    }

    /**
     * Regression guard for the sizing defect: the cycle-detection set used to be allocated with one slot per
     * element of the <i>whole</i> array, so a one-element range over a 3,000,000-element array allocated a ~64 MB
     * identity table (and zero-filled it) to produce three characters. Measured by allocation rather than wall
     * clock so the assertion is deterministic: pre-fix 67,109,008 bytes, post-fix 400 bytes.
     */
    @Test
    public void deepToString_oneElementRangeDoesNotAllocatePerElementOfTheArray() {
        final com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean();
        org.junit.jupiter.api.Assumptions.assumeTrue(bean.isThreadAllocatedMemorySupported() && bean.isThreadAllocatedMemoryEnabled(),
                "per-thread allocation counters unavailable");

        final Object[] big = new Object[3_000_000];
        Arrays.fill(big, "x");

        for (int i = 0; i < 3; i++) { // warm up, and let the array itself be allocated before measuring
            CommonUtil.deepToString(big, 0, 1);
        }

        final long before = bean.getCurrentThreadAllocatedBytes();
        assertEquals("[x]", CommonUtil.deepToString(big, 0, 1));
        final long allocated = bean.getCurrentThreadAllocatedBytes() - before;

        assertTrue(allocated < 64 * 1024, "deepToString of a 1-element range allocated " + allocated + " bytes; it must not scale with the array length");
    }

    // ================================================================================================
    // B3 - shuffle must not default to the shared SecureRandom
    // ================================================================================================

    @Test
    public void shuffle_defaultOverloadsStillPermute() {
        final int[] ints = new int[200];
        for (int i = 0; i < ints.length; i++) {
            ints[i] = i;
        }

        final int[] copy = ints.clone();
        CommonUtil.shuffle(ints);
        Arrays.sort(copy);
        final int[] sorted = ints.clone();
        Arrays.sort(sorted);
        assertArrayEquals(copy, sorted, "shuffle must permute, not change the multiset");

        final String[] objs = { "a", "b", "c", "d", "e", "f", "g", "h" };
        CommonUtil.shuffle(objs);
        assertEquals(CommonUtil.asSet("a", "b", "c", "d", "e", "f", "g", "h"), new HashSet<>(Arrays.asList(objs)));

        final List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        CommonUtil.shuffle(list);
        assertEquals(CommonUtil.asSet("a", "b", "c", "d", "e"), new HashSet<>(list));

        final Set<String> coll = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d", "e"));
        CommonUtil.shuffle(coll);
        assertEquals(CommonUtil.asSet("a", "b", "c", "d", "e"), new HashSet<>(coll));
    }

    @Test
    public void shuffle_rangeOverloadLeavesTheRestUntouched() {
        final int[] a = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        CommonUtil.shuffle(a, 3, 7);

        assertArrayEquals(new int[] { 0, 1, 2 }, Arrays.copyOfRange(a, 0, 3));
        assertArrayEquals(new int[] { 7, 8, 9 }, Arrays.copyOfRange(a, 7, 10));

        final int[] middle = Arrays.copyOfRange(a, 3, 7);
        Arrays.sort(middle);
        assertArrayEquals(new int[] { 3, 4, 5, 6 }, middle);
    }

    @Test
    public void shuffle_nullEmptyAndSingletonAreNoOps() {
        CommonUtil.shuffle((int[]) null);
        CommonUtil.shuffle(new int[0]);
        CommonUtil.shuffle((Object[]) null);
        CommonUtil.shuffle((List<String>) null);
        CommonUtil.shuffle((Collection<String>) null);

        final int[] one = { 7 };
        CommonUtil.shuffle(one);
        assertArrayEquals(new int[] { 7 }, one);

        final int[] range = { 1, 2, 3 };
        CommonUtil.shuffle(range, 1, 2); // a one-element range is a no-op
        assertArrayEquals(new int[] { 1, 2, 3 }, range);

        final List<String> single = new ArrayList<>(Arrays.asList("a"));
        CommonUtil.shuffle(single);
        assertEquals(Arrays.asList("a"), single);
    }

    @Test
    public void shuffle_explicitRandomOverloadIsStillDeterministic() {
        final Integer[] a = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        final Integer[] b = a.clone();

        CommonUtil.shuffle(a, new Random(42));
        CommonUtil.shuffle(b, new Random(42));

        assertArrayEquals(a, b, "an explicitly supplied Random must still drive the permutation");
    }

    /**
     * Regression guard: the default RNG used to be a process-wide {@code SecureRandom}, which made this loop
     * roughly 30x slower single-threaded (and far worse under contention).
     */
    @Test
    public void shuffle_defaultRngIsNotTheSharedSecureRandom() {
        final int[] a = new int[10_000];
        for (int i = 0; i < a.length; i++) {
            a[i] = i;
        }

        final Random plain = new Random(42);

        for (int i = 0; i < 20; i++) { // warm up both paths
            CommonUtil.shuffle(a);
            CommonUtil.shuffle(a, plain);
        }

        // Calibrated against a plain java.util.Random in the same run rather than against a fixed wall-clock
        // budget, so the guard keeps discriminating on a slow or loaded machine. A shared SecureRandom was
        // measured at ~34x this reference; ThreadLocalRandom is within noise of it.
        final long referenceMillis = millisFor(() -> CommonUtil.shuffle(a, plain));
        final long defaultMillis = millisFor(() -> CommonUtil.shuffle(a));

        assertTrue(defaultMillis < 5 * referenceMillis + 50, "200 shuffles of int[10000] took " + defaultMillis + " ms against " + referenceMillis
                + " ms for an explicit java.util.Random; the default RNG looks like a SecureRandom again");
    }

    /** Runs {@code op} 200 times and returns the elapsed milliseconds. */
    private static long millisFor(final Runnable op) {
        final long t0 = System.nanoTime();

        for (int i = 0; i < 200; i++) {
            op.run();
        }

        return (System.nanoTime() - t0) / 1_000_000L;
    }

    // ================================================================================================
    // B4 - nullToEmpty(T[], Class) validates arrayType
    // ================================================================================================

    @Test
    public void nullToEmpty_withArrayType_rejectsNullType() {
        final IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> CommonUtil.nullToEmpty((String[]) null, null));
        assertEquals("'arrayType' cannot be null", e1.getMessage());

        // Behaviour change: this used to succeed because arrayType was never touched for a non-null array.
        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> CommonUtil.nullToEmpty(new String[] { "x" }, null));
        assertEquals("'arrayType' cannot be null", e2.getMessage());
    }

    @Test
    public void nullToEmpty_withArrayType_normalBehaviourIsUnchanged() {
        final String[] empty = CommonUtil.nullToEmpty((String[]) null, String[].class);
        assertEquals(0, empty.length);
        assertSame(String.class, empty.getClass().getComponentType());
        assertSame(empty, CommonUtil.nullToEmpty((String[]) null, String[].class), "a registered array type yields the shared instance");

        final String[] given = { "x" };
        assertSame(given, CommonUtil.nullToEmpty(given, String[].class));

        // An array type that is not pre-registered still yields a correctly typed empty array. (Whether that
        // instance is shared is deliberately not asserted: it is the empty-array pool's policy, not this
        // method's contract.)
        final java.time.Month[] custom = CommonUtil.nullToEmpty((java.time.Month[]) null, java.time.Month[].class);
        assertEquals(0, custom.length);
        assertSame(java.time.Month.class, custom.getClass().getComponentType());
        assertEquals(0, CommonUtil.nullToEmpty((java.time.Month[]) null, java.time.Month[].class).length);
    }

    // ================================================================================================
    // B5 - println must not leak a pooled StringBuilder when rendering throws
    // ================================================================================================

    private static final class ThrowingToString {
        @Override
        public String toString() {
            throw new IllegalStateException("boom");
        }
    }

    private static String captureOut(final Runnable r) {
        final PrintStream original = System.out;
        final ByteArrayOutputStream buf = new ByteArrayOutputStream();

        try {
            System.setOut(new PrintStream(buf, true, StandardCharsets.UTF_8));
            r.run();
        } finally {
            System.setOut(original);
        }

        return buf.toString(StandardCharsets.UTF_8).replace("\r\n", "\n").trim();
    }

    @Test
    public void println_rendersTheSameOutputAsBefore() {
        assertEquals("[a, b]", captureOut(() -> N.println(Arrays.asList("a", "b"))));
        assertEquals("[a, b]", captureOut(() -> N.println(new String[] { "a", "b" })));
        assertEquals("{k=v}", captureOut(() -> N.println(CommonUtil.asMap("k", "v"))));
        assertEquals("plain", captureOut(() -> N.println("plain")));
        assertEquals("null", captureOut(() -> N.println((Object) null)));
        assertEquals("[]", captureOut(() -> N.println(new ArrayList<>())));

        final List<String> list = Arrays.asList("a");
        final List<?>[] returned = new List<?>[1];
        captureOut(() -> returned[0] = N.println(list));
        assertSame(list, returned[0], "println returns its argument");
    }

    @Test
    public void println_returnsThePooledBufferForEveryRenderedShape() {
        // All three pooled branches - Collection, Object[] and Map - must release the builder on failure.
        final Object[] badArray = { "ok", new ThrowingToString() };
        final Map<String, Object> badMap = CommonUtil.newLinkedHashMap();
        badMap.put("ok", "v");
        badMap.put("boom", new ThrowingToString());

        for (final Object bad : new Object[] { Arrays.asList("ok", new ThrowingToString()), badArray, badMap }) {
            final List<StringBuilder> drained = new ArrayList<>();
            for (int i = 0; i < 128; i++) {
                drained.add(Objectory.createStringBuilder());
            }

            final StringBuilder marker = drained.get(0);
            Objectory.recycle(marker);

            assertThrows(IllegalStateException.class, () -> captureOut(() -> N.println(bad)));
            assertSame(marker, Objectory.createStringBuilder(), "the pooled StringBuilder was not returned for " + bad.getClass().getSimpleName());

            for (final StringBuilder sb : drained) {
                Objectory.recycle(sb);
            }
        }
    }

    @Test
    public void println_stillWorksAfterAFailedCall() {
        final List<Object> bad = Arrays.asList("ok", new ThrowingToString());

        for (int i = 0; i < 5; i++) {
            assertThrows(IllegalStateException.class, () -> captureOut(() -> N.println(bad)));
        }

        assertEquals("[a, b]", captureOut(() -> N.println(Arrays.asList("a", "b"))));
    }

    // ================================================================================================
    // excludeAll / excludeAllToSet - the single-element shortcut must not bypass a Set's own membership
    // ================================================================================================

    private static TreeSet<String> caseInsensitive(final String... values) {
        final TreeSet<String> set = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        set.addAll(Arrays.asList(values));
        return set;
    }

    private static Set<String> identitySet(final String... values) {
        final Set<String> set = Collections.newSetFromMap(new IdentityHashMap<>());
        set.addAll(Arrays.asList(values));
        return set;
    }

    @Test
    public void excludeAll_oneElementSetUsesTheSetsOwnMembership() {
        final List<String> c = Arrays.asList("a", "b", "c");

        // Before the fix a one-element Set silently fell back to equals: this returned [a, b, c].
        assertEquals(Arrays.asList("b", "c"), N.excludeAll(c, caseInsensitive("A")));
        assertEquals(Arrays.asList("c"), N.excludeAll(c, caseInsensitive("A", "B")));
    }

    @Test
    public void excludeAll_oneElementIdentitySetUsesIdentity() {
        final String a = new String("a"); // NOSONAR - a distinct instance is the point of the test
        final List<String> c = Arrays.asList("a", "b", "c");

        // Identity membership: a different "a" instance excludes nothing.
        assertEquals(Arrays.asList("a", "b", "c"), N.excludeAll(c, identitySet(a)));
        assertEquals(Arrays.asList("a", "b", "c"), N.excludeAll(c, identitySet(a, new String("b")))); // NOSONAR

        // The very instances held by the collection are excluded.
        assertEquals(Arrays.asList("b", "c"), N.excludeAll(c, identitySet(c.get(0))));
    }

    @Test
    public void excludeAll_oneElementNonSetStillMatchesByEquals() {
        final List<String> c = Arrays.asList("a", "b", "c");

        assertEquals(Arrays.asList("b", "c"), N.excludeAll(c, Arrays.asList("a")));
        assertEquals(Arrays.asList("b", "c"), N.excludeAll(c, Arrays.asList(new String("a")))); // NOSONAR
        assertEquals(Arrays.asList("a", "b", "c"), N.excludeAll(c, Arrays.asList("A")));
        assertEquals(Arrays.asList("a", "b", "c"), N.excludeAll(c, new ArrayList<>()));
    }

    @Test
    public void excludeAll_removesAllOccurrencesAndKeepsOrder() {
        assertEquals(Arrays.asList("b", "c", "b"), N.excludeAll(Arrays.asList("a", "b", "c", "a", "b"), Arrays.asList("a")));
        assertEquals(Arrays.asList("c"), N.excludeAll(Arrays.asList("a", "b", "c", "a", "b"), Arrays.asList("a", "b")));
        assertEquals(new ArrayList<>(), N.excludeAll(new ArrayList<String>(), Arrays.asList("a")));
    }

    @Test
    public void excludeAllToSet_oneElementSetUsesTheSetsOwnMembership() {
        final List<String> c = Arrays.asList("a", "b", "c");

        assertEquals(CommonUtil.asSet("b", "c"), N.excludeAllToSet(c, caseInsensitive("A")));
        assertEquals(CommonUtil.asSet("c"), N.excludeAllToSet(c, caseInsensitive("A", "B")));

        final String a = new String("a"); // NOSONAR
        assertEquals(CommonUtil.asSet("a", "b", "c"), N.excludeAllToSet(c, identitySet(a)));
    }

    @Test
    public void excludeAllToSet_oneElementNonSetStillMatchesByEquals() {
        final List<String> c = Arrays.asList("a", "b", "c", "b");

        final Set<String> result = N.excludeAllToSet(c, Arrays.asList("a"));
        assertEquals(CommonUtil.asSet("b", "c"), result);
        assertTrue(result instanceof LinkedHashSet, "a List source keeps encounter order");
        assertEquals(Arrays.asList("b", "c"), new ArrayList<>(result));
    }

    // ================================================================================================
    // D3 - removeAll(Collection, Iterable) must snapshot into a hash-based container
    // ================================================================================================

    @Test
    public void removeAll_withListArgument_semanticsAreUnchanged() {
        final List<String> c = new ArrayList<>(Arrays.asList("a", "b", "c", "a"));
        assertTrue(N.removeAll(c, Arrays.asList("a", "c")));
        assertEquals(Arrays.asList("b"), c);

        final List<String> none = new ArrayList<>(Arrays.asList("a", "b"));
        assertFalse(N.removeAll(none, Arrays.asList("z")));
        assertEquals(Arrays.asList("a", "b"), none);

        final List<String> withNull = new ArrayList<>(Arrays.asList("a", null, "b"));
        assertTrue(N.removeAll(withNull, Collections.singletonList(null)));
        assertEquals(Arrays.asList("a", "b"), withNull);
    }

    @Test
    public void removeAll_withSortedSetArgument_keepsItsComparator() {
        final List<String> c = new ArrayList<>(Arrays.asList("a", "b", "c"));

        assertTrue(N.removeAll(c, caseInsensitive("A", "B")));
        assertEquals(Arrays.asList("c"), c);
    }

    @Test
    public void removeAll_withPlainSetOrNonCollectionIterableArgument() {
        final List<String> c1 = new ArrayList<>(Arrays.asList("a", "b", "c"));
        assertTrue(N.removeAll(c1, CommonUtil.asSet("a", "c")));
        assertEquals(Arrays.asList("b"), c1);

        final List<String> c2 = new ArrayList<>(Arrays.asList("a", "b", "c"));
        final List<String> backing = Arrays.asList("a", "c");
        final Iterable<String> plainIterable = backing::iterator; // not a Collection: routes through the Iterator overload
        assertTrue(N.removeAll(c2, plainIterable));
        assertEquals(Arrays.asList("b"), c2);

        final List<String> c3 = new ArrayList<>(Arrays.asList("a", "b"));
        assertFalse(N.removeAll(c3, new ArrayList<>()));
        assertEquals(Arrays.asList("a", "b"), c3);
    }

    @Test
    public void removeAll_selfRemovalStillWorks() {
        final List<String> c = new ArrayList<>(Arrays.asList("a", "b", "c"));

        assertTrue(N.removeAll(c, c)); // the snapshot exists precisely so this is safe
        assertTrue(c.isEmpty());
    }

    /** Regression guard: a List argument used to be snapshotted into a List, making removeAll quadratic. */
    @Test
    public void removeAll_withLargeListArgumentIsLinear() {
        final int n = 20_000;
        final List<Integer> viaList = new ArrayList<>(n);
        final List<Integer> viaSet = new ArrayList<>(n);
        final List<Integer> toRemove = new ArrayList<>(n);

        for (int i = 0; i < n; i++) {
            viaList.add(i);
            viaSet.add(i);
            toRemove.add(i + n); // disjoint: every probe must scan the whole argument in the quadratic version
        }

        // A Set argument was already snapshotted into a HashSet before the fix, so it is the linear reference.
        // Comparing against it in the same run keeps this guard machine-independent: the List argument used to
        // be ~100x slower, and is now within noise of the Set.
        final Set<Integer> toRemoveSet = new HashSet<>(toRemove);
        final long referenceMillis = millisOnce(() -> assertFalse(N.removeAll(viaSet, toRemoveSet)));
        final long listMillis = millisOnce(() -> assertFalse(N.removeAll(viaList, toRemove)));

        assertEquals(n, viaList.size());
        assertEquals(n, viaSet.size());
        assertTrue(listMillis < 10 * referenceMillis + 50, "removeAll with a " + n + "-element List argument took " + listMillis + " ms against "
                + referenceMillis + " ms for the same values as a Set; it looks quadratic again");
    }

    /** Runs {@code op} once and returns the elapsed milliseconds. */
    private static long millisOnce(final Runnable op) {
        final long t0 = System.nanoTime();
        op.run();
        return (System.nanoTime() - t0) / 1_000_000L;
    }

    // ================================================================================================
    // hasMatchCountBetween - argument validation order
    // ================================================================================================

    @Test
    public void hasMatchCountBetween_reportsTheActuallyInvalidArgument() {
        final String[] a = { "a", "b" };

        assertEquals("'atMost' cannot be negative: -1",
                assertThrows(IllegalArgumentException.class, () -> N.hasMatchCountBetween(a, 0, -1, s -> true)).getMessage());
        assertEquals("'atLeast' cannot be negative: -1",
                assertThrows(IllegalArgumentException.class, () -> N.hasMatchCountBetween(a, -1, 5, s -> true)).getMessage());
        assertEquals("'atLeast' must be <= 'atMost'",
                assertThrows(IllegalArgumentException.class, () -> N.hasMatchCountBetween(a, 3, 2, s -> true)).getMessage());

        assertEquals("'atMost' cannot be negative: -1",
                assertThrows(IllegalArgumentException.class, () -> N.hasMatchCountBetween(Arrays.asList(a), 0, -1, s -> true)).getMessage());
        assertEquals("'atMost' cannot be negative: -1",
                assertThrows(IllegalArgumentException.class, () -> N.hasMatchCountBetween(Arrays.asList(a).iterator(), 0, -1, s -> true)).getMessage());
    }

    @Test
    public void hasMatchCountBetween_normalBehaviourIsUnchanged() {
        final String[] a = { "a", "bb", "ccc" };

        assertTrue(N.hasMatchCountBetween(a, 1, 2, s -> s.length() < 3));
        assertFalse(N.hasMatchCountBetween(a, 3, 3, s -> s.length() < 3));
        assertTrue(N.hasMatchCountBetween(new String[0], 0, 0, s -> true));
        assertFalse(N.hasMatchCountBetween(new String[0], 1, 2, s -> true));
    }

    // ================================================================================================
    // indicesOfMin / indicesOfMax - null policy is unchanged after the comparator rename
    // ================================================================================================

    @Test
    public void indicesOfMinMax_nullPolicyIsUnchanged() {
        final Integer[] a = { 3, null, 1, 1, 5, null };

        assertArrayEquals(new int[] { 2, 3 }, CommonUtil.indicesOfMin(a));
        assertArrayEquals(new int[] { 4 }, CommonUtil.indicesOfMax(a));

        final Integer[] allNull = { null, null };
        assertArrayEquals(new int[] { 0, 1 }, CommonUtil.indicesOfMin(allNull));
        assertArrayEquals(new int[] { 0, 1 }, CommonUtil.indicesOfMax(allNull));

        assertArrayEquals(new int[] { 2, 3 }, CommonUtil.indicesOfMin(Arrays.asList(a)));
        assertArrayEquals(new int[] { 4 }, CommonUtil.indicesOfMax(Arrays.asList(a)));

        assertArrayEquals(new int[0], CommonUtil.indicesOfMin((Integer[]) null));
        assertArrayEquals(new int[0], CommonUtil.indicesOfMax((Integer[]) null));
    }

    // ================================================================================================
    // top(..., keepEncounterOrder) - still works after the dead comparator branch was removed
    // ================================================================================================

    @Test
    public void top_keepEncounterOrderStillWorks() {
        final Integer[] a = { 5, 1, 9, 3, 7 };

        assertEquals(Arrays.asList(9, 7), N.top(a, 2, true));
        assertEquals(CommonUtil.asSet(9, 7), new HashSet<>(N.top(a, 2, false)));
        assertEquals(Arrays.asList(5, 9, 7), N.top(a, 3, true));

        assertEquals(Arrays.asList(9, 7), N.top(Arrays.asList(a), 2, true));
        assertEquals(Arrays.asList(1, 3), N.top(a, 2, Comparator.<Integer> reverseOrder(), true));
    }

    // ================================================================================================
    // cs.tolerance - the fuzzy index lookups name their argument
    // ================================================================================================

    @Test
    public void fuzzyIndexOf_namesTheToleranceArgument() {
        assertEquals("'tolerance' cannot be negative: -1.0",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.indexOf(new double[] { 1d }, 1d, 0, -1d)).getMessage());
        assertEquals("'tolerance' cannot be negative: -1.0",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.lastIndexOf(new double[] { 1d }, 1d, 0, -1d)).getMessage());
        assertEquals("'tolerance' cannot be negative: -1.0",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.indexOf(new float[] { 1f }, 1f, 0, -1f)).getMessage());
        assertEquals("'tolerance' cannot be negative: -1.0",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.lastIndexOf(new float[] { 1f }, 1f, 0, -1f)).getMessage());

        assertEquals(1, CommonUtil.indexOf(new double[] { 1d, 2d, 3d }, 2.05d, 0, 0.1d));
    }

    // ================================================================================================
    // J3 - the {} / %s placeholder rule the javadoc now states
    // ================================================================================================

    @Test
    public void checkArgument_placeholderStylesAreNotMixed() {
        assertEquals("x=1 y=2",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "x={} y={}", (Object) 1, (Object) 2)).getMessage());

        // No {} anywhere in the template: %s is used instead.
        assertEquals("x=1 y=2",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "x=%s y=%s", (Object) 1, (Object) 2)).getMessage());

        // Mixed: only {} is substituted, and the surplus argument is appended.
        assertEquals("x=1 y=%s: [2]",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "x={} y=%s", (Object) 1, (Object) 2)).getMessage());

        assertEquals("x=1 y=%s [2]",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "x={} y=%s", new Object[] { 1, 2 })).getMessage());
    }

    // ================================================================================================
    // SN 1220 - diagnostic suppliers are intentionally required only on the failure path
    // ================================================================================================

    @Test
    public void diagnosticMessageSuppliers_areOnlyRequiredOnFailure() {
        // Intentional exception to eager callback validation: successful checks do not need diagnostics.
        final java.util.function.Supplier<String> nullSupplier = null;
        assertDoesNotThrow(() -> CommonUtil.checkArgument(true, nullSupplier));
        assertDoesNotThrow(() -> CommonUtil.checkState(true, nullSupplier));
        final Object value = new Object();
        assertSame(value, CommonUtil.requireNonNull(value, nullSupplier));

        assertThrows(NullPointerException.class, () -> CommonUtil.checkArgument(false, nullSupplier));
        assertThrows(NullPointerException.class, () -> CommonUtil.checkState(false, nullSupplier));
        assertThrows(NullPointerException.class, () -> CommonUtil.requireNonNull(null, nullSupplier));

        // Non-null suppliers are also left untouched when the main condition succeeds.
        final int[] invocations = { 0 };
        CommonUtil.checkArgument(true, () -> {
            invocations[0]++;
            return "nope";
        });
        CommonUtil.checkState(true, () -> {
            invocations[0]++;
            return "nope";
        });
        assertSame(value, CommonUtil.requireNonNull(value, () -> {
            invocations[0]++;
            return "nope";
        }));
        assertEquals(0, invocations[0]);

        final java.util.function.Supplier<String> messageSupplier = () -> {
            invocations[0]++;
            return "diagnostic message";
        };
        assertEquals("diagnostic message", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, messageSupplier)).getMessage());
        assertEquals("diagnostic message", assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, messageSupplier)).getMessage());
        assertEquals("diagnostic message", assertThrows(NullPointerException.class, () -> CommonUtil.requireNonNull(null, messageSupplier)).getMessage());
        assertEquals(3, invocations[0]);
    }

    // ================================================================================================
    // D1 / J2 - the two NaN policies, now documented, are frozen here
    // ================================================================================================

    @Test
    public void primitiveMinMax_propagateNaN() {
        assertTrue(Double.isNaN(N.min(new double[] { 1d, Double.NaN })));
        assertTrue(Double.isNaN(N.max(new double[] { 1d, Double.NaN })));
        assertTrue(Float.isNaN(N.min(new float[] { 1f, Float.NaN })));
        assertTrue(Float.isNaN(N.max(new float[] { 1f, Float.NaN })));
    }

    @Test
    public void boxedMinMax_orderNaNAsTheLargestValue() {
        // Double.compareTo ranks NaN above everything, so it is never the minimum and always the maximum.
        assertEquals(Double.valueOf(1d), N.min(new Double[] { 1d, Double.NaN }));
        assertTrue(Double.isNaN(N.max(new Double[] { 1d, Double.NaN })));

        assertEquals(Float.valueOf(1f), N.min(new Float[] { 1f, Float.NaN }));
        assertTrue(Float.isNaN(N.max(new Float[] { 1f, Float.NaN })));

        assertEquals(Double.valueOf(1d), N.min(Arrays.asList(1d, Double.NaN)));
        assertTrue(Double.isNaN(N.max(Arrays.asList(1d, Double.NaN))));

        assertEquals(Double.valueOf(1d), N.min(Arrays.asList(1d, Double.NaN).iterator()));
        assertTrue(Double.isNaN(N.max(Arrays.asList(1d, Double.NaN).iterator())));

        assertEquals(Double.valueOf(Double.NaN), N.min(new Double[] { Double.NaN, Double.NaN }));

        // -0.0 sorts below 0.0 in that same total order.
        assertEquals(Double.valueOf(-0.0d), N.min(new Double[] { 0.0d, -0.0d }));
        assertEquals(Double.valueOf(0.0d), N.max(new Double[] { 0.0d, -0.0d }));
    }

    @Test
    public void minByMaxBy_orderTheExtractedKeyAsTheBoxedFamilyDoes() {
        final String[] a = { "one", "nan", "two" };
        final java.util.function.Function<String, Double> key = s -> "nan".equals(s) ? Double.NaN : (double) s.length();

        assertEquals("one", N.minBy(a, key::apply)); // a NaN key never wins the minimum
        assertEquals("nan", N.maxBy(a, key::apply)); // ... and always wins the maximum

        assertEquals("one", N.minBy(Arrays.asList(a), key::apply));
        assertEquals("nan", N.maxBy(Arrays.asList(a), key::apply));
        assertEquals("one", N.minBy(Arrays.asList(a).iterator(), key::apply));
        assertEquals("nan", N.maxBy(Arrays.asList(a).iterator(), key::apply));
    }

    @Test
    public void extractorMinMax_orderNaNAsTheLargestValue() {
        final Double[] a = { 1d, Double.NaN, 2d };

        assertEquals(1d, N.minDoubleOrDefaultIfEmpty(a, d -> d, -1d), 0d);
        assertTrue(Double.isNaN(N.maxDoubleOrDefaultIfEmpty(a, d -> d, -1d)));

        assertEquals(-1d, N.minDoubleOrDefaultIfEmpty(new Double[0], d -> d, -1d), 0d);
        assertEquals(-1d, N.maxDoubleOrDefaultIfEmpty(new Double[0], d -> d, -1d), 0d);
    }

    @Test
    public void boxedMinMax_nullPolicyIsUnchanged() {
        assertEquals(Integer.valueOf(1), N.min(new Integer[] { 5, null, 1 }));
        assertEquals(Integer.valueOf(5), N.max(new Integer[] { 5, null, 1 }));
        assertNull(N.min(new Integer[] { null, null }));
        assertNull(N.max(new Integer[] { null, null }));
        assertNotNull(N.min(new Integer[] { 1 }));
    }

    // ================================================================================================
    // J2 - boxedMinMax_orderNaNAsTheLargestValue above covers the T[] / Iterable / Iterator shapes; the
    // NaN paragraph was added to all 14 boxed methods, so the remaining four shapes are pinned here too.
    // ================================================================================================

    private static long bits(final Double d) {
        return Double.doubleToRawLongBits(d);
    }

    @Test
    public void boxedMinMax_orderNaNAndSignedZeroOnTheRemainingFourShapes() {
        final Double nan = Double.NaN;
        final Double one = 1d;
        final Double two = 2d;
        final Double posZero = 0d;
        final Double negZero = -0d;
        final Double[] withNaN = { 1d, Double.NaN, 2d };
        final List<Double> listWithNaN = Arrays.asList(withNaN);

        // min(T, T) / max(T, T) - boxed locals on purpose: two double literals bind to the primitive overload.
        assertEquals(one, N.min(nan, one));
        assertTrue(Double.isNaN(N.max(nan, one)));
        assertEquals(bits(-0d), bits(N.min(posZero, negZero)));
        assertEquals(bits(0d), bits(N.max(posZero, negZero)));

        // min(T, T, T) / max(T, T, T)
        assertEquals(one, N.min(nan, one, two));
        assertTrue(Double.isNaN(N.max(nan, one, two)));
        assertEquals(bits(-0d), bits(N.min(posZero, negZero, one)));

        // min(T[], from, to) / max(T[], from, to)
        assertEquals(one, N.min(withNaN, 0, withNaN.length));
        assertTrue(Double.isNaN(N.max(withNaN, 0, withNaN.length)));

        // min(Collection, from, to) / max(Collection, from, to)
        assertEquals(one, N.min(listWithNaN, 0, listWithNaN.size()));
        assertTrue(Double.isNaN(N.max(listWithNaN, 0, listWithNaN.size())));
    }

    // ================================================================================================
    // J3 - the remaining shapes of the 66 rewritten @param errorMessageTemplate sentences
    // ================================================================================================

    @Test
    public void errorMessageTemplate_handlesMoreThanTwoAndZeroPlaceholders() {
        // Three placeholders of each style: the "{} wins, %s only as a fallback" rule is decided once for the
        // whole template, not per placeholder.
        assertEquals("x=1 y=2 z=3",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "x={} y={} z={}", 1, 2, 3)).getMessage());
        assertEquals("x=1 y=2 z=3",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "x=%s y=%s z=%s", 1, 2, 3)).getMessage());

        // No placeholder at all: the arguments are appended in square brackets.
        assertEquals("no placeholder: [7]",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "no placeholder", (Object) 7)).getMessage());
        assertEquals("no placeholder: [7, 8]",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "no placeholder", (Object) 7, (Object) 8)).getMessage());
    }

    // ================================================================================================
    // removeAll(Collection, Iterable/Iterator/T...) - one deterministic membership rule
    // ================================================================================================

    @Test
    public void removeAll_withIdentitySetArgument_keepsItsMembership() {
        final String kept = new String("x");
        final List<String> c = new ArrayList<>(Collections.singletonList(kept));

        // An identity set holding a *different*, equal instance matches nothing. The old snapshot re-hashed the
        // argument into a HashSet, which matched by equals and removed the element.
        assertFalse(N.removeAll(c, identitySet(new String("x"))));
        assertEquals(Collections.singletonList("x"), c);

        // The same instance is still removed.
        assertTrue(N.removeAll(c, identitySet(kept)));
        assertTrue(c.isEmpty());
    }

    @Test
    public void removeAll_removesOnlyTheInstancesTheArgumentActuallyMatched() {
        final String first = new String("x");
        final String second = new String("x");
        final List<String> c = new ArrayList<>(Arrays.asList(first, second));

        assertTrue(N.removeAll(c, identitySet(first)));
        assertEquals(1, c.size());
        assertSame(second, c.get(0), "an equal-but-distinct sibling the identity set did not match must survive");
    }

    @Test
    public void removeAll_setReceiverOutcomeDoesNotDependOnTheArgumentSize() {
        // AbstractSet.removeAll switches between "iterate the receiver" and "iterate the argument" on
        // size() > c.size(), so a comparator-based receiver used to match one way for a one-element argument
        // and the other way for a ten-element one.
        final List<String> small = Arrays.asList("A");
        final List<String> large = new ArrayList<>(small);

        for (int i = 0; i < 9; i++) {
            large.add("unrelated" + i);
        }

        for (final List<String> arg : Arrays.asList(small, large)) {
            final TreeSet<String> receiver = caseInsensitive("a", "b", "c");
            assertTrue(N.removeAll(receiver, arg), "argument size " + arg.size());
            assertEquals(Arrays.asList("b", "c"), new ArrayList<>(receiver), "argument size " + arg.size());
        }
    }

    @Test
    public void removeAll_iteratorAndIterableOverloadsAgree() {
        final TreeSet<String> viaIterable = caseInsensitive("a", "b", "c");
        final TreeSet<String> viaIterator = caseInsensitive("a", "b", "c");
        final TreeSet<String> viaVarargs = caseInsensitive("a", "b", "c");

        assertTrue(N.removeAll(viaIterable, Arrays.asList("A")));
        assertTrue(N.removeAll(viaIterator, Arrays.asList("A").iterator()));
        assertTrue(N.removeAll(viaVarargs, "A"));

        assertEquals(Arrays.asList("b", "c"), new ArrayList<>(viaIterable));
        assertEquals(new ArrayList<>(viaIterable), new ArrayList<>(viaIterator));
        assertEquals(new ArrayList<>(viaIterable), new ArrayList<>(viaVarargs));
    }

    @Test
    public void removeAll_selfRemovalIsSafeForASetReceiverToo() {
        final Set<String> hash = new LinkedHashSet<>(Arrays.asList("a", "b", "c"));
        assertTrue(N.removeAll(hash, hash));
        assertTrue(hash.isEmpty());

        final Set<String> viaIterator = new LinkedHashSet<>(Arrays.asList("a", "b", "c"));
        assertTrue(N.removeAll(viaIterator, viaIterator.iterator()));
        assertTrue(viaIterator.isEmpty());

        // ... and a view backed by the receiver.
        final Map<String, Integer> map = new java.util.LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        final Set<String> keys = map.keySet();
        assertTrue(N.removeAll(keys, keys));
        assertTrue(map.isEmpty());
    }

    // ================================================================================================
    // concat(T[]...) - the result type comes from the varargs array, at every arity
    // ================================================================================================

    @Test
    public void concat_varargs_componentTypeIsTheVarargsArraysAtEveryArity() {
        final Object[][] one = { new String[] { "x" } };
        final Object[][] two = { new String[] { "x" }, new Object[0] };

        // The one-array fast path used to return aa[0].clone(), i.e. a String[], so storing a non-String threw
        // ArrayStoreException for an arity the two-array path handled.
        final Object[] fromOne = N.concat(one);
        assertSame(Object.class, fromOne.getClass().getComponentType());
        fromOne[0] = 1;

        assertSame(Object.class, N.concat(two).getClass().getComponentType());
        assertSame(Object.class, N.concat(new Object[0][]).getClass().getComponentType());

        final Object[][] withNull = { null };
        assertSame(Object.class, N.concat(withNull).getClass().getComponentType());
        assertEquals(0, N.concat(withNull).length);

        // A genuinely typed varargs array still yields that type, and the contents are unchanged.
        final String[][] typed = { { "x" }, { "y", "z" } };
        assertSame(String.class, N.concat(typed).getClass().getComponentType());
        assertArrayEquals(new String[] { "x", "y", "z" }, N.concat(typed));

        final String[][] typedSingle = { { "x" } };
        final String[] single = N.concat(typedSingle);
        assertSame(String.class, single.getClass().getComponentType());
        assertArrayEquals(new String[] { "x" }, single);
        assertNotSame(typedSingle[0], single, "the result must be a fresh array, not the sole input");

        assertThrows(IllegalArgumentException.class, () -> N.concat((String[][]) null));
    }
}
