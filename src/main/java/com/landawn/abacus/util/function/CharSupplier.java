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
 * Represents a supplier of char-valued results. This is the char-producing
 * primitive specialization of {@link java.util.function.Supplier}.
 * 
 * <p>There is no requirement that a new or distinct result be returned each time the supplier is invoked.
 * 
 * <p>This is a functional interface whose functional method is {@link #getAsChar()}.
 * 
 * @see java.util.function.Supplier
 */
@FunctionalInterface
public interface CharSupplier extends Throwables.CharSupplier<RuntimeException> { //NOSONAR

    /**
     * A supplier that always returns the null character ('\0').
     * This is useful as a default value or for initialization purposes.
     */
    CharSupplier ZERO = () -> 0;

    /**
     * A supplier that returns random char values.
     * Each invocation returns a new random char value within the valid Unicode range.
     * The randomness is provided by {@code Util.RAND_CHAR.nextInt() % Util.CHAR_MOD}.
     */
    CharSupplier RANDOM = () -> (char) Math.abs(Util.RAND_CHAR.nextInt() % Util.CHAR_MOD);

    /**
     * Gets a char result.
     *
     * @return a char value
     */
    @Override
    char getAsChar();
}