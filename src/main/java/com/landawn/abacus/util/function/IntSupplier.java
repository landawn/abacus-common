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
 * Represents a supplier of {@code int}-valued results. This is the {@code int}-producing
 * primitive specialization of {@link java.util.function.Supplier}.
 *
 * <p>This interface extends both {@link java.util.function.IntSupplier} and
 * {@link Throwables.IntSupplier}, providing compatibility with the standard Java functional
 * interfaces while also supporting the Throwables framework.
 *
 * <p>There is no requirement that a new or distinct result be returned each time the supplier is invoked.
 *
 * <p>This is a functional interface whose functional method is {@link #getAsInt()}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @see java.util.function.Supplier
 */
@FunctionalInterface
public interface IntSupplier extends Throwables.IntSupplier<RuntimeException>, java.util.function.IntSupplier { //NOSONAR
    /**
     * A supplier that always returns zero.
     */
    IntSupplier ZERO = () -> 0;
    /**
     * A supplier that draws random {@code int} values from the internal random number generator.
     * Successive values are not guaranteed to be distinct.
     */
    IntSupplier RANDOM = Util.RAND_INT::nextInt;

    /**
     * Gets an int result.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntSupplier counter = new AtomicInteger()::incrementAndGet;
     * int value = counter.getAsInt();   // Returns 1, then 2, then 3, etc.
     * }</pre>
     *
     * @return an {@code int} value
     */
    @Override
    int getAsInt();

    /**
     * Returns this object as a {@link Throwables.IntSupplier} view.
     *
     * <p>The returned object has the same behavior as this one. This method does not translate
     * exceptions or make the original implementation capable of throwing new checked exceptions; the
     * exception type parameter is for target-type compatibility with APIs that accept {@code Throwables.IntSupplier}.
     *
     * @param <E> the target exception type for compatibility with {@code Throwables.IntSupplier}
     * @return a {@link Throwables.IntSupplier} view of this object
     */
    default <E extends Throwable> Throwables.IntSupplier<E> toThrowable() {
        return (Throwables.IntSupplier<E>) this;
    }
}
