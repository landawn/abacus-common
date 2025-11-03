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
 * Represents a function that accepts one {@code int}-valued argument and two object-valued arguments
 * and produces a result. This is a three-arity specialization of {@link java.util.function.Function}.
 *
 * <p>This is a functional interface whose functional method is {@link #apply(int, Object, Object)}.
 *
 * @param <T> the type of the first object argument to the function
 * @param <U> the type of the second object argument to the function
 * @param <R> the type of the result of the function
 * @see java.util.function.Function
 * @see IntBiFunction
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface IntBiObjFunction<T, U, R> extends Throwables.IntBiObjFunction<T, U, R, RuntimeException> { //NOSONAR
    /**
     * Applies this function to the given arguments.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntBiObjFunction<String, String, String> formatter =
     *     (index, prefix, suffix) -> prefix + index + suffix;
     * String result = formatter.apply(5, "Item_", "_end"); // Returns "Item_5_end"
     *
     * IntBiObjFunction<List<String>, String, Boolean> listInserter =
     *     (index, list, element) -> { list.add(index, element); return true; };
     * List<String> myList = new ArrayList<>(Arrays.asList("a", "b", "c"));
     * listInserter.apply(1, myList, "x"); // list becomes ["a", "x", "b", "c"]
     * }</pre>
     *
     * @param i the {@code int} argument
     * @param t the first object argument
     * @param u the second object argument
     * @return the function result
     */
    @Override
    R apply(int i, T t, U u);

    /**
     * Returns a composed function that first applies this function to its input, and then applies
     * the {@code after} function to the result. If evaluation of either function throws an exception,
     * it is relayed to the caller of the composed function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntBiObjFunction<String, String, Integer> summer =
     *     (i, s1, s2) -> i + s1.length() + s2.length();
     * Function<Integer, String> toString = Object::toString;
     * IntBiObjFunction<String, String, String> combined = summer.andThen(toString);
     * String result = combined.apply(5, "ab", "xyz"); // Returns "10"
     * }</pre>
     *
     * @param <V> the type of output of the {@code after} function, and of the composed function
     * @param after the function to apply after this function is applied. Must not be {@code null}.
     * @return a composed {@code IntBiObjFunction} that first applies this function and then applies the {@code after}
     *         function
     */
    default <V> IntBiObjFunction<T, U, V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return (i, t, u) -> after.apply(apply(i, t, u));
    }
}
