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
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 *
 */
public abstract class Tuple {
    public static final Tuple EMPTY = new Tuple() {
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
        public Object[] toArray() {
            return N.EMPTY_OBJECT_ARRAY;
        }

        @Override
        public <A> A[] toArray(A[] a) {
            return a;
        }

        @Override
        public <E extends Exception> void forEach(Try.Consumer<?, E> consumer) throws E {
            N.checkArgNotNull(consumer);
            // do nothing.
        }

        @Override
        public Stream<? extends Tuple> stream() {
            return Stream.empty();
        }

        @Override
        public String toString() {
            return "[]";
        }
    };

    Tuple() {
    }

    public abstract int arity();

    public abstract boolean anyNull();

    public abstract boolean allNull();

    public abstract Object[] toArray();

    public abstract <A> A[] toArray(A[] a);

    public abstract <E extends Exception> void forEach(Try.Consumer<?, E> consumer) throws E;

    protected abstract Stream<? extends Tuple> stream();

    public static <T1> Tuple1<T1> of(T1 _1) {
        return new Tuple1<>(_1);
    }

    public static <T1, T2> Tuple2<T1, T2> of(T1 _1, T2 _2) {
        return new Tuple2<>(_1, _2);
    }

    public static <T1, T2, T3> Tuple3<T1, T2, T3> of(T1 _1, T2 _2, T3 _3) {
        return new Tuple3<>(_1, _2, _3);
    }

    public static <T1, T2, T3, T4> Tuple4<T1, T2, T3, T4> of(T1 _1, T2 _2, T3 _3, T4 _4) {
        return new Tuple4<>(_1, _2, _3, _4);
    }

    public static <T1, T2, T3, T4, T5> Tuple5<T1, T2, T3, T4, T5> of(T1 _1, T2 _2, T3 _3, T4 _4, T5 _5) {
        return new Tuple5<>(_1, _2, _3, _4, _5);
    }

    public static <T1, T2, T3, T4, T5, T6> Tuple6<T1, T2, T3, T4, T5, T6> of(T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6) {
        return new Tuple6<>(_1, _2, _3, _4, _5, _6);
    }

    public static <T1, T2, T3, T4, T5, T6, T7> Tuple7<T1, T2, T3, T4, T5, T6, T7> of(T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6, T7 _7) {
        return new Tuple7<>(_1, _2, _3, _4, _5, _6, _7);
    }

    public static <T1, T2, T3, T4, T5, T6, T7, T8> Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> of(T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6, T7 _7, T8 _8) {
        return new Tuple8<>(_1, _2, _3, _4, _5, _6, _7, _8);
    }

    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> of(T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6, T7 _7, T8 _8,
            T9 _9) {
        return new Tuple9<>(_1, _2, _3, _4, _5, _6, _7, _8, _9);
    }

    public static <K, V> Tuple2<K, V> from(final Map.Entry<K, V> entry) {
        return new Tuple2<>(entry.getKey(), entry.getValue());
    }

    public static <T extends Tuple> T from(final Object[] a) {
        final int len = a == null ? 0 : a.length;

        Tuple result = null;

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

        return (T) result;
    }

    public static <T extends Tuple> T from(final Collection<?> c) {
        final int len = c == null ? 0 : c.size();
        final Iterator<?> iter = c == null ? null : c.iterator();

        Tuple result = null;

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

        return (T) result;
    }

    public final static class Tuple1<T1> extends Tuple {
        public final T1 _1;

        // For Kryo
        Tuple1() {
            this(null);
        }

        Tuple1(T1 _1) {
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

        @Override
        public Object[] toArray() {
            return new Object[] { _1 };
        }

        @Override
        public <A> A[] toArray(A[] a) {
            if (a.length < 1) {
                a = N.copyOf(a, 1);
            }

            a[0] = (A) _1;

            return a;
        }

        @Override
        public <E extends Exception> void forEach(Try.Consumer<?, E> consumer) throws E {
            final Try.Consumer<Object, E> objConsumer = (Try.Consumer<Object, E>) consumer;

            objConsumer.accept(_1);
        }

        public <E extends Exception> void accept(final Try.Consumer<? super Tuple1<T1>, E> action) throws E {
            action.accept(this);
        }

        public <U, E extends Exception> U map(final Try.Function<? super Tuple1<T1>, U, E> mapper) throws E {
            return mapper.apply(this);
        }

        public <E extends Exception> Optional<Tuple1<T1>> filter(final Try.Predicate<? super Tuple1<T1>, E> predicate) throws E {
            return predicate.test(this) ? Optional.of(this) : Optional.<Tuple1<T1>> empty();
        }

        @Override
        public Stream<Tuple1<T1>> stream() {
            return Stream.of(this);
        }

        /**
         * <pre>
         * <code>
         * Optional#ofNullable(_1)
         * </code>
         * </pre>
         * 
         * @return
         * @see Optional#ofNullable(Object)
         */
        public Optional<T1> toOptional() {
            return Optional.ofNullable(_1);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + N.hashCode(_1);
            return result;
        }

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

        @Override
        public String toString() {
            return "[" + N.toString(_1) + "]";
        }
    }

    public final static class Tuple2<T1, T2> extends Tuple {
        public final T1 _1;
        public final T2 _2;

        // For Kryo
        Tuple2() {
            this(null, null);
        }

        Tuple2(T1 _1, T2 _2) {
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

        public Tuple2<T2, T1> reversed() {
            return of(_2, _1);
        }

        @Override
        public Object[] toArray() {
            return new Object[] { _1, _2 };
        }

        @Override
        public <A> A[] toArray(A[] a) {
            if (a.length < 2) {
                a = N.copyOf(a, 2);
            }

            a[0] = (A) _1;
            a[1] = (A) _2;

            return a;
        }

        @Override
        public <E extends Exception> void forEach(Try.Consumer<?, E> consumer) throws E {
            final Try.Consumer<Object, E> objConsumer = (Try.Consumer<Object, E>) consumer;

            objConsumer.accept(_1);
            objConsumer.accept(_2);
        }

        public <E extends Exception> void accept(final Try.Consumer<? super Tuple2<T1, T2>, E> action) throws E {
            action.accept(this);
        }

        public <E extends Exception> void accept(final Try.BiConsumer<? super T1, ? super T2, E> action) throws E {
            action.accept(_1, _2);
        }

        public <U, E extends Exception> U map(final Try.Function<? super Tuple2<T1, T2>, U, E> mapper) throws E {
            return mapper.apply(this);
        }

        public <U, E extends Exception> U map(final Try.BiFunction<? super T1, ? super T2, U, E> mapper) throws E {
            return mapper.apply(_1, _2);
        }

        public <E extends Exception> Optional<Tuple2<T1, T2>> filter(final Try.Predicate<? super Tuple2<T1, T2>, E> predicate) throws E {
            return predicate.test(this) ? Optional.of(this) : Optional.<Tuple2<T1, T2>> empty();
        }

        public <E extends Exception> Optional<Tuple2<T1, T2>> filter(final Try.BiPredicate<? super T1, ? super T2, E> predicate) throws E {
            return predicate.test(_1, _2) ? Optional.of(this) : Optional.<Tuple2<T1, T2>> empty();
        }

        @Override
        public Stream<Tuple2<T1, T2>> stream() {
            return Stream.of(this);
        }

        public Pair<T1, T2> toPair() {
            return Pair.of(_1, _2);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + N.hashCode(_1);
            result = prime * result + N.hashCode(_2);
            return result;
        }

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

        @Override
        public String toString() {
            return "[" + N.toString(_1) + ", " + N.toString(_2) + "]";
        }
    }

    public final static class Tuple3<T1, T2, T3> extends Tuple {
        public final T1 _1;
        public final T2 _2;
        public final T3 _3;

        // For Kryo
        Tuple3() {
            this(null, null, null);
        }

        Tuple3(T1 _1, T2 _2, T3 _3) {
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

        public Tuple3<T3, T2, T1> reversed() {
            return new Tuple3<>(_3, _2, _1);
        }

        @Override
        public Object[] toArray() {
            return new Object[] { _1, _2, _3 };
        }

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

        @Override
        public <E extends Exception> void forEach(Try.Consumer<?, E> consumer) throws E {
            final Try.Consumer<Object, E> objConsumer = (Try.Consumer<Object, E>) consumer;

            objConsumer.accept(_1);
            objConsumer.accept(_2);
            objConsumer.accept(_3);
        }

        public <E extends Exception> void accept(final Try.Consumer<? super Tuple3<T1, T2, T3>, E> action) throws E {
            action.accept(this);
        }

        public <E extends Exception> void accept(final Try.TriConsumer<? super T1, ? super T2, ? super T3, E> action) throws E {
            action.accept(_1, _2, _3);
        }

        public <U, E extends Exception> U map(final Try.Function<? super Tuple3<T1, T2, T3>, U, E> mapper) throws E {
            return mapper.apply(this);
        }

        public <U, E extends Exception> U map(final Try.TriFunction<? super T1, ? super T2, ? super T3, U, E> mapper) throws E {
            return mapper.apply(_1, _2, _3);
        }

        public <E extends Exception> Optional<Tuple3<T1, T2, T3>> filter(final Try.Predicate<? super Tuple3<T1, T2, T3>, E> predicate) throws E {
            return predicate.test(this) ? Optional.of(this) : Optional.<Tuple3<T1, T2, T3>> empty();
        }

        public <E extends Exception> Optional<Tuple3<T1, T2, T3>> filter(final Try.TriPredicate<? super T1, ? super T2, ? super T3, E> predicate) throws E {
            return predicate.test(_1, _2, _3) ? Optional.of(this) : Optional.<Tuple3<T1, T2, T3>> empty();
        }

        @Override
        public Stream<Tuple3<T1, T2, T3>> stream() {
            return Stream.of(this);
        }

        public Triple<T1, T2, T3> toTriple() {
            return Triple.of(_1, _2, _3);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + N.hashCode(_1);
            result = prime * result + N.hashCode(_2);
            result = prime * result + N.hashCode(_3);
            return result;
        }

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

        @Override
        public String toString() {
            return "[" + N.toString(_1) + ", " + N.toString(_2) + ", " + N.toString(_3) + "]";
        }
    }

    public final static class Tuple4<T1, T2, T3, T4> extends Tuple {
        public final T1 _1;
        public final T2 _2;
        public final T3 _3;
        public final T4 _4;

        // For Kryo
        Tuple4() {
            this(null, null, null, null);
        }

        Tuple4(T1 _1, T2 _2, T3 _3, T4 _4) {
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

        public Tuple4<T4, T3, T2, T1> reversed() {
            return new Tuple4<>(_4, _3, _2, _1);
        }

        @Override
        public Object[] toArray() {
            return new Object[] { _1, _2, _3, _4 };
        }

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

        @Override
        public <E extends Exception> void forEach(Try.Consumer<?, E> consumer) throws E {
            final Try.Consumer<Object, E> objConsumer = (Try.Consumer<Object, E>) consumer;

            objConsumer.accept(_1);
            objConsumer.accept(_2);
            objConsumer.accept(_3);
            objConsumer.accept(_4);
        }

        public <E extends Exception> void accept(final Try.Consumer<? super Tuple4<T1, T2, T3, T4>, E> action) throws E {
            action.accept(this);
        }

        public <U, E extends Exception> U map(final Try.Function<? super Tuple4<T1, T2, T3, T4>, U, E> mapper) throws E {
            return mapper.apply(this);
        }

        public <E extends Exception> Optional<Tuple4<T1, T2, T3, T4>> filter(final Try.Predicate<? super Tuple4<T1, T2, T3, T4>, E> predicate) throws E {
            return predicate.test(this) ? Optional.of(this) : Optional.<Tuple4<T1, T2, T3, T4>> empty();
        }

        @Override
        public Stream<Tuple4<T1, T2, T3, T4>> stream() {
            return Stream.of(this);
        }

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

        @Override
        public String toString() {
            return "[" + N.toString(_1) + ", " + N.toString(_2) + ", " + N.toString(_3) + ", " + N.toString(_4) + "]";
        }
    }

    public final static class Tuple5<T1, T2, T3, T4, T5> extends Tuple {
        public final T1 _1;
        public final T2 _2;
        public final T3 _3;
        public final T4 _4;
        public final T5 _5;

        // For Kryo
        Tuple5() {
            this(null, null, null, null, null);
        }

        Tuple5(T1 _1, T2 _2, T3 _3, T4 _4, T5 _5) {
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

        public Tuple5<T5, T4, T3, T2, T1> reversed() {
            return new Tuple5<>(_5, _4, _3, _2, _1);
        }

        @Override
        public Object[] toArray() {
            return new Object[] { _1, _2, _3, _4, _5 };
        }

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

        @Override
        public <E extends Exception> void forEach(Try.Consumer<?, E> consumer) throws E {
            final Try.Consumer<Object, E> objConsumer = (Try.Consumer<Object, E>) consumer;

            objConsumer.accept(_1);
            objConsumer.accept(_2);
            objConsumer.accept(_3);
            objConsumer.accept(_4);
            objConsumer.accept(_5);
        }

        public <E extends Exception> void accept(final Try.Consumer<? super Tuple5<T1, T2, T3, T4, T5>, E> action) throws E {
            action.accept(this);
        }

        public <U, E extends Exception> U map(final Try.Function<? super Tuple5<T1, T2, T3, T4, T5>, U, E> mapper) throws E {
            return mapper.apply(this);
        }

        public <E extends Exception> Optional<Tuple5<T1, T2, T3, T4, T5>> filter(final Try.Predicate<? super Tuple5<T1, T2, T3, T4, T5>, E> predicate)
                throws E {
            return predicate.test(this) ? Optional.of(this) : Optional.<Tuple5<T1, T2, T3, T4, T5>> empty();
        }

        @Override
        public Stream<Tuple5<T1, T2, T3, T4, T5>> stream() {
            return Stream.of(this);
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
            return result;
        }

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

        @Override
        public String toString() {
            return "[" + N.toString(_1) + ", " + N.toString(_2) + ", " + N.toString(_3) + ", " + N.toString(_4) + ", " + N.toString(_5) + "]";
        }
    }

    public static final class Tuple6<T1, T2, T3, T4, T5, T6> extends Tuple {
        public final T1 _1;
        public final T2 _2;
        public final T3 _3;
        public final T4 _4;
        public final T5 _5;
        public final T6 _6;

        // For Kryo
        Tuple6() {
            this(null, null, null, null, null, null);
        }

        Tuple6(T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6) {
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

        public Tuple6<T6, T5, T4, T3, T2, T1> reversed() {
            return new Tuple6<>(_6, _5, _4, _3, _2, _1);
        }

        @Override
        public Object[] toArray() {
            return new Object[] { _1, _2, _3, _4, _5, _6 };
        }

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

        @Override
        public <E extends Exception> void forEach(Try.Consumer<?, E> consumer) throws E {
            final Try.Consumer<Object, E> objConsumer = (Try.Consumer<Object, E>) consumer;

            objConsumer.accept(_1);
            objConsumer.accept(_2);
            objConsumer.accept(_3);
            objConsumer.accept(_4);
            objConsumer.accept(_5);
            objConsumer.accept(_6);
        }

        public <E extends Exception> void accept(final Try.Consumer<? super Tuple6<T1, T2, T3, T4, T5, T6>, E> action) throws E {
            action.accept(this);
        }

        public <U, E extends Exception> U map(final Try.Function<? super Tuple6<T1, T2, T3, T4, T5, T6>, U, E> mapper) throws E {
            return mapper.apply(this);
        }

        public <E extends Exception> Optional<Tuple6<T1, T2, T3, T4, T5, T6>> filter(final Try.Predicate<? super Tuple6<T1, T2, T3, T4, T5, T6>, E> predicate)
                throws E {
            return predicate.test(this) ? Optional.of(this) : Optional.<Tuple6<T1, T2, T3, T4, T5, T6>> empty();
        }

        @Override
        public Stream<Tuple6<T1, T2, T3, T4, T5, T6>> stream() {
            return Stream.of(this);
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
            return result;
        }

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

        @Override
        public String toString() {
            return "[" + N.toString(_1) + ", " + N.toString(_2) + ", " + N.toString(_3) + ", " + N.toString(_4) + ", " + N.toString(_5) + ", " + N.toString(_6)
                    + "]";
        }
    }

    public static final class Tuple7<T1, T2, T3, T4, T5, T6, T7> extends Tuple {
        public final T1 _1;
        public final T2 _2;
        public final T3 _3;
        public final T4 _4;
        public final T5 _5;
        public final T6 _6;
        public final T7 _7;

        // For Kryo
        Tuple7() {
            this(null, null, null, null, null, null, null);
        }

        Tuple7(T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6, T7 _7) {
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

        public Tuple7<T7, T6, T5, T4, T3, T2, T1> reversed() {
            return new Tuple7<>(_7, _6, _5, _4, _3, _2, _1);
        }

        @Override
        public Object[] toArray() {
            return new Object[] { _1, _2, _3, _4, _5, _6, _7 };
        }

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

        @Override
        public <E extends Exception> void forEach(Try.Consumer<?, E> consumer) throws E {
            final Try.Consumer<Object, E> objConsumer = (Try.Consumer<Object, E>) consumer;

            objConsumer.accept(_1);
            objConsumer.accept(_2);
            objConsumer.accept(_3);
            objConsumer.accept(_4);
            objConsumer.accept(_5);
            objConsumer.accept(_6);
            objConsumer.accept(_7);
        }

        public <E extends Exception> void accept(final Try.Consumer<? super Tuple7<T1, T2, T3, T4, T5, T6, T7>, E> action) throws E {
            action.accept(this);
        }

        public <U, E extends Exception> U map(final Try.Function<? super Tuple7<T1, T2, T3, T4, T5, T6, T7>, U, E> mapper) throws E {
            return mapper.apply(this);
        }

        public <E extends Exception> Optional<Tuple7<T1, T2, T3, T4, T5, T6, T7>> filter(
                final Try.Predicate<? super Tuple7<T1, T2, T3, T4, T5, T6, T7>, E> predicate) throws E {
            return predicate.test(this) ? Optional.of(this) : Optional.<Tuple7<T1, T2, T3, T4, T5, T6, T7>> empty();
        }

        @Override
        public Stream<Tuple7<T1, T2, T3, T4, T5, T6, T7>> stream() {
            return Stream.of(this);
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
            return result;
        }

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

        @Override
        public String toString() {
            return "[" + N.toString(_1) + ", " + N.toString(_2) + ", " + N.toString(_3) + ", " + N.toString(_4) + ", " + N.toString(_5) + ", " + N.toString(_6)
                    + ", " + N.toString(_7) + "]";
        }
    }

    public static final class Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> extends Tuple {
        public final T1 _1;
        public final T2 _2;
        public final T3 _3;
        public final T4 _4;
        public final T5 _5;
        public final T6 _6;
        public final T7 _7;
        public final T8 _8;

        // For Kryo
        Tuple8() {
            this(null, null, null, null, null, null, null, null);
        }

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

        public Tuple8<T8, T7, T6, T5, T4, T3, T2, T1> reversed() {
            return new Tuple8<>(_8, _7, _6, _5, _4, _3, _2, _1);
        }

        @Override
        public Object[] toArray() {
            return new Object[] { _1, _2, _3, _4, _5, _6, _7, _8 };
        }

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

        @Override
        public <E extends Exception> void forEach(Try.Consumer<?, E> consumer) throws E {
            final Try.Consumer<Object, E> objConsumer = (Try.Consumer<Object, E>) consumer;

            objConsumer.accept(_1);
            objConsumer.accept(_2);
            objConsumer.accept(_3);
            objConsumer.accept(_4);
            objConsumer.accept(_5);
            objConsumer.accept(_6);
            objConsumer.accept(_7);
            objConsumer.accept(_8);
        }

        public <E extends Exception> void accept(final Try.Consumer<? super Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>, E> action) throws E {
            action.accept(this);
        }

        public <U, E extends Exception> U map(final Try.Function<? super Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>, U, E> mapper) throws E {
            return mapper.apply(this);
        }

        public <E extends Exception> Optional<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> filter(
                final Try.Predicate<? super Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>, E> predicate) throws E {
            return predicate.test(this) ? Optional.of(this) : Optional.<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> empty();
        }

        @Override
        public Stream<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> stream() {
            return Stream.of(this);
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
            return result;
        }

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

        @Override
        public String toString() {
            return "[" + N.toString(_1) + ", " + N.toString(_2) + ", " + N.toString(_3) + ", " + N.toString(_4) + ", " + N.toString(_5) + ", " + N.toString(_6)
                    + ", " + N.toString(_7) + ", " + N.toString(_8) + "]";
        }
    }

    public static final class Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> extends Tuple {
        public final T1 _1;
        public final T2 _2;
        public final T3 _3;
        public final T4 _4;
        public final T5 _5;
        public final T6 _6;
        public final T7 _7;
        public final T8 _8;
        public final T9 _9;

        // For Kryo
        Tuple9() {
            this(null, null, null, null, null, null, null, null, null);
        }

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

        public Tuple9<T9, T8, T7, T6, T5, T4, T3, T2, T1> reversed() {
            return new Tuple9<>(_9, _8, _7, _6, _5, _4, _3, _2, _1);
        }

        @Override
        public Object[] toArray() {
            return new Object[] { _1, _2, _3, _4, _5, _6, _7, _8, _9 };
        }

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

        @Override
        public <E extends Exception> void forEach(Try.Consumer<?, E> consumer) throws E {
            final Try.Consumer<Object, E> objConsumer = (Try.Consumer<Object, E>) consumer;

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

        public <E extends Exception> void accept(final Try.Consumer<? super Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>, E> action) throws E {
            action.accept(this);
        }

        public <U, E extends Exception> U map(final Try.Function<? super Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>, U, E> mapper) throws E {
            return mapper.apply(this);
        }

        public <E extends Exception> Optional<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> filter(
                final Try.Predicate<? super Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>, E> predicate) throws E {
            return predicate.test(this) ? Optional.of(this) : Optional.<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> empty();
        }

        @Override
        public Stream<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> stream() {
            return Stream.of(this);
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
            result = prime * result + N.hashCode(_9);
            return result;
        }

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

        @Override
        public String toString() {
            return "[" + N.toString(_1) + ", " + N.toString(_2) + ", " + N.toString(_3) + ", " + N.toString(_4) + ", " + N.toString(_5) + ", " + N.toString(_6)
                    + ", " + N.toString(_7) + ", " + N.toString(_8) + ", " + N.toString(_9) + "]";
        }
    }
}
