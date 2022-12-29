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

/**
 *
 * @author Haiyang Li
 * @param <L>
 * @param <R>
 * @since 0.8
 * @see N#difference(Collection, Collection)
 * @see N#differentSet(Collection, Collection)
 * @see N#symmetricDifference(Collection, Collection)
 * @see N#symmetricDifferentSet(Collection, Collection)
 * @see N#excludeAll(Collection, Collection)
 * @see N#excludeAllToSet(Collection, Collection)
 * @see N#removeAll(Collection, Collection)
 * @see N#intersection(Collection, Collection)
 * @see N#commonSet(Collection, Collection)
 */
public class Difference<L, R> {

    final L common;

    final L leftOnly;

    final R rightOnly;

    Difference(L common, L leftOnly, R rightOnly) {
        this.common = common;
        this.leftOnly = leftOnly;
        this.rightOnly = rightOnly;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static Difference<List<Boolean>, List<Boolean>> of(boolean[] a, boolean[] b) {
        return of(BooleanList.of(a), BooleanList.of(b));
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static Difference<List<Character>, List<Character>> of(char[] a, char[] b) {
        return of(CharList.of(a), CharList.of(b));
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static Difference<List<Byte>, List<Byte>> of(byte[] a, byte[] b) {
        return of(ByteList.of(a), ByteList.of(b));
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static Difference<List<Short>, List<Short>> of(short[] a, short[] b) {
        return of(ShortList.of(a), ShortList.of(b));
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static Difference<List<Integer>, List<Integer>> of(int[] a, int[] b) {
        return of(IntList.of(a), IntList.of(b));
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static Difference<List<Long>, List<Long>> of(long[] a, long[] b) {
        return of(LongList.of(a), LongList.of(b));
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static Difference<List<Float>, List<Float>> of(float[] a, float[] b) {
        return of(FloatList.of(a), FloatList.of(b));
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static Difference<List<Double>, List<Double>> of(double[] a, double[] b) {
        return of(DoubleList.of(a), DoubleList.of(b));
    }

    /**
     *
     * @param <T1>
     * @param <T2>
     * @param <L>
     * @param <R>
     * @param a
     * @param b
     * @return
     * @see N#difference(Collection, Collection)
     * @see N#differentSet(Collection, Collection)
     * @see N#symmetricDifference(Collection, Collection)
     * @see N#symmetricDifferentSet(Collection, Collection)
     * @see N#excludeAll(Collection, Collection)
     * @see N#excludeAllToSet(Collection, Collection)
     * @see N#removeAll(Collection, Collection)
     * @see N#intersection(Collection, Collection)
     * @see N#commonSet(Collection, Collection)
     */
    public static <T1, T2, L extends List<T1>, R extends List<T2>> Difference<L, R> of(T1[] a, T2[] b) {
        return of(Arrays.asList(a), Arrays.asList(b));
    }

    /**
     *
     * @param <T1>
     * @param <T2>
     * @param <L>
     * @param <R>
     * @param a
     * @param b
     * @return
     * @see N#difference(Collection, Collection)
     * @see N#differentSet(Collection, Collection)
     * @see N#symmetricDifference(Collection, Collection)
     * @see N#symmetricDifferentSet(Collection, Collection)
     * @see N#excludeAll(Collection, Collection)
     * @see N#excludeAllToSet(Collection, Collection)
     * @see N#removeAll(Collection, Collection)
     * @see N#intersection(Collection, Collection)
     * @see N#commonSet(Collection, Collection)
     */
    public static <T1, T2, L extends List<T1>, R extends List<T2>> Difference<L, R> of(Collection<? extends T1> a, Collection<? extends T2> b) {
        List<T1> common = new ArrayList<>();
        List<T1> leftOnly = new ArrayList<>();
        List<T2> rightOnly = new ArrayList<>();

        if (N.isNullOrEmpty(a)) {
            if (N.isNullOrEmpty(b)) {
                // Do nothing. All empty.
            } else {
                rightOnly.addAll(b);
            }
        } else if (N.isNullOrEmpty(b)) {
            leftOnly.addAll(a);
        } else {
            final Multiset<T2> bOccurrences = Multiset.from(b);

            for (T1 e : a) {
                if (bOccurrences.getAndRemove(e) > 0) {
                    common.add(e);
                } else {
                    leftOnly.add(e);
                }
            }

            for (T2 e : b) {
                if (bOccurrences.getAndRemove(e) > 0) {
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
     *
     * @param a
     * @param b
     * @return
     */
    public static Difference<List<Boolean>, List<Boolean>> of(BooleanList a, BooleanList b) {
        List<Boolean> common = new ArrayList<>();
        List<Boolean> leftOnly = new ArrayList<>();
        List<Boolean> rightOnly = new ArrayList<>();

        if (N.isNullOrEmpty(a)) {
            if (N.isNullOrEmpty(b)) {
                // Do nothing. All empty.
            } else {
                rightOnly = b.toList();
            }
        } else if (N.isNullOrEmpty(b)) {
            leftOnly = a.toList();
        } else {
            final Multiset<Boolean> bOccurrences = b.toMultiset();

            boolean e = false;
            for (int i = 0, len = a.size(); i < len; i++) {
                e = a.get(i);

                if (bOccurrences.getAndRemove(e) > 0) {
                    common.add(e);
                } else {
                    leftOnly.add(e);
                }
            }

            for (int i = 0, len = b.size(); i < len; i++) {
                e = b.get(i);

                if (bOccurrences.getAndRemove(e) > 0) {
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
     *
     * @param a
     * @param b
     * @return
     */
    public static Difference<List<Character>, List<Character>> of(CharList a, CharList b) {
        List<Character> common = new ArrayList<>();
        List<Character> leftOnly = new ArrayList<>();
        List<Character> rightOnly = new ArrayList<>();

        if (N.isNullOrEmpty(a)) {
            if (N.isNullOrEmpty(b)) {
                // Do nothing. All empty.
            } else {
                rightOnly = b.toList();
            }
        } else if (N.isNullOrEmpty(b)) {
            leftOnly = a.toList();
        } else {
            final Multiset<Character> bOccurrences = b.toMultiset();

            char e = 0;
            for (int i = 0, len = a.size(); i < len; i++) {
                e = a.get(i);

                if (bOccurrences.getAndRemove(e) > 0) {
                    common.add(e);
                } else {
                    leftOnly.add(e);
                }
            }

            for (int i = 0, len = b.size(); i < len; i++) {
                e = b.get(i);

                if (bOccurrences.getAndRemove(e) > 0) {
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
     *
     * @param a
     * @param b
     * @return
     */
    public static Difference<List<Byte>, List<Byte>> of(ByteList a, ByteList b) {
        List<Byte> common = new ArrayList<>();
        List<Byte> leftOnly = new ArrayList<>();
        List<Byte> rightOnly = new ArrayList<>();

        if (N.isNullOrEmpty(a)) {
            if (N.isNullOrEmpty(b)) {
                // Do nothing. All empty.
            } else {
                rightOnly = b.toList();
            }
        } else if (N.isNullOrEmpty(b)) {
            leftOnly = a.toList();
        } else {
            final Multiset<Byte> bOccurrences = b.toMultiset();

            byte e = 0;
            for (int i = 0, len = a.size(); i < len; i++) {
                e = a.get(i);

                if (bOccurrences.getAndRemove(e) > 0) {
                    common.add(e);
                } else {
                    leftOnly.add(e);
                }
            }

            for (int i = 0, len = b.size(); i < len; i++) {
                e = b.get(i);

                if (bOccurrences.getAndRemove(e) > 0) {
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
     *
     * @param a
     * @param b
     * @return
     */
    public static Difference<List<Short>, List<Short>> of(ShortList a, ShortList b) {
        List<Short> common = new ArrayList<>();
        List<Short> leftOnly = new ArrayList<>();
        List<Short> rightOnly = new ArrayList<>();

        if (N.isNullOrEmpty(a)) {
            if (N.isNullOrEmpty(b)) {
                // Do nothing. All empty.
            } else {
                rightOnly = b.toList();
            }
        } else if (N.isNullOrEmpty(b)) {
            leftOnly = a.toList();
        } else {
            final Multiset<Short> bOccurrences = b.toMultiset();

            short e = 0;
            for (int i = 0, len = a.size(); i < len; i++) {
                e = a.get(i);

                if (bOccurrences.getAndRemove(e) > 0) {
                    common.add(e);
                } else {
                    leftOnly.add(e);
                }
            }

            for (int i = 0, len = b.size(); i < len; i++) {
                e = b.get(i);

                if (bOccurrences.getAndRemove(e) > 0) {
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
     *
     * @param a
     * @param b
     * @return
     */
    public static Difference<List<Integer>, List<Integer>> of(IntList a, IntList b) {
        List<Integer> common = new ArrayList<>();
        List<Integer> leftOnly = new ArrayList<>();
        List<Integer> rightOnly = new ArrayList<>();

        if (N.isNullOrEmpty(a)) {
            if (N.isNullOrEmpty(b)) {
                // Do nothing. All empty.
            } else {
                rightOnly = b.toList();
            }
        } else if (N.isNullOrEmpty(b)) {
            leftOnly = a.toList();
        } else {
            final Multiset<Integer> bOccurrences = b.toMultiset();

            int e = 0;
            for (int i = 0, len = a.size(); i < len; i++) {
                e = a.get(i);

                if (bOccurrences.getAndRemove(e) > 0) {
                    common.add(e);
                } else {
                    leftOnly.add(e);
                }
            }

            for (int i = 0, len = b.size(); i < len; i++) {
                e = b.get(i);

                if (bOccurrences.getAndRemove(e) > 0) {
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
     *
     * @param a
     * @param b
     * @return
     */
    public static Difference<List<Long>, List<Long>> of(LongList a, LongList b) {
        List<Long> common = new ArrayList<>();
        List<Long> leftOnly = new ArrayList<>();
        List<Long> rightOnly = new ArrayList<>();

        if (N.isNullOrEmpty(a)) {
            if (N.isNullOrEmpty(b)) {
                // Do nothing. All empty.
            } else {
                rightOnly = b.toList();
            }
        } else if (N.isNullOrEmpty(b)) {
            leftOnly = a.toList();
        } else {
            final Multiset<Long> bOccurrences = b.toMultiset();

            long e = 0;
            for (int i = 0, len = a.size(); i < len; i++) {
                e = a.get(i);

                if (bOccurrences.getAndRemove(e) > 0) {
                    common.add(e);
                } else {
                    leftOnly.add(e);
                }
            }

            for (int i = 0, len = b.size(); i < len; i++) {
                e = b.get(i);

                if (bOccurrences.getAndRemove(e) > 0) {
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
     *
     * @param a
     * @param b
     * @return
     */
    public static Difference<List<Float>, List<Float>> of(FloatList a, FloatList b) {
        List<Float> common = new ArrayList<>();
        List<Float> leftOnly = new ArrayList<>();
        List<Float> rightOnly = new ArrayList<>();

        if (N.isNullOrEmpty(a)) {
            if (N.isNullOrEmpty(b)) {
                // Do nothing. All empty.
            } else {
                rightOnly = b.toList();
            }
        } else if (N.isNullOrEmpty(b)) {
            leftOnly = a.toList();
        } else {
            final Multiset<Float> bOccurrences = b.toMultiset();

            float e = 0;
            for (int i = 0, len = a.size(); i < len; i++) {
                e = a.get(i);

                if (bOccurrences.getAndRemove(e) > 0) {
                    common.add(e);
                } else {
                    leftOnly.add(e);
                }
            }

            for (int i = 0, len = b.size(); i < len; i++) {
                e = b.get(i);

                if (bOccurrences.getAndRemove(e) > 0) {
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
     *
     * @param a
     * @param b
     * @return
     */
    public static Difference<List<Double>, List<Double>> of(DoubleList a, DoubleList b) {
        List<Double> common = new ArrayList<>();
        List<Double> leftOnly = new ArrayList<>();
        List<Double> rightOnly = new ArrayList<>();

        if (N.isNullOrEmpty(a)) {
            if (N.isNullOrEmpty(b)) {
                // Do nothing. All empty.
            } else {
                rightOnly = b.toList();
            }
        } else if (N.isNullOrEmpty(b)) {
            leftOnly = a.toList();
        } else {
            final Multiset<Double> bOccurrences = b.toMultiset();

            double e = 0;
            for (int i = 0, len = a.size(); i < len; i++) {
                e = a.get(i);

                if (bOccurrences.getAndRemove(e) > 0) {
                    common.add(e);
                } else {
                    leftOnly.add(e);
                }
            }

            for (int i = 0, len = b.size(); i < len; i++) {
                e = b.get(i);

                if (bOccurrences.getAndRemove(e) > 0) {
                    rightOnly.add(e);
                }

                if (bOccurrences.isEmpty()) {
                    break;
                }
            }
        }

        return new Difference<>(common, leftOnly, rightOnly);
    }

    public L inCommon() {
        return common;
    }

    /**
     * On left only.
     *
     * @return
     */
    public L onLeftOnly() {
        return leftOnly;
    }

    /**
     * On right only.
     *
     * @return
     */
    public R onRightOnly() {
        return rightOnly;
    }

    /**
     *
     * @return
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
     * The Class MapDifference.
     *
     * @param <L>
     * @param <R>
     * @param <D>
     *
     * @see N#difference(Collection, Collection)
     * @see N#differentSet(Collection, Collection)
     * @see N#symmetricDifference(Collection, Collection)
     * @see N#symmetricDifferentSet(Collection, Collection)
     * @see N#excludeAll(Collection, Collection)
     * @see N#excludeAllToSet(Collection, Collection)
     * @see N#removeAll(Collection, Collection)
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
        MapDifference(L common, L leftOnly, R rightOnly, D withDifferentValues) {
            super(common, leftOnly, rightOnly);
            this.diffValues = withDifferentValues;
        }

        /**
         *
         * @param <CK>
         * @param <K1>
         * @param <V1>
         * @param <K2>
         * @param <V2>
         * @param map1
         * @param map2
         * @return
         */
        public static <CK, K1 extends CK, V1, K2 extends CK, V2> MapDifference<Map<K1, V1>, Map<K2, V2>, Map<CK, Pair<V1, V2>>> of(
                final Map<? extends K1, ? extends V1> map1, final Map<? extends K2, ? extends V2> map2) {
            return of(map1, map2, Fn.equal());
        }

        /**
         *
         * @param <K1>
         * @param <V1>
         * @param <K2>
         * @param <V2>
         * @param <L>
         * @param <R>
         * @param <D>
         * @param map1
         * @param map2
         * @param valueEquivalence
         * @return
         */
        @SuppressWarnings("unlikely-arg-type")
        public static <CK, K1 extends CK, V1, K2 extends CK, V2> MapDifference<Map<K1, V1>, Map<K2, V2>, Map<CK, Pair<V1, V2>>> of(
                final Map<? extends K1, ? extends V1> map1, final Map<? extends K2, ? extends V2> map2,
                final BiPredicate<? super V1, ? super V2> valueEquivalence) {
            N.checkArgNotNull(valueEquivalence, "valueEquivalence");

            final Map<K1, V1> common = new LinkedHashMap<>();
            final Map<K1, V1> leftOnly = new LinkedHashMap<>();
            final Map<K2, V2> rightOnly = new LinkedHashMap<>();
            final Map<CK, Pair<V1, V2>> withDifferentValues = new LinkedHashMap<>();

            if (N.isNullOrEmpty(map1)) {
                if (N.isNullOrEmpty(map2)) {
                    // Do nothing. All empty.
                } else {
                    rightOnly.putAll(map2);
                }
            } else if (N.isNullOrEmpty(map2)) {
                leftOnly.putAll(map1);
            } else {
                Object key1 = null;
                V2 val2 = null;
                for (Entry<K1, V1> entry1 : ((Map<K1, V1>) map1).entrySet()) {
                    key1 = entry1.getKey();
                    val2 = map2.get(key1);

                    if (val2 == null) {
                        if (map2.containsKey(key1)) {
                            if (entry1.getValue() == null) {
                                common.put(entry1.getKey(), entry1.getValue());
                            } else {
                                withDifferentValues.put(entry1.getKey(), Pair.of(entry1.getValue(), val2));
                            }
                        } else {
                            leftOnly.put(entry1.getKey(), entry1.getValue());
                        }
                    } else if (valueEquivalence.test(entry1.getValue(), val2)) {
                        common.put(entry1.getKey(), entry1.getValue());
                    } else {
                        withDifferentValues.put(entry1.getKey(), Pair.of(entry1.getValue(), val2));
                    }
                }

                for (Entry<K2, V2> entry2 : ((Map<K2, V2>) map2).entrySet()) {
                    if (common.containsKey(entry2.getKey()) || withDifferentValues.containsKey(entry2.getKey())) {
                        continue;
                    }

                    rightOnly.put(entry2.getKey(), entry2.getValue());
                }
            }

            return new MapDifference<>(common, leftOnly, rightOnly, withDifferentValues);
        }

        /**
         *
         * @param bean1
         * @param bean2
         * @return
         */
        public static MapDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> of(final Object bean1, final Object bean2) {
            if (!ClassUtil.isBeanClass(bean1.getClass()) || !ClassUtil.isBeanClass(bean2.getClass())) {
                throw new IllegalArgumentException(
                        bean1.getClass().getCanonicalName() + " or " + bean2.getClass().getCanonicalName() + " is not a bean class");
            }

            return of(Maps.bean2Map(bean1), Maps.bean2Map(bean2));
        }

        /**
         *
         * @param bean1
         * @param bean2
         * @param valueEquivalence
         * @return
         */
        public static MapDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> of(final Object bean1, final Object bean2,
                final BiPredicate<?, ?> valueEquivalence) {
            if (!ClassUtil.isBeanClass(bean1.getClass()) || !ClassUtil.isBeanClass(bean2.getClass())) {
                throw new IllegalArgumentException(
                        bean1.getClass().getCanonicalName() + " or " + bean2.getClass().getCanonicalName() + " is not a bean class");
            }

            final Map<String, Object> map1 = Maps.bean2Map(bean1);
            final Map<String, Object> map2 = Maps.bean2Map(bean2);
            final BiPredicate<Object, Object> valueEquivalenceToUse = (BiPredicate<Object, Object>) valueEquivalence;

            return of(map1, map2, valueEquivalenceToUse);
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
         *
         * @return
         */
        @Override
        @SuppressWarnings("rawtypes")
        public boolean areEqual() {
            return super.areEqual() && ((Map) diffValues).isEmpty();
        }

        /**
         *
         * @return
         */
        @Override
        public String toString() {
            return "{inCommon=" + common + ", onLeftOnly=" + leftOnly + ", onRightOnly=" + rightOnly + ", withDifferentValues=" + diffValues + "}";
        }
    }
}
