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
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.cs;

/**
 * Represents an operation that accepts a single {@code double}-valued argument and returns no result.
 * This is the primitive type specialization of {@link java.util.function.Consumer} for {@code double}.
 * Unlike most other functional interfaces, {@code DoubleConsumer} is expected to operate via side-effects.
 *
 * <p>This is a functional interface whose functional method is {@link #accept(double)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @see java.util.function.Consumer
 * @see java.util.function.DoubleConsumer
 */
@FunctionalInterface
public interface DoubleConsumer extends Throwables.DoubleConsumer<RuntimeException>, java.util.function.DoubleConsumer { //NOSONAR
    /**
     * Performs this operation on the given argument.
     * This method is expected to operate via side-effects.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleConsumer printer = d -> System.out.println("Value: " + d);
     * printer.accept(3.14);   // Prints: Value: 3.14
     *
     * DoubleStream.of(1.1, 2.2, 3.3).forEach(printer);
     * }</pre>
     *
     * @param value the input argument
     */
    @Override
    void accept(double value);

    /**
     * Returns a composed {@code DoubleConsumer} that performs, in sequence, this operation followed by the {@code after} operation.
     * If performing either operation throws an exception, it is relayed to the caller of the composed operation.
     * If performing this operation throws an exception, the {@code after} operation will not be performed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleConsumer logger = d -> System.out.println("Processing: " + d);
     * DoubleConsumer validator = d -> { if (d < 0) throw new IllegalArgumentException(); };
     * DoubleConsumer combined = logger.andThen(validator);
     * combined.accept(5.5);   // Logs then validates
     * }</pre>
     *
     * @param after the operation to perform after this operation. Must not be {@code null}.
     * @return a composed {@code DoubleConsumer} that performs in sequence this operation followed by the {@code after} operation
     * @throws IllegalArgumentException if {@code after} is null
     */
    @Override
    default DoubleConsumer andThen(final java.util.function.DoubleConsumer after) {
        N.checkArgNotNull(after, cs.after);
        return (final double value) -> {
            accept(value);
            after.accept(value);
        };
    }

    /**
     * Returns this object as a {@link Throwables.DoubleConsumer} view.
     *
     * <p>The returned object has the same behavior as this one. This method does not translate
     * exceptions or make the original implementation capable of throwing new checked exceptions; the
     * exception type parameter is for target-type compatibility with APIs that accept {@code Throwables.DoubleConsumer}.
     *
     * @param <E> the target exception type for compatibility with {@code Throwables.DoubleConsumer}
     * @return a {@link Throwables.DoubleConsumer} view of this object
     */
    default <E extends Throwable> Throwables.DoubleConsumer<E> toThrowable() {
        return (Throwables.DoubleConsumer<E>) this;
    }
}
