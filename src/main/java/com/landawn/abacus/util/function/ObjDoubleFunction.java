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
 * A functional interface that represents a function that accepts an object-valued argument
 * and a double-valued argument, and produces a result. This is a specialization of BiFunction
 * for the case where the second argument is a primitive double.
 *
 * <p>This is a functional interface whose functional method is {@link #apply(Object, double)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <T> the type of the object argument to the function
 * @param <R> the type of the result of the function
 * @see java.util.function.BiFunction
 */
@FunctionalInterface
public interface ObjDoubleFunction<T, R> extends Throwables.ObjDoubleFunction<T, R, RuntimeException> { // NOSONAR
    /**
     * Applies this function to the given arguments.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjDoubleFunction<String, String> formatter = (str, value) ->
     *     String.format("%s: %.2f", str, value);
     * String result = formatter.apply("Price", 19.99);   // Returns "Price: 19.99"
     * }</pre>
     *
     * @param t the first function argument of type T
     * @param u the second function argument, a primitive double value
     * @return the function result of type R
     */
    @Override
    R apply(T t, double u);

    /**
     * Returns a composed function that first applies this function to its input,
     * and then applies the {@code after} function to the result. If evaluation of
     * either function throws an exception, it is relayed to the caller of the
     * composed function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjDoubleFunction<String, Double> priceCalculator = (name, discount) ->
     *     100.0 * (1 - discount);   // base price $100
     * Function<Double, String> formatter = price ->
     *     String.format("$%.2f", price);
     *
     * ObjDoubleFunction<String, String> discountedPriceFormatter =
     *     priceCalculator.andThen(formatter);
     *
     * String result = discountedPriceFormatter.apply("Product", 0.2);   // Returns "$80.00"
     * }</pre>
     *
     * @param <V> the type of output of the {@code after} function, and of the
     *           composed function
     * @param after the function to apply after this function is applied.
     * @return a composed function that first applies this function and then
     *         applies the {@code after} function
     * @throws IllegalArgumentException if {@code after} is {@code null}.
     */
    default <V> ObjDoubleFunction<T, V> andThen(final java.util.function.Function<? super R, ? extends V> after) throws IllegalArgumentException {
        N.checkArgNotNull(after, cs.after);

        return (t, u) -> after.apply(apply(t, u));
    }

    /**
     * Returns this object as a {@link Throwables.ObjDoubleFunction} view.
     *
     * <p>The returned object has the same behavior as this one. This method does not translate
     * exceptions or make the original implementation capable of throwing new checked exceptions; the
     * exception type parameter is for target-type compatibility with APIs that accept {@code Throwables.ObjDoubleFunction}.
     *
     * @param <E> the target exception type for compatibility with {@code Throwables.ObjDoubleFunction}
     * @return a {@link Throwables.ObjDoubleFunction} view of this object
     */
    default <E extends Throwable> Throwables.ObjDoubleFunction<T, R, E> toThrowable() {
        return (Throwables.ObjDoubleFunction<T, R, E>) this;
    }
}
