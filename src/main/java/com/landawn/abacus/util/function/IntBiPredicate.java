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
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public interface IntBiPredicate extends Try.IntBiPredicate<RuntimeException> {

    static final IntBiPredicate ALWAYS_TRUE = new IntBiPredicate() {
        @Override
        public boolean test(int t, int u) {
            return true;
        }
    };

    static final IntBiPredicate ALWAYS_FALSE = new IntBiPredicate() {
        @Override
        public boolean test(int t, int u) {
            return false;
        }
    };

    static final IntBiPredicate EQUAL = new IntBiPredicate() {
        @Override
        public boolean test(int t, int u) {
            return t == u;
        }
    };

    static final IntBiPredicate NOT_EQUAL = new IntBiPredicate() {
        @Override
        public boolean test(int t, int u) {
            return t != u;
        }
    };

    static final IntBiPredicate GREATER_THAN = new IntBiPredicate() {
        @Override
        public boolean test(int t, int u) {
            return t > u;
        }
    };

    static final IntBiPredicate GREATER_EQUAL = new IntBiPredicate() {
        @Override
        public boolean test(int t, int u) {
            return t >= u;
        }
    };

    static final IntBiPredicate LESS_THAN = new IntBiPredicate() {
        @Override
        public boolean test(int t, int u) {
            return t < u;
        }
    };

    static final IntBiPredicate LESS_EQUAL = new IntBiPredicate() {
        @Override
        public boolean test(int t, int u) {
            return t <= u;
        }
    };

    @Override
    boolean test(int t, int u);

    default IntBiPredicate negate() {
        return (t, u) -> !test(t, u);
    }

    default IntBiPredicate and(IntBiPredicate other) {
        N.checkArgNotNull(other);

        return (t, u) -> test(t, u) && other.test(t, u);
    }

    default IntBiPredicate or(IntBiPredicate other) {
        N.checkArgNotNull(other);

        return (t, u) -> test(t, u) || other.test(t, u);
    }
}
