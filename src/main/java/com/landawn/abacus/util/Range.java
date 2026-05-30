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
 * An immutable mathematical range representing a continuous interval between two comparable values,
 * supporting both open and closed boundaries. This class provides comprehensive range operations
 * including containment testing, intersection calculation, and range comparison, making it ideal
 * for numerical intervals, date ranges, version ranges, and any scenario requiring bounded value sets.
 *
 * <p>A range defines a contiguous set of values between a lower and upper endpoint, where each endpoint
 * can be either <em>closed</em> (inclusive) or <em>open</em> (exclusive). This flexibility allows
 * precise modeling of mathematical intervals such as [1,10], (0,1), [start, end), etc. The class
 * ensures type safety by requiring elements to implement {@code Comparable} and maintains immutability
 * for thread-safe operations.</p>
 *
 * <p><b>⚠️ IMPORTANT - Thread Safety:</b>
 * This class is thread-safe if and only if the contained objects and any custom comparators
 * are thread-safe. The Range itself is immutable, but thread safety depends on the mutability
 * and thread safety of the generic type {@code T}.</p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Immutable Design:</b> All instances are immutable, ensuring thread safety and preventing accidental modification</li>
 *   <li><b>Flexible Boundaries:</b> Support for open, closed, and mixed boundary types (open-closed, closed-open)</li>
 *   <li><b>Type Safety:</b> Generic constraints ensure only comparable types can be used</li>
 *   <li><b>Mathematical Operations:</b> Intersection, span, containment, and overlap testing</li>
 *   <li><b>Null Safety:</b> Proper handling of null values with clear semantics</li>
 *   <li><b>Performance Optimized:</b> Efficient algorithms for range operations and comparisons</li>
 *   <li><b>Serializable:</b> Supports Java serialization for persistence and distributed systems</li>
 *   <li><b>Functional Programming:</b> Map operations for range transformation</li>
 * </ul>
 *
 * <p><b>⚠️ IMPORTANT - Immutable Design:</b>
 * <ul>
 *   <li>This class implements {@link Immutable}, guaranteeing that instances cannot be modified after creation</li>
 *   <li>All endpoint values and boundary types are final and set only during construction</li>
 *   <li>All operations return new Range instances rather than modifying existing ones</li>
 *   <li>Thread-safe by design due to immutability (assuming contained objects are thread-safe)</li>
 * </ul>
 *
 * <p><b>Design Philosophy:</b>
 * <ul>
 *   <li><b>Mathematical Precision:</b> Accurate representation of mathematical intervals with proper boundary semantics</li>
 *   <li><b>Type Safety Over Flexibility:</b> Compile-time guarantees prevent runtime errors with incomparable types</li>
 *   <li><b>Immutability Over Performance:</b> Prioritizes correctness and thread safety over minimal performance gains</li>
 *   <li><b>Explicit Boundaries:</b> Clear distinction between inclusive and exclusive endpoints</li>
 *   <li><b>Composability:</b> Range operations can be chained and combined naturally</li>
 * </ul>
 *
 * <p><b>Boundary Type System:</b>
 * <ul>
 *   <li><b>Closed Boundary:</b> {@code [value]} - Includes the endpoint value in the range</li>
 *   <li><b>Open Boundary:</b> {@code (value)} - Excludes the endpoint value from the range</li>
 *   <li><b>Mathematical Notation:</b> Follows standard mathematical interval notation</li>
 *   <li><b>Four Combinations:</b> {@code [a,b]}, {@code (a,b)}, {@code [a,b)}, {@code (a,b]}</li>
 * </ul>
 *
 * <p><b>Generic Type Parameter:</b>
 * <ul>
 *   <li><b>{@code T extends Comparable<? super T>}:</b> The type of elements in the range</li>
 *   <li><b>Comparable Constraint:</b> Ensures elements can be ordered and compared</li>
 *   <li><b>Wildcard Bounds:</b> Allows for proper variance in comparison operations</li>
 *   <li><b>Type Safety:</b> Prevents mixing incomparable types at compile time</li>
 * </ul>
 *
 * <p><b>Common Usage Patterns:</b>
 * <pre>{@code
 * // Creating ranges with different boundary types
 * Range<Integer> closedRange = Range.closed(1, 10);   // [1, 10] - includes 1 and 10
 * Range<Integer> openRange = Range.open(1, 10);   // (1, 10) - excludes 1 and 10
 * Range<Integer> halfOpen = Range.closedOpen(1, 10);   // [1, 10) - includes 1, excludes 10
 * Range<Integer> halfClosed = Range.openClosed(1, 10);   // (1, 10] - excludes 1, includes 10
 *
 * // Single element ranges
 * Range<String> single = Range.just("value");   // [value, value] - contains only "value"
 *
 * // Containment testing
 * boolean contains5 = closedRange.contains(5);   // true - 5 is in [1, 10]
 * boolean contains1 = openRange.contains(1);   // false - 1 is not in (1, 10)
 * boolean contains10 = halfOpen.contains(10);   // false - 10 is not in [1, 10)
 *
 * // Range operations
 * Range<Integer> other = Range.closed(5, 15);
 * boolean overlaps = closedRange.isOverlappedBy(other);   // true - ranges overlap
 * Optional<Range<Integer>> intersection = closedRange.intersection(other);   // [5, 10]
 * Range<Integer> span = closedRange.span(other);   // [1, 15] - encompasses both ranges
 * }</pre>
 *
 * <p><b>Advanced Usage Examples:</b></p>
 * <pre>{@code
 * // Working with date ranges
 * LocalDate start = LocalDate.of(2024, 1, 1);
 * LocalDate end = LocalDate.of(2024, 12, 31);
 * Range<LocalDate> year2024 = Range.closed(start, end);
 * boolean isInYear = year2024.contains(LocalDate.now());
 *
 * // Functional transformation
 * Range<Integer> intRange = Range.closed(1, 5);
 * Range<String> stringRange = intRange.map(String::valueOf);   // ["1", "5"]
 *
 * // Collection containment
 * List<Integer> values = Arrays.asList(2, 3, 4);
 * boolean allInRange = closedRange.containsAll(values);   // true for [1, 10]
 *
 * // Range positioning tests
 * Range<Integer> before = Range.closed(-5, 0);
 * Range<Integer> after = Range.closed(15, 20);
 * boolean isBefore = before.isBeforeRange(closedRange);   // true
 * boolean isAfter = after.isAfterRange(closedRange);   // true
 *
 * // Element positioning
 * boolean startsAt1 = closedRange.isStartedBy(1);   // true for [1, 10]
 * boolean endsAt10 = closedRange.isEndedBy(10);   // true for [1, 10]
 * }</pre>
 *
 * <p><b>Endpoint System Design:</b>
 * <ul>
 *   <li><b>{@link LowerEndpoint}:</b> Represents the lower boundary with inclusion/exclusion semantics</li>
 *   <li><b>{@link UpperEndpoint}:</b> Represents the upper boundary with inclusion/exclusion semantics</li>
 *   <li><b>Endpoint Abstraction:</b> Common behavior for boundary value handling and comparison</li>
 *   <li><b>Type Safety:</b> Endpoint types ensure proper boundary semantics are maintained</li>
 * </ul>
 *
 * <p><b>BoundType Enumeration:</b>
 * <ul>
 *   <li><b>CLOSED_CLOSED:</b> {@code [min, max]} - Both endpoints included</li>
 *   <li><b>OPEN_OPEN:</b> {@code (min, max)} - Both endpoints excluded</li>
 *   <li><b>CLOSED_OPEN:</b> {@code [min, max)} - Lower included, upper excluded</li>
 *   <li><b>OPEN_CLOSED:</b> {@code (min, max]} - Lower excluded, upper included</li>
 * </ul>
 *
 * <p><b>Performance Characteristics:</b>
 * <ul>
 *   <li><b>Creation Cost:</b> O(1) - Simple object allocation with boundary validation</li>
 *   <li><b>Containment Test:</b> O(1) - Direct comparison with endpoint values</li>
 *   <li><b>Range Operations:</b> O(1) - Intersection, span, and overlap calculations</li>
 *   <li><b>Collection Containment:</b> O(n) where n is the collection size</li>
 *   <li><b>Memory Overhead:</b> Minimal - Two endpoint objects plus boundary type enum</li>
 * </ul>
 *
 * <p><b>Thread Safety Considerations:</b>
 * <ul>
 *   <li><b>Immutable Structure:</b> Range itself is completely immutable after construction</li>
 *   <li><b>Element Thread Safety:</b> Thread safety depends on the thread safety of type {@code T}</li>
 *   <li><b>Concurrent Access:</b> Safe for concurrent read access if {@code T} is thread-safe</li>
 *   <li><b>No Synchronization:</b> No internal synchronization needed due to immutability</li>
 * </ul>
 *
 * <p><b>Serialization Support:</b>
 * <ul>
 *   <li><b>Serializable Implementation:</b> Implements {@code Serializable} for persistence</li>
 *   <li><b>SerialVersionUID:</b> Stable serial version UID for version compatibility</li>
 *   <li><b>Endpoint Serialization:</b> Endpoint classes also implement Serializable</li>
 *   <li><b>Compatibility:</b> Maintains serialization compatibility across versions</li>
 * </ul>
 *
 * <p><b>Mathematical Operations:</b>
 * <ul>
 *   <li><b>Intersection:</b> Returns the overlapping portion of two ranges, or empty if no overlap</li>
 *   <li><b>Span:</b> Returns the smallest range that encompasses both input ranges</li>
 *   <li><b>Containment:</b> Tests whether a range completely contains another range</li>
 *   <li><b>Overlap:</b> Tests whether two ranges have any common elements</li>
 * </ul>
 *
 * <p><b>Null Handling:</b>
 * <ul>
 *   <li><b>Null Endpoints:</b> Endpoint values must not be {@code null}; all factory methods throw {@code IllegalArgumentException} if either endpoint is {@code null}</li>
 *   <li><b>Query Arguments:</b> Methods such as {@link #contains}, {@link #isStartedBy}, and {@link #isAfter} accept {@code null} query arguments and return {@code false} rather than throwing</li>
 *   <li><b>Range Arguments:</b> Methods such as {@link #containsRange} and {@link #isOverlappedBy} accept a {@code null} range argument and return {@code false}</li>
 * </ul>
 *
 * <p><b>Error Handling:</b>
 * <ul>
 *   <li><b>IllegalArgumentException:</b> Thrown when an endpoint is {@code null} or {@code min > max} during construction, or when {@code positionOf} receives a {@code null} element</li>
 *   <li><b>NullPointerException:</b> Thrown by {@link #span(Range)} if {@code other} is {@code null}</li>
 *   <li><b>ClassCastException:</b> Thrown when elements are not properly comparable</li>
 *   <li><b>Validation:</b> Comprehensive validation of range parameters during construction</li>
 * </ul>
 *
 * <p><b>Best Practices:</b>
 * <ul>
 *   <li>Use appropriate boundary types based on the mathematical meaning of your range</li>
 *   <li>Prefer {@code closed()} for inclusive ranges and {@code open()} for exclusive ranges</li>
 *   <li>Use {@code just()} for singleton ranges containing exactly one element</li>
 *   <li>Check {@code intersection().isPresent()} before accessing intersection results</li>
 *   <li>Consider using specialized range types for common domains (dates, numbers)</li>
 *   <li>Document the boundary semantics when using ranges in public APIs</li>
 *   <li>Ensure contained objects are immutable for full thread safety</li>
 * </ul>
 *
 * <p><b>Common Anti-Patterns to Avoid:</b>
 * <ul>
 *   <li>Creating ranges where min > max (will throw IllegalArgumentException)</li>
 *   <li>Using mutable objects as range elements in multi-threaded environments</li>
 *   <li>Ignoring boundary types and assuming all ranges are closed</li>
 *   <li>Using ranges for discrete values where collections would be more appropriate</li>
 *   <li>Calling {@code intersection().get()} without checking {@code isPresent()}</li>
 *   <li>Comparing ranges without considering boundary differences</li>
 * </ul>
 *
 * <p><b>Comparison with Alternative Approaches:</b>
 * <ul>
 *   <li><b>vs. Pair&lt;T,T&gt;:</b> Range provides domain-specific operations vs. generic tuple</li>
 *   <li><b>vs. Custom Classes:</b> Range provides standard mathematical interval operations</li>
 *   <li><b>vs. Arrays:</b> Range represents continuous intervals vs. discrete collections</li>
 *   <li><b>vs. Sets:</b> Range represents mathematical intervals vs. arbitrary element collections</li>
 * </ul>
 *
 * <p><b>Integration with Other Utilities:</b>
 * <ul>
 *   <li><b>{@link Optional}:</b> Used for intersection results that may not exist</li>
 *   <li><b>{@link Function}:</b> Used for range transformation via map operations</li>
 *   <li><b>{@link Collection}:</b> Support for testing containment of multiple elements</li>
 *   <li><b>{@link Comparable}:</b> Foundation for all range element comparison operations</li>
 * </ul>
 *
 * <p><b>Usage Examples: Time Range Processing</b>
 * <pre>{@code
 * public class TimeRangeProcessor {
 *     public List<Range<LocalDateTime>> findOverlappingMeetings(
 *             List<Meeting> meetings, Range<LocalDateTime> timeWindow) {
 *         return meetings.stream()
 *             .map(meeting -> Range.closed(meeting.getStartTime(), meeting.getEndTime()))
 *             .filter(meetingRange -> meetingRange.isOverlappedBy(timeWindow))
 *             .collect(Collectors.toList());
 *     }
 *
 *     public Optional<Range<LocalDateTime>> findLongestFreeTime(
 *             List<Range<LocalDateTime>> busyPeriods, Range<LocalDateTime> workingHours) {
 *         Range<LocalDateTime> result = workingHours;
 *         for (Range<LocalDateTime> busy : busyPeriods) {
 *             Optional<Range<LocalDateTime>> intersected = result.intersection(busy);
 *             if (intersected.isPresent()) {
 *                 result = intersected.get();
 *             }
 *         }
 *         return result.isEmpty() ? Optional.empty() : Optional.of(result);
 *     }
 *
 *     public boolean isValidBusinessHours(Range<LocalTime> proposed) {
 *         Range<LocalTime> businessHours = Range.closed(
 *             LocalTime.of(9, 0), LocalTime.of(17, 0));
 *         return businessHours.containsRange(proposed);
 *     }
 * }
 * }</pre>
 *
 * @param <T> the type of elements in this range, must implement {@code Comparable}
 * @see Immutable
 * @see Serializable
 * @see Comparable
 * @see Optional
 * @see Function
 * @see Collection
 * @see BoundType
 * @see LowerEndpoint
 * @see UpperEndpoint
 */
@com.landawn.abacus.annotation.Immutable
public final class Range<T extends Comparable<? super T>> implements Serializable, Immutable {

    @Serial
    private static final long serialVersionUID = 545606166758706779L;

    /**
     * The lower endpoint of this range. Whether the endpoint value itself is
     * included depends on the range's bound type (open or closed).
     */
    private final LowerEndpoint<T> lowerEndpoint;
    /**
     * The upper endpoint of this range. Whether the endpoint value itself is
     * included depends on the range's bound type (open or closed).
     */
    private final UpperEndpoint<T> upperEndpoint;

    /**
     * The type of bounds for this range, indicating whether endpoints are open or closed.
     */
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Range<Integer> range = Range.just(5);
     * // Creates range [5, 5]
     * range.contains(5);   // returns true
     * range.contains(4);   // returns false
     * }</pre>
     *
     * @param <T> the type of the elements in this range, must implement {@code Comparable}.
     * @param element the single value to use for both endpoints of this range, must not be {@code null}.
     * @return a new closed {@code Range} {@code [element, element]} containing only the specified element.
     * @throws IllegalArgumentException if {@code element} is {@code null}.
     * @see #closed(Comparable, Comparable)
     */
    public static <T extends Comparable<? super T>> Range<T> just(final T element) throws IllegalArgumentException {
        return closed(element, element);
    }

    /**
     * Creates an open range where both endpoints are exclusive. The range includes all values
     * strictly greater than min and strictly less than max.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Range<Integer> range = Range.open(1, 5);
     * // Creates range (1, 5)
     * range.contains(1);   // returns false
     * range.contains(3);   // returns true
     * range.contains(5);   // returns false
     * }</pre>
     *
     * @param <T> the type of the elements in this range, must implement {@code Comparable}.
     * @param min the lower bound (exclusive) of the range, must not be null.
     * @param max the upper bound (exclusive) of the range, must not be null.
     * @return a new open {@code Range} {@code (min, max)} from min (exclusive) to max (exclusive).
     * @throws IllegalArgumentException if {@code min} or {@code max} is {@code null}, or if {@code min > max}.
     * @see #closed(Comparable, Comparable)
     * @see #openClosed(Comparable, Comparable)
     * @see #closedOpen(Comparable, Comparable)
     */
    public static <T extends Comparable<? super T>> Range<T> open(final T min, final T max) throws IllegalArgumentException {
        if (min == null || max == null || min.compareTo(max) > 0) {
            throw new IllegalArgumentException("'fromInclusive' and 'toInclusive' cannot be null, or min > max");//NOSONAR
        }

        return new Range<>(new LowerEndpoint<>(min, false), new UpperEndpoint<>(max, false), BoundType.OPEN_OPEN);
    }

    /**
     * Creates a half-open range where the lower endpoint is exclusive and the upper endpoint
     * is inclusive. The range includes all values strictly greater than min and less than or
     * equal to max.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Range<Integer> range = Range.openClosed(1, 5);
     * // Creates range (1, 5]
     * range.contains(1);   // returns false
     * range.contains(3);   // returns true
     * range.contains(5);   // returns true
     * }</pre>
     *
     * @param <T> the type of the elements in this range, must implement {@code Comparable}.
     * @param min the lower bound (exclusive) of the range, must not be null.
     * @param max the upper bound (inclusive) of the range, must not be null.
     * @return a new {@code Range} {@code (min, max]} from min (exclusive) to max (inclusive).
     * @throws IllegalArgumentException if {@code min} or {@code max} is {@code null}, or if {@code min > max}.
     * @see #open(Comparable, Comparable)
     * @see #closed(Comparable, Comparable)
     * @see #closedOpen(Comparable, Comparable)
     */
    public static <T extends Comparable<? super T>> Range<T> openClosed(final T min, final T max) throws IllegalArgumentException {
        if (min == null || max == null || min.compareTo(max) > 0) {
            throw new IllegalArgumentException("'fromInclusive' and 'toInclusive' cannot be null, or min > max");
        }

        return new Range<>(new LowerEndpoint<>(min, false), new UpperEndpoint<>(max, true), BoundType.OPEN_CLOSED);
    }

    /**
     * Creates a half-open range where the lower endpoint is inclusive and the upper endpoint
     * is exclusive. The range includes all values greater than or equal to min and strictly
     * less than max.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Range<Integer> range = Range.closedOpen(1, 5);
     * // Creates range [1, 5)
     * range.contains(1);   // returns true
     * range.contains(3);   // returns true
     * range.contains(5);   // returns false
     * }</pre>
     *
     * @param <T> the type of the elements in this range, must implement {@code Comparable}.
     * @param min the lower bound (inclusive) of the range, must not be null.
     * @param max the upper bound (exclusive) of the range, must not be null.
     * @return a new {@code Range} {@code [min, max)} from min (inclusive) to max (exclusive).
     * @throws IllegalArgumentException if {@code min} or {@code max} is {@code null}, or if {@code min > max}.
     * @see #open(Comparable, Comparable)
     * @see #closed(Comparable, Comparable)
     * @see #openClosed(Comparable, Comparable)
     */
    public static <T extends Comparable<? super T>> Range<T> closedOpen(final T min, final T max) throws IllegalArgumentException {
        if (min == null || max == null || min.compareTo(max) > 0) {
            throw new IllegalArgumentException("'fromInclusive' and 'toInclusive' cannot be null, or min > max");
        }

        return new Range<>(new LowerEndpoint<>(min, true), new UpperEndpoint<>(max, false), BoundType.CLOSED_OPEN);
    }

    /**
     * Creates a closed range where both endpoints are inclusive. The range includes all values
     * greater than or equal to min and less than or equal to max.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Range<Integer> range = Range.closed(1, 5);
     * // Creates range [1, 5]
     * range.contains(1);   // returns true
     * range.contains(3);   // returns true
     * range.contains(5);   // returns true
     * }</pre>
     *
     * @param <T> the type of the elements in this range, must implement {@code Comparable}.
     * @param min the lower bound (inclusive) of the range, must not be null.
     * @param max the upper bound (inclusive) of the range, must not be null.
     * @return a new closed {@code Range} {@code [min, max]} from min (inclusive) to max (inclusive).
     * @throws IllegalArgumentException if {@code min} or {@code max} is {@code null}, or if {@code min > max}.
     * @see #open(Comparable, Comparable)
     * @see #openClosed(Comparable, Comparable)
     * @see #closedOpen(Comparable, Comparable)
     * @see #just(Comparable)
     */
    public static <T extends Comparable<? super T>> Range<T> closed(final T min, final T max) throws IllegalArgumentException {
        if (min == null || max == null || min.compareTo(max) > 0) {
            throw new IllegalArgumentException("'fromInclusive' and 'toInclusive' cannot be null, or min > max");
        }

        return new Range<>(new LowerEndpoint<>(min, true), new UpperEndpoint<>(max, true), BoundType.CLOSED_CLOSED);
    }

    /**
     * Transforms this range by applying the given mapping function to both endpoints.
     * The resulting range maintains the same bound types (open/closed) as the original range.
     * The mapper function is applied to both the lower and upper endpoints to create a new
     * range with potentially different element types.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Range<Integer> intRange = Range.closed(1, 5);
     * Range<String> strRange = intRange.map(String::valueOf);
     * // Creates range ["1", "5"]
     * }</pre>
     *
     * @param <U> the type of elements in the resulting range, must implement {@code Comparable}.
     * @param mapper the function to apply to both endpoints, must not be {@code null} and must not return {@code null} values.
     * @return a new {@code Range<U>} with transformed endpoints maintaining the same bound types.
     * @throws IllegalArgumentException if the mapped lower endpoint is greater than the mapped upper endpoint.
     * @throws NullPointerException if {@code mapper} is {@code null}, or if {@code mapper} returns {@code null} for either endpoint.
     * @see #boundType()
     */
    public <U extends Comparable<? super U>> Range<U> map(final Function<? super T, ? extends U> mapper) {
        final U newLower = mapper.apply(lowerEndpoint.value);
        final U newUpper = mapper.apply(upperEndpoint.value);

        if (newLower.compareTo(newUpper) > 0) {
            throw new IllegalArgumentException(
                    "The mapped lower endpoint (" + newLower + ") must not be greater than the mapped upper endpoint (" + newUpper + ")");
        }

        return new Range<>(new LowerEndpoint<>(newLower, lowerEndpoint.isClosed), new UpperEndpoint<>(newUpper, upperEndpoint.isClosed), boundType);
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
     * @return the {@link BoundType} enum value representing this range's endpoint types.
     * @see BoundType
     * @see #lowerEndpoint()
     * @see #upperEndpoint()
     */
    public BoundType boundType() {
        return boundType;
    }

    /**
     * Returns the lower endpoint (minimum value) of this range. This value represents
     * the lower bound of the range, which may be either inclusive or exclusive depending
     * on the range's bound type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Range<Integer> range = Range.closed(1, 5);
     * Integer lower = range.lowerEndpoint();   // returns 1
     * }</pre>
     *
     * @return the lower endpoint value of this range.
     * @see #upperEndpoint()
     * @see #boundType()
     */
    public T lowerEndpoint() {
        return lowerEndpoint.value;
    }

    /**
     * Returns the upper endpoint (maximum value) of this range. This value represents
     * the upper bound of the range, which may be either inclusive or exclusive depending
     * on the range's bound type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Range<Integer> range = Range.closed(1, 5);
     * Integer upper = range.upperEndpoint();   // returns 5
     * }</pre>
     *
     * @return the upper endpoint value of this range.
     * @see #lowerEndpoint()
     * @see #boundType()
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Range<Integer> range = Range.closedOpen(1, 5);
     * range.contains(1);      // returns true (lower bound is inclusive)
     * range.contains(3);      // returns true
     * range.contains(5);      // returns false (upper bound is exclusive)
     * range.contains(null);   // returns false
     * }</pre>
     *
     * @param valueToFind the element to check for containment, {@code null} returns false.
     * @return {@code true} if the specified element occurs within this range's bounds, {@code false} otherwise.
     * @see #containsAll(Collection)
     * @see #containsAny(Collection)
     * @see #containsRange(Range)
     */
    public boolean contains(final T valueToFind) {
        if (valueToFind == null) {
            return false;
        }

        return lowerEndpoint.includes(valueToFind) && upperEndpoint.includes(valueToFind);
    }

    /**
     * Determines whether this range contains <em>all</em> elements in the specified collection.
     *
     * <p>This method iterates over the given collection and checks each element using
     * {@link #contains}. The evaluation short-circuits and returns {@code false}
     * as soon as an element is found that is not contained within this range.</p>
     *
     * <p>An empty or {@code null} collection is considered trivially satisfied and
     * results in {@code true}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Range<Integer> range = Range.closed(1, 10);
     *
     * range.containsAll(Arrays.asList(2, 5, 8));    // true
     * range.containsAll(Arrays.asList(2, 5, 15));   // false (15 is outside the range)
     * range.containsAll(Collections.emptyList());  // true
     * }</pre>
     *
     * @param c the collection of elements to test; may be {@code null} or empty
     * @return {@code true} if every element in {@code c} is contained within this range,
     *         or if {@code c} is {@code null} or empty; {@code false} otherwise
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
     * Determines whether this range contains <em>any</em> element in the specified collection.
     *
     * <p>This method iterates over the given collection and checks each element using
     * {@link #contains}. The evaluation short-circuits and returns {@code true}
     * as soon as a contained element is found.</p>
     *
     * <p>If the collection is {@code null} or empty, this method returns {@code false}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Range<Integer> range = Range.closed(1, 10);
     *
     * range.containsAny(Arrays.asList(15, 20, 8));  // true (8 is within the range)
     * range.containsAny(Arrays.asList(15, 20));     // false
     * range.containsAny(Collections.emptyList());  // false
     * }</pre>
     *
     * @param c the collection of elements to test; may be {@code null} or empty
     * @return {@code true} if at least one element in {@code c} is contained within this range;
     *         {@code false} if none are contained or if {@code c} is {@code null} or empty
     */
    public boolean containsAny(final Collection<? extends T> c) {
        if (N.isEmpty(c)) {
            return false;
        }

        for (final T e : c) {
            if (contains(e)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks whether this range starts with the specified element. Returns {@code true} only if
     * the lower endpoint is closed (inclusive) and equals the specified element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Range<Integer> range1 = Range.closed(5, 10);
     * range1.isStartedBy(5);   // returns true
     *
     * Range<Integer> range2 = Range.open(5, 10);
     * range2.isStartedBy(5);   // returns false (lower bound is exclusive)
     * }</pre>
     *
     * @param element the element to check against the lower endpoint, {@code null} returns false
     * @return {@code true} if this range has a closed lower endpoint that equals the specified element
     * @see #isEndedBy(Comparable)
     * @see #lowerEndpoint()
     */
    public boolean isStartedBy(final T element) {
        if (element == null) {
            return false;
        }

        return lowerEndpoint.isClosed && lowerEndpoint.compareTo(element) == 0;
    }

    /**
     * Checks whether this range ends with the specified element. Returns {@code true} only if
     * the upper endpoint is closed (inclusive) and equals the specified element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Range<Integer> range1 = Range.closed(5, 10);
     * range1.isEndedBy(10);   // returns true
     *
     * Range<Integer> range2 = Range.closedOpen(5, 10);
     * range2.isEndedBy(10);   // returns false (upper bound is exclusive)
     * }</pre>
     *
     * @param element the element to check against the upper endpoint, {@code null} returns false
     * @return {@code true} if this range has a closed upper endpoint that equals the specified element
     * @see #isStartedBy(Comparable)
     * @see #upperEndpoint()
     */
    public boolean isEndedBy(final T element) {
        if (element == null) {
            return false;
        }

        return upperEndpoint.isClosed && upperEndpoint.compareTo(element) == 0;
    }

    /**
     * Checks whether this range lies entirely after the specified element, i.e. every value
     * in this range is greater than the element. This is {@code true} when the element falls
     * below the lower endpoint, taking the lower bound type into account: for a closed lower
     * endpoint the element must be strictly less than the lower value, while for an open lower
     * endpoint an element equal to the lower value also counts as being before the range.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Range<Integer> range = Range.closed(5, 10);
     * range.isAfter(3);   // returns true
     * range.isAfter(5);   // returns false (5 is included in range)
     * range.isAfter(7);   // returns false (7 is within range)
     *
     * Range<Integer> openRange = Range.open(5, 10);
     * openRange.isAfter(5);   // returns true (5 is excluded by the open lower bound)
     * }</pre>
     *
     * @param element the element to check, {@code null} returns false
     * @return {@code true} if this entire range is after (greater than) the specified element
     */
    public boolean isAfter(final T element) {
        if (element == null) {
            return false;
        }

        return !lowerEndpoint.includes(element);
    }

    /**
     * Checks whether this range lies entirely before the specified element, i.e. every value
     * in this range is less than the element. This is {@code true} when the element falls
     * above the upper endpoint, taking the upper bound type into account: for a closed upper
     * endpoint the element must be strictly greater than the upper value, while for an open
     * upper endpoint an element equal to the upper value also counts as being after the range.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Range<Integer> range = Range.closed(5, 10);
     * range.isBefore(12);   // returns true
     * range.isBefore(10);   // returns false (10 is included in range)
     * range.isBefore(7);    // returns false (7 is within range)
     *
     * Range<Integer> openRange = Range.open(5, 10);
     * openRange.isBefore(10);   // returns true (10 is excluded by the open upper bound)
     * }</pre>
     *
     * @param element the element to check, {@code null} returns false
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Range<Integer> range = Range.closed(5, 10);
     * range.positionOf(3);    // returns 1 (range is after 3)
     * range.positionOf(7);    // returns 0 (7 is within range)
     * range.positionOf(12);   // returns -1 (range is before 12)
     * }</pre>
     *
     * @param element the element to compare against this range, must not be null
     * @return {@code -1} if this range is entirely before the element, {@code 0} if the element
     *         is contained within this range, or {@code 1} if this range is entirely after the element
     * @throws IllegalArgumentException if element is null
     * @see #isBefore(Comparable)
     * @see #isAfter(Comparable)
     * @see #contains(Comparable)
     */
    public int positionOf(final T element) throws IllegalArgumentException {
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
     * For an open endpoint in the other range, this range's corresponding endpoint may share the
     * same value (since the other range excludes that boundary value) or extend beyond it.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Range<Integer> range1 = Range.closed(1, 10);
     * Range<Integer> range2 = Range.closed(3, 7);
     * Range<Integer> range3 = Range.closed(5, 15);
     * Range<Integer> range4 = Range.open(1, 10);
     *
     * range1.containsRange(range2);   // returns true
     * range1.containsRange(range3);   // returns false (extends beyond upper bound)
     * range1.containsRange(range4);   // returns true (open range (1,10) is within [1,10])
     * range1.containsRange(null);     // returns false
     * }</pre>
     *
     * @param other the range to check for containment, {@code null} returns false
     * @return {@code true} if this range contains all elements of the specified range
     * @see #isOverlappedBy(Range)
     * @see #contains(Comparable)
     */
    public boolean containsRange(final Range<T> other) {
        if (other == null) {
            return false;
        }

        // Check lower bound containment
        final int lowerCmp = lowerEndpoint.value.compareTo(other.lowerEndpoint.value);
        final boolean lowerContained;
        if (lowerCmp < 0) {
            lowerContained = true; // this lower bound is strictly less
        } else if (lowerCmp == 0) {
            // Equal bounds: this contains other's lower if this is closed OR other is open
            lowerContained = lowerEndpoint.isClosed || !other.lowerEndpoint.isClosed;
        } else {
            lowerContained = false; // this lower bound is greater
        }

        // Check upper bound containment
        final int upperCmp = upperEndpoint.value.compareTo(other.upperEndpoint.value);
        final boolean upperContained;
        if (upperCmp > 0) {
            upperContained = true; // this upper bound is strictly greater
        } else if (upperCmp == 0) {
            // Equal bounds: this contains other's upper if this is closed OR other is open
            upperContained = upperEndpoint.isClosed || !other.upperEndpoint.isClosed;
        } else {
            upperContained = false; // this upper bound is less
        }

        return lowerContained && upperContained;
    }

    /**
     * Checks whether this range is completely after the specified range, meaning every value
     * in this range is greater than every value in the other range with no shared elements.
     *
     * <p>The check accounts for bound types: if the other range's upper endpoint is closed
     * (inclusive), this range must start strictly above that value; if it is open (exclusive),
     * this range may start at the same value.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Range<Integer> range1 = Range.closed(10, 15);
     * Range<Integer> range2 = Range.closed(1, 5);
     * Range<Integer> range3 = Range.closed(1, 10);
     *
     * range1.isAfterRange(range2);   // returns true  (10 > 5)
     * range1.isAfterRange(range3);   // returns false (ranges share the value 10)
     *
     * Range<Integer> range4 = Range.closed(10, 15);
     * Range<Integer> range5 = Range.open(1, 10);     // upper bound 10 is exclusive
     * range4.isAfterRange(range5);   // returns true  (range5 excludes 10, range4 starts at 10)
     * }</pre>
     *
     * @param other the range to compare against, {@code null} returns {@code false}
     * @return {@code true} if this range is completely after the specified range with no shared elements
     * @see #isBeforeRange(Range)
     * @see #isOverlappedBy(Range)
     */
    public boolean isAfterRange(final Range<T> other) {
        if (other == null) {
            return false;
        }
        return other.upperEndpoint.isClosed ? isAfter(other.upperEndpoint.value) : lowerEndpoint.compareTo(other.upperEndpoint.value) >= 0;
    }

    /**
     * Checks whether this range is completely before the specified range, meaning every value
     * in this range is less than every value in the other range with no shared elements.
     *
     * <p>The check accounts for bound types: if the other range's lower endpoint is closed
     * (inclusive), this range must end strictly below that value; if it is open (exclusive),
     * this range may end at the same value.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Range<Integer> range1 = Range.closed(1, 5);
     * Range<Integer> range2 = Range.closed(10, 15);
     * Range<Integer> range3 = Range.closed(5, 10);
     *
     * range1.isBeforeRange(range2);   // returns true  (5 < 10)
     * range1.isBeforeRange(range3);   // returns false (ranges share the value 5)
     *
     * Range<Integer> range4 = Range.closed(1, 5);
     * Range<Integer> range5 = Range.open(5, 10);     // lower bound 5 is exclusive
     * range4.isBeforeRange(range5);   // returns true  (range5 excludes 5, range4 ends at 5)
     * }</pre>
     *
     * @param other the range to compare against, {@code null} returns {@code false}
     * @return {@code true} if this range is completely before the specified range with no shared elements
     * @see #isAfterRange(Range)
     * @see #isOverlappedBy(Range)
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Range<Integer> range1 = Range.closed(1, 5);
     * Range<Integer> range2 = Range.closed(3, 8);
     * Range<Integer> range3 = Range.closed(6, 10);
     * Range<Integer> range4 = Range.open(5, 10);
     *
     * range1.isOverlappedBy(range2);   // returns true (overlap from 3 to 5)
     * range1.isOverlappedBy(range3);   // returns false (no overlap)
     * range1.isOverlappedBy(range4);   // returns false (ranges touch at 5 but not both inclusive)
     * }</pre>
     *
     * @param other the range to test for overlap, {@code null} returns false
     * @return {@code true} if the specified range overlaps with this range; otherwise, false
     * @see #intersection(Range)
     * @see #isBeforeRange(Range)
     * @see #isAfterRange(Range)
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Range<Integer> range1 = Range.closed(1, 5);
     * Range<Integer> range2 = Range.closed(3, 8);
     * Optional<Range<Integer>> intersection = range1.intersection(range2);
     * // Returns Optional containing Range.closed(3, 5)
     *
     * Range<Integer> range3 = Range.closed(6, 10);
     * Optional<Range<Integer>> noIntersection = range1.intersection(range3);
     * // Returns Optional.empty()
     *
     * Range<Integer> range4 = Range.open(1, 5);
     * Range<Integer> range5 = Range.closed(1, 5);
     * Optional<Range<Integer>> intersection2 = range4.intersection(range5);
     * // Returns Optional containing Range.open(1, 5) - more restrictive bounds
     * }</pre>
     *
     * @param other the range to intersect with this range; a {@code null} value is treated
     *              as non-overlapping and yields {@code Optional.empty()}
     * @return an {@code Optional} containing the intersection range if the ranges overlap;
     *         {@code Optional.empty()} if they do not overlap (or {@code other} is {@code null});
     *         or an {@code Optional} containing this range if the two ranges are equal
     * @see #isOverlappedBy(Range)
     * @see #span(Range)
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Range<Integer> range1 = Range.closed(1, 3);
     * Range<Integer> range2 = Range.closed(5, 7);
     * Range<Integer> span = range1.span(range2);
     * // Returns Range.closed(1, 7), which also includes value 4 that is in neither input range
     *
     * Range<Integer> range3 = Range.open(1, 3);
     * Range<Integer> range4 = Range.open(5, 7);
     * Range<Integer> span2 = range3.span(range4);
     * // Returns Range.open(1, 7)
     * }</pre>
     *
     * @param other the range to span with this range, must not be {@code null}
     * @return the minimal range that contains all values from both input ranges
     * @throws NullPointerException if {@code other} is {@code null}
     * @see #intersection(Range)
     */
    public Range<T> span(final Range<T> other) {
        final int lowerCmp = N.compare(lowerEndpoint.value, other.lowerEndpoint.value);
        final LowerEndpoint<T> newLowerEndpoint;

        if (lowerCmp < 0) {
            newLowerEndpoint = lowerEndpoint;
        } else if (lowerCmp > 0) {
            newLowerEndpoint = other.lowerEndpoint;
        } else {
            newLowerEndpoint = lowerEndpoint.isClosed ? lowerEndpoint : other.lowerEndpoint;
        }

        final int upperCmp = N.compare(upperEndpoint.value, other.upperEndpoint.value);
        final UpperEndpoint<T> newUpperEndpoint;

        if (upperCmp > 0) {
            newUpperEndpoint = upperEndpoint;
        } else if (upperCmp < 0) {
            newUpperEndpoint = other.upperEndpoint;
        } else {
            newUpperEndpoint = upperEndpoint.isClosed ? upperEndpoint : other.upperEndpoint;
        }

        BoundType boundType = null;//NOSONAR

        if (newLowerEndpoint.isClosed) {
            boundType = newUpperEndpoint.isClosed ? BoundType.CLOSED_CLOSED : BoundType.CLOSED_OPEN;
        } else {
            boundType = newUpperEndpoint.isClosed ? BoundType.OPEN_CLOSED : BoundType.OPEN_OPEN;
        }

        return new Range<>(newLowerEndpoint, newUpperEndpoint, boundType);
    }

    /**
     * Checks if this range is empty. A range is empty if both endpoints are the same
     * value and at least one endpoint is open. Only a closed range with equal endpoints
     * contains that single endpoint value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Range<Integer> emptyRange = Range.open(5, 5);
     * emptyRange.isEmpty();   // returns true
     *
     * Range<Integer> halfOpenEmptyRange = Range.closedOpen(5, 5);
     * halfOpenEmptyRange.isEmpty();   // returns true
     *
     * Range<Integer> pointRange = Range.closed(5, 5);
     * pointRange.isEmpty();   // returns false (contains the value 5)
     *
     * Range<Integer> normalRange = Range.open(5, 6);
     * normalRange.isEmpty();   // returns false
     * }</pre>
     *
     * @return {@code true} if this range contains no values, {@code false} otherwise
     */
    public boolean isEmpty() {
        //NOSONAR
        return (!lowerEndpoint.isClosed || !upperEndpoint.isClosed) && lowerEndpoint.compareTo(upperEndpoint.value) == 0;
    }

    // Basics
    //--------------------------------------------------------------------

    /**
     * Compares this range to another object for equality. Two ranges are equal if they
     * have the same lower and upper endpoint values and the same bound type at each
     * endpoint (i.e. the same open/closed inclusiveness on both ends).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Range<Integer> range1 = Range.closed(1, 5);
     * Range<Integer> range2 = Range.closed(1, 5);
     * Range<Integer> range3 = Range.open(1, 5);
     *
     * range1.equals(range2);   // returns true
     * range1.equals(range3);   // returns false (different bound types)
     * }</pre>
     *
     * @param obj the reference object with which to compare
     * @return {@code true} if this object is equal to the obj argument; {@code false} otherwise
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
     * Enumerates the four possible combinations of lower and upper boundary inclusiveness
     * for a {@link Range}. A <em>closed</em> bound includes its endpoint value while an
     * <em>open</em> bound excludes it.
     *
     * @see Range#boundType()
     */
    public enum BoundType {

        /**
         * Both bounds are open (exclusive). Represents range (a, b) where neither a nor b are included.
         */
        OPEN_OPEN,

        /**
         * Lower bound is open (exclusive), upper bound is closed (inclusive). Represents range (a, b] where only b is included.
         */
        OPEN_CLOSED,

        /**
         * Lower bound is closed (inclusive), upper bound is open (exclusive). Represents range [a, b) where only a is included.
         */
        CLOSED_OPEN,

        /**
         * Both bounds are closed (inclusive). Represents range [a, b] where both a and b are included.
         */
        CLOSED_CLOSED
    }

    /**
     * Abstract base for a range boundary, pairing an endpoint value with a flag indicating
     * whether the boundary is closed (inclusive) or open (exclusive). Subclasses define how
     * inclusion is evaluated for the lower and upper sides of a range.
     *
     * @param <T> the type of elements in this endpoint, must implement {@code Comparable}
     * @see LowerEndpoint
     * @see UpperEndpoint
     */
    abstract static class Endpoint<T extends Comparable<? super T>> implements Serializable {

        @Serial
        private static final long serialVersionUID = -1404748904424344410L;

        /** The boundary value for this endpoint. */
        final T value; //NOSONAR

        /** {@code true} if this endpoint is closed (inclusive); {@code false} if open (exclusive). */
        final boolean isClosed;

        /**
         * Constructs an endpoint with the given boundary value and inclusiveness flag.
         *
         * @param value    the boundary value for this endpoint
         * @param isClosed {@code true} for a closed (inclusive) endpoint, {@code false} for an open (exclusive) endpoint
         */
        protected Endpoint(final T value, final boolean isClosed) {
            this.value = value;
            this.isClosed = isClosed;
        }

        /**
         * Compares this endpoint's value to the specified value.
         *
         * @param value the value to compare against this endpoint
         * @return a negative integer, zero, or a positive integer if this endpoint value is less than, equal to, or greater than the specified value
         */
        public int compareTo(final T value) {
            return N.compare(this.value, value);
        }

        /**
         * Checks whether the specified value is included by this endpoint.
         *
         * @param value the value to check for inclusion
         * @return {@code true} if this endpoint includes the specified value, {@code false} otherwise
         */
        public abstract boolean includes(T value);

    }

    /**
     * The lower (minimum) boundary of a {@link Range}. A value is included by this endpoint
     * when it is greater than the endpoint value, or equal to it when the boundary is closed.
     *
     * @param <T> the type of elements in this endpoint, must implement {@code Comparable}
     * @see UpperEndpoint
     */
    static class LowerEndpoint<T extends Comparable<? super T>> extends Endpoint<T> {

        @Serial
        private static final long serialVersionUID = -1369183906861608859L;

        /**
         * Constructs a lower endpoint with the given boundary value and inclusiveness flag.
         *
         * @param value    the lower boundary value
         * @param isClosed {@code true} for a closed (inclusive) lower bound, {@code false} for an open (exclusive) lower bound
         */
        LowerEndpoint(final T value, final boolean isClosed) {
            super(value, isClosed);
        }

        /**
         * Checks whether the specified value is included by this lower endpoint.
         *
         * @param value the value to check
         * @return {@code true} if the value is greater than (or equal to, if closed) this lower endpoint value
         */
        @Override
        public boolean includes(final T value) {
            return isClosed ? N.compare(value, this.value) >= 0 : N.compare(value, this.value) > 0;
        }

        /**
         * Returns a hash code value for this lower endpoint based on its value and closed/open flag.
         *
         * @return a hash code value for this object
         */
        @Override
        public int hashCode() {
            final int result = isClosed ? 0 : 1;
            return 37 * result + N.hashCode(value);
        }

        /**
         * Compares this lower endpoint to another object for equality.
         * Two lower endpoints are equal if they have the same boundary value and the same closed/open flag.
         *
         * @param obj the reference object with which to compare
         * @return {@code true} if {@code obj} is a {@code LowerEndpoint} with the same value and closed/open flag; {@code false} otherwise
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

        /**
         * Returns a string representation of this lower endpoint.
         * Closed endpoints are formatted as {@code [value}, open endpoints as {@code (value}.
         *
         * @return a string representation of this lower endpoint
         */
        @Override
        public String toString() {
            return (isClosed ? "[" : "(") + N.toString(value);
        }
    }

    /**
     * The upper (maximum) boundary of a {@link Range}. A value is included by this endpoint
     * when it is less than the endpoint value, or equal to it when the boundary is closed.
     *
     * @param <T> the type of elements in this endpoint, must implement {@code Comparable}
     * @see LowerEndpoint
     */
    static class UpperEndpoint<T extends Comparable<? super T>> extends Endpoint<T> {

        @Serial
        private static final long serialVersionUID = 3180376045860768477L;

        /**
         * Constructs an upper endpoint with the given boundary value and inclusiveness flag.
         *
         * @param value    the upper boundary value
         * @param isClosed {@code true} for a closed (inclusive) upper bound, {@code false} for an open (exclusive) upper bound
         */
        UpperEndpoint(final T value, final boolean isClosed) {
            super(value, isClosed);
        }

        /**
         * Checks whether the specified value is included by this upper endpoint.
         *
         * @param value the value to check
         * @return {@code true} if the value is less than (or equal to, if closed) this upper endpoint value
         */
        @Override
        public boolean includes(final T value) {
            return isClosed ? N.compare(value, this.value) <= 0 : N.compare(value, this.value) < 0;
        }

        /**
         * Returns a hash code value for this upper endpoint based on its value and closed/open flag.
         *
         * @return a hash code value for this object
         */
        @Override
        public int hashCode() {
            final int result = isClosed ? 0 : 1;
            return 37 * result + N.hashCode(value);
        }

        /**
         * Compares this upper endpoint to another object for equality.
         * Two upper endpoints are equal if they have the same boundary value and the same closed/open flag.
         *
         * @param obj the reference object with which to compare
         * @return {@code true} if {@code obj} is an {@code UpperEndpoint} with the same value and closed/open flag; {@code false} otherwise
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

        /**
         * Returns a string representation of this upper endpoint.
         * Closed endpoints are formatted as {@code value]}, open endpoints as {@code value)}.
         *
         * @return a string representation of this upper endpoint
         */
        @Override
        public String toString() {
            return N.toString(value) + (isClosed ? "]" : ")");
        }
    }
}
