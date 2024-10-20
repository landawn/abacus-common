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
 * Refer to JDK API documentation at: <a href="https://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html">https://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html</a>
 *
 */
public interface DoubleUnaryOperator extends Throwables.DoubleUnaryOperator<RuntimeException>, java.util.function.DoubleUnaryOperator { //NOSONAR

    /**
     *
     *
     * @param operand
     * @return
     */
    @Override
    double applyAsDouble(double operand);

    /**
     *
     *
     * @param before
     * @return
     */
    @Override
    default DoubleUnaryOperator compose(final java.util.function.DoubleUnaryOperator before) {
        return (final double v) -> applyAsDouble(before.applyAsDouble(v));
    }

    /**
     *
     *
     * @param after
     * @return
     */
    @Override
    default DoubleUnaryOperator andThen(final java.util.function.DoubleUnaryOperator after) {
        return (final double t) -> after.applyAsDouble(applyAsDouble(t));
    }

    /**
     *
     *
     * @return
     */
    static DoubleUnaryOperator identity() {
        return t -> t;
    }
}
