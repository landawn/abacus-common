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
 * Represents an operation that accepts an object-valued argument and a byte-valued argument,
 * and returns no result. This is a two-arity specialization of {@code Consumer}.
 * Unlike most other functional interfaces, {@code ObjByteConsumer} is expected to operate via side-effects.
 *
 * <p>This is a functional interface whose functional method is {@link #accept(Object, byte)}.
 *
 * <p>The interface extends {@code Throwables.ObjByteConsumer} with {@code RuntimeException} as the exception type,
 * making it suitable for use in contexts where checked exceptions are not required.
 *
 * <p>Note: Unlike some other primitive specializations, this interface does not provide default methods
 * for composition as the JDK does not provide a standard ObjByteConsumer interface.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * ObjByteConsumer<ByteBuffer> putByte = (buffer, value) -> buffer.put(value);
 * ByteBuffer buffer = ByteBuffer.allocate(10);
 * putByte.accept(buffer, (byte) 42);
 *
 * ObjByteConsumer<OutputStream> writeByte = (stream, b) -> {
 *     try {
 *         stream.write(b);
 *     } catch (IOException e) {
 *         throw new RuntimeException(e);
 *     }
 * };
 *
 * ObjByteConsumer<byte[]> setAtIndex = (array, value) -> {
 *     if (array.length > 0) {
 *         array[0] = value;
 *     }
 * };
 * }</pre>
 *
 * @param <T> the type of the object argument to the operation
 *
 * @see java.util.function.Consumer
 * @see java.util.function.ObjIntConsumer
 * @see java.util.function.ObjLongConsumer
 * @see java.util.function.ObjDoubleConsumer
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface ObjByteConsumer<T> extends Throwables.ObjByteConsumer<T, RuntimeException> { //NOSONAR
    /**
     * Performs this operation on the given arguments.
     * This method is expected to operate via side-effects.
     *
     * @param t the first input argument
     * @param value the second input argument
     */
    @Override
    void accept(T t, byte value);
}
