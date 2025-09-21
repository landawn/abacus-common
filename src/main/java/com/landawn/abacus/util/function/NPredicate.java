/*
 * Copyright (C) 2021 HaiYang Li
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
 * Represents a predicate (boolean-valued function) of a variable number of arguments.
 * This is a variable-arity (varargs) generalization of {@code Predicate}.
 * 
 * <p>This is a functional interface whose functional method is {@link #test(Object[])}.
 * 
 * <p>The 'N' in NPredicate stands for 'N-ary', indicating that this predicate can accept
 * any number of arguments of the same type.
 * 
 * <p>Example usage:
 * <pre>{@code
 * NPredicate<Integer> allPositive = args -> {
 *     for (Integer n : args) {
 *         if (n <= 0) return false;
 *     }
 *     return true;
 * };
 * boolean result1 = allPositive.test(1, 2, 3, 4); // returns true
 * boolean result2 = allPositive.test(1, -2, 3);   // returns false
 * 
 * NPredicate<String> anyEmpty = args -> {
 *     for (String s : args) {
 *         if (s.isEmpty()) return true;
 *     }
 *     return false;
 * };
 * }</pre>
 * 
 * @param <T> the type of the input to the predicate
 * 
 * @see java.util.function.Predicate
 * @see java.util.function.BiPredicate
 */
@FunctionalInterface
public interface NPredicate<T> {

    /**
     * Evaluates this predicate on the given arguments.
     * 
     * <p>The varargs parameter allows this method to accept any number of arguments
     * of type T, including zero arguments (empty array), and produces a boolean result.
     * 
     * <p>Common use cases include:
     * <ul>
     *   <li>Validating multiple values against a condition</li>
     *   <li>Checking if all/any/none of the arguments meet certain criteria</li>
     *   <li>Implementing complex multi-argument validation rules</li>
     *   <li>Testing relationships between multiple values</li>
     * </ul>
     * 
     * <p>Note: The {@code @SuppressWarnings("unchecked")} annotation is used because
     * varargs with generics can generate unchecked warnings at the call site.
     *
     * @param args the input arguments as a varargs array
     * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
     */
    @SuppressWarnings("unchecked")
    boolean test(T... args);

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     * 
     * <p>The returned predicate will return {@code true} when this predicate returns {@code false},
     * and {@code false} when this predicate returns {@code true}.
     * 
     * <p>Example usage:
     * <pre>{@code
     * NPredicate<Integer> allEven = args -> {
     *     for (Integer n : args) {
     *         if (n % 2 != 0) return false;
     *     }
     *     return true;
     * };
     * NPredicate<Integer> notAllEven = allEven.negate();
     * notAllEven.test(2, 4, 5); // returns {@code true} (not all are even)
     * }</pre>
     * 
     * @return a predicate that represents the logical negation of this predicate
     */
    default NPredicate<T> negate() {
        return t -> !test(t);
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
     * NPredicate<Integer> allPositive = args -> {
     *     for (Integer n : args) {
     *         if (n <= 0) return false;
     *     }
     *     return true;
     * };
     * NPredicate<Integer> sumLessThan100 = args -> {
     *     int sum = 0;
     *     for (Integer n : args) {
     *         sum += n;
     *     }
     *     return sum < 100;
     * };
     * NPredicate<Integer> combined = allPositive.and(sumLessThan100);
     * combined.test(10, 20, 30); // returns {@code true} (all positive AND sum < 100)
     * }</pre>
     *
     * @param other a predicate that will be logically-ANDed with this predicate
     * @return a composed predicate that represents the short-circuiting logical
     *         AND of this predicate and the {@code other} predicate
     */
    default NPredicate<T> and(final NPredicate<? super T> other) {
        return t -> test(t) && other.test(t);
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
     * NPredicate<String> anyEmpty = args -> {
     *     for (String s : args) {
     *         if (s.isEmpty()) return true;
     *     }
     *     return false;
     * };
     * NPredicate<String> anyNull = args -> {
     *     for (String s : args) {
     *         if (s == null) return true;
     *     }
     *     return false;
     * };
     * NPredicate<String> anyInvalid = anyEmpty.or(anyNull);
     * anyInvalid.test("hello", "", "world"); // returns {@code true} (one is empty)
     * }</pre>
     *
     * @param other a predicate that will be logically-ORed with this predicate
     * @return a composed predicate that represents the short-circuiting logical
     *         OR of this predicate and the {@code other} predicate
     */
    default NPredicate<T> or(final NPredicate<? super T> other) {
        return t -> test(t) || other.test(t);
    }
}