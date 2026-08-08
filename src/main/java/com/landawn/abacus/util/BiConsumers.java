/*
 * Copyright (c) 2026, Haiyang Li.
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

import java.util.Collection;
import java.util.Map;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.SequentialOnly;
import com.landawn.abacus.annotation.Stateful;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.IntBiObjConsumer;

/**
 * Utility class providing various BiConsumer implementations and factory methods.
 * This class contains predefined BiConsumers for common collection and map operations.
 */
public final class BiConsumers {

    /** The Constant DO_NOTHING. */
    @SuppressWarnings("rawtypes")
    private static final BiConsumer DO_NOTHING = (t, u) -> {
        // do nothing.
    };

    /** The Constant ADD. */
    private static final BiConsumer<Collection<Object>, Object> ADD = Collection::add;

    /** The Constant ADD_ALL. */
    private static final BiConsumer<Collection<Object>, Collection<Object>> ADD_ALL = Collection::addAll;

    // /** The Constant ADD_ALL_2. */ // commented out with ofAddAlll (triple-l PrimitiveList marker)
    // @SuppressWarnings("rawtypes")
    // private static final BiConsumer<PrimitiveList, PrimitiveList> ADD_ALL_2 = PrimitiveList::addAll;

    /** The Constant REMOVE. */
    private static final BiConsumer<Collection<Object>, Object> REMOVE = Collection::remove;

    /** The Constant REMOVE_ALL. */
    private static final BiConsumer<Collection<Object>, Collection<Object>> REMOVE_ALL = Collection::removeAll;

    // /** The Constant REMOVE_ALL_2. */ // commented out with ofRemoveAlll (triple-l PrimitiveList marker)
    // @SuppressWarnings("rawtypes")
    // private static final BiConsumer<PrimitiveList, PrimitiveList> REMOVE_ALL_2 = PrimitiveList::removeAll;

    /** The Constant PUT. */
    private static final BiConsumer<Map<Object, Object>, Map.Entry<Object, Object>> PUT = (t, u) -> t.put(u.getKey(), u.getValue());

    /** The Constant PUT_ALL. */
    private static final BiConsumer<Map<Object, Object>, Map<Object, Object>> PUT_ALL = Map::putAll;

    /** The Constant REMOVE_BY_KEY. */
    private static final BiConsumer<Map<Object, Object>, Object> REMOVE_BY_KEY = Map::remove;

    /** The Constant MERGE. */
    private static final BiConsumer<Joiner, Joiner> MERGE = Joiner::merge;

    /** The Constant APPEND. */
    private static final BiConsumer<StringBuilder, Object> APPEND = StringBuilder::append;

    private BiConsumers() {
    }

    /**
     * Returns a BiConsumer that does nothing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiConsumers.doNothing().accept("a", "b");
     * BiConsumers.doNothing().accept(null, null);
     * }</pre>
     *
     * @param <T> the type of the first argument to the consumer
     * @param <U> the type of the second argument to the consumer
     * @return a BiConsumer that performs no operation
     */
    public static <T, U> BiConsumer<T, U> doNothing() {
        return DO_NOTHING;
    }

    /**
     * Returns a BiConsumer that adds an element to a collection.
     * The BiConsumer calls Collection.add(element) on the first argument with the second argument.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiConsumers.<String, java.util.List<String>>ofAdd().accept(new java.util.ArrayList<>(java.util.List.of("a")), "b");        // adds "b" to the list
     * }</pre>
     *
     * @param <T> the type of element to add
     * @param <C> the type of collection
     * @return a BiConsumer that adds the second argument to the first argument collection
     */
    public static <T, C extends Collection<? super T>> BiConsumer<C, T> ofAdd() {
        return (BiConsumer<C, T>) ADD;
    }

    /**
     * Returns a BiConsumer that adds all elements from one collection to another.
     * The BiConsumer calls Collection.addAll(collection) on the first argument with the second argument.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiConsumers.<String, java.util.List<String>>ofAddAll().accept(new java.util.ArrayList<>(java.util.List.of("a")), java.util.List.of("b", "c"));   // adds "b" and "c" to the list
     * }</pre>
     *
     * @param <T> the type of elements in the collections
     * @param <C> the type of collection
     * @return a BiConsumer that adds all elements from the second collection to the first collection
     */
    public static <T, C extends Collection<T>> BiConsumer<C, C> ofAddAll() {
        return (BiConsumer<C, C>) ADD_ALL;
    }

    // /**
    //  * Returns a BiConsumer that adds all elements from one PrimitiveList to another.
    //  * The BiConsumer calls PrimitiveList.addAll(list) on the first argument with the second argument.
    //  *
    //  * <p><b>Usage Examples:</b></p>
    //  * <pre>{@code
    //  * BiConsumers.ofAddAlll().accept(com.landawn.abacus.util.IntList.of(1, 2), com.landawn.abacus.util.IntList.of(3, 4));   // adds 3 and 4 to the first list
    //  * }</pre>
    //  *
    //  * @param <T> the type of PrimitiveList
    //  * @return a BiConsumer that adds all elements from the second PrimitiveList to the first PrimitiveList
    //  */
    // @Beta
    // @SuppressWarnings("rawtypes")
    // public static <T extends PrimitiveList> BiConsumer<T, T> ofAddAlll() {
    //     return (BiConsumer<T, T>) ADD_ALL_2;
    // }

    /**
     * Returns a BiConsumer that removes an element from a collection.
     * The BiConsumer calls Collection.remove(element) on the first argument with the second argument.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiConsumers.<String, java.util.List<String>>ofRemove().accept(new java.util.ArrayList<>(java.util.List.of("a", "b")), "a");   // removes "a" from the list
     * }</pre>
     *
     * @param <T> the type of element to remove
     * @param <C> the type of collection
     * @return a BiConsumer that removes the second argument from the first argument collection
     */
    public static <T, C extends Collection<? super T>> BiConsumer<C, T> ofRemove() {
        return (BiConsumer<C, T>) REMOVE;
    }

    /**
     * Returns a BiConsumer that removes all elements of one collection from another.
     * The BiConsumer calls Collection.removeAll(collection) on the first argument with the second argument.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiConsumers.<String, java.util.List<String>>ofRemoveAll().accept(new java.util.ArrayList<>(java.util.List.of("a", "b")), java.util.List.of("a"));   // removes "a" from the list
     * }</pre>
     *
     * @param <T> the type of elements in the collections
     * @param <C> the type of collection
     * @return a BiConsumer that removes all elements in the second collection from the first collection
     */
    public static <T, C extends Collection<T>> BiConsumer<C, C> ofRemoveAll() {
        return (BiConsumer<C, C>) REMOVE_ALL;
    }

    // /**
    //  * Returns a BiConsumer that removes all elements of one PrimitiveList from another.
    //  * The BiConsumer calls PrimitiveList.removeAll(list) on the first argument with the second argument.
    //  *
    //  * <p><b>Usage Examples:</b></p>
    //  * <pre>{@code
    //  * BiConsumers.ofRemoveAlll().accept(com.landawn.abacus.util.IntList.of(1, 2, 3), com.landawn.abacus.util.IntList.of(2));   // removes 2 from the first list
    //  * }</pre>
    //  *
    //  * @param <T> the type of PrimitiveList
    //  * @return a BiConsumer that removes all elements in the second PrimitiveList from the first PrimitiveList
    //  */
    // @Beta
    // @SuppressWarnings("rawtypes")
    // public static <T extends PrimitiveList> BiConsumer<T, T> ofRemoveAlll() {
    //     return (BiConsumer<T, T>) REMOVE_ALL_2;
    // }

    /**
     * Returns a BiConsumer that puts a Map.Entry into a Map.
     * The BiConsumer extracts the key and value from the entry and puts them into the map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiConsumers.<String, Integer, java.util.Map<String, Integer>, java.util.Map.Entry<String, Integer>>ofPut().accept(new java.util.HashMap<>(), java.util.Map.entry("k", 1));   // adds the entry into the map
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param <M> the type of map
     * @param <E> the type of map entry
     * @return a BiConsumer that puts the entry into the map
     */
    public static <K, V, M extends Map<K, V>, E extends Map.Entry<K, V>> BiConsumer<M, E> ofPut() {
        return (BiConsumer<M, E>) PUT;
    }

    /**
     * Returns a BiConsumer that puts all entries from one map into another.
     * The BiConsumer calls Map.putAll(map) on the first argument with the second argument.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiConsumers.<String, Integer, java.util.Map<String, Integer>>ofPutAll().accept(new java.util.HashMap<>(java.util.Map.of("k", 1)), java.util.Map.of("m", 2));   // adds all entries of the second map into the first
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param <M> the type of map
     * @return a BiConsumer that puts all entries from the second map into the first map
     */
    public static <K, V, M extends Map<K, V>> BiConsumer<M, M> ofPutAll() {
        return (BiConsumer<M, M>) PUT_ALL;
    }

    /**
     * Returns a BiConsumer that removes an entry from a map by key.
     * The BiConsumer calls Map.remove(key) on the first argument with the second argument as the key.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiConsumers.<String, Integer, java.util.Map<String, Integer>>ofRemoveByKey().accept(new java.util.HashMap<>(java.util.Map.of("k", 1)), "k");   // removes the entry with key "k"
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param <M> the type of map
     * @return a BiConsumer that removes the entry with the given key from the map
     */
    public static <K, V, M extends Map<K, V>> BiConsumer<M, K> ofRemoveByKey() {
        return (BiConsumer<M, K>) REMOVE_BY_KEY;
    }

    /**
     * Returns a BiConsumer that merges two Joiner instances.
     * The BiConsumer calls Joiner.merge(joiner) on the first argument with the second argument.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiConsumers.ofMerge().accept(Joiner.with(",").append("a"), Joiner.with(",").append("b"));   // adds the second Joiner's content into the first
     * }</pre>
     *
     * @return a BiConsumer that merges the second Joiner into the first Joiner
     */
    public static BiConsumer<Joiner, Joiner> ofMerge() {
        return MERGE;
    }

    /**
     * Returns a BiConsumer that appends an object to a StringBuilder.
     * The BiConsumer calls StringBuilder.append(object) on the first argument with the second argument.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiConsumers.<String>ofAppend().accept(new StringBuilder("a"), "hello");   // adds "hello" to the StringBuilder
     * }</pre>
     *
     * @param <T> the type of object to append
     * @return a BiConsumer that appends the second argument to the first argument StringBuilder
     */
    public static <T> BiConsumer<StringBuilder, T> ofAppend() {
        return (BiConsumer<StringBuilder, T>) APPEND;
    }

    /**
     * Returns a stateful BiConsumer that accepts elements based on their index position.
     * The consumer maintains an internal counter that increments with each accept call.
     * This method is marked as Beta, SequentialOnly, and Stateful, indicating it should not be saved, cached for reuse, or used in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiConsumers.indexed((i, t, u) -> System.out.println(i)).accept("a", "b");  // prints 0
     * }</pre>
     *
     * @param <T> the type of the first argument to the consumer
     * @param <U> the type of the second argument to the consumer
     * @param action the IntBiObjConsumer that accepts an index and two elements
     * @return a stateful BiConsumer that applies the given IntBiObjConsumer with an incrementing index
     * @throws IllegalArgumentException if {@code action} is {@code null}.
     */
    @Beta
    @SequentialOnly
    @Stateful
    public static <T, U> BiConsumer<T, U> indexed(final IntBiObjConsumer<T, U> action) throws IllegalArgumentException {
        N.checkArgNotNull(action, cs.action);

        return new BiConsumer<>() {
            private final MutableInt idx = new MutableInt(0);

            @Override
            public void accept(final T t, final U u) {
                action.accept(idx.getAndIncrement(), t, u);
            }
        };
    }
}