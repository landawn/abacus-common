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
 * Represents a function that accepts a byte-valued argument and produces an int-valued result.
 * This is the {@code byte}-to-{@code int} primitive specialization for {@link java.util.function.Function}.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsInt(byte)}.
 *
 * @see java.util.function.Function
 * @see java.util.function.ToIntFunction
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface ByteToIntFunction {
    /**
     * A default implementation that performs a widening primitive conversion from byte to int.
     * This is equivalent to a simple cast operation that preserves the byte's value,
     * converting it from the range [-128, 127] to the same value as an int.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteToIntFunction defaultConversion = ByteToIntFunction.DEFAULT;
     * int result = defaultConversion.applyAsInt((byte) 42);   // Returns 42
     * }</pre>
     */
    ByteToIntFunction DEFAULT = value -> value;

    /**
     * Applies this function to the given byte argument and returns an int result.
     * This method performs a transformation from a byte value to an int value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteToIntFunction unsignedConverter = b -> Byte.toUnsignedInt(b);
     * int unsigned = unsignedConverter.applyAsInt((byte) -1);   // Returns 255
     *
     * ByteToIntFunction multiplier = b -> b * 10;
     * int result = multiplier.applyAsInt((byte) 5);   // Returns 50
     * }</pre>
     *
     * @param value the byte function argument
     * @return the int function result
     */
    int applyAsInt(byte value);
}
