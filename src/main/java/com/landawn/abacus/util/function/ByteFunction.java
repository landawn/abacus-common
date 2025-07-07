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
 */
@FunctionalInterface
public interface ByteFunction<R> extends Throwables.ByteFunction<R, RuntimeException> { //NOSONAR
    /**
     * A function that boxes a primitive byte value into a Byte object.
     * This is useful when you need to convert primitive bytes to their wrapper type.
     */
    ByteFunction<Byte> BOX = value -> value;

    /**
     * Applies this function to the given byte argument.
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
     * @param <V> the type of output of the {@code after} function, and of the composed function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then applies the 
     *         {@code after} function
     */
    default <V> ByteFunction<V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return t -> after.apply(apply(t));
    }

    /**
     * Returns a function that always returns its byte input argument as a Byte object.
     * This is an identity function for byte values that performs boxing.
     *
     * @return a function that always returns its input argument as a Byte
     */
    static ByteFunction<Byte> identity() {
        return t -> t;
    }
}