/*
 * Copyright (C) 2016 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.landawn.abacus.util.function;

import com.landawn.abacus.util.Throwables;

/**
 * Represents a function that accepts a double-valued argument and produces an int-valued result.
 * This is the double-to-int primitive specialization for {@link java.util.function.Function}.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsInt(double)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @see java.util.function.Function
 * @see java.util.function.DoubleToIntFunction
 * @see DoubleToLongFunction
 */
@FunctionalInterface
public interface DoubleToIntFunction extends Throwables.DoubleToIntFunction<RuntimeException>, java.util.function.DoubleToIntFunction { //NOSONAR
    /**
     * A default implementation that casts the double value to int.
     * Note that this conversion truncates the decimal part and may overflow for large double values.
     */
    DoubleToIntFunction DEFAULT = value -> (int) value;

    /**
     * Applies this function to the given argument.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleToIntFunction truncate = DoubleToIntFunction.DEFAULT;
     * int result = truncate.applyAsInt(3.14);  // Returns 3
     *
     * DoubleToIntFunction rounder = val -> (int) Math.round(val);
     * int result2 = rounder.applyAsInt(3.7);  // Returns 4
     * }</pre>
     *
     * @param value the double function argument
     * @return the int function result
     */
    @Override
    int applyAsInt(double value);
}
