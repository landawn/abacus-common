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
 * Abstract base class for implementing poolable objects.
 * This class provides a convenient base implementation of the {@link Poolable} interface,
 * managing the {@link ActivityPrint} for tracking object lifecycle and usage.
 * 
 * <p>Subclasses need only implement the {@link #destroy(Poolable.Caller)} method to define
 * their cleanup behavior when removed from a pool.
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * public class PooledConnection extends AbstractPoolable {
 *     private final Connection connection;
 *     
 *     public PooledConnection(Connection conn) {
 *         super(3600000, 300000); // 1 hour lifetime, 5 minute max idle
 *         this.connection = conn;
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
 *     
 *     public Connection getConnection() {
 *         return connection;
 *     }
 * }
 * }</pre>
 * 
 * @see Poolable
 * @see ActivityPrint
 * @see ObjectPool
 * @see KeyedObjectPool
 */
public abstract class AbstractPoolable implements Poolable {

    /**
     * The activity print tracking lifecycle and usage statistics for this poolable object.
     * This field is final and initialized in the constructor.
     */
    final ActivityPrint activityPrint;

    /**
     * Constructs a new AbstractPoolable with the specified lifetime and idle time limits.
     * 
     * @param liveTime the maximum lifetime in milliseconds before this object expires
     * @param maxIdleTime the maximum idle time in milliseconds before this object expires
     * @throws IllegalArgumentException if liveTime or maxIdleTime is not positive
     */
    protected AbstractPoolable(final long liveTime, final long maxIdleTime) {
        activityPrint = new ActivityPrint(liveTime, maxIdleTime);
    }

    /**
     * Returns the activity print for this poolable object.
     * The activity print tracks creation time, access patterns, and expiration status.
     * 
     * @return the ActivityPrint associated with this object
     */
    @Override
    public ActivityPrint activityPrint() {
        return activityPrint;
    }
}