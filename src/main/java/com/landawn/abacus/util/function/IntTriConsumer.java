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
 * Represents an operation that accepts three {@code int}-valued arguments and
 * returns no result. This is the three-arity specialization of
 * {@link java.util.function.Consumer}. Unlike most other functional interfaces,
 * {@code IntTriConsumer} is expected to operate via side-effects.
 *
 * <p>This interface extends {@link Throwables.IntTriConsumer} with
 * {@link RuntimeException}, providing compatibility with the abacus-common framework's
 * exception handling capabilities.
 *
 * <p>This is a functional interface whose functional method is
 * {@link #accept(int, int, int)}.
 *
 * @see java.util.function.Consumer
 * @see java.util.function.BiConsumer
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface IntTriConsumer extends Throwables.IntTriConsumer<RuntimeException> { //NOSONAR
    /**
     * Performs this operation on the given arguments.
     * This method is expected to operate via side-effects.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntTriConsumer coordinate3D = (x, y, z) ->
     *     System.out.println("Coordinate: (" + x + ", " + y + ", " + z + ")");
     * coordinate3D.accept(10, 20, 30);  // Prints: Coordinate: (10, 20, 30)
     *
     * IntTriConsumer rgbPrinter = (r, g, b) ->
     *     System.out.printf("RGB Color: rgb(%d, %d, %d)%n", r, g, b);
     * rgbPrinter.accept(255, 128, 0);
     * }</pre>
     *
     * @param a the first input argument
     * @param b the second input argument
     * @param c the third input argument
     */
    @Override
    void accept(int a, int b, int c);

    /**
     * Returns a composed {@code IntTriConsumer} that performs, in sequence, this
     * operation followed by the {@code after} operation. If performing either
     * operation throws an exception, it is relayed to the caller of the
     * composed operation. If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * <p>This method allows for chaining multiple consumers to create a pipeline
     * of operations that will be executed sequentially on the same three input
     * values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntTriConsumer logger = (a, b, c) -> System.out.println("Values: " + a + ", " + b + ", " + c);
     * IntTriConsumer sum = (a, b, c) -> System.out.println("Sum: " + (a + b + c));
     * IntTriConsumer combined = logger.andThen(sum);
     * combined.accept(5, 10, 15);  // Logs then calculates sum
     * }</pre>
     *
     * @param after the operation to perform after this operation. Must not be {@code null}.
     * @return a composed {@code IntTriConsumer} that performs in sequence this
     *         operation followed by the {@code after} operation
     */
    default IntTriConsumer andThen(final IntTriConsumer after) {
        return (a, b, c) -> {
            accept(a, b, c);
            after.accept(a, b, c);
        };
    }
}
