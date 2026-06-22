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
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.cs;

/**
 * Represents an operation that accepts an {@code int}-valued and an object-valued argument,
 * and returns no result. This is the {@code (int, reference)} specialization of {@link java.util.function.BiConsumer}.
 * Unlike most other functional interfaces, {@code IntObjConsumer} is expected to operate via side-effects.
 *
 * <p>The parameter order follows the {@code IntObj*} convention: the {@code int} argument comes
 * first, followed by the object argument of type {@code T}.
 *
 * <p>This is a functional interface whose functional method is {@link #accept(int, Object)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <T> the type of the object argument to the operation
 * @see java.util.function.BiConsumer
 * @see IntConsumer
 */
@FunctionalInterface
public interface IntObjConsumer<T> extends Throwables.IntObjConsumer<T, RuntimeException> { // NOSONAR
    /**
     * Performs this operation on the given arguments.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntObjConsumer<StringBuilder> appender = (value, sb) -> sb.append(value);
     * StringBuilder sb = new StringBuilder("Value: ");
     * appender.accept(42, sb);   // sb becomes "Value: 42"
     * }</pre>
     *
     * @param t the {@code int} argument
     * @param u the object argument
     */
    // @ai-ignore IntObj*/ObjInt* argument order convention - intentional: class name prefix determines parameter order. IntObj* has int first; ObjInt* has object first. Do not suggest reordering.
    @Override
    void accept(int t, T u);

    /**
     * Returns a composed {@code IntObjConsumer} that performs, in sequence, this operation followed by
     * the {@code after} operation. If performing either operation throws an exception, it is relayed
     * to the caller of the composed operation. If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * @param after the operation to perform after this operation. Must not be {@code null}.
     * @return a composed {@code IntObjConsumer} that performs in sequence this operation followed by
     *         the {@code after} operation
     * @throws IllegalArgumentException if {@code after} is null
     */
    default IntObjConsumer<T> andThen(final IntObjConsumer<? super T> after) {
        N.checkArgNotNull(after, cs.after);
        return (t, u) -> {
            accept(t, u);
            after.accept(t, u);
        };
    }

    /**
     * Returns this object as a {@link Throwables.IntObjConsumer} view.
     *
     * <p>The returned object has the same behavior as this one. This method does not translate
     * exceptions or make the original implementation capable of throwing new checked exceptions; the
     * exception type parameter is for target-type compatibility with APIs that accept {@code Throwables.IntObjConsumer}.
     *
     * @param <E> the target exception type for compatibility with {@code Throwables.IntObjConsumer}
     * @return a {@link Throwables.IntObjConsumer} view of this object
     */
    default <E extends Throwable> Throwables.IntObjConsumer<T, E> toThrowable() {
        return (Throwables.IntObjConsumer<T, E>) this;
    }
}
