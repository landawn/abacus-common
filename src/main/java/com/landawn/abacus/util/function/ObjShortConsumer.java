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
 * A functional interface that represents an operation that accepts an object-valued and a
 * short-valued argument, and returns no result. This is a specialization of BiConsumer.
 *
 * <p>This is a functional interface whose functional method is {@link #accept(Object, short)}.
 *
 * @param <T> the type of the object argument to the operation
 * @see java.util.function.BiConsumer
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface ObjShortConsumer<T> extends Throwables.ObjShortConsumer<T, RuntimeException> { //NOSONAR
    /**
     * Performs this operation on the given arguments.
     *
     * <p>This method consumes an object of type T and a short value, performing some
     * side-effect operation without returning any result. Common use cases include
     * updating the object's state based on the short value, working with small numeric
     * ranges, or processing data where memory efficiency is important and values fit
     * within the short range (-32,768 to 32,767).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjShortConsumer<List<Short>> addToList = (list, value) -> list.add(value);
     * ObjShortConsumer<StringBuilder> appendShort = (sb, value) -> sb.append(value);
     *
     * List<Short> values = new ArrayList<>();
     * addToList.accept(values, (short) 42);
     * StringBuilder sb = new StringBuilder("Port: ");
     * appendShort.accept(sb, (short) 8080);
     * }</pre>
     *
     * @param t the first input argument of type T
     * @param value the second input argument, a primitive short value if the operation cannot be completed
     */
    @Override
    void accept(T t, short value);
}
