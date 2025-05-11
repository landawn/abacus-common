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
 * A mutable {@code float} wrapper.
 *
 * <p>
 * {@code MutableFloat} is NOT thread-safe.
 *
 * @version $Id: MutableFloat.java 1669791 2015-03-28 15:22:59Z britter $
 * @see Float
 */
public final class MutableFloat extends Number implements Comparable<MutableFloat>, Mutable {

    @Serial
    private static final long serialVersionUID = 5787169186L;

    private float value;

    /**
     * Constructs a new MutableFloat with the default value of zero.
     */
    MutableFloat() {
    }

    /**
     * Constructs a new MutableFloat with the specified value.
     *
     * @param value the initial value to store
     */
    MutableFloat(final float value) {
        this.value = value;
    }

    /**
     *
     * @param value
     * @return
     */
    public static MutableFloat of(final float value) {
        return new MutableFloat(value);
    }

    public float value() {
        return value;
    }

    //-----------------------------------------------------------------------

    /**
     * Gets the value as a Float instance.
     *
     * @return
     * @deprecated replace by {@link #value()}.
     */
    @Deprecated
    public float getValue() {
        return value;
    }

    /**
     * Sets the value.
     *
     * @param value the value to set
     */
    public void setValue(final float value) {
        this.value = value;
    }

    /**
     * Returns the current value and then set new value
     *
     * @param value
     * @return
     */
    public float getAndSet(final float value) {
        final float result = this.value;
        this.value = value;
        return result;
    }

    /**
     * Sets with the specified value and then return it.
     *
     * @param value
     * @return
     */
    public float setAndGet(final float value) {
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
    public <E extends Exception> boolean setIf(final float newValue, final Throwables.FloatPredicate<E> predicate) throws E {
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
    //    public <E extends Exception> boolean setIf(float newValue, Throwables.FloatBiPredicate<E> predicate) throws E {
    //        if (predicate.test(this.value, newValue)) {
    //            this.value = newValue;
    //            return true;
    //        }
    //
    //        return false;
    //    }

    //-----------------------------------------------------------------------

    /**
     * Checks whether the float value is the special NaN value.
     *
     * @return {@code true} if NaN
     */
    public boolean isNaN() {
        return Float.isNaN(value);
    }

    /**
     * Checks whether the float value is infinite.
     *
     * @return {@code true} if infinite
     */
    public boolean isInfinite() {
        return Float.isInfinite(value);
    }

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
    public void add(final float operand) {
        value += operand;
    }

    /**
     * Subtracts a value from the value of this instance.
     *
     * @param operand the value to subtract
     */
    public void subtract(final float operand) {
        value -= operand;
    }

    /**
     * Increments by one the current value.
     *
     * @return
     */
    public float getAndIncrement() {
        return value++;
    }

    /**
     * Decrements by one the current value.
     *
     * @return
     */
    public float getAndDecrement() {
        return value--;
    }

    /**
     * Increments by one the current value.
     *
     * @return
     */
    public float incrementAndGet() {
        return ++value;
    }

    /**
     * Decrements by one the current value.
     *
     * @return
     */
    public float decrementAndGet() {
        return --value;
    }

    /**
     * Adds the given value to the current value.
     *
     * @param delta the value to add
     * @return
     */
    public float getAndAdd(final float delta) {
        final float prev = value;
        value += delta;
        return prev;
    }

    /**
     * Adds the given value to the current value.
     *
     * @param delta the value to add
     * @return
     */
    public float addAndGet(final float delta) {
        return value += delta;
    }

    //-----------------------------------------------------------------------
    // shortValue and byteValue rely on Number implementation

    /**
     * Returns the value of this MutableFloat as an int.
     *
     * @return
     */
    @Override
    public int intValue() {
        return (int) value;
    }

    /**
     * Returns the value of this MutableFloat as a long.
     *
     * @return
     */
    @Override
    public long longValue() {
        return (long) value;
    }

    /**
     * Returns the value of this MutableFloat as a float.
     *
     * @return
     */
    @Override
    public float floatValue() {
        return value;
    }

    /**
     * Returns the value of this MutableFloat as a double.
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
    public int compareTo(final MutableFloat other) {
        return Float.compare(value, other.value);
    }

    //-----------------------------------------------------------------------

    /**
     * Compares this object against some other object. The result is {@code true} if and only if the argument is
     * not {@code null} and is a {@code Float} object that represents a {@code float} that has the
     * identical bit pattern to the bit pattern of the {@code float} represented by this object. For this
     * purpose, two float values are considered to be the same if and only if the method
     * {@link Float#floatToIntBits(float)}returns the same int value when applied to each.
     * <p>
     * Note that in most cases, for two instances of class {@code Float},{@code f1} and {@code f2},
     * the value of {@code f1.equals(f2)} is {@code true} if and only if <blockquote>
     *
     * <pre>
     *   f1.floatValue() == f2.floatValue()
     * </pre>
     *
     * </blockquote>
     * <p>
     * Also has the value {@code true}. However, there are two exceptions:
     * <ul>
     * <li>If {@code f1} and {@code f2} both represent {@code Float.NaN}, then the
     * {@code equals} method returns {@code true}, even though {@code Float.NaN==Float.NaN} has
     * the value {@code false}.
     * <li>If {@code f1} represents {@code +0.0f} while {@code f2} represents {@code -0.0f},
     * or vice versa, the {@code equal} test has the value {@code false}, even though
     * {@code 0.0f==-0.0f} has the value {@code true}.
     * </ul>
     * This definition allows hashtables to operate properly.
     *
     * @param obj the object to compare with, {@code null} returns false
     * @return {@code true} if the objects are the same; {@code false} otherwise.
     * @see java.lang.Float#floatToIntBits(float)
     */
    @Override
    public boolean equals(final Object obj) {
        return obj instanceof MutableFloat && Float.compare(((MutableFloat) obj).value, value) == 0;
    }

    /**
     * Returns a suitable hash code for this mutable.
     *
     * @return a suitable hash code
     */
    @Override
    public int hashCode() {
        return Float.floatToIntBits(value);
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
