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
 * Represents a function that produces a char-valued result.
 * This is the {@code char}-producing primitive specialization for {@link java.util.function.Function}.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsChar(Object)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <T> the type of the input to the function
 *
 * @see java.util.function.Function
 */
@FunctionalInterface
public interface ToCharFunction<T> extends Throwables.ToCharFunction<T, RuntimeException> { //NOSONAR
    /**
     * A function that safely unboxes a Character object to a primitive char value.
     * Returns 0 ('\0') for {@code null} inputs and the char value for {@code non-null} inputs.
     * This provides null-safe unboxing behavior.
     */
    ToCharFunction<Character> UNBOX = value -> value == null ? 0 : value;

    /**
     * Applies this function to the given argument.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ToCharFunction<Character> unbox = ToCharFunction.UNBOX;
     * char result1 = unbox.applyAsChar('A');    // returns 'A'
     * char result2 = unbox.applyAsChar(null);   // returns '\0' (0)
     *
     * ToCharFunction<String> firstChar = str -> str != null && str.length() > 0 ? str.charAt(0) : '\0';
     * char result3 = firstChar.applyAsChar("Hello");   // returns 'H'
     *
     * ToCharFunction<Integer> digitToChar = n -> Character.forDigit(n, 10);
     * char result4 = digitToChar.applyAsChar(5);   // returns '5'
     * }</pre>
     *
     * @param value the function argument
     * @return the function result as a char value
     */
    @Override
    char applyAsChar(T value);

    /**
     * Returns this object as a {@link Throwables.ToCharFunction} view.
     *
     * <p>The returned object has the same behavior as this one. This method does not translate
     * exceptions or make the original implementation capable of throwing new checked exceptions; the
     * exception type parameter is for target-type compatibility with APIs that accept {@code Throwables.ToCharFunction}.
     *
     * @param <E> the target exception type for compatibility with {@code Throwables.ToCharFunction}
     * @return a {@link Throwables.ToCharFunction} view of this object
     */
    default <E extends Throwable> Throwables.ToCharFunction<T, E> toThrowable() {
        return (Throwables.ToCharFunction<T, E>) this;
    }
}
