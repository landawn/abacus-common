/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.util;

/**
 * <p>
 * Note: it's copied from Apache Commons Lang developed at The Apache Software Foundation (http://www.apache.org/), or
 * under the Apache License 2.0. The methods copied from other products/frameworks may be modified in this class.
 * </p>
 * 
 * A mutable <code>byte</code> wrapper.
 * <p>
 * Note that as MutableByte does not extend Byte, it is not treated by String.format as a Byte parameter. 
 * 
 * @see Byte
 * @since 2.1
 * @version $Id: MutableByte.java 1669791 2015-03-28 15:22:59Z britter $
 */
public final class MutableByte extends Number implements Comparable<MutableByte>, Mutable {

    /**
     * Required for serialization support.
     * 
     * @see java.io.Serializable
     */
    private static final long serialVersionUID = -1585823265L;

    /** The mutable value. */
    private byte value;

    /**
     * Constructs a new MutableByte with the default value of zero.
     */
    MutableByte() {
        super();
    }

    /**
     * Constructs a new MutableByte with the specified value.
     * 
     * @param value  the initial value to store
     */
    MutableByte(final byte value) {
        super();
        this.value = value;
    }

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
     * @return the value as a Byte, never null
     * @deprecated replace by {@link #value()}.
     */
    @Deprecated
    public byte getValue() {
        return value;
    }

    /**
     * Sets the value.
     * 
     * @param value  the value to set
     */
    public void setValue(final byte value) {
        this.value = value;
    }

    public byte getAndSet(final byte value) {
        final byte result = this.value;
        this.value = value;
        return result;
    }

    public byte setAndGet(final byte value) {
        this.value = value;
        return this.value;
    }

    /**
     * Set with the specified new value and returns <code>true</code> if <code>predicate</code> returns true.
     * Otherwise just return <code>false</code> without setting the value to new value.
     * 
     * @param newValue
     * @param predicate - test the current value.
     * @return
     */
    public <E extends Exception> boolean setIf(byte newValue, Try.BytePredicate<E> predicate) throws E {
        if (predicate.test(this.value)) {
            this.value = newValue;
            return true;
        }

        return false;
    }

    /**
     * Set with the specified new value and returns <code>true</code> if <code>predicate</code> returns true.
     * Otherwise just return <code>false</code> without setting the value to new value.
     * 
     * @param newValue
     * @param predicate the first parameter is the current value, the second parameter is the new value.
     * @return
     */
    public <E extends Exception> boolean setIf(byte newValue, Try.ByteBiPredicate<E> predicate) throws E {
        if (predicate.test(this.value, newValue)) {
            this.value = newValue;
            return true;
        }

        return false;
    }

    //-----------------------------------------------------------------------
    /**
     * Increments the value.
     *
     * @since Commons Lang 2.2
     */
    public void increment() {
        value++;
    }

    /**
     * Decrements the value.
     *
     * @since Commons Lang 2.2
     */
    public void decrement() {
        value--;
    }

    //-----------------------------------------------------------------------
    /**
     * Adds a value to the value of this instance.
     * 
     * @param operand  the value to add, not null
     * @since Commons Lang 2.2
     */
    public void add(final byte operand) {
        this.value += operand;
    }

    /**
     * Subtracts a value from the value of this instance.
     * 
     * @param operand  the value to subtract, not null
     * @since Commons Lang 2.2
     */
    public void subtract(final byte operand) {
        this.value -= operand;
    }

    /**
     * Increments by one the current value.
     *
     * @return the previous value
     */
    public final byte getAndIncrement() {
        return value++;
    }

    /**
     * Decrements by one the current value.
     *
     * @return the previous value
     */
    public final byte getAndDecrement() {
        return value--;
    }

    /**
     * Increments by one the current value.
     *
     * @return the updated value
     */
    public final byte incrementAndGet() {
        return ++value;
    }

    /**
     * Decrements by one the current value.
     *
     * @return the updated value
     */
    public final byte decrementAndGet() {
        return --value;
    }

    /**
     * Adds the given value to the current value.
     *
     * @param delta the value to add
     * @return the previous value
     */
    public final byte getAndAdd(final byte delta) {
        final byte prev = value;
        value += delta;
        return prev;
    }

    /**
     * Adds the given value to the current value.
     *
     * @param delta the value to add
     * @return the updated value
     */
    public final byte addAndGet(final byte delta) {
        return value += delta;
    }

    //-----------------------------------------------------------------------
    // shortValue relies on Number implementation
    /**
     * Returns the value of this MutableByte as a byte.
     *
     * @return the numeric value represented by this object after conversion to type byte.
     */
    @Override
    public byte byteValue() {
        return value;
    }

    /**
     * Returns the value of this MutableByte as an int.
     *
     * @return the numeric value represented by this object after conversion to type int.
     */
    @Override
    public int intValue() {
        return value;
    }

    /**
     * Returns the value of this MutableByte as a long.
     *
     * @return the numeric value represented by this object after conversion to type long.
     */
    @Override
    public long longValue() {
        return value;
    }

    /**
     * Returns the value of this MutableByte as a float.
     *
     * @return the numeric value represented by this object after conversion to type float.
     */
    @Override
    public float floatValue() {
        return value;
    }

    /**
     * Returns the value of this MutableByte as a double.
     *
     * @return the numeric value represented by this object after conversion to type double.
     */
    @Override
    public double doubleValue() {
        return value;
    }

    //-----------------------------------------------------------------------
    /**
     * Compares this mutable to another in ascending order.
     * 
     * @param other  the other mutable to compare to, not null
     * @return negative if this is less, zero if equal, positive if greater
     */
    @Override
    public int compareTo(final MutableByte other) {
        return (this.value > other.value) ? 1 : ((this.value == other.value) ? 0 : -1);
    }

    //-----------------------------------------------------------------------
    /**
     * Compares this object to the specified object. The result is <code>true</code> if and only if the argument is
     * not <code>null</code> and is a <code>MutableByte</code> object that contains the same <code>byte</code> value
     * as this object.
     * 
     * @param obj  the object to compare with, null returns false
     * @return <code>true</code> if the objects are the same; <code>false</code> otherwise.
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
     * @return the mutable value as a string
     */
    @Override
    public String toString() {
        return String.valueOf(value);
    }

}
