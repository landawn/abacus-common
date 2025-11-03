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
 * A functional interface that represents an operation that accepts an object-valued argument
 * and an int-valued argument, and returns no result. This is a specialization of BiConsumer
 * for the case where the second argument is a primitive int.
 *
 * <p>This interface extends both {@link Throwables.ObjIntConsumer} and
 * {@link java.util.function.ObjIntConsumer}, providing compatibility with the standard
 * Java functional interfaces while adding exception handling capabilities.
 *
 * <p>This is a functional interface whose functional method is {@link #accept(Object, int)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <T> the type of the object argument to the operation
 * @see java.util.function.ObjIntConsumer
 * @see java.util.function.BiConsumer
 */
@FunctionalInterface
public interface ObjIntConsumer<T> extends Throwables.ObjIntConsumer<T, RuntimeException>, java.util.function.ObjIntConsumer<T> { // NOSONAR
    /**
     * Performs this operation on the given arguments.
     *
     * <p>This method consumes an object of type T and an int value, performing some
     * side-effect operation without returning any result. Common use cases include
     * array or list indexing operations, counter updates, or any operation where
     * an integer parameter modifies the state of an object.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIntConsumer<List<String>> setAtIndex = (list, index) ->
     *     list.set(index, "Updated");
     * ObjIntConsumer<Counter> incrementBy = (counter, amount) ->
     *     counter.add(amount);
     * }</pre>
     *
     * @param t the first input argument of type T
     * @param value the second input argument, a primitive int value if the operation cannot be completed
     */
    @Override
    void accept(T t, int value);

    /**
     * Returns a composed {@code ObjIntConsumer} that performs, in sequence, this
     * operation followed by the {@code after} operation. If performing either
     * operation throws an exception, it is relayed to the caller of the
     * composed operation. If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * <p>This method allows for chaining multiple consumers together, where each
     * consumer receives the same input arguments. This is useful for performing
     * multiple independent operations on the same data.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIntConsumer<StringBuilder> appendIndex = (sb, index) ->
     *     sb.append("[").append(index).append("]");
     * ObjIntConsumer<StringBuilder> appendNewline = (sb, ignored) ->
     *     sb.append("\n");
     *
     * ObjIntConsumer<StringBuilder> combined = appendIndex.andThen(appendNewline);
     * StringBuilder builder = new StringBuilder();
     * combined.accept(builder, 5); // builder contains "[5]\n"
     * }</pre>
     *
     * @param after the operation to perform after this operation
     * @return a composed {@code ObjIntConsumer} that performs in sequence this
     *         operation followed by the {@code after} operation
     */
    default ObjIntConsumer<T> andThen(final ObjIntConsumer<? super T> after) {
        return (t, u) -> {
            accept(t, u);
            after.accept(t, u);
        };
    }
}
