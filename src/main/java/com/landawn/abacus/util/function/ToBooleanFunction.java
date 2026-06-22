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
 * Represents a function that produces a boolean-valued result.
 * This is the {@code boolean}-producing primitive specialization for {@link java.util.function.Function}.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsBoolean(Object)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <T> the type of the input to the function
 *
 * @see java.util.function.Function
 * @see java.util.function.Predicate
 */
@FunctionalInterface
public interface ToBooleanFunction<T> extends Throwables.ToBooleanFunction<T, RuntimeException> { //NOSONAR
    /**
     * A function that safely unboxes a Boolean object to a primitive boolean value.
     * Returns {@code false} for {@code null} inputs and the boolean value for {@code non-null} inputs.
     * This provides null-safe unboxing behavior.
     */
    ToBooleanFunction<Boolean> UNBOX = value -> value != null && value;

    /**
     * Applies this function to the given argument.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ToBooleanFunction<Boolean> unbox = ToBooleanFunction.UNBOX;
     * boolean result1 = unbox.applyAsBoolean(Boolean.TRUE);   // returns true
     * boolean result2 = unbox.applyAsBoolean(null);           // returns false
     *
     * ToBooleanFunction<String> isEmpty = String::isEmpty;
     * boolean result3 = isEmpty.applyAsBoolean("");        // returns true
     * boolean result4 = isEmpty.applyAsBoolean("hello");   // returns false
     *
     * ToBooleanFunction<Integer> isEven = n -> n % 2 == 0;
     * boolean result5 = isEven.applyAsBoolean(4);   // returns true
     * }</pre>
     *
     * @param value the function argument
     * @return the function result as a boolean value
     */
    @Override
    boolean applyAsBoolean(T value);

    /**
     * Returns this object as a {@link Throwables.ToBooleanFunction} view.
     *
     * <p>The returned object has the same behavior as this one. This method does not translate
     * exceptions or make the original implementation capable of throwing new checked exceptions; the
     * exception type parameter is for target-type compatibility with APIs that accept {@code Throwables.ToBooleanFunction}.
     *
     * @param <E> the target exception type for compatibility with {@code Throwables.ToBooleanFunction}
     * @return a {@link Throwables.ToBooleanFunction} view of this object
     */
    default <E extends Throwable> Throwables.ToBooleanFunction<T, E> toThrowable() {
        return (Throwables.ToBooleanFunction<T, E>) this;
    }
}
