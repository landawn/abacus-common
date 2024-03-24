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
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.Stream;

/**
 *
 * @author haiyangl
 * @param <T>
 * @see ImmutableList#builder()
 * @see ImmutableSet#builder()
 */
@SuppressWarnings({ "java:S6539" })
public class Builder<T> {

    final T val;

    Builder(T val) {
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
    public static final <T, L extends List<T>> ListBuilder<T, L> of(L val) throws IllegalArgumentException {
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
    public static final <T, C extends Collection<T>> CollectionBuilder<T, C> of(C val) throws IllegalArgumentException {
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
    public static final <K, V, M extends Map<K, V>> MapBuilder<K, V, M> of(M val) throws IllegalArgumentException {
        return new MapBuilder<>(val);
    }

    /**
     *
     * @param <T>
     * @param val
     * @return
     * @throws IllegalArgumentException if the specified {@code val} is {@code null}.
     */
    public static final <T> MultisetBuilder<T> of(Multiset<T> val) throws IllegalArgumentException {
        return new MultisetBuilder<>(val);
    }

    /**
     *
     * @param <T>
     * @param val
     * @return
     * @throws IllegalArgumentException if the specified {@code val} is {@code null}.
     */
    public static final <T> LongMultisetBuilder<T> of(LongMultiset<T> val) throws IllegalArgumentException {
        return new LongMultisetBuilder<>(val);
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
    public static final <K, E, V extends Collection<E>, M extends Multimap<K, E, V>> MultimapBuilder<K, E, V, M> of(M val) throws IllegalArgumentException {
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
        creatorMap.put(LongMultiset.class, val -> Builder.of((LongMultiset) val));

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
    public static final <T> Builder<T> of(T val) {
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
        } else if (val instanceof LongMultiset) {
            result = of((LongMultiset) val);
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
     * @param <R>
     * @param <E>
     * @param mapper
     * @return
     * @throws E the e
     * @deprecated
     */
    @Deprecated
    public <R, E extends Exception> Builder<R> map(final Throwables.Function<? super T, ? extends R, E> mapper) throws E {
        return of(mapper.apply(val));
    }

    /**
     *
     * @param <E>
     * @param predicate
     * @return <code>Optional</code> with the value if <code>predicate</code> returns true,
     * otherwise, return an empty <code>Optional</code>
     * @throws E the e
     * @deprecated
     */
    @Deprecated
    public <E extends Exception> Optional<T> filter(final Throwables.Predicate<? super T, E> predicate) throws E {
        return predicate.test(val) ? Optional.of(val) : Optional.<T> empty();
    }

    /**
     *
     * @param <E>
     * @param consumer
     * @return
     * @throws E the e
     */
    public <E extends Exception> Builder<T> accept(final Throwables.Consumer<? super T, E> consumer) throws E {
        consumer.accept(val);

        return this;
    }

    /**
     *
     * @param <R>
     * @param <E>
     * @param func
     * @return
     * @throws E the e
     */
    public <R, E extends Exception> R apply(final Throwables.Function<? super T, ? extends R, E> func) throws E {
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
        BooleanListBuilder(BooleanList val) {
            super(val);
        }

        /**
         *
         * @param index
         * @param e
         * @return
         */
        public BooleanListBuilder set(int index, boolean e) {
            val.set(index, e);

            return this;
        }

        /**
         *
         * @param e
         * @return
         */
        public BooleanListBuilder add(boolean e) {
            val.add(e);

            return this;
        }

        /**
         *
         * @param index
         * @param e
         * @return
         */
        public BooleanListBuilder add(int index, boolean e) {
            val.add(index, e);

            return this;
        }

        /**
         * Adds the all.
         *
         * @param c
         * @return
         */
        public BooleanListBuilder addAll(BooleanList c) {
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
        public BooleanListBuilder addAll(int index, BooleanList c) {
            val.addAll(index, c);

            return this;
        }

        /**
         *
         * @param e
         * @return
         */
        public BooleanListBuilder remove(boolean e) {
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
        public BooleanListBuilder removeAll(BooleanList c) {
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
        CharListBuilder(CharList val) {
            super(val);
        }

        /**
         *
         * @param index
         * @param e
         * @return
         */
        public CharListBuilder set(int index, char e) {
            val.set(index, e);

            return this;
        }

        /**
         *
         * @param e
         * @return
         */
        public CharListBuilder add(char e) {
            val.add(e);

            return this;
        }

        /**
         *
         * @param index
         * @param e
         * @return
         */
        public CharListBuilder add(int index, char e) {
            val.add(index, e);

            return this;
        }

        /**
         * Adds the all.
         *
         * @param c
         * @return
         */
        public CharListBuilder addAll(CharList c) {
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
        public CharListBuilder addAll(int index, CharList c) {
            val.addAll(index, c);

            return this;
        }

        /**
         *
         * @param e
         * @return
         */
        public CharListBuilder remove(char e) {
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
        public CharListBuilder removeAll(CharList c) {
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
        ByteListBuilder(ByteList val) {
            super(val);
        }

        /**
         *
         * @param index
         * @param e
         * @return
         */
        public ByteListBuilder set(int index, byte e) {
            val.set(index, e);

            return this;
        }

        /**
         *
         * @param e
         * @return
         */
        public ByteListBuilder add(byte e) {
            val.add(e);

            return this;
        }

        /**
         *
         * @param index
         * @param e
         * @return
         */
        public ByteListBuilder add(int index, byte e) {
            val.add(index, e);

            return this;
        }

        /**
         * Adds the all.
         *
         * @param c
         * @return
         */
        public ByteListBuilder addAll(ByteList c) {
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
        public ByteListBuilder addAll(int index, ByteList c) {
            val.addAll(index, c);

            return this;
        }

        /**
         *
         * @param e
         * @return
         */
        public ByteListBuilder remove(byte e) {
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
        public ByteListBuilder removeAll(ByteList c) {
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
        ShortListBuilder(ShortList val) {
            super(val);
        }

        /**
         *
         * @param index
         * @param e
         * @return
         */
        public ShortListBuilder set(int index, short e) {
            val.set(index, e);

            return this;
        }

        /**
         *
         * @param e
         * @return
         */
        public ShortListBuilder add(short e) {
            val.add(e);

            return this;
        }

        /**
         *
         * @param index
         * @param e
         * @return
         */
        public ShortListBuilder add(int index, short e) {
            val.add(index, e);

            return this;
        }

        /**
         * Adds the all.
         *
         * @param c
         * @return
         */
        public ShortListBuilder addAll(ShortList c) {
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
        public ShortListBuilder addAll(int index, ShortList c) {
            val.addAll(index, c);

            return this;
        }

        /**
         *
         * @param e
         * @return
         */
        public ShortListBuilder remove(short e) {
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
        public ShortListBuilder removeAll(ShortList c) {
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
        IntListBuilder(IntList val) {
            super(val);
        }

        /**
         *
         * @param index
         * @param e
         * @return
         */
        public IntListBuilder set(int index, int e) {
            val.set(index, e);

            return this;
        }

        /**
         *
         * @param e
         * @return
         */
        public IntListBuilder add(int e) {
            val.add(e);

            return this;
        }

        /**
         *
         * @param index
         * @param e
         * @return
         */
        public IntListBuilder add(int index, int e) {
            val.add(index, e);

            return this;
        }

        /**
         * Adds the all.
         *
         * @param c
         * @return
         */
        public IntListBuilder addAll(IntList c) {
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
        public IntListBuilder addAll(int index, IntList c) {
            val.addAll(index, c);

            return this;
        }

        /**
         *
         * @param e
         * @return
         */
        public IntListBuilder remove(int e) {
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
        public IntListBuilder removeAll(IntList c) {
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
        LongListBuilder(LongList val) {
            super(val);
        }

        /**
         *
         * @param index
         * @param e
         * @return
         */
        public LongListBuilder set(int index, long e) {
            val.set(index, e);

            return this;
        }

        /**
         *
         * @param e
         * @return
         */
        public LongListBuilder add(long e) {
            val.add(e);

            return this;
        }

        /**
         *
         * @param index
         * @param e
         * @return
         */
        public LongListBuilder add(int index, long e) {
            val.add(index, e);

            return this;
        }

        /**
         * Adds the all.
         *
         * @param c
         * @return
         */
        public LongListBuilder addAll(LongList c) {
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
        public LongListBuilder addAll(int index, LongList c) {
            val.addAll(index, c);

            return this;
        }

        /**
         *
         * @param e
         * @return
         */
        public LongListBuilder remove(long e) {
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
        public LongListBuilder removeAll(LongList c) {
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
        FloatListBuilder(FloatList val) {
            super(val);
        }

        /**
         *
         * @param index
         * @param e
         * @return
         */
        public FloatListBuilder set(int index, float e) {
            val.set(index, e);

            return this;
        }

        /**
         *
         * @param e
         * @return
         */
        public FloatListBuilder add(float e) {
            val.add(e);

            return this;
        }

        /**
         *
         * @param index
         * @param e
         * @return
         */
        public FloatListBuilder add(int index, float e) {
            val.add(index, e);

            return this;
        }

        /**
         * Adds the all.
         *
         * @param c
         * @return
         */
        public FloatListBuilder addAll(FloatList c) {
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
        public FloatListBuilder addAll(int index, FloatList c) {
            val.addAll(index, c);

            return this;
        }

        /**
         *
         * @param e
         * @return
         */
        public FloatListBuilder remove(float e) {
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
        public FloatListBuilder removeAll(FloatList c) {
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
        DoubleListBuilder(DoubleList val) {
            super(val);
        }

        /**
         *
         * @param index
         * @param e
         * @return
         */
        public DoubleListBuilder set(int index, double e) {
            val.set(index, e);

            return this;
        }

        /**
         *
         * @param e
         * @return
         */
        public DoubleListBuilder add(double e) {
            val.add(e);

            return this;
        }

        /**
         *
         * @param index
         * @param e
         * @return
         */
        public DoubleListBuilder add(int index, double e) {
            val.add(index, e);

            return this;
        }

        /**
         * Adds the all.
         *
         * @param c
         * @return
         */
        public DoubleListBuilder addAll(DoubleList c) {
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
        public DoubleListBuilder addAll(int index, DoubleList c) {
            val.addAll(index, c);

            return this;
        }

        /**
         *
         * @param e
         * @return
         */
        public DoubleListBuilder remove(double e) {
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
        public DoubleListBuilder removeAll(DoubleList c) {
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
        ListBuilder(L c) {
            super(c);
        }

        /**
         *
         * @param index
         * @param e
         * @return
         */
        public ListBuilder<T, L> add(int index, T e) {
            val.add(index, e);

            return this;
        }

        /**
         * Adds the all.
         *
         * @param index
         * @param c
         * @return
         */
        public ListBuilder<T, L> addAll(int index, Collection<? extends T> c) {
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
        public ListBuilder<T, L> remove(int index) {
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
        CollectionBuilder(C c) {
            super(c);
        }

        /**
         *
         * @param e
         * @return
         */
        public CollectionBuilder<T, C> add(T e) {
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
        public CollectionBuilder<T, C> remove(Object e) {
            val.remove(e);

            return this;
        }

        /**
         * Removes the all.
         *
         * @param c
         * @return
         */
        public CollectionBuilder<T, C> removeAll(Collection<?> c) {
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
        MultisetBuilder(Multiset<T> c) {
            super(c);
        }

        /**
         *
         * @param e
         * @return
         */
        public MultisetBuilder<T> add(T e) {
            val.add(e);

            return this;
        }

        /**
         * Adds the all.
         *
         * @param c
         * @return
         */
        public MultisetBuilder<T> addAll(final Collection<? extends T> c) {
            val.addAll(c);

            return this;
        }

        /**
         * Adds the all.
         *
         * @param m
         * @return
         */
        public MultisetBuilder<T> addAll(final Map<? extends T, Integer> m) {
            val.addAll(m);

            return this;
        }

        /**
         * Adds the all.
         *
         * @param multiset
         * @return
         */
        public MultisetBuilder<T> addAll(final Multiset<? extends T> multiset) {
            val.addAll(multiset);

            return this;
        }

        /**
         *
         * @param e
         * @return
         */
        public MultisetBuilder<T> remove(Object e) {
            val.remove(e);

            return this;
        }

        /**
         * Removes the all.
         *
         * @param c
         * @return
         */
        public MultisetBuilder<T> removeAll(Collection<?> c) {
            val.removeAll(c);

            return this;
        }

        /**
         * Removes the all.
         *
         * @param m
         * @return
         */
        public MultisetBuilder<T> removeAll(final Map<? extends T, Integer> m) {
            val.removeAll(m);

            return this;
        }

        /**
         * Removes the all.
         *
         * @param multiset
         * @return
         */
        public MultisetBuilder<T> removeAll(Multiset<? extends T> multiset) {
            val.removeAll(multiset);

            return this;
        }
    }

    /**
     * The Class LongMultisetBuilder.
     *
     * @param <T>
     */
    public static final class LongMultisetBuilder<T> extends Builder<LongMultiset<T>> {

        /**
         * Instantiates a new long multiset builder.
         *
         * @param c
         */
        LongMultisetBuilder(LongMultiset<T> c) {
            super(c);
        }

        /**
         *
         * @param e
         * @return
         */
        public LongMultisetBuilder<T> add(T e) {
            val.add(e);

            return this;
        }

        /**
         * Adds the all.
         *
         * @param c
         * @return
         */
        public LongMultisetBuilder<T> addAll(final Collection<? extends T> c) {
            val.addAll(c);

            return this;
        }

        /**
         * Adds the all.
         *
         * @param m
         * @return
         */
        public LongMultisetBuilder<T> addAll(final Map<? extends T, Long> m) {
            val.addAll(m);

            return this;
        }

        /**
         * Adds the all.
         *
         * @param multiset
         * @return
         */
        public LongMultisetBuilder<T> addAll(final LongMultiset<? extends T> multiset) {
            val.addAll(multiset);

            return this;
        }

        /**
         *
         * @param e
         * @return
         */
        public LongMultisetBuilder<T> remove(Object e) {
            val.remove(e);

            return this;
        }

        /**
         * Removes the all.
         *
         * @param c
         * @return
         */
        public LongMultisetBuilder<T> removeAll(Collection<?> c) {
            val.removeAll(c);

            return this;
        }

        /**
         * Removes the all.
         *
         * @param m
         * @return
         */
        public LongMultisetBuilder<T> removeAll(final Map<? extends T, Long> m) {
            val.removeAll(m);

            return this;
        }

        /**
         * Removes the all.
         *
         * @param multiset
         * @return
         */
        public LongMultisetBuilder<T> removeAll(LongMultiset<? extends T> multiset) {
            val.removeAll(multiset);

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
        MapBuilder(M m) {
            super(m);
        }

        /**
         *
         * @param k
         * @param v
         * @return
         */
        public MapBuilder<K, V, M> put(K k, V v) {
            val.put(k, v);

            return this;
        }

        /**
         *
         * @param m
         * @return
         */
        public MapBuilder<K, V, M> putAll(Map<? extends K, ? extends V> m) {
            if (N.notEmpty(m)) {
                val.putAll(m);
            }

            return this;
        }

        /**
         * Put if absent.
         *
         * @param key
         * @param value
         * @return
         */
        public MapBuilder<K, V, M> putIfAbsent(K key, V value) {
            V v = val.get(key);

            if (v == null && val.containsKey(key) == false) {
                val.put(key, value);
            }

            return this;
        }

        /**
         * Put if absent.
         *
         * @param key
         * @param supplier
         * @return
         */
        public MapBuilder<K, V, M> putIfAbsent(K key, Supplier<V> supplier) {
            V v = val.get(key);

            if (v == null && val.containsKey(key) == false) {
                val.put(key, supplier.get());
            }

            return this;
        }

        /**
         *
         * @param k
         * @return
         */
        public MapBuilder<K, V, M> remove(Object k) {
            val.remove(k);

            return this;
        }

        /**
         * Removes the all.
         *
         * @param keysToRemove
         * @return
         */
        public MapBuilder<K, V, M> removeAll(Collection<?> keysToRemove) {
            if (N.notEmpty(keysToRemove)) {
                for (Object k : keysToRemove) {
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
        MultimapBuilder(M m) {
            super(m);
        }

        /**
         *
         * @param key
         * @param e
         * @return
         */
        public MultimapBuilder<K, E, V, M> put(K key, E e) {
            val.put(key, e);

            return this;
        }

        /**
         *
         * @param k
         * @param c
         * @return
         */
        public MultimapBuilder<K, E, V, M> putAll(final K k, final Collection<? extends E> c) {
            val.putAll(k, c);

            return this;
        }

        /**
         *
         * @param m
         * @return
         */
        public MultimapBuilder<K, E, V, M> putAll(Map<? extends K, ? extends E> m) {
            val.putAll(m);

            return this;
        }

        /**
         *
         * @param m
         * @return
         */
        public MultimapBuilder<K, E, V, M> putAll(Multimap<? extends K, ? extends E, ? extends V> m) {
            val.putAll(m);

            return this;
        }

        /**
         *
         * @param k
         * @param e
         * @return
         */
        public MultimapBuilder<K, E, V, M> remove(Object k, Object e) {
            val.remove(k, e);

            return this;
        }

        /**
         * Removes the all.
         *
         * @param k
         * @return
         */
        public MultimapBuilder<K, E, V, M> removeAll(Object k) {
            val.removeAll(k);

            return this;
        }

        /**
         * Removes the all.
         *
         * @param k
         * @param valuesToRemove
         * @return
         */
        public MultimapBuilder<K, E, V, M> removeAll(Object k, Collection<?> valuesToRemove) {
            val.removeAll(k, valuesToRemove);

            return this;
        }

        /**
         * Removes the all.
         *
         * @param m
         * @return
         */
        public MultimapBuilder<K, E, V, M> removeAll(Map<? extends K, ? extends E> m) {
            val.removeAll(m);

            return this;
        }

        /**
         * Removes the all.
         *
         * @param m
         * @return
         */
        public MultimapBuilder<K, E, V, M> removeAll(Multimap<?, ?, ?> m) {
            val.removeAll(m);

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
        DataSetBuilder(DataSet ds) {
            super(ds);
        }

        /**
         *
         * @param columnName
         * @param newColumnName
         * @return
         */
        public DataSetBuilder renameColumn(String columnName, String newColumnName) {
            val.renameColumn(columnName, newColumnName);

            return this;
        }

        /**
         *
         * @param oldNewNames
         * @return
         */
        public DataSetBuilder renameColumns(Map<String, String> oldNewNames) {
            val.renameColumns(oldNewNames);

            return this;
        }

        /**
         *
         * @param <E>
         * @param columnName
         * @param func
         * @return
         * @throws E the e
         */
        public <E extends Exception> DataSetBuilder renameColumn(String columnName, Throwables.Function<String, String, E> func) throws E {
            val.renameColumn(columnName, func);

            return this;
        }

        /**
         *
         * @param <E>
         * @param columnNames
         * @param func
         * @return
         * @throws E the e
         */
        public <E extends Exception> DataSetBuilder renameColumns(Collection<String> columnNames, Throwables.Function<String, String, E> func) throws E {
            val.renameColumns(columnNames, func);

            return this;
        }

        /**
         *
         * @param <E>
         * @param func
         * @return
         * @throws E the e
         */
        public <E extends Exception> DataSetBuilder renameColumns(Throwables.Function<String, String, E> func) throws E {
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
        public DataSetBuilder addColumn(String columnName, List<?> column) {
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
        public DataSetBuilder addColumn(int columnIndex, String columnName, List<?> column) {
            val.addColumn(columnIndex, columnName, column);

            return this;
        }

        /**
         * Adds the column.
         *
         * @param <T>
         * @param <E>
         * @param newColumnName
         * @param fromColumnName
         * @param func
         * @return
         * @throws E the e
         */
        public <T, E extends Exception> DataSetBuilder addColumn(String newColumnName, String fromColumnName, Throwables.Function<T, ?, E> func) throws E {
            val.addColumn(newColumnName, fromColumnName, func);

            return this;
        }

        /**
         * Adds the column.
         *
         * @param <T>
         * @param <E>
         * @param columnIndex
         * @param newColumnName
         * @param fromColumnName
         * @param func
         * @return
         * @throws E the e
         */
        public <T, E extends Exception> DataSetBuilder addColumn(int columnIndex, String newColumnName, String fromColumnName,
                Throwables.Function<T, ?, E> func) throws E {
            val.addColumn(columnIndex, newColumnName, fromColumnName, func);

            return this;
        }

        /**
         * Adds the column.
         *
         * @param <E>
         * @param newColumnName
         * @param fromColumnNames
         * @param func
         * @return
         * @throws E the e
         */
        public <E extends Exception> DataSetBuilder addColumn(String newColumnName, Collection<String> fromColumnNames,
                Throwables.Function<? super DisposableObjArray, ?, E> func) throws E {
            val.addColumn(newColumnName, fromColumnNames, func);

            return this;
        }

        /**
         * Adds the column.
         *
         * @param <E>
         * @param columnIndex
         * @param newColumnName
         * @param fromColumnNames
         * @param func
         * @return
         * @throws E the e
         */
        public <E extends Exception> DataSetBuilder addColumn(int columnIndex, String newColumnName, Collection<String> fromColumnNames,
                Throwables.Function<? super DisposableObjArray, ?, E> func) throws E {
            val.addColumn(columnIndex, newColumnName, fromColumnNames, func);

            return this;
        }

        /**
         * Adds the column.
         *
         * @param <E>
         * @param newColumnName
         * @param fromColumnNames
         * @param func
         * @return
         * @throws E the e
         */
        public <E extends Exception> DataSetBuilder addColumn(String newColumnName, Tuple2<String, String> fromColumnNames,
                Throwables.BiFunction<?, ?, ?, E> func) throws E {
            val.addColumn(newColumnName, fromColumnNames, func);

            return this;
        }

        /**
         * Adds the column.
         *
         * @param <E>
         * @param columnIndex
         * @param newColumnName
         * @param fromColumnNames
         * @param func
         * @return
         * @throws E the e
         */
        public <E extends Exception> DataSetBuilder addColumn(int columnIndex, String newColumnName, Tuple2<String, String> fromColumnNames,
                Throwables.BiFunction<?, ?, ?, E> func) throws E {
            val.addColumn(columnIndex, newColumnName, fromColumnNames, func);

            return this;
        }

        /**
         * Adds the column.
         *
         * @param <E>
         * @param newColumnName
         * @param fromColumnNames
         * @param func
         * @return
         * @throws E the e
         */
        public <E extends Exception> DataSetBuilder addColumn(String newColumnName, Tuple3<String, String, String> fromColumnNames,
                Throwables.TriFunction<?, ?, ?, ?, E> func) throws E {
            val.addColumn(newColumnName, fromColumnNames, func);

            return this;
        }

        /**
         * Adds the column.
         *
         * @param <E>
         * @param columnIndex
         * @param newColumnName
         * @param fromColumnNames
         * @param func
         * @return
         * @throws E the e
         */
        public <E extends Exception> DataSetBuilder addColumn(int columnIndex, String newColumnName, Tuple3<String, String, String> fromColumnNames,
                Throwables.TriFunction<?, ?, ?, ?, E> func) throws E {
            val.addColumn(columnIndex, newColumnName, fromColumnNames, func);

            return this;
        }

        /**
         * Removes the column.
         *
         * @param columnName
         * @return
         */
        public DataSetBuilder removeColumn(String columnName) {
            val.removeColumn(columnName);

            return this;
        }

        /**
         * Removes the columns.
         *
         * @param columnNames
         * @return
         */
        public DataSetBuilder removeColumns(Collection<String> columnNames) {
            val.removeColumns(columnNames);

            return this;
        }

        /**
         * Removes the columns.
         *
         * @param <E>
         * @param filter
         * @return
         * @throws E the e
         */
        public <E extends Exception> DataSetBuilder removeColumns(Throwables.Predicate<String, E> filter) throws E {
            val.removeColumns(filter);

            return this;
        }

        //        /**
        //         * Removes the columns if.
        //         *
        //         * @param <E>
        //         * @param filter
        //         * @return
        //         * @throws E the e
        //         * @deprecated replaced by {@code removeColumns}.
        //         */
        //        @Deprecated
        //        public <E extends Exception> DataSetBuilder removeColumnsIf(Predicate<String, E> filter) throws E {
        //            val.removeColumnsIf(filter);
        //
        //            return this;
        //        }

        /**
         *
         * @param <T>
         * @param <E>
         * @param columnName
         * @param func
         * @return
         * @throws E the e
         */
        public <T, E extends Exception> DataSetBuilder updateColumn(String columnName, Throwables.Function<T, ?, E> func) throws E {
            val.updateColumn(columnName, func);

            return this;
        }

        /**
         *
         *
         * @param <E>
         * @param columnNames
         * @param func
         * @return
         * @throws E the e
         */
        public <E extends Exception> DataSetBuilder updateColumns(Collection<String> columnNames, Throwables.Function<?, ?, E> func) throws E {
            val.updateColumns(columnNames, func);

            return this;
        }

        /**
         *
         * @param columnName
         * @param targetType
         * @return
         */
        public DataSetBuilder convertColumn(String columnName, Class<?> targetType) {
            val.convertColumn(columnName, targetType);

            return this;
        }

        /**
         *
         * @param columnTargetTypes
         * @return
         */
        public DataSetBuilder convertColumns(Map<String, Class<?>> columnTargetTypes) {
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
        public DataSetBuilder combineColumns(Collection<String> columnNames, String newColumnName, Class<?> newColumnClass) {
            val.combineColumns(columnNames, newColumnName, newColumnClass);

            return this;
        }

        /**
         *
         * @param <E>
         * @param columnNames
         * @param newColumnName
         * @param combineFunc
         * @return
         * @throws E the e
         */
        public <E extends Exception> DataSetBuilder combineColumns(Collection<String> columnNames, String newColumnName,
                Throwables.Function<? super DisposableObjArray, ?, E> combineFunc) throws E {
            val.combineColumns(columnNames, newColumnName, combineFunc);

            return this;
        }

        /**
         *
         * @param <E>
         * @param columnNames
         * @param newColumnName
         * @param combineFunc
         * @return
         * @throws E the e
         */
        public <E extends Exception> DataSetBuilder combineColumns(Tuple2<String, String> columnNames, String newColumnName,
                Throwables.BiFunction<?, ?, ?, E> combineFunc) throws E {
            val.combineColumns(columnNames, newColumnName, combineFunc);

            return this;
        }

        /**
         *
         * @param <E>
         * @param columnNames
         * @param newColumnName
         * @param combineFunc
         * @return
         * @throws E the e
         */
        public <E extends Exception> DataSetBuilder combineColumns(Tuple3<String, String, String> columnNames, String newColumnName,
                Throwables.TriFunction<?, ?, ?, ?, E> combineFunc) throws E {
            val.combineColumns(columnNames, newColumnName, combineFunc);

            return this;
        }

        /**
         *
         * @param <E>
         * @param columnNameFilter
         * @param newColumnName
         * @param newColumnClass
         * @return
         * @throws E the e
         */
        public <E extends Exception> DataSetBuilder combineColumns(Throwables.Predicate<String, E> columnNameFilter, String newColumnName,
                Class<?> newColumnClass) throws E {
            val.combineColumns(columnNameFilter, newColumnName, newColumnClass);

            return this;
        }

        /**
         *
         * @param <E>
         * @param <E2>
         * @param columnNameFilter
         * @param newColumnName
         * @param combineFunc
         * @return
         * @throws E the e
         * @throws E2 the e2
         */
        public <E extends Exception, E2 extends Exception> DataSetBuilder combineColumns(Throwables.Predicate<String, E> columnNameFilter, String newColumnName,
                Throwables.Function<? super DisposableObjArray, ?, E2> combineFunc) throws E, E2 {
            val.combineColumns(columnNameFilter, newColumnName, combineFunc);

            return this;
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @param columnName
         * @param newColumnNames
         * @param divideFunc
         * @return
         * @throws E the e
         */
        public <T, E extends Exception> DataSetBuilder divideColumn(String columnName, Collection<String> newColumnNames,
                Throwables.Function<T, ? extends List<?>, E> divideFunc) throws E {
            val.divideColumn(columnName, newColumnNames, divideFunc);

            return this;
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @param columnName
         * @param newColumnNames
         * @param output
         * @return
         * @throws E the e
         */
        public <T, E extends Exception> DataSetBuilder divideColumn(String columnName, Collection<String> newColumnNames,
                Throwables.BiConsumer<T, Object[], E> output) throws E {
            val.divideColumn(columnName, newColumnNames, output);

            return this;
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @param columnName
         * @param newColumnNames
         * @param output
         * @return
         * @throws E the e
         */
        public <T, E extends Exception> DataSetBuilder divideColumn(String columnName, Tuple2<String, String> newColumnNames,
                Throwables.BiConsumer<T, Pair<Object, Object>, E> output) throws E {
            val.divideColumn(columnName, newColumnNames, output);

            return this;
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @param columnName
         * @param newColumnNames
         * @param output
         * @return
         * @throws E the e
         */
        public <T, E extends Exception> DataSetBuilder divideColumn(String columnName, Tuple3<String, String, String> newColumnNames,
                Throwables.BiConsumer<T, Triple<Object, Object, Object>, E> output) throws E {
            val.divideColumn(columnName, newColumnNames, output);

            return this;
        }

        /**
         *
         * @param <E>
         * @param func
         * @return
         * @throws E the e
         */
        public <E extends Exception> DataSetBuilder updateAll(Throwables.Function<?, ?, E> func) throws E {
            val.updateAll(func);

            return this;
        }

        /**
         *
         * @param <E>
         * @param predicate
         * @param newValue
         * @return
         * @throws E the e
         */
        public <E extends Exception> DataSetBuilder replaceIf(Throwables.Predicate<?, E> predicate, Object newValue) throws E {
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
        public DataSetBuilder prepend(DataSet other) {
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
        public DataSetBuilder append(DataSet other) {
            val.append(other);

            return this;
        }

        /**
         *
         * @param columnName
         * @return
         */
        public DataSetBuilder sortBy(String columnName) {
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
        public <T> DataSetBuilder sortBy(String columnName, Comparator<T> cmp) {
            val.sortBy(columnName, cmp);

            return this;
        }

        /**
         *
         * @param columnNames
         * @return
         */
        public DataSetBuilder sortBy(Collection<String> columnNames) {
            val.sortBy(columnNames);

            return this;
        }

        /**
         *
         * @param columnNames
         * @param cmp
         * @return
         */
        public DataSetBuilder sortBy(Collection<String> columnNames, Comparator<? super Object[]> cmp) {
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
        public DataSetBuilder sortBy(Collection<String> columnNames, Function<? super DisposableObjArray, ? extends Comparable> keyMapper) {
            val.sortBy(columnNames, keyMapper);

            return this;
        }

        /**
         * Parallel sort by.
         *
         * @param columnName
         * @return
         */
        public DataSetBuilder parallelSortBy(String columnName) {
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
        public <T> DataSetBuilder parallelSortBy(String columnName, Comparator<T> cmp) {
            val.parallelSortBy(columnName, cmp);

            return this;
        }

        /**
         * Parallel sort by.
         *
         * @param columnNames
         * @return
         */
        public DataSetBuilder parallelSortBy(Collection<String> columnNames) {
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
        public DataSetBuilder parallelSortBy(Collection<String> columnNames, Comparator<? super Object[]> cmp) {
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
        public DataSetBuilder parallelSortBy(Collection<String> columnNames, Function<? super DisposableObjArray, ? extends Comparable> keyMapper) {
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
    public static <T extends Comparable<? super T>> ComparisonBuilder compare(T left, T right) {
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
    public static <T> ComparisonBuilder compare(T left, T right, Comparator<T> comparator) {
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
    public static <T extends Comparable<? super T>> ComparisonBuilder compareNullLess(T left, T right) {
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
    public static <T extends Comparable<? super T>> ComparisonBuilder compareNullBigger(T left, T right) {
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
    public static ComparisonBuilder compareFalseLess(boolean left, boolean right) {
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
    public static ComparisonBuilder compareTrueLess(boolean left, boolean right) {
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
    public static ComparisonBuilder compare(char left, char right) {
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
    public static ComparisonBuilder compare(byte left, byte right) {
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
    public static ComparisonBuilder compare(short left, short right) {
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
    public static ComparisonBuilder compare(int left, int right) {
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
    public static ComparisonBuilder compare(long left, long right) {
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
    public static ComparisonBuilder compare(float left, float right) {
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
    public static ComparisonBuilder compare(double left, double right) {
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
    public static EquivalenceBuilder equals(Object left, Object right) {
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
    public static <T> EquivalenceBuilder equals(T left, T right, BiFunction<? super T, ? super T, Boolean> func) {
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
    public static EquivalenceBuilder equals(boolean left, boolean right) {
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
    public static EquivalenceBuilder equals(char left, char right) {
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
    public static EquivalenceBuilder equals(byte left, byte right) {
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
    public static EquivalenceBuilder equals(short left, short right) {
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
    public static EquivalenceBuilder equals(int left, int right) {
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
    public static EquivalenceBuilder equals(long left, long right) {
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
    public static EquivalenceBuilder equals(float left, float right) {
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
    public static EquivalenceBuilder equals(double left, double right) {
        return new EquivalenceBuilder().equals(left, right);
    }

    /**
     * Add the hash code of the specified {@code value} to result.
     *
     * @param value
     * @return this
     */
    public static HashCodeBuilder hash(Object value) {
        return new HashCodeBuilder().hash(value);
    }

    /**
     *
     * @param <T>
     * @param value
     * @param func
     * @return
     */
    public static <T> HashCodeBuilder hash(T value, ToIntFunction<? super T> func) {
        return new HashCodeBuilder().hash(value, func);
    }

    /**
     * Add the hash code of the specified {@code value} to result.
     *
     * @param value
     * @return this
     */
    public static HashCodeBuilder hash(boolean value) {
        return new HashCodeBuilder().hash(value);
    }

    /**
     * Add the hash code of the specified {@code value} to result.
     *
     * @param value
     * @return this
     */
    public static HashCodeBuilder hash(char value) {
        return new HashCodeBuilder().hash(value);
    }

    /**
     * Add the hash code of the specified {@code value} to result.
     *
     * @param value
     * @return this
     */
    public static HashCodeBuilder hash(byte value) {
        return new HashCodeBuilder().hash(value);
    }

    /**
     * Add the hash code of the specified {@code value} to result.
     *
     * @param value
     * @return this
     */
    public static HashCodeBuilder hash(short value) {
        return new HashCodeBuilder().hash(value);
    }

    /**
     * Add the hash code of the specified {@code value} to result.
     *
     * @param value
     * @return this
     */
    public static HashCodeBuilder hash(int value) {
        return new HashCodeBuilder().hash(value);
    }

    /**
     * Add the hash code of the specified {@code value} to result.
     *
     * @param value
     * @return this
     */
    public static HashCodeBuilder hash(long value) {
        return new HashCodeBuilder().hash(value);
    }

    /**
     * Add the hash code of the specified {@code value} to result.
     *
     * @param value
     * @return this
     */
    public static HashCodeBuilder hash(float value) {
        return new HashCodeBuilder().hash(value);
    }

    /**
     * Add the hash code of the specified {@code value} to result.
     *
     * @param value
     * @return this
     */
    public static HashCodeBuilder hash(double value) {
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
        public <T extends Comparable<? super T>> ComparisonBuilder compare(T left, T right) {
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
         */
        public <T> ComparisonBuilder compare(T left, T right, Comparator<T> comparator) {
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
        public <T extends Comparable<? super T>> ComparisonBuilder compareNullLess(T left, T right) {
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
        public <T extends Comparable<? super T>> ComparisonBuilder compareNullBigger(T left, T right) {
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
        public ComparisonBuilder compareFalseLess(boolean left, boolean right) {
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
        public ComparisonBuilder compareTrueLess(boolean left, boolean right) {
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
        public ComparisonBuilder compare(char left, char right) {
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
        public ComparisonBuilder compare(byte left, byte right) {
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
        public ComparisonBuilder compare(short left, short right) {
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
        public ComparisonBuilder compare(int left, int right) {
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
        public ComparisonBuilder compare(long left, long right) {
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
        public ComparisonBuilder compare(float left, float right) {
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
        public ComparisonBuilder compare(double left, double right) {
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
        public EquivalenceBuilder equals(Object left, Object right) {
            if (result) {
                result = N.equals(left, right);
            }

            return this;
        }

        /**
         *
         * @param <T>
         * @param left
         * @param right
         * @param func
         * @return
         */
        public <T> EquivalenceBuilder equals(T left, T right, BiFunction<? super T, ? super T, Boolean> func) {
            N.checkArgNotNull(func, "func");

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
        public EquivalenceBuilder equals(boolean left, boolean right) {
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
        public EquivalenceBuilder equals(char left, char right) {
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
        public EquivalenceBuilder equals(byte left, byte right) {
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
        public EquivalenceBuilder equals(short left, short right) {
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
        public EquivalenceBuilder equals(int left, int right) {
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
        public EquivalenceBuilder equals(long left, long right) {
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
        public EquivalenceBuilder equals(float left, float right) {
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
        public EquivalenceBuilder equals(double left, double right) {
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
        public HashCodeBuilder hash(Object value) {
            result = result * 31 + N.hashCode(value);

            return this;
        }

        /**
         *
         * @param <T>
         * @param value
         * @param func
         * @return
         */
        public <T> HashCodeBuilder hash(T value, ToIntFunction<? super T> func) {
            N.checkArgNotNull(func, "func");

            result = result * 31 + func.applyAsInt(value);

            return this;
        }

        /**
         * Add the hash code of the specified {@code value} to result.
         *
         * @param value
         * @return this
         */
        public HashCodeBuilder hash(boolean value) {
            result = result * 31 + (value ? 1231 : 1237);

            return this;
        }

        /**
         * Add the hash code of the specified {@code value} to result.
         *
         * @param value
         * @return this
         */
        public HashCodeBuilder hash(char value) {
            result = result * 31 + N.hashCode(value);

            return this;
        }

        /**
         * Add the hash code of the specified {@code value} to result.
         *
         * @param value
         * @return this
         */
        public HashCodeBuilder hash(byte value) {
            result = result * 31 + N.hashCode(value);

            return this;
        }

        /**
         * Add the hash code of the specified {@code value} to result.
         *
         * @param value
         * @return this
         */
        public HashCodeBuilder hash(short value) {
            result = result * 31 + N.hashCode(value);

            return this;
        }

        /**
         * Add the hash code of the specified {@code value} to result.
         *
         * @param value
         * @return this
         */
        public HashCodeBuilder hash(int value) {
            result = result * 31 + N.hashCode(value);

            return this;
        }

        /**
         * Add the hash code of the specified {@code value} to result.
         *
         * @param value
         * @return this
         */
        public HashCodeBuilder hash(long value) {
            result = result * 31 + N.hashCode(value);

            return this;
        }

        /**
         * Add the hash code of the specified {@code value} to result.
         *
         * @param value
         * @return this
         */
        public HashCodeBuilder hash(float value) {
            result = result * 31 + N.hashCode(value);

            return this;
        }

        /**
         * Add the hash code of the specified {@code value} to result.
         *
         * @param value
         * @return this
         */
        public HashCodeBuilder hash(double value) {
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
        private X(T val) { //NOSONAR
            super(val);
        }

        //        /**
        //         *
        //         * @param output
        //         * @param e
        //         * @return return the specified {@code output}
        //         */
        //        public static <E, C extends Collection<E>> C add(final C output, final E e) {
        //            output.add(e);
        //            return output;
        //        }
        //
        //        /**
        //         *
        //         * @param output
        //         * @param e
        //         * @return return the specified {@code output}
        //         */
        //        public static <E, C extends Collection<E>> C addAll(final C output, final Collection<? extends E> c) {
        //            if (c == null || c.size() == 0) {
        //                return output;
        //            }
        //
        //            output.addAll(c);
        //            return output;
        //        }
        //
        //        /**
        //         *
        //         * @param output
        //         * @param e
        //         * @return return the specified {@code output}
        //         */
        //        public static <E, C extends Collection<E>> C remove(final C output, final Object e) {
        //            if (output == null || output.size() == 0) {
        //                return output;
        //            }
        //
        //            output.remove(e);
        //            return output;
        //        }
        //
        //        /**
        //         *
        //         * @param output
        //         * @param e
        //         * @return return the specified {@code output}
        //         */
        //        public static <E, C extends Collection<E>> C removeAll(final C output, final Collection<?> c) {
        //            if (output == null || output.size() == 0 || c == null || c.size() == 0) {
        //                return output;
        //            }
        //
        //            output.removeAll(c);
        //            return output;
        //        }
        //
        //        /**
        //         *
        //         * @param output
        //         * @param key
        //         * @param value
        //         * @return
        //         * @return return the specified {@code output}
        //         */
        //        public static <K, V, M extends Map<K, V>> M put(final M output, K key, final V value) {
        //            output.put(key, value);
        //            return output;
        //        }
        //
        //        /**
        //         *
        //         * @param output
        //         * @param entryToAdd
        //         * @return
        //         * @return return the specified {@code output}
        //         */
        //        public static <K, V, M extends Map<K, V>> M put(final M output, final Map.Entry<? extends K, ? extends V> entryToAdd) {
        //            output.put(entryToAdd.getKey(), entryToAdd.getValue());
        //            return output;
        //        }
        //
        //        /**
        //         *
        //         * @param output
        //         * @param entriesToAdd
        //         * @return
        //         * @return return the specified {@code output}
        //         */
        //        public static <K, V, M extends Map<K, V>> M putAll(final M output, final Map<? extends K, ? extends V> entriesToAdd) {
        //            if (entriesToAdd == null || entriesToAdd.size() == 0) {
        //                return output;
        //            }
        //
        //            output.putAll(entriesToAdd);
        //            return output;
        //        }
        //
        //        /**
        //         *
        //         * @param output
        //         * @param key
        //         * @param value
        //         * @return
        //         * @return return the specified {@code output}
        //         */
        //        public static <K, V, M extends Map<K, V>> M putIfAbsent(final M output, K key, final V value) {
        //            if (!output.containsKey(key)) {
        //                output.put(key, value);
        //            }
        //
        //            return output;
        //        }
        //
        //        /**
        //         *
        //         * @param output
        //         * @param entryToAdd
        //         * @return
        //         * @return return the specified {@code output}
        //         */
        //        public static <K, V, M extends Map<K, V>> M putIfAbsent(final M output, final Map.Entry<? extends K, ? extends V> entryToAdd) {
        //            if (!output.containsKey(entryToAdd.getKey())) {
        //                output.put(entryToAdd.getKey(), entryToAdd.getValue());
        //            }
        //            return output;
        //        }
        //
        //        /**
        //         *
        //         * @param output
        //         * @param key
        //         * @param oldValue
        //         * @param newValue
        //         * @return return the specified {@code output}
        //         */
        //        public static <K, V, M extends Map<K, V>> M replace(final M output, final K key, final V newValue) {
        //            if (output == null || output.size() == 0) {
        //                return output;
        //            }
        //
        //            final V curValue = output.get(key);
        //
        //            if ((curValue != null || output.containsKey(key))) {
        //                output.put(key, newValue);
        //            }
        //
        //            return output;
        //        }
        //
        //        /**
        //         *
        //         * @param output
        //         * @param key
        //         * @param oldValue
        //         * @param newValue
        //         * @return return the specified {@code output}
        //         */
        //        public static <K, V, M extends Map<K, V>> M replace(final M output, final K key, final V oldValue, final V newValue) {
        //            if (output == null || output.size() == 0) {
        //                return output;
        //            }
        //
        //            final V curValue = output.get(key);
        //
        //            if ((curValue != null || output.containsKey(key)) && N.equals(curValue, oldValue)) {
        //                output.put(key, newValue);
        //            }
        //
        //            return output;
        //        }
        //
        //        /**
        //         *
        //         * @param output
        //         * @param key
        //         * @return
        //         * @return return the specified {@code output}
        //         */
        //        public static <K, V, M extends Map<K, V>> M remove(final M output, final Object key) {
        //            if (output == null || output.size() == 0) {
        //                return output;
        //            }
        //
        //            output.remove(key);
        //            return output;
        //        }
        //
        //        /**
        //         *
        //         * @param output
        //         * @param key
        //         * @return
        //         * @return return the specified {@code output}
        //         */
        //        public static <K, V, M extends Map<K, V>> M remove(final M output, final Map.Entry<?, ?> entryToRemove) {
        //            if (output == null || output.size() == 0) {
        //                return output;
        //            }
        //
        //            if (N.equals(output.get(entryToRemove.getKey()), entryToRemove.getValue())) {
        //                output.remove(entryToRemove.getKey());
        //            }
        //
        //            return output;
        //        }
        //
        //        /**
        //         *
        //         * @param output
        //         * @param key
        //         * @param value
        //         * @return
        //         * @return return the specified {@code output}
        //         */
        //        public static <K, V, M extends Map<K, V>> M remove(final M output, final Object key, final Object value) {
        //            if (output == null || output.size() == 0) {
        //                return output;
        //            }
        //
        //            if (N.equals(output.get(key), value)) {
        //                output.remove(key);
        //            }
        //
        //            return output;
        //        }
        //
        //        /**
        //         *
        //         * @param output
        //         * @param keys
        //         * @return
        //         * @return return the specified {@code output}
        //         */
        //        public static <K, V, M extends Map<K, V>> M removeAll(final M output, final Collection<?> keys) {
        //            if (output == null || output.size() == 0 || keys == null || keys.size() == 0) {
        //                return output;
        //            }
        //
        //            for (Object key : keys) {
        //                output.remove(key);
        //            }
        //
        //            return output;
        //        }
        //
        //        /**
        //         *
        //         * @param output
        //         * @param entriesToRemove
        //         * @return
        //         * @return return the specified {@code output}
        //         */
        //        public static <K, V, M extends Map<K, V>> M removeAll(final M output, final Map<?, ?> entriesToRemove) {
        //            if (output == null || output.size() == 0 || entriesToRemove == null || entriesToRemove.size() == 0) {
        //                return output;
        //            }
        //
        //            for (Map.Entry<?, ?> entry : entriesToRemove.entrySet()) {
        //                if (N.equals(output.get(entry.getKey()), entry.getValue())) {
        //                    output.remove(entry.getKey());
        //                }
        //            }
        //
        //            return output;
        //        }
    }
}
