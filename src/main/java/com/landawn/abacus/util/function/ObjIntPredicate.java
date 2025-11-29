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
 * A functional interface that represents a predicate (boolean-valued function) of an
 * object-valued argument and an int-valued argument. This is a specialization of
 * BiPredicate for the case where the second argument is a primitive int.
 *
 * <p>This interface is typically used for testing conditions that involve both an object
 * and an integer value, such as index validation, size checks, count comparisons, or
 * filtering operations based on numeric positions or quantities.
 *
 * <p>This is a functional interface whose functional method is {@link #test(Object, int)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <T> the type of the object argument to the predicate
 * @see java.util.function.BiPredicate
 * @see java.util.function.IntPredicate
 */
@FunctionalInterface
public interface ObjIntPredicate<T> extends Throwables.ObjIntPredicate<T, RuntimeException> { // NOSONAR
    /**
     * Evaluates this predicate on the given arguments.
     *
     * <p>This method tests whether the given object and int value satisfy the
     * condition represented by this predicate. It should return {@code true} if
     * the input arguments match the predicate's criteria, {@code false} otherwise.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIntPredicate<List<String>> hasElementAt = (list, index) ->
     *     index >= 0 && index < list.size();
     * ObjIntPredicate<String> hasMinLength = (str, minLen) ->
     *     str.length() >= minLen;
     *
     * List<String> items = List.of("a", "b", "c");
     * boolean valid = hasElementAt.test(items, 5);          // Returns false
     * boolean longEnough = hasMinLength.test("Hello", 3);   // Returns true
     * }</pre>
     *
     * @param t the first input argument of type T
     * @param u the second input argument, a primitive int value
     * @return {@code true} if the input arguments match the predicate, {@code false} otherwise
     */
    @Override
    boolean test(T t, int u);

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     *
     * <p>The returned predicate will return {@code true} when this predicate returns
     * {@code false}, and vice versa. This is useful for inverting conditions without
     * having to write a new predicate.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIntPredicate<String> hasMinLength = (str, minLen) ->
     *     str.length() >= minLen;
     * ObjIntPredicate<String> tooShort = hasMinLength.negate();
     * // tooShort tests if string length < minLen
     * }</pre>
     *
     * @return a predicate that represents the logical negation of this predicate
     */
    default ObjIntPredicate<T> negate() {
        return (t, u) -> !test(t, u);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical
     * AND of this predicate and another. When evaluating the composed predicate,
     * if this predicate is {@code false}, then the {@code other} predicate is
     * not evaluated.
     *
     * <p>Any exceptions thrown during evaluation of either predicate are relayed
     * to the caller; if evaluation of this predicate throws an exception, the
     * {@code other} predicate will not be evaluated.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIntPredicate<List<String>> validIndex = (list, index) ->
     *     index >= 0 && index < list.size();
     * ObjIntPredicate<List<String>> nonEmptyAt = (list, index) ->
     *     !list.get(index).isEmpty();
     *
     * ObjIntPredicate<List<String>> hasNonEmptyAt =
     *     validIndex.and(nonEmptyAt);
     * }</pre>
     *
     * @param other a predicate that will be logically-ANDed with this predicate
     * @return a composed predicate that represents the short-circuiting logical
     *         AND of this predicate and the {@code other} predicate
     */
    default ObjIntPredicate<T> and(final ObjIntPredicate<T> other) {
        return (t, u) -> test(t, u) && other.test(t, u);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical
     * OR of this predicate and another. When evaluating the composed predicate,
     * if this predicate is {@code true}, then the {@code other} predicate is
     * not evaluated.
     *
     * <p>Any exceptions thrown during evaluation of either predicate are relayed
     * to the caller; if evaluation of this predicate throws an exception, the
     * {@code other} predicate will not be evaluated.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIntPredicate<String> isEmpty = (str, ignored) ->
     *     str.isEmpty();
     * ObjIntPredicate<String> tooLong = (str, maxLen) ->
     *     str.length() > maxLen;
     *
     * ObjIntPredicate<String> invalid =
     *     isEmpty.or(tooLong);
     * }</pre>
     *
     * @param other a predicate that will be logically-ORed with this predicate
     * @return a composed predicate that represents the short-circuiting logical
     *         OR of this predicate and the {@code other} predicate
     */
    default ObjIntPredicate<T> or(final ObjIntPredicate<T> other) {
        return (t, u) -> test(t, u) || other.test(t, u);
    }
}
