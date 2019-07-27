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
public interface IntUnaryOperator extends java.util.function.IntUnaryOperator, Try.IntUnaryOperator<RuntimeException> {

    @Override
    int applyAsInt(int operand);

    @Override
    default IntUnaryOperator compose(java.util.function.IntUnaryOperator before) {
        N.checkArgNotNull(before);
        return (int v) -> applyAsInt(before.applyAsInt(v));
    }

    @Override
    default IntUnaryOperator andThen(java.util.function.IntUnaryOperator after) {
        N.checkArgNotNull(after);
        return (int t) -> after.applyAsInt(applyAsInt(t));
    }

    static IntUnaryOperator identity() {
        return t -> t;
    }
}
