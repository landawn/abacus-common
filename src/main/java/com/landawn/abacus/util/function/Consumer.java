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

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.Throwables;

/**
 * Represents an operation that accepts a single input argument and returns no result.
 * Unlike most other functional interfaces, {@code Consumer} is expected to operate via side-effects.
 * 
 * <p>This interface extends both {@link java.util.function.Consumer} and {@link Throwables.Consumer},
 * providing compatibility with the standard Java Consumer while restricting thrown exceptions to RuntimeException.
 * 
 * <p>This is a functional interface whose functional method is {@link #accept(Object)}.
 * 
 * <p>For more information about functional interfaces, refer to the JDK API documentation at:
 * <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a>
 * 
 * @param <T> the type of the input to the operation
 * 
 * @see java.util.function.Consumer
 */
@SuppressFBWarnings("NM_SAME_SIMPLE_NAME_AS_INTERFACE")
@FunctionalInterface
public interface Consumer<T> extends Throwables.Consumer<T, RuntimeException>, java.util.function.Consumer<T> { //NOSONAR

    /**
     * Performs this operation on the given argument.
     * This method is expected to operate via side-effects.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Consumer<String> printer = System.out::println;
     * printer.accept("Hello, World!"); // Prints: Hello, World!
     *
     * List<String> list = new ArrayList<>();
     * Consumer<String> listAdder = list::add;
     * listAdder.accept("item");
     * }</pre>
     *
     * @param t the input argument
     */
    @Override
    void accept(T t);

    /**
     * Returns a composed {@code Consumer} that performs, in sequence, this operation
     * followed by the {@code after} operation. If performing either operation throws an exception,
     * it is relayed to the caller of the composed operation.
     *
     * <p>This method overrides the default implementation in {@link java.util.function.Consumer}
     * to return the more specific {@code Consumer} type from this package.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Consumer<String> logger = s -> System.out.println("Processing: " + s);
     * Consumer<String> validator = s -> { if (s.isEmpty()) throw new IllegalArgumentException(); };
     * Consumer<String> combined = logger.andThen(validator);
     * combined.accept("data"); // Logs then validates
     * }</pre>
     *
     * @param after the operation to perform after this operation. Must not be {@code null}.
     * @return a composed {@code Consumer} that performs in sequence this operation followed by
     *         the {@code after} operation
     */
    @Override
    default Consumer<T> andThen(final java.util.function.Consumer<? super T> after) {
        return (final T t) -> {
            accept(t);
            after.accept(t);
        };
    }

    /**
     * Converts this Consumer to a Throwables.Consumer with a specified exception type.
     * This method performs an unchecked cast and is useful when you need to adapt this Consumer
     * to a context that expects a different exception type.
     * 
     * <p>Note: Since this is an unchecked cast, ensure that the actual implementation
     * only throws RuntimeException or its subclasses.
     *
     * @param <E> the type of exception that the returned Consumer is declared to throw
     * @return a Throwables.Consumer that is functionally equivalent to this Consumer
     */
    default <E extends Throwable> Throwables.Consumer<T, E> toThrowable() {
        return (Throwables.Consumer<T, E>) this;
    }
}