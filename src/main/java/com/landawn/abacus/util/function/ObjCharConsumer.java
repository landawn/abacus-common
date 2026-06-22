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
 * Represents an operation that accepts an object-valued argument and a char-valued argument,
 * and returns no result. This is a two-arity specialization of {@code Consumer}.
 * Unlike most other functional interfaces, {@code ObjCharConsumer} is expected to operate via side-effects.
 *
 * <p>This is a functional interface whose functional method is {@link #accept(Object, char)}.
 *
 * <p>The interface extends {@code Throwables.ObjCharConsumer} with {@code RuntimeException} as the exception type,
 * making it suitable for use in contexts where checked exceptions are not required.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * ObjCharConsumer<StringBuilder> appendChar = (sb, ch) -> sb.append(ch);
 * StringBuilder builder = new StringBuilder("Hello");
 * appendChar.accept(builder, '!');   // builder becomes "Hello!"
 *
 * ObjCharConsumer<Map<String, Character>> putChar = (map, value) ->
 *     map.put("lastChar", value);
 *
 * ObjCharConsumer<CharBuffer> putCharBuffer = (buffer, ch) -> buffer.put(ch);
 * CharBuffer buffer = CharBuffer.allocate(10);
 * putCharBuffer.accept(buffer, 'A');
 * }</pre>
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <T> the type of the object argument to the operation
 *
 * @see java.util.function.Consumer
 * @see java.util.function.BiConsumer
 * @see java.util.function.ObjIntConsumer
 */
@FunctionalInterface
public interface ObjCharConsumer<T> extends Throwables.ObjCharConsumer<T, RuntimeException> { //NOSONAR
    /**
     * Performs this operation on the given arguments.
     * This method is expected to operate via side-effects.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjCharConsumer<StringBuilder> appendChar = (sb, ch) -> sb.append(ch);
     * appendChar.accept(new StringBuilder("Hi"), '!');
     * }</pre>
     *
     * @param t the object input argument
     * @param value the char input argument
     */
    // @ai-ignore Obj* argument order convention - intentional: class name prefix determines parameter order. ObjChar* has object first, char second. Do not suggest reordering.
    @Override
    void accept(T t, char value);

    /**
     * Returns a composed {@code ObjCharConsumer} that performs, in sequence, this
     * operation followed by the {@code after} operation. If performing either
     * operation throws an exception, it is relayed to the caller of the
     * composed operation. If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjCharConsumer<StringBuilder> append = (sb, ch) -> sb.append(ch);
     * ObjCharConsumer<StringBuilder> nl     = (sb, ch) -> sb.append('\n');
     * ObjCharConsumer<StringBuilder> both   = append.andThen(nl);
     * both.accept(new StringBuilder(), 'X');
     * }</pre>
     *
     * @param after the operation to perform after this operation. Must not be {@code null}.
     * @return a composed {@code ObjCharConsumer} that performs in sequence this
     *         operation followed by the {@code after} operation
     * @throws IllegalArgumentException if {@code after} is null
     */
    default ObjCharConsumer<T> andThen(final ObjCharConsumer<? super T> after) {
        N.checkArgNotNull(after, cs.after);
        return (t, value) -> {
            accept(t, value);
            after.accept(t, value);
        };
    }

    /**
     * Returns this object as a {@link Throwables.ObjCharConsumer} view.
     *
     * <p>The returned object has the same behavior as this one. This method does not translate
     * exceptions or make the original implementation capable of throwing new checked exceptions; the
     * exception type parameter is for target-type compatibility with APIs that accept {@code Throwables.ObjCharConsumer}.
     *
     * @param <E> the target exception type for compatibility with {@code Throwables.ObjCharConsumer}
     * @return a {@link Throwables.ObjCharConsumer} view of this object
     */
    default <E extends Throwable> Throwables.ObjCharConsumer<T, E> toThrowable() {
        return (Throwables.ObjCharConsumer<T, E>) this;
    }
}
