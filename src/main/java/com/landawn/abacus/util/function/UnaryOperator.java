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

import com.landawn.abacus.util.Fn.UnaryOperators;
import com.landawn.abacus.util.Throwables;

/**
 * Represents an operation on a single operand that produces a result of the same type as its operand.
 * This is a specialization of {@link Function} for the case where the operand and result are of the same type.
 *
 * <p>This interface extends Function, Throwables.UnaryOperator and the standard Java UnaryOperator,
 * providing compatibility with the Abacus framework's error handling mechanisms and the standard
 * Java functional interfaces.
 *
 * <p>This is a functional interface whose functional method is {@link #apply(Object)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a>
 *
 * @param <T> the type of the operand and result of the operator
 */
@FunctionalInterface
public interface UnaryOperator<T> extends Function<T, T>, Throwables.UnaryOperator<T, RuntimeException>, java.util.function.UnaryOperator<T> { //NOSONAR

    /**
     * Returns a composed operator that first applies the before operator to its input,
     * and then applies this operator to the result. If evaluation of either operator
     * throws an exception, it is relayed to the caller of the composed operator.
     * 
     * <p>Example usage:
     * <pre>{@code
     * UnaryOperator<String> trim = String::trim;
     * UnaryOperator<String> upperCase = String::toUpperCase;
     * 
     * UnaryOperator<String> trimThenUpper = upperCase.compose(trim);
     * String result = trimThenUpper.apply("  hello  "); // returns "HELLO"
     * 
     * UnaryOperator<Integer> doubleIt = x -> x * 2;
     * UnaryOperator<Integer> addTen = x -> x + 10;
     * 
     * UnaryOperator<Integer> addTenThenDouble = doubleIt.compose(addTen);
     * Integer result2 = addTenThenDouble.apply(5); // returns 30 ((5 + 10) * 2)
     * }</pre>
     *
     * @param before the operator to apply before this operator is applied
     * @return a composed operator that first applies the before operator and then applies this operator
     * @see #andThen(java.util.function.UnaryOperator)
     */
    default UnaryOperator<T> compose(final java.util.function.UnaryOperator<T> before) {
        return t -> apply(before.apply(t));
    }

    /**
     * Returns a composed operator that first applies this operator to its input, and then
     * applies the after operator to the result. If evaluation of either operator throws an
     * exception, it is relayed to the caller of the composed operator.
     * 
     * <p>Example usage:
     * <pre>{@code
     * UnaryOperator<String> trim = String::trim;
     * UnaryOperator<String> upperCase = String::toUpperCase;
     * 
     * UnaryOperator<String> trimAndUpper = trim.andThen(upperCase);
     * String result = trimAndUpper.apply("  hello  "); // returns "HELLO"
     * 
     * UnaryOperator<Integer> doubleIt = x -> x * 2;
     * UnaryOperator<Integer> addTen = x -> x + 10;
     * 
     * UnaryOperator<Integer> doubleThenAddTen = doubleIt.andThen(addTen);
     * Integer result2 = doubleThenAddTen.apply(5); // returns 20 ((5 * 2) + 10)
     * }</pre>
     *
     * @param after the operator to apply after this operator is applied
     * @return a composed operator that first applies this operator and then applies the after operator
     * @see #compose(java.util.function.UnaryOperator)
     */
    default UnaryOperator<T> andThen(final java.util.function.UnaryOperator<T> after) {
        return t -> after.apply(apply(t));
    }

    /**
     * Returns a unary operator that always returns its input argument unchanged.
     * This is useful as a default or no-op operator in functional compositions.
     * 
     * <p>Example usage:
     * <pre>{@code
     * UnaryOperator<String> identity = UnaryOperator.identity();
     * String result = identity.apply("hello"); // returns "hello"
     * 
     * List<UnaryOperator<Integer>> operations = Arrays.asList(
     *     x -> x * 2,
     *     UnaryOperator.identity(), // no-op
     *     x -> x + 10
     * );
     * 
     * // Can be used in streams
     * Stream.of("a", "b", "c")
     *     .map(UnaryOperator.<String>identity())
     *     .collect(Collectors.toList()); // returns ["a", "b", "c"]
     * }</pre>
     *
     * @param <T> the type of the input and output of the operator
     * @return a unary operator that always returns its input argument
     */
    static <T> UnaryOperator<T> identity() {
        return UnaryOperators.identity();
    }

    /**
     * Converts this UnaryOperator to a Throwables.UnaryOperator that can throw checked exceptions.
     * This method is useful when you need to use this operator in a context that expects
     * a Throwables.UnaryOperator with a specific exception type.
     * 
     * <p>Example usage:
     * <pre>{@code
     * UnaryOperator<String> operator = s -> s.toUpperCase();
     * Throwables.UnaryOperator<String, IOException> throwableOperator = 
     *     operator.toThrowable();
     * }</pre>
     *
     * @param <E> the type of exception that the returned operator may throw
     * @return a Throwables.UnaryOperator that wraps this operator
     */
    @Override
    default <E extends Throwable> Throwables.UnaryOperator<T, E> toThrowable() {
        return (Throwables.UnaryOperator<T, E>) this;
    }
}