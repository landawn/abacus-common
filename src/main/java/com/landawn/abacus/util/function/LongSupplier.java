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
 * Represents a supplier of {@code long}-valued results. This is the {@code long}-producing
 * primitive specialization of {@link java.util.function.Supplier}.
 *
 * <p>There is no requirement that a new or distinct result be returned each time the supplier is invoked.
 *
 * <p>This interface extends both {@link java.util.function.LongSupplier} and
 * {@link com.landawn.abacus.util.Throwables.LongSupplier}, providing compatibility with the standard Java
 * functional interfaces while also supporting the Throwables framework.
 *
 * <p>This is a functional interface whose functional method is {@link #getAsLong()}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @see java.util.function.LongSupplier
 * @see java.util.function.Supplier
 */
@FunctionalInterface
public interface LongSupplier extends Throwables.LongSupplier<RuntimeException>, java.util.function.LongSupplier { //NOSONAR
    /**
     * A supplier that always returns zero (0L).
     *
     * <p>This is useful for providing a default or initial value in contexts requiring a long supplier.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongSupplier zeroSupplier = LongSupplier.ZERO;
     * long value = zeroSupplier.getAsLong();   // returns 0L
     * }</pre>
     *
     */
    LongSupplier ZERO = () -> 0;
    /**
     * A supplier that returns random long values.
     *
     * <p>This supplier uses an internal random number generator to draw values uniformly across the
     * entire range of {@code long}. Successive values are not guaranteed to be distinct.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongSupplier randomSupplier = LongSupplier.RANDOM;
     * long value1 = randomSupplier.getAsLong();   // returns a random long
     * long value2 = randomSupplier.getAsLong();   // returns another random long
     * }</pre>
     *
     */
    LongSupplier RANDOM = Util.RAND_LONG::nextLong;

    /**
     * Gets a result as a long value.
     *
     * <p>This method should be implemented to provide the long value when called.
     * The implementation may return the same value or different values on subsequent calls,
     * depending on the specific supplier implementation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongSupplier timestamp = System::currentTimeMillis;
     * long time = timestamp.getAsLong();   // Returns current time in milliseconds
     * }</pre>
     *
     * @return a long value
     */
    @Override
    long getAsLong();

    /**
     * Returns this object as a {@link Throwables.LongSupplier} view.
     *
     * <p>The returned object has the same behavior as this one. This method does not translate
     * exceptions or make the original implementation capable of throwing new checked exceptions; the
     * exception type parameter is for target-type compatibility with APIs that accept {@code Throwables.LongSupplier}.
     *
     * @param <E> the target exception type for compatibility with {@code Throwables.LongSupplier}
     * @return a {@link Throwables.LongSupplier} view of this object
     */
    default <E extends Throwable> Throwables.LongSupplier<E> toThrowable() {
        return (Throwables.LongSupplier<E>) this;
    }
}
