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
 * Represents a supplier of short-valued results. This is the short-producing
 * primitive specialization of {@link java.util.function.Supplier}.
 *
 * <p>There is no requirement that a new or distinct result be returned each time the supplier is invoked.
 *
 * <p>This is a functional interface whose functional method is {@link #getAsShort()}.
 *
 * @see java.util.function.Supplier
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface ShortSupplier extends Throwables.ShortSupplier<RuntimeException> { //NOSONAR
    /**
     * A supplier that always returns zero (0).
     * This supplier returns the same value on every invocation.
     */
    ShortSupplier ZERO = () -> 0;
    /**
     * A supplier that returns random short values.
     * Each invocation returns a different pseudo-random short value within the full range of short values.
     * The values are generated using {@code Util.RAND_SHORT.nextInt()}.
     */
    ShortSupplier RANDOM = () -> (short) Util.RAND_SHORT.nextInt();

    /**
     * Gets a result as a short value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortSupplier zero = ShortSupplier.ZERO;
     * short value1 = zero.getAsShort();   // returns 0
     *
     * ShortSupplier random = ShortSupplier.RANDOM;
     * short value2 = random.getAsShort();   // returns a random short value
     *
     * ShortSupplier counter = new ShortSupplier() {
     *     private short count = 0;
     *     public short getAsShort() { return count++; }
     * };
     * short value3 = counter.getAsShort();   // returns 0
     * short value4 = counter.getAsShort();   // returns 1
     * }</pre>
     *
     * @return a short value
     */
    @Override
    short getAsShort();
}
