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

import com.landawn.abacus.util.Try;

/**
 * Refer to JDK API documentation at: <a href="https://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html">https://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html</a>
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public interface ToDoubleFunction<T> extends java.util.function.ToDoubleFunction<T>, Try.ToDoubleFunction<T, RuntimeException> {

    static final ToDoubleFunction<Double> UNBOX = new ToDoubleFunction<Double>() {
        @Override
        public double applyAsDouble(Double value) {
            return value == null ? 0 : value.doubleValue();
        }
    };

    static final ToDoubleFunction<Number> FROM_NUM = new ToDoubleFunction<Number>() {
        @Override
        public double applyAsDouble(Number value) {
            return value == null ? 0 : value.doubleValue();
        }
    };

    /**
     * @deprecated replaced with {@code FROM_NUM}.
     */
    @Deprecated
    static final ToDoubleFunction<Number> NUM = FROM_NUM;

    @Override
    double applyAsDouble(T value);
}
