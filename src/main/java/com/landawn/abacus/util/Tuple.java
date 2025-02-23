/*
 * Copyright (c) 2017, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.u.Optional;

@com.landawn.abacus.annotation.Immutable
@SuppressWarnings({ "java:S116", "java:S117" })
public abstract class Tuple<TP> implements Immutable {

    Tuple() {
    }

    public abstract int arity();

    public abstract boolean anyNull();

    public abstract boolean allNull();

    /**
     *
     * @param valueToFind
     * @return
     */
    public abstract boolean contains(final Object valueToFind);

    public abstract Object[] toArray();

    /**
     *
     * @param <A>
     * @param a
     * @return
     */
    public abstract <A> A[] toArray(A[] a);

    /**
     *
     * @param <E>
     * @param consumer
     * @throws E the e
     */
    public abstract <E extends Exception> void forEach(Throwables.Consumer<?, E> consumer) throws E;

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void accept(final Throwables.Consumer<? super TP, E> action) throws E {
        action.accept((TP) this);
    }

    /**
     *
     * @param <R>
     * @param <E>
     * @param mapper
     * @return
     * @throws E the e
     */
    public <R, E extends Exception> R map(final Throwables.Function<? super TP, R, E> mapper) throws E {
        return mapper.apply((TP) this);
    }

    /**
     *
     * @param <E>
     * @param predicate
     * @return
     * @throws E the e
     */
    @Beta
    public <E extends Exception> Optional<TP> filter(final Throwables.Predicate<? super TP, E> predicate) throws E {
        return predicate.test((TP) this) ? Optional.of((TP) this) : Optional.empty();
    }

    //    /**
    //     *
    //     *
    //     * @return
    //     * @deprecated unlikely to be useful. It's marked to be removed.
    //     */
    //    @Deprecated
    //    @Beta
    //    public Stream<TP> stream() {
    //        return Stream.of((TP) this);
    //    }
    //
    //    /**
    //     *
    //     *
    //     * @param <T>
    //     * @param <E>
    //     * @param func
    //     * @return
    //     * @throws E
    //     * @deprecated unlikely to be useful. It's marked to be removed.
    //     */
    //    @Deprecated
    //    @Beta
    //    public <T, E extends Exception> Stream<T> stream(final Throwables.Function<? super TP, Stream<T>, E> func) throws E {
    //        return func.apply((TP) this);
    //    }
    //
    //    /**
    //     *
    //     * @deprecated {@code Optional} is misused. It's marked to be removed.
    //     */
    //    @Deprecated
    //    @Beta
    //    public Optional<TP> toOptional() {
    //        return Optional.of((TP) this);
    //    }

    /**
     *
     * @param <T1>
     * @param _1 the 1
     * @return
     */
    public static <T1> Tuple1<T1> of(final T1 _1) {
        return new Tuple1<>(_1);
    }

    /**
     *
     * @param <T1>
     * @param <T2>
     * @param _1 the 1
     * @param _2 the 2
     * @return
     */
    public static <T1, T2> Tuple2<T1, T2> of(final T1 _1, final T2 _2) {
        return new Tuple2<>(_1, _2);
    }

    /**
     *
     * @param <T1>
     * @param <T2>
     * @param <T3>
     * @param _1 the 1
     * @param _2 the 2
     * @param _3 the 3
     * @return
     */
    public static <T1, T2, T3> Tuple3<T1, T2, T3> of(final T1 _1, final T2 _2, final T3 _3) {
        return new Tuple3<>(_1, _2, _3);
    }

    /**
     *
     * @param <T1>
     * @param <T2>
     * @param <T3>
     * @param <T4>
     * @param _1 the 1
     * @param _2 the 2
     * @param _3 the 3
     * @param _4 the 4
     * @return
     */
    public static <T1, T2, T3, T4> Tuple4<T1, T2, T3, T4> of(final T1 _1, final T2 _2, final T3 _3, final T4 _4) {
        return new Tuple4<>(_1, _2, _3, _4);
    }

    /**
     *
     * @param <T1>
     * @param <T2>
     * @param <T3>
     * @param <T4>
     * @param <T5>
     * @param _1 the 1
     * @param _2 the 2
     * @param _3 the 3
     * @param _4 the 4
     * @param _5 the 5
     * @return
     */
    public static <T1, T2, T3, T4, T5> Tuple5<T1, T2, T3, T4, T5> of(final T1 _1, final T2 _2, final T3 _3, final T4 _4, final T5 _5) {
        return new Tuple5<>(_1, _2, _3, _4, _5);
    }

    /**
     *
     * @param <T1>
     * @param <T2>
     * @param <T3>
     * @param <T4>
     * @param <T5>
     * @param <T6>
     * @param _1 the 1
     * @param _2 the 2
     * @param _3 the 3
     * @param _4 the 4
     * @param _5 the 5
     * @param _6 the 6
     * @return
     */
    public static <T1, T2, T3, T4, T5, T6> Tuple6<T1, T2, T3, T4, T5, T6> of(final T1 _1, final T2 _2, final T3 _3, final T4 _4, final T5 _5, final T6 _6) {
        return new Tuple6<>(_1, _2, _3, _4, _5, _6);
    }

    /**
     *
     * @param <T1>
     * @param <T2>
     * @param <T3>
     * @param <T4>
     * @param <T5>
     * @param <T6>
     * @param <T7>
     * @param _1 the 1
     * @param _2 the 2
     * @param _3 the 3
     * @param _4 the 4
     * @param _5 the 5
     * @param _6 the 6
     * @param _7 the 7
     * @return
     */
    public static <T1, T2, T3, T4, T5, T6, T7> Tuple7<T1, T2, T3, T4, T5, T6, T7> of(final T1 _1, final T2 _2, final T3 _3, final T4 _4, final T5 _5,
            final T6 _6, final T7 _7) {
        return new Tuple7<>(_1, _2, _3, _4, _5, _6, _7);
    }

    /**
     *
     * @param <T1>
     * @param <T2>
     * @param <T3>
     * @param <T4>
     * @param <T5>
     * @param <T6>
     * @param <T7>
     * @param <T8>
     * @param _1 the 1
     * @param _2 the 2
     * @param _3 the 3
     * @param _4 the 4
     * @param _5 the 5
     * @param _6 the 6
     * @param _7 the 7
     * @param _8 the 8
     * @return
     * @deprecated you should consider using <code>class Some Bean/Record class: MyRecord { final T1 propName1, final T2 propName2...}</code>
     */
    @Deprecated
    public static <T1, T2, T3, T4, T5, T6, T7, T8> Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> of(final T1 _1, final T2 _2, final T3 _3, final T4 _4, final T5 _5,
            final T6 _6, final T7 _7, final T8 _8) {
        return new Tuple8<>(_1, _2, _3, _4, _5, _6, _7, _8);
    }

    /**
     *
     * @param <T1>
     * @param <T2>
     * @param <T3>
     * @param <T4>
     * @param <T5>
     * @param <T6>
     * @param <T7>
     * @param <T8>
     * @param <T9>
     * @param _1 the 1
     * @param _2 the 2
     * @param _3 the 3
     * @param _4 the 4
     * @param _5 the 5
     * @param _6 the 6
     * @param _7 the 7
     * @param _8 the 8
     * @param _9 the 9
     * @return
     * @deprecated you should consider using <code>class Some Bean/Record class: MyRecord { final T1 propName1, final T2 propName2...}</code>
     */
    @Deprecated
    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> of(final T1 _1, final T2 _2, final T3 _3, final T4 _4,
            final T5 _5, final T6 _6, final T7 _7, final T8 _8, final T9 _9) {
        return new Tuple9<>(_1, _2, _3, _4, _5, _6, _7, _8, _9);
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param entry
     * @return
     */
    @Beta
    public static <K, V> Tuple2<K, V> create(final Map.Entry<K, V> entry) {
        return new Tuple2<>(entry.getKey(), entry.getValue());
    }

    /**
     *
     * @param <TP>
     * @param a
     * @return
     */
    @Beta
    public static <TP extends Tuple<TP>> TP create(final Object[] a) {
        final int len = a == null ? 0 : a.length;

        Tuple<?> result = null;

        switch (len) {
            case 0:
                result = Tuple0.EMPTY;
                break;

            case 1:
                result = new Tuple1<>(a[0]);
                break;

            case 2:
                result = new Tuple2<>(a[0], a[1]);
                break;

            case 3:
                result = new Tuple3<>(a[0], a[1], a[2]);
                break;

            case 4:
                result = new Tuple4<>(a[0], a[1], a[2], a[3]);
                break;

            case 5:
                result = new Tuple5<>(a[0], a[1], a[2], a[3], a[4]);
                break;

            case 6:
                result = new Tuple6<>(a[0], a[1], a[2], a[3], a[4], a[5]);
                break;

            case 7:
                result = new Tuple7<>(a[0], a[1], a[2], a[3], a[4], a[5], a[6]);
                break;

            case 8:
                result = new Tuple8<>(a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7]);
                break;

            case 9:
                result = new Tuple9<>(a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8]);
                break;

            default:
                throw new IllegalArgumentException("Too many elements((" + a.length + ") to fill in Tuple.");
        }

        return (TP) result;
    }

    /**
     *
     * @param <TP>
     * @param c
     * @return
     */
    @Beta
    public static <TP extends Tuple<TP>> TP create(final Collection<?> c) {
        final int len = c == null ? 0 : c.size();
        final Iterator<?> iter = c == null ? null : c.iterator();

        Tuple<?> result = null;

        switch (len) {
            case 0:
                result = Tuple0.EMPTY;
                break;

            case 1:
                result = new Tuple1<>(iter.next());
                break;

            case 2:
                result = new Tuple2<>(iter.next(), iter.next());
                break;

            case 3:
                result = new Tuple3<>(iter.next(), iter.next(), iter.next());
                break;

            case 4:
                result = new Tuple4<>(iter.next(), iter.next(), iter.next(), iter.next());
                break;

            case 5:
                result = new Tuple5<>(iter.next(), iter.next(), iter.next(), iter.next(), iter.next());
                break;

            case 6:
                result = new Tuple6<>(iter.next(), iter.next(), iter.next(), iter.next(), iter.next(), iter.next());
                break;

            case 7:
                result = new Tuple7<>(iter.next(), iter.next(), iter.next(), iter.next(), iter.next(), iter.next(), iter.next());
                break;

            case 8:
                result = new Tuple8<>(iter.next(), iter.next(), iter.next(), iter.next(), iter.next(), iter.next(), iter.next(), iter.next());
                break;

            case 9:
                result = new Tuple9<>(iter.next(), iter.next(), iter.next(), iter.next(), iter.next(), iter.next(), iter.next(), iter.next(), iter.next());
                break;

            default:
                throw new IllegalArgumentException("Too many elements((" + c.size() + ") to fill in Tuple.");
        }

        return (TP) result;
    }

    /**
     *
     * @param <T>
     * @param tp
     * @return
     */
    @Beta
    public static <T> List<T> toList(final Tuple1<? extends T> tp) {
        return N.asList(tp._1);
    }

    /**
     *
     * @param <T>
     * @param tp
     * @return
     */
    @Beta
    public static <T> List<T> toList(final Tuple2<? extends T, ? extends T> tp) {
        return N.asList(tp._1, tp._2);
    }

    /**
     *
     * @param <T>
     * @param tp
     * @return
     */
    @Beta
    public static <T> List<T> toList(final Tuple3<? extends T, ? extends T, ? extends T> tp) {
        return N.asList(tp._1, tp._2, tp._3);
    }

    /**
     *
     * @param <T>
     * @param tp
     * @return
     */
    @Beta
    public static <T> List<T> toList(final Tuple4<? extends T, ? extends T, ? extends T, ? extends T> tp) {
        return N.asList(tp._1, tp._2, tp._3, tp._4);
    }

    /**
     *
     * @param <T>
     * @param tp
     * @return
     */
    @Beta
    public static <T> List<T> toList(final Tuple5<? extends T, ? extends T, ? extends T, ? extends T, ? extends T> tp) {
        return N.asList(tp._1, tp._2, tp._3, tp._4, tp._5);
    }

    /**
     *
     * @param <T>
     * @param tp
     * @return
     */
    @Beta
    public static <T> List<T> toList(final Tuple6<? extends T, ? extends T, ? extends T, ? extends T, ? extends T, ? extends T> tp) {
        return N.asList(tp._1, tp._2, tp._3, tp._4, tp._5, tp._6);
    }

    /**
     *
     * @param <T>
     * @param tp
     * @return
     */
    @Beta
    public static <T> List<T> toList(final Tuple7<? extends T, ? extends T, ? extends T, ? extends T, ? extends T, ? extends T, ? extends T> tp) {
        return N.asList(tp._1, tp._2, tp._3, tp._4, tp._5, tp._6, tp._7);
    }

    /**
     *
     * @param <T>
     * @param tp
     * @return
     */
    @Beta
    public static <T> List<T> toList(final Tuple8<? extends T, ? extends T, ? extends T, ? extends T, ? extends T, ? extends T, ? extends T, ? extends T> tp) {
        return N.asList(tp._1, tp._2, tp._3, tp._4, tp._5, tp._6, tp._7, tp._8);
    }

    /**
     *
     * @param <T>
     * @param tp
     * @return
     */
    @Beta
    public static <T> List<T> toList(
            final Tuple9<? extends T, ? extends T, ? extends T, ? extends T, ? extends T, ? extends T, ? extends T, ? extends T, ? extends T> tp) {
        return N.asList(tp._1, tp._2, tp._3, tp._4, tp._5, tp._6, tp._7, tp._8, tp._9);
    }

    /**
     *
     * @param <T1>
     * @param <T2>
     * @param <T3>
     * @param tp
     * @return
     */
    @Beta
    public static <T1, T2, T3> Tuple3<T1, T2, T3> flatten(final Tuple2<Tuple2<T1, T2>, T3> tp) {
        return new Tuple3<>(tp._1._1, tp._1._2, tp._2);
    }

    /**
     *
     * @param <T1>
     * @param <T2>
     * @param <T3>
     * @param <T4>
     * @param <T5>
     * @param tp
     * @return
     */
    @Beta
    public static <T1, T2, T3, T4, T5> Tuple5<T1, T2, T3, T4, T5> flatten(final Tuple3<Tuple3<T1, T2, T3>, T4, T5> tp) {
        return new Tuple5<>(tp._1._1, tp._1._2, tp._1._3, tp._2, tp._3);
    }

    //    public static <T1, T2, T3, T4> Tuple4<T1, T2, T3, T4> flatt(Tuple2<Tuple2<Tuple2<T1, T2>, T3>, T4> tp) {
    //        return new Tuple4<>(tp._1._1._1, tp._1._1._2, tp._1._2, tp._2);
    //    }

    /**
     * The Class Tuple1.
     *
     */
    @Beta
    static final class Tuple0 extends Tuple<Tuple0> {

        private static final Tuple0 EMPTY = new Tuple0();

        Tuple0() {
        }

        @Override
        public int arity() {
            return 0;
        }

        @Override
        public boolean anyNull() {
            return false;
        }

        @Override
        public boolean allNull() {
            return true;
        }

        @Override
        public boolean contains(final Object valueToFind) {
            return false;
        }

        @Override
        public Object[] toArray() {
            return N.EMPTY_OBJECT_ARRAY;
        }

        @Override
        public <A> A[] toArray(final A[] a) {
            return a;
        }

        @Override
        public <E extends Exception> void forEach(final Throwables.Consumer<?, E> consumer) throws IllegalArgumentException, E {
            N.checkArgNotNull(consumer);
            // do nothing.
        }

        @Override
        public String toString() {
            return Strings.STR_FOR_EMPTY_ARRAY;
        }
    }

    /**
     * The Class Tuple1.
     *
     * @param <T1>
     */
    public static final class Tuple1<T1> extends Tuple<Tuple1<T1>> {

        /** The  1. */
        public final T1 _1;

        /**
         * Instantiates a new tuple 1.
         */
        // For Kryo
        Tuple1() {
            this(null);
        }

        /**
         * Instantiates a new tuple 1.
         *
         * @param _1 the 1
         */
        Tuple1(final T1 _1) {
            this._1 = _1;
        }

        @Override
        public int arity() {
            return 1;
        }

        @Override
        public boolean anyNull() {
            return _1 == null;
        }

        @Override
        public boolean allNull() {
            return _1 == null;
        }

        /**
         *
         * @param valueToFind
         * @return
         */
        @Override
        public boolean contains(final Object valueToFind) {
            return N.equals(_1, valueToFind);
        }

        @Override
        public Object[] toArray() {
            return new Object[] { _1 };
        }

        /**
         *
         * @param <A>
         * @param a
         * @return
         */
        @Override
        public <A> A[] toArray(A[] a) {
            if (a.length < 1) {
                a = N.copyOf(a, 1);
            }

            a[0] = (A) _1;

            return a;
        }

        /**
         *
         * @param <E>
         * @param consumer
         * @throws E the e
         */
        @Override
        public <E extends Exception> void forEach(final Throwables.Consumer<?, E> consumer) throws E {
            final Throwables.Consumer<Object, E> objConsumer = (Throwables.Consumer<Object, E>) consumer;

            objConsumer.accept(_1);
        }

        //    /**
        //     *
        //     * @return
        //     */
        //    @Beta
        //    public Nullable<T1> toNullable() {
        //        return Nullable.of(_1);
        //    }

        @Override
        public int hashCode() {
            final int prime = 31;
            final int result = 1;
            return prime * result + N.hashCode(_1);
        }

        /**
         *
         * @param obj
         * @return
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj != null && obj.getClass().equals(Tuple1.class)) {
                final Tuple1<?> other = (Tuple1<?>) obj;

                return N.equals(_1, other._1);
            }

            return false;
        }

        @Override
        public String toString() {
            return "[" + N.toString(_1) + "]";
        }
    }

    /**
     * The Class Tuple2.
     *
     * @param <T1>
     * @param <T2>
     */
    public static final class Tuple2<T1, T2> extends Tuple<Tuple2<T1, T2>> {

        /** The  1. */
        public final T1 _1;

        /** The  2. */
        public final T2 _2;

        /**
         * Instantiates a new tuple 2.
         */
        // For Kryo
        Tuple2() {
            this(null, null);
        }

        /**
         * Instantiates a new tuple 2.
         *
         * @param _1 the 1
         * @param _2 the 2
         */
        Tuple2(final T1 _1, final T2 _2) {
            this._1 = _1;
            this._2 = _2;
        }

        @Override
        public int arity() {
            return 2;
        }

        @Override
        public boolean anyNull() {
            return _1 == null || _2 == null;
        }

        @Override
        public boolean allNull() {
            return _1 == null && _2 == null;
        }

        /**
         *
         * @param valueToFind
         * @return
         */
        @Override
        public boolean contains(final Object valueToFind) {
            return N.equals(_1, valueToFind) || N.equals(_2, valueToFind);
        }

        @Override
        public Object[] toArray() {
            return new Object[] { _1, _2 };
        }

        /**
         *
         * @param <A>
         * @param a
         * @return
         */
        @Override
        public <A> A[] toArray(A[] a) {
            if (a.length < 2) {
                a = N.copyOf(a, 2);
            }

            a[0] = (A) _1;
            a[1] = (A) _2;

            return a;
        }

        public Pair<T1, T2> toPair() {
            return Pair.of(_1, _2);
        }

        public ImmutableEntry<T1, T2> toEntry() {
            return ImmutableEntry.of(_1, _2);
        }

        public Tuple2<T2, T1> reverse() {
            return of(_2, _1); //NOSONAR
        }

        /**
         *
         * @param <E>
         * @param consumer
         * @throws E the e
         */
        @Override
        public <E extends Exception> void forEach(final Throwables.Consumer<?, E> consumer) throws E {
            final Throwables.Consumer<Object, E> objConsumer = (Throwables.Consumer<Object, E>) consumer;

            objConsumer.accept(_1);
            objConsumer.accept(_2);
        }

        /**
         *
         * @param <E>
         * @param action
         * @throws E the e
         */
        public <E extends Exception> void accept(final Throwables.BiConsumer<? super T1, ? super T2, E> action) throws E {
            action.accept(_1, _2);
        }

        /**
         *
         * @param <R>
         * @param <E>
         * @param mapper
         * @return
         * @throws E the e
         */
        public <R, E extends Exception> R map(final Throwables.BiFunction<? super T1, ? super T2, ? extends R, E> mapper) throws E {
            return mapper.apply(_1, _2);
        }

        /**
         *
         * @param <E>
         * @param predicate
         * @return
         * @throws E the e
         */
        public <E extends Exception> Optional<Tuple2<T1, T2>> filter(final Throwables.BiPredicate<? super T1, ? super T2, E> predicate) throws E {
            return predicate.test(_1, _2) ? Optional.of(this) : Optional.empty();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + N.hashCode(_1);
            return prime * result + N.hashCode(_2);
        }

        /**
         *
         * @param obj
         * @return
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj != null && obj.getClass().equals(Tuple2.class)) {
                final Tuple2<?, ?> other = (Tuple2<?, ?>) obj;

                return N.equals(_1, other._1) && N.equals(_2, other._2);
            }

            return false;
        }

        @Override
        public String toString() {
            return "[" + N.toString(_1) + ", " + N.toString(_2) + "]";
        }
    }

    /**
     * The Class Tuple3.
     *
     * @param <T1>
     * @param <T2>
     * @param <T3>
     */
    public static final class Tuple3<T1, T2, T3> extends Tuple<Tuple3<T1, T2, T3>> {

        /** The  1. */
        public final T1 _1;

        /** The  2. */
        public final T2 _2;

        /** The  3. */
        public final T3 _3;

        /**
         * Instantiates a new tuple 3.
         */
        // For Kryo
        Tuple3() {
            this(null, null, null);
        }

        /**
         * Instantiates a new tuple 3.
         *
         * @param _1 the 1
         * @param _2 the 2
         * @param _3 the 3
         */
        Tuple3(final T1 _1, final T2 _2, final T3 _3) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
        }

        @Override
        public int arity() {
            return 3;
        }

        @Override
        public boolean anyNull() {
            return _1 == null || _2 == null || _3 == null;
        }

        @Override
        public boolean allNull() {
            return _1 == null && _2 == null && _3 == null;
        }

        /**
         *
         * @param valueToFind
         * @return
         */
        @Override
        public boolean contains(final Object valueToFind) {
            return N.equals(_1, valueToFind) || N.equals(_2, valueToFind) || N.equals(_3, valueToFind);
        }

        @Override
        public Object[] toArray() {
            return new Object[] { _1, _2, _3 };
        }

        /**
         *
         * @param <A>
         * @param a
         * @return
         */
        @Override
        public <A> A[] toArray(A[] a) {
            if (a.length < 3) {
                a = N.copyOf(a, 3);
            }

            a[0] = (A) _1;
            a[1] = (A) _2;
            a[2] = (A) _3;

            return a;
        }

        public Triple<T1, T2, T3> toTriple() {
            return Triple.of(_1, _2, _3);
        }

        public Tuple3<T3, T2, T1> reverse() {
            return new Tuple3<>(_3, _2, _1);
        }

        /**
         *
         * @param <E>
         * @param consumer
         * @throws E the e
         */
        @Override
        public <E extends Exception> void forEach(final Throwables.Consumer<?, E> consumer) throws E {
            final Throwables.Consumer<Object, E> objConsumer = (Throwables.Consumer<Object, E>) consumer;

            objConsumer.accept(_1);
            objConsumer.accept(_2);
            objConsumer.accept(_3);
        }

        /**
         *
         * @param <E>
         * @param action
         * @throws E the e
         */
        public <E extends Exception> void accept(final Throwables.TriConsumer<? super T1, ? super T2, ? super T3, E> action) throws E {
            action.accept(_1, _2, _3);
        }

        /**
         *
         * @param <R>
         * @param <E>
         * @param mapper
         * @return
         * @throws E the e
         */
        public <R, E extends Exception> R map(final Throwables.TriFunction<? super T1, ? super T2, ? super T3, ? extends R, E> mapper) throws E {
            return mapper.apply(_1, _2, _3);
        }

        /**
         *
         * @param <E>
         * @param predicate
         * @return
         * @throws E the e
         */
        public <E extends Exception> Optional<Tuple3<T1, T2, T3>> filter(final Throwables.TriPredicate<? super T1, ? super T2, ? super T3, E> predicate)
                throws E {
            return predicate.test(_1, _2, _3) ? Optional.of(this) : Optional.empty();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + N.hashCode(_1);
            result = prime * result + N.hashCode(_2);
            return prime * result + N.hashCode(_3);
        }

        /**
         *
         * @param obj
         * @return
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj != null && obj.getClass().equals(Tuple3.class)) {
                final Tuple3<?, ?, ?> other = (Tuple3<?, ?, ?>) obj;

                return N.equals(_1, other._1) && N.equals(_2, other._2) && N.equals(_3, other._3);
            }

            return false;
        }

        @Override
        public String toString() {
            return "[" + N.toString(_1) + ", " + N.toString(_2) + ", " + N.toString(_3) + "]";
        }
    }

    /**
     * The Class Tuple4.
     *
     * @param <T1>
     * @param <T2>
     * @param <T3>
     * @param <T4>
     */
    public static final class Tuple4<T1, T2, T3, T4> extends Tuple<Tuple4<T1, T2, T3, T4>> {

        /** The  1. */
        public final T1 _1;

        /** The  2. */
        public final T2 _2;

        /** The  3. */
        public final T3 _3;

        /** The  4. */
        public final T4 _4;

        /**
         * Instantiates a new tuple 4.
         */
        // For Kryo
        Tuple4() {
            this(null, null, null, null);
        }

        /**
         * Instantiates a new tuple 4.
         *
         * @param _1 the 1
         * @param _2 the 2
         * @param _3 the 3
         * @param _4 the 4
         */
        Tuple4(final T1 _1, final T2 _2, final T3 _3, final T4 _4) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
        }

        @Override
        public int arity() {
            return 4;
        }

        @Override
        public boolean anyNull() {
            return _1 == null || _2 == null || _3 == null || _4 == null;
        }

        @Override
        public boolean allNull() {
            return _1 == null && _2 == null && _3 == null && _4 == null;
        }

        /**
         *
         * @param valueToFind
         * @return
         */
        @Override
        public boolean contains(final Object valueToFind) {
            return N.equals(_1, valueToFind) || N.equals(_2, valueToFind) || N.equals(_3, valueToFind) || N.equals(_4, valueToFind);
        }

        @Override
        public Object[] toArray() {
            return new Object[] { _1, _2, _3, _4 };
        }

        /**
         *
         * @param <A>
         * @param a
         * @return
         */
        @Override
        public <A> A[] toArray(A[] a) {
            if (a.length < 4) {
                a = N.copyOf(a, 4);
            }

            a[0] = (A) _1;
            a[1] = (A) _2;
            a[2] = (A) _3;
            a[3] = (A) _4;

            return a;
        }

        public Tuple4<T4, T3, T2, T1> reverse() {
            return new Tuple4<>(_4, _3, _2, _1);
        }

        /**
         *
         * @param <E>
         * @param consumer
         * @throws E the e
         */
        @Override
        public <E extends Exception> void forEach(final Throwables.Consumer<?, E> consumer) throws E {
            final Throwables.Consumer<Object, E> objConsumer = (Throwables.Consumer<Object, E>) consumer;

            objConsumer.accept(_1);
            objConsumer.accept(_2);
            objConsumer.accept(_3);
            objConsumer.accept(_4);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + N.hashCode(_1);
            result = prime * result + N.hashCode(_2);
            result = prime * result + N.hashCode(_3);
            return prime * result + N.hashCode(_4);
        }

        /**
         *
         * @param obj
         * @return
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj != null && obj.getClass().equals(Tuple4.class)) {
                final Tuple4<?, ?, ?, ?> other = (Tuple4<?, ?, ?, ?>) obj;

                return N.equals(_1, other._1) && N.equals(_2, other._2) && N.equals(_3, other._3) && N.equals(_4, other._4);
            }

            return false;
        }

        @Override
        public String toString() {
            return "[" + N.toString(_1) + ", " + N.toString(_2) + ", " + N.toString(_3) + ", " + N.toString(_4) + "]";
        }
    }

    /**
     * The Class Tuple5.
     *
     * @param <T1>
     * @param <T2>
     * @param <T3>
     * @param <T4>
     * @param <T5>
     */
    public static final class Tuple5<T1, T2, T3, T4, T5> extends Tuple<Tuple5<T1, T2, T3, T4, T5>> {

        /** The  1. */
        public final T1 _1;

        /** The  2. */
        public final T2 _2;

        /** The  3. */
        public final T3 _3;

        /** The  4. */
        public final T4 _4;

        /** The  5. */
        public final T5 _5;

        /**
         * Instantiates a new tuple 5.
         */
        // For Kryo
        Tuple5() {
            this(null, null, null, null, null);
        }

        /**
         * Instantiates a new tuple 5.
         *
         * @param _1 the 1
         * @param _2 the 2
         * @param _3 the 3
         * @param _4 the 4
         * @param _5 the 5
         */
        Tuple5(final T1 _1, final T2 _2, final T3 _3, final T4 _4, final T5 _5) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
            this._5 = _5;
        }

        @Override
        public int arity() {
            return 5;
        }

        @Override
        public boolean anyNull() {
            return _1 == null || _2 == null || _3 == null || _4 == null || _5 == null;
        }

        @Override
        public boolean allNull() {
            return _1 == null && _2 == null && _3 == null && _4 == null && _5 == null;
        }

        /**
         *
         * @param valueToFind
         * @return
         */
        @Override
        public boolean contains(final Object valueToFind) {
            return N.equals(_1, valueToFind) || N.equals(_2, valueToFind) || N.equals(_3, valueToFind) || N.equals(_4, valueToFind)
                    || N.equals(_5, valueToFind);
        }

        @Override
        public Object[] toArray() {
            return new Object[] { _1, _2, _3, _4, _5 };
        }

        /**
         *
         * @param <A>
         * @param a
         * @return
         */
        @Override
        public <A> A[] toArray(A[] a) {
            if (a.length < 5) {
                a = N.copyOf(a, 5);
            }

            a[0] = (A) _1;
            a[1] = (A) _2;
            a[2] = (A) _3;
            a[3] = (A) _4;
            a[4] = (A) _5;

            return a;
        }

        public Tuple5<T5, T4, T3, T2, T1> reverse() {
            return new Tuple5<>(_5, _4, _3, _2, _1);
        }

        /**
         *
         * @param <E>
         * @param consumer
         * @throws E the e
         */
        @Override
        public <E extends Exception> void forEach(final Throwables.Consumer<?, E> consumer) throws E {
            final Throwables.Consumer<Object, E> objConsumer = (Throwables.Consumer<Object, E>) consumer;

            objConsumer.accept(_1);
            objConsumer.accept(_2);
            objConsumer.accept(_3);
            objConsumer.accept(_4);
            objConsumer.accept(_5);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + N.hashCode(_1);
            result = prime * result + N.hashCode(_2);
            result = prime * result + N.hashCode(_3);
            result = prime * result + N.hashCode(_4);
            return prime * result + N.hashCode(_5);
        }

        /**
         *
         * @param obj
         * @return
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj != null && obj.getClass().equals(Tuple5.class)) {
                final Tuple5<?, ?, ?, ?, ?> other = (Tuple5<?, ?, ?, ?, ?>) obj;

                return N.equals(_1, other._1) && N.equals(_2, other._2) && N.equals(_3, other._3) && N.equals(_4, other._4) && N.equals(_5, other._5);
            }

            return false;
        }

        @Override
        public String toString() {
            return "[" + N.toString(_1) + ", " + N.toString(_2) + ", " + N.toString(_3) + ", " + N.toString(_4) + ", " + N.toString(_5) + "]";
        }
    }

    /**
     * The Class Tuple6.
     *
     * @param <T1>
     * @param <T2>
     * @param <T3>
     * @param <T4>
     * @param <T5>
     * @param <T6>
     */
    public static final class Tuple6<T1, T2, T3, T4, T5, T6> extends Tuple<Tuple6<T1, T2, T3, T4, T5, T6>> {

        /** The  1. */
        public final T1 _1;

        /** The  2. */
        public final T2 _2;

        /** The  3. */
        public final T3 _3;

        /** The  4. */
        public final T4 _4;

        /** The  5. */
        public final T5 _5;

        /** The  6. */
        public final T6 _6;

        /**
         * Instantiates a new tuple 6.
         */
        // For Kryo
        Tuple6() {
            this(null, null, null, null, null, null);
        }

        /**
         * Instantiates a new tuple 6.
         *
         * @param _1 the 1
         * @param _2 the 2
         * @param _3 the 3
         * @param _4 the 4
         * @param _5 the 5
         * @param _6 the 6
         */
        Tuple6(final T1 _1, final T2 _2, final T3 _3, final T4 _4, final T5 _5, final T6 _6) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
            this._5 = _5;
            this._6 = _6;
        }

        @Override
        public int arity() {
            return 6;
        }

        @Override
        public boolean anyNull() {
            return _1 == null || _2 == null || _3 == null || _4 == null || _5 == null || _6 == null;
        }

        @Override
        public boolean allNull() {
            return _1 == null && _2 == null && _3 == null && _4 == null && _5 == null && _6 == null;
        }

        /**
         *
         * @param valueToFind
         * @return
         */
        @Override
        public boolean contains(final Object valueToFind) {
            return N.equals(_1, valueToFind) || N.equals(_2, valueToFind) || N.equals(_3, valueToFind) || N.equals(_4, valueToFind) || N.equals(_5, valueToFind)
                    || N.equals(_6, valueToFind);
        }

        @Override
        public Object[] toArray() {
            return new Object[] { _1, _2, _3, _4, _5, _6 };
        }

        /**
         *
         * @param <A>
         * @param a
         * @return
         */
        @Override
        public <A> A[] toArray(A[] a) {
            if (a.length < 6) {
                a = N.copyOf(a, 6);
            }

            a[0] = (A) _1;
            a[1] = (A) _2;
            a[2] = (A) _3;
            a[3] = (A) _4;
            a[4] = (A) _5;
            a[5] = (A) _6;

            return a;
        }

        public Tuple6<T6, T5, T4, T3, T2, T1> reverse() {
            return new Tuple6<>(_6, _5, _4, _3, _2, _1);
        }

        /**
         *
         * @param <E>
         * @param consumer
         * @throws E the e
         */
        @Override
        public <E extends Exception> void forEach(final Throwables.Consumer<?, E> consumer) throws E {
            final Throwables.Consumer<Object, E> objConsumer = (Throwables.Consumer<Object, E>) consumer;

            objConsumer.accept(_1);
            objConsumer.accept(_2);
            objConsumer.accept(_3);
            objConsumer.accept(_4);
            objConsumer.accept(_5);
            objConsumer.accept(_6);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + N.hashCode(_1);
            result = prime * result + N.hashCode(_2);
            result = prime * result + N.hashCode(_3);
            result = prime * result + N.hashCode(_4);
            result = prime * result + N.hashCode(_5);
            return prime * result + N.hashCode(_6);
        }

        /**
         *
         * @param obj
         * @return
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj != null && obj.getClass().equals(Tuple6.class)) {
                final Tuple6<?, ?, ?, ?, ?, ?> other = (Tuple6<?, ?, ?, ?, ?, ?>) obj;

                return N.equals(_1, other._1) && N.equals(_2, other._2) && N.equals(_3, other._3) && N.equals(_4, other._4) && N.equals(_5, other._5)
                        && N.equals(_6, other._6);
            }

            return false;
        }

        @Override
        public String toString() {
            return "[" + N.toString(_1) + ", " + N.toString(_2) + ", " + N.toString(_3) + ", " + N.toString(_4) + ", " + N.toString(_5) + ", " + N.toString(_6)
                    + "]";
        }
    }

    /**
     * The Class Tuple7.
     *
     * @param <T1>
     * @param <T2>
     * @param <T3>
     * @param <T4>
     * @param <T5>
     * @param <T6>
     * @param <T7>
     */
    public static final class Tuple7<T1, T2, T3, T4, T5, T6, T7> extends Tuple<Tuple7<T1, T2, T3, T4, T5, T6, T7>> {

        /** The  1. */
        public final T1 _1;

        /** The  2. */
        public final T2 _2;

        /** The  3. */
        public final T3 _3;

        /** The  4. */
        public final T4 _4;

        /** The  5. */
        public final T5 _5;

        /** The  6. */
        public final T6 _6;

        /** The  7. */
        public final T7 _7;

        /**
         * Instantiates a new tuple 7.
         */
        // For Kryo
        Tuple7() {
            this(null, null, null, null, null, null, null);
        }

        /**
         * Instantiates a new tuple 7.
         *
         * @param _1 the 1
         * @param _2 the 2
         * @param _3 the 3
         * @param _4 the 4
         * @param _5 the 5
         * @param _6 the 6
         * @param _7 the 7
         */
        Tuple7(final T1 _1, final T2 _2, final T3 _3, final T4 _4, final T5 _5, final T6 _6, final T7 _7) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
            this._5 = _5;
            this._6 = _6;
            this._7 = _7;
        }

        @Override
        public int arity() {
            return 7;
        }

        @Override
        public boolean anyNull() {
            return _1 == null || _2 == null || _3 == null || _4 == null || _5 == null || _6 == null || _7 == null;
        }

        @Override
        public boolean allNull() {
            return _1 == null && _2 == null && _3 == null && _4 == null && _5 == null && _6 == null && _7 == null;
        }

        /**
         *
         * @param valueToFind
         * @return
         */
        @Override
        public boolean contains(final Object valueToFind) {
            return N.equals(_1, valueToFind) || N.equals(_2, valueToFind) || N.equals(_3, valueToFind) || N.equals(_4, valueToFind) || N.equals(_5, valueToFind)
                    || N.equals(_6, valueToFind) || N.equals(_7, valueToFind);
        }

        @Override
        public Object[] toArray() {
            return new Object[] { _1, _2, _3, _4, _5, _6, _7 };
        }

        /**
         *
         * @param <A>
         * @param a
         * @return
         */
        @Override
        public <A> A[] toArray(A[] a) {
            if (a.length < 7) {
                a = N.copyOf(a, 7);
            }

            a[0] = (A) _1;
            a[1] = (A) _2;
            a[2] = (A) _3;
            a[3] = (A) _4;
            a[4] = (A) _5;
            a[5] = (A) _6;
            a[6] = (A) _7;

            return a;
        }

        public Tuple7<T7, T6, T5, T4, T3, T2, T1> reverse() {
            return new Tuple7<>(_7, _6, _5, _4, _3, _2, _1);
        }

        /**
         *
         * @param <E>
         * @param consumer
         * @throws E the e
         */
        @Override
        public <E extends Exception> void forEach(final Throwables.Consumer<?, E> consumer) throws E {
            final Throwables.Consumer<Object, E> objConsumer = (Throwables.Consumer<Object, E>) consumer;

            objConsumer.accept(_1);
            objConsumer.accept(_2);
            objConsumer.accept(_3);
            objConsumer.accept(_4);
            objConsumer.accept(_5);
            objConsumer.accept(_6);
            objConsumer.accept(_7);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + N.hashCode(_1);
            result = prime * result + N.hashCode(_2);
            result = prime * result + N.hashCode(_3);
            result = prime * result + N.hashCode(_4);
            result = prime * result + N.hashCode(_5);
            result = prime * result + N.hashCode(_6);
            return prime * result + N.hashCode(_7);
        }

        /**
         *
         * @param obj
         * @return
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj != null && obj.getClass().equals(Tuple7.class)) {
                final Tuple7<?, ?, ?, ?, ?, ?, ?> other = (Tuple7<?, ?, ?, ?, ?, ?, ?>) obj;

                return N.equals(_1, other._1) && N.equals(_2, other._2) && N.equals(_3, other._3) && N.equals(_4, other._4) && N.equals(_5, other._5)
                        && N.equals(_6, other._6) && N.equals(_7, other._7);
            }

            return false;
        }

        @Override
        public String toString() {
            return "[" + N.toString(_1) + ", " + N.toString(_2) + ", " + N.toString(_3) + ", " + N.toString(_4) + ", " + N.toString(_5) + ", " + N.toString(_6)
                    + ", " + N.toString(_7) + "]";
        }
    }

    /**
     * The Class Tuple8.
     *
     * @param <T1>
     * @param <T2>
     * @param <T3>
     * @param <T4>
     * @param <T5>
     * @param <T6>
     * @param <T7>
     * @param <T8>
     */
    public static final class Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> extends Tuple<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> {

        /** The  1. */
        public final T1 _1;

        /** The  2. */
        public final T2 _2;

        /** The  3. */
        public final T3 _3;

        /** The  4. */
        public final T4 _4;

        /** The  5. */
        public final T5 _5;

        /** The  6. */
        public final T6 _6;

        /** The  7. */
        public final T7 _7;

        /** The  8. */
        public final T8 _8;

        /**
         * Instantiates a new tuple 8.
         */
        // For Kryo
        Tuple8() {
            this(null, null, null, null, null, null, null, null);
        }

        /**
         * Instantiates a new tuple 8.
         *
         * @param _1 the 1
         * @param _2 the 2
         * @param _3 the 3
         * @param _4 the 4
         * @param _5 the 5
         * @param _6 the 6
         * @param _7 the 7
         * @param _8 the 8
         */
        Tuple8(final T1 _1, final T2 _2, final T3 _3, final T4 _4, final T5 _5, final T6 _6, final T7 _7, final T8 _8) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
            this._5 = _5;
            this._6 = _6;
            this._7 = _7;
            this._8 = _8;
        }

        //    public static <T1, T2, T3, T4, T5, T6, T7, T8> Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> flatten(Tuple2<Tuple7<T1, T2, T3, T4, T5, T6, T7>, T8> tp) {
        //        return new Tuple8<>(tp._1._1, tp._1._2, tp._1._3, tp._1._4, tp._1._5, tp._1._6, tp._1._7, tp._2);
        //    }

        @Override
        public int arity() {
            return 8;
        }

        @Override
        public boolean anyNull() {
            return _1 == null || _2 == null || _3 == null || _4 == null || _5 == null || _6 == null || _7 == null || _8 == null;
        }

        @Override
        public boolean allNull() {
            return _1 == null && _2 == null && _3 == null && _4 == null && _5 == null && _6 == null && _7 == null && _8 == null;
        }

        /**
         *
         * @param valueToFind
         * @return
         */
        @Override
        public boolean contains(final Object valueToFind) {
            return N.equals(_1, valueToFind) || N.equals(_2, valueToFind) || N.equals(_3, valueToFind) || N.equals(_4, valueToFind) || N.equals(_5, valueToFind)
                    || N.equals(_6, valueToFind) || N.equals(_7, valueToFind) || N.equals(_8, valueToFind);
        }

        @Override
        public Object[] toArray() {
            return new Object[] { _1, _2, _3, _4, _5, _6, _7, _8 };
        }

        /**
         *
         * @param <A>
         * @param a
         * @return
         */
        @Override
        public <A> A[] toArray(A[] a) {
            if (a.length < 8) {
                a = N.copyOf(a, 8);
            }

            a[0] = (A) _1;
            a[1] = (A) _2;
            a[2] = (A) _3;
            a[3] = (A) _4;
            a[4] = (A) _5;
            a[5] = (A) _6;
            a[6] = (A) _7;
            a[7] = (A) _8;

            return a;
        }

        public Tuple8<T8, T7, T6, T5, T4, T3, T2, T1> reverse() {
            return new Tuple8<>(_8, _7, _6, _5, _4, _3, _2, _1);
        }

        /**
         *
         * @param <E>
         * @param consumer
         * @throws E the e
         */
        @Override
        public <E extends Exception> void forEach(final Throwables.Consumer<?, E> consumer) throws E {
            final Throwables.Consumer<Object, E> objConsumer = (Throwables.Consumer<Object, E>) consumer;

            objConsumer.accept(_1);
            objConsumer.accept(_2);
            objConsumer.accept(_3);
            objConsumer.accept(_4);
            objConsumer.accept(_5);
            objConsumer.accept(_6);
            objConsumer.accept(_7);
            objConsumer.accept(_8);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + N.hashCode(_1);
            result = prime * result + N.hashCode(_2);
            result = prime * result + N.hashCode(_3);
            result = prime * result + N.hashCode(_4);
            result = prime * result + N.hashCode(_5);
            result = prime * result + N.hashCode(_6);
            result = prime * result + N.hashCode(_7);
            return prime * result + N.hashCode(_8);
        }

        /**
         *
         * @param obj
         * @return
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj != null && obj.getClass().equals(Tuple8.class)) {
                final Tuple8<?, ?, ?, ?, ?, ?, ?, ?> other = (Tuple8<?, ?, ?, ?, ?, ?, ?, ?>) obj;

                return N.equals(_1, other._1) && N.equals(_2, other._2) && N.equals(_3, other._3) && N.equals(_4, other._4) && N.equals(_5, other._5)
                        && N.equals(_6, other._6) && N.equals(_7, other._7) && N.equals(_8, other._8);
            }

            return false;
        }

        @Override
        public String toString() {
            return "[" + N.toString(_1) + ", " + N.toString(_2) + ", " + N.toString(_3) + ", " + N.toString(_4) + ", " + N.toString(_5) + ", " + N.toString(_6)
                    + ", " + N.toString(_7) + ", " + N.toString(_8) + "]";
        }
    }

    /**
     * The Class Tuple9.
     *
     * @param <T1>
     * @param <T2>
     * @param <T3>
     * @param <T4>
     * @param <T5>
     * @param <T6>
     * @param <T7>
     * @param <T8>
     * @param <T9>
     */
    public static final class Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> extends Tuple<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> {

        /** The  1. */
        public final T1 _1;

        /** The  2. */
        public final T2 _2;

        /** The  3. */
        public final T3 _3;

        /** The  4. */
        public final T4 _4;

        /** The  5. */
        public final T5 _5;

        /** The  6. */
        public final T6 _6;

        /** The  7. */
        public final T7 _7;

        /** The  8. */
        public final T8 _8;

        /** The  9. */
        public final T9 _9;

        /**
         * Instantiates a new tuple 9.
         */
        // For Kryo
        Tuple9() {
            this(null, null, null, null, null, null, null, null, null);
        }

        /**
         * Instantiates a new tuple 9.
         *
         * @param _1 the 1
         * @param _2 the 2
         * @param _3 the 3
         * @param _4 the 4
         * @param _5 the 5
         * @param _6 the 6
         * @param _7 the 7
         * @param _8 the 8
         * @param _9 the 9
         */
        Tuple9(final T1 _1, final T2 _2, final T3 _3, final T4 _4, final T5 _5, final T6 _6, final T7 _7, final T8 _8, final T9 _9) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
            this._5 = _5;
            this._6 = _6;
            this._7 = _7;
            this._8 = _8;
            this._9 = _9;
        }

        //    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> flatten(
        //            Tuple2<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>, T9> tp) {
        //        return new Tuple9<>(tp._1._1, tp._1._2, tp._1._3, tp._1._4, tp._1._5, tp._1._6, tp._1._7, tp._1._8, tp._2);
        //    }

        @Override
        public int arity() {
            return 9;
        }

        @Override
        public boolean anyNull() {
            return _1 == null || _2 == null || _3 == null || _4 == null || _5 == null || _6 == null || _7 == null || _8 == null || _9 == null;
        }

        @Override
        public boolean allNull() {
            return _1 == null && _2 == null && _3 == null && _4 == null && _5 == null && _6 == null && _7 == null && _8 == null && _9 == null;
        }

        /**
         *
         * @param valueToFind
         * @return
         */
        @Override
        public boolean contains(final Object valueToFind) {
            return N.equals(_1, valueToFind) || N.equals(_2, valueToFind) || N.equals(_3, valueToFind) || N.equals(_4, valueToFind) || N.equals(_5, valueToFind)
                    || N.equals(_6, valueToFind) || N.equals(_7, valueToFind) || N.equals(_8, valueToFind) || N.equals(_9, valueToFind);
        }

        @Override
        public Object[] toArray() {
            return new Object[] { _1, _2, _3, _4, _5, _6, _7, _8, _9 };
        }

        /**
         *
         * @param <A>
         * @param a
         * @return
         */
        @Override
        public <A> A[] toArray(A[] a) {
            if (a.length < 9) {
                a = N.copyOf(a, 9);
            }

            a[0] = (A) _1;
            a[1] = (A) _2;
            a[2] = (A) _3;
            a[3] = (A) _4;
            a[4] = (A) _5;
            a[5] = (A) _6;
            a[6] = (A) _7;
            a[7] = (A) _8;
            a[8] = (A) _9;

            return a;
        }

        public Tuple9<T9, T8, T7, T6, T5, T4, T3, T2, T1> reverse() {
            return new Tuple9<>(_9, _8, _7, _6, _5, _4, _3, _2, _1);
        }

        /**
         *
         * @param <E>
         * @param consumer
         * @throws E the e
         */
        @Override
        public <E extends Exception> void forEach(final Throwables.Consumer<?, E> consumer) throws E {
            final Throwables.Consumer<Object, E> objConsumer = (Throwables.Consumer<Object, E>) consumer;

            objConsumer.accept(_1);
            objConsumer.accept(_2);
            objConsumer.accept(_3);
            objConsumer.accept(_4);
            objConsumer.accept(_5);
            objConsumer.accept(_6);
            objConsumer.accept(_7);
            objConsumer.accept(_8);
            objConsumer.accept(_9);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + N.hashCode(_1);
            result = prime * result + N.hashCode(_2);
            result = prime * result + N.hashCode(_3);
            result = prime * result + N.hashCode(_4);
            result = prime * result + N.hashCode(_5);
            result = prime * result + N.hashCode(_6);
            result = prime * result + N.hashCode(_7);
            result = prime * result + N.hashCode(_8);
            return prime * result + N.hashCode(_9);
        }

        /**
         *
         * @param obj
         * @return
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj != null && obj.getClass().equals(Tuple9.class)) {
                final Tuple9<?, ?, ?, ?, ?, ?, ?, ?, ?> other = (Tuple9<?, ?, ?, ?, ?, ?, ?, ?, ?>) obj;

                return N.equals(_1, other._1) && N.equals(_2, other._2) && N.equals(_3, other._3) && N.equals(_4, other._4) && N.equals(_5, other._5)
                        && N.equals(_6, other._6) && N.equals(_7, other._7) && N.equals(_8, other._8) && N.equals(_9, other._9);
            }

            return false;
        }

        @Override
        public String toString() {
            return "[" + N.toString(_1) + ", " + N.toString(_2) + ", " + N.toString(_3) + ", " + N.toString(_4) + ", " + N.toString(_5) + ", " + N.toString(_6)
                    + ", " + N.toString(_7) + ", " + N.toString(_8) + ", " + N.toString(_9) + "]";
        }
    }
}
