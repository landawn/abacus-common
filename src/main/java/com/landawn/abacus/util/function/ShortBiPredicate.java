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
 * Represents a predicate (boolean-valued function) of two {@code short}-valued arguments.
 * This is the primitive type specialization of {@link java.util.function.BiPredicate} for {@code short}.
 *
 * <p>This is a functional interface whose functional method is {@link #test(short, short)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @see java.util.function.Predicate
 * @see java.util.function.BiPredicate
 * @see ShortPredicate
 * @see ShortTriPredicate
 */
@FunctionalInterface
public interface ShortBiPredicate extends Throwables.ShortBiPredicate<RuntimeException> { //NOSONAR
    /**
     * A predicate that always returns {@code true} regardless of the input arguments.
     */
    ShortBiPredicate ALWAYS_TRUE = (a, b) -> true;
    /**
     * A predicate that always returns {@code false} regardless of the input arguments.
     */
    ShortBiPredicate ALWAYS_FALSE = (a, b) -> false;
    /**
     * A predicate that returns {@code true} if both arguments are equal.
     */
    ShortBiPredicate EQUAL = (a, b) -> a == b;
    /**
     * A predicate that returns {@code true} if the arguments are not equal.
     */
    ShortBiPredicate NOT_EQUAL = (a, b) -> a != b;
    /**
     * A predicate that returns {@code true} if the first argument is greater than the second.
     */
    ShortBiPredicate GREATER_THAN = (a, b) -> a > b;
    /**
     * A predicate that returns {@code true} if the first argument is greater than or equal to the second.
     */
    ShortBiPredicate GREATER_THAN_OR_EQUAL = (a, b) -> a >= b;
    /**
     * A predicate that returns {@code true} if the first argument is less than the second.
     */
    ShortBiPredicate LESS_THAN = (a, b) -> a < b;
    /**
     * A predicate that returns {@code true} if the first argument is less than or equal to the second.
     */
    ShortBiPredicate LESS_THAN_OR_EQUAL = (a, b) -> a <= b;

    /**
     * Evaluates this predicate on the given arguments.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortBiPredicate sumGreaterThan10 = (a, b) -> (a + b) > 10;
     * boolean result = sumGreaterThan10.test((short) 5, (short) 7);   // Returns true
     * }</pre>
     *
     * @param a the first input argument
     * @param b the second input argument
     * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
     */
    @Override
    boolean test(short a, short b);

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortBiPredicate equal = (a, b) -> a == b;
     * ShortBiPredicate notEqual = equal.negate();
     * }</pre>
     *
     * @return a predicate that represents the logical negation of this predicate
     */
    default ShortBiPredicate negate() {
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
     * ShortBiPredicate isPositive = (a, b) -> a > 0 && b > 0;
     * ShortBiPredicate lessThan100 = (a, b) -> a < 100 && b < 100;
     *
     * ShortBiPredicate inRange = isPositive.and(lessThan100);
     * boolean result1 = inRange.test((short) 10, (short) 20);     // returns true (both positive and < 100)
     * boolean result2 = inRange.test((short) -5, (short) 50);     // returns false (not both positive)
     * boolean result3 = inRange.test((short) 150, (short) 200);   // returns false (not both < 100)
     * }</pre>
     *
     * @param other a predicate that will be logically-ANDed with this predicate.
     * @return a composed predicate that represents the short-circuiting logical AND of this predicate and the {@code other} predicate
     * @throws IllegalArgumentException if {@code other} is {@code null}.
     */
    default ShortBiPredicate and(final ShortBiPredicate other) throws IllegalArgumentException {
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
     * ShortBiPredicate equals = ShortBiPredicate.EQUAL;
     * ShortBiPredicate bothZero = (a, b) -> a == 0 && b == 0;
     *
     * ShortBiPredicate equalsOrBothZero = equals.or(bothZero);
     * boolean result1 = equalsOrBothZero.test((short) 5, (short) 5);   // returns true (equal)
     * boolean result2 = equalsOrBothZero.test((short) 0, (short) 0);   // returns true (both zero AND equal)
     * boolean result3 = equalsOrBothZero.test((short) 3, (short) 7);   // returns false
     * }</pre>
     *
     * @param other a predicate that will be logically-ORed with this predicate.
     * @return a composed predicate that represents the short-circuiting logical OR of this predicate and the {@code other} predicate
     * @throws IllegalArgumentException if {@code other} is {@code null}.
     */
    default ShortBiPredicate or(final ShortBiPredicate other) throws IllegalArgumentException {
        N.checkArgNotNull(other, cs.other);

        return (a, b) -> test(a, b) || other.test(a, b);
    }

    /**
     * Returns this object as a {@link Throwables.ShortBiPredicate} view.
     *
     * <p>The returned object has the same behavior as this one. This method does not translate
     * exceptions or make the original implementation capable of throwing new checked exceptions; the
     * exception type parameter is for target-type compatibility with APIs that accept {@code Throwables.ShortBiPredicate}.
     *
     * @param <E> the target exception type for compatibility with {@code Throwables.ShortBiPredicate}
     * @return a {@link Throwables.ShortBiPredicate} view of this object
     */
    default <E extends Throwable> Throwables.ShortBiPredicate<E> toThrowable() {
        return (Throwables.ShortBiPredicate<E>) this;
    }
}
