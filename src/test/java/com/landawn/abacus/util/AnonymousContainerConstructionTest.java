package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static testfixtures.SupplierReviewFixtures.anonymousList;
import static testfixtures.SupplierReviewFixtures.anonymousMap;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.Supplier;

@SuppressWarnings({ "rawtypes", "unchecked", "serial" })
public class AnonymousContainerConstructionTest extends com.landawn.abacus.TestBase {
    private static final AtomicInteger OUTER_CALLS = new AtomicInteger();
    private static final AtomicInteger INITIALIZER_CALLS = new AtomicInteger();
    private static boolean failOuter;

    public static class Owner {
        public int seed = 7;

        public Owner() {
            OUTER_CALLS.incrementAndGet();
            if (failOuter) {
                throw new AssertionError("Outer constructor must not run for rejected containers");
            }
        }

        public List<String> list() {
            return new ArrayList<>() {
                {
                    INITIALIZER_CALLS.incrementAndGet();
                    add("x" + seed);
                }
            };
        }

        public class InnerValue {
            public int value() {
                return seed;
            }
        }

        // Declared in a static context, yet its constructor takes an Owner: it extends an inner class of Owner.
        public static InnerValue staticAnonymousInnerValue() {
            return new Owner().new InnerValue() {
            };
        }

        public Map<String, Integer> map() {
            return new HashMap<>() {
                {
                    INITIALIZER_CALLS.incrementAndGet();
                    put("x", seed);
                }
            };
        }

        public Object[] otherContainers(final int captured) {
            return new Object[] { new ArrayList<String>() {
            }, new HashMap<String, Integer>() {
            }, new java.util.HashSet<String>() {
            }, new java.util.ArrayDeque<String>() {
            }, new java.util.TreeSet<String>() {
            }, new TreeMap<String, Integer>() {
            }, new IdentityHashMap<String, Integer>() {
            }, new java.util.concurrent.ConcurrentSkipListMap<String, Integer>() {
            }, new ArrayList<String>(10) {
            }, new HashMap<String, Integer>(10) {
            }, new ArrayList<Integer>() {
                {
                    add(captured);
                }
            }, new HashMap<String, Integer>() {
                {
                    put("n", captured);
                }
            }, new LinkedHashMap<String, Integer>() {
                @Override
                protected boolean removeEldestEntry(final Map.Entry<String, Integer> eldest) {
                    return size() > 2;
                }
            }, new ArrayList<String>() {
                @Override
                public boolean add(final String value) {
                    return super.add(value.toUpperCase());
                }
            } };
        }

        public class Middle {
            public Middle() {
                OUTER_CALLS.incrementAndGet();
            }

            public List<String> list() {
                return new ArrayList<>() {
                };
            }
        }

        public class NamedList extends ArrayList<String> {
            public NamedList() {
                add("named" + seed);
            }
        }

        public class NamedMap extends HashMap<String, Integer> {
            public NamedMap() {
                put("named", seed);
            }
        }

        public Bean bean() {
            return new Bean() {
                {
                    setName("x" + seed);
                }
            };
        }

        public Runnable behavior() {
            return new Runnable() {
                @Override
                public void run() {
                    INITIALIZER_CALLS.incrementAndGet();
                }
            };
        }
    }

    public static class Bean {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    private static class NoDefaultOwner {
        NoDefaultOwner(final int ignored) {
        }

        List<String> list() {
            return new ArrayList<>() {
            };
        }
    }

    @BeforeEach
    public void resetCounters() {
        failOuter = false;
        OUTER_CALLS.set(0);
        INITIALIZER_CALLS.set(0);
    }

    // Class-only recreation must neither execute outer constructors nor rerun initializers. Factory discovery stays lazy.
    private static void assertRejected(final Object container) {
        final Class<?> type = container.getClass();
        assertTrue(type.isAnonymousClass());
        final int outerCalls = OUTER_CALLS.get();
        final int initializerCalls = INITIALIZER_CALLS.get();
        final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> CommonUtil.newInstance(type));
        assertTrue(failure.getMessage().contains("anonymous collection/map"), failure.getMessage());
        assertTrue(failure.getMessage().contains(type.getName()), failure.getMessage());
        if (container instanceof Map) {
            final Class<? extends Map> mapType = (Class<? extends Map>) type;
            // Lookup must succeed; rejection is deferred until the returned factory is invoked.
            final Supplier<? extends Map<Object, Object>> supplier = Suppliers.ofMap(mapType);
            final IntFunction<? extends Map<Object, Object>> creator = IntFunctions.ofMap(mapType);
            // Every factory path reports newInstance's reason, not a generic wrapper message.
            assertEquals(failure.getMessage(), assertThrows(IllegalArgumentException.class, supplier::get).getMessage());
            assertEquals(failure.getMessage(), assertThrows(IllegalArgumentException.class, () -> CommonUtil.newMap(mapType)).getMessage());
            for (final int capacity : new int[] { -1, 0, 1, 100 }) {
                assertEquals(failure.getMessage(), assertThrows(IllegalArgumentException.class, () -> creator.apply(capacity)).getMessage());
                assertEquals(failure.getMessage(), assertThrows(IllegalArgumentException.class, () -> CommonUtil.newMap(mapType, capacity)).getMessage());
            }
        } else {
            final Class<? extends Collection> listType = (Class<? extends Collection>) type;
            // Lookup must succeed; rejection is deferred until the returned factory is invoked.
            final Supplier<? extends Collection<Object>> supplier = Suppliers.ofCollection(listType);
            final IntFunction<? extends Collection<Object>> creator = IntFunctions.ofCollection(listType);
            assertEquals(failure.getMessage(), assertThrows(IllegalArgumentException.class, supplier::get).getMessage());
            assertEquals(failure.getMessage(), assertThrows(IllegalArgumentException.class, () -> CommonUtil.newCollection(listType)).getMessage());
            for (final int capacity : new int[] { -1, 0, 1, 100 }) {
                assertEquals(failure.getMessage(), assertThrows(IllegalArgumentException.class, () -> creator.apply(capacity)).getMessage());
                assertEquals(failure.getMessage(),
                        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newCollection(listType, capacity)).getMessage());
            }
        }
        assertEquals(outerCalls, OUTER_CALLS.get());
        assertEquals(initializerCalls, INITIALIZER_CALLS.get());
    }

    @Test
    public void rejectsInstanceContainersWithoutConstructingThrowingOuter() {
        final Owner owner = new Owner();
        owner.seed = 42;
        final List<String> list = owner.list();
        final Map<String, Integer> map = owner.map();
        final Object[] others = owner.otherContainers(9);
        final List<String> nested = owner.new Middle().list();
        failOuter = true;
        try {
            assertRejected(list);
            assertRejected(map);
            for (final Object other : others) {
                assertRejected(other);
            }
            assertRejected(nested);
        } finally {
            failOuter = false;
        }
        assertEquals(List.of("x42"), list);
        assertEquals(Map.of("x", 42), map);
    }

    private static Object[] staticContainers(final int captured) {
        return new Object[] { new ArrayList<String>() {
            {
                INITIALIZER_CALLS.incrementAndGet();
                add("x");
            }
        }, new HashMap<String, Integer>() {
            {
                INITIALIZER_CALLS.incrementAndGet();
                put("x", 1);
            }
        }, new ArrayList<Integer>() {
            {
                add(captured);
            }
        }, new HashMap<String, Integer>() {
            {
                put("n", captured);
            }
        } };
    }

    @Test
    public void rejectsStaticAndUnconstructibleOuterContainers() {
        for (final Object container : staticContainers(9)) {
            assertRejected(container);
        }
        assertRejected(new NoDefaultOwner(1).list());
    }

    @Test
    public void sharedViewsAreNeverClearedOrPopulated() {
        final Map<String, Integer> backing = new LinkedHashMap<>(Map.of("x", 1));
        final Map<String, Integer> view = new AbstractMap<>() {
            @Override
            public Set<Entry<String, Integer>> entrySet() {
                return backing.entrySet();
            }

            @Override
            public Integer put(final String key, final Integer value) {
                return backing.put(key, value);
            }

            @Override
            public void clear() {
                fail("The backing map must never be cleared");
            }
        };
        final List<String> backingList = new ArrayList<>(List.of("x"));
        final List<String> listView = new java.util.AbstractList<>() {
            @Override
            public String get(final int index) {
                return backingList.get(index);
            }

            @Override
            public int size() {
                return backingList.size();
            }

            @Override
            public void clear() {
                fail("The backing list must never be cleared");
            }
        };
        assertRejected(view);
        assertRejected(listView);
        assertThrows(IllegalArgumentException.class, () -> N.fromJson("{\"y\":2}", view.getClass()));
        assertThrows(IllegalArgumentException.class, () -> N.fromJson("[\"y\"]", listView.getClass()));
        assertEquals(Map.of("x", 1), Maps.filter(view, (k, v) -> true));
        assertEquals(Map.of("x", 1), backing);
        assertEquals(List.of("x"), backingList);
    }

    @Test
    public void preservesNamedContainersAndAnonymousNonContainers() {
        final Owner owner = new Owner();
        owner.seed = 42;
        final Bean original = owner.bean();
        final Bean recreated = CommonUtil.newInstance(original.getClass());
        assertEquals("x7", recreated.getName()); // Existing fresh-enclosing-instance semantics remain unchanged.
        final Bean copied = Beans.copy(original);
        assertEquals(original.getClass(), copied.getClass());
        assertEquals("x42", copied.getName());
        assertEquals("parsed", N.fromJson("{\"name\":\"parsed\"}", original.getClass()).getName());
        final Runnable behavior = CommonUtil.newInstance(owner.behavior().getClass());
        behavior.run();
        assertEquals(1, INITIALIZER_CALLS.get());
        assertEquals(List.of("named7"), CommonUtil.newInstance(Owner.NamedList.class));
        assertEquals(Map.of("named", 7), CommonUtil.newInstance(Owner.NamedMap.class));
        assertEquals(List.of("named7"), Suppliers.ofCollection(Owner.NamedList.class).get());
        assertEquals(Map.of("named", 7), IntFunctions.ofMap(Owner.NamedMap.class).apply(4));
        assertEquals(ArrayList.class, CommonUtil.newInstance(ArrayList.class).getClass());
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newInstance((Class<?>) null));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newInstance(int.class));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newInstance(String[].class));
    }

    // An anonymous class declared in a static method that extends an inner class of its enclosing class has an
    // enclosing-instance constructor, so newInstance still creates it (it is no container) with a new enclosing object.
    @Test
    public void staticContextAnonymousInnerSubclassIsStillCreated() {
        final Class<?> type = Owner.staticAnonymousInnerValue().getClass();
        assertTrue(type.isAnonymousClass());
        final int outerCalls = OUTER_CALLS.get();
        final Object created = CommonUtil.newInstance(type);
        assertSame(type, created.getClass());
        assertEquals(outerCalls + 1, OUTER_CALLS.get());
        assertEquals(7, ((Owner.InnerValue) created).value());
    }

    // BiMap.copyOf, Multiset.toMap and SetMultimap.toImmutableMap share Maps.newTargetMap's family fallback: an anonymous
    // IdentityHashMap source keeps two identity-distinct keys that are equal, instead of merging them (or, for Multiset,
    // throwing because two counts would collapse).
    // Static context: before the fallback fix these anonymous maps fell back to an equals-keyed LinkedHashMap.
    private static <K, V> Map<K, V> staticAnonymousIdentityMap() {
        return new IdentityHashMap<>() {
        };
    }

    @Test
    public void copiesOfAnonymousIdentityMapsKeepIdentityKeys() {
        final String first = new String("k");
        final String second = new String("k");
        final Map<String, Integer> source = staticAnonymousIdentityMap();
        source.put(first, 1);
        source.put(second, 2);
        final BiMap<String, Integer> biMap = BiMap.copyOf(source);
        assertEquals(2, biMap.size());
        assertEquals(1, biMap.get(first));
        assertEquals(2, biMap.get(second));

        final java.util.function.Supplier<Map<String, MutableInt>> backingMapSupplier = AnonymousContainerConstructionTest::staticAnonymousIdentityMap;
        final Multiset<String> multiset = new Multiset<>(backingMapSupplier);
        multiset.add(first);
        multiset.add(second);
        multiset.add(second);
        final Map<String, Integer> counts = multiset.toMap();
        assertInstanceOf(IdentityHashMap.class, counts);
        assertEquals(1, counts.get(first));
        assertEquals(2, counts.get(second));

        final Map<String, Set<Integer>> backing = staticAnonymousIdentityMap();
        final SetMultimap<String, Integer> multimap = SetMultimap.wrap(backing);
        multimap.put(first, 1);
        multimap.put(second, 2);
        final Map<String, ? extends Set<Integer>> immutable = multimap.toImmutableMap();
        assertEquals(2, immutable.size());
        assertEquals(Set.of(1), immutable.get(first));
        assertEquals(Set.of(2), immutable.get(second));
    }

    @Test
    public void explicitFactoriesRemainAnOptInButDoNotEnableReaders() {
        final Class listType = anonymousList().getClass();
        final Class mapType = anonymousMap().getClass();
        registerFixtureFactoriesOnce(listType, mapType);
        final int before = REGISTERED_FACTORY_CALLS.get();
        final var lists = Suppliers.ofCollection(listType);
        final var maps = IntFunctions.ofMap(mapType);
        assertEquals(before, REGISTERED_FACTORY_CALLS.get());
        assertNotSame(lists.get(), lists.get());
        assertEquals(mapType, maps.apply(3).getClass());
        assertEquals(listType, CommonUtil.newCollection(listType, 4).getClass());
        assertEquals(mapType, CommonUtil.newMap(mapType).getClass());
        assertEquals(before + 10, REGISTERED_FACTORY_CALLS.get());
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newInstance(listType));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newInstance(mapType));
        assertThrows(IllegalArgumentException.class, () -> N.fromJson("[\"y\"]", listType));
        assertThrows(IllegalArgumentException.class, () -> N.fromJson("{\"y\":2}", mapType));
        assertEquals(before + 10, REGISTERED_FACTORY_CALLS.get());
    }

    private static final AtomicInteger REGISTERED_FACTORY_CALLS = new AtomicInteger();
    private static boolean fixtureFactoriesRegistered;

    // Registrations are global and permanent (registerFor* returns false once a factory exists), so they are made once per
    // JVM: a rerun of the test in the same JVM must not fail on a second registration. The fixture classes are used by no
    // other test, so no factory can have been cached for them before the first run.
    private static synchronized void registerFixtureFactoriesOnce(final Class listType, final Class mapType) {
        if (!fixtureFactoriesRegistered) {
            assertTrue(Suppliers.registerForCollection(listType, () -> {
                REGISTERED_FACTORY_CALLS.incrementAndGet();
                return anonymousList();
            }));
            assertTrue(Suppliers.registerForMap(mapType, () -> {
                REGISTERED_FACTORY_CALLS.incrementAndGet();
                return anonymousMap();
            }));
            assertTrue(IntFunctions.registerForCollection(listType, n -> {
                REGISTERED_FACTORY_CALLS.addAndGet(n);
                return anonymousList();
            }));
            assertTrue(IntFunctions.registerForMap(mapType, n -> {
                REGISTERED_FACTORY_CALLS.addAndGet(n);
                return anonymousMap();
            }));
            fixtureFactoriesRegistered = true;
        }
    }

    @Test
    public void predefinedImmutableReplacementsStillWork() {
        final var map = new ImmutableMap<>(Map.of("x", 1)) {
        };
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newInstance(map.getClass()));
        assertEquals(HashMap.class, Suppliers.ofMap(map.getClass()).get().getClass());
        assertEquals(HashMap.class, IntFunctions.ofMap(map.getClass()).apply(2).getClass());
        assertEquals(Map.of("x", 1), map);
    }

    @Test
    public void mapFallbackPreservesIdentityKeysIncludingCachedFailures() {
        final Map<String, Integer> source = new IdentityHashMap<>() {
        };
        final String first = new String("equal");
        final String second = new String("equal");
        source.put(first, 1);
        source.put(second, 2);
        source.put(null, 3);
        for (int iteration = 0; iteration < 2; iteration++) {
            final Map<String, Integer> result = Maps.filter(source, (k, v) -> true);
            assertInstanceOf(IdentityHashMap.class, result);
            assertEquals(3, result.size());
            assertEquals(1, result.get(first));
            assertEquals(2, result.get(second));
            assertEquals(3, result.get(null));
            assertNull(result.get(new String("equal")));
        }
        Maps.replaceKeys(source, k -> k);
        assertEquals(3, source.size());
    }

    public static class PrefilledIdentityMap extends IdentityHashMap<String, Integer> {
        public PrefilledIdentityMap() {
            put("initializer", -1);
        }
    }

    @Test
    public void nonemptyNamedMapFallbackAlsoPreservesIdentityKeys() {
        final Map<String, Integer> source = new PrefilledIdentityMap();
        source.clear();
        final String first = new String("equal");
        final String second = new String("equal");
        source.put(first, 1);
        source.put(second, 2);
        // A constructor that succeeds but returns a nonempty map takes a separate fallback path.
        final Map<String, Integer> result = Maps.filter(source, (k, v) -> true);
        assertEquals(IdentityHashMap.class, result.getClass());
        assertEquals(2, result.size());
        assertEquals(1, result.get(first));
        assertEquals(2, result.get(second));
        assertFalse(result.containsKey("initializer"));
        assertEquals(2, source.size());
    }

    private static WeakHashMap<Object, Integer> staticWeakMap() {
        return new WeakHashMap<>() {
        };
    }

    private static ConcurrentHashMap<String, Integer> staticConcurrentMap() {
        return new ConcurrentHashMap<>() {
        };
    }

    @Test
    public void weakMapFallbackPreservesEqualityNullsAndCachedFailures() {
        for (final Map<Object, Integer> source : List.of(new WeakHashMap<Object, Integer>() {
        }, staticWeakMap())) {
            final String key = new String("key");
            source.put(key, 1);
            source.put(null, null);
            try {
                // Repeating the operation covers both the failed construction and cached fallback paths.
                for (int iteration = 0; iteration < 2; iteration++) {
                    final Map<Object, Integer> result = Maps.filter(source, (k, v) -> true);
                    assertEquals(WeakHashMap.class, result.getClass());
                    assertEquals(2, result.size());
                    assertEquals(1, result.get(new String("key")));
                    assertTrue(result.containsKey(null));
                    assertNull(result.get(null));
                }
            } finally {
                Reference.reachabilityFence(key);
            }
        }
    }

    private record WeakKeyFixture(Map<Object, Integer> source, Map<Object, Integer> result, WeakReference<Object> key, WeakReference<Object> control) {
    }

    private static WeakKeyFixture weakKeyFixture() {
        final Object key = new Object();
        final Map<Object, Integer> source = staticWeakMap();
        source.put(key, 1);
        final Map<Object, Integer> result = Maps.filter(source, (k, v) -> true);
        assertEquals(WeakHashMap.class, result.getClass());
        assertEquals(1, result.size());
        return new WeakKeyFixture(source, result, new WeakReference<>(key), new WeakReference<>(new Object()));
    }

    @Test
    public void weakMapFallbackDoesNotKeepKeysAlive() throws InterruptedException {
        final WeakKeyFixture fixture = weakKeyFixture();
        try {
            // Reference clearing and enqueueing are separate steps; allow both maps to expunge their stale entries.
            for (int attempt = 0; attempt < 100 && (fixture.key().get() != null || !fixture.result().isEmpty() || !fixture.source().isEmpty()); attempt++) {
                System.gc();
                Thread.sleep(20);
            }
            // A control reference distinguishes disabled/unavailable GC from accidental strong key retention.
            org.junit.jupiter.api.Assumptions.assumeTrue(fixture.control().get() == null, "GC did not collect the control reference");
            assertNull(fixture.key().get(), "The fallback must not retain weak keys strongly");
            assertTrue(fixture.result().isEmpty());
            assertTrue(fixture.source().isEmpty());
        } finally {
            Reference.reachabilityFence(fixture);
        }
    }

    @Test
    public void concurrentMapFallbackPreservesConcurrentOperationsAndCachedFailures() {
        for (final Map<String, Integer> source : List.of(new ConcurrentHashMap<String, Integer>() {
        }, staticConcurrentMap())) {
            source.put("key", 1);
            for (int iteration = 0; iteration < 2; iteration++) {
                final Map<String, Integer> result = Maps.filter(source, (k, v) -> true);
                assertEquals(ConcurrentHashMap.class, result.getClass());
                assertEquals(Map.of("key", 1), result);
                java.util.stream.IntStream.range(0, 1000).parallel().forEach(i -> result.merge("count", 1, Integer::sum));
                assertEquals(1000, result.get("count"));
                assertThrows(NullPointerException.class, () -> result.put(null, 1));
                assertThrows(NullPointerException.class, () -> result.put("null", null));
                assertEquals(Map.of("key", 1), source);
            }
        }
    }

    public static class PrefilledWeakMap extends WeakHashMap<String, Integer> {
        public PrefilledWeakMap() {
            put("initializer", -1);
        }
    }

    public static class PrefilledConcurrentMap extends ConcurrentHashMap<String, Integer> {
        public PrefilledConcurrentMap() {
            put("initializer", -1);
        }
    }

    @Test
    public void nonemptyNamedFallbackPreservesWeakAndConcurrentFamilies() {
        for (final Map<String, Integer> source : List.of(new PrefilledWeakMap(), new PrefilledConcurrentMap())) {
            source.clear();
            source.put("key", 1);
            final Map<String, Integer> result = Maps.filter(source, (k, v) -> true);
            assertEquals(source instanceof WeakHashMap ? WeakHashMap.class : ConcurrentHashMap.class, result.getClass());
            assertEquals(Map.of("key", 1), result);
            assertEquals(Map.of("key", 1), source);
        }
    }

    @Test
    public void concurrentFallbackDoesNotOverrideNullProducingOrInvertedResults() {
        final Map<String, List<Integer>> columns = new ConcurrentHashMap<>() {
        };
        columns.put("a", java.util.Arrays.asList(1, null));
        columns.put("b", List.of(2));
        final List<Map<String, Integer>> rows = Maps.transpose(columns);
        assertEquals(2, rows.size());
        assertEquals(LinkedHashMap.class, rows.get(1).getClass());
        assertTrue(rows.get(1).containsKey("a"));
        assertNull(rows.get(1).get("a"));

        // Inversion has different keys and deliberately does not inherit weak-key or concurrent-map constraints.
        final Map<String, Integer> weak = new WeakHashMap<>() {
        };
        weak.put("a", null);
        final Map<Integer, String> inverted = Maps.invert(weak);
        assertEquals(LinkedHashMap.class, inverted.getClass());
        assertEquals("a", inverted.get(null));
    }

    @Test
    public void mapFallbackPreservesOrderAndSortedComparator() {
        final Map<String, Integer> ordered = new LinkedHashMap<>() {
        };
        ordered.put("b", 1);
        ordered.put("a", 2);
        final Map<String, Integer> result = Maps.filter(ordered, (k, v) -> true);
        assertEquals(LinkedHashMap.class, result.getClass());
        assertEquals(List.of("b", "a"), new ArrayList<>(result.keySet()));
        final TreeMap<String, Integer> sorted = new TreeMap<>(String.CASE_INSENSITIVE_ORDER) {
        };
        sorted.put("a", 1);
        final TreeMap<String, Integer> sortedResult = (TreeMap<String, Integer>) Maps.filter(sorted, (k, v) -> true);
        assertSame(sorted.comparator(), sortedResult.comparator());
        assertEquals(1, sortedResult.get("A"));
    }
}
