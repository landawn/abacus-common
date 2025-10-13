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
 * A mutable wrapper for a {@code long} value, providing methods to modify the wrapped value.
 * 
 * <p>This class is useful in scenarios where you need to pass a long by reference,
 * accumulate values in lambda expressions, or store frequently changing long values
 * in collections without creating new Long objects.</p>
 * 
 * <p><strong>Note: This class is NOT thread-safe.</strong> If multiple threads access a
 * MutableLong instance concurrently, and at least one thread modifies it, external
 * synchronization is required.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * MutableLong sum = MutableLong.of(0L);
 * list.forEach(item -> sum.add(item.getValue()));
 * System.out.println("Total: " + sum.value());
 * }</pre>
 * 
 * <p>Note: This class is adapted from Apache Commons Lang.</p>
 * 
 * @version $Id: MutableLong.java 1669791 2015-03-28 15:22:59Z britter $
 * @see Long
 * @see Number
 * @see Comparable
 * @see Mutable
 */
public final class MutableLong extends Number implements Comparable<MutableLong>, Mutable {

    @Serial
    private static final long serialVersionUID = 62986528375L;

    private long value;

    /**
     * Constructs a new MutableLong with the default value of zero.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableLong num = new MutableLong(); // value is 0L
     * }</pre>
     */
    MutableLong() {
    }

    /**
     * Constructs a new MutableLong with the specified initial value.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableLong num = new MutableLong(1000000L); // value is 1000000L
     * }</pre>
     * 
     * @param value the initial value to store
     */
    MutableLong(final long value) {
        this.value = value;
    }

    /**
     * Creates a new MutableLong instance with the specified value.
     * This is a factory method that provides a more fluent way to create instances.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableLong timestamp = MutableLong.of(System.currentTimeMillis());
     * }</pre>
     * 
     * @param value the initial value
     * @return a new MutableLong instance containing the specified value
     */
    public static MutableLong of(final long value) {
        return new MutableLong(value);
    }

    /**
     * Returns the current long value.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableLong num = MutableLong.of(42L);
     * long val = num.value(); // returns 42L
     * }</pre>
     * 
     * @return the current long value
     */
    public long value() {
        return value;
    }

    //-----------------------------------------------------------------------

    /**
     * Gets the value as a long.
     *
     * @return the current value
     * @deprecated replace by {@link #value()}.
     */
    @Deprecated
    public long getValue() {
        return value;
    }

    /**
     * Sets the value to the specified long.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableLong num = MutableLong.of(10L);
     * num.setValue(20L); // value is now 20L
     * }</pre>
     * 
     * @param value the value to set
     */
    public void setValue(final long value) {
        this.value = value;
    }

    /**
     * Returns the current value and then sets the new value.
     * This is an atomic-like operation for single-threaded use.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableLong num = MutableLong.of(10L);
     * long old = num.getAndSet(20L); // returns 10L, value is now 20L
     * }</pre>
     * 
     * @param value the new value to set
     * @return the value before it was updated
     */
    public long getAndSet(final long value) {
        final long result = this.value;
        this.value = value;
        return result;
    }

    /**
     * Sets the value and then returns it.
     * This is useful when you want to update and immediately use the new value.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableLong num = MutableLong.of(10L);
     * long newVal = num.setAndGet(20L); // returns 20L, value is now 20L
     * }</pre>
     * 
     * @param value the new value to set
     * @return the new value after it has been set
     */
    public long setAndGet(final long value) {
        this.value = value;
        return this.value;
    }

    /**
     * Sets the value to newValue if the predicate evaluates to true for the current value.
     * If the predicate returns false, the value remains unchanged.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableLong num = MutableLong.of(10L);
     * boolean updated = num.setIf(20L, v -> v < 15L); // returns true, value is now 20L
     * updated = num.setIf(30L, v -> v < 15L); // returns false, value remains 20L
     * }</pre>
     * 
     * @param <E> the type of exception the predicate may throw
     * @param newValue the new value to set if the condition is met
     * @param predicate the predicate to test the current value
     * @return {@code true} if the value was updated, {@code false} otherwise
     * @throws E if the predicate throws an exception
     */
    public <E extends Exception> boolean setIf(final long newValue, final Throwables.LongPredicate<E> predicate) throws E {
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
     * <p>Example:</p>
     * <pre>{@code
     * MutableLong num = MutableLong.of(10L);
     * num.increment(); // value is now 11L
     * }</pre>
     */
    public void increment() {
        value++;
    }

    /**
     * Decrements the value by one.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableLong num = MutableLong.of(10L);
     * num.decrement(); // value is now 9L
     * }</pre>
     */
    public void decrement() {
        value--;
    }

    //-----------------------------------------------------------------------

    /**
     * Adds the specified operand to the current value.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableLong num = MutableLong.of(10L);
     * num.add(5L); // value is now 15L
     * }</pre>
     * 
     * @param operand the value to add
     */
    public void add(final long operand) {
        value += operand;
    }

    /**
     * Subtracts the specified operand from the current value.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableLong num = MutableLong.of(10L);
     * num.subtract(3L); // value is now 7L
     * }</pre>
     * 
     * @param operand the value to subtract
     */
    public void subtract(final long operand) {
        value -= operand;
    }

    /**
     * Returns the current value and then increments it by one.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableLong num = MutableLong.of(10L);
     * long old = num.getAndIncrement(); // returns 10L, value is now 11L
     * }</pre>
     * 
     * @return the value before incrementing
     */
    public long getAndIncrement() {
        return value++;
    }

    /**
     * Returns the current value and then decrements it by one.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableLong num = MutableLong.of(10L);
     * long old = num.getAndDecrement(); // returns 10L, value is now 9L
     * }</pre>
     * 
     * @return the value before decrementing
     */
    public long getAndDecrement() {
        return value--;
    }

    /**
     * Increments the value by one and then returns it.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableLong num = MutableLong.of(10L);
     * long newVal = num.incrementAndGet(); // returns 11L, value is now 11L
     * }</pre>
     * 
     * @return the value after incrementing
     */
    public long incrementAndGet() {
        return ++value;
    }

    /**
     * Decrements the value by one and then returns it.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableLong num = MutableLong.of(10L);
     * long newVal = num.decrementAndGet(); // returns 9L, value is now 9L
     * }</pre>
     * 
     * @return the value after decrementing
     */
    public long decrementAndGet() {
        return --value;
    }

    /**
     * Returns the current value and then adds the specified delta.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableLong num = MutableLong.of(10L);
     * long old = num.getAndAdd(5L); // returns 10L, value is now 15L
     * }</pre>
     * 
     * @param delta the value to add
     * @return the value before adding
     */
    public long getAndAdd(final long delta) {
        final long prev = value;
        value += delta;
        return prev;
    }

    /**
     * Adds the specified delta to the current value and then returns it.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableLong num = MutableLong.of(10L);
     * long newVal = num.addAndGet(5L); // returns 15L, value is now 15L
     * }</pre>
     * 
     * @param delta the value to add
     * @return the value after adding
     */
    public long addAndGet(final long delta) {
        return value += delta;
    }

    //-----------------------------------------------------------------------
    // shortValue and byteValue rely on Number implementation

    /**
     * Returns the value of this MutableLong as an int.
     * Note that this may involve truncation of the high-order bits if the value exceeds the int range.
     *
     * @return the value as an int
     */
    @Override
    public int intValue() {
        return (int) value;
    }

    /**
     * Returns the value of this MutableLong as a long.
     * 
     * @return the long value
     */
    @Override
    public long longValue() {
        return value;
    }

    /**
     * Returns the value of this MutableLong as a float.
     * Note that this may involve rounding and precision loss for large values.
     *
     * @return the value as a float
     */
    @Override
    public float floatValue() {
        return value;
    }

    /**
     * Returns the value of this MutableLong as a double.
     * 
     * @return the value as a double
     */
    @Override
    public double doubleValue() {
        return value;
    }

    //-----------------------------------------------------------------------

    /**
     * Compares this MutableLong to another MutableLong in ascending order.
     * Returns a negative value if this is less than the other, zero if equal,
     * or a positive value if this is greater than the other.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableLong a = MutableLong.of(10L);
     * MutableLong b = MutableLong.of(20L);
     * int result = a.compareTo(b); // returns negative value
     * }</pre>
     * 
     * @param other the other MutableLong to compare to, not null
     * @return negative if this is less, zero if equal, positive if greater
     */
    @Override
    public int compareTo(final MutableLong other) {
        return Long.compare(value, other.value);
    }

    //-----------------------------------------------------------------------

    /**
     * Compares this object to the specified object. The result is {@code true} if and only if
     * the argument is not {@code null} and is a {@code MutableLong} object that contains the
     * same {@code long} value as this object.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableLong a = MutableLong.of(10L);
     * MutableLong b = MutableLong.of(10L);
     * boolean equal = a.equals(b); // returns true
     * }</pre>
     * 
     * @param obj the object to compare with, {@code null} returns false
     * @return {@code true} if the objects are the same; {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof MutableLong) {
            return value == ((MutableLong) obj).value;
        }
        return false;
    }

    /**
     * Returns a hash code for this MutableLong.
     * The hash code is computed using {@link Long#hashCode(long)} on the wrapped value.
     *
     * @return a suitable hash code
     */
    @Override
    public int hashCode() {
        return Long.hashCode(value);
    }

    //-----------------------------------------------------------------------

    /**
     * Returns the String representation of this MutableLong's value.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableLong num = MutableLong.of(42L);
     * String str = num.toString(); // returns "42"
     * }</pre>
     * 
     * @return the String representation of the current value
     */
    @Override
    public String toString() {
        return N.stringOf(value);
    }

}