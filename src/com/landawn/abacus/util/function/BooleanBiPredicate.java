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
public interface BooleanBiPredicate extends Try.BooleanBiPredicate<RuntimeException> {

    static final BooleanBiPredicate ALWAYS_TRUE = new BooleanBiPredicate() {
        @Override
        public boolean test(boolean t, boolean u) {
            return true;
        }
    };

    static final BooleanBiPredicate ALWAYS_FALSE = new BooleanBiPredicate() {
        @Override
        public boolean test(boolean t, boolean u) {
            return false;
        }
    };

    static final BooleanBiPredicate BOTH_TRUE = new BooleanBiPredicate() {
        @Override
        public boolean test(boolean t, boolean u) {
            return t && u;
        }
    };

    static final BooleanBiPredicate BOTH_FALSE = new BooleanBiPredicate() {
        @Override
        public boolean test(boolean t, boolean u) {
            return t == false && u == false;
        }
    };

    static final BooleanBiPredicate EQUAL = new BooleanBiPredicate() {
        @Override
        public boolean test(boolean t, boolean u) {
            return t == u;
        }
    };

    static final BooleanBiPredicate NOT_EQUAL = new BooleanBiPredicate() {
        @Override
        public boolean test(boolean t, boolean u) {
            return t != u;
        }
    };

    @Override
    boolean test(boolean t, boolean u);

    default BooleanBiPredicate negate() {
        return (t, u) -> !test(t, u);
    }

    default BooleanBiPredicate and(BooleanBiPredicate other) {
        N.checkArgNotNull(other);

        return (t, u) -> test(t, u) && other.test(t, u);
    }

    default BooleanBiPredicate or(BooleanBiPredicate other) {
        N.checkArgNotNull(other);

        return (t, u) -> test(t, u) || other.test(t, u);
    }
}
