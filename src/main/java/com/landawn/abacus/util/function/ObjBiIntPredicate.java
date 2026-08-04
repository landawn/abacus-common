/*
 * Copyright (C) 2024 HaiYang Li
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

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.cs;

/**
 * Represents a predicate (boolean-valued function) of an object-valued argument and two int-valued arguments.
 * This is a three-arity specialization of {@code Predicate}.
 *
 * <p>This is a functional interface whose functional method is {@link #test(Object, int, int)}.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * ObjBiIntPredicate<String> isSubstringValid = (str, start, end) ->
 *     start >= 0 && end <= str.length() && start < end;
 * boolean valid = isSubstringValid.test("Hello", 0, 5);   // returns true
 *
 * ObjBiIntPredicate<int[][]> isValidPosition = (matrix, row, col) ->
 *     row >= 0 && row < matrix.length && matrix[row] != null &&
 *     col >= 0 && col < matrix[row].length;
 *
 * ObjBiIntPredicate<List<?>> isRangeWithinBounds = (list, from, to) ->
 *     from >= 0 && to <= list.size() && from <= to;
 * }</pre>
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <T> the type of the object argument to the predicate
 *
 * @see java.util.function.Predicate
 * @see java.util.function.BiPredicate
 */
@FunctionalInterface
public interface ObjBiIntPredicate<T> extends Throwables.ObjBiIntPredicate<T, RuntimeException> { // NOSONAR
    /**
     * Evaluates this predicate on the given arguments.
     *
     * @param t the object input argument
     * @param i the first int input argument
     * @param j the second int input argument
     * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
     */
    @Override
    boolean test(T t, int i, int j);

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjBiIntPredicate<String> isInBounds = (str, start, end) ->
     *     start >= 0 && end <= str.length();
     * ObjBiIntPredicate<String> isOutOfBounds = isInBounds.negate();
     * isOutOfBounds.test("Hello", -1, 5);   // returns true
     * }</pre>
     *
     * @return a predicate that represents the logical negation of this predicate
     */
    default ObjBiIntPredicate<T> negate() {
        return (t, i, j) -> !test(t, i, j);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical
     * AND of this predicate and another.
     *
     * <p>When evaluating the composed predicate, if this predicate is {@code false},
     * then the {@code other} predicate is not evaluated.
     *
     * <p>Any exceptions thrown during evaluation of either predicate are relayed
     * to the caller; if evaluation of this predicate throws an exception, the
     * {@code other} predicate will not be evaluated.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjBiIntPredicate<String> isValidRange = (str, start, end) ->
     *     start >= 0 && end <= str.length() && start <= end;
     * ObjBiIntPredicate<String> isNonEmpty = (str, start, end) ->
     *     end > start;
     *
     * ObjBiIntPredicate<String> combined = isValidRange.and(isNonEmpty);
     * combined.test("Hello", 2, 2);   // returns false (not non-empty)
     * }</pre>
     *
     * @param other a predicate that will be logically-ANDed with this predicate.
     * @return a composed predicate that represents the short-circuiting logical
     *         AND of this predicate and the {@code other} predicate
     * @throws IllegalArgumentException if {@code other} is {@code null}.
     */
    default ObjBiIntPredicate<T> and(final ObjBiIntPredicate<? super T> other) throws IllegalArgumentException {
        N.checkArgNotNull(other, cs.other);

        return (t, i, j) -> test(t, i, j) && other.test(t, i, j);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical
     * OR of this predicate and another.
     *
     * <p>When evaluating the composed predicate, if this predicate is {@code true},
     * then the {@code other} predicate is not evaluated.
     *
     * <p>Any exceptions thrown during evaluation of either predicate are relayed
     * to the caller; if evaluation of this predicate throws an exception, the
     * {@code other} predicate will not be evaluated.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjBiIntPredicate<List<?>> isEmpty = (list, from, to) ->
     *     list.isEmpty();
     * ObjBiIntPredicate<List<?>> isFullRange = (list, from, to) ->
     *     from == 0 && to == list.size();
     *
     * ObjBiIntPredicate<List<?>> specialCase = isEmpty.or(isFullRange);
     * // Returns true if list is empty OR if range covers entire list
     * }</pre>
     *
     * @param other a predicate that will be logically-ORed with this predicate.
     * @return a composed predicate that represents the short-circuiting logical
     *         OR of this predicate and the {@code other} predicate
     * @throws IllegalArgumentException if {@code other} is {@code null}.
     */
    default ObjBiIntPredicate<T> or(final ObjBiIntPredicate<? super T> other) throws IllegalArgumentException {
        N.checkArgNotNull(other, cs.other);

        return (t, i, j) -> test(t, i, j) || other.test(t, i, j);
    }

    /**
     * Returns this object as a {@link Throwables.ObjBiIntPredicate} view.
     *
     * <p>The returned object has the same behavior as this one. This method does not translate
     * exceptions or make the original implementation capable of throwing new checked exceptions; the
     * exception type parameter is for target-type compatibility with APIs that accept {@code Throwables.ObjBiIntPredicate}.
     *
     * @param <E> the target exception type for compatibility with {@code Throwables.ObjBiIntPredicate}
     * @return a {@link Throwables.ObjBiIntPredicate} view of this object
     */
    default <E extends Throwable> Throwables.ObjBiIntPredicate<T, E> toThrowable() {
        return (Throwables.ObjBiIntPredicate<T, E>) this;
    }
}
