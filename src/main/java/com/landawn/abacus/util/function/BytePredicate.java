/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */
package com.landawn.abacus.util.function;

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Throwables;

public interface BytePredicate extends Throwables.BytePredicate<RuntimeException> { //NOSONAR

    BytePredicate ALWAYS_TRUE = value -> true;

    BytePredicate ALWAYS_FALSE = value -> false;

    BytePredicate IS_ZERO = value -> value == 0;

    BytePredicate NOT_ZERO = value -> value != 0;

    BytePredicate IS_POSITIVE = value -> value > 0;

    BytePredicate NOT_POSITIVE = value -> value <= 0;

    BytePredicate IS_NEGATIVE = value -> value < 0;

    BytePredicate NOT_NEGATIVE = value -> value >= 0;

    /**
     *
     *
     * @param value
     * @return
     */
    @Override
    boolean test(byte value);

    /**
     * Returns the specified instance.
     *
     * @param predicate
     * @return
     */
    static BytePredicate of(final BytePredicate predicate) {
        N.checkArgNotNull(predicate);

        return predicate;
    }

    /**
     *
     *
     * @return
     */
    default BytePredicate negate() {
        return t -> !test(t);
    }

    /**
     *
     *
     * @param other
     * @return
     */
    default BytePredicate and(final BytePredicate other) {
        N.checkArgNotNull(other);

        return t -> test(t) && other.test(t);
    }

    /**
     *
     *
     * @param other
     * @return
     */
    default BytePredicate or(final BytePredicate other) {
        N.checkArgNotNull(other);

        return t -> test(t) || other.test(t);
    }

    /**
     *
     *
     * @param targetByte
     * @return
     */
    static BytePredicate equal(final byte targetByte) { //NOSONAR
        return value -> value == targetByte;
    }

    /**
     *
     *
     * @param targetByte
     * @return
     */
    static BytePredicate notEqual(final byte targetByte) {
        return value -> value != targetByte;
    }

    /**
     *
     *
     * @param targetByte
     * @return
     */
    static BytePredicate greaterThan(final byte targetByte) {
        return value -> value > targetByte;
    }

    /**
     *
     *
     * @param targetByte
     * @return
     */
    static BytePredicate greaterEqual(final byte targetByte) {
        return value -> value >= targetByte;
    }

    /**
     *
     *
     * @param targetByte
     * @return
     */
    static BytePredicate lessThan(final byte targetByte) {
        return value -> value < targetByte;
    }

    /**
     *
     *
     * @param targetByte
     * @return
     */
    static BytePredicate lessEqual(final byte targetByte) {
        return value -> value <= targetByte;
    }

    /**
     *
     *
     * @param minValue
     * @param maxValue
     * @return
     */
    static BytePredicate between(final byte minValue, final byte maxValue) {
        return value -> value > minValue && value < maxValue;
    }
}
