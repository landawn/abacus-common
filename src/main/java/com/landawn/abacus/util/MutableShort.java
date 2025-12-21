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
 * A mutable wrapper for a {@code short} value, providing methods to modify the wrapped value.
 * 
 * <p>This class is useful in scenarios where you need to pass a short by reference,
 * accumulate short values in lambda expressions, or store frequently changing short values
 * in collections without creating new Short objects.</p>
 * 
 * <p><strong>Note: This class is NOT thread-safe.</strong> If multiple threads access a
 * MutableShort instance concurrently, and at least one thread modifies it, external
 * synchronization is required.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * MutableShort counter = MutableShort.of((short)0);
 * shortArray.forEach(s -> counter.add(s));
 * System.out.println("Sum: " + counter.value());
 * }</pre>
 * 
 * <p>Note: This class is adapted from Apache Commons Lang.</p>
 * 
 * @version $Id: MutableShort.java 1669791 2015-03-28 15:22:59Z britter $
 * @see Short
 * @see Number
 * @see Comparable
 * @see Mutable
 */
public final class MutableShort extends Number implements Comparable<MutableShort>, Mutable {

    @Serial
    private static final long serialVersionUID = -2135791679L;

    /**
     * The mutable short value.
     */
    private short value;

    /**
     * Constructs a new MutableShort with the default value of zero.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableShort num = new MutableShort();   // value is 0
     * }</pre>
     */
    MutableShort() {
    }

    /**
     * Constructs a new MutableShort with the specified initial value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableShort num = new MutableShort((short)100);   // value is 100
     * }</pre>
     * 
     * @param value the initial value to store
     */
    MutableShort(final short value) {
        this.value = value;
    }

    /**
     * Creates a new MutableShort instance with the specified value.
     * This is a factory method that provides a more fluent way to create instances.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableShort count = MutableShort.of((short)42);
     * }</pre>
     * 
     * @param value the initial value
     * @return a new MutableShort instance containing the specified value
     */
    public static MutableShort of(final short value) {
        return new MutableShort(value);
    }

    /**
     * Returns the current short value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableShort num = MutableShort.of((short)42);
     * short val = num.value();   // returns 42
     * }</pre>
     * 
     * @return the current short value
     */
    public short value() {
        return value;
    }

    //-----------------------------------------------------------------------

    /**
     * Returns the current short value.
     *
     * @return the current short value
     * @deprecated replaced by {@link #value()}.
     */
    @Deprecated
    public short getValue() {
        return value;
    }

    /**
     * Sets the value to the specified short.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableShort num = MutableShort.of((short)10);
     * num.setValue((short)20);   // value is now 20
     * }</pre>
     * 
     * @param value the value to set
     */
    public void setValue(final short value) {
        this.value = value;
    }

    /**
     * Returns the current value and then sets the new value.
     * This is an atomic-like operation for single-threaded use.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableShort num = MutableShort.of((short)10);
     * short old = num.getAndSet((short)20);   // returns 10, value is now 20
     * }</pre>
     * 
     * @param value the new value to set
     * @return the value before it was updated
     */
    public short getAndSet(final short value) {
        final short result = this.value;
        this.value = value;
        return result;
    }

    /**
     * Sets the value and then returns it.
     * This is useful when you want to update and immediately use the new value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableShort num = MutableShort.of((short)10);
     * short newVal = num.setAndGet((short)20);   // returns 20, value is now 20
     * }</pre>
     * 
     * @param value the new value to set
     * @return the new value after it has been set
     */
    public short setAndGet(final short value) {
        this.value = value;
        return this.value;
    }

    /**
     * Sets the value to newValue if the predicate evaluates to {@code true} for the current value.
     * If the predicate returns {@code false}, the value remains unchanged.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableShort num = MutableShort.of((short)10);
     * boolean updated = num.setIf((short)20, v -> v < 15);   // returns true, value is now 20
     * updated = num.setIf((short)30, v -> v < 15);           // returns false, value remains 20
     * }</pre>
     * 
     * @param <E> the type of exception the predicate may throw
     * @param newValue the new value to set if the condition is met
     * @param predicate the predicate to test the current value
     * @return {@code true} if the value was updated, {@code false} otherwise
     * @throws E if the predicate throws an exception
     */
    public <E extends Exception> boolean setIf(final short newValue, final Throwables.ShortPredicate<E> predicate) throws E {
        if (predicate.test(value)) {
            value = newValue;
            return true;
        }

        return false;
    }

    //-----------------------------------------------------------------------

    /**
     * Increments the value by one.
     * Note: short overflow will wrap around (32767 + 1 = -32768).
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableShort num = MutableShort.of((short)10);
     * num.increment();   // value is now 11
     * }</pre>
     */
    public void increment() {
        value++;
    }

    /**
     * Decrements the value by one.
     * Note: short underflow will wrap around (-32768 - 1 = 32767).
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableShort num = MutableShort.of((short)10);
     * num.decrement();   // value is now 9
     * }</pre>
     */
    public void decrement() {
        value--;
    }

    //-----------------------------------------------------------------------

    /**
     * Adds the specified operand to the current value.
     * Note: short overflow will wrap around.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableShort num = MutableShort.of((short)10);
     * num.add((short)5);   // value is now 15
     * }</pre>
     * 
     * @param operand the value to add
     */
    public void add(final short operand) {
        value += operand;
    }

    /**
     * Subtracts the specified operand from the current value.
     * Note: short underflow will wrap around.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableShort num = MutableShort.of((short)10);
     * num.subtract((short)3);   // value is now 7
     * }</pre>
     * 
     * @param operand the value to subtract
     */
    public void subtract(final short operand) {
        value -= operand;
    }

    /**
     * Returns the current value and then increments it by one.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableShort num = MutableShort.of((short)10);
     * short old = num.getAndIncrement();   // returns 10, value is now 11
     * }</pre>
     * 
     * @return the value before incrementing
     */
    public short getAndIncrement() {
        return value++;
    }

    /**
     * Returns the current value and then decrements it by one.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableShort num = MutableShort.of((short)10);
     * short old = num.getAndDecrement();   // returns 10, value is now 9
     * }</pre>
     * 
     * @return the value before decrementing
     */
    public short getAndDecrement() {
        return value--;
    }

    /**
     * Increments the value by one and then returns it.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableShort num = MutableShort.of((short)10);
     * short newVal = num.incrementAndGet();   // returns 11, value is now 11
     * }</pre>
     * 
     * @return the value after incrementing
     */
    public short incrementAndGet() {
        return ++value;
    }

    /**
     * Decrements the value by one and then returns it.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableShort num = MutableShort.of((short)10);
     * short newVal = num.decrementAndGet();   // returns 9, value is now 9
     * }</pre>
     * 
     * @return the value after decrementing
     */
    public short decrementAndGet() {
        return --value;
    }

    /**
     * Returns the current value and then adds the specified delta.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableShort num = MutableShort.of((short)10);
     * short old = num.getAndAdd((short)5);   // returns 10, value is now 15
     * }</pre>
     * 
     * @param delta the value to add
     * @return the value before adding
     */
    public short getAndAdd(final short delta) {
        final short prev = value;
        value += delta;
        return prev;
    }

    /**
     * Adds the specified delta to the current value and then returns it.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableShort num = MutableShort.of((short)10);
     * short newVal = num.addAndGet((short)5);   // returns 15, value is now 15
     * }</pre>
     * 
     * @param delta the value to add
     * @return the value after adding
     */
    public short addAndGet(final short delta) {
        return value += delta;
    }

    //-----------------------------------------------------------------------
    // byteValue relies on Number implementation

    /**
     * Returns the value of this MutableShort as a short.
     * This is equivalent to calling {@link #value()}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableShort num = MutableShort.of((short)42);
     * short val = num.shortValue();   // returns 42
     * }</pre>
     *
     * @return the short value represented by this object
     */
    @Override
    public short shortValue() {
        return value;
    }

    /**
     * Returns the value of this MutableShort as an int after a widening primitive conversion.
     * The short value is sign-extended to create the int value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableShort num = MutableShort.of((short)100);
     * int val = num.intValue();   // returns 100
     * }</pre>
     *
     * @return the numeric value represented by this object after conversion to type int
     */
    @Override
    public int intValue() {
        return value;
    }

    /**
     * Returns the value of this MutableShort as a long after a widening primitive conversion.
     * The short value is sign-extended to create the long value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableShort num = MutableShort.of((short)50);
     * long val = num.longValue();   // returns 50L
     * }</pre>
     *
     * @return the numeric value represented by this object after conversion to type long
     */
    @Override
    public long longValue() {
        return value;
    }

    /**
     * Returns the value of this MutableShort as a float after a widening primitive conversion.
     * The conversion is exact (no loss of precision).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableShort num = MutableShort.of((short)75);
     * float val = num.floatValue();   // returns 75.0f
     * }</pre>
     *
     * @return the numeric value represented by this object after conversion to type float
     */
    @Override
    public float floatValue() {
        return value;
    }

    /**
     * Returns the value of this MutableShort as a double after a widening primitive conversion.
     * The conversion is exact (no loss of precision).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableShort num = MutableShort.of((short)-128);
     * double val = num.doubleValue();   // returns -128.0
     * }</pre>
     *
     * @return the numeric value represented by this object after conversion to type double
     */
    @Override
    public double doubleValue() {
        return value;
    }

    //-----------------------------------------------------------------------

    /**
     * Compares this MutableShort to another MutableShort in ascending order.
     * Returns a negative value if this is less than the other, zero if equal,
     * or a positive value if this is greater than the other.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableShort a = MutableShort.of((short)10);
     * MutableShort b = MutableShort.of((short)20);
     * int result = a.compareTo(b);   // returns negative value
     * }</pre>
     * 
     * @param other the other MutableShort to compare to, not null
     * @return negative if this is less, zero if equal, positive if greater
     */
    @Override
    public int compareTo(final MutableShort other) {
        return Short.compare(value, other.value);
    }

    //-----------------------------------------------------------------------

    /**
     * Compares this object to the specified object. The result is {@code true} if and only if
     * the argument is not {@code null} and is a {@code MutableShort} object that contains the
     * same {@code short} value as this object.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableShort a = MutableShort.of((short)10);
     * MutableShort b = MutableShort.of((short)10);
     * boolean equal = a.equals(b);   // returns true
     * }</pre>
     * 
     * @param obj the object to compare with, {@code null} returns false
     * @return {@code true} if the objects are the same; {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof MutableShort) {
            return value == ((MutableShort) obj).value;
        }
        return false;
    }

    /**
     * Returns a hash code for this MutableShort.
     * The hash code is equal to the short value (widened to int).
     *
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        return value;
    }

    //-----------------------------------------------------------------------

    /**
     * Returns the String representation of this MutableShort's value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableShort num = MutableShort.of((short)42);
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
