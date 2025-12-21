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
 * A mutable wrapper for an {@code int} value, providing methods to modify the wrapped value.
 * 
 * <p>This class is useful in scenarios where you need to pass an integer by reference,
 * accumulate values in lambda expressions, or store frequently changing integer values
 * in collections without creating new Integer objects.</p>
 * 
 * <p><strong>Note: This class is NOT thread-safe.</strong> If multiple threads access a
 * MutableInt instance concurrently, and at least one thread modifies it, external
 * synchronization is required.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * MutableInt counter = MutableInt.of(0);
 * list.forEach(item -> counter.increment());
 * System.out.println("Count: " + counter.value());
 * }</pre>
 * 
 * <p>Note: This class is adapted from Apache Commons Lang.</p>
 * 
 * @version $Id: MutableInt.java 1669791 2015-03-28 15:22:59Z britter $
 * @see Integer
 * @see Number
 * @see Comparable
 * @see Mutable
 */
public final class MutableInt extends Number implements Comparable<MutableInt>, Mutable {

    @Serial
    private static final long serialVersionUID = 512176391864L;

    /**
     * The mutable int value.
     */
    private int value;

    /**
     * Constructs a new MutableInt with the default value of zero.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableInt num = new MutableInt();   // value is 0
     * }</pre>
     */
    MutableInt() {
    }

    /**
     * Constructs a new MutableInt with the specified initial value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableInt num = new MutableInt(42);   // value is 42
     * }</pre>
     * 
     * @param value the initial value to store
     */
    MutableInt(final int value) {
        this.value = value;
    }

    /**
     * Creates a new MutableInt instance with the specified value.
     * This is a factory method that provides a more fluent way to create instances.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableInt count = MutableInt.of(10);
     * }</pre>
     * 
     * @param value the initial value
     * @return a new MutableInt instance containing the specified value
     */
    public static MutableInt of(final int value) {
        return new MutableInt(value);
    }

    /**
     * Returns the current int value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableInt num = MutableInt.of(42);
     * int val = num.value();   // returns 42
     * }</pre>
     * 
     * @return the current int value
     */
    public int value() {
        return value;
    }

    //-----------------------------------------------------------------------

    /**
     * Gets the value as a primitive int.
     *
     * @return the current int value
     * @deprecated replaced by {@link #value()}.
     */
    @Deprecated
    public int getValue() {
        return value;
    }

    /**
     * Sets the value to the specified int.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableInt num = MutableInt.of(10);
     * num.setValue(20);   // value is now 20
     * }</pre>
     * 
     * @param value the value to set
     */
    public void setValue(final int value) {
        this.value = value;
    }

    /**
     * Returns the current value and then sets the new value.
     * This is an atomic-like operation for single-threaded use.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableInt num = MutableInt.of(10);
     * int old = num.getAndSet(20);   // returns 10, value is now 20
     * }</pre>
     * 
     * @param value the new value to set
     * @return the value before it was updated
     */
    public int getAndSet(final int value) {
        final int result = this.value;
        this.value = value;
        return result;
    }

    /**
     * Sets the value and then returns it.
     * This is useful when you want to update and immediately use the new value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableInt num = MutableInt.of(10);
     * int newVal = num.setAndGet(20);   // returns 20, value is now 20
     * }</pre>
     * 
     * @param value the new value to set
     * @return the new value after it has been set
     */
    public int setAndGet(final int value) {
        this.value = value;
        return this.value;
    }

    /**
     * Sets the value to newValue if the predicate evaluates to {@code true} for the current value.
     * If the predicate returns {@code false}, the value remains unchanged.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableInt num = MutableInt.of(10);
     * boolean updated = num.setIf(20, v -> v < 15);   // returns true, value is now 20
     * updated = num.setIf(30, v -> v < 15);           // returns false, value remains 20
     * }</pre>
     * 
     * @param <E> the type of exception the predicate may throw
     * @param newValue the new value to set if the condition is met
     * @param predicate the predicate to test the current value
     * @return {@code true} if the value was updated, {@code false} otherwise
     * @throws E if the predicate throws an exception
     */
    public <E extends Exception> boolean setIf(final int newValue, final Throwables.IntPredicate<E> predicate) throws E {
        if (predicate.test(value)) {
            value = newValue;
            return true;
        }

        return false;
    }

    //-----------------------------------------------------------------------

    /**
     * Increments the value by one.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableInt num = MutableInt.of(10);
     * num.increment();   // value is now 11
     * }</pre>
     */
    public void increment() {
        value++;
    }

    /**
     * Decrements the value by one.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableInt num = MutableInt.of(10);
     * num.decrement();   // value is now 9
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
     * MutableInt num = MutableInt.of(10);
     * num.add(5);   // value is now 15
     * }</pre>
     * 
     * @param operand the value to add
     */
    public void add(final int operand) {
        value += operand;
    }

    /**
     * Subtracts the specified operand from the current value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableInt num = MutableInt.of(10);
     * num.subtract(3);   // value is now 7
     * }</pre>
     * 
     * @param operand the value to subtract
     */
    public void subtract(final int operand) {
        value -= operand;
    }

    /**
     * Returns the current value and then increments it by one.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableInt num = MutableInt.of(10);
     * int old = num.getAndIncrement();   // returns 10, value is now 11
     * }</pre>
     * 
     * @return the value before incrementing
     */
    public int getAndIncrement() {
        return value++;
    }

    /**
     * Returns the current value and then decrements it by one.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableInt num = MutableInt.of(10);
     * int old = num.getAndDecrement();   // returns 10, value is now 9
     * }</pre>
     * 
     * @return the value before decrementing
     */
    public int getAndDecrement() {
        return value--;
    }

    /**
     * Increments the value by one and then returns it.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableInt num = MutableInt.of(10);
     * int newVal = num.incrementAndGet();   // returns 11, value is now 11
     * }</pre>
     * 
     * @return the value after incrementing
     */
    public int incrementAndGet() {
        return ++value;
    }

    /**
     * Decrements the value by one and then returns it.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableInt num = MutableInt.of(10);
     * int newVal = num.decrementAndGet();   // returns 9, value is now 9
     * }</pre>
     * 
     * @return the value after decrementing
     */
    public int decrementAndGet() {
        return --value;
    }

    /**
     * Returns the current value and then adds the specified delta.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableInt num = MutableInt.of(10);
     * int old = num.getAndAdd(5);   // returns 10, value is now 15
     * }</pre>
     * 
     * @param delta the value to add
     * @return the value before adding
     */
    public int getAndAdd(final int delta) {
        final int prev = value;
        value += delta;
        return prev;
    }

    /**
     * Adds the specified delta to the current value and then returns it.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableInt num = MutableInt.of(10);
     * int newVal = num.addAndGet(5);   // returns 15, value is now 15
     * }</pre>
     * 
     * @param delta the value to add
     * @return the value after adding
     */
    public int addAndGet(final int delta) {
        return value += delta;
    }

    //-----------------------------------------------------------------------
    // shortValue and byteValue rely on Number implementation

    /**
     * Returns the value of this MutableInt as an int.
     * This method is part of the Number interface implementation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableInt num = MutableInt.of(42);
     * int value = num.intValue();   // returns 42
     * }</pre>
     *
     * @return the int value represented by this object
     */
    @Override
    public int intValue() {
        return value;
    }

    /**
     * Returns the value of this MutableInt as a long.
     * This method is part of the Number interface implementation.
     * The int value is widened to a long.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableInt num = MutableInt.of(42);
     * long value = num.longValue();   // returns 42L
     * }</pre>
     *
     * @return the value represented by this object after conversion to type long
     */
    @Override
    public long longValue() {
        return value;
    }

    /**
     * Returns the value of this MutableInt as a float.
     * This method is part of the Number interface implementation.
     * The int value is widened to a float. Note that this conversion may lose precision
     * for very large integer values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableInt num = MutableInt.of(42);
     * float value = num.floatValue();   // returns 42.0f
     * }</pre>
     *
     * @return the value represented by this object after conversion to type float
     */
    @Override
    public float floatValue() {
        return value;
    }

    /**
     * Returns the value of this MutableInt as a double.
     * This method is part of the Number interface implementation.
     * The int value is widened to a double without loss of precision.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableInt num = MutableInt.of(42);
     * double value = num.doubleValue();   // returns 42.0
     * }</pre>
     *
     * @return the value represented by this object after conversion to type double
     */
    @Override
    public double doubleValue() {
        return value;
    }

    //-----------------------------------------------------------------------

    /**
     * Compares this MutableInt to another MutableInt in ascending order.
     * Returns a negative value if this is less than the other, zero if equal,
     * or a positive value if this is greater than the other.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableInt a = MutableInt.of(10);
     * MutableInt b = MutableInt.of(20);
     * int result = a.compareTo(b);   // returns negative value
     * }</pre>
     * 
     * @param other the other MutableInt to compare to, not null
     * @return negative if this is less, zero if equal, positive if greater
     */
    @Override
    public int compareTo(final MutableInt other) {
        return Integer.compare(value, other.value);
    }

    //-----------------------------------------------------------------------

    /**
     * Compares this object to the specified object. The result is {@code true} if and only if
     * the argument is not {@code null} and is a {@code MutableInt} object that contains the
     * same {@code int} value as this object.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableInt a = MutableInt.of(10);
     * MutableInt b = MutableInt.of(10);
     * boolean equal = a.equals(b);   // returns true
     * }</pre>
     * 
     * @param obj the object to compare with, {@code null} returns false
     * @return {@code true} if the objects are the same; {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof MutableInt) {
            return value == ((MutableInt) obj).value;
        }
        return false;
    }

    /**
     * Returns a hash code for this MutableInt.
     * The hash code is equal to the contained int value itself.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableInt num = MutableInt.of(42);
     * int hash = num.hashCode();   // returns 42
     * }</pre>
     *
     * @return a hash code value for this object, equal to the primitive int value
     */
    @Override
    public int hashCode() {
        return value;
    }

    //-----------------------------------------------------------------------

    /**
     * Returns the String representation of this MutableInt's value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableInt num = MutableInt.of(42);
     * String str = num.toString();   // returns "42"
     * }</pre>
     * 
     * @return the String representation of the current value
     */
    @Override
    public String toString() {
        return N.stringOf(value);
    }

}
