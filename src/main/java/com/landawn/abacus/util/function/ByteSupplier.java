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
 * Represents a supplier of byte-valued results. This is the byte-producing
 * primitive specialization of {@link java.util.function.Supplier}.
 *
 * <p>There is no requirement that a new or distinct result be returned each time the supplier is invoked.
 *
 * <p>This is a functional interface whose functional method is {@link #getAsByte()}.
 *
 * @see java.util.function.Supplier
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface ByteSupplier extends Throwables.ByteSupplier<RuntimeException> { //NOSONAR
    /**
     * A supplier that always returns zero.
     * This is useful as a default value or for initialization purposes.
     */
    ByteSupplier ZERO = () -> 0;
    /**
     * A supplier that returns random byte values.
     * Each invocation returns a new random byte value between -128 and 127 (inclusive).
     * The randomness is provided by {@code Util.RAND_BYTE.nextInt()}.
     */
    ByteSupplier RANDOM = () -> (byte) Util.RAND_BYTE.nextInt();

    /**
     * Gets a byte result.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteSupplier counter = new ByteSupplier() {
     *     private byte count = 0;
     *     public byte getAsByte() { return count++; }
     * };
     * byte value = counter.getAsByte();  // Returns 0, then 1, 2, ...
     * }</pre>
     *
     * @return a byte value
     */
    @Override
    byte getAsByte();
}
