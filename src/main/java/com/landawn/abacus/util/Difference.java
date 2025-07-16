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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
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
 * <p>
 * The {@code Difference} class provides methods to find:
 * <ul>
 *   <li>Common elements present in both collections</li>
 *   <li>Elements only present in the first collection (left)</li>
 *   <li>Elements only present in the second collection (right)</li>
 * </ul>
 * 
 * <p>When comparing collections, the comparison takes occurrences into account. This means that if an element
 * appears multiple times in either collection, each occurrence is considered separately.
 * 
 * <p>Example usage:
 * <pre>{@code
 * List<String> list1 = Arrays.asList("a", "b", "c", "b");
 * List<String> list2 = Arrays.asList("b", "c", "d", "c");
 * Difference<List<String>, List<String>> diff = Difference.of(list1, list2);
 * 
 * // diff.inCommon() returns ["b", "c"]
 * // diff.onLeftOnly() returns ["a", "b"]
 * // diff.onRightOnly() returns ["d", "c"]
 * }</pre>
 * 
 * <p>For maps and beans, additional comparison capabilities are provided through the {@link MapDifference} 
 * and {@link BeanDifference} subclasses, which can also identify entries/properties with different values.
 *
 * @param <L> The type of the collection containing elements from the left (first) collection
 * @param <R> The type of the collection containing elements from the right (second) collection
 * @see com.landawn.abacus.annotation.DiffIgnore
 * @see N#difference(Collection, Collection)
 * @see N#symmetricDifference(Collection, Collection)
 * @see N#excludeAll(Collection, Collection)
 * @see N#excludeAllToSet(Collection, Collection)
 * @see N#removeAll(Collection, Iterable)
 * @see N#intersection(Collection, Collection)
 * @see N#commonSet(Collection, Collection)
 */
public sealed class Difference<L, R> permits KeyValueDifference {

    final L common;

    final L leftOnly;

    final R rightOnly;

    Difference(final L common, final L leftOnly, final R rightOnly) {
        this.common = common;
        this.leftOnly = leftOnly;
        this.rightOnly = rightOnly;
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
     * @param a The first boolean array to compare. Can be {@code null}, which is treated as an empty array.
     * @param b The second boolean array to compare. Can be {@code null}, which is treated as an empty array.
     * @return A {@code Difference} object containing {@code BooleanList} instances for common elements,
     *         elements only in the first array, and elements only in the second array.
     * @see IntList#difference(IntList)
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
     * @param a The first char array to compare. Can be {@code null}, which is treated as an empty array.
     * @param b The second char array to compare. Can be {@code null}, which is treated as an empty array.
     * @return A {@code Difference} object containing {@code CharList} instances for common elements,
     *         elements only in the first array, and elements only in the second array.
     * @see IntList#difference(IntList)
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
     * @param a The first byte array to compare. Can be {@code null}, which is treated as an empty array.
     * @param b The second byte array to compare. Can be {@code null}, which is treated as an empty array.
     * @return A {@code Difference} object containing {@code ByteList} instances for common elements,
     *         elements only in the first array, and elements only in the second array.
     * @see IntList#difference(IntList)
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
     * @param a The first short array to compare. Can be {@code null}, which is treated as an empty array.
     * @param b The second short array to compare. Can be {@code null}, which is treated as an empty array.
     * @return A {@code Difference} object containing {@code ShortList} instances for common elements,
     *         elements only in the first array, and elements only in the second array.
     * @see IntList#difference(IntList)
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
     * @param a The first int array to compare. Can be {@code null}, which is treated as an empty array.
     * @param b The second int array to compare. Can be {@code null}, which is treated as an empty array.
     * @return A {@code Difference} object containing {@code IntList} instances for common elements,
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
     * @param a The first long array to compare. Can be {@code null}, which is treated as an empty array.
     * @param b The second long array to compare. Can be {@code null}, which is treated as an empty array.
     * @return A {@code Difference} object containing {@code LongList} instances for common elements,
     *         elements only in the first array, and elements only in the second array.
     * @see IntList#difference(IntList)
     * @see N#difference(Collection, Collection)
     */
    public static Difference<LongList, LongList> of(final long[] a, final long[] b) {
        return of(LongList.of(a), LongList.of(b));
    }

    /**
     * Compares two float arrays and identifies the differences between them.
     * 
     * <p>Float comparison uses standard equality (==). Special float values like NaN, positive
     * infinity, and negative infinity are compared according to Java's float equality rules.
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
     * @param a The first float array to compare. Can be {@code null}, which is treated as an empty array.
     * @param b The second float array to compare. Can be {@code null}, which is treated as an empty array.
     * @return A {@code Difference} object containing {@code FloatList} instances for common elements,
     *         elements only in the first array, and elements only in the second array.
     * @see IntList#difference(IntList)
     * @see N#difference(Collection, Collection)
     */
    public static Difference<FloatList, FloatList> of(final float[] a, final float[] b) {
        return of(FloatList.of(a), FloatList.of(b));
    }

    /**
     * Compares two double arrays and identifies the differences between them.
     * 
     * <p>Double comparison uses standard equality (==). Special double values like NaN, positive
     * infinity, and negative infinity are compared according to Java's double equality rules.
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
     * @param a The first double array to compare. Can be {@code null}, which is treated as an empty array.
     * @param b The second double array to compare. Can be {@code null}, which is treated as an empty array.
     * @return A {@code Difference} object containing {@code DoubleList} instances for common elements,
     *         elements only in the first array, and elements only in the second array.
     * @see IntList#difference(IntList)
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
     * @param <T1> The element type of the first array
     * @param <T2> The element type of the second array
     * @param <L> The type of List containing elements of type T1
     * @param <R> The type of List containing elements of type T2
     * @param a The first array to compare. Can be {@code null}, which is treated as an empty array.
     * @param b The second array to compare. Can be {@code null}, which is treated as an empty array.
     * @return A {@code Difference} object containing lists of common elements (type L),
     *         elements only in the first array (type L), and elements only in the second array (type R).
     * @see IntList#difference(IntList)
     * @see N#difference(Collection, Collection)
     * @see N#symmetricDifference(Collection, Collection)
     * @see N#excludeAll(Collection, Collection)
     * @see N#excludeAllToSet(Collection, Collection)
     * @see N#removeAll(Collection, Iterable)
     * @see N#intersection(Collection, Collection)
     * @see N#commonSet(Collection, Collection)
     */
    public static <T1, T2, L extends List<T1>, R extends List<T2>> Difference<L, R> of(final T1[] a, final T2[] b) {
        return of(Arrays.asList(a), Arrays.asList(b));
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
     * <pre>{@code
     * Collection<String> c1 = Arrays.asList("a", "b", "b", "c");
     * Collection<String> c2 = Arrays.asList("b", "b", "b", "d");
     * Difference<List<String>, List<String>> diff = Difference.of(c1, c2);
     * // Results in:
     * // common: ["b", "b"]
     * // leftOnly: ["a", "c"]
     * // rightOnly: ["b", "d"]
     * }</pre>
     * 
     * <p>The comparison takes into account the number of occurrences of each value. For example,
     * if {@code "apple"} appears twice in the first collection and once in the second collection, the result will
     * have one {@code "apple"} in common and one {@code "apple"} in left only.
     *
     * @param <T1> The element type of the first collection
     * @param <T2> The element type of the second collection
     * @param <L> The type of List containing elements of type T1
     * @param <R> The type of List containing elements of type T2
     * @param a The first collection to compare. Can be {@code null} or empty.
     * @param b The second collection to compare. Can be {@code null} or empty.
     * @return A {@code Difference} object containing lists of common elements (type L),
     *         elements only in the first collection (type L), and elements only in the second collection (type R).
     * @see IntList#difference(IntList)
     * @see N#difference(Collection, Collection)
     * @see N#symmetricDifference(Collection, Collection)
     * @see N#excludeAll(Collection, Collection)
     * @see N#excludeAllToSet(Collection, Collection)
     * @see N#removeAll(Collection, Iterable)
     * @see N#intersection(Collection, Collection)
     * @see N#commonSet(Collection, Collection)
     */
    @SuppressWarnings("unlikely-arg-type")
    public static <T1, T2, L extends List<T1>, R extends List<T2>> Difference<L, R> of(final Collection<? extends T1> a, final Collection<? extends T2> b) {
        final List<T1> common = new ArrayList<>();
        final List<T1> leftOnly = new ArrayList<>();
        final List<T2> rightOnly = new ArrayList<>();

        if (N.isEmpty(a)) {
            if (N.isEmpty(b)) {
                // Do nothing. All empty.
            } else {
                rightOnly.addAll(b);
            }
        } else if (N.isEmpty(b)) {
            leftOnly.addAll(a);
        } else {
            final Multiset<T2> bOccurrences = Multiset.create(b);

            for (final T1 e : a) {
                //noinspection SuspiciousMethodCalls
                if (bOccurrences.remove(e)) {
                    common.add(e);
                } else {
                    leftOnly.add(e);
                }
            }

            for (final T2 e : b) {
                if (bOccurrences.remove(e)) {
                    rightOnly.add(e);
                }

                if (bOccurrences.isEmpty()) {
                    break;
                }
            }
        }

        return new Difference<>((L) common, (L) leftOnly, (R) rightOnly);
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
     * @param a The first BooleanList to compare. Can be {@code null} or empty.
     * @param b The second BooleanList to compare. Can be {@code null} or empty.
     * @return A {@code Difference} object containing BooleanLists for common elements,
     *         elements only in the first list, and elements only in the second list.
     * @see IntList#difference(IntList)
     * @see N#difference(Collection, Collection)
     */
    public static Difference<BooleanList, BooleanList> of(final BooleanList a, final BooleanList b) {
        final BooleanList common = new BooleanList();
        BooleanList leftOnly = new BooleanList();
        BooleanList rightOnly = new BooleanList();

        if (N.isEmpty(a)) {
            if (N.isEmpty(b)) {
                // Do nothing. All empty.
            } else {
                rightOnly = b.copy();
            }
        } else if (N.isEmpty(b)) {
            leftOnly = a.copy();
        } else {
            final Multiset<Boolean> bOccurrences = b.toMultiset();

            boolean e = false;
            for (int i = 0, len = a.size(); i < len; i++) {
                e = a.get(i);

                if (bOccurrences.remove(e)) {
                    common.add(e);
                } else {
                    leftOnly.add(e);
                }
            }

            for (int i = 0, len = b.size(); i < len; i++) {
                e = b.get(i);

                if (bOccurrences.remove(e)) {
                    rightOnly.add(e);
                }

                if (bOccurrences.isEmpty()) {
                    break;
                }
            }
        }

        return new Difference<>(common, leftOnly, rightOnly);
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
     * @param a The first CharList to compare. Can be {@code null} or empty.
     * @param b The second CharList to compare. Can be {@code null} or empty.
     * @return A {@code Difference} object containing CharLists for common elements,
     *         elements only in the first list, and elements only in the second list.
     * @see IntList#difference(IntList)
     * @see N#difference(Collection, Collection)
     */
    public static Difference<CharList, CharList> of(final CharList a, final CharList b) {
        final CharList common = new CharList();
        CharList leftOnly = new CharList();
        CharList rightOnly = new CharList();

        if (N.isEmpty(a)) {
            if (N.isEmpty(b)) {
                // Do nothing. All empty.
            } else {
                rightOnly = b.copy();
            }
        } else if (N.isEmpty(b)) {
            leftOnly = a.copy();
        } else {
            final Multiset<Character> bOccurrences = b.toMultiset();

            char e = 0;
            for (int i = 0, len = a.size(); i < len; i++) {
                e = a.get(i);

                if (bOccurrences.remove(e)) {
                    common.add(e);
                } else {
                    leftOnly.add(e);
                }
            }

            for (int i = 0, len = b.size(); i < len; i++) {
                e = b.get(i);

                if (bOccurrences.remove(e)) {
                    rightOnly.add(e);
                }

                if (bOccurrences.isEmpty()) {
                    break;
                }
            }
        }

        return new Difference<>(common, leftOnly, rightOnly);
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
     * @param a The first ByteList to compare. Can be {@code null} or empty.
     * @param b The second ByteList to compare. Can be {@code null} or empty.
     * @return A {@code Difference} object containing ByteLists for common elements,
     *         elements only in the first list, and elements only in the second list.
     * @see IntList#difference(IntList)
     * @see N#difference(Collection, Collection)
     */
    public static Difference<ByteList, ByteList> of(final ByteList a, final ByteList b) {
        final ByteList common = new ByteList();
        ByteList leftOnly = new ByteList();
        ByteList rightOnly = new ByteList();

        if (N.isEmpty(a)) {
            if (N.isEmpty(b)) {
                // Do nothing. All empty.
            } else {
                rightOnly = b.copy();
            }
        } else if (N.isEmpty(b)) {
            leftOnly = a.copy();
        } else {
            final Multiset<Byte> bOccurrences = b.toMultiset();

            byte e = 0;
            for (int i = 0, len = a.size(); i < len; i++) {
                e = a.get(i);

                if (bOccurrences.remove(e)) {
                    common.add(e);
                } else {
                    leftOnly.add(e);
                }
            }

            for (int i = 0, len = b.size(); i < len; i++) {
                e = b.get(i);

                if (bOccurrences.remove(e)) {
                    rightOnly.add(e);
                }

                if (bOccurrences.isEmpty()) {
                    break;
                }
            }
        }

        return new Difference<>(common, leftOnly, rightOnly);
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
     * @param a The first ShortList to compare. Can be {@code null} or empty.
     * @param b The second ShortList to compare. Can be {@code null} or empty.
     * @return A {@code Difference} object containing ShortLists for common elements,
     *         elements only in the first list, and elements only in the second list.
     * @see IntList#difference(IntList)
     * @see N#difference(Collection, Collection)
     */
    public static Difference<ShortList, ShortList> of(final ShortList a, final ShortList b) {
        final ShortList common = new ShortList();
        ShortList leftOnly = new ShortList();
        ShortList rightOnly = new ShortList();

        if (N.isEmpty(a)) {
            if (N.isEmpty(b)) {
                // Do nothing. All empty.
            } else {
                rightOnly = b.copy();
            }
        } else if (N.isEmpty(b)) {
            leftOnly = a.copy();
        } else {
            final Multiset<Short> bOccurrences = b.toMultiset();

            short e = 0;
            for (int i = 0, len = a.size(); i < len; i++) {
                e = a.get(i);

                if (bOccurrences.remove(e)) {
                    common.add(e);
                } else {
                    leftOnly.add(e);
                }
            }

            for (int i = 0, len = b.size(); i < len; i++) {
                e = b.get(i);

                if (bOccurrences.remove(e)) {
                    rightOnly.add(e);
                }

                if (bOccurrences.isEmpty()) {
                    break;
                }
            }
        }

        return new Difference<>(common, leftOnly, rightOnly);
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
     * @param a The first IntList to compare. Can be {@code null} or empty.
     * @param b The second IntList to compare. Can be {@code null} or empty.
     * @return A {@code Difference} object containing IntLists for common elements,
     *         elements only in the first list, and elements only in the second list.
     * @see IntList#difference(IntList)
     * @see N#difference(Collection, Collection)
     */
    public static Difference<IntList, IntList> of(final IntList a, final IntList b) {
        final IntList common = new IntList();
        IntList leftOnly = new IntList();
        IntList rightOnly = new IntList();

        if (N.isEmpty(a)) {
            if (N.isEmpty(b)) {
                // Do nothing. All empty.
            } else {
                rightOnly = b.copy();
            }
        } else if (N.isEmpty(b)) {
            leftOnly = a.copy();
        } else {
            final Multiset<Integer> bOccurrences = b.toMultiset();

            int e = 0;
            for (int i = 0, len = a.size(); i < len; i++) {
                e = a.get(i);

                if (bOccurrences.remove(e)) {
                    common.add(e);
                } else {
                    leftOnly.add(e);
                }
            }

            for (int i = 0, len = b.size(); i < len; i++) {
                e = b.get(i);

                if (bOccurrences.remove(e)) {
                    rightOnly.add(e);
                }

                if (bOccurrences.isEmpty()) {
                    break;
                }
            }
        }

        return new Difference<>(common, leftOnly, rightOnly);
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
     * @param a The first LongList to compare. Can be {@code null} or empty.
     * @param b The second LongList to compare. Can be {@code null} or empty.
     * @return A {@code Difference} object containing LongLists for common elements,
     *         elements only in the first list, and elements only in the second list.
     * @see IntList#difference(IntList)
     * @see N#difference(Collection, Collection)
     */
    public static Difference<LongList, LongList> of(final LongList a, final LongList b) {
        final LongList common = new LongList();
        LongList leftOnly = new LongList();
        LongList rightOnly = new LongList();

        if (N.isEmpty(a)) {
            if (N.isEmpty(b)) {
                // Do nothing. All empty.
            } else {
                rightOnly = b.copy();
            }
        } else if (N.isEmpty(b)) {
            leftOnly = a.copy();
        } else {
            final Multiset<Long> bOccurrences = b.toMultiset();

            long e = 0;
            for (int i = 0, len = a.size(); i < len; i++) {
                e = a.get(i);

                if (bOccurrences.remove(e)) {
                    common.add(e);
                } else {
                    leftOnly.add(e);
                }
            }

            for (int i = 0, len = b.size(); i < len; i++) {
                e = b.get(i);

                if (bOccurrences.remove(e)) {
                    rightOnly.add(e);
                }

                if (bOccurrences.isEmpty()) {
                    break;
                }
            }
        }

        return new Difference<>(common, leftOnly, rightOnly);
    }

    /**
     * Compares two FloatLists and identifies the differences between them.
     * 
     * <p>Float values are compared using standard equality. Special values (NaN, positive/negative
     * infinity) are handled according to Java's float comparison rules. Each occurrence of a value
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
     * @param a The first FloatList to compare. Can be {@code null} or empty.
     * @param b The second FloatList to compare. Can be {@code null} or empty.
     * @return A {@code Difference} object containing FloatLists for common elements,
     *         elements only in the first list, and elements only in the second list.
     * @see IntList#difference(IntList)
     * @see N#difference(Collection, Collection)
     */
    public static Difference<FloatList, FloatList> of(final FloatList a, final FloatList b) {
        final FloatList common = new FloatList();
        FloatList leftOnly = new FloatList();
        FloatList rightOnly = new FloatList();

        if (N.isEmpty(a)) {
            if (N.isEmpty(b)) {
                // Do nothing. All empty.
            } else {
                rightOnly = b.copy();
            }
        } else if (N.isEmpty(b)) {
            leftOnly = a.copy();
        } else {
            final Multiset<Float> bOccurrences = b.toMultiset();

            float e = 0;
            for (int i = 0, len = a.size(); i < len; i++) {
                e = a.get(i);

                if (bOccurrences.remove(e)) {
                    common.add(e);
                } else {
                    leftOnly.add(e);
                }
            }

            for (int i = 0, len = b.size(); i < len; i++) {
                e = b.get(i);

                if (bOccurrences.remove(e)) {
                    rightOnly.add(e);
                }

                if (bOccurrences.isEmpty()) {
                    break;
                }
            }
        }

        return new Difference<>(common, leftOnly, rightOnly);
    }

    /**
     * Compares two DoubleLists and identifies the differences between them.
     * 
     * <p>Double values are compared using standard equality. Special values (NaN, positive/negative
     * infinity) are handled according to Java's double comparison rules. The method efficiently
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
     * @param a The first DoubleList to compare. Can be {@code null} or empty.
     * @param b The second DoubleList to compare. Can be {@code null} or empty.
     * @return A {@code Difference} object containing DoubleLists for common elements,
     *         elements only in the first list, and elements only in the second list.
     * @see IntList#difference(IntList)
     * @see N#difference(Collection, Collection)
     */
    public static Difference<DoubleList, DoubleList> of(final DoubleList a, final DoubleList b) {
        final DoubleList common = new DoubleList();
        DoubleList leftOnly = new DoubleList();
        DoubleList rightOnly = new DoubleList();

        if (N.isEmpty(a)) {
            if (N.isEmpty(b)) {
                // Do nothing. All empty.
            } else {
                rightOnly = b.copy();
            }
        } else if (N.isEmpty(b)) {
            leftOnly = a.copy();
        } else {
            final Multiset<Double> bOccurrences = b.toMultiset();

            double e = 0;
            for (int i = 0, len = a.size(); i < len; i++) {
                e = a.get(i);

                if (bOccurrences.remove(e)) {
                    common.add(e);
                } else {
                    leftOnly.add(e);
                }
            }

            for (int i = 0, len = b.size(); i < len; i++) {
                e = b.get(i);

                if (bOccurrences.remove(e)) {
                    rightOnly.add(e);
                }

                if (bOccurrences.isEmpty()) {
                    break;
                }
            }
        }

        return new Difference<>(common, leftOnly, rightOnly);
    }

    /**
     * Returns the elements that are common to both collections/arrays.
     * <p>
     * These are elements that appear in both the left (first) and right (second) collections.
     * The number of occurrences in the result corresponds to the minimum number of occurrences
     * in either collection. For example, if an element appears 3 times in the first collection
     * and 2 times in the second, it will appear 2 times in the common elements.
     * 
     * <p>The returned collection maintains the order of elements as they appear in the first collection.
     *
     * @return A collection containing the common elements. The type L is typically a List or similar collection.
     */
    public L inCommon() {
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
     * <p>The returned collection maintains the order of elements as they appear in the first collection.
     *
     * @return A collection containing elements only in the left collection. The type L is typically a List or similar collection.
     */
    public L onLeftOnly() {
        return leftOnly;
    }

    /**
     * Returns the elements that exist only in the right (second) collection/array.
     * <p>
     * These are elements that appear in the second collection but not in the first collection,
     * or elements that appear more frequently in the second collection than in the first.
     * For example, if an element appears 2 times in the first collection and 5 times in the
     * second collection, it will appear 3 times in the right-only elements.
     * 
     * <p>The returned collection maintains the order of elements as they appear in the second collection.
     *
     * @return A collection containing elements only in the right collection. The type R is typically a List or similar collection.
     */
    public R onRightOnly() {
        return rightOnly;
    }

    /**
     * Checks whether the two compared collections/arrays contain exactly the same elements with the same occurrences.
     * <p>
     * This method returns {@code true} if and only if:
     * <ul>
     *   <li>Both leftOnly and rightOnly collections are empty</li>
     *   <li>All elements from both input collections are in the common elements</li>
     * </ul>
     * 
     * <p>Note that element order is not considered - only the presence and count of each element matters.
     * For example, [1, 2, 3] and [3, 2, 1] would be considered equal, but [1, 2, 2] and [1, 2, 3]
     * would not be equal.
     *
     * @return {@code true} if the two compared collections have exactly the same elements with the same occurrences, {@code false} otherwise
     */
    @SuppressWarnings("rawtypes")
    public boolean areEqual() {
        return (leftOnly instanceof Collection && (((Collection) leftOnly).isEmpty() && ((Collection) rightOnly).isEmpty()))
                || (leftOnly instanceof Map && (((Map) leftOnly).isEmpty() && ((Map) rightOnly).isEmpty()))
                || (leftOnly instanceof PrimitiveList && (((PrimitiveList) leftOnly).isEmpty() && ((PrimitiveList) rightOnly).isEmpty()));
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
     * @param obj The object to compare with this {@code Difference}
     * @return {@code true} if the specified object is equal to this {@code Difference}, {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj != null && obj.getClass().equals(Difference.class)) {
            final Difference<?, ?> other = (Difference<?, ?>) obj;
            return inCommon().equals(other.inCommon()) && onLeftOnly().equals(other.onLeftOnly()) && onRightOnly().equals(other.onRightOnly());
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
     * @return A hash code value for this {@code Difference} object
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + N.hashCode(inCommon());
        result = prime * result + N.hashCode(onLeftOnly());
        return prime * result + N.hashCode(onRightOnly());
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
     * <p>Format: {@code {areEqual=<boolean>, inCommon=<common>, onLeftOnly=<leftOnly>, onRightOnly=<rightOnly>}}
     * 
     * <p>This method is useful for debugging and logging purposes.
     *
     * @return A string representation of this {@code Difference} object
     */
    @Override
    public String toString() {
        return "{areEqual=" + areEqual() + ", inCommon=" + common + ", onLeftOnly=" + leftOnly + ", onRightOnly=" + rightOnly + "}";
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
     * @param <L> The type of the collection containing entries/properties from the left (first) structure
     * @param <R> The type of the collection containing entries/properties from the right (second) structure
     * @param <D> The type of the collection containing entries/properties with different values
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
    static abstract sealed class KeyValueDifference<L, R, D> extends Difference<L, R> permits MapDifference, BeanDifference {

        /** The different values. */
        private final D diffValues;

        /**
         * Instantiates a new KeyValueDifference.
         *
         * @param common
         * @param leftOnly
         * @param rightOnly
         * @param withDifferentValues
         */
        KeyValueDifference(final L common, final L leftOnly, final R rightOnly, final D withDifferentValues) {
            super(common, leftOnly, rightOnly);
            diffValues = withDifferentValues;
        }

        /**
         * Returns the entries/properties that exist in both structures but have different values.
         * <p>
         * For maps, this returns a map where:
         * <ul>
         *   <li>Keys are the common keys between both maps</li>
         *   <li>Values are {@link Pair} objects containing the values from the left and right maps</li>
         * </ul>
         * 
         * <p>For beans, this returns a map where:
         * <ul>
         *   <li>Keys are the property names that exist in both beans</li>
         *   <li>Values are {@link Pair} objects containing the property values from the left and right beans</li>
         * </ul>
         * 
         * <p>Example:
         * <pre>{@code
         * Map<String, Integer> map1 = Map.of("a", 1, "b", 2);
         * Map<String, Integer> map2 = Map.of("a", 1, "b", 3);
         * MapDifference<...> diff = MapDifference.of(map1, map2);
         * // diff.withDifferentValues() returns {"b": Pair.of(2, 3)}
         * }</pre>
         *
         * @return A map containing entries/properties with different values. The type D is typically
         *         a Map with Pair values representing the differing values from left and right.
         */
        public D withDifferentValues() {
            return diffValues;
        }

        /**
         * Checks whether the two compared structures have exactly the same entries/properties with the same values.
         * <p>
         * This method returns {@code true} if and only if:
         * <ul>
         *   <li>Both structures have no entries/properties that exist only in one structure (leftOnly and rightOnly are empty)</li>
         *   <li>There are no entries/properties with different values (withDifferentValues is empty)</li>
         * </ul>
         * 
         * <p>This is a stricter equality check than the parent class's {@link #areEqual()} method,
         * as it also considers whether any entries/properties have different values.
         *
         * @return {@code true} if the two structures have exactly the same entries/properties with the same values, {@code false} otherwise
         */
        @SuppressWarnings("rawtypes")
        @Override
        public boolean areEqual() {
            return super.areEqual() && ((Map) diffValues).isEmpty();
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
         * @param obj The object to compare with this {@code KeyValueDifference}
         * @return {@code true} if the specified object is equal to this {@code KeyValueDifference}, {@code false} otherwise
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }

            if (obj instanceof KeyValueDifference other) {
                return inCommon().equals(other.inCommon()) && onLeftOnly().equals(other.onLeftOnly()) && onRightOnly().equals(other.onRightOnly())
                        && withDifferentValues().equals(other.withDifferentValues());
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
         * @return A hash code value for this {@code KeyValueDifference} object
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + N.hashCode(inCommon());
            result = prime * result + N.hashCode(onLeftOnly());
            result = prime * result + N.hashCode(onRightOnly());
            return prime * result + N.hashCode(withDifferentValues());
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
         * <p>Format: {@code {areEqual=<boolean>, inCommon=<common>, onLeftOnly=<leftOnly>, onRightOnly=<rightOnly>, withDifferentValues=<diffValues>}}
         *
         * @return A string representation of this {@code KeyValueDifference} object
         */
        @Override
        public String toString() {
            return "{areEqual=" + areEqual() + ", inCommon=" + common + ", onLeftOnly=" + leftOnly + ", onRightOnly=" + rightOnly + ", withDifferentValues="
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
     * <p>Example usage:
     * <pre>{@code
     * Map<String, Integer> map1 = Map.of("a", 1, "b", 2, "c", 3);
     * Map<String, Integer> map2 = Map.of("b", 2, "c", 4, "d", 5);
     * MapDifference<...> diff = MapDifference.of(map1, map2);
     * 
     * // diff.inCommon() returns {"b": 2}
     * // diff.onLeftOnly() returns {"a": 1}
     * // diff.onRightOnly() returns {"d": 5}
     * // diff.withDifferentValues() returns {"c": Pair.of(3, 4)}
     * }</pre>
     *
     * @param <L> The type of the map containing entries from the left (first) map
     * @param <R> The type of the map containing entries from the right (second) map
     * @param <D> The type of the map containing entries with different values (typically Map with Pair values)
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
         * Instantiates a new {@code MapDifference}.
         *
         * @param common
         * @param leftOnly
         * @param rightOnly
         * @param withDifferentValues
         */
        MapDifference(final L common, final L leftOnly, final R rightOnly, final D withDifferentValues) {
            super(common, leftOnly, rightOnly, withDifferentValues);
        }

        /**
         * Compares two maps and identifies the differences between them using default equality comparison for values.
         * <p>
         * This method creates a {@code MapDifference} object that contains:
         * <ul>
         *   <li>Common entries: Key-value pairs present in both maps with equal values (using {@code equals})</li>
         *   <li>Left-only entries: Key-value pairs present only in the first map</li>
         *   <li>Right-only entries: Key-value pairs present only in the second map</li>
         *   <li>Different values: Keys present in both maps but with different values</li>
         * </ul>
         * 
         * <p>The comparison uses the standard {@code equals} method to compare values. Null values are
         * handled correctly - if both maps have a null value for the same key, it's considered a common entry.
         * 
         * <p>The order of entries in the result maps depends on the input map types:
         * <ul>
         *   <li>If either input map is a {@link LinkedHashMap} or {@link SortedMap}, results use {@link LinkedHashMap}</li>
         *   <li>Otherwise, results use {@link HashMap}</li>
         * </ul>
         * 
         * <p>Example:
         * <pre>{@code
         * Map<String, Integer> map1 = Map.of("a", 1, "b", 2, "c", 3);
         * Map<String, Integer> map2 = Map.of("b", 2, "c", 4, "d", 5);
         * MapDifference<...> diff = MapDifference.of(map1, map2);
         * // Results in:
         * // common: {"b": 2}
         * // leftOnly: {"a": 1}
         * // rightOnly: {"d": 5}
         * // withDifferentValues: {"c": Pair.of(3, 4)}
         * }</pre>
         *
         * @param <CK> The common key type shared by both maps
         * @param <K1> The key type of the first map (must extend CK)
         * @param <V1> The value type of the first map
         * @param <K2> The key type of the second map (must extend CK)
         * @param <V2> The value type of the second map
         * @param map1 The first map to compare. Can be {@code null} or empty.
         * @param map2 The second map to compare. Can be {@code null} or empty.
         * @return A {@code MapDifference} object containing the comparison results
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
                final Map<? extends K1, ? extends V1> map1, final Map<? extends K2, ? extends V2> map2) {
            return of(map1, map2, null, (k, v1, v2) -> N.equals(v1, v2));
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
         * <p>If {@code keysToCompare} is {@code null}, all keys from both maps are compared (equivalent
         * to calling {@link #of(Map, Map)}).
         * 
         * <p>Example:
         * <pre>{@code
         * Map<String, Integer> map1 = Map.of("a", 1, "b", 2, "c", 3, "d", 4);
         * Map<String, Integer> map2 = Map.of("a", 1, "b", 5, "c", 3, "e", 6);
         * Collection<String> keys = Arrays.asList("a", "b", "e");
         * MapDifference<...> diff = MapDifference.of(map1, map2, keys);
         * // Results in:
         * // common: {"a": 1}
         * // leftOnly: {} (empty, since "a" and "b" exist in both, "e" doesn't exist in map1)
         * // rightOnly: {"e": 6}
         * // withDifferentValues: {"b": Pair.of(2, 5)}
         * // Note: "c" and "d" are not compared because they're not in keysToCompare
         * }</pre>
         *
         * @param <CK> The common key type shared by both maps
         * @param <K1> The key type of the first map (must extend CK)
         * @param <V1> The value type of the first map
         * @param <K2> The key type of the second map (must extend CK)
         * @param <V2> The value type of the second map
         * @param map1 The first map to compare. Can be {@code null} or empty.
         * @param map2 The second map to compare. Can be {@code null} or empty.
         * @param keysToCompare The keys to compare between the two maps. If {@code null}, all keys will be compared.
         * @return A {@code MapDifference} object containing the comparison results for the specified keys
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
                final Map<? extends K1, ? extends V1> map1, final Map<? extends K2, ? extends V2> map2, final Collection<CK> keysToCompare) {
            return of(map1, map2, keysToCompare, (k, v1, v2) -> N.equals(v1, v2));
        }

        /**
         * Compares two maps using a custom value equivalence predicate to determine if values are equal.
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
         * <p>Example with custom equivalence:
         * <pre>{@code
         * Map<String, Double> map1 = Map.of("a", 1.0, "b", 2.001);
         * Map<String, Double> map2 = Map.of("a", 1.0, "b", 2.0);
         * 
         * // Compare with tolerance of 0.01
         * BiPredicate<Double, Double> approxEqual = (v1, v2) -> Math.abs(v1 - v2) < 0.01;
         * MapDifference<...> diff = MapDifference.of(map1, map2, approxEqual);
         * // Results in:
         * // common: {"a": 1.0, "b": 2.001} (both are considered equal)
         * // leftOnly: {}
         * // rightOnly: {}
         * // withDifferentValues: {}
         * }</pre>
         *
         * @param <CK> The common key type shared by both maps
         * @param <K1> The key type of the first map (must extend CK)
         * @param <V1> The value type of the first map
         * @param <K2> The key type of the second map (must extend CK)
         * @param <V2> The value type of the second map
         * @param map1 The first map to compare. Can be {@code null} or empty.
         * @param map2 The second map to compare. Can be {@code null} or empty.
         * @param valueEquivalence The predicate to determine if two values are equivalent. Must not be {@code null}.
         * @return A {@code MapDifference} object containing the comparison results
         * @throws IllegalArgumentException if {@code valueEquivalence} is {@code null}
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
                final Map<? extends K1, ? extends V1> map1, final Map<? extends K2, ? extends V2> map2,
                final BiPredicate<? super V1, ? super V2> valueEquivalence) throws IllegalArgumentException {
            N.checkArgNotNull(valueEquivalence, cs.valueEquivalence);

            return of(map1, map2, null, (k, v1, v2) -> valueEquivalence.test(v1, v2));
        }

        /**
         * Compares two maps using a custom value equivalence predicate that also considers the key.
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
         * <p>Example with key-dependent equivalence:
         * <pre>{@code
         * Map<String, Double> prices1 = Map.of("apple", 1.99, "gold", 1850.50);
         * Map<String, Double> prices2 = Map.of("apple", 2.01, "gold", 1851.00);
         * 
         * // Different tolerance for different items
         * TriPredicate<String, Double, Double> priceEqual = (key, v1, v2) -> {
         *     double tolerance = key.equals("gold") ? 5.0 : 0.1;
         *     return Math.abs(v1 - v2) <= tolerance;
         * };
         * 
         * MapDifference<...> diff = MapDifference.of(prices1, prices2, priceEqual);
         * // Results in:
         * // common: {"gold": 1850.50} (within $5 tolerance)
         * // leftOnly: {}
         * // rightOnly: {}
         * // withDifferentValues: {"apple": Pair.of(1.99, 2.01)} (exceeds $0.10 tolerance)
         * }</pre>
         *
         * @param <CK> The common key type shared by both maps
         * @param <K1> The key type of the first map (must extend CK)
         * @param <V1> The value type of the first map
         * @param <K2> The key type of the second map (must extend CK)
         * @param <V2> The value type of the second map
         * @param map1 The first map to compare. Can be {@code null} or empty.
         * @param map2 The second map to compare. Can be {@code null} or empty.
         * @param valueEquivalence The predicate to determine if two values are equivalent for a given key. Must not be {@code null}.
         * @return A {@code MapDifference} object containing the comparison results
         * @throws IllegalArgumentException if {@code valueEquivalence} is {@code null}
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
                final Map<? extends K1, ? extends V1> map1, final Map<? extends K2, ? extends V2> map2,
                final TriPredicate<? super K1, ? super V1, ? super V2> valueEquivalence) throws IllegalArgumentException {
            N.checkArgNotNull(valueEquivalence, cs.valueEquivalence);

            return of(map1, map2, null, valueEquivalence);
        }

        /**
         * Compares two maps for specified keys using a custom value equivalence predicate.
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
         *   <li>Only keys in {@code keysToCompare} are considered (if null, all keys are compared)</li>
         *   <li>For each key, values are compared using the {@code valueEquivalence} predicate</li>
         *   <li>The predicate receives the key and both values, allowing key-dependent comparison</li>
         * </ol>
         * 
         * <p>Example:
         * <pre>{@code
         * Map<String, Object> config1 = Map.of(
         *     "timeout", 30,
         *     "retries", 3,
         *     "debug", true,
         *     "url", "http://api.com"
         * );
         * Map<String, Object> config2 = Map.of(
         *     "timeout", 30.0,  // Note: different type
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
         *         return v1.toString().equals(v2.toString());
         *     }
         *     return Objects.equals(v1, v2);
         * };
         * 
         * MapDifference<...> diff = MapDifference.of(config1, config2, keysToCheck, configEqual);
         * // Results in:
         * // common: {"timeout": 30, "debug": true}
         * // leftOnly: {}
         * // rightOnly: {}
         * // withDifferentValues: {"retries": Pair.of(3, 5)}
         * // Note: "url" and "proxy" are not compared
         * }</pre>
         *
         * @param <CK> The common key type shared by both maps
         * @param <K1> The key type of the first map (must extend CK)
         * @param <V1> The value type of the first map
         * @param <K2> The key type of the second map (must extend CK)
         * @param <V2> The value type of the second map
         * @param map1 The first map to compare. Can be {@code null} or empty.
         * @param map2 The second map to compare. Can be {@code null} or empty.
         * @param keysToCompare The keys to compare. If {@code null}, all keys will be compared.
         * @param valueEquivalence The predicate to determine if values are equivalent. Must not be {@code null}.
         * @return A {@code MapDifference} object containing the comparison results
         * @throws IllegalArgumentException if {@code valueEquivalence} is {@code null}
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
        public static <CK, K1 extends CK, V1, K2 extends CK, V2> MapDifference<Map<K1, V1>, Map<K2, V2>, Map<CK, Pair<V1, V2>>> of(
                final Map<? extends K1, ? extends V1> map1, final Map<? extends K2, ? extends V2> map2, final Collection<CK> keysToCompare,
                final TriPredicate<? super K1, ? super V1, ? super V2> valueEquivalence) {
            N.checkArgNotNull(valueEquivalence, cs.valueEquivalence);

            final boolean isOrderedMap = (map1 instanceof LinkedHashMap || map1 instanceof SortedMap)
                    || (map2 instanceof LinkedHashMap || map2 instanceof SortedMap);

            final Map<K1, V1> common = isOrderedMap ? new LinkedHashMap<>() : new HashMap<>();
            final Map<K1, V1> leftOnly = isOrderedMap ? new LinkedHashMap<>() : new HashMap<>();
            final Map<K2, V2> rightOnly = isOrderedMap ? new LinkedHashMap<>() : new HashMap<>();
            final Map<CK, Pair<V1, V2>> withDifferentValues = isOrderedMap ? new LinkedHashMap<>() : new HashMap<>();

            if (N.isEmpty(keysToCompare)) {
                if (N.isEmpty(map1)) {
                    if (N.isEmpty(map2)) {
                        // Do nothing. All empty.
                    } else {
                        rightOnly.putAll(map2);
                    }
                } else if (N.isEmpty(map2)) {
                    leftOnly.putAll(map1);
                } else {
                    K1 key1 = null;
                    V1 val1 = null;
                    K2 key2 = null;
                    V2 val2 = null;

                    for (final Entry<K1, V1> entry1 : ((Map<K1, V1>) map1).entrySet()) {
                        key1 = entry1.getKey();
                        val1 = entry1.getValue();
                        //noinspection SuspiciousMethodCalls
                        val2 = map2.get(key1);

                        if (val2 == null) {
                            //noinspection SuspiciousMethodCalls
                            if (map2.containsKey(key1)) {
                                if (entry1.getValue() == null) {
                                    common.put(key1, val1);
                                } else {
                                    //noinspection ConstantValue
                                    withDifferentValues.put(key1, Pair.of(val1, val2));
                                }
                            } else {
                                leftOnly.put(key1, val1);
                            }
                        } else if (valueEquivalence.test(key1, val1, val2)) {
                            common.put(key1, val1);
                        } else {
                            withDifferentValues.put(key1, Pair.of(val1, val2));
                        }
                    }

                    for (final Entry<K2, V2> entry2 : ((Map<K2, V2>) map2).entrySet()) {
                        key2 = entry2.getKey();

                        //noinspection SuspiciousMethodCalls
                        if (common.containsKey(key2) || withDifferentValues.containsKey(key2)) {
                            continue;
                        }

                        rightOnly.put(key2, entry2.getValue());
                    }
                }

            } else {
                if (N.isEmpty(map1)) {
                    if (N.isEmpty(map2)) {
                        // Do nothing. All empty.
                    } else {
                        Maps.putIf(rightOnly, map2, keysToCompare::contains);
                    }
                } else if (N.isEmpty(map2)) {
                    Maps.putIf(leftOnly, map1, keysToCompare::contains);
                } else {
                    K1 key1 = null;
                    V1 val1 = null;
                    K2 key2 = null;
                    V2 val2 = null;

                    for (final Entry<K1, V1> entry1 : ((Map<K1, V1>) map1).entrySet()) {
                        key1 = entry1.getKey();

                        if (!keysToCompare.contains(key1)) {
                            continue;
                        }

                        val1 = entry1.getValue();
                        //noinspection SuspiciousMethodCalls
                        val2 = map2.get(key1);

                        if (val2 == null) {
                            //noinspection SuspiciousMethodCalls
                            if (map2.containsKey(key1)) {
                                if (entry1.getValue() == null) {
                                    common.put(key1, val1);
                                } else {
                                    //noinspection ConstantValue
                                    withDifferentValues.put(key1, Pair.of(val1, val2));
                                }
                            } else {
                                leftOnly.put(key1, val1);
                            }
                        } else if (valueEquivalence.test(key1, val1, val2)) {
                            common.put(key1, val1);
                        } else {
                            withDifferentValues.put(key1, Pair.of(val1, val2));
                        }
                    }

                    for (final Entry<K2, V2> entry2 : ((Map<K2, V2>) map2).entrySet()) {
                        key2 = entry2.getKey();

                        //noinspection SuspiciousMethodCalls
                        if (!keysToCompare.contains(key2) || common.containsKey(key2) || withDifferentValues.containsKey(key2)) {
                            continue;
                        }

                        rightOnly.put(key2, entry2.getValue());
                    }
                }

            }
            return new MapDifference<>(common, leftOnly, rightOnly, withDifferentValues);
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
         * <p>Example:
         * <pre>{@code
         * List<Map<String, Object>> users1 = Arrays.asList(
         *     Map.of("id", 1, "name", "John", "age", 30),
         *     Map.of("id", 2, "name", "Jane", "age", 25)
         * );
         * List<Map<String, Object>> users2 = Arrays.asList(
         *     Map.of("id", 1, "name", "John", "age", 31),  // age changed
         *     Map.of("id", 3, "name", "Bob", "age", 28)
         * );
         * 
         * MapDifference<...> diff = MapDifference.of(users1, users2, map -> map.get("id"));
         * // Results in:
         * // common: [] (empty, no identical maps)
         * // leftOnly: [{"id": 2, "name": "Jane", "age": 25}]
         * // rightOnly: [{"id": 3, "name": "Bob", "age": 28}]
         * // withDifferentValues: {1: MapDifference of the two "John" maps showing age difference}
         * }</pre>
         *
         * @param <CK> The common key type of the maps
         * @param <CV> The common value type of the maps
         * @param <K> The type of the identifier extracted from each map
         * @param a The first collection of maps to compare. Can be {@code null} or empty.
         * @param b The second collection of maps to compare. Can be {@code null} or empty.
         * @param idExtractor Function to extract a unique identifier from each map. Must not be {@code null}.
         * @return A {@code MapDifference} object containing the comparison results
         * @throws IllegalArgumentException if {@code idExtractor} is {@code null}
         * @throws IllegalStateException if duplicate IDs are found within a single collection
         */
        public static <CK, CV, K> MapDifference<List<Map<CK, CV>>, List<Map<CK, CV>>, Map<K, MapDifference<Map<CK, CV>, Map<CK, CV>, Map<CK, Pair<CV, CV>>>>> of(
                final Collection<? extends Map<CK, CV>> a, final Collection<? extends Map<CK, CV>> b,
                final Function<? super Map<CK, CV>, ? extends K> idExtractor) {
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
         * <p>Example:
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
         * MapDifference<...> diff = MapDifference.of(users1, users2, keysToCompare, 
         *                                            map -> map.get("id"));
         * // Results in:
         * // common: [{"id": 1, ...}] (John's record, since name and age match)
         * // leftOnly: []
         * // rightOnly: []
         * // withDifferentValues: {2: MapDifference showing age difference for Jane}
         * }</pre>
         *
         * @param <CK> The common key type of the maps
         * @param <CV> The common value type of the maps
         * @param <K> The type of the identifier extracted from each map
         * @param a The first collection of maps to compare. Can be {@code null} or empty.
         * @param b The second collection of maps to compare. Can be {@code null} or empty.
         * @param keysToCompare The keys to compare within each map. If {@code null}, all keys are compared.
         * @param idExtractor Function to extract a unique identifier from each map. Must not be {@code null}.
         * @return A {@code MapDifference} object containing the comparison results
         * @throws IllegalArgumentException if {@code idExtractor} is {@code null}
         * @throws IllegalStateException if duplicate IDs are found within a single collection
         */
        public static <CK, CV, K> MapDifference<List<Map<CK, CV>>, List<Map<CK, CV>>, Map<K, MapDifference<Map<CK, CV>, Map<CK, CV>, Map<CK, Pair<CV, CV>>>>> of(
                final Collection<? extends Map<CK, CV>> a, final Collection<? extends Map<CK, CV>> b, final Collection<CK> keysToCompare,
                final Function<? super Map<CK, CV>, ? extends K> idExtractor) {
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
         * <p>Example:
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
         * MapDifference<...> diff = MapDifference.of(
         *     oldFormat, newFormat,
         *     map -> map.get("user_id"),  // ID extractor for old format
         *     map -> map.get("id")        // ID extractor for new format
         * );
         * // Results in:
         * // common: [] (empty, different structures)
         * // leftOnly: [{"user_id": 2, "full_name": "Jane Smith"}]
         * // rightOnly: [{"id": 3, "firstName": "Bob", "lastName": "Johnson"}]
         * // withDifferentValues: {1: MapDifference showing structural differences}
         * }</pre>
         *
         * @param <CK> The common key type shared by maps in both collections
         * @param <K1> The key type of maps in the first collection (must extend CK)
         * @param <V1> The value type of maps in the first collection
         * @param <K2> The key type of maps in the second collection (must extend CK)
         * @param <V2> The value type of maps in the second collection
         * @param <K> The type of the identifier used to match maps between collections
         * @param a The first collection of maps to compare. Can be {@code null} or empty.
         * @param b The second collection of maps to compare. Can be {@code null} or empty.
         * @param idExtractor1 Function to extract IDs from maps in the first collection. Must not be {@code null}.
         * @param idExtractor2 Function to extract IDs from maps in the second collection. Must not be {@code null}.
         * @return A {@code MapDifference} object containing the comparison results
         * @throws IllegalArgumentException if either {@code idExtractor1} or {@code idExtractor2} is {@code null}
         * @throws IllegalStateException if duplicate IDs are found within a single collection
         */
        public static <CK, K1 extends CK, V1, K2 extends CK, V2, K> MapDifference<List<Map<K1, V1>>, List<Map<K2, V2>>, Map<K, MapDifference<Map<K1, V1>, Map<K2, V2>, Map<CK, Pair<V1, V2>>>>> of(
                final Collection<? extends Map<K1, V1>> a, final Collection<? extends Map<K2, V2>> b,
                final Function<? super Map<K1, V1>, ? extends K> idExtractor1, final Function<? super Map<K2, V2>, ? extends K> idExtractor2) {

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
         * <p>Example comparing different API versions with selective field comparison:
         * <pre>{@code
         * // Collection A: Version 1 API
         * List<Map<String, Object>> v1Data = Arrays.asList(
         *     Map.of("userId", 1, "userName", "john_doe", "email", "john@old.com", 
         *            "createdAt", "2023-01-01"),
         *     Map.of("userId", 2, "userName", "jane_smith", "email", "jane@old.com", 
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
         * // Only compare username fields (note: different key names in each version)
         * Collection<String> keysToCompare = Arrays.asList("userName", "username");
         * 
         * MapDifference<...> diff = MapDifference.of(
         *     v1Data, v2Data, keysToCompare,
         *     map -> map.get("userId"),  // V1 uses "userId"
         *     map -> map.get("id")       // V2 uses "id"
         * );
         * // Results in:
         * // common: Maps with ID 1 (john_doe) - username matches
         * // leftOnly: Maps with ID 2 (jane_smith)
         * // rightOnly: Maps with ID 3 (bob_jones)
         * // withDifferentValues: {} (empty, since we're only comparing username)
         * }</pre>
         *
         * @param <CK> The common key type shared by maps in both collections
         * @param <K1> The key type of maps in the first collection (must extend CK)
         * @param <V1> The value type of maps in the first collection
         * @param <K2> The key type of maps in the second collection (must extend CK)
         * @param <V2> The value type of maps in the second collection
         * @param <K> The type of the identifier used to match maps between collections
         * @param a The first collection of maps to compare. Can be {@code null} or empty.
         * @param b The second collection of maps to compare. Can be {@code null} or empty.
         * @param keysToCompare Keys to compare within matched maps. If {@code null}, all keys are compared.
         * @param idExtractor1 Function to extract IDs from maps in the first collection. Must not be {@code null}.
         * @param idExtractor2 Function to extract IDs from maps in the second collection. Must not be {@code null}.
         * @return A {@code MapDifference} object containing detailed comparison results
         * @throws IllegalArgumentException if either {@code idExtractor1} or {@code idExtractor2} is {@code null}
         * @throws IllegalStateException if duplicate IDs are found within a single collection
         */
        public static <CK, K1 extends CK, V1, K2 extends CK, V2, K> MapDifference<List<Map<K1, V1>>, List<Map<K2, V2>>, Map<K, MapDifference<Map<K1, V1>, Map<K2, V2>, Map<CK, Pair<V1, V2>>>>> of(
                final Collection<? extends Map<K1, V1>> a, final Collection<? extends Map<K2, V2>> b, final Collection<CK> keysToCompare,
                final Function<? super Map<K1, V1>, ? extends K> idExtractor1, final Function<? super Map<K2, V2>, ? extends K> idExtractor2) {
            N.checkArgNotNull(idExtractor1, cs.idExtractor1);
            N.checkArgNotNull(idExtractor2, cs.idExtractor2);

            final boolean isEmptyPropNamesToCompare = N.isEmpty(keysToCompare);

            final List<Map<K1, V1>> common = new ArrayList<>();
            final List<Map<K1, V1>> leftOnly = new ArrayList<>();
            final List<Map<K2, V2>> rightOnly = new ArrayList<>();
            final Map<K, MapDifference<Map<K1, V1>, Map<K2, V2>, Map<CK, Pair<V1, V2>>>> withDifferentValues = new LinkedHashMap<>();

            if (N.isEmpty(a)) {
                if (N.isEmpty(b)) {
                    // Do nothing. All empty.
                } else {
                    rightOnly.addAll(b);
                }
            } else if (N.isEmpty(b)) {
                leftOnly.addAll(a);
            } else {
                final Map<K, Map<? extends K1, ? extends V1>> beanMapA = N.toMap(a, idExtractor1, Fn.identity(), Fn.throwingMerger(),
                        IntFunctions.ofLinkedHashMap());
                final Map<K, Map<? extends K2, ? extends V2>> beanMapB = N.toMap(b, idExtractor2, Fn.identity(), Fn.throwingMerger(),
                        IntFunctions.ofLinkedHashMap());

                Map<K1, V1> mapA = null;
                Map<K2, V2> mapB = null;
                boolean areEqual = false;

                for (final Map.Entry<K, Map<? extends K1, ? extends V1>> entry : beanMapA.entrySet()) {
                    mapA = (Map<K1, V1>) entry.getValue();

                    if (beanMapB.containsKey(entry.getKey())) {
                        mapB = (Map<K2, V2>) beanMapB.get(entry.getKey());
                        areEqual = isEmptyPropNamesToCompare ? N.equals(mapA, mapB) : N.equalsByKeys(mapA, mapB, keysToCompare);

                        if (areEqual) {
                            common.add(mapA);
                        } else {
                            withDifferentValues.put(entry.getKey(), MapDifference.of(mapA, mapB, keysToCompare));
                        }
                    } else {
                        leftOnly.add(mapA);
                    }
                }

                for (final Map.Entry<K, Map<? extends K2, ? extends V2>> entry : beanMapB.entrySet()) {
                    if (!beanMapA.containsKey(entry.getKey())) {
                        rightOnly.add((Map<K2, V2>) entry.getValue());
                    }
                }
            }

            return new MapDifference<>(common, leftOnly, rightOnly, withDifferentValues);
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
     *   <li>When comparing all properties, null values in both beans are ignored</li>
     *   <li>When comparing specific properties, null values are included in the comparison</li>
     * </ul>
     * 
     * <p>Example usage:
     * <pre>{@code
     * class Person {
     *     private String name;
     *     private int age;
     *     private String email;
     *     // getters and setters...
     * }
     * 
     * Person person1 = new Person("John", 30, "john@old.com");
     * Person person2 = new Person("John", 31, "john@new.com");
     * 
     * BeanDifference<...> diff = BeanDifference.of(person1, person2);
     * // diff.inCommon() returns {"name": "John"}
     * // diff.onLeftOnly() returns {} (empty)
     * // diff.onRightOnly() returns {} (empty)
     * // diff.withDifferentValues() returns {"age": Pair.of(30, 31), "email": Pair.of("john@old.com", "john@new.com")}
     * }</pre>
     *
     * @param <L> The type of the map representing properties from the left (first) bean
     * @param <R> The type of the map representing properties from the right (second) bean
     * @param <D> The type of the map containing properties with different values
     * @see KeyValueDifference
     * @see com.landawn.abacus.annotation.DiffIgnore
     * @see Maps#difference(Map, Map)
     * @see Maps#symmetricDifference(Map, Map)
     * @see N#difference(Collection, Collection)
     */
    public static final class BeanDifference<L, R, D> extends KeyValueDifference<L, R, D> {
        /**
         * Instantiates a new {@code BeanDifference}.
         *
         * @param common
         * @param leftOnly
         * @param rightOnly
         * @param withDifferentValues
         */
        BeanDifference(final L common, final L leftOnly, final R rightOnly, final D withDifferentValues) {
            super(common, leftOnly, rightOnly, withDifferentValues);
        }

        /**
         * Compares two beans and identifies the differences between them using default equality comparison.
         * <p>
         * This method creates a {@code BeanDifference} object that contains:
         * <ul>
         *   <li>Common properties: Properties present in both beans with equal values (using {@code equals})</li>
         *   <li>Left-only properties: Properties present only in the first bean</li>
         *   <li>Right-only properties: Properties present only in the second bean</li>
         *   <li>Different values: Properties present in both beans but with different values</li>
         * </ul>
         * 
         * <p>Special behavior:
         * <ul>
         *   <li>Properties annotated with {@code @DiffIgnore} are excluded from comparison</li>
         *   <li>Properties where both beans have {@code null} values are not included in the results</li>
         *   <li>The beans can be of different classes; only properties with matching names are compared</li>
         *   <li>Property values are compared using the standard {@code equals} method</li>
         * </ul>
         * 
         * <p>Example:
         * <pre>{@code
         * class User {
         *     private String name;
         *     private String email;
         *     @DiffIgnore
         *     private Date lastModified;
         *     // getters and setters...
         * }
         * 
         * User user1 = new User("John", "john@old.com");
         * User user2 = new User("John", "john@new.com");
         * 
         * BeanDifference<...> diff = BeanDifference.of(user1, user2);
         * // Results in:
         * // common: {"name": "John"}
         * // leftOnly: {}
         * // rightOnly: {}
         * // withDifferentValues: {"email": Pair.of("john@old.com", "john@new.com")}
         * // Note: lastModified is ignored due to @DiffIgnore
         * }</pre>
         *
         * @param bean1 The first bean to compare. Can be {@code null}.
         * @param bean2 The second bean to compare. Can be {@code null}.
         * @return A {@code BeanDifference} object containing the comparison results
         * @throws IllegalArgumentException if either bean is not a valid bean class (i.e., is a primitive, array, collection, map, etc.)
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
        public static BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> of(final Object bean1, final Object bean2) {
            return of(bean1, bean2, null, (k, v1, v2) -> N.equals(v1, v2));
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
         *   <li>If a specified property doesn't exist in a bean, it's treated as a missing property</li>
         * </ul>
         * 
         * <p>Example:
         * <pre>{@code
         * class Employee {
         *     private String id;
         *     private String name;
         *     private Double salary;
         *     private String department;
         *     // getters and setters...
         * }
         * 
         * Employee emp1 = new Employee("E001", "John", 50000.0, "Sales");
         * Employee emp2 = new Employee("E001", "John", 55000.0, "Marketing");
         * 
         * // Compare only id and name
         * Collection<String> propsToCompare = Arrays.asList("id", "name");
         * BeanDifference<...> diff = BeanDifference.of(emp1, emp2, propsToCompare);
         * // Results in:
         * // common: {"id": "E001", "name": "John"}
         * // leftOnly: {}
         * // rightOnly: {}
         * // withDifferentValues: {}
         * // Note: salary and department differences are ignored
         * }</pre>
         *
         * @param bean1 The first bean to compare. Can be {@code null}.
         * @param bean2 The second bean to compare. Can be {@code null}.
         * @param propNamesToCompare The property names to compare. If {@code null} or empty, all properties are compared.
         * @return A {@code BeanDifference} object containing the comparison results for the specified properties
         * @throws IllegalArgumentException if either bean is not a valid bean class
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
                final Collection<String> propNamesToCompare) {
            return of(bean1, bean2, propNamesToCompare, (k, v1, v2) -> N.equals(v1, v2));
        }

        /**
         * Compares two beans using a custom value equivalence predicate.
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
         * <p>Example:
         * <pre>{@code
         * class Product {
         *     private String name;
         *     private Double price;
         *     private String description;
         *     // getters and setters...
         * }
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
         * BeanDifference<...> diff = BeanDifference.of(p1, p2, fuzzyEquals);
         * // Results in:
         * // common: {"name": "Widget", "price": 10.99, "description": "A useful widget"}
         * // All properties are considered equal due to custom comparison
         * }</pre>
         *
         * @param bean1 The first bean to compare. Can be {@code null}.
         * @param bean2 The second bean to compare. Can be {@code null}.
         * @param valueEquivalence The predicate to determine if two property values are equivalent. Must not be {@code null}.
         * @return A {@code BeanDifference} object containing the comparison results
         * @throws IllegalArgumentException if either bean is not a valid bean class, or if {@code valueEquivalence} is {@code null}
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
        public static BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> of(final Object bean1, final Object bean2,
                final BiPredicate<?, ?> valueEquivalence) {
            N.checkArgNotNull(valueEquivalence, cs.valueEquivalence);

            final BiPredicate<Object, Object> valueEquivalenceToUse = (BiPredicate<Object, Object>) valueEquivalence;

            return of(bean1, bean2, null, (k, v1, v2) -> valueEquivalenceToUse.test(v1, v2));
        }

        /**
         * Compares two beans using a property-aware custom value equivalence predicate.
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
         * <p>Example:
         * <pre>{@code
         * class Account {
         *     private String accountId;
         *     private Double balance;
         *     private Date lastActivity;
         *     private String status;
         *     // getters and setters...
         * }
         * 
         * Account acc1 = new Account("ACC001", 1000.001, date1, "ACTIVE");
         * Account acc2 = new Account("ACC001", 1000.0, date2, "active");
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
         * BeanDifference<...> diff = BeanDifference.of(acc1, acc2, smartEquals);
         * // Results may show all properties as common if they meet the criteria
         * }</pre>
         *
         * @param bean1 The first bean to compare. Can be {@code null}.
         * @param bean2 The second bean to compare. Can be {@code null}.
         * @param valueEquivalence The predicate to determine if values are equivalent for a given property. 
         *                         Receives (propertyName, value1, value2). Must not be {@code null}.
         * @return A {@code BeanDifference} object containing the comparison results
         * @throws IllegalArgumentException if either bean is not a valid bean class, or if {@code valueEquivalence} is {@code null}
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
        public static BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> of(final Object bean1, final Object bean2,
                final TriPredicate<String, ?, ?> valueEquivalence) {
            return of(bean1, bean2, null, valueEquivalence);
        }

        /**
         * Compares two beans for specified properties using a property-aware custom value equivalence predicate.
         * <p>
         * This method combines selective property comparison with custom equivalence logic that can
         * vary by property name. It provides maximum control over the comparison process.
         * 
         * <p>Behavior:
         * <ul>
         *   <li>Only properties in {@code propNamesToCompare} are examined</li>
         *   <li>The {@code valueEquivalence} predicate determines equality for each property</li>
         *   <li>{@code null} values in both beans are included in comparison (unlike the default behavior)</li>
         *   <li>{@code @DiffIgnore} annotations are ignored when specific properties are requested</li>
         * </ul>
         * 
         * <p>Example:
         * <pre>{@code
         * class Customer {
         *     private String id;
         *     private String name;
         *     private String email;
         *     private Double creditLimit;
         *     private String notes;
         *     // getters and setters...
         * }
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
         * BeanDifference<...> diff = BeanDifference.of(c1, c2, fieldsToCheck, fieldEquals);
         * // Results in:
         * // common: {"name": "John Doe", "creditLimit": 5000.0}
         * // withDifferentValues: {"email": Pair.of("john@old.com", "john@new.com")}
         * // Note: id and notes are not compared
         * }</pre>
         *
         * @param bean1 The first bean to compare. Can be {@code null}.
         * @param bean2 The second bean to compare. Can be {@code null}.
         * @param propNamesToCompare The property names to compare. If {@code null} or empty, all properties are compared.
         * @param valueEquivalence The predicate to determine if values are equivalent for a given property. Must not be {@code null}.
         * @return A {@code BeanDifference} object containing the comparison results
         * @throws IllegalArgumentException if either bean is not a valid bean class, or if {@code valueEquivalence} is {@code null}
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
        public static BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> of(final Object bean1, final Object bean2,
                final Collection<String> propNamesToCompare, final TriPredicate<String, ?, ?> valueEquivalence) {
            N.checkArgNotNull(valueEquivalence, cs.valueEquivalence);

            if (bean1 != null && !ClassUtil.isBeanClass(bean1.getClass())) {
                throw new IllegalArgumentException(bean1.getClass().getCanonicalName() + " is not a bean class"); // NOSONAR
            }

            if (bean2 != null && !ClassUtil.isBeanClass(bean2.getClass())) {
                throw new IllegalArgumentException(bean2.getClass().getCanonicalName() + " is not a bean class"); // NOSONAR
            }

            N.checkArgNotNull(valueEquivalence, cs.valueEquivalence);

            final TriPredicate<String, Object, Object> valueEquivalenceToUse = (TriPredicate<String, Object, Object>) valueEquivalence;
            final Map<String, Object> common = new LinkedHashMap<>();
            final Map<String, Object> leftOnly = new LinkedHashMap<>();
            final Map<String, Object> rightOnly = new LinkedHashMap<>();
            final Map<String, Pair<Object, Object>> withDifferentValues = new LinkedHashMap<>();

            if (N.isEmpty(propNamesToCompare)) {
                if (bean1 == null) {
                    if (bean2 == null) {
                        // Do nothing. All empty.
                    } else {
                        Maps.bean2Map(bean2, true, ClassUtil.getDiffIgnoredPropNames(bean2.getClass()), rightOnly);
                    }
                } else if (bean2 == null) {
                    Maps.bean2Map(bean1, true, ClassUtil.getDiffIgnoredPropNames(bean1.getClass()), leftOnly);
                } else {
                    final Class<?> bean1Class = bean1.getClass();
                    final Class<?> bean2Class = bean2.getClass();
                    final Set<String> ignoredPropNamesForNullValues = new HashSet<>();
                    final ImmutableSet<String> diffIgnoredPropNamesForBean1 = ClassUtil.getDiffIgnoredPropNames(bean1Class);
                    final ImmutableSet<String> diffIgnoredPropNamesForBean2 = ClassUtil.getDiffIgnoredPropNames(bean2Class);
                    final BeanInfo beanInfo1 = ParserUtil.getBeanInfo(bean1Class);
                    final BeanInfo beanInfo2 = ParserUtil.getBeanInfo(bean2Class);
                    Object val1 = null;
                    Object val2 = null;

                    {
                        PropInfo propInfo2 = null;

                        for (final PropInfo propInfo1 : beanInfo1.propInfoList) {
                            if (!diffIgnoredPropNamesForBean1.isEmpty() && diffIgnoredPropNamesForBean1.contains(propInfo1.name)) {
                                continue;
                            }

                            val1 = propInfo1.getPropValue(bean1);
                            propInfo2 = beanInfo2.getPropInfo(propInfo1.name);

                            if (propInfo2 == null) {
                                leftOnly.put(propInfo1.name, val1);
                            } else {
                                val2 = propInfo2.getPropValue(bean2);

                                if (val2 == null && val1 == null) {
                                    ignoredPropNamesForNullValues.add(propInfo1.name);
                                    continue; // ignore null value comparison.
                                }

                                if (valueEquivalenceToUse.test(propInfo1.name, val1, val2)) {
                                    common.put(propInfo1.name, val1);
                                } else {
                                    withDifferentValues.put(propInfo1.name, Pair.of(val1, val2));
                                }
                            }
                        }
                    }

                    for (final PropInfo propInfo : beanInfo2.propInfoList) {
                        if (!diffIgnoredPropNamesForBean2.isEmpty() && diffIgnoredPropNamesForBean2.contains(propInfo.name)) {
                            continue;
                        }

                        if (ignoredPropNamesForNullValues.contains(propInfo.name) || common.containsKey(propInfo.name)
                                || withDifferentValues.containsKey(propInfo.name)) {
                            continue;
                        }

                        rightOnly.put(propInfo.name, propInfo.getPropValue(bean2));
                    }
                }

            } else {
                if (bean1 == null) {
                    if (bean2 == null) {
                        // Do nothing. All empty.
                    } else {
                        Maps.bean2Map(bean2, propNamesToCompare, rightOnly);
                    }
                } else if (bean2 == null) {
                    Maps.bean2Map(bean1, propNamesToCompare, leftOnly);
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
                            leftOnly.put(propName, val1);
                        } else {
                            val2 = propInfo2.getPropValue(bean2);

                            if ((val2 == null && val1 == null) // Compare all specified properties even values are both null
                                    || valueEquivalenceToUse.test(propName, val1, val2)) {
                                common.put(propName, val1);
                            } else {
                                withDifferentValues.put(propName, Pair.of(val1, val2));
                            }
                        }
                    }

                    for (String propName : propNamesToCompare) {
                        propInfo2 = beanInfo2.getPropInfo(propName);

                        if (propInfo2 == null || common.containsKey(propName) || withDifferentValues.containsKey(propName)) {
                            continue;
                        }

                        rightOnly.put(propName, propInfo2.getPropValue(bean2));
                    }
                }

            }

            return new BeanDifference<>(common, leftOnly, rightOnly, withDifferentValues);
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
         *   <li>Beans with matching identifiers are compared using {@link #of(Object, Object)}</li>
         *   <li>All properties are compared (except those marked with {@code @DiffIgnore})</li>
         *   <li>Results are categorized into common, left-only, right-only, and different beans</li>
         * </ol>
         * 
         * <p>Example:
         * <pre>{@code
         * class Employee {
         *     private String id;
         *     private String name;
         *     private String department;
         *     // getters and setters...
         * }
         * 
         * List<Employee> team1 = Arrays.asList(
         *     new Employee("E001", "John", "Sales"),
         *     new Employee("E002", "Jane", "Marketing")
         * );
         * List<Employee> team2 = Arrays.asList(
         *     new Employee("E001", "John", "Engineering"),  // Different department
         *     new Employee("E003", "Bob", "Sales")
         * );
         * 
         * BeanDifference<...> diff = BeanDifference.of(team1, team2, emp -> emp.getId());
         * // Results in:
         * // common: [] (empty, as E001 has different department)
         * // leftOnly: [Employee E002]
         * // rightOnly: [Employee E003]
         * // withDifferentValues: {"E001": BeanDifference showing department change}
         * }</pre>
         *
         * @param <T> The type of beans in both collections
         * @param <C> The type of the list containing beans (typically List<T>)
         * @param <K> The type of the identifier used to match beans between collections
         * @param a The first collection of beans to compare. Can be {@code null} or empty.
         * @param b The second collection of beans to compare. Can be {@code null} or empty.
         * @param idExtractor The function to extract a unique identifier from each bean. Must not be {@code null}.
         * @return A {@code BeanDifference} object containing the comparison results
         * @throws IllegalArgumentException if {@code idExtractor} is {@code null} or if the collections contain non-bean objects
         */
        public static <T, C extends List<T>, K> BeanDifference<C, C, Map<K, BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>>> of(
                final Collection<? extends T> a, final Collection<? extends T> b, final Function<? super T, K> idExtractor) {
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
         * <p>Example:
         * <pre>{@code
         * class Product {
         *     private String sku;
         *     private String name;
         *     private Double price;
         *     private Date lastModified;
         *     private String internalNotes;
         *     // getters and setters...
         * }
         * 
         * List<Product> catalog1 = Arrays.asList(
         *     new Product("SKU001", "Widget", 19.99, date1, "Check supplier"),
         *     new Product("SKU002", "Gadget", 29.99, date2, "Bestseller")
         * );
         * List<Product> catalog2 = Arrays.asList(
         *     new Product("SKU001", "Widget", 21.99, date3, "New supplier"),
         *     new Product("SKU003", "Tool", 39.99, date4, "New item")
         * );
         * 
         * // Compare only name and price, ignoring dates and notes
         * Collection<String> propsToCompare = Arrays.asList("name", "price");
         * BeanDifference<...> diff = BeanDifference.of(
         *     catalog1, catalog2, propsToCompare, 
         *     product -> product.getSku()
         * );
         * // Results show price difference for SKU001, ignoring other field changes
         * }</pre>
         *
         * @param <T> The type of beans in both collections
         * @param <C> The type of the list containing beans (typically List<T>)
         * @param <K> The type of the identifier used to match beans between collections
         * @param a The first collection of beans to compare. Can be {@code null} or empty.
         * @param b The second collection of beans to compare. Can be {@code null} or empty.
         * @param propNamesToCompare The property names to compare. If {@code null} or empty, all properties are compared.
         * @param idExtractor The function to extract a unique identifier from each bean. Must not be {@code null}.
         * @return A {@code BeanDifference} object containing the comparison results
         * @throws IllegalArgumentException if {@code idExtractor} is {@code null} or if the collections contain non-bean objects
         */
        public static <T, C extends List<T>, K> BeanDifference<C, C, Map<K, BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>>> of(
                final Collection<? extends T> a, final Collection<? extends T> b, final Collection<String> propNamesToCompare,
                final Function<? super T, K> idExtractor) {
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
         * <p>Example:
         * <pre>{@code
         * class CustomerV1 {
         *     private Long customerId;
         *     private String fullName;
         *     private String emailAddress;
         *     // getters and setters...
         * }
         * 
         * class CustomerV2 {
         *     private String id;  // Changed from Long to String
         *     private String fullName;  // Same property name
         *     private String email;  // Renamed from emailAddress
         *     private String phoneNumber;  // New field
         *     // getters and setters...
         * }
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
         * BeanDifference<...> diff = BeanDifference.of(
         *     oldCustomers, newCustomers,
         *     v1 -> v1.getCustomerId().toString(),  // Convert Long to String
         *     v2 -> v2.getId()
         * );
         * // Results show:
         * // - Customer 101 has matching fullName but missing email/phoneNumber mappings
         * // - Customer 102 exists only in old system
         * // - Customer 103 exists only in new system
         * }</pre>
         *
         * @param <T1> The type of beans in the first collection
         * @param <T2> The type of beans in the second collection
         * @param <L> The type of the list containing beans from the first collection
         * @param <R> The type of the list containing beans from the second collection
         * @param <K> The type of the identifier used to match beans between collections
         * @param a The first collection of beans to compare. Can be {@code null} or empty.
         * @param b The second collection of beans to compare. Can be {@code null} or empty.
         * @param idExtractor1 The function to extract identifiers from beans in the first collection. Must not be {@code null}.
         * @param idExtractor2 The function to extract identifiers from beans in the second collection. Must not be {@code null}.
         * @return A {@code BeanDifference} object containing the comparison results
         * @throws IllegalArgumentException if either {@code idExtractor1} or {@code idExtractor2} is {@code null}, 
         *         or if the collections contain non-bean objects
         */
        public static <T1, T2, L extends List<T1>, R extends List<T2>, K> BeanDifference<L, R, Map<K, BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>>> of(
                final Collection<? extends T1> a, final Collection<? extends T2> b, final Function<? super T1, ? extends K> idExtractor1,
                final Function<? super T2, ? extends K> idExtractor2) {
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
         * <p>Example:
         * <pre>{@code
         * class Order {
         *     private String orderId;
         *     private String customerName;
         *     private Double totalAmount;
         *     private String status;
         *     private Date orderDate;
         *     private String notes;
         *     // getters and setters...
         * }
         * 
         * class OrderSummary {
         *     private Long id;
         *     private String customer;  // Maps to customerName
         *     private BigDecimal total;  // Maps to totalAmount
         *     private String status;
         *     private LocalDate date;    // Different date type
         *     private Map<String, Object> metadata;
         *     // getters and setters...
         * }
         * 
         * List<Order> orders = Arrays.asList(
         *     new Order("ORD-001", "John Doe", 150.00, "SHIPPED", date1, "Rush delivery"),
         *     new Order("ORD-002", "Jane Smith", 75.50, "PENDING", date2, "Gift wrap")
         * );
         * 
         * List<OrderSummary> summaries = Arrays.asList(
         *     new OrderSummary(1L, "John Doe", new BigDecimal("150.00"), "DELIVERED", localDate1, metadata1),
         *     new OrderSummary(3L, "Bob Johnson", new BigDecimal("200.00"), "PENDING", localDate3, metadata3)
         * );
         * 
         * // Compare only status field (the only field with matching names)
         * Collection<String> propsToCompare = Arrays.asList("status");
         * 
         * BeanDifference<...> diff = BeanDifference.of(
         *     orders, summaries, propsToCompare,
         *     order -> order.getOrderId().substring(4),  // Extract numeric part
         *     summary -> summary.getId().toString()
         * );
         * // Compares only the "status" property for matched orders
         * }</pre>
         *
         * @param <T1> The type of beans in the first collection
         * @param <T2> The type of beans in the second collection
         * @param <L> The type of the list containing beans from the first collection
         * @param <R> The type of the list containing beans from the second collection
         * @param <K> The type of the identifier used to match beans between collections
         * @param a The first collection of beans to compare. Can be {@code null} or empty.
         * @param b The second collection of beans to compare. Can be {@code null} or empty.
         * @param propNamesToCompare The property names to compare. If {@code null} or empty, all matching properties are compared.
         * @param idExtractor1 The function to extract identifiers from beans in the first collection. Must not be {@code null}.
         * @param idExtractor2 The function to extract identifiers from beans in the second collection. Must not be {@code null}.
         * @return A {@code BeanDifference} object containing the comparison results
         * @throws IllegalArgumentException if either {@code idExtractor1} or {@code idExtractor2} is {@code null},
         *         or if the collections contain non-bean objects
         */
        public static <T1, T2, L extends List<T1>, R extends List<T2>, K> BeanDifference<L, R, Map<K, BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>>> of(
                final Collection<? extends T1> a, final Collection<? extends T2> b, final Collection<String> propNamesToCompare,
                final Function<? super T1, ? extends K> idExtractor1, final Function<? super T2, ? extends K> idExtractor2) {
            N.checkArgNotNull(idExtractor1, cs.idExtractor1);
            N.checkArgNotNull(idExtractor2, cs.idExtractor2);

            final Class<T1> clsA = N.isEmpty(a) ? null : (Class<T1>) N.firstOrNullIfEmpty(a).getClass();
            final Class<T2> clsB = N.isEmpty(b) ? null : (Class<T2>) N.firstOrNullIfEmpty(b).getClass();

            if (clsA != null && !ClassUtil.isBeanClass(clsA)) {
                throw new IllegalArgumentException(clsA.getCanonicalName() + " is not a bean class"); // NOSONAR
            }

            if (clsB != null && !ClassUtil.isBeanClass(clsB)) {
                throw new IllegalArgumentException(clsB.getCanonicalName() + " is not a bean class"); // NOSONAR
            }

            final boolean isEmptyPropNamesToCompare = N.isEmpty(propNamesToCompare);

            final List<T1> common = new ArrayList<>();
            final List<T1> leftOnly = new ArrayList<>();
            final List<T2> rightOnly = new ArrayList<>();
            final Map<K, BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>> withDifferentValues = new LinkedHashMap<>();

            if (N.isEmpty(a)) {
                if (N.isEmpty(b)) {
                    // Do nothing. All empty.
                } else {
                    rightOnly.addAll(b);
                }
            } else if (N.isEmpty(b)) {
                leftOnly.addAll(a);
            } else {
                final Map<K, T1> beanMapA = N.toMap(a, idExtractor1, Fn.identity(), Fn.throwingMerger(), IntFunctions.ofLinkedHashMap());
                final Map<K, T2> beanMapB = N.toMap(b, idExtractor2, Fn.identity(), Fn.throwingMerger(), IntFunctions.ofLinkedHashMap());
                T1 beanA = null;
                T2 beanB = null;
                boolean areEqual = false;

                for (final Map.Entry<K, T1> entry : beanMapA.entrySet()) {
                    beanA = entry.getValue();

                    if (beanMapB.containsKey(entry.getKey())) {
                        beanB = beanMapB.get(entry.getKey());
                        areEqual = isEmptyPropNamesToCompare ? N.equals(beanA, beanB) : N.equalsByProps(beanA, beanB, propNamesToCompare);

                        if (areEqual) {
                            common.add(beanA);
                        } else {
                            withDifferentValues.put(entry.getKey(), BeanDifference.of(beanA, beanB, propNamesToCompare));
                        }
                    } else {
                        leftOnly.add(beanA);
                    }
                }

                for (final Map.Entry<K, T2> entry : beanMapB.entrySet()) {
                    if (!beanMapA.containsKey(entry.getKey())) {
                        rightOnly.add(entry.getValue());
                    }
                }
            }

            return new BeanDifference<>((L) common, (L) leftOnly, (R) rightOnly, withDifferentValues);

        }
    }
}
