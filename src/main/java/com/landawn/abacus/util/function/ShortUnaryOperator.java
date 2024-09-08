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
public interface ShortUnaryOperator extends Throwables.ShortUnaryOperator<RuntimeException> { //NOSONAR

    /**
     *
     *
     * @param operand
     * @return
     */
    @Override
    short applyAsShort(short operand);

    /**
     *
     *
     * @param before
     * @return
     */
    default ShortUnaryOperator compose(final ShortUnaryOperator before) {
        N.checkArgNotNull(before);

        return v -> applyAsShort(before.applyAsShort(v));
    }

    /**
     *
     *
     * @param after
     * @return
     */
    default ShortUnaryOperator andThen(final ShortUnaryOperator after) {
        N.checkArgNotNull(after);

        return t -> after.applyAsShort(applyAsShort(t));
    }

    /**
     *
     *
     * @return
     */
    static ShortUnaryOperator identity() {
        return t -> t;
    }
}
