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
public interface LongPredicate extends java.util.function.LongPredicate, Try.LongPredicate<RuntimeException> {

    static final LongPredicate ALWAYS_TRUE = new LongPredicate() {
        @Override
        public boolean test(long value) {
            return true;
        }
    };

    static final LongPredicate ALWAYS_FALSE = new LongPredicate() {
        @Override
        public boolean test(long value) {
            return false;
        }
    };

    static final LongPredicate IS_ZERO = new LongPredicate() {
        @Override
        public boolean test(long value) {
            return value == 0;
        }
    };

    static final LongPredicate NOT_ZERO = new LongPredicate() {
        @Override
        public boolean test(long value) {
            return value != 0;
        }
    };

    static final LongPredicate IS_POSITIVE = new LongPredicate() {
        @Override
        public boolean test(long value) {
            return value > 0;
        }
    };

    static final LongPredicate NOT_POSITIVE = new LongPredicate() {
        @Override
        public boolean test(long value) {
            return value <= 0;
        }
    };

    static final LongPredicate IS_NEGATIVE = new LongPredicate() {
        @Override
        public boolean test(long value) {
            return value < 0;
        }
    };

    static final LongPredicate NOT_NEGATIVE = new LongPredicate() {
        @Override
        public boolean test(long value) {
            return value >= 0;
        }
    };

    @Override
    boolean test(long value);

    @Override
    default LongPredicate negate() {
        return (value) -> !test(value);
    }

    @Override
    default LongPredicate or(java.util.function.LongPredicate other) {
        N.checkArgNotNull(other);
        return (value) -> test(value) || other.test(value);
    }

    @Override
    default LongPredicate and(java.util.function.LongPredicate other) {
        N.checkArgNotNull(other);
        return (value) -> test(value) && other.test(value);
    }

    /**
     * Returns the specified instance
     * 
     * @param predicate
     * @return
     */
    static LongPredicate of(final LongPredicate predicate) {
        N.checkArgNotNull(predicate);

        return predicate;
    }

    static LongPredicate equal(long targetLong) {
        return value -> value == targetLong;
    }

    static LongPredicate notEqual(long targetLong) {
        return value -> value != targetLong;
    }

    static LongPredicate greaterThan(long targetLong) {
        return value -> value > targetLong;
    }

    static LongPredicate greaterEqual(long targetLong) {
        return value -> value >= targetLong;
    }

    static LongPredicate lessThan(long targetLong) {
        return value -> value < targetLong;
    }

    static LongPredicate lessEqual(long targetLong) {
        return value -> value <= targetLong;
    }

    static LongPredicate between(long minValue, long maxValue) {
        return value -> value > minValue && value < maxValue;
    }
}
