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
 * Represents a function that accepts two arguments and produces an int-valued result.
 * This is the int-producing primitive specialization for {@link java.util.function.BiFunction}.
 *
 * <p>This interface extends both the Throwables.ToIntBiFunction and the standard Java
 * ToIntBiFunction, providing compatibility with both the abacus-common framework's error handling
 * mechanisms and the standard Java functional interfaces.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsInt(Object, Object)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <T> the type of the first argument to the function
 * @param <U> the type of the second argument to the function
 */
@FunctionalInterface
public interface ToIntBiFunction<T, U> extends Throwables.ToIntBiFunction<T, U, RuntimeException>, java.util.function.ToIntBiFunction<T, U> { //NOSONAR
    /**
     * Applies this function to the given arguments and returns an int result.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ToIntBiFunction<String, String> combinedLength = (s1, s2) -> s1.length() + s2.length();
     * int total = combinedLength.applyAsInt("Hello", "World"); // returns 10
     *
     * ToIntBiFunction<Integer, Integer> multiplier = (a, b) -> a * b;
     * int product = multiplier.applyAsInt(5, 6); // returns 30
     * }</pre>
     *
     * @param t the first function argument
     * @param u the second function argument
     * @return the function result as a primitive int
     */
    @Override
    int applyAsInt(T t, U u);
}
