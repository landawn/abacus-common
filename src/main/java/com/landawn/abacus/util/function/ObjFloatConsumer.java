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
 * and a float-valued argument, and returns no result. This is a specialization of BiConsumer
 * for the case where the second argument is a primitive float.
 *
 * <p>This interface is the float primitive specialization of {@link ObjDoubleConsumer}.
 * Unlike the JDK which only provides specializations for int, long, and double primitives,
 * this interface extends support to float primitives for better type safety and performance.
 *
 * <p>This is a functional interface whose functional method is {@link #accept(Object, float)}.
 *
 * @param <T> the type of the object argument to the operation
 * @see ObjDoubleConsumer
 * @see java.util.function.BiConsumer
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface ObjFloatConsumer<T> extends Throwables.ObjFloatConsumer<T, RuntimeException> { //NOSONAR
    /**
     * Performs this operation on the given arguments.
     *
     * <p>This method consumes an object of type T and a float value, performing some
     * side-effect operation without returning any result. Common use cases include
     * updating the object's state based on the float value, accumulating float values,
     * or performing calculations where the result is stored within the object.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjFloatConsumer<FloatBuffer> addToBuffer = (buffer, value) -> buffer.put(value);
     * ObjFloatConsumer<Statistics> updateStats = (stats, value) -> stats.addSample(value);
     * }</pre>
     *
     * @param t the first input argument of type T
     * @param value the second input argument, a primitive float value if the operation cannot be completed
     */
    @Override
    void accept(T t, float value);
}
