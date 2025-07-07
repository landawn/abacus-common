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

/**
 * Represents an operation that accepts a variable number of byte-valued arguments and returns no result.
 * This is a functional interface designed to consume byte arrays of any length.
 * 
 * <p>This is a functional interface whose functional method is {@link #accept(byte...)}.
 * 
 * @see java.util.function.Consumer
 */
@FunctionalInterface
public interface ByteNConsumer {

    /**
     * Performs this operation on the given byte array arguments.
     * The array can be of any length, including zero.
     *
     * @param args the byte array input arguments. Can be empty but not null.
     */
    void accept(byte... args);

    /**
     * Returns a composed {@code ByteNConsumer} that performs, in sequence, this operation 
     * followed by the {@code after} operation. If performing either operation throws an 
     * exception, it is relayed to the caller of the composed operation.
     * 
     * <p>The byte array is passed to both consumers in the same order.
     *
     * @param after the operation to perform after this operation. Must not be null.
     * @return a composed {@code ByteNConsumer} that performs in sequence this operation 
     *         followed by the {@code after} operation
     */
    default ByteNConsumer andThen(final ByteNConsumer after) {
        return args -> {
            accept(args);
            after.accept(args);
        };
    }
}