/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */
package com.landawn.abacus.util.function;

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Try;

public interface BytePredicate extends Try.BytePredicate<RuntimeException> {

    static final BytePredicate ALWAYS_TRUE = new BytePredicate() {
        @Override
        public boolean test(byte value) {
            return true;
        }
    };

    static final BytePredicate ALWAYS_FALSE = new BytePredicate() {
        @Override
        public boolean test(byte value) {
            return false;
        }
    };

    static final BytePredicate IS_ZERO = new BytePredicate() {
        @Override
        public boolean test(byte value) {
            return value == 0;
        }
    };

    static final BytePredicate NOT_ZERO = new BytePredicate() {
        @Override
        public boolean test(byte value) {
            return value != 0;
        }
    };

    static final BytePredicate IS_POSITIVE = new BytePredicate() {
        @Override
        public boolean test(byte value) {
            return value > 0;
        }
    };

    static final BytePredicate NOT_POSITIVE = new BytePredicate() {
        @Override
        public boolean test(byte value) {
            return value <= 0;
        }
    };

    static final BytePredicate IS_NEGATIVE = new BytePredicate() {
        @Override
        public boolean test(byte value) {
            return value < 0;
        }
    };

    static final BytePredicate NOT_NEGATIVE = new BytePredicate() {
        @Override
        public boolean test(byte value) {
            return value >= 0;
        }
    };

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
        return (t) -> !test(t);
    }

    default BytePredicate and(BytePredicate other) {
        N.checkArgNotNull(other);

        return (t) -> test(t) && other.test(t);
    }

    default BytePredicate or(BytePredicate other) {
        N.checkArgNotNull(other);

        return (t) -> test(t) || other.test(t);
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
