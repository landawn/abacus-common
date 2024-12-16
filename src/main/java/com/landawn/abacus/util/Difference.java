/*
 * Copyright (C) 2016 HaiYang Li
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiPredicate;

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.function.TriPredicate;

/**
 * It is used to compare two collections and find the common elements, elements only in the left collection, and elements only in the right collection. Occurrences are considered.
 *
 * @param <L>
 * @param <R>
 * @see N#difference(Collection, Collection)
 * @see N#symmetricDifference(Collection, Collection)
 * @see N#excludeAll(Collection, Collection)
 * @see N#excludeAllToSet(Collection, Collection)
 * @see N#removeAll(Collection, Iterable)
 * @see N#intersection(Collection, Collection)
 * @see N#commonSet(Collection, Collection)
 */
public class Difference<L, R> {

    final L common;

    final L leftOnly;

    final R rightOnly;

    Difference(final L common, final L leftOnly, final R rightOnly) {
        this.common = common;
        this.leftOnly = leftOnly;
        this.rightOnly = rightOnly;
    }

    /**
     * Compares two boolean arrays and finds the common elements, elements only in the first array, and elements only in the second array.
     *
     * @param a The first boolean array to be compared.
     * @param b The second boolean array to be compared.
     * @return A Difference object containing the common elements, elements only in the first array, and elements only in the second array.
     * @see IntList#difference(IntList)
     * @see N#difference(Collection, Collection)
     */
    public static Difference<BooleanList, BooleanList> of(final boolean[] a, final boolean[] b) {
        return of(BooleanList.of(a), BooleanList.of(b));
    }

    /**
     * Compares two char arrays and finds the common elements, elements only in the first array, and elements only in the second array.
     *
     * @param a The first char array to be compared.
     * @param b The second char array to be compared.
     * @return A Difference object containing the common elements, elements only in the first array, and elements only in the second array.
     * @see IntList#difference(IntList)
     * @see N#difference(Collection, Collection)
     */
    public static Difference<CharList, CharList> of(final char[] a, final char[] b) {
        return of(CharList.of(a), CharList.of(b));
    }

    /**
     * Compares two byte arrays and finds the common elements, elements only in the first array, and elements only in the second array.
     *
     * @param a The first byte array to be compared.
     * @param b The second byte array to be compared.
     * @return A Difference object containing the common elements, elements only in the first array, and elements only in the second array.
     * @see IntList#difference(IntList)
     * @see N#difference(Collection, Collection)
     */
    public static Difference<ByteList, ByteList> of(final byte[] a, final byte[] b) {
        return of(ByteList.of(a), ByteList.of(b));
    }

    /**
     * Compares two short arrays and finds the common elements, elements only in the first array, and elements only in the second array.
     *
     * @param a The first short array to be compared.
     * @param b The second short array to be compared.
     * @return A Difference object containing the common elements, elements only in the first array, and elements only in the second array.
     * @see IntList#difference(IntList)
     * @see N#difference(Collection, Collection)
     */
    public static Difference<ShortList, ShortList> of(final short[] a, final short[] b) {
        return of(ShortList.of(a), ShortList.of(b));
    }

    /**
     * Compares two int arrays and finds the common elements, elements only in the first array, and elements only in the second array.
     *
     * @param a The first int array to be compared.
     * @param b The second int array to be compared.
     * @return A Difference object containing the common elements, elements only in the first array, and elements only in the second array.
     * @see IntList#difference(IntList)
     * @see N#difference(Collection, Collection)
     */
    public static Difference<IntList, IntList> of(final int[] a, final int[] b) {
        return of(IntList.of(a), IntList.of(b));
    }

    /**
     * Compares two long arrays and finds the common elements, elements only in the first array, and elements only in the second array.
     *
     * @param a The first long array to be compared.
     * @param b The second long array to be compared.
     * @return A Difference object containing the common elements, elements only in the first array, and elements only in the second array.
     * @see IntList#difference(IntList)
     * @see N#difference(Collection, Collection)
     */
    public static Difference<LongList, LongList> of(final long[] a, final long[] b) {
        return of(LongList.of(a), LongList.of(b));
    }

    /**
     * Compares two float arrays and finds the common elements, elements only in the first array, and elements only in the second array.
     *
     * @param a The first float array to be compared.
     * @param b The second float array to be compared.
     * @return A Difference object containing the common elements, elements only in the first array, and elements only in the second array.
     * @see IntList#difference(IntList)
     * @see N#difference(Collection, Collection)
     */
    public static Difference<FloatList, FloatList> of(final float[] a, final float[] b) {
        return of(FloatList.of(a), FloatList.of(b));
    }

    /**
     * Compares two double arrays and finds the common elements, elements only in the first array, and elements only in the second array.
     *
     * @param a The first double array to be compared.
     * @param b The second double array to be compared.
     * @return A Difference object containing the common elements, elements only in the first array, and elements only in the second array.
     * @see IntList#difference(IntList)
     * @see N#difference(Collection, Collection)
     */
    public static Difference<DoubleList, DoubleList> of(final double[] a, final double[] b) {
        return of(DoubleList.of(a), DoubleList.of(b));
    }

    /**
     * Compares two arrays and finds the common elements, elements only in the first array, and elements only in the second array.
     *
     * @param <T1> The type of elements in the first array.
     * @param <T2> The type of elements in the second array.
     * @param <L> The type of List that contains elements of type T1.
     * @param <R> The type of List that contains elements of type T2.
     * @param a The first array to be compared.
     * @param b The second array to be compared.
     * @return A Difference object containing the common elements, elements only in the first array, and elements only in the second array.
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
     * Compares two collections and finds the common elements, elements only in the first collection, and elements only in the second collection.
     *
     * @param <T1> The type of elements in the first collection.
     * @param <T2> The type of elements in the second collection.
     * @param <L> The type of List that contains elements of type T1.
     * @param <R> The type of List that contains elements of type T2.
     * @param a The first collection to be compared.
     * @param b The second collection to be compared.
     * @return A Difference object containing the common elements, elements only in the first collection, and elements only in the second collection.
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
     * Compares two BooleanLists and finds the common elements, elements only in the first list, and elements only in the second list.
     *
     * @param a The first BooleanList to be compared.
     * @param b The second BooleanList to be compared.
     * @return A Difference object containing the common elements, elements only in the first list, and elements only in the second list.
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
     * Compares two CharLists and finds the common elements, elements only in the first list, and elements only in the second list.
     *
     * @param a The first CharList to be compared.
     * @param b The second CharList to be compared.
     * @return A Difference object containing the common elements, elements only in the first list, and elements only in the second list.
     * @return
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
     * Compares two ByteLists and finds the common elements, elements only in the first list, and elements only in the second list.
     *
     * @param a The first ByteList to be compared.
     * @param b The second ByteList to be compared.
     * @return
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
     * Compares two ShortLists and finds the common elements, elements only in the first list, and elements only in the second list.
     *
     * @param a The first ShortList to be compared.
     * @param b The second ShortList to be compared.
     * @return
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
     * Compares two IntLists and finds the common elements, elements only in the first list, and elements only in the second list.
     *
     * @param a The first IntList to be compared.
     * @param b The second IntList to be compared.
     * @return
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
     * Compares two LongLists and finds the common elements, elements only in the first list, and elements only in the second list.
     *
     * @param a The first LongList to be compared.
     * @param b The second LongList to be compared.
     * @return
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
     * Compares two FloatLists and finds the common elements, elements only in the first list, and elements only in the second list.
     *
     * @param a The first FloatList to be compared.
     * @param b The second FloatList to be compared.
     * @return
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
     * Compares two DoubleLists and finds the common elements, elements only in the first list, and elements only in the second list.
     *
     * @param a The first DoubleList to be compared.
     * @param b The second DoubleList to be compared.
     * @return
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
     * Returns the common elements found in both collections.
     *
     * @return the common elements
     */
    public L inCommon() {
        return common;
    }

    /**
     * Returns the elements that are only in the left collection.
     *
     * @return the elements only in the left collection
     */
    public L onLeftOnly() {
        return leftOnly;
    }

    /**
     * Returns the elements that are only in the right collection.
     *
     * @return the elements only in the right collection
     */
    public R onRightOnly() {
        return rightOnly;
    }

    /**
     * Checks if the two input collections have the exact same elements Occurrences matter.
     *
     * @return {@code true} if the two input collections have the exact same elements, {@code false} otherwise
     */
    @SuppressWarnings("rawtypes")
    public boolean areEqual() {
        return (leftOnly instanceof Collection && (((Collection) leftOnly).isEmpty() && ((Collection) rightOnly).isEmpty()))
                || (leftOnly instanceof Map && (((Map) leftOnly).isEmpty() && ((Map) rightOnly).isEmpty()));
    }

    @Override
    public String toString() {
        return "{inCommon=" + common + ", onLeftOnly=" + leftOnly + ", onRightOnly=" + rightOnly + "}";
    }

    /**
     * It is used to compare two maps and find the common elements, elements only in the left map, elements only in the right map, and elements with different values.
     *
     * @param <L>
     * @param <R>
     * @param <D>
     *
     * @see IntList#difference(IntList)
     * @see N#difference(Collection, Collection)
     * @see N#symmetricDifference(Collection, Collection)
     * @see N#excludeAll(Collection, Collection)
     * @see N#excludeAllToSet(Collection, Collection)
     * @see N#removeAll(Collection, Iterable)
     * @see N#intersection(Collection, Collection)
     * @see N#commonSet(Collection, Collection)
     */
    public static final class MapDifference<L, R, D> extends Difference<L, R> {

        /** The diff values. */
        private final D diffValues;

        /**
         * Instantiates a new map difference.
         *
         * @param common
         * @param leftOnly
         * @param rightOnly
         * @param withDifferentValues
         */
        MapDifference(final L common, final L leftOnly, final R rightOnly, final D withDifferentValues) {
            super(common, leftOnly, rightOnly);
            diffValues = withDifferentValues;
        }

        /**
         * Compares two maps and finds the common elements, elements only in the left map, elements only in the right map, and elements with different values.
         *
         * @param <CK>
         * @param <K1>
         * @param <V1>
         * @param <K2>
         * @param <V2>
         * @param map1
         * @param map2
         * @return
         * @see IntList#difference(IntList)
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
            return of(map1, map2, Fn.equal());
        }

        /**
         * Compares two maps and finds the common elements, elements only in the left map, elements only in the right map, and elements with different values.
         * It uses a custom value equivalence BiPredicate to determine if two values are considered equal.
         *
         * @param <CK> The common key type of the two maps.
         * @param <K1> The key type of the first map.
         * @param <V1> The value type of the first map.
         * @param <K2> The key type of the second map.
         * @param <V2> The value type of the second map.
         * @param map1 The first map to be compared.
         * @param map2 The second map to be compared.
         * @param valueEquivalence The BiPredicate used to determine if two values are considered equal.
         * @return A MapDifference object containing the common elements, elements only in the left map, elements only in the right map, and elements with different values.
         * @throws IllegalArgumentException If {@code valueEquivalence} is {@code null}.
         * @see IntList#difference(IntList)
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
            return compare(map1, map2, valueEquivalence);
        }

        private static <CK, K1 extends CK, V1, K2 extends CK, V2> MapDifference<Map<K1, V1>, Map<K2, V2>, Map<CK, Pair<V1, V2>>> compare(
                final Map<? extends K1, ? extends V1> map1, final Map<? extends K2, ? extends V2> map2,
                final BiPredicate<? super V1, ? super V2> valueEquivalence) {
            N.checkArgNotNull(valueEquivalence, cs.valueEquivalence);

            final TriPredicate<K1, V1, V2> triValueEquivalenceToUse = (k, v1, v2) -> valueEquivalence.test(v1, v2);

            return of(map1, map2, triValueEquivalenceToUse);
        }

        /**
         * Compares two maps and finds the common elements, elements only in the left map, elements only in the right map, and elements with different values.
         * It uses a custom value equivalence BiPredicate to determine if two values are considered equal.
         *
         * @param <CK> The common key type of the two maps.
         * @param <K1> The key type of the first map.
         * @param <V1> The value type of the first map.
         * @param <K2> The key type of the second map.
         * @param <V2> The value type of the second map.
         * @param map1 The first map to be compared.
         * @param map2 The second map to be compared.
         * @param valueEquivalence The TriPredicate used to determine if two values are considered equal. The first parameter of the TriPredicate is the key from the first map, the second and third parameters are the values from the first and second maps respectively.
         * @return A MapDifference object containing the common elements, elements only in the left map, elements only in the right map, and elements with different values.
         * @throws IllegalArgumentException If {@code valueEquivalence} is {@code null}.
         * @see IntList#difference(IntList)
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
            return compare(map1, map2, valueEquivalence);
        }

        @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE")
        @SuppressWarnings("unlikely-arg-type")
        private static <CK, K1 extends CK, V1, K2 extends CK, V2> MapDifference<Map<K1, V1>, Map<K2, V2>, Map<CK, Pair<V1, V2>>> compare(
                final Map<? extends K1, ? extends V1> map1, final Map<? extends K2, ? extends V2> map2,
                final TriPredicate<? super K1, ? super V1, ? super V2> valueEquivalence) {
            N.checkArgNotNull(valueEquivalence, cs.valueEquivalence);

            final Map<K1, V1> common = new LinkedHashMap<>();
            final Map<K1, V1> leftOnly = new LinkedHashMap<>();
            final Map<K2, V2> rightOnly = new LinkedHashMap<>();
            final Map<CK, Pair<V1, V2>> withDifferentValues = new LinkedHashMap<>();

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

            return new MapDifference<>(common, leftOnly, rightOnly, withDifferentValues);
        }

        /**
         * Compares two beans and finds the common properties, properties only in the first bean, properties only in the second bean, and properties with different values.
         * It uses the Maps.bean2Map method to convert the beans into maps and then compares them.
         *
         * @param bean1 The first bean to be compared.
         * @param bean2 The second bean to be compared.
         * @return A MapDifference object containing the common properties, properties only in the first bean, properties only in the second bean, and properties with different values.
         * @throws IllegalArgumentException If either of the beans is not a bean class.
         * @see IntList#difference(IntList)
         * @see N#difference(Collection, Collection)
         * @see N#symmetricDifference(Collection, Collection)
         * @see N#excludeAll(Collection, Collection)
         * @see N#excludeAllToSet(Collection, Collection)
         * @see N#removeAll(Collection, Iterable)
         * @see N#intersection(Collection, Collection)
         * @see N#commonSet(Collection, Collection)
         */
        public static MapDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> of(final Object bean1, final Object bean2) {
            if (!ClassUtil.isBeanClass(bean1.getClass()) || !ClassUtil.isBeanClass(bean2.getClass())) {
                throw new IllegalArgumentException(bean1.getClass().getCanonicalName() + " or " + bean2.getClass().getCanonicalName() + " is not a bean class"); // NOSONAR
            }

            return of(Maps.bean2Map(bean1), Maps.bean2Map(bean2));
        }

        /**
         * Compares two beans and finds the common properties, properties only in the first bean, properties only in the second bean, and properties with different values.
         * It uses the Maps.bean2Map method to convert the beans into maps and then compares them using a custom value equivalence BiPredicate.
         *
         * @param bean1 The first bean to be compared.
         * @param bean2 The second bean to be compared.
         * @param valueEquivalence The BiPredicate used to determine if two values are considered equal.
         * @return A MapDifference object containing the common properties, properties only in the first bean, properties only in the second bean, and properties with different values.
         * @throws IllegalArgumentException If either of the beans is not a bean class, or if {@code valueEquivalence} is {@code null}.
         * @see IntList#difference(IntList)
         * @see N#difference(Collection, Collection)
         * @see N#symmetricDifference(Collection, Collection)
         * @see N#excludeAll(Collection, Collection)
         * @see N#excludeAllToSet(Collection, Collection)
         * @see N#removeAll(Collection, Iterable)
         * @see N#intersection(Collection, Collection)
         * @see N#commonSet(Collection, Collection)
         */
        public static MapDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> of(final Object bean1, final Object bean2,
                final BiPredicate<?, ?> valueEquivalence) {
            if (!ClassUtil.isBeanClass(bean1.getClass()) || !ClassUtil.isBeanClass(bean2.getClass())) {
                throw new IllegalArgumentException(bean1.getClass().getCanonicalName() + " or " + bean2.getClass().getCanonicalName() + " is not a bean class"); // NOSONAR
            }

            final Map<String, Object> map1 = Maps.bean2Map(bean1);
            final Map<String, Object> map2 = Maps.bean2Map(bean2);
            final BiPredicate<Object, Object> valueEquivalenceToUse = (BiPredicate<Object, Object>) valueEquivalence;

            return compare(map1, map2, valueEquivalenceToUse);
        }

        /**
         * Compares two beans and finds the common properties, properties only in the first bean, properties only in the second bean, and properties with different values.
         * It uses the Maps.bean2Map method to convert the beans into maps and then compares them using a custom value equivalence BiPredicate.
         *
         * @param bean1 The first bean to be compared.
         * @param bean2 The second bean to be compared.
         * @param valueEquivalence The TriPredicate used to determine if two values are considered equal. The first parameter of the TriPredicate is the property name, the second and third parameters are the property values from the first and second beans respectively.
         * @return A MapDifference object containing the common properties, properties only in the first bean, properties only in the second bean, and properties with different values.
         * @throws IllegalArgumentException If either of the beans is not a bean class, or if {@code valueEquivalence} is {@code null}.
         * @see IntList#difference(IntList)
         * @see N#difference(Collection, Collection)
         * @see N#symmetricDifference(Collection, Collection)
         * @see N#excludeAll(Collection, Collection)
         * @see N#excludeAllToSet(Collection, Collection)
         * @see N#removeAll(Collection, Iterable)
         * @see N#intersection(Collection, Collection)
         * @see N#commonSet(Collection, Collection)
         */
        public static MapDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> of(final Object bean1, final Object bean2,
                final TriPredicate<String, ?, ?> valueEquivalence) {
            if (!ClassUtil.isBeanClass(bean1.getClass()) || !ClassUtil.isBeanClass(bean2.getClass())) {
                throw new IllegalArgumentException(bean1.getClass().getCanonicalName() + " or " + bean2.getClass().getCanonicalName() + " is not a bean class"); // NOSONAR
            }

            final Map<String, Object> map1 = Maps.bean2Map(bean1);
            final Map<String, Object> map2 = Maps.bean2Map(bean2);
            final TriPredicate<String, Object, Object> valueEquivalenceToUse = (TriPredicate<String, Object, Object>) valueEquivalence;

            return compare(map1, map2, valueEquivalenceToUse);
        }

        /**
         * Returns the entries that appear in both maps, but with different values.
         *
         * @return
         */
        public D withDifferentValues() {
            return diffValues;
        }

        /**
         * Checks if the two input maps have the exact same entries.
         *
         * @return {@code true} if the two input maps have the exact same entries, {@code false} otherwise
         */
        @Override
        @SuppressWarnings("rawtypes")
        public boolean areEqual() {
            return super.areEqual() && ((Map) diffValues).isEmpty();
        }

        @Override
        public String toString() {
            return "{inCommon=" + common + ", onLeftOnly=" + leftOnly + ", onRightOnly=" + rightOnly + ", withDifferentValues=" + diffValues + "}";
        }
    }
}
