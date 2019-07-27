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
 * Refer to JDK API documentation at: <a href="https://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html">https://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html</a>
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public interface DoubleUnaryOperator extends java.util.function.DoubleUnaryOperator, Try.DoubleUnaryOperator<RuntimeException> {

    @Override
    double applyAsDouble(double operand);

    @Override
    default DoubleUnaryOperator compose(java.util.function.DoubleUnaryOperator before) {
        N.checkArgNotNull(before);
        return (double v) -> applyAsDouble(before.applyAsDouble(v));
    }

    @Override
    default DoubleUnaryOperator andThen(java.util.function.DoubleUnaryOperator after) {
        N.checkArgNotNull(after);
        return (double t) -> after.applyAsDouble(applyAsDouble(t));
    }

    static DoubleUnaryOperator identity() {
        return t -> t;
    }
}
