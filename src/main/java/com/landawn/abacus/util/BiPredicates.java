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

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.SequentialOnly;
import com.landawn.abacus.annotation.Stateful;
import com.landawn.abacus.util.function.BiPredicate;
import com.landawn.abacus.util.function.IntBiObjPredicate;

/**
 * Utility class providing various BiPredicate implementations and factory methods.
 * This class contains predefined BiPredicates and methods for creating indexed BiPredicates.
 *
 * <p>This class is a top-level sibling of {@link Fn} (formerly nested as {@code Fn.BiPredicates}),
 * not a nested type. Use {@link Fn} for the general functional-interface factory and {@link Fnn}
 * for {@link Throwables} variants that can declare checked exceptions. For one-argument predicates
 * see {@link Predicates}; for three-argument predicates see {@link TriPredicates}.</p>
 *
 * @see Fn
 * @see Fnn
 * @see Predicates
 * @see TriPredicates
 * @see BiConsumers
 * @see BiFunctions
 */
public final class BiPredicates {

    /** The Constant ALWAYS_TRUE. */
    @SuppressWarnings("rawtypes")
    private static final BiPredicate ALWAYS_TRUE = (t, u) -> true;

    /** The Constant ALWAYS_FALSE. */
    @SuppressWarnings("rawtypes")
    private static final BiPredicate ALWAYS_FALSE = (t, u) -> false;

    /** The Constant EQUAL. */
    @SuppressWarnings("rawtypes")
    static final BiPredicate EQUAL = N::equals;

    /** The Constant NOT_EQUAL. */
    @SuppressWarnings("rawtypes")
    static final BiPredicate NOT_EQUAL = (t, u) -> !N.equals(t, u);

    /** The Constant GREATER_THAN. */
    @SuppressWarnings("rawtypes")
    static final BiPredicate<? extends Comparable, ? extends Comparable> GREATER_THAN = (t, u) -> N.compare(t, u) > 0;

    /** The Constant GREATER_THAN_OR_EQUAL. */
    @SuppressWarnings("rawtypes")
    static final BiPredicate<? extends Comparable, ? extends Comparable> GREATER_THAN_OR_EQUAL = (t, u) -> N.compare(t, u) >= 0;

    /** The Constant LESS_THAN. */
    @SuppressWarnings("rawtypes")
    static final BiPredicate<? extends Comparable, ? extends Comparable> LESS_THAN = (t, u) -> N.compare(t, u) < 0;

    /** The Constant LESS_THAN_OR_EQUAL. */
    @SuppressWarnings("rawtypes")
    static final BiPredicate<? extends Comparable, ? extends Comparable> LESS_THAN_OR_EQUAL = (t, u) -> N.compare(t, u) <= 0;

    private BiPredicates() {
    }

    /**
     * Returns a BiPredicate that always returns {@code true} regardless of input.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiPredicates.alwaysTrue().test("a", "b");          // returns true
     * BiPredicates.alwaysTrue().test(null, null);        // returns true
     * BiPredicates.alwaysTrue().test(new Object(), 1);   // returns true
     * }</pre>
     *
     * @param <T> the type of the first argument to the predicate
     * @param <U> the type of the second argument to the predicate
     * @return a BiPredicate that always returns true
     */
    public static <T, U> BiPredicate<T, U> alwaysTrue() {
        return ALWAYS_TRUE;
    }

    /**
     * Returns a BiPredicate that always returns {@code false} regardless of input.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiPredicates.alwaysFalse().test("a", "b");          // returns false
     * BiPredicates.alwaysFalse().test(null, null);        // returns false
     * BiPredicates.alwaysFalse().test(new Object(), 1);   // returns false
     * }</pre>
     *
     * @param <T> the type of the first argument to the predicate
     * @param <U> the type of the second argument to the predicate
     * @return a BiPredicate that always returns false
     */
    public static <T, U> BiPredicate<T, U> alwaysFalse() {
        return ALWAYS_FALSE;
    }

    /**
     * Returns a stateful BiPredicate that tests elements based on their index position.
     * The predicate maintains an internal counter that increments with each test.
     * This method is marked as Beta, SequentialOnly, and Stateful, indicating it should not be saved, cached for reuse, or used in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiPredicates.indexed((i, t, u) -> i < 5).test("a", "b");  // returns true (index 0 < 5)
     * }</pre>
     *
     * <p>The returned callback uses indices from zero through {@link Integer#MAX_VALUE}. Later calls
     * throw {@link ArithmeticException} without invoking user code. An invocation consumes its index
     * even when user code throws.</p>
     *
     * @param <T> the type of the first argument to the predicate
     * @param <U> the type of the second argument to the predicate
     * @param predicate the IntBiObjPredicate that accepts an index and two elements for testing
     * @return a stateful BiPredicate that applies the given IntBiObjPredicate with an incrementing index
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     */
    @Beta
    @SequentialOnly
    @Stateful
    public static <T, U> BiPredicate<T, U> indexed(final IntBiObjPredicate<T, U> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.predicate);

        return new BiPredicate<>() {
            private long idx;

            /**
             * {@inheritDoc}
             * @throws ArithmeticException if all nonnegative {@code int} indices have already been used by prior invocations
             */
            @Override
            public boolean test(final T t, final U u) throws ArithmeticException {
                // Keep exhaustion representable and reject before invoking user code.
                if (idx > Integer.MAX_VALUE) {
                    throw new ArithmeticException("Index exceeds Integer.MAX_VALUE");
                }

                return predicate.test((int) idx++, t, u);
            }
        };
    }
}
