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
 * Represents an operation that accepts a single {@code boolean}-valued argument and returns no result.
 * This is the primitive type specialization of {@link java.util.function.Consumer} for {@code boolean}.
 * Unlike most other functional interfaces, {@code BooleanConsumer} is expected to operate via side-effects.
 *
 * <p>This is a functional interface whose functional method is {@link #accept(boolean)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface BooleanConsumer extends Throwables.BooleanConsumer<RuntimeException> { //NOSONAR
    /**
     * Performs this operation on the given argument.
     * This method is expected to operate via side-effects.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanConsumer logger = value -> System.out.println("Boolean value: " + value);
     * logger.accept(true);   // Prints: Boolean value: true
     *
     * List<Boolean> results = new ArrayList<>();
     * BooleanConsumer collector = results::add;
     * collector.accept(false);
     * }</pre>
     *
     * @param value the input argument
     */
    @Override
    void accept(boolean value);

    /**
     * Returns a composed {@code BooleanConsumer} that performs, in sequence, this operation followed by the {@code after} operation.
     * If performing either operation throws an exception, it is relayed to the caller of the composed operation.
     * If performing this operation throws an exception, the {@code after} operation will not be performed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanConsumer logger = value -> System.out.println("Value: " + value);
     * BooleanConsumer validator = value -> { if (!value) throw new IllegalArgumentException(); };
     * BooleanConsumer combined = logger.andThen(validator);
     * combined.accept(true);   // Logs then validates
     * }</pre>
     *
     * @param after the operation to perform after this operation. Must not be {@code null}.
     * @return a composed {@code BooleanConsumer} that performs in sequence this operation followed by the {@code after} operation
     */
    default BooleanConsumer andThen(final BooleanConsumer after) {
        return value -> {
            accept(value);
            after.accept(value);
        };
    }
}
