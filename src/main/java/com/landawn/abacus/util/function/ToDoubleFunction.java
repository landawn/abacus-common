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

import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Throwables;

/**
 * Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a>
 *
 */
public interface ToDoubleFunction<T> extends Throwables.ToDoubleFunction<T, RuntimeException>, java.util.function.ToDoubleFunction<T> { //NOSONAR

    ToDoubleFunction<Double> UNBOX = value -> value == null ? 0 : value;

    ToDoubleFunction<Number> FROM_NUM = value -> value == null ? 0 : Numbers.toDouble(value);

    /**
     *
     * @param value
     * @return
     */
    @Override
    double applyAsDouble(T value);
}
