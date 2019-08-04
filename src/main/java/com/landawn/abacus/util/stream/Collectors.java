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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collector.Characteristics;

import com.landawn.abacus.DataSet;
import com.landawn.abacus.exception.DuplicatedResultException;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.BiMap;
import com.landawn.abacus.util.BooleanList;
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.ByteSummaryStatistics;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.CharSummaryStatistics;
import com.landawn.abacus.util.ContinuableFuture;
import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.DoubleSummaryStatistics;
import com.landawn.abacus.util.FloatList;
import com.landawn.abacus.util.FloatSummaryStatistics;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Fn.BiConsumers;
import com.landawn.abacus.util.Fn.BinaryOperators;
import com.landawn.abacus.util.Fn.Suppliers;
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
import com.landawn.abacus.util.MutableLong;
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
import com.landawn.abacus.util.u.Holder;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.BooleanSupplier;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToByteFunction;
import com.landawn.abacus.util.function.ToCharFunction;
import com.landawn.abacus.util.function.ToDoubleFunction;
import com.landawn.abacus.util.function.ToFloatFunction;
import com.landawn.abacus.util.function.ToIntFunction;
import com.landawn.abacus.util.function.ToLongFunction;
import com.landawn.abacus.util.function.ToShortFunction;
import com.landawn.abacus.util.function.TriFunction;

/**
 * 
 * @see {@code java.util.stream.Collectors}
 * 
 */
public abstract class Collectors {
    static final Object NONE = new Object();

    @Deprecated
    static final Set<Characteristics> CH_CONCURRENT_ID = Collections
            .unmodifiableSet(EnumSet.of(Characteristics.CONCURRENT, Characteristics.UNORDERED, Characteristics.IDENTITY_FINISH));
    @Deprecated
    static final Set<Characteristics> CH_CONCURRENT_NOID = Collections.unmodifiableSet(EnumSet.of(Characteristics.CONCURRENT, Characteristics.UNORDERED));

    static final Set<Characteristics> CH_UNORDERED_ID = Collections.unmodifiableSet(EnumSet.of(Characteristics.UNORDERED, Characteristics.IDENTITY_FINISH));
    static final Set<Characteristics> CH_UNORDERED_NOID = Collections.unmodifiableSet(EnumSet.of(Characteristics.UNORDERED));

    static final Set<Characteristics> CH_ID = Collections.unmodifiableSet(EnumSet.of(Characteristics.IDENTITY_FINISH));
    static final Set<Characteristics> CH_NOID = Collections.emptySet();

    // ============================================================================================================

    static final Function<List<Object>, ImmutableList<Object>> ImmutableList_Finisher = new Function<List<Object>, ImmutableList<Object>>() {
        @Override
        public ImmutableList<Object> apply(List<Object> t) {
            return ImmutableList.of(t);
        }
    };

    static final Function<Set<Object>, ImmutableSet<Object>> ImmutableSet_Finisher = new Function<Set<Object>, ImmutableSet<Object>>() {
        @Override
        public ImmutableSet<Object> apply(Set<Object> t) {
            return ImmutableSet.of(t);
        }
    };

    static final Function<Map<Object, Object>, ImmutableMap<Object, Object>> ImmutableMap_Finisher = new Function<Map<Object, Object>, ImmutableMap<Object, Object>>() {
        @Override
        public ImmutableMap<Object, Object> apply(Map<Object, Object> t) {
            return ImmutableMap.of(t);
        }
    };

    static final BiConsumer<Multiset<Object>, Object> Multiset_Accumulator = new BiConsumer<Multiset<Object>, Object>() {
        @Override
        public void accept(Multiset<Object> c, Object t) {
            c.add(t);
        }
    };

    static final BinaryOperator<Multiset<Object>> Multiset_Combiner = new BinaryOperator<Multiset<Object>>() {
        @Override
        public Multiset<Object> apply(Multiset<Object> a, Multiset<Object> b) {
            a.addAll(b);
            return a;
        }
    };

    static final BiConsumer<LongMultiset<Object>, Object> LongMultiset_Accumulator = new BiConsumer<LongMultiset<Object>, Object>() {
        @Override
        public void accept(LongMultiset<Object> c, Object t) {
            c.add(t);
        }
    };

    static final BinaryOperator<LongMultiset<Object>> LongMultiset_Combiner = new BinaryOperator<LongMultiset<Object>>() {
        @Override
        public LongMultiset<Object> apply(LongMultiset<Object> a, LongMultiset<Object> b) {
            a.addAll(b);
            return a;
        }
    };

    static final BiConsumer<BooleanList, Boolean> BooleanList_Accumulator = new BiConsumer<BooleanList, Boolean>() {
        @Override
        public void accept(BooleanList c, Boolean t) {
            c.add(t.booleanValue());
        }
    };

    static final BinaryOperator<BooleanList> BooleanList_Combiner = new BinaryOperator<BooleanList>() {
        @Override
        public BooleanList apply(BooleanList a, BooleanList b) {
            a.addAll(b);
            return a;
        }
    };

    static final Function<BooleanList, boolean[]> BooleanArray_Finisher = new Function<BooleanList, boolean[]>() {
        @Override
        public boolean[] apply(BooleanList t) {
            return t.trimToSize().array();
        }
    };

    static final BiConsumer<CharList, Character> CharList_Accumulator = new BiConsumer<CharList, Character>() {
        @Override
        public void accept(CharList c, Character t) {
            c.add(t.charValue());
        }
    };

    static final BinaryOperator<CharList> CharList_Combiner = new BinaryOperator<CharList>() {
        @Override
        public CharList apply(CharList a, CharList b) {
            a.addAll(b);
            return a;
        }
    };

    static final Function<CharList, char[]> CharArray_Finisher = new Function<CharList, char[]>() {
        @Override
        public char[] apply(CharList t) {
            return t.trimToSize().array();
        }
    };

    static final BiConsumer<ByteList, Byte> ByteList_Accumulator = new BiConsumer<ByteList, Byte>() {
        @Override
        public void accept(ByteList c, Byte t) {
            c.add(t.byteValue());
        }
    };

    static final BinaryOperator<ByteList> ByteList_Combiner = new BinaryOperator<ByteList>() {
        @Override
        public ByteList apply(ByteList a, ByteList b) {
            if (a.size() >= b.size()) {
                a.addAll(b);
                return a;
            } else {
                b.addAll(a);
                return b;
            }
        }
    };

    static final Function<ByteList, byte[]> ByteArray_Finisher = new Function<ByteList, byte[]>() {
        @Override
        public byte[] apply(ByteList t) {
            return t.trimToSize().array();
        }
    };

    static final BiConsumer<ShortList, Short> ShortList_Accumulator = new BiConsumer<ShortList, Short>() {
        @Override
        public void accept(ShortList c, Short t) {
            c.add(t.shortValue());
        }
    };

    static final BinaryOperator<ShortList> ShortList_Combiner = new BinaryOperator<ShortList>() {
        @Override
        public ShortList apply(ShortList a, ShortList b) {
            if (a.size() >= b.size()) {
                a.addAll(b);
                return a;
            } else {
                b.addAll(a);
                return b;
            }
        }
    };

    static final Function<ShortList, short[]> ShortArray_Finisher = new Function<ShortList, short[]>() {
        @Override
        public short[] apply(ShortList t) {
            return t.trimToSize().array();
        }
    };

    static final BiConsumer<IntList, Integer> IntList_Accumulator = new BiConsumer<IntList, Integer>() {
        @Override
        public void accept(IntList c, Integer t) {
            c.add(t.intValue());
        }
    };

    static final BinaryOperator<IntList> IntList_Combiner = new BinaryOperator<IntList>() {
        @Override
        public IntList apply(IntList a, IntList b) {
            if (a.size() >= b.size()) {
                a.addAll(b);
                return a;
            } else {
                b.addAll(a);
                return b;
            }
        }
    };

    static final Function<IntList, int[]> IntArray_Finisher = new Function<IntList, int[]>() {
        @Override
        public int[] apply(IntList t) {
            return t.trimToSize().array();
        }
    };

    static final BiConsumer<LongList, Long> LongList_Accumulator = new BiConsumer<LongList, Long>() {
        @Override
        public void accept(LongList c, Long t) {
            c.add(t.longValue());
        }
    };

    static final BinaryOperator<LongList> LongList_Combiner = new BinaryOperator<LongList>() {
        @Override
        public LongList apply(LongList a, LongList b) {
            if (a.size() >= b.size()) {
                a.addAll(b);
                return a;
            } else {
                b.addAll(a);
                return b;
            }
        }
    };

    static final Function<LongList, long[]> LongArray_Finisher = new Function<LongList, long[]>() {
        @Override
        public long[] apply(LongList t) {
            return t.trimToSize().array();
        }
    };

    static final BiConsumer<FloatList, Float> FloatList_Accumulator = new BiConsumer<FloatList, Float>() {
        @Override
        public void accept(FloatList c, Float t) {
            c.add(t.floatValue());
        }
    };

    static final BinaryOperator<FloatList> FloatList_Combiner = new BinaryOperator<FloatList>() {
        @Override
        public FloatList apply(FloatList a, FloatList b) {
            if (a.size() >= b.size()) {
                a.addAll(b);
                return a;
            } else {
                b.addAll(a);
                return b;
            }
        }
    };

    static final Function<FloatList, float[]> FloatArray_Finisher = new Function<FloatList, float[]>() {
        @Override
        public float[] apply(FloatList t) {
            return t.trimToSize().array();
        }
    };

    static final BiConsumer<DoubleList, Double> DoubleList_Accumulator = new BiConsumer<DoubleList, Double>() {
        @Override
        public void accept(DoubleList c, Double t) {
            c.add(t.doubleValue());
        }
    };

    static final BinaryOperator<DoubleList> DoubleList_Combiner = new BinaryOperator<DoubleList>() {
        @Override
        public DoubleList apply(DoubleList a, DoubleList b) {
            if (a.size() >= b.size()) {
                a.addAll(b);
                return a;
            } else {
                b.addAll(a);
                return b;
            }
        }
    };

    static final Function<DoubleList, double[]> DoubleArray_Finisher = new Function<DoubleList, double[]>() {
        @Override
        public double[] apply(DoubleList t) {
            return t.trimToSize().array();
        }
    };

    static final BiConsumer<Joiner, CharSequence> Joiner_Accumulator = new BiConsumer<Joiner, CharSequence>() {
        @Override
        public void accept(Joiner a, CharSequence t) {
            a.append(t);
        }
    };

    static final BinaryOperator<Joiner> Joiner_Combiner = new BinaryOperator<Joiner>() {
        @Override
        public Joiner apply(Joiner a, Joiner b) {
            if (a.length() > b.length()) {
                a.merge(b);
                b.close();
                return a;
            } else {
                b.merge(a);
                a.close();
                return b;
            }
        }
    };

    static final Function<Joiner, String> Joiner_Finisher = new Function<Joiner, String>() {
        @Override
        public String apply(Joiner a) {
            return a.toString();
        }
    };

    static final Function<Object, ? extends Long> Counting_Accumulator = new Function<Object, Long>() {
        @Override
        public Long apply(Object t) {
            return 1L;
        }
    };

    static final BinaryOperator<Long> Counting_Combiner = new BinaryOperator<Long>() {
        @Override
        public Long apply(Long a, Long b) {
            return a.longValue() + b.longValue();
        }
    };

    static final Function<Object, ? extends Integer> CountingInt_Accumulator = new Function<Object, Integer>() {
        @Override
        public Integer apply(Object t) {
            return 1;
        }
    };

    static final BinaryOperator<Integer> CountingInt_Combiner = new BinaryOperator<Integer>() {
        @Override
        public Integer apply(Integer a, Integer b) {
            return a.intValue() + b.intValue();
        }
    };

    static final Supplier<long[]> SummingInt_Supplier = new Supplier<long[]>() {
        @Override
        public long[] get() {
            return new long[1];
        }
    };

    static final BinaryOperator<long[]> SummingInt_Combiner = new BinaryOperator<long[]>() {
        @Override
        public long[] apply(long[] a, long[] b) {
            a[0] += b[0];
            return a;
        }
    };

    static final Function<long[], Integer> SummingInt_Finisher = new Function<long[], Integer>() {
        @Override
        public Integer apply(long[] a) {
            return N.toIntExact(a[0]);
        }
    };

    static final Supplier<long[]> SummingInt_Supplier_2 = new Supplier<long[]>() {
        @Override
        public long[] get() {
            return new long[2];
        }
    };

    static final BinaryOperator<long[]> SummingInt_Combiner_2 = new BinaryOperator<long[]>() {
        @Override
        public long[] apply(long[] a, long[] b) {
            a[0] += b[0];
            a[1] += b[1];
            return a;
        }
    };

    static final Function<long[], OptionalInt> SummingInt_Finisher_2 = new Function<long[], OptionalInt>() {
        @Override
        public OptionalInt apply(long[] a) {
            return a[1] == 0 ? OptionalInt.empty() : OptionalInt.of(N.toIntExact(a[0]));
        }
    };

    static final Supplier<long[]> SummingLong_Supplier = new Supplier<long[]>() {
        @Override
        public long[] get() {
            return new long[1];
        }
    };

    static final BinaryOperator<long[]> SummingLong_Combiner = new BinaryOperator<long[]>() {
        @Override
        public long[] apply(long[] a, long[] b) {
            a[0] += b[0];
            return a;
        }
    };

    static final Function<long[], Long> SummingLong_Finisher = new Function<long[], Long>() {
        @Override
        public Long apply(long[] a) {
            return a[0];
        }
    };

    static final Supplier<long[]> SummingLong_Supplier_2 = new Supplier<long[]>() {
        @Override
        public long[] get() {
            return new long[2];
        }
    };

    static final BinaryOperator<long[]> SummingLong_Combiner_2 = new BinaryOperator<long[]>() {
        @Override
        public long[] apply(long[] a, long[] b) {
            a[0] += b[0];
            a[1] += b[1];
            return a;
        }
    };

    static final Function<long[], OptionalLong> SummingLong_Finisher_2 = new Function<long[], OptionalLong>() {
        @Override
        public OptionalLong apply(long[] a) {
            return a[1] == 0 ? OptionalLong.empty() : OptionalLong.of(a[0]);
        }
    };

    static final Supplier<KahanSummation> SummingDouble_Supplier = new Supplier<KahanSummation>() {
        @Override
        public KahanSummation get() {
            return new KahanSummation();
        }
    };

    static final BinaryOperator<KahanSummation> SummingDouble_Combiner = new BinaryOperator<KahanSummation>() {
        @Override
        public KahanSummation apply(KahanSummation a, KahanSummation b) {
            a.combine(b);
            return a;
        }
    };

    static final Function<KahanSummation, Double> SummingDouble_Finisher = new Function<KahanSummation, Double>() {
        @Override
        public Double apply(KahanSummation a) {
            return a.sum();
        }
    };

    static final Supplier<KahanSummation> SummingDouble_Supplier_2 = new Supplier<KahanSummation>() {
        @Override
        public KahanSummation get() {
            return new KahanSummation();
        }
    };

    static final BinaryOperator<KahanSummation> SummingDouble_Combiner_2 = new BinaryOperator<KahanSummation>() {
        @Override
        public KahanSummation apply(KahanSummation a, KahanSummation b) {
            a.combine(b);

            return a;
        }
    };

    static final Function<KahanSummation, OptionalDouble> SummingDouble_Finisher_2 = new Function<KahanSummation, OptionalDouble>() {
        @Override
        public OptionalDouble apply(KahanSummation a) {
            return a.count() == 0 ? OptionalDouble.empty() : OptionalDouble.of(a.sum());
        }
    };

    static final Supplier<BigInteger[]> SummingBigInteger_Supplier = new Supplier<BigInteger[]>() {
        @Override
        public BigInteger[] get() {
            return new BigInteger[] { BigInteger.ZERO };
        }
    };

    static final BinaryOperator<BigInteger[]> SummingBigInteger_Combiner = new BinaryOperator<BigInteger[]>() {
        @Override
        public BigInteger[] apply(BigInteger[] a, BigInteger[] b) {
            a[0] = a[0].add(b[0]);
            return a;
        }
    };

    static final Function<BigInteger[], BigInteger> SummingBigInteger_Finisher = new Function<BigInteger[], BigInteger>() {
        @Override
        public BigInteger apply(BigInteger[] a) {
            return a[0];
        }
    };

    static final Supplier<BigDecimal[]> SummingBigDecimal_Supplier = new Supplier<BigDecimal[]>() {
        @Override
        public BigDecimal[] get() {
            return new BigDecimal[] { BigDecimal.ZERO };
        }
    };

    static final BinaryOperator<BigDecimal[]> SummingBigDecimal_Combiner = new BinaryOperator<BigDecimal[]>() {
        @Override
        public BigDecimal[] apply(BigDecimal[] a, BigDecimal[] b) {
            a[0] = a[0].add(b[0]);
            return a;
        }
    };

    static final Function<BigDecimal[], BigDecimal> SummingBigDecimal_Finisher = new Function<BigDecimal[], BigDecimal>() {
        @Override
        public BigDecimal apply(BigDecimal[] a) {
            return a[0];
        }
    };

    static final Supplier<long[]> AveragingInt_Supplier = new Supplier<long[]>() {
        @Override
        public long[] get() {
            return new long[2];
        }
    };

    static final BinaryOperator<long[]> AveragingInt_Combiner = new BinaryOperator<long[]>() {
        @Override
        public long[] apply(long[] a, long[] b) {
            a[0] += b[0];
            a[1] += b[1];
            return a;
        }
    };

    static final Function<long[], Double> AveragingInt_Finisher = new Function<long[], Double>() {
        @Override
        public Double apply(long[] a) {
            return a[1] == 0 ? 0d : ((double) a[0]) / a[1];
        }
    };

    static final Function<long[], OptionalDouble> AveragingInt_Finisher_2 = new Function<long[], OptionalDouble>() {
        @Override
        public OptionalDouble apply(long[] a) {
            if (a[1] == 0) {
                return OptionalDouble.empty();
            } else {
                return OptionalDouble.of(((double) a[0]) / a[1]);
            }
        }
    };

    static final Supplier<long[]> AveragingLong_Supplier = new Supplier<long[]>() {
        @Override
        public long[] get() {
            return new long[2];
        }
    };

    static final BinaryOperator<long[]> AveragingLong_Combiner = new BinaryOperator<long[]>() {
        @Override
        public long[] apply(long[] a, long[] b) {
            a[0] += b[0];
            a[1] += b[1];
            return a;
        }
    };

    static final Function<long[], Double> AveragingLong_Finisher = new Function<long[], Double>() {
        @Override
        public Double apply(long[] a) {
            return a[1] == 0 ? 0d : ((double) a[0]) / a[1];
        }
    };

    static final Function<long[], OptionalDouble> AveragingLong_Finisher_2 = new Function<long[], OptionalDouble>() {
        @Override
        public OptionalDouble apply(long[] a) {
            if (a[1] == 0) {
                return OptionalDouble.empty();
            } else {
                return OptionalDouble.of(((double) a[0]) / a[1]);
            }
        }
    };

    static final Supplier<KahanSummation> AveragingDouble_Supplier = new Supplier<KahanSummation>() {
        @Override
        public KahanSummation get() {
            return new KahanSummation();
        }
    };

    static final BinaryOperator<KahanSummation> AveragingDouble_Combiner = new BinaryOperator<KahanSummation>() {
        @Override
        public KahanSummation apply(KahanSummation a, KahanSummation b) {
            a.combine(b);
            return a;
        }
    };

    static final Function<KahanSummation, Double> AveragingDouble_Finisher = new Function<KahanSummation, Double>() {
        @Override
        public Double apply(KahanSummation a) {
            return a.average().orElse(0);
        }
    };

    static final Function<KahanSummation, OptionalDouble> AveragingDouble_Finisher_2 = new Function<KahanSummation, OptionalDouble>() {
        @Override
        public OptionalDouble apply(KahanSummation a) {
            return a.average();
        }
    };

    static final Supplier<Pair<BigInteger, MutableLong>> AveragingBigInteger_Supplier = new Supplier<Pair<BigInteger, MutableLong>>() {
        @Override
        public Pair<BigInteger, MutableLong> get() {
            return Pair.of(BigInteger.ZERO, MutableLong.of(0));
        }
    };

    static final BinaryOperator<Pair<BigInteger, MutableLong>> AveragingBigInteger_Combiner = new BinaryOperator<Pair<BigInteger, MutableLong>>() {
        @Override
        public Pair<BigInteger, MutableLong> apply(Pair<BigInteger, MutableLong> a, Pair<BigInteger, MutableLong> b) {
            a.setLeft(a.left.add(b.left));
            a.right.add(b.right.value());
            return a;
        }
    };

    static final Function<Pair<BigInteger, MutableLong>, BigDecimal> AveragingBigInteger_Finisher = new Function<Pair<BigInteger, MutableLong>, BigDecimal>() {
        @Override
        public BigDecimal apply(Pair<BigInteger, MutableLong> a) {
            return a.right.value() == 0 ? BigDecimal.ZERO : new BigDecimal(a.left).divide(new BigDecimal(a.right.value()));
        }
    };

    static final Supplier<Pair<BigDecimal, MutableLong>> AveragingBigDecimal_Supplier = new Supplier<Pair<BigDecimal, MutableLong>>() {
        @Override
        public Pair<BigDecimal, MutableLong> get() {
            return Pair.of(BigDecimal.ZERO, MutableLong.of(0));
        }
    };

    static final BinaryOperator<Pair<BigDecimal, MutableLong>> AveragingBigDecimal_Combiner = new BinaryOperator<Pair<BigDecimal, MutableLong>>() {
        @Override
        public Pair<BigDecimal, MutableLong> apply(Pair<BigDecimal, MutableLong> a, Pair<BigDecimal, MutableLong> b) {
            a.setLeft(a.left.add(b.left));
            a.right.add(b.right.value());
            return a;
        }
    };

    static final Function<Pair<BigDecimal, MutableLong>, BigDecimal> AveragingBigDecimal_Finisher = new Function<Pair<BigDecimal, MutableLong>, BigDecimal>() {
        @Override
        public BigDecimal apply(Pair<BigDecimal, MutableLong> a) {
            return a.right.value() == 0 ? BigDecimal.ZERO : a.left.divide(new BigDecimal(a.right.value()));
        }
    };

    static final Supplier<CharSummaryStatistics> SummarizingChar_Supplier = new Supplier<CharSummaryStatistics>() {
        @Override
        public CharSummaryStatistics get() {
            return new CharSummaryStatistics();
        }
    };

    static final BinaryOperator<CharSummaryStatistics> SummarizingChar_Combiner = new BinaryOperator<CharSummaryStatistics>() {
        @Override
        public CharSummaryStatistics apply(CharSummaryStatistics a, CharSummaryStatistics b) {
            a.combine(b);
            return a;
        }
    };

    static final Supplier<ByteSummaryStatistics> SummarizingByte_Supplier = new Supplier<ByteSummaryStatistics>() {
        @Override
        public ByteSummaryStatistics get() {
            return new ByteSummaryStatistics();
        }
    };

    static final BinaryOperator<ByteSummaryStatistics> SummarizingByte_Combiner = new BinaryOperator<ByteSummaryStatistics>() {
        @Override
        public ByteSummaryStatistics apply(ByteSummaryStatistics a, ByteSummaryStatistics b) {
            a.combine(b);
            return a;
        }
    };

    static final Supplier<ShortSummaryStatistics> SummarizingShort_Supplier = new Supplier<ShortSummaryStatistics>() {
        @Override
        public ShortSummaryStatistics get() {
            return new ShortSummaryStatistics();
        }
    };

    static final BinaryOperator<ShortSummaryStatistics> SummarizingShort_Combiner = new BinaryOperator<ShortSummaryStatistics>() {
        @Override
        public ShortSummaryStatistics apply(ShortSummaryStatistics a, ShortSummaryStatistics b) {
            a.combine(b);
            return a;
        }
    };

    static final Supplier<IntSummaryStatistics> SummarizingInt_Supplier = new Supplier<IntSummaryStatistics>() {
        @Override
        public IntSummaryStatistics get() {
            return new IntSummaryStatistics();
        }
    };

    static final BinaryOperator<IntSummaryStatistics> SummarizingInt_Combiner = new BinaryOperator<IntSummaryStatistics>() {
        @Override
        public IntSummaryStatistics apply(IntSummaryStatistics a, IntSummaryStatistics b) {
            a.combine(b);
            return a;
        }
    };

    static final Supplier<LongSummaryStatistics> SummarizingLong_Supplier = new Supplier<LongSummaryStatistics>() {
        @Override
        public LongSummaryStatistics get() {
            return new LongSummaryStatistics();
        }
    };

    static final BinaryOperator<LongSummaryStatistics> SummarizingLong_Combiner = new BinaryOperator<LongSummaryStatistics>() {
        @Override
        public LongSummaryStatistics apply(LongSummaryStatistics a, LongSummaryStatistics b) {
            a.combine(b);
            return a;
        }
    };

    static final Supplier<FloatSummaryStatistics> SummarizingFloat_Supplier = new Supplier<FloatSummaryStatistics>() {
        @Override
        public FloatSummaryStatistics get() {
            return new FloatSummaryStatistics();
        }
    };

    static final BinaryOperator<FloatSummaryStatistics> SummarizingFloat_Combiner = new BinaryOperator<FloatSummaryStatistics>() {
        @Override
        public FloatSummaryStatistics apply(FloatSummaryStatistics a, FloatSummaryStatistics b) {
            a.combine(b);
            return a;
        }
    };

    static final Supplier<DoubleSummaryStatistics> SummarizingDouble_Supplier = new Supplier<DoubleSummaryStatistics>() {
        @Override
        public DoubleSummaryStatistics get() {
            return new DoubleSummaryStatistics();
        }
    };

    static final BinaryOperator<DoubleSummaryStatistics> SummarizingDouble_Combiner = new BinaryOperator<DoubleSummaryStatistics>() {
        @Override
        public DoubleSummaryStatistics apply(DoubleSummaryStatistics a, DoubleSummaryStatistics b) {
            a.combine(b);
            return a;
        }
    };

    static final Function<Holder<Object>, Object> Reducing_Finisher_0 = new Function<Holder<Object>, Object>() {
        @Override
        public Object apply(Holder<Object> a) {
            return a.value();
        }
    };

    static final BiConsumer<OptHolder<Object>, Object> Reducing_Accumulator = new BiConsumer<OptHolder<Object>, Object>() {
        @Override
        public void accept(OptHolder<Object> a, Object t) {
            a.accept(t);
        }
    };

    static final BinaryOperator<OptHolder<Object>> Reducing_Combiner = new BinaryOperator<OptHolder<Object>>() {
        @Override
        public OptHolder<Object> apply(OptHolder<Object> a, OptHolder<Object> b) {
            if (b.present) {
                a.accept(b.value);
            }

            return a;
        }
    };

    static final Function<OptHolder<Object>, Optional<Object>> Reducing_Finisher = new Function<OptHolder<Object>, Optional<Object>>() {
        @Override
        public Optional<Object> apply(OptHolder<Object> a) {
            return a.present ? Optional.of(a.value) : (Optional<Object>) Optional.empty();
        }
    };

    static final BiConsumer<MappingOptHolder<Object, Object>, Object> Reducing_Accumulator_2 = new BiConsumer<MappingOptHolder<Object, Object>, Object>() {
        @Override
        public void accept(MappingOptHolder<Object, Object> a, Object t) {
            a.accept(t);
        }
    };

    static final BinaryOperator<MappingOptHolder<Object, Object>> Reducing_Combiner_2 = new BinaryOperator<MappingOptHolder<Object, Object>>() {
        @Override
        public MappingOptHolder<Object, Object> apply(MappingOptHolder<Object, Object> a, MappingOptHolder<Object, Object> b) {
            if (b.present) {
                if (a.present) {
                    a.value = a.op.apply(a.value, b.value);
                } else {
                    a.value = b.value;
                    a.present = true;
                }
            }

            return a;
        }
    };

    static final Function<MappingOptHolder<Object, Object>, Optional<Object>> Reducing_Finisher_2 = new Function<MappingOptHolder<Object, Object>, Optional<Object>>() {
        @Override
        public Optional<Object> apply(MappingOptHolder<Object, Object> a) {
            return a.present ? Optional.of(a.value) : (Optional<Object>) Optional.empty();
        }
    };

    // ============================================================================================================

    Collectors() {
    }

    static class CollectorImpl<T, A, R> implements Collector<T, A, R> {
        private static final Function<Object, Object> IDENTITY_FINISHER = new Function<Object, Object>() {
            @Override
            public Object apply(Object t) {
                return t;
            }
        };

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

    public static <T, C extends Collection<T>> Collector<T, ?, C> toCollection(Supplier<? extends C> collectionFactory) {
        final BiConsumer<C, T> accumulator = BiConsumers.ofAdd();
        final BinaryOperator<C> combiner = BinaryOperators.<T, C> ofAddAllToBigger();

        return new CollectorImpl<>(collectionFactory, accumulator, combiner, CH_ID);
    }

    public static <T> Collector<T, ?, List<T>> toList() {
        final Supplier<List<T>> supplier = Suppliers.<T> ofList();

        return toCollection(supplier);
    }

    public static <T> Collector<T, ?, LinkedList<T>> toLinkedList() {
        final Supplier<LinkedList<T>> supplier = Suppliers.<T> ofLinkedList();

        return toCollection(supplier);
    }

    public static <T> Collector<T, ?, ImmutableList<T>> toImmutableList() {
        final Collector<T, ?, List<T>> downstream = toList();
        @SuppressWarnings("rawtypes")
        final Function<List<T>, ImmutableList<T>> finisher = (Function) ImmutableList_Finisher;

        return collectingAndThen(downstream, finisher);
    }

    public static <T> Collector<T, ?, Set<T>> toSet() {
        final Supplier<Set<T>> supplier = Suppliers.<T> ofSet();

        return toCollection(supplier);
    }

    public static <T> Collector<T, ?, LinkedHashSet<T>> toLinkedHashSet() {
        final Supplier<LinkedHashSet<T>> supplier = Suppliers.<T> ofLinkedHashSet();

        return toCollection(supplier);
    }

    public static <T> Collector<T, ?, ImmutableSet<T>> toImmutableSet() {
        final Collector<T, ?, Set<T>> downstream = toSet();
        @SuppressWarnings("rawtypes")
        final Function<Set<T>, ImmutableSet<T>> finisher = (Function) ImmutableSet_Finisher;

        return collectingAndThen(downstream, finisher);
    }

    public static <T> Collector<T, ?, Queue<T>> toQueue() {
        final Supplier<Queue<T>> supplier = Suppliers.<T> ofQueue();

        return toCollection(supplier);
    }

    public static <T> Collector<T, ?, Deque<T>> toDeque() {
        final Supplier<Deque<T>> supplier = Suppliers.<T> ofDeque();

        return toCollection(supplier);
    }

    public static <T, C extends Collection<T>> Collector<T, ?, C> toCollection(final Supplier<? extends C> collectionFactory, final int atMostSize) {
        final BiConsumer<C, T> accumulator = new BiConsumer<C, T>() {
            @Override
            public void accept(C c, T t) {
                if (c.size() < atMostSize) {
                    c.add(t);
                }
            }
        };

        final BinaryOperator<C> combiner = new BinaryOperator<C>() {
            @Override
            public C apply(C a, C b) {
                if (a.size() < atMostSize) {
                    final int n = atMostSize - a.size();

                    if (b.size() <= n) {
                        a.addAll(b);
                    } else {
                        if (b instanceof List) {
                            a.addAll(((List<T>) b).subList(0, n));
                        } else {
                            final Iterator<T> iter = b.iterator();

                            for (int i = 0; i < n; i++) {
                                a.add(iter.next());
                            }
                        }
                    }
                }

                return a;
            }
        };

        return new CollectorImpl<>(collectionFactory, accumulator, combiner, CH_ID);
    }

    public static <T> Collector<T, ?, List<T>> toList(final int atMostSize) {
        final Supplier<List<T>> supplier = new Supplier<List<T>>() {
            @Override
            public List<T> get() {
                return new ArrayList<T>(N.min(256, atMostSize));
            }
        };

        return toCollection(supplier, atMostSize);
    }

    public static <T> Collector<T, ?, Set<T>> toSet(final int atMostSize) {
        final Supplier<Set<T>> supplier = new Supplier<Set<T>>() {
            @Override
            public Set<T> get() {
                return new HashSet<T>(N.initHashCapacity(atMostSize));
            }
        };

        return toCollection(supplier, atMostSize);
    }

    public static <T> Collector<T, ?, Multiset<T>> toMultiset() {
        final Supplier<Multiset<T>> supplier = Suppliers.ofMultiset();

        return toMultiset(supplier);
    }

    @SuppressWarnings("rawtypes")
    public static <T> Collector<T, ?, Multiset<T>> toMultiset(Supplier<Multiset<T>> supplier) {
        final BiConsumer<Multiset<T>, T> accumulator = (BiConsumer) Multiset_Accumulator;
        final BinaryOperator<Multiset<T>> combiner = (BinaryOperator) Multiset_Combiner;

        return new CollectorImpl<>(supplier, accumulator, combiner, CH_UNORDERED_ID);
    }

    public static <T> Collector<T, ?, LongMultiset<T>> toLongMultiset() {
        final Supplier<LongMultiset<T>> supplier = Suppliers.ofLongMultiset();

        return toLongMultiset(supplier);
    }

    @SuppressWarnings("rawtypes")
    public static <T> Collector<T, ?, LongMultiset<T>> toLongMultiset(Supplier<LongMultiset<T>> supplier) {
        final BiConsumer<LongMultiset<T>, T> accumulator = (BiConsumer) LongMultiset_Accumulator;
        final BinaryOperator<LongMultiset<T>> combiner = (BinaryOperator) LongMultiset_Combiner;

        return new CollectorImpl<>(supplier, accumulator, combiner, CH_UNORDERED_ID);
    }

    public static <T> Collector<T, ?, Object[]> toArray() {
        return toArray(Suppliers.ofEmptyObjectArray());
    }

    public static <T, A> Collector<T, ?, A[]> toArray(final Supplier<A[]> arraySupplier) {
        final Supplier<List<A>> supplier = Suppliers.<A> ofList();
        @SuppressWarnings("rawtypes")
        final BiConsumer<List<A>, T> accumulator = (BiConsumer) BiConsumers.ofAdd();
        final BinaryOperator<List<A>> combiner = BinaryOperators.<A, List<A>> ofAddAllToBigger();
        final Function<List<A>, A[]> finisher = new Function<List<A>, A[]>() {
            @Override
            public A[] apply(List<A> t) {
                final A[] a = arraySupplier.get();

                if (a.length >= t.size()) {
                    return t.toArray(a);
                } else {
                    return t.toArray((A[]) Array.newInstance(a.getClass().getComponentType(), t.size()));
                }
            }
        };

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_NOID);
    }

    public static <T, A> Collector<T, ?, A[]> toArray(final IntFunction<A[]> arraySupplier) {
        final Supplier<List<A>> supplier = Suppliers.<A> ofList();
        @SuppressWarnings("rawtypes")
        final BiConsumer<List<A>, T> accumulator = (BiConsumer) BiConsumers.ofAdd();
        final BinaryOperator<List<A>> combiner = BinaryOperators.<A, List<A>> ofAddAllToBigger();
        final Function<List<A>, A[]> finisher = new Function<List<A>, A[]>() {
            @Override
            public A[] apply(List<A> t) {
                return t.toArray(arraySupplier.apply(t.size()));
            }
        };

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_NOID);
    }

    public static Collector<Boolean, ?, BooleanList> toBooleanList() {
        final Supplier<BooleanList> supplier = Suppliers.ofBooleanList();
        final BiConsumer<BooleanList, Boolean> accumulator = BooleanList_Accumulator;
        final BinaryOperator<BooleanList> combiner = BooleanList_Combiner;

        return new CollectorImpl<>(supplier, accumulator, combiner, CH_ID);
    }

    public static Collector<Boolean, ?, boolean[]> toBooleanArray() {
        final Supplier<BooleanList> supplier = Suppliers.ofBooleanList();
        final BiConsumer<BooleanList, Boolean> accumulator = BooleanList_Accumulator;
        final BinaryOperator<BooleanList> combiner = BooleanList_Combiner;
        final Function<BooleanList, boolean[]> finisher = BooleanArray_Finisher;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_NOID);
    }

    public static Collector<Character, ?, CharList> toCharList() {
        final Supplier<CharList> supplier = Suppliers.ofCharList();
        final BiConsumer<CharList, Character> accumulator = CharList_Accumulator;
        final BinaryOperator<CharList> combiner = CharList_Combiner;

        return new CollectorImpl<>(supplier, accumulator, combiner, CH_ID);
    }

    public static Collector<Character, ?, char[]> toCharArray() {
        final Supplier<CharList> supplier = Suppliers.ofCharList();
        final BiConsumer<CharList, Character> accumulator = CharList_Accumulator;
        final BinaryOperator<CharList> combiner = CharList_Combiner;
        final Function<CharList, char[]> finisher = CharArray_Finisher;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_NOID);
    }

    public static Collector<Byte, ?, ByteList> toByteList() {
        final Supplier<ByteList> supplier = Suppliers.ofByteList();
        final BiConsumer<ByteList, Byte> accumulator = ByteList_Accumulator;
        final BinaryOperator<ByteList> combiner = ByteList_Combiner;

        return new CollectorImpl<>(supplier, accumulator, combiner, CH_ID);
    }

    public static Collector<Byte, ?, byte[]> toByteArray() {
        final Supplier<ByteList> supplier = Suppliers.ofByteList();
        final BiConsumer<ByteList, Byte> accumulator = ByteList_Accumulator;
        final BinaryOperator<ByteList> combiner = ByteList_Combiner;
        final Function<ByteList, byte[]> finisher = ByteArray_Finisher;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_NOID);
    }

    public static Collector<Short, ?, ShortList> toShortList() {
        final Supplier<ShortList> supplier = Suppliers.ofShortList();
        final BiConsumer<ShortList, Short> accumulator = ShortList_Accumulator;
        final BinaryOperator<ShortList> combiner = ShortList_Combiner;

        return new CollectorImpl<>(supplier, accumulator, combiner, CH_ID);
    }

    public static Collector<Short, ?, short[]> toShortArray() {
        final Supplier<ShortList> supplier = Suppliers.ofShortList();
        final BiConsumer<ShortList, Short> accumulator = ShortList_Accumulator;
        final BinaryOperator<ShortList> combiner = ShortList_Combiner;
        final Function<ShortList, short[]> finisher = ShortArray_Finisher;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_NOID);
    }

    public static Collector<Integer, ?, IntList> toIntList() {
        final Supplier<IntList> supplier = Suppliers.ofIntList();
        final BiConsumer<IntList, Integer> accumulator = IntList_Accumulator;
        final BinaryOperator<IntList> combiner = IntList_Combiner;

        return new CollectorImpl<>(supplier, accumulator, combiner, CH_ID);
    }

    public static Collector<Integer, ?, int[]> toIntArray() {
        final Supplier<IntList> supplier = Suppliers.ofIntList();
        final BiConsumer<IntList, Integer> accumulator = IntList_Accumulator;
        final BinaryOperator<IntList> combiner = IntList_Combiner;
        final Function<IntList, int[]> finisher = IntArray_Finisher;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_NOID);
    }

    public static Collector<Long, ?, LongList> toLongList() {
        final Supplier<LongList> supplier = Suppliers.ofLongList();
        final BiConsumer<LongList, Long> accumulator = LongList_Accumulator;
        final BinaryOperator<LongList> combiner = LongList_Combiner;

        return new CollectorImpl<>(supplier, accumulator, combiner, CH_ID);
    }

    public static Collector<Long, ?, long[]> toLongArray() {
        final Supplier<LongList> supplier = Suppliers.ofLongList();
        final BiConsumer<LongList, Long> accumulator = LongList_Accumulator;
        final BinaryOperator<LongList> combiner = LongList_Combiner;
        final Function<LongList, long[]> finisher = LongArray_Finisher;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_NOID);
    }

    public static Collector<Float, ?, FloatList> toFloatList() {
        final Supplier<FloatList> supplier = Suppliers.ofFloatList();
        final BiConsumer<FloatList, Float> accumulator = FloatList_Accumulator;
        final BinaryOperator<FloatList> combiner = FloatList_Combiner;

        return new CollectorImpl<>(supplier, accumulator, combiner, CH_ID);
    }

    public static Collector<Float, ?, float[]> toFloatArray() {
        final Supplier<FloatList> supplier = Suppliers.ofFloatList();
        final BiConsumer<FloatList, Float> accumulator = FloatList_Accumulator;
        final BinaryOperator<FloatList> combiner = FloatList_Combiner;
        final Function<FloatList, float[]> finisher = FloatArray_Finisher;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_NOID);
    }

    public static Collector<Double, ?, DoubleList> toDoubleList() {
        final Supplier<DoubleList> supplier = Suppliers.ofDoubleList();
        final BiConsumer<DoubleList, Double> accumulator = DoubleList_Accumulator;
        final BinaryOperator<DoubleList> combiner = DoubleList_Combiner;

        return new CollectorImpl<>(supplier, accumulator, combiner, CH_ID);
    }

    public static Collector<Double, ?, double[]> toDoubleArray() {
        final Supplier<DoubleList> supplier = Suppliers.ofDoubleList();
        final BiConsumer<DoubleList, Double> accumulator = DoubleList_Accumulator;
        final BinaryOperator<DoubleList> combiner = DoubleList_Combiner;
        final Function<DoubleList, double[]> finisher = DoubleArray_Finisher;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_NOID);
    }

    private static final Supplier<Holder<Optional<Object>>> onlyOne_supplier = new Supplier<Holder<Optional<Object>>>() {
        @Override
        public Holder<Optional<Object>> get() {
            return Holder.of(Optional.empty());
        }
    };

    private static final BiConsumer<Holder<Optional<Object>>, Object> onlyOne_accumulator = new BiConsumer<Holder<Optional<Object>>, Object>() {
        @Override
        public void accept(Holder<Optional<Object>> holder, Object val) {
            if (holder.value().isPresent()) {
                throw new DuplicatedResultException("Duplicate values");
            }

            holder.setValue(Optional.of(val));
        }
    };

    private static final BinaryOperator<Holder<Optional<Object>>> onlyOne_combiner = new BinaryOperator<Holder<Optional<Object>>>() {
        @Override
        public Holder<Optional<Object>> apply(Holder<Optional<Object>> t, Holder<Optional<Object>> u) {
            if (t.value().isPresent() && u.value().isPresent()) {
                throw new DuplicatedResultException("Duplicate values");
            }

            return t.value().isPresent() ? t : u;
        }
    };

    private static final Function<Holder<Optional<Object>>, Optional<Object>> onlyOne_finisher = new Function<Holder<Optional<Object>>, Optional<Object>>() {
        @Override
        public Optional<Object> apply(Holder<Optional<Object>> t) {
            return t.value();
        }
    };

    /**
     * {@code DuplicatedResultException} is threw if there are more than one values are collected.
     * 
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
     * {@code DuplicatedResultException} is threw if there are more than one values are collected.
     * 
     * @param predicate
     * @return
     */
    public static <T> Collector<T, ?, Optional<T>> onlyOne(final Predicate<? super T> predicate) {
        final Collector<T, ?, Optional<T>> downstream = onlyOne();

        return filtering(predicate, downstream);
    }

    private static final Supplier<Holder<Object>> first_last_supplier = new Supplier<Holder<Object>>() {
        @Override
        public Holder<Object> get() {
            return Holder.of(NONE);
        }
    };

    private static final BiConsumer<Holder<Object>, Object> first_accumulator = new BiConsumer<Holder<Object>, Object>() {
        @Override
        public void accept(Holder<Object> holder, Object val) {
            if (holder.value() == NONE) {
                holder.setValue(val);
            }
        }
    };

    private static final BiConsumer<Holder<Object>, Object> last_accumulator = new BiConsumer<Holder<Object>, Object>() {
        @Override
        public void accept(Holder<Object> holder, Object val) {
            holder.setValue(val);
        }
    };

    private static final BinaryOperator<Holder<Object>> first_last_combiner = new BinaryOperator<Holder<Object>>() {
        @Override
        public Holder<Object> apply(Holder<Object> t, Holder<Object> u) {
            if (t.value() != NONE && u.value() != NONE) {
                throw new UnsupportedOperationException("The 'first' and 'last' Collector only can be used in sequential stream");
            }

            return t.value() != NONE ? t : u;
        }
    };

    private static final Function<Holder<Object>, Optional<Object>> first_last_finisher = new Function<Holder<Object>, Optional<Object>>() {
        @Override
        public Optional<Object> apply(Holder<Object> t) {
            return t.value() == NONE ? Optional.empty() : Optional.of(t.value());
        }
    };

    /** 
     * Only works for sequential Stream.
     * 
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
     * @param n
     * @return
     * @throws UnsupportedOperationException operated by multiple threads
     */
    public static <T> Collector<T, ?, List<T>> first(final int n) {
        N.checkArgNotNegative(n, "n");

        final Supplier<List<T>> supplier = new Supplier<List<T>>() {
            @Override
            public List<T> get() {
                return new ArrayList<T>(N.min(256, n));
            }
        };

        final BiConsumer<List<T>, T> accumulator = new BiConsumer<List<T>, T>() {
            @Override
            public void accept(List<T> c, T t) {
                if (c.size() < n) {
                    c.add(t);
                }
            }
        };

        final BinaryOperator<List<T>> combiner = new BinaryOperator<List<T>>() {
            @Override
            public List<T> apply(List<T> a, List<T> b) {
                if (N.notNullOrEmpty(a) && N.notNullOrEmpty(b)) {
                    throw new UnsupportedOperationException("The 'first' and 'last' Collector only can be used in sequential stream");
                }

                return a.size() > 0 ? a : b;
            }
        };

        return new CollectorImpl<>(supplier, accumulator, combiner, CH_ID);
    }

    /**
     * Only works for sequential Stream.
     * 
     * @param n
     * @return
     * @throws UnsupportedOperationException operated by multiple threads
     */
    public static <T> Collector<T, ?, List<T>> last(final int n) {
        N.checkArgNotNegative(n, "n");

        final Supplier<Deque<T>> supplier = new Supplier<Deque<T>>() {
            @Override
            public Deque<T> get() {
                return n <= 1024 ? new ArrayDeque<T>(n) : new LinkedList<T>();
            }
        };

        final BiConsumer<Deque<T>, T> accumulator = new BiConsumer<Deque<T>, T>() {
            @Override
            public void accept(Deque<T> dqueue, T t) {
                if (n > 0) {
                    if (dqueue.size() >= n) {
                        dqueue.pollFirst();
                    }

                    dqueue.offerLast(t);
                }
            }
        };

        final BinaryOperator<Deque<T>> combiner = new BinaryOperator<Deque<T>>() {
            @Override
            public Deque<T> apply(Deque<T> a, Deque<T> b) {
                if (N.notNullOrEmpty(a) && N.notNullOrEmpty(b)) {
                    throw new UnsupportedOperationException("The 'first' and 'last' Collector only can be used in sequential stream");
                }

                while (b.size() < n && !a.isEmpty()) {
                    b.addFirst(a.pollLast());
                }

                return b;
            }
        };

        final Function<Deque<T>, List<T>> finisher = new Function<Deque<T>, List<T>>() {
            @Override
            public List<T> apply(Deque<T> dqueue) {
                return new ArrayList<>(dqueue);
            }
        };

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_NOID);
    }

    public static Collector<CharSequence, ?, String> joining() {
        return joining("", "", "");
    }

    public static Collector<CharSequence, ?, String> joining(CharSequence delimiter) {
        return joining(delimiter, "", "");
    }

    public static Collector<CharSequence, ?, String> joining(final CharSequence delimiter, final CharSequence prefix, final CharSequence suffix) {
        final Supplier<Joiner> supplier = new Supplier<Joiner>() {
            @Override
            public Joiner get() {
                return Joiner.with(delimiter, prefix, suffix).reuseCachedBuffer(true);
            }
        };

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

        final BiConsumer<A, T> accumulator = new BiConsumer<A, T>() {
            @Override
            public void accept(A a, T t) {
                if (predicate.test(t)) {
                    downstreamAccumulator.accept(a, t);
                }
            }
        };

        return new CollectorImpl<>(downstream.supplier(), accumulator, downstream.combiner(), downstream.finisher(), downstream.characteristics());
    }

    public static <T, U> Collector<T, ?, List<U>> mapping(Function<? super T, ? extends U> mapper) {
        return Collectors.mapping(mapper, Collectors.<U> toList());
    }

    public static <T, U, A, R> Collector<T, ?, R> mapping(final Function<? super T, ? extends U> mapper, final Collector<? super U, A, R> downstream) {
        final BiConsumer<A, ? super U> downstreamAccumulator = downstream.accumulator();

        final BiConsumer<A, T> accumulator = new BiConsumer<A, T>() {
            @Override
            public void accept(A a, T t) {
                downstreamAccumulator.accept(a, mapper.apply(t));
            }
        };

        return new CollectorImpl<>(downstream.supplier(), accumulator, downstream.combiner(), downstream.finisher(), downstream.characteristics());
    }

    public static <T, U> Collector<T, ?, List<U>> flatMapping(final Function<? super T, ? extends Stream<? extends U>> mapper) {
        return flatMapping(mapper, Collectors.<U> toList());
    }

    public static <T, U, A, R> Collector<T, ?, R> flatMapping(final Function<? super T, ? extends Stream<? extends U>> mapper,
            final Collector<? super U, A, R> downstream) {
        final BiConsumer<A, ? super U> downstreamAccumulator = downstream.accumulator();

        final BiConsumer<A, T> accumulator = new BiConsumer<A, T>() {
            @Override
            public void accept(final A a, final T t) {
                try (Stream<? extends U> stream = mapper.apply(t)) {
                    final ObjIterator<? extends U> iter = stream.iteratorEx();

                    while (iter.hasNext()) {
                        downstreamAccumulator.accept(a, iter.next());
                    }
                }
            }
        };

        return new CollectorImpl<>(downstream.supplier(), accumulator, downstream.combiner(), downstream.finisher(), downstream.characteristics());
    }

    public static <T, U> Collector<T, ?, List<U>> flattMapping(final Function<? super T, ? extends Collection<? extends U>> mapper) {
        return flattMapping(mapper, Collectors.<U> toList());
    }

    public static <T, U, A, R> Collector<T, ?, R> flattMapping(final Function<? super T, ? extends Collection<? extends U>> mapper,
            final Collector<? super U, A, R> downstream) {
        final BiConsumer<A, ? super U> downstreamAccumulator = downstream.accumulator();

        final BiConsumer<A, T> accumulator = new BiConsumer<A, T>() {
            @Override
            public void accept(final A a, final T t) {
                final Collection<? extends U> c = mapper.apply(t);

                if (N.notNullOrEmpty(c)) {
                    for (U u : c) {
                        downstreamAccumulator.accept(a, u);
                    }
                }
            }
        };

        return new CollectorImpl<>(downstream.supplier(), accumulator, downstream.combiner(), downstream.finisher(), downstream.characteristics());
    }

    public static <T, T2, U> Collector<T, ?, List<U>> flatMapping(final Function<? super T, ? extends Stream<? extends T2>> flatMapper,
            final BiFunction<? super T, ? super T2, ? extends U> mapper) {
        return flatMapping(flatMapper, mapper, Collectors.<U> toList());
    }

    public static <T, T2, U, A, R> Collector<T, ?, R> flatMapping(final Function<? super T, ? extends Stream<? extends T2>> flatMapper,
            final BiFunction<? super T, ? super T2, ? extends U> mapper, final Collector<? super U, A, R> downstream) {
        final BiConsumer<A, ? super U> downstreamAccumulator = downstream.accumulator();

        final BiConsumer<A, T> accumulator = new BiConsumer<A, T>() {
            @Override
            public void accept(final A a, final T t) {
                try (Stream<? extends T2> stream = flatMapper.apply(t)) {
                    final ObjIterator<? extends T2> iter = stream.iteratorEx();

                    while (iter.hasNext()) {
                        downstreamAccumulator.accept(a, mapper.apply(t, iter.next()));
                    }
                }
            }
        };

        return new CollectorImpl<>(downstream.supplier(), accumulator, downstream.combiner(), downstream.finisher(), downstream.characteristics());
    }

    public static <T, T2, U> Collector<T, ?, List<U>> flattMapping(final Function<? super T, ? extends Collection<? extends T2>> flatMapper,
            final BiFunction<? super T, ? super T2, ? extends U> mapper) {
        return flattMapping(flatMapper, mapper, Collectors.<U> toList());
    }

    public static <T, T2, U, A, R> Collector<T, ?, R> flattMapping(final Function<? super T, ? extends Collection<? extends T2>> flatMapper,
            final BiFunction<? super T, ? super T2, ? extends U> mapper, final Collector<? super U, A, R> downstream) {
        final BiConsumer<A, ? super U> downstreamAccumulator = downstream.accumulator();

        final BiConsumer<A, T> accumulator = new BiConsumer<A, T>() {
            @Override
            public void accept(final A a, final T t) {
                final Collection<? extends T2> c = flatMapper.apply(t);

                if (N.notNullOrEmpty(c)) {
                    for (T2 t2 : c) {
                        downstreamAccumulator.accept(a, mapper.apply(t, t2));
                    }
                }
            }
        };

        return new CollectorImpl<>(downstream.supplier(), accumulator, downstream.combiner(), downstream.finisher(), downstream.characteristics());
    }

    public static <T, A, R, RR> Collector<T, A, RR> collectingAndThen(final Collector<T, A, R> downstream, final Function<R, RR> finisher) {
        N.checkArgNotNull(finisher);

        final Function<A, R> downstreamFinisher = downstream.finisher();

        final Function<A, RR> thenFinisher = new Function<A, RR>() {
            @Override
            public RR apply(A t) {
                return finisher.apply(downstreamFinisher.apply(t));
            }
        };

        Set<Characteristics> characteristics = downstream.characteristics();

        if (characteristics.contains(Characteristics.IDENTITY_FINISH)) {
            if (characteristics.size() == 1)
                characteristics = Collectors.CH_NOID;
            else {
                characteristics = EnumSet.copyOf(characteristics);
                characteristics.remove(Characteristics.IDENTITY_FINISH);
                characteristics = Collections.unmodifiableSet(characteristics);
            }
        }

        return new CollectorImpl<>(downstream.supplier(), downstream.accumulator(), downstream.combiner(), thenFinisher, characteristics);
    }

    public static <T, A, R, RR> Collector<T, A, RR> collectingAndThen(final java.util.stream.Collector<T, A, R> downstream,
            final java.util.function.Function<R, RR> finisher) {
        N.checkArgNotNull(downstream);
        N.checkArgNotNull(finisher);

        return collectingAndThen(Collector.from(downstream), r -> finisher.apply(r));
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
     * @param mapper a function which classifies input elements.
     * @return a collector which collects distinct elements to the {@code List}.
     * @since 0.3.8
     */
    public static <T> Collector<T, ?, List<T>> distinctBy(final Function<? super T, ?> mapper) {
        @SuppressWarnings("rawtypes")
        final Supplier<Map<Object, T>> supplier = (Supplier) Suppliers.<Object, T> ofLinkedHashMap();

        final BiConsumer<Map<Object, T>, T> accumulator = new BiConsumer<Map<Object, T>, T>() {
            @Override
            public void accept(Map<Object, T> map, T t) {
                final Object key = mapper.apply(t);

                if (map.containsKey(key) == false) {
                    map.put(key, t);
                }
            }
        };

        final BinaryOperator<Map<Object, T>> combiner = new BinaryOperator<Map<Object, T>>() {
            @Override
            public Map<Object, T> apply(Map<Object, T> a, Map<Object, T> b) {
                for (Map.Entry<Object, T> entry : b.entrySet()) {
                    if (a.containsKey(entry.getKey()) == false) {
                        a.put(entry.getKey(), entry.getValue());
                    }
                }

                return a;
            }
        };

        final Function<Map<Object, T>, List<T>> finisher = new Function<Map<Object, T>, List<T>>() {
            @Override
            public List<T> apply(Map<Object, T> map) {
                return new ArrayList<>(map.values());
            }
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
     * @param mapper a function which classifies input elements.
     * @return a collector which counts a number of distinct classes the mapper
     *         function returns for the stream elements.
     */
    public static <T> Collector<T, ?, Integer> distinctCount(Function<? super T, ?> mapper) {
        final Collector<Object, ?, Set<Object>> downstream = Collectors.toSet();

        final Function<Set<Object>, Integer> finisher = new Function<Set<Object>, Integer>() {
            @Override
            public Integer apply(Set<Object> t) {
                return t.size();
            }
        };

        return Collectors.collectingAndThen(Collectors.mapping(mapper, downstream), finisher);
    }

    public static <T> Collector<T, ?, Long> counting() {
        final Function<? super T, ? extends Long> accumulator = Counting_Accumulator;
        final BinaryOperator<Long> combiner = Counting_Combiner;

        return reducing(0L, accumulator, combiner);
    }

    public static <T> Collector<T, ?, Integer> countingInt() {
        final Function<? super T, ? extends Integer> accumulator = CountingInt_Accumulator;
        final BinaryOperator<Integer> combiner = CountingInt_Combiner;

        return reducing(0, accumulator, combiner);
    }

    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> Collector<T, ?, Optional<T>> min() {
        return minBy(Fn.naturalOrder());
    }

    public static <T> Collector<T, ?, Optional<T>> minBy(final Comparator<? super T> comparator) {
        N.checkArgNotNull(comparator);

        final BinaryOperator<T> op = new BinaryOperator<T>() {
            @Override
            public T apply(T a, T b) {
                return comparator.compare(a, b) <= 0 ? a : b;
            }
        };

        return reducing(op);
    }

    public static <T> Collector<T, ?, T> minByOrGet(final Comparator<? super T> comparator, final Supplier<? extends T> other) {
        N.checkArgNotNull(comparator);

        final BinaryOperator<T> op = new BinaryOperator<T>() {
            @Override
            public T apply(T a, T b) {
                return comparator.compare(a, b) <= 0 ? a : b;
            }
        };

        return reducingOrGet(op, other);
    }

    public static <T, X extends RuntimeException> Collector<T, ?, T> minByOrThrow(final Comparator<? super T> comparator,
            final Supplier<? extends X> exceptionSupplier) {
        N.checkArgNotNull(comparator);

        final BinaryOperator<T> op = new BinaryOperator<T>() {
            @Override
            public T apply(T a, T b) {
                return comparator.compare(a, b) <= 0 ? a : b;
            }
        };

        return reducingOrThrow(op, exceptionSupplier);
    }

    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> Collector<T, ?, Optional<T>> max() {
        return maxBy(Fn.naturalOrder());
    }

    public static <T> Collector<T, ?, Optional<T>> maxBy(final Comparator<? super T> comparator) {
        N.checkArgNotNull(comparator);

        final BinaryOperator<T> op = new BinaryOperator<T>() {
            @Override
            public T apply(T a, T b) {
                return comparator.compare(a, b) >= 0 ? a : b;
            }
        };

        return reducing(op);
    }

    public static <T> Collector<T, ?, T> maxByOrGet(final Comparator<? super T> comparator, final Supplier<? extends T> other) {
        N.checkArgNotNull(comparator);

        final BinaryOperator<T> op = new BinaryOperator<T>() {
            @Override
            public T apply(T a, T b) {
                return comparator.compare(a, b) >= 0 ? a : b;
            }
        };

        return reducingOrGet(op, other);
    }

    public static <T, X extends RuntimeException> Collector<T, ?, T> maxByOrThrow(final Comparator<? super T> comparator,
            final Supplier<? extends X> exceptionSupplier) {
        N.checkArgNotNull(comparator);

        final BinaryOperator<T> op = new BinaryOperator<T>() {
            @Override
            public T apply(T a, T b) {
                return comparator.compare(a, b) >= 0 ? a : b;
            }
        };

        return reducingOrThrow(op, exceptionSupplier);
    }

    /**
     * 
     * @param comparator
     * @return
     * @see Collectors#minMax(Comparator, BiFunction)
     */
    public static <T, R> Collector<T, ?, Optional<Pair<T, T>>> minMax(final Comparator<? super T> comparator) {
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

        final BiFunction<Optional<T>, Optional<T>, Optional<R>> finisher2 = new BiFunction<Optional<T>, Optional<T>, Optional<R>>() {
            @Override
            public Optional<R> apply(Optional<T> min, Optional<T> max) {
                return min.isPresent() ? Optional.of((R) finisher.apply(min.get(), max.get())) : Optional.<R> empty();
            }
        };

        return combine(Collectors.minBy(comparator), Collectors.maxBy(comparator), finisher2);
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
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> Collector<T, ?, List<T>> maxAll() {
        return maxAll(Fn.naturalOrder());
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
     * @param comparator
     * @param atMostSize
     * @return
     */
    public static <T> Collector<T, ?, List<T>> maxAll(final Comparator<? super T> comparator, final int atMostSize) {
        final Supplier<Pair<T, List<T>>> supplier = new Supplier<Pair<T, List<T>>>() {
            @Override
            public Pair<T, List<T>> get() {
                final List<T> list = new ArrayList<T>(Math.min(16, atMostSize));
                return Pair.of((T) NONE, list);
            }
        };

        final BiConsumer<Pair<T, List<T>>, T> accumulator = new BiConsumer<Pair<T, List<T>>, T>() {
            @Override
            public void accept(Pair<T, List<T>> a, T t) {
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

                    if (cmp >= 0) {
                        if (a.right.size() < atMostSize) {
                            a.right.add(t);
                        }
                    }
                }
            }
        };

        final BinaryOperator<Pair<T, List<T>>> combiner = new BinaryOperator<Pair<T, List<T>>>() {
            @Override
            public Pair<T, List<T>> apply(Pair<T, List<T>> a, Pair<T, List<T>> b) {
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
            }
        };

        final Function<Pair<T, List<T>>, List<T>> finisher = new Function<Pair<T, List<T>>, List<T>>() {
            @Override
            public List<T> apply(Pair<T, List<T>> a) {
                return a.right;
            }
        };

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    /**
     * Use occurrences to save the count of largest objects if {@code areAllLargestSame = true}(e.g. {@code Number/String/...}) and return a list by repeat the largest object {@code n} times.
     *  
     * @param areAllLargestSame
     * @return
     * @see Collectors#maxAll(Comparator, int, boolean)
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> Collector<T, ?, List<T>> maxAll(final boolean areAllLargestSame) {
        return maxAll(Integer.MAX_VALUE, areAllLargestSame);
    }

    /**
     * Use occurrences to save the count of largest objects if {@code areAllLargestSame = true}(e.g. {@code Number/String/...}) and return a list by repeat the largest object {@code n} times.
     * 
     * @param atMostSize
     * @param areAllLargestSame
     * @return
     * @see Collectors#maxAll(Comparator, int, boolean)
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> Collector<T, ?, List<T>> maxAll(final int atMostSize, final boolean areAllLargestSame) {
        return maxAll(Fn.naturalOrder(), atMostSize, areAllLargestSame);
    }

    /**
     * Use occurrences to save the count of largest objects if {@code areAllLargestSame = true}(e.g. {@code Number/String/...}) and return a list by repeat the largest object {@code n} times.
     * 
     * The default implementation is equivalent to, for this {@code map}:
     * <pre> 
     * <code>
     * if (areAllLargestSame) {
     *     final Function<Pair<Optional<T>, Integer>, List<T>> finisher = new Function<Pair<Optional<T>, Integer>, List<T>>() {
     *        @Override
     *        public List<T> apply(Pair<Optional<T>, Integer> t) {
     *            int n = N.min(atMostSize, t.right.intValue());
     *            return n == 0 ? new ArrayList<T>() : N.repeat(t.left.get(), n);
     *        }
     *     };
     *
     *     return maxAlll(comparator, countingInt(), finisher);
     * } else {
     *     return maxAll(comparator, atMostSize);
     * }
     * </code>
     * </pre>
     * @param atMostSize
     * @param areAllLargestSame
     * @return
     */
    public static <T> Collector<T, ?, List<T>> maxAll(final Comparator<? super T> comparator, final int atMostSize, final boolean areAllLargestSame) {
        N.checkArgPositive(atMostSize, "atMostSize");

        if (areAllLargestSame) {
            final Function<Pair<Optional<T>, Integer>, List<T>> finisher = new Function<Pair<Optional<T>, Integer>, List<T>>() {
                @Override
                public List<T> apply(Pair<Optional<T>, Integer> t) {
                    int n = N.min(atMostSize, t.right.intValue());
                    return n == 0 ? new ArrayList<T>() : N.repeat(t.left.get(), n);
                }
            };

            return maxAlll(comparator, countingInt(), finisher);
        } else {
            return maxAll(comparator, atMostSize);
        }
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
     * @param <D> the result type of the downstream reduction
     * @param downstream a {@code Collector} implementing the downstream
     *        reduction
     * @return a {@code Collector} which finds all the maximal elements.
     * @see #maxAll(Comparator, Collector)
     * @see #maxAll(Comparator)
     * @see #maxAll()
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable, A, D> Collector<T, ?, D> maxAll(Collector<T, A, D> downstream) {
        return maxAll(Fn.naturalOrder(), downstream);
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
     * @param <D> the result type of the downstream reduction
     * @param comparator a {@code Comparator} to compare the elements
     * @param downstream a {@code Collector} implementing the downstream
     *        reduction
     * @return a {@code Collector} which finds all the maximal elements.
     * @see #maxAll(Comparator)
     * @see #maxAll(Collector)
     * @see #maxAll()
     */
    public static <T, A, D> Collector<T, ?, D> maxAll(final Comparator<? super T> comparator, final Collector<? super T, A, D> downstream) {
        final Supplier<A> downstreamSupplier = downstream.supplier();
        final BiConsumer<A, ? super T> downstreamAccumulator = downstream.accumulator();
        final BinaryOperator<A> downstreamCombiner = downstream.combiner();
        final MutableBoolean isCollection = MutableBoolean.of(false);
        final MutableBoolean isMap = MutableBoolean.of(false);

        final Supplier<Pair<T, A>> supplier = new Supplier<Pair<T, A>>() {
            @SuppressWarnings("rawtypes")
            @Override
            public Pair<T, A> get() {
                final A container = downstreamSupplier.get();

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

        final BiConsumer<Pair<T, A>, T> accumulator = new BiConsumer<Pair<T, A>, T>() {
            @SuppressWarnings("rawtypes")
            @Override
            public void accept(Pair<T, A> a, T t) {
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

        final BinaryOperator<Pair<T, A>> combiner = new BinaryOperator<Pair<T, A>>() {
            @Override
            public Pair<T, A> apply(Pair<T, A> a, Pair<T, A> b) {
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
            }
        };

        final Function<Pair<T, A>, D> finisher = new Function<Pair<T, A>, D>() {
            @Override
            public D apply(Pair<T, A> t) {
                return downstream.finisher().apply(t.right);
            }
        };

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    @SuppressWarnings("unchecked")
    static <T> T none() {
        return (T) NONE;
    }

    @SuppressWarnings("rawtypes")
    public static <T extends Comparable, A, D> Collector<T, ?, Pair<Optional<T>, D>> maxAlll(Collector<T, A, D> downstream) {
        return maxAlll(Fn.naturalOrder(), downstream);
    }

    public static <T, A, D> Collector<T, ?, Pair<Optional<T>, D>> maxAlll(final Comparator<? super T> comparator, final Collector<? super T, A, D> downstream) {
        return maxAlll(comparator, downstream, Fn.<Pair<Optional<T>, D>> identity());
    }

    public static <T, A, D, R> Collector<T, ?, R> maxAlll(final Comparator<? super T> comparator, final Collector<? super T, A, D> downstream,
            final Function<Pair<Optional<T>, D>, R> finisher) {
        final Supplier<A> downstreamSupplier = downstream.supplier();
        final BiConsumer<A, ? super T> downstreamAccumulator = downstream.accumulator();
        final BinaryOperator<A> downstreamCombiner = downstream.combiner();
        final MutableBoolean isCollection = MutableBoolean.of(false);
        final MutableBoolean isMap = MutableBoolean.of(false);

        final Supplier<Pair<T, A>> supplier = new Supplier<Pair<T, A>>() {
            @SuppressWarnings("rawtypes")
            @Override
            public Pair<T, A> get() {
                final A container = downstreamSupplier.get();

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

        final BiConsumer<Pair<T, A>, T> accumulator = new BiConsumer<Pair<T, A>, T>() {
            @SuppressWarnings("rawtypes")
            @Override
            public void accept(Pair<T, A> a, T t) {
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

        final BinaryOperator<Pair<T, A>> combiner = new BinaryOperator<Pair<T, A>>() {
            @Override
            public Pair<T, A> apply(Pair<T, A> a, Pair<T, A> b) {
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
            }
        };

        final Function<Pair<T, A>, R> finalFinisher = new Function<Pair<T, A>, R>() {
            @Override
            public R apply(Pair<T, A> a) {
                @SuppressWarnings("rawtypes")
                final Pair<Optional<T>, D> result = (Pair) a;
                result.setLeft(a.left == NONE ? Optional.<T> empty() : Optional.of(a.left));
                result.setRight(downstream.finisher().apply(a.right));

                return finisher.apply(result);
            }
        };

        return new CollectorImpl<>(supplier, accumulator, combiner, finalFinisher, CH_UNORDERED_NOID);
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
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> Collector<T, ?, List<T>> minAll() {
        return minAll(Fn.naturalOrder());
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
     * @param comparator
     * @param atMostSize
     * @return
     */
    public static <T> Collector<T, ?, List<T>> minAll(Comparator<? super T> comparator, int atMostSize) {
        return maxAll(Fn.reversedOrder(comparator), atMostSize);
    }

    /**
     * Use occurrences to save the count of largest objects if {@code areAllSmallestSame = true}(e.g. {@code Number/String/...}) and return a list by repeat the smallest object {@code n} times.
     *  
     * @param areAllSmallestSame
     * @return
     * @see Collectors#maxAll(Comparator, int, boolean)
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> Collector<T, ?, List<T>> minAll(final boolean areAllSmallestSame) {
        return minAll(Integer.MAX_VALUE, areAllSmallestSame);
    }

    /**
     * Use occurrences to save the count of largest objects if {@code areAllSmallestSame = true}(e.g. {@code Number/String/...}) and return a list by repeat the smallest object {@code n} times.
     *  
     * @param atMostSize
     * @param areAllSmallestSame
     * @return
     * @see Collectors#maxAll(Comparator, int, boolean)
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> Collector<T, ?, List<T>> minAll(final int atMostSize, final boolean areAllSmallestSame) {
        return minAll(Fn.naturalOrder(), atMostSize, areAllSmallestSame);
    }

    /**
     * Use occurrences to save the count of largest objects if {@code areAllSmallestSame = true}(e.g. {@code Number/String/...}) and return a list by repeat the smallest object {@code n} times.
     *  
     * @param comparator
     * @param atMostSize
     * @param areAllSmallestSame
     * @return
     * @see Collectors#maxAll(Comparator, int, boolean)
     */
    public static <T> Collector<T, ?, List<T>> minAll(final Comparator<? super T> comparator, final int atMostSize, final boolean areAllSmallestSame) {
        return maxAll(Fn.reversedOrder(comparator), atMostSize, areAllSmallestSame);
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
     * @param <D> the result type of the downstream reduction
     * @param downstream a {@code Collector} implementing the downstream
     *        reduction
     * @return a {@code Collector} which finds all the minimal elements.
     * @see #minAll(Comparator, Collector)
     * @see #minAll(Comparator)
     * @see #minAll()
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable, A, D> Collector<T, ?, D> minAll(Collector<T, A, D> downstream) {
        return minAll(Fn.naturalOrder(), downstream);
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
     * @param <D> the result type of the downstream reduction
     * @param comparator a {@code Comparator} to compare the elements
     * @param downstream a {@code Collector} implementing the downstream
     *        reduction
     * @return a {@code Collector} which finds all the minimal elements.
     * @see #minAll(Comparator)
     * @see #minAll(Collector)
     * @see #minAll()
     */
    public static <T, A, D> Collector<T, ?, D> minAll(Comparator<? super T> comparator, Collector<T, A, D> downstream) {
        return maxAll(Fn.reversedOrder(comparator), downstream);
    }

    @SuppressWarnings("rawtypes")
    public static <T extends Comparable, A, D> Collector<T, ?, Pair<Optional<T>, D>> minAlll(Collector<T, A, D> downstream) {
        return minAlll(Fn.naturalOrder(), downstream);
    }

    public static <T, A, D> Collector<T, ?, Pair<Optional<T>, D>> minAlll(final Comparator<? super T> comparator, final Collector<? super T, A, D> downstream) {
        return minAlll(comparator, downstream, Fn.<Pair<Optional<T>, D>> identity());
    }

    public static <T, A, D, R> Collector<T, ?, R> minAlll(final Comparator<? super T> comparator, final Collector<? super T, A, D> downstream,
            final Function<Pair<Optional<T>, D>, R> finisher) {
        return maxAlll(Fn.reversedOrder(comparator), downstream, finisher);
    }

    public static <T> Collector<T, ?, Integer> summingInt(final ToIntFunction<? super T> mapper) {
        final Supplier<long[]> supplier = SummingInt_Supplier;

        final BiConsumer<long[], T> accumulator = new BiConsumer<long[], T>() {
            @Override
            public void accept(long[] a, T t) {
                a[0] += mapper.applyAsInt(t);
            }
        };

        final BinaryOperator<long[]> combiner = SummingInt_Combiner;
        final Function<long[], Integer> finisher = SummingInt_Finisher;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    public static <T> Collector<T, ?, OptionalInt> summingIntt(final ToIntFunction<? super T> mapper) {
        final Supplier<long[]> supplier = SummingInt_Supplier_2;

        final BiConsumer<long[], T> accumulator = new BiConsumer<long[], T>() {
            @Override
            public void accept(long[] a, T t) {
                a[0] += mapper.applyAsInt(t);
            }
        };

        final BinaryOperator<long[]> combiner = SummingInt_Combiner_2;
        final Function<long[], OptionalInt> finisher = SummingInt_Finisher_2;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    public static <T> Collector<T, ?, Long> summingLong(final ToLongFunction<? super T> mapper) {
        final Supplier<long[]> supplier = SummingLong_Supplier;

        final BiConsumer<long[], T> accumulator = new BiConsumer<long[], T>() {
            @Override
            public void accept(long[] a, T t) {
                a[0] += mapper.applyAsLong(t);
            }
        };

        final BinaryOperator<long[]> combiner = SummingLong_Combiner;
        final Function<long[], Long> finisher = SummingLong_Finisher;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    public static <T> Collector<T, ?, OptionalLong> summingLongg(final ToLongFunction<? super T> mapper) {
        final Supplier<long[]> supplier = SummingLong_Supplier_2;

        final BiConsumer<long[], T> accumulator = new BiConsumer<long[], T>() {
            @Override
            public void accept(long[] a, T t) {
                a[0] += mapper.applyAsLong(t);
            }
        };

        final BinaryOperator<long[]> combiner = SummingLong_Combiner_2;
        final Function<long[], OptionalLong> finisher = SummingLong_Finisher_2;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    public static <T> Collector<T, ?, Double> summingDouble(final ToDoubleFunction<? super T> mapper) {
        final Supplier<KahanSummation> supplier = SummingDouble_Supplier;

        final BiConsumer<KahanSummation, T> accumulator = new BiConsumer<KahanSummation, T>() {
            @Override
            public void accept(KahanSummation a, T t) {
                a.add(mapper.applyAsDouble(t));
            }
        };

        final BinaryOperator<KahanSummation> combiner = SummingDouble_Combiner;
        final Function<KahanSummation, Double> finisher = SummingDouble_Finisher;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    public static <T> Collector<T, ?, OptionalDouble> summingDoubble(final ToDoubleFunction<? super T> mapper) {
        final Supplier<KahanSummation> supplier = SummingDouble_Supplier_2;

        final BiConsumer<KahanSummation, T> accumulator = new BiConsumer<KahanSummation, T>() {
            @Override
            public void accept(KahanSummation a, T t) {
                a.add(mapper.applyAsDouble(t));
            }
        };

        final BinaryOperator<KahanSummation> combiner = SummingDouble_Combiner_2;
        final Function<KahanSummation, OptionalDouble> finisher = SummingDouble_Finisher_2;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    public static <T> Collector<T, ?, BigInteger> summingBigInteger(final Function<? super T, BigInteger> mapper) {
        final Supplier<BigInteger[]> supplier = SummingBigInteger_Supplier;

        final BiConsumer<BigInteger[], T> accumulator = new BiConsumer<BigInteger[], T>() {
            @Override
            public void accept(BigInteger[] a, T t) {
                a[0] = a[0].add(mapper.apply(t));
            }
        };

        final BinaryOperator<BigInteger[]> combiner = SummingBigInteger_Combiner;
        final Function<BigInteger[], BigInteger> finisher = SummingBigInteger_Finisher;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    public static <T> Collector<T, ?, BigDecimal> summingBigDecimal(final Function<? super T, BigDecimal> mapper) {
        final Supplier<BigDecimal[]> supplier = SummingBigDecimal_Supplier;

        final BiConsumer<BigDecimal[], T> accumulator = new BiConsumer<BigDecimal[], T>() {
            @Override
            public void accept(BigDecimal[] a, T t) {
                a[0] = a[0].add(mapper.apply(t));
            }
        };

        final BinaryOperator<BigDecimal[]> combiner = SummingBigDecimal_Combiner;
        final Function<BigDecimal[], BigDecimal> finisher = SummingBigDecimal_Finisher;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    public static <T> Collector<T, ?, Double> averagingInt(final ToIntFunction<? super T> mapper) {
        final Supplier<long[]> supplier = AveragingInt_Supplier;

        final BiConsumer<long[], T> accumulator = new BiConsumer<long[], T>() {
            @Override
            public void accept(long[] a, T t) {
                a[0] += mapper.applyAsInt(t);
                a[1]++;
            }
        };

        final BinaryOperator<long[]> combiner = AveragingInt_Combiner;
        final Function<long[], Double> finisher = AveragingInt_Finisher;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    public static <T> Collector<T, ?, OptionalDouble> averagingIntt(final ToIntFunction<? super T> mapper) {
        final Supplier<long[]> supplier = AveragingInt_Supplier;

        final BiConsumer<long[], T> accumulator = new BiConsumer<long[], T>() {
            @Override
            public void accept(long[] a, T t) {
                a[0] += mapper.applyAsInt(t);
                a[1]++;
            }
        };

        final BinaryOperator<long[]> combiner = AveragingInt_Combiner;
        final Function<long[], OptionalDouble> finisher = AveragingInt_Finisher_2;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    public static <T> Collector<T, ?, Double> averagingLong(final ToLongFunction<? super T> mapper) {
        final Supplier<long[]> supplier = AveragingLong_Supplier;

        final BiConsumer<long[], T> accumulator = new BiConsumer<long[], T>() {
            @Override
            public void accept(long[] a, T t) {
                a[0] += mapper.applyAsLong(t);
                a[1]++;
            }
        };

        final BinaryOperator<long[]> combiner = AveragingLong_Combiner;
        final Function<long[], Double> finisher = AveragingLong_Finisher;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    public static <T> Collector<T, ?, OptionalDouble> averagingLongg(final ToLongFunction<? super T> mapper) {
        final Supplier<long[]> supplier = AveragingLong_Supplier;

        final BiConsumer<long[], T> accumulator = new BiConsumer<long[], T>() {
            @Override
            public void accept(long[] a, T t) {
                a[0] += mapper.applyAsLong(t);
                a[1]++;
            }
        };

        final BinaryOperator<long[]> combiner = AveragingLong_Combiner;
        final Function<long[], OptionalDouble> finisher = AveragingLong_Finisher_2;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    public static <T> Collector<T, ?, Double> averagingDouble(final ToDoubleFunction<? super T> mapper) {
        final Supplier<KahanSummation> supplier = AveragingDouble_Supplier;

        final BiConsumer<KahanSummation, T> accumulator = new BiConsumer<KahanSummation, T>() {
            @Override
            public void accept(KahanSummation a, T t) {
                a.add(mapper.applyAsDouble(t));
            }
        };

        final BinaryOperator<KahanSummation> combiner = AveragingDouble_Combiner;
        final Function<KahanSummation, Double> finisher = AveragingDouble_Finisher;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    public static <T> Collector<T, ?, OptionalDouble> averagingDoubble(final ToDoubleFunction<? super T> mapper) {
        final Supplier<KahanSummation> supplier = AveragingDouble_Supplier;

        final BiConsumer<KahanSummation, T> accumulator = new BiConsumer<KahanSummation, T>() {
            @Override
            public void accept(KahanSummation a, T t) {
                a.add(mapper.applyAsDouble(t));
            }
        };

        final BinaryOperator<KahanSummation> combiner = AveragingDouble_Combiner;
        final Function<KahanSummation, OptionalDouble> finisher = AveragingDouble_Finisher_2;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    public static <T> Collector<T, ?, BigDecimal> averagingBigInteger(final Function<? super T, BigInteger> mapper) {
        final Supplier<Pair<BigInteger, MutableLong>> supplier = AveragingBigInteger_Supplier;

        final BiConsumer<Pair<BigInteger, MutableLong>, T> accumulator = new BiConsumer<Pair<BigInteger, MutableLong>, T>() {
            @Override
            public void accept(Pair<BigInteger, MutableLong> a, T t) {
                a.setLeft(a.left.add(mapper.apply(t)));
                a.right.increment();
            }
        };

        final BinaryOperator<Pair<BigInteger, MutableLong>> combiner = AveragingBigInteger_Combiner;
        final Function<Pair<BigInteger, MutableLong>, BigDecimal> finisher = AveragingBigInteger_Finisher;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    public static <T> Collector<T, ?, BigDecimal> averagingBigDecimal(final Function<? super T, BigDecimal> mapper) {
        final Supplier<Pair<BigDecimal, MutableLong>> supplier = AveragingBigDecimal_Supplier;

        final BiConsumer<Pair<BigDecimal, MutableLong>, T> accumulator = new BiConsumer<Pair<BigDecimal, MutableLong>, T>() {
            @Override
            public void accept(Pair<BigDecimal, MutableLong> a, T t) {
                a.setLeft(a.left.add(mapper.apply(t)));
                a.right.increment();
            }
        };

        final BinaryOperator<Pair<BigDecimal, MutableLong>> combiner = AveragingBigDecimal_Combiner;
        final Function<Pair<BigDecimal, MutableLong>, BigDecimal> finisher = AveragingBigDecimal_Finisher;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    public static <T> Collector<T, ?, CharSummaryStatistics> summarizingChar(final ToCharFunction<? super T> mapper) {
        final Supplier<CharSummaryStatistics> supplier = SummarizingChar_Supplier;

        final BiConsumer<CharSummaryStatistics, T> accumulator = new BiConsumer<CharSummaryStatistics, T>() {
            @Override
            public void accept(CharSummaryStatistics a, T t) {
                a.accept(mapper.applyAsChar(t));
            }
        };

        final BinaryOperator<CharSummaryStatistics> combiner = SummarizingChar_Combiner;

        return new CollectorImpl<T, CharSummaryStatistics, CharSummaryStatistics>(supplier, accumulator, combiner, CH_UNORDERED_ID);
    }

    public static <T> Collector<T, ?, ByteSummaryStatistics> summarizingByte(final ToByteFunction<? super T> mapper) {
        final Supplier<ByteSummaryStatistics> supplier = SummarizingByte_Supplier;

        final BiConsumer<ByteSummaryStatistics, T> accumulator = new BiConsumer<ByteSummaryStatistics, T>() {
            @Override
            public void accept(ByteSummaryStatistics a, T t) {
                a.accept(mapper.applyAsByte(t));
            }
        };

        final BinaryOperator<ByteSummaryStatistics> combiner = SummarizingByte_Combiner;

        return new CollectorImpl<T, ByteSummaryStatistics, ByteSummaryStatistics>(supplier, accumulator, combiner, CH_UNORDERED_ID);
    }

    public static <T> Collector<T, ?, ShortSummaryStatistics> summarizingShort(final ToShortFunction<? super T> mapper) {
        final Supplier<ShortSummaryStatistics> supplier = SummarizingShort_Supplier;

        final BiConsumer<ShortSummaryStatistics, T> accumulator = new BiConsumer<ShortSummaryStatistics, T>() {
            @Override
            public void accept(ShortSummaryStatistics a, T t) {
                a.accept(mapper.applyAsShort(t));
            }
        };

        final BinaryOperator<ShortSummaryStatistics> combiner = SummarizingShort_Combiner;

        return new CollectorImpl<T, ShortSummaryStatistics, ShortSummaryStatistics>(supplier, accumulator, combiner, CH_UNORDERED_ID);
    }

    public static <T> Collector<T, ?, IntSummaryStatistics> summarizingInt(final ToIntFunction<? super T> mapper) {
        final Supplier<IntSummaryStatistics> supplier = SummarizingInt_Supplier;

        final BiConsumer<IntSummaryStatistics, T> accumulator = new BiConsumer<IntSummaryStatistics, T>() {
            @Override
            public void accept(IntSummaryStatistics a, T t) {
                a.accept(mapper.applyAsInt(t));
            }
        };

        final BinaryOperator<IntSummaryStatistics> combiner = SummarizingInt_Combiner;

        return new CollectorImpl<T, IntSummaryStatistics, IntSummaryStatistics>(supplier, accumulator, combiner, CH_UNORDERED_ID);
    }

    public static <T> Collector<T, ?, LongSummaryStatistics> summarizingLong(final ToLongFunction<? super T> mapper) {
        final Supplier<LongSummaryStatistics> supplier = SummarizingLong_Supplier;

        final BiConsumer<LongSummaryStatistics, T> accumulator = new BiConsumer<LongSummaryStatistics, T>() {
            @Override
            public void accept(LongSummaryStatistics a, T t) {
                a.accept(mapper.applyAsLong(t));
            }
        };

        final BinaryOperator<LongSummaryStatistics> combiner = SummarizingLong_Combiner;

        return new CollectorImpl<T, LongSummaryStatistics, LongSummaryStatistics>(supplier, accumulator, combiner, CH_UNORDERED_ID);
    }

    public static <T> Collector<T, ?, FloatSummaryStatistics> summarizingFloat(final ToFloatFunction<? super T> mapper) {
        final Supplier<FloatSummaryStatistics> supplier = SummarizingFloat_Supplier;

        final BiConsumer<FloatSummaryStatistics, T> accumulator = new BiConsumer<FloatSummaryStatistics, T>() {
            @Override
            public void accept(FloatSummaryStatistics a, T t) {
                a.accept(mapper.applyAsFloat(t));
            }
        };

        final BinaryOperator<FloatSummaryStatistics> combiner = SummarizingFloat_Combiner;

        return new CollectorImpl<T, FloatSummaryStatistics, FloatSummaryStatistics>(supplier, accumulator, combiner, CH_UNORDERED_ID);
    }

    public static <T> Collector<T, ?, DoubleSummaryStatistics> summarizingDouble(final ToDoubleFunction<? super T> mapper) {
        final Supplier<DoubleSummaryStatistics> supplier = SummarizingDouble_Supplier;

        final BiConsumer<DoubleSummaryStatistics, T> accumulator = new BiConsumer<DoubleSummaryStatistics, T>() {
            @Override
            public void accept(DoubleSummaryStatistics a, T t) {
                a.accept(mapper.applyAsDouble(t));
            }
        };

        final BinaryOperator<DoubleSummaryStatistics> combiner = SummarizingDouble_Combiner;

        return new CollectorImpl<T, DoubleSummaryStatistics, DoubleSummaryStatistics>(supplier, accumulator, combiner, CH_UNORDERED_ID);
    }

    public static <T> Collector<T, ?, T> reducing(final T identity, final BinaryOperator<T> op) {
        final BiConsumer<Holder<T>, T> accumulator = new BiConsumer<Holder<T>, T>() {
            @Override
            public void accept(Holder<T> a, T t) {
                a.setValue(op.apply(a.value(), t));
            }
        };

        final BinaryOperator<Holder<T>> combiner = new BinaryOperator<Holder<T>>() {
            @Override
            public Holder<T> apply(Holder<T> a, Holder<T> b) {
                a.setValue(op.apply(a.value(), b.value()));
                return a;
            }
        };

        @SuppressWarnings("rawtypes")
        final Function<Holder<T>, T> finisher = (Function) Reducing_Finisher_0;

        return new CollectorImpl<>(holderSupplier(identity), accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    @SuppressWarnings("rawtypes")
    public static <T> Collector<T, ?, Optional<T>> reducing(final BinaryOperator<T> op) {
        final Supplier<OptHolder<T>> supplier = new Supplier<OptHolder<T>>() {
            @Override
            public OptHolder<T> get() {
                return new OptHolder<T>(op);
            }
        };

        final BiConsumer<OptHolder<T>, T> accumulator = (BiConsumer) Reducing_Accumulator;
        final BinaryOperator<OptHolder<T>> combiner = (BinaryOperator) Reducing_Combiner;
        final Function<OptHolder<T>, Optional<T>> finisher = (Function) Reducing_Finisher;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    @SuppressWarnings("rawtypes")
    public static <T> Collector<T, ?, T> reducingOrGet(final BinaryOperator<T> op, final Supplier<? extends T> other) {
        final Supplier<OptHolder<T>> supplier = new Supplier<OptHolder<T>>() {
            @Override
            public OptHolder<T> get() {
                return new OptHolder<T>(op);
            }
        };

        final BiConsumer<OptHolder<T>, T> accumulator = (BiConsumer) Reducing_Accumulator;
        final BinaryOperator<OptHolder<T>> combiner = (BinaryOperator) Reducing_Combiner;

        final Function<OptHolder<T>, T> finisher = new Function<OptHolder<T>, T>() {
            @Override
            public T apply(OptHolder<T> a) {
                return a.present ? a.value : other.get();
            }
        };

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    @SuppressWarnings("rawtypes")
    public static <T, X extends RuntimeException> Collector<T, ?, T> reducingOrThrow(final BinaryOperator<T> op,
            final Supplier<? extends X> exceptionSupplier) {
        final Supplier<OptHolder<T>> supplier = new Supplier<OptHolder<T>>() {
            @Override
            public OptHolder<T> get() {
                return new OptHolder<T>(op);
            }
        };

        final BiConsumer<OptHolder<T>, T> accumulator = (BiConsumer) Reducing_Accumulator;
        final BinaryOperator<OptHolder<T>> combiner = (BinaryOperator) Reducing_Combiner;

        final Function<OptHolder<T>, T> finisher = new Function<OptHolder<T>, T>() {
            @Override
            public T apply(OptHolder<T> a) {
                if (a.present) {
                    return a.value;
                } else {
                    throw exceptionSupplier.get();
                }
            }
        };

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    public static <T, U> Collector<T, ?, U> reducing(final U identity, final Function<? super T, ? extends U> mapper, final BinaryOperator<U> op) {
        final BiConsumer<Holder<U>, T> accumulator = new BiConsumer<Holder<U>, T>() {
            @Override
            public void accept(Holder<U> a, T t) {
                a.setValue(op.apply(a.value(), mapper.apply(t)));
            }
        };

        final BinaryOperator<Holder<U>> combiner = new BinaryOperator<Holder<U>>() {
            @Override
            public Holder<U> apply(Holder<U> a, Holder<U> b) {
                a.setValue(op.apply(a.value(), b.value()));

                return a;
            }
        };

        @SuppressWarnings("rawtypes")
        final Function<Holder<U>, U> finisher = (Function) Reducing_Finisher_0;

        return new CollectorImpl<>(holderSupplier(identity), accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    @SuppressWarnings("rawtypes")
    public static <T, U> Collector<T, ?, Optional<U>> reducing(final Function<? super T, ? extends U> mapper, final BinaryOperator<U> op) {
        final Supplier<MappingOptHolder<T, U>> supplier = new Supplier<MappingOptHolder<T, U>>() {
            @Override
            public MappingOptHolder<T, U> get() {
                return new MappingOptHolder<T, U>(mapper, op);
            }
        };

        final BiConsumer<MappingOptHolder<T, U>, T> accumulator = (BiConsumer) Reducing_Accumulator_2;
        final BinaryOperator<MappingOptHolder<T, U>> combiner = (BinaryOperator) Reducing_Combiner_2;
        final Function<MappingOptHolder<T, U>, Optional<U>> finisher = (Function) Reducing_Finisher_2;

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    @SuppressWarnings("unchecked")
    private static <T> Supplier<Holder<T>> holderSupplier(final T identity) {
        return new Supplier<Holder<T>>() {
            @Override
            public Holder<T> get() {
                return Holder.of(identity);
            }
        };
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

    @SuppressWarnings("rawtypes")
    public static <T, U> Collector<T, ?, U> reducingOrGet(final Function<? super T, ? extends U> mapper, final BinaryOperator<U> op,
            final Supplier<? extends U> other) {
        final Supplier<MappingOptHolder<T, U>> supplier = new Supplier<MappingOptHolder<T, U>>() {
            @Override
            public MappingOptHolder<T, U> get() {
                return new MappingOptHolder<T, U>(mapper, op);
            }
        };

        final BiConsumer<MappingOptHolder<T, U>, T> accumulator = (BiConsumer) Reducing_Accumulator_2;
        final BinaryOperator<MappingOptHolder<T, U>> combiner = (BinaryOperator) Reducing_Combiner_2;
        final Function<MappingOptHolder<T, U>, U> finisher = new Function<MappingOptHolder<T, U>, U>() {
            @Override
            public U apply(MappingOptHolder<T, U> a) {
                return a.present ? a.value : other.get();
            }
        };

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    @SuppressWarnings("rawtypes")
    public static <T, U, X extends RuntimeException> Collector<T, ?, U> reducingOrThrow(final Function<? super T, ? extends U> mapper,
            final BinaryOperator<U> op, final Supplier<? extends X> exceptionSupplier) {
        final Supplier<MappingOptHolder<T, U>> supplier = new Supplier<MappingOptHolder<T, U>>() {
            @Override
            public MappingOptHolder<T, U> get() {
                return new MappingOptHolder<T, U>(mapper, op);
            }
        };

        final BiConsumer<MappingOptHolder<T, U>, T> accumulator = (BiConsumer) Reducing_Accumulator_2;
        final BinaryOperator<MappingOptHolder<T, U>> combiner = (BinaryOperator) Reducing_Combiner_2;
        final Function<MappingOptHolder<T, U>, U> finisher = new Function<MappingOptHolder<T, U>, U>() {
            @Override
            public U apply(MappingOptHolder<T, U> a) {
                if (a.present) {
                    return a.value;
                } else {
                    throw exceptionSupplier.get();
                }
            }
        };

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
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
        final Supplier<Pair<CharSequence, Integer>> supplier = new Supplier<Pair<CharSequence, Integer>>() {
            @Override
            public Pair<CharSequence, Integer> get() {
                return Pair.of(null, -1);
            }
        };

        final BiConsumer<Pair<CharSequence, Integer>, CharSequence> accumulator = new BiConsumer<Pair<CharSequence, Integer>, CharSequence>() {
            @Override
            public void accept(Pair<CharSequence, Integer> a, CharSequence t) {
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
            }
        };

        final BinaryOperator<Pair<CharSequence, Integer>> combiner = new BinaryOperator<Pair<CharSequence, Integer>>() {
            @Override
            public Pair<CharSequence, Integer> apply(Pair<CharSequence, Integer> a, Pair<CharSequence, Integer> b) {
                if (a.right == -1) {
                    return b;
                }

                if (b.right != -1) {
                    accumulator.accept(a, b.left.subSequence(0, b.right));
                }

                return a;
            }
        };

        final Function<Pair<CharSequence, Integer>, String> finisher = new Function<Pair<CharSequence, Integer>, String>() {
            @Override
            public String apply(Pair<CharSequence, Integer> a) {
                return a.left == null ? "" : a.left.subSequence(0, a.right).toString();
            }
        };

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
        final Supplier<Pair<CharSequence, Integer>> supplier = new Supplier<Pair<CharSequence, Integer>>() {
            @Override
            public Pair<CharSequence, Integer> get() {
                return Pair.of(null, -1);
            }
        };

        final BiConsumer<Pair<CharSequence, Integer>, CharSequence> accumulator = new BiConsumer<Pair<CharSequence, Integer>, CharSequence>() {
            @Override
            public void accept(Pair<CharSequence, Integer> a, CharSequence t) {
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
            }
        };

        final BinaryOperator<Pair<CharSequence, Integer>> combiner = new BinaryOperator<Pair<CharSequence, Integer>>() {
            @Override
            public Pair<CharSequence, Integer> apply(Pair<CharSequence, Integer> a, Pair<CharSequence, Integer> b) {
                if (a.right == -1) {
                    return b;
                }

                if (b.right != -1) {
                    accumulator.accept(a, b.left.subSequence(b.left.length() - b.right, b.left.length()));
                }

                return a;
            }
        };

        final Function<Pair<CharSequence, Integer>, String> finisher = new Function<Pair<CharSequence, Integer>, String>() {
            @Override
            public String apply(Pair<CharSequence, Integer> a) {
                return a.left == null ? "" : a.left.subSequence(a.left.length() - a.right, a.left.length()).toString();
            }
        };

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    public static <T, K> Collector<T, ?, Map<K, List<T>>> groupingBy(Function<? super T, ? extends K> keyMapper) {
        final Collector<? super T, ?, List<T>> downstream = toList();

        return groupingBy(keyMapper, downstream);
    }

    public static <T, K, M extends Map<K, List<T>>> Collector<T, ?, M> groupingBy(final Function<? super T, ? extends K> keyMapper,
            final Supplier<? extends M> mapFactory) {
        final Collector<? super T, ?, List<T>> downstream = toList();

        return groupingBy(keyMapper, downstream, mapFactory);
    }

    public static <T, K, A, D> Collector<T, ?, Map<K, D>> groupingBy(final Function<? super T, ? extends K> keyMapper,
            final Collector<? super T, A, D> downstream) {
        final Supplier<Map<K, D>> mapFactory = Suppliers.ofMap();

        return groupingBy(keyMapper, downstream, mapFactory);
    }

    public static <T, K, A, D, M extends Map<K, D>> Collector<T, ?, M> groupingBy(final Function<? super T, ? extends K> keyMapper,
            final Collector<? super T, A, D> downstream, final Supplier<? extends M> mapFactory) {
        final Supplier<A> downstreamSupplier = downstream.supplier();
        final BiConsumer<A, ? super T> downstreamAccumulator = downstream.accumulator();

        final Function<K, A> mappingFunction = new Function<K, A>() {
            @Override
            public A apply(K k) {
                return downstreamSupplier.get();
            }
        };

        final BiConsumer<Map<K, A>, T> accumulator = new BiConsumer<Map<K, A>, T>() {
            @Override
            public void accept(Map<K, A> m, T t) {
                K key = N.checkArgNotNull(keyMapper.apply(t), "element cannot be mapped to a null key");
                A container = computeIfAbsent(m, key, mappingFunction);
                downstreamAccumulator.accept(container, t);
            }
        };

        final BinaryOperator<Map<K, A>> combiner = Collectors.<K, A, Map<K, A>> mapMerger(downstream.combiner());
        @SuppressWarnings("unchecked")
        final Supplier<Map<K, A>> mangledFactory = (Supplier<Map<K, A>>) mapFactory;

        @SuppressWarnings("unchecked")
        final Function<A, A> downstreamFinisher = (Function<A, A>) downstream.finisher();

        final BiFunction<? super K, ? super A, ? extends A> function = new BiFunction<K, A, A>() {
            @Override
            public A apply(K k, A v) {
                return downstreamFinisher.apply(v);
            }
        };

        final Function<Map<K, A>, M> finisher = new Function<Map<K, A>, M>() {
            @Override
            public M apply(Map<K, A> intermediate) {
                replaceAll(intermediate, function);
                @SuppressWarnings("unchecked")
                M castResult = (M) intermediate;
                return castResult;
            }
        };

        return new CollectorImpl<>(mangledFactory, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    public static <T, K> Collector<T, ?, ConcurrentMap<K, List<T>>> groupingByConcurrent(Function<? super T, ? extends K> keyMapper) {
        final Collector<? super T, ?, List<T>> downstream = toList();

        return groupingByConcurrent(keyMapper, downstream);
    }

    public static <T, K, M extends ConcurrentMap<K, List<T>>> Collector<T, ?, M> groupingByConcurrent(final Function<? super T, ? extends K> keyMapper,
            final Supplier<? extends M> mapFactory) {
        final Collector<? super T, ?, List<T>> downstream = toList();

        return groupingByConcurrent(keyMapper, downstream, mapFactory);
    }

    public static <T, K, A, D> Collector<T, ?, ConcurrentMap<K, D>> groupingByConcurrent(Function<? super T, ? extends K> keyMapper,
            Collector<? super T, A, D> downstream) {
        final Supplier<ConcurrentMap<K, D>> mapFactory = Suppliers.ofConcurrentMap();

        return groupingByConcurrent(keyMapper, downstream, mapFactory);
    }

    public static <T, K, A, D, M extends ConcurrentMap<K, D>> Collector<T, ?, M> groupingByConcurrent(final Function<? super T, ? extends K> keyMapper,
            Collector<? super T, A, D> downstream, final Supplier<? extends M> mapFactory) {
        final Supplier<A> downstreamSupplier = downstream.supplier();
        final BiConsumer<A, ? super T> downstreamAccumulator = downstream.accumulator();

        final Function<K, A> mappingFunction = new Function<K, A>() {
            @Override
            public A apply(K k) {
                return downstreamSupplier.get();
            }
        };

        final BiConsumer<ConcurrentMap<K, A>, T> accumulator = new BiConsumer<ConcurrentMap<K, A>, T>() {
            @Override
            public void accept(ConcurrentMap<K, A> m, T t) {
                K key = N.checkArgNotNull(keyMapper.apply(t), "element cannot be mapped to a null key");
                A container = computeIfAbsent(m, key, mappingFunction);
                downstreamAccumulator.accept(container, t);
            }
        };

        final BinaryOperator<ConcurrentMap<K, A>> combiner = Collectors.<K, A, ConcurrentMap<K, A>> mapMerger(downstream.combiner());
        @SuppressWarnings("unchecked")
        final Supplier<ConcurrentMap<K, A>> mangledFactory = (Supplier<ConcurrentMap<K, A>>) mapFactory;

        if (downstream.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
            return new CollectorImpl<>(mangledFactory, accumulator, combiner, CH_UNORDERED_ID);
        } else {
            @SuppressWarnings("unchecked")
            final Function<A, A> downstreamFinisher = (Function<A, A>) downstream.finisher();

            final BiFunction<? super K, ? super A, ? extends A> function = new BiFunction<K, A, A>() {
                @Override
                public A apply(K k, A v) {
                    return downstreamFinisher.apply(v);
                }
            };

            final Function<ConcurrentMap<K, A>, M> finisher = new Function<ConcurrentMap<K, A>, M>() {
                @Override
                public M apply(ConcurrentMap<K, A> intermediate) {
                    replaceAll(intermediate, function);
                    @SuppressWarnings("unchecked")
                    M castResult = (M) intermediate;
                    return castResult;
                }
            };

            return new CollectorImpl<>(mangledFactory, accumulator, combiner, finisher, CH_UNORDERED_NOID);
        }
    }

    public static <T> Collector<T, ?, Map<Boolean, List<T>>> partitioningBy(Predicate<? super T> predicate) {
        final Collector<? super T, ?, List<T>> downstream = toList();

        return partitioningBy(predicate, downstream);
    }

    public static <T, D, A> Collector<T, ?, Map<Boolean, D>> partitioningBy(final Predicate<? super T> predicate, final Collector<? super T, A, D> downstream) {
        final Supplier<Map<Boolean, A>> supplier = new Supplier<Map<Boolean, A>>() {
            @Override
            public Map<Boolean, A> get() {
                final Map<Boolean, A> map = new HashMap<>(2);
                map.put(true, downstream.supplier().get());
                map.put(false, downstream.supplier().get());
                return map;
            }
        };

        final BiConsumer<A, ? super T> downstreamAccumulator = downstream.accumulator();
        final BiConsumer<Map<Boolean, A>, T> accumulator = new BiConsumer<Map<Boolean, A>, T>() {
            @Override
            public void accept(Map<Boolean, A> a, T t) {
                downstreamAccumulator.accept(predicate.test(t) ? a.get(Boolean.TRUE) : a.get(Boolean.FALSE), t);
            }
        };

        final BinaryOperator<A> op = downstream.combiner();
        final BinaryOperator<Map<Boolean, A>> combiner = new BinaryOperator<Map<Boolean, A>>() {
            @Override
            public Map<Boolean, A> apply(Map<Boolean, A> a, Map<Boolean, A> b) {
                a.put(Boolean.TRUE, op.apply(a.get(Boolean.TRUE), b.get(Boolean.TRUE)));
                a.put(Boolean.FALSE, op.apply(a.get(Boolean.FALSE), b.get(Boolean.FALSE)));
                return a;
            }
        };

        final Function<Map<Boolean, A>, Map<Boolean, D>> finisher = new Function<Map<Boolean, A>, Map<Boolean, D>>() {
            @Override
            public Map<Boolean, D> apply(Map<Boolean, A> a) {
                @SuppressWarnings("rawtypes")
                final Map<Boolean, D> result = (Map) a;

                result.put(Boolean.TRUE, downstream.finisher().apply((a.get(Boolean.TRUE))));
                result.put(Boolean.FALSE, downstream.finisher().apply((a.get(Boolean.FALSE))));

                return result;
            }
        };

        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, CH_UNORDERED_NOID);
    }

    public static <K, V> Collector<Map.Entry<K, V>, ?, Map<K, V>> toMap() {
        final Function<Map.Entry<K, V>, ? extends K> keyMapper = Fn.<K, V> key();
        final Function<Map.Entry<K, V>, ? extends V> valueMapper = Fn.<K, V> value();

        return toMap(keyMapper, valueMapper);
    }

    public static <K, V> Collector<Map.Entry<K, V>, ?, Map<K, V>> toMap(final BinaryOperator<V> mergeFunction) {
        final Function<Map.Entry<K, V>, ? extends K> keyMapper = Fn.<K, V> key();
        final Function<Map.Entry<K, V>, ? extends V> valueMapper = Fn.<K, V> value();

        return toMap(keyMapper, valueMapper, mergeFunction);
    }

    public static <K, V, M extends Map<K, V>> Collector<Map.Entry<K, V>, ?, M> toMap(final Supplier<? extends M> mapFactory) {
        final Function<Map.Entry<K, V>, ? extends K> keyMapper = Fn.<K, V> key();
        final Function<Map.Entry<K, V>, ? extends V> valueMapper = Fn.<K, V> value();

        return toMap(keyMapper, valueMapper, mapFactory);
    }

    public static <K, V, M extends Map<K, V>> Collector<Map.Entry<K, V>, ?, M> toMap(final BinaryOperator<V> mergeFunction,
            final Supplier<? extends M> mapFactory) {
        final Function<Map.Entry<K, V>, ? extends K> keyMapper = Fn.<K, V> key();
        final Function<Map.Entry<K, V>, ? extends V> valueMapper = Fn.<K, V> value();

        return toMap(keyMapper, valueMapper, mergeFunction, mapFactory);
    }

    public static <T, K, V> Collector<T, ?, Map<K, V>> toMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        final BinaryOperator<V> mergeFunction = Fn.throwingMerger();

        return toMap(keyMapper, valueMapper, mergeFunction);
    }

    public static <T, K, V> Collector<T, ?, Map<K, V>> toMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper,
            BinaryOperator<V> mergeFunction) {
        final Supplier<Map<K, V>> mapFactory = Suppliers.<K, V> ofMap();

        return toMap(keyMapper, valueMapper, mergeFunction, mapFactory);
    }

    public static <T, K, V, M extends Map<K, V>> Collector<T, ?, M> toMap(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper, final Supplier<? extends M> mapFactory) {
        final BinaryOperator<V> mergeFunction = Fn.throwingMerger();

        return toMap(keyMapper, valueMapper, mergeFunction, mapFactory);
    }

    public static <T, K, V, M extends Map<K, V>> Collector<T, ?, M> toMap(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper, final BinaryOperator<V> mergeFunction, final Supplier<? extends M> mapFactory) {
        final BiConsumer<M, T> accumulator = new BiConsumer<M, T>() {
            @Override
            public void accept(M map, T element) {
                merge(map, keyMapper.apply(element), valueMapper.apply(element), mergeFunction);
            }
        };

        final BinaryOperator<M> combiner = (BinaryOperator<M>) mapMerger(mergeFunction);

        return new CollectorImpl<>(mapFactory, accumulator, combiner, CH_UNORDERED_ID);
    }

    /**
     * 
     * @param keyMapper
     * @param downstream
     * @return
     * @see Collectors#groupingBy(Function, Collector)
     */
    public static <T, K, A, D> Collector<T, ?, Map<K, D>> toMap(final Function<? super T, ? extends K> keyMapper, final Collector<? super T, A, D> downstream) {
        return groupingBy(keyMapper, downstream);
    }

    /**
     * 
     * @param keyMapper
     * @param downstream
     * @param mapFactory
     * @return
     * @see Collectors#groupingBy(Function, Collector, Supplier)
     */
    public static <T, K, A, D, M extends Map<K, D>> Collector<T, ?, M> toMap(final Function<? super T, ? extends K> keyMapper,
            final Collector<? super T, A, D> downstream, final Supplier<? extends M> mapFactory) {
        return groupingBy(keyMapper, downstream, mapFactory);
    }

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param downstream
     * @return
     * @see Collectors#groupingBy(Function, Collector)
     */
    public static <T, K, V, A, D> Collector<T, ?, Map<K, D>> toMap(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper, final Collector<? super V, A, D> downstream) {
        return groupingBy(keyMapper, mapping(valueMapper, downstream));
    }

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param downstream
     * @param mapFactory
     * @return
     * @see Collectors#groupingBy(Function, Collector, Supplier)
     */
    public static <T, K, V, A, D, M extends Map<K, D>> Collector<T, ?, M> toMap(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper, final Collector<? super V, A, D> downstream, final Supplier<? extends M> mapFactory) {
        return groupingBy(keyMapper, mapping(valueMapper, downstream), mapFactory);
    }

    public static <K, V> Collector<Map.Entry<K, V>, ?, ImmutableMap<K, V>> toImmutableMap() {
        final Collector<Map.Entry<K, V>, ?, Map<K, V>> downstream = toMap();
        @SuppressWarnings("rawtypes")
        final Function<Map<K, V>, ImmutableMap<K, V>> finisher = (Function) ImmutableMap_Finisher;

        return collectingAndThen(downstream, finisher);
    }

    public static <K, V> Collector<Map.Entry<K, V>, ?, ImmutableMap<K, V>> toImmutableMap(final BinaryOperator<V> mergeFunction) {
        final Collector<Map.Entry<K, V>, ?, Map<K, V>> downstream = toMap(mergeFunction);
        @SuppressWarnings("rawtypes")
        final Function<Map<K, V>, ImmutableMap<K, V>> finisher = (Function) ImmutableMap_Finisher;

        return collectingAndThen(downstream, finisher);
    }

    public static <T, K, V> Collector<T, ?, ImmutableMap<K, V>> toImmutableMap(Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper) {
        final Collector<T, ?, Map<K, V>> downstream = toMap(keyMapper, valueMapper);
        @SuppressWarnings("rawtypes")
        final Function<Map<K, V>, ImmutableMap<K, V>> finisher = (Function) ImmutableMap_Finisher;

        return collectingAndThen(downstream, finisher);
    }

    public static <T, K, V> Collector<T, ?, ImmutableMap<K, V>> toImmutableMap(Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper, BinaryOperator<V> mergeFunction) {
        final Collector<T, ?, Map<K, V>> downstream = toMap(keyMapper, valueMapper, mergeFunction);
        @SuppressWarnings("rawtypes")
        final Function<Map<K, V>, ImmutableMap<K, V>> finisher = (Function) ImmutableMap_Finisher;

        return collectingAndThen(downstream, finisher);
    }

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @return
     * @see #toMap(Function, Function)
     */
    public static <T, K, V> Collector<T, ?, LinkedHashMap<K, V>> toLinkedHashMap(Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper) {
        final BinaryOperator<V> mergeFunction = Fn.throwingMerger();

        return toLinkedHashMap(keyMapper, valueMapper, mergeFunction);
    }

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @return
     * @see #toMap(Function, Function, BinaryOperator)
     */
    public static <T, K, V> Collector<T, ?, LinkedHashMap<K, V>> toLinkedHashMap(Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper, BinaryOperator<V> mergeFunction) {
        final Supplier<LinkedHashMap<K, V>> mapFactory = Suppliers.ofLinkedHashMap();

        return toMap(keyMapper, valueMapper, mergeFunction, mapFactory);
    }

    public static <T, K, V> Collector<T, ?, ConcurrentMap<K, V>> toConcurrentMap(Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper) {
        final BinaryOperator<V> mergeFunction = Fn.throwingMerger();

        return toConcurrentMap(keyMapper, valueMapper, mergeFunction);
    }

    public static <T, K, V, M extends ConcurrentMap<K, V>> Collector<T, ?, M> toConcurrentMap(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper, Supplier<? extends M> mapFactory) {
        final BinaryOperator<V> mergeFunction = Fn.throwingMerger();

        return toConcurrentMap(keyMapper, valueMapper, mergeFunction, mapFactory);
    }

    public static <T, K, V> Collector<T, ?, ConcurrentMap<K, V>> toConcurrentMap(Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper, BinaryOperator<V> mergeFunction) {
        final Supplier<ConcurrentMap<K, V>> mapFactory = Suppliers.ofConcurrentMap();

        return toConcurrentMap(keyMapper, valueMapper, mergeFunction, mapFactory);
    }

    public static <T, K, V, M extends ConcurrentMap<K, V>> Collector<T, ?, M> toConcurrentMap(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper, final BinaryOperator<V> mergeFunction, Supplier<? extends M> mapFactory) {

        final BiConsumer<M, T> accumulator = new BiConsumer<M, T>() {
            @Override
            public void accept(M map, T element) {
                merge(map, keyMapper.apply(element), valueMapper.apply(element), mergeFunction);
            }
        };

        final BinaryOperator<M> combiner = (BinaryOperator<M>) concurrentMapMerger(mergeFunction);

        return new CollectorImpl<T, M, M>(mapFactory, accumulator, combiner, CH_UNORDERED_ID);
    }

    public static <T, K, V> Collector<T, ?, BiMap<K, V>> toBiMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        final BinaryOperator<V> mergeFunction = Fn.throwingMerger();

        return toBiMap(keyMapper, valueMapper, mergeFunction);
    }

    public static <T, K, V> Collector<T, ?, BiMap<K, V>> toBiMap(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper, final Supplier<BiMap<K, V>> mapFactory) {
        final BinaryOperator<V> mergeFunction = Fn.throwingMerger();

        return toBiMap(keyMapper, valueMapper, mergeFunction, mapFactory);
    }

    public static <T, K, V> Collector<T, ?, BiMap<K, V>> toBiMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper,
            BinaryOperator<V> mergeFunction) {
        final Supplier<BiMap<K, V>> mapFactory = Suppliers.ofBiMap();

        return toBiMap(keyMapper, valueMapper, mergeFunction, mapFactory);
    }

    public static <T, K, V> Collector<T, ?, BiMap<K, V>> toBiMap(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper, final BinaryOperator<V> mergeFunction, final Supplier<BiMap<K, V>> mapFactory) {
        return toMap(keyMapper, valueMapper, mergeFunction, mapFactory);
    }

    @SuppressWarnings("rawtypes")
    public static <K, V> Collector<Map.Entry<? extends K, ? extends V>, ?, ListMultimap<K, V>> toMultimap() {
        final Function<Map.Entry<? extends K, ? extends V>, ? extends K> keyMapper = (Function) Fn.key();
        final Function<Map.Entry<? extends K, ? extends V>, ? extends V> valueMapper = (Function) Fn.value();

        return toMultimap(keyMapper, valueMapper);
    }

    @SuppressWarnings("rawtypes")
    public static <K, V, C extends Collection<V>, M extends Multimap<K, V, C>> Collector<Map.Entry<? extends K, ? extends V>, ?, M> toMultimap(
            final Supplier<? extends M> mapFactory) {
        final Function<Map.Entry<? extends K, ? extends V>, ? extends K> keyMapper = (Function) Fn.key();
        final Function<Map.Entry<? extends K, ? extends V>, ? extends V> valueMapper = (Function) Fn.value();

        return toMultimap(keyMapper, valueMapper, mapFactory);
    }

    public static <T, K> Collector<T, ?, ListMultimap<K, T>> toMultimap(Function<? super T, ? extends K> keyMapper) {
        final Function<? super T, ? extends T> valueMapper = Fn.identity();

        return toMultimap(keyMapper, valueMapper);
    }

    public static <T, K, C extends Collection<T>, M extends Multimap<K, T, C>> Collector<T, ?, M> toMultimap(final Function<? super T, ? extends K> keyMapper,
            final Supplier<? extends M> mapFactory) {
        final Function<? super T, ? extends T> valueMapper = Fn.identity();

        return toMultimap(keyMapper, valueMapper, mapFactory);
    }

    public static <T, K, V> Collector<T, ?, ListMultimap<K, V>> toMultimap(Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper) {
        final Supplier<ListMultimap<K, V>> mapFactory = Suppliers.ofListMultimap();

        return toMultimap(keyMapper, valueMapper, mapFactory);
    }

    public static <T, K, V, C extends Collection<V>, M extends Multimap<K, V, C>> Collector<T, ?, M> toMultimap(
            final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper, final Supplier<? extends M> mapFactory) {
        final BiConsumer<M, T> accumulator = new BiConsumer<M, T>() {
            @Override
            public void accept(M map, T element) {
                map.put(keyMapper.apply(element), valueMapper.apply(element));
            }
        };

        final BinaryOperator<M> combiner = Collectors.<K, V, C, M> multimapMerger();

        return new CollectorImpl<>(mapFactory, accumulator, combiner, CH_UNORDERED_ID);
    }

    public static <T> Collector<T, ?, DataSet> toDataSet() {
        return toDataSet(null);
    }

    public static <T> Collector<T, ?, DataSet> toDataSet(final List<String> columnNames) {
        @SuppressWarnings("rawtypes")
        final Collector<T, List<T>, List<T>> collector = (Collector) toList();

        final Function<List<T>, DataSet> finisher = new Function<List<T>, DataSet>() {
            @Override
            public DataSet apply(List<T> t) {
                return N.newDataSet(columnNames, t);
            }
        };

        return new CollectorImpl<T, List<T>, DataSet>(collector.supplier(), collector.accumulator(), collector.combiner(), finisher, CH_NOID);
    }

    public static <T, A1, A2, R1, R2> Collector<T, Tuple2<A1, A2>, Tuple2<R1, R2>> combine(final Collector<? super T, A1, R1> collector1,
            final Collector<? super T, A2, R2> collector2) {
        final Supplier<A1> supplier1 = collector1.supplier();
        final Supplier<A2> supplier2 = collector2.supplier();
        final BiConsumer<A1, ? super T> accumulator1 = collector1.accumulator();
        final BiConsumer<A2, ? super T> accumulator2 = collector2.accumulator();
        final BinaryOperator<A1> combiner1 = collector1.combiner();
        final BinaryOperator<A2> combiner2 = collector2.combiner();
        final Function<A1, R1> finisher1 = collector1.finisher();
        final Function<A2, R2> finisher2 = collector2.finisher();

        final Supplier<Tuple2<A1, A2>> supplier = new Supplier<Tuple2<A1, A2>>() {
            @Override
            public Tuple2<A1, A2> get() {
                return Tuple.of(supplier1.get(), supplier2.get());
            }
        };

        final BiConsumer<Tuple2<A1, A2>, T> accumulator = new BiConsumer<Tuple2<A1, A2>, T>() {
            @Override
            public void accept(Tuple2<A1, A2> acct, T e) {
                accumulator1.accept(acct._1, e);
                accumulator2.accept(acct._2, e);
            }
        };

        final BinaryOperator<Tuple2<A1, A2>> combiner = new BinaryOperator<Tuple2<A1, A2>>() {
            @Override
            public Tuple2<A1, A2> apply(Tuple2<A1, A2> t, Tuple2<A1, A2> u) {
                return Tuple.of(combiner1.apply(t._1, u._1), combiner2.apply(t._2, u._2));
            }
        };

        List<Characteristics> common = N.intersection(collector1.characteristics(), collector2.characteristics());
        final Set<Characteristics> characteristics = N.isNullOrEmpty(common) ? CH_NOID : new HashSet<>(common);

        if (characteristics.contains(Characteristics.IDENTITY_FINISH)) {
            return new CollectorImpl<>(supplier, accumulator, combiner, characteristics);
        } else {
            final Function<Tuple2<A1, A2>, Tuple2<R1, R2>> finisher = new Function<Tuple2<A1, A2>, Tuple2<R1, R2>>() {
                @Override
                public Tuple2<R1, R2> apply(Tuple2<A1, A2> t) {
                    return Tuple.of(finisher1.apply(t._1), finisher2.apply(t._2));
                }
            };

            return new CollectorImpl<>(supplier, accumulator, combiner, finisher, characteristics);
        }
    }

    public static <T, A1, A2, R1, R2, R> Collector<T, Tuple2<A1, A2>, R> combine(final Collector<? super T, A1, R1> collector1,
            final Collector<? super T, A2, R2> collector2, final BiFunction<? super R1, ? super R2, R> finisher) {
        final Supplier<A1> supplier1 = collector1.supplier();
        final Supplier<A2> supplier2 = collector2.supplier();
        final BiConsumer<A1, ? super T> accumulator1 = collector1.accumulator();
        final BiConsumer<A2, ? super T> accumulator2 = collector2.accumulator();
        final BinaryOperator<A1> combiner1 = collector1.combiner();
        final BinaryOperator<A2> combiner2 = collector2.combiner();
        final Function<A1, R1> finisher1 = collector1.finisher();
        final Function<A2, R2> finisher2 = collector2.finisher();

        final Supplier<Tuple2<A1, A2>> supplier = new Supplier<Tuple2<A1, A2>>() {
            @Override
            public Tuple2<A1, A2> get() {
                return Tuple.of(supplier1.get(), supplier2.get());
            }
        };

        final BiConsumer<Tuple2<A1, A2>, T> accumulator = new BiConsumer<Tuple2<A1, A2>, T>() {
            @Override
            public void accept(Tuple2<A1, A2> acct, T e) {
                accumulator1.accept(acct._1, e);
                accumulator2.accept(acct._2, e);
            }
        };

        final BinaryOperator<Tuple2<A1, A2>> combiner = new BinaryOperator<Tuple2<A1, A2>>() {
            @Override
            public Tuple2<A1, A2> apply(Tuple2<A1, A2> t, Tuple2<A1, A2> u) {
                return Tuple.of(combiner1.apply(t._1, u._1), combiner2.apply(t._2, u._2));
            }
        };

        final List<Characteristics> common = N.intersection(collector1.characteristics(), collector2.characteristics());
        common.remove(Characteristics.IDENTITY_FINISH);
        final Set<Characteristics> characteristics = N.isNullOrEmpty(common) ? CH_NOID : new HashSet<>(common);

        final Function<Tuple2<A1, A2>, R> finalFinisher = new Function<Tuple2<A1, A2>, R>() {
            @Override
            public R apply(Tuple2<A1, A2> t) {
                return finisher.apply(finisher1.apply(t._1), finisher2.apply(t._2));
            }
        };

        return new CollectorImpl<>(supplier, accumulator, combiner, finalFinisher, characteristics);
    }

    public static <T, A1, A2, A3, R1, R2, R3> Collector<T, Tuple3<A1, A2, A3>, Tuple3<R1, R2, R3>> combine(final Collector<? super T, A1, R1> collector1,
            final Collector<? super T, A2, R2> collector2, final Collector<? super T, A3, R3> collector3) {
        final Supplier<A1> supplier1 = collector1.supplier();
        final Supplier<A2> supplier2 = collector2.supplier();
        final Supplier<A3> supplier3 = collector3.supplier();
        final BiConsumer<A1, ? super T> accumulator1 = collector1.accumulator();
        final BiConsumer<A2, ? super T> accumulator2 = collector2.accumulator();
        final BiConsumer<A3, ? super T> accumulator3 = collector3.accumulator();
        final BinaryOperator<A1> combiner1 = collector1.combiner();
        final BinaryOperator<A2> combiner2 = collector2.combiner();
        final BinaryOperator<A3> combiner3 = collector3.combiner();
        final Function<A1, R1> finisher1 = collector1.finisher();
        final Function<A2, R2> finisher2 = collector2.finisher();
        final Function<A3, R3> finisher3 = collector3.finisher();

        final Supplier<Tuple3<A1, A2, A3>> supplier = new Supplier<Tuple3<A1, A2, A3>>() {
            @Override
            public Tuple3<A1, A2, A3> get() {
                return Tuple.of(supplier1.get(), supplier2.get(), supplier3.get());
            }
        };

        final BiConsumer<Tuple3<A1, A2, A3>, T> accumulator = new BiConsumer<Tuple3<A1, A2, A3>, T>() {
            @Override
            public void accept(Tuple3<A1, A2, A3> acct, T e) {
                accumulator1.accept(acct._1, e);
                accumulator2.accept(acct._2, e);
                accumulator3.accept(acct._3, e);
            }
        };

        final BinaryOperator<Tuple3<A1, A2, A3>> combiner = new BinaryOperator<Tuple3<A1, A2, A3>>() {
            @Override
            public Tuple3<A1, A2, A3> apply(Tuple3<A1, A2, A3> t, Tuple3<A1, A2, A3> u) {
                return Tuple.of(combiner1.apply(t._1, u._1), combiner2.apply(t._2, u._2), combiner3.apply(t._3, u._3));
            }
        };

        List<Characteristics> common = N.intersection(collector1.characteristics(), collector2.characteristics());

        if (N.notNullOrEmpty(common)) {
            common = N.intersection(common, collector3.characteristics());
        }

        final Set<Characteristics> characteristics = N.isNullOrEmpty(common) ? CH_NOID : new HashSet<>(common);

        if (characteristics.contains(Characteristics.IDENTITY_FINISH)) {
            return new CollectorImpl<>(supplier, accumulator, combiner, characteristics);
        } else {
            final Function<Tuple3<A1, A2, A3>, Tuple3<R1, R2, R3>> finisher = new Function<Tuple3<A1, A2, A3>, Tuple3<R1, R2, R3>>() {
                @Override
                public Tuple3<R1, R2, R3> apply(Tuple3<A1, A2, A3> t) {
                    return Tuple.of(finisher1.apply(t._1), finisher2.apply(t._2), finisher3.apply(t._3));
                }
            };

            return new CollectorImpl<>(supplier, accumulator, combiner, finisher, characteristics);
        }
    }

    public static <T, A1, A2, A3, R1, R2, R3, R> Collector<T, Tuple3<A1, A2, A3>, R> combine(final Collector<? super T, A1, R1> collector1,
            final Collector<? super T, A2, R2> collector2, final Collector<? super T, A3, R3> collector3,
            final TriFunction<? super R1, ? super R2, ? super R3, R> finisher) {
        final Supplier<A1> supplier1 = collector1.supplier();
        final Supplier<A2> supplier2 = collector2.supplier();
        final Supplier<A3> supplier3 = collector3.supplier();
        final BiConsumer<A1, ? super T> accumulator1 = collector1.accumulator();
        final BiConsumer<A2, ? super T> accumulator2 = collector2.accumulator();
        final BiConsumer<A3, ? super T> accumulator3 = collector3.accumulator();
        final BinaryOperator<A1> combiner1 = collector1.combiner();
        final BinaryOperator<A2> combiner2 = collector2.combiner();
        final BinaryOperator<A3> combiner3 = collector3.combiner();
        final Function<A1, R1> finisher1 = collector1.finisher();
        final Function<A2, R2> finisher2 = collector2.finisher();
        final Function<A3, R3> finisher3 = collector3.finisher();

        final Supplier<Tuple3<A1, A2, A3>> supplier = new Supplier<Tuple3<A1, A2, A3>>() {
            @Override
            public Tuple3<A1, A2, A3> get() {
                return Tuple.of(supplier1.get(), supplier2.get(), supplier3.get());
            }
        };

        final BiConsumer<Tuple3<A1, A2, A3>, T> accumulator = new BiConsumer<Tuple3<A1, A2, A3>, T>() {
            @Override
            public void accept(Tuple3<A1, A2, A3> acct, T e) {
                accumulator1.accept(acct._1, e);
                accumulator2.accept(acct._2, e);
                accumulator3.accept(acct._3, e);
            }
        };

        final BinaryOperator<Tuple3<A1, A2, A3>> combiner = new BinaryOperator<Tuple3<A1, A2, A3>>() {
            @Override
            public Tuple3<A1, A2, A3> apply(Tuple3<A1, A2, A3> t, Tuple3<A1, A2, A3> u) {
                return Tuple.of(combiner1.apply(t._1, u._1), combiner2.apply(t._2, u._2), combiner3.apply(t._3, u._3));
            }
        };

        List<Characteristics> common = N.intersection(collector1.characteristics(), collector2.characteristics());

        if (N.notNullOrEmpty(common)) {
            common = N.intersection(common, collector3.characteristics());
        }

        common.remove(Characteristics.IDENTITY_FINISH);
        final Set<Characteristics> characteristics = N.isNullOrEmpty(common) ? CH_NOID : new HashSet<>(common);

        final Function<Tuple3<A1, A2, A3>, R> finalFinisher = new Function<Tuple3<A1, A2, A3>, R>() {
            @Override
            public R apply(Tuple3<A1, A2, A3> t) {
                return finisher.apply(finisher1.apply(t._1), finisher2.apply(t._2), finisher3.apply(t._3));
            }
        };

        return new CollectorImpl<>(supplier, accumulator, combiner, finalFinisher, characteristics);
    }

    @SuppressWarnings("rawtypes")
    public static <T, A1, A2, A3, A4, R1, R2, R3, R4> Collector<T, Tuple4<A1, A2, A3, A4>, Tuple4<R1, R2, R3, R4>> combine(
            final Collector<? super T, A1, R1> collector1, final Collector<? super T, A2, R2> collector2, final Collector<? super T, A3, R3> collector3,
            final Collector<? super T, A4, R4> collector4) {
        final List<Collector<? super T, ?, ?>> collectors = (List) N.asList(collector1, collector2, collector3, collector4);

        final Function<List<?>, Tuple4<A1, A2, A3, A4>> func = new Function<List<?>, Tuple4<A1, A2, A3, A4>>() {
            @Override
            public Tuple4<A1, A2, A3, A4> apply(List<?> t) {
                return Tuple4.from(t);
            }
        };

        return (Collector) collectingAndThen(combine(collectors), func);
    }

    @SuppressWarnings("rawtypes")
    public static <T, A1, A2, A3, A4, A5, R1, R2, R3, R4, R5> Collector<T, Tuple5<A1, A2, A3, A4, A5>, Tuple5<R1, R2, R3, R4, R5>> combine(
            final Collector<? super T, A1, R1> collector1, final Collector<? super T, A2, R2> collector2, final Collector<? super T, A3, R3> collector3,
            final Collector<? super T, A4, R4> collector4, final Collector<? super T, A5, R5> collector5) {

        final List<Collector<? super T, ?, ?>> collectors = (List) N.asList(collector1, collector2, collector3, collector4, collector5);

        final Function<List<?>, Tuple5<A1, A2, A3, A4, A5>> func = new Function<List<?>, Tuple5<A1, A2, A3, A4, A5>>() {
            @Override
            public Tuple5<A1, A2, A3, A4, A5> apply(List<?> t) {
                return Tuple5.from(t);
            }
        };

        return (Collector) collectingAndThen(combine(collectors), func);
    }

    /**
     * 
     * @param collectors
     * @return
     * @see Tuple#from(Collection)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Collector<T, ?, List<?>> combine(final List<? extends Collector<? super T, ?, ?>> collectors) {
        N.checkArgument(N.notNullOrEmpty(collectors), "The specified 'collectors' can't be null or empty");

        final int len = collectors.size();
        final Collector<T, Object, Object>[] cs = collectors.toArray(new Collector[len]);

        final Supplier<List<Object>> supplier = new Supplier<List<Object>>() {
            @Override
            public List<Object> get() {
                final List<Object> res = new ArrayList<>(len);

                for (int i = 0; i < len; i++) {
                    res.add(cs[i].supplier().get());
                }

                return res;
            }
        };

        final BiConsumer<List<Object>, T> accumulator = new BiConsumer<List<Object>, T>() {
            @Override
            public void accept(List<Object> acct, T e) {
                for (int i = 0; i < len; i++) {
                    cs[i].accumulator().accept(acct.get(i), e);
                }
            }
        };

        final BinaryOperator<List<Object>> combiner = new BinaryOperator<List<Object>>() {
            @Override
            public List<Object> apply(List<Object> t, List<Object> u) {
                for (int i = 0; i < len; i++) {
                    t.set(i, cs[i].combiner().apply(t.get(i), u.get(i)));
                }

                return t;
            }
        };

        Collection<Characteristics> common = cs[0].characteristics();

        for (int i = 1; i < len && N.notNullOrEmpty(common); i++) {
            common = N.intersection(common, cs[i].characteristics());
        }

        final Set<Characteristics> characteristics = N.isNullOrEmpty(common) ? CH_NOID : new HashSet<>(common);

        if (characteristics.contains(Characteristics.IDENTITY_FINISH)) {
            return new CollectorImpl<>(supplier, accumulator, combiner, characteristics);
        } else {
            final Function<List<Object>, List<Object>> finisher = new Function<List<Object>, List<Object>>() {
                @Override
                public List<Object> apply(List<Object> t) {
                    for (int i = 0; i < len; i++) {
                        t.set(i, cs[i].finisher().apply(t.get(i)));
                    }

                    return t;
                }
            };

            return (Collector) new CollectorImpl<>(supplier, accumulator, combiner, finisher, characteristics);
        }
    }

    public static <T, A1, A2, R1, R2> Collector<T, Tuple2<A1, A2>, Tuple2<R1, R2>> combine(final java.util.stream.Collector<? super T, A1, R1> collector1,
            final java.util.stream.Collector<? super T, A2, R2> collector2) {
        final java.util.function.Supplier<A1> supplier1 = collector1.supplier();
        final java.util.function.Supplier<A2> supplier2 = collector2.supplier();
        final java.util.function.BiConsumer<A1, ? super T> accumulator1 = collector1.accumulator();
        final java.util.function.BiConsumer<A2, ? super T> accumulator2 = collector2.accumulator();
        final java.util.function.BinaryOperator<A1> combiner1 = collector1.combiner();
        final java.util.function.BinaryOperator<A2> combiner2 = collector2.combiner();
        final java.util.function.Function<A1, R1> finisher1 = collector1.finisher();
        final java.util.function.Function<A2, R2> finisher2 = collector2.finisher();

        final Supplier<Tuple2<A1, A2>> supplier = new Supplier<Tuple2<A1, A2>>() {
            @Override
            public Tuple2<A1, A2> get() {
                return Tuple.of(supplier1.get(), supplier2.get());
            }
        };

        final BiConsumer<Tuple2<A1, A2>, T> accumulator = new BiConsumer<Tuple2<A1, A2>, T>() {
            @Override
            public void accept(Tuple2<A1, A2> acct, T e) {
                accumulator1.accept(acct._1, e);
                accumulator2.accept(acct._2, e);
            }
        };

        final BinaryOperator<Tuple2<A1, A2>> combiner = new BinaryOperator<Tuple2<A1, A2>>() {
            @Override
            public Tuple2<A1, A2> apply(Tuple2<A1, A2> t, Tuple2<A1, A2> u) {
                return Tuple.of(combiner1.apply(t._1, u._1), combiner2.apply(t._2, u._2));
            }
        };

        List<Characteristics> common = N.intersection(collector1.characteristics(), collector2.characteristics());
        final Set<Characteristics> characteristics = N.isNullOrEmpty(common) ? CH_NOID : new HashSet<>(common);

        if (characteristics.contains(Characteristics.IDENTITY_FINISH)) {
            return new CollectorImpl<>(supplier, accumulator, combiner, characteristics);
        } else {
            final Function<Tuple2<A1, A2>, Tuple2<R1, R2>> finisher = new Function<Tuple2<A1, A2>, Tuple2<R1, R2>>() {
                @Override
                public Tuple2<R1, R2> apply(Tuple2<A1, A2> t) {
                    return Tuple.of(finisher1.apply(t._1), finisher2.apply(t._2));
                }
            };

            return new CollectorImpl<>(supplier, accumulator, combiner, finisher, characteristics);
        }
    }

    public static <T, A1, A2, R1, R2, R> Collector<T, Tuple2<A1, A2>, R> combine(final java.util.stream.Collector<? super T, A1, R1> collector1,
            final java.util.stream.Collector<? super T, A2, R2> collector2, final java.util.function.BiFunction<? super R1, ? super R2, R> finisher) {
        final java.util.function.Supplier<A1> supplier1 = collector1.supplier();
        final java.util.function.Supplier<A2> supplier2 = collector2.supplier();
        final java.util.function.BiConsumer<A1, ? super T> accumulator1 = collector1.accumulator();
        final java.util.function.BiConsumer<A2, ? super T> accumulator2 = collector2.accumulator();
        final java.util.function.BinaryOperator<A1> combiner1 = collector1.combiner();
        final java.util.function.BinaryOperator<A2> combiner2 = collector2.combiner();
        final java.util.function.Function<A1, R1> finisher1 = collector1.finisher();
        final java.util.function.Function<A2, R2> finisher2 = collector2.finisher();

        final Supplier<Tuple2<A1, A2>> supplier = new Supplier<Tuple2<A1, A2>>() {
            @Override
            public Tuple2<A1, A2> get() {
                return Tuple.of(supplier1.get(), supplier2.get());
            }
        };

        final BiConsumer<Tuple2<A1, A2>, T> accumulator = new BiConsumer<Tuple2<A1, A2>, T>() {
            @Override
            public void accept(Tuple2<A1, A2> acct, T e) {
                accumulator1.accept(acct._1, e);
                accumulator2.accept(acct._2, e);
            }
        };

        final BinaryOperator<Tuple2<A1, A2>> combiner = new BinaryOperator<Tuple2<A1, A2>>() {
            @Override
            public Tuple2<A1, A2> apply(Tuple2<A1, A2> t, Tuple2<A1, A2> u) {
                return Tuple.of(combiner1.apply(t._1, u._1), combiner2.apply(t._2, u._2));
            }
        };

        final List<Characteristics> common = N.intersection(collector1.characteristics(), collector2.characteristics());
        common.remove(Characteristics.IDENTITY_FINISH);
        final Set<Characteristics> characteristics = N.isNullOrEmpty(common) ? CH_NOID : new HashSet<>(common);

        final Function<Tuple2<A1, A2>, R> finalFinisher = new Function<Tuple2<A1, A2>, R>() {
            @Override
            public R apply(Tuple2<A1, A2> t) {
                return finisher.apply(finisher1.apply(t._1), finisher2.apply(t._2));
            }
        };

        return new CollectorImpl<>(supplier, accumulator, combiner, finalFinisher, characteristics);
    }

    public static <T, A1, A2, A3, R1, R2, R3> Collector<T, Tuple3<A1, A2, A3>, Tuple3<R1, R2, R3>> combine(
            final java.util.stream.Collector<? super T, A1, R1> collector1, final java.util.stream.Collector<? super T, A2, R2> collector2,
            final java.util.stream.Collector<? super T, A3, R3> collector3) {
        final java.util.function.Supplier<A1> supplier1 = collector1.supplier();
        final java.util.function.Supplier<A2> supplier2 = collector2.supplier();
        final java.util.function.Supplier<A3> supplier3 = collector3.supplier();
        final java.util.function.BiConsumer<A1, ? super T> accumulator1 = collector1.accumulator();
        final java.util.function.BiConsumer<A2, ? super T> accumulator2 = collector2.accumulator();
        final java.util.function.BiConsumer<A3, ? super T> accumulator3 = collector3.accumulator();
        final java.util.function.BinaryOperator<A1> combiner1 = collector1.combiner();
        final java.util.function.BinaryOperator<A2> combiner2 = collector2.combiner();
        final java.util.function.BinaryOperator<A3> combiner3 = collector3.combiner();
        final java.util.function.Function<A1, R1> finisher1 = collector1.finisher();
        final java.util.function.Function<A2, R2> finisher2 = collector2.finisher();
        final java.util.function.Function<A3, R3> finisher3 = collector3.finisher();

        final Supplier<Tuple3<A1, A2, A3>> supplier = new Supplier<Tuple3<A1, A2, A3>>() {
            @Override
            public Tuple3<A1, A2, A3> get() {
                return Tuple.of(supplier1.get(), supplier2.get(), supplier3.get());
            }
        };

        final BiConsumer<Tuple3<A1, A2, A3>, T> accumulator = new BiConsumer<Tuple3<A1, A2, A3>, T>() {
            @Override
            public void accept(Tuple3<A1, A2, A3> acct, T e) {
                accumulator1.accept(acct._1, e);
                accumulator2.accept(acct._2, e);
                accumulator3.accept(acct._3, e);
            }
        };

        final BinaryOperator<Tuple3<A1, A2, A3>> combiner = new BinaryOperator<Tuple3<A1, A2, A3>>() {
            @Override
            public Tuple3<A1, A2, A3> apply(Tuple3<A1, A2, A3> t, Tuple3<A1, A2, A3> u) {
                return Tuple.of(combiner1.apply(t._1, u._1), combiner2.apply(t._2, u._2), combiner3.apply(t._3, u._3));
            }
        };

        List<Characteristics> common = N.intersection(collector1.characteristics(), collector2.characteristics());

        if (N.notNullOrEmpty(common)) {
            common = N.intersection(common, collector3.characteristics());
        }

        final Set<Characteristics> characteristics = N.isNullOrEmpty(common) ? CH_NOID : new HashSet<>(common);

        if (characteristics.contains(Characteristics.IDENTITY_FINISH)) {
            return new CollectorImpl<>(supplier, accumulator, combiner, characteristics);
        } else {
            final Function<Tuple3<A1, A2, A3>, Tuple3<R1, R2, R3>> finisher = new Function<Tuple3<A1, A2, A3>, Tuple3<R1, R2, R3>>() {
                @Override
                public Tuple3<R1, R2, R3> apply(Tuple3<A1, A2, A3> t) {
                    return Tuple.of(finisher1.apply(t._1), finisher2.apply(t._2), finisher3.apply(t._3));
                }
            };

            return new CollectorImpl<>(supplier, accumulator, combiner, finisher, characteristics);
        }
    }

    public static <T, A1, A2, A3, R1, R2, R3, R> Collector<T, Tuple3<A1, A2, A3>, R> combine(final java.util.stream.Collector<? super T, A1, R1> collector1,
            final java.util.stream.Collector<? super T, A2, R2> collector2, final java.util.stream.Collector<? super T, A3, R3> collector3,
            final TriFunction<? super R1, ? super R2, ? super R3, R> finisher) {
        final java.util.function.Supplier<A1> supplier1 = collector1.supplier();
        final java.util.function.Supplier<A2> supplier2 = collector2.supplier();
        final java.util.function.Supplier<A3> supplier3 = collector3.supplier();
        final java.util.function.BiConsumer<A1, ? super T> accumulator1 = collector1.accumulator();
        final java.util.function.BiConsumer<A2, ? super T> accumulator2 = collector2.accumulator();
        final java.util.function.BiConsumer<A3, ? super T> accumulator3 = collector3.accumulator();
        final java.util.function.BinaryOperator<A1> combiner1 = collector1.combiner();
        final java.util.function.BinaryOperator<A2> combiner2 = collector2.combiner();
        final java.util.function.BinaryOperator<A3> combiner3 = collector3.combiner();
        final java.util.function.Function<A1, R1> finisher1 = collector1.finisher();
        final java.util.function.Function<A2, R2> finisher2 = collector2.finisher();
        final java.util.function.Function<A3, R3> finisher3 = collector3.finisher();

        final Supplier<Tuple3<A1, A2, A3>> supplier = new Supplier<Tuple3<A1, A2, A3>>() {
            @Override
            public Tuple3<A1, A2, A3> get() {
                return Tuple.of(supplier1.get(), supplier2.get(), supplier3.get());
            }
        };

        final BiConsumer<Tuple3<A1, A2, A3>, T> accumulator = new BiConsumer<Tuple3<A1, A2, A3>, T>() {
            @Override
            public void accept(Tuple3<A1, A2, A3> acct, T e) {
                accumulator1.accept(acct._1, e);
                accumulator2.accept(acct._2, e);
                accumulator3.accept(acct._3, e);
            }
        };

        final BinaryOperator<Tuple3<A1, A2, A3>> combiner = new BinaryOperator<Tuple3<A1, A2, A3>>() {
            @Override
            public Tuple3<A1, A2, A3> apply(Tuple3<A1, A2, A3> t, Tuple3<A1, A2, A3> u) {
                return Tuple.of(combiner1.apply(t._1, u._1), combiner2.apply(t._2, u._2), combiner3.apply(t._3, u._3));
            }
        };

        List<Characteristics> common = N.intersection(collector1.characteristics(), collector2.characteristics());

        if (N.notNullOrEmpty(common)) {
            common = N.intersection(common, collector3.characteristics());
        }

        common.remove(Characteristics.IDENTITY_FINISH);
        final Set<Characteristics> characteristics = N.isNullOrEmpty(common) ? CH_NOID : new HashSet<>(common);

        final Function<Tuple3<A1, A2, A3>, R> finalFinisher = new Function<Tuple3<A1, A2, A3>, R>() {
            @Override
            public R apply(Tuple3<A1, A2, A3> t) {
                return finisher.apply(finisher1.apply(t._1), finisher2.apply(t._2), finisher3.apply(t._3));
            }
        };

        return new CollectorImpl<>(supplier, accumulator, combiner, finalFinisher, characteristics);
    }

    @SuppressWarnings("rawtypes")
    public static <T, A1, A2, A3, A4, R1, R2, R3, R4> Collector<T, Tuple4<A1, A2, A3, A4>, Tuple4<R1, R2, R3, R4>> combine(
            final java.util.stream.Collector<? super T, A1, R1> collector1, final java.util.stream.Collector<? super T, A2, R2> collector2,
            final java.util.stream.Collector<? super T, A3, R3> collector3, final java.util.stream.Collector<? super T, A4, R4> collector4) {
        final List<java.util.stream.Collector<? super T, ?, ?>> collectors = (List) N.asList(collector1, collector2, collector3, collector4);

        final Function<List<?>, Tuple4<A1, A2, A3, A4>> func = new Function<List<?>, Tuple4<A1, A2, A3, A4>>() {
            @Override
            public Tuple4<A1, A2, A3, A4> apply(List<?> t) {
                return Tuple4.from(t);
            }
        };

        return (Collector) collectingAndThen(combine(collectors), func);
    }

    @SuppressWarnings("rawtypes")
    public static <T, A1, A2, A3, A4, A5, R1, R2, R3, R4, R5> Collector<T, Tuple5<A1, A2, A3, A4, A5>, Tuple5<R1, R2, R3, R4, R5>> combine(
            final java.util.stream.Collector<? super T, A1, R1> collector1, final java.util.stream.Collector<? super T, A2, R2> collector2,
            final java.util.stream.Collector<? super T, A3, R3> collector3, final java.util.stream.Collector<? super T, A4, R4> collector4,
            final java.util.stream.Collector<? super T, A5, R5> collector5) {

        final List<java.util.stream.Collector<? super T, ?, ?>> collectors = (List) N.asList(collector1, collector2, collector3, collector4, collector5);

        final Function<List<?>, Tuple5<A1, A2, A3, A4, A5>> func = new Function<List<?>, Tuple5<A1, A2, A3, A4, A5>>() {
            @Override
            public Tuple5<A1, A2, A3, A4, A5> apply(List<?> t) {
                return Tuple5.from(t);
            }
        };

        return (Collector) collectingAndThen(combine(collectors), func);
    }

    /**
     * 
     * @param collectors
     * @return
     * @see Tuple#from(Collection)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Collector<T, ?, List<?>> combine(final Collection<? extends java.util.stream.Collector<? super T, ?, ?>> collectors) {
        N.checkArgument(N.notNullOrEmpty(collectors), "The specified 'collectors' can't be null or empty");

        final int len = collectors.size();
        final java.util.stream.Collector<T, Object, Object>[] cs = collectors.toArray(new java.util.stream.Collector[len]);

        final Supplier<List<Object>> supplier = new Supplier<List<Object>>() {
            @Override
            public List<Object> get() {
                final List<Object> res = new ArrayList<>(len);

                for (int i = 0; i < len; i++) {
                    res.add(cs[i].supplier().get());
                }

                return res;
            }
        };

        final BiConsumer<List<Object>, T> accumulator = new BiConsumer<List<Object>, T>() {
            @Override
            public void accept(List<Object> acct, T e) {
                for (int i = 0; i < len; i++) {
                    cs[i].accumulator().accept(acct.get(i), e);
                }
            }
        };

        final BinaryOperator<List<Object>> combiner = new BinaryOperator<List<Object>>() {
            @Override
            public List<Object> apply(List<Object> t, List<Object> u) {
                for (int i = 0; i < len; i++) {
                    t.set(i, cs[i].combiner().apply(t.get(i), u.get(i)));
                }

                return t;
            }
        };

        Collection<Characteristics> common = cs[0].characteristics();

        for (int i = 1; i < len && N.notNullOrEmpty(common); i++) {
            common = N.intersection(common, cs[i].characteristics());
        }

        final Set<Characteristics> characteristics = N.isNullOrEmpty(common) ? CH_NOID : new HashSet<>(common);

        if (characteristics.contains(Characteristics.IDENTITY_FINISH)) {
            return new CollectorImpl<>(supplier, accumulator, combiner, characteristics);
        } else {
            final Function<List<Object>, List<Object>> finisher = new Function<List<Object>, List<Object>>() {
                @Override
                public List<Object> apply(List<Object> t) {
                    for (int i = 0; i < len; i++) {
                        t.set(i, cs[i].finisher().apply(t.get(i)));
                    }

                    return t;
                }
            };

            return (Collector) new CollectorImpl<>(supplier, accumulator, combiner, finisher, characteristics);
        }
    }

    //    public static <T> Collector<T, ?, DataSet> toDataSet(final String entityName, final Class<?> entityClass, final List<String> columnNames) {
    //        @SuppressWarnings("rawtypes")
    //        final Collector<T, List<T>, List<T>> collector = (Collector) toList();
    //
    //        final Function<List<T>, DataSet> finisher = new Function<List<T>, DataSet>() {
    //            @Override
    //            public DataSet apply(List<T> t) {
    //                return N.newDataSet(entityName, entityClass, columnNames, t);
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

        return new BinaryOperator<M>() {
            @Override
            public M apply(M m1, M m2) {
                for (Map.Entry<K, V> e : m2.entrySet()) {
                    final V oldValue = m1.get(e.getKey());

                    if (oldValue == null && m1.containsKey(e.getKey()) == false) {
                        m1.put(e.getKey(), e.getValue());
                    } else {
                        m1.put(e.getKey(), mergeFunction.apply(oldValue, e.getValue()));
                    }
                }
                return m1;
            }
        };
    }

    private static <K, V, M extends ConcurrentMap<K, V>> BinaryOperator<M> concurrentMapMerger(final BinaryOperator<V> mergeFunction) {
        N.checkArgNotNull(mergeFunction);

        return new BinaryOperator<M>() {
            @Override
            public M apply(M m1, M m2) {
                for (Map.Entry<K, V> e : m2.entrySet()) {
                    final V oldValue = m1.get(e.getKey());

                    if (oldValue == null && m1.containsKey(e.getKey()) == false) {
                        m1.put(e.getKey(), e.getValue());
                    } else {
                        m1.put(e.getKey(), mergeFunction.apply(oldValue, e.getValue()));
                    }
                }
                return m1;
            }
        };
    }

    private static <K, U, V extends Collection<U>, M extends Multimap<K, U, V>> BinaryOperator<M> multimapMerger() {
        return new BinaryOperator<M>() {
            @Override
            public M apply(M m1, M m2) {
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
            }
        };
    }

    @SuppressWarnings("rawtypes")
    private static final Supplier<BlockingQueue> queueSupplier = new Supplier<BlockingQueue>() {
        @Override
        public BlockingQueue get() {
            return new ArrayBlockingQueue(64);
        }
    };

    /**
     * Note: Generally it's much slower than other {@code Collectors}.
     * 
     * @param streamingCollector
     * @return
     * @see Stream#observe(BlockingQueue, Predicate, long)
     * @see Stream#asyncCall(Try.Function)
     */
    @SuppressWarnings("rawtypes")
    public static <T, R> Collector<T, ?, R> streaming(final Function<? super Stream<T>, R> streamingCollector) {
        return streaming((Supplier) queueSupplier, streamingCollector);
    }

    /**
     * Note: Generally it's much slower than other {@code Collectors}.
     * 
     * @param maxWaitIntervalInMillis
     * @param streamingCollector
     * @return
     * @see Stream#observe(BlockingQueue, Predicate, long)
     * @see Stream#asyncCall(Try.Function)
     */
    @SuppressWarnings("rawtypes")
    public static <T, R> Collector<T, ?, R> streaming(final long maxWaitIntervalInMillis, final Function<? super Stream<T>, R> streamingCollector) {
        return streaming(maxWaitIntervalInMillis, (Supplier) queueSupplier, streamingCollector);
    }

    /**
     * Note: Generally it's much slower than other {@code Collectors}.
     * 
     * @param supplier
     * @param streamingCollector
     * @return
     * @see Stream#observe(BlockingQueue, Predicate, long)
     * @see Stream#asyncCall(Try.Function)
     */
    public static <T, R> Collector<T, ?, R> streaming(final Supplier<? extends BlockingQueue<T>> queueSupplier,
            final Function<? super Stream<T>, R> streamingCollector) {
        return streaming(10, queueSupplier, streamingCollector);
    }

    /**
     * Note: Generally it's much slower than other {@code Collectors}.
     * 
     * @param maxWaitIntervalInMillis
     * @param supplier
     * @param streamingCollector
     * @return
     * @see Stream#observe(BlockingQueue, Predicate, long)
     * @see Stream#asyncCall(Try.Function)
     */
    public static <T, R> Collector<T, ?, R> streaming(final long maxWaitIntervalInMillis, final Supplier<? extends BlockingQueue<T>> queueSupplier,
            final Function<? super Stream<T>, R> streamingCollector) {
        final Function<Stream<T>, ContinuableFuture<R>> streamingCollector2 = new Function<Stream<T>, ContinuableFuture<R>>() {
            @Override
            public ContinuableFuture<R> apply(Stream<T> t) {
                return t.asyncCall(streamingCollector);
            }
        };

        return streaming(queueSupplier, streamingCollector2, maxWaitIntervalInMillis);
    }

    /**
     * Note: Generally it's much slower than other {@code Collectors}.
     * 
     * @param supplier
     * @param streamingCollector
     * @param maxWaitIntervalInMillis
     * @return
     * @see Stream#observe(BlockingQueue, Predicate, long)
     * @see Stream#asyncCall(Try.Function)
     */
    @SuppressWarnings("rawtypes")
    public static <T, R> Collector<T, ?, R> streaming(final Supplier<? extends BlockingQueue<T>> queueSupplier,
            final Function<? super Stream<T>, ContinuableFuture<R>> streamingCollector, final long maxWaitIntervalInMillis) {
        final T NULL = (T) NONE;

        final Supplier<Tuple4<BlockingQueue<T>, MutableBoolean, MutableBoolean, Holder<ContinuableFuture<R>>>> supplier = new Supplier<Tuple4<BlockingQueue<T>, MutableBoolean, MutableBoolean, Holder<ContinuableFuture<R>>>>() {
            @Override
            public Tuple4<BlockingQueue<T>, MutableBoolean, MutableBoolean, Holder<ContinuableFuture<R>>> get() {
                return Tuple.of((BlockingQueue<T>) queueSupplier.get(), MutableBoolean.of(true), MutableBoolean.of(false), new Holder<ContinuableFuture<R>>()); // _1 = queue, _2 = hasMore, _3 = isComplete, _4=future.
            }
        };

        final BiConsumer<Tuple4<BlockingQueue<T>, MutableBoolean, MutableBoolean, Holder<ContinuableFuture<R>>>, T> accumulator = new BiConsumer<Tuple4<BlockingQueue<T>, MutableBoolean, MutableBoolean, Holder<ContinuableFuture<R>>>, T>() {
            @Override
            public void accept(Tuple4<BlockingQueue<T>, MutableBoolean, MutableBoolean, Holder<ContinuableFuture<R>>> xxx, T t) {
                if (xxx._4.value() == null) {
                    initStream(xxx, streamingCollector, maxWaitIntervalInMillis, NULL);
                }

                t = t == null ? NULL : t;

                if (xxx._3.isFalse() && xxx._1.offer(t) == false) {
                    try {
                        while (xxx._3.isFalse() && xxx._1.offer(t, maxWaitIntervalInMillis, TimeUnit.MILLISECONDS) == false) {
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };

        final BinaryOperator<Tuple4<BlockingQueue<T>, MutableBoolean, MutableBoolean, Holder<ContinuableFuture<R>>>> combiner = new BinaryOperator<Tuple4<BlockingQueue<T>, MutableBoolean, MutableBoolean, Holder<ContinuableFuture<R>>>>() {
            @Override
            public Tuple4<BlockingQueue<T>, MutableBoolean, MutableBoolean, Holder<ContinuableFuture<R>>> apply(
                    Tuple4<BlockingQueue<T>, MutableBoolean, MutableBoolean, Holder<ContinuableFuture<R>>> t,
                    Tuple4<BlockingQueue<T>, MutableBoolean, MutableBoolean, Holder<ContinuableFuture<R>>> u) {
                throw new UnsupportedOperationException("Should not happen");
            }
        };

        final Function<Tuple4<BlockingQueue<T>, MutableBoolean, MutableBoolean, Holder<ContinuableFuture<R>>>, R> finisher = new Function<Tuple4<BlockingQueue<T>, MutableBoolean, MutableBoolean, Holder<ContinuableFuture<R>>>, R>() {
            @Override
            public R apply(Tuple4<BlockingQueue<T>, MutableBoolean, MutableBoolean, Holder<ContinuableFuture<R>>> xxx) {
                if (xxx._4.value() == null) {
                    initStream(xxx, streamingCollector, maxWaitIntervalInMillis, NULL);
                }

                xxx._2.setFalse();

                try {
                    return xxx._4.value().get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        return new CollectorImpl(supplier, accumulator, combiner, finisher, CH_CONCURRENT_NOID);
    }

    private static <T, R> void initStream(final Tuple4<BlockingQueue<T>, MutableBoolean, MutableBoolean, Holder<ContinuableFuture<R>>> tp,
            final Function<? super Stream<T>, ContinuableFuture<R>> streamingCollector, final long maxWaitIntervalInMillis, final T NULL) {
        synchronized (tp) {
            if (tp._4.value() == null) {
                final BooleanSupplier hasMore = new BooleanSupplier() {
                    @Override
                    public boolean getAsBoolean() {
                        return tp._2.value();
                    }
                };

                tp._4.setValue(streamingCollector.apply(Stream.observe(tp._1, hasMore, maxWaitIntervalInMillis).map(new Function<T, T>() {
                    @Override
                    public T apply(T t) {
                        return t == NULL ? null : t;
                    }
                }).onClose(new Runnable() {
                    @Override
                    public void run() {
                        tp._3.setTrue();

                        while (tp._1.size() > 0) {
                            tp._1.clear();
                        }
                    }
                })));
            }
        }
    }

    static <K, V> void merge(Map<K, V> map, K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        N.checkArgNotNull(remappingFunction);

        final V oldValue = map.get(key);

        if (oldValue == null && map.containsKey(key) == false) {
            map.put(key, value);
        } else {
            map.put(key, remappingFunction.apply(oldValue, value));
        }
    }

    public static abstract class MoreCollectors extends Collectors {
        protected MoreCollectors() {
            // for extention.
        }
    }
}
