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
 * Represents an operation that accepts three float-valued arguments and returns no result.
 * This is a functional interface whose functional method is {@link #accept(float, float, float)}.
 *
 * <p>This is a primitive type specialization of consumer for three {@code float} arguments.
 * Unlike most other functional interfaces, {@code FloatTriConsumer} is expected to operate via side-effects.</p>
 *
 * @see java.util.function.Consumer
 * @see java.util.function.BiConsumer
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface FloatTriConsumer extends Throwables.FloatTriConsumer<RuntimeException> { //NOSONAR
    /**
     * Performs this operation on the given float arguments.
     * This method is expected to operate via side-effects.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatTriConsumer coordinate3D = (x, y, z) ->
     *     System.out.printf("3D Point: (%.2f, %.2f, %.2f)%n", x, y, z);
     * coordinate3D.accept(1.5f, 2.3f, 3.7f);   // Prints: 3D Point: (1.50, 2.30, 3.70)
     * }</pre>
     *
     * @param a the first float input argument
     * @param b the second float input argument
     * @param c the third float input argument
     */
    @Override
    void accept(float a, float b, float c);

    /**
     * Returns a composed {@code FloatTriConsumer} that performs, in sequence, this
     * operation followed by the {@code after} operation. If performing either
     * operation throws an exception, it is relayed to the caller of the
     * composed operation. If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatTriConsumer logger = (a, b, c) -> System.out.printf("Values: %.2f, %.2f, %.2f%n", a, b, c);
     * FloatTriConsumer sum = (a, b, c) -> System.out.printf("Sum: %.2f%n", a + b + c);
     * FloatTriConsumer combined = logger.andThen(sum);
     * combined.accept(1.5f, 2.5f, 3.5f);   // Logs then calculates sum
     * }</pre>
     *
     * @param after the operation to perform after this operation. Must not be {@code null}.
     * @return a composed {@code FloatTriConsumer} that performs in sequence this
     *         operation followed by the {@code after} operation
     */
    default FloatTriConsumer andThen(final FloatTriConsumer after) {
        return (a, b, c) -> {
            accept(a, b, c);
            after.accept(a, b, c);
        };
    }
}
