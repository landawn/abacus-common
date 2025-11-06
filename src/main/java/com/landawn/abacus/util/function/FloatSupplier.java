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
 * Represents a supplier of float-valued results. This is a functional interface
 * whose functional method is {@link #getAsFloat()}.
 *
 * <p>This is a primitive type specialization of {@link java.util.function.Supplier} for {@code float}.</p>
 *
 * <p>There is no requirement that a new or distinct result be returned each
 * time the supplier is invoked.</p>
 *
 * @see java.util.function.Supplier
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface FloatSupplier extends Throwables.FloatSupplier<RuntimeException> { //NOSONAR
    /**
     * A supplier that always returns zero (0.0f).
     */
    FloatSupplier ZERO = () -> 0;
    /**
     * A supplier that returns random float values between 0.0 (inclusive) and 1.0 (exclusive).
     * The random values are generated using {@code Util.RAND_FLOAT.nextFloat()}.
     */
    FloatSupplier RANDOM = Util.RAND_FLOAT::nextFloat;

    /**
     * Gets a float result.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatSupplier pi = () -> 3.14159f;
     * float value = pi.getAsFloat(); // Returns 3.14159f
     * }</pre>
     *
     * @return a float value
     */
    @Override
    float getAsFloat();

    /**
     * Converts this {@code FloatSupplier} to a {@code Throwables.FloatSupplier} that can throw a checked exception.
     * This method provides a way to use this supplier in contexts that require explicit exception handling.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatSupplier supplier = () -> { ... };
     * var throwableSupplier = supplier.toThrowable();
     * // Can now be used in contexts that handle checked exceptions
     * }</pre>
     *
     * @param <E> the type of exception that the returned supplier can throw
     * @return a {@code Throwables.FloatSupplier} view of this supplier that can throw exceptions of type {@code E}
     */
    default <E extends Throwable> Throwables.FloatSupplier<E> toThrowable() {
        return (Throwables.FloatSupplier<E>) this;
    }

}
