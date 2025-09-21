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

import com.landawn.abacus.util.Throwables;

/**
 * Represents a predicate (boolean-valued function) of an object-valued argument and two int-valued arguments.
 * This is a three-arity specialization of {@code Predicate}.
 * 
 * <p>This is a functional interface whose functional method is {@link #test(Object, int, int)}.
 * 
 * <p>The interface extends {@code Throwables.ObjBiIntPredicate} with {@code RuntimeException} as the exception type,
 * making it suitable for use in contexts where checked exceptions are not required.
 * 
 * <p>Example usage:
 * <pre>{@code
 * ObjBiIntPredicate<String> isSubstringValid = (str, start, end) -> 
 *     start >= 0 && end <= str.length() && start < end;
 * boolean valid = isSubstringValid.test("Hello", 0, 5); // returns true
 * 
 * ObjBiIntPredicate<int[][]> isValidPosition = (matrix, row, col) -> 
 *     row >= 0 && row < matrix.length && 
 *     col >= 0 && col < matrix[0].length;
 * 
 * ObjBiIntPredicate<List<?>> isRangeWithinBounds = (list, from, to) -> 
 *     from >= 0 && to <= list.size() && from <= to;
 * }</pre>
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
     * <p>This method takes an object of type T and two int values, then evaluates them
     * to produce a boolean result.
     * 
     * <p>Common use cases include:
     * <ul>
     *   <li>Validating array or collection bounds with start/end indices</li>
     *   <li>Checking if coordinates are valid in 2D data structures</li>
     *   <li>Testing if a range is valid for a given object</li>
     *   <li>Verifying business rules involving an object and two numeric parameters</li>
     *   <li>Implementing access control based on object state and numeric conditions</li>
     * </ul>
     *
     * @param t the object input argument
     * @param i the first int input argument (often used as start index, row, or x-coordinate)
     * @param j the second int input argument (often used as end index, column, or y-coordinate)
     * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
     * @throws RuntimeException if any error occurs during predicate evaluation
     */
    @Override
    boolean test(T t, int i, int j);

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     * 
     * <p>The returned predicate will return {@code true} when this predicate returns {@code false},
     * and {@code false} when this predicate returns {@code true}.
     * 
     * <p>Example usage:
     * <pre>{@code
     * ObjBiIntPredicate<String> isInBounds = (str, start, end) -> 
     *     start >= 0 && end <= str.length();
     * ObjBiIntPredicate<String> isOutOfBounds = isInBounds.negate();
     * isOutOfBounds.test("Hello", -1, 5); // returns true
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
     * <p>Example usage:
     * <pre>{@code
     * ObjBiIntPredicate<String> isValidRange = (str, start, end) -> 
     *     start >= 0 && end <= str.length() && start <= end;
     * ObjBiIntPredicate<String> isNonEmpty = (str, start, end) -> 
     *     end > start;
     * 
     * ObjBiIntPredicate<String> combined = isValidRange.and(isNonEmpty);
     * combined.test("Hello", 2, 2); // returns {@code false} (not non-empty)
     * }</pre>
     *
     * @param other a predicate that will be logically-ANDed with this predicate
     * @return a composed predicate that represents the short-circuiting logical
     *         AND of this predicate and the {@code other} predicate
     */
    default ObjBiIntPredicate<T> and(final ObjBiIntPredicate<T> other) {
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
     * <p>Example usage:
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
     * @param other a predicate that will be logically-ORed with this predicate
     * @return a composed predicate that represents the short-circuiting logical
     *         OR of this predicate and the {@code other} predicate
     */
    default ObjBiIntPredicate<T> or(final ObjBiIntPredicate<T> other) {
        return (t, i, j) -> test(t, i, j) || other.test(t, i, j);
    }
}