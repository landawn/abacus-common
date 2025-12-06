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

import com.landawn.abacus.util.Throwables;

/**
 * Represents a function that accepts three arguments and produces an int-valued result.
 * This is the int-producing primitive specialization for a three-argument function.
 *
 * <p>This interface extends the Throwables.ToIntTriFunction, providing compatibility
 * with the abacus-common framework's error handling mechanisms while limiting thrown exceptions
 * to RuntimeException.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsInt(Object, Object, Object)}.
 *
 * @param <A> the type of the first argument to the function
 * @param <B> the type of the second argument to the function
 * @param <C> the type of the third argument to the function
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface ToIntTriFunction<A, B, C> extends Throwables.ToIntTriFunction<A, B, C, RuntimeException> { //NOSONAR
    /**
     * Applies this function to the given arguments and returns an int result.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ToIntTriFunction<String, String, String> totalLength =
     *     (s1, s2, s3) -> s1.length() + s2.length() + s3.length();
     * int total = totalLength.applyAsInt("Hello", "World", "!");   // returns 11
     *
     * ToIntTriFunction<Integer, Integer, Integer> sum =
     *     (a, b, c) -> a + b + c;
     * int result = sum.applyAsInt(10, 20, 30);   // returns 60
     *
     * ToIntTriFunction<List<String>, Integer, Integer> subListSize =
     *     (list, from, to) -> list.subList(from, to).size();
     * int size = subListSize.applyAsInt(Arrays.asList("a", "b", "c", "d"), 1, 3);   // returns 2
     * }</pre>
     *
     * @param a the first function argument
     * @param b the second function argument
     * @param c the third function argument
     * @return the function result as a primitive int
     */
    @Override
    int applyAsInt(A a, B b, C c);
}
