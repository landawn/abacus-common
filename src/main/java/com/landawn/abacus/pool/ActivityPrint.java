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

public final class ActivityPrint implements Cloneable, Serializable {

    @Serial
    private static final long serialVersionUID = -45207875951748322L;

    private long createdTime;

    private long liveTime;

    private long maxIdleTime;

    private long lastAccessTime;

    private int accessCount;

    /**
     *
     * @param liveTime
     * @param maxIdleTime
     * @throws IllegalArgumentException
     */
    public ActivityPrint(final long liveTime, final long maxIdleTime) throws IllegalArgumentException {
        if (liveTime <= 0) {
            throw new IllegalArgumentException("Illegal liveTime[" + liveTime + "]. ");
        }

        if (maxIdleTime <= 0) {
            throw new IllegalArgumentException("Illegal maxIdleTime[" + maxIdleTime + "]. ");
        }

        createdTime = System.currentTimeMillis();

        this.liveTime = liveTime;
        this.maxIdleTime = maxIdleTime;

        lastAccessTime = createdTime;
        accessCount = 0;
    }

    /**
     *
     * @param liveTime
     * @param maxIdleTime
     * @return
     */
    public static ActivityPrint valueOf(final long liveTime, final long maxIdleTime) {
        return new ActivityPrint(liveTime, maxIdleTime);
    }

    /**
     * Gets the live time.
     *
     * @return
     */
    public long getLiveTime() {
        return liveTime;
    }

    /**
     * Sets the live time.
     *
     * @param liveTime
     * @return
     * @throws IllegalArgumentException the illegal argument exception
     */
    public ActivityPrint setLiveTime(final long liveTime) throws IllegalArgumentException {
        if (liveTime < 0) {
            throw new IllegalArgumentException("Illegal live time: " + liveTime);
        }

        this.liveTime = liveTime;

        return this;
    }

    /**
     * Gets the max idle time.
     *
     * @return
     */
    public long getMaxIdleTime() {
        return maxIdleTime;
    }

    /**
     * Sets the max idle time.
     *
     * @param maxIdleTime
     * @return
     * @throws IllegalArgumentException the illegal argument exception
     */
    public ActivityPrint setMaxIdleTime(final long maxIdleTime) throws IllegalArgumentException {
        if (maxIdleTime < 0) {
            throw new IllegalArgumentException("Illegal idle time: " + maxIdleTime);
        }

        this.maxIdleTime = maxIdleTime;

        return this;
    }

    /**
     * Gets the creation time.
     *
     * @return
     */
    public long getCreatedTime() {
        return createdTime;
    }

    /**
     * Sets the creation time.
     *
     * @param createdTime
     * @return
     */
    ActivityPrint setCreatedTime(final long createdTime) {
        this.createdTime = createdTime;

        return this;
    }

    /**
     * Gets the last access time.
     *
     * @return
     */
    public long getLastAccessTime() {
        return lastAccessTime;
    }

    /**
     * Update last access time.
     */
    public void updateLastAccessTime() {
        lastAccessTime = System.currentTimeMillis();
    }

    /**
     * Gets the access count.
     *
     * @return
     */
    public int getAccessCount() {
        return accessCount;
    }

    /**
     * Update access count.
     */
    public void updateAccessCount() {
        accessCount++;
    }

    /**
     * Gets the expiration time.
     *
     * @return
     */
    public long getExpirationTime() {
        return ((Long.MAX_VALUE - createdTime) < liveTime) ? Long.MAX_VALUE : (createdTime + liveTime);
    }

    /**
     * Checks if is expired.
     *
     * @return {@code true}, if is expired
     */
    public boolean isExpired() {
        final long now = System.currentTimeMillis();

        return (maxIdleTime < (now - lastAccessTime)) || (liveTime < (now - createdTime));
    }

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

    @Override
    public int hashCode() {
        long h = 7;
        h = (h * 31) + createdTime;
        h = (h * 31) + liveTime;
        h = (h * 31) + maxIdleTime;
        h = (h * 31) + lastAccessTime;
        h = (h * 31) + accessCount;

        return (int) h;
    }

    /**
     *
     * @param obj
     * @return {@code true}, if successful
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

    @Override
    public String toString() {
        return "{createdTime=" + createdTime + ", liveTime=" + liveTime + ", maxIdleTime=" + maxIdleTime + ", lastAccessedTime=" + lastAccessTime
                + ", accessCount=" + accessCount + "}";
    }
}
