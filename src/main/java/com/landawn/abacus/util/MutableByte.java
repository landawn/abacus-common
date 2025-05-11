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
 * A mutable {@code byte} wrapper.
 *
 * <p>
 * {@code MutableByte} is NOT thread-safe.
 *
 * @version $Id: MutableByte.java 1669791 2015-03-28 15:22:59Z britter $
 * @see Byte
 */
public final class MutableByte extends Number implements Comparable<MutableByte>, Mutable {

    @Serial
    private static final long serialVersionUID = -1585823265L;

    private byte value;

    /**
     * Constructs a new MutableByte with the default value of zero.
     */
    MutableByte() {
    }

    /**
     * Constructs a new MutableByte with the specified value.
     *
     * @param value the initial value to store
     */
    MutableByte(final byte value) {
        this.value = value;
    }

    /**
     *
     * @param value
     * @return
     */
    public static MutableByte of(final byte value) {
        return new MutableByte(value);
    }

    public byte value() {
        return value;
    }

    //-----------------------------------------------------------------------

    /**
     * Gets the value as a Byte instance.
     *
     * @return
     * @deprecated replace by {@link #value()}.
     */
    @Deprecated
    public byte getValue() {
        return value;
    }

    /**
     * Sets the value.
     *
     * @param value the value to set
     */
    public void setValue(final byte value) {
        this.value = value;
    }

    /**
     * Returns the current value and then set new value
     *
     * @param value
     * @return
     */
    public byte getAndSet(final byte value) {
        final byte result = this.value;
        this.value = value;
        return result;
    }

    /**
     * Sets with the specified value and then return it.
     *
     * @param value
     * @return
     */
    public byte setAndGet(final byte value) {
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
    public <E extends Exception> boolean setIf(final byte newValue, final Throwables.BytePredicate<E> predicate) throws E {
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
    //    public <E extends Exception> boolean setIf(byte newValue, Throwables.ByteBiPredicate<E> predicate) throws E {
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
    public void add(final byte operand) {
        value += operand;
    }

    /**
     * Subtracts a value from the value of this instance.
     *
     * @param operand the value to subtract, not null
     */
    public void subtract(final byte operand) {
        value -= operand;
    }

    /**
     * Increments by one the current value.
     *
     * @return
     */
    public byte getAndIncrement() {
        return value++;
    }

    /**
     * Decrements by one the current value.
     *
     * @return
     */
    public byte getAndDecrement() {
        return value--;
    }

    /**
     * Increments by one the current value.
     *
     * @return
     */
    public byte incrementAndGet() {
        return ++value;
    }

    /**
     * Decrements by one the current value.
     *
     * @return
     */
    public byte decrementAndGet() {
        return --value;
    }

    /**
     * Adds the given value to the current value.
     *
     * @param delta the value to add
     * @return
     */
    public byte getAndAdd(final byte delta) {
        final byte prev = value;
        value += delta;
        return prev;
    }

    /**
     * Adds the given value to the current value.
     *
     * @param delta the value to add
     * @return
     */
    public byte addAndGet(final byte delta) {
        return value += delta;
    }

    //-----------------------------------------------------------------------
    // shortValue relies on Number implementation

    /**
     * Returns the value of this MutableByte as a byte.
     *
     * @return
     */
    @Override
    public byte byteValue() {
        return value;
    }

    /**
     * Returns the value of this MutableByte as a short.
     *
     * @return
     */
    @Override
    public short shortValue() {
        return value;
    }

    /**
     * Returns the value of this MutableByte as an int.
     *
     * @return
     */
    @Override
    public int intValue() {
        return value;
    }

    /**
     * Returns the value of this MutableByte as a long.
     *
     * @return
     */
    @Override
    public long longValue() {
        return value;
    }

    /**
     * Returns the value of this MutableByte as a float.
     *
     * @return
     */
    @Override
    public float floatValue() {
        return value;
    }

    /**
     * Returns the value of this MutableByte as a double.
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
    public int compareTo(final MutableByte other) {
        return Byte.compare(value, other.value);
    }

    //-----------------------------------------------------------------------

    /**
     * Compares this object to the specified object. The result is {@code true} if and only if the argument is
     * not {@code null} and is a {@code MutableByte} object that contains the same {@code byte} value
     * as this object.
     *
     * @param obj the object to compare with, {@code null} returns false
     * @return {@code true} if the objects are the same; {@code false} otherwise.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof MutableByte) {
            return value == ((MutableByte) obj).value;
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
        return value;
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
