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

/**
 * Represents a function that accepts a double-valued argument and produces a float-valued result.
 * This is the double-to-float primitive specialization for {@link java.util.function.Function}.
 * 
 * <p>This is a functional interface whose functional method is {@link #applyAsFloat(double)}.
 *
 * @see java.util.function.Function
 * @see DoubleToIntFunction
 * @see DoubleToLongFunction
 */
@FunctionalInterface
public interface DoubleToFloatFunction {

    /**
     * A default implementation that casts the double value to float.
     * Note that this conversion may lose precision for large double values.
     */
    DoubleToFloatFunction DEFAULT = value -> (float) value;

    /**
     * Applies this function to the given argument.
     *
     * @param value the double function argument
     * @return the float function result
     */
    float applyAsFloat(double value);
}