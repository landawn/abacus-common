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
 * A functional interface that represents an operation that accepts an object-valued argument
 * and a short-valued argument, and returns no result. This is a specialization of BiConsumer
 * for the case where the second argument is a primitive short.
 *
 * <p>This interface is the short primitive specialization of {@link ObjIntConsumer}.
 * Unlike the JDK which only provides specializations for int, long, and double primitives,
 * this interface extends support to short primitives for better type safety and performance.
 *
 * <p>This is a functional interface whose functional method is {@link #accept(Object, short)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <T> the type of the object argument to the operation
 * @see java.util.function.BiConsumer
 * @see ObjIntConsumer
 * @see ObjLongConsumer
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
     * @param value the second input argument, a primitive short value
     */
    @Override
    void accept(T t, short value);

    /**
     * Returns a composed {@code ObjShortConsumer} that performs, in sequence, this
     * operation followed by the {@code after} operation. If performing either
     * operation throws an exception, it is relayed to the caller of the
     * composed operation. If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjShortConsumer<List<Short>> add = (list, v) -> list.add(v);
     * ObjShortConsumer<List<Short>> log = (list, v) -> System.out.println("Added " + v);
     * ObjShortConsumer<List<Short>> combined = add.andThen(log);
     * combined.accept(new ArrayList<>(), (short) 7);
     * }</pre>
     *
     * @param after the operation to perform after this operation. Must not be {@code null}.
     * @return a composed {@code ObjShortConsumer} that performs in sequence this
     *         operation followed by the {@code after} operation
     * @throws IllegalArgumentException if {@code after} is null
     */
    default ObjShortConsumer<T> andThen(final ObjShortConsumer<? super T> after) {
        N.checkArgNotNull(after, cs.after);
        return (t, value) -> {
            accept(t, value);
            after.accept(t, value);
        };
    }

    /**
     * Returns this object as a {@link Throwables.ObjShortConsumer} view.
     *
     * <p>The returned object has the same behavior as this one. This method does not translate
     * exceptions or make the original implementation capable of throwing new checked exceptions; the
     * exception type parameter is for target-type compatibility with APIs that accept {@code Throwables.ObjShortConsumer}.
     *
     * @param <E> the target exception type for compatibility with {@code Throwables.ObjShortConsumer}
     * @return a {@link Throwables.ObjShortConsumer} view of this object
     */
    default <E extends Throwable> Throwables.ObjShortConsumer<T, E> toThrowable() {
        return (Throwables.ObjShortConsumer<T, E>) this;
    }
}
