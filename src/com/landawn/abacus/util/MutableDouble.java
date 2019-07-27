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
 * A mutable <code>double</code> wrapper.
 * <p>
 * Note that as MutableDouble does not extend Double, it is not treated by String.format as a Double parameter. 
 * 
 * @see Double
 * @since 2.1
 * @version $Id: MutableDouble.java 1669791 2015-03-28 15:22:59Z britter $
 */
public final class MutableDouble extends Number implements Comparable<MutableDouble>, Mutable {

    /**
     * Required for serialization support.
     * 
     * @see java.io.Serializable
     */
    private static final long serialVersionUID = 1587163916L;

    /** The mutable value. */
    private double value;

    /**
     * Constructs a new MutableDouble with the default value of zero.
     */
    MutableDouble() {
        super();
    }

    /**
     * Constructs a new MutableDouble with the specified value.
     * 
     * @param value  the initial value to store
     */
    MutableDouble(final double value) {
        super();
        this.value = value;
    }

    public static MutableDouble of(final double value) {
        return new MutableDouble(value);
    }

    public double value() {
        return value;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the value as a Double instance.
     * 
     * @return the value as a Double, never null
     * @deprecated replace by {@link #value()}.
     */
    @Deprecated
    public double getValue() {
        return value;
    }

    /**
     * Sets the value.
     * 
     * @param value  the value to set
     */
    public void setValue(final double value) {
        this.value = value;
    }

    public double getAndSet(final double value) {
        final double result = this.value;
        this.value = value;
        return result;
    }

    public double setAndGet(final double value) {
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
    public <E extends Exception> boolean setIf(double newValue, Try.DoublePredicate<E> predicate) throws E {
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
    public <E extends Exception> boolean setIf(double newValue, Try.DoubleBiPredicate<E> predicate) throws E {
        if (predicate.test(this.value, newValue)) {
            this.value = newValue;
            return true;
        }

        return false;
    }

    //-----------------------------------------------------------------------
    /**
     * Checks whether the double value is the special NaN value.
     * 
     * @return true if NaN
     */
    public boolean isNaN() {
        return Double.isNaN(value);
    }

    /**
     * Checks whether the double value is infinite.
     * 
     * @return true if infinite
     */
    public boolean isInfinite() {
        return Double.isInfinite(value);
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
     * @param operand  the value to add
     * @since Commons Lang 2.2
     */
    public void add(final double operand) {
        this.value += operand;
    }

    /**
     * Subtracts a value from the value of this instance.
     * 
     * @param operand  the value to subtract, not null
     * @since Commons Lang 2.2
     */
    public void subtract(final double operand) {
        this.value -= operand;
    }

    /**
     * Increments by one the current value.
     *
     * @return the previous value
     */
    public final double getAndIncrement() {
        return value++;
    }

    /**
     * Decrements by one the current value.
     *
     * @return the previous value
     */
    public final double getAndDecrement() {
        return value--;
    }

    /**
     * Increments by one the current value.
     *
     * @return the updated value
     */
    public final double incrementAndGet() {
        return ++value;
    }

    /**
     * Decrements by one the current value.
     *
     * @return the updated value
     */
    public final double decrementAndGet() {
        return --value;
    }

    /**
     * Adds the given value to the current value.
     *
     * @param delta the value to add
     * @return the previous value
     */
    public final double getAndAdd(final double delta) {
        final double prev = value;
        value += delta;
        return prev;
    }

    /**
     * Adds the given value to the current value.
     *
     * @param delta the value to add
     * @return the updated value
     */
    public final double addAndGet(final double delta) {
        return value += delta;
    }

    //-----------------------------------------------------------------------
    // shortValue and byteValue rely on Number implementation
    /**
     * Returns the value of this MutableDouble as an int.
     *
     * @return the numeric value represented by this object after conversion to type int.
     */
    @Override
    public int intValue() {
        return (int) value;
    }

    /**
     * Returns the value of this MutableDouble as a long.
     *
     * @return the numeric value represented by this object after conversion to type long.
     */
    @Override
    public long longValue() {
        return (long) value;
    }

    /**
     * Returns the value of this MutableDouble as a float.
     *
     * @return the numeric value represented by this object after conversion to type float.
     */
    @Override
    public float floatValue() {
        return (float) value;
    }

    /**
     * Returns the value of this MutableDouble as a double.
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
    public int compareTo(final MutableDouble other) {
        return Double.compare(this.value, other.value);
    }

    //-----------------------------------------------------------------------
    /**
     * Compares this object against the specified object. The result is <code>true</code> if and only if the argument
     * is not <code>null</code> and is a <code>Double</code> object that represents a double that has the identical
     * bit pattern to the bit pattern of the double represented by this object. For this purpose, two
     * <code>double</code> values are considered to be the same if and only if the method
     * {@link Double#doubleToLongBits(double)}returns the same long value when applied to each.
     * <p>
     * Note that in most cases, for two instances of class <code>Double</code>,<code>d1</code> and <code>d2</code>,
     * the value of <code>d1.equals(d2)</code> is <code>true</code> if and only if <blockquote>
     * 
     * <pre>
     *   d1.doubleValue()&nbsp;== d2.doubleValue()
     * </pre>
     * 
     * </blockquote>
     * <p>
     * also has the value <code>true</code>. However, there are two exceptions:
     * <ul>
     * <li>If <code>d1</code> and <code>d2</code> both represent <code>Double.NaN</code>, then the
     * <code>equals</code> method returns <code>true</code>, even though <code>Double.NaN==Double.NaN</code> has
     * the value <code>false</code>.
     * <li>If <code>d1</code> represents <code>+0.0</code> while <code>d2</code> represents <code>-0.0</code>,
     * or vice versa, the <code>equal</code> test has the value <code>false</code>, even though
     * <code>+0.0==-0.0</code> has the value <code>true</code>. This allows hashtables to operate properly.
     * </ul>
     * 
     * @param obj  the object to compare with, null returns false
     * @return <code>true</code> if the objects are the same; <code>false</code> otherwise.
     */
    @Override
    public boolean equals(final Object obj) {
        return obj instanceof MutableDouble && Double.compare(((MutableDouble) obj).value, value) == 0;
    }

    /**
     * Returns a suitable hash code for this mutable.
     * 
     * @return a suitable hash code
     */
    @Override
    public int hashCode() {
        final long bits = Double.doubleToLongBits(value);
        return (int) (bits ^ bits >>> 32);
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
