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

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.cs;

/**
 * Represents a function that accepts two {@code short}-valued arguments and produces a result.
 * This is the primitive type specialization of {@link java.util.function.BiFunction} for {@code short}.
 *
 * <p>This is a functional interface whose functional method is {@link #apply(short, short)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <R> the type of the result of the function
 *
 * @see java.util.function.BiFunction
 * @see ShortFunction
 * @see ShortTriFunction
 */
@FunctionalInterface
public interface ShortBiFunction<R> extends Throwables.ShortBiFunction<R, RuntimeException> { //NOSONAR
    /**
     * Applies this function to the given arguments.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortBiFunction<String> formatter = (a, b) -> String.format("%d+%d=%d", a, b, a + b);
     * String result = formatter.apply((short) 5, (short) 3);   // Returns "5+3=8"
     * }</pre>
     *
     * @param a the first function argument
     * @param b the second function argument
     * @return the function result
     */
    @Override
    R apply(short a, short b);

    /**
     * Returns a composed function that first applies this function to its input, and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to the caller of the composed function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortBiFunction<Integer> sum = (a, b) -> a + b;
     * Function<Integer, String> toString = Object::toString;
     * ShortBiFunction<String> combined = sum.andThen(toString);
     * String result = combined.apply((short) 5, (short) 3);   // Returns "8"
     * }</pre>
     *
     * @param <V> the type of output of the {@code after} function, and of the composed function
     * @param after the function to apply after this function is applied.
     * @return a composed function that first applies this function and then applies the {@code after} function
     * @throws IllegalArgumentException if {@code after} is {@code null}.
     */
    default <V> ShortBiFunction<V> andThen(final java.util.function.Function<? super R, ? extends V> after) throws IllegalArgumentException {
        N.checkArgNotNull(after, cs.after);

        return (a, b) -> after.apply(apply(a, b));
    }

    /**
     * Returns this object as a {@link Throwables.ShortBiFunction} view.
     *
     * <p>The returned object has the same behavior as this one. This method does not translate
     * exceptions or make the original implementation capable of throwing new checked exceptions; the
     * exception type parameter is for target-type compatibility with APIs that accept {@code Throwables.ShortBiFunction}.
     *
     * @param <E> the target exception type for compatibility with {@code Throwables.ShortBiFunction}
     * @return a {@link Throwables.ShortBiFunction} view of this object
     */
    default <E extends Throwable> Throwables.ShortBiFunction<R, E> toThrowable() {
        return (Throwables.ShortBiFunction<R, E>) this;
    }
}
