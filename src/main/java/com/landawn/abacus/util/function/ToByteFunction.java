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
 * Represents a function that produces a byte-valued result.
 * This is the {@code byte}-producing primitive specialization for {@link java.util.function.Function}.
 * 
 * <p>This is a functional interface whose functional method is {@link #applyAsByte(Object)}.
 * 
 * @param <T> the type of the input to the function
 * 
 * @see java.util.function.Function
 * @see java.util.function.ToIntFunction
 */
@FunctionalInterface
public interface ToByteFunction<T> extends Throwables.ToByteFunction<T, RuntimeException> { //NOSONAR

    /**
     * A function that safely unboxes a Byte object to a primitive byte value.
     * Returns 0 for null inputs and the byte value for non-null inputs.
     * This provides null-safe unboxing behavior.
     */
    ToByteFunction<Byte> UNBOX = value -> value == null ? 0 : value;

    /**
     * A function that converts any Number to a byte value.
     * Returns 0 for null inputs and calls {@link Number#byteValue()} for non-null inputs.
     * This provides a convenient way to convert various numeric types to byte.
     */
    ToByteFunction<Number> FROM_NUM = value -> value == null ? 0 : value.byteValue();

    /**
     * Applies this function to the given argument.
     *
     * @param value the function argument
     * @return the function result as a byte value
     */
    @Override
    byte applyAsByte(T value);
}