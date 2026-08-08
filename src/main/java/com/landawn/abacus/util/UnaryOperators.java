/*
 * Copyright (c) 2026, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.util;

import com.landawn.abacus.util.function.UnaryOperator;

/**
 * Utility class providing various UnaryOperator implementations and factory methods.
 * This class contains the identity operator.
 */
public final class UnaryOperators {

    /** The Constant IDENTITY. */
    @SuppressWarnings("rawtypes")
    private static final UnaryOperator IDENTITY = t -> t;

    private UnaryOperators() {
    }

    /**
     * Returns a UnaryOperator that always returns its input argument unchanged.
     * This is the identity function for UnaryOperator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * UnaryOperators.identity().apply("hello");   // returns "hello"
     * UnaryOperators.identity().apply(42);        // returns 42
     * UnaryOperators.identity().apply(null);      // returns null
     * }</pre>
     *
     * @param <T> the type of the operand and result of the operator
     * @return a UnaryOperator that returns its input argument
     */
    public static <T> UnaryOperator<T> identity() {
        return IDENTITY;
    }
}
