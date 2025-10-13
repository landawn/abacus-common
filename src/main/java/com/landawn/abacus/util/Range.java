/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.util;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.function.Function;

import com.landawn.abacus.util.u.Optional;

/**
 * <p>
 * Note: it's copied from Apache Commons Lang developed at <a href="http://www.apache.org/">The Apache Software Foundation</a>, or
 * under the Apache License 2.0. The methods copied from other products/frameworks may be modified in this class.
 * </p>
 *
 * <p>
 * An immutable range of objects from a minimum to maximum point inclusive.
 * </p>
 *
 * <p>
 * #ThreadSafe# if the objects and comparator are thread-safe
 * </p>
 *
 * @version $Id: Range.java 1565243 2014-02-06 13:37:12Z sebb $
 * @param <T>
 */
@com.landawn.abacus.annotation.Immutable
public final class Range<T extends Comparable<? super T>> implements Serializable, Immutable {

    @Serial
    private static final long serialVersionUID = 545606166758706779L;

    /**
     * The minimum value in this range (inclusive).
     */
    private final LowerEndpoint<T> lowerEndpoint;
    /**
     * The maximum value in this range (inclusive).
     */
    private final UpperEndpoint<T> upperEndpoint;

    private final BoundType boundType;

    private Range(final LowerEndpoint<T> lowerEndpoint, final UpperEndpoint<T> upperEndpoint, final BoundType boundType) {
        this.lowerEndpoint = lowerEndpoint;
        this.upperEndpoint = upperEndpoint;
        this.boundType = boundType;
    }

    /**
     * Creates a range containing only a single element. Both the lower and upper endpoints
     * of the range will be set to the specified element, and both endpoints will be closed
     * (inclusive).
     * 
     * <p>The range uses the natural ordering of the elements to determine where values lie 
     * in the range.</p>
     * 
     * <p>Example:</p>
     * <pre>
     * Range&lt;Integer&gt; range = Range.just(5);
     * // Creates range [5, 5]
     * range.contains(5);  // returns true
     * range.contains(4);  // returns false
     * </pre>
     *
     * @param <T> the type of the elements in this range, must implement Comparable
     * @param element the single value to use for both endpoints of this range, must not be null
     * @return a new Range containing only the specified element
     * @throws IllegalArgumentException if the element is null
     */
    public static <T extends Comparable<? super T>> Range<T> just(final T element) {
        return closed(element, element);
    }

    /**
     * Creates an open range where both endpoints are exclusive. The range includes all values
     * strictly greater than min and strictly less than max.
     * 
     * <p>Example:</p>
     * <pre>
     * Range&lt;Integer&gt; range = Range.open(1, 5);
     * // Creates range (1, 5)
     * range.contains(1);  // returns false
     * range.contains(3);  // returns true
     * range.contains(5);  // returns false
     * </pre>
     *
     * @param <T> the type of the elements in this range, must implement Comparable
     * @param min the lower bound (exclusive) of the range, must not be null
     * @param max the upper bound (exclusive) of the range, must not be null
     * @return a new open Range from min (exclusive) to max (exclusive)
     * @throws IllegalArgumentException if min or max is null, or if min > max
     */
    public static <T extends Comparable<? super T>> Range<T> open(final T min, final T max) {
        if (min == null || max == null || min.compareTo(max) > 0) {
            throw new IllegalArgumentException("'fromInclusive' and 'toInclusive' can't be null, or min > max");//NOSONAR
        }

        return new Range<>(new LowerEndpoint<>(min, false), new UpperEndpoint<>(max, false), BoundType.OPEN_OPEN);
    }

    /**
     * Creates a half-open range where the lower endpoint is exclusive and the upper endpoint
     * is inclusive. The range includes all values strictly greater than min and less than or
     * equal to max.
     * 
     * <p>Example:</p>
     * <pre>
     * Range&lt;Integer&gt; range = Range.openClosed(1, 5);
     * // Creates range (1, 5]
     * range.contains(1);  // returns false
     * range.contains(3);  // returns true
     * range.contains(5);  // returns true
     * </pre>
     *
     * @param <T> the type of the elements in this range, must implement Comparable
     * @param min the lower bound (exclusive) of the range, must not be null
     * @param max the upper bound (inclusive) of the range, must not be null
     * @return a new Range from min (exclusive) to max (inclusive)
     * @throws IllegalArgumentException if min or max is null, or if min > max
     */
    public static <T extends Comparable<? super T>> Range<T> openClosed(final T min, final T max) {
        if (min == null || max == null || min.compareTo(max) > 0) {
            throw new IllegalArgumentException("'fromInclusive' and 'toInclusive' can't be null, or min > max");
        }

        return new Range<>(new LowerEndpoint<>(min, false), new UpperEndpoint<>(max, true), BoundType.OPEN_CLOSED);
    }

    /**
     * Creates a half-open range where the lower endpoint is inclusive and the upper endpoint
     * is exclusive. The range includes all values greater than or equal to min and strictly
     * less than max.
     * 
     * <p>Example:</p>
     * <pre>
     * Range&lt;Integer&gt; range = Range.closedOpen(1, 5);
     * // Creates range [1, 5)
     * range.contains(1);  // returns true
     * range.contains(3);  // returns true
     * range.contains(5);  // returns false
     * </pre>
     *
     * @param <T> the type of the elements in this range, must implement Comparable
     * @param min the lower bound (inclusive) of the range, must not be null
     * @param max the upper bound (exclusive) of the range, must not be null
     * @return a new Range from min (inclusive) to max (exclusive)
     * @throws IllegalArgumentException if min or max is null, or if min > max
     */
    public static <T extends Comparable<? super T>> Range<T> closedOpen(final T min, final T max) {
        if (min == null || max == null || min.compareTo(max) > 0) {
            throw new IllegalArgumentException("'fromInclusive' and 'toInclusive' can't be null, or min > max");
        }

        return new Range<>(new LowerEndpoint<>(min, true), new UpperEndpoint<>(max, false), BoundType.CLOSED_OPEN);
    }

    /**
     * Creates a closed range where both endpoints are inclusive. The range includes all values
     * greater than or equal to min and less than or equal to max.
     * 
     * <p>Example:</p>
     * <pre>
     * Range&lt;Integer&gt; range = Range.closed(1, 5);
     * // Creates range [1, 5]
     * range.contains(1);  // returns true
     * range.contains(3);  // returns true
     * range.contains(5);  // returns true
     * </pre>
     *
     * @param <T> the type of the elements in this range, must implement Comparable
     * @param min the lower bound (inclusive) of the range, must not be null
     * @param max the upper bound (inclusive) of the range, must not be null
     * @return a new closed Range from min (inclusive) to max (inclusive)
     * @throws IllegalArgumentException if min or max is null, or if min > max
     */
    public static <T extends Comparable<? super T>> Range<T> closed(final T min, final T max) {
        if (min == null || max == null || min.compareTo(max) > 0) {
            throw new IllegalArgumentException("'fromInclusive' and 'toInclusive' can't be null, or min > max");
        }

        return new Range<>(new LowerEndpoint<>(min, true), new UpperEndpoint<>(max, true), BoundType.CLOSED_CLOSED);
    }

    /**
     * Transforms this range by applying the given mapping function to both endpoints.
     * The resulting range maintains the same bound types (open/closed) as the original range.
     * The mapper function is applied to both the lower and upper endpoints to create a new
     * range with potentially different element types.
     *
     * <p>Example:</p>
     * <pre>
     * Range&lt;Integer&gt; intRange = Range.closed(1, 5);
     * Range&lt;String&gt; strRange = intRange.map(String::valueOf);
     * // Creates range ["1", "5"]
     * </pre>
     *
     * @param <U> the type of elements in the resulting range, must implement Comparable
     * @param mapper the function to apply to both endpoints, must not be null and must not return null values
     * @return a new Range with transformed endpoints maintaining the same bound types
     */
    public <U extends Comparable<? super U>> Range<U> map(final Function<? super T, ? extends U> mapper) {
        return new Range<>(new LowerEndpoint<>(mapper.apply(lowerEndpoint.value), lowerEndpoint.isClosed),
                new UpperEndpoint<>(mapper.apply(upperEndpoint.value), upperEndpoint.isClosed), boundType);
    }

    /**
     * Returns the bound type of this range, indicating whether the lower and upper
     * endpoints are open (exclusive) or closed (inclusive).
     * 
     * <p>The possible bound types are:</p>
     * <ul>
     *   <li>OPEN_OPEN - both endpoints are exclusive</li>
     *   <li>OPEN_CLOSED - lower endpoint is exclusive, upper endpoint is inclusive</li>
     *   <li>CLOSED_OPEN - lower endpoint is inclusive, upper endpoint is exclusive</li>
     *   <li>CLOSED_CLOSED - both endpoints are inclusive</li>
     * </ul>
     *
     * @return the BoundType enum value representing this range's endpoint types
     */
    public BoundType boundType() {
        return boundType;
    }

    /**
     * Returns the lower endpoint (minimum value) of this range. This value represents
     * the lower bound of the range, which may be either inclusive or exclusive depending
     * on the range's bound type.
     * 
     * <p>Example:</p>
     * <pre>
     * Range&lt;Integer&gt; range = Range.closed(1, 5);
     * Integer lower = range.lowerEndpoint();  // returns 1
     * </pre>
     *
     * @return the lower endpoint value of this range
     */
    public T lowerEndpoint() {
        return lowerEndpoint.value;
    }

    /**
     * Returns the upper endpoint (maximum value) of this range. This value represents
     * the upper bound of the range, which may be either inclusive or exclusive depending
     * on the range's bound type.
     * 
     * <p>Example:</p>
     * <pre>
     * Range&lt;Integer&gt; range = Range.closed(1, 5);
     * Integer upper = range.upperEndpoint();  // returns 5
     * </pre>
     *
     * @return the upper endpoint value of this range
     */
    public T upperEndpoint() {
        return upperEndpoint.value;
    }

    // Element tests
    //--------------------------------------------------------------------

    /**
     * Checks whether the specified element occurs within this range. The containment
     * check respects the bound types of the range endpoints.
     * 
     * <p>Example:</p>
     * <pre>
     * Range&lt;Integer&gt; range = Range.closedOpen(1, 5);
     * range.contains(1);    // returns {@code true} (lower bound is inclusive)
     * range.contains(3);    // returns true
     * range.contains(5);    // returns {@code false} (upper bound is exclusive)
     * range.contains(null); // returns false
     * </pre>
     *
     * @param valueToFind the element to check for containment, null returns false
     * @return {@code true} if the specified element occurs within this range's bounds, {@code false} otherwise
     */
    public boolean contains(final T valueToFind) {
        if (valueToFind == null) {
            return false;
        }

        return lowerEndpoint.includes(valueToFind) && upperEndpoint.includes(valueToFind);
    }

    /**
     * Checks whether all elements in the specified collection are contained within this range.
     * Returns true if the collection is empty or if every non-null element in the collection
     * is within the range bounds.
     * 
     * <p>Example:</p>
     * <pre>
     * Range&lt;Integer&gt; range = Range.closed(1, 10);
     * List&lt;Integer&gt; values = Arrays.asList(2, 5, 8);
     * range.containsAll(values);  // returns true
     * 
     * values = Arrays.asList(2, 5, 15);
     * range.containsAll(values);  // returns {@code false} (15 is outside range)
     * </pre>
     *
     * @param c the collection of elements to check, may be null or empty
     * @return {@code true} if all elements in the collection are contained within this range,
     *         true if the collection is null or empty, {@code false} otherwise
     */
    public boolean containsAll(final Collection<? extends T> c) {
        if (N.isEmpty(c)) {
            return true;
        }

        for (final T e : c) {
            if (!contains(e)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks whether this range starts with the specified element. Returns true only if
     * the lower endpoint is closed (inclusive) and equals the specified element.
     * 
     * <p>Example:</p>
     * <pre>
     * Range&lt;Integer&gt; range1 = Range.closed(5, 10);
     * range1.isStartedBy(5);   // returns true
     * 
     * Range&lt;Integer&gt; range2 = Range.open(5, 10);
     * range2.isStartedBy(5);   // returns {@code false} (lower bound is exclusive)
     * </pre>
     *
     * @param element the element to check against the lower endpoint, null returns false
     * @return {@code true} if this range has a closed lower endpoint that equals the specified element
     */
    public boolean isStartedBy(final T element) {
        if (element == null) {
            return false;
        }

        return lowerEndpoint.isClosed && lowerEndpoint.compareTo(element) == 0;
    }

    /**
     * Checks whether this range ends with the specified element. Returns true only if
     * the upper endpoint is closed (inclusive) and equals the specified element.
     * 
     * <p>Example:</p>
     * <pre>
     * Range&lt;Integer&gt; range1 = Range.closed(5, 10);
     * range1.isEndedBy(10);    // returns true
     * 
     * Range&lt;Integer&gt; range2 = Range.closedOpen(5, 10);
     * range2.isEndedBy(10);    // returns {@code false} (upper bound is exclusive)
     * </pre>
     *
     * @param element the element to check against the upper endpoint, null returns false
     * @return {@code true} if this range has a closed upper endpoint that equals the specified element
     */
    public boolean isEndedBy(final T element) {
        if (element == null) {
            return false;
        }

        return upperEndpoint.isClosed && upperEndpoint.compareTo(element) == 0;
    }

    /**
     * Checks whether this range is entirely after the specified element. Returns true if
     * the element is less than the lower endpoint of this range (considering bound type).
     * 
     * <p>Example:</p>
     * <pre>
     * Range&lt;Integer&gt; range = Range.closed(5, 10);
     * range.isAfter(3);   // returns true
     * range.isAfter(5);   // returns {@code false} (5 is included in range)
     * range.isAfter(7);   // returns {@code false} (7 is within range)
     * </pre>
     *
     * @param element the element to check, null returns false
     * @return {@code true} if this entire range is after (greater than) the specified element
     */
    public boolean isAfter(final T element) {
        if (element == null) {
            return false;
        }

        return !lowerEndpoint.includes(element);
    }

    /**
     * Checks whether this range is entirely before the specified element. Returns true if
     * the element is greater than the upper endpoint of this range (considering bound type).
     * 
     * <p>Example:</p>
     * <pre>
     * Range&lt;Integer&gt; range = Range.closed(5, 10);
     * range.isBefore(12);  // returns true
     * range.isBefore(10);  // returns {@code false} (10 is included in range)
     * range.isBefore(7);   // returns {@code false} (7 is within range)
     * </pre>
     *
     * @param element the element to check, null returns false
     * @return {@code true} if this entire range is before (less than) the specified element
     */
    public boolean isBefore(final T element) {
        if (element == null) {
            return false;
        }

        return !upperEndpoint.includes(element);
    }

    /**
     * Compares the position of the specified element relative to this range.
     * 
     * <p>Returns:</p>
     * <ul>
     *   <li>-1 if this range is entirely before the element</li>
     *   <li>0 if the element is contained within this range</li>
     *   <li>1 if this range is entirely after the element</li>
     * </ul>
     * 
     * <p>Example:</p>
     * <pre>
     * Range&lt;Integer&gt; range = Range.closed(5, 10);
     * range.compareTo(3);   // returns 1 (range is after 3)
     * range.compareTo(7);   // returns 0 (7 is within range)
     * range.compareTo(12);  // returns -1 (range is before 12)
     * </pre>
     *
     * @param element the element to compare against this range, must not be null
     * @return -1, 0, or 1 depending on the element's position relative to this range
     * @throws IllegalArgumentException if element is null
     */
    public int compareTo(final T element) {
        if (element == null) {
            // Comparable API says throw NPE on null
            throw new IllegalArgumentException("Element is null");
        }

        if (isBefore(element)) {
            return -1;
        } else if (isAfter(element)) {
            return 1;
        } else {
            return 0;
        }
    }

    // Range tests
    //--------------------------------------------------------------------

    /**
     * Checks whether this range contains all elements of the specified range.
     * A range contains another range if every possible value in the other range
     * is also contained in this range, respecting bound types.
     *
     * <p>For a closed endpoint in the other range, this range must contain that endpoint value.
     * For an open endpoint in the other range, this range's corresponding endpoint must extend
     * strictly beyond the other's endpoint.</p>
     *
     * <p>Example:</p>
     * <pre>
     * Range&lt;Integer&gt; range1 = Range.closed(1, 10);
     * Range&lt;Integer&gt; range2 = Range.closed(3, 7);
     * Range&lt;Integer&gt; range3 = Range.closed(5, 15);
     * Range&lt;Integer&gt; range4 = Range.open(1, 10);
     *
     * range1.containsRange(range2);  // returns true
     * range1.containsRange(range3);  // returns {@code false} (extends beyond upper bound)
     * range1.containsRange(range4);  // returns true (open range (1,10) is within [1,10])
     * range1.containsRange(null);    // returns false
     * </pre>
     *
     * @param other the range to check for containment, null returns false
     * @return {@code true} if this range contains all elements of the specified range
     * @throws RuntimeException if ranges cannot be compared
     */
    public boolean containsRange(final Range<T> other) {
        if (other == null) {
            return false;
        }

        return (lowerEndpoint.isClosed ? contains(other.lowerEndpoint.value) : lowerEndpoint.value.compareTo(other.lowerEndpoint.value) < 0)
                && (upperEndpoint.isClosed ? contains(other.upperEndpoint.value) : upperEndpoint.value.compareTo(other.upperEndpoint.value) > 0);
    }

    /**
     * Checks whether this range is completely after the specified range.
     * Returns true if the lower endpoint of this range is greater than or equal to
     * the upper endpoint of the other range (considering bound types).
     * 
     * <p>Example:</p>
     * <pre>
     * Range&lt;Integer&gt; range1 = Range.closed(10, 15);
     * Range&lt;Integer&gt; range2 = Range.closed(1, 5);
     * Range&lt;Integer&gt; range3 = Range.closed(1, 10);
     * 
     * range1.isAfterRange(range2);  // returns true
     * range1.isAfterRange(range3);  // returns {@code false} (ranges touch at 10)
     * </pre>
     *
     * @param other the range to compare against, null returns false
     * @return {@code true} if this range is completely after the specified range
     * @throws RuntimeException if ranges cannot be compared
     */
    public boolean isAfterRange(final Range<T> other) {
        if (other == null) {
            return false;
        }
        return other.upperEndpoint.isClosed ? isAfter(other.upperEndpoint.value) : lowerEndpoint.compareTo(other.upperEndpoint.value) >= 0;
    }

    /**
     * Checks whether this range is completely before the specified range.
     * Returns true if the upper endpoint of this range is less than or equal to
     * the lower endpoint of the other range (considering bound types).
     * 
     * <p>Example:</p>
     * <pre>
     * Range&lt;Integer&gt; range1 = Range.closed(1, 5);
     * Range&lt;Integer&gt; range2 = Range.closed(10, 15);
     * Range&lt;Integer&gt; range3 = Range.closed(5, 10);
     * 
     * range1.isBeforeRange(range2);  // returns true
     * range1.isBeforeRange(range3);  // returns {@code false} (ranges touch at 5)
     * </pre>
     *
     * @param other the range to compare against, null returns false
     * @return {@code true} if this range is completely before the specified range
     * @throws RuntimeException if ranges cannot be compared
     */
    public boolean isBeforeRange(final Range<T> other) {
        if (other == null) {
            return false;
        }

        return other.lowerEndpoint.isClosed ? isBefore(other.lowerEndpoint.value) : upperEndpoint.compareTo(other.lowerEndpoint.value) <= 0;
    }

    /**
     * Checks whether this range overlaps with the specified range.
     * Two ranges overlap if there is at least one element that is contained in both ranges.
     * Ranges that touch at a single point are considered overlapping only if that point
     * is included in both ranges.
     * 
     * <p>Example:</p>
     * <pre>
     * Range&lt;Integer&gt; range1 = Range.closed(1, 5);
     * Range&lt;Integer&gt; range2 = Range.closed(3, 8);
     * Range&lt;Integer&gt; range3 = Range.closed(6, 10);
     * Range&lt;Integer&gt; range4 = Range.open(5, 10);
     * 
     * range1.isOverlappedBy(range2);  // returns {@code true} (overlap from 3 to 5)
     * range1.isOverlappedBy(range3);  // returns {@code false} (no overlap)
     * range1.isOverlappedBy(range4);  // returns {@code false} (ranges touch at 5 but not both inclusive)
     * </pre>
     *
     * @param other the range to test for overlap, null returns false
     * @return {@code true} if the specified range overlaps with this range; otherwise, false
     * @throws RuntimeException if ranges cannot be compared
     */
    public boolean isOverlappedBy(final Range<T> other) {
        //NOSONAR
        return other != null && !isAfterRange(other) && !isBeforeRange(other);
    }

    /**
     * Calculates the intersection of this range with another overlapping range.
     * The intersection is the largest range that is contained by both input ranges.
     * If the ranges do not overlap, returns an empty Optional. The intersection preserves
     * the appropriate bound types from both ranges.
     *
     * <p>Example:</p>
     * <pre>
     * Range&lt;Integer&gt; range1 = Range.closed(1, 5);
     * Range&lt;Integer&gt; range2 = Range.closed(3, 8);
     * Optional&lt;Range&lt;Integer&gt;&gt; intersection = range1.intersection(range2);
     * // Returns Optional containing Range.closed(3, 5)
     *
     * Range&lt;Integer&gt; range3 = Range.closed(6, 10);
     * Optional&lt;Range&lt;Integer&gt;&gt; noIntersection = range1.intersection(range3);
     * // Returns Optional.empty()
     *
     * Range&lt;Integer&gt; range4 = Range.open(1, 5);
     * Range&lt;Integer&gt; range5 = Range.closed(1, 5);
     * Optional&lt;Range&lt;Integer&gt;&gt; intersection2 = range4.intersection(range5);
     * // Returns Optional containing Range.open(1, 5) - more restrictive bounds
     * </pre>
     *
     * @param other the range to intersect with this range, must not be null
     * @return an Optional containing the intersection range if the ranges overlap,
     *         Optional.empty() if they don't overlap, or Optional containing this range if they are equal
     */
    public Optional<Range<T>> intersection(final Range<T> other) {
        if (!this.isOverlappedBy(other)) {
            return Optional.empty();
        } else if (this.equals(other)) {
            return Optional.of(this);
        }

        final LowerEndpoint<T> newLowerEndpoint = lowerEndpoint.includes(other.lowerEndpoint.value) ? other.lowerEndpoint : lowerEndpoint;
        final UpperEndpoint<T> newUpperEndpoint = upperEndpoint.includes(other.upperEndpoint.value) ? other.upperEndpoint : upperEndpoint;

        BoundType boundType = null;//NOSONAR

        if (newLowerEndpoint.isClosed) {
            boundType = newUpperEndpoint.isClosed ? BoundType.CLOSED_CLOSED : BoundType.CLOSED_OPEN;
        } else {
            boundType = newUpperEndpoint.isClosed ? BoundType.OPEN_CLOSED : BoundType.OPEN_OPEN;
        }

        return Optional.of(new Range<>(newLowerEndpoint, newUpperEndpoint, boundType));
    }

    /**
     * Returns the minimal range that encloses both this range and the specified range.
     * The span is the smallest range that contains every value contained in either of the
     * input ranges. If the input ranges are connected (overlapping or touching), the span
     * is their union. If they are not connected, the span includes values between the ranges
     * that are not in either input range.
     *
     * <p>The span operation takes the minimum of the lower endpoints and the maximum of the
     * upper endpoints, preserving the most inclusive bound type at each endpoint.</p>
     *
     * <p>This operation is commutative, associative, and idempotent.</p>
     *
     * <p>Example:</p>
     * <pre>
     * Range&lt;Integer&gt; range1 = Range.closed(1, 3);
     * Range&lt;Integer&gt; range2 = Range.closed(5, 7);
     * Range&lt;Integer&gt; span = range1.span(range2);
     * // Returns Range.closed(1, 7) which includes values 4 not in either range
     *
     * Range&lt;Integer&gt; range3 = Range.open(1, 3);
     * Range&lt;Integer&gt; range4 = Range.open(5, 7);
     * Range&lt;Integer&gt; span2 = range3.span(range4);
     * // Returns Range.open(1, 7)
     * </pre>
     *
     * @param other the range to span with this range, must not be null
     * @return the minimal range that contains all values from both input ranges
     */
    public Range<T> span(final Range<T> other) {
        final LowerEndpoint<T> newLowerEndpoint = lowerEndpoint.includes(other.lowerEndpoint.value) ? lowerEndpoint : other.lowerEndpoint;
        final UpperEndpoint<T> newUpperEndpoint = upperEndpoint.includes(other.upperEndpoint.value) ? upperEndpoint : other.upperEndpoint;

        BoundType boundType = null;//NOSONAR

        if (newLowerEndpoint.isClosed) {
            boundType = newUpperEndpoint.isClosed ? BoundType.CLOSED_CLOSED : BoundType.CLOSED_OPEN;
        } else {
            boundType = newUpperEndpoint.isClosed ? BoundType.OPEN_CLOSED : BoundType.OPEN_OPEN;
        }

        return new Range<>(newLowerEndpoint, newUpperEndpoint, boundType);
    }

    /**
     * Checks if this range is empty. A range is empty if and only if it has the form
     * (a, a) where both endpoints are the same value and both are exclusive (open).
     * All other ranges contain at least one value.
     * 
     * <p>Example:</p>
     * <pre>
     * Range&lt;Integer&gt; emptyRange = Range.open(5, 5);
     * emptyRange.isEmpty();     // returns true
     * 
     * Range&lt;Integer&gt; pointRange = Range.closed(5, 5);
     * pointRange.isEmpty();     // returns {@code false} (contains the value 5)
     * 
     * Range&lt;Integer&gt; normalRange = Range.open(5, 6);
     * normalRange.isEmpty();    // returns false
     * </pre>
     *
     * @return {@code true} if this range contains no values, {@code false} otherwise
     */
    public boolean isEmpty() {
        //NOSONAR
        return !lowerEndpoint.isClosed && !upperEndpoint.isClosed && lowerEndpoint.compareTo(upperEndpoint.value) == 0;
    }

    // Basics
    //--------------------------------------------------------------------

    /**
     * Compares this range to another object for equality. Two ranges are equal if they
     * have the same lower and upper endpoints with the same bound types. The comparison
     * ignores any differences in comparators.
     * 
     * <p>Example:</p>
     * <pre>
     * Range&lt;Integer&gt; range1 = Range.closed(1, 5);
     * Range&lt;Integer&gt; range2 = Range.closed(1, 5);
     * Range&lt;Integer&gt; range3 = Range.open(1, 5);
     * 
     * range1.equals(range2);  // returns true
     * range1.equals(range3);  // returns {@code false} (different bound types)
     * </pre>
     *
     * @param obj the reference object with which to compare
     * @return {@code true} if this object is equal to the obj argument; false otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Range) {
            final Range<T> other = (Range<T>) obj;
            return N.equals(lowerEndpoint, other.lowerEndpoint) && N.equals(upperEndpoint, other.upperEndpoint);
        }

        return false;
    }

    /**
     * Returns a hash code value for this range. The hash code is computed based on
     * the class, lower endpoint, and upper endpoint. Equal ranges will have equal
     * hash codes.
     *
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        int result = 17;

        result = 37 * result + getClass().hashCode();
        result = 37 * result + lowerEndpoint.hashCode();
        return 37 * result + upperEndpoint.hashCode();
    }

    /**
     * Returns a string representation of this range. The format shows the lower and
     * upper endpoints with brackets indicating whether each endpoint is inclusive
     * (square bracket) or exclusive (parenthesis).
     * 
     * <p>Example formats:</p>
     * <ul>
     *   <li>[1, 5] - both endpoints inclusive</li>
     *   <li>(1, 5) - both endpoints exclusive</li>
     *   <li>[1, 5) - lower inclusive, upper exclusive</li>
     *   <li>(1, 5] - lower exclusive, upper inclusive</li>
     * </ul>
     *
     * @return a string representation of this range
     */
    @Override
    public String toString() {
        return lowerEndpoint.toString() + ", " + upperEndpoint.toString();
    }

    /**
     * The Enum BoundType.
     */
    public enum BoundType {

        OPEN_OPEN, OPEN_CLOSED, CLOSED_OPEN, CLOSED_CLOSED
    }

    /**
     * The Class Endpoint.
     *
     * @param <T>
     */
    abstract static class Endpoint<T extends Comparable<? super T>> implements Serializable {

        @Serial
        private static final long serialVersionUID = -1404748904424344410L;

        final T value; //NOSONAR

        final boolean isClosed;

        /**
         * Instantiates a new endpoint.
         *
         * @param value
         * @param isClosed
         */
        protected Endpoint(final T value, final boolean isClosed) {
            this.value = value;
            this.isClosed = isClosed;
        }

        /**
         *
         * @param value
         * @return
         */
        public int compareTo(final T value) {
            return N.compare(this.value, value);
        }

        /**
         *
         * @param value
         * @return
         */
        public abstract boolean includes(T value);

    }

    /**
     * The Class LowerEndpoint.
     *
     * @param <T>
     */
    static class LowerEndpoint<T extends Comparable<? super T>> extends Endpoint<T> {

        @Serial
        private static final long serialVersionUID = -1369183906861608859L;

        /**
         * Instantiates a new lower endpoint.
         *
         * @param value
         * @param isClosed
         */
        LowerEndpoint(final T value, final boolean isClosed) {
            super(value, isClosed);
        }

        /**
         *
         * @param value
         * @return
         */
        @Override
        public boolean includes(final T value) {
            return isClosed ? N.compare(value, this.value) >= 0 : N.compare(value, this.value) > 0;
        }

        /**
         * <p>
         * Gets a suitable hash code for the range.
         * </p>
         *
         * @return a hash code value for this object
         */
        @Override
        public int hashCode() {
            final int result = isClosed ? 0 : 1;
            return 37 * result + N.hashCode(value);
        }

        /**
         * <p>
         * Compares this range to another object to test if they are equal.
         * </p>
         * .
         *
         * <p>
         * To be equal, the minimum and maximum values must be equal, which ignores any differences in the comparator.
         * </p>
         *
         * @param obj
         *            the reference object with which to compare
         * @return {@code true} if this object is equal
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof LowerEndpoint) {
                final LowerEndpoint<T> other = (LowerEndpoint<T>) obj;

                return N.equals(isClosed, other.isClosed) && N.equals(value, other.value);
            }

            return false;
        }

        @Override
        public String toString() {
            return (isClosed ? "[" : "(") + N.toString(value);
        }
    }

    /**
     * The Class UpperEndpoint.
     *
     * @param <T>
     */
    static class UpperEndpoint<T extends Comparable<? super T>> extends Endpoint<T> {

        @Serial
        private static final long serialVersionUID = 3180376045860768477L;

        /**
         * Instantiates a new upper endpoint.
         *
         * @param value
         * @param isClosed
         */
        UpperEndpoint(final T value, final boolean isClosed) {
            super(value, isClosed);
        }

        /**
         *
         * @param value
         * @return
         */
        @Override
        public boolean includes(final T value) {
            return isClosed ? N.compare(value, this.value) <= 0 : N.compare(value, this.value) < 0;
        }

        @Override
        public int hashCode() {
            final int result = isClosed ? 0 : 1;
            return 37 * result + N.hashCode(value);
        }

        /**
         *
         * @param obj
         * @return
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof UpperEndpoint) {
                final UpperEndpoint<T> other = (UpperEndpoint<T>) obj;

                return N.equals(isClosed, other.isClosed) && N.equals(value, other.value);
            }

            return false;
        }

        @Override
        public String toString() {
            return N.toString(value) + (isClosed ? "]" : ")");
        }
    }
}