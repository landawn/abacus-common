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
 * Represents a function that accepts a long-valued argument and produces a
 * result. This is the {@code long}-consuming primitive specialization for
 * {@link java.util.function.Function}.
 *
 * <p>This interface extends both {@link Throwables.LongFunction} with
 * {@link RuntimeException} and {@link java.util.function.LongFunction},
 * providing compatibility with the Java standard library while supporting the
 * abacus-common framework's exception handling capabilities.
 *
 * <p>This is a functional interface whose functional method is
 * {@link #apply(long)}.
 *
 * <p>Refer to JDK API documentation at:
 * <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <R> the type of the result of the function
 *
 * @see java.util.function.Function
 * @see java.util.function.LongFunction
 */
@FunctionalInterface
public interface LongFunction<R> extends Throwables.LongFunction<R, RuntimeException>, java.util.function.LongFunction<R> { //NOSONAR
    /**
     * A function that boxes a primitive long value into a Long object.
     * This is useful when you need to convert primitive long values to their
     * wrapper type for use with APIs that require objects.
     */
    LongFunction<Long> BOX = value -> value;

    /**
     * Applies this function to the given argument.
     *
     * <p>The function transforms a long value into a result of type R.
     * Common use cases include:
     * <ul>
     *   <li>Converting long values to other types (e.g., String, Date)</li>
     *   <li>Creating objects from long values (e.g., timestamps to Date objects)</li>
     *   <li>Looking up values in a map or cache using long keys</li>
     *   <li>Transforming long IDs to entity objects</li>
     *   <li>Formatting long values for display</li>
     *   <li>Computing derived values from long inputs</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongFunction<String> toString = value -> "Value: " + value;
     * String result = toString.apply(42L);  // Returns "Value: 42"
     * }</pre>
     *
     * @param value the function argument, a long value to be transformed
     * @return the function result of type R
     */
    @Override
    R apply(long value);

    /**
     * Returns a composed function that first applies this function to
     * its input, and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * <p>This method enables function composition, allowing you to chain
     * transformations where the output of this function becomes the input
     * of the next function. This is useful for building complex transformations
     * from simpler ones.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongFunction<Date> toDate = timestamp -> new Date(timestamp);
     * LongFunction<String> toDateString = toDate.andThen(Date::toString);
     * String result = toDateString.apply(System.currentTimeMillis());
     * // Converts timestamp to Date, then to String
     * }</pre>
     *
     * @param <V> the type of output of the {@code after} function, and of the
     *           composed function
     * @param after the function to apply after this function is applied.
     *              Must not be {@code null}.
     * @return a composed function that first applies this function and then
     *         applies the {@code after} function
     */
    default <V> LongFunction<V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return t -> after.apply(apply(t));
    }

    /**
     * Returns a function that always returns its input argument.
     *
     * <p>This identity function is useful in several scenarios:
     * <ul>
     *   <li>As a default transformation when no change is needed</li>
     *   <li>In stream operations where the long values should pass through unchanged</li>
     *   <li>For testing or as a placeholder in function compositions</li>
     *   <li>When conditionally applying transformations</li>
     * </ul>
     *
     * <p>Note that this returns a Long object (boxed value), not a primitive long.
     * For operations that need to maintain primitive types, consider using
     * {@link java.util.function.LongUnaryOperator} instead.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongFunction<Long> identity = LongFunction.identity();
     * Long result = identity.apply(42L);  // Returns 42L (boxed)
     * }</pre>
     *
     * @return a function that always returns its input argument as a Long object
     */
    static LongFunction<Long> identity() {
        return t -> t;
    }
}
