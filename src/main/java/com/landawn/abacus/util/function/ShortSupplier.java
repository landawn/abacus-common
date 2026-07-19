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
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @see java.util.function.Supplier
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
     * Each invocation draws a random short value from the full range of short values using an
     * internal random number generator. Successive values are not guaranteed to be distinct.
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

    /**
     * Returns this object as a {@link Throwables.ShortSupplier} view.
     *
     * <p>The returned object has the same behavior as this one. This method does not translate
     * exceptions or make the original implementation capable of throwing new checked exceptions; the
     * exception type parameter is for target-type compatibility with APIs that accept {@code Throwables.ShortSupplier}.
     *
     * @param <E> the target exception type for compatibility with {@code Throwables.ShortSupplier}
     * @return a {@link Throwables.ShortSupplier} view of this object
     */
    default <E extends Throwable> Throwables.ShortSupplier<E> toThrowable() {
        return (Throwables.ShortSupplier<E>) this;
    }
}
