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
import com.landawn.abacus.util.Fn.Factory;
import com.landawn.abacus.util.function.TriPredicate;

/**
 * It is used to compare two collections and find the common elements, elements only in the left collection, and elements only in the right collection. Occurrences are considered.
 *
 * @param <L>
 * @param <R>
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + N.hashCode(inCommon());
        result = prime * result + N.hashCode(onLeftOnly());
        return prime * result + N.hashCode(onRightOnly());
    }

    @Override
    public String toString() {
        return "{areEqual=" + areEqual() + ", inCommon=" + common + ", onLeftOnly=" + leftOnly + ", onRightOnly=" + rightOnly + "}";
    }

    /**
     * It is used to compare two maps/beans and find the common entries (key/value), entries only in the left map/bean, entries only in the right map/bean, and entries with different values.
     *
     * @param <L>
     * @param <R>
     * @param <D>
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
         * Returns the entries that appear in both maps/beans, but with different values.
         *
         * @return
         */
        public D withDifferentValues() {
            return diffValues;
        }

        /**
         * Checks if the two input maps/beans have the exact same entries.
         *
         * @return {@code true} if the two input maps/beans have the exact same entries, {@code false} otherwise
         */
        @Override
        @SuppressWarnings("rawtypes")
        public boolean areEqual() {
            return super.areEqual() && ((Map) diffValues).isEmpty();
        }

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

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + N.hashCode(inCommon());
            result = prime * result + N.hashCode(onLeftOnly());
            result = prime * result + N.hashCode(onRightOnly());
            return prime * result + N.hashCode(withDifferentValues());
        }

        @Override
        public String toString() {
            return "{areEqual=" + areEqual() + ", inCommon=" + common + ", onLeftOnly=" + leftOnly + ", onRightOnly=" + rightOnly + ", withDifferentValues="
                    + diffValues + "}";
        }
    }

    /**
     * It is used to compare two maps and find the common elements, elements only in the left map, elements only in the right map, and elements with different values.
     *
     * @param <L>
     * @param <R>
     * @param <D>
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
         * Compares two maps and finds the common elements, elements only in the left map, 
         * elements only in the right map, and elements with different values.
         *
         * @param <CK> The common key type shared by both maps.
         * @param <K1> The key type of the first map, which extends {@code CK}.
         * @param <V1> The value type of the first map.
         * @param <K2> The key type of the second map, which extends {@code CK}.
         * @param <V2> The value type of the second map.
         * @param map1 The first map to be compared.
         * @param map2 The second map to be compared.
         * @return A {@code MapDifference} object containing:
         *         <ul>
         *           <li>The common entries.</li>
         *           <li>Entries unique to each map.</li>
         *           <li>Entries with the same keys but different values.</li>
         *         </ul>
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
         * Compares two maps and finds the common elements, elements only in the left map, elements only in the right map, 
         * and elements with different values.
         *
         * <p>This method supports comparing maps with different key and value types, and allows specifying a custom 
         * equivalence predicate for comparing values. It also supports restricting the comparison to a specific set of keys.</p>
         *
         * @param <CK> The common key type of the two maps.
         * @param <K1> The key type of the first map.
         * @param <V1> The value type of the first map.
         * @param <K2> The key type of the second map.
         * @param <V2> The value type of the second map.
         * @param map1 The first map to be compared.
         * @param map2 The second map to be compared.
         * @param keysToCompare The keys to compare between the two maps. If {@code null}, all keys will be compared.
         *         right map, and elements with different values.
         * @throws IllegalArgumentException If {@code valueEquivalence} is {@code null}.
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
            return of(map1, map2, null, (k, v1, v2) -> valueEquivalence.test(v1, v2));
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
            return of(map1, map2, null, valueEquivalence);
        }

        /**
         * Compares two maps and finds the common elements, elements only in the left map, elements only in the right map, and elements with different values.
         * Only the entries with key in {@code keysToCompare} will be compared.
         *
         * @param <CK> The common key type of the two maps.
         * @param <K1> The key type of the first map.
         * @param <V1> The value type of the first map.
         * @param <K2> The key type of the second map.
         * @param <V2> The value type of the second map.
         * @param map1 The first map to be compared.
         * @param map2 The second map to be compared.
         * @param keysToCompare The keys to compare between the two maps. If {@code null}, all keys will be compared.
         * @param valueEquivalence The TriPredicate used to determine if two values are considered equal. The first parameter of the TriPredicate is the key from the first map, the second and third parameters are the values from the first and second maps respectively.
         * @return A MapDifference object containing the common elements, elements only in the left map, elements only in the right map, and elements with different values.
         * @throws IllegalArgumentException If {@code valueEquivalence} is {@code null}.
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
         * Compares two collections of maps and finds the common elements, elements only in the first collection, elements only in the second collection, and elements with different values.
         *
         * @param <CK> The common key type of the two maps.
         * @param <CV> The value type of the maps.
         * @param <K> The key type used to identify each map.
         * @param a The first collection of maps to be compared.
         * @param b The second collection of maps to be compared.
         * @return A MapDifference object containing the common elements, elements only in the first collection, elements only in the second collection, and elements with different values.
         */
        public static <CK, CV, K> MapDifference<List<Map<CK, CV>>, List<Map<CK, CV>>, Map<K, MapDifference<Map<CK, CV>, Map<CK, CV>, Map<CK, Pair<CV, CV>>>>> of(
                final Collection<? extends Map<? extends CK, ? extends CV>> a, final Collection<? extends Map<? extends CK, ? extends CV>> b,
                final Function<? super Map<? extends CK, ? extends CV>, ? extends K> idExtractor) {
            return of(a, b, null, idExtractor, idExtractor);
        }

        /**
         * Compares two collections of maps and finds the common elements, elements only in the first collection, elements only in the second collection, and elements with different values.
         *
         * @param <CK> The common key type of the two maps.
         * @param <CV> The value type of the maps.
         * @param <K> The key type used to identify each map.
         * @param a The first collection of maps to be compared.
         * @param b The second collection of maps to be compared.
         * @param keysToCompare The keys to compare between the two maps. If {@code null}, all keys will be compared.
         * @param idExtractor The function used to extract the ID from each map.
         * @return A MapDifference object containing the common elements, elements only in the first collection, elements only in the second collection, and elements with different values.
         */
        public static <CK, CV, K> MapDifference<List<Map<CK, CV>>, List<Map<CK, CV>>, Map<K, MapDifference<Map<CK, CV>, Map<CK, CV>, Map<CK, Pair<CV, CV>>>>> of(
                final Collection<? extends Map<? extends CK, ? extends CV>> a, final Collection<? extends Map<? extends CK, ? extends CV>> b,
                final Collection<CK> keysToCompare, final Function<? super Map<? extends CK, ? extends CV>, ? extends K> idExtractor) {
            return of(a, b, keysToCompare, idExtractor, idExtractor);
        }

        /**
         * Compares two collections of maps and finds the common elements, elements only in the first collection, elements only in the second collection, and elements with different values.
         *
         * @param <CK> The common key type of the two maps.
         * @param <K1> The key type of the first map.
         * @param <V1> The value type of the first map.
         * @param <K2> The key type of the second map.
         * @param <V2> The value type of the second map.
         * @param a The first collection of maps to be compared.
         * @param b The second collection of maps to be compared.
         * @param idExtractor1 The function used to extract the ID from the first map.
         * @param idExtractor2 The function used to extract the ID from the second map.
         * @return A MapDifference object containing the common elements, elements only in the first collection, elements only in the second collection, and elements with different values.
         */
        public static <CK, K1 extends CK, V1, K2 extends CK, V2, K> MapDifference<List<Map<K1, V1>>, List<Map<K2, V2>>, Map<K, MapDifference<Map<K1, V1>, Map<K2, V2>, Map<CK, Pair<V1, V2>>>>> of(
                final Collection<? extends Map<? extends K1, ? extends V1>> a, final Collection<? extends Map<? extends K2, ? extends V2>> b,
                final Function<? super Map<? extends K1, ? extends V1>, ? extends K> idExtractor1,
                final Function<? super Map<? extends K2, ? extends V2>, ? extends K> idExtractor2) {

            return of(a, b, null, idExtractor1, idExtractor2);
        }

        /**
         * Compares two collections of maps and finds the common elements, elements only in the first collection, elements only in the second collection, and elements with different values.
         *
         * @param <CK> The common key type of the two maps.
         * @param <K1> The key type of the first map.
         * @param <V1> The value type of the first map.
         * @param <K2> The key type of the second map.
         * @param <V2> The value type of the second map.
         * @param a The first collection of maps to be compared.
         * @param b The second collection of maps to be compared.
         * @param keysToCompare The keys to compare between the two maps. If {@code null}, all keys will be compared.
         * @param idExtractor1 The function used to extract the ID from the first map.
         * @param idExtractor2 The function used to extract the ID from the second map.
         * @return A MapDifference object containing the common elements, elements only in the first collection, elements only in the second collection, and elements with different values.
         */
        public static <CK, K1 extends CK, V1, K2 extends CK, V2, K> MapDifference<List<Map<K1, V1>>, List<Map<K2, V2>>, Map<K, MapDifference<Map<K1, V1>, Map<K2, V2>, Map<CK, Pair<V1, V2>>>>> of(
                final Collection<? extends Map<? extends K1, ? extends V1>> a, final Collection<? extends Map<? extends K2, ? extends V2>> b,
                final Collection<CK> keysToCompare, final Function<? super Map<? extends K1, ? extends V1>, ? extends K> idExtractor1,
                final Function<? super Map<? extends K2, ? extends V2>, ? extends K> idExtractor2) {

            final boolean isEmptyPropNamesToCompare = N.isEmpty(keysToCompare);

            final List<Map<K1, V1>> common = new ArrayList<>();
            final List<Map<K1, V1>> leftOnly = new ArrayList<>();
            final List<Map<K2, V2>> rightOnly = new ArrayList<>();
            final Map<K, MapDifference<Map<K1, V1>, Map<K2, V2>, Map<CK, Pair<V1, V2>>>> withDifferentValues = new LinkedHashMap<>();

            if (N.isEmpty(a)) {
                if (N.isEmpty(b)) {
                    // Do nothing. All empty.
                } else {
                    rightOnly.addAll((Collection<? extends Map<K2, V2>>) b);
                }
            } else if (N.isEmpty(b)) {
                leftOnly.addAll((Collection<? extends Map<K1, V1>>) a);
            } else {
                final Map<K, Map<? extends K1, ? extends V1>> beanMapA = N.toMap(a, idExtractor1, Fn.identity(), Fn.throwingMerger(),
                        Factory.ofLinkedHashMap());
                final Map<K, Map<? extends K2, ? extends V2>> beanMapB = N.toMap(b, idExtractor2, Fn.identity(), Fn.throwingMerger(),
                        Factory.ofLinkedHashMap());

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
     * Represents the difference between two beans, including common properties, properties only in the first bean, 
     * properties only in the second bean, and properties with different values.
     *
     *
     * @param <L> The type of the map representing properties only in the first bean.
     * @param <R> The type of the map representing properties only in the second bean.
     * @param <D> The type of the map representing properties with different values.
     *
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
         * Compares two beans and finds the common properties, properties only in the first bean, properties only in the second bean, and properties with different values.
         *
         * @param bean1 The first bean to be compared.
         * @param bean2 The second bean to be compared.
         * @return A MapDifference object containing the common properties, properties only in the first bean, properties only in the second bean, and properties with different values.
         * @throws IllegalArgumentException If either of the beans is not a bean class.
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
         * Compares two beans and finds the common properties, properties only in the first bean, properties only in the second bean, and properties with different values.
         *
         * @param bean1 The first bean to be compared.
         * @param bean2 The second bean to be compared.
         * @param propNamesToCompare The property names to be compared.
         * @return A MapDifference object containing the common properties, properties only in the first bean, properties only in the second bean, and properties with different values.
         * @throws IllegalArgumentException If either of the beans is not a bean class.
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
         * Compares two beans and finds the common properties, properties only in the first bean, properties only in the second bean, and properties with different values.
         * Property values are compared by the specified equivalence BiPredicate.
         *
         * @param bean1 The first bean to be compared.
         * @param bean2 The second bean to be compared.
         * @param valueEquivalence The BiPredicate used to determine if two values are considered equal.
         * @return A MapDifference object containing the common properties, properties only in the first bean, properties only in the second bean, and properties with different values.
         * @throws IllegalArgumentException If either of the beans is not a bean class, or if {@code valueEquivalence} is {@code null}.
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
            final BiPredicate<Object, Object> valueEquivalenceToUse = (BiPredicate<Object, Object>) valueEquivalence;

            return of(bean1, bean2, null, (k, v1, v2) -> valueEquivalenceToUse.test(v1, v2));
        }

        /**
         * Compares two beans and finds the common properties, properties only in the first bean, properties only in the second bean, and properties with different values.
         * Property values are compared by the specified equivalence BiPredicate.
         *
         * @param bean1 The first bean to be compared.
         * @param bean2 The second bean to be compared.
         * @param valueEquivalence The TriPredicate used to determine if two values are considered equal. The first parameter of the TriPredicate is the property name, the second and third parameters are the property values from the first and second beans respectively.
         * @return A MapDifference object containing the common properties, properties only in the first bean, properties only in the second bean, and properties with different values.
         * @throws IllegalArgumentException If either of the beans is not a bean class, or if {@code valueEquivalence} is {@code null}.
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
         * Compares two beans and finds the common properties, properties only in the first bean, properties only in the second bean, and properties with different values.
         * Property values are compared by the specified equivalence TriPredicate.
         *
         * @param bean1 The first bean to be compared.
         * @param bean2 The second bean to be compared.
         * @param propNamesToCompare The property names to be compared.
         * @param valueEquivalence The TriPredicate used to determine if two values are considered equal. The first parameter of the TriPredicate is the property name, the second and third parameters are the property values from the first and second beans respectively.
         * @return A MapDifference object containing the common properties, properties only in the first bean, properties only in the second bean, and properties with different values.
         * @throws IllegalArgumentException If either of the beans is not a bean class, or if {@code valueEquivalence} is {@code null}.
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
         * Compares two collections of beans and finds the common beans, beans only in the first collection, beans only in the second collection, and beans with different values.
         * It uses the specified identifier extractor function to identify each bean.
         *
         * @param <T> The type of the bean.
         * @param <C> The type of the list containing common beans.
         * @param <K> The type of the key used to identify each bean.
         * @param a The first collection of beans to be compared.
         * @param b The second collection of beans to be compared.
         * @param idExtractor The function used to extract the ID from each bean.
         * @return A BeanDifference object containing:
         *         <ul>
         *           <li>The common beans.</li>
         *           <li>Beans only in the first collection.</li>
         *           <li>Beans only in the second collection.</li>
         *           <li>Beans with different values.</li>
         *         </ul>
         */
        public static <T, C extends List<T>, K> BeanDifference<C, C, Map<K, BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>>> of(
                final Collection<? extends T> a, final Collection<? extends T> b, final Function<? super T, K> idExtractor) {
            return of(a, b, null, idExtractor, idExtractor);
        }

        /**
         * Compares two collections of beans and finds the common beans, beans only in the first collection, beans only in the second collection, and beans with different values.
         * It uses the specified property names to compare the beans.
         *
         * @param <T> The type of the bean.
         * @param <C> The type of the list containing common beans.
         * @param <K> The type of the key used to identify each bean.
         * @param a The first collection of beans to be compared.
         * @param b The second collection of beans to be compared.
         * @param propNamesToCompare The property names to be compared. If {@code null}, all properties will be compared.
         * @param idExtractor The function used to extract the ID from each bean.
         * @return A BeanDifference object containing:
         *         <ul>
         *           <li>The common beans.</li>
         *           <li>Beans only in the first collection.</li>
         *           <li>Beans only in the second collection.</li>
         *           <li>Beans with different values.</li>
         *         </ul>
         */
        public static <T, C extends List<T>, K> BeanDifference<C, C, Map<K, BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>>> of(
                final Collection<? extends T> a, final Collection<? extends T> b, final Collection<String> propNamesToCompare,
                final Function<? super T, K> idExtractor) {
            return of(a, b, propNamesToCompare, idExtractor, idExtractor);
        }

        /**
         * Compares two collections of beans and finds the common beans, beans only in the first collection, beans only in the second collection, and beans with different values.
         * It uses the specified identifier extractor functions to identify each bean.
         *
         * @param <T1> The type of the first bean.
         * @param <T2> The type of the second bean.
         * @param <L> The type of the list containing common beans.
         * @param <R> The type of the list containing beans only in the second collection.
         * @param <K> The type of the key used to identify each bean.
         * @param a The first collection of beans to be compared.
         * @param b The second collection of beans to be compared.
         * @param idExtractor1 The function to extract the identifier from the first bean.
         * @param idExtractor2 The function to extract the identifier from the second bean.
         * @return A BeanDifference object containing:
         *         <ul>
         *           <li>The common beans.</li>
         *           <li>Beans only in the first collection.</li>
         *           <li>Beans only in the second collection.</li>
         *           <li>Beans with different values.</li>
         *         </ul>
         */
        public static <T1, T2, L extends List<T1>, R extends List<T2>, K> BeanDifference<L, R, Map<K, BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>>> of(
                final Collection<? extends T1> a, final Collection<? extends T2> b, final Function<? super T1, ? extends K> idExtractor1,
                final Function<? super T2, ? extends K> idExtractor2) {
            return of(a, b, null, idExtractor1, idExtractor2);
        }

        /**
         * Compares two collections of beans and finds the common beans, beans only in the first collection, beans only in the second collection, and beans with different values.
         * It uses the specified property names to compare the beans.
         *
         * @param <T1> The type of the first bean.
         * @param <T2> The type of the second bean.
         * @param <L> The type of the list containing common beans.
         * @param <R> The type of the list containing beans only in the second collection.
         * @param <K> The type of the key used to identify each bean.
         * @param a The first collection of beans to be compared.
         * @param b The second collection of beans to be compared.
         * @param propNamesToCompare The property names to be compared. If {@code null}, all properties will be compared.
         * @param idExtractor1 The function to extract the identifier from the first bean.
         * @param idExtractor2 The function to extract the identifier from the second bean.
         * @return A BeanDifference object containing:
         *         <ul>
         *           <li>The common beans.</li>
         *           <li>Beans only in the first collection.</li>
         *           <li>Beans only in the second collection.</li>
         *           <li>Beans with different values.</li>
         *         </ul>
         */
        public static <T1, T2, L extends List<T1>, R extends List<T2>, K> BeanDifference<L, R, Map<K, BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>>> of(
                final Collection<? extends T1> a, final Collection<? extends T2> b, final Collection<String> propNamesToCompare,
                final Function<? super T1, ? extends K> idExtractor1, final Function<? super T2, ? extends K> idExtractor2) {

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
                final Map<K, T1> beanMapA = N.toMap(a, idExtractor1, Fn.identity(), Fn.throwingMerger(), Factory.ofLinkedHashMap());
                final Map<K, T2> beanMapB = N.toMap(b, idExtractor2, Fn.identity(), Fn.throwingMerger(), Factory.ofLinkedHashMap());
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
