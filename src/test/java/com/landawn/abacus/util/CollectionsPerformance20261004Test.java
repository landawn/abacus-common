package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("unit")
class CollectionsPerformance20261004Test extends TestBase {
    enum Key {
        A, B
    }

    @Test
    void multimapCopiesDoNotCaptureAncestorStorage() throws Exception {
        ListMultimap<String, Object> source = ListMultimap.of("a", new byte[1024]);
        ListMultimap<String, Object> copy = source.copy();
        ListMultimap<String, Object> secondCopy = copy.copy();
        source.put("later", new Object());
        copy.clear();
        secondCopy.clear();
        assertDoesNotCapture(copy.mapSupplier, source.backingMap);
        assertDoesNotCapture(secondCopy.mapSupplier, source.backingMap);
        assertDoesNotCapture(secondCopy.mapSupplier, copy.backingMap);
        SetMultimap<String, Object> sets = SetMultimap.of("a", new Object());
        SetMultimap<String, Object> setCopy = sets.copy();
        setCopy.clear();
        assertDoesNotCapture(setCopy.mapSupplier, sets.backingMap);
        assertEquals(2, source.toMultiset().size());
    }

    private static void assertDoesNotCapture(Object factory, Object forbidden) throws Exception {
        if (factory == null) {
            return;
        }
        assertNotSame(forbidden, factory);
        for (Field field : factory.getClass().getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                assertNotSame(forbidden, field.get(factory), "Factory retains an ancestor backing map");
            }
        }
    }

    public static final class LimitedMap<K, V> extends LinkedHashMap<K, V> {
        private final int limit;

        public LimitedMap(int limit) {
            this.limit = limit;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > limit;
        }
    }

    @Test
    void multimapCopiesPreserveMapMetadataAndExplicitFactories() {
        TreeMap<String, List<Integer>> sorted = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        sorted.put("A", new ArrayList<>(List.of(1)));
        ListMultimap<String, Integer> copy = new ListMultimap<>(sorted, ArrayList::new).copy();
        assertEquals(List.of(1), copy.get("a"));
        assertSame(String.CASE_INSENSITIVE_ORDER, ((TreeMap<?, ?>) copy.backingMap).comparator());
        assertEquals(1, copy.toMultiset().getCount("a"));
        EnumMap<Key, List<Integer>> enums = new EnumMap<>(Key.class);
        ListMultimap<Key, Integer> emptyEnumCopy = new ListMultimap<>(enums, ArrayList::new).copy();
        emptyEnumCopy.put(Key.B, 2);
        emptyEnumCopy.put(Key.A, 1);
        assertEquals(List.of(Key.A, Key.B), new ArrayList<>(emptyEnumCopy.backingMap.keySet()));
        assertInstanceOf(EnumMap.class, emptyEnumCopy.copy().backingMap);
        String a = new String("same"), b = new String("same");
        IdentityHashMap<String, List<Integer>> identity = new IdentityHashMap<>();
        identity.put(a, new ArrayList<>(List.of(1)));
        identity.put(b, new ArrayList<>(List.of(2)));
        assertEquals(2, new ListMultimap<>(identity, ArrayList::new).copy().keyCount());
        assertInstanceOf(WeakHashMap.class, new ListMultimap<>(new WeakHashMap<String, List<Integer>>(), ArrayList::new).copy().backingMap);
        ConcurrentSkipListMap<String, List<Integer>> concurrent = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);
        assertInstanceOf(ConcurrentSkipListMap.class, new ListMultimap<>(concurrent, ArrayList::new).copy().backingMap);
        LimitedMap<Integer, List<Integer>> limited = new LimitedMap<>(100);
        ListMultimap<Integer, Integer> growing = new ListMultimap<>(limited, ArrayList::new);
        for (int i = 0; i < 20; i++) {
            growing.put(i, i);
        }
        assertEquals(20, growing.copy().keyCount(), "Custom int constructor receives the current size, not construction-time size");
        AtomicInteger calls = new AtomicInteger();
        ListMultimap<String, Integer> supplied = new ListMultimap<>(() -> {
            calls.incrementAndGet();
            return new LinkedHashMap<>();
        }, ArrayList::new);
        supplied.put("x", 1);
        supplied.copy().copy();
        assertEquals(3, calls.get());
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newMultimap(() -> sorted, ArrayList::new));
    }

    static final class CountingList extends ArrayList<Integer> {
        int nextCalls;

        CountingList(int size) {
            super(size);
            for (int i = 0; i < size; i++) {
                add(i);
            }
        }

        @Override
        public Iterator<Integer> iterator() {
            return counting(super.listIterator());
        }

        @Override
        public ListIterator<Integer> listIterator(int index) {
            return counting(super.listIterator(index));
        }

        private ListIterator<Integer> counting(ListIterator<Integer> it) {
            return new ListIterator<>() {
                public boolean hasNext() {
                    return it.hasNext();
                }

                public Integer next() {
                    nextCalls++;
                    return it.next();
                }

                public boolean hasPrevious() {
                    return it.hasPrevious();
                }

                public Integer previous() {
                    return it.previous();
                }

                public int nextIndex() {
                    return it.nextIndex();
                }

                public int previousIndex() {
                    return it.previousIndex();
                }

                public void remove() {
                    it.remove();
                }

                public void set(Integer e) {
                    it.set(e);
                }

                public void add(Integer e) {
                    it.add(e);
                }
            };
        }
    }

    @Test
    void rangedMismatchPositionsListsAndPreservesCollectionFallback() {
        CountingList a = new CountingList(100_001), b = new CountingList(100_001);
        AtomicInteger extracts = new AtomicInteger();
        assertEquals(-1, CommonUtil.mismatch(a, 100_000, b, 100_000, 1, v -> {
            extracts.incrementAndGet();
            return v;
        }));
        assertEquals(2, a.nextCalls + b.nextCalls);
        assertEquals(2, extracts.get());
        List<Integer> linked = new LinkedList<>(List.of(7, 8, 9));
        assertEquals(-1, CommonUtil.mismatch(linked, 1, List.of(8, 9), 0, 2, v -> v));
        assertEquals(1, CommonUtil.mismatch(new LinkedHashSet<>(List.of(7, 8, 9)), 1, List.of(8, 0), 0, 2, v -> v));
        assertEquals(-1, CommonUtil.mismatch(a, a.size(), b, b.size(), 0, v -> v));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.mismatch(a, -1, b, 0, 1, v -> v));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.mismatch(a, 0, b, 0, 1, null));
    }

    static final class Countable extends Throwables.Iterator<Integer, Exception> {
        int remaining = 100, nextCalls, countCalls;
        boolean failNext, failCount;

        @Override
        public boolean hasNext() {
            return remaining > 0;
        }

        @Override
        public Integer next() throws Exception {
            if (failNext) {
                failNext = false;
                throw new Exception("next");
            }
            nextCalls++;
            return remaining--;
        }

        @Override
        public long count() throws Exception {
            countCalls++;
            if (failCount) {
                throw new Exception("count");
            }
            int n = remaining;
            remaining = 0;
            return n;
        }
    }

    @Test
    void skippedCountDelegatesAndPreservesFailureProgressAndClose() throws Exception {
        Countable source = new Countable();
        AtomicInteger closes = new AtomicInteger();
        assertEquals(95, Seq.of(source).skip(2).skip(3).onClose(closes::incrementAndGet).count());
        assertEquals(5, source.nextCalls);
        assertEquals(1, source.countCalls);
        assertEquals(1, closes.get());
        assertEquals(Integer.MAX_VALUE - 1L, Seq.range(0, Integer.MAX_VALUE).skip(1).count());
        assertEquals(0, Seq.of(1, 2).skip(100).count());
        Countable partial = new Countable();
        partial.next();
        Seq<Integer, Exception> seq = Seq.of(partial).skip(2);
        partial.failNext = true;
        assertThrows(Exception.class, () -> seq.iteratorEx().hasNext());
        assertEquals(97, seq.count());
        assertEquals(3, partial.nextCalls);
        Countable failing = new Countable();
        failing.failCount = true;
        AtomicInteger failedClose = new AtomicInteger();
        assertThrows(Exception.class, () -> Seq.of(failing).skip(1).onClose(failedClose::incrementAndGet).count());
        assertEquals(1, failedClose.get());
        AtomicInteger mapped = new AtomicInteger();
        assertEquals(2, Seq.of(1, 2, 3).map(v -> {
            mapped.incrementAndGet();
            return v;
        }).skip(1).count());
        assertEquals(3, mapped.get());
    }

    @Test
    void datasetOccurrenceResultsKeepIndependentColumns() {
        Object payload = new Object();
        Dataset left = Dataset.columns(List.of("key", "value"), List.of(List.of(1, 1, 2), List.of(payload, payload, payload)));
        Dataset right = Dataset.columns(List.of("value", "key"), List.of(List.of(payload), List.of(1)));
        Dataset intersection = N.intersection(left, right, List.of("key"), true);
        Dataset difference = N.difference(left, right, List.of("key"), true);
        Dataset multi = N.intersection(left, right, List.of("key", "value"), true);
        assertEquals(1, intersection.size());
        assertEquals(2, difference.size());
        assertEquals(1, multi.size());
        assertEquals(List.of("key", "value"), new ArrayList<>(difference.columnNames()));
        left.set(1, 1, "changed");
        difference.set(0, 0, 3);
        assertEquals(1, (int) left.get(1, 0));
        assertSame(payload, difference.get(0, 1));
        assertSame(payload, intersection.get(0, 1));
        assertEquals(2, N.symmetricDifference(left, right, List.of("key"), true).size());
    }

    @Test
    void primitiveFiltersPreserveRangesOrderBitsAndSingleEvaluation() throws Exception {
        Object[] arrays = { new boolean[75], new char[75], new byte[75], new short[75], new int[75], new long[75], new float[75], new double[75] };
        for (Object input : arrays) {
            for (int i = 0; i < 75; i++) {
                if (input instanceof boolean[] a) {
                    a[i] = (i & 1) == 0;
                } else if (input instanceof char[] a) {
                    a[i] = (char) i;
                } else if (input instanceof byte[] a) {
                    a[i] = (byte) i;
                } else if (input instanceof short[] a) {
                    a[i] = (short) i;
                } else if (input instanceof int[] a) {
                    a[i] = i;
                } else if (input instanceof long[] a) {
                    a[i] = Long.MAX_VALUE - i;
                } else if (input instanceof float[] a) {
                    a[i] = i == 3 ? -0f : i == 10 ? Float.intBitsToFloat(0x7fc00002) : i;
                } else if (input instanceof double[] a) {
                    a[i] = i == 3 ? -0d : i == 10 ? Double.longBitsToDouble(0x7ff8000000000002L) : i;
                }
            }
            var filterMethod = Arrays.stream(N.class.getMethods())
                    .filter(m -> m.getName().equals("filter") && m.getParameterCount() == 4 && m.getParameterTypes()[0] == input.getClass())
                    .findFirst()
                    .orElseThrow();
            Class<?> predicate = filterMethod.getParameterTypes()[3];
            int[] cursor = { 3 };
            Object test = Proxy.newProxyInstance(predicate.getClassLoader(), new Class<?>[] { predicate }, (proxy, method, args) -> {
                int index = cursor[0]++;
                assertEquals(java.lang.reflect.Array.get(input, index), args[0]);
                return (index - 3) % 7 == 0;
            });
            Object result = filterMethod.invoke(null, input, 3, 73, test);
            assertEquals(73, cursor[0]);
            assertEquals(10, java.lang.reflect.Array.getLength(result));
            for (int i = 0; i < 10; i++) {
                Object expected = java.lang.reflect.Array.get(input, 3 + i * 7), actual = java.lang.reflect.Array.get(result, i);
                assertEquals(expected, actual);
                if (expected instanceof Float f) {
                    assertEquals(Float.floatToRawIntBits(f), Float.floatToRawIntBits((Float) actual));
                }
                if (expected instanceof Double d) {
                    assertEquals(Double.doubleToRawLongBits(d), Double.doubleToRawLongBits((Double) actual));
                }
            }
        }
    }

    @Test
    void rejectingPrimitiveFilterDoesNotAllocateInputSizedScratch() {
        Assumptions.assumeTrue(ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean);
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        Assumptions.assumeTrue(bean.isThreadAllocatedMemorySupported());
        bean.setThreadAllocatedMemoryEnabled(true);
        double[] input = new double[1_000_000];
        int[] calls = { 0 };
        com.landawn.abacus.util.function.DoublePredicate predicate = v -> {
            calls[0]++;
            return false;
        };
        N.filter(input, predicate);
        calls[0] = 0;
        long id = Thread.currentThread().threadId(), before = bean.getThreadAllocatedBytes(id);
        double[] result = N.filter(input, predicate);
        long allocated = bean.getThreadAllocatedBytes(id) - before;
        assertEquals(input.length, calls[0]);
        assertEquals(0, result.length);
        assertTrue(allocated < 100_000, "Unexpected input-sized scratch: " + allocated);
    }

    @Test
    void primitiveFilterGrowthBoundsDenseAndSparseAllocations() {
        Assumptions.assumeTrue(ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean);
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        Assumptions.assumeTrue(bean.isThreadAllocatedMemorySupported());
        bean.setThreadAllocatedMemoryEnabled(true);
        double[] input = new double[1_000_000];
        for (int i = 0; i < input.length; i++) {
            input[i] = i;
        }
        com.landawn.abacus.util.function.DoublePredicate[] predicates = { value -> value % 100 == 0, value -> true, value -> value < 300_000,
                value -> value % 2 == 0 };
        int[] expectedLengths = { 10_000, 1_000_000, 300_000, 500_000 };
        long[] allocationBounds = { 500_000, 12_200_000, 6_600_000, 4_200_000 };
        long id = Thread.currentThread().threadId();
        for (int i = 0; i < predicates.length; i++) {
            for (int warmup = 0; warmup < 3; warmup++) {
                N.filter(input, predicates[i]);
            }
            long before = bean.getThreadAllocatedBytes(id);
            double[] result = N.filter(input, predicates[i]);
            long allocated = bean.getThreadAllocatedBytes(id) - before;
            assertEquals(expectedLengths[i], result.length);
            assertTrue(allocated < allocationBounds[i], "Pattern " + i + " allocated " + allocated);
        }
    }

    @Test
    void primitiveFilterCapturesValuesBeforeLaterPredicateMutations() {
        int[] input = { 1, 2, 3, 4 };
        int[] calls = { 0 };
        int[] result = N.filter(input, value -> {
            int index = calls[0]++;
            if (index > 0) {
                input[index - 1] = -1;
            }
            input[index] = value * 10;
            return true;
        });
        assertArrayEquals(new int[] { 10, 20, 30, 40 }, result);
        assertEquals(input.length, calls[0]);
    }

    @Test
    void numericMedianPairsMatchSortedRangesWithoutChangingInput() {
        Random random = new Random(731);
        for (int size : new int[] { 1, 2, 3, 4, 15, 16, 17, 31, 32, 129, 1024, 8192 }) {
            for (int shape = 0; shape < 5; shape++) {
                int[] ints = new int[size + 4];
                long[] longs = new long[size + 4];
                float[] floats = new float[size + 4];
                double[] doubles = new double[size + 4];
                for (int i = 0; i < ints.length; i++) {
                    int value = shape == 0 ? random.nextInt() : shape == 1 ? i : shape == 2 ? -i : shape == 3 ? 7 : random.nextInt(5);
                    ints[i] = value;
                    longs[i] = shape == 0 ? random.nextLong() : value;
                    floats[i] = value;
                    doubles[i] = value;
                }
                if (shape == 4 && size >= 4) {
                    floats[2] = -0f;
                    floats[3] = Float.intBitsToFloat(0x7fc00002);
                    doubles[2] = -0d;
                    doubles[3] = Double.longBitsToDouble(0x7ff8000000000002L);
                }
                int[] original = ints.clone(), si = Arrays.copyOfRange(ints, 2, size + 2);
                long[] sl = Arrays.copyOfRange(longs, 2, size + 2);
                float[] sf = Arrays.copyOfRange(floats, 2, size + 2);
                double[] sd = Arrays.copyOfRange(doubles, 2, size + 2);
                Arrays.sort(si);
                Arrays.sort(sl);
                Arrays.sort(sf);
                Arrays.sort(sd);
                var pi = Median.of(ints, 2, size + 2);
                var pl = Median.of(longs, 2, size + 2);
                var pf = Median.of(floats, 2, size + 2);
                var pd = Median.of(doubles, 2, size + 2);
                int lower = (size - 1) / 2, upper = size / 2;
                assertEquals(si[lower], pi.left().intValue());
                assertEquals(sl[lower], pl.left().longValue());
                assertEquals(Float.floatToRawIntBits(sf[lower]), Float.floatToRawIntBits(pf.left()));
                assertEquals(Double.doubleToRawLongBits(sd[lower]), Double.doubleToRawLongBits(pd.left()));
                assertEquals(size % 2 == 0, pi.right().isPresent());
                assertEquals(size % 2 == 0, pl.right().isPresent());
                assertEquals(size % 2 == 0, pf.right().isPresent());
                assertEquals(size % 2 == 0, pd.right().isPresent());
                if (size % 2 == 0) {
                    assertEquals(si[upper], pi.right().get());
                    assertEquals(sl[upper], pl.right().get());
                    assertEquals(Float.floatToRawIntBits(sf[upper]), Float.floatToRawIntBits(pf.right().get()));
                    assertEquals(Double.doubleToRawLongBits(sd[upper]), Double.doubleToRawLongBits(pd.right().get()));
                }
                assertArrayEquals(original, ints);
            }
        }
    }
}
