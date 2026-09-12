package com.landawn.abacus.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.Stream;

public abstract class MutableTuple {
    public static final MutableTuple EMPTY = new MutableTuple() {
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
            return CommonUtil.EMPTY_OBJECT_ARRAY;
        }

        @Override
        public <A> A[] toArray(A[] a) {
            return a;
        }

        @Override
        public <E extends Exception> void forEach(Throwables.Consumer<?, E> consumer) throws E {
            CommonUtil.checkArgNotNull(consumer);
        }

        @Override
        public Stream<? extends MutableTuple> stream() {
            return Stream.empty();
        }

        @Override
        public String toString() {
            return "[]";
        }
    };

    MutableTuple() {
    }

    public abstract int arity();

    public abstract boolean anyNull();

    public abstract boolean allNull();

    public abstract Object[] toArray();

    public abstract <A> A[] toArray(A[] a);

    public abstract <E extends Exception> void forEach(Throwables.Consumer<?, E> consumer) throws E;

    protected abstract Stream<? extends MutableTuple> stream();

    public static <T1> MutableTuple1<T1> of(T1 _1) {
        return new MutableTuple1<>(_1);
    }

    public static <T1, T2> MutableTuple2<T1, T2> of(T1 _1, T2 _2) {
        return new MutableTuple2<>(_1, _2);
    }

    public static <T1, T2, T3> MutableTuple3<T1, T2, T3> of(T1 _1, T2 _2, T3 _3) {
        return new MutableTuple3<>(_1, _2, _3);
    }

    public static <T1, T2, T3, T4> MutableTuple4<T1, T2, T3, T4> of(T1 _1, T2 _2, T3 _3, T4 _4) {
        return new MutableTuple4<>(_1, _2, _3, _4);
    }

    public static <T1, T2, T3, T4, T5> MutableTuple5<T1, T2, T3, T4, T5> of(T1 _1, T2 _2, T3 _3, T4 _4, T5 _5) {
        return new MutableTuple5<>(_1, _2, _3, _4, _5);
    }

    public static <T1, T2, T3, T4, T5, T6> MutableTuple6<T1, T2, T3, T4, T5, T6> of(T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6) {
        return new MutableTuple6<>(_1, _2, _3, _4, _5, _6);
    }

    public static <T1, T2, T3, T4, T5, T6, T7> MutableTuple7<T1, T2, T3, T4, T5, T6, T7> of(T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6, T7 _7) {
        return new MutableTuple7<>(_1, _2, _3, _4, _5, _6, _7);
    }

    public static <T1, T2, T3, T4, T5, T6, T7, T8> MutableTuple8<T1, T2, T3, T4, T5, T6, T7, T8> of(T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6, T7 _7, T8 _8) {
        return new MutableTuple8<>(_1, _2, _3, _4, _5, _6, _7, _8);
    }

    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9> MutableTuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> of(T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6, T7 _7,
            T8 _8, T9 _9) {
        return new MutableTuple9<>(_1, _2, _3, _4, _5, _6, _7, _8, _9);
    }

    public static <K, V> MutableTuple2<K, V> from(final Map.Entry<K, V> entry) {
        return new MutableTuple2<>(entry.getKey(), entry.getValue());
    }

    public static <T extends MutableTuple> T from(final Object[] a) {
        final int len = a == null ? 0 : a.length;

        MutableTuple result = null;

        switch (len) {
            case 0:
                result = EMPTY;
                break;

            case 1:
                result = new MutableTuple1<>(a[0]);
                break;

            case 2:
                result = new MutableTuple2<>(a[0], a[1]);
                break;

            case 3:
                result = new MutableTuple3<>(a[0], a[1], a[2]);
                break;

            case 4:
                result = new MutableTuple4<>(a[0], a[1], a[2], a[3]);
                break;

            case 5:
                result = new MutableTuple5<>(a[0], a[1], a[2], a[3], a[4]);
                break;

            case 6:
                result = new MutableTuple6<>(a[0], a[1], a[2], a[3], a[4], a[5]);
                break;

            case 7:
                result = new MutableTuple7<>(a[0], a[1], a[2], a[3], a[4], a[5], a[6]);
                break;

            case 8:
                result = new MutableTuple8<>(a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7]);
                break;

            case 9:
                result = new MutableTuple9<>(a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8]);
                break;

            default:
                throw new IllegalArgumentException("Too many elements((" + a.length + ") to fill in MutableTuple.");
        }

        return (T) result;
    }

    public static <T extends MutableTuple> T from(final Collection<?> c) {
        final int len = c == null ? 0 : c.size();
        final Iterator<?> iter = c == null ? null : c.iterator();

        MutableTuple result = null;

        switch (len) {
            case 0:
                result = EMPTY;
                break;

            case 1:
                result = new MutableTuple1<>(iter.next());
                break;

            case 2:
                result = new MutableTuple2<>(iter.next(), iter.next());
                break;

            case 3:
                result = new MutableTuple3<>(iter.next(), iter.next(), iter.next());
                break;

            case 4:
                result = new MutableTuple4<>(iter.next(), iter.next(), iter.next(), iter.next());
                break;

            case 5:
                result = new MutableTuple5<>(iter.next(), iter.next(), iter.next(), iter.next(), iter.next());
                break;

            case 6:
                result = new MutableTuple6<>(iter.next(), iter.next(), iter.next(), iter.next(), iter.next(), iter.next());
                break;

            case 7:
                result = new MutableTuple7<>(iter.next(), iter.next(), iter.next(), iter.next(), iter.next(), iter.next(), iter.next());
                break;

            case 8:
                result = new MutableTuple8<>(iter.next(), iter.next(), iter.next(), iter.next(), iter.next(), iter.next(), iter.next(), iter.next());
                break;

            case 9:
                result = new MutableTuple9<>(iter.next(), iter.next(), iter.next(), iter.next(), iter.next(), iter.next(), iter.next(), iter.next(),
                        iter.next());
                break;

            default:
                throw new IllegalArgumentException("Too many elements((" + c.size() + ") to fill in MutableTuple.");
        }

        return (T) result;
    }

    public static final class MutableTuple1<T1> extends MutableTuple {
        public volatile T1 _1;

        MutableTuple1() {
            this(null);
        }

        MutableTuple1(T1 _1) {
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
                a = CommonUtil.copyOf(a, 1);
            }

            a[0] = (A) _1;

            return a;
        }

        @Override
        public <E extends Exception> void forEach(Throwables.Consumer<?, E> consumer) throws E {
            final Throwables.Consumer<Object, E> objConsumer = (Throwables.Consumer<Object, E>) consumer;

            objConsumer.accept(_1);
        }

        public <E extends Exception> void accept(final Throwables.Consumer<? super MutableTuple1<T1>, E> action) throws E {
            action.accept(this);
        }

        public <U, E extends Exception> U map(final Throwables.Function<? super MutableTuple1<T1>, U, E> mapper) throws E {
            return mapper.apply(this);
        }

        public <E extends Exception> Optional<MutableTuple1<T1>> filter(final Throwables.Predicate<? super MutableTuple1<T1>, E> predicate) throws E {
            return predicate.test(this) ? Optional.of(this) : Optional.<MutableTuple1<T1>> empty();
        }

        @Override
        public Stream<MutableTuple1<T1>> stream() {
            return Stream.of(this);
        }

        public Optional<T1> toOptional() {
            return Optional.ofNullable(_1);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            return prime * result + CommonUtil.hashCode(_1);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj != null && obj.getClass().equals(MutableTuple1.class)) {
                final MutableTuple1<?> other = (MutableTuple1<?>) obj;

                return CommonUtil.equals(this._1, other._1);
            }

            return false;
        }

        @Override
        public String toString() {
            return "[" + CommonUtil.toString(_1) + "]";
        }
    }

    public static final class MutableTuple2<T1, T2> extends MutableTuple {
        public volatile T1 _1;
        public volatile T2 _2;

        MutableTuple2() {
            this(null, null);
        }

        MutableTuple2(T1 _1, T2 _2) {
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

        public MutableTuple2<T2, T1> reversed() {
            return of(_2, _1);
        }

        @Override
        public Object[] toArray() {
            return new Object[] { _1, _2 };
        }

        @Override
        public <A> A[] toArray(A[] a) {
            if (a.length < 2) {
                a = CommonUtil.copyOf(a, 2);
            }

            a[0] = (A) _1;
            a[1] = (A) _2;

            return a;
        }

        @Override
        public <E extends Exception> void forEach(Throwables.Consumer<?, E> consumer) throws E {
            final Throwables.Consumer<Object, E> objConsumer = (Throwables.Consumer<Object, E>) consumer;

            objConsumer.accept(_1);
            objConsumer.accept(_2);
        }

        public <E extends Exception> void accept(final Throwables.Consumer<? super MutableTuple2<T1, T2>, E> action) throws E {
            action.accept(this);
        }

        public <E extends Exception> void accept(final Throwables.BiConsumer<? super T1, ? super T2, E> action) throws E {
            action.accept(_1, _2);
        }

        public <U, E extends Exception> U map(final Throwables.Function<? super MutableTuple2<T1, T2>, U, E> mapper) throws E {
            return mapper.apply(this);
        }

        public <U, E extends Exception> U map(final Throwables.BiFunction<? super T1, ? super T2, U, E> mapper) throws E {
            return mapper.apply(_1, _2);
        }

        public <E extends Exception> Optional<MutableTuple2<T1, T2>> filter(final Throwables.Predicate<? super MutableTuple2<T1, T2>, E> predicate) throws E {
            return predicate.test(this) ? Optional.of(this) : Optional.<MutableTuple2<T1, T2>> empty();
        }

        public <E extends Exception> Optional<MutableTuple2<T1, T2>> filter(final Throwables.BiPredicate<? super T1, ? super T2, E> predicate) throws E {
            return predicate.test(_1, _2) ? Optional.of(this) : Optional.<MutableTuple2<T1, T2>> empty();
        }

        @Override
        public Stream<MutableTuple2<T1, T2>> stream() {
            return Stream.of(this);
        }

        public Pair<T1, T2> toPair() {
            return Pair.of(_1, _2);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + CommonUtil.hashCode(_1);
            return prime * result + CommonUtil.hashCode(_2);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj != null && obj.getClass().equals(MutableTuple2.class)) {
                final MutableTuple2<?, ?> other = (MutableTuple2<?, ?>) obj;

                return CommonUtil.equals(this._1, other._1) && CommonUtil.equals(this._2, other._2);
            }

            return false;
        }

        @Override
        public String toString() {
            return "[" + CommonUtil.toString(_1) + ", " + CommonUtil.toString(_2) + "]";
        }
    }

    public static final class MutableTuple3<T1, T2, T3> extends MutableTuple {
        public volatile T1 _1;
        public volatile T2 _2;
        public volatile T3 _3;

        MutableTuple3() {
            this(null, null, null);
        }

        MutableTuple3(T1 _1, T2 _2, T3 _3) {
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

        public MutableTuple3<T3, T2, T1> reversed() {
            return new MutableTuple3<>(_3, _2, _1);
        }

        @Override
        public Object[] toArray() {
            return new Object[] { _1, _2, _3 };
        }

        @Override
        public <A> A[] toArray(A[] a) {
            if (a.length < 3) {
                a = CommonUtil.copyOf(a, 3);
            }

            a[0] = (A) _1;
            a[1] = (A) _2;
            a[2] = (A) _3;

            return a;
        }

        @Override
        public <E extends Exception> void forEach(Throwables.Consumer<?, E> consumer) throws E {
            final Throwables.Consumer<Object, E> objConsumer = (Throwables.Consumer<Object, E>) consumer;

            objConsumer.accept(_1);
            objConsumer.accept(_2);
            objConsumer.accept(_3);
        }

        public <E extends Exception> void accept(final Throwables.Consumer<? super MutableTuple3<T1, T2, T3>, E> action) throws E {
            action.accept(this);
        }

        public <E extends Exception> void accept(final Throwables.TriConsumer<? super T1, ? super T2, ? super T3, E> action) throws E {
            action.accept(_1, _2, _3);
        }

        public <U, E extends Exception> U map(final Throwables.Function<? super MutableTuple3<T1, T2, T3>, U, E> mapper) throws E {
            return mapper.apply(this);
        }

        public <U, E extends Exception> U map(final Throwables.TriFunction<? super T1, ? super T2, ? super T3, U, E> mapper) throws E {
            return mapper.apply(_1, _2, _3);
        }

        public <E extends Exception> Optional<MutableTuple3<T1, T2, T3>> filter(final Throwables.Predicate<? super MutableTuple3<T1, T2, T3>, E> predicate)
                throws E {
            return predicate.test(this) ? Optional.of(this) : Optional.<MutableTuple3<T1, T2, T3>> empty();
        }

        public <E extends Exception> Optional<MutableTuple3<T1, T2, T3>> filter(final Throwables.TriPredicate<? super T1, ? super T2, ? super T3, E> predicate)
                throws E {
            return predicate.test(_1, _2, _3) ? Optional.of(this) : Optional.<MutableTuple3<T1, T2, T3>> empty();
        }

        @Override
        public Stream<MutableTuple3<T1, T2, T3>> stream() {
            return Stream.of(this);
        }

        public Triple<T1, T2, T3> toTriple() {
            return Triple.of(_1, _2, _3);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + CommonUtil.hashCode(_1);
            result = prime * result + CommonUtil.hashCode(_2);
            return prime * result + CommonUtil.hashCode(_3);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj != null && obj.getClass().equals(MutableTuple3.class)) {
                final MutableTuple3<?, ?, ?> other = (MutableTuple3<?, ?, ?>) obj;

                return CommonUtil.equals(this._1, other._1) && CommonUtil.equals(this._2, other._2) && CommonUtil.equals(this._3, other._3);
            }

            return false;
        }

        @Override
        public String toString() {
            return "[" + CommonUtil.toString(_1) + ", " + CommonUtil.toString(_2) + ", " + CommonUtil.toString(_3) + "]";
        }
    }

    public static final class MutableTuple4<T1, T2, T3, T4> extends MutableTuple {
        public volatile T1 _1;
        public volatile T2 _2;
        public volatile T3 _3;
        public volatile T4 _4;

        MutableTuple4() {
            this(null, null, null, null);
        }

        MutableTuple4(T1 _1, T2 _2, T3 _3, T4 _4) {
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

        public MutableTuple4<T4, T3, T2, T1> reversed() {
            return new MutableTuple4<>(_4, _3, _2, _1);
        }

        @Override
        public Object[] toArray() {
            return new Object[] { _1, _2, _3, _4 };
        }

        @Override
        public <A> A[] toArray(A[] a) {
            if (a.length < 4) {
                a = CommonUtil.copyOf(a, 4);
            }

            a[0] = (A) _1;
            a[1] = (A) _2;
            a[2] = (A) _3;
            a[3] = (A) _4;

            return a;
        }

        @Override
        public <E extends Exception> void forEach(Throwables.Consumer<?, E> consumer) throws E {
            final Throwables.Consumer<Object, E> objConsumer = (Throwables.Consumer<Object, E>) consumer;

            objConsumer.accept(_1);
            objConsumer.accept(_2);
            objConsumer.accept(_3);
            objConsumer.accept(_4);
        }

        public <E extends Exception> void accept(final Throwables.Consumer<? super MutableTuple4<T1, T2, T3, T4>, E> action) throws E {
            action.accept(this);
        }

        public <U, E extends Exception> U map(final Throwables.Function<? super MutableTuple4<T1, T2, T3, T4>, U, E> mapper) throws E {
            return mapper.apply(this);
        }

        public <E extends Exception> Optional<MutableTuple4<T1, T2, T3, T4>> filter(
                final Throwables.Predicate<? super MutableTuple4<T1, T2, T3, T4>, E> predicate) throws E {
            return predicate.test(this) ? Optional.of(this) : Optional.<MutableTuple4<T1, T2, T3, T4>> empty();
        }

        @Override
        public Stream<MutableTuple4<T1, T2, T3, T4>> stream() {
            return Stream.of(this);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + CommonUtil.hashCode(_1);
            result = prime * result + CommonUtil.hashCode(_2);
            result = prime * result + CommonUtil.hashCode(_3);
            return prime * result + CommonUtil.hashCode(_4);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj != null && obj.getClass().equals(MutableTuple4.class)) {
                final MutableTuple4<?, ?, ?, ?> other = (MutableTuple4<?, ?, ?, ?>) obj;

                return CommonUtil.equals(this._1, other._1) && CommonUtil.equals(this._2, other._2) && CommonUtil.equals(this._3, other._3)
                        && CommonUtil.equals(this._4, other._4);
            }

            return false;
        }

        @Override
        public String toString() {
            return "[" + CommonUtil.toString(_1) + ", " + CommonUtil.toString(_2) + ", " + CommonUtil.toString(_3) + ", " + CommonUtil.toString(_4) + "]";
        }
    }

    public static final class MutableTuple5<T1, T2, T3, T4, T5> extends MutableTuple {
        public volatile T1 _1;
        public volatile T2 _2;
        public volatile T3 _3;
        public volatile T4 _4;
        public volatile T5 _5;

        MutableTuple5() {
            this(null, null, null, null, null);
        }

        MutableTuple5(T1 _1, T2 _2, T3 _3, T4 _4, T5 _5) {
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

        public MutableTuple5<T5, T4, T3, T2, T1> reversed() {
            return new MutableTuple5<>(_5, _4, _3, _2, _1);
        }

        @Override
        public Object[] toArray() {
            return new Object[] { _1, _2, _3, _4, _5 };
        }

        @Override
        public <A> A[] toArray(A[] a) {
            if (a.length < 5) {
                a = CommonUtil.copyOf(a, 5);
            }

            a[0] = (A) _1;
            a[1] = (A) _2;
            a[2] = (A) _3;
            a[3] = (A) _4;
            a[4] = (A) _5;

            return a;
        }

        @Override
        public <E extends Exception> void forEach(Throwables.Consumer<?, E> consumer) throws E {
            final Throwables.Consumer<Object, E> objConsumer = (Throwables.Consumer<Object, E>) consumer;

            objConsumer.accept(_1);
            objConsumer.accept(_2);
            objConsumer.accept(_3);
            objConsumer.accept(_4);
            objConsumer.accept(_5);
        }

        public <E extends Exception> void accept(final Throwables.Consumer<? super MutableTuple5<T1, T2, T3, T4, T5>, E> action) throws E {
            action.accept(this);
        }

        public <U, E extends Exception> U map(final Throwables.Function<? super MutableTuple5<T1, T2, T3, T4, T5>, U, E> mapper) throws E {
            return mapper.apply(this);
        }

        public <E extends Exception> Optional<MutableTuple5<T1, T2, T3, T4, T5>> filter(
                final Throwables.Predicate<? super MutableTuple5<T1, T2, T3, T4, T5>, E> predicate) throws E {
            return predicate.test(this) ? Optional.of(this) : Optional.<MutableTuple5<T1, T2, T3, T4, T5>> empty();
        }

        @Override
        public Stream<MutableTuple5<T1, T2, T3, T4, T5>> stream() {
            return Stream.of(this);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + CommonUtil.hashCode(_1);
            result = prime * result + CommonUtil.hashCode(_2);
            result = prime * result + CommonUtil.hashCode(_3);
            result = prime * result + CommonUtil.hashCode(_4);
            return prime * result + CommonUtil.hashCode(_5);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj != null && obj.getClass().equals(MutableTuple5.class)) {
                final MutableTuple5<?, ?, ?, ?, ?> other = (MutableTuple5<?, ?, ?, ?, ?>) obj;

                return CommonUtil.equals(this._1, other._1) && CommonUtil.equals(this._2, other._2) && CommonUtil.equals(this._3, other._3)
                        && CommonUtil.equals(this._4, other._4) && CommonUtil.equals(this._5, other._5);
            }

            return false;
        }

        @Override
        public String toString() {
            return "[" + CommonUtil.toString(_1) + ", " + CommonUtil.toString(_2) + ", " + CommonUtil.toString(_3) + ", " + CommonUtil.toString(_4) + ", "
                    + CommonUtil.toString(_5) + "]";
        }
    }

    public static final class MutableTuple6<T1, T2, T3, T4, T5, T6> extends MutableTuple {
        public volatile T1 _1;
        public volatile T2 _2;
        public volatile T3 _3;
        public volatile T4 _4;
        public volatile T5 _5;
        public volatile T6 _6;

        MutableTuple6() {
            this(null, null, null, null, null, null);
        }

        MutableTuple6(T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6) {
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

        public MutableTuple6<T6, T5, T4, T3, T2, T1> reversed() {
            return new MutableTuple6<>(_6, _5, _4, _3, _2, _1);
        }

        @Override
        public Object[] toArray() {
            return new Object[] { _1, _2, _3, _4, _5, _6 };
        }

        @Override
        public <A> A[] toArray(A[] a) {
            if (a.length < 6) {
                a = CommonUtil.copyOf(a, 6);
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
        public <E extends Exception> void forEach(Throwables.Consumer<?, E> consumer) throws E {
            final Throwables.Consumer<Object, E> objConsumer = (Throwables.Consumer<Object, E>) consumer;

            objConsumer.accept(_1);
            objConsumer.accept(_2);
            objConsumer.accept(_3);
            objConsumer.accept(_4);
            objConsumer.accept(_5);
            objConsumer.accept(_6);
        }

        public <E extends Exception> void accept(final Throwables.Consumer<? super MutableTuple6<T1, T2, T3, T4, T5, T6>, E> action) throws E {
            action.accept(this);
        }

        public <U, E extends Exception> U map(final Throwables.Function<? super MutableTuple6<T1, T2, T3, T4, T5, T6>, U, E> mapper) throws E {
            return mapper.apply(this);
        }

        public <E extends Exception> Optional<MutableTuple6<T1, T2, T3, T4, T5, T6>> filter(
                final Throwables.Predicate<? super MutableTuple6<T1, T2, T3, T4, T5, T6>, E> predicate) throws E {
            return predicate.test(this) ? Optional.of(this) : Optional.<MutableTuple6<T1, T2, T3, T4, T5, T6>> empty();
        }

        @Override
        public Stream<MutableTuple6<T1, T2, T3, T4, T5, T6>> stream() {
            return Stream.of(this);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + CommonUtil.hashCode(_1);
            result = prime * result + CommonUtil.hashCode(_2);
            result = prime * result + CommonUtil.hashCode(_3);
            result = prime * result + CommonUtil.hashCode(_4);
            result = prime * result + CommonUtil.hashCode(_5);
            return prime * result + CommonUtil.hashCode(_6);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj != null && obj.getClass().equals(MutableTuple6.class)) {
                final MutableTuple6<?, ?, ?, ?, ?, ?> other = (MutableTuple6<?, ?, ?, ?, ?, ?>) obj;

                return CommonUtil.equals(this._1, other._1) && CommonUtil.equals(this._2, other._2) && CommonUtil.equals(this._3, other._3)
                        && CommonUtil.equals(this._4, other._4) && CommonUtil.equals(this._5, other._5) && CommonUtil.equals(this._6, other._6);
            }

            return false;
        }

        @Override
        public String toString() {
            return "[" + CommonUtil.toString(_1) + ", " + CommonUtil.toString(_2) + ", " + CommonUtil.toString(_3) + ", " + CommonUtil.toString(_4) + ", "
                    + CommonUtil.toString(_5) + ", " + CommonUtil.toString(_6) + "]";
        }
    }

    public static final class MutableTuple7<T1, T2, T3, T4, T5, T6, T7> extends MutableTuple {
        public volatile T1 _1;
        public volatile T2 _2;
        public volatile T3 _3;
        public volatile T4 _4;
        public volatile T5 _5;
        public volatile T6 _6;
        public volatile T7 _7;

        MutableTuple7() {
            this(null, null, null, null, null, null, null);
        }

        MutableTuple7(T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6, T7 _7) {
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

        public MutableTuple7<T7, T6, T5, T4, T3, T2, T1> reversed() {
            return new MutableTuple7<>(_7, _6, _5, _4, _3, _2, _1);
        }

        @Override
        public Object[] toArray() {
            return new Object[] { _1, _2, _3, _4, _5, _6, _7 };
        }

        @Override
        public <A> A[] toArray(A[] a) {
            if (a.length < 7) {
                a = CommonUtil.copyOf(a, 7);
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

        public <E extends Exception> void accept(final Throwables.Consumer<? super MutableTuple7<T1, T2, T3, T4, T5, T6, T7>, E> action) throws E {
            action.accept(this);
        }

        public <U, E extends Exception> U map(final Throwables.Function<? super MutableTuple7<T1, T2, T3, T4, T5, T6, T7>, U, E> mapper) throws E {
            return mapper.apply(this);
        }

        public <E extends Exception> Optional<MutableTuple7<T1, T2, T3, T4, T5, T6, T7>> filter(
                final Throwables.Predicate<? super MutableTuple7<T1, T2, T3, T4, T5, T6, T7>, E> predicate) throws E {
            return predicate.test(this) ? Optional.of(this) : Optional.<MutableTuple7<T1, T2, T3, T4, T5, T6, T7>> empty();
        }

        @Override
        public Stream<MutableTuple7<T1, T2, T3, T4, T5, T6, T7>> stream() {
            return Stream.of(this);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + CommonUtil.hashCode(_1);
            result = prime * result + CommonUtil.hashCode(_2);
            result = prime * result + CommonUtil.hashCode(_3);
            result = prime * result + CommonUtil.hashCode(_4);
            result = prime * result + CommonUtil.hashCode(_5);
            result = prime * result + CommonUtil.hashCode(_6);
            return prime * result + CommonUtil.hashCode(_7);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj != null && obj.getClass().equals(MutableTuple7.class)) {
                final MutableTuple7<?, ?, ?, ?, ?, ?, ?> other = (MutableTuple7<?, ?, ?, ?, ?, ?, ?>) obj;

                return CommonUtil.equals(this._1, other._1) && CommonUtil.equals(this._2, other._2) && CommonUtil.equals(this._3, other._3)
                        && CommonUtil.equals(this._4, other._4) && CommonUtil.equals(this._5, other._5) && CommonUtil.equals(this._6, other._6)
                        && CommonUtil.equals(this._7, other._7);
            }

            return false;
        }

        @Override
        public String toString() {
            return "[" + CommonUtil.toString(_1) + ", " + CommonUtil.toString(_2) + ", " + CommonUtil.toString(_3) + ", " + CommonUtil.toString(_4) + ", "
                    + CommonUtil.toString(_5) + ", " + CommonUtil.toString(_6) + ", " + CommonUtil.toString(_7) + "]";
        }
    }

    public static final class MutableTuple8<T1, T2, T3, T4, T5, T6, T7, T8> extends MutableTuple {
        public volatile T1 _1;
        public volatile T2 _2;
        public volatile T3 _3;
        public volatile T4 _4;
        public volatile T5 _5;
        public volatile T6 _6;
        public volatile T7 _7;
        public volatile T8 _8;

        MutableTuple8() {
            this(null, null, null, null, null, null, null, null);
        }

        MutableTuple8(T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6, T7 _7, T8 _8) {
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

        public MutableTuple8<T8, T7, T6, T5, T4, T3, T2, T1> reversed() {
            return new MutableTuple8<>(_8, _7, _6, _5, _4, _3, _2, _1);
        }

        @Override
        public Object[] toArray() {
            return new Object[] { _1, _2, _3, _4, _5, _6, _7, _8 };
        }

        @Override
        public <A> A[] toArray(A[] a) {
            if (a.length < 8) {
                a = CommonUtil.copyOf(a, 8);
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

        public <E extends Exception> void accept(final Throwables.Consumer<? super MutableTuple8<T1, T2, T3, T4, T5, T6, T7, T8>, E> action) throws E {
            action.accept(this);
        }

        public <U, E extends Exception> U map(final Throwables.Function<? super MutableTuple8<T1, T2, T3, T4, T5, T6, T7, T8>, U, E> mapper) throws E {
            return mapper.apply(this);
        }

        public <E extends Exception> Optional<MutableTuple8<T1, T2, T3, T4, T5, T6, T7, T8>> filter(
                final Throwables.Predicate<? super MutableTuple8<T1, T2, T3, T4, T5, T6, T7, T8>, E> predicate) throws E {
            return predicate.test(this) ? Optional.of(this) : Optional.<MutableTuple8<T1, T2, T3, T4, T5, T6, T7, T8>> empty();
        }

        @Override
        public Stream<MutableTuple8<T1, T2, T3, T4, T5, T6, T7, T8>> stream() {
            return Stream.of(this);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + CommonUtil.hashCode(_1);
            result = prime * result + CommonUtil.hashCode(_2);
            result = prime * result + CommonUtil.hashCode(_3);
            result = prime * result + CommonUtil.hashCode(_4);
            result = prime * result + CommonUtil.hashCode(_5);
            result = prime * result + CommonUtil.hashCode(_6);
            result = prime * result + CommonUtil.hashCode(_7);
            return prime * result + CommonUtil.hashCode(_8);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj != null && obj.getClass().equals(MutableTuple8.class)) {
                final MutableTuple8<?, ?, ?, ?, ?, ?, ?, ?> other = (MutableTuple8<?, ?, ?, ?, ?, ?, ?, ?>) obj;

                return CommonUtil.equals(this._1, other._1) && CommonUtil.equals(this._2, other._2) && CommonUtil.equals(this._3, other._3)
                        && CommonUtil.equals(this._4, other._4) && CommonUtil.equals(this._5, other._5) && CommonUtil.equals(this._6, other._6)
                        && CommonUtil.equals(this._7, other._7) && CommonUtil.equals(this._8, other._8);
            }

            return false;
        }

        @Override
        public String toString() {
            return "[" + CommonUtil.toString(_1) + ", " + CommonUtil.toString(_2) + ", " + CommonUtil.toString(_3) + ", " + CommonUtil.toString(_4) + ", "
                    + CommonUtil.toString(_5) + ", " + CommonUtil.toString(_6) + ", " + CommonUtil.toString(_7) + ", " + CommonUtil.toString(_8) + "]";
        }
    }

    public static final class MutableTuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> extends MutableTuple {
        public volatile T1 _1;
        public volatile T2 _2;
        public volatile T3 _3;
        public volatile T4 _4;
        public volatile T5 _5;
        public volatile T6 _6;
        public volatile T7 _7;
        public volatile T8 _8;
        public volatile T9 _9;

        MutableTuple9() {
            this(null, null, null, null, null, null, null, null, null);
        }

        MutableTuple9(T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6, T7 _7, T8 _8, T9 _9) {
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

        public MutableTuple9<T9, T8, T7, T6, T5, T4, T3, T2, T1> reversed() {
            return new MutableTuple9<>(_9, _8, _7, _6, _5, _4, _3, _2, _1);
        }

        @Override
        public Object[] toArray() {
            return new Object[] { _1, _2, _3, _4, _5, _6, _7, _8, _9 };
        }

        @Override
        public <A> A[] toArray(A[] a) {
            if (a.length < 9) {
                a = CommonUtil.copyOf(a, 9);
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

        public <E extends Exception> void accept(final Throwables.Consumer<? super MutableTuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>, E> action) throws E {
            action.accept(this);
        }

        public <U, E extends Exception> U map(final Throwables.Function<? super MutableTuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>, U, E> mapper) throws E {
            return mapper.apply(this);
        }

        public <E extends Exception> Optional<MutableTuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> filter(
                final Throwables.Predicate<? super MutableTuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>, E> predicate) throws E {
            return predicate.test(this) ? Optional.of(this) : Optional.<MutableTuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> empty();
        }

        @Override
        public Stream<MutableTuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> stream() {
            return Stream.of(this);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + CommonUtil.hashCode(_1);
            result = prime * result + CommonUtil.hashCode(_2);
            result = prime * result + CommonUtil.hashCode(_3);
            result = prime * result + CommonUtil.hashCode(_4);
            result = prime * result + CommonUtil.hashCode(_5);
            result = prime * result + CommonUtil.hashCode(_6);
            result = prime * result + CommonUtil.hashCode(_7);
            result = prime * result + CommonUtil.hashCode(_8);
            return prime * result + CommonUtil.hashCode(_9);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj != null && obj.getClass().equals(MutableTuple9.class)) {
                final MutableTuple9<?, ?, ?, ?, ?, ?, ?, ?, ?> other = (MutableTuple9<?, ?, ?, ?, ?, ?, ?, ?, ?>) obj;

                return CommonUtil.equals(this._1, other._1) && CommonUtil.equals(this._2, other._2) && CommonUtil.equals(this._3, other._3)
                        && CommonUtil.equals(this._4, other._4) && CommonUtil.equals(this._5, other._5) && CommonUtil.equals(this._6, other._6)
                        && CommonUtil.equals(this._7, other._7) && CommonUtil.equals(this._8, other._8) && CommonUtil.equals(this._9, other._9);
            }

            return false;
        }

        @Override
        public String toString() {
            return "[" + CommonUtil.toString(_1) + ", " + CommonUtil.toString(_2) + ", " + CommonUtil.toString(_3) + ", " + CommonUtil.toString(_4) + ", "
                    + CommonUtil.toString(_5) + ", " + CommonUtil.toString(_6) + ", " + CommonUtil.toString(_7) + ", " + CommonUtil.toString(_8) + ", "
                    + CommonUtil.toString(_9) + "]";
        }
    }
}
