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
 * Represents an operation that accepts a variable number of {@code boolean}-valued arguments and returns no result.
 * This is the N-arity specialization of {@link BooleanConsumer}.
 * Unlike most other functional interfaces, {@code BooleanNConsumer} is expected to operate via side-effects.
 *
 * <p>This is a functional interface whose functional method is {@link #accept(boolean...)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface BooleanNConsumer {
    /**
     * Performs this operation on the given arguments.
     * The behavior of this operation is generally expected to be side-effecting.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanNConsumer logger = values -> {
     *     for (boolean v : values) System.out.print(v + " ");
     *     System.out.println();
     * };
     * logger.accept(true, false, true); // Prints: true false true
     * }</pre>
     *
     * @param args the input arguments as a variable-length array of {@code boolean} values.
     *             May be empty but must not be {@code null}.
     */
    void accept(boolean... args);

    /**
     * Returns a composed {@code BooleanNConsumer} that performs, in sequence, this operation followed by the {@code after} operation.
     * If performing either operation throws an exception, it is relayed to the caller of the composed operation.
     * If performing this operation throws an exception, the {@code after} operation will not be performed.
     *
     * @param after the operation to perform after this operation. Must not be {@code null}.
     * @return a composed {@code BooleanNConsumer} that performs in sequence this operation followed by the {@code after} operation
     */
    default BooleanNConsumer andThen(final BooleanNConsumer after) {
        return args -> {
            accept(args);
            after.accept(args);
        };
    }

    /**
     * Converts this {@code BooleanNConsumer} to a {@code Throwables.BooleanNConsumer} that can throw a checked exception.
     * This method provides a way to use this consumer in contexts that require explicit exception handling.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanNConsumer consumer = (...) -> { ... };
     * var throwableConsumer = consumer.toThrowable();
     * // Can now be used in contexts that handle checked exceptions
     * }</pre>
     *
     * @param <E> the type of exception that the returned consumer can throw
     * @return a {@code Throwables.BooleanNConsumer} view of this consumer that can throw exceptions of type {@code E}
     */
    default <E extends Throwable> Throwables.BooleanNConsumer<E> toThrowable() {
        return (Throwables.BooleanNConsumer<E>) this;
    }

}
