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

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.cs;

/**
 * Represents a predicate (boolean-valued function) of two {@code byte}-valued arguments.
 * This is the primitive type specialization of {@link java.util.function.BiPredicate} for {@code byte}.
 *
 * <p>This is a functional interface whose functional method is {@link #test(byte, byte)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface ByteBiPredicate extends Throwables.ByteBiPredicate<RuntimeException> { //NOSONAR
    /**
     * A predicate that always returns {@code true} regardless of the input arguments.
     */
    ByteBiPredicate ALWAYS_TRUE = (a, b) -> true;
    /**
     * A predicate that always returns {@code false} regardless of the input arguments.
     */
    ByteBiPredicate ALWAYS_FALSE = (a, b) -> false;
    /**
     * A predicate that returns {@code true} if both arguments are equal.
     */
    ByteBiPredicate EQUAL = (a, b) -> a == b;
    /**
     * A predicate that returns {@code true} if the arguments are not equal.
     */
    ByteBiPredicate NOT_EQUAL = (a, b) -> a != b;
    /**
     * A predicate that returns {@code true} if the first argument is greater than the second.
     */
    ByteBiPredicate GREATER_THAN = (a, b) -> a > b;
    /**
     * A predicate that returns {@code true} if the first argument is greater than or equal to the second.
     */
    ByteBiPredicate GREATER_THAN_OR_EQUAL = (a, b) -> a >= b;
    /**
     * A predicate that returns {@code true} if the first argument is less than the second.
     */
    ByteBiPredicate LESS_THAN = (a, b) -> a < b;
    /**
     * A predicate that returns {@code true} if the first argument is less than or equal to the second.
     */
    ByteBiPredicate LESS_THAN_OR_EQUAL = (a, b) -> a <= b;

    /**
     * Evaluates this predicate on the given arguments.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteBiPredicate sumGreaterThan10 = (a, b) -> (a + b) > 10;
     * boolean result = sumGreaterThan10.test((byte)5, (byte)7);   // Returns true
     * }</pre>
     *
     * @param a the first input argument
     * @param b the second input argument
     * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
     */
    @Override
    boolean test(byte a, byte b);

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteBiPredicate equal = (a, b) -> a == b;
     * ByteBiPredicate notEqual = equal.negate();
     * }</pre>
     *
     * @return a predicate that represents the logical negation of this predicate
     */
    default ByteBiPredicate negate() {
        return (a, b) -> !test(a, b);
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
     * ByteBiPredicate bothPositive = (a, b) -> a > 0 && b > 0;
     * ByteBiPredicate sumUnder100 = (a, b) -> (a + b) < 100;
     * ByteBiPredicate combined = bothPositive.and(sumUnder100);
     * }</pre>
     *
     * @param other a predicate that will be logically-ANDed with this predicate. Must not be {@code null}.
     * @return a composed predicate that represents the short-circuiting logical AND of this predicate and the {@code other} predicate
     * @throws IllegalArgumentException if {@code other} is null
     */
    default ByteBiPredicate and(final ByteBiPredicate other) {
        N.checkArgNotNull(other, cs.other);
        return (a, b) -> test(a, b) && other.test(a, b);
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
     * ByteBiPredicate eitherZero = (a, b) -> a == 0 || b == 0;
     * ByteBiPredicate eitherNegative = (a, b) -> a < 0 || b < 0;
     * ByteBiPredicate combined = eitherZero.or(eitherNegative);
     * }</pre>
     *
     * @param other a predicate that will be logically-ORed with this predicate. Must not be {@code null}.
     * @return a composed predicate that represents the short-circuiting logical OR of this predicate and the {@code other} predicate
     * @throws IllegalArgumentException if {@code other} is null
     */
    default ByteBiPredicate or(final ByteBiPredicate other) {
        N.checkArgNotNull(other, cs.other);
        return (a, b) -> test(a, b) || other.test(a, b);
    }

    /**
     * Returns this object as a {@link Throwables.ByteBiPredicate} view.
     *
     * <p>The returned object has the same behavior as this one. This method does not translate
     * exceptions or make the original implementation capable of throwing new checked exceptions; the
     * exception type parameter is for target-type compatibility with APIs that accept {@code Throwables.ByteBiPredicate}.
     *
     * @param <E> the target exception type for compatibility with {@code Throwables.ByteBiPredicate}
     * @return a {@link Throwables.ByteBiPredicate} view of this object
     */
    default <E extends Throwable> Throwables.ByteBiPredicate<E> toThrowable() {
        return (Throwables.ByteBiPredicate<E>) this;
    }
}
