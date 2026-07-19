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
 * Represents an operation that accepts an object-valued argument and a boolean-valued argument,
 * and returns no result. This is a two-arity specialization of {@code Consumer}.
 * Unlike most other functional interfaces, {@code ObjBooleanConsumer} is expected to operate via side-effects.
 *
 * <p>This is a functional interface whose functional method is {@link #accept(Object, boolean)}.
 *
 * <p>The interface extends {@code Throwables.ObjBooleanConsumer} with {@code RuntimeException} as the exception type,
 * making it suitable for use in contexts where checked exceptions are not required.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * ObjBooleanConsumer<String> conditionalPrinter = (message, shouldPrint) -> {
 *     if (shouldPrint) {
 *         System.out.println(message);
 *     }
 * };
 * conditionalPrinter.accept("Hello World", true);   // prints "Hello World"
 * conditionalPrinter.accept("Hidden", false);   // prints nothing
 *
 * ObjBooleanConsumer<User> setActiveStatus = (user, isActive) -> {
 *     user.setActive(isActive);
 *     user.setLastModified(new Date());
 * };
 *
 * ObjBooleanConsumer<File> setReadOnly = (file, readOnly) -> {
 *     if (readOnly) {
 *         file.setReadOnly();
 *     } else {
 *         file.setWritable(true);
 *     }
 * };
 * }</pre>
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <T> the type of the object argument to the operation
 *
 * @see java.util.function.Consumer
 * @see java.util.function.ObjIntConsumer
 * @see java.util.function.ObjLongConsumer
 * @see java.util.function.ObjDoubleConsumer
 */
@FunctionalInterface
public interface ObjBooleanConsumer<T> extends Throwables.ObjBooleanConsumer<T, RuntimeException> { //NOSONAR
    /**
     * Performs this operation on the given arguments.
     * This method is expected to operate via side-effects.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjBooleanConsumer<StringBuilder> mark = (sb, enabled) -> sb.append(enabled ? "Y" : "N");
     * mark.accept(new StringBuilder(), true);
     * }</pre>
     *
     * @param t the object to receive or contextualize the side effect
     * @param value the boolean flag that influences the side effect
     */
    @Override
    void accept(T t, boolean value);

    /**
     * Returns a composed {@code ObjBooleanConsumer} that performs, in sequence, this
     * operation followed by the {@code after} operation. If performing either
     * operation throws an exception, it is relayed to the caller of the
     * composed operation. If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjBooleanConsumer<StringBuilder> mark = (sb, b) -> sb.append(b ? 'Y' : 'N');
     * ObjBooleanConsumer<StringBuilder> nl   = (sb, b) -> sb.append('\n');
     * ObjBooleanConsumer<StringBuilder> markln = mark.andThen(nl);
     * markln.accept(new StringBuilder(), true);
     * }</pre>
     *
     * @param after the operation to perform after this operation. Must not be {@code null}.
     * @return a composed {@code ObjBooleanConsumer} that performs in sequence this
     *         operation followed by the {@code after} operation
     * @throws IllegalArgumentException if {@code after} is null
     */
    default ObjBooleanConsumer<T> andThen(final ObjBooleanConsumer<? super T> after) {
        N.checkArgNotNull(after, cs.after);
        return (t, value) -> {
            accept(t, value);
            after.accept(t, value);
        };
    }

    /**
     * Returns this object as a {@link Throwables.ObjBooleanConsumer} view.
     *
     * <p>The returned object has the same behavior as this one. This method does not translate
     * exceptions or make the original implementation capable of throwing new checked exceptions; the
     * exception type parameter is for target-type compatibility with APIs that accept {@code Throwables.ObjBooleanConsumer}.
     *
     * @param <E> the target exception type for compatibility with {@code Throwables.ObjBooleanConsumer}
     * @return a {@link Throwables.ObjBooleanConsumer} view of this object
     */
    default <E extends Throwable> Throwables.ObjBooleanConsumer<T, E> toThrowable() {
        return (Throwables.ObjBooleanConsumer<T, E>) this;
    }
}
