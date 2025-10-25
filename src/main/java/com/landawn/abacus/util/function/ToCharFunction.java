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
 * Represents a function that produces a char-valued result. This is the
 * char-producing primitive specialization for {@link java.util.function.Function}.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsChar(Object)}.
 *
 * @param <T> the type of the input to the function
 */
@FunctionalInterface
public interface ToCharFunction<T> extends Throwables.ToCharFunction<T, RuntimeException> { //NOSONAR

    /**
     * A predefined ToCharFunction instance that unboxes a Character object to a primitive char.
     * Returns 0 if the input is null, otherwise returns the char value of the Character object.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Character boxed = 'A';
     * char primitive = ToCharFunction.UNBOX.applyAsChar(boxed); // returns 'A'
     * char defaultValue = ToCharFunction.UNBOX.applyAsChar(null); // returns '\0' (0)
     * }</pre>
     */
    ToCharFunction<Character> UNBOX = value -> value == null ? 0 : value;

    /**
     * Applies this function to the given argument and returns a char result.
     *
     * @param value the function argument
     * @return the function result as a primitive char if any error occurs during function execution
     */
    @Override
    char applyAsChar(T value);
}