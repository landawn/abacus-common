/*
 * Copyright 2017 Haiyang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.util.function;

import com.landawn.abacus.util.Throwables;

/**
 * A functional interface that represents a predicate (boolean-valued function) of four arguments.
 * This is the four-arity specialization of {@link java.util.function.Predicate}.
 *
 * <p>This interface extends the standard Java functional interfaces to support predicates
 * with four parameters, which is useful for complex conditional logic that requires
 * multiple inputs to determine a boolean result.
 *
 * <p>This is a functional interface whose functional method is {@link #test(Object, Object, Object, Object)}.
 *
 * @param <A> the type of the first argument to the predicate
 * @param <B> the type of the second argument to the predicate
 * @param <C> the type of the third argument to the predicate
 * @param <D> the type of the fourth argument to the predicate
 * @see java.util.function.Predicate
 * @see java.util.function.BiPredicate
 * @see TriPredicate
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface QuadPredicate<A, B, C, D> extends Throwables.QuadPredicate<A, B, C, D, RuntimeException> { //NOSONAR
    /**
     * Evaluates this predicate on the given arguments.
     *
     * <p>This method tests whether the four input arguments satisfy the condition
     * represented by this predicate. It should return {@code true} if the input
     * arguments match the predicate's criteria, {@code false} otherwise.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * QuadPredicate<Integer, Integer, Integer, Integer> allPositive =
     *     (a, b, c, d) -> a > 0 && b > 0 && c > 0 && d > 0;
     *
     * QuadPredicate<String, String, String, String> allEqual =
     *     (a, b, c, d) -> a.equals(b) && b.equals(c) && c.equals(d);
     *
     * QuadPredicate<User, Product, Integer, Double> canPurchase =
     *     (user, product, quantity, discount) ->
     *         user.getBalance() >= product.getPrice() * quantity * (1 - discount) &&
     *         product.getStock() >= quantity &&
     *         user.isVerified() &&
     *         discount <= user.getMaxDiscount();
     *
     * boolean result1 = allPositive.test(1, 2, 3, 4); // Returns true
     * boolean result2 = allEqual.test("A", "A", "A", "B"); // Returns false
     * }</pre>
     *
     * @param a the first input argument
     * @param b the second input argument
     * @param c the third input argument
     * @param d the fourth input argument
     * @return {@code true} if the input arguments match the predicate, {@code false} otherwise
     */
    @Override
    boolean test(A a, B b, C c, D d);

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     *
     * <p>The returned predicate will return {@code true} when this predicate returns
     * {@code false}, and vice versa. This is useful for inverting complex conditions
     * without having to rewrite the logic.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * QuadPredicate<Integer, Integer, Integer, Integer> allPositive =
     *     (a, b, c, d) -> a > 0 && b > 0 && c > 0 && d > 0;
     * QuadPredicate<Integer, Integer, Integer, Integer> hasNegative =
     *     allPositive.negate();
     *
     * boolean result = hasNegative.test(1, -2, 3, 4); // Returns true
     * }</pre>
     *
     * @return a predicate that represents the logical negation of this predicate
     */
    default QuadPredicate<A, B, C, D> negate() {
        return (a, b, c, d) -> !test(a, b, c, d);
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * QuadPredicate<User, Product, Integer, Double> hasBalance =
     *     (user, product, quantity, discount) ->
     *         user.getBalance() >= product.getPrice() * quantity * (1 - discount);
     *
     * QuadPredicate<User, Product, Integer, Double> hasStock =
     *     (user, product, quantity, discount) ->
     *         product.getStock() >= quantity;
     *
     * QuadPredicate<User, Product, Integer, Double> canPurchase =
     *     hasBalance.and(hasStock);
     * }</pre>
     *
     * @param other a predicate that will be logically-ANDed with this predicate
     * @return a composed predicate that represents the short-circuiting logical
     *         AND of this predicate and the {@code other} predicate
     */
    default QuadPredicate<A, B, C, D> and(final QuadPredicate<? super A, ? super B, ? super C, ? super D> other) {
        return (a, b, c, d) -> test(a, b, c, d) && other.test(a, b, c, d);
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * QuadPredicate<User, Product, Integer, Double> isPremiumUser =
     *     (user, product, quantity, discount) -> user.isPremium();
     *
     * QuadPredicate<User, Product, Integer, Double> hasLoyaltyPoints =
     *     (user, product, quantity, discount) ->
     *         user.getLoyaltyPoints() >= 1000;
     *
     * QuadPredicate<User, Product, Integer, Double> getsFreeShipping =
     *     isPremiumUser.or(hasLoyaltyPoints);
     * }</pre>
     *
     * @param other a predicate that will be logically-ORed with this predicate
     * @return a composed predicate that represents the short-circuiting logical
     *         OR of this predicate and the {@code other} predicate
     */
    default QuadPredicate<A, B, C, D> or(final QuadPredicate<? super A, ? super B, ? super C, ? super D> other) {
        return (a, b, c, d) -> test(a, b, c, d) || other.test(a, b, c, d);
    }

    /**
     * Converts this {@code QuadPredicate} to a {@code Throwables.QuadPredicate} that can throw a checked exception.
     * This method provides a way to use this predicate in contexts that require explicit exception handling.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * QuadPredicate predicate = (...) -> { ... };
     * var throwablePredicate = predicate.toThrowable();
     * // Can now be used in contexts that handle checked exceptions
     * }</pre>
     *
     * @param <E> the type of exception that the returned predicate can throw
     * @return a {@code Throwables.QuadPredicate} view of this predicate that can throw exceptions of type {@code E}
     */
    default <E extends Throwable> Throwables.QuadPredicate<A, B, C, D, E> toThrowable() {
        return (Throwables.QuadPredicate<A, B, C, D, E>) this;
    }

}
