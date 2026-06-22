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
 * Represents an operation that accepts three byte-valued arguments and returns no result.
 * This is the three-arity specialization of {@link ByteConsumer} for byte values.
 * Unlike most other functional interfaces, {@code ByteTriConsumer} is expected to operate via side-effects.
 *
 * <p>This is a functional interface whose functional method is {@link #accept(byte, byte, byte)}.
 *
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @see java.util.function.Consumer
 * @see ByteBiConsumer
 */
@FunctionalInterface
public interface ByteTriConsumer extends Throwables.ByteTriConsumer<RuntimeException> { //NOSONAR
    /**
     * Performs this operation on the given byte arguments.
     * This method is expected to operate via side-effects.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteTriConsumer rangePrinter = (min, mid, max) ->
     *     System.out.println("Range: [" + min + ", " + mid + ", " + max + "]");
     * rangePrinter.accept((byte) 0, (byte) 50, (byte) 100);   // Prints: Range: [0, 50, 100]
     * }</pre>
     *
     * @param a the first byte input argument
     * @param b the second byte input argument
     * @param c the third byte input argument
     */
    @Override
    void accept(byte a, byte b, byte c);

    /**
     * Returns a composed {@code ByteTriConsumer} that performs, in sequence, this operation
     * followed by the {@code after} operation. If performing either operation throws an exception,
     * it is relayed to the caller of the composed operation. If performing this operation throws
     * an exception, the {@code after} operation will not be performed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteTriConsumer logger = (a, b, c) -> System.out.println("Values: " + a + ", " + b + ", " + c);
     * ByteTriConsumer sum = (a, b, c) -> System.out.println("Sum: " + (a + b + c));
     * ByteTriConsumer combined = logger.andThen(sum);
     * combined.accept((byte) 1, (byte) 2, (byte) 3);   // Logs then calculates sum
     * }</pre>
     *
     * @param after the operation to perform after this operation. Must not be {@code null}.
     * @return a composed {@code ByteTriConsumer} that performs in sequence this operation
     *         followed by the {@code after} operation
     * @throws IllegalArgumentException if {@code after} is null
     */
    default ByteTriConsumer andThen(final ByteTriConsumer after) {
        N.checkArgNotNull(after, cs.after);
        return (a, b, c) -> {
            accept(a, b, c);
            after.accept(a, b, c);
        };
    }

    /**
     * Returns this object as a {@link Throwables.ByteTriConsumer} view.
     *
     * <p>The returned object has the same behavior as this one. This method does not translate
     * exceptions or make the original implementation capable of throwing new checked exceptions; the
     * exception type parameter is for target-type compatibility with APIs that accept {@code Throwables.ByteTriConsumer}.
     *
     * @param <E> the target exception type for compatibility with {@code Throwables.ByteTriConsumer}
     * @return a {@link Throwables.ByteTriConsumer} view of this object
     */
    default <E extends Throwable> Throwables.ByteTriConsumer<E> toThrowable() {
        return (Throwables.ByteTriConsumer<E>) this;
    }
}
