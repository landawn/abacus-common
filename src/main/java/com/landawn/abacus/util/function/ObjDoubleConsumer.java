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
 * and a double-valued argument, and returns no result. This is a specialization of BiConsumer
 * for the case where the second argument is a primitive double.
 *
 * <p>This interface extends both {@link Throwables.ObjDoubleConsumer} and 
 * {@link java.util.function.ObjDoubleConsumer}, providing compatibility with the standard
 * Java functional interfaces while adding exception handling capabilities.
 *
 * <p>This is a functional interface whose functional method is {@link #accept(Object, double)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a>
 *
 * @param <T> the type of the object argument to the operation
 * @see java.util.function.ObjDoubleConsumer
 * @see java.util.function.BiConsumer
 */
@FunctionalInterface
public interface ObjDoubleConsumer<T> extends Throwables.ObjDoubleConsumer<T, RuntimeException>, java.util.function.ObjDoubleConsumer<T> { //NOSONAR

    /**
     * Performs this operation on the given arguments.
     *
     * <p>This method consumes an object of type T and a double value, performing some
     * side-effect operation without returning any result. Common use cases include
     * updating the object's state based on the double value, accumulating values,
     * or performing calculations where the result is stored within the object.
     *
     * @param t the first input argument of type T
     * @param value the second input argument, a primitive double value if the operation cannot be completed
     */
    @Override
    void accept(T t, double value);

    /**
     * Returns a composed {@code ObjDoubleConsumer} that performs, in sequence, this
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
     * ObjDoubleConsumer<List<Double>> addToList = (list, value) -> list.add(value);
     * ObjDoubleConsumer<List<Double>> printValue = (list, value) -> System.out.println(value);
     * ObjDoubleConsumer<List<Double>> combined = addToList.andThen(printValue);
     * // This will both add the value to the list and print it
     * combined.accept(myList, 3.14);
     * }</pre>
     *
     * @param after the operation to perform after this operation
     * @return a composed {@code ObjDoubleConsumer} that performs in sequence this
     *         operation followed by the {@code after} operation
     */
    default ObjDoubleConsumer<T> andThen(final ObjDoubleConsumer<? super T> after) {
        return (t, u) -> {
            accept(t, u);
            after.accept(t, u);
        };
    }
}