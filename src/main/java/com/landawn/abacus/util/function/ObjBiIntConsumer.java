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

import com.landawn.abacus.util.Throwables;

/**
 * Represents an operation that accepts an object-valued argument and two int-valued arguments,
 * and returns no result. This is a three-arity specialization of {@code Consumer}.
 * Unlike most other functional interfaces, {@code ObjBiIntConsumer} is expected to operate via side-effects.
 *
 * <p>This is a functional interface whose functional method is {@link #accept(Object, int, int)}.
 *
 * <p>The interface extends {@code Throwables.ObjBiIntConsumer} with {@code RuntimeException} as the exception type,
 * making it suitable for use in contexts where checked exceptions are not required.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * ObjBiIntConsumer<String> printSubstring = (str, start, end) ->
 *     System.out.println(str.substring(start, end));
 * printSubstring.accept("Hello World", 0, 5);  // prints "Hello"
 *
 * ObjBiIntConsumer<int[]> setRange = (array, start, value) -> {
 *     for (int i = start; i < array.length && i < start + value; i++) {
 *         array[i] = value;
 *     }
 * };
 * }</pre>
 *
 * @param <T> the type of the object argument to the operation
 *
 * @see java.util.function.Consumer
 * @see java.util.function.ObjIntConsumer
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface ObjBiIntConsumer<T> extends Throwables.ObjBiIntConsumer<T, RuntimeException> { // NOSONAR
    /**
     * Performs this operation on the given arguments.
     *
     * <p>This method processes an object of type T and two int values, typically producing
     * side effects such as modifying the object, writing to output, or storing values.
     *
     * <p>Common use cases include:
     * <ul>
     *   <li>Processing array or list elements with two indices (start/end, row/column)</li>
     *   <li>Setting properties with two numeric parameters</li>
     *   <li>Logging objects with two associated numeric values</li>
     *   <li>Matrix operations where the object is the matrix and ints are coordinates</li>
     * </ul>
     *
     * @param t the object input argument
     * @param i the first int input argument
     * @param j the second int input argument
     */
    @Override
    void accept(T t, int i, int j);

    /**
     * Returns a composed {@code ObjBiIntConsumer} that performs, in sequence, this
     * operation followed by the {@code after} operation.
     *
     * <p>If performing either operation throws an exception, it is relayed to the
     * caller of the composed operation. If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * <p>This method is useful for chaining multiple operations that need to process
     * the same object and two int values in sequence.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjBiIntConsumer<StringBuilder> appendRange = (sb, start, end) ->
     *     sb.append(" Range: [").append(start).append(", ").append(end).append("]");
     * ObjBiIntConsumer<StringBuilder> appendSum = (sb, start, end) ->
     *     sb.append(" Sum: ").append(start + end);
     *
     * ObjBiIntConsumer<StringBuilder> combined = appendRange.andThen(appendSum);
     * StringBuilder sb = new StringBuilder("Data:");
     * combined.accept(sb, 10, 20);
     * // Result: "Data: Range: [10, 20] Sum: 30"
     * }</pre>
     *
     * @param after the operation to perform after this operation
     * @return a composed {@code ObjBiIntConsumer} that performs in sequence this
     *         operation followed by the {@code after} operation
     */
    default ObjBiIntConsumer<T> andThen(final ObjBiIntConsumer<? super T> after) {
        return (t, i, j) -> {
            accept(t, i, j);
            after.accept(t, i, j);
        };
    }
}
