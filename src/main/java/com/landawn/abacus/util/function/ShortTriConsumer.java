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
 * Represents an operation that accepts three short-valued arguments and returns no result.
 * This is the three-arity specialization of {@link ShortConsumer}.
 * Unlike most other functional interfaces, {@code ShortTriConsumer} is expected to operate via side-effects.
 *
 * <p>This is a functional interface whose functional method is {@link #accept(short, short, short)}.
 *
 * @see ShortConsumer
 * @see ShortBiConsumer
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface ShortTriConsumer extends Throwables.ShortTriConsumer<RuntimeException> { //NOSONAR
    /**
     * Performs this operation on the given arguments.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortTriConsumer sumPrinter = (a, b, c) ->
     *     System.out.println("Sum: " + (a + b + c));
     * sumPrinter.accept((short) 10, (short) 20, (short) 30); // Prints "Sum: 60"
     *
     * ShortTriConsumer rangeSetter = (min, max, value) -> {
     *     if (value < min || value > max) {
     *         throw new IllegalArgumentException("Value out of range");
     *     }
     *     config.setValue(value);
     * };
     * rangeSetter.accept((short) 0, (short) 100, (short) 50); // Sets value to 50
     * }</pre>
     *
     * @param a the first input argument
     * @param b the second input argument
     * @param c the third input argument
     */
    @Override
    void accept(short a, short b, short c);

    /**
     * Returns a composed {@code ShortTriConsumer} that performs, in sequence, this operation followed by the {@code after} operation.
     * If performing either operation throws an exception, it is relayed to the caller of the composed operation.
     * If performing this operation throws an exception, the {@code after} operation will not be performed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortTriConsumer logger = (a, b, c) -> System.out.println("Values: " + a + ", " + b + ", " + c);
     * ShortTriConsumer sum = (a, b, c) -> System.out.println("Sum: " + (a + b + c));
     * ShortTriConsumer combined = logger.andThen(sum);
     * combined.accept((short) 10, (short) 20, (short) 30); // Logs then calculates sum
     * }</pre>
     *
     * @param after the operation to perform after this operation. Must not be {@code null}.
     * @return a composed {@code ShortTriConsumer} that performs in sequence this operation followed by the {@code after} operation
     */
    default ShortTriConsumer andThen(final ShortTriConsumer after) {
        return (a, b, c) -> {
            accept(a, b, c);
            after.accept(a, b, c);
        };
    }

    /**
     * Converts this {@code ShortTriConsumer} to a {@code Throwables.ShortTriConsumer} that can throw a checked exception.
     * This method provides a way to use this consumer in contexts that require explicit exception handling.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortTriConsumer consumer = (...) -> { ... };
     * var throwableConsumer = consumer.toThrowable();
     * // Can now be used in contexts that handle checked exceptions
     * }</pre>
     *
     * @param <E> the type of exception that the returned consumer can throw
     * @return a {@code Throwables.ShortTriConsumer} view of this consumer that can throw exceptions of type {@code E}
     */
    default <E extends Throwable> Throwables.ShortTriConsumer<E> toThrowable() {
        return (Throwables.ShortTriConsumer<E>) this;
    }

}
