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
     * <p>
     * Obtains a range using the specified element as both the minimum and maximum in this range.
     * </p>
     *
     * <p>
     * The range uses the natural ordering of the elements to determine where values lie in the range.
     * </p>
     *
     * @param <T>
     *            the type of the elements in this range
     * @param element
     *            the value to use for this range, not null
     * @return
     * @throws IllegalArgumentException if the element is null
     */
    public static <T extends Comparable<? super T>> Range<T> just(final T element) {
        return closed(element, element);
    }

    /**
     *
     * @param <T>
     * @param min
     * @param max
     * @return
     * @throws IllegalArgumentException if the <i>min</i> or <i>max</i> is {@code null}, or min > max.
     */
    public static <T extends Comparable<? super T>> Range<T> open(final T min, final T max) {
        if (min == null || max == null || min.compareTo(max) > 0) {
            throw new IllegalArgumentException("'fromInclusive' and 'toInclusive' can't be null, or min > max");//NOSONAR
        }

        return new Range<>(new LowerEndpoint<>(min, false), new UpperEndpoint<>(max, false), BoundType.OPEN_OPEN);
    }

    /**
     *
     * @param <T>
     * @param min
     * @param max
     * @return
     * @throws IllegalArgumentException if the <i>min</i> or <i>max</i> is {@code null}, or min > max.
     */
    public static <T extends Comparable<? super T>> Range<T> openClosed(final T min, final T max) {
        if (min == null || max == null || min.compareTo(max) > 0) {
            throw new IllegalArgumentException("'fromInclusive' and 'toInclusive' can't be null, or min > max");
        }

        return new Range<>(new LowerEndpoint<>(min, false), new UpperEndpoint<>(max, true), BoundType.OPEN_CLOSED);
    }

    /**
     *
     * @param <T>
     * @param min
     * @param max
     * @return
     * @throws IllegalArgumentException if the <i>min</i> or <i>max</i> is {@code null}, or min > max.
     */
    public static <T extends Comparable<? super T>> Range<T> closedOpen(final T min, final T max) {
        if (min == null || max == null || min.compareTo(max) > 0) {
            throw new IllegalArgumentException("'fromInclusive' and 'toInclusive' can't be null, or min > max");
        }

        return new Range<>(new LowerEndpoint<>(min, true), new UpperEndpoint<>(max, false), BoundType.CLOSED_OPEN);
    }

    /**
     *
     * @param <T>
     * @param min
     * @param max
     * @return
     * @throws IllegalArgumentException if the <i>min</i> or <i>max</i> is {@code null}, or min > max.
     */
    public static <T extends Comparable<? super T>> Range<T> closed(final T min, final T max) {
        if (min == null || max == null || min.compareTo(max) > 0) {
            throw new IllegalArgumentException("'fromInclusive' and 'toInclusive' can't be null, or min > max");
        }

        return new Range<>(new LowerEndpoint<>(min, true), new UpperEndpoint<>(max, true), BoundType.CLOSED_CLOSED);
    }

    /**
     *
     * @param <U>
     * @param mapper
     * @return
     */
    public <U extends Comparable<? super U>> Range<U> map(final Function<? super T, ? extends U> mapper) {
        return new Range<>(new LowerEndpoint<>(mapper.apply(lowerEndpoint.value), lowerEndpoint.isClosed),
                new UpperEndpoint<>(mapper.apply(upperEndpoint.value), upperEndpoint.isClosed), boundType);
    }

    public BoundType boundType() {
        return boundType;
    }

    /**
     * <p>
     * Gets the minimum value in this range.
     * </p>
     *
     * @return
     */
    public T lowerEndpoint() {
        return lowerEndpoint.value;
    }

    /**
     * <p>
     * Gets the maximum value in this range.
     * </p>
     *
     * @return
     */
    public T upperEndpoint() {
        return upperEndpoint.value;
    }

    // Element tests
    //--------------------------------------------------------------------

    /**
     * <p>
     * Checks whether the specified element occurs within this range.
     * </p>
     *
     * @param valueToFind
     *            the element to check for, {@code null} returns false
     * @return {@code true} if the specified element occurs within this range
     */
    public boolean contains(final T valueToFind) {
        if (valueToFind == null) {
            return false;
        }

        return lowerEndpoint.includes(valueToFind) && upperEndpoint.includes(valueToFind);
    }

    /**
     *
     * @param c
     * @return
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
     * <p>
     * Checks whether this range starts with the specified element.
     * </p>
     *
     * @param element
     *            the element to check for, {@code null} returns false
     * @return {@code true} if the specified element occurs within this range
     */
    public boolean isStartedBy(final T element) {
        if (element == null) {
            return false;
        }

        return lowerEndpoint.isClosed && lowerEndpoint.compareTo(element) == 0;
    }

    /**
     * <p>
     * Checks whether this range starts with the specified element.
     * </p>
     *
     * @param element
     *            the element to check for, {@code null} returns false
     * @return {@code true} if the specified element occurs within this range
     */
    public boolean isEndedBy(final T element) {
        if (element == null) {
            return false;
        }

        return upperEndpoint.isClosed && upperEndpoint.compareTo(element) == 0;
    }

    /**
     * <p>
     * Checks whether this range is after the specified element.
     * </p>
     *
     * @param element
     *            the element to check for, {@code null} returns false
     * @return {@code true} if this range is entirely after the specified element
     */
    public boolean isAfter(final T element) {
        if (element == null) {
            return false;
        }

        return !lowerEndpoint.includes(element);
    }

    /**
     * <p>
     * Checks whether this range is before the specified element.
     * </p>
     *
     * @param element
     *            the element to check for, {@code null} returns false
     * @return {@code true} if this range is entirely before the specified element
     */
    public boolean isBefore(final T element) {
        if (element == null) {
            return false;
        }

        return !upperEndpoint.includes(element);
    }

    /**
     * <p>
     * Checks where the specified element occurs relative to this range.
     * </p>
     *
     * <p>
     * Returns {@code -1} if this range is before the specified element,
     * {@code 1} if this range is after the specified element, otherwise {@code 0} if the specified element is contained in this range.
     * </p>
     *
     * @param element
     *            the element to check for, not null
     * @return -1, 0 or +1 depending on the element's location relative to the range
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
     * <p>
     * Checks whether this range contains all the elements of the specified range.
     * </p>
     *
     * @param other
     *            the range to check, {@code null} returns false
     * @return {@code true} if this range contains the specified range
     * @throws RuntimeException
     *             if ranges cannot be compared
     */
    public boolean containsRange(final Range<T> other) {
        if (other == null) {
            return false;
        }

        return (other.lowerEndpoint.isClosed ? contains(other.lowerEndpoint.value) : lowerEndpoint.value.compareTo(other.lowerEndpoint.value) <= 0)
                && (other.upperEndpoint.isClosed ? contains(other.upperEndpoint.value) : upperEndpoint.value.compareTo(other.upperEndpoint.value) >= 0);
    }

    /**
     * <p>
     * Checks whether this range is completely after the specified range.
     * </p>
     *
     *
     * @param other
     *            the range to check, {@code null} returns false
     * @return {@code true} if this range is completely after the specified range
     * @throws RuntimeException
     *             if ranges cannot be compared
     */
    public boolean isAfterRange(final Range<T> other) {
        if (other == null) {
            return false;
        }
        return other.upperEndpoint.isClosed ? isAfter(other.upperEndpoint.value) : lowerEndpoint.compareTo(other.upperEndpoint.value) >= 0;
    }

    /**
     * <p>
     * Checks whether this range is completely before the specified range.
     * </p>
     *
     *
     * @param other
     *            the range to check, {@code null} returns false
     * @return {@code true} if this range is complete before the specified range
     * @throws RuntimeException
     *             if ranges cannot be compared
     */
    public boolean isBeforeRange(final Range<T> other) {
        if (other == null) {
            return false;
        }

        return other.lowerEndpoint.isClosed ? isBefore(other.lowerEndpoint.value) : upperEndpoint.compareTo(other.lowerEndpoint.value) <= 0;
    }

    /**
     * <p>
     * Checks whether this range is overlapped by the specified range.
     * </p>
     *
     * <p>
     * Two ranges overlap if there is at least one element in common.
     * </p>
     *
     *
     * @param other
     *            the range to test, {@code null} returns false
     * @return {@code true} if the specified range overlaps with this range; otherwise, {@code false}
     * @throws RuntimeException
     *             if ranges cannot be compared
     */
    public boolean isOverlappedBy(final Range<T> other) {
        //NOSONAR
        return other != null && !isAfterRange(other) && !isBeforeRange(other);
    }

    /**
     * Calculate the intersection of {@code this} and an overlapping Range.
     *
     * @param other
     *            overlapping Range
     * @return range representing the intersection of {@code this} and {@code other}, {@code this} if equal, or {@code Optional.empty()} if they're not overlapped.
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
     * Copied from Guava under Apache License v2.0
     * <br />
     * Returns the minimal range that {@linkplain #encloses encloses} both this range and {@code
     * other}. For example, the span of {@code [1..3]} and {@code (5..7)} is {@code [1..7)}.
     *
     * <p><i>If</i> the input ranges are {@linkplain #isConnected connected}, the returned range can
     * also be called their <i>union</i>. If they are not, note that the span might contain values
     * that are not contained in either input range.
     *
     * <p>Like {@link #intersection(Range) intersection}, this operation is commutative, associative
     * and idempotent. Unlike it, it is always well-defined for any two input ranges.
     *
     * @param other
     * @return
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
     * Checks if is empty.
     *
     * @return {@code true}, if is empty
     */
    public boolean isEmpty() {
        //NOSONAR
        return !lowerEndpoint.isClosed && !upperEndpoint.isClosed && lowerEndpoint.compareTo(upperEndpoint.value) == 0;
    }

    // Basics
    //--------------------------------------------------------------------

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

        if (obj instanceof Range) {
            final Range<T> other = (Range<T>) obj;
            return N.equals(lowerEndpoint, other.lowerEndpoint) && N.equals(upperEndpoint, other.upperEndpoint);
        }

        return false;
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
        int result = 17;

        result = 37 * result + getClass().hashCode();
        result = 37 * result + lowerEndpoint.hashCode();
        return 37 * result + upperEndpoint.hashCode();
    }

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
