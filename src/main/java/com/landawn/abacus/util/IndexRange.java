/*
 * Copyright (C) 2026 HaiYang Li
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

import java.util.function.IntConsumer;
import java.util.stream.IntStream;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.u.Optional;

/**
 * Represents a half-open range of character indices, {@code [start, end)}, within a string.
 *
 * <p>The {@code start} index is inclusive and the {@code end} index is exclusive, so an
 * {@code IndexRange} can be passed directly to {@link String#substring(int, int)}:
 * {@code str.substring(range.start(), range.end())}. This mirrors the span contract of
 * {@link java.util.regex.MatchResult#start()} and {@link java.util.regex.MatchResult#end()},
 * where {@code end()} is the offset after the last matched character.</p>
 *
 * <p>Instances of this record are returned by the {@code Strings.substringIndicesBetween(...)}
 * methods to describe the position of each matched substring. As a {@code record}, it is
 * immutable and provides value-based {@code equals} and {@code hashCode}. Its {@code toString}
 * renders the half-open interval in mathematical notation, {@code "[start, end)"}.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * IndexRange range = new IndexRange(2, 5);
 * range.start();                 // returns 2
 * range.end();                   // returns 5
 * "a[bcd]e".substring(range.start(), range.end());   // returns "bcd"
 *
 * new IndexRange(0, 0);          // valid: empty range
 * new IndexRange(-1, 3);         // throws IllegalArgumentException (negative start)
 * new IndexRange(4, 2);          // throws IllegalArgumentException (end < start)
 *
 * // Range operations
 * new IndexRange(2, 5).containsRange(new IndexRange(3, 4));   // returns true
 * new IndexRange(2, 5).isOverlappedBy(new IndexRange(4, 8));  // returns true
 * new IndexRange(2, 5).intersection(new IndexRange(4, 8));    // returns u.Optional.of(new IndexRange(4, 5))
 * new IndexRange(2, 5).span(new IndexRange(4, 8));            // returns new IndexRange(2, 8)
 * new IndexRange(2, 5).isEmpty();                             // returns false
 * }</pre>
 *
 * @param start the inclusive start index of the range, must be {@code >= 0}
 * @param end the exclusive end index of the range, must be {@code >= start}
 * @see Strings#substringIndicesBetween(String, char, char)
 * @see java.util.regex.MatchResult#start()
 * @see java.util.regex.MatchResult#end()
 */
public record IndexRange(int start, int end) {

    /**
     * Constructs an {@code IndexRange} with the specified start (inclusive) and end (exclusive) indices.
     *
     * @param start the inclusive start index of the range, must be {@code >= 0}
     * @param end the exclusive end index of the range, must be {@code >= start}
     * @throws IllegalArgumentException if {@code start} is negative or {@code end} is less than {@code start}
     */
    public IndexRange {
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("Invalid index range: [" + start + ", " + end + ")");
        }
    }

    /**
     * Creates an {@code IndexRange} with the specified start (inclusive) and end (exclusive) indices.
     * This is a static factory alias for the canonical constructor.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexRange range = IndexRange.of(2, 5);
     * range.toString();   // returns "[2, 5)"
     * }</pre>
     *
     * @param start the inclusive start index of the range, must be {@code >= 0}
     * @param end the exclusive end index of the range, must be {@code >= start}
     * @return a new {@code IndexRange} {@code [start, end)}
     * @throws IllegalArgumentException if {@code start} is negative or {@code end} is less than {@code start}
     */
    public static IndexRange of(final int start, final int end) {
        return new IndexRange(start, end);
    }

    /**
     * Converts this index range to a {@code Range<Integer>} with the same bounds.
     * The returned range is half-open, {@code [start, end)}, with an inclusive lower
     * endpoint and an exclusive upper endpoint, so it covers exactly the same indices
     * as this range.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Range<Integer> range = new IndexRange(1, 5).toRange();
     * range.contains(1);   // returns true
     * range.contains(4);   // returns true
     * range.contains(5);   // returns false (upper bound is exclusive)
     * }</pre>
     *
     * @return a half-open {@code Range<Integer>} {@code [start, end)} covering the same indices as this range
     * @see Range#closedOpen(Comparable, Comparable)
     */
    public Range<Integer> toRange() {
        return Range.closedOpen(start, end);
    }

    /**
     * Checks whether the specified index occurs within this range.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexRange range = new IndexRange(2, 5);
     * range.contains(2);   // returns true (start is inclusive)
     * range.contains(3);   // returns true
     * range.contains(5);   // returns false (end is exclusive)
     * range.contains(1);   // returns false
     * }</pre>
     *
     * @param index the index to check for containment
     * @return {@code true} if the specified index is contained in this range, {@code false} otherwise
     * @see #containsRange(IndexRange)
     */
    public boolean contains(final int index) {
        return index >= start && index < end;
    }

    /**
     * Checks whether this range contains all indices of the specified range.
     * A range contains another range if every index in the other range is also
     * contained in this range.
     *
     * <p>An empty range is contained by every range, because it has no indices
     * that can fall outside this range.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexRange range1 = new IndexRange(1, 10);
     * IndexRange range2 = new IndexRange(3, 7);
     * IndexRange range3 = new IndexRange(5, 15);
     *
     * range1.containsRange(range2);               // returns true
     * range1.containsRange(range3);               // returns false (extends beyond upper bound)
     * range1.containsRange(new IndexRange(5, 5)); // returns true (the other range is empty)
     * range1.containsRange(null);                 // returns false
     * }</pre>
     *
     * @param other the range to check for containment, {@code null} returns {@code false}
     * @return {@code true} if this range contains all indices of the specified range
     * @see #isOverlappedBy(IndexRange)
     */
    public boolean containsRange(final IndexRange other) {
        if (other == null) {
            return false;
        }

        if (other.isEmpty()) {
            return true;
        }

        return start <= other.start && end >= other.end;
    }

    /**
     * Checks whether this range lies entirely after the specified index, i.e. every index
     * in this range is greater than the specified index.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexRange range = new IndexRange(2, 5);
     * range.isAfter(1);   // returns true
     * range.isAfter(2);   // returns false (2 is included in the range)
     * range.isAfter(3);   // returns false (3 is within the range)
     * }</pre>
     *
     * @param index the index to check
     * @return {@code true} if this entire range is after (greater than) the specified index
     * @see #isBefore(int)
     */
    public boolean isAfter(final int index) {
        return index < start;
    }

    /**
     * Checks whether this range is completely after the specified range, meaning every index
     * in this range is greater than every index in the other range with no shared indices.
     *
     * <p>Because both ranges are half-open, this range may begin exactly where the other
     * range ends: {@code [5, 10)} is after {@code [1, 5)} since the other range excludes 5.</p>
     *
     * <p>Empty ranges are compared by position: {@code [5, 5)} is after {@code [1, 5)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexRange range1 = new IndexRange(10, 15);
     * IndexRange range2 = new IndexRange(1, 5);
     * IndexRange range3 = new IndexRange(1, 11);
     *
     * range1.isAfterRange(range2);   // returns true  (10 >= 5)
     * range1.isAfterRange(range3);   // returns false (ranges share index 10)
     * }</pre>
     *
     * @param other the range to compare against, {@code null} returns {@code false}
     * @return {@code true} if this range is completely after the specified range with no shared indices
     * @see #isBeforeRange(IndexRange)
     * @see #isOverlappedBy(IndexRange)
     */
    public boolean isAfterRange(final IndexRange other) {
        if (other == null) {
            return false;
        }

        return start >= other.end;
    }

    /**
     * Checks whether this range lies entirely before the specified index, i.e. every index
     * in this range is less than the specified index.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexRange range = new IndexRange(2, 5);
     * range.isBefore(6);   // returns true
     * range.isBefore(5);   // returns true (5 is excluded by the half-open range)
     * range.isBefore(4);   // returns false (4 is within the range)
     * }</pre>
     *
     * @param index the index to check
     * @return {@code true} if this entire range is before (less than) the specified index
     * @see #isAfter(int)
     */
    public boolean isBefore(final int index) {
        return index >= end;
    }

    /**
     * Checks whether this range is completely before the specified range, meaning every index
     * in this range is less than every index in the other range with no shared indices.
     *
     * <p>Because both ranges are half-open, this range may end exactly where the other range
     * begins: {@code [1, 5)} is before {@code [5, 10)} since this range excludes 5.</p>
     *
     * <p>Empty ranges are compared by position: {@code [5, 5)} is before {@code [6, 10)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexRange range1 = new IndexRange(1, 5);
     * IndexRange range2 = new IndexRange(10, 15);
     * IndexRange range3 = new IndexRange(4, 10);
     *
     * range1.isBeforeRange(range2);   // returns true  (5 <= 10)
     * range1.isBeforeRange(range3);   // returns false (ranges share index 4)
     * }</pre>
     *
     * @param other the range to compare against, {@code null} returns {@code false}
     * @return {@code true} if this range is completely before the specified range with no shared indices
     * @see #isAfterRange(IndexRange)
     * @see #isOverlappedBy(IndexRange)
     */
    public boolean isBeforeRange(final IndexRange other) {
        if (other == null) {
            return false;
        }

        return end <= other.start;
    }

    /**
     * Checks whether this range overlaps with the specified range.
     * Two ranges overlap if there is at least one index that is contained in both ranges.
     * Ranges that touch at a single boundary do not overlap because both are half-open:
     * {@code [1, 5)} and {@code [5, 10)} share no index.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexRange range1 = new IndexRange(1, 5);
     * IndexRange range2 = new IndexRange(3, 8);
     * IndexRange range3 = new IndexRange(6, 10);
     * IndexRange range4 = new IndexRange(5, 10);
     *
     * range1.isOverlappedBy(range2);   // returns true (overlap from 3 to 5)
     * range1.isOverlappedBy(range3);   // returns false (no overlap)
     * range1.isOverlappedBy(range4);   // returns false (ranges touch at 5 but share no index)
     * }</pre>
     *
     * @param other the range to test for overlap, {@code null} returns {@code false}
     * @return {@code true} if the specified range overlaps with this range; otherwise, {@code false}
     * @see #intersection(IndexRange)
     * @see #isBeforeRange(IndexRange)
     * @see #isAfterRange(IndexRange)
     */
    public boolean isOverlappedBy(final IndexRange other) {
        return other != null && !isEmpty() && !other.isEmpty() && !isAfterRange(other) && !isBeforeRange(other);
    }

    /**
     * Calculates the intersection of this range with another overlapping range.
     * The intersection is the largest range that is contained by both input ranges.
     * If the ranges do not overlap, returns an empty {@code u.Optional}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexRange range1 = new IndexRange(1, 5);
     * IndexRange range2 = new IndexRange(3, 8);
     * u.Optional<IndexRange> intersection = range1.intersection(range2);
     * // returns u.Optional containing new IndexRange(3, 5)
     *
     * IndexRange range3 = new IndexRange(6, 10);
     * u.Optional<IndexRange> noIntersection = range1.intersection(range3);
     * // returns u.Optional.empty()
     * }</pre>
     *
     * @param other the range to intersect with this range; a {@code null} value is treated
     *              as non-overlapping and yields {@code u.Optional.empty()}
     * @return an {@link u.Optional}{@code <IndexRange>} containing the intersection range if the ranges overlap;
     *         {@code u.Optional.empty()} if they do not overlap (or {@code other} is {@code null},
     *         or either range is empty)
     * @see #isOverlappedBy(IndexRange)
     * @see #span(IndexRange)
     */
    public Optional<IndexRange> intersection(final IndexRange other) {
        if (!isOverlappedBy(other)) {
            return Optional.empty();
        } else if (this.equals(other)) {
            return Optional.of(this);
        }

        return Optional.of(new IndexRange(Math.max(start, other.start), Math.min(end, other.end)));
    }

    /**
     * Returns the minimal range that encloses both this range and the specified range.
     * The span is the smallest range that contains every index contained in either of the
     * input ranges. If the input ranges are connected (overlapping or touching), the span
     * is their union; if not, the span includes the gap between them.
     *
     * <p>This operation is commutative, associative, and idempotent.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexRange range1 = new IndexRange(1, 3);
     * IndexRange range2 = new IndexRange(5, 7);
     * IndexRange span = range1.span(range2);
     * // returns new IndexRange(1, 7), which also includes the indices 3..4 that are in neither input range
     * }</pre>
     *
     * @param other the range to span with this range, must not be {@code null}
     * @return the minimal range that contains all indices from both input ranges
     * @throws NullPointerException if {@code other} is {@code null}
     * @see #intersection(IndexRange)
     */
    public IndexRange span(final IndexRange other) {
        N.requireNonNull(other, "other");

        return new IndexRange(Math.min(start, other.start), Math.max(end, other.end));
    }

    /**
     * Returns a new range whose start and end indices are offset by the specified delta.
     * The shifted range covers exactly the same length of indices, just displaced by {@code delta}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * new IndexRange(2, 5).shift(3);    // returns new IndexRange(5, 8)
     * new IndexRange(2, 5).shift(-1);   // returns new IndexRange(1, 4)
     * new IndexRange(2, 5).shift(-2);   // returns new IndexRange(0, 3)
     * }</pre>
     *
     * @param delta the number of positions to shift this range by
     * @return a new {@code IndexRange} {@code [start + delta, end + delta)}
     * @throws IllegalArgumentException if the shifted range falls out of valid bounds
     *         (negative start index or integer overflow)
     */
    public IndexRange shift(final int delta) {
        final long newStart = (long) start + delta;
        final long newEnd = (long) end + delta;

        if (newStart < 0 || newStart > Integer.MAX_VALUE || newEnd > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Shifted index range is out of bounds: [" + newStart + ", " + newEnd + ")");
        }

        return new IndexRange((int) newStart, (int) newEnd);
    }

    /**
     * Checks if this range is empty. A half-open range {@code [start, end)} is empty
     * when {@code start == end}, meaning it contains no indices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * new IndexRange(5, 5).isEmpty();   // returns true
     * new IndexRange(5, 6).isEmpty();   // returns false
     * }</pre>
     *
     * @return {@code true} if this range contains no indices, {@code false} otherwise
     */
    public boolean isEmpty() {
        return start == end;
    }

    /**
     * Returns the number of indices in this range, i.e. {@code end - start}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * new IndexRange(2, 5).length();   // returns 3
     * "abcde".substring(2, 5).length() == new IndexRange(2, 5).length();   // returns true
     * }</pre>
     *
     * @return the number of indices in this range
     */
    public int length() {
        return end - start;
    }

    /**
     * Performs the specified action for each index contained in this range, in ascending order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Integer> indices = new ArrayList<>();
     * new IndexRange(2, 5).forEach(indices::add);
     * // indices is [2, 3, 4]
     * }</pre>
     *
     * @param action the action to be performed for each index, must not be {@code null}
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @see #intStream()
     */
    public void forEach(final IntConsumer action) {
        N.checkArgNotNull(action, cs.action);

        for (int i = start; i < end; i++) {
            action.accept(i);
        }
    }

    /**
     * Returns a sequential {@code IntStream} over the indices contained in this range.
     * The returned stream is empty if this range is empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int sum = new IndexRange(2, 5).intStream().sum();   // returns 9 (2 + 3 + 4)
     * }</pre>
     *
     * @return an {@code IntStream} over the indices {@code [start, end)}
     * @see #forEach(IntConsumer)
     */
    @Beta
    public IntStream intStream() {
        return IntStream.range(start, end);
    }

    /**
     * Returns a string representation of this range in mathematical interval notation.
     * The start index is rendered with an inclusive square bracket and the end index
     * with an exclusive parenthesis: {@code [start, end)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * new IndexRange(2, 5).toString();   // returns "[2, 5)"
     * new IndexRange(5, 5).toString();   // returns "[5, 5)"
     * }</pre>
     *
     * @return a string representation of this range
     */
    @Override
    public String toString() {
        return "[" + start + ", " + end + ")";
    }
}
