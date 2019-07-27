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
public interface BooleanPredicate extends Try.BooleanPredicate<RuntimeException> {

    static final BooleanPredicate ALWAYS_TRUE = new BooleanPredicate() {
        @Override
        public boolean test(boolean value) {
            return true;
        }
    };

    static final BooleanPredicate ALWAYS_FALSE = new BooleanPredicate() {
        @Override
        public boolean test(boolean value) {
            return false;
        }
    };

    static final BooleanPredicate IS_TRUE = new BooleanPredicate() {
        @Override
        public boolean test(boolean value) {
            return value == true;
        }
    };

    static final BooleanPredicate IS_FALSE = new BooleanPredicate() {
        @Override
        public boolean test(boolean value) {
            return value == false;
        }
    };

    @Override
    boolean test(boolean value);

    /**
     * Returns the specified instance
     * 
     * @param predicate
     * @return
     */
    static BooleanPredicate of(final BooleanPredicate predicate) {
        N.checkArgNotNull(predicate);

        return predicate;
    }

    default BooleanPredicate negate() {
        return (t) -> !test(t);
    }

    default BooleanPredicate and(BooleanPredicate other) {
        N.checkArgNotNull(other);

        return (t) -> test(t) && other.test(t);
    }

    default BooleanPredicate or(BooleanPredicate other) {
        N.checkArgNotNull(other);

        return (t) -> test(t) || other.test(t);
    }
}
