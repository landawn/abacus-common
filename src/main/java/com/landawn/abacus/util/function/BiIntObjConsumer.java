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
 * Represents an operation that accepts two {@code int}-valued arguments and a single object-valued
 * argument, and returns no result. This is a specialization of consumer for two primitive {@code int}
 * values and one reference type.
 *
 * <p>This is a functional interface whose functional method is {@link #accept(int, int, Object)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <T> the type of the object argument to the operation
 */
@FunctionalInterface
public interface BiIntObjConsumer<T> extends Throwables.BiIntObjConsumer<T, RuntimeException> { // NOSONAR
    /**
     * Performs this operation on the given arguments.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiIntObjConsumer<StringBuilder> appender = (i, j, sb) -> sb.append(i).append(", ").append(j);
     * StringBuilder sb = new StringBuilder();
     * appender.accept(10, 20, sb);   // sb becomes "10, 20"
     * }</pre>
     *
     * @param i the first input argument (int value)
     * @param j the second input argument (int value)
     * @param t the third input argument (object value)
     */
    @Override
    void accept(int i, int j, T t);

    /**
     * Returns a composed {@code BiIntObjConsumer} that performs, in sequence, this operation followed by the {@code after} operation.
     * If performing either operation throws an exception, it is relayed to the caller of the composed operation.
     * If performing this operation throws an exception, the {@code after} operation will not be performed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiIntObjConsumer<List<String>> adder = (i, j, list) -> list.add("sum");
     * BiIntObjConsumer<List<String>> printer = (i, j, list) -> System.out.println(list);
     * BiIntObjConsumer<List<String>> combined = adder.andThen(printer);
     * }</pre>
     *
     * @param after the operation to perform after this operation. Must not be {@code null}.
     * @return a composed {@code BiIntObjConsumer} that performs in sequence this operation followed by the {@code after} operation
     */
    default BiIntObjConsumer<T> andThen(final BiIntObjConsumer<? super T> after) {
        return (i, j, t) -> {
            accept(i, j, t);
            after.accept(i, j, t);
        };
    }
}
