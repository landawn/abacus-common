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
 * Represents an operation that accepts an object-valued argument and a char-valued argument,
 * and returns no result. This is a two-arity specialization of {@code Consumer}.
 * Unlike most other functional interfaces, {@code ObjCharConsumer} is expected to operate via side-effects.
 *
 * <p>This is a functional interface whose functional method is {@link #accept(Object, char)}.
 *
 * <p>The interface extends {@code Throwables.ObjCharConsumer} with {@code RuntimeException} as the exception type,
 * making it suitable for use in contexts where checked exceptions are not required.
 *
 * <p>Note: Unlike some other primitive specializations, this interface does not provide default methods
 * for composition as the JDK does not provide a standard ObjCharConsumer interface.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * ObjCharConsumer<StringBuilder> appendChar = (sb, ch) -> sb.append(ch);
 * StringBuilder builder = new StringBuilder("Hello");
 * appendChar.accept(builder, '!'); // builder becomes "Hello!"
 *
 * ObjCharConsumer<Map<String, Character>> putChar = (map, value) ->
 *     map.put("lastChar", value);
 *
 * ObjCharConsumer<CharBuffer> putCharBuffer = (buffer, ch) -> buffer.put(ch);
 * CharBuffer buffer = CharBuffer.allocate(10);
 * putCharBuffer.accept(buffer, 'A');
 * }</pre>
 *
 * @param <T> the type of the object argument to the operation
 *
 * @see java.util.function.Consumer
 * @see java.util.function.BiConsumer
 * @see java.util.function.ObjIntConsumer
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface ObjCharConsumer<T> extends Throwables.ObjCharConsumer<T, RuntimeException> { //NOSONAR
    /**
     * Performs this operation on the given arguments.
     *
     * <p>This method processes an object of type T and a char value, typically producing
     * side effects such as appending the char to a buffer, storing it in a collection,
     * or modifying the object based on the char value.
     *
     * <p>Common use cases include:
     * <ul>
     *   <li>Appending characters to StringBuilder or StringBuffer</li>
     *   <li>Writing characters to output streams or buffers</li>
     *   <li>Setting character values in maps or collections</li>
     *   <li>Processing text data character by character</li>
     *   <li>Updating object state based on character input</li>
     * </ul>
     *
     * @param t the object input argument
     * @param value the char input argument
     */
    @Override
    void accept(T t, char value);

    /**
     * Converts this {@code ObjCharConsumer} to a {@code Throwables.ObjCharConsumer} that can throw a checked exception.
     * This method provides a way to use this consumer in contexts that require explicit exception handling.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjCharConsumer consumer = (...) -> { ... };
     * var throwableConsumer = consumer.toThrowable();
     * // Can now be used in contexts that handle checked exceptions
     * }</pre>
     *
     * @param <E> the type of exception that the returned consumer can throw
     * @return a {@code Throwables.ObjCharConsumer} view of this consumer that can throw exceptions of type {@code E}
     */
    default <E extends Throwable> Throwables.ObjCharConsumer<T, E> toThrowable() {
        return (Throwables.ObjCharConsumer<T, E>) this;
    }

}
