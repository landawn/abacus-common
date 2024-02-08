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
 *
 * @author Haiyang Li
 */
public interface BooleanPredicate extends Throwables.BooleanPredicate<RuntimeException> { //NOSONAR

    BooleanPredicate ALWAYS_TRUE = value -> true;

    BooleanPredicate ALWAYS_FALSE = value -> false;

    BooleanPredicate IS_TRUE = value -> value;

    BooleanPredicate IS_FALSE = value -> !value;

    /**
     * 
     *
     * @param value 
     * @return 
     */
    @Override
    boolean test(boolean value);

    /**
     * Returns the specified instance.
     *
     * @param predicate 
     * @return 
     */
    static BooleanPredicate of(final BooleanPredicate predicate) {
        N.checkArgNotNull(predicate);

        return predicate;
    }

    /**
     * 
     *
     * @return 
     */
    default BooleanPredicate negate() {
        return t -> !test(t);
    }

    /**
     * 
     *
     * @param other 
     * @return 
     */
    default BooleanPredicate and(BooleanPredicate other) {
        N.checkArgNotNull(other);

        return t -> test(t) && other.test(t);
    }

    /**
     * 
     *
     * @param other 
     * @return 
     */
    default BooleanPredicate or(BooleanPredicate other) {
        N.checkArgNotNull(other);

        return t -> test(t) || other.test(t);
    }
}
