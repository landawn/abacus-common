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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.stream.Stream;

/**
 * This class represents a Builder that can hold a value of type {@code T}.
 * It provides methods to perform operations on the value it holds, such as mapping, filtering, and applying functions.
 * The class also includes nested classes for building specific types of objects, such as lists, maps, and data sets.
 *
 * @param <T> The type of the value this Builder can hold.
 *
 * @see ImmutableList#builder()
 * @see ImmutableSet#builder()
 */
@SuppressWarnings({ "java:S6539" })
public class Builder<T> {

    final T val;

    Builder(final T val) {
        N.checkArgNotNull(val);

        this.val = val;
    }

    /**
     *
     * @param val
     * @return
     * @throws IllegalArgumentException if the specified {@code val} is {@code null}.
     */
    public static final BooleanListBuilder of(final BooleanList val) throws IllegalArgumentException {
        return new BooleanListBuilder(val);
    }

    /**
     *
     * @param val
     * @return
     * @throws IllegalArgumentException if the specified {@code val} is {@code null}.
     */
    public static final CharListBuilder of(final CharList val) throws IllegalArgumentException {
        return new CharListBuilder(val);
    }

    /**
     *
     * @param val
     * @return
     * @throws IllegalArgumentException if the specified {@code val} is {@code null}.
     */
    public static final ByteListBuilder of(final ByteList val) throws IllegalArgumentException {
        return new ByteListBuilder(val);
    }

    /**
     *
     * @param val
     * @return
     * @throws IllegalArgumentException if the specified {@code val} is {@code null}.
     */
    public static final ShortListBuilder of(final ShortList val) throws IllegalArgumentException {
        return new ShortListBuilder(val);
    }

    /**
     *
     * @param val
     * @return
     * @throws IllegalArgumentException if the specified {@code val} is {@code null}.
     */
    public static final IntListBuilder of(final IntList val) throws IllegalArgumentException {
        return new IntListBuilder(val);
    }

    /**
     *
     * @param val
     * @return
     * @throws IllegalArgumentException if the specified {@code val} is {@code null}.
     */
    public static final LongListBuilder of(final LongList val) throws IllegalArgumentException {
        return new LongListBuilder(val);
    }

    /**
     *
     * @param val
     * @return
     * @throws IllegalArgumentException if the specified {@code val} is {@code null}.
     */
    public static final FloatListBuilder of(final FloatList val) throws IllegalArgumentException {
        return new FloatListBuilder(val);
    }

    /**
     *
     * @param val
     * @return
     * @throws IllegalArgumentException if the specified {@code val} is {@code null}.
     */
    public static final DoubleListBuilder of(final DoubleList val) throws IllegalArgumentException {
        return new DoubleListBuilder(val);
    }

    /**
     *
     * @param <T>
     * @param <L>
     * @param val
     * @return
     * @throws IllegalArgumentException if the specified {@code val} is {@code null}.
     */
    public static final <T, L extends List<T>> ListBuilder<T, L> of(final L val) throws IllegalArgumentException {
        return new ListBuilder<>(val);
    }

    /**
     *
     * @param <T>
     * @param <C>
     * @param val
     * @return
     * @throws IllegalArgumentException if the specified {@code val} is {@code null}.
     */
    public static final <T, C extends Collection<T>> CollectionBuilder<T, C> of(final C val) throws IllegalArgumentException {
        return new CollectionBuilder<>(val);
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <M>
     * @param val
     * @return
     * @throws IllegalArgumentException if the specified {@code val} is {@code null}.
     */
    public static final <K, V, M extends Map<K, V>> MapBuilder<K, V, M> of(final M val) throws IllegalArgumentException {
        return new MapBuilder<>(val);
    }

    /**
     *
     * @param <T>
     * @param val
     * @return
     * @throws IllegalArgumentException if the specified {@code val} is {@code null}.
     */
    public static final <T> MultisetBuilder<T> of(final Multiset<T> val) throws IllegalArgumentException {
        return new MultisetBuilder<>(val);
    }

    /**
     *
     * @param <K> the key type
     * @param <E>
     * @param <V> the value type
     * @param <M>
     * @param val
     * @return
     * @throws IllegalArgumentException if the specified {@code val} is {@code null}.
     */
    public static final <K, E, V extends Collection<E>, M extends Multimap<K, E, V>> MultimapBuilder<K, E, V, M> of(final M val)
            throws IllegalArgumentException {
        return new MultimapBuilder<>(val);
    }

    /**
     *
     *
     * @param val
     * @return
     * @throws IllegalArgumentException if the specified {@code val} is {@code null}.
     */
    public static final DataSetBuilder of(final DataSet val) throws IllegalArgumentException {
        return new DataSetBuilder(val);
    }

    @SuppressWarnings("rawtypes")
    private static final Map<Class<?>, Function<Object, Builder>> creatorMap = new HashMap<>();

    static {
        initCreatorMap();
    }

    @SuppressWarnings("rawtypes")
    private static void initCreatorMap() {
        creatorMap.put(BooleanList.class, val -> Builder.of((BooleanList) val));
        creatorMap.put(CharList.class, val -> Builder.of((CharList) val));
        creatorMap.put(ByteList.class, val -> Builder.of((ByteList) val));
        creatorMap.put(ShortList.class, val -> Builder.of((ShortList) val));
        creatorMap.put(IntList.class, val -> Builder.of((IntList) val));
        creatorMap.put(LongList.class, val -> Builder.of((LongList) val));
        creatorMap.put(FloatList.class, val -> Builder.of((FloatList) val));
        creatorMap.put(DoubleList.class, val -> Builder.of((DoubleList) val));

        creatorMap.put(List.class, val -> Builder.of((List) val));
        creatorMap.put(ArrayList.class, val -> Builder.of((List) val));
        creatorMap.put(LinkedList.class, val -> Builder.of((List) val));

        creatorMap.put(Set.class, val -> Builder.of((Collection) val));
        creatorMap.put(HashSet.class, val -> Builder.of((Collection) val));
        creatorMap.put(LinkedHashSet.class, val -> Builder.of((Collection) val));

        creatorMap.put(Map.class, val -> Builder.of((Map) val));
        creatorMap.put(HashMap.class, val -> Builder.of((Map) val));
        creatorMap.put(LinkedHashMap.class, val -> Builder.of((Map) val));
        creatorMap.put(TreeMap.class, val -> Builder.of((Map) val));

        creatorMap.put(Multiset.class, val -> Builder.of((Multiset) val));

        creatorMap.put(Multimap.class, val -> Builder.of((Multimap) val));
        creatorMap.put(ListMultimap.class, val -> Builder.of((Multimap) val));
        creatorMap.put(SetMultimap.class, val -> Builder.of((Multimap) val));

        creatorMap.put(DataSet.class, val -> Builder.of((DataSet) val));
        creatorMap.put(RowDataSet.class, val -> Builder.of((DataSet) val));
    }

    /**
     *
     * @param <T>
     * @param val
     * @return
     * @throws IllegalArgumentException if the specified {@code val} is {@code null}.
     */
    @SuppressWarnings("rawtypes")
    public static final <T> Builder<T> of(final T val) throws IllegalArgumentException {
        N.checkArgNotNull(val);

        final Function<Object, Builder> func = creatorMap.get(val.getClass());

        if (func != null) {
            return func.apply(val);
        }

        Builder result = null;

        if (val instanceof List) {
            result = of((List) val);
        } else if (val instanceof Collection) {
            result = of((Collection) val);
        } else if (val instanceof Map) {
            result = of((Map) val);
        } else if (val instanceof Multimap) {
            result = of((Multimap) val);
        } else if (val instanceof DataSet) {
            result = of((DataSet) val);
        } else if (val instanceof Multiset) {
            result = of((Multiset) val);
        } else {
            result = new Builder<>(val);
        }

        return result;
    }

    //    public static <T> Builder<T> get(final Supplier<T> supplier) {
    //        return new Builder<>(supplier.get());
    //    }

    /**
     *
     *
     * @return
     */
    public T val() {
        return val;
    }

    /**
     *
     *
     * @param <R>
     * @param mapper
     * @return
     * @deprecated
     */
    @Deprecated
    public <R> Builder<R> map(final Function<? super T, ? extends R> mapper) {
        return of(mapper.apply(val));
    }

    /**
     *
     *
     * @param predicate
     * @return {@code Optional} with the value if {@code predicate} returns {@code true},
     * otherwise, return an empty {@code Optional}
     * @deprecated
     */
    @Deprecated
    public Optional<T> filter(final Predicate<? super T> predicate) {
        return predicate.test(val) ? Optional.of(val) : Optional.<T> empty();
    }

    /**
     *
     *
     * @param consumer
     * @return
     */
    public Builder<T> accept(final Consumer<? super T> consumer) {
        consumer.accept(val);

        return this;
    }

    /**
     *
     *
     * @param <R>
     * @param func
     * @return
     */
    public <R> R apply(final Function<? super T, ? extends R> func) {
        return func.apply(val);
    }

    /**
     *
     *
     * @return
     */
    public Stream<T> stream() {
        return Stream.of(val);
    }

    //    /**
    //    * Returns an empty <code>Nullable</code> if {@code val()} is {@code null} while {@code targetType} is primitive or can't be assigned to {@code targetType}.
    //    * Please be aware that {@code null} can be assigned to any {@code Object} type except primitive types: {@code boolean/char/byte/short/int/long/double}.
    //    *
    //    * @param val
    //    * @param targetType
    //    * @return
    //    */
    //    @SuppressWarnings("unchecked")
    //    public <TT> Nullable<TT> castIfAssignable(final Class<TT> targetType) {
    //        if (N.isPrimitive(targetType)) {
    //            return val != null && N.wrapperOf(targetType).isAssignableFrom(val.getClass()) ? Nullable.of((TT) val) : Nullable.<TT> empty();
    //        }
    //
    //        return val == null || targetType.isAssignableFrom(val.getClass()) ? Nullable.of((TT) val) : Nullable.<TT> empty();
    //    }
    //
    //    /**
    //     *
    //     * @param b
    //     * @param actionForTrue do nothing if it's {@code null} even {@code b} is true.
    //     * @param actionForFalse do nothing if it's {@code null} even {@code b} is false.
    //     * @throws E1
    //     * @throws E2
    //     */
    //    public <E1 extends Exception, E2 extends Exception> void ifOrElse(final boolean b, final Try.Consumer<? super T, E1> actionForTrue,
    //            final Try.Consumer<? super T, E2> actionForFalse) throws E1, E2 {
    //        if (b) {
    //            if (actionForTrue != null) {
    //                actionForTrue.accept(val);
    //            }
    //        } else {
    //            if (actionForFalse != null) {
    //                actionForFalse.accept(val);
    //            }
    //        }
    //    }
    //
    //    /**
    //     *
    //     * @param predicate
    //     * @param actionForTrue do nothing if it's {@code null} even {@code b} is true.
    //     * @param actionForFalse do nothing if it's {@code null} even {@code b} is false.
    //     * @throws E0
    //     * @throws E1
    //     * @throws E2
    //     */
    //    public <E0 extends Exception, E1 extends Exception, E2 extends Exception> void ifOrElse(final Try.Predicate<? super T, E0> predicate,
    //            final Try.Consumer<? super T, E1> actionForTrue, final Try.Consumer<? super T, E2> actionForFalse) throws E0, E1, E2 {
    //        if (predicate.test(val)) {
    //            if (actionForTrue != null) {
    //                actionForTrue.accept(val);
    //            }
    //        } else {
    //            if (actionForFalse != null) {
    //                actionForFalse.accept(val);
    //            }
    //        }
    //    }
    //
    //    /**
    //     * Returns an empty {@code Optional} if {@code cmd} is executed successfully, otherwise a {@code Optional} with the exception threw.
    //     *
    //     * @param cmd
    //     * @return
    //     */
    //    public Optional<Exception> run(final Try.Consumer<? super T, ? extends Exception> cmd) {
    //        try {
    //            cmd.accept(val);
    //            return Optional.empty();
    //        } catch (Exception e) {
    //            return Optional.of(e);
    //        }
    //    }
    //
    //    /**
    //     * Returns a {@code Pair} with {@code left=returnedValue, right=null} if {@code cmd} is executed successfully, otherwise a {@code Pair} with {@code left=null, right=exception}.
    //     *
    //     * @param cmd
    //     * @return
    //     */
    //    public <R> Pair<R, Exception> call(final Try.Function<? super T, R, ? extends Exception> cmd) {
    //        try {
    //            return Pair.of(cmd.apply(val), null);
    //        } catch (Exception e) {
    //            return Pair.of(null, e);
    //        }
    //    }
    //
    //    /**
    //     * Returns a {@code Nullable} with the value returned by {@code action} or an empty {@code Nullable} if exception happens.
    //     *
    //     * @param cmd
    //     * @return
    //     */
    //    public <R> Nullable<R> tryOrEmpty(final Try.Function<? super T, R, ? extends Exception> cmd) {
    //        try {
    //            return Nullable.of(cmd.apply(val));
    //        } catch (Exception e) {
    //            return Nullable.<R> empty();
    //        }
    //    }

    /**
     * The Class BooleanListBuilder.
     */
    public static final class BooleanListBuilder extends Builder<BooleanList> {

        /**
         * Instantiates a new boolean list builder.
         *
         * @param val
         */
        BooleanListBuilder(final BooleanList val) {
            super(val);
        }

        /**
         *
         * @param index
         * @param e
         * @return
         */
        public BooleanListBuilder set(final int index, final boolean e) {
            val.set(index, e);

            return this;
        }

        /**
         *
         * @param e
         * @return
         */
        public BooleanListBuilder add(final boolean e) {
            val.add(e);

            return this;
        }

        /**
         *
         * @param index
         * @param e
         * @return
         */
        public BooleanListBuilder add(final int index, final boolean e) {
            val.add(index, e);

            return this;
        }

        /**
         * Adds the all.
         *
         * @param c
         * @return
         */
        public BooleanListBuilder addAll(final BooleanList c) {
            val.addAll(c);

            return this;
        }

        /**
         * Adds the all.
         *
         * @param index
         * @param c
         * @return
         */
        public BooleanListBuilder addAll(final int index, final BooleanList c) {
            val.addAll(index, c);

            return this;
        }

        /**
         *
         * @param e
         * @return
         */
        public BooleanListBuilder remove(final boolean e) {
            val.remove(e);

            return this;
        }

        //        public BooleanListBuilder removeAllOccurrences(double e) {
        //            value.removeAllOccurrences(e);
        //
        //            return this;
        //        }

        /**
         * Removes the all.
         *
         * @param c
         * @return
         */
        public BooleanListBuilder removeAll(final BooleanList c) {
            val.removeAll(c);

            return this;
        }
    }

    /**
     * The Class CharListBuilder.
     */
    public static final class CharListBuilder extends Builder<CharList> {

        /**
         * Instantiates a new char list builder.
         *
         * @param val
         */
        CharListBuilder(final CharList val) {
            super(val);
        }

        /**
         *
         * @param index
         * @param e
         * @return
         */
        public CharListBuilder set(final int index, final char e) {
            val.set(index, e);

            return this;
        }

        /**
         *
         * @param e
         * @return
         */
        public CharListBuilder add(final char e) {
            val.add(e);

            return this;
        }

        /**
         *
         * @param index
         * @param e
         * @return
         */
        public CharListBuilder add(final int index, final char e) {
            val.add(index, e);

            return this;
        }

        /**
         * Adds the all.
         *
         * @param c
         * @return
         */
        public CharListBuilder addAll(final CharList c) {
            val.addAll(c);

            return this;
        }

        /**
         * Adds the all.
         *
         * @param index
         * @param c
         * @return
         */
        public CharListBuilder addAll(final int index, final CharList c) {
            val.addAll(index, c);

            return this;
        }

        /**
         *
         * @param e
         * @return
         */
        public CharListBuilder remove(final char e) {
            val.remove(e);

            return this;
        }

        //        public CharListBuilder removeAllOccurrences(double e) {
        //            value.removeAllOccurrences(e);
        //
        //            return this;
        //        }

        /**
         * Removes the all.
         *
         * @param c
         * @return
         */
        public CharListBuilder removeAll(final CharList c) {
            val.removeAll(c);

            return this;
        }
    }

    /**
     * The Class ByteListBuilder.
     */
    public static final class ByteListBuilder extends Builder<ByteList> {

        /**
         * Instantiates a new byte list builder.
         *
         * @param val
         */
        ByteListBuilder(final ByteList val) {
            super(val);
        }

        /**
         *
         * @param index
         * @param e
         * @return
         */
        public ByteListBuilder set(final int index, final byte e) {
            val.set(index, e);

            return this;
        }

        /**
         *
         * @param e
         * @return
         */
        public ByteListBuilder add(final byte e) {
            val.add(e);

            return this;
        }

        /**
         *
         * @param index
         * @param e
         * @return
         */
        public ByteListBuilder add(final int index, final byte e) {
            val.add(index, e);

            return this;
        }

        /**
         * Adds the all.
         *
         * @param c
         * @return
         */
        public ByteListBuilder addAll(final ByteList c) {
            val.addAll(c);

            return this;
        }

        /**
         * Adds the all.
         *
         * @param index
         * @param c
         * @return
         */
        public ByteListBuilder addAll(final int index, final ByteList c) {
            val.addAll(index, c);

            return this;
        }

        /**
         *
         * @param e
         * @return
         */
        public ByteListBuilder remove(final byte e) {
            val.remove(e);

            return this;
        }

        //        public ByteListBuilder removeAllOccurrences(double e) {
        //            value.removeAllOccurrences(e);
        //
        //            return this;
        //        }

        /**
         * Removes the all.
         *
         * @param c
         * @return
         */
        public ByteListBuilder removeAll(final ByteList c) {
            val.removeAll(c);

            return this;
        }
    }

    /**
     * The Class ShortListBuilder.
     */
    public static final class ShortListBuilder extends Builder<ShortList> {

        /**
         * Instantiates a new short list builder.
         *
         * @param val
         */
        ShortListBuilder(final ShortList val) {
            super(val);
        }

        /**
         *
         * @param index
         * @param e
         * @return
         */
        public ShortListBuilder set(final int index, final short e) {
            val.set(index, e);

            return this;
        }

        /**
         *
         * @param e
         * @return
         */
        public ShortListBuilder add(final short e) {
            val.add(e);

            return this;
        }

        /**
         *
         * @param index
         * @param e
         * @return
         */
        public ShortListBuilder add(final int index, final short e) {
            val.add(index, e);

            return this;
        }

        /**
         * Adds the all.
         *
         * @param c
         * @return
         */
        public ShortListBuilder addAll(final ShortList c) {
            val.addAll(c);

            return this;
        }

        /**
         * Adds the all.
         *
         * @param index
         * @param c
         * @return
         */
        public ShortListBuilder addAll(final int index, final ShortList c) {
            val.addAll(index, c);

            return this;
        }

        /**
         *
         * @param e
         * @return
         */
        public ShortListBuilder remove(final short e) {
            val.remove(e);

            return this;
        }

        //        public ShortListBuilder removeAllOccurrences(double e) {
        //            value.removeAllOccurrences(e);
        //
        //            return this;
        //        }

        /**
         * Removes the all.
         *
         * @param c
         * @return
         */
        public ShortListBuilder removeAll(final ShortList c) {
            val.removeAll(c);

            return this;
        }
    }

    /**
     * The Class IntListBuilder.
     */
    public static final class IntListBuilder extends Builder<IntList> {

        /**
         * Instantiates a new int list builder.
         *
         * @param val
         */
        IntListBuilder(final IntList val) {
            super(val);
        }

        /**
         *
         * @param index
         * @param e
         * @return
         */
        public IntListBuilder set(final int index, final int e) {
            val.set(index, e);

            return this;
        }

        /**
         *
         * @param e
         * @return
         */
        public IntListBuilder add(final int e) {
            val.add(e);

            return this;
        }

        /**
         *
         * @param index
         * @param e
         * @return
         */
        public IntListBuilder add(final int index, final int e) {
            val.add(index, e);

            return this;
        }

        /**
         * Adds the all.
         *
         * @param c
         * @return
         */
        public IntListBuilder addAll(final IntList c) {
            val.addAll(c);

            return this;
        }

        /**
         * Adds the all.
         *
         * @param index
         * @param c
         * @return
         */
        public IntListBuilder addAll(final int index, final IntList c) {
            val.addAll(index, c);

            return this;
        }

        /**
         *
         * @param e
         * @return
         */
        public IntListBuilder remove(final int e) {
            val.remove(e);

            return this;
        }

        //        public IntListBuilder removeAllOccurrences(double e) {
        //            value.removeAllOccurrences(e);
        //
        //            return this;
        //        }

        /**
         * Removes the all.
         *
         * @param c
         * @return
         */
        public IntListBuilder removeAll(final IntList c) {
            val.removeAll(c);

            return this;
        }
    }

    /**
     * The Class LongListBuilder.
     */
    public static final class LongListBuilder extends Builder<LongList> {

        /**
         * Instantiates a new long list builder.
         *
         * @param val
         */
        LongListBuilder(final LongList val) {
            super(val);
        }

        /**
         *
         * @param index
         * @param e
         * @return
         */
        public LongListBuilder set(final int index, final long e) {
            val.set(index, e);

            return this;
        }

        /**
         *
         * @param e
         * @return
         */
        public LongListBuilder add(final long e) {
            val.add(e);

            return this;
        }

        /**
         *
         * @param index
         * @param e
         * @return
         */
        public LongListBuilder add(final int index, final long e) {
            val.add(index, e);

            return this;
        }

        /**
         * Adds the all.
         *
         * @param c
         * @return
         */
        public LongListBuilder addAll(final LongList c) {
            val.addAll(c);

            return this;
        }

        /**
         * Adds the all.
         *
         * @param index
         * @param c
         * @return
         */
        public LongListBuilder addAll(final int index, final LongList c) {
            val.addAll(index, c);

            return this;
        }

        /**
         *
         * @param e
         * @return
         */
        public LongListBuilder remove(final long e) {
            val.remove(e);

            return this;
        }

        //        public LongListBuilder removeAllOccurrences(double e) {
        //            value.removeAllOccurrences(e);
        //
        //            return this;
        //        }

        /**
         * Removes the all.
         *
         * @param c
         * @return
         */
        public LongListBuilder removeAll(final LongList c) {
            val.removeAll(c);

            return this;
        }
    }

    /**
     * The Class FloatListBuilder.
     */
    public static final class FloatListBuilder extends Builder<FloatList> {

        /**
         * Instantiates a new float list builder.
         *
         * @param val
         */
        FloatListBuilder(final FloatList val) {
            super(val);
        }

        /**
         *
         * @param index
         * @param e
         * @return
         */
        public FloatListBuilder set(final int index, final float e) {
            val.set(index, e);

            return this;
        }

        /**
         *
         * @param e
         * @return
         */
        public FloatListBuilder add(final float e) {
            val.add(e);

            return this;
        }

        /**
         *
         * @param index
         * @param e
         * @return
         */
        public FloatListBuilder add(final int index, final float e) {
            val.add(index, e);

            return this;
        }

        /**
         * Adds the all.
         *
         * @param c
         * @return
         */
        public FloatListBuilder addAll(final FloatList c) {
            val.addAll(c);

            return this;
        }

        /**
         * Adds the all.
         *
         * @param index
         * @param c
         * @return
         */
        public FloatListBuilder addAll(final int index, final FloatList c) {
            val.addAll(index, c);

            return this;
        }

        /**
         *
         * @param e
         * @return
         */
        public FloatListBuilder remove(final float e) {
            val.remove(e);

            return this;
        }

        //        public FloatListBuilder removeAllOccurrences(double e) {
        //            value.removeAllOccurrences(e);
        //
        //            return this;
        //        }

        /**
         * Removes the all.
         *
         * @param c
         * @return
         */
        public FloatListBuilder removeAll(final FloatList c) {
            val.removeAll(c);

            return this;
        }
    }

    /**
     * The Class DoubleListBuilder.
     */
    public static final class DoubleListBuilder extends Builder<DoubleList> {

        /**
         * Instantiates a new double list builder.
         *
         * @param val
         */
        DoubleListBuilder(final DoubleList val) {
            super(val);
        }

        /**
         *
         * @param index
         * @param e
         * @return
         */
        public DoubleListBuilder set(final int index, final double e) {
            val.set(index, e);

            return this;
        }

        /**
         *
         * @param e
         * @return
         */
        public DoubleListBuilder add(final double e) {
            val.add(e);

            return this;
        }

        /**
         *
         * @param index
         * @param e
         * @return
         */
        public DoubleListBuilder add(final int index, final double e) {
            val.add(index, e);

            return this;
        }

        /**
         * Adds the all.
         *
         * @param c
         * @return
         */
        public DoubleListBuilder addAll(final DoubleList c) {
            val.addAll(c);

            return this;
        }

        /**
         * Adds the all.
         *
         * @param index
         * @param c
         * @return
         */
        public DoubleListBuilder addAll(final int index, final DoubleList c) {
            val.addAll(index, c);

            return this;
        }

        /**
         *
         * @param e
         * @return
         */
        public DoubleListBuilder remove(final double e) {
            val.remove(e);

            return this;
        }

        //        public DoubleListBuilder removeAllOccurrences(double e) {
        //            value.removeAllOccurrences(e);
        //
        //            return this;
        //        }

        /**
         * Removes the all.
         *
         * @param c
         * @return
         */
        public DoubleListBuilder removeAll(final DoubleList c) {
            val.removeAll(c);

            return this;
        }
    }

    /**
     * The Class ListBuilder.
     *
     * @param <T>
     * @param <L>
     */
    public static final class ListBuilder<T, L extends List<T>> extends CollectionBuilder<T, L> {

        /**
         * Instantiates a new list builder.
         *
         * @param c
         */
        ListBuilder(final L c) {
            super(c);
        }

        /**
         *
         * @param index
         * @param e
         * @return
         */
        public ListBuilder<T, L> add(final int index, final T e) {
            val.add(index, e);

            return this;
        }

        /**
         * Adds the all.
         *
         * @param index
         * @param c
         * @return
         * @throws IndexOutOfBoundsException
         */
        public ListBuilder<T, L> addAll(final int index, final Collection<? extends T> c) throws IndexOutOfBoundsException {
            N.checkIndex(index, val.size());

            if (N.notEmpty(c)) {
                val.addAll(index, c);
            }

            return this;
        }

        /**
         *
         * @param index
         * @return
         */
        public ListBuilder<T, L> remove(final int index) {
            val.remove(index);

            return this;
        }
    }

    /**
     * The Class CollectionBuilder.
     *
     * @param <T>
     * @param <C>
     */
    public static class CollectionBuilder<T, C extends Collection<T>> extends Builder<C> {

        /**
         * Instantiates a new collection builder.
         *
         * @param c
         */
        CollectionBuilder(final C c) {
            super(c);
        }

        /**
         *
         * @param e
         * @return
         */
        public CollectionBuilder<T, C> add(final T e) {
            val.add(e);

            return this;
        }

        /**
         * Adds the all.
         *
         * @param c
         * @return
         */
        public CollectionBuilder<T, C> addAll(final Collection<? extends T> c) {
            if (N.notEmpty(c)) {
                val.addAll(c);
            }

            return this;
        }

        /**
         * Adds the all.
         *
         * @param a
         * @return
         */
        public CollectionBuilder<T, C> addAll(final T... a) {
            if (N.notEmpty(a)) {
                val.addAll(Arrays.asList(a));
            }

            return this;
        }

        /**
         *
         * @param e
         * @return
         */
        public CollectionBuilder<T, C> remove(final Object e) {
            val.remove(e);

            return this;
        }

        /**
         * Removes the all.
         *
         * @param c
         * @return
         */
        public CollectionBuilder<T, C> removeAll(final Collection<?> c) {
            if (N.notEmpty(c)) {
                val.removeAll(c);
            }

            return this;
        }

        /**
         * Removes the all.
         *
         * @param a
         * @return
         */
        public CollectionBuilder<T, C> removeAll(final T... a) {
            if (N.notEmpty(a)) {
                val.removeAll(Arrays.asList(a));
            }

            return this;
        }
    }

    /**
     * The Class MultisetBuilder.
     *
     * @param <T>
     */
    public static final class MultisetBuilder<T> extends Builder<Multiset<T>> {

        /**
         * Instantiates a new multiset builder.
         *
         * @param c
         */
        MultisetBuilder(final Multiset<T> c) {
            super(c);
        }

        /**
         *
         * @param e
         * @param occurrences
         * @return
         */
        public MultisetBuilder<T> setCount(final T e, final int occurrences) {
            val.setCount(e, occurrences);

            return this;
        }

        /**
         *
         * @param e
         * @return
         */
        public MultisetBuilder<T> add(final T e) {
            val.add(e);

            return this;
        }

        /**
         *
         * @param e
         * @param occurrencesToAdd
         * @return
         */
        public MultisetBuilder<T> add(final T e, final int occurrencesToAdd) {
            val.add(e, occurrencesToAdd);

            return this;
        }

        /**
         *
         * @param e
         * @return
         */
        public MultisetBuilder<T> remove(final Object e) {
            val.remove(e);

            return this;
        }

        /**
         *
         * @param e
         * @param occurrencesToAdd
         * @return
         */
        public MultisetBuilder<T> remove(final Object e, final int occurrencesToAdd) {
            val.remove(e, occurrencesToAdd);

            return this;
        }

        /**
         * Removes the all.
         *
         * @param c
         * @return
         * @see #removeAllOccurrences(Collection)
         * @deprecated Use {@link #removeAllOccurrences(Collection<?>)} instead
         */
        @Deprecated
        public MultisetBuilder<T> removeAll(final Collection<?> c) {
            return removeAllOccurrences(c);
        }

        /**
         *
         * @param e
         * @return
         */
        public MultisetBuilder<T> removeAllOccurrences(final Object e) {
            val.removeAllOccurrences(e);

            return this;
        }

        /**
         *
         * @param c
         * @return
         */
        public MultisetBuilder<T> removeAllOccurrences(final Collection<?> c) {
            val.removeAllOccurrences(c);

            return this;
        }
    }

    /**
     * The Class MapBuilder.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <M>
     */
    public static final class MapBuilder<K, V, M extends Map<K, V>> extends Builder<M> {

        /**
         * Instantiates a new map builder.
         *
         * @param m
         */
        MapBuilder(final M m) {
            super(m);
        }

        /**
         *
         * @param k
         * @param v
         * @return
         */
        public MapBuilder<K, V, M> put(final K k, final V v) {
            val.put(k, v);

            return this;
        }

        /**
         *
         * @param m
         * @return
         */
        public MapBuilder<K, V, M> putAll(final Map<? extends K, ? extends V> m) {
            if (N.notEmpty(m)) {
                val.putAll(m);
            }

            return this;
        }

        /**
         * Put if absent or the associated value is {@code null}
         *
         * <br />
         * Absent -> key is not found in the specified map or found with {@code null} value.
         *
         * @param key
         * @param value
         * @return
         * @see Map#putIfAbsent(Object, Object)
         */
        public MapBuilder<K, V, M> putIfAbsent(final K key, final V value) {
            final V v = val.get(key);

            // if (v == null && val.containsKey(key) == false) {
            if (v == null) {
                val.put(key, value);
            }

            return this;
        }

        /**
         * Put if absent or the associated value is {@code null
         *
         * <br />
         * Absent -> key is not found in the specified map or found with {@code null} value.
         *
         * @param key
         * @param supplier
         * @return
         * @see Map#putIfAbsent(Object, Object)
         */
        public MapBuilder<K, V, M> putIfAbsent(final K key, final Supplier<V> supplier) {
            final V v = val.get(key);

            // if (v == null && val.containsKey(key) == false) {
            if (v == null) {
                val.put(key, supplier.get());
            }

            return this;
        }

        /**
         *
         * @param k
         * @return
         */
        public MapBuilder<K, V, M> remove(final Object k) {
            val.remove(k);

            return this;
        }

        /**
         * Removes the all.
         *
         * @param keysToRemove
         * @return
         */
        public MapBuilder<K, V, M> removeAll(final Collection<?> keysToRemove) {
            if (N.notEmpty(keysToRemove)) {
                for (final Object k : keysToRemove) {
                    val.remove(k);
                }
            }

            return this;
        }
    }

    /**
     * The Class MultimapBuilder.
     *
     * @param <K> the key type
     * @param <E>
     * @param <V> the value type
     * @param <M>
     */
    public static final class MultimapBuilder<K, E, V extends Collection<E>, M extends Multimap<K, E, V>> extends Builder<M> {

        /**
         * Instantiates a new multimap builder.
         *
         * @param m
         */
        MultimapBuilder(final M m) {
            super(m);
        }

        /**
         *
         * @param key
         * @param e
         * @return
         */
        public MultimapBuilder<K, E, V, M> put(final K key, final E e) {
            val.put(key, e);

            return this;
        }

        /**
         *
         * @param m
         * @return
         */
        public MultimapBuilder<K, E, V, M> put(final Map<? extends K, ? extends E> m) {
            val.put(m);

            return this;
        }

        /**
         *
         * @param k
         * @param c
         * @return
         */
        public MultimapBuilder<K, E, V, M> putMany(final K k, final Collection<? extends E> c) {
            val.putMany(k, c);

            return this;
        }

        /**
         *
         * @param m
         * @return
         */
        public MultimapBuilder<K, E, V, M> putMany(final Map<? extends K, ? extends Collection<? extends E>> m) {
            val.putMany(m);

            return this;
        }

        /**
         *
         * @param m
         * @return
         */
        public MultimapBuilder<K, E, V, M> putMany(final Multimap<? extends K, ? extends E, ? extends V> m) {
            val.putMany(m);

            return this;
        }

        /**
         *
         * @param k
         * @param e
         * @return
         */
        public MultimapBuilder<K, E, V, M> removeOne(final Object k, final Object e) {
            val.removeOne(k, e);

            return this;
        }

        /**
         *
         * @param m
         * @return
         */
        public MultimapBuilder<K, E, V, M> removeOne(final Map<? extends K, ? extends E> m) {
            val.removeOne(m);

            return this;
        }

        /**
         *
         * @param k
         * @return
         */
        public MultimapBuilder<K, E, V, M> removeAll(final Object k) {
            val.removeAll(k);

            return this;
        }

        /**
         *
         * @param k
         * @param valuesToRemove
         * @return
         */
        public MultimapBuilder<K, E, V, M> removeMany(final Object k, final Collection<?> valuesToRemove) {
            val.removeMany(k, valuesToRemove);

            return this;
        }

        /**
         *
         * @param m
         * @return
         */
        public MultimapBuilder<K, E, V, M> removeMany(final Map<?, ? extends Collection<?>> m) {
            val.removeMany(m);

            return this;
        }

        /**
         * Removes the all.
         *
         * @param m
         * @return
         */
        public MultimapBuilder<K, E, V, M> removeMany(final Multimap<?, ?, ?> m) {
            val.removeMany(m);

            return this;
        }
    }

    /**
     * The Class DataSetBuilder.
     */
    public static final class DataSetBuilder extends Builder<DataSet> {

        /**
         * Instantiates a new data set builder.
         *
         * @param ds
         */
        DataSetBuilder(final DataSet ds) {
            super(ds);
        }

        /**
         *
         * @param columnName
         * @param newColumnName
         * @return
         */
        public DataSetBuilder renameColumn(final String columnName, final String newColumnName) {
            val.renameColumn(columnName, newColumnName);

            return this;
        }

        /**
         *
         * @param oldNewNames
         * @return
         */
        public DataSetBuilder renameColumns(final Map<String, String> oldNewNames) {
            val.renameColumns(oldNewNames);

            return this;
        }

        //        /**
        //         *
        //         * @param columnName
        //         * @param func
        //         * @return
        //         */
        //        public DataSetBuilder renameColumn(String columnName, Function<? super String, String> func) {
        //            val.renameColumn(columnName, func);
        //
        //            return this;
        //        }

        /**
         *
         * @param columnNames
         * @param func
         * @return
         */
        public DataSetBuilder renameColumns(final Collection<String> columnNames, final Function<? super String, String> func) {
            val.renameColumns(columnNames, func);

            return this;
        }

        /**
         *
         * @param func
         * @return
         */
        public DataSetBuilder renameColumns(final Function<? super String, String> func) {
            val.renameColumns(func);

            return this;
        }

        /**
         * Adds the column.
         *
         * @param columnName
         * @param column
         * @return
         */
        public DataSetBuilder addColumn(final String columnName, final List<?> column) {
            val.addColumn(columnName, column);

            return this;
        }

        /**
         * Adds the column.
         *
         * @param columnIndex
         * @param columnName
         * @param column
         * @return
         */
        public DataSetBuilder addColumn(final int columnIndex, final String columnName, final List<?> column) {
            val.addColumn(columnIndex, columnName, column);

            return this;
        }

        /**
         * Adds the column.
         *
         * @param newColumnName
         * @param fromColumnName
         * @param func
         * @return
         */
        public DataSetBuilder addColumn(final String newColumnName, final String fromColumnName, final Function<?, ?> func) {
            val.addColumn(newColumnName, fromColumnName, func);

            return this;
        }

        /**
         * Adds the column.
         *
         * @param columnIndex
         * @param newColumnName
         * @param fromColumnName
         * @param func
         * @return
         */
        public DataSetBuilder addColumn(final int columnIndex, final String newColumnName, final String fromColumnName, final Function<?, ?> func) {
            val.addColumn(columnIndex, newColumnName, fromColumnName, func);

            return this;
        }

        /**
         * Adds the column.
         *
         * @param newColumnName
         * @param fromColumnNames
         * @param func
         * @return
         */
        public DataSetBuilder addColumn(final String newColumnName, final Collection<String> fromColumnNames,
                final Function<? super DisposableObjArray, ?> func) {
            val.addColumn(newColumnName, fromColumnNames, func);

            return this;
        }

        /**
         * Adds the column.
         *
         * @param columnIndex
         * @param newColumnName
         * @param fromColumnNames
         * @param func
         * @return
         */
        public DataSetBuilder addColumn(final int columnIndex, final String newColumnName, final Collection<String> fromColumnNames,
                final Function<? super DisposableObjArray, ?> func) {
            val.addColumn(columnIndex, newColumnName, fromColumnNames, func);

            return this;
        }

        /**
         * Adds the column.
         *
         * @param newColumnName
         * @param fromColumnNames
         * @param func
         * @return
         */
        public DataSetBuilder addColumn(final String newColumnName, final Tuple2<String, String> fromColumnNames, final BiFunction<?, ?, ?> func) {
            val.addColumn(newColumnName, fromColumnNames, func);

            return this;
        }

        /**
         * Adds the column.
         *
         * @param columnIndex
         * @param newColumnName
         * @param fromColumnNames
         * @param func
         * @return
         */
        public DataSetBuilder addColumn(final int columnIndex, final String newColumnName, final Tuple2<String, String> fromColumnNames,
                final BiFunction<?, ?, ?> func) {
            val.addColumn(columnIndex, newColumnName, fromColumnNames, func);

            return this;
        }

        /**
         * Adds the column.
         *
         * @param newColumnName
         * @param fromColumnNames
         * @param func
         * @return
         */
        public DataSetBuilder addColumn(final String newColumnName, final Tuple3<String, String, String> fromColumnNames, final TriFunction<?, ?, ?, ?> func) {
            val.addColumn(newColumnName, fromColumnNames, func);

            return this;
        }

        /**
         * Adds the column.
         *
         * @param columnIndex
         * @param newColumnName
         * @param fromColumnNames
         * @param func
         * @return
         */
        public DataSetBuilder addColumn(final int columnIndex, final String newColumnName, final Tuple3<String, String, String> fromColumnNames,
                final TriFunction<?, ?, ?, ?> func) {
            val.addColumn(columnIndex, newColumnName, fromColumnNames, func);

            return this;
        }

        /**
         * Removes the column.
         *
         * @param columnName
         * @return
         */
        public DataSetBuilder removeColumn(final String columnName) {
            val.removeColumn(columnName);

            return this;
        }

        /**
         * Removes the columns.
         *
         * @param columnNames
         * @return
         */
        public DataSetBuilder removeColumns(final Collection<String> columnNames) {
            val.removeColumns(columnNames);

            return this;
        }

        /**
         * Removes the columns.
         *
         * @param filter
         * @return
         */
        public DataSetBuilder removeColumns(final Predicate<? super String> filter) {
            val.removeColumns(filter);

            return this;
        }

        //        /**
        //         *
        //         * @param filter
        //         * @return
        //         * @deprecated replaced by {@code removeColumns}.
        //         */
        //        @Deprecated
        //        public DataSetBuilder removeColumnsIf(Predicate<? super String> filter) {
        //            val.removeColumnsIf(filter);
        //
        //            return this;
        //        }

        /**
         *
         *
         * @param columnName
         * @param func
         * @return
         */
        public DataSetBuilder updateColumn(final String columnName, final Function<?, ?> func) {
            val.updateColumn(columnName, func);

            return this;
        }

        /**
         *
         *
         * @param columnNames
         * @param func
         * @return
         */
        public DataSetBuilder updateColumns(final Collection<String> columnNames, final Function<?, ?> func) {
            val.updateColumns(columnNames, func);

            return this;
        }

        /**
         *
         * @param columnName
         * @param targetType
         * @return
         */
        public DataSetBuilder convertColumn(final String columnName, final Class<?> targetType) {
            val.convertColumn(columnName, targetType);

            return this;
        }

        /**
         *
         * @param columnTargetTypes
         * @return
         */
        public DataSetBuilder convertColumns(final Map<String, Class<?>> columnTargetTypes) {
            val.convertColumns(columnTargetTypes);

            return this;
        }

        /**
         *
         * @param columnNames
         * @param newColumnName
         * @param newColumnClass
         * @return
         */
        public DataSetBuilder combineColumns(final Collection<String> columnNames, final String newColumnName, final Class<?> newColumnClass) {
            val.combineColumns(columnNames, newColumnName, newColumnClass);

            return this;
        }

        /**
         *
         *
         * @param columnNames
         * @param newColumnName
         * @param combineFunc
         * @return
         */
        public DataSetBuilder combineColumns(final Collection<String> columnNames, final String newColumnName,
                final Function<? super DisposableObjArray, ?> combineFunc) {
            val.combineColumns(columnNames, newColumnName, combineFunc);

            return this;
        }

        /**
         *
         *
         * @param columnNames
         * @param newColumnName
         * @param combineFunc
         * @return
         */
        public DataSetBuilder combineColumns(final Tuple2<String, String> columnNames, final String newColumnName, final BiFunction<?, ?, ?> combineFunc) {
            val.combineColumns(columnNames, newColumnName, combineFunc);

            return this;
        }

        /**
         *
         *
         * @param columnNames
         * @param newColumnName
         * @param combineFunc
         * @return
         */
        public DataSetBuilder combineColumns(final Tuple3<String, String, String> columnNames, final String newColumnName,
                final TriFunction<?, ?, ?, ?> combineFunc) {
            val.combineColumns(columnNames, newColumnName, combineFunc);

            return this;
        }

        /**
         *
         * @param columnNameFilter
         * @param newColumnName
         * @param newColumnClass
         * @return
         */
        public DataSetBuilder combineColumns(final Predicate<? super String> columnNameFilter, final String newColumnName, final Class<?> newColumnClass) {
            val.combineColumns(columnNameFilter, newColumnName, newColumnClass);

            return this;
        }

        /**
         *
         *
         * @param columnNameFilter
         * @param newColumnName
         * @param combineFunc
         * @return
         */
        public DataSetBuilder combineColumns(final Predicate<? super String> columnNameFilter, final String newColumnName,
                final Function<? super DisposableObjArray, ?> combineFunc) {
            val.combineColumns(columnNameFilter, newColumnName, combineFunc);

            return this;
        }

        /**
         *
         *
         * @param columnName
         * @param newColumnNames
         * @param divideFunc
         * @return
         */
        public DataSetBuilder divideColumn(final String columnName, final Collection<String> newColumnNames, final Function<?, ? extends List<?>> divideFunc) {
            val.divideColumn(columnName, newColumnNames, divideFunc);

            return this;
        }

        /**
         *
         *
         * @param columnName
         * @param newColumnNames
         * @param output
         * @return
         */
        public DataSetBuilder divideColumn(final String columnName, final Collection<String> newColumnNames, final BiConsumer<?, Object[]> output) {
            val.divideColumn(columnName, newColumnNames, output);

            return this;
        }

        /**
         *
         *
         * @param columnName
         * @param newColumnNames
         * @param output
         * @return
         */
        public DataSetBuilder divideColumn(final String columnName, final Tuple2<String, String> newColumnNames,
                final BiConsumer<?, Pair<Object, Object>> output) {
            val.divideColumn(columnName, newColumnNames, output);

            return this;
        }

        /**
         *
         *
         * @param columnName
         * @param newColumnNames
         * @param output
         * @return
         */
        public DataSetBuilder divideColumn(final String columnName, final Tuple3<String, String, String> newColumnNames,
                final BiConsumer<?, Triple<Object, Object, Object>> output) {
            val.divideColumn(columnName, newColumnNames, output);

            return this;
        }

        /**
         *
         *
         * @param func
         * @return
         */
        public DataSetBuilder updateAll(final Function<?, ?> func) {
            val.updateAll(func);

            return this;
        }

        /**
         *
         *
         * @param predicate
         * @param newValue
         * @return
         */
        public DataSetBuilder replaceIf(final Predicate<?> predicate, final Object newValue) {
            val.replaceIf(predicate, newValue);

            return this;
        }

        /**
         * Prepend the specified {@code other} into this {@code DataSet}.
         * <br />
         * The columns of two {@code DataSet} must be same.
         *
         * @param other
         * @return
         * @see DataSet#prepend(DataSet)
         */
        public DataSetBuilder prepend(final DataSet other) {
            val.prepend(other);

            return this;
        }

        /**
         * Append the specified {@code other} into this {@code DataSet}.
         * <br />
         * The columns of two {@code DataSet} must be same.
         *
         * @param other
         * @return
         * @see DataSet#append(DataSet)
         */
        public DataSetBuilder append(final DataSet other) {
            val.append(other);

            return this;
        }

        /**
         *
         * @param columnName
         * @return
         */
        public DataSetBuilder sortBy(final String columnName) {
            val.sortBy(columnName);

            return this;
        }

        /**
         *
         * @param <T>
         * @param columnName
         * @param cmp
         * @return
         */
        public <T> DataSetBuilder sortBy(final String columnName, final Comparator<T> cmp) {
            val.sortBy(columnName, cmp);

            return this;
        }

        /**
         *
         * @param columnNames
         * @return
         */
        public DataSetBuilder sortBy(final Collection<String> columnNames) {
            val.sortBy(columnNames);

            return this;
        }

        /**
         *
         * @param columnNames
         * @param cmp
         * @return
         */
        public DataSetBuilder sortBy(final Collection<String> columnNames, final Comparator<? super Object[]> cmp) {
            val.sortBy(columnNames, cmp);

            return this;
        }

        /**
         *
         * @param columnNames
         * @param keyMapper
         * @return
         */
        @SuppressWarnings("rawtypes")
        public DataSetBuilder sortBy(final Collection<String> columnNames, final Function<? super DisposableObjArray, ? extends Comparable> keyMapper) {
            val.sortBy(columnNames, keyMapper);

            return this;
        }

        /**
         * Parallel sort by.
         *
         * @param columnName
         * @return
         */
        public DataSetBuilder parallelSortBy(final String columnName) {
            val.parallelSortBy(columnName);

            return this;
        }

        /**
         * Parallel sort by.
         *
         * @param <T>
         * @param columnName
         * @param cmp
         * @return
         */
        public <T> DataSetBuilder parallelSortBy(final String columnName, final Comparator<T> cmp) {
            val.parallelSortBy(columnName, cmp);

            return this;
        }

        /**
         * Parallel sort by.
         *
         * @param columnNames
         * @return
         */
        public DataSetBuilder parallelSortBy(final Collection<String> columnNames) {
            val.parallelSortBy(columnNames);

            return this;
        }

        /**
         * Parallel sort by.
         *
         * @param columnNames
         * @param cmp
         * @return
         */
        public DataSetBuilder parallelSortBy(final Collection<String> columnNames, final Comparator<? super Object[]> cmp) {
            val.parallelSortBy(columnNames, cmp);

            return this;
        }

        /**
         * Parallel sort by.
         *
         * @param columnNames
         * @param keyMapper
         * @return
         */
        @SuppressWarnings("rawtypes")
        public DataSetBuilder parallelSortBy(final Collection<String> columnNames, final Function<? super DisposableObjArray, ? extends Comparable> keyMapper) {
            val.parallelSortBy(columnNames, keyMapper);

            return this;
        }
    }

    /**
     * Compares two comparable objects as specified by {@link
     * Comparable#compareTo}, <i>if</i> the result of this comparison chain
     * has not already been determined.
     *
     * @param <T>
     * @param left
     * @param right
     * @return {@code ComparisonChain}
     */
    public static <T extends Comparable<? super T>> ComparisonBuilder compare(final T left, final T right) {
        return new ComparisonBuilder().compare(left, right);
    }

    /**
     * Compares two objects using a comparator, <i>if</i> the result of this
     * comparison chain has not already been determined.
     *
     * @param <T>
     * @param left
     * @param right
     * @param comparator
     * @return {@code ComparisonChain}
     */
    public static <T> ComparisonBuilder compare(final T left, final T right, final Comparator<T> comparator) {
        return new ComparisonBuilder().compare(left, right, comparator);
    }

    //    /**
    //     *
    //     * @param <T>
    //     * @param left
    //     * @param right
    //     * @param func
    //     * @return
    //     */
    //    public static <T> ComparisonBuilder compare(T left, T right, BiFunction<? super T, ? super T, Integer> func) {
    //        return new ComparisonBuilder().compare(left, right, func);
    //    }

    /**
     * {@code null} is smaller.
     *
     * @param <T>
     * @param left
     * @param right
     * @return
     */
    public static <T extends Comparable<? super T>> ComparisonBuilder compareNullLess(final T left, final T right) {
        return new ComparisonBuilder().compareNullLess(left, right);
    }

    /**
     * {@code null} is bigger.
     *
     * @param <T>
     * @param left
     * @param right
     * @return
     */
    public static <T extends Comparable<? super T>> ComparisonBuilder compareNullBigger(final T left, final T right) {
        return new ComparisonBuilder().compareNullBigger(left, right);
    }

    /**
     * Compares two {@code boolean} values, considering {@code false} to be less
     * than {@code true}, <i>if</i> the result of this comparison chain has not
     * already been determined.
     *
     * @param left
     * @param right
     * @return {@code ComparisonChain}
     */
    public static ComparisonBuilder compareFalseLess(final boolean left, final boolean right) {
        return new ComparisonBuilder().compareFalseLess(left, right);
    }

    /**
     * Compares two {@code boolean} values, considering {@code true} to be less
     * than {@code false}, <i>if</i> the result of this comparison chain has not
     * already been determined.
     *
     * @param left
     * @param right
     * @return {@code ComparisonChain}
     */
    public static ComparisonBuilder compareTrueLess(final boolean left, final boolean right) {
        return new ComparisonBuilder().compareTrueLess(left, right);
    }

    /**
     * Compares two {@code char} values as specified by {@link N#compare},
     * <i>if</i> the result of this comparison chain has not already been
     * determined.
     *
     * @param left
     * @param right
     * @return {@code ComparisonChain}
     */
    public static ComparisonBuilder compare(final char left, final char right) {
        return new ComparisonBuilder().compare(left, right);
    }

    /**
     * Compares two {@code byte} values as specified by {@link N#compare},
     * <i>if</i> the result of this comparison chain has not already been
     * determined.
     *
     * @param left
     * @param right
     * @return {@code ComparisonChain}
     */
    public static ComparisonBuilder compare(final byte left, final byte right) {
        return new ComparisonBuilder().compare(left, right);
    }

    /**
     * Compares two {@code short} values as specified by {@link N#compare},
     * <i>if</i> the result of this comparison chain has not already been
     * determined.
     *
     * @param left
     * @param right
     * @return {@code ComparisonChain}
     */
    public static ComparisonBuilder compare(final short left, final short right) {
        return new ComparisonBuilder().compare(left, right);
    }

    /**
     * Compares two {@code int} values as specified by {@link N#compare},
     * <i>if</i> the result of this comparison chain has not already been
     * determined.
     *
     * @param left
     * @param right
     * @return {@code ComparisonChain}
     */
    public static ComparisonBuilder compare(final int left, final int right) {
        return new ComparisonBuilder().compare(left, right);
    }

    /**
     * Compares two {@code long} values as specified by {@link N#compare},
     * <i>if</i> the result of this comparison chain has not already been
     * determined.
     *
     * @param left
     * @param right
     * @return {@code ComparisonChain}
     */
    public static ComparisonBuilder compare(final long left, final long right) {
        return new ComparisonBuilder().compare(left, right);
    }

    /**
     * Compares two {@code float} values as specified by {@link
     * Float#compare}, <i>if</i> the result of this comparison chain has not
     * already been determined.
     *
     * @param left
     * @param right
     * @return {@code ComparisonChain}
     */
    public static ComparisonBuilder compare(final float left, final float right) {
        return new ComparisonBuilder().compare(left, right);
    }

    /**
     * Compares two {@code double} values as specified by {@link
     * Double#compare}, <i>if</i> the result of this comparison chain has not
     * already been determined.
     *
     * @param left
     * @param right
     * @return {@code ComparisonChain}
     */
    public static ComparisonBuilder compare(final double left, final double right) {
        return new ComparisonBuilder().compare(left, right);
    }

    /**
     * Compares two comparable objects as specified by {@link
     * N#equals(Object, Object)}, <i>if</i> the result of this equivalence chain
     * has not already been determined.
     *
     * @param left
     * @param right
     * @return {@code EquivalenceChain}
     */
    public static EquivalenceBuilder equals(final Object left, final Object right) {
        return new EquivalenceBuilder().equals(left, right);
    }

    /**
     *
     * @param <T>
     * @param left
     * @param right
     * @param func
     * @return
     */
    public static <T> EquivalenceBuilder equals(final T left, final T right, final BiFunction<? super T, ? super T, Boolean> func) {
        return new EquivalenceBuilder().equals(left, right, func);
    }

    /**
     * Compares two {@code boolean} values as specified by {@code left == right},
     * <i>if</i> the result of this equivalence chain has not already been
     * determined.
     *
     * @param left
     * @param right
     * @return {@code EquivalenceChain}
     */
    public static EquivalenceBuilder equals(final boolean left, final boolean right) {
        return new EquivalenceBuilder().equals(left, right);
    }

    /**
     * Compares two {@code char} values as specified by {@code left == right},
     * <i>if</i> the result of this equivalence chain has not already been
     * determined.
     *
     * @param left
     * @param right
     * @return {@code EquivalenceChain}
     */
    public static EquivalenceBuilder equals(final char left, final char right) {
        return new EquivalenceBuilder().equals(left, right);
    }

    /**
     * Compares two {@code byte} values as specified by {@code left == right},
     * <i>if</i> the result of this equivalence chain has not already been
     * determined.
     *
     * @param left
     * @param right
     * @return {@code EquivalenceChain}
     */
    public static EquivalenceBuilder equals(final byte left, final byte right) {
        return new EquivalenceBuilder().equals(left, right);
    }

    /**
     * Compares two {@code short} values as specified by {@code left == right},
     * <i>if</i> the result of this equivalence chain has not already been
     * determined.
     *
     * @param left
     * @param right
     * @return {@code EquivalenceChain}
     */
    public static EquivalenceBuilder equals(final short left, final short right) {
        return new EquivalenceBuilder().equals(left, right);
    }

    /**
     * Compares two {@code int} values as specified by {@code left == right},
     * <i>if</i> the result of this equivalence chain has not already been
     * determined.
     *
     * @param left
     * @param right
     * @return {@code EquivalenceChain}
     */
    public static EquivalenceBuilder equals(final int left, final int right) {
        return new EquivalenceBuilder().equals(left, right);
    }

    /**
     * Compares two {@code long} values as specified by {@code left == right},
     * <i>if</i> the result of this equivalence chain has not already been
     * determined.
     *
     * @param left
     * @param right
     * @return {@code EquivalenceChain}
     */
    public static EquivalenceBuilder equals(final long left, final long right) {
        return new EquivalenceBuilder().equals(left, right);
    }

    /**
     * Compares two {@code float} values as specified by {@link
     * Float#compare}, <i>if</i> the result of this equivalence chain has not
     * already been determined.
     *
     * @param left
     * @param right
     * @return {@code EquivalenceChain}
     */
    public static EquivalenceBuilder equals(final float left, final float right) {
        return new EquivalenceBuilder().equals(left, right);
    }

    /**
     * Compares two {@code double} values as specified by {@link
     * Double#compare}, <i>if</i> the result of this equivalence chain has not
     * already been determined.
     *
     * @param left
     * @param right
     * @return {@code EquivalenceChain}
     */
    public static EquivalenceBuilder equals(final double left, final double right) {
        return new EquivalenceBuilder().equals(left, right);
    }

    /**
     * Add the hash code of the specified {@code value} to result.
     *
     * @param value
     * @return this
     */
    public static HashCodeBuilder hash(final Object value) {
        return new HashCodeBuilder().hash(value);
    }

    /**
     *
     * @param <T>
     * @param value
     * @param func
     * @return
     */
    public static <T> HashCodeBuilder hash(final T value, final ToIntFunction<? super T> func) {
        return new HashCodeBuilder().hash(value, func);
    }

    /**
     * Add the hash code of the specified {@code value} to result.
     *
     * @param value
     * @return this
     */
    public static HashCodeBuilder hash(final boolean value) {
        return new HashCodeBuilder().hash(value);
    }

    /**
     * Add the hash code of the specified {@code value} to result.
     *
     * @param value
     * @return this
     */
    public static HashCodeBuilder hash(final char value) {
        return new HashCodeBuilder().hash(value);
    }

    /**
     * Add the hash code of the specified {@code value} to result.
     *
     * @param value
     * @return this
     */
    public static HashCodeBuilder hash(final byte value) {
        return new HashCodeBuilder().hash(value);
    }

    /**
     * Add the hash code of the specified {@code value} to result.
     *
     * @param value
     * @return this
     */
    public static HashCodeBuilder hash(final short value) {
        return new HashCodeBuilder().hash(value);
    }

    /**
     * Add the hash code of the specified {@code value} to result.
     *
     * @param value
     * @return this
     */
    public static HashCodeBuilder hash(final int value) {
        return new HashCodeBuilder().hash(value);
    }

    /**
     * Add the hash code of the specified {@code value} to result.
     *
     * @param value
     * @return this
     */
    public static HashCodeBuilder hash(final long value) {
        return new HashCodeBuilder().hash(value);
    }

    /**
     * Add the hash code of the specified {@code value} to result.
     *
     * @param value
     * @return this
     */
    public static HashCodeBuilder hash(final float value) {
        return new HashCodeBuilder().hash(value);
    }

    /**
     * Add the hash code of the specified {@code value} to result.
     *
     * @param value
     * @return this
     */
    public static HashCodeBuilder hash(final double value) {
        return new HashCodeBuilder().hash(value);
    }

    /**
     * The Class ComparisonChain.
     */
    public static final class ComparisonBuilder {

        /** The result. */
        private int result = 0;

        /**
         * Instantiates a new comparison chain.
         */
        private ComparisonBuilder() {
            // singleton.
        }

        /**
         * Compares two comparable objects as specified by {@link
         * Comparable#compareTo}, <i>if</i> the result of this comparison chain
         * has not already been determined.
         *
         * @param <T>
         * @param left
         * @param right
         * @return this
         */
        public <T extends Comparable<? super T>> ComparisonBuilder compare(final T left, final T right) {
            if (result == 0) {
                result = N.compare(left, right);
            }

            return this;
        }

        /**
         * Compares two objects using a comparator, <i>if</i> the result of this
         * comparison chain has not already been determined.
         *
         * @param <T>
         * @param left
         * @param right
         * @param comparator
         * @return this
         * @throws IllegalArgumentException
         */
        public <T> ComparisonBuilder compare(final T left, final T right, final Comparator<T> comparator) throws IllegalArgumentException {
            if (result == 0) {
                if (comparator == null) {
                    result = Comparators.NATURAL_ORDER.compare(left, right);
                } else {
                    result = comparator.compare(left, right);
                }
            }

            return this;
        }

        //    /**
        //     *
        //     * @param <T>
        //     * @param left
        //     * @param right
        //     * @param func
        //     * @return
        //     */
        //    public <T> ComparisonBuilder compare(T left, T right, BiFunction<? super T, ? super T, Integer> func) {
        //        N.checkArgNotNull(func, "func");
        //
        //        if (result == 0) {
        //            result = func.apply(left, right);
        //        }
        //
        //        return this;
        //    }

        /**
         * {@code null} is smaller.
         *
         * @param <T>
         * @param left
         * @param right
         * @return
         */
        public <T extends Comparable<? super T>> ComparisonBuilder compareNullLess(final T left, final T right) {
            if (result == 0) {
                result = left == null ? (right == null ? 0 : -1) : (right == null ? 1 : left.compareTo(right));
            }

            return this;
        }

        /**
         * {@code null} is bigger.
         *
         * @param <T>
         * @param left
         * @param right
         * @return
         */
        public <T extends Comparable<? super T>> ComparisonBuilder compareNullBigger(final T left, final T right) {
            if (result == 0) {
                result = left == null ? (right == null ? 0 : 1) : (right == null ? -1 : left.compareTo(right));
            }

            return this;
        }

        /**
         * Compares two {@code boolean} values, considering {@code false} to be less
         * than {@code true}, <i>if</i> the result of this comparison chain has not
         * already been determined.
         *
         * @param left
         * @param right
         * @return this
         */
        public ComparisonBuilder compareFalseLess(final boolean left, final boolean right) {
            if (result == 0) {
                result = left == right ? 0 : (left ? -1 : 1);
            }

            return this;
        }

        /**
         * Compares two {@code boolean} values, considering {@code true} to be less
         * than {@code false}, <i>if</i> the result of this comparison chain has not
         * already been determined.
         *
         * @param left
         * @param right
         * @return this
         */
        public ComparisonBuilder compareTrueLess(final boolean left, final boolean right) {
            if (result == 0) {
                result = left == right ? 0 : (left ? 1 : -1);
            }

            return this;
        }

        /**
         * Compares two {@code char} values as specified by {@link N#compare},
         * <i>if</i> the result of this comparison chain has not already been
         * determined.
         *
         * @param left
         * @param right
         * @return this
         */
        public ComparisonBuilder compare(final char left, final char right) {
            if (result == 0) {
                result = N.compare(left, right);
            }

            return this;
        }

        /**
         * Compares two {@code byte} values as specified by {@link N#compare},
         * <i>if</i> the result of this comparison chain has not already been
         * determined.
         *
         * @param left
         * @param right
         * @return this
         */
        public ComparisonBuilder compare(final byte left, final byte right) {
            if (result == 0) {
                result = N.compare(left, right);
            }

            return this;
        }

        /**
         * Compares two {@code short} values as specified by {@link N#compare},
         * <i>if</i> the result of this comparison chain has not already been
         * determined.
         *
         * @param left
         * @param right
         * @return this
         */
        public ComparisonBuilder compare(final short left, final short right) {
            if (result == 0) {
                result = N.compare(left, right);
            }

            return this;
        }

        /**
         * Compares two {@code int} values as specified by {@link N#compare},
         * <i>if</i> the result of this comparison chain has not already been
         * determined.
         *
         * @param left
         * @param right
         * @return this
         */
        public ComparisonBuilder compare(final int left, final int right) {
            if (result == 0) {
                result = N.compare(left, right);
            }

            return this;
        }

        /**
         * Compares two {@code long} values as specified by {@link N#compare},
         * <i>if</i> the result of this comparison chain has not already been
         * determined.
         *
         * @param left
         * @param right
         * @return this
         */
        public ComparisonBuilder compare(final long left, final long right) {
            if (result == 0) {
                result = N.compare(left, right);
            }

            return this;
        }

        /**
         * Compares two {@code float} values as specified by {@link
         * Float#compare}, <i>if</i> the result of this comparison chain has not
         * already been determined.
         *
         * @param left
         * @param right
         * @return this
         */
        public ComparisonBuilder compare(final float left, final float right) {
            if (result == 0) {
                result = N.compare(left, right);
            }

            return this;
        }

        /**
         * Compares two {@code double} values as specified by {@link
         * Double#compare}, <i>if</i> the result of this comparison chain has not
         * already been determined.
         *
         * @param left
         * @param right
         * @return this
         */
        public ComparisonBuilder compare(final double left, final double right) {
            if (result == 0) {
                result = N.compare(left, right);
            }

            return this;
        }

        /**
         *
         * @return
         */
        public int result() {
            return result;
        }
    }

    /**
     * The Class EquivalenceChain.
     */
    public static final class EquivalenceBuilder {

        /** The result. */
        private boolean result = true;

        /**
         * Instantiates a new equivalence chain.
         */
        private EquivalenceBuilder() {
            // singleton.
        }

        /**
         * Compares two comparable objects as specified by {@link
         * N#equals(Object, Object)}, <i>if</i> the result of this equivalence chain
         * has not already been determined.
         *
         * @param left
         * @param right
         * @return this
         */
        public EquivalenceBuilder equals(final Object left, final Object right) {
            if (result) {
                result = N.equals(left, right);
            }

            return this;
        }

        /**
         *
         *
         * @param <T>
         * @param left
         * @param right
         * @param func
         * @return
         * @throws IllegalArgumentException
         */
        public <T> EquivalenceBuilder equals(final T left, final T right, final BiFunction<? super T, ? super T, Boolean> func)
                throws IllegalArgumentException {
            N.checkArgNotNull(func, cs.func);

            if (result) {
                result = func.apply(left, right);
            }

            return this;
        }

        /**
         * Compares two {@code boolean} values as specified by {@code left == right},
         * <i>if</i> the result of this equivalence chain has not already been
         * determined.
         *
         * @param left
         * @param right
         * @return this
         */
        public EquivalenceBuilder equals(final boolean left, final boolean right) {
            if (result) {
                result = left == right;
            }

            return this;
        }

        /**
         * Compares two {@code char} values as specified by {@code left == right},
         * <i>if</i> the result of this equivalence chain has not already been
         * determined.
         *
         * @param left
         * @param right
         * @return this
         */
        public EquivalenceBuilder equals(final char left, final char right) {
            if (result) {
                result = left == right;
            }

            return this;
        }

        /**
         * Compares two {@code byte} values as specified by {@code left == right},
         * <i>if</i> the result of this equivalence chain has not already been
         * determined.
         *
         * @param left
         * @param right
         * @return this
         */
        public EquivalenceBuilder equals(final byte left, final byte right) {
            if (result) {
                result = left == right;
            }

            return this;
        }

        /**
         * Compares two {@code int} values as specified by {@code left == right},
         * <i>if</i> the result of this equivalence chain has not already been
         * determined.
         *
         * @param left
         * @param right
         * @return this
         */
        public EquivalenceBuilder equals(final short left, final short right) {
            if (result) {
                result = left == right;
            }

            return this;
        }

        /**
         * Compares two {@code int} values as specified by {@code left == right},
         * <i>if</i> the result of this equivalence chain has not already been
         * determined.
         *
         * @param left
         * @param right
         * @return this
         */
        public EquivalenceBuilder equals(final int left, final int right) {
            if (result) {
                result = left == right;
            }

            return this;
        }

        /**
         * Compares two {@code long} values as specified by {@code left == right},
         * <i>if</i> the result of this equivalence chain has not already been
         * determined.
         *
         * @param left
         * @param right
         * @return this
         */
        public EquivalenceBuilder equals(final long left, final long right) {
            if (result) {
                result = left == right;
            }

            return this;
        }

        /**
         * Compares two {@code float} values as specified by {@link
         * Float#compare}, <i>if</i> the result of this equivalence chain has not
         * already been determined.
         *
         * @param left
         * @param right
         * @return this
         */
        public EquivalenceBuilder equals(final float left, final float right) {
            if (result) {
                result = Float.compare(left, right) == 0;
            }

            return this;
        }

        /**
         * Compares two {@code double} values as specified by {@link
         * Double#compare}, <i>if</i> the result of this equivalence chain has not
         * already been determined.
         *
         * @param left
         * @param right
         * @return this
         */
        public EquivalenceBuilder equals(final double left, final double right) {
            if (result) {
                result = Double.compare(left, right) == 0;
            }

            return this;
        }

        /**
         *
         * @return
         */
        public boolean result() {
            return result;
        }
    }

    /**
     * The Class HashCodeChain.
     */
    public static final class HashCodeBuilder {

        /** The result. */
        private int result = 0;

        /**
         * Instantiates a new hash code chain.
         */
        private HashCodeBuilder() {
            // singleton.
        }

        /**
         * Add the hash code of the specified {@code value} to result.
         *
         * @param value
         * @return this
         */
        public HashCodeBuilder hash(final Object value) {
            result = result * 31 + N.hashCode(value);

            return this;
        }

        /**
         *
         *
         * @param <T>
         * @param value
         * @param func
         * @return
         * @throws IllegalArgumentException
         */
        public <T> HashCodeBuilder hash(final T value, final ToIntFunction<? super T> func) throws IllegalArgumentException {
            N.checkArgNotNull(func, cs.func);

            result = result * 31 + func.applyAsInt(value);

            return this;
        }

        /**
         * Add the hash code of the specified {@code value} to result.
         *
         * @param value
         * @return this
         */
        public HashCodeBuilder hash(final boolean value) {
            result = result * 31 + (value ? 1231 : 1237);

            return this;
        }

        /**
         * Add the hash code of the specified {@code value} to result.
         *
         * @param value
         * @return this
         */
        public HashCodeBuilder hash(final char value) {
            result = result * 31 + N.hashCode(value);

            return this;
        }

        /**
         * Add the hash code of the specified {@code value} to result.
         *
         * @param value
         * @return this
         */
        public HashCodeBuilder hash(final byte value) {
            result = result * 31 + N.hashCode(value);

            return this;
        }

        /**
         * Add the hash code of the specified {@code value} to result.
         *
         * @param value
         * @return this
         */
        public HashCodeBuilder hash(final short value) {
            result = result * 31 + N.hashCode(value);

            return this;
        }

        /**
         * Add the hash code of the specified {@code value} to result.
         *
         * @param value
         * @return this
         */
        public HashCodeBuilder hash(final int value) {
            result = result * 31 + N.hashCode(value);

            return this;
        }

        /**
         * Add the hash code of the specified {@code value} to result.
         *
         * @param value
         * @return this
         */
        public HashCodeBuilder hash(final long value) {
            result = result * 31 + N.hashCode(value);

            return this;
        }

        /**
         * Add the hash code of the specified {@code value} to result.
         *
         * @param value
         * @return this
         */
        public HashCodeBuilder hash(final float value) {
            result = result * 31 + N.hashCode(value);

            return this;
        }

        /**
         * Add the hash code of the specified {@code value} to result.
         *
         * @param value
         * @return this
         */
        public HashCodeBuilder hash(final double value) {
            result = result * 31 + N.hashCode(value);

            return this;
        }

        /**
         *
         * @return
         */
        public int result() {
            return result;
        }
    }

    /**
     * The Class X.
     *
     * @param <T>
     */
    @Beta
    public static final class X<T> extends Builder<T> {

        /**
         * Instantiates a new x.
         *
         * @param val
         */
        private X(final T val) { //NOSONAR
            super(val);
        }
    }
}
