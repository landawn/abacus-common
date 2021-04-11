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
import com.landawn.abacus.util.Throwables;

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public interface DoubleBiPredicate extends Throwables.DoubleBiPredicate<RuntimeException> {

    static final DoubleBiPredicate ALWAYS_TRUE = new DoubleBiPredicate() {
        @Override
        public boolean test(double t, double u) {
            return true;
        }
    };

    static final DoubleBiPredicate ALWAYS_FALSE = new DoubleBiPredicate() {
        @Override
        public boolean test(double t, double u) {
            return false;
        }
    };
    static final DoubleBiPredicate EQUAL = new DoubleBiPredicate() {
        @Override
        public boolean test(double t, double u) {
            return Double.compare(t, u) == 0;
        }
    };

    static final DoubleBiPredicate NOT_EQUAL = new DoubleBiPredicate() {
        @Override
        public boolean test(double t, double u) {
            return Double.compare(t, u) != 0;
        }
    };

    static final DoubleBiPredicate GREATER_THAN = new DoubleBiPredicate() {
        @Override
        public boolean test(double t, double u) {
            return Double.compare(t, u) > 0;
        }
    };

    static final DoubleBiPredicate GREATER_EQUAL = new DoubleBiPredicate() {
        @Override
        public boolean test(double t, double u) {
            return Double.compare(t, u) >= 0;
        }
    };

    static final DoubleBiPredicate LESS_THAN = new DoubleBiPredicate() {
        @Override
        public boolean test(double t, double u) {
            return Double.compare(t, u) < 0;
        }
    };

    static final DoubleBiPredicate LESS_EQUAL = new DoubleBiPredicate() {
        @Override
        public boolean test(double t, double u) {
            return Double.compare(t, u) <= 0;
        }
    };

    @Override
    boolean test(double t, double u);

    default DoubleBiPredicate negate() {
        return (t, u) -> !test(t, u);
    }

    default DoubleBiPredicate and(DoubleBiPredicate other) {
        N.checkArgNotNull(other);

        return (t, u) -> test(t, u) && other.test(t, u);
    }

    default DoubleBiPredicate or(DoubleBiPredicate other) {
        N.checkArgNotNull(other);

        return (t, u) -> test(t, u) || other.test(t, u);
    }
}
