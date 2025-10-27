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

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.util.Throwables;

/**
 * Represents a function that accepts a long-valued argument and an object argument, 
 * and produces a result. This is a two-arity specialization of {@code Function}.
 * 
 * <p>This is a functional interface whose functional method is {@link #apply(long, Object)}.
 * 
 * <p>The interface extends {@code Throwables.LongObjFunction} with {@code RuntimeException} as the exception type,
 * making it suitable for use in contexts where checked exceptions are not required.
 * 
 * @param <T> the type of the object argument to the function
 * @param <R> the type of the result of the function
 * 
 * @see java.util.function.Function
 * @see java.util.function.BiFunction
 */
@FunctionalInterface
public interface LongObjFunction<T, R> extends Throwables.LongObjFunction<T, R, RuntimeException> { // NOSONAR
    /**
     * Applies this function to the given arguments.
     * 
     * <p>This method takes a primitive long value as the first argument and an object of type T
     * as the second argument, then processes them to produce a result of type R.
     *
     * @param t the long-valued first argument
     * @param u the object second argument of type T
     * @return the function result of type R if any error occurs during function execution
     */
    @Override
    @MayReturnNull
    R apply(long t, T u);

    /**
     * Returns a composed function that first applies this function to its input,
     * and then applies the {@code after} function to the result.
     * 
     * <p>If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongObjFunction<String, Integer> lengthPlusLong = (l, s) -> s.length() + (int)l;
     * Function<Integer, String> intToString = Object::toString;
     * LongObjFunction<String, String> composed = lengthPlusLong.andThen(intToString);
     * // composed.apply(5L, "hello") returns "10" (5 + 5 = 10)
     * }</pre>
     *
     * @param <V> the type of output of the {@code after} function, and of the
     *           composed function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then
     *         applies the {@code after} function
     */
    default <V> LongObjFunction<T, V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return (i, t) -> after.apply(apply(i, t));
    }
}
