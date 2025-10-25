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
 * A mutable wrapper for a {@code float} value, providing methods to modify the wrapped value.
 * 
 * <p>This class is useful in scenarios where you need to pass a float by reference,
 * accumulate float values in lambda expressions, or store frequently changing float values
 * in collections without creating new Float objects.</p>
 * 
 * <p><strong>Note: This class is NOT thread-safe.</strong> If multiple threads access a
 * MutableFloat instance concurrently, and at least one thread modifies it, external
 * synchronization is required.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * MutableFloat sum = MutableFloat.of(0.0f);
 * floatList.forEach(value -> sum.add(value));
 * System.out.println("Total: " + sum.value());
 * }</pre>
 * 
 * <p>Note: This class is adapted from Apache Commons Lang.</p>
 * 
 * @version $Id: MutableFloat.java 1669791 2015-03-28 15:22:59Z britter $
 * @see Float
 * @see Number
 * @see Comparable
 * @see Mutable
 */
public final class MutableFloat extends Number implements Comparable<MutableFloat>, Mutable {

    @Serial
    private static final long serialVersionUID = 5787169186L;

    private float value;

    /**
     * Constructs a new MutableFloat with the default value of zero.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableFloat num = new MutableFloat(); // value is 0.0f
     * }</pre>
     */
    MutableFloat() {
    }

    /**
     * Constructs a new MutableFloat with the specified initial value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableFloat num = new MutableFloat(3.14f); // value is 3.14f
     * }</pre>
     * 
     * @param value the initial value to store
     */
    MutableFloat(final float value) {
        this.value = value;
    }

    /**
     * Creates a new MutableFloat instance with the specified value.
     * This is a factory method that provides a more fluent way to create instances.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableFloat temperature = MutableFloat.of(98.6f);
     * }</pre>
     * 
     * @param value the initial value
     * @return a new MutableFloat instance containing the specified value
     */
    public static MutableFloat of(final float value) {
        return new MutableFloat(value);
    }

    /**
     * Returns the current float value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableFloat num = MutableFloat.of(42.5f);
     * float val = num.value(); // returns 42.5f
     * }</pre>
     * 
     * @return the current float value
     */
    public float value() {
        return value;
    }

    //-----------------------------------------------------------------------

    /**
     * Gets the value as a Float instance.
     * 
     * @return the current value
     * @deprecated replace by {@link #value()}.
     */
    @Deprecated
    public float getValue() {
        return value;
    }

    /**
     * Sets the value to the specified float.
     *
     * <p>This method allows setting any valid float value including special values
     * like {@link Float#NaN}, {@link Float#POSITIVE_INFINITY}, and {@link Float#NEGATIVE_INFINITY}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableFloat num = MutableFloat.of(10.5f);
     * num.setValue(20.7f); // value is now 20.7f
     *
     * // Special values are allowed
     * num.setValue(Float.NaN);
     * num.setValue(Float.POSITIVE_INFINITY);
     * }</pre>
     *
     * @param value the value to set
     */
    public void setValue(final float value) {
        this.value = value;
    }

    /**
     * Returns the current value and then sets the new value.
     * This is an atomic-like operation for single-threaded use.
     *
     * <p>This method is useful when you need both the old and new values in a single operation,
     * such as calculating deltas or maintaining state transitions.</p>
     *
     * <p><strong>Note:</strong> While this operation appears atomic, this class is NOT thread-safe.
     * For multi-threaded scenarios, use {@link java.util.concurrent.atomic.AtomicReference} with Float.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableFloat num = MutableFloat.of(10.5f);
     * float old = num.getAndSet(20.7f); // returns 10.5f, value is now 20.7f
     *
     * // Useful for tracking changes
     * MutableFloat temperature = MutableFloat.of(98.6f);
     * float previousTemp = temperature.getAndSet(99.1f);
     * float delta = temperature.value() - previousTemp; // 0.5f
     * }</pre>
     *
     * @param value the new value to set
     * @return the value before it was updated
     */
    public float getAndSet(final float value) {
        final float result = this.value;
        this.value = value;
        return result;
    }

    /**
     * Sets the value and then returns it.
     * This is useful when you want to update and immediately use the new value.
     *
     * <p>This method is particularly useful in fluent programming and when you need to
     * both update a value and use it in the same expression.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableFloat num = MutableFloat.of(10.5f);
     * float newVal = num.setAndGet(20.7f); // returns 20.7f, value is now 20.7f
     *
     * // Useful in method chaining or inline assignments
     * MutableFloat score = MutableFloat.of(0.0f);
     * System.out.println("New score: " + score.setAndGet(95.5f)); // prints and updates
     *
     * // Can be used in calculations
     * float result = score.setAndGet(100.0f) * 0.9f; // Sets to 100.0f and calculates
     * }</pre>
     *
     * @param value the new value to set
     * @return the new value after it has been set
     */
    public float setAndGet(final float value) {
        this.value = value;
        return this.value;
    }

    /**
     * Sets the value to newValue if the predicate evaluates to true for the current value.
     * If the predicate returns false, the value remains unchanged.
     *
     * <p>This method provides conditional update functionality, useful for implementing
     * business rules, validation, or state machine transitions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableFloat num = MutableFloat.of(10.5f);
     * boolean updated = num.setIf(20.5f, v -> v < 15.0f); // returns true, value is now 20.5f
     * updated = num.setIf(30.5f, v -> v < 15.0f); // returns false, value remains 20.5f
     *
     * // More complex predicates
     * MutableFloat temperature = MutableFloat.of(98.6f);
     * // Only update if temperature is within normal range
     * temperature.setIf(99.5f, t -> t >= 97.0f && t <= 100.0f);
     *
     * // With exception handling
     * MutableFloat price = MutableFloat.of(100.0f);
     * price.setIf(120.0f, p -> {
     *     if (p < 0) throw new IllegalStateException("Negative price");
     *     return p < 150.0f;
     * });
     * }</pre>
     *
     * @param <E> the type of exception the predicate may throw
     * @param newValue the new value to set if the condition is met
     * @param predicate the predicate to test the current value
     * @return {@code true} if the value was updated, {@code false} otherwise
     * @throws E if the predicate throws an exception
     */
    public <E extends Exception> boolean setIf(final float newValue, final Throwables.FloatPredicate<E> predicate) throws E {
        if (predicate.test(value)) {
            value = newValue;
            return true;
        }

        return false;
    }

    //-----------------------------------------------------------------------

    /**
     * Checks whether the float value is the special NaN (Not a Number) value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableFloat num = MutableFloat.of(Float.NaN);
     * boolean isNaN = num.isNaN(); // returns true
     * }</pre>
     * 
     * @return {@code true} if the value is NaN, {@code false} otherwise
     */
    public boolean isNaN() {
        return Float.isNaN(value);
    }

    /**
     * Checks whether the float value is infinite (positive or negative infinity).
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableFloat num = MutableFloat.of(Float.POSITIVE_INFINITY);
     * boolean isInf = num.isInfinite(); // returns true
     * }</pre>
     * 
     * @return {@code true} if the value is infinite, {@code false} otherwise
     */
    public boolean isInfinite() {
        return Float.isInfinite(value);
    }

    //-----------------------------------------------------------------------

    /**
     * Increments the value by one.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableFloat num = MutableFloat.of(10.5f);
     * num.increment(); // value is now 11.5f
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
     * MutableFloat num = MutableFloat.of(10.5f);
     * num.decrement(); // value is now 9.5f
     * }</pre>
     */
    public void decrement() {
        value--;
    }

    //-----------------------------------------------------------------------

    /**
     * Adds the specified operand to the current value.
     *
     * <p>Note: Float addition may result in precision loss or overflow to infinity.
     * This operation follows standard IEEE 754 floating-point arithmetic rules.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableFloat num = MutableFloat.of(10.5f);
     * num.add(5.3f); // value is now 15.8f
     *
     * // Accumulating values in a loop
     * MutableFloat sum = MutableFloat.of(0.0f);
     * for (float value : floatArray) {
     *     sum.add(value);
     * }
     *
     * // Be aware of overflow
     * MutableFloat large = MutableFloat.of(Float.MAX_VALUE);
     * large.add(Float.MAX_VALUE); // Results in POSITIVE_INFINITY
     * }</pre>
     *
     * @param operand the value to add
     */
    public void add(final float operand) {
        value += operand;
    }

    /**
     * Subtracts the specified operand from the current value.
     *
     * <p>Note: Float subtraction may result in precision loss or underflow to negative infinity.
     * This operation follows standard IEEE 754 floating-point arithmetic rules.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableFloat num = MutableFloat.of(10.5f);
     * num.subtract(3.2f); // value is now 7.3f
     *
     * // Tracking decrements
     * MutableFloat balance = MutableFloat.of(1000.0f);
     * balance.subtract(250.0f); // balance is now 750.0f
     *
     * // Be aware of underflow
     * MutableFloat small = MutableFloat.of(-Float.MAX_VALUE);
     * small.subtract(Float.MAX_VALUE); // Results in NEGATIVE_INFINITY
     * }</pre>
     *
     * @param operand the value to subtract
     */
    public void subtract(final float operand) {
        value -= operand;
    }

    /**
     * Returns the current value and then increments it by one.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableFloat num = MutableFloat.of(10.5f);
     * float old = num.getAndIncrement(); // returns 10.5f, value is now 11.5f
     * }</pre>
     * 
     * @return the value before incrementing
     */
    public float getAndIncrement() {
        return value++;
    }

    /**
     * Returns the current value and then decrements it by one.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableFloat num = MutableFloat.of(10.5f);
     * float old = num.getAndDecrement(); // returns 10.5f, value is now 9.5f
     * }</pre>
     * 
     * @return the value before decrementing
     */
    public float getAndDecrement() {
        return value--;
    }

    /**
     * Increments the value by one and then returns it.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableFloat num = MutableFloat.of(10.5f);
     * float newVal = num.incrementAndGet(); // returns 11.5f, value is now 11.5f
     * }</pre>
     * 
     * @return the value after incrementing
     */
    public float incrementAndGet() {
        return ++value;
    }

    /**
     * Decrements the value by one and then returns it.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableFloat num = MutableFloat.of(10.5f);
     * float newVal = num.decrementAndGet(); // returns 9.5f, value is now 9.5f
     * }</pre>
     * 
     * @return the value after decrementing
     */
    public float decrementAndGet() {
        return --value;
    }

    /**
     * Returns the current value and then adds the specified delta.
     *
     * <p>This method is useful when you need to know the value before modification,
     * commonly used in delta calculations or state tracking.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableFloat num = MutableFloat.of(10.5f);
     * float old = num.getAndAdd(5.3f); // returns 10.5f, value is now 15.8f
     *
     * // Tracking changes
     * MutableFloat counter = MutableFloat.of(100.0f);
     * float previous = counter.getAndAdd(25.5f);
     * float change = counter.value() - previous; // 25.5f
     *
     * // Accumulating with history
     * MutableFloat total = MutableFloat.of(0.0f);
     * List<Float> history = new ArrayList<>();
     * history.add(total.getAndAdd(10.5f)); // Adds 10.5f, saves old value (0.0f)
     * history.add(total.getAndAdd(20.3f)); // Adds 20.3f, saves old value (10.5f)
     * }</pre>
     *
     * @param delta the value to add
     * @return the value before adding
     */
    public float getAndAdd(final float delta) {
        final float prev = value;
        value += delta;
        return prev;
    }

    /**
     * Adds the specified delta to the current value and then returns it.
     *
     * <p>This method is useful when you need the updated value immediately after modification,
     * commonly used in calculations or conditional logic.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableFloat num = MutableFloat.of(10.5f);
     * float newVal = num.addAndGet(5.3f); // returns 15.8f, value is now 15.8f
     *
     * // Using the result directly in calculations
     * MutableFloat score = MutableFloat.of(85.0f);
     * float bonus = score.addAndGet(10.0f) * 0.1f; // Adds 10, then calculates 10% of 95.0f
     *
     * // Conditional logic based on updated value
     * MutableFloat progress = MutableFloat.of(75.0f);
     * if (progress.addAndGet(15.0f) >= 100.0f) {
     *     System.out.println("Task complete!"); // Executes if progress reaches 100%
     * }
     * }</pre>
     *
     * @param delta the value to add
     * @return the value after adding
     */
    public float addAndGet(final float delta) {
        return value += delta;
    }

    //-----------------------------------------------------------------------
    // shortValue and byteValue rely on Number implementation

    /**
     * Returns the value of this MutableFloat as an int.
     * This may involve rounding or truncation.
     * 
     * @return the value as an int
     */
    @Override
    public int intValue() {
        return (int) value;
    }

    /**
     * Returns the value of this MutableFloat as a long.
     * This may involve rounding or truncation.
     * 
     * @return the value as a long
     */
    @Override
    public long longValue() {
        return (long) value;
    }

    /**
     * Returns the value of this MutableFloat as a float.
     * 
     * @return the float value
     */
    @Override
    public float floatValue() {
        return value;
    }

    /**
     * Returns the value of this MutableFloat as a double.
     * 
     * @return the value as a double
     */
    @Override
    public double doubleValue() {
        return value;
    }

    //-----------------------------------------------------------------------

    /**
     * Compares this MutableFloat to another MutableFloat in ascending order.
     * The comparison is consistent with Float.compare() and handles NaN values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableFloat a = MutableFloat.of(10.5f);
     * MutableFloat b = MutableFloat.of(20.5f);
     * int result = a.compareTo(b); // returns negative value
     * }</pre>
     *
     * @param other the other MutableFloat to compare to, not null
     * @return negative if this is less, zero if equal, positive if greater
     */
    @Override
    public int compareTo(final MutableFloat other) {
        if (other == null) {
            throw new NullPointerException("Cannot compare to null");
        }
        return Float.compare(value, other.value);
    }

    //-----------------------------------------------------------------------

    /**
     * Compares this object to the specified object. The result is {@code true} if and only if
     * the argument is not {@code null} and is a {@code MutableFloat} object that represents
     * a float that has the identical bit pattern to the bit pattern of the float represented
     * by this object.
     * 
     * <p>For this purpose, two float values are considered to be the same if and only if the
     * method {@link Float#floatToIntBits(float)} returns the same int value when applied to each.
     * This definition allows hash tables to operate properly.</p>
     * 
     * <p>Note that in most cases, for two instances of class {@code MutableFloat}, {@code f1}
     * and {@code f2}, the value of {@code f1.equals(f2)} is {@code true} if and only if
     * {@code f1.floatValue() == f2.floatValue()} also has the value {@code true}. However,
     * there are two exceptions:</p>
     * <ul>
     * <li>If {@code f1} and {@code f2} both represent {@code Float.NaN}, then the
     * {@code equals} method returns {@code true}, even though {@code Float.NaN==Float.NaN}
     * has the value {@code false}.</li>
     * <li>If {@code f1} represents {@code +0.0f} while {@code f2} represents {@code -0.0f},
     * or vice versa, the {@code equal} test has the value {@code false}, even though
     * {@code 0.0f==-0.0f} has the value {@code true}. This allows hashtables to operate properly.</li>
     * </ul>
     * 
     * @param obj the object to compare with, {@code null} returns false
     * @return {@code true} if the objects are the same; {@code false} otherwise
     * @see java.lang.Float#floatToIntBits(float)
     */
    @Override
    public boolean equals(final Object obj) {
        return obj instanceof MutableFloat && Float.compare(((MutableFloat) obj).value, value) == 0;
    }

    /**
     * Returns a hash code for this MutableFloat.
     * The hash code is computed using Float.hashCode() to ensure consistency
     * with equals() method and modern Java best practices.
     *
     * @return a suitable hash code
     */
    @Override
    public int hashCode() {
        return Float.hashCode(value);
    }

    //-----------------------------------------------------------------------

    /**
     * Returns the String representation of this MutableFloat's value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableFloat num = MutableFloat.of(3.14f);
     * String str = num.toString(); // returns "3.14"
     * }</pre>
     * 
     * @return the String representation of the current value
     */
    @Override
    public String toString() {
        return N.stringOf(value);
    }

}