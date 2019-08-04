/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.pool;

import java.io.Serializable;

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public final class ActivityPrint implements Cloneable, Serializable {
    private static final long serialVersionUID = -45207875951748322L;

    private long createTime;
    private long liveTime;
    private long maxIdleTime;
    private long lastAccessTime;
    private int accessCount;

    public ActivityPrint(long liveTime, long maxIdleTime) throws IllegalArgumentException {
        if (liveTime <= 0) {
            throw new IllegalArgumentException("Illegal liveTime[" + liveTime + "]. ");
        }

        if (maxIdleTime <= 0) {
            throw new IllegalArgumentException("Illegal maxIdleTime[" + maxIdleTime + "]. ");
        }

        this.createTime = System.currentTimeMillis();

        this.liveTime = liveTime;
        this.maxIdleTime = maxIdleTime;

        this.lastAccessTime = createTime;
        this.accessCount = 0;
    }

    public static ActivityPrint valueOf(long liveTime, long maxIdleTime) {
        return new ActivityPrint(liveTime, maxIdleTime);
    }

    public long getLiveTime() {
        return liveTime;
    }

    public ActivityPrint setLiveTime(long liveTime) throws IllegalArgumentException {
        if (liveTime < 0) {
            throw new IllegalArgumentException("Illegal live time: " + liveTime);
        }

        this.liveTime = liveTime;

        return this;
    }

    public long getMaxIdleTime() {
        return maxIdleTime;
    }

    public ActivityPrint setMaxIdleTime(long maxIdleTime) throws IllegalArgumentException {
        if (maxIdleTime < 0) {
            throw new IllegalArgumentException("Illegal idle time: " + maxIdleTime);
        }

        this.maxIdleTime = maxIdleTime;

        return this;
    }

    public long getCreateTime() {
        return createTime;
    }

    ActivityPrint setCreateTime(long createTime) {
        this.createTime = createTime;

        return this;
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }

    public void updateLastAccessTime() {
        lastAccessTime = System.currentTimeMillis();
    }

    public int getAccessCount() {
        return accessCount;
    }

    public void updateAccessCount() {
        accessCount++;
    }

    public long getExpirationTime() {
        return ((Long.MAX_VALUE - createTime) < liveTime) ? Long.MAX_VALUE : (createTime + liveTime);
    }

    public boolean isExpired() {
        long now = System.currentTimeMillis();

        return (maxIdleTime < (now - lastAccessTime)) || (liveTime < (now - createTime));
    }

    @Override
    public Object clone() {
        ActivityPrint result = null;

        try {
            result = (ActivityPrint) super.clone();
        } catch (CloneNotSupportedException e) {
            // ignore;
        }

        return result;
    }

    @Override
    public int hashCode() {
        long h = 7;
        h = (h * 31) + createTime;
        h = (h * 31) + liveTime;
        h = (h * 31) + maxIdleTime;
        h = (h * 31) + lastAccessTime;
        h = (h * 31) + accessCount;

        return (int) h;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof ActivityPrint) {
            ActivityPrint other = (ActivityPrint) obj;

            return (createTime == other.createTime) && (liveTime == other.liveTime) && (maxIdleTime == other.maxIdleTime)
                    && (lastAccessTime == other.lastAccessTime) && (accessCount == other.accessCount);
        }

        return false;
    }

    @Override
    public String toString() {
        return "{createTime=" + createTime + ", liveTime=" + liveTime + ", maxIdleTime=" + maxIdleTime + ", lastAccessedTime=" + lastAccessTime
                + ", accessCount=" + accessCount + "}";
    }
}
