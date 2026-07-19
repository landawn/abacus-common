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

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.cs;

/**
 * Represents an operation that accepts three double-valued arguments and returns no result.
 * This is the three-arity specialization of {@link java.util.function.Consumer}.
 * Unlike most other functional interfaces, {@code DoubleTriConsumer} is expected to operate via side-effects.
 *
 * <p>This is a functional interface whose functional method is {@link #accept(double, double, double)}.</p>
 *
 * <p>This interface extends {@link Throwables.DoubleTriConsumer} with {@link RuntimeException},
 * providing exception handling capabilities while maintaining compatibility with standard functional programming patterns.</p>
 *
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @see java.util.function.Consumer
 * @see java.util.function.DoubleConsumer
 * @see java.util.function.BiConsumer
 */
@FunctionalInterface
public interface DoubleTriConsumer extends Throwables.DoubleTriConsumer<RuntimeException> { //NOSONAR
    /**
     * Performs this operation on the given three double arguments.
     *
     * <p>This method is expected to operate via side-effects, such as modifying external state,
     * printing output, or updating data structures. The specific behavior depends on the implementation.</p>
     *
     * <p>Common use cases include:</p>
     * <ul>
     *   <li>Processing three-dimensional coordinates (x, y, z)</li>
     *   <li>Handling RGB color values</li>
     *   <li>Performing calculations with three related double values</li>
     *   <li>Logging or recording three related measurements</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleTriConsumer pointPrinter = (x, y, z) ->
     *     System.out.printf("Point: (%.2f, %.2f, %.2f)%n", x, y, z);
     * pointPrinter.accept(1.0, 2.5, 3.7);   // Prints: Point: (1.00, 2.50, 3.70)
     * }</pre>
     *
     * @param a the first double argument
     * @param b the second double argument
     * @param c the third double argument
     */
    @Override
    void accept(double a, double b, double c);

    /**
     * Returns a composed {@code DoubleTriConsumer} that performs, in sequence, this operation
     * followed by the {@code after} operation. If performing either operation throws an exception,
     * it is relayed to the caller of the composed operation. If performing this operation throws
     * an exception, the {@code after} operation will not be performed.
     *
     * <p>This method allows for chaining multiple consumer operations. The composed consumer will:</p>
     * <ol>
     *   <li>First execute this consumer's {@code accept} method with the given arguments</li>
     *   <li>Then execute the {@code after} consumer's {@code accept} method with the same arguments</li>
     * </ol>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleTriConsumer printValues = (a, b, c) -> System.out.printf("Values: %.2f, %.2f, %.2f%n", a, b, c);
     * DoubleTriConsumer sumValues = (a, b, c) -> System.out.println("Sum: " + (a + b + c));
     *
     * // This will first print the values, then print their sum
     * DoubleTriConsumer combined = printValues.andThen(sumValues);
     * combined.accept(1.0, 2.0, 3.0);
     * }</pre>
     *
     * @param after the operation to perform after this operation. Must not be {@code null}.
     * @return a composed {@code DoubleTriConsumer} that performs in sequence this operation followed by the {@code after} operation
     * @throws IllegalArgumentException if {@code after} is null
     */
    default DoubleTriConsumer andThen(final DoubleTriConsumer after) {
        N.checkArgNotNull(after, cs.after);
        return (a, b, c) -> {
            accept(a, b, c);
            after.accept(a, b, c);
        };
    }

    /**
     * Returns this object as a {@link Throwables.DoubleTriConsumer} view.
     *
     * <p>The returned object has the same behavior as this one. This method does not translate
     * exceptions or make the original implementation capable of throwing new checked exceptions; the
     * exception type parameter is for target-type compatibility with APIs that accept {@code Throwables.DoubleTriConsumer}.
     *
     * @param <E> the target exception type for compatibility with {@code Throwables.DoubleTriConsumer}
     * @return a {@link Throwables.DoubleTriConsumer} view of this object
     */
    default <E extends Throwable> Throwables.DoubleTriConsumer<E> toThrowable() {
        return (Throwables.DoubleTriConsumer<E>) this;
    }
}
