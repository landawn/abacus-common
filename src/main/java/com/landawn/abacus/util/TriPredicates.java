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

import com.landawn.abacus.util.function.TriPredicate;

/**
 * Utility class providing various TriPredicate implementations and factory methods.
 * This class contains predefined TriPredicates for common operations.
 */
public final class TriPredicates {

    /** The Constant ALWAYS_TRUE. */
    @SuppressWarnings("rawtypes")
    private static final TriPredicate ALWAYS_TRUE = (a, b, c) -> true;

    /** The Constant ALWAYS_FALSE. */
    @SuppressWarnings({ "rawtypes" })
    private static final TriPredicate ALWAYS_FALSE = (a, b, c) -> false;

    private TriPredicates() {
    }

    /**
     * Returns a TriPredicate that always returns {@code true} regardless of input.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TriPredicates.alwaysTrue().test("a", "b", "c");           // returns true
     * TriPredicates.alwaysTrue().test(null, null, null);        // returns true
     * TriPredicates.alwaysTrue().test(new Object(), 1, true);   // returns true
     * }</pre>
     *
     * @param <A> the type of the first argument to the predicate
     * @param <B> the type of the second argument to the predicate
     * @param <C> the type of the third argument to the predicate
     * @return a TriPredicate that always returns true
     */
    public static <A, B, C> TriPredicate<A, B, C> alwaysTrue() {
        return ALWAYS_TRUE;
    }

    /**
     * Returns a TriPredicate that always returns {@code false} regardless of input.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TriPredicates.alwaysFalse().test("a", "b", "c");           // returns false
     * TriPredicates.alwaysFalse().test(null, null, null);        // returns false
     * TriPredicates.alwaysFalse().test(new Object(), 1, true);   // returns false
     * }</pre>
     *
     * @param <A> the type of the first argument to the predicate
     * @param <B> the type of the second argument to the predicate
     * @param <C> the type of the third argument to the predicate
     * @return a TriPredicate that always returns false
     */
    public static <A, B, C> TriPredicate<A, B, C> alwaysFalse() {
        return ALWAYS_FALSE;
    }

}
