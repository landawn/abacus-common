/*
 * Copyright (c) 2017, Haiyang Li.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Map;

import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.Stream;

/**
 * The Class Tuple.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public abstract class Tuple<TP> {

    /** The Constant EMPTY. */
    private static final Tuple0 EMPTY = new Tuple0();

    /**
     * Instantiates a new tuple.
     */
    Tuple() {
    }

    /**
     *
     * @return
     */
    public abstract int arity();

    /**
     *
     * @return true, if successful
     */
    public abstract boolean anyNull();

    /**
     *
     * @return true, if successful
     */
    public abstract boolean allNull();

    /**
     * 
     * @param objToFind
     * @return
     */
    public abstract boolean contains(final Object objToFind);

    /**
     *
     * @return
     */
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
     * @param <U>
     * @param <E>
     * @param mapper
     * @return
     * @throws E the e
     */
    public <U, E extends Exception> U map(final Throwables.Function<? super TP, U, E> mapper) throws E {
        return mapper.apply((TP) this);
    }

    /**
     *
     * @param <E>
     * @param predicate
     * @return
     * @throws E the e
     */
    public <E extends Exception> Optional<TP> filter(final Throwables.Predicate<? super TP, E> predicate) throws E {
        return predicate.test((TP) this) ? Optional.of((TP) this) : Optional.<TP> empty();
    }

    /**
     *
     * @return
     */
    public Stream<TP> stream() {
        return Stream.of((TP) this);
    }

    /**
     *
     * @return
     */
    public <T, E extends Exception> Stream<T> stream(final Throwables.Function<? super TP, Stream<T>, E> func) throws E {
        return func.apply((TP) this);
    }

    /**
     * 
     * @return
     */
    public Optional<TP> toOptional() {
        return Optional.of((TP) this);
    }

    /**
     *
     * @param <T1>
     * @param _1 the 1
     * @return
     */
    public static <T1> Tuple1<T1> of(T1 _1) {
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
    public static <T1, T2> Tuple2<T1, T2> of(T1 _1, T2 _2) {
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
    public static <T1, T2, T3> Tuple3<T1, T2, T3> of(T1 _1, T2 _2, T3 _3) {
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
    public static <T1, T2, T3, T4> Tuple4<T1, T2, T3, T4> of(T1 _1, T2 _2, T3 _3, T4 _4) {
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
    public static <T1, T2, T3, T4, T5> Tuple5<T1, T2, T3, T4, T5> of(T1 _1, T2 _2, T3 _3, T4 _4, T5 _5) {
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
    public static <T1, T2, T3, T4, T5, T6> Tuple6<T1, T2, T3, T4, T5, T6> of(T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6) {
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
    public static <T1, T2, T3, T4, T5, T6, T7> Tuple7<T1, T2, T3, T4, T5, T6, T7> of(T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6, T7 _7) {
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
     * @deprecated you should consider using <code>class SomeClass { final T1 propName1, final T2 propName2...}</code>
     */
    @Deprecated
    public static <T1, T2, T3, T4, T5, T6, T7, T8> Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> of(T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6, T7 _7, T8 _8) {
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
     * @deprecated you should consider using <code>class SomeClass { final T1 propName1, final T2 propName2...}</code>
     */
    @Deprecated
    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> of(T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6, T7 _7, T8 _8,
            T9 _9) {
        return new Tuple9<>(_1, _2, _3, _4, _5, _6, _7, _8, _9);
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param entry
     * @return
     */
    public static <K, V> Tuple2<K, V> from(final Map.Entry<K, V> entry) {
        return new Tuple2<>(entry.getKey(), entry.getValue());
    }

    /**
     *
     * @param <T>
     * @param a
     * @return
     */
    public static <TP extends Tuple<TP>> TP from(final Object[] a) {
        final int len = a == null ? 0 : a.length;

        Tuple<?> result = null;

        switch (len) {
            case 0:
                result = EMPTY;
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
                throw new RuntimeException("Too many elements(" + a.length + ") to fill in Tuple.");
        }

        return (TP) result;
    }

    /**
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <TP extends Tuple<TP>> TP from(final Collection<?> c) {
        final int len = c == null ? 0 : c.size();
        final Iterator<?> iter = c == null ? null : c.iterator();

        Tuple<?> result = null;

        switch (len) {
            case 0:
                result = EMPTY;
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
                throw new RuntimeException("Too many elements(" + c.size() + ") to fill in Tuple.");
        }

        return (TP) result;
    }

    public static <T1, T2, T3> Tuple3<T1, T2, T3> flatten(Tuple2<Tuple2<T1, T2>, T3> tp) {
        return new Tuple3<>(tp._1._1, tp._1._2, tp._2);
    }

    //    public static <T1, T2, T3, T4> Tuple4<T1, T2, T3, T4> flatt(Tuple2<Tuple2<Tuple2<T1, T2>, T3>, T4> tp) {
    //        return new Tuple4<>(tp._1._1._1, tp._1._1._2, tp._1._2, tp._2);
    //    }

    /**
     * The Class Tuple1.
     *
     * @param <T1>
     */
    static final class Tuple0 extends Tuple<Tuple0> {
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
        public boolean contains(final Object objToFind) {
            return false;
        }

        @Override
        public Object[] toArray() {
            return N.EMPTY_OBJECT_ARRAY;
        }

        @Override
        public <A> A[] toArray(A[] a) {
            return a;
        }

        @Override
        public <E extends Exception> void forEach(Throwables.Consumer<?, E> consumer) throws E {
            N.checkArgNotNull(consumer);
            // do nothing.
        }

        @Override
        public String toString() {
            return "[]";
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
        Tuple1(T1 _1) {
            this._1 = _1;
        }

        /**
         *
         * @return
         */
        @Override
        public int arity() {
            return 1;
        }

        /**
         *
         * @return true, if successful
         */
        @Override
        public boolean anyNull() {
            return _1 == null;
        }

        /**
         *
         * @return true, if successful
         */
        @Override
        public boolean allNull() {
            return _1 == null;
        }

        @Override
        public boolean contains(final Object objToFind) {
            return N.equals(_1, objToFind);
        }

        /**
         *
         * @return
         */
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
        public <E extends Exception> void forEach(Throwables.Consumer<?, E> consumer) throws E {
            final Throwables.Consumer<Object, E> objConsumer = (Throwables.Consumer<Object, E>) consumer;

            objConsumer.accept(_1);
        }

        /**
         *
         * @return
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + N.hashCode(_1);
            return result;
        }

        /**
         *
         * @param obj
         * @return true, if successful
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj != null && obj.getClass().equals(Tuple1.class)) {
                final Tuple1<?> other = (Tuple1<?>) obj;

                return N.equals(this._1, other._1);
            }

            return false;
        }

        /**
         *
         * @return
         */
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
        Tuple2(T1 _1, T2 _2) {
            this._1 = _1;
            this._2 = _2;
        }

        /**
         *
         * @return
         */
        @Override
        public int arity() {
            return 2;
        }

        /**
         *
         * @return true, if successful
         */
        @Override
        public boolean anyNull() {
            return _1 == null || _2 == null;
        }

        /**
         *
         * @return true, if successful
         */
        @Override
        public boolean allNull() {
            return _1 == null && _2 == null;
        }

        @Override
        public boolean contains(final Object objToFind) {
            return N.equals(_1, objToFind) || N.equals(_2, objToFind);
        }

        /**
         *
         * @return
         */
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

        /**
         *
         * @return
         */
        public Pair<T1, T2> toPair() {
            return Pair.of(_1, _2);
        }

        /**
         * 
         * @return
         */
        public ImmutableEntry<T1, T2> toEntry() {
            return ImmutableEntry.of(_1, _2);
        }

        /**
         *
         * @return
         */
        public Tuple2<T2, T1> reverse() {
            return of(_2, _1);
        }

        /**
         *
         * @param <E>
         * @param consumer
         * @throws E the e
         */
        @Override
        public <E extends Exception> void forEach(Throwables.Consumer<?, E> consumer) throws E {
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
         * @param <U>
         * @param <E>
         * @param mapper
         * @return
         * @throws E the e
         */
        public <U, E extends Exception> U map(final Throwables.BiFunction<? super T1, ? super T2, U, E> mapper) throws E {
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
            return predicate.test(_1, _2) ? Optional.of(this) : Optional.<Tuple2<T1, T2>> empty();
        }

        /**
         *
         * @return
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + N.hashCode(_1);
            result = prime * result + N.hashCode(_2);
            return result;
        }

        /**
         *
         * @param obj
         * @return true, if successful
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj != null && obj.getClass().equals(Tuple2.class)) {
                final Tuple2<?, ?> other = (Tuple2<?, ?>) obj;

                return N.equals(this._1, other._1) && N.equals(this._2, other._2);
            }

            return false;
        }

        /**
         *
         * @return
         */
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
        Tuple3(T1 _1, T2 _2, T3 _3) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
        }

        /**
         *
         * @return
         */
        @Override
        public int arity() {
            return 3;
        }

        /**
         *
         * @return true, if successful
         */
        @Override
        public boolean anyNull() {
            return _1 == null || _2 == null || _3 == null;
        }

        /**
         *
         * @return true, if successful
         */
        @Override
        public boolean allNull() {
            return _1 == null && _2 == null && _3 == null;
        }

        @Override
        public boolean contains(final Object objToFind) {
            return N.equals(_1, objToFind) || N.equals(_2, objToFind) || N.equals(_3, objToFind);
        }

        /**
         *
         * @return
         */
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

        /**
         *
         * @return
         */
        public Triple<T1, T2, T3> toTriple() {
            return Triple.of(_1, _2, _3);
        }

        /**
         *
         * @return
         */
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
        public <E extends Exception> void forEach(Throwables.Consumer<?, E> consumer) throws E {
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
         * @param <U>
         * @param <E>
         * @param mapper
         * @return
         * @throws E the e
         */
        public <U, E extends Exception> U map(final Throwables.TriFunction<? super T1, ? super T2, ? super T3, U, E> mapper) throws E {
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
            return predicate.test(_1, _2, _3) ? Optional.of(this) : Optional.<Tuple3<T1, T2, T3>> empty();
        }

        /**
         *
         * @return
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + N.hashCode(_1);
            result = prime * result + N.hashCode(_2);
            result = prime * result + N.hashCode(_3);
            return result;
        }

        /**
         *
         * @param obj
         * @return true, if successful
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj != null && obj.getClass().equals(Tuple3.class)) {
                final Tuple3<?, ?, ?> other = (Tuple3<?, ?, ?>) obj;

                return N.equals(this._1, other._1) && N.equals(this._2, other._2) && N.equals(this._3, other._3);
            }

            return false;
        }

        /**
         *
         * @return
         */
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
        Tuple4(T1 _1, T2 _2, T3 _3, T4 _4) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
        }

        /**
         *
         * @return
         */
        @Override
        public int arity() {
            return 4;
        }

        /**
         *
         * @return true, if successful
         */
        @Override
        public boolean anyNull() {
            return _1 == null || _2 == null || _3 == null || _4 == null;
        }

        /**
         *
         * @return true, if successful
         */
        @Override
        public boolean allNull() {
            return _1 == null && _2 == null && _3 == null && _4 == null;
        }

        @Override
        public boolean contains(final Object objToFind) {
            return N.equals(_1, objToFind) || N.equals(_2, objToFind) || N.equals(_3, objToFind) || N.equals(_4, objToFind);
        }

        /**
         *
         * @return
         */
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

        /**
         *
         * @return
         */
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
        public <E extends Exception> void forEach(Throwables.Consumer<?, E> consumer) throws E {
            final Throwables.Consumer<Object, E> objConsumer = (Throwables.Consumer<Object, E>) consumer;

            objConsumer.accept(_1);
            objConsumer.accept(_2);
            objConsumer.accept(_3);
            objConsumer.accept(_4);
        }

        /**
         *
         * @return
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + N.hashCode(_1);
            result = prime * result + N.hashCode(_2);
            result = prime * result + N.hashCode(_3);
            result = prime * result + N.hashCode(_4);
            return result;
        }

        /**
         *
         * @param obj
         * @return true, if successful
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj != null && obj.getClass().equals(Tuple4.class)) {
                final Tuple4<?, ?, ?, ?> other = (Tuple4<?, ?, ?, ?>) obj;

                return N.equals(this._1, other._1) && N.equals(this._2, other._2) && N.equals(this._3, other._3) && N.equals(this._4, other._4);
            }

            return false;
        }

        /**
         *
         * @return
         */
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
        Tuple5(T1 _1, T2 _2, T3 _3, T4 _4, T5 _5) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
            this._5 = _5;
        }

        /**
         *
         * @return
         */
        @Override
        public int arity() {
            return 5;
        }

        /**
         *
         * @return true, if successful
         */
        @Override
        public boolean anyNull() {
            return _1 == null || _2 == null || _3 == null || _4 == null || _5 == null;
        }

        /**
         *
         * @return true, if successful
         */
        @Override
        public boolean allNull() {
            return _1 == null && _2 == null && _3 == null && _4 == null && _5 == null;
        }

        @Override
        public boolean contains(final Object objToFind) {
            return N.equals(_1, objToFind) || N.equals(_2, objToFind) || N.equals(_3, objToFind) || N.equals(_4, objToFind) || N.equals(_5, objToFind);
        }

        /**
         *
         * @return
         */
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

        /**
         *
         * @return
         */
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
        public <E extends Exception> void forEach(Throwables.Consumer<?, E> consumer) throws E {
            final Throwables.Consumer<Object, E> objConsumer = (Throwables.Consumer<Object, E>) consumer;

            objConsumer.accept(_1);
            objConsumer.accept(_2);
            objConsumer.accept(_3);
            objConsumer.accept(_4);
            objConsumer.accept(_5);
        }

        /**
         *
         * @return
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + N.hashCode(_1);
            result = prime * result + N.hashCode(_2);
            result = prime * result + N.hashCode(_3);
            result = prime * result + N.hashCode(_4);
            result = prime * result + N.hashCode(_5);
            return result;
        }

        /**
         *
         * @param obj
         * @return true, if successful
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj != null && obj.getClass().equals(Tuple5.class)) {
                final Tuple5<?, ?, ?, ?, ?> other = (Tuple5<?, ?, ?, ?, ?>) obj;

                return N.equals(this._1, other._1) && N.equals(this._2, other._2) && N.equals(this._3, other._3) && N.equals(this._4, other._4)
                        && N.equals(this._5, other._5);
            }

            return false;
        }

        /**
         *
         * @return
         */
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
        Tuple6(T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
            this._5 = _5;
            this._6 = _6;
        }

        /**
         *
         * @return
         */
        @Override
        public int arity() {
            return 6;
        }

        /**
         *
         * @return true, if successful
         */
        @Override
        public boolean anyNull() {
            return _1 == null || _2 == null || _3 == null || _4 == null || _5 == null || _6 == null;
        }

        /**
         *
         * @return true, if successful
         */
        @Override
        public boolean allNull() {
            return _1 == null && _2 == null && _3 == null && _4 == null && _5 == null && _6 == null;
        }

        @Override
        public boolean contains(final Object objToFind) {
            return N.equals(_1, objToFind) || N.equals(_2, objToFind) || N.equals(_3, objToFind) || N.equals(_4, objToFind) || N.equals(_5, objToFind)
                    || N.equals(_6, objToFind);
        }

        /**
         *
         * @return
         */
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

        /**
         *
         * @return
         */
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
        public <E extends Exception> void forEach(Throwables.Consumer<?, E> consumer) throws E {
            final Throwables.Consumer<Object, E> objConsumer = (Throwables.Consumer<Object, E>) consumer;

            objConsumer.accept(_1);
            objConsumer.accept(_2);
            objConsumer.accept(_3);
            objConsumer.accept(_4);
            objConsumer.accept(_5);
            objConsumer.accept(_6);
        }

        /**
         *
         * @return
         */
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
            return result;
        }

        /**
         *
         * @param obj
         * @return true, if successful
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj != null && obj.getClass().equals(Tuple6.class)) {
                final Tuple6<?, ?, ?, ?, ?, ?> other = (Tuple6<?, ?, ?, ?, ?, ?>) obj;

                return N.equals(this._1, other._1) && N.equals(this._2, other._2) && N.equals(this._3, other._3) && N.equals(this._4, other._4)
                        && N.equals(this._5, other._5) && N.equals(this._6, other._6);
            }

            return false;
        }

        /**
         *
         * @return
         */
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
        Tuple7(T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6, T7 _7) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
            this._5 = _5;
            this._6 = _6;
            this._7 = _7;
        }

        /**
         *
         * @return
         */
        @Override
        public int arity() {
            return 7;
        }

        /**
         *
         * @return true, if successful
         */
        @Override
        public boolean anyNull() {
            return _1 == null || _2 == null || _3 == null || _4 == null || _5 == null || _6 == null || _7 == null;
        }

        /**
         *
         * @return true, if successful
         */
        @Override
        public boolean allNull() {
            return _1 == null && _2 == null && _3 == null && _4 == null && _5 == null && _6 == null && _7 == null;
        }

        @Override
        public boolean contains(final Object objToFind) {
            return N.equals(_1, objToFind) || N.equals(_2, objToFind) || N.equals(_3, objToFind) || N.equals(_4, objToFind) || N.equals(_5, objToFind)
                    || N.equals(_6, objToFind) || N.equals(_7, objToFind);
        }

        /**
         *
         * @return
         */
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

        /**
         *
         * @return
         */
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
        public <E extends Exception> void forEach(Throwables.Consumer<?, E> consumer) throws E {
            final Throwables.Consumer<Object, E> objConsumer = (Throwables.Consumer<Object, E>) consumer;

            objConsumer.accept(_1);
            objConsumer.accept(_2);
            objConsumer.accept(_3);
            objConsumer.accept(_4);
            objConsumer.accept(_5);
            objConsumer.accept(_6);
            objConsumer.accept(_7);
        }

        /**
         *
         * @return
         */
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
            return result;
        }

        /**
         *
         * @param obj
         * @return true, if successful
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj != null && obj.getClass().equals(Tuple7.class)) {
                final Tuple7<?, ?, ?, ?, ?, ?, ?> other = (Tuple7<?, ?, ?, ?, ?, ?, ?>) obj;

                return N.equals(this._1, other._1) && N.equals(this._2, other._2) && N.equals(this._3, other._3) && N.equals(this._4, other._4)
                        && N.equals(this._5, other._5) && N.equals(this._6, other._6) && N.equals(this._7, other._7);
            }

            return false;
        }

        /**
         *
         * @return
         */
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
        Tuple8(T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6, T7 _7, T8 _8) {
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

        /**
         *
         * @return
         */
        @Override
        public int arity() {
            return 8;
        }

        /**
         *
         * @return true, if successful
         */
        @Override
        public boolean anyNull() {
            return _1 == null || _2 == null || _3 == null || _4 == null || _5 == null || _6 == null || _7 == null || _8 == null;
        }

        /**
         *
         * @return true, if successful
         */
        @Override
        public boolean allNull() {
            return _1 == null && _2 == null && _3 == null && _4 == null && _5 == null && _6 == null && _7 == null && _8 == null;
        }

        @Override
        public boolean contains(final Object objToFind) {
            return N.equals(_1, objToFind) || N.equals(_2, objToFind) || N.equals(_3, objToFind) || N.equals(_4, objToFind) || N.equals(_5, objToFind)
                    || N.equals(_6, objToFind) || N.equals(_7, objToFind) || N.equals(_8, objToFind);
        }

        /**
         *
         * @return
         */
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

        /**
         *
         * @return
         */
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
        public <E extends Exception> void forEach(Throwables.Consumer<?, E> consumer) throws E {
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

        /**
         *
         * @return
         */
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
            return result;
        }

        /**
         *
         * @param obj
         * @return true, if successful
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj != null && obj.getClass().equals(Tuple8.class)) {
                final Tuple8<?, ?, ?, ?, ?, ?, ?, ?> other = (Tuple8<?, ?, ?, ?, ?, ?, ?, ?>) obj;

                return N.equals(this._1, other._1) && N.equals(this._2, other._2) && N.equals(this._3, other._3) && N.equals(this._4, other._4)
                        && N.equals(this._5, other._5) && N.equals(this._6, other._6) && N.equals(this._7, other._7) && N.equals(this._8, other._8);
            }

            return false;
        }

        /**
         *
         * @return
         */
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
        Tuple9(T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6, T7 _7, T8 _8, T9 _9) {
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

        /**
         *
         * @return
         */
        @Override
        public int arity() {
            return 9;
        }

        /**
         *
         * @return true, if successful
         */
        @Override
        public boolean anyNull() {
            return _1 == null || _2 == null || _3 == null || _4 == null || _5 == null || _6 == null || _7 == null || _8 == null || _9 == null;
        }

        /**
         *
         * @return true, if successful
         */
        @Override
        public boolean allNull() {
            return _1 == null && _2 == null && _3 == null && _4 == null && _5 == null && _6 == null && _7 == null && _8 == null && _9 == null;
        }

        @Override
        public boolean contains(final Object objToFind) {
            return N.equals(_1, objToFind) || N.equals(_2, objToFind) || N.equals(_3, objToFind) || N.equals(_4, objToFind) || N.equals(_5, objToFind)
                    || N.equals(_6, objToFind) || N.equals(_7, objToFind) || N.equals(_8, objToFind) || N.equals(_9, objToFind);
        }

        /**
         *
         * @return
         */
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

        /**
         *
         * @return
         */
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
        public <E extends Exception> void forEach(Throwables.Consumer<?, E> consumer) throws E {
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

        /**
         *
         * @return
         */
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
            result = prime * result + N.hashCode(_9);
            return result;
        }

        /**
         *
         * @param obj
         * @return true, if successful
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj != null && obj.getClass().equals(Tuple9.class)) {
                final Tuple9<?, ?, ?, ?, ?, ?, ?, ?, ?> other = (Tuple9<?, ?, ?, ?, ?, ?, ?, ?, ?>) obj;

                return N.equals(this._1, other._1) && N.equals(this._2, other._2) && N.equals(this._3, other._3) && N.equals(this._4, other._4)
                        && N.equals(this._5, other._5) && N.equals(this._6, other._6) && N.equals(this._7, other._7) && N.equals(this._8, other._8)
                        && N.equals(this._9, other._9);
            }

            return false;
        }

        /**
         *
         * @return
         */
        @Override
        public String toString() {
            return "[" + N.toString(_1) + ", " + N.toString(_2) + ", " + N.toString(_3) + ", " + N.toString(_4) + ", " + N.toString(_5) + ", " + N.toString(_6)
                    + ", " + N.toString(_7) + ", " + N.toString(_8) + ", " + N.toString(_9) + "]";
        }
    }
}
