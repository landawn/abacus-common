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
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.cs;

/**
 * A functional interface that represents a function that accepts two short-valued
 * arguments and produces a result. This is the primitive type specialization of
 * {@link java.util.function.BiFunction} for {@code short} arguments.
 *
 * <p>The JDK itself has no two-argument, primitive-to-object function specialization at all
 * (only single-argument forms like {@link java.util.function.IntFunction}); this project fills that
 * gap for all eight primitive types, and this interface is the {@code short} member of that family,
 * offering better type safety and performance than the boxed generic {@code BiFunction<Short, Short, R>}.
 *
 * <p>This is a functional interface whose functional method is {@link #apply(short, short)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <R> the type of the result of the function
 * @see java.util.function.BiFunction
 * @see IntBiFunction
 * @see LongBiFunction
 * @see DoubleBiFunction
 */
@FunctionalInterface
public interface ShortBiFunction<R> extends Throwables.ShortBiFunction<R, RuntimeException> { //NOSONAR
    /**
     * Applies this function to the given short arguments.
     *
     * <p>This method takes two short values as input and produces a result of type R.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortBiFunction<Integer> sum = (a, b) -> (int) (a + b);
     * ShortBiFunction<String> formatter = (x, y) ->
     *     String.format("Point(%d, %d)", x, y);
     * ShortBiFunction<Boolean> inRange = (value, max) ->
     *     value >= 0 && value <= max;
     *
     * Integer total = sum.apply((short) 100, (short) 200);      // Returns 300
     * String point = formatter.apply((short) 10, (short) 20);   // Returns "Point(10, 20)"
     * Boolean valid = inRange.apply((short) 50, (short) 100);   // Returns true
     * }</pre>
     *
     * @param a the first function argument
     * @param b the second function argument
     * @return the function result
     */
    @Override
    R apply(short a, short b);

    /**
     * Returns a composed function that first applies this function to its input,
     * and then applies the {@code after} function to the result. If evaluation of
     * either function throws an exception, it is relayed to the caller of the
     * composed function.
     *
     * <p>This method enables function composition, allowing you to chain multiple
     * transformations together. The output of this function becomes the input to
     * the {@code after} function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortBiFunction<Integer> multiply = (a, b) -> (int) (a * b);
     * Function<Integer, String> addPrefix = result ->
     *     "Result: " + result;
     *
     * ShortBiFunction<String> multiplyAndFormat =
     *     multiply.andThen(addPrefix);
     *
     * String result = multiplyAndFormat.apply((short) 5, (short) 6);
     * // Returns "Result: 30"
     *
     * // More complex example
     * ShortBiFunction<Point> createPoint = (x, y) -> new Point(x, y);
     * Function<Point, Double> calculateDistance = point ->
     *     Math.sqrt(point.x * point.x + point.y * point.y);
     *
     * ShortBiFunction<Double> distanceFromOrigin =
     *     createPoint.andThen(calculateDistance);
     *
     * Double distance = distanceFromOrigin.apply((short) 3, (short) 4);
     * // Returns 5.0
     * }</pre>
     *
     * @param <V> the type of output of the {@code after} function, and of the
     *           composed function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then
     *         applies the {@code after} function
     * @throws IllegalArgumentException if {@code after} is null
     */
    default <V> ShortBiFunction<V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        N.checkArgNotNull(after, cs.after);
        return (a, b) -> after.apply(apply(a, b));
    }

    /**
     * Returns this object as a {@link Throwables.ShortBiFunction} view.
     *
     * <p>The returned object has the same behavior as this one. This method does not translate
     * exceptions or make the original implementation capable of throwing new checked exceptions; the
     * exception type parameter is for target-type compatibility with APIs that accept {@code Throwables.ShortBiFunction}.
     *
     * @param <E> the target exception type for compatibility with {@code Throwables.ShortBiFunction}
     * @return a {@link Throwables.ShortBiFunction} view of this object
     */
    default <E extends Throwable> Throwables.ShortBiFunction<R, E> toThrowable() {
        return (Throwables.ShortBiFunction<R, E>) this;
    }
}
