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
 * Represents an operation that accepts a single {@code int}-valued argument and returns no result.
 * This is the primitive type specialization of {@link java.util.function.Consumer} for {@code int}.
 * Unlike most other functional interfaces, {@code IntConsumer} is expected to operate via side-effects.
 *
 * <p>This interface extends both {@link java.util.function.IntConsumer} and
 * {@link Throwables.IntConsumer}, providing compatibility with the standard Java functional
 * interfaces while also supporting the Throwables framework.
 *
 * <p>This is a functional interface whose functional method is {@link #accept(int)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @see java.util.function.Consumer
 */
@FunctionalInterface
public interface IntConsumer extends Throwables.IntConsumer<RuntimeException>, java.util.function.IntConsumer { //NOSONAR
    /**
     * Performs this operation on the given argument.
     * This method is expected to operate via side-effects.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntConsumer printer = value -> System.out.println("Value: " + value);
     * printer.accept(42);  // Prints: Value: 42
     *
     * List<Integer> numbers = new ArrayList<>();
     * IntConsumer collector = numbers::add;
     * collector.accept(10);
     * }</pre>
     *
     * @param t the input argument
     */
    @Override
    void accept(int t);

    /**
     * Returns a composed {@code IntConsumer} that performs, in sequence, this operation followed by
     * the {@code after} operation. If performing either operation throws an exception, it is relayed
     * to the caller of the composed operation. If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntConsumer logger = value -> System.out.println("Processing: " + value);
     * IntConsumer validator = value -> { if (value < 0) throw new IllegalArgumentException(); };
     * IntConsumer combined = logger.andThen(validator);
     * combined.accept(100);  // Logs then validates
     * }</pre>
     *
     * @param after the operation to perform after this operation. Must not be {@code null}.
     * @return a composed {@code IntConsumer} that performs in sequence this operation followed by the {@code after} operation
     */
    @Override
    default IntConsumer andThen(final java.util.function.IntConsumer after) {
        return (final int t) -> {
            accept(t);
            after.accept(t);
        };
    }
}
