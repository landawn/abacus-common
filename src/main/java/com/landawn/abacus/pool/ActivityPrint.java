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

import java.io.Serial;
import java.io.Serializable;

import com.landawn.abacus.annotation.SuppressFBWarnings;

/**
 * Tracks the activity and lifecycle information for poolable objects.
 * This class maintains timestamps and counters to determine when an object should expire
 * from a pool based on age or inactivity.
 * 
 * <p>An ActivityPrint monitors:
 * <ul>
 *   <li>Creation time - when the object was created</li>
 *   <li>Live time - maximum lifetime before expiration</li>
 *   <li>Max idle time - maximum time between accesses before expiration</li>
 *   <li>Last access time - when the object was last accessed</li>
 *   <li>Access count - number of times the object has been accessed</li>
 * </ul>
 * 
 * <p>Objects expire when either:
 * <ul>
 *   <li>Current time - creation time &gt; live time, or</li>
 *   <li>Current time - last access time &gt; max idle time</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create activity print with 1 hour lifetime, 10 minute max idle
 * ActivityPrint activity = new ActivityPrint(3600000, 600000);
 * 
 * // Update on access
 * activity.updateLastAccessTime();
 * activity.updateAccessCount();
 * 
 * // Check expiration
 * if (activity.isExpired()) {
 *     // Remove from pool
 * }
 * }</pre>
 * 
 * @see Poolable
 */
public final class ActivityPrint implements Cloneable, Serializable {

    @Serial
    private static final long serialVersionUID = -45207875951748322L;

    /**
     * The timestamp when this ActivityPrint was created (in milliseconds).
     */
    private long createdTime;

    /**
     * The maximum lifetime allowed for the pooled object (in milliseconds).
     */
    private long liveTime;

    /**
     * The maximum idle time allowed before the pooled object is considered inactive (in milliseconds).
     */
    private long maxIdleTime;

    /**
     * The timestamp of the last access to the pooled object (in milliseconds).
     */
    private long lastAccessTime;

    /**
     * The total number of times this pooled object has been accessed.
     */
    private int accessCount;

    /**
     * Creates a new ActivityPrint with the specified lifetime and idle time limits.
     * 
     * @param liveTime maximum lifetime in milliseconds (must be positive)
     * @param maxIdleTime maximum idle time in milliseconds (must be positive)
     * @throws IllegalArgumentException if liveTime or maxIdleTime is not positive
     */
    public ActivityPrint(final long liveTime, final long maxIdleTime) throws IllegalArgumentException {
        if (liveTime <= 0) {
            throw new IllegalArgumentException("liveTime must be positive, got: " + liveTime);
        }

        if (maxIdleTime <= 0) {
            throw new IllegalArgumentException("maxIdleTime must be positive, got: " + maxIdleTime);
        }

        createdTime = System.currentTimeMillis();

        this.liveTime = liveTime;
        this.maxIdleTime = maxIdleTime;

        lastAccessTime = createdTime;
        accessCount = 0;
    }

    /**
     * Factory method to create a new ActivityPrint with the specified lifetime and idle time limits.
     * This is a convenience method equivalent to calling the constructor.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ActivityPrint activity = ActivityPrint.valueOf(3600000, 300000);
     * }</pre>
     *
     * @param liveTime maximum lifetime in milliseconds (must be positive)
     * @param maxIdleTime maximum idle time in milliseconds (must be positive)
     * @return a new ActivityPrint instance
     * @throws IllegalArgumentException if liveTime or maxIdleTime is not positive
     */
    public static ActivityPrint valueOf(final long liveTime, final long maxIdleTime) {
        return new ActivityPrint(liveTime, maxIdleTime);
    }

    /**
     * Returns the maximum lifetime for this activity print.
     * 
     * @return the maximum lifetime in milliseconds
     */
    public long getLiveTime() {
        return liveTime;
    }

    /**
     * Sets the maximum lifetime for this activity print.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * activity.setLiveTime(7200000);   // set to 2 hours
     * }</pre>
     *
     * @param liveTime the new maximum lifetime in milliseconds
     * @return this ActivityPrint instance for method chaining
     * @throws IllegalArgumentException if liveTime is negative
     */
    public ActivityPrint setLiveTime(final long liveTime) throws IllegalArgumentException {
        if (liveTime < 0) {
            throw new IllegalArgumentException("liveTime cannot be negative, got: " + liveTime);
        }

        this.liveTime = liveTime;

        return this;
    }

    /**
     * Returns the maximum idle time for this activity print.
     * 
     * @return the maximum idle time in milliseconds
     */
    public long getMaxIdleTime() {
        return maxIdleTime;
    }

    /**
     * Sets the maximum idle time for this activity print.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * activity.setMaxIdleTime(600000);   // set to 10 minutes
     * }</pre>
     *
     * @param maxIdleTime the new maximum idle time in milliseconds
     * @return this ActivityPrint instance for method chaining
     * @throws IllegalArgumentException if maxIdleTime is negative
     */
    public ActivityPrint setMaxIdleTime(final long maxIdleTime) throws IllegalArgumentException {
        if (maxIdleTime < 0) {
            throw new IllegalArgumentException("maxIdleTime cannot be negative, got: " + maxIdleTime);
        }

        this.maxIdleTime = maxIdleTime;

        return this;
    }

    /**
     * Returns the creation time of the object associated with this activity print.
     * 
     * @return the creation time in milliseconds since epoch
     */
    public long getCreatedTime() {
        return createdTime;
    }

    /**
     * Sets the creation time for this activity print.
     * This method is package-private and typically used only for testing or special cases.
     * 
     * @param createdTime the new creation time in milliseconds since epoch
     * @return this ActivityPrint instance for method chaining
     */
    ActivityPrint setCreatedTime(final long createdTime) {
        this.createdTime = createdTime;

        return this;
    }

    /**
     * Returns the last access time for the object associated with this activity print.
     * 
     * @return the last access time in milliseconds since epoch
     */
    public long getLastAccessTime() {
        return lastAccessTime;
    }

    /**
     * Updates the last access time to the current system time.
     * This method should be called whenever the associated object is accessed from the pool.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * E pooledObject = pool.get();
     * if (pooledObject != null) {
     *     pooledObject.activityPrint().updateLastAccessTime();
     * }
     * }</pre>
     */
    public void updateLastAccessTime() {
        lastAccessTime = System.currentTimeMillis();
    }

    /**
     * Returns the number of times the associated object has been accessed.
     * 
     * @return the access count
     */
    public int getAccessCount() {
        return accessCount;
    }

    /**
     * Increments the access count by one.
     * This method should be called whenever the associated object is accessed from the pool.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * pooledObject.activityPrint().updateAccessCount();
     * }</pre>
     */
    public void updateAccessCount() {
        accessCount++;
    }

    /**
     * Calculates and returns the expiration time for the associated object.
     * The expiration time is the creation time plus the live time.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long expirationTime = activity.getExpirationTime();
     * long timeUntilExpiration = expirationTime - System.currentTimeMillis();
     * }</pre>
     *
     * @return the expiration time in milliseconds since epoch, or Long.MAX_VALUE if it would overflow
     */
    public long getExpirationTime() {
        return ((Long.MAX_VALUE - createdTime) < liveTime) ? Long.MAX_VALUE : (createdTime + liveTime);
    }

    /**
     * Checks whether the associated object has expired based on its lifetime or idle time.
     *
     * <p>An object is considered expired if either:
     * <ul>
     *   <li>It has exceeded its maximum lifetime (current time - creation time &gt; live time)</li>
     *   <li>It has been idle too long (current time - last access time &gt; max idle time)</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * if (pooledObject.activityPrint().isExpired()) {
     *     pooledObject.destroy(Caller.EVICT);
     * }
     * }</pre>
     *
     * @return {@code true} if the object has expired, {@code false} otherwise
     */
    public boolean isExpired() {
        final long now = System.currentTimeMillis();

        return (maxIdleTime < (now - lastAccessTime)) || (liveTime < (now - createdTime));
    }

    /**
     * Creates and returns a copy of this ActivityPrint.
     * The clone will have the same values for all fields.
     * 
     * @return a clone of this ActivityPrint
     */
    @Override
    public Object clone() { //NOSONAR
        ActivityPrint result = null;

        try {
            result = (ActivityPrint) super.clone();
        } catch (final CloneNotSupportedException e) {
            // ignore;
        }

        return result;
    }

    /**
     * Returns a hash code value for this ActivityPrint.
     * The hash code is computed from all fields.
     * 
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        int result = 7;
        result = 31 * result + Long.hashCode(createdTime);
        result = 31 * result + Long.hashCode(liveTime);
        result = 31 * result + Long.hashCode(maxIdleTime);
        result = 31 * result + Long.hashCode(lastAccessTime);
        result = 31 * result + accessCount;

        return result;
    }

    /**
     * Indicates whether some other object is equal to this ActivityPrint.
     * Two ActivityPrint objects are equal if all their fields have the same values.
     * 
     * @param obj the reference object with which to compare
     * @return {@code true} if this object is equal to the obj argument; {@code false} otherwise
     */
    @SuppressFBWarnings
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof ActivityPrint other) {
            return (createdTime == other.createdTime) && (liveTime == other.liveTime) && (maxIdleTime == other.maxIdleTime)
                    && (lastAccessTime == other.lastAccessTime) && (accessCount == other.accessCount);
        }

        return false;
    }

    /**
     * Returns a string representation of this ActivityPrint.
     * The string contains all field values in a readable format.
     * 
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return "{createdTime=" + createdTime + ", liveTime=" + liveTime + ", maxIdleTime=" + maxIdleTime + ", lastAccessedTime=" + lastAccessTime
                + ", accessCount=" + accessCount + "}";
    }
}
