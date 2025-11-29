/*
 * Copyright (C) 2024 HaiYang Li
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
 * Represents a function that accepts a {@code boolean}-valued argument and produces a
 * {@code char}-valued result. This is the {@code boolean}-to-{@code char} primitive specialization
 * for {@link java.util.function.Function}.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsChar(boolean)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface BooleanToCharFunction {
    /**
     * A default function that converts a boolean value to a char: {@code true} to 'Y' and {@code false} to 'N'.
     * This follows a common convention for representing boolean values as single characters.
     */
    BooleanToCharFunction DEFAULT = value -> value ? 'Y' : 'N';

    /**
     * Applies this function to the given argument.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanToCharFunction converter = value -> value ? 'T' : 'F';
     * char result = converter.applyAsChar(true);  // Returns 'T'
     * }</pre>
     *
     * @param value the function argument
     * @return the function result as a {@code char}
     */
    char applyAsChar(boolean value);
}
