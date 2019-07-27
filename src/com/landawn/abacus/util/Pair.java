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
 * @since 0.8
 * 
 * @author Haiyang Li
 *
 * @param <L>
 * @param <R>
 */
public final class Pair<L, R> { // implements Map.Entry<L, R> {
    public L left;
    public R right;

    public Pair() {
    }

    Pair(final L l, final R r) {
        this.left = l;
        this.right = r;
    }

    public static <L, R> Pair<L, R> of(final L l, final R r) {
        return new Pair<>(l, r);
    }

    public static <K, V> Pair<K, V> from(final Map.Entry<K, V> entry) {
        return new Pair<>(entry.getKey(), entry.getValue());
    }

    /**
     * 
     * @return 
     */
    public L getLeft() {
        return left;
    }

    public void setLeft(final L left) {
        this.left = left;
    }

    /**
     * 
     * @return 
     */
    public R getRight() {
        return right;
    }

    public void setRight(final R right) {
        this.right = right;
    }

    public void set(final L left, final R right) {
        this.left = left;
        this.right = right;
    }

    public L getAndSetLeft(L newLeft) {
        final L res = left;
        left = newLeft;
        return res;
    }

    public L setAndGetLeft(L newLeft) {
        left = newLeft;
        return left;
    }

    public R getAndSetRight(R newRight) {
        final R res = newRight;
        right = newRight;
        return res;
    }

    public R setAndGetRight(R newRight) {
        right = newRight;
        return right;
    }

    /**
     * Set to the specified <code>newLeft</code> and returns <code>true</code>
     * if <code>predicate</code> returns true. Otherwise returns
     * <code>false</code> without setting the value to new value.
     * 
     * @param newLeft
     * @param predicate - the first parameter is current pair, the second
     *        parameter is the <code>newLeft</code>
     * @return
     */
    public <E extends Exception> boolean setLeftIf(final L newLeft, Try.BiPredicate<? super Pair<L, R>, ? super L, E> predicate) throws E {
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
     * @param newRight
     * @param predicate - the first parameter is current pair, the second
     *        parameter is the <code>newRight</code>
     * @return
     */
    public <E extends Exception> boolean setRightIf(final R newRight, Try.BiPredicate<? super Pair<L, R>, ? super R, E> predicate) throws E {
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
     * @param newLeft
     * @param newRight
     * @param predicate - the first parameter is current pair, the second
     *        parameter is the <code>newLeft</code>, the third parameter is the <code>newRight</code>.
     * @return
     */
    public <E extends Exception> boolean setIf(final L newLeft, final R newRight, Try.TriPredicate<? super Pair<L, R>, ? super L, ? super R, E> predicate)
            throws E {
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
    //     * @Deprecated don't access this method by {@code Pair} interface.
    //     */
    //    @Deprecated
    //    @Override
    //    public L getKey() {
    //        return left;
    //    }
    //
    //    /**
    //     * 
    //     * @Deprecated don't access this method by {@code Pair} interface.
    //     */
    //    @Deprecated
    //    @Override
    //    public R getValue() {
    //        return right;
    //    }
    //
    //    /**
    //     * 
    //     * @Deprecated don't access this method by {@code Pair} interface.
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
    public Pair<R, L> reversed() {
        return new Pair<>(this.right, this.left);
    }

    public Pair<L, R> copy() {
        return new Pair<>(this.left, this.right);
    }

    public Object[] toArray() {
        return new Object[] { left, right };
    }

    public <A> A[] toArray(A[] a) {
        if (a.length < 2) {
            a = N.copyOf(a, 2);
        }

        a[0] = (A) left;
        a[1] = (A) right;

        return a;
    }

    public <E extends Exception> void forEach(Try.Consumer<?, E> comsumer) throws E {
        final Try.Consumer<Object, E> objComsumer = (Try.Consumer<Object, E>) comsumer;

        objComsumer.accept(left);
        objComsumer.accept(right);
    }

    public <E extends Exception> void accept(final Try.BiConsumer<? super L, ? super R, E> action) throws E {
        action.accept(left, right);
    }

    public <E extends Exception> void accept(final Try.Consumer<? super Pair<L, R>, E> action) throws E {
        action.accept(this);
    }

    public <U, E extends Exception> U map(final Try.BiFunction<? super L, ? super R, U, E> mapper) throws E {
        return mapper.apply(left, right);
    }

    public <U, E extends Exception> U map(final Try.Function<? super Pair<L, R>, U, E> mapper) throws E {
        return mapper.apply(this);
    }

    public <E extends Exception> Optional<Pair<L, R>> filter(final Try.BiPredicate<? super L, ? super R, E> predicate) throws E {
        return predicate.test(left, right) ? Optional.of(this) : Optional.<Pair<L, R>> empty();
    }

    public <E extends Exception> Optional<Pair<L, R>> filter(final Try.Predicate<? super Pair<L, R>, E> predicate) throws E {
        return predicate.test(this) ? Optional.of(this) : Optional.<Pair<L, R>> empty();
    }

    public Stream<Pair<L, R>> stream() {
        return Stream.of(this);
    }

    public Tuple2<L, R> toTuple() {
        return Tuple.of(left, right);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + N.hashCode(left);
        result = prime * result + N.hashCode(right);
        return result;
    }

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

    @Override
    public String toString() {
        return "[" + N.toString(left) + ", " + N.toString(right) + "]";
        // return N.toString(left) + "=" + N.toString(right);
    }
}
