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

/**
 * Package-private helpers for random primitive values used by supplier defaults in this package.
 *
 * <p>The {@code RANDOM} supplier constants draw their values from
 * {@link java.util.concurrent.ThreadLocalRandom#current()} on every call, so concurrent callers do not
 * share (or contend on) one generator.
 */
final class Util {
    private Util() {
        // Utility class - prevent instantiation
    }

    /** Bound for random {@code char} generation: {@code Character.MAX_VALUE + 1}. */
    static final int CHAR_MOD = Character.MAX_VALUE + 1;
}
