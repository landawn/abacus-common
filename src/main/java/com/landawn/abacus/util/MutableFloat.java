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
 * A mutable <code>float</code> wrapper.
 * <p>
 * Note that as MutableFloat does not extend Float, it is not treated by String.format as a Float parameter. 
 * 
 * @see Float
 * @since 2.1
 * @version $Id: MutableFloat.java 1669791 2015-03-28 15:22:59Z britter $
 */
public final class MutableFloat extends Number implements Comparable<MutableFloat>, Mutable {

    /**
     * Required for serialization support.
     * 
     * @see java.io.Serializable
     */
    private static final long serialVersionUID = 5787169186L;

    /** The mutable value. */
    private float value;

    /**
     * Constructs a new MutableFloat with the default value of zero.
     */
    MutableFloat() {
        super();
    }

    /**
     * Constructs a new MutableFloat with the specified value.
     * 
     * @param value  the initial value to store
     */
    MutableFloat(final float value) {
        super();
        this.value = value;
    }

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
     * @return the value as a Float, never null
     * @deprecated replace by {@link #value()}.
     */
    @Deprecated
    public float getValue() {
        return value;
    }

    /**
     * Sets the value.
     * 
     * @param value  the value to set
     */
    public void setValue(final float value) {
        this.value = value;
    }

    public float getAndSet(final float value) {
        final float result = this.value;
        this.value = value;
        return result;
    }

    public float setAndGet(final float value) {
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
    public <E extends Exception> boolean setIf(float newValue, Try.FloatPredicate<E> predicate) throws E {
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
    public <E extends Exception> boolean setIf(float newValue, Try.FloatBiPredicate<E> predicate) throws E {
        if (predicate.test(this.value, newValue)) {
            this.value = newValue;
            return true;
        }

        return false;
    }

    //-----------------------------------------------------------------------
    /**
     * Checks whether the float value is the special NaN value.
     * 
     * @return true if NaN
     */
    public boolean isNaN() {
        return Float.isNaN(value);
    }

    /**
     * Checks whether the float value is infinite.
     * 
     * @return true if infinite
     */
    public boolean isInfinite() {
        return Float.isInfinite(value);
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
    public void add(final float operand) {
        this.value += operand;
    }

    /**
     * Subtracts a value from the value of this instance.
     * 
     * @param operand  the value to subtract
     * @since Commons Lang 2.2
     */
    public void subtract(final float operand) {
        this.value -= operand;
    }

    /**
     * Increments by one the current value.
     *
     * @return the previous value
     */
    public final float getAndIncrement() {
        return value++;
    }

    /**
     * Decrements by one the current value.
     *
     * @return the previous value
     */
    public final float getAndDecrement() {
        return value--;
    }

    /**
     * Increments by one the current value.
     *
     * @return the updated value
     */
    public final float incrementAndGet() {
        return ++value;
    }

    /**
     * Decrements by one the current value.
     *
     * @return the updated value
     */
    public final float decrementAndGet() {
        return --value;
    }

    /**
     * Adds the given value to the current value.
     *
     * @param delta the value to add
     * @return the previous value
     */
    public final float getAndAdd(final float delta) {
        final float prev = value;
        value += delta;
        return prev;
    }

    /**
     * Adds the given value to the current value.
     *
     * @param delta the value to add
     * @return the updated value
     */
    public final float addAndGet(final float delta) {
        return value += delta;
    }

    //-----------------------------------------------------------------------
    // shortValue and byteValue rely on Number implementation
    /**
     * Returns the value of this MutableFloat as an int.
     *
     * @return the numeric value represented by this object after conversion to type int.
     */
    @Override
    public int intValue() {
        return (int) value;
    }

    /**
     * Returns the value of this MutableFloat as a long.
     *
     * @return the numeric value represented by this object after conversion to type long.
     */
    @Override
    public long longValue() {
        return (long) value;
    }

    /**
     * Returns the value of this MutableFloat as a float.
     *
     * @return the numeric value represented by this object after conversion to type float.
     */
    @Override
    public float floatValue() {
        return value;
    }

    /**
     * Returns the value of this MutableFloat as a double.
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
    public int compareTo(final MutableFloat other) {
        return Float.compare(this.value, other.value);
    }

    //-----------------------------------------------------------------------
    /**
     * Compares this object against some other object. The result is <code>true</code> if and only if the argument is
     * not <code>null</code> and is a <code>Float</code> object that represents a <code>float</code> that has the
     * identical bit pattern to the bit pattern of the <code>float</code> represented by this object. For this
     * purpose, two float values are considered to be the same if and only if the method
     * {@link Float#floatToIntBits(float)}returns the same int value when applied to each.
     * <p>
     * Note that in most cases, for two instances of class <code>Float</code>,<code>f1</code> and <code>f2</code>,
     * the value of <code>f1.equals(f2)</code> is <code>true</code> if and only if <blockquote>
     * 
     * <pre>
     *   f1.floatValue() == f2.floatValue()
     * </pre>
     * 
     * </blockquote>
     * <p>
     * also has the value <code>true</code>. However, there are two exceptions:
     * <ul>
     * <li>If <code>f1</code> and <code>f2</code> both represent <code>Float.NaN</code>, then the
     * <code>equals</code> method returns <code>true</code>, even though <code>Float.NaN==Float.NaN</code> has
     * the value <code>false</code>.
     * <li>If <code>f1</code> represents <code>+0.0f</code> while <code>f2</code> represents <code>-0.0f</code>,
     * or vice versa, the <code>equal</code> test has the value <code>false</code>, even though
     * <code>0.0f==-0.0f</code> has the value <code>true</code>.
     * </ul>
     * This definition allows hashtables to operate properly.
     * 
     * @param obj  the object to compare with, null returns false
     * @return <code>true</code> if the objects are the same; <code>false</code> otherwise.
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
     * @return the mutable value as a string
     */
    @Override
    public String toString() {
        return String.valueOf(value);
    }

}
