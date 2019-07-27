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
public interface LongTriPredicate extends Try.LongTriPredicate<RuntimeException> {

    public static final LongTriPredicate ALWAYS_TRUE = new LongTriPredicate() {
        @Override
        public boolean test(long a, long b, long c) {
            return true;
        }
    };

    public static final LongTriPredicate ALWAYS_FALSE = new LongTriPredicate() {
        @Override
        public boolean test(long a, long b, long c) {
            return false;
        }
    };

    @Override
    boolean test(long a, long b, long c);

    default LongTriPredicate negate() {
        return (a, b, c) -> !test(a, b, c);
    }

    default LongTriPredicate and(LongTriPredicate other) {
        N.checkArgNotNull(other);

        return (a, b, c) -> test(a, b, c) && other.test(a, b, c);
    }

    default LongTriPredicate or(LongTriPredicate other) {
        N.checkArgNotNull(other);

        return (a, b, c) -> test(a, b, c) || other.test(a, b, c);
    }
}
