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
 * Represents a predicate (boolean-valued function) of three arguments. This is
 * the three-arity specialization of {@link java.util.function.Predicate}.
 *
 * <p>This interface extends the Throwables.TriPredicate, providing compatibility
 * with the abacus-common framework's error handling mechanisms while limiting thrown exceptions
 * to RuntimeException.
 *
 * <p>This is a functional interface whose functional method is {@link #test(Object, Object, Object)}.
 *
 * @param <A> the type of the first argument to the predicate
 * @param <B> the type of the second argument to the predicate
 * @param <C> the type of the third argument to the predicate
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface TriPredicate<A, B, C> extends Throwables.TriPredicate<A, B, C, RuntimeException> { //NOSONAR
    /**
     * Evaluates this predicate on the given arguments.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TriPredicate<Integer, Integer, Integer> isValidTriangle =
     *     (a, b, c) -> a + b > c && a + c > b && b + c > a;
     * boolean valid = isValidTriangle.test(3, 4, 5); // returns true
     * boolean invalid = isValidTriangle.test(1, 2, 5); // returns false
     *
     * TriPredicate<String, String, Boolean> conditionalEquals =
     *     (s1, s2, ignoreCase) -> ignoreCase ? s1.equalsIgnoreCase(s2) : s1.equals(s2);
     * boolean result1 = conditionalEquals.test("Hello", "hello", true); // returns true
     * boolean result2 = conditionalEquals.test("Hello", "hello", false); // returns false
     * }</pre>
     *
     * @param a the first input argument
     * @param b the second input argument
     * @param c the third input argument
     * @return {@code true} if the input arguments match the predicate, {@code false} otherwise
     */
    @Override
    boolean test(A a, B b, C c);

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TriPredicate<Integer, Integer, Integer> isSum = (a, b, c) -> a + b == c;
     * TriPredicate<Integer, Integer, Integer> isNotSum = isSum.negate();
     *
     * boolean test1 = isSum.test(2, 3, 5); // returns true
     * boolean test2 = isNotSum.test(2, 3, 5); // returns false
     * boolean test3 = isNotSum.test(2, 3, 6); // returns true
     * }</pre>
     *
     * @return a predicate that represents the logical negation of this predicate
     */
    default TriPredicate<A, B, C> negate() {
        return (a, b, c) -> !test(a, b, c);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical AND of this
     * predicate and another. When evaluating the composed predicate, if this predicate is
     * {@code false}, then the other predicate is not evaluated.
     *
     * <p>Any exceptions thrown during evaluation of either predicate are relayed to the caller;
     * if evaluation of this predicate throws an exception, the other predicate will not be evaluated.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TriPredicate<Integer, Integer, Integer> isPositive = (a, b, c) -> a > 0 && b > 0 && c > 0;
     * TriPredicate<Integer, Integer, Integer> sumGreaterThan10 = (a, b, c) -> a + b + c > 10;
     *
     * TriPredicate<Integer, Integer, Integer> combined = isPositive.and(sumGreaterThan10);
     * boolean test1 = combined.test(3, 4, 5); // returns {@code true} (all positive AND sum=12>10)
     * boolean test2 = combined.test(1, 2, 3); // returns {@code false} (all positive BUT sum=6<10)
     * boolean test3 = combined.test(-1, 5, 7); // returns {@code false} (not all positive)
     * }</pre>
     *
     * @param other a predicate that will be logically-ANDed with this predicate
     * @return a composed predicate that represents the short-circuiting logical AND of this
     *         predicate and the other predicate
     */
    default TriPredicate<A, B, C> and(final TriPredicate<A, B, C> other) {
        return (a, b, c) -> test(a, b, c) && other.test(a, b, c);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical OR of this
     * predicate and another. When evaluating the composed predicate, if this predicate is
     * {@code true}, then the other predicate is not evaluated.
     *
     * <p>Any exceptions thrown during evaluation of either predicate are relayed to the caller;
     * if evaluation of this predicate throws an exception, the other predicate will not be evaluated.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TriPredicate<String, String, String> hasEmptyString =
     *     (s1, s2, s3) -> s1.isEmpty() || s2.isEmpty() || s3.isEmpty();
     * TriPredicate<String, String, String> hasTotalLengthOverTwenty =
     *     (s1, s2, s3) -> s1.length() + s2.length() + s3.length() > 20;
     *
     * TriPredicate<String, String, String> combined = hasEmptyString.or(hasTotalLengthOverTwenty);
     * boolean test1 = combined.test("", "hello", "world"); // returns {@code true} (has empty string)
     * boolean test2 = combined.test("this", "is", "a very long string"); // returns {@code true} (total length > 20)
     * boolean test3 = combined.test("short", "text", "here"); // returns false
     * }</pre>
     *
     * @param other a predicate that will be logically-ORed with this predicate
     * @return a composed predicate that represents the short-circuiting logical OR of this
     *         predicate and the other predicate
     */
    default TriPredicate<A, B, C> or(final TriPredicate<A, B, C> other) {
        return (a, b, c) -> test(a, b, c) || other.test(a, b, c);
    }

    /**
     * Converts this TriPredicate to a Throwables.TriPredicate that can throw checked exceptions.
     * This method is useful when you need to use this predicate in a context that expects
     * a Throwables.TriPredicate with a specific exception type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TriPredicate<String, Integer, Boolean> predicate = (s, i, b) -> { ... };
     * var throwablePredicate =
     *     predicate.toThrowable();
     * }</pre>
     *
     * @param <E> the type of exception that the returned predicate may throw
     * @return a Throwables.TriPredicate that wraps this predicate
     */
    default <E extends Throwable> Throwables.TriPredicate<A, B, C, E> toThrowable() {
        return (Throwables.TriPredicate<A, B, C, E>) this;
    }
}
