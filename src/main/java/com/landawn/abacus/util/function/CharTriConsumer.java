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
 * Represents an operation that accepts three char-valued arguments and returns no result.
 * This is the three-arity specialization of {@link CharConsumer}.
 * Unlike most other functional interfaces, {@code CharTriConsumer} is expected to operate via side-effects.
 *
 * <p>This is a functional interface whose functional method is {@link #accept(char, char, char)}.
 *
 * @see java.util.function.Consumer
 * @see CharConsumer
 * @see CharBiConsumer
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface CharTriConsumer extends Throwables.CharTriConsumer<RuntimeException> { //NOSONAR
    /**
     * Performs this operation on the given char arguments.
     * This method is expected to operate via side-effects.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharTriConsumer wordBuilder = (c1, c2, c3) ->
     *     System.out.println("Word: " + c1 + c2 + c3);
     * wordBuilder.accept('d', 'o', 'g'); // Prints: Word: dog
     * }</pre>
     *
     * @param a the first char input argument
     * @param b the second char input argument
     * @param c the third char input argument
     */
    @Override
    void accept(char a, char b, char c);

    /**
     * Returns a composed {@code CharTriConsumer} that performs, in sequence, this operation
     * followed by the {@code after} operation. If performing either operation throws an exception,
     * it is relayed to the caller of the composed operation. If performing this operation throws
     * an exception, the {@code after} operation will not be performed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharTriConsumer logger = (a, b, c) -> System.out.println("Chars: " + a + ", " + b + ", " + c);
     * CharTriConsumer validator = (a, b, c) -> { if (!Character.isLetter(a)) throw new IllegalArgumentException(); };
     * CharTriConsumer combined = logger.andThen(validator);
     * combined.accept('A', 'B', 'C'); // Logs then validates
     * }</pre>
     *
     * @param after the operation to perform after this operation. Must not be {@code null}.
     * @return a composed {@code CharTriConsumer} that performs in sequence this operation
     *         followed by the {@code after} operation
     */
    default CharTriConsumer andThen(final CharTriConsumer after) {
        return (a, b, c) -> {
            accept(a, b, c);
            after.accept(a, b, c);
        };
    }

    /**
     * Converts this {@code CharTriConsumer} to a {@code Throwables.CharTriConsumer} that can throw a checked exception.
     * This method provides a way to use this consumer in contexts that require explicit exception handling.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharTriConsumer consumer = (...) -> { ... };
     * var throwableConsumer = consumer.toThrowable();
     * // Can now be used in contexts that handle checked exceptions
     * }</pre>
     *
     * @param <E> the type of exception that the returned consumer can throw
     * @return a {@code Throwables.CharTriConsumer} view of this consumer that can throw exceptions of type {@code E}
     */
    default <E extends Throwable> Throwables.CharTriConsumer<E> toThrowable() {
        return (Throwables.CharTriConsumer<E>) this;
    }

}
