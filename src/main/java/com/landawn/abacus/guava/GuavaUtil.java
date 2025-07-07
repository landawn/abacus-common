/*
 * Copyright (C) 2023 HaiYang Li
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
package com.landawn.abacus.guava;

import java.util.function.Function;

import com.google.common.collect.Range;

/**
 * A utility class that provides helper methods for working with Google Guava data structures.
 * This class contains static utility methods that extend the functionality of Guava's classes.
 * 
 * <p>Currently, this class provides transformation utilities for Guava's {@link Range} class,
 * allowing you to transform ranges from one type to another using mapping functions.
 * 
 * <p>This class cannot be instantiated and all methods are static.
 * 
 * @since 2023
 */
public class GuavaUtil {

    /**
     * Private constructor to prevent instantiation of this utility class.
     * This class should only be used through its static methods.
     * 
     * @throws AssertionError if someone tries to instantiate this class through reflection
     */
    private GuavaUtil() {
        // Utility class.
    }

    /**
     * Transforms a Range from one comparable type to another using the provided mapping function.
     * This method preserves the bound types (open/closed) of the original range.
     * 
     * <p>The transformation is applied to both the lower and upper bounds of the range (if they exist).
     * The resulting range will have the same bound types as the original:
     * <ul>
     *   <li>If the original has a closed lower bound, the result will have a closed lower bound</li>
     *   <li>If the original has an open upper bound, the result will have an open upper bound</li>
     *   <li>If the original is unbounded (e.g., {@code Range.all()}), the result will also be unbounded</li>
     * </ul>
     * 
     * <p>This method is particularly useful when you need to convert ranges of one type to another,
     * such as converting a range of integers to a range of strings, or a range of dates to a range
     * of timestamps.
     * 
     * <p>Example usage:
     * <pre>{@code
     * // Transform a range of integers to a range of strings
     * Range<Integer> intRange = Range.closed(1, 10);
     * Range<String> stringRange = GuavaUtil.transform(intRange, Object::toString);
     * // Result: [1..10] as strings
     * 
     * // Transform a range of dates to timestamps
     * Range<Date> dateRange = Range.closedOpen(startDate, endDate);
     * Range<Long> timestampRange = GuavaUtil.transform(dateRange, Date::getTime);
     * 
     * // Handle unbounded ranges
     * Range<Integer> unbounded = Range.greaterThan(5);
     * Range<Double> doubleRange = GuavaUtil.transform(unbounded, Integer::doubleValue);
     * // Result: (5.0..+∞)
     * }</pre>
     *
     * @param <T> the type of the source range elements, must be Comparable
     * @param <U> the type of the target range elements, must be Comparable
     * @param range the source range to transform, must not be null
     * @param mapper the function to transform elements from type T to type U, must not be null
     * @return a new Range with elements of type U, preserving the original range's bound types
     */
    public static <T extends Comparable<? super T>, U extends Comparable<? super U>> Range<U> transform(final Range<T> range, final Function<T, U> mapper) {
        if (range.hasLowerBound() && range.hasUpperBound()) {
            return Range.range(mapper.apply(range.lowerEndpoint()), range.lowerBoundType(), mapper.apply(range.upperEndpoint()), range.upperBoundType());
        } else if (range.hasLowerBound()) {
            return Range.downTo(mapper.apply(range.lowerEndpoint()), range.lowerBoundType());
        } else if (range.hasUpperBound()) {
            return Range.upTo(mapper.apply(range.upperEndpoint()), range.upperBoundType());
        } else {
            return Range.all();
        }
    }

}