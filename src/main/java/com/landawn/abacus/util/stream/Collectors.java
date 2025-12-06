/*
 * Copyright (C) 2016, 2017, 2018, 2019 HaiYang Li
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

package com.landawn.abacus.util.stream;

import static com.landawn.abacus.util.stream.StreamBase.ERROR_MSG_FOR_NO_SUCH_EX;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Deque;
import java.util.DoubleSummaryStatistics;
import java.util.EnumSet;
import java.util.IntSummaryStatistics;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.BiMap;
import com.landawn.abacus.util.BigDecimalSummaryStatistics;
import com.landawn.abacus.util.BigIntegerSummaryStatistics;
import com.landawn.abacus.util.BooleanList;
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.ByteSummaryStatistics;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.CharSummaryStatistics;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.Comparators;
import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.FloatList;
import com.landawn.abacus.util.FloatSummaryStatistics;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Fn.BiConsumers;
import com.landawn.abacus.util.Fn.BinaryOperators;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.ImmutableSet;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.KahanSummation;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.LongList;
import com.landawn.abacus.util.Multimap;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.ObjIterator;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.ShortList;
import com.landawn.abacus.util.ShortSummaryStatistics;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.Tuple.Tuple4;
import com.landawn.abacus.util.Tuple.Tuple5;
import com.landawn.abacus.util.Tuple.Tuple6;
import com.landawn.abacus.util.Tuple.Tuple7;
import com.landawn.abacus.util.cs;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.QuadFunction;
import com.landawn.abacus.util.function.ToByteFunction;
import com.landawn.abacus.util.function.ToCharFunction;
import com.landawn.abacus.util.function.ToFloatFunction;
import com.landawn.abacus.util.function.ToShortFunction;
import com.landawn.abacus.util.function.TriFunction;

// Claude Opus 4 and generate the Javadoc with blow prompts 
// Please generate comprehensive javadoc for all public methods starting from line 6 in Collectors.java, including public static methods.  Please use javadoc of method "toCollection" as a template to generate javadoc for other methods. Please include a very simple sample in couple of lines if appropriate. Please don't take shortcut. The generated javadoc should be specific and details enough to describe the behavior of the method. And merge the generated javadoc into source file Collectors.java to replace existing javadoc in Collectors.java. Don't generate javadoc for method which is not found in Collectors.java. Remember don't use any cache file because I have modified Collectors.java. Again, don't generate javadoc for the method which is not in the attached file. Please read and double check if the method is in the attached file before starting to generate javadoc. If the method is not the attached file, don't generate javadoc for it.
/**
 * A comprehensive factory utility class providing static methods for creating {@link Collector} instances
 * for use with Java Streams. This abstract sealed class extends the functionality of the standard
 * {@code java.util.stream.Collectors} with additional collectors for specialized data structures,
 * advanced aggregations, and custom collection types specific to the abacus-common framework.
 *
 * <p>Collectors serves as the central hub for all stream collection operations, offering pre-built
 * collectors for common scenarios and sophisticated collectors for complex data transformations.
 * The class provides collectors for primitive lists, immutable collections, multimaps, bidirectional
 * maps, statistical summaries, and advanced grouping operations with extensive customization options.</p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Extended Collection Types:</b> Collectors for primitive lists, immutable collections, and specialized maps</li>
 *   <li><b>Advanced Grouping:</b> Sophisticated grouping and partitioning with custom downstream collectors</li>
 *   <li><b>Statistical Aggregation:</b> Comprehensive summary statistics for all numeric types including BigDecimal</li>
 *   <li><b>Multimap Support:</b> Collection of values per key with various collection types</li>
 *   <li><b>Bidirectional Maps:</b> BiMap collectors for reversible key-value associations</li>
 *   <li><b>Functional Composition:</b> Teeing and combining collectors for parallel processing</li>
 *   <li><b>Performance Optimization:</b> Specialized collectors optimized for specific data types</li>
 *   <li><b>Null Safety:</b> Consistent null handling across all collector implementations</li>
 * </ul>
 *
 * <p><b>Common Use Cases:</b>
 * <ul>
 *   <li><b>Data Aggregation:</b> Collecting stream elements into various container types</li>
 *   <li><b>Grouping Operations:</b> Partitioning data by keys with downstream processing</li>
 *   <li><b>Statistical Analysis:</b> Computing summary statistics and numerical aggregations</li>
 *   <li><b>Data Transformation:</b> Converting streams to specialized collection types</li>
 *   <li><b>Performance Optimization:</b> Using primitive collections to avoid boxing overhead</li>
 *   <li><b>Immutable Collections:</b> Creating thread-safe immutable data structures</li>
 *   <li><b>Multi-valued Mappings:</b> Building maps with multiple values per key</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b>
 * <pre>{@code
 * // Primitive list collection (avoiding boxing)
 * IntList numbers = Stream.of(1, 2, 3, 4, 5)
 *     .collect(Collectors.toIntList());
 *
 * // Immutable collection creation
 * ImmutableList<String> names = people.stream()
 *     .map(Person::getName)
 *     .collect(Collectors.toImmutableList());
 *
 * // BiMap for bidirectional lookups
 * BiMap<String, Integer> userIds = users.stream()
 *     .collect(Collectors.toBiMap(User::getName, User::getId));
 *
 * // Multimap grouping with value transformation
 * ListMultimap<Department, Employee> byDept = employees.stream()
 *     .collect(Collectors.toMultimap(Employee::getDepartment));
 *
 * // Advanced grouping with downstream processing
 * Map<String, Double> avgSalaryByDept = employees.stream()
 *     .collect(Collectors.groupingBy(
 *         Employee::getDepartment,
 *         Collectors.averagingDouble(Employee::getSalary)));
 *
 * // Statistical summary collection
 * DoubleSummaryStatistics stats = values.stream()
 *     .collect(Collectors.summarizingDouble(Double::doubleValue));
 *
 * // Teeing for parallel collection
 * Pair<Integer, Double> countAndAvg = numbers.stream()
 *     .collect(Collectors.teeing(
 *         Collectors.counting(),
 *         Collectors.averagingDouble(Number::doubleValue),
 *         Pair::of));
 * }</pre>
 *
 * <p><b>Collector Categories:</b>
 * <ul>
 *   <li><b>Basic Collections:</b> {@code toList()}, {@code toSet()}, {@code toCollection()}</li>
 *   <li><b>Primitive Collections:</b> {@code toIntList()}, {@code toLongList()}, {@code toDoubleList()}, etc.</li>
 *   <li><b>Immutable Collections:</b> {@code toImmutableList()}, {@code toImmutableSet()}, {@code toImmutableMap()}</li>
 *   <li><b>Maps:</b> {@code toMap()}, {@code toLinkedHashMap()}, {@code toConcurrentMap()}, {@code toBiMap()}</li>
 *   <li><b>Multimaps:</b> {@code toMultimap()}, {@code flatMappingToMultimap()}</li>
 *   <li><b>Grouping:</b> {@code groupingBy()}, {@code partitioningBy()}, {@code countingBy()}</li>
 *   <li><b>Aggregation:</b> {@code summingInt()}, {@code averagingDouble()}, {@code summarizing*()}</li>
 *   <li><b>Reduction:</b> {@code reducing()}, {@code maxBy()}, {@code minBy()}</li>
 *   <li><b>String Operations:</b> {@code joining()}, {@code commonPrefix()}, {@code commonSuffix()}</li>
 * </ul>
 *
 * <p><b>Primitive Collection Support:</b>
 * <ul>
 *   <li><b>Performance Benefit:</b> Avoid boxing/unboxing overhead for primitive types</li>
 *   <li><b>Memory Efficiency:</b> Reduced memory footprint compared to wrapper collections</li>
 *   <li><b>Type Safety:</b> Compile-time type checking for primitive operations</li>
 *   <li><b>Available Types:</b> boolean, byte, char, short, int, long, float, double</li>
 * </ul>
 *
 * <p><b>Immutable Collection Benefits:</b>
 * <ul>
 *   <li><b>Thread Safety:</b> Immutable collections are inherently thread-safe</li>
 *   <li><b>Defensive Copying:</b> Prevents accidental modification of shared data</li>
 *   <li><b>API Clarity:</b> Clear intent that data should not be modified</li>
 *   <li><b>Performance:</b> Optimized implementations for read-only access patterns</li>
 * </ul>
 *
 * <p><b>Advanced Grouping Features:</b>
 * <ul>
 *   <li><b>Downstream Collectors:</b> Chain collectors for complex aggregations</li>
 *   <li><b>Custom Map Types:</b> Control ordering and concurrency with map suppliers</li>
 *   <li><b>Value Transformation:</b> Transform grouped values with downstream processing</li>
 *   <li><b>Concurrent Grouping:</b> Thread-safe grouping operations for parallel streams</li>
 * </ul>
 *
 * <p><b>BiMap Collector Features:</b>
 * <ul>
 *   <li><b>Bidirectional Lookup:</b> Efficient key-to-value and value-to-key operations</li>
 *   <li><b>Uniqueness Enforcement:</b> Ensures bijective relationship between keys and values</li>
 *   <li><b>Merge Functions:</b> Handle duplicate keys or values with custom merge logic</li>
 *   <li><b>Force Operations:</b> Override uniqueness constraints when necessary</li>
 * </ul>
 *
 * <p><b>Multimap Collector Options:</b>
 * <ul>
 *   <li><b>Collection Types:</b> Choose List, Set, or custom collections for values</li>
 *   <li><b>Flat Mapping:</b> Expand single elements into multiple key-value pairs</li>
 *   <li><b>Key/Value Extraction:</b> Flexible mapping from elements to keys and values</li>
 *   <li><b>Custom Implementations:</b> Support for different Multimap implementations</li>
 * </ul>
 *
 * <p><b>Statistical Collectors:</b>
 * <ul>
 *   <li><b>Primitive Statistics:</b> int, long, double with count, sum, min, max, average</li>
 *   <li><b>BigDecimal/BigInteger:</b> High-precision arithmetic with overflow protection</li>
 *   <li><b>Custom Numeric Types:</b> byte, short, float with specialized summary statistics</li>
 *   <li><b>Kahan Summation:</b> Improved accuracy for floating-point summation</li>
 * </ul>
 *
 * <p><b>Performance Characteristics:</b>
 * <ul>
 *   <li>Collection building: O(n) time where n is the number of elements</li>
 *   <li>Grouping operations: O(n) time with O(k) space where k is the number of groups</li>
 *   <li>Map construction: O(n) average time, O(n²) worst case for hash-based maps</li>
 *   <li>Statistical aggregation: O(n) time with O(1) space for most operations</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b>
 * <ul>
 *   <li><b>Collector Instances:</b> All collectors are thread-safe and can be reused</li>
 *   <li><b>Concurrent Collections:</b> Some collectors produce thread-safe results</li>
 *   <li><b>Parallel Streams:</b> Full support for parallel stream operations</li>
 *   <li><b>Accumulator Safety:</b> Internal accumulators handle concurrent access appropriately</li>
 * </ul>
 *
 * <p><b>Error Handling:</b>
 * <ul>
 *   <li>Throws {@link IllegalArgumentException} for null required parameters</li>
 *   <li>Throws {@link IllegalStateException} for constraint violations (e.g., duplicate keys)</li>
 *   <li>Handles null elements according to the specific collector's documented behavior</li>
 *   <li>Provides clear error messages for debugging collection failures</li>
 * </ul>
 *
 * <p><b>Integration with Standard Collectors:</b>
 * <ul>
 *   <li>Fully compatible with {@code java.util.stream.Collectors}</li>
 *   <li>Can be used as downstream collectors in standard grouping operations</li>
 *   <li>Supports all {@link Collector.Characteristics} for optimization</li>
 *   <li>Works seamlessly with custom collectors and third-party libraries</li>
 * </ul>
 *
 * <p><b>Best Practices:</b>
 * <ul>
 *   <li>Use primitive collectors when working with primitive streams for better performance</li>
 *   <li>Choose immutable collectors for shared or cached data structures</li>
 *   <li>Consider concurrent collectors for parallel stream processing</li>
 *   <li>Use appropriate initial capacity hints for known data sizes</li>
 *   <li>Prefer specific collectors over generic ones for type safety and performance</li>
 * </ul>
 *
 * <p><b>Extension Points:</b>
 * <ul>
 *   <li><b>{@link MoreCollectors}:</b> Additional specialized collectors for advanced use cases</li>
 *   <li><b>Custom Suppliers:</b> Provide custom collection implementations via suppliers</li>
 *   <li><b>Merge Functions:</b> Define custom behavior for handling duplicate keys/values</li>
 *   <li><b>Downstream Collectors:</b> Chain collectors for complex data transformations</li>
 * </ul>
 *
 * <p><b>Memory Management:</b>
 * <ul>
 *   <li>Collectors optimize memory usage based on known collection characteristics</li>
 *   <li>Primitive collections reduce memory overhead compared to object collections</li>
 *   <li>Immutable collections may share internal structures for memory efficiency</li>
 *   <li>Consider using streaming collectors for very large datasets</li>
 * </ul>
 *
 * <p><b>Comparison with Standard Collectors:</b>
 * <ul>
 *   <li><b>Extended Types:</b> Support for primitive and immutable collections</li>
 *   <li><b>Enhanced Features:</b> BiMaps, Multimaps, and advanced statistical operations</li>
 *   <li><b>Better Performance:</b> Optimized implementations for specific use cases</li>
 *   <li><b>Null Safety:</b> More predictable null handling across all operations</li>
 * </ul>
 *
 * @see java.util.stream.Collector
 * @see java.util.stream.Collectors
 * @see MoreCollectors
 * @see Stream
 * @see ImmutableList
 * @see ImmutableSet
 * @see ImmutableMap
 * @see BiMap
 * @see Multimap
 * @see ListMultimap
 */
@SuppressWarnings({ "java:S1694" })
public abstract sealed class Collectors permits Collectors.MoreCollectors { // NOSONAR

    static final Object NONE = ClassUtil.createNullMask();   //NOSONAR

    static final Characteristics[] CH_NOID = {};

    static final Characteristics[] CH_ID = { Characteristics.IDENTITY_FINISH };

    static final Characteristics[] CH_UNORDERED_NOID = { Characteristics.UNORDERED };

    static final Characteristics[] CH_UNORDERED_ID = { Characteristics.UNORDERED, Characteristics.IDENTITY_FINISH };

    /**
     * @deprecated This class is not intended to be used.
     */
    @Deprecated
    static final Characteristics[] CH_CONCURRENT_NOID = { Characteristics.CONCURRENT, Characteristics.UNORDERED };

    /**
     * @deprecated This class is not intended to be used.
     */
    @Deprecated
    static final Characteristics[] CH_CONCURRENT_ID = { Characteristics.CONCURRENT, Characteristics.UNORDERED, Characteristics.IDENTITY_FINISH };

    // ============================================================================================================

    private static final Function<List<Object>, ImmutableList<Object>> ImmutableList_Finisher = ImmutableList::wrap;

    private static final Function<Set<Object>, ImmutableSet<Object>> ImmutableSet_Finisher = ImmutableSet::wrap;

    private static final Function<Map<Object, Object>, ImmutableMap<Object, Object>> ImmutableMap_Finisher = ImmutableMap::wrap;

    private static final BiConsumer<Multiset<Object>, Object> Multiset_Accumulator = Multiset::add;

    private static final BinaryOperator<Multiset<Object>> Multiset_Combiner = (a, b) -> {
        a.addAll(b);
        return a;
    };

    private static final BiConsumer<BooleanList, Boolean> BooleanList_Accumulator = BooleanList::add;

    private static final BinaryOperator<BooleanList> BooleanList_Combiner = (a, b) -> {
        a.addAll(b);
        return a;
    };

    @SuppressWarnings("deprecation")
    private static final Function<BooleanList, boolean[]> BooleanArray_Finisher = t -> t.trimToSize().array();

    private static final BiConsumer<CharList, Character> CharList_Accumulator = CharList::add;

    private static final BinaryOperator<CharList> CharList_Combiner = (a, b) -> {
        a.addAll(b);
        return a;
    };

    @SuppressWarnings("deprecation")
    private static final Function<CharList, char[]> CharArray_Finisher = t -> t.trimToSize().array();

    private static final BiConsumer<ByteList, Byte> ByteList_Accumulator = ByteList::add;

    private static final BinaryOperator<ByteList> ByteList_Combiner = (a, b) -> {
        if (a.size() >= b.size()) {
            a.addAll(b);
            return a;
        } else {
            b.addAll(a);
            return b;
        }
    };

    @SuppressWarnings("deprecation")
    private static final Function<ByteList, byte[]> ByteArray_Finisher = t -> t.trimToSize().array();

    private static final BiConsumer<ShortList, Short> ShortList_Accumulator = ShortList::add;

    private static final BinaryOperator<ShortList> ShortList_Combiner = (a, b) -> {
        if (a.size() >= b.size()) {
            a.addAll(b);
            return a;
        } else {
            b.addAll(a);
            return b;
        }
    };

    @SuppressWarnings("deprecation")
    private static final Function<ShortList, short[]> ShortArray_Finisher = t -> t.trimToSize().array();

    private static final BiConsumer<IntList, Integer> IntList_Accumulator = IntList::add;

    private static final BinaryOperator<IntList> IntList_Combiner = (a, b) -> {
        if (a.size() >= b.size()) {
            a.addAll(b);
            return a;
        } else {
            b.addAll(a);
            return b;
        }
    };

    @SuppressWarnings("deprecation")
    private static final Function<IntList, int[]> IntArray_Finisher = t -> t.trimToSize().array();

    private static final BiConsumer<LongList, Long> LongList_Accumulator = LongList::add;

    private static final BinaryOperator<LongList> LongList_Combiner = (a, b) -> {
        if (a.size() >= b.size()) {
            a.addAll(b);
            return a;
        } else {
            b.addAll(a);
            return b;
        }
    };

    @SuppressWarnings("deprecation")
    private static final Function<LongList, long[]> LongArray_Finisher = t -> t.trimToSize().array();

    private static final BiConsumer<FloatList, Float> FloatList_Accumulator = FloatList::add;

    private static final BinaryOperator<FloatList> FloatList_Combiner = (a, b) -> {
        if (a.size() >= b.size()) {
            a.addAll(b);
            return a;
        } else {
            b.addAll(a);
            return b;
        }
    };

    @SuppressWarnings("deprecation")
    private static final Function<FloatList, float[]> FloatArray_Finisher = t -> t.trimToSize().array();

    private static final BiConsumer<DoubleList, Double> DoubleList_Accumulator = DoubleList::add;

    private static final BinaryOperator<DoubleList> DoubleList_Combiner = (a, b) -> {
        if (a.size() >= b.size()) {
            a.addAll(b);
            return a;
        } else {
            b.addAll(a);
            return b;
        }
    };

    @SuppressWarnings("deprecation")
    private static final Function<DoubleList, double[]> DoubleArray_Finisher = t -> t.trimToSize().array();

    private static final BiConsumer<Joiner, Object> Joiner_Accumulator = Joiner::append;

    private static final BinaryOperator<Joiner> Joiner_Combiner = (a, b) -> {
        if (a.length() > b.length()) {
            a.merge(b);
            b.close();
            return a;
        } else {
            b.merge(a);
            a.close();
            return b;
        }
    };

    private static final Function<Joiner, String> Joiner_Finisher = Joiner::toString;

    private static final Supplier<int[]> SummingInt_Supplier = () -> new int[1];
    private static final Supplier<int[]> SummingInt_Supplier_2 = () -> new int[2];
    private static final Supplier<int[]> SummingInt_Supplier_3 = () -> new int[3];

    private static final Supplier<long[]> SummingIntToLong_Supplier = () -> new long[1];
    private static final Supplier<long[]> SummingIntToLong_Supplier_2 = () -> new long[2];
    private static final Supplier<long[]> SummingIntToLong_Supplier_3 = () -> new long[3];

    private static final BinaryOperator<int[]> SummingInt_Combiner = (a, b) -> {
        a[0] += b[0];
        return a;
    };

    private static final BinaryOperator<int[]> SummingInt_Combiner_2 = (a, b) -> {
        a[0] += b[0];
        a[1] += b[1];
        return a;
    };

    private static final BinaryOperator<int[]> SummingInt_Combiner_3 = (a, b) -> {
        a[0] += b[0];
        a[1] += b[1];
        a[2] += b[2];
        return a;
    };

    private static final BinaryOperator<long[]> SummingIntToLong_Combiner = (a, b) -> {
        a[0] += b[0];
        return a;
    };

    private static final BinaryOperator<long[]> SummingIntToLong_Combiner_2 = (a, b) -> {
        a[0] += b[0];
        a[1] += b[1];
        return a;
    };

    private static final BinaryOperator<long[]> SummingIntToLong_Combiner_3 = (a, b) -> {
        a[0] += b[0];
        a[1] += b[1];
        a[2] += b[2];
        return a;
    };

    private static final Function<int[], Integer> SummingInt_Finisher = a -> a[0];
    private static final Function<int[], Tuple2<Integer, Integer>> SummingInt_Finisher_2 = a -> Tuple.of(a[0], a[1]);
    private static final Function<int[], Tuple3<Integer, Integer, Integer>> SummingInt_Finisher_3 = a -> Tuple.of(a[0], a[1], a[2]);

    private static final Function<long[], Long> SummingIntToLong_Finisher = a -> a[0];
    private static final Function<long[], Tuple2<Long, Long>> SummingIntToLong_Finisher_2 = a -> Tuple.of(a[0], a[1]);
    private static final Function<long[], Tuple3<Long, Long, Long>> SummingIntToLong_Finisher_3 = a -> Tuple.of(a[0], a[1], a[2]);

    private static final Supplier<long[]> SummingLong_Supplier = () -> new long[1];
    private static final Supplier<long[]> SummingLong_Supplier_2 = () -> new long[2];
    private static final Supplier<long[]> SummingLong_Supplier_3 = () -> new long[3];

    private static final BinaryOperator<long[]> SummingLong_Combiner = (a, b) -> {
        a[0] += b[0];
        return a;
    };

    private static final BinaryOperator<long[]> SummingLong_Combiner_2 = (a, b) -> {
        a[0] += b[0];
        a[1] += b[1];
        return a;
    };

    private static final BinaryOperator<long[]> SummingLong_Combiner_3 = (a, b) -> {
        a[0] += b[0];
        a[1] += b[1];
        a[2] += b[2];
        return a;
    };

    private static final Function<long[], Long> SummingLong_Finisher = a -> a[0];
    private static final Function<long[], Tuple2<Long, Long>> SummingLong_Finisher_2 = a -> Tuple.of(a[0], a[1]);
    private static final Function<long[], Tuple3<Long, Long, Long>> SummingLong_Finisher_3 = a -> Tuple.of(a[0], a[1], a[2]);

    private static final Supplier<KahanSummation> SummingDouble_Supplier = KahanSummation::new;
    private static final Supplier<KahanSummation[]> SummingDouble_Supplier_2 = () -> new KahanSummation[] { new KahanSummation(), new KahanSummation() };
    private static final Supplier<KahanSummation[]> SummingDouble_Supplier_3 = () -> new KahanSummation[] { new KahanSummation(), new KahanSummation(),
            new KahanSummation() };

    private static final BinaryOperator<KahanSummation> SummingDouble_Combiner = (a, b) -> {
        a.combine(b);
        return a;
    };

    private static final BinaryOperator<KahanSummation[]> SummingDouble_Combiner_2 = (a, b) -> {
        a[0].combine(b[0]);
        a[1].combine(b[1]);
        return a;
    };

    private static final BinaryOperator<KahanSummation[]> SummingDouble_Combiner_3 = (a, b) -> {
        a[0].combine(b[0]);
        a[1].combine(b[1]);
        a[2].combine(b[2]);
        return a;
    };

    private static final Function<KahanSummation, Double> SummingDouble_Finisher = KahanSummation::sum;
    private static final Function<KahanSummation[], Tuple2<Double, Double>> SummingDouble_Finisher_2 = a -> Tuple.of(a[0].sum(), a[1].sum());
    private static final Function<KahanSummation[], Tuple3<Double, Double, Double>> SummingDouble_Finisher_3 = a -> Tuple.of(a[0].sum(), a[1].sum(),
            a[2].sum());

    private static final Supplier<BigInteger[]> SummingBigInteger_Supplier = () -> new BigInteger[] { BigInteger.ZERO };
    private static final Supplier<BigInteger[]> SummingBigInteger_Supplier_2 = () -> new BigInteger[] { BigInteger.ZERO, BigInteger.ZERO };
    private static final Supplier<BigInteger[]> SummingBigInteger_Supplier_3 = () -> new BigInteger[] { BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO };

    private static final BinaryOperator<BigInteger[]> SummingBigInteger_Combiner = (a, b) -> {
        a[0] = a[0].add(b[0]);
        return a;
    };

    private static final BinaryOperator<BigInteger[]> SummingBigInteger_Combiner_2 = (a, b) -> {
        a[0] = a[0].add(b[0]);
        a[1] = a[1].add(b[1]);
        return a;
    };

    private static final BinaryOperator<BigInteger[]> SummingBigInteger_Combiner_3 = (a, b) -> {
        a[0] = a[0].add(b[0]);
        a[1] = a[1].add(b[1]);
        a[2] = a[2].add(b[2]);
        return a;
    };

    private static final Function<BigInteger[], BigInteger> SummingBigInteger_Finisher = a -> a[0];
    private static final Function<BigInteger[], Tuple2<BigInteger, BigInteger>> SummingBigInteger_Finisher_2 = a -> Tuple.of(a[0], a[1]);
    private static final Function<BigInteger[], Tuple3<BigInteger, BigInteger, BigInteger>> SummingBigInteger_Finisher_3 = a -> Tuple.of(a[0], a[1], a[2]);

    private static final Supplier<BigDecimal[]> SummingBigDecimal_Supplier = () -> new BigDecimal[] { BigDecimal.ZERO };
    private static final Supplier<BigDecimal[]> SummingBigDecimal_Supplier_2 = () -> new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO };
    private static final Supplier<BigDecimal[]> SummingBigDecimal_Supplier_3 = () -> new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO };

    private static final BinaryOperator<BigDecimal[]> SummingBigDecimal_Combiner = (a, b) -> {
        a[0] = a[0].add(b[0]);
        return a;
    };

    private static final BinaryOperator<BigDecimal[]> SummingBigDecimal_Combiner_2 = (a, b) -> {
        a[0] = a[0].add(b[0]);
        a[1] = a[1].add(b[1]);
        return a;
    };

    private static final BinaryOperator<BigDecimal[]> SummingBigDecimal_Combiner_3 = (a, b) -> {
        a[0] = a[0].add(b[0]);
        a[1] = a[1].add(b[1]);
        a[2] = a[2].add(b[2]);
        return a;
    };

    private static final Function<BigDecimal[], BigDecimal> SummingBigDecimal_Finisher = a -> a[0];
    private static final Function<BigDecimal[], Tuple2<BigDecimal, BigDecimal>> SummingBigDecimal_Finisher_2 = a -> Tuple.of(a[0], a[1]);
    private static final Function<BigDecimal[], Tuple3<BigDecimal, BigDecimal, BigDecimal>> SummingBigDecimal_Finisher_3 = a -> Tuple.of(a[0], a[1], a[2]);

    private static final Supplier<long[]> AveragingInt_Supplier = () -> new long[2];
    private static final Supplier<Pair<long[], long[]>> AveragingInt_Supplier_2 = () -> Pair.of(new long[2], new long[2]);
    private static final Supplier<Pair<long[], long[]>> AveragingInt_Supplier_3 = () -> Pair.of(new long[3], new long[3]);

    private static final BinaryOperator<long[]> AveragingInt_Combiner = (a, b) -> {
        a[0] += b[0];
        a[1] += b[1];
        return a;
    };

    private static final BinaryOperator<Pair<long[], long[]>> AveragingInt_Combiner_2 = (a, b) -> {
        a.left()[0] += b.left()[0];
        a.left()[1] += b.left()[1];
        a.right()[0] += b.right()[0];
        a.right()[1] += b.right()[1];
        return a;
    };

    private static final BinaryOperator<Pair<long[], long[]>> AveragingInt_Combiner_3 = (a, b) -> {
        a.left()[0] += b.left()[0];
        a.left()[1] += b.left()[1];
        a.left()[2] += b.left()[2];
        a.right()[0] += b.right()[0];
        a.right()[1] += b.right()[1];
        a.right()[2] += b.right()[2];
        return a;
    };

    private static final Function<long[], OptionalDouble> AveragingInt_Finisher_op = a -> {
        if (a[1] == 0) {
            return OptionalDouble.empty();
        } else {
            return OptionalDouble.of(((double) a[0]) / a[1]);
        }
    };

    private static final Function<long[], Double> AveragingInt_Finisher_orElseThrow = a -> {
        if (a[1] == 0) {
            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
        }

        return ((double) a[0]) / a[1];
    };

    private static final Function<Pair<long[], long[]>, Tuple2<Double, Double>> AveragingInt_Finisher_2 = a -> {
        return Tuple.of(a.right()[0] == 0 ? 0.0d : ((double) a.left()[0]) / a.right()[0], a.right()[1] == 0 ? 0.0d : ((double) a.left()[1]) / a.right()[1]);
    };

    private static final Function<Pair<long[], long[]>, Tuple3<Double, Double, Double>> AveragingInt_Finisher_3 = a -> {
        return Tuple.of((a.right()[0] == 0 ? 0.0D : (double) a.left()[0]) / a.right()[0], a.right()[1] == 0 ? 0.0D : ((double) a.left()[1]) / a.right()[1],
                a.right()[2] == 0 ? 0.0D : ((double) a.left()[2]) / a.right()[2]);
    };

    private static final Supplier<long[]> AveragingLong_Supplier = () -> new long[2];
    private static final Supplier<Pair<long[], long[]>> AveragingLong_Supplier_2 = () -> Pair.of(new long[2], new long[2]);
    private static final Supplier<Pair<long[], long[]>> AveragingLong_Supplier_3 = () -> Pair.of(new long[3], new long[3]);

    private static final BinaryOperator<long[]> AveragingLong_Combiner = (a, b) -> {
        a[0] += b[0];
        a[1] += b[1];
        return a;
    };

    private static final BinaryOperator<Pair<long[], long[]>> AveragingLong_Combiner_2 = (a, b) -> {
        a.left()[0] += b.left()[0];
        a.left()[1] += b.left()[1];
        a.right()[0] += b.right()[0];
        a.right()[1] += b.right()[1];
        return a;
    };

    private static final BinaryOperator<Pair<long[], long[]>> AveragingLong_Combiner_3 = (a, b) -> {
        a.left()[0] += b.left()[0];
        a.left()[1] += b.left()[1];
        a.left()[2] += b.left()[2];
        a.right()[0] += b.right()[0];
        a.right()[1] += b.right()[1];
        a.right()[2] += b.right()[2];
        return a;
    };

    private static final Function<long[], OptionalDouble> AveragingLong_Finisher_op = a -> {
        if (a[1] == 0) {
            return OptionalDouble.empty();
        } else {
            return OptionalDouble.of(((double) a[0]) / a[1]);
        }
    };

    private static final Function<long[], Double> AveragingLong_Finisher_orElseThrow = a -> {
        if (a[1] == 0) {
            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
        }

        return ((double) a[0]) / a[1];
    };

    private static final Function<Pair<long[], long[]>, Tuple2<Double, Double>> AveragingLong_Finisher_2 = a -> {
        return Tuple.of(a.right()[0] == 0 ? 0.0D : ((double) a.left()[0]) / a.right()[0], a.right()[1] == 0 ? 0.0D : ((double) a.left()[1]) / a.right()[1]);
    };

    private static final Function<Pair<long[], long[]>, Tuple3<Double, Double, Double>> AveragingLong_Finisher_3 = a -> {
        return Tuple.of(a.right()[0] == 0 ? 0.0D : ((double) a.left()[0]) / a.right()[0], a.right()[1] == 0 ? 0.0D : ((double) a.left()[1]) / a.right()[1],
                a.right()[2] == 0 ? 0.0D : ((double) a.left()[2]) / a.right()[2]);
    };

    private static final Supplier<KahanSummation> AveragingDouble_Supplier = KahanSummation::new;
    private static final Supplier<KahanSummation[]> AveragingDouble_Supplier_2 = () -> new KahanSummation[] { new KahanSummation(), new KahanSummation() };
    private static final Supplier<KahanSummation[]> AveragingDouble_Supplier_3 = () -> new KahanSummation[] { new KahanSummation(), new KahanSummation(),
            new KahanSummation() };

    private static final BinaryOperator<KahanSummation> AveragingDouble_Combiner = (a, b) -> {
        a.combine(b);
        return a;
    };

    private static final BinaryOperator<KahanSummation[]> AveragingDouble_Combiner_2 = (a, b) -> {
        a[0].combine(b[0]);
        a[1].combine(b[1]);
        return a;
    };

    private static final BinaryOperator<KahanSummation[]> AveragingDouble_Combiner_3 = (a, b) -> {
        a[0].combine(b[0]);
        a[1].combine(b[1]);
        a[2].combine(b[2]);
        return a;
    };

    private static final Function<KahanSummation, OptionalDouble> AveragingDouble_Finisher_op = KahanSummation::average;

    private static final Function<KahanSummation, Double> AveragingDouble_Finisher_orElseThrow = a -> {
        if (a.count() == 0) {
            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
        }

        return a.average().get();
    };

    private static final Function<KahanSummation[], Tuple2<Double, Double>> AveragingDouble_Finisher_2 = a -> {
        return Tuple.of(a[0].count() == 0 ? 0.0D : a[0].average().get(), a[1].count() == 0 ? 0.0D : a[1].average().get());
    };

    private static final Function<KahanSummation[], Tuple3<Double, Double, Double>> AveragingDouble_Finisher_3 = a -> {
        return Tuple.of(a[0].count() == 0 ? 0.0D : a[0].average().get(), a[1].count() == 0 ? 0.0D : a[1].average().get(),
                a[2].count() == 0 ? 0.0D : a[2].average().get());
    };

    private static final Supplier<Pair<BigInteger, long[]>> AveragingBigInteger_Supplier = () -> Pair.of(BigInteger.ZERO, new long[1]);

    private static final Supplier<Pair<BigInteger[], long[]>> AveragingBigInteger_Supplier_2 = () -> Pair
            .of(new BigInteger[] { BigInteger.ZERO, BigInteger.ZERO }, new long[2]);

    private static final Supplier<Pair<BigInteger[], long[]>> AveragingBigInteger_Supplier_3 = () -> Pair
            .of(new BigInteger[] { BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO }, new long[3]);

    private static final BinaryOperator<Pair<BigInteger, long[]>> AveragingBigInteger_Combiner = (a, b) -> {
        a.setLeft(a.left().add(b.left()));
        a.right()[0] += b.right()[0];
        return a;
    };

    private static final BinaryOperator<Pair<BigInteger[], long[]>> AveragingBigInteger_Combiner_2 = (a, b) -> {
        a.left()[0] = a.left()[0].add(b.left()[0]);
        a.left()[1] = a.left()[1].add(b.left()[1]);
        a.right()[0] += b.right()[0];
        a.right()[1] += b.right()[1];
        return a;
    };

    private static final BinaryOperator<Pair<BigInteger[], long[]>> AveragingBigInteger_Combiner_3 = (a, b) -> {
        a.left()[0] = a.left()[0].add(b.left()[0]);
        a.left()[1] = a.left()[1].add(b.left()[1]);
        a.left()[2] = a.left()[2].add(b.left()[2]);
        a.right()[0] += b.right()[0];
        a.right()[1] += b.right()[1];
        a.right()[2] += b.right()[2];
        return a;
    };

    @SuppressWarnings("BigDecimalMethodWithoutRoundingCalled")
    private static final Function<Pair<BigInteger, long[]>, BigDecimal> AveragingBigInteger_Finisher = a -> {
        return a.right()[0] == 0 ? BigDecimal.ZERO : new BigDecimal(a.left()).divide(new BigDecimal(a.right()[0]));
    };

    @SuppressWarnings("BigDecimalMethodWithoutRoundingCalled")
    private static final Function<Pair<BigInteger, long[]>, Optional<BigDecimal>> AveragingBigInteger_Finisher_op = a -> a.right()[0] == 0 ? Optional.empty()
            : Optional.of(new BigDecimal(a.left()).divide(new BigDecimal(a.right()[0])));

    @SuppressWarnings("BigDecimalMethodWithoutRoundingCalled")
    private static final Function<Pair<BigInteger, long[]>, BigDecimal> AveragingBigInteger_Finisher_orElseThrow = a -> {
        if (a.right()[0] == 0) {
            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
        }

        return new BigDecimal(a.left()).divide(new BigDecimal(a.right()[0]));
    };

    @SuppressWarnings("BigDecimalMethodWithoutRoundingCalled")
    private static final Function<Pair<BigInteger[], long[]>, Tuple2<BigDecimal, BigDecimal>> AveragingBigInteger_Finisher_2 = a -> {
        return Tuple.of(a.right()[0] == 0 ? BigDecimal.ZERO : new BigDecimal(a.left()[0]).divide(new BigDecimal(a.right()[0])),
                a.right()[1] == 0 ? BigDecimal.ZERO : new BigDecimal(a.left()[1]).divide(new BigDecimal(a.right()[1])));
    };

    @SuppressWarnings("BigDecimalMethodWithoutRoundingCalled")
    private static final Function<Pair<BigInteger[], long[]>, Tuple3<BigDecimal, BigDecimal, BigDecimal>> AveragingBigInteger_Finisher_3 = a -> {
        return Tuple.of(a.right()[0] == 0 ? BigDecimal.ZERO : new BigDecimal(a.left()[0]).divide(new BigDecimal(a.right()[0])),
                a.right()[1] == 0 ? BigDecimal.ZERO : new BigDecimal(a.left()[1]).divide(new BigDecimal(a.right()[1])),
                a.right()[2] == 0 ? BigDecimal.ZERO : new BigDecimal(a.left()[2]).divide(new BigDecimal(a.right()[2])));
    };

    private static final Supplier<Pair<BigDecimal, long[]>> AveragingBigDecimal_Supplier = () -> Pair.of(BigDecimal.ZERO, new long[1]);

    private static final Supplier<Pair<BigDecimal[], long[]>> AveragingBigDecimal_Supplier_2 = () -> Pair
            .of(new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO }, new long[2]);

    private static final Supplier<Pair<BigDecimal[], long[]>> AveragingBigDecimal_Supplier_3 = () -> Pair
            .of(new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO }, new long[3]);

    private static final BinaryOperator<Pair<BigDecimal, long[]>> AveragingBigDecimal_Combiner = (a, b) -> {
        a.setLeft(a.left().add(b.left()));
        a.right()[0] += b.right()[0];
        return a;
    };

    private static final BinaryOperator<Pair<BigDecimal[], long[]>> AveragingBigDecimal_Combiner_2 = (a, b) -> {
        a.left()[0] = a.left()[0].add(b.left()[0]);
        a.left()[1] = a.left()[1].add(b.left()[1]);
        a.right()[0] += b.right()[0];
        a.right()[1] += b.right()[1];
        return a;
    };

    private static final BinaryOperator<Pair<BigDecimal[], long[]>> AveragingBigDecimal_Combiner_3 = (a, b) -> {
        a.left()[0] = a.left()[0].add(b.left()[0]);
        a.left()[1] = a.left()[1].add(b.left()[1]);
        a.left()[2] = a.left()[2].add(b.left()[2]);
        a.right()[0] += b.right()[0];
        a.right()[1] += b.right()[1];
        a.right()[2] += b.right()[2];
        return a;
    };

    @SuppressWarnings("BigDecimalMethodWithoutRoundingCalled")
    private static final Function<Pair<BigDecimal, long[]>, BigDecimal> AveragingBigDecimal_Finisher = a -> {
        return a.right()[0] == 0 ? BigDecimal.ZERO : a.left().divide(new BigDecimal(a.right()[0]));
    };

    @SuppressWarnings("BigDecimalMethodWithoutRoundingCalled")
    private static final Function<Pair<BigDecimal, long[]>, Optional<BigDecimal>> AveragingBigDecimal_Finisher_op = a -> a.right()[0] == 0 ? Optional.empty()
            : Optional.of(a.left().divide(new BigDecimal(a.right()[0])));

    @SuppressWarnings("BigDecimalMethodWithoutRoundingCalled")
    private static final Function<Pair<BigDecimal, long[]>, BigDecimal> AveragingBigDecimal_Finisher_orElseThrow = a -> {
        if (a.right()[0] == 0) {
            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
        }

        return a.left().divide(new BigDecimal(a.right()[0]));
    };

    @SuppressWarnings("BigDecimalMethodWithoutRoundingCalled")
    private static final Function<Pair<BigDecimal[], long[]>, Tuple2<BigDecimal, BigDecimal>> AveragingBigDecimal_Finisher_2 = a -> {
        return Tuple.of(a.right()[0] == 0 ? BigDecimal.ZERO : a.left()[0].divide(new BigDecimal(a.right()[0])),
                a.right()[1] == 0 ? BigDecimal.ZERO : a.left()[1].divide(new BigDecimal(a.right()[1])));
    };

    @SuppressWarnings("BigDecimalMethodWithoutRoundingCalled")
    private static final Function<Pair<BigDecimal[], long[]>, Tuple3<BigDecimal, BigDecimal, BigDecimal>> AveragingBigDecimal_Finisher_3 = a -> {
        return Tuple.of(a.right()[0] == 0 ? BigDecimal.ZERO : a.left()[0].divide(new BigDecimal(a.right()[0])),
                a.right()[1] == 0 ? BigDecimal.ZERO : a.left()[1].divide(new BigDecimal(a.right()[1])),
                a.right()[2] == 0 ? BigDecimal.ZERO : a.left()[2].divide(new BigDecimal(a.right()[2])));
    };

    private static final Supplier<CharSummaryStatistics> SummarizingChar_Supplier = CharSummaryStatistics::new;

    private static final BinaryOperator<CharSummaryStatistics> SummarizingChar_Combiner = (a, b) -> {
        a.combine(b);
        return a;
    };

    private static final Supplier<ByteSummaryStatistics> SummarizingByte_Supplier = ByteSummaryStatistics::new;

    private static final BinaryOperator<ByteSummaryStatistics> SummarizingByte_Combiner = (a, b) -> {
        a.combine(b);
        return a;
    };

    private static final Supplier<ShortSummaryStatistics> SummarizingShort_Supplier = ShortSummaryStatistics::new;

    private static final BinaryOperator<ShortSummaryStatistics> SummarizingShort_Combiner = (a, b) -> {
        a.combine(b);
        return a;
    };

    private static final Supplier<IntSummaryStatistics> SummarizingInt_Supplier = IntSummaryStatistics::new;

    private static final BinaryOperator<IntSummaryStatistics> SummarizingInt_Combiner = (a, b) -> {
        a.combine(b);
        return a;
    };

    private static final Supplier<LongSummaryStatistics> SummarizingLong_Supplier = LongSummaryStatistics::new;

    private static final BinaryOperator<LongSummaryStatistics> SummarizingLong_Combiner = (a, b) -> {
        a.combine(b);
        return a;
    };

    private static final Supplier<FloatSummaryStatistics> SummarizingFloat_Supplier = FloatSummaryStatistics::new;

    private static final BinaryOperator<FloatSummaryStatistics> SummarizingFloat_Combiner = (a, b) -> {
        a.combine(b);
        return a;
    };

    private static final Supplier<DoubleSummaryStatistics> SummarizingDouble_Supplier = DoubleSummaryStatistics::new;

    private static final BinaryOperator<DoubleSummaryStatistics> SummarizingDouble_Combiner = (a, b) -> {
        a.combine(b);
        return a;
    };

    private static final Supplier<BigIntegerSummaryStatistics> SummarizingBigInteger_Supplier = BigIntegerSummaryStatistics::new;

    private static final BinaryOperator<BigIntegerSummaryStatistics> SummarizingBigInteger_Combiner = (a, b) -> {
        a.combine(b);
        return a;
    };

    private static final Supplier<BigDecimalSummaryStatistics> SummarizingBigDecimal_Supplier = BigDecimalSummaryStatistics::new;

    private static final BinaryOperator<BigDecimalSummaryStatistics> SummarizingBigDecimal_Combiner = (a, b) -> {
        a.combine(b);
        return a;
    };

    private static final Function<Holder<Object>, Object> Reducing_Finisher_0 = Holder::value;

    private static final BiConsumer<OptHolder<Object>, Object> Reducing_Accumulator = OptHolder::accept;

    private static final BinaryOperator<OptHolder<Object>> Reducing_Combiner = (a, b) -> {
        if (b.present) {
            a.accept(b.value);
        }

        return a;
    };

    private static final Function<OptHolder<Object>, Optional<Object>> Reducing_Finisher = a -> a.present ? Optional.of(a.value)
            : (Optional<Object>) Optional.empty();

    private static final BiConsumer<MappingOptHolder<Object, Object>, Object> Reducing_Accumulator_2 = MappingOptHolder::accept;

    private static final BinaryOperator<MappingOptHolder<Object, Object>> Reducing_Combiner_2 = (a, b) -> {
        if (b.present) {
            if (a.present) {
                a.value = a.op.apply(a.value, b.value);
            } else {
                a.value = b.value;
                a.present = true;
            }
        }

        return a;
    };

    private static final Function<MappingOptHolder<Object, Object>, Optional<Object>> Reducing_Finisher_2 = a -> a.present ? Optional.of(a.value)
            : (Optional<Object>) Optional.empty();

    // ============================================================================================================

    Collectors() {
    }

    /**
     * Creates a new {@code Collector} with the specified supplier, accumulator, and combiner.
     *
     * <p>This is a factory method for creating custom collectors. The type parameter indicates
     * that the intermediate accumulation type is the same as the final result type.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a custom collector that concatenates strings
     * Collector<String, StringBuilder, StringBuilder> collector = Collectors.create(
     *     StringBuilder::new,
     *     (sb, s) -> sb.append(s).append(","),
     *     (sb1, sb2) -> sb1.append(sb2)
     * );
     * }</pre>
     *
     * @param <T> the type of input elements to the collector
     * @param <R> the type of the mutable result container and the final result
     * @param supplier the supplier function that provides a new mutable result container
     * @param accumulator the accumulator function that folds a value into a mutable result container
     * @param combiner the combiner function that merges two result containers
     * @param characteristics optional characteristics of the collector
     * @return a new {@code Collector} with the specified supplier, accumulator, and combiner
     * @see Collector#of(Supplier, BiConsumer, BinaryOperator, Characteristics...)
     */
    public static <T, R> Collector<T, R, R> create(final Supplier<? extends R> supplier, final BiConsumer<? super R, ? super T> accumulator,
            final BinaryOperator<R> combiner, final Characteristics... characteristics) {
        return Collector.of((Supplier<R>) supplier, (BiConsumer<R, T>) accumulator, combiner, characteristics);
    }

    /**
     * Creates a new {@code Collector} with the specified supplier, accumulator, and combiner.
     *
     * <p>This is a factory method for creating custom collectors. The type parameter indicates
     * that the intermediate accumulation type is the same as the final result type. This variant
     * accepts characteristics as a Collection rather than varargs.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a custom collector with characteristics collection
     * Set<Collector.Characteristics> chars = EnumSet.of(Collector.Characteristics.CONCURRENT);
     * Collector<String, StringBuilder, StringBuilder> collector = Collectors.create(
     *     StringBuilder::new,
     *     (sb, s) -> sb.append(s),
     *     (sb1, sb2) -> sb1.append(sb2),
     *     chars
     * );
     * }</pre>
     *
     * @param <T> the type of input elements to the collector
     * @param <R> the type of the mutable result container and the final result
     * @param supplier the supplier function that provides a new mutable result container
     * @param accumulator the accumulator function that folds a value into a mutable result container
     * @param combiner the combiner function that merges two result containers
     * @param characteristics optional characteristics of the collector
     * @return a new {@code Collector} with the specified supplier, accumulator, and combiner
     * @see Collector#of(Supplier, BiConsumer, BinaryOperator, Characteristics...)
     */
    public static <T, R> Collector<T, R, R> create(final Supplier<? extends R> supplier, final BiConsumer<? super R, ? super T> accumulator,
            final BinaryOperator<R> combiner, final Collection<Characteristics> characteristics) {
        return Collector.of((Supplier<R>) supplier, (BiConsumer<R, T>) accumulator, combiner,
                N.isEmpty(characteristics) ? CH_NOID : characteristics.toArray(Characteristics[]::new));
    }

    /**
     * Creates a new {@code Collector} with the specified supplier, accumulator, combiner, and finisher.
     *
     * <p>This factory method creates custom collectors where the intermediate accumulation type
     * differs from the final result type. The finisher function transforms the accumulated result
     * into the desired output type.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a collector that builds a string from integers
     * Collector<Integer, StringBuilder, String> collector = Collectors.create(
     *     StringBuilder::new,
     *     (sb, i) -> sb.append(i).append(" "),
     *     (sb1, sb2) -> sb1.append(sb2),
     *     StringBuilder::toString
     * );
     * }</pre>
     *
     * @param <T> the type of input elements to the collector
     * @param <A> the type of the intermediate accumulation result
     * @param <R> the type of the final result of the collector
     * @param supplier the supplier function that provides a new mutable result container
     * @param accumulator the accumulator function that folds a value into a mutable result container
     * @param combiner the combiner function that merges two result containers
     * @param finisher the function that transforms the intermediate result to the final result
     * @param characteristics optional characteristics of the collector
     * @return a new {@code Collector} with the specified components
     * @see Collector#of(Supplier, BiConsumer, BinaryOperator, Function, Characteristics...)
     */
    public static <T, A, R> Collector<T, A, R> create(final Supplier<? extends A> supplier, final BiConsumer<? super A, ? super T> accumulator,
            final BinaryOperator<A> combiner, final Function<? super A, ? extends R> finisher, final Characteristics... characteristics) {
        return Collector.of((Supplier<A>) supplier, (BiConsumer<A, T>) accumulator, combiner, (Function<A, R>) finisher, characteristics);
    }

    /**
     * Creates a new {@code Collector} with the specified supplier, accumulator, combiner, and finisher.
     *
     * <p>This factory method creates custom collectors where the intermediate accumulation type
     * differs from the final result type. The finisher function transforms the accumulated result
     * into the desired output type. This variant accepts characteristics as a Collection rather
     * than varargs.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a collector with finisher and characteristics collection
     * Set<Collector.Characteristics> chars = EnumSet.of(Collector.Characteristics.UNORDERED);
     * Collector<Integer, List<Integer>, Integer> collector = Collectors.create(
     *     ArrayList::new,
     *     List::add,
     *     (l1, l2) -> { l1.addAll(l2); return l1; },
     *     list -> list.stream().mapToInt(Integer::intValue).sum(),
     *     chars
     * );
     * }</pre>
     *
     * @param <T> the type of input elements to the collector
     * @param <A> the type of the intermediate accumulation result
     * @param <R> the type of the final result of the collector
     * @param supplier the supplier function that provides a new mutable result container
     * @param accumulator the accumulator function that folds a value into a mutable result container
     * @param combiner the combiner function that merges two result containers
     * @param finisher the function that transforms the intermediate result to the final result
     * @param characteristics optional characteristics of the collector
     * @return a new {@code Collector} with the specified components
     * @see Collector#of(Supplier, BiConsumer, BinaryOperator, Function, Characteristics...)
     */
    public static <T, A, R> Collector<T, A, R> create(final Supplier<? extends A> supplier, final BiConsumer<? super A, ? super T> accumulator,
            final BinaryOperator<A> combiner, final Function<? super A, ? extends R> finisher, final Collection<Characteristics> characteristics) {
        return Collector.of((Supplier<A>) supplier, (BiConsumer<A, T>) accumulator, combiner, (Function<A, R>) finisher,
                N.isEmpty(characteristics) ? CH_NOID : characteristics.toArray(Characteristics[]::new));
    }

    /**
     * Returns a {@code Collector} that accumulates the input elements into a new collection,
     * created by the provided factory function.
     *
     * <p>This collector is useful when you need to collect elements into a specific type of
     * collection that is not covered by the standard collectors (like {@code toList()},
     * {@code toSet()}, etc.). The collection factory should return a new, empty collection
     * instance each time it's called.</p>
     *
     * <p><b>Null Handling:</b> The collector allows null elements if the underlying collection
     * supports null values. The behavior depends on the collection implementation provided
     * by the factory.</p>
     *
     * <p><b>Parallel Stream Support:</b> This collector supports parallel streams. When used
     * with parallel streams, partial results are combined using the larger collection as the
     * target for better performance.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect to a LinkedList
     * LinkedList<Integer> numbers = Stream.of(1, 2, 3, 4, 5)
     *     .collect(Collectors.toCollection(LinkedList::new));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <C> the type of the resulting collection
     * @param collectionFactory a supplier providing a new empty collection into which
     *                         the results will be inserted
     * @return a {@code Collector} which collects all input elements into a collection,
     *         in encounter order
     * @see #toCollection(int, Supplier)
     * @see #toCollection(Supplier, BiConsumer)
     * @see #toList()
     * @see #toSet()
     */
    public static <T, C extends Collection<T>> Collector<T, ?, C> toCollection(final Supplier<? extends C> collectionFactory) {
        final BiConsumer<C, T> accumulator = BiConsumers.ofAdd();
        final BinaryOperator<C> combiner = BinaryOperators.ofAddAllToBigger();

        return create(collectionFactory, accumulator, combiner, CH_ID);
    }

    /**
     * Returns a {@code Collector} that accumulates the input elements into a new collection,
     * created by the provided factory function, limiting the collection to at most the
     * specified number of elements.
     *
     * <p>This collector is particularly useful when you want to collect only a limited number
     * of elements from a potentially large stream. Once the collection reaches the specified
     * size limit, subsequent elements are ignored. For parallel streams, the combiner ensures
     * that the final collection size does not exceed the limit.</p>
     *
     * <p>When combining collections in parallel processing, if the target collection is a
     * {@code List}, the combiner uses {@code subList} for efficiency. For other collection
     * types, it iterates through elements until the size limit is reached.</p>
     *
     * <p><b>Null Handling:</b> The collector allows null elements if the underlying collection
     * supports null values. Null elements count toward the size limit.</p>
     *
     * <p><b>Parallel Stream Support:</b> This collector supports parallel streams with
     * appropriate combining logic to maintain the size limit.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect first 10 elements from a large stream
     * List<Integer> firstTen = Stream.iterate(1, n -> n + 1)
     *     .limit(1000)
     *     .collect(Collectors.toCollection(10, ArrayList::new));
     * // Result will contain only [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <C> the type of the resulting collection
     * @param atMostSize the maximum number of elements to collect
     * @param collectionFactory a supplier providing a new empty collection into which
     *                         the results will be inserted
     * @return a {@code Collector} which collects at most the specified number of input
     *         elements into a collection, in encounter order
     * @throws IllegalArgumentException if {@code atMostSize} is negative
     * @see #toCollection(Supplier)
     * @see #first(int)
     * @see #toList(int)
     */
    public static <T, C extends Collection<T>> Collector<T, ?, C> toCollection(final int atMostSize, final Supplier<? extends C> collectionFactory) {
        final BiConsumer<C, T> accumulator = (c, t) -> {
            if (c.size() < atMostSize) {
                c.add(t);
            }
        };

        final BinaryOperator<C> combiner = (a, b) -> {
            if (a.size() < atMostSize) {
                final int n = atMostSize - a.size();

                if (b.size() <= n) {
                    a.addAll(b);
                } else {
                    if (b instanceof List) {
                        a.addAll(((List<T>) b).subList(0, n));
                    } else {
                        final Iterator<T> iter = b.iterator();

                        while (iter.hasNext() && a.size() < atMostSize) {
                            a.add(iter.next());
                        }
                    }
                }
            }

            return a;
        };

        return create(collectionFactory, accumulator, combiner, CH_ID);
    }

    /**
     * Returns a {@code Collector} that accumulates the input elements into a new collection,
     * created by the provided factory function, using a custom accumulator.
     *
     * <p>This collector allows you to specify custom logic for how elements are added to
     * the collection. The accumulator function receives the collection and an element,
     * and is responsible for updating the collection.</p>
     *
     * <p>The combiner uses the default behavior of adding all elements from one collection
     * to another, with the larger collection being used as the target.</p>
     *
     * <p><b>Null Handling:</b> Null handling depends on the custom accumulator implementation.
     * The accumulator function is responsible for handling null elements appropriately.</p>
     *
     * <p><b>Parallel Stream Support:</b> This collector supports parallel streams with
     * a default combiner that merges collections by adding elements to the larger one.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect only even numbers
     * List<Integer> evens = Stream.of(1, 2, 3, 4, 5, 6)
     *     .collect(Collectors.toCollection(ArrayList::new,
     *         (list, n) -> { if (n % 2 == 0) list.add(n); }));
     * // Result: [2, 4, 6]
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <C> the type of the resulting collection
     * @param supplier a supplier providing a new empty collection into which
     *                 the results will be inserted
     * @param accumulator a function for incorporating a new element into a collection
     * @return a {@code Collector} which collects input elements into a collection
     *         using the specified accumulator
     * @see #toCollection(Supplier, BiConsumer, BinaryOperator)
     * @see #toCollection(Supplier)
     */
    public static <T, C extends Collection<T>> Collector<T, ?, C> toCollection(final Supplier<? extends C> supplier, final BiConsumer<C, T> accumulator) {
        final BinaryOperator<C> combiner = BinaryOperators.ofAddAllToBigger();

        return toCollection(supplier, accumulator, combiner);
    }

    /**
     * Returns a {@code Collector} that accumulates the input elements into a new collection,
     * created by the provided factory function, using custom accumulator and combiner functions.
     *
     * <p>This collector provides full control over how elements are accumulated into the
     * collection and how partial collections are combined in parallel processing. This is
     * the most flexible form of collection collector.</p>
     *
     * <p>The accumulator function defines how individual elements are added to the collection.
     * The combiner function defines how two collections are merged together, which is
     * essential for parallel stream processing.</p>
     *
     * <p><b>Null Handling:</b> Null handling depends on the custom accumulator and combiner
     * implementations. These functions are responsible for handling null elements and null
     * collections appropriately.</p>
     *
     * <p><b>Parallel Stream Support:</b> This collector supports parallel streams with
     * the provided custom combiner function for merging partial results.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Custom collector that adds elements in reverse order
     * List<String> reversed = Stream.of("a", "b", "c")
     *     .collect(Collectors.toCollection(
     *         LinkedList::new,
     *         (list, elem) -> list.addFirst(elem),
     *         (list1, list2) -> { list1.addAll(0, list2); return list1; }
     *     ));
     * // Result: ["c", "b", "a"]
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <C> the type of the resulting collection
     * @param supplier a supplier providing a new empty collection into which
     *                 the results will be inserted
     * @param accumulator a function for incorporating a new element into a collection
     * @param combiner a function for combining two collections into one
     * @return a {@code Collector} which collects input elements into a collection
     *         using the specified accumulator and combiner
     * @see #toCollection(Supplier, BiConsumer)
     * @see #toCollection(Supplier)
     * @see #create(Supplier, BiConsumer, BinaryOperator, Characteristics...)
     */
    public static <T, C extends Collection<T>> Collector<T, ?, C> toCollection(final Supplier<? extends C> supplier, final BiConsumer<C, T> accumulator,
            final BinaryOperator<C> combiner) {
        return toCollection(supplier, accumulator, combiner, CH_ID);
    }

    /*     
     * Returns a {@code Collector} that accumulates the input elements into a new collection,
     * created by the provided factory function, using custom accumulator and combiner functions,
     * along with specified characteristics.
     * 
     * <p>This collector provides full control over how elements are accumulated into the
     * collection and how partial collections are combined in parallel processing, while also
     * allowing you to specify characteristics like CONCURRENT or UNORDERED.</p>
     * 
     * @param <T> the type of input elements
     * @param <C> the type of the resulting collection
     * @param supplier a supplier providing a new empty collection into which 
     *                 the results will be inserted
     * @param accumulator a function for incorporating a new element into a collection
     * @param combiner a function for combining two collections into one
     * @param characteristics optional characteristics of the collector
     * @return a {@code Collector} which collects input elements into a collection
     */
    static <T, C extends Collection<T>> Collector<T, ?, C> toCollection(final Supplier<? extends C> supplier, final BiConsumer<C, T> accumulator,
            final BinaryOperator<C> combiner, final Characteristics... characteristics) {
        return create(supplier, accumulator, combiner, characteristics);
    }

    /**
     * Returns a {@code Collector} that accumulates the input elements into a new {@code List}.
     *
     * <p>This is one of the most commonly used collectors. It collects all stream elements
     * into a {@code List} in encounter order. There are no guarantees on the type,
     * mutability, serializability, or thread-safety of the {@code List} returned.</p>
     *
     * <p>For streams with defined encounter order, the elements will appear in the
     * resulting list in the same order. For unordered streams, no order is guaranteed.</p>
     *
     * <p><b>Null Handling:</b> This collector accepts null elements. The resulting list
     * will contain null values in the positions where they were encountered in the stream.</p>
     *
     * <p><b>Parallel Stream Support:</b> This collector supports parallel streams and
     * efficiently combines partial results.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect stream elements to a list
     * List<String> list = Stream.of("apple", "banana", "orange")
     *     .collect(Collectors.toList());
     * // Result: ["apple", "banana", "orange"]
     * }</pre>
     *
     * @param <T> the type of input elements
     * @return a {@code Collector} which collects all input elements into a {@code List},
     *         in encounter order
     * @see #toLinkedList()
     * @see #toImmutableList()
     * @see #toUnmodifiableList()
     * @see #toList(int)
     */
    public static <T> Collector<T, ?, List<T>> toList() {
        final Supplier<List<T>> supplier = Suppliers.ofList();

        return toCollection(supplier);
    }

    /**
     * Returns a {@code Collector} that accumulates the input elements into a new {@code LinkedList}.
     *
     * <p>This collector is useful when you specifically need a {@code LinkedList} implementation,
     * for example when you need efficient insertion and removal at both ends of the list.
     * Elements are collected in encounter order.</p>
     *
     * <p>Unlike the generic {@code toList()} collector, this method guarantees that the
     * returned collection will be a {@code LinkedList}.</p>
     *
     * <p><b>Null Handling:</b> This collector accepts null elements as {@code LinkedList}
     * supports null values.</p>
     *
     * <p><b>Parallel Stream Support:</b> This collector supports parallel streams and
     * efficiently combines partial results.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect to a LinkedList for efficient head/tail operations
     * LinkedList<Integer> linkedList = Stream.of(1, 2, 3, 4, 5)
     *     .collect(Collectors.toLinkedList());
     * // Can now efficiently use: linkedList.addFirst(0), linkedList.removeLast(), etc.
     * }</pre>
     *
     * @param <T> the type of input elements
     * @return a {@code Collector} which collects all input elements into a {@code LinkedList},
     *         in encounter order
     * @see #toList()
     * @see #toDeque()
     */
    public static <T> Collector<T, ?, LinkedList<T>> toLinkedList() {
        final Supplier<LinkedList<T>> supplier = Suppliers.ofLinkedList();

        return toCollection(supplier);
    }

    /**
     * Returns a {@code Collector} that accumulates the input elements into an {@code ImmutableList}.
     *
     * <p>This collector first collects all elements into a regular list, then converts it
     * to an immutable list. The returned list cannot be modified after creation - any
     * attempt to modify it will result in an {@code UnsupportedOperationException}.</p>
     *
     * <p>This is useful when you need to ensure that the collected data cannot be
     * accidentally modified after collection. Elements are preserved in encounter order.</p>
     *
     * <p><b>Null Handling:</b> This collector accepts null elements if they are present
     * in the stream. The resulting immutable list will contain null values.</p>
     *
     * <p><b>Parallel Stream Support:</b> This collector supports parallel streams and
     * efficiently combines partial results.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create an immutable list from stream elements
     * ImmutableList<String> immutable = Stream.of("a", "b", "c")
     *     .collect(Collectors.toImmutableList());
     * // immutable.add("d");   // This would throw UnsupportedOperationException
     * }</pre>
     *
     * @param <T> the type of input elements
     * @return a {@code Collector} which collects all input elements into an {@code ImmutableList},
     *         in encounter order
     * @see #toList()
     * @see #toUnmodifiableList()
     * @see #toImmutableSet()
     */
    public static <T> Collector<T, ?, ImmutableList<T>> toImmutableList() {
        final Collector<T, ?, List<T>> downstream = toList();
        @SuppressWarnings("rawtypes")
        final Function<List<T>, ImmutableList<T>> finisher = (Function) ImmutableList_Finisher;

        return collectingAndThen(downstream, finisher);
    }

    /**
     * Returns a {@code Collector} that accumulates the input elements into an unmodifiable {@code List}.
     * 
     * <p>This collector produces a list that cannot be modified after creation. Any attempt
     * to modify the returned list, either directly or through its iterator, will result
     * in an {@code UnsupportedOperationException}.</p>
     * 
     * <p>This method delegates to the JDK's {@code Collectors.toUnmodifiableList()} method
     * and provides the same guarantees. Elements are preserved in encounter order.</p>
     *
     * <p><b>Null Handling:</b> This collector does not accept null elements. If the stream
     * contains any null values, a {@code NullPointerException} will be thrown.</p>
     *
     * <p><b>Parallel Stream Support:</b> This collector supports parallel streams and
     * efficiently combines partial results.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create an unmodifiable list
     * List<Integer> unmodifiable = Stream.of(1, 2, 3)
     *     .collect(Collectors.toUnmodifiableList());
     * // unmodifiable.add(4);   // This would throw UnsupportedOperationException
     * }</pre>
     *
     * @param <T> the type of input elements
     * @return a {@code Collector} which collects all input elements into an unmodifiable {@code List}
     * @see java.util.stream.Collectors#toUnmodifiableList
     */
    public static <T> Collector<T, ?, List<T>> toUnmodifiableList() {
        return java.util.stream.Collectors.toUnmodifiableList();
    }

    /**
     * Returns a {@code Collector} that accumulates the input elements into a new {@code Set}.
     *
     * <p>This collector accumulates elements into a {@code Set}, automatically removing
     * duplicates. The elements are compared using their {@code equals} method. There are
     * no guarantees on the type, mutability, serializability, or thread-safety of the
     * {@code Set} returned.</p>
     *
     * <p>For ordered streams, no order is preserved in the resulting set, as sets
     * are inherently unordered collections (unless a specific ordered set implementation
     * is used).</p>
     *
     * <p><b>Null Handling:</b> This collector accepts null elements if the underlying
     * set implementation supports null values.</p>
     *
     * <p><b>Parallel Stream Support:</b> This collector supports parallel streams and
     * has the UNORDERED characteristic for optimal performance.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect unique elements to a set
     * Set<Integer> uniqueNumbers = Stream.of(1, 2, 2, 3, 3, 3)
     *     .collect(Collectors.toSet());
     * // Result: [1, 2, 3] (order not guaranteed)
     * }</pre>
     *
     * @param <T> the type of input elements
     * @return a {@code Collector} which collects all input elements into a {@code Set}
     * @see #toLinkedHashSet()
     * @see #toImmutableSet()
     * @see #toUnmodifiableSet()
     * @see #toSet(int)
     */
    public static <T> Collector<T, ?, Set<T>> toSet() {
        final Supplier<Set<T>> supplier = Suppliers.ofSet();
        final BiConsumer<Set<T>, T> accumulator = BiConsumers.ofAdd();
        final BinaryOperator<Set<T>> combiner = BinaryOperators.ofAddAllToBigger();

        return create(supplier, accumulator, combiner, CH_UNORDERED_ID);
    }

    /**
     * Returns a {@code Collector} that accumulates the input elements into a new {@code LinkedHashSet}.
     * 
     * <p>This collector accumulates elements into a {@code LinkedHashSet}, which maintains
     * insertion order while removing duplicates. This is useful when you need both the
     * uniqueness guarantee of a set and predictable iteration order.</p>
     * 
     * <p>Elements are compared using their {@code equals} method for uniqueness, and the
     * iteration order matches the order in which elements were first encountered in the stream.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect unique elements preserving order
     * Set<String> orderedSet = Stream.of("c", "a", "b", "a", "c")
     *     .collect(Collectors.toLinkedHashSet());
     * // Result: ["c", "a", "b"] in that order
     * }</pre>
     *
     * @param <T> the type of input elements
     * @return a {@code Collector} which collects all input elements into a {@code LinkedHashSet}
     */
    public static <T> Collector<T, ?, Set<T>> toLinkedHashSet() {
        final Supplier<Set<T>> supplier = Suppliers.ofLinkedHashSet();
        final BiConsumer<Set<T>, T> accumulator = BiConsumers.ofAdd();
        final BinaryOperator<Set<T>> combiner = BinaryOperators.ofAddAllToBigger();

        return create(supplier, accumulator, combiner, CH_UNORDERED_ID);

    }

    /**
     * Returns a {@code Collector} that accumulates the input elements into an {@code ImmutableSet}.
     * 
     * <p>This collector first collects all unique elements into a regular set, then converts
     * it to an immutable set. The returned set cannot be modified after creation - any
     * attempt to modify it will result in an {@code UnsupportedOperationException}.</p>
     * 
     * <p>Duplicate elements are removed based on their {@code equals} method. The iteration
     * order of the resulting set is not specified.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create an immutable set from stream elements
     * ImmutableSet<String> immutable = Stream.of("a", "b", "b", "c")
     *     .collect(Collectors.toImmutableSet());
     * // Result contains: ["a", "b", "c"]
     * // immutable.add("d");   // This would throw UnsupportedOperationException
     * }</pre>
     *
     * @param <T> the type of input elements
     * @return a {@code Collector} which collects all input elements into an {@code ImmutableSet}
     */
    public static <T> Collector<T, ?, ImmutableSet<T>> toImmutableSet() {
        final Collector<T, ?, Set<T>> downstream = toSet();
        @SuppressWarnings("rawtypes")
        final Function<Set<T>, ImmutableSet<T>> finisher = (Function) ImmutableSet_Finisher;

        return collectingAndThen(downstream, finisher);
    }

    /**
     * Returns a {@code Collector} that accumulates the input elements into an unmodifiable {@code Set}.
     * 
     * <p>This collector produces a set that cannot be modified after creation. Any attempt
     * to modify the returned set, either directly or through its iterator, will result
     * in an {@code UnsupportedOperationException}. Duplicates are removed based on the
     * {@code equals} method.</p>
     * 
     * <p>This method delegates to the JDK's {@code Collectors.toUnmodifiableSet()} method
     * and provides the same guarantees.</p>
     *
     * <p><b>Null Handling:</b> This collector does not accept null elements. If the stream
     * contains any null values, a {@code NullPointerException} will be thrown.</p>
     *
     * <p><b>Parallel Stream Support:</b> This collector supports parallel streams and
     * efficiently combines partial results.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create an unmodifiable set
     * Set<Integer> unmodifiable = Stream.of(1, 2, 2, 3)
     *     .collect(Collectors.toUnmodifiableSet());
     * // Result contains: [1, 2, 3]
     * // unmodifiable.add(4);   // This would throw UnsupportedOperationException
     * }</pre>
     *
     * @param <T> the type of input elements
     * @return a {@code Collector} which collects all input elements into an unmodifiable {@code Set}
     * @see java.util.stream.Collectors#toUnmodifiableSet
     */
    public static <T> Collector<T, ?, Set<T>> toUnmodifiableSet() {
        return java.util.stream.Collectors.toUnmodifiableSet();
    }

    /**
     * Returns a {@code Collector} that accumulates the input elements into a new {@code Queue}.
     * 
     * <p>This collector accumulates elements into a {@code Queue} implementation. The specific
     * implementation of {@code Queue} returned is not specified. Elements are added to the
     * queue in encounter order, making this suitable for FIFO (First-In-First-Out) processing.</p>
     * 
     * <p>The returned queue supports all optional {@code Queue} operations. There are no
     * guarantees on the thread-safety of the {@code Queue} returned.</p>
     *
     * <p><b>Null Handling:</b> This collector accepts null elements as the underlying
     * {@code Queue} implementation supports null values.</p>
     *
     * <p><b>Parallel Stream Support:</b> This collector supports parallel streams and
     * efficiently combines partial results.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect elements into a queue for FIFO processing
     * Queue<String> taskQueue = Stream.of("task1", "task2", "task3")
     *     .collect(Collectors.toQueue());
     * // Process: taskQueue.poll() returns "task1", then "task2", then "task3"
     * }</pre>
     *
     * @param <T> the type of input elements
     * @return a {@code Collector} which collects all input elements into a {@code Queue}
     */
    public static <T> Collector<T, ?, Queue<T>> toQueue() {
        final Supplier<Queue<T>> supplier = Suppliers.ofQueue();

        return toCollection(supplier);
    }

    /**
     * Returns a {@code Collector} that accumulates the input elements into a new {@code Deque}.
     * 
     * <p>This collector accumulates elements into a {@code Deque} (double-ended queue)
     * implementation. Elements can be efficiently added or removed from both ends of the
     * deque. The specific implementation of {@code Deque} returned is not specified.</p>
     * 
     * <p>Elements are added to the deque in encounter order. The deque can be used for
     * both FIFO and LIFO (Last-In-First-Out) operations.</p>
     *
     * <p><b>Null Handling:</b> This collector accepts null elements as the underlying
     * {@code Deque} implementation supports null values.</p>
     *
     * <p><b>Parallel Stream Support:</b> This collector supports parallel streams and
     * efficiently combines partial results.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect elements into a deque for flexible processing
     * Deque<Integer> deque = Stream.of(1, 2, 3, 4)
     *     .collect(Collectors.toDeque());
     * // Can use: deque.addFirst(), deque.removeLast(), etc.
     * }</pre>
     *
     * @param <T> the type of input elements
     * @return a {@code Collector} which collects all input elements into a {@code Deque}
     */
    public static <T> Collector<T, ?, Deque<T>> toDeque() {
        final Supplier<Deque<T>> supplier = Suppliers.ofDeque();

        return toCollection(supplier);
    }

    /**
     * Returns a {@code Collector} that accumulates the input elements into a new {@code List},
     * limiting the list to at most the specified number of elements.
     * 
     * <p>This collector is useful when you want to collect only a limited number of elements
     * into a list. Once the list reaches the specified size limit, subsequent elements are
     * ignored. This is more efficient than collecting all elements and then truncating.</p>
     * 
     * <p>The initial capacity of the list is optimized based on the {@code atMostSize}
     * parameter to avoid unnecessary resizing. Elements are collected in encounter order.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect at most 5 elements from a potentially large stream
     * List<Integer> topFive = Stream.iterate(1, n -> n + 1)
     *     .limit(1000)
     *     .collect(Collectors.toList(5));
     * // Result: [1, 2, 3, 4, 5]
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param atMostSize the maximum number of elements to collect
     * @return a {@code Collector} which collects at most the specified number of input
     *         elements into a {@code List}, in encounter order
     */
    public static <T> Collector<T, ?, List<T>> toList(final int atMostSize) {
        final Supplier<List<T>> supplier = () -> new ArrayList<>(N.min(256, atMostSize));

        return toCollection(atMostSize, supplier);
    }

    /**
     * Returns a {@code Collector} that accumulates the input elements into a new {@code Set},
     * limiting the set to at most the specified number of elements.
     * 
     * <p>This collector is useful when you want to collect only a limited number of unique
     * elements into a set. Once the set reaches the specified size limit, subsequent elements
     * are ignored. Duplicates are automatically handled by the set semantics.</p>
     * 
     * <p>Note that due to the nature of sets removing duplicates, the final size may be
     * less than {@code atMostSize} if the stream contains duplicate elements.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect at most 3 unique elements
     * Set<Integer> limitedSet = Stream.of(1, 2, 2, 3, 4, 5)
     *     .collect(Collectors.toSet(3));
     * // Result might be: [1, 2, 3] (order not guaranteed)
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param atMostSize the maximum number of elements to collect
     * @return a {@code Collector} which collects at most the specified number of unique
     *         input elements into a {@code Set}
     */
    public static <T> Collector<T, ?, Set<T>> toSet(final int atMostSize) {
        final Supplier<Set<T>> supplier = () -> N.newHashSet(atMostSize);

        return toCollection(atMostSize, supplier);
    }

    /**
     * Returns a {@code Collector} that accumulates the input elements into a new {@code Multiset}.
     * 
     * <p>A {@code Multiset} is a collection that allows duplicate elements and keeps track
     * of the count of each element. Unlike a regular set, the same element can appear
     * multiple times, and the multiset maintains the count of occurrences.</p>
     * 
     * <p>This collector is useful for frequency counting and when you need to know not just
     * which elements are present, but how many times each appears.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Count frequency of elements
     * Multiset<String> wordCounts = Stream.of("apple", "banana", "apple", "apple", "banana")
     *     .collect(Collectors.toMultiset());
     * // wordCounts.count("apple") returns 3
     * // wordCounts.count("banana") returns 2
     * }</pre>
     *
     * @param <T> the type of input elements
     * @return a {@code Collector} which collects all input elements into a {@code Multiset},
     *         maintaining counts of duplicate elements
     */
    public static <T> Collector<T, ?, Multiset<T>> toMultiset() {
        final Supplier<Multiset<T>> supplier = Suppliers.ofMultiset();

        return toMultiset(supplier);
    }

    /**
     * Returns a {@code Collector} that accumulates the input elements into a {@code Multiset}
     * created by the provided factory function.
     * 
     * <p>This collector allows you to specify the exact type of {@code Multiset} implementation
     * to use. The multiset will maintain counts of duplicate elements as they are collected.</p>
     * 
     * <p>The collector handles merging of multisets correctly in parallel operations,
     * combining the counts of elements from different segments of the stream.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Use a custom Multiset implementation
     * Multiset<Integer> counts = Stream.of(1, 2, 2, 3, 3, 3)
     *     .collect(Collectors.toMultiset(HashMultiset::new));
     * // counts.count(3) returns 3
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param supplier a supplier providing a new empty {@code Multiset} into which
     *                 the results will be inserted
     * @return a {@code Collector} which collects all input elements into a {@code Multiset}
     */
    @SuppressWarnings("rawtypes")
    public static <T> Collector<T, ?, Multiset<T>> toMultiset(final Supplier<Multiset<T>> supplier) {
        final BiConsumer<Multiset<T>, T> accumulator = (BiConsumer) Multiset_Accumulator;
        final BinaryOperator<Multiset<T>> combiner = (BinaryOperator) Multiset_Combiner;

        return create(supplier, accumulator, combiner, CH_UNORDERED_ID);
    }

    /**
     * Returns a {@code Collector} that accumulates the input elements into a new {@code Object} array.
     * 
     * <p>This collector collects all stream elements into an {@code Object[]} array. Elements
     * are stored in encounter order. This is useful when you need the result as an array
     * rather than a collection.</p>
     * 
     * <p>The collector first accumulates elements into a list for efficiency, then converts
     * to an array as the final step.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect stream elements to an Object array
     * Object[] array = Stream.of("a", "b", "c")
     *     .collect(Collectors.toArray());
     * // Result: ["a", "b", "c"] as Object[]
     * }</pre>
     *
     * @param <T> the type of input elements
     * @return a {@code Collector} which collects all input elements into an {@code Object[]},
     *         in encounter order
     */
    public static <T> Collector<T, ?, Object[]> toArray() {
        return toArray(Suppliers.ofEmptyObjectArray());
    }

    /**
     * Returns a {@code Collector} that accumulates the input elements into an array
     * created by the provided array supplier.
     * 
     * <p>This collector allows you to specify the exact type of array to create. The array
     * supplier should provide an array of the desired type and size. If the supplied array
     * is large enough to hold all elements, it will be used; otherwise, a new array of the
     * same type will be created.</p>
     * 
     * <p>Elements are collected in encounter order. This method uses reflection to create
     * arrays of the appropriate type when necessary.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect to a String array
     * String[] strings = Stream.of("a", "b", "c")
     *     .collect(Collectors.toArray(() -> new String[0]));
     * // Result: ["a", "b", "c"] as String[]
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <A> the component type of the array
     * @param arraySupplier a supplier providing an array of the desired type
     * @return a {@code Collector} which collects all input elements into an array
     */
    public static <T, A> Collector<T, ?, A[]> toArray(final Supplier<A[]> arraySupplier) {
        final Supplier<List<A>> supplier = Suppliers.ofList();
        @SuppressWarnings("rawtypes")
        final BiConsumer<List<A>, T> accumulator = (BiConsumer) BiConsumers.ofAdd();
        final BinaryOperator<List<A>> combiner = BinaryOperators.ofAddAllToBigger();
        final Function<List<A>, A[]> finisher = t -> {
            final A[] a = arraySupplier.get();

            if (a.length >= t.size()) {
                return t.toArray(a);
            } else {
                return t.toArray((A[]) Array.newInstance(a.getClass().getComponentType(), t.size()));
            }
        };

        return create(supplier, accumulator, combiner, finisher, CH_NOID);
    }

    /**
     * Returns a {@code Collector} that accumulates the input elements into an array
     * created by the provided array factory function.
     * 
     * <p>This collector allows you to create an array of the exact size needed. The
     * {@code IntFunction} receives the size of the collection and should return an
     * array of that size. This is more efficient than the supplier version as it
     * creates an array of the exact size needed.</p>
     * 
     * <p>Elements are collected in encounter order. This is the preferred method when
     * you know the component type of the array at compile time.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect to an Integer array with exact sizing
     * Integer[] numbers = Stream.of(1, 2, 3, 4, 5)
     *     .collect(Collectors.toArray(Integer[]::new));
     * // Result: [1, 2, 3, 4, 5] as Integer[]
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <A> the component type of the array
     * @param arraySupplier a function which produces a new array of the desired
     *                      type and the provided length
     * @return a {@code Collector} which collects all input elements into an array
     */
    public static <T, A> Collector<T, ?, A[]> toArray(final IntFunction<A[]> arraySupplier) {
        final Supplier<List<A>> supplier = Suppliers.ofList();
        @SuppressWarnings("rawtypes")
        final BiConsumer<List<A>, T> accumulator = (BiConsumer) BiConsumers.ofAdd();
        final BinaryOperator<List<A>> combiner = BinaryOperators.ofAddAllToBigger();
        final Function<List<A>, A[]> finisher = t -> t.toArray(arraySupplier.apply(t.size()));

        return create(supplier, accumulator, combiner, finisher, CH_NOID);
    }

    /**
     * Returns a {@code Collector} that accumulates {@code Boolean} elements into a {@code BooleanList}.
     * 
     * <p>This collector is optimized for primitive boolean values, storing them in a
     * specialized list implementation that avoids boxing overhead. The {@code BooleanList}
     * provides efficient storage and access for boolean values.</p>
     * 
     * <p>Elements are collected in encounter order. The collector handles both combining
     * partial results in parallel streams and converting to the final {@code BooleanList}.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect boolean values efficiently
     * BooleanList flags = Stream.of(true, false, true, true, false)
     *     .collect(Collectors.toBooleanList());
     * // Result: [true, false, true, true, false] in BooleanList
     * }</pre>
     *
     * @return a {@code Collector} which collects {@code Boolean} elements into a {@code BooleanList}
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    public static Collector<Boolean, ?, BooleanList> toBooleanList() {
        final Supplier<BooleanList> supplier = Suppliers.ofBooleanList();
        final BiConsumer<BooleanList, Boolean> accumulator = BooleanList_Accumulator;
        final BinaryOperator<BooleanList> combiner = BooleanList_Combiner;

        return create(supplier, accumulator, combiner, CH_ID);
    }

    /**
     * Returns a {@code Collector} that accumulates {@code Boolean} elements into a primitive {@code boolean} array.
     * 
     * <p>This collector efficiently converts a stream of {@code Boolean} objects into a
     * primitive {@code boolean[]} array, avoiding boxing overhead in the final result.
     * Elements are collected in encounter order.</p>
     * 
     * <p>The collector internally uses a {@code BooleanList} for accumulation, then converts
     * to an array as the final step, ensuring efficient memory usage throughout the process.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert Boolean stream to primitive array
     * boolean[] array = Stream.of(true, false, true)
     *     .collect(Collectors.toBooleanArray());
     * // Result: [true, false, true] as boolean[]
     * }</pre>
     *
     * @return a {@code Collector} which collects {@code Boolean} elements into a {@code boolean[]}
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    public static Collector<Boolean, ?, boolean[]> toBooleanArray() {
        final Supplier<BooleanList> supplier = Suppliers.ofBooleanList();
        final BiConsumer<BooleanList, Boolean> accumulator = BooleanList_Accumulator;
        final BinaryOperator<BooleanList> combiner = BooleanList_Combiner;
        final Function<BooleanList, boolean[]> finisher = BooleanArray_Finisher;

        return create(supplier, accumulator, combiner, finisher, CH_NOID);
    }

    /**
     * Returns a {@code Collector} that accumulates {@code Character} elements into a {@code CharList}.
     * 
     * <p>This collector is optimized for primitive char values, storing them in a
     * specialized list implementation that avoids boxing overhead. The {@code CharList}
     * provides efficient storage and access for character values.</p>
     * 
     * <p>Elements are collected in encounter order. This is particularly useful when
     * working with character data that needs to be processed as a list.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect characters efficiently
     * CharList chars = Stream.of('a', 'b', 'c', 'd')
     *     .collect(Collectors.toCharList());
     * // Result: ['a', 'b', 'c', 'd'] in CharList
     * }</pre>
     *
     * @return a {@code Collector} which collects {@code Character} elements into a {@code CharList}
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    public static Collector<Character, ?, CharList> toCharList() {
        final Supplier<CharList> supplier = Suppliers.ofCharList();
        final BiConsumer<CharList, Character> accumulator = CharList_Accumulator;
        final BinaryOperator<CharList> combiner = CharList_Combiner;

        return create(supplier, accumulator, combiner, CH_ID);
    }

    /**
     * Returns a {@code Collector} that accumulates {@code Character} elements into a primitive {@code char} array.
     * 
     * <p>This collector efficiently converts a stream of {@code Character} objects into a
     * primitive {@code char[]} array, avoiding boxing overhead in the final result.
     * This is useful for text processing operations that need character data as an array.</p>
     * 
     * <p>Elements are collected in encounter order. The collector internally uses a
     * {@code CharList} for efficient accumulation.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert Character stream to primitive array
     * char[] array = Stream.of('H', 'e', 'l', 'l', 'o')
     *     .collect(Collectors.toCharArray());
     * // Result: ['H', 'e', 'l', 'l', 'o'] as char[]
     * }</pre>
     *
     * @return a {@code Collector} which collects {@code Character} elements into a {@code char[]}
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    public static Collector<Character, ?, char[]> toCharArray() {
        final Supplier<CharList> supplier = Suppliers.ofCharList();
        final BiConsumer<CharList, Character> accumulator = CharList_Accumulator;
        final BinaryOperator<CharList> combiner = CharList_Combiner;
        final Function<CharList, char[]> finisher = CharArray_Finisher;

        return create(supplier, accumulator, combiner, finisher, CH_NOID);
    }

    /**
     * Returns a {@code Collector} that accumulates {@code Byte} elements into a {@code ByteList}.
     * 
     * <p>This collector is optimized for primitive byte values, storing them in a
     * specialized list implementation that avoids boxing overhead. The {@code ByteList}
     * provides efficient storage and access for byte values.</p>
     * 
     * <p>Elements are collected in encounter order. This is particularly useful when
     * working with binary data or byte-oriented operations.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect bytes efficiently
     * ByteList bytes = Stream.of((byte)1, (byte)2, (byte)3)
     *     .collect(Collectors.toByteList());
     * // Result: [1, 2, 3] in ByteList
     * }</pre>
     *
     * @return a {@code Collector} which collects {@code Byte} elements into a {@code ByteList}
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    public static Collector<Byte, ?, ByteList> toByteList() {
        final Supplier<ByteList> supplier = Suppliers.ofByteList();
        final BiConsumer<ByteList, Byte> accumulator = ByteList_Accumulator;
        final BinaryOperator<ByteList> combiner = ByteList_Combiner;

        return create(supplier, accumulator, combiner, CH_ID);
    }

    /**
     * Returns a {@code Collector} that accumulates {@code Byte} elements into a primitive {@code byte} array.
     * 
     * <p>This collector efficiently converts a stream of {@code Byte} objects into a
     * primitive {@code byte[]} array, avoiding boxing overhead in the final result.
     * This is essential for binary data processing and I/O operations.</p>
     * 
     * <p>Elements are collected in encounter order. The collector internally uses a
     * {@code ByteList} for efficient accumulation.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert Byte stream to primitive array
     * byte[] array = Stream.of((byte)10, (byte)20, (byte)30)
     *     .collect(Collectors.toByteArray());
     * // Result: [10, 20, 30] as byte[]
     * }</pre>
     *
     * @return a {@code Collector} which collects {@code Byte} elements into a {@code byte[]}
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    public static Collector<Byte, ?, byte[]> toByteArray() {
        final Supplier<ByteList> supplier = Suppliers.ofByteList();
        final BiConsumer<ByteList, Byte> accumulator = ByteList_Accumulator;
        final BinaryOperator<ByteList> combiner = ByteList_Combiner;
        final Function<ByteList, byte[]> finisher = ByteArray_Finisher;

        return create(supplier, accumulator, combiner, finisher, CH_NOID);
    }

    /**
     * Returns a {@code Collector} that accumulates {@code Short} elements into a {@code ShortList}.
     * 
     * <p>This collector is optimized for primitive short values, storing them in a
     * specialized list implementation that avoids boxing overhead. The {@code ShortList}
     * provides efficient storage and access for short values.</p>
     * 
     * <p>Elements are collected in encounter order. This is useful when working with
     * numeric data that fits within the short range (-32,768 to 32,767).</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect shorts efficiently
     * ShortList shorts = Stream.of((short)100, (short)200, (short)300)
     *     .collect(Collectors.toShortList());
     * // Result: [100, 200, 300] in ShortList
     * }</pre>
     *
     * @return a {@code Collector} which collects {@code Short} elements into a {@code ShortList}
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    public static Collector<Short, ?, ShortList> toShortList() {
        final Supplier<ShortList> supplier = Suppliers.ofShortList();
        final BiConsumer<ShortList, Short> accumulator = ShortList_Accumulator;
        final BinaryOperator<ShortList> combiner = ShortList_Combiner;

        return create(supplier, accumulator, combiner, CH_ID);
    }

    /**
     * Returns a {@code Collector} that accumulates {@code Short} elements into a primitive {@code short} array.
     * 
     * <p>This collector efficiently converts a stream of {@code Short} objects into a
     * primitive {@code short[]} array, avoiding boxing overhead in the final result.
     * Elements are collected in encounter order.</p>
     * 
     * <p>The collector internally uses a {@code ShortList} for efficient accumulation
     * before converting to the final array format.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert Short stream to primitive array
     * short[] array = Stream.of((short)1, (short)2, (short)3)
     *     .collect(Collectors.toShortArray());
     * // Result: [1, 2, 3] as short[]
     * }</pre>
     *
     * @return a {@code Collector} which collects {@code Short} elements into a {@code short[]}
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    public static Collector<Short, ?, short[]> toShortArray() {
        final Supplier<ShortList> supplier = Suppliers.ofShortList();
        final BiConsumer<ShortList, Short> accumulator = ShortList_Accumulator;
        final BinaryOperator<ShortList> combiner = ShortList_Combiner;
        final Function<ShortList, short[]> finisher = ShortArray_Finisher;

        return create(supplier, accumulator, combiner, finisher, CH_NOID);
    }

    /**
     * Returns a {@code Collector} that accumulates {@code Integer} elements into an {@code IntList}.
     * 
     * <p>This collector is optimized for primitive int values, storing them in a
     * specialized list implementation that avoids boxing overhead. The {@code IntList}
     * provides efficient storage and access for integer values.</p>
     * 
     * <p>Elements are collected in encounter order. This is one of the most commonly
     * used primitive collectors for numeric operations.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect integers efficiently
     * IntList ints = Stream.of(1, 2, 3, 4, 5)
     *     .collect(Collectors.toIntList());
     * // Result: [1, 2, 3, 4, 5] in IntList
     * }</pre>
     *
     * @return a {@code Collector} which collects {@code Integer} elements into an {@code IntList}
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    public static Collector<Integer, ?, IntList> toIntList() {
        final Supplier<IntList> supplier = Suppliers.ofIntList();
        final BiConsumer<IntList, Integer> accumulator = IntList_Accumulator;
        final BinaryOperator<IntList> combiner = IntList_Combiner;

        return create(supplier, accumulator, combiner, CH_ID);
    }

    /**
     * Returns a {@code Collector} that accumulates {@code Integer} elements into a primitive {@code int} array.
     * 
     * <p>This collector efficiently converts a stream of {@code Integer} objects into a
     * primitive {@code int[]} array, avoiding boxing overhead in the final result.
     * This is essential for performance-critical numeric computations.</p>
     * 
     * <p>Elements are collected in encounter order. The collector internally uses an
     * {@code IntList} for efficient accumulation.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert Integer stream to primitive array
     * int[] array = Stream.of(10, 20, 30, 40, 50)
     *     .collect(Collectors.toIntArray());
     * // Result: [10, 20, 30, 40, 50] as int[]
     * }</pre>
     *
     * @return a {@code Collector} which collects {@code Integer} elements into an {@code int[]}
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    public static Collector<Integer, ?, int[]> toIntArray() {
        final Supplier<IntList> supplier = Suppliers.ofIntList();
        final BiConsumer<IntList, Integer> accumulator = IntList_Accumulator;
        final BinaryOperator<IntList> combiner = IntList_Combiner;
        final Function<IntList, int[]> finisher = IntArray_Finisher;

        return create(supplier, accumulator, combiner, finisher, CH_NOID);
    }

    /**
     * Returns a {@code Collector} that accumulates {@code Long} elements into a {@code LongList}.
     * 
     * <p>This collector is optimized for primitive long values, storing them in a
     * specialized list implementation that avoids boxing overhead. The {@code LongList}
     * provides efficient storage and access for long values.</p>
     * 
     * <p>Elements are collected in encounter order. This is useful for working with
     * large numeric values or timestamps.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect longs efficiently
     * LongList longs = Stream.of(1000L, 2000L, 3000L)
     *     .collect(Collectors.toLongList());
     * // Result: [1000, 2000, 3000] in LongList
     * }</pre>
     *
     * @return a {@code Collector} which collects {@code Long} elements into a {@code LongList}
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    public static Collector<Long, ?, LongList> toLongList() {
        final Supplier<LongList> supplier = Suppliers.ofLongList();
        final BiConsumer<LongList, Long> accumulator = LongList_Accumulator;
        final BinaryOperator<LongList> combiner = LongList_Combiner;

        return create(supplier, accumulator, combiner, CH_ID);
    }

    /**
     * Returns a {@code Collector} that accumulates {@code Long} elements into a primitive {@code long} array.
     * 
     * <p>This collector efficiently converts a stream of {@code Long} objects into a
     * primitive {@code long[]} array, avoiding boxing overhead in the final result.
     * This is important for memory efficiency when working with large datasets.</p>
     * 
     * <p>Elements are collected in encounter order. The collector internally uses a
     * {@code LongList} for efficient accumulation.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert Long stream to primitive array
     * long[] array = Stream.of(100L, 200L, 300L)
     *     .collect(Collectors.toLongArray());
     * // Result: [100, 200, 300] as long[]
     * }</pre>
     *
     * @return a {@code Collector} which collects {@code Long} elements into a {@code long[]}
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    public static Collector<Long, ?, long[]> toLongArray() {
        final Supplier<LongList> supplier = Suppliers.ofLongList();
        final BiConsumer<LongList, Long> accumulator = LongList_Accumulator;
        final BinaryOperator<LongList> combiner = LongList_Combiner;
        final Function<LongList, long[]> finisher = LongArray_Finisher;

        return create(supplier, accumulator, combiner, finisher, CH_NOID);
    }

    /**
     * Returns a {@code Collector} that accumulates {@code Float} elements into a {@code FloatList}.
     * 
     * <p>This collector is optimized for primitive float values, storing them in a
     * specialized list implementation that avoids boxing overhead. The {@code FloatList}
     * provides efficient storage and access for floating-point values.</p>
     * 
     * <p>Elements are collected in encounter order. This is useful for scientific
     * calculations and graphics programming where float precision is sufficient.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect floats efficiently
     * FloatList floats = Stream.of(1.5f, 2.5f, 3.5f)
     *     .collect(Collectors.toFloatList());
     * // Result: [1.5, 2.5, 3.5] in FloatList
     * }</pre>
     *
     * @return a {@code Collector} which collects {@code Float} elements into a {@code FloatList}
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    public static Collector<Float, ?, FloatList> toFloatList() {
        final Supplier<FloatList> supplier = Suppliers.ofFloatList();
        final BiConsumer<FloatList, Float> accumulator = FloatList_Accumulator;
        final BinaryOperator<FloatList> combiner = FloatList_Combiner;

        return create(supplier, accumulator, combiner, CH_ID);
    }

    /**
     * Returns a {@code Collector} that accumulates {@code Float} elements into a primitive {@code float} array.
     * 
     * <p>This collector efficiently converts a stream of {@code Float} objects into a
     * primitive {@code float[]} array, avoiding boxing overhead in the final result.
     * This is essential for performance in graphics and scientific applications.</p>
     * 
     * <p>Elements are collected in encounter order. The collector internally uses a
     * {@code FloatList} for efficient accumulation.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert Float stream to primitive array
     * float[] array = Stream.of(1.0f, 2.0f, 3.0f)
     *     .collect(Collectors.toFloatArray());
     * // Result: [1.0, 2.0, 3.0] as float[]
     * }</pre>
     *
     * @return a {@code Collector} which collects {@code Float} elements into a {@code float[]}
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    public static Collector<Float, ?, float[]> toFloatArray() {
        final Supplier<FloatList> supplier = Suppliers.ofFloatList();
        final BiConsumer<FloatList, Float> accumulator = FloatList_Accumulator;
        final BinaryOperator<FloatList> combiner = FloatList_Combiner;
        final Function<FloatList, float[]> finisher = FloatArray_Finisher;

        return create(supplier, accumulator, combiner, finisher, CH_NOID);
    }

    /**
     * Returns a {@code Collector} that accumulates {@code Double} elements into a {@code DoubleList}.
     * 
     * <p>This collector is optimized for primitive double values, storing them in a
     * specialized list implementation that avoids boxing overhead. The {@code DoubleList}
     * provides efficient storage and access for double-precision floating-point values.</p>
     * 
     * <p>Elements are collected in encounter order. This is useful for high-precision
     * numeric calculations where double precision is required.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect doubles efficiently
     * DoubleList doubles = Stream.of(1.5, 2.5, 3.5)
     *     .collect(Collectors.toDoubleList());
     * // Result: [1.5, 2.5, 3.5] in DoubleList
     * }</pre>
     *
     * @return a {@code Collector} which collects {@code Double} elements into a {@code DoubleList}
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    public static Collector<Double, ?, DoubleList> toDoubleList() {
        final Supplier<DoubleList> supplier = Suppliers.ofDoubleList();
        final BiConsumer<DoubleList, Double> accumulator = DoubleList_Accumulator;
        final BinaryOperator<DoubleList> combiner = DoubleList_Combiner;

        return create(supplier, accumulator, combiner, CH_ID);
    }

    /**
     * Returns a {@code Collector} that accumulates {@code Double} elements into a primitive {@code double} array.
     * 
     * <p>This collector efficiently converts a stream of {@code Double} objects into a
     * primitive {@code double[]} array, avoiding boxing overhead in the final result.
     * This is essential for performance in scientific and financial calculations.</p>
     * 
     * <p>Elements are collected in encounter order. The collector internally uses a
     * {@code DoubleList} for efficient accumulation.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert Double stream to primitive array
     * double[] array = Stream.of(1.1, 2.2, 3.3)
     *     .collect(Collectors.toDoubleArray());
     * // Result: [1.1, 2.2, 3.3] as double[]
     * }</pre>
     *
     * @return a {@code Collector} which collects {@code Double} elements into a {@code double[]}
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    public static Collector<Double, ?, double[]> toDoubleArray() {
        final Supplier<DoubleList> supplier = Suppliers.ofDoubleList();
        final BiConsumer<DoubleList, Double> accumulator = DoubleList_Accumulator;
        final BinaryOperator<DoubleList> combiner = DoubleList_Combiner;
        final Function<DoubleList, double[]> finisher = DoubleArray_Finisher;

        return create(supplier, accumulator, combiner, finisher, CH_NOID);
    }

    private static final Supplier<Holder<Optional<Object>>> onlyOne_supplier = () -> Holder.of(Optional.empty());

    private static final BiConsumer<Holder<Optional<Object>>, Object> onlyOne_accumulator = (holder, val) -> {
        if (holder.value().isPresent()) {
            throw new TooManyElementsException("Duplicate values");
        }

        holder.setValue(Optional.of(val));
    };

    private static final BinaryOperator<Holder<Optional<Object>>> onlyOne_combiner = (t, u) -> {
        if (t.value().isPresent() && u.value().isPresent()) {
            throw new TooManyElementsException("Duplicate values");
        }

        return t.value().isPresent() ? t : u;
    };

    private static final Function<Holder<Optional<Object>>, Optional<Object>> onlyOne_finisher = Holder::value;

    /**
     * Returns a {@code Collector} that expects exactly one element and returns it wrapped
     * in an {@code Optional}.
     * 
     * <p>This collector throws a {@code TooManyElementsException} if more than one element
     * is encountered in the stream. It returns an empty {@code Optional} if the stream
     * is empty. This is useful for operations where you expect at most one result.</p>
     * 
     * <p>This collector enforces uniqueness and is suitable for queries that should return
     * a single result or no result at all.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find the only element matching a condition
     * Optional<String> result = list.stream()
     *     .filter(s -> s.startsWith("unique"))
     *     .collect(Collectors.onlyOne());
     * // Throws TooManyElementsException if multiple matches found
     * }</pre>
     *
     * @param <T> the type of input elements
     * @return a {@code Collector} which collects the single element into an {@code Optional}
     * @throws TooManyElementsException if more than one element is collected
     */
    @SuppressWarnings("rawtypes")
    public static <T> Collector<T, ?, Optional<T>> onlyOne() {
        final Supplier<Holder<Optional<T>>> supplier = (Supplier) onlyOne_supplier;
        final BiConsumer<Holder<Optional<T>>, T> accumulator = (BiConsumer) onlyOne_accumulator;
        final BinaryOperator<Holder<Optional<T>>> combiner = (BinaryOperator) onlyOne_combiner;
        final Function<Holder<Optional<T>>, Optional<T>> finisher = (Function) onlyOne_finisher;

        return create(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    /**
     * Returns a {@code Collector} that expects exactly one element matching the given
     * predicate and returns it wrapped in an {@code Optional}.
     * 
     * <p>This collector filters elements using the provided predicate, then expects
     * exactly one element to pass the filter. It throws a {@code TooManyElementsException}
     * if more than one element matches the predicate. It returns an empty {@code Optional}
     * if no elements match.</p>
     * 
     * <p>This is a combination of filtering and the onlyOne collector, useful for
     * finding a unique element matching specific criteria.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find the only even number in a specific range
     * Optional<Integer> onlyEven = Stream.of(1, 3, 4, 5, 7)
     *     .collect(Collectors.onlyOne(n -> n % 2 == 0));
     * // Result: Optional[4]
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param predicate a predicate to apply to elements
     * @return a {@code Collector} which collects the single matching element into an {@code Optional}
     * @throws TooManyElementsException if more than one element matches the predicate
     */
    public static <T> Collector<T, ?, Optional<T>> onlyOne(final Predicate<? super T> predicate) {
        final Collector<T, ?, Optional<T>> downstream = onlyOne();

        return filtering(predicate, downstream);
    }

    private static final Supplier<Holder<Object>> first_last_supplier = () -> Holder.of(NONE);

    private static final BiConsumer<Holder<Object>, Object> first_accumulator = (holder, val) -> {
        if (holder.value() == NONE) {
            holder.setValue(val);
        }
    };

    private static final BiConsumer<Holder<Object>, Object> last_accumulator = Holder::setValue;

    private static final BinaryOperator<Holder<Object>> first_last_combiner = (t, u) -> {
        if (t.value() != NONE && u.value() != NONE) {
            throw new UnsupportedOperationException("The 'first' and 'last' Collector only can be used in sequential stream");   //NOSONAR
        }

        return t.value() != NONE ? t : u;
    };

    private static final Function<Holder<Object>, Optional<Object>> first_last_finisher = t -> t.value() == NONE ? Optional.empty() : Optional.of(t.value());

    /**
     * Returns a {@code Collector} that collects the first element of a sequential stream
     * into an {@code Optional}.
     * 
     * <p>This collector captures the first element encountered in the stream. If the stream
     * is empty, it returns an empty {@code Optional}. This collector is designed for
     * sequential streams only and will throw an {@code UnsupportedOperationException}
     * if used with parallel streams.</p>
     * 
     * <p>Note that this is different from {@code findFirst()} as it's implemented as a
     * collector and can be combined with other collectors.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get the first element as a collector
     * Optional<String> first = Stream.of("a", "b", "c")
     *     .collect(Collectors.first());
     * // Result: Optional["a"]
     * }</pre>
     *
     * @param <T> the type of input elements
     * @return a {@code Collector} which collects the first element into an {@code Optional}
     * @throws UnsupportedOperationException if used with parallel streams
     */
    @SuppressWarnings("rawtypes")
    public static <T> Collector<T, ?, Optional<T>> first() {
        final Supplier<Holder<T>> supplier = (Supplier) first_last_supplier;
        final BiConsumer<Holder<T>, T> accumulator = (BiConsumer) first_accumulator;
        final BinaryOperator<Holder<T>> combiner = (BinaryOperator) first_last_combiner;
        final Function<Holder<T>, Optional<T>> finisher = (Function) first_last_finisher;

        return create(supplier, accumulator, combiner, finisher, CH_NOID);
    }

    /**
     * Returns a {@code Collector} that collects the last element of a sequential stream
     * into an {@code Optional}.
     * 
     * <p>This collector captures the last element encountered in the stream. If the stream
     * is empty, it returns an empty {@code Optional}. This collector is designed for
     * sequential streams only and will throw an {@code UnsupportedOperationException}
     * if used with parallel streams.</p>
     * 
     * <p>Each element encountered replaces the previous one, so the final result is
     * the last element in encounter order.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get the last element as a collector
     * Optional<String> last = Stream.of("a", "b", "c")
     *     .collect(Collectors.last());
     * // Result: Optional["c"]
     * }</pre>
     *
     * @param <T> the type of input elements
     * @return a {@code Collector} which collects the last element into an {@code Optional}
     * @throws UnsupportedOperationException if used with parallel streams
     */
    @SuppressWarnings("rawtypes")
    public static <T> Collector<T, ?, Optional<T>> last() {
        final Supplier<Holder<T>> supplier = (Supplier) first_last_supplier;
        final BiConsumer<Holder<T>, T> accumulator = (BiConsumer) last_accumulator;
        final BinaryOperator<Holder<T>> combiner = (BinaryOperator) first_last_combiner;
        final Function<Holder<T>, Optional<T>> finisher = (Function) first_last_finisher;

        return create(supplier, accumulator, combiner, finisher, CH_NOID);
    }

    /**
     * Returns a {@code Collector} that collects the first n elements of a sequential stream
     * into a {@code List}.
     * 
     * <p>This collector captures up to the first n elements encountered in the stream.
     * If the stream contains fewer than n elements, all elements are collected. This
     * collector is designed for sequential streams only and will throw an
     * {@code UnsupportedOperationException} if used with parallel streams.</p>
     * 
     * <p>The collector stops accumulating once n elements have been collected, making
     * it efficient for large streams when you only need the first few elements.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get the first 3 elements
     * List<Integer> firstThree = Stream.of(1, 2, 3, 4, 5)
     *     .collect(Collectors.first(3));
     * // Result: [1, 2, 3]
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param n the maximum number of elements to collect
     * @return a {@code Collector} which collects the first n elements into a {@code List}
     * @throws IllegalArgumentException if n is negative
     * @throws UnsupportedOperationException if used with parallel streams
     */
    public static <T> Collector<T, ?, List<T>> first(final int n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        final Supplier<List<T>> supplier = () -> new ArrayList<>(N.min(256, n));

        final BiConsumer<List<T>, T> accumulator = (c, t) -> {
            if (c.size() < n) {
                c.add(t);
            }
        };

        final BinaryOperator<List<T>> combiner = (a, b) -> {
            if (N.notEmpty(a) && N.notEmpty(b)) {
                throw new UnsupportedOperationException("The 'first' and 'last' Collector only can be used in sequential stream");
            }

            return a.size() > 0 ? a : b;
        };

        return create(supplier, accumulator, combiner, CH_ID);
    }

    /**
     * Returns a {@code Collector} that collects the last n elements of a sequential stream
     * into a {@code List}.
     * 
     * <p>This collector maintains a sliding window of the last n elements encountered.
     * It uses a {@code Deque} internally for efficient addition and removal of elements.
     * For small values of n (â‰¤ 1024), an {@code ArrayDeque} is used; otherwise, a
     * {@code LinkedList} is used.</p>
     * 
     * <p>This collector is designed for sequential streams only and will throw an
     * {@code UnsupportedOperationException} if used with parallel streams.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get the last 3 elements
     * List<Integer> lastThree = Stream.of(1, 2, 3, 4, 5)
     *     .collect(Collectors.last(3));
     * // Result: [3, 4, 5]
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param n the maximum number of elements to collect
     * @return a {@code Collector} which collects the last n elements into a {@code List}
     * @throws IllegalArgumentException if n is negative
     * @throws UnsupportedOperationException if used with parallel streams
     */
    public static <T> Collector<T, ?, List<T>> last(final int n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        final Supplier<Deque<T>> supplier = () -> n <= 1024 ? new ArrayDeque<>(n) : new LinkedList<>();

        final BiConsumer<Deque<T>, T> accumulator = (deque, t) -> {
            if (n > 0) {
                if (deque.size() >= n) {
                    deque.pollFirst();
                }

                deque.offerLast(t);
            }
        };

        final BinaryOperator<Deque<T>> combiner = (a, b) -> {
            if (N.notEmpty(a) && N.notEmpty(b)) {
                throw new UnsupportedOperationException("The 'first' and 'last' Collector only can be used in sequential stream");
            }

            while (b.size() < n && !a.isEmpty()) {
                b.addFirst(a.pollLast());
            }

            return b;
        };

        final Function<Deque<T>, List<T>> finisher = ArrayList::new;

        return create(supplier, accumulator, combiner, finisher, CH_NOID);
    }

    /**
     * Returns a {@code Collector} that concatenates the input elements into a {@code String}.
     * 
     * <p>This collector concatenates all elements directly without any delimiter between them.
     * It's equivalent to calling {@code joining("", "", "")}. The elements are converted
     * to strings using their {@code toString()} method.</p>
     * 
     * <p>This is useful for simple string concatenation where no formatting is needed.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Concatenate characters into a string
     * String result = Stream.of("H", "e", "l", "l", "o")
     *     .collect(Collectors.joining());
     * // Result: "Hello"
     * }</pre>
     *
     * @return a {@code Collector} which concatenates CharSequence elements into a {@code String}
     */
    public static Collector<Object, ?, String> joining() {
        return joining("", "", "");
    }

    /**
     * Returns a {@code Collector} that concatenates the input elements, separated by
     * the specified delimiter, into a {@code String}.
     * 
     * <p>This collector joins elements with the specified delimiter between each element.
     * No prefix or suffix is added to the result. The elements are converted to strings
     * using their {@code toString()} method.</p>
     * 
     * <p>This is one of the most commonly used string collectors for creating
     * comma-separated values, space-separated words, etc.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create comma-separated values
     * String csv = Stream.of("apple", "banana", "orange")
     *     .collect(Collectors.joining(", "));
     * // Result: "apple, banana, orange"
     * }</pre>
     *
     * @param delimiter the delimiter to be used between each element
     * @return a {@code Collector} which concatenates CharSequence elements, separated by the
     *         specified delimiter, into a {@code String}
     */
    public static Collector<Object, ?, String> joining(final CharSequence delimiter) {
        return joining(delimiter, "", "");
    }

    /**
     * Returns a {@code Collector} that concatenates the input elements, separated by the
     * specified delimiter, with the specified prefix and suffix, into a {@code String}.
     * 
     * <p>This collector provides full control over the string joining process. Elements
     * are joined with the delimiter between each element, and the entire result is
     * wrapped with the specified prefix and suffix.</p>
     * 
     * <p>The collector uses an efficient {@code Joiner} implementation that reuses
     * internal buffers for better performance. Elements are converted to strings using
     * their {@code toString()} method.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a formatted list
     * String list = Stream.of("apple", "banana", "orange")
     *     .collect(Collectors.joining(", ", "[", "]"));
     * // Result: "[apple, banana, orange]"
     * }</pre>
     *
     * @param delimiter the delimiter to be used between each element
     * @param prefix the sequence of characters to be used at the beginning
     * @param suffix the sequence of characters to be used at the end
     * @return a {@code Collector} which concatenates CharSequence elements, separated by the
     *         specified delimiter, into a {@code String} with the specified prefix and suffix
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    public static Collector<Object, ?, String> joining(final CharSequence delimiter, final CharSequence prefix, final CharSequence suffix) {
        @SuppressWarnings("resource")
        final Supplier<Joiner> supplier = () -> Joiner.with(delimiter, prefix, suffix).reuseCachedBuffer();

        final BiConsumer<Joiner, Object> accumulator = Joiner_Accumulator;
        final BinaryOperator<Joiner> combiner = Joiner_Combiner;
        final Function<Joiner, String> finisher = Joiner_Finisher;

        return create(supplier, accumulator, combiner, finisher, CH_NOID);
    }

    /**
     * Returns a {@code Collector} which passes only those elements to the specified
     * downstream collector which match the given predicate.
     * 
     * <p>This collector acts as a filter in the collection process. Only elements that
     * satisfy the predicate are passed to the downstream collector. This is useful for
     * conditional collection and can be more efficient than filtering the stream first.</p>
     * 
     * <p>If the downstream collector is short-circuiting, this method returns a
     * short-circuiting collector as well. This is similar to the JDK 9+ filtering
     * collector but with short-circuiting support.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect only even numbers to a list
     * List<Integer> evens = Stream.of(1, 2, 3, 4, 5, 6)
     *     .collect(Collectors.filtering(n -> n % 2 == 0, Collectors.toList()));
     * // Result: [2, 4, 6]
     * }</pre>
     *
     * @param <T> the type of the input elements
     * @param <A> intermediate accumulation type of the downstream collector
     * @param <R> result type of collector
     * @param predicate a filter function to be applied to the input elements
     * @param downstream a collector which will accept filtered values
     * @return a collector which applies the predicate to the input elements and provides
     *         the elements for which predicate returned {@code true} to the downstream collector
     * @see MoreCollectors#combine(Collector, Collector, BiFunction)
     */
    public static <T, A, R> Collector<T, ?, R> filtering(final Predicate<? super T> predicate, final Collector<? super T, A, R> downstream) {
        final BiConsumer<A, ? super T> downstreamAccumulator = downstream.accumulator();

        final BiConsumer<A, T> accumulator = (a, t) -> {
            if (predicate.test(t)) {
                downstreamAccumulator.accept(a, t);
            }
        };

        return create(downstream.supplier(), accumulator, downstream.combiner(), downstream.finisher(), downstream.characteristics());
    }

    /**
     * Returns a {@code Collector} which filters input elements by the supplied predicate,
     * collecting them to a {@code List}.
     * 
     * <p>This is a convenience method that combines filtering with collecting to a list.
     * It behaves like {@code filtering(predicate, Collectors.toList())}. Only elements
     * that satisfy the predicate are included in the resulting list.</p>
     * 
     * <p>There are no guarantees on the type, mutability, serializability, or
     * thread-safety of the {@code List} returned.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Filter and collect positive numbers
     * List<Integer> positives = Stream.of(-1, 2, -3, 4, -5, 6)
     *     .collect(Collectors.filteringToList(n -> n > 0));
     * // Result: [2, 4, 6]
     * }</pre>
     *
     * @param <T> the type of the input elements
     * @param predicate a filter function to be applied to the input elements
     * @return a collector which applies the predicate to the input elements and collects
     *         the elements for which predicate returned {@code true} to the {@code List}
     * @see #filtering(Predicate, Collector)
     */
    @Beta
    public static <T> Collector<T, ?, List<T>> filteringToList(final Predicate<? super T> predicate) {
        final Collector<? super T, ?, List<T>> downstream = Collectors.toList();

        return filtering(predicate, downstream);
    }

    /**
     * Returns a {@code Collector} that applies a mapping function to each input element
     * before accumulation by the downstream collector.
     * 
     * <p>This collector transforms each element using the provided mapper function before
     * passing it to the downstream collector. This is useful for transforming elements
     * during the collection process without modifying the original stream.</p>
     * 
     * <p>The mapping is done lazily during accumulation, which can be more efficient
     * than mapping the entire stream first.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect string lengths to a list
     * List<Integer> lengths = Stream.of("apple", "banana", "orange")
     *     .collect(Collectors.mapping(String::length, Collectors.toList()));
     * // Result: [5, 6, 6]
     * }</pre>
     *
     * @param <T> the type of the input elements
     * @param <U> type of elements accepted by the downstream collector
     * @param <A> intermediate accumulation type of the downstream collector
     * @param <R> result type of collector
     * @param mapper a function to be applied to the input elements
     * @param downstream a collector which will accept mapped values
     * @return a collector which applies the mapping function to the input elements and
     *         provides the mapped results to the downstream collector
     */
    public static <T, U, A, R> Collector<T, ?, R> mapping(final Function<? super T, ? extends U> mapper, final Collector<? super U, A, R> downstream) {
        final BiConsumer<A, ? super U> downstreamAccumulator = downstream.accumulator();

        final BiConsumer<A, T> accumulator = (a, t) -> downstreamAccumulator.accept(a, mapper.apply(t));

        return create(downstream.supplier(), accumulator, downstream.combiner(), downstream.finisher(), downstream.characteristics());
    }

    /**
     * Returns a {@code Collector} that applies a mapping function to each input element
     * and collects the results to a {@code List}.
     * 
     * <p>This is a convenience method that combines mapping with collecting to a list.
     * It behaves like {@code mapping(mapper, Collectors.toList())}. Each element is
     * transformed by the mapper function and the results are collected into a list.</p>
     * 
     * <p>There are no guarantees on the type, mutability, serializability, or
     * thread-safety of the {@code List} returned.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Extract and collect first characters
     * List<Character> firstChars = Stream.of("apple", "banana", "cherry")
     *     .collect(Collectors.mappingToList(s -> s.charAt(0)));
     * // Result: ['a', 'b', 'c']
     * }</pre>
     *
     * @param <T> the type of the input elements
     * @param <U> the type of the mapped elements
     * @param mapper a function to be applied to the input elements
     * @return a collector which applies the mapping function to the input elements and
     *         collects the mapped results to a {@code List}
     */
    @Beta
    public static <T, U> Collector<T, ?, List<U>> mappingToList(final Function<? super T, ? extends U> mapper) {
        return Collectors.mapping(mapper, Collectors.<U> toList());
    }

    /**
     * Returns a {@code Collector} that applies a flat mapping function to each input
     * element and accumulates the elements of the resulting streams.
     * 
     * <p>This collector is similar to {@code flatMap} but operates during the collection
     * phase. The mapper function returns a {@code Stream} for each input element, and
     * all elements from these streams are accumulated by the downstream collector.</p>
     * 
     * <p>The streams returned by the mapper function are automatically closed after
     * their elements have been consumed. This ensures proper resource management.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Flatten nested lists during collection
     * List<Integer> flattened = Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4))
     *     .collect(Collectors.flatMapping(list -> list.stream(), Collectors.toList()));
     * // Result: [1, 2, 3, 4]
     * }</pre>
     *
     * @param <T> the type of the input elements
     * @param <U> type of elements accepted by the downstream collector
     * @param <A> intermediate accumulation type of the downstream collector
     * @param <R> result type of collector
     * @param mapper a function to be applied to the input elements, which returns a stream
     *               of results
     * @param downstream a collector which will accept elements of the streams returned by mapper
     * @return a collector which applies the flat mapping function to the input elements and
     *         provides the elements of the resulting streams to the downstream collector
     */
    public static <T, U, A, R> Collector<T, ?, R> flatMapping(final Function<? super T, ? extends Stream<? extends U>> mapper,
            final Collector<? super U, A, R> downstream) {
        final BiConsumer<A, ? super U> downstreamAccumulator = downstream.accumulator();

        final BiConsumer<A, T> accumulator = (a, t) -> {
            try (Stream<? extends U> stream = mapper.apply(t)) {
                final ObjIterator<? extends U> iter = StreamBase.iterate(stream);

                while (iter.hasNext()) {
                    downstreamAccumulator.accept(a, iter.next());
                }
            }
        };

        return create(downstream.supplier(), accumulator, downstream.combiner(), downstream.finisher(), downstream.characteristics());
    }

    /**
     * Returns a {@code Collector} that applies a flat mapping function to each input
     * element and collects all resulting elements to a {@code List}.
     * 
     * <p>This is a convenience method that combines flat mapping with collecting to a list.
     * It behaves like {@code flatMapping(mapper, Collectors.toList())}. Each element is
     * transformed into a stream by the mapper function, and all elements from all streams
     * are collected into a single list.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Split strings into characters and collect
     * List<Character> chars = Stream.of("abc", "def")
     *     .collect(Collectors.flatMappingToList(
     *         s -> s.chars().mapToObj(c -> (char)c)));
     * // Result: ['a', 'b', 'c', 'd', 'e', 'f']
     * }</pre>
     *
     * @param <T> the type of the input elements
     * @param <U> the type of elements in the streams returned by mapper
     * @param mapper a function to be applied to the input elements, which returns a stream
     *               of results
     * @return a collector which applies the flat mapping function to the input elements and
     *         collects all resulting elements to a {@code List}
     */
    @Beta
    public static <T, U> Collector<T, ?, List<U>> flatMappingToList(final Function<? super T, ? extends Stream<? extends U>> mapper) {
        return flatMapping(mapper, Collectors.<U> toList());
    }

    /**
     * Returns a {@code Collector} that applies a flat mapping function returning collections
     * to each input element and accumulates all elements from the resulting collections.
     * 
     * <p>This collector is similar to {@code flatMapping} but works with {@code Collection}
     * instead of {@code Stream}. This can be more efficient when the mapper function
     * already returns collections, avoiding the overhead of stream creation.</p>
     * 
     * <p>Empty collections returned by the mapper are handled efficiently and don't
     * contribute any elements to the downstream collector.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Flatten a map's values
     * Map<String, List<Integer>> map = new HashMap<>();
     * map.put("a", Arrays.asList(1, 2));
     * map.put("b", Arrays.asList(3, 4));
     * List<Integer> values = map.entrySet().stream()
     *     .collect(Collectors.flatmapping(Map.Entry::getValue, Collectors.toList()));
     * // Result: [1, 2, 3, 4]
     * }</pre>
     *
     * @param <T> the type of the input elements
     * @param <U> type of elements in collections returned by mapper
     * @param <A> intermediate accumulation type of the downstream collector
     * @param <R> result type of collector
     * @param mapper a function to be applied to the input elements, which returns a
     *               collection of results
     * @param downstream a collector which will accept elements of the collections returned
     *                   by mapper
     * @return a collector which applies the flat mapping function to the input elements and
     *         provides the elements of the resulting collections to the downstream collector
     */
    public static <T, U, A, R> Collector<T, ?, R> flatmapping(final Function<? super T, ? extends Collection<? extends U>> mapper, // NOSONAR
            final Collector<? super U, A, R> downstream) {
        final BiConsumer<A, ? super U> downstreamAccumulator = downstream.accumulator();

        final BiConsumer<A, T> accumulator = (a, t) -> {
            final Collection<? extends U> c = mapper.apply(t);

            if (N.notEmpty(c)) {
                for (final U u : c) {
                    downstreamAccumulator.accept(a, u);
                }
            }
        };

        return create(downstream.supplier(), accumulator, downstream.combiner(), downstream.finisher(), downstream.characteristics());
    }

    /**
     * Returns a {@code Collector} that applies a flat mapping function returning collections
     * and collects all resulting elements to a {@code List}.
     * 
     * <p>This is a convenience method that combines flat mapping of collections with
     * collecting to a list. Each element is transformed into a collection by the mapper
     * function, and all elements from all collections are collected into a single list.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect all tags from multiple posts
     * List<Post> posts = getPosts();
     * List<String> allTags = posts.stream()
     *     .collect(Collectors.flatmappingToList(Post::getTags));
     * // Collects all tags from all posts into a single list
     * }</pre>
     *
     * @param <T> the type of the input elements
     * @param <U> the type of elements in collections returned by mapper
     * @param mapper a function to be applied to the input elements, which returns a
     *               collection of results
     * @return a collector which applies the flat mapping function to the input elements and
     *         collects all resulting elements to a {@code List}
     */
    @Beta
    public static <T, U> Collector<T, ?, List<U>> flatmappingToList(final Function<? super T, ? extends Collection<? extends U>> mapper) { // NOSONAR
        return flatmapping(mapper, Collectors.<U> toList());
    }

    /**
     * Returns a {@code Collector} that performs an additional finishing transformation
     * on the results of another collector.
     * 
     * <p>This collector adapts a downstream collector to perform an additional finishing
     * transformation. After the downstream collector finishes its operation, the finisher
     * function is applied to transform the result. This is useful for post-processing
     * collected data.</p>
     * 
     * <p>The characteristics of the returned collector are derived from the downstream
     * collector, with {@code IDENTITY_FINISH} removed if present.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect to a list then get its size
     * Integer count = Stream.of("a", "b", "c")
     *     .collect(Collectors.collectingAndThen(
     *         Collectors.toList(),
     *         List::size));
     * // Result: 3
     * }</pre>
     *
     * @param <T> the type of the input elements
     * @param <A> intermediate accumulation type of the downstream collector
     * @param <R> result type of the downstream collector
     * @param <RR> result type of the resulting collector
     * @param downstream a collector
     * @param finisher a function to be applied to the final result of the downstream collector
     * @return a collector which performs the action of the downstream collector, followed by
     *         an additional finishing step
     * @throws IllegalArgumentException if downstream or finisher is null
     */
    public static <T, A, R, RR> Collector<T, A, RR> collectingAndThen(final Collector<T, A, R> downstream, final Function<? super R, RR> finisher)
            throws IllegalArgumentException {
        N.checkArgNotNull(downstream);
        N.checkArgNotNull(finisher);

        final Function<A, R> downstreamFinisher = downstream.finisher();

        final Function<A, RR> thenFinisher = t -> finisher.apply(downstreamFinisher.apply(t));

        Set<Characteristics> characteristics = downstream.characteristics();

        if (characteristics.contains(Characteristics.IDENTITY_FINISH)) {
            if (characteristics.size() == 1) {
                characteristics = N.emptySet();
            } else {
                characteristics = EnumSet.copyOf(characteristics);
                characteristics.remove(Characteristics.IDENTITY_FINISH);
                characteristics = Collections.unmodifiableSet(characteristics);
            }
        }

        return create(downstream.supplier(), downstream.accumulator(), downstream.combiner(), thenFinisher, characteristics);
    }

    /**
     * Returns a {@code Collector} that wraps the result in an {@code Optional}, returning
     * an empty {@code Optional} if no elements were collected.
     * 
     * <p>This collector tracks whether any elements were accumulated and returns an
     * empty {@code Optional} if the stream was empty, or an {@code Optional} containing
     * the collected result otherwise. This is useful when you want to distinguish between
     * an empty collection result and no elements being processed.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Returns Optional.empty() for empty stream
     * Optional<List<String>> result = Stream.<String>empty()
     *     .collect(Collectors.collectingOrEmpty(Collectors.toList()));
     * // Result: Optional.empty()
     * 
     * // Returns Optional with empty list for stream with no matching elements
     * Optional<List<String>> filtered = Stream.of("a", "b", "c")
     *     .filter(s -> s.length() > 5)
     *     .collect(Collectors.collectingOrEmpty(Collectors.toList()));
     * // Result: Optional[[]] (Optional containing empty list)
     * }</pre>
     *
     * @param <T> the type of the input elements
     * @param <A> intermediate accumulation type of the collector
     * @param <R> result type of the collector
     * @param collector the downstream collector
     * @return a collector which returns an {@code Optional} of the collected result,
     *         or empty if no elements were collected
     * @throws IllegalArgumentException if collector is null
     */
    @Beta
    public static <T, A, R> Collector<T, A, Optional<R>> collectingOrEmpty(final Collector<T, A, R> collector) throws IllegalArgumentException {
        N.checkArgNotNull(collector);

        final MutableBoolean accumulated = MutableBoolean.of(false);
        final BiConsumer<A, T> downstreamAccumulator = collector.accumulator();
        final Function<A, R> downstreamFinisher = collector.finisher();

        final BiConsumer<A, T> newAccumulator = (a, t) -> {
            downstreamAccumulator.accept(a, t);
            accumulated.setTrue();
        };

        final Function<A, Optional<R>> newFinisher = a -> {
            if (accumulated.isTrue()) {
                return Optional.of(downstreamFinisher.apply(a));
            } else {
                return Optional.empty();
            }
        };

        Set<Characteristics> characteristics = collector.characteristics();

        if (characteristics.contains(Characteristics.IDENTITY_FINISH)) {
            if (characteristics.size() == 1) {
                characteristics = N.emptySet();
            } else {
                characteristics = EnumSet.copyOf(characteristics);
                characteristics.remove(Characteristics.IDENTITY_FINISH);
                characteristics = Collections.unmodifiableSet(characteristics);
            }
        }

        return create(collector.supplier(), newAccumulator, collector.combiner(), newFinisher, characteristics);
    }

    /**
     * Returns a {@code Collector} that uses a default value when no elements are collected.
     * 
     * <p>This collector tracks whether any elements were accumulated. If no elements
     * were collected, it returns the specified default value instead of the collector's
     * normal empty result. This is useful for providing fallback values.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Returns default list when stream is empty
     * List<String> result = Stream.<String>empty()
     *     .collect(Collectors.collectingOrElseIfEmpty(
     *         Collectors.toList(),
     *         Arrays.asList("default")));
     * // Result: ["default"]
     * }</pre>
     *
     * @param <T> the type of the input elements
     * @param <A> intermediate accumulation type of the collector
     * @param <R> result type of the collector
     * @param collector the downstream collector
     * @param defaultForEmpty the default value to return if no elements are collected
     * @return a collector which returns the collected result, or the default value if
     *         no elements were collected
     */
    @Beta
    public static <T, A, R> Collector<T, A, R> collectingOrElseIfEmpty(final Collector<T, A, R> collector, final R defaultForEmpty) {
        return collectingOrElseGetIfEmpty(collector, () -> defaultForEmpty);
    }

    /**
     * Returns a {@code Collector} that uses a supplier to provide a default value when
     * no elements are collected.
     * 
     * <p>This collector tracks whether any elements were accumulated. If no elements
     * were collected, it calls the supplier to get a default value instead of the
     * collector's normal empty result. The supplier is only called if needed, allowing
     * for lazy evaluation of the default.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Compute default only if needed
     * List<String> result = Stream.<String>empty()
     *     .collect(Collectors.collectingOrElseGetIfEmpty(
     *         Collectors.toList(),
     *         () -> loadDefaultsFromDatabase()));
     * // loadDefaultsFromDatabase() is only called because stream was empty
     * }</pre>
     *
     * @param <T> the type of the input elements
     * @param <A> intermediate accumulation type of the collector
     * @param <R> result type of the collector
     * @param collector the downstream collector
     * @param defaultForEmpty a supplier for the default value if no elements are collected
     * @return a collector which returns the collected result, or the supplied default value
     *         if no elements were collected
     * @throws IllegalArgumentException if collector is null
     */
    @Beta
    public static <T, A, R> Collector<T, A, R> collectingOrElseGetIfEmpty(final Collector<T, A, R> collector, final Supplier<? extends R> defaultForEmpty)
            throws IllegalArgumentException {
        N.checkArgNotNull(collector);

        final MutableBoolean accumulated = MutableBoolean.of(false);
        final BiConsumer<A, T> downstreamAccumulator = collector.accumulator();
        final Function<A, R> downstreamFinisher = collector.finisher();

        final BiConsumer<A, T> newAccumulator = (a, t) -> {
            downstreamAccumulator.accept(a, t);
            accumulated.setTrue();
        };

        final Function<A, R> newFinisher = a -> {
            if (accumulated.isTrue()) {
                return downstreamFinisher.apply(a);
            } else {
                return defaultForEmpty.get();
            }
        };

        Set<Characteristics> characteristics = collector.characteristics();

        if (characteristics.contains(Characteristics.IDENTITY_FINISH)) {
            if (characteristics.size() == 1) {
                characteristics = N.emptySet();
            } else {
                characteristics = EnumSet.copyOf(characteristics);
                characteristics.remove(Characteristics.IDENTITY_FINISH);
                characteristics = Collections.unmodifiableSet(characteristics);
            }
        }

        return create(collector.supplier(), newAccumulator, collector.combiner(), newFinisher, characteristics);
    }

    /**
     * Returns a {@code Collector} that throws {@code NoSuchElementException} when no
     * elements are collected.
     * 
     * <p>This collector tracks whether any elements were accumulated. If no elements
     * were collected, it throws a {@code NoSuchElementException}. This is useful when
     * an empty result is an error condition.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Throws if no elements match
     * List<String> result = Stream.of("a", "b", "c")
     *     .filter(s -> s.length() > 5)
     *     .collect(Collectors.collectingOrElseThrowIfEmpty(Collectors.toList()));
     * // Throws NoSuchElementException
     * }</pre>
     *
     * @param <T> the type of the input elements
     * @param <A> intermediate accumulation type of the collector
     * @param <R> result type of the collector
     * @param collector the downstream collector
     * @return a collector which returns the collected result, or throws if no elements
     *         were collected
     * @throws NoSuchElementException if no elements are collected
     */
    @Beta
    public static <T, A, R> Collector<T, A, R> collectingOrElseThrowIfEmpty(final Collector<T, A, R> collector) {
        return collectingOrElseGetIfEmpty(collector, () -> {
            throw noSuchElementExceptionSupplier.get();
        });
    }

    /**
     * Returns a {@code Collector} that throws a custom exception when no elements are collected.
     * 
     * <p>This collector tracks whether any elements were accumulated. If no elements
     * were collected, it throws the exception provided by the supplier. This allows
     * for custom error handling when empty results are not acceptable.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Throws custom exception if no valid data found
     * List<Data> result = dataStream
     *     .filter(Data::isValid)
     *     .collect(Collectors.collectingOrElseThrowIfEmpty(
     *         Collectors.toList(),
     *         () -> new DataNotFoundException("No valid data found")));
     * }</pre>
     *
     * @param <T> the type of the input elements
     * @param <A> intermediate accumulation type of the collector
     * @param <R> result type of the collector
     * @param collector the downstream collector
     * @param exceptionSupplier supplier for the exception to throw if no elements are collected
     * @return a collector which returns the collected result, or throws if no elements
     *         were collected
     */
    @Beta
    public static <T, A, R> Collector<T, A, R> collectingOrElseThrowIfEmpty(final Collector<T, A, R> collector,
            final Supplier<? extends RuntimeException> exceptionSupplier) {
        return collectingOrElseGetIfEmpty(collector, () -> {
            throw exceptionSupplier.get();
        });
    }

    /**
     * Returns a {@code Collector} which collects distinct elements into a {@code List}
     * based on a key extraction function.
     * 
     * <p>This collector maintains the first occurrence of each distinct key. Elements
     * are considered distinct if their extracted keys are equal according to
     * {@code Object.equals()}. For ordered streams, the first element with each distinct
     * key is preserved.</p>
     * 
     * <p>This operation is equivalent to {@code stream.distinct(keyMapper).toList()},
     * but may work faster as it's implemented as a single collector.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Keep first person of each age
     * List<Person> distinctByAge = people.stream()
     *     .collect(Collectors.distinctByToList(Person::getAge));
     * // If multiple people have the same age, only the first is kept
     * }</pre>
     *
     * @param <T> the type of the input elements
     * @param keyMapper a function which classifies input elements
     * @return a collector which collects distinct elements to the {@code List}
     */
    public static <T> Collector<T, ?, List<T>> distinctByToList(final Function<? super T, ?> keyMapper) {
        return distinctByToCollection(keyMapper, Suppliers.ofList());
    }

    /**
     * Returns a {@code Collector} which collects distinct elements into a collection
     * based on a key extraction function.
     * 
     * <p>This collector maintains the first occurrence of each distinct key. Elements
     * are added to the supplied collection type. The collector uses a {@code LinkedHashMap}
     * internally to maintain insertion order while checking for duplicates.</p>
     * 
     * <p>For ordered streams, this preserves the encounter order of the first occurrence
     * of each distinct element.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect distinct strings by length to a TreeSet
     * Set<String> distinctByLength = words.stream()
     *     .collect(Collectors.distinctByToCollection(
     *         String::length,
     *         TreeSet::new));
     * }</pre>
     *
     * @param <T> the type of the input elements
     * @param <C> the type of the resulting collection
     * @param keyMapper a function which classifies input elements
     * @param supplier a supplier providing a new empty collection into which the
     *                 distinct elements will be inserted
     * @return a collector which collects distinct elements to the specified collection type
     */
    public static <T, C extends Collection<T>> Collector<T, ?, C> distinctByToCollection(final Function<? super T, ?> keyMapper,
            final Supplier<? extends C> supplier) {
        final Supplier<Map<Object, T>> mappSupplier = Suppliers.ofLinkedHashMap();

        final BiConsumer<Map<Object, T>, T> accumulator = (map, t) -> {
            final Object key = keyMapper.apply(t);

            if (!map.containsKey(key)) {
                map.put(key, t);
            }
        };

        final BinaryOperator<Map<Object, T>> combiner = (a, b) -> {
            for (final Map.Entry<Object, T> entry : b.entrySet()) {
                if (!a.containsKey(entry.getKey())) {
                    a.put(entry.getKey(), entry.getValue());
                }
            }

            return a;
        };

        final Function<Map<Object, T>, C> finisher = map -> {
            final C c = supplier.get();
            c.addAll(map.values());
            return c;
        };

        return create(mappSupplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    /**
     * Returns a {@code Collector} which counts the number of distinct values produced
     * by the mapper function.
     * 
     * <p>This collector counts unique values based on the keys extracted by the mapper
     * function. Elements with equal keys (according to {@code Object.equals()}) are
     * counted only once. This is useful for counting unique properties without collecting
     * the actual elements.</p>
     * 
     * <p>The operation is equivalent to {@code stream.map(mapper).distinct().count()},
     * but is implemented as a single collector which can be more efficient.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Count unique departments
     * Integer uniqueDepartments = employees.stream()
     *     .collect(Collectors.distinctByToCounting(Employee::getDepartment));
     * // Result: number of distinct departments
     * }</pre>
     *
     * @param <T> the type of the input elements
     * @param keyMapper a function which classifies input elements
     * @return a collector which counts the number of distinct classes the mapper
     *         function returns for the stream elements
     */
    public static <T> Collector<T, ?, Integer> distinctByToCounting(final Function<? super T, ?> keyMapper) {
        final Supplier<Set<Object>> supplier = Suppliers.ofSet();

        final BiConsumer<Set<Object>, T> accumulator = (c, t) -> c.add(keyMapper.apply(t));

        final BinaryOperator<Set<Object>> combiner = BinaryOperators.ofAddAllToBigger();

        final Function<Set<Object>, Integer> finisher = Set::size;

        return create(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    /**
     * Returns a {@code Collector} that counts the number of input elements.
     * 
     * <p>This collector counts all elements in the stream, returning the count as a
     * {@code Long}. This is equivalent to {@code stream.count()} but can be used
     * as a downstream collector in more complex collection operations.</p>
     * 
     * <p>The implementation optimizes by incrementing a counter rather than storing
     * elements, making it memory efficient for large streams.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Count total elements
     * Long count = stream.collect(Collectors.counting());
     * 
     * // Use as downstream collector
     * Map<String, Long> counts = stream
     *     .collect(Collectors.groupingBy(
     *         String::toLowerCase,
     *         Collectors.counting()));
     * }</pre>
     *
     * @param <T> the type of the input elements
     * @return a {@code Collector} that counts the input elements
     */
    public static <T> Collector<T, ?, Long> counting() {
        return summingLong(it -> 1L);
    }

    /**
     * Returns a {@code Collector} that counts the number of input elements as an {@code Integer}.
     * 
     * <p>This collector counts all elements in the stream, returning the count as an
     * {@code Integer}. This is useful when you know the count will fit in an integer
     * range and want to avoid the overhead of {@code Long}.</p>
     * 
     * <p>Be aware that this collector may overflow for streams with more than
     * {@code Integer.MAX_VALUE} elements.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Count elements as int
     * Integer count = stream.collect(Collectors.countingToInt());
     * 
     * // Use in grouping
     * Map<Category, Integer> categoryCounts = products.stream()
     *     .collect(Collectors.groupingBy(
     *         Product::getCategory,
     *         Collectors.countingToInt()));
     * }</pre>
     *
     * @param <T> the type of the input elements
     * @return a {@code Collector} that counts the input elements as an {@code Integer}
     */
    public static <T> Collector<T, ?, Integer> countingToInt() {
        return summingInt(it -> 1);
    }

    /**
     * Returns a {@code Collector} that finds the minimum element according to the
     * natural ordering.
     * 
     * <p>This collector finds the minimum element, treating {@code null} as the largest
     * value (using {@code nullsLast} comparison). The result is wrapped in an
     * {@code Optional} which is empty if the stream is empty.</p>
     * 
     * <p>Elements must implement {@code Comparable}. For custom comparison logic,
     * use {@link #min(Comparator)} instead.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find minimum value
     * Optional<Integer> min = Stream.of(3, 1, 4, 1, 5)
     *     .collect(Collectors.min());
     * // Result: Optional[1]
     * }</pre>
     *
     * @param <T> the type of the input elements, must be {@code Comparable}
     * @return a {@code Collector} that produces the minimal element, wrapped in an
     *         {@code Optional}
     */
    public static <T extends Comparable<? super T>> Collector<T, ?, Optional<T>> min() {
        return min(Comparators.nullsLast());
    }

    /**
     * Returns a {@code Collector} that finds the minimum element according to a given
     * {@code Comparator}.
     * 
     * <p>This collector finds the minimum element using the provided comparator.
     * The result is wrapped in an {@code Optional} which is empty if the stream is empty.
     * The comparator is used for all comparisons, including handling of {@code null} values
     * if present.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find shortest string
     * Optional<String> shortest = Stream.of("apple", "pie", "banana")
     *     .collect(Collectors.min(Comparator.comparing(String::length)));
     * // Result: Optional["pie"]
     * }</pre>
     *
     * @param <T> the type of the input elements
     * @param comparator a {@code Comparator} to compare the elements
     * @return a {@code Collector} that produces the minimal element according to the
     *         comparator, wrapped in an {@code Optional}
     * @throws IllegalArgumentException if the comparator is null
     */
    public static <T> Collector<T, ?, Optional<T>> min(final Comparator<? super T> comparator) throws IllegalArgumentException {
        N.checkArgNotNull(comparator);

        final BinaryOperator<T> op = (a, b) -> comparator.compare(a, b) <= 0 ? a : b;

        return reducing(op);
    }

    /**
     * Returns a {@code Collector} that finds the minimum element with a default value
     * for empty streams.
     * 
     * <p>This collector finds the minimum element according to natural ordering.
     * If the stream is empty, it returns the specified default value instead of
     * an empty {@code Optional}. This is useful when you always need a value.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find minimum with default
     * Integer min = Stream.<Integer>empty()
     *     .collect(Collectors.minOrElse(Integer.MAX_VALUE));
     * // Result: Integer.MAX_VALUE (the default)
     * }</pre>
     *
     * @param <T> the type of the input elements, must be {@code Comparable}
     * @param defaultForEmpty the default value to return if the stream is empty
     * @return a {@code Collector} that produces the minimal element, or the default
     *         value if no elements are present
     */
    public static <T extends Comparable<? super T>> Collector<T, ?, T> minOrElse(final T defaultForEmpty) {
        return minOrElseGet(() -> defaultForEmpty);
    }

    /**
     * Returns a {@code Collector} that finds the minimum element according to a comparator,
     * with a default value for empty streams.
     * 
     * <p>This collector finds the minimum element using the provided comparator.
     * If the stream is empty, it returns the specified default value. This combines
     * custom comparison with a fallback value.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find shortest string with default
     * String shortest = emptyStream
     *     .collect(Collectors.minOrElse(
     *         Comparator.comparing(String::length),
     *         "NO_DATA"));
     * // Result: "NO_DATA" if stream is empty
     * }</pre>
     *
     * @param <T> the type of the input elements
     * @param comparator a {@code Comparator} to compare the elements
     * @param defaultForEmpty the default value to return if the stream is empty
     * @return a {@code Collector} that produces the minimal element according to the
     *         comparator, or the default value if no elements are present
     */
    public static <T> Collector<T, ?, T> minOrElse(final Comparator<? super T> comparator, final T defaultForEmpty) {
        return minOrElseGet(comparator, () -> defaultForEmpty);
    }

    /**
     * Returns a {@code Collector} that finds the minimum element according to the
     * natural order, or returns the value supplied by {@code supplierForEmpty} if
     * no elements are present.
     * 
     * <p>This collector is useful when you need a default value instead of dealing
     * with an empty {@code Optional}. The elements must be {@code Comparable}.</p>
     * 
     * <p>The returned collector handles {@code null} values by placing them last in the
     * ordering (nulls are considered greater than {@code non-null} values).</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get minimum or default value
     * Integer min = Stream.of(5, 3, 8, 2)
     *     .collect(Collectors.minOrElseGet(() -> 0));
     * // Result: 2
     * 
     * // Empty stream returns default
     * Integer defaultMin = Stream.<Integer>empty()
     *     .collect(Collectors.minOrElseGet(() -> -1));
     * // Result: -1
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param supplierForEmpty a supplier providing the default value if no elements are present
     * @return a {@code Collector} which finds the minimum element or returns the default value
     */
    public static <T extends Comparable<? super T>> Collector<T, ?, T> minOrElseGet(final Supplier<? extends T> supplierForEmpty) {
        return minOrElseGet(Comparators.nullsLast(), supplierForEmpty);
    }

    /**
     * Returns a {@code Collector} that finds the minimum element according to the
     * provided comparator, or returns the value supplied by {@code supplierForEmpty}
     * if no elements are present.
     * 
     * <p>This collector provides more flexibility than {@code minOrElseGet(Supplier)}
     * by allowing custom comparison logic through the comparator parameter.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find minimum by string length or return default
     * String shortest = Stream.of("apple", "pi", "banana")
     *     .collect(Collectors.minOrElseGet(
     *         Comparator.comparingInt(String::length),
     *         () -> "none"));
     * // Result: "pi"
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param comparator a {@code Comparator} for comparing elements
     * @param supplierForEmpty a supplier providing the default value if no elements are present
     * @return a {@code Collector} which finds the minimum element or returns the default value
     * @throws IllegalArgumentException if the comparator is null
     */
    public static <T> Collector<T, ?, T> minOrElseGet(final Comparator<? super T> comparator, final Supplier<? extends T> supplierForEmpty)
            throws IllegalArgumentException {
        N.checkArgNotNull(comparator);

        final BinaryOperator<T> op = (a, b) -> comparator.compare(a, b) <= 0 ? a : b;

        return reducingOrElseGet(op, supplierForEmpty);
    }

    /**
     * Returns a {@code Collector} that finds the minimum element according to the
     * natural order, throwing a {@code NoSuchElementException} if no elements are present.
     * 
     * <p>This collector is useful when the absence of elements should be treated as
     * an exceptional condition. The elements must be {@code Comparable}.</p>
     * 
     * <p>The returned collector handles {@code null} values by placing them last in the
     * ordering (nulls are considered greater than {@code non-null} values).</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find minimum element
     * Integer min = Stream.of(5, 3, 8, 2)
     *     .collect(Collectors.minOrElseThrow());
     * // Result: 2
     * 
     * // Empty stream throws exception
     * Stream.<Integer>empty()
     *     .collect(Collectors.minOrElseThrow());
     * // Throws: NoSuchElementException
     * }</pre>
     *
     * @param <T> the type of input elements
     * @return a {@code Collector} which finds the minimum element or throws if empty
     */
    public static <T extends Comparable<? super T>> Collector<T, ?, T> minOrElseThrow() {
        return minOrElseThrow(Comparators.nullsLast());
    }

    /**
     * Returns a {@code Collector} that finds the minimum element according to the
     * provided comparator, throwing a {@code NoSuchElementException} if no elements are present.
     * 
     * <p>This collector provides more flexibility than {@code minOrElseThrow()}
     * by allowing custom comparison logic through the comparator parameter.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find minimum by string length
     * String shortest = Stream.of("apple", "pi", "banana")
     *     .collect(Collectors.minOrElseThrow(
     *         Comparator.comparingInt(String::length)));
     * // Result: "pi"
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param comparator a {@code Comparator} for comparing elements
     * @return a {@code Collector} which finds the minimum element or throws if empty
     */
    public static <T> Collector<T, ?, T> minOrElseThrow(final Comparator<? super T> comparator) {
        return minOrElseThrow(comparator, noSuchElementExceptionSupplier);
    }

    /**
     * Returns a {@code Collector} that finds the minimum element according to the
     * provided comparator, throwing a custom exception if no elements are present.
     * 
     * <p>This collector allows you to specify both the comparison logic and the
     * exception to throw when the stream is empty, providing maximum flexibility.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find minimum with custom exception
     * Person youngest = people.stream()
     *     .collect(Collectors.minOrElseThrow(
     *         Comparator.comparingInt(Person::getAge),
     *         () -> new IllegalStateException("No people found")));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param comparator a {@code Comparator} for comparing elements
     * @param exceptionSupplier a supplier providing the exception to throw if no elements are present
     * @return a {@code Collector} which finds the minimum element or throws a custom exception
     * @throws IllegalArgumentException if the comparator is null
     */
    public static <T> Collector<T, ?, T> minOrElseThrow(final Comparator<? super T> comparator, final Supplier<? extends RuntimeException> exceptionSupplier)
            throws IllegalArgumentException {
        N.checkArgNotNull(comparator);

        final BinaryOperator<T> op = (a, b) -> comparator.compare(a, b) <= 0 ? a : b;

        return reducingOrElseThrow(op, exceptionSupplier);
    }

    private static final Supplier<NoSuchElementException> noSuchElementExceptionSupplier = NoSuchElementException::new;

    /**
     * Returns a {@code Collector} that finds the minimum element by extracting a
     * {@code Comparable} key from each element, returning an {@code Optional}.
     * 
     * <p>This collector is useful when you want to find the minimum element based
     * on a specific property. The key extractor function should return a
     * {@code Comparable} value.</p>
     * 
     * <p>The returned collector handles {@code null} keys by placing them last in the
     * ordering (null keys are considered greater than {@code non-null} keys).</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find person with minimum age
     * Optional<Person> youngest = people.stream()
     *     .collect(Collectors.minBy(Person::getAge));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param keyMapper a function extracting a {@code Comparable} key from each element
     * @return a {@code Collector} which finds the element with the minimum key
     */
    @SuppressWarnings("rawtypes")
    public static <T> Collector<T, ?, Optional<T>> minBy(final Function<? super T, ? extends Comparable> keyMapper) {
        return min(Comparators.nullsLastBy(keyMapper));
    }

    /**
     * Returns a {@code Collector} that finds the minimum element by extracting a
     * {@code Comparable} key from each element, or returns the value supplied by
     * {@code supplierForEmpty} if no elements are present.
     * 
     * <p>This collector combines the convenience of key-based comparison with
     * a default value for empty streams.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find person with minimum age or return default
     * Person youngest = people.stream()
     *     .collect(Collectors.minByOrElseGet(
     *         Person::getAge,
     *         () -> new Person("Unknown", 0)));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param keyMapper a function extracting a {@code Comparable} key from each element
     * @param supplierForEmpty a supplier providing the default value if no elements are present
     * @return a {@code Collector} which finds the element with the minimum key or returns default
     */
    @SuppressWarnings("rawtypes")
    public static <T> Collector<T, ?, T> minByOrElseGet(final Function<? super T, ? extends Comparable> keyMapper,
            final Supplier<? extends T> supplierForEmpty) {
        return minOrElseGet(Comparators.nullsLastBy(keyMapper), supplierForEmpty);
    }

    /**
     * Returns a {@code Collector} that finds the minimum element by extracting a
     * {@code Comparable} key from each element, throwing a {@code NoSuchElementException}
     * if no elements are present.
     * 
     * <p>This collector is useful when you need to find the minimum based on a
     * property and want to treat empty streams as exceptional cases.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find person with minimum age or throw
     * Person youngest = people.stream()
     *     .collect(Collectors.minByOrElseThrow(Person::getAge));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param keyMapper a function extracting a {@code Comparable} key from each element
     * @return a {@code Collector} which finds the element with the minimum key or throws
     */
    @SuppressWarnings("rawtypes")
    public static <T> Collector<T, ?, T> minByOrElseThrow(final Function<? super T, ? extends Comparable> keyMapper) {
        return minOrElseThrow(Comparators.nullsLastBy(keyMapper));
    }

    /**
     * Returns a {@code Collector} that finds the minimum element by extracting a
     * {@code Comparable} key from each element, throwing a custom exception if
     * no elements are present.
     * 
     * <p>This collector provides maximum flexibility for key-based minimum finding
     * with custom exception handling.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find product with minimum price or throw custom exception
     * Product cheapest = products.stream()
     *     .collect(Collectors.minByOrElseThrow(
     *         Product::getPrice,
     *         () -> new ProductNotFoundException("No products available")));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param keyMapper a function extracting a {@code Comparable} key from each element
     * @param exceptionSupplier a supplier providing the exception to throw if no elements are present
     * @return a {@code Collector} which finds the element with the minimum key or throws
     */
    @SuppressWarnings("rawtypes")
    public static <T> Collector<T, ?, T> minByOrElseThrow(final Function<? super T, ? extends Comparable> keyMapper,
            final Supplier<? extends RuntimeException> exceptionSupplier) {
        return minOrElseThrow(Comparators.nullsLastBy(keyMapper), exceptionSupplier);
    }

    /**
     * Returns a {@code Collector} that finds the maximum element according to the
     * natural order, returning an {@code Optional}.
     * 
     * <p>This collector is the counterpart to {@code min()} and finds the largest
     * element. The elements must be {@code Comparable}.</p>
     * 
     * <p>The returned collector handles {@code null} values by placing them first in the
     * ordering (nulls are considered less than {@code non-null} values).</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find maximum element
     * Optional<Integer> max = Stream.of(5, 3, 8, 2)
     *     .collect(Collectors.max());
     * // Result: Optional[8]
     * }</pre>
     *
     * @param <T> the type of input elements
     * @return a {@code Collector} which finds the maximum element
     */
    public static <T extends Comparable<? super T>> Collector<T, ?, Optional<T>> max() {
        return max(Comparators.nullsFirst());
    }

    /**
     * Returns a {@code Collector} that finds the maximum element according to the
     * provided comparator, returning an {@code Optional}.
     * 
     * <p>This collector allows custom comparison logic for finding the maximum
     * element. If multiple elements are considered equal according to the comparator,
     * the first encountered element is returned.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find longest string
     * Optional<String> longest = Stream.of("apple", "pi", "banana")
     *     .collect(Collectors.max(Comparator.comparingInt(String::length)));
     * // Result: Optional["banana"]
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param comparator a {@code Comparator} for comparing elements
     * @return a {@code Collector} which finds the maximum element
     * @throws IllegalArgumentException if the comparator is null
     */
    public static <T> Collector<T, ?, Optional<T>> max(final Comparator<? super T> comparator) throws IllegalArgumentException {
        N.checkArgNotNull(comparator);

        final BinaryOperator<T> op = (a, b) -> comparator.compare(a, b) >= 0 ? a : b;

        return reducing(op);
    }

    /**
     * Returns a {@code Collector} that finds the maximum element according to the
     * natural order, or returns the specified default value if no elements are present.
     * 
     * <p>This collector is a convenience method that provides a default value
     * directly instead of using a supplier.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find maximum with default value
     * Integer max = Stream.of(5, 3, 8, 2)
     *     .collect(Collectors.maxOrElse(0));
     * // Result: 8
     * 
     * // Empty stream returns default
     * Integer defaultMax = Stream.<Integer>empty()
     *     .collect(Collectors.maxOrElse(0));
     * // Result: 0
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param defaultForEmpty the default value to return if no elements are present
     * @return a {@code Collector} which finds the maximum element or returns the default
     */
    public static <T extends Comparable<? super T>> Collector<T, ?, T> maxOrElse(final T defaultForEmpty) {
        return maxOrElseGet(() -> defaultForEmpty);
    }

    /**
     * Returns a {@code Collector} that finds the maximum element according to the
     * provided comparator, or returns the specified default value if no elements are present.
     * 
     * <p>This collector combines custom comparison logic with a simple default value
     * for empty streams.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find longest string with default
     * String longest = Stream.of("apple", "pi", "banana")
     *     .collect(Collectors.maxOrElse(
     *         Comparator.comparingInt(String::length),
     *         "empty"));
     * // Result: "banana"
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param comparator a {@code Comparator} for comparing elements
     * @param defaultForEmpty the default value to return if no elements are present
     * @return a {@code Collector} which finds the maximum element or returns the default
     */
    public static <T> Collector<T, ?, T> maxOrElse(final Comparator<? super T> comparator, final T defaultForEmpty) {
        return maxOrElseGet(comparator, () -> defaultForEmpty);
    }

    /**
     * Returns a {@code Collector} that finds the maximum element according to the
     * natural order, or returns the value supplied by {@code supplierForEmpty} if
     * no elements are present.
     * 
     * <p>This collector is useful when you need a default value instead of dealing
     * with an empty {@code Optional}. The elements must be {@code Comparable}.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find maximum or compute default
     * Integer max = Stream.of(5, 3, 8, 2)
     *     .collect(Collectors.maxOrElseGet(() -> computeDefault()));
     * // Result: 8
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param supplierForEmpty a supplier providing the default value if no elements are present
     * @return a {@code Collector} which finds the maximum element or returns the default
     */
    public static <T extends Comparable<? super T>> Collector<T, ?, T> maxOrElseGet(final Supplier<? extends T> supplierForEmpty) {
        return maxOrElseGet(Comparators.nullsFirst(), supplierForEmpty);
    }

    /**
     * Returns a {@code Collector} that finds the maximum element according to the
     * provided comparator, or returns the value supplied by {@code supplierForEmpty}
     * if no elements are present.
     * 
     * <p>This collector provides full flexibility for finding maximum elements with
     * custom comparison and default value logic.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find person with maximum age or create default
     * Person oldest = people.stream()
     *     .collect(Collectors.maxOrElseGet(
     *         Comparator.comparingInt(Person::getAge),
     *         () -> new Person("Unknown", 0)));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param comparator a {@code Comparator} for comparing elements
     * @param supplierForEmpty a supplier providing the default value if no elements are present
     * @return a {@code Collector} which finds the maximum element or returns the default
     * @throws IllegalArgumentException if the comparator is null
     */
    public static <T> Collector<T, ?, T> maxOrElseGet(final Comparator<? super T> comparator, final Supplier<? extends T> supplierForEmpty)
            throws IllegalArgumentException {
        N.checkArgNotNull(comparator);

        final BinaryOperator<T> op = (a, b) -> comparator.compare(a, b) >= 0 ? a : b;

        return reducingOrElseGet(op, supplierForEmpty);
    }

    /**
     * Returns a {@code Collector} that finds the maximum element according to the
     * natural order, throwing a {@code NoSuchElementException} if no elements are present.
     * 
     * <p>This collector is useful when the absence of elements should be treated as
     * an exceptional condition. The elements must be {@code Comparable}.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find maximum element
     * Integer max = Stream.of(5, 3, 8, 2)
     *     .collect(Collectors.maxOrElseThrow());
     * // Result: 8
     * 
     * // Empty stream throws exception
     * Stream.<Integer>empty()
     *     .collect(Collectors.maxOrElseThrow());
     * // Throws: NoSuchElementException
     * }</pre>
     *
     * @param <T> the type of input elements
     * @return a {@code Collector} which finds the maximum element or throws if empty
     */
    public static <T extends Comparable<? super T>> Collector<T, ?, T> maxOrElseThrow() {
        return maxOrElseThrow(Comparators.nullsFirst());
    }

    /**
     * Returns a {@code Collector} that finds the maximum element according to the
     * provided comparator, throwing a {@code NoSuchElementException} if no elements are present.
     * 
     * <p>This collector provides more flexibility than {@code maxOrElseThrow()}
     * by allowing custom comparison logic.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find longest string or throw
     * String longest = Stream.of("apple", "pi", "banana")
     *     .collect(Collectors.maxOrElseThrow(
     *         Comparator.comparingInt(String::length)));
     * // Result: "banana"
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param comparator a {@code Comparator} for comparing elements
     * @return a {@code Collector} which finds the maximum element or throws if empty
     */
    public static <T> Collector<T, ?, T> maxOrElseThrow(final Comparator<? super T> comparator) {
        return maxOrElseThrow(comparator, noSuchElementExceptionSupplier);
    }

    /**
     * Returns a {@code Collector} that finds the maximum element according to the
     * provided comparator, throwing a custom exception if no elements are present.
     * 
     * <p>This collector allows you to specify both the comparison logic and the
     * exception to throw when the stream is empty.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find maximum with custom exception
     * Product mostExpensive = products.stream()
     *     .collect(Collectors.maxOrElseThrow(
     *         Comparator.comparingDouble(Product::getPrice),
     *         () -> new ProductNotFoundException("No products found")));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param comparator a {@code Comparator} for comparing elements
     * @param exceptionSupplier a supplier providing the exception to throw if no elements are present
     * @return a {@code Collector} which finds the maximum element or throws a custom exception
     * @throws IllegalArgumentException if the comparator is null
     */
    public static <T> Collector<T, ?, T> maxOrElseThrow(final Comparator<? super T> comparator, final Supplier<? extends RuntimeException> exceptionSupplier)
            throws IllegalArgumentException {
        N.checkArgNotNull(comparator);

        final BinaryOperator<T> op = (a, b) -> comparator.compare(a, b) >= 0 ? a : b;

        return reducingOrElseThrow(op, exceptionSupplier);
    }

    /**
     * Returns a {@code Collector} that finds the maximum element by extracting a
     * {@code Comparable} key from each element, returning an {@code Optional}.
     * 
     * <p>This collector is useful when you want to find the maximum element based
     * on a specific property. The key extractor function should return a
     * {@code Comparable} value.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find person with maximum age
     * Optional<Person> oldest = people.stream()
     *     .collect(Collectors.maxBy(Person::getAge));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param keyMapper a function extracting a {@code Comparable} key from each element
     * @return a {@code Collector} which finds the element with the maximum key
     */
    @SuppressWarnings("rawtypes")
    public static <T> Collector<T, ?, Optional<T>> maxBy(final Function<? super T, ? extends Comparable> keyMapper) {
        return max(Comparators.nullsFirstBy(keyMapper));
    }

    /**
     * Returns a {@code Collector} that finds the maximum element by extracting a
     * {@code Comparable} key from each element, or returns the value supplied by
     * {@code supplierForEmpty} if no elements are present.
     * 
     * <p>This collector combines the convenience of key-based comparison with
     * a default value for empty streams.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find person with maximum age or return default
     * Person oldest = people.stream()
     *     .collect(Collectors.maxByOrElseGet(
     *         Person::getAge,
     *         () -> new Person("Unknown", 0)));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param keyMapper a function extracting a {@code Comparable} key from each element
     * @param supplierForEmpty a supplier providing the default value if no elements are present
     * @return a {@code Collector} which finds the element with the maximum key or returns default
     */
    @SuppressWarnings("rawtypes")
    public static <T> Collector<T, ?, T> maxByOrElseGet(final Function<? super T, ? extends Comparable> keyMapper,
            final Supplier<? extends T> supplierForEmpty) {
        return maxOrElseGet(Comparators.nullsFirstBy(keyMapper), supplierForEmpty);
    }

    /**
     * Returns a {@code Collector} that finds the maximum element by extracting a
     * {@code Comparable} key from each element, throwing a {@code NoSuchElementException}
     * if no elements are present.
     * 
     * <p>This collector is useful when you need to find the maximum based on a
     * property and want to treat empty streams as exceptional cases.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find person with maximum age or throw
     * Person oldest = people.stream()
     *     .collect(Collectors.maxByOrElseThrow(Person::getAge));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param keyMapper a function extracting a {@code Comparable} key from each element
     * @return a {@code Collector} which finds the element with the maximum key or throws
     */
    @SuppressWarnings("rawtypes")
    public static <T> Collector<T, ?, T> maxByOrElseThrow(final Function<? super T, ? extends Comparable> keyMapper) {
        return maxOrElseThrow(Comparators.nullsFirstBy(keyMapper));
    }

    /**
     * Returns a {@code Collector} that finds the maximum element by extracting a
     * {@code Comparable} key from each element, throwing a custom exception if
     * no elements are present.
     * 
     * <p>This collector provides maximum flexibility for key-based maximum finding
     * with custom exception handling.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find product with maximum price or throw custom exception
     * Product mostExpensive = products.stream()
     *     .collect(Collectors.maxByOrElseThrow(
     *         Product::getPrice,
     *         () -> new ProductNotFoundException("No products available")));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param keyMapper a function extracting a {@code Comparable} key from each element
     * @param exceptionSupplier a supplier providing the exception to throw if no elements are present
     * @return a {@code Collector} which finds the element with the maximum key or throws
     */
    @SuppressWarnings("rawtypes")
    public static <T> Collector<T, ?, T> maxByOrElseThrow(final Function<? super T, ? extends Comparable> keyMapper,
            final Supplier<? extends RuntimeException> exceptionSupplier) {
        return maxOrElseThrow(Comparators.nullsFirstBy(keyMapper), exceptionSupplier);
    }

    /**
     * It's copied from StreamEx: <a href="https://github.com/amaembo/streamex">StreamEx</a> under Apache License v2 and may be modified.
     * <br />
     *
     * Returns a {@code Collector} which finds all the elements which are equal
     * to each other and smaller than any other element according to the natural
     * order. The found elements are collected to {@link List}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find all minimum integers
     * List<Integer> mins = Stream.of(3, 1, 4, 1, 5, 1)
     *     .collect(Collectors.minAll());
     * // Result: [1, 1, 1]
     *
     * // Find all shortest strings
     * List<String> shortest = Stream.of("apple", "pie", "cat", "dog")
     *     .collect(Collectors.minAll());
     * // Result: ["pie", "cat", "dog"]
     * }</pre>
     *
     * @param <T> the type of the input elements
     * @return a {@code Collector} which finds all the minimal elements and
     *         collects them to the {@code List}.
     * @see #minAll(Comparator)
     * @see #minAll(Collector)
     */
    public static <T extends Comparable<? super T>> Collector<T, ?, List<T>> minAll() {
        return minAll(Comparators.nullsLast());
    }

    /**
     * It's copied from StreamEx: <a href="https://github.com/amaembo/streamex">StreamEx</a> under Apache License v2 and may be modified.
     * <br />
     *
     * Returns a {@code Collector} which finds all the elements which are equal
     * to each other and smaller than any other element according to the
     * specified {@link Comparator}. The found elements are collected to
     * {@link List}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find all people with the minimum age
     * List<Person> youngest = people.stream()
     *     .collect(Collectors.minAll(Comparator.comparingInt(Person::getAge)));
     *
     * // Find all strings with minimum length (case-insensitive)
     * List<String> shortest = Stream.of("Apple", "pie", "CAT", "dog")
     *     .collect(Collectors.minAll(Comparator.comparingInt(String::length)));
     * // Result: ["pie", "CAT", "dog"]
     * }</pre>
     *
     * @param <T> the type of the input elements
     * @param comparator a {@code Comparator} to compare the elements
     * @return a {@code Collector} which finds all the minimal elements and
     *         collects them to the {@code List}.
     * @see #minAll(Comparator, Collector)
     * @see #minAll()
     */
    public static <T> Collector<T, ?, List<T>> minAll(final Comparator<? super T> comparator) {
        return minAll(comparator, Integer.MAX_VALUE);
    }

    /**
     * Returns a {@code Collector} which finds all the elements which are equal
     * to each other and smaller than any other element according to the
     * specified {@link Comparator}, collecting at most the specified number of elements.
     * 
     * <p>This collector is useful when you want to find all minimal elements but
     * limit the result size for memory efficiency.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find at most 5 minimum values
     * List<Integer> mins = Stream.of(1, 3, 1, 5, 1, 2, 1)
     *     .collect(Collectors.minAll(Comparator.naturalOrder(), 5));
     * // Result: [1, 1, 1, 1] (all minimal values, limited by actual count)
     * }</pre>
     *
     * @param <T> the type of the input elements
     * @param comparator a {@code Comparator} to compare the elements
     * @param atMostSize the maximum number of minimal elements to collect
     * @return a {@code Collector} which finds at most the specified number of minimal elements
     */
    public static <T> Collector<T, ?, List<T>> minAll(final Comparator<? super T> comparator, final int atMostSize) {
        return maxAll(Comparators.reverseOrder(comparator), atMostSize);
    }

    /**
     * It's copied from StreamEx: <a href="https://github.com/amaembo/streamex">StreamEx</a> under Apache License v2 and may be modified.
     * <br />
     *
     * Returns a {@code Collector} which finds all the elements which are equal
     * to each other and smaller than any other element according to the natural
     * order. The found elements are reduced using the specified downstream
     * {@code Collector}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Count how many minimum values exist
     * Long minCount = Stream.of(3, 1, 4, 1, 5, 1)
     *     .collect(Collectors.minAll(Collectors.counting()));
     * // Result: 3
     *
     * // Join all minimum strings
     * String joined = Stream.of("apple", "pie", "cat", "dog")
     *     .collect(Collectors.minAll(Collectors.joining(", ")));
     * // Result: "pie, cat, dog"
     * }</pre>
     *
     * @param <T> the type of the input elements
     * @param <A> the intermediate accumulation type of the downstream collector
     * @param <R> the result type of the downstream reduction
     * @param downstream a {@code Collector} implementing the downstream
     *        reduction
     * @return a {@code Collector} which finds all the minimal elements.
     * @see #minAll(Comparator, Collector)
     * @see #minAll(Comparator)
     * @see #minAll()
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable, A, R> Collector<T, ?, R> minAll(final Collector<T, A, R> downstream) {
        return minAll(Comparators.nullsLast(), downstream);
    }

    /**
     * It's copied from StreamEx: <a href="https://github.com/amaembo/streamex">StreamEx</a> under Apache License v2 and may be modified.
     * <br />
     *
     * Returns a {@code Collector} which finds all the elements which are equal
     * to each other and smaller than any other element according to the
     * specified {@link Comparator}. The found elements are reduced using the
     * specified downstream {@code Collector}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find all people with minimum age and collect to a set
     * Set<Person> youngest = people.stream()
     *     .collect(Collectors.minAll(
     *         Comparator.comparingInt(Person::getAge),
     *         Collectors.toSet()));
     *
     * // Sum all products with minimum price
     * double totalMinPrice = products.stream()
     *     .collect(Collectors.minAll(
     *         Comparator.comparingDouble(Product::getPrice),
     *         Collectors.summingDouble(Product::getPrice)));
     * }</pre>
     *
     * @param <T> the type of the input elements
     * @param <A> the intermediate accumulation type of the downstream collector
     * @param <R> the result type of the downstream reduction
     * @param comparator a {@code Comparator} to compare the elements
     * @param downstream a {@code Collector} implementing the downstream
     *        reduction
     * @return a {@code Collector} which finds all the minimal elements.
     * @see #minAll(Comparator)
     * @see #minAll(Collector)
     * @see #minAll()
     */
    public static <T, A, R> Collector<T, ?, R> minAll(final Comparator<? super T> comparator, final Collector<T, A, R> downstream) {
        return maxAll(Comparators.reverseOrder(comparator), downstream);
    }

    /**
     * Returns a {@code Collector} which finds all the elements which are equal
     * to each other and smaller than any other element according to the natural
     * order, returning an {@code Optional} containing the minimum element and the
     * result of the downstream collector.
     * 
     * <p>This collector is useful when you need both the minimum element and
     * some aggregation of all minimum elements.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find minimum value and count of occurrences
     * Optional<Pair<Integer, Long>> result = Stream.of(1, 3, 1, 5, 1)
     *     .collect(Collectors.minAlll(Collectors.counting()));
     * // Result: Optional[Pair(1, 3)]
     * }</pre>
     *
     * @param <T> the type of the input elements
     * @param <R> the result type of the downstream reduction
     * @param downstream a {@code Collector} implementing the downstream reduction
     * @return a {@code Collector} which finds the minimum element and applies downstream
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable, R> Collector<T, ?, Optional<Pair<T, R>>> minAlll(final Collector<T, ?, R> downstream) {
        return minAlll(Comparators.nullsLast(), downstream);
    }

    /**
     * Returns a {@code Collector} which finds all the elements which are equal
     * to each other and smaller than any other element according to the
     * specified {@link Comparator}, returning an {@code Optional} containing the
     * minimum element and the result of the downstream collector.
     * 
     * <p>This collector provides both the minimum element and an aggregation
     * of all elements that are equal to the minimum.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find shortest string and concatenate all shortest strings
     * Optional<Pair<String, String>> result = Stream.of("a", "bb", "a", "ccc")
     *     .collect(Collectors.minAlll(
     *         Comparator.comparingInt(String::length),
     *         Collectors.joining(",")));
     * // Result: Optional[Pair("a", "a,a")]
     * }</pre>
     *
     * @param <T> the type of the input elements
     * @param <R> the result type of the downstream reduction
     * @param comparator a {@code Comparator} to compare the elements
     * @param downstream a {@code Collector} implementing the downstream reduction
     * @return a {@code Collector} which finds the minimum element and applies downstream
     */
    public static <T, R> Collector<T, ?, Optional<Pair<T, R>>> minAlll(final Comparator<? super T> comparator, final Collector<? super T, ?, R> downstream) {
        return minAlll(comparator, downstream, Fn.identity());
    }

    /**
     * Returns a {@code Collector} which finds all the elements which are equal
     * to each other and smaller than any other element according to the
     * specified {@link Comparator}, applies a downstream collector, and then
     * applies a finisher function to the result.
     * 
     * <p>This collector provides maximum flexibility by allowing transformation
     * of the final result.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find minimum and format result
     * String result = Stream.of(1, 3, 1, 5, 1)
     *     .collect(Collectors.minAlll(
     *         Comparator.naturalOrder(),
     *         Collectors.counting(),
     *         opt -> opt.map(p -> "Min: " + p.left() + ", Count: " + p.right())
     *                   .orElse("No elements")));
     * // Result: "Min: 1, Count: 3"
     * }</pre>
     *
     * @param <T> the type of the input elements
     * @param <R> the result type of the downstream reduction
     * @param <RR> the final result type after applying the finisher
     * @param comparator a {@code Comparator} to compare the elements
     * @param downstream a {@code Collector} implementing the downstream reduction
     * @param finisher a function to apply to the final result
     * @return a {@code Collector} which finds the minimum and transforms the result
     */
    public static <T, R, RR> Collector<T, ?, RR> minAlll(final Comparator<? super T> comparator, final Collector<? super T, ?, R> downstream,
            final Function<Optional<Pair<T, R>>, RR> finisher) {
        return maxAlll(Comparators.reverseOrder(comparator), downstream, finisher);
    }

    /**
     * It's copied from StreamEx: <a href="https://github.com/amaembo/streamex">StreamEx</a> under Apache License v2 and may be modified.
     * <br />
     *
     * Returns a {@code Collector} which finds all the elements which are equal
     * to each other and bigger than any other element according to the natural
     * order. The found elements are collected to {@link List}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find all maximum integers
     * List<Integer> maxs = Stream.of(3, 5, 4, 5, 1, 5)
     *     .collect(Collectors.maxAll());
     * // Result: [5, 5, 5]
     *
     * // Find all longest strings
     * List<String> longest = Stream.of("apple", "pie", "banana", "cherry")
     *     .collect(Collectors.maxAll());
     * // Result: ["apple", "banana", "cherry"]
     * }</pre>
     *
     * @param <T> the type of the input elements
     * @return a {@code Collector} which finds all the maximal elements and
     *         collects them to the {@code List}.
     * @see #maxAll(Comparator)
     * @see #maxAll(Collector)
     */
    public static <T extends Comparable<? super T>> Collector<T, ?, List<T>> maxAll() {
        return maxAll(Comparators.nullsFirst());
    }

    /**
     * It's copied from StreamEx: <a href="https://github.com/amaembo/streamex">StreamEx</a> under Apache License v2 and may be modified.
     * <br />
     *
     * Returns a {@code Collector} which finds all the elements which are equal
     * to each other and bigger than any other element according to the
     * specified {@link Comparator}. The found elements are collected to
     * {@link List}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find all people with the maximum age
     * List<Person> oldest = people.stream()
     *     .collect(Collectors.maxAll(Comparator.comparingInt(Person::getAge)));
     *
     * // Find all strings with maximum length
     * List<String> longest = Stream.of("Apple", "pie", "banana", "cherry")
     *     .collect(Collectors.maxAll(Comparator.comparingInt(String::length)));
     * // Result: ["Apple", "banana", "cherry"]
     * }</pre>
     *
     * @param <T> the type of the input elements
     * @param comparator a {@code Comparator} to compare the elements
     * @return a {@code Collector} which finds all the maximal elements and
     *         collects them to the {@code List}.
     * @see #maxAll(Comparator, Collector)
     * @see #maxAll()
     */
    public static <T> Collector<T, ?, List<T>> maxAll(final Comparator<? super T> comparator) {
        return maxAll(comparator, Integer.MAX_VALUE);
    }

    /**
     * Returns a {@code Collector} which finds all the elements which are equal
     * to each other and bigger than any other element according to the
     * specified {@link Comparator}, collecting at most the specified number of elements.
     * 
     * <p>This collector is useful when you want to find all maximal elements but
     * limit the result size for memory efficiency.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find at most 3 maximum values
     * List<Integer> maxs = Stream.of(5, 3, 5, 1, 5, 2)
     *     .collect(Collectors.maxAll(Comparator.naturalOrder(), 3));
     * // Result: [5, 5, 5]
     * }</pre>
     *
     * @param <T> the type of the input elements
     * @param comparator a {@code Comparator} to compare the elements
     * @param atMostSize the maximum number of maximal elements to collect
     * @return a {@code Collector} which finds at most the specified number of maximal elements
     */
    public static <T> Collector<T, ?, List<T>> maxAll(final Comparator<? super T> comparator, final int atMostSize) {
        final Supplier<Pair<T, List<T>>> supplier = () -> {
            final List<T> list = new ArrayList<>(Math.min(16, atMostSize));
            return Pair.of((T) NONE, list);
        };

        final BiConsumer<Pair<T, List<T>>, T> accumulator = (a, t) -> {
            if (a.left() == NONE) {
                a.setLeft(t);

                if (a.right().size() < atMostSize) {
                    a.right().add(t);
                }
            } else {
                final int cmp = comparator.compare(t, a.left());

                if (cmp > 0) {
                    a.setLeft(t);
                    a.right().clear();
                }

                if ((cmp >= 0) && (a.right().size() < atMostSize)) {
                    a.right().add(t);
                }
            }
        };

        final BinaryOperator<Pair<T, List<T>>> combiner = (a, b) -> {
            if (b.left() == NONE) {
                return a;
            } else if (a.left() == NONE) {
                return b;
            }

            final int cmp = comparator.compare(a.left(), b.left());

            if (cmp > 0) {
                return a;
            } else if (cmp < 0) {
                return b;
            }

            if (a.right().size() < atMostSize) {
                if (b.right().size() <= atMostSize - a.right().size()) {
                    a.right().addAll(b.right());
                } else {
                    a.right().addAll(b.right().subList(0, atMostSize - a.right().size()));
                }
            }

            return a;
        };

        final Function<Pair<T, List<T>>, List<T>> finisher = Pair::right;

        return create(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    /**
     * It's copied from StreamEx: <a href="https://github.com/amaembo/streamex">StreamEx</a> under Apache License v2 and may be modified.
     * <br />
     *
     * Returns a {@code Collector} which finds all the elements which are equal
     * to each other and bigger than any other element according to the natural
     * order. The found elements are reduced using the specified downstream
     * {@code Collector}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Count how many maximum values exist
     * Long maxCount = Stream.of(3, 5, 4, 5, 1, 5)
     *     .collect(Collectors.maxAll(Collectors.counting()));
     * // Result: 3
     *
     * // Join all maximum strings with a delimiter
     * String joined = Stream.of("apple", "pie", "banana", "cherry")
     *     .collect(Collectors.maxAll(Collectors.joining(" & ")));
     * // Result: "apple & banana & cherry"
     * }</pre>
     *
     * @param <T> the type of the input elements
     * @param <R> the result type of the downstream reduction
     * @param downstream a {@code Collector} implementing the downstream
     *        reduction
     * @return a {@code Collector} which finds all the maximal elements.
     * @see #maxAll(Comparator, Collector)
     * @see #maxAll(Comparator)
     * @see #maxAll()
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable, R> Collector<T, ?, R> maxAll(final Collector<T, ?, R> downstream) {
        return maxAll(Comparators.nullsFirst(), downstream);
    }

    /**
     * It's copied from StreamEx: <a href="https://github.com/amaembo/streamex">StreamEx</a> under Apache License v2 and may be modified.
     * <br />
     *
     * Returns a {@code Collector} which finds all the elements which are equal
     * to each other and bigger than any other element according to the
     * specified {@link Comparator}. The found elements are reduced using the
     * specified downstream {@code Collector}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find all people with maximum age and collect to a set
     * Set<Person> oldest = people.stream()
     *     .collect(Collectors.maxAll(
     *         Comparator.comparingInt(Person::getAge),
     *         Collectors.toSet()));
     *
     * // Calculate average price of all products with maximum price
     * double avgMaxPrice = products.stream()
     *     .collect(Collectors.maxAll(
     *         Comparator.comparingDouble(Product::getPrice),
     *         Collectors.averagingDouble(Product::getPrice)));
     * }</pre>
     *
     * @param <T> the type of the input elements
     * @param <R> the result type of the downstream reduction
     * @param comparator a {@code Comparator} to compare the elements
     * @param downstream a {@code Collector} implementing the downstream
     *        reduction
     * @return a {@code Collector} which finds all the maximal elements.
     * @see #maxAll(Comparator)
     * @see #maxAll(Collector)
     * @see #maxAll()
     */
    @SuppressWarnings("rawtypes")
    public static <T, R> Collector<T, ?, R> maxAll(final Comparator<? super T> comparator, final Collector<? super T, ?, R> downstream) {
        final Supplier<Object> downstreamSupplier = (Supplier<Object>) downstream.supplier();
        final BiConsumer<Object, ? super T> downstreamAccumulator = (BiConsumer<Object, ? super T>) downstream.accumulator();
        final BinaryOperator<Object> downstreamCombiner = (BinaryOperator<Object>) downstream.combiner();
        final Function<Object, R> downstreamFinisher = (Function<Object, R>) downstream.finisher();
        final MutableBoolean isCollection = MutableBoolean.of(false);
        final MutableBoolean isMap = MutableBoolean.of(false);

        final Supplier<Pair<T, Object>> supplier = () -> {
            final Object container = downstreamSupplier.get();

            if (container instanceof Collection<?> coll && coll.isEmpty()) {
                try {
                    //noinspection RedundantOperationOnEmptyContainer
                    coll.clear();

                    isCollection.setTrue();
                } catch (final Exception e) {
                    // ignore
                }
            } else if (container instanceof Map<?, ?> map && map.isEmpty()) {
                try {
                    //noinspection RedundantOperationOnEmptyContainer
                    map.clear();

                    isMap.setTrue();
                } catch (final Exception e) {
                    // ignore
                }
            }

            return Pair.of((T) none(), container);
        };

        final BiConsumer<Pair<T, Object>, T> accumulator = (a, t) -> {
            if (a.left() == NONE) {
                a.setLeft(t);
                downstreamAccumulator.accept(a.right(), t);
            } else {
                final int cmp = comparator.compare(t, a.left());

                if (cmp > 0) {
                    if (isCollection.isTrue()) {
                        ((Collection) a.right()).clear();
                    } else if (isMap.isTrue()) {
                        ((Map) a.right()).clear();
                    } else {
                        a.setRight(downstreamSupplier.get());
                    }

                    a.setLeft(t);
                }

                if (cmp >= 0) {
                    downstreamAccumulator.accept(a.right(), t);
                }
            }
        };

        final BinaryOperator<Pair<T, Object>> combiner = (a, b) -> {
            if (b.left() == NONE) {
                return a;
            } else if (a.left() == NONE) {
                return b;
            }

            final int cmp = comparator.compare(a.left(), b.left());

            if (cmp > 0) {
                return a;
            } else if (cmp < 0) {
                return b;
            }

            a.setRight(downstreamCombiner.apply(a.right(), b.right()));

            return a;
        };

        final Function<Pair<T, Object>, R> finisher = t -> downstreamFinisher.apply(t.right());

        return create(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    /**
     * Returns a {@code Collector} which finds all the elements which are equal
     * to each other and bigger than any other element according to the natural
     * order, returning an {@code Optional} containing the maximum element and the
     * result of the downstream collector.
     * 
     * <p>This collector is useful when you need both the maximum element and
     * some aggregation of all maximum elements.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find maximum value and sum of all maximums
     * Optional<Pair<Integer, Integer>> result = Stream.of(5, 3, 5, 1, 5)
     *     .collect(Collectors.maxAlll(Collectors.summingInt(i -> i)));
     * // Result: Optional[Pair(5, 15)]
     * }</pre>
     *
     * @param <T> the type of the input elements
     * @param <R> the result type of the downstream reduction
     * @param downstream a {@code Collector} implementing the downstream reduction
     * @return a {@code Collector} which finds the maximum element and applies downstream
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable, R> Collector<T, ?, Optional<Pair<T, R>>> maxAlll(final Collector<T, ?, R> downstream) {
        return maxAlll(Comparators.nullsFirst(), downstream);
    }

    /**
     * Returns a {@code Collector} which finds all the elements which are equal
     * to each other and bigger than any other element according to the
     * specified {@link Comparator}, returning an {@code Optional} containing the
     * maximum element and the result of the downstream collector.
     * 
     * <p>This collector provides both the maximum element and an aggregation
     * of all elements that are equal to the maximum.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find longest strings and join them
     * Optional<Pair<String, String>> result = Stream.of("abc", "xy", "def")
     *     .collect(Collectors.maxAlll(
     *         Comparator.comparingInt(String::length),
     *         Collectors.joining("-")));
     * // Result: Optional[Pair("abc", "abc-def")]
     * }</pre>
     *
     * @param <T> the type of the input elements
     * @param <R> the result type of the downstream reduction
     * @param comparator a {@code Comparator} to compare the elements
     * @param downstream a {@code Collector} implementing the downstream reduction
     * @return a {@code Collector} which finds the maximum element and applies downstream
     */
    public static <T, R> Collector<T, ?, Optional<Pair<T, R>>> maxAlll(final Comparator<? super T> comparator, final Collector<? super T, ?, R> downstream) {
        return maxAlll(comparator, downstream, Fn.identity());
    }

    /**
     * Returns a {@code Collector} which finds all the elements which are equal
     * to each other and bigger than any other element according to the
     * specified {@link Comparator}, applies a downstream collector, and then
     * applies a finisher function to the result.
     * 
     * <p>This collector provides maximum flexibility by allowing transformation
     * of the final result.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find maximum and create custom result
     * boolean hasMax = Stream.of(1, 3, 5, 5, 2)
     *     .collect(Collectors.maxAlll(
     *         Comparator.naturalOrder(),
     *         Collectors.toList(),
     *         opt -> opt.isPresent() && opt.get().right().size() > 1));
     * // Result: true (multiple elements with max value 5)
     * }</pre>
     *
     * @param <T> the type of the input elements
     * @param <R> the result type of the downstream reduction
     * @param <RR> the final result type after applying the finisher
     * @param comparator a {@code Comparator} to compare the elements
     * @param downstream a {@code Collector} implementing the downstream reduction
     * @param finisher a function to apply to the final result
     * @return a {@code Collector} which finds the maximum and transforms the result
     */
    @SuppressWarnings("rawtypes")
    public static <T, R, RR> Collector<T, ?, RR> maxAlll(final Comparator<? super T> comparator, final Collector<? super T, ?, R> downstream,
            final Function<Optional<Pair<T, R>>, RR> finisher) {
        final Supplier<Object> downstreamSupplier = (Supplier<Object>) downstream.supplier();
        final BiConsumer<Object, ? super T> downstreamAccumulator = (BiConsumer<Object, ? super T>) downstream.accumulator();
        final BinaryOperator<Object> downstreamCombiner = (BinaryOperator<Object>) downstream.combiner();
        final Function<Object, R> downstreamFinisher = (Function<Object, R>) downstream.finisher();
        final MutableBoolean isCollection = MutableBoolean.of(false);
        final MutableBoolean isMap = MutableBoolean.of(false);

        final Supplier<Pair<T, Object>> supplier = () -> {
            final Object container = downstreamSupplier.get();

            if (container instanceof Collection<?> coll && coll.isEmpty()) {
                try {
                    //noinspection RedundantOperationOnEmptyContainer
                    coll.clear();

                    isCollection.setTrue();
                } catch (final Exception e) {
                    // ignore
                }
            } else if (container instanceof Map<?, ?> map && map.isEmpty()) {
                try {
                    //noinspection RedundantOperationOnEmptyContainer
                    map.clear();

                    isMap.setTrue();
                } catch (final Exception e) {
                    // ignore
                }
            }

            return Pair.of((T) none(), container);
        };

        final BiConsumer<Pair<T, Object>, T> accumulator = (a, t) -> {
            if (a.left() == NONE) {
                a.setLeft(t);
                downstreamAccumulator.accept(a.right(), t);
            } else {
                final int cmp = comparator.compare(t, a.left());

                if (cmp > 0) {
                    if (isCollection.isTrue()) {
                        ((Collection) a.right()).clear();
                    } else if (isMap.isTrue()) {
                        ((Map) a.right()).clear();
                    } else {
                        a.setRight(downstreamSupplier.get());
                    }

                    a.setLeft(t);
                }

                if (cmp >= 0) {
                    downstreamAccumulator.accept(a.right(), t);
                }
            }
        };

        final BinaryOperator<Pair<T, Object>> combiner = (a, b) -> {
            if (b.left() == NONE) {
                return a;
            } else if (a.left() == NONE) {
                return b;
            }

            final int cmp = comparator.compare(a.left(), b.left());

            if (cmp > 0) {
                return a;
            } else if (cmp < 0) {
                return b;
            } else {
                a.setRight(downstreamCombiner.apply(a.right(), b.right()));
                return a;
            }
        };

        final Function<Pair<T, Object>, RR> finalFinisher = a -> {
            final Optional<Pair<T, R>> result = a.left() == NONE ? Optional.empty() : Optional.of(Pair.of(a.left(), downstreamFinisher.apply(a.right())));

            return finisher.apply(result);
        };

        return create(supplier, accumulator, combiner, finalFinisher, CH_UNORDERED_NOID);
    }

    /**
     * Returns a {@code Collector} that finds both the minimum and maximum elements from
     * a stream of {@code Comparable} elements.
     * 
     * <p>This collector processes the stream in a single pass to find both the minimum
     * and maximum elements according to their natural ordering. If the stream is empty,
     * an empty {@code Optional} is returned.</p>
     * 
     * <p>The collector produces a stable result for ordered streams: if several
     * minimal or maximal elements appear, the collector always selects the
     * first encountered.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find min and max from a stream of integers
     * Optional<Pair<Integer, Integer>> result = Stream.of(5, 2, 8, 1, 9)
     *     .collect(Collectors.minMax());
     * // Result: Optional[Pair(1, 9)]
     * }</pre>
     *
     * @param <T> the type of input elements, must be Comparable
     * @return a {@code Collector} which finds the minimum and maximum elements,
     *         wrapped in an {@code Optional<Pair>}
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> Collector<T, ?, Optional<Pair<T, T>>> minMax() {
        return minMax(Comparators.naturalOrder());
    }

    /**
     * Returns a {@code Collector} that finds both the minimum and maximum elements from
     * a stream according to the specified comparator.
     * 
     * <p>This collector processes the stream in a single pass to find both the minimum
     * and maximum elements according to the provided comparator. If the stream is empty,
     * an empty {@code Optional} is returned.</p>
     * 
     * <p>The collector produces a stable result for ordered streams: if several
     * minimal or maximal elements appear, the collector always selects the
     * first encountered.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find shortest and longest strings
     * Optional<Pair<String, String>> result = Stream.of("a", "abc", "ab", "abcd")
     *     .collect(Collectors.minMax(Comparator.comparing(String::length)));
     * // Result: Optional[Pair(a, abcd)]
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param comparator comparator used to compare elements
     * @return a {@code Collector} which finds the minimum and maximum elements,
     *         wrapped in an {@code Optional<Pair>}
     * @see #minMax(Comparator, BiFunction)
     */
    public static <T> Collector<T, ?, Optional<Pair<T, T>>> minMax(final Comparator<? super T> comparator) {
        return minMax(comparator, Fn.pair());
    }

    /**
     * Returns a {@code Collector} which finds the minimal and maximal element
     * according to the supplied comparator, then applies finisher function to
     * them producing the final result.
     * 
     * <p>This collector is useful when you need to perform a custom operation on
     * the minimum and maximum elements found. The finisher function is only called
     * if the stream contains at least one element.</p>
     *
     * <p>This collector produces a stable result for ordered stream: if several
     * minimal or maximal elements appear, the collector always selects the
     * first encountered.</p>
     *
     * <p>If there are no input elements, the finisher method is not called and
     * empty {@code Optional} is returned. Otherwise, the finisher result is
     * wrapped into {@code Optional}.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Calculate the range (difference between max and min)
     * Optional<Integer> range = Stream.of(5, 2, 8, 1, 9)
     *     .collect(Collectors.minMax(Integer::compare, (min, max) -> max - min));
     * // Result: Optional[8]
     * }</pre>
     *
     * @param <T> the type of the input elements
     * @param <R> the type of the result wrapped into {@code Optional}
     * @param comparator comparator which is used to find the minimal and maximal elements
     * @param finisher a {@link BiFunction} which takes the minimal and maximal elements and produces the final result
     * @return a {@code Collector} which finds minimal and maximal elements
     */
    public static <T, R> Collector<T, ?, Optional<R>> minMax(final Comparator<? super T> comparator,
            final BiFunction<? super T, ? super T, ? extends R> finisher) {

        final BiFunction<Optional<T>, Optional<T>, Optional<R>> finisher2 = (min,
                max) -> min.isPresent() ? Optional.of((R) finisher.apply(min.get(), max.get())) : Optional.empty();

        return MoreCollectors.combine(Collectors.min(comparator), Collectors.max(comparator), finisher2);
    }

    /**
     * Returns a {@code Collector} that finds both the minimum and maximum elements from
     * a stream by comparing the results of applying the given key extraction function.
     * 
     * <p>This collector is useful when you want to find the elements with the minimum
     * and maximum values based on a specific property. The key mapper function extracts
     * a {@code Comparable} value from each element for comparison.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find the person with minimum and maximum age
     * Optional<Pair<Person, Person>> result = people.stream()
     *     .collect(Collectors.minMaxBy(Person::getAge));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param keyMapper function to extract a {@code Comparable} key from elements
     * @return a {@code Collector} which finds the elements with minimum and maximum keys,
     *         wrapped in an {@code Optional<Pair>}
     */
    @SuppressWarnings("rawtypes")
    public static <T> Collector<T, ?, Optional<Pair<T, T>>> minMaxBy(final Function<? super T, ? extends Comparable> keyMapper) {
        return minMax(Comparators.comparingBy(keyMapper));
    }

    /**
     * Returns a {@code Collector} that finds the elements with minimum and maximum keys
     * according to the provided key mapper, then applies a finisher function to produce
     * a final result.
     * 
     * <p>This collector combines the functionality of finding min/max by a key with
     * a custom finisher operation. The finisher is only called if the stream contains
     * at least one element.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find the age difference between youngest and oldest person
     * Optional<Integer> ageDiff = people.stream()
     *     .collect(Collectors.minMaxBy(Person::getAge, 
     *         (youngest, oldest) -> oldest.getAge() - youngest.getAge()));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <R> the type of the result wrapped into {@code Optional}
     * @param keyMapper function to extract a {@code Comparable} key from elements
     * @param finisher a {@link BiFunction} which takes the elements with minimum and maximum keys
     *                 and produces the final result
     * @return a {@code Collector} which finds elements with minimum and maximum keys
     *         and applies the finisher function
     */
    @SuppressWarnings("rawtypes")
    public static <T, R> Collector<T, ?, Optional<R>> minMaxBy(final Function<? super T, ? extends Comparable> keyMapper,
            final BiFunction<? super T, ? super T, ? extends R> finisher) {
        return minMax(Comparators.comparingBy(keyMapper), finisher);
    }

    /**
     * Returns a {@code Collector} that finds both the minimum and maximum elements from
     * a stream of {@code Comparable} elements, returning a default value if the stream is empty.
     * 
     * <p>This collector is similar to {@link #minMax()}, but instead of returning an
     * {@code Optional}, it returns the result of the supplier function when the stream
     * is empty. This is useful when you want to avoid dealing with {@code Optional}
     * and have a sensible default value.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find min and max with default values
     * Pair<Integer, Integer> result = Stream.<Integer>empty()
     *     .collect(Collectors.minMaxOrElseGet(() -> Pair.of(0, 100)));
     * // Result: Pair(0, 100)
     * }</pre>
     *
     * @param <T> the type of input elements, must be Comparable
     * @param supplierForEmpty supplier that provides a default {@code Pair} when stream is empty
     * @return a {@code Collector} which finds the minimum and maximum elements,
     *         or returns the supplied default if stream is empty
     */
    public static <T extends Comparable<? super T>> Collector<T, ?, Pair<T, T>> minMaxOrElseGet(
            final Supplier<Pair<? extends T, ? extends T>> supplierForEmpty) {
        return minMaxOrElseGet(Comparators.naturalOrder(), supplierForEmpty);
    }

    /**
     * Returns a {@code Collector} that finds both the minimum and maximum elements according
     * to the specified comparator, returning a default value if the stream is empty.
     * 
     * <p>This collector processes the stream to find both extremes in a single pass.
     * If the stream is empty, the supplier function is called to provide a default result.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find shortest and longest strings with defaults
     * Pair<String, String> result = Stream.<String>empty()
     *     .collect(Collectors.minMaxOrElseGet(
     *         Comparator.comparing(String::length),
     *         () -> Pair.of("", "default")));
     * // Result: Pair(, default)
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param comparator comparator used to compare elements
     * @param supplierForEmpty supplier that provides a default {@code Pair} when stream is empty
     * @return a {@code Collector} which finds the minimum and maximum elements,
     *         or returns the supplied default if stream is empty
     */
    public static <T> Collector<T, ?, Pair<T, T>> minMaxOrElseGet(final Comparator<? super T> comparator,
            final Supplier<Pair<? extends T, ? extends T>> supplierForEmpty) {
        return MoreCollectors.combine(Collectors.min(comparator), Collectors.max(comparator), (min, max) -> {
            if (min.isPresent()) {
                return Pair.of(min.get(), max.get());
            } else {
                return (Pair<T, T>) supplierForEmpty.get();
            }
        });
    }

    /**
     * Returns a {@code Collector} that finds both the minimum and maximum elements from
     * a stream of {@code Comparable} elements, throwing an exception if the stream is empty.
     * 
     * <p>This collector is useful when an empty stream represents an error condition
     * in your application logic. It guarantees that the result will always contain
     * valid minimum and maximum values.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // This will throw NoSuchElementException if stream is empty
     * Pair<Integer, Integer> result = Stream.of(5, 2, 8, 1, 9)
     *     .collect(Collectors.minMaxOrElseThrow());
     * // Result: Pair(1, 9)
     * }</pre>
     *
     * @param <T> the type of input elements, must be Comparable
     * @return a {@code Collector} which finds the minimum and maximum elements
     * @throws NoSuchElementException if the stream is empty
     */
    public static <T extends Comparable<? super T>> Collector<T, ?, Pair<T, T>> minMaxOrElseThrow() {
        return minMaxOrElseThrow(Comparators.naturalOrder());
    }

    /**
     * Returns a {@code Collector} that finds both the minimum and maximum elements according
     * to the specified comparator, throwing an exception if the stream is empty.
     * 
     * <p>This collector processes the stream to find both extremes in a single pass.
     * If the stream is empty, a {@code NoSuchElementException} is thrown.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find shortest and longest strings (throws if empty)
     * Pair<String, String> result = Stream.of("a", "abc", "ab")
     *     .collect(Collectors.minMaxOrElseThrow(
     *         Comparator.comparing(String::length)));
     * // Result: Pair(a, abc)
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param comparator comparator used to compare elements
     * @return a {@code Collector} which finds the minimum and maximum elements
     * @throws NoSuchElementException if the stream is empty
     */
    public static <T> Collector<T, ?, Pair<T, T>> minMaxOrElseThrow(final Comparator<? super T> comparator) {
        return MoreCollectors.combine(Collectors.minOrElseThrow(comparator), Collectors.maxOrElseThrow(comparator), Fn.pair());
    }

    @SuppressWarnings("unchecked")
    static <T> T none() { //NOSONAR
        return (T) NONE;
    }

    /**
     * Returns a {@code Collector} that produces the sum of an integer-valued function
     * applied to the input elements.
     * 
     * <p>This collector sums up integer values extracted from the stream elements.
     * If no elements are present, the result is 0. The sum is computed using
     * integer arithmetic, which may overflow for large values.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Sum the lengths of all strings
     * Integer totalLength = Stream.of("hello", "world", "!")
     *     .collect(Collectors.summingInt(String::length));
     * // Result: 11
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param mapper function to extract an integer value from an element
     * @return a {@code Collector} that produces the sum of the extracted values
     */
    public static <T> Collector<T, ?, Integer> summingInt(final ToIntFunction<? super T> mapper) {
        final BiConsumer<int[], T> accumulator = (a, t) -> a[0] += mapper.applyAsInt(t);

        return create(SummingInt_Supplier, accumulator, SummingInt_Combiner, SummingInt_Finisher, CH_UNORDERED_NOID);
    }

    /**
     * Returns a {@code Collector} that produces the sum of an integer-valued function
     * applied to the input elements, with the result as a {@code Long}.
     * 
     * <p>This collector is similar to {@link #summingInt(ToIntFunction)} but returns
     * a {@code Long} result to avoid integer overflow issues when summing large
     * numbers of elements or large values.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Sum large numbers safely
     * Long total = Stream.of(1_000_000, 2_000_000, 3_000_000)
     *     .collect(Collectors.summingIntToLong(i -> i));
     * // Result: 6000000L
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param mapper function to extract an integer value from an element
     * @return a {@code Collector} that produces the sum as a {@code Long}
     */
    public static <T> Collector<T, ?, Long> summingIntToLong(final ToIntFunction<? super T> mapper) {
        final BiConsumer<long[], T> accumulator = (a, t) -> a[0] += mapper.applyAsInt(t);

        return create(SummingIntToLong_Supplier, accumulator, SummingIntToLong_Combiner, SummingIntToLong_Finisher, CH_UNORDERED_NOID);
    }

    /**
     * Returns a {@code Collector} that produces the sum of a long-valued function
     * applied to the input elements.
     * 
     * <p>This collector sums up long values extracted from the stream elements.
     * If no elements are present, the result is 0L. The sum is computed using
     * long arithmetic, which may overflow for very large values.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Sum file sizes
     * Long totalSize = files.stream()
     *     .collect(Collectors.summingLong(File::length));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param mapper function to extract a long value from an element
     * @return a {@code Collector} that produces the sum of the extracted values
     */
    public static <T> Collector<T, ?, Long> summingLong(final ToLongFunction<? super T> mapper) {
        final BiConsumer<long[], T> accumulator = (a, t) -> a[0] += mapper.applyAsLong(t);

        return create(SummingLong_Supplier, accumulator, SummingLong_Combiner, SummingLong_Finisher, CH_UNORDERED_NOID);
    }

    /**
     * Returns a {@code Collector} that produces the sum of a double-valued function
     * applied to the input elements.
     *
     * <p>This collector sums up double values extracted from the stream elements.
     * If no elements are present, the result is 0.0. The sum is computed using
     * Kahan summation algorithm for improved numerical accuracy with floating-point values.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Sum prices with decimal values
     * Double totalPrice = items.stream()
     *     .collect(Collectors.summingDouble(Item::getPrice));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param mapper function to extract a double value from an element
     * @return a {@code Collector} that produces the sum of the extracted values
     */
    public static <T> Collector<T, ?, Double> summingDouble(final ToDoubleFunction<? super T> mapper) {
        final BiConsumer<KahanSummation, T> accumulator = (a, t) -> a.add(mapper.applyAsDouble(t));

        return create(SummingDouble_Supplier, accumulator, SummingDouble_Combiner, SummingDouble_Finisher, CH_UNORDERED_NOID);
    }

    /**
     * Returns a {@code Collector} that produces the sum of {@code BigInteger} values
     * extracted from the input elements.
     * 
     * <p>This collector is useful for summing very large integer values that exceed
     * the range of primitive types. If no elements are present, the result is
     * {@code BigInteger.ZERO}.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Sum very large numbers
     * BigInteger total = Stream.of("1000000000000000000", "2000000000000000000")
     *     .collect(Collectors.summingBigInteger(BigInteger::new));
     * // Result: 3000000000000000000
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param mapper function to extract a {@code BigInteger} value from an element
     * @return a {@code Collector} that produces the sum of the extracted values
     */
    public static <T> Collector<T, ?, BigInteger> summingBigInteger(final Function<? super T, BigInteger> mapper) {
        final BiConsumer<BigInteger[], T> accumulator = (a, t) -> a[0] = a[0].add(mapper.apply(t));

        return create(SummingBigInteger_Supplier, accumulator, SummingBigInteger_Combiner, SummingBigInteger_Finisher, CH_UNORDERED_NOID);
    }

    /**
     * Returns a {@code Collector} that produces the sum of {@code BigDecimal} values
     * extracted from the input elements.
     * 
     * <p>This collector is useful for precise decimal arithmetic, especially for
     * financial calculations. If no elements are present, the result is
     * {@code BigDecimal.ZERO}.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Sum monetary values precisely
     * BigDecimal totalAmount = transactions.stream()
     *     .collect(Collectors.summingBigDecimal(Transaction::getAmount));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param mapper function to extract a {@code BigDecimal} value from an element
     * @return a {@code Collector} that produces the sum of the extracted values
     */
    public static <T> Collector<T, ?, BigDecimal> summingBigDecimal(final Function<? super T, BigDecimal> mapper) {
        final BiConsumer<BigDecimal[], T> accumulator = (a, t) -> a[0] = a[0].add(mapper.apply(t));

        return create(SummingBigDecimal_Supplier, accumulator, SummingBigDecimal_Combiner, SummingBigDecimal_Finisher, CH_UNORDERED_NOID);
    }

    /**
     * Returns a {@code Collector} that produces the arithmetic mean of an integer-valued
     * function applied to the input elements.
     *
     * <p>This collector computes the average by mapping each element to an integer value
     * using the provided mapper function, then calculating the arithmetic mean of all values.
     * If no elements are present in the stream, the result is 0.0. The average is computed
     * and returned as a double-precision floating-point value to preserve fractional precision.</p>
     *
     * <p>This method delegates to the standard Java {@link java.util.stream.Collectors#averagingInt(ToIntFunction)}
     * implementation for compatibility with the Java Stream API.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Calculate average length of strings
     * Double avgLength = Stream.of("hello", "world", "!")
     *     .collect(Collectors.averagingInt(String::length));
     * // Result: 3.6666666666666665 (average of 5, 5, and 1)
     *
     * // Calculate average age of people
     * List<Person> people = Arrays.asList(
     *     new Person("Alice", 25),
     *     new Person("Bob", 30),
     *     new Person("Charlie", 35)
     * );
     * Double avgAge = people.stream()
     *     .collect(Collectors.averagingInt(Person::getAge));
     * // Result: 30.0
     *
     * // Empty stream returns 0.0
     * Double emptyAvg = Stream.<String>empty()
     *     .collect(Collectors.averagingInt(String::length));
     * // Result: 0.0
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param mapper a function extracting an integer value from each input element
     * @return a {@code Collector} that produces the arithmetic mean of the extracted integer values as a {@code Double}
     * @see #averagingIntOrEmpty(ToIntFunction)
     * @see #averagingIntOrElseThrow(ToIntFunction)
     * @see java.util.stream.Collectors#averagingInt(ToIntFunction)
     */
    public static <T> Collector<T, ?, Double> averagingInt(final ToIntFunction<? super T> mapper) {
        return java.util.stream.Collectors.averagingInt(mapper);
    }

    /**
     * Returns a {@code Collector} that produces the arithmetic mean of an integer-valued
     * function applied to the input elements, wrapped in an {@code OptionalDouble}.
     *
     * <p>This collector computes the average by mapping each element to an integer value
     * using the provided mapper function, then calculating the arithmetic mean of all values.
     * Unlike {@link #averagingInt(ToIntFunction)}, this method returns an {@code OptionalDouble}
     * which will be empty if no elements are present in the stream. This allows for safer
     * handling of empty streams without having to distinguish between a true zero average
     * and the absence of elements.</p>
     *
     * <p>The average is computed and returned as a double-precision floating-point value
     * to preserve fractional precision.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Calculate average length of strings with Optional result
     * OptionalDouble avgLength = Stream.of("hello", "world", "!")
     *     .collect(Collectors.averagingIntOrEmpty(String::length));
     * // Result: OptionalDouble[3.6666666666666665]
     * avgLength.ifPresent(avg -> System.out.println("Average: " + avg));
     *
     * // Empty stream returns empty Optional
     * OptionalDouble emptyAvg = Stream.<String>empty()
     *     .collect(Collectors.averagingIntOrEmpty(String::length));
     * // Result: OptionalDouble.empty
     * System.out.println(emptyAvg.isPresent()); // false
     *
     * // Safe handling with orElse
     * double result = Stream.of("a", "bb")
     *     .collect(Collectors.averagingIntOrEmpty(String::length))
     *     .orElse(0.0);
     * // Result: 1.5
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param mapper a function extracting an integer value from each input element
     * @return a {@code Collector} that produces an {@code OptionalDouble} containing the arithmetic mean,
     *         or an empty {@code OptionalDouble} if no elements were present
     * @see #averagingInt(ToIntFunction)
     * @see #averagingIntOrElseThrow(ToIntFunction)
     */
    public static <T> Collector<T, ?, OptionalDouble> averagingIntOrEmpty(final ToIntFunction<? super T> mapper) {
        final BiConsumer<long[], T> accumulator = (a, t) -> {
            a[0] += mapper.applyAsInt(t);
            a[1]++;
        };

        return create(AveragingInt_Supplier, accumulator, AveragingInt_Combiner, AveragingInt_Finisher_op, CH_UNORDERED_NOID);
    }

    /**
     * Returns a {@code Collector} that produces the arithmetic mean of an integer-valued
     * function applied to the input elements, throwing an exception if no elements are present.
     *
     * <p>This collector computes the average by mapping each element to an integer value
     * using the provided mapper function, then calculating the arithmetic mean of all values.
     * Unlike {@link #averagingInt(ToIntFunction)} which returns 0.0 for empty streams and
     * {@link #averagingIntOrEmpty(ToIntFunction)} which returns an empty Optional, this method
     * throws a {@code NoSuchElementException} if the stream is empty. This is useful when an
     * empty stream represents an error condition and you want to fail fast rather than return
     * a default value.</p>
     *
     * <p>The average is computed and returned as a double-precision floating-point value
     * to preserve fractional precision.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Calculate average age (throws if no people)
     * List<Person> people = Arrays.asList(
     *     new Person("Alice", 25),
     *     new Person("Bob", 30)
     * );
     * Double avgAge = people.stream()
     *     .collect(Collectors.averagingIntOrElseThrow(Person::getAge));
     * // Result: 27.5
     *
     * // Empty stream throws exception
     * try {
     *     Double avg = Stream.<Person>empty()
     *         .collect(Collectors.averagingIntOrElseThrow(Person::getAge));
     * } catch (NoSuchElementException e) {
     *     System.out.println("Cannot compute average of empty stream");
     * }
     *
     * // Using with filtering - throws if no elements match
     * Double avgAdultAge = people.stream()
     *     .filter(p -> p.getAge() >= 18)
     *     .collect(Collectors.averagingIntOrElseThrow(Person::getAge));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param mapper a function extracting an integer value from each input element
     * @return a {@code Collector} that produces the arithmetic mean of the extracted integer values as a {@code Double}
     * @throws NoSuchElementException if no elements are present in the stream
     * @see #averagingInt(ToIntFunction)
     * @see #averagingIntOrEmpty(ToIntFunction)
     */
    public static <T> Collector<T, ?, Double> averagingIntOrElseThrow(final ToIntFunction<? super T> mapper) {
        final BiConsumer<long[], T> accumulator = (a, t) -> {
            a[0] += mapper.applyAsInt(t);
            a[1]++;
        };

        return create(AveragingInt_Supplier, accumulator, AveragingInt_Combiner, AveragingInt_Finisher_orElseThrow, CH_UNORDERED_NOID);
    }

    /**
     * Returns a {@code Collector} that produces the arithmetic mean of a long-valued
     * function applied to the input elements.
     *
     * <p>This collector computes the average by mapping each element to a long value
     * using the provided mapper function, then calculating the arithmetic mean of all values.
     * If no elements are present in the stream, the result is 0.0. The average is computed
     * and returned as a double-precision floating-point value to preserve fractional precision.</p>
     *
     * <p>This method delegates to the standard Java {@link java.util.stream.Collectors#averagingLong(ToLongFunction)}
     * implementation for compatibility with the Java Stream API.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Calculate average file size in bytes
     * List<File> files = Arrays.asList(
     *     new File("file1.txt"),
     *     new File("file2.txt"),
     *     new File("file3.txt")
     * );
     * Double avgSize = files.stream()
     *     .collect(Collectors.averagingLong(File::length));
     *
     * // Calculate average timestamp values
     * List<Event> events = getEvents();
     * Double avgTimestamp = events.stream()
     *     .collect(Collectors.averagingLong(Event::getTimestamp));
     *
     * // Empty stream returns 0.0
     * Double emptyAvg = Stream.<File>empty()
     *     .collect(Collectors.averagingLong(File::length));
     * // Result: 0.0
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param mapper a function extracting a long value from each input element
     * @return a {@code Collector} that produces the arithmetic mean of the extracted long values as a {@code Double}
     * @see #averagingLongOrEmpty(ToLongFunction)
     * @see #averagingLongOrElseThrow(ToLongFunction)
     * @see java.util.stream.Collectors#averagingLong(ToLongFunction)
     */
    public static <T> Collector<T, ?, Double> averagingLong(final ToLongFunction<? super T> mapper) {
        return java.util.stream.Collectors.averagingLong(mapper);
    }

    /**
     * Returns a {@code Collector} that produces the arithmetic mean of a long-valued
     * function applied to the input elements, wrapped in an {@code OptionalDouble}.
     *
     * <p>This collector computes the average by mapping each element to a long value
     * using the provided mapper function, then calculating the arithmetic mean of all values.
     * Unlike {@link #averagingLong(ToLongFunction)}, this method returns an {@code OptionalDouble}
     * which will be empty if no elements are present in the stream. This allows for safer
     * handling of empty streams without having to distinguish between a true zero average
     * and the absence of elements.</p>
     *
     * <p>The average is computed and returned as a double-precision floating-point value
     * to preserve fractional precision.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Calculate average file size with Optional result
     * List<File> files = Arrays.asList(
     *     new File("file1.txt"),
     *     new File("file2.txt")
     * );
     * OptionalDouble avgSize = files.stream()
     *     .collect(Collectors.averagingLongOrEmpty(File::length));
     * avgSize.ifPresent(size -> System.out.println("Average size: " + size));
     *
     * // Empty stream returns empty Optional
     * OptionalDouble emptyAvg = Stream.<File>empty()
     *     .collect(Collectors.averagingLongOrEmpty(File::length));
     * System.out.println(emptyAvg.isPresent()); // false
     *
     * // Safe handling with orElse
     * double result = files.stream()
     *     .collect(Collectors.averagingLongOrEmpty(File::length))
     *     .orElse(0.0);
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param mapper a function extracting a long value from each input element
     * @return a {@code Collector} that produces an {@code OptionalDouble} containing the arithmetic mean,
     *         or an empty {@code OptionalDouble} if no elements were present
     * @see #averagingLong(ToLongFunction)
     * @see #averagingLongOrElseThrow(ToLongFunction)
     */
    public static <T> Collector<T, ?, OptionalDouble> averagingLongOrEmpty(final ToLongFunction<? super T> mapper) {
        final BiConsumer<long[], T> accumulator = (a, t) -> {
            a[0] += mapper.applyAsLong(t);
            a[1]++;
        };

        return create(AveragingLong_Supplier, accumulator, AveragingLong_Combiner, AveragingLong_Finisher_op, CH_UNORDERED_NOID);
    }

    /**
     * Returns a {@code Collector} that produces the arithmetic mean of a long-valued
     * function applied to the input elements, throwing an exception if no elements are present.
     *
     * <p>This collector computes the average by mapping each element to a long value
     * using the provided mapper function, then calculating the arithmetic mean of all values.
     * Unlike {@link #averagingLong(ToLongFunction)} which returns 0.0 for empty streams and
     * {@link #averagingLongOrEmpty(ToLongFunction)} which returns an empty Optional, this method
     * throws a {@code NoSuchElementException} if the stream is empty. This is useful when an
     * empty stream represents an error condition and you want to fail fast rather than return
     * a default value.</p>
     *
     * <p>The average is computed and returned as a double-precision floating-point value
     * to preserve fractional precision.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Calculate average processing time (throws if no data)
     * List<Record> records = getRecords();
     * Double avgTime = records.stream()
     *     .collect(Collectors.averagingLongOrElseThrow(Record::getProcessingTime));
     *
     * // Empty stream throws exception
     * try {
     *     Double avg = Stream.<Record>empty()
     *         .collect(Collectors.averagingLongOrElseThrow(Record::getProcessingTime));
     * } catch (NoSuchElementException e) {
     *     System.out.println("Cannot compute average of empty stream");
     * }
     *
     * // Using with filtering - throws if no elements match
     * Double avgLargeFileSize = files.stream()
     *     .filter(f -> f.length() > 1000000)
     *     .collect(Collectors.averagingLongOrElseThrow(File::length));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param mapper a function extracting a long value from each input element
     * @return a {@code Collector} that produces the arithmetic mean of the extracted long values as a {@code Double}
     * @throws NoSuchElementException if no elements are present in the stream
     * @see #averagingLong(ToLongFunction)
     * @see #averagingLongOrEmpty(ToLongFunction)
     */
    public static <T> Collector<T, ?, Double> averagingLongOrElseThrow(final ToLongFunction<? super T> mapper) {
        final BiConsumer<long[], T> accumulator = (a, t) -> {
            a[0] += mapper.applyAsLong(t);
            a[1]++;
        };

        return create(AveragingLong_Supplier, accumulator, AveragingLong_Combiner, AveragingLong_Finisher_orElseThrow, CH_UNORDERED_NOID);
    }

    /**
     * Returns a {@code Collector} that produces the arithmetic mean of a double-valued
     * function applied to the input elements.
     *
     * <p>This collector computes the average by mapping each element to a double value
     * using the provided mapper function, then calculating the arithmetic mean of all values.
     * If no elements are present in the stream, the result is 0.0. The average is computed
     * and returned as a double-precision floating-point value.</p>
     *
     * <p>This method delegates to the standard Java {@link java.util.stream.Collectors#averagingDouble(ToDoubleFunction)}
     * implementation for compatibility with the Java Stream API.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Calculate average price of products
     * List<Product> products = Arrays.asList(
     *     new Product("Laptop", 999.99),
     *     new Product("Mouse", 29.99),
     *     new Product("Keyboard", 79.99)
     * );
     * Double avgPrice = products.stream()
     *     .collect(Collectors.averagingDouble(Product::getPrice));
     * // Result: 369.99
     *
     * // Calculate average score
     * List<Double> scores = Arrays.asList(85.5, 90.0, 78.3, 92.7);
     * Double avgScore = scores.stream()
     *     .collect(Collectors.averagingDouble(d -> d));
     * // Result: 86.625
     *
     * // Empty stream returns 0.0
     * Double emptyAvg = Stream.<Product>empty()
     *     .collect(Collectors.averagingDouble(Product::getPrice));
     * // Result: 0.0
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param mapper a function extracting a double value from each input element
     * @return a {@code Collector} that produces the arithmetic mean of the extracted double values as a {@code Double}
     * @see #averagingDoubleOrEmpty(ToDoubleFunction)
     * @see #averagingDoubleOrElseThrow(ToDoubleFunction)
     * @see java.util.stream.Collectors#averagingDouble(ToDoubleFunction)
     */
    public static <T> Collector<T, ?, Double> averagingDouble(final ToDoubleFunction<? super T> mapper) {
        return java.util.stream.Collectors.averagingDouble(mapper);
    }

    /**
     * Returns a {@code Collector} that produces the arithmetic mean of a double-valued
     * function applied to the input elements, wrapped in an {@code OptionalDouble}.
     *
     * <p>This collector computes the average by mapping each element to a double value
     * using the provided mapper function, then calculating the arithmetic mean of all values.
     * Unlike {@link #averagingDouble(ToDoubleFunction)}, this method returns an {@code OptionalDouble}
     * which will be empty if no elements are present in the stream. This allows for safer
     * handling of empty streams without having to distinguish between a true zero average
     * and the absence of elements.</p>
     *
     * <p>The average is computed and returned as a double-precision floating-point value.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Calculate average price with Optional result
     * List<Product> products = Arrays.asList(
     *     new Product("Laptop", 999.99),
     *     new Product("Mouse", 29.99)
     * );
     * OptionalDouble avgPrice = products.stream()
     *     .collect(Collectors.averagingDoubleOrEmpty(Product::getPrice));
     * avgPrice.ifPresent(price -> System.out.println("Average: $" + price));
     * // Output: Average: $514.99
     *
     * // Empty stream returns empty Optional
     * OptionalDouble emptyAvg = Stream.<Product>empty()
     *     .collect(Collectors.averagingDoubleOrEmpty(Product::getPrice));
     * System.out.println(emptyAvg.isPresent()); // false
     *
     * // Safe handling with orElse
     * double result = products.stream()
     *     .collect(Collectors.averagingDoubleOrEmpty(Product::getPrice))
     *     .orElse(0.0);
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param mapper a function extracting a double value from each input element
     * @return a {@code Collector} that produces an {@code OptionalDouble} containing the arithmetic mean,
     *         or an empty {@code OptionalDouble} if no elements were present
     * @see #averagingDouble(ToDoubleFunction)
     * @see #averagingDoubleOrElseThrow(ToDoubleFunction)
     */
    public static <T> Collector<T, ?, OptionalDouble> averagingDoubleOrEmpty(final ToDoubleFunction<? super T> mapper) {
        final BiConsumer<KahanSummation, T> accumulator = (a, t) -> a.add(mapper.applyAsDouble(t));

        return create(AveragingDouble_Supplier, accumulator, AveragingDouble_Combiner, AveragingDouble_Finisher_op, CH_UNORDERED_NOID);
    }

    /**
     * Returns a {@code Collector} that produces the arithmetic mean of a double-valued
     * function applied to the input elements, throwing an exception if no elements are present.
     *
     * <p>This collector computes the average by mapping each element to a double value
     * using the provided mapper function, then calculating the arithmetic mean of all values.
     * Unlike {@link #averagingDouble(ToDoubleFunction)} which returns 0.0 for empty streams and
     * {@link #averagingDoubleOrEmpty(ToDoubleFunction)} which returns an empty Optional, this method
     * throws a {@code NoSuchElementException} if the stream is empty. This is useful when an
     * empty stream represents an error condition and you want to fail fast rather than return
     * a default value.</p>
     *
     * <p>The average is computed and returned as a double-precision floating-point value.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Calculate average score (throws if no scores)
     * List<Student> students = Arrays.asList(
     *     new Student("Alice", 85.5),
     *     new Student("Bob", 92.0),
     *     new Student("Charlie", 78.3)
     * );
     * Double avgScore = students.stream()
     *     .collect(Collectors.averagingDoubleOrElseThrow(Student::getScore));
     * // Result: 85.26666666666667
     *
     * // Empty stream throws exception
     * try {
     *     Double avg = Stream.<Student>empty()
     *         .collect(Collectors.averagingDoubleOrElseThrow(Student::getScore));
     * } catch (NoSuchElementException e) {
     *     System.out.println("Cannot compute average of empty stream");
     * }
     *
     * // Using with filtering - throws if no elements match
     * Double avgHighScore = students.stream()
     *     .filter(s -> s.getScore() >= 90.0)
     *     .collect(Collectors.averagingDoubleOrElseThrow(Student::getScore));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param mapper a function extracting a double value from each input element
     * @return a {@code Collector} that produces the arithmetic mean of the extracted double values as a {@code Double}
     * @throws NoSuchElementException if no elements are present in the stream
     * @see #averagingDouble(ToDoubleFunction)
     * @see #averagingDoubleOrEmpty(ToDoubleFunction)
     */
    public static <T> Collector<T, ?, Double> averagingDoubleOrElseThrow(final ToDoubleFunction<? super T> mapper) {
        final BiConsumer<KahanSummation, T> accumulator = (a, t) -> a.add(mapper.applyAsDouble(t));

        return create(AveragingDouble_Supplier, accumulator, AveragingDouble_Combiner, AveragingDouble_Finisher_orElseThrow, CH_UNORDERED_NOID);
    }

    /**
     * Returns a {@code Collector} that produces the arithmetic mean of {@code BigInteger}
     * values extracted from the input elements.
     *
     * <p>This collector computes the average by mapping each element to a {@code BigInteger} value
     * using the provided mapper function, then calculating the arithmetic mean of all values.
     * The average is calculated as a {@code BigDecimal} to preserve precision, which is essential
     * when working with arbitrary-precision integers. If no elements are present in the stream,
     * the result is {@code BigDecimal.ZERO}.</p>
     *
     * <p>This collector is particularly useful when dealing with very large integer values that
     * exceed the capacity of primitive types (int, long) or when precision is critical.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Calculate average of very large numbers
     * BigDecimal avg = Stream.of("1000000000000000000", "2000000000000000000", "3000000000000000000")
     *     .map(BigInteger::new)
     *     .collect(Collectors.averagingBigInteger(n -> n));
     * // Result: 2000000000000000000
     *
     * // Average account balances with precise arithmetic
     * List<Account> accounts = getAccounts();
     * BigDecimal avgBalance = accounts.stream()
     *     .collect(Collectors.averagingBigInteger(Account::getBalanceBigInt));
     *
     * // Empty stream returns BigDecimal.ZERO
     * BigDecimal emptyAvg = Stream.<BigInteger>empty()
     *     .collect(Collectors.averagingBigInteger(n -> n));
     * // Result: BigDecimal.ZERO
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param mapper a function extracting a {@code BigInteger} value from each input element
     * @return a {@code Collector} that produces the arithmetic mean as a {@code BigDecimal}
     * @see #averagingBigIntegerOrEmpty(Function)
     * @see #averagingBigIntegerOrElseThrow(Function)
     * @see java.util.stream.Collectors#averagingDouble(ToDoubleFunction)
     */
    public static <T> Collector<T, ?, BigDecimal> averagingBigInteger(final Function<? super T, BigInteger> mapper) {
        final BiConsumer<Pair<BigInteger, long[]>, T> accumulator = (a, t) -> {
            a.setLeft(a.left().add(mapper.apply(t)));
            a.right()[0] += 1;
        };

        return create(AveragingBigInteger_Supplier, accumulator, AveragingBigInteger_Combiner, AveragingBigInteger_Finisher, CH_UNORDERED_NOID);
    }

    /**
     * Returns a {@code Collector} that produces the arithmetic mean of {@code BigInteger}
     * values extracted from the input elements, wrapped in an {@code Optional<BigDecimal>}.
     *
     * <p>This collector computes the average by mapping each element to a {@code BigInteger} value
     * using the provided mapper function, then calculating the arithmetic mean of all values.
     * Unlike {@link #averagingBigInteger(Function)}, this method returns an {@code Optional<BigDecimal>}
     * which will be empty if no elements are present in the stream. This allows for safer handling
     * of empty streams without having to distinguish between a true zero average and the absence
     * of elements.</p>
     *
     * <p>The average is calculated as a {@code BigDecimal} to preserve precision.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Calculate average of very large numbers with Optional result
     * Optional<BigDecimal> avg = Stream.of("1000000000000000000", "2000000000000000000")
     *     .map(BigInteger::new)
     *     .collect(Collectors.averagingBigIntegerOrEmpty(n -> n));
     * avg.ifPresent(a -> System.out.println("Average: " + a));
     * // Output: Average: 1500000000000000000
     *
     * // Empty stream returns empty Optional
     * Optional<BigDecimal> emptyAvg = Stream.<BigInteger>empty()
     *     .collect(Collectors.averagingBigIntegerOrEmpty(n -> n));
     * System.out.println(emptyAvg.isPresent()); // false
     *
     * // Safe handling with orElse
     * BigDecimal result = Stream.of(BigInteger.TEN, BigInteger.valueOf(20))
     *     .collect(Collectors.averagingBigIntegerOrEmpty(n -> n))
     *     .orElse(BigDecimal.ZERO);
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param mapper a function extracting a {@code BigInteger} value from each input element
     * @return a {@code Collector} that produces an {@code Optional<BigDecimal>} containing the arithmetic mean,
     *         or an empty {@code Optional} if no elements were present
     * @see #averagingBigInteger(Function)
     * @see #averagingBigIntegerOrElseThrow(Function)
     */
    public static <T> Collector<T, ?, Optional<BigDecimal>> averagingBigIntegerOrEmpty(final Function<? super T, BigInteger> mapper) {
        final BiConsumer<Pair<BigInteger, long[]>, T> accumulator = (a, t) -> {
            a.setLeft(a.left().add(mapper.apply(t)));
            a.right()[0] += 1;
        };

        return create(AveragingBigInteger_Supplier, accumulator, AveragingBigInteger_Combiner, AveragingBigInteger_Finisher_op, CH_UNORDERED_NOID);
    }

    /**
     * Returns a {@code Collector} that produces the arithmetic mean of {@code BigInteger}
     * values extracted from the input elements, throwing an exception if no elements are present.
     *
     * <p>This collector computes the average by mapping each element to a {@code BigInteger} value
     * using the provided mapper function, then calculating the arithmetic mean of all values.
     * Unlike {@link #averagingBigInteger(Function)} which returns {@code BigDecimal.ZERO} for empty streams
     * and {@link #averagingBigIntegerOrEmpty(Function)} which returns an empty Optional, this method
     * throws a {@code NoSuchElementException} if the stream is empty. This is useful when an empty
     * stream represents an error condition and you want to fail fast rather than return a default value.</p>
     *
     * <p>The average is calculated as a {@code BigDecimal} to preserve precision.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Calculate average balance (throws if no accounts)
     * List<Account> accounts = getAccounts();
     * BigDecimal avgBalance = accounts.stream()
     *     .collect(Collectors.averagingBigIntegerOrElseThrow(Account::getBalanceBigInt));
     *
     * // Empty stream throws exception
     * try {
     *     BigDecimal avg = Stream.<Account>empty()
     *         .collect(Collectors.averagingBigIntegerOrElseThrow(Account::getBalanceBigInt));
     * } catch (NoSuchElementException e) {
     *     System.out.println("Cannot compute average of empty stream");
     * }
     *
     * // Using with filtering - throws if no elements match
     * BigDecimal avgLargeBalance = accounts.stream()
     *     .filter(a -> a.getBalanceBigInt().compareTo(BigInteger.valueOf(1000000)) > 0)
     *     .collect(Collectors.averagingBigIntegerOrElseThrow(Account::getBalanceBigInt));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param mapper a function extracting a {@code BigInteger} value from each input element
     * @return a {@code Collector} that produces the arithmetic mean as a {@code BigDecimal}
     * @throws NoSuchElementException if no elements are present in the stream
     * @see #averagingBigInteger(Function)
     * @see #averagingBigIntegerOrEmpty(Function)
     */
    public static <T> Collector<T, ?, BigDecimal> averagingBigIntegerOrElseThrow(final Function<? super T, BigInteger> mapper) {
        final BiConsumer<Pair<BigInteger, long[]>, T> accumulator = (a, t) -> {
            a.setLeft(a.left().add(mapper.apply(t)));
            a.right()[0] += 1;
        };

        return create(AveragingBigInteger_Supplier, accumulator, AveragingBigInteger_Combiner, AveragingBigInteger_Finisher_orElseThrow, CH_UNORDERED_NOID);
    }

    /**
     * Returns a {@code Collector} that produces the arithmetic mean of {@code BigDecimal}
     * values extracted from the input elements.
     *
     * <p>This collector computes the average by mapping each element to a {@code BigDecimal} value
     * using the provided mapper function, then calculating the arithmetic mean of all values.
     * The result is returned as a {@code BigDecimal} to preserve maximum precision. If no elements
     * are present in the stream, the result is {@code BigDecimal.ZERO}.</p>
     *
     * <p>This collector is particularly useful for precise decimal arithmetic, especially in
     * financial calculations, scientific computations, or any scenario where precision loss
     * from floating-point arithmetic is unacceptable.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Calculate average transaction amount with precise decimal arithmetic
     * List<Transaction> transactions = Arrays.asList(
     *     new Transaction(new BigDecimal("100.50")),
     *     new Transaction(new BigDecimal("250.75")),
     *     new Transaction(new BigDecimal("175.25"))
     * );
     * BigDecimal avgAmount = transactions.stream()
     *     .collect(Collectors.averagingBigDecimal(Transaction::getAmount));
     * // Result: 175.50 (precise)
     *
     * // Average prices with full precision
     * List<BigDecimal> prices = Arrays.asList(
     *     new BigDecimal("19.99"),
     *     new BigDecimal("29.99"),
     *     new BigDecimal("39.99")
     * );
     * BigDecimal avgPrice = prices.stream()
     *     .collect(Collectors.averagingBigDecimal(p -> p));
     * // Result: 29.99 (exact)
     *
     * // Empty stream returns BigDecimal.ZERO
     * BigDecimal emptyAvg = Stream.<BigDecimal>empty()
     *     .collect(Collectors.averagingBigDecimal(d -> d));
     * // Result: BigDecimal.ZERO
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param mapper a function extracting a {@code BigDecimal} value from each input element
     * @return a {@code Collector} that produces the arithmetic mean as a {@code BigDecimal}
     * @see #averagingBigDecimalOrEmpty(Function)
     * @see #averagingBigDecimalOrElseThrow(Function)
     * @see java.util.stream.Collectors#averagingDouble(ToDoubleFunction)
     */
    public static <T> Collector<T, ?, BigDecimal> averagingBigDecimal(final Function<? super T, BigDecimal> mapper) {
        final BiConsumer<Pair<BigDecimal, long[]>, T> accumulator = (a, t) -> {
            a.setLeft(a.left().add(mapper.apply(t)));
            a.right()[0] += 1;
        };

        return create(AveragingBigDecimal_Supplier, accumulator, AveragingBigDecimal_Combiner, AveragingBigDecimal_Finisher, CH_UNORDERED_NOID);
    }

    /**
     * Returns a {@code Collector} that produces the arithmetic mean of {@code BigDecimal}
     * values extracted from the input elements, wrapped in an {@code Optional<BigDecimal>}.
     *
     * <p>This collector computes the average by mapping each element to a {@code BigDecimal} value
     * using the provided mapper function, then calculating the arithmetic mean of all values.
     * Unlike {@link #averagingBigDecimal(Function)}, this method returns an {@code Optional<BigDecimal>}
     * which will be empty if no elements are present in the stream. This allows for safer handling
     * of empty streams without having to distinguish between a true zero average and the absence
     * of elements.</p>
     *
     * <p>This collector is particularly useful for precise decimal arithmetic in scenarios where
     * empty inputs are valid and need to be handled distinctly.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Calculate average transaction amount with Optional result
     * List<Transaction> transactions = Arrays.asList(
     *     new Transaction(new BigDecimal("100.50")),
     *     new Transaction(new BigDecimal("250.75"))
     * );
     * Optional<BigDecimal> avgAmount = transactions.stream()
     *     .collect(Collectors.averagingBigDecimalOrEmpty(Transaction::getAmount));
     * avgAmount.ifPresent(amt -> System.out.println("Average: $" + amt));
     * // Output: Average: $175.625
     *
     * // Empty stream returns empty Optional
     * Optional<BigDecimal> emptyAvg = Stream.<Transaction>empty()
     *     .collect(Collectors.averagingBigDecimalOrEmpty(Transaction::getAmount));
     * System.out.println(emptyAvg.isPresent()); // false
     *
     * // Safe handling with orElse
     * BigDecimal result = transactions.stream()
     *     .collect(Collectors.averagingBigDecimalOrEmpty(Transaction::getAmount))
     *     .orElse(BigDecimal.ZERO);
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param mapper a function extracting a {@code BigDecimal} value from each input element
     * @return a {@code Collector} that produces an {@code Optional<BigDecimal>} containing the arithmetic mean,
     *         or an empty {@code Optional} if no elements were present
     * @see #averagingBigDecimal(Function)
     * @see #averagingBigDecimalOrElseThrow(Function)
     */
    public static <T> Collector<T, ?, Optional<BigDecimal>> averagingBigDecimalOrEmpty(final Function<? super T, BigDecimal> mapper) {
        final BiConsumer<Pair<BigDecimal, long[]>, T> accumulator = (a, t) -> {
            a.setLeft(a.left().add(mapper.apply(t)));
            a.right()[0] += 1;
        };

        return create(AveragingBigDecimal_Supplier, accumulator, AveragingBigDecimal_Combiner, AveragingBigDecimal_Finisher_op, CH_UNORDERED_NOID);
    }

    /**
     * Returns a {@code Collector} that produces the arithmetic mean of {@code BigDecimal}
     * values extracted from the input elements, throwing an exception if no elements are present.
     *
     * <p>This collector computes the average by mapping each element to a {@code BigDecimal} value
     * using the provided mapper function, then calculating the arithmetic mean of all values.
     * Unlike {@link #averagingBigDecimal(Function)} which returns {@code BigDecimal.ZERO} for empty streams
     * and {@link #averagingBigDecimalOrEmpty(Function)} which returns an empty Optional, this method
     * throws a {@code NoSuchElementException} if the stream is empty. This is useful when an empty
     * stream represents an error condition and you want to fail fast rather than return a default value.</p>
     *
     * <p>This collector is particularly useful for precise decimal arithmetic in scenarios where
     * the presence of data is a precondition.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Calculate average price (throws if no items)
     * List<Item> items = getItems();
     * BigDecimal avgPrice = items.stream()
     *     .collect(Collectors.averagingBigDecimalOrElseThrow(Item::getPrice));
     *
     * // Empty stream throws exception
     * try {
     *     BigDecimal avg = Stream.<Item>empty()
     *         .collect(Collectors.averagingBigDecimalOrElseThrow(Item::getPrice));
     * } catch (NoSuchElementException e) {
     *     System.out.println("Cannot compute average of empty stream");
     * }
     *
     * // Using with filtering - throws if no elements match
     * BigDecimal avgExpensivePrice = items.stream()
     *     .filter(i -> i.getPrice().compareTo(new BigDecimal("100")) > 0)
     *     .collect(Collectors.averagingBigDecimalOrElseThrow(Item::getPrice));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param mapper a function extracting a {@code BigDecimal} value from each input element
     * @return a {@code Collector} that produces the arithmetic mean as a {@code BigDecimal}
     * @throws NoSuchElementException if no elements are present in the stream
     * @see #averagingBigDecimal(Function)
     * @see #averagingBigDecimalOrEmpty(Function)
     */
    public static <T> Collector<T, ?, BigDecimal> averagingBigDecimalOrElseThrow(final Function<? super T, BigDecimal> mapper) {
        final BiConsumer<Pair<BigDecimal, long[]>, T> accumulator = (a, t) -> {
            a.setLeft(a.left().add(mapper.apply(t)));
            a.right()[0] += 1;
        };

        return create(AveragingBigDecimal_Supplier, accumulator, AveragingBigDecimal_Combiner, AveragingBigDecimal_Finisher_orElseThrow, CH_UNORDERED_NOID);
    }

    /**
     * Returns a {@code Collector} that produces summary statistics for char values
     * extracted from the input elements.
     *
     * <p>This collector computes comprehensive statistics by mapping each element to a char value
     * using the provided mapper function. The resulting {@code CharSummaryStatistics} object contains
     * the count, sum, minimum value, maximum value, and average of all char values encountered.
     * This is particularly useful for analyzing character data and obtaining multiple statistical
     * measures in a single, efficient pass through the stream.</p>
     *
     * <p>The collector is unordered and can be used efficiently in parallel streams.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get statistics of first characters in words
     * List<String> words = Arrays.asList("apple", "banana", "cherry", "date");
     * CharSummaryStatistics stats = words.stream()
     *     .collect(Collectors.summarizingChar(s -> s.charAt(0)));
     * System.out.println("Count: " + stats.getCount());       // 4
     * System.out.println("Min: " + stats.getMin());           // 'a'
     * System.out.println("Max: " + stats.getMax());           // 'd'
     * System.out.println("Average: " + stats.getAverage());   // average of char values
     *
     * // Analyze character codes
     * String text = "Hello";
     * CharSummaryStatistics charStats = text.chars()
     *     .mapToObj(c -> (char) c)
     *     .collect(Collectors.summarizingChar(c -> c));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param mapper a function extracting a char value from each input element
     * @return a {@code Collector} that produces {@code CharSummaryStatistics} containing comprehensive statistics
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    public static <T> Collector<T, ?, CharSummaryStatistics> summarizingChar(final ToCharFunction<? super T> mapper) { // NOSONAR
        final Supplier<CharSummaryStatistics> supplier = SummarizingChar_Supplier;

        final BiConsumer<CharSummaryStatistics, T> accumulator = (a, t) -> a.accept(mapper.applyAsChar(t));

        final BinaryOperator<CharSummaryStatistics> combiner = SummarizingChar_Combiner;

        return create(supplier, accumulator, combiner, CH_UNORDERED_ID);
    }

    /**
     * Returns a {@code Collector} that produces summary statistics for byte values
     * extracted from the input elements.
     *
     * <p>This collector computes comprehensive statistics by mapping each element to a byte value
     * using the provided mapper function. The resulting {@code ByteSummaryStatistics} object contains
     * the count, sum, minimum value, maximum value, and average of all byte values encountered.
     * This is useful for analyzing byte data and obtaining multiple statistical measures in a single,
     * efficient pass through the stream.</p>
     *
     * <p>The collector is unordered and can be used efficiently in parallel streams.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get statistics of byte values
     * List<Byte> bytes = Arrays.asList((byte)1, (byte)5, (byte)3, (byte)9, (byte)2);
     * ByteSummaryStatistics stats = bytes.stream()
     *     .collect(Collectors.summarizingByte(b -> b));
     * System.out.println("Count: " + stats.getCount());       // 5
     * System.out.println("Sum: " + stats.getSum());           // 20
     * System.out.println("Min: " + stats.getMin());           // 1
     * System.out.println("Max: " + stats.getMax());           // 9
     * System.out.println("Average: " + stats.getAverage());   // 4.0
     *
     * // Analyze byte array data
     * byte[] data = {10, 20, 30, 40, 50};
     * ByteSummaryStatistics byteStats = IntStream.range(0, data.length)
     *     .mapToObj(i -> data[i])
     *     .collect(Collectors.summarizingByte(b -> b));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param mapper a function extracting a byte value from each input element
     * @return a {@code Collector} that produces {@code ByteSummaryStatistics} containing comprehensive statistics
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    public static <T> Collector<T, ?, ByteSummaryStatistics> summarizingByte(final ToByteFunction<? super T> mapper) { // NOSONAR
        final Supplier<ByteSummaryStatistics> supplier = SummarizingByte_Supplier;

        final BiConsumer<ByteSummaryStatistics, T> accumulator = (a, t) -> a.accept(mapper.applyAsByte(t));

        final BinaryOperator<ByteSummaryStatistics> combiner = SummarizingByte_Combiner;

        return create(supplier, accumulator, combiner, CH_UNORDERED_ID);
    }

    /**
     * Returns a {@code Collector} that produces summary statistics for short values
     * extracted from the input elements.
     *
     * <p>This collector computes comprehensive statistics by mapping each element to a short value
     * using the provided mapper function. The resulting {@code ShortSummaryStatistics} object contains
     * the count, sum, minimum value, maximum value, and average of all short values encountered.
     * This is useful for analyzing short integer data and obtaining multiple statistical measures
     * in a single, efficient pass through the stream.</p>
     *
     * <p>The collector is unordered and can be used efficiently in parallel streams.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get statistics of short values
     * List<Short> shorts = Arrays.asList((short)100, (short)200, (short)300, (short)150);
     * ShortSummaryStatistics stats = shorts.stream()
     *     .collect(Collectors.summarizingShort(s -> s));
     * System.out.println("Count: " + stats.getCount());       // 4
     * System.out.println("Sum: " + stats.getSum());           // 750
     * System.out.println("Min: " + stats.getMin());           // 100
     * System.out.println("Max: " + stats.getMax());           // 300
     * System.out.println("Average: " + stats.getAverage());   // 187.5
     *
     * // Analyze port numbers
     * List<Server> servers = getServers();
     * ShortSummaryStatistics portStats = servers.stream()
     *     .collect(Collectors.summarizingShort(Server::getPort));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param mapper a function extracting a short value from each input element
     * @return a {@code Collector} that produces {@code ShortSummaryStatistics} containing comprehensive statistics
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    public static <T> Collector<T, ?, ShortSummaryStatistics> summarizingShort(final ToShortFunction<? super T> mapper) { // NOSONAR
        final Supplier<ShortSummaryStatistics> supplier = SummarizingShort_Supplier;

        final BiConsumer<ShortSummaryStatistics, T> accumulator = (a, t) -> a.accept(mapper.applyAsShort(t));

        final BinaryOperator<ShortSummaryStatistics> combiner = SummarizingShort_Combiner;

        return create(supplier, accumulator, combiner, CH_UNORDERED_ID);
    }

    /**
     * Returns a {@code Collector} that produces summary statistics for integer values
     * extracted from the input elements.
     *
     * <p>This collector computes comprehensive statistics by mapping each element to an integer value
     * using the provided mapper function. The resulting {@code IntSummaryStatistics} object contains
     * the count, sum, minimum value, maximum value, and average of all integer values encountered.
     * This is useful for analyzing integer data and obtaining multiple statistical measures in a single,
     * efficient pass through the stream.</p>
     *
     * <p>The collector is unordered and can be used efficiently in parallel streams.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get statistics of string lengths
     * List<String> strings = Arrays.asList("a", "abc", "ab", "abcde");
     * IntSummaryStatistics stats = strings.stream()
     *     .collect(Collectors.summarizingInt(String::length));
     * System.out.println("Count: " + stats.getCount());       // 4
     * System.out.println("Sum: " + stats.getSum());           // 11
     * System.out.println("Min: " + stats.getMin());           // 1
     * System.out.println("Max: " + stats.getMax());           // 5
     * System.out.println("Average: " + stats.getAverage());   // 2.75
     *
     * // Analyze ages
     * List<Person> people = getPeople();
     * IntSummaryStatistics ageStats = people.stream()
     *     .collect(Collectors.summarizingInt(Person::getAge));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param mapper a function extracting an integer value from each input element
     * @return a {@code Collector} that produces {@code IntSummaryStatistics} containing comprehensive statistics
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    public static <T> Collector<T, ?, IntSummaryStatistics> summarizingInt(final ToIntFunction<? super T> mapper) {
        final Supplier<IntSummaryStatistics> supplier = SummarizingInt_Supplier;

        final BiConsumer<IntSummaryStatistics, T> accumulator = (a, t) -> a.accept(mapper.applyAsInt(t));

        final BinaryOperator<IntSummaryStatistics> combiner = SummarizingInt_Combiner;

        return create(supplier, accumulator, combiner, CH_UNORDERED_ID);
    }

    /**
     * Returns a {@code Collector} that produces summary statistics for long values
     * extracted from the input elements.
     *
     * <p>This collector computes comprehensive statistics by mapping each element to a long value
     * using the provided mapper function. The resulting {@code LongSummaryStatistics} object contains
     * the count, sum, minimum value, maximum value, and average of all long values encountered.
     * This is useful for analyzing long integer data and obtaining multiple statistical measures
     * in a single, efficient pass through the stream.</p>
     *
     * <p>The collector is unordered and can be used efficiently in parallel streams.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get statistics of file sizes
     * List<File> files = Arrays.asList(
     *     new File("file1.txt"),
     *     new File("file2.txt"),
     *     new File("file3.txt")
     * );
     * LongSummaryStatistics stats = files.stream()
     *     .collect(Collectors.summarizingLong(File::length));
     * System.out.println("Total size: " + stats.getSum());
     * System.out.println("Largest file: " + stats.getMax());
     * System.out.println("Smallest file: " + stats.getMin());
     * System.out.println("Average size: " + stats.getAverage());
     *
     * // Analyze timestamps
     * List<Event> events = getEvents();
     * LongSummaryStatistics timestampStats = events.stream()
     *     .collect(Collectors.summarizingLong(Event::getTimestamp));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param mapper a function extracting a long value from each input element
     * @return a {@code Collector} that produces {@code LongSummaryStatistics} containing comprehensive statistics
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    public static <T> Collector<T, ?, LongSummaryStatistics> summarizingLong(final ToLongFunction<? super T> mapper) {
        final Supplier<LongSummaryStatistics> supplier = SummarizingLong_Supplier;

        final BiConsumer<LongSummaryStatistics, T> accumulator = (a, t) -> a.accept(mapper.applyAsLong(t));

        final BinaryOperator<LongSummaryStatistics> combiner = SummarizingLong_Combiner;

        return create(supplier, accumulator, combiner, CH_UNORDERED_ID);
    }

    /**
     * Returns a {@code Collector} that produces summary statistics for float values
     * extracted from the input elements.
     *
     * <p>This collector computes comprehensive statistics by mapping each element to a float value
     * using the provided mapper function. The resulting {@code FloatSummaryStatistics} object contains
     * the count, sum, minimum value, maximum value, and average of all float values encountered.
     * This is useful for analyzing floating-point data and obtaining multiple statistical measures
     * in a single, efficient pass through the stream.</p>
     *
     * <p>The collector is unordered and can be used efficiently in parallel streams.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get statistics of float measurements
     * List<Measurement> measurements = Arrays.asList(
     *     new Measurement(12.5f),
     *     new Measurement(15.8f),
     *     new Measurement(9.3f)
     * );
     * FloatSummaryStatistics stats = measurements.stream()
     *     .collect(Collectors.summarizingFloat(Measurement::getValue));
     * System.out.println("Count: " + stats.getCount());       // 3
     * System.out.println("Sum: " + stats.getSum());           // 37.6
     * System.out.println("Min: " + stats.getMin());           // 9.3
     * System.out.println("Max: " + stats.getMax());           // 15.8
     * System.out.println("Average: " + stats.getAverage());   // 12.533...
     *
     * // Analyze sensor readings
     * List<Float> readings = Arrays.asList(98.6f, 100.2f, 99.1f);
     * FloatSummaryStatistics readingStats = readings.stream()
     *     .collect(Collectors.summarizingFloat(f -> f));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param mapper a function extracting a float value from each input element
     * @return a {@code Collector} that produces {@code FloatSummaryStatistics} containing comprehensive statistics
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    public static <T> Collector<T, ?, FloatSummaryStatistics> summarizingFloat(final ToFloatFunction<? super T> mapper) { // NOSONAR
        final Supplier<FloatSummaryStatistics> supplier = SummarizingFloat_Supplier;

        final BiConsumer<FloatSummaryStatistics, T> accumulator = (a, t) -> a.accept(mapper.applyAsFloat(t));

        final BinaryOperator<FloatSummaryStatistics> combiner = SummarizingFloat_Combiner;

        return create(supplier, accumulator, combiner, CH_UNORDERED_ID);
    }

    /**
     * Returns a {@code Collector} that produces summary statistics for double values
     * extracted from the input elements.
     *
     * <p>This collector computes comprehensive statistics by mapping each element to a double value
     * using the provided mapper function. The resulting {@code DoubleSummaryStatistics} object contains
     * the count, sum, minimum value, maximum value, and average of all double values encountered.
     * This is useful for analyzing double-precision floating-point data and obtaining multiple
     * statistical measures in a single, efficient pass through the stream.</p>
     *
     * <p>The collector is unordered and can be used efficiently in parallel streams.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get statistics of product prices
     * List<Product> products = Arrays.asList(
     *     new Product("Laptop", 999.99),
     *     new Product("Mouse", 29.99),
     *     new Product("Keyboard", 79.99)
     * );
     * DoubleSummaryStatistics stats = products.stream()
     *     .collect(Collectors.summarizingDouble(Product::getPrice));
     * System.out.println("Count: " + stats.getCount());        // 3
     * System.out.println("Total: $" + stats.getSum());         // $1109.97
     * System.out.println("Min: $" + stats.getMin());           // $29.99
     * System.out.println("Max: $" + stats.getMax());           // $999.99
     * System.out.println("Average: $" + stats.getAverage());   // $369.99
     *
     * // Analyze test scores
     * List<Student> students = getStudents();
     * DoubleSummaryStatistics scoreStats = students.stream()
     *     .collect(Collectors.summarizingDouble(Student::getScore));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param mapper a function extracting a double value from each input element
     * @return a {@code Collector} that produces {@code DoubleSummaryStatistics} containing comprehensive statistics
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    public static <T> Collector<T, ?, DoubleSummaryStatistics> summarizingDouble(final ToDoubleFunction<? super T> mapper) {
        final Supplier<DoubleSummaryStatistics> supplier = SummarizingDouble_Supplier;

        final BiConsumer<DoubleSummaryStatistics, T> accumulator = (a, t) -> a.accept(mapper.applyAsDouble(t));

        final BinaryOperator<DoubleSummaryStatistics> combiner = SummarizingDouble_Combiner;

        return create(supplier, accumulator, combiner, CH_UNORDERED_ID);
    }

    /**
     * Returns a {@code Collector} that produces summary statistics for {@code BigInteger}
     * values extracted from the input elements.
     *
     * <p>This collector computes comprehensive statistics by mapping each element to a {@code BigInteger} value
     * using the provided mapper function. The resulting {@code BigIntegerSummaryStatistics} object contains
     * the count, sum, minimum value, maximum value, and average (as {@code BigDecimal}) of all {@code BigInteger}
     * values encountered. This is particularly useful for analyzing very large integer values that exceed the
     * capacity of primitive types and obtaining multiple statistical measures in a single, efficient pass
     * through the stream.</p>
     *
     * <p>The collector is unordered and can be used efficiently in parallel streams.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get statistics of very large numbers
     * List<String> largeNumbers = Arrays.asList(
     *     "1000000000000000000",
     *     "2000000000000000000",
     *     "3000000000000000000"
     * );
     * BigIntegerSummaryStatistics stats = largeNumbers.stream()
     *     .map(BigInteger::new)
     *     .collect(Collectors.summarizingBigInteger(n -> n));
     * System.out.println("Count: " + stats.getCount());       // 3
     * System.out.println("Sum: " + stats.getSum());           // 6000000000000000000
     * System.out.println("Min: " + stats.getMin());           // 1000000000000000000
     * System.out.println("Max: " + stats.getMax());           // 3000000000000000000
     * System.out.println("Average: " + stats.getAverage());   // 2000000000000000000
     *
     * // Analyze account balances
     * List<Account> accounts = getAccounts();
     * BigIntegerSummaryStatistics balanceStats = accounts.stream()
     *     .collect(Collectors.summarizingBigInteger(Account::getBalanceBigInt));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param mapper a function extracting a {@code BigInteger} value from each input element
     * @return a {@code Collector} that produces {@code BigIntegerSummaryStatistics} containing comprehensive statistics
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    public static <T> Collector<T, ?, BigIntegerSummaryStatistics> summarizingBigInteger(final Function<? super T, BigInteger> mapper) {
        final Supplier<BigIntegerSummaryStatistics> supplier = SummarizingBigInteger_Supplier;

        final BiConsumer<BigIntegerSummaryStatistics, T> accumulator = (a, t) -> a.accept(mapper.apply(t));

        final BinaryOperator<BigIntegerSummaryStatistics> combiner = SummarizingBigInteger_Combiner;

        return create(supplier, accumulator, combiner, CH_UNORDERED_ID);
    }

    /**
     * Returns a {@code Collector} that produces summary statistics for {@code BigDecimal}
     * values extracted from the input elements.
     *
     * <p>This collector computes comprehensive statistics by mapping each element to a {@code BigDecimal} value
     * using the provided mapper function. The resulting {@code BigDecimalSummaryStatistics} object contains
     * the count, sum, minimum value, maximum value, and average of all {@code BigDecimal} values encountered.
     * This is particularly useful for precise decimal arithmetic, especially in financial calculations, and
     * for obtaining multiple statistical measures in a single, efficient pass through the stream.</p>
     *
     * <p>The collector is unordered and can be used efficiently in parallel streams.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get statistics of monetary transaction amounts
     * List<Transaction> transactions = Arrays.asList(
     *     new Transaction(new BigDecimal("100.50")),
     *     new Transaction(new BigDecimal("250.75")),
     *     new Transaction(new BigDecimal("175.25")),
     *     new Transaction(new BigDecimal("89.99"))
     * );
     * BigDecimalSummaryStatistics stats = transactions.stream()
     *     .collect(Collectors.summarizingBigDecimal(Transaction::getAmount));
     * System.out.println("Count: " + stats.getCount());        // 4
     * System.out.println("Total: $" + stats.getSum());         // $616.49
     * System.out.println("Min: $" + stats.getMin());           // $89.99
     * System.out.println("Max: $" + stats.getMax());           // $250.75
     * System.out.println("Average: $" + stats.getAverage());   // $154.1225
     *
     * // Analyze invoice amounts
     * List<Invoice> invoices = getInvoices();
     * BigDecimalSummaryStatistics invoiceStats = invoices.stream()
     *     .collect(Collectors.summarizingBigDecimal(Invoice::getAmount));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param mapper a function extracting a {@code BigDecimal} value from each input element
     * @return a {@code Collector} that produces {@code BigDecimalSummaryStatistics} containing comprehensive statistics
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    public static <T> Collector<T, ?, BigDecimalSummaryStatistics> summarizingBigDecimal(final Function<? super T, BigDecimal> mapper) {
        final Supplier<BigDecimalSummaryStatistics> supplier = SummarizingBigDecimal_Supplier;

        final BiConsumer<BigDecimalSummaryStatistics, T> accumulator = (a, t) -> a.accept(mapper.apply(t));

        final BinaryOperator<BigDecimalSummaryStatistics> combiner = SummarizingBigDecimal_Combiner;

        return create(supplier, accumulator, combiner, CH_UNORDERED_ID);
    }

    /**
     * Returns a {@code Collector} that performs a reduction on the input elements using
     * the provided identity value and binary operator.
     *
     * <p>This collector accumulates elements by repeatedly applying the binary operator,
     * starting with the identity value. The identity value serves as both the initial accumulation
     * value and the result when no elements are present. The identity must be an identity for
     * the combiner function, meaning {@code op.apply(identity, x)} must equal {@code x} for any value {@code x}.</p>
     *
     * <p>The reduction operation is equivalent to:
     * <pre>{@code
     *     T result = identity;
     *     for (T element : stream)
     *         result = op.apply(result, element);
     *     return result;
     * }</pre>
     *
     * <p>The collector is unordered, allowing for efficient parallel processing when the binary
     * operator is associative.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Calculate product of all numbers
     * Integer product = Stream.of(1, 2, 3, 4)
     *     .collect(Collectors.reducing(1, (a, b) -> a * b));
     * // Result: 24
     *
     * // Concatenate strings with a seed value
     * String result = Stream.of("a", "b", "c")
     *     .collect(Collectors.reducing("start:", (s1, s2) -> s1 + s2));
     * // Result: "start:abc"
     *
     * // Find maximum with default value
     * Integer max = Stream.of(5, 3, 8, 1)
     *     .collect(Collectors.reducing(Integer.MIN_VALUE, Integer::max));
     * // Result: 8
     *
     * // Empty stream returns identity
     * Integer emptyProduct = Stream.<Integer>empty()
     *     .collect(Collectors.reducing(1, (a, b) -> a * b));
     * // Result: 1
     * }</pre>
     *
     * @param <T> the type of input elements and the result
     * @param identity the identity value for the reduction operation (also returned when no elements are present)
     * @param op an associative, stateless binary operator for combining two values
     * @return a {@code Collector} that reduces the input elements using the binary operator
     * @see #reducing(BinaryOperator)
     * @see #reducing(Object, Function, BinaryOperator)
     * @see java.util.stream.Collectors#reducing(Object, BinaryOperator)
     */
    public static <T> Collector<T, ?, T> reducing(final T identity, final BinaryOperator<T> op) {
        final BiConsumer<Holder<T>, T> accumulator = (a, t) -> a.setValue(op.apply(a.value(), t));

        final BinaryOperator<Holder<T>> combiner = (a, b) -> {
            a.setValue(op.apply(a.value(), b.value()));
            return a;
        };

        @SuppressWarnings("rawtypes")
        final Function<Holder<T>, T> finisher = (Function) Reducing_Finisher_0;

        return create(holderSupplier(identity), accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    /**
     * Returns a {@code Collector} that performs a reduction on the input elements using
     * the provided binary operator, with no identity value.
     *
     * <p>This collector accumulates elements by repeatedly applying the binary operator.
     * Unlike {@link #reducing(Object, BinaryOperator)}, this method does not require an identity value.
     * The first element encountered in the stream becomes the initial value for the reduction,
     * and subsequent elements are combined with it using the binary operator.</p>
     *
     * <p>If the stream is empty, an empty {@code Optional} is returned. This allows for safe
     * handling of the case where no reduction can be performed.</p>
     *
     * <p>The collector is unordered, allowing for efficient parallel processing when the binary
     * operator is associative.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find the longest string
     * Optional<String> longest = Stream.of("a", "abc", "ab")
     *     .collect(Collectors.reducing((s1, s2) ->
     *         s1.length() >= s2.length() ? s1 : s2));
     * // Result: Optional["abc"]
     *
     * // Find maximum number
     * Optional<Integer> max = Stream.of(5, 3, 8, 1, 9)
     *     .collect(Collectors.reducing(Integer::max));
     * // Result: Optional[9]
     *
     * // Concatenate all strings
     * Optional<String> concatenated = Stream.of("Hello", " ", "World")
     *     .collect(Collectors.reducing(String::concat));
     * // Result: Optional["Hello World"]
     *
     * // Empty stream returns empty Optional
     * Optional<Integer> empty = Stream.<Integer>empty()
     *     .collect(Collectors.reducing(Integer::max));
     * // Result: Optional.empty
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param op an associative, stateless binary operator for combining two values
     * @return a {@code Collector} that reduces the input elements into an {@code Optional} containing the reduced value,
     *         or an empty {@code Optional} if no elements were present
     * @see #reducing(Object, BinaryOperator)
     * @see #reducingOrElseGet(BinaryOperator, Supplier)
     * @see #reducingOrElseThrow(BinaryOperator)
     * @see java.util.stream.Collectors#reducing(BinaryOperator)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Collector<T, ?, Optional<T>> reducing(final BinaryOperator<T> op) {
        final Supplier<OptHolder<T>> supplier = () -> new OptHolder<>(op);

        final BiConsumer<OptHolder<T>, T> accumulator = (BiConsumer) Reducing_Accumulator;
        final BinaryOperator<OptHolder<T>> combiner = (BinaryOperator) Reducing_Combiner;
        final Function<OptHolder<T>, Optional<T>> finisher = (Function) Reducing_Finisher;

        return create(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    /**
     * Returns a {@code Collector} that performs a reduction on the elements using
     * the provided binary operator, returning a default value if the stream is empty.
     *
     * <p>This collector is similar to {@link #reducing(BinaryOperator)} but returns
     * the result of the supplier function instead of an empty {@code Optional} when
     * the stream is empty.</p>
     *
     * <p>The collector is unordered, allowing for efficient parallel processing when the binary
     * operator is associative.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find maximum with default
     * Integer max = Stream.<Integer>empty()
     *     .collect(Collectors.reducingOrElseGet(Integer::max, () -> 0));
     * // Result: 0
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param op binary operator used to reduce elements
     * @param supplierForEmpty supplier that provides a default value when stream is empty
     * @return a {@code Collector} that reduces elements or returns the supplied default
     */
    @SuppressWarnings("rawtypes")
    public static <T> Collector<T, ?, T> reducingOrElseGet(final BinaryOperator<T> op, final Supplier<? extends T> supplierForEmpty) {
        final Supplier<OptHolder<T>> supplier = () -> new OptHolder<>(op);

        final BiConsumer<OptHolder<T>, T> accumulator = (BiConsumer) Reducing_Accumulator;
        final BinaryOperator<OptHolder<T>> combiner = (BinaryOperator) Reducing_Combiner;

        final Function<OptHolder<T>, T> finisher = a -> a.present ? a.value : supplierForEmpty.get();

        return create(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    /**
     * Returns a {@code Collector} that performs a reduction on the elements using
     * the provided binary operator, throwing an exception if the stream is empty.
     *
     * <p>This collector is useful when an empty stream represents an error condition.
     * It guarantees that a result will be produced or an exception will be thrown.</p>
     *
     * <p>The collector is unordered, allowing for efficient parallel processing when the binary
     * operator is associative.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find minimum (throws if empty)
     * Integer min = Stream.of(5, 2, 8, 1)
     *     .collect(Collectors.reducingOrElseThrow(Integer::min));
     * // Result: 1
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param op binary operator used to reduce elements
     * @return a {@code Collector} that reduces elements or throws if empty
     * @throws NoSuchElementException if the stream is empty
     */
    public static <T> Collector<T, ?, T> reducingOrElseThrow(final BinaryOperator<T> op) {
        return reducingOrElseThrow(op, noSuchElementExceptionSupplier);
    }

    /**
     * Returns a {@code Collector} that reduces the input elements using the provided binary operator,
     * throwing a specified exception if no elements are present.
     * 
     * <p>This collector is useful when you need to reduce elements and want to handle the empty case
     * with a custom exception. The collector applies the binary operator sequentially to combine
     * elements into a single result.</p>
     * 
     * <p>The returned collector is unordered, meaning the order of reduction may vary in parallel
     * streams unless the binary operator is associative and commutative.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find the maximum value, throw if empty
     * Integer max = Stream.of(3, 1, 4, 1, 5)
     *     .collect(Collectors.reducingOrElseThrow(
     *         Integer::max,
     *         () -> new IllegalStateException("No elements")
     *     ));
     * // Result: 5
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param op a binary operator used to reduce the input elements
     * @param exceptionSupplier a supplier that provides the exception to throw when no elements are present
     * @return a {@code Collector} which reduces the input elements using the binary operator
     * @throws RuntimeException the exception provided by the supplier if no elements are present
     */
    @SuppressWarnings("rawtypes")
    public static <T> Collector<T, ?, T> reducingOrElseThrow(final BinaryOperator<T> op, final Supplier<? extends RuntimeException> exceptionSupplier) {
        final Supplier<OptHolder<T>> supplier = () -> new OptHolder<>(op);

        final BiConsumer<OptHolder<T>, T> accumulator = (BiConsumer) Reducing_Accumulator;
        final BinaryOperator<OptHolder<T>> combiner = (BinaryOperator) Reducing_Combiner;

        final Function<OptHolder<T>, T> finisher = a -> {
            if (a.present) {
                return a.value;
            } else {
                throw exceptionSupplier.get();
            }
        };

        return create(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    /**
     * Returns a {@code Collector} that reduces the input elements using the provided mapper function
     * and binary operator, with an identity value.
     * 
     * <p>This collector first maps each input element using the mapper function, then reduces
     * the mapped values using the binary operator, starting with the identity value. The identity
     * value is used as the initial accumulation value and should be the identity for the binary
     * operator.</p>
     * 
     * <p>The returned collector is unordered, meaning the order of reduction may vary in parallel
     * streams unless the binary operator is associative and commutative.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Sum the lengths of strings
     * Integer totalLength = Stream.of("hello", "world", "java")
     *     .collect(Collectors.reducing(0, String::length, Integer::sum));
     * // Result: 14
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <R> the type of the result
     * @param identity the identity value for the reduction (also the value returned when there are no elements)
     * @param mapper a function to map input elements to the type used for reduction
     * @param op a binary operator used to reduce the mapped values
     * @return a {@code Collector} which reduces the input elements
     */
    public static <T, R> Collector<T, ?, R> reducing(final R identity, final Function<? super T, ? extends R> mapper, final BinaryOperator<R> op) {
        final BiConsumer<Holder<R>, T> accumulator = (a, t) -> a.setValue(op.apply(a.value(), mapper.apply(t)));

        final BinaryOperator<Holder<R>> combiner = (a, b) -> {
            a.setValue(op.apply(a.value(), b.value()));

            return a;
        };

        @SuppressWarnings("rawtypes")
        final Function<Holder<R>, R> finisher = (Function) Reducing_Finisher_0;

        return create(holderSupplier(identity), accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    /**
     * Returns a {@code Collector} that reduces the input elements using the provided mapper function
     * and binary operator, returning an {@code Optional} describing the result.
     * 
     * <p>This collector first maps each input element using the mapper function, then reduces
     * the mapped values using the binary operator. If no elements are present, an empty
     * {@code Optional} is returned.</p>
     * 
     * <p>The returned collector is unordered, meaning the order of reduction may vary in parallel
     * streams unless the binary operator is associative and commutative.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find the longest string
     * Optional<String> longest = Stream.of("apple", "banana", "pear")
     *     .collect(Collectors.reducing(
     *         s -> s,
     *         (s1, s2) -> s1.length() >= s2.length() ? s1 : s2
     *     ));
     * // Result: Optional[banana]
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <R> the type of the result
     * @param mapper a function to map input elements to the type used for reduction
     * @param op a binary operator used to reduce the mapped values
     * @return a {@code Collector} which reduces the input elements into an {@code Optional}
     */
    @SuppressWarnings("rawtypes")
    public static <T, R> Collector<T, ?, Optional<R>> reducing(final Function<? super T, ? extends R> mapper, final BinaryOperator<R> op) {
        final Supplier<MappingOptHolder<T, R>> supplier = () -> new MappingOptHolder<>(mapper, op);

        final BiConsumer<MappingOptHolder<T, R>, T> accumulator = (BiConsumer) Reducing_Accumulator_2;
        final BinaryOperator<MappingOptHolder<T, R>> combiner = (BinaryOperator) Reducing_Combiner_2;
        final Function<MappingOptHolder<T, R>, Optional<R>> finisher = (Function) Reducing_Finisher_2;

        return create(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    private static <T> Supplier<Holder<T>> holderSupplier(final T identity) {
        return () -> Holder.of(identity);
    }

    private static class OptHolder<T> implements Consumer<T> {
        BinaryOperator<T> op = null;
        T value = null;
        boolean present = false;

        OptHolder(final BinaryOperator<T> op) {
            this.op = op;
        }

        @Override
        public void accept(final T t) {
            if (present) {
                value = op.apply(value, t);
            } else {
                value = t;
                present = true;
            }
        }
    }

    private static class MappingOptHolder<T, U> implements Consumer<T> {
        final Function<? super T, ? extends U> mapper;
        final BinaryOperator<U> op;
        U value = null;
        boolean present = false;

        MappingOptHolder(final Function<? super T, ? extends U> mapper, final BinaryOperator<U> op) {
            this.mapper = mapper;
            this.op = op;
        }

        @Override
        public void accept(final T t) {
            if (present) {
                value = op.apply(value, mapper.apply(t));
            } else {
                value = mapper.apply(t);
                present = true;
            }
        }
    }

    /**
     * Returns a {@code Collector} that reduces the input elements using the provided mapper function
     * and binary operator, returning a default value if no elements are present.
     * 
     * <p>This collector first maps each input element using the mapper function, then reduces
     * the mapped values using the binary operator. If no elements are present, the value from
     * the supplier is returned.</p>
     * 
     * <p>The returned collector is unordered, meaning the order of reduction may vary in parallel
     * streams unless the binary operator is associative and commutative.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get the product of all values, or 1 if empty
     * Integer product = Stream.<Integer>empty()
     *     .collect(Collectors.reducingOrElseGet(
     *         i -> i,
     *         (a, b) -> a * b,
     *         () -> 1
     *     ));
     * // Result: 1
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <R> the type of the result
     * @param mapper a function to map input elements to the type used for reduction
     * @param op a binary operator used to reduce the mapped values
     * @param supplierForEmpty a supplier that provides the value to return when no elements are present
     * @return a {@code Collector} which reduces the input elements
     */
    @SuppressWarnings("rawtypes")
    public static <T, R> Collector<T, ?, R> reducingOrElseGet(final Function<? super T, ? extends R> mapper, final BinaryOperator<R> op,
            final Supplier<? extends R> supplierForEmpty) {
        final Supplier<MappingOptHolder<T, R>> supplier = () -> new MappingOptHolder<>(mapper, op);

        final BiConsumer<MappingOptHolder<T, R>, T> accumulator = (BiConsumer) Reducing_Accumulator_2;
        final BinaryOperator<MappingOptHolder<T, R>> combiner = (BinaryOperator) Reducing_Combiner_2;
        final Function<MappingOptHolder<T, R>, R> finisher = a -> a.present ? a.value : supplierForEmpty.get();

        return create(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    /**
     * Returns a {@code Collector} that reduces the input elements using the provided mapper function
     * and binary operator, throwing a specified exception if no elements are present.
     * 
     * <p>This collector first maps each input element using the mapper function, then reduces
     * the mapped values using the binary operator. If no elements are present, the exception
     * from the supplier is thrown.</p>
     * 
     * <p>The returned collector is unordered, meaning the order of reduction may vary in parallel
     * streams unless the binary operator is associative and commutative.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find the minimum after mapping, throw if empty
     * Integer minLength = Stream.of("apple", "pie", "banana")
     *     .collect(Collectors.reducingOrElseThrow(
     *         String::length,
     *         Integer::min,
     *         () -> new NoSuchElementException("No strings found")
     *     ));
     * // Result: 3
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <R> the type of the result
     * @param mapper a function to map input elements to the type used for reduction
     * @param op a binary operator used to reduce the mapped values
     * @param exceptionSupplier a supplier that provides the exception to throw when no elements are present
     * @return a {@code Collector} which reduces the input elements
     * @throws RuntimeException the exception provided by the supplier if no elements are present
     */
    @SuppressWarnings("rawtypes")
    public static <T, R> Collector<T, ?, R> reducingOrElseThrow(final Function<? super T, ? extends R> mapper, final BinaryOperator<R> op,
            final Supplier<? extends RuntimeException> exceptionSupplier) {
        final Supplier<MappingOptHolder<T, R>> supplier = () -> new MappingOptHolder<>(mapper, op);

        final BiConsumer<MappingOptHolder<T, R>, T> accumulator = (BiConsumer) Reducing_Accumulator_2;
        final BinaryOperator<MappingOptHolder<T, R>> combiner = (BinaryOperator) Reducing_Combiner_2;
        final Function<MappingOptHolder<T, R>, R> finisher = a -> {
            if (a.present) {
                return a.value;
            } else {
                throw exceptionSupplier.get();
            }
        };

        return create(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    /**
     * Returns a {@code Collector} that reduces the input elements using the provided mapper function
     * and binary operator, throwing a {@code NoSuchElementException} if no elements are present.
     * 
     * <p>This collector first maps each input element using the mapper function, then reduces
     * the mapped values using the binary operator. If no elements are present, a
     * {@code NoSuchElementException} is thrown.</p>
     * 
     * <p>The returned collector is unordered, meaning the order of reduction may vary in parallel
     * streams unless the binary operator is associative and commutative.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Concatenate all strings
     * String result = Stream.of("Hello", " ", "World")
     *     .collect(Collectors.reducingOrElseThrow(
     *         s -> s,
     *         String::concat
     *     ));
     * // Result: "Hello World"
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <R> the type of the result
     * @param mapper a function to map input elements to the type used for reduction
     * @param op a binary operator used to reduce the mapped values
     * @return a {@code Collector} which reduces the input elements
     * @throws NoSuchElementException if no elements are present
     */
    public static <T, R> Collector<T, ?, R> reducingOrElseThrow(final Function<? super T, ? extends R> mapper, final BinaryOperator<R> op) {
        return reducingOrElseThrow(mapper, op, noSuchElementExceptionSupplier);
    }

    /**
     * Returns a {@code Collector} that computes the common prefix of input {@code CharSequence} objects,
     * returning the result as a {@code String}.
     * 
     * <p>This collector finds the longest sequence of characters that appears at the beginning
     * of all input sequences. For empty input, an empty string is returned. The collector
     * handles Unicode surrogate pairs correctly, ensuring that the prefix doesn't end with
     * an incomplete surrogate pair.</p>
     * 
     * <p>This is a short-circuiting collector: it may stop processing elements once it
     * determines that the common prefix is empty.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find common prefix
     * String prefix = Stream.of("prefix_test", "prefix_example", "prefix_demo")
     *     .collect(Collectors.commonPrefix());
     * // Result: "prefix_"
     * }</pre>
     *
     * @return a {@code Collector} which computes the common prefix of input sequences
     */
    public static Collector<CharSequence, ?, String> commonPrefix() {
        final Supplier<Pair<CharSequence, Integer>> supplier = () -> Pair.of(null, -1);

        final BiConsumer<Pair<CharSequence, Integer>, CharSequence> accumulator = (a, t) -> {
            if (a.right() == -1) {
                a.setLeft(t);
                a.setRight(N.len(t));
            } else if (a.right() > 0) {

                a.setRight(Strings.lengthOfCommonPrefix(a.left(), t));
                a.setLeft(N.len(t) < N.len(a.left()) ? t : a.left());
            }
        };

        final BinaryOperator<Pair<CharSequence, Integer>> combiner = (a, b) -> {
            if (a.right() == -1) {
                return b;
            } else if (b.right() == -1) {
                return a;
            }

            accumulator.accept(a, b.right() == 0 ? "" : b.left().subSequence(0, b.right()));

            return a;
        };

        final Function<Pair<CharSequence, Integer>, String> finisher = a -> a.left() == null || a.right() <= 0 ? ""
                : a.left().subSequence(0, a.right()).toString();

        return create(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    /**
     * Returns a {@code Collector} that computes the common suffix of input {@code CharSequence} objects,
     * returning the result as a {@code String}.
     * 
     * <p>This collector finds the longest sequence of characters that appears at the end
     * of all input sequences. For empty input, an empty string is returned. The collector
     * handles Unicode surrogate pairs correctly, ensuring that the suffix doesn't start with
     * an incomplete surrogate pair.</p>
     * 
     * <p>This is a short-circuiting collector: it may stop processing elements once it
     * determines that the common suffix is empty.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find common suffix
     * String suffix = Stream.of("test_suffix", "example_suffix", "demo_suffix")
     *     .collect(Collectors.commonSuffix());
     * // Result: "_suffix"
     * }</pre>
     *
     * @return a {@code Collector} which computes the common suffix of input sequences
     */
    public static Collector<CharSequence, ?, String> commonSuffix() {
        final Supplier<Pair<CharSequence, Integer>> supplier = () -> Pair.of(null, -1);

        final BiConsumer<Pair<CharSequence, Integer>, CharSequence> accumulator = (a, t) -> {
            if (a.right() == -1) {
                a.setLeft(t);
                a.setRight(t.length());
            } else if (a.right() > 0) {

                a.setRight(Strings.lengthOfCommonSuffix(a.left(), t));
                a.setLeft(N.len(t) < N.len(a.left()) ? t : a.left());
            }
        };

        final BinaryOperator<Pair<CharSequence, Integer>> combiner = (a, b) -> {
            if (a.right() == -1) {
                return b;
            } else if (b.right() == -1) {
                return a;
            }

            final int bLen = b.left().length();

            accumulator.accept(a, b.right() == 0 ? "" : b.left().subSequence(bLen - b.right(), bLen));

            return a;
        };

        final Function<Pair<CharSequence, Integer>, String> finisher = a -> {
            if (a.left() == null || a.right() <= 0) {
                return "";
            }

            final int aLen = a.left().length();
            return a.left().subSequence(aLen - a.right(), aLen).toString();
        };

        return create(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    /**
     * Returns a {@code Collector} that groups input elements by a classifier function,
     * collecting the elements in each group into a {@code List}.
     *
     * <p>This collector applies the classifier function to each input element to determine
     * its group key, then collects all elements with the same key into a list. The lists
     * preserve the encounter order of elements within each group.</p>
     *
     * <p>The returned collector is not concurrent and produces an unordered map.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group strings by their length
     * Map<Integer, List<String>> groupedByLength = 
     *     Stream.of("apple", "pie", "banana", "cat")
     *         .collect(Collectors.groupingBy(String::length));
     * // Result: {5=[apple], 3=[pie, cat], 6=[banana]}
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the type of keys
     * @param keyMapper a classifier function mapping input elements to keys
     * @return a {@code Collector} implementing the group-by operation
     */
    public static <T, K> Collector<T, ?, Map<K, List<T>>> groupingBy(final Function<? super T, ? extends K> keyMapper) {
        final Collector<? super T, ?, List<T>> downstream = toList();

        return groupingBy(keyMapper, downstream);
    }

    /**
     * Returns a {@code Collector} that groups input elements by a classifier function,
     * collecting the elements in each group into a {@code List} and storing them in a
     * map created by the provided factory.
     * 
     * <p>This collector applies the classifier function to each input element to determine
     * its group key, then collects all elements with the same key into a list. The map
     * instance is created by the provided factory function.</p>
     * 
     * <p>The returned collector is not concurrent.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group strings by first letter into a TreeMap
     * TreeMap<Character, List<String>> groupedByFirstLetter = 
     *     Stream.of("apple", "apricot", "banana", "cherry")
     *         .collect(Collectors.groupingBy(
     *             s -> s.charAt(0),
     *             TreeMap::new
     *         ));
     * // Result: {a=[apple, apricot], b=[banana], c=[cherry]}
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the type of keys
     * @param <M> the type of the resulting map
     * @param keyMapper a classifier function mapping input elements to keys
     * @param mapFactory a supplier providing a new empty map into which the results will be inserted
     * @return a {@code Collector} implementing the group-by operation
     */
    public static <T, K, M extends Map<K, List<T>>> Collector<T, ?, M> groupingBy(final Function<? super T, ? extends K> keyMapper,
            final Supplier<? extends M> mapFactory) {
        final Collector<? super T, ?, List<T>> downstream = toList();

        return groupingBy(keyMapper, downstream, mapFactory);
    }

    /**
     * Returns a {@code Collector} that groups input elements by a classifier function,
     * and then performs a reduction operation on the values in each group using the
     * specified downstream collector.
     * 
     * <p>This collector applies the classifier function to each input element to determine
     * its group key, then applies the downstream collector to all elements with the same key.
     * The results are stored in a map where keys are produced by the classifier function
     * and values are results of the downstream collector.</p>
     * 
     * <p>The returned collector is not concurrent and produces an unordered map.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Count strings by their length
     * Map<Integer, Long> countByLength =
     *     Stream.of("apple", "pie", "banana", "cat", "dog")
     *         .collect(Collectors.groupingBy(
     *             String::length,
     *             Collectors.counting()
     *         ));
     * // Result: {3=3, 5=1, 6=1}
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the type of keys
     * @param <A> the intermediate accumulation type of the downstream collector
     * @param <D> the result type of the downstream reduction
     * @param keyMapper a classifier function mapping input elements to keys
     * @param downstream a collector implementing the downstream reduction
     * @return a {@code Collector} implementing the cascaded group-by operation
     */
    public static <T, K, A, D> Collector<T, ?, Map<K, D>> groupingBy(final Function<? super T, ? extends K> keyMapper,
            final Collector<? super T, A, D> downstream) {
        final Supplier<Map<K, D>> mapFactory = Suppliers.ofMap();

        return groupingBy(keyMapper, downstream, mapFactory);
    }

    /**
     * Returns a {@code Collector} that groups input elements by a classifier function,
     * performs a reduction operation on the values in each group using the specified
     * downstream collector, and stores the results in a map created by the provided factory.
     * 
     * <p>This collector applies the classifier function to each input element to determine
     * its group key, then applies the downstream collector to all elements with the same key.
     * The results are stored in a map instance created by the provided factory function.</p>
     * 
     * <p>The returned collector is not concurrent.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group persons by city and collect names in a TreeMap
     * TreeMap<String, Set<String>> namesByCity = 
     *     persons.stream()
     *         .collect(Collectors.groupingBy(
     *             Person::getCity,
     *             Collectors.mapping(Person::getName, Collectors.toSet()),
     *             TreeMap::new
     *         ));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the type of keys
     * @param <A> the intermediate accumulation type of the downstream collector
     * @param <D> the result type of the downstream reduction
     * @param <M> the type of the resulting map
     * @param keyMapper a classifier function mapping input elements to keys
     * @param downstream a collector implementing the downstream reduction
     * @param mapFactory a supplier providing a new empty map into which the results will be inserted
     * @return a {@code Collector} implementing the cascaded group-by operation
     * @see java.util.stream.Collectors#groupingBy(Function, Supplier, Collector)
     */
    public static <T, K, A, D, M extends Map<K, D>> Collector<T, ?, M> groupingBy(final Function<? super T, ? extends K> keyMapper,
            final Collector<? super T, A, D> downstream, final Supplier<? extends M> mapFactory) throws IllegalArgumentException {
        final Supplier<M> mapSupplier = (Supplier<M>) mapFactory;

        return java.util.stream.Collectors.groupingBy(keyMapper, mapSupplier, downstream);
    }

    /**
     * Returns a concurrent {@code Collector} that groups input elements by a classifier function,
     * collecting the elements in each group into a {@code List}.
     * 
     * <p>This collector is similar to {@link #groupingBy(Function)}, but the returned map
     * is concurrent and can be used safely in parallel streams. The collector applies the
     * classifier function to each input element to determine its group key, then collects
     * all elements with the same key into a list.</p>
     * 
     * <p>The returned collector is concurrent and unordered.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group strings by length in parallel
     * ConcurrentMap<Integer, List<String>> groupedByLength = 
     *     largeListOfStrings.parallelStream()
     *         .collect(Collectors.groupingByConcurrent(String::length));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the type of keys
     * @param keyMapper a classifier function mapping input elements to keys
     * @return a concurrent {@code Collector} implementing the group-by operation
     */
    public static <T, K> Collector<T, ?, ConcurrentMap<K, List<T>>> groupingByConcurrent(final Function<? super T, ? extends K> keyMapper) {
        final Collector<? super T, ?, List<T>> downstream = toList();

        return groupingByConcurrent(keyMapper, downstream);
    }

    /**
     * Returns a concurrent {@code Collector} that groups input elements by a classifier function,
     * collecting the elements in each group into a {@code List} and storing them in a
     * concurrent map created by the provided factory.
     * 
     * <p>This collector is similar to {@link #groupingBy(Function, Supplier)}, but the returned
     * map is concurrent and can be used safely in parallel streams. The map instance is created
     * by the provided factory function.</p>
     * 
     * <p>The returned collector is concurrent and unordered.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group strings by first letter into a ConcurrentSkipListMap
     * ConcurrentSkipListMap<Character, List<String>> groupedByFirstLetter = 
     *     strings.parallelStream()
     *         .collect(Collectors.groupingByConcurrent(
     *             s -> s.charAt(0),
     *             ConcurrentSkipListMap::new
     *         ));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the type of keys
     * @param <M> the type of the resulting concurrent map
     * @param keyMapper a classifier function mapping input elements to keys
     * @param mapFactory a supplier providing a new empty concurrent map into which the results will be inserted
     * @return a concurrent {@code Collector} implementing the group-by operation
     */
    public static <T, K, M extends ConcurrentMap<K, List<T>>> Collector<T, ?, M> groupingByConcurrent(final Function<? super T, ? extends K> keyMapper,
            final Supplier<? extends M> mapFactory) {
        final Collector<? super T, ?, List<T>> downstream = toList();

        return groupingByConcurrent(keyMapper, downstream, mapFactory);
    }

    /**
     * Returns a concurrent {@code Collector} that groups input elements by a classifier function,
     * and then performs a reduction operation on the values in each group using the
     * specified downstream collector.
     * 
     * <p>This collector is similar to {@link #groupingBy(Function, Collector)}, but the returned
     * map is concurrent and can be used safely in parallel streams. The collector applies the
     * classifier function to each input element to determine its group key, then applies the
     * downstream collector to all elements with the same key.</p>
     * 
     * <p>The returned collector is concurrent and unordered.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Count strings by length in parallel
     * ConcurrentMap<Integer, Long> countByLength = 
     *     largeListOfStrings.parallelStream()
     *         .collect(Collectors.groupingByConcurrent(
     *             String::length,
     *             Collectors.counting()
     *         ));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the type of keys
     * @param <A> the intermediate accumulation type of the downstream collector
     * @param <D> the result type of the downstream reduction
     * @param keyMapper a classifier function mapping input elements to keys
     * @param downstream a collector implementing the downstream reduction
     * @return a concurrent {@code Collector} implementing the cascaded group-by operation
     */
    public static <T, K, A, D> Collector<T, ?, ConcurrentMap<K, D>> groupingByConcurrent(final Function<? super T, ? extends K> keyMapper,
            final Collector<? super T, A, D> downstream) {
        final Supplier<ConcurrentMap<K, D>> mapFactory = Suppliers.ofConcurrentMap();

        return groupingByConcurrent(keyMapper, downstream, mapFactory);
    }

    /**
     * Returns a concurrent {@code Collector} that groups input elements by a classifier function,
     * performs a reduction operation on the values in each group using the specified
     * downstream collector, and stores the results in a concurrent map created by the provided factory.
     * 
     * <p>This collector is similar to {@link #groupingBy(Function, Collector, Supplier)}, but the
     * returned map is concurrent and can be used safely in parallel streams. The map instance
     * is created by the provided factory function.</p>
     * 
     * <p>The returned collector is concurrent and unordered.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group persons by department and sum salaries in parallel
     * ConcurrentHashMap<String, Double> salaryByDept = 
     *     employees.parallelStream()
     *         .collect(Collectors.groupingByConcurrent(
     *             Employee::getDepartment,
     *             Collectors.summingDouble(Employee::getSalary),
     *             ConcurrentHashMap::new
     *         ));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the type of keys
     * @param <A> the intermediate accumulation type of the downstream collector
     * @param <D> the result type of the downstream reduction
     * @param <M> the type of the resulting concurrent map
     * @param keyMapper a classifier function mapping input elements to keys
     * @param downstream a collector implementing the downstream reduction
     * @param mapFactory a supplier providing a new empty concurrent map into which the results will be inserted
     * @return a concurrent {@code Collector} implementing the cascaded group-by operation
     * @see java.util.stream.Collectors#groupingByConcurrent(Function, Supplier, Collector)
     */
    public static <T, K, A, D, M extends ConcurrentMap<K, D>> Collector<T, ?, M> groupingByConcurrent(final Function<? super T, ? extends K> keyMapper,
            final Collector<? super T, A, D> downstream, final Supplier<? extends M> mapFactory) throws IllegalArgumentException {
        final Supplier<M> mapSupplier = (Supplier<M>) mapFactory;

        return java.util.stream.Collectors.groupingByConcurrent(keyMapper, mapSupplier, downstream);
    }

    /**
     * Returns a {@code Collector} that partitions the input elements according to a predicate,
     * collecting the elements in each partition into a {@code List}.
     * 
     * <p>This collector applies the predicate to each input element to determine whether it
     * belongs to the {@code true} or {@code false} partition. The returned map always contains
     * exactly two entries: one for {@code true} and one for {@code false}, each containing a
     * list of elements (which may be empty).</p>
     * 
     * <p>The returned collector is unordered.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Partition numbers into even and odd
     * Map<Boolean, List<Integer>> evenOddPartition = 
     *     Stream.of(1, 2, 3, 4, 5, 6)
     *         .collect(Collectors.partitioningBy(n -> n % 2 == 0));
     * // Result: {false=[1, 3, 5], true=[2, 4, 6]}
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param predicate a predicate used for classifying input elements
     * @return a {@code Collector} implementing the partitioning operation
     */
    public static <T> Collector<T, ?, Map<Boolean, List<T>>> partitioningBy(final Predicate<? super T> predicate) {
        final Collector<? super T, ?, List<T>> downstream = toList();

        return partitioningBy(predicate, downstream);
    }

    /**
     * Returns a {@code Collector} that partitions the input elements according to a predicate,
     * and then performs a reduction operation on the values in each partition using the
     * specified downstream collector.
     * 
     * <p>This collector applies the predicate to each input element to determine whether it
     * belongs to the {@code true} or {@code false} partition, then applies the downstream
     * collector to all elements in each partition. The returned map always contains exactly
     * two entries: one for {@code true} and one for {@code false}.</p>
     * 
     * <p>The returned collector is unordered.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Partition strings by length and count each partition
     * Map<Boolean, Long> lengthPartitionCount = 
     *     Stream.of("a", "bb", "ccc", "dd", "e")
     *         .collect(Collectors.partitioningBy(
     *             s -> s.length() > 1,
     *             Collectors.counting()
     *         ));
     * // Result: {false=2, true=3}
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <D> the result type of the downstream reduction
     * @param <A> the intermediate accumulation type of the downstream collector
     * @param predicate a predicate used for classifying input elements
     * @param downstream a collector implementing the downstream reduction
     * @return a {@code Collector} implementing the cascaded partitioning operation
     */
    public static <T, D, A> Collector<T, ?, Map<Boolean, D>> partitioningBy(final Predicate<? super T> predicate, final Collector<? super T, A, D> downstream) {
        final Supplier<Map<Boolean, A>> supplier = () -> {
            final Map<Boolean, A> map = N.newHashMap(2);
            map.put(true, downstream.supplier().get());
            map.put(false, downstream.supplier().get());
            return map;
        };

        final BiConsumer<A, ? super T> downstreamAccumulator = downstream.accumulator();
        final BiConsumer<Map<Boolean, A>, T> accumulator = (a, t) -> downstreamAccumulator
                .accept(predicate.test(t) ? a.get(Boolean.TRUE) : a.get(Boolean.FALSE), t);

        final BinaryOperator<A> op = downstream.combiner();
        final BinaryOperator<Map<Boolean, A>> combiner = (a, b) -> {
            a.put(Boolean.TRUE, op.apply(a.get(Boolean.TRUE), b.get(Boolean.TRUE)));
            a.put(Boolean.FALSE, op.apply(a.get(Boolean.FALSE), b.get(Boolean.FALSE)));
            return a;
        };

        final Function<Map<Boolean, A>, Map<Boolean, D>> finisher = a -> {
            @SuppressWarnings("rawtypes")
            final Map<Boolean, D> result = (Map) a;

            result.put(Boolean.TRUE, downstream.finisher().apply((a.get(Boolean.TRUE))));
            result.put(Boolean.FALSE, downstream.finisher().apply((a.get(Boolean.FALSE))));

            return result;
        };

        return create(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    /**
     * Returns a {@code Collector} that counts the number of input elements grouped by a
     * classifier function, storing the counts as {@code Long} values.
     * 
     * <p>This collector applies the classifier function to each input element to determine
     * its group key, then counts the number of elements for each key. The returned map
     * contains the count for each distinct key.</p>
     *
     * <p>The returned collector is not concurrent and produces an unordered map.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Count strings by their first letter
     * Map<Character, Long> letterCounts = 
     *     Stream.of("apple", "apricot", "banana", "cherry", "apple")
     *         .collect(Collectors.countingBy(s -> s.charAt(0)));
     * // Result: {a=3, b=1, c=1}
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the type of keys
     * @param keyMapper a classifier function mapping input elements to keys
     * @return a {@code Collector} implementing the counting operation
     */
    public static <T, K> Collector<T, ?, Map<K, Long>> countingBy(final Function<? super T, ? extends K> keyMapper) {
        return countingBy(keyMapper, Suppliers.ofMap());
    }

    /**
     * Returns a {@code Collector} that counts the number of input elements grouped by a
     * classifier function, storing the counts as {@code Long} values in a map created by
     * the provided factory.
     * 
     * <p>This collector applies the classifier function to each input element to determine
     * its group key, then counts the number of elements for each key. The map instance is
     * created by the provided factory function.</p>
     * 
     * <p>The returned collector is unordered.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Count strings by length into a TreeMap
     * TreeMap<Integer, Long> lengthCounts = 
     *     Stream.of("a", "bb", "ccc", "dd", "e")
     *         .collect(Collectors.countingBy(
     *             String::length,
     *             TreeMap::new
     *         ));
     * // Result: {1=2, 2=2, 3=1}
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the type of keys
     * @param <M> the type of the resulting map
     * @param keyMapper a classifier function mapping input elements to keys
     * @param mapFactory a supplier providing a new empty map into which the results will be inserted
     * @return a {@code Collector} implementing the counting operation
     */
    public static <T, K, M extends Map<K, Long>> Collector<T, ?, M> countingBy(final Function<? super T, ? extends K> keyMapper,
            final Supplier<? extends M> mapFactory) {
        final Collector<? super T, ?, Long> downstream = counting();

        return groupingBy(keyMapper, downstream, mapFactory);
    }

    /**
     * Returns a {@code Collector} that counts the number of input elements grouped by a
     * classifier function, storing the counts as {@code Integer} values.
     * 
     * <p>This collector is similar to {@link #countingBy(Function)}, but returns counts
     * as {@code Integer} values instead of {@code Long}. This is useful when you know
     * the counts will not exceed {@code Integer.MAX_VALUE}.</p>
     *
     * <p>The returned collector is not concurrent and produces an unordered map.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Count words by length (as integers)
     * Map<Integer, Integer> wordLengthCounts = 
     *     Stream.of("hello", "world", "java", "code")
     *         .collect(Collectors.countingToIntBy(String::length));
     * // Result: {4=2, 5=2}
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the type of keys
     * @param keyMapper a classifier function mapping input elements to keys
     * @return a {@code Collector} implementing the counting operation with integer results
     */
    public static <T, K> Collector<T, ?, Map<K, Integer>> countingToIntBy(final Function<? super T, ? extends K> keyMapper) {
        return countingToIntBy(keyMapper, Suppliers.ofMap());
    }

    /**
     * Returns a {@code Collector} that counts the number of input elements grouped by a
     * classifier function, storing the counts as {@code Integer} values in a map created by
     * the provided factory.
     * 
     * <p>This collector is similar to {@link #countingBy(Function, Supplier)}, but returns
     * counts as {@code Integer} values instead of {@code Long}. The map instance is created
     * by the provided factory function.</p>
     * 
     * <p>The returned collector is unordered.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Count items by category into a LinkedHashMap (as integers)
     * LinkedHashMap<String, Integer> categoryCounts = 
     *     items.stream()
     *         .collect(Collectors.countingToIntBy(
     *             Item::getCategory,
     *             LinkedHashMap::new
     *         ));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the type of keys
     * @param <M> the type of the resulting map
     * @param keyMapper a classifier function mapping input elements to keys
     * @param mapFactory a supplier providing a new empty map into which the results will be inserted
     * @return a {@code Collector} implementing the counting operation with integer results
     */
    public static <T, K, M extends Map<K, Integer>> Collector<T, ?, M> countingToIntBy(final Function<? super T, ? extends K> keyMapper,
            final Supplier<? extends M> mapFactory) {
        final Collector<? super T, ?, Integer> downstream = countingToInt();

        return groupingBy(keyMapper, downstream, mapFactory);
    }

    /**
     * Returns a {@code Collector} that accumulates {@code Map.Entry} elements into a {@code Map}.
     *
     * <p>This collector extracts keys and values from {@code Map.Entry} objects and
     * accumulates them into a new map. If duplicate keys are encountered, an
     * {@code IllegalStateException} is thrown.</p>
     *
     * <p><b>Null Handling:</b> Both null keys and null values are supported if the
     * underlying map implementation allows them.</p>
     *
     * <p><b>Parallel Stream Support:</b> This collector supports parallel streams.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert a list of entries to a map
     * Map<String, Integer> map = entryList.stream()
     *     .collect(Collectors.toMap());
     * }</pre>
     *
     * @param <K> the type of keys
     * @param <V> the type of values
     * @return a {@code Collector} which collects {@code Map.Entry} elements into a {@code Map}
     * @throws IllegalStateException if duplicate keys are encountered
     * @see #toMap(BinaryOperator)
     * @see #toMap(Supplier)
     * @see #toMap(Function, Function)
     */
    public static <K, V> Collector<Map.Entry<K, V>, ?, Map<K, V>> toMap() {
        final Function<Map.Entry<K, V>, ? extends K> keyMapper = Fn.key();
        final Function<Map.Entry<K, V>, ? extends V> valueMapper = Fn.value();

        return toMap(keyMapper, valueMapper);
    }

    /**
     * Returns a {@code Collector} that accumulates {@code Map.Entry} elements into a {@code Map},
     * using the provided merge function to resolve collisions between values associated with
     * the same key.
     *
     * <p>This collector extracts keys and values from {@code Map.Entry} objects and
     * accumulates them into a new map. When duplicate keys are encountered, the merge
     * function is used to combine the values.</p>
     *
     * <p><b>Null Handling:</b> Both null keys and null values are supported if the
     * underlying map implementation allows them. The merge function must handle null values
     * if they may be present.</p>
     *
     * <p><b>Parallel Stream Support:</b> This collector supports parallel streams.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge entries, keeping the maximum value for duplicate keys
     * Map<String, Integer> maxValues = entries.stream()
     *     .collect(Collectors.toMap(Integer::max));
     * }</pre>
     *
     * @param <K> the type of keys
     * @param <V> the type of values
     * @param mergeFunction a merge function used to resolve collisions between values associated with the same key
     * @return a {@code Collector} which collects {@code Map.Entry} elements into a {@code Map}
     * @see #toMap()
     * @see #toMap(BinaryOperator, Supplier)
     * @see #toMap(Function, Function, BinaryOperator)
     */
    public static <K, V> Collector<Map.Entry<K, V>, ?, Map<K, V>> toMap(final BinaryOperator<V> mergeFunction) {
        final Function<Map.Entry<K, V>, ? extends K> keyMapper = Fn.key();
        final Function<Map.Entry<K, V>, ? extends V> valueMapper = Fn.value();

        return toMap(keyMapper, valueMapper, mergeFunction);
    }

    /**
     * Returns a {@code Collector} that accumulates {@code Map.Entry} elements into a {@code Map}
     * created by the provided factory.
     * 
     * <p>This collector extracts keys and values from {@code Map.Entry} objects and
     * accumulates them into a map instance created by the provided factory function.
     * If duplicate keys are encountered, an {@code IllegalStateException} is thrown.</p>
     * 
     * <p>The returned collector produces an unordered map.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert entries to a TreeMap
     * TreeMap<String, Integer> sortedMap = entries.stream()
     *     .collect(Collectors.toMap(TreeMap::new));
     * }</pre>
     *
     * @param <K> the type of keys
     * @param <V> the type of values
     * @param <M> the type of the resulting map
     * @param mapFactory a supplier providing a new empty map into which the results will be inserted
     * @return a {@code Collector} which collects {@code Map.Entry} elements into a {@code Map}
     * @throws IllegalStateException if duplicate keys are encountered
     */
    public static <K, V, M extends Map<K, V>> Collector<Map.Entry<K, V>, ?, M> toMap(final Supplier<? extends M> mapFactory) {
        final Function<Map.Entry<K, V>, ? extends K> keyMapper = Fn.key();
        final Function<Map.Entry<K, V>, ? extends V> valueMapper = Fn.value();

        return toMap(keyMapper, valueMapper, mapFactory);
    }

    /**
     * Returns a {@code Collector} that accumulates {@code Map.Entry} elements into a {@code Map}
     * created by the provided factory, using the provided merge function to resolve collisions
     * between values associated with the same key.
     * 
     * <p>This collector extracts keys and values from {@code Map.Entry} objects and
     * accumulates them into a map instance created by the provided factory function.
     * When duplicate keys are encountered, the merge function is used to combine the values.</p>
     * 
     * <p>The returned collector produces an unordered map.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge entries into a LinkedHashMap, summing values for duplicate keys
     * LinkedHashMap<String, Integer> summedMap = entries.stream()
     *     .collect(Collectors.toMap(Integer::sum, LinkedHashMap::new));
     * }</pre>
     *
     * @param <K> the type of keys
     * @param <V> the type of values
     * @param <M> the type of the resulting map
     * @param mergeFunction a merge function used to resolve collisions between values associated with the same key
     * @param mapFactory a supplier providing a new empty map into which the results will be inserted
     * @return a {@code Collector} which collects {@code Map.Entry} elements into a {@code Map}
     */
    public static <K, V, M extends Map<K, V>> Collector<Map.Entry<K, V>, ?, M> toMap(final BinaryOperator<V> mergeFunction,
            final Supplier<? extends M> mapFactory) {
        final Function<Map.Entry<K, V>, ? extends K> keyMapper = Fn.key();
        final Function<Map.Entry<K, V>, ? extends V> valueMapper = Fn.value();

        return toMap(keyMapper, valueMapper, mergeFunction, mapFactory);
    }

    /**
     * Returns a {@code Collector} that accumulates elements into a {@code Map} whose keys
     * and values are the result of applying the provided mapping functions to the input elements.
     *
     * <p>If the mapped keys contain duplicates, an {@code IllegalStateException} is thrown
     * when the collection operation is performed. To handle duplicate keys, use
     * {@link #toMap(Function, Function, BinaryOperator)} instead.</p>
     *
     * <p><b>Null Handling:</b> Both null keys and null values are supported if the
     * underlying map implementation allows them. The key and value mapper functions must
     * handle null input elements appropriately.</p>
     *
     * <p><b>Parallel Stream Support:</b> This collector supports parallel streams.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a map from student ID to student name
     * Map<Integer, String> studentMap = students.stream()
     *     .collect(Collectors.toMap(Student::getId, Student::getName));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the type of keys
     * @param <V> the type of values
     * @param keyMapper a mapping function to produce keys
     * @param valueMapper a mapping function to produce values
     * @return a {@code Collector} which collects elements into a {@code Map}
     * @throws IllegalStateException if duplicate keys are encountered
     * @see #toMap(Function, Function, BinaryOperator)
     * @see #toMap(Function, Function, Supplier)
     * @see #toMap(Function, Function, BinaryOperator, Supplier)
     */
    public static <T, K, V> Collector<T, ?, Map<K, V>> toMap(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper) {
        final BinaryOperator<V> mergeFunction = Fn.throwingMerger();

        return toMap(keyMapper, valueMapper, mergeFunction);
    }

    /**
     * Returns a {@code Collector} that accumulates elements into a {@code Map} whose keys
     * and values are the result of applying the provided mapping functions to the input elements.
     *
     * <p>This collector uses the provided merge function to handle collisions between values
     * associated with the same key. The merge function is used to combine the existing value
     * with the new value when duplicate keys are encountered.</p>
     *
     * <p>The returned map is a general-purpose {@code Map} implementation. The map is created
     * using the default map factory which typically returns a {@code HashMap}.</p>
     *
     * <p><b>Null Handling:</b> Both null keys and null values are supported if the
     * underlying map implementation allows them. The key and value mapper functions as well
     * as the merge function must handle null values if they may be present.</p>
     *
     * <p><b>Parallel Stream Support:</b> This collector supports parallel streams.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Sum values for duplicate keys
     * Map<String, Integer> result = Stream.of("apple", "banana", "apple")
     *     .collect(Collectors.toMap(
     *         Function.identity(),
     *         String::length,
     *         Integer::sum));
     * // Result: {apple=10, banana=6}
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the output type of the key mapping function
     * @param <V> the output type of the value mapping function
     * @param keyMapper a mapping function to produce keys
     * @param valueMapper a mapping function to produce values
     * @param mergeFunction a merge function, used to resolve collisions between
     *                      values associated with the same key
     * @return a {@code Collector} which collects elements into a {@code Map}
     *         whose keys and values are the result of applying mapping functions to
     *         the input elements
     * @see #toMap(Function, Function)
     * @see #toMap(Function, Function, Supplier)
     * @see #toMap(Function, Function, BinaryOperator, Supplier)
     */
    public static <T, K, V> Collector<T, ?, Map<K, V>> toMap(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper, final BinaryOperator<V> mergeFunction) {
        final Supplier<Map<K, V>> mapFactory = Suppliers.ofMap();

        return toMap(keyMapper, valueMapper, mergeFunction, mapFactory);
    }

    /**
     * Returns a {@code Collector} that accumulates elements into a {@code Map} whose keys
     * and values are the result of applying the provided mapping functions to the input elements.
     *
     * <p>If the mapped keys contain duplicates, an {@code IllegalStateException} is thrown
     * when the collection operation is performed.</p>
     *
     * <p>The Map is created by the provided supplier function. This allows you to specify
     * the exact type of Map to be created (e.g., {@code HashMap}, {@code TreeMap}, etc.).</p>
     *
     * <p><b>Null Handling:</b> Null keys and values are supported if the map implementation
     * created by the factory allows them.</p>
     *
     * <p><b>Parallel Stream Support:</b> This collector supports parallel streams.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect to a TreeMap
     * TreeMap<String, Integer> result = Stream.of("apple", "banana", "cherry")
     *     .collect(Collectors.toMap(
     *         Function.identity(),
     *         String::length,
     *         TreeMap::new));
     * // Result: {apple=5, banana=6, cherry=6} (sorted by key)
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the output type of the key mapping function
     * @param <V> the output type of the value mapping function
     * @param <M> the type of the resulting {@code Map}
     * @param keyMapper a mapping function to produce keys
     * @param valueMapper a mapping function to produce values
     * @param mapFactory a supplier providing a new empty {@code Map}
     *                   into which the results will be inserted
     * @return a {@code Collector} which collects elements into a {@code Map}
     *         whose keys and values are the result of applying mapping functions to
     *         the input elements
     * @throws IllegalStateException if duplicate keys are encountered
     * @see #toMap(Function, Function)
     * @see #toMap(Function, Function, BinaryOperator, Supplier)
     */
    public static <T, K, V, M extends Map<K, V>> Collector<T, ?, M> toMap(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper, final Supplier<? extends M> mapFactory) {
        final BinaryOperator<V> mergeFunction = Fn.throwingMerger();

        return toMap(keyMapper, valueMapper, mergeFunction, mapFactory);
    }

    /**
     * Returns a {@code Collector} that accumulates elements into a {@code Map} whose keys
     * and values are the result of applying the provided mapping functions to the input elements.
     *
     * <p>This is the most general form of map collector, allowing you to specify the key mapper,
     * value mapper, merge function for handling duplicate keys, and the map factory for creating
     * the result map.</p>
     *
     * <p>The merge function is used to resolve collisions between values associated with the same key.
     * The map is created by the provided supplier function.</p>
     *
     * <p><b>Null Handling:</b> Null keys and values are supported if the map implementation
     * created by the factory allows them. The key mapper, value mapper, and merge function must
     * handle null values appropriately if they may be present.</p>
     *
     * <p><b>Parallel Stream Support:</b> This collector supports parallel streams with
     * the UNORDERED characteristic for optimal performance.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect to a LinkedHashMap, concatenating values for duplicate keys
     * LinkedHashMap<String, String> result = Stream.of("a:1", "b:2", "a:3")
     *     .collect(Collectors.toMap(
     *         s -> s.split(":")[0],
     *         s -> s.split(":")[1],
     *         (v1, v2) -> v1 + "," + v2,
     *         LinkedHashMap::new));
     * // Result: {a=1,3, b=2} (maintains insertion order)
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the output type of the key mapping function
     * @param <V> the output type of the value mapping function
     * @param <M> the type of the resulting {@code Map}
     * @param keyMapper a mapping function to produce keys
     * @param valueMapper a mapping function to produce values
     * @param mergeFunction a merge function, used to resolve collisions between
     *                      values associated with the same key
     * @param mapFactory a supplier providing a new empty {@code Map}
     *                   into which the results will be inserted
     * @return a {@code Collector} which collects elements into a {@code Map}
     *         whose keys and values are the result of applying mapping functions to
     *         the input elements
     * @see #toMap(Function, Function)
     * @see #toMap(Function, Function, BinaryOperator)
     * @see #toMap(Function, Function, Supplier)
     * @see #toLinkedHashMap(Function, Function, BinaryOperator)
     * @see #toConcurrentMap(Function, Function, BinaryOperator, Supplier)
     */
    public static <T, K, V, M extends Map<K, V>> Collector<T, ?, M> toMap(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper, final BinaryOperator<V> mergeFunction, final Supplier<? extends M> mapFactory) {
        final BiConsumer<M, T> accumulator = (map, element) -> map.merge(keyMapper.apply(element), valueMapper.apply(element), mergeFunction);

        final BinaryOperator<M> combiner = mapMerger(mergeFunction);

        return create(mapFactory, accumulator, combiner, CH_UNORDERED_ID);
    }

    /**
     * Returns a {@code Collector} that accumulates {@code Map.Entry} elements into an {@code ImmutableMap}.
     * 
     * <p>This collector is designed to work with streams of {@code Map.Entry} objects,
     * using the entry's key and value directly. If duplicate keys are encountered,
     * an {@code IllegalStateException} is thrown.</p>
     * 
     * <p>The resulting map is immutable and cannot be modified after creation.
     * Any attempt to modify the returned map will result in an {@code UnsupportedOperationException}.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert a map to an immutable map
     * ImmutableMap<String, Integer> result = originalMap.entrySet().stream()
     *     .filter(e -> e.getValue() > 0)
     *     .collect(Collectors.toImmutableMap());
     * }</pre>
     *
     * @param <K> the type of map keys
     * @param <V> the type of map values
     * @return a {@code Collector} which collects {@code Map.Entry} elements into an {@code ImmutableMap}
     * @throws IllegalStateException if duplicate keys are encountered
     */
    public static <K, V> Collector<Map.Entry<K, V>, ?, ImmutableMap<K, V>> toImmutableMap() {
        final Collector<Map.Entry<K, V>, ?, Map<K, V>> downstream = toMap();
        @SuppressWarnings("rawtypes")
        final Function<Map<K, V>, ImmutableMap<K, V>> finisher = (Function) ImmutableMap_Finisher;

        return collectingAndThen(downstream, finisher);
    }

    /**
     * Returns a {@code Collector} that accumulates {@code Map.Entry} elements into an {@code ImmutableMap}
     * with a merge function to handle duplicate keys.
     * 
     * <p>This collector is designed to work with streams of {@code Map.Entry} objects.
     * When duplicate keys are encountered, the provided merge function is used to combine
     * the values.</p>
     * 
     * <p>The resulting map is immutable and cannot be modified after creation.
     * Any attempt to modify the returned map will result in an {@code UnsupportedOperationException}.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge entries with duplicate keys by summing values
     * ImmutableMap<String, Integer> result = entries.stream()
     *     .collect(Collectors.toImmutableMap(Integer::sum));
     * }</pre>
     *
     * @param <K> the type of map keys
     * @param <V> the type of map values
     * @param mergeFunction a merge function, used to resolve collisions between
     *                      values associated with the same key
     * @return a {@code Collector} which collects {@code Map.Entry} elements into an {@code ImmutableMap}
     */
    public static <K, V> Collector<Map.Entry<K, V>, ?, ImmutableMap<K, V>> toImmutableMap(final BinaryOperator<V> mergeFunction) {
        final Collector<Map.Entry<K, V>, ?, Map<K, V>> downstream = toMap(mergeFunction);
        @SuppressWarnings("rawtypes")
        final Function<Map<K, V>, ImmutableMap<K, V>> finisher = (Function) ImmutableMap_Finisher;

        return collectingAndThen(downstream, finisher);
    }

    /**
     * Returns a {@code Collector} that accumulates elements into an {@code ImmutableMap}
     * whose keys and values are the result of applying the provided mapping functions.
     * 
     * <p>If duplicate keys are encountered, an {@code IllegalStateException} is thrown.
     * The resulting map is immutable and cannot be modified after creation.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create an immutable map from a list of strings
     * ImmutableMap<String, Integer> result = Stream.of("apple", "banana", "cherry")
     *     .collect(Collectors.toImmutableMap(
     *         Function.identity(),
     *         String::length));
     * // Result: {apple=5, banana=6, cherry=6}
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the output type of the key mapping function
     * @param <V> the output type of the value mapping function
     * @param keyMapper a mapping function to produce keys
     * @param valueMapper a mapping function to produce values
     * @return a {@code Collector} which collects elements into an {@code ImmutableMap}
     * @throws IllegalStateException if duplicate keys are encountered
     */
    public static <T, K, V> Collector<T, ?, ImmutableMap<K, V>> toImmutableMap(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper) {
        final Collector<T, ?, Map<K, V>> downstream = toMap(keyMapper, valueMapper);
        @SuppressWarnings("rawtypes")
        final Function<Map<K, V>, ImmutableMap<K, V>> finisher = (Function) ImmutableMap_Finisher;

        return collectingAndThen(downstream, finisher);
    }

    /**
     * Returns a {@code Collector} that accumulates elements into an {@code ImmutableMap}
     * whose keys and values are the result of applying the provided mapping functions,
     * with a merge function to handle duplicate keys.
     * 
     * <p>When duplicate keys are encountered, the provided merge function is used to
     * combine the values. The resulting map is immutable and cannot be modified after creation.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create an immutable map, concatenating values for duplicate keys
     * ImmutableMap<String, String> result = Stream.of("a:1", "b:2", "a:3")
     *     .collect(Collectors.toImmutableMap(
     *         s -> s.split(":")[0],
     *         s -> s.split(":")[1],
     *         (v1, v2) -> v1 + "," + v2));
     * // Result: {a=1,3, b=2}
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the output type of the key mapping function
     * @param <V> the output type of the value mapping function
     * @param keyMapper a mapping function to produce keys
     * @param valueMapper a mapping function to produce values
     * @param mergeFunction a merge function, used to resolve collisions between
     *                      values associated with the same key
     * @return a {@code Collector} which collects elements into an {@code ImmutableMap}
     */
    public static <T, K, V> Collector<T, ?, ImmutableMap<K, V>> toImmutableMap(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper, final BinaryOperator<V> mergeFunction) {
        final Collector<T, ?, Map<K, V>> downstream = toMap(keyMapper, valueMapper, mergeFunction);
        @SuppressWarnings("rawtypes")
        final Function<Map<K, V>, ImmutableMap<K, V>> finisher = (Function) ImmutableMap_Finisher;

        return collectingAndThen(downstream, finisher);
    }

    /**
     * Returns a {@code Collector} that accumulates elements into an unmodifiable {@code Map}
     * whose keys and values are the result of applying the provided mapping functions.
     * 
     * <p>This method delegates to the standard Java {@code Collectors.toUnmodifiableMap} method.
     * If duplicate keys are encountered, an {@code IllegalStateException} is thrown.
     * The returned map does not permit {@code null} keys or values.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create an unmodifiable map
     * Map<String, Integer> result = Stream.of("apple", "banana", "cherry")
     *     .collect(Collectors.toUnmodifiableMap(
     *         Function.identity(),
     *         String::length));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the output type of the key mapping function
     * @param <U> the output type of the value mapping function
     * @param keyMapper a mapping function to produce keys
     * @param valueMapper a mapping function to produce values
     * @return a {@code Collector} which collects elements into an unmodifiable {@code Map}
     * @see java.util.stream.Collectors#toUnmodifiableMap(Function, Function)
     */
    public static <T, K, U> Collector<T, ?, Map<K, U>> toUnmodifiableMap(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends U> valueMapper) {
        return java.util.stream.Collectors.toUnmodifiableMap(keyMapper, valueMapper);
    }

    /**
     * Returns a {@code Collector} that accumulates elements into an unmodifiable {@code Map}
     * whose keys and values are the result of applying the provided mapping functions,
     * with a merge function to handle duplicate keys.
     * 
     * <p>This method delegates to the standard Java {@code Collectors.toUnmodifiableMap} method.
     * When duplicate keys are encountered, the provided merge function is used to combine values.
     * The returned map does not permit {@code null} keys or values.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create an unmodifiable map, summing values for duplicate keys
     * Map<String, Integer> result = items.stream()
     *     .collect(Collectors.toUnmodifiableMap(
     *         Item::getCategory,
     *         Item::getValue,
     *         Integer::sum));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the output type of the key mapping function
     * @param <U> the output type of the value mapping function
     * @param keyMapper a mapping function to produce keys
     * @param valueMapper a mapping function to produce values
     * @param mergeFunction a merge function, used to resolve collisions between
     *                      values associated with the same key
     * @return a {@code Collector} which collects elements into an unmodifiable {@code Map}
     * @see java.util.stream.Collectors#toUnmodifiableMap(Function, Function, BinaryOperator)
     */
    public static <T, K, U> Collector<T, ?, Map<K, U>> toUnmodifiableMap(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends U> valueMapper, final BinaryOperator<U> mergeFunction) {
        return java.util.stream.Collectors.toUnmodifiableMap(keyMapper, valueMapper, mergeFunction);
    }

    /**
     * Returns a {@code Collector} that accumulates elements into a {@code LinkedHashMap}
     * whose keys and values are the result of applying the provided mapping functions.
     * 
     * <p>This collector maintains the insertion order of the elements. If duplicate keys
     * are encountered, an {@code IllegalStateException} is thrown.</p>
     * 
     * <p>The returned map is a {@code LinkedHashMap}, which maintains a doubly-linked list
     * running through all of its entries, defining the iteration ordering.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a LinkedHashMap maintaining insertion order
     * Map<String, Integer> result = Stream.of("apple", "banana", "cherry")
     *     .collect(Collectors.toLinkedHashMap(
     *         Function.identity(),
     *         String::length));
     * // Result maintains order: {apple=5, banana=6, cherry=6}
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the output type of the key mapping function
     * @param <V> the output type of the value mapping function
     * @param keyMapper a mapping function to produce keys
     * @param valueMapper a mapping function to produce values
     * @return a {@code Collector} which collects elements into a {@code LinkedHashMap}
     * @see #toMap(Function, Function)
     */
    public static <T, K, V> Collector<T, ?, Map<K, V>> toLinkedHashMap(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper) {
        final BinaryOperator<V> mergeFunction = Fn.throwingMerger();

        return toLinkedHashMap(keyMapper, valueMapper, mergeFunction);
    }

    /**
     * Returns a {@code Collector} that accumulates elements into a {@code LinkedHashMap}
     * whose keys and values are the result of applying the provided mapping functions,
     * with a merge function to handle duplicate keys.
     * 
     * <p>This collector maintains the insertion order of the elements. When duplicate keys
     * are encountered, the provided merge function is used to combine the values.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a LinkedHashMap, keeping the last value for duplicate keys
     * Map<String, String> result = Stream.of("a:1", "b:2", "a:3")
     *     .collect(Collectors.toLinkedHashMap(
     *         s -> s.split(":")[0],
     *         s -> s.split(":")[1],
     *         (v1, v2) -> v2));
     * // Result maintains order: {a=3, b=2}
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the output type of the key mapping function
     * @param <V> the output type of the value mapping function
     * @param keyMapper a mapping function to produce keys
     * @param valueMapper a mapping function to produce values
     * @param mergeFunction a merge function, used to resolve collisions between
     *                      values associated with the same key
     * @return a {@code Collector} which collects elements into a {@code LinkedHashMap}
     * @see #toMap(Function, Function, BinaryOperator)
     */
    public static <T, K, V> Collector<T, ?, Map<K, V>> toLinkedHashMap(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper, final BinaryOperator<V> mergeFunction) {
        final Supplier<Map<K, V>> mapFactory = Suppliers.ofLinkedHashMap();

        return toMap(keyMapper, valueMapper, mergeFunction, mapFactory);
    }

    /**
     * Returns a {@code Collector} that accumulates elements into a {@code ConcurrentMap}
     * whose keys and values are the result of applying the provided mapping functions.
     * 
     * <p>This collector is suitable for concurrent collection operations. If duplicate keys
     * are encountered, an {@code IllegalStateException} is thrown. The returned map is
     * thread-safe and can be safely accessed by multiple threads concurrently.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a thread-safe concurrent map
     * ConcurrentMap<String, Integer> result = Stream.of("apple", "banana", "cherry")
     *     .parallel()
     *     .collect(Collectors.toConcurrentMap(
     *         Function.identity(),
     *         String::length));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the output type of the key mapping function
     * @param <V> the output type of the value mapping function
     * @param keyMapper a mapping function to produce keys
     * @param valueMapper a mapping function to produce values
     * @return a {@code Collector} which collects elements into a {@code ConcurrentMap}
     */
    public static <T, K, V> Collector<T, ?, ConcurrentMap<K, V>> toConcurrentMap(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper) {
        final BinaryOperator<V> mergeFunction = Fn.throwingMerger();

        return toConcurrentMap(keyMapper, valueMapper, mergeFunction);
    }

    /**
     * Returns a {@code Collector} that accumulates elements into a {@code ConcurrentMap}
     * whose keys and values are the result of applying the provided mapping functions,
     * using the specified map factory.
     * 
     * <p>This collector allows you to specify the exact type of {@code ConcurrentMap} to use.
     * If duplicate keys are encountered, an {@code IllegalStateException} is thrown.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a specific type of concurrent map
     * ConcurrentSkipListMap<String, Integer> result = Stream.of("apple", "banana", "cherry")
     *     .collect(Collectors.toConcurrentMap(
     *         Function.identity(),
     *         String::length,
     *         ConcurrentSkipListMap::new));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the output type of the key mapping function
     * @param <V> the output type of the value mapping function
     * @param <M> the type of the resulting {@code ConcurrentMap}
     * @param keyMapper a mapping function to produce keys
     * @param valueMapper a mapping function to produce values
     * @param mapFactory a supplier providing a new empty {@code ConcurrentMap}
     *                   into which the results will be inserted
     * @return a {@code Collector} which collects elements into a {@code ConcurrentMap}
     */
    public static <T, K, V, M extends ConcurrentMap<K, V>> Collector<T, ?, M> toConcurrentMap(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper, final Supplier<? extends M> mapFactory) {
        final BinaryOperator<V> mergeFunction = Fn.throwingMerger();

        return toConcurrentMap(keyMapper, valueMapper, mergeFunction, mapFactory);
    }

    /**
     * Returns a {@code Collector} that accumulates elements into a {@code ConcurrentMap}
     * whose keys and values are the result of applying the provided mapping functions,
     * with a merge function to handle duplicate keys.
     * 
     * <p>This collector is suitable for concurrent collection operations. When duplicate keys
     * are encountered, the provided merge function is used to combine the values. The returned
     * map is thread-safe.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a concurrent map, summing values for duplicate keys
     * ConcurrentMap<String, Integer> result = items.parallelStream()
     *     .collect(Collectors.toConcurrentMap(
     *         Item::getCategory,
     *         Item::getValue,
     *         Integer::sum));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the output type of the key mapping function
     * @param <V> the output type of the value mapping function
     * @param keyMapper a mapping function to produce keys
     * @param valueMapper a mapping function to produce values
     * @param mergeFunction a merge function, used to resolve collisions between
     *                      values associated with the same key
     * @return a {@code Collector} which collects elements into a {@code ConcurrentMap}
     */
    public static <T, K, V> Collector<T, ?, ConcurrentMap<K, V>> toConcurrentMap(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper, final BinaryOperator<V> mergeFunction) {
        final Supplier<ConcurrentMap<K, V>> mapFactory = Suppliers.ofConcurrentMap();

        return toConcurrentMap(keyMapper, valueMapper, mergeFunction, mapFactory);
    }

    /**
     * Returns a {@code Collector} that accumulates elements into a {@code ConcurrentMap}
     * whose keys and values are the result of applying the provided mapping functions,
     * with a merge function to handle duplicate keys and a specified map factory.
     * 
     * <p>This is the most general form of concurrent map collector, allowing you to specify
     * all aspects of the collection process. The collector is suitable for parallel streams
     * and produces a thread-safe result.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a ConcurrentSkipListMap with custom merge logic
     * ConcurrentSkipListMap<String, String> result = items.parallelStream()
     *     .collect(Collectors.toConcurrentMap(
     *         Item::getKey,
     *         Item::getValue,
     *         (v1, v2) -> v1 + ";" + v2,
     *         ConcurrentSkipListMap::new));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the output type of the key mapping function
     * @param <V> the output type of the value mapping function
     * @param <M> the type of the resulting {@code ConcurrentMap}
     * @param keyMapper a mapping function to produce keys
     * @param valueMapper a mapping function to produce values
     * @param mergeFunction a merge function, used to resolve collisions between
     *                      values associated with the same key
     * @param mapFactory a supplier providing a new empty {@code ConcurrentMap}
     *                   into which the results will be inserted
     * @return a {@code Collector} which collects elements into a {@code ConcurrentMap}
     */
    public static <T, K, V, M extends ConcurrentMap<K, V>> Collector<T, ?, M> toConcurrentMap(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper, final BinaryOperator<V> mergeFunction, final Supplier<? extends M> mapFactory) {

        final BiConsumer<M, T> accumulator = (map, element) -> map.merge(keyMapper.apply(element), valueMapper.apply(element), mergeFunction);

        final BinaryOperator<M> combiner = mapMerger(mergeFunction);

        return create(mapFactory, accumulator, combiner, CH_CONCURRENT_ID);
    }

    /**
     * Returns a {@code Collector} that accumulates elements into a {@code BiMap}
     * whose keys and values are the result of applying the provided mapping functions.
     * 
     * <p>A {@code BiMap} is a map that preserves the uniqueness of its values as well
     * as that of its keys. If duplicate keys or values are encountered, an
     * {@code IllegalStateException} is thrown.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a bidirectional map
     * BiMap<Integer, String> result = Stream.of("apple", "banana", "cherry")
     *     .collect(Collectors.toBiMap(
     *         String::length,
     *         Function.identity()));
     * // Can retrieve key by value: result.inverse().get("apple") returns 5
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the output type of the key mapping function
     * @param <V> the output type of the value mapping function
     * @param keyMapper a mapping function to produce keys
     * @param valueMapper a mapping function to produce values
     * @return a {@code Collector} which collects elements into a {@code BiMap}
     * @throws IllegalStateException if duplicate keys or values are encountered
     */
    public static <T, K, V> Collector<T, ?, BiMap<K, V>> toBiMap(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper) {
        final BinaryOperator<V> mergeFunction = Fn.throwingMerger();

        return toBiMap(keyMapper, valueMapper, mergeFunction);
    }

    /**
     * Returns a {@code Collector} that accumulates elements into a {@code BiMap}
     * whose keys and values are the result of applying the provided mapping functions,
     * using the specified map factory.
     * 
     * <p>This collector allows you to specify the exact type of {@code BiMap} to use.
     * If duplicate keys or values are encountered, an {@code IllegalStateException} is thrown.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a specific type of BiMap
     * BiMap<String, Integer> result = Stream.of("apple", "banana", "cherry")
     *     .collect(Collectors.toBiMap(
     *         Function.identity(),
     *         String::length,
     *         HashBiMap::create));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the output type of the key mapping function
     * @param <V> the output type of the value mapping function
     * @param keyMapper a mapping function to produce keys
     * @param valueMapper a mapping function to produce values
     * @param mapFactory a supplier providing a new empty {@code BiMap}
     *                   into which the results will be inserted
     * @return a {@code Collector} which collects elements into a {@code BiMap}
     */
    public static <T, K, V> Collector<T, ?, BiMap<K, V>> toBiMap(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper, final Supplier<BiMap<K, V>> mapFactory) {
        final BinaryOperator<V> mergeFunction = Fn.throwingMerger();

        return toBiMap(keyMapper, valueMapper, mergeFunction, mapFactory);
    }

    /**
     * Returns a {@code Collector} that accumulates elements into a {@code BiMap}
     * whose keys and values are the result of applying the provided mapping functions,
     * with a merge function to handle duplicate keys.
     * 
     * <p>When duplicate keys are encountered, the provided merge function is used to
     * combine the values. Note that the resulting value must still be unique across
     * the map, or an {@code IllegalStateException} will be thrown.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a BiMap, using merge function for duplicate keys
     * BiMap<String, String> result = Stream.of("a:1", "b:2", "a:3")
     *     .collect(Collectors.toBiMap(
     *         s -> s.split(":")[0],
     *         s -> s.split(":")[1],
     *         (v1, v2) -> v1 + v2));
     * // Result: {a=13, b=2}
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the output type of the key mapping function
     * @param <V> the output type of the value mapping function
     * @param keyMapper a mapping function to produce keys
     * @param valueMapper a mapping function to produce values
     * @param mergeFunction a merge function, used to resolve collisions between
     *                      values associated with the same key
     * @return a {@code Collector} which collects elements into a {@code BiMap}
     * @throws IllegalStateException if duplicate values are encountered
     */
    public static <T, K, V> Collector<T, ?, BiMap<K, V>> toBiMap(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper, final BinaryOperator<V> mergeFunction) {
        final Supplier<BiMap<K, V>> mapFactory = Suppliers.ofBiMap();

        return toBiMap(keyMapper, valueMapper, mergeFunction, mapFactory);
    }

    /**
     * Returns a {@code Collector} that accumulates elements into a {@code BiMap}
     * whose keys and values are the result of applying the provided mapping functions,
     * with a merge function to handle duplicate keys and a specified map factory.
     * 
     * <p>This is the most general form of BiMap collector, allowing you to specify
     * all aspects of the collection process.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a custom BiMap with merge logic
     * BiMap<String, Integer> result = items.stream()
     *     .collect(Collectors.toBiMap(
     *         Item::getName,
     *         Item::getId,
     *         (id1, id2) -> Math.max(id1, id2),
     *         HashBiMap::create));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the output type of the key mapping function
     * @param <V> the output type of the value mapping function
     * @param keyMapper a mapping function to produce keys
     * @param valueMapper a mapping function to produce values
     * @param mergeFunction a merge function, used to resolve collisions between
     *                      values associated with the same key
     * @param mapFactory a supplier providing a new empty {@code BiMap}
     *                   into which the results will be inserted
     * @return a {@code Collector} which collects elements into a {@code BiMap}
     */
    public static <T, K, V> Collector<T, ?, BiMap<K, V>> toBiMap(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper, final BinaryOperator<V> mergeFunction, final Supplier<BiMap<K, V>> mapFactory) {
        // return toMap(keyMapper, valueMapper, mergeFunction, mapFactory);

        final BiConsumer<BiMap<K, V>, T> accumulator = (map, element) -> merge(map, keyMapper.apply(element), valueMapper.apply(element), mergeFunction);

        final BinaryOperator<BiMap<K, V>> combiner = biMapMerger(mergeFunction);

        return create(mapFactory, accumulator, combiner, CH_UNORDERED_ID);
    }

    static <K, V> void merge(final BiMap<K, V> biMap, final K key, final V value, final BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        N.checkArgNotNull(remappingFunction);

        final V oldValue = biMap.get(key);

        if (oldValue == null && !biMap.containsKey(key)) {
            biMap.put(key, value);
        } else {
            biMap.forcePut(key, remappingFunction.apply(oldValue, value));
        }
    }

    static <K, V> BinaryOperator<BiMap<K, V>> biMapMerger(final BinaryOperator<V> mergeFunction) {
        N.checkArgNotNull(mergeFunction);

        return (m1, m2) -> {
            for (final Map.Entry<K, V> e : m2.entrySet()) {
                final V oldValue = m1.get(e.getKey());

                if (oldValue == null && !m1.containsKey(e.getKey())) {
                    m1.put(e.getKey(), e.getValue());
                } else {
                    m1.forcePut(e.getKey(), mergeFunction.apply(oldValue, e.getValue()));
                }
            }

            return m1;
        };
    }

    /**
     * Returns a {@code Collector} that accumulates {@code Map.Entry} elements into a {@code ListMultimap}.
     * 
     * <p>This collector is designed to work with streams of {@code Map.Entry} objects,
     * using the entry's key and value directly. Multiple values for the same key are
     * collected into a list in encounter order.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert entries to a multimap
     * ListMultimap<String, Integer> result = entries.stream()
     *     .collect(Collectors.toMultimap());
     * // Entries with same key are grouped: {a=[1, 2], b=[3]}
     * }</pre>
     *
     * @param <K> the type of map keys
     * @param <V> the type of map values
     * @return a {@code Collector} which collects {@code Map.Entry} elements into a {@code ListMultimap}
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Collector<Map.Entry<K, V>, ?, ListMultimap<K, V>> toMultimap() {
        final Function<Map.Entry<? extends K, ? extends V>, ? extends K> keyMapper = (Function) Fn.key();
        final Function<Map.Entry<? extends K, ? extends V>, ? extends V> valueMapper = (Function) Fn.value();

        return toMultimap(keyMapper, valueMapper);
    }

    /**
     * Returns a {@code Collector} that accumulates {@code Map.Entry} elements into a {@code Multimap}
     * using the specified map factory.
     * 
     * <p>This collector allows you to specify the exact type of {@code Multimap} to use.
     * Multiple values for the same key are collected into the collection type specified
     * by the multimap implementation.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a specific type of multimap from entries
     * SetMultimap<String, Integer> result = entries.stream()
     *     .collect(Collectors.toMultimap(HashMultimap::create));
     * // Values for same key are stored in a set
     * }</pre>
     *
     * @param <K> the type of map keys
     * @param <V> the type of map values
     * @param <C> the type of collection used to store values
     * @param <M> the type of the resulting {@code Multimap}
     * @param mapFactory a supplier providing a new empty {@code Multimap}
     *                   into which the results will be inserted
     * @return a {@code Collector} which collects {@code Map.Entry} elements into a {@code Multimap}
     */
    @SuppressWarnings("rawtypes")
    public static <K, V, C extends Collection<V>, M extends Multimap<K, V, C>> Collector<Map.Entry<K, V>, ?, M> toMultimap(
            final Supplier<? extends M> mapFactory) {
        final Function<Map.Entry<? extends K, ? extends V>, ? extends K> keyMapper = (Function) Fn.key();
        final Function<Map.Entry<? extends K, ? extends V>, ? extends V> valueMapper = (Function) Fn.value();

        return toMultimap(keyMapper, valueMapper, mapFactory);
    }

    /**
     * Returns a {@code Collector} that accumulates elements into a {@code ListMultimap}
     * whose keys are the result of applying the provided mapping function to the input elements.
     * 
     * <p>The values in the multimap are the input elements themselves. Multiple elements
     * that map to the same key are collected into a list in encounter order.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group strings by their first character
     * ListMultimap<Character, String> result = Stream.of("apple", "apricot", "banana")
     *     .collect(Collectors.toMultimap(s -> s.charAt(0)));
     * // Result: {a=[apple, apricot], b=[banana]}
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the output type of the key mapping function
     * @param keyMapper a mapping function to produce keys
     * @return a {@code Collector} which collects elements into a {@code ListMultimap}
     */
    public static <T, K> Collector<T, ?, ListMultimap<K, T>> toMultimap(final Function<? super T, ? extends K> keyMapper) {
        final Function<? super T, ? extends T> valueMapper = Fn.identity();

        return toMultimap(keyMapper, valueMapper);
    }

    /**
     * Returns a {@code Collector} that accumulates elements into a {@code Multimap}
     * whose keys are the result of applying the provided mapping function to the input elements,
     * using the specified map factory.
     * 
     * <p>The values in the multimap are the input elements themselves. This collector
     * allows you to specify the exact type of {@code Multimap} to use.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group strings by length into a SetMultimap
     * SetMultimap<Integer, String> result = Stream.of("apple", "banana", "cherry", "apple")
     *     .collect(Collectors.toMultimap(
     *         String::length,
     *         HashMultimap::create));
     * // Result: {5=[apple], 6=[banana, cherry]} (duplicates removed by Set)
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the output type of the key mapping function
     * @param <C> the type of collection used to store values
     * @param <M> the type of the resulting {@code Multimap}
     * @param keyMapper a mapping function to produce keys
     * @param mapFactory a supplier providing a new empty {@code Multimap}
     *                   into which the results will be inserted
     * @return a {@code Collector} which collects elements into a {@code Multimap}
     */
    public static <T, K, C extends Collection<T>, M extends Multimap<K, T, C>> Collector<T, ?, M> toMultimap(final Function<? super T, ? extends K> keyMapper,
            final Supplier<? extends M> mapFactory) {
        final Function<? super T, ? extends T> valueMapper = Fn.identity();

        return toMultimap(keyMapper, valueMapper, mapFactory);
    }

    /**
     * Returns a {@code Collector} that accumulates elements into a {@code ListMultimap}
     * whose keys and values are the result of applying the provided mapping functions
     * to the input elements.
     * 
     * <p>Multiple values that map to the same key are collected into a list in encounter order.
     * This is the default multimap collector that creates a {@code ListMultimap}.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a multimap from person objects
     * ListMultimap<String, String> result = persons.stream()
     *     .collect(Collectors.toMultimap(
     *         Person::getDepartment,
     *         Person::getName));
     * // Groups names by department
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the output type of the key mapping function
     * @param <V> the output type of the value mapping function
     * @param keyMapper a mapping function to produce keys
     * @param valueMapper a mapping function to produce values
     * @return a {@code Collector} which collects elements into a {@code ListMultimap}
     */
    public static <T, K, V> Collector<T, ?, ListMultimap<K, V>> toMultimap(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper) {
        final Supplier<ListMultimap<K, V>> mapFactory = Suppliers.ofListMultimap();

        return toMultimap(keyMapper, valueMapper, mapFactory);
    }

    /**
     * Returns a {@code Collector} that accumulates elements into a {@code Multimap}
     * whose keys and values are the result of applying the provided mapping functions
     * to the input elements, using the specified map factory.
     * 
     * <p>This is the most general form of multimap collector, allowing you to specify
     * the key mapper, value mapper, and the exact type of {@code Multimap} to create.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a TreeMultimap with sorted keys
     * TreeMultimap<String, Integer> result = items.stream()
     *     .collect(Collectors.toMultimap(
     *         Item::getCategory,
     *         Item::getValue,
     *         TreeMultimap::create));
     * // Keys are sorted, values are also sorted within each key
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the output type of the key mapping function
     * @param <V> the output type of the value mapping function
     * @param <C> the type of collection used to store values
     * @param <M> the type of the resulting {@code Multimap}
     * @param keyMapper a mapping function to produce keys
     * @param valueMapper a mapping function to produce values
     * @param mapFactory a supplier providing a new empty {@code Multimap}
     *                   into which the results will be inserted
     * @return a {@code Collector} which collects elements into a {@code Multimap}
     */
    public static <T, K, V, C extends Collection<V>, M extends Multimap<K, V, C>> Collector<T, ?, M> toMultimap(
            final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper, final Supplier<? extends M> mapFactory) {
        final BiConsumer<M, T> accumulator = (map, element) -> map.put(keyMapper.apply(element), valueMapper.apply(element));

        final BinaryOperator<M> combiner = Collectors.multimapMerger();

        return create(mapFactory, accumulator, combiner, CH_UNORDERED_ID);
    }

    /**
     * Returns a {@code Collector} that accumulates elements into a {@code ListMultimap}
     * by applying a key mapping function and a flat mapping function that produces
     * multiple values for each input element.
     * 
     * <p>This collector is useful when each input element needs to be mapped to multiple
     * values for the same key. The flat value extractor should return a {@code Stream}
     * of values for each input element.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Map each person to multiple skills
     * ListMultimap<String, String> result = persons.stream()
     *     .collect(Collectors.flatMappingValueToMultimap(
     *         Person::getDepartment,
     *         person -> person.getSkills().stream()));
     * // Each person's skills are added under their department key
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the output type of the key mapping function
     * @param <V> the type of values in the resulting multimap
     * @param keyMapper a mapping function to produce keys
     * @param flatValueExtractor a function that returns a stream of values for each element
     * @return a {@code Collector} which collects elements into a {@code ListMultimap}
     * @see Collectors#toMultimap(Function, Function)
     */
    public static <T, K, V> Collector<T, ?, ListMultimap<K, V>> flatMappingValueToMultimap(final Function<? super T, K> keyMapper,
            final Function<? super T, ? extends Stream<? extends V>> flatValueExtractor) {
        return flatMappingValueToMultimap(keyMapper, flatValueExtractor, Suppliers.ofListMultimap());
    }

    /**
     * Returns a {@code Collector} that accumulates elements into a {@code Multimap}
     * by applying a key mapping function and a flat mapping function that produces
     * multiple values for each input element, using the specified map factory.
     * 
     * <p>This collector allows you to specify the exact type of {@code Multimap} to use
     * while flat mapping values. The flat value extractor should return a {@code Stream}
     * of values for each input element.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a SetMultimap with flat mapped values
     * SetMultimap<String, String> result = items.stream()
     *     .collect(Collectors.flatMappingValueToMultimap(
     *         Item::getCategory,
     *         item -> item.getTags().stream(),
     *         HashMultimap::create));
     * // Tags are deduplicated within each category
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the output type of the key mapping function
     * @param <V> the type of values in the resulting multimap
     * @param <C> the type of collection used to store values
     * @param <M> the type of the resulting {@code Multimap}
     * @param keyMapper a mapping function to produce keys
     * @param flatValueExtractor a function that returns a stream of values for each element
     * @param mapFactory a supplier providing a new empty {@code Multimap}
     *                   into which the results will be inserted
     * @return a {@code Collector} which collects elements into a {@code Multimap}
     * @see Collectors#toMultimap(Function, Function, Supplier)
     */
    public static <T, K, V, C extends Collection<V>, M extends Multimap<K, V, C>> Collector<T, ?, M> flatMappingValueToMultimap(
            final Function<? super T, K> keyMapper, final Function<? super T, ? extends Stream<? extends V>> flatValueExtractor,
            final Supplier<? extends M> mapFactory) {

        final BiConsumer<M, T> accumulator = (map, element) -> {
            final K key = keyMapper.apply(element);

            try (Stream<? extends V> stream = flatValueExtractor.apply(element)) {
                if (stream.isParallel()) {
                    stream.sequential().forEach(value -> map.put(key, value));
                } else {
                    stream.forEach(value -> map.put(key, value));
                }
            }
        };

        final BinaryOperator<M> combiner = Collectors.multimapMerger();

        return create(mapFactory, accumulator, combiner, CH_UNORDERED_ID);
    }

    /**
     * Returns a {@code Collector} that accumulates elements into a {@code ListMultimap}
     * by applying a key mapping function and a flat mapping function that produces
     * a collection of values for each input element.
     * 
     * <p>This collector is similar to {@link #flatMappingValueToMultimap} but works with
     * collections instead of streams. Each input element is mapped to a collection of values
     * that are all associated with the same key.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Map each person to their list of phone numbers
     * ListMultimap<String, String> result = persons.stream()
     *     .collect(Collectors.flatmappingValueToMultimap(
     *         Person::getDepartment,
     *         Person::getPhoneNumbers));
     * // All phone numbers are added under the person's department
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the output type of the key mapping function
     * @param <V> the type of values in the resulting multimap
     * @param keyMapper a mapping function to produce keys
     * @param flatValueExtractor a function that returns a collection of values for each element
     * @return a {@code Collector} which collects elements into a {@code ListMultimap}
     * @see Collectors#toMultimap(Function, Function)
     */
    public static <T, K, V> Collector<T, ?, ListMultimap<K, V>> flatmappingValueToMultimap(final Function<? super T, K> keyMapper, // NOSONAR
            final Function<? super T, ? extends Collection<? extends V>> flatValueExtractor) {
        return flatmappingValueToMultimap(keyMapper, flatValueExtractor, Suppliers.ofListMultimap());
    }

    /**
     * Returns a {@code Collector} that accumulates elements into a {@code Multimap}
     * by applying a key mapping function and a flat mapping function that produces
     * a collection of values for each input element, using the specified map factory.
     * 
     * <p>This collector allows you to specify the exact type of {@code Multimap} to use
     * while flat mapping collections of values.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a TreeMultimap with flat mapped collections
     * TreeMultimap<String, String> result = items.stream()
     *     .collect(Collectors.flatmappingValueToMultimap(
     *         Item::getCategory,
     *         Item::getRelatedItems,
     *         TreeMultimap::create));
     * // Related items are sorted within each category
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the output type of the key mapping function
     * @param <V> the type of values in the resulting multimap
     * @param <C> the type of collection used to store values
     * @param <M> the type of the resulting {@code Multimap}
     * @param keyMapper a mapping function to produce keys
     * @param flatValueExtractor a function that returns a collection of values for each element
     * @param mapFactory a supplier providing a new empty {@code Multimap}
     *                   into which the results will be inserted
     * @return a {@code Collector} which collects elements into a {@code Multimap}
     * @see Collectors#toMultimap(Function, Function, Supplier)
     */
    public static <T, K, V, C extends Collection<V>, M extends Multimap<K, V, C>> Collector<T, ?, M> flatmappingValueToMultimap( // NOSONAR
            final Function<? super T, K> keyMapper, final Function<? super T, ? extends Collection<? extends V>> flatValueExtractor,
            final Supplier<? extends M> mapFactory) {

        final BiConsumer<M, T> accumulator = (map, element) -> {
            final K key = keyMapper.apply(element);
            final Collection<? extends V> values = flatValueExtractor.apply(element);

            if (N.notEmpty(values)) {
                for (final V value : values) {
                    map.put(key, value);
                }
            }
        };

        final BinaryOperator<M> combiner = Collectors.multimapMerger();

        return create(mapFactory, accumulator, combiner, CH_UNORDERED_ID);
    }

    /**
     * Returns a {@code Collector} that accumulates elements into a {@code ListMultimap}
     * by extracting multiple keys from each element using a flat mapping function.
     * 
     * <p>This collector is useful when each input element can be associated with multiple
     * keys. The flat key extractor function returns a stream of keys for each element,
     * and the value mapper extracts the value to be associated with those keys. Each
     * key-value pair is added to the resulting multimap.</p>
     * 
     * <p>For parallel streams, the sequential processing of keys ensures thread safety
     * when populating the multimap. The resulting multimap allows duplicate key-value
     * pairs and preserves the encounter order of values for each key.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Map products to multiple categories
     * ListMultimap<String, Product> productsByCategory = products.stream()
     *     .collect(Collectors.flatMappingKeyToMultimap(
     *         p -> p.getCategories().stream(),
     *         Function.identity()
     *     ));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the type of keys in the multimap
     * @param <V> the type of values in the multimap
     * @param flatKeyExtractor a function that extracts a stream of keys from each element
     * @param valueMapper a function to produce values for the multimap
     * @return a {@code Collector} which collects elements into a {@code ListMultimap}
     *         using flat key extraction
     * @see Collectors#toMultimap(Function, Function)
     */
    public static <T, K, V> Collector<T, ?, ListMultimap<K, V>> flatMappingKeyToMultimap(final Function<? super T, Stream<? extends K>> flatKeyExtractor,
            final Function<? super T, V> valueMapper) {
        return flatMappingKeyToMultimap(flatKeyExtractor, valueMapper, Suppliers.ofListMultimap());
    }

    /**
     * Returns a {@code Collector} that accumulates elements into a custom {@code Multimap}
     * implementation by extracting multiple keys from each element using a flat mapping function.
     * 
     * <p>This collector provides full control over the type of multimap created by accepting
     * a custom map factory. The flat key extractor function returns a stream of keys for each
     * element, and the value mapper extracts the value to be associated with those keys.
     * Each key-value pair is added to the resulting multimap.</p>
     * 
     * <p>For parallel streams, the sequential processing of keys ensures thread safety
     * when populating the multimap. The behavior of the resulting multimap (such as whether
     * it allows duplicate key-value pairs) depends on the implementation provided by the
     * map factory.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Map users to multiple roles using a SetMultimap
     * SetMultimap<String, User> usersByRole = users.stream()
     *     .collect(Collectors.flatMappingKeyToMultimap(
     *         u -> u.getRoles().stream(),
     *         Function.identity(),
     *         Suppliers.ofSetMultimap()
     *     ));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the type of keys in the multimap
     * @param <V> the type of values in the multimap
     * @param <C> the type of collection used to store values for each key
     * @param <M> the type of the resulting multimap
     * @param flatKeyExtractor a function that extracts a stream of keys from each element
     * @param valueMapper a function to produce values for the multimap
     * @param mapFactory a supplier providing a new empty multimap into which the results
     *                   will be inserted
     * @return a {@code Collector} which collects elements into a multimap using flat
     *         key extraction
     * @see Collectors#toMultimap(Function, Function, Supplier)
     */
    public static <T, K, V, C extends Collection<V>, M extends Multimap<K, V, C>> Collector<T, ?, M> flatMappingKeyToMultimap(
            final Function<? super T, Stream<? extends K>> flatKeyExtractor, final Function<? super T, V> valueMapper, final Supplier<? extends M> mapFactory) {

        final BiConsumer<M, T> accumulator = (map, element) -> {
            final V value = valueMapper.apply(element);

            try (Stream<? extends K> stream = flatKeyExtractor.apply(element)) {
                if (stream.isParallel()) {
                    stream.sequential().forEach(key -> map.put(key, value));
                } else {
                    stream.forEach(key -> map.put(key, value));
                }
            }
        };

        final BinaryOperator<M> combiner = Collectors.multimapMerger();

        return create(mapFactory, accumulator, combiner, CH_UNORDERED_ID);
    }

    /**
     * Returns a {@code Collector} that accumulates elements into a {@code ListMultimap}
     * by extracting multiple keys from each element using a collection-based mapping function.
     * 
     * <p>This collector is similar to {@link #flatMappingKeyToMultimap(Function, Function)}
     * but accepts a function that returns a collection of keys instead of a stream. This
     * can be more convenient when the keys are already available as a collection. Each
     * key from the collection is associated with the value extracted by the value mapper.</p>
     * 
     * <p>The method handles empty key collections gracefully by not adding any entries
     * to the multimap for such elements. The resulting multimap allows duplicate key-value
     * pairs and preserves the encounter order of values for each key.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Map employees to multiple departments
     * ListMultimap<String, Employee> employeesByDept = employees.stream()
     *     .collect(Collectors.flatmappingKeyToMultimap(
     *         Employee::getDepartments,
     *         Function.identity()
     *     ));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the type of keys in the multimap
     * @param <V> the type of values in the multimap
     * @param flatKeyExtractor a function that extracts a collection of keys from each element
     * @param valueMapper a function to produce values for the multimap
     * @return a {@code Collector} which collects elements into a {@code ListMultimap}
     *         using collection-based key extraction
     * @see Collectors#toMultimap(Function, Function)
     */
    public static <T, K, V> Collector<T, ?, ListMultimap<K, V>> flatmappingKeyToMultimap( // NOSONAR
            final Function<? super T, ? extends Collection<? extends K>> flatKeyExtractor, final Function<? super T, V> valueMapper) {
        return flatmappingKeyToMultimap(flatKeyExtractor, valueMapper, Suppliers.ofListMultimap());
    }

    /**
     * Returns a {@code Collector} that accumulates elements into a custom {@code Multimap}
     * implementation by extracting multiple keys from each element using a collection-based
     * mapping function.
     * 
     * <p>This collector provides full control over the type of multimap created by accepting
     * a custom map factory. The flat key extractor function returns a collection of keys for
     * each element, and the value mapper extracts the value to be associated with those keys.
     * Each key from the collection is paired with the extracted value and added to the multimap.</p>
     * 
     * <p>The method handles empty key collections gracefully by not adding any entries
     * to the multimap for such elements. The behavior of the resulting multimap (such as
     * whether it allows duplicate key-value pairs) depends on the implementation provided
     * by the map factory.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Map documents to multiple tags using a custom multimap
     * SetMultimap<String, Document> docsByTag = documents.stream()
     *     .collect(Collectors.flatmappingKeyToMultimap(
     *         Document::getTags,
     *         Function.identity(),
     *         Suppliers.ofLinkedSetMultimap()
     *     ));
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <K> the type of keys in the multimap
     * @param <V> the type of values in the multimap
     * @param <C> the type of collection used to store values for each key
     * @param <M> the type of the resulting multimap
     * @param flatKeyExtractor a function that extracts a collection of keys from each element
     * @param valueMapper a function to produce values for the multimap
     * @param mapFactory a supplier providing a new empty multimap into which the results
     *                   will be inserted
     * @return a {@code Collector} which collects elements into a multimap using
     *         collection-based key extraction
     * @see Collectors#toMultimap(Function, Function, Supplier)
     */
    public static <T, K, V, C extends Collection<V>, M extends Multimap<K, V, C>> Collector<T, ?, M> flatmappingKeyToMultimap( // NOSONAR
            final Function<? super T, ? extends Collection<? extends K>> flatKeyExtractor, final Function<? super T, V> valueMapper,
            final Supplier<? extends M> mapFactory) {

        final BiConsumer<M, T> accumulator = (map, element) -> {
            final V value = valueMapper.apply(element);
            final Collection<? extends K> keys = flatKeyExtractor.apply(element);

            if (N.notEmpty(keys)) {
                for (final K key : keys) {
                    map.put(key, value);
                }
            }
        };

        final BinaryOperator<M> combiner = Collectors.multimapMerger();

        return create(mapFactory, accumulator, combiner, CH_UNORDERED_ID);
    }

    static <K, V> void replaceAll(final Map<K, V> map, final BiFunction<? super K, ? super V, ? extends V> function) {
        N.checkArgNotNull(function);

        try {
            for (final Map.Entry<K, V> entry : map.entrySet()) {
                entry.setValue(function.apply(entry.getKey(), entry.getValue()));
            }
        } catch (final IllegalStateException ise) {
            throw new ConcurrentModificationException(ise);
        }
    }

    private static <K, V, M extends Map<K, V>> BinaryOperator<M> mapMerger(final BinaryOperator<V> mergeFunction) {
        N.checkArgNotNull(mergeFunction);

        return (m1, m2) -> {
            for (final Map.Entry<K, V> e : m2.entrySet()) {
                final V oldValue = m1.get(e.getKey());

                if (oldValue == null && !m1.containsKey(e.getKey())) {
                    m1.put(e.getKey(), e.getValue());
                } else {
                    m1.put(e.getKey(), mergeFunction.apply(oldValue, e.getValue()));
                }
            }
            return m1;
        };
    }

    private static <K, U, V extends Collection<U>, M extends Multimap<K, U, V>> BinaryOperator<M> multimapMerger() {
        return (m1, m2) -> {
            K key = null;
            V value = null;
            for (final Map.Entry<K, V> e : m2.entrySet()) {
                key = e.getKey();
                value = N.requireNonNull(e.getValue(), "Multimap value cannot be null");

                if (N.notEmpty(value)) {
                    final V oldValue = m1.get(key);

                    if (oldValue == null) {
                        m1.putMany(key, value);
                    } else {
                        oldValue.addAll(value);
                    }
                }
            }
            return m1;
        };
    }

    static <K, V> void merge(final Map<K, V> map, final K key, final V value, final BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        N.checkArgNotNull(remappingFunction);

        final V oldValue = map.get(key);

        if (oldValue == null && !map.containsKey(key)) {
            map.put(key, value);
        } else {
            map.put(key, remappingFunction.apply(oldValue, value));
        }
    }

    /**
     * Returns a {@code Collector} that performs a reduction of its input elements under
     * two specified downstream collectors, then merges their results using the provided
     * merger function.
     * 
     * <p>This collector allows you to perform two different collection operations on the
     * same stream of elements in a single pass. Each element is processed by both downstream
     * collectors independently, and their final results are combined using the merger function.
     * This is more efficient than processing the stream twice.</p>
     * 
     * <p>The characteristics of the returned collector are determined by the intersection
     * of characteristics from both downstream collectors, excluding IDENTITY_FINISH.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Calculate both sum and count in one pass
     * Pair<Integer, Long> result = Stream.of(1, 2, 3, 4, 5)
     *     .collect(Collectors.teeing(
     *         Collectors.summingInt(i -> i),
     *         Collectors.counting(),
     *         (sum, count) -> Pair.of(sum, count)
     *     ));
     * // Result: Pair(15, 5)
     * }</pre>
     *
     * @param <T> the type of input elements
     * @param <R1> the result type of the first downstream collector
     * @param <R2> the result type of the second downstream collector
     * @param <R> the final result type
     * @param downstream1 the first downstream collector
     * @param downstream2 the second downstream collector
     * @param merger a function to merge the results of the two downstream collectors
     * @return a {@code Collector} which performs the reduction of its input elements
     *         under the two downstream collectors and merges the results
     */
    public static <T, R1, R2, R> Collector<T, ?, R> teeing(final Collector<? super T, ?, R1> downstream1, final Collector<? super T, ?, R2> downstream2,
            final BiFunction<? super R1, ? super R2, R> merger) {
        return java.util.stream.Collectors.teeing(downstream1, downstream2, merger);
    }

    /**
     * Extended collector factory providing advanced collectors for multi-value operations.
     *
     * <p>This class extends {@code Collectors} to provide additional collector methods that
     * operate on multiple values or mappers simultaneously. These collectors are particularly
     * useful when you need to perform multiple aggregations on the same stream elements in
     * a single pass, avoiding the need to process the stream multiple times.</p>
     *
     * <p>Key features include:</p>
     * <ul>
     * <li>Multiple summing operations (e.g., summing 2 or 3 different properties at once)</li>
     * <li>Multiple averaging operations with tuple results</li>
     * <li>Combining multiple collectors with custom merging functions</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Calculate both sum and average in one pass
     * Tuple2<Integer, Integer> result = orders.stream()
     *     .collect(MoreCollectors.summingInt(
     *         Order::getQuantity,
     *         Order::getPrice
     *     ));
     * }</pre>
     *
     * @see Collectors
     */
    public static final class MoreCollectors extends Collectors {
        MoreCollectors() {
            // for extension.
        }

        /**
         * Returns a {@code Collector} that computes the sum of two integer-valued functions
         * applied to the input elements.
         * 
         * <p>This collector applies two different integer mapping functions to each input
         * element and maintains separate sums for each. The result is a tuple containing
         * both sums. This is useful when you need to calculate multiple integer sums from
         * the same stream in a single pass.</p>
         * 
         * <p>The collector handles integer overflow by wrapping around as per Java's
         * integer arithmetic rules. The computation is performed in encounter order
         * and is not affected by stream ordering.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Calculate total price and quantity from orders
         * Tuple2<Integer, Integer> totals = orders.stream()
         *     .collect(MoreCollectors.summingInt(
         *         Order::getPrice,
         *         Order::getQuantity
         *     ));
         * }</pre>
         *
         * @param <T> the type of input elements
         * @param mapper1 a function extracting the first integer property to sum
         * @param mapper2 a function extracting the second integer property to sum
         * @return a {@code Collector} that produces a tuple of the two sums
         */
        public static <T> Collector<T, ?, Tuple2<Integer, Integer>> summingInt(final ToIntFunction<? super T> mapper1, final ToIntFunction<? super T> mapper2) {
            final BiConsumer<int[], T> accumulator = (a, t) -> {
                a[0] += mapper1.applyAsInt(t);
                a[1] += mapper2.applyAsInt(t);
            };

            return create(SummingInt_Supplier_2, accumulator, SummingInt_Combiner_2, SummingInt_Finisher_2, CH_UNORDERED_NOID);
        }

        /**
         * Returns a {@code Collector} that computes the sum of three integer-valued functions
         * applied to the input elements.
         * 
         * <p>This collector applies three different integer mapping functions to each input
         * element and maintains separate sums for each. The result is a tuple containing
         * all three sums. This is useful when you need to calculate multiple integer sums
         * from the same stream in a single pass.</p>
         * 
         * <p>The collector handles integer overflow by wrapping around as per Java's
         * integer arithmetic rules. The computation is performed in encounter order
         * and is not affected by stream ordering.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Calculate total sales, units, and returns from transactions
         * Tuple3<Integer, Integer, Integer> stats = transactions.stream()
         *     .collect(MoreCollectors.summingInt(
         *         Transaction::getSalesAmount,
         *         Transaction::getUnitsSold,
         *         Transaction::getReturns
         *     ));
         * }</pre>
         *
         * @param <T> the type of input elements
         * @param mapper1 a function extracting the first integer property to sum
         * @param mapper2 a function extracting the second integer property to sum
         * @param mapper3 a function extracting the third integer property to sum
         * @return a {@code Collector} that produces a tuple of the three sums
         */
        public static <T> Collector<T, ?, Tuple3<Integer, Integer, Integer>> summingInt(final ToIntFunction<? super T> mapper1,
                final ToIntFunction<? super T> mapper2, final ToIntFunction<? super T> mapper3) {
            final BiConsumer<int[], T> accumulator = (a, t) -> {
                a[0] += mapper1.applyAsInt(t);
                a[1] += mapper2.applyAsInt(t);
                a[2] += mapper3.applyAsInt(t);
            };

            return create(SummingInt_Supplier_3, accumulator, SummingInt_Combiner_3, SummingInt_Finisher_3, CH_UNORDERED_NOID);
        }

        /**
         * Returns a {@code Collector} that computes the sum of two integer-valued functions
         * applied to the input elements, with the results widened to {@code long}.
         * 
         * <p>This collector is similar to {@link #summingInt(ToIntFunction, ToIntFunction)}
         * but returns the sums as {@code long} values to avoid integer overflow issues.
         * This is particularly useful when summing values that might exceed the range
         * of {@code int}.</p>
         * 
         * <p>The computation is performed in encounter order and is not affected by
         * stream ordering. Each integer value is widened to long before addition.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Calculate large totals without overflow risk
         * Tuple2<Long, Long> largeTotals = largeDataset.stream()
         *     .collect(MoreCollectors.summingIntToLong(
         *         Data::getCount,
         *         Data::getValue
         *     ));
         * }</pre>
         *
         * @param <T> the type of input elements
         * @param mapper1 a function extracting the first integer property to sum
         * @param mapper2 a function extracting the second integer property to sum
         * @return a {@code Collector} that produces a tuple of the two sums as longs
         */
        public static <T> Collector<T, ?, Tuple2<Long, Long>> summingIntToLong(final ToIntFunction<? super T> mapper1, final ToIntFunction<? super T> mapper2) {
            final BiConsumer<long[], T> accumulator = (a, t) -> {
                a[0] += mapper1.applyAsInt(t);
                a[1] += mapper2.applyAsInt(t);
            };

            return create(SummingIntToLong_Supplier_2, accumulator, SummingIntToLong_Combiner_2, SummingIntToLong_Finisher_2, CH_UNORDERED_NOID);
        }

        /**
         * Returns a {@code Collector} that computes the sum of three integer-valued functions
         * applied to the input elements, with the results widened to {@code long}.
         * 
         * <p>This collector is similar to {@link #summingInt(ToIntFunction, ToIntFunction, ToIntFunction)}
         * but returns the sums as {@code long} values to avoid integer overflow issues.
         * This is particularly useful when summing values that might exceed the range
         * of {@code int}.</p>
         * 
         * <p>The computation is performed in encounter order and is not affected by
         * stream ordering. Each integer value is widened to long before addition.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Calculate large metrics without overflow
         * Tuple3<Long, Long, Long> metrics = dataset.stream()
         *     .collect(MoreCollectors.summingIntToLong(
         *         Data::getViews,
         *         Data::getClicks,
         *         Data::getConversions
         *     ));
         * }</pre>
         *
         * @param <T> the type of input elements
         * @param mapper1 a function extracting the first integer property to sum
         * @param mapper2 a function extracting the second integer property to sum
         * @param mapper3 a function extracting the third integer property to sum
         * @return a {@code Collector} that produces a tuple of the three sums as longs
         */
        public static <T> Collector<T, ?, Tuple3<Long, Long, Long>> summingIntToLong(final ToIntFunction<? super T> mapper1,
                final ToIntFunction<? super T> mapper2, final ToIntFunction<? super T> mapper3) {
            final BiConsumer<long[], T> accumulator = (a, t) -> {
                a[0] += mapper1.applyAsInt(t);
                a[1] += mapper2.applyAsInt(t);
                a[2] += mapper3.applyAsInt(t);
            };

            return create(SummingIntToLong_Supplier_3, accumulator, SummingIntToLong_Combiner_3, SummingIntToLong_Finisher_3, CH_UNORDERED_NOID);
        }

        /**
         * Returns a {@code Collector} that computes the sum of two long-valued functions
         * applied to the input elements.
         * 
         * <p>This collector applies two different long mapping functions to each input
         * element and maintains separate sums for each. The result is a tuple containing
         * both sums. This is useful when you need to calculate multiple long sums from
         * the same stream in a single pass.</p>
         * 
         * <p>The collector handles long overflow by wrapping around as per Java's
         * long arithmetic rules. The computation is performed in encounter order
         * and is not affected by stream ordering.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Calculate total bytes read and written
         * Tuple2<Long, Long> ioStats = files.stream()
         *     .collect(MoreCollectors.summingLong(
         *         File::getBytesRead,
         *         File::getBytesWritten
         *     ));
         * }</pre>
         *
         * @param <T> the type of input elements
         * @param mapper1 a function extracting the first long property to sum
         * @param mapper2 a function extracting the second long property to sum
         * @return a {@code Collector} that produces a tuple of the two sums
         */
        public static <T> Collector<T, ?, Tuple2<Long, Long>> summingLong(final ToLongFunction<? super T> mapper1, final ToLongFunction<? super T> mapper2) {
            final BiConsumer<long[], T> accumulator = (a, t) -> {
                a[0] += mapper1.applyAsLong(t);
                a[1] += mapper2.applyAsLong(t);
            };

            return create(SummingLong_Supplier_2, accumulator, SummingLong_Combiner_2, SummingLong_Finisher_2, CH_UNORDERED_NOID);
        }

        /**
         * Returns a {@code Collector} that computes the sum of three long-valued functions
         * applied to the input elements.
         * 
         * <p>This collector applies three different long mapping functions to each input
         * element and maintains separate sums for each. The result is a tuple containing
         * all three sums. This is useful when you need to calculate multiple long sums
         * from the same stream in a single pass.</p>
         * 
         * <p>The collector handles long overflow by wrapping around as per Java's
         * long arithmetic rules. The computation is performed in encounter order
         * and is not affected by stream ordering.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Calculate storage metrics
         * Tuple3<Long, Long, Long> storage = servers.stream()
         *     .collect(MoreCollectors.summingLong(
         *         Server::getTotalSpace,
         *         Server::getUsedSpace,
         *         Server::getFreeSpace
         *     ));
         * }</pre>
         *
         * @param <T> the type of input elements
         * @param mapper1 a function extracting the first long property to sum
         * @param mapper2 a function extracting the second long property to sum
         * @param mapper3 a function extracting the third long property to sum
         * @return a {@code Collector} that produces a tuple of the three sums
         */
        public static <T> Collector<T, ?, Tuple3<Long, Long, Long>> summingLong(final ToLongFunction<? super T> mapper1,
                final ToLongFunction<? super T> mapper2, final ToLongFunction<? super T> mapper3) {
            final BiConsumer<long[], T> accumulator = (a, t) -> {
                a[0] += mapper1.applyAsLong(t);
                a[1] += mapper2.applyAsLong(t);
                a[2] += mapper3.applyAsLong(t);
            };

            return create(SummingLong_Supplier_3, accumulator, SummingLong_Combiner_3, SummingLong_Finisher_3, CH_UNORDERED_NOID);
        }

        /**
         * Returns a {@code Collector} that computes the sum of two double-valued functions
         * applied to the input elements.
         *
         * <p>This collector applies two different double mapping functions to each input
         * element and maintains separate sums for each. The result is a tuple containing
         * both sums. This is useful when you need to calculate multiple double sums from
         * the same stream in a single pass.</p>
         *
         * <p>The computation is performed in encounter order and is not affected by
         * stream ordering. Special floating-point values (NaN, infinity) are handled
         * according to IEEE 754 standard.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Calculate total revenue and tax
         * Tuple2<Double, Double> financials = sales.stream()
         *     .collect(MoreCollectors.summingDouble(
         *         Sale::getRevenue,
         *         Sale::getTax
         *     ));
         * }</pre>
         *
         * @param <T> the type of input elements
         * @param mapper1 a function extracting the first double property to sum
         * @param mapper2 a function extracting the second double property to sum
         * @return a {@code Collector} that produces a tuple of the two sums
         */
        public static <T> Collector<T, ?, Tuple2<Double, Double>> summingDouble(final ToDoubleFunction<? super T> mapper1,
                final ToDoubleFunction<? super T> mapper2) {
            final BiConsumer<KahanSummation[], T> accumulator = (a, t) -> {
                a[0].add(mapper1.applyAsDouble(t));
                a[1].add(mapper2.applyAsDouble(t));
            };

            return create(SummingDouble_Supplier_2, accumulator, SummingDouble_Combiner_2, SummingDouble_Finisher_2, CH_UNORDERED_NOID);
        }

        /**
         * Returns a {@code Collector} that computes the sum of three double-valued functions
         * applied to the input elements.
         *
         * <p>This collector applies three different double mapping functions to each input
         * element and maintains separate sums for each. The result is a tuple containing
         * all three sums. This is useful when you need to calculate multiple double sums
         * from the same stream in a single pass.</p>
         *
         * <p>The computation is performed in encounter order and is not affected by
         * stream ordering. Special floating-point values (NaN, infinity) are handled
         * according to IEEE 754 standard.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Calculate geometric measurements
         * Tuple3<Double, Double, Double> dims = shapes.stream()
         *     .collect(MoreCollectors.summingDouble(
         *         Shape::getArea,
         *         Shape::getPerimeter,
         *         Shape::getVolume
         *     ));
         * }</pre>
         *
         * @param <T> the type of input elements
         * @param mapper1 a function extracting the first double property to sum
         * @param mapper2 a function extracting the second double property to sum
         * @param mapper3 a function extracting the third double property to sum
         * @return a {@code Collector} that produces a tuple of the three sums
         */
        public static <T> Collector<T, ?, Tuple3<Double, Double, Double>> summingDouble(final ToDoubleFunction<? super T> mapper1,
                final ToDoubleFunction<? super T> mapper2, final ToDoubleFunction<? super T> mapper3) {
            final BiConsumer<KahanSummation[], T> accumulator = (a, t) -> {
                a[0].add(mapper1.applyAsDouble(t));
                a[1].add(mapper2.applyAsDouble(t));
                a[2].add(mapper3.applyAsDouble(t));
            };

            return create(SummingDouble_Supplier_3, accumulator, SummingDouble_Combiner_3, SummingDouble_Finisher_3, CH_UNORDERED_NOID);
        }

        /**
         * Returns a {@code Collector} that computes the sum of two {@code BigInteger}-valued
         * functions applied to the input elements.
         * 
         * <p>This collector maintains arbitrary precision throughout the calculation,
         * making it suitable for very large integer values that exceed the range of
         * primitive types. The result is a tuple containing both sums. Null values
         * returned by the mapping functions will cause a NullPointerException.</p>
         * 
         * <p>The computation is performed in encounter order and is not affected by
         * stream ordering. BigInteger arithmetic is exact and does not overflow.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Calculate large cryptographic values
         * Tuple2<BigInteger, BigInteger> crypto = keys.stream()
         *     .collect(MoreCollectors.summingBigInteger(
         *         Key::getPublicExponent,
         *         Key::getPrivateExponent
         *     ));
         * }</pre>
         *
         * @param <T> the type of input elements
         * @param mapper1 a function extracting the first BigInteger property to sum
         * @param mapper2 a function extracting the second BigInteger property to sum
         * @return a {@code Collector} that produces a tuple of the two sums
         */
        public static <T> Collector<T, ?, Tuple2<BigInteger, BigInteger>> summingBigInteger(final Function<? super T, BigInteger> mapper1,
                final Function<? super T, BigInteger> mapper2) {
            final BiConsumer<BigInteger[], T> accumulator = (a, t) -> {
                a[0] = a[0].add(mapper1.apply(t));
                a[1] = a[1].add(mapper2.apply(t));
            };

            return create(SummingBigInteger_Supplier_2, accumulator, SummingBigInteger_Combiner_2, SummingBigInteger_Finisher_2, CH_UNORDERED_NOID);
        }

        /**
         * Returns a {@code Collector} that computes the sum of three {@code BigInteger}-valued
         * functions applied to the input elements.
         * 
         * <p>This collector maintains arbitrary precision throughout the calculation,
         * making it suitable for very large integer values that exceed the range of
         * primitive types. The result is a tuple containing all three sums. Null values
         * returned by the mapping functions will cause a NullPointerException.</p>
         * 
         * <p>The computation is performed in encounter order and is not affected by
         * stream ordering. BigInteger arithmetic is exact and does not overflow.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Calculate blockchain statistics
         * Tuple3<BigInteger, BigInteger, BigInteger> blockchain = blocks.stream()
         *     .collect(MoreCollectors.summingBigInteger(
         *         Block::getDifficulty,
         *         Block::getReward,
         *         Block::getGasUsed
         *     ));
         * }</pre>
         *
         * @param <T> the type of input elements
         * @param mapper1 a function extracting the first BigInteger property to sum
         * @param mapper2 a function extracting the second BigInteger property to sum
         * @param mapper3 a function extracting the third BigInteger property to sum
         * @return a {@code Collector} that produces a tuple of the three sums
         */
        public static <T> Collector<T, ?, Tuple3<BigInteger, BigInteger, BigInteger>> summingBigInteger(final Function<? super T, BigInteger> mapper1,
                final Function<? super T, BigInteger> mapper2, final Function<? super T, BigInteger> mapper3) {
            final BiConsumer<BigInteger[], T> accumulator = (a, t) -> {
                a[0] = a[0].add(mapper1.apply(t));
                a[1] = a[1].add(mapper2.apply(t));
                a[2] = a[2].add(mapper3.apply(t));
            };

            return create(SummingBigInteger_Supplier_3, accumulator, SummingBigInteger_Combiner_3, SummingBigInteger_Finisher_3, CH_UNORDERED_NOID);
        }

        /**
         * Returns a {@code Collector} that computes the sum of two {@code BigDecimal}-valued
         * functions applied to the input elements.
         * 
         * <p>This collector maintains arbitrary precision decimal arithmetic throughout
         * the calculation, making it suitable for financial and scientific calculations
         * requiring exact decimal representation. The result is a tuple containing both
         * sums. Null values returned by the mapping functions will cause a NullPointerException.</p>
         * 
         * <p>The computation is performed in encounter order and is not affected by
         * stream ordering. BigDecimal arithmetic preserves all decimal places.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Calculate financial totals with exact precision
         * Tuple2<BigDecimal, BigDecimal> totals = invoices.stream()
         *     .collect(MoreCollectors.summingBigDecimal(
         *         Invoice::getSubtotal,
         *         Invoice::getTaxAmount
         *     ));
         * }</pre>
         *
         * @param <T> the type of input elements
         * @param mapper1 a function extracting the first BigDecimal property to sum
         * @param mapper2 a function extracting the second BigDecimal property to sum
         * @return a {@code Collector} that produces a tuple of the two sums
         */
        public static <T> Collector<T, ?, Tuple2<BigDecimal, BigDecimal>> summingBigDecimal(final Function<? super T, BigDecimal> mapper1,
                final Function<? super T, BigDecimal> mapper2) {
            final BiConsumer<BigDecimal[], T> accumulator = (a, t) -> {
                a[0] = a[0].add(mapper1.apply(t));
                a[1] = a[1].add(mapper2.apply(t));
            };

            return create(SummingBigDecimal_Supplier_2, accumulator, SummingBigDecimal_Combiner_2, SummingBigDecimal_Finisher_2, CH_UNORDERED_NOID);
        }

        /**
         * Returns a {@code Collector} that computes the sum of three {@code BigDecimal}-valued
         * functions applied to the input elements.
         * 
         * <p>This collector maintains arbitrary precision decimal arithmetic throughout
         * the calculation, making it suitable for financial and scientific calculations
         * requiring exact decimal representation. The result is a tuple containing all
         * three sums. Null values returned by the mapping functions will cause a
         * NullPointerException.</p>
         * 
         * <p>The computation is performed in encounter order and is not affected by
         * stream ordering. BigDecimal arithmetic preserves all decimal places.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Calculate currency conversions with precision
         * Tuple3<BigDecimal, BigDecimal, BigDecimal> currencies = exchanges.stream()
         *     .collect(MoreCollectors.summingBigDecimal(
         *         Exchange::getUsdAmount,
         *         Exchange::getEurAmount,
         *         Exchange::getGbpAmount
         *     ));
         * }</pre>
         *
         * @param <T> the type of input elements
         * @param mapper1 a function extracting the first BigDecimal property to sum
         * @param mapper2 a function extracting the second BigDecimal property to sum
         * @param mapper3 a function extracting the third BigDecimal property to sum
         * @return a {@code Collector} that produces a tuple of the three sums
         */
        public static <T> Collector<T, ?, Tuple3<BigDecimal, BigDecimal, BigDecimal>> summingBigDecimal(final Function<? super T, BigDecimal> mapper1,
                final Function<? super T, BigDecimal> mapper2, final Function<? super T, BigDecimal> mapper3) {
            final BiConsumer<BigDecimal[], T> accumulator = (a, t) -> {
                a[0] = a[0].add(mapper1.apply(t));
                a[1] = a[1].add(mapper2.apply(t));
                a[2] = a[2].add(mapper3.apply(t));
            };

            return create(SummingBigDecimal_Supplier_3, accumulator, SummingBigDecimal_Combiner_3, SummingBigDecimal_Finisher_3, CH_UNORDERED_NOID);
        }

        /**
         * Returns a {@code Collector} that computes the arithmetic mean of two integer-valued
         * functions applied to the input elements.
         * 
         * <p>This collector calculates the average of values extracted by two different
         * mapping functions from each element. The averages are computed using double
         * arithmetic to avoid precision loss. If no elements are collected, both averages
         * will be 0.0.</p>
         * 
         * <p>The computation maintains running sums and counts internally, converting to
         * averages only in the final step. This approach minimizes rounding errors during
         * parallel computation.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Calculate average age and salary
         * Tuple2<Double, Double> averages = employees.stream()
         *     .collect(MoreCollectors.averagingInt(
         *         Employee::getAge,
         *         Employee::getSalary
         *     ));
         * }</pre>
         *
         * @param <T> the type of input elements
         * @param mapper1 a function extracting the first integer property to average
         * @param mapper2 a function extracting the second integer property to average
         * @return a {@code Collector} that produces a tuple of the two averages
         */
        public static <T> Collector<T, ?, Tuple2<Double, Double>> averagingInt(final ToIntFunction<? super T> mapper1, final ToIntFunction<? super T> mapper2) {
            final BiConsumer<Pair<long[], long[]>, T> accumulator = (a, t) -> {
                a.left()[0] += mapper1.applyAsInt(t);
                a.left()[1] += mapper2.applyAsInt(t);
                a.right()[0] += 1;
                a.right()[1] += 1;
            };

            return create(AveragingInt_Supplier_2, accumulator, AveragingInt_Combiner_2, AveragingInt_Finisher_2, CH_UNORDERED_NOID);
        }

        /**
         * Returns a {@code Collector} that computes the arithmetic mean of three integer-valued
         * functions applied to the input elements.
         * 
         * <p>This collector calculates the average of values extracted by three different
         * mapping functions from each element. The averages are computed using double
         * arithmetic to avoid precision loss. If no elements are collected, all averages
         * will be 0.0.</p>
         * 
         * <p>The computation maintains running sums and counts internally, converting to
         * averages only in the final step. This approach minimizes rounding errors during
         * parallel computation.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Calculate average scores
         * Tuple3<Double, Double, Double> avgScores = students.stream()
         *     .collect(MoreCollectors.averagingInt(
         *         Student::getMathScore,
         *         Student::getScienceScore,
         *         Student::getEnglishScore
         *     ));
         * }</pre>
         *
         * @param <T> the type of input elements
         * @param mapper1 a function extracting the first integer property to average
         * @param mapper2 a function extracting the second integer property to average
         * @param mapper3 a function extracting the third integer property to average
         * @return a {@code Collector} that produces a tuple of the three averages
         */
        public static <T> Collector<T, ?, Tuple3<Double, Double, Double>> averagingInt(final ToIntFunction<? super T> mapper1,
                final ToIntFunction<? super T> mapper2, final ToIntFunction<? super T> mapper3) {
            final BiConsumer<Pair<long[], long[]>, T> accumulator = (a, t) -> {
                a.left()[0] += mapper1.applyAsInt(t);
                a.left()[1] += mapper2.applyAsInt(t);
                a.left()[2] += mapper3.applyAsInt(t);
                a.right()[0] += 1;
                a.right()[1] += 1;
                a.right()[2] += 1;
            };

            return create(AveragingInt_Supplier_3, accumulator, AveragingInt_Combiner_3, AveragingInt_Finisher_3, CH_UNORDERED_NOID);
        }

        /**
         * Returns a {@code Collector} that computes the arithmetic mean of two long-valued
         * functions applied to the input elements.
         * 
         * <p>This collector calculates the average of values extracted by two different
         * mapping functions from each element. The averages are computed using double
         * arithmetic to handle the division. If no elements are collected, both averages
         * will be 0.0.</p>
         * 
         * <p>The computation maintains running sums and counts internally, converting to
         * averages only in the final step. This approach minimizes rounding errors during
         * parallel computation and handles large long values correctly.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Calculate average file sizes
         * Tuple2<Double, Double> avgSizes = files.stream()
         *     .collect(MoreCollectors.averagingLong(
         *         File::getOriginalSize,
         *         File::getCompressedSize
         *     ));
         * }</pre>
         *
         * @param <T> the type of input elements
         * @param mapper1 a function extracting the first long property to average
         * @param mapper2 a function extracting the second long property to average
         * @return a {@code Collector} that produces a tuple of the two averages
         */
        public static <T> Collector<T, ?, Tuple2<Double, Double>> averagingLong(final ToLongFunction<? super T> mapper1,
                final ToLongFunction<? super T> mapper2) {
            final BiConsumer<Pair<long[], long[]>, T> accumulator = (a, t) -> {
                a.left()[0] += mapper1.applyAsLong(t);
                a.left()[1] += mapper2.applyAsLong(t);
                a.right()[0] += 1;
                a.right()[1] += 1;
            };

            return create(AveragingLong_Supplier_2, accumulator, AveragingLong_Combiner_2, AveragingLong_Finisher_2, CH_UNORDERED_NOID);
        }

        /**
         * Returns a {@code Collector} that computes the arithmetic mean of three long-valued
         * functions applied to the input elements.
         * 
         * <p>This collector calculates the average of values extracted by three different
         * mapping functions from each element. The averages are computed using double
         * arithmetic to handle the division. If no elements are collected, all averages
         * will be 0.0.</p>
         * 
         * <p>The computation maintains running sums and counts internally, converting to
         * averages only in the final step. This approach minimizes rounding errors during
         * parallel computation and handles large long values correctly.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Calculate average network metrics
         * Tuple3<Double, Double, Double> avgMetrics = connections.stream()
         *     .collect(MoreCollectors.averagingLong(
         *         Connection::getBytesReceived,
         *         Connection::getBytesSent,
         *         Connection::getLatencyNanos
         *     ));
         * }</pre>
         *
         * @param <T> the type of input elements
         * @param mapper1 a function extracting the first long property to average
         * @param mapper2 a function extracting the second long property to average
         * @param mapper3 a function extracting the third long property to average
         * @return a {@code Collector} that produces a tuple of the three averages
         */
        public static <T> Collector<T, ?, Tuple3<Double, Double, Double>> averagingLong(final ToLongFunction<? super T> mapper1,
                final ToLongFunction<? super T> mapper2, final ToLongFunction<? super T> mapper3) {
            final BiConsumer<Pair<long[], long[]>, T> accumulator = (a, t) -> {
                a.left()[0] += mapper1.applyAsLong(t);
                a.left()[1] += mapper2.applyAsLong(t);
                a.left()[2] += mapper3.applyAsLong(t);
                a.right()[0] += 1;
                a.right()[1] += 1;
                a.right()[2] += 1;
            };

            return create(AveragingLong_Supplier_3, accumulator, AveragingLong_Combiner_3, AveragingLong_Finisher_3, CH_UNORDERED_NOID);
        }

        /**
         * Returns a {@code Collector} that computes the arithmetic mean of two double-valued
         * functions applied to the input elements.
         *
         * <p>This collector calculates the average of values extracted by two different
         * mapping functions from each element. The averages are computed using double
         * arithmetic to handle the division. If no elements are collected, both averages
         * will be 0.0.</p>
         *
         * <p>The computation uses Kahan summation for improved numerical accuracy,
         * minimizing rounding errors during parallel computation.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Calculate average temperatures
         * Tuple2<Double, Double> avgTemps = readings.stream()
         *     .collect(MoreCollectors.averagingDouble(
         *         Reading::getIndoorTemp,
         *         Reading::getOutdoorTemp
         *     ));
         * }</pre>
         *
         * @param <T> the type of input elements
         * @param mapper1 a function extracting the first double property to average
         * @param mapper2 a function extracting the second double property to average
         * @return a {@code Collector} that produces a tuple of the two averages
         */
        public static <T> Collector<T, ?, Tuple2<Double, Double>> averagingDouble(final ToDoubleFunction<? super T> mapper1,
                final ToDoubleFunction<? super T> mapper2) {
            final BiConsumer<KahanSummation[], T> accumulator = (a, t) -> {
                a[0].add(mapper1.applyAsDouble(t));
                a[1].add(mapper2.applyAsDouble(t));
            };

            return create(AveragingDouble_Supplier_2, accumulator, AveragingDouble_Combiner_2, AveragingDouble_Finisher_2, CH_UNORDERED_NOID);
        }

        /**
         * Returns a {@code Collector} that computes the arithmetic mean of three double-valued
         * functions applied to the input elements.
         *
         * <p>This collector calculates the average of values extracted by three different
         * mapping functions from each element. The averages are computed using double
         * arithmetic to handle the division. If no elements are collected, all averages
         * will be 0.0.</p>
         *
         * <p>The computation uses Kahan summation for improved numerical accuracy,
         * minimizing rounding errors during parallel computation.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Calculate average sensor readings
         * Tuple3<Double, Double, Double> avgReadings = sensors.stream()
         *     .collect(MoreCollectors.averagingDouble(
         *         Sensor::getPressure,
         *         Sensor::getHumidity,
         *         Sensor::getVoltage
         *     ));
         * }</pre>
         *
         * @param <T> the type of input elements
         * @param mapper1 a function extracting the first double property to average
         * @param mapper2 a function extracting the second double property to average
         * @param mapper3 a function extracting the third double property to average
         * @return a {@code Collector} that produces a tuple of the three averages
         */
        public static <T> Collector<T, ?, Tuple3<Double, Double, Double>> averagingDouble(final ToDoubleFunction<? super T> mapper1,
                final ToDoubleFunction<? super T> mapper2, final ToDoubleFunction<? super T> mapper3) {
            final BiConsumer<KahanSummation[], T> accumulator = (a, t) -> {
                a[0].add(mapper1.applyAsDouble(t));
                a[1].add(mapper2.applyAsDouble(t));
                a[2].add(mapper3.applyAsDouble(t));
            };

            return create(AveragingDouble_Supplier_3, accumulator, AveragingDouble_Combiner_3, AveragingDouble_Finisher_3, CH_UNORDERED_NOID);
        }

        /**
         * Returns a {@code Collector} that calculates the average of {@code BigInteger} values
         * extracted by two mapping functions from the input elements.
         * 
         * <p>This collector is useful when you need to compute averages of very large integer
         * values that might exceed the range of primitive types. The collector accumulates
         * the sum of values from each mapper and counts the elements, then computes the
         * average as {@code BigDecimal} values with full precision.</p>
         * 
         * <p>The returned collector produces a {@code Tuple2} containing the averages of
         * the values extracted by each mapper function. Each average is calculated as
         * the sum divided by the count of elements.
         * If no elements are collected, all averages will be BigDecimal.ZERO.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Calculate average of price and quantity for large values
         * Tuple2<BigDecimal, BigDecimal> averages = orders.stream()
         *     .collect(MoreCollectors.averagingBigInteger(
         *         order -> order.getPrice(),
         *         order -> order.getQuantity()
         *     ));
         * }</pre>
         *
         * @param <T> the type of input elements
         * @param mapper1 a function extracting the first {@code BigInteger} value from an element
         * @param mapper2 a function extracting the second {@code BigInteger} value from an element
         * @return a {@code Collector} which calculates the averages of the extracted values
         *         as a {@code Tuple2<BigDecimal, BigDecimal>}
         */
        public static <T> Collector<T, ?, Tuple2<BigDecimal, BigDecimal>> averagingBigInteger(final Function<? super T, BigInteger> mapper1,
                final Function<? super T, BigInteger> mapper2) {
            final BiConsumer<Pair<BigInteger[], long[]>, T> accumulator = (a, t) -> {
                a.left()[0] = a.left()[0].add(mapper1.apply(t));
                a.left()[1] = a.left()[1].add(mapper2.apply(t));
                a.right()[0] += 1;
                a.right()[1] += 1;
            };

            return create(AveragingBigInteger_Supplier_2, accumulator, AveragingBigInteger_Combiner_2, AveragingBigInteger_Finisher_2, CH_UNORDERED_NOID);
        }

        /**
         * Returns a {@code Collector} that calculates the average of {@code BigInteger} values
         * extracted by three mapping functions from the input elements.
         * 
         * <p>This collector is useful when you need to compute averages of very large integer
         * values for multiple properties simultaneously. The collector accumulates the sum of
         * values from each mapper and counts the elements, then computes the averages as
         * {@code BigDecimal} values with full precision.</p>
         * 
         * <p>The returned collector produces a {@code Tuple3} containing the averages of
         * the values extracted by each mapper function. Each average is calculated as
         * the sum divided by the count of elements.
         * If no elements are collected, all averages will be BigDecimal.ZERO.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Calculate average of price, quantity, and discount for large values
         * Tuple3<BigDecimal, BigDecimal, BigDecimal> averages = orders.stream()
         *     .collect(MoreCollectors.averagingBigInteger(
         *         order -> order.getPrice(),
         *         order -> order.getQuantity(),
         *         order -> order.getDiscount()
         *     ));
         * }</pre>
         *
         * @param <T> the type of input elements
         * @param mapper1 a function extracting the first {@code BigInteger} value from an element
         * @param mapper2 a function extracting the second {@code BigInteger} value from an element
         * @param mapper3 a function extracting the third {@code BigInteger} value from an element
         * @return a {@code Collector} which calculates the averages of the extracted values
         *         as a {@code Tuple3<BigDecimal, BigDecimal, BigDecimal>}
         */
        public static <T> Collector<T, ?, Tuple3<BigDecimal, BigDecimal, BigDecimal>> averagingBigInteger(final Function<? super T, BigInteger> mapper1,
                final Function<? super T, BigInteger> mapper2, final Function<? super T, BigInteger> mapper3) {
            final BiConsumer<Pair<BigInteger[], long[]>, T> accumulator = (a, t) -> {
                a.left()[0] = a.left()[0].add(mapper1.apply(t));
                a.left()[1] = a.left()[1].add(mapper2.apply(t));
                a.left()[2] = a.left()[2].add(mapper3.apply(t));
                a.right()[0] += 1;
                a.right()[1] += 1;
                a.right()[2] += 1;
            };

            return create(AveragingBigInteger_Supplier_3, accumulator, AveragingBigInteger_Combiner_3, AveragingBigInteger_Finisher_3, CH_UNORDERED_NOID);
        }

        /**
         * Returns a {@code Collector} that calculates the average of {@code BigDecimal} values
         * extracted by two mapping functions from the input elements.
         * 
         * <p>This collector is useful when you need to compute averages of decimal values
         * with arbitrary precision. The collector accumulates the sum of values from each
         * mapper and counts the elements, then computes the averages maintaining full
         * precision throughout the calculation.</p>
         * 
         * <p>The returned collector produces a {@code Tuple2} containing the averages of
         * the values extracted by each mapper function. Each average is calculated as
         * the sum divided by the count of elements using {@code BigDecimal} arithmetic.
         * If no elements are collected, all averages will be BigDecimal.ZERO.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Calculate average of price and tax amount with high precision
         * Tuple2<BigDecimal, BigDecimal> averages = transactions.stream()
         *     .collect(MoreCollectors.averagingBigDecimal(
         *         trans -> trans.getPrice(),
         *         trans -> trans.getTaxAmount()
         *     ));
         * }</pre>
         *
         * @param <T> the type of input elements
         * @param mapper1 a function extracting the first {@code BigDecimal} value from an element
         * @param mapper2 a function extracting the second {@code BigDecimal} value from an element
         * @return a {@code Collector} which calculates the averages of the extracted values
         *         as a {@code Tuple2<BigDecimal, BigDecimal>}
         */
        public static <T> Collector<T, ?, Tuple2<BigDecimal, BigDecimal>> averagingBigDecimal(final Function<? super T, BigDecimal> mapper1,
                final Function<? super T, BigDecimal> mapper2) {
            final BiConsumer<Pair<BigDecimal[], long[]>, T> accumulator = (a, t) -> {
                a.left()[0] = a.left()[0].add(mapper1.apply(t));
                a.left()[1] = a.left()[1].add(mapper2.apply(t));
                a.right()[0] += 1;
                a.right()[1] += 1;
            };

            return create(AveragingBigDecimal_Supplier_2, accumulator, AveragingBigDecimal_Combiner_2, AveragingBigDecimal_Finisher_2, CH_UNORDERED_NOID);
        }

        /**
         * Returns a {@code Collector} that calculates the average of {@code BigDecimal} values
         * extracted by three mapping functions from the input elements.
         * 
         * <p>This collector is useful when you need to compute averages of decimal values
         * for multiple properties simultaneously with arbitrary precision. The collector
         * accumulates the sum of values from each mapper and counts the elements, then
         * computes the averages maintaining full precision throughout the calculation.</p>
         * 
         * <p>The returned collector produces a {@code Tuple3} containing the averages of
         * the values extracted by each mapper function. Each average is calculated as
         * the sum divided by the count of elements using {@code BigDecimal} arithmetic.
         * If no elements are collected, all averages will be BigDecimal.ZERO.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Calculate average of price, tax, and shipping cost with high precision
         * Tuple3<BigDecimal, BigDecimal, BigDecimal> averages = orders.stream()
         *     .collect(MoreCollectors.averagingBigDecimal(
         *         order -> order.getPrice(),
         *         order -> order.getTax(),
         *         order -> order.getShippingCost()
         *     ));
         * }</pre>
         *
         * @param <T> the type of input elements
         * @param mapper1 a function extracting the first {@code BigDecimal} value from an element
         * @param mapper2 a function extracting the second {@code BigDecimal} value from an element
         * @param mapper3 a function extracting the third {@code BigDecimal} value from an element
         * @return a {@code Collector} which calculates the averages of the extracted values
         *         as a {@code Tuple3<BigDecimal, BigDecimal, BigDecimal>}
         */
        public static <T> Collector<T, ?, Tuple3<BigDecimal, BigDecimal, BigDecimal>> averagingBigDecimal(final Function<? super T, BigDecimal> mapper1,
                final Function<? super T, BigDecimal> mapper2, final Function<? super T, BigDecimal> mapper3) {
            final BiConsumer<Pair<BigDecimal[], long[]>, T> accumulator = (a, t) -> {
                a.left()[0] = a.left()[0].add(mapper1.apply(t));
                a.left()[1] = a.left()[1].add(mapper2.apply(t));
                a.left()[2] = a.left()[2].add(mapper3.apply(t));
                a.right()[0] += 1;
                a.right()[1] += 1;
                a.right()[2] += 1;
            };

            return create(AveragingBigDecimal_Supplier_3, accumulator, AveragingBigDecimal_Combiner_3, AveragingBigDecimal_Finisher_3, CH_UNORDERED_NOID);
        }

        /**
         * Returns a {@code Collector} that combines the results of two downstream collectors
         * into a {@code Tuple2}.
         * 
         * <p>This collector is useful when you need to perform two different collection
         * operations on the same stream of elements in a single pass. Each element is
         * processed by both downstream collectors, and their final results are combined
         * into a tuple.</p>
         * 
         * <p>The returned collector forwards elements to both downstream collectors and
         * combines their results using {@code Tuple.of()}. The characteristics of the
         * returned collector are the intersection of the characteristics of the two
         * downstream collectors.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Count elements and calculate sum in one pass
         * Tuple2<Long, Integer> result = Stream.of(1, 2, 3, 4, 5)
         *     .collect(MoreCollectors.combine(
         *         Collectors.counting(),
         *         Collectors.summingInt(i -> i)
         *     ));
         * // result._1 = 5L (count), result._2 = 15 (sum)
         * }</pre>
         *
         * @param <T> the type of input elements
         * @param <R1> the result type of the first downstream collector
         * @param <R2> the result type of the second downstream collector
         * @param downstream1 the first downstream collector
         * @param downstream2 the second downstream collector
         * @return a {@code Collector} which combines the results of two downstream collectors
         *         into a {@code Tuple2<R1, R2>}
         * @throws IllegalArgumentException if any downstream collector is null
         */
        public static <T, R1, R2> Collector<T, ?, Tuple2<R1, R2>> combine(final Collector<? super T, ?, R1> downstream1,
                final Collector<? super T, ?, R2> downstream2) {
            return combine(downstream1, downstream2, Tuple::of);
        }

        /**
         * Returns a {@code Collector} that combines the results of three downstream collectors
         * into a {@code Tuple3}.
         * 
         * <p>This collector is useful when you need to perform three different collection
         * operations on the same stream of elements in a single pass. Each element is
         * processed by all three downstream collectors, and their final results are
         * combined into a tuple.</p>
         * 
         * <p>The returned collector forwards elements to all downstream collectors and
         * combines their results using {@code Tuple.of()}. The characteristics of the
         * returned collector are the intersection of the characteristics of all three
         * downstream collectors.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Count, sum, and find max in one pass
         * Tuple3<Long, Integer, Optional<Integer>> result = Stream.of(1, 2, 3, 4, 5)
         *     .collect(MoreCollectors.combine(
         *         Collectors.counting(),
         *         Collectors.summingInt(i -> i),
         *         Collectors.maxBy(Integer::compare)
         *     ));
         * }</pre>
         *
         * @param <T> the type of input elements
         * @param <R1> the result type of the first downstream collector
         * @param <R2> the result type of the second downstream collector
         * @param <R3> the result type of the third downstream collector
         * @param downstream1 the first downstream collector
         * @param downstream2 the second downstream collector
         * @param downstream3 the third downstream collector
         * @return a {@code Collector} which combines the results of three downstream collectors
         *         into a {@code Tuple3<R1, R2, R3>}
         * @throws IllegalArgumentException if any downstream collector is null
         */
        public static <T, R1, R2, R3> Collector<T, ?, Tuple3<R1, R2, R3>> combine(final Collector<? super T, ?, R1> downstream1,
                final Collector<? super T, ?, R2> downstream2, final Collector<? super T, ?, R3> downstream3) {
            return combine(downstream1, downstream2, downstream3, Tuple::of);
        }

        /**
         * Returns a {@code Collector} that combines the results of four downstream collectors
         * into a {@code Tuple4}.
         * 
         * <p>This collector is useful when you need to perform four different collection
         * operations on the same stream of elements in a single pass. Each element is
         * processed by all four downstream collectors, and their final results are
         * combined into a tuple.</p>
         * 
         * <p>The returned collector forwards elements to all downstream collectors and
         * combines their results using {@code Tuple.of()}. The characteristics of the
         * returned collector are the intersection of the characteristics of all four
         * downstream collectors.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Multiple statistics in one pass
         * Tuple4<Long, Integer, Optional<Integer>, Double> stats = 
         *     Stream.of(1, 2, 3, 4, 5)
         *         .collect(MoreCollectors.combine(
         *             Collectors.counting(),
         *             Collectors.summingInt(i -> i),
         *             Collectors.maxBy(Integer::compare),
         *             Collectors.averagingInt(i -> i)
         *         ));
         * }</pre>
         *
         * @param <T> the type of input elements
         * @param <R1> the result type of the first downstream collector
         * @param <R2> the result type of the second downstream collector
         * @param <R3> the result type of the third downstream collector
         * @param <R4> the result type of the fourth downstream collector
         * @param downstream1 the first downstream collector
         * @param downstream2 the second downstream collector
         * @param downstream3 the third downstream collector
         * @param downstream4 the fourth downstream collector
         * @return a {@code Collector} which combines the results of four downstream collectors
         *         into a {@code Tuple4<R1, R2, R3, R4>}
         * @throws IllegalArgumentException if any downstream collector is null
         */
        public static <T, R1, R2, R3, R4> Collector<T, ?, Tuple4<R1, R2, R3, R4>> combine(final Collector<? super T, ?, R1> downstream1,
                final Collector<? super T, ?, R2> downstream2, final Collector<? super T, ?, R3> downstream3, final Collector<? super T, ?, R4> downstream4) {
            return combine(downstream1, downstream2, downstream3, downstream4, Tuple::of);
        }

        /**
         * Returns a {@code Collector} that combines the results of five downstream collectors
         * into a {@code Tuple5}.
         * 
         * <p>This collector is useful when you need to perform five different collection
         * operations on the same stream of elements in a single pass. Each element is
         * processed by all five downstream collectors, and their final results are
         * combined into a tuple.</p>
         * 
         * <p>The returned collector forwards elements to all downstream collectors and
         * combines their results using {@code Tuple.of()}. The characteristics of the
         * returned collector are the intersection of the characteristics of all five
         * downstream collectors.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Comprehensive statistics in one pass
         * Tuple5<Long, Integer, Optional<Integer>, Optional<Integer>, Double> stats = 
         *     Stream.of(1, 2, 3, 4, 5)
         *         .collect(MoreCollectors.combine(
         *             Collectors.counting(),
         *             Collectors.summingInt(i -> i),
         *             Collectors.minBy(Integer::compare),
         *             Collectors.maxBy(Integer::compare),
         *             Collectors.averagingInt(i -> i)
         *         ));
         * }</pre>
         *
         * @param <T> the type of input elements
         * @param <R1> the result type of the first downstream collector
         * @param <R2> the result type of the second downstream collector
         * @param <R3> the result type of the third downstream collector
         * @param <R4> the result type of the fourth downstream collector
         * @param <R5> the result type of the fifth downstream collector
         * @param downstream1 the first downstream collector
         * @param downstream2 the second downstream collector
         * @param downstream3 the third downstream collector
         * @param downstream4 the fourth downstream collector
         * @param downstream5 the fifth downstream collector
         * @return a {@code Collector} which combines the results of five downstream collectors
         *         into a {@code Tuple5<R1, R2, R3, R4, R5>}
         * @throws IllegalArgumentException if any downstream collector is null
         */
        public static <T, R1, R2, R3, R4, R5> Collector<T, ?, Tuple5<R1, R2, R3, R4, R5>> combine(final Collector<? super T, ?, R1> downstream1,
                final Collector<? super T, ?, R2> downstream2, final Collector<? super T, ?, R3> downstream3, final Collector<? super T, ?, R4> downstream4,
                final Collector<? super T, ?, R5> downstream5) throws IllegalArgumentException {
            N.checkArgNotNull(downstream1, "downstream1");   //NOSONAR
            N.checkArgNotNull(downstream2, "downstream2");   //NOSONAR
            N.checkArgNotNull(downstream3, "downstream3");   //NOSONAR
            N.checkArgNotNull(downstream4, "downstream4");   //NOSONAR
            N.checkArgNotNull(downstream5, "downstream5");   //NOSONAR

            final List<Collector<? super T, ?, ?>> downstreams = Array.asList(downstream1, downstream2, downstream3, downstream4, downstream5);

            final Function<Object[], Tuple5<R1, R2, R3, R4, R5>> finalMerger = a -> Tuple.of((R1) a[0], (R2) a[1], (R3) a[2], (R4) a[3], (R5) a[4]);

            return combine(downstreams, finalMerger);
        }

        /**
         * Returns a {@code Collector} that combines the results of six downstream collectors
         * into a {@code Tuple6}.
         * 
         * <p>This collector is useful when you need to perform six different collection
         * operations on the same stream of elements in a single pass. Each element is
         * processed by all six downstream collectors, and their final results are
         * combined into a tuple.</p>
         * 
         * <p>The returned collector forwards elements to all downstream collectors and
         * combines their results using {@code Tuple.of()}. The characteristics of the
         * returned collector are the intersection of the characteristics of all six
         * downstream collectors.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Multiple aggregations for product analysis
         * Tuple6<Long, Double, Double, Optional<Product>, Optional<Product>, List<String>> result = 
         *     products.stream()
         *         .collect(MoreCollectors.combine(
         *             Collectors.counting(),
         *             Collectors.summingDouble(Product::getPrice),
         *             Collectors.averagingDouble(Product::getRating),
         *             Collectors.minBy(Comparator.comparing(Product::getPrice)),
         *             Collectors.maxBy(Comparator.comparing(Product::getRating)),
         *             Collectors.mapping(Product::getName, Collectors.toList())
         *         ));
         * }</pre>
         *
         * @param <T> the type of input elements
         * @param <R1> the result type of the first downstream collector
         * @param <R2> the result type of the second downstream collector
         * @param <R3> the result type of the third downstream collector
         * @param <R4> the result type of the fourth downstream collector
         * @param <R5> the result type of the fifth downstream collector
         * @param <R6> the result type of the sixth downstream collector
         * @param downstream1 the first downstream collector
         * @param downstream2 the second downstream collector
         * @param downstream3 the third downstream collector
         * @param downstream4 the fourth downstream collector
         * @param downstream5 the fifth downstream collector
         * @param downstream6 the sixth downstream collector
         * @return a {@code Collector} which combines the results of six downstream collectors
         *         into a {@code Tuple6<R1, R2, R3, R4, R5, R6>}
         * @throws IllegalArgumentException if any downstream collector is null
         */
        public static <T, R1, R2, R3, R4, R5, R6> Collector<T, ?, Tuple6<R1, R2, R3, R4, R5, R6>> combine(final Collector<? super T, ?, R1> downstream1,
                final Collector<? super T, ?, R2> downstream2, final Collector<? super T, ?, R3> downstream3, final Collector<? super T, ?, R4> downstream4,
                final Collector<? super T, ?, R5> downstream5, final Collector<? super T, ?, R6> downstream6) throws IllegalArgumentException {
            N.checkArgNotNull(downstream1, cs.downstream1);
            N.checkArgNotNull(downstream2, cs.downstream2);
            N.checkArgNotNull(downstream3, cs.downstream3);
            N.checkArgNotNull(downstream4, cs.downstream4);
            N.checkArgNotNull(downstream5, cs.downstream5);
            N.checkArgNotNull(downstream6, cs.downstream6);

            final List<Collector<? super T, ?, ?>> downstreams = Array.asList(downstream1, downstream2, downstream3, downstream4, downstream5, downstream6);

            final Function<Object[], Tuple6<R1, R2, R3, R4, R5, R6>> finalMerger = a -> Tuple.of((R1) a[0], (R2) a[1], (R3) a[2], (R4) a[3], (R5) a[4],
                    (R6) a[5]);

            return combine(downstreams, finalMerger);
        }

        /**
         * Returns a {@code Collector} that combines the results of seven downstream collectors
         * into a {@code Tuple7}.
         * 
         * <p>This collector is useful when you need to perform seven different collection
         * operations on the same stream of elements in a single pass. Each element is
         * processed by all seven downstream collectors, and their final results are
         * combined into a tuple.</p>
         * 
         * <p>The returned collector forwards elements to all downstream collectors and
         * combines their results using {@code Tuple.of()}. The characteristics of the
         * returned collector are the intersection of the characteristics of all seven
         * downstream collectors.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Comprehensive order statistics
         * Tuple7<Long, Double, Double, LocalDate, LocalDate,
         *        Map<String, Long>, Set<String>> stats =
         *     orders.stream()
         *         .collect(MoreCollectors.combine(
         *             Collectors.counting(),
         *             Collectors.summingDouble(Order::getTotal),
         *             Collectors.averagingDouble(Order::getTotal),
         *             Collectors.collectingAndThen(
         *                 Collectors.mapping(Order::getDate, Collectors.minBy(LocalDate::compareTo)),
         *                 opt -> opt.orElse(null)),
         *             Collectors.collectingAndThen(
         *                 Collectors.mapping(Order::getDate, Collectors.maxBy(LocalDate::compareTo)),
         *                 opt -> opt.orElse(null)),
         *             Collectors.groupingBy(Order::getStatus, Collectors.counting()),
         *             Collectors.mapping(Order::getCustomerId, Collectors.toSet())
         *         ));
         * }</pre>
         *
         * @param <T> the type of input elements
         * @param <R1> the result type of the first downstream collector
         * @param <R2> the result type of the second downstream collector
         * @param <R3> the result type of the third downstream collector
         * @param <R4> the result type of the fourth downstream collector
         * @param <R5> the result type of the fifth downstream collector
         * @param <R6> the result type of the sixth downstream collector
         * @param <R7> the result type of the seventh downstream collector
         * @param downstream1 the first downstream collector
         * @param downstream2 the second downstream collector
         * @param downstream3 the third downstream collector
         * @param downstream4 the fourth downstream collector
         * @param downstream5 the fifth downstream collector
         * @param downstream6 the sixth downstream collector
         * @param downstream7 the seventh downstream collector
         * @return a {@code Collector} which combines the results of seven downstream collectors
         *         into a {@code Tuple7<R1, R2, R3, R4, R5, R6, R7>}
         * @throws IllegalArgumentException if any downstream collector is null
         */
        public static <T, R1, R2, R3, R4, R5, R6, R7> Collector<T, ?, Tuple7<R1, R2, R3, R4, R5, R6, R7>> combine(final Collector<? super T, ?, R1> downstream1,
                final Collector<? super T, ?, R2> downstream2, final Collector<? super T, ?, R3> downstream3, final Collector<? super T, ?, R4> downstream4,
                final Collector<? super T, ?, R5> downstream5, final Collector<? super T, ?, R6> downstream6, final Collector<? super T, ?, R7> downstream7)
                throws IllegalArgumentException {
            N.checkArgNotNull(downstream1, cs.downstream1);
            N.checkArgNotNull(downstream2, cs.downstream2);
            N.checkArgNotNull(downstream3, cs.downstream3);
            N.checkArgNotNull(downstream4, cs.downstream4);
            N.checkArgNotNull(downstream5, cs.downstream5);
            N.checkArgNotNull(downstream6, cs.downstream6);
            N.checkArgNotNull(downstream7, cs.downstream7);

            final List<Collector<? super T, ?, ?>> downstreams = Array.asList(downstream1, downstream2, downstream3, downstream4, downstream5, downstream6,
                    downstream7);

            final Function<Object[], Tuple7<R1, R2, R3, R4, R5, R6, R7>> finalMerger = a -> Tuple.of((R1) a[0], (R2) a[1], (R3) a[2], (R4) a[3], (R5) a[4],
                    (R6) a[5], (R7) a[6]);

            return combine(downstreams, finalMerger);
        }

        /**
         * Returns a {@code Collector} that combines the results of two downstream collectors
         * using a specified merger function.
         * 
         * <p>This collector is useful when you need to perform two different collection
         * operations on the same stream of elements and combine their results in a custom
         * way. Each element is processed by both downstream collectors, and their final
         * results are combined using the provided merger function.</p>
         * 
         * <p>The returned collector forwards elements to both downstream collectors and
         * combines their results using the provided merger. The characteristics of the
         * returned collector are the intersection of the characteristics of the two
         * downstream collectors, excluding {@code IDENTITY_FINISH}.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Calculate count and average, returning formatted string
         * String summary = Stream.of(1, 2, 3, 4, 5)
         *     .collect(MoreCollectors.combine(
         *         Collectors.counting(),
         *         Collectors.averagingDouble(i -> i),
         *         (count, avg) -> String.format("Count: %d, Average: %.2f", count, avg)
         *     ));
         * // Result: "Count: 5, Average: 3.00"
         * }</pre>
         *
         * @param <T> the type of input elements
         * @param <R1> the result type of the first downstream collector
         * @param <R2> the result type of the second downstream collector
         * @param <R> the final result type
         * @param downstream1 the first downstream collector
         * @param downstream2 the second downstream collector
         * @param merger a function to merge the results of the two downstream collectors
         * @return a {@code Collector} which combines the results of two downstream collectors
         *         using the merger function
         * @throws IllegalArgumentException if any downstream collector or merger is null
         */
        @SuppressWarnings("rawtypes")
        public static <T, R1, R2, R> Collector<T, ?, R> combine(final Collector<? super T, ?, R1> downstream1, final Collector<? super T, ?, R2> downstream2,
                final BiFunction<? super R1, ? super R2, R> merger) throws IllegalArgumentException {
            N.checkArgNotNull(downstream1, cs.downstream1);
            N.checkArgNotNull(downstream2, cs.downstream2);
            N.checkArgNotNull(merger, "merger");   //NOSONAR

            final Supplier<Object> c1supplier = (Supplier) downstream1.supplier();
            final Supplier<Object> c2Supplier = (Supplier) downstream2.supplier();
            final BiConsumer<Object, ? super T> c1Accumulator = (BiConsumer) downstream1.accumulator();
            final BiConsumer<Object, ? super T> c2Accumulator = (BiConsumer) downstream2.accumulator();
            final BinaryOperator<Object> c1Combiner = (BinaryOperator) downstream1.combiner();
            final BinaryOperator<Object> c2Combiner = (BinaryOperator) downstream2.combiner();
            final Function<Object, R1> c1Finisher = (Function) downstream1.finisher();
            final Function<Object, R2> c2Finisher = (Function) downstream2.finisher();

            final Supplier<Tuple2<Object, Object>> supplier = () -> Tuple.of(c1supplier.get(), c2Supplier.get());

            final BiConsumer<Tuple2<Object, Object>, T> accumulator = (acct, e) -> {
                c1Accumulator.accept(acct._1, e);
                c2Accumulator.accept(acct._2, e);
            };

            final BinaryOperator<Tuple2<Object, Object>> combiner = (t, u) -> Tuple.of(c1Combiner.apply(t._1, u._1), c2Combiner.apply(t._2, u._2));

            final Function<Tuple2<Object, Object>, R> finisher = t -> merger.apply(c1Finisher.apply(t._1), c2Finisher.apply(t._2));

            final List<Characteristics> common = N.intersection(downstream1.characteristics(), downstream2.characteristics());
            common.remove(Characteristics.IDENTITY_FINISH);
            final Set<Characteristics> characteristics = N.isEmpty(common) ? N.emptySet() : N.newHashSet(common);

            return create(supplier, accumulator, combiner, finisher, characteristics);
        }

        /**
         * Returns a {@code Collector} that combines the results of three downstream collectors
         * using a specified merger function.
         * 
         * <p>This collector is useful when you need to perform three different collection
         * operations on the same stream of elements and combine their results in a custom
         * way. Each element is processed by all three downstream collectors, and their
         * final results are combined using the provided merger function.</p>
         * 
         * <p>The returned collector forwards elements to all downstream collectors and
         * combines their results using the provided merger. The characteristics of the
         * returned collector are the intersection of the characteristics of all three
         * downstream collectors, excluding {@code IDENTITY_FINISH}.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Create custom statistics object
         * Statistics stats = Stream.of(1, 2, 3, 4, 5)
         *     .collect(MoreCollectors.combine(
         *         Collectors.counting(),
         *         Collectors.summingInt(i -> i),
         *         Collectors.averagingDouble(i -> i),
         *         (count, sum, avg) -> new Statistics(count, sum, avg)
         *     ));
         * }</pre>
         *
         * @param <T> the type of input elements
         * @param <R1> the result type of the first downstream collector
         * @param <R2> the result type of the second downstream collector
         * @param <R3> the result type of the third downstream collector
         * @param <R> the final result type
         * @param downstream1 the first downstream collector
         * @param downstream2 the second downstream collector
         * @param downstream3 the third downstream collector
         * @param merger a function to merge the results of the three downstream collectors
         * @return a {@code Collector} which combines the results of three downstream collectors
         *         using the merger function
         * @throws IllegalArgumentException if any downstream collector or merger is null
         */
        @SuppressWarnings("rawtypes")
        public static <T, R1, R2, R3, R> Collector<T, ?, R> combine(final Collector<? super T, ?, R1> downstream1,
                final Collector<? super T, ?, R2> downstream2, final Collector<? super T, ?, R3> downstream3,
                final TriFunction<? super R1, ? super R2, ? super R3, R> merger) throws IllegalArgumentException {
            N.checkArgNotNull(downstream1, cs.downstream1);
            N.checkArgNotNull(downstream2, cs.downstream2);
            N.checkArgNotNull(downstream3, cs.downstream3);
            N.checkArgNotNull(merger, cs.merger);

            final Supplier<Object> c1supplier = (Supplier) downstream1.supplier();
            final Supplier<Object> c2Supplier = (Supplier) downstream2.supplier();
            final Supplier<Object> c3Supplier = (Supplier) downstream3.supplier();
            final BiConsumer<Object, ? super T> c1Accumulator = (BiConsumer) downstream1.accumulator();
            final BiConsumer<Object, ? super T> c2Accumulator = (BiConsumer) downstream2.accumulator();
            final BiConsumer<Object, ? super T> c3Accumulator = (BiConsumer) downstream3.accumulator();
            final BinaryOperator<Object> c1Combiner = (BinaryOperator) downstream1.combiner();
            final BinaryOperator<Object> c2Combiner = (BinaryOperator) downstream2.combiner();
            final BinaryOperator<Object> c3Combiner = (BinaryOperator) downstream3.combiner();
            final Function<Object, R1> c1Finisher = (Function) downstream1.finisher();
            final Function<Object, R2> c2Finisher = (Function) downstream2.finisher();
            final Function<Object, R3> c3Finisher = (Function) downstream3.finisher();

            final Supplier<Tuple3<Object, Object, Object>> supplier = () -> Tuple.of(c1supplier.get(), c2Supplier.get(), c3Supplier.get());

            final BiConsumer<Tuple3<Object, Object, Object>, T> accumulator = (acct, e) -> {
                c1Accumulator.accept(acct._1, e);
                c2Accumulator.accept(acct._2, e);
                c3Accumulator.accept(acct._3, e);
            };

            final BinaryOperator<Tuple3<Object, Object, Object>> combiner = (t, u) -> Tuple.of(c1Combiner.apply(t._1, u._1), c2Combiner.apply(t._2, u._2),
                    c3Combiner.apply(t._3, u._3));

            final Function<Tuple3<Object, Object, Object>, R> finisher = t -> merger.apply(c1Finisher.apply(t._1), c2Finisher.apply(t._2),
                    c3Finisher.apply(t._3));

            final List<Characteristics> common = N.intersection(downstream1.characteristics(), downstream2.characteristics());
            common.remove(Characteristics.IDENTITY_FINISH);
            final Set<Characteristics> characteristics = N.isEmpty(common) ? N.emptySet() : N.newHashSet(common);

            return create(supplier, accumulator, combiner, finisher, characteristics);
        }

        /**
         * Returns a {@code Collector} that combines the results of four downstream collectors
         * using a specified merger function.
         * 
         * <p>This collector is useful when you need to perform four different collection
         * operations on the same stream of elements and combine their results in a custom
         * way. Each element is processed by all four downstream collectors, and their
         * final results are combined using the provided merger function.</p>
         * 
         * <p>The returned collector forwards elements to all downstream collectors and
         * combines their results using the provided merger. The characteristics of the
         * returned collector are the intersection of the characteristics of all four
         * downstream collectors, excluding {@code IDENTITY_FINISH}.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Create comprehensive product analysis
         * ProductAnalysis analysis = products.stream()
         *     .collect(MoreCollectors.combine(
         *         Collectors.counting(),
         *         Collectors.averagingDouble(Product::getPrice),
         *         Collectors.mapping(Product::getCategory, 
         *             Collectors.groupingBy(Function.identity(), Collectors.counting())),
         *         Collectors.partitioningBy(p -> p.getRating() >= 4.0),
         *         (count, avgPrice, categoryMap, ratingPartition) -> 
         *             new ProductAnalysis(count, avgPrice, categoryMap, ratingPartition)
         *     ));
         * }</pre>
         *
         * @param <T> the type of input elements
         * @param <R1> the result type of the first downstream collector
         * @param <R2> the result type of the second downstream collector
         * @param <R3> the result type of the third downstream collector
         * @param <R4> the result type of the fourth downstream collector
         * @param <R> the final result type
         * @param downstream1 the first downstream collector
         * @param downstream2 the second downstream collector
         * @param downstream3 the third downstream collector
         * @param downstream4 the fourth downstream collector
         * @param merger a function to merge the results of the four downstream collectors
         * @return a {@code Collector} which combines the results of four downstream collectors
         *         using the merger function
         * @throws IllegalArgumentException if any downstream collector or merger is null
         */
        public static <T, R1, R2, R3, R4, R> Collector<T, ?, R> combine(final Collector<? super T, ?, R1> downstream1,
                final Collector<? super T, ?, R2> downstream2, final Collector<? super T, ?, R3> downstream3, final Collector<? super T, ?, R4> downstream4,
                final QuadFunction<? super R1, ? super R2, ? super R3, ? super R4, R> merger) throws IllegalArgumentException {
            N.checkArgNotNull(downstream1, cs.downstream1);
            N.checkArgNotNull(downstream2, cs.downstream2);
            N.checkArgNotNull(downstream3, cs.downstream3);
            N.checkArgNotNull(downstream4, cs.downstream4);
            N.checkArgNotNull(merger, cs.merger);

            final List<Collector<? super T, ?, ?>> downstreams = Array.asList(downstream1, downstream2, downstream3, downstream4);

            final Function<Object[], R> finalMerger = a -> merger.apply((R1) a[0], (R2) a[1], (R3) a[2], (R4) a[3]);

            return combine(downstreams, finalMerger);
        }

        /**
         * Returns a {@code Collector} that combines the results of multiple downstream collectors
         * using a specified merger function.
         * 
         * <p>This collector is useful when you need to perform multiple different collection
         * operations on the same stream of elements and combine their results in a custom
         * way. Each element is processed by all downstream collectors, and their final
         * results are combined using the provided merger function that receives an array
         * of results.</p>
         * 
         * <p>The returned collector forwards elements to all downstream collectors and
         * combines their results using the provided merger. The merger function receives
         * an array containing the results from each downstream collector in the same order
         * as they appear in the collection. The characteristics of the returned collector
         * are the intersection of the characteristics of all downstream collectors,
         * excluding {@code IDENTITY_FINISH}.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Multiple collectors with dynamic combination
         * List<Collector<Order, ?, ?>> collectors = Arrays.asList(
         *     Collectors.counting(),
         *     Collectors.summingDouble(Order::getAmount),
         *     Collectors.averagingDouble(Order::getAmount),
         *     Collectors.mapping(Order::getCustomer, Collectors.toSet())
         * );
         * 
         * OrderSummary summary = orders.stream()
         *     .collect(MoreCollectors.combine(collectors, 
         *         results -> new OrderSummary(
         *             (Long) results[0],
         *             (Double) results[1], 
         *             (Double) results[2],
         *             (Set<String>) results[3]
         *         )
         *     ));
         * }</pre>
         *
         * @param <T> the type of input elements
         * @param <R> the final result type
         * @param downstreams a collection of downstream collectors to combine
         * @param merger a function to merge the results array from all downstream collectors
         * @return a {@code Collector} which combines the results of multiple downstream collectors
         *         using the merger function
         * @throws IllegalArgumentException if downstreams is {@code null} or empty, or if merger is null
         */
        public static <T, R> Collector<T, ?, R> combine(final Collection<? extends Collector<? super T, ?, ?>> downstreams,
                final Function<Object[], R> merger) { //NOSONAR
            N.checkArgument(N.notEmpty(downstreams), "The specified 'collectors' can't be null or empty");
            N.checkArgNotNull(merger, cs.merger);

            final int size = downstreams.size();

            final Supplier<Object>[] suppliers = downstreams.stream().map(Collector::supplier).toArray(i -> new Supplier[size]);
            final BiConsumer<Object, ? super T>[] accumulators = downstreams.stream().map(Collector::accumulator).toArray(i -> new BiConsumer[size]);
            final BinaryOperator<Object>[] combiners = downstreams.stream().map(Collector::combiner).toArray(i -> new BinaryOperator[size]);
            final Function<Object, Object>[] finishers = downstreams.stream().map(Collector::finisher).toArray(i -> new Function[size]);

            final Supplier<Object[]> supplier = () -> {
                final Object[] a = new Object[size];

                for (int i = 0; i < size; i++) {
                    a[i] = suppliers[i].get();
                }

                return a;
            };

            final BiConsumer<Object[], T> accumulator = (a, e) -> {
                for (int i = 0; i < size; i++) {
                    accumulators[i].accept(a[i], e);
                }
            };

            final BinaryOperator<Object[]> combiner = (a, b) -> {
                for (int i = 0; i < size; i++) {
                    a[i] = combiners[i].apply(a[i], b[i]);
                }

                return a;
            };

            final Function<Object[], R> finisher = a -> {
                for (int i = 0; i < size; i++) {
                    a[i] = finishers[i].apply(a[i]);
                }

                return merger.apply(a);
            };

            final Collection<Characteristics> common = N.intersection(downstreams.stream().map(Collector::characteristics).filter(N::notEmpty).toList());

            common.remove(Characteristics.IDENTITY_FINISH);

            final Set<Characteristics> characteristics = N.isEmpty(common) ? N.emptySet() : N.newHashSet(common);

            return create(supplier, accumulator, combiner, finisher, characteristics);
        }

        /**
         * Returns a {@code Collector} that accumulates the input elements into a new {@code Dataset}.
         * 
         * <p>This collector is useful when you need to convert a stream of elements into a
         * {@code Dataset} structure. The collector creates a {@code Dataset} with auto-generated
         * column names based on the properties of the elements.</p>
         * 
         * <p>The returned collector accumulates elements into a list and then creates a
         * {@code Dataset} from that list. The column names are automatically determined
         * based on the structure of the elements.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Convert stream of entities to Dataset with auto-generated column names
         * List<Person> persons = Arrays.asList(
         *     new Person("John", 25),
         *     new Person("Jane", 30)
         * );
         * Dataset dataset = persons.stream()
         *     .collect(MoreCollectors.toDataset());
         * }</pre>
         *
         * @param <T> the type of input elements
         * @return a {@code Collector} which collects all input elements into a {@code Dataset}
         */
        public static <T> Collector<T, ?, Dataset> toDataset() {
            return toDataset(null);
        }

        /**
         * Returns a {@code Collector} that accumulates the input elements into a new {@code Dataset}
         * with specified column names.
         * 
         * <p>This collector is useful when you need to convert a stream of elements into a
         * {@code Dataset} structure with explicitly specified column names. The collector
         * uses the provided column names for the resulting data structure.</p>
         * 
         * <p>The returned collector accumulates elements into a list and then creates a
         * {@code Dataset} from that list using the specified column names. If column names
         * are not provided (null), they will be auto-generated based on the element structure.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Convert stream to Dataset with specified column names
         * List<String> columnNames = Arrays.asList("Name", "Age");
         * List<Person> persons = Arrays.asList(
         *     new Person("John", 25),
         *     new Person("Jane", 30)
         * );
         * Dataset dataset = persons.stream()
         *     .collect(MoreCollectors.toDataset(columnNames));
         * }</pre>
         *
         * @param <T> the type of input elements
         * @param columnNames the names of columns for the resulting {@code Dataset}, or {@code null} for auto-generated names
         * @return a {@code Collector} which collects all input elements into a {@code Dataset}
         *         with the specified column names
         */
        public static <T> Collector<T, ?, Dataset> toDataset(final List<String> columnNames) {
            @SuppressWarnings("rawtypes")
            final Collector<T, List<T>, List<T>> collector = (Collector) Collectors.toList();

            final Function<List<T>, Dataset> finisher = it -> N.notEmpty(columnNames) ? N.newDataset(columnNames, it) : N.newDataset(it);

            return create(collector.supplier(), collector.accumulator(), collector.combiner(), finisher, Collectors.CH_NOID);
        }
    }
}
