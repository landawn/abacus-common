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
 * Represents an operation that accepts two {@code long}-valued arguments and
 * returns no result. This is the primitive type specialization of
 * {@link java.util.function.BiConsumer} for {@code long}. Unlike most other
 * functional interfaces, {@code LongBiConsumer} is expected to operate via
 * side-effects.
 *
 * <p>This interface extends {@link Throwables.LongBiConsumer} with
 * {@link RuntimeException}, providing compatibility with the Abacus framework's
 * exception handling capabilities.
 *
 * <p>This is a functional interface whose functional method is
 * {@link #accept(long, long)}.
 *
 * @see java.util.function.BiConsumer
 * @see java.util.function.LongConsumer
 * @since 1.8
 */
@FunctionalInterface
public interface LongBiConsumer extends Throwables.LongBiConsumer<RuntimeException> { //NOSONAR

    /**
     * Performs this operation on the given arguments.
     *
     * <p>The consumer processes two long values and performs some side-effect
     * operation. Common use cases include:
     * <ul>
     *   <li>Accumulating or aggregating two long values</li>
     *   <li>Updating state based on two long parameters</li>
     *   <li>Processing pairs of timestamps or IDs</li>
     *   <li>Recording statistics or metrics from two long values</li>
     *   <li>Performing range-based operations with start and end values</li>
     * </ul>
     *
     * @param t the first input argument
     * @param u the second input argument
     */
    @Override
    void accept(long t, long u);

    /**
     * Returns a composed {@code LongBiConsumer} that performs, in sequence, this
     * operation followed by the {@code after} operation. If performing either
     * operation throws an exception, it is relayed to the caller of the
     * composed operation. If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * <p>This method allows for chaining multiple consumers to create a pipeline
     * of operations that will be executed sequentially on the same two input
     * values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongBiConsumer logger = (a, b) -> System.out.println("Processing: " + a + ", " + b);
     * LongBiConsumer processor = (a, b) -> processValues(a, b);
     * LongBiConsumer combined = processor.andThen(logger);
     * combined.accept(100L, 200L); // First processes, then logs
     * }</pre>
     *
     * @param after the operation to perform after this operation. Must not be null
     * @return a composed {@code LongBiConsumer} that performs in sequence this
     *         operation followed by the {@code after} operation
     */
    default LongBiConsumer andThen(final LongBiConsumer after) {
        return (t, u) -> {
            accept(t, u);
            after.accept(t, u);
        };
    }
}
