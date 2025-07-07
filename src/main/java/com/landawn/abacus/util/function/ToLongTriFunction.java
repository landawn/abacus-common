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
 * Represents a function that accepts three arguments and produces a long-valued result.
 * This is the long-producing primitive specialization for a three-argument function.
 *
 * <p>This interface extends the Throwables.ToLongTriFunction, providing compatibility
 * with the Abacus framework's error handling mechanisms while limiting thrown exceptions
 * to RuntimeException.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsLong(Object, Object, Object)}.
 *
 * @param <A> the type of the first argument to the function
 * @param <B> the type of the second argument to the function
 * @param <C> the type of the third argument to the function
 */
@FunctionalInterface
public interface ToLongTriFunction<A, B, C> extends Throwables.ToLongTriFunction<A, B, C, RuntimeException> { //NOSONAR

    /**
     * Applies this function to the given arguments and returns a long result.
     * 
     * <p>Example usage:
     * <pre>{@code
     * ToLongTriFunction<Integer, Integer, Integer> multiplier = 
     *     (a, b, c) -> (long) a * b * c;
     * long product = multiplier.applyAsLong(1000, 1000, 100); // returns 100000000L
     * 
     * ToLongTriFunction<Date, Date, TimeUnit> timeDifference = 
     *     (start, end, unit) -> unit.convert(end.getTime() - start.getTime(), TimeUnit.MILLISECONDS);
     * long days = timeDifference.applyAsLong(startDate, endDate, TimeUnit.DAYS); // returns days between dates
     * 
     * ToLongTriFunction<String, Integer, Integer> hashCombiner = 
     *     (str, seed1, seed2) -> str.hashCode() + seed1 * 31L + seed2;
     * long hash = hashCombiner.applyAsLong("test", 7, 13); // returns combined hash value
     * }</pre>
     *
     * @param a the first function argument
     * @param b the second function argument
     * @param c the third function argument
     * @return the function result as a primitive long
     * @throws RuntimeException if any error occurs during function execution
     */
    @Override
    long applyAsLong(A a, B b, C c);
}