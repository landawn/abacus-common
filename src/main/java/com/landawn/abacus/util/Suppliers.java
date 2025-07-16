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
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.Supplier;

/**
 * Utility class providing static methods to create various types of suppliers for collections, maps, arrays, and other commonly used objects.
 *
 * <p>This class serves as a centralized factory for creating {@link Supplier} instances that generate
 * different types of data structures and objects. It provides convenient methods for creating suppliers
 * for collections (List, Set, Queue, etc.), maps, primitive arrays, and other utility objects like
 * UUIDs and exceptions.</p>
 *
 * <p>The class supports both built-in Java collections and custom collection implementations from
 * the Abacus library. It also provides registration mechanisms for custom suppliers when working
 * with non-standard collection or map implementations.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Suppliers for all major collection types (ArrayList, LinkedList, HashSet, TreeSet, etc.)</li>
 *   <li>Suppliers for various map implementations (HashMap, TreeMap, ConcurrentHashMap, etc.)</li>
 *   <li>Suppliers for primitive arrays and specialized list types</li>
 *   <li>UUID and GUID generators</li>
 *   <li>Exception instance suppliers</li>
 *   <li>Custom supplier registration for non-standard types</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Create a supplier for ArrayList
 * Supplier<List<String>> listSupplier = Suppliers.ofList();
 * List<String> list = listSupplier.get();
 *
 * // Create a supplier for TreeSet
 * Supplier<Set<Integer>> setSupplier = Suppliers.ofSet();
 * Set<Integer> set = setSupplier.get();
 *
 * // Create a supplier for HashMap
 * Supplier<Map<String, Object>> mapSupplier = Suppliers.ofMap();
 * Map<String, Object> map = mapSupplier.get();
 *
 * // Generate UUIDs
 * Supplier<String> uuidSupplier = Suppliers.ofUUID();
 * String uuid = uuidSupplier.get();
 *
 * // Dynamic collection type creation
 * Supplier<? extends Collection<String>> dynamicSupplier = Suppliers.ofCollection(LinkedHashSet.class);
 * Collection<String> collection = dynamicSupplier.get();
 * }</pre>
 *
 * <p>This class is thread-safe and all returned suppliers are safe for concurrent use unless
 * otherwise specified. The class uses internal caching to optimize supplier creation for
 * frequently requested types.</p>
 *
 * @since 1.0
 * @see java.util.function.Supplier
 * @see java.util.Collection
 * @see java.util.Map
 */
public final class Suppliers {

    /** The Constant UUID. */
    private static final Supplier<String> UUID = Strings::uuid;

    /** The Constant GUID. */
    private static final Supplier<String> GUID = Strings::guid;

    /** The Constant EMPTY_BOOLEAN_ARRAY. */
    private static final Supplier<boolean[]> EMPTY_BOOLEAN_ARRAY = () -> N.EMPTY_BOOLEAN_ARRAY;

    /** The Constant EMPTY_CHAR_ARRAY. */
    private static final Supplier<char[]> EMPTY_CHAR_ARRAY = () -> N.EMPTY_CHAR_ARRAY;

    /** The Constant EMPTY_BYTE_ARRAY. */
    private static final Supplier<byte[]> EMPTY_BYTE_ARRAY = () -> N.EMPTY_BYTE_ARRAY;

    /** The Constant EMPTY_SHORT_ARRAY. */
    private static final Supplier<short[]> EMPTY_SHORT_ARRAY = () -> N.EMPTY_SHORT_ARRAY;

    /** The Constant EMPTY_INT_ARRAY. */
    private static final Supplier<int[]> EMPTY_INT_ARRAY = () -> N.EMPTY_INT_ARRAY;

    /** The Constant EMPTY_LONG_ARRAY. */
    private static final Supplier<long[]> EMPTY_LONG_ARRAY = () -> N.EMPTY_LONG_ARRAY;

    /** The Constant EMPTY_FLOAT_ARRAY. */
    private static final Supplier<float[]> EMPTY_FLOAT_ARRAY = () -> N.EMPTY_FLOAT_ARRAY;

    /** The Constant EMPTY_DOUBLE_ARRAY. */
    private static final Supplier<double[]> EMPTY_DOUBLE_ARRAY = () -> N.EMPTY_DOUBLE_ARRAY;

    /** The Constant EMPTY_STRING_ARRAY. */
    private static final Supplier<String[]> EMPTY_STRING_ARRAY = () -> N.EMPTY_STRING_ARRAY;

    /** The Constant EMPTY_OBJECT_ARRAY. */
    private static final Supplier<Object[]> EMPTY_OBJECT_ARRAY = () -> N.EMPTY_OBJECT_ARRAY;

    /** The Constant EMPTY_STRING. */
    private static final Supplier<String> EMPTY_STRING = () -> Strings.EMPTY;

    /** The Constant BOOLEAN_LIST. */
    private static final Supplier<BooleanList> BOOLEAN_LIST = BooleanList::new;

    /** The Constant CHAR_LIST. */
    private static final Supplier<CharList> CHAR_LIST = CharList::new;

    /** The Constant BYTE_LIST. */
    private static final Supplier<ByteList> BYTE_LIST = ByteList::new;

    /** The Constant SHORT_LIST. */
    private static final Supplier<ShortList> SHORT_LIST = ShortList::new;

    /** The Constant INT_LIST. */
    private static final Supplier<IntList> INT_LIST = IntList::new;

    /** The Constant LONG_LIST. */
    private static final Supplier<LongList> LONG_LIST = LongList::new;

    /** The Constant FLOAT_LIST. */
    private static final Supplier<FloatList> FLOAT_LIST = FloatList::new;

    /** The Constant DOUBLE_LIST. */
    private static final Supplier<DoubleList> DOUBLE_LIST = DoubleList::new;

    /** The Constant LIST. */
    @SuppressWarnings("rawtypes")
    private static final Supplier<? super List> LIST = ArrayList::new;

    /** The Constant LINKED_LIST. */
    @SuppressWarnings("rawtypes")
    private static final Supplier<? super LinkedList> LINKED_LIST = LinkedList::new;

    /** The Constant SET. */
    @SuppressWarnings("rawtypes")
    private static final Supplier<? super Set> SET = N::newHashSet;

    /** The Constant LINKED_HASH_SET. */
    @SuppressWarnings("rawtypes")
    private static final Supplier<? super Set> LINKED_HASH_SET = N::newLinkedHashSet;

    /** The Constant TREE_SET. */
    @SuppressWarnings("rawtypes")
    private static final Supplier<? super TreeSet> TREE_SET = TreeSet::new;

    /** The Constant QUEUE. */
    @SuppressWarnings("rawtypes")
    private static final Supplier<? super Queue> QUEUE = LinkedList::new;

    /** The Constant DEQUE. */
    @SuppressWarnings("rawtypes")
    private static final Supplier<? super Deque> DEQUE = LinkedList::new;

    /** The Constant ARRAY_DEQUE. */
    @SuppressWarnings("rawtypes")
    private static final Supplier<? super ArrayDeque> ARRAY_DEQUE = ArrayDeque::new;

    /** The Constant LINKED_BLOCKING_QUEUE. */
    @SuppressWarnings("rawtypes")
    private static final Supplier<? super LinkedBlockingQueue> LINKED_BLOCKING_QUEUE = LinkedBlockingQueue::new;

    /** The Constant LINKED_BLOCKING_DEQUE. */
    @SuppressWarnings("rawtypes")
    private static final Supplier<? super LinkedBlockingDeque> LINKED_BLOCKING_DEQUE = LinkedBlockingDeque::new;

    /** The Constant CONCURRENT_LINKED_QUEUE. */
    @SuppressWarnings("rawtypes")
    private static final Supplier<? super ConcurrentLinkedQueue> CONCURRENT_LINKED_QUEUE = ConcurrentLinkedQueue::new;

    /** The Constant PRIORITY_QUEUE. */
    @SuppressWarnings("rawtypes")
    private static final Supplier<? super PriorityQueue> PRIORITY_QUEUE = PriorityQueue::new;

    /** The Constant MAP. */
    @SuppressWarnings("rawtypes")
    private static final Supplier<? super Map> MAP = N::newHashMap;

    /** The Constant LINKED_HASH_MAP. */
    @SuppressWarnings("rawtypes")
    private static final Supplier<? super Map> LINKED_HASH_MAP = N::newLinkedHashMap;

    /** The Constant IDENTITY_HASH_MAP. */
    @SuppressWarnings("rawtypes")
    private static final Supplier<? super IdentityHashMap> IDENTITY_HASH_MAP = IdentityHashMap::new;

    /** The Constant TREE_MAP. */
    @SuppressWarnings("rawtypes")
    private static final Supplier<? super TreeMap> TREE_MAP = TreeMap::new;

    /** The Constant CONCURRENT_HASH_MAP. */
    @SuppressWarnings("rawtypes")
    private static final Supplier<? super ConcurrentHashMap> CONCURRENT_HASH_MAP = ConcurrentHashMap::new;

    /** The Constant CONCURRENT_HASH_SET. */
    @SuppressWarnings("rawtypes")
    private static final Supplier<? super Set> CONCURRENT_HASH_SET = ConcurrentHashMap::newKeySet;

    /** The Constant BI_MAP. */
    @SuppressWarnings("rawtypes")
    private static final Supplier<? super BiMap> BI_MAP = BiMap::new;

    /** The Constant MULTISET. */
    @SuppressWarnings("rawtypes")
    private static final Supplier<? super Multiset> MULTISET = Multiset::new;

    /** The Constant LIST_MULTIMAP. */
    @SuppressWarnings("rawtypes")
    private static final Supplier<? super ListMultimap> LIST_MULTIMAP = N::newListMultimap;

    /** The Constant SET_MULTIMAP. */
    @SuppressWarnings("rawtypes")
    private static final Supplier<? super SetMultimap> SET_MULTIMAP = N::newSetMultimap;

    /** The Constant STRING_BUILDER. */
    private static final Supplier<StringBuilder> STRING_BUILDER = StringBuilder::new;

    private Suppliers() {
        // utility class
    }

    /**
     * Returns the provided supplier as is - a shorthand identity method for suppliers.
     * 
     * <p>This method serves as a shorthand convenience method that can help with type inference
     * in certain contexts. It's part of a family of shorthand methods like {@code p()} for Predicate
     * and others.</p>
     *
     * @param <T> the type of results supplied by the supplier
     * @param supplier the supplier to return
     * @return the supplier unchanged
     * @see #of(Object, Function)
     * @see Fn#s(Supplier)
     * @see Fn#s(Object, Function)
     * @see Fn#ss(com.landawn.abacus.util.Throwables.Supplier)
     * @see Fn#ss(Object, com.landawn.abacus.util.Throwables.Function)
     * @see IntFunctions#of(IntFunction)
     */
    @Beta
    public static <T> Supplier<T> of(final Supplier<T> supplier) {
        return supplier;
    }

    /**
     * Creates a supplier that always returns the result of applying the provided function to the given value.
     * 
     * <p>This method creates a supplier that captures the provided value and function,
     * and when the supplier is called, it applies the function to the value and returns the result.</p>
     *
     * @param <A> the type of the input value
     * @param <T> the type of results supplied by the supplier
     * @param a the value to be processed by the function
     * @param func the function to apply to the value
     * @return a supplier that will return the result of applying the function to the value
     * @see #of(Supplier)
     * @see Fn#s(Supplier)
     * @see Fn#s(Object, Function)
     * @see Fn#ss(com.landawn.abacus.util.Throwables.Supplier)
     * @see Fn#ss(Object, com.landawn.abacus.util.Throwables.Function)
     * @see IntFunctions#of(IntFunction)
     */
    @Beta
    public static <A, T> Supplier<T> of(final A a, final Function<? super A, ? extends T> func) {
        return () -> func.apply(a);
    }

    /**
     * Returns a supplier that always supplies the same instance.
     * 
     * <p>This method creates a supplier that always returns the provided instance,
     * useful for creating constant suppliers.</p>
     *
     * @param <T> the type of the instance
     * @param instance the instance to be supplied
     * @return a supplier that always returns the same instance
     */
    public static <T> Supplier<T> ofInstance(final T instance) {
        return () -> instance;
    }

    /**
     * Returns a supplier that generates UUID strings.
     * 
     * <p>Each call to the supplier's get() method will generate a new UUID string.</p>
     *
     * @return a supplier that generates UUID strings
     * @see #ofGUID()
     */
    public static Supplier<String> ofUUID() {
        return UUID;
    }

    /**
     * Returns a supplier that generates GUID strings.
     * 
     * <p>Each call to the supplier's get() method will generate a new GUID string.</p>
     *
     * @return a supplier that generates GUID strings
     * @see #ofUUID()
     */
    public static Supplier<String> ofGUID() {
        return GUID;
    }

    /**
     * Returns a supplier that supplies empty boolean arrays.
     * 
     * <p>This supplier always returns the same empty boolean array instance for efficiency.</p>
     *
     * @return a supplier that returns an empty boolean array
     */
    public static Supplier<boolean[]> ofEmptyBooleanArray() {
        return EMPTY_BOOLEAN_ARRAY;
    }

    /**
     * Returns a supplier that supplies empty char arrays.
     * 
     * <p>This supplier always returns the same empty char array instance for efficiency.</p>
     *
     * @return a supplier that returns an empty char array
     */
    public static Supplier<char[]> ofEmptyCharArray() {
        return EMPTY_CHAR_ARRAY;
    }

    /**
     * Returns a supplier that supplies empty byte arrays.
     * 
     * <p>This supplier always returns the same empty byte array instance for efficiency.</p>
     *
     * @return a supplier that returns an empty byte array
     */
    public static Supplier<byte[]> ofEmptyByteArray() {
        return EMPTY_BYTE_ARRAY;
    }

    /**
     * Returns a supplier that supplies empty short arrays.
     * 
     * <p>This supplier always returns the same empty short array instance for efficiency.</p>
     *
     * @return a supplier that returns an empty short array
     */
    public static Supplier<short[]> ofEmptyShortArray() {
        return EMPTY_SHORT_ARRAY;
    }

    /**
     * Returns a supplier that supplies empty int arrays.
     * 
     * <p>This supplier always returns the same empty int array instance for efficiency.</p>
     *
     * @return a supplier that returns an empty int array
     */
    public static Supplier<int[]> ofEmptyIntArray() {
        return EMPTY_INT_ARRAY;
    }

    /**
     * Returns a supplier that supplies empty long arrays.
     * 
     * <p>This supplier always returns the same empty long array instance for efficiency.</p>
     *
     * @return a supplier that returns an empty long array
     */
    public static Supplier<long[]> ofEmptyLongArray() {
        return EMPTY_LONG_ARRAY;
    }

    /**
     * Returns a supplier that supplies empty float arrays.
     * 
     * <p>This supplier always returns the same empty float array instance for efficiency.</p>
     *
     * @return a supplier that returns an empty float array
     */
    public static Supplier<float[]> ofEmptyFloatArray() {
        return EMPTY_FLOAT_ARRAY;
    }

    /**
     * Returns a supplier that supplies empty double arrays.
     * 
     * <p>This supplier always returns the same empty double array instance for efficiency.</p>
     *
     * @return a supplier that returns an empty double array
     */
    public static Supplier<double[]> ofEmptyDoubleArray() {
        return EMPTY_DOUBLE_ARRAY;
    }

    /**
     * Returns a supplier that supplies empty String arrays.
     * 
     * <p>This supplier always returns the same empty String array instance for efficiency.</p>
     *
     * @return a supplier that returns an empty String array
     */
    public static Supplier<String[]> ofEmptyStringArray() {
        return EMPTY_STRING_ARRAY;
    }

    /**
     * Returns a supplier that supplies empty Object arrays.
     * 
     * <p>This supplier always returns the same empty Object array instance for efficiency.</p>
     *
     * @return a supplier that returns an empty Object array
     */
    public static Supplier<Object[]> ofEmptyObjectArray() {
        return EMPTY_OBJECT_ARRAY;
    }

    /**
     * Returns a supplier that supplies empty strings.
     * 
     * <p>This supplier always returns the same empty string instance.</p>
     *
     * @return a supplier that returns an empty string
     */
    public static Supplier<String> ofEmptyString() {
        return EMPTY_STRING;
    }

    /**
     * Returns a supplier that creates new BooleanList instances.
     * 
     * <p>Each call to the supplier's get() method will create a new, empty BooleanList.</p>
     *
     * @return a supplier that creates new BooleanList instances
     */
    public static Supplier<BooleanList> ofBooleanList() {
        return BOOLEAN_LIST;
    }

    /**
     * Returns a supplier that creates new CharList instances.
     * 
     * <p>Each call to the supplier's get() method will create a new, empty CharList.</p>
     *
     * @return a supplier that creates new CharList instances
     */
    public static Supplier<CharList> ofCharList() {
        return CHAR_LIST;
    }

    /**
     * Returns a supplier that creates new ByteList instances.
     * 
     * <p>Each call to the supplier's get() method will create a new, empty ByteList.</p>
     *
     * @return a supplier that creates new ByteList instances
     */
    public static Supplier<ByteList> ofByteList() {
        return BYTE_LIST;
    }

    /**
     * Returns a supplier that creates new ShortList instances.
     * 
     * <p>Each call to the supplier's get() method will create a new, empty ShortList.</p>
     *
     * @return a supplier that creates new ShortList instances
     */
    public static Supplier<ShortList> ofShortList() {
        return SHORT_LIST;
    }

    /**
     * Returns a supplier that creates new IntList instances.
     * 
     * <p>Each call to the supplier's get() method will create a new, empty IntList.</p>
     *
     * @return a supplier that creates new IntList instances
     */
    public static Supplier<IntList> ofIntList() {
        return INT_LIST;
    }

    /**
     * Returns a supplier that creates new LongList instances.
     * 
     * <p>Each call to the supplier's get() method will create a new, empty LongList.</p>
     *
     * @return a supplier that creates new LongList instances
     */
    public static Supplier<LongList> ofLongList() {
        return LONG_LIST;
    }

    /**
     * Returns a supplier that creates new FloatList instances.
     * 
     * <p>Each call to the supplier's get() method will create a new, empty FloatList.</p>
     *
     * @return a supplier that creates new FloatList instances
     */
    public static Supplier<FloatList> ofFloatList() {
        return FLOAT_LIST;
    }

    /**
     * Returns a supplier that creates new DoubleList instances.
     * 
     * <p>Each call to the supplier's get() method will create a new, empty DoubleList.</p>
     *
     * @return a supplier that creates new DoubleList instances
     */
    public static Supplier<DoubleList> ofDoubleList() {
        return DOUBLE_LIST;
    }

    /**
     * Returns a supplier that creates new List instances (ArrayList).
     * 
     * <p>Each call to the supplier's get() method will create a new, empty ArrayList.</p>
     *
     * @param <T> the type of elements in the list
     * @return a supplier that creates new ArrayList instances
     */
    @SuppressWarnings("rawtypes")
    public static <T> Supplier<List<T>> ofList() {
        return (Supplier) LIST;
    }

    /**
     * Returns a supplier that creates new LinkedList instances.
     * 
     * <p>Each call to the supplier's get() method will create a new, empty LinkedList.</p>
     *
     * @param <T> the type of elements in the list
     * @return a supplier that creates new LinkedList instances
     */
    @SuppressWarnings("rawtypes")
    public static <T> Supplier<LinkedList<T>> ofLinkedList() {
        return (Supplier) LINKED_LIST;
    }

    /**
     * Returns a supplier that creates new Set instances (HashSet).
     * 
     * <p>Each call to the supplier's get() method will create a new, empty HashSet.</p>
     *
     * @param <T> the type of elements in the set
     * @return a supplier that creates new HashSet instances
     */
    @SuppressWarnings("rawtypes")
    public static <T> Supplier<Set<T>> ofSet() {
        return (Supplier) SET;
    }

    /**
     * Returns a supplier that creates new LinkedHashSet instances.
     * 
     * <p>Each call to the supplier's get() method will create a new, empty LinkedHashSet.</p>
     *
     * @param <T> the type of elements in the set
     * @return a supplier that creates new LinkedHashSet instances
     */
    @SuppressWarnings("rawtypes")
    public static <T> Supplier<Set<T>> ofLinkedHashSet() {
        return (Supplier) LINKED_HASH_SET;
    }

    /**
     * Returns a supplier that creates new SortedSet instances (TreeSet).
     * 
     * <p>Each call to the supplier's get() method will create a new, empty TreeSet.</p>
     *
     * @param <T> the type of elements in the set
     * @return a supplier that creates new TreeSet instances
     */
    @SuppressWarnings("rawtypes")
    public static <T> Supplier<SortedSet<T>> ofSortedSet() {
        return (Supplier) TREE_SET;
    }

    /**
     * Returns a supplier that creates new NavigableSet instances (TreeSet).
     * 
     * <p>Each call to the supplier's get() method will create a new, empty TreeSet.</p>
     *
     * @param <T> the type of elements in the set
     * @return a supplier that creates new TreeSet instances
     */
    @SuppressWarnings("rawtypes")
    public static <T> Supplier<NavigableSet<T>> ofNavigableSet() {
        return (Supplier) TREE_SET;
    }

    /**
     * Returns a supplier that creates new TreeSet instances.
     * 
     * <p>Each call to the supplier's get() method will create a new, empty TreeSet.</p>
     *
     * @param <T> the type of elements in the set
     * @return a supplier that creates new TreeSet instances
     */
    @SuppressWarnings("rawtypes")
    public static <T> Supplier<TreeSet<T>> ofTreeSet() {
        return (Supplier) TREE_SET;
    }

    /**
     * Returns a supplier that creates new Queue instances (LinkedList).
     * 
     * <p>Each call to the supplier's get() method will create a new, empty LinkedList as a Queue.</p>
     *
     * @param <T> the type of elements in the queue
     * @return a supplier that creates new LinkedList instances as Queue
     */
    @SuppressWarnings("rawtypes")
    public static <T> Supplier<Queue<T>> ofQueue() {
        return (Supplier) QUEUE;
    }

    /**
     * Returns a supplier that creates new Deque instances (LinkedList).
     * 
     * <p>Each call to the supplier's get() method will create a new, empty LinkedList as a Deque.</p>
     *
     * @param <T> the type of elements in the deque
     * @return a supplier that creates new LinkedList instances as Deque
     */
    @SuppressWarnings("rawtypes")
    public static <T> Supplier<Deque<T>> ofDeque() {
        return (Supplier) DEQUE;
    }

    /**
     * Returns a supplier that creates new ArrayDeque instances.
     * 
     * <p>Each call to the supplier's get() method will create a new, empty ArrayDeque.</p>
     *
     * @param <T> the type of elements in the deque
     * @return a supplier that creates new ArrayDeque instances
     */
    @SuppressWarnings("rawtypes")
    public static <T> Supplier<ArrayDeque<T>> ofArrayDeque() {
        return (Supplier) ARRAY_DEQUE;
    }

    /**
     * Returns a supplier that creates new LinkedBlockingQueue instances.
     * 
     * <p>Each call to the supplier's get() method will create a new, empty LinkedBlockingQueue
     * with unbounded capacity.</p>
     *
     * @param <T> the type of elements in the queue
     * @return a supplier that creates new LinkedBlockingQueue instances
     */
    @SuppressWarnings("rawtypes")
    public static <T> Supplier<LinkedBlockingQueue<T>> ofLinkedBlockingQueue() {
        return (Supplier) LINKED_BLOCKING_QUEUE;
    }

    /**
     * Returns a supplier that creates new LinkedBlockingDeque instances.
     * 
     * <p>Each call to the supplier's get() method will create a new, empty LinkedBlockingDeque
     * with unbounded capacity.</p>
     *
     * @param <T> the type of elements in the deque
     * @return a supplier that creates new LinkedBlockingDeque instances
     */
    @SuppressWarnings("rawtypes")
    public static <T> Supplier<LinkedBlockingDeque<T>> ofLinkedBlockingDeque() {
        return (Supplier) LINKED_BLOCKING_DEQUE;
    }

    /**
     * Returns a supplier that creates new ConcurrentLinkedQueue instances.
     * 
     * <p>Each call to the supplier's get() method will create a new, empty ConcurrentLinkedQueue.</p>
     *
     * @param <T> the type of elements in the queue
     * @return a supplier that creates new ConcurrentLinkedQueue instances
     */
    @SuppressWarnings("rawtypes")
    public static <T> Supplier<ConcurrentLinkedQueue<T>> ofConcurrentLinkedQueue() {
        return (Supplier) CONCURRENT_LINKED_QUEUE;
    }

    /**
     * Returns a supplier that creates new PriorityQueue instances.
     * 
     * <p>Each call to the supplier's get() method will create a new, empty PriorityQueue
     * with natural ordering.</p>
     *
     * @param <T> the type of elements in the queue
     * @return a supplier that creates new PriorityQueue instances
     */
    @SuppressWarnings("rawtypes")
    public static <T> Supplier<PriorityQueue<T>> ofPriorityQueue() {
        return (Supplier) PRIORITY_QUEUE;
    }

    /**
     * Returns a supplier that creates new Map instances (HashMap).
     * 
     * <p>Each call to the supplier's get() method will create a new, empty HashMap.</p>
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return a supplier that creates new HashMap instances
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Supplier<Map<K, V>> ofMap() {
        return (Supplier) MAP;
    }

    /**
     * Returns a supplier that creates new LinkedHashMap instances.
     * 
     * <p>Each call to the supplier's get() method will create a new, empty LinkedHashMap.</p>
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return a supplier that creates new LinkedHashMap instances
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Supplier<Map<K, V>> ofLinkedHashMap() {
        return (Supplier) LINKED_HASH_MAP;
    }

    /**
     * Returns a supplier that creates new IdentityHashMap instances.
     * 
     * <p>Each call to the supplier's get() method will create a new, empty IdentityHashMap.
     * IdentityHashMap uses reference equality (==) instead of object equality (equals).</p>
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return a supplier that creates new IdentityHashMap instances
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Supplier<IdentityHashMap<K, V>> ofIdentityHashMap() {
        return (Supplier) IDENTITY_HASH_MAP;
    }

    /**
     * Returns a supplier that creates new SortedMap instances (TreeMap).
     * 
     * <p>Each call to the supplier's get() method will create a new, empty TreeMap.</p>
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return a supplier that creates new TreeMap instances
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Supplier<SortedMap<K, V>> ofSortedMap() {
        return (Supplier) TREE_MAP;
    }

    /**
     * Returns a supplier that creates new NavigableMap instances (TreeMap).
     * 
     * <p>Each call to the supplier's get() method will create a new, empty TreeMap.</p>
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return a supplier that creates new TreeMap instances
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Supplier<NavigableMap<K, V>> ofNavigableMap() {
        return (Supplier) TREE_MAP;
    }

    /**
     * Returns a supplier that creates new TreeMap instances.
     * 
     * <p>Each call to the supplier's get() method will create a new, empty TreeMap.</p>
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return a supplier that creates new TreeMap instances
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Supplier<TreeMap<K, V>> ofTreeMap() {
        return (Supplier) TREE_MAP;
    }

    /**
     * Returns a supplier that creates new ConcurrentMap instances (ConcurrentHashMap).
     * 
     * <p>Each call to the supplier's get() method will create a new, empty ConcurrentHashMap.</p>
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return a supplier that creates new ConcurrentHashMap instances
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Supplier<ConcurrentMap<K, V>> ofConcurrentMap() {
        return (Supplier) CONCURRENT_HASH_MAP;
    }

    /**
     * Returns a supplier that creates new ConcurrentHashMap instances.
     * 
     * <p>Each call to the supplier's get() method will create a new, empty ConcurrentHashMap.</p>
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return a supplier that creates new ConcurrentHashMap instances
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Supplier<ConcurrentHashMap<K, V>> ofConcurrentHashMap() {
        return (Supplier) CONCURRENT_HASH_MAP;
    }

    /**
     * Returns a supplier that creates new concurrent Set instances.
     * 
     * <p>Each call to the supplier's get() method will create a new, empty Set backed by ConcurrentHashMap.</p>
     *
     * @param <T> the type of elements in the set
     * @return a supplier that creates new concurrent Set instances
     */
    @SuppressWarnings("rawtypes")
    public static <T> Supplier<Set<T>> ofConcurrentHashSet() {
        return (Supplier) CONCURRENT_HASH_SET;
    }

    /**
     * Returns a supplier that creates new BiMap instances.
     * 
     * <p>Each call to the supplier's get() method will create a new, empty BiMap.
     * A BiMap maintains a bidirectional mapping between keys and values.</p>
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return a supplier that creates new BiMap instances
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Supplier<BiMap<K, V>> ofBiMap() {
        return (Supplier) BI_MAP;
    }

    /**
     * Returns a supplier that creates new Multiset instances.
     * 
     * <p>Each call to the supplier's get() method will create a new, empty Multiset.
     * A Multiset is a collection that allows duplicate elements and counts their occurrences.</p>
     *
     * @param <T> the type of elements in the multiset
     * @return a supplier that creates new Multiset instances
     */
    @SuppressWarnings("rawtypes")
    public static <T> Supplier<Multiset<T>> ofMultiset() {
        return (Supplier) MULTISET;
    }

    /**
     * Returns a supplier that creates new Multiset instances with a specific map type.
     * 
     * <p>Each call to the supplier's get() method will create a new, empty Multiset
     * backed by the specified type of Map.</p>
     *
     * @param <T> the type of elements in the multiset
     * @param valueMapType the class of Map to use for storing element counts
     * @return a supplier that creates new Multiset instances
     */
    @SuppressWarnings("rawtypes")
    public static <T> Supplier<Multiset<T>> ofMultiset(final Class<? extends Map> valueMapType) {
        return () -> N.newMultiset(valueMapType);
    }

    /**
     * Returns a supplier that creates new Multiset instances with a custom map supplier.
     * 
     * <p>Each call to the supplier's get() method will create a new, empty Multiset
     * backed by a Map created by the provided supplier.</p>
     *
     * @param <T> the type of elements in the multiset
     * @param mapSupplier supplier to create the backing Map
     * @return a supplier that creates new Multiset instances
     */
    public static <T> Supplier<Multiset<T>> ofMultiset(final java.util.function.Supplier<? extends Map<T, ?>> mapSupplier) {
        return () -> N.newMultiset(mapSupplier);
    }

    /**
     * Returns a Supplier that creates a new ListMultimap with default backing Map and List implementations.
     * 
     * <p>The returned supplier creates ListMultimaps backed by HashMap and ArrayList.
     * Each invocation of the supplier creates a new empty ListMultimap instance.</p>
     *
     * @param <K> the type of keys maintained by the multimap
     * @param <E> the type of mapped values
     * @return a Supplier that creates new ListMultimap instances
     */
    @SuppressWarnings("rawtypes")
    public static <K, E> Supplier<ListMultimap<K, E>> ofListMultimap() {
        return (Supplier) LIST_MULTIMAP;
    }

    /**
     * Returns a Supplier that creates a new ListMultimap with the specified Map type and default List implementation.
     * 
     * <p>The returned supplier creates ListMultimaps backed by the specified Map type and ArrayList.
     * Each invocation of the supplier creates a new empty ListMultimap instance.</p>
     *
     * @param <K> the type of keys maintained by the multimap
     * @param <E> the type of mapped values
     * @param mapType the Class object representing the Map implementation to use
     * @return a Supplier that creates new ListMultimap instances with the specified Map type
     * @throws IllegalArgumentException if mapType is null
     */
    @SuppressWarnings("rawtypes")
    public static <K, E> Supplier<ListMultimap<K, E>> ofListMultimap(final Class<? extends Map> mapType) {
        return () -> N.newListMultimap(mapType);
    }

    /**
     * Returns a Supplier that creates a new ListMultimap with the specified Map and List types.
     * 
     * <p>The returned supplier creates ListMultimaps backed by the specified Map and List implementations.
     * Each invocation of the supplier creates a new empty ListMultimap instance.</p>
     *
     * @param <K> the type of keys maintained by the multimap
     * @param <E> the type of mapped values
     * @param mapType the Class object representing the Map implementation to use
     * @param valueType the Class object representing the List implementation to use for values
     * @return a Supplier that creates new ListMultimap instances with the specified types
     * @throws IllegalArgumentException if mapType or valueType is null
     */
    @SuppressWarnings("rawtypes")
    public static <K, E> Supplier<ListMultimap<K, E>> ofListMultimap(final Class<? extends Map> mapType, final Class<? extends List> valueType) {
        return () -> N.newListMultimap(mapType, valueType);
    }

    /**
     * Returns a Supplier that creates a new ListMultimap using the provided map and value suppliers.
     * 
     * <p>The returned supplier creates ListMultimaps using custom suppliers for both the backing Map
     * and the List instances used for values. This allows for complete control over the multimap's
     * internal structure.</p>
     *
     * @param <K> the type of keys maintained by the multimap
     * @param <E> the type of mapped values
     * @param mapSupplier supplier that creates the backing Map instances
     * @param valueSupplier supplier that creates the List instances for values
     * @return a Supplier that creates new ListMultimap instances using the provided suppliers
     * @throws IllegalArgumentException if mapSupplier or valueSupplier is null
     */
    public static <K, E> Supplier<ListMultimap<K, E>> ofListMultimap(final java.util.function.Supplier<? extends Map<K, List<E>>> mapSupplier,
            final java.util.function.Supplier<? extends List<E>> valueSupplier) {
        return () -> N.newListMultimap(mapSupplier, valueSupplier);
    }

    /**
     * Returns a Supplier that creates a new SetMultimap with default backing Map and Set implementations.
     * 
     * <p>The returned supplier creates SetMultimaps backed by HashMap and HashSet.
     * Each invocation of the supplier creates a new empty SetMultimap instance.</p>
     *
     * @param <K> the type of keys maintained by the multimap
     * @param <E> the type of mapped values
     * @return a Supplier that creates new SetMultimap instances
     */
    @SuppressWarnings("rawtypes")
    public static <K, E> Supplier<SetMultimap<K, E>> ofSetMultimap() {
        return (Supplier) SET_MULTIMAP;
    }

    /**
     * Returns a Supplier that creates a new SetMultimap with the specified Map type and default Set implementation.
     * 
     * <p>The returned supplier creates SetMultimaps backed by the specified Map type and HashSet.
     * Each invocation of the supplier creates a new empty SetMultimap instance.</p>
     *
     * @param <K> the type of keys maintained by the multimap
     * @param <E> the type of mapped values
     * @param mapType the Class object representing the Map implementation to use
     * @return a Supplier that creates new SetMultimap instances with the specified Map type
     * @throws IllegalArgumentException if mapType is null
     */
    @SuppressWarnings("rawtypes")
    public static <K, E> Supplier<SetMultimap<K, E>> ofSetMultimap(final Class<? extends Map> mapType) {
        return () -> N.newSetMultimap(mapType);
    }

    /**
     * Returns a Supplier that creates a new SetMultimap with the specified Map and Set types.
     * 
     * <p>The returned supplier creates SetMultimaps backed by the specified Map and Set implementations.
     * Each invocation of the supplier creates a new empty SetMultimap instance.</p>
     *
     * @param <K> the type of keys maintained by the multimap
     * @param <E> the type of mapped values
     * @param mapType the Class object representing the Map implementation to use
     * @param valueType the Class object representing the Set implementation to use for values
     * @return a Supplier that creates new SetMultimap instances with the specified types
     * @throws IllegalArgumentException if mapType or valueType is null
     */
    @SuppressWarnings("rawtypes")
    public static <K, E> Supplier<SetMultimap<K, E>> ofSetMultimap(final Class<? extends Map> mapType, final Class<? extends Set> valueType) {
        return () -> N.newSetMultimap(mapType, valueType);
    }

    /**
     * Returns a Supplier that creates a new SetMultimap using the provided map and value suppliers.
     * 
     * <p>The returned supplier creates SetMultimaps using custom suppliers for both the backing Map
     * and the Set instances used for values. This allows for complete control over the multimap's
     * internal structure.</p>
     *
     * @param <K> the type of keys maintained by the multimap
     * @param <E> the type of mapped values
     * @param mapSupplier supplier that creates the backing Map instances
     * @param valueSupplier supplier that creates the Set instances for values
     * @return a Supplier that creates new SetMultimap instances using the provided suppliers
     * @throws IllegalArgumentException if mapSupplier or valueSupplier is null
     */
    public static <K, E> Supplier<SetMultimap<K, E>> ofSetMultimap(final java.util.function.Supplier<? extends Map<K, Set<E>>> mapSupplier,
            final java.util.function.Supplier<? extends Set<E>> valueSupplier) {
        return () -> N.newSetMultimap(mapSupplier, valueSupplier);
    }

    /**
     * Returns a Supplier that creates a new Multimap using the provided map and value collection suppliers.
     * 
     * <p>This is the most general multimap supplier, allowing any Collection type for values.
     * The returned supplier creates Multimaps using custom suppliers for both the backing Map
     * and the Collection instances used for values.</p>
     *
     * @param <K> the type of keys maintained by the multimap
     * @param <E> the type of mapped values
     * @param <V> the type of Collection used to store values
     * @param mapSupplier supplier that creates the backing Map instances
     * @param valueSupplier supplier that creates the Collection instances for values
     * @return a Supplier that creates new Multimap instances using the provided suppliers
     * @throws IllegalArgumentException if mapSupplier or valueSupplier is null
     */
    public static <K, E, V extends Collection<E>> Supplier<Multimap<K, E, V>> ofMultimap(final java.util.function.Supplier<? extends Map<K, V>> mapSupplier,
            final java.util.function.Supplier<? extends V> valueSupplier) {
        return () -> N.newMultimap(mapSupplier, valueSupplier);
    }

    /**
     * Returns a Supplier that creates new StringBuilder instances.
     * 
     * <p>Each invocation of the supplier creates a new empty StringBuilder with default initial capacity.</p>
     *
     * @return a Supplier that creates new StringBuilder instances
     */
    public static Supplier<StringBuilder> ofStringBuilder() {
        return STRING_BUILDER;
    }

    @SuppressWarnings("rawtypes")
    private static final Map<Class<?>, Supplier> collectionSupplierPool = new ConcurrentHashMap<>();

    /**
     * Returns a Supplier that creates Collection instances of the specified type.
     * 
     * <p>This method provides suppliers for various Collection implementations including List, Set, Queue,
     * and their subtypes. The method uses a cache to avoid creating duplicate suppliers for the same type.</p>
     * 
     * <p>Supported types include:
     * <ul>
     *   <li>Collection, List, ArrayList - returns ArrayList supplier</li>
     *   <li>LinkedList - returns LinkedList supplier</li>
     *   <li>Set, HashSet - returns HashSet supplier</li>
     *   <li>LinkedHashSet - returns LinkedHashSet supplier</li>
     *   <li>SortedSet, TreeSet - returns TreeSet supplier</li>
     *   <li>Queue, Deque - returns ArrayDeque supplier</li>
     *   <li>Various concurrent collections</li>
     * </ul>
     * </p>
     *
     * @param <T> the element type of the collection
     * @param targetType the Class object representing the desired Collection implementation
     * @return a Supplier that creates instances of the specified Collection type
     * @throws IllegalArgumentException if targetType is not a Collection class, is abstract and cannot be instantiated,
     *         or if no suitable implementation can be found
     */
    @SuppressWarnings("rawtypes")
    public static <T> Supplier<? extends Collection<T>> ofCollection(final Class<? extends Collection> targetType) throws IllegalArgumentException {
        Supplier ret = collectionSupplierPool.get(targetType);

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
                throw new IllegalArgumentException("Can't create instance for abstract class: " + targetType);
            } else {
                try {
                    if (N.newInstance(targetType) != null) {
                        ret = () -> N.newInstance(targetType);
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
                        throw new IllegalArgumentException("Not able to create instance for collection: " + ClassUtil.getCanonicalClassName(targetType));
                    }
                }
            }

            collectionSupplierPool.put(targetType, ret);
        }

        return ret;
    }

    @SuppressWarnings("rawtypes")
    private static final Map<Class<?>, Supplier> mapSupplierPool = new ConcurrentHashMap<>();

    /**
     * Returns a Supplier that creates Map instances of the specified type.
     * 
     * <p>This method provides suppliers for various Map implementations including HashMap, LinkedHashMap,
     * TreeMap, and concurrent maps. The method uses a cache to avoid creating duplicate suppliers for the same type.</p>
     * 
     * <p>Supported types include:
     * <ul>
     *   <li>Map, HashMap - returns HashMap supplier</li>
     *   <li>LinkedHashMap - returns LinkedHashMap supplier</li>
     *   <li>SortedMap, TreeMap - returns TreeMap supplier</li>
     *   <li>IdentityHashMap - returns IdentityHashMap supplier</li>
     *   <li>ConcurrentHashMap - returns ConcurrentHashMap supplier</li>
     *   <li>BiMap - returns BiMap supplier</li>
     * </ul>
     * </p>
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param targetType the Class object representing the desired Map implementation
     * @return a Supplier that creates instances of the specified Map type
     * @throws IllegalArgumentException if targetType is not a Map class, is abstract and cannot be instantiated,
     *         or if no suitable implementation can be found
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Supplier<? extends Map<K, V>> ofMap(final Class<? extends Map> targetType) throws IllegalArgumentException {
        Supplier ret = mapSupplierPool.get(targetType);

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
            } else if (ConcurrentHashMap.class.isAssignableFrom(targetType)) {
                ret = ofConcurrentHashMap();
            } else if (BiMap.class.isAssignableFrom(targetType)) {
                ret = ofBiMap();
            } else if (ImmutableMap.class.isAssignableFrom(targetType)) {
                ret = ofMap();
            } else if (Modifier.isAbstract(targetType.getModifiers())) {
                throw new IllegalArgumentException("Not able to create instance for abstract Map: " + targetType);
            } else {
                try {
                    if (N.newInstance(targetType) != null) {
                        ret = () -> N.newInstance(targetType);
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
                        throw new IllegalArgumentException("Not able to create instance for Map: " + targetType);
                    }
                }
            }

            mapSupplierPool.put(targetType, ret);
        }

        return ret;
    }

    /**
     * Registers a custom Supplier for creating instances of the specified Collection class.
     * 
     * <p>This method allows registering custom suppliers for Collection implementations that are not
     * built-in or require special initialization. Once registered, the supplier will be used by
     * {@link #ofCollection(Class)} when creating instances of the target class.</p>
     * 
     * <p>Note: Built-in classes (like ArrayList, HashSet, etc.) cannot be registered with custom suppliers.</p>
     *
     * @param <T> the Collection type
     * @param targetClass the Class object of the Collection implementation to register
     * @param supplier the Supplier that creates instances of the target class
     * @return true if the registration was successful, false if a supplier was already registered for this class
     * @throws IllegalArgumentException if targetClass or supplier is null, or if targetClass is a built-in class
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Collection> boolean registerForCollection(final Class<T> targetClass, final java.util.function.Supplier<T> supplier)
            throws IllegalArgumentException {
        N.checkArgNotNull(targetClass, cs.targetClass);
        N.checkArgNotNull(supplier, cs.Supplier);

        if (N.isBuiltinClass(targetClass)) {
            throw new IllegalArgumentException("Can't register Supplier with built-in class: " + ClassUtil.getCanonicalClassName(targetClass));
        }

        if (collectionSupplierPool.containsKey(targetClass)) {
            return false;
        }

        return collectionSupplierPool.put(targetClass, Fn.from(supplier)) == null;
    }

    /**
     * Registers a custom Supplier for creating instances of the specified Map class.
     * 
     * <p>This method allows registering custom suppliers for Map implementations that are not
     * built-in or require special initialization. Once registered, the supplier will be used by
     * {@link #ofMap(Class)} when creating instances of the target class.</p>
     * 
     * <p>Note: Built-in classes (like HashMap, TreeMap, etc.) cannot be registered with custom suppliers.</p>
     *
     * @param <T> the Map type
     * @param targetClass the Class object of the Map implementation to register
     * @param supplier the Supplier that creates instances of the target class
     * @return true if the registration was successful, false if a supplier was already registered for this class
     * @throws IllegalArgumentException if targetClass or supplier is null, or if targetClass is a built-in class
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Map> boolean registerForMap(final Class<T> targetClass, final java.util.function.Supplier<T> supplier)
            throws IllegalArgumentException {
        N.checkArgNotNull(targetClass, cs.targetClass);
        N.checkArgNotNull(supplier, cs.Supplier);

        if (N.isBuiltinClass(targetClass)) {
            throw new IllegalArgumentException("Can't register Supplier with built-in class: " + ClassUtil.getCanonicalClassName(targetClass));
        }

        if (mapSupplierPool.containsKey(targetClass)) {
            return false;
        }

        return mapSupplierPool.put(targetClass, Fn.from(supplier)) == null;
    }

    /**
     * Throws UnsupportedOperationException. ImmutableList creation is not supported.
     *
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated unsupported operation.
     */
    @Deprecated
    public static Supplier<ImmutableList<?>> ofImmutableList() {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws UnsupportedOperationException. ImmutableSet creation is not supported.
     *
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated unsupported operation.
     */
    @Deprecated
    public static Supplier<ImmutableSet<?>> ofImmutableSet() {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws UnsupportedOperationException. ImmutableMap creation is not supported.
     *
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated unsupported operation.
     */
    @Deprecated
    public static Supplier<ImmutableMap<?, ?>> ofImmutableMap() {
        throw new UnsupportedOperationException();
    }

    //    /**
    //     *
    //     * @param <T>
    //     * @param <C>
    //     * @param supplier
    //     * @return a stateful {@code Supplier}. Don't save or cache for reuse or use it in parallel stream.
    //     * @deprecated
    //     */
    //    @Deprecated
    //    @SequentialOnly
    //    @Stateful
    //    public static <T, C extends Collection<T>> Supplier<? extends C> single(final java.util.function.Supplier<? extends C> supplier) {
    //        return new Supplier<>() {
    //            private C c = null;
    //
    //            @Override
    //            public C get() {
    //                if (c == null) {
    //                    c = supplier.get();
    //                } else {
    //                    c.clear();
    //                }
    //
    //                return c;
    //            }
    //        };
    //    }

    private static final Supplier<Exception> EXCEPTION = Exception::new;

    /**
     * Returns a Supplier that creates new Exception instances.
     * 
     * <p>Each invocation of the supplier creates a new Exception with no message or cause.</p>
     *
     * @return a Supplier that creates new Exception instances
     */
    @Beta
    public static Supplier<Exception> newException() {
        return EXCEPTION;
    }

    private static final Supplier<RuntimeException> RUNTIME_EXCEPTION = RuntimeException::new;

    /**
     * Returns a Supplier that creates new RuntimeException instances.
     * 
     * <p>Each invocation of the supplier creates a new RuntimeException with no message or cause.</p>
     *
     * @return a Supplier that creates new RuntimeException instances
     */
    @Beta
    public static Supplier<RuntimeException> newRuntimeException() {
        return RUNTIME_EXCEPTION;
    }

    private static final Supplier<NoSuchElementException> NO_SUCH_ELEMENT_EXCEPTION = NoSuchElementException::new;

    /**
     * Returns a Supplier that creates new NoSuchElementException instances.
     * 
     * <p>Each invocation of the supplier creates a new NoSuchElementException with no message.</p>
     *
     * @return a Supplier that creates new NoSuchElementException instances
     */
    @Beta
    public static Supplier<NoSuchElementException> newNoSuchElementException() {
        return NO_SUCH_ELEMENT_EXCEPTION;
    }
}