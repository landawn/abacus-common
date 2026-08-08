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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;

import com.landawn.abacus.util.function.BinaryOperator;

/**
 * Utility class providing various BinaryOperator implementations and factory methods.
 * This class contains predefined BinaryOperators for common merge and combination operations.
 */
public final class BinaryOperators {

    /** The Constant THROWING_MERGER. */
    @SuppressWarnings("rawtypes")
    static final BinaryOperator THROWING_MERGER = (t, u) -> {
        throw new IllegalStateException(String.format("Duplicate key (attempted merging values %s and %s)", t, u));
    };

    /** The Constant IGNORING_MERGER. */
    @SuppressWarnings("rawtypes")
    static final BinaryOperator IGNORING_MERGER = (t, u) -> t;

    /** The Constant REPLACING_MERGER. */
    @SuppressWarnings("rawtypes")
    static final BinaryOperator REPLACING_MERGER = (t, u) -> u;

    /** The Constant ADD_ALL_TO_FIRST. */
    private static final BinaryOperator<Collection<Object>> ADD_ALL_TO_FIRST = (t, u) -> {
        t.addAll(u);
        return t;
    };

    /** The Constant ADD_ALL_TO_BIGGER. */
    private static final BinaryOperator<Collection<Object>> ADD_ALL_TO_BIGGER = (t, u) -> {
        if (t.size() >= u.size()) {
            t.addAll(u);
            return t;
        } else {
            u.addAll(t);
            return u;
        }
    };

    /** The Constant REMOVE_ALL_FROM_FIRST. */
    private static final BinaryOperator<Collection<Object>> REMOVE_ALL_FROM_FIRST = (t, u) -> {
        t.removeAll(u);
        return t;
    };

    /** The Constant PUT_ALL_TO_FIRST. */
    private static final BinaryOperator<Map<Object, Object>> PUT_ALL_TO_FIRST = (t, u) -> {
        t.putAll(u);
        return t;
    };

    /** The Constant PUT_ALL_TO_BIGGER. */
    private static final BinaryOperator<Map<Object, Object>> PUT_ALL_TO_BIGGER = (t, u) -> {
        if (t.size() >= u.size()) {
            t.putAll(u);
            return t;
        } else {
            u.putAll(t);
            return u;
        }
    };

    /** The Constant MERGE_TO_FIRST. */
    private static final BinaryOperator<Joiner> MERGE_TO_FIRST = Joiner::merge;

    /** The Constant MERGE_TO_BIGGER. */
    private static final BinaryOperator<Joiner> MERGE_TO_BIGGER = (t, u) -> {
        if (t.length() >= u.length()) {
            return t.merge(u);
        } else {
            return u.merge(t);
        }
    };

    /** The Constant APPEND_TO_FIRST. */
    private static final BinaryOperator<StringBuilder> APPEND_TO_FIRST = StringBuilder::append;

    /** The Constant APPEND_TO_BIGGER. */
    private static final BinaryOperator<StringBuilder> APPEND_TO_BIGGER = (t, u) -> {
        if (t.length() >= u.length()) {
            return t.append(u);
        } else {
            return u.insert(0, t);
        }
    };

    /** The Constant CONCAT. */
    private static final BinaryOperator<String> CONCAT = (t, u) -> t + u;

    /** The Constant ADD_INTEGER. */
    private static final BinaryOperator<Integer> ADD_INTEGER = Integer::sum;

    /** The Constant ADD_LONG. */
    private static final BinaryOperator<Long> ADD_LONG = Long::sum;

    /** The Constant ADD_DOUBLE. */
    private static final BinaryOperator<Double> ADD_DOUBLE = Double::sum;

    /** The Constant ADD_BIG_INTEGER. */
    private static final BinaryOperator<BigInteger> ADD_BIG_INTEGER = BigInteger::add;

    /** The Constant ADD_BIG_DECIMAL. */
    private static final BinaryOperator<BigDecimal> ADD_BIG_DECIMAL = BigDecimal::add;

    private BinaryOperators() {
    }

    /**
     * Returns a BinaryOperator that adds all elements from the second collection to the first.
     * This method is deprecated, use ofAddAllToFirst() instead.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BinaryOperators.<String, java.util.List<String>>ofAddAll().apply(new java.util.ArrayList<>(java.util.List.of("a")), new java.util.ArrayList<>(java.util.List.of("b")));   // adds the second list into the first, returns the first
     * }</pre>
     *
     * @param <T> the type of elements in the collection
     * @param <C> the type of collection
     * @return a BinaryOperator that adds all elements from the second collection to the first and returns the first
     * @deprecated replaced by {@link #ofAddAllToFirst()}
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public static <T, C extends Collection<T>> BinaryOperator<C> ofAddAll() {
        return (BinaryOperator<C>) ADD_ALL_TO_FIRST;
    }

    /**
     * Returns a BinaryOperator that adds all elements from the second collection to the first.
     * The operator modifies and returns the first collection.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BinaryOperators.<String, java.util.List<String>>ofAddAllToFirst().apply(new java.util.ArrayList<>(java.util.List.of("a")), new java.util.ArrayList<>(java.util.List.of("b")));   // adds the second list into the first, returns the first
     * }</pre>
     *
     * @param <T> the type of elements in the collection
     * @param <C> the type of collection
     * @return a BinaryOperator that adds all elements from the second collection to the first and returns the first
     */
    @SuppressWarnings("unchecked")
    public static <T, C extends Collection<T>> BinaryOperator<C> ofAddAllToFirst() {
        return (BinaryOperator<C>) ADD_ALL_TO_FIRST;
    }

    /**
     * Returns a BinaryOperator that adds all elements to the bigger collection.
     * The operator compares sizes and adds the smaller collection to the larger one, returning the larger.
     * Both inputs must be mutable. If the second collection is larger, its existing elements precede
     * the first collection's elements, so this operator does not preserve first-then-second order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BinaryOperators.<String, java.util.List<String>>ofAddAllToBigger().apply(new java.util.ArrayList<>(java.util.List.of("a", "b")), new java.util.ArrayList<>(java.util.List.of("c")));   // returns the bigger list after adding the smaller into it
     * }</pre>
     *
     * @param <T> the type of elements in the collection
     * @param <C> the type of collection
     * @return a BinaryOperator that adds all elements to the bigger collection and returns it
     */
    @SuppressWarnings("unchecked")
    public static <T, C extends Collection<T>> BinaryOperator<C> ofAddAllToBigger() {
        return (BinaryOperator<C>) ADD_ALL_TO_BIGGER;
    }

    /**
     * Returns a BinaryOperator that removes all elements of the second collection from the first.
     * This method is deprecated, use ofRemoveAllFromFirst() instead.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BinaryOperators.<String, java.util.List<String>>ofRemoveAll().apply(new java.util.ArrayList<>(java.util.List.of("a", "b")), new java.util.ArrayList<>(java.util.List.of("a")));   // removes the second list from the first, returns the first
     * }</pre>
     *
     * @param <T> the type of elements in the collection
     * @param <C> the type of collection
     * @return a BinaryOperator that removes all elements of the second collection from the first and returns the first
     * @deprecated replaced by {@link #ofRemoveAllFromFirst()}
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public static <T, C extends Collection<T>> BinaryOperator<C> ofRemoveAll() {
        return (BinaryOperator<C>) REMOVE_ALL_FROM_FIRST;
    }

    /**
     * Returns a BinaryOperator that removes all elements of the second collection from the first.
     * The operator modifies and returns the first collection.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BinaryOperators.<String, java.util.List<String>>ofRemoveAllFromFirst().apply(new java.util.ArrayList<>(java.util.List.of("a", "b")), new java.util.ArrayList<>(java.util.List.of("a")));   // removes the second list from the first, returns the first
     * }</pre>
     *
     * @param <T> the type of elements in the collection
     * @param <C> the type of collection
     * @return a BinaryOperator that removes all elements of the second collection from the first and returns the first
     */
    @SuppressWarnings("unchecked")
    public static <T, C extends Collection<T>> BinaryOperator<C> ofRemoveAllFromFirst() {
        return (BinaryOperator<C>) REMOVE_ALL_FROM_FIRST;
    }

    /**
     * Returns a BinaryOperator that puts all entries from the second map into the first.
     * This method is deprecated, use ofPutAllToFirst() instead.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BinaryOperators.<String, Integer, java.util.Map<String, Integer>>ofPutAll().apply(new java.util.HashMap<>(java.util.Map.of("k", 1)), new java.util.HashMap<>(java.util.Map.of("m", 2)));   // returns the first map after putting all entries of the second
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param <M> the type of map
     * @return a BinaryOperator that puts all entries from the second map into the first and returns the first
     * @deprecated replaced by {@link #ofPutAllToFirst()}
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public static <K, V, M extends Map<K, V>> BinaryOperator<M> ofPutAll() {
        return (BinaryOperator<M>) PUT_ALL_TO_FIRST;
    }

    /**
     * Returns a BinaryOperator that puts all entries from the second map into the first.
     * The operator modifies and returns the first map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BinaryOperators.<String, Integer, java.util.Map<String, Integer>>ofPutAllToFirst().apply(new java.util.HashMap<>(java.util.Map.of("k", 1)), new java.util.HashMap<>(java.util.Map.of("m", 2)));   // returns the first map after putting all entries of the second
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param <M> the type of map
     * @return a BinaryOperator that puts all entries from the second map into the first and returns the first
     */
    @SuppressWarnings("unchecked")
    public static <K, V, M extends Map<K, V>> BinaryOperator<M> ofPutAllToFirst() {
        return (BinaryOperator<M>) PUT_ALL_TO_FIRST;
    }

    /**
     * Returns a BinaryOperator that puts all entries into the bigger map.
     * The operator compares sizes and puts the smaller map into the larger one, returning the larger.
     * Both inputs must be mutable. For duplicate keys, values from the smaller map replace values in
     * the larger map; the winning argument therefore depends on their sizes.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BinaryOperators.<String, Integer, java.util.Map<String, Integer>>ofPutAllToBigger().apply(new java.util.HashMap<>(java.util.Map.of("k", 1, "k2", 2)), new java.util.HashMap<>(java.util.Map.of("m", 3)));   // returns the bigger map after putting all entries of the smaller into it
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @param <M> the type of map
     * @return a BinaryOperator that puts all entries into the bigger map and returns it
     */
    @SuppressWarnings("unchecked")
    public static <K, V, M extends Map<K, V>> BinaryOperator<M> ofPutAllToBigger() {
        return (BinaryOperator<M>) PUT_ALL_TO_BIGGER;
    }

    /**
     * Returns a BinaryOperator that merges two Joiners.
     * This method is deprecated, use ofMergeToFirst() instead.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BinaryOperators.ofMerge().apply(Joiner.with(",").append("a"), Joiner.with(",").append("b"));   // returns the first Joiner after merging the second into it
     * }</pre>
     *
     * @return a BinaryOperator that merges the second Joiner into the first and returns the first
     * @deprecated replaced by {@link #ofMergeToFirst()}
     */
    @Deprecated
    public static BinaryOperator<Joiner> ofMerge() {
        return MERGE_TO_FIRST;
    }

    /**
     * Returns a BinaryOperator that merges the second Joiner into the first.
     * The operator modifies and returns the first Joiner.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BinaryOperators.ofMergeToFirst().apply(Joiner.with(",").append("a"), Joiner.with(",").append("b"));   // returns the first Joiner after merging the second into it
     * }</pre>
     *
     * @return a BinaryOperator that merges the second Joiner into the first and returns the first
     */
    public static BinaryOperator<Joiner> ofMergeToFirst() {
        return MERGE_TO_FIRST;
    }

    /**
     * Returns a BinaryOperator that merges to the bigger Joiner.
     * The operator compares lengths and merges the smaller Joiner into the larger one, returning the larger.
     * Both inputs are mutated candidates. If the second joiner is larger, it remains first and the first
     * joiner is merged after it, so this operator does not preserve first-then-second order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BinaryOperators.ofMergeToBigger().apply(Joiner.with(",").append("a").append("b"), Joiner.with(",").append("c"));   // returns the bigger Joiner after merging the smaller into it
     * }</pre>
     *
     * @return a BinaryOperator that merges to the bigger Joiner and returns it
     */
    public static BinaryOperator<Joiner> ofMergeToBigger() {
        return MERGE_TO_BIGGER;
    }

    /**
     * Returns a BinaryOperator that appends the second StringBuilder to the first.
     * This method is deprecated, use ofAppendToFirst() instead.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BinaryOperators.ofAppend().apply(new StringBuilder("a"), new StringBuilder("b"));   // returns the first StringBuilder after appending the second
     * }</pre>
     *
     * @return a BinaryOperator that appends the second StringBuilder to the first and returns the first
     * @deprecated replaced by {@link #ofAppendToFirst()}
     */
    @Deprecated
    public static BinaryOperator<StringBuilder> ofAppend() {
        return APPEND_TO_FIRST;
    }

    /**
     * Returns a BinaryOperator that appends the second StringBuilder to the first.
     * The operator modifies and returns the first StringBuilder.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BinaryOperators.ofAppendToFirst().apply(new StringBuilder("a"), new StringBuilder("b"));   // returns the first StringBuilder after appending the second
     * }</pre>
     *
     * @return a BinaryOperator that appends the second StringBuilder to the first and returns the first
     */
    public static BinaryOperator<StringBuilder> ofAppendToFirst() {
        return APPEND_TO_FIRST;
    }

    /**
     * Returns a BinaryOperator that appends to the bigger StringBuilder.
     * The operator combines the two builders into the larger one (appending the second to the first, or inserting
     * the first at the front of the second), preserving first-then-second order and returning the larger.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BinaryOperators.ofAppendToBigger().apply(new StringBuilder("ab"), new StringBuilder("c"));   // returns the bigger StringBuilder after appending the smaller
     * }</pre>
     *
     * @return a BinaryOperator that appends to the bigger StringBuilder and returns it
     */
    public static BinaryOperator<StringBuilder> ofAppendToBigger() {
        return APPEND_TO_BIGGER;
    }

    /**
     * Returns a BinaryOperator that concatenates two strings.
     * The operator performs string concatenation using the + operator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BinaryOperators.ofConcat().apply("Hello","World");      // returns "HelloWorld"
     * }</pre>
     *
     * @return a BinaryOperator that concatenates two strings
     */
    public static BinaryOperator<String> ofConcat() {
        return CONCAT;
    }

    /**
     * Returns a BinaryOperator that adds two Integer values.
     * The operator uses Integer.sum for addition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BinaryOperators.ofAddInt().apply(5,3);                  // returns 8
     * }</pre>
     *
     * @return a BinaryOperator that adds two Integer values
     */
    public static BinaryOperator<Integer> ofAddInt() {
        return ADD_INTEGER;
    }

    /**
     * Returns a BinaryOperator that adds two Long values.
     * The operator uses Long.sum for addition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BinaryOperators.ofAddLong().apply(5L,3L);               // returns 8L
     * }</pre>
     *
     * @return a BinaryOperator that adds two Long values
     */
    public static BinaryOperator<Long> ofAddLong() {
        return ADD_LONG;
    }

    /**
     * Returns a BinaryOperator that adds two Double values.
     * The operator uses Double.sum for addition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BinaryOperators.ofAddDouble().apply(5.0,3.0);           // returns 8.0
     * }</pre>
     *
     * @return a BinaryOperator that adds two Double values
     */
    public static BinaryOperator<Double> ofAddDouble() {
        return ADD_DOUBLE;
    }

    /**
     * Returns a BinaryOperator that adds two BigInteger values.
     * The operator uses BigInteger.add for addition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BinaryOperators.ofAddBigInteger().apply(BigInteger.TEN,BigInteger.ONE); // returns BigInteger 11
     * }</pre>
     *
     * @return a BinaryOperator that adds two BigInteger values
     */
    public static BinaryOperator<BigInteger> ofAddBigInteger() {
        return ADD_BIG_INTEGER;
    }

    /**
     * Returns a BinaryOperator that adds two BigDecimal values.
     * The operator uses BigDecimal.add for addition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BinaryOperators.ofAddBigDecimal().apply(BigDecimal.TEN,BigDecimal.ONE); // returns BigDecimal 11
     * }</pre>
     *
     * @return a BinaryOperator that adds two BigDecimal values
     */
    public static BinaryOperator<BigDecimal> ofAddBigDecimal() {
        return ADD_BIG_DECIMAL;
    }
}
