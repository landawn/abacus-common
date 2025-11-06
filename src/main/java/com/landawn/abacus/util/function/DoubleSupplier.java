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
 * Represents a supplier of double-valued results. This is the double-producing primitive specialization of {@link java.util.function.Supplier}.
 *
 * <p>There is no requirement that a new or distinct result be returned each time the supplier is invoked.
 *
 * <p>This is a functional interface whose functional method is {@link #getAsDouble()}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @see java.util.function.Supplier
 * @see java.util.function.DoubleSupplier
 */
@FunctionalInterface
public interface DoubleSupplier extends Throwables.DoubleSupplier<RuntimeException>, java.util.function.DoubleSupplier { //NOSONAR
    /**
     * A {@code DoubleSupplier} which always returns zero.
     */
    DoubleSupplier ZERO = () -> 0;
    /**
     * A {@code DoubleSupplier} which returns a random double value between 0.0 (inclusive) and 1.0 (exclusive).
     * Each invocation returns a new pseudorandom value.
     */
    DoubleSupplier RANDOM = Util.RAND_DOUBLE::nextDouble;

    /**
     * Gets a double result.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleSupplier random = Math::random;
     * double value = random.getAsDouble(); // Returns random value between 0.0 and 1.0
     * }</pre>
     *
     * @return a double value
     */
    @Override
    double getAsDouble();

    /**
     * Converts this {@code DoubleSupplier} to a {@code Throwables.DoubleSupplier} that can throw a checked exception.
     * This method provides a way to use this supplier in contexts that require explicit exception handling.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleSupplier supplier = () -> { ... };
     * var throwableSupplier = supplier.toThrowable();
     * // Can now be used in contexts that handle checked exceptions
     * }</pre>
     *
     * @param <E> the type of exception that the returned supplier can throw
     * @return a {@code Throwables.DoubleSupplier} view of this supplier that can throw exceptions of type {@code E}
     */
    default <E extends Throwable> Throwables.DoubleSupplier<E> toThrowable() {
        return (Throwables.DoubleSupplier<E>) this;
    }

}
