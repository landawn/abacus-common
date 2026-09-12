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

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
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
 * The Range itself is immutable and uses natural ordering via {@link Comparable}. It is
 * thread-safe for concurrent reads when the endpoint values of type {@code T} are themselves
 * thread-safe (or are not mutated after the range is created). There is no custom-comparator
 * option; ordering always follows each element's {@code compareTo}.</p>
 *
 * <p><b>Ordering vs. equality:</b> every membership and boundary decision
 * ({@link #contains}, {@link #overlaps}, {@link #intersection}, {@link #span}, {@link #isEmpty})
 * is made with {@code T.compareTo}, but {@link #equals(Object)} and {@link #hashCode()} compare the
 * endpoint <em>objects</em> with {@code equals}. When {@code T}'s {@code compareTo} is inconsistent
 * with its {@code equals}, the two views diverge:</p>
 * <pre>{@code
 * Range<BigDecimal> a = Range.closed(new BigDecimal("5.0"),  new BigDecimal("6.0"));
 * Range<BigDecimal> b = Range.closed(new BigDecimal("5.00"), new BigDecimal("6.00"));
 * a.equals(b);                       // false - the endpoint objects are not equal
 * a.contains(new BigDecimal("5.5")); // true, and so does b - the same interval
 * a.span(b);                         // [5.0, 6.0]   - keeps this range's endpoint objects
 * b.span(a);                         // [5.00, 6.00] - keeps b's, so span is not `equals`-commutative
 * }</pre>
 * <p>The results always describe the same interval; only which equal-by-{@code compareTo} endpoint
 * object is retained differs, and no canonical choice exists for a general {@code Comparable}. The same
 * applies to {@link #intersection(Range)}. Prefer a {@code T} whose {@code compareTo} is consistent with
 * {@code equals} when ranges are used as map keys or compared with {@code equals}.</p>
 *
 * <p><b>Natural ordering of floating-point endpoints:</b> {@code Double}/{@code Float} endpoints follow
 * {@link Double#compareTo(Double)}, not {@code ==}. {@code NaN} therefore sorts above every other value,
 * so {@code Range.closed(1.0, Double.NaN)} is a valid range containing every value numerically
 * {@code >= 1.0}, positive infinity, and {@code NaN} itself. Negative infinity is excluded (while {@code Range.closed(Double.NaN, 1.0)}
 * is rejected as {@code min > max}); and {@code -0.0} sorts below {@code 0.0}, so
 * {@code Range.just(0.0).contains(-0.0)} is {@code false}. Screen out {@code NaN} before constructing a
 * range if that is not the intent.</p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Immutable Design:</b> All instances are immutable, ensuring thread safety and preventing accidental modification</li>
 *   <li><b>Flexible Boundaries:</b> Support for open, closed, and mixed boundary types (open-closed, closed-open)</li>
 *   <li><b>Type Safety:</b> Generic constraints ensure only comparable types can be used</li>
 *   <li><b>Mathematical Operations:</b> Intersection, span, containment, and overlap testing</li>
 *   <li><b>Null Safety:</b> Proper handling of {@code null} values with clear semantics</li>
 *   <li><b>Performance Optimized:</b> Efficient algorithms for range operations and comparisons</li>
 *   <li><b>Serializable:</b> Supports Java serialization for persistence and distributed systems</li>
 *   <li><b>Endpoint Mapping:</b> {@link #mapEndpoints(Function)} rebuilds a range from transformed endpoints</li>
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
 * Range<Integer> closedRange = Range.closed(1, 10);      // returns [1, 10] - includes 1 and 10
 * Range<Integer> openRange = Range.open(1, 10);          // returns (1, 10) - excludes 1 and 10
 * Range<Integer> halfOpen = Range.closedOpen(1, 10);     // returns [1, 10) - includes 1, excludes 10
 * Range<Integer> halfClosed = Range.openClosed(1, 10);   // returns (1, 10] - excludes 1, includes 10
 *
 * // Single element ranges
 * Range<String> single = Range.just("value");   // returns [value, value] - contains only "value"
 *
 * // Containment testing
 * boolean contains5 = closedRange.contains(5);   // returns true - 5 is in [1, 10]
 * boolean contains1 = openRange.contains(1);     // returns false - 1 is not in (1, 10)
 * boolean contains10 = halfOpen.contains(10);    // returns false - 10 is not in [1, 10)
 *
 * // Range operations
 * Range<Integer> other = Range.closed(5, 15);
 * boolean overlaps = closedRange.overlaps(other);                            // returns true - ranges overlap
 * Optional<Range<Integer>> intersection = closedRange.intersection(other);   // returns [5, 10]
 * Range<Integer> span = closedRange.span(other);                             // returns [1, 15] - encompasses both ranges
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
 * Range<String> stringRange = intRange.mapEndpoints(String::valueOf);   // maps the endpoints only: ["1", "5"]
 *
 * // Collection containment
 * List<Integer> values = Arrays.asList(2, 3, 4);
 * boolean allInRange = closedRange.containsAll(values);   // returns true for [1, 10]
 *
 * // Range positioning tests
 * Range<Integer> before = Range.closed(-5, 0);
 * Range<Integer> after = Range.closed(15, 20);
 * boolean isBefore = before.isBeforeRange(closedRange);   // returns true
 * boolean isAfter = after.isAfterRange(closedRange);      // returns true
 *
 * // Element positioning
 * boolean startsAt1 = closedRange.isStartedBy(1);   // returns true for [1, 10]
 * boolean endsAt10 = closedRange.isEndedBy(10);     // returns true for [1, 10]
 * }</pre>
 *
 * <p><b>Endpoint System Design:</b>
 * <ul>
 *   <li><b>Lower endpoint:</b> the lower boundary value plus its inclusion/exclusion flag</li>
 *   <li><b>Upper endpoint:</b> the upper boundary value plus its inclusion/exclusion flag</li>
 *   <li><b>Endpoint Abstraction:</b> Common behavior for boundary value handling and comparison</li>
 *   <li><b>Type Safety:</b> Endpoint types ensure proper boundary semantics are maintained</li>
 * </ul>
 * <p>The endpoint classes themselves are an implementation detail and are not part of the public API;
 * read the boundary values with {@link #lowerEndpoint()} / {@link #upperEndpoint()} and their
 * inclusion flags with {@link #boundType()}.</p>
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
 * <p><b>Empty Ranges:</b>
 * <ul>
 *   <li>A range is {@linkplain #isEmpty() empty} when its endpoints are equal and at least one bound
 *       is open, e.g. {@code (5, 5)}, {@code [5, 5)}, {@code (5, 5]}</li>
 *   <li>{@link #contains} is {@code false} for every element, and {@link #overlaps} is {@code false}
 *       against every range, including itself</li>
 *   <li>{@link #containsRange} returns {@code true} for an empty argument, and {@link #span} ignores
 *       an empty operand &mdash; the empty set is a subset of every set and contributes no values</li>
 *   <li>{@link #isBefore} and {@link #isAfter} still answer from the endpoints, so away from the
 *       shared endpoint value exactly one of them is {@code true} &mdash; they do <em>not</em> both
 *       report {@code true} for every element. At the shared endpoint value itself the answer depends
 *       on the bound types: {@code (5, 5)} reports {@code true} from both, {@code [5, 5)} only from
 *       {@link #isBefore}, and {@code (5, 5]} only from {@link #isAfter}</li>
 *   <li>{@link #elementCompareTo} nevertheless rejects an empty range for <em>every</em> element: its
 *       {@code 0} result means "this range contains the element", which an empty range can never do,
 *       and the three-valued result has no fourth value left for "not contained, on neither side". Use
 *       {@link #isBefore} / {@link #isAfter} directly when a side is all that is needed</li>
 *   <li>{@link #isBeforeRange} and {@link #isAfterRange} likewise answer from the endpoints and never
 *       consult {@link #isEmpty()}, so an empty operand still takes a side: {@code [1, 2]} is before
 *       {@code [100, 100)} and after {@code [0, 0)}. Only where an empty range is compared at its own
 *       value &mdash; most visibly against itself &mdash; do both report {@code true}</li>
 * </ul>
 *
 * <p><b>Endpoints, not elements:</b> a range is defined by its two ordered endpoints and their
 * inclusivity, and has no notion of a successor, so it cannot recognise that a discrete domain has no
 * value inside it. {@code Range.open(5, 6)} over {@code Integer} contains no {@code Integer}, yet it
 * is not {@linkplain #isEmpty() degenerate} and it {@linkplain #overlaps overlaps} itself. Every
 * relation on this class &mdash; {@link #overlaps}, {@link #intersection}, {@link #span},
 * {@link #containsRange}, {@link #isBeforeRange}, {@link #isAfterRange} &mdash; is interval algebra on
 * endpoints; only {@link #contains}, {@link #containsAll} and {@link #containsAny} test actual values.
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
 *   <li><b>Deliberate Divergence:</b> {@link #elementCompareTo} rejects a {@code null} element with
 *       {@code IllegalArgumentException} and {@link #span(Range)} rejects a {@code null} range with
 *       {@code NullPointerException}, because for those two operations there is no sensible
 *       "not found" answer to return</li>
 *   <li><b>Range Arguments:</b> Methods such as {@link #containsRange} and {@link #overlaps} accept a {@code null} range argument and return {@code false}</li>
 * </ul>
 *
 * <p><b>Error Handling:</b>
 * <ul>
 *   <li><b>IllegalArgumentException:</b> Thrown when an endpoint is {@code null} or {@code min > max} during construction, or when {@code elementCompareTo} receives a {@code null} element</li>
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
 *   <li><b>{@link Function}:</b> Used for endpoint transformation via {@link #mapEndpoints(Function)}</li>
 *   <li><b>{@link Collection}:</b> Support for testing containment of multiple elements</li>
 *   <li><b>{@link Comparable}:</b> Foundation for all range element comparison operations</li>
 * </ul>
 *
 * <p><b>Usage Examples: Time Range Processing</b></p>
 * <pre>{@code
 * public class TimeRangeProcessor {
 *     public List<Range<LocalDateTime>> findOverlappingMeetings(
 *             List<Meeting> meetings, Range<LocalDateTime> timeWindow) {
 *         return meetings.stream()
 *             .map(meeting -> Range.closed(meeting.getStartTime(), meeting.getEndTime()))
 *             .filter(meetingRange -> meetingRange.overlaps(timeWindow))
 *             .collect(Collectors.toList());
 *     }
 *
 *     public Optional<Range<LocalDateTime>> findCommonBusyWindow(
 *             List<Range<LocalDateTime>> busyPeriods, Range<LocalDateTime> workingHours) {
 *         Range<LocalDateTime> result = workingHours;
 *         for (Range<LocalDateTime> busy : busyPeriods) {
 *             Optional<Range<LocalDateTime>> intersected = result.intersection(busy);
 *             if (!intersected.isPresent()) {
 *                 return Optional.empty();
 *             }
 *             result = intersected.get();
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
        if (element == null) {
            throw new IllegalArgumentException("'element' cannot be null");
        }

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
     * @param min the lower bound (exclusive) of the range, must not be {@code null}.
     * @param max the upper bound (exclusive) of the range, must not be {@code null}.
     * @return a new open {@code Range} {@code (min, max)} from min (exclusive) to max (exclusive).
     * @throws IllegalArgumentException if {@code min} or {@code max} is {@code null}, or if {@code min > max}.
     * @see #closed(Comparable, Comparable)
     * @see #openClosed(Comparable, Comparable)
     * @see #closedOpen(Comparable, Comparable)
     */
    public static <T extends Comparable<? super T>> Range<T> open(final T min, final T max) throws IllegalArgumentException {
        checkBounds(min, max);

        return new Range<>(new LowerEndpoint<>(min, false), new UpperEndpoint<>(max, false), BoundType.OPEN_OPEN);
    }

    /**
     * Validates the endpoints shared by all two-argument factory methods.
     *
     * @param <T> the endpoint type
     * @param min the lower bound
     * @param max the upper bound
     * @throws IllegalArgumentException if either bound is {@code null}, or {@code min > max}
     */
    private static <T extends Comparable<? super T>> void checkBounds(final T min, final T max) throws IllegalArgumentException {
        if (min == null) {
            throw new IllegalArgumentException("'min' cannot be null");//NOSONAR
        }

        if (max == null) {
            throw new IllegalArgumentException("'max' cannot be null");
        }

        if (min.compareTo(max) > 0) {
            throw new IllegalArgumentException("'min' (" + min + ") must not be greater than 'max' (" + max + ")");
        }
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
     * @param min the lower bound (exclusive) of the range, must not be {@code null}.
     * @param max the upper bound (inclusive) of the range, must not be {@code null}.
     * @return a new {@code Range} {@code (min, max]} from min (exclusive) to max (inclusive).
     * @throws IllegalArgumentException if {@code min} or {@code max} is {@code null}, or if {@code min > max}.
     * @see #open(Comparable, Comparable)
     * @see #closed(Comparable, Comparable)
     * @see #closedOpen(Comparable, Comparable)
     */
    public static <T extends Comparable<? super T>> Range<T> openClosed(final T min, final T max) throws IllegalArgumentException {
        checkBounds(min, max);

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
     * @param min the lower bound (inclusive) of the range, must not be {@code null}.
     * @param max the upper bound (exclusive) of the range, must not be {@code null}.
     * @return a new {@code Range} {@code [min, max)} from min (inclusive) to max (exclusive).
     * @throws IllegalArgumentException if {@code min} or {@code max} is {@code null}, or if {@code min > max}.
     * @see #open(Comparable, Comparable)
     * @see #closed(Comparable, Comparable)
     * @see #openClosed(Comparable, Comparable)
     */
    public static <T extends Comparable<? super T>> Range<T> closedOpen(final T min, final T max) throws IllegalArgumentException {
        checkBounds(min, max);

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
     * @param min the lower bound (inclusive) of the range, must not be {@code null}.
     * @param max the upper bound (inclusive) of the range, must not be {@code null}.
     * @return a new closed {@code Range} {@code [min, max]} from min (inclusive) to max (inclusive).
     * @throws IllegalArgumentException if {@code min} or {@code max} is {@code null}, or if {@code min > max}.
     * @see #open(Comparable, Comparable)
     * @see #openClosed(Comparable, Comparable)
     * @see #closedOpen(Comparable, Comparable)
     * @see #just(Comparable)
     */
    public static <T extends Comparable<? super T>> Range<T> closed(final T min, final T max) throws IllegalArgumentException {
        checkBounds(min, max);

        return new Range<>(new LowerEndpoint<>(min, true), new UpperEndpoint<>(max, true), BoundType.CLOSED_CLOSED);
    }

    /**
     * Builds a new range by applying the given function to <b>this range's two endpoints only</b>,
     * keeping each endpoint's bound type (open/closed) unchanged. The mapper is invoked exactly twice
     * &mdash; once for the lower endpoint value and once for the upper endpoint value.
     *
     * <p><b>The result is only meaningful for an order-preserving (monotonically non-decreasing)
     * mapper.</b> This method maps endpoints, not members: it cannot compute the image of the values
     * between them. If {@code mapper} does not preserve order, the returned range still satisfies
     * {@code lower <= upper} (otherwise an {@code IllegalArgumentException} is thrown) but it will not
     * describe the image of this range:</p>
     * <pre>{@code
     * Range.closed(2, 10).mapEndpoints(v -> v % 7);   // returns [2, 3] - NOT the image of [2, 10]
     * }</pre>
     *
     * <p>The same caution applies when the mapping changes the ordering in use, for example mapping
     * numbers to their decimal strings switches from numeric to lexicographic order.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Range<Integer> intRange = Range.closed(1, 5);
     * Range<String> strRange = intRange.mapEndpoints(String::valueOf);
     * // Creates range ["1", "5"]
     *
     * Range<Long> millis = Range.closedOpen(1L, 5L);
     * Range<Long> micros = millis.mapEndpoints(v -> v * 1_000L);   // [1000, 5000) - bound types kept
     * }</pre>
     *
     * @param <U> the type of elements in the resulting range, must implement {@code Comparable}.
     * @param mapper an order-preserving function applied to both endpoints; it must not be {@code null}
     *        and must not return {@code null} for either endpoint.
     * @return a new {@code Range<U>} with transformed endpoints maintaining the same bound types.
     * @throws IllegalArgumentException if {@code mapper} is {@code null} or returns {@code null} for either endpoint,
     *         or if the mapped lower endpoint is greater than the mapped upper endpoint.
     * @see #boundType()
     */
    public <U extends Comparable<? super U>> Range<U> mapEndpoints(final Function<? super T, ? extends U> mapper) throws IllegalArgumentException {
        N.checkArgNotNull(mapper, cs.mapper);

        final U newLower = N.checkArgNotNull(mapper.apply(lowerEndpoint.value), "mapper returned null for the lower endpoint");
        final U newUpper = N.checkArgNotNull(mapper.apply(upperEndpoint.value), "mapper returned null for the upper endpoint");

        if (newLower.compareTo(newUpper) > 0) {
            throw new IllegalArgumentException(
                    "The mapped lower endpoint (" + newLower + ") must not be greater than the mapped upper endpoint (" + newUpper + ")");
        }

        return new Range<>(new LowerEndpoint<>(newLower, lowerEndpoint.isClosed), new UpperEndpoint<>(newUpper, upperEndpoint.isClosed), boundType);
    }

    /**
     * Builds a new range by applying the given function to this range's two endpoints.
     *
     * @param <U> the type of elements in the resulting range, must implement {@code Comparable}.
     * @param mapper an order-preserving function applied to both endpoints; it must not be {@code null}
     *        and must not return {@code null} for either endpoint.
     * @return a new {@code Range<U>} with transformed endpoints maintaining the same bound types.
     * @throws IllegalArgumentException if {@code mapper} is {@code null} or returns {@code null} for either endpoint,
     *         or if the mapped lower endpoint is greater than the mapped upper endpoint.
     * @deprecated renamed to {@link #mapEndpoints(Function)}, which states that only the two endpoints
     *             are mapped and that the mapper must preserve order.
     */
    @Deprecated
    public <U extends Comparable<? super U>> Range<U> map(final Function<? super T, ? extends U> mapper) throws IllegalArgumentException {
        return mapEndpoints(mapper);
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Range<Integer> range = Range.closed(1, 5);
     * Range.BoundType type = range.boundType();   // returns Range.BoundType.CLOSED_CLOSED
     *
     * Range.open(1, 5).boundType();         // returns BoundType.OPEN_OPEN
     * Range.closedOpen(1, 5).boundType();   // returns BoundType.CLOSED_OPEN
     * Range.openClosed(1, 5).boundType();   // returns BoundType.OPEN_CLOSED
     * }</pre>
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
     * @param valueToFind the element to check for containment, {@code null} returns {@code false}.
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
     * range.containsAll(Arrays.asList(2, 5, 8));    // returns true
     * range.containsAll(Arrays.asList(2, 5, 15));   // returns false (15 is outside the range)
     * range.containsAll(Collections.emptyList());   // returns true
     * }</pre>
     *
     * @param c the collection of elements to test; may be {@code null} or empty
     * @return {@code true} if every element in {@code c} is contained within this range,
     *         or if {@code c} is {@code null} or empty; {@code false} otherwise
     * @see #contains(Comparable)
     * @see #containsAny(Collection)
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
     * range.containsAny(Arrays.asList(15, 20, 8));   // returns true (8 is within the range)
     * range.containsAny(Arrays.asList(15, 20));      // returns false
     * range.containsAny(Collections.emptyList());    // returns false
     * }</pre>
     *
     * @param c the collection of elements to test; may be {@code null} or empty
     * @return {@code true} if at least one element in {@code c} is contained within this range;
     *         {@code false} if none are contained or if {@code c} is {@code null} or empty
     * @see #contains(Comparable)
     * @see #containsAll(Collection)
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

        return lowerEndpoint.isClosed && lowerEndpoint.compareToValue(element) == 0;
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

        return upperEndpoint.isClosed && upperEndpoint.compareToValue(element) == 0;
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
     * <p><b>Empty ranges:</b> this method answers from the lower endpoint alone, so it keeps working on
     * an {@linkplain #isEmpty() empty} range such as {@code (5, 5)}: it returns {@code true} for every
     * element below the shared endpoint value and {@code false} for every element above it. At the
     * shared value itself the open/closed lower bound decides &mdash; {@code (5, 5).isAfter(5)} and
     * {@code (5, 5].isAfter(5)} are {@code true} (the lower bound excludes 5), while
     * {@code [5, 5).isAfter(5)} is {@code false}. Only for {@code (x, x)} at {@code x} do this method
     * and {@link #isBefore} both report {@code true}. {@link #elementCompareTo(Comparable)} rejects
     * empty ranges outright, because its {@code 0} result would claim containment.</p>
     *
     * @param element the element to check, {@code null} returns false
     * @return {@code true} if this entire range is after (greater than) the specified element
     * @see #isBefore(Comparable)
     * @see #elementCompareTo(Comparable)
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
     * <p><b>Empty ranges:</b> this method answers from the upper endpoint alone, so it keeps working on
     * an {@linkplain #isEmpty() empty} range such as {@code (5, 5)}: it returns {@code true} for every
     * element above the shared endpoint value and {@code false} for every element below it. At the
     * shared value itself the open/closed upper bound decides &mdash; {@code (5, 5).isBefore(5)} and
     * {@code [5, 5).isBefore(5)} are {@code true} (the upper bound excludes 5), while
     * {@code (5, 5].isBefore(5)} is {@code false}. Only for {@code (x, x)} at {@code x} do this method
     * and {@link #isAfter} both report {@code true}. {@link #elementCompareTo(Comparable)} rejects
     * empty ranges outright, because its {@code 0} result would claim containment.</p>
     *
     * @param element the element to check, {@code null} returns false
     * @return {@code true} if this entire range is before (less than) the specified element
     * @see #isAfter(Comparable)
     * @see #elementCompareTo(Comparable)
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
     * @throws IllegalArgumentException if element is null.
     * @see #isBefore(Comparable)
     * @see #isAfter(Comparable)
     * @see #contains(Comparable)
     * @deprecated the returned sign is the position of the <em>range</em> relative to the element, which is
     *             the inverse of Apache Commons Lang's {@code Range.elementCompareTo(T)} and of
     *             {@link Comparable#compareTo}. Use {@link #elementCompareTo(Comparable)}, which returns
     *             {@code -1} for an element below the range and {@code 1} for one above it.
     *             <p><b>Two differences when migrating, not just the sign:</b> negate the result, and note
     *             that this method stays total on an {@linkplain #isEmpty() empty} range (it returns
     *             {@code -1} or {@code 1} from the endpoints) whereas {@code elementCompareTo}
     *             <em>throws</em> {@link IllegalStateException} for an empty range. Guard with
     *             {@link #isEmpty()}, or use {@link #isBefore(Comparable)} / {@link #isAfter(Comparable)},
     *             if empty ranges can reach the call site.</p>
     */
    @Deprecated
    public int positionOf(final T element) throws IllegalArgumentException {
        if (element == null) {
            // Library convention: reject null with IllegalArgumentException (not the NPE Comparable would imply)
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

    /**
     * Compares the specified element to this range, using the same sign convention as
     * {@link Comparable#compareTo}: negative when the element lies <em>below</em> this range, zero when
     * this range contains it, and positive when it lies <em>above</em> this range.
     *
     * <p>Returns:</p>
     * <ul>
     *   <li>{@code -1} if the element is below this range (this range is entirely after it)</li>
     *   <li>{@code 0} if the element is contained in this range</li>
     *   <li>{@code 1} if the element is above this range (this range is entirely before it)</li>
     * </ul>
     *
     * <p>This matches Apache Commons Lang's {@code Range.elementCompareTo(T)}. It is the sign-inverse of
     * the deprecated {@link #positionOf(Comparable)}, which reports the position of the <em>range</em>
     * relative to the element rather than the position of the element.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Range<Integer> range = Range.closed(5, 10);
     * range.elementCompareTo(3);    // returns -1 (3 is below the range)
     * range.elementCompareTo(7);    // returns 0  (7 is within the range)
     * range.elementCompareTo(12);   // returns 1  (12 is above the range)
     * }</pre>
     *
     * @param element the element to compare against this range, must not be {@code null}
     * @return {@code -1} if the element is below this range, {@code 0} if it is contained in this
     *         range, or {@code 1} if it is above this range
     * @throws IllegalArgumentException if {@code element} is {@code null}
     * @throws IllegalStateException if this range is {@linkplain #isEmpty() empty}, for <em>every</em>
     *         element: a {@code 0} result asserts containment, which an empty range can never satisfy,
     *         and the three-valued result has no way to say "not contained, on neither side". Call
     *         {@link #isBefore(Comparable)} / {@link #isAfter(Comparable)} instead when only a side is
     *         needed &mdash; those keep working on an empty range
     * @see #contains(Comparable)
     * @see #isBefore(Comparable)
     * @see #isAfter(Comparable)
     */
    public int elementCompareTo(final T element) throws IllegalArgumentException, IllegalStateException {
        if (element == null) {
            // Library convention: reject null with IllegalArgumentException (not the NPE Comparable would imply)
            throw new IllegalArgumentException("'element' cannot be null");
        }

        // An empty range contains nothing, so the 0 result -- which means "this range contains the
        // element" -- can never be correct for it, and the three-valued return has no fourth value
        // left to say "not contained, on neither side". Fail loudly rather than pick a side.
        // (isBefore/isAfter remain usable on an empty range; they answer from a single endpoint.)
        if (isEmpty()) {
            throw new IllegalStateException("Cannot compare an element to the empty range " + this);
        }

        if (isAfter(element)) {
            return -1;
        } else if (isBefore(element)) {
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
     * <p>An empty range is contained by every range, because it has no elements that can fall
     * outside this range.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Range<Integer> range1 = Range.closed(1, 10);
     * Range<Integer> range2 = Range.closed(3, 7);
     * Range<Integer> range3 = Range.closed(5, 15);
     * Range<Integer> range4 = Range.open(1, 10);
     *
     * range1.containsRange(range2);             // returns true
     * range1.containsRange(range3);             // returns false (extends beyond upper bound)
     * range1.containsRange(range4);             // returns true (open range (1,10) is within [1,10])
     * range1.containsRange(Range.open(20, 20)); // returns true (the other range is empty)
     * range1.containsRange(null);               // returns false
     * }</pre>
     *
     * @param other the range to check for containment, {@code null} returns false
     * @return {@code true} if this range contains all elements of the specified range
     * @see #overlaps(Range)
     * @see #contains(Comparable)
     */
    public boolean containsRange(final Range<T> other) {
        if (other == null) {
            return false;
        }

        if (other.isEmpty()) {
            return true;
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
     * <p><b>Degenerate ranges are ordered by their endpoints, not treated as absent.</b> This method
     * and {@link #isBeforeRange(Range)} compare endpoints and never consult {@link #isEmpty()}, so a
     * {@linkplain #isEmpty() degenerate} operand still takes a side: {@code [1, 2]} is before
     * {@code [100, 100)} and after {@code [0, 0)}. Where a degenerate range sits at the same value it
     * is compared against &mdash; most visibly a degenerate range against itself &mdash; both
     * predicates report {@code true}, because a range with no values is trivially on both sides of
     * itself. Guard with {@link #isEmpty()} if the two predicates must be mutually exclusive.</p>
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
     * Range<Integer> range5 = Range.open(1, 10);   // upper bound 10 is exclusive
     * range4.isAfterRange(range5);                 // returns true  (range5 excludes 10, range4 starts at 10)
     *
     * Range<Integer> empty = Range.closedOpen(5, 5);
     * empty.isAfterRange(empty);                   // returns true, and so does isBeforeRange
     * Range.closed(1, 2).isAfterRange(empty);      // returns false - [1, 2] sits below 5
     * }</pre>
     *
     * @param other the range to compare against, {@code null} returns {@code false}
     * @return {@code true} if this range is completely after the specified range with no shared elements
     * @see #isBeforeRange(Range)
     * @see #overlaps(Range)
     * @see #isEmpty()
     */
    public boolean isAfterRange(final Range<T> other) {
        if (other == null) {
            return false;
        }
        return other.upperEndpoint.isClosed ? isAfter(other.upperEndpoint.value) : lowerEndpoint.compareToValue(other.upperEndpoint.value) >= 0;
    }

    /**
     * Checks whether this range is completely before the specified range, meaning every value
     * in this range is less than every value in the other range with no shared elements.
     *
     * <p>The check accounts for bound types: if the other range's lower endpoint is closed
     * (inclusive), this range must end strictly below that value; if it is open (exclusive),
     * this range may end at the same value.</p>
     *
     * <p><b>Degenerate ranges are ordered by their endpoints, not treated as absent</b> &mdash; see
     * {@link #isAfterRange(Range)} for the full rule and examples. In particular a
     * {@linkplain #isEmpty() degenerate} range is reported both before and after itself.</p>
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
     * Range<Integer> range5 = Range.open(5, 10);   // lower bound 5 is exclusive
     * range4.isBeforeRange(range5);                // returns true  (range5 excludes 5, range4 ends at 5)
     *
     * Range<Integer> empty = Range.closedOpen(5, 5);
     * empty.isBeforeRange(empty);                  // returns true, and so does isAfterRange
     * Range.closed(1, 2).isBeforeRange(empty);     // returns true - [1, 2] sits below 5
     * }</pre>
     *
     * @param other the range to compare against, {@code null} returns {@code false}
     * @return {@code true} if this range is completely before the specified range with no shared elements
     * @see #isAfterRange(Range)
     * @see #overlaps(Range)
     * @see #isEmpty()
     */
    public boolean isBeforeRange(final Range<T> other) {
        if (other == null) {
            return false;
        }

        return other.lowerEndpoint.isClosed ? isBefore(other.lowerEndpoint.value) : upperEndpoint.compareToValue(other.lowerEndpoint.value) <= 0;
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
     * range1.overlaps(range2);   // returns true (overlap from 3 to 5)
     * range1.overlaps(range3);   // returns false (no overlap)
     * range1.overlaps(range4);   // returns false (ranges touch at 5 but not both inclusive)
     * }</pre>
     *
     * @param other the range to test for overlap, {@code null} returns false
     * @return {@code true} if the specified range overlaps with this range; otherwise, false
     * @see #intersection(Range)
     * @see #isBeforeRange(Range)
     * @see #isAfterRange(Range)
     * @deprecated Use {@link #overlaps(Range)}. The overlap relation is symmetric, so the active name is clearer.
     */
    @Deprecated
    public boolean isOverlappedBy(final Range<T> other) {
        return overlaps(other);
    }

    /**
     * Checks whether this range overlaps with the specified range.
     * This is the canonical overlap operation for ranges.
     *
     * <p>Two ranges overlap when neither is {@linkplain #isEmpty() degenerate} and their
     * endpoint-delimited intervals intersect: this range's lower endpoint must not lie above the
     * other's upper endpoint, nor the other's lower endpoint above this one's upper endpoint. Where
     * the two touch at a single shared value, that value counts only if <em>both</em> of the bounds
     * meeting there are closed &mdash; so {@code [1, 5]} overlaps {@code [5, 10]} but not
     * {@code (5, 10]}, and {@code [5, 5]} overlaps itself.</p>
     *
     * <p><b>This is interval algebra on the endpoints, not a search for a shared element.</b> A
     * {@code Range} has no notion of a successor, so it cannot see that two ranges over a discrete
     * domain share no value: {@code Range.open(5, 6)} over {@code Integer} overlaps itself even though
     * {@link #contains(Comparable)} is {@code false} for every {@code Integer}. See {@link #isEmpty()}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Range<Integer> range = Range.closed(1, 5);
     * range.overlaps(Range.closed(3, 8));      // returns true
     * range.overlaps(Range.closed(6, 10));     // returns false
     * range.overlaps(Range.closed(5, 10));     // returns true  - both bounds at 5 are closed
     * range.overlaps(Range.openClosed(5, 10)); // returns false - the other excludes 5
     * range.overlaps(Range.open(3, 3));        // returns false - the other is degenerate
     * range.overlaps(null);                    // returns false
     * }</pre>
     *
     * @param other the range to test for overlap, {@code null} returns false
     * @return {@code true} if the specified range overlaps with this range; otherwise, false
     * @see #intersection(Range)
     * @see #isEmpty()
     */
    public boolean overlaps(final Range<T> other) {
        //NOSONAR
        return other != null && !isEmpty() && !other.isEmpty() && !isAfterRange(other) && !isBeforeRange(other);
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
     *         {@code Optional.empty()} if they do not overlap (or {@code other} is {@code null},
     *         or either range is empty); or an {@code Optional} containing this range if the
     *         two ranges are equal and non-empty
     * @see #overlaps(Range)
     * @see #span(Range)
     */
    public Optional<Range<T>> intersection(final Range<T> other) {
        if (!this.overlaps(other)) {
            return Optional.empty();
        } else if (this.equals(other)) {
            return Optional.of(this);
        }

        final LowerEndpoint<T> newLowerEndpoint = lowerEndpoint.includes(other.lowerEndpoint.value) ? other.lowerEndpoint : lowerEndpoint;
        final UpperEndpoint<T> newUpperEndpoint = upperEndpoint.includes(other.upperEndpoint.value) ? other.upperEndpoint : upperEndpoint;

        final BoundType newBoundType = BoundType.of(newLowerEndpoint.isClosed, newUpperEndpoint.isClosed);

        return Optional.of(new Range<>(newLowerEndpoint, newUpperEndpoint, newBoundType));
    }

    /**
     * Returns the minimal range that encloses both this range and the specified range.
     * The span is the smallest range that contains every value contained in either of the
     * input ranges. If the input ranges are connected (overlapping or touching), the span
     * is their union. If they are not connected, the span includes values between the ranges
     * that are not in either input range.
     *
     * <p>The span operation takes the minimum of the lower endpoints and the maximum of the
     * upper endpoints, preserving the most inclusive bound type at each endpoint. An empty
     * operand contributes no values, so spanning a non-empty range with an empty range returns
     * the non-empty operand unchanged.</p>
     *
     * <p>This operation is commutative, associative, and idempotent <em>up to the equivalence induced by
     * {@code compareTo}</em>. When {@code T}'s {@code compareTo} is inconsistent with its {@code equals}
     * (as for {@link java.math.BigDecimal}), {@code a.span(b)} and {@code b.span(a)} can select different
     * &mdash; but numerically equal &mdash; endpoint objects, so the two results describe the same
     * interval yet are not {@link #equals(Object) equal}. See <b>Ordering vs. equality</b> in the class
     * documentation.</p>
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
     * @throws IllegalArgumentException if {@code other} is {@code null}
     * @see #intersection(Range)
     */
    public Range<T> span(final Range<T> other) throws IllegalArgumentException {
        N.checkArgNotNull(other, cs.other);

        if (isEmpty()) {
            if (!other.isEmpty()) {
                return other;
            }

            // Every empty range represents the same empty set, but Range equality also records
            // endpoints and bound types. Select one deterministically so span is commutative whenever
            // compareTo agrees with equals; when it does not, the two orders can still yield equal-but-
            // not-`equals` results (see the "Ordering vs. equality" note in the class documentation).
            final int emptyCmp = lowerEndpoint.value.compareTo(other.lowerEndpoint.value);
            return emptyCmp < 0 || (emptyCmp == 0 && boundType.ordinal() <= other.boundType.ordinal()) ? this : other;
        } else if (other.isEmpty()) {
            return this;
        }

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

        final BoundType newBoundType = BoundType.of(newLowerEndpoint.isClosed, newUpperEndpoint.isClosed);

        return new Range<>(newLowerEndpoint, newUpperEndpoint, newBoundType);
    }

    /**
     * Checks whether this range is <i>degenerate</i>: its two endpoints are equal and at least one of
     * them is exclusive, so no value can satisfy both bounds. Only a closed range with equal
     * endpoints, such as {@code [5, 5]}, contains its single endpoint value.
     *
     * <p><b>This is a test on the endpoints, not on the elements of a discrete domain.</b> A
     * {@code Range} is defined purely by its ordered endpoints and their inclusivity; it has no notion
     * of a successor, so it cannot tell that {@code Range.open(5, 6)} holds no {@code Integer}. That
     * range reports {@code false} here, and {@link #contains(Comparable)} returns {@code false} for
     * every {@code Integer}. Check emptiness in a discrete domain by testing the values you care
     * about, not with this method.</p>
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
     * @return {@code true} if this range is degenerate - equal endpoints with at least one exclusive
     *         bound - and therefore contains no value of any domain; {@code false} otherwise, which
     *         does <em>not</em> guarantee that a discrete domain has a value inside it
     */
    public boolean isEmpty() {
        //NOSONAR
        return (!lowerEndpoint.isClosed || !upperEndpoint.isClosed) && lowerEndpoint.compareToValue(upperEndpoint.value) == 0;
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

        if (obj instanceof final Range<?> other) {
            return N.equals(lowerEndpoint, other.lowerEndpoint) && N.equals(upperEndpoint, other.upperEndpoint);
        }

        return false;
    }

    /**
     * Returns a hash code value for this range, derived from both endpoint values and their
     * inclusiveness. Equal ranges (see {@link #equals(Object)}) have equal hash codes; ranges that
     * differ only in bound type generally do not.
     *
     * <p>As always, the hash code is only as stable as the endpoints' own {@code hashCode()}: for
     * endpoint types whose hash is not value-based (or not stable across JVM runs), neither is this
     * one.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Range<Integer> range1 = Range.closed(1, 5);
     * Range<Integer> range2 = Range.closed(1, 5);
     * int hash1 = range1.hashCode();
     * int hash2 = range2.hashCode();
     * assert hash1 == hash2;
     *
     * Range<Integer> range3 = Range.open(1, 5);
     * boolean mayDiffer = range1.hashCode() != range3.hashCode();
     * }</pre>
     *
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        // No getClass() term: Range is final, so it would distinguish nothing, and Class does not
        // override hashCode() - mixing in its identity hash made equal ranges hash differently from
        // one JVM run to the next.
        int result = 17;

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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Range<Integer> range = Range.closed(1, 5);
     * range.toString();                    // returns "[1, 5]"
     *
     * Range.open(1, 5).toString();         // returns "(1, 5)"
     * Range.closedOpen(1, 5).toString();   // returns "[1, 5)"
     * Range.openClosed(1, 5).toString();   // returns "(1, 5]"
     * }</pre>
     *
     * @return a string representation of this range
     */
    @Override
    public String toString() {
        return lowerEndpoint.toString() + ", " + upperEndpoint.toString();
    }

    /**
     * Restores a {@code Range} from a stream, rejecting any state no factory method could have
     * produced.
     *
     * <p>The bounds are validated by the {@code open}/{@code closed}/{@code just} factories, not by
     * this class's private constructor, and deserialization runs neither. Without this check a
     * hand-crafted or corrupted stream could yield a reversed range such as {@code [3, 2]}, which
     * {@link #contains(Comparable)} rejects for every value while {@link #isEmpty()} reports
     * {@code false}, or a range whose {@link BoundType} disagrees with its own endpoints.</p>
     *
     * @param in the stream to read this range from
     * @throws ClassNotFoundException if the class of a serialized object cannot be found
     * @throws IOException if reading from the stream fails
     * @throws InvalidObjectException if an endpoint or the bound type is missing, if the endpoint
     *         values are not mutually comparable, if the lower endpoint value is greater than the
     *         upper endpoint value, or if the bound type does not match the endpoints' inclusivity
     */
    @Serial
    private void readObject(final ObjectInputStream in) throws ClassNotFoundException, IOException, InvalidObjectException {
        in.defaultReadObject();

        if (lowerEndpoint == null || upperEndpoint == null || boundType == null) {
            throw new InvalidObjectException("A Range must have a lower endpoint, an upper endpoint and a bound type");
        }

        if (lowerEndpoint.value == null || upperEndpoint.value == null) {
            throw new InvalidObjectException("Range endpoint values must not be null");
        }

        // A stream can also pair endpoints of unrelated types, which surfaces here as a
        // ClassCastException from compareTo. Report that as the same rejection rather than letting an
        // implementation detail of the comparison escape.
        try {
            if (lowerEndpoint.value.compareTo(upperEndpoint.value) > 0) {
                throw new InvalidObjectException("'min' (" + lowerEndpoint.value + ") must not be greater than 'max' (" + upperEndpoint.value + ")");
            }
        } catch (final ClassCastException e) {
            final InvalidObjectException ioe = new InvalidObjectException(
                    "Range endpoint values are not mutually comparable: " + lowerEndpoint.value + " and " + upperEndpoint.value);
            ioe.initCause(e);
            throw ioe;
        }

        if (boundType != BoundType.of(lowerEndpoint.isClosed, upperEndpoint.isClosed)) {
            throw new InvalidObjectException("The bound type " + boundType + " does not match the endpoints " + this);
        }
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
        CLOSED_CLOSED;

        /**
         * Returns the {@code BoundType} for the given endpoint inclusiveness flags.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * BoundType.of(true, false);    // returns CLOSED_OPEN
         * BoundType.of(false, false);   // returns OPEN_OPEN
         * }</pre>
         *
         * @param lowerClosed {@code true} if the lower bound is closed (inclusive)
         * @param upperClosed {@code true} if the upper bound is closed (inclusive)
         * @return the matching {@code BoundType}, never {@code null}
         */
        public static BoundType of(final boolean lowerClosed, final boolean upperClosed) {
            if (lowerClosed) {
                return upperClosed ? CLOSED_CLOSED : CLOSED_OPEN;
            }

            return upperClosed ? OPEN_CLOSED : OPEN_OPEN;
        }
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
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Range<Integer> range = Range.closed(1, 5);
         * // The lower endpoint value is 1, upper endpoint value is 5
         * // LowerEndpoint.compareTo(1) returns 0 (equal), compareTo(0) returns positive (endpoint value is greater than the argument)
         * // UpperEndpoint.compareTo(5) returns 0 (equal), compareTo(3) returns positive (endpoint value is greater than the argument)
         *
         * // Used internally: N.compare(endpoint.value, value)
         * }</pre>
         *
         * @param value the value to compare against this endpoint
         * @return a negative integer, zero, or a positive integer if this endpoint value is less than, equal to, or greater than the specified value
         */
        public int compareToValue(final T value) {
            return N.compare(this.value, value);
        }

        /**
         * Returns a hash code derived from this endpoint's value and its closed/open flag.
         *
         * @return a hash code value for this endpoint
         */
        @Override
        public int hashCode() {
            final int result = isClosed ? 0 : 1;
            return 37 * result + N.hashCode(value);
        }

        /**
         * Compares this endpoint to another object for equality. Two endpoints are equal when they are
         * of the same concrete endpoint class (lower vs. upper) and hold the same boundary value with
         * the same closed/open flag.
         *
         * @param obj the reference object with which to compare
         * @return {@code true} if {@code obj} is an endpoint of the same side with the same value and
         *         closed/open flag; {@code false} otherwise
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            // getClass() rather than instanceof: a LowerEndpoint and an UpperEndpoint holding the same
            // value and flag must not compare equal, since [x, ... and ...,x] mean different things.
            if (obj == null || !getClass().equals(obj.getClass())) {
                return false;
            }

            final Endpoint<?> other = (Endpoint<?>) obj;

            return isClosed == other.isClosed && N.equals(value, other.value);
        }

        /**
         * Checks whether the specified value is included by this endpoint.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // LowerEndpoint (closed, value=1).includes(1) returns true (>= 1)
         * // LowerEndpoint (open, value=1).includes(1) returns false (> 1)
         * // UpperEndpoint (closed, value=5).includes(5) returns true (<= 5)
         * // UpperEndpoint (open, value=5).includes(5) returns false (< 5)
         * }</pre>
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
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // LowerEndpoint (closed, value=1): includes values >= 1
         * // LowerEndpoint includes(1) returns true, includes(0) returns false, includes(2) returns true
         *
         * // LowerEndpoint (open, value=1): includes values > 1
         * // LowerEndpoint includes(1) returns false, includes(2) returns true
         * }</pre>
         *
         * @param value the value to check
         * @return {@code true} if the value is greater than (or equal to, if closed) this lower endpoint value
         */
        @Override
        public boolean includes(final T value) {
            return isClosed ? N.compare(value, this.value) >= 0 : N.compare(value, this.value) > 0;
        }

        /**
         * Returns a string representation of this lower endpoint.
         * Closed endpoints are formatted as {@code [value}, open endpoints as {@code (value}.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // LowerEndpoint(value=1, isClosed=true).toString() returns "[1"
         * // LowerEndpoint(value=1, isClosed=false).toString() returns "(1"
         * }</pre>
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
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // UpperEndpoint (closed, value=5): includes values <= 5
         * // UpperEndpoint includes(5) returns true, includes(3) returns true, includes(6) returns false
         *
         * // UpperEndpoint (open, value=5): includes values < 5
         * // UpperEndpoint includes(5) returns false, includes(4) returns true
         * }</pre>
         *
         * @param value the value to check
         * @return {@code true} if the value is less than (or equal to, if closed) this upper endpoint value
         */
        @Override
        public boolean includes(final T value) {
            return isClosed ? N.compare(value, this.value) <= 0 : N.compare(value, this.value) < 0;
        }

        /**
         * Returns a string representation of this upper endpoint.
         * Closed endpoints are formatted as {@code value]}, open endpoints as {@code value)}.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // UpperEndpoint(value=5, isClosed=true).toString() returns "5]"
         * // UpperEndpoint(value=5, isClosed=false).toString() returns "5)"
         * }</pre>
         *
         * @return a string representation of this upper endpoint
         */
        @Override
        public String toString() {
            return N.toString(value) + (isClosed ? "]" : ")");
        }
    }
}
