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
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.IntBiObjFunction;

/**
 * Utility class providing various BiFunction implementations and factory methods.
 * This class contains predefined BiFunctions for common collection and map operations.
 */
public final class BiFunctions {

    /** The Constant RETURN_FIRST. */
    private static final BiFunction<Object, Object, Object> RETURN_FIRST = (t, u) -> t;

    /** The Constant RETURN_SECOND. */
    private static final BiFunction<Object, Object, Object> RETURN_SECOND = (t, u) -> u;

    /** The Constant ADD. */
    private static final BiFunction<Collection<Object>, Object, Collection<Object>> ADD = (t, u) -> {
        t.add(u);
        return t;
    };

    /** The Constant ADD_ALL. */
    private static final BiFunction<Collection<Object>, Collection<Object>, Collection<Object>> ADD_ALL = (t, u) -> {
        t.addAll(u);
        return t;
    };

    // /** The Constant ADD_ALL_2. */ // commented out with ofAddAlll (triple-l PrimitiveList marker)
    // @SuppressWarnings("rawtypes")
    // private static final BiFunction<PrimitiveList, PrimitiveList, PrimitiveList> ADD_ALL_2 = (t, u) -> {
    //     t.addAll(u);
    //     return t;
    // };

    /** The Constant REMOVE. */
    private static final BiFunction<Collection<Object>, Object, Collection<Object>> REMOVE = (t, u) -> {
        t.remove(u);
        return t;
    };

    /** The Constant REMOVE_ALL. */
    private static final BiFunction<Collection<Object>, Collection<Object>, Collection<Object>> REMOVE_ALL = (t, u) -> {
        t.removeAll(u);
        return t;
    };

    // /** The Constant REMOVE_ALL_2. */ // commented out with ofRemoveAlll (triple-l PrimitiveList marker)
    // @SuppressWarnings("rawtypes")
    // private static final BiFunction<PrimitiveList, PrimitiveList, PrimitiveList> REMOVE_ALL_2 = (t, u) -> {
    //     t.removeAll(u);
    //     return t;
    // };

    /** The Constant PUT. */
    private static final BiFunction<Map<Object, Object>, Map.Entry<Object, Object>, Map<Object, Object>> PUT = (t, u) -> {
        t.put(u.getKey(), u.getValue());
        return t;
    };

    /** The Constant PUT_ALL. */
    private static final BiFunction<Map<Object, Object>, Map<Object, Object>, Map<Object, Object>> PUT_ALL = (t, u) -> {
        t.putAll(u);
        return t;
    };

    /** The Constant REMOVE_BY_KEY. */
    private static final BiFunction<Map<Object, Object>, Object, Map<Object, Object>> REMOVE_BY_KEY = (t, u) -> {
        t.remove(u);
        return t;
    };

    /** The Constant MERGE. */
    private static final BiFunction<Joiner, Joiner, Joiner> MERGE = Joiner::merge;

    /** The Constant APPEND. */
    private static final BiFunction<StringBuilder, Object, StringBuilder> APPEND = StringBuilder::append;

    private BiFunctions() {
    }

    /**
     * Returns a BiFunction that always returns the first argument.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiFunctions.selectFirst().apply("first","second");   // returns "first"
     * BiFunctions.selectFirst().apply(1,2);                // returns 1
     * }</pre>
     *
     * @param <T> the type of the first argument and result
     * @param <U> the type of the second argument
     * @return a BiFunction that returns the first argument
     */
    public static <T, U> BiFunction<T, U, T> selectFirst() {
        return (BiFunction<T, U, T>) RETURN_FIRST;
    }

    /**
     * Returns a BiFunction that always returns the second argument.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiFunctions.selectSecond().apply("first","second");   // returns "second"
     * BiFunctions.selectSecond().apply(1,2);                // returns 2
     * }</pre>
     *
     * @param <T> the type of the first argument
     * @param <U> the type of the second argument and result
     * @return a BiFunction that returns the second argument
     */
    public static <T, U> BiFunction<T, U, U> selectSecond() {
        return (BiFunction<T, U, U>) RETURN_SECOND;
    }

    /**
     * Returns a BiFunction that adds an element to a collection and returns the collection.
     * The BiFunction calls Collection.add(element) and returns the modified collection.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiFunctions.<String, java.util.List<String>>ofAdd().apply(new java.util.ArrayList<>(java.util.List.of("a")), "b");        // adds "b", returns the list
     * }</pre>
     *
     * @param <T> the type of element to add
     * @param <C> the type of collection
     * @return a BiFunction that adds the second argument to the first argument collection and returns the collection
     */
    public static <T, C extends Collection<? super T>> BiFunction<C, T, C> ofAdd() {
        return (BiFunction<C, T, C>) ADD;
    }

    /**
     * Returns a BiFunction that adds all elements from one collection to another and returns the target collection.
     * The BiFunction calls Collection.addAll(collection) and returns the modified collection.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiFunctions.<String, java.util.List<String>>ofAddAll().apply(new java.util.ArrayList<>(java.util.List.of("a")), new java.util.ArrayList<>(java.util.List.of("b")));   // adds all, returns the first list
     * }</pre>
     *
     * @param <T> the type of elements in the collections
     * @param <C> the type of collection
     * @return a BiFunction that adds all elements from the second collection to the first and returns the first collection
     */
    public static <T, C extends Collection<T>> BiFunction<C, C, C> ofAddAll() {
        return (BiFunction<C, C, C>) ADD_ALL;
    }

    // /**
    //  * Returns a BiFunction that adds all elements from one PrimitiveList to another and returns the target list.
    //  * The BiFunction calls PrimitiveList.addAll(list) and returns the modified list.
    //  *
    //  * <p><b>Usage Examples:</b></p>
    //  * <pre>{@code
    //  * BiFunctions.ofAddAlll().apply(com.landawn.abacus.util.IntList.of(1, 2), com.landawn.abacus.util.IntList.of(3, 4));   // adds all primitives, returns the first list
    //  * }</pre>
    //  *
    //  * @param <T> the type of PrimitiveList
    //  * @return a BiFunction that adds all elements from the second PrimitiveList to the first and returns the first list
    //  */
    // @Beta
    // @SuppressWarnings("rawtypes")
    // public static <T extends PrimitiveList> BiFunction<T, T, T> ofAddAlll() {
    //     return (BiFunction<T, T, T>) ADD_ALL_2;
    // }

    /**
     * Returns a BiFunction that removes an element from a collection and returns the collection.
     * The BiFunction calls Collection.remove(element) and returns the modified collection.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiFunctions.<String, java.util.List<String>>ofRemove().apply(new java.util.ArrayList<>(java.util.List.of("a", "b")), "a");   // removes "a", returns the list
     * }</pre>
     *
     * @param <T> the type of element to remove
     * @param <C> the type of collection
     * @return a BiFunction that removes the second argument from the first argument collection and returns the collection
     */
    public static <T, C extends Collection<? super T>> BiFunction<C, T, C> ofRemove() {
        return (BiFunction<C, T, C>) REMOVE;
    }

    /**
     * Returns a BiFunction that removes all elements of one collection from another and returns the target collection.
     * The BiFunction calls Collection.removeAll(collection) and returns the modified collection.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiFunctions.<String, java.util.List<String>>ofRemoveAll().apply(new java.util.ArrayList<>(java.util.List.of("a", "b")), new java.util.ArrayList<>(java.util.List.of("a")));   // removes all, returns the first list
     * }</pre>
     *
     * @param <T> the type of elements in the collections
     * @param <C> the type of collection
     * @return a BiFunction that removes all elements in the second collection from the first and returns the first collection
     */
    public static <T, C extends Collection<T>> BiFunction<C, C, C> ofRemoveAll() {
        return (BiFunction<C, C, C>) REMOVE_ALL;
    }

    // /**
    //  * Returns a BiFunction that removes all elements of one PrimitiveList from another and returns the target list.
    //  * The BiFunction calls PrimitiveList.removeAll(list) and returns the modified list.
    //  *
    //  * <p><b>Usage Examples:</b></p>
    //  * <pre>{@code
    //  * BiFunctions.ofRemoveAlll().apply(com.landawn.abacus.util.IntList.of(1, 2, 3), com.landawn.abacus.util.IntList.of(2));   // removes all primitives, returns the first list
    //  * }</pre>
    //  *
    //  * @param <T> the type of PrimitiveList
    //  * @return a BiFunction that removes all elements in the second PrimitiveList from the first and returns the first list
    //  */
    // @Beta
    // @SuppressWarnings("rawtypes")
    // public static <T extends PrimitiveList> BiFunction<T, T, T> ofRemoveAlll() {
    //     return (BiFunction<T, T, T>) REMOVE_ALL_2;
    // }

    /**
     * Returns a BiFunction that puts a Map.Entry into a Map and returns the map.
     * The BiFunction extracts the key and value from the entry, puts them into the map, and returns the map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiFunctions.<String, Integer, java.util.Map<String, Integer>, java.util.Map.Entry<String, Integer>>ofPut().apply(new java.util.HashMap<>(), java.util.Map.entry("k", 1));   // returns the map after the put
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param <M> the type of map
     * @param <E> the type of map entry
     * @return a BiFunction that puts the entry into the map and returns the map
     */
    public static <K, V, M extends Map<K, V>, E extends Map.Entry<K, V>> BiFunction<M, E, M> ofPut() {
        return (BiFunction<M, E, M>) PUT;
    }

    /**
     * Returns a BiFunction that puts all entries from one map into another and returns the target map.
     * The BiFunction calls Map.putAll(map) and returns the modified map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiFunctions.<String, Integer, java.util.Map<String, Integer>>ofPutAll().apply(new java.util.HashMap<>(java.util.Map.of("k", 1)), java.util.Map.of("m", 2));   // returns the first map after putting all
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param <M> the type of map
     * @return a BiFunction that puts all entries from the second map into the first and returns the first map
     */
    public static <K, V, M extends Map<K, V>> BiFunction<M, M, M> ofPutAll() {
        return (BiFunction<M, M, M>) PUT_ALL;
    }

    /**
     * Returns a BiFunction that removes an entry from a map by key and returns the map.
     * The BiFunction calls Map.remove(key) and returns the modified map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiFunctions.<String, Integer, java.util.Map<String, Integer>>ofRemoveByKey().apply(new java.util.HashMap<>(java.util.Map.of("k", 1)), "k");   // removes by key, returns the map
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param <M> the type of map
     * @return a BiFunction that removes the entry with the given key from the map and returns the map
     */
    public static <K, V, M extends Map<K, V>> BiFunction<M, K, M> ofRemoveByKey() {
        return (BiFunction<M, K, M>) REMOVE_BY_KEY;
    }

    /**
     * Returns a BiFunction that merges two Joiner instances and returns the result.
     * The BiFunction calls Joiner.merge(joiner) and returns the merged Joiner.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiFunctions.ofMerge().apply(Joiner.with(",").append("a"), Joiner.with(",").append("b"));   // returns the merged Joiner
     * }</pre>
     *
     * @return a BiFunction that merges the second Joiner into the first and returns the result
     */
    public static BiFunction<Joiner, Joiner, Joiner> ofMerge() {
        return MERGE;
    }

    /**
     * Returns a BiFunction that appends an object to a StringBuilder and returns the StringBuilder.
     * The BiFunction calls StringBuilder.append(object) and returns the modified StringBuilder.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiFunctions.<String>ofAppend().apply(new StringBuilder("a"), "hello");   // returns the StringBuilder after appending
     * }</pre>
     *
     * @param <T> the type of object to append
     * @return a BiFunction that appends the second argument to the first argument StringBuilder and returns the StringBuilder
     */
    public static <T> BiFunction<StringBuilder, T, StringBuilder> ofAppend() {
        return (BiFunction<StringBuilder, T, StringBuilder>) APPEND;
    }

    /**
     * Returns a stateful BiFunction that applies a function based on element index position.
     * The function maintains an internal counter that increments with each apply call, starting from 0.
     *
     * <p><b>Important:</b> This method is marked as {@code @Beta}, {@code @SequentialOnly}, and {@code @Stateful},
     * indicating it should not be saved, cached for reuse, or used in parallel streams. Each invocation
     * creates a new instance with its own independent counter starting at 0.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create indexed pairs from two lists
     * List<String> names = Arrays.asList("Alice", "Bob", "Charlie");
     * List<Integer> ages = Arrays.asList(25, 30, 35);
     *
     * BiFunction<String, Integer, String> indexedFormatter =
     *     BiFunctions.indexed((idx, name, age) ->
     *         String.format("[%d] %s is %d years old", idx, name, age));
     *
     * // Apply to pairs - index increments with each call
     * System.out.println(indexedFormatter.apply(names.get(0), ages.get(0)));
     * // Output: "[0] Alice is 25 years old"
     * System.out.println(indexedFormatter.apply(names.get(1), ages.get(1)));
     * // Output: "[1] Bob is 30 years old"
     * }</pre>
     *
     * @param <T> the type of the first argument to the function
     * @param <U> the type of the second argument to the function
     * @param <R> the type of the result of the function
     * @param func the IntBiObjFunction that accepts an index and two elements and produces a result
     * @return a stateful BiFunction that applies the given IntBiObjFunction with an incrementing index
     * @throws IllegalArgumentException if {@code func} is {@code null}.
     */
    @Beta
    @SequentialOnly
    @Stateful
    public static <T, U, R> BiFunction<T, U, R> indexed(final IntBiObjFunction<T, U, ? extends R> func) throws IllegalArgumentException {
        N.checkArgNotNull(func, cs.func);

        return new BiFunction<>() {
            private final MutableInt idx = new MutableInt(0);

            @Override
            public R apply(final T t, final U u) {
                return func.apply(idx.getAndIncrement(), t, u);
            }
        };
    }
}
