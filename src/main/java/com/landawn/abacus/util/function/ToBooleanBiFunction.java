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

/**
 * Represents a function that accepts two arguments and produces a boolean-valued result.
 * This is the {@code boolean}-producing primitive specialization for {@link java.util.function.BiFunction}.
 * 
 * <p>This is a functional interface whose functional method is {@link #applyAsBoolean(Object, Object)}.
 * 
 * @param <T> the type of the first argument to the function
 * @param <U> the type of the second argument to the function
 * 
 * @see java.util.function.BiFunction
 * @see ToBooleanFunction
 */
@FunctionalInterface
public interface ToBooleanBiFunction<T, U> {

    /**
     * Applies this function to the given arguments.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ToBooleanBiFunction<String, String> equals = String::equals;
     * boolean result1 = equals.applyAsBoolean("hello", "hello"); // returns true
     * boolean result2 = equals.applyAsBoolean("hello", "world"); // returns false
     *
     * ToBooleanBiFunction<Integer, Integer> greaterThan = (a, b) -> a > b;
     * boolean result3 = greaterThan.applyAsBoolean(10, 5); // returns true
     *
     * ToBooleanBiFunction<List, Object> contains = List::contains;
     * boolean result4 = contains.applyAsBoolean(Arrays.asList(1, 2, 3), 2); // returns true
     * }</pre>
     *
     * @param t the first function argument
     * @param u the second function argument
     * @return the function result as a boolean value
     */
    boolean applyAsBoolean(T t, U u);
}