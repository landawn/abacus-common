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
import com.landawn.abacus.util.cs;

/**
 * Represents an operation that accepts two input arguments and returns no result.
 * This is the two-arity specialization of {@link java.util.function.Consumer}.
 * Unlike most other functional interfaces, {@code BiConsumer} is expected
 * to operate via side-effects.
 *
 * <p>This is a functional interface whose functional method is {@link #accept(Object, Object)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <T> the type of the first argument to the operation
 * @param <U> the type of the second argument to the operation
 */
@SuppressFBWarnings("NM_SAME_SIMPLE_NAME_AS_INTERFACE")
@FunctionalInterface
public interface BiConsumer<T, U> extends Throwables.BiConsumer<T, U, RuntimeException>, java.util.function.BiConsumer<T, U> { //NOSONAR
    /**
     * Performs this operation on the given arguments.
     * This method is expected to operate via side-effects, modifying the state of the arguments
     * or external state.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiConsumer<String, Integer> printer = (str, num) -> System.out.println(str + ": " + num);
     * printer.accept("Count", 42);   // Prints: Count: 42
     *
     * Map<String, Integer> map = new HashMap<>();
     * BiConsumer<String, Integer> mapPutter = (key, value) -> map.put(key, value);
     * mapPutter.accept("age", 30);
     * }</pre>
     *
     * @param t the first input argument
     * @param u the second input argument
     */
    @Override
    void accept(T t, U u);

    /**
     * Returns a composed {@code BiConsumer} that performs, in sequence, this operation followed by the {@code after} operation.
     * If performing either operation throws an exception, it is relayed to the caller of the composed operation.
     * If performing this operation throws an exception, the {@code after} operation will not be performed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiConsumer<String, Integer> logger = (name, value) -> System.out.println("Logging: " + name + " = " + value);
     * BiConsumer<String, Integer> validator = (name, value) -> {
     *     if (value < 0) throw new IllegalArgumentException();
     * };
     * BiConsumer<String, Integer> combined = logger.andThen(validator);
     * combined.accept("score", 85);   // Logs then validates
     * }</pre>
     *
     * @param after the operation to perform after this operation. Must not be {@code null}.
     * @return a composed {@code BiConsumer} that performs in sequence this operation followed by the {@code after} operation
     * @throws NullPointerException if {@code after} is null
     */
    @Override
    default BiConsumer<T, U> andThen(final java.util.function.BiConsumer<? super T, ? super U> after) {
        java.util.Objects.requireNonNull(after, cs.after);
        return (t, u) -> {
            accept(t, u);
            after.accept(t, u);
        };
    }

    /**
     * Returns this consumer as a {@link Throwables.BiConsumer} view.
     *
     * <p>The returned consumer has the same behavior as this one. This method does not translate
     * exceptions or make the original implementation capable of throwing new checked exceptions; the
     * exception type parameter is for target-type compatibility with APIs that accept
     * {@code Throwables.BiConsumer}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiConsumer<String, String> concatenator = (s1, s2) -> System.out.println(s1 + s2);
     * var throwableConsumer = concatenator.toThrowable();
     * // Can now be used in a context whose callback declares an exception type
     * }</pre>
     *
     * @param <E> the target exception type for compatibility with {@code Throwables.BiConsumer}
     * @return a {@code Throwables.BiConsumer} view of this consumer
     */
    default <E extends Throwable> Throwables.BiConsumer<T, U, E> toThrowable() {
        return (Throwables.BiConsumer<T, U, E>) this;
    }
}
