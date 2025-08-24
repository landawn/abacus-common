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

import java.security.SecureRandom;
import java.util.Random;

/**
 * Internal utility class providing secure random number generators for different primitive types.
 * This class is used by functional interfaces and suppliers to generate random values in a
 * cryptographically secure manner.
 * 
 * <p>Each primitive type has its own dedicated {@link SecureRandom} instance to ensure
 * thread-safe random value generation without contention. Using separate instances also
 * allows for better performance in multi-threaded environments.</p>
 * 
 * <p>The random generators in this class are used by:</p>
 * <ul>
 *   <li>Default supplier implementations for primitive types</li>
 *   <li>Random value generators in functional interfaces</li>
 *   <li>Test data generation utilities</li>
 * </ul>
 * 
 * <p><strong>Note:</strong> This class uses {@link SecureRandom} rather than {@link Random}
 * to ensure cryptographically strong random values, which is important for:</p>
 * <ul>
 *   <li>Security-sensitive applications</li>
 *   <li>Avoiding predictable patterns in generated values</li>
 *   <li>Better distribution of random values</li>
 * </ul>
 * 
 */
final class Util {
    private Util() {
        // singleton.
    }

    /**
     * Secure random generator for boolean values.
     * Used by BooleanSupplier implementations.
     */
    static final Random RAND_BOOLEAN = new SecureRandom();

    /**
     * Secure random generator for char values.
     * Used by CharSupplier implementations.
     */
    static final Random RAND_CHAR = new SecureRandom();

    /**
     * Secure random generator for byte values.
     * Used by ByteSupplier implementations.
     */
    static final Random RAND_BYTE = new SecureRandom();

    /**
     * Secure random generator for short values.
     * Used by ShortSupplier implementations.
     */
    static final Random RAND_SHORT = new SecureRandom();

    /**
     * Secure random generator for int values.
     * Used by IntSupplier implementations.
     */
    static final Random RAND_INT = new SecureRandom();

    /**
     * Secure random generator for long values.
     * Used by LongSupplier implementations.
     */
    static final Random RAND_LONG = new SecureRandom();

    /**
     * Secure random generator for float values.
     * Used by FloatSupplier implementations.
     */
    static final Random RAND_FLOAT = new SecureRandom();

    /**
     * Secure random generator for double values.
     * Used by DoubleSupplier implementations.
     */
    static final Random RAND_DOUBLE = new SecureRandom();

    /**
     * Modulo value for generating random char values.
     * Equals to Character.MAX_VALUE + 1 (65536).
     * Used to ensure random char generation stays within valid char range.
     */
    static final int CHAR_MOD = Character.MAX_VALUE + 1;
}
