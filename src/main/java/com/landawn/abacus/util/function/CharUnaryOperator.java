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

import com.landawn.abacus.util.Throwables;

/**
 *
 *
 */
public interface CharUnaryOperator extends Throwables.CharUnaryOperator<RuntimeException> { //NOSONAR

    /**
     *
     *
     * @param operand
     * @return
     */
    @Override
    char applyAsChar(char operand);

    /**
     *
     *
     * @param before
     * @return
     */
    default CharUnaryOperator compose(final CharUnaryOperator before) {
        return v -> applyAsChar(before.applyAsChar(v));
    }

    /**
     *
     *
     * @param after
     * @return
     */
    default CharUnaryOperator andThen(final CharUnaryOperator after) {
        return t -> after.applyAsChar(applyAsChar(t));
    }

    /**
     *
     *
     * @return
     */
    static CharUnaryOperator identity() {
        return t -> t;
    }
}
