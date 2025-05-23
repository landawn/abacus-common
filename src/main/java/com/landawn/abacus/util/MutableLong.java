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

/**
 * <p>
 * Note: it's copied from Apache Commons Lang developed at <a href="http://www.apache.org/">The Apache Software Foundation</a>, or
 * under the Apache License 2.0. The methods copied from other products/frameworks may be modified in this class.
 * </p>
 *
 * A mutable {@code long} wrapper.
 *
 * <p>
 * {@code MutableLong} is NOT thread-safe.
 *
 * @version $Id: MutableLong.java 1669791 2015-03-28 15:22:59Z britter $
 * @see Long
 */
public final class MutableLong extends Number implements Comparable<MutableLong>, Mutable {

    @Serial
    private static final long serialVersionUID = 62986528375L;

    private long value;

    /**
     * Constructs a new MutableLong with the default value of zero.
     */
    MutableLong() {
    }

    /**
     * Constructs a new MutableLong with the specified value.
     *
     * @param value the initial value to store
     */
    MutableLong(final long value) {
        this.value = value;
    }

    /**
     *
     * @param value
     * @return
     */
    public static MutableLong of(final long value) {
        return new MutableLong(value);
    }

    public long value() {
        return value;
    }

    //-----------------------------------------------------------------------

    /**
     * Gets the value as a Long instance.
     *
     * @return
     * @deprecated replace by {@link #value()}.
     */
    @Deprecated
    public long getValue() {
        return value;
    }

    /**
     * Sets the value.
     *
     * @param value the value to set
     */
    public void setValue(final long value) {
        this.value = value;
    }

    /**
     * Returns the current value and then set new value
     *
     * @param value
     * @return
     */
    public long getAndSet(final long value) {
        final long result = this.value;
        this.value = value;
        return result;
    }

    /**
     * Sets with the specified value and then return it.
     *
     * @param value
     * @return
     */
    public long setAndGet(final long value) {
        this.value = value;
        return this.value;
    }

    /**
     * Set with the specified new value and returns {@code true} if {@code predicate} returns {@code true}.
     * Otherwise, just return {@code false} without setting the value to new value.
     *
     * @param <E>
     * @param newValue
     * @param predicate - test the current value.
     * @return
     * @throws E the e
     */
    public <E extends Exception> boolean setIf(final long newValue, final Throwables.LongPredicate<E> predicate) throws E {
        if (predicate.test(value)) {
            value = newValue;
            return true;
        }

        return false;
    }

    //    /**
    //     * Set with the specified new value and returns <code>true</code> if <code>predicate</code> returns true.
    //     * Otherwise, just return <code>false</code> without setting the value to new value.
    //     *
    //     * @param <E>
    //     * @param newValue
    //     * @param predicate the first parameter is the current value, the second parameter is the new value.
    //     * @return
    //     * @throws E the e
    //     * @deprecated
    //     */
    //    @Deprecated
    //    public <E extends Exception> boolean setIf(long newValue, Throwables.LongBiPredicate<E> predicate) throws E {
    //        if (predicate.test(this.value, newValue)) {
    //            this.value = newValue;
    //            return true;
    //        }
    //
    //        return false;
    //    }

    //-----------------------------------------------------------------------

    /**
     * Increments the value.
     *
     */
    public void increment() {
        value++;
    }

    /**
     * Decrements the value.
     *
     */
    public void decrement() {
        value--;
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a value to the value of this instance.
     *
     * @param operand the value to add, not null
     */
    public void add(final long operand) {
        value += operand;
    }

    /**
     * Subtracts a value from the value of this instance.
     *
     * @param operand the value to subtract, not null
     */
    public void subtract(final long operand) {
        value -= operand;
    }

    /**
     * Increments by one the current value.
     *
     * @return
     */
    public long getAndIncrement() {
        return value++;
    }

    /**
     * Decrements by one the current value.
     *
     * @return
     */
    public long getAndDecrement() {
        return value--;
    }

    /**
     * Increments by one the current value.
     *
     * @return
     */
    public long incrementAndGet() {
        return ++value;
    }

    /**
     * Decrements by one the current value.
     *
     * @return
     */
    public long decrementAndGet() {
        return --value;
    }

    /**
     * Adds the given value to the current value.
     *
     * @param delta the value to add
     * @return
     */
    public long getAndAdd(final long delta) {
        final long prev = value;
        value += delta;
        return prev;
    }

    /**
     * Adds the given value to the current value.
     *
     * @param delta the value to add
     * @return
     */
    public long addAndGet(final long delta) {
        return value += delta;
    }

    //-----------------------------------------------------------------------
    // shortValue and byteValue rely on Number implementation

    /**
     * Returns the value of this MutableLong as an int.
     *
     * @return
     */
    @Override
    public int intValue() {
        return (int) value;
    }

    /**
     * Returns the value of this MutableLong as a long.
     *
     * @return
     */
    @Override
    public long longValue() {
        return value;
    }

    /**
     * Returns the value of this MutableLong as a float.
     *
     * @return
     */
    @Override
    public float floatValue() {
        return value;
    }

    /**
     * Returns the value of this MutableLong as a double.
     *
     * @return
     */
    @Override
    public double doubleValue() {
        return value;
    }

    //-----------------------------------------------------------------------

    /**
     * Compares this mutable to another in ascending order.
     *
     * @param other the other mutable to compare to, not null
     * @return negative if this is less, zero if equal, positive if greater
     */
    @Override
    public int compareTo(final MutableLong other) {
        return Long.compare(value, other.value);
    }

    //-----------------------------------------------------------------------

    /**
     * Compares this object to the specified object. The result is {@code true} if and only if the argument
     * is not {@code null} and is a {@code MutableLong} object that contains the same {@code long}
     * value as this object.
     *
     * @param obj the object to compare with, {@code null} returns false
     * @return {@code true} if the objects are the same; {@code false} otherwise.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof MutableLong) {
            return value == ((MutableLong) obj).value;
        }
        return false;
    }

    /**
     * Returns a suitable hash code for this mutable.
     *
     * @return a suitable hash code
     */
    @Override
    public int hashCode() {
        return Long.hashCode(value);
    }

    //-----------------------------------------------------------------------

    /**
     * Returns the String value of this mutable.
     *
     * @return
     */
    @Override
    public String toString() {
        return N.stringOf(value);
    }

}
