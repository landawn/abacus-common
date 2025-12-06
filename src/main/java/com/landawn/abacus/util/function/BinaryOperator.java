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

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.Throwables;

/**
 * Represents an operation upon two operands of the same type, producing a result of the same type as the operands.
 * This is a specialization of {@link BiFunction} for the case where the operands and the result are all of the same type.
 *
 * <p>This interface extends both {@link BiFunction}, {@link Throwables.BinaryOperator}, and
 * {@link java.util.function.BinaryOperator}, providing compatibility with the abacus-common framework's
 * error handling mechanisms and the standard Java functional interfaces.
 *
 * <p>This is a functional interface whose functional method is {@link #apply(Object, Object)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <T> the type of the operands and result of the operator
 * @see BiFunction
 * @see java.util.function.BinaryOperator
 */
@SuppressFBWarnings("NM_SAME_SIMPLE_NAME_AS_INTERFACE")
@FunctionalInterface
public interface BinaryOperator<T> extends BiFunction<T, T, T>, Throwables.BinaryOperator<T, RuntimeException>, java.util.function.BinaryOperator<T> { //NOSONAR
    /**
     * Applies this operator to the given operands.
     *
     * <p>This method is inherited from {@link BiFunction} and performs an operation on two operands
     * of type {@code T}, producing a result of the same type {@code T}. Common use cases include:
     * <ul>
     *   <li>Mathematical operations (addition, multiplication, min, max)</li>
     *   <li>String concatenation or merging</li>
     *   <li>Combining or merging objects of the same type</li>
     *   <li>Reduction operations in stream processing</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BinaryOperator<Integer> adder = (a, b) -> a + b;
     * Integer sum = adder.apply(5, 3);   // Returns 8
     *
     * BinaryOperator<String> concatenator = (s1, s2) -> s1 + s2;
     * String combined = concatenator.apply("Hello, ", "World!");   // Returns "Hello, World!"
     * }</pre>
     *
     * @param t the first operand
     * @param u the second operand
     * @return the operator result
     */
    @Override
    T apply(T t, T u);

    /**
     * Converts this {@code BinaryOperator} to a {@code Throwables.BinaryOperator} that can throw a checked exception.
     * This method provides a way to use this operator in contexts that require explicit exception handling.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BinaryOperator<String> concatenator = (s1, s2) -> s1 + s2;
     * var throwableOp = concatenator.toThrowable();
     * }</pre>
     *
     * @param <E> the type of exception that the returned operator can throw
     * @return a {@code Throwables.BinaryOperator} view of this operator that can throw exceptions of type {@code E}
     */
    @Override
    default <E extends Throwable> Throwables.BinaryOperator<T, E> toThrowable() {
        return (Throwables.BinaryOperator<T, E>) this;
    }
}
