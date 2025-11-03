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
 * Represents an operation that accepts two {@code byte}-valued arguments and returns no result.
 * This is the primitive type specialization of {@link BiConsumer} for {@code byte}.
 *
 * <p>This is a functional interface whose functional method is {@link #accept(byte, byte)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface ByteBiConsumer extends Throwables.ByteBiConsumer<RuntimeException> { //NOSONAR
    /**
     * Performs this operation on the given arguments.
     * This method is expected to operate via side-effects.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteBiConsumer rangeChecker = (min, max) ->
     *     System.out.println("Range: [" + min + ", " + max + "]");
     * rangeChecker.accept((byte) 10, (byte) 100); // Prints: Range: [10, 100]
     *
     * Map<Byte, Byte> byteMap = new HashMap<>();
     * ByteBiConsumer mapPutter = (key, value) -> byteMap.put(key, value);
     * mapPutter.accept((byte) 1, (byte) 42);
     * }</pre>
     *
     * @param t the first input argument
     * @param u the second input argument
     */
    @Override
    void accept(byte t, byte u);

    /**
     * Returns a composed {@code ByteBiConsumer} that performs, in sequence, this operation followed by the {@code after} operation.
     * If performing either operation throws an exception, it is relayed to the caller of the composed operation.
     * If performing this operation throws an exception, the {@code after} operation will not be performed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteBiConsumer logger = (a, b) -> System.out.println("Values: " + a + ", " + b);
     * ByteBiConsumer validator = (a, b) -> { if (a < 0 || b < 0) throw new IllegalArgumentException(); };
     * ByteBiConsumer combined = logger.andThen(validator);
     * combined.accept((byte) 5, (byte) 10); // Logs then validates
     * }</pre>
     *
     * @param after the operation to perform after this operation. Must not be {@code null}.
     * @return a composed {@code ByteBiConsumer} that performs in sequence this operation followed by the {@code after} operation
     */
    default ByteBiConsumer andThen(final ByteBiConsumer after) {
        return (t, u) -> {
            accept(t, u);
            after.accept(t, u);
        };
    }
}
