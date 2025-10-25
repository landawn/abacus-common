/*
 * Copyright (c) 2025, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractQueue;
import java.util.AbstractSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.EnumMap;
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
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.SequentialOnly;
import com.landawn.abacus.annotation.Stateful;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableArray;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.function.IntFunction;

/**
 * Utility class providing static methods to create various types of IntFunction instances for collections, maps, arrays, and other commonly used objects.
 *
 * <p>This class serves as a centralized factory for creating {@link IntFunction} instances that generate
 * different types of data structures and objects based on an integer capacity parameter. It provides
 * convenient methods for creating functions that instantiate collections (List, Set, Queue, etc.), maps,
 * primitive arrays, and specialized list types.</p>
 *
 * <p>The class is designed as a sealed abstract class that permits only the nested {@code Factory} class
 * to extend it, ensuring controlled inheritance and maintaining the utility class pattern. All methods
 * are static and thread-safe.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Functions for all major collection types (ArrayList, LinkedList, HashSet, TreeSet, etc.)</li>
 *   <li>Functions for various map implementations (HashMap, TreeMap, ConcurrentHashMap, etc.)</li>
 *   <li>Functions for primitive arrays (boolean[], int[], String[], etc.)</li>
 *   <li>Functions for specialized primitive list types (IntList, BooleanList, etc.)</li>
 *   <li>Functions for concurrent and blocking collections</li>
 *   <li>Dynamic type support through reflection for custom implementations</li>
 *   <li>Registration mechanism for custom collection and map creators</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create a function for ArrayList with initial capacity
 * IntFunction<List<String>> listCreator = IntFunctions.ofList();
 * List<String> list = listCreator.apply(100); // ArrayList with capacity 100
 *
 * // Create a function for TreeSet (capacity ignored)
 * IntFunction<Set<Integer>> setCreator = IntFunctions.ofSet();
 * Set<Integer> set = setCreator.apply(50);
 *
 * // Create a function for HashMap with initial capacity
 * IntFunction<Map<String, Object>> mapCreator = IntFunctions.ofMap();
 * Map<String, Object> map = mapCreator.apply(200); // HashMap with capacity 200
 *
 * // Create a function for primitive arrays
 * IntFunction<int[]> arrayCreator = IntFunctions.ofIntArray();
 * int[] array = arrayCreator.apply(1000); // int array with length 1000
 *
 * // Dynamic collection type creation
 * IntFunction<? extends Collection<String>> dynamicCreator = 
 *     IntFunctions.ofCollection(LinkedHashSet.class);
 * Collection<String> collection = dynamicCreator.apply(75);
 * }</pre>
 *
 * <p>The class uses internal caching to optimize function creation for frequently requested types
 * and provides registration mechanisms for custom implementations that are not built-in.</p>
 *
 * <p><b>Thread Safety:</b> All methods and returned functions are thread-safe unless explicitly
 * marked otherwise (e.g., stateful functions marked with {@code @Stateful}).</p>
 *
 * @since 1.0
 * @see java.util.function.IntFunction
 * @see java.util.Collection
 * @see java.util.Map
 */
@SuppressWarnings({ "java:S1694" })
public final class IntFunctions {
    /** The Constant BOOLEAN_ARRAY. */
    private static final IntFunction<boolean[]> BOOLEAN_ARRAY = boolean[]::new;

    /** The Constant CHAR_ARRAY. */
    private static final IntFunction<char[]> CHAR_ARRAY = char[]::new;

    /** The Constant BYTE_ARRAY. */
    private static final IntFunction<byte[]> BYTE_ARRAY = byte[]::new;

    /** The Constant SHORT_ARRAY. */
    private static final IntFunction<short[]> SHORT_ARRAY = short[]::new;

    /** The Constant INT_ARRAY. */
    private static final IntFunction<int[]> INT_ARRAY = int[]::new;

    /** The Constant LONG_ARRAY. */
    private static final IntFunction<long[]> LONG_ARRAY = long[]::new;

    /** The Constant FLOAT_ARRAY. */
    private static final IntFunction<float[]> FLOAT_ARRAY = float[]::new;

    /** The Constant DOUBLE_ARRAY. */
    private static final IntFunction<double[]> DOUBLE_ARRAY = double[]::new;

    /** The Constant STRING_ARRAY. */
    private static final IntFunction<String[]> STRING_ARRAY = String[]::new;

    /** The Constant OBJECT_ARRAY. */
    private static final IntFunction<Object[]> OBJECT_ARRAY = Object[]::new;

    /** The Constant BOOLEAN_LIST. */
    private static final IntFunction<BooleanList> BOOLEAN_LIST = BooleanList::new;

    /** The Constant CHAR_LIST. */
    private static final IntFunction<CharList> CHAR_LIST = CharList::new;

    /** The Constant BYTE_LIST. */
    private static final IntFunction<ByteList> BYTE_LIST = ByteList::new;

    /** The Constant SHORT_LIST. */
    private static final IntFunction<ShortList> SHORT_LIST = ShortList::new;

    /** The Constant INT_LIST. */
    private static final IntFunction<IntList> INT_LIST = IntList::new;

    /** The Constant LONG_LIST. */
    private static final IntFunction<LongList> LONG_LIST = LongList::new;

    /** The Constant FLOAT_LIST. */
    private static final IntFunction<FloatList> FLOAT_LIST = FloatList::new;

    /** The Constant DOUBLE_LIST. */
    private static final IntFunction<DoubleList> DOUBLE_LIST = DoubleList::new;

    /** The Constant LIST_FACTORY. */
    @SuppressWarnings("rawtypes")
    private static final IntFunction<? super List> LIST_FACTORY = ArrayList::new;

    /** The Constant LINKED_LIST_FACTORY. */
    @SuppressWarnings("rawtypes")
    private static final IntFunction<? super LinkedList> LINKED_LIST_FACTORY = len -> new LinkedList<>();

    /** The Constant SET_FACTORY. */
    @SuppressWarnings("rawtypes")
    private static final IntFunction<? super Set> SET_FACTORY = N::newHashSet;

    /** The Constant LINKED_HASH_SET_FACTORY. */
    @SuppressWarnings("rawtypes")
    private static final IntFunction<? super Set> LINKED_HASH_SET_FACTORY = N::newLinkedHashSet;

    /** The Constant TREE_SET_FACTORY. */
    @SuppressWarnings("rawtypes")
    private static final IntFunction<? super TreeSet> TREE_SET_FACTORY = len -> new TreeSet<>();

    /** The Constant QUEUE_FACTORY. */
    @SuppressWarnings("rawtypes")
    private static final IntFunction<? super Queue> QUEUE_FACTORY = len -> new LinkedList();

    /** The Constant DEQUE_FACTORY. */
    @SuppressWarnings("rawtypes")
    private static final IntFunction<? super Deque> DEQUE_FACTORY = len -> new LinkedList();

    /** The Constant ARRAY_DEQUE_FACTORY. */
    @SuppressWarnings("rawtypes")
    private static final IntFunction<? super ArrayDeque> ARRAY_DEQUE_FACTORY = ArrayDeque::new;

    /** The Constant LINKED_BLOCKING_QUEUE_FACTORY. */
    @SuppressWarnings("rawtypes")
    private static final IntFunction<? super LinkedBlockingQueue> LINKED_BLOCKING_QUEUE_FACTORY = LinkedBlockingQueue::new;

    /** The Constant ARRAY_BLOCKING_QUEUE_FACTORY. */
    @SuppressWarnings("rawtypes")
    private static final IntFunction<? super ArrayBlockingQueue> ARRAY_BLOCKING_QUEUE_FACTORY = ArrayBlockingQueue::new;

    /** The Constant LINKED_BLOCKING_DEQUE_FACTORY. */
    @SuppressWarnings("rawtypes")
    private static final IntFunction<? super LinkedBlockingDeque> LINKED_BLOCKING_DEQUE_FACTORY = LinkedBlockingDeque::new;

    /** The Constant CONCURRENT_LINKED_QUEUE_FACTORY. */
    @SuppressWarnings("rawtypes")
    private static final IntFunction<? super ConcurrentLinkedQueue> CONCURRENT_LINKED_QUEUE_FACTORY = capacity -> new ConcurrentLinkedQueue();

    /** The Constant PRIORITY_QUEUE_FACTORY. */
    @SuppressWarnings("rawtypes")
    private static final IntFunction<? super PriorityQueue> PRIORITY_QUEUE_FACTORY = PriorityQueue::new;

    /** The Constant MAP_FACTORY. */
    @SuppressWarnings("rawtypes")
    private static final IntFunction<? super Map> MAP_FACTORY = N::newHashMap;

    /** The Constant LINKED_HASH_MAP_FACTORY. */
    @SuppressWarnings("rawtypes")
    private static final IntFunction<? super Map> LINKED_HASH_MAP_FACTORY = N::newLinkedHashMap;

    /** The Constant IDENTITY_HASH_MAP_FACTORY. */
    @SuppressWarnings("rawtypes")
    private static final IntFunction<? super IdentityHashMap> IDENTITY_HASH_MAP_FACTORY = N::newIdentityHashMap;

    /** The Constant TREE_MAP_FACTORY. */
    @SuppressWarnings("rawtypes")
    private static final IntFunction<? super TreeMap> TREE_MAP_FACTORY = len -> N.newTreeMap();

    /** The Constant CONCURRENT_HASH_MAP_FACTORY. */
    @SuppressWarnings("rawtypes")
    private static final IntFunction<? super ConcurrentHashMap> CONCURRENT_HASH_MAP_FACTORY = N::newConcurrentHashMap;

    /** The Constant BI_MAP_FACTORY. */
    @SuppressWarnings("rawtypes")
    private static final IntFunction<? super BiMap> BI_MAP_FACTORY = N::newBiMap;

    /** The Constant MULTISET_FACTORY. */
    @SuppressWarnings("rawtypes")
    private static final IntFunction<? super Multiset> MULTISET_FACTORY = N::newMultiset;

    /** The Constant LIST_MULTIMAP_FACTORY. */
    @SuppressWarnings("rawtypes")
    private static final IntFunction<? super ListMultimap> LIST_MULTIMAP_FACTORY = N::newLinkedListMultimap;

    /** The Constant SET_MULTIMAP_FACTORY. */
    @SuppressWarnings("rawtypes")
    private static final IntFunction<? super SetMultimap> SET_MULTIMAP_FACTORY = N::newSetMultimap;

    private IntFunctions() {
        // utility class
    }

    /**
     * Returns the provided IntFunction as is - a shorthand identity method for IntFunction instances.
     * 
     * <p>This method serves as a shorthand convenience method that can help with type inference
     * in certain contexts. It's part of a family of factory methods that handle various function types.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Instead of explicitly typing:
     * IntFunction<String[]> arrayCreator = size -> new String[size];
     * // You can use:
     * var arrayCreator = IntFunctions.of(size -> new String[size]);
     * }</pre>
     *
     * @param <T> the type of the result of the function
     * @param func the IntFunction to return
     * @return the IntFunction unchanged
     */
    public static <T> IntFunction<T> of(IntFunction<T> func) {
        return func;
    }

    /**
     * Returns an IntFunction that creates boolean arrays of the specified size.
     * 
     * <p>The returned function creates new boolean arrays with all elements initialized to false.</p>
     *
     * @return an IntFunction that creates boolean arrays
     */
    public static IntFunction<boolean[]> ofBooleanArray() {
        return BOOLEAN_ARRAY;
    }

    /**
     * Returns an IntFunction that creates char arrays of the specified size.
     * 
     * <p>The returned function creates new char arrays with all elements initialized to '\u0000'.</p>
     *
     * @return an IntFunction that creates char arrays
     */
    public static IntFunction<char[]> ofCharArray() {
        return CHAR_ARRAY;
    }

    /**
     * Returns an IntFunction that creates byte arrays of the specified size.
     * 
     * <p>The returned function creates new byte arrays with all elements initialized to 0.</p>
     *
     * @return an IntFunction that creates byte arrays
     */
    public static IntFunction<byte[]> ofByteArray() {
        return BYTE_ARRAY;
    }

    /**
     * Returns an IntFunction that creates short arrays of the specified size.
     * 
     * <p>The returned function creates new short arrays with all elements initialized to 0.</p>
     *
     * @return an IntFunction that creates short arrays
     */
    public static IntFunction<short[]> ofShortArray() {
        return SHORT_ARRAY;
    }

    /**
     * Returns an IntFunction that creates int arrays of the specified size.
     * 
     * <p>The returned function creates new int arrays with all elements initialized to 0.</p>
     *
     * @return an IntFunction that creates int arrays
     */
    public static IntFunction<int[]> ofIntArray() {
        return INT_ARRAY;
    }

    /**
     * Returns an IntFunction that creates long arrays of the specified size.
     * 
     * <p>The returned function creates new long arrays with all elements initialized to 0L.</p>
     *
     * @return an IntFunction that creates long arrays
     */
    public static IntFunction<long[]> ofLongArray() {
        return LONG_ARRAY;
    }

    /**
     * Returns an IntFunction that creates float arrays of the specified size.
     * 
     * <p>The returned function creates new float arrays with all elements initialized to 0.0f.</p>
     *
     * @return an IntFunction that creates float arrays
     */
    public static IntFunction<float[]> ofFloatArray() {
        return FLOAT_ARRAY;
    }

    /**
     * Returns an IntFunction that creates double arrays of the specified size.
     * 
     * <p>The returned function creates new double arrays with all elements initialized to 0.0.</p>
     *
     * @return an IntFunction that creates double arrays
     */
    public static IntFunction<double[]> ofDoubleArray() {
        return DOUBLE_ARRAY;
    }

    /**
     * Returns an IntFunction that creates String arrays of the specified size.
     * 
     * <p>The returned function creates new String arrays with all elements initialized to null.</p>
     *
     * @return an IntFunction that creates String arrays
     */
    public static IntFunction<String[]> ofStringArray() {
        return STRING_ARRAY;
    }

    /**
     * Returns an IntFunction that creates Object arrays of the specified size.
     * 
     * <p>The returned function creates new Object arrays with all elements initialized to null.</p>
     *
     * @return an IntFunction that creates Object arrays
     */
    public static IntFunction<Object[]> ofObjectArray() {
        return OBJECT_ARRAY;
    }

    /**
     * Returns an IntFunction that creates BooleanList instances with the specified initial capacity.
     * 
     * <p>BooleanList is a specialized list implementation for primitive boolean values,
     * avoiding boxing/unboxing overhead.</p>
     *
     * @return an IntFunction that creates BooleanList instances
     */
    public static IntFunction<BooleanList> ofBooleanList() {
        return BOOLEAN_LIST;
    }

    /**
     * Returns an IntFunction that creates CharList instances with the specified initial capacity.
     * 
     * <p>CharList is a specialized list implementation for primitive char values,
     * avoiding boxing/unboxing overhead.</p>
     *
     * @return an IntFunction that creates CharList instances
     */
    public static IntFunction<CharList> ofCharList() {
        return CHAR_LIST;
    }

    /**
     * Returns an IntFunction that creates ByteList instances with the specified initial capacity.
     * 
     * <p>ByteList is a specialized list implementation for primitive byte values,
     * avoiding boxing/unboxing overhead.</p>
     *
     * @return an IntFunction that creates ByteList instances
     */
    public static IntFunction<ByteList> ofByteList() {
        return BYTE_LIST;
    }

    /**
     * Returns an IntFunction that creates ShortList instances with the specified initial capacity.
     * 
     * <p>ShortList is a specialized list implementation for primitive short values,
     * avoiding boxing/unboxing overhead.</p>
     *
     * @return an IntFunction that creates ShortList instances
     */
    public static IntFunction<ShortList> ofShortList() {
        return SHORT_LIST;
    }

    /**
     * Returns an IntFunction that creates IntList instances with the specified initial capacity.
     * 
     * <p>IntList is a specialized list implementation for primitive int values,
     * avoiding boxing/unboxing overhead.</p>
     *
     * @return an IntFunction that creates IntList instances
     */
    public static IntFunction<IntList> ofIntList() {
        return INT_LIST;
    }

    /**
     * Returns an IntFunction that creates LongList instances with the specified initial capacity.
     * 
     * <p>LongList is a specialized list implementation for primitive long values,
     * avoiding boxing/unboxing overhead.</p>
     *
     * @return an IntFunction that creates LongList instances
     */
    public static IntFunction<LongList> ofLongList() {
        return LONG_LIST;
    }

    /**
     * Returns an IntFunction that creates FloatList instances with the specified initial capacity.
     * 
     * <p>FloatList is a specialized list implementation for primitive float values,
     * avoiding boxing/unboxing overhead.</p>
     *
     * @return an IntFunction that creates FloatList instances
     */
    public static IntFunction<FloatList> ofFloatList() {
        return FLOAT_LIST;
    }

    /**
     * Returns an IntFunction that creates DoubleList instances with the specified initial capacity.
     * 
     * <p>DoubleList is a specialized list implementation for primitive double values,
     * avoiding boxing/unboxing overhead.</p>
     *
     * @return an IntFunction that creates DoubleList instances
     */
    public static IntFunction<DoubleList> ofDoubleList() {
        return DOUBLE_LIST;
    }

    /**
     * Returns an IntFunction that creates ArrayList instances with the specified initial capacity.
     * 
     * <p>The returned function creates new ArrayList instances optimized with the given initial capacity
     * to avoid resizing during element addition.</p>
     *
     * @param <T> the type of elements in the list
     * @return an IntFunction that creates ArrayList instances
     */
    @SuppressWarnings("rawtypes")
    public static <T> IntFunction<List<T>> ofList() {
        return (IntFunction) LIST_FACTORY;
    }

    /**
     * Returns an IntFunction that creates LinkedList instances.
     * 
     * <p>The returned function creates new LinkedList instances. Note that the capacity parameter
     * is ignored as LinkedList does not support initial capacity.</p>
     *
     * @param <T> the type of elements in the list
     * @return an IntFunction that creates LinkedList instances
     */
    @SuppressWarnings("rawtypes")
    public static <T> IntFunction<LinkedList<T>> ofLinkedList() {
        return (IntFunction) LINKED_LIST_FACTORY;
    }

    /**
     * Returns an IntFunction that creates HashSet instances with the specified initial capacity.
     * 
     * <p>The returned function creates new HashSet instances optimized with the given initial capacity
     * to avoid rehashing during element addition.</p>
     *
     * @param <T> the type of elements in the set
     * @return an IntFunction that creates HashSet instances
     */
    @SuppressWarnings("rawtypes")
    public static <T> IntFunction<Set<T>> ofSet() {
        return (IntFunction) SET_FACTORY;
    }

    /**
     * Returns an IntFunction that creates LinkedHashSet instances with the specified initial capacity.
     * 
     * <p>The returned function creates new LinkedHashSet instances that maintain insertion order
     * and are optimized with the given initial capacity.</p>
     *
     * @param <T> the type of elements in the set
     * @return an IntFunction that creates LinkedHashSet instances
     */
    @SuppressWarnings("rawtypes")
    public static <T> IntFunction<Set<T>> ofLinkedHashSet() {
        return (IntFunction) LINKED_HASH_SET_FACTORY;
    }

    /**
     * Returns an IntFunction that creates TreeSet instances.
     * 
     * <p>The returned function creates new TreeSet instances that maintain elements in sorted order.
     * Note that the capacity parameter is ignored as TreeSet does not support initial capacity.</p>
     *
     * @param <T> the type of elements in the set
     * @return an IntFunction that creates TreeSet instances
     */
    @SuppressWarnings("rawtypes")
    public static <T> IntFunction<SortedSet<T>> ofSortedSet() {
        return (IntFunction) TREE_SET_FACTORY;
    }

    /**
     * Returns an IntFunction that creates TreeSet instances as NavigableSet.
     * 
     * <p>The returned function creates new TreeSet instances that provide navigation methods
     * for accessing elements relative to other elements. Note that the capacity parameter
     * is ignored as TreeSet does not support initial capacity.</p>
     *
     * @param <T> the type of elements in the set
     * @return an IntFunction that creates TreeSet instances as NavigableSet
     */
    @SuppressWarnings("rawtypes")
    public static <T> IntFunction<NavigableSet<T>> ofNavigableSet() {
        return (IntFunction) TREE_SET_FACTORY;
    }

    /**
     * Returns an IntFunction that creates TreeSet instances.
     * 
     * <p>The returned function creates new TreeSet instances that maintain elements in sorted order.
     * Note that the capacity parameter is ignored as TreeSet does not support initial capacity.</p>
     *
     * @param <T> the type of elements in the set
     * @return an IntFunction that creates TreeSet instances
     */
    @SuppressWarnings("rawtypes")
    public static <T> IntFunction<TreeSet<T>> ofTreeSet() {
        return (IntFunction) TREE_SET_FACTORY;
    }

    /**
     * Returns an IntFunction that creates LinkedList instances as Queue.
     * 
     * <p>The returned function creates new LinkedList instances that implement the Queue interface.
     * Note that the capacity parameter is ignored as LinkedList does not support initial capacity.</p>
     *
     * @param <T> the type of elements in the queue
     * @return an IntFunction that creates Queue instances (backed by LinkedList)
     */
    @SuppressWarnings("rawtypes")
    public static <T> IntFunction<Queue<T>> ofQueue() {
        return (IntFunction) QUEUE_FACTORY;
    }

    /**
     * Returns an IntFunction that creates LinkedList instances as Deque.
     * 
     * <p>The returned function creates new LinkedList instances that implement the Deque interface,
     * supporting element insertion and removal at both ends. Note that the capacity parameter
     * is ignored as LinkedList does not support initial capacity.</p>
     *
     * @param <T> the type of elements in the deque
     * @return an IntFunction that creates Deque instances (backed by LinkedList)
     */
    @SuppressWarnings("rawtypes")
    public static <T> IntFunction<Deque<T>> ofDeque() {
        return (IntFunction) DEQUE_FACTORY;
    }

    /**
     * Returns an IntFunction that creates ArrayDeque instances with the specified initial capacity.
     * The returned function can be used to create pre-sized ArrayDeque collections for performance optimization.
     *
     * @param <T> the type of elements to be stored in the ArrayDeque
     * @return an IntFunction that accepts an initial capacity and returns a new ArrayDeque instance
     * @see ArrayDeque#ArrayDeque(int)
     */
    @SuppressWarnings("rawtypes")
    public static <T> IntFunction<ArrayDeque<T>> ofArrayDeque() {
        return (IntFunction) ARRAY_DEQUE_FACTORY;
    }

    /**
     * Returns an IntFunction that creates LinkedBlockingQueue instances with the specified initial capacity.
     * The returned function can be used to create thread-safe blocking queues with bounded capacity.
     *
     * @param <T> the type of elements to be stored in the LinkedBlockingQueue
     * @return an IntFunction that accepts an initial capacity and returns a new LinkedBlockingQueue instance
     * @see LinkedBlockingQueue#LinkedBlockingQueue(int)
     */
    @SuppressWarnings("rawtypes")
    public static <T> IntFunction<LinkedBlockingQueue<T>> ofLinkedBlockingQueue() {
        return (IntFunction) LINKED_BLOCKING_QUEUE_FACTORY;
    }

    /**
     * Returns an IntFunction that creates ArrayBlockingQueue instances with the specified capacity.
     * The returned function creates fixed-size, thread-safe blocking queues backed by an array.
     *
     * @param <T> the type of elements to be stored in the ArrayBlockingQueue
     * @return an IntFunction that accepts a capacity and returns a new ArrayBlockingQueue instance
     * @see ArrayBlockingQueue#ArrayBlockingQueue(int)
     */
    @SuppressWarnings("rawtypes")
    public static <T> IntFunction<ArrayBlockingQueue<T>> ofArrayBlockingQueue() {
        return (IntFunction) ARRAY_BLOCKING_QUEUE_FACTORY;
    }

    /**
     * Returns an IntFunction that creates LinkedBlockingDeque instances with the specified initial capacity.
     * The returned function creates thread-safe, optionally-bounded blocking deques based on linked nodes.
     *
     * @param <T> the type of elements to be stored in the LinkedBlockingDeque
     * @return an IntFunction that accepts an initial capacity and returns a new LinkedBlockingDeque instance
     * @see LinkedBlockingDeque#LinkedBlockingDeque(int)
     */
    @SuppressWarnings("rawtypes")
    public static <T> IntFunction<LinkedBlockingDeque<T>> ofLinkedBlockingDeque() {
        return (IntFunction) LINKED_BLOCKING_DEQUE_FACTORY;
    }

    /**
     * Returns an IntFunction that creates ConcurrentLinkedQueue instances.
     * The returned function creates unbounded thread-safe queues based on linked nodes.
     * Note: The capacity parameter is ignored as ConcurrentLinkedQueue is always unbounded.
     *
     * @param <T> the type of elements to be stored in the ConcurrentLinkedQueue
     * @return an IntFunction that accepts a capacity (ignored) and returns a new ConcurrentLinkedQueue instance
     * @see ConcurrentLinkedQueue#ConcurrentLinkedQueue()
     */
    @SuppressWarnings("rawtypes")
    public static <T> IntFunction<ConcurrentLinkedQueue<T>> ofConcurrentLinkedQueue() {
        return (IntFunction) CONCURRENT_LINKED_QUEUE_FACTORY;
    }

    /**
     * Returns an IntFunction that creates PriorityQueue instances with the specified initial capacity.
     * The returned function creates unbounded priority queues based on a priority heap.
     *
     * @param <T> the type of elements to be stored in the PriorityQueue
     * @return an IntFunction that accepts an initial capacity and returns a new PriorityQueue instance
     * @see PriorityQueue#PriorityQueue(int)
     */
    @SuppressWarnings("rawtypes")
    public static <T> IntFunction<PriorityQueue<T>> ofPriorityQueue() {
        return (IntFunction) PRIORITY_QUEUE_FACTORY;
    }

    /**
     * Returns an IntFunction that creates HashMap instances with the specified initial capacity.
     * The returned function creates hash table based Map implementations.
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @return an IntFunction that accepts an initial capacity and returns a new HashMap instance
     * @see HashMap#HashMap(int)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> IntFunction<Map<K, V>> ofMap() {
        return (IntFunction) MAP_FACTORY;
    }

    /**
     * Returns an IntFunction that creates LinkedHashMap instances with the specified initial capacity.
     * The returned function creates hash table and linked list implementations of the Map interface,
     * with predictable iteration order.
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @return an IntFunction that accepts an initial capacity and returns a new LinkedHashMap instance
     * @see LinkedHashMap#LinkedHashMap(int)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> IntFunction<Map<K, V>> ofLinkedHashMap() {
        return (IntFunction) LINKED_HASH_MAP_FACTORY;
    }

    /**
     * Returns an IntFunction that creates IdentityHashMap instances with the specified expected maximum size.
     * The returned function creates maps that use reference-equality instead of object-equality when comparing keys.
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @return an IntFunction that accepts an expected maximum size and returns a new IdentityHashMap instance
     * @see IdentityHashMap#IdentityHashMap(int)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> IntFunction<IdentityHashMap<K, V>> ofIdentityHashMap() {
        return (IntFunction) IDENTITY_HASH_MAP_FACTORY;
    }

    /**
     * Returns an IntFunction that creates TreeMap instances.
     * The returned function creates Red-Black tree based NavigableMap implementations.
     * Note: The capacity parameter is ignored as TreeMap doesn't have a capacity constructor.
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @return an IntFunction that accepts a capacity (ignored) and returns a new TreeMap instance as SortedMap
     * @see TreeMap#TreeMap()
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> IntFunction<SortedMap<K, V>> ofSortedMap() {
        return (IntFunction) TREE_MAP_FACTORY;
    }

    /**
     * Returns an IntFunction that creates TreeMap instances.
     * The returned function creates Red-Black tree based NavigableMap implementations.
     * Note: The capacity parameter is ignored as TreeMap doesn't have a capacity constructor.
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @return an IntFunction that accepts a capacity (ignored) and returns a new TreeMap instance as NavigableMap
     * @see TreeMap#TreeMap()
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> IntFunction<NavigableMap<K, V>> ofNavigableMap() {
        return (IntFunction) TREE_MAP_FACTORY;
    }

    /**
     * Returns an IntFunction that creates TreeMap instances.
     * The returned function creates Red-Black tree based NavigableMap implementations.
     * Note: The capacity parameter is ignored as TreeMap doesn't have a capacity constructor.
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @return an IntFunction that accepts a capacity (ignored) and returns a new TreeMap instance
     * @see TreeMap#TreeMap()
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> IntFunction<TreeMap<K, V>> ofTreeMap() {
        return (IntFunction) TREE_MAP_FACTORY;
    }

    /**
     * Returns an IntFunction that creates ConcurrentHashMap instances with the specified initial capacity.
     * The returned function creates thread-safe hash table based Map implementations.
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @return an IntFunction that accepts an initial capacity and returns a new ConcurrentHashMap instance as ConcurrentMap
     * @see ConcurrentHashMap#ConcurrentHashMap(int)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> IntFunction<ConcurrentMap<K, V>> ofConcurrentMap() {
        return (IntFunction) CONCURRENT_HASH_MAP_FACTORY;
    }

    /**
     * Returns an IntFunction that creates ConcurrentHashMap instances with the specified initial capacity.
     * The returned function creates thread-safe hash table based Map implementations.
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @return an IntFunction that accepts an initial capacity and returns a new ConcurrentHashMap instance
     * @see ConcurrentHashMap#ConcurrentHashMap(int)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> IntFunction<ConcurrentHashMap<K, V>> ofConcurrentHashMap() {
        return (IntFunction) CONCURRENT_HASH_MAP_FACTORY;
    }

    /**
     * Returns an IntFunction that creates BiMap instances with the specified initial capacity.
     * BiMap is a bidirectional map that preserves the uniqueness of its values as well as that of its keys.
     *
     * @param <K> the type of keys maintained by the BiMap
     * @param <V> the type of mapped values
     * @return an IntFunction that accepts an initial capacity and returns a new BiMap instance
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> IntFunction<BiMap<K, V>> ofBiMap() {
        return (IntFunction) BI_MAP_FACTORY;
    }

    /**
     * Returns an IntFunction that creates Multiset instances with the specified initial capacity.
     * A Multiset is a collection that supports order-independent equality and may contain duplicate elements.
     *
     * @param <T> the type of elements in the Multiset
     * @return an IntFunction that accepts an initial capacity and returns a new Multiset instance
     */
    @SuppressWarnings("rawtypes")
    public static <T> IntFunction<Multiset<T>> ofMultiset() {
        return (IntFunction) MULTISET_FACTORY;
    }

    /**
     * Returns an IntFunction that creates ListMultimap instances with the specified initial capacity.
     * A ListMultimap is a Multimap that can hold duplicate key-value pairs and maintains insertion ordering of values for a given key.
     *
     * @param <K> the type of keys maintained by the multimap
     * @param <E> the type of mapped values
     * @return an IntFunction that accepts an initial capacity and returns a new ListMultimap instance
     */
    @SuppressWarnings("rawtypes")
    public static <K, E> IntFunction<ListMultimap<K, E>> ofListMultimap() {
        return (IntFunction) LIST_MULTIMAP_FACTORY;
    }

    /**
     * Returns an IntFunction that creates SetMultimap instances with the specified initial capacity.
     * A SetMultimap is a Multimap that cannot hold duplicate key-value pairs.
     *
     * @param <K> the type of keys maintained by the multimap
     * @param <E> the type of mapped values
     * @return an IntFunction that accepts an initial capacity and returns a new SetMultimap instance
     */
    @SuppressWarnings("rawtypes")
    public static <K, E> IntFunction<SetMultimap<K, E>> ofSetMultimap() {
        return (IntFunction) SET_MULTIMAP_FACTORY;
    }

    /**
     * Returns a new created stateful IntFunction whose apply method will return the same DisposableObjArray instance.
     * The DisposableObjArray is created lazily on first call and reused for subsequent calls.
     * This method is marked as Beta, SequentialOnly, and Stateful, indicating it should not be saved, cached for reuse, or used in parallel streams.
     *
     * @return a stateful IntFunction that returns a reusable DisposableObjArray instance
     */
    @Beta
    @SequentialOnly
    @Stateful
    public static IntFunction<DisposableObjArray> ofDisposableArray() {
        return new IntFunction<>() {
            private DisposableObjArray ret = null;

            @Override
            public DisposableObjArray apply(final int len) {
                if (ret == null) {
                    ret = DisposableObjArray.wrap(new Object[len]);
                }

                return ret;
            }
        };
    }

    /**
     * Returns a new created stateful IntFunction whose apply method will return the same DisposableArray instance.
     * The DisposableArray is created lazily on first call with the specified component type and reused for subsequent calls.
     * This method is marked as Beta, SequentialOnly, and Stateful, indicating it should not be saved, cached for reuse, or used in parallel streams.
     *
     * @param <T> the component type of the array
     * @param componentType the Class object representing the component type of the array
     * @return a stateful IntFunction that returns a reusable DisposableArray instance
     */
    @Beta
    @SequentialOnly
    @Stateful
    public static <T> IntFunction<DisposableArray<T>> ofDisposableArray(final Class<T> componentType) {
        return new IntFunction<>() {
            private DisposableArray<T> ret = null;

            @Override
            public DisposableArray<T> apply(final int len) {
                if (ret == null) {
                    ret = DisposableArray.wrap(N.newArray(componentType, len));
                }

                return ret;
            }
        };
    }

    @SuppressWarnings("rawtypes")
    private static final Map<Class<?>, IntFunction> collectionCreatorPool = new ConcurrentHashMap<>();

    /**
     * Returns an IntFunction that creates Collection instances of the specified target type with the given initial capacity.
     * This method supports various Collection implementations and attempts to find an appropriate constructor or factory method.
     * The returned IntFunction is cached for performance optimization.
     *
     * @param <T> the type of elements in the collection
     * @param targetType the Class object representing the desired Collection implementation
     * @return an IntFunction that accepts an initial capacity and returns a new Collection instance of the specified type
     * @throws IllegalArgumentException if targetType is not a Collection class or if no suitable constructor/factory can be found
     */
    @SuppressWarnings("rawtypes")
    public static <T> IntFunction<? extends Collection<T>> ofCollection(final Class<? extends Collection> targetType) throws IllegalArgumentException {
        IntFunction ret = collectionCreatorPool.get(targetType);

        if (ret == null) {
            N.checkArgument(Collection.class.isAssignableFrom(targetType), "'targetType': {} is not a Collection class", targetType);

            if (Collection.class.equals(targetType) || AbstractCollection.class.equals(targetType) || List.class.equals(targetType)
                    || AbstractList.class.equals(targetType) || ArrayList.class.equals(targetType)) {
                ret = ofList();
            } else if (LinkedList.class.equals(targetType)) {
                ret = ofLinkedList();
            } else if (Set.class.equals(targetType) || AbstractSet.class.equals(targetType) || HashSet.class.equals(targetType)) {
                ret = ofSet();
            } else if (LinkedHashSet.class.equals(targetType)) {
                ret = ofLinkedHashSet();
            } else if (SortedSet.class.isAssignableFrom(targetType)) {
                ret = ofSortedSet();
            } else if (Queue.class.equals(targetType) || AbstractQueue.class.equals(targetType) || Deque.class.equals(targetType)) {
                return ofDeque();
            } else if (BlockingQueue.class.equals(targetType) || LinkedBlockingQueue.class.equals(targetType)) {
                return ofLinkedBlockingQueue();
            } else if (ArrayBlockingQueue.class.equals(targetType)) {
                return ofArrayBlockingQueue();
            } else if (BlockingDeque.class.equals(targetType) || LinkedBlockingDeque.class.equals(targetType)) {
                return ofLinkedBlockingDeque();
            } else if (ConcurrentLinkedQueue.class.equals(targetType)) {
                return ofConcurrentLinkedQueue();
            } else if (PriorityQueue.class.equals(targetType)) {
                return ofPriorityQueue();
            } else if (ImmutableList.class.isAssignableFrom(targetType)) {
                ret = ofList();
            } else if (ImmutableSet.class.isAssignableFrom(targetType)) {
                ret = ofSet();
            } else if (Modifier.isAbstract(targetType.getModifiers())) {
                throw new IllegalArgumentException("Not able to create instance for collection: " + targetType);
            } else {
                try {
                    final Constructor<?> constructor = ClassUtil.getDeclaredConstructor(targetType, int.class);

                    //noinspection ConstantValue
                    if (constructor != null && N.invoke(constructor, 9) != null) { // magic number?
                        ret = size -> {
                            try {
                                return (Collection<T>) N.invoke(constructor, size);
                            } catch (final Throwable e) { // NOSONAR
                                throw new IllegalArgumentException("Not able to create instance for collection: " + targetType, e);
                            }
                        };
                    }
                } catch (final Throwable e) { // NOSONAR
                    // ignore
                }

                try {
                    if (ret == null && N.newInstance(targetType) != null) {
                        ret = size -> {
                            try {
                                return (Collection<T>) N.newInstance(targetType);
                            } catch (final Exception e) {
                                throw new IllegalArgumentException("Not able to create instance for collection: " + targetType, e);
                            }
                        };
                    }
                } catch (final Throwable e) { // NOSONAR
                    // ignore
                }

                if (ret == null) {
                    if (targetType.isAssignableFrom(LinkedHashSet.class)) {
                        ret = ofLinkedHashSet();
                    } else if (targetType.isAssignableFrom(HashSet.class)) {
                        ret = ofSet();
                    } else if (targetType.isAssignableFrom(LinkedList.class)) {
                        ret = ofLinkedList();
                    } else if (targetType.isAssignableFrom(ArrayList.class)) {
                        ret = ofList();
                    } else {
                        throw new IllegalArgumentException("Not able to create instance for collection: " + targetType);
                    }
                }
            }

            collectionCreatorPool.put(targetType, ret);
        }

        return ret;
    }

    @SuppressWarnings("rawtypes")
    private static final Map<Class<?>, IntFunction> mapCreatorPool = new ConcurrentHashMap<>();

    /**
     * Returns an IntFunction that creates Map instances of the specified target type with the given initial capacity.
     * This method supports various Map implementations and attempts to find an appropriate constructor or factory method.
     * The returned IntFunction is cached for performance optimization.
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param targetType the Class object representing the desired Map implementation
     * @return an IntFunction that accepts an initial capacity and returns a new Map instance of the specified type
     * @throws IllegalArgumentException if targetType is not a Map class or if no suitable constructor/factory can be found
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> IntFunction<? extends Map<K, V>> ofMap(final Class<? extends Map> targetType) throws IllegalArgumentException {
        IntFunction ret = mapCreatorPool.get(targetType);

        if (ret == null) {
            N.checkArgument(Map.class.isAssignableFrom(targetType), "'targetType': {} is not a Map class", targetType);

            if (Map.class.equals(targetType) || AbstractMap.class.equals(targetType) || HashMap.class.equals(targetType) || EnumMap.class.equals(targetType)) {
                ret = ofMap();
            } else if (LinkedHashMap.class.equals(targetType)) {
                ret = ofLinkedHashMap();
            } else if (SortedMap.class.isAssignableFrom(targetType)) {
                ret = ofSortedMap();
            } else if (IdentityHashMap.class.isAssignableFrom(targetType)) {
                ret = ofIdentityHashMap();
            } else if (ConcurrentHashMap.class.isAssignableFrom(targetType) || ConcurrentMap.class.equals(targetType)) {
                ret = ofConcurrentHashMap();
            } else if (BiMap.class.isAssignableFrom(targetType)) {
                ret = ofBiMap();
            } else if (ImmutableMap.class.isAssignableFrom(targetType)) {
                ret = ofMap();
            } else if (Modifier.isAbstract(targetType.getModifiers())) {
                throw new IllegalArgumentException("Not able to create instance for abstract Map: " + targetType);
            } else {
                try {
                    final Constructor<?> constructor = ClassUtil.getDeclaredConstructor(targetType, int.class);

                    //noinspection ConstantValue
                    if (constructor != null && N.invoke(constructor, 9) != null) { // magic number?
                        ret = size -> {
                            try {
                                return (Map<K, V>) N.invoke(constructor, size);
                            } catch (final Throwable e) { // NOSONAR
                                throw new IllegalArgumentException("Not able to create instance for Map: " + targetType, e);
                            }
                        };
                    }
                } catch (final Throwable e) { // NOSONAR
                    // ignore
                }

                try {
                    if (ret == null && N.newInstance(targetType) != null) {
                        ret = size -> {
                            try {
                                return (Map<K, V>) N.newInstance(targetType);
                            } catch (final Exception e) {
                                throw new IllegalArgumentException("Not able to create instance for Map: " + targetType, e);
                            }
                        };
                    }
                } catch (final Throwable e) { // NOSONAR
                    // ignore
                }

                if (ret == null) {
                    if (targetType.isAssignableFrom(TreeMap.class)) {
                        ret = ofTreeMap();
                    } else if (targetType.isAssignableFrom(LinkedHashMap.class)) {
                        ret = ofLinkedHashMap();
                    } else if (targetType.isAssignableFrom(HashMap.class)) {
                        ret = ofMap();
                    } else {
                        throw new IllegalArgumentException("Not able to create instance for Map: " + ClassUtil.getCanonicalClassName(targetType));
                    }
                }
            }

            mapCreatorPool.put(targetType, ret);
        }

        return ret;
    }

    /**
     * Registers a custom IntFunction creator for the specified Collection target class.
     * The registered creator will be used by ofCollection method to create instances of the target class.
     * Built-in collection classes cannot be registered.
     *
     * @param <T> the type of Collection to register
     * @param targetClass the Class object representing the Collection type to register
     * @param creator the IntFunction that creates instances of the target class with specified capacity
     * @return {@code true} if the registration was successful, {@code false} if a creator was already registered for this class
     * @throws IllegalArgumentException if targetClass or creator is null, or if targetClass is a built-in class
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Collection> boolean registerForCollection(final Class<T> targetClass, final java.util.function.IntFunction<T> creator)
            throws IllegalArgumentException {
        N.checkArgNotNull(targetClass, cs.targetClass);
        N.checkArgNotNull(creator, cs.creator);

        if (N.isBuiltinClass(targetClass)) {
            throw new IllegalArgumentException("Can't register IntFunction with built-in class: " + ClassUtil.getCanonicalClassName(targetClass));
        }

        if (collectionCreatorPool.containsKey(targetClass)) {
            return false;
        }

        return collectionCreatorPool.put(targetClass, Fn.from(creator)) == null;
    }

    /**
     * Registers a custom IntFunction creator for the specified Map target class.
     * The registered creator will be used by ofMap method to create instances of the target class.
     * Built-in map classes cannot be registered.
     *
     * @param <T> the type of Map to register
     * @param targetClass the Class object representing the Map type to register
     * @param creator the IntFunction that creates instances of the target class with specified capacity
     * @return {@code true} if the registration was successful, {@code false} if a creator was already registered for this class
     * @throws IllegalArgumentException if targetClass or creator is null, or if targetClass is a built-in class
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Map> boolean registerForMap(final Class<T> targetClass, final java.util.function.IntFunction<T> creator)
            throws IllegalArgumentException {
        N.checkArgNotNull(targetClass, cs.targetClass);
        N.checkArgNotNull(creator, cs.creator);

        if (N.isBuiltinClass(targetClass)) {
            throw new IllegalArgumentException("Can't register IntFunction with built-in class: " + ClassUtil.getCanonicalClassName(targetClass));
        }

        if (mapCreatorPool.containsKey(targetClass)) {
            return false;
        }

        return mapCreatorPool.put(targetClass, Fn.from(creator)) == null;

    }

    /**
     * Returns an IntFunction for creating ImmutableList instances.
     * This operation is not supported.
     *
     * @return never returns normally
     * @throws UnsupportedOperationException always thrown as this operation is not supported
     * @deprecated unsupported operation
     */
    @Deprecated
    public static IntFunction<ImmutableList<?>> ofImmutableList() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns an IntFunction for creating ImmutableSet instances.
     * This operation is not supported.
     *
     * @return never returns normally
     * @throws UnsupportedOperationException always thrown as this operation is not supported
     * @deprecated unsupported operation
     */
    @Deprecated
    public static IntFunction<ImmutableSet<?>> ofImmutableSet() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns an IntFunction for creating ImmutableMap instances.
     * This operation is not supported.
     *
     * @return never returns normally
     * @throws UnsupportedOperationException always thrown as this operation is not supported
     * @deprecated unsupported operation
     */
    @Deprecated
    public static IntFunction<ImmutableMap<?, ?>> ofImmutableMap() {
        throw new UnsupportedOperationException();
    }

    //    /**
}
