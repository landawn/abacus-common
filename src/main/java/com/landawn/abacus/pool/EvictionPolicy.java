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
 * Enumeration of eviction policies that determine which objects to remove from a pool
 * when it reaches capacity or during periodic eviction runs.
 * 
 * <p>The eviction policy affects both:
 * <ul>
 *   <li>Automatic eviction of expired objects during scheduled eviction runs</li>
 *   <li>Selection of objects to remove during vacate operations when the pool is full</li>
 * </ul>
 * 
 * <p>Each policy uses different criteria from the object's {@link ActivityPrint} to determine
 * eviction priority. Objects with lower values according to the policy's criteria are
 * evicted first.
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // LRU-style pool - evicts least recently used objects
 * ObjectPool<Resource> lruPool = PoolFactory.createObjectPool(
 *     100, 60000, EvictionPolicy.LAST_ACCESS_TIME
 * );
 * 
 * // LFU-style pool - evicts least frequently used objects  
 * ObjectPool<Resource> lfuPool = PoolFactory.createObjectPool(
 *     100, 60000, EvictionPolicy.ACCESS_COUNT
 * );
 * 
 * // Time-based pool - evicts objects closest to expiration
 * ObjectPool<Resource> timePool = PoolFactory.createObjectPool(
 *     100, 60000, EvictionPolicy.EXPIRATION_TIME
 * );
 * }</pre>
 * 
 * @see AbstractPool
 * @see ActivityPrint
 * @see PoolFactory
 */
public enum EvictionPolicy {
    /**
     * Least Recently Used (LRU) eviction policy.
     * Objects are evicted based on their last access time, with the least recently
     * accessed objects being evicted first.
     * 
     * <p>This policy is ideal for:
     * <ul>
     *   <li>General-purpose caching where recent access indicates future use</li>
     *   <li>Scenarios where temporal locality is important</li>
     *   <li>Default choice when unsure which policy to use</li>
     * </ul>
     * 
     * <p>Objects with the oldest {@link ActivityPrint#getLastAccessTime()} are evicted first.
     */
    LAST_ACCESS_TIME,

    /**
     * Time-based eviction policy.
     * Objects are evicted based on their expiration time, with objects closest to
     * expiration being evicted first.
     * 
     * <p>This policy is ideal for:
     * <ul>
     *   <li>Pools where objects have varying lifetimes</li>
     *   <li>Scenarios where you want to maximize object lifetime utilization</li>
     *   <li>Cases where objects become less valuable as they age</li>
     * </ul>
     * 
     * <p>Objects with the earliest {@link ActivityPrint#getExpirationTime()} are evicted first.
     * This ensures objects are used for as much of their lifetime as possible.
     */
    EXPIRATION_TIME,

    /**
     * Least Frequently Used (LFU) eviction policy.
     * Objects are evicted based on their access count, with the least frequently
     * accessed objects being evicted first.
     * 
     * <p>This policy is ideal for:
     * <ul>
     *   <li>Scenarios where popular objects should remain cached</li>
     *   <li>Pools where access frequency is a good predictor of future use</li>
     *   <li>Long-running applications with stable access patterns</li>
     * </ul>
     * 
     * <p>Objects with the lowest {@link ActivityPrint#getAccessCount()} are evicted first.
     * Note that this policy may keep old but frequently accessed objects indefinitely.
     */
    ACCESS_COUNT
}
