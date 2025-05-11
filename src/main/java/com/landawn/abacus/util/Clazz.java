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
 * It's designed to provide a convenient way to parameterize the generic type (e.g., {@code List.<String>class}).
 * <br />
 * But the returned Class by all the methods doesn't have the actual parameterized type information. For example:
 * <pre>
 * <code>
 * {@code Class<List<String>> clazz = Clazz.ofList(String.class);}
 * // clazz doesn't have the actual type parameters information.
 * // you won't be able to get type parameter {@code String} by: cls.getTypeParameters();
 * // To save the real type parameters: you need to either:
 * {@code Type<List<String>> type = Type.of("List<String>"); // or Type.ofList(String.class)}
 *
 * // Or
 * Type&lt;List&lt;String&gt;&gt; type = new TypeReference&lt;List&lt;String&gt;&gt;() {}.type();
 *
 * </code>
 * </pre>
 *
 * @see com.landawn.abacus.type.Type
 * @see TypeReference
 * @see TypeReference.TypeToken
 */
@SuppressWarnings("java:S1172")
public final class Clazz {

    @SuppressWarnings("rawtypes")
    public static final Class<Map<String, Object>> PROPS_MAP = (Class) LinkedHashMap.class;

    @SuppressWarnings("rawtypes")
    public static final Class<Map<String, Object>> MAP = (Class) Map.class;

    @SuppressWarnings("rawtypes")
    public static final Class<Map<String, Object>> LINKED_HASH_MAP = (Class) LinkedHashMap.class;

    @SuppressWarnings("rawtypes")
    public static final Class<List<String>> STRING_LIST = (Class) List.class;

    @SuppressWarnings("rawtypes")
    public static final Class<List<Integer>> INTEGER_LIST = (Class) List.class;

    @SuppressWarnings("rawtypes")
    public static final Class<List<Long>> LONG_LIST = (Class) List.class;

    @SuppressWarnings("rawtypes")
    public static final Class<List<Double>> DOUBLE_LIST = (Class) List.class;

    @SuppressWarnings("rawtypes")
    public static final Class<List<Object>> OBJECT_LIST = (Class) List.class;

    @SuppressWarnings("rawtypes")
    public static final Class<Set<String>> STRING_SET = (Class) Set.class;

    @SuppressWarnings("rawtypes")
    public static final Class<Set<Integer>> INTEGER_SET = (Class) Set.class;

    @SuppressWarnings("rawtypes")
    public static final Class<Set<Long>> LONG_SET = (Class) Set.class;

    @SuppressWarnings("rawtypes")
    public static final Class<Set<Double>> DOUBLE_SET = (Class) Set.class;

    @SuppressWarnings("rawtypes")
    public static final Class<Set<Object>> OBJECT_SET = (Class) Set.class;

    private Clazz() {
        // singleton.
    }

    /**
     * Returns the class of the specified type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <T>
     * @param cls
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<T> of(final Class<? super T> cls) {
        return (Class) cls;
    }

    /**
     * Returns the class of {@code List} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <T>
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<List<T>> ofList() {
        return (Class) List.class;
    }

    /**
     * Returns the class of {@code List} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <T>
     * @param eleCls
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<List<T>> ofList(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) List.class;
    }

    /**
     * Returns the class of {@code LinkedList} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <T>
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<List<T>> ofLinkedList() {
        return (Class) LinkedList.class;
    }

    /**
     * Returns the class of {@code LinkedList} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <T>
     * @param eleCls
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<List<T>> ofLinkedList(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) LinkedList.class;
    }

    /**
     * Returns the class of {@code List} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyCls
     * @param valueCls
     * @return
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
     * Returns the class of {@code Set} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyCls
     * @param valueCls
     * @return
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
     * Returns the class of {@code Set} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <T>
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Set<T>> ofSet() {
        return (Class) Set.class;
    }

    /**
     * Returns the class of {@code Set} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <T>
     * @param eleCls
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Set<T>> ofSet(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) Set.class;
    }

    /**
     * Returns the class of {@code LinkedHashSet} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <T>
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Set<T>> ofLinkedHashSet() {
        return (Class) LinkedHashSet.class;
    }

    /**
     * Returns the class of {@code LinkedHashSet} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <T>
     * @param eleCls
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Set<T>> ofLinkedHashSet(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) LinkedHashSet.class;
    }

    /**
     * Returns the class of {@code SortedSet} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <T>
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<SortedSet<T>> ofSortedSet() {
        return (Class) SortedSet.class;
    }

    /**
     * Returns the class of {@code SortedSet} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <T>
     * @param eleCls
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<SortedSet<T>> ofSortedSet(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) SortedSet.class;
    }

    /**
     * Returns the class of {@code NavigableSet} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <T>
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<NavigableSet<T>> ofNavigableSet() {
        return (Class) NavigableSet.class;
    }

    /**
     * Returns the class of {@code NavigableSet} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <T>
     * @param eleCls
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<NavigableSet<T>> ofNavigableSet(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) NavigableSet.class;
    }

    /**
     * Returns the class of {@code TreeSet} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <T>
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<NavigableSet<T>> ofTreeSet() {
        return (Class) TreeSet.class;
    }

    /**
     * Returns the class of {@code TreeSet} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <T>
     * @param eleCls
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<NavigableSet<T>> ofTreeSet(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) TreeSet.class;
    }

    /**
     * Returns the class of {@code Queue} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <T>
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Queue<T>> ofQueue() {
        return (Class) Queue.class;
    }

    /**
     * Returns the class of {@code Queue} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <T>
     * @param eleCls
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Queue<T>> ofQueue(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) Queue.class;
    }

    /**
     * Returns the class of {@code Deque} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <T>
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Deque<T>> ofDeque() {
        return (Class) Deque.class;
    }

    /**
     * Returns the class of {@code Deque} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <T>
     * @param eleCls
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Deque<T>> ofDeque(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) Deque.class;
    }

    /**
     * Returns the class of {@code ArrayDeque} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <T>
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Deque<T>> ofArrayDeque() {
        return (Class) ArrayDeque.class;
    }

    /**
     * Returns the class of {@code ArrayDeque} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <T>
     * @param eleCls
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Deque<T>> ofArrayDeque(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) ArrayDeque.class;
    }

    /**
     * Returns the class of {@code ConcurrentLinkedQueue} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <T>
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Queue<T>> ofConcurrentLinkedQueue() {
        return (Class) ConcurrentLinkedQueue.class;
    }

    /**
     * Returns the class of {@code ConcurrentLinkedQueue} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <T>
     * @param eleCls
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Queue<T>> ofConcurrentLinkedQueue(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) ConcurrentLinkedQueue.class;
    }

    /**
     * Returns the class of {@code PriorityQueue} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <T>
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Queue<T>> ofPriorityQueue() {
        return (Class) PriorityQueue.class;
    }

    /**
     * Returns the class of {@code PriorityQueue} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <T>
     * @param eleCls
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Queue<T>> ofPriorityQueue(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) PriorityQueue.class;
    }

    /**
     * Returns the class of {@code LinkedBlockingQueue} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <T>
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<BlockingQueue<T>> ofLinkedBlockingQueue() {
        return (Class) LinkedBlockingQueue.class;
    }

    /**
     * Returns the class of {@code LinkedBlockingQueue} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <T>
     * @param eleCls
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<BlockingQueue<T>> ofLinkedBlockingQueue(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) LinkedBlockingQueue.class;
    }

    /**
     * Returns the class of {@code Collection} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <T>
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Collection<T>> ofCollection() {
        return (Class) Collection.class;
    }

    /**
     * Returns the class of {@code Collection} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <T>
     * @param eleCls
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Collection<T>> ofCollection(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) Collection.class;
    }

    /**
     * Returns the class of {@code Map} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<Map<K, V>> ofMap() {
        return (Class) Map.class;
    }

    /**
     * Returns the class of {@code Map} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyCls
     * @param valueCls
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<Map<K, V>> ofMap(@SuppressWarnings("unused") final Class<K> keyCls, @SuppressWarnings("unused") final Class<V> valueCls) {
        return (Class) Map.class;
    }

    /**
     * Returns the class of {@code LinkedHashMap} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<Map<K, V>> ofLinkedHashMap() {
        return (Class) LinkedHashMap.class;
    }

    /**
     * Returns the class of {@code LinkedHashMap} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyCls
     * @param valueCls
     * @return
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
     * Returns the class of {@code SortedMap} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<SortedMap<K, V>> ofSortedMap() {
        return (Class) SortedMap.class;
    }

    /**
     * Returns the class of {@code SortedMap} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyCls
     * @param valueCls
     * @return
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
     * Returns the class of {@code NavigableMap} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<NavigableMap<K, V>> ofNavigableMap() {
        return (Class) NavigableMap.class;
    }

    /**
     * Returns the class of {@code NavigableMap} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyCls
     * @param valueCls
     * @return
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
     * Returns the class of {@code TreeMap} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<NavigableMap<K, V>> ofTreeMap() {
        return (Class) TreeMap.class;
    }

    /**
     * Returns the class of {@code TreeMap} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyCls
     * @param valueCls
     * @return
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
     * Returns the class of {@code ConcurrentMap} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<ConcurrentMap<K, V>> ofConcurrentMap() {
        return (Class) ConcurrentMap.class;
    }

    /**
     * Returns the class of {@code ConcurrentMap} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyCls
     * @param valueCls
     * @return
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
     * Returns the class of {@code ConcurrentHashMap} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<ConcurrentMap<K, V>> ofConcurrentHashMap() {
        return (Class) ConcurrentHashMap.class;
    }

    /**
     * Returns the class of {@code ConcurrentHashMap} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyCls
     * @param valueCls
     * @return
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
     * Returns the class of {@code BiMap} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<BiMap<K, V>> ofBiMap() {
        return (Class) BiMap.class;
    }

    /**
     * Returns the class of {@code BiMap} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyCls
     * @param valueCls
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<BiMap<K, V>> ofBiMap(@SuppressWarnings("unused") final Class<K> keyCls, @SuppressWarnings("unused") final Class<V> valueCls) {
        return (Class) BiMap.class;
    }

    /**
     * Returns the class of {@code Multimap} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <T>
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Multiset<T>> ofMultiset() {
        return (Class) Multiset.class;
    }

    /**
     * Returns the class of {@code Multimap} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <T>
     * @param eleCls
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Multiset<T>> ofMultiset(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) Multiset.class;
    }

    /**
     * Returns the class of {@code ListMultimap} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <K> the key type
     * @param <E>
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, E> Class<ListMultimap<K, E>> ofListMultimap() {
        return (Class) ListMultimap.class;
    }

    /**
     * Returns the class of {@code ListMultimap} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <K> the key type
     * @param <E>
     * @param keyCls
     * @param valueEleCls
     * @return
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
     * Returns the class of {@code SetMultimap} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <K> the key type
     * @param <E>
     * @return
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, E> Class<SetMultimap<K, E>> ofSetMultimap() {
        return (Class) SetMultimap.class;
    }

    /**
     * Returns the class of {@code SetMultimap} type. Warning: the returned class doesn't have the actual type parameters information.
     *
     * @param <K> the key type
     * @param <E>
     * @param keyCls
     * @param valueEleCls
     * @return
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
