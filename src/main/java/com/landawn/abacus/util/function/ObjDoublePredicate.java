/*
 * Copyright (C) 2016 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util.function;

import com.landawn.abacus.util.Throwables;

/**
 * A functional interface that represents a predicate (boolean-valued function) of an
 * object-valued argument and a double-valued argument. This is a specialization of
 * BiPredicate for the case where the second argument is a primitive double.
 *
 * <p>This interface is typically used for testing conditions that involve both an object
 * and a double value, such as threshold checks, range validations, or filtering operations
 * based on numeric criteria.
 *
 * <p>This is a functional interface whose functional method is {@link #test(Object, double)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a>
 *
 * @param <T> the type of the object argument to the predicate
 * @see java.util.function.BiPredicate
 * @see java.util.function.Predicate
 */
@FunctionalInterface
public interface ObjDoublePredicate<T> extends Throwables.ObjDoublePredicate<T, RuntimeException> { // NOSONAR
    /**
     * Evaluates this predicate on the given arguments.
     *
     * <p>This method tests whether the given object and double value satisfy the
     * condition represented by this predicate. It should return {@code true} if
     * the input arguments match the predicate's criteria, {@code false} otherwise.
     *
     * <p>Example usage:
     * <pre>{@code
     * ObjDoublePredicate<Product> isPriceAbove = (product, threshold) -> 
     *     product.getPrice() > threshold;
     * boolean isExpensive = isPriceAbove.test(myProduct, 100.0);
     * }</pre>
     *
     * @param t the first input argument of type T
     * @param u the second input argument, a primitive double value
     * @return {@code true} if the input arguments match the predicate,
     *         otherwise {@code false}
     * @throws RuntimeException if the predicate evaluation fails
     */
    @Override
    boolean test(T t, double u);

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     *
     * <p>The returned predicate will return {@code true} when this predicate returns
     * {@code false}, and vice versa. This is useful for inverting conditions without
     * having to write a new predicate.
     *
     * <p>Example usage:
     * <pre>{@code
     * ObjDoublePredicate<Product> isPriceAbove = (product, threshold) -> 
     *     product.getPrice() > threshold;
     * ObjDoublePredicate<Product> isPriceNotAbove = isPriceAbove.negate();
     * // isPriceNotAbove tests if price <= threshold
     * }</pre>
     *
     * @return a predicate that represents the logical negation of this predicate
     */
    default ObjDoublePredicate<T> negate() {
        return (t, u) -> !test(t, u);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical
     * AND of this predicate and another. When evaluating the composed predicate,
     * if this predicate is {@code false}, then the {@code other} predicate is
     * not evaluated.
     *
     * <p>Any exceptions thrown during evaluation of either predicate are relayed
     * to the caller; if evaluation of this predicate throws an exception, the
     * {@code other} predicate will not be evaluated.
     *
     * <p>Example usage:
     * <pre>{@code
     * ObjDoublePredicate<Product> isPriceInRange = (product, max) -> 
     *     product.getPrice() >= 0 && product.getPrice() <= max;
     * ObjDoublePredicate<Product> isAvailable = (product, ignored) -> 
     *     product.isInStock();
     * 
     * ObjDoublePredicate<Product> isAffordableAndAvailable = 
     *     isPriceInRange.and(isAvailable);
     * }</pre>
     *
     * @param other a predicate that will be logically-ANDed with this predicate
     * @return a composed predicate that represents the short-circuiting logical
     *         AND of this predicate and the {@code other} predicate
     */
    default ObjDoublePredicate<T> and(final ObjDoublePredicate<T> other) {
        return (t, u) -> test(t, u) && other.test(t, u);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical
     * OR of this predicate and another. When evaluating the composed predicate,
     * if this predicate is {@code true}, then the {@code other} predicate is
     * not evaluated.
     *
     * <p>Any exceptions thrown during evaluation of either predicate are relayed
     * to the caller; if evaluation of this predicate throws an exception, the
     * {@code other} predicate will not be evaluated.
     *
     * <p>Example usage:
     * <pre>{@code
     * ObjDoublePredicate<Product> isCheap = (product, threshold) -> 
     *     product.getPrice() < threshold;
     * ObjDoublePredicate<Product> isOnSale = (product, ignored) -> 
     *     product.hasDiscount();
     * 
     * ObjDoublePredicate<Product> isGoodDeal = 
     *     isCheap.or(isOnSale);
     * }</pre>
     *
     * @param other a predicate that will be logically-ORed with this predicate
     * @return a composed predicate that represents the short-circuiting logical
     *         OR of this predicate and the {@code other} predicate
     */
    default ObjDoublePredicate<T> or(final ObjDoublePredicate<T> other) {
        return (t, u) -> test(t, u) || other.test(t, u);
    }
}