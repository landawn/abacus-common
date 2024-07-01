/*
 * Copyright (c) 2015, Haiyang Li.
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

import java.util.Map;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.Stream;

/**
 *
 * @author Haiyang Li
 * @param <L>
 * @param <R>
 * @since 0.8
 */
public final class Pair<L, R> implements Map.Entry<L, R>, Mutable {
    // implements Map.Entry<L, R> {
    public L left; //NOSONAR

    public R right; //NOSONAR

    /**
     *
     */
    public Pair() {
    }

    Pair(final L l, final R r) {
        this.left = l;
        this.right = r;
    }

    /**
     *
     * @param <L>
     * @param <R>
     * @param l
     * @param r
     * @return
     */
    public static <L, R> Pair<L, R> of(final L l, final R r) {
        return new Pair<>(l, r);
    }

    /**
     * Create a new {@code Pair} with the values from the specified {@code entry}.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param entry
     * @return
     */
    public static <K, V> Pair<K, V> create(final Map.Entry<K, V> entry) {
        return new Pair<>(entry.getKey(), entry.getValue());
    }

    private static final Pair<?, ?>[] EMPTY_ARRAY = new Pair[0];

    /**
     *
     *
     * @param <L>
     * @param <R>
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <L, R> Pair<L, R>[] emptyArray() {
        return (Pair<L, R>[]) EMPTY_ARRAY;
    }

    /**
     * Gets the left.
     *
     * @return
     */
    public L getLeft() {
        return left;
    }

    /**
     * Sets the left.
     *
     * @param left the new left
     */
    public void setLeft(final L left) {
        this.left = left;
    }

    /**
     * Gets the right.
     *
     * @return
     */
    public R getRight() {
        return right;
    }

    /**
     * Sets the right.
     *
     * @param right the new right
     */
    public void setRight(final R right) {
        this.right = right;
    }

    /**
     *
     * @param left
     * @param right
     */
    public void set(final L left, final R right) {
        this.left = left;
        this.right = right;
    }

    /**
     * Gets the and set left.
     *
     * @param newLeft
     * @return
     */
    public L getAndSetLeft(L newLeft) {
        final L res = left;
        left = newLeft;
        return res;
    }

    /**
     * Sets the and get left.
     *
     * @param newLeft
     * @return
     */
    public L setAndGetLeft(L newLeft) {
        left = newLeft;
        return left;
    }

    /**
     * Gets the and set right.
     *
     * @param newRight
     * @return
     */
    public R getAndSetRight(R newRight) {
        final R res = newRight;
        right = newRight;
        return res;
    }

    /**
     * Sets the and get right.
     *
     * @param newRight
     * @return
     */
    public R setAndGetRight(R newRight) {
        right = newRight;
        return right;
    }

    /**
     * Set to the specified <code>newLeft</code> and returns <code>true</code>
     * if <code>predicate</code> returns true. Otherwise returns
     * <code>false</code> without setting the value to new value.
     *
     * @param <E>
     * @param newLeft
     * @param predicate - the first parameter is current pair, the second
     *        parameter is the <code>newLeft</code>
     * @return
     * @throws E the e
     */
    public <E extends Exception> boolean setLeftIf(final L newLeft, Throwables.BiPredicate<? super Pair<L, R>, ? super L, E> predicate) throws E {
        if (predicate.test(this, newLeft)) {
            this.left = newLeft;
            return true;
        }

        return false;
    }

    /**
     * Set to the specified <code>newRight</code> and returns <code>true</code>
     * if <code>predicate</code> returns true. Otherwise returns
     * <code>false</code> without setting the value to new value.
     *
     * @param <E>
     * @param newRight
     * @param predicate - the first parameter is current pair, the second
     *        parameter is the <code>newRight</code>
     * @return
     * @throws E the e
     */
    public <E extends Exception> boolean setRightIf(final R newRight, Throwables.BiPredicate<? super Pair<L, R>, ? super R, E> predicate) throws E {
        if (predicate.test(this, newRight)) {
            this.right = newRight;
            return true;
        }

        return false;
    }

    /**
     * Set to the specified <code>newLeft</code> and <code>newRight</code> and returns <code>true</code>
     * if <code>predicate</code> returns true. Otherwise returns
     * <code>false</code> without setting the left/right to new values.
     *
     * @param <E>
     * @param newLeft
     * @param newRight
     * @param predicate - the first parameter is current pair, the second
     *        parameter is the <code>newLeft</code>, the third parameter is the <code>newRight</code>.
     * @return
     * @throws E the e
     */
    public <E extends Exception> boolean setIf(final L newLeft, final R newRight,
            Throwables.TriPredicate<? super Pair<L, R>, ? super L, ? super R, E> predicate) throws E {
        if (predicate.test(this, newLeft, newRight)) {
            this.left = newLeft;
            this.right = newRight;
            return true;
        }

        return false;
    }
    //
    //    /**
    //     *
    //     * @deprecated don't access this method by {@code Pair} interface.
    //     */
    //    @Deprecated
    //    @Override
    //    public L getKey() {
    //        return left;
    //    }
    //
    //    /**
    //     *
    //     * @deprecated don't access this method by {@code Pair} interface.
    //     */
    //    @Deprecated
    //    @Override
    //    public R getValue() {
    //        return right;
    //    }
    //
    //    /**
    //     *
    //     * @deprecated don't access this method by {@code Pair} interface.
    //     */
    //    @Deprecated
    //    @Override
    //    public R setValue(R value) {
    //        R oldValue = this.right;
    //        this.right = value;
    //
    //        return oldValue;
    //    }

    //    public R getAndSetValue(R newRight) {
    //        return getAndSetRight(newRight);
    //    }
    //
    //    public R setAndGetValue(R newRight) {
    //        return setAndGetRight(newRight);
    //    }
    //
    //    /**
    //     *
    //     * @param newRight
    //     * @param predicate
    //     * @return
    //     * @see #setRightIf(Object, BiPredicate)
    //     */
    //    public boolean setValueIf(final R newRight, BiPredicate<? super Pair<L, R>, ? super R> predicate) {
    //        return setRightIf(newRight, predicate);
    //    }

    /**
     * Returns a new instance of Pair&lt;R, L&gt;.
     *
     * @return a new instance of Pair&lt;R, L&gt;.
     */
    @Beta
    public Pair<R, L> reverse() {
        return new Pair<>(this.right, this.left);
    }

    /**
     *
     *
     * @return
     */
    public Pair<L, R> copy() {
        return new Pair<>(this.left, this.right);
    }

    /**
     *
     *
     * @return
     */
    public Object[] toArray() {
        return new Object[] { left, right };
    }

    /**
     *
     * @param <A>
     * @param a
     * @return
     */
    public <A> A[] toArray(A[] a) {
        if (a.length < 2) {
            a = N.copyOf(a, 2);
        }

        a[0] = (A) left;
        a[1] = (A) right;

        return a;
    }

    /**
     *
     * @param <E>
     * @param comsumer
     * @throws E the e
     */
    public <E extends Exception> void forEach(Throwables.Consumer<?, E> comsumer) throws E {
        final Throwables.Consumer<Object, E> objComsumer = (Throwables.Consumer<Object, E>) comsumer;

        objComsumer.accept(left);
        objComsumer.accept(right);
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void accept(final Throwables.BiConsumer<? super L, ? super R, E> action) throws E {
        action.accept(left, right);
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void accept(final Throwables.Consumer<? super Pair<L, R>, E> action) throws E {
        action.accept(this);
    }

    /**
     *
     * @param <U>
     * @param <E>
     * @param mapper
     * @return
     * @throws E the e
     */
    public <U, E extends Exception> U map(final Throwables.BiFunction<? super L, ? super R, U, E> mapper) throws E {
        return mapper.apply(left, right);
    }

    /**
     *
     * @param <U>
     * @param <E>
     * @param mapper
     * @return
     * @throws E the e
     */
    public <U, E extends Exception> U map(final Throwables.Function<? super Pair<L, R>, U, E> mapper) throws E {
        return mapper.apply(this);
    }

    /**
     *
     * @param <E>
     * @param predicate
     * @return
     * @throws E the e
     */
    public <E extends Exception> Optional<Pair<L, R>> filter(final Throwables.BiPredicate<? super L, ? super R, E> predicate) throws E {
        return predicate.test(left, right) ? Optional.of(this) : Optional.<Pair<L, R>> empty();
    }

    /**
     *
     * @param <E>
     * @param predicate
     * @return
     * @throws E the e
     */
    public <E extends Exception> Optional<Pair<L, R>> filter(final Throwables.Predicate<? super Pair<L, R>, E> predicate) throws E {
        return predicate.test(this) ? Optional.of(this) : Optional.<Pair<L, R>> empty();
    }

    /**
     *
     *
     * @return
     */
    public Stream<Pair<L, R>> stream() {
        return Stream.of(this);
    }

    /**
     *
     *
     * @param <T>
     * @param <E>
     * @param func
     * @return
     * @throws E
     */
    public <T, E extends Exception> Stream<T> stream(final Throwables.Function<? super Pair<L, R>, Stream<T>, E> func) throws E {
        return func.apply(this);
    }

    /**
     *
     *
     * @return
     */
    public Optional<Pair<L, R>> toOptional() {
        return Optional.of(this);
    }

    /**
     *
     *
     * @return
     */
    public Tuple2<L, R> toTuple() {
        return Tuple.of(left, right);
    }

    /**
     *
     *
     * @return
     */
    public ImmutableEntry<L, R> toImmutableEntry() {
        return ImmutableEntry.of(left, right);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + N.hashCode(left);
        return prime * result + N.hashCode(right);
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

        if (obj instanceof Pair) {
            final Pair<L, R> other = (Pair<L, R>) obj;

            return N.equals(left, other.left) && N.equals(right, other.right);
        }

        return false;
    }

    /**
     *
     *
     * @return
     */
    @Override
    public String toString() {
        return "[" + N.toString(left) + ", " + N.toString(right) + "]";
    }

    /**
     * @deprecated using {@link #getLeft()}
     */
    @Deprecated
    @Override
    public L getKey() {
        return left;
    }

    /**
     * @deprecated using {@link #getRight()}
     */
    @Deprecated
    @Override
    public R getValue() {
        return right;
    }

    /**
     * @param value
     * @deprecated using {@link #setRight(Object)}
     */
    @Deprecated
    @Override
    public R setValue(R value) {
        return right;
    }
}
