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
 * Represents an operation that accepts a single char-valued argument and returns no result.
 * This is the primitive type specialization of {@link java.util.function.Consumer} for {@code char}.
 * Unlike most other functional interfaces, {@code CharConsumer} is expected to operate via side-effects.
 *
 * <p>This is a functional interface whose functional method is {@link #accept(char)}.
 *
 * @see java.util.function.Consumer
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface CharConsumer extends Throwables.CharConsumer<RuntimeException> { //NOSONAR
    /**
     * Performs this operation on the given char argument.
     * This method is expected to operate via side-effects.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharConsumer printer = c -> System.out.println("Character: " + c);
     * printer.accept('A'); // Prints: Character: A
     *
     * StringBuilder sb = new StringBuilder();
     * CharConsumer appender = sb::append;
     * appender.accept('H');
     * appender.accept('i'); // sb now contains "Hi"
     * }</pre>
     *
     * @param t the char input argument
     */
    @Override
    void accept(char t);

    /**
     * Returns a composed {@code CharConsumer} that performs, in sequence, this operation
     * followed by the {@code after} operation. If performing either operation throws an exception,
     * it is relayed to the caller of the composed operation. If performing this operation throws
     * an exception, the {@code after} operation will not be performed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharConsumer logger = c -> System.out.println("Processing: " + c);
     * CharConsumer validator = c -> { if (!Character.isLetter(c)) throw new IllegalArgumentException(); };
     * CharConsumer combined = logger.andThen(validator);
     * combined.accept('A'); // Logs then validates
     * }</pre>
     *
     * @param after the operation to perform after this operation. Must not be {@code null}.
     * @return a composed {@code CharConsumer} that performs in sequence this operation
     *         followed by the {@code after} operation
     */
    default CharConsumer andThen(final CharConsumer after) {
        return t -> {
            accept(t);
            after.accept(t);
        };
    }

    /**
     * Converts this {@code CharConsumer} to a {@code Throwables.CharConsumer} that can throw a checked exception.
     * This method provides a way to use this consumer in contexts that require explicit exception handling.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharConsumer consumer = c -> System.out.println(c);
     * var throwableConsumer = consumer.toThrowable();
     * // Can now be used in contexts that handle checked exceptions
     * }</pre>
     *
     * @param <E> the type of exception that the returned consumer can throw
     * @return a {@code Throwables.CharConsumer} view of this consumer that can throw exceptions of type {@code E}
     */
    default <E extends Throwable> Throwables.CharConsumer<E> toThrowable() {
        return (Throwables.CharConsumer<E>) this;
    }
}
