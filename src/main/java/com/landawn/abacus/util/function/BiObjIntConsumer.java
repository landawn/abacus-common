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

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.cs;

/**
 * Represents an operation that accepts two object-valued arguments and a single {@code int}-valued
 * argument, and returns no result. This is a specialization of consumer for two reference types
 * and one primitive {@code int} value.
 *
 * <p>This is a functional interface whose functional method is {@link #accept(Object, Object, int)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <T> the type of the first object argument to the operation
 * @param <U> the type of the second object argument to the operation
 */
@FunctionalInterface
public interface BiObjIntConsumer<T, U> extends Throwables.BiObjIntConsumer<T, U, RuntimeException> { //NOSONAR
    /**
     * Performs this operation on the given arguments.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiObjIntConsumer<String, List<String>> repeater = (str, list, times) -> {
     *     for (int j = 0; j < times; j++) list.add(str);
     * };
     * List<String> list = new ArrayList<>();
     * repeater.accept("hello", list, 3);   // Adds "hello" 3 times
     * }</pre>
     *
     * @param t the first input argument (object value)
     * @param u the second input argument (object value)
     * @param i the third input argument (int value)
     */
    @Override
    void accept(T t, U u, int i);

    /**
     * Returns a composed {@code BiObjIntConsumer} that performs, in sequence, this operation followed by the {@code after} operation.
     * If performing either operation throws an exception, it is relayed to the caller of the composed operation.
     * If performing this operation throws an exception, the {@code after} operation will not be performed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiObjIntConsumer<String, StringBuilder> appender = (str, sb, i) -> sb.append(str).append(i);
     * BiObjIntConsumer<String, StringBuilder> logger = (str, sb, i) -> System.out.println(sb);
     * BiObjIntConsumer<String, StringBuilder> combined = appender.andThen(logger);
     * }</pre>
     *
     * @param after the operation to perform after this operation. Must not be {@code null}.
     * @return a composed {@code BiObjIntConsumer} that performs in sequence this operation followed by the {@code after} operation
     * @throws IllegalArgumentException if {@code after} is null
     */
    default BiObjIntConsumer<T, U> andThen(final BiObjIntConsumer<? super T, ? super U> after) {
        N.checkArgNotNull(after, cs.after);
        return (t, u, i) -> {
            accept(t, u, i);
            after.accept(t, u, i);
        };
    }

    /**
     * Returns this object as a {@link Throwables.BiObjIntConsumer} view.
     *
     * <p>The returned object has the same behavior as this one. This method does not translate
     * exceptions or make the original implementation capable of throwing new checked exceptions; the
     * exception type parameter is for target-type compatibility with APIs that accept {@code Throwables.BiObjIntConsumer}.
     *
     * @param <E> the target exception type for compatibility with {@code Throwables.BiObjIntConsumer}
     * @return a {@link Throwables.BiObjIntConsumer} view of this object
     */
    default <E extends Throwable> Throwables.BiObjIntConsumer<T, U, E> toThrowable() {
        return (Throwables.BiObjIntConsumer<T, U, E>) this;
    }
}
