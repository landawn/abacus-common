package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.management.ManagementFactory;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class MapsMetadataPerformance20261030Test {
    private static volatile Object allocationSink;

    private static final class CountingCollection extends AbstractCollection<Object> {
        private final Collection<?> values;
        private int sizeCalls;
        private int sizeElementVisits;
        private int nextCalls;
        private int iteratorCalls;

        CountingCollection(final Collection<?> values) {
            this.values = values;
        }

        @Override
        public int size() {
            sizeCalls++;
            int size = 0;

            for (final Object ignored : values) {
                size++;
                sizeElementVisits++;
            }

            return size;
        }

        @Override
        public boolean isEmpty() {
            return values.isEmpty();
        }

        @Override
        public Iterator<Object> iterator() {
            iteratorCalls++;
            final Iterator<?> source = values.iterator();

            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return source.hasNext();
                }

                @Override
                public Object next() {
                    nextCalls++;
                    return source.next();
                }
            };
        }
    }

    public static final class CapacityBoundMap<K, V> extends LinkedHashMap<K, V> {
        private static final long serialVersionUID = 1L;
        private final int capacity;

        public CapacityBoundMap(final int capacity) {
            this.capacity = capacity;
        }

        @Override
        protected boolean removeEldestEntry(final Map.Entry<K, V> eldest) {
            return size() > capacity;
        }
    }

    private static final class CountingValue {
        private static int comparisons;
        private final int value;

        CountingValue(final int value) {
            this.value = value;
        }

        @Override
        public boolean equals(final Object other) {
            comparisons++;
            return other instanceof CountingValue && value == ((CountingValue) other).value;
        }

        @Override
        public int hashCode() {
            return value;
        }
    }

    @Test
    void sparseLookupPreservesEncounterOrderDuplicatesNullSkippingAndMutability() {
        final List<Object> reads = new ArrayList<>();
        final Map<String, String> source = new HashMap<>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String get(final Object key) {
                reads.add(key);
                return super.get(key);
            }
        };
        source.put("a", "first");
        source.put("b", "second");
        source.put("null", null);
        final List<String> keys = Arrays.asList("missing", "b", "null", "a", "b");
        final List<String> result = Maps.getValuesIfPresent(source, keys);

        assertEquals(List.of("second", "first", "second"), result);
        assertEquals(keys, reads);
        result.add("mutable");
        assertEquals(4, result.size());
        final List<String> empty = Maps.getValuesIfPresent(source, List.of("missing"));
        empty.add("mutable");
        assertEquals(List.of("mutable"), empty);
    }

    @Test
    void intersectionPreservesComparatorIdentityAndCustomConstructorCapacity() {
        final LinkedHashMap<String, Integer> source = new LinkedHashMap<>();
        source.put("aa", 1);
        source.put("AA", 1);
        source.put("bb", null);
        final TreeMap<String, Integer> insensitive = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        insensitive.put("AA", 1);

        // One key in map2 can match more than one key in map; its size is only an allocation hint.
        assertEquals(List.of("aa", "AA"), new ArrayList<>(Maps.intersection(source, insensitive).keySet()));

        final String first = new String("same");
        final String second = new String("same");
        final IdentityHashMap<String, Integer> identity = new IdentityHashMap<>();
        identity.put(first, 1);
        identity.put(second, 1);
        final Map<String, Integer> identityResult = Maps.intersection(identity, Map.of("same", 1));
        assertSame(IdentityHashMap.class, identityResult.getClass());
        assertEquals(2, identityResult.size());
        assertTrue(identityResult.containsKey(first));
        assertTrue(identityResult.containsKey(second));

        final Comparator<String> reverse = Comparator.reverseOrder();
        final TreeMap<String, Integer> sorted = new TreeMap<>(reverse);
        sorted.putAll(source);
        final Map<String, Integer> sortedResult = Maps.intersection(sorted, source);
        assertSame(reverse, ((TreeMap<String, Integer>) sortedResult).comparator());
        assertEquals(sorted, sortedResult);

        final CapacityBoundMap<String, Integer> bounded = new CapacityBoundMap<>(4);
        bounded.put("a", 1);
        bounded.put("b", 2);
        bounded.put("c", 3);
        bounded.put("d", 4);
        final CapacityBoundMap<String, Integer> result = (CapacityBoundMap<String, Integer>) Maps.intersection(bounded, Map.of("a", 1));
        assertEquals(4, result.capacity);
        result.put("b", 2);
        result.put("c", 3);
        result.put("d", 4);
        assertEquals(4, result.size());
        assertEquals(4, bounded.size());
    }

    @Test
    void indexedNonListSkipsSizingForFirstElementAndSizesLaterIndicesOnce() {
        final CountingCollection values = new CountingCollection(Collections.nCopies(100_000, "value"));
        final Map<String, Object> root = Map.of("values", values);

        assertEquals("value", Maps.getByPath(root, "values[0]"));
        assertEquals(0, values.sizeCalls);
        assertEquals(0, values.sizeElementVisits);
        assertEquals(1, values.nextCalls);
        assertEquals(1, values.iteratorCalls);
        assertEquals("value", Maps.getByPath(root, "values[2]"));
        assertTrue(values.sizeCalls <= 1, "A later index must not repeat a potentially linear size computation");
        assertTrue(values.sizeElementVisits <= 100_000);
        assertEquals(4, values.nextCalls);
        assertEquals(2, values.iteratorCalls);
    }

    @Test
    void indexedStandardSetMissesDoNotCreateAnIterator() {
        final int[] iteratorCalls = { 0 };
        final List<Set<Integer>> sets = List.of(new HashSet<Integer>(1 << 18) {
            @Override
            public Iterator<Integer> iterator() {
                iteratorCalls[0]++;
                return super.iterator();
            }
        }, new LinkedHashSet<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                iteratorCalls[0]++;
                return super.iterator();
            }
        }, new TreeSet<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                iteratorCalls[0]++;
                return super.iterator();
            }
        });

        for (final Set<Integer> values : sets) {
            values.add(42); // The HashSet is deliberately sparse: walking its table can be especially expensive.
            final Map<String, Object> root = Map.of("values", values);
            iteratorCalls[0] = 0;

            assertFalse(Maps.getByPathIfExists(root, "values[1]").isPresent());
            assertFalse(Maps.getByPathIfExists(root, "values[2147483647]").isPresent());
            assertEquals(0, iteratorCalls[0], values.getClass().getSuperclass().getSimpleName());

            assertEquals(Integer.valueOf(42), Maps.<Integer> getByPath(root, "values[0]"));
            assertEquals(1, iteratorCalls[0]);
            values.clear();
            assertFalse(Maps.getByPathIfExists(root, "values[0]").isPresent());
        }
    }

    @Test
    void indexedNonListMissDoesNotIterateAfterTheBoundsCheck() {
        final CountingCollection values = new CountingCollection(Collections.nCopies(100_000, "value"));

        assertFalse(Maps.getByPathIfExists(Map.of("values", values), "values[100000]").isPresent());
        assertEquals(0, values.iteratorCalls);
        assertEquals(0, values.nextCalls);
        assertEquals(1, values.sizeCalls);
        assertEquals(100_000, values.sizeElementVisits);
    }

    @Test
    void indexedTreeSetViewsPreserveFirstElementAndBounds() {
        final TreeSet<Integer> values = new TreeSet<>();
        for (int i = 0; i < 100; i++) {
            values.add(i);
        }

        for (final Collection<Integer> view : List.of(values.subSet(20, 40), values.descendingSet(), values.headSet(0))) {
            final CountingCollection counted = new CountingCollection(view);
            final Object expected = view.isEmpty() ? null : view.iterator().next();

            assertEquals(expected, Maps.getByPath(Map.of("values", view), "values[0]"));
            assertEquals(expected, Maps.getByPath(Map.of("values", counted), "values[0]"));
            assertEquals(0, counted.sizeCalls);
            assertEquals(0, counted.sizeElementVisits);
            assertEquals(view.isEmpty() ? 0 : 1, counted.nextCalls);
            assertFalse(Maps.getByPathIfExists(Map.of("values", view), "values[100]").isPresent());
        }

        assertEquals(Integer.valueOf(21), Maps.<Integer> getByPath(Map.of("values", values.subSet(20, 40)), "values[1]"));
    }

    @Test
    void differencePreservesDenseResultsNullPresenceAndCustomCapacity() {
        final CapacityBoundMap<String, Integer> source = new CapacityBoundMap<>(4);
        source.put("a", 1);
        source.put("b", null);
        source.put("c", 3);
        source.put("d", 4);
        final Map<String, Integer> other = new HashMap<>();
        other.put("a", 2);
        other.put("b", 2);
        final Map<String, ?> differences = Maps.difference(source, other);
        assertSame(CapacityBoundMap.class, differences.getClass());
        assertEquals(4, ((CapacityBoundMap<?, ?>) differences).capacity);
        assertEquals(List.of("a", "b", "c", "d"), new ArrayList<>(differences.keySet()));
        assertEquals(4, differences.size());

        final Map<String, Integer> withNull = new HashMap<>();
        withNull.put("b", null);
        assertEquals(3, Maps.difference(source, withNull).size());
        assertEquals(4, Maps.difference(source, Collections.emptyMap()).size());
        assertEquals(4, Maps.difference(source, null).size());
        assertEquals(4, source.size());
    }

    @Test
    void symmetricDifferencePreservesCustomMaximumCapacityAndDisjointEntries() {
        final CapacityBoundMap<String, Integer> source = new CapacityBoundMap<>(4);
        source.put("a", 1);
        source.put("b", null);
        source.put("c", 3);
        source.put("d", 4);
        final LinkedHashMap<String, Integer> other = new LinkedHashMap<>();
        other.put("e", null);
        other.put("f", 6);
        final Map<String, ?> result = Maps.symmetricDifference(source, other);
        assertSame(CapacityBoundMap.class, result.getClass());
        assertEquals(6, ((CapacityBoundMap<?, ?>) result).capacity);
        assertEquals(List.of("a", "b", "c", "d", "e", "f"), new ArrayList<>(result.keySet()));
        assertEquals(6, result.size());
        assertEquals(4, Maps.symmetricDifference(source, null).size());
        assertEquals(2, Maps.symmetricDifference(Collections.emptyMap(), other).size());
        assertEquals(4, source.size());
        assertEquals(2, other.size());
    }

    @Test
    void comparisonSizingDoesNotRepeatLookupsOrValueComparisons() {
        final List<Object> reads = new ArrayList<>();
        final LinkedHashMap<String, CountingValue> source = new LinkedHashMap<>();
        final LinkedHashMap<String, CountingValue> other = new LinkedHashMap<>() {
            private static final long serialVersionUID = 1L;

            @Override
            public CountingValue get(final Object key) {
                reads.add(key);
                return super.get(key);
            }
        };
        final List<String> keys = List.of("c", "b", "a", "d");
        for (int i = 0; i < keys.size(); i++) {
            source.put(keys.get(i), new CountingValue(i));
            other.put(keys.get(i), new CountingValue(-1));
        }

        for (final Supplier<?> operation : List.<Supplier<?>> of(() -> Maps.intersection(source, other), () -> Maps.difference(source, other),
                () -> Maps.symmetricDifference(source, other))) {
            reads.clear();
            CountingValue.comparisons = 0;
            operation.get();
            assertEquals(keys, reads);
            assertEquals(keys.size(), CountingValue.comparisons);
        }
    }

    @Test
    void indexedPathsPreserveNullBoundsNestedTraversalAndLists() {
        final CountingCollection inner = new CountingCollection(Arrays.asList(null, Map.of("name", "found")));
        final CountingCollection outer = new CountingCollection(List.of(inner));
        final Map<String, Object> root = Map.of("nested", outer, "list", Arrays.asList(null, "list-value"));

        assertTrue(Maps.getByPathIfExists(root, "nested[0][0]").isPresent());
        assertNull(Maps.getByPathIfExists(root, "nested[0][0]").get());
        assertEquals("found", Maps.getByPath(root, "nested[0][1].name"));
        assertFalse(Maps.getByPathIfExists(root, "nested[0][2]").isPresent());
        assertFalse(Maps.getByPathIfExists(root, "nested[1]").isPresent());
        assertFalse(Maps.getByPathIfExists(root, "nested[0][0].name").isPresent());
        assertTrue(Maps.getByPathIfExists(root, "list[0]").isPresent());
        assertEquals("list-value", Maps.getByPath(root, "list[1]"));
        assertFalse(Maps.getByPathIfExists(root, "list[2]").isPresent());
        assertFalse(Maps.getByPathIfExists(Map.of("empty", Collections.emptySet()), "empty[0]").isPresent());
    }

    @Test
    void malformedIndexSyntaxDoesNotReadTheCollection() {
        final CountingCollection values = new CountingCollection(List.of("value"));
        final Map<String, Object> root = Map.of("values", values);

        for (final String path : List.of("values[-1]", "values[+1]", "values[]", "values[0][x]", "values[2147483648]", "values[0]junk[0]")) {
            assertFalse(Maps.getByPathIfExists(root, path).isPresent(), path);
        }

        assertEquals(0, values.iteratorCalls);
        assertEquals(0, values.nextCalls);
        assertEquals(0, values.sizeCalls);
    }

    @Test
    void iteratorFailuresAreNotReclassifiedAsMissingPaths() {
        final RuntimeException failure = new NoSuchElementException("iterator failure");
        final Collection<Object> broken = new AbstractCollection<>() {
            @Override
            public int size() {
                return 1;
            }

            @Override
            public Iterator<Object> iterator() {
                return new Iterator<>() {
                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public Object next() {
                        throw failure;
                    }
                };
            }
        };

        assertSame(failure, assertThrows(NoSuchElementException.class, () -> Maps.getByPath(Map.of("broken", broken), "broken[0]")));
    }

    @Test
    @Tag("slow-test")
    void sparseResultsDoNotAllocateInputSizedBackingStorage() {
        final com.sun.management.ThreadMXBean bean = allocationBean();
        final List<Integer> absent = Collections.nCopies(100_000, -1);
        final Map<Integer, Integer> singleton = Map.of(1, 1);
        final HashMap<Integer, Integer> large = new HashMap<>();
        for (int i = 0; i < 100_000; i++) {
            large.put(i, i);
        }

        assertTrue(allocatedBytes(bean, () -> Maps.getValuesIfPresent(singleton, absent)) < 16_384,
                "An empty lookup result must not allocate storage proportional to the requested key count");
        assertTrue(allocatedBytes(bean, () -> Maps.intersection(large, singleton)) < 16_384,
                "An intersection with a tiny other map should use a correspondingly small initial table");
        final HashMap<Integer, Integer> oneChange = new HashMap<>(large);
        oneChange.put(0, -1);
        assertTrue(allocatedBytes(bean, () -> Maps.difference(large, oneChange)) < 16_384,
                "A sparse difference should not speculatively allocate storage for half of its input");
        assertTrue(allocatedBytes(bean, () -> Maps.symmetricDifference(large, oneChange)) < 16_384,
                "A sparse symmetric difference should not speculatively allocate storage for all input entries");
    }

    private static com.sun.management.ThreadMXBean allocationBean() {
        final java.lang.management.ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        assumeTrue(bean instanceof com.sun.management.ThreadMXBean);
        final com.sun.management.ThreadMXBean allocation = (com.sun.management.ThreadMXBean) bean;
        assumeTrue(allocation.isThreadAllocatedMemorySupported());
        if (!allocation.isThreadAllocatedMemoryEnabled()) {
            allocation.setThreadAllocatedMemoryEnabled(true);
        }
        return allocation;
    }

    private static long allocatedBytes(final com.sun.management.ThreadMXBean bean, final Supplier<?> operation) {
        for (int i = 0; i < 10; i++) {
            allocationSink = operation.get();
        }
        final long threadId = Thread.currentThread().threadId();
        final long before = bean.getThreadAllocatedBytes(threadId);
        allocationSink = operation.get();
        return bean.getThreadAllocatedBytes(threadId) - before;
    }
}
