/*
 * Copyright (C) 2016 HaiYang Li
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeSet;
import java.util.function.BiPredicate;
import java.util.function.Function;

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.util.Difference.KeyValueDifference;
import com.landawn.abacus.util.function.TriPredicate;

/**
 * A utility class for comparing two collections, arrays, maps, or beans to identify their differences.
 * This sealed class provides a comprehensive framework for analyzing data structure differences with
 * strongly-typed results and flexible comparison capabilities.
 *
 * <p>The {@code Difference} class provides methods to find:
 * <ul>
 *   <li><b>Common elements:</b> Elements present in both structures</li>
 *   <li><b>Left-only elements:</b> Elements only present in the first structure</li>
 *   <li><b>Right-only elements:</b> Elements only present in the second structure</li>
 * </ul>
 *
 * <p>When comparing collections, the comparison takes occurrences into account. This means that if an element
 * appears multiple times in either collection, each occurrence is considered separately, providing accurate
 * frequency-based difference analysis.</p>
 *
 * <p><b>IMPORTANT - Sealed Class:</b>
 * <ul>
 *   <li>This is a <b>sealed class</b> that only permits {@link KeyValueDifference} as a direct subclass</li>
 *   <li>Specialized implementations include {@link MapDifference} and {@link BeanDifference} (both extend {@link KeyValueDifference})</li>
 *   <li>Cannot be extended by classes outside this hierarchy for API stability</li>
 *   <li>Provides controlled inheritance for type safety and specialized comparison logic</li>
 * </ul>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Type Safety:</b> Strongly typed with separate left and right collection types</li>
 *   <li><b>Flexible Comparison:</b> Supports arrays, collections, maps, and Java beans</li>
 *   <li><b>Occurrence-Aware:</b> Considers element frequency in collections for accurate differences</li>
 *   <li><b>Null Handling:</b> Proper handling of {@code null} inputs and {@code null} elements</li>
 *   <li><b>Equality Testing:</b> Built-in equality comparison based on difference results</li>
 *   <li><b>Rich Factory Methods:</b> Multiple factory methods for different data structure types</li>
 * </ul>
 *
 * <p><b>Common Use Cases:</b>
 * <ul>
 *   <li><b>Data Synchronization:</b> Identifying changes between data sets for sync operations</li>
 *   <li><b>Configuration Management:</b> Comparing configuration states and identifying changes</li>
 *   <li><b>Testing and Validation:</b> Verifying expected vs actual results in unit tests</li>
 *   <li><b>Version Control:</b> Analyzing differences between data structure versions</li>
 *   <li><b>Data Migration:</b> Identifying missing or extra data during migration processes</li>
 *   <li><b>Audit Trails:</b> Tracking changes and modifications to data structures</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b>
 * <pre>{@code
 * // Basic collection comparison
 * List<String> list1 = Arrays.asList("a", "b", "c", "b");
 * List<String> list2 = Arrays.asList("b", "c", "d", "c");
 * Difference<List<String>, List<String>> diff = Difference.of(list1, list2);
 *
 * List<String> common = diff.common();             // returns ["b", "c"]
 * List<String> onlyOnLeft = diff.onlyOnLeft();     // returns ["a", "b"]
 * List<String> onlyOnRight = diff.onlyOnRight();   // returns ["c", "d"]
 * boolean equal = diff.areEqual();                 // returns false
 *
 * // Array comparison with primitive types
 * int[] array1 = {1, 2, 3, 2};
 * int[] array2 = {2, 3, 4, 3};
 * Difference<IntList, IntList> intDiff = Difference.of(array1, array2);
 *
 * // Map comparison (using specialized MapDifference)
 * Map<String, Integer> map1 = Map.of("a", 1, "b", 2, "c", 3);
 * Map<String, Integer> map2 = Map.of("b", 2, "c", 4, "d", 5);
 * MapDifference<?, ?, ?> mapDiff = MapDifference.of(map1, map2);
 *
 * // Bean comparison (using specialized BeanDifference)
 * record Person(String name, int age) {}
 * Person person1 = new Person("John", 30);
 * Person person2 = new Person("John", 31);
 * BeanDifference<?, ?, ?> beanDiff = BeanDifference.of(person1, person2);
 * }</pre>
 *
 * <p><b>Supported Data Types:</b>
 * <ul>
 *   <li><b>Primitive Arrays:</b> boolean[], char[], byte[], short[], int[], long[], float[], double[]</li>
 *   <li><b>Object Arrays:</b> T[] with automatic List conversion</li>
 *   <li><b>Collections:</b> Any Collection implementations with flexible type compatibility</li>
 *   <li><b>Primitive Lists:</b> BooleanList, CharList, ByteList, ShortList, IntList, LongList, FloatList, DoubleList</li>
 *   <li><b>Maps:</b> Through specialized {@link MapDifference} subclass</li>
 *   <li><b>Java Beans:</b> Through specialized {@link BeanDifference} subclass</li>
 * </ul>
 *
 * <p><b>Factory Methods:</b>
 * <ul>
 *   <li>{@link #of(boolean[], boolean[])} - Boolean array comparison</li>
 *   <li>{@link #of(Object[], Object[])} - Generic object array comparison</li>
 *   <li>{@link #of(Collection, Collection)} - Collection comparison with type flexibility</li>
 *   <li>{@link #of(IntList, IntList)} - Primitive list comparison (and similar for other primitives)</li>
 *   <li>{@link MapDifference#of(Map, Map)} - Map comparison with value difference detection</li>
 *   <li>{@link BeanDifference#of(Object, Object)} - Bean property comparison</li>
 * </ul>
 *
 * <p><b>Comparison Algorithm:</b>
 * <ul>
 *   <li>Counts occurrences in the right-hand structure</li>
 *   <li>Consumes one right-hand occurrence for each matching left-hand element</li>
 *   <li>Collects unmatched elements while preserving their encounter order. Only the <i>number</i> of
 *       surplus occurrences is meaningful: when several right-hand elements are equal, the instances placed
 *       in {@link #onlyOnRight()} are the earliest ones in encounter order, not specifically the
 *       unmatched ones, so equal-but-distinct instances are not paired stably</li>
 *   <li>Uses {@code ArrayList} results for object arrays and collections, and the corresponding primitive-list type for primitive inputs</li>
 * </ul>
 *
 * <p><b>Performance Characteristics:</b>
 * <ul>
 *   <li>Time complexity: O(n + m) where n and m are the sizes of input structures. A key selection that is
 *       not already a {@link Set} is copied into a {@code HashSet} first, so supplying a {@code List} does
 *       not add a factor; a {@code Set} is used as given, which means a {@code SortedSet} selection is
 *       probed in O(log k) under its own comparator rather than by {@code equals}/{@code hashCode}</li>
 *   <li>Space complexity: O(n + m) for storing difference results</li>
 *   <li>Primitive overloads store their results in the corresponding primitive-list type. The occurrence
 *       counting itself is <i>not</i> primitive: each element is boxed into a {@code Multiset} of the
 *       wrapper type, so a large primitive comparison allocates accordingly (measured: ~114 MB for a
 *       single 1,000,000-element {@code IntList} pair)</li>
 * </ul>
 *
 * <p><b>Mutability and Thread Use:</b>
 * A {@code Difference} owns independent result containers, but those containers are mutable and are
 * returned directly by the accessors:
 * <ul>
 *   <li>All fields are final and set during construction</li>
 *   <li>Result collections are independent copies of the input data, not views</li>
 *   <li>Mutating an accessor result mutates the state subsequently observed through this instance</li>
 *   <li>Concurrent reads are safe only while no thread modifies a returned result container</li>
 *   <li>Factory methods can be called concurrently</li>
 * </ul>
 *
 * <p><b>Specialized Subclasses:</b>
 * <ul>
 *   <li><b>{@link MapDifference}:</b> Extends functionality for Map comparison with value difference detection</li>
 *   <li><b>{@link BeanDifference}:</b> Extends functionality for Java bean property comparison</li>
 *   <li><b>{@link KeyValueDifference}:</b> Abstract base for key-value structure comparisons</li>
 * </ul>
 *
 * <p><b>Integration Points:</b>
 * <ul>
 *   <li><b>{@link N}:</b> Utility methods for set operations and collection differences</li>
 *   <li><b>{@link Maps}:</b> Map-specific difference and comparison utilities</li>
 *   <li><b>Collection Framework:</b> Full compatibility with Java Collections</li>
 *   <li><b>Stream API:</b> Results can be processed with streams for further analysis</li>
 * </ul>
 *
 * <p><b>Best Practices:</b>
 * <ul>
 *   <li>Use appropriate factory methods based on your data structure type</li>
 *   <li>Check {@link #areEqual()} for quick equality testing before accessing difference details</li>
 *   <li>Use specialized subclasses (MapDifference, BeanDifference) for advanced comparison needs</li>
 *   <li>Consider element ordering requirements when interpreting results</li>
 * </ul>
 *
 * <p><b>Null Handling:</b>
 * <ul>
 *   <li>Null inputs are treated as empty collections</li>
 *   <li>Null elements within collections are handled according to element type</li>
 *   <li>Results maintain {@code null} safety with proper type checking</li>
 * </ul>
 *
 * @param <L> the type of the collection containing elements from the left (first) structure.
 * @param <R> the type of the collection containing elements from the right (second) structure.
 *
 * @see KeyValueDifference
 * @see MapDifference
 * @see BeanDifference
 * @see N#difference(Collection, Collection)
 * @see N#symmetricDifference(Collection, Collection)
 * @see N#excludeAll(Collection, Collection)
 * @see N#excludeAllToSet(Collection, Collection)
 * @see N#removeAll(Collection, Iterable)
 * @see N#intersection(Collection, Collection)
 * @see N#commonSet(Collection, Collection)
 * @see Maps#difference(Map, Map)
 * @see com.landawn.abacus.annotation.DiffIgnore
 */
public sealed class Difference<L, R> permits KeyValueDifference {

    /** The elements/entries found in both compared structures. Never {@code null}, but may be empty. */
    final L common;

    /** The elements/entries found only in the left (first) structure. Never {@code null}, but may be empty. */
    final L onlyOnLeft;

    /** The elements/entries found only in the right (second) structure. Never {@code null}, but may be empty. */
    final R onlyOnRight;

    /**
     * Creates a {@code Difference} from three already-computed result containers.
     * The containers are stored by reference; they are neither copied nor wrapped, and they are
     * handed straight back by {@link #common()}, {@link #onlyOnLeft()} and {@link #onlyOnRight()}.
     *
     * @param common the elements/entries found in both structures
     * @param onlyOnLeft the elements/entries found only in the left (first) structure
     * @param onlyOnRight the elements/entries found only in the right (second) structure
     */
    Difference(final L common, final L onlyOnLeft, final R onlyOnRight) {
        this.common = common;
        this.onlyOnLeft = onlyOnLeft;
        this.onlyOnRight = onlyOnRight;
    }

    /**
     * Compares two boolean arrays and identifies the differences between them.
     * <p>
     * This method creates a {@code Difference} object that contains:
     * <ul>
     *   <li>Common elements: boolean values that appear in both arrays (considering occurrences)</li>
     *   <li>Left only: boolean values that appear only in the first array</li>
     *   <li>Right only: boolean values that appear only in the second array</li>
     * </ul>
     *
     * <p>The comparison takes into account the number of occurrences of each value. For example,
     * if {@code true} appears twice in the first array and once in the second array, the result will
     * have one {@code true} in common and one {@code true} in left only.
     *
     * <p><b>Usage Examples:</b>
     * <pre>{@code
     * boolean[] a = {true, false, true};
     * boolean[] b = {true, true, true};
     * Difference<BooleanList, BooleanList> diff = Difference.of(a, b);
     * // common: [true, true]
     * // onlyOnLeft: [false]
     * // onlyOnRight: [true]
     * }</pre>
     *
     * @param a the first boolean array to compare. Can be {@code null}, which is treated as an empty array.
     * @param b the second boolean array to compare. Can be {@code null}, which is treated as an empty array.
     * @return a {@code Difference} object containing {@code BooleanList} instances for common elements,
     *         elements only in the first array, and elements only in the second array.
     * @see BooleanList#difference(BooleanList)
     * @see N#difference(Collection, Collection)
     */
    public static Difference<BooleanList, BooleanList> of(final boolean[] a, final boolean[] b) {
        return of(BooleanList.of(a), BooleanList.of(b));
    }

    /**
     * Compares two char arrays and identifies the differences between them.
     * <p>
     * This method creates a {@code Difference} object that contains:
     * <ul>
     *   <li>Common elements: characters that appear in both arrays (considering occurrences)</li>
     *   <li>Left only: characters that appear only in the first array</li>
     *   <li>Right only: characters that appear only in the second array</li>
     * </ul>
     *
     * <p>The comparison takes into account the number of occurrences of each value. For example,
     * if {@code 'a'} appears twice in the first array and once in the second array, the result will
     * have one {@code 'a'} in common and one {@code 'a'} in left only.
     *
     * <p><b>Usage Examples:</b>
     * <pre>{@code
     * char[] a = {'a', 'b', 'c', 'a'};
     * char[] b = {'a', 'b', 'd'};
     * Difference<CharList, CharList> diff = Difference.of(a, b);
     * // common: [a, b]
     * // onlyOnLeft: [c, a]
     * // onlyOnRight: [d]
     * }</pre>
     *
     * @param a the first char array to compare. Can be {@code null}, which is treated as an empty array.
     * @param b the second char array to compare. Can be {@code null}, which is treated as an empty array.
     * @return a {@code Difference} object containing {@code CharList} instances for common elements,
     *         elements only in the first array, and elements only in the second array.
     * @see CharList#difference(CharList)
     * @see N#difference(Collection, Collection)
     */
    public static Difference<CharList, CharList> of(final char[] a, final char[] b) {
        return of(CharList.of(a), CharList.of(b));
    }

    /**
     * Compares two byte arrays and identifies the differences between them.
     * <p>
     * This method creates a {@code Difference} object that contains:
     * <ul>
     *   <li>Common elements: byte values that appear in both arrays (considering occurrences)</li>
     *   <li>Left only: byte values that appear only in the first array</li>
     *   <li>Right only: byte values that appear only in the second array</li>
     * </ul>
     *
     * <p>The comparison takes into account the number of occurrences of each value. For example,
     * if {@code 1} appears twice in the first array and once in the second array, the result will
     * have one {@code 1} in common and one {@code 1} in left only.
     *
     * <p><b>Usage Examples:</b>
     * <pre>{@code
     * byte[] a = {1, 2, 3, 2};
     * byte[] b = {2, 3, 4, 3};
     * Difference<ByteList, ByteList> diff = Difference.of(a, b);
     * // common: [2, 3]
     * // onlyOnLeft: [1, 2]
     * // onlyOnRight: [3, 4]
     * }</pre>
     *
     * @param a the first byte array to compare. Can be {@code null}, which is treated as an empty array.
     * @param b the second byte array to compare. Can be {@code null}, which is treated as an empty array.
     * @return a {@code Difference} object containing {@code ByteList} instances for common elements,
     *         elements only in the first array, and elements only in the second array.
     * @see ByteList#difference(ByteList)
     * @see N#difference(Collection, Collection)
     */
    public static Difference<ByteList, ByteList> of(final byte[] a, final byte[] b) {
        return of(ByteList.of(a), ByteList.of(b));
    }

    /**
     * Compares two short arrays and identifies the differences between them.
     * <p>
     * This method creates a {@code Difference} object that contains:
     * <ul>
     *   <li>Common elements: short values that appear in both arrays (considering occurrences)</li>
     *   <li>Left only: short values that appear only in the first array</li>
     *   <li>Right only: short values that appear only in the second array</li>
     * </ul>
     *
     * <p>The comparison takes into account the number of occurrences of each value. For example,
     * if {@code 1} appears twice in the first array and once in the second array, the result will
     * have one {@code 1} in common and one {@code 1} in left only.
     *
     * <p><b>Usage Examples:</b>
     * <pre>{@code
     * short[] a = {1, 2, 3, 2};
     * short[] b = {2, 3, 4, 3};
     * Difference<ShortList, ShortList> diff = Difference.of(a, b);
     * // common: [2, 3]
     * // onlyOnLeft: [1, 2]
     * // onlyOnRight: [3, 4]
     * }</pre>
     *
     * @param a the first short array to compare. Can be {@code null}, which is treated as an empty array.
     * @param b the second short array to compare. Can be {@code null}, which is treated as an empty array.
     * @return a {@code Difference} object containing {@code ShortList} instances for common elements,
     *         elements only in the first array, and elements only in the second array.
     * @see ShortList#difference(ShortList)
     * @see N#difference(Collection, Collection)
     */
    public static Difference<ShortList, ShortList> of(final short[] a, final short[] b) {
        return of(ShortList.of(a), ShortList.of(b));
    }

    /**
     * Compares two int arrays and identifies the differences between them.
     * <p>
     * This method creates a {@code Difference} object that contains:
     * <ul>
     *   <li>Common elements: integer values that appear in both arrays (considering occurrences)</li>
     *   <li>Left only: integer values that appear only in the first array</li>
     *   <li>Right only: integer values that appear only in the second array</li>
     * </ul>
     *
     * <p>The comparison takes into account the number of occurrences of each value. For example,
     * if {@code 1} appears twice in the first array and once in the second array, the result will
     * have one {@code 1} in common and one {@code 1} in left only.
     *
     * <p><b>Usage Examples:</b>
     * <pre>{@code
     * int[] a = {1, 2, 3, 2};
     * int[] b = {2, 3, 4, 3};
     * Difference<IntList, IntList> diff = Difference.of(a, b);
     * // common: [2, 3]
     * // onlyOnLeft: [1, 2]
     * // onlyOnRight: [3, 4]
     * }</pre>
     *
     * @param a the first int array to compare. Can be {@code null}, which is treated as an empty array.
     * @param b the second int array to compare. Can be {@code null}, which is treated as an empty array.
     * @return a {@code Difference} object containing {@code IntList} instances for common elements,
     *         elements only in the first array, and elements only in the second array.
     * @see IntList#difference(IntList)
     * @see N#difference(Collection, Collection)
     */
    public static Difference<IntList, IntList> of(final int[] a, final int[] b) {
        return of(IntList.of(a), IntList.of(b));
    }

    /**
     * Compares two long arrays and identifies the differences between them.
     * <p>
     * This method creates a {@code Difference} object that contains:
     * <ul>
     *   <li>Common elements: long values that appear in both arrays (considering occurrences)</li>
     *   <li>Left only: long values that appear only in the first array</li>
     *   <li>Right only: long values that appear only in the second array</li>
     * </ul>
     *
     * <p>The comparison takes into account the number of occurrences of each value. For example,
     * if {@code 1} appears twice in the first array and once in the second array, the result will
     * have one {@code 1} in common and one {@code 1} in left only.
     *
     * <p><b>Usage Examples:</b>
     * <pre>{@code
     * long[] a = {1L, 2L, 3L, 2L};
     * long[] b = {2L, 3L, 4L, 3L};
     * Difference<LongList, LongList> diff = Difference.of(a, b);
     * // common: [2, 3]
     * // onlyOnLeft: [1, 2]
     * // onlyOnRight: [3, 4]
     * }</pre>
     *
     * @param a the first long array to compare. Can be {@code null}, which is treated as an empty array.
     * @param b the second long array to compare. Can be {@code null}, which is treated as an empty array.
     * @return a {@code Difference} object containing {@code LongList} instances for common elements,
     *         elements only in the first array, and elements only in the second array.
     * @see LongList#difference(LongList)
     * @see N#difference(Collection, Collection)
     */
    public static Difference<LongList, LongList> of(final long[] a, final long[] b) {
        return of(LongList.of(a), LongList.of(b));
    }

    /**
     * Compares two float arrays and identifies the differences between them.
     *
     * <p>Float comparison uses {@link Float#equals(Object)} semantics (equivalent to {@link Float#compare(float, float)}).
     * As a result, {@code NaN} is considered equal to {@code NaN}, and {@code -0.0f} is not considered equal to {@code +0.0f}.
     * The comparison accounts for the number of times each value appears.
     *
     * <p>
     * This method creates a {@code Difference} object that contains:
     * <ul>
     *   <li>Common elements: float values that appear in both arrays (considering occurrences)</li>
     *   <li>Left only: float values that appear only in the first array</li>
     *   <li>Right only: float values that appear only in the second array</li>
     * </ul>
     *
     * <p>The comparison takes into account the number of occurrences of each value. For example,
     * if {@code 1} appears twice in the first array and once in the second array, the result will
     * have one {@code 1} in common and one {@code 1} in left only.
     *
     * <p><b>{@code NaN} and signed zero.</b> Occurrences are counted after boxing, so a match follows
     * {@link Float#equals(Object)} rather than {@code ==}: two {@code NaN}s <i>do</i> match each other, and
     * {@code +0.0} and {@code -0.0} do <i>not</i> match. Same rule as {@link FloatList#difference(FloatList)}.
     *
     * <p><b>Usage Examples:</b>
     * <pre>{@code
     * float[] a = {1.0f, 2.0f, 3.0f, 2.0f};
     * float[] b = {2.0f, 3.0f, 4.0f, 3.0f};
     * Difference<FloatList, FloatList> diff = Difference.of(a, b);
     * // common: [2.0, 3.0]
     * // onlyOnLeft: [1.0, 2.0]
     * // onlyOnRight: [3.0, 4.0]
     * }</pre>
     *
     * @param a the first float array to compare. Can be {@code null}, which is treated as an empty array.
     * @param b the second float array to compare. Can be {@code null}, which is treated as an empty array.
     * @return a {@code Difference} object containing {@code FloatList} instances for common elements,
     *         elements only in the first array, and elements only in the second array.
     * @see FloatList#difference(FloatList)
     * @see N#difference(Collection, Collection)
     */
    public static Difference<FloatList, FloatList> of(final float[] a, final float[] b) {
        return of(FloatList.of(a), FloatList.of(b));
    }

    /**
     * Compares two double arrays and identifies the differences between them.
     *
     * <p>Double comparison uses {@link Double#equals(Object)} semantics (equivalent to {@link Double#compare(double, double)}).
     * As a result, {@code NaN} is considered equal to {@code NaN}, and {@code -0.0d} is not considered equal to {@code +0.0d}.
     * Multiple occurrences of the same value are tracked separately.
     *
     * <p>
     * This method creates a {@code Difference} object that contains:
     * <ul>
     *   <li>Common elements: double values that appear in both arrays (considering occurrences)</li>
     *   <li>Left only: double values that appear only in the first array</li>
     *   <li>Right only: double values that appear only in the second array</li>
     * </ul>
     *
     * <p>The comparison takes into account the number of occurrences of each value. For example,
     * if {@code 1} appears twice in the first array and once in the second array, the result will
     * have one {@code 1} in common and one {@code 1} in left only.
     *
     * <p><b>{@code NaN} and signed zero.</b> Occurrences are counted after boxing, so a match follows
     * {@link Double#equals(Object)} rather than {@code ==}: two {@code NaN}s <i>do</i> match each other, and
     * {@code +0.0} and {@code -0.0} do <i>not</i> match. Same rule as {@link DoubleList#difference(DoubleList)}.
     *
     * <p><b>Usage Examples:</b>
     * <pre>{@code
     * double[] a = {1.0, 2.0, 3.0, 2.0};
     * double[] b = {2.0, 3.0, 4.0, 3.0};
     * Difference<DoubleList, DoubleList> diff = Difference.of(a, b);
     * // common: [2.0, 3.0]
     * // onlyOnLeft: [1.0, 2.0]
     * // onlyOnRight: [3.0, 4.0]
     * }</pre>
     *
     * @param a the first double array to compare. Can be {@code null}, which is treated as an empty array.
     * @param b the second double array to compare. Can be {@code null}, which is treated as an empty array.
     * @return a {@code Difference} object containing {@code DoubleList} instances for common elements,
     *         elements only in the first array, and elements only in the second array.
     * @see DoubleList#difference(DoubleList)
     * @see N#difference(Collection, Collection)
     */
    public static Difference<DoubleList, DoubleList> of(final double[] a, final double[] b) {
        return of(DoubleList.of(a), DoubleList.of(b));
    }

    /**
     * Compares two object arrays and identifies the differences between them.
     *
     * <p>The arrays can contain different types (T1 and T2), allowing for comparison of arrays
     * with related but different element types. Elements are compared using their {@code equals}
     * method. The comparison respects the number of occurrences of each element.
     *
     * <p>
     * This method creates a {@code Difference} object that contains:
     * <ul>
     *   <li>Common elements: elements that appear in both arrays (considering occurrences)</li>
     *   <li>Left only: elements that appear only in the first array</li>
     *   <li>Right only: elements that appear only in the second array</li>
     * </ul>
     *
     * <p>The comparison takes into account the number of occurrences of each value. For example,
     * if {@code "apple"} appears twice in the first array and once in the second array, the result will
     * have one {@code "apple"} in common and one {@code "apple"} in left only.
     *
     * <p>The returned lists maintain the order of elements as they appear in the original arrays.
     *
     * <p><b>Usage Examples:</b>
     * <pre>{@code
     * String[] a = {"apple", "banana", "cherry", "banana"};
     * String[] b = {"banana", "cherry", "date", "date"};
     * Difference<List<String>, List<String>> diff = Difference.of(a, b);
     * // common: ["banana", "cherry"]
     * // onlyOnLeft: ["apple", "banana"]
     * // onlyOnRight: ["date", "date"]
     * }</pre>
     *
     * @param <T1> the element type of the first array
     * @param <T2> the element type of the second array
     * @param a the first array to compare. Can be {@code null}, which is treated as an empty array.
     * @param b the second array to compare. Can be {@code null}, which is treated as an empty array.
     * @return a non-{@code null} {@code Difference} whose three result lists are {@code ArrayList} instances
     * @see N#difference(Collection, Collection)
     * @see N#symmetricDifference(Collection, Collection)
     * @see N#excludeAll(Collection, Collection)
     * @see N#excludeAllToSet(Collection, Collection)
     * @see N#removeAll(Collection, Iterable)
     * @see N#intersection(Collection, Collection)
     * @see N#commonSet(Collection, Collection)
     */
    public static <T1, T2> Difference<List<T1>, List<T2>> of(final T1[] a, final T2[] b) {
        return of(Array.asList(a), Array.asList(b));
    }

    /**
     * Compares two collections and identifies the differences between them.
     *
     * <p>
     * This method creates a {@code Difference} object that contains:
     * <ul>
     *   <li>Common elements: elements that appear in both collections (considering occurrences)</li>
     *   <li>Left only: elements that appear only in the first collection</li>
     *   <li>Right only: elements that appear only in the second collection</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Collection<String> c1 = Arrays.asList("a", "b", "b", "c");
     * Collection<String> c2 = Arrays.asList("b", "b", "b", "d");
     * Difference<List<String>, List<String>> diff = Difference.of(c1, c2);
     * // Results in:
     * // common: ["b", "b"]
     * // onlyOnLeft: ["a", "c"]
     * // onlyOnRight: ["b", "d"]
     * }</pre>
     *
     * <p>The comparison takes into account the number of occurrences of each value. For example,
     * if {@code "apple"} appears twice in the first collection and once in the second collection, the result will
     * have one {@code "apple"} in common and one {@code "apple"} in left only.
     *
     * @param <T1> the element type of the first collection
     * @param <T2> the element type of the second collection
     * @param a the first collection to compare. Can be {@code null} or empty.
     * @param b the second collection to compare. Can be {@code null} or empty.
     * @return a non-{@code null} {@code Difference} whose three result lists are {@code ArrayList} instances
     * @see N#difference(Collection, Collection)
     * @see N#symmetricDifference(Collection, Collection)
     * @see N#excludeAll(Collection, Collection)
     * @see N#excludeAllToSet(Collection, Collection)
     * @see N#removeAll(Collection, Iterable)
     * @see N#intersection(Collection, Collection)
     * @see N#commonSet(Collection, Collection)
     */
    @SuppressWarnings("unlikely-arg-type")
    public static <T1, T2> Difference<List<T1>, List<T2>> of(final Collection<? extends T1> a, final Collection<? extends T2> b) {
        final List<T1> common = new ArrayList<>();
        final List<T1> onlyOnLeft = new ArrayList<>();
        final List<T2> onlyOnRight = new ArrayList<>();

        if (N.isEmpty(a)) {
            if (N.isEmpty(b)) {
                // Do nothing. All empty.
            } else {
                onlyOnRight.addAll(b);
            }
        } else if (N.isEmpty(b)) {
            onlyOnLeft.addAll(a);
        } else {
            final Multiset<T2> bOccurrences = Multiset.create(b);

            for (final T1 e : a) {
                //noinspection SuspiciousMethodCalls
                if (bOccurrences.remove(e)) {
                    common.add(e);
                } else {
                    onlyOnLeft.add(e);
                }
            }

            for (final T2 e : b) {
                if (bOccurrences.remove(e)) {
                    onlyOnRight.add(e);
                }

                if (bOccurrences.isEmpty()) {
                    break;
                }
            }
        }

        return new Difference<>(common, onlyOnLeft, onlyOnRight);
    }

    /**
     * Compares two BooleanLists and identifies the differences between them.
     * <p>
     * This method creates a {@code Difference} object that contains:
     * <ul>
     *   <li>Common elements: boolean values that appear in both lists (considering occurrences)</li>
     *   <li>Left only: boolean values that appear only in the first list</li>
     *   <li>Right only: boolean values that appear only in the second list</li>
     * </ul>
     *
     * <p>The comparison takes into account the number of occurrences of each value. For example,
     * if {@code true} appears twice in the first list and once in the second list, the result will
     * have one {@code true} in common and one {@code true} in left only.
     *
     * <p><b>Usage Examples:</b>
     * <pre>{@code
     * BooleanList a = BooleanList.of(true, false, true);
     * BooleanList b = BooleanList.of(true, true, true);
     * Difference<BooleanList, BooleanList> diff = Difference.of(a, b);
     * // common: [true, true]
     * // onlyOnLeft: [false]
     * // onlyOnRight: [true]
     * }</pre>
     *
     * @param a the first BooleanList to compare. Can be {@code null} or empty.
     * @param b the second BooleanList to compare. Can be {@code null} or empty.
     * @return a {@code Difference} object containing BooleanLists for common elements,
     *         elements only in the first list, and elements only in the second list.
     * @see BooleanList#difference(BooleanList)
     * @see N#difference(Collection, Collection)
     */
    public static Difference<BooleanList, BooleanList> of(final BooleanList a, final BooleanList b) {
        final BooleanList common = new BooleanList();
        BooleanList onlyOnLeft = new BooleanList();
        BooleanList onlyOnRight = new BooleanList();

        if (N.isEmpty(a)) {
            if (N.isEmpty(b)) {
                // Do nothing. All empty.
            } else {
                onlyOnRight = b.copy();
            }
        } else if (N.isEmpty(b)) {
            onlyOnLeft = a.copy();
        } else {
            final Multiset<Boolean> bOccurrences = b.toMultiset();

            boolean e = false;
            for (int i = 0, len = a.size(); i < len; i++) {
                e = a.get(i);

                if (bOccurrences.remove(e)) {
                    common.add(e);
                } else {
                    onlyOnLeft.add(e);
                }
            }

            for (int i = 0, len = b.size(); i < len; i++) {
                e = b.get(i);

                if (bOccurrences.remove(e)) {
                    onlyOnRight.add(e);
                }

                if (bOccurrences.isEmpty()) {
                    break;
                }
            }
        }

        return new Difference<>(common, onlyOnLeft, onlyOnRight);
    }

    /**
     * Compares two CharLists and identifies the differences between them.
     * <p>
     * This method creates a {@code Difference} object that contains:
     * <ul>
     *   <li>Common elements: characters that appear in both lists (considering occurrences)</li>
     *   <li>Left only: characters that appear only in the first list</li>
     *   <li>Right only: characters that appear only in the second list</li>
     * </ul>
     *
     * <p>The comparison takes into account the number of occurrences of each value. For example,
     * if {@code 'a'} appears twice in the first list and once in the second list, the result will
     * have one {@code 'a'} in common and one {@code 'a'} in left only.
     *
     * <p><b>Usage Examples:</b>
     * <pre>{@code
     * CharList a = CharList.of('a', 'b', 'c', 'a');
     * CharList b = CharList.of('a', 'b', 'd');
     * Difference<CharList, CharList> diff = Difference.of(a, b);
     * // common: [a, b]
     * // onlyOnLeft: [c, a]
     * // onlyOnRight: [d]
     * }</pre>
     *
     * @param a the first CharList to compare. Can be {@code null} or empty.
     * @param b the second CharList to compare. Can be {@code null} or empty.
     * @return a {@code Difference} object containing CharLists for common elements,
     *         elements only in the first list, and elements only in the second list.
     * @see CharList#difference(CharList)
     * @see N#difference(Collection, Collection)
     */
    public static Difference<CharList, CharList> of(final CharList a, final CharList b) {
        final CharList common = new CharList();
        CharList onlyOnLeft = new CharList();
        CharList onlyOnRight = new CharList();

        if (N.isEmpty(a)) {
            if (N.isEmpty(b)) {
                // Do nothing. All empty.
            } else {
                onlyOnRight = b.copy();
            }
        } else if (N.isEmpty(b)) {
            onlyOnLeft = a.copy();
        } else {
            final Multiset<Character> bOccurrences = b.toMultiset();

            char e = 0;
            for (int i = 0, len = a.size(); i < len; i++) {
                e = a.get(i);

                if (bOccurrences.remove(e)) {
                    common.add(e);
                } else {
                    onlyOnLeft.add(e);
                }
            }

            for (int i = 0, len = b.size(); i < len; i++) {
                e = b.get(i);

                if (bOccurrences.remove(e)) {
                    onlyOnRight.add(e);
                }

                if (bOccurrences.isEmpty()) {
                    break;
                }
            }
        }

        return new Difference<>(common, onlyOnLeft, onlyOnRight);
    }

    /**
     * Compares two ByteLists and identifies the differences between them.
     * <p>
     * This method creates a {@code Difference} object that contains:
     * <ul>
     *   <li>Common elements: byte values that appear in both lists (considering occurrences)</li>
     *   <li>Left only: byte values that appear only in the first list</li>
     *   <li>Right only: byte values that appear only in the second list</li>
     * </ul>
     *
     * <p>The comparison takes into account the number of occurrences of each value. For example,
     * if {@code 1} appears twice in the first list and once in the second list, the result will
     * have one {@code 1} in common and one {@code 1} in left only.
     *
     * <p><b>Usage Examples:</b>
     * <pre>{@code
     * ByteList a = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 2);
     * ByteList b = ByteList.of((byte) 2, (byte) 3, (byte) 4, (byte) 3);
     * Difference<ByteList, ByteList> diff = Difference.of(a, b);
     * // common: [2, 3]
     * // onlyOnLeft: [1, 2]
     * // onlyOnRight: [3, 4]
     * }</pre>
     *
     * @param a the first ByteList to compare. Can be {@code null} or empty.
     * @param b the second ByteList to compare. Can be {@code null} or empty.
     * @return a {@code Difference} object containing ByteLists for common elements,
     *         elements only in the first list, and elements only in the second list.
     * @see ByteList#difference(ByteList)
     * @see N#difference(Collection, Collection)
     */
    public static Difference<ByteList, ByteList> of(final ByteList a, final ByteList b) {
        final ByteList common = new ByteList();
        ByteList onlyOnLeft = new ByteList();
        ByteList onlyOnRight = new ByteList();

        if (N.isEmpty(a)) {
            if (N.isEmpty(b)) {
                // Do nothing. All empty.
            } else {
                onlyOnRight = b.copy();
            }
        } else if (N.isEmpty(b)) {
            onlyOnLeft = a.copy();
        } else {
            final Multiset<Byte> bOccurrences = b.toMultiset();

            byte e = 0;
            for (int i = 0, len = a.size(); i < len; i++) {
                e = a.get(i);

                if (bOccurrences.remove(e)) {
                    common.add(e);
                } else {
                    onlyOnLeft.add(e);
                }
            }

            for (int i = 0, len = b.size(); i < len; i++) {
                e = b.get(i);

                if (bOccurrences.remove(e)) {
                    onlyOnRight.add(e);
                }

                if (bOccurrences.isEmpty()) {
                    break;
                }
            }
        }

        return new Difference<>(common, onlyOnLeft, onlyOnRight);
    }

    /**
     * Compares two ShortLists and identifies the differences between them.
     * <p>
     * This method creates a {@code Difference} object that contains:
     * <ul>
     *   <li>Common elements: short values that appear in both lists (considering occurrences)</li>
     *   <li>Left only: short values that appear only in the first list</li>
     *   <li>Right only: short values that appear only in the second list</li>
     * </ul>
     *
     * <p>The comparison takes into account the number of occurrences of each value. For example,
     * if {@code 1} appears twice in the first list and once in the second list, the result will
     * have one {@code 1} in common and one {@code 1} in left only.
     *
     * <p><b>Usage Examples:</b>
     * <pre>{@code
     * ShortList a = ShortList.of((short) 1, (short) 2, (short) 3, (short) 2);
     * ShortList b = ShortList.of((short) 2, (short) 3, (short) 4, (short) 3);
     * Difference<ShortList, ShortList> diff = Difference.of(a, b);
     * // common: [2, 3]
     * // onlyOnLeft: [1, 2]
     * // onlyOnRight: [3, 4]
     * }</pre>
     *
     * @param a the first ShortList to compare. Can be {@code null} or empty.
     * @param b the second ShortList to compare. Can be {@code null} or empty.
     * @return a {@code Difference} object containing ShortLists for common elements,
     *         elements only in the first list, and elements only in the second list.
     * @see ShortList#difference(ShortList)
     * @see N#difference(Collection, Collection)
     */
    public static Difference<ShortList, ShortList> of(final ShortList a, final ShortList b) {
        final ShortList common = new ShortList();
        ShortList onlyOnLeft = new ShortList();
        ShortList onlyOnRight = new ShortList();

        if (N.isEmpty(a)) {
            if (N.isEmpty(b)) {
                // Do nothing. All empty.
            } else {
                onlyOnRight = b.copy();
            }
        } else if (N.isEmpty(b)) {
            onlyOnLeft = a.copy();
        } else {
            final Multiset<Short> bOccurrences = b.toMultiset();

            short e = 0;
            for (int i = 0, len = a.size(); i < len; i++) {
                e = a.get(i);

                if (bOccurrences.remove(e)) {
                    common.add(e);
                } else {
                    onlyOnLeft.add(e);
                }
            }

            for (int i = 0, len = b.size(); i < len; i++) {
                e = b.get(i);

                if (bOccurrences.remove(e)) {
                    onlyOnRight.add(e);
                }

                if (bOccurrences.isEmpty()) {
                    break;
                }
            }
        }

        return new Difference<>(common, onlyOnLeft, onlyOnRight);
    }

    /**
     * Compares two IntLists and identifies the differences between them.
     * <p>
     * This method creates a {@code Difference} object that contains:
     * <ul>
     *   <li>Common elements: integer values that appear in both lists (considering occurrences)</li>
     *   <li>Left only: integer values that appear only in the first list</li>
     *   <li>Right only: integer values that appear only in the second list</li>
     * </ul>
     *
     * <p>The comparison takes into account the number of occurrences of each value. For example,
     * if {@code 1} appears twice in the first list and once in the second list, the result will
     * have one {@code 1} in common and one {@code 1} in left only.
     *
     * <p><b>Usage Examples:</b>
     * <pre>{@code
     * IntList a = IntList.of(1, 2, 3, 2);
     * IntList b = IntList.of(2, 3, 4, 3);
     * Difference<IntList, IntList> diff = Difference.of(a, b);
     * // common: [2, 3]
     * // onlyOnLeft: [1, 2]
     * // onlyOnRight: [3, 4]
     * }</pre>
     *
     * @param a the first IntList to compare. Can be {@code null} or empty.
     * @param b the second IntList to compare. Can be {@code null} or empty.
     * @return a {@code Difference} object containing IntLists for common elements,
     *         elements only in the first list, and elements only in the second list.
     * @see IntList#difference(IntList)
     * @see N#difference(Collection, Collection)
     */
    public static Difference<IntList, IntList> of(final IntList a, final IntList b) {
        final IntList common = new IntList();
        IntList onlyOnLeft = new IntList();
        IntList onlyOnRight = new IntList();

        if (N.isEmpty(a)) {
            if (N.isEmpty(b)) {
                // Do nothing. All empty.
            } else {
                onlyOnRight = b.copy();
            }
        } else if (N.isEmpty(b)) {
            onlyOnLeft = a.copy();
        } else {
            final Multiset<Integer> bOccurrences = b.toMultiset();

            int e = 0;
            for (int i = 0, len = a.size(); i < len; i++) {
                e = a.get(i);

                if (bOccurrences.remove(e)) {
                    common.add(e);
                } else {
                    onlyOnLeft.add(e);
                }
            }

            for (int i = 0, len = b.size(); i < len; i++) {
                e = b.get(i);

                if (bOccurrences.remove(e)) {
                    onlyOnRight.add(e);
                }

                if (bOccurrences.isEmpty()) {
                    break;
                }
            }
        }

        return new Difference<>(common, onlyOnLeft, onlyOnRight);
    }

    /**
     * Compares two LongLists and identifies the differences between them.
     * <p>
     * This method creates a {@code Difference} object that contains:
     * <ul>
     *   <li>Common elements: long values that appear in both lists (considering occurrences)</li>
     *   <li>Left only: long values that appear only in the first list</li>
     *   <li>Right only: long values that appear only in the second list</li>
     * </ul>
     *
     * <p>The comparison takes into account the number of occurrences of each value. For example,
     * if {@code 1} appears twice in the first list and once in the second list, the result will
     * have one {@code 1} in common and one {@code 1} in left only.
     *
     * <p><b>Usage Examples:</b>
     * <pre>{@code
     * LongList a = LongList.of(1L, 2L, 3L, 2L);
     * LongList b = LongList.of(2L, 3L, 4L, 3L);
     * Difference<LongList, LongList> diff = Difference.of(a, b);
     * // common: [2, 3]
     * // onlyOnLeft: [1, 2]
     * // onlyOnRight: [3, 4]
     * }</pre>
     *
     * @param a the first LongList to compare. Can be {@code null} or empty.
     * @param b the second LongList to compare. Can be {@code null} or empty.
     * @return a {@code Difference} object containing LongLists for common elements,
     *         elements only in the first list, and elements only in the second list.
     * @see LongList#difference(LongList)
     * @see N#difference(Collection, Collection)
     */
    public static Difference<LongList, LongList> of(final LongList a, final LongList b) {
        final LongList common = new LongList();
        LongList onlyOnLeft = new LongList();
        LongList onlyOnRight = new LongList();

        if (N.isEmpty(a)) {
            if (N.isEmpty(b)) {
                // Do nothing. All empty.
            } else {
                onlyOnRight = b.copy();
            }
        } else if (N.isEmpty(b)) {
            onlyOnLeft = a.copy();
        } else {
            final Multiset<Long> bOccurrences = b.toMultiset();

            long e = 0;
            for (int i = 0, len = a.size(); i < len; i++) {
                e = a.get(i);

                if (bOccurrences.remove(e)) {
                    common.add(e);
                } else {
                    onlyOnLeft.add(e);
                }
            }

            for (int i = 0, len = b.size(); i < len; i++) {
                e = b.get(i);

                if (bOccurrences.remove(e)) {
                    onlyOnRight.add(e);
                }

                if (bOccurrences.isEmpty()) {
                    break;
                }
            }
        }

        return new Difference<>(common, onlyOnLeft, onlyOnRight);
    }

    /**
     * Compares two FloatLists and identifies the differences between them.
     *
     * <p>Float values are compared using {@link Float#equals(Object)} semantics (equivalent to
     * {@link Float#compare(float, float)}). As a result, {@code NaN} is considered equal to {@code NaN},
     * and {@code -0.0f} is not considered equal to {@code +0.0f}. Each occurrence of a value
     * is tracked separately for accurate difference calculation.
     *
     * <p>
     * This method creates a {@code Difference} object that contains:
     * <ul>
     *   <li>Common elements: float values that appear in both lists (considering occurrences)</li>
     *   <li>Left only: float values that appear only in the first list</li>
     *   <li>Right only: float values that appear only in the second list</li>
     * </ul>
     *
     * <p>The comparison takes into account the number of occurrences of each value. For example,
     * if {@code 1} appears twice in the first list and once in the second list, the result will
     * have one {@code 1} in common and one {@code 1} in left only.
     *
     * <p><b>{@code NaN} and signed zero.</b> Occurrences are counted after boxing, so a match follows
     * {@link Float#equals(Object)} rather than {@code ==}: two {@code NaN}s <i>do</i> match each other, and
     * {@code +0.0} and {@code -0.0} do <i>not</i> match. Same rule as {@link FloatList#difference(FloatList)}.
     *
     * <p><b>Usage Examples:</b>
     * <pre>{@code
     * FloatList a = FloatList.of(1.0f, 2.0f, 3.0f, 2.0f);
     * FloatList b = FloatList.of(2.0f, 3.0f, 4.0f, 3.0f);
     * Difference<FloatList, FloatList> diff = Difference.of(a, b);
     * // common: [2.0, 3.0]
     * // onlyOnLeft: [1.0, 2.0]
     * // onlyOnRight: [3.0, 4.0]
     * }</pre>
     *
     * @param a the first FloatList to compare. Can be {@code null} or empty.
     * @param b the second FloatList to compare. Can be {@code null} or empty.
     * @return a {@code Difference} object containing FloatLists for common elements,
     *         elements only in the first list, and elements only in the second list.
     * @see FloatList#difference(FloatList)
     * @see N#difference(Collection, Collection)
     */
    public static Difference<FloatList, FloatList> of(final FloatList a, final FloatList b) {
        final FloatList common = new FloatList();
        FloatList onlyOnLeft = new FloatList();
        FloatList onlyOnRight = new FloatList();

        if (N.isEmpty(a)) {
            if (N.isEmpty(b)) {
                // Do nothing. All empty.
            } else {
                onlyOnRight = b.copy();
            }
        } else if (N.isEmpty(b)) {
            onlyOnLeft = a.copy();
        } else {
            final Multiset<Float> bOccurrences = b.toMultiset();

            float e = 0;
            for (int i = 0, len = a.size(); i < len; i++) {
                e = a.get(i);

                if (bOccurrences.remove(e)) {
                    common.add(e);
                } else {
                    onlyOnLeft.add(e);
                }
            }

            for (int i = 0, len = b.size(); i < len; i++) {
                e = b.get(i);

                if (bOccurrences.remove(e)) {
                    onlyOnRight.add(e);
                }

                if (bOccurrences.isEmpty()) {
                    break;
                }
            }
        }

        return new Difference<>(common, onlyOnLeft, onlyOnRight);
    }

    /**
     * Compares two DoubleLists and identifies the differences between them.
     *
     * <p>Double values are compared using {@link Double#equals(Object)} semantics (equivalent to
     * {@link Double#compare(double, double)}). As a result, {@code NaN} is considered equal to {@code NaN},
     * and {@code -0.0d} is not considered equal to {@code +0.0d}. The method efficiently
     * processes both lists while maintaining the original order of elements.
     *
     * <p>
     * This method creates a {@code Difference} object that contains:
     * <ul>
     *   <li>Common elements: double values that appear in both lists (considering occurrences)</li>
     *   <li>Left only: double values that appear only in the first list</li>
     *   <li>Right only: double values that appear only in the second list</li>
     * </ul>
     *
     * <p>The comparison takes into account the number of occurrences of each value. For example,
     * if {@code 1} appears twice in the first list and once in the second list, the result will
     * have one {@code 1} in common and one {@code 1} in left only.
     *
     * <p><b>{@code NaN} and signed zero.</b> Occurrences are counted after boxing, so a match follows
     * {@link Double#equals(Object)} rather than {@code ==}: two {@code NaN}s <i>do</i> match each other, and
     * {@code +0.0} and {@code -0.0} do <i>not</i> match. Same rule as {@link DoubleList#difference(DoubleList)}.
     *
     * <p><b>Usage Examples:</b>
     * <pre>{@code
     * DoubleList a = DoubleList.of(1.0, 2.0, 3.0, 2.0);
     * DoubleList b = DoubleList.of(2.0, 3.0, 4.0, 3.0);
     * Difference<DoubleList, DoubleList> diff = Difference.of(a, b);
     * // common: [2.0, 3.0]
     * // onlyOnLeft: [1.0, 2.0]
     * // onlyOnRight: [3.0, 4.0]
     * }</pre>
     *
     * @param a the first DoubleList to compare. Can be {@code null} or empty.
     * @param b the second DoubleList to compare. Can be {@code null} or empty.
     * @return a {@code Difference} object containing DoubleLists for common elements,
     *         elements only in the first list, and elements only in the second list.
     * @see DoubleList#difference(DoubleList)
     * @see N#difference(Collection, Collection)
     */
    public static Difference<DoubleList, DoubleList> of(final DoubleList a, final DoubleList b) {
        final DoubleList common = new DoubleList();
        DoubleList onlyOnLeft = new DoubleList();
        DoubleList onlyOnRight = new DoubleList();

        if (N.isEmpty(a)) {
            if (N.isEmpty(b)) {
                // Do nothing. All empty.
            } else {
                onlyOnRight = b.copy();
            }
        } else if (N.isEmpty(b)) {
            onlyOnLeft = a.copy();
        } else {
            final Multiset<Double> bOccurrences = b.toMultiset();

            double e = 0;
            for (int i = 0, len = a.size(); i < len; i++) {
                e = a.get(i);

                if (bOccurrences.remove(e)) {
                    common.add(e);
                } else {
                    onlyOnLeft.add(e);
                }
            }

            for (int i = 0, len = b.size(); i < len; i++) {
                e = b.get(i);

                if (bOccurrences.remove(e)) {
                    onlyOnRight.add(e);
                }

                if (bOccurrences.isEmpty()) {
                    break;
                }
            }
        }

        return new Difference<>(common, onlyOnLeft, onlyOnRight);
    }

    /**
     * Returns the elements that are common to both collections/arrays.
     * <p>
     * These are elements that appear in both the left (first) and right (second) collections.
     * The number of occurrences in the result corresponds to the minimum number of occurrences
     * in either collection. For example, if an element appears 3 times in the first collection
     * and 2 times in the second, it will appear 2 times in the common elements.
     *
     * <p>For collection/array comparisons the returned collection maintains the order of elements
     * as they appear in the first collection. For the single-map / single-bean {@link MapDifference}
     * and {@link BeanDifference} comparisons the returned value is instead a map of the common
     * entries/properties; for their collection-based comparisons it is a {@code List} of the
     * maps/beans matched on both sides.
     *
     * <p><b>Usage Examples:</b>
     * <pre>{@code
     * List<String> list1 = Arrays.asList("a", "b", "c", "b");
     * List<String> list2 = Arrays.asList("b", "c", "d", "c");
     * Difference<List<String>, List<String>> diff = Difference.of(list1, list2);
     * List<String> common = diff.common();  // returns ["b", "c"]
     * }</pre>
     *
     * @return a non-{@code null} collection (or map, for key-value differences) containing the common
     *         elements; never {@code null} but may be empty
     * @see #onlyOnLeft()
     * @see #onlyOnRight()
     */
    public L common() {
        return common;
    }

    /**
     * Returns the elements that exist only in the left (first) collection/array.
     * <p>
     * These are elements that appear in the first collection but not in the second collection,
     * or elements that appear more frequently in the first collection than in the second.
     * For example, if an element appears 5 times in the first collection and 2 times in the
     * second collection, it will appear 3 times in the left-only elements.
     *
     * <p>For collection/array comparisons the returned collection maintains the order of elements
     * as they appear in the first collection. For the single-map / single-bean {@link MapDifference}
     * and {@link BeanDifference} comparisons the returned value is instead a map of the
     * entries/properties present only on the left; for their collection-based comparisons it is a
     * {@code List} of the maps/beans matched only on the left.
     *
     * <p><b>Usage Examples:</b>
     * <pre>{@code
     * List<String> list1 = Arrays.asList("a", "b", "c", "b");
     * List<String> list2 = Arrays.asList("b", "c", "d", "c");
     * Difference<List<String>, List<String>> diff = Difference.of(list1, list2);
     * List<String> onlyOnLeft = diff.onlyOnLeft();  // returns ["a", "b"]
     * }</pre>
     *
     * @return a non-{@code null} collection (or map, for key-value differences) containing elements
     *         present only in the left structure; never {@code null} but may be empty
     * @see #common()
     * @see #onlyOnRight()
     */
    public L onlyOnLeft() {
        return onlyOnLeft;
    }

    /**
     * Returns the elements that exist only in the right (second) collection/array.
     * <p>
     * These are elements that appear in the second collection but not in the first collection,
     * or elements that appear more frequently in the second collection than in the first.
     * For example, if an element appears 2 times in the first collection and 5 times in the
     * second collection, it will appear 3 times in the right-only elements.
     *
     * <p>For collection/array comparisons the returned collection maintains the order of elements
     * as they appear in the second collection. For the single-map / single-bean {@link MapDifference}
     * and {@link BeanDifference} comparisons the returned value is instead a map of the
     * entries/properties present only on the right; for their collection-based comparisons it is a
     * {@code List} of the maps/beans matched only on the right.
     *
     * <p>When several right-hand elements compare equal, the count is exact but the identities are not:
     * the instances reported are the earliest equal occurrences in the second collection, not specifically
     * the ones left over after matching. This is only observable for equal-but-distinct instances.
     *
     * <p><b>Usage Examples:</b>
     * <pre>{@code
     * List<String> list1 = Arrays.asList("a", "b", "c", "b");
     * List<String> list2 = Arrays.asList("b", "c", "d", "c");
     * Difference<List<String>, List<String>> diff = Difference.of(list1, list2);
     * List<String> onlyOnRight = diff.onlyOnRight();  // returns ["c", "d"]
     * }</pre>
     *
     * @return a non-{@code null} collection (or map, for key-value differences) containing elements
     *         present only in the right structure; never {@code null} but may be empty
     * @see #common()
     * @see #onlyOnLeft()
     */
    public R onlyOnRight() {
        return onlyOnRight;
    }

    /**
     * Checks whether the two compared collections/arrays contain exactly the same elements with the same occurrences.
     * <p>
     * This method returns {@code true} if and only if:
     * <ul>
     *   <li>Both onlyOnLeft and onlyOnRight collections are empty</li>
     *   <li>All elements from both input collections are in the common elements</li>
     * </ul>
     *
     * <p>Note that element order is not considered - only the presence and count of each element matters.
     * For example, [1, 2, 3] and [3, 2, 1] would be considered equal, but [1, 2, 2] and [1, 2, 3]
     * would not be equal.
     *
     * <p><b>Usage Examples:</b>
     * <pre>{@code
     * List<String> list1 = Arrays.asList("a", "b", "c");
     * List<String> list2 = Arrays.asList("c", "b", "a");
     * Difference<List<String>, List<String>> diff = Difference.of(list1, list2);
     * boolean equal = diff.areEqual();  // returns true (order does not matter)
     * }</pre>
     *
     * @return {@code true} if the two compared collections have exactly the same elements with the same occurrences, {@code false} otherwise
     * @see #common()
     * @see #onlyOnLeft()
     * @see #onlyOnRight()
     */
    public boolean areEqual() {
        // Each container is tested on its own type. Deciding the kind once from onlyOnLeft and casting
        // onlyOnRight to it would bake in an assumption - that L and R are always the same kind - that
        // nothing here enforces, and an unrecognised container would make the whole test silently false,
        // reporting "not equal" for two empty results.
        return isEmptyResult(onlyOnLeft) && isEmptyResult(onlyOnRight);
    }

    /**
     * Throws if {@code c} holds two elements with the same extracted identifier.
     *
     * <p>Same rule, and the same {@link IllegalStateException}, as the
     * {@code N.toMap(.., Fn.throwingMerger(), ..)} that the both-sides-non-empty path of the collection-based
     * {@code MapDifference.of}/{@code BeanDifference.of} factories builds - so a duplicate identifier is
     * reported whether or not the other collection happens to be empty.</p>
     *
     * @param <T> the element type
     * @param c the collection to check; must be non-empty
     * @param idExtractor the identifier function
     * @throws IllegalStateException if two elements produce the same identifier
     */
    private static <T> void checkNoDuplicateIds(final Collection<? extends T> c, final Function<? super T, ?> idExtractor) throws IllegalStateException {
        N.toMap(c, idExtractor, Fn.identity(), Fn.throwingMerger(), IntFunctions.ofLinkedHashMap());
    }

    /**
     * Reports whether one of the three result-container kinds this class produces is empty.
     *
     * @param result a {@link #common()}/{@link #onlyOnLeft()}/{@link #onlyOnRight()} container
     * @return {@code true} if it holds no elements/entries
     * @throws IllegalStateException if {@code result} is none of the container kinds this class produces -
     *         a bug in a factory rather than a caller error, so it fails loudly instead of answering
     *         {@code false} for something it cannot measure
     */
    private static boolean isEmptyResult(final Object result) throws IllegalStateException {
        if (result instanceof Collection) {
            return ((Collection<?>) result).isEmpty();
        } else if (result instanceof Map) {
            return ((Map<?, ?>) result).isEmpty();
        } else if (result instanceof PrimitiveList) {
            return ((PrimitiveList<?, ?, ?>) result).isEmpty();
        }

        throw new IllegalStateException("Unsupported difference result container: " + (result == null ? "null" : result.getClass().getName()));
    }

    /**
     * Compares this {@code Difference} object with another object for equality.
     * <p>
     * Two {@code Difference} objects are considered equal if they have the same:
     * <ul>
     *   <li>Common elements (compared using equals)</li>
     *   <li>Left-only elements (compared using equals)</li>
     *   <li>Right-only elements (compared using equals)</li>
     * </ul>
     *
     * <p>This method properly handles {@code null} values and ensures type safety by checking
     * that the compared object is also a {@code Difference} instance.
     *
     * <p><b>Usage Examples:</b>
     * <pre>{@code
     * Difference<List<String>, List<String>> diff1 = Difference.of(Arrays.asList("a", "b"), Arrays.asList("a", "c"));
     * Difference<List<String>, List<String>> diff2 = Difference.of(Arrays.asList("a", "b"), Arrays.asList("a", "c"));
     * boolean eq = diff1.equals(diff2);  // returns true
     * }</pre>
     *
     * @param obj the object to compare with this {@code Difference}; may be {@code null}
     * @return {@code true} if the specified object is also a {@code Difference} with equal common,
     *         left-only, and right-only contents; {@code false} otherwise (including when {@code obj} is {@code null})
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        // Exclude KeyValueDifference: its equals also compares withDifferentValues(), so accepting
        // it here would make equals asymmetric (base.equals(kv) true, kv.equals(base) false) and
        // inconsistent with hashCode.
        if (obj instanceof Difference<?, ?> other && !(obj instanceof KeyValueDifference)) {
            return common().equals(other.common()) && onlyOnLeft().equals(other.onlyOnLeft()) && onlyOnRight().equals(other.onlyOnRight());
        }

        return false;
    }

    /**
     * Returns a hash code value for this {@code Difference} object.
     * <p>
     * The hash code is computed based on the hash codes of the common elements,
     * left-only elements, and right-only elements. This ensures that two {@code Difference}
     * objects that are equal according to the {@link #equals(Object)} method will have
     * the same hash code.
     *
     * <p>The implementation uses a standard hash code calculation pattern with a prime
     * number (31) to combine the hash codes of the three components.
     *
     * <p><b>Usage Examples:</b>
     * <pre>{@code
     * Difference<List<String>, List<String>> diff = Difference.of(Arrays.asList("a", "b"), Arrays.asList("a", "c"));
     * int hash = diff.hashCode();  // returns consistent with equals()
     * }</pre>
     *
     * @return a hash code value for this {@code Difference} object
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + N.hashCode(common());
        result = prime * result + N.hashCode(onlyOnLeft());
        return prime * result + N.hashCode(onlyOnRight());
    }

    /**
     * Returns a string representation of this {@code Difference} object.
     * <p>
     * The string representation includes:
     * <ul>
     *   <li>The result of {@link #areEqual()}</li>
     *   <li>The common elements</li>
     *   <li>The left-only elements</li>
     *   <li>The right-only elements</li>
     * </ul>
     *
     * <p>Format: {@code {areEqual=<boolean>, common=<common>, onlyOnLeft=<onlyOnLeft>, onlyOnRight=<onlyOnRight>}}
     *
     * <p>This method is useful for debugging and logging purposes.
     *
     * <p><b>Usage Examples:</b>
     * <pre>{@code
     * Difference<List<String>, List<String>> diff = Difference.of(Arrays.asList("a", "b"), Arrays.asList("a", "c"));
     * String str = diff.toString();  // returns "{areEqual=false, common=[a], onlyOnLeft=[b], onlyOnRight=[c]}"
     * }</pre>
     *
     * @return a string representation of this {@code Difference} object
     */
    @Override
    public String toString() {
        return "{areEqual=" + areEqual() + ", common=" + common + ", onlyOnLeft=" + onlyOnLeft + ", onlyOnRight=" + onlyOnRight + "}";
    }

    /**
     * Abstract base class for comparing key-value structures (maps or beans) that provides additional
     * functionality beyond basic difference comparison.
     * <p>
     * In addition to the standard difference operations (common, left-only, right-only), this class
     * identifies entries/properties that exist in both structures but have different values.
     *
     * <p>This class serves as the base for:
     * <ul>
     *   <li>{@link MapDifference} - for comparing maps</li>
     *   <li>{@link BeanDifference} - for comparing Java beans</li>
     * </ul>
     *
     * @param <L> the type of the collection containing entries/properties from the left (first) structure
     * @param <R> the type of the collection containing entries/properties from the right (second) structure
     * @param <D> the type of the collection containing entries/properties with different values
     * @see com.landawn.abacus.annotation.DiffIgnore
     * @see Maps#difference(Map, Map)
     * @see Maps#symmetricDifference(Map, Map)
     * @see N#difference(Collection, Collection)
     * @see N#symmetricDifference(Collection, Collection)
     * @see N#excludeAll(Collection, Collection)
     * @see N#excludeAllToSet(Collection, Collection)
     * @see N#removeAll(Collection, Iterable)
     * @see N#intersection(Collection, Collection)
     * @see N#commonSet(Collection, Collection)
     */
    public abstract static sealed class KeyValueDifference<L, R, D> extends Difference<L, R> permits MapDifference, BeanDifference {

        /**
         * The entries/properties present in both structures whose values are not equivalent.
         * Never {@code null}, but may be empty.
         */
        private final D diffValues;

        /**
         * Creates a {@code KeyValueDifference} from four already-computed result containers.
         * The containers are stored by reference; they are neither copied nor wrapped.
         *
         * @param common the entries/properties found in both structures with equivalent values
         * @param onlyOnLeft the entries/properties found only in the left (first) structure
         * @param onlyOnRight the entries/properties found only in the right (second) structure
         * @param differentValues the entries/properties found in both structures but with non-equivalent values
         */
        KeyValueDifference(final L common, final L onlyOnLeft, final R onlyOnRight, final D differentValues) {
            super(common, onlyOnLeft, onlyOnRight);
            diffValues = differentValues;
        }

        /**
         * Returns the entries/properties that exist in both structures but have different values.
         * <p>
         * For maps, this returns a map where:
         * <ul>
         *   <li>Keys are the keys present in both maps whose values are not equivalent</li>
         *   <li>Values are {@link Pair} objects containing the values from the left and right maps</li>
         * </ul>
         *
         * <p>For beans, this returns a map where:
         * <ul>
         *   <li>Keys are the property names that exist in both beans but whose values are not equivalent</li>
         *   <li>Values are {@link Pair} objects containing the property values from the left and right beans</li>
         * </ul>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, Integer> map1 = Map.of("a", 1, "b", 2);
         * Map<String, Integer> map2 = Map.of("a", 1, "b", 3);
         * MapDifference<?, ?, ?> diff = MapDifference.of(map1, map2);
         * // diff.differentValues() returns {"b": Pair.of(2, 3)}
         * }</pre>
         *
         * @return a non-{@code null} map containing entries/properties with different values; for the
         *         single-map and single-bean comparisons the keys are the keys/property names that are
         *         present in both structures but whose values are not equivalent, and the values are
         *         {@link Pair} instances holding the left and right values respectively
         *         (collection-based comparisons instead map each matched identifier to a nested
         *         {@code Difference} describing the per-entry/per-property changes). May be empty but never {@code null}.
         * @see #areEqual()
         */
        public D differentValues() {
            return diffValues;
        }

        /**
         * Checks whether the two compared structures have exactly the same entries/properties with the same values.
         * <p>
         * This method returns {@code true} if and only if:
         * <ul>
         *   <li>Both structures have no entries/properties that exist only in one structure (onlyOnLeft and onlyOnRight are empty)</li>
         *   <li>There are no entries/properties with different values (differentValues is empty)</li>
         * </ul>
         *
         * <p>This is a stricter equality check than the parent class's {@link Difference#areEqual()} method,
         * as it also considers whether any entries/properties have different values.
         *
         * <p><b>Usage Examples:</b>
         * <pre>{@code
         * Map<String, Integer> map1 = Map.of("a", 1, "b", 2);
         * Map<String, Integer> map2 = Map.of("a", 1, "b", 3);
         * MapDifference<?, ?, ?> diff = MapDifference.of(map1, map2);
         * boolean equal = diff.areEqual();  // returns false (different values for "b")
         * }</pre>
         *
         * @return {@code true} if the two structures have exactly the same entries/properties with the same values, {@code false} otherwise
         * @see #differentValues()
         */
        @Override
        public boolean areEqual() {
            // Measured through the same helper as the three base containers, so a container kind it cannot
            // measure fails loudly here too rather than reporting "not equal".
            return super.areEqual() && isEmptyResult(diffValues);
        }

        /**
         * Compares this {@code KeyValueDifference} object with another object for equality.
         * <p>
         * Two {@code KeyValueDifference} objects are considered equal if they have the same:
         * <ul>
         *   <li>Common entries/properties</li>
         *   <li>Left-only entries/properties</li>
         *   <li>Right-only entries/properties</li>
         *   <li>Entries/properties with different values</li>
         * </ul>
         *
         * <p><b>Usage Examples:</b>
         * <pre>{@code
         * Map<String, Integer> map1 = Map.of("a", 1);
         * Map<String, Integer> map2 = Map.of("a", 2);
         * MapDifference<?, ?, ?> diff1 = MapDifference.of(map1, map2);
         * MapDifference<?, ?, ?> diff2 = MapDifference.of(map1, map2);
         * boolean eq = diff1.equals(diff2);  // returns true
         * }</pre>
         *
         * @param obj the object to compare with this {@code KeyValueDifference}; may be {@code null}
         * @return {@code true} if the specified object is of exactly the same class as this one and has equal
         *         common, left-only, right-only, and different-value contents; {@code false} otherwise
         *         (including when {@code obj} is {@code null})
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }

            // Require the exact same class rather than any KeyValueDifference: a MapDifference over
            // Map<String, Object> and a BeanDifference hold structurally identical containers, so an
            // "instanceof KeyValueDifference" test makes results of two different comparisons compare
            // equal. This mirrors the base class, which excludes KeyValueDifference for the same reason.
            if (obj != null && obj.getClass() == getClass()) {
                final KeyValueDifference<?, ?, ?> other = (KeyValueDifference<?, ?, ?>) obj;

                return common().equals(other.common()) && onlyOnLeft().equals(other.onlyOnLeft()) && onlyOnRight().equals(other.onlyOnRight())
                        && differentValues().equals(other.differentValues());
            }

            return false;
        }

        /**
         * Returns a hash code value for this {@code KeyValueDifference} object.
         * <p>
         * The hash code is computed based on the hash codes of:
         * <ul>
         *   <li>Common entries/properties</li>
         *   <li>Left-only entries/properties</li>
         *   <li>Right-only entries/properties</li>
         *   <li>Entries/properties with different values</li>
         * </ul>
         *
         * <p><b>Usage Examples:</b>
         * <pre>{@code
         * Map<String, Integer> map1 = Map.of("a", 1);
         * Map<String, Integer> map2 = Map.of("a", 2);
         * MapDifference<?, ?, ?> diff = MapDifference.of(map1, map2);
         * int hash = diff.hashCode();  // returns consistent with equals()
         * }</pre>
         *
         * @return a hash code value for this {@code KeyValueDifference} object
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + N.hashCode(common());
            result = prime * result + N.hashCode(onlyOnLeft());
            result = prime * result + N.hashCode(onlyOnRight());
            return prime * result + N.hashCode(differentValues());
        }

        /**
         * Returns a string representation of this {@code KeyValueDifference} object.
         * <p>
         * The string representation includes:
         * <ul>
         *   <li>The result of {@link #areEqual()}</li>
         *   <li>The common entries/properties</li>
         *   <li>The left-only entries/properties</li>
         *   <li>The right-only entries/properties</li>
         *   <li>The entries/properties with different values</li>
         * </ul>
         *
         * <p>Format: {@code {areEqual=<boolean>, common=<common>, onlyOnLeft=<onlyOnLeft>, onlyOnRight=<onlyOnRight>, differentValues=<diffValues>}}
         *
         * <p><b>Usage Examples:</b>
         * <pre>{@code
         * Map<String, Integer> map1 = Map.of("a", 1, "b", 2);
         * Map<String, Integer> map2 = Map.of("a", 1, "b", 3);
         * MapDifference<?, ?, ?> diff = MapDifference.of(map1, map2);
         * String str = diff.toString();  // returns "{areEqual=false, common={a=1}, onlyOnLeft={}, onlyOnRight={}, differentValues={b=(2, 3)}}"
         * }</pre>
         *
         * @return a string representation of this {@code KeyValueDifference} object
         */
        @Override
        public String toString() {
            return "{areEqual=" + areEqual() + ", common=" + common + ", onlyOnLeft=" + onlyOnLeft + ", onlyOnRight=" + onlyOnRight + ", differentValues="
                    + diffValues + "}";
        }
    }

    /**
     * Represents the difference between two maps, providing detailed comparison results including
     * common entries, entries unique to each map, and entries with different values.
     * <p>
     * This class extends {@link KeyValueDifference} to provide map-specific comparison functionality.
     * It can compare maps with different key and value types, and supports custom value equivalence
     * predicates for flexible comparison logic.
     *
     * <p>The comparison results include:
     * <ul>
     *   <li>Common entries: Key-value pairs that exist in both maps with equal values</li>
     *   <li>Left-only entries: Key-value pairs that exist only in the first map</li>
     *   <li>Right-only entries: Key-value pairs that exist only in the second map</li>
     *   <li>Different values: Keys that exist in both maps but with different values</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map1 = Map.of("a", 1, "b", 2, "c", 3);
     * Map<String, Integer> map2 = Map.of("b", 2, "c", 4, "d", 5);
     * MapDifference<?, ?, ?> diff = MapDifference.of(map1, map2);
     *
     * // diff.common() returns {"b": 2}
     * // diff.onlyOnLeft() returns {"a": 1}
     * // diff.onlyOnRight() returns {"d": 5}
     * // diff.differentValues() returns {"c": Pair.of(3, 4)}
     * }</pre>
     *
     * @param <L> the type of the map containing entries from the left (first) map
     * @param <R> the type of the map containing entries from the right (second) map
     * @param <D> the type of the map containing entries with different values (typically Map with Pair values)
     * @see com.landawn.abacus.annotation.DiffIgnore
     * @see Maps#difference(Map, Map)
     * @see Maps#symmetricDifference(Map, Map)
     * @see N#difference(Collection, Collection)
     * @see N#symmetricDifference(Collection, Collection)
     * @see N#excludeAll(Collection, Collection)
     * @see N#excludeAllToSet(Collection, Collection)
     * @see N#removeAll(Collection, Iterable)
     * @see N#intersection(Collection, Collection)
     * @see N#commonSet(Collection, Collection)
     */
    public static final class MapDifference<L, R, D> extends KeyValueDifference<L, R, D> {

        /**
         * Creates a {@code MapDifference} from four already-computed result containers.
         * The containers are stored by reference; they are neither copied nor wrapped.
         *
         * @param common the entries found in both maps with equivalent values
         * @param onlyOnLeft the entries found only in the first map
         * @param onlyOnRight the entries found only in the second map
         * @param differentValues the entries whose keys are present in both maps but whose values are not equivalent
         */
        MapDifference(final L common, final L onlyOnLeft, final R onlyOnRight, final D differentValues) {
            super(common, onlyOnLeft, onlyOnRight, differentValues);
        }

        /**
         * Creates the set used to record which entries of the right-hand map the left-hand pass already
         * consumed, using <i>that map's own</i> key equality.
         *
         * <p>The left-hand pass decides "does map2 have this key?" by calling {@code map2.get}/
         * {@code map2.containsKey}, i.e. through map2's key equality. The right-hand pass must skip exactly
         * the entries that pass consumed. Probing the result maps instead ({@code common.containsKey(key2)})
         * is wrong, because they are {@code HashMap}/{@code LinkedHashMap} keyed by {@code equals}/
         * {@code hashCode}: whenever map2's key equality is <i>coarser</i> than {@code equals} the two
         * disagree and the same entry is filed twice - once in {@code common} (or {@code differentValues})
         * and again in {@code onlyOnRight}. A plain natural-ordering {@code TreeMap<BigDecimal>} is enough to
         * trigger it, since {@code 2.0} and {@code 2.00} compare equal but are not {@code equals}.
         *
         * <p>The tracker therefore mirrors map2: a {@code SortedMap}'s comparator (a {@code null} comparator
         * means natural ordering, which {@code TreeSet} accepts) and identity for an {@code IdentityHashMap}.
         * Only keys that map2 has already accepted are added, so its comparator is never handed a key it
         * rejected.
         *
         * <p>Every other conforming {@link Map} defines {@code containsKey} through {@code equals}/
         * {@code hashCode} - the same relation the result maps use - so for those this returns {@code null}
         * and the callers fall back to probing {@code common}/{@code differentValues} directly. That keeps
         * the overwhelmingly common case allocation-free: measured on two 200k-entry {@code HashMap}s, always
         * allocating a tracker raised the per-call allocation from 11.6 MB to 18.2 MB.
         *
         * <p>A map that hides a coarser relation behind a class this method cannot recognise gets no tracker:
         * {@code Collections.unmodifiableMap(someTreeMap)} and {@code ImmutableMap.wrap(someTreeMap)} both
         * forward {@code containsKey} to the {@code TreeMap} while being neither a {@code SortedMap} nor a
         * type whose backing map can be reached portably. Those are not repaired here; they are caught after
         * the fact by {@link #checkConsumedRightEntries(int, int, Map)}, and the fix for a caller is to use
         * the sorted view ({@link Collections#unmodifiableSortedMap(SortedMap)},
         * {@link ImmutableSortedMap#wrap(SortedMap)}), which this method does recognise.
         *
         * @param map2 the right-hand map whose key equality is to be mirrored
         * @return an empty set that answers membership the way {@code map2} does, or {@code null} when
         *         {@code map2} uses {@code equals}/{@code hashCode} and no separate tracking is needed
         */
        @SuppressWarnings("unchecked")
        private static Set<Object> newConsumedKeyTracker(final Map<?, ?> map2) {
            if (map2 instanceof SortedMap) {
                return new TreeSet<>((Comparator<Object>) ((SortedMap<?, ?>) map2).comparator());
            } else if (map2 instanceof IdentityHashMap) {
                return Collections.newSetFromMap(new IdentityHashMap<>());
            }

            return null;
        }

        /**
         * Verifies that the right-hand pass skipped exactly the entries the left-hand pass consumed.
         *
         * <p>The two passes speak different languages: the left pass resolves each key of {@code map1}
         * through {@code map2}'s own key equality, while the right pass decides "already consumed?" through
         * {@code equals}/{@code hashCode} (or through the tracker from
         * {@link #newConsumedKeyTracker(Map)} when one could be built). Whenever those two relations
         * disagree the result is self-contradictory rather than merely surprising:</p>
         * <ul>
         *   <li>if {@code map2}'s equality is <i>coarser</i> than {@code equals} and no tracker was
         *       available, an entry is filed both as common (or differing) <i>and</i> as right-only, so a
         *       consumer applying the difference applies the same change twice;</li>
         *   <li>if {@code map1}'s equality is <i>finer</i> than {@code map2}'s, several left entries resolve
         *       to one right entry, so {@code areEqual()} can report {@code true} for maps of different
         *       sizes.</li>
         * </ul>
         *
         * <p>Both cases are caught by one identity: every left key that {@code map2} resolved consumes
         * exactly one entry of {@code map2}, so the number of resolutions must equal the number of entries
         * the right pass skipped. Counting resolutions and skips - rather than comparing against
         * {@code map1.size()}/{@code map2.size()} - keeps this correct when only a subset of the keys is
         * being compared, because both counters then range over the selected entries alone.</p>
         *
         * <p>The check is deliberately <i>conservative</i> in one corner: when the right-hand map hides a
         * coarser relation and the key selection happens to exclude the spelling that map stores, the
         * result would have come out right by accident, yet the counts still disagree and the call is
         * rejected. That input is broken in general - the same map pair compared without a selection really
         * does double-file - and the remedy is the same, so failing is preferred over returning an answer
         * that only luck made correct.</p>
         *
         * @param resolvedLeftCount the number of compared {@code map1} entries whose key {@code map2} resolved
         * @param consumedRightCount the number of compared {@code map2} entries the right-hand pass skipped as
         *        already consumed
         * @param map2 the right-hand map, named in the exception message
         * @throws IllegalArgumentException if the two counts disagree
         */
        private static void checkConsumedRightEntries(final int resolvedLeftCount, final int consumedRightCount, final Map<?, ?> map2)
                throws IllegalArgumentException {
            if (resolvedLeftCount != consumedRightCount) {
                throw new IllegalArgumentException(
                        "Cannot compute a consistent difference: " + resolvedLeftCount + " entry/entries of the first map were matched in the second map (a "
                                + map2.getClass().getName() + "), but " + consumedRightCount + " entry/entries of the second map account for them."
                                + " The two maps disagree on which keys are \"the same key\", so no partition into common/left-only/right-only"
                                + " exists - some entry would have to be reported twice, or one entry would have to answer for several."
                                + " Compare maps that share one key equality: use keys compared by equals/hashCode, or, if the right-hand map"
                                + " sorts or identity-compares its keys, pass it as a SortedMap/IdentityHashMap rather than behind a wrapper"
                                + " that hides that (for example Collections.unmodifiableSortedMap instead of Collections.unmodifiableMap).");
            }
        }

        /**
         * Verifies that every compared entry of one input is still represented in the results.
         *
         * <p>{@link #checkConsumedRightEntries(int, int, Map)} catches an input map whose key equality is
         * <i>coarser</i> than {@code equals}. This catches the opposite: an input whose key equality is
         * <i>finer</i>, so that two of its keys are distinct to it but {@code equals}-equal to each other -
         * an {@code IdentityHashMap} holding two equal-but-distinct keys, or a {@code SortedMap} whose
         * comparator splits keys that {@code equals} joins. The result maps are keyed by
         * {@code equals}/{@code hashCode}, so the second such entry silently overwrites the first and the
         * difference reports fewer entries than the input holds. The consumed-entry identity cannot see it,
         * because nothing was consumed - both entries were headed for the same bucket.</p>
         *
         * @param considered the number of entries of that input the comparison looked at
         * @param buckets the result containers those entries were distributed into
         * @param which {@code "first"} or {@code "second"}, for the message
         * @param source the input map, named in the message
         * @throws IllegalArgumentException if the buckets hold fewer entries than were considered
         */
        private static void checkNoEntriesCollapsed(final int considered, final String which, final Map<?, ?> source, final Map<?, ?>... buckets)
                throws IllegalArgumentException {
            int held = 0;

            for (final Map<?, ?> bucket : buckets) {
                held += bucket.size();
            }

            if (held < considered) {
                throw new IllegalArgumentException("Cannot compute a consistent difference: " + considered + " compared entry/entries of the " + which
                        + " map (a " + source.getClass().getName() + ") collapsed into " + held
                        + " result entry/entries. Its key equality is finer than equals/hashCode - it holds keys that are distinct to it"
                        + " but equal to each other - and the result maps are keyed by equals/hashCode, so reporting them would lose one."
                        + " Compare maps whose keys are distinct under equals/hashCode.");
            }
        }

        /**
         * Compares two maps and identifies the differences between them using default equality comparison for values.
         * <p>
         * This method creates a {@code MapDifference} object that contains:
         * <ul>
         *   <li>Common entries: Key-value pairs present in both maps with equal values</li>
         *   <li>Left-only entries: Key-value pairs present only in the first map</li>
         *   <li>Right-only entries: Key-value pairs present only in the second map</li>
         *   <li>Different values: Keys present in both maps but with different values</li>
         * </ul>
         *
         * <p>Values are compared with {@link N#deepEquals(Object, Object)}, so array-valued entries are matched
         * by content (including nested arrays) rather than by reference; use
         * {@link #ofByValues(Map, Map, BiPredicate)} to supply a different value equivalence. Null values are
         * handled correctly - if both maps have a {@code null} value for the same key, it's considered a common entry.
         *
         * <p>Which keys count as "the same key" is decided by {@code map2}: every key of {@code map1} is
         * looked up in {@code map2}, so a {@code SortedMap} whose comparator is inconsistent with
         * {@code equals} matches by that comparator. A plain {@code TreeMap<BigDecimal>}, for instance,
         * treats {@code 2.0} and {@code 2.00} as one key. The result maps are hash-based and keyed by
         * {@code equals}/{@code hashCode}, so they report whichever key object {@code map1} (or, for
         * {@code onlyOnRight}, {@code map2}) actually stores.
         *
         * <p><b>Both maps must agree on what "the same key" means, and a {@code SortedMap}/
         * {@code IdentityHashMap} must expose that through its type.</b> When they do not, no consistent
         * partition exists - an entry would have to be reported both as common (or differing) and as
         * right-only, or one entry of {@code map2} would have to answer for several entries of
         * {@code map1} - and this method throws {@link IllegalArgumentException} rather than return a
         * self-contradictory result. Three shapes trigger it:</p>
         * <ul>
         *   <li>a right-hand map whose key equality is <i>coarser</i> than {@code equals} but whose type
         *       does not reveal it - for example {@code Collections.unmodifiableMap(aTreeMap)} or
         *       {@code ImmutableMap.wrap(aTreeMap)}; use {@link Collections#unmodifiableSortedMap(SortedMap)}
         *       or {@link ImmutableSortedMap#wrap(SortedMap)} instead;</li>
         *   <li>a left-hand map whose key equality is <i>finer</i> than the right-hand map's - for example a
         *       {@code HashMap} holding both {@code 2.0} and {@code 2.00} compared against a
         *       {@code TreeMap<BigDecimal>}, where two left entries would answer to one right entry;</li>
         *   <li>either map holding two keys that are distinct to <i>it</i> but {@code equals}-equal to each
         *       other <i>when both would be filed into the same result map</i> - an {@code IdentityHashMap}
         *       holding two equal-but-distinct keys compared against an empty or unrelated map, say. The result
         *       maps are keyed by {@code equals}/{@code hashCode}, so one of the two would silently overwrite the
         *       other and the difference would report fewer entries than the input holds. This one is rejected
         *       even when the other map is empty. When the two keys are filed into <i>different</i> result maps -
         *       {@code map2}'s {@code kA} matching a key of {@code map1} while its {@code equals}-equal {@code kB}
         *       does not - nothing is lost, so the call succeeds and both entries are reported; because the result
         *       maps are keyed by {@code equals}/{@code hashCode}, the same key then appears in two of them.</li>
         * </ul>
         *
         * <p>The order of entries in the result maps depends on the input map types:
         * <ul>
         *   <li>If either input map is a {@link LinkedHashMap} or {@link SortedMap}, results use {@link LinkedHashMap}</li>
         *   <li>Otherwise, results use {@link HashMap}</li>
         * </ul>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, Integer> map1 = Map.of("a", 1, "b", 2, "c", 3);
         * Map<String, Integer> map2 = Map.of("b", 2, "c", 4, "d", 5);
         * MapDifference<?, ?, ?> diff = MapDifference.of(map1, map2);
         * // Results in:
         * // common: {"b": 2}
         * // onlyOnLeft: {"a": 1}
         * // onlyOnRight: {"d": 5}
         * // differentValues: {"c": Pair.of(3, 4)}
         * }</pre>
         *
         * <p><b>Note - untyped {@code null} arguments.</b> This class inherits the collection and array
         * factories declared on {@link Difference}, so a call written with bare {@code null} literals -
         * {@code MapDifference.of(null, null)} - is ambiguous between them and does not compile. Give the
         * arguments a type ({@code MapDifference.of((Map<K, V>) null, (Map<K, V>) null)}) or pass typed
         * variables; both maps may be {@code null} at run time.
         *
         * @param <CK> the common key type shared by both maps
         * @param <K1> the key type of the first map (must extend CK)
         * @param <V1> the value type of the first map
         * @param <K2> the key type of the second map (must extend CK)
         * @param <V2> the value type of the second map
         * @param map1 the first map to compare. Can be {@code null} or empty.
         * @param map2 the second map to compare. Can be {@code null} or empty. Every key of {@code map1} is
         *        looked up in this map, so this map must tolerate them: if it rejects {@code null} keys (for
         *        example {@code Map.of(...)} or a natural-ordering {@code TreeMap}) and {@code map1} has a
         *        {@code null} key, the lookup throws {@link NullPointerException}. {@code map1} is never
         *        queried in return.
         * @return a {@code MapDifference} object containing the comparison results
         * @throws IllegalArgumentException if the two maps do not agree on which keys are "the same key"
         *         - see the key-equality note above.
         * @see Maps#difference(Map, Map)
         * @see Maps#symmetricDifference(Map, Map)
         * @see N#difference(Collection, Collection)
         * @see N#symmetricDifference(Collection, Collection)
         * @see N#excludeAll(Collection, Collection)
         * @see N#excludeAllToSet(Collection, Collection)
         * @see N#removeAll(Collection, Iterable)
         * @see N#intersection(Collection, Collection)
         * @see N#commonSet(Collection, Collection)
         */
        public static <CK, K1 extends CK, V1, K2 extends CK, V2> MapDifference<Map<K1, V1>, Map<K2, V2>, Map<CK, Pair<V1, V2>>> of(
                final Map<? extends K1, ? extends V1> map1, final Map<? extends K2, ? extends V2> map2) throws IllegalArgumentException {
            return ofByEntries(map1, map2, null, (k, v1, v2) -> N.deepEquals(v1, v2));
        }

        /**
         * Compares two maps and identifies the differences between them, considering only specified keys.
         * <p>
         * This method creates a {@code MapDifference} object that contains:
         * <ul>
         *   <li>Common entries: Key-value pairs for the specified keys that exist in both maps with equal values</li>
         *   <li>Left-only entries: Key-value pairs for the specified keys that exist only in the first map</li>
         *   <li>Right-only entries: Key-value pairs for the specified keys that exist only in the second map</li>
         *   <li>Different values: Specified keys that exist in both maps but with different values</li>
         * </ul>
         *
         * <p>Only the keys present in the {@code keysToCompare} collection are considered for comparison.
         * Keys not in this collection are ignored, even if they exist in either or both maps.
         *
         * <p>If {@code keysToCompare} is {@code null} or empty, all keys from both maps are compared (equivalent
         * to calling {@link #of(Map, Map)}).
         *
         * <p>Values are compared with {@link N#deepEquals(Object, Object)}, so array-valued entries are matched
         * by content (including nested arrays) rather than by reference.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, Integer> map1 = Map.of("a", 1, "b", 2, "c", 3, "d", 4);
         * Map<String, Integer> map2 = Map.of("a", 1, "b", 5, "c", 3, "e", 6);
         * Collection<String> keys = Arrays.asList("a", "b", "e");
         * MapDifference<?, ?, ?> diff = MapDifference.of(map1, map2, keys);
         * // Results in:
         * // common: {"a": 1}
         * // onlyOnLeft: {} (empty, since the compared keys "a" and "b" both exist in map2)
         * // onlyOnRight: {"e": 6}
         * // differentValues: {"b": Pair.of(2, 5)}
         * // Note: "c" and "d" are not compared because they're not in keysToCompare
         * }</pre>
         *
         * @param <CK> the common key type shared by both maps
         * @param <K1> the key type of the first map (must extend CK)
         * @param <V1> the value type of the first map
         * @param <K2> the key type of the second map (must extend CK)
         * @param <V2> the value type of the second map
         * @param map1 the first map to compare. Can be {@code null} or empty.
         * @param map2 the second map to compare. Can be {@code null} or empty. Every key of {@code map1} is
         *        looked up in this map, so this map must tolerate them: if it rejects {@code null} keys (for
         *        example {@code Map.of(...)} or a natural-ordering {@code TreeMap}) and {@code map1} has a
         *        {@code null} key, the lookup throws {@link NullPointerException}. {@code map1} is never
         *        queried in return.
         * @param keysToCompare the keys to compare between the two maps. If {@code null} or empty, all keys will be compared.
         *        If it is not already a {@link Set}, it is copied into a
         *        {@code HashSet} for the lookups, so its elements must have consistent {@code equals}/{@code hashCode}.
         *        A {@code Set} is used as given; if that {@code Set} rejects {@code null} (for example
         *        {@code Set.of(...)} or a natural-ordering {@code TreeSet}) and a compared map has a {@code null}
         *        key, the lookup throws {@link NullPointerException}.
         * @return a {@code MapDifference} object containing the comparison results for the specified keys
         * @throws IllegalArgumentException if the two maps do not agree on which keys are "the same key"
         *         - see {@link #of(Map, Map)}.
         * @see Maps#difference(Map, Map)
         * @see Maps#symmetricDifference(Map, Map)
         * @see N#difference(Collection, Collection)
         * @see N#symmetricDifference(Collection, Collection)
         * @see N#excludeAll(Collection, Collection)
         * @see N#excludeAllToSet(Collection, Collection)
         * @see N#removeAll(Collection, Iterable)
         * @see N#intersection(Collection, Collection)
         * @see N#commonSet(Collection, Collection)
         */
        public static <CK, K1 extends CK, V1, K2 extends CK, V2> MapDifference<Map<K1, V1>, Map<K2, V2>, Map<CK, Pair<V1, V2>>> of(
                final Map<? extends K1, ? extends V1> map1, final Map<? extends K2, ? extends V2> map2, final Collection<CK> keysToCompare)
                throws IllegalArgumentException {
            return ofByEntries(map1, map2, keysToCompare, (k, v1, v2) -> N.deepEquals(v1, v2));
        }

        /**
         * Compares two maps using a custom value equivalence predicate to determine if values are equal.
         *
         * <p>The name is not {@code of} because three-argument {@code of} overloads taking a key/property
         * selection and taking a value equivalence would be mutually ambiguous for a {@code null}
         * argument, which the selection overload documents as "compare everything".</p>
         * <p>
         * This method creates a {@code MapDifference} object that contains:
         * <ul>
         *   <li>Common entries: Key-value pairs present in both maps with equivalent values (according to the predicate)</li>
         *   <li>Left-only entries: Key-value pairs present only in the first map</li>
         *   <li>Right-only entries: Key-value pairs present only in the second map</li>
         *   <li>Different values: Keys present in both maps but with non-equivalent values</li>
         * </ul>
         *
         * <p>The {@code valueEquivalence} predicate allows for custom comparison logic. This is useful when:
         * <ul>
         *   <li>Values need fuzzy matching (e.g., floating-point comparison with tolerance)</li>
         *   <li>Only certain fields of complex objects should be compared</li>
         *   <li>Case-insensitive string comparison is needed</li>
         * </ul>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, Double> map1 = Map.of("a", 1.0, "b", 2.001);
         * Map<String, Double> map2 = Map.of("a", 1.0, "b", 2.0);
         *
         * // Compare with tolerance of 0.01
         * BiPredicate<Double, Double> approxEqual = (v1, v2) -> Math.abs(v1 - v2) < 0.01;
         * MapDifference<?, ?, ?> diff = MapDifference.ofByValues(map1, map2, approxEqual);
         * // Results in:
         * // common: {"a": 1.0, "b": 2.001} (both are considered equal)
         * // onlyOnLeft: {}
         * // onlyOnRight: {}
         * // differentValues: {}
         * }</pre>
         *
         * @param <CK> the common key type shared by both maps
         * @param <K1> the key type of the first map (must extend CK)
         * @param <V1> the value type of the first map
         * @param <K2> the key type of the second map (must extend CK)
         * @param <V2> the value type of the second map
         * @param map1 the first map to compare. Can be {@code null} or empty.
         * @param map2 the second map to compare. Can be {@code null} or empty. Every key of {@code map1} is
         *        looked up in this map, so this map must tolerate them: if it rejects {@code null} keys (for
         *        example {@code Map.of(...)} or a natural-ordering {@code TreeMap}) and {@code map1} has a
         *        {@code null} key, the lookup throws {@link NullPointerException}. {@code map1} is never
         *        queried in return.
         * @param valueEquivalence the predicate to determine if two values are equivalent.
         * @return a {@code MapDifference} object containing the comparison results
         * @throws IllegalArgumentException if {@code valueEquivalence} is {@code null}, or if the two maps do
         *         not agree on which keys are "the same key" - see {@link #of(Map, Map)}.
         * @see Maps#difference(Map, Map)
         * @see Maps#symmetricDifference(Map, Map)
         * @see N#difference(Collection, Collection)
         * @see N#symmetricDifference(Collection, Collection)
         * @see N#excludeAll(Collection, Collection)
         * @see N#excludeAllToSet(Collection, Collection)
         * @see N#removeAll(Collection, Iterable)
         * @see N#intersection(Collection, Collection)
         * @see N#commonSet(Collection, Collection)
         */
        public static <CK, K1 extends CK, V1, K2 extends CK, V2> MapDifference<Map<K1, V1>, Map<K2, V2>, Map<CK, Pair<V1, V2>>> ofByValues(
                final Map<? extends K1, ? extends V1> map1, final Map<? extends K2, ? extends V2> map2,
                final BiPredicate<? super V1, ? super V2> valueEquivalence) throws IllegalArgumentException {
            N.checkArgNotNull(valueEquivalence, cs.valueEquivalence);

            return ofByEntries(map1, map2, null, (k, v1, v2) -> valueEquivalence.test(v1, v2));
        }

        /**
         * Compares two maps using a custom value equivalence predicate that also considers the key.
         *
         * <p>The name is not {@code of} because three-argument {@code of} overloads taking a key/property
         * selection and taking a value equivalence would be mutually ambiguous for a {@code null}
         * argument, which the selection overload documents as "compare everything".</p>
         * <p>
         * This method creates a {@code MapDifference} object that contains:
         * <ul>
         *   <li>Common entries: Key-value pairs present in both maps with equivalent values (according to the predicate)</li>
         *   <li>Left-only entries: Key-value pairs present only in the first map</li>
         *   <li>Right-only entries: Key-value pairs present only in the second map</li>
         *   <li>Different values: Keys present in both maps but with non-equivalent values</li>
         * </ul>
         *
         * <p>The {@code valueEquivalence} TriPredicate receives three parameters:
         * <ol>
         *   <li>The key from the first map</li>
         *   <li>The value from the first map</li>
         *   <li>The value from the second map</li>
         * </ol>
         *
         * <p>This allows for key-dependent comparison logic. For example:
         * <ul>
         *   <li>Different tolerance levels for different numeric keys</li>
         *   <li>Case-sensitive comparison for some keys, case-insensitive for others</li>
         *   <li>Ignoring certain fields based on the key</li>
         * </ul>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, Double> prices1 = Map.of("apple", 1.99, "gold", 1850.50);
         * Map<String, Double> prices2 = Map.of("apple", 2.15, "gold", 1851.00);
         *
         * // Different tolerance for different items
         * TriPredicate<String, Double, Double> priceEqual = (key, v1, v2) -> {
         *     double tolerance = key.equals("gold") ? 5.0 : 0.1;
         *     return Math.abs(v1 - v2) <= tolerance;
         * };
         *
         * MapDifference<?, ?, ?> diff = MapDifference.ofByEntries(prices1, prices2, priceEqual);
         * // Results in:
         * // common: {"gold": 1850.50} (within $5 tolerance)
         * // onlyOnLeft: {}
         * // onlyOnRight: {}
         * // differentValues: {"apple": Pair.of(1.99, 2.15)} (exceeds $0.10 tolerance)
         * }</pre>
         *
         * @param <CK> the common key type shared by both maps
         * @param <K1> the key type of the first map (must extend CK)
         * @param <V1> the value type of the first map
         * @param <K2> the key type of the second map (must extend CK)
         * @param <V2> the value type of the second map
         * @param map1 the first map to compare. Can be {@code null} or empty.
         * @param map2 the second map to compare. Can be {@code null} or empty. Every key of {@code map1} is
         *        looked up in this map, so this map must tolerate them: if it rejects {@code null} keys (for
         *        example {@code Map.of(...)} or a natural-ordering {@code TreeMap}) and {@code map1} has a
         *        {@code null} key, the lookup throws {@link NullPointerException}. {@code map1} is never
         *        queried in return.
         * @param valueEquivalence the predicate to determine if two values are equivalent for a given key.
         * @return a {@code MapDifference} object containing the comparison results
         * @throws IllegalArgumentException if {@code valueEquivalence} is {@code null}, or if the two maps do
         *         not agree on which keys are "the same key" - see {@link #of(Map, Map)}.
         * @see Maps#difference(Map, Map)
         * @see Maps#symmetricDifference(Map, Map)
         * @see N#difference(Collection, Collection)
         * @see N#symmetricDifference(Collection, Collection)
         * @see N#excludeAll(Collection, Collection)
         * @see N#excludeAllToSet(Collection, Collection)
         * @see N#removeAll(Collection, Iterable)
         * @see N#intersection(Collection, Collection)
         * @see N#commonSet(Collection, Collection)
         */
        public static <CK, K1 extends CK, V1, K2 extends CK, V2> MapDifference<Map<K1, V1>, Map<K2, V2>, Map<CK, Pair<V1, V2>>> ofByEntries(
                final Map<? extends K1, ? extends V1> map1, final Map<? extends K2, ? extends V2> map2,
                final TriPredicate<? super K1, ? super V1, ? super V2> valueEquivalence) throws IllegalArgumentException {
            N.checkArgNotNull(valueEquivalence, cs.valueEquivalence);

            return ofByEntries(map1, map2, null, valueEquivalence);
        }

        /**
         * Compares two maps for specified keys using a custom value equivalence predicate.
         *
         * <p>The name is not {@code of} because three-argument {@code of} overloads taking a key/property
         * selection and taking a value equivalence would be mutually ambiguous for a {@code null}
         * argument, which the selection overload documents as "compare everything".</p>
         * <p>
         * This method combines the functionality of key filtering and custom value comparison.
         * It creates a {@code MapDifference} object that contains:
         * <ul>
         *   <li>Common entries: Key-value pairs for specified keys with equivalent values</li>
         *   <li>Left-only entries: Key-value pairs for specified keys that exist only in the first map</li>
         *   <li>Right-only entries: Key-value pairs for specified keys that exist only in the second map</li>
         *   <li>Different values: Specified keys that exist in both maps but with non-equivalent values</li>
         * </ul>
         *
         * <p>The comparison process:
         * <ol>
         *   <li>Only keys in {@code keysToCompare} are considered (if {@code null}, all keys are compared)</li>
         *   <li>For each key, values are compared using the {@code valueEquivalence} predicate</li>
         *   <li>The predicate receives the key and both values, allowing key-dependent comparison</li>
         * </ol>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, Object> config1 = Map.of(
         *     "timeout", 30,
         *     "retries", 3,
         *     "debug", true,
         *     "url", "http://api.com"
         * );
         * Map<String, Object> config2 = Map.of(
         *     "timeout", 30.0,
         *     "retries", 5,
         *     "debug", true,
         *     "proxy", "proxy.com"
         * );
         *
         * Collection<String> keysToCheck = Arrays.asList("timeout", "retries", "debug");
         *
         * // Custom equivalence that handles type differences
         * TriPredicate<String, Object, Object> configEqual = (key, v1, v2) -> {
         *     if (key.equals("timeout")) {
         *         return ((Number) v1).doubleValue() == ((Number) v2).doubleValue();
         *     }
         *     return Objects.equals(v1, v2);
         * };
         *
         * MapDifference<?, ?, ?> diff = MapDifference.ofByEntries(config1, config2, keysToCheck, configEqual);
         * // Results in:
         * // common: {"timeout": 30, "debug": true}
         * // onlyOnLeft: {}
         * // onlyOnRight: {}
         * // differentValues: {"retries": Pair.of(3, 5)}
         * // Note: "url" and "proxy" are not compared
         * }</pre>
         *
         * @param <CK> the common key type shared by both maps
         * @param <K1> the key type of the first map (must extend CK)
         * @param <V1> the value type of the first map
         * @param <K2> the key type of the second map (must extend CK)
         * @param <V2> the value type of the second map
         * @param map1 the first map to compare. Can be {@code null} or empty.
         * @param map2 the second map to compare. Can be {@code null} or empty. Every key of {@code map1} is
         *        looked up in this map, so this map must tolerate them: if it rejects {@code null} keys (for
         *        example {@code Map.of(...)} or a natural-ordering {@code TreeMap}) and {@code map1} has a
         *        {@code null} key, the lookup throws {@link NullPointerException}. {@code map1} is never
         *        queried in return.
         * @param keysToCompare the keys to compare. If {@code null} or empty, all keys will be compared.
         *        If it is not already a {@link Set}, it is copied into a
         *        {@code HashSet} for the lookups, so its elements must have consistent {@code equals}/{@code hashCode}.
         *        A {@code Set} is used as given; if that {@code Set} rejects {@code null} (for example
         *        {@code Set.of(...)} or a natural-ordering {@code TreeSet}) and a compared map has a {@code null}
         *        key, the lookup throws {@link NullPointerException}.
         * @param valueEquivalence the predicate to determine if values are equivalent.
         * @return a {@code MapDifference} object containing the comparison results
         * @throws IllegalArgumentException if {@code valueEquivalence} is {@code null}, or if the two maps do
         *         not agree on which keys are "the same key" - see {@link #of(Map, Map)}.
         * @see Maps#difference(Map, Map)
         * @see Maps#symmetricDifference(Map, Map)
         * @see N#difference(Collection, Collection)
         * @see N#symmetricDifference(Collection, Collection)
         * @see N#excludeAll(Collection, Collection)
         * @see N#excludeAllToSet(Collection, Collection)
         * @see N#removeAll(Collection, Iterable)
         * @see N#intersection(Collection, Collection)
         * @see N#commonSet(Collection, Collection)
         */
        @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE")
        @SuppressWarnings("unlikely-arg-type")
        public static <CK, K1 extends CK, V1, K2 extends CK, V2> MapDifference<Map<K1, V1>, Map<K2, V2>, Map<CK, Pair<V1, V2>>> ofByEntries(
                final Map<? extends K1, ? extends V1> map1, final Map<? extends K2, ? extends V2> map2, final Collection<CK> keysToCompare,
                final TriPredicate<? super K1, ? super V1, ? super V2> valueEquivalence) throws IllegalArgumentException {
            N.checkArgNotNull(valueEquivalence, cs.valueEquivalence);

            final boolean isOrderedMap = (map1 instanceof LinkedHashMap || map1 instanceof SortedMap)
                    || (map2 instanceof LinkedHashMap || map2 instanceof SortedMap);

            final Map<K1, V1> common = isOrderedMap ? new LinkedHashMap<>() : new HashMap<>();
            final Map<K1, V1> onlyOnLeft = isOrderedMap ? new LinkedHashMap<>() : new HashMap<>();
            final Map<K2, V2> onlyOnRight = isOrderedMap ? new LinkedHashMap<>() : new HashMap<>();
            final Map<CK, Pair<V1, V2>> differentValues = isOrderedMap ? new LinkedHashMap<>() : new HashMap<>();

            // The selection is probed once per entry of both maps. A List-valued selection would make that
            // a linear scan and the whole comparison O(n * m), so copy anything that is not already a Set
            // into a HashSet. An existing Set is reused as-is (including a SortedSet, whose contains() is
            // O(log m) and whose comparator may define a different key equality than equals/hashCode).
            final Collection<CK> keySelection = N.isEmpty(keysToCompare) || keysToCompare instanceof Set ? keysToCompare : new HashSet<>(keysToCompare);

            if (N.isEmpty(keySelection)) {
                if (N.isEmpty(map1)) {
                    if (N.isEmpty(map2)) {
                        // Do nothing. All empty.
                    } else {
                        onlyOnRight.putAll(map2);
                        checkNoEntriesCollapsed(map2.size(), "second", map2, onlyOnRight);
                    }
                } else if (N.isEmpty(map2)) {
                    onlyOnLeft.putAll(map1);
                    checkNoEntriesCollapsed(map1.size(), "first", map1, onlyOnLeft);
                } else {
                    final Set<Object> consumedRightKeys = newConsumedKeyTracker(map2);
                    int resolvedLeftCount = 0;
                    int consumedRightCount = 0;
                    K1 key1 = null;
                    V1 val1 = null;
                    K2 key2 = null;
                    V2 val2 = null;

                    for (final Entry<K1, V1> entry1 : ((Map<K1, V1>) map1).entrySet()) {
                        key1 = entry1.getKey();
                        val1 = entry1.getValue();
                        //noinspection SuspiciousMethodCalls
                        val2 = map2.get(key1);

                        //noinspection SuspiciousMethodCalls
                        if (val2 == null && !map2.containsKey(key1)) {
                            onlyOnLeft.put(key1, val1);
                        } else {
                            // map2 resolved this key, so one of its entries is consumed here whichever bucket
                            // the value comparison below picks.
                            resolvedLeftCount++;

                            if (consumedRightKeys != null) {
                                consumedRightKeys.add(key1);
                            }

                            if (valueEquivalence.test(key1, val1, val2)) {
                                common.put(key1, val1);
                            } else {
                                differentValues.put(key1, Pair.of(val1, val2));
                            }
                        }
                    }

                    for (final Entry<K2, V2> entry2 : ((Map<K2, V2>) map2).entrySet()) {
                        key2 = entry2.getKey();

                        //noinspection SuspiciousMethodCalls
                        if (consumedRightKeys == null ? common.containsKey(key2) || differentValues.containsKey(key2) : consumedRightKeys.contains(key2)) {
                            consumedRightCount++;
                            continue;
                        }

                        onlyOnRight.put(key2, entry2.getValue());
                    }

                    checkConsumedRightEntries(resolvedLeftCount, consumedRightCount, map2);
                    checkNoEntriesCollapsed(map1.size(), "first", map1, common, onlyOnLeft, differentValues);
                    checkNoEntriesCollapsed(map2.size(), "second", map2, common, onlyOnRight, differentValues);
                }

            } else {
                if (N.isEmpty(map1)) {
                    if (N.isEmpty(map2)) {
                        // Do nothing. All empty.
                    } else {
                        int selectedRightCount = 0;

                        for (final Entry<? extends K2, ? extends V2> entry : map2.entrySet()) {
                            if (keySelection.contains(entry.getKey())) {
                                selectedRightCount++;
                                onlyOnRight.put(entry.getKey(), entry.getValue());
                            }
                        }

                        checkNoEntriesCollapsed(selectedRightCount, "second", map2, onlyOnRight);
                    }
                } else if (N.isEmpty(map2)) {
                    int selectedLeftCount = 0;

                    for (final Entry<? extends K1, ? extends V1> entry : map1.entrySet()) {
                        if (keySelection.contains(entry.getKey())) {
                            selectedLeftCount++;
                            onlyOnLeft.put(entry.getKey(), entry.getValue());
                        }
                    }

                    checkNoEntriesCollapsed(selectedLeftCount, "first", map1, onlyOnLeft);
                } else {
                    final Set<Object> consumedRightKeys = newConsumedKeyTracker(map2);
                    int resolvedLeftCount = 0;
                    int consumedRightCount = 0;
                    int selectedLeftCount = 0;
                    int selectedRightCount = 0;
                    K1 key1 = null;
                    V1 val1 = null;
                    K2 key2 = null;
                    V2 val2 = null;

                    for (final Entry<K1, V1> entry1 : ((Map<K1, V1>) map1).entrySet()) {
                        key1 = entry1.getKey();

                        if (!keySelection.contains(key1)) {
                            continue;
                        }

                        selectedLeftCount++;
                        val1 = entry1.getValue();
                        //noinspection SuspiciousMethodCalls
                        val2 = map2.get(key1);

                        //noinspection SuspiciousMethodCalls
                        if (val2 == null && !map2.containsKey(key1)) {
                            onlyOnLeft.put(key1, val1);
                        } else {
                            // map2 resolved this key, so one of its entries is consumed here whichever bucket
                            // the value comparison below picks.
                            resolvedLeftCount++;

                            if (consumedRightKeys != null) {
                                consumedRightKeys.add(key1);
                            }

                            if (valueEquivalence.test(key1, val1, val2)) {
                                common.put(key1, val1);
                            } else {
                                differentValues.put(key1, Pair.of(val1, val2));
                            }
                        }
                    }

                    for (final Entry<K2, V2> entry2 : ((Map<K2, V2>) map2).entrySet()) {
                        key2 = entry2.getKey();

                        // "Already consumed?" is asked before "selected?" so that the count stays exact: a
                        // tracker mirroring map2 can recognise an entry the equals-based key selection would
                        // not, and that entry is consumed either way.
                        //noinspection SuspiciousMethodCalls
                        if (consumedRightKeys == null ? common.containsKey(key2) || differentValues.containsKey(key2) : consumedRightKeys.contains(key2)) {
                            consumedRightCount++;
                            selectedRightCount++;
                            continue;
                        }

                        //noinspection SuspiciousMethodCalls
                        if (!keySelection.contains(key2)) {
                            continue;
                        }

                        selectedRightCount++;
                        onlyOnRight.put(key2, entry2.getValue());
                    }

                    // Both counters range over the selected entries only, so the identity holds unchanged
                    // when a key selection is in force.
                    checkConsumedRightEntries(resolvedLeftCount, consumedRightCount, map2);
                    checkNoEntriesCollapsed(selectedLeftCount, "first", map1, common, onlyOnLeft, differentValues);
                    checkNoEntriesCollapsed(selectedRightCount, "second", map2, common, onlyOnRight, differentValues);
                }

            }
            return new MapDifference<>(common, onlyOnLeft, onlyOnRight, differentValues);
        }

        /**
         * Compares two collections of maps with the same key and value types, identifying common maps,
         * maps unique to each collection, and maps with different values.
         * <p>
         * This method is designed for comparing collections of maps where each map represents an entity
         * or record. Maps are matched between collections using the provided {@code idExtractor} function,
         * which extracts a unique identifier from each map.
         *
         * <p>The comparison results include:
         * <ul>
         *   <li>Common: Maps that exist in both collections with identical key-value pairs</li>
         *   <li>Left-only: Maps that exist only in the first collection</li>
         *   <li>Right-only: Maps that exist only in the second collection</li>
         *   <li>Different values: Maps that exist in both collections (same ID) but have different values</li>
         * </ul>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Map<String, Object>> users1 = Arrays.asList(
         *     Map.of("id", 1, "name", "John", "age", 30),
         *     Map.of("id", 2, "name", "Jane", "age", 25)
         * );
         * List<Map<String, Object>> users2 = Arrays.asList(
         *     Map.of("id", 1, "name", "John", "age", 31),
         *     Map.of("id", 3, "name", "Bob", "age", 28)
         * );
         *
         * MapDifference<?, ?, ?> diff = MapDifference.of(users1, users2, map -> map.get("id"));
         * // Results in:
         * // common: [] (empty, no identical maps)
         * // onlyOnLeft: [{"id": 2, "name": "Jane", "age": 25}]
         * // onlyOnRight: [{"id": 3, "name": "Bob", "age": 28}]
         * // differentValues: {1: MapDifference of the two "John" maps showing age difference}
         * }</pre>
         *
         * @param <CK> the common key type of the maps
         * @param <CV> the common value type of the maps
         * @param <K> the type of the identifier extracted from each map
         * @param a the first collection of maps to compare. Can be {@code null} or empty.
         * @param b the second collection of maps to compare. Can be {@code null} or empty.
         * @param idExtractor Function to extract a unique identifier from each map.
         * @return a {@code MapDifference} object containing the comparison results
         * @throws IllegalArgumentException if {@code idExtractor} is {@code null}, or if a matched pair of maps
         *         does not agree on which keys are "the same key" - see {@link #of(Map, Map)}.
         * @throws IllegalStateException if duplicate IDs are found within a single collection
         */
        public static <CK, CV, K> MapDifference<List<Map<CK, CV>>, List<Map<CK, CV>>, Map<K, MapDifference<Map<CK, CV>, Map<CK, CV>, Map<CK, Pair<CV, CV>>>>> of(
                final Collection<? extends Map<CK, CV>> a, final Collection<? extends Map<CK, CV>> b,
                final Function<? super Map<CK, CV>, ? extends K> idExtractor) throws IllegalArgumentException, IllegalStateException {
            N.checkArgNotNull(idExtractor, cs.idExtractor);

            return of(a, b, null, idExtractor, idExtractor);
        }

        /**
         * Compares two collections of maps with the same key and value types, considering only specified keys
         * when comparing individual maps.
         * <p>
         * This method extends the functionality of {@link #of(Collection, Collection, Function)} by allowing
         * you to specify which keys should be compared when determining if two maps with the same ID have
         * different values.
         *
         * <p>The comparison process:
         * <ol>
         *   <li>Maps are matched between collections using the {@code idExtractor}</li>
         *   <li>When comparing matched maps, only the keys in {@code keysToCompare} are considered</li>
         *   <li>Keys not in {@code keysToCompare} are ignored during value comparison</li>
         * </ol>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Map<String, Object>> users1 = Arrays.asList(
         *     Map.of("id", 1, "name", "John", "age", 30, "lastLogin", "2024-01-01"),
         *     Map.of("id", 2, "name", "Jane", "age", 25, "lastLogin", "2024-01-02")
         * );
         * List<Map<String, Object>> users2 = Arrays.asList(
         *     Map.of("id", 1, "name", "John", "age", 30, "lastLogin", "2024-02-01"),
         *     Map.of("id", 2, "name", "Jane", "age", 26, "lastLogin", "2024-02-02")
         * );
         *
         * // Only compare name and age, ignore lastLogin
         * Collection<String> keysToCompare = Arrays.asList("name", "age");
         * MapDifference<?, ?, ?> diff = MapDifference.of(users1, users2, keysToCompare,
         *                                            map -> map.get("id"));
         * // Results in:
         * // common: [{"id": 1, ...}] (John's record, since name and age match)
         * // onlyOnLeft: []
         * // onlyOnRight: []
         * // differentValues: {2: MapDifference showing age difference for Jane}
         * }</pre>
         *
         * @param <CK> the common key type of the maps
         * @param <CV> the common value type of the maps
         * @param <K> the type of the identifier extracted from each map
         * @param a the first collection of maps to compare. Can be {@code null} or empty.
         * @param b the second collection of maps to compare. Can be {@code null} or empty.
         * @param keysToCompare the keys to compare within each map. If {@code null} or empty, all keys are compared.
         * @param idExtractor Function to extract a unique identifier from each map.
         * @return a {@code MapDifference} object containing the comparison results
         * @throws IllegalArgumentException if {@code idExtractor} is {@code null}, or if a matched pair of maps
         *         does not agree on which keys are "the same key" - see {@link #of(Map, Map)}.
         * @throws IllegalStateException if duplicate IDs are found within a single collection
         */
        public static <CK, CV, K> MapDifference<List<Map<CK, CV>>, List<Map<CK, CV>>, Map<K, MapDifference<Map<CK, CV>, Map<CK, CV>, Map<CK, Pair<CV, CV>>>>> of(
                final Collection<? extends Map<CK, CV>> a, final Collection<? extends Map<CK, CV>> b, final Collection<CK> keysToCompare,
                final Function<? super Map<CK, CV>, ? extends K> idExtractor) throws IllegalArgumentException, IllegalStateException {
            N.checkArgNotNull(idExtractor, cs.idExtractor);

            return of(a, b, keysToCompare, idExtractor, idExtractor);
        }

        /**
         * Compares two collections of maps with potentially different key and value types, using separate
         * ID extractors for each collection.
         * <p>
         * This method is useful when comparing collections of maps that represent similar entities but
         * may have different structures. For example, comparing API responses from different versions
         * or comparing data from different sources.
         *
         * <p>The comparison matches maps between collections based on the IDs extracted by the respective
         * ID extractors. Maps with the same ID are compared to identify differences in their key-value pairs.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Collection A: Old API format
         * List<Map<String, Object>> oldFormat = Arrays.asList(
         *     Map.of("user_id", 1, "full_name", "John Doe"),
         *     Map.of("user_id", 2, "full_name", "Jane Smith")
         * );
         *
         * // Collection B: New API format
         * List<Map<String, Object>> newFormat = Arrays.asList(
         *     Map.of("id", 1, "firstName", "John", "lastName", "Doe"),
         *     Map.of("id", 3, "firstName", "Bob", "lastName", "Johnson")
         * );
         *
         * MapDifference<?, ?, ?> diff = MapDifference.of(
         *     oldFormat, newFormat,
         *     map -> map.get("user_id"),
         *     map -> map.get("id")
         * );
         * // Results in:
         * // common: [] (empty, different structures)
         * // onlyOnLeft: [{"user_id": 2, "full_name": "Jane Smith"}]
         * // onlyOnRight: [{"id": 3, "firstName": "Bob", "lastName": "Johnson"}]
         * // differentValues: {1: MapDifference showing structural differences}
         * }</pre>
         *
         * @param <CK> the common key type shared by maps in both collections
         * @param <K1> the key type of maps in the first collection (must extend CK)
         * @param <V1> the value type of maps in the first collection
         * @param <K2> the key type of maps in the second collection (must extend CK)
         * @param <V2> the value type of maps in the second collection
         * @param <K> the type of the identifier used to match maps between collections
         * @param a the first collection of maps to compare. Can be {@code null} or empty.
         * @param b the second collection of maps to compare. Can be {@code null} or empty.
         * @param idExtractor1 Function to extract IDs from maps in the first collection.
         * @param idExtractor2 Function to extract IDs from maps in the second collection.
         * @return a {@code MapDifference} object containing the comparison results
         * @throws IllegalArgumentException if any of {@code idExtractor1}, {@code idExtractor2} is {@code null}, or if
         *         a matched pair of maps does not agree on which keys are "the same key" - see {@link #of(Map, Map)}.
         * @throws IllegalStateException if duplicate IDs are found within a single collection
         */
        public static <CK, K1 extends CK, V1, K2 extends CK, V2, K> MapDifference<List<Map<K1, V1>>, List<Map<K2, V2>>, Map<K, MapDifference<Map<K1, V1>, Map<K2, V2>, Map<CK, Pair<V1, V2>>>>> of(
                final Collection<? extends Map<K1, V1>> a, final Collection<? extends Map<K2, V2>> b,
                final Function<? super Map<K1, V1>, ? extends K> idExtractor1, final Function<? super Map<K2, V2>, ? extends K> idExtractor2)
                throws IllegalArgumentException, IllegalStateException {
            N.checkArgNotNull(idExtractor1, cs.idExtractor1);
            N.checkArgNotNull(idExtractor2, cs.idExtractor2);

            return of(a, b, null, idExtractor1, idExtractor2);
        }

        /**
         * Compares two collections of maps with potentially different types, using separate ID extractors
         * and considering only specified keys during comparison.
         * <p>
         * This is the most flexible comparison method, combining:
         * <ul>
         *   <li>Support for different map structures via separate ID extractors</li>
         *   <li>Selective key comparison via {@code keysToCompare}</li>
         *   <li>Type flexibility with different key/value types for each collection</li>
         * </ul>
         *
         * <p>The comparison process:
         * <ol>
         *   <li>Maps from collection A are matched with maps from collection B using their extracted IDs</li>
         *   <li>For matched maps, only the keys specified in {@code keysToCompare} are compared</li>
         *   <li>Maps are categorized as common (identical values for compared keys), left-only,
         *       right-only, or having different values</li>
         * </ol>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Collection A: Version 1 API
         * List<Map<String, Object>> v1Data = Arrays.asList(
         *     Map.of("userId", 1, "username", "john_doe", "email", "john@old.com",
         *            "createdAt", "2023-01-01"),
         *     Map.of("userId", 2, "username", "jane_smith", "email", "jane@old.com",
         *            "createdAt", "2023-02-01")
         * );
         *
         * // Collection B: Version 2 API
         * List<Map<String, Object>> v2Data = Arrays.asList(
         *     Map.of("id", 1, "username", "john_doe", "emailAddress", "john@new.com",
         *            "created", "2023-01-01T00:00:00Z"),
         *     Map.of("id", 3, "username", "bob_jones", "emailAddress", "bob@new.com",
         *            "created", "2023-03-01T00:00:00Z")
         * );
         *
         * // Only compare the shared "username" field
         * Collection<String> keysToCompare = Arrays.asList("username");
         *
         * MapDifference<?, ?, ?> diff = MapDifference.of(
         *     v1Data, v2Data, keysToCompare,
         *     map -> map.get("userId"),
         *     map -> map.get("id")
         * );
         * // Results in:
         * // common: Maps with ID 1 (john_doe) - username matches
         * // onlyOnLeft: Maps with ID 2 (jane_smith)
         * // onlyOnRight: Maps with ID 3 (bob_jones)
         * // differentValues: {} (empty, since we're only comparing username)
         * }</pre>
         *
         * @param <CK> the common key type shared by maps in both collections
         * @param <K1> the key type of maps in the first collection (must extend CK)
         * @param <V1> the value type of maps in the first collection
         * @param <K2> the key type of maps in the second collection (must extend CK)
         * @param <V2> the value type of maps in the second collection
         * @param <K> the type of the identifier used to match maps between collections
         * @param a the first collection of maps to compare. Can be {@code null} or empty.
         * @param b the second collection of maps to compare. Can be {@code null} or empty.
         * @param keysToCompare Keys to compare within matched maps. If {@code null} or empty, all keys are compared.
         * @param idExtractor1 Function to extract IDs from maps in the first collection.
         * @param idExtractor2 Function to extract IDs from maps in the second collection.
         * @return a {@code MapDifference} object containing detailed comparison results
         * @throws IllegalArgumentException if any of {@code idExtractor1}, {@code idExtractor2} is {@code null}, or if
         *         a matched pair of maps does not agree on which keys are "the same key" - see {@link #of(Map, Map)}.
         * @throws IllegalStateException if duplicate IDs are found within a single collection
         */
        public static <CK, K1 extends CK, V1, K2 extends CK, V2, K> MapDifference<List<Map<K1, V1>>, List<Map<K2, V2>>, Map<K, MapDifference<Map<K1, V1>, Map<K2, V2>, Map<CK, Pair<V1, V2>>>>> of(
                final Collection<? extends Map<K1, V1>> a, final Collection<? extends Map<K2, V2>> b, final Collection<CK> keysToCompare,
                final Function<? super Map<K1, V1>, ? extends K> idExtractor1, final Function<? super Map<K2, V2>, ? extends K> idExtractor2)
                throws IllegalArgumentException, IllegalStateException {
            N.checkArgNotNull(idExtractor1, cs.idExtractor1);
            N.checkArgNotNull(idExtractor2, cs.idExtractor2);

            final List<Map<K1, V1>> common = new ArrayList<>();
            final List<Map<K1, V1>> onlyOnLeft = new ArrayList<>();
            final List<Map<K2, V2>> onlyOnRight = new ArrayList<>();
            final Map<K, MapDifference<Map<K1, V1>, Map<K2, V2>, Map<CK, Pair<V1, V2>>>> differentValues = new LinkedHashMap<>();

            if (N.isEmpty(a)) {
                if (N.isEmpty(b)) {
                    // Do nothing. All empty.
                } else {
                    // The id check belongs on this path too. It used to run only in the both-sides-non-empty
                    // branch below, so a collection carrying duplicate ids was accepted in silence while the
                    // other side was empty and started throwing the documented IllegalStateException the moment
                    // one element was added to it.
                    checkNoDuplicateIds(b, idExtractor2);

                    onlyOnRight.addAll(b);
                }
            } else if (N.isEmpty(b)) {
                checkNoDuplicateIds(a, idExtractor1);

                onlyOnLeft.addAll(a);
            } else {
                final Map<K, Map<? extends K1, ? extends V1>> beanMapA = N.toMap(a, idExtractor1, Fn.identity(), Fn.throwingMerger(),
                        IntFunctions.ofLinkedHashMap());
                final Map<K, Map<? extends K2, ? extends V2>> beanMapB = N.toMap(b, idExtractor2, Fn.identity(), Fn.throwingMerger(),
                        IntFunctions.ofLinkedHashMap());

                Map<K1, V1> mapA = null;
                Map<K2, V2> mapB = null;

                for (final Map.Entry<K, Map<? extends K1, ? extends V1>> entry : beanMapA.entrySet()) {
                    mapA = (Map<K1, V1>) entry.getValue();

                    if (beanMapB.containsKey(entry.getKey())) {
                        mapB = (Map<K2, V2>) beanMapB.get(entry.getKey());
                        // Compute the per-map difference once and decide equality from it, so this check cannot
                        // disagree with the value equality used to build that difference. A raw N.equals/
                        // N.equalsByKeys pre-check compares array values by reference and would file an
                        // equal-content pair under differentValues with an empty difference.
                        final MapDifference<Map<K1, V1>, Map<K2, V2>, Map<CK, Pair<V1, V2>>> mapDiff = MapDifference.of(mapA, mapB, keysToCompare);

                        if (mapDiff.areEqual()) {
                            common.add(mapA);
                        } else {
                            differentValues.put(entry.getKey(), mapDiff);
                        }
                    } else {
                        onlyOnLeft.add(mapA);
                    }
                }

                for (final Map.Entry<K, Map<? extends K2, ? extends V2>> entry : beanMapB.entrySet()) {
                    if (!beanMapA.containsKey(entry.getKey())) {
                        onlyOnRight.add((Map<K2, V2>) entry.getValue());
                    }
                }
            }

            return new MapDifference<>(common, onlyOnLeft, onlyOnRight, differentValues);
        }
    }

    /**
     * Represents the difference between two Java beans, providing detailed comparison results including
     * common properties, properties unique to each bean, and properties with different values.
     * <p>
     * This class extends {@link KeyValueDifference} to provide bean-specific comparison functionality.
     * It uses reflection to compare bean properties and supports various comparison options including
     * custom value equivalence predicates and selective property comparison.
     *
     * <p>The comparison results include:
     * <ul>
     *   <li>Common properties: Properties that exist in both beans with equal values</li>
     *   <li>Left-only properties: Properties that exist only in the first bean</li>
     *   <li>Right-only properties: Properties that exist only in the second bean</li>
     *   <li>Different values: Properties that exist in both beans but with different values</li>
     * </ul>
     *
     * <p>Special handling:
     * <ul>
     *   <li>Properties annotated with {@code @DiffIgnore} are excluded from comparison</li>
     *   <li>When comparing all properties, a property whose value is {@code null} on both sides is omitted
     *       from every result map, provided the value-equivalence predicate accepts the two as equivalent</li>
     *   <li>When comparing specific properties, {@code null} values are included in the comparison</li>
     *   <li>When one bean is {@code null}, every property of the other bean is reported on its own side,
     *       {@code null}-valued properties included</li>
     *   <li>The value-equivalence predicate is invoked for every property present on both sides, including
     *       when both values are {@code null}; predicate arguments may therefore be {@code null}</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * record Person(String name, int age, String email) {}
     *
     * Person person1 = new Person("John", 30, "john@old.com");
     * Person person2 = new Person("John", 31, "john@new.com");
     *
     * BeanDifference<?, ?, ?> diff = BeanDifference.of(person1, person2);
     * // diff.common() returns {"name": "John"}
     * // diff.onlyOnLeft() returns {} (empty)
     * // diff.onlyOnRight() returns {} (empty)
     * // diff.differentValues() returns {"age": Pair.of(30, 31), "email": Pair.of("john@old.com", "john@new.com")}
     * }</pre>
     *
     * @param <L> the type of the map representing properties from the left (first) bean
     * @param <R> the type of the map representing properties from the right (second) bean
     * @param <D> the type of the map containing properties with different values
     * @see KeyValueDifference
     * @see com.landawn.abacus.annotation.DiffIgnore
     * @see Maps#difference(Map, Map)
     * @see Maps#symmetricDifference(Map, Map)
     * @see N#difference(Collection, Collection)
     */
    public static final class BeanDifference<L, R, D> extends KeyValueDifference<L, R, D> {
        /**
         * Creates a {@code BeanDifference} from four already-computed result containers.
         * The containers are stored by reference; they are neither copied nor wrapped.
         *
         * @param common the properties found in both beans with equivalent values
         * @param onlyOnLeft the properties found only in the first bean
         * @param onlyOnRight the properties found only in the second bean
         * @param differentValues the properties present in both beans but whose values are not equivalent
         */
        BeanDifference(final L common, final L onlyOnLeft, final R onlyOnRight, final D differentValues) {
            super(common, onlyOnLeft, onlyOnRight, differentValues);
        }

        /**
         * Compares two beans and identifies the differences between them using default equality comparison.
         * <p>
         * This method creates a {@code BeanDifference} object that contains:
         * <ul>
         *   <li>Common properties: Properties present in both beans with equal values, compared with
         *       {@link N#deepEquals(Object, Object)} so array-valued properties are matched by content
         *       (including nested arrays) rather than by reference</li>
         *   <li>Left-only properties: Properties present only in the first bean</li>
         *   <li>Right-only properties: Properties present only in the second bean</li>
         *   <li>Different values: Properties present in both beans but with different values</li>
         * </ul>
         *
         * <p>Special behavior:
         * <ul>
         *   <li>Properties annotated with {@code @DiffIgnore} on <i>either</i> bean class are excluded from comparison</li>
         *   <li>Properties where both beans have {@code null} values are not included in the results</li>
         *   <li>The beans can be of different classes; only properties with matching names are compared</li>
         *   <li>If exactly one bean is {@code null}, every property of the other is reported on its own
         *       side, {@code null}-valued properties included</li>
         * </ul>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * record User(String name, String email, @DiffIgnore Date lastModified) {}
         *
         * User user1 = new User("John", "john@old.com", new Date(1));
         * User user2 = new User("John", "john@new.com", new Date(2));
         *
         * BeanDifference<?, ?, ?> diff = BeanDifference.of(user1, user2);
         * // Results in:
         * // common: {"name": "John"}
         * // onlyOnLeft: {}
         * // onlyOnRight: {}
         * // differentValues: {"email": Pair.of("john@old.com", "john@new.com")}
         * // Note: lastModified is ignored due to @DiffIgnore
         * }</pre>
         *
         * <p><b>Note - untyped {@code null} arguments.</b> This class inherits the collection and array
         * factories declared on {@link Difference}, so a call written with bare {@code null} literals -
         * {@code BeanDifference.of(null, null)} - is ambiguous between them and does not compile. Cast the
         * arguments ({@code BeanDifference.of((Object) null, (Object) null)}) or pass typed variables; both
         * beans may be {@code null} at run time.
         *
         * @param bean1 the first bean to compare. May be {@code null}, in which case every property of
         *        {@code bean2} that is not {@code @DiffIgnore}-annotated is reported as right-only, including
         *        properties whose value is {@code null}.
         * @param bean2 the second bean to compare. May be {@code null}, in which case every property of
         *        {@code bean1} that is not {@code @DiffIgnore}-annotated is reported as left-only, including
         *        properties whose value is {@code null}.
         * @return a {@code BeanDifference} object containing the comparison results
         * @throws IllegalArgumentException if a non-{@code null} bean argument is not a valid bean class (e.g., a
         *         primitive wrapper, array, {@link Collection}, or {@link Map}).
         * @throws RuntimeException if bean metadata cannot be resolved or a selected property cannot be read; a getter or reflection failure is propagated as an unchecked exception
         * @see MapDifference#of(Map, Map)
         * @see BeanDifference#of(Object, Object, Collection)
         * @see Maps#difference(Map, Map)
         * @see Maps#symmetricDifference(Map, Map)
         * @see N#difference(Collection, Collection)
         * @see N#symmetricDifference(Collection, Collection)
         * @see N#excludeAll(Collection, Collection)
         * @see N#excludeAllToSet(Collection, Collection)
         * @see N#removeAll(Collection, Iterable)
         * @see N#intersection(Collection, Collection)
         * @see N#commonSet(Collection, Collection)
         * @see com.landawn.abacus.annotation.DiffIgnore
         */
        public static BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> of(final Object bean1, final Object bean2)
                throws IllegalArgumentException, RuntimeException {
            return ofByProps(bean1, bean2, null, (k, v1, v2) -> N.deepEquals(v1, v2));
        }

        /**
         * Compares two beans considering only the specified properties.
         * <p>
         * This method creates a {@code BeanDifference} object that contains comparison results
         * only for the properties specified in {@code propNamesToCompare}. Properties not in this
         * collection are completely ignored, even if they exist in one or both beans.
         *
         * <p>Key differences from {@link #of(Object, Object)}:
         * <ul>
         *   <li>Only properties in {@code propNamesToCompare} are examined</li>
         *   <li>Properties with {@code null} values in both beans ARE included in the common properties</li>
         *   <li>{@code @DiffIgnore} annotations are ignored when specific properties are requested</li>
         *   <li>If a specified property exists in the first bean but not the second, it is reported as a
         *       left-only property; if it exists in the second bean but not the first, it is reported as a
         *       right-only property; if it exists in neither bean, it is omitted entirely</li>
         * </ul>
         *
         * <p>Property values are compared with {@link N#deepEquals(Object, Object)}, so array-valued properties
         * are matched by content (including nested arrays) rather than by reference.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * record Employee(String id, String name, Double salary, String department) {}
         *
         * Employee emp1 = new Employee("E001", "John", 50000.0, "Sales");
         * Employee emp2 = new Employee("E001", "John", 55000.0, "Marketing");
         *
         * // Compare only id and name
         * Collection<String> propsToCompare = Arrays.asList("id", "name");
         * BeanDifference<?, ?, ?> diff = BeanDifference.of(emp1, emp2, propsToCompare);
         * // Results in:
         * // common: {"id": "E001", "name": "John"}
         * // onlyOnLeft: {}
         * // onlyOnRight: {}
         * // differentValues: {}
         * // Note: salary and department differences are ignored
         * }</pre>
         *
         * @param bean1 the first bean to compare. May be {@code null}, in which case the requested properties of
         *        {@code bean2} are reported as right-only, including those whose value is {@code null}.
         * @param bean2 the second bean to compare. May be {@code null}, in which case the requested properties of
         *        {@code bean1} are reported as left-only, including those whose value is {@code null}.
         * @param propNamesToCompare the property names to compare. If {@code null} or empty, all properties are compared.
         * @return a {@code BeanDifference} object containing the comparison results for the specified properties
         * @throws IllegalArgumentException if a non-{@code null} bean argument is not a valid bean class.
         * @throws RuntimeException if bean metadata cannot be resolved or a selected property cannot be read; a getter or reflection failure is propagated as an unchecked exception
         * @see MapDifference#of(Map, Map)
         * @see BeanDifference#of(Object, Object)
         * @see Maps#difference(Map, Map)
         * @see Maps#symmetricDifference(Map, Map)
         * @see N#difference(Collection, Collection)
         * @see N#symmetricDifference(Collection, Collection)
         * @see N#excludeAll(Collection, Collection)
         * @see N#excludeAllToSet(Collection, Collection)
         * @see N#removeAll(Collection, Iterable)
         * @see N#intersection(Collection, Collection)
         * @see N#commonSet(Collection, Collection)
         * @see com.landawn.abacus.annotation.DiffIgnore
         */
        public static BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> of(final Object bean1, final Object bean2,
                final Collection<String> propNamesToCompare) throws IllegalArgumentException, RuntimeException {
            return ofByProps(bean1, bean2, propNamesToCompare, (k, v1, v2) -> N.deepEquals(v1, v2));
        }

        /**
         * Compares two beans using a custom value equivalence predicate.
         *
         * <p>The name is not {@code of} because three-argument {@code of} overloads taking a key/property
         * selection and taking a value equivalence would be mutually ambiguous for a {@code null}
         * argument, which the selection overload documents as "compare everything".</p>
         * <p>
         * This method allows for custom comparison logic when determining if property values
         * are equal. The predicate receives both property values and should return {@code true}
         * if they are considered equivalent.
         *
         * <p>Use cases for custom equivalence:
         * <ul>
         *   <li>Comparing floating-point numbers with tolerance</li>
         *   <li>Case-insensitive string comparison</li>
         *   <li>Comparing only certain fields of nested objects</li>
         *   <li>Ignoring whitespace or formatting differences</li>
         * </ul>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * record Product(String name, Double price, String description) {}
         *
         * Product p1 = new Product("Widget", 10.99, "A useful widget");
         * Product p2 = new Product("WIDGET", 10.991, "A USEFUL WIDGET");
         *
         * // Case-insensitive comparison with price tolerance
         * BiPredicate<Object, Object> fuzzyEquals = (v1, v2) -> {
         *     if (v1 instanceof String && v2 instanceof String) {
         *         return ((String) v1).equalsIgnoreCase((String) v2);
         *     } else if (v1 instanceof Double && v2 instanceof Double) {
         *         return Math.abs((Double) v1 - (Double) v2) < 0.01;
         *     }
         *     return Objects.equals(v1, v2);
         * };
         *
         * BeanDifference<?, ?, ?> diff = BeanDifference.ofByValues(p1, p2, fuzzyEquals);
         * // Results in:
         * // common: {"name": "Widget", "price": 10.99, "description": "A useful widget"}
         * // All properties are considered equal due to custom comparison
         * }</pre>
         *
         * @param bean1 the first bean to compare. May be {@code null}.
         * @param bean2 the second bean to compare. May be {@code null}.
         * @param valueEquivalence the predicate to determine if two property values are equivalent. It is invoked
         *        for every property present on both beans, so either argument may be {@code null}.
         * @return a {@code BeanDifference} object containing the comparison results
         * @throws IllegalArgumentException if a non-{@code null} bean argument is not a valid bean class, or if
         *         {@code valueEquivalence} is {@code null}.
         * @throws RuntimeException if bean metadata cannot be resolved, a selected property cannot be read, or the supplied identifier extractor or comparison callback throws during processing.
         * @see MapDifference#of(Map, Map)
         * @see BeanDifference#of(Object, Object, Collection)
         * @see Maps#difference(Map, Map)
         * @see Maps#symmetricDifference(Map, Map)
         * @see N#difference(Collection, Collection)
         * @see N#symmetricDifference(Collection, Collection)
         * @see N#excludeAll(Collection, Collection)
         * @see N#excludeAllToSet(Collection, Collection)
         * @see N#removeAll(Collection, Iterable)
         * @see N#intersection(Collection, Collection)
         * @see N#commonSet(Collection, Collection)
         * @see com.landawn.abacus.annotation.DiffIgnore
         */
        public static BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> ofByValues(final Object bean1,
                final Object bean2, final BiPredicate<Object, Object> valueEquivalence) throws IllegalArgumentException, RuntimeException {
            N.checkArgNotNull(valueEquivalence, cs.valueEquivalence);

            return ofByProps(bean1, bean2, null, (k, v1, v2) -> valueEquivalence.test(v1, v2));
        }

        /**
         * Compares two beans using a property-aware custom value equivalence predicate.
         *
         * <p>The name is not {@code of} because three-argument {@code of} overloads taking a key/property
         * selection and taking a value equivalence would be mutually ambiguous for a {@code null}
         * argument, which the selection overload documents as "compare everything".</p>
         * <p>
         * This method provides the most flexible bean comparison, allowing the equivalence logic
         * to vary based on the property name. The TriPredicate receives the property name and
         * both values, enabling property-specific comparison rules.
         *
         * <p>Use cases:
         * <ul>
         *   <li>Different comparison rules for different property types</li>
         *   <li>Ignoring certain properties dynamically</li>
         *   <li>Property-specific tolerance levels or formatting rules</li>
         *   <li>Complex business logic that depends on the property context</li>
         * </ul>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * record Account(String accountId, Double balance, Date lastActivity, String status) {}
         *
         * Account acc1 = new Account("ACC001", 1000.001, new Date(0), "ACTIVE");
         * Account acc2 = new Account("ACC001", 1000.0, new Date(30 * 60 * 1000), "active");
         *
         * // Property-specific comparison rules
         * TriPredicate<String, Object, Object> smartEquals = (propName, v1, v2) -> {
         *     switch (propName) {
         *         case "balance":
         *             // Allow small rounding differences
         *             return Math.abs((Double) v1 - (Double) v2) < 0.01;
         *         case "lastActivity":
         *             // Ignore time differences less than 1 hour
         *             return Math.abs(((Date) v1).getTime() - ((Date) v2).getTime()) < 3600000;
         *         case "status":
         *             // Case-insensitive comparison
         *             return ((String) v1).equalsIgnoreCase((String) v2);
         *         default:
         *             return Objects.equals(v1, v2);
         *     }
         * };
         *
         * BeanDifference<?, ?, ?> diff = BeanDifference.ofByProps(acc1, acc2, smartEquals);
         * // Results may show all properties as common if they meet the criteria
         * }</pre>
         *
         * @param bean1 the first bean to compare. May be {@code null}.
         * @param bean2 the second bean to compare. May be {@code null}.
         * @param valueEquivalence the predicate to determine if values are equivalent for a given property;
         *                         receives {@code (propertyName, value1, value2)}. It is invoked for every property
         *                         present on both beans, so either value may be {@code null}.
         * @return a {@code BeanDifference} object containing the comparison results
         * @throws IllegalArgumentException if a non-{@code null} bean argument is not a valid bean class, or if
         *         {@code valueEquivalence} is {@code null}.
         * @throws RuntimeException if bean metadata cannot be resolved, a selected property cannot be read, or the supplied identifier extractor or comparison callback throws during processing.
         * @see MapDifference#of(Map, Map)
         * @see BeanDifference#of(Object, Object, Collection)
         * @see Maps#difference(Map, Map)
         * @see Maps#symmetricDifference(Map, Map)
         * @see N#difference(Collection, Collection)
         * @see N#symmetricDifference(Collection, Collection)
         * @see N#excludeAll(Collection, Collection)
         * @see N#excludeAllToSet(Collection, Collection)
         * @see N#removeAll(Collection, Iterable)
         * @see N#intersection(Collection, Collection)
         * @see N#commonSet(Collection, Collection)
         * @see com.landawn.abacus.annotation.DiffIgnore
         */
        public static BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> ofByProps(final Object bean1,
                final Object bean2, final TriPredicate<String, Object, Object> valueEquivalence) throws IllegalArgumentException, RuntimeException {
            N.checkArgNotNull(valueEquivalence, cs.valueEquivalence);

            return ofByProps(bean1, bean2, null, valueEquivalence);
        }

        /**
         * Compares two beans for specified properties using a property-aware custom value equivalence predicate.
         *
         * <p>The name is not {@code of} because three-argument {@code of} overloads taking a key/property
         * selection and taking a value equivalence would be mutually ambiguous for a {@code null}
         * argument, which the selection overload documents as "compare everything".</p>
         * <p>
         * This method combines selective property comparison with custom equivalence logic that can
         * vary by property name. It provides maximum control over the comparison process.
         *
         * <p>Behavior:
         * <ul>
         *   <li>Only properties in {@code propNamesToCompare} are examined</li>
         *   <li>The {@code valueEquivalence} predicate determines equality for each property, and is consulted
         *       even when both values are {@code null}</li>
         *   <li>{@code null} values in both beans are included in comparison (unlike the default behavior)</li>
         *   <li>{@code @DiffIgnore} annotations are ignored when specific properties are requested</li>
         * </ul>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * record Customer(String id, String name, String email, Double creditLimit, String notes) {}
         *
         * Customer c1 = new Customer("C001", "John Doe", "john@old.com", 5000.0, "VIP customer");
         * Customer c2 = new Customer("C001", "JOHN DOE", "john@new.com", 5000.01, "VIP Customer");
         *
         * // Compare only specific fields with custom logic
         * Collection<String> fieldsToCheck = Arrays.asList("name", "email", "creditLimit");
         *
         * TriPredicate<String, Object, Object> fieldEquals = (field, v1, v2) -> {
         *     switch (field) {
         *         case "name":
         *             // Case-insensitive name comparison
         *             return ((String) v1).equalsIgnoreCase((String) v2);
         *         case "creditLimit":
         *             // Allow small differences in credit limit
         *             return Math.abs((Double) v1 - (Double) v2) < 0.1;
         *         default:
         *             return Objects.equals(v1, v2);
         *     }
         * };
         *
         * BeanDifference<?, ?, ?> diff = BeanDifference.ofByProps(c1, c2, fieldsToCheck, fieldEquals);
         * // Results in:
         * // common: {"name": "John Doe", "creditLimit": 5000.0}
         * // differentValues: {"email": Pair.of("john@old.com", "john@new.com")}
         * // Note: id and notes are not compared
         * }</pre>
         *
         * @param bean1 the first bean to compare. May be {@code null}.
         * @param bean2 the second bean to compare. May be {@code null}.
         * @param propNamesToCompare the property names to compare. If {@code null} or empty, all properties are compared.
         * @param valueEquivalence the predicate to determine if values are equivalent for a given property. It is
         *        invoked for every requested property present on both beans, so either value may be {@code null}.
         * @return a {@code BeanDifference} object containing the comparison results
         * @throws IllegalArgumentException if a non-{@code null} bean argument is not a valid bean class, or if
         *         {@code valueEquivalence} is {@code null}.
         * @throws RuntimeException if bean metadata cannot be resolved, a selected property cannot be read, or the supplied identifier extractor or comparison callback throws during processing.
         * @see MapDifference#of(Map, Map)
         * @see BeanDifference#of(Object, Object, Collection)
         * @see Maps#difference(Map, Map)
         * @see Maps#symmetricDifference(Map, Map)
         * @see N#difference(Collection, Collection)
         * @see N#symmetricDifference(Collection, Collection)
         * @see N#excludeAll(Collection, Collection)
         * @see N#excludeAllToSet(Collection, Collection)
         * @see N#removeAll(Collection, Iterable)
         * @see N#intersection(Collection, Collection)
         * @see N#commonSet(Collection, Collection)
         * @see com.landawn.abacus.annotation.DiffIgnore
         */
        @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE")
        public static BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> ofByProps(final Object bean1,
                final Object bean2, final Collection<String> propNamesToCompare, final TriPredicate<String, Object, Object> valueEquivalence)
                throws IllegalArgumentException, RuntimeException {
            N.checkArgNotNull(valueEquivalence, cs.valueEquivalence);

            if (bean1 != null && !Beans.isBeanClass(bean1.getClass())) {
                throw new IllegalArgumentException(bean1.getClass().getCanonicalName() + " is not a bean class"); // NOSONAR
            }

            if (bean2 != null && !Beans.isBeanClass(bean2.getClass())) {
                throw new IllegalArgumentException(bean2.getClass().getCanonicalName() + " is not a bean class"); // NOSONAR
            }

            final Map<String, Object> common = new LinkedHashMap<>();
            final Map<String, Object> onlyOnLeft = new LinkedHashMap<>();
            final Map<String, Object> onlyOnRight = new LinkedHashMap<>();
            final Map<String, Pair<Object, Object>> differentValues = new LinkedHashMap<>();

            if (N.isEmpty(propNamesToCompare)) {
                if (bean1 == null) {
                    if (bean2 == null) {
                        // Do nothing. All empty.
                    } else {
                        // ignoreNullProperty must be false: a property that is present but null is still a
                        // property the other side does not have at all. Skipping it would drop it from the
                        // result entirely and let a bean whose properties are all null compare "equal" to null.
                        Beans.beanToMap(bean2, false, Beans.getIgnoredPropNamesForDiff(bean2.getClass()), onlyOnRight);
                    }
                } else if (bean2 == null) {
                    Beans.beanToMap(bean1, false, Beans.getIgnoredPropNamesForDiff(bean1.getClass()), onlyOnLeft);
                } else {
                    final Class<?> bean1Class = bean1.getClass();
                    final Class<?> bean2Class = bean2.getClass();
                    final Set<String> ignoredPropNamesForNullValues = new HashSet<>();
                    final ImmutableSet<String> diffIgnoredPropNamesForBean1 = Beans.getIgnoredPropNamesForDiff(bean1Class);
                    final ImmutableSet<String> diffIgnoredPropNamesForBean2 = Beans.getIgnoredPropNamesForDiff(bean2Class);
                    final BeanInfo beanInfo1 = ParserUtil.getBeanInfo(bean1Class);
                    final BeanInfo beanInfo2 = ParserUtil.getBeanInfo(bean2Class);
                    // BeanInfo.getPropInfo resolves a name FUZZILY (Beans.isPropName matches case-insensitively
                    // and across naming styles), so bean1's "userName" legitimately resolves to bean2's
                    // "username". Reconciling the second pass by exact name therefore filed one logical property
                    // in both `common` and `onlyOnRight`, and let a @DiffIgnore property spelled differently on
                    // the two classes leak into `onlyOnRight`. Track what the first pass actually consumed, by
                    // PropInfo identity - the same shape MapDifference uses for its consumed-key tracker.
                    final Set<PropInfo> consumedPropInfosOfBean2 = Collections.newSetFromMap(new IdentityHashMap<>());
                    Object val1 = null;
                    Object val2 = null;

                    {
                        PropInfo propInfo2 = null;

                        for (final PropInfo propInfo1 : beanInfo1.propInfoList) {
                            propInfo2 = beanInfo2.getPropInfo(propInfo1.name);
                            // A property annotated @DiffIgnore on EITHER bean class is excluded from the diff,
                            // including a fuzzy match whose canonical name on the right differs from the left.
                            if ((!diffIgnoredPropNamesForBean1.isEmpty() && diffIgnoredPropNamesForBean1.contains(propInfo1.name))
                                    || (!diffIgnoredPropNamesForBean2.isEmpty() && (diffIgnoredPropNamesForBean2.contains(propInfo1.name)
                                            || (propInfo2 != null && diffIgnoredPropNamesForBean2.contains(propInfo2.name))))) {
                                if (propInfo2 != null) {
                                    consumedPropInfosOfBean2.add(propInfo2);
                                }

                                continue;
                            }

                            val1 = propInfo1.getPropValue(bean1);

                            if (propInfo2 == null) {
                                onlyOnLeft.put(propInfo1.name, val1);
                            } else {
                                consumedPropInfosOfBean2.add(propInfo2);
                                val2 = propInfo2.getPropValue(bean2);

                                // The equivalence predicate decides first, even when both values are null: a
                                // caller-supplied predicate may well consider two nulls different, and skipping
                                // the call would silently override it. Only once the predicate has agreed the
                                // two sides match is a null/null property dropped from the results, which is the
                                // long-standing behaviour of this all-properties overload.
                                if (valueEquivalence.test(propInfo1.name, val1, val2)) {
                                    if (val1 == null && val2 == null) {
                                        ignoredPropNamesForNullValues.add(propInfo1.name);
                                    } else {
                                        common.put(propInfo1.name, val1);
                                    }
                                } else {
                                    differentValues.put(propInfo1.name, Pair.of(val1, val2));
                                }
                            }
                        }
                    }

                    for (final PropInfo propInfo : beanInfo2.propInfoList) {
                        if (consumedPropInfosOfBean2.contains(propInfo)
                                || (!diffIgnoredPropNamesForBean2.isEmpty() && diffIgnoredPropNamesForBean2.contains(propInfo.name))) {
                            continue;
                        }

                        if ((!diffIgnoredPropNamesForBean1.isEmpty() && diffIgnoredPropNamesForBean1.contains(propInfo.name))
                                || ignoredPropNamesForNullValues.contains(propInfo.name) || common.containsKey(propInfo.name)
                                || differentValues.containsKey(propInfo.name) || onlyOnLeft.containsKey(propInfo.name)) {
                            continue;
                        }

                        onlyOnRight.put(propInfo.name, propInfo.getPropValue(bean2));
                    }
                }

            } else {
                if (bean1 == null) {
                    if (bean2 == null) {
                        // Do nothing. All empty.
                    } else {
                        // Skip names not defined on the class, matching the two-bean path below
                        // (the strict Beans.beanToMap would throw IllegalArgumentException for them).
                        final BeanInfo beanInfo2 = ParserUtil.getBeanInfo(bean2.getClass());
                        PropInfo propInfo = null;

                        for (final String propName : propNamesToCompare) {
                            propInfo = beanInfo2.getPropInfo(propName);

                            if (propInfo != null) {
                                onlyOnRight.put(propName, propInfo.getPropValue(bean2));
                            }
                        }
                    }
                } else if (bean2 == null) {
                    // Skip names not defined on the class, matching the two-bean path below.
                    final BeanInfo beanInfo1 = ParserUtil.getBeanInfo(bean1.getClass());
                    PropInfo propInfo = null;

                    for (final String propName : propNamesToCompare) {
                        propInfo = beanInfo1.getPropInfo(propName);

                        if (propInfo != null) {
                            onlyOnLeft.put(propName, propInfo.getPropValue(bean1));
                        }
                    }
                } else {
                    final BeanInfo beanInfo1 = ParserUtil.getBeanInfo(bean1.getClass());
                    final BeanInfo beanInfo2 = ParserUtil.getBeanInfo(bean2.getClass());
                    PropInfo propInfo1 = null;
                    PropInfo propInfo2 = null;
                    Object val1 = null;
                    Object val2 = null;

                    for (String propName : propNamesToCompare) {
                        propInfo1 = beanInfo1.getPropInfo(propName);

                        if (propInfo1 == null) {
                            continue;
                        }

                        val1 = propInfo1.getPropValue(bean1);

                        propInfo2 = beanInfo2.getPropInfo(propName);

                        if (propInfo2 == null) {
                            onlyOnLeft.put(propName, val1);
                        } else {
                            val2 = propInfo2.getPropValue(bean2);

                            // Every explicitly requested property is compared, including one that is null on both
                            // sides; the predicate is consulted for that case too rather than being short-circuited.
                            if (valueEquivalence.test(propName, val1, val2)) {
                                common.put(propName, val1);
                            } else {
                                differentValues.put(propName, Pair.of(val1, val2));
                            }
                        }
                    }

                    for (String propName : propNamesToCompare) {
                        propInfo2 = beanInfo2.getPropInfo(propName);

                        if (propInfo2 == null || common.containsKey(propName) || differentValues.containsKey(propName)) {
                            continue;
                        }

                        onlyOnRight.put(propName, propInfo2.getPropValue(bean2));
                    }
                }

            }

            return new BeanDifference<>(common, onlyOnLeft, onlyOnRight, differentValues);
        }

        /**
         * Compares two collections of beans, identifying common beans, beans unique to each collection,
         * and beans that exist in both collections but have different property values.
         * <p>
         * This method uses the provided {@code idExtractor} function to identify matching beans between
         * the two collections. Beans with the same identifier are compared property by property to find
         * differences.
         *
         * <p>The comparison process:
         * <ol>
         *   <li>Each bean is identified using the {@code idExtractor} function</li>
         *   <li>Beans with matching identifiers are always compared property by property via
         *       {@link #of(Object, Object, Collection)}; that per-bean difference decides whether the pair is
         *       reported as common or listed under {@code differentValues}</li>
         *   <li>All properties are considered (except those marked with {@code @DiffIgnore})</li>
         *   <li>Results are categorized into common, left-only, right-only, and different beans</li>
         * </ol>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * record Employee(String id, String name, String department) {}
         *
         * List<Employee> team1 = Arrays.asList(
         *     new Employee("E001", "John", "Sales"),
         *     new Employee("E002", "Jane", "Marketing")
         * );
         * List<Employee> team2 = Arrays.asList(
         *     new Employee("E001", "John", "Engineering"),
         *     new Employee("E003", "Bob", "Sales")
         * );
         *
         * BeanDifference<?, ?, ?> diff = BeanDifference.of(team1, team2, Employee::id);
         * // Results in:
         * // common: [] (empty, as E001 has different department)
         * // onlyOnLeft: [Employee E002]
         * // onlyOnRight: [Employee E003]
         * // differentValues: {"E001": BeanDifference showing department change}
         * }</pre>
         *
         * @param <T> the type of beans in both collections
         * @param <K> the type of the identifier used to match beans between collections
         * @param a the first collection of beans to compare. Can be {@code null} or empty.
         * @param b the second collection of beans to compare. Can be {@code null} or empty.
         * @param idExtractor the function to extract a unique identifier from each bean.
         * @return a non-{@code null} {@code BeanDifference} object containing the comparison results
         * @throws IllegalArgumentException if a non-{@code null} element of either collection is not a valid bean
         *         instance, or if {@code idExtractor} is {@code null}.
         * @throws IllegalStateException if duplicate identifiers are found within a single collection
         * @throws RuntimeException if bean metadata cannot be resolved, a selected property cannot be read, or the supplied identifier extractor or comparison callback throws during processing.
         */
        public static <T, K> BeanDifference<List<T>, List<T>, Map<K, BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>>> of(
                final Collection<? extends T> a, final Collection<? extends T> b, final Function<? super T, K> idExtractor)
                throws IllegalArgumentException, IllegalStateException, RuntimeException {
            N.checkArgNotNull(idExtractor, cs.idExtractor);

            return of(a, b, null, idExtractor, idExtractor);
        }

        /**
         * Compares two collections of beans considering only specified properties.
         * <p>
         * This method combines collection-level comparison with selective property comparison.
         * Only the properties listed in {@code propNamesToCompare} are examined when comparing
         * matched beans.
         *
         * <p>Use cases:
         * <ul>
         *   <li>Comparing only business-critical properties while ignoring metadata</li>
         *   <li>Performance optimization by comparing only necessary fields</li>
         *   <li>Versioning scenarios where only certain fields should trigger differences</li>
         * </ul>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * record Product(String sku, String name, Double price, Date lastModified, String internalNotes) {}
         *
         * List<Product> catalog1 = Arrays.asList(
         *     new Product("SKU001", "Widget", 19.99, new Date(1), "Check supplier"),
         *     new Product("SKU002", "Gadget", 29.99, new Date(2), "Bestseller")
         * );
         * List<Product> catalog2 = Arrays.asList(
         *     new Product("SKU001", "Widget", 21.99, new Date(3), "New supplier"),
         *     new Product("SKU003", "Tool", 39.99, new Date(4), "New item")
         * );
         *
         * // Compare only name and price, ignoring dates and notes
         * Collection<String> propsToCompare = Arrays.asList("name", "price");
         * BeanDifference<?, ?, ?> diff = BeanDifference.of(
         *     catalog1, catalog2, propsToCompare,
         *     Product::sku
         * );
         * // Results show price difference for SKU001, ignoring other field changes
         * }</pre>
         *
         * @param <T> the type of beans in both collections
         * @param <K> the type of the identifier used to match beans between collections
         * @param a the first collection of beans to compare. Can be {@code null} or empty.
         * @param b the second collection of beans to compare. Can be {@code null} or empty.
         * @param propNamesToCompare the property names to compare. If {@code null} or empty, all properties are compared.
         * @param idExtractor the function to extract a unique identifier from each bean.
         * @return a non-{@code null} {@code BeanDifference} object containing the comparison results
         * @throws IllegalArgumentException if a non-{@code null} element of either collection is not a valid bean
         *         instance, or if {@code idExtractor} is {@code null}.
         * @throws IllegalStateException if duplicate identifiers are found within a single collection
         * @throws RuntimeException if bean metadata cannot be resolved, a selected property cannot be read, or the supplied identifier extractor or comparison callback throws during processing.
         */
        public static <T, K> BeanDifference<List<T>, List<T>, Map<K, BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>>> of(
                final Collection<? extends T> a, final Collection<? extends T> b, final Collection<String> propNamesToCompare,
                final Function<? super T, K> idExtractor) throws IllegalArgumentException, IllegalStateException, RuntimeException {
            N.checkArgNotNull(idExtractor, cs.idExtractor);

            return of(a, b, propNamesToCompare, idExtractor, idExtractor);
        }

        /**
         * Compares two collections of potentially different bean types using separate identifier extractors.
         * <p>
         * This method is useful when comparing beans from different systems or versions where:
         * <ul>
         *   <li>The bean classes may be different but represent the same concept</li>
         *   <li>The identifier extraction logic differs between the collections</li>
         *   <li>You need to map between different data models</li>
         * </ul>
         *
         * <p>Only properties with matching names between the bean types are compared. Properties
         * unique to either bean type are reported as left-only or right-only properties.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * record CustomerV1(Long customerId, String fullName, String emailAddress) {}
         *
         * record CustomerV2(String id, String fullName, String email, String phoneNumber) {}
         *
         * List<CustomerV1> oldCustomers = Arrays.asList(
         *     new CustomerV1(101L, "John Doe", "john@example.com"),
         *     new CustomerV1(102L, "Jane Smith", "jane@example.com")
         * );
         * List<CustomerV2> newCustomers = Arrays.asList(
         *     new CustomerV2("101", "John Doe", "john@newdomain.com", "555-1234"),
         *     new CustomerV2("103", "Bob Johnson", "bob@example.com", "555-5678")
         * );
         *
         * BeanDifference<?, ?, ?> diff = BeanDifference.of(
         *     oldCustomers, newCustomers,
         *     v1 -> v1.customerId().toString(),
         *     CustomerV2::id
         * );
         * // Results show:
         * // - Customer 101 has matching fullName but missing email/phoneNumber mappings
         * // - Customer 102 exists only in old system
         * // - Customer 103 exists only in new system
         * }</pre>
         *
         * @param <T1> the type of beans in the first collection
         * @param <T2> the type of beans in the second collection
         * @param <K> the type of the identifier used to match beans between collections
         * @param a the first collection of beans to compare. Can be {@code null} or empty.
         * @param b the second collection of beans to compare. Can be {@code null} or empty.
         * @param idExtractor1 the function to extract identifiers from beans in the first collection.
         * @param idExtractor2 the function to extract identifiers from beans in the second collection.
         * @return a non-{@code null} {@code BeanDifference} object containing the comparison results
         * @throws IllegalArgumentException if a non-{@code null} element of either collection is not a valid bean
         *         instance, or if any of {@code idExtractor1}, {@code idExtractor2} is {@code null}.
         * @throws IllegalStateException if duplicate identifiers are found within a single collection
         * @throws RuntimeException if bean metadata cannot be resolved, a selected property cannot be read, or the supplied identifier extractor or comparison callback throws during processing.
         */
        public static <T1, T2, K> BeanDifference<List<T1>, List<T2>, Map<K, BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>>> of(
                final Collection<? extends T1> a, final Collection<? extends T2> b, final Function<? super T1, ? extends K> idExtractor1,
                final Function<? super T2, ? extends K> idExtractor2) throws IllegalArgumentException, IllegalStateException, RuntimeException {
            N.checkArgNotNull(idExtractor1, cs.idExtractor1);
            N.checkArgNotNull(idExtractor2, cs.idExtractor2);

            return of(a, b, null, idExtractor1, idExtractor2);
        }

        /**
         * Compares two collections of potentially different bean types with separate identifier extractors
         * and selective property comparison.
         * <p>
         * This method provides the most comprehensive bean collection comparison, combining:
         * <ul>
         *   <li>Support for different bean types in each collection</li>
         *   <li>Custom identifier extraction for each collection</li>
         *   <li>Selective property comparison</li>
         * </ul>
         *
         * <p>This is particularly useful for:
         * <ul>
         *   <li>Data migration scenarios between different versions</li>
         *   <li>System integration comparisons</li>
         *   <li>Audit trails focusing on specific fields</li>
         *   <li>Performance-sensitive comparisons of large objects</li>
         * </ul>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * record Order(String orderId, String customerName, Double totalAmount,
         *         String status, Date orderDate, String notes) {}
         *
         * record OrderSummary(Long id, String customer, BigDecimal total,
         *         String status, LocalDate date, Map<String, Object> metadata) {}
         *
         * List<Order> orders = Arrays.asList(
         *     new Order("ORD-001", "John Doe", 150.00, "SHIPPED", new Date(1), "Rush delivery"),
         *     new Order("ORD-002", "Jane Smith", 75.50, "PENDING", new Date(2), "Gift wrap")
         * );
         *
         * List<OrderSummary> summaries = Arrays.asList(
         *     new OrderSummary(1L, "John Doe", new BigDecimal("150.00"), "DELIVERED", LocalDate.of(2025, 1, 1), Map.of()),
         *     new OrderSummary(3L, "Bob Johnson", new BigDecimal("200.00"), "PENDING", LocalDate.of(2025, 1, 3), Map.of())
         * );
         *
         * // Compare only status field (the only field with matching names)
         * Collection<String> propsToCompare = Arrays.asList("status");
         *
         * BeanDifference<?, ?, ?> diff = BeanDifference.of(
         *     orders, summaries, propsToCompare,
         *     order -> Long.parseLong(order.orderId().substring(4)),
         *     OrderSummary::id
         * );
         * // Compares only the "status" property for matched orders
         * }</pre>
         *
         * @param <T1> the type of beans in the first collection
         * @param <T2> the type of beans in the second collection
         * @param <K> the type of the identifier used to match beans between collections
         * @param a the first collection of beans to compare. Can be {@code null} or empty.
         * @param b the second collection of beans to compare. Can be {@code null} or empty.
         * @param propNamesToCompare the property names to compare. If {@code null} or empty, all matching properties are compared.
         * @param idExtractor1 the function to extract identifiers from beans in the first collection.
         * @param idExtractor2 the function to extract identifiers from beans in the second collection.
         * @return a non-{@code null} {@code BeanDifference} object containing the comparison results
         * @throws IllegalArgumentException if a non-{@code null} element of either collection is not a valid bean
         *         instance, or if any of {@code idExtractor1}, {@code idExtractor2} is {@code null}.
         * @throws IllegalStateException if duplicate identifiers are found within a single collection
         * @throws RuntimeException if bean metadata cannot be resolved, a selected property cannot be read, or the supplied identifier extractor or comparison callback throws during processing.
         */
        public static <T1, T2, K> BeanDifference<List<T1>, List<T2>, Map<K, BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>>> of(
                final Collection<? extends T1> a, final Collection<? extends T2> b, final Collection<String> propNamesToCompare,
                final Function<? super T1, ? extends K> idExtractor1, final Function<? super T2, ? extends K> idExtractor2)
                throws IllegalArgumentException, IllegalStateException, RuntimeException {

            N.checkArgNotNull(idExtractor1, cs.idExtractor1);
            N.checkArgNotNull(idExtractor2, cs.idExtractor2);

            // Validate every (non-null) element in both collections, not just the first: a collection whose
            // leading element is a bean (or null) can still hold a non-bean element later, which would
            // otherwise slip through and be mishandled as a bean.
            checkAllBeanElements(a);
            checkAllBeanElements(b);

            final List<T1> common = new ArrayList<>();
            final List<T1> onlyOnLeft = new ArrayList<>();
            final List<T2> onlyOnRight = new ArrayList<>();
            final Map<K, BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>> differentValues = new LinkedHashMap<>();

            if (N.isEmpty(a)) {
                if (N.isEmpty(b)) {
                    // Do nothing. All empty.
                } else {
                    // The id check belongs on this path too. It used to run only in the both-sides-non-empty
                    // branch below, so a collection carrying duplicate ids was accepted in silence while the
                    // other side was empty and started throwing the documented IllegalStateException the moment
                    // one element was added to it.
                    checkNoDuplicateIds(b, idExtractor2);

                    onlyOnRight.addAll(b);
                }
            } else if (N.isEmpty(b)) {
                checkNoDuplicateIds(a, idExtractor1);

                onlyOnLeft.addAll(a);
            } else {
                final Map<K, T1> beanMapA = N.toMap(a, idExtractor1, Fn.identity(), Fn.throwingMerger(), IntFunctions.ofLinkedHashMap());
                final Map<K, T2> beanMapB = N.toMap(b, idExtractor2, Fn.identity(), Fn.throwingMerger(), IntFunctions.ofLinkedHashMap());
                T1 beanA = null;
                T2 beanB = null;

                for (final Map.Entry<K, T1> entry : beanMapA.entrySet()) {
                    beanA = entry.getValue();

                    if (beanMapB.containsKey(entry.getKey())) {
                        beanB = beanMapB.get(entry.getKey());
                        // Compute the per-bean difference once and decide equality from it. Its areEqual()
                        // honors @DiffIgnore (and the requested property subset), unlike a raw N.equals/
                        // N.equalsByProps pre-check which would compare @DiffIgnore-annotated properties too.
                        final BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> beanDiff = BeanDifference.of(beanA,
                                beanB, propNamesToCompare);

                        if (beanDiff.areEqual()) {
                            common.add(beanA);
                        } else {
                            differentValues.put(entry.getKey(), beanDiff);
                        }
                    } else {
                        onlyOnLeft.add(beanA);
                    }
                }

                for (final Map.Entry<K, T2> entry : beanMapB.entrySet()) {
                    if (!beanMapA.containsKey(entry.getKey())) {
                        onlyOnRight.add(entry.getValue());
                    }
                }
            }

            return new BeanDifference<>(common, onlyOnLeft, onlyOnRight, differentValues);

        }

        /**
         * @throws IllegalArgumentException if a non-null element of {@code c} does not have a bean class
         */
        private static void checkAllBeanElements(final Collection<?> c) throws IllegalArgumentException {
            if (N.isEmpty(c)) {
                return;
            }

            for (final Object e : c) {
                if (e != null && !Beans.isBeanClass(e.getClass())) {
                    throw new IllegalArgumentException(e.getClass().getCanonicalName() + " is not a bean class"); // NOSONAR
                }
            }
        }
    }
}
