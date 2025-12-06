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
 * Represents an operation that accepts two char-valued arguments and returns no result.
 * This is the two-arity specialization of {@link CharConsumer}.
 * Unlike most other functional interfaces, {@code CharBiConsumer} is expected to operate via side-effects.
 *
 * <p>This is a functional interface whose functional method is {@link #accept(char, char)}.
 *
 * @see java.util.function.BiConsumer
 * @see CharConsumer
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface CharBiConsumer extends Throwables.CharBiConsumer<RuntimeException> { //NOSONAR
    /**
     * Performs this operation on the given char arguments.
     * This method is expected to operate via side-effects.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharBiConsumer charPrinter = (first, second) ->
     *     System.out.println("Characters: " + first + ", " + second);
     * charPrinter.accept('A', 'Z');   // Prints: Characters: A, Z
     *
     * StringBuilder sb = new StringBuilder();
     * CharBiConsumer appender = (c1, c2) -> sb.append(c1).append(c2);
     * appender.accept('H', 'i');
     * }</pre>
     *
     * @param t the first char input argument
     * @param u the second char input argument
     */
    @Override
    void accept(char t, char u);

    /**
     * Returns a composed {@code CharBiConsumer} that performs, in sequence, this operation
     * followed by the {@code after} operation. If performing either operation throws an exception,
     * it is relayed to the caller of the composed operation. If performing this operation throws
     * an exception, the {@code after} operation will not be performed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharBiConsumer logger = (c1, c2) -> System.out.println("Processing: " + c1 + ", " + c2);
     * CharBiConsumer validator = (c1, c2) -> { if (!Character.isLetter(c1)) throw new IllegalArgumentException(); };
     * CharBiConsumer combined = logger.andThen(validator);
     * combined.accept('A', 'B');   // Logs then validates
     * }</pre>
     *
     * @param after the operation to perform after this operation. Must not be {@code null}.
     * @return a composed {@code CharBiConsumer} that performs in sequence this operation
     *         followed by the {@code after} operation
     */
    default CharBiConsumer andThen(final CharBiConsumer after) {
        return (t, u) -> {
            accept(t, u);
            after.accept(t, u);
        };
    }
}
