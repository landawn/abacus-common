/*
 * Copyright (C) 2016 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util.function;

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Try;

/**
 * Refer to JDK API documentation at: <a href="https://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html">https://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html</a>
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public interface DoublePredicate extends java.util.function.DoublePredicate, Try.DoublePredicate<RuntimeException> {
    static final DoublePredicate ALWAYS_TRUE = new DoublePredicate() {
        @Override
        public boolean test(double value) {
            return true;
        }
    };

    static final DoublePredicate ALWAYS_FALSE = new DoublePredicate() {
        @Override
        public boolean test(double value) {
            return false;
        }
    };

    static final DoublePredicate IS_ZERO = new DoublePredicate() {
        @Override
        public boolean test(double value) {
            return value == 0;
        }
    };

    static final DoublePredicate NOT_ZERO = new DoublePredicate() {
        @Override
        public boolean test(double value) {
            return value != 0;
        }
    };

    static final DoublePredicate IS_POSITIVE = new DoublePredicate() {
        @Override
        public boolean test(double value) {
            return value > 0;
        }
    };

    static final DoublePredicate NOT_POSITIVE = new DoublePredicate() {
        @Override
        public boolean test(double value) {
            return value <= 0;
        }
    };

    static final DoublePredicate IS_NEGATIVE = new DoublePredicate() {
        @Override
        public boolean test(double value) {
            return value < 0;
        }
    };

    static final DoublePredicate NOT_NEGATIVE = new DoublePredicate() {
        @Override
        public boolean test(double value) {
            return value >= 0;
        }
    };

    @Override
    boolean test(double value);

    @Override
    default DoublePredicate negate() {
        return (value) -> !test(value);
    }

    @Override
    default DoublePredicate and(java.util.function.DoublePredicate other) {
        N.checkArgNotNull(other);
        return (value) -> test(value) && other.test(value);
    }

    @Override
    default DoublePredicate or(java.util.function.DoublePredicate other) {
        N.checkArgNotNull(other);
        return (value) -> test(value) || other.test(value);
    }

    /**
     * Returns the specified instance
     * 
     * @param predicate
     * @return
     */
    static DoublePredicate of(final DoublePredicate predicate) {
        N.checkArgNotNull(predicate);

        return predicate;
    }

    static DoublePredicate equal(double targetDouble) {
        return value -> value == targetDouble;
    }

    static DoublePredicate notEqual(double targetDouble) {
        return value -> value != targetDouble;
    }

    static DoublePredicate greaterThan(double targetDouble) {
        return value -> N.compare(value, targetDouble) > 0;
    }

    static DoublePredicate greaterEqual(double targetDouble) {
        return value -> N.compare(value, targetDouble) >= 0;
    }

    static DoublePredicate lessThan(double targetDouble) {
        return value -> N.compare(value, targetDouble) < 0;
    }

    static DoublePredicate lessEqual(double targetDouble) {
        return value -> N.compare(value, targetDouble) <= 0;
    }

    static DoublePredicate between(double minValue, double maxValue) {
        return value -> N.compare(value, minValue) > 0 && N.compare(value, maxValue) < 0;
    }
}
