package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.text.SimpleDateFormat;
import java.util.AbstractList;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;

/**
 * Tests for the CommonUtil fixes of the 2026-09-24 review (ledger C-101 .. C-185). Every method name carries its ledger ID.
 */
public class CommonUtilReview20260924Test extends TestBase {

    // ============================================================ C-101: map views are rejected before clear()

    private static Map<String, Integer> abc() {
        final Map<String, Integer> m = new LinkedHashMap<>();
        m.put("a", 1);
        m.put("b", 2);
        m.put("c", 3);
        return m;
    }

    @Test
    public void testC101_reverseRotateShuffleOnMapViewsKeepBackingMap() {
        final List<java.util.function.Consumer<Collection<?>>> ops = Arrays.asList(CommonUtil::reverse, c -> CommonUtil.rotate(c, 1),
                CommonUtil::shuffle, c -> CommonUtil.shuffle(c, new Random(7)));

        for (final java.util.function.Consumer<Collection<?>> op : ops) {
            final Map<String, Integer> m1 = abc();
            assertThrows(UnsupportedOperationException.class, () -> op.accept(m1.keySet()));
            assertEquals(abc(), m1);

            final Map<String, Integer> m2 = abc();
            assertThrows(UnsupportedOperationException.class, () -> op.accept(m2.values()));
            assertEquals(abc(), m2);

            final Map<String, Integer> m3 = abc();
            assertThrows(UnsupportedOperationException.class, () -> op.accept(m3.entrySet()));
            assertEquals(abc(), m3);

            final Map<String, Integer> m4 = new HashMap<>(abc());
            assertThrows(UnsupportedOperationException.class, () -> op.accept(m4.values()));
            assertEquals(abc(), m4);

            final Map<String, Integer> m5 = abc();
            assertThrows(UnsupportedOperationException.class, () -> op.accept(Collections.synchronizedSet(m5.keySet())));
            assertEquals(abc(), m5);
        }
    }

    @Test
    public void testC101_unmodifiableSetStillThrowsAndStaysIntact() {
        final Set<String> set = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList("a", "b", "c")));
        assertThrows(UnsupportedOperationException.class, () -> CommonUtil.reverse(set));
        assertEquals(Arrays.asList("a", "b", "c"), new ArrayList<>(set));
    }

    @Test
    public void testC101_supportedCollectionsStillReverse() {
        // a full bounded queue: the probe add() fails with IllegalStateException, which is ignored
        final ArrayBlockingQueue<String> q = new ArrayBlockingQueue<>(3);
        q.addAll(Arrays.asList("a", "b", "c"));
        CommonUtil.reverse(q);
        assertEquals(Arrays.asList("c", "b", "a"), new ArrayList<>(q));

        final Set<String> withNull = new LinkedHashSet<>(Arrays.asList("x", null));
        CommonUtil.reverse(withNull);
        assertEquals(Arrays.asList(null, "x"), new ArrayList<>(withNull));

        final Set<String> synced = Collections.synchronizedSet(new LinkedHashSet<>(Arrays.asList("1", "2", "3")));
        CommonUtil.rotate(synced, 1);
        assertEquals(Arrays.asList("3", "1", "2"), new ArrayList<>(synced));

        final java.util.ArrayDeque<Integer> deque = new java.util.ArrayDeque<>(Arrays.asList(1, 2, 3));
        CommonUtil.reverse(deque);
        assertEquals(Arrays.asList(3, 2, 1), new ArrayList<>(deque));
    }

    // ============================================================ C-102 / C-133 / C-152 / C-106: typed descending paths

    @Test
    public void testC102_descendingIteratorForNonPublicJdkNavigableSets() {
        final TreeMap<String, Integer> tm = new TreeMap<>(abc());

        assertEquals(Arrays.asList("c", "b", "a"), toList(CommonUtil.getDescendingIteratorIfPossible(tm.keySet())));
        assertEquals(Arrays.asList("c", "b", "a"), toList(CommonUtil.getDescendingIteratorIfPossible(tm.navigableKeySet())));
        assertEquals(Arrays.asList("c", "b", "a"),
                toList(CommonUtil.getDescendingIteratorIfPossible(Collections.unmodifiableNavigableSet(new TreeSet<>(tm.keySet())))));
        assertEquals(Arrays.asList("c", "b", "a"),
                toList(CommonUtil.getDescendingIteratorIfPossible(Collections.synchronizedNavigableSet(new TreeSet<>(tm.keySet())))));
        assertEquals(Arrays.asList("c", "b", "a"),
                toList(CommonUtil.getDescendingIteratorIfPossible(Collections.unmodifiableSortedSet(new TreeSet<>(tm.keySet())))));
    }

    @Test
    public void testC133_sequencedCollectionsUseReversedView() {
        assertEquals(Arrays.asList(3, 2, 1), toList(CommonUtil.getDescendingIteratorIfPossible(new LinkedHashSet<>(Arrays.asList(1, 2, 3)))));
        final Map<String, Integer> lhm = abc();
        assertEquals(Arrays.asList("c", "b", "a"), toList(CommonUtil.getDescendingIteratorIfPossible(lhm.keySet())));
        assertEquals(Arrays.asList(3, 2, 1), toList(CommonUtil.getDescendingIteratorIfPossible(lhm.values())));
        // pinned: a List that is not a Deque still yields null (callers take their List paths)
        assertNull(CommonUtil.getDescendingIteratorIfPossible(new ArrayList<>(Arrays.asList("a"))));
        assertNull(CommonUtil.getDescendingIteratorIfPossible(Collections.unmodifiableList(new LinkedList<>(Arrays.asList("a")))));
        // Deque path unchanged
        assertEquals(Arrays.asList(2, 1), toList(CommonUtil.getDescendingIteratorIfPossible(new LinkedList<>(Arrays.asList(1, 2)))));
    }

    /**
     * A class with a public descendingIterator(). (A non-public class in ANOTHER package - where the reflective invoke really fails
     * with IllegalAccessException - cannot be declared here, so the test sets the negative-cache flag that such a failure records.
     * The real failure path is exercised by CommonUtilReview20260925Test#testU0602_inaccessibleDescendingIteratorIsNegativeCachedForReal
     * through the com.landawn.abacus.testfixture.CommonUtilReviewFixtures fixture - U06-02, 2026-09-25.)
     */
    private static final class HiddenDescending<T> implements Iterable<T> {
        final List<T> data;
        int descendingCalls = 0;

        HiddenDescending(final List<T> data) {
            this.data = data;
        }

        @Override
        public Iterator<T> iterator() {
            return data.iterator();
        }

        @SuppressWarnings("unused")
        public Iterator<T> descendingIterator() {
            descendingCalls++;
            final List<T> copy = new ArrayList<>(data);
            Collections.reverse(copy);
            return copy.iterator();
        }
    }

    @Test
    public void testC102_inaccessibleReflectiveMethodIsNegativeCached() throws Exception {
        final HiddenDescending<String> it = new HiddenDescending<>(Arrays.asList("a", "b", "c"));

        // reachable: the reflective path is used and cached
        assertEquals(Arrays.asList("c", "b", "a"), toList(CommonUtil.getDescendingIteratorIfPossible(it)));
        assertEquals(1, it.descendingCalls);

        // an IllegalAccessException on invoke sets this flag; once set, the method is never invoked again
        final java.lang.reflect.Field poolField = CommonUtil.class.getDeclaredField("descendingIteratorMethodPool");
        poolField.setAccessible(true);
        final Object holder = ((ClassValue<?>) poolField.get(null)).get(HiddenDescending.class);
        final java.lang.reflect.Field flag = holder.getClass().getDeclaredField("inaccessible");
        flag.setAccessible(true);
        flag.setBoolean(holder, true);

        try {
            for (int i = 0; i < 3; i++) {
                assertNull(CommonUtil.getDescendingIteratorIfPossible(it));
                assertEquals("c", CommonUtil.lastElement(it).orElse(null));
            }

            assertEquals(1, it.descendingCalls);
        } finally {
            flag.setBoolean(holder, false);
        }
    }

    @Test
    public void testC152_lastElementsAndLastElementOnSortedViews() {
        final TreeMap<Integer, String> tm = new TreeMap<>();

        for (int i = 0; i < 10; i++) {
            tm.put(i, "v" + i);
        }

        assertEquals(Arrays.asList(7, 8, 9), CommonUtil.lastElements(tm.keySet(), 3));
        assertEquals(9, CommonUtil.lastElement(tm.keySet()).orElse(null));
        assertEquals(Arrays.asList(8, 9), CommonUtil.lastElements(new LinkedHashSet<>(tm.keySet()), 2));
        assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), CommonUtil.lastElements(tm.keySet(), 10));
        assertEquals(Collections.emptyList(), CommonUtil.lastElements(tm.keySet(), 0));
        assertEquals("v9", CommonUtil.lastOrNullIfEmpty(tm.values()));
        assertEquals(9, CommonUtil.findLast(tm.keySet(), x -> x > 5).orElse(null));
        assertEquals(8, CommonUtil.lastIndexOf(tm.keySet(), 8));
    }

    private static <T> List<T> toList(final Iterator<T> iter) {
        assertNotNull(iter);
        final List<T> list = new ArrayList<>();
        iter.forEachRemaining(list::add);
        return list;
    }

    // ============================================================ C-103: a SortedSet is left unchanged quietly

    @Test
    public void testC103_sortedSetsAreQuietNoOps() {
        final TreeSet<Integer> ts = new TreeSet<>(Arrays.asList(1, 2, 3));
        final List<NavigableSet<Integer>> sets = Arrays.asList(ts, ts.descendingSet(), ts.headSet(3, true),
                new ConcurrentSkipListSet<>(Arrays.asList(1, 2, 3)));

        for (final NavigableSet<Integer> s : sets) {
            final List<Integer> before = new ArrayList<>(s);
            CommonUtil.reverse(s);
            CommonUtil.rotate(s, 1);
            CommonUtil.shuffle(s);
            CommonUtil.shuffle(s, new Random(3));
            assertEquals(before, new ArrayList<>(s));
        }

        final SortedSet<Integer> unmodifiable = Collections.unmodifiableSortedSet(new TreeSet<>(Arrays.asList(1, 2, 3)));
        assertDoesNotThrow(() -> CommonUtil.reverse(unmodifiable));
        assertDoesNotThrow(() -> CommonUtil.rotate(unmodifiable, 1));
        assertDoesNotThrow(() -> CommonUtil.shuffle(unmodifiable));
        assertEquals(Arrays.asList(1, 2, 3), new ArrayList<>(unmodifiable));

        // validation first
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.shuffle(ts, null));
    }

    @Test
    public void testC103_priorityQueueStillRoundTrips() {
        final PriorityQueue<Integer> pq = new PriorityQueue<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7));
        CommonUtil.reverse(pq);
        assertEquals(1, pq.peek());
        assertEquals(7, pq.size());
    }

    // ============================================================ C-104

    @Test
    public void testC104_lastNonNullReadsTheElementOnce() {
        final AtomicInteger gets = new AtomicInteger();
        final List<String> computing = new CountingList(gets, "a", null, "c");

        assertEquals("c", CommonUtil.lastNonNull(computing).orElse(null));
        assertEquals(1, gets.get());

        gets.set(0);
        assertEquals("a", CommonUtil.lastNonNull(new CountingList(gets, "a", null, null)).orElse(null));
        assertEquals(3, gets.get());
        assertFalse(CommonUtil.lastNonNull(new CountingList(gets, null, null)).isPresent());
        assertFalse(CommonUtil.lastNonNull(new CountingList(gets)).isPresent());
    }

    private static final class CountingList extends AbstractList<String> implements RandomAccess {
        private final AtomicInteger gets;
        private final String[] data;

        CountingList(final AtomicInteger gets, final String... data) {
            this.gets = gets;
            this.data = data;
        }

        @Override
        public String get(final int index) {
            gets.incrementAndGet();
            return data[index];
        }

        @Override
        public int size() {
            return data.length;
        }
    }

    // ============================================================ C-107 / C-110 / C-117 / C-123 / C-115: stepped copyOfRange

    @Test
    public void testC117_reverseIdiomOnEmptyInputReturnsEmpty() {
        assertEquals(0, CommonUtil.copyOfRange(new boolean[0], -1, -1, -1).length);
        assertEquals(0, CommonUtil.copyOfRange(new char[0], -1, -1, -1).length);
        assertEquals(0, CommonUtil.copyOfRange(new byte[0], -1, -1, -1).length);
        assertEquals(0, CommonUtil.copyOfRange(new short[0], -1, -1, -1).length);
        assertEquals(0, CommonUtil.copyOfRange(new int[0], -1, -1, -1).length);
        assertEquals(0, CommonUtil.copyOfRange(new long[0], -1, -1, -1).length);
        assertEquals(0, CommonUtil.copyOfRange(new float[0], -1, -1, -1).length);
        assertEquals(0, CommonUtil.copyOfRange(new double[0], -1, -1, -2).length);
        assertEquals(0, CommonUtil.copyOfRange(new String[0], -1, -1, -1).length);
        final String[] typed = CommonUtil.copyOfRange(new Object[0], -1, -1, -1, String[].class);
        assertEquals(0, typed.length);
        assertEquals(String.class, typed.getClass().getComponentType());
        assertEquals(new ArrayList<>(), CommonUtil.copyOfRange(new ArrayList<String>(), -1, -1, -1));
        assertEquals(new ArrayList<>(), CommonUtil.copyOfRange(new LinkedList<String>(), -1, -1, -1));
        assertEquals("", CommonUtil.copyOfRange("", -1, -1, -1));
        assertEquals("", CommonUtil.copyOfRange((String) null, -1, -1, -1));

        // the generic reverse idiom now works for any length
        for (int n = 0; n < 4; n++) {
            final int[] a = new int[n];

            for (int i = 0; i < n; i++) {
                a[i] = i;
            }

            final int[] r = CommonUtil.copyOfRange(a, a.length - 1, -1, -1);
            assertEquals(n, r.length);

            for (int i = 0; i < n; i++) {
                assertEquals(n - 1 - i, r[i]);
            }
        }
    }

    @Test
    public void testC117_sentinelPairOnNonEmptyInput() {
        final int[] a = { 1, 2, 3 };
        assertEquals(0, CommonUtil.copyOfRange(a, -1, -1, -1).length);
        assertEquals(0, CommonUtil.copyOfRange(a, -1, -1, 1).length);
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.copyOfRange(a, -1, -1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.copyOfRange(a, -2, -1, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.copyOfRange(a, -1, 2, 1));
        assertArrayEquals(new int[] { 1 }, CommonUtil.copyOfRange(new int[] { 1 }, 0, -1, -1));
        assertArrayEquals(new int[] { 3, 1 }, CommonUtil.copyOfRange(a, 2, -1, -2));
        // pinned behaviour kept: fromIndex == length with a negative step
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.copyOfRange(a, 3, -1, -1));
        assertEquals(Arrays.asList("c", "b", "a"), CommonUtil.copyOfRange(Arrays.asList("a", "b", "c"), 2, -1, -1));
        assertEquals(0, CommonUtil.copyOfRange(Arrays.asList("a", "b", "c"), -1, -1, -3).size());
    }

    @Test
    public void testC110_directionMismatchIsEmptyAsDocumented() {
        assertEquals(0, CommonUtil.copyOfRange(new float[] { 1, 2, 3 }, 0, 3, -1).length);
        assertArrayEquals(new float[] { 3, 2, 1 }, CommonUtil.copyOfRange(new float[] { 1, 2, 3 }, 2, -1, -1));
        assertEquals(0, CommonUtil.copyOfRange(new double[] { 1, 2, 3 }, 2, 0, 1).length);
        assertEquals(new ArrayList<>(), CommonUtil.copyOfRange(Arrays.asList("a", "b", "c"), 0, 3, -1));
        assertEquals("", CommonUtil.copyOfRange("abc", 0, 3, -1));
        assertEquals("cba", CommonUtil.copyOfRange("abc", 2, -1, -1));
        // javadoc examples
        assertEquals("edcba", CommonUtil.copyOfRange("abcde", 4, -1, -1));
        assertEquals(Arrays.asList("d", "c", "b", "a"), CommonUtil.copyOfRange(Arrays.asList("a", "b", "c", "d"), 3, -1, -1));
        assertArrayEquals(new String[] { "d", "b" }, CommonUtil.copyOfRange(new Object[] { "a", "b", "c", "d" }, 3, -1, -2, String[].class));
        assertEquals(0, CommonUtil.copyOfRange(new Object[] { "a", "b", "c", "d" }, 0, 4, -1, String[].class).length);
        assertArrayEquals(new boolean[] { false, true }, CommonUtil.copyOfRange(new boolean[] { true, false }, 1, -1, -1));
    }

    @Test
    public void testC107_nullStringBehavesLikeEmptyString() {
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.copyOfRange((String) null, 0, -1, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.copyOfRange("", 0, -1, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.copyOfRange((String) null, 0, -1, -2));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.copyOfRange("", 0, -1, -2));

        for (final String s : new String[] { null, "" }) {
            assertEquals("", CommonUtil.copyOfRange(s, 0, 0, -1));
            assertEquals("", CommonUtil.copyOfRange(s, 0, 0, 2));
            assertEquals("", CommonUtil.copyOfRange(s, -1, -1, -1));
            assertThrows(IllegalArgumentException.class, () -> CommonUtil.copyOfRange(s, 0, 0, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.copyOfRange(s, 0, 1, 1));
        }

        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.copyOfRange("abc", 3, -1, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.copyOfRange("abc", 3, 0, -2));
    }

    @Test
    public void testC115_stringSteppingWorksOnCodeUnits() {
        final String s = "a😀b"; // a, emoji (surrogate pair), b
        assertEquals("b\uDE00\uD83Da", CommonUtil.copyOfRange(s, s.length() - 1, -1, -1));
        assertEquals("éè", CommonUtil.copyOfRange("èé", 1, -1, -1));
    }

    // ============================================================ C-108 / C-132 / C-113: List sorting

    /** A copy-on-write list that counts element-wise set() calls. */
    private static final class CountingCow<T> extends CopyOnWriteArrayList<T> {
        private static final long serialVersionUID = 1L;
        int sets = 0;

        CountingCow(final Collection<T> c) {
            super(c);
        }

        @Override
        public T set(final int index, final T element) {
            sets++;
            return super.set(index, element);
        }
    }

    private static List<Integer> shuffled(final int n, final long seed) {
        final List<Integer> list = new ArrayList<>(n);

        for (int i = 0; i < n; i++) {
            list.add(i);
        }

        Collections.shuffle(list, new Random(seed));
        return list;
    }

    @Test
    public void testC108_copyOnWriteRangeSortIsNotElementWise() {
        final List<Integer> data = shuffled(5000, 1);

        final CountingCow<Integer> partial = new CountingCow<>(data);
        CommonUtil.sort(partial, 1, partial.size(), Comparator.naturalOrder());
        final List<Integer> expected = new ArrayList<>(data);
        expected.subList(1, expected.size()).sort(null);
        assertEquals(expected, new ArrayList<>(partial));
        assertEquals(0, partial.sets);

        final CountingCow<Integer> full = new CountingCow<>(data);
        CommonUtil.parallelSort(full, Comparator.naturalOrder());
        final List<Integer> sorted = new ArrayList<>(data);
        sorted.sort(null);
        assertEquals(sorted, new ArrayList<>(full));
        assertEquals(0, full.sets);

        final CountingCow<Integer> parallelPartial = new CountingCow<>(data);
        CommonUtil.parallelSort(parallelPartial, 10, 4000, Comparator.naturalOrder());
        final List<Integer> expected2 = new ArrayList<>(data);
        expected2.subList(10, 4000).sort(null);
        assertEquals(expected2, new ArrayList<>(parallelPartial));
        assertEquals(0, parallelPartial.sets);
    }

    @Test
    public void testC108_rangeSortAcrossListKinds() {
        final List<Integer> data = shuffled(3000, 2);
        final List<List<Integer>> kinds = Arrays.asList(new ArrayList<>(data), new LinkedList<>(data), new Vector<>(data),
                Arrays.asList(data.toArray(new Integer[0])), Collections.synchronizedList(new ArrayList<>(data)), new CopyOnWriteArrayList<>(data),
                new ArrayList<>(data).subList(0, data.size()));

        for (final int[] range : new int[][] { { 0, 3000 }, { 5, 2995 }, { 0, 1 }, { 7, 7 }, { 2999, 3000 }, { 100, 2600 } }) {
            final List<Integer> expected = new ArrayList<>(data);
            expected.subList(range[0], range[1]).sort(null);

            for (final List<Integer> kind : kinds) {
                final List<Integer> a = new ArrayList<>(kind); // fresh copies per range for each kind
                final List<Integer> s1 = copyOfKind(kind, data);
                CommonUtil.sort(s1, range[0], range[1], Comparator.naturalOrder());
                assertEquals(expected, new ArrayList<>(s1), kind.getClass() + " " + Arrays.toString(range));

                final List<Integer> s2 = copyOfKind(kind, data);
                CommonUtil.parallelSort(s2, range[0], range[1], Comparator.naturalOrder());
                assertEquals(expected, new ArrayList<>(s2), kind.getClass() + " " + Arrays.toString(range));
                assertEquals(data.size(), a.size());
            }
        }
    }

    private static List<Integer> copyOfKind(final List<Integer> kind, final List<Integer> data) {
        if (kind instanceof LinkedList) {
            return new LinkedList<>(data);
        } else if (kind instanceof Vector) {
            return new Vector<>(data);
        } else if (kind instanceof CopyOnWriteArrayList) {
            return new CopyOnWriteArrayList<>(data);
        } else if (kind.getClass().getName().contains("Synchronized")) {
            return Collections.synchronizedList(new ArrayList<>(data));
        } else if (kind.getClass().getName().equals("java.util.Arrays$ArrayList")) {
            return Arrays.asList(data.toArray(new Integer[0]));
        } else if (kind.getClass().getName().contains("SubList")) {
            return new ArrayList<>(data).subList(0, data.size());
        }

        return new ArrayList<>(data);
    }

    @Test
    public void testC108_sortIsStableAndRejectsUnmodifiable() {
        final List<String> words = new CopyOnWriteArrayList<>(Arrays.asList("bb", "a2", "cc", "a1", "dd", "b1"));
        CommonUtil.sort(words, 1, 6, Comparator.comparing(s -> s.charAt(0)));
        assertEquals(Arrays.asList("bb", "a2", "a1", "b1", "cc", "dd"), words);

        final List<String> ll = new LinkedList<>(Arrays.asList("z", "b2", "a", "b1", "c"));
        CommonUtil.parallelSort(ll, 1, 5, Comparator.comparing(s -> s.charAt(0)));
        assertEquals(Arrays.asList("z", "a", "b2", "b1", "c"), ll);

        assertThrows(UnsupportedOperationException.class, () -> CommonUtil.sort(Collections.unmodifiableList(Arrays.asList(1, 2, 3)), 0, 2));
        assertThrows(UnsupportedOperationException.class,
                () -> CommonUtil.parallelSort(Collections.unmodifiableList(Arrays.asList(3, 2, 1)), 1, 3, Comparator.<Integer> naturalOrder()));
        assertThrows(UnsupportedOperationException.class, () -> CommonUtil.sort(List.of(1, 2), 0, 2));
        assertThrows(UnsupportedOperationException.class, () -> CommonUtil.sort(List.of(2, 1)));

        final List<Integer> throwing = new CopyOnWriteArrayList<>(Arrays.asList(3, 1, 2));
        assertThrows(IllegalStateException.class, () -> CommonUtil.sort(throwing, 1, 3, (x, y) -> {
            throw new IllegalStateException();
        }));
        assertEquals(Arrays.asList(3, 1, 2), throwing);
    }

    @Test
    public void testC132_singleElementUnmodifiableListsAreNoOps() {
        assertDoesNotThrow(() -> CommonUtil.reverseSort(List.of(1)));
        assertDoesNotThrow(() -> CommonUtil.sort(List.of(1)));
        assertDoesNotThrow(() -> CommonUtil.sort(List.of("a"), Comparator.<String> naturalOrder()));
        assertDoesNotThrow(() -> CommonUtil.reverseSortBy(List.of("a"), String::length));
        assertDoesNotThrow(() -> CommonUtil.reverseSortByInt(List.of("a"), String::length));
        assertDoesNotThrow(() -> CommonUtil.sortByDouble(Collections.unmodifiableList(Arrays.asList("a")), s -> 1.0));
        assertDoesNotThrow(() -> CommonUtil.parallelSort(List.of(1)));
        assertDoesNotThrow(() -> CommonUtil.parallelSort(Collections.unmodifiableList(Arrays.asList(1, 2)), 1, 2, Comparator.<Integer> naturalOrder()));
        assertDoesNotThrow(() -> CommonUtil.sort(List.of(), 0, 0, Comparator.<Integer> naturalOrder()));
        assertDoesNotThrow(() -> CommonUtil.sort((List<Integer>) null, 0, 0, Comparator.<Integer> naturalOrder()));
        assertThrows(UnsupportedOperationException.class, () -> CommonUtil.reverseSort(List.of(1, 2)));
    }

    @Test
    public void testC113_documentedKeyOrderAndStability() {
        final List<String> list = new ArrayList<>(Arrays.asList("b", null, "a"));
        CommonUtil.sortBy(list, s -> s == null ? null : s.charAt(0));
        assertEquals(Arrays.asList(null, "a", "b"), list); // null key first

        final List<String> rev = new ArrayList<>(Arrays.asList("b", null, "a"));
        CommonUtil.reverseSortBy(rev, s -> s == null ? null : s.charAt(0));
        assertEquals(Arrays.asList("b", "a", null), rev); // null key last

        final List<String> stable = new ArrayList<>(Arrays.asList("x1", "y1", "x2", "y2", "x3"));
        CommonUtil.sortByInt(stable, s -> s.charAt(0));
        assertEquals(Arrays.asList("x1", "x2", "x3", "y1", "y2"), stable);
    }

    // ============================================================ C-111 / C-137: documented float/double total order

    @Test
    public void testC111_isSortedAndSortUseTotalOrder() {
        assertFalse(CommonUtil.isSorted(new double[] { 0.0, -0.0 }));
        assertFalse(CommonUtil.isSorted(new double[] { Double.NaN, 1 }));
        assertTrue(CommonUtil.isSorted(new float[] { 1f, Float.NaN }));
        final double[] d = { 1, Double.NaN, 0.0, -0.0, Double.POSITIVE_INFINITY };
        CommonUtil.sort(d);
        assertArrayEquals(new double[] { -0.0, 0.0, 1, Double.POSITIVE_INFINITY, Double.NaN }, d);
    }

    @Test
    public void testC137_reverseSortPutsNaNFirst() {
        final double[] d = { 1, Double.NaN, -0.0, 0.0, 3 };
        CommonUtil.reverseSort(d);
        assertArrayEquals(new double[] { Double.NaN, 3, 1, 0.0, -0.0 }, d);
        final float[] f = { 1, Float.NaN, -0.0f, 0.0f };
        CommonUtil.reverseSort(f);
        assertArrayEquals(new float[] { Float.NaN, 1, 0.0f, -0.0f }, f);
    }

    // ============================================================ C-112

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testC112_nonArrayNewTypeIsNamed() {
        final Object[] src = { "a", "b" };
        final IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> CommonUtil.copyOfRange(src, 0, 1, (Class) String.class));
        assertTrue(e1.getMessage().contains("'newType' must be an array class"), e1.getMessage());
        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class,
                () -> CommonUtil.copyOfRange(src, 0, 2, 2, (Class) String.class));
        assertTrue(e2.getMessage().contains("'newType' must be an array class"), e2.getMessage());
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.copyOfRange(src, 0, 1, (Class) null));
        assertArrayEquals(new String[] { "a" }, CommonUtil.copyOfRange(src, 0, 1, String[].class));
    }

    // ============================================================ C-116 / C-120 / C-122: copy

    @Test
    public void testC116_partialCopyStateOnArrayStoreExceptionMatchesSystemArraycopy() {
        final Object[] src = { "s0", 7, "s2", "s3" };
        final String[] dest = new String[5];
        assertThrows(ArrayStoreException.class, () -> CommonUtil.copy(src, 0, dest, 1, 3));
        assertArrayEquals(new String[] { null, "s0", null, null, null }, dest);

        final String[] jdk = new String[5];
        assertThrows(ArrayStoreException.class, () -> System.arraycopy(src, 0, jdk, 1, 3));
        assertArrayEquals(jdk, dest);

        // overlapping self-copies still work both ways
        final Object[] self = { 1, 2, 3, 4, 5 };
        CommonUtil.copy(self, 0, self, 1, 4);
        assertArrayEquals(new Object[] { 1, 1, 2, 3, 4 }, self);
        final int[] ints = { 1, 2, 3, 4, 5 };
        CommonUtil.copy(ints, 1, ints, 0, 4);
        assertArrayEquals(new int[] { 2, 3, 4, 5, 5 }, ints);
        final int[] ints2 = { 1, 2, 3, 4, 5 };
        CommonUtil.copy(ints2, 0, ints2, 1, 4);
        assertArrayEquals(new int[] { 1, 1, 2, 3, 4 }, ints2);
    }

    @Test
    public void testC120_nullArrayWithZeroLengthIsNoOp() {
        final Object dest = new String[3];
        final Object src = new String[] { "a" };
        assertDoesNotThrow(() -> CommonUtil.copy((Object) null, 0, dest, 0, 0));
        assertDoesNotThrow(() -> CommonUtil.copy(src, 0, (Object) null, 0, 0));
        assertDoesNotThrow(() -> CommonUtil.copy((Object) null, 0, (Object) null, 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.copy((Object) null, 0, dest, 0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.copy((Object) null, 1, dest, 0, 0));
        // incompatible non-null arrays still fail, even with length 0
        assertThrows(ArrayStoreException.class, () -> CommonUtil.copy((Object) new int[1], 0, dest, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.copy("abc", 0, dest, 0, 0));
    }

    @Test
    public void testC122_threeArgCopyOfRangeUnchanged() {
        assertArrayEquals(new int[] { 2, 3 }, CommonUtil.copyOfRange(new int[] { 1, 2, 3 }, 1, 3));
        assertArrayEquals(new char[0], CommonUtil.copyOfRange(new char[] { 'a' }, 1, 1));
        assertArrayEquals(new Object[] { "b" }, CommonUtil.copyOfRange(new Object[] { "a", "b" }, 1, 2, Object[].class));
        assertDoesNotThrow(() -> CommonUtil.copy((int[]) null, 0, (int[]) null, 0, 0));
        assertDoesNotThrow(() -> CommonUtil.copy((Object[]) null, 0, new Object[1], 1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.copyOfRange(new long[1], 0, 2));
    }

    // ============================================================ C-118 / C-119: cycle, fill

    @Test
    public void testC118_cycleAndRepeatElementsToSizeAcceptSubtypes() {
        final List<Integer> ints = Arrays.asList(1, 2);
        // COMPILE-ONLY-FIX-BEGIN
        final List<Number> cycled = CommonUtil.cycle(ints, 2);
        final List<Number> repeated = CommonUtil.repeatElementsToSize(ints, 5);
        // COMPILE-ONLY-FIX-END
        assertEquals(Arrays.asList(1, 2, 1, 2), CommonUtil.cycle(ints, 2));
        assertEquals(Arrays.asList(1, 1, 1, 2, 2), CommonUtil.repeatElementsToSize(ints, 5));
        assertEquals(Collections.emptyList(), CommonUtil.cycle((Collection<Integer>) null, 3));
        assertEquals(Collections.emptyList(), CommonUtil.repeatElementsToSize(ints, 0));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.repeatElementsToSize(Collections.<Integer> emptyList(), 1));
    }

    @Test
    public void testC119_growingFillFailsBeforeOverwriting() {
        final List<String> fixed = Arrays.asList("a", "b");
        assertThrows(UnsupportedOperationException.class, () -> CommonUtil.fill(fixed, 0, 3, "z"));
        assertEquals(Arrays.asList("a", "b"), fixed);

        final List<String> list = new ArrayList<>(Arrays.asList("a"));
        CommonUtil.fill(list, 3, 5, "z");
        assertEquals(Arrays.asList("a", null, null, "z", "z"), list);

        final List<String> list2 = new ArrayList<>(Arrays.asList("a", "b", "c"));
        CommonUtil.fill(list2, 1, 5, "z");
        assertEquals(Arrays.asList("a", "z", "z", "z", "z"), list2);

        final List<Integer> ll = new LinkedList<>();

        for (int i = 0; i < 100; i++) {
            ll.add(i);
        }

        CommonUtil.fill(ll, 10, 150, -1);
        assertEquals(150, ll.size());
        assertEquals(9, ll.get(9));
        assertEquals(-1, ll.get(10));
        assertEquals(-1, ll.get(149));

        final List<String> empty = new ArrayList<>();
        CommonUtil.fill(empty, 0, 2, "é");
        assertEquals(Arrays.asList("é", "é"), empty);
    }

    // ============================================================ C-125 / C-126: equalsEverything / hashCodeEverything

    public static class Event {
        private String name;
        private Calendar when;

        public Event() {
        }

        public Event(final String name, final Calendar when) {
            this.name = name;
            this.when = when;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public Calendar getWhen() {
            return when;
        }

        public void setWhen(final Calendar when) {
            this.when = when;
        }
    }

    /** A user bean whose own equals is shallow over List<int[]>: equalsEverything must keep traversing it. */
    public static class ArrayHolder {
        private List<int[]> rows;

        public ArrayHolder() {
        }

        public ArrayHolder(final List<int[]> rows) {
            this.rows = rows;
        }

        public List<int[]> getRows() {
            return rows;
        }

        public void setRows(final List<int[]> rows) {
            this.rows = rows;
        }

        @Override
        public boolean equals(final Object obj) {
            return obj instanceof ArrayHolder && java.util.Objects.equals(rows, ((ArrayHolder) obj).rows);
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    public record ArrayRecord(int[] values, String label) {
    }

    public static class BizException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final String code;

        public BizException(final String message, final String code) {
            super(message);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    private static void assertEverythingEqual(final Object a, final Object b) {
        assertTrue(CommonUtil.equalsEverything(a, b));
        assertTrue(CommonUtil.equalsEverything(b, a));
        assertEquals(CommonUtil.hashCodeEverything(a), CommonUtil.hashCodeEverything(b));
    }

    private static void assertEverythingDifferent(final Object a, final Object b) {
        assertFalse(CommonUtil.equalsEverything(a, b));
        assertFalse(CommonUtil.equalsEverything(b, a));
    }

    @Test
    public void testC125_jdkBeanShapedValuesAreLeaves() {
        assertEverythingDifferent(new GregorianCalendar(2020, 0, 1), new GregorianCalendar(2024, 5, 30));
        assertNotEquals(CommonUtil.hashCodeEverything(new GregorianCalendar(2020, 0, 1)), CommonUtil.hashCodeEverything(new GregorianCalendar(2024, 5, 30)));
        assertEverythingEqual(new GregorianCalendar(2020, 0, 1), new GregorianCalendar(2020, 0, 1));

        assertEverythingDifferent(new Event("launch", new GregorianCalendar(2020, 0, 1)), new Event("launch", new GregorianCalendar(2024, 5, 30)));
        assertEverythingEqual(new Event("launch", new GregorianCalendar(2020, 0, 1)), new Event("launch", new GregorianCalendar(2020, 0, 1)));

        final RuntimeException[] ex = new RuntimeException[2];

        for (int i = 0; i < 2; i++) {
            ex[i] = new RuntimeException("msg" + i);
        }

        assertEverythingDifferent(ex[0], ex[1]);
        assertEverythingEqual(ex[0], ex[0]);
        assertEverythingDifferent(new BizException("a", "C1"), new BizException("b", "C1"));
        assertEverythingDifferent(new SimpleDateFormat("yyyy"), new SimpleDateFormat("MM-dd"));
        assertEverythingEqual(new SimpleDateFormat("yyyy"), new SimpleDateFormat("yyyy"));
        assertEverythingDifferent(new Thread("t"), new Thread("t"));
    }

    @Test
    public void testC125_userBeansAndRecordsStillTraverse() {
        assertEverythingEqual(new ArrayHolder(Arrays.asList(new int[] { 1, 2 })), new ArrayHolder(Arrays.asList(new int[] { 1, 2 })));
        assertEverythingDifferent(new ArrayHolder(Arrays.asList(new int[] { 1, 2 })), new ArrayHolder(Arrays.asList(new int[] { 1, 3 })));
        assertEverythingEqual(new ArrayRecord(new int[] { 1 }, "ü"), new ArrayRecord(new int[] { 1 }, "ü"));
        assertEverythingDifferent(new ArrayRecord(new int[] { 1 }, "x"), new ArrayRecord(new int[] { 2 }, "x"));
        assertEverythingEqual(null, null);
        assertEverythingDifferent(new ArrayHolder(null), null);
        assertEverythingEqual(Arrays.asList(new ArrayHolder(Collections.emptyList())), Arrays.asList(new ArrayHolder(Collections.emptyList())));
    }

    @Test
    public void testC126_freshBeanGettersNoLongerOverflowForJdkTypes() {
        assertEverythingEqual(new java.awt.Point(1, 2), new java.awt.Point(1, 2));
        assertEverythingDifferent(new java.awt.Point(1, 2), new java.awt.Point(1, 3));
        assertEverythingEqual(new java.awt.Rectangle(1, 2, 3, 4), new java.awt.Rectangle(1, 2, 3, 4));
        assertEverythingDifferent(new java.awt.Rectangle(1, 2, 3, 4), new java.awt.Rectangle(1, 2, 3, 5));
        assertEverythingEqual(new java.awt.Dimension(1, 2), new java.awt.Dimension(1, 2));
        assertDoesNotThrow(() -> CommonUtil.hashCodeEverything(Arrays.asList(new java.awt.Point(1, 2))));
    }

    // ============================================================ C-127: deepToString expands arrays only (doc)

    @Test
    public void testC127_deepToStringMatchesArraysDeepToString() {
        final Object[] nested = { new int[] { 5 }, "x", new Object[] { new long[] { 1L } } };
        assertEquals(Arrays.deepToString(nested), CommonUtil.deepToString(nested));
        assertEquals("[[[5]]]", CommonUtil.toString(new Object[] { List.of(new int[] { 5 }) }));
        assertTrue(CommonUtil.deepToString(new Object[] { List.of(new int[] { 5 }) }).startsWith("[[[I@"));
    }

    // ============================================================ C-128 / C-129 / C-130 / C-131

    @Test
    public void testC128_checkBeanClassNamesTheRejectionReason() {
        final IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkBeanClass(ArrayList.class));
        assertTrue(e1.getMessage().startsWith("Not a bean class: java.util.ArrayList - Collection implementations"), e1.getMessage());
        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkBeanClass(String.class));
        assertTrue(e2.getMessage().contains("CharSequence implementations"), e2.getMessage());
        final IllegalArgumentException e3 = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkBeanClass(Integer.class));
        assertTrue(e3.getMessage().contains("Number implementations"), e3.getMessage());
        final IllegalArgumentException e4 = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkBeanClass(HashMap.class));
        assertTrue(e4.getMessage().contains("Map implementations"), e4.getMessage());
        final IllegalArgumentException e5 = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkBeanClass(Object.class));
        assertTrue(e5.getMessage().contains("no property getter/setter method"), e5.getMessage());
        final IllegalArgumentException e6 = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkBeanClass(null));
        assertTrue(e6.getMessage().startsWith("Not a bean class: null"), e6.getMessage());
        assertDoesNotThrow(() -> CommonUtil.checkBeanClass(Event.class));
    }

    @Test
    public void testC129_deltaIsValidatedEagerly() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.equals(new float[] { 1 }, new float[] { 1, 2 }, -1f));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.equals((float[]) null, new float[] { 1 }, -1f));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.equals(new float[0], new float[0], Float.NaN));
        final float[] same = { 1f };
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.equals(same, same, -0.5f));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.equals(new double[] { 1 }, new double[] { 1, 2 }, -1d));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.equals((double[]) null, null, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.equals(new float[] { 1 }, 0, new float[] { 1 }, 0, 0, -1f));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.equals(new double[] { 1 }, 0, new double[] { 1 }, 0, 0, Double.NaN));
        // range validation still comes first for the range overloads
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.equals(new double[] { 1 }, 0, new double[] { 1 }, 0, 2, -1d));

        assertTrue(CommonUtil.equals(new float[] { 1 }, new float[] { 1.01f }, 0.02f));
        assertTrue(CommonUtil.equals((double[]) null, null, 0d));
        assertFalse(CommonUtil.equals(new double[] { 1 }, new double[] { 1, 2 }, 0d));
    }

    @Test
    public void testC130_fuzzyElementSemanticsAsDocumented() {
        assertTrue(CommonUtil.equals(new float[] { 0f }, new float[] { -0f }, 0f));
        assertFalse(CommonUtil.equals(new float[] { 0f }, new float[] { -0f }));
        assertTrue(CommonUtil.equals(new double[] { Double.NaN }, new double[] { Double.NaN }, 0d));
        assertTrue(CommonUtil.equals(new double[] { Double.POSITIVE_INFINITY }, new double[] { Double.POSITIVE_INFINITY }, 0d));
    }

    @Test
    public void testC131_nullVersusEmptyRangeHashAsDocumented() {
        assertEquals(0, CommonUtil.hashCode((int[]) null, 0, 0));
        assertEquals(1, CommonUtil.hashCode(new int[] { 1, 2 }, 1, 1));
        assertEquals(1, CommonUtil.deepHashCode(new Object[] { "a" }, 1, 1));
        assertEquals(0, CommonUtil.deepHashCode((Object[]) null, 0, 0));
    }

    // ============================================================ C-136 / C-141 / C-142 / C-143 / C-138: check* family

    @Test
    public void testC136_nanMessagesSayNaN() {
        assertEquals("'val' cannot be NaN", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(Double.NaN, "val")).getMessage());
        assertEquals("'val' cannot be NaN", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(Float.NaN, "val")).getMessage());
        assertEquals("'val' cannot be NaN", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(Double.NaN, "val")).getMessage());
        assertEquals("'val' cannot be NaN", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(Float.NaN, "val")).getMessage());
        assertEquals("'val' cannot be negative: -1.0",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-1.0, "val")).getMessage());
        assertEquals("'val' cannot be zero or negative: 0.0",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(0.0f, "val")).getMessage());
        assertEquals("ratio must be a real number",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(Double.NaN, "ratio must be a real number")).getMessage());
        assertEquals(-0.0, CommonUtil.checkArgNotNegative(-0.0, "val"));
        assertEquals(Double.POSITIVE_INFINITY, CommonUtil.checkArgPositive(Double.POSITIVE_INFINITY, "val"));
    }

    @Test
    public void testC141_javadocExamplesUseRealNames() {
        assertEquals("'array' cannot be null or empty", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new int[0], "array")).getMessage());
        assertEquals("'items' cannot be null or empty",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(Collections.emptyList(), "items")).getMessage());
        assertEquals("'greeting' cannot be null or empty or blank",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotBlank("   ", "greeting")).getMessage());
        assertEquals("'count' cannot be negative: -1", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative((byte) -1, "count")).getMessage());
        assertEquals("'count' cannot be zero or negative: 0", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(0L, "count")).getMessage());
        assertEquals("count must not be negative",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-1, "count must not be negative")).getMessage());
        assertEquals("'dataset' cannot be null or empty",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((Dataset) null, "dataset")).getMessage());
    }

    @Test
    public void testC142_nullOrEmptyContainersPassElementChecks() {
        assertDoesNotThrow(() -> CommonUtil.checkElementNotNull((Object[]) null));
        assertDoesNotThrow(() -> CommonUtil.checkElementNotNull(new Object[0], "a"));
        assertDoesNotThrow(() -> CommonUtil.checkElementNotNull((Collection<?>) null));
        assertDoesNotThrow(() -> CommonUtil.checkKeyNotNull((Map<?, ?>) null));
        assertDoesNotThrow(() -> CommonUtil.checkValueNotNull(new HashMap<>(), "m"));
    }

    @Test
    public void testC143_surplusArgumentsAreAppendedAsDocumented() {
        assertEquals("x 1: [2, 3]", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "x {}", 1, 2, 3)).getMessage());
        assertEquals("x 1 [2, 3, 4]",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "x {}", 1, 2, 3, 4)).getMessage());
        assertEquals("x: [1]", assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "x", 1)).getMessage());
    }

    @Test
    public void testC138_floatWidensToDoubleAsDocumented() {
        final float f = 0.1f;
        assertEquals("ratio was 0.10000000149011612", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "ratio was {}", f)).getMessage());
        assertEquals("ratio was 0.1", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "ratio was {}", (Object) f)).getMessage());
    }

    // ============================================================ C-139 / C-158: enum pools

    private enum Op {
        PLUS {
            @Override
            int apply(final int a, final int b) {
                return a + b;
            }
        },
        MINUS {
            @Override
            int apply(final int a, final int b) {
                return a - b;
            }
        };

        abstract int apply(int a, int b);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testC139_enumNameMapResolvesConstantBodies() {
        final ImmutableBiMap<Op, String> names = CommonUtil.enumNameMap((Class) Op.PLUS.getClass());
        assertEquals("PLUS", names.get(Op.PLUS));
        assertEquals(Op.MINUS, names.inverse().get("MINUS"));
        assertSame(names, CommonUtil.enumNameMap(Op.class));
        assertEquals(Arrays.asList(Op.PLUS, Op.MINUS), CommonUtil.enumListOf((Class) Op.MINUS.getClass()));

        // COMPILE-ONLY-FIX-BEGIN
        final Class<? extends Op> c = Op.PLUS.getClass();
        assertEquals(2, CommonUtil.enumNameMap(c).size());
        // COMPILE-ONLY-FIX-END
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testC139_nonEnumClassesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.enumNameMap((Class) String.class));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.enumListOf((Class) String.class));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.enumSetOf((Class) String.class));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.enumSetOf((Class) Runnable.class));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.enumNameMap(null));
        assertEquals(CommonUtil.asSet(Op.PLUS, Op.MINUS), CommonUtil.enumSetOf((Class) Op.PLUS.getClass()));
    }

    // ============================================================ C-144 / C-145 / C-146 / C-147 / C-148

    @Test
    public void testC144_supplierReturningNullIsNPE() {
        final String msg = "supplier returned null";
        assertEquals(msg, assertThrows(NullPointerException.class, () -> CommonUtil.toCollection(new int[0], n -> null)).getMessage());
        assertEquals(msg, assertThrows(NullPointerException.class, () -> CommonUtil.toCollection(new int[] { 1 }, n -> null)).getMessage());
        assertEquals(msg, assertThrows(NullPointerException.class, () -> CommonUtil.toCollection(new boolean[] { true }, 0, 1, n -> null)).getMessage());
        assertEquals(msg, assertThrows(NullPointerException.class, () -> CommonUtil.toCollection(new double[] { 1 }, 1, 1, n -> null)).getMessage());
        assertEquals(msg, assertThrows(NullPointerException.class, () -> CommonUtil.toCollection(new String[0], n -> null)).getMessage());
        assertEquals(msg, assertThrows(NullPointerException.class, () -> CommonUtil.toCollection(new String[] { "a" }, n -> null)).getMessage());
        assertEquals(msg, assertThrows(NullPointerException.class,
                () -> CommonUtil.toCollection(new String[] { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j" }, n -> null)).getMessage());
        assertEquals(msg, assertThrows(NullPointerException.class, () -> CommonUtil.toCollection(Collections.<String> emptyList(), n -> null)).getMessage());
        assertEquals(msg, assertThrows(NullPointerException.class, () -> CommonUtil.toCollection(List.of("a"), n -> null)).getMessage());
        final Iterable<String> iterable = () -> List.of("a").iterator();
        assertEquals(msg, assertThrows(NullPointerException.class, () -> CommonUtil.toCollection(iterable, n -> null)).getMessage());
        assertEquals(msg, assertThrows(NullPointerException.class, () -> CommonUtil.toCollection((Iterator<String>) null, () -> null)).getMessage());
        assertEquals(msg, assertThrows(NullPointerException.class, () -> CommonUtil.toCollection(List.of("a").iterator(), () -> null)).getMessage());

        final String mmsg = "mapSupplier returned null";
        assertEquals(mmsg, assertThrows(NullPointerException.class,
                () -> CommonUtil.toMap(Collections.<String> emptyList(), s -> s, s -> s, n -> null)).getMessage());
        assertEquals(mmsg, assertThrows(NullPointerException.class, () -> CommonUtil.toMap(List.of("a"), s -> s, s -> s, n -> null)).getMessage());
        assertEquals(mmsg, assertThrows(NullPointerException.class,
                () -> CommonUtil.toMap(List.of("a"), s -> s, s -> s, (x, y) -> x, n -> null)).getMessage());
        assertEquals(mmsg, assertThrows(NullPointerException.class,
                () -> CommonUtil.toMap((Iterator<String>) null, s -> s, s -> s, () -> null)).getMessage());
        assertEquals(mmsg, assertThrows(NullPointerException.class,
                () -> CommonUtil.toMap(List.of("a").iterator(), s -> s, s -> s, (x, y) -> x, () -> null)).getMessage());

        // happy paths unchanged
        assertEquals(Arrays.asList(1, 2), CommonUtil.toCollection(new int[] { 1, 2 }, ArrayList::new));
        assertEquals(new LinkedHashSet<>(Arrays.asList("é")), CommonUtil.toCollection(new String[] { "é", "é" }, LinkedHashSet::new));
    }

    @Test
    public void testC145_nullMergeResultRemovesKey() {
        final List<String> words = Arrays.asList("apple", "ant", "bee");
        final Map<Character, String> m1 = CommonUtil.toMap(words, s -> s.charAt(0), s -> s, (x, y) -> null, HashMap::new);
        assertEquals(Collections.singletonMap('b', "bee"), m1);
        final Map<Character, String> m2 = CommonUtil.toMap(words.iterator(), s -> s.charAt(0), s -> s, (x, y) -> null, HashMap::new);
        assertEquals(Collections.singletonMap('b', "bee"), m2);

        final List<String> calls = new ArrayList<>();
        final Map<Character, String> m3 = CommonUtil.toMap(Arrays.asList("a1", "a2", "a3"), s -> s.charAt(0), s -> s, (x, y) -> {
            calls.add(x + "+" + y);
            return null;
        }, LinkedHashMap::new);
        assertEquals(Collections.singletonList("a1+a2"), calls);
        assertEquals(Collections.singletonMap('a', "a3"), m3);

        // non-null merges unchanged
        assertEquals(Collections.singletonMap('a', "a1a2"), CommonUtil.toMap(Arrays.asList("a1", "a2"), s -> s.charAt(0), s -> s, String::concat, HashMap::new));
    }

    @Test
    public void testC146_toSetOfSmallDomainArrays() {
        assertEquals(Collections.singleton(false), CommonUtil.toSet(new boolean[100_000]));
        assertEquals(new HashSet<>(Arrays.asList(true, false)), CommonUtil.toSet(new boolean[] { true, false, true }));
        assertEquals(Collections.singleton((byte) 0), CommonUtil.toSet(new byte[10_000]));
        assertEquals(new HashSet<>(Arrays.asList('a', 'é')), CommonUtil.toSet(new char[] { 'a', 'é', 'a' }));
        assertEquals(new HashSet<>(Arrays.asList((short) 1, (short) 2)), CommonUtil.toSet(new short[] { 1, 2, 1 }, 0, 3));
        assertTrue(CommonUtil.toSet(new short[] { 1 }, 1, 1).isEmpty());
    }

    @Test
    public void testC147_floatWideningAsDocumented() {
        assertArrayEquals(new double[] { 1.21 }, CommonUtil.toDoubleArray(Arrays.asList(1.21f)));
    }

    @Test
    public void testC148_toPriorityQueueHeapifies() {
        final PriorityQueue<Integer> pq = CommonUtil.toPriorityQueue(3, 1, 2);
        assertEquals(1, pq.peek());
        assertEquals(3, pq.size());
        assertTrue(CommonUtil.toPriorityQueue((Integer[]) null).isEmpty());
        assertThrows(NullPointerException.class, () -> CommonUtil.toPriorityQueue(1, null));
        pq.add(0);
        assertEquals(0, pq.poll());
    }

    @Test
    public void testC149_C155_duplicatesAsDocumented() {
        final Map<String, Integer> m = CommonUtil.asMap("a", 1, "b", 2, "a", 3);
        assertEquals(Arrays.asList("a", "b"), new ArrayList<>(m.keySet()));
        assertEquals(3, m.get("a"));
        assertEquals(Arrays.asList("a", "b"), new ArrayList<>(CommonUtil.asSet("a", "a", "b")));
        assertEquals(1, CommonUtil.asSet("a", "a").size());
    }

    // ============================================================ C-151 / C-153 / C-154 / C-134 / C-150

    /** An ArrayList whose iterator() is unusable: only listIterator(int) may be used. */
    private static final class ListIteratorOnly<T> extends ArrayList<T> {
        private static final long serialVersionUID = 1L;

        ListIteratorOnly(final Collection<T> c) {
            super(c);
        }

        @Override
        public Iterator<T> iterator() {
            throw new UnsupportedOperationException("iterator()");
        }
    }

    @Test
    public void testC151_rangeCompareStartsListIteratorsAtTheOffset() {
        final List<Integer> a = new ListIteratorOnly<>(Arrays.asList(9, 1, 2, 3));
        final List<Integer> b = new ListIteratorOnly<>(Arrays.asList(1, 2, 4));
        assertTrue(CommonUtil.compare(a, 1, b, 0, 3) < 0);
        assertEquals(0, CommonUtil.compare(a, 1, b, 0, 2));
        assertTrue(CommonUtil.compare(a, 1, b, 0, 3, Comparator.<Integer> reverseOrder()) > 0);
        // non-List collections still walk their prefix
        assertEquals(0, CommonUtil.compare(new LinkedHashSet<>(Arrays.asList(5, 6, 7)), 1, new LinkedHashSet<>(Arrays.asList(6, 7)), 0, 2));
        assertTrue(CommonUtil.compare(Arrays.asList(null, "a"), 0, Arrays.asList("a", "a"), 0, 2) < 0);
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.compare(Arrays.asList(1), 1, Arrays.asList(1), 0, 1));
    }

    @Test
    public void testC153_tooManyElementsMessages() {
        assertEquals("Expected at most one element but was: [a, b]",
                assertThrows(TooManyElementsException.class, () -> CommonUtil.getOnlyElement(Arrays.asList("a", "b"))).getMessage());
        assertEquals("Expected at most one element but was: [a, b, ...]",
                assertThrows(TooManyElementsException.class, () -> CommonUtil.getOnlyElement(Arrays.asList("a", "b", "c"))).getMessage());
        assertEquals("Expected at most one element but was: [a, b]",
                assertThrows(TooManyElementsException.class, () -> CommonUtil.getOnlyElement(Arrays.asList("a", "b").iterator())).getMessage());
        assertEquals("Expected at most one element but was: [a, b, ...]",
                assertThrows(TooManyElementsException.class, () -> CommonUtil.getOnlyElement(Arrays.asList("a", "b", "c").iterator())).getMessage());
        assertEquals("Expected at most one element but was: [é, null]",
                assertThrows(TooManyElementsException.class, () -> CommonUtil.getOnlyElement(Arrays.asList("é", null))).getMessage());

        final Set<String> one = new HashSet<>(Arrays.asList("x"));
        final String intMsg = assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.getElement(one, 3)).getMessage();
        final String longMsg = assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.getElement(one, 3L)).getMessage();
        assertEquals("index (3) must be less than size (1)", longMsg);
        assertEquals(intMsg, longMsg);
        assertEquals("x", CommonUtil.getElement(one, 0L));
    }

    @Test
    public void testC154_comparatorDecidesNullTolerance() {
        assertThrows(NullPointerException.class, () -> CommonUtil.lessThan(null, 1, Comparator.<Integer> naturalOrder()));
        assertTrue(CommonUtil.lessThan(null, 1, Comparator.<Integer> nullsFirst(Comparator.naturalOrder())));
        assertTrue(CommonUtil.gt(null, 1, Comparator.<Integer> nullsLast(Comparator.naturalOrder())));
    }

    @Test
    public void testC134_valueSearchForPredicateNeedsObjectCast() {
        final Predicate<Object> p1 = x -> true;
        final Predicate<Object> p2 = x -> false;
        final List<Predicate<Object>> preds = Arrays.asList(p1, p2, p1);
        assertArrayEquals(new int[] { 0, 2 }, CommonUtil.indicesOfAll(preds, (Object) p1));
        assertArrayEquals(new int[] { 1, 3 }, CommonUtil.indicesOfAll(Arrays.asList("a", null, "b", null), (Object) null));
        assertArrayEquals(new int[] { 3 }, CommonUtil.indicesOfAll(Arrays.asList("a", null, "b", null), (Object) null, 2));
    }

    @Test
    public void testC150_methodReferenceComparatorsCompile() {
        assertTrue(CommonUtil.compare(Arrays.asList(1, 2), Arrays.asList(1, 3), Integer::compare) < 0);
        assertTrue(CommonUtil.compare(new String[] { "a" }, new String[] { "b" }, (String x, String y) -> x.compareTo(y)) < 0);
        final java.nio.file.Path p1 = java.nio.file.Paths.get("a");
        final java.nio.file.Path p2 = java.nio.file.Paths.get("b");
        assertTrue(p1.compareTo(p2) < 0);
    }

    // ============================================================ C-157 / C-169 / C-165: reflection factories

    @Test
    public void testC157_newInstanceRejectsFactoryStandIns() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> CommonUtil.newInstance(AbstractQueue.class));
        assertTrue(e.getMessage().contains("AbstractQueue"), e.getMessage());
        assertTrue(CommonUtil.newInstance(List.class) instanceof ArrayList);
        assertTrue(CommonUtil.newInstance(java.util.Queue.class) instanceof java.util.Queue);
        assertTrue(CommonUtil.newInstance(Map.class) instanceof Map);
        assertTrue(CommonUtil.newInstance(SortedSet.class) instanceof TreeSet);
        // newCollection keeps the documented stand-in
        assertTrue(CommonUtil.newCollection(ImmutableList.class) instanceof ArrayList);
    }

    @Test
    public void testC169_primitiveAndArrayClassesAreNotCalledAbstract() {
        final IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> CommonUtil.newInstance(int.class));
        assertTrue(e1.getMessage().contains("primitive or array"), e1.getMessage());
        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> CommonUtil.newInstance(int[].class));
        assertTrue(e2.getMessage().contains("newArray"), e2.getMessage());
        assertEquals(0, ((int[]) CommonUtil.newArray(int.class, 0)).length);
    }

    @Test
    public void testC165_proxyNeedsAnInterface() {
        final InvocationHandler h = (p, m, args) -> null;
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newProxyInstance(new Class<?>[0], h));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newProxyInstance((Class<?>[]) null, h));
        assertThrows(NullPointerException.class, () -> CommonUtil.newProxyInstance(new Class<?>[] { null }, h));
        final Runnable r = CommonUtil.newProxyInstance(new Class<?>[] { Runnable.class }, h);
        assertDoesNotThrow(r::run);
    }

    // ============================================================ C-161 / C-167 / C-162: sized factories

    @Test
    public void testC161_negativeSizesNameTheParameter() {
        for (final org.junit.jupiter.api.function.Executable ex : Arrays.<org.junit.jupiter.api.function.Executable> asList(() -> CommonUtil.newHashSet(-1),
                () -> CommonUtil.newLinkedHashMap(-1), () -> CommonUtil.newMultiset(-1), () -> CommonUtil.newListMultimap(-1),
                () -> CommonUtil.newSetMultimap(-1), () -> CommonUtil.newLinkedListMultimap(-1), () -> CommonUtil.newLinkedSetMultimap(-1),
                () -> CommonUtil.newBiMap(-1), () -> CommonUtil.newConcurrentHashSet(-1))) {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, ex);
            assertEquals("'expectedSize' cannot be negative: -1", e.getMessage());
        }

        final BiMap<String, Integer> bm = CommonUtil.newBiMap(100);

        for (int i = 0; i < 100; i++) {
            bm.put("k" + i, i);
        }

        assertEquals(100, bm.size());
        assertEquals("k42", bm.inverse().get(42));
        assertTrue(CommonUtil.newBiMap(0).isEmpty());
        assertTrue(CommonUtil.newBiMap(16, 0.5f).isEmpty());
        assertTrue(CommonUtil.newConcurrentHashSet(0).add("é"));
        assertTrue(CommonUtil.newMultiset(0).isEmpty());
    }

    @Test
    public void testC162_sortedSetMultimapSortsValues() {
        final SetMultimap<String, Integer> m = CommonUtil.newSortedSetMultimap();
        m.put("b", 2);
        m.put("a", 9);
        m.put("a", 5);
        assertEquals(Arrays.asList("a", "b"), new ArrayList<>(m.keySet()));
        assertEquals(Arrays.asList(5, 9), new ArrayList<>(m.get("a")));
        assertEquals(Arrays.asList(3), new ArrayList<>(CommonUtil.newSortedSetMultimap(CommonUtil.asMap("b", 3)).get("b")));
    }

    // ============================================================ C-159 / C-160 / C-164 / C-168: Dataset factories

    @Test
    public void testC159_firstRowMapKeysMustBeStrings() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> CommonUtil.newDataset(List.of(Map.of(1, "a"))));
        assertTrue(e.getMessage().contains("must be Strings"), e.getMessage());
        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> CommonUtil.newDataset(List.of(Map.of())));
        assertTrue(e2.getMessage().contains("empty Map"), e2.getMessage());
        final Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", 1);
        row.put("name", "é");
        final Dataset ds = CommonUtil.newDataset(List.of(row));
        assertEquals(Arrays.asList("id", "name"), ds.columnNames());
        assertEquals("é", ds.moveToRow(0).get("name"));
    }

    @Test
    public void testC159_nullRowRulesAsDocumented() {
        // standing ruling C-029 (pinned in RowDatasetFullTest): the array factory rejects a null row, the Collection one fills it with nulls
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newDataset(Arrays.asList("a", "b"), new Object[][] { { 1, 2 }, null }));
        final Dataset fromList = CommonUtil.newDataset(Arrays.asList("a", "b"), Arrays.asList(Arrays.asList(1, 2), null));
        assertEquals(2, fromList.size());
        assertNull(fromList.moveToRow(1).get("a"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newDataset(Arrays.asList("a", "b"), new Object[][] { { 1 } }));
    }

    @Test
    public void testC160_factoriesOwnFreshStorage() {
        final List<Object> column = new ArrayList<>(Arrays.asList(1, 2));
        final Dataset ds = CommonUtil.newDataset("c", column);
        column.add(3);
        assertEquals(2, ds.size());

        final Map<String, List<Object>> cols = new LinkedHashMap<>();
        cols.put("x", new ArrayList<>(Arrays.asList(1, 2, 3)));
        cols.put("y", new ArrayList<>(Arrays.asList("a")));
        final Dataset ds2 = CommonUtil.newDataset(cols);
        cols.get("x").clear();
        assertEquals(3, ds2.size());
        assertNull(ds2.moveToRow(2).get("y"));

        final Dataset kv = CommonUtil.newDataset("k", "v", CommonUtil.asMap("a", 1));
        assertEquals(1, kv.size());
        kv.addColumn("w", Arrays.asList(9));
        assertEquals(3, kv.columnCount());

        final Dataset empty = CommonUtil.newEmptyDataset(Arrays.asList("p", "q"));
        empty.addRow(new Object[] { 1, 2 });
        assertEquals(1, empty.size());

        final Dataset merged = CommonUtil.merge(Arrays.asList(ds, CommonUtil.newDataset("d", Arrays.asList(5)), CommonUtil.newDataset("c", Arrays.asList(7))));
        assertEquals(Arrays.asList("c", "d"), merged.columnNames());
        assertEquals(4, merged.size());
        assertNull(merged.moveToRow(0).get("d"));
    }

    @Test
    public void testC164_singleColumnRowRulesAsDocumented() {
        assertEquals(1, (Integer) CommonUtil.newDataset(List.of("c"), List.of(List.of(1))).moveToRow(0).get("c"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newDataset(List.of("c"), List.of(List.of(1, 2))));
        assertEquals("x", CommonUtil.newDataset(List.of("c"), List.of("x")).moveToRow(0).get("c"));
        assertNull(CommonUtil.newDataset(List.of("c"), List.of(Map.of("a", 1))).moveToRow(0).get("c"));
        assertEquals(List.of(1, 2), CommonUtil.newDataset("c", List.of(List.of(1, 2))).moveToRow(0).get("c"));
    }

    // ============================================================ C-163: toArray(c, from, to, IntFunction)

    @Test
    public void testC163_nonListRangeFollowsCollectionToArrayContract() {
        final Set<String> set = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d"));
        assertArrayEquals(new String[] { "b", "c" }, CommonUtil.toArray(set, 1, 3, n -> new String[1]));
        assertArrayEquals(new String[] { "b", "c" }, CommonUtil.toArray(Arrays.asList("a", "b", "c", "d"), 1, 3, n -> new String[1]));
        assertArrayEquals(new String[] { "b", "c", null, "x", "x" }, CommonUtil.toArray(set, 1, 3, n -> new String[] { "x", "x", "x", "x", "x" }));
        assertArrayEquals(new String[] { "b", "c", null, "x", "x" },
                CommonUtil.toArray(Arrays.asList("a", "b", "c", "d"), 1, 3, n -> new String[] { "x", "x", "x", "x", "x" }));
        assertArrayEquals(new String[] { "b", "c" }, CommonUtil.toArray(set, 1, 3, String[]::new));
        assertEquals(String[].class, CommonUtil.toArray(set, 0, 4, n -> new String[0]).getClass());
    }

    // ============================================================ C-181 / C-182 / C-183 / C-184 / C-185 / C-169 examples

    @Test
    public void testC182_fractionalAndNaNNumbersToIntegralTargets() {
        assertEquals(1, CommonUtil.convert(1.9, Integer.class));
        assertEquals(-1, CommonUtil.convert(-1.9, int.class));
        assertEquals(2L, CommonUtil.convert(2.99f, Long.class));
        assertThrows(NumberFormatException.class, () -> CommonUtil.convert("1.9", Integer.class));
        assertThrows(ArithmeticException.class, () -> CommonUtil.convert(Double.NaN, Integer.class));
    }

    /** Marker for a dynamic proxy class: it lives in a {@code jdk.proxyN} package, so it is not a built-in class. */
    public interface ConverterSource {
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testC181_registerConverterIsFirstWins() {
        final Object src = java.lang.reflect.Proxy.newProxyInstance(ConverterSource.class.getClassLoader(), new Class<?>[] { ConverterSource.class },
                (p, m, args) -> m.getName().equals("toString") ? "proxy" : null);
        final Class cls = src.getClass();
        assertTrue(CommonUtil.registerConverter(cls, (v, t) -> "first"));
        assertFalse(CommonUtil.registerConverter(cls, (v, t) -> "second"));
        assertEquals("first", CommonUtil.convert(src, String.class));
    }

    @Test
    public void testC183_defaultValueOfAsDocumented() {
        assertEquals(0, (int) CommonUtil.defaultValueOf(int.class, false));
        assertNull(CommonUtil.defaultValueOf(Integer.class, false));
        assertEquals(0, CommonUtil.defaultValueOf(Integer.class, true));
    }

    @Test
    public void testC184_castIfAssignableIgnoresTypeArguments() {
        final com.landawn.abacus.type.Type<List<String>> listOfString = new TypeReference<List<String>>() {
        }.type();
        assertTrue(CommonUtil.castIfAssignable(List.of(1), listOfString).isPresent());
    }

    @Test
    public void testC185_notEmptyConsumesOneShotIterables() {
        final Iterator<String> source = List.of("a", "b").iterator();
        final Iterable<String> once = () -> source;
        assertTrue(CommonUtil.notEmpty(once));
        assertTrue(CommonUtil.notEmpty(once)); // hasNext() alone does not consume
        assertFalse(CommonUtil.notEmpty((Iterable<String>) () -> Collections.emptyIterator()));
    }

    @Test
    public void testC169_javadocExamplesHaveConcreteValues() {
        assertArrayEquals(new boolean[] { true, false, false }, CommonUtil.toBooleanArray(new int[] { 1, 0, -1 }));
        assertArrayEquals(new boolean[] { true, true, false }, CommonUtil.toBooleanArray(Arrays.asList(true, null, false), true));
        assertArrayEquals(new boolean[] { true, false }, CommonUtil.toBooleanArray(Arrays.asList(true, null, false), 1, 3, true));
        assertArrayEquals(new char[] { 'a', 'x', 'c' }, CommonUtil.toCharArray(Arrays.asList('a', null, 'c'), 'x'));
        assertArrayEquals(new char[] { 'x', 'c' }, CommonUtil.toCharArray(Arrays.asList('a', null, 'c'), 1, 3, 'x'));
        assertArrayEquals(new byte[] { 1, 9, 3 }, CommonUtil.toByteArray(Arrays.asList((byte) 1, null, (byte) 3), (byte) 9));
        assertArrayEquals(new short[] { 9, 3 }, CommonUtil.toShortArray(Arrays.asList((short) 1, null, (short) 3), 1, 3, (short) 9));
        assertArrayEquals(new int[] { 97, 98 }, CommonUtil.toIntArray(new char[] { 'a', 'b' }));
        assertArrayEquals(new int[] { 9, 3 }, CommonUtil.toIntArray(Arrays.asList(1, null, 3), 1, 3, 9));
        assertArrayEquals(new long[] { 1, 9, 3 }, CommonUtil.toLongArray(Arrays.asList(1L, null, 3L), 9L));
        assertArrayEquals(new float[] { 9.0f, 3.0f }, CommonUtil.toFloatArray(Arrays.asList(1.0f, null, 3.0f), 1, 3, 9.0f));
        assertArrayEquals(new double[] { 1.0, 9.0, 3.0 }, CommonUtil.toDoubleArray(Arrays.asList(1.0d, null, 3.0d), 9.0d));
    }

    // ============================================================ batch 2: the convert engine (C-170 .. C-182)

    private static <T> com.landawn.abacus.type.Type<T> type(final TypeReference<T> ref) {
        return ref.type();
    }

    public static class ImmutableListBean {
        private ImmutableList<String> list;
        private ImmutableSet<Integer> ids;

        public ImmutableList<String> getList() {
            return list;
        }

        public void setList(final ImmutableList<String> list) {
            this.list = list;
        }

        public ImmutableSet<Integer> getIds() {
            return ids;
        }

        public void setIds(final ImmutableSet<Integer> ids) {
            this.ids = ids;
        }
    }

    @Test
    public void testC170_immutableTargetsFromCollections() {
        final ImmutableList<String> l = CommonUtil.convert(List.of(1, 2), type(new TypeReference<ImmutableList<String>>() {
        }));
        assertEquals(Arrays.asList("1", "2"), l);
        assertThrows(UnsupportedOperationException.class, () -> l.add("x"));

        assertEquals(Arrays.asList(1), CommonUtil.convert(new ArrayList<>(List.of(1)), ImmutableList.class));
        final ImmutableList<Integer> same = ImmutableList.of(1);
        assertSame(same, CommonUtil.convert(same, ImmutableList.class));
        assertEquals(Arrays.asList("a"), CommonUtil.convert(ImmutableList.of("a"), type(new TypeReference<ImmutableList<String>>() {
        })));

        final ImmutableSet<String> set = CommonUtil.convert(List.of(3, 1, 3, 2), type(new TypeReference<ImmutableSet<String>>() {
        }));
        assertEquals(Arrays.asList("3", "1", "2"), new ArrayList<>(set)); // insertion order

        final ImmutableSortedSet<String> sorted = CommonUtil.convert(List.of("b", "a", "é"), type(new TypeReference<ImmutableSortedSet<String>>() {
        }));
        assertEquals(Arrays.asList("a", "b", "é"), new ArrayList<>(sorted));
        final ImmutableNavigableSet<String> nav = CommonUtil.convert(List.of("b", "a"), type(new TypeReference<ImmutableNavigableSet<String>>() {
        }));
        assertEquals("a", nav.first());

        final ImmutableCollection<String> coll = CommonUtil.convert(new Integer[] { 1 }, type(new TypeReference<ImmutableCollection<String>>() {
        }));
        assertEquals(Arrays.asList("1"), new ArrayList<>(coll));

        assertTrue(CommonUtil.convert(List.of(), type(new TypeReference<ImmutableList<String>>() {
        })).isEmpty());
        assertTrue(CommonUtil.convert(new HashSet<>(), ImmutableSet.class).isEmpty());
    }

    @Test
    public void testC170_immutableAndEnumMapTargets() {
        final ImmutableMap<String, Integer> m = CommonUtil.convert(Map.of("a", "1"), type(new TypeReference<ImmutableMap<String, Integer>>() {
        }));
        assertEquals(Map.of("a", 1), m);
        assertThrows(UnsupportedOperationException.class, () -> m.put("b", 2));
        assertEquals(Map.of("a", "1"), CommonUtil.convert(new HashMap<>(Map.of("a", "1")), ImmutableMap.class));

        final ImmutableNavigableMap<String, Integer> nm = CommonUtil.convert(Map.of("b", "2", "a", "1"),
                type(new TypeReference<ImmutableNavigableMap<String, Integer>>() {
                }));
        assertEquals("a", nm.firstKey());
        final ImmutableSortedMap<String, Integer> sm = CommonUtil.convert(Map.of("b", "2", "a", "1"), type(new TypeReference<ImmutableSortedMap<String, Integer>>() {
        }));
        assertEquals("a", sm.firstKey());

        final ImmutableBiMap<String, Integer> bm = CommonUtil.convert(Map.of("a", "1"), type(new TypeReference<ImmutableBiMap<String, Integer>>() {
        }));
        assertEquals("a", bm.inverse().get(1));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.convert(Map.of("a", "1", "b", "1"), type(new TypeReference<ImmutableBiMap<String, Integer>>() {
        })));

        final EnumMap<java.util.concurrent.TimeUnit, Integer> em = CommonUtil.convert(Map.of("SECONDS", "1"),
                type(new TypeReference<EnumMap<java.util.concurrent.TimeUnit, Integer>>() {
                }));
        assertEquals(Integer.valueOf(1), em.get(java.util.concurrent.TimeUnit.SECONDS));
        assertTrue(CommonUtil.convert(Map.of(), type(new TypeReference<ImmutableMap<String, Integer>>() {
        })).isEmpty());
    }

    @SuppressWarnings({ "rawtypes" })
    @Test
    public void testC170_enumSetTargetsAndRejections() {
        final EnumSet<java.util.concurrent.TimeUnit> es = CommonUtil.convert(List.of("SECONDS", "DAYS"),
                type(new TypeReference<EnumSet<java.util.concurrent.TimeUnit>>() {
                }));
        assertEquals(EnumSet.of(java.util.concurrent.TimeUnit.SECONDS, java.util.concurrent.TimeUnit.DAYS), es);

        final IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class,
                () -> CommonUtil.convert(new ArrayList<>(List.of(java.util.concurrent.TimeUnit.SECONDS)), (Class) EnumSet.class));
        assertTrue(e1.getMessage().contains("EnumSet requires an enum element type"), e1.getMessage());
        final com.landawn.abacus.type.Type rawEnumMap = CommonUtil.typeOf(EnumMap.class); // no enum key type argument
        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> CommonUtil.convert(new HashMap<>(Map.of("a", 1)), rawEnumMap));
        assertTrue(e2.getMessage().contains("EnumMap requires an enum key type"), e2.getMessage());
    }

    @Test
    public void testC170_singleValuesAndBeansAndMaps() {
        assertEquals(Arrays.asList("x"), CommonUtil.convert("x", ImmutableList.class));
        assertEquals(Arrays.asList(1), CommonUtil.convert("[1]", type(new TypeReference<ImmutableList<Integer>>() {
        })));

        final Map<String, Object> props = new HashMap<>();
        props.put("list", new ArrayList<>(List.of("a", "b")));
        props.put("ids", List.of("1", "2"));
        final ImmutableListBean bean = Beans.mapToBean(props, ImmutableListBean.class);
        assertEquals(Arrays.asList("a", "b"), bean.getList());
        assertEquals(new HashSet<>(Arrays.asList(1, 2)), bean.getIds());
        assertEquals(Arrays.asList("a", "b"), CommonUtil.convert(props, ImmutableListBean.class).getList());

        assertEquals(Arrays.asList("a", "b"), Maps.getAs(props, "list", type(new TypeReference<ImmutableList<String>>() {
        })).orElseThrow());
        assertEquals(Arrays.asList("a", "b"), Maps.getAs(props, "list", ImmutableList.class).orElseThrow());
        final Map<String, Object> props2 = new HashMap<>();
        props2.put("list", ImmutableList.of("z"));
        assertEquals(Arrays.asList("z"), Beans.mapToBean(props2, ImmutableListBean.class).getList());
    }

    // ------------------------------------------------------------ C-171

    @Test
    public void testC171_charToEveryNumberTypeIsTheCodeUnit() {
        assertEquals(55, CommonUtil.convert('7', Integer.class));
        assertEquals(55L, CommonUtil.convert('7', Long.class));
        assertEquals(Short.valueOf((short) 65), CommonUtil.convert('A', Short.class));
        assertEquals(Byte.valueOf((byte) 65), CommonUtil.convert('A', byte.class));
        assertEquals(65.0, CommonUtil.convert('A', Double.class));
        assertEquals(65.0f, CommonUtil.convert('A', float.class));
        assertEquals(java.math.BigInteger.valueOf(65), CommonUtil.convert('A', java.math.BigInteger.class));
        assertEquals(0, new java.math.BigDecimal(65).compareTo(CommonUtil.convert('A', java.math.BigDecimal.class)));
        assertEquals(65, CommonUtil.convert('A', java.util.concurrent.atomic.AtomicInteger.class).get());
        assertEquals(55L, CommonUtil.convert('7', java.util.concurrent.atomic.AtomicLong.class).get());
        assertEquals(228L, CommonUtil.convert('ä', Long.class));
        assertEquals(0xD83DL, CommonUtil.convert('\uD83D', Long.class)); // a lone surrogate is still a code unit
        assertEquals(65535, CommonUtil.convert('￿', int.class));
        assertThrows(ArithmeticException.class, () -> CommonUtil.convert('￿', Short.class));
        assertThrows(ArithmeticException.class, () -> CommonUtil.convert('ÿ', Byte.class));
        assertEquals(65L, Maps.getAs(Map.of("c", 'A'), "c", Long.class).orElseThrow());
    }

    @Test
    public void testC171_integralNumbersToCharAreCodeUnits() {
        final Object[] fives = { (byte) 5, (short) 5, 5, 5L, java.math.BigInteger.valueOf(5), new java.math.BigDecimal("5"), new java.math.BigDecimal("5.00"),
                new java.math.BigDecimal("5E0"), 5.0d, 5.0f, new java.util.concurrent.atomic.AtomicInteger(5), new java.util.concurrent.atomic.AtomicLong(5) };

        for (final Object v : fives) {
            assertEquals(Character.valueOf('\u0005'), CommonUtil.convert(v, Character.class), v.getClass().getName());
            assertEquals('\u0005', (char) CommonUtil.convert(v, char.class), v.getClass().getName());
        }

        for (final long v : new long[] { 0, 9, 10, 65, 65535 }) {
            assertEquals(Character.valueOf((char) v), CommonUtil.convert(v, Character.class));
            assertEquals(Character.valueOf((char) v), CommonUtil.convert((int) v, Character.class));
        }

        assertEquals('A', (char) CommonUtil.convert(65.0, char.class));
        assertEquals('A', (char) CommonUtil.convert(new java.math.BigDecimal("6.5E1"), char.class));

        for (final Object bad : new Object[] { -1, 65536, -1L, 65536L, (short) -1, java.math.BigInteger.valueOf(70000), -1.0, 1e10 }) {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> CommonUtil.convert(bad, Character.class), bad.toString());
            assertTrue(e.getMessage().startsWith("Integer value out of char range: "), e.getMessage());
        }

        assertThrows(NumberFormatException.class, () -> CommonUtil.convert(65.9d, Character.class));
        assertThrows(NumberFormatException.class, () -> CommonUtil.convert(Double.NaN, Character.class));
        assertThrows(NumberFormatException.class, () -> CommonUtil.convert(new java.math.BigDecimal("5.5"), Character.class));
        // text is unchanged: a single char, or a decimal code unit
        assertEquals(Character.valueOf('7'), CommonUtil.convert("7", Character.class));
        assertEquals(Character.valueOf('A'), CommonUtil.convert("65", Character.class));
    }

    // ------------------------------------------------------------ C-172

    @Test
    public void testC172_orderAndComparatorArePreserved() {
        final Map<String, Object> lhm = new LinkedHashMap<>();

        for (final String k : new String[] { "zz", "a", "mmm", "q1", "b" }) {
            lhm.put(k, 1);
        }

        final Map<String, Integer> m = CommonUtil.convert(lhm, type(new TypeReference<Map<String, Integer>>() {
        }));
        assertEquals(Arrays.asList("zz", "a", "mmm", "q1", "b"), new ArrayList<>(m.keySet()));

        final Set<String> s = CommonUtil.convert(Arrays.asList("zz", "a", "zz", "c9", "b"), type(new TypeReference<Set<String>>() {
        }));
        assertEquals(Arrays.asList("zz", "a", "c9", "b"), new ArrayList<>(s));

        final TreeMap<String, Object> ci = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        ci.put("b", "2");
        ci.put("A", "1");
        final java.util.SortedMap<String, Integer> sorted = CommonUtil.convert(ci, type(new TypeReference<java.util.SortedMap<String, Integer>>() {
        }));
        assertEquals(Integer.valueOf(1), sorted.get("a"));
        assertSame(String.CASE_INSENSITIVE_ORDER, sorted.comparator());

        final TreeMap<String, Object> rev = new TreeMap<>(Comparator.reverseOrder());
        rev.put("1", "x");
        rev.put("2", "y");
        final TreeMap<String, String> keptRev = CommonUtil.convert(rev, type(new TypeReference<TreeMap<String, String>>() {
        }));
        assertEquals(Arrays.asList("2", "1"), new ArrayList<>(keptRev.keySet()));
        // converting key type: the old comparator cannot compare Integers, so natural order is used
        final java.util.SortedMap<Integer, String> natural = CommonUtil.convert(rev, type(new TypeReference<java.util.SortedMap<Integer, String>>() {
        }));
        assertEquals(Arrays.asList(1, 2), new ArrayList<>(natural.keySet()));

        final TreeSet<String> revSet = new TreeSet<>(Comparator.reverseOrder());
        revSet.addAll(Arrays.asList("a", "b"));
        final NavigableSet<String> keptSet = CommonUtil.convert(revSet, type(new TypeReference<NavigableSet<String>>() {
        }));
        assertEquals(Arrays.asList("b", "a"), new ArrayList<>(keptSet));

        final HashMap<String, Integer> hm = CommonUtil.convert(lhm, type(new TypeReference<HashMap<String, Integer>>() {
        }));
        assertEquals(HashMap.class, hm.getClass());
    }

    // ------------------------------------------------------------ C-173 / C-177 / C-181 (converters)

    /**
     * Marker for dynamic proxy classes (jdk.proxyN package): registrable, unlike classes in com.landawn.abacus.*.
     */
    public interface ConvertSource173 {
    }

    public interface ConvertSource177 {
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testC173_nullConverterResultForPrimitiveTargetIsDefault() {
        final Object src = java.lang.reflect.Proxy.newProxyInstance(ConvertSource173.class.getClassLoader(), new Class<?>[] { ConvertSource173.class },
                (p, m, args) -> m.getName().equals("hashCode") ? 173 : null);
        assertTrue(CommonUtil.registerConverter((Class) src.getClass(), (v, t) -> null));
        assertEquals(0, (int) CommonUtil.convert(src, int.class));
        assertEquals(false, CommonUtil.convert(src, boolean.class));
        assertEquals(0, (int) CommonUtil.convert(src, CommonUtil.typeOf(int.class)));
        assertNull(CommonUtil.convert(src, Integer.class));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testC177_assignableValuesBypassTheConverter() {
        final Object src = java.lang.reflect.Proxy.newProxyInstance(ConvertSource177.class.getClassLoader(), new Class<?>[] { ConvertSource177.class },
                (p, m, args) -> m.getName().equals("hashCode") ? 177 : m.getName().equals("equals") ? p == args[0] : "proxy177");
        final AtomicInteger calls = new AtomicInteger();
        assertTrue(CommonUtil.registerConverter((Class) src.getClass(), (v, t) -> {
            calls.incrementAndGet();

            if (t == String.class) {
                return "converted";
            }

            throw new IllegalArgumentException("Unsupported target: " + t);
        }));

        assertSame(src, CommonUtil.convert(src, ConvertSource177.class));
        assertSame(src, CommonUtil.convert(src, Object.class));
        assertSame(src, CommonUtil.convert(src, src.getClass()));
        assertSame(src, CommonUtil.convert(src, CommonUtil.typeOf(ConvertSource177.class)));
        assertSame(src, CommonUtil.convert(List.of(src), type(new TypeReference<List<ConvertSource177>>() {
        })).get(0));
        assertSame(src, CommonUtil.convert(Map.of("k", src), type(new TypeReference<Map<String, ConvertSource177>>() {
        })).get("k"));
        assertSame(src, CommonUtil.convert(List.of(src), ConvertSource177[].class)[0]);
        assertEquals(0, calls.get());

        assertEquals("converted", CommonUtil.convert(src, String.class));
        assertEquals(1, calls.get());
    }

    // ------------------------------------------------------------ C-174 / C-178

    @Test
    public void testC174_textSourcesToBooleanAreRead() throws Exception {
        for (final String t : new String[] { "true", "TRUE", "1", " Y " }) {
            final Boolean expected = CommonUtil.convert(t, Boolean.class);
            assertEquals(expected, CommonUtil.convert(new java.io.StringReader(t), Boolean.class), t);
            assertEquals(expected, CommonUtil.convert(new java.io.ByteArrayInputStream(t.getBytes(java.nio.charset.StandardCharsets.UTF_8)), boolean.class), t);
            assertEquals(expected, CommonUtil.convert(new javax.sql.rowset.serial.SerialClob(t.toCharArray()), Boolean.class), t);
        }

        assertTrue(CommonUtil.convert(new java.io.StringReader("true"), Boolean.class));
        assertFalse(CommonUtil.convert(new java.io.StringReader("false"), Boolean.class));
        assertFalse(CommonUtil.convert(new java.io.StringReader("junk"), Boolean.class));
        assertFalse(CommonUtil.convert(new java.io.StringReader(""), Boolean.class));
        assertFalse(CommonUtil.convert(new java.io.StringReader(""), boolean.class));
        // unchanged (documented): char[] / byte[] are not read as text for a boolean
        assertFalse(CommonUtil.convert("true".toCharArray(), Boolean.class));
    }

    @Test
    public void testC178_textSourceMatrixAsDocumented() {
        assertArrayEquals(new char[] { 'T', 'e' }, CommonUtil.convert(new java.io.StringReader("Te"), char[].class));
        assertArrayEquals("ab".getBytes(), CommonUtil.convert(new java.io.ByteArrayInputStream("ab".getBytes()), byte[].class));
        assertEquals("Te", CommonUtil.convert(new java.io.StringReader("Te"), String.class));
        assertThrows(NumberFormatException.class, () -> CommonUtil.convert("Test", char[].class));
        assertEquals("['a', 'b']", CommonUtil.convert(new char[] { 'a', 'b' }, String.class));
        assertEquals("[97, 98]", CommonUtil.convert(new byte[] { 97, 98 }, String.class));
        assertEquals(42, CommonUtil.convert(new java.io.StringReader("42"), Integer.class));
    }

    // ------------------------------------------------------------ C-175 / C-176

    @Test
    public void testC175_nullKeyIntoTreeMapIsIAE() {
        final Map<String, String> src = new HashMap<>();
        src.put(null, "1");
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> CommonUtil.convert(src, type(new TypeReference<TreeMap<String, Integer>>() {
        })));
        assertTrue(e.getMessage().contains("not accepted by"), e.getMessage());
        assertEquals(Integer.valueOf(1), CommonUtil.convert(src, type(new TypeReference<HashMap<String, Integer>>() {
        })).get(null));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.convert(Map.of("1", "a", "01", "b"), type(new TypeReference<Map<Integer, String>>() {
        })));
    }

    @Test
    public void testC176_singleValueWrapRuleAsDocumented() {
        assertEquals(Arrays.asList("[1,2]"), CommonUtil.convert("[1,2]", type(new TypeReference<List<String>>() {
        })));
        assertEquals(Arrays.asList("[1,2]"), CommonUtil.convert("[1,2]", List.class));
        assertArrayEquals(new String[] { "[1,2]" }, CommonUtil.convert("[1,2]", String[].class));
        assertEquals(Arrays.asList(1, 2), CommonUtil.convert("[1,2]", type(new TypeReference<List<Integer>>() {
        })));
        assertEquals(2, CommonUtil.convert(new StringBuilder("[1,2]"), type(new TypeReference<List<String>>() {
        })).size());
    }

    // ------------------------------------------------------------ C-180

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testC180_jdkImplementationClassesAreBuiltin() throws Exception {
        assertTrue(CommonUtil.isBuiltinClass(java.util.TimeZone.getDefault().getClass()));
        assertTrue(CommonUtil.isBuiltinClass(java.nio.file.Paths.get("x").getClass()));
        assertTrue(CommonUtil.isBuiltinClass(Class.forName("sun.misc.Unsafe")));
        assertTrue(CommonUtil.isBuiltinClass(Class.forName("sun.misc.Unsafe").arrayType()));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.registerConverter(java.util.TimeZone.getDefault().getClass(), (v, t) -> "x"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.registerConverter(java.nio.file.Paths.get("x").getClass(), (v, t) -> "x"));
        // a JDK-internal exception class (java.base module, sun.* package) is refused by the shared rule
        final Class internalException = Class.forName(System.getProperty("os.name").startsWith("Windows") ? "sun.nio.fs.WindowsException" : "sun.nio.fs.UnixException");
        assertThrows(IllegalArgumentException.class, () -> ExceptionUtil.registerRuntimeExceptionMapper(internalException, e -> new RuntimeException()));

        final Object proxy = java.lang.reflect.Proxy.newProxyInstance(ConvertSource177.class.getClassLoader(), new Class<?>[] { Runnable.class },
                (p, m, args) -> null);
        assertFalse(CommonUtil.isBuiltinClass(proxy.getClass()));
        assertFalse(CommonUtil.isBuiltinClass(org.opentest4j.AssertionFailedError.class)); // third-party classes stay registrable
        assertTrue(CommonUtil.isBuiltinClass(String.class));
        assertTrue(CommonUtil.isBuiltinClass(CommonUtilReview20260924Test.class)); // com.landawn.abacus.* package rule unchanged
    }
}
