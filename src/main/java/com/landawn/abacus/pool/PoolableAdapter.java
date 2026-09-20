/*
 * Copyright (C) 2015 HaiYang Li
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

package com.landawn.abacus.pool;

import com.landawn.abacus.util.N;

/**
 * An adapter class that makes any object poolable by implementing the Poolable interface.
 * The adapted object reference is fixed after construction, while the inherited, publicly exposed
 * {@link ActivityPrint} remains mutable so the pool can update access and expiration metadata.
 *
 * <p>The adapter maintains an activityPrint to track the adapted object's lifecycle
 * and provides a no-op {@link #destroy(Caller)} method since the adapter doesn't
 * own the adapted object's lifecycle.
 *
 * <p>Common use cases:
 * <ul>
 *   <li>Pooling simple value objects or data holders</li>
 *   <li>Pooling objects from third-party libraries</li>
 *   <li>Temporary pooling of objects that don't need cleanup</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Wrap with infinite lifetime
 * String data = "cached data";
 * PoolableAdapter<String> adapted = new PoolableAdapter<>(data);
 * pool.add(adapted);
 *
 * // Wrap with specific lifetime limits
 * ByteBuffer buffer = ByteBuffer.allocate(1024);
 * PoolableAdapter<ByteBuffer> adaptedBuffer = new PoolableAdapter<>(
 *     buffer,
 *     600000,  // 10 minute lifetime
 *     60000    // 1 minute max idle
 * );
 * pool.add(adaptedBuffer);
 *
 * // Retrieve and use
 * PoolableAdapter<String> retrieved = pool.poll();
 * if (retrieved != null) {
 *     String value = retrieved.value();
 *     // use value
 *     pool.add(retrieved);   // adds it back to the pool
 * }
 * }</pre>
 *
 * @param <T> the type of the adapted object
 * @see Poolable
 * @see AbstractPoolable
 * @see ObjectPool
 * @see KeyedObjectPool
 */
public final class PoolableAdapter<T> extends AbstractPoolable {

    /**
     * The adapted object. This field is final and never modified after construction.
     */
    private final T srcObject;

    /**
     * Constructs a new PoolableAdapter with infinite lifetime and idle time.
     * The adapted object will never expire based on time.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * PoolableAdapter<String> adapted = new PoolableAdapter<>("cached");
     * adapted.value();                            // returns "cached"
     * adapted.activityPrint().getMaxLiveTime();   // returns Long.MAX_VALUE (never expires by lifetime)
     * adapted.activityPrint().getMaxIdleTime();   // returns Long.MAX_VALUE (never expires by idle)
     * new PoolableAdapter<>(null).value();        // returns null (a null value is permitted)
     * }</pre>
     *
     * @param value the object to adapt, can be {@code null}; must be {@code Serializable} if this adapter
     *        (or a pool containing it) is to be serialized
     */
    public PoolableAdapter(final T value) {
        this(value, Long.MAX_VALUE, Long.MAX_VALUE);
    }

    /**
     * Constructs a new PoolableAdapter with specified lifetime and idle time limits.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * PoolableAdapter<String> adapted = new PoolableAdapter<>("data", 600000, 60000);   // 10 min live, 1 min idle
     * adapted.value();                                                                  // returns "data"
     * adapted.activityPrint().getMaxLiveTime();                                         // returns 600000
     * adapted.activityPrint().getMaxIdleTime();                                         // returns 60000
     * }</pre>
     *
     * @param value the object to adapt, can be {@code null}; must be {@code Serializable} if this adapter
     *        (or a pool containing it) is to be serialized
     * @param maxLiveTime the maximum lifetime in milliseconds before expiration
     * @param maxIdleTime the maximum idle time in milliseconds before expiration
     * @throws IllegalArgumentException if maxLiveTime or maxIdleTime is not positive.
     */
    public PoolableAdapter(final T value, final long maxLiveTime, final long maxIdleTime) throws IllegalArgumentException {
        super(maxLiveTime, maxIdleTime);
        this.srcObject = value;
    }

    /**
     * Creates a new PoolableAdapter with infinite lifetime and idle time.
     * This is a convenience factory method equivalent to calling the constructor.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> data = Arrays.asList("a", "b", "c");
     * PoolableAdapter<List<String>> adapted = PoolableAdapter.of(data);
     * }</pre>
     *
     * @param <T> the type of the object to adapt
     * @param value the object to adapt, can be {@code null}; must be {@code Serializable} if the adapter
     *        (or a pool containing it) is to be serialized
     * @return a new PoolableAdapter containing the source object
     */
    public static <T> PoolableAdapter<T> of(final T value) {
        return new PoolableAdapter<>(value);
    }

    /**
     * Creates a new PoolableAdapter with specified lifetime and idle time limits.
     * This is a convenience factory method for creating adapters with expiration settings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Cache a computed result for 5 minutes, expire after 1 minute idle
     * ComplexResult result = computeExpensiveOperation();
     * PoolableAdapter<ComplexResult> adapted = PoolableAdapter.of(
     *     result, 300000, 60000
     * );
     * }</pre>
     *
     * @param <T> the type of the object to adapt
     * @param value the object to adapt, can be {@code null}; must be {@code Serializable} if the adapter
     *        (or a pool containing it) is to be serialized
     * @param maxLiveTime the maximum lifetime in milliseconds before expiration
     * @param maxIdleTime the maximum idle time in milliseconds before expiration
     * @return a new PoolableAdapter with the specified settings
     * @throws IllegalArgumentException if maxLiveTime or maxIdleTime is not positive.
     */
    public static <T> PoolableAdapter<T> of(final T value, final long maxLiveTime, final long maxIdleTime) throws IllegalArgumentException {
        return new PoolableAdapter<>(value, maxLiveTime, maxIdleTime);
    }

    /**
     * Returns the adapted object.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * PoolableAdapter<String> adapted = pool.poll();
     * if (adapted != null) {
     *     String data = adapted.value();
     *     // use data
     * }
     * }</pre>
     *
     * @return the object adapted by this PoolableAdapter, may be {@code null}
     */
    public T value() {
        return srcObject;
    }

    /**
     * No-op implementation of destroy.
     * Since the adapter doesn't own the adapted object, it performs no cleanup.
     * The adapted object remains unchanged and can still be accessed via {@link #value()}.
     *
     * @param caller the reason for destruction (ignored)
     */
    @Override
    public void destroy(final Caller caller) {
        // should not set the srcObject to null because it may be retrieved by
        // other thread and evicted out pool later.
        // srcObject = null;
    }

    /**
     * Returns a hash code for this adapter.
     * The hash code is based solely on the adapted object.
     *
     * @return the hash code of the adapted object, or 0 if the adapted object is null
     */
    @Override
    public int hashCode() {
        return N.hashCode(srcObject);
    }

    /**
     * Compares this adapter to another object for equality.
     * Two adapters are equal if they adapt equal objects (according to the adapted object's equals method).
     *
     * @param obj the object to compare with
     * @return {@code true} if the adapted objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        return this == obj || (obj instanceof PoolableAdapter && N.equals(((PoolableAdapter<?>) obj).srcObject, srcObject));
    }

    /**
     * Returns a string representation of this adapter.
     * The string includes both the adapted object and the activity print information.
     *
     * @return a string representation of this adapter
     */
    @Override
    public String toString() {
        return "{srcObject=" + srcObject + "; activityPrint=" + activityPrint + "}";
    }
}
