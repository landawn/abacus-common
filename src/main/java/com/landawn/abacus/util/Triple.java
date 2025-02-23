/*
 * Copyright (c) 2015, Haiyang Li.
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

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.u.Optional;

/**
 *
 * @param <L>
 * @param <M>
 * @param <R>
 */
@SuppressFBWarnings("PA_PUBLIC_PRIMITIVE_ATTRIBUTE")
public final class Triple<L, M, R> implements Mutable {

    public L left; //NOSONAR

    public M middle; //NOSONAR

    public R right; //NOSONAR

    public Triple() {
    }

    Triple(final L l, final M m, final R r) {
        left = l;
        middle = m;
        right = r;
    }

    /**
     *
     * @param <L>
     * @param <M>
     * @param <R>
     * @param l
     * @param m
     * @param r
     * @return
     */
    public static <L, M, R> Triple<L, M, R> of(final L l, final M m, final R r) {
        return new Triple<>(l, m, r);
    }

    private static final Triple<?, ?, ?>[] EMPTY_ARRAY = new Triple[0];

    /**
     *
     * @param <L>
     * @param <M>
     * @param <R>
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <L, M, R> Triple<L, M, R>[] emptyArray() {
        return (Triple<L, M, R>[]) EMPTY_ARRAY;
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
     * Gets the middle.
     *
     * @return
     */
    public M getMiddle() {
        return middle;
    }

    /**
     * Sets the middle.
     *
     * @param middle the new middle
     */
    public void setMiddle(final M middle) {
        this.middle = middle;
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
     * @param middle
     * @param right
     */
    public void set(final L left, final M middle, final R right) {
        this.left = left;
        this.middle = middle;
        this.right = right;
    }

    /**
     * Returns the current left value and then sets it with the specified value
     *
     * @param newLeft
     * @return
     */
    public L getAndSetLeft(final L newLeft) {
        final L res = left;
        left = newLeft;
        return res;
    }

    /**
     * Sets left with the specified value and then return it.
     *
     * @param newLeft
     * @return
     */
    public L setAndGetLeft(final L newLeft) {
        left = newLeft;
        return left;
    }

    /**
     * Returns the current middle value and then sets it with the specified value
     *
     * @param newMiddle
     * @return
     */
    public M getAndSetMiddle(final M newMiddle) {
        final M res = middle;
        middle = newMiddle;
        return res;
    }

    /**
     * Sets middle with the specified value and then return it.
     *
     * @param newMiddle
     * @return
     */
    public M setAndGetMiddle(final M newMiddle) {
        middle = newMiddle;
        return middle;
    }

    /**
     Returns the current right value and then sets it with the specified value.
     *
     * @param newRight
     * @return
     */
    public R getAndSetRight(final R newRight) {
        final R res = right;
        right = newRight;
        return res;
    }

    /**
     * Sets right with the specified value and then return it.
     *
     * @param newRight
     * @return
     */
    public R setAndGetRight(final R newRight) {
        right = newRight;
        return right;
    }

    /**
     * Set to the specified {@code newLeft} and returns {@code true}
     * if {@code predicate} returns {@code true}. Otherwise returns
     * {@code false} without setting the value to new value.
     *
     * @param <E>
     * @param newLeft
     * @param predicate - the first parameter is current pair, the second
     *        parameter is the {@code newLeft}
     * @return
     * @throws E the e
     */
    public <E extends Exception> boolean setLeftIf(final L newLeft, final Throwables.BiPredicate<? super Triple<L, M, R>, ? super L, E> predicate) throws E {
        if (predicate.test(this, newLeft)) {
            left = newLeft;
            return true;
        }

        return false;
    }

    /**
     * Set to the specified {@code newMiddle} and returns {@code true}
     * if {@code predicate} returns {@code true}. Otherwise returns
     * {@code false} without setting the value to new value.
     *
     * @param <E>
     * @param newMiddle
     * @param predicate - the first parameter is current pair, the second
     *        parameter is the {@code newMiddle}
     * @return
     * @throws E the e
     */
    public <E extends Exception> boolean setMiddleIf(final M newMiddle, final Throwables.BiPredicate<? super Triple<L, M, R>, ? super M, E> predicate)
            throws E {
        if (predicate.test(this, newMiddle)) {
            middle = newMiddle;
            return true;
        }

        return false;
    }

    /**
     * Set to the specified {@code newRight} and returns {@code true}
     * if {@code predicate} returns {@code true}. Otherwise returns
     * {@code false} without setting the value to new value.
     *
     * @param <E>
     * @param newRight
     * @param predicate - the first parameter is current pair, the second
     *        parameter is the {@code newRight}
     * @return
     * @throws E the e
     */
    public <E extends Exception> boolean setRightIf(final R newRight, final Throwables.BiPredicate<? super Triple<L, M, R>, ? super R, E> predicate) throws E {
        if (predicate.test(this, newRight)) {
            right = newRight;
            return true;
        }

        return false;
    }

    /**
     * Set to the specified {@code newLeft} and {@code newRight} and
     * returns {@code true} if {@code predicate} returns {@code true}.
     * Otherwise returns {@code false} without setting the left/right to
     * new values.
     *
     * @param <E>
     * @param newLeft
     * @param newMiddle
     * @param newRight
     * @param predicate - the first parameter is current pair, the second
     *        parameter is the {@code newLeft}, the third parameter is the
     *        {@code newMiddle}, the fourth parameter is the
     *        {@code newRight}
     * @return
     * @throws E the e
     */
    public <E extends Exception> boolean setIf(final L newLeft, final M newMiddle, final R newRight,
            final Throwables.QuadPredicate<? super Triple<L, M, R>, ? super L, ? super M, ? super R, E> predicate) throws E {
        if (predicate.test(this, newLeft, newMiddle, newRight)) {
            left = newLeft;
            middle = newMiddle;
            right = newRight;
            return true;
        }

        return false;
    }

    //    /**
    //     * Swap the left and right value. they must be same type.
    //     */
    //    public void reverse() {
    //        Object tmp = left;
    //        this.left = (L) right;
    //        this.right = (R) tmp;
    //    }

    /**
     * Returns a new instance of Triple&lt;R, M, L&gt;.
     *
     * @return a new instance of Triple&lt;R, M, L&gt;.
     */
    @Beta
    public Triple<R, M, L> reverse() {
        return new Triple<>(right, middle, left);
    }

    public Triple<L, M, R> copy() {
        return new Triple<>(left, middle, right);
    }

    public Object[] toArray() {
        return new Object[] { left, middle, right };
    }

    /**
     *
     * @param <A>
     * @param a
     * @return
     */
    public <A> A[] toArray(A[] a) {
        if (a.length < 3) {
            a = N.copyOf(a, 3);
        }

        a[0] = (A) left;
        a[1] = (A) middle;
        a[2] = (A) right;

        return a;
    }

    /**
     *
     * @param <E>
     * @param consumer
     * @throws E the e
     */
    public <E extends Exception> void forEach(final Throwables.Consumer<?, E> consumer) throws E {
        final Throwables.Consumer<Object, E> objConsumer = (Throwables.Consumer<Object, E>) consumer;

        objConsumer.accept(left);
        objConsumer.accept(middle);
        objConsumer.accept(right);
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void accept(final Throwables.TriConsumer<? super L, ? super M, ? super R, E> action) throws E {
        action.accept(left, middle, right);
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void accept(final Throwables.Consumer<? super Triple<L, M, R>, E> action) throws E {
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
    public <U, E extends Exception> U map(final Throwables.TriFunction<? super L, ? super M, ? super R, ? extends U, E> mapper) throws E {
        return mapper.apply(left, middle, right);
    }

    /**
     *
     * @param <U>
     * @param <E>
     * @param mapper
     * @return
     * @throws E the e
     */
    public <U, E extends Exception> U map(final Throwables.Function<? super Triple<L, M, R>, ? extends U, E> mapper) throws E {
        return mapper.apply(this);
    }

    /**
     *
     * @param <E>
     * @param predicate
     * @return
     * @throws E the e
     */
    public <E extends Exception> Optional<Triple<L, M, R>> filter(final Throwables.TriPredicate<? super L, ? super M, ? super R, E> predicate) throws E {
        return predicate.test(left, middle, right) ? Optional.of(this) : Optional.empty();
    }

    /**
     *
     * @param <E>
     * @param predicate
     * @return
     * @throws E the e
     */
    public <E extends Exception> Optional<Triple<L, M, R>> filter(final Throwables.Predicate<? super Triple<L, M, R>, E> predicate) throws E {
        return predicate.test(this) ? Optional.of(this) : Optional.empty();
    }

    //    /**
    //     *
    //     *
    //     * @return
    //     * @deprecated {@code Optional} is misused. It's marked to be removed.
    //     */
    //    @Deprecated
    //    @Beta
    //    public Stream<Triple<L, M, R>> stream() {
    //        return Stream.of(this);
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
    //     * @deprecated {@code Optional} is misused. It's marked to be removed.
    //     */
    //    @Deprecated
    //    @Beta
    //    public <T, E extends Exception> Stream<T> stream(final Throwables.Function<? super Triple<L, M, R>, Stream<T>, E> func) throws E {
    //        return func.apply(this);
    //    }
    //
    //    /**
    //     *
    //     *
    //     * @return
    //     * @deprecated {@code Optional} is misused. It's marked to be removed.
    //     */
    //    @Deprecated
    //    public Optional<Triple<L, M, R>> toOptional() {
    //        return Optional.of(this);
    //    }

    public Tuple3<L, M, R> toTuple() {
        return Tuple.of(left, middle, right);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + N.hashCode(left);
        result = prime * result + N.hashCode(middle);
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

        if (obj instanceof Triple) {
            final Triple<L, M, R> other = (Triple<L, M, R>) obj;

            return N.equals(left, other.left) && N.equals(middle, other.middle) && N.equals(right, other.right);
        }

        return false;
    }

    @Override
    public String toString() {
        return "[" + N.toString(left) + ", " + N.toString(middle) + ", " + N.toString(right) + "]";
    }
}
