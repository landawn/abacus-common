/*
 * Copyright (C) 2018 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A utility class that provides a convenient way to parameterize generic types for collection classes.
 * This class is designed to work around Java's type erasure limitation by providing typed Class references
 * for generic collections.
 * 
 * <p><b>Important Note:</b> The Class objects returned by all methods in this utility do NOT contain
 * actual type parameter information due to Java's type erasure. These methods are primarily useful for
 * providing type hints to APIs that accept Class parameters.</p>
 * 
 * <p>For retaining actual type parameter information, consider using:
 * <ul>
 *   <li>{@code Type.of("List<String>")} or {@code Type.ofList(String.class)}</li>
 *   <li>{@code new TypeReference<List<String>>() {}.type()}</li>
 * </ul>
 * </p>
 * 
 * <p>Example usage:
 * <pre>{@code
 * // Get a typed Class reference (without actual type parameters)
 * Class<List<String>> listClass = Clazz.ofList(String.class);
 * 
 * // Use predefined constants
 * Class<Map<String, Object>> propsMap = Clazz.PROPS_MAP;
 * }</pre></p>
 *
 * @see com.landawn.abacus.type.Type
 * @see TypeReference
 * @see TypeReference.TypeToken
 */
@SuppressWarnings("java:S1172")
public final class Clazz {

    /**
     * A constant representing the class type for {@code Map<String, Object>} with LinkedHashMap implementation.
     * Commonly used for properties or configuration maps.
     */
    @SuppressWarnings("rawtypes")
    public static final Class<Map<String, Object>> PROPS_MAP = (Class) LinkedHashMap.class;

    /**
     * A constant representing the class type for {@code Map<String, Object>}.
     * This refers to the Map interface, not a specific implementation.
     */
    @SuppressWarnings("rawtypes")
    public static final Class<Map<String, Object>> MAP = (Class) Map.class;

    /**
     * A constant representing the class type for {@code Map<String, Object>} with LinkedHashMap implementation.
     * Useful when ordering of map entries needs to be preserved.
     */
    @SuppressWarnings("rawtypes")
    public static final Class<Map<String, Object>> LINKED_HASH_MAP = (Class) LinkedHashMap.class;

    /**
     * A constant representing the class type for {@code List<String>}.
     * One of the most commonly used generic list types.
     */
    @SuppressWarnings("rawtypes")
    public static final Class<List<String>> STRING_LIST = (Class) List.class;

    /**
     * A constant representing the class type for {@code List<Integer>}.
     * Commonly used for lists of integer values.
     */
    @SuppressWarnings("rawtypes")
    public static final Class<List<Integer>> INTEGER_LIST = (Class) List.class;

    /**
     * A constant representing the class type for {@code List<Long>}.
     * Commonly used for lists of long integer values.
     */
    @SuppressWarnings("rawtypes")
    public static final Class<List<Long>> LONG_LIST = (Class) List.class;

    /**
     * A constant representing the class type for {@code List<Double>}.
     * Commonly used for lists of floating-point values.
     */
    @SuppressWarnings("rawtypes")
    public static final Class<List<Double>> DOUBLE_LIST = (Class) List.class;

    /**
     * A constant representing the class type for {@code List<Object>}.
     * Used for lists that can contain any type of objects.
     */
    @SuppressWarnings("rawtypes")
    public static final Class<List<Object>> OBJECT_LIST = (Class) List.class;

    /**
     * A constant representing the class type for {@code Set<String>}.
     * Commonly used for unique string collections.
     */
    @SuppressWarnings("rawtypes")
    public static final Class<Set<String>> STRING_SET = (Class) Set.class;

    /**
     * A constant representing the class type for {@code Set<Integer>}.
     * Commonly used for unique integer collections.
     */
    @SuppressWarnings("rawtypes")
    public static final Class<Set<Integer>> INTEGER_SET = (Class) Set.class;

    /**
     * A constant representing the class type for {@code Set<Long>}.
     * Commonly used for unique long integer collections.
     */
    @SuppressWarnings("rawtypes")
    public static final Class<Set<Long>> LONG_SET = (Class) Set.class;

    /**
     * A constant representing the class type for {@code Set<Double>}.
     * Commonly used for unique floating-point collections.
     */
    @SuppressWarnings("rawtypes")
    public static final Class<Set<Double>> DOUBLE_SET = (Class) Set.class;

    /**
     * A constant representing the class type for {@code Set<Object>}.
     * Used for sets that can contain any type of objects.
     */
    @SuppressWarnings("rawtypes")
    public static final Class<Set<Object>> OBJECT_SET = (Class) Set.class;

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private Clazz() {
        // singleton.
    }

    /**
     * Returns a typed Class reference for the specified class.
     * This method provides a convenient way to cast a class to a more specific generic type.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information
     * due to Java's type erasure.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<ArrayList<String>> arrayListClass = Clazz.of(ArrayList.class);
     * }</pre></p>
     *
     * @param <T> the target type
     * @param cls the class to cast
     * @return a typed Class reference
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<T> of(final Class<? super T> cls) {
        return (Class) cls;
    }

    /**
     * Returns a Class reference for {@code List} with unspecified element type.
     * This is equivalent to {@code List.class} but with proper generic typing.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<List<String>> listClass = Clazz.ofList();
     * }</pre></p>
     *
     * @param <T> the element type of the list
     * @return the Class object representing {@code List<T>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<List<T>> ofList() {
        return (Class) List.class;
    }

    /**
     * Returns a Class reference for {@code List} with the specified element type.
     * The element class parameter is used only for type inference and is not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<List<String>> stringListClass = Clazz.ofList(String.class);
     * Class<List<Integer>> intListClass = Clazz.ofList(Integer.class);
     * }</pre></p>
     *
     * @param <T> the element type of the list
     * @param eleCls the class of elements (used only for type inference)
     * @return the Class object representing {@code List<T>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<List<T>> ofList(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) List.class;
    }

    /**
     * Returns a Class reference for {@code LinkedList} with unspecified element type.
     * Useful when you specifically need a LinkedList implementation reference.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<List<String>> linkedListClass = Clazz.ofLinkedList();
     * }</pre></p>
     *
     * @param <T> the element type of the list
     * @return the Class object representing {@code LinkedList<T>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<List<T>> ofLinkedList() {
        return (Class) LinkedList.class;
    }

    /**
     * Returns a Class reference for {@code LinkedList} with the specified element type.
     * The element class parameter is used only for type inference and is not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<List<String>> linkedListClass = Clazz.ofLinkedList(String.class);
     * }</pre></p>
     *
     * @param <T> the element type of the list
     * @param eleCls the class of elements (used only for type inference)
     * @return the Class object representing {@code LinkedList<T>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<List<T>> ofLinkedList(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) LinkedList.class;
    }

    /**
     * Returns a Class reference for {@code List<Map<K, V>>} with the specified key and value types.
     * Useful for representing lists of maps, such as database query results.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<List<Map<String, Object>>> listOfMapsClass = Clazz.ofListOfMap(String.class, Object.class);
     * }</pre></p>
     *
     * @param <K> the key type of the maps in the list
     * @param <V> the value type of the maps in the list
     * @param keyCls the class of map keys (used only for type inference)
     * @param valueCls the class of map values (used only for type inference)
     * @return the Class object representing {@code List<Map<K, V>>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<List<Map<K, V>>> ofListOfMap(@SuppressWarnings("unused") final Class<K> keyCls,
            @SuppressWarnings("unused") final Class<V> valueCls) {
        return (Class) List.class;
    }

    /**
     * Returns a Class reference for {@code Set<Map<K, V>>} with the specified key and value types.
     * Useful for representing unique collections of maps.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<Set<Map<String, Integer>>> setOfMapsClass = Clazz.ofSetOfMap(String.class, Integer.class);
     * }</pre></p>
     *
     * @param <K> the key type of the maps in the set
     * @param <V> the value type of the maps in the set
     * @param keyCls the class of map keys (used only for type inference)
     * @param valueCls the class of map values (used only for type inference)
     * @return the Class object representing {@code Set<Map<K, V>>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<Set<Map<K, V>>> ofSetOfMap(@SuppressWarnings("unused") final Class<K> keyCls,
            @SuppressWarnings("unused") final Class<V> valueCls) {
        return (Class) Set.class;
    }

    /**
     * Returns a Class reference for {@code Set} with unspecified element type.
     * This is equivalent to {@code Set.class} but with proper generic typing.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<Set<String>> setClass = Clazz.ofSet();
     * }</pre></p>
     *
     * @param <T> the element type of the set
     * @return the Class object representing {@code Set<T>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Set<T>> ofSet() {
        return (Class) Set.class;
    }

    /**
     * Returns a Class reference for {@code Set} with the specified element type.
     * The element class parameter is used only for type inference and is not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<Set<String>> stringSetClass = Clazz.ofSet(String.class);
     * Class<Set<Integer>> intSetClass = Clazz.ofSet(Integer.class);
     * }</pre></p>
     *
     * @param <T> the element type of the set
     * @param eleCls the class of elements (used only for type inference)
     * @return the Class object representing {@code Set<T>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Set<T>> ofSet(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) Set.class;
    }

    /**
     * Returns a Class reference for {@code LinkedHashSet} with unspecified element type.
     * Useful when you need a Set that preserves insertion order.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<Set<String>> linkedHashSetClass = Clazz.ofLinkedHashSet();
     * }</pre></p>
     *
     * @param <T> the element type of the set
     * @return the Class object representing {@code LinkedHashSet<T>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Set<T>> ofLinkedHashSet() {
        return (Class) LinkedHashSet.class;
    }

    /**
     * Returns a Class reference for {@code LinkedHashSet} with the specified element type.
     * The element class parameter is used only for type inference and is not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<Set<String>> linkedHashSetClass = Clazz.ofLinkedHashSet(String.class);
     * }</pre></p>
     *
     * @param <T> the element type of the set
     * @param eleCls the class of elements (used only for type inference)
     * @return the Class object representing {@code LinkedHashSet<T>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Set<T>> ofLinkedHashSet(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) LinkedHashSet.class;
    }

    /**
     * Returns a Class reference for {@code SortedSet} with unspecified element type.
     * SortedSet maintains elements in sorted order according to their natural ordering or a comparator.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<SortedSet<String>> sortedSetClass = Clazz.ofSortedSet();
     * }</pre></p>
     *
     * @param <T> the element type of the sorted set
     * @return the Class object representing {@code SortedSet<T>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<SortedSet<T>> ofSortedSet() {
        return (Class) SortedSet.class;
    }

    /**
     * Returns a Class reference for {@code SortedSet} with the specified element type.
     * The element class parameter is used only for type inference and is not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<SortedSet<Integer>> sortedIntSetClass = Clazz.ofSortedSet(Integer.class);
     * }</pre></p>
     *
     * @param <T> the element type of the sorted set
     * @param eleCls the class of elements (used only for type inference)
     * @return the Class object representing {@code SortedSet<T>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<SortedSet<T>> ofSortedSet(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) SortedSet.class;
    }

    /**
     * Returns a Class reference for {@code NavigableSet} with unspecified element type.
     * NavigableSet extends SortedSet with navigation methods for finding closest matches.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<NavigableSet<String>> navigableSetClass = Clazz.ofNavigableSet();
     * }</pre></p>
     *
     * @param <T> the element type of the navigable set
     * @return the Class object representing {@code NavigableSet<T>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<NavigableSet<T>> ofNavigableSet() {
        return (Class) NavigableSet.class;
    }

    /**
     * Returns a Class reference for {@code NavigableSet} with the specified element type.
     * The element class parameter is used only for type inference and is not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<NavigableSet<Double>> navigableDoubleSetClass = Clazz.ofNavigableSet(Double.class);
     * }</pre></p>
     *
     * @param <T> the element type of the navigable set
     * @param eleCls the class of elements (used only for type inference)
     * @return the Class object representing {@code NavigableSet<T>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<NavigableSet<T>> ofNavigableSet(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) NavigableSet.class;
    }

    /**
     * Returns a Class reference for {@code TreeSet} with unspecified element type.
     * TreeSet is a NavigableSet implementation based on a TreeMap.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<NavigableSet<String>> treeSetClass = Clazz.ofTreeSet();
     * }</pre></p>
     *
     * @param <T> the element type of the tree set
     * @return the Class object representing {@code TreeSet<T>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<NavigableSet<T>> ofTreeSet() {
        return (Class) TreeSet.class;
    }

    /**
     * Returns a Class reference for {@code TreeSet} with the specified element type.
     * The element class parameter is used only for type inference and is not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<NavigableSet<String>> treeSetClass = Clazz.ofTreeSet(String.class);
     * }</pre></p>
     *
     * @param <T> the element type of the tree set
     * @param eleCls the class of elements (used only for type inference)
     * @return the Class object representing {@code TreeSet<T>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<NavigableSet<T>> ofTreeSet(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) TreeSet.class;
    }

    /**
     * Returns a Class reference for {@code Queue} with unspecified element type.
     * Queue represents a collection designed for holding elements prior to processing.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<Queue<String>> queueClass = Clazz.ofQueue();
     * }</pre></p>
     *
     * @param <T> the element type of the queue
     * @return the Class object representing {@code Queue<T>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Queue<T>> ofQueue() {
        return (Class) Queue.class;
    }

    /**
     * Returns a Class reference for {@code Queue} with the specified element type.
     * The element class parameter is used only for type inference and is not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<Queue<Task>> taskQueueClass = Clazz.ofQueue(Task.class);
     * }</pre></p>
     *
     * @param <T> the element type of the queue
     * @param eleCls the class of elements (used only for type inference)
     * @return the Class object representing {@code Queue<T>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Queue<T>> ofQueue(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) Queue.class;
    }

    /**
     * Returns a Class reference for {@code Deque} with unspecified element type.
     * Deque (double-ended queue) supports element insertion and removal at both ends.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<Deque<String>> dequeClass = Clazz.ofDeque();
     * }</pre></p>
     *
     * @param <T> the element type of the deque
     * @return the Class object representing {@code Deque<T>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Deque<T>> ofDeque() {
        return (Class) Deque.class;
    }

    /**
     * Returns a Class reference for {@code Deque} with the specified element type.
     * The element class parameter is used only for type inference and is not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<Deque<Integer>> intDequeClass = Clazz.ofDeque(Integer.class);
     * }</pre></p>
     *
     * @param <T> the element type of the deque
     * @param eleCls the class of elements (used only for type inference)
     * @return the Class object representing {@code Deque<T>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Deque<T>> ofDeque(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) Deque.class;
    }

    /**
     * Returns a Class reference for {@code ArrayDeque} with unspecified element type.
     * ArrayDeque is a resizable-array implementation of the Deque interface.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<Deque<String>> arrayDequeClass = Clazz.ofArrayDeque();
     * }</pre></p>
     *
     * @param <T> the element type of the array deque
     * @return the Class object representing {@code ArrayDeque<T>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Deque<T>> ofArrayDeque() {
        return (Class) ArrayDeque.class;
    }

    /**
     * Returns a Class reference for {@code ArrayDeque} with the specified element type.
     * The element class parameter is used only for type inference and is not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<Deque<String>> arrayDequeClass = Clazz.ofArrayDeque(String.class);
     * }</pre></p>
     *
     * @param <T> the element type of the array deque
     * @param eleCls the class of elements (used only for type inference)
     * @return the Class object representing {@code ArrayDeque<T>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Deque<T>> ofArrayDeque(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) ArrayDeque.class;
    }

    /**
     * Returns a Class reference for {@code ConcurrentLinkedQueue} with unspecified element type.
     * ConcurrentLinkedQueue is an unbounded thread-safe queue based on linked nodes.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<Queue<String>> concurrentQueueClass = Clazz.ofConcurrentLinkedQueue();
     * }</pre></p>
     *
     * @param <T> the element type of the concurrent queue
     * @return the Class object representing {@code ConcurrentLinkedQueue<T>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Queue<T>> ofConcurrentLinkedQueue() {
        return (Class) ConcurrentLinkedQueue.class;
    }

    /**
     * Returns a Class reference for {@code ConcurrentLinkedQueue} with the specified element type.
     * The element class parameter is used only for type inference and is not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<Queue<Message>> messageQueueClass = Clazz.ofConcurrentLinkedQueue(Message.class);
     * }</pre></p>
     *
     * @param <T> the element type of the concurrent queue
     * @param eleCls the class of elements (used only for type inference)
     * @return the Class object representing {@code ConcurrentLinkedQueue<T>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Queue<T>> ofConcurrentLinkedQueue(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) ConcurrentLinkedQueue.class;
    }

    /**
     * Returns a Class reference for {@code PriorityQueue} with unspecified element type.
     * PriorityQueue is an unbounded priority queue based on a priority heap.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<Queue<Task>> priorityQueueClass = Clazz.ofPriorityQueue();
     * }</pre></p>
     *
     * @param <T> the element type of the priority queue
     * @return the Class object representing {@code PriorityQueue<T>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Queue<T>> ofPriorityQueue() {
        return (Class) PriorityQueue.class;
    }

    /**
     * Returns a Class reference for {@code PriorityQueue} with the specified element type.
     * The element class parameter is used only for type inference and is not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<Queue<Integer>> intPriorityQueueClass = Clazz.ofPriorityQueue(Integer.class);
     * }</pre></p>
     *
     * @param <T> the element type of the priority queue
     * @param eleCls the class of elements (used only for type inference)
     * @return the Class object representing {@code PriorityQueue<T>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Queue<T>> ofPriorityQueue(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) PriorityQueue.class;
    }

    /**
     * Returns a Class reference for {@code LinkedBlockingQueue} with unspecified element type.
     * LinkedBlockingQueue is an optionally-bounded blocking queue based on linked nodes.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<BlockingQueue<String>> blockingQueueClass = Clazz.ofLinkedBlockingQueue();
     * }</pre></p>
     *
     * @param <T> the element type of the blocking queue
     * @return the Class object representing {@code LinkedBlockingQueue<T>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<BlockingQueue<T>> ofLinkedBlockingQueue() {
        return (Class) LinkedBlockingQueue.class;
    }

    /**
     * Returns a Class reference for {@code LinkedBlockingQueue} with the specified element type.
     * The element class parameter is used only for type inference and is not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<BlockingQueue<Task>> taskQueueClass = Clazz.ofLinkedBlockingQueue(Task.class);
     * }</pre></p>
     *
     * @param <T> the element type of the blocking queue
     * @param eleCls the class of elements (used only for type inference)
     * @return the Class object representing {@code LinkedBlockingQueue<T>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<BlockingQueue<T>> ofLinkedBlockingQueue(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) LinkedBlockingQueue.class;
    }

    /**
     * Returns a Class reference for {@code Collection} with unspecified element type.
     * Collection is the root interface in the collection hierarchy.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<Collection<String>> collectionClass = Clazz.ofCollection();
     * }</pre></p>
     *
     * @param <T> the element type of the collection
     * @return the Class object representing {@code Collection<T>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Collection<T>> ofCollection() {
        return (Class) Collection.class;
    }

    /**
     * Returns a Class reference for {@code Collection} with the specified element type.
     * The element class parameter is used only for type inference and is not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<Collection<User>> userCollectionClass = Clazz.ofCollection(User.class);
     * }</pre></p>
     *
     * @param <T> the element type of the collection
     * @param eleCls the class of elements (used only for type inference)
     * @return the Class object representing {@code Collection<T>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Collection<T>> ofCollection(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) Collection.class;
    }

    /**
     * Returns a Class reference for {@code Map} with unspecified key and value types.
     * This is equivalent to {@code Map.class} but with proper generic typing.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<Map<String, Object>> mapClass = Clazz.ofMap();
     * }</pre></p>
     *
     * @param <K> the key type of the map
     * @param <V> the value type of the map
     * @return the Class object representing {@code Map<K, V>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<Map<K, V>> ofMap() {
        return (Class) Map.class;
    }

    /**
     * Returns a Class reference for {@code Map} with the specified key and value types.
     * The key and value class parameters are used only for type inference and are not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<Map<String, Integer>> stringIntMapClass = Clazz.ofMap(String.class, Integer.class);
     * Class<Map<Long, User>> userMapClass = Clazz.ofMap(Long.class, User.class);
     * }</pre></p>
     *
     * @param <K> the key type of the map
     * @param <V> the value type of the map
     * @param keyCls the class of map keys (used only for type inference)
     * @param valueCls the class of map values (used only for type inference)
     * @return the Class object representing {@code Map<K, V>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<Map<K, V>> ofMap(@SuppressWarnings("unused") final Class<K> keyCls, @SuppressWarnings("unused") final Class<V> valueCls) {
        return (Class) Map.class;
    }

    /**
     * Returns a Class reference for {@code LinkedHashMap} with unspecified key and value types.
     * LinkedHashMap maintains insertion order of entries.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<Map<String, Object>> linkedMapClass = Clazz.ofLinkedHashMap();
     * }</pre></p>
     *
     * @param <K> the key type of the map
     * @param <V> the value type of the map
     * @return the Class object representing {@code LinkedHashMap<K, V>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<Map<K, V>> ofLinkedHashMap() {
        return (Class) LinkedHashMap.class;
    }

    /**
     * Returns a Class reference for {@code LinkedHashMap} with the specified key and value types.
     * The key and value class parameters are used only for type inference and are not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<Map<String, Object>> propsMapClass = Clazz.ofLinkedHashMap(String.class, Object.class);
     * }</pre></p>
     *
     * @param <K> the key type of the map
     * @param <V> the value type of the map
     * @param keyCls the class of map keys (used only for type inference)
     * @param valueCls the class of map values (used only for type inference)
     * @return the Class object representing {@code LinkedHashMap<K, V>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<Map<K, V>> ofLinkedHashMap(@SuppressWarnings("unused") final Class<K> keyCls,
            @SuppressWarnings("unused") final Class<V> valueCls) {
        return (Class) LinkedHashMap.class;
    }

    /**
     * Returns a Class reference for {@code SortedMap} with unspecified key and value types.
     * SortedMap maintains mappings in ascending key order.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<SortedMap<String, Integer>> sortedMapClass = Clazz.ofSortedMap();
     * }</pre></p>
     *
     * @param <K> the key type of the sorted map
     * @param <V> the value type of the sorted map
     * @return the Class object representing {@code SortedMap<K, V>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<SortedMap<K, V>> ofSortedMap() {
        return (Class) SortedMap.class;
    }

    /**
     * Returns a Class reference for {@code SortedMap} with the specified key and value types.
     * The key and value class parameters are used only for type inference and are not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<SortedMap<Integer, String>> sortedMapClass = Clazz.ofSortedMap(Integer.class, String.class);
     * }</pre></p>
     *
     * @param <K> the key type of the sorted map
     * @param <V> the value type of the sorted map
     * @param keyCls the class of map keys (used only for type inference)
     * @param valueCls the class of map values (used only for type inference)
     * @return the Class object representing {@code SortedMap<K, V>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<SortedMap<K, V>> ofSortedMap(@SuppressWarnings("unused") final Class<K> keyCls,
            @SuppressWarnings("unused") final Class<V> valueCls) {
        return (Class) SortedMap.class;
    }

    /**
     * Returns a Class reference for {@code NavigableMap} with unspecified key and value types.
     * NavigableMap extends SortedMap with navigation methods returning the closest matches for given search targets.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<NavigableMap<String, Object>> navigableMapClass = Clazz.ofNavigableMap();
     * }</pre></p>
     *
     * @param <K> the key type of the navigable map
     * @param <V> the value type of the navigable map
     * @return the Class object representing {@code NavigableMap<K, V>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<NavigableMap<K, V>> ofNavigableMap() {
        return (Class) NavigableMap.class;
    }

    /**
     * Returns a Class reference for {@code NavigableMap} with the specified key and value types.
     * The key and value class parameters are used only for type inference and are not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<NavigableMap<Date, Event>> eventMapClass = Clazz.ofNavigableMap(Date.class, Event.class);
     * }</pre></p>
     *
     * @param <K> the key type of the navigable map
     * @param <V> the value type of the navigable map
     * @param keyCls the class of map keys (used only for type inference)
     * @param valueCls the class of map values (used only for type inference)
     * @return the Class object representing {@code NavigableMap<K, V>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<NavigableMap<K, V>> ofNavigableMap(@SuppressWarnings("unused") final Class<K> keyCls,
            @SuppressWarnings("unused") final Class<V> valueCls) {
        return (Class) NavigableMap.class;
    }

    /**
     * Returns a Class reference for {@code TreeMap} with unspecified key and value types.
     * TreeMap is a Red-Black tree based NavigableMap implementation.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<NavigableMap<String, Integer>> treeMapClass = Clazz.ofTreeMap();
     * }</pre></p>
     *
     * @param <K> the key type of the tree map
     * @param <V> the value type of the tree map
     * @return the Class object representing {@code TreeMap<K, V>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<NavigableMap<K, V>> ofTreeMap() {
        return (Class) TreeMap.class;
    }

    /**
     * Returns a Class reference for {@code TreeMap} with the specified key and value types.
     * The key and value class parameters are used only for type inference and are not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<NavigableMap<String, List<String>>> treeMapClass = Clazz.ofTreeMap(String.class, List.class);
     * }</pre></p>
     *
     * @param <K> the key type of the tree map
     * @param <V> the value type of the tree map
     * @param keyCls the class of map keys (used only for type inference)
     * @param valueCls the class of map values (used only for type inference)
     * @return the Class object representing {@code TreeMap<K, V>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<NavigableMap<K, V>> ofTreeMap(@SuppressWarnings("unused") final Class<K> keyCls,
            @SuppressWarnings("unused") final Class<V> valueCls) {
        return (Class) TreeMap.class;
    }

    /**
     * Returns a Class reference for {@code ConcurrentMap} with unspecified key and value types.
     * ConcurrentMap provides thread-safe operations on a map.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<ConcurrentMap<String, Object>> concurrentMapClass = Clazz.ofConcurrentMap();
     * }</pre></p>
     *
     * @param <K> the key type of the concurrent map
     * @param <V> the value type of the concurrent map
     * @return the Class object representing {@code ConcurrentMap<K, V>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<ConcurrentMap<K, V>> ofConcurrentMap() {
        return (Class) ConcurrentMap.class;
    }

    /**
     * Returns a Class reference for {@code ConcurrentMap} with the specified key and value types.
     * The key and value class parameters are used only for type inference and are not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<ConcurrentMap<Long, Session>> sessionMapClass = Clazz.ofConcurrentMap(Long.class, Session.class);
     * }</pre></p>
     *
     * @param <K> the key type of the concurrent map
     * @param <V> the value type of the concurrent map
     * @param keyCls the class of map keys (used only for type inference)
     * @param valueCls the class of map values (used only for type inference)
     * @return the Class object representing {@code ConcurrentMap<K, V>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<ConcurrentMap<K, V>> ofConcurrentMap(@SuppressWarnings("unused") final Class<K> keyCls,
            @SuppressWarnings("unused") final Class<V> valueCls) {
        return (Class) ConcurrentMap.class;
    }

    /**
     * Returns a Class reference for {@code ConcurrentHashMap} with unspecified key and value types.
     * ConcurrentHashMap is a hash table supporting full concurrency of retrievals and high expected concurrency for updates.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<ConcurrentMap<String, Object>> concurrentHashMapClass = Clazz.ofConcurrentHashMap();
     * }</pre></p>
     *
     * @param <K> the key type of the concurrent hash map
     * @param <V> the value type of the concurrent hash map
     * @return the Class object representing {@code ConcurrentHashMap<K, V>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<ConcurrentMap<K, V>> ofConcurrentHashMap() {
        return (Class) ConcurrentHashMap.class;
    }

    /**
     * Returns a Class reference for {@code ConcurrentHashMap} with the specified key and value types.
     * The key and value class parameters are used only for type inference and are not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<ConcurrentMap<String, AtomicInteger>> counterMapClass = 
     *     Clazz.ofConcurrentHashMap(String.class, AtomicInteger.class);
     * }</pre></p>
     *
     * @param <K> the key type of the concurrent hash map
     * @param <V> the value type of the concurrent hash map
     * @param keyCls the class of map keys (used only for type inference)
     * @param valueCls the class of map values (used only for type inference)
     * @return the Class object representing {@code ConcurrentHashMap<K, V>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<ConcurrentMap<K, V>> ofConcurrentHashMap(@SuppressWarnings("unused") final Class<K> keyCls,
            @SuppressWarnings("unused") final Class<V> valueCls) {
        return (Class) ConcurrentHashMap.class;
    }
    /**
     * Returns a Class reference for {@code BiMap} with unspecified key and value types.
     * BiMap is a bidirectional map that preserves the uniqueness of its values as well as that of its keys.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<BiMap<String, Integer>> biMapClass = Clazz.ofBiMap();
     * }</pre></p>
     *
     * @param <K> the key type of the bimap
     * @param <V> the value type of the bimap
     * @return the Class object representing {@code BiMap<K, V>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<BiMap<K, V>> ofBiMap() {
        return (Class) BiMap.class;
    }

    /**
     * Returns a Class reference for {@code BiMap} with the specified key and value types.
     * The key and value class parameters are used only for type inference and are not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<BiMap<String, Integer>> idMapClass = Clazz.ofBiMap(String.class, Integer.class);
     * }</pre></p>
     *
     * @param <K> the key type of the bimap
     * @param <V> the value type of the bimap
     * @param keyCls the class of map keys (used only for type inference)
     * @param valueCls the class of map values (used only for type inference)
     * @return the Class object representing {@code BiMap<K, V>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<BiMap<K, V>> ofBiMap(@SuppressWarnings("unused") final Class<K> keyCls, @SuppressWarnings("unused") final Class<V> valueCls) {
        return (Class) BiMap.class;
    }

    /**
     * Returns a Class reference for {@code Multiset} with unspecified element type.
     * Multiset is a collection that supports order-independent equality, like Set, but may have duplicate elements.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<Multiset<String>> multisetClass = Clazz.ofMultiset();
     * }</pre></p>
     *
     * @param <T> the element type of the multiset
     * @return the Class object representing {@code Multiset<T>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Multiset<T>> ofMultiset() {
        return (Class) Multiset.class;
    }

    /**
     * Returns a Class reference for {@code Multiset} with the specified element type.
     * The element class parameter is used only for type inference and is not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<Multiset<String>> wordCountClass = Clazz.ofMultiset(String.class);
     * }</pre></p>
     *
     * @param <T> the element type of the multiset
     * @param eleCls the class of elements (used only for type inference)
     * @return the Class object representing {@code Multiset<T>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Multiset<T>> ofMultiset(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) Multiset.class;
    }

    /**
     * Returns a Class reference for {@code ListMultimap} with unspecified key and value element types.
     * ListMultimap is a multimap that can hold multiple values per key in a List collection.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<ListMultimap<String, Integer>> multiMapClass = Clazz.ofListMultimap();
     * }</pre></p>
     *
     * @param <K> the key type of the list multimap
     * @param <E> the element type of the value collections
     * @return the Class object representing {@code ListMultimap<K, E>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, E> Class<ListMultimap<K, E>> ofListMultimap() {
        return (Class) ListMultimap.class;
    }

    /**
     * Returns a Class reference for {@code ListMultimap} with the specified key and value element types.
     * The key and value element class parameters are used only for type inference and are not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<ListMultimap<String, Integer>> groupedDataClass = 
     *     Clazz.ofListMultimap(String.class, Integer.class);
     * }</pre></p>
     *
     * @param <K> the key type of the list multimap
     * @param <E> the element type of the value collections
     * @param keyCls the class of map keys (used only for type inference)
     * @param valueEleCls the class of value collection elements (used only for type inference)
     * @return the Class object representing {@code ListMultimap<K, E>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, E> Class<ListMultimap<K, E>> ofListMultimap(@SuppressWarnings("unused") final Class<K> keyCls,
            @SuppressWarnings("unused") final Class<E> valueEleCls) {
        return (Class) ListMultimap.class;
    }

    /**
     * Returns a Class reference for {@code SetMultimap} with unspecified key and value element types.
     * SetMultimap is a multimap that can hold multiple unique values per key in a Set collection.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<SetMultimap<String, Integer>> multiMapClass = Clazz.ofSetMultimap();
     * }</pre></p>
     *
     * @param <K> the key type of the set multimap
     * @param <E> the element type of the value collections
     * @return the Class object representing {@code SetMultimap<K, E>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, E> Class<SetMultimap<K, E>> ofSetMultimap() {
        return (Class) SetMultimap.class;
    }

    /**
     * Returns a Class reference for {@code SetMultimap} with the specified key and value element types.
     * The key and value element class parameters are used only for type inference and are not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * Class<SetMultimap<String, String>> tagMapClass = 
     *     Clazz.ofSetMultimap(String.class, String.class);
     * }</pre></p>
     *
     * @param <K> the key type of the set multimap
     * @param <E> the element type of the value collections
     * @param keyCls the class of map keys (used only for type inference)
     * @param valueEleCls the class of value collection elements (used only for type inference)
     * @return the Class object representing {@code SetMultimap<K, E>}
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, E> Class<SetMultimap<K, E>> ofSetMultimap(@SuppressWarnings("unused") final Class<K> keyCls,
            @SuppressWarnings("unused") final Class<E> valueEleCls) {
        return (Class) SetMultimap.class;
    }
}
