/*
 * Copyright 2017 Haiyang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.util.function;

import com.landawn.abacus.util.Throwables;

/**
 * A functional interface that represents a function that accepts four arguments and
 * produces a result. This is the four-arity specialization of {@link java.util.function.Function}.
 *
 * <p>This interface extends the standard Java functional interfaces to support functions
 * with four parameters, which is useful for more complex transformations that require
 * multiple inputs to compute a result.
 *
 * <p>This is a functional interface whose functional method is {@link #apply(Object, Object, Object, Object)}.
 *
 * @param <A> the type of the first argument to the function
 * @param <B> the type of the second argument to the function
 * @param <C> the type of the third argument to the function
 * @param <D> the type of the fourth argument to the function
 * @param <R> the type of the result of the function
 * @see java.util.function.Function
 * @see java.util.function.BiFunction
 * @see TriFunction
 */
@FunctionalInterface
public interface QuadFunction<A, B, C, D, R> extends Throwables.QuadFunction<A, B, C, D, R, RuntimeException> { //NOSONAR

    /**
     * Applies this function to the given arguments.
     *
     * <p>This method takes four arguments of types A, B, C, and D as input and
     * produces a result of type R. The function should be deterministic, meaning
     * that for the same inputs, it should always produce the same output.
     *
     * <p>Example usage:
     * <pre>{@code
     * QuadFunction<Integer, Integer, Integer, Integer, Integer> sum4 = 
     *     (a, b, c, d) -> a + b + c + d;
     * 
     * QuadFunction<String, String, String, String, Address> createAddress = 
     *     (street, city, state, zip) -> new Address(street, city, state, zip);
     * 
     * QuadFunction<Double, Double, Double, Double, Double> calculateAverage = 
     *     (a, b, c, d) -> (a + b + c + d) / 4.0;
     * 
     * Integer total = sum4.apply(10, 20, 30, 40); // Returns 100
     * Address addr = createAddress.apply("123 Main St", "Springfield", "IL", "62701");
     * }</pre>
     *
     * @param a the first function argument
     * @param b the second function argument
     * @param c the third function argument
     * @param d the fourth function argument
     * @return the function result
     * @throws RuntimeException if the function cannot compute a result
     */
    @Override
    R apply(A a, B b, C c, D d);

    /**
     * Returns a composed function that first applies this function to its input,
     * and then applies the {@code after} function to the result. If evaluation of
     * either function throws an exception, it is relayed to the caller of the
     * composed function.
     *
     * <p>This method enables function composition, allowing you to chain multiple
     * transformations together. The output of this function becomes the input to
     * the {@code after} function.
     *
     * <p>Example usage:
     * <pre>{@code
     * QuadFunction<Integer, Integer, Integer, Integer, Integer> sum4 = 
     *     (a, b, c, d) -> a + b + c + d;
     * Function<Integer, String> intToString = 
     *     num -> "Total: " + num;
     * 
     * QuadFunction<Integer, Integer, Integer, Integer, String> sumAndFormat = 
     *     sum4.andThen(intToString);
     * 
     * String result = sumAndFormat.apply(10, 20, 30, 40); // Returns "Total: 100"
     * 
     * // More complex example
     * QuadFunction<User, Product, Integer, Double, Order> createOrder = 
     *     (user, product, quantity, discount) -> 
     *         new Order(user, product, quantity, discount);
     * Function<Order, OrderConfirmation> processOrder = 
     *     order -> orderService.process(order);
     * 
     * QuadFunction<User, Product, Integer, Double, OrderConfirmation> placeOrder = 
     *     createOrder.andThen(processOrder);
     * }</pre>
     *
     * @param <V> the type of output of the {@code after} function, and of the
     *           composed function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then
     *         applies the {@code after} function
     */
    default <V> QuadFunction<A, B, C, D, V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return (a, b, c, d) -> after.apply(apply(a, b, c, d));
    }
}