/*
 * Copyright (C) 2024 HaiYang Li
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
 * Represents a predicate (boolean-valued function) of two {@code int}-valued arguments and a single
 * object-valued argument. This is a specialization of predicate for two primitive {@code int} values
 * and one reference type.
 * 
 * <p>This is a functional interface whose functional method is {@link #test(int, int, Object)}.
 * 
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a>
 *
 * @param <T> the type of the object argument to the predicate
 */
@FunctionalInterface
public interface BiIntObjPredicate<T> extends Throwables.BiIntObjPredicate<T, RuntimeException> { // NOSONAR

    /**
     * Evaluates this predicate on the given arguments.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiIntObjPredicate<String> lengthChecker = (i, j, str) -> str.length() == (i + j);
     * boolean result = lengthChecker.test(3, 2, "hello"); // Returns true (5 == 3+2)
     * }</pre>
     *
     * @param i the first input argument (int value)
     * @param j the second input argument (int value)
     * @param t the third input argument (object value)
     * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
     */
    @Override
    boolean test(int i, int j, T t);

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiIntObjPredicate<String> isEqual = (i, j, str) -> str.length() == (i + j);
     * BiIntObjPredicate<String> isNotEqual = isEqual.negate();
     * }</pre>
     *
     * @return a predicate that represents the logical negation of this predicate
     */
    default BiIntObjPredicate<T> negate() {
        return (i, j, t) -> !test(i, j, t);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical AND of this predicate and another.
     * When evaluating the composed predicate, if this predicate is {@code false}, then the {@code other}
     * predicate is not evaluated.
     *
     * <p>Any exceptions thrown during evaluation of either predicate are relayed to the caller;
     * if evaluation of this predicate throws an exception, the {@code other} predicate will not be evaluated.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiIntObjPredicate<String> positiveSum = (i, j, s) -> (i + j) > 0;
     * BiIntObjPredicate<String> notEmpty = (i, j, s) -> !s.isEmpty();
     * BiIntObjPredicate<String> combined = positiveSum.and(notEmpty);
     * }</pre>
     *
     * @param other a predicate that will be logically-ANDed with this predicate. Must not be {@code null}.
     * @return a composed predicate that represents the short-circuiting logical AND of this predicate and the {@code other} predicate
     */
    default BiIntObjPredicate<T> and(final BiIntObjPredicate<T> other) {
        return (i, j, t) -> test(i, j, t) && other.test(i, j, t);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical OR of this predicate and another.
     * When evaluating the composed predicate, if this predicate is {@code true}, then the {@code other}
     * predicate is not evaluated.
     *
     * <p>Any exceptions thrown during evaluation of either predicate are relayed to the caller;
     * if evaluation of this predicate throws an exception, the {@code other} predicate will not be evaluated.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiIntObjPredicate<String> largeSum = (i, j, s) -> (i + j) > 100;
     * BiIntObjPredicate<String> longString = (i, j, s) -> s.length() > 50;
     * BiIntObjPredicate<String> combined = largeSum.or(longString);
     * }</pre>
     *
     * @param other a predicate that will be logically-ORed with this predicate. Must not be {@code null}.
     * @return a composed predicate that represents the short-circuiting logical OR of this predicate and the {@code other} predicate
     */
    default BiIntObjPredicate<T> or(final BiIntObjPredicate<T> other) {
        return (i, j, t) -> test(i, j, t) || other.test(i, j, t);
    }
}