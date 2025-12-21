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
 * A mutable wrapper for a {@code double} value, providing methods to modify the wrapped value.
 * 
 * <p>This class is useful in scenarios where you need to pass a double by reference,
 * accumulate double values in lambda expressions, or store frequently changing double values
 * in collections without creating new Double objects.</p>
 * 
 * <p><strong>Note: This class is NOT thread-safe.</strong> If multiple threads access a
 * MutableDouble instance concurrently, and at least one thread modifies it, external
 * synchronization is required.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * MutableDouble sum = MutableDouble.of(0.0);
 * prices.forEach(price -> sum.add(price));
 * System.out.println("Total: " + sum.value());
 * }</pre>
 * 
 * <p>Note: This class is adapted from Apache Commons Lang.</p>
 * 
 * @version $Id: MutableDouble.java 1669791 2015-03-28 15:22:59Z britter $
 * @see Double
 * @see Number
 * @see Comparable
 * @see Mutable
 */
public final class MutableDouble extends Number implements Comparable<MutableDouble>, Mutable {

    @Serial
    private static final long serialVersionUID = 1587163916L;

    /**
     * The mutable double value.
     */
    private double value;

    /**
     * Constructs a new MutableDouble with the default value of zero.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableDouble num = new MutableDouble();   // value is 0.0
     * }</pre>
     */
    MutableDouble() {
    }

    /**
     * Constructs a new MutableDouble with the specified initial value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableDouble num = new MutableDouble(3.14159);   // value is 3.14159
     * }</pre>
     * 
     * @param value the initial value to store
     */
    MutableDouble(final double value) {
        this.value = value;
    }

    /**
     * Creates a new MutableDouble instance with the specified value.
     * This is a factory method that provides a more fluent way to create instances.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableDouble price = MutableDouble.of(19.99);
     * }</pre>
     * 
     * @param value the initial value
     * @return a new MutableDouble instance containing the specified value
     */
    public static MutableDouble of(final double value) {
        return new MutableDouble(value);
    }

    /**
     * Returns the current double value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableDouble num = MutableDouble.of(42.5);
     * double val = num.value();   // returns 42.5
     * }</pre>
     * 
     * @return the current double value
     */
    public double value() {
        return value;
    }

    //-----------------------------------------------------------------------

    /**
     * Gets the value as a primitive double.
     *
     * @return the current double value
     * @deprecated replaced by {@link #value()}.
     */
    @Deprecated
    public double getValue() {
        return value;
    }

    /**
     * Sets the value to the specified double.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableDouble num = MutableDouble.of(10.5);
     * num.setValue(20.7);   // value is now 20.7
     * }</pre>
     * 
     * @param value the value to set
     */
    public void setValue(final double value) {
        this.value = value;
    }

    /**
     * Returns the current value and then sets the new value.
     * This is an atomic-like operation for single-threaded use.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableDouble num = MutableDouble.of(10.5);
     * double old = num.getAndSet(20.7);   // returns 10.5, value is now 20.7
     * }</pre>
     * 
     * @param value the new value to set
     * @return the value before it was updated
     */
    public double getAndSet(final double value) {
        final double result = this.value;
        this.value = value;
        return result;
    }

    /**
     * Sets the value and then returns it.
     * This method first updates the internal value to the specified value,
     * then returns the newly set value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableDouble num = MutableDouble.of(10.5);
     * double newVal = num.setAndGet(20.7);   // returns 20.7, value is now 20.7
     * }</pre>
     * 
     * @param value the new value to set
     * @return the newly set value
     */
    public double setAndGet(final double value) {
        this.value = value;
        return this.value;
    }

    /**
     * Sets the value to newValue if the predicate returns {@code true} for the current value.
     * If the predicate returns {@code false}, the value remains unchanged.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableDouble num = MutableDouble.of(10.5);
     * boolean changed = num.setIf(20.7, val -> val < 15);   // returns true, value becomes 20.7
     * changed = num.setIf(5.0, val -> val < 15);            // returns false, value remains 20.7
     * }</pre>
     * 
     * @param <E> the type of exception that the predicate may throw
     * @param newValue the new value to set if the predicate returns true
     * @param predicate the predicate that tests the current value
     * @return {@code true} if the value was set, {@code false} otherwise
     * @throws E if the predicate throws an exception
     */
    public <E extends Exception> boolean setIf(final double newValue, final Throwables.DoublePredicate<E> predicate) throws E {
        if (predicate.test(value)) {
            value = newValue;
            return true;
        }

        return false;
    }

    //-----------------------------------------------------------------------

    /**
     * Checks whether the double value is the special NaN (Not-a-Number) value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableDouble num = MutableDouble.of(Double.NaN);
     * boolean isNan = num.isNaN();   // returns true
     * }</pre>
     * 
     * @return {@code true} if the value is NaN, {@code false} otherwise
     */
    public boolean isNaN() {
        return Double.isNaN(value);
    }

    /**
     * Checks whether the double value is infinite (positive or negative infinity).
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableDouble num = MutableDouble.of(Double.POSITIVE_INFINITY);
     * boolean isInf = num.isInfinite();   // returns true
     * }</pre>
     * 
     * @return {@code true} if the value is infinite, {@code false} otherwise
     */
    public boolean isInfinite() {
        return Double.isInfinite(value);
    }

    //-----------------------------------------------------------------------

    /**
     * Increments the value by 1.0.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableDouble num = MutableDouble.of(5.5);
     * num.increment();   // value is now 6.5
     * }</pre>
     */
    public void increment() {
        value++;
    }

    /**
     * Decrements the value by 1.0.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableDouble num = MutableDouble.of(5.5);
     * num.decrement();   // value is now 4.5
     * }</pre>
     */
    public void decrement() {
        value--;
    }

    //-----------------------------------------------------------------------

    /**
     * Adds the specified operand to the current value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableDouble num = MutableDouble.of(10.0);
     * num.add(5.5);   // value is now 15.5
     * }</pre>
     * 
     * @param operand the value to add to the current value
     */
    public void add(final double operand) {
        value += operand;
    }

    /**
     * Subtracts the specified operand from the current value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableDouble num = MutableDouble.of(10.0);
     * num.subtract(3.5);   // value is now 6.5
     * }</pre>
     * 
     * @param operand the value to subtract from the current value
     */
    public void subtract(final double operand) {
        value -= operand;
    }

    /**
     * Returns the current value and then increments it by 1.0.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableDouble num = MutableDouble.of(5.0);
     * double old = num.getAndIncrement();   // returns 5.0, value is now 6.0
     * }</pre>
     * 
     * @return the value before incrementing
     */
    public double getAndIncrement() {
        return value++;
    }

    /**
     * Returns the current value and then decrements it by 1.0.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableDouble num = MutableDouble.of(5.0);
     * double old = num.getAndDecrement();   // returns 5.0, value is now 4.0
     * }</pre>
     * 
     * @return the value before decrementing
     */
    public double getAndDecrement() {
        return value--;
    }

    /**
     * Increments the value by 1.0 and then returns it.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableDouble num = MutableDouble.of(5.0);
     * double newVal = num.incrementAndGet();   // returns 6.0, value is now 6.0
     * }</pre>
     * 
     * @return the value after incrementing
     */
    public double incrementAndGet() {
        return ++value;
    }

    /**
     * Decrements the value by 1.0 and then returns it.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableDouble num = MutableDouble.of(5.0);
     * double newVal = num.decrementAndGet();   // returns 4.0, value is now 4.0
     * }</pre>
     * 
     * @return the value after decrementing
     */
    public double decrementAndGet() {
        return --value;
    }

    /**
     * Returns the current value and then adds the given delta to it.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableDouble num = MutableDouble.of(10.0);
     * double old = num.getAndAdd(5.5);   // returns 10.0, value is now 15.5
     * }</pre>
     * 
     * @param delta the value to add
     * @return the value before adding delta
     */
    public double getAndAdd(final double delta) {
        final double prev = value;
        value += delta;
        return prev;
    }

    /**
     * Adds the given delta to the current value and then returns it.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableDouble num = MutableDouble.of(10.0);
     * double newVal = num.addAndGet(5.5);   // returns 15.5, value is now 15.5
     * }</pre>
     * 
     * @param delta the value to add
     * @return the value after adding delta
     */
    public double addAndGet(final double delta) {
        return value += delta;
    }

    //-----------------------------------------------------------------------
    // shortValue and byteValue rely on Number implementation

    /**
     * Returns the value of this MutableDouble as an int by casting.
     * This may involve rounding or truncation. The fractional part is discarded.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableDouble num = MutableDouble.of(42.7);
     * int intVal = num.intValue();   // returns 42
     * }</pre>
     *
     * @return the numeric value represented by this object after conversion to type int
     */
    @Override
    public int intValue() {
        return (int) value;
    }

    /**
     * Returns the value of this MutableDouble as a long by casting.
     * This may involve rounding or truncation. The fractional part is discarded.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableDouble num = MutableDouble.of(42.7);
     * long longVal = num.longValue();   // returns 42L
     * }</pre>
     *
     * @return the numeric value represented by this object after conversion to type long
     */
    @Override
    public long longValue() {
        return (long) value;
    }

    /**
     * Returns the value of this MutableDouble as a float by casting.
     * This may involve rounding and loss of precision since float has less precision than double.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableDouble num = MutableDouble.of(42.123456789);
     * float floatVal = num.floatValue();   // returns 42.123456f (with precision loss)
     * }</pre>
     *
     * @return the numeric value represented by this object after conversion to type float
     */
    @Override
    public float floatValue() {
        return (float) value;
    }

    /**
     * Returns the value of this MutableDouble as a double.
     * This is an exact conversion with no loss of precision.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableDouble num = MutableDouble.of(42.5);
     * double doubleVal = num.doubleValue();   // returns 42.5
     * }</pre>
     *
     * @return the numeric value represented by this object
     */
    @Override
    public double doubleValue() {
        return value;
    }

    //-----------------------------------------------------------------------

    /**
     * Compares this MutableDouble to another MutableDouble numerically.
     * The comparison follows the same rules as {@link Double#compare(double, double)}:
     * <ul>
     * <li>Negative zero is considered less than positive zero</li>
     * <li>NaN is considered equal to itself and greater than all other values including positive infinity</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableDouble num1 = MutableDouble.of(10.5);
     * MutableDouble num2 = MutableDouble.of(20.5);
     * int result = num1.compareTo(num2);   // returns negative value
     * }</pre>
     *
     * @param other the other MutableDouble to compare to, not null
     * @return a negative value if this is less than other, zero if equal,
     *         or a positive value if greater
     */
    @Override
    public int compareTo(final MutableDouble other) {
        return Double.compare(value, other.value);
    }

    //-----------------------------------------------------------------------

    /**
     * Compares this object against the specified object for equality.
     * The result is {@code true} if and only if the argument is not {@code null}
     * and is a {@code MutableDouble} object that contains the same {@code double} value
     * as this object.
     *
     * <p>The comparison uses {@link Double#compare(double, double)} to handle special cases:</p>
     * <ul>
     * <li>Two NaN values are considered equal</li>
     * <li>Positive zero and negative zero are considered different</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableDouble num1 = MutableDouble.of(10.5);
     * MutableDouble num2 = MutableDouble.of(10.5);
     * boolean isEqual = num1.equals(num2);   // returns true
     * }</pre>
     *
     * @param obj the object to compare with, {@code null} returns false
     * @return {@code true} if the objects are equal; {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        return obj instanceof MutableDouble && Double.compare(((MutableDouble) obj).value, value) == 0;
    }

    /**
     * Returns a hash code for this MutableDouble.
     * The hash code is computed using {@link Double#hashCode(double)} on the wrapped value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableDouble num = MutableDouble.of(42.5);
     * int hash = num.hashCode();   // returns Double.hashCode(42.5)
     * }</pre>
     * 
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        return Double.hashCode(value);
    }

    //-----------------------------------------------------------------------

    /**
     * Returns a String representation of this MutableDouble's value.
     * The string representation is determined by {@link N#stringOf(double)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableDouble num = MutableDouble.of(42.5);
     * String str = num.toString();   // returns "42.5"
     * }</pre>
     *
     * @return the String representation of the current value
     */
    @Override
    public String toString() {
        return N.stringOf(value);
    }

}
