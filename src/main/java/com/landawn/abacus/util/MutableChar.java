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
import java.io.Serializable;

/**
 * A mutable wrapper for a {@code char} value, providing methods to modify the wrapped value.
 * 
 * <p>This class is useful in scenarios where you need to pass a char by reference,
 * modify characters in lambda expressions, or store changing char values in collections
 * without creating new Character objects.</p>
 * 
 * <p><strong>Note: This class is NOT thread-safe.</strong> If multiple threads access a
 * MutableChar instance concurrently, and at least one thread modifies it, external
 * synchronization is required.</p>
 * 
 * <p>Note that MutableChar does not extend Character, so it is not treated by
 * {@code String.format} as a Character parameter.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * MutableChar letter = MutableChar.of('A');
 * letter.increment(); // Now 'B'
 * letter.add(2); // Now 'D'
 * }</pre>
 * 
 * <p>Note: This class is adapted from Apache Commons Lang.</p>
 * 
 * @version $Id: MutableChar.java 1669791 2015-03-28 15:22:59Z britter $
 * @see Character
 * @see Serializable
 * @see Comparable
 * @see Mutable
 */
public final class MutableChar implements Mutable, Serializable, Comparable<MutableChar> {
    @Serial
    private static final long serialVersionUID = 6807507696378901820L;

    private char value;

    /**
     * Constructs a new MutableChar with the default value of zero (null character).
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableChar ch = new MutableChar(); // value is '\0'
     * }</pre>
     */
    MutableChar() {
    }

    /**
     * Constructs a new MutableChar with the specified initial value.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableChar ch = new MutableChar('X'); // value is 'X'
     * }</pre>
     * 
     * @param value the initial value to store
     */
    MutableChar(final char value) {
        this.value = value;
    }

    /**
     * Creates a new MutableChar instance with the specified value.
     * This is a factory method that provides a more fluent way to create instances.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableChar letter = MutableChar.of('Z');
     * }</pre>
     * 
     * @param value the initial value
     * @return a new MutableChar instance containing the specified value
     */
    public static MutableChar of(final char value) {
        return new MutableChar(value);
    }

    /**
     * Returns the current char value.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableChar ch = MutableChar.of('A');
     * char val = ch.value(); // returns 'A'
     * }</pre>
     * 
     * @return the current char value
     */
    public char value() {
        return value;
    }

    //-----------------------------------------------------------------------

    /**
     * Gets the value as a Character instance.
     * 
     * @return the current value
     * @deprecated replace by {@link #value()}.
     */
    @Deprecated
    public char getValue() {
        return value;
    }

    /**
     * Sets the value to the specified char.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableChar ch = MutableChar.of('A');
     * ch.setValue('B'); // value is now 'B'
     * }</pre>
     * 
     * @param value the value to set
     */
    public void setValue(final char value) {
        this.value = value;
    }

    /**
     * Returns the current value and then sets the new value.
     * This is an atomic-like operation for single-threaded use.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableChar ch = MutableChar.of('A');
     * char old = ch.getAndSet('B'); // returns 'A', value is now 'B'
     * }</pre>
     * 
     * @param value the new value to set
     * @return the value before it was updated
     */
    public char getAndSet(final char value) {
        final char result = this.value;
        this.value = value;
        return result;
    }

    /**
     * Sets the value and then returns it.
     * This is useful when you want to update and immediately use the new value.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableChar ch = MutableChar.of('A');
     * char newVal = ch.setAndGet('B'); // returns 'B', value is now 'B'
     * }</pre>
     * 
     * @param value the new value to set
     * @return the new value after it has been set
     */
    public char setAndGet(final char value) {
        this.value = value;
        return this.value;
    }

    /**
     * Sets the value to newValue if the predicate evaluates to true for the current value.
     * If the predicate returns false, the value remains unchanged.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableChar ch = MutableChar.of('A');
     * boolean updated = ch.setIf('Z', c -> c < 'M'); // returns true, value is now 'Z'
     * updated = ch.setIf('A', c -> c < 'M'); // returns false, value remains 'Z'
     * }</pre>
     * 
     * @param <E> the type of exception the predicate may throw
     * @param newValue the new value to set if the condition is met
     * @param predicate the predicate to test the current value
     * @return {@code true} if the value was updated, {@code false} otherwise
     * @throws E if the predicate throws an exception
     */
    public <E extends Exception> boolean setIf(final char newValue, final Throwables.CharPredicate<E> predicate) throws E {
        if (predicate.test(value)) {
            value = newValue;
            return true;
        }

        return false;
    }

    //-----------------------------------------------------------------------

    /**
     * Increments the value by one.
     * This moves to the next character in Unicode order.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableChar ch = MutableChar.of('A');
     * ch.increment(); // value is now 'B'
     * }</pre>
     */
    public void increment() {
        value++;
    }

    /**
     * Decrements the value by one.
     * This moves to the previous character in Unicode order.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableChar ch = MutableChar.of('B');
     * ch.decrement(); // value is now 'A'
     * }</pre>
     */
    public void decrement() {
        value--;
    }

    //-----------------------------------------------------------------------

    /**
     * Adds the specified operand to the current value.
     * The operand is treated as a numeric offset in Unicode.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableChar ch = MutableChar.of('A');
     * ch.add(3); // value is now 'D'
     * }</pre>
     * 
     * @param operand the value to add (as Unicode offset)
     */
    public void add(final char operand) {
        value += operand;
    }

    /**
     * Subtracts the specified operand from the current value.
     * The operand is treated as a numeric offset in Unicode.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableChar ch = MutableChar.of('D');
     * ch.subtract(3); // value is now 'A'
     * }</pre>
     * 
     * @param operand the value to subtract (as Unicode offset)
     */
    public void subtract(final char operand) {
        value -= operand;
    }

    /**
     * Returns the current value and then increments it by one.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableChar ch = MutableChar.of('A');
     * char old = ch.getAndIncrement(); // returns 'A', value is now 'B'
     * }</pre>
     * 
     * @return the value before incrementing
     */
    public char getAndIncrement() {
        return value++;
    }

    /**
     * Returns the current value and then decrements it by one.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableChar ch = MutableChar.of('B');
     * char old = ch.getAndDecrement(); // returns 'B', value is now 'A'
     * }</pre>
     * 
     * @return the value before decrementing
     */
    public char getAndDecrement() {
        return value--;
    }

    /**
     * Increments the value by one and then returns it.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableChar ch = MutableChar.of('A');
     * char newVal = ch.incrementAndGet(); // returns 'B', value is now 'B'
     * }</pre>
     * 
     * @return the value after incrementing
     */
    public char incrementAndGet() {
        return ++value;
    }

    /**
     * Decrements the value by one and then returns it.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableChar ch = MutableChar.of('B');
     * char newVal = ch.decrementAndGet(); // returns 'A', value is now 'A'
     * }</pre>
     * 
     * @return the value after decrementing
     */
    public char decrementAndGet() {
        return --value;
    }

    /**
     * Returns the current value and then adds the specified delta.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableChar ch = MutableChar.of('A');
     * char old = ch.getAndAdd(5); // returns 'A', value is now 'F'
     * }</pre>
     * 
     * @param delta the value to add (as Unicode offset)
     * @return the value before adding
     */
    public char getAndAdd(final char delta) {
        final char prev = value;
        value += delta;
        return prev;
    }

    /**
     * Adds the specified delta to the current value and then returns it.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableChar ch = MutableChar.of('A');
     * char newVal = ch.addAndGet(5); // returns 'F', value is now 'F'
     * }</pre>
     * 
     * @param delta the value to add (as Unicode offset)
     * @return the value after adding
     */
    public char addAndGet(final char delta) {
        return value += delta;
    }

    //-----------------------------------------------------------------------

    /**
     * Compares this MutableChar to another MutableChar in ascending order.
     * Returns a negative value if this is less than the other, zero if equal,
     * or a positive value if this is greater than the other.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableChar a = MutableChar.of('A');
     * MutableChar b = MutableChar.of('B');
     * int result = a.compareTo(b); // returns negative value
     * }</pre>
     * 
     * @param other the other MutableChar to compare to, not null
     * @return negative if this is less, zero if equal, positive if greater
     */
    @Override
    public int compareTo(final MutableChar other) {
        return Character.compare(value, other.value);
    }

    //-----------------------------------------------------------------------

    /**
     * Compares this object to the specified object. The result is {@code true} if and only if
     * the argument is not {@code null} and is a {@code MutableChar} object that contains the
     * same {@code char} value as this object.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableChar a = MutableChar.of('X');
     * MutableChar b = MutableChar.of('X');
     * boolean equal = a.equals(b); // returns true
     * }</pre>
     * 
     * @param obj the object to compare with, {@code null} returns false
     * @return {@code true} if the objects are the same; {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof MutableChar) {
            return value == ((MutableChar) obj).value;
        }

        return false;
    }

    /**
     * Returns a hash code for this MutableChar.
     * The hash code is equal to the char value.
     * 
     * @return a suitable hash code
     */
    @Override
    public int hashCode() {
        return value;
    }

    //-----------------------------------------------------------------------

    /**
     * Returns the String representation of this MutableChar's value.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MutableChar ch = MutableChar.of('X');
     * String str = ch.toString(); // returns "X"
     * }</pre>
     * 
     * @return the String representation of the current value
     */
    @Override
    public String toString() {
        return N.stringOf(value);
    }
}