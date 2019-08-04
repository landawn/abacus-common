/*
 * Copyright (C) 2018 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
 * It's designed to provide a convenient way to parameterized the generic type (e.g. {@code List.<String>class}).
 * <br /> 
 * But the returned Class by all the methods doesn't have the actual parameterized type informations. For example:
 * <pre>
 * <code>
 * List&ltString&gt clazz = Clazz.ofList(String.class);
 * // clazz doesn't have the actual type parameters information. 
 * // you won't be able to get type parameter {@code String} by: cls.getTypeParameters();
 * // To save the real type parameters: you need to either:
 * Type&ltList&ltString&gt&gt type = Type.of("List&ltString&gt"); // or Type.ofList(String.class)
 * 
 * // Or
 * Type&ltList&ltString&gt&gt type = new TypeReference&ltList&ltString&gt&gt() {}.type();
 *
 * </code>
 * </pre>
 * 
 * @since 1.2
 * 
 * @author Haiyang Li
 */
public final class Clazz {

    @SuppressWarnings("rawtypes")
    public static final Class<Map<String, Object>> PROPS_MAP = (Class) Map.class;

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
     * @param cls
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<T> of(Class<? super T> cls) {
        return (Class) cls;
    }

    @SuppressWarnings("rawtypes")
    public static <T> Class<List<T>> ofList() {
        return (Class) List.class;
    }

    @SuppressWarnings("rawtypes")
    public static <T> Class<List<T>> ofList(final Class<T> eleCls) {
        return (Class) List.class;
    }

    @SuppressWarnings("rawtypes")
    public static <T> Class<List<T>> ofLinkedList() {
        return (Class) LinkedList.class;
    }

    @SuppressWarnings("rawtypes")
    public static <T> Class<List<T>> ofLinkedList(final Class<T> eleCls) {
        return (Class) LinkedList.class;
    }

    @SuppressWarnings("rawtypes")
    public static <K, V> Class<List<Map<K, V>>> ofListOfMap(final Class<K> keyCls, final Class<V> valueCls) {
        return (Class) List.class;
    }

    @SuppressWarnings("rawtypes")
    public static <K, V> Class<Set<Map<K, V>>> ofSetOfMap(final Class<K> keyCls, final Class<V> valueCls) {
        return (Class) Set.class;
    }

    @SuppressWarnings("rawtypes")
    public static <T> Class<Set<T>> ofSet() {
        return (Class) Set.class;
    }

    @SuppressWarnings("rawtypes")
    public static <T> Class<Set<T>> ofSet(final Class<T> eleCls) {
        return (Class) Set.class;
    }

    @SuppressWarnings("rawtypes")
    public static <T> Class<Set<T>> ofLinkedHashSet() {
        return (Class) LinkedHashSet.class;
    }

    @SuppressWarnings("rawtypes")
    public static <T> Class<Set<T>> ofLinkedHashSet(final Class<T> eleCls) {
        return (Class) LinkedHashSet.class;
    }

    @SuppressWarnings("rawtypes")
    public static <T> Class<SortedSet<T>> ofSortedSet() {
        return (Class) SortedSet.class;
    }

    @SuppressWarnings("rawtypes")
    public static <T> Class<SortedSet<T>> ofSortedSet(final Class<T> eleCls) {
        return (Class) SortedSet.class;
    }

    @SuppressWarnings("rawtypes")
    public static <T> Class<NavigableSet<T>> ofNavigableSet() {
        return (Class) NavigableSet.class;
    }

    @SuppressWarnings("rawtypes")
    public static <T> Class<NavigableSet<T>> ofNavigableSet(final Class<T> eleCls) {
        return (Class) NavigableSet.class;
    }

    @SuppressWarnings("rawtypes")
    public static <T> Class<NavigableSet<T>> ofTreeSet() {
        return (Class) TreeSet.class;
    }

    @SuppressWarnings("rawtypes")
    public static <T> Class<NavigableSet<T>> ofTreeSet(final Class<T> eleCls) {
        return (Class) TreeSet.class;
    }

    @SuppressWarnings("rawtypes")
    public static <T> Class<Queue<T>> ofQueue() {
        return (Class) Queue.class;
    }

    @SuppressWarnings("rawtypes")
    public static <T> Class<Queue<T>> ofQueue(final Class<T> eleCls) {
        return (Class) Queue.class;
    }

    @SuppressWarnings("rawtypes")
    public static <T> Class<Deque<T>> ofDeque() {
        return (Class) Deque.class;
    }

    @SuppressWarnings("rawtypes")
    public static <T> Class<Deque<T>> ofDeque(final Class<T> eleCls) {
        return (Class) Deque.class;
    }

    @SuppressWarnings("rawtypes")
    public static <T> Class<Deque<T>> ofArrayDeque() {
        return (Class) ArrayDeque.class;
    }

    @SuppressWarnings("rawtypes")
    public static <T> Class<Deque<T>> ofArrayDeque(final Class<T> eleCls) {
        return (Class) ArrayDeque.class;
    }

    @SuppressWarnings("rawtypes")
    public static <T> Class<Queue<T>> ofConcurrentLinkedQueue() {
        return (Class) ConcurrentLinkedQueue.class;
    }

    @SuppressWarnings("rawtypes")
    public static <T> Class<Queue<T>> ofConcurrentLinkedQueue(final Class<T> eleCls) {
        return (Class) ConcurrentLinkedQueue.class;
    }

    @SuppressWarnings("rawtypes")
    public static <T> Class<Queue<T>> ofPriorityQueue() {
        return (Class) PriorityQueue.class;
    }

    @SuppressWarnings("rawtypes")
    public static <T> Class<Queue<T>> ofPriorityQueue(final Class<T> eleCls) {
        return (Class) PriorityQueue.class;
    }

    @SuppressWarnings("rawtypes")
    public static <T> Class<BlockingQueue<T>> ofLinkedBlockingQueue() {
        return (Class) LinkedBlockingQueue.class;
    }

    @SuppressWarnings("rawtypes")
    public static <T> Class<BlockingQueue<T>> ofLinkedBlockingQueue(final Class<T> eleCls) {
        return (Class) LinkedBlockingQueue.class;
    }

    @SuppressWarnings("rawtypes")
    public static <T> Class<Collection<T>> ofCollection() {
        return (Class) Collection.class;
    }

    @SuppressWarnings("rawtypes")
    public static <T> Class<Collection<T>> ofCollection(final Class<T> eleCls) {
        return (Class) Collection.class;
    }

    @SuppressWarnings("rawtypes")
    public static <K, V> Class<Map<K, V>> ofMap() {
        return (Class) Map.class;
    }

    @SuppressWarnings("rawtypes")
    public static <K, V> Class<Map<K, V>> ofMap(final Class<K> keyCls, final Class<V> valueCls) {
        return (Class) Map.class;
    }

    @SuppressWarnings("rawtypes")
    public static <K, V> Class<Map<K, V>> ofLinkedHashMap() {
        return (Class) LinkedHashMap.class;
    }

    @SuppressWarnings("rawtypes")
    public static <K, V> Class<Map<K, V>> ofLinkedHashMap(final Class<K> keyCls, final Class<V> valueCls) {
        return (Class) LinkedHashMap.class;
    }

    @SuppressWarnings("rawtypes")
    public static <K, V> Class<SortedMap<K, V>> ofSortedMap() {
        return (Class) SortedMap.class;
    }

    @SuppressWarnings("rawtypes")
    public static <K, V> Class<SortedMap<K, V>> ofSortedMap(final Class<K> keyCls, final Class<V> valueCls) {
        return (Class) SortedMap.class;
    }

    @SuppressWarnings("rawtypes")
    public static <K, V> Class<NavigableMap<K, V>> ofNavigableMap() {
        return (Class) NavigableMap.class;
    }

    @SuppressWarnings("rawtypes")
    public static <K, V> Class<NavigableMap<K, V>> ofNavigableMap(final Class<K> keyCls, final Class<V> valueCls) {
        return (Class) NavigableMap.class;
    }

    @SuppressWarnings("rawtypes")
    public static <K, V> Class<NavigableMap<K, V>> ofTreeMap() {
        return (Class) TreeMap.class;
    }

    @SuppressWarnings("rawtypes")
    public static <K, V> Class<NavigableMap<K, V>> ofTreeMap(final Class<K> keyCls, final Class<V> valueCls) {
        return (Class) TreeMap.class;
    }

    @SuppressWarnings("rawtypes")
    public static <K, V> Class<ConcurrentMap<K, V>> ofConcurrentMap() {
        return (Class) ConcurrentMap.class;
    }

    @SuppressWarnings("rawtypes")
    public static <K, V> Class<ConcurrentMap<K, V>> ofConcurrentMap(final Class<K> keyCls, final Class<V> valueCls) {
        return (Class) ConcurrentMap.class;
    }

    @SuppressWarnings("rawtypes")
    public static <K, V> Class<ConcurrentMap<K, V>> ofConcurrentHashMap() {
        return (Class) ConcurrentHashMap.class;
    }

    @SuppressWarnings("rawtypes")
    public static <K, V> Class<ConcurrentMap<K, V>> ofConcurrentHashMap(final Class<K> keyCls, final Class<V> valueCls) {
        return (Class) ConcurrentHashMap.class;
    }

    @SuppressWarnings("rawtypes")
    public static <K, V> Class<BiMap<K, V>> ofBiMap() {
        return (Class) BiMap.class;
    }

    @SuppressWarnings("rawtypes")
    public static <K, V> Class<BiMap<K, V>> ofBiMap(final Class<K> keyCls, final Class<V> valueCls) {
        return (Class) BiMap.class;
    }

    @SuppressWarnings("rawtypes")
    public static <T> Class<Multiset<T>> ofMultiset() {
        return (Class) Multiset.class;
    }

    @SuppressWarnings("rawtypes")
    public static <T> Class<Multiset<T>> ofMultiset(final Class<T> eleCls) {
        return (Class) Multiset.class;
    }

    @SuppressWarnings("rawtypes")
    public static <T> Class<LongMultiset<T>> ofLongMultiset() {
        return (Class) LongMultiset.class;
    }

    @SuppressWarnings("rawtypes")
    public static <T> Class<LongMultiset<T>> ofLongMultiset(final Class<T> eleCls) {
        return (Class) LongMultiset.class;
    }

    @SuppressWarnings("rawtypes")
    public static <K, E> Class<ListMultimap<K, E>> ofListMultimap() {
        return (Class) ListMultimap.class;
    }

    @SuppressWarnings("rawtypes")
    public static <K, E> Class<ListMultimap<K, E>> ofListMultimap(final Class<K> keyCls, final Class<E> valueEleCls) {
        return (Class) ListMultimap.class;
    }

    @SuppressWarnings("rawtypes")
    public static <K, E> Class<SetMultimap<K, E>> ofSetMultimap() {
        return (Class) SetMultimap.class;
    }

    @SuppressWarnings("rawtypes")
    public static <K, E> Class<SetMultimap<K, E>> ofSetMultimap(final Class<K> keyCls, final Class<E> valueEleCls) {
        return (Class) SetMultimap.class;
    }
}
