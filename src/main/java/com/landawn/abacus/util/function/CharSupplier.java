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
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface CharSupplier extends Throwables.CharSupplier<RuntimeException> { //NOSONAR
    /**
     * A supplier that always returns the {@code null} character ('\0').
     * This is useful as a default value or for initialization purposes.
     */
    CharSupplier ZERO = () -> 0;
    /**
     * A supplier that returns random char values.
     * Each invocation returns a new random char value within the valid Unicode range (0 to 65535).
     * This supplier is useful for generating random characters for testing, simulation, or data generation purposes.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char randomChar1 = CharSupplier.RANDOM.getAsChar();   // Returns a random char, e.g., 'k'
     * char randomChar2 = CharSupplier.RANDOM.getAsChar();   // Returns another random char, e.g., '&'
     * }</pre>
     */
    CharSupplier RANDOM = () -> (char) Util.RAND_CHAR.nextInt(Util.CHAR_MOD);

    /**
     * Gets a char result.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharSupplier letterA = () -> 'A';
     * char value = letterA.getAsChar();   // Returns 'A'
     * }</pre>
     *
     * @return a char value
     */
    @Override
    char getAsChar();
}
