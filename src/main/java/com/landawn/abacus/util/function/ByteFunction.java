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
 * Represents a function that accepts a byte-valued argument and produces a result.
 * This is the byte-consuming primitive specialization for {@link java.util.function.Function}.
 *
 * <p>This is a functional interface whose functional method is {@link #apply(byte)}.
 *
 * @param <R> the type of the result of the function
 *
 * @see java.util.function.Function
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface ByteFunction<R> extends Throwables.ByteFunction<R, RuntimeException> { //NOSONAR
    /**
     * A predefined function that boxes a primitive {@code byte} value into a {@link Byte} object.
     * This is equivalent to {@code Byte::valueOf} and provides a convenient way to convert
     * primitive bytes to their wrapper type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteFunction<Byte> boxer = ByteFunction.BOX;
     * Byte boxed = boxer.apply((byte) 42); // Returns Byte.valueOf((byte) 42)
     * }</pre>
     */
    ByteFunction<Byte> BOX = value -> value;

    /**
     * Applies this function to the given byte-valued argument and produces a result.
     *
     * <p>This method transforms a {@code byte} value into a result of type {@code R}.
     * Common use cases include:
     * <ul>
     *   <li>Converting byte values to other numeric types or strings</li>
     *   <li>Creating objects from byte values (e.g., status codes to enum values)</li>
     *   <li>Interpreting byte data in binary protocols or file formats</li>
     *   <li>Mapping byte values to lookup tables or configuration objects</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteFunction<String> toHex = b -> String.format("%02X", b);
     * String hex = toHex.apply((byte) 255); // Returns "FF"
     *
     * ByteFunction<Integer> toUnsigned = b -> Byte.toUnsignedInt(b);
     * Integer unsigned = toUnsigned.apply((byte) -1); // Returns 255
     * }</pre>
     *
     * @param value the byte input argument
     * @return the function result of type R
     */
    @Override
    R apply(byte value);

    /**
     * Returns a composed function that first applies this function to its input,
     * and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to the caller
     * of the composed function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteFunction<Integer> toInt = b -> (int) b;
     * Function<Integer, String> toString = Object::toString;
     * ByteFunction<String> combined = toInt.andThen(toString);
     * String result = combined.apply((byte) 42); // Returns "42"
     * }</pre>
     *
     * @param <V> the type of output of the {@code after} function, and of the composed function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then applies the
     *         {@code after} function
     */
    default <V> ByteFunction<V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return t -> after.apply(apply(t));
    }

    /**
     * Returns a function that always returns its input argument unchanged as a {@link Byte} object.
     *
     * <p>This is the identity function for byte values, performing boxing from primitive {@code byte}
     * to {@link Byte}. It is useful when a {@code ByteFunction<Byte>} is required but no
     * transformation is needed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteFunction<Byte> identity = ByteFunction.identity();
     * Byte result = identity.apply((byte) 42); // Returns 42 (boxed)
     * }</pre>
     *
     * @return a function that always returns its input argument as a Byte
     */
    static ByteFunction<Byte> identity() {
        return t -> t;
    }

    /**
     * Converts this {@code ByteFunction} to a {@code Throwables.ByteFunction} that can throw a checked exception.
     * This method provides a way to use this function in contexts that require explicit exception handling.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteFunction function = (...) -> { ... };
     * var throwableFunction = function.toThrowable();
     * // Can now be used in contexts that handle checked exceptions
     * }</pre>
     *
     * @param <E> the type of exception that the returned function can throw
     * @return a {@code Throwables.ByteFunction} view of this function that can throw exceptions of type {@code E}
     */
    default <E extends Throwable> Throwables.ByteFunction<R, E> toThrowable() {
        return (Throwables.ByteFunction<R, E>) this;
    }

}
