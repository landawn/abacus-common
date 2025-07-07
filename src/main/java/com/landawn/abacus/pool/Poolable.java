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

/**
 * Interface for objects that can be managed by a pool.
 * Objects implementing this interface can track their activity and lifecycle within a pool,
 * and provide cleanup logic when removed from the pool.
 * 
 * <p>Poolable objects maintain an {@link ActivityPrint} that tracks:
 * <ul>
 *   <li>Creation time and maximum lifetime</li>
 *   <li>Last access time and maximum idle time</li>
 *   <li>Access count</li>
 * </ul>
 * 
 * <p>The {@link #destroy(Caller)} method is called when an object is removed from the pool,
 * allowing for proper cleanup of resources.
 * 
 * <p>Usage example:
 * <pre>{@code
 * public class PooledConnection implements Poolable {
 *     private final Connection connection;
 *     private final ActivityPrint activityPrint;
 *     
 *     public PooledConnection(Connection conn) {
 *         this.connection = conn;
 *         this.activityPrint = new ActivityPrint(3600000, 600000); // 1hr live, 10min idle
 *     }
 *     
 *     @Override
 *     public ActivityPrint activityPrint() {
 *         return activityPrint;
 *     }
 *     
 *     @Override
 *     public void destroy(Caller caller) {
 *         try {
 *             connection.close();
 *         } catch (SQLException e) {
 *             // log error
 *         }
 *     }
 * }
 * }</pre>
 * 
 * @see ActivityPrint
 * @see ObjectPool
 * @see KeyedObjectPool
 */
public interface Poolable {

    /**
     * Returns the activity print associated with this poolable object.
     * The activity print tracks the lifecycle and usage statistics of this object within the pool.
     * 
     * <p>The returned ActivityPrint should be the same instance throughout the object's lifetime
     * to ensure consistent tracking of access patterns and expiration.
     * 
     * @return the ActivityPrint for this object, never null
     */
    ActivityPrint activityPrint();

    /**
     * Destroys this poolable object and releases any resources it holds.
     * This method is called when the object is being removed from the pool.
     * 
     * <p>Implementations should:
     * <ul>
     *   <li>Close any open resources (connections, files, etc.)</li>
     *   <li>Cancel any pending operations</li>
     *   <li>Clear references to help garbage collection</li>
     *   <li>Handle exceptions gracefully without throwing</li>
     * </ul>
     * 
     * <p>The caller parameter indicates why the object is being destroyed,
     * which can be useful for logging or different cleanup strategies.
     * 
     * @param caller the reason for destruction, providing context about why the object is being removed
     * @see Caller
     */
    void destroy(Caller caller);

    /**
     * Wraps the provided object in a PoolableWrapper with maximum lifetime and idle time.
     * This is a convenience method for creating poolable objects from existing instances.
     * 
     * <p>The wrapped object will have:
     * <ul>
     *   <li>Live time: Long.MAX_VALUE (effectively infinite)</li>
     *   <li>Max idle time: Long.MAX_VALUE (effectively infinite)</li>
     * </ul>
     * 
     * <p>Usage example:
     * <pre>{@code
     * String data = "some data";
     * PoolableWrapper<String> poolable = Poolable.wrap(data);
     * pool.add(poolable);
     * }</pre>
     * 
     * @param <T> the type of the object to wrap
     * @param srcObject the object to wrap, must not be null
     * @return a PoolableWrapper containing the source object
     */
    static <T> PoolableWrapper<T> wrap(final T srcObject) {
        return PoolableWrapper.of(srcObject);
    }

    /**
     * Wraps the provided object in a PoolableWrapper with specified lifetime and idle time limits.
     * This allows fine-grained control over when wrapped objects expire.
     * 
     * <p>Usage example:
     * <pre>{@code
     * // Wrap a buffer that expires after 5 minutes or 1 minute of inactivity
     * ByteBuffer buffer = ByteBuffer.allocate(1024);
     * PoolableWrapper<ByteBuffer> poolable = Poolable.wrap(buffer, 300000, 60000);
     * pool.add(poolable);
     * }</pre>
     * 
     * @param <T> the type of the object to wrap
     * @param srcObject the object to wrap, must not be null
     * @param liveTime maximum lifetime in milliseconds before the object expires
     * @param maxIdleTime maximum idle time in milliseconds before the object expires
     * @return a PoolableWrapper containing the source object with the specified expiration settings
     * @throws IllegalArgumentException if liveTime or maxIdleTime is not positive
     */
    static <T> PoolableWrapper<T> wrap(final T srcObject, final long liveTime, final long maxIdleTime) {
        return PoolableWrapper.of(srcObject, liveTime, maxIdleTime);
    }

    /**
     * Enumeration of reasons why a poolable object might be destroyed.
     * This provides context to the {@link Poolable#destroy(Caller)} method about
     * why the object is being removed from the pool.
     */
    enum Caller {
        /**
         * The pool is being closed and all objects are being destroyed.
         * This typically happens during application shutdown.
         */
        CLOSE(0),

        /**
         * The object is being evicted because it has expired.
         * This can be due to exceeding its maximum lifetime or idle time.
         */
        EVICT(1),

        /**
         * The object is being removed to make space in the pool.
         * This happens during vacate operations when the pool needs to free capacity.
         */
        VACATE(2),

        /**
         * The object is being removed due to a remove(), replace, or clear() operation.
         * This indicates explicit removal by user action.
         */
        REMOVE_REPLACE_CLEAR(3),

        /**
         * The object could not be added to the pool.
         * This happens when autoDestroyOnFailedToAdd is true and the add operation fails.
         */
        PUT_ADD_FAILURE(4),

        /**
         * The object is being destroyed for some other external reason.
         * This can be used for custom destruction scenarios.
         */
        OTHER_OUTER(5);

        private final int value;

        Caller(final int value) {
            this.value = value;
        }

        /**
         * Returns the numeric value associated with this caller reason.
         * 
         * @return the numeric identifier for this caller type
         */
        public int value() {
            return value;
        }
    }
}