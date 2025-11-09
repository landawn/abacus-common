/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */
package com.landawn.abacus.util.function;

import com.landawn.abacus.util.Throwables;

/**
 * Represents a predicate (boolean-valued function) of one byte-valued argument.
 * This is the byte-consuming primitive type specialization of {@link java.util.function.Predicate}.
 *
 * <p>This is a functional interface whose functional method is {@link #test(byte)}.
 *
 * @see java.util.function.Predicate
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface BytePredicate extends Throwables.BytePredicate<RuntimeException> { //NOSONAR
    /**
     * A predicate that always evaluates to {@code true}, regardless of the input byte value.
     */
    BytePredicate ALWAYS_TRUE = value -> true;
    /**
     * A predicate that always evaluates to {@code false}, regardless of the input byte value.
     */
    BytePredicate ALWAYS_FALSE = value -> false;
    /**
     * A predicate that tests if the byte value is equal to zero.
     */
    BytePredicate IS_ZERO = value -> value == 0;
    /**
     * A predicate that tests if the byte value is not equal to zero.
     */
    BytePredicate NOT_ZERO = value -> value != 0;
    /**
     * A predicate that tests if the byte value is positive (greater than zero).
     */
    BytePredicate IS_POSITIVE = value -> value > 0;
    /**
     * A predicate that tests if the byte value is not positive (less than or equal to zero).
     */
    BytePredicate NOT_POSITIVE = value -> value <= 0;
    /**
     * A predicate that tests if the byte value is negative (less than zero).
     */
    BytePredicate IS_NEGATIVE = value -> value < 0;
    /**
     * A predicate that tests if the byte value is not negative (greater than or equal to zero).
     */
    BytePredicate NOT_NEGATIVE = value -> value >= 0;

    /**
     * Evaluates this predicate on the given byte argument.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BytePredicate isPositive = value -> value > 0;
     * boolean result = isPositive.test((byte) 5); // Returns true
     * }</pre>
     *
     * @param value the byte input argument
     * @return {@code true} if the input argument matches the predicate, {@code false} otherwise
     */
    @Override
    boolean test(byte value);

    /**
     * Returns the specified BytePredicate instance.
     * This method is useful for type inference or when you need to explicitly cast a lambda expression.
     *
     * @param predicate the predicate to return
     * @return the same predicate instance
     */
    static BytePredicate of(final BytePredicate predicate) {
        return predicate;
    }

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     * The returned predicate will return {@code true} when this predicate returns {@code false}, and vice versa.
     *
     * @return a predicate that represents the logical negation of this predicate
     */
    default BytePredicate negate() {
        return t -> !test(t);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical AND of this predicate and another.
     * When evaluating the composed predicate, if this predicate is {@code false}, then the {@code other} predicate is not evaluated.
     *
     * @param other a predicate that will be logically-ANDed with this predicate
     * @return a composed predicate that represents the short-circuiting logical AND of this predicate and the {@code other} predicate
     */
    default BytePredicate and(final BytePredicate other) {
        return t -> test(t) && other.test(t);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical OR of this predicate and another.
     * When evaluating the composed predicate, if this predicate is {@code true}, then the {@code other} predicate is not evaluated.
     *
     * @param other a predicate that will be logically-ORed with this predicate
     * @return a composed predicate that represents the short-circuiting logical OR of this predicate and the {@code other} predicate
     */
    default BytePredicate or(final BytePredicate other) {
        return t -> test(t) || other.test(t);
    }

    /**
     * Returns a predicate that tests if the byte value is equal to the specified target value.
     *
     * @param targetByte the value to compare against
     * @return a predicate that tests if the input is equal to {@code targetByte}
     */
    static BytePredicate equal(final byte targetByte) { //NOSONAR
        return value -> value == targetByte;
    }

    /**
     * Returns a predicate that tests if the byte value is not equal to the specified target value.
     *
     * @param targetByte the value to compare against
     * @return a predicate that tests if the input is not equal to {@code targetByte}
     */
    static BytePredicate notEqual(final byte targetByte) {
        return value -> value != targetByte;
    }

    /**
     * Returns a predicate that tests if the byte value is greater than the specified target value.
     *
     * @param targetByte the value to compare against
     * @return a predicate that tests if the input is greater than {@code targetByte}
     */
    static BytePredicate greaterThan(final byte targetByte) {
        return value -> value > targetByte;
    }

    /**
     * Returns a predicate that tests if the byte value is greater than or equal to the specified target value.
     *
     * @param targetByte the value to compare against
     * @return a predicate that tests if the input is greater than or equal to {@code targetByte}
     */
    static BytePredicate greaterEqual(final byte targetByte) {
        return value -> value >= targetByte;
    }

    /**
     * Returns a predicate that tests if the byte value is less than the specified target value.
     *
     * @param targetByte the value to compare against
     * @return a predicate that tests if the input is less than {@code targetByte}
     */
    static BytePredicate lessThan(final byte targetByte) {
        return value -> value < targetByte;
    }

    /**
     * Returns a predicate that tests if the byte value is less than or equal to the specified target value.
     *
     * @param targetByte the value to compare against
     * @return a predicate that tests if the input is less than or equal to {@code targetByte}
     */
    static BytePredicate lessEqual(final byte targetByte) {
        return value -> value <= targetByte;
    }

    /**
     * Returns a predicate that tests if the byte value is between the specified minimum and maximum values (exclusive).
     * The test returns {@code true} if the value is greater than {@code minValue} AND less than {@code maxValue}.
     *
     * @param minValue the exclusive lower bound
     * @param maxValue the exclusive upper bound
     * @return a predicate that tests if the input is between {@code minValue} and {@code maxValue} (exclusive)
     */
    static BytePredicate between(final byte minValue, final byte maxValue) {
        return value -> value > minValue && value < maxValue;
    }
}
