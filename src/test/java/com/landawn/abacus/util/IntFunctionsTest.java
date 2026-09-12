package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractQueue;
import java.util.AbstractSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableArray;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;

public class IntFunctionsTest extends TestBase {

    public static class CustomConcurrentSkipListSet<E> extends ConcurrentSkipListSet<E> {
        private static final long serialVersionUID = 1L;
    }

    public static class CustomConcurrentSkipListMap<K, V> extends ConcurrentSkipListMap<K, V> {
        private static final long serialVersionUID = 1L;
    }

    public static class CustomConcurrentHashMap<K, V> extends ConcurrentHashMap<K, V> {
        private static final long serialVersionUID = 1L;
    }

    public static class CustomIdentityHashMap<K, V> extends IdentityHashMap<K, V> {
        private static final long serialVersionUID = 1L;
    }

    /**
     * A target type no other test asks for, so the per-type cache slot for it is guaranteed empty when the
     * race below starts. It is served by the reflective {@code (int)}-constructor branch, which builds a
     * FRESH creator on every call - unlike the branches that hand back a shared singleton, where threads
     * agreeing on one instance would prove nothing.
     */
    public static class RacedCollection1<E> extends ArrayList<E> {
        private static final long serialVersionUID = 1L;

        public RacedCollection1(final int initialCapacity) {
            super(initialCapacity);
        }
    }

    /** A second fresh target type, so the race is run more than once per JVM. */
    public static class RacedCollection2<E> extends ArrayList<E> {
        private static final long serialVersionUID = 1L;

        public RacedCollection2(final int initialCapacity) {
            super(initialCapacity);
        }
    }

    public static class CountingCollection<E> extends ArrayList<E> {
        private static final long serialVersionUID = 1L;
        static int constructorCalls;

        public CountingCollection(final int initialCapacity) {
            super(initialCapacity);
            constructorCalls++;
        }
    }

    public static class CountingMap<K, V> extends HashMap<K, V> {
        private static final long serialVersionUID = 1L;
        static int constructorCalls;

        public CountingMap(final int initialCapacity) {
            super(initialCapacity);
            constructorCalls++;
        }
    }

    public static class CountingNoArgCollection<E> extends ArrayList<E> {
        private static final long serialVersionUID = 1L;
        static int constructorCalls;

        public CountingNoArgCollection() {
            constructorCalls++;
        }
    }

    public static class CountingNoArgMap<K, V> extends HashMap<K, V> {
        private static final long serialVersionUID = 1L;
        static int constructorCalls;

        public CountingNoArgMap() {
            constructorCalls++;
        }
    }

    public abstract static class CustomAbstractSortedSet<E> extends AbstractSet<E> implements SortedSet<E> {
        private static final long serialVersionUID = 1L;
    }

    public abstract static class CustomAbstractSortedMap<K, V> extends AbstractMap<K, V> implements SortedMap<K, V> {
        private static final long serialVersionUID = 1L;
    }

    @Test
    public void testOf() {
        assertEquals("Size: 10", IntFunctions.of(size -> "Size: " + size).apply(10));
    }

    @Test
    public void testOfPrimitiveArrays() {
        boolean[] booleans = IntFunctions.ofBooleanArray().apply(5);
        assertEquals(5, booleans.length);
        assertFalse(booleans[0]);
        assertEquals(0, IntFunctions.ofBooleanArray().apply(0).length);

        char[] chars = IntFunctions.ofCharArray().apply(5);
        assertEquals(5, chars.length);
        assertEquals('\u0000', chars[0]);
        assertEquals(0, IntFunctions.ofCharArray().apply(0).length);

        byte[] bytes = IntFunctions.ofByteArray().apply(5);
        assertEquals(5, bytes.length);
        assertEquals(0, bytes[0]);
        assertEquals(0, IntFunctions.ofByteArray().apply(0).length);

        short[] shorts = IntFunctions.ofShortArray().apply(5);
        assertEquals(5, shorts.length);
        assertEquals(0, shorts[0]);
        assertEquals(0, IntFunctions.ofShortArray().apply(0).length);

        int[] ints = IntFunctions.ofIntArray().apply(5);
        assertEquals(5, ints.length);
        assertEquals(0, ints[0]);
        assertEquals(0, IntFunctions.ofIntArray().apply(0).length);

        long[] longs = IntFunctions.ofLongArray().apply(5);
        assertEquals(5, longs.length);
        assertEquals(0L, longs[0]);
        assertEquals(0, IntFunctions.ofLongArray().apply(0).length);

        float[] floats = IntFunctions.ofFloatArray().apply(5);
        assertEquals(5, floats.length);
        assertEquals(0.0f, floats[0]);
        assertEquals(0, IntFunctions.ofFloatArray().apply(0).length);

        double[] doubles = IntFunctions.ofDoubleArray().apply(5);
        assertEquals(5, doubles.length);
        assertEquals(0.0, doubles[0]);
        assertEquals(0, IntFunctions.ofDoubleArray().apply(0).length);

        String[] strings = IntFunctions.ofStringArray().apply(5);
        assertEquals(5, strings.length);
        assertEquals(null, strings[0]);
        assertEquals(0, IntFunctions.ofStringArray().apply(0).length);

        Object[] objects = IntFunctions.ofObjectArray().apply(5);
        assertEquals(5, objects.length);
        assertEquals(null, objects[0]);
        assertEquals(0, IntFunctions.ofObjectArray().apply(0).length);
    }

    @Test
    public void testOfPrimitiveLists() {
        assertEmpty(IntFunctions.ofBooleanList().apply(10));
        assertEmpty(IntFunctions.ofBooleanList().apply(0));
        assertEmpty(IntFunctions.ofCharList().apply(10));
        assertEmpty(IntFunctions.ofByteList().apply(10));
        assertEmpty(IntFunctions.ofShortList().apply(10));
        assertEmpty(IntFunctions.ofIntList().apply(10));
        assertEmpty(IntFunctions.ofLongList().apply(10));
        assertEmpty(IntFunctions.ofFloatList().apply(10));
        assertEmpty(IntFunctions.ofDoubleList().apply(0));
    }

    @Test
    public void testOfCollectionFactories() {
        assertCreated(IntFunctions.ofList(), ArrayList.class);
        assertEmpty(IntFunctions.<String> ofList().apply(0));
        assertCreated(IntFunctions.ofLinkedList(), LinkedList.class);
        assertCreated(IntFunctions.ofSet(), HashSet.class);
        assertCreated(IntFunctions.ofLinkedHashSet(), LinkedHashSet.class);
        assertCreated(IntFunctions.ofSortedSet(), TreeSet.class);
        assertCreated(IntFunctions.ofNavigableSet(), TreeSet.class);
        assertCreated(IntFunctions.ofTreeSet(), TreeSet.class);
        assertCreated(IntFunctions.ofQueue(), LinkedList.class);
        assertCreated(IntFunctions.ofDeque(), LinkedList.class);
        assertCreated(IntFunctions.ofArrayDeque(), ArrayDeque.class);
        assertCreated(IntFunctions.ofConcurrentLinkedQueue(), ConcurrentLinkedQueue.class);

        LinkedBlockingQueue<String> lbq = IntFunctions.<String> ofLinkedBlockingQueue().apply(10);
        assertEquals(0, lbq.size());
        assertEquals(10, lbq.remainingCapacity());
        assertEquals(100, IntFunctions.<String> ofLinkedBlockingQueue().apply(100).remainingCapacity());

        ArrayBlockingQueue<String> abq = IntFunctions.<String> ofArrayBlockingQueue().apply(10);
        assertEquals(0, abq.size());
        assertEquals(10, abq.remainingCapacity());
        assertEquals(100, IntFunctions.<String> ofArrayBlockingQueue().apply(100).remainingCapacity());

        LinkedBlockingDeque<String> lbd = IntFunctions.<String> ofLinkedBlockingDeque().apply(10);
        assertEquals(0, lbd.size());
        assertEquals(10, lbd.remainingCapacity());
        assertEquals(100, IntFunctions.<String> ofLinkedBlockingDeque().apply(100).remainingCapacity());

        assertEmpty(IntFunctions.<String> ofPriorityQueue().apply(10));
        assertEmpty(IntFunctions.<String> ofPriorityQueue().apply(1));
    }

    @Test
    public void testOfMapFactories() {
        assertCreated(IntFunctions.ofMap(), HashMap.class);
        assertEmpty(IntFunctions.<String, Integer> ofMap().apply(0));
        assertCreated(IntFunctions.ofLinkedHashMap(), LinkedHashMap.class);
        assertCreated(IntFunctions.ofIdentityHashMap(), IdentityHashMap.class);
        assertCreated(IntFunctions.ofSortedMap(), TreeMap.class);
        assertCreated(IntFunctions.ofNavigableMap(), TreeMap.class);
        assertCreated(IntFunctions.ofTreeMap(), TreeMap.class);
        assertCreated(IntFunctions.ofConcurrentMap(), ConcurrentHashMap.class);
        assertCreated(IntFunctions.ofConcurrentHashMap(), ConcurrentHashMap.class);
        assertCreated(IntFunctions.ofBiMap(), BiMap.class);
        assertEmpty(IntFunctions.<String> ofMultiset().apply(10));
        assertEmpty(IntFunctions.<String> ofMultiset().apply(0));
        assertEquals(0, IntFunctions.<String, Integer> ofListMultimap().apply(10).totalValueCount());
        assertEquals(0, IntFunctions.<String, Integer> ofListMultimap().apply(0).totalValueCount());
        assertEquals(0, IntFunctions.<String, Integer> ofSetMultimap().apply(10).totalValueCount());
        assertEquals(0, IntFunctions.<String, Integer> ofSetMultimap().apply(0).totalValueCount());
    }

    @Test
    public void testOfListMultimapBackedByPlainHashMap() {
        ListMultimap<Integer, Integer> multimap = IntFunctions.<Integer, Integer> ofListMultimap().apply(16);
        for (int k = 8; k >= 0; k--) {
            multimap.put(k, k);
        }
        List<Integer> keyOrder = new ArrayList<>(multimap.keySet());
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8), keyOrder);

        ListMultimap<Integer, Integer> reference = CommonUtil.newListMultimap();
        for (int k = 8; k >= 0; k--) {
            reference.put(k, k);
        }
        assertEquals(new ArrayList<>(reference.keySet()), keyOrder);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testOfCollection() {
        assertCreated(IntFunctions.ofCollection(Collection.class), ArrayList.class);
        assertCreated(IntFunctions.ofCollection(AbstractCollection.class), ArrayList.class);
        assertCreated(IntFunctions.ofCollection(List.class), ArrayList.class);
        assertCreated(IntFunctions.ofCollection(AbstractList.class), ArrayList.class);
        assertCreated(IntFunctions.ofCollection(ImmutableList.class), ArrayList.class);
        assertCreated(IntFunctions.ofCollection(Set.class), HashSet.class);
        assertCreated(IntFunctions.ofCollection(AbstractSet.class), HashSet.class);
        assertCreated(IntFunctions.ofCollection(ImmutableSet.class), HashSet.class);
        assertCreated(IntFunctions.ofCollection(Queue.class), LinkedList.class);
        assertCreated(IntFunctions.ofCollection(AbstractQueue.class), LinkedList.class);
        assertCreated(IntFunctions.ofCollection(Deque.class), LinkedList.class);
        assertCreated(IntFunctions.ofCollection(BlockingQueue.class), LinkedBlockingQueue.class);
        assertCreated(IntFunctions.ofCollection(BlockingDeque.class), LinkedBlockingDeque.class);
        assertCreated(IntFunctions.ofCollection(ArrayList.class), ArrayList.class);
        assertCreated(IntFunctions.ofCollection(LinkedList.class), LinkedList.class);
        assertCreated(IntFunctions.ofCollection(HashSet.class), HashSet.class);
        assertCreated(IntFunctions.ofCollection(LinkedHashSet.class), LinkedHashSet.class);
        assertCreated(IntFunctions.ofCollection(TreeSet.class), TreeSet.class);
        assertCreated(IntFunctions.ofCollection(SortedSet.class), TreeSet.class);
        assertCreated(IntFunctions.ofCollection(NavigableSet.class), TreeSet.class);
        assertCreated(IntFunctions.ofCollection(PriorityQueue.class), PriorityQueue.class);
        assertCreated(IntFunctions.ofCollection(ArrayDeque.class), ArrayDeque.class);
        assertCreated(IntFunctions.ofCollection(ConcurrentLinkedQueue.class), ConcurrentLinkedQueue.class);
        assertCreated(IntFunctions.ofCollection(LinkedBlockingQueue.class), LinkedBlockingQueue.class);
        assertCreated(IntFunctions.ofCollection(ArrayBlockingQueue.class), ArrayBlockingQueue.class);
        assertCreated(IntFunctions.ofCollection(LinkedBlockingDeque.class), LinkedBlockingDeque.class);

        IntFunction<? extends Collection<String>> cached = IntFunctions.ofCollection(ArrayList.class);
        assertSame(cached, IntFunctions.ofCollection(ArrayList.class));
        assertNotSame(cached, IntFunctions.ofCollection(LinkedList.class));

        assertThrows(IllegalArgumentException.class, () -> IntFunctions.ofCollection((Class) String.class));
        assertThrows(IllegalArgumentException.class, () -> IntFunctions.ofCollection(null));
        assertThrows(IllegalArgumentException.class, () -> IntFunctions.ofCollection(CustomAbstractSortedSet.class));
        assertEquals(ConcurrentSkipListSet.class, IntFunctions.ofCollection(ConcurrentSkipListSet.class).apply(8).getClass());
    }

    @Test
    public void testOfCollectionAndOfMap_immutableSortedFamiliesFallBackToSortedMutableTypes() {
        // ImmutableSortedSet/ImmutableNavigableSet are ImmutableSets and ImmutableSortedMap/
        // ImmutableNavigableMap are ImmutableMaps, so they must not fall through to HashSet/HashMap.
        // Mirrors SupplierSubtypeTest.immutableSortedFamiliesFallBackToSortedMutableTypes.
        assertEquals(TreeSet.class, IntFunctions.ofCollection(ImmutableSortedSet.class).apply(8).getClass());
        assertEquals(TreeSet.class, IntFunctions.ofCollection(ImmutableNavigableSet.class).apply(8).getClass());
        assertEquals(TreeMap.class, IntFunctions.ofMap(ImmutableSortedMap.class).apply(8).getClass());
        assertEquals(TreeMap.class, IntFunctions.ofMap(ImmutableNavigableMap.class).apply(8).getClass());

        // The unsorted immutable families keep their existing mappings.
        assertEquals(ArrayList.class, IntFunctions.ofCollection(ImmutableList.class).apply(8).getClass());
        assertEquals(HashSet.class, IntFunctions.ofCollection(ImmutableSet.class).apply(8).getClass());
        assertEquals(HashMap.class, IntFunctions.ofMap(ImmutableMap.class).apply(8).getClass());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testOfCollection_queueAndDequeTargetsArePublishedToThePerTypeCache() throws Exception {
        // The javadoc promises the creator is "cached per target type for performance". Every branch hands
        // back the same shared singleton, so an empty cache slot has no other observable effect and the slot
        // itself has to be read reflectively.
        final Field poolField = IntFunctions.class.getDeclaredField("collectionCreatorPool");
        poolField.setAccessible(true);
        final ClassValue<?> pool = (ClassValue<?>) poolField.get(null);

        final Class[] targetTypes = { Queue.class, Deque.class, AbstractQueue.class, BlockingQueue.class, LinkedBlockingQueue.class,
                ArrayBlockingQueue.class, BlockingDeque.class, LinkedBlockingDeque.class, ConcurrentLinkedQueue.class, PriorityQueue.class };

        for (final Class targetType : targetTypes) {
            final IntFunction<?> creator = IntFunctions.ofCollection(targetType);
            final AtomicReference<?> slot = (AtomicReference<?>) pool.get(targetType);
            assertSame(creator, slot.get(), "no creator published to the cache for " + targetType.getName());
            assertSame(creator, IntFunctions.ofCollection(targetType));
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testOfCollection_concurrentFirstCallSharesTheOneWinningCreator() throws Exception {
        // Pins the claim the publish carries - "Publish once so concurrent discovery and registration share the
        // same winning factory" - i.e. the compareAndSet loser branch that re-reads the slot. The target types
        // below take the reflective branch, which builds a fresh creator per call, so a dropped loser branch
        // would hand the losing threads a creator that is NOT the one in the cache.
        final Field poolField = IntFunctions.class.getDeclaredField("collectionCreatorPool");
        poolField.setAccessible(true);
        final ClassValue<?> pool = (ClassValue<?>) poolField.get(null);

        final Class[] freshTargetTypes = { RacedCollection1.class, RacedCollection2.class };
        final int threadCount = 64;

        for (final Class targetType : freshTargetTypes) {
            final CyclicBarrier allReady = new CyclicBarrier(threadCount);
            final List<IntFunction<?>> seen = Collections.synchronizedList(new ArrayList<>());
            final List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
            final Thread[] threads = new Thread[threadCount];

            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(() -> {
                    try {
                        allReady.await();   // release every thread onto the very first call together
                        seen.add(IntFunctions.ofCollection(targetType));
                    } catch (final Throwable e) {
                        failures.add(e);
                    }
                }, "race-" + targetType.getSimpleName() + "-" + i);
            }

            for (final Thread thread : threads) {
                thread.start();
            }

            for (final Thread thread : threads) {
                thread.join(60_000);
            }

            assertTrue(failures.isEmpty(), () -> String.valueOf(failures));
            assertEquals(threadCount, seen.size());

            final IntFunction<?> winner = seen.get(0);

            for (final IntFunction<?> creator : seen) {
                assertSame(winner, creator, "threads racing the first ofCollection(" + targetType.getName() + ") disagreed");
            }

            assertSame(winner, ((AtomicReference<?>) pool.get(targetType)).get(), "the winning creator was not the published one");
            assertSame(winner, IntFunctions.ofCollection(targetType), "a later call must reuse the published creator");
            assertEquals(targetType, winner.apply(3).getClass());
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testOfCollection_documentedTypeSubstitutions() {
        // Pins the documented caveat: the creator is NOT always an instance of the requested target type.
        final Class[] substituted = { AbstractQueue.class, ImmutableList.class, ImmutableSet.class, ImmutableSortedSet.class,
                ImmutableNavigableSet.class };

        for (final Class targetType : substituted) {
            final Object created = IntFunctions.ofCollection(targetType).apply(4);
            assertFalse(targetType.isInstance(created), targetType.getName() + " -> " + created.getClass().getName());
        }

        // AbstractQueue is served by the LinkedList (Deque) creator, which does not extend AbstractQueue.
        assertEquals(LinkedList.class, IntFunctions.ofCollection(AbstractQueue.class).apply(4).getClass());

        // An Immutable* target yields a plain MUTABLE JDK collection ...
        final Collection<String> mutable = IntFunctions.<String> ofCollection(ImmutableSet.class).apply(4);
        assertEquals(HashSet.class, mutable.getClass());
        assertTrue(mutable.add("mutated"));
        assertEquals(1, mutable.size());

        // ... so casting the result to the requested Immutable* type throws.
        final Object asImmutableList = IntFunctions.ofCollection(ImmutableList.class).apply(4);
        assertThrows(ClassCastException.class, () -> ImmutableList.class.cast(asImmutableList));

        // ImmutableCollection itself has no creator and is rejected.
        assertThrows(IllegalArgumentException.class, () -> IntFunctions.ofCollection(ImmutableCollection.class));
    }

    @Test
    public void testBoundedCapacityFactoriesRejectZeroCapacity() {
        // Pins the documented edge of the four bounded factories: the JDK constructors require capacity >= 1.
        assertThrows(IllegalArgumentException.class, () -> IntFunctions.ofLinkedBlockingQueue().apply(0));
        assertThrows(IllegalArgumentException.class, () -> IntFunctions.ofArrayBlockingQueue().apply(0));
        assertThrows(IllegalArgumentException.class, () -> IntFunctions.ofLinkedBlockingDeque().apply(0));
        assertThrows(IllegalArgumentException.class, () -> IntFunctions.ofPriorityQueue().apply(0));

        // Reachable through ordinary API: an empty range asks the factory for capacity 0.
        assertThrows(IllegalArgumentException.class, () -> IntList.of(1, 2, 3).toCollection(0, 0, IntFunctions.ofPriorityQueue()));

        // Contrast: the unbounded / capacity-ignoring factories accept 0.
        assertEquals(0, IntFunctions.ofList().apply(0).size());
        assertEquals(0, IntFunctions.ofArrayDeque().apply(0).size());
        assertEquals(0, IntFunctions.ofConcurrentLinkedQueue().apply(0).size());
        assertEquals(0, IntFunctions.ofSet().apply(0).size());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testOfMap() {
        assertCreated(IntFunctions.ofMap(Map.class), HashMap.class);
        assertCreated(IntFunctions.ofMap(AbstractMap.class), HashMap.class);
        assertCreated(IntFunctions.ofMap(ImmutableMap.class), HashMap.class);
        assertCreated(IntFunctions.ofMap(SortedMap.class), TreeMap.class);
        assertCreated(IntFunctions.ofMap(NavigableMap.class), TreeMap.class);
        assertCreated(IntFunctions.ofMap(ConcurrentMap.class), ConcurrentHashMap.class);
        assertCreated(IntFunctions.ofMap(HashMap.class), HashMap.class);
        assertCreated(IntFunctions.ofMap(LinkedHashMap.class), LinkedHashMap.class);
        assertCreated(IntFunctions.ofMap(TreeMap.class), TreeMap.class);
        assertCreated(IntFunctions.ofMap(IdentityHashMap.class), IdentityHashMap.class);
        assertCreated(IntFunctions.ofMap(ConcurrentHashMap.class), ConcurrentHashMap.class);
        assertCreated(IntFunctions.ofMap(BiMap.class), BiMap.class);

        IntFunction<? extends Map<String, Integer>> cached = IntFunctions.ofMap(HashMap.class);
        assertSame(cached, IntFunctions.ofMap(HashMap.class));
        assertNotSame(cached, IntFunctions.ofMap(LinkedHashMap.class));

        assertThrows(IllegalArgumentException.class, () -> IntFunctions.ofMap((Class) String.class));
        assertThrows(IllegalArgumentException.class, () -> IntFunctions.ofMap(null));
        assertThrows(IllegalArgumentException.class, () -> IntFunctions.ofMap(CustomAbstractSortedMap.class));
        assertEquals(ConcurrentSkipListMap.class, IntFunctions.ofMap(ConcurrentSkipListMap.class).apply(8).getClass());
    }

    @Test
    public void testCustomTypePreservation() {
        assertTrue(IntFunctions.<String> ofCollection(CustomConcurrentSkipListSet.class).apply(10) instanceof CustomConcurrentSkipListSet);
        assertTrue(IntFunctions.<String, Integer> ofMap(CustomConcurrentSkipListMap.class).apply(10) instanceof CustomConcurrentSkipListMap);
        assertTrue(IntFunctions.<String, Integer> ofMap(CustomConcurrentHashMap.class).apply(10) instanceof CustomConcurrentHashMap);
        assertTrue(IntFunctions.<String, Integer> ofMap(CustomIdentityHashMap.class).apply(10) instanceof CustomIdentityHashMap);
    }

    @Test
    public void testDynamicFactoriesDoNotProbeByInstantiation() {
        CountingCollection.constructorCalls = 0;
        IntFunction<? extends Collection<String>> collectionFactory = IntFunctions.ofCollection(CountingCollection.class);
        assertEquals(0, CountingCollection.constructorCalls);
        assertTrue(collectionFactory.apply(7) instanceof CountingCollection);
        assertEquals(1, CountingCollection.constructorCalls);

        CountingMap.constructorCalls = 0;
        IntFunction<? extends Map<String, Integer>> mapFactory = IntFunctions.ofMap(CountingMap.class);
        assertEquals(0, CountingMap.constructorCalls);
        assertTrue(mapFactory.apply(7) instanceof CountingMap);
        assertEquals(1, CountingMap.constructorCalls);

        CountingNoArgCollection.constructorCalls = 0;
        IntFunction<? extends Collection<String>> noArgCollectionFactory = IntFunctions.ofCollection(CountingNoArgCollection.class);
        assertEquals(0, CountingNoArgCollection.constructorCalls);
        assertTrue(noArgCollectionFactory.apply(7) instanceof CountingNoArgCollection);
        assertEquals(1, CountingNoArgCollection.constructorCalls);

        CountingNoArgMap.constructorCalls = 0;
        IntFunction<? extends Map<String, Integer>> noArgMapFactory = IntFunctions.ofMap(CountingNoArgMap.class);
        assertEquals(0, CountingNoArgMap.constructorCalls);
        assertTrue(noArgMapFactory.apply(7) instanceof CountingNoArgMap);
        assertEquals(1, CountingNoArgMap.constructorCalls);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testRegister() {
        assertThrows(IllegalArgumentException.class, () -> IntFunctions.registerForCollection(ArrayList.class, size -> new ArrayList<>(size)));
        assertThrows(IllegalArgumentException.class, () -> IntFunctions.registerForCollection(null, size -> new ArrayList<>(size)));
        assertEquals("'creator' cannot be null",
                assertThrows(IllegalArgumentException.class, () -> IntFunctions.registerForCollection(ArrayList.class, null)).getMessage());
        assertThrows(IllegalArgumentException.class, () -> IntFunctions.registerForCollection((Class) String.class, size -> new ArrayList<>()));

        assertThrows(IllegalArgumentException.class, () -> IntFunctions.registerForMap(HashMap.class, size -> new HashMap<>(size)));
        assertThrows(IllegalArgumentException.class, () -> IntFunctions.registerForMap(null, size -> new HashMap<>(size)));
        assertEquals("'creator' cannot be null",
                assertThrows(IllegalArgumentException.class, () -> IntFunctions.registerForMap(HashMap.class, null)).getMessage());
        assertThrows(IllegalArgumentException.class, () -> IntFunctions.registerForMap((Class) String.class, size -> new HashMap<>()));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testOfDisposableArray() {
        IntFunction<DisposableObjArray> func = IntFunctions.ofDisposableArray();
        DisposableObjArray array1 = func.apply(10);
        assertNotNull(array1);
        assertSame(array1, func.apply(20));
        assertNotSame(array1, IntFunctions.ofDisposableArray().apply(15));

        IntFunction<DisposableArray<String>> typed = IntFunctions.ofDisposableArray(String.class);
        DisposableArray<String> typed1 = typed.apply(10);
        assertNotNull(typed1);
        assertSame(typed1, typed.apply(20));
        assertNotSame(typed1, IntFunctions.ofDisposableArray(String.class).apply(15));
        assertNotNull(IntFunctions.ofDisposableArray(Integer.class).apply(5));

        assertThrows(IllegalArgumentException.class, () -> IntFunctions.ofDisposableArray(null));
        assertThrows(IllegalArgumentException.class, () -> IntFunctions.ofDisposableArray((Class) int.class));
    }

    @Test
    public void testOfImmutableFactories() {
        assertThrows(UnsupportedOperationException.class, IntFunctions::ofImmutableList);
        assertThrows(UnsupportedOperationException.class, IntFunctions::ofImmutableSet);
        assertThrows(UnsupportedOperationException.class, IntFunctions::ofImmutableMap);
    }

    private static void assertEmpty(BooleanList list) {
        assertEquals(0, list.size());
    }

    private static void assertEmpty(CharList list) {
        assertEquals(0, list.size());
    }

    private static void assertEmpty(ByteList list) {
        assertEquals(0, list.size());
    }

    private static void assertEmpty(ShortList list) {
        assertEquals(0, list.size());
    }

    private static void assertEmpty(IntList list) {
        assertEquals(0, list.size());
    }

    private static void assertEmpty(LongList list) {
        assertEquals(0, list.size());
    }

    private static void assertEmpty(FloatList list) {
        assertEquals(0, list.size());
    }

    private static void assertEmpty(DoubleList list) {
        assertEquals(0, list.size());
    }

    private static void assertEmpty(Collection<?> collection) {
        assertEquals(0, collection.size());
    }

    private static void assertEmpty(Map<?, ?> map) {
        assertEquals(0, map.size());
    }

    private static void assertEmpty(Multiset<?> multiset) {
        assertEquals(0, multiset.size());
    }

    private static void assertCreated(IntFunction<?> func, Class<?> type) {
        Object created = func.apply(10);
        assertTrue(type.isInstance(created), created.getClass().getName());
        if (created instanceof Collection) {
            assertEquals(0, ((Collection<?>) created).size());
        } else if (created instanceof Map) {
            assertEquals(0, ((Map<?, ?>) created).size());
        }
    }
}
