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
public interface BooleanUnaryOperator extends Throwables.BooleanUnaryOperator<RuntimeException> { //NOSONAR

    /**
    * 
    *
    * @param operand 
    * @return 
    */
    @Override
    boolean applyAsBoolean(boolean operand);

    /**
     * 
     *
     * @param before 
     * @return 
     */
    default BooleanUnaryOperator compose(BooleanUnaryOperator before) {
        N.checkArgNotNull(before);

        return v -> applyAsBoolean(before.applyAsBoolean(v));
    }

    /**
     * 
     *
     * @param after 
     * @return 
     */
    default BooleanUnaryOperator andThen(BooleanUnaryOperator after) {
        N.checkArgNotNull(after);

        return t -> after.applyAsBoolean(applyAsBoolean(t));
    }

    /**
     * 
     *
     * @return 
     */
    static BooleanUnaryOperator identity() {
        return t -> t;
    }
}
