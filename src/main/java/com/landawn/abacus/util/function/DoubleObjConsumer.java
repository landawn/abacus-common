/*
 * Copyright (C) 2024 HaiYang Li
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

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.cs;

/**
 * Represents an operation that accepts a double-valued argument and an object-valued argument,
 * and returns no result. This is the (double, reference) specialization of {@link java.util.function.BiConsumer}.
 * Unlike most other functional interfaces, {@code DoubleObjConsumer} is expected to operate via side-effects.
 *
 * <p>This is a functional interface whose functional method is {@link #accept(double, Object)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <T> the type of the object argument to the operation
 *
 * @see java.util.function.BiConsumer
 */
@FunctionalInterface
public interface DoubleObjConsumer<T> extends Throwables.DoubleObjConsumer<T, RuntimeException> { // NOSONAR
    /**
     * Performs this operation on the given arguments.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleObjConsumer<String> logger = (value, msg) ->
     *     System.out.println(msg + ": " + value);
     * logger.accept(3.14, "Pi");   // Prints: Pi: 3.14
     *
     * Map<String, Double> map = new HashMap<>();
     * DoubleObjConsumer<String> mapPutter = (value, key) -> map.put(key, value);
     * mapPutter.accept(98.6, "temperature");
     * }</pre>
     *
     * @param t the double input argument
     * @param u the object input argument
     */
    @Override
    void accept(double t, T u);

    /**
     * Returns a composed {@code DoubleObjConsumer} that performs, in sequence, this operation followed by the {@code after} operation.
     * If performing either operation throws an exception, it is relayed to the caller of the composed operation.
     * If performing this operation throws an exception, the {@code after} operation will not be performed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleObjConsumer<String> logger = (val, msg) -> System.out.println(msg + ": " + val);
     * DoubleObjConsumer<String> validator = (val, msg) -> {
     *     if (val < 0) throw new IllegalArgumentException(msg);
     * };
     * DoubleObjConsumer<String> combined = logger.andThen(validator);
     * combined.accept(5.5, "value");   // Logs then validates
     * }</pre>
     *
     * @param after the operation to perform after this operation. Must not be {@code null}.
     * @return a composed {@code DoubleObjConsumer} that performs in sequence this operation followed by the {@code after} operation
     * @throws IllegalArgumentException if {@code after} is null
     */
    default DoubleObjConsumer<T> andThen(final DoubleObjConsumer<? super T> after) {
        N.checkArgNotNull(after, cs.after);
        return (t, u) -> {
            accept(t, u);
            after.accept(t, u);
        };
    }

    /**
     * Returns this object as a {@link Throwables.DoubleObjConsumer} view.
     *
     * <p>The returned object has the same behavior as this one. This method does not translate
     * exceptions or make the original implementation capable of throwing new checked exceptions; the
     * exception type parameter is for target-type compatibility with APIs that accept {@code Throwables.DoubleObjConsumer}.
     *
     * @param <E> the target exception type for compatibility with {@code Throwables.DoubleObjConsumer}
     * @return a {@link Throwables.DoubleObjConsumer} view of this object
     */
    default <E extends Throwable> Throwables.DoubleObjConsumer<T, E> toThrowable() {
        return (Throwables.DoubleObjConsumer<T, E>) this;
    }
}
