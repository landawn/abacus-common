/*
 * Copyright (C) 2016, 2017, 2018, 2019 HaiYang Li
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
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
import com.landawn.abacus.util.Comparators;
import com.landawn.abacus.util.DataSet;
import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.DoubleSummaryStatistics;
import com.landawn.abacus.util.FloatList;
import com.landawn.abacus.util.FloatSummaryStatistics;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Fn.BiConsumers;
import com.landawn.abacus.util.Fn.BinaryOperators;
import com.landawn.abacus.util.Fn.Suppliers;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.ImmutableSet;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.IntSummaryStatistics;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.KahanSummation;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.LongList;
import com.landawn.abacus.util.LongMultiset;
import com.landawn.abacus.util.LongSummaryStatistics;
import com.landawn.abacus.util.Multimap;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.ObjIterator;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.ShortList;
import com.landawn.abacus.util.ShortSummaryStatistics;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.Tuple.Tuple4;
import com.landawn.abacus.util.Tuple.Tuple5;
import com.landawn.abacus.util.Tuple.Tuple6;
import com.landawn.abacus.util.Tuple.Tuple7;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.QuadFunction;
import com.landawn.abacus.util.function.ToByteFunction;
import com.landawn.abacus.util.function.ToCharFunction;
import com.landawn.abacus.util.function.ToFloatFunction;
import com.landawn.abacus.util.function.ToShortFunction;
import com.landawn.abacus.util.function.TriFunction;

/**
 *
 * @see {@code java.util.stream.Collectors}
 *
 */
public abstract class Collectors {
    static final Object NONE = new Object(); //NOSONAR

    /**
     * @deprecated
     */
    @Deprecated
    static final Set<Characteristics> CH_CONCURRENT_ID = Collections
            .unmodifiableSet(EnumSet.of(Characteristics.CONCURRENT, Characteristics.UNORDERED, Characteristics.IDENTITY_FINISH));
    /**
     * @deprecated
     */
    @Deprecated
    static final Set<Characteristics> CH_CONCURRENT_NOID = Collections.unmodifiableSet(EnumSet.of(Characteristics.CONCURRENT, Characteristics.UNORDERED));

    static final Set<Characteristics> CH_UNORDERED_ID = Collections.unmodifiableSet(EnumSet.of(Characteristics.UNORDERED, Characteristics.IDENTITY_FINISH));
    static final Set<Characteristics> CH_UNORDERED_NOID = Collections.unmodifiableSet(EnumSet.of(Characteristics.UNORDERED));

    static final Set<Characteristics> CH_ID = Collections.unmodifiableSet(EnumSet.of(Characteristics.IDENTITY_FINISH));
    static final Set<Characteristics> CH_NOID = Collections.emptySet();

    // ============================================================================================================

    @SuppressWarnings("deprecation")
    static final Function<List<Object>, ImmutableList<Object>> ImmutableList_Finisher = ImmutableList::wrap;

    @SuppressWarnings("deprecation")
    static final Function<Set<Object>, ImmutableSet<Object>> ImmutableSet_Finisher = ImmutableSet::wrap;

    @SuppressWarnings("deprecation")
    static final Function<Map<Object, Object>, ImmutableMap<Object, Object>> ImmutableMap_Finisher = ImmutableMap::wrap;

    static final BiConsumer<Multiset<Object>, Object> Multiset_Accumulator = Multiset::add;

    static final BinaryOperator<Multiset<Object>> Multiset_Combiner = (a, b) -> {
        a.addAll(b);
        return a;
    };

    static final BiConsumer<LongMultiset<Object>, Object> LongMultiset_Accumulator = LongMultiset::add;

    static final BinaryOperator<LongMultiset<Object>> LongMultiset_Combiner = (a, b) -> {
        a.addAll(b);
        return a;
    };

    static final BiConsumer<BooleanList, Boolean> BooleanList_Accumulator = BooleanList::add;

    static final BinaryOperator<BooleanList> BooleanList_Combiner = (a, b) -> {
        a.addAll(b);
        return a;
    };

    static final Function<BooleanList, boolean[]> BooleanArray_Finisher = t -> t.trimToSize().array();

    static final BiConsumer<CharList, Character> CharList_Accumulator = CharList::add;

    static final BinaryOperator<CharList> CharList_Combiner = (a, b) -> {
        a.addAll(b);
        return a;
    };

    static final Function<CharList, char[]> CharArray_Finisher = t -> t.trimToSize().array();

    static final BiConsumer<ByteList, Byte> ByteList_Accumulator = ByteList::add;

    static final BinaryOperator<ByteList> ByteList_Combiner = (a, b) -> {
        if (a.size() >= b.size()) {
            a.addAll(b);
            return a;
        } else {
            b.addAll(a);
            return b;
        }
    };

    static final Function<ByteList, byte[]> ByteArray_Finisher = t -> t.trimToSize().array();

    static final BiConsumer<ShortList, Short> ShortList_Accumulator = ShortList::add;

    static final BinaryOperator<ShortList> ShortList_Combiner = (a, b) -> {
        if (a.size() >= b.size()) {
            a.addAll(b);
            return a;
        } else {
            b.addAll(a);
            return b;
        }
    };

    static final Function<ShortList, short[]> ShortArray_Finisher = t -> t.trimToSize().array();

    static final BiConsumer<IntList, Integer> IntList_Accumulator = IntList::add;

    static final BinaryOperator<IntList> IntList_Combiner = (a, b) -> {
        if (a.size() >= b.size()) {
            a.addAll(b);
            return a;
        } else {
            b.addAll(a);
            return b;
        }
    };

    static final Function<IntList, int[]> IntArray_Finisher = t -> t.trimToSize().array();

    static final BiConsumer<LongList, Long> LongList_Accumulator = LongList::add;

    static final BinaryOperator<LongList> LongList_Combiner = (a, b) -> {
        if (a.size() >= b.size()) {
            a.addAll(b);
            return a;
        } else {
            b.addAll(a);
            return b;
        }
    };

    static final Function<LongList, long[]> LongArray_Finisher = t -> t.trimToSize().array();

    static final BiConsumer<FloatList, Float> FloatList_Accumulator = FloatList::add;

    static final BinaryOperator<FloatList> FloatList_Combiner = (a, b) -> {
        if (a.size() >= b.size()) {
            a.addAll(b);
            return a;
        } else {
            b.addAll(a);
            return b;
        }
    };

    static final Function<FloatList, float[]> FloatArray_Finisher = t -> t.trimToSize().array();

    static final BiConsumer<DoubleList, Double> DoubleList_Accumulator = DoubleList::add;

    static final BinaryOperator<DoubleList> DoubleList_Combiner = (a, b) -> {
        if (a.size() >= b.size()) {
            a.addAll(b);
            return a;
        } else {
            b.addAll(a);
            return b;
        }
    };

    static final Function<DoubleList, double[]> DoubleArray_Finisher = t -> t.trimToSize().array();

    static final BiConsumer<Joiner, CharSequence> Joiner_Accumulator = Joiner::append;

    static final BinaryOperator<Joiner> Joiner_Combiner = (a, b) -> {
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

    static final Function<Joiner, String> Joiner_Finisher = Joiner::toString;

    static final Supplier<int[]> SummingInt_Supplier = () -> new int[1];
    static final Supplier<int[]> SummingInt_Supplier_2 = () -> new int[2];
    static final Supplier<int[]> SummingInt_Supplier_3 = () -> new int[3];

    static final Supplier<long[]> SummingIntToLong_Supplier = () -> new long[1];
    static final Supplier<long[]> SummingIntToLong_Supplier_2 = () -> new long[2];
    static final Supplier<long[]> SummingIntToLong_Supplier_3 = () -> new long[3];

    static final BinaryOperator<int[]> SummingInt_Combiner = (a, b) -> {
        a[0] += b[0];
        return a;
    };

    static final BinaryOperator<int[]> SummingInt_Combiner_2 = (a, b) -> {
        a[0] += b[0];
        a[1] += b[1];
        return a;
    };

    static final BinaryOperator<int[]> SummingInt_Combiner_3 = (a, b) -> {
        a[0] += b[0];
        a[1] += b[1];
        a[2] += b[2];
        return a;
    };

    static final BinaryOperator<long[]> SummingIntToLong_Combiner = (a, b) -> {
        a[0] += b[0];
        return a;
    };

    static final BinaryOperator<long[]> SummingIntToLong_Combiner_2 = (a, b) -> {
        a[0] += b[0];
        a[1] += b[1];
        return a;
    };

    static final BinaryOperator<long[]> SummingIntToLong_Combiner_3 = (a, b) -> {
        a[0] += b[0];
        a[1] += b[1];
        a[2] += b[2];
        return a;
    };

    static final Function<int[], Integer> SummingInt_Finisher = a -> a[0];
    static final Function<int[], Tuple2<Integer, Integer>> SummingInt_Finisher_2 = a -> Tuple.of(a[0], a[1]);
    static final Function<int[], Tuple3<Integer, Integer, Integer>> SummingInt_Finisher_3 = a -> Tuple.of(a[0], a[1], a[2]);

    static final Function<long[], Long> SummingIntToLong_Finisher = a -> a[0];
    static final Function<long[], Tuple2<Long, Long>> SummingIntToLong_Finisher_2 = a -> Tuple.of(a[0], a[1]);
    static final Function<long[], Tuple3<Long, Long, Long>> SummingIntToLong_Finisher_3 = a -> Tuple.of(a[0], a[1], a[2]);

    static final Supplier<long[]> SummingLong_Supplier = () -> new long[1];
    static final Supplier<long[]> SummingLong_Supplier_2 = () -> new long[2];
    static final Supplier<long[]> SummingLong_Supplier_3 = () -> new long[3];

    static final BinaryOperator<long[]> SummingLong_Combiner = (a, b) -> {
        a[0] += b[0];
        return a;
    };

    static final BinaryOperator<long[]> SummingLong_Combiner_2 = (a, b) -> {
        a[0] += b[0];
        a[1] += b[1];
        return a;
    };

    static final BinaryOperator<long[]> SummingLong_Combiner_3 = (a, b) -> {
        a[0] += b[0];
        a[1] += b[1];
        a[2] += b[2];
        return a;
    };

    static final Function<long[], Long> SummingLong_Finisher = a -> a[0];
    static final Function<long[], Tuple2<Long, Long>> SummingLong_Finisher_2 = a -> Tuple.of(a[0], a[1]);
    static final Function<long[], Tuple3<Long, Long, Long>> SummingLong_Finisher_3 = a -> Tuple.of(a[0], a[1], a[2]);

    static final Supplier<KahanSummation> SummingDouble_Supplier = KahanSummation::new;
    static final Supplier<KahanSummation[]> SummingDouble_Supplier_2 = () -> new KahanSummation[] { new KahanSummation(), new KahanSummation() };
    static final Supplier<KahanSummation[]> SummingDouble_Supplier_3 = () -> new KahanSummation[] { new KahanSummation(), new KahanSummation(),
            new KahanSummation() };

    static final BinaryOperator<KahanSummation> SummingDouble_Combiner = (a, b) -> {
        a.combine(b);
        return a;
    };

    static final BinaryOperator<KahanSummation[]> SummingDouble_Combiner_2 = (a, b) -> {
        a[0].combine(b[0]);
        a[1].combine(b[1]);
        return a;
    };

    static final BinaryOperator<KahanSummation[]> SummingDouble_Combiner_3 = (a, b) -> {
        a[0].combine(b[0]);
        a[1].combine(b[1]);
        a[2].combine(b[2]);
        return a;
    };

    static final Function<KahanSummation, Double> SummingDouble_Finisher = KahanSummation::sum;
    static final Function<KahanSummation[], Tuple2<Double, Double>> SummingDouble_Finisher_2 = a -> Tuple.of(a[0].sum(), a[1].sum());
    static final Function<KahanSummation[], Tuple3<Double, Double, Double>> SummingDouble_Finisher_3 = a -> Tuple.of(a[0].sum(), a[1].sum(), a[2].sum());

    static final Supplier<BigInteger[]> SummingBigInteger_Supplier = () -> new BigInteger[] { BigInteger.ZERO };
    static final Supplier<BigInteger[]> SummingBigInteger_Supplier_2 = () -> new BigInteger[] { BigInteger.ZERO, BigInteger.ZERO };
    static final Supplier<BigInteger[]> SummingBigInteger_Supplier_3 = () -> new BigInteger[] { BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO };

    static final BinaryOperator<BigInteger[]> SummingBigInteger_Combiner = (a, b) -> {
        a[0] = a[0].add(b[0]);
        return a;
    };

    static final BinaryOperator<BigInteger[]> SummingBigInteger_Combiner_2 = (a, b) -> {
        a[0] = a[0].add(b[0]);
        a[1] = a[1].add(b[1]);
        return a;
    };

    static final BinaryOperator<BigInteger[]> SummingBigInteger_Combiner_3 = (a, b) -> {
        a[0] = a[0].add(b[0]);
        a[1] = a[1].add(b[1]);
        a[2] = a[2].add(b[2]);
        return a;
    };

    static final Function<BigInteger[], BigInteger> SummingBigInteger_Finisher = a -> a[0];
    static final Function<BigInteger[], Tuple2<BigInteger, BigInteger>> SummingBigInteger_Finisher_2 = a -> Tuple.of(a[0], a[1]);
    static final Function<BigInteger[], Tuple3<BigInteger, BigInteger, BigInteger>> SummingBigInteger_Finisher_3 = a -> Tuple.of(a[0], a[1], a[2]);

    static final Supplier<BigDecimal[]> SummingBigDecimal_Supplier = () -> new BigDecimal[] { BigDecimal.ZERO };
    static final Supplier<BigDecimal[]> SummingBigDecimal_Supplier_2 = () -> new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO };
    static final Supplier<BigDecimal[]> SummingBigDecimal_Supplier_3 = () -> new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO };

    static final BinaryOperator<BigDecimal[]> SummingBigDecimal_Combiner = (a, b) -> {
        a[0] = a[0].add(b[0]);
        return a;
    };

    static final BinaryOperator<BigDecimal[]> SummingBigDecimal_Combiner_2 = (a, b) -> {
        a[0] = a[0].add(b[0]);
        a[1] = a[1].add(b[1]);
        return a;
    };

    static final BinaryOperator<BigDecimal[]> SummingBigDecimal_Combiner_3 = (a, b) -> {
        a[0] = a[0].add(b[0]);
        a[1] = a[1].add(b[1]);
        a[2] = a[2].add(b[2]);
        return a;
    };

    static final Function<BigDecimal[], BigDecimal> SummingBigDecimal_Finisher = a -> a[0];
    static final Function<BigDecimal[], Tuple2<BigDecimal, BigDecimal>> SummingBigDecimal_Finisher_2 = a -> Tuple.of(a[0], a[1]);
    static final Function<BigDecimal[], Tuple3<BigDecimal, BigDecimal, BigDecimal>> SummingBigDecimal_Finisher_3 = a -> Tuple.of(a[0], a[1], a[2]);

    static final Supplier<long[]> AveragingInt_Supplier = () -> new long[2];
    static final Supplier<Pair<long[], long[]>> AveragingInt_Supplier_2 = () -> Pair.of(new long[2], new long[2]);
    static final Supplier<Pair<long[], long[]>> AveragingInt_Supplier_3 = () -> Pair.of(new long[3], new long[3]);

    static final BinaryOperator<long[]> AveragingInt_Combiner = (a, b) -> {
        a[0] += b[0];
        a[1] += b[1];
        return a;
    };

    static final BinaryOperator<Pair<long[], long[]>> AveragingInt_Combiner_2 = (a, b) -> {
        a.left[0] += b.left[0];
        a.left[1] += b.left[1];
        a.right[0] += b.right[0];
        a.right[1] += b.right[1];
        return a;
    };

    static final BinaryOperator<Pair<long[], long[]>> AveragingInt_Combiner_3 = (a, b) -> {
        a.left[0] += b.left[0];
        a.left[1] += b.left[1];
        a.left[2] += b.left[2];
        a.right[0] += b.right[0];
        a.right[1] += b.right[1];
        a.right[2] += b.right[2];
        return a;
    };

    static final Function<long[], Double> AveragingInt_Finisher = a -> {
        if (a[1] == 0) {
            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
        }

        return ((double) a[0]) / a[1];
    };

    static final Function<long[], OptionalDouble> AveragingInt_Finisher_op = a -> {
        if (a[1] == 0) {
            return OptionalDouble.empty();
        } else {
            return OptionalDouble.of(((double) a[0]) / a[1]);
        }
    };

    static final Function<Pair<long[], long[]>, Tuple2<Double, Double>> AveragingInt_Finisher_2 = a -> {
        if (a.right[0] == 0 || a.right[1] == 0) {
            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
        }

        return Tuple2.of(((double) a.left[0]) / a.right[0], ((double) a.left[1]) / a.right[1]);
    };

    static final Function<Pair<long[], long[]>, Tuple3<Double, Double, Double>> AveragingInt_Finisher_3 = a -> {
        if (a.right[0] == 0 || a.right[1] == 0 || a.right[2] == 0) {
            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
        }

        return Tuple2.of(((double) a.left[0]) / a.right[0], ((double) a.left[1]) / a.right[1], ((double) a.left[2]) / a.right[2]);
    };

    static final Supplier<long[]> AveragingLong_Supplier = () -> new long[2];
    static final Supplier<Pair<long[], long[]>> AveragingLong_Supplier_2 = () -> Pair.of(new long[2], new long[2]);
    static final Supplier<Pair<long[], long[]>> AveragingLong_Supplier_3 = () -> Pair.of(new long[3], new long[3]);

    static final BinaryOperator<long[]> AveragingLong_Combiner = (a, b) -> {
        a[0] += b[0];
        a[1] += b[1];
        return a;
    };

    static final BinaryOperator<Pair<long[], long[]>> AveragingLong_Combiner_2 = (a, b) -> {
        a.left[0] += b.left[0];
        a.left[1] += b.left[1];
        a.right[0] += b.right[0];
        a.right[1] += b.right[1];
        return a;
    };

    static final BinaryOperator<Pair<long[], long[]>> AveragingLong_Combiner_3 = (a, b) -> {
        a.left[0] += b.left[0];
        a.left[1] += b.left[1];
        a.left[2] += b.left[2];
        a.right[0] += b.right[0];
        a.right[1] += b.right[1];
        a.right[2] += b.right[2];
        return a;
    };

    static final Function<long[], Double> AveragingLong_Finisher = a -> {
        if (a[1] == 0) {
            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
        }

        return ((double) a[0]) / a[1];
    };

    static final Function<long[], OptionalDouble> AveragingLong_Finisher_op = a -> {
        if (a[1] == 0) {
            return OptionalDouble.empty();
        } else {
            return OptionalDouble.of(((double) a[0]) / a[1]);
        }
    };

    static final Function<Pair<long[], long[]>, Tuple2<Double, Double>> AveragingLong_Finisher_2 = a -> {
        if (a.right[0] == 0 || a.right[1] == 0) {
            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
        }

        return Tuple2.of(((double) a.left[0]) / a.right[0], ((double) a.left[1]) / a.right[1]);
    };

    static final Function<Pair<long[], long[]>, Tuple3<Double, Double, Double>> AveragingLong_Finisher_3 = a -> {
        if (a.right[0] == 0 || a.right[1] == 0 || a.right[2] == 0) {
            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
        }

        return Tuple2.of(((double) a.left[0]) / a.right[0], ((double) a.left[1]) / a.right[1], ((double) a.left[2]) / a.right[2]);
    };

    static final Supplier<KahanSummation> AveragingDouble_Supplier = KahanSummation::new;
    static final Supplier<KahanSummation[]> AveragingDouble_Supplier_2 = () -> new KahanSummation[] { new KahanSummation(), new KahanSummation() };
    static final Supplier<KahanSummation[]> AveragingDouble_Supplier_3 = () -> new KahanSummation[] { new KahanSummation(), new KahanSummation(),
            new KahanSummation() };

    static final BinaryOperator<KahanSummation> AveragingDouble_Combiner = (a, b) -> {
        a.combine(b);
        return a;
    };

    static final BinaryOperator<KahanSummation[]> AveragingDouble_Combiner_2 = (a, b) -> {
        a[0].combine(b[0]);
        a[1].combine(b[1]);
        return a;
    };

    static final BinaryOperator<KahanSummation[]> AveragingDouble_Combiner_3 = (a, b) -> {
        a[0].combine(b[0]);
        a[1].combine(b[1]);
        a[2].combine(b[2]);
        return a;
    };

    static final Function<KahanSummation, Double> AveragingDouble_Finisher = a -> {
        if (a.count() == 0) {
            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
        }

        return a.average().get();
    };

    static final Function<KahanSummation, OptionalDouble> AveragingDouble_Finisher_op = KahanSummation::average;

    static final Function<KahanSummation[], Tuple2<Double, Double>> AveragingDouble_Finisher_2 = a -> {
        if (a[0].count() == 0 || a[1].count() == 0) {
            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
        }

        return Tuple.of(a[0].average().get(), a[1].average().get());
    };

    static final Function<KahanSummation[], Tuple3<Double, Double, Double>> AveragingDouble_Finisher_3 = a -> {
        if (a[0].count() == 0 || a[1].count() == 0 || a[2].count() == 0) {
            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
        }

        return Tuple.of(a[0].average().get(), a[1].average().get(), a[2].average().get());
    };

    static final Supplier<Pair<BigInteger, long[]>> AveragingBigInteger_Supplier = () -> Pair.of(BigInteger.ZERO, new long[1]);

    static final Supplier<Pair<BigInteger[], long[]>> AveragingBigInteger_Supplier_2 = () -> Pair.of(new BigInteger[] { BigInteger.ZERO, BigInteger.ZERO },
            new long[2]);

    static final Supplier<Pair<BigInteger[], long[]>> AveragingBigInteger_Supplier_3 = () -> Pair
            .of(new BigInteger[] { BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO }, new long[3]);

    static final BinaryOperator<Pair<BigInteger, long[]>> AveragingBigInteger_Combiner = (a, b) -> {
        a.setLeft(a.left.add(b.left));
        a.right[0] += b.right[0];
        return a;
    };

    static final BinaryOperator<Pair<BigInteger[], long[]>> AveragingBigInteger_Combiner_2 = (a, b) -> {
        a.left[0] = a.left[0].add(b.left[0]);
        a.left[1] = a.left[1].add(b.left[1]);
        a.right[0] += b.right[0];
        a.right[1] += b.right[1];
        return a;
    };

    static final BinaryOperator<Pair<BigInteger[], long[]>> AveragingBigInteger_Combiner_3 = (a, b) -> {
        a.left[0] = a.left[0].add(b.left[0]);
        a.left[1] = a.left[1].add(b.left[1]);
        a.left[2] = a.left[2].add(b.left[2]);
        a.right[0] += b.right[0];
        a.right[1] += b.right[1];
        a.right[2] += b.right[2];
        return a;
    };

    static final Function<Pair<BigInteger, long[]>, BigDecimal> AveragingBigInteger_Finisher = a -> {
        if (a.right[0] == 0) {
            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
        }

        return new BigDecimal(a.left).divide(new BigDecimal(a.right[0]));
    };

    static final Function<Pair<BigInteger, long[]>, Optional<BigDecimal>> AveragingBigInteger_Finisher_op = a -> a.right[0] == 0 ? Optional.<BigDecimal> empty()
            : Optional.of(new BigDecimal(a.left).divide(new BigDecimal(a.right[0])));

    static final Function<Pair<BigInteger[], long[]>, Tuple2<BigDecimal, BigDecimal>> AveragingBigInteger_Finisher_2 = a -> {
        if (a.right[0] == 0 || a.right[1] == 0) {
            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
        }

        return Tuple.of(new BigDecimal(a.left[0]).divide(new BigDecimal(a.right[0])), new BigDecimal(a.left[1]).divide(new BigDecimal(a.right[1])));
    };

    static final Function<Pair<BigInteger[], long[]>, Tuple3<BigDecimal, BigDecimal, BigDecimal>> AveragingBigInteger_Finisher_3 = a -> {
        if (a.right[0] == 0 || a.right[1] == 0 || a.right[2] == 0) {
            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
        }

        return Tuple.of(new BigDecimal(a.left[0]).divide(new BigDecimal(a.right[0])), new BigDecimal(a.left[1]).divide(new BigDecimal(a.right[1])),
                new BigDecimal(a.left[2]).divide(new BigDecimal(a.right[2])));
    };

    static final Supplier<Pair<BigDecimal, long[]>> AveragingBigDecimal_Supplier = () -> Pair.of(BigDecimal.ZERO, new long[1]);

    static final Supplier<Pair<BigDecimal[], long[]>> AveragingBigDecimal_Supplier_2 = () -> Pair.of(new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO },
            new long[2]);

    static final Supplier<Pair<BigDecimal[], long[]>> AveragingBigDecimal_Supplier_3 = () -> Pair
            .of(new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO }, new long[3]);

    static final BinaryOperator<Pair<BigDecimal, long[]>> AveragingBigDecimal_Combiner = (a, b) -> {
        a.setLeft(a.left.add(b.left));
        a.right[0] += b.right[0];
        return a;
    };

    static final BinaryOperator<Pair<BigDecimal[], long[]>> AveragingBigDecimal_Combiner_2 = (a, b) -> {
        a.left[0] = a.left[0].add(b.left[0]);
        a.left[1] = a.left[1].add(b.left[1]);
        a.right[0] += b.right[0];
        a.right[1] += b.right[1];
        return a;
    };

    static final BinaryOperator<Pair<BigDecimal[], long[]>> AveragingBigDecimal_Combiner_3 = (a, b) -> {
        a.left[0] = a.left[0].add(b.left[0]);
        a.left[1] = a.left[1].add(b.left[1]);
        a.left[2] = a.left[2].add(b.left[2]);
        a.right[0] += b.right[0];
        a.right[1] += b.right[1];
        a.right[2] += b.right[2];
        return a;
    };

    static final Function<Pair<BigDecimal, long[]>, BigDecimal> AveragingBigDecimal_Finisher = a -> {
        if (a.right[0] == 0) {
            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
        }

        return a.left.divide(new BigDecimal(a.right[0]));
    };

    static final Function<Pair<BigDecimal, long[]>, Optional<BigDecimal>> AveragingBigDecimal_Finisher_op = a -> a.right[0] == 0 ? Optional.<BigDecimal> empty()
            : Optional.of(a.left.divide(new BigDecimal(a.right[0])));

    static final Function<Pair<BigDecimal[], long[]>, Tuple2<BigDecimal, BigDecimal>> AveragingBigDecimal_Finisher_2 = a -> {
        if (a.right[0] == 0 || a.right[1] == 0) {
            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
        }

        return Tuple.of(a.left[0].divide(new BigDecimal(a.right[0])), a.left[1].divide(new BigDecimal(a.right[1])));
    };

    static final Function<Pair<BigDecimal[], long[]>, Tuple3<BigDecimal, BigDecimal, BigDecimal>> AveragingBigDecimal_Finisher_3 = a -> {
        if (a.right[0] == 0 || a.right[1] == 0 || a.right[2] == 0) {
            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
        }

        return Tuple.of(a.left[0].divide(new BigDecimal(a.right[0])), a.left[1].divide(new BigDecimal(a.right[1])),
                a.left[2].divide(new BigDecimal(a.right[2])));
    };

    static final Supplier<CharSummaryStatistics> SummarizingChar_Supplier = CharSummaryStatistics::new;

    static final BinaryOperator<CharSummaryStatistics> SummarizingChar_Combiner = (a, b) -> {
        a.combine(b);
        return a;
    };

    static final Supplier<ByteSummaryStatistics> SummarizingByte_Supplier = ByteSummaryStatistics::new;

    static final BinaryOperator<ByteSummaryStatistics> SummarizingByte_Combiner = (a, b) -> {
        a.combine(b);
        return a;
    };

    static final Supplier<ShortSummaryStatistics> SummarizingShort_Supplier = ShortSummaryStatistics::new;

    static final BinaryOperator<ShortSummaryStatistics> SummarizingShort_Combiner = (a, b) -> {
        a.combine(b);
        return a;
    };

    static final Supplier<IntSummaryStatistics> SummarizingInt_Supplier = IntSummaryStatistics::new;

    static final BinaryOperator<IntSummaryStatistics> SummarizingInt_Combiner = (a, b) -> {
        a.combine(b);
        return a;
    };

    static final Supplier<LongSummaryStatistics> SummarizingLong_Supplier = LongSummaryStatistics::new;

    static final BinaryOperator<LongSummaryStatistics> SummarizingLong_Combiner = (a, b) -> {
        a.combine(b);
        return a;
    };

    static final Supplier<FloatSummaryStatistics> SummarizingFloat_Supplier = FloatSummaryStatistics::new;

    static final BinaryOperator<FloatSummaryStatistics> SummarizingFloat_Combiner = (a, b) -> {
        a.combine(b);
        return a;
    };

    static final Supplier<DoubleSummaryStatistics> SummarizingDouble_Supplier = DoubleSummaryStatistics::new;

    static final BinaryOperator<DoubleSummaryStatistics> SummarizingDouble_Combiner = (a, b) -> {
        a.combine(b);
        return a;
    };

    static final Supplier<BigIntegerSummaryStatistics> SummarizingBigInteger_Supplier = BigIntegerSummaryStatistics::new;

    static final BinaryOperator<BigIntegerSummaryStatistics> SummarizingBigInteger_Combiner = (a, b) -> {
        a.combine(b);
        return a;
    };

    static final Supplier<BigDecimalSummaryStatistics> SummarizingBigDecimal_Supplier = BigDecimalSummaryStatistics::new;

    static final BinaryOperator<BigDecimalSummaryStatistics> SummarizingBigDecimal_Combiner = (a, b) -> {
        a.combine(b);
        return a;
    };

    static final Function<Holder<Object>, Object> Reducing_Finisher_0 = Holder::value;

    static final BiConsumer<OptHolder<Object>, Object> Reducing_Accumulator = OptHolder::accept;

    static final BinaryOperator<OptHolder<Object>> Reducing_Combiner = (a, b) -> {
        if (b.present) {
            a.accept(b.value);
        }

        return a;
    };

    static final Function<OptHolder<Object>, Optional<Object>> Reducing_Finisher = a -> a.present ? Optional.of(a.value) : (Optional<Object>) Optional.empty();

    static final BiConsumer<MappingOptHolder<Object, Object>, Object> Reducing_Accumulator_2 = MappingOptHolder::accept;

    static final BinaryOperator<MappingOptHolder<Object, Object>> Reducing_Combiner_2 = (a, b) -> {
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

    static final Function<MappingOptHolder<Object, Object>, Optional<Object>> Reducing_Finisher_2 = a -> a.present ? Optional.of(a.value)
            : (Optional<Object>) Optional.empty();

    // ============================================================================================================

    Collectors() {
    }

    static class CollectorImpl<T, A, R> implements Collector<T, A, R> {
        private static final Function<Object, Object> IDENTITY_FINISHER = t -> t;

        private final Supplier<A> supplier;
        private final BiConsumer<A, T> accumulator;
        private final BinaryOperator<A> combiner;
        private final Function<A, R> finisher;
        private final Set<Characteristics> characteristics;

        CollectorImpl(final Supplier<? extends A> supplier, final BiConsumer<? super A, ? super T> accumulator, final BinaryOperator<A> combiner,
                final Set<Characteristics> characteristics) {
            this(supplier, accumulator, combiner, (Function<A, R>) IDENTITY_FINISHER, characteristics);
        }

        @SuppressWarnings("rawtypes")
        CollectorImpl(final Supplier<? extends A> supplier, final BiConsumer<? super A, ? super T> accumulator, final BinaryOperator<A> combiner,
                final Function<? super A, ? extends R> finisher, final Set<Characteristics> characteristics) {
            this.supplier = (Supplier) supplier;
            this.accumulator = (BiConsumer) accumulator;
            this.combiner = combiner;
            this.finisher = (Function) finisher;
            this.characteristics = characteristics == null ? N.<Characteristics> emptySet() : characteristics;
        }

        @Override
        public BiConsumer<A, T> accumulator() {
            return accumulator;
        }

        @Override
        public Supplier<A> supplier() {
            return supplier;
        }

        @Override
        public BinaryOperator<A> combiner() {
            return combiner;
        }

        @Override
        public Function<A, R> finisher() {
            return finisher;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return characteristics;
        }
    }

    /**
     *
     *
     * @param <T>
     * @param <C>
     * @param collectionFactory
     * @return
     */
    public static <T, C extends Collection<T>> Collector<T, ?, C> toCollection(Supplier<? extends C> collectionFactory) {
        final BiConsumer<C, T> accumulator = BiConsumers.ofAdd();
        final BinaryOperator<C> combiner = BinaryOperators.<T, C> ofAddAllToBigger();

        return new CollectorImpl<>(collectionFactory, accumulator, combiner, CH_ID);
    }

    /**
     *
     *
     * @param <T>
     * @return
     */
    public static <T> Collector<T, ?, List<T>> toList() {
        final Supplier<List<T>> supplier = Suppliers.<T> ofList();

        return toCollection(supplier);
    }

    /**
     *
     *
     * @param <T>
     * @return
     */
    public static <T> Collector<T, ?, LinkedList<T>> toLinkedList() {
        final Supplier<LinkedList<T>> supplier = Suppliers.<T> ofLinkedList();

        return toCollection(supplier);
    }

    /**
     *
     *
     * @param <T>
     * @return
     */
    public static <T> Collector<T, ?, ImmutableList<T>> toImmutableList() {
        final Collector<T, ?, List<T>> downstream = toList();
        @SuppressWarnings("rawtypes")
        final Function<List<T>, ImmutableList<T>> finisher = (Function) ImmutableList_Finisher;

        return collectingAndThen(downstream, finisher);
    }

    /**
     *
     *
     * @param <T>
     * @return
     */
    public static <T> Collector<T, ?, Set<T>> toSet() {
        final Supplier<Set<T>> supplier = Suppliers.<T> ofSet();

        return toCollection(supplier);
    }

    /**
     *
     *
     * @param <T>
     * @return
     */
    public static <T> Collector<T, ?, Set<T>> toLinkedHashSet() {
        final Supplier<Set<T>> supplier = Suppliers.<T> ofLinkedHashSet();

        return toCollection(supplier);
    }

    /**
     *
     *
     * @param <T>
     * @return
     */
    public static <T> Collector<T, ?, ImmutableSet<T>> toImmutableSet() {
        final Collector<T, ?, Set<T>> downstream = toSet();
        @SuppressWarnings("rawtypes")
        final Function<Set<T>, ImmutableSet<T>> finisher = (Function) ImmutableSet_Finisher;

        return collectingAndThen(downstream, finisher);
    }

    /**
     *
     *
     * @param <T>
     * @return
     */
    public static <T> Collector<T, ?, Queue<T>> toQueue() {
        final Supplier<Queue<T>> supplier = Suppliers.<T> ofQueue();

        return toCollection(supplier);
    }

    /**
     *
     *
     * @param <T>
     * @return
     */
    public static <T> Collector<T, ?, Deque<T>> toDeque() {
        final Supplier<Deque<T>> supplier = Suppliers.<T> ofDeque();

        return toCollection(supplier);
    }

    /**
     *
     *
     * @param <T>
     * @param <C>
     * @param collectionFactory
     * @param atMostSize
     * @return
     */
    public static <T, C extends Collection<T>> Collector<T, ?, C> toCollection(final Supplier<? extends C> collectionFactory, final int atMostSize) {
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

        return new CollectorImpl<>(collectionFactory, accumulator, combiner, CH_ID);
    }

    /**
     *
     *
     * @param <T>
     * @param atMostSize
     * @return
     */
    public static <T> Collector<T, ?, List<T>> toList(final int atMostSize) {
        final Supplier<List<T>> supplier = () -> new ArrayList<>(N.min(256, atMostSize));

        return toCollection(supplier, atMostSize);
    }

    /**
     *
     *
     * @param <T>
     * @param atMostSize
     * @return
     */
    public static <T> Collector<T, ?, Set<T>> toSet(final int atMostSize) {
        final Supplier<Set<T>> supplier = () -> N.newHashSet(atMostSize);

        return toCollection(supplier, atMostSize);
    }

    /**
     *
     *
     * @param <T>
     * @return
     */
    public static <T> Collector<T, ?, Multiset<T>> toMultiset() {
        final Supplier<Multiset<T>> supplier = Suppliers.ofMultiset();

        return toMultiset(supplier);
    }

    /**
     *
     *
     * @param <T>
     * @param supplier
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> Collector<T, ?, Multiset<T>> toMultiset(Supplier<Multiset<T>> supplier) {
        final BiConsumer<Multiset<T>, T> accumulator = (BiConsumer) Multiset_Accumulator;
        final BinaryOperator<Multiset<T>> combiner = (BinaryOperator) Multiset_Combiner;

        return new CollectorImpl<>(supplier, accumulator, combiner, CH_UNORDERED_ID);
    }

    /**
     *
     *
     * @param <T>
     * @return
     */
    public static <T> Collector<T, ?, LongMultiset<T>> toLongMultiset() {
        final Supplier<LongMultiset<T>> supplier = Suppliers.ofLongMultiset();

        return toLongMultiset(supplier);
    }

    /**
     *
     *
     * @param <T>
     * @param supplier
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> Collector<T, ?, LongMultiset<T>> toLongMultiset(Supplier<LongMultiset<T>> supplier) {
        final BiConsumer<LongMultiset<T>, T> accumulator = (BiConsumer) LongMultiset_Accumulator;
        final BinaryOperator<LongMultiset<T>> combiner = (BinaryOperator) LongMultiset_Combiner;

        return new CollectorImpl<>(supplier, accumulator, combiner, CH_UNORDERED_ID);
    }

    /**
     *
     *
     * @param <T>
     * @return
     */
    public static <T> Collector<T, ?, Object[]> toArray() {
        return toArray(Suppliers.ofEmptyObjectArray());
    }

    /**
     *
     *
     * @param <T>
     * @param <A>
     * @param arraySupplier
     * @return
     */
    public static <T, A> Collector<T, ?, A[]> toArray(final Supplier<A[]> arraySupplier) {
        final Supplier<List<A>> supplier = Suppliers.<A> ofList();
        @SuppressWarnings("rawtypes")
        final BiConsumer<List<A>, T> accumulator = (BiConsumer) BiConsumers.ofAdd();
        final BinaryOperator<List<A>> combiner = BinaryOperators.<A, List<A>> ofAddAllToBigger();
        final Function<List<A>, A[]> finisher = t -> {
            final A[] a = arraySupplier.get();

            if (a.length >= t.size()) {
                return t.toArray(a);
            } else {
                return t.toArray((A[]) Array.newInstance(a.getClass().getComponentType(), t.size()));
            }
        };

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_NOID);
    }

    /**
     *
     *
     * @param <T>
     * @param <A>
     * @param arraySupplier
     * @return
     */
    public static <T, A> Collector<T, ?, A[]> toArray(final IntFunction<A[]> arraySupplier) {
        final Supplier<List<A>> supplier = Suppliers.<A> ofList();
        @SuppressWarnings("rawtypes")
        final BiConsumer<List<A>, T> accumulator = (BiConsumer) BiConsumers.ofAdd();
        final BinaryOperator<List<A>> combiner = BinaryOperators.<A, List<A>> ofAddAllToBigger();
        final Function<List<A>, A[]> finisher = t -> t.toArray(arraySupplier.apply(t.size()));

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_NOID);
    }

    /**
     *
     *
     * @return
     */
    public static Collector<Boolean, ?, BooleanList> toBooleanList() {
        final Supplier<BooleanList> supplier = Suppliers.ofBooleanList();
        final BiConsumer<BooleanList, Boolean> accumulator = BooleanList_Accumulator;
        final BinaryOperator<BooleanList> combiner = BooleanList_Combiner;

        return new CollectorImpl<>(supplier, accumulator, combiner, CH_ID);
    }

    /**
     *
     *
     * @return
     */
    public static Collector<Boolean, ?, boolean[]> toBooleanArray() {
        final Supplier<BooleanList> supplier = Suppliers.ofBooleanList();
        final BiConsumer<BooleanList, Boolean> accumulator = BooleanList_Accumulator;
        final BinaryOperator<BooleanList> combiner = BooleanList_Combiner;
        final Function<BooleanList, boolean[]> finisher = BooleanArray_Finisher;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_NOID);
    }

    /**
     *
     *
     * @return
     */
    public static Collector<Character, ?, CharList> toCharList() {
        final Supplier<CharList> supplier = Suppliers.ofCharList();
        final BiConsumer<CharList, Character> accumulator = CharList_Accumulator;
        final BinaryOperator<CharList> combiner = CharList_Combiner;

        return new CollectorImpl<>(supplier, accumulator, combiner, CH_ID);
    }

    /**
     *
     *
     * @return
     */
    public static Collector<Character, ?, char[]> toCharArray() {
        final Supplier<CharList> supplier = Suppliers.ofCharList();
        final BiConsumer<CharList, Character> accumulator = CharList_Accumulator;
        final BinaryOperator<CharList> combiner = CharList_Combiner;
        final Function<CharList, char[]> finisher = CharArray_Finisher;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_NOID);
    }

    /**
     *
     *
     * @return
     */
    public static Collector<Byte, ?, ByteList> toByteList() {
        final Supplier<ByteList> supplier = Suppliers.ofByteList();
        final BiConsumer<ByteList, Byte> accumulator = ByteList_Accumulator;
        final BinaryOperator<ByteList> combiner = ByteList_Combiner;

        return new CollectorImpl<>(supplier, accumulator, combiner, CH_ID);
    }

    /**
     *
     *
     * @return
     */
    public static Collector<Byte, ?, byte[]> toByteArray() {
        final Supplier<ByteList> supplier = Suppliers.ofByteList();
        final BiConsumer<ByteList, Byte> accumulator = ByteList_Accumulator;
        final BinaryOperator<ByteList> combiner = ByteList_Combiner;
        final Function<ByteList, byte[]> finisher = ByteArray_Finisher;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_NOID);
    }

    /**
     *
     *
     * @return
     */
    public static Collector<Short, ?, ShortList> toShortList() {
        final Supplier<ShortList> supplier = Suppliers.ofShortList();
        final BiConsumer<ShortList, Short> accumulator = ShortList_Accumulator;
        final BinaryOperator<ShortList> combiner = ShortList_Combiner;

        return new CollectorImpl<>(supplier, accumulator, combiner, CH_ID);
    }

    /**
     *
     *
     * @return
     */
    public static Collector<Short, ?, short[]> toShortArray() {
        final Supplier<ShortList> supplier = Suppliers.ofShortList();
        final BiConsumer<ShortList, Short> accumulator = ShortList_Accumulator;
        final BinaryOperator<ShortList> combiner = ShortList_Combiner;
        final Function<ShortList, short[]> finisher = ShortArray_Finisher;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_NOID);
    }

    /**
     *
     *
     * @return
     */
    public static Collector<Integer, ?, IntList> toIntList() {
        final Supplier<IntList> supplier = Suppliers.ofIntList();
        final BiConsumer<IntList, Integer> accumulator = IntList_Accumulator;
        final BinaryOperator<IntList> combiner = IntList_Combiner;

        return new CollectorImpl<>(supplier, accumulator, combiner, CH_ID);
    }

    /**
     *
     *
     * @return
     */
    public static Collector<Integer, ?, int[]> toIntArray() {
        final Supplier<IntList> supplier = Suppliers.ofIntList();
        final BiConsumer<IntList, Integer> accumulator = IntList_Accumulator;
        final BinaryOperator<IntList> combiner = IntList_Combiner;
        final Function<IntList, int[]> finisher = IntArray_Finisher;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_NOID);
    }

    /**
     *
     *
     * @return
     */
    public static Collector<Long, ?, LongList> toLongList() {
        final Supplier<LongList> supplier = Suppliers.ofLongList();
        final BiConsumer<LongList, Long> accumulator = LongList_Accumulator;
        final BinaryOperator<LongList> combiner = LongList_Combiner;

        return new CollectorImpl<>(supplier, accumulator, combiner, CH_ID);
    }

    /**
     *
     *
     * @return
     */
    public static Collector<Long, ?, long[]> toLongArray() {
        final Supplier<LongList> supplier = Suppliers.ofLongList();
        final BiConsumer<LongList, Long> accumulator = LongList_Accumulator;
        final BinaryOperator<LongList> combiner = LongList_Combiner;
        final Function<LongList, long[]> finisher = LongArray_Finisher;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_NOID);
    }

    /**
     *
     *
     * @return
     */
    public static Collector<Float, ?, FloatList> toFloatList() {
        final Supplier<FloatList> supplier = Suppliers.ofFloatList();
        final BiConsumer<FloatList, Float> accumulator = FloatList_Accumulator;
        final BinaryOperator<FloatList> combiner = FloatList_Combiner;

        return new CollectorImpl<>(supplier, accumulator, combiner, CH_ID);
    }

    /**
     *
     *
     * @return
     */
    public static Collector<Float, ?, float[]> toFloatArray() {
        final Supplier<FloatList> supplier = Suppliers.ofFloatList();
        final BiConsumer<FloatList, Float> accumulator = FloatList_Accumulator;
        final BinaryOperator<FloatList> combiner = FloatList_Combiner;
        final Function<FloatList, float[]> finisher = FloatArray_Finisher;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_NOID);
    }

    /**
     *
     *
     * @return
     */
    public static Collector<Double, ?, DoubleList> toDoubleList() {
        final Supplier<DoubleList> supplier = Suppliers.ofDoubleList();
        final BiConsumer<DoubleList, Double> accumulator = DoubleList_Accumulator;
        final BinaryOperator<DoubleList> combiner = DoubleList_Combiner;

        return new CollectorImpl<>(supplier, accumulator, combiner, CH_ID);
    }

    /**
     *
     *
     * @return
     */
    public static Collector<Double, ?, double[]> toDoubleArray() {
        final Supplier<DoubleList> supplier = Suppliers.ofDoubleList();
        final BiConsumer<DoubleList, Double> accumulator = DoubleList_Accumulator;
        final BinaryOperator<DoubleList> combiner = DoubleList_Combiner;
        final Function<DoubleList, double[]> finisher = DoubleArray_Finisher;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_NOID);
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
     * {@code TooManyElementsException} is threw if there are more than one values are collected.
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> Collector<T, ?, Optional<T>> onlyOne() {
        final Supplier<Holder<Optional<T>>> supplier = (Supplier) onlyOne_supplier;
        final BiConsumer<Holder<Optional<T>>, T> accumulator = (BiConsumer) onlyOne_accumulator;
        final BinaryOperator<Holder<Optional<T>>> combiner = (BinaryOperator) onlyOne_combiner;
        final Function<Holder<Optional<T>>, Optional<T>> finisher = (Function) onlyOne_finisher;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    /**
     * {@code TooManyElementsException} is threw if there are more than one values are collected.
     *
     * @param <T>
     * @param predicate
     * @return
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
            throw new UnsupportedOperationException("The 'first' and 'last' Collector only can be used in sequential stream"); //NOSONAR
        }

        return t.value() != NONE ? t : u;
    };

    private static final Function<Holder<Object>, Optional<Object>> first_last_finisher = t -> t.value() == NONE ? Optional.empty() : Optional.of(t.value());

    /**
     * Only works for sequential Stream.
     *
     * @param <T>
     * @return
     * @throws UnsupportedOperationException operated by multiple threads
     */
    @SuppressWarnings("rawtypes")
    public static <T> Collector<T, ?, Optional<T>> first() {
        final Supplier<Holder<T>> supplier = (Supplier) first_last_supplier;
        final BiConsumer<Holder<T>, T> accumulator = (BiConsumer) first_accumulator;
        final BinaryOperator<Holder<T>> combiner = (BinaryOperator) first_last_combiner;
        final Function<Holder<T>, Optional<T>> finisher = (Function) first_last_finisher;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_NOID);
    }

    /**
     * Only works for sequential Stream.
     *
     * @param <T>
     * @return
     * @throws UnsupportedOperationException operated by multiple threads
     */
    @SuppressWarnings("rawtypes")
    public static <T> Collector<T, ?, Optional<T>> last() {
        final Supplier<Holder<T>> supplier = (Supplier) first_last_supplier;
        final BiConsumer<Holder<T>, T> accumulator = (BiConsumer) last_accumulator;
        final BinaryOperator<Holder<T>> combiner = (BinaryOperator) first_last_combiner;
        final Function<Holder<T>, Optional<T>> finisher = (Function) first_last_finisher;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_NOID);
    }

    /**
     * Only works for sequential Stream.
     *
     * @param <T>
     * @param n
     * @return
     * @throws UnsupportedOperationException operated by multiple threads
     */
    public static <T> Collector<T, ?, List<T>> first(final int n) {
        N.checkArgNotNegative(n, "n");

        final Supplier<List<T>> supplier = () -> new ArrayList<>(N.min(256, n));

        final BiConsumer<List<T>, T> accumulator = (c, t) -> {
            if (c.size() < n) {
                c.add(t);
            }
        };

        final BinaryOperator<List<T>> combiner = (a, b) -> {
            if (N.notNullOrEmpty(a) && N.notNullOrEmpty(b)) {
                throw new UnsupportedOperationException("The 'first' and 'last' Collector only can be used in sequential stream");
            }

            return a.size() > 0 ? a : b;
        };

        return new CollectorImpl<>(supplier, accumulator, combiner, CH_ID);
    }

    /**
     * Only works for sequential Stream.
     *
     * @param <T>
     * @param n
     * @return
     * @throws UnsupportedOperationException operated by multiple threads
     */
    public static <T> Collector<T, ?, List<T>> last(final int n) {
        N.checkArgNotNegative(n, "n");

        final Supplier<Deque<T>> supplier = () -> n <= 1024 ? new ArrayDeque<>(n) : new LinkedList<>();

        final BiConsumer<Deque<T>, T> accumulator = (dqueue, t) -> {
            if (n > 0) {
                if (dqueue.size() >= n) {
                    dqueue.pollFirst();
                }

                dqueue.offerLast(t);
            }
        };

        final BinaryOperator<Deque<T>> combiner = (a, b) -> {
            if (N.notNullOrEmpty(a) && N.notNullOrEmpty(b)) {
                throw new UnsupportedOperationException("The 'first' and 'last' Collector only can be used in sequential stream");
            }

            while (b.size() < n && !a.isEmpty()) {
                b.addFirst(a.pollLast());
            }

            return b;
        };

        final Function<Deque<T>, List<T>> finisher = ArrayList::new;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_NOID);
    }

    /**
     *
     *
     * @return
     */
    public static Collector<CharSequence, ?, String> joining() {
        return joining("", "", "");
    }

    /**
     *
     *
     * @param delimiter
     * @return
     */
    public static Collector<CharSequence, ?, String> joining(CharSequence delimiter) {
        return joining(delimiter, "", "");
    }

    /**
     *
     *
     * @param delimiter
     * @param prefix
     * @param suffix
     * @return
     */
    public static Collector<CharSequence, ?, String> joining(final CharSequence delimiter, final CharSequence prefix, final CharSequence suffix) {
        final Supplier<Joiner> supplier = () -> Joiner.with(delimiter, prefix, suffix).reuseCachedBuffer();

        final BiConsumer<Joiner, CharSequence> accumulator = Joiner_Accumulator;
        final BinaryOperator<Joiner> combiner = Joiner_Combiner;
        final Function<Joiner, String> finisher = Joiner_Finisher;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_NOID);
    }

    /**
     * It's copied from StreamEx: https://github.com/amaembo/streamex under Apache License v2 and may be modified.
     * <br />
     *
     * Returns a {@code Collector} which filters input elements by the supplied
     * predicate, collecting them to the list.
     *
     * <p>
     * This method behaves like
     * {@code filtering(predicate, Collectors.toList())}.
     *
     * <p>
     * There are no guarantees on the type, mutability, serializability, or
     * thread-safety of the {@code List} returned.
     *
     * @param <T> the type of the input elements
     * @param predicate a filter function to be applied to the input elements
     * @return a collector which applies the predicate to the input elements and
     *         collects the elements for which predicate returned true to the
     *         {@code List}
     * @see #filtering(Predicate, Collector)
     * @since 0.6.0
     */
    @Beta
    public static <T> Collector<T, ?, List<T>> filtering(Predicate<? super T> predicate) {
        final Collector<? super T, ?, List<T>> downstream = Collectors.toList();

        return filtering(predicate, downstream);
    }

    /**
     * It's copied from StreamEx: https://github.com/amaembo/streamex under Apache License v2 and may be modified.
     * <br />
     *
     * Returns a {@code Collector} which passes only those elements to the
     * specified downstream collector which match given predicate.
     *
     * <p>
     * This method returns a
     * <a href="package-summary.html#ShortCircuitReduction">short-circuiting
     * collector</a> if downstream collector is short-circuiting.
     *
     * <p>
     * The operation performed by the returned collector is equivalent to
     * {@code stream.filter(predicate).collect(downstream)}. This collector is
     * mostly useful as a downstream collector in cascaded operation involving
     * {@link #pairing(Collector, Collector, BiFunction)} collector.
     *
     * <p>
     * This method is similar to {@code Collectors.filtering} method which
     * appears in JDK 9. However when downstream collector is
     * <a href="package-summary.html#ShortCircuitReduction">short-circuiting</a>
     * , this method will also return a short-circuiting collector.
     *
     * @param <T> the type of the input elements
     * @param <A> intermediate accumulation type of the downstream collector
     * @param <R> result type of collector
     * @param predicate a filter function to be applied to the input elements
     * @param downstream a collector which will accept filtered values
     * @return a collector which applies the predicate to the input elements and
     *         provides the elements for which predicate returned true to the
     *         downstream collector
     * @see #pairing(Collector, Collector, BiFunction)
     * @since 0.4.0
     */
    public static <T, A, R> Collector<T, ?, R> filtering(final Predicate<? super T> predicate, final Collector<? super T, A, R> downstream) {
        final BiConsumer<A, ? super T> downstreamAccumulator = downstream.accumulator();

        final BiConsumer<A, T> accumulator = (a, t) -> {
            if (predicate.test(t)) {
                downstreamAccumulator.accept(a, t);
            }
        };

        return new CollectorImpl<>(downstream.supplier(), accumulator, downstream.combiner(), downstream.finisher(), downstream.characteristics());
    }

    /**
     *
     *
     * @param <T>
     * @param <U>
     * @param mapper
     * @return
     */
    @Beta
    public static <T, U> Collector<T, ?, List<U>> mapping(Function<? super T, ? extends U> mapper) {
        return Collectors.mapping(mapper, Collectors.<U> toList());
    }

    /**
     *
     *
     * @param <T>
     * @param <U>
     * @param <A>
     * @param <R>
     * @param mapper
     * @param downstream
     * @return
     */
    public static <T, U, A, R> Collector<T, ?, R> mapping(final Function<? super T, ? extends U> mapper, final Collector<? super U, A, R> downstream) {
        final BiConsumer<A, ? super U> downstreamAccumulator = downstream.accumulator();

        final BiConsumer<A, T> accumulator = (a, t) -> downstreamAccumulator.accept(a, mapper.apply(t));

        return new CollectorImpl<>(downstream.supplier(), accumulator, downstream.combiner(), downstream.finisher(), downstream.characteristics());
    }

    /**
     *
     *
     * @param <T>
     * @param <U>
     * @param mapper
     * @return
     */
    @Beta
    public static <T, U> Collector<T, ?, List<U>> flatMaping(final Function<? super T, ? extends java.util.stream.Stream<? extends U>> mapper) {
        return flatMaping(mapper, Collectors.<U> toList());
    }

    /**
     *
     *
     * @param <T>
     * @param <U>
     * @param <A>
     * @param <R>
     * @param mapper
     * @param downstream
     * @return
     */
    public static <T, U, A, R> Collector<T, ?, R> flatMaping(final Function<? super T, ? extends java.util.stream.Stream<? extends U>> mapper,
            final Collector<? super U, A, R> downstream) {
        final BiConsumer<A, ? super U> downstreamAccumulator = downstream.accumulator();

        final BiConsumer<A, T> accumulator = (a, t) -> {
            try (java.util.stream.Stream<? extends U> stream = mapper.apply(t)) {
                final Iterator<? extends U> iter = stream.iterator();

                while (iter.hasNext()) {
                    downstreamAccumulator.accept(a, iter.next());
                }
            }
        };

        return new CollectorImpl<>(downstream.supplier(), accumulator, downstream.combiner(), downstream.finisher(), downstream.characteristics());
    }

    /**
     *
     *
     * @param <T>
     * @param <U>
     * @param mapper
     * @return
     */
    @Beta
    public static <T, U> Collector<T, ?, List<U>> flattMaping(final Function<? super T, ? extends Stream<? extends U>> mapper) {
        return flattMaping(mapper, Collectors.<U> toList());
    }

    /**
     *
     *
     * @param <T>
     * @param <U>
     * @param <A>
     * @param <R>
     * @param mapper
     * @param downstream
     * @return
     */
    public static <T, U, A, R> Collector<T, ?, R> flattMaping(final Function<? super T, ? extends Stream<? extends U>> mapper,
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

        return new CollectorImpl<>(downstream.supplier(), accumulator, downstream.combiner(), downstream.finisher(), downstream.characteristics());
    }

    /**
     *
     *
     * @param <T>
     * @param <U>
     * @param mapper
     * @return
     */
    @Beta
    public static <T, U> Collector<T, ?, List<U>> flatmapping(final Function<? super T, ? extends Collection<? extends U>> mapper) {
        return flatmapping(mapper, Collectors.<U> toList());
    }

    /**
     *
     *
     * @param <T>
     * @param <U>
     * @param <A>
     * @param <R>
     * @param mapper
     * @param downstream
     * @return
     */
    public static <T, U, A, R> Collector<T, ?, R> flatmapping(final Function<? super T, ? extends Collection<? extends U>> mapper,
            final Collector<? super U, A, R> downstream) {
        final BiConsumer<A, ? super U> downstreamAccumulator = downstream.accumulator();

        final BiConsumer<A, T> accumulator = (a, t) -> {
            final Collection<? extends U> c = mapper.apply(t);

            if (N.notNullOrEmpty(c)) {
                for (U u : c) {
                    downstreamAccumulator.accept(a, u);
                }
            }
        };

        return new CollectorImpl<>(downstream.supplier(), accumulator, downstream.combiner(), downstream.finisher(), downstream.characteristics());
    }

    /**
     *
     *
     * @param <T>
     * @param <T2>
     * @param <U>
     * @param flatMapper
     * @param mapper
     * @return
     */
    @Beta
    public static <T, T2, U> Collector<T, ?, List<U>> flatMaping(final Function<? super T, ? extends java.util.stream.Stream<? extends T2>> flatMapper,
            final BiFunction<? super T, ? super T2, ? extends U> mapper) {
        return flatMaping(flatMapper, mapper, Collectors.<U> toList());
    }

    /**
     *
     *
     * @param <T>
     * @param <T2>
     * @param <U>
     * @param <A>
     * @param <R>
     * @param flatMapper
     * @param mapper
     * @param downstream
     * @return
     */
    @Beta
    public static <T, T2, U, A, R> Collector<T, ?, R> flatMaping(final Function<? super T, ? extends java.util.stream.Stream<? extends T2>> flatMapper,
            final BiFunction<? super T, ? super T2, ? extends U> mapper, final Collector<? super U, A, R> downstream) {
        final BiConsumer<A, ? super U> downstreamAccumulator = downstream.accumulator();

        final BiConsumer<A, T> accumulator = (a, t) -> {
            try (java.util.stream.Stream<? extends T2> stream = flatMapper.apply(t)) {
                final Iterator<? extends T2> iter = stream.iterator();

                while (iter.hasNext()) {
                    downstreamAccumulator.accept(a, mapper.apply(t, iter.next()));
                }
            }
        };

        return new CollectorImpl<>(downstream.supplier(), accumulator, downstream.combiner(), downstream.finisher(), downstream.characteristics());
    }

    /**
     *
     *
     * @param <T>
     * @param <T2>
     * @param <U>
     * @param flatMapper
     * @param mapper
     * @return
     */
    @Beta
    public static <T, T2, U> Collector<T, ?, List<U>> flattMaping(final Function<? super T, ? extends Stream<? extends T2>> flatMapper,
            final BiFunction<? super T, ? super T2, ? extends U> mapper) {
        return flattMaping(flatMapper, mapper, Collectors.<U> toList());
    }

    /**
     *
     *
     * @param <T>
     * @param <T2>
     * @param <U>
     * @param <A>
     * @param <R>
     * @param flatMapper
     * @param mapper
     * @param downstream
     * @return
     */
    @Beta
    public static <T, T2, U, A, R> Collector<T, ?, R> flattMaping(final Function<? super T, ? extends Stream<? extends T2>> flatMapper,
            final BiFunction<? super T, ? super T2, ? extends U> mapper, final Collector<? super U, A, R> downstream) {
        final BiConsumer<A, ? super U> downstreamAccumulator = downstream.accumulator();

        final BiConsumer<A, T> accumulator = (a, t) -> {
            try (Stream<? extends T2> stream = flatMapper.apply(t)) {
                final ObjIterator<? extends T2> iter = StreamBase.iterate(stream);

                while (iter.hasNext()) {
                    downstreamAccumulator.accept(a, mapper.apply(t, iter.next()));
                }
            }
        };

        return new CollectorImpl<>(downstream.supplier(), accumulator, downstream.combiner(), downstream.finisher(), downstream.characteristics());
    }

    /**
     *
     *
     * @param <T>
     * @param <T2>
     * @param <U>
     * @param flatMapper
     * @param mapper
     * @return
     */
    @Beta
    public static <T, T2, U> Collector<T, ?, List<U>> flatmapping(final Function<? super T, ? extends Collection<? extends T2>> flatMapper,
            final BiFunction<? super T, ? super T2, ? extends U> mapper) {
        return flatmapping(flatMapper, mapper, Collectors.<U> toList());
    }

    /**
     *
     *
     * @param <T>
     * @param <T2>
     * @param <U>
     * @param <A>
     * @param <R>
     * @param flatMapper
     * @param mapper
     * @param downstream
     * @return
     */
    @Beta
    public static <T, T2, U, A, R> Collector<T, ?, R> flatmapping(final Function<? super T, ? extends Collection<? extends T2>> flatMapper,
            final BiFunction<? super T, ? super T2, ? extends U> mapper, final Collector<? super U, A, R> downstream) {
        final BiConsumer<A, ? super U> downstreamAccumulator = downstream.accumulator();

        final BiConsumer<A, T> accumulator = (a, t) -> {
            final Collection<? extends T2> c = flatMapper.apply(t);

            if (N.notNullOrEmpty(c)) {
                for (T2 t2 : c) {
                    downstreamAccumulator.accept(a, mapper.apply(t, t2));
                }
            }
        };

        return new CollectorImpl<>(downstream.supplier(), accumulator, downstream.combiner(), downstream.finisher(), downstream.characteristics());
    }

    /**
     *
     *
     * @param <T>
     * @param <A>
     * @param <R>
     * @param <RR>
     * @param downstream
     * @param finisher
     * @return
     */
    public static <T, A, R, RR> Collector<T, A, RR> collectingAndThen(final Collector<T, A, R> downstream, final Function<R, RR> finisher) {
        N.checkArgNotNull(downstream);
        N.checkArgNotNull(finisher);

        final Function<A, R> downstreamFinisher = downstream.finisher();

        final Function<A, RR> thenFinisher = t -> finisher.apply(downstreamFinisher.apply(t));

        Set<Characteristics> characteristics = downstream.characteristics();

        if (characteristics.contains(Characteristics.IDENTITY_FINISH)) {
            if (characteristics.size() == 1) {
                characteristics = Collectors.CH_NOID;
            } else {
                characteristics = EnumSet.copyOf(characteristics);
                characteristics.remove(Characteristics.IDENTITY_FINISH);
                characteristics = Collections.unmodifiableSet(characteristics);
            }
        }

        return new CollectorImpl<>(downstream.supplier(), downstream.accumulator(), downstream.combiner(), thenFinisher, characteristics);
    }

    /**
     *
     *
     * @param <T>
     * @param <A>
     * @param <R>
     * @param collector
     * @return
     */
    @Beta
    public static <T, A, R> Collector<T, A, Optional<R>> collectingOrEmpty(final Collector<T, A, R> collector) {
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
                characteristics = Collectors.CH_NOID;
            } else {
                characteristics = EnumSet.copyOf(characteristics);
                characteristics.remove(Characteristics.IDENTITY_FINISH);
                characteristics = Collections.unmodifiableSet(characteristics);
            }
        }

        return new CollectorImpl<>(collector.supplier(), newAccumulator, collector.combiner(), newFinisher, characteristics);
    }

    /**
     *
     *
     * @param <T>
     * @param <A>
     * @param <R>
     * @param collector
     * @param defaultForEmpty
     * @return
     */
    @Beta
    public static <T, A, R> Collector<T, A, R> collectingOrElseIfEmpty(final Collector<T, A, R> collector, final R defaultForEmpty) {
        return collectingOrElseGetIfEmpty(collector, () -> defaultForEmpty);
    }

    /**
     *
     *
     * @param <T>
     * @param <A>
     * @param <R>
     * @param collector
     * @param defaultForEmpty
     * @return
     */
    @Beta
    public static <T, A, R> Collector<T, A, R> collectingOrElseGetIfEmpty(final Collector<T, A, R> collector, final Supplier<? extends R> defaultForEmpty) {
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
                characteristics = Collectors.CH_NOID;
            } else {
                characteristics = EnumSet.copyOf(characteristics);
                characteristics.remove(Characteristics.IDENTITY_FINISH);
                characteristics = Collections.unmodifiableSet(characteristics);
            }
        }

        return new CollectorImpl<>(collector.supplier(), newAccumulator, collector.combiner(), newFinisher, characteristics);
    }

    /**
     *
     *
     * @param <T>
     * @param <A>
     * @param <R>
     * @param collector
     * @return
     */
    @Beta
    public static <T, A, R> Collector<T, A, R> collectingOrElseThrowIfEmpty(final Collector<T, A, R> collector) {
        return collectingOrElseGetIfEmpty(collector, () -> {
            throw noSuchElementExceptionSupplier.get();
        });
    }

    /**
     *
     *
     * @param <T>
     * @param <A>
     * @param <R>
     * @param collector
     * @param exceptionSupplier
     * @return
     */
    @Beta
    public static <T, A, R> Collector<T, A, R> collectingOrElseThrowIfEmpty(final Collector<T, A, R> collector,
            final Supplier<? extends RuntimeException> exceptionSupplier) {
        return collectingOrElseGetIfEmpty(collector, () -> {
            throw exceptionSupplier.get();
        });
    }

    /**
     * It's copied from StreamEx: https://github.com/amaembo/streamex under Apache License v2 and may be modified.
     * <br />
     *
     * Returns a {@code Collector} which collects into the {@link List} the
     * input elements for which given mapper function returns distinct results.
     *
     * <p>
     * For ordered source the order of collected elements is preserved. If the
     * same result is returned by mapper function for several elements, only the
     * first element is included into the resulting list.
     *
     * <p>
     * There are no guarantees on the type, mutability, serializability, or
     * thread-safety of the {@code List} returned.
     *
     * <p>
     * The operation performed by the returned collector is equivalent to
     * {@code stream.distinct(mapper).toList()}, but may work faster.
     *
     * @param <T> the type of the input elements
     * @param keyExtractor a function which classifies input elements.
     * @return a collector which collects distinct elements to the {@code List}.
     * @since 0.3.8
     */
    public static <T> Collector<T, ?, List<T>> distinctBy(final Function<? super T, ?> keyExtractor) {
        return distinctBy(keyExtractor, Suppliers.ofList());
    }

    /**
     *
     *
     * @param <T>
     * @param <C>
     * @param keyExtractor
     * @param suppplier
     * @return
     */
    public static <T, C extends Collection<T>> Collector<T, ?, C> distinctBy(final Function<? super T, ?> keyExtractor, final Supplier<? extends C> suppplier) {
        final Supplier<Map<Object, T>> supplier = Suppliers.<Object, T> ofLinkedHashMap();

        final BiConsumer<Map<Object, T>, T> accumulator = (map, t) -> {
            final Object key = keyExtractor.apply(t);

            if (!map.containsKey(key)) {
                map.put(key, t);
            }
        };

        final BinaryOperator<Map<Object, T>> combiner = (a, b) -> {
            for (Map.Entry<Object, T> entry : b.entrySet()) {
                if (!a.containsKey(entry.getKey())) {
                    a.put(entry.getKey(), entry.getValue());
                }
            }

            return a;
        };

        final Function<Map<Object, T>, C> finisher = map -> {
            final C c = suppplier.get();
            c.addAll(map.values());
            return c;
        };

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    /**
     * It's copied from StreamEx: https://github.com/amaembo/streamex under Apache License v2 and may be modified.
     * <br />
     *
     * Returns a {@code Collector} which counts a number of distinct values the
     * mapper function returns for the stream elements.
     *
     * <p>
     * The operation performed by the returned collector is equivalent to
     * {@code stream.map(mapper).distinct().count()}. This collector is mostly
     * useful as a downstream collector.
     *
     * @param <T> the type of the input elements
     * @param keyExtractor a function which classifies input elements.
     * @return a collector which counts a number of distinct classes the mapper
     *         function returns for the stream elements.
     */
    public static <T> Collector<T, ?, Integer> distinctCount(final Function<? super T, ?> keyExtractor) {
        final Supplier<Set<Object>> supplier = Suppliers.<Object> ofSet();

        final BiConsumer<Set<Object>, T> accumulator = (c, t) -> c.add(keyExtractor.apply(t));

        final BinaryOperator<Set<Object>> combiner = BinaryOperators.<Object, Set<Object>> ofAddAllToBigger();

        final Function<Set<Object>, Integer> finisher = Set::size;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    /**
     *
     *
     * @param <T>
     * @return
     */
    public static <T> Collector<T, ?, Long> counting() {
        return summingLong(it -> 1L);
    }

    /**
     *
     *
     * @param <T>
     * @return
     */
    public static <T> Collector<T, ?, Integer> countingToInt() {
        return summingInt(it -> 1);
    }

    /**
     *
     *
     * @param <T>
     * @return
     */
    public static <T extends Comparable<? super T>> Collector<T, ?, Optional<T>> min() {
        return min(Fn.nullsLast());
    }

    /**
     *
     *
     * @param <T>
     * @param comparator
     * @return
     */
    public static <T> Collector<T, ?, Optional<T>> min(final Comparator<? super T> comparator) {
        N.checkArgNotNull(comparator);

        final BinaryOperator<T> op = (a, b) -> comparator.compare(a, b) <= 0 ? a : b;

        return reducing(op);
    }

    /**
     *
     *
     * @param <T>
     * @param defaultForEmpty
     * @return
     */
    public static <T extends Comparable<? super T>> Collector<T, ?, T> minOrElse(final T defaultForEmpty) {
        return minOrElseGet(() -> defaultForEmpty);
    }

    /**
     *
     *
     * @param <T>
     * @param comparator
     * @param defaultForEmpty
     * @return
     */
    public static <T> Collector<T, ?, T> minOrElse(final Comparator<? super T> comparator, final T defaultForEmpty) {
        return minOrElseGet(comparator, () -> defaultForEmpty);
    }

    /**
     *
     *
     * @param <T>
     * @param supplierForEmpty
     * @return
     */
    public static <T extends Comparable<? super T>> Collector<T, ?, T> minOrElseGet(final Supplier<? extends T> supplierForEmpty) {
        return minOrElseGet(Fn.nullsLast(), supplierForEmpty);
    }

    /**
     *
     *
     * @param <T>
     * @param comparator
     * @param supplierForEmpty
     * @return
     */
    public static <T> Collector<T, ?, T> minOrElseGet(final Comparator<? super T> comparator, final Supplier<? extends T> supplierForEmpty) {
        N.checkArgNotNull(comparator);

        final BinaryOperator<T> op = (a, b) -> comparator.compare(a, b) <= 0 ? a : b;

        return reducingOrElseGet(op, supplierForEmpty);
    }

    /**
     *
     *
     * @param <T>
     * @return
     */
    public static <T extends Comparable<? super T>> Collector<T, ?, T> minOrElseThrow() {
        return minOrElseThrow(Fn.nullsLast());
    }

    /**
     *
     *
     * @param <T>
     * @param comparator
     * @return
     */
    public static <T> Collector<T, ?, T> minOrElseThrow(final Comparator<? super T> comparator) {
        return minOrElseThrow(comparator, noSuchElementExceptionSupplier);
    }

    /**
     *
     *
     * @param <T>
     * @param comparator
     * @param exceptionSupplier
     * @return
     */
    public static <T> Collector<T, ?, T> minOrElseThrow(final Comparator<? super T> comparator, final Supplier<? extends RuntimeException> exceptionSupplier) {
        N.checkArgNotNull(comparator);

        final BinaryOperator<T> op = (a, b) -> comparator.compare(a, b) <= 0 ? a : b;

        return reducingOrElseThrow(op, exceptionSupplier);
    }

    private static final Supplier<NoSuchElementException> noSuchElementExceptionSupplier = NoSuchElementException::new;

    /**
     *
     *
     * @param <T>
     * @param keyMapper
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> Collector<T, ?, Optional<T>> minBy(final Function<? super T, ? extends Comparable> keyMapper) {
        return min(Comparators.comparingBy(keyMapper));
    }

    /**
     *
     *
     * @param <T>
     * @param keyMapper
     * @param supplierForEmpty
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> Collector<T, ?, T> minByOrElseGet(final Function<? super T, ? extends Comparable> keyMapper,
            final Supplier<? extends T> supplierForEmpty) {
        return minOrElseGet(Comparators.comparingBy(keyMapper), supplierForEmpty);
    }

    /**
     *
     *
     * @param <T>
     * @param keyMapper
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> Collector<T, ?, T> minByOrElseThrow(final Function<? super T, ? extends Comparable> keyMapper) {
        return minOrElseThrow(Comparators.comparingBy(keyMapper));
    }

    /**
     *
     *
     * @param <T>
     * @param keyMapper
     * @param exceptionSupplier
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> Collector<T, ?, T> minByOrElseThrow(final Function<? super T, ? extends Comparable> keyMapper,
            final Supplier<? extends RuntimeException> exceptionSupplier) {
        return minOrElseThrow(Comparators.comparingBy(keyMapper), exceptionSupplier);
    }

    /**
     *
     *
     * @param <T>
     * @return
     */
    public static <T extends Comparable<? super T>> Collector<T, ?, Optional<T>> max() {
        return max(Fn.nullsFirst());
    }

    /**
     *
     *
     * @param <T>
     * @param comparator
     * @return
     */
    public static <T> Collector<T, ?, Optional<T>> max(final Comparator<? super T> comparator) {
        N.checkArgNotNull(comparator);

        final BinaryOperator<T> op = (a, b) -> comparator.compare(a, b) >= 0 ? a : b;

        return reducing(op);
    }

    /**
     *
     *
     * @param <T>
     * @param defaultForEmpty
     * @return
     */
    public static <T extends Comparable<? super T>> Collector<T, ?, T> maxOrElse(final T defaultForEmpty) {
        return maxOrElseGet(() -> defaultForEmpty);
    }

    /**
     *
     *
     * @param <T>
     * @param comparator
     * @param defaultForEmpty
     * @return
     */
    public static <T> Collector<T, ?, T> maxOrElse(final Comparator<? super T> comparator, final T defaultForEmpty) {
        return maxOrElseGet(comparator, () -> defaultForEmpty);
    }

    /**
     *
     *
     * @param <T>
     * @param supplierForEmpty
     * @return
     */
    public static <T extends Comparable<? super T>> Collector<T, ?, T> maxOrElseGet(final Supplier<? extends T> supplierForEmpty) {
        return maxOrElseGet(Fn.nullsFirst(), supplierForEmpty);
    }

    /**
     *
     *
     * @param <T>
     * @param comparator
     * @param supplierForEmpty
     * @return
     */
    public static <T> Collector<T, ?, T> maxOrElseGet(final Comparator<? super T> comparator, final Supplier<? extends T> supplierForEmpty) {
        N.checkArgNotNull(comparator);

        final BinaryOperator<T> op = (a, b) -> comparator.compare(a, b) >= 0 ? a : b;

        return reducingOrElseGet(op, supplierForEmpty);
    }

    /**
     *
     *
     * @param <T>
     * @return
     */
    public static <T extends Comparable<? super T>> Collector<T, ?, T> maxOrElseThrow() {
        return maxOrElseThrow(Fn.nullsFirst());
    }

    /**
     *
     *
     * @param <T>
     * @param comparator
     * @return
     */
    public static <T> Collector<T, ?, T> maxOrElseThrow(final Comparator<? super T> comparator) {
        return maxOrElseThrow(comparator, noSuchElementExceptionSupplier);
    }

    /**
     *
     *
     * @param <T>
     * @param comparator
     * @param exceptionSupplier
     * @return
     */
    public static <T> Collector<T, ?, T> maxOrElseThrow(final Comparator<? super T> comparator, final Supplier<? extends RuntimeException> exceptionSupplier) {
        N.checkArgNotNull(comparator);

        final BinaryOperator<T> op = (a, b) -> comparator.compare(a, b) >= 0 ? a : b;

        return reducingOrElseThrow(op, exceptionSupplier);
    }

    /**
     *
     *
     * @param <T>
     * @param keyMapper
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> Collector<T, ?, Optional<T>> maxBy(final Function<? super T, ? extends Comparable> keyMapper) {
        return max(Comparators.comparingBy(keyMapper));
    }

    /**
     *
     *
     * @param <T>
     * @param keyMapper
     * @param supplierForEmpty
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> Collector<T, ?, T> maxByOrElseGet(final Function<? super T, ? extends Comparable> keyMapper,
            final Supplier<? extends T> supplierForEmpty) {
        return maxOrElseGet(Comparators.comparingBy(keyMapper), supplierForEmpty);
    }

    /**
     *
     *
     * @param <T>
     * @param keyMapper
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> Collector<T, ?, T> maxByOrElseThrow(final Function<? super T, ? extends Comparable> keyMapper) {
        return maxOrElseThrow(Comparators.comparingBy(keyMapper));
    }

    /**
     *
     *
     * @param <T>
     * @param keyMapper
     * @param exceptionSupplier
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> Collector<T, ?, T> maxByOrElseThrow(final Function<? super T, ? extends Comparable> keyMapper,
            final Supplier<? extends RuntimeException> exceptionSupplier) {
        return maxOrElseThrow(Comparators.comparingBy(keyMapper), exceptionSupplier);
    }

    /**
     * It's copied from StreamEx: https://github.com/amaembo/streamex under Apache License v2 and may be modified.
     * <br />
     *
     * Returns a {@code Collector} which finds all the elements which are equal
     * to each other and smaller than any other element according to the natural
     * order. The found elements are collected to {@link List}.
     *
     * @param <T> the type of the input elements
     * @return a {@code Collector} which finds all the minimal elements and
     *         collects them to the {@code List}.
     * @see #minAll(Comparator)
     * @see #minAll(Collector)
     */
    public static <T extends Comparable<? super T>> Collector<T, ?, List<T>> minAll() {
        return minAll(Fn.nullsLast());
    }

    /**
     * It's copied from StreamEx: https://github.com/amaembo/streamex under Apache License v2 and may be modified.
     * <br />
     *
     * Returns a {@code Collector} which finds all the elements which are equal
     * to each other and smaller than any other element according to the
     * specified {@link Comparator}. The found elements are collected to
     * {@link List}.
     *
     * @param <T> the type of the input elements
     * @param comparator a {@code Comparator} to compare the elements
     * @return a {@code Collector} which finds all the minimal elements and
     *         collects them to the {@code List}.
     * @see #minAll(Comparator, Collector)
     * @see #minAll()
     */
    public static <T> Collector<T, ?, List<T>> minAll(Comparator<? super T> comparator) {
        return minAll(comparator, Integer.MAX_VALUE);
    }

    /**
     *
     *
     * @param <T>
     * @param comparator
     * @param atMostSize
     * @return
     */
    public static <T> Collector<T, ?, List<T>> minAll(Comparator<? super T> comparator, int atMostSize) {
        return maxAll(Fn.reversedOrder(comparator), atMostSize);
    }

    /**
     * It's copied from StreamEx: https://github.com/amaembo/streamex under Apache License v2 and may be modified.
     * <br />
     *
     * Returns a {@code Collector} which finds all the elements which are equal
     * to each other and smaller than any other element according to the natural
     * order. The found elements are reduced using the specified downstream
     * {@code Collector}.
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
    public static <T extends Comparable, A, R> Collector<T, ?, R> minAll(Collector<T, A, R> downstream) {
        return minAll(Fn.nullsLast(), downstream);
    }

    /**
     * It's copied from StreamEx: https://github.com/amaembo/streamex under Apache License v2 and may be modified.
     * <br />
     *
     * Returns a {@code Collector} which finds all the elements which are equal
     * to each other and smaller than any other element according to the
     * specified {@link Comparator}. The found elements are reduced using the
     * specified downstream {@code Collector}.
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
    public static <T, A, R> Collector<T, ?, R> minAll(Comparator<? super T> comparator, Collector<T, A, R> downstream) {
        return maxAll(Fn.reversedOrder(comparator), downstream);
    }

    /**
     *
     *
     * @param <T>
     * @param <R>
     * @param downstream
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable, R> Collector<T, ?, Optional<Pair<T, R>>> minAlll(Collector<T, ?, R> downstream) {
        return minAlll(Fn.nullsLast(), downstream);
    }

    /**
     *
     *
     * @param <T>
     * @param <R>
     * @param comparator
     * @param downstream
     * @return
     */
    public static <T, R> Collector<T, ?, Optional<Pair<T, R>>> minAlll(final Comparator<? super T> comparator, final Collector<? super T, ?, R> downstream) {
        return minAlll(comparator, downstream, Fn.<Optional<Pair<T, R>>> identity());
    }

    /**
     *
     *
     * @param <T>
     * @param <R>
     * @param <RR>
     * @param comparator
     * @param downstream
     * @param finisher
     * @return
     */
    public static <T, R, RR> Collector<T, ?, RR> minAlll(final Comparator<? super T> comparator, final Collector<? super T, ?, R> downstream,
            final Function<Optional<Pair<T, R>>, RR> finisher) {
        return maxAlll(Fn.reversedOrder(comparator), downstream, finisher);
    }

    /**
     * It's copied from StreamEx: https://github.com/amaembo/streamex under Apache License v2 and may be modified.
     * <br />
     *
     * Returns a {@code Collector} which finds all the elements which are equal
     * to each other and bigger than any other element according to the natural
     * order. The found elements are collected to {@link List}.
     *
     * @param <T> the type of the input elements
     * @return a {@code Collector} which finds all the maximal elements and
     *         collects them to the {@code List}.
     * @see #maxAll(Comparator)
     * @see #maxAll(Collector)
     */
    public static <T extends Comparable<? super T>> Collector<T, ?, List<T>> maxAll() {
        return maxAll(Fn.nullsFirst());
    }

    /**
     * It's copied from StreamEx: https://github.com/amaembo/streamex under Apache License v2 and may be modified.
     * <br />
     *
     * Returns a {@code Collector} which finds all the elements which are equal
     * to each other and bigger than any other element according to the
     * specified {@link Comparator}. The found elements are collected to
     * {@link List}.
     *
     * @param <T> the type of the input elements
     * @param comparator a {@code Comparator} to compare the elements
     * @return a {@code Collector} which finds all the maximal elements and
     *         collects them to the {@code List}.
     * @see #maxAll(Comparator, Collector)
     * @see #maxAll()
     */
    public static <T> Collector<T, ?, List<T>> maxAll(Comparator<? super T> comparator) {
        return maxAll(comparator, Integer.MAX_VALUE);
    }

    /**
     *
     *
     * @param <T>
     * @param comparator
     * @param atMostSize
     * @return
     */
    public static <T> Collector<T, ?, List<T>> maxAll(final Comparator<? super T> comparator, final int atMostSize) {
        final Supplier<Pair<T, List<T>>> supplier = () -> {
            final List<T> list = new ArrayList<>(Math.min(16, atMostSize));
            return Pair.of((T) NONE, list);
        };

        final BiConsumer<Pair<T, List<T>>, T> accumulator = (a, t) -> {
            if (a.left == NONE) {
                a.left = t;

                if (a.right.size() < atMostSize) {
                    a.right.add(t);
                }
            } else {
                int cmp = comparator.compare(t, a.left);

                if (cmp > 0) {
                    a.left = t;
                    a.right.clear();
                }

                if ((cmp >= 0) && (a.right.size() < atMostSize)) {
                    a.right.add(t);
                }
            }
        };

        final BinaryOperator<Pair<T, List<T>>> combiner = (a, b) -> {
            if (b.left == NONE) {
                return a;
            } else if (a.left == NONE) {
                return b;
            }

            int cmp = comparator.compare(a.left, b.left);

            if (cmp > 0) {
                return a;
            } else if (cmp < 0) {
                return b;
            }

            if (a.right.size() < atMostSize) {
                if (b.right.size() <= atMostSize - a.right.size()) {
                    a.right.addAll(b.right);
                } else {
                    a.right.addAll(b.right.subList(0, atMostSize - a.right.size()));
                }
            }

            return a;
        };

        final Function<Pair<T, List<T>>, List<T>> finisher = a -> a.right;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    /**
     * It's copied from StreamEx: https://github.com/amaembo/streamex under Apache License v2 and may be modified.
     * <br />
     *
     * Returns a {@code Collector} which finds all the elements which are equal
     * to each other and bigger than any other element according to the natural
     * order. The found elements are reduced using the specified downstream
     * {@code Collector}.
     *
     * @param <T> the type of the input elements
     * @param <A> the intermediate accumulation type of the downstream collector
     * @param <R> the result type of the downstream reduction
     * @param downstream a {@code Collector} implementing the downstream
     *        reduction
     * @return a {@code Collector} which finds all the maximal elements.
     * @see #maxAll(Comparator, Collector)
     * @see #maxAll(Comparator)
     * @see #maxAll()
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable, R> Collector<T, ?, R> maxAll(Collector<T, ?, R> downstream) {
        return maxAll(Fn.nullsFirst(), downstream);
    }

    /**
     * It's copied from StreamEx: https://github.com/amaembo/streamex under Apache License v2 and may be modified.
     * <br />
     *
     * Returns a {@code Collector} which finds all the elements which are equal
     * to each other and bigger than any other element according to the
     * specified {@link Comparator}. The found elements are reduced using the
     * specified downstream {@code Collector}.
     *
     * @param <T> the type of the input elements
     * @param <A> the intermediate accumulation type of the downstream collector
     * @param <R> the result type of the downstream reduction
     * @param comparator a {@code Comparator} to compare the elements
     * @param downstream a {@code Collector} implementing the downstream
     *        reduction
     * @return a {@code Collector} which finds all the maximal elements.
     * @see #maxAll(Comparator)
     * @see #maxAll(Collector)
     * @see #maxAll()
     */
    public static <T, R> Collector<T, ?, R> maxAll(final Comparator<? super T> comparator, final Collector<? super T, ?, R> downstream) {
        final Supplier<Object> downstreamSupplier = (Supplier<Object>) downstream.supplier();
        final BiConsumer<Object, ? super T> downstreamAccumulator = (BiConsumer<Object, ? super T>) downstream.accumulator();
        final BinaryOperator<Object> downstreamCombiner = (BinaryOperator<Object>) downstream.combiner();
        final Function<Object, R> downstreamFinisher = (Function<Object, R>) downstream.finisher();
        final MutableBoolean isCollection = MutableBoolean.of(false);
        final MutableBoolean isMap = MutableBoolean.of(false);

        final Supplier<Pair<T, Object>> supplier = new Supplier<>() {
            @SuppressWarnings("rawtypes")
            @Override
            public Pair<T, Object> get() {
                final Object container = downstreamSupplier.get();

                if (container instanceof Collection && ((Collection) container).size() == 0) {
                    try {
                        ((Collection) container).clear();

                        isCollection.setTrue();
                    } catch (Exception e) {
                        // ignore
                    }
                } else if (container instanceof Map && ((Map) container).size() == 0) {
                    try {
                        ((Map) container).clear();

                        isMap.setTrue();
                    } catch (Exception e) {
                        // ignore
                    }
                }

                return Pair.of((T) none(), container);
            }
        };

        final BiConsumer<Pair<T, Object>, T> accumulator = new BiConsumer<>() {
            @SuppressWarnings("rawtypes")
            @Override
            public void accept(Pair<T, Object> a, T t) {
                if (a.left == NONE) {
                    a.left = t;
                    downstreamAccumulator.accept(a.right, t);
                } else {
                    final int cmp = comparator.compare(t, a.left);

                    if (cmp > 0) {
                        if (isCollection.isTrue()) {
                            ((Collection) a.right).clear();
                        } else if (isMap.isTrue()) {
                            ((Map) a.right).clear();
                        } else {
                            a.right = downstreamSupplier.get();
                        }

                        a.left = t;
                    }

                    if (cmp >= 0) {
                        downstreamAccumulator.accept(a.right, t);
                    }
                }
            }
        };

        final BinaryOperator<Pair<T, Object>> combiner = (a, b) -> {
            if (b.left == NONE) {
                return a;
            } else if (a.left == NONE) {
                return b;
            }

            final int cmp = comparator.compare(a.left, b.left);

            if (cmp > 0) {
                return a;
            } else if (cmp < 0) {
                return b;
            }

            a.right = downstreamCombiner.apply(a.right, b.right);

            return a;
        };

        final Function<Pair<T, Object>, R> finisher = t -> downstreamFinisher.apply(t.right);

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    /**
     *
     *
     * @param <T>
     * @param <R>
     * @param downstream
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable, R> Collector<T, ?, Optional<Pair<T, R>>> maxAlll(Collector<T, ?, R> downstream) {
        return maxAlll(Fn.nullsFirst(), downstream);
    }

    /**
     *
     *
     * @param <T>
     * @param <R>
     * @param comparator
     * @param downstream
     * @return
     */
    public static <T, R> Collector<T, ?, Optional<Pair<T, R>>> maxAlll(final Comparator<? super T> comparator, final Collector<? super T, ?, R> downstream) {
        return maxAlll(comparator, downstream, Fn.<Optional<Pair<T, R>>> identity());
    }

    /**
     *
     *
     * @param <T>
     * @param <R>
     * @param <RR>
     * @param comparator
     * @param downstream
     * @param finisher
     * @return
     */
    public static <T, R, RR> Collector<T, ?, RR> maxAlll(final Comparator<? super T> comparator, final Collector<? super T, ?, R> downstream,
            final Function<Optional<Pair<T, R>>, RR> finisher) {
        final Supplier<Object> downstreamSupplier = (Supplier<Object>) downstream.supplier();
        final BiConsumer<Object, ? super T> downstreamAccumulator = (BiConsumer<Object, ? super T>) downstream.accumulator();
        final BinaryOperator<Object> downstreamCombiner = (BinaryOperator<Object>) downstream.combiner();
        final Function<Object, R> downstreamFinisher = (Function<Object, R>) downstream.finisher();
        final MutableBoolean isCollection = MutableBoolean.of(false);
        final MutableBoolean isMap = MutableBoolean.of(false);

        final Supplier<Pair<T, Object>> supplier = new Supplier<>() {
            @SuppressWarnings("rawtypes")
            @Override
            public Pair<T, Object> get() {
                final Object container = downstreamSupplier.get();

                if (container instanceof Collection && ((Collection) container).size() == 0) {
                    try {
                        ((Collection) container).clear();

                        isCollection.setTrue();
                    } catch (Exception e) {
                        // ignore
                    }
                } else if (container instanceof Map && ((Map) container).size() == 0) {
                    try {
                        ((Map) container).clear();

                        isMap.setTrue();
                    } catch (Exception e) {
                        // ignore
                    }
                }

                return Pair.of((T) none(), container);
            }
        };

        final BiConsumer<Pair<T, Object>, T> accumulator = new BiConsumer<>() {
            @SuppressWarnings("rawtypes")
            @Override
            public void accept(Pair<T, Object> a, T t) {
                if (a.left == NONE) {
                    a.left = t;
                    downstreamAccumulator.accept(a.right, t);
                } else {
                    final int cmp = comparator.compare(t, a.left);

                    if (cmp > 0) {
                        if (isCollection.isTrue()) {
                            ((Collection) a.right).clear();
                        } else if (isMap.isTrue()) {
                            ((Map) a.right).clear();
                        } else {
                            a.right = downstreamSupplier.get();
                        }

                        a.left = t;
                    }

                    if (cmp >= 0) {
                        downstreamAccumulator.accept(a.right, t);
                    }
                }
            }
        };

        final BinaryOperator<Pair<T, Object>> combiner = (a, b) -> {
            if (b.left == NONE) {
                return a;
            } else if (a.left == NONE) {
                return b;
            }

            final int cmp = comparator.compare(a.left, b.left);

            if (cmp > 0) {
                return a;
            } else if (cmp < 0) {
                return b;
            } else {
                a.right = downstreamCombiner.apply(a.right, b.right);
                return a;
            }
        };

        final Function<Pair<T, Object>, RR> finalFinisher = a -> {
            final Optional<Pair<T, R>> result = a.left == NONE ? Optional.empty() : Optional.of(Pair.of(a.left, downstreamFinisher.apply(a.right)));

            return finisher.apply(result);
        };

        return new CollectorImpl<>(supplier, accumulator, combiner, finalFinisher, CH_UNORDERED_NOID);
    }

    /**
     *
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> Collector<T, ?, Optional<Pair<T, T>>> minMax() {
        return minMax(Fn.naturalOrder());
    }

    /**
     *
     *
     * @param <T>
     * @param comparator
     * @return
     * @see Collectors#minMax(Comparator, BiFunction)
     */
    public static <T> Collector<T, ?, Optional<Pair<T, T>>> minMax(final Comparator<? super T> comparator) {
        return minMax(comparator, Fn.<T, T> pair());
    }

    /**
     * It's copied from StreamEx: https://github.com/amaembo/streamex under Apache License v2 and may be modified.
     * <br />
     *
     * Returns a {@code Collector} which finds the minimal and maximal element
     * according to the supplied comparator, then applies finisher function to
     * them producing the final result.
     *
     * <p>
     * This collector produces stable result for ordered stream: if several
     * minimal or maximal elements appear, the collector always selects the
     * first encountered.
     *
     * <p>
     * If there are no input elements, the finisher method is not called and
     * empty {@code Optional} is returned. Otherwise the finisher result is
     * wrapped into {@code Optional}.
     *
     * @param <T> the type of the input elements
     * @param <R> the type of the result wrapped into {@code Optional}
     * @param comparator comparator which is used to find minimal and maximal
     *        element
     * @param finisher a {@link BiFunction} which takes minimal and maximal
     *        element and produces the final result.
     * @return a {@code Collector} which finds minimal and maximal elements.
     */
    public static <T, R> Collector<T, ?, Optional<R>> minMax(final Comparator<? super T> comparator,
            final BiFunction<? super T, ? super T, ? extends R> finisher) {

        final BiFunction<Optional<T>, Optional<T>, Optional<R>> finisher2 = (min,
                max) -> min.isPresent() ? Optional.of((R) finisher.apply(min.get(), max.get())) : Optional.<R> empty();

        return MoreCollectors.combine(Collectors.min(comparator), Collectors.max(comparator), finisher2);
    }

    /**
     *
     *
     * @param <T>
     * @param keyMapper
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> Collector<T, ?, Optional<Pair<T, T>>> minMaxBy(final Function<? super T, ? extends Comparable> keyMapper) {
        return minMax(Comparators.comparingBy(keyMapper));
    }

    /**
     *
     *
     * @param <T>
     * @param <R>
     * @param keyMapper
     * @param finisher
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T, R> Collector<T, ?, Optional<R>> minMaxBy(final Function<? super T, ? extends Comparable> keyMapper,
            final BiFunction<? super T, ? super T, ? extends R> finisher) {
        return minMax(Comparators.comparingBy(keyMapper), finisher);
    }

    /**
     *
     *
     * @param <T>
     * @param supplierForEmpty
     * @return
     */
    public static <T extends Comparable<? super T>> Collector<T, ?, Pair<T, T>> minMaxOrElseGet(
            final Supplier<Pair<? extends T, ? extends T>> supplierForEmpty) {
        return minMaxOrElseGet(Fn.naturalOrder(), supplierForEmpty);
    }

    /**
     *
     *
     * @param <T>
     * @param comparator
     * @param supplierForEmpty
     * @return
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
     *
     *
     * @param <T>
     * @return
     */
    public static <T extends Comparable<? super T>> Collector<T, ?, Pair<T, T>> minMaxOrElseThrow() {
        return minMaxOrElseThrow(Fn.naturalOrder());
    }

    /**
     *
     *
     * @param <T>
     * @param comparator
     * @return
     */
    public static <T> Collector<T, ?, Pair<T, T>> minMaxOrElseThrow(final Comparator<? super T> comparator) {
        return MoreCollectors.combine(Collectors.minOrElseThrow(comparator), Collectors.maxOrElseThrow(comparator), Fn.<T, T> pair());
    }

    @SuppressWarnings("unchecked")
    static <T> T none() { //NOSONAR
        return (T) NONE;
    }

    /**
     *
     *
     * @param <T>
     * @param mapper
     * @return
     */
    public static <T> Collector<T, ?, Integer> summingInt(final ToIntFunction<? super T> mapper) {
        final BiConsumer<int[], T> accumulator = (a, t) -> a[0] += mapper.applyAsInt(t);

        return new CollectorImpl<>(SummingInt_Supplier, accumulator, SummingInt_Combiner, SummingInt_Finisher, CH_UNORDERED_NOID);
    }

    /**
     *
     *
     * @param <T>
     * @param mapper
     * @return
     */
    public static <T> Collector<T, ?, Long> summingIntToLong(final ToIntFunction<? super T> mapper) {
        final BiConsumer<long[], T> accumulator = (a, t) -> a[0] += mapper.applyAsInt(t);

        return new CollectorImpl<>(SummingIntToLong_Supplier, accumulator, SummingIntToLong_Combiner, SummingIntToLong_Finisher, CH_UNORDERED_NOID);
    }

    /**
     *
     *
     * @param <T>
     * @param mapper
     * @return
     */
    public static <T> Collector<T, ?, Long> summingLong(final ToLongFunction<? super T> mapper) {
        final BiConsumer<long[], T> accumulator = (a, t) -> a[0] += mapper.applyAsLong(t);

        return new CollectorImpl<>(SummingLong_Supplier, accumulator, SummingLong_Combiner, SummingLong_Finisher, CH_UNORDERED_NOID);
    }

    /**
     *
     *
     * @param <T>
     * @param mapper
     * @return
     */
    public static <T> Collector<T, ?, Double> summingDouble(final ToDoubleFunction<? super T> mapper) {
        final BiConsumer<KahanSummation, T> accumulator = (a, t) -> a.add(mapper.applyAsDouble(t));

        return new CollectorImpl<>(SummingDouble_Supplier, accumulator, SummingDouble_Combiner, SummingDouble_Finisher, CH_UNORDERED_NOID);
    }

    /**
     *
     *
     * @param <T>
     * @param mapper
     * @return
     */
    public static <T> Collector<T, ?, BigInteger> summingBigInteger(final Function<? super T, BigInteger> mapper) {
        final BiConsumer<BigInteger[], T> accumulator = (a, t) -> a[0] = a[0].add(mapper.apply(t));

        return new CollectorImpl<>(SummingBigInteger_Supplier, accumulator, SummingBigInteger_Combiner, SummingBigInteger_Finisher, CH_UNORDERED_NOID);
    }

    /**
     *
     *
     * @param <T>
     * @param mapper
     * @return
     */
    public static <T> Collector<T, ?, BigDecimal> summingBigDecimal(final Function<? super T, BigDecimal> mapper) {
        final BiConsumer<BigDecimal[], T> accumulator = (a, t) -> a[0] = a[0].add(mapper.apply(t));

        return new CollectorImpl<>(SummingBigDecimal_Supplier, accumulator, SummingBigDecimal_Combiner, SummingBigDecimal_Finisher, CH_UNORDERED_NOID);
    }

    /**
     *
     *
     * @param <T>
     * @param mapper
     * @return
     */
    public static <T> Collector<T, ?, OptionalDouble> averagingInt(final ToIntFunction<? super T> mapper) {
        final BiConsumer<long[], T> accumulator = (a, t) -> {
            a[0] += mapper.applyAsInt(t);
            a[1]++;
        };

        return new CollectorImpl<>(AveragingInt_Supplier, accumulator, AveragingInt_Combiner, AveragingInt_Finisher_op, CH_UNORDERED_NOID);
    }

    /**
     *
     *
     * @param <T>
     * @param mapper
     * @return
     */
    public static <T> Collector<T, ?, Double> averagingIntOrElseThrow(final ToIntFunction<? super T> mapper) {
        final BiConsumer<long[], T> accumulator = (a, t) -> {
            a[0] += mapper.applyAsInt(t);
            a[1]++;
        };

        return new CollectorImpl<>(AveragingInt_Supplier, accumulator, AveragingInt_Combiner, AveragingInt_Finisher, CH_UNORDERED_NOID);
    }

    /**
     *
     *
     * @param <T>
     * @param mapper
     * @return
     */
    public static <T> Collector<T, ?, OptionalDouble> averagingLong(final ToLongFunction<? super T> mapper) {
        final BiConsumer<long[], T> accumulator = (a, t) -> {
            a[0] += mapper.applyAsLong(t);
            a[1]++;
        };

        return new CollectorImpl<>(AveragingLong_Supplier, accumulator, AveragingLong_Combiner, AveragingLong_Finisher_op, CH_UNORDERED_NOID);
    }

    /**
     *
     *
     * @param <T>
     * @param mapper
     * @return
     */
    public static <T> Collector<T, ?, Double> averagingLongOrElseThrow(final ToLongFunction<? super T> mapper) {
        final BiConsumer<long[], T> accumulator = (a, t) -> {
            a[0] += mapper.applyAsLong(t);
            a[1]++;
        };

        return new CollectorImpl<>(AveragingLong_Supplier, accumulator, AveragingLong_Combiner, AveragingLong_Finisher, CH_UNORDERED_NOID);
    }

    /**
     *
     *
     * @param <T>
     * @param mapper
     * @return
     */
    public static <T> Collector<T, ?, OptionalDouble> averagingDouble(final ToDoubleFunction<? super T> mapper) {
        final BiConsumer<KahanSummation, T> accumulator = (a, t) -> a.add(mapper.applyAsDouble(t));

        return new CollectorImpl<>(AveragingDouble_Supplier, accumulator, AveragingDouble_Combiner, AveragingDouble_Finisher_op, CH_UNORDERED_NOID);
    }

    /**
     *
     *
     * @param <T>
     * @param mapper
     * @return
     */
    public static <T> Collector<T, ?, Double> averagingDoubleOrElseThrow(final ToDoubleFunction<? super T> mapper) {
        final BiConsumer<KahanSummation, T> accumulator = (a, t) -> a.add(mapper.applyAsDouble(t));

        return new CollectorImpl<>(AveragingDouble_Supplier, accumulator, AveragingDouble_Combiner, AveragingDouble_Finisher, CH_UNORDERED_NOID);
    }

    /**
     *
     *
     * @param <T>
     * @param mapper
     * @return
     */
    public static <T> Collector<T, ?, Optional<BigDecimal>> averagingBigInteger(final Function<? super T, BigInteger> mapper) {
        final BiConsumer<Pair<BigInteger, long[]>, T> accumulator = (a, t) -> {
            a.setLeft(a.left.add(mapper.apply(t)));
            a.right[0] += 1;
        };

        return new CollectorImpl<>(AveragingBigInteger_Supplier, accumulator, AveragingBigInteger_Combiner, AveragingBigInteger_Finisher_op, CH_UNORDERED_NOID);
    }

    /**
     *
     *
     * @param <T>
     * @param mapper
     * @return
     */
    public static <T> Collector<T, ?, BigDecimal> averagingBigIntegerOrElseThrow(final Function<? super T, BigInteger> mapper) {
        final BiConsumer<Pair<BigInteger, long[]>, T> accumulator = (a, t) -> {
            a.setLeft(a.left.add(mapper.apply(t)));
            a.right[0] += 1;
        };

        return new CollectorImpl<>(AveragingBigInteger_Supplier, accumulator, AveragingBigInteger_Combiner, AveragingBigInteger_Finisher, CH_UNORDERED_NOID);
    }

    /**
     *
     *
     * @param <T>
     * @param mapper
     * @return
     */
    public static <T> Collector<T, ?, Optional<BigDecimal>> averagingBigDecimal(final Function<? super T, BigDecimal> mapper) {
        final BiConsumer<Pair<BigDecimal, long[]>, T> accumulator = (a, t) -> {
            a.setLeft(a.left.add(mapper.apply(t)));
            a.right[0] += 1;
        };

        return new CollectorImpl<>(AveragingBigDecimal_Supplier, accumulator, AveragingBigDecimal_Combiner, AveragingBigDecimal_Finisher_op, CH_UNORDERED_NOID);
    }

    /**
     *
     *
     * @param <T>
     * @param mapper
     * @return
     */
    public static <T> Collector<T, ?, BigDecimal> averagingBigDecimalOrElseThrow(final Function<? super T, BigDecimal> mapper) {
        final BiConsumer<Pair<BigDecimal, long[]>, T> accumulator = (a, t) -> {
            a.setLeft(a.left.add(mapper.apply(t)));
            a.right[0] += 1;
        };

        return new CollectorImpl<>(AveragingBigDecimal_Supplier, accumulator, AveragingBigDecimal_Combiner, AveragingBigDecimal_Finisher, CH_UNORDERED_NOID);
    }

    /**
     *
     *
     * @param <T>
     * @param mapper
     * @return
     */
    public static <T> Collector<T, ?, CharSummaryStatistics> summarizingChar(final ToCharFunction<? super T> mapper) {
        final Supplier<CharSummaryStatistics> supplier = SummarizingChar_Supplier;

        final BiConsumer<CharSummaryStatistics, T> accumulator = (a, t) -> a.accept(mapper.applyAsChar(t));

        final BinaryOperator<CharSummaryStatistics> combiner = SummarizingChar_Combiner;

        return new CollectorImpl<>(supplier, accumulator, combiner, CH_UNORDERED_ID);
    }

    /**
     *
     *
     * @param <T>
     * @param mapper
     * @return
     */
    public static <T> Collector<T, ?, ByteSummaryStatistics> summarizingByte(final ToByteFunction<? super T> mapper) {
        final Supplier<ByteSummaryStatistics> supplier = SummarizingByte_Supplier;

        final BiConsumer<ByteSummaryStatistics, T> accumulator = (a, t) -> a.accept(mapper.applyAsByte(t));

        final BinaryOperator<ByteSummaryStatistics> combiner = SummarizingByte_Combiner;

        return new CollectorImpl<>(supplier, accumulator, combiner, CH_UNORDERED_ID);
    }

    /**
     *
     *
     * @param <T>
     * @param mapper
     * @return
     */
    public static <T> Collector<T, ?, ShortSummaryStatistics> summarizingShort(final ToShortFunction<? super T> mapper) {
        final Supplier<ShortSummaryStatistics> supplier = SummarizingShort_Supplier;

        final BiConsumer<ShortSummaryStatistics, T> accumulator = (a, t) -> a.accept(mapper.applyAsShort(t));

        final BinaryOperator<ShortSummaryStatistics> combiner = SummarizingShort_Combiner;

        return new CollectorImpl<>(supplier, accumulator, combiner, CH_UNORDERED_ID);
    }

    /**
     *
     *
     * @param <T>
     * @param mapper
     * @return
     */
    public static <T> Collector<T, ?, IntSummaryStatistics> summarizingInt(final ToIntFunction<? super T> mapper) {
        final Supplier<IntSummaryStatistics> supplier = SummarizingInt_Supplier;

        final BiConsumer<IntSummaryStatistics, T> accumulator = (a, t) -> a.accept(mapper.applyAsInt(t));

        final BinaryOperator<IntSummaryStatistics> combiner = SummarizingInt_Combiner;

        return new CollectorImpl<>(supplier, accumulator, combiner, CH_UNORDERED_ID);
    }

    /**
     *
     *
     * @param <T>
     * @param mapper
     * @return
     */
    public static <T> Collector<T, ?, LongSummaryStatistics> summarizingLong(final ToLongFunction<? super T> mapper) {
        final Supplier<LongSummaryStatistics> supplier = SummarizingLong_Supplier;

        final BiConsumer<LongSummaryStatistics, T> accumulator = (a, t) -> a.accept(mapper.applyAsLong(t));

        final BinaryOperator<LongSummaryStatistics> combiner = SummarizingLong_Combiner;

        return new CollectorImpl<>(supplier, accumulator, combiner, CH_UNORDERED_ID);
    }

    /**
     *
     *
     * @param <T>
     * @param mapper
     * @return
     */
    public static <T> Collector<T, ?, FloatSummaryStatistics> summarizingFloat(final ToFloatFunction<? super T> mapper) {
        final Supplier<FloatSummaryStatistics> supplier = SummarizingFloat_Supplier;

        final BiConsumer<FloatSummaryStatistics, T> accumulator = (a, t) -> a.accept(mapper.applyAsFloat(t));

        final BinaryOperator<FloatSummaryStatistics> combiner = SummarizingFloat_Combiner;

        return new CollectorImpl<>(supplier, accumulator, combiner, CH_UNORDERED_ID);
    }

    /**
     *
     *
     * @param <T>
     * @param mapper
     * @return
     */
    public static <T> Collector<T, ?, DoubleSummaryStatistics> summarizingDouble(final ToDoubleFunction<? super T> mapper) {
        final Supplier<DoubleSummaryStatistics> supplier = SummarizingDouble_Supplier;

        final BiConsumer<DoubleSummaryStatistics, T> accumulator = (a, t) -> a.accept(mapper.applyAsDouble(t));

        final BinaryOperator<DoubleSummaryStatistics> combiner = SummarizingDouble_Combiner;

        return new CollectorImpl<>(supplier, accumulator, combiner, CH_UNORDERED_ID);
    }

    /**
     *
     *
     * @param <T>
     * @param mapper
     * @return
     */
    public static <T> Collector<T, ?, BigIntegerSummaryStatistics> summarizingBigInteger(final Function<? super T, BigInteger> mapper) {
        final Supplier<BigIntegerSummaryStatistics> supplier = SummarizingBigInteger_Supplier;

        final BiConsumer<BigIntegerSummaryStatistics, T> accumulator = (a, t) -> a.accept(mapper.apply(t));

        final BinaryOperator<BigIntegerSummaryStatistics> combiner = SummarizingBigInteger_Combiner;

        return new CollectorImpl<>(supplier, accumulator, combiner, CH_UNORDERED_ID);
    }

    /**
     *
     *
     * @param <T>
     * @param mapper
     * @return
     */
    public static <T> Collector<T, ?, BigDecimalSummaryStatistics> summarizingBigDecimal(final Function<? super T, BigDecimal> mapper) {
        final Supplier<BigDecimalSummaryStatistics> supplier = SummarizingBigDecimal_Supplier;

        final BiConsumer<BigDecimalSummaryStatistics, T> accumulator = (a, t) -> a.accept(mapper.apply(t));

        final BinaryOperator<BigDecimalSummaryStatistics> combiner = SummarizingBigDecimal_Combiner;

        return new CollectorImpl<>(supplier, accumulator, combiner, CH_UNORDERED_ID);
    }

    /**
     *
     *
     * @param <T>
     * @param identity
     * @param op
     * @return
     */
    public static <T> Collector<T, ?, T> reducing(final T identity, final BinaryOperator<T> op) {
        final BiConsumer<Holder<T>, T> accumulator = (a, t) -> a.setValue(op.apply(a.value(), t));

        final BinaryOperator<Holder<T>> combiner = (a, b) -> {
            a.setValue(op.apply(a.value(), b.value()));
            return a;
        };

        @SuppressWarnings("rawtypes")
        final Function<Holder<T>, T> finisher = (Function) Reducing_Finisher_0;

        return new CollectorImpl<>(holderSupplier(identity), accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    /**
     *
     *
     * @param <T>
     * @param op
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> Collector<T, ?, Optional<T>> reducing(final BinaryOperator<T> op) {
        final Supplier<OptHolder<T>> supplier = () -> new OptHolder<>(op);

        final BiConsumer<OptHolder<T>, T> accumulator = (BiConsumer) Reducing_Accumulator;
        final BinaryOperator<OptHolder<T>> combiner = (BinaryOperator) Reducing_Combiner;
        final Function<OptHolder<T>, Optional<T>> finisher = (Function) Reducing_Finisher;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    /**
     *
     *
     * @param <T>
     * @param op
     * @param supplierForEmpty
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> Collector<T, ?, T> reducingOrElseGet(final BinaryOperator<T> op, final Supplier<? extends T> supplierForEmpty) {
        final Supplier<OptHolder<T>> supplier = () -> new OptHolder<>(op);

        final BiConsumer<OptHolder<T>, T> accumulator = (BiConsumer) Reducing_Accumulator;
        final BinaryOperator<OptHolder<T>> combiner = (BinaryOperator) Reducing_Combiner;

        final Function<OptHolder<T>, T> finisher = a -> a.present ? a.value : supplierForEmpty.get();

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    /**
     *
     *
     * @param <T>
     * @param op
     * @return
     */
    public static <T> Collector<T, ?, T> reducingOrElseThrow(final BinaryOperator<T> op) {
        return reducingOrElseThrow(op, noSuchElementExceptionSupplier);
    }

    /**
     *
     *
     * @param <T>
     * @param op
     * @param exceptionSupplier
     * @return
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

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    /**
     *
     *
     * @param <T>
     * @param <R>
     * @param identity
     * @param mapper
     * @param op
     * @return
     */
    public static <T, R> Collector<T, ?, R> reducing(final R identity, final Function<? super T, ? extends R> mapper, final BinaryOperator<R> op) {
        final BiConsumer<Holder<R>, T> accumulator = (a, t) -> a.setValue(op.apply(a.value(), mapper.apply(t)));

        final BinaryOperator<Holder<R>> combiner = (a, b) -> {
            a.setValue(op.apply(a.value(), b.value()));

            return a;
        };

        @SuppressWarnings("rawtypes")
        final Function<Holder<R>, R> finisher = (Function) Reducing_Finisher_0;

        return new CollectorImpl<>(holderSupplier(identity), accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    /**
     *
     *
     * @param <T>
     * @param <R>
     * @param mapper
     * @param op
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T, R> Collector<T, ?, Optional<R>> reducing(final Function<? super T, ? extends R> mapper, final BinaryOperator<R> op) {
        final Supplier<MappingOptHolder<T, R>> supplier = () -> new MappingOptHolder<>(mapper, op);

        final BiConsumer<MappingOptHolder<T, R>, T> accumulator = (BiConsumer) Reducing_Accumulator_2;
        final BinaryOperator<MappingOptHolder<T, R>> combiner = (BinaryOperator) Reducing_Combiner_2;
        final Function<MappingOptHolder<T, R>, Optional<R>> finisher = (Function) Reducing_Finisher_2;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    @SuppressWarnings("unchecked")
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
        public void accept(T t) {
            if (present) {
                value = op.apply(value, t);
            } else {
                value = t;
                present = true;
            }
        }
    }

    private static class MappingOptHolder<T, U> implements Consumer<T> {
        Function<? super T, ? extends U> mapper;
        BinaryOperator<U> op;
        U value = null;
        boolean present = false;

        MappingOptHolder(final Function<? super T, ? extends U> mapper, final BinaryOperator<U> op) {
            this.mapper = mapper;
            this.op = op;
        }

        @Override
        public void accept(T t) {
            if (present) {
                value = op.apply(value, mapper.apply(t));
            } else {
                value = mapper.apply(t);
                present = true;
            }
        }
    }

    /**
     *
     *
     * @param <T>
     * @param <R>
     * @param mapper
     * @param op
     * @param supplierForEmpty
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T, R> Collector<T, ?, R> reducingOrElseGet(final Function<? super T, ? extends R> mapper, final BinaryOperator<R> op,
            final Supplier<? extends R> supplierForEmpty) {
        final Supplier<MappingOptHolder<T, R>> supplier = () -> new MappingOptHolder<>(mapper, op);

        final BiConsumer<MappingOptHolder<T, R>, T> accumulator = (BiConsumer) Reducing_Accumulator_2;
        final BinaryOperator<MappingOptHolder<T, R>> combiner = (BinaryOperator) Reducing_Combiner_2;
        final Function<MappingOptHolder<T, R>, R> finisher = a -> a.present ? a.value : supplierForEmpty.get();

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    /**
     *
     *
     * @param <T>
     * @param <R>
     * @param mapper
     * @param op
     * @param exceptionSupplier
     * @return
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

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    /**
     *
     *
     * @param <T>
     * @param <R>
     * @param mapper
     * @param op
     * @return
     */
    public static <T, R> Collector<T, ?, R> reducingOrElseThrow(final Function<? super T, ? extends R> mapper, final BinaryOperator<R> op) {
        return reducingOrElseThrow(mapper, op, noSuchElementExceptionSupplier);
    }

    /**
     * It's copied from StreamEx: https://github.com/amaembo/streamex under Apache License v2 and may be modified.
     * <br />
     *
     * Returns a {@code Collector} which computes a common prefix of input
     * {@code CharSequence} objects returning the result as {@code String}. For
     * empty input the empty {@code String} is returned.
     *
     * <p>
     * The returned {@code Collector} handles specially Unicode surrogate pairs:
     * the returned prefix may end with
     * <a href="http://www.unicode.org/glossary/#high_surrogate_code_unit">
     * Unicode high-surrogate code unit</a> only if it's not succeeded by
     * <a href="http://www.unicode.org/glossary/#low_surrogate_code_unit">
     * Unicode low-surrogate code unit</a> in any of the input sequences.
     * Normally the ending high-surrogate code unit is removed from the prefix.
     *
     * <p>
     * This method returns a
     * <a href="package-summary.html#ShortCircuitReduction">short-circuiting
     * collector</a>: it may not process all the elements if the common prefix
     * is empty.
     *
     * @return a {@code Collector} which computes a common prefix.
     * @since 0.5.0
     */
    public static Collector<CharSequence, ?, String> commonPrefix() {
        final Supplier<Pair<CharSequence, Integer>> supplier = () -> Pair.of(null, -1);

        final BiConsumer<Pair<CharSequence, Integer>, CharSequence> accumulator = (a, t) -> {
            if (a.right == -1) {
                a.left = t;
                a.right = t.length();
            } else if (a.right > 0) {
                if (t.length() < a.right) {
                    a.right = t.length();
                }

                for (int i = 0, to = a.right; i < to; i++) {
                    if (a.left.charAt(i) != t.charAt(i)) {
                        if (i > 0 && Character.isHighSurrogate(t.charAt(i - 1))
                                && (Character.isLowSurrogate(t.charAt(i)) || Character.isLowSurrogate(a.left.charAt(i)))) {
                            i--;
                        }

                        a.right = i;

                        break;
                    }
                }
            }
        };

        final BinaryOperator<Pair<CharSequence, Integer>> combiner = (a, b) -> {
            if (a.right == -1) {
                return b;
            }

            if (b.right != -1) {
                accumulator.accept(a, b.left.subSequence(0, b.right));
            }

            return a;
        };

        final Function<Pair<CharSequence, Integer>, String> finisher = a -> a.left == null ? "" : a.left.subSequence(0, a.right).toString();

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    /**
     * It's copied from StreamEx: https://github.com/amaembo/streamex under Apache License v2 and may be modified.
     * <br />
     *
     * Returns a {@code Collector} which computes a common suffix of input
     * {@code CharSequence} objects returning the result as {@code String}. For
     * empty input the empty {@code String} is returned.
     *
     * <p>
     * The returned {@code Collector} handles specially Unicode surrogate pairs:
     * the returned suffix may start with
     * <a href="http://www.unicode.org/glossary/#low_surrogate_code_unit">
     * Unicode low-surrogate code unit</a> only if it's not preceded by
     * <a href="http://www.unicode.org/glossary/#high_surrogate_code_unit">
     * Unicode high-surrogate code unit</a> in any of the input sequences.
     * Normally the starting low-surrogate code unit is removed from the suffix.
     *
     * <p>
     * This method returns a
     * <a href="package-summary.html#ShortCircuitReduction">short-circuiting
     * collector</a>: it may not process all the elements if the common suffix
     * is empty.
     *
     * @return a {@code Collector} which computes a common suffix.
     * @since 0.5.0
     */
    public static Collector<CharSequence, ?, String> commonSuffix() {
        final Supplier<Pair<CharSequence, Integer>> supplier = () -> Pair.of(null, -1);

        final BiConsumer<Pair<CharSequence, Integer>, CharSequence> accumulator = (a, t) -> {
            if (a.right == -1) {
                a.left = t;
                a.right = t.length();
            } else if (a.right > 0) {
                int alen = a.left.length();
                int blen = t.length();

                if (blen < a.right) {
                    a.right = blen;
                }

                for (int i = 0, to = a.right; i < to; i++) {
                    if (a.left.charAt(alen - 1 - i) != t.charAt(blen - 1 - i)) {
                        if (i > 0 && Character.isLowSurrogate(t.charAt(blen - i))
                                && (Character.isHighSurrogate(t.charAt(blen - 1 - i)) || Character.isHighSurrogate(a.left.charAt(alen - 1 - i)))) {
                            i--;
                        }

                        a.right = i;

                        break;
                    }
                }
            }
        };

        final BinaryOperator<Pair<CharSequence, Integer>> combiner = (a, b) -> {
            if (a.right == -1) {
                return b;
            }

            if (b.right != -1) {
                accumulator.accept(a, b.left.subSequence(b.left.length() - b.right, b.left.length()));
            }

            return a;
        };

        final Function<Pair<CharSequence, Integer>, String> finisher = a -> a.left == null ? ""
                : a.left.subSequence(a.left.length() - a.right, a.left.length()).toString();

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param keyMapper
     * @return
     */
    public static <T, K> Collector<T, ?, Map<K, List<T>>> groupingBy(Function<? super T, ? extends K> keyMapper) {
        final Collector<? super T, ?, List<T>> downstream = toList();

        return groupingBy(keyMapper, downstream);
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <M>
     * @param keyMapper
     * @param mapFactory
     * @return
     */
    public static <T, K, M extends Map<K, List<T>>> Collector<T, ?, M> groupingBy(final Function<? super T, ? extends K> keyMapper,
            final Supplier<? extends M> mapFactory) {
        final Collector<? super T, ?, List<T>> downstream = toList();

        return groupingBy(keyMapper, downstream, mapFactory);
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <A>
     * @param <D>
     * @param keyMapper
     * @param downstream
     * @return
     */
    public static <T, K, A, D> Collector<T, ?, Map<K, D>> groupingBy(final Function<? super T, ? extends K> keyMapper,
            final Collector<? super T, A, D> downstream) {
        final Supplier<Map<K, D>> mapFactory = Suppliers.ofMap();

        return groupingBy(keyMapper, downstream, mapFactory);
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <A>
     * @param <D>
     * @param <M>
     * @param keyMapper
     * @param downstream
     * @param mapFactory
     * @return
     */
    public static <T, K, A, D, M extends Map<K, D>> Collector<T, ?, M> groupingBy(final Function<? super T, ? extends K> keyMapper,
            final Collector<? super T, A, D> downstream, final Supplier<? extends M> mapFactory) {
        final Supplier<A> downstreamSupplier = downstream.supplier();
        final BiConsumer<A, ? super T> downstreamAccumulator = downstream.accumulator();

        final Function<K, A> mappingFunction = k -> downstreamSupplier.get();

        final BiConsumer<Map<K, A>, T> accumulator = (m, t) -> {
            K key = N.checkArgNotNull(keyMapper.apply(t), "element cannot be mapped to a null key");
            A container = computeIfAbsent(m, key, mappingFunction);
            downstreamAccumulator.accept(container, t);
        };

        final BinaryOperator<Map<K, A>> combiner = Collectors.<K, A, Map<K, A>> mapMerger(downstream.combiner());
        @SuppressWarnings("unchecked")
        final Supplier<Map<K, A>> mangledFactory = (Supplier<Map<K, A>>) mapFactory;

        @SuppressWarnings("unchecked")
        final Function<A, A> downstreamFinisher = (Function<A, A>) downstream.finisher();

        final BiFunction<? super K, ? super A, ? extends A> function = (k, v) -> downstreamFinisher.apply(v);

        final Function<Map<K, A>, M> finisher = intermediate -> {
            replaceAll(intermediate, function);
            return (M) intermediate;
        };

        return new CollectorImpl<>(mangledFactory, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param keyMapper
     * @return
     */
    public static <T, K> Collector<T, ?, ConcurrentMap<K, List<T>>> groupingByConcurrent(Function<? super T, ? extends K> keyMapper) {
        final Collector<? super T, ?, List<T>> downstream = toList();

        return groupingByConcurrent(keyMapper, downstream);
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <M>
     * @param keyMapper
     * @param mapFactory
     * @return
     */
    public static <T, K, M extends ConcurrentMap<K, List<T>>> Collector<T, ?, M> groupingByConcurrent(final Function<? super T, ? extends K> keyMapper,
            final Supplier<? extends M> mapFactory) {
        final Collector<? super T, ?, List<T>> downstream = toList();

        return groupingByConcurrent(keyMapper, downstream, mapFactory);
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <A>
     * @param <D>
     * @param keyMapper
     * @param downstream
     * @return
     */
    public static <T, K, A, D> Collector<T, ?, ConcurrentMap<K, D>> groupingByConcurrent(Function<? super T, ? extends K> keyMapper,
            Collector<? super T, A, D> downstream) {
        final Supplier<ConcurrentMap<K, D>> mapFactory = Suppliers.ofConcurrentMap();

        return groupingByConcurrent(keyMapper, downstream, mapFactory);
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <A>
     * @param <D>
     * @param <M>
     * @param keyMapper
     * @param downstream
     * @param mapFactory
     * @return
     */
    public static <T, K, A, D, M extends ConcurrentMap<K, D>> Collector<T, ?, M> groupingByConcurrent(final Function<? super T, ? extends K> keyMapper,
            Collector<? super T, A, D> downstream, final Supplier<? extends M> mapFactory) {
        final Supplier<A> downstreamSupplier = downstream.supplier();
        final BiConsumer<A, ? super T> downstreamAccumulator = downstream.accumulator();

        final Function<K, A> mappingFunction = k -> downstreamSupplier.get();

        final BiConsumer<ConcurrentMap<K, A>, T> accumulator = (m, t) -> {
            K key = N.checkArgNotNull(keyMapper.apply(t), "element cannot be mapped to a null key");
            A container = computeIfAbsent(m, key, mappingFunction);
            downstreamAccumulator.accept(container, t);
        };

        final BinaryOperator<ConcurrentMap<K, A>> combiner = Collectors.<K, A, ConcurrentMap<K, A>> mapMerger(downstream.combiner());
        @SuppressWarnings("unchecked")
        final Supplier<ConcurrentMap<K, A>> mangledFactory = (Supplier<ConcurrentMap<K, A>>) mapFactory;

        if (downstream.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
            return new CollectorImpl<>(mangledFactory, accumulator, combiner, CH_UNORDERED_ID);
        } else {
            @SuppressWarnings("unchecked")
            final Function<A, A> downstreamFinisher = (Function<A, A>) downstream.finisher();

            final BiFunction<? super K, ? super A, ? extends A> function = (k, v) -> downstreamFinisher.apply(v);

            final Function<ConcurrentMap<K, A>, M> finisher = intermediate -> {
                replaceAll(intermediate, function);
                return (M) intermediate;
            };

            return new CollectorImpl<>(mangledFactory, accumulator, combiner, finisher, CH_UNORDERED_NOID);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param predicate
     * @return
     */
    public static <T> Collector<T, ?, Map<Boolean, List<T>>> partitioningBy(Predicate<? super T> predicate) {
        final Collector<? super T, ?, List<T>> downstream = toList();

        return partitioningBy(predicate, downstream);
    }

    /**
     *
     *
     * @param <T>
     * @param <D>
     * @param <A>
     * @param predicate
     * @param downstream
     * @return
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

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param keyMapper
     * @return
     */
    public static <T, K> Collector<T, ?, Map<K, Long>> countingBy(Function<? super T, ? extends K> keyMapper) {
        return countingBy(keyMapper, Suppliers.<K, Long> ofMap());
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <M>
     * @param keyMapper
     * @param mapFactory
     * @return
     */
    public static <T, K, M extends Map<K, Long>> Collector<T, ?, M> countingBy(final Function<? super T, ? extends K> keyMapper,
            final Supplier<? extends M> mapFactory) {
        final Collector<? super T, ?, Long> downstream = counting();

        return groupingBy(keyMapper, downstream, mapFactory);
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param keyMapper
     * @return
     */
    public static <T, K> Collector<T, ?, Map<K, Integer>> countingToIntBy(Function<? super T, ? extends K> keyMapper) {
        return countingToIntBy(keyMapper, Suppliers.<K, Integer> ofMap());
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <M>
     * @param keyMapper
     * @param mapFactory
     * @return
     */
    public static <T, K, M extends Map<K, Integer>> Collector<T, ?, M> countingToIntBy(final Function<? super T, ? extends K> keyMapper,
            final Supplier<? extends M> mapFactory) {
        final Collector<? super T, ?, Integer> downstream = countingToInt();

        return groupingBy(keyMapper, downstream, mapFactory);
    }

    /**
     *
     *
     * @param <K>
     * @param <V>
     * @return
     */
    public static <K, V> Collector<Map.Entry<K, V>, ?, Map<K, V>> toMap() {
        final Function<Map.Entry<K, V>, ? extends K> keyMapper = Fn.<K, V> key();
        final Function<Map.Entry<K, V>, ? extends V> valueMapper = Fn.<K, V> value();

        return toMap(keyMapper, valueMapper);
    }

    /**
     *
     *
     * @param <K>
     * @param <V>
     * @param mergeFunction
     * @return
     */
    public static <K, V> Collector<Map.Entry<K, V>, ?, Map<K, V>> toMap(final BinaryOperator<V> mergeFunction) {
        final Function<Map.Entry<K, V>, ? extends K> keyMapper = Fn.<K, V> key();
        final Function<Map.Entry<K, V>, ? extends V> valueMapper = Fn.<K, V> value();

        return toMap(keyMapper, valueMapper, mergeFunction);
    }

    /**
     *
     *
     * @param <K>
     * @param <V>
     * @param <M>
     * @param mapFactory
     * @return
     */
    public static <K, V, M extends Map<K, V>> Collector<Map.Entry<K, V>, ?, M> toMap(final Supplier<? extends M> mapFactory) {
        final Function<Map.Entry<K, V>, ? extends K> keyMapper = Fn.<K, V> key();
        final Function<Map.Entry<K, V>, ? extends V> valueMapper = Fn.<K, V> value();

        return toMap(keyMapper, valueMapper, mapFactory);
    }

    /**
     *
     *
     * @param <K>
     * @param <V>
     * @param <M>
     * @param mergeFunction
     * @param mapFactory
     * @return
     */
    public static <K, V, M extends Map<K, V>> Collector<Map.Entry<K, V>, ?, M> toMap(final BinaryOperator<V> mergeFunction,
            final Supplier<? extends M> mapFactory) {
        final Function<Map.Entry<K, V>, ? extends K> keyMapper = Fn.<K, V> key();
        final Function<Map.Entry<K, V>, ? extends V> valueMapper = Fn.<K, V> value();

        return toMap(keyMapper, valueMapper, mergeFunction, mapFactory);
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <V>
     * @param keyMapper
     * @param valueMapper
     * @return
     */
    public static <T, K, V> Collector<T, ?, Map<K, V>> toMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        final BinaryOperator<V> mergeFunction = Fn.throwingMerger();

        return toMap(keyMapper, valueMapper, mergeFunction);
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <V>
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @return
     */
    public static <T, K, V> Collector<T, ?, Map<K, V>> toMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper,
            BinaryOperator<V> mergeFunction) {
        final Supplier<Map<K, V>> mapFactory = Suppliers.<K, V> ofMap();

        return toMap(keyMapper, valueMapper, mergeFunction, mapFactory);
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <V>
     * @param <M>
     * @param keyMapper
     * @param valueMapper
     * @param mapFactory
     * @return
     */
    public static <T, K, V, M extends Map<K, V>> Collector<T, ?, M> toMap(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper, final Supplier<? extends M> mapFactory) {
        final BinaryOperator<V> mergeFunction = Fn.throwingMerger();

        return toMap(keyMapper, valueMapper, mergeFunction, mapFactory);
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <V>
     * @param <M>
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @param mapFactory
     * @return
     */
    public static <T, K, V, M extends Map<K, V>> Collector<T, ?, M> toMap(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper, final BinaryOperator<V> mergeFunction, final Supplier<? extends M> mapFactory) {
        final BiConsumer<M, T> accumulator = (map, element) -> merge(map, keyMapper.apply(element), valueMapper.apply(element), mergeFunction);

        final BinaryOperator<M> combiner = (BinaryOperator<M>) mapMerger(mergeFunction);

        return new CollectorImpl<>(mapFactory, accumulator, combiner, CH_UNORDERED_ID);
    }

    /**
     *
     *
     * @param <K>
     * @param <V>
     * @return
     */
    public static <K, V> Collector<Map.Entry<K, V>, ?, ImmutableMap<K, V>> toImmutableMap() {
        final Collector<Map.Entry<K, V>, ?, Map<K, V>> downstream = toMap();
        @SuppressWarnings("rawtypes")
        final Function<Map<K, V>, ImmutableMap<K, V>> finisher = (Function) ImmutableMap_Finisher;

        return collectingAndThen(downstream, finisher);
    }

    /**
     *
     *
     * @param <K>
     * @param <V>
     * @param mergeFunction
     * @return
     */
    public static <K, V> Collector<Map.Entry<K, V>, ?, ImmutableMap<K, V>> toImmutableMap(final BinaryOperator<V> mergeFunction) {
        final Collector<Map.Entry<K, V>, ?, Map<K, V>> downstream = toMap(mergeFunction);
        @SuppressWarnings("rawtypes")
        final Function<Map<K, V>, ImmutableMap<K, V>> finisher = (Function) ImmutableMap_Finisher;

        return collectingAndThen(downstream, finisher);
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <V>
     * @param keyMapper
     * @param valueMapper
     * @return
     */
    public static <T, K, V> Collector<T, ?, ImmutableMap<K, V>> toImmutableMap(Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper) {
        final Collector<T, ?, Map<K, V>> downstream = toMap(keyMapper, valueMapper);
        @SuppressWarnings("rawtypes")
        final Function<Map<K, V>, ImmutableMap<K, V>> finisher = (Function) ImmutableMap_Finisher;

        return collectingAndThen(downstream, finisher);
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <V>
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @return
     */
    public static <T, K, V> Collector<T, ?, ImmutableMap<K, V>> toImmutableMap(Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper, BinaryOperator<V> mergeFunction) {
        final Collector<T, ?, Map<K, V>> downstream = toMap(keyMapper, valueMapper, mergeFunction);
        @SuppressWarnings("rawtypes")
        final Function<Map<K, V>, ImmutableMap<K, V>> finisher = (Function) ImmutableMap_Finisher;

        return collectingAndThen(downstream, finisher);
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <V>
     * @param keyMapper
     * @param valueMapper
     * @return
     * @see #toMap(Function, Function)
     */
    public static <T, K, V> Collector<T, ?, Map<K, V>> toLinkedHashMap(Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper) {
        final BinaryOperator<V> mergeFunction = Fn.throwingMerger();

        return toLinkedHashMap(keyMapper, valueMapper, mergeFunction);
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <V>
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @return
     * @see #toMap(Function, Function, BinaryOperator)
     */
    public static <T, K, V> Collector<T, ?, Map<K, V>> toLinkedHashMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper,
            BinaryOperator<V> mergeFunction) {
        final Supplier<Map<K, V>> mapFactory = Suppliers.ofLinkedHashMap();

        return toMap(keyMapper, valueMapper, mergeFunction, mapFactory);
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <V>
     * @param keyMapper
     * @param valueMapper
     * @return
     */
    public static <T, K, V> Collector<T, ?, ConcurrentMap<K, V>> toConcurrentMap(Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper) {
        final BinaryOperator<V> mergeFunction = Fn.throwingMerger();

        return toConcurrentMap(keyMapper, valueMapper, mergeFunction);
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <V>
     * @param <M>
     * @param keyMapper
     * @param valueMapper
     * @param mapFactory
     * @return
     */
    public static <T, K, V, M extends ConcurrentMap<K, V>> Collector<T, ?, M> toConcurrentMap(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper, Supplier<? extends M> mapFactory) {
        final BinaryOperator<V> mergeFunction = Fn.throwingMerger();

        return toConcurrentMap(keyMapper, valueMapper, mergeFunction, mapFactory);
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <V>
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @return
     */
    public static <T, K, V> Collector<T, ?, ConcurrentMap<K, V>> toConcurrentMap(Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper, BinaryOperator<V> mergeFunction) {
        final Supplier<ConcurrentMap<K, V>> mapFactory = Suppliers.ofConcurrentMap();

        return toConcurrentMap(keyMapper, valueMapper, mergeFunction, mapFactory);
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <V>
     * @param <M>
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @param mapFactory
     * @return
     */
    public static <T, K, V, M extends ConcurrentMap<K, V>> Collector<T, ?, M> toConcurrentMap(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper, final BinaryOperator<V> mergeFunction, Supplier<? extends M> mapFactory) {

        final BiConsumer<M, T> accumulator = (map, element) -> merge(map, keyMapper.apply(element), valueMapper.apply(element), mergeFunction);

        final BinaryOperator<M> combiner = (BinaryOperator<M>) concurrentMapMerger(mergeFunction);

        return new CollectorImpl<>(mapFactory, accumulator, combiner, CH_UNORDERED_ID);
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <V>
     * @param keyMapper
     * @param valueMapper
     * @return
     */
    public static <T, K, V> Collector<T, ?, BiMap<K, V>> toBiMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        final BinaryOperator<V> mergeFunction = Fn.throwingMerger();

        return toBiMap(keyMapper, valueMapper, mergeFunction);
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <V>
     * @param keyMapper
     * @param valueMapper
     * @param mapFactory
     * @return
     */
    public static <T, K, V> Collector<T, ?, BiMap<K, V>> toBiMap(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper, final Supplier<BiMap<K, V>> mapFactory) {
        final BinaryOperator<V> mergeFunction = Fn.throwingMerger();

        return toBiMap(keyMapper, valueMapper, mergeFunction, mapFactory);
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <V>
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @return
     */
    public static <T, K, V> Collector<T, ?, BiMap<K, V>> toBiMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper,
            BinaryOperator<V> mergeFunction) {
        final Supplier<BiMap<K, V>> mapFactory = Suppliers.ofBiMap();

        return toBiMap(keyMapper, valueMapper, mergeFunction, mapFactory);
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <V>
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @param mapFactory
     * @return
     */
    public static <T, K, V> Collector<T, ?, BiMap<K, V>> toBiMap(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper, final BinaryOperator<V> mergeFunction, final Supplier<BiMap<K, V>> mapFactory) {
        return toMap(keyMapper, valueMapper, mergeFunction, mapFactory);
    }

    /**
     *
     *
     * @param <K>
     * @param <V>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Collector<Map.Entry<K, V>, ?, ListMultimap<K, V>> toMultimap() {
        final Function<Map.Entry<? extends K, ? extends V>, ? extends K> keyMapper = (Function) Fn.key();
        final Function<Map.Entry<? extends K, ? extends V>, ? extends V> valueMapper = (Function) Fn.value();

        return toMultimap(keyMapper, valueMapper);
    }

    /**
     *
     *
     * @param <K>
     * @param <V>
     * @param <C>
     * @param <M>
     * @param mapFactory
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <K, V, C extends Collection<V>, M extends Multimap<K, V, C>> Collector<Map.Entry<K, V>, ?, M> toMultimap(
            final Supplier<? extends M> mapFactory) {
        final Function<Map.Entry<? extends K, ? extends V>, ? extends K> keyMapper = (Function) Fn.key();
        final Function<Map.Entry<? extends K, ? extends V>, ? extends V> valueMapper = (Function) Fn.value();

        return toMultimap(keyMapper, valueMapper, mapFactory);
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param keyMapper
     * @return
     */
    public static <T, K> Collector<T, ?, ListMultimap<K, T>> toMultimap(Function<? super T, ? extends K> keyMapper) {
        final Function<? super T, ? extends T> valueMapper = Fn.identity();

        return toMultimap(keyMapper, valueMapper);
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <C>
     * @param <M>
     * @param keyMapper
     * @param mapFactory
     * @return
     */
    public static <T, K, C extends Collection<T>, M extends Multimap<K, T, C>> Collector<T, ?, M> toMultimap(final Function<? super T, ? extends K> keyMapper,
            final Supplier<? extends M> mapFactory) {
        final Function<? super T, ? extends T> valueMapper = Fn.identity();

        return toMultimap(keyMapper, valueMapper, mapFactory);
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <V>
     * @param keyMapper
     * @param valueMapper
     * @return
     */
    public static <T, K, V> Collector<T, ?, ListMultimap<K, V>> toMultimap(Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper) {
        final Supplier<ListMultimap<K, V>> mapFactory = Suppliers.ofListMultimap();

        return toMultimap(keyMapper, valueMapper, mapFactory);
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <V>
     * @param <C>
     * @param <M>
     * @param keyMapper
     * @param valueMapper
     * @param mapFactory
     * @return
     */
    public static <T, K, V, C extends Collection<V>, M extends Multimap<K, V, C>> Collector<T, ?, M> toMultimap(
            final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper, final Supplier<? extends M> mapFactory) {
        final BiConsumer<M, T> accumulator = (map, element) -> map.put(keyMapper.apply(element), valueMapper.apply(element));

        final BinaryOperator<M> combiner = Collectors.<K, V, C, M> multimapMerger();

        return new CollectorImpl<>(mapFactory, accumulator, combiner, CH_UNORDERED_ID);
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <V>
     * @param keyMapper
     * @param flatValueMapper
     * @return
     * @see Collectors#toMultimap(Function, Function)
     */
    public static <T, K, V> Collector<T, ?, ListMultimap<K, V>> flatMapingValueToMultimap(final Function<? super T, K> keyMapper,
            final Function<? super T, ? extends java.util.stream.Stream<? extends V>> flatValueMapper) {
        return flatMapingValueToMultimap(keyMapper, flatValueMapper, Suppliers.<K, V> ofListMultimap());
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <V>
     * @param <C>
     * @param <M>
     * @param keyMapper
     * @param flatValueMapper
     * @param mapFactory
     * @return
     * @see Collectors#toMultimap(Function, Function, Supplier)
     */
    public static <T, K, V, C extends Collection<V>, M extends Multimap<K, V, C>> Collector<T, ?, M> flatMapingValueToMultimap(
            final Function<? super T, K> keyMapper, final Function<? super T, ? extends java.util.stream.Stream<? extends V>> flatValueMapper,
            final Supplier<? extends M> mapFactory) {

        final BiConsumer<M, T> accumulator = (map, element) -> {
            final K key = keyMapper.apply(element);

            try (java.util.stream.Stream<? extends V> stream = flatValueMapper.apply(element)) {
                if (stream.isParallel()) {
                    stream.sequential().forEach(value -> map.put(key, value));
                } else {
                    stream.forEach(value -> map.put(key, value));
                }
            }
        };

        final BinaryOperator<M> combiner = Collectors.<K, V, C, M> multimapMerger();

        return new CollectorImpl<>(mapFactory, accumulator, combiner, CH_UNORDERED_ID);
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <V>
     * @param keyMapper
     * @param flatValueMapper
     * @return
     * @see Collectors#toMultimap(Function, Function)
     */
    public static <T, K, V> Collector<T, ?, ListMultimap<K, V>> flattMapingValueToMultimap(final Function<? super T, K> keyMapper,
            final Function<? super T, ? extends Stream<? extends V>> flatValueMapper) {
        return flattMapingValueToMultimap(keyMapper, flatValueMapper, Suppliers.<K, V> ofListMultimap());
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <V>
     * @param <C>
     * @param <M>
     * @param keyMapper
     * @param flatValueMapper
     * @param mapFactory
     * @return
     * @see Collectors#toMultimap(Function, Function, Supplier)
     */
    public static <T, K, V, C extends Collection<V>, M extends Multimap<K, V, C>> Collector<T, ?, M> flattMapingValueToMultimap(
            final Function<? super T, K> keyMapper, final Function<? super T, ? extends Stream<? extends V>> flatValueMapper,
            final Supplier<? extends M> mapFactory) {

        final BiConsumer<M, T> accumulator = (map, element) -> {
            final K key = keyMapper.apply(element);

            try (Stream<? extends V> stream = flatValueMapper.apply(element)) {
                if (stream.isParallel()) {
                    stream.sequential().forEach(value -> map.put(key, value));
                } else {
                    stream.forEach(value -> map.put(key, value));
                }
            }
        };

        final BinaryOperator<M> combiner = Collectors.<K, V, C, M> multimapMerger();

        return new CollectorImpl<>(mapFactory, accumulator, combiner, CH_UNORDERED_ID);
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <V>
     * @param keyMapper
     * @param flatValueMapper
     * @return
     * @see Collectors#toMultimap(Function, Function)
     */
    public static <T, K, V> Collector<T, ?, ListMultimap<K, V>> flatmappingValueToMultimap(final Function<? super T, K> keyMapper,
            final Function<? super T, ? extends Collection<? extends V>> flatValueMapper) {
        return flatmappingValueToMultimap(keyMapper, flatValueMapper, Suppliers.<K, V> ofListMultimap());
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <V>
     * @param <C>
     * @param <M>
     * @param keyMapper
     * @param flatValueMapper
     * @param mapFactory
     * @return
     * @see Collectors#toMultimap(Function, Function, Supplier)
     */
    public static <T, K, V, C extends Collection<V>, M extends Multimap<K, V, C>> Collector<T, ?, M> flatmappingValueToMultimap(
            final Function<? super T, K> keyMapper, final Function<? super T, ? extends Collection<? extends V>> flatValueMapper,
            final Supplier<? extends M> mapFactory) {

        final BiConsumer<M, T> accumulator = (map, element) -> {
            final K key = keyMapper.apply(element);
            final Collection<? extends V> values = flatValueMapper.apply(element);

            if (N.notNullOrEmpty(values)) {
                for (V value : values) {
                    map.put(key, value);
                }
            }
        };

        final BinaryOperator<M> combiner = Collectors.<K, V, C, M> multimapMerger();

        return new CollectorImpl<>(mapFactory, accumulator, combiner, CH_UNORDERED_ID);
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <V>
     * @param flatKeyMapper
     * @param valueMapper
     * @return
     * @see Collectors#toMultimap(Function, Function)
     */
    public static <T, K, V> Collector<T, ?, ListMultimap<K, V>> flatMapingKeyToMultimap(
            final Function<? super T, java.util.stream.Stream<? extends K>> flatKeyMapper, final Function<? super T, V> valueMapper) {
        return flatMapingKeyToMultimap(flatKeyMapper, valueMapper, Suppliers.<K, V> ofListMultimap());
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <V>
     * @param <C>
     * @param <M>
     * @param flatKeyMapper
     * @param valueMapper
     * @param mapFactory
     * @return
     * @see Collectors#toMultimap(Function, Function, Supplier)
     */
    public static <T, K, V, C extends Collection<V>, M extends Multimap<K, V, C>> Collector<T, ?, M> flatMapingKeyToMultimap(
            final Function<? super T, java.util.stream.Stream<? extends K>> flatKeyMapper, final Function<? super T, V> valueMapper,
            final Supplier<? extends M> mapFactory) {

        final BiConsumer<M, T> accumulator = (map, element) -> {
            final V value = valueMapper.apply(element);

            try (java.util.stream.Stream<? extends K> stream = flatKeyMapper.apply(element)) {
                if (stream.isParallel()) {
                    stream.sequential().forEach(key -> map.put(key, value));
                } else {
                    stream.forEach(key -> map.put(key, value));
                }
            }
        };

        final BinaryOperator<M> combiner = Collectors.<K, V, C, M> multimapMerger();

        return new CollectorImpl<>(mapFactory, accumulator, combiner, CH_UNORDERED_ID);
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <V>
     * @param flatKeyMapper
     * @param valueMapper
     * @return
     * @see Collectors#toMultimap(Function, Function)
     */
    public static <T, K, V> Collector<T, ?, ListMultimap<K, V>> flattMapingKeyToMultimap(final Function<? super T, Stream<? extends K>> flatKeyMapper,
            final Function<? super T, V> valueMapper) {
        return flattMapingKeyToMultimap(flatKeyMapper, valueMapper, Suppliers.<K, V> ofListMultimap());
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <V>
     * @param <C>
     * @param <M>
     * @param flatKeyMapper
     * @param valueMapper
     * @param mapFactory
     * @return
     * @see Collectors#toMultimap(Function, Function, Supplier)
     */
    public static <T, K, V, C extends Collection<V>, M extends Multimap<K, V, C>> Collector<T, ?, M> flattMapingKeyToMultimap(
            final Function<? super T, Stream<? extends K>> flatKeyMapper, final Function<? super T, V> valueMapper, final Supplier<? extends M> mapFactory) {

        final BiConsumer<M, T> accumulator = (map, element) -> {
            final V value = valueMapper.apply(element);

            try (Stream<? extends K> stream = flatKeyMapper.apply(element)) {
                if (stream.isParallel()) {
                    stream.sequential().forEach(key -> map.put(key, value));
                } else {
                    stream.forEach(key -> map.put(key, value));
                }
            }
        };

        final BinaryOperator<M> combiner = Collectors.<K, V, C, M> multimapMerger();

        return new CollectorImpl<>(mapFactory, accumulator, combiner, CH_UNORDERED_ID);
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <V>
     * @param flatKeyMapper
     * @param valueMapper
     * @return
     * @see Collectors#toMultimap(Function, Function)
     */
    public static <T, K, V> Collector<T, ?, ListMultimap<K, V>> flatmappingKeyToMultimap(
            final Function<? super T, ? extends Collection<? extends K>> flatKeyMapper, final Function<? super T, V> valueMapper) {
        return flatmappingKeyToMultimap(flatKeyMapper, valueMapper, Suppliers.<K, V> ofListMultimap());
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <V>
     * @param <C>
     * @param <M>
     * @param flatKeyMapper
     * @param valueMapper
     * @param mapFactory
     * @return
     * @see Collectors#toMultimap(Function, Function, Supplier)
     */
    public static <T, K, V, C extends Collection<V>, M extends Multimap<K, V, C>> Collector<T, ?, M> flatmappingKeyToMultimap(
            final Function<? super T, ? extends Collection<? extends K>> flatKeyMapper, final Function<? super T, V> valueMapper,
            final Supplier<? extends M> mapFactory) {

        final BiConsumer<M, T> accumulator = (map, element) -> {
            final V value = valueMapper.apply(element);
            final Collection<? extends K> keys = flatKeyMapper.apply(element);

            if (N.notNullOrEmpty(keys)) {
                for (K key : keys) {
                    map.put(key, value);
                }
            }
        };

        final BinaryOperator<M> combiner = Collectors.<K, V, C, M> multimapMerger();

        return new CollectorImpl<>(mapFactory, accumulator, combiner, CH_UNORDERED_ID);
    }

    //    public static <T> Collector<T, ?, DataSet> toDataSet(final String beanName, final Class<?> beanClass, final List<String> columnNames) {
    //        @SuppressWarnings("rawtypes")
    //        final Collector<T, List<T>, List<T>> collector = (Collector) toList();
    //
    //        final Function<List<T>, DataSet> finisher = new Function<List<T>, DataSet>() {
    //            @Override
    //            public DataSet apply(List<T> t) {
    //                return N.newDataSet(beanName, beanClass, columnNames, t);
    //            }
    //        };
    //
    //        return new CollectorImpl<T, List<T>, DataSet>(collector.supplier(), collector.accumulator(), collector.combiner(), finisher);
    //    }

    static <K, V> void replaceAll(Map<K, V> map, BiFunction<? super K, ? super V, ? extends V> function) {
        N.checkArgNotNull(function);

        try {
            for (Map.Entry<K, V> entry : map.entrySet()) {
                entry.setValue(function.apply(entry.getKey(), entry.getValue()));
            }
        } catch (IllegalStateException ise) {
            throw new ConcurrentModificationException(ise);
        }
    }

    private static <K, V> V computeIfAbsent(Map<K, V> map, K key, Function<? super K, ? extends V> mappingFunction) {
        N.checkArgNotNull(mappingFunction);
        V v = null;

        if ((v = map.get(key)) == null) {
            V newValue = null;
            if ((newValue = mappingFunction.apply(key)) != null) {
                map.put(key, newValue);
                return newValue;
            }
        }

        return v;
    }

    private static <K, V, M extends Map<K, V>> BinaryOperator<M> mapMerger(final BinaryOperator<V> mergeFunction) {
        N.checkArgNotNull(mergeFunction);

        return (m1, m2) -> {
            for (Map.Entry<K, V> e : m2.entrySet()) {
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

    private static <K, V, M extends ConcurrentMap<K, V>> BinaryOperator<M> concurrentMapMerger(final BinaryOperator<V> mergeFunction) {
        N.checkArgNotNull(mergeFunction);

        return (m1, m2) -> {
            for (Map.Entry<K, V> e : m2.entrySet()) {
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
            for (Map.Entry<K, V> e : m2.entrySet()) {
                N.checkArgNotNull(e.getValue());
                key = e.getKey();
                value = e.getValue();

                if (N.notNullOrEmpty(value)) {
                    V oldValue = m1.get(key);

                    if (oldValue == null) {
                        m1.putAll(key, value);
                    } else {
                        oldValue.addAll(value);
                    }
                }
            }
            return m1;
        };
    }

    static <K, V> void merge(Map<K, V> map, K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        N.checkArgNotNull(remappingFunction);

        final V oldValue = map.get(key);

        if (oldValue == null && !map.containsKey(key)) {
            map.put(key, value);
        } else {
            map.put(key, remappingFunction.apply(oldValue, value));
        }
    }

    /**
     *
     * @param <T>
     * @param <R1>
     * @param <R2>
     * @param <R>
     * @param downstream1
     * @param downstream2
     * @param merger
     * @return
     */
    public static <T, R1, R2, R> Collector<T, ?, R> teeing(final Collector<? super T, ?, R1> downstream1, final Collector<? super T, ?, R2> downstream2,
            final BiFunction<? super R1, ? super R2, R> merger) {
        return MoreCollectors.combine(downstream1, downstream2, merger);
    }

    public abstract static class MoreCollectors extends Collectors {
        protected MoreCollectors() {
            // for extension.
        }

        /**
         *
         *
         * @param <T>
         * @param mapper1
         * @param mapper2
         * @return
         */
        public static <T> Collector<T, ?, Tuple2<Integer, Integer>> summingInt(final ToIntFunction<? super T> mapper1, final ToIntFunction<? super T> mapper2) {
            final BiConsumer<int[], T> accumulator = (a, t) -> {
                a[0] += mapper1.applyAsInt(t);
                a[1] += mapper2.applyAsInt(t);
            };

            return new CollectorImpl<>(SummingInt_Supplier_2, accumulator, SummingInt_Combiner_2, SummingInt_Finisher_2, CH_UNORDERED_NOID);
        }

        /**
         *
         *
         * @param <T>
         * @param mapper1
         * @param mapper2
         * @param mapper3
         * @return
         */
        public static <T> Collector<T, ?, Tuple3<Integer, Integer, Integer>> summingInt(final ToIntFunction<? super T> mapper1,
                final ToIntFunction<? super T> mapper2, final ToIntFunction<? super T> mapper3) {
            final BiConsumer<int[], T> accumulator = (a, t) -> {
                a[0] += mapper1.applyAsInt(t);
                a[1] += mapper2.applyAsInt(t);
                a[2] += mapper3.applyAsInt(t);
            };

            return new CollectorImpl<>(SummingInt_Supplier_3, accumulator, SummingInt_Combiner_3, SummingInt_Finisher_3, CH_UNORDERED_NOID);
        }

        /**
         *
         *
         * @param <T>
         * @param mapper1
         * @param mapper2
         * @return
         */
        public static <T> Collector<T, ?, Tuple2<Long, Long>> summingIntToLong(final ToIntFunction<? super T> mapper1, final ToIntFunction<? super T> mapper2) {
            final BiConsumer<long[], T> accumulator = (a, t) -> {
                a[0] += mapper1.applyAsInt(t);
                a[1] += mapper2.applyAsInt(t);
            };

            return new CollectorImpl<>(SummingIntToLong_Supplier_2, accumulator, SummingIntToLong_Combiner_2, SummingIntToLong_Finisher_2, CH_UNORDERED_NOID);
        }

        /**
         *
         *
         * @param <T>
         * @param mapper1
         * @param mapper2
         * @param mapper3
         * @return
         */
        public static <T> Collector<T, ?, Tuple3<Long, Long, Long>> summingIntToLong(final ToIntFunction<? super T> mapper1,
                final ToIntFunction<? super T> mapper2, final ToIntFunction<? super T> mapper3) {
            final BiConsumer<long[], T> accumulator = (a, t) -> {
                a[0] += mapper1.applyAsInt(t);
                a[1] += mapper2.applyAsInt(t);
                a[2] += mapper3.applyAsInt(t);
            };

            return new CollectorImpl<>(SummingIntToLong_Supplier_3, accumulator, SummingIntToLong_Combiner_3, SummingIntToLong_Finisher_3, CH_UNORDERED_NOID);
        }

        /**
         *
         *
         * @param <T>
         * @param mapper1
         * @param mapper2
         * @return
         */
        public static <T> Collector<T, ?, Tuple2<Long, Long>> summingLong(final ToLongFunction<? super T> mapper1, final ToLongFunction<? super T> mapper2) {
            final BiConsumer<long[], T> accumulator = (a, t) -> {
                a[0] += mapper1.applyAsLong(t);
                a[1] += mapper2.applyAsLong(t);
            };

            return new CollectorImpl<>(SummingLong_Supplier_2, accumulator, SummingLong_Combiner_2, SummingLong_Finisher_2, CH_UNORDERED_NOID);
        }

        /**
         *
         *
         * @param <T>
         * @param mapper1
         * @param mapper2
         * @param mapper3
         * @return
         */
        public static <T> Collector<T, ?, Tuple3<Long, Long, Long>> summingLong(final ToLongFunction<? super T> mapper1,
                final ToLongFunction<? super T> mapper2, final ToLongFunction<? super T> mapper3) {
            final BiConsumer<long[], T> accumulator = (a, t) -> {
                a[0] += mapper1.applyAsLong(t);
                a[1] += mapper2.applyAsLong(t);
                a[2] += mapper3.applyAsLong(t);
            };

            return new CollectorImpl<>(SummingLong_Supplier_3, accumulator, SummingLong_Combiner_3, SummingLong_Finisher_3, CH_UNORDERED_NOID);
        }

        /**
         *
         *
         * @param <T>
         * @param mapper1
         * @param mapper2
         * @return
         */
        public static <T> Collector<T, ?, Tuple2<Double, Double>> summingDouble(final ToDoubleFunction<? super T> mapper1,
                final ToDoubleFunction<? super T> mapper2) {
            final BiConsumer<KahanSummation[], T> accumulator = (a, t) -> {
                a[0].add(mapper1.applyAsDouble(t));
                a[1].add(mapper2.applyAsDouble(t));
            };

            return new CollectorImpl<>(SummingDouble_Supplier_2, accumulator, SummingDouble_Combiner_2, SummingDouble_Finisher_2, CH_UNORDERED_NOID);
        }

        /**
         *
         *
         * @param <T>
         * @param mapper1
         * @param mapper2
         * @param mapper3
         * @return
         */
        public static <T> Collector<T, ?, Tuple3<Double, Double, Double>> summingDouble(final ToDoubleFunction<? super T> mapper1,
                final ToDoubleFunction<? super T> mapper2, final ToDoubleFunction<? super T> mapper3) {
            final BiConsumer<KahanSummation[], T> accumulator = (a, t) -> {
                a[0].add(mapper1.applyAsDouble(t));
                a[1].add(mapper2.applyAsDouble(t));
                a[2].add(mapper3.applyAsDouble(t));
            };

            return new CollectorImpl<>(SummingDouble_Supplier_3, accumulator, SummingDouble_Combiner_3, SummingDouble_Finisher_3, CH_UNORDERED_NOID);
        }

        /**
         *
         *
         * @param <T>
         * @param mapper1
         * @param mapper2
         * @return
         */
        public static <T> Collector<T, ?, Tuple2<BigInteger, BigInteger>> summingBigInteger(final Function<? super T, BigInteger> mapper1,
                final Function<? super T, BigInteger> mapper2) {
            final BiConsumer<BigInteger[], T> accumulator = (a, t) -> {
                a[0] = a[0].add(mapper1.apply(t));
                a[1] = a[1].add(mapper2.apply(t));
            };

            return new CollectorImpl<>(SummingBigInteger_Supplier_2, accumulator, SummingBigInteger_Combiner_2, SummingBigInteger_Finisher_2,
                    CH_UNORDERED_NOID);
        }

        /**
         *
         *
         * @param <T>
         * @param mapper1
         * @param mapper2
         * @param mapper3
         * @return
         */
        public static <T> Collector<T, ?, Tuple3<BigInteger, BigInteger, BigInteger>> summingBigInteger(final Function<? super T, BigInteger> mapper1,
                final Function<? super T, BigInteger> mapper2, final Function<? super T, BigInteger> mapper3) {
            final BiConsumer<BigInteger[], T> accumulator = (a, t) -> {
                a[0] = a[0].add(mapper1.apply(t));
                a[1] = a[1].add(mapper2.apply(t));
                a[2] = a[2].add(mapper3.apply(t));
            };

            return new CollectorImpl<>(SummingBigInteger_Supplier_3, accumulator, SummingBigInteger_Combiner_3, SummingBigInteger_Finisher_3,
                    CH_UNORDERED_NOID);
        }

        /**
         *
         *
         * @param <T>
         * @param mapper1
         * @param mapper2
         * @return
         */
        public static <T> Collector<T, ?, Tuple2<BigDecimal, BigDecimal>> summingBigDecimal(final Function<? super T, BigDecimal> mapper1,
                final Function<? super T, BigDecimal> mapper2) {
            final BiConsumer<BigDecimal[], T> accumulator = (a, t) -> {
                a[0] = a[0].add(mapper1.apply(t));
                a[1] = a[1].add(mapper2.apply(t));
            };

            return new CollectorImpl<>(SummingBigDecimal_Supplier_2, accumulator, SummingBigDecimal_Combiner_2, SummingBigDecimal_Finisher_2,
                    CH_UNORDERED_NOID);
        }

        /**
         *
         *
         * @param <T>
         * @param mapper1
         * @param mapper2
         * @param mapper3
         * @return
         */
        public static <T> Collector<T, ?, Tuple3<BigDecimal, BigDecimal, BigDecimal>> summingBigDecimal(final Function<? super T, BigDecimal> mapper1,
                final Function<? super T, BigDecimal> mapper2, final Function<? super T, BigDecimal> mapper3) {
            final BiConsumer<BigDecimal[], T> accumulator = (a, t) -> {
                a[0] = a[0].add(mapper1.apply(t));
                a[1] = a[1].add(mapper2.apply(t));
                a[2] = a[2].add(mapper3.apply(t));
            };

            return new CollectorImpl<>(SummingBigDecimal_Supplier_3, accumulator, SummingBigDecimal_Combiner_3, SummingBigDecimal_Finisher_3,
                    CH_UNORDERED_NOID);
        }

        /**
         *
         *
         * @param <T>
         * @param mapper1
         * @param mapper2
         * @return
         */
        public static <T> Collector<T, ?, Tuple2<Double, Double>> averagingInt(final ToIntFunction<? super T> mapper1, final ToIntFunction<? super T> mapper2) {
            final BiConsumer<Pair<long[], long[]>, T> accumulator = (a, t) -> {
                a.left[0] += mapper1.applyAsInt(t);
                a.left[1] += mapper2.applyAsInt(t);
                a.right[0] += 1;
                a.right[1] += 1;
            };

            return new CollectorImpl<>(AveragingInt_Supplier_2, accumulator, AveragingInt_Combiner_2, AveragingInt_Finisher_2, CH_UNORDERED_NOID);
        }

        /**
         *
         *
         * @param <T>
         * @param mapper1
         * @param mapper2
         * @param mapper3
         * @return
         */
        public static <T> Collector<T, ?, Tuple3<Double, Double, Double>> averagingInt(final ToIntFunction<? super T> mapper1,
                final ToIntFunction<? super T> mapper2, final ToIntFunction<? super T> mapper3) {
            final BiConsumer<Pair<long[], long[]>, T> accumulator = (a, t) -> {
                a.left[0] += mapper1.applyAsInt(t);
                a.left[1] += mapper2.applyAsInt(t);
                a.left[2] += mapper3.applyAsInt(t);
                a.right[0] += 1;
                a.right[1] += 1;
                a.right[2] += 1;
            };

            return new CollectorImpl<>(AveragingInt_Supplier_3, accumulator, AveragingInt_Combiner_3, AveragingInt_Finisher_3, CH_UNORDERED_NOID);
        }

        /**
         *
         *
         * @param <T>
         * @param mapper1
         * @param mapper2
         * @return
         */
        public static <T> Collector<T, ?, Tuple2<Double, Double>> averagingLong(final ToLongFunction<? super T> mapper1,
                final ToLongFunction<? super T> mapper2) {
            final BiConsumer<Pair<long[], long[]>, T> accumulator = (a, t) -> {
                a.left[0] += mapper1.applyAsLong(t);
                a.left[1] += mapper2.applyAsLong(t);
                a.right[0] += 1;
                a.right[1] += 1;
            };

            return new CollectorImpl<>(AveragingLong_Supplier_2, accumulator, AveragingLong_Combiner_2, AveragingLong_Finisher_2, CH_UNORDERED_NOID);
        }

        /**
         *
         *
         * @param <T>
         * @param mapper1
         * @param mapper2
         * @param mapper3
         * @return
         */
        public static <T> Collector<T, ?, Tuple3<Double, Double, Double>> averagingLong(final ToLongFunction<? super T> mapper1,
                final ToLongFunction<? super T> mapper2, final ToLongFunction<? super T> mapper3) {
            final BiConsumer<Pair<long[], long[]>, T> accumulator = (a, t) -> {
                a.left[0] += mapper1.applyAsLong(t);
                a.left[1] += mapper2.applyAsLong(t);
                a.left[2] += mapper3.applyAsLong(t);
                a.right[0] += 1;
                a.right[1] += 1;
                a.right[2] += 1;
            };

            return new CollectorImpl<>(AveragingLong_Supplier_3, accumulator, AveragingLong_Combiner_3, AveragingLong_Finisher_3, CH_UNORDERED_NOID);
        }

        /**
         *
         *
         * @param <T>
         * @param mapper1
         * @param mapper2
         * @return
         */
        public static <T> Collector<T, ?, Tuple2<Double, Double>> averagingDouble(final ToDoubleFunction<? super T> mapper1,
                final ToDoubleFunction<? super T> mapper2) {
            final BiConsumer<KahanSummation[], T> accumulator = (a, t) -> {
                a[0].add(mapper1.applyAsDouble(t));
                a[1].add(mapper2.applyAsDouble(t));
            };

            return new CollectorImpl<>(AveragingDouble_Supplier_2, accumulator, AveragingDouble_Combiner_2, AveragingDouble_Finisher_2, CH_UNORDERED_NOID);
        }

        /**
         *
         *
         * @param <T>
         * @param mapper1
         * @param mapper2
         * @param mapper3
         * @return
         */
        public static <T> Collector<T, ?, Tuple3<Double, Double, Double>> averagingDouble(final ToDoubleFunction<? super T> mapper1,
                final ToDoubleFunction<? super T> mapper2, final ToDoubleFunction<? super T> mapper3) {
            final BiConsumer<KahanSummation[], T> accumulator = (a, t) -> {
                a[0].add(mapper1.applyAsDouble(t));
                a[1].add(mapper2.applyAsDouble(t));
                a[2].add(mapper3.applyAsDouble(t));
            };

            return new CollectorImpl<>(AveragingDouble_Supplier_3, accumulator, AveragingDouble_Combiner_3, AveragingDouble_Finisher_3, CH_UNORDERED_NOID);
        }

        /**
         *
         *
         * @param <T>
         * @param mapper1
         * @param mapper2
         * @return
         */
        public static <T> Collector<T, ?, Tuple2<BigDecimal, BigDecimal>> averagingBigInteger(final Function<? super T, BigInteger> mapper1,
                final Function<? super T, BigInteger> mapper2) {
            final BiConsumer<Pair<BigInteger[], long[]>, T> accumulator = (a, t) -> {
                a.left[0] = a.left[0].add(mapper1.apply(t));
                a.left[1] = a.left[1].add(mapper2.apply(t));
                a.right[0] += 1;
                a.right[1] += 1;
            };

            return new CollectorImpl<>(AveragingBigInteger_Supplier_2, accumulator, AveragingBigInteger_Combiner_2, AveragingBigInteger_Finisher_2,
                    CH_UNORDERED_NOID);
        }

        /**
         *
         *
         * @param <T>
         * @param mapper1
         * @param mapper2
         * @param mapper3
         * @return
         */
        public static <T> Collector<T, ?, Tuple3<BigDecimal, BigDecimal, BigDecimal>> averagingBigInteger(final Function<? super T, BigInteger> mapper1,
                final Function<? super T, BigInteger> mapper2, final Function<? super T, BigInteger> mapper3) {
            final BiConsumer<Pair<BigInteger[], long[]>, T> accumulator = (a, t) -> {
                a.left[0] = a.left[0].add(mapper1.apply(t));
                a.left[1] = a.left[1].add(mapper2.apply(t));
                a.left[2] = a.left[2].add(mapper3.apply(t));
                a.right[0] += 1;
                a.right[1] += 1;
                a.right[2] += 1;
            };

            return new CollectorImpl<>(AveragingBigInteger_Supplier_3, accumulator, AveragingBigInteger_Combiner_3, AveragingBigInteger_Finisher_3,
                    CH_UNORDERED_NOID);
        }

        /**
         *
         *
         * @param <T>
         * @param mapper1
         * @param mapper2
         * @return
         */
        public static <T> Collector<T, ?, Tuple2<BigDecimal, BigDecimal>> averagingBigDecimal(final Function<? super T, BigDecimal> mapper1,
                final Function<? super T, BigDecimal> mapper2) {
            final BiConsumer<Pair<BigDecimal[], long[]>, T> accumulator = (a, t) -> {
                a.left[0] = a.left[0].add(mapper1.apply(t));
                a.left[1] = a.left[1].add(mapper2.apply(t));
                a.right[0] += 1;
                a.right[1] += 1;
            };

            return new CollectorImpl<>(AveragingBigDecimal_Supplier_2, accumulator, AveragingBigDecimal_Combiner_2, AveragingBigDecimal_Finisher_2,
                    CH_UNORDERED_NOID);
        }

        /**
         *
         *
         * @param <T>
         * @param mapper1
         * @param mapper2
         * @param mapper3
         * @return
         */
        public static <T> Collector<T, ?, Tuple3<BigDecimal, BigDecimal, BigDecimal>> averagingBigDecimal(final Function<? super T, BigDecimal> mapper1,
                final Function<? super T, BigDecimal> mapper2, final Function<? super T, BigDecimal> mapper3) {
            final BiConsumer<Pair<BigDecimal[], long[]>, T> accumulator = (a, t) -> {
                a.left[0] = a.left[0].add(mapper1.apply(t));
                a.left[1] = a.left[1].add(mapper2.apply(t));
                a.left[2] = a.left[2].add(mapper3.apply(t));
                a.right[0] += 1;
                a.right[1] += 1;
                a.right[2] += 1;
            };

            return new CollectorImpl<>(AveragingBigDecimal_Supplier_3, accumulator, AveragingBigDecimal_Combiner_3, AveragingBigDecimal_Finisher_3,
                    CH_UNORDERED_NOID);
        }

        /**
         *
         *
         * @param <T>
         * @param <R1>
         * @param <R2>
         * @param downstream1
         * @param downstream2
         * @return
         */
        public static <T, R1, R2> Collector<T, ?, Tuple2<R1, R2>> combine(final Collector<? super T, ?, R1> downstream1,
                final Collector<? super T, ?, R2> downstream2) {
            return combine(downstream1, downstream2, Tuple::of);
        }

        /**
         *
         * @param <T>
         * @param <R1>
         * @param <R2>
         * @param <R3>
         * @param downstream1
         * @param downstream2
         * @param downstream3
         * @return
         */
        public static <T, R1, R2, R3> Collector<T, ?, Tuple3<R1, R2, R3>> combine(final Collector<? super T, ?, R1> downstream1,
                final Collector<? super T, ?, R2> downstream2, final Collector<? super T, ?, R3> downstream3) {
            return combine(downstream1, downstream2, downstream3, Tuple::of);
        }

        /**
         *
         *
         * @param <T>
         * @param <R1>
         * @param <R2>
         * @param <R3>
         * @param <R4>
         * @param downstream1
         * @param downstream2
         * @param downstream3
         * @param downstream4
         * @return
         */
        public static <T, R1, R2, R3, R4> Collector<T, ?, Tuple4<R1, R2, R3, R4>> combine(final Collector<? super T, ?, R1> downstream1,
                final Collector<? super T, ?, R2> downstream2, final Collector<? super T, ?, R3> downstream3, final Collector<? super T, ?, R4> downstream4) {
            return combine(downstream1, downstream2, downstream3, downstream4, Tuple::of);
        }

        /**
         *
         *
         * @param <T>
         * @param <R1>
         * @param <R2>
         * @param <R3>
         * @param <R4>
         * @param <R5>
         * @param downstream1
         * @param downstream2
         * @param downstream3
         * @param downstream4
         * @param downstream5
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T, R1, R2, R3, R4, R5> Collector<T, ?, Tuple5<R1, R2, R3, R4, R5>> combine(final Collector<? super T, ?, R1> downstream1,
                final Collector<? super T, ?, R2> downstream2, final Collector<? super T, ?, R3> downstream3, final Collector<? super T, ?, R4> downstream4,
                final Collector<? super T, ?, R5> downstream5) {
            N.checkArgNotNull(downstream1, "downstream1"); //NOSONAR
            N.checkArgNotNull(downstream2, "downstream2"); //NOSONAR
            N.checkArgNotNull(downstream3, "downstream3"); //NOSONAR
            N.checkArgNotNull(downstream4, "downstream4"); //NOSONAR
            N.checkArgNotNull(downstream5, "downstream5"); //NOSONAR

            final List<Collector<? super T, ?, ?>> downstreams = (List) Array.asList(downstream1, downstream2, downstream3, downstream4, downstream5);

            final Function<Object[], Tuple5<R1, R2, R3, R4, R5>> finalMerger = a -> Tuple.of((R1) a[0], (R2) a[1], (R3) a[2], (R4) a[3], (R5) a[4]);

            return combine(downstreams, finalMerger);
        }

        /**
         *
         *
         * @param <T>
         * @param <R1>
         * @param <R2>
         * @param <R3>
         * @param <R4>
         * @param <R5>
         * @param <R6>
         * @param downstream1
         * @param downstream2
         * @param downstream3
         * @param downstream4
         * @param downstream5
         * @param downstream6
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T, R1, R2, R3, R4, R5, R6> Collector<T, ?, Tuple6<R1, R2, R3, R4, R5, R6>> combine(final Collector<? super T, ?, R1> downstream1,
                final Collector<? super T, ?, R2> downstream2, final Collector<? super T, ?, R3> downstream3, final Collector<? super T, ?, R4> downstream4,
                final Collector<? super T, ?, R5> downstream5, final Collector<? super T, ?, R6> downstream6) {
            N.checkArgNotNull(downstream1, "downstream1");
            N.checkArgNotNull(downstream2, "downstream2");
            N.checkArgNotNull(downstream3, "downstream3");
            N.checkArgNotNull(downstream4, "downstream4");
            N.checkArgNotNull(downstream5, "downstream5");
            N.checkArgNotNull(downstream6, "downstream6");

            final List<Collector<? super T, ?, ?>> downstreams = (List) Array.asList(downstream1, downstream2, downstream3, downstream4, downstream5,
                    downstream6);

            final Function<Object[], Tuple6<R1, R2, R3, R4, R5, R6>> finalMerger = a -> Tuple.of((R1) a[0], (R2) a[1], (R3) a[2], (R4) a[3], (R5) a[4],
                    (R6) a[5]);

            return combine(downstreams, finalMerger);
        }

        /**
         *
         *
         * @param <T>
         * @param <R1>
         * @param <R2>
         * @param <R3>
         * @param <R4>
         * @param <R5>
         * @param <R6>
         * @param <R7>
         * @param downstream1
         * @param downstream2
         * @param downstream3
         * @param downstream4
         * @param downstream5
         * @param downstream6
         * @param downstream7
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T, R1, R2, R3, R4, R5, R6, R7> Collector<T, ?, Tuple7<R1, R2, R3, R4, R5, R6, R7>> combine(final Collector<? super T, ?, R1> downstream1,
                final Collector<? super T, ?, R2> downstream2, final Collector<? super T, ?, R3> downstream3, final Collector<? super T, ?, R4> downstream4,
                final Collector<? super T, ?, R5> downstream5, final Collector<? super T, ?, R6> downstream6, final Collector<? super T, ?, R7> downstream7) {
            N.checkArgNotNull(downstream1, "downstream1");
            N.checkArgNotNull(downstream2, "downstream2");
            N.checkArgNotNull(downstream3, "downstream3");
            N.checkArgNotNull(downstream4, "downstream4");
            N.checkArgNotNull(downstream5, "downstream5");
            N.checkArgNotNull(downstream6, "downstream6");
            N.checkArgNotNull(downstream7, "downstream7");

            final List<Collector<? super T, ?, ?>> downstreams = (List) Array.asList(downstream1, downstream2, downstream3, downstream4, downstream5,
                    downstream6, downstream7);

            final Function<Object[], Tuple7<R1, R2, R3, R4, R5, R6, R7>> finalMerger = a -> Tuple.of((R1) a[0], (R2) a[1], (R3) a[2], (R4) a[3], (R5) a[4],
                    (R6) a[5], (R7) a[6]);

            return combine(downstreams, finalMerger);
        }

        /**
         *
         * @param <T>
         * @param <R1>
         * @param <R2>
         * @param <R>
         * @param downstream1
         * @param downstream2
         * @param merger
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T, R1, R2, R> Collector<T, ?, R> combine(final Collector<? super T, ?, R1> downstream1, final Collector<? super T, ?, R2> downstream2,
                final BiFunction<? super R1, ? super R2, R> merger) {
            N.checkArgNotNull(downstream1, "downstream1");
            N.checkArgNotNull(downstream2, "downstream2");
            N.checkArgNotNull(merger, "merger"); //NOSONAR

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
            final Set<Characteristics> characteristics = N.isNullOrEmpty(common) ? CH_NOID : N.newHashSet(common);

            return new CollectorImpl<>(supplier, accumulator, combiner, finisher, characteristics);
        }

        /**
         *
         * @param <T>
         * @param <R1>
         * @param <R2>
         * @param <R3>
         * @param <R>
         * @param downstream1
         * @param downstream2
         * @param downstream3
         * @param merger
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T, R1, R2, R3, R> Collector<T, ?, R> combine(final Collector<? super T, ?, R1> downstream1,
                final Collector<? super T, ?, R2> downstream2, final Collector<? super T, ?, R3> downstream3,
                final TriFunction<? super R1, ? super R2, ? super R3, R> merger) {
            N.checkArgNotNull(downstream1, "downstream1");
            N.checkArgNotNull(downstream2, "downstream2");
            N.checkArgNotNull(downstream3, "downstream3");
            N.checkArgNotNull(merger, "merger");

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
            final Set<Characteristics> characteristics = N.isNullOrEmpty(common) ? CH_NOID : N.newHashSet(common);

            return new CollectorImpl<>(supplier, accumulator, combiner, finisher, characteristics);
        }

        /**
         *
         *
         * @param <T>
         * @param <R1>
         * @param <R2>
         * @param <R3>
         * @param <R4>
         * @param <R>
         * @param downstream1
         * @param downstream2
         * @param downstream3
         * @param downstream4
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T, R1, R2, R3, R4, R> Collector<T, ?, R> combine(final Collector<? super T, ?, R1> downstream1,
                final Collector<? super T, ?, R2> downstream2, final Collector<? super T, ?, R3> downstream3, final Collector<? super T, ?, R4> downstream4,
                final QuadFunction<? super R1, ? super R2, ? super R3, ? super R4, R> merger) {
            N.checkArgNotNull(downstream1, "downstream1");
            N.checkArgNotNull(downstream2, "downstream2");
            N.checkArgNotNull(downstream3, "downstream3");
            N.checkArgNotNull(downstream4, "downstream4");
            N.checkArgNotNull(merger, "merger");

            final List<Collector<? super T, ?, ?>> downstreams = (List) Array.asList(downstream1, downstream2, downstream3, downstream4);

            final Function<Object[], R> finalMerger = a -> merger.apply((R1) a[0], (R2) a[1], (R3) a[2], (R4) a[3]);

            return combine(downstreams, finalMerger);
        }

        /**
         *
         * @param <T>
         * @param <R>
         * @param downstreams
         * @param merger
         * @return
         */
        public static <T, R> Collector<T, ?, R> combine(final Collection<? extends Collector<? super T, ?, ?>> downstreams,
                final Function<Object[], R> merger) { //NOSONAR
            N.checkArgument(N.notNullOrEmpty(downstreams), "The specified 'collectors' can't be null or empty");
            N.checkArgNotNull(merger, "merger");

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

            final Collection<Characteristics> common = N.intersection(downstreams.stream().map(Collector::characteristics).filter(N::notNullOrEmpty).toList());

            common.remove(Characteristics.IDENTITY_FINISH);

            final Set<Characteristics> characteristics = N.isNullOrEmpty(common) ? CH_NOID : N.newHashSet(common);

            return new CollectorImpl<>(supplier, accumulator, combiner, finisher, characteristics);
        }

        /**
         *
         *
         * @param <T>
         * @return
         */
        public static <T> Collector<T, ?, DataSet> toDataSet() {
            return toDataSet(null);
        }

        /**
         *
         *
         * @param <T>
         * @param columnNames
         * @return
         */
        public static <T> Collector<T, ?, DataSet> toDataSet(final List<String> columnNames) {
            @SuppressWarnings("rawtypes")
            final Collector<T, List<T>, List<T>> collector = (Collector) Collectors.toList();

            final Function<List<T>, DataSet> finisher = t -> N.newDataSet(columnNames, t);

            return new Collectors.CollectorImpl<>(collector.supplier(), collector.accumulator(), collector.combiner(), finisher, Collectors.CH_NOID);
        }
    }
}
