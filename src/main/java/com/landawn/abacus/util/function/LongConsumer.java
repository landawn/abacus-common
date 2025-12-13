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
 * Represents an operation that accepts a single {@code long}-valued argument and
 * returns no result. This is the primitive type specialization of
 * {@link java.util.function.Consumer} for {@code long}. Unlike most other
 * functional interfaces, {@code LongConsumer} is expected to operate via
 * side-effects.
 *
 * <p>This interface extends both {@link Throwables.LongConsumer} with
 * {@link RuntimeException} and {@link java.util.function.LongConsumer},
 * providing compatibility with the Java standard library while supporting the
 * abacus-common framework's exception handling capabilities.
 *
 * <p>This is a functional interface whose functional method is
 * {@link #accept(long)}.
 *
 * <p>Refer to JDK API documentation at:
 * <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @see java.util.function.Consumer
 * @see java.util.function.LongConsumer
 */
@FunctionalInterface
public interface LongConsumer extends Throwables.LongConsumer<RuntimeException>, java.util.function.LongConsumer { //NOSONAR
    /**
     * Performs this operation on the given argument.
     * This method is expected to operate via side-effects.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongConsumer printer = value -> System.out.println("Value: " + value);
     * printer.accept(123456789L);   // Prints: Value: 123456789
     *
     * LongStream.range(1, 5).forEach(printer);
     * }</pre>
     *
     * @param t the input argument
     */
    @Override
    void accept(long t);

    /**
     * Returns a composed {@code LongConsumer} that performs, in sequence, this operation followed by
     * the {@code after} operation. If performing either operation throws an exception, it is relayed
     * to the caller of the composed operation. If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongConsumer logger = value -> System.out.println("Processing: " + value);
     * LongConsumer validator = value -> { if (value < 0) throw new IllegalArgumentException(); };
     * LongConsumer combined = logger.andThen(validator);
     * combined.accept(100L);   // Logs then validates
     * }</pre>
     *
     * @param after the operation to perform after this operation. Must not be {@code null}.
     * @return a composed {@code LongConsumer} that performs in sequence this operation followed by the {@code after} operation
     */
    @Override
    default LongConsumer andThen(final java.util.function.LongConsumer after) {
        return (final long t) -> {
            accept(t);
            after.accept(t);
        };
    }
}
