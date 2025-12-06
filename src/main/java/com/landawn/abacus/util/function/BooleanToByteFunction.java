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
 * {@code byte}-valued result. This is the {@code boolean}-to-{@code byte} primitive specialization
 * for {@link java.util.function.Function}.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsByte(boolean)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface BooleanToByteFunction {
    /**
     * A default function that converts a boolean value to a byte: {@code true} to {@code 1} and {@code false} to {@code 0}.
     * This follows the common convention of representing boolean values as bytes in many programming contexts.
     */
    BooleanToByteFunction DEFAULT = value -> value ? (byte) 1 : (byte) 0;

    /**
     * Applies this function to the given argument.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanToByteFunction converter = value -> value ? (byte) 1 : (byte) 0;
     * byte result = converter.applyAsByte(true);   // Returns 1
     * }</pre>
     *
     * @param value the function argument
     * @return the function result as a {@code byte}
     */
    byte applyAsByte(boolean value);
}
