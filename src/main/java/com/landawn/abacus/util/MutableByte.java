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
 * A mutable wrapper for a {@code byte} value, providing methods to modify the wrapped value.
 * 
 * <p>This class is useful in scenarios where you need to pass a byte by reference,
 * accumulate byte values in lambda expressions, or store frequently changing byte values
 * in collections without creating new Byte objects.</p>
 * 
 * <p><strong>Note: This class is NOT thread-safe.</strong> If multiple threads access a
 * MutableByte instance concurrently, and at least one thread modifies it, external
 * synchronization is required.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * MutableByte counter = MutableByte.of((byte)0);
 * byteArray.forEach(b -> counter.add(b));
 * System.out.println("Sum: " + counter.value());
 * }</pre>
 * 
 * <p>Note: This class is adapted from Apache Commons Lang.</p>
 * 
 * @version $Id: MutableByte.java 1669791 2015-03-28 15:22:59Z britter $
 * @see Byte
 * @see Number
 * @see Comparable
 * @see Mutable
 */
public final class MutableByte extends Number implements Comparable<MutableByte>, Mutable {

    @Serial
    private static final long serialVersionUID = -1585823265L;

    /**
     * The mutable byte value.
     */
    private byte value;

    /**
     * Constructs a new MutableByte with the default value of zero.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableByte num = new MutableByte(); // value is 0
     * }</pre>
     */
    MutableByte() {
    }

    /**
     * Constructs a new MutableByte with the specified initial value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableByte num = new MutableByte((byte)127); // value is 127
     * }</pre>
     * 
     * @param value the initial value to store
     */
    MutableByte(final byte value) {
        this.value = value;
    }

    /**
     * Creates a new MutableByte instance with the specified value.
     * This is a factory method that provides a more fluent way to create instances.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableByte flags = MutableByte.of((byte)0xFF);
     * }</pre>
     * 
     * @param value the initial value
     * @return a new MutableByte instance containing the specified value
     */
    public static MutableByte of(final byte value) {
        return new MutableByte(value);
    }

    /**
     * Returns the current byte value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableByte num = MutableByte.of((byte)42);
     * byte val = num.value(); // returns 42
     * }</pre>
     * 
     * @return the current byte value
     */
    public byte value() {
        return value;
    }

    //-----------------------------------------------------------------------

    /**
     * Gets the value as a byte primitive.
     *
     * <p>This method is deprecated in favor of {@link #value()} which provides
     * the same functionality with a more concise name.</p>
     *
     * @return the current byte value
     * @deprecated replace by {@link #value()}.
     */
    @Deprecated
    public byte getValue() {
        return value;
    }

    /**
     * Sets the value to the specified byte.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableByte num = MutableByte.of((byte)10);
     * num.setValue((byte)20); // value is now 20
     * }</pre>
     * 
     * @param value the value to set
     */
    public void setValue(final byte value) {
        this.value = value;
    }

    /**
     * Returns the current value and then sets the new value.
     * This is an atomic-like operation for single-threaded use.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableByte num = MutableByte.of((byte)10);
     * byte old = num.getAndSet((byte)20); // returns 10, value is now 20
     * }</pre>
     * 
     * @param value the new value to set
     * @return the value before it was updated
     */
    public byte getAndSet(final byte value) {
        final byte result = this.value;
        this.value = value;
        return result;
    }

    /**
     * Sets the value and then returns it.
     * This is useful when you want to update and immediately use the new value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableByte num = MutableByte.of((byte)10);
     * byte newVal = num.setAndGet((byte)20); // returns 20, value is now 20
     * }</pre>
     * 
     * @param value the new value to set
     * @return the new value after it has been set
     */
    public byte setAndGet(final byte value) {
        this.value = value;
        return this.value;
    }

    /**
     * Sets the value to newValue if the predicate evaluates to {@code true} for the current value.
     * If the predicate returns {@code false}, the value remains unchanged.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableByte num = MutableByte.of((byte)10);
     * boolean updated = num.setIf((byte)20, v -> v < 15); // returns true, value is now 20
     * updated = num.setIf((byte)30, v -> v < 15); // returns false, value remains 20
     * }</pre>
     * 
     * @param <E> the type of exception the predicate may throw
     * @param newValue the new value to set if the condition is met
     * @param predicate the predicate to test the current value
     * @return {@code true} if the value was updated, {@code false} otherwise
     * @throws E if the predicate throws an exception
     */
    public <E extends Exception> boolean setIf(final byte newValue, final Throwables.BytePredicate<E> predicate) throws E {
        if (predicate.test(value)) {
            value = newValue;
            return true;
        }

        return false;
    }

    //-----------------------------------------------------------------------

    /**
     * Increments the value by one.
     * Note: byte overflow will wrap around (127 + 1 = -128).
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableByte num = MutableByte.of((byte)10);
     * num.increment(); // value is now 11
     * }</pre>
     */
    public void increment() {
        value++;
    }

    /**
     * Decrements the value by one.
     * Note: byte underflow will wrap around (-128 - 1 = 127).
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableByte num = MutableByte.of((byte)10);
     * num.decrement(); // value is now 9
     * }</pre>
     */
    public void decrement() {
        value--;
    }

    //-----------------------------------------------------------------------

    /**
     * Adds the specified operand to the current value.
     * Note: byte overflow will wrap around.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableByte num = MutableByte.of((byte)10);
     * num.add((byte)5); // value is now 15
     * }</pre>
     * 
     * @param operand the value to add
     */
    public void add(final byte operand) {
        value += operand;
    }

    /**
     * Subtracts the specified operand from the current value.
     * Note: byte underflow will wrap around.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableByte num = MutableByte.of((byte)10);
     * num.subtract((byte)3); // value is now 7
     * }</pre>
     * 
     * @param operand the value to subtract
     */
    public void subtract(final byte operand) {
        value -= operand;
    }

    /**
     * Returns the current value and then increments it by one.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableByte num = MutableByte.of((byte)10);
     * byte old = num.getAndIncrement(); // returns 10, value is now 11
     * }</pre>
     * 
     * @return the value before incrementing
     */
    public byte getAndIncrement() {
        return value++;
    }

    /**
     * Returns the current value and then decrements it by one.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableByte num = MutableByte.of((byte)10);
     * byte old = num.getAndDecrement(); // returns 10, value is now 9
     * }</pre>
     * 
     * @return the value before decrementing
     */
    public byte getAndDecrement() {
        return value--;
    }

    /**
     * Increments the value by one and then returns it.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableByte num = MutableByte.of((byte)10);
     * byte newVal = num.incrementAndGet(); // returns 11, value is now 11
     * }</pre>
     * 
     * @return the value after incrementing
     */
    public byte incrementAndGet() {
        return ++value;
    }

    /**
     * Decrements the value by one and then returns it.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableByte num = MutableByte.of((byte)10);
     * byte newVal = num.decrementAndGet(); // returns 9, value is now 9
     * }</pre>
     * 
     * @return the value after decrementing
     */
    public byte decrementAndGet() {
        return --value;
    }

    /**
     * Returns the current value and then adds the specified delta.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableByte num = MutableByte.of((byte)10);
     * byte old = num.getAndAdd((byte)5); // returns 10, value is now 15
     * }</pre>
     * 
     * @param delta the value to add
     * @return the value before adding
     */
    public byte getAndAdd(final byte delta) {
        final byte prev = value;
        value += delta;
        return prev;
    }

    /**
     * Adds the specified delta to the current value and then returns it.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableByte num = MutableByte.of((byte)10);
     * byte newVal = num.addAndGet((byte)5); // returns 15, value is now 15
     * }</pre>
     * 
     * @param delta the value to add
     * @return the value after adding
     */
    public byte addAndGet(final byte delta) {
        return value += delta;
    }

    //-----------------------------------------------------------------------
    // shortValue relies on Number implementation

    /**
     * Returns the value of this MutableByte as a byte.
     * This is equivalent to calling {@link #value()}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableByte num = MutableByte.of((byte)42);
     * byte val = num.byteValue(); // returns 42
     * }</pre>
     *
     * @return the byte value represented by this object
     */
    @Override
    public byte byteValue() {
        return value;
    }

    /**
     * Returns the value of this MutableByte as a short after a widening primitive conversion.
     * The byte value is sign-extended to create the short value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableByte num = MutableByte.of((byte)-1);
     * short val = num.shortValue(); // returns -1 (sign-extended)
     * }</pre>
     *
     * @return the numeric value represented by this object after conversion to type short
     */
    @Override
    public short shortValue() {
        return value;
    }

    /**
     * Returns the value of this MutableByte as an int after a widening primitive conversion.
     * The byte value is sign-extended to create the int value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableByte num = MutableByte.of((byte)100);
     * int val = num.intValue(); // returns 100
     * }</pre>
     *
     * @return the numeric value represented by this object after conversion to type int
     */
    @Override
    public int intValue() {
        return value;
    }

    /**
     * Returns the value of this MutableByte as a long after a widening primitive conversion.
     * The byte value is sign-extended to create the long value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableByte num = MutableByte.of((byte)50);
     * long val = num.longValue(); // returns 50L
     * }</pre>
     *
     * @return the numeric value represented by this object after conversion to type long
     */
    @Override
    public long longValue() {
        return value;
    }

    /**
     * Returns the value of this MutableByte as a float after a widening primitive conversion.
     * The conversion is exact (no loss of precision).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableByte num = MutableByte.of((byte)75);
     * float val = num.floatValue(); // returns 75.0f
     * }</pre>
     *
     * @return the numeric value represented by this object after conversion to type float
     */
    @Override
    public float floatValue() {
        return value;
    }

    /**
     * Returns the value of this MutableByte as a double after a widening primitive conversion.
     * The conversion is exact (no loss of precision).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableByte num = MutableByte.of((byte)-128);
     * double val = num.doubleValue(); // returns -128.0
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
     * Compares this MutableByte to another MutableByte in ascending order.
     * Returns a negative value if this is less than the other, zero if equal,
     * or a positive value if this is greater than the other.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableByte a = MutableByte.of((byte)10);
     * MutableByte b = MutableByte.of((byte)20);
     * int result = a.compareTo(b); // returns negative value
     * }</pre>
     * 
     * @param other the other MutableByte to compare to, not null
     * @return negative if this is less, zero if equal, positive if greater
     */
    @Override
    public int compareTo(final MutableByte other) {
        return Byte.compare(value, other.value);
    }

    //-----------------------------------------------------------------------

    /**
     * Compares this object to the specified object. The result is {@code true} if and only if
     * the argument is not {@code null} and is a {@code MutableByte} object that contains the
     * same {@code byte} value as this object.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableByte a = MutableByte.of((byte)10);
     * MutableByte b = MutableByte.of((byte)10);
     * boolean equal = a.equals(b); // returns true
     * }</pre>
     * 
     * @param obj the object to compare with, {@code null} returns false
     * @return {@code true} if the objects are the same; {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof MutableByte) {
            return value == ((MutableByte) obj).value;
        }
        return false;
    }

    /**
     * Returns a hash code for this MutableByte.
     * The hash code is equal to the byte value cast to an int.
     *
     * <p>This implementation ensures that two MutableByte objects with equal values
     * will have equal hash codes, satisfying the {@link Object#hashCode()} contract.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableByte num = MutableByte.of((byte)42);
     * int hash = num.hashCode(); // returns 42
     * }</pre>
     *
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        return value;
    }

    //-----------------------------------------------------------------------

    /**
     * Returns the String representation of this MutableByte's value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableByte num = MutableByte.of((byte)42);
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
