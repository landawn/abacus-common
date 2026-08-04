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
 * Package-private helpers for random primitive values used by supplier defaults in this package.
 *
 * <p>Each primitive category has its own dedicated {@link SecureRandom} instance.
 * {@code SecureRandom} is thread-safe; separate instances avoid sharing one generator across
 * all categories, though concurrent callers of the same instance may still contend.
 */
final class Util {
    private Util() {
        // Utility class - prevent instantiation
    }

    /** Shared {@link SecureRandom} for {@code boolean} supplier defaults. */
    static final Random RAND_BOOLEAN = new SecureRandom();
    /** Shared {@link SecureRandom} for {@code char} supplier defaults. */
    static final Random RAND_CHAR = new SecureRandom();
    /** Shared {@link SecureRandom} for {@code byte} supplier defaults. */
    static final Random RAND_BYTE = new SecureRandom();
    /** Shared {@link SecureRandom} for {@code short} supplier defaults. */
    static final Random RAND_SHORT = new SecureRandom();
    /** Shared {@link SecureRandom} for {@code int} supplier defaults. */
    static final Random RAND_INT = new SecureRandom();
    /** Shared {@link SecureRandom} for {@code long} supplier defaults. */
    static final Random RAND_LONG = new SecureRandom();
    /** Shared {@link SecureRandom} for {@code float} supplier defaults. */
    static final Random RAND_FLOAT = new SecureRandom();
    /** Shared {@link SecureRandom} for {@code double} supplier defaults. */
    static final Random RAND_DOUBLE = new SecureRandom();
    /** Bound for random {@code char} generation: {@code Character.MAX_VALUE + 1}. */
    static final int CHAR_MOD = Character.MAX_VALUE + 1;
}
