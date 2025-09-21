/*
 * Copyright (C) 2017 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

/**
 * An immutable container that holds a value paired with a timestamp.
 * This class is useful for tracking when a value was created or last updated,
 * implementing caching mechanisms, or maintaining temporal data.
 * 
 * <p>The timestamp represents milliseconds since the Unix epoch (January 1, 1970 UTC).
 * Once created, both the value and timestamp are immutable.
 * 
 * <p>Example usage:
 * <pre>{@code
 * // Create a timed value with current timestamp
 * Timed<String> timedData = Timed.of("Hello World");
 * 
 * // Create a timed value with specific timestamp
 * long specificTime = System.currentTimeMillis() - 3600000; // 1 hour ago
 * Timed<Integer> timedCount = Timed.of(42, specificTime);
 * 
 * // Access value and timestamp
 * String data = timedData.value();
 * long when = timedData.timestamp();
 * }</pre>
 * 
 * @param <T> the type of the value being timed
 * @since 1.0
 */
@com.landawn.abacus.annotation.Immutable
public final class Timed<T> implements Immutable {

    private final long timeInMillis;

    private final T value;

    /**
     * Private constructor. Use static factory methods {@link #of(Object)} or 
     * {@link #of(Object, long)} to create instances.
     *
     * @param value the value to associate with a timestamp
     * @param timeInMillis the timestamp in milliseconds since epoch
     */
    Timed(final T value, final long timeInMillis) {
        this.value = value;
        this.timeInMillis = timeInMillis;
    }

    /**
     * Creates a new Timed instance with the specified value and the current system time.
     * The timestamp is captured at the moment this method is called.
     * 
     * <p>Example usage:
     * <pre>{@code
     * Timed<String> message = Timed.of("System started");
     * Timed<List<String>> snapshot = Timed.of(activeUsers);
     * }</pre>
     *
     * @param <T> the type of the value
     * @param value the value to associate with the current timestamp; can be null
     * @return a new Timed instance containing the value and current timestamp
     */
    public static <T> Timed<T> of(final T value) {
        return new Timed<>(value, System.currentTimeMillis());
    }

    /**
     * Creates a new Timed instance with the specified value and timestamp.
     * This allows creating timed values with historical timestamps or timestamps
     * from external sources.
     * 
     * <p>Example usage:
     * <pre>{@code
     * // Create with a past timestamp
     * long yesterday = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);
     * Timed<String> historicalEvent = Timed.of("Event occurred", yesterday);
     * 
     * // Create from a database timestamp
     * long dbTimestamp = resultSet.getTimestamp("created_at").getTime();
     * Timed<User> timedUser = Timed.of(user, dbTimestamp);
     * }</pre>
     *
     * @param <T> the type of the value
     * @param value the value to associate with the timestamp; can be null
     * @param timeInMillis the timestamp in milliseconds since epoch
     * @return a new Timed instance containing the value and specified timestamp
     */
    public static <T> Timed<T> of(final T value, final long timeInMillis) {
        return new Timed<>(value, timeInMillis);
    }

    /**
     * Returns the timestamp associated with this timed value.
     * The timestamp represents milliseconds since the Unix epoch (January 1, 1970 UTC).
     * 
     * <p>Example usage:
     * <pre>{@code
     * Timed<String> timedData = Timed.of("data");
     * long age = System.currentTimeMillis() - timedData.timestamp();
     * System.out.println("Data is " + age + " milliseconds old");
     * }</pre>
     *
     * @return the timestamp in milliseconds since epoch
     */
    public long timestamp() {
        return timeInMillis;
    }

    /**
     * Returns the value contained in this timed instance.
     * The returned value may be null if null was provided when creating the instance.
     * 
     * <p>Example usage:
     * <pre>{@code
     * Timed<User> timedUser = Timed.of(currentUser);
     * User user = timedUser.value();
     * processUser(user);
     * }</pre>
     *
     * @return the value associated with the timestamp; may be null
     */
    public T value() {
        return value;
    }

    /**
     * Returns a hash code value for this timed instance.
     * The hash code is computed based on both the timestamp and the value.
     * 
     * <p>The hash code is calculated as: {@code (int)(timestamp * 31 + valueHashCode)}
     * where valueHashCode is 0 if the value is null, otherwise it's the value's hash code.
     *
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        return (int) (timeInMillis * 31 + (value == null ? 0 : value.hashCode()));
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * Two Timed instances are considered equal if they have the same timestamp
     * and their values are equal (or both null).
     * 
     * <p>Example usage:
     * <pre>{@code
     * Timed<String> t1 = Timed.of("hello", 1000L);
     * Timed<String> t2 = Timed.of("hello", 1000L);
     * Timed<String> t3 = Timed.of("hello", 2000L);
     * 
     * assertTrue(t1.equals(t2));  // Same value and timestamp
     * assertFalse(t1.equals(t3)); // Same value, different timestamp
     * }</pre>
     *
     * @param obj the reference object with which to compare
     * @return {@code true} if this object is equal to the obj argument; false otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Timed<?> other) {
            return timeInMillis == other.timeInMillis && N.equals(value, other.value);
        }

        return false;
    }

    /**
     * Returns a string representation of this timed instance.
     * The string representation consists of the timestamp followed by a colon,
     * a space, and the string representation of the value.
     * 
     * <p>Format: {@code "timestamp: value"}
     * 
     * <p>Example output:
     * <pre>{@code
     * Timed<String> t = Timed.of("Hello", 1609459200000L);
     * System.out.println(t); // Prints: "1609459200000: Hello"
     * }</pre>
     *
     * @return a string representation of this timed instance
     */
    @Override
    public String toString() {
        return timeInMillis + ": " + N.toString(value);
    }
}