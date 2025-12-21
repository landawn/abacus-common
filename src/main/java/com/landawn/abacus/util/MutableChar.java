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
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * MutableChar letter = MutableChar.of('A');
 * letter.increment();       // Now 'B'
 * letter.incrementAndGet(); // Now 'C', returns 'C'
 * char old = letter.getAndSet('Z');  // old is 'C', now 'Z'
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

    /**
     * The mutable char value.
     */
    private char value;

    /**
     * Constructs a new MutableChar with the default value of zero (null character '\u0000').
     * This constructor has package-private visibility.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableChar ch = new MutableChar();   // value is '\0'
     * }</pre>
     */
    MutableChar() {
    }

    /**
     * Constructs a new MutableChar with the specified initial value.
     * This constructor has package-private visibility.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableChar ch = new MutableChar('X');   // value is 'X'
     * }</pre>
     *
     * @param value the initial value to store
     */
    MutableChar(final char value) {
        this.value = value;
    }

    /**
     * Creates a new MutableChar instance with the specified value.
     * This is a factory method that provides a more fluent way to create instances
     * compared to using the constructor directly.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableChar letter = MutableChar.of('Z');
     * }</pre>
     *
     * @param value the initial char value
     * @return a new MutableChar instance containing the specified value
     */
    public static MutableChar of(final char value) {
        return new MutableChar(value);
    }

    /**
     * Returns the current char value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableChar ch = MutableChar.of('A');
     * char val = ch.value();   // returns 'A'
     * }</pre>
     *
     * @return the current char value
     */
    public char value() {
        return value;
    }

    //-----------------------------------------------------------------------

    /**
     * Gets the value as a char.
     *
     * <p><strong>Deprecated:</strong> Use {@link #value()} instead for better naming consistency.</p>
     *
     * @return the current char value
     * @deprecated replaced by {@link #value()}.
     */
    @Deprecated
    public char getValue() {
        return value;
    }

    /**
     * Sets the value to the specified char.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableChar ch = MutableChar.of('A');
     * ch.setValue('B');   // value is now 'B'
     * }</pre>
     *
     * @param value the value to set
     */
    public void setValue(final char value) {
        this.value = value;
    }

    /**
     * Returns the current value and then atomically sets it to the new value.
     * This operation is useful for retrieving the old value while updating to a new one
     * in a single operation (though not thread-safe without external synchronization).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableChar ch = MutableChar.of('A');
     * char old = ch.getAndSet('B');     // returns 'A', value is now 'B'
     * System.out.println(old);          // prints 'A'
     * System.out.println(ch.value());   // prints 'B'
     * }</pre>
     *
     * @param value the new value to set
     * @return the previous value before the update
     */
    public char getAndSet(final char value) {
        final char result = this.value;
        this.value = value;
        return result;
    }

    /**
     * Sets the value to the new value and then returns it.
     * This is useful when you want to update the value and immediately use the new value
     * in the same expression.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableChar ch = MutableChar.of('A');
     * char newVal = ch.setAndGet('B');   // returns 'B', value is now 'B'
     * System.out.println(newVal);        // prints 'B'
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
     * Conditionally sets the value to newValue if the predicate evaluates to {@code true} for the current value.
     * If the predicate returns {@code false}, the value remains unchanged.
     *
     * <p>This method is useful for conditional updates based on the current state.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableChar ch = MutableChar.of('A');
     * // Update only if current value is less than 'M'
     * boolean updated = ch.setIf('Z', c -> c < 'M');   // returns true, value is now 'Z'
     * // This update will fail because 'Z' is not < 'M'
     * updated = ch.setIf('A', c -> c < 'M');           // returns false, value remains 'Z'
     * }</pre>
     *
     * @param <E> the type of exception that the predicate may throw
     * @param newValue the new value to set if the condition is met
     * @param predicate the predicate to test against the current value
     * @return {@code true} if the value was updated (predicate returned true), {@code false} otherwise
     * @throws E if the predicate throws an exception during evaluation
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
     * Increments the value by one, moving to the next character in Unicode order.
     * This is equivalent to the {@code ++} operator for primitives.
     *
     * <p>Note: This operation wraps around if incrementing causes overflow
     * (e.g., incrementing '\uffff' results in '\u0000').</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableChar ch = MutableChar.of('A');
     * ch.increment();   // value is now 'B'
     * ch.increment();   // value is now 'C'
     * }</pre>
     */
    public void increment() {
        value++;
    }

    /**
     * Decrements the value by one, moving to the previous character in Unicode order.
     * This is equivalent to the {@code --} operator for primitives.
     *
     * <p>Note: This operation wraps around if decrementing causes underflow
     * (e.g., decrementing '\u0000' results in '\uffff').</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableChar ch = MutableChar.of('B');
     * ch.decrement();   // value is now 'A'
     * }</pre>
     */
    public void decrement() {
        value--;
    }

    //-----------------------------------------------------------------------

    /**
     * Returns the current value and then increments it by one.
     * This mimics the post-increment operator ({@code value++}) for primitives.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableChar ch = MutableChar.of('A');
     * char old = ch.getAndIncrement();   // returns 'A', value is now 'B'
     * System.out.println(old);           // prints 'A'
     * System.out.println(ch.value());    // prints 'B'
     * }</pre>
     *
     * @return the value before incrementing
     */
    public char getAndIncrement() {
        return value++;
    }

    /**
     * Returns the current value and then decrements it by one.
     * This mimics the post-decrement operator ({@code value--}) for primitives.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableChar ch = MutableChar.of('B');
     * char old = ch.getAndDecrement();   // returns 'B', value is now 'A'
     * System.out.println(old);           // prints 'B'
     * System.out.println(ch.value());    // prints 'A'
     * }</pre>
     *
     * @return the value before decrementing
     */
    public char getAndDecrement() {
        return value--;
    }

    /**
     * Increments the value by one and then returns the new value.
     * This mimics the pre-increment operator ({@code ++value}) for primitives.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableChar ch = MutableChar.of('A');
     * char newVal = ch.incrementAndGet();   // returns 'B', value is now 'B'
     * System.out.println(newVal);           // prints 'B'
     * }</pre>
     *
     * @return the value after incrementing
     */
    public char incrementAndGet() {
        return ++value;
    }

    /**
     * Decrements the value by one and then returns the new value.
     * This mimics the pre-decrement operator ({@code --value}) for primitives.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableChar ch = MutableChar.of('B');
     * char newVal = ch.decrementAndGet();   // returns 'A', value is now 'A'
     * System.out.println(newVal);           // prints 'A'
     * }</pre>
     *
     * @return the value after decrementing
     */
    public char decrementAndGet() {
        return --value;
    }

    //-----------------------------------------------------------------------

    /**
     * Compares this MutableChar to another MutableChar.
     * The comparison is based on the underlying char values in natural ascending order.
     *
     * <p>Returns a negative integer if this value is less than the other,
     * zero if they are equal, or a positive integer if this value is greater.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableChar a = MutableChar.of('A');
     * MutableChar b = MutableChar.of('B');
     * int result = a.compareTo(b);   // returns negative value (A < B)
     *
     * MutableChar c = MutableChar.of('A');
     * result = a.compareTo(c);   // returns 0 (A == A)
     *
     * result = b.compareTo(a);   // returns positive value (B > A)
     * }</pre>
     *
     * @param other the other MutableChar to compare to
     * @return a negative integer, zero, or a positive integer as this value
     *         is less than, equal to, or greater than the specified value
     * @throws NullPointerException if {@code other} is null
     */
    @Override
    public int compareTo(final MutableChar other) {
        return Character.compare(value, other.value);
    }

    //-----------------------------------------------------------------------

    /**
     * Compares this object to the specified object for equality.
     * The result is {@code true} if and only if the argument is not {@code null},
     * is a {@code MutableChar} object, and contains the same {@code char} value as this object.
     *
     * <p>Note: This does not consider a MutableChar equal to a Character object with the same value.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableChar a = MutableChar.of('X');
     * MutableChar b = MutableChar.of('X');
     * MutableChar c = MutableChar.of('Y');
     *
     * boolean equal = a.equals(b);                // returns true
     * equal = a.equals(c);                        // returns false
     * equal = a.equals(null);                     // returns false
     * equal = a.equals(Character.valueOf('X'));   // returns false (different types)
     * }</pre>
     *
     * @param obj the object to compare with, may be {@code null}
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
     * The hash code is equal to the numeric value of the contained char.
     * This ensures that two MutableChar instances with the same value will have the same hash code,
     * making them suitable for use in hash-based collections.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableChar a = MutableChar.of('A');
     * MutableChar b = MutableChar.of('A');
     *
     * int hash1 = a.hashCode();   // returns 65 (Unicode value of 'A')
     * int hash2 = b.hashCode();   // returns 65
     * assert hash1 == hash2;  // true
     * }</pre>
     *
     * @return a hash code value for this object, equal to the char value
     */
    @Override
    public int hashCode() {
        return value;
    }

    //-----------------------------------------------------------------------

    /**
     * Returns the String representation of this MutableChar's value.
     * The returned string contains a single character - the value of this MutableChar.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableChar ch = MutableChar.of('X');
     * String str = ch.toString();   // returns "X"
     *
     * MutableChar newline = MutableChar.of('\n');
     * String str2 = newline.toString();   // returns a string containing a newline character
     * }</pre>
     *
     * @return the String representation of the current char value
     */
    @Override
    public String toString() {
        return N.stringOf(value);
    }
}
