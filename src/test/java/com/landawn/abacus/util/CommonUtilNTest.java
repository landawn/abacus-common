package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Covers the fixes applied on 2026-08-31 after the <i>third</i> full review pass over {@code CommonUtil}/{@code N}.
 *
 * <p>Each test below is an A/B guard: it is written so that it fails against the code as it stood before the pass.
 *
 * <ul>
 *   <li><b>B1</b> - {@code removeDuplicates(x[], from, to, false)} (and the {@code distinct(x[], from, to)} overloads
 *       that delegate to it) sized their {@code LinkedHashSet} from {@code a.length} instead of the requested range.</li>
 *   <li><b>B2</b> - the {@code T[]} array-combining family disagreed about whose component type the result gets when
 *       {@code a} is empty, so the returned runtime type depended on {@code a}'s length.</li>
 *   <li><b>B3</b> - {@code containsAll} and {@code containsAny}/{@code containsNone} use different membership
 *       oracles; the behaviour is now documented, and pinned here so doc and code cannot drift apart.</li>
 *   <li><b>B4</b> - {@code retainAll}'s O(n+m) fast path was gated on the <i>receiver</i> being a {@code HashSet},
 *       while the quadratic lookup happens on the <i>argument</i> side.</li>
 *   <li><b>B5</b> - {@code removeOccurrences} (behind {@code Dataset} intersection/difference) never returned its
 *       pooled {@code Object[]} buffers when the multi-key branch threw part-way through.</li>
 *   <li><b>B6</b> - the unmodifiable-class registry knew only the 1-2 element {@code List.of}/{@code Set.of}/
 *       {@code Map.of} shapes, so the arity most code uses got wrapped a second time.</li>
 *   <li><b>D2</b> - five dynamic {@code Class}-keyed static caches retained every class they saw.</li>
 *   <li><b>D5</b>, <b>J4</b>, <b>O1</b>, <b>O2</b> - see the individual tests.</li>
 * </ul>
 *
 * @see CommonUtilNReviewFixes20260831Test
 */
public class CommonUtilNTest extends TestBase {

    // ---------------------------------------------------------------------------------------------------------
    // B1: removeDuplicates / distinct must size their working set by the requested range, not the whole array.
    // ---------------------------------------------------------------------------------------------------------

    /** Allocation, not wall-clock: deterministic, and the baseline overshoots by five orders of magnitude. */
    private static long allocatedBy(final Runnable r) {
        final com.sun.management.ThreadMXBean tmx = (com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean();

        for (int i = 0; i < 5; i++) { // warm the call path so class-loading is not billed to the measurement
            r.run();
        }

        final long before = tmx.getCurrentThreadAllocatedBytes();
        r.run();
        return tmx.getCurrentThreadAllocatedBytes() - before;
    }

    @Test
    public void testB1_removeDuplicatesSubRangeDoesNotAllocateForTheWholeArray() {
        final int[] big = new int[2_000_000];

        for (int i = 0; i < big.length; i++) {
            big[i] = i;
        }

        // The baseline sized a LinkedHashSet from a.length, so this allocated a ~16MB table to hold 2 elements.
        final long allocated = allocatedBy(() -> N.removeDuplicates(big, 0, 2, false));

        assertTrue(allocated < 100_000, "removeDuplicates(int[2M], 0, 2, false) allocated " + allocated + " bytes");
    }

    @Test
    public void testB1_distinctSubRangeDoesNotAllocateForTheWholeArray() {
        final int[] big = new int[2_000_000];

        for (int i = 0; i < big.length; i++) {
            big[i] = i;
        }

        final long allocated = allocatedBy(() -> N.distinct(big, 0, 2));

        assertTrue(allocated < 100_000, "distinct(int[2M], 0, 2) allocated " + allocated + " bytes");
    }

    @Test
    public void testB1_removeDuplicatesStillCorrectForEveryPrimitiveAndString() {
        assertArrayEquals(new char[] { 'd', 'a', 'b' }, N.removeDuplicates(new char[] { 'x', 'd', 'a', 'd', 'b', 'a', 'x' }, 1, 6, false));
        assertArrayEquals(new byte[] { 3, 1, 2 }, N.removeDuplicates(new byte[] { 9, 3, 1, 3, 2, 1, 9 }, 1, 6, false));
        assertArrayEquals(new short[] { 3, 1, 2 }, N.removeDuplicates(new short[] { 9, 3, 1, 3, 2, 1, 9 }, 1, 6, false));
        assertArrayEquals(new int[] { 3, 1, 2 }, N.removeDuplicates(new int[] { 9, 3, 1, 3, 2, 1, 9 }, 1, 6, false));
        assertArrayEquals(new long[] { 3, 1, 2 }, N.removeDuplicates(new long[] { 9, 3, 1, 3, 2, 1, 9 }, 1, 6, false));
        assertArrayEquals(new float[] { 3, 1, 2 }, N.removeDuplicates(new float[] { 9, 3, 1, 3, 2, 1, 9 }, 1, 6, false));
        assertArrayEquals(new double[] { 3, 1, 2 }, N.removeDuplicates(new double[] { 9, 3, 1, 3, 2, 1, 9 }, 1, 6, false));
        assertArrayEquals(new String[] { "c", "a", "b" }, N.removeDuplicates(new String[] { "z", "c", "a", "c", "b", "a", "z" }, 1, 6, false));

        // sorted branch, whole-array and sub-range, must be unaffected
        assertArrayEquals(new int[] { 1, 2, 3 }, N.removeDuplicates(new int[] { 1, 1, 2, 3, 3 }, 0, 5, true));
        assertArrayEquals(new int[] { 1, 2 }, N.removeDuplicates(new int[] { 0, 1, 1, 2, 9 }, 1, 4, true));

        // degenerate ranges
        assertArrayEquals(new int[0], N.removeDuplicates(new int[] { 1, 2, 3 }, 1, 1, false));
        assertArrayEquals(new int[] { 2 }, N.removeDuplicates(new int[] { 1, 2, 3 }, 1, 2, false));
        assertArrayEquals(new int[0], N.removeDuplicates((int[]) null, 0, 0, false));

        // distinct(x[], from, to) delegates to the same engine
        assertArrayEquals(new int[] { 3, 1, 2 }, N.distinct(new int[] { 9, 3, 1, 3, 2, 1, 9 }, 1, 6));
        assertArrayEquals(new char[] { 'd', 'a', 'b' }, N.distinct(new char[] { 'x', 'd', 'a', 'd', 'b', 'a', 'x' }, 1, 6));
        assertArrayEquals(new boolean[] { true, false }, N.distinct(new boolean[] { false, true, true, false, true }, 1, 5));
    }

    // ---------------------------------------------------------------------------------------------------------
    // B2: one component-type policy for the T[] family - the result type never depends on a's length.
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testB2_resultComponentTypeIsAlwaysAsRegardlessOfItsLength() {
        final Object[] emptyStrings = new String[0];
        final Object[] oneString = new String[] { "x" };
        final Object[] objectsHoldingStrings = new Object[] { "y" };

        // Before the fix concat/replaceRange cloned the *other* array whenever `a` was empty, so the returned
        // runtime type flipped between String[] and Object[] purely on a.length.
        assertEquals(String[].class, N.concat(emptyStrings, objectsHoldingStrings).getClass());
        assertEquals(String[].class, N.concat(oneString, objectsHoldingStrings).getClass());
        assertEquals(String[].class, N.replaceRange(emptyStrings, 0, 0, objectsHoldingStrings).getClass());
        assertEquals(String[].class, N.replaceRange(oneString, 0, 1, objectsHoldingStrings).getClass());
        assertEquals(String[].class, N.addAll(emptyStrings, objectsHoldingStrings).getClass());
        assertEquals(String[].class, N.insertAll(emptyStrings, 0, objectsHoldingStrings).getClass());

        // ... and both empty arrays now agree with the rest of the family instead of taking b's type.
        assertEquals(String[].class, N.concat(emptyStrings, new Object[0]).getClass());
        assertEquals(Object[].class, N.concat(new Object[0], emptyStrings).getClass());

        assertArrayEquals(new Object[] { "y" }, N.concat(emptyStrings, objectsHoldingStrings));
        assertArrayEquals(new Object[] { "x", "y" }, N.concat(oneString, objectsHoldingStrings));
        assertArrayEquals(new Object[] { "y" }, N.replaceRange(emptyStrings, 0, 0, objectsHoldingStrings));
    }

    @Test
    public void testB2_nullAndEmptyContractsUnchanged() {
        assertArrayEquals(new String[] { "a" }, N.concat(new String[] { "a" }, (String[]) null));
        assertArrayEquals(new String[] { "b" }, N.concat((String[]) null, new String[] { "b" }));
        assertEquals(null, N.concat((String[]) null, (String[]) null));
        assertArrayEquals(new String[0], N.concat(new String[0], new String[0]));
        assertEquals(String[].class, N.concat((String[]) null, new String[] { "b" }).getClass());

        // concat must keep returning a copy, never an alias of either input
        final String[] a = { "a" };
        assertNotSame(a, N.concat(a, new String[0]));

        // replaceRange with an empty replacement still deletes the range; the empty/empty case now returns a copy
        final String[] empty = new String[0];
        assertArrayEquals(new String[0], N.replaceRange(empty, 0, 0, new String[0]));
        assertNotSame(empty, N.replaceRange(empty, 0, 0, new String[0]));
        assertArrayEquals(new String[] { "a", "c" }, N.replaceRange(new String[] { "a", "b", "c" }, 1, 2, new String[0]));

        // addAll/insertAll already took a's type; their null/empty contracts are untouched
        assertArrayEquals(new String[] { "a" }, N.addAll(new String[] { "a" }, (String[]) null));
        assertArrayEquals(new String[0], N.addAll(new String[0], (String[]) null));
        assertArrayEquals(new String[] { "a", "b" }, N.addAll(new String[] { "a" }, "b"));
        assertArrayEquals(new String[] { "b", "a" }, N.insertAll(new String[] { "a" }, 0, "b"));
        // NB: String[] has its own addAll/insert overloads that tolerate a null array; the generic T[] one
        // rejects it, so use a non-String element type to reach it.
        assertThrows(IllegalArgumentException.class, () -> N.addAll((Integer[]) null, 1));
    }

    @Test
    public void testB2_documentedArrayStoreExceptionContract() {
        final Object[] emptyStrings = new String[0];
        final Object[] oneString = new String[] { "x" };
        final Object[] incompatible = new Object[] { 1 };

        // a's component type decides, so an unassignable element of the second array is rejected - and now that the
        // empty-a shortcut is gone, an empty `a` behaves exactly like a non-empty one.
        assertThrows(ArrayStoreException.class, () -> N.concat(emptyStrings, incompatible));
        assertThrows(ArrayStoreException.class, () -> N.concat(oneString, incompatible));
        assertThrows(ArrayStoreException.class, () -> N.addAll(emptyStrings, incompatible));
        assertThrows(ArrayStoreException.class, () -> N.addAll(oneString, incompatible));
        assertThrows(ArrayStoreException.class, () -> N.insertAll(emptyStrings, 0, incompatible));
        assertThrows(ArrayStoreException.class, () -> N.insertAll(oneString, 1, incompatible));
        assertThrows(ArrayStoreException.class, () -> N.replaceRange(emptyStrings, 0, 0, incompatible));
        assertThrows(ArrayStoreException.class, () -> N.replaceRange(oneString, 0, 1, incompatible));

        // A null `a` has no component type to preserve, so b's is used and nothing is rejected.
        final Object[] fromNull = N.concat((Object[]) null, incompatible);
        assertEquals(Object[].class, fromNull.getClass());
        assertArrayEquals(new Object[] { 1 }, fromNull);

        // An Object[] that happens to hold assignable elements is still accepted (this is why a component-type
        // pre-check would be wrong).
        assertArrayEquals(new Object[] { "x", "y" }, N.concat(oneString, new Object[] { "y" }));
        assertArrayEquals(new Object[] { "x", "y" }, N.addAll(oneString, new Object[] { "y" }));

        // The varargs concat keeps its own (documented, unchanged) rule: the DECLARED component type of the
        // varargs array, never an element's runtime type - so it never raises ArrayStoreException here.
        final Object[] fromVarargs = N.concat(new Object[][] { new String[] { "a" }, new Object[] { 1 } });
        assertEquals(Object[].class, fromVarargs.getClass());
        assertArrayEquals(new Object[] { "a", 1 }, fromVarargs);
        assertEquals(String[].class, N.concat(new String[][] { { "a" }, { "b" } }).getClass());
    }

    // ---------------------------------------------------------------------------------------------------------
    // B3: the documented membership oracles - containsAll asks the receiver, containsAny/None let a Set decide.
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testB3_documentedMembershipOracles() {
        final TreeSet<String> caseInsensitive = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitive.add("A");
        caseInsensitive.add("B");

        final Collection<String> lowerCase = new ArrayList<>(Arrays.asList("a", "b", "c"));

        // containsAll delegates to Collection.containsAll -> the receiver's equals decides, so "A"/"B" are absent.
        assertFalse(N.containsAll(lowerCase, (Collection<String>) caseInsensitive));
        assertFalse(N.containsAll(lowerCase, caseInsensitive));

        // containsAny/containsNone go through disjoint(...), which asks the Set argument -> "A" matches "a".
        assertTrue(N.containsAny(lowerCase, (Collection<String>) caseInsensitive));
        assertTrue(N.containsAny(lowerCase, caseInsensitive));
        assertFalse(N.containsNone(lowerCase, (Collection<String>) caseInsensitive));

        // containsNone stays the exact complement of containsAny for every input shape.
        assertEquals(!N.containsAny(lowerCase, caseInsensitive), N.containsNone(lowerCase, caseInsensitive));

        // With ordinary equals-based collections the two families agree, as callers expect.
        final Set<String> plain = CommonUtil.asSet("a", "b");
        assertTrue(N.containsAll(lowerCase, plain));
        assertTrue(N.containsAny(lowerCase, plain));
        assertFalse(N.containsNone(lowerCase, plain));
    }

    @Test
    public void testB3_everyOverloadMatchesItsDocumentedOracle() {
        final TreeSet<String> ci = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        ci.add("A");
        ci.add("B");

        final Collection<String> src = new ArrayList<>(Arrays.asList("a", "b", "c"));
        final Iterable<String> asIterable = () -> new ArrayList<>(src).iterator();

        // containsAll: every overload defers to the receiver (or, for a bare Iterable/Iterator, to plain element
        // equality). None of them lets the argument Set's comparator decide -> "A"/"B" count as absent.
        assertFalse(N.containsAll(src, (Collection<String>) ci));
        assertFalse(N.containsAll(src, ci.toArray()));
        assertFalse(N.containsAll(src, ci));
        assertFalse(N.containsAll(asIterable, ci));
        assertFalse(N.containsAll(src.iterator(), ci));

        // containsAny: the argument Set's rule decides wherever it survives as a Set...
        assertTrue(N.containsAny(src, (Collection<String>) ci));
        assertTrue(N.containsAny(src, ci));
        assertTrue(N.containsAny(asIterable, ci));
        assertTrue(N.containsAny(src.iterator(), ci));
        // ...but a varargs array cannot carry it, so that overload falls back to element equality.
        assertFalse(N.containsAny(src, ci.toArray()));

        // containsNone is the exact complement of containsAny for every shape.
        assertEquals(!N.containsAny(src, (Collection<String>) ci), N.containsNone(src, (Collection<String>) ci));
        assertEquals(!N.containsAny(src, ci.toArray()), N.containsNone(src, ci.toArray()));
        assertEquals(!N.containsAny(src, ci), N.containsNone(src, ci));
        assertEquals(!N.containsAny(asIterable, ci), N.containsNone(asIterable, ci));
        assertEquals(!N.containsAny(src.iterator(), ci), N.containsNone(src.iterator(), ci));

        // With ordinary equals-based arguments every overload agrees, which is the case callers actually hit.
        final Set<String> plain = CommonUtil.asSet("a", "b");
        assertTrue(N.containsAll(src, plain));
        assertTrue(N.containsAll(asIterable, plain));
        assertTrue(N.containsAll(src.iterator(), plain));
        assertTrue(N.containsAny(src, plain));
        assertTrue(N.containsAny(asIterable, plain));
        assertTrue(N.containsAny(src.iterator(), plain));
        assertFalse(N.containsNone(src, plain));

        // disjoint's scan direction is size-driven but cannot change the answer: when neither side is a Set both
        // use plain element equality, so the two orderings must agree.
        final List<String> big = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            big.add("x" + i);
        }

        big.add("a");
        assertEquals(N.containsAny(src, big), N.containsAny(big, src));
    }

    // ---------------------------------------------------------------------------------------------------------
    // B4: retainAll must snapshot a non-Set argument regardless of the receiver's type.
    // ---------------------------------------------------------------------------------------------------------

    /** Counts {@code contains} calls, which is exactly what the O(n*m) path does once per receiver element. */
    private static final class ContainsCountingList<T> extends ArrayList<T> {
        private static final long serialVersionUID = 1L;

        private int containsCalls = 0;

        ContainsCountingList(final Collection<? extends T> c) {
            super(c);
        }

        @Override
        public boolean contains(final Object o) {
            containsCalls++;
            return super.contains(o);
        }
    }

    @Test
    public void testB4_retainAllSnapshotsListArgumentForAnyReceiverType() {
        final List<Integer> values = new ArrayList<>();

        for (int i = 0; i < 40; i++) {
            values.add(i);
        }

        // ArrayList receiver: the baseline skipped the snapshot entirely and called contains() 40 times.
        final ContainsCountingList<Integer> keepForList = new ContainsCountingList<>(values);
        final List<Integer> arrayListReceiver = new ArrayList<>(values);
        assertFalse(N.retainAll(arrayListReceiver, keepForList));
        assertEquals(0, keepForList.containsCalls, "argument should have been copied into a HashSet, not scanned");
        assertEquals(values, arrayListReceiver);

        // TreeSet receiver: same story.
        final ContainsCountingList<Integer> keepForTree = new ContainsCountingList<>(values);
        final Set<Integer> treeSetReceiver = new TreeSet<>(values);
        assertFalse(N.retainAll(treeSetReceiver, keepForTree));
        assertEquals(0, keepForTree.containsCalls);

        // HashSet receiver: unchanged, it already took the fast path.
        final ContainsCountingList<Integer> keepForHash = new ContainsCountingList<>(values);
        assertFalse(N.retainAll(CommonUtil.newHashSet(values), keepForHash));
        assertEquals(0, keepForHash.containsCalls);
    }

    @Test
    public void testB4_retainAllSemanticsUnchanged() {
        final List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        assertTrue(N.retainAll(list, Arrays.asList(2, 4, 6)));
        assertEquals(Arrays.asList(2, 4), list);

        // empty/null argument still means "keep nothing"
        final List<Integer> toClear = new ArrayList<>(Arrays.asList(1, 2));
        assertTrue(N.retainAll(toClear, Collections.<Integer> emptyList()));
        assertTrue(toClear.isEmpty());
        assertFalse(N.retainAll(new ArrayList<Integer>(), Arrays.asList(1)));

        // a Set argument is still passed through untouched, so its own membership rule decides
        final TreeSet<String> caseInsensitive = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitive.add("A");

        final List<String> subject = new ArrayList<>(Arrays.asList("a", "b"));
        assertTrue(N.retainAll(subject, caseInsensitive));
        assertEquals(Arrays.asList("a"), subject, "a Set argument must keep deciding membership itself");

        // small inputs (both sizes <= 9) still bypass the copy and behave identically
        final List<Integer> small = new ArrayList<>(Arrays.asList(1, 2, 3));
        assertTrue(N.retainAll(small, Arrays.asList(3)));
        assertEquals(Arrays.asList(3), small);

        // de-duplicating the argument into a HashSet must not de-duplicate the receiver
        final List<Integer> withDuplicates = new ArrayList<>(Arrays.asList(1, 1, 2, 1));
        assertTrue(N.retainAll(withDuplicates, Arrays.asList(1, 1, 1, 1, 1, 1, 1, 1, 1, 1)));
        assertEquals(Arrays.asList(1, 1, 1), withDuplicates);

        // the receiver may legitimately BE the argument; snapshotting it first also removes any CME risk
        final List<Integer> self = new ArrayList<>();

        for (int i = 0; i < 30; i++) {
            self.add(i);
        }

        final List<Integer> selfExpected = new ArrayList<>(self);
        assertFalse(N.retainAll(self, self));
        assertEquals(selfExpected, self);

        // null elements survive the snapshot
        final List<String> withNull = new ArrayList<>(Arrays.asList("a", null, "b"));
        assertTrue(N.retainAll(withNull, Arrays.asList(null, "a", "c", "d", "e", "f", "g", "h", "i", "j", "k")));
        assertEquals(Arrays.asList("a", null), withNull);
    }

    // ---------------------------------------------------------------------------------------------------------
    // B5: removeOccurrences must return every pooled Object[] it borrowed, including on the exception path.
    // ---------------------------------------------------------------------------------------------------------

    /** A key value whose {@code hashCode()} blows up inside {@code Multiset.add(Wrapper, 1)}. */
    private static final class ExplodingKey {
        @Override
        public int hashCode() {
            throw new IllegalStateException("boom");
        }

        @Override
        public boolean equals(final Object obj) {
            return this == obj;
        }
    }

    @SuppressWarnings("unchecked")
    private static int pooledObjectArrayCount(final int length) throws Exception {
        final Field poolField = Objectory.class.getDeclaredField("objectArrayPool");
        poolField.setAccessible(true);

        final Queue<Object[]>[] pool = (Queue<Object[]>[]) poolField.get(null);
        final Queue<Object[]> queue = pool[length];

        return queue == null ? 0 : queue.size();
    }

    /**
     * Drains Objectory's shared per-length array pool. Safe because this suite runs sequentially and the only
     * effect on other tests is that they allocate fresh arrays instead of pooled ones.
     */
    private static void drainObjectArrayPool(final int length) {
        // The per-length queue is capped well below this, so the pool is guaranteed empty afterwards.
        for (int i = 0; i < 256; i++) {
            Objectory.createObjectArray(length);
        }
    }

    @Test
    public void testB5_pooledRowBuffersAreReturnedWhenTheMultiKeyBranchThrows() throws Exception {
        final List<String> columnNames = Arrays.asList("k1", "k2");

        // NB: Arrays.asList(new Object[]{...}) would spread the array into the varargs slot; build the row list
        // explicitly so the Dataset sees one Object[] row rather than two scalar rows.
        final List<Object> rowsOfA = new ArrayList<>();
        rowsOfA.add(new Object[] { "a", 1 });

        final Dataset a = CommonUtil.newDataset(columnNames, rowsOfA);
        // Rows 0 and 1 land two buffers in the multiset; row 2 borrows a third and then explodes while being hashed.
        final List<Object> rowsOfB = new ArrayList<>();
        rowsOfB.add(new Object[] { "x", 1 });
        rowsOfB.add(new Object[] { "y", 2 });
        rowsOfB.add(new Object[] { new ExplodingKey(), 3 });

        final Dataset b = CommonUtil.newDataset(columnNames, rowsOfB);

        drainObjectArrayPool(2);
        assertEquals(0, pooledObjectArrayCount(2));

        assertThrows(IllegalStateException.class, () -> N.difference(a, b, columnNames));

        assertTrue(pooledObjectArrayCount(2) >= 3, "every borrowed Object[] should be back in the pool, found " + pooledObjectArrayCount(2));
    }

    @Test
    public void testB5_pooledRowBuffersAreReturnedWhenTheScanLoopThrows() throws Exception {
        final List<String> columnNames = Arrays.asList("k1", "k2");

        final List<Object> rowsOfA = new ArrayList<>();
        rowsOfA.add(new Object[] { "a", 1 });
        rowsOfA.add(new Object[] { new ExplodingKey(), 2 }); // blows up while the scan loop hashes it

        final List<Object> rowsOfB = new ArrayList<>();
        rowsOfB.add(new Object[] { "x", 1 });
        rowsOfB.add(new Object[] { "y", 2 });

        final Dataset a = CommonUtil.newDataset(columnNames, rowsOfA);
        final Dataset b = CommonUtil.newDataset(columnNames, rowsOfB);

        drainObjectArrayPool(2);
        assertEquals(0, pooledObjectArrayCount(2));

        assertThrows(IllegalStateException.class, () -> N.difference(a, b, columnNames));

        // two arrays are held by the multiset snapshot, one is the scan-loop scratch buffer
        assertTrue(pooledObjectArrayCount(2) >= 3, "every borrowed Object[] should be back in the pool, found " + pooledObjectArrayCount(2));
    }

    @Test
    public void testB5_pooledRowBufferIsReturnedWhenTheVeryFirstRowThrows() throws Exception {
        final List<String> columnNames = Arrays.asList("k1", "k2");

        final List<Object> rowsOfA = new ArrayList<>();
        rowsOfA.add(new Object[] { "a", 1 });

        final List<Object> rowsOfB = new ArrayList<>();
        rowsOfB.add(new Object[] { new ExplodingKey(), 1 });

        final Dataset a = CommonUtil.newDataset(columnNames, rowsOfA);
        final Dataset b = CommonUtil.newDataset(columnNames, rowsOfB);

        drainObjectArrayPool(2);
        assertEquals(0, pooledObjectArrayCount(2));

        assertThrows(IllegalStateException.class, () -> N.intersection(a, b, columnNames));

        // nothing had been handed to the multiset yet; only the scratch buffer was borrowed
        assertTrue(pooledObjectArrayCount(2) >= 1, "the scratch Object[] should be back in the pool, found " + pooledObjectArrayCount(2));
    }

    @Test
    public void testB5_multiKeyDatasetOperationsStillCorrect() {
        final List<String> columnNames = Arrays.asList("k1", "k2");
        final Dataset a = CommonUtil.newDataset(columnNames, Arrays.<Object> asList(new Object[] { "a", 1 }, new Object[] { "b", 2 }, new Object[] { "c", 3 }));
        final Dataset b = CommonUtil.newDataset(columnNames, Arrays.<Object> asList(new Object[] { "b", 2 }, new Object[] { "z", 9 }));

        final Dataset difference = N.difference(a, b, columnNames);
        assertEquals(2, difference.size());
        assertEquals(Arrays.asList("a", "c"), difference.getColumn("k1"));

        final Dataset intersection = N.intersection(a, b, columnNames);
        assertEquals(1, intersection.size());
        assertEquals(Arrays.asList("b"), intersection.getColumn("k1"));

        final Dataset symmetric = N.symmetricDifference(a, b, columnNames);
        assertEquals(3, symmetric.size());

        // single-key branch (no pooled buffers) must be untouched
        final Dataset singleKey = N.difference(a, b, Arrays.asList("k1"));
        assertEquals(Arrays.asList("a", "c"), singleKey.getColumn("k1"));

        // empty operands on either side of the multi-key branch
        final Dataset empty = CommonUtil.newEmptyDataset(columnNames);
        assertEquals(3, N.difference(a, empty, columnNames).size());
        assertEquals(0, N.intersection(a, empty, columnNames).size());
        assertEquals(0, N.difference(empty, a, columnNames).size());
        assertEquals(3, N.symmetricDifference(a, empty, columnNames).size());
    }

    // ---------------------------------------------------------------------------------------------------------
    // B6: the unmodifiable registry must recognise the N-element List.of/Set.of/Map.of shapes.
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testB6_jdkImmutablesOfEveryArityAreReturnedUnwrapped() {
        final List<String> list1 = List.of("a");
        final List<String> list3 = List.of("a", "b", "c");
        assertSame(list1, CommonUtil.unmodifiableList(list1));
        assertSame(list3, CommonUtil.unmodifiableList(list3), "ListN was not registered, so it got wrapped a second time");

        final Set<String> set1 = Set.of("a");
        final Set<String> set3 = Set.of("a", "b", "c");
        assertSame(set1, CommonUtil.unmodifiableSet(set1));
        assertSame(set3, CommonUtil.unmodifiableSet(set3));

        final Map<String, String> map1 = Map.of("k", "v");
        final Map<String, String> map2 = Map.of("k", "v", "k2", "v2");
        assertSame(map1, CommonUtil.unmodifiableMap(map1));
        assertSame(map2, CommonUtil.unmodifiableMap(map2));

        assertSame(Collections.emptySortedSet(), CommonUtil.unmodifiableSortedSet(Collections.<String> emptySortedSet()));
        assertSame(Collections.emptyNavigableSet(), CommonUtil.unmodifiableNavigableSet(Collections.<String> emptyNavigableSet()));
        assertSame(Collections.emptySortedMap(), CommonUtil.unmodifiableSortedMap(Collections.<String, String> emptySortedMap()));
        assertSame(Collections.emptyNavigableMap(), CommonUtil.unmodifiableNavigableMap(Collections.<String, String> emptyNavigableMap()));

        // a genuinely mutable collection is still wrapped, and the wrapper is still unmodifiable
        final List<String> mutable = new ArrayList<>(List.of("a", "b", "c"));
        final List<String> wrapped = CommonUtil.unmodifiableList(mutable);
        assertNotSame(mutable, wrapped);
        assertThrows(UnsupportedOperationException.class, () -> wrapped.add("d"));
    }

    @Test
    public void testB6_mutabilityOfRecognizesEveryFactoryArity() {
        assertEquals(Mutability.KNOWN_UNMODIFIABLE, CommonUtil.mutabilityOf(List.of("a", "b", "c")));
        assertEquals(Mutability.KNOWN_UNMODIFIABLE, CommonUtil.mutabilityOf(Set.of("a", "b", "c")));
        assertEquals(Mutability.KNOWN_UNMODIFIABLE, CommonUtil.mutabilityOf(Map.of("k", "v", "k2", "v2")));
        assertEquals(Mutability.KNOWN_MUTABLE, CommonUtil.mutabilityOf(new ArrayList<>(List.of("a"))));
        assertEquals(Mutability.KNOWN_MUTABLE, CommonUtil.mutabilityOf(CommonUtil.newHashMap(Map.of("k", "v"))));
    }

    // ---------------------------------------------------------------------------------------------------------
    // D2: the dynamic Class-keyed caches must not be Maps that retain every class they see.
    // ---------------------------------------------------------------------------------------------------------

    private enum Weekday {
        MON, TUE, WED
    }

    @Test
    public void testD2_dynamicClassKeyedCachesAreClassValues() throws Exception {
        for (final String fieldName : new String[] { "enumListPool", "enumSetPool", "enumMapPool", "descendingIteratorMethodPool" }) {
            final Field f = CommonUtil.class.getDeclaredField(fieldName);

            assertEquals(ClassValue.class, f.getType(), fieldName + " must be a ClassValue so a cached application class stays collectable");
        }
    }

    @Test
    public void testD2_enumPoolsStillCacheAndReturnTheSameViews() {
        assertEquals(Arrays.asList(Weekday.MON, Weekday.TUE, Weekday.WED), CommonUtil.enumListOf(Weekday.class));
        assertSame(CommonUtil.enumListOf(Weekday.class), CommonUtil.enumListOf(Weekday.class));

        assertEquals(CommonUtil.asSet(Weekday.MON, Weekday.TUE, Weekday.WED), CommonUtil.enumSetOf(Weekday.class));
        assertSame(CommonUtil.enumSetOf(Weekday.class), CommonUtil.enumSetOf(Weekday.class));
        assertTrue(CommonUtil.enumSetOf(Weekday.class).contains(Weekday.TUE));

        final ImmutableBiMap<Weekday, String> names = CommonUtil.enumNameMap(Weekday.class);
        assertEquals("TUE", names.get(Weekday.TUE));
        assertEquals(Weekday.WED, names.inverse().get("WED"));
        assertEquals(3, names.size());
        assertSame(names, CommonUtil.enumNameMap(Weekday.class));
        // the wrapped map is still EnumMap-backed, so keys stay in ordinal (declaration) order
        assertEquals(Arrays.asList(Weekday.MON, Weekday.TUE, Weekday.WED), new ArrayList<>(names.keySet()));
        assertEquals(Arrays.asList("MON", "TUE", "WED"), new ArrayList<>(names.values()));

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.enumListOf(null));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.enumSetOf(null));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.enumNameMap(null));
    }

    @Test
    public void testD2_descendingIteratorCachesStillWork() {
        // TreeSet has a public descendingIterator(); the reflective lookup is cached per class.
        final TreeSet<String> sorted = new TreeSet<>(Arrays.asList("a", "b", "c"));
        assertEquals("c", CommonUtil.lastElement(sorted).orElse(null));
        assertEquals("c", CommonUtil.lastElement(sorted).orElse(null));
        assertEquals(2, CommonUtil.lastIndexOf(sorted, "c"));

        // a class without descendingIterator() takes the negative-cache path
        final Set<String> hashSet = CommonUtil.newLinkedHashSet(Arrays.asList("a", "b", "c"));
        assertEquals("c", CommonUtil.lastElement(hashSet).orElse(null));
        assertEquals("c", CommonUtil.lastElement(hashSet).orElse(null));
    }

    /**
     * Constant-specific class bodies make every constant an anonymous subclass - the shape most likely to break
     * the raw {@code Class} casts that the {@code ClassValue} pools have to use.
     */
    private enum WithConstantBodies {
        A {
            @Override
            int weight() {
                return 1;
            }
        },
        B {
            @Override
            int weight() {
                return 2;
            }
        };

        abstract int weight();
    }

    private enum SingleConstant {
        ONLY
    }

    private enum NoConstants {
        // deliberately empty
    }

    @Test
    public void testD2_enumPoolsHandleAwkwardEnumShapes() {
        // constant-specific bodies: getEnumConstants() still returns the declaring type's constants
        assertEquals(Arrays.asList(WithConstantBodies.A, WithConstantBodies.B), CommonUtil.enumListOf(WithConstantBodies.class));
        assertEquals(2, CommonUtil.enumSetOf(WithConstantBodies.class).size());
        assertTrue(CommonUtil.enumSetOf(WithConstantBodies.class).contains(WithConstantBodies.B));
        assertEquals("B", CommonUtil.enumNameMap(WithConstantBodies.class).get(WithConstantBodies.B));
        assertEquals(WithConstantBodies.A, CommonUtil.enumNameMap(WithConstantBodies.class).inverse().get("A"));
        assertEquals(1, WithConstantBodies.A.weight());

        assertEquals(Arrays.asList(SingleConstant.ONLY), CommonUtil.enumListOf(SingleConstant.class));
        assertEquals(1, CommonUtil.enumSetOf(SingleConstant.class).size());

        // an enum with no constants must not blow up in EnumSet.allOf / new EnumMap(rawClass)
        assertTrue(CommonUtil.enumListOf(NoConstants.class).isEmpty());
        assertTrue(CommonUtil.enumSetOf(NoConstants.class).isEmpty());
        assertTrue(CommonUtil.enumNameMap(NoConstants.class).isEmpty());
    }

    // ---------------------------------------------------------------------------------------------------------
    // D1: the null-comparator branch that checkComparator used to provide is unreachable and is gone.
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testD1_binarySearchStillRejectsANullComparator() {
        final Integer[] a = { 1, 2, 3 };
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.binarySearch(a, 2, (Comparator<Integer>) null));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.binarySearch(a, 0, 3, 2, (Comparator<Integer>) null));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.binarySearch(Arrays.asList(a), 2, (Comparator<Integer>) null));

        assertEquals(1, CommonUtil.binarySearch(a, 2, Comparator.naturalOrder()));
        assertEquals(1, CommonUtil.binarySearch(a, 0, 3, 2, Comparator.naturalOrder()));
        assertEquals(1, CommonUtil.binarySearch(Arrays.asList(a), 2, Comparator.naturalOrder()));
    }

    // ---------------------------------------------------------------------------------------------------------
    // D5: indicesOfAll must guard its skip loop the way its indexOf sibling already does.
    // ---------------------------------------------------------------------------------------------------------

    /** A collection whose {@code size()} overstates what its iterator actually yields. */
    private static final class ShortIteratorCollection extends ArrayList<String> {
        private static final long serialVersionUID = 1L;

        private final int reportedSize;

        ShortIteratorCollection(final List<String> actual, final int reportedSize) {
            super(actual);
            this.reportedSize = reportedSize;
        }

        @Override
        public int size() {
            return reportedSize;
        }

        @Override
        public Iterator<String> iterator() {
            return new ArrayList<>(this.subList(0, super.size())).iterator();
        }
    }

    @Test
    public void testD5_indicesOfAllToleratesAShortIteratorLikeIndexOfDoes() {
        // Not a RandomAccess-List shortcut: the anonymous subclass keeps RandomAccess, so force the iterator path
        // by wrapping in a plain Collection view.
        final Collection<String> lying = new ShortIteratorCollectionView(new ShortIteratorCollection(Arrays.asList("a", "b"), 6));

        // indexOf already guarded its skip loop and returns "not found" rather than blowing up.
        assertEquals(CommonUtil.INDEX_NOT_FOUND, CommonUtil.indexOf(lying, "a", 4));
        assertArrayEquals(new int[0], CommonUtil.indicesOfAll(lying, "a", 4));
        assertArrayEquals(new int[0], CommonUtil.indicesOfAll(lying, (String s) -> true, 4));
    }

    /** Non-RandomAccess pass-through so the iterator branch of indicesOfAll is exercised. */
    private static final class ShortIteratorCollectionView extends java.util.AbstractCollection<String> {
        private final ShortIteratorCollection delegate;

        ShortIteratorCollectionView(final ShortIteratorCollection delegate) {
            this.delegate = delegate;
        }

        @Override
        public Iterator<String> iterator() {
            return delegate.iterator();
        }

        @Override
        public int size() {
            return delegate.size();
        }
    }

    @Test
    public void testD5_indicesOfAllStillCorrectForWellBehavedCollections() {
        final List<String> linked = new LinkedList<>(Arrays.asList("a", "b", "a", "c", "a"));
        assertArrayEquals(new int[] { 0, 2, 4 }, CommonUtil.indicesOfAll(linked, "a"));
        assertArrayEquals(new int[] { 2, 4 }, CommonUtil.indicesOfAll(linked, "a", 1));
        assertArrayEquals(new int[] { 2, 4 }, CommonUtil.indicesOfAll(linked, (String s) -> "a".equals(s), 1));
        assertArrayEquals(new int[] { 0, 2, 4 }, CommonUtil.indicesOfAll(linked, "a", -3));
        assertArrayEquals(new int[] { 0, 2, 4 }, CommonUtil.indicesOfAll(new ArrayList<>(linked), "a"));
    }

    // ---------------------------------------------------------------------------------------------------------
    // J4: min/max(Collection, from, to) must report the same failure as their comparator overloads.
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testJ4_minMaxRangeOverloadsShareOneEmptyMessage() {
        final List<String> empty = new ArrayList<>();

        final String minNatural = assertThrows(IllegalArgumentException.class, () -> N.min(empty, 0, 0)).getMessage();
        final String minCmp = assertThrows(IllegalArgumentException.class, () -> N.min(empty, 0, 0, Comparator.<String> naturalOrder())).getMessage();
        assertEquals(minCmp, minNatural);

        final String maxNatural = assertThrows(IllegalArgumentException.class, () -> N.max(empty, 0, 0)).getMessage();
        final String maxCmp = assertThrows(IllegalArgumentException.class, () -> N.max(empty, 0, 0, Comparator.<String> naturalOrder())).getMessage();
        assertEquals(maxCmp, maxNatural);

        // null and out-of-range behaviour is unchanged
        assertThrows(IllegalArgumentException.class, () -> N.min((List<String>) null, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> N.max((List<String>) null, 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.min(Arrays.asList("a"), 0, 5));

        // An out-of-range span is now reported as such by both overloads, matching the @throws contract; the
        // natural-order overload used to short-circuit to IllegalArgumentException before checking the range.
        assertThrows(IndexOutOfBoundsException.class, () -> N.min(empty, 0, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> N.min(empty, 0, 5, Comparator.<String> naturalOrder()));
        assertThrows(IndexOutOfBoundsException.class, () -> N.max(empty, 0, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> N.max(empty, 0, 5, Comparator.<String> naturalOrder()));
        assertEquals("a", N.min(Arrays.asList("b", "a", "c"), 0, 3));
        assertEquals("c", N.max(Arrays.asList("b", "a", "c"), 0, 3));
    }

    // ---------------------------------------------------------------------------------------------------------
    // O1 / O2
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testO1_moveRangeAgreesBetweenRandomAccessAndLinkedLists() {
        for (int from = 0; from < 5; from++) {
            for (int to = from; to <= 5; to++) {
                for (int newPos = 0; newPos <= 5 - (to - from); newPos++) {
                    final List<Integer> arrayList = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4));
                    final List<Integer> linkedList = new LinkedList<>(Arrays.asList(0, 1, 2, 3, 4));

                    final boolean movedA = N.moveRange(arrayList, from, to, newPos);
                    final boolean movedL = N.moveRange(linkedList, from, to, newPos);

                    assertEquals(movedL, movedA);
                    assertEquals(linkedList, arrayList, "moveRange(" + from + ", " + to + ", " + newPos + ")");
                }
            }
        }

        final List<Integer> list = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4));
        assertTrue(N.moveRange(list, 0, 2, 3));
        assertEquals(Arrays.asList(2, 3, 4, 0, 1), list);
        assertThrows(IndexOutOfBoundsException.class, () -> N.moveRange(list, 0, 2, 4));

        // fixed-size but RandomAccess and set()-capable: the new set(i, ..) path must write through
        final List<Integer> fixedSize = Arrays.asList(0, 1, 2, 3, 4);
        assertTrue(N.moveRange(fixedSize, 0, 2, 3));
        assertEquals(Arrays.asList(2, 3, 4, 0, 1), fixedSize);

        // an immutable RandomAccess list still refuses, exactly as the ListIterator path did
        assertThrows(UnsupportedOperationException.class, () -> N.moveRange(List.of(0, 1, 2), 0, 1, 2));
    }

    @Test
    public void testO2_removeAtSingleIndexValidatesLikeTheMultiIndexPath() {
        final List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));

        final IndexOutOfBoundsException single = assertThrows(IndexOutOfBoundsException.class, () -> N.removeAt(list, 5));
        assertTrue(single.getMessage().contains("index"), "single-index removeAt should use the library's own index check, was: " + single.getMessage());
        assertThrows(IndexOutOfBoundsException.class, () -> N.removeAt(list, -1));
        assertEquals(Arrays.asList("a", "b", "c"), list, "a failed removeAt must not mutate the list");

        assertTrue(N.removeAt(list, 1));
        assertEquals(Arrays.asList("a", "c"), list);

        final List<String> multi = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        assertTrue(N.removeAt(multi, 0, 2));
        assertEquals(Arrays.asList("b", "d"), multi);
        assertFalse(N.removeAt(multi, new int[0]));
    }

    // ---------------------------------------------------------------------------------------------------------
    // D3: the pruned empty-guard branches must not have changed any answer.
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testD3_primitiveConcatAndSymmetricDifferenceEdgeCases() {
        assertArrayEquals(new int[] { 1, 2 }, N.concat(new int[] { 1, 2 }, new int[0]));
        assertArrayEquals(new int[] { 1, 2 }, N.concat(new int[] { 1, 2 }, (int[]) null));
        assertArrayEquals(new int[] { 3 }, N.concat(new int[0], new int[] { 3 }));
        assertArrayEquals(new int[0], N.concat(new int[0], new int[0]));
        assertArrayEquals(new char[] { 'a' }, N.concat(new char[] { 'a' }, new char[0]));
        assertArrayEquals(new double[] { 1d }, N.concat(new double[] { 1d }, new double[0]));

        final int[] source = { 1, 2 };
        assertNotSame(source, N.concat(source, new int[0]));

        assertEquals(Arrays.asList(1, 2), N.symmetricDifference(Arrays.asList(1, 2), Collections.<Integer> emptyList()));
        assertEquals(Arrays.asList(3), N.symmetricDifference(Collections.<Integer> emptyList(), Arrays.asList(3)));
        assertEquals(Collections.emptyList(), N.symmetricDifference(Collections.<Integer> emptyList(), Collections.<Integer> emptyList()));
        assertEquals(Arrays.asList(1, 3), N.symmetricDifference(Arrays.asList(1, 2), Arrays.asList(2, 3)));
    }

    @Test
    public void testD3_symmetricDifferenceReturnsAFreshList() {
        final List<Integer> a = new ArrayList<>(Arrays.asList(1, 2));
        final List<Integer> result = N.symmetricDifference(a, Collections.<Integer> emptyList());

        assertNotSame(a, result);
        result.add(99);
        assertEquals(Arrays.asList(1, 2), a);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Guard against NoSuchElementException regressions on the skip loops touched above.
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testSkipLoopsDoNotThrowNoSuchElement() {
        final Collection<String> lying = new ShortIteratorCollectionView(new ShortIteratorCollection(Arrays.asList("a"), 4));

        try {
            CommonUtil.indicesOfAll(lying, "a", 3);
            CommonUtil.indicesOfAll(lying, (String s) -> true, 3);
        } catch (final NoSuchElementException e) {
            org.junit.jupiter.api.Assertions.fail("skip loop must be guarded by hasNext(): " + e);
        }
    }
}
