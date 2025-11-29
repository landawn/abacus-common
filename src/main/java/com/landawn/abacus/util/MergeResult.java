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

package com.landawn.abacus.util;

import java.util.Comparator;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.SequentialOnly;
import com.landawn.abacus.annotation.Stateful;
import com.landawn.abacus.util.function.BiFunction;

/**
 * An enumeration representing the result of a merge operation between two values.
 * 
 * <p>This enum is designed to indicate which of two values should be selected during
 * a merge operation. It provides utility methods to create merge strategies based on
 * comparison criteria such as minimum or maximum values.</p>
 * 
 * <p>The enum contains two constants:</p>
 * <ul>
 *   <li>{@link #TAKE_FIRST} - Indicates that the first value should be selected</li>
 *   <li>{@link #TAKE_SECOND} - Indicates that the second value should be selected</li>
 * </ul>
 * 
 * <p>Example usage in a merge operation:</p>
 * <pre>{@code
 * // Using with Stream merge operations
 * Stream<Integer> stream1 = Stream.of(1, 3, 5);
 * Stream<Integer> stream2 = Stream.of(2, 4, 6);
 * 
 * // Merge taking the minimum value
 * Stream<Integer> merged = stream1.merge(stream2, MergeResult::minFirst);
 * 
 * // Using with custom comparator
 * BiFunction<Person, Person, MergeResult> elderFirst = 
 *     MergeResult.maxFirst(Comparator.comparing(Person::getAge));
 * }</pre>
 * 
 * @see BiFunction
 * @see Comparator
 */
public enum MergeResult {
    /**
     * Indicates that the first value should be taken in a merge operation.
     * 
     * <p>When this result is returned from a merge function, it signals that
     * the first of the two compared values should be selected.</p>
     */
    TAKE_FIRST,

    /**
     * Indicates that the second value should be taken in a merge operation.
     * 
     * <p>When this result is returned from a merge function, it signals that
     * the second of the two compared values should be selected.</p>
     */
    TAKE_SECOND;

    /*, THIRD, FOURTH, FIFTH, SIXTH, SEVENTH*/

    /**
     * Compares two comparable values and returns a MergeResult indicating
     * which value is smaller (or equal).
     * 
     * <p>This method uses natural ordering to compare the values. If the first
     * value is less than or equal to the second, {@link #TAKE_FIRST} is returned;
     * otherwise, {@link #TAKE_SECOND} is returned.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MergeResult result = MergeResult.minFirst(5, 10);       // returns TAKE_FIRST
     * MergeResult result2 = MergeResult.minFirst("b", "a");   // returns TAKE_SECOND
     * }</pre>
     * 
     * @param <T> the type of the values being compared (must be Comparable)
     * @param a the first value to compare
     * @param b the second value to compare
     * @return {@link #TAKE_FIRST} if a ≤ b, {@link #TAKE_SECOND} otherwise
     */
    public static <T extends Comparable<? super T>> MergeResult minFirst(final T a, final T b) {
        return N.compare(a, b) <= 0 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
    }

    /**
     * Compares two values using the provided comparator and returns a MergeResult
     * indicating which value is smaller (or equal).
     * 
     * <p>If the comparator determines that the first value is less than or equal
     * to the second, {@link #TAKE_FIRST} is returned; otherwise, {@link #TAKE_SECOND}
     * is returned.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Comparator<String> lengthComparator = Comparator.comparing(String::length);
     * MergeResult result = MergeResult.minFirst("hello", "hi", lengthComparator); 
     * // returns TAKE_SECOND (since "hi" has shorter length)
     * }</pre>
     * 
     * @param <T> the type of the values being compared
     * @param a the first value to compare
     * @param b the second value to compare
     * @param cmp the comparator to use for comparison
     * @return {@link #TAKE_FIRST} if cmp.compare(a, b) ≤ 0, {@link #TAKE_SECOND} otherwise
     */
    public static <T> MergeResult minFirst(final T a, final T b, final Comparator<? super T> cmp) {
        return cmp.compare(a, b) <= 0 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
    }

    /**
     * Compares two comparable values and returns a MergeResult indicating
     * which value is larger (or equal).
     * 
     * <p>This method uses natural ordering to compare the values. If the first
     * value is greater than or equal to the second, {@link #TAKE_FIRST} is returned;
     * otherwise, {@link #TAKE_SECOND} is returned.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MergeResult result = MergeResult.maxFirst(5, 10);       // returns TAKE_SECOND
     * MergeResult result2 = MergeResult.maxFirst("b", "a");   // returns TAKE_FIRST
     * }</pre>
     * 
     * @param <T> the type of the values being compared (must be Comparable)
     * @param a the first value to compare
     * @param b the second value to compare
     * @return {@link #TAKE_FIRST} if a ≥ b, {@link #TAKE_SECOND} otherwise
     */
    public static <T extends Comparable<? super T>> MergeResult maxFirst(final T a, final T b) {
        return N.compare(a, b) >= 0 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
    }

    /**
     * Compares two values using the provided comparator and returns a MergeResult
     * indicating which value is larger (or equal).
     * 
     * <p>If the comparator determines that the first value is greater than or equal
     * to the second, {@link #TAKE_FIRST} is returned; otherwise, {@link #TAKE_SECOND}
     * is returned.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Comparator<String> lengthComparator = Comparator.comparing(String::length);
     * MergeResult result = MergeResult.maxFirst("hello", "hi", lengthComparator); 
     * // returns TAKE_FIRST (since "hello" has longer length)
     * }</pre>
     * 
     * @param <T> the type of the values being compared
     * @param a the first value to compare
     * @param b the second value to compare
     * @param cmp the comparator to use for comparison
     * @return {@link #TAKE_FIRST} if cmp.compare(a, b) ≥ 0, {@link #TAKE_SECOND} otherwise
     */
    public static <T> MergeResult maxFirst(final T a, final T b, final Comparator<? super T> cmp) {
        return cmp.compare(a, b) >= 0 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
    }

    @SuppressWarnings("rawtypes")
    private static final BiFunction<Comparable, Comparable, MergeResult> MIN_FIRST_BF = (a, b) -> N.compare(a, b) <= 0 ? MergeResult.TAKE_FIRST
            : MergeResult.TAKE_SECOND;

    /**
     * Returns a BiFunction that compares two comparable values and returns
     * the MergeResult indicating which is smaller.
     * 
     * <p>The returned function uses natural ordering for comparison. This is useful
     * when you need to pass a merge strategy to methods that accept BiFunctions.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiFunction<Integer, Integer, MergeResult> minMerger = MergeResult.minFirst();
     * Stream<Integer> merged = stream1.merge(stream2, minMerger);
     * }</pre>
     * 
     * @param <T> the type of the values (must extend Comparable)
     * @return a BiFunction that returns {@link #TAKE_FIRST} if first ≤ second,
     *         {@link #TAKE_SECOND} otherwise
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> BiFunction<T, T, MergeResult> minFirst() {
        return (BiFunction) MIN_FIRST_BF;
    }

    /**
     * Returns a BiFunction that compares two values using the provided comparator
     * and returns the MergeResult indicating which is smaller.
     * 
     * <p>This factory method creates a reusable merge strategy that can be passed
     * to methods accepting BiFunctions.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiFunction<Person, Person, MergeResult> youngerFirst = 
     *     MergeResult.minFirst(Comparator.comparing(Person::getAge));
     * Stream<Person> merged = stream1.merge(stream2, youngerFirst);
     * }</pre>
     * 
     * @param <T> the type of the values being compared
     * @param cmp the comparator to use for comparison (must not be null)
     * @return a BiFunction that uses the comparator to determine merge results
     * @throws IllegalArgumentException if cmp is null
     */
    public static <T> BiFunction<T, T, MergeResult> minFirst(final Comparator<? super T> cmp) throws IllegalArgumentException {
        N.checkArgNotNull(cmp, cs.cmp);

        return (a, b) -> cmp.compare(a, b) <= 0 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
    }

    @SuppressWarnings("rawtypes")
    private static final BiFunction<Comparable, Comparable, MergeResult> MAX_FIRST_BF = (a, b) -> N.compare(a, b) >= 0 ? MergeResult.TAKE_FIRST
            : MergeResult.TAKE_SECOND;

    /**
     * Returns a BiFunction that compares two comparable values and returns
     * the MergeResult indicating which is larger.
     * 
     * <p>The returned function uses natural ordering for comparison. This is useful
     * when you need to pass a merge strategy to methods that accept BiFunctions.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiFunction<Integer, Integer, MergeResult> maxMerger = MergeResult.maxFirst();
     * Stream<Integer> merged = stream1.merge(stream2, maxMerger);
     * }</pre>
     * 
     * @param <T> the type of the values (must extend Comparable)
     * @return a BiFunction that returns {@link #TAKE_FIRST} if first ≥ second,
     *         {@link #TAKE_SECOND} otherwise
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> BiFunction<T, T, MergeResult> maxFirst() {
        return (BiFunction) MAX_FIRST_BF;
    }

    /**
     * Returns a BiFunction that compares two values using the provided comparator
     * and returns the MergeResult indicating which is larger.
     * 
     * <p>This factory method creates a reusable merge strategy that can be passed
     * to methods accepting BiFunctions.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiFunction<Person, Person, MergeResult> elderFirst = 
     *     MergeResult.maxFirst(Comparator.comparing(Person::getAge));
     * Stream<Person> merged = stream1.merge(stream2, elderFirst);
     * }</pre>
     * 
     * @param <T> the type of the values being compared
     * @param cmp the comparator to use for comparison (must not be null)
     * @return a BiFunction that uses the comparator to determine merge results
     * @throws IllegalArgumentException if cmp is null
     */
    public static <T> BiFunction<T, T, MergeResult> maxFirst(final Comparator<? super T> cmp) throws IllegalArgumentException {
        N.checkArgNotNull(cmp, cs.cmp);

        return (a, b) -> cmp.compare(a, b) >= 0 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
    }

    /**
     * Returns a stateful BiFunction that alternates between returning TAKE_FIRST
     * and TAKE_SECOND on successive calls.
     * 
     * <p><strong>Warning:</strong> This method returns a stateful function that
     * maintains internal state between calls. It should not be:</p>
     * <ul>
     *   <li>Cached or reused across different operations</li>
     *   <li>Used in parallel streams</li>
     *   <li>Shared between threads</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiFunction<String, String, MergeResult> alternator = MergeResult.alternate();
     * alternator.apply("a", "b");   // returns TAKE_FIRST
     * alternator.apply("c", "d");   // returns TAKE_SECOND
     * alternator.apply("e", "f");   // returns TAKE_FIRST
     * }</pre>
     * 
     * @param <T> the type of the values (not used in the decision)
     * @return a stateful BiFunction that alternates between merge results
     * @deprecated Use {@link Fn#alternate()} instead for better API organization
     * @see Fn#alternate()
     */
    @Deprecated
    @Beta
    @SequentialOnly
    @Stateful
    public static <T> BiFunction<T, T, MergeResult> alternate() {
        return Fn.alternate();
    }
}
