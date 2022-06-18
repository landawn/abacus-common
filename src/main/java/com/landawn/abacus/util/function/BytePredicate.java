/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */
package com.landawn.abacus.util.function;

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Throwables;

public interface BytePredicate extends Throwables.BytePredicate<RuntimeException> {

    BytePredicate ALWAYS_TRUE = value -> true;

    BytePredicate ALWAYS_FALSE = value -> false;

    BytePredicate IS_ZERO = value -> value == 0;

    BytePredicate NOT_ZERO = value -> value != 0;

    BytePredicate IS_POSITIVE = value -> value > 0;

    BytePredicate NOT_POSITIVE = value -> value <= 0;

    BytePredicate IS_NEGATIVE = value -> value < 0;

    BytePredicate NOT_NEGATIVE = value -> value >= 0;

    @Override
    boolean test(byte value);

    /**
     * Returns the specified instance
     *
     * @param predicate
     * @return
     */
    static BytePredicate of(final BytePredicate predicate) {
        N.checkArgNotNull(predicate);

        return predicate;
    }

    default BytePredicate negate() {
        return t -> !test(t);
    }

    default BytePredicate and(BytePredicate other) {
        N.checkArgNotNull(other);

        return t -> test(t) && other.test(t);
    }

    default BytePredicate or(BytePredicate other) {
        N.checkArgNotNull(other);

        return t -> test(t) || other.test(t);
    }

    static BytePredicate equal(byte targetByte) {
        return value -> value == targetByte;
    }

    static BytePredicate notEqual(byte targetByte) {
        return value -> value != targetByte;
    }

    static BytePredicate greaterThan(byte targetByte) {
        return value -> value > targetByte;
    }

    static BytePredicate greaterEqual(byte targetByte) {
        return value -> value >= targetByte;
    }

    static BytePredicate lessThan(byte targetByte) {
        return value -> value < targetByte;
    }

    static BytePredicate lessEqual(byte targetByte) {
        return value -> value <= targetByte;
    }

    static BytePredicate between(byte minValue, byte maxValue) {
        return value -> value > minValue && value < maxValue;
    }
}
