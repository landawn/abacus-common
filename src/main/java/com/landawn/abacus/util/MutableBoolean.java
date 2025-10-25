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
 * A mutable wrapper for a {@code boolean} value, providing methods to modify the wrapped value.
 * 
 * <p>This class is useful in scenarios where you need to pass a boolean by reference,
 * toggle flags in lambda expressions, or store changing boolean values in collections
 * without creating new Boolean objects.</p>
 * 
 * <p><strong>Note: This class is NOT thread-safe.</strong> If multiple threads access a
 * MutableBoolean instance concurrently, and at least one thread modifies it, external
 * synchronization is required.</p>
 * 
 * <p>Note that MutableBoolean does not extend Boolean, so it is not treated by
 * {@code String.format} as a Boolean parameter.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * MutableBoolean flag = MutableBoolean.of(false);
 * list.forEach(item -> {
 *     if (item.meetsCondition()) {
 *         flag.setTrue();
 *     }
 * });
 * System.out.println("Condition met: " + flag.value());
 * }</pre>
 * 
 * <p>Note: This class is adapted from Apache Commons Lang.</p>
 * 
 * @version $Id: MutableBoolean.java 1669791 2015-03-28 15:22:59Z britter $
 * @see Boolean
 * @see Serializable
 * @see Comparable
 * @see Mutable
 */
public final class MutableBoolean implements Mutable, Serializable, Comparable<MutableBoolean> {

    @Serial
    private static final long serialVersionUID = -4830728138360036487L;

    private boolean value;

    /**
     * Constructs a new MutableBoolean with the default value of {@code false}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableBoolean flag = new MutableBoolean(); // value is false
     * }</pre>
     */
    MutableBoolean() {
    }

    /**
     * Constructs a new MutableBoolean with the specified initial value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableBoolean flag = new MutableBoolean(true); // value is true
     * }</pre>
     * 
     * @param value the initial value to store
     */
    MutableBoolean(final boolean value) {
        this.value = value;
    }

    /**
     * Creates a new MutableBoolean instance with the specified value.
     * This is a factory method that provides a more fluent way to create instances.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableBoolean enabled = MutableBoolean.of(true);
     * }</pre>
     * 
     * @param value the initial value
     * @return a new MutableBoolean instance containing the specified value
     */
    public static MutableBoolean of(final boolean value) {
        return new MutableBoolean(value);
    }

    /**
     * Returns the current boolean value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableBoolean flag = MutableBoolean.of(true);
     * boolean val = flag.value(); // returns true
     * }</pre>
     * 
     * @return the current boolean value
     */
    public boolean value() {
        return value;
    }

    //-----------------------------------------------------------------------

    /**
     * Gets the value as a Boolean instance.
     * 
     * @return the current value
     * @deprecated replace by {@link #value()}.
     */
    @Deprecated
    public boolean getValue() { // NOSONAR
        return value;
    }

    /**
     * Sets the value to the specified boolean.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableBoolean flag = MutableBoolean.of(false);
     * flag.setValue(true); // value is now true
     * }</pre>
     * 
     * @param value the value to set
     */
    public void setValue(final boolean value) {
        this.value = value;
    }

    /**
     * Returns the current value and then sets the new value.
     * This is an atomic-like operation for single-threaded use.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableBoolean flag = MutableBoolean.of(true);
     * boolean old = flag.getAndSet(false); // returns true, value is now false
     * }</pre>
     * 
     * @param value the new value to set
     * @return the value before it was updated
     */
    public boolean getAndSet(final boolean value) { // NOSONAR
        final boolean result = this.value;
        this.value = value;
        return result;
    }

    /**
     * Sets the value and then returns it.
     * This is useful when you want to update and immediately use the new value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableBoolean flag = MutableBoolean.of(false);
     * boolean newVal = flag.setAndGet(true); // returns true, value is now true
     * }</pre>
     * 
     * @param value the new value to set
     * @return the new value after it has been set
     */
    public boolean setAndGet(final boolean value) {
        this.value = value;
        return this.value;
    }

    /**
     * Returns the current value and then inverts it (true becomes false, {@code false} becomes true).
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableBoolean flag = MutableBoolean.of(true);
     * boolean old = flag.getAndNegate(); // returns true, value is now false
     * }</pre>
     * 
     * @return the value before negating
     */
    public boolean getAndNegate() { // NOSONAR
        final boolean result = value;
        value = !value;
        return result;
    }

    /**
     * Inverts the current value and then returns it (true becomes false, {@code false} becomes true).
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableBoolean flag = MutableBoolean.of(true);
     * boolean newVal = flag.negateAndGet(); // returns false, value is now false
     * }</pre>
     * 
     * @return the value after negating
     */
    public boolean negateAndGet() {
        value = !value;
        return value;
    }

    /**
     * Sets the value to newValue if the predicate evaluates to true for the current value.
     * If the predicate returns false, the value remains unchanged.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableBoolean flag = MutableBoolean.of(false);
     * boolean updated = flag.setIf(true, v -> !v); // returns true, value is now true
     * updated = flag.setIf(false, v -> !v); // returns false, value remains true
     * }</pre>
     * 
     * @param <E> the type of exception the predicate may throw
     * @param newValue the new value to set if the condition is met
     * @param predicate the predicate to test the current value
     * @return {@code true} if the value was updated, {@code false} otherwise
     * @throws E if the predicate throws an exception
     */
    public <E extends Exception> boolean setIf(final boolean newValue, final Throwables.BooleanPredicate<E> predicate) throws E {
        if (predicate.test(value)) {
            value = newValue;
            return true;
        }

        return false;
    }

    /**
     * Sets the value to {@code false}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableBoolean flag = MutableBoolean.of(true);
     * flag.setFalse(); // value is now false
     * }</pre>
     */
    public void setFalse() {
        value = false;
    }

    /**
     * Sets the value to {@code true}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableBoolean flag = MutableBoolean.of(false);
     * flag.setTrue(); // value is now true
     * }</pre>
     */
    public void setTrue() {
        value = true;
    }

    //-----------------------------------------------------------------------

    /**
     * Checks if the current value is {@code true}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableBoolean flag = MutableBoolean.of(true);
     * if (flag.isTrue()) {
     *     // execute when true
     * }
     * }</pre>
     * 
     * @return {@code true} if the current value is {@code true}
     */
    public boolean isTrue() { //NOSONAR
        return value;
    }

    /**
     * Checks if the current value is {@code false}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableBoolean flag = MutableBoolean.of(false);
     * if (flag.isFalse()) {
     *     // execute when false
     * }
     * }</pre>
     * 
     * @return {@code true} if the current value is {@code false}
     */
    public boolean isFalse() {
        return !value;
    }

    /**
     * Inverts the current value (true becomes false, {@code false} becomes true).
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableBoolean flag = MutableBoolean.of(true);
     * flag.invert(); // value is now false
     * flag.invert(); // value is now true again
     * }</pre>
     */
    public void invert() {
        value = !value;
    }

    //-----------------------------------------------------------------------

    /**
     * Compares this MutableBoolean to another MutableBoolean in ascending order.
     * {@code false} is considered less than {@code true}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableBoolean a = MutableBoolean.of(false);
     * MutableBoolean b = MutableBoolean.of(true);
     * int result = a.compareTo(b); // returns negative value (false < true)
     * }</pre>
     * 
     * @param other the other MutableBoolean to compare to, not null
     * @return negative if this is less (false < true), zero if equal, positive if greater
     */
    @Override
    public int compareTo(final MutableBoolean other) {
        return (value == other.value) ? 0 : (value ? 1 : -1);
    }

    //-----------------------------------------------------------------------

    /**
     * Compares this object to the specified object. The result is {@code true} if and only if
     * the argument is not {@code null} and is a {@code MutableBoolean} object that contains the
     * same {@code boolean} value as this object.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableBoolean a = MutableBoolean.of(true);
     * MutableBoolean b = MutableBoolean.of(true);
     * boolean equal = a.equals(b); // returns true
     * }</pre>
     * 
     * @param obj the object to compare with, {@code null} returns false
     * @return {@code true} if the objects are the same; {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof MutableBoolean) {
            return value == ((MutableBoolean) obj).value;
        }
        return false;
    }

    /**
     * Returns a hash code for this MutableBoolean.
     * The hash code is the same as Boolean.TRUE.hashCode() for true values
     * and Boolean.FALSE.hashCode() for false values.
     * 
     * @return a suitable hash code
     */
    @Override
    public int hashCode() {
        return value ? Boolean.TRUE.hashCode() : Boolean.FALSE.hashCode();
    }

    //-----------------------------------------------------------------------

    /**
     * Returns the String representation of this MutableBoolean's value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableBoolean flag = MutableBoolean.of(true);
     * String str = flag.toString(); // returns "true"
     * }</pre>
     * 
     * @return the String representation of the current value ("true" or "false")
     */
    @Override
    public String toString() {
        return N.stringOf(value);
    }

}