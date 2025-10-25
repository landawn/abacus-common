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
 * @since 1.0
 * @see java.util.function.Supplier
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
     * @return a float value
     */
    @Override
    float getAsFloat();
}