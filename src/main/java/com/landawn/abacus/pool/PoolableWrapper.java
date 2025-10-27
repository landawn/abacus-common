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

import com.landawn.abacus.util.Immutable;
import com.landawn.abacus.util.N;

/**
 * A wrapper class that makes any object poolable by implementing the Poolable interface.
 * This class is immutable and provides a simple way to pool objects that don't naturally
 * implement the Poolable interface.
 * 
 * <p>The wrapper maintains an {@link ActivityPrint} to track the wrapped object's lifecycle
 * and provides a no-op {@link #destroy(Poolable.Caller)} method since the wrapper doesn't
 * own the wrapped object's lifecycle.
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
 * PoolableWrapper<String> wrapped = PoolableWrapper.of(data);
 * pool.add(wrapped);
 * 
 * // Wrap with specific lifetime limits
 * ByteBuffer buffer = ByteBuffer.allocate(1024);
 * PoolableWrapper<ByteBuffer> wrappedBuffer = PoolableWrapper.of(
 *     buffer, 
 *     600000,  // 10 minute lifetime
 *     60000    // 1 minute max idle
 * );
 * pool.add(wrappedBuffer);
 * 
 * // Retrieve and use
 * PoolableWrapper<String> retrieved = pool.take();
 * if (retrieved != null) {
 *     String value = retrieved.value();
 *     // use value
 *     pool.add(retrieved); // return to pool
 * }
 * }</pre>
 * 
 * @param <T> the type of the wrapped object
 * @see Poolable
 * @see AbstractPoolable
 * @see ObjectPool
 * @see KeyedObjectPool
 */
@com.landawn.abacus.annotation.Immutable
public final class PoolableWrapper<T> extends AbstractPoolable implements Immutable {

    /**
     * The wrapped object. This field is final and never modified after construction.
     */
    private final T srcObject;

    /**
     * Constructs a new PoolableWrapper with infinite lifetime and idle time.
     * The wrapped object will never expire based on time.
     * 
     * @param srcObject the object to wrap, can be null
     */
    public PoolableWrapper(final T srcObject) {
        this(srcObject, Long.MAX_VALUE, Long.MAX_VALUE);
    }

    /**
     * Constructs a new PoolableWrapper with specified lifetime and idle time limits.
     * 
     * @param srcObject the object to wrap, can be null
     * @param liveTime the maximum lifetime in milliseconds before expiration
     * @param maxIdleTime the maximum idle time in milliseconds before expiration
     * @throws IllegalArgumentException if liveTime or maxIdleTime is not positive
     */
    public PoolableWrapper(final T srcObject, final long liveTime, final long maxIdleTime) {
        super(liveTime, maxIdleTime);
        this.srcObject = srcObject;
    }

    /**
     * Creates a new PoolableWrapper with infinite lifetime and idle time.
     * This is a convenience factory method equivalent to calling the constructor.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> data = Arrays.asList("a", "b", "c");
     * PoolableWrapper<List<String>> wrapped = PoolableWrapper.of(data);
     * }</pre>
     * 
     * @param <T> the type of the object to wrap
     * @param srcObject the object to wrap, can be null
     * @return a new PoolableWrapper containing the source object
     */
    public static <T> PoolableWrapper<T> of(final T srcObject) {
        return new PoolableWrapper<>(srcObject);
    }

    /**
     * Creates a new PoolableWrapper with specified lifetime and idle time limits.
     * This is a convenience factory method for creating wrappers with expiration settings.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Cache a computed result for 5 minutes, expire after 1 minute idle
     * ComplexResult result = computeExpensiveOperation();
     * PoolableWrapper<ComplexResult> wrapped = PoolableWrapper.of(
     *     result, 300000, 60000
     * );
     * }</pre>
     * 
     * @param <T> the type of the object to wrap
     * @param srcObject the object to wrap, can be null
     * @param liveTime the maximum lifetime in milliseconds before expiration
     * @param maxIdleTime the maximum idle time in milliseconds before expiration
     * @return a new PoolableWrapper with the specified settings
     * @throws IllegalArgumentException if liveTime or maxIdleTime is not positive
     */
    public static <T> PoolableWrapper<T> of(final T srcObject, final long liveTime, final long maxIdleTime) {
        return new PoolableWrapper<>(srcObject, liveTime, maxIdleTime);
    }

    /**
     * Returns the wrapped object.
     * 
     * @return the object wrapped by this PoolableWrapper, may be null
     */
    public T value() {
        return srcObject;
    }

    /**
     * No-op implementation of destroy.
     * Since the wrapper doesn't own the wrapped object, it performs no cleanup.
     * The wrapped object remains unchanged and can still be accessed via {@link #value()}.
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
     * Returns a hash code for this wrapper.
     * The hash code is based solely on the wrapped object.
     * 
     * @return the hash code of the wrapped object, or 0 if the wrapped object is null
     */
    @Override
    public int hashCode() {
        return N.hashCode(srcObject);
    }

    /**
     * Compares this wrapper to another object for equality.
     * Two wrappers are equal if they wrap equal objects (according to the wrapped object's equals method).
     * 
     * @param obj the object to compare with
     * @return {@code true} if the wrapped objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        return this == obj || (obj instanceof PoolableWrapper && N.equals(((PoolableWrapper<?>) obj).srcObject, srcObject));
    }

    /**
     * Returns a string representation of this wrapper.
     * The string includes both the wrapped object and the activity print information.
     * 
     * @return a string representation of this wrapper
     */
    @Override
    public String toString() {
        return "{srcObject=" + srcObject + "; activityPrint=" + activityPrint + "}";
    }
}
